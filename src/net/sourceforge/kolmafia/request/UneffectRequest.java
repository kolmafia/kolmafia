package net.sourceforge.kolmafia.request;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLCharacter.TurtleBlessing;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.KoLmafiaCLI;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.moods.MoodManager;
import net.sourceforge.kolmafia.objectpool.EffectPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.objectpool.SkillPool;
import net.sourceforge.kolmafia.persistence.ConcoctionDatabase;
import net.sourceforge.kolmafia.persistence.EffectDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.persistence.SkillDatabase;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.session.InventoryManager;
import net.sourceforge.kolmafia.session.ResultProcessor;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class UneffectRequest extends GenericRequest {
  private final int effectId;
  private final boolean isShruggable;
  private final boolean isAsdon;
  private final boolean needsCocoa;
  private final boolean isTimer;
  private final AdventureResult effect;

  private static final Set<AdventureResult> currentEffectRemovals = new HashSet<>();

  private static final AdventureResult USED_REMEDY = ItemPool.get(ItemPool.REMEDY, -1);
  private static final AdventureResult USED_CURE_ALL = ItemPool.get(ItemPool.ANCIENT_CURE_ALL, -1);

  private static final Pattern ID1_PATTERN = Pattern.compile("whicheffect=(\\d+)");
  private static final Pattern ID2_PATTERN = Pattern.compile("whichbuff=(\\d+)");

  public static final Map<String, String> EFFECT_SKILL = new HashMap<>();

  static {
    for (Entry<Integer, String> entry : EffectDatabase.defaultActions.entrySet()) {
      if (entry.getValue().startsWith("cast 1")) {
        String effectName = EffectDatabase.getEffectName(entry.getKey());
        String skillName = entry.getValue().substring(7);
        if (skillName.contains("|")) {
          skillName = skillName.substring(0, skillName.indexOf("|"));
        }
        UneffectRequest.EFFECT_SKILL.put(effectName, skillName);
      }
    }
  }

  public UneffectRequest(final AdventureResult effect) {
    super(
        UneffectRequest.isShruggable(effect.getEffectId())
            ? "charsheet.php"
            : UneffectRequest.isAsdon(effect.getEffectId()) ? "campground.php" : "uneffect.php");

    this.effect = effect;
    this.effectId = effect.getEffectId();
    this.isShruggable = UneffectRequest.isShruggable(this.effectId);
    this.isAsdon = UneffectRequest.isAsdon(effectId);
    this.needsCocoa = UneffectRequest.needsCocoa(this.effectId);
    this.isTimer = this.effectId >= EffectPool.TIMER1 && this.effectId <= EffectPool.TIMER10;

    if (this.isShruggable) {
      this.addFormField("action", "unbuff");
      this.addFormField("ajax", "1");
      this.addFormField("whichbuff", String.valueOf(this.effectId));
    } else if (this.isAsdon) {
      this.addFormField("preaction", "undrive");
      this.addFormField("ajax", "1");
    } else {
      this.addFormField("using", "Yep.");
      this.addFormField("whicheffect", String.valueOf(this.effectId));
      this.addFormField("pwd");
    }
  }

  @Override
  protected boolean retryOnTimeout() {
    return true;
  }

  public static final boolean isRemovable(final String effectName) {
    return UneffectRequest.isRemovable(EffectDatabase.getEffectId(effectName));
  }

  public static final boolean isRemovable(final int effectId) {
    // http://forums.kingdomofloathing.com/vb/showthread.php?t=195401
    //
    // The following effects should now no longer be removable by any means:
    // Goofball Withdrawal
    // Soul-Crushing Headache
    // Coated in Slime
    // Everything Looks Yellow
    // Everything Looks Blue
    // Everything Looks Red
    // Deep-Tainted Mind
    // Timer effects

    return switch (effectId) {
      case -1,
          // So, what about the following?
          EffectPool.CURSED_BY_RNG,
          EffectPool.FORM_OF_ROACH,
          EffectPool.SHAPE_OF_MOLE,
          EffectPool.FORM_OF_BIRD,
          // Known to be unremovable
          EffectPool.GOOFBALL_WITHDRAWAL,
          EffectPool.SOUL_CRUSHING_HEADACHE,
          EffectPool.COATED_IN_SLIME,
          EffectPool.EVERYTHING_LOOKS_YELLOW,
          EffectPool.EVERYTHING_LOOKS_BLUE,
          EffectPool.EVERYTHING_LOOKS_RED,
          EffectPool.EVERYTHING_LOOKS_GREEN,
          EffectPool.DEEP_TAINTED_MIND,
          EffectPool.SPIRIT_PARIAH,
          EffectPool.BORED_WITH_EXPLOSIONS,
          EffectPool.FEELING_QUEASY -> false;
      default -> true;
    };
  }

  public static final boolean isShruggable(final int effectId) {
    switch (effectId) {
      case EffectPool.TIMER1:
      case EffectPool.TIMER2:
      case EffectPool.TIMER3:
      case EffectPool.TIMER4:
      case EffectPool.TIMER5:
      case EffectPool.TIMER6:
      case EffectPool.TIMER7:
      case EffectPool.TIMER8:
      case EffectPool.TIMER9:
      case EffectPool.TIMER10:
      case EffectPool.JUST_THE_BEST_ANAPESTS:
      case EffectPool.REASSURED:
      case EffectPool.HARE_BRAINED:
      case EffectPool.RECORD_HUNGER:
      case EffectPool.DRUNK_AVUNCULAR:
      case EffectPool.SHRIEKING_WEASEL:
      case EffectPool.POWER_MAN:
      case EffectPool.LUCKY_STRUCK:
      case EffectPool.MINISTRATIONS_IN_THE_DARK:
      case EffectPool.SUPERDRIFTING:
      case EffectPool.CARTOGRAPHICALLY_CHARGED:
      case EffectPool.CARTOGRAPHICALLY_AWARE:
      case EffectPool.CARTOGRAPHICALLY_ROOTED:
        return true;
    }

    // Some songs are not AT buffs but are still shruggable
    if (EffectDatabase.isSong(effectId)) {
      return true;
    }

    String name = EffectDatabase.getEffectName(effectId);
    if (name == null) {
      return false;
    }
    int id = SkillDatabase.getSkillId(UneffectRequest.effectToSkill(name));
    return id != -1 && SkillDatabase.isBuff(id);
  }

  public static final boolean isShruggable(final String effectName) {
    int effectId = EffectDatabase.getEffectId(effectName);
    return UneffectRequest.isShruggable(effectId);
  }

  public static final boolean isAsdon(final int effectId) {
    return effectId >= EffectPool.OBNOXIOUSLY && effectId <= EffectPool.WATERPROOFLY;
  }

  public static final boolean needsCocoa(final String effectName) {
    return UneffectRequest.needsCocoa(EffectDatabase.getEffectId(effectName));
  }

  public static final boolean needsCocoa(final int effectId) {
    return switch (effectId) {
      case EffectPool.CURSE_OF_CLUMSINESS,
          EffectPool.CURSE_OF_DULLNESS,
          EffectPool.CURSE_OF_EXPOSURE,
          EffectPool.CURSE_OF_FORGETFULNESS,
          EffectPool.CURSE_OF_HOLLOWNESS,
          EffectPool.CURSE_OF_IMPOTENCE,
          EffectPool.CURSE_OF_LONELINESS,
          EffectPool.CURSE_OF_MISFORTUNE,
          EffectPool.CURSE_OF_SLUGGISHNESS,
          EffectPool.CURSE_OF_VULNERABILITY,
          EffectPool.CURSE_OF_WEAKNESS,
          EffectPool.TOUCHED_BY_A_GHOST,
          EffectPool.CHILLED_TO_THE_BONE,
          EffectPool.NAUSEATED -> true;
      default -> false;
    };
  }

  public static final boolean isRemovableIntrinsic(final int effectId) {
    if (EffectPool.CITIZEN_OF_A_ZONE == effectId) {
      return true;
    }
    return !"".equals(getUneffectSkill(effectId));
  }

  /**
   * Given the name of an effect, return the name of the skill that created that effect
   *
   * @param effectName The name of the effect
   * @return skill The name of the skill
   */
  public static final String effectToSkill(final String effectName) {
    for (Entry<String, String> entry : UneffectRequest.EFFECT_SKILL.entrySet()) {
      if (entry.getKey().equalsIgnoreCase(effectName)) {
        return entry.getValue();
      }
    }

    return null;
  }

  public static final String skillToEffect(final String skillName) {
    // Some skills can produce different effects depending on current effects or class
    int skillId = SkillDatabase.getSkillId(skillName);

    switch (skillId) {
      case SkillPool.SPIRIT_BOON:
        {
          TurtleBlessing blessing = KoLCharacter.getBlessingType();
          if (blessing == null) return "none";
          return switch (blessing) {
            case SHE_WHO_WAS -> EffectDatabase.getEffectName(EffectPool.BOON_OF_SHE_WHO_WAS);
            case STORM -> EffectDatabase.getEffectName(EffectPool.BOON_OF_THE_STORM_TORTOISE);
            case WAR -> EffectDatabase.getEffectName(EffectPool.BOON_OF_THE_WAR_SNAPPER);
          };
        }
      case SkillPool.SHE_WHO_WAS_BLESSING:
        return EffectDatabase.getEffectName(
            KoLCharacter.isTurtleTamer()
                ? EffectPool.BLESSING_OF_SHE_WHO_WAS
                : EffectPool.DISDAIN_OF_SHE_WHO_WAS);
      case SkillPool.STORM_BLESSING:
        return EffectDatabase.getEffectName(
            KoLCharacter.isTurtleTamer()
                ? EffectPool.BLESSING_OF_THE_STORM_TORTOISE
                : EffectPool.DISDAIN_OF_THE_STORM_TORTOISE);
      case SkillPool.WAR_BLESSING:
        return EffectDatabase.getEffectName(
            KoLCharacter.isTurtleTamer()
                ? EffectPool.BLESSING_OF_THE_WAR_SNAPPER
                : EffectPool.DISDAIN_OF_THE_WAR_SNAPPER);
      case SkillPool.TURTLE_POWER:
        {
          TurtleBlessing blessing = KoLCharacter.getBlessingType();
          if (blessing == null) return "none";
          return switch (blessing) {
            case SHE_WHO_WAS -> EffectDatabase.getEffectName(EffectPool.AVATAR_OF_SHE_WHO_WAS);
            case STORM -> EffectDatabase.getEffectName(EffectPool.AVATAR_OF_THE_STORM_TORTOISE);
            case WAR -> EffectDatabase.getEffectName(EffectPool.AVATAR_OF_THE_WAR_SNAPPER);
          };
        }
      case SkillPool.SHIELD_OF_THE_PASTALORD:
        return EffectDatabase.getEffectName(
            KoLCharacter.isPastamancer()
                ? EffectPool.SHIELD_OF_THE_PASTALORD
                : EffectPool.FLIMSY_SHIELD_OF_THE_PASTALORD);
      case SkillPool.BLOOD_SUGAR_SAUCE_MAGIC:
        return EffectDatabase.getEffectName(
            KoLCharacter.isSauceror()
                ? EffectPool.BLOOD_SUGAR_SAUCE_MAGIC
                : EffectPool.BLOOD_SUGAR_SAUCE_MAGIC_LITE);
      case SkillPool.SILENT_HUNTER:
        return EffectDatabase.getEffectName(
            KoLCharacter.isSealClubber()
                ? EffectPool.SILENT_HUNTING
                : EffectPool.NEARLY_SILENT_HUNTING);
    }

    // Handle remaining skills with a lookup

    for (Entry<String, String> entry : UneffectRequest.EFFECT_SKILL.entrySet()) {
      if (entry.getValue().equalsIgnoreCase(skillName)) {
        return entry.getKey();
      }
    }

    return null;
  }

  private static Set<Entry<String, Set<Integer>>> REMOVABLE_BY_SKILL;
  private static final Map<String, Set<Integer>> removeWithSkillMap = new LinkedHashMap<>();

  private static Set<Entry<Integer, Set<Integer>>> REMOVABLE_BY_ITEM;
  private static final Map<Integer, Set<Integer>> removeWithItemMap = new LinkedHashMap<>();

  public static final void reset() {
    Set<Integer> removableEffects;

    removableEffects = new HashSet<>();
    removeWithItemMap.put(ItemPool.ANTIDOTE, removableEffects);
    removableEffects.add(EffectPool.HARDLY_POISONED);
    removableEffects.add(EffectPool.MAJORLY_POISONED);
    removableEffects.add(EffectPool.A_LITTLE_BIT_POISONED);
    removableEffects.add(EffectPool.SOMEWHAT_POISONED);
    removableEffects.add(EffectPool.REALLY_QUITE_POISONED);

    removableEffects = new HashSet<>();
    removeWithItemMap.put(ItemPool.TINY_HOUSE, removableEffects);
    removableEffects.add(EffectPool.BEATEN_UP);
    removableEffects.add(EffectPool.CONFUSED);
    removableEffects.add(EffectPool.EMBARRASSED);
    removableEffects.add(EffectPool.SUNBURNED);
    removableEffects.add(EffectPool.WUSSINESS);

    removableEffects = new HashSet<>();
    removeWithItemMap.put(ItemPool.TEARS, removableEffects);
    removableEffects.add(EffectPool.BEATEN_UP);

    removableEffects = new HashSet<>();
    removeWithItemMap.put(ItemPool.TRIPPLES, removableEffects);
    removableEffects.add(EffectPool.BEATEN_UP);

    removableEffects = new HashSet<>();
    removeWithItemMap.put(ItemPool.HOT_DREADSYLVANIAN_COCOA, removableEffects);
    removableEffects.add(EffectPool.TOUCHED_BY_A_GHOST);
    removableEffects.add(EffectPool.CHILLED_TO_THE_BONE);
    removableEffects.add(EffectPool.NAUSEATED);
    removableEffects.add(EffectPool.CURSE_OF_HOLLOWNESS);
    removableEffects.add(EffectPool.CURSE_OF_VULNERABILITY);
    removableEffects.add(EffectPool.CURSE_OF_EXPOSURE);
    removableEffects.add(EffectPool.CURSE_OF_IMPOTENCE);
    removableEffects.add(EffectPool.CURSE_OF_DULLNESS);
    removableEffects.add(EffectPool.CURSE_OF_WEAKNESS);
    removableEffects.add(EffectPool.CURSE_OF_SLUGGISHNESS);
    removableEffects.add(EffectPool.CURSE_OF_FORGETFULNESS);
    removableEffects.add(EffectPool.CURSE_OF_MISFORTUNE);
    removableEffects.add(EffectPool.CURSE_OF_CLUMSINESS);
    removableEffects.add(EffectPool.CURSE_OF_LONELINESS);

    UneffectRequest.REMOVABLE_BY_ITEM = removeWithItemMap.entrySet();

    removableEffects = new HashSet<>();
    removeWithSkillMap.put("Tongue of the Walrus", removableEffects);
    removableEffects.add(EffectPool.AXE_WOUND);
    removableEffects.add(EffectPool.BEATEN_UP);
    removableEffects.add(EffectPool.GRILLED);
    removableEffects.add(EffectPool.HALF_EATEN_BRAIN);
    removableEffects.add(EffectPool.MISSING_FINGERS);
    removableEffects.add(EffectPool.SUNBURNED);

    removableEffects = new HashSet<>();
    removeWithSkillMap.put("Disco Nap", removableEffects);
    removableEffects.add(EffectPool.CONFUSED);
    removableEffects.add(EffectPool.EMBARRASSED);
    removableEffects.add(EffectPool.SLEEPY);
    removableEffects.add(EffectPool.SUNBURNED);
    removableEffects.add(EffectPool.WUSSINESS);
    removableEffects.add(EffectPool.DISAVOWED);
    if (KoLCharacter.hasSkill(SkillPool.ADVENTURER_OF_LEISURE)) {
      removableEffects.add(EffectPool.AFFRONTED_DECENCY);
      removableEffects.add(EffectPool.APATHY);
      removableEffects.add(EffectPool.CONSUMED_BY_FEAR);
      removableEffects.add(EffectPool.CUNCTATITIS);
      removableEffects.add(EffectPool.EASILY_EMBARRASSED);
      removableEffects.add(EffectPool.EXISTENTIAL_TORMENT);
      removableEffects.add(EffectPool.LIGHT_HEADED);
      removableEffects.add(EffectPool.N_SPATIAL_VISION);
      removableEffects.add(EffectPool.PRESTIDIGYSFUNCTION);
      removableEffects.add(EffectPool.RAINY_SOUL_MIASMA);
      removableEffects.add(EffectPool.SOCIALISMYDIA);
      removableEffects.add(EffectPool.TENUOUS_GRIP_ON_REALITY);
      removableEffects.add(EffectPool.TETANUS);
      removableEffects.add(EffectPool.THE_COLORS);
      removableEffects.add(EffectPool.THE_DISEASE);
    }

    // If it can be removed by Shake It Off, it can also be removed by Hot Tub
    removableEffects = new HashSet<>();
    removeWithSkillMap.put("Shake It Off", removableEffects);
    removableEffects.add(EffectPool.A_REVOLUTION_IN_YOUR_MOUTH);
    removableEffects.add(EffectPool.AFFRONTED_DECENCY);
    removableEffects.add(EffectPool.ALL_COVERED_IN_WHATSIT);
    removableEffects.add(EffectPool.APATHY);
    removableEffects.add(EffectPool.AXE_WOUND);
    removableEffects.add(EffectPool.BARKING_DOGS);
    removableEffects.add(EffectPool.BEATEN_UP);
    removableEffects.add(EffectPool.BEER_IN_YOUR_SHOES);
    removableEffects.add(EffectPool.BLOODY_HAND);
    removableEffects.add(EffectPool.CONFUSED);
    removableEffects.add(EffectPool.CONSUMED_BY_FEAR);
    removableEffects.add(EffectPool.CORRODED_WEAPON);
    removableEffects.add(EffectPool.CUNCTATITIS);
    removableEffects.add(EffectPool.DEADENED_PALATE);
    removableEffects.add(EffectPool.EASILY_EMBARRASSED);
    removableEffects.add(EffectPool.EMBARRASSED);
    removableEffects.add(EffectPool.EXISTENTIAL_TORMENT);
    removableEffects.add(EffectPool.FLARED_NOSTRILS);
    removableEffects.add(EffectPool.GRILLED);
    removableEffects.add(EffectPool.HALF_EATEN_BRAIN);
    removableEffects.add(EffectPool.HERNIA);
    removableEffects.add(EffectPool.LIGHT_HEADED);
    removableEffects.add(EffectPool.MISSING_FINGERS);
    removableEffects.add(EffectPool.N_SPATIAL_VISION);
    removableEffects.add(EffectPool.NATURAL_1);
    removableEffects.add(EffectPool.ONCE_CURSED);
    removableEffects.add(EffectPool.PRESTIDIGYSFUNCTION);
    removableEffects.add(EffectPool.RAINY_SOUL_MIASMA);
    removableEffects.add(EffectPool.SLEEPY);
    removableEffects.add(EffectPool.SOCIALISMYDIA);
    removableEffects.add(EffectPool.STRANGULATED);
    removableEffects.add(EffectPool.SUNBURNED);
    removableEffects.add(EffectPool.TANGLED_UP);
    removableEffects.add(EffectPool.TEMPORARY_BLINDNESS);
    removableEffects.add(EffectPool.TENUOUS_GRIP_ON_REALITY);
    removableEffects.add(EffectPool.TETANUS);
    removableEffects.add(EffectPool.TOAD_IN_THE_HOLE);
    removableEffects.add(EffectPool.TURNED_INTO_A_SKELETON);
    removableEffects.add(EffectPool.THE_COLORS);
    removableEffects.add(EffectPool.THE_DISEASE);
    removableEffects.add(EffectPool.THRICE_CURSED);
    removableEffects.add(EffectPool.TWICE_CURSED);
    removableEffects.add(EffectPool.WUSSINESS);

    removableEffects = new HashSet<>();
    removeWithSkillMap.put("Pep Talk", removableEffects);
    removableEffects.add(EffectPool.OVERCONFIDENT);

    removableEffects = new HashSet<>();
    removeWithSkillMap.put("Blood Sugar Sauce Magic", removableEffects);
    removableEffects.add(EffectPool.BLOOD_SUGAR_SAUCE_MAGIC_LITE);
    removableEffects.add(EffectPool.BLOOD_SUGAR_SAUCE_MAGIC);

    removableEffects = new HashSet<>();
    removeWithSkillMap.put("Spirit of Nothing", removableEffects);
    removableEffects.add(EffectPool.SPIRIT_OF_CAYENNE);
    removableEffects.add(EffectPool.SPIRIT_OF_PEPPERMINT);
    removableEffects.add(EffectPool.SPIRIT_OF_GARLIC);
    removableEffects.add(EffectPool.SPIRIT_OF_WORMWOOD);
    removableEffects.add(EffectPool.SPIRIT_OF_BACON_GREASE);

    removableEffects = new HashSet<>();
    removeWithSkillMap.put("Iron Palm Technique", removableEffects);
    removableEffects.add(EffectPool.IRON_PALMS);

    removableEffects = new HashSet<>();
    removeWithSkillMap.put("Wolf Form", removableEffects);
    removableEffects.add(EffectPool.WOLF_FORM);

    removableEffects = new HashSet<>();
    removeWithSkillMap.put("Mist Form", removableEffects);
    removableEffects.add(EffectPool.MIST_FORM);

    removableEffects = new HashSet<>();
    removeWithSkillMap.put("Flock of Bats Form", removableEffects);
    removableEffects.add(EffectPool.BATS_FORM);

    removableEffects = new HashSet<>();
    removeWithSkillMap.put("Absorb Cowrruption", removableEffects);
    removableEffects.add(EffectPool.COWRRUPTION);

    removableEffects = new HashSet<>();
    removeWithSkillMap.put("Gelatinous Reconstruction", removableEffects);
    removableEffects.add(EffectPool.BEATEN_UP);

    UneffectRequest.REMOVABLE_BY_SKILL = removeWithSkillMap.entrySet();
  }

  static {
    UneffectRequest.reset();
  }

  public static void removeEffectsWithItem(final int itemId) {
    Set<Integer> effects = UneffectRequest.removeWithItemMap.get(itemId);
    UneffectRequest.removeEffects(effects);
  }

  public static void removeEffectsWithSkill(final int skillId) {
    String skillName = SkillDatabase.getSkillName(skillId);

    if (skillName == null) {
      return;
    }

    Set<Integer> effects = UneffectRequest.removeWithSkillMap.get(skillName);
    UneffectRequest.removeEffects(effects);
  }

  private static void removeEffects(final Set<Integer> effects) {
    if (effects == null) {
      return;
    }

    for (Integer effectId : effects) {
      AdventureResult effect = EffectPool.get(effectId);
      KoLConstants.activeEffects.remove(effect);
    }
  }

  public static String getUneffectSkill(final int effectId) {
    Integer effect = Integer.valueOf(effectId);

    for (Entry<String, Set<Integer>> removable : UneffectRequest.REMOVABLE_BY_SKILL) {
      Set<Integer> removables = removable.getValue();

      if (!removables.contains(effectId)) {
        continue;
      }

      String skillName = removable.getKey();

      if (KoLCharacter.inGLover() && !KoLCharacter.hasGs(skillName)) {
        continue;
      }

      if (KoLCharacter.hasSkill(skillName)) {
        return "cast " + skillName;
      }
    }

    return "";
  }

  public static String getUneffectSkill(final String effectName) {
    int effectId = EffectDatabase.getEffectId(effectName);
    return UneffectRequest.getUneffectSkill(effectId);
  }

  private String getAction() {
    Boolean canRemoveWithHotTub = false;
    String name = this.effect.getName();
    int effectId = this.effectId;

    // If there's an action defined in your mood, use it.

    String action = MoodManager.getDefaultAction("gain_effect", name);
    String skillName = null;

    if (action.startsWith("cast ")) {
      skillName = action.substring(5);
    } else if (action.startsWith("skill ")) {
      skillName = action.substring(6);
    }

    if (skillName != null && !KoLCharacter.hasSkill(skillName)) {
      action = "";
    }

    if (!action.equals("") && !action.startsWith("uneffect ")) {
      KoLmafia.updateDisplay(name + " will be removed via pre-defined trigger (" + action + ")...");

      return action;
    }

    // If it's shruggable, then the cleanest way is to just shrug it.

    if (this.isShruggable || this.isAsdon) {
      return "uneffect [" + effectId + "]";
    }

    // See if it can be removed by a skill.

    for (Entry<String, Set<Integer>> removable : UneffectRequest.REMOVABLE_BY_SKILL) {
      Set<Integer> removables = removable.getValue();

      if (!removables.contains(effectId)) {
        continue;
      }

      skillName = removable.getKey();

      // If Shake It Off can remove it, so can Hot Tub
      if (skillName.equals("Shake It Off")) {
        canRemoveWithHotTub = true;
      }

      if (KoLCharacter.hasSkill(skillName)) {
        if (!KoLCharacter.inGLover() || KoLCharacter.hasGs(skillName)) {
          KoLmafia.updateDisplay(name + " will be removed by skill " + skillName + "...");

          return "cast " + skillName;
        }
      }
    }

    // Can we use Hot Tub to remove it, Hot Tubs available and allowed to use them ?
    // In Hardcore or Ronin, antidotes are shorter of supply, so use Hot Tub in preference
    if (canRemoveWithHotTub
        && InventoryManager.getCount(ItemPool.VIP_LOUNGE_KEY) > 0
        && !KoLCharacter.inBadMoon()
        && Preferences.getInteger("_hotTubSoaks") < 5
        && Preferences.getBoolean("uneffectWithHotTub")
        && !KoLCharacter.canInteract()) {
      KoLmafia.updateDisplay(name + " will be removed by hot tub...");
      return "hottub";
    }

    // See if it can be removed by an item.

    boolean hasRemedy =
        InventoryManager.hasItem(ItemPool.REMEDY)
            || InventoryManager.hasItem(ItemPool.ANCIENT_CURE_ALL);

    for (Entry<Integer, Set<Integer>> removable : UneffectRequest.REMOVABLE_BY_ITEM) {
      Set<Integer> removables = removable.getValue();

      if (!removables.contains(effectId)) {
        continue;
      }

      int itemId = removable.getKey().intValue();
      String itemName = ItemDatabase.getItemName(itemId);

      if (InventoryManager.hasItem(itemId)
          || InventoryManager.canUseNPCStores(itemId)
          || InventoryManager.canUseCoinmasters(itemId)
          || (this.needsCocoa || !hasRemedy)
              && (InventoryManager.canUseMall(itemId)
                  || InventoryManager.canUseClanStash(itemId))) {
        KoLmafia.updateDisplay(name + " will be removed by item " + itemName + "...");

        return "use 1 [" + itemId + "]";
      }
    }

    // Default to using a remedy.

    KoLmafia.updateDisplay(
        name + " has no specific item or skill for removal.  Trying generics...");

    return "uneffect [" + effectId + "]";
  }

  @Override
  public void run() {
    int index = KoLConstants.activeEffects.indexOf(this.effect);
    if (index == -1) {
      KoLmafia.updateDisplay(MafiaState.ERROR, "You don't have that effect.");
      return;
    }

    AdventureResult effect = KoLConstants.activeEffects.get(index);

    if (!UneffectRequest.isRemovable(this.effectId)) {
      KoLmafia.updateDisplay(MafiaState.ERROR, effect.getName() + " is unremovable.");
      return;
    }

    if (effect.getCount() == Integer.MAX_VALUE && !isRemovableIntrinsic(this.effectId)) {
      KoLmafia.updateDisplay(
          MafiaState.ERROR, effect.getName() + " is intrinsic and cannot be removed.");
      return;
    }

    if (!UneffectRequest.currentEffectRemovals.contains(effect)) {
      String action = this.getAction();

      if (!action.equals("")
          && !action.startsWith("uneffect")
          && !action.startsWith("shrug")
          && !action.startsWith("remedy")) {
        UneffectRequest.currentEffectRemovals.add(effect);
        KoLmafiaCLI.DEFAULT_SHELL.executeLine(action);
        UneffectRequest.currentEffectRemovals.remove(effect);
        return;
      }
    }

    // If you either have cocoa in inventory or allow purchasing it
    // via coinmaster or mall, we will have done so above.
    if (this.needsCocoa) {
      KoLmafia.updateDisplay(
          MafiaState.ERROR,
          effect.getName() + " can be removed only with hot Dreadsylvanian cocoa.");
      return;
    }

    if (this.isTimer) {
      KoLmafia.updateDisplay("Canceling your timer...");
    } else if (this.isShruggable) {
      KoLmafia.updateDisplay("Shrugging off your buff...");
    } else if (this.isAsdon) {
      KoLmafia.updateDisplay("No longer driving...");
    } else if (InventoryManager.hasItem(ItemPool.ANCIENT_CURE_ALL)) {
      KoLmafia.updateDisplay("Using ancient cure-all...");
    } else if (InventoryManager.retrieveItem(ItemPool.REMEDY)) {
      KoLmafia.updateDisplay("Using soft green whatever...");
    } else {
      return;
    }

    super.run();
  }

  @Override
  public void processResults() {
    // Using a remedy no longer says "Effect removed." If you have
    // more remedies available, it gives a new list, not containing
    // the effect. If you have no more remedies, it says "You don't
    // have any more green fluffy antidote echo drops, or whatever
    // they're called."

    if (this.responseText == null) {
      // What's wrong?
      return;
    }

    KoLConstants.activeEffects.remove(this.effect);

    // If you lose Inigo's, what you can craft changes
    if (this.effectId == EffectPool.INIGOS || this.effectId == EffectPool.CRAFT_TEA) {
      ConcoctionDatabase.setRefreshNeeded(true);
    }

    // If Gar-ish is gained or lost and autoGarish isn't set, benefit of Lasagna changes
    if (this.effectId == EffectPool.GARISH && !Preferences.getBoolean("autoGarish")) {
      ConcoctionDatabase.setRefreshNeeded(true);
    }

    KoLmafia.updateDisplay(this.effect.getName() + " removed.");
  }

  public static final boolean registerRequest(final String location) {
    if (!location.startsWith("uneffect.php") && !location.startsWith("charsheet.php")) {
      return false;
    }

    if (!location.contains("?")) {
      return true;
    }

    Matcher idMatcher =
        location.startsWith("uneffect.php")
            ? UneffectRequest.ID1_PATTERN.matcher(location)
            : UneffectRequest.ID2_PATTERN.matcher(location);

    if (!idMatcher.find()) {
      return true;
    }

    int id = StringUtilities.parseInt(idMatcher.group(1));
    String name = EffectDatabase.getEffectName(id);

    if (UneffectRequest.isRemovable(id) && location.startsWith("uneffect")) {
      if (InventoryManager.getCount(ItemPool.ANCIENT_CURE_ALL) > 0) {
        ResultProcessor.processResult(UneffectRequest.USED_CURE_ALL);
      } else {
        ResultProcessor.processResult(UneffectRequest.USED_REMEDY);
      }
    }

    RequestLogger.updateSessionLog();
    RequestLogger.updateSessionLog("uneffect " + name);
    return true;
  }
}
