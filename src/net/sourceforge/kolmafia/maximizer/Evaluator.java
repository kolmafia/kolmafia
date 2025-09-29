package net.sourceforge.kolmafia.maximizer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.FamiliarData;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLCharacter.TurtleBlessing;
import net.sourceforge.kolmafia.KoLCharacter.TurtleBlessingLevel;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLConstants.WeaponType;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.Modeable;
import net.sourceforge.kolmafia.ModifierType;
import net.sourceforge.kolmafia.Modifiers;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.RestrictedItemType;
import net.sourceforge.kolmafia.SpecialOutfit;
import net.sourceforge.kolmafia.equipment.Slot;
import net.sourceforge.kolmafia.equipment.SlotSet;
import net.sourceforge.kolmafia.modifiers.BitmapModifier;
import net.sourceforge.kolmafia.modifiers.BooleanModifier;
import net.sourceforge.kolmafia.modifiers.DerivedModifier;
import net.sourceforge.kolmafia.modifiers.DoubleModifier;
import net.sourceforge.kolmafia.modifiers.DoubleModifierCollection;
import net.sourceforge.kolmafia.modifiers.MultiStringModifier;
import net.sourceforge.kolmafia.modifiers.StringModifier;
import net.sourceforge.kolmafia.objectpool.EffectPool;
import net.sourceforge.kolmafia.objectpool.FamiliarPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.objectpool.SkillPool;
import net.sourceforge.kolmafia.persistence.AdventureDatabase;
import net.sourceforge.kolmafia.persistence.EquipmentDatabase;
import net.sourceforge.kolmafia.persistence.FamiliarDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase.FoldGroup;
import net.sourceforge.kolmafia.persistence.ItemFinder;
import net.sourceforge.kolmafia.persistence.ItemFinder.Match;
import net.sourceforge.kolmafia.persistence.ModifierDatabase;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.EquipmentRequest;
import net.sourceforge.kolmafia.request.StandardRequest;
import net.sourceforge.kolmafia.session.EquipmentManager;
import net.sourceforge.kolmafia.session.InventoryManager;
import net.sourceforge.kolmafia.utilities.StringUtilities;

@SuppressWarnings("incomplete-switch")
public class Evaluator {
  public boolean failed;
  boolean exceeded;
  private Evaluator tiebreaker;
  private final DoubleModifierCollection weight = new DoubleModifierCollection();
  private Map<DoubleModifier, Double> min;
  private Map<DoubleModifier, Double> max;
  private double totalMin, totalMax;
  private int dump = 0;
  private int clownosity = 0;
  private int raveosity = 0;
  private int surgeonosity = 0;
  private int stinkycheese = 0;
  private int beeosity = 2;
  private final EnumSet<BooleanModifier> booleanMask = EnumSet.noneOf(BooleanModifier.class);
  private final Set<BooleanModifier> booleanValue = EnumSet.noneOf(BooleanModifier.class);
  private final List<FamiliarData> familiars = new ArrayList<>();
  private final List<FamiliarData> carriedFamiliars = new ArrayList<>();
  private int carriedFamiliarsNeeded = 0;
  private boolean cardNeeded = false;
  private final Map<Modeable, Boolean> modeablesNeeded = Modeable.getBooleanMap();

  // Some modeables are forced based on certain expressions appearing in a maximize call
  // For example, if you request "sea" the Crown of Ed will always pick fish. This does pose
  // an issue if the maximizer would choose the SCUBA gear to provide water-breathing, as it would
  // not consider a different mode for the Crown. e.g. "maximize sea, ml" would not consider the
  // "bear" mode for the hat. Something for someone to fix in the future.
  private final Map<Modeable, String> forcedModeables = Modeable.getStringMap(m -> "");

  /** if slots[i] >= 0 then equipment of type i can be considered for maximization */
  private final EnumMap<Slot, Integer> slots = new EnumMap<>(Slot.class);

  private String weaponType = null;
  private int hands = 0;
  int melee = 0; // +/-2 or higher: require, +/-1: disallow other type
  private boolean effective = false;
  private boolean requireClub = false;
  private boolean requireShield = false;
  private boolean requireUtensil = false;
  private boolean requireSword = false;
  private boolean requireKnife = false;
  private boolean requireAccordion = false;
  private boolean noTiebreaker = false;
  private boolean current =
      !KoLCharacter.canInteract() || Preferences.getBoolean("maximizerAlwaysCurrent");
  private final Set<String> posOutfits = new HashSet<>();
  private final Set<String> negOutfits = new HashSet<>();
  private final Set<AdventureResult> posEquip = new HashSet<>();
  private final Set<AdventureResult> negEquip = new HashSet<>();
  private final Map<AdventureResult, Double> bonuses = new HashMap<>();
  private final List<BonusFunction> bonusFunc = new ArrayList<>();

  record BonusFunction(Function<AdventureResult, Double> bonusFunction, Double weight) {}

  private static final Pattern MUS_EXP_PERC_PATTERN =
      Pattern.compile("^mus(cle)? exp(erience)? perc(ent(age)?)?");
  private static final Pattern MUS_EXP_PATTERN = Pattern.compile("^mus(cle)? exp(erience)?");
  private static final Pattern MUS_PERC_PATTERN = Pattern.compile("^mus(cle)? perc(ent(age)?)?");
  private static final Pattern MYS_EXP_PERC_PATTERN =
      Pattern.compile("^mys(t(ical(ity)?)?)? exp(erience)? perc(ent(age)?)?");
  private static final Pattern MYS_EXP_PATTERN =
      Pattern.compile("^mys(t(ical(ity)?)?)? exp(erience)?");
  private static final Pattern MYS_PERC_PATTERN =
      Pattern.compile("^mys(t(ical(ity)?)?)? perc(ent(age)?)?");
  private static final Pattern MOX_EXP_PERC_PATTERN =
      Pattern.compile("^mox(ie)? exp(erience)? perc(ent(age)?)?");
  private static final Pattern MOX_EXP_PATTERN = Pattern.compile("^mox(ie)? exp(erience)?");
  private static final Pattern MOX_PERC_PATTERN = Pattern.compile("^mox(ie)? perc(ent(age)?)?");
  private static final String TIEBREAKER =
      "1 familiar weight, 1 familiar experience, 1 initiative, 5 exp, 1 item, 1 meat, 0.1 DA 1000 max, 1 DR, 0.5 all res, -10 mana cost, 1.0 mus, 0.5 mys, 1.0 mox, 1.5 mainstat, 1 HP, 1 MP, 1 weapon damage, 1 ranged damage, 1 spell damage, 1 cold damage, 1 hot damage, 1 sleaze damage, 1 spooky damage, 1 stench damage, 1 cold spell damage, 1 hot spell damage, 1 sleaze spell damage, 1 spooky spell damage, 1 stench spell damage, -1 fumble, 1 HP regen max, 3 MP regen max, 1 critical hit percent, 0.1 food drop, 0.1 booze drop, 0.1 hat drop, 0.1 weapon drop, 0.1 offhand drop, 0.1 shirt drop, 0.1 pants drop, 0.1 accessory drop, 1 DB combat damage, 0.1 sixgun damage";
  private static final Pattern KEYWORD_PATTERN =
      Pattern.compile(
          "\\G\\s*(\\+|-|)([\\d.]*)\\s*(\"[^\"]+\"|(?:[^-+,0-9]|(?<! )[-+0-9])+),?\\s*");
  // Groups: 1=sign 2=weight 3=keyword

  // Equipment slots, that aren't the primary slot of any item type,
  // that are repurposed here (rather than making the array bigger).
  // Watches have to be handled specially because only one can be
  // used - otherwise, they'd fill up the list, leaving no room for
  // any non-watches to put in the other two acc slots.
  // 1-handed weapons have to be ranked separately due to the following
  // possibility: all of your best weapons are 2-hand, but you've got
  // a really good off-hand, better than any weapon.  There would
  // otherwise be no suitable weapons to go with that off-hand.
  static final Slot OFFHAND_MELEE = Slot.ACCESSORY2;
  static final Slot OFFHAND_RANGED = Slot.ACCESSORY3;
  static final Slot WEAPON_1H = Slot.STICKER3;

  // Slots starting with EquipmentSlot.ALL_SLOTS are equipment
  // for other familiars being considered.

  private static int relevantSkill(int skillId) {
    return KoLCharacter.hasSkill(skillId) ? 1 : 0;
  }

  private int relevantFamiliar(int id) {
    if (KoLCharacter.getFamiliar().getId() == id) {
      return 1;
    }
    for (FamiliarData familiar : this.familiars) {
      if (familiar.getId() == id) {
        return 1;
      }
    }
    return 0;
  }

  private int maxUseful(Slot slot) {
    return switch (slot) {
      case /* Evaluator.WEAPON_1H */ STICKER3 -> 1
          + relevantSkill(SkillPool.DOUBLE_FISTED_SKULL_SMASHING)
          + this.relevantFamiliar(FamiliarPool.HAND);
      case OFFHAND -> 1 + this.relevantFamiliar(FamiliarPool.LEFT_HAND);
      case ACCESSORY1 -> 3;
      case FAMILIAR ->
      // Familiar items include weapons, hats and pants, make sure we have enough to consider for
      // other slots
      1
          + this.relevantFamiliar(FamiliarPool.SCARECROW)
          + this.relevantFamiliar(FamiliarPool.HAND)
          + this.relevantFamiliar(FamiliarPool.HATRACK);
      default -> 1;
    };
  }

  private static Slot toUseSlot(Slot slot) {
    return switch (slot) {
      case /* Evaluator.OFFHAND_MELEE */ ACCESSORY2, /* Evaluator.OFFHAND_RANGED */
          ACCESSORY3 -> Slot.OFFHAND;
      case /* Evaluator.WEAPON_1H */ STICKER3 -> Slot.WEAPON;
      default -> slot;
    };
  }

  private Evaluator() {
    this.totalMin = Double.NEGATIVE_INFINITY;
    this.totalMax = Double.POSITIVE_INFINITY;
  }

  Evaluator(String expr) {
    this();

    Evaluator tiebreaker = new Evaluator();
    this.tiebreaker = tiebreaker;
    tiebreaker.min = new EnumMap<>(DoubleModifier.class);
    tiebreaker.max = new EnumMap<>(DoubleModifier.class);
    for (var mod : DoubleModifier.DOUBLE_MODIFIERS) {
      tiebreaker.min.put(mod, Double.NEGATIVE_INFINITY);
      tiebreaker.max.put(mod, Double.POSITIVE_INFINITY);
    }
    tiebreaker.parse(Evaluator.TIEBREAKER);

    this.min = new EnumMap<>(tiebreaker.min);
    this.max = new EnumMap<>(tiebreaker.max);
    this.parse(expr);
  }

