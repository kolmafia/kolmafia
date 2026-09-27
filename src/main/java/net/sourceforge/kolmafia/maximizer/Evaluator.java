package net.sourceforge.kolmafia.maximizer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.ExpressionOverrides;
import net.sourceforge.kolmafia.FamiliarData;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLCharacter.TurtleBlessing;
import net.sourceforge.kolmafia.KoLCharacter.TurtleBlessingLevel;
import net.sourceforge.kolmafia.KoLConstants.WeaponType;
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
import net.sourceforge.kolmafia.modifiers.Modifier;
import net.sourceforge.kolmafia.modifiers.StringModifier;
import net.sourceforge.kolmafia.objectpool.EffectPool;
import net.sourceforge.kolmafia.objectpool.FamiliarPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.objectpool.SkillPool;
import net.sourceforge.kolmafia.persistence.EquipmentDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase.FoldGroup;
import net.sourceforge.kolmafia.persistence.ModifierDatabase;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.EquipmentRequest;
import net.sourceforge.kolmafia.request.StandardRequest;
import net.sourceforge.kolmafia.session.EquipmentManager;

@SuppressWarnings("incomplete-switch")
public class Evaluator {
  public boolean failed;
  boolean exceeded;
  private Evaluator tiebreaker;
  private final MaximizerExpression expression = new MaximizerExpression();
  private List<ScoreModifier> activeScoreModifiers = List.of();
  private boolean shouldPredictDerivedModifiers;
  private final List<FamiliarData> carriedFamiliars = new ArrayList<>();
  private int carriedFamiliarsNeeded = 0;
  private boolean cardNeeded = false;
  private final Map<Modeable, Boolean> modeablesNeeded = Modeable.getBooleanMap();