  private void parse(String expr) {
    expr = expr.trim().toLowerCase();
    Matcher m = KEYWORD_PATTERN.matcher(expr);
    boolean hadFamiliar = false;
    boolean forceCurrent = false;
    int pos = 0;
    DoubleModifier index = null;

    int equipBeeosity = 0;
    int outfitBeeosity = 0;

    while (pos < expr.length()) {
      if (!m.find()) {
        KoLmafia.updateDisplay(MafiaState.ERROR, "Unable to interpret: " + expr.substring(pos));
        return;
      }
      pos = m.end();
      double weight =
          StringUtilities.parseDouble(
              m.end(2) == m.start(2) ? m.group(1) + "1" : m.group(1) + m.group(2));

      String keyword = m.group(3).trim();
      if (keyword.startsWith("\"") && keyword.endsWith("\"")) {
        keyword = keyword.substring(1, keyword.length() - 1).trim();
      }
      if (keyword.equals("min")) {
        if (index != null) {
          this.min.put(index, weight);
        } else {
          this.totalMin = weight;
        }
        continue;
      }

      if (keyword.equals("max")) {
        if (index != null) {
          this.max.put(index, weight);
        } else {
          this.totalMax = weight;
        }
        continue;
      }

      if (keyword.equals("dump")) {
        this.dump = (int) weight;
        continue;
      }

      if (keyword.startsWith("hand")) {
        this.hands = (int) weight;
        if (this.hands >= 2) {
          // this.slots[ EquipmentManager.OFFHAND ] = -1;
        }
        continue;
      }

      if (keyword.startsWith("tie")) {
        this.noTiebreaker = weight < 0.0;
        continue;
      }

      if (keyword.startsWith("current")) {
        this.current = weight > 0.0;
        forceCurrent = true;
        continue;
      }

      if (keyword.startsWith("type ")) {
        this.weaponType = keyword.substring(5).trim();
        continue;
      }

      if (keyword.equals("club")) {
        this.requireClub = weight > 0.0;
        continue;
      }

      if (keyword.equals("shield")) {
        this.requireShield = weight > 0.0;
        forcedModeables.put(Modeable.UMBRELLA, "forward-facing");
        this.hands = 1;
        continue;
      }

      if (keyword.equals("utensil")) {
        this.requireUtensil = weight > 0.0;
        continue;
      }
      if (keyword.equals("sword")) {
        this.requireSword = weight > 0.0;
        continue;
      }

      if (keyword.equals("knife")) {
        this.requireKnife = weight > 0.0;
        continue;
      }

      if (keyword.equals("accordion")) {
        this.requireAccordion = weight > 0.0;
        continue;
      }

      if (keyword.equals("melee")) {
        this.melee = (int) (weight * 2.0);
        continue;
      }

      if (keyword.equals("effective")) {
        this.effective = weight > 0.0;
        continue;
      }

      if (keyword.equals("empty")) {
        for (var slot : SlotSet.ALL_SLOTS) {
          this.slots.merge(
              slot,
              ((int) weight)
                  * (EquipmentManager.getEquipment(slot).equals(EquipmentRequest.UNEQUIP) ? 1 : -1),
              Integer::sum);
        }
        continue;
      }

      if (keyword.equals("clownosity")) {
        // If no weight specified, assume 100%
        this.clownosity = (m.end(2) == m.start(2)) ? 100 : (int) weight * 25;
        continue;
      }

      if (keyword.equals("raveosity")) {
        // If no weight specified, assume 7
        this.raveosity = (m.end(2) == m.start(2)) ? 7 : (int) weight;
        continue;
      }

      if (keyword.equals("surgeonosity")) {
        // If no weight specified, assume 5
        this.surgeonosity = (m.end(2) == m.start(2)) ? 5 : (int) weight;
        continue;
      }

      if (keyword.equals("beeosity")) {
        this.beeosity = (int) weight;
        continue;
      }

      if (keyword.equals("stinkycheese") || keyword.equals("stinky cheese")) {
        this.stinkycheese = (int) weight;
        continue;
      }

      if (keyword.equals("sea")) {
        var adventureUnderwater =
            EnumSet.of(BooleanModifier.ADVENTURE_UNDERWATER, BooleanModifier.UNDERWATER_FAMILIAR);
        this.booleanMask.addAll(adventureUnderwater);
        this.booleanValue.addAll(adventureUnderwater);
        index = null;
        // Force Crown of Ed to Fish
        forcedModeables.put(Modeable.EDPIECE, "fish");
        continue;
      }

      if (keyword.startsWith("equip ")) {
        AdventureResult match =
            ItemFinder.getFirstMatchingItem(keyword.substring(6).trim(), Match.EQUIP);
        if (match == null) {
          return;
        }
        if (weight > 0.0) {
          this.posEquip.add(match);
          equipBeeosity += KoLCharacter.getBeeosity(match.getName());
        } else {
          this.negEquip.add(match);
        }
        continue;
      }

      if (keyword.startsWith("bonus ")) {
        AdventureResult match =
            ItemFinder.getFirstMatchingItem(keyword.substring(6).trim(), Match.EQUIP);
        if (match == null) {
          return;
        }
        this.bonuses.put(match, weight);
        continue;
      }

      if (keyword.startsWith("letter")) {
        keyword = keyword.substring(6).trim();
        if (keyword.isEmpty()) { // no keyword counts letters
          this.bonusFunc.add(new BonusFunction(LetterBonus::letterBonus, weight));
        } else {
          String finalKeyword = keyword;
          this.bonusFunc.add(
              new BonusFunction(ar -> LetterBonus.letterBonus(ar, finalKeyword), weight));
        }
        continue;
      }

      if (keyword.equals("number")) {
        this.bonusFunc.add(new BonusFunction(LetterBonus::numberBonus, weight));
        continue;
      }

      if (keyword.equals("plumber")) {
        if (!KoLCharacter.isPlumber()) {
          KoLmafia.updateDisplay(MafiaState.ERROR, "You are not a Plumber");
          return;
        }
        // Pick a tool that matches your prime stat
        AdventureResult item = pickPlumberTool(KoLCharacter.getPrimeIndex());
        if (item == null) {
          // Otherwise, pick best available tool
          // You are guaranteed to have work boots, at least
          item = pickPlumberTool(-1);
        }
        this.posEquip.add(item);
        continue;
      }

      if (keyword.equals("cold plumber")) {
        if (!KoLCharacter.isPlumber()) {
          KoLmafia.updateDisplay(MafiaState.ERROR, "You are not a Plumber");
          return;
        }
        // Mysticality plumber item
        AdventureResult item1 = pickPlumberTool(1);
        if (item1 == null) {
          KoLmafia.updateDisplay(MafiaState.ERROR, "You don't have an appropriate flower to wield");
          return;
        }
        AdventureResult item2 = ItemPool.get(ItemPool.FROSTY_BUTTON);
        this.posEquip.add(item1);
        this.posEquip.add(item2);
        continue;
      }

      if (keyword.startsWith("outfit")) {
        keyword = keyword.substring(6).trim();
        if (keyword.isEmpty()) { // allow "+outfit" to mean "keep the current outfit on"
          keyword = KoLCharacter.currentStringModifier(StringModifier.OUTFIT);
        }
        SpecialOutfit outfit = EquipmentManager.getMatchingOutfit(keyword);
        if (outfit == null || outfit.getOutfitId() <= 0) {
          KoLmafia.updateDisplay(MafiaState.ERROR, "Unknown or custom outfit: " + keyword);
          return;
        }
        if (weight > 0.0) {
          this.posOutfits.add(outfit.getName());
          int bees = 0;
          AdventureResult[] pieces = outfit.getPieces();
          for (AdventureResult piece : pieces) {
            bees += KoLCharacter.getBeeosity(piece.getName());
          }
          outfitBeeosity = Math.max(outfitBeeosity, bees);
        } else {
          this.negOutfits.add(outfit.getName());
        }
        continue;
      }

      if (keyword.startsWith("switch ")) {
        if (KoLCharacter.inPokefam()) {
          continue;
        }
        keyword = keyword.substring(7).trim();
        int id = FamiliarDatabase.getFamiliarId(keyword);
        if (id == -1) {
          KoLmafia.updateDisplay(MafiaState.ERROR, "Unknown familiar: " + keyword);
          return;
        }
        if (hadFamiliar && weight < 0.0) continue;
        FamiliarData fam = KoLCharacter.usableFamiliar(id);
        if (fam == null && weight > 1.0) { // Allow a familiar to be faked for testing
          fam = new FamiliarData(id);
          fam.setWeight((int) weight);
        }
        hadFamiliar = fam != null;
        if (fam != null
            && !fam.equals(KoLCharacter.getFamiliar())
            && fam.canEquip()
            && !this.familiars.contains(fam)) {
          this.familiars.add(fam);
        }
        continue;
      }

      Slot slot = EquipmentRequest.slotNumber(keyword);
      if (SlotSet.ALL_SLOTS.contains(slot)) {
        this.slots.merge(slot, (int) weight, Integer::sum);
        continue;
      }

      index = DoubleModifier.byCaselessName(keyword);

      // Adjust for generic abbreviations
      if (index == null) {
        if (keyword.endsWith(" res")) {
          keyword += "istance";
        } else if (keyword.endsWith(" dmg")) {
          keyword = keyword.substring(0, keyword.length() - 3) + "damage";
        } else if (keyword.endsWith(" dmg percent")) {
          keyword = keyword.substring(0, keyword.length() - 11) + "damage percent";
        } else if (keyword.endsWith(" exp")) {
          keyword = keyword.substring(0, keyword.length() - 3) + "experience";
        }
        index = DoubleModifier.byCaselessName(keyword);
      }

      if (index == null) {
        BooleanModifier modifier = BooleanModifier.byCaselessName(keyword);
        if (modifier != null) {
          this.booleanMask.add(modifier);
          if (weight > 0.0) {
            this.booleanValue.add(modifier);
          }
          continue;
        }
      }

      // Match keyword with multiple modifiers
      if (index == null) {
        switch (keyword) {
          case "all resistance" -> {
            this.weight.set(DoubleModifier.COLD_RESISTANCE, weight);
            this.weight.set(DoubleModifier.HOT_RESISTANCE, weight);
            this.weight.set(DoubleModifier.SLEAZE_RESISTANCE, weight);
            this.weight.set(DoubleModifier.SPOOKY_RESISTANCE, weight);
            this.weight.set(DoubleModifier.STENCH_RESISTANCE, weight);
            continue;
          }
          case "elemental damage" -> {
            this.weight.set(DoubleModifier.COLD_DAMAGE, weight);
            this.weight.set(DoubleModifier.HOT_DAMAGE, weight);
            this.weight.set(DoubleModifier.SLEAZE_DAMAGE, weight);
            this.weight.set(DoubleModifier.SPOOKY_DAMAGE, weight);
            this.weight.set(DoubleModifier.STENCH_DAMAGE, weight);
            continue;
          }
          case "hp regen" -> {
            this.weight.set(DoubleModifier.HP_REGEN_MIN, weight / 2);
            this.weight.set(DoubleModifier.HP_REGEN_MAX, weight / 2);
            continue;
          }
          case "mp regen" -> {
            this.weight.set(DoubleModifier.MP_REGEN_MIN, weight / 2);
            this.weight.set(DoubleModifier.MP_REGEN_MAX, weight / 2);
            continue;
          }
          case "passive damage" -> {
            this.weight.set(DoubleModifier.DAMAGE_AURA, weight);
            this.weight.set(DoubleModifier.THORNS, weight);
            continue;
          }
        }
      }

      // Match keyword with specific abbreviations
      if (index == null) {
        if (keyword.equals("init")) {
          index = DoubleModifier.INITIATIVE;
        } else if (keyword.equals("hp")) {
          index = DoubleModifier.HP;
        } else if (keyword.equals("mp")) {
          index = DoubleModifier.MP;
        } else if (keyword.equals("da")) {
          index = DoubleModifier.DAMAGE_ABSORPTION;
        } else if (keyword.equals("dr")) {
          index = DoubleModifier.DAMAGE_REDUCTION;
        } else if (keyword.equals("ml")) {
          index = DoubleModifier.MONSTER_LEVEL;
        } else if (MUS_EXP_PERC_PATTERN.matcher(keyword).find()) {
          index = DoubleModifier.MUS_EXPERIENCE_PCT;
        } else if (MUS_EXP_PATTERN.matcher(keyword).find()) {
          index = DoubleModifier.MUS_EXPERIENCE;
        } else if (MUS_PERC_PATTERN.matcher(keyword).find()) {
          index = DoubleModifier.MUS_PCT;
        } else if (MYS_EXP_PERC_PATTERN.matcher(keyword).find()) {
          index = DoubleModifier.MYS_EXPERIENCE_PCT;
        } else if (MYS_EXP_PATTERN.matcher(keyword).find()) {
          index = DoubleModifier.MYS_EXPERIENCE;
        } else if (MYS_PERC_PATTERN.matcher(keyword).find()) {
          index = DoubleModifier.MYS_PCT;
        } else if (MOX_EXP_PERC_PATTERN.matcher(keyword).find()) {
          index = DoubleModifier.MOX_EXPERIENCE_PCT;
        } else if (MOX_EXP_PATTERN.matcher(keyword).find()) {
          index = DoubleModifier.MOX_EXPERIENCE;
        } else if (MOX_PERC_PATTERN.matcher(keyword).find()) {
          index = DoubleModifier.MOX_PCT;
        } else if (keyword.startsWith("mus")) {
          index = DoubleModifier.MUS;
        } else if (keyword.startsWith("mys")) {
          index = DoubleModifier.MYS;
        } else if (keyword.startsWith("mox")) {
          index = DoubleModifier.MOX;
        } else if (keyword.startsWith("main")) {
          index = DoubleModifier.primeStat();
        } else if (keyword.startsWith("com")) {
          index = DoubleModifier.COMBAT_RATE;
          if (AdventureDatabase.isUnderwater(Modifiers.currentLocation)) {
            this.weight.set(DoubleModifier.UNDERWATER_COMBAT_RATE, weight);
          }
        } else if (keyword.startsWith("item")) {
          index = DoubleModifier.ITEMDROP;
        } else if (keyword.startsWith("meat")) {
          index = DoubleModifier.MEATDROP;
        } else if (keyword.startsWith("adv")) {
          this.beeosity = 999;
          index = DoubleModifier.ADVENTURES;
        } else if (keyword.startsWith("fites")) {
          this.beeosity = 999;
          index = DoubleModifier.PVP_FIGHTS;
        } else if (keyword.startsWith("exp")) {
          index = DoubleModifier.EXPERIENCE;
        } else if (keyword.startsWith("crit")) {
          index = DoubleModifier.CRITICAL_PCT;
        } else if (keyword.startsWith("spell crit")) {
          index = DoubleModifier.SPELL_CRITICAL_PCT;
        } else if (keyword.startsWith("sprinkle")) {
          index = DoubleModifier.SPRINKLES;
        } else if (keyword.equals("ocrs")) {
          this.noTiebreaker = true;
          this.beeosity = 999;
          index = DoubleModifier.RANDOM_MONSTER_MODIFIERS;
        }
      }

      if (index != null) {
        // We found a match.
        String modifierName = index.getName();
        this.weight.set(index, weight);
        continue;
      }

      KoLmafia.updateDisplay(MafiaState.ERROR, "Unrecognized keyword: " + keyword);
      return;
    }

    // If no tiebreaker, consider current unless -current specified
    if (!forceCurrent && this.noTiebreaker) {
      this.current = true;
    }

    this.beeosity = Math.max(Math.max(this.beeosity, equipBeeosity), outfitBeeosity);

    // Make sure indirect sources have at least a little weight;
    addFudge(
        DoubleModifier.EXPERIENCE,
        DoubleModifier.MONSTER_LEVEL,
        DoubleModifier.MONSTER_LEVEL_PERCENT,
        DoubleModifier.MUS_EXPERIENCE,
        DoubleModifier.MYS_EXPERIENCE,
        DoubleModifier.MOX_EXPERIENCE,
        DoubleModifier.MUS_EXPERIENCE_PCT,
        DoubleModifier.MYS_EXPERIENCE_PCT,
        DoubleModifier.MOX_EXPERIENCE_PCT,
        DoubleModifier.VOLLEYBALL_WEIGHT,
        DoubleModifier.SOMBRERO_WEIGHT,
        DoubleModifier.VOLLEYBALL_EFFECTIVENESS,
        DoubleModifier.SOMBRERO_EFFECTIVENESS,
        DoubleModifier.SOMBRERO_BONUS);

    addFudge(
        DoubleModifier.ITEMDROP,
        DoubleModifier.FOODDROP,
        DoubleModifier.BOOZEDROP,
        DoubleModifier.HATDROP,
        DoubleModifier.WEAPONDROP,
        DoubleModifier.OFFHANDDROP,
        DoubleModifier.SHIRTDROP,
        DoubleModifier.PANTSDROP,
        DoubleModifier.ACCESSORYDROP,
        DoubleModifier.CANDYDROP,
        DoubleModifier.GEARDROP,
        DoubleModifier.FAIRY_WEIGHT,
        DoubleModifier.FAIRY_EFFECTIVENESS,
        DoubleModifier.SPORADIC_ITEMDROP,
        DoubleModifier.PICKPOCKET_CHANCE);

    addFudge(
        DoubleModifier.MEATDROP,
        DoubleModifier.LEPRECHAUN_WEIGHT,
        DoubleModifier.LEPRECHAUN_EFFECTIVENESS,
        DoubleModifier.SPORADIC_MEATDROP,
        DoubleModifier.MEAT_BONUS);

    addFudge(DoubleModifier.DAMAGE_AURA, DoubleModifier.SPORADIC_DAMAGE_AURA);
    addFudge(DoubleModifier.THORNS, DoubleModifier.SPORADIC_THORNS);
  }

  private void addFudge(DoubleModifier source, DoubleModifier... extras) {
    final double fudge = this.weight.get(source) * 0.0001f;
    if (fudge > 0) {
      for (var extra : extras) {
        this.weight.add(extra, fudge);
      }
    }
  }

  private AdventureResult pickPlumberTool(int primeIndex) {
    AdventureResult hammer = ItemPool.get(ItemPool.HAMMER);
    boolean haveHammer = InventoryManager.hasItem(hammer);
    AdventureResult heavyHammer = ItemPool.get(ItemPool.HEAVY_HAMMER);
    boolean haveHeavyHammer = InventoryManager.hasItem(heavyHammer);
    AdventureResult fireFlower = ItemPool.get(ItemPool.PLUMBER_FIRE_FLOWER);
    boolean haveFireFlower = InventoryManager.hasItem(fireFlower);
    AdventureResult bonfireFlower = ItemPool.get(ItemPool.BONFIRE_FLOWER);
    boolean haveBonfireFlower = InventoryManager.hasItem(bonfireFlower);
    AdventureResult workBoots = ItemPool.get(ItemPool.WORK_BOOTS);
    boolean haveWorkBoots = InventoryManager.hasItem(workBoots);
    AdventureResult fancyBoots = ItemPool.get(ItemPool.FANCY_BOOTS);
    boolean haveFancyBoots = InventoryManager.hasItem(fancyBoots);

    // Find the best plumber tool
    return switch (primeIndex) {
      case 0 -> // Muscle
      haveHeavyHammer ? heavyHammer : haveHammer ? hammer : null;
      case 1 -> // Mysticality
      haveBonfireFlower ? bonfireFlower : haveFireFlower ? fireFlower : null;
      case 2 -> // Moxie
      haveFancyBoots ? fancyBoots : haveWorkBoots ? workBoots : null;
      default ->
      // If you don't care about stat, pick the best item you own.
      haveHeavyHammer
          ? heavyHammer
          : haveBonfireFlower
              ? bonfireFlower
              : haveFancyBoots
                  ? fancyBoots
                  : haveHammer ? hammer : haveFireFlower ? fireFlower : workBoots;
    };
  }

  public double getScore(Modifiers mods, Map<Slot, AdventureResult> equipment) {
    this.failed = false;
    this.exceeded = false;
    var predicted = mods.predict();

    double score = 0.0;
    for (var mod : DoubleModifier.DOUBLE_MODIFIERS) {
      double weight = this.weight.get(mod);
      double min = this.min.get(mod);
      if (weight == 0.0 && min == Double.NEGATIVE_INFINITY) continue;
      double val = mods.getDouble(mod);
      double max = this.max.get(mod);
      switch (mod) {
        case MUS:
          val = predicted.get(DerivedModifier.BUFFED_MUS);
          break;
        case MYS:
          val = predicted.get(DerivedModifier.BUFFED_MYS);
          break;
        case MOX:
          val = predicted.get(DerivedModifier.BUFFED_MOX);
          break;
        case FAMILIAR_WEIGHT:
          val += mods.getDouble(DoubleModifier.HIDDEN_FAMILIAR_WEIGHT);
          if (mods.getDouble(DoubleModifier.FAMILIAR_WEIGHT_PCT) < 0.0) {
            val *= 0.5f;
          }
          break;
        case MANA_COST:
          val += mods.getDouble(DoubleModifier.STACKABLE_MANA_COST);
          break;
        case INITIATIVE:
          val += Math.min(0.0, mods.getDouble(DoubleModifier.INITIATIVE_PENALTY));
          break;
        case MEATDROP:
          val +=
              100.0
                  + Math.min(0.0, mods.getDouble(DoubleModifier.MEATDROP_PENALTY))
                  + mods.getDouble(DoubleModifier.SPORADIC_MEATDROP)
                  + mods.getDouble(DoubleModifier.MEAT_BONUS) / 10000.0;
          break;
        case ITEMDROP:
          val +=
              100.0
                  + Math.min(0.0, mods.getDouble(DoubleModifier.ITEMDROP_PENALTY))
                  + mods.getDouble(DoubleModifier.SPORADIC_ITEMDROP);
          break;
        case HP:
          val = predicted.get(DerivedModifier.BUFFED_HP);
          break;
        case MP:
          val = predicted.get(DerivedModifier.BUFFED_MP);
          break;
        case WEAPON_DAMAGE:
          // Incorrect - needs to estimate base damage
          val += mods.getDouble(DoubleModifier.WEAPON_DAMAGE_PCT);
          break;
        case RANGED_DAMAGE:
          // Incorrect - needs to estimate base damage
          val += mods.getDouble(DoubleModifier.RANGED_DAMAGE_PCT);
          break;
        case SPELL_DAMAGE:
          // Incorrect - base damage depends on spell used
          val += mods.getDouble(DoubleModifier.SPELL_DAMAGE_PCT);
          break;
        case COLD_RESISTANCE:
          if (mods.getBoolean(BooleanModifier.COLD_IMMUNITY)) {
            val = 100.0;
          } else if (mods.getBoolean(BooleanModifier.COLD_VULNERABILITY)) {
            val -= 100.0;
          }
          break;
        case HOT_RESISTANCE:
          if (mods.getBoolean(BooleanModifier.HOT_IMMUNITY)) {
            val = 100.0;
          } else if (mods.getBoolean(BooleanModifier.HOT_VULNERABILITY)) {
            val -= 100.0;
          }
          break;
        case SLEAZE_RESISTANCE:
          if (mods.getBoolean(BooleanModifier.SLEAZE_IMMUNITY)) {
            val = 100.0;
          } else if (mods.getBoolean(BooleanModifier.SLEAZE_VULNERABILITY)) {
            val -= 100.0;
          }
          break;
        case SPOOKY_RESISTANCE:
          if (mods.getBoolean(BooleanModifier.SPOOKY_IMMUNITY)) {
            val = 100.0;
          } else if (mods.getBoolean(BooleanModifier.SPOOKY_VULNERABILITY)) {
            val -= 100.0;
          }
          break;
        case STENCH_RESISTANCE:
          if (mods.getBoolean(BooleanModifier.STENCH_IMMUNITY)) {
            val = 100.0;
          } else if (mods.getBoolean(BooleanModifier.STENCH_VULNERABILITY)) {
            val -= 100.0;
          }
          break;
        case EXPERIENCE:
          double baseExp =
              KoLCharacter.estimatedBaseExp(
                  mods.getDouble(DoubleModifier.MONSTER_LEVEL)
                      * (1 + mods.getDouble(DoubleModifier.MONSTER_LEVEL_PERCENT) / 100));
          double expPct = mods.getDouble(DoubleModifier.primeStatExpPercent()) / 100.0f;
          double exp = mods.getDouble(DoubleModifier.primeStatExp());

          val = ((baseExp + exp) * (1 + expPct)) / 2.0f;
          break;
        case DAMAGE_AURA:
          val += mods.getDouble(DoubleModifier.SPORADIC_DAMAGE_AURA);
          break;
        case THORNS:
          val += mods.getDouble(DoubleModifier.SPORADIC_THORNS);
          break;
      }
      if (val < min) this.failed = true;
      score += weight * Math.min(val, max);
    }
    if (this.stinkycheese > 0) {
      int val = mods.getBitmap(BitmapModifier.STINKYCHEESE);
      score += this.stinkycheese * val;
    }
    if (!this.bonuses.isEmpty()) {
      for (AdventureResult item : equipment.values()) {
        if (this.bonuses.containsKey(item)) {
          score += this.bonuses.get(item);
        }
      }
    }
    if (!this.bonusFunc.isEmpty()) {
      for (BonusFunction func : this.bonusFunc) {
        for (AdventureResult item : equipment.values()) {
          score += func.bonusFunction.apply(item) * func.weight;
        }
      }
    }
    // Add fudge factor for Rollover Effect
    if (!mods.getStrings(MultiStringModifier.ROLLOVER_EFFECT).isEmpty()) {
      score += 0.01f;
    }
    if (score < this.totalMin) this.failed = true;
    if (score >= this.totalMax) this.exceeded = true;
    // special handling for -osity:
    // The "weight" specified is actually the desired -osity.
    // Allow partials to contribute to the score (1:1 ratio) up to the desired value.
    // Similar to setting a max.
    if (this.clownosity > 0) {
      int osity = mods.getBitmap(BitmapModifier.CLOWNINESS);
      score += Math.min(osity, this.clownosity);
      if (osity < this.clownosity) this.failed = true;
    }
    if (this.raveosity > 0) {
      int osity = mods.getBitmap(BitmapModifier.RAVEOSITY);
      score += Math.min(osity, this.raveosity);
      if (osity < this.raveosity) this.failed = true;
    }
    if (this.surgeonosity > 0) {
      int osity = mods.getBitmap(BitmapModifier.SURGEONOSITY);
      score += Math.min(osity, this.surgeonosity);
      if (osity < this.surgeonosity) this.failed = true;
    }
    if (!this.failed
        && !this.booleanMask.isEmpty()
        && !mods.getBooleans(this.booleanMask).equals(this.booleanValue)) {
      this.failed = true;
    }
    return score;
  }