  private record ScoreModifier(Modifier modifier, double weight, double min, double max) {}

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
    for (FamiliarData familiar : this.expression.familiars) {
      if (familiar.getId() == id) {
        return 1;
      }
    }
    return 0;
  }

  private int maxUseful(Slot slot) {
    return switch (slot) {
      case /* Evaluator.WEAPON_1H */ STICKER3 ->
          1
              + relevantSkill(SkillPool.DOUBLE_FISTED_SKULL_SMASHING)
              + this.relevantFamiliar(FamiliarPool.HAND);
      case OFFHAND -> 1 + this.relevantFamiliar(FamiliarPool.LEFT_HAND);
      case ACCESSORY1 -> 3;
      case FAMILIAR ->
          // Familiar items include weapons, hats and pants, make sure we have enough to consider
          // for
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
      case /* Evaluator.OFFHAND_MELEE */ ACCESSORY2, /* Evaluator.OFFHAND_RANGED */ ACCESSORY3 ->
          Slot.OFFHAND;
      case /* Evaluator.WEAPON_1H */ STICKER3 -> Slot.WEAPON;
      default -> slot;
    };
  }

  private Evaluator() {}

  public Evaluator(String expr) {
    this();

    Evaluator tiebreaker = new Evaluator();
    this.tiebreaker = tiebreaker;
    tiebreaker.expression.parse(MaximizerExpression.TIEBREAKER);
    tiebreaker.initializeScoreModifiers();

    this.expression.min.putAll(tiebreaker.expression.min);
    this.expression.max.putAll(tiebreaker.expression.max);
    this.expression.parse(expr);
    this.initializeScoreModifiers();
  }

  private void initializeScoreModifiers() {
    var active = new ArrayList<ScoreModifier>();
    this.shouldPredictDerivedModifiers = false;
    for (var modifier : DoubleModifier.DOUBLE_MODIFIERS) {
      double weight = this.expression.weight.getOrDefault(modifier, 0.0);
      double min = this.expression.min.get(modifier);
      if (weight == 0.0 && min == Double.NEGATIVE_INFINITY) {
        continue;
      }

      active.add(new ScoreModifier(modifier, weight, min, this.expression.max.get(modifier)));
      if (modifier == DoubleModifier.MUS
          || modifier == DoubleModifier.MYS
          || modifier == DoubleModifier.MOX
          || modifier == DoubleModifier.HP
          || modifier == DoubleModifier.MP) {
        this.shouldPredictDerivedModifiers = true;
      }
    }
    for (var modifier : MaximizerExpression.OSITY_MODIFIERS) {
      double weight = this.expression.weight.getOrDefault(modifier, 0.0);
      double min = this.expression.min.get(modifier);
      if (weight != 0.0 || min != Double.NEGATIVE_INFINITY) {
        active.add(new ScoreModifier(modifier, weight, min, this.expression.max.get(modifier)));
      }
    }
    this.activeScoreModifiers = List.copyOf(active);
  }

  public double getScore(
      Modifiers mods, Map<Slot, AdventureResult> equipment, Map<Modeable, String> modeables) {
    this.failed = false;
    this.exceeded = false;
    var predicted = this.shouldPredictDerivedModifiers ? mods.predict() : null;

    double score = 0.0;
    for (var scoreModifier : this.activeScoreModifiers) {
      var mod = scoreModifier.modifier();
      double weight = scoreModifier.weight();
      double min = scoreModifier.min();
      double val = 0.0;
      double max = scoreModifier.max();
      if (mod instanceof BitmapModifier bitmapModifier) {
        val = mods.getBitmap(bitmapModifier);
      } else if (mod instanceof DoubleModifier) {
        var doubleModifier = (DoubleModifier) mod;
        val = mods.getDouble(doubleModifier);
        switch (doubleModifier) {
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
      }
      if (val < min) this.failed = true;
      score += weight * Math.min(val, max);
    }
    if (this.expression.stinkycheese > 0) {
      int val = mods.getBitmap(BitmapModifier.STINKYCHEESE);
      score += this.expression.stinkycheese * val;
    }
    if (!this.expression.bonuses.isEmpty()) {
      for (AdventureResult item : equipment.values()) {
        MaximizerExpression.ItemBonus itemBonus = this.expression.bonuses.get(item);
        // Add the base bonus
        if (itemBonus == null) continue;
        score += itemBonus.base();
        // If it's a modeable and has a bonus for it
        var modeable = Modeable.find(item);
        if (modeable == null) continue;
        var mode = modeables.get(modeable);
        if (mode == null) continue;
        Double bonus = itemBonus.modes().get(mode);
        if (bonus != null) score += bonus;
      }
    }
    if (!this.expression.modBonuses.isEmpty()) {
      for (AdventureResult item : equipment.values()) {
        Modifiers itemMods = ModifierDatabase.getItemModifiers(item.getItemId());
        if (itemMods == null) {
          continue;
        }
        Modifiers modeMods = null;
        Modeable modeable = Modeable.find(item);
        if (modeable != null) {
          modeMods =
              ModifierDatabase.getModifiers(modeable.getModifierType(), modeables.get(modeable));
        }

        for (Entry<BooleanModifier, Double> modBonus : this.expression.modBonuses.entrySet()) {
          if (itemMods.getBoolean(modBonus.getKey())
              || modeMods != null && modeMods.getBoolean(modBonus.getKey())) {
            score += modBonus.getValue();
          }
        }
      }
    }
    if (!this.expression.bonusFunc.isEmpty()) {
      for (MaximizerExpression.BonusFunction func : this.expression.bonusFunc) {
        for (AdventureResult item : equipment.values()) {
          score += func.bonusFunction().apply(item) * func.weight();
        }
      }
    }
    // Add fudge factor for Rollover Effect
    if (mods.hasString(StringModifier.ROLLOVER_EFFECT)) {
      score += 0.01f;
    }
    if (score < this.expression.totalMin) this.failed = true;
    if (score >= this.expression.totalMax) this.exceeded = true;
    if (!this.failed
        && !this.expression.booleanMask.isEmpty()
        && !mods.getBooleans(this.expression.booleanMask).equals(this.expression.booleanValue)) {
      this.failed = true;
    }
    return score;
  }

  public double getScore(Modifiers mods) {
    return this.getScore(mods, Map.of(), Map.of());
  }

  void checkEquipment(Modifiers mods, Map<Slot, AdventureResult> equipment, int beeosity) {
    boolean outfitSatisfied = this.expression.posOutfits.isEmpty();
    boolean equipSatisfied = this.expression.posEquip.isEmpty();
    if (!this.failed && !this.expression.posEquip.isEmpty()) {
      equipSatisfied = true;
      for (AdventureResult item : this.expression.posEquip) {
        if (!KoLCharacter.hasEquipped(equipment, item)) {
          equipSatisfied = false;
          break;
        }
      }
    }
    if (!this.failed) {
      String outfit = mods.getString(StringModifier.OUTFIT);
      if (this.expression.negOutfits.contains(outfit)) {
        this.failed = true;
      } else {
        outfitSatisfied =
            this.expression.posOutfits.contains(outfit) || this.expression.posOutfits.isEmpty();
      }
    }
    // negEquip is not checked, since enumerateEquipment should make it
    // impossible for such items to be chosen.
    if (!outfitSatisfied || !equipSatisfied) {
      this.failed = true;
    }
    if (beeosity > this.expression.beeosity) {
      this.failed = true;
    }
  }

  double getTiebreaker(Modifiers mods) {
    if (this.expression.noTiebreaker) return 0.0;
    return this.tiebreaker.getScore(mods);
  }

  boolean isUsingTiebreaker() {
    return !this.expression.noTiebreaker;
  }

  int melee() {
    return this.expression.melee;
  }

  boolean isWeaponTypeRequired() {
    return this.expression.requireClub
        || this.expression.requireUtensil
        || this.expression.requireSword
        || this.expression.requireKnife
        || this.expression.requireAccordion;
  }

  boolean isShieldRequired() {
    return this.expression.requireShield;
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
    EnumSet<BooleanModifier> bools = mods.getBooleans(this.expression.booleanMask);
    if (!this.expression.booleanValue.containsAll(bools)) return Constraint.VIOLATES;
    if (!bools.isEmpty()) return Constraint.MEETS;
    return Constraint.IRRELEVANT;
  }

  public static boolean cannotGainEffect(int effectId) {
    // Return true if effect cannot be gained due to current other effects or class
    return switch (effectId) {
      case EffectPool.NEARLY_SILENT_HUNTING -> KoLCharacter.isSealClubber();
      case EffectPool.SILENT_HUNTING, EffectPool.BARREL_CHESTED -> !KoLCharacter.isSealClubber();
      case EffectPool.BOON_OF_SHE_WHO_WAS ->
          KoLCharacter.getBlessingType() != TurtleBlessing.SHE_WHO_WAS
              || KoLCharacter.getBlessingLevel() == TurtleBlessingLevel.AVATAR;
      case EffectPool.BOON_OF_THE_STORM_TORTOISE ->
          KoLCharacter.getBlessingType() != TurtleBlessing.STORM
              || KoLCharacter.getBlessingLevel() == TurtleBlessingLevel.AVATAR;
      case EffectPool.BOON_OF_THE_WAR_SNAPPER ->
          KoLCharacter.getBlessingType() != TurtleBlessing.WAR
              || KoLCharacter.getBlessingLevel() == TurtleBlessingLevel.AVATAR;
      case EffectPool.AVATAR_OF_SHE_WHO_WAS ->
          KoLCharacter.getBlessingType() != TurtleBlessing.SHE_WHO_WAS
              || KoLCharacter.getBlessingLevel() != TurtleBlessingLevel.GLORIOUS_BLESSING;
      case EffectPool.AVATAR_OF_THE_STORM_TORTOISE ->
          KoLCharacter.getBlessingType() != TurtleBlessing.STORM
              || KoLCharacter.getBlessingLevel() != TurtleBlessingLevel.GLORIOUS_BLESSING;
      case EffectPool.AVATAR_OF_THE_WAR_SNAPPER ->
          KoLCharacter.getBlessingType() != TurtleBlessing.WAR
              || KoLCharacter.getBlessingLevel() != TurtleBlessingLevel.GLORIOUS_BLESSING;
      case EffectPool.BLESSING_OF_SHE_WHO_WAS ->
          !KoLCharacter.isTurtleTamer()
              || KoLCharacter.getBlessingType() == TurtleBlessing.SHE_WHO_WAS
              || KoLCharacter.getBlessingLevel() == TurtleBlessingLevel.PARIAH
              || KoLCharacter.getBlessingLevel() == TurtleBlessingLevel.AVATAR;
      case EffectPool.BLESSING_OF_THE_STORM_TORTOISE ->
          !KoLCharacter.isTurtleTamer()
              || KoLCharacter.getBlessingType() == TurtleBlessing.STORM
              || KoLCharacter.getBlessingLevel() == TurtleBlessingLevel.PARIAH
              || KoLCharacter.getBlessingLevel() == TurtleBlessingLevel.AVATAR;
      case EffectPool.BLESSING_OF_THE_WAR_SNAPPER ->
          !KoLCharacter.isTurtleTamer()
              || KoLCharacter.getBlessingType() == TurtleBlessing.WAR
              || KoLCharacter.getBlessingLevel() == TurtleBlessingLevel.PARIAH
              || KoLCharacter.getBlessingLevel() == TurtleBlessingLevel.AVATAR;
      case EffectPool.DISDAIN_OF_SHE_WHO_WAS,
          EffectPool.DISDAIN_OF_THE_STORM_TORTOISE,
          EffectPool.DISDAIN_OF_THE_WAR_SNAPPER ->
          KoLCharacter.isTurtleTamer();
      case EffectPool.BARREL_OF_LAUGHS -> !KoLCharacter.isTurtleTamer();
      case EffectPool.FLIMSY_SHIELD_OF_THE_PASTALORD,
          EffectPool.BLOODY_POTATO_BITS,
          EffectPool.SLINKING_NOODLE_GLOB,
          EffectPool.WHISPERING_STRANDS,
          EffectPool.MACARONI_COATING,
          EffectPool.PENNE_FEDORA,
          EffectPool.PASTA_EYEBALL,
          EffectPool.SPICE_HAZE,
          EffectPool.LEGENDARY_BLOODY_POTATO_BITS,
          EffectPool.LEGENDARY_SLINKING_NOODLE_GLOB,
          EffectPool.LEGENDARY_WHISPERING_STRANDS,
          EffectPool.LEGENDARY_MACARONI_COATING,
          EffectPool.LEGENDARY_PENNE_FEDORA,
          EffectPool.LEGENDARY_PASTA_EYEBALL,
          EffectPool.LEGENDARY_SPICE_HAZE ->
          KoLCharacter.isPastamancer();
      case EffectPool.SHIELD_OF_THE_PASTALORD, EffectPool.PORK_BARREL ->
          !KoLCharacter.isPastamancer();
      case EffectPool.BLOOD_SUGAR_SAUCE_MAGIC,
          EffectPool.SOULERSKATES,
          EffectPool.WARLOCK_WARSTOCK_WARBARREL ->
          !KoLCharacter.isSauceror();
      case EffectPool.BLOOD_SUGAR_SAUCE_MAGIC_LITE -> KoLCharacter.isSauceror();
      case EffectPool.DOUBLE_BARRELED -> !KoLCharacter.isDiscoBandit();
      case EffectPool.BEER_BARREL_POLKA -> !KoLCharacter.isAccordionThief();
      case EffectPool.UNMUFFLED ->
          !Preferences.getString("peteMotorbikeMuffler").equals("Extra-Loud Muffler");
      case EffectPool.MUFFLED ->
          !Preferences.getString("peteMotorbikeMuffler").equals("Extra-Quiet Muffler");
      default -> false;
    };
  }

  void enumerateEquipment(EquipScope equipScope, int maxPrice, PriceLevel priceLevel)
      throws MaximizerInterruptedException {
    // Items automatically considered regardless of their score -
    // synergies, hobo power, brimstone, etc.
    SlotList<CheckedItem> automatic = new SlotList<>(this.expression.familiars.size());
    // Items to be considered based on their score
    SlotList<CheckedItem> ranked = new SlotList<>(this.expression.familiars.size());

    double nullScore = this.getScore(new Modifiers());

    Map<Integer, Boolean> usefulOutfits = new HashMap<>();
    Map<AdventureResult, AdventureResult> outfitPieces = new HashMap<>();
    for (var outfitEntry : EquipmentDatabase.normalOutfits.entrySet()) {
      var i = outfitEntry.getKey();
      var outfit = outfitEntry.getValue();
      if (outfit == null) continue;
      if (this.expression.negOutfits.contains(outfit.getName())) continue;
      if (this.expression.posOutfits.contains(outfit.getName())) {
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
      if (this.expression.negEquip.contains(preItem)) continue;
      if (KoLCharacter.inBeecore()
          && KoLCharacter.getBeeosity(name)
              > this.expression.beeosity) { // too beechin' all by itself!
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
            && (item.automaticFlag
                || this.expression.posEquip.contains(item)
                // Modeable items are already automaticFlag, avoids a needless lookup
                || this.getScore(familiarMods, Map.of(Slot.FAMILIAR, item), Map.of()) - nullScore
                    > 0.0)) {
          ranked.get(Slot.FAMILIAR).add(item);
        }
      }
      for (int f = this.expression.familiars.size() - 1; f >= 0; --f) {
        FamiliarData fam = this.expression.familiars.get(f);
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
            && (item.automaticFlag
                || this.expression.posEquip.contains(item)
                // Modeable items are already automaticFlag, avoids a needless lookup
                || this.getScore(familiarMods, Map.of(Slot.FAMILIAR, item), Map.of()) - nullScore
                    > 0.0)) {
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

      if (!StandardRequest.isAllowed(RestrictedItemType.ITEMS, item.getName())) {
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
            if (this.expression.hands == 1 && hands != 1) {
              continue;
            }
            if (this.expression.hands > 1 && hands < this.expression.hands) {
              continue;
            }
            WeaponType weaponType = EquipmentDatabase.getWeaponType(id);
            if (this.expression.melee > 0 && weaponType != WeaponType.MELEE) {
              continue;
            }
            if (this.expression.melee < 0 && weaponType != WeaponType.RANGED) {
              continue;
            }
            String type = EquipmentDatabase.getItemType(id);
            if (this.expression.weaponType != null && !type.contains(this.expression.weaponType)) {
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
              } else if (!this.expression.requireShield && !EquipmentDatabase.isMainhandOnly(id)) {
                switch (weaponType) {
                  case MELEE -> auxSlot = Evaluator.OFFHAND_MELEE;
                  case RANGED -> auxSlot = Evaluator.OFFHAND_RANGED;
                  case NONE -> {}
                }
              }
            }
            if (this.expression.requireClub && !EquipmentDatabase.isClub(id)) {
              slot = auxSlot;
            }
            if (this.expression.requireUtensil && !EquipmentDatabase.isUtensil(id)) {
              slot = auxSlot;
            }
            if (this.expression.requireSword && !EquipmentDatabase.isSword(id)) {
              slot = auxSlot;
            }
            if (this.expression.requireKnife && !EquipmentDatabase.isKnife(id)) {
              slot = auxSlot;
            }
            if (this.expression.requireAccordion && !EquipmentDatabase.isAccordion(id)) {
              slot = auxSlot;
            }
            if (this.expression.effective) {
              if (id != ItemPool.FOURTH_SABER
                  && id != ItemPool.REPLICA_FOURTH_SABER
                  && !ModifierDatabase.getBooleanModifier(
                      ModifierType.ITEM, id, BooleanModifier.ATTACKS_CANT_MISS)) {
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
                && this.expression.weight.getOrDefault(DoubleModifier.ITEMDROP, 0.0) > 0
                && (Preferences.getInteger("garbageChampagneCharge") > 0
                    || !Preferences.getBoolean("_garbageItemChanged"))) {
              // This is always going to be worth including if useful
              item.requiredFlag = true;
              item.automaticFlag = true;
              break gotItem;
            }
            break;

          case OFFHAND:
            if (this.expression.requireShield
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
                && (this.expression.weight.getOrDefault(DoubleModifier.EXPERIENCE, 0.0) > 0
                    || this.expression.weight.getOrDefault(DoubleModifier.MUS_EXPERIENCE, 0.0) > 0
                    || this.expression.weight.getOrDefault(DoubleModifier.MYS_EXPERIENCE, 0.0) > 0
                    || this.expression.weight.getOrDefault(DoubleModifier.MOX_EXPERIENCE, 0.0) > 0)
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
            && this.expression.current) { // Make sure the current item in each slot is considered
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

        if (((id == ItemPool.HATSEAT
                    && this.expression.slots.getOrDefault(Slot.CROWNOFTHRONES, 0) >= 0)
                || (id == ItemPool.BUDDY_BJORN
                    && this.expression.slots.getOrDefault(Slot.BUDDYBJORN, 0) >= 0))
            && !KoLCharacter.isSneakyPete()
            && !KoLCharacter.inAxecore()
            && !KoLCharacter.isJarlsberg()) {
          this.carriedFamiliarsNeeded++;
        }

        if (id == ItemPool.CARD_SLEEVE
            && this.expression.slots.getOrDefault(Slot.CARDSLEEVE, 0) >= 0) {
          this.cardNeeded = true;
        }

        if (id == ItemPool.VAMPYRIC_CLOAKE) {
          mods = new Modifiers(mods);
          mods.applyVampyricCloakeModifiers();
        }

        if (modeable != null) {
          var slotWeightings =
              switch (modeable.getSlot()) {
                case ACCESSORY1 ->
                    List.of(
                        this.expression.slots.getOrDefault(Slot.ACCESSORY1, 0),
                        this.expression.slots.getOrDefault(Slot.ACCESSORY2, 0),
                        this.expression.slots.getOrDefault(Slot.ACCESSORY3, 0));
                case OFFHAND ->
                    List.of(
                        this.expression.slots.getOrDefault(Slot.OFFHAND, 0),
                        this.expression.slots.getOrDefault(Slot.FAMILIAR, 0));
                default -> List.of(this.expression.slots.getOrDefault(modeable.getSlot(), 0));
              };
          modeablesNeeded.put(modeable, slotWeightings.stream().anyMatch(s -> s >= 0));
        }

        if (this.expression.posEquip.contains(item)) {
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
            || (this.expression.weight.getOrDefault(BitmapModifier.CLOWNINESS, 0.0) > 0
                && mods.getRawBitmap(BitmapModifier.CLOWNINESS) != 0)
            || (this.expression.weight.getOrDefault(BitmapModifier.RAVEOSITY, 0.0) > 0
                && mods.getRawBitmap(BitmapModifier.RAVEOSITY) != 0)
            || (this.expression.weight.getOrDefault(BitmapModifier.SURGEONOSITY, 0.0) > 0
                && mods.getRawBitmap(BitmapModifier.SURGEONOSITY) != 0)
            || (this.expression.stinkycheese > 0
                && mods.getRawBitmap(BitmapModifier.STINKYCHEESE) != 0)
            || ((mods.getRawBitmap(BitmapModifier.SYNERGETIC) & usefulSynergies) != 0)) {
          item.automaticFlag = true;
          break gotItem;
        } else if (mods.hasUnarmedBonus()) {
          // Figure out what modifiers this item would have if unarmed
          Modifiers unarmedMods = new Modifiers(ModifierDatabase.getItemModifiers(id));
          ExpressionOverrides overrides = new ExpressionOverrides();
          overrides.setUnarmed(true);
          unarmedMods.recalculateExpressions(overrides);
          // Unlike below, modeables can reach here, so score mode bonuses at their current state
          double score =
              this.getScore(unarmedMods, Map.of(Slot.NONE, item), Modeable.getStateMap());
          if (score > nullScore) {
            // The item has an unarmed bonus that is relevant. Ensure that it is always considered,
            // but it should not take up a spot on the shortlist.
            item.conditionalFlag = true;
            item.automaticFlag = true;
            break gotItem;
          }
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
          if (!this.expression.forcedModeables.get(modeable).isEmpty()) {
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
        // Modeable items never reach here (they break gotItem above), so we leave out modes
        double delta = this.getScore(mods, Map.of(Slot.NONE, item), Map.of()) - nullScore;
        if (delta < 0.0) continue;
        if (delta == 0.0) {
          if (KoLCharacter.hasEquipped(item) && this.expression.current) break gotItem;
          if (item.initial == 0) continue;
          if (item.automaticFlag) continue;
        }

        if (mods.getRawBitmap(BitmapModifier.MUTEX) != 0) {
          // This item may turn out to be unequippable, so don't
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
    if (this.expression.slots.getOrDefault(Slot.BUDDYBJORN, 0) < 0) {
      useBjornFamiliar = KoLCharacter.getBjorned();
    } else {
      bestCarriedFamiliar = KoLCharacter.getBjorned();
    }

    // If we're not allowed to change the current familiar, lock it
    if (this.expression.slots.getOrDefault(Slot.CROWNOFTHRONES, 0) < 0) {
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

                      if (!this.expression.forcedModeables.get(modeable).isEmpty()) {
                        return this.expression.forcedModeables.get(modeable);
                      }

                      CheckedItem item =
                          new CheckedItem(modeable.getItemId(), equipScope, maxPrice, priceLevel);
                      var bestMode = modeable.getState();
                      MaximizerSpeculation best = new MaximizerSpeculation();
                      best.attachment = item;
                      best.equipment.put(modeable.getSlot(), item);
                      best.setModeable(modeable, bestMode);

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

    SlotList<MaximizerSpeculation> speculationList =
        new SlotList<>(this.expression.familiars.size());

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
          spec.setFamiliar(this.expression.familiars.get(entry.famIndex()));
          useSlot = Slot.FAMILIAR;
        }
        spec.equipment.put(useSlot, item);

        switch (item.getItemId()) {
          case ItemPool.HATSEAT:
            if (this.expression.slots.getOrDefault(Slot.CROWNOFTHRONES, 0) < 0) {
              spec.setEnthroned(useCrownFamiliar);
            } else if (this.carriedFamiliarsNeeded > 1) {
              item.automaticFlag = true;
              spec.setEnthroned(secondBestCarriedFamiliar);
            } else {
              spec.setEnthroned(bestCarriedFamiliar);
            }
            break;
          case ItemPool.BUDDY_BJORN:
            if (this.expression.slots.getOrDefault(Slot.BUDDYBJORN, 0) < 0) {
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
        if (outfitSpec.compareTo(compareSpec) <= 0
            && !this.expression.posOutfits.contains(outfit.getName())) {
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
    if (this.expression.dump > 0) {
      outfitSummary.append("]");
      RequestLogger.printLine(outfitSummary.toString());
    }

    for (var entry : ranked.entries()) {
      List<CheckedItem> checkedItemList = ranked.get(entry);
      var automaticEntry = automatic.get(entry);

      if (this.expression.dump > 0) {
        RequestLogger.printLine(
            "SLOT " + (entry.isSlot() ? entry.slot() : "BONUS FAMILIAR #" + entry.famIndex()));
      }

      if (this.expression.dump > 1) {
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
            } else if (total < useful
                && beeotches < useful
                && beeosity < this.expression.beeosity) {
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

      if (this.expression.dump > 0) {
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
        if (this.expression.slots.getOrDefault(slot, 0) >= thresh) {
          spec.equipment.put(slot, null);
          anySlots = true;
        }
      }
      if (anySlots) break;
    }

    if (spec.equipment.get(Slot.OFFHAND) != null) {
      this.expression.hands = 1;
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

          if (this.expression.familiars.stream().anyMatch(f -> f.canEquip(modeable.getItem()))) {
            backupSlots.add(Slot.FAMILIAR);
          }

          // Slots we're ignoring are not considered, they keep their modes
          // A slot is considered ignored if not null
          boolean itemInIgnoredSlot =
              spec.equipment.values().stream()
                  .anyMatch(i -> i != null && i.getItemId() == modeable.getItemId());

          if (!itemInIgnoredSlot
              && backupSlots.stream().anyMatch(s -> spec.equipment.get(s) == null)) {
            spec.setModeable(modeable, mode);
          }
        });

    spec.tryAll(
        this.expression.familiars,
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