  public double getScore(Modifiers mods) {
    return this.getScore(mods, Map.of());
  }

  void checkEquipment(Modifiers mods, Map<Slot, AdventureResult> equipment, int beeosity) {
    boolean outfitSatisfied = this.posOutfits.isEmpty();
    boolean equipSatisfied = this.posEquip.isEmpty();
    if (!this.failed && !this.posEquip.isEmpty()) {
      equipSatisfied = true;
      for (AdventureResult item : this.posEquip) {
        if (!KoLCharacter.hasEquipped(equipment, item)) {
          equipSatisfied = false;
          break;
        }
      }
    }
    if (!this.failed) {
      String outfit = mods.getString(StringModifier.OUTFIT);
      if (this.negOutfits.contains(outfit)) {
        this.failed = true;
      } else {
        outfitSatisfied = this.posOutfits.contains(outfit) || this.posOutfits.isEmpty();
      }
    }
    // negEquip is not checked, since enumerateEquipment should make it
    // impossible for such items to be chosen.
    if (!outfitSatisfied || !equipSatisfied) {
      this.failed = true;
    }
    if (beeosity > this.beeosity) {
      this.failed = true;
    }
  }

  double getTiebreaker(Modifiers mods) {
    if (this.noTiebreaker) return 0.0;
    return this.tiebreaker.getScore(mods);
  }

  boolean isUsingTiebreaker() {
    return !this.noTiebreaker;
  }

  enum Constraint {
    /** Item violates a constraint, don't use it */
    VIOLATES,
    /** Item not relevant to any constraints */
    IRRELEVANT,
    /** Item meets a constraint, give it special handling */
    MEETS
  }

  Constraint checkConstraints(Modifiers mods) {
    if (mods == null) return Constraint.IRRELEVANT;
    EnumSet<BooleanModifier> bools = mods.getBooleans(this.booleanMask);
    if (!this.booleanValue.containsAll(bools)) return Constraint.VIOLATES;
    if (!bools.isEmpty()) return Constraint.MEETS;
    return Constraint.IRRELEVANT;
  }

  public static boolean cannotGainEffect(int effectId) {
    // Return true if effect cannot be gained due to current other effects or class
    return switch (effectId) {
      case EffectPool.NEARLY_SILENT_HUNTING -> KoLCharacter.isSealClubber();
      case EffectPool.SILENT_HUNTING, EffectPool.BARREL_CHESTED -> !KoLCharacter.isSealClubber();
      case EffectPool.BOON_OF_SHE_WHO_WAS -> KoLCharacter.getBlessingType()
              != TurtleBlessing.SHE_WHO_WAS
          || KoLCharacter.getBlessingLevel() == TurtleBlessingLevel.AVATAR;
      case EffectPool.BOON_OF_THE_STORM_TORTOISE -> KoLCharacter.getBlessingType()
              != TurtleBlessing.STORM
          || KoLCharacter.getBlessingLevel() == TurtleBlessingLevel.AVATAR;
      case EffectPool.BOON_OF_THE_WAR_SNAPPER -> KoLCharacter.getBlessingType()
              != TurtleBlessing.WAR
          || KoLCharacter.getBlessingLevel() == TurtleBlessingLevel.AVATAR;
      case EffectPool.AVATAR_OF_SHE_WHO_WAS -> KoLCharacter.getBlessingType()
              != TurtleBlessing.SHE_WHO_WAS
          || KoLCharacter.getBlessingLevel() != TurtleBlessingLevel.GLORIOUS_BLESSING;
      case EffectPool.AVATAR_OF_THE_STORM_TORTOISE -> KoLCharacter.getBlessingType()
              != TurtleBlessing.STORM
          || KoLCharacter.getBlessingLevel() != TurtleBlessingLevel.GLORIOUS_BLESSING;
      case EffectPool.AVATAR_OF_THE_WAR_SNAPPER -> KoLCharacter.getBlessingType()
              != TurtleBlessing.WAR
          || KoLCharacter.getBlessingLevel() != TurtleBlessingLevel.GLORIOUS_BLESSING;
      case EffectPool.BLESSING_OF_SHE_WHO_WAS -> !KoLCharacter.isTurtleTamer()
          || KoLCharacter.getBlessingType() == TurtleBlessing.SHE_WHO_WAS
          || KoLCharacter.getBlessingLevel() == TurtleBlessingLevel.PARIAH
          || KoLCharacter.getBlessingLevel() == TurtleBlessingLevel.AVATAR;
      case EffectPool.BLESSING_OF_THE_STORM_TORTOISE -> !KoLCharacter.isTurtleTamer()
          || KoLCharacter.getBlessingType() == TurtleBlessing.STORM
          || KoLCharacter.getBlessingLevel() == TurtleBlessingLevel.PARIAH
          || KoLCharacter.getBlessingLevel() == TurtleBlessingLevel.AVATAR;
      case EffectPool.BLESSING_OF_THE_WAR_SNAPPER -> !KoLCharacter.isTurtleTamer()
          || KoLCharacter.getBlessingType() == TurtleBlessing.WAR
          || KoLCharacter.getBlessingLevel() == TurtleBlessingLevel.PARIAH
          || KoLCharacter.getBlessingLevel() == TurtleBlessingLevel.AVATAR;
      case EffectPool.DISDAIN_OF_SHE_WHO_WAS,
          EffectPool.DISDAIN_OF_THE_STORM_TORTOISE,
          EffectPool.DISDAIN_OF_THE_WAR_SNAPPER -> KoLCharacter.isTurtleTamer();
      case EffectPool.BARREL_OF_LAUGHS -> !KoLCharacter.isTurtleTamer();
      case EffectPool.FLIMSY_SHIELD_OF_THE_PASTALORD,
          EffectPool.BLOODY_POTATO_BITS,
          EffectPool.SLINKING_NOODLE_GLOB,
          EffectPool.WHISPERING_STRANDS,
          EffectPool.MACARONI_COATING,
          EffectPool.PENNE_FEDORA,
          EffectPool.PASTA_EYEBALL,
          EffectPool.SPICE_HAZE -> KoLCharacter.isPastamancer();
      case EffectPool.SHIELD_OF_THE_PASTALORD, EffectPool.PORK_BARREL -> !KoLCharacter
          .isPastamancer();
      case EffectPool.BLOOD_SUGAR_SAUCE_MAGIC,
          EffectPool.SOULERSKATES,
          EffectPool.WARLOCK_WARSTOCK_WARBARREL -> !KoLCharacter.isSauceror();
      case EffectPool.BLOOD_SUGAR_SAUCE_MAGIC_LITE -> KoLCharacter.isSauceror();
      case EffectPool.DOUBLE_BARRELED -> !KoLCharacter.isDiscoBandit();
      case EffectPool.BEER_BARREL_POLKA -> !KoLCharacter.isAccordionThief();
      case EffectPool.UNMUFFLED -> !Preferences.getString("peteMotorbikeMuffler")
          .equals("Extra-Loud Muffler");
      case EffectPool.MUFFLED -> !Preferences.getString("peteMotorbikeMuffler")
          .equals("Extra-Quiet Muffler");
      default -> false;
    };
  }

  void enumerateEquipment(EquipScope equipScope, int maxPrice, PriceLevel priceLevel)
      throws MaximizerInterruptedException {
    // Items automatically considered regardless of their score -
    // synergies, hobo power, brimstone, etc.
    SlotList<CheckedItem> automatic = new SlotList<>(this.familiars.size());
    // Items to be considered based on their score
    SlotList<CheckedItem> ranked = new SlotList<>(this.familiars.size());

    double nullScore = this.getScore(new Modifiers());

    Map<Integer, Boolean> usefulOutfits = new HashMap<>();
    Map<AdventureResult, AdventureResult> outfitPieces = new HashMap<>();
    for (var outfitEntry : EquipmentDatabase.normalOutfits.entrySet()) {
      var i = outfitEntry.getKey();
      var outfit = outfitEntry.getValue();
      if (outfit == null) continue;
      if (this.negOutfits.contains(outfit.getName())) continue;
      if (this.posOutfits.contains(outfit.getName())) {
        usefulOutfits.put(i, true);
        continue;
      }

      Modifiers mods = ModifierDatabase.getModifiers(ModifierType.OUTFIT, outfit.getName());
      if (mods == null) continue;

      switch (this.checkConstraints(mods)) {
        case VIOLATES:
          continue;
        case IRRELEVANT:
          // intentionally not including outfit.getPieces() because this is
          // only rating whether the outfit itself is useful, not its pieces
          double delta = this.getScore(mods) - nullScore;
          if (delta <= 0.0) continue;
          break;
      }
      usefulOutfits.put(i, true);
    }

    int usefulSynergies = 0;
    for (Entry<String, Integer> entry : ModifierDatabase.getSynergies()) {
      Modifiers mods = ModifierDatabase.getModifiers(ModifierType.SYNERGY, entry.getKey());
      int value = entry.getValue();
      if (mods == null) continue;
      double delta = this.getScore(mods) - nullScore;
      if (delta > 0.0) usefulSynergies |= value;
    }

    boolean hoboPowerUseful = isCatUseful(nullScore, "_hoboPower");
    boolean smithsnessUseful = isCatUseful(nullScore, "_smithsness");
    boolean brimstoneUseful = isCatUseful(nullScore, "_brimstone");
    boolean cloathingUseful = isCatUseful(nullScore, "_cloathing");
    boolean slimeHateUseful = isCatUseful(nullScore, "_slimeHate");
    boolean mcHugeLargeUseful = isCatUseful(nullScore, "_mcHugeLarge");

    // This relies on the special sauce glove having a lower ID
    // than any chefstaff.
    boolean gloveAvailable = false;

    int id = 0;
    while ((id = EquipmentDatabase.nextEquipmentItemId(id)) != -1) {
      Slot slot = EquipmentManager.itemIdToEquipmentType(id);
      if (slot == Slot.NONE) continue;
      AdventureResult preItem = ItemPool.get(id, 1);
      String name = preItem.getName();
      CheckedItem item = null;
      if (this.negEquip.contains(preItem)) continue;
      if (KoLCharacter.inBeecore()
          && KoLCharacter.getBeeosity(name) > this.beeosity) { // too beechin' all by itself!
        continue;
      }

      var modeable = Modeable.find(id);

      boolean famCanEquip = KoLCharacter.getFamiliar().canEquip(preItem);
      if (famCanEquip && slot != Slot.FAMILIAR) {
        // Modifiers when worn by Hatrack or Scarecrow
        Modifiers familiarMods = new Modifiers();
        int familiarId = KoLCharacter.getFamiliar().getId();
        if ((familiarId == FamiliarPool.HATRACK && slot == Slot.HAT)
            || (familiarId == FamiliarPool.SCARECROW && slot == Slot.PANTS)) {
          familiarMods.applyFamiliarModifiers(KoLCharacter.getFamiliar(), preItem);
        }
        // Normal item modifiers when used by Disembodied Hand and Left-Hand
        else {
          familiarMods = ModifierDatabase.getItemModifiersInFamiliarSlot(id);

          // Some items work differently with the Left Hand
          if (familiarId == FamiliarPool.LEFT_HAND) {
            familiarMods =
                switch (id) {
                  case ItemPool.KOL_COL_13_SNOWGLOBE, ItemPool.GLOWING_ESCA -> null;
                  default -> familiarMods;
                };
          }
        }

        // no enchantments
        if (familiarMods == null) {
          familiarMods = new Modifiers();
        }

        item = new CheckedItem(id, equipScope, maxPrice, priceLevel);

        switch (this.checkConstraints(familiarMods)) {
          case VIOLATES:
            continue;
          case MEETS:
            item.automaticFlag = true;
        }

        if (modeable != null) {
          item.automaticFlag = true;
        }

        if (item.getCount() != 0
            && (this.getScore(familiarMods) - nullScore > 0.0 || item.automaticFlag)) {
          ranked.get(Slot.FAMILIAR).add(item);
        }
      }
      for (int f = this.familiars.size() - 1; f >= 0; --f) {
        FamiliarData fam = this.familiars.get(f);
        if (!fam.canEquip(preItem)) continue;
        // Modifiers when worn by Hatrack or Scarecrow
        Modifiers familiarMods = new Modifiers();
        int familiarId = fam.getId();
        if ((familiarId == FamiliarPool.HATRACK && slot == Slot.HAT)
            || (familiarId == FamiliarPool.SCARECROW && slot == Slot.PANTS)) {
          familiarMods.applyFamiliarModifiers(fam, preItem);
        } else {
          // Normal item modifiers when used by Disembodied Hand
          familiarMods = ModifierDatabase.getItemModifiers(id);
          if (familiarMods == null) { // no enchantments
            familiarMods = new Modifiers();
          }
        }
        if (item == null) {
          item = new CheckedItem(id, equipScope, maxPrice, priceLevel);
        }

        switch (this.checkConstraints(familiarMods)) {
          case VIOLATES:
            continue;
          case MEETS:
            item.automaticFlag = true;
        }

        if (modeable != null) {
          item.automaticFlag = true;
        }

        if (item.getCount() != 0
            && (this.getScore(familiarMods) - nullScore > 0.0 || item.automaticFlag)) {
          ranked.getFamiliar(f).add(item);
        }
      }

      if (!EquipmentManager.canEquip(id) && !KoLCharacter.hasEquipped(id)) continue;
      if (item == null) {
        item = new CheckedItem(id, equipScope, maxPrice, priceLevel);
      }

      if (item.getCount() == 0) {
        continue;
      }

      Slot auxSlot = Slot.NONE;
      gotItem:
      {
        switch (slot) {
          case FAMILIAR:
            if (!famCanEquip) continue;
            break;

          case WEAPON:
            int hands = EquipmentDatabase.getHands(id);
            if (this.hands == 1 && hands != 1) {
              continue;
            }
            if (this.hands > 1 && hands < this.hands) {
              continue;
            }
            WeaponType weaponType = EquipmentDatabase.getWeaponType(id);
            if (this.melee > 0 && weaponType != WeaponType.MELEE) {
              continue;
            }
            if (this.melee < 0 && weaponType != WeaponType.RANGED) {
              continue;
            }
            String type = EquipmentDatabase.getItemType(id);
            if (this.weaponType != null && !type.contains(this.weaponType)) {
              continue;
            }
            if (hands == 1) {
              slot = Evaluator.WEAPON_1H;
              if (type.equals("chefstaff")) { // Don't allow chefstaves to displace other
                // 1H weapons from the shortlist if you can't
                // equip them anyway.
                if (!KoLCharacter.hasSkill(SkillPool.SPIRIT_OF_RIGATONI)
                    && !KoLCharacter.isJarlsberg()
                    && !(KoLCharacter.isSauceror() && gloveAvailable)) {
                  continue;
                }
                // In any case, don't put this in an aux slot.
              } else if (!this.requireShield && !EquipmentDatabase.isMainhandOnly(id)) {
                switch (weaponType) {
                  case MELEE -> auxSlot = Evaluator.OFFHAND_MELEE;
                  case RANGED -> auxSlot = Evaluator.OFFHAND_RANGED;
                  case NONE -> {}
                }
              }
            }
            if (this.requireClub && !EquipmentDatabase.isClub(id)) {
              slot = auxSlot;
            }
            if (this.requireUtensil && !EquipmentDatabase.isUtensil(id)) {
              slot = auxSlot;
            }
            if (this.requireSword && !EquipmentDatabase.isSword(id)) {
              slot = auxSlot;
            }
            if (this.requireKnife && !EquipmentDatabase.isKnife(id)) {
              slot = auxSlot;
            }
            if (this.requireAccordion && !EquipmentDatabase.isAccordion(id)) {
              slot = auxSlot;
            }
            if (this.effective) {
              if (id != ItemPool.FOURTH_SABER
                  && id != ItemPool.REPLICA_FOURTH_SABER
                  && id != ItemPool.JUNE_CLEAVER) {
                // Always uses best stat, so always considered effective
                if (KoLCharacter.getAdjustedMoxie() >= KoLCharacter.getAdjustedMuscle()
                    && weaponType != WeaponType.RANGED
                    && (!EquipmentDatabase.isKnife(id)
                        || !KoLCharacter.hasSkill(SkillPool.TRICKY_KNIFEWORK))) {
                  slot = auxSlot;
                }
                if (KoLCharacter.getAdjustedMoxie() < KoLCharacter.getAdjustedMuscle()
                    && weaponType != WeaponType.MELEE) {
                  slot = auxSlot;
                }
              }
            }
            if (id == ItemPool.BROKEN_CHAMPAGNE
                && this.weight.get(DoubleModifier.ITEMDROP) > 0
                && (Preferences.getInteger("garbageChampagneCharge") > 0
                    || !Preferences.getBoolean("_garbageItemChanged"))) {
              // This is always going to be worth including if useful
              item.requiredFlag = true;
              item.automaticFlag = true;
              break gotItem;
            }
            break;

          case OFFHAND:
            if (this.requireShield
                && !EquipmentDatabase.isShield(id)
                && id != ItemPool.UNBREAKABLE_UMBRELLA) {
              continue;
            }
            if (hoboPowerUseful && name.startsWith("Hodgman's")) {
              Modifiers.hoboPower = 100.0;
              item.automaticFlag = true;
            }
            break;

          case ACCESSORY1:
            if (id == ItemPool.SPECIAL_SAUCE_GLOVE
                && KoLCharacter.isSauceror()
                && !KoLCharacter.hasSkill(SkillPool.SPIRIT_OF_RIGATONI)) {
              item.validate(maxPrice, priceLevel);

              if (item.getCount() == 0) {
                continue;
              }

              item.automaticFlag = true;
              gloveAvailable = true;
              break gotItem;
            }
            break;
          case SHIRT:
            if (id == ItemPool.MAKESHIFT_GARBAGE_SHIRT
                && (this.weight.get(DoubleModifier.EXPERIENCE) > 0
                    || this.weight.get(DoubleModifier.MUS_EXPERIENCE) > 0
                    || this.weight.get(DoubleModifier.MYS_EXPERIENCE) > 0
                    || this.weight.get(DoubleModifier.MOX_EXPERIENCE) > 0)
                && Preferences.getInteger("garbageShirtCharge") > 0) {
              // This is always going to be worth including if useful
              item.requiredFlag = true;
              item.automaticFlag = true;
              break gotItem;
            }
            break;
        }

        // Some items can only be equipped in certain paths in hardcore
        // Will only affect characters who buy items for other paths whilst in run

        if (KoLCharacter.isHardcore()) {
          switch (id) {
            case ItemPool.BORIS_HELM:
            case ItemPool.BORIS_HELM_ASKEW:
              if (!KoLCharacter.isAvatarOfBoris()) {
                continue;
              }
              break;
            case ItemPool.RIGHT_BEAR_ARM:
            case ItemPool.LEFT_BEAR_ARM:
              if (!KoLCharacter.isZombieMaster()) {
                continue;
              }
              break;
            case ItemPool.JARLS_PAN:
            case ItemPool.JARLS_COSMIC_PAN:
              if (!KoLCharacter.isJarlsberg()) {
                continue;
              }
              break;
            case ItemPool.FOLDER_HOLDER:
              if (!KoLCharacter.inHighschool()) {
                continue;
              }
              break;
            case ItemPool.PETE_JACKET:
            case ItemPool.PETE_JACKET_COLLAR:
              if (!KoLCharacter.isSneakyPete()) {
                continue;
              }
              break;
            case ItemPool.THORS_PLIERS:
              if (!KoLCharacter.inRaincore()) {
                continue;
              }
              break;
            case ItemPool.CROWN_OF_ED:
              if (!KoLCharacter.isEd()) {
                continue;
              }
              break;
            default:
              break;
          }
        }

        if (usefulOutfits.getOrDefault(EquipmentDatabase.getOutfitWithItem(id), false)) {
          item.validate(maxPrice, priceLevel);

          if (item.getCount() == 0) {
            continue;
          }
          outfitPieces.put(item, item);
        }

        if (KoLCharacter.hasEquipped(item)
            && this.current) { // Make sure the current item in each slot is considered
          // for keeping, unless it's actively harmful, unless -current
          // option is used
          item.automaticFlag = true;
        }

        Modifiers mods = ModifierDatabase.getItemModifiers(id);
        if (mods == null) { // no enchantments
          mods = new Modifiers();
        }

        boolean wrongClass = false;
        String classType = mods.getString(StringModifier.CLASS);
        if (!classType.isEmpty() && !classType.equals(KoLCharacter.getAscensionClassName())) {
          wrongClass = true;
        }

        if (mods.getBoolean(BooleanModifier.SINGLE)) {
          item.singleFlag = true;
        }

        // If you have a familiar carrier, we'll need to check 1 or 2 Familiars best carried
        // unless you specified not to change them

        if (((id == ItemPool.HATSEAT && this.slots.getOrDefault(Slot.CROWNOFTHRONES, 0) >= 0)
                || (id == ItemPool.BUDDY_BJORN && this.slots.getOrDefault(Slot.BUDDYBJORN, 0) >= 0))
            && !KoLCharacter.isSneakyPete()
            && !KoLCharacter.inAxecore()
            && !KoLCharacter.isJarlsberg()) {
          this.carriedFamiliarsNeeded++;
        }

        if (id == ItemPool.CARD_SLEEVE && this.slots.getOrDefault(Slot.CARDSLEEVE, 0) >= 0) {
          this.cardNeeded = true;
        }

        if (id == ItemPool.VAMPYRIC_CLOAKE) {
          mods = new Modifiers(mods);
          mods.applyVampyricCloakeModifiers();
        }

        if (modeable != null) {
          var slotWeightings =
              switch (modeable.getSlot()) {
                case ACCESSORY1 -> List.of(
                    this.slots.getOrDefault(Slot.ACCESSORY1, 0),
                    this.slots.getOrDefault(Slot.ACCESSORY2, 0),
                    this.slots.getOrDefault(Slot.ACCESSORY3, 0));
                case OFFHAND -> List.of(
                    this.slots.getOrDefault(Slot.OFFHAND, 0),
                    this.slots.getOrDefault(Slot.FAMILIAR, 0));
                default -> List.of(this.slots.getOrDefault(modeable.getSlot(), 0));
              };
          modeablesNeeded.put(modeable, slotWeightings.stream().anyMatch(s -> s >= 0));
        }

        if (this.posEquip.contains(item)) {
          item.automaticFlag = true;
          item.requiredFlag = true;
          break gotItem;
        }

        switch (this.checkConstraints(mods)) {
          case VIOLATES:
            continue;
          case MEETS:
            item.automaticFlag = true;
            break gotItem;
        }

        if ((hoboPowerUseful && mods.getDouble(DoubleModifier.HOBO_POWER) > 0.0)
            || (smithsnessUseful && !wrongClass && mods.getDouble(DoubleModifier.SMITHSNESS) > 0.0)
            || (brimstoneUseful && mods.getRawBitmap(BitmapModifier.BRIMSTONE) != 0)
            || (cloathingUseful && mods.getRawBitmap(BitmapModifier.CLOATHING) != 0)
            || (slimeHateUseful && mods.getDouble(DoubleModifier.SLIME_HATES_IT) > 0.0)
            || (mcHugeLargeUseful && mods.getRawBitmap(BitmapModifier.MCHUGELARGE) != 0)
            || (this.clownosity > 0 && mods.getRawBitmap(BitmapModifier.CLOWNINESS) != 0)
            || (this.raveosity > 0 && mods.getRawBitmap(BitmapModifier.RAVEOSITY) != 0)
            || (this.surgeonosity > 0 && mods.getRawBitmap(BitmapModifier.SURGEONOSITY) != 0)
            || (this.stinkycheese > 0 && mods.getRawBitmap(BitmapModifier.STINKYCHEESE) != 0)
            || ((mods.getRawBitmap(BitmapModifier.SYNERGETIC) & usefulSynergies) != 0)) {
          item.automaticFlag = true;
          break gotItem;
        }

        // Always carry through items with changeable contents to speculation, but don't force them
        // to go further
        if ((id == ItemPool.HATSEAT || id == ItemPool.BUDDY_BJORN)
            && !KoLCharacter.isSneakyPete()
            && !KoLCharacter.inAxecore()
            && !KoLCharacter.isJarlsberg()) {
          break gotItem;
        }

        if (id == ItemPool.CARD_SLEEVE) {
          break gotItem;
        }

        if (modeable != null) {
          if (!forcedModeables.get(modeable).isEmpty()) {
            item.automaticFlag = true;
          }
          break gotItem;
        }

        String intrinsic = mods.getString(StringModifier.INTRINSIC_EFFECT);
        if (!intrinsic.isEmpty()) {
          Modifiers newMods = new Modifiers();
          newMods.add(mods);
          newMods.add(ModifierDatabase.getModifiers(ModifierType.EFFECT, intrinsic));
          mods = newMods;
        }
        double delta = this.getScore(mods, Map.of(Slot.HAT, item)) - nullScore;
        if (delta < 0.0) continue;
        if (delta == 0.0) {
          if (KoLCharacter.hasEquipped(item) && this.current) break gotItem;
          if (item.initial == 0) continue;
          if (item.automaticFlag) continue;
        }

        if (mods.getBoolean(BooleanModifier.UNARMED)
            || mods.getRawBitmap(BitmapModifier.MUTEX)
                != 0) { // This item may turn out to be unequippable, so don't
          // count it towards the shortlist length.
          item.conditionalFlag = true;
        }
      }
      // "break gotItem" goes here
      if (slot != Slot.NONE) ranked.get(slot).add(item);
      if (auxSlot != Slot.NONE) ranked.get(auxSlot).add(item);
    }

    // Get best Familiars for Crown of Thrones and Buddy Bjorn
    // Assume current ones are best if in use
    FamiliarData bestCarriedFamiliar = FamiliarData.NO_FAMILIAR;
    FamiliarData secondBestCarriedFamiliar = FamiliarData.NO_FAMILIAR;
    FamiliarData useBjornFamiliar = null;
    FamiliarData useCrownFamiliar = null;

    // If we're not allowed to change the current familiar, lock it
    if (this.slots.getOrDefault(Slot.BUDDYBJORN, 0) < 0) {
      useBjornFamiliar = KoLCharacter.getBjorned();
    } else {
      bestCarriedFamiliar = KoLCharacter.getBjorned();
    }

    // If we're not allowed to change the current familiar, lock it
    if (this.slots.getOrDefault(Slot.CROWNOFTHRONES, 0) < 0) {
      useCrownFamiliar = KoLCharacter.getEnthroned();
    } else {
      secondBestCarriedFamiliar = KoLCharacter.getEnthroned();
    }

    if (bestCarriedFamiliar == FamiliarData.NO_FAMILIAR
        && !(secondBestCarriedFamiliar == FamiliarData.NO_FAMILIAR)) {
      bestCarriedFamiliar = secondBestCarriedFamiliar;
      secondBestCarriedFamiliar = FamiliarData.NO_FAMILIAR;
    }
    if (secondBestCarriedFamiliar != FamiliarData.NO_FAMILIAR) {
      // Make sure best is better than secondBest !
      MaximizerSpeculation best = new MaximizerSpeculation();
      MaximizerSpeculation secondBest = new MaximizerSpeculation();
      CheckedItem item = new CheckedItem(ItemPool.HATSEAT, equipScope, maxPrice, priceLevel);
      best.attachment = secondBest.attachment = item;
      best.equipment.put(Slot.HAT, item);
      secondBest.equipment.put(Slot.HAT, item);
      best.setEnthroned(bestCarriedFamiliar);
      secondBest.setEnthroned(secondBestCarriedFamiliar);
      if (secondBest.compareTo(best) > 0) {
        FamiliarData temp = bestCarriedFamiliar;
        bestCarriedFamiliar = secondBestCarriedFamiliar;
        secondBestCarriedFamiliar = temp;
      }
    }

    if (this.carriedFamiliarsNeeded > 0) {
      MaximizerSpeculation best = new MaximizerSpeculation();
      MaximizerSpeculation secondBest = new MaximizerSpeculation();
      CheckedItem item = new CheckedItem(ItemPool.HATSEAT, equipScope, maxPrice, priceLevel);
      best.attachment = secondBest.attachment = item;
      best.equipment.put(Slot.HAT, item);
      secondBest.equipment.put(Slot.HAT, item);
      best.setEnthroned(bestCarriedFamiliar);
      secondBest.setEnthroned(secondBestCarriedFamiliar);

      // Check each familiar in hat to see if they are worthwhile
      List<FamiliarData> familiarList = KoLCharacter.usableFamiliars();
      for (FamiliarData familiar : familiarList) {
        if (familiar != null
            && familiar != FamiliarData.NO_FAMILIAR
            && familiar.canCarry()
            && StandardRequest.isAllowed(RestrictedItemType.FAMILIARS, familiar.getRace())
            && !familiar.equals(KoLCharacter.getFamiliar())
            && !this.carriedFamiliars.contains(familiar)
            && !familiar.equals(useCrownFamiliar)
            && !familiar.equals(useBjornFamiliar)
            && !familiar.equals(bestCarriedFamiliar)
            && !(KoLCharacter.inBeecore() && KoLCharacter.hasBeeosity(familiar.getRace()))) {
          MaximizerSpeculation spec = new MaximizerSpeculation();
          spec.attachment = item;
          spec.equipment.put(Slot.HAT, item);
          spec.setEnthroned(familiar);
          spec.setUnscored();
          if (spec.compareTo(best) > 0) {
            secondBest = best.clone();
            best = spec.clone();
            secondBestCarriedFamiliar = bestCarriedFamiliar;
            bestCarriedFamiliar = familiar;
          } else if (spec.compareTo(secondBest) > 0) {
            secondBest = spec.clone();
            secondBestCarriedFamiliar = familiar;
          }
        }
      }
      this.carriedFamiliars.add(bestCarriedFamiliar);
      if (this.carriedFamiliarsNeeded > 1) {
        this.carriedFamiliars.add(secondBestCarriedFamiliar);
      }
    }

    // Get best Card for Card Sleeve
    CheckedItem bestCard = null;
    AdventureResult useCard = null;

    if (this.cardNeeded) {
      MaximizerSpeculation best = new MaximizerSpeculation();

      // Check each card in sleeve to see if they are worthwhile
      for (int c = 4967; c <= 5007; c++) {
        CheckedItem card = new CheckedItem(c, equipScope, maxPrice, priceLevel);
        AdventureResult equippedCard = EquipmentManager.getEquipment(Slot.CARDSLEEVE);
        if (card.getCount() > 0 || (equippedCard != null && c == equippedCard.getItemId())) {
          MaximizerSpeculation spec = new MaximizerSpeculation();
          CheckedItem sleeve =
              new CheckedItem(ItemPool.CARD_SLEEVE, equipScope, maxPrice, priceLevel);
          spec.attachment = sleeve;
          spec.equipment.put(Slot.OFFHAND, sleeve);
          spec.equipment.put(Slot.CARDSLEEVE, card);
          if (spec.compareTo(best) > 0) {
            best = spec.clone();
            bestCard = card;
          }
        }
      }
    }

    Map<Modeable, String> bestModes =
        modeablesNeeded.entrySet().stream()
            .collect(
                Collectors.toMap(
                    Entry::getKey,
                    entry -> {
                      if (!entry.getValue()) return "";
                      var modeable = entry.getKey();

                      if (!forcedModeables.get(modeable).isEmpty()) {
                        return forcedModeables.get(modeable);
                      }

                      MaximizerSpeculation best = null;
                      CheckedItem item =
                          new CheckedItem(modeable.getItemId(), equipScope, maxPrice, priceLevel);
                      var bestMode = modeable.getState();

                      // Check each mode in modeable to determine the best
                      for (String mode : modeable.getModes()) {
                        MaximizerSpeculation spec = new MaximizerSpeculation();
                        spec.attachment = item;
                        spec.equipment.put(modeable.getSlot(), item);
                        spec.setModeable(modeable, mode);
                        if (spec.compareTo(best) > 0) {
                          best = spec.clone();
                          bestMode = mode;
                        }
                      }

                      return bestMode;
                    }));

    SlotList<MaximizerSpeculation> speculationList = new SlotList<>(this.familiars.size());

    for (var entry : ranked.entries()) {
      List<CheckedItem> checkedItemList = entry.value();

      // If we currently have nothing equipped, also consider leaving nothing equipped
      if (!entry.isSlot()
          || EquipmentManager.getEquipment(Evaluator.toUseSlot(entry.slot()))
              == EquipmentRequest.UNEQUIP) {
        checkedItemList.add(new CheckedItem(-1, equipScope, maxPrice, priceLevel));
      }

      List<MaximizerSpeculation> specs = speculationList.get(entry);

      for (CheckedItem item : checkedItemList) {
        MaximizerSpeculation spec = new MaximizerSpeculation();
        spec.attachment = item;
        Slot useSlot;
        if (entry.isSlot()) {
          useSlot = Evaluator.toUseSlot(entry.slot());
        } else {
          spec.setFamiliar(this.familiars.get(entry.famIndex()));
          useSlot = Slot.FAMILIAR;
        }
        spec.equipment.put(useSlot, item);

        switch (item.getItemId()) {
          case ItemPool.HATSEAT:
            if (this.slots.getOrDefault(Slot.CROWNOFTHRONES, 0) < 0) {
              spec.setEnthroned(useCrownFamiliar);
            } else if (this.carriedFamiliarsNeeded > 1) {
              item.automaticFlag = true;
              spec.setEnthroned(secondBestCarriedFamiliar);
            } else {
              spec.setEnthroned(bestCarriedFamiliar);
            }
            break;
          case ItemPool.BUDDY_BJORN:
            if (this.slots.getOrDefault(Slot.BUDDYBJORN, 0) < 0) {
              spec.setBjorned(useBjornFamiliar);
            } else if (this.carriedFamiliarsNeeded > 1) {
              item.automaticFlag = true;
              spec.setBjorned(secondBestCarriedFamiliar);
            } else {
              spec.setBjorned(bestCarriedFamiliar);
            }
            break;
          case ItemPool.CARD_SLEEVE:
            {
              MaximizerSpeculation current = new MaximizerSpeculation();
              if (bestCard != null) {
                spec.equipment.put(Slot.CARDSLEEVE, bestCard);
                useCard = bestCard;
              } else {
                spec.equipment.put(Slot.CARDSLEEVE, current.equipment.get(Slot.CARDSLEEVE));
                useCard = current.equipment.get(Slot.CARDSLEEVE);
              }
              break;
            }
          case ItemPool.FOLDER_HOLDER:
          case ItemPool.REPLICA_FOLDER_HOLDER:
            {
              MaximizerSpeculation current = new MaximizerSpeculation();
              spec.equipment.put(Slot.FOLDER1, current.equipment.get(Slot.FOLDER1));
              spec.equipment.put(Slot.FOLDER2, current.equipment.get(Slot.FOLDER2));
              spec.equipment.put(Slot.FOLDER3, current.equipment.get(Slot.FOLDER3));
              spec.equipment.put(Slot.FOLDER4, current.equipment.get(Slot.FOLDER4));
              spec.equipment.put(Slot.FOLDER5, current.equipment.get(Slot.FOLDER5));
              break;
            }
          case ItemPool.COWBOY_BOOTS:
            {
              MaximizerSpeculation current = new MaximizerSpeculation();
              spec.equipment.put(Slot.BOOTSKIN, current.equipment.get(Slot.BOOTSKIN));
              spec.equipment.put(Slot.BOOTSPUR, current.equipment.get(Slot.BOOTSPUR));
              break;
            }
          default:
            {
              var modeable = Modeable.find(item);
              if (EquipmentManager.isStickerWeapon(item)) {
                MaximizerSpeculation current = new MaximizerSpeculation();
                spec.equipment.put(Slot.STICKER1, current.equipment.get(Slot.STICKER1));
                spec.equipment.put(Slot.STICKER2, current.equipment.get(Slot.STICKER2));
                spec.equipment.put(Slot.STICKER3, current.equipment.get(Slot.STICKER3));
              } else if (modeable != null) {
                var best = bestModes.getOrDefault(modeable, "");
                if (!best.isEmpty()) {
                  spec.setModeable(modeable, best);
                }
              }
              break;
            }
        }
        spec.getScore(); // force evaluation
        spec.failed = false; // individual items are not expected to fulfill all requirements

        specs.add(spec);
      }

      Collections.sort(specs);
    }

    // Compare sets which improve with the number of items equipped with the best items in the same
    // spots

    // Compare synergies with best items in the same spots, and remove automatic flag if not better
    for (Entry<String, Integer> entry : ModifierDatabase.getSynergies()) {
      String synergy = entry.getKey();
      int mask = entry.getValue();
      int index = synergy.indexOf("/");
      String itemName1 = synergy.substring(0, index);
      String itemName2 = synergy.substring(index + 1);
      int itemId1 = ItemDatabase.getItemId(itemName1);
      int itemId2 = ItemDatabase.getItemId(itemName2);
      Slot slot1 = EquipmentManager.itemIdToEquipmentType(itemId1);
      Slot slot2 = EquipmentManager.itemIdToEquipmentType(itemId2);
      CheckedItem item1 = null;
      CheckedItem item2 = null;

      // The only times the slots will be wrong for looking at speculation lists for current
      // synergies are 1 handed swords
      // They are always item 1
      int hands = EquipmentDatabase.getHands(itemId1);
      WeaponType weaponType = EquipmentDatabase.getWeaponType(itemId1);
      Slot slot1SpecLookup = slot1;
      if (hands == 1 && weaponType == WeaponType.MELEE) {
        slot1SpecLookup = Evaluator.WEAPON_1H;
      }

      if (slot1 == Slot.NONE || slot2 == Slot.NONE) {
        continue;
      }

      ListIterator<MaximizerSpeculation> sI =
          speculationList
              .get(slot1SpecLookup)
              .listIterator(speculationList.get(slot1SpecLookup).size());

      while (sI.hasPrevious() && item1 == null) {
        CheckedItem checkItem = sI.previous().attachment;
        checkItem.validate(maxPrice, priceLevel);
        if (checkItem.getName().equals(itemName1)) {
          item1 = checkItem;
        }
      }

      sI = speculationList.get(slot2).listIterator(speculationList.get(slot2).size());

      while (sI.hasPrevious() && item2 == null) {
        CheckedItem checkItem = sI.previous().attachment;
        checkItem.validate(maxPrice, priceLevel);
        if (checkItem.getName().equals(itemName2)) {
          item2 = checkItem;
        }
      }

      if (item1 == null || item2 == null) {
        continue;
      }

      // Found a synergy in our speculationList, so compare it with the best individual items

      int accCompared = 0;
      MaximizerSpeculation synergySpec = new MaximizerSpeculation();
      MaximizerSpeculation compareSpec = new MaximizerSpeculation();

      Slot newSlot1 = slot1;
      int compareItemNo =
          slot1 == Slot.ACCESSORY1
              ? speculationList.get(slot1SpecLookup).size() - 3
              : speculationList.get(slot1SpecLookup).size() - 1;
      do {
        CheckedItem compareItem =
            speculationList.get(slot1SpecLookup).get(compareItemNo).attachment;
        if (compareItem.conditionalFlag) {
          compareItemNo--;
        } else {
          compareSpec.equipment.put(
              newSlot1, speculationList.get(slot1SpecLookup).get(compareItemNo).attachment);
          break;
        }
        if (compareItemNo < 0) {
          compareSpec.equipment.put(newSlot1, EquipmentRequest.UNEQUIP);
          break;
        }
      } while (compareItemNo >= 0);
      if (slot1 == Slot.ACCESSORY1) {
        accCompared++;
      }
      synergySpec.equipment.put(newSlot1, item1);

      Slot newSlot2 = jumpAccessories(slot2, accCompared);
      compareItemNo =
          slot2 == Slot.ACCESSORY1
              ? speculationList.get(slot2).size() - 2
              : speculationList.get(slot2).size() - 1;
      do {
        CheckedItem compareItem = speculationList.get(slot2).get(compareItemNo).attachment;
        if (compareItem.conditionalFlag
            || compareItem.getName().equals(compareSpec.equipment.get(newSlot1).getName())) {
          compareItemNo--;
        } else {
          compareSpec.equipment.put(
              newSlot2, speculationList.get(slot2).get(compareItemNo).attachment);
          break;
        }
        if (compareItemNo < 0) {
          compareSpec.equipment.put(newSlot2, EquipmentRequest.UNEQUIP);
          break;
        }
      } while (compareItemNo >= 0);
      synergySpec.equipment.put(newSlot2, item2);

      if (synergySpec.compareTo(compareSpec) <= 0 || synergySpec.failed) {
        // Not useful, so remove it's automatic flag so it won't be put forward unless it's good
        // enough in it's own right
        sI =
            speculationList
                .get(slot1SpecLookup)
                .listIterator(speculationList.get(slot1SpecLookup).size());

        while (sI.hasPrevious()) {
          MaximizerSpeculation spec = sI.previous();
          CheckedItem checkItem = spec.attachment;
          checkItem.validate(maxPrice, priceLevel);
          if (checkItem.getName().equals(itemName1)) {
            spec.attachment.automaticFlag = false;
            break;
          }
        }

        sI = speculationList.get(slot2).listIterator(speculationList.get(slot2).size());

        while (sI.hasPrevious()) {
          MaximizerSpeculation spec = sI.previous();
          CheckedItem checkItem = spec.attachment;
          checkItem.validate(maxPrice, priceLevel);
          if (checkItem.getName().equals(itemName2)) {
            spec.attachment.automaticFlag = false;
            break;
          }
        }
      }
    }

    // However, that's only two item Synergies, and there are two three item synergies effectively.
    // Ugly hack to reinstate them if necessary. They are always accessories, which simplifies
    // things.
    int count = 0;
    while (count < 2) {
      int itemId1;
      int itemId2;
      int itemId3;
      CheckedItem item1 = null;
      CheckedItem item2 = null;
      CheckedItem item3 = null;
      Slot slot = Slot.ACCESSORY1;

      if (count == 0) {
        itemId1 = ItemPool.MONSTROUS_MONOCLE;
        itemId2 = ItemPool.MUSTY_MOCCASINS;
        itemId3 = ItemPool.MOLTEN_MEDALLION;
      } else {
        itemId1 = ItemPool.BRAZEN_BRACELET;
        itemId2 = ItemPool.BITTER_BOWTIE;
        itemId3 = ItemPool.BEWITCHING_BOOTS;
      }
      count++;

      ListIterator<MaximizerSpeculation> sI =
          speculationList.get(slot).listIterator(speculationList.get(slot).size());

      while (sI.hasPrevious()) {
        CheckedItem checkItem = sI.previous().attachment;
        checkItem.validate(maxPrice, priceLevel);
        if (checkItem.getItemId() == itemId1) {
          item1 = checkItem;
        } else if (checkItem.getItemId() == itemId2) {
          item2 = checkItem;
        } else if (checkItem.getItemId() == itemId3) {
          item3 = checkItem;
        }
        if (item1 != null && item2 != null && item3 != null) {
          break;
        }
      }

      if (item1 == null || item2 == null || item3 == null) {
        continue;
      }

      // All three in our speculationList, so compare it with the best 3 accessories items

      MaximizerSpeculation synergySpec = new MaximizerSpeculation();
      MaximizerSpeculation compareSpec = new MaximizerSpeculation();

      int compareItemNo = speculationList.get(slot).size() - 1;
      compareSpec.equipment.put(slot, EquipmentRequest.UNEQUIP);
      compareSpec.equipment.put(Slot.ACCESSORY2, EquipmentRequest.UNEQUIP);
      compareSpec.equipment.put(Slot.ACCESSORY3, EquipmentRequest.UNEQUIP);
      Slot newSlot = slot;
      do {
        CheckedItem compareItem = speculationList.get(slot).get(compareItemNo).attachment;
        if (!compareItem.conditionalFlag) {
          compareSpec.equipment.put(
              newSlot, speculationList.get(slot).get(compareItemNo).attachment);
          newSlot = incrementAccessory(newSlot);
        }
        compareItemNo--;
      } while (compareItemNo >= 0 && newSlot != Slot.NONE);
      synergySpec.equipment.put(slot, item1);
      synergySpec.equipment.put(Slot.ACCESSORY2, item2);
      synergySpec.equipment.put(Slot.ACCESSORY3, item3);

      if (synergySpec.compareTo(compareSpec) > 0 && !synergySpec.failed) {
        // Useful, so automatic flag it again
        sI = speculationList.get(slot).listIterator(speculationList.get(slot).size());

        int found = 0;
        while (sI.hasPrevious() && found < 3) {
          MaximizerSpeculation spec = sI.previous();
          CheckedItem checkItem = spec.attachment;
          checkItem.validate(maxPrice, priceLevel);
          if (checkItem.getItemId() == itemId1) {
            spec.attachment.automaticFlag = true;
            found++;
          } else if (checkItem.getItemId() == itemId2) {
            spec.attachment.automaticFlag = true;
            found++;
          } else if (checkItem.getItemId() == itemId3) {
            spec.attachment.automaticFlag = true;
            found++;
          }
        }
      }
    }

    // Compare outfits with best item in the same spot, and remove if not better
    // Compare the accessories to the worst ones, not the best
    StringBuilder outfitSummary = new StringBuilder();
    outfitSummary.append("Outfits [");
    int outfitCount = 0;
    for (Integer i : usefulOutfits.keySet()) {
      if (usefulOutfits.get(i)) {
        int accCount = 0;
        MaximizerSpeculation outfitSpec = new MaximizerSpeculation();
        MaximizerSpeculation compareSpec = new MaximizerSpeculation();
        // Get pieces of outfit
        SpecialOutfit outfit = EquipmentDatabase.getOutfit(i);
        AdventureResult[] pieces = outfit.getPieces();
        for (AdventureResult piece : pieces) {
          int outfitItemId = piece.getItemId();
          Slot slot = EquipmentManager.itemIdToEquipmentType(outfitItemId);
          // For some items, Evaluator uses a different slot
          // I don't think any outfits use an offhand weapon or watch though?
          int hands = EquipmentDatabase.getHands(outfitItemId);
          if (hands == 1) {
            slot = Evaluator.WEAPON_1H;
          }

          // Compare outfit with best individual non conditional item that hasn't previously been
          // used
          // For accessories compare with 3rd best for first accessory, 2nd best for second
          // accessory, best for third
          Slot newSlot = jumpAccessories(slot, accCount);
          // if we're comparing 1-handed weapons, assign the spec slot as weapon
          newSlot = newSlot == Evaluator.WEAPON_1H ? Slot.WEAPON : newSlot;
          int compareItemNo = speculationList.get(slot).size() - 1;
          int accSkip = slot == Slot.ACCESSORY1 ? 2 - accCount : 0;
          while (compareItemNo >= 0) {
            CheckedItem compareItem = speculationList.get(slot).get(compareItemNo).attachment;
            if (compareItem.conditionalFlag) {
              compareItemNo--;
            } else if (accSkip > 0) {
              // Valid item, but we're looking for 2nd or 3rd best non-conditional
              compareItemNo--;
              accSkip--;
            } else {
              compareSpec.equipment.put(newSlot, compareItem);
              break;
            }
            if (compareItemNo < 0) {
              compareSpec.equipment.put(newSlot, EquipmentRequest.UNEQUIP);
              break;
            }
          }
          CheckedItem outfitItem = new CheckedItem(outfitItemId, equipScope, maxPrice, priceLevel);
          outfitSpec.equipment.put(newSlot, outfitItem);
        }
        if (outfitSpec.compareTo(compareSpec) <= 0 && !this.posOutfits.contains(outfit.getName())) {
          usefulOutfits.put(i, false);
        } else {
          if (outfitCount > 0) {
            outfitSummary.append(", ");
          }
          outfitSummary.append(outfit.toString());
          outfitCount++;
        }
      }
    }
    if (this.dump > 0) {
      outfitSummary.append("]");
      RequestLogger.printLine(outfitSummary.toString());
    }

    for (var entry : ranked.entries()) {
      List<CheckedItem> checkedItemList = ranked.get(entry);
      var automaticEntry = automatic.get(entry);

      if (this.dump > 0) {
        RequestLogger.printLine(
            "SLOT " + (entry.isSlot() ? entry.slot() : "BONUS FAMILIAR #" + entry.famIndex()));
      }

      if (this.dump > 1) {
        RequestLogger.printLine(speculationList.get(entry).toString());
      }

      // Do we have any required items for the slot?
      int total = 0;
      for (CheckedItem item : checkedItemList) {
        if (item.requiredFlag) {
          automatic.get(entry).add(item);
          // Don't increase total if it's one of the required flagged foldables by Evaluator rather
          // than user
          int itemId = item.getItemId();
          if (itemId != ItemPool.BROKEN_CHAMPAGNE && itemId != ItemPool.MAKESHIFT_GARBAGE_SHIRT) {
            ++total;
          }
        }
      }

      int useful = entry.isSlot() ? this.maxUseful(entry.slot()) : 1;

      // If slots already handled by required items, we're done with the slot
      if (useful > total) {
        ListIterator<MaximizerSpeculation> speculationIterator =
            speculationList.get(entry).listIterator(speculationList.get(entry).size());

        int beeotches = 0;
        int beeosity = 0;
        int b;

        while (speculationIterator.hasPrevious()) {
          CheckedItem item = speculationIterator.previous().attachment;
          item.validate(maxPrice, priceLevel);

          // If we only need as many fold items as we have, then we can
          // count them against the items we need to pass through
          FoldGroup group = ItemDatabase.getFoldGroup(item.getName());
          int foldItemsNeeded = 0;
          if (group != null && Preferences.getBoolean("maximizerFoldables")) {
            foldItemsNeeded += Math.max(item.getCount(), useful);
            // How many times have we already used this fold item?
            for (var checkSlot : SlotSet.SLOTS) {
              if (entry.isSlot() && checkSlot.ordinal() >= entry.slot().ordinal()) break;
              List<CheckedItem> checkItemList = automatic.get(checkSlot);
              if (checkItemList != null) {
                for (CheckedItem checkItem : checkItemList) {
                  FoldGroup checkGroup = ItemDatabase.getFoldGroup(checkItem.getName());
                  if (checkGroup != null
                      && group.names.getFirst().equals(checkGroup.names.getFirst())) {
                    foldItemsNeeded += Math.max(checkItem.getCount(), this.maxUseful(checkSlot));
                  }
                }
              }
            }
            // And how many times do we expect to use them for the rest of the slots?
            if (entry.isSlot() && entry.slot().ordinal() < Slot.FAMILIAR.ordinal()) {
              for (var checkSlot :
                  EnumSet.range(Slot.byOrdinal(entry.slot().ordinal() + 1), Slot.FAMILIAR)) {
                ListIterator<MaximizerSpeculation> checkIterator =
                    speculationList
                        .get(checkSlot)
                        .listIterator(speculationList.get(checkSlot).size());
                int usefulCheckCount = this.maxUseful(checkSlot);
                while (checkIterator.hasPrevious()) {
                  CheckedItem checkItem = checkIterator.previous().attachment;
                  FoldGroup checkGroup = ItemDatabase.getFoldGroup(checkItem.getName());
                  if (checkGroup != null
                      && group.names.getFirst().equals(checkGroup.names.getFirst())) {
                    if (usefulCheckCount > 0 || checkItem.requiredFlag) {
                      foldItemsNeeded += Math.max(checkItem.getCount(), this.maxUseful(checkSlot));
                    }
                  } else if (checkItem.automaticFlag || !checkItem.conditionalFlag) {
                    usefulCheckCount--;
                  }
                }
              }
            }
          }

          if (item.getCount() == 0) {
            // If we don't have one, and they aren't nothing, skip
            continue;
          }
          if (KoLCharacter.inBeecore()
              && (b = KoLCharacter.getBeeosity(item.getName())) > 0) { // This item is a beeotch!
            // Don't count it towards the number of items desired
            // in this slot's shortlist, since it may turn out to be
            // advantageous to use up all our allowed beeosity on
            // other slots.
            if (item.automaticFlag) {
              if (!automaticEntry.contains(item)) {
                automaticEntry.add(item);
              }
              beeotches += item.getCount();
              beeosity += b * item.getCount();
            } else if (total < useful && beeotches < useful && beeosity < this.beeosity) {
              if (!automaticEntry.contains(item)) {
                automaticEntry.add(item);
              }
              beeotches += item.getCount();
              beeosity += b * item.getCount();
            }
          } else if (item.automaticFlag) {
            if (!automaticEntry.contains(item)) {
              automaticEntry.add(item);
              if (!item.conditionalFlag && item.getCount() >= foldItemsNeeded) {
                total += item.getCount();
              }
            }
          } else if (total < useful) {
            if (!automaticEntry.contains(item)) {
              automaticEntry.add(item);
              if (!item.conditionalFlag && item.getCount() >= foldItemsNeeded) {
                total += item.getCount();
              }
            }
          }
        }
      }

      // Blunt object fix for only having a foldable that might be needed elsewhere
      if (automaticEntry.size() == 1
          && ItemDatabase.getFoldGroup(automaticEntry.getFirst().getName()) != null) {
        automaticEntry.add(new CheckedItem(-1, equipScope, maxPrice, priceLevel));
      }

      if (this.dump > 0) {
        RequestLogger.printLine(automaticEntry.toString());
      }
    }

    automatic.get(Slot.WEAPON).addAll(automatic.get(Evaluator.WEAPON_1H));
    automatic.get(Evaluator.OFFHAND_MELEE).addAll(automatic.get(Slot.OFFHAND));
    automatic.get(Evaluator.OFFHAND_RANGED).addAll(automatic.get(Slot.OFFHAND));

    MaximizerSpeculation spec = new MaximizerSpeculation();
    // The threshold in the slots array that indicates that a slot
    // should be considered will be either >= 1 or >= 0, depending
    // on whether inclusive or exclusive slot specs were used.
    for (int thresh = 1; ; --thresh) {
      if (thresh < 0) return; // no slots enabled
      boolean anySlots = false;
      for (var slot : SlotSet.SLOTS) {
        if (this.slots.getOrDefault(slot, 0) >= thresh) {
          spec.equipment.put(slot, null);
          anySlots = true;
        }
      }
      if (anySlots) break;
    }

    if (spec.equipment.get(Slot.OFFHAND) != null) {
      this.hands = 1;
      automatic.set(Slot.WEAPON, automatic.get(Evaluator.WEAPON_1H));

      Iterator<AdventureResult> i = outfitPieces.keySet().iterator();
      while (i.hasNext()) {
        id = i.next().getItemId();
        if (EquipmentManager.itemIdToEquipmentType(id) == Slot.WEAPON
            && EquipmentDatabase.getHands(id) > 1) {
          i.remove();
        }
      }
    }

    bestModes.forEach(
        (modeable, mode) -> {
          Set<Slot> backupSlots = EnumSet.noneOf(Slot.class);
          backupSlots.add(modeable.getSlot());

          if (modeable.getSlot() == Slot.ACCESSORY1) {
            backupSlots.add(Slot.ACCESSORY2);
            backupSlots.add(Slot.ACCESSORY3);
          }

          if (this.familiars.stream().anyMatch(f -> f.canEquip(modeable.getItem()))) {
            backupSlots.add(Slot.FAMILIAR);
          }

          if (backupSlots.stream().anyMatch(s -> spec.equipment.get(s) == null)) {
            spec.setModeable(modeable, mode);
          }
        });

    spec.tryAll(
        this.familiars,
        this.carriedFamiliars,
        usefulOutfits,
        outfitPieces,
        automatic,
        useCard,
        useCrownFamiliar,
        useBjornFamiliar);
  }

  private boolean isCatUseful(double nullScore, String catName) {
    Modifiers mods = ModifierDatabase.getModifiers(ModifierType.MAX_CAT, catName);
    return mods != null && this.getScore(mods) - nullScore > 0.0;
  }

  private Slot jumpAccessories(Slot base, int jumpIfFromStart) {
    if (base == Slot.ACCESSORY1) {
      if (jumpIfFromStart == 0) {
        return Slot.ACCESSORY1;
      } else if (jumpIfFromStart == 1) {
        return Slot.ACCESSORY2;
      } else {
        return Slot.ACCESSORY3;
      }
    } else {
      return base;
    }
  }

  private Slot incrementAccessory(Slot base) {
    if (base == Slot.ACCESSORY1) {
      return Slot.ACCESSORY2;
    } else if (base == Slot.ACCESSORY2) {
      return Slot.ACCESSORY3;
    } else if (base == Slot.ACCESSORY3) {
      // sentinel value
      return Slot.NONE;
    }
    throw new IllegalStateException("Unexpected value: " + base);
  }
}
