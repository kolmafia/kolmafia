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
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.ExpressionOverrides;
import net.sourceforge.kolmafia.FamiliarData;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLConstants.WeaponType;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.Modeable;
import net.sourceforge.kolmafia.ModifierType;
import net.sourceforge.kolmafia.Modifiers;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.SpecialOutfit;
import net.sourceforge.kolmafia.equipment.Slot;
import net.sourceforge.kolmafia.equipment.SlotSet;
import net.sourceforge.kolmafia.modifiers.BitmapModifier;
import net.sourceforge.kolmafia.modifiers.BooleanModifier;
import net.sourceforge.kolmafia.modifiers.DerivedModifier;
import net.sourceforge.kolmafia.modifiers.DoubleModifier;
import net.sourceforge.kolmafia.modifiers.DoubleModifierCollection;
import net.sourceforge.kolmafia.modifiers.StringModifier;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.objectpool.SkillPool;
import net.sourceforge.kolmafia.persistence.AdventureDatabase;
import net.sourceforge.kolmafia.persistence.EquipmentDatabase;
import net.sourceforge.kolmafia.persistence.FamiliarDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.persistence.ItemFinder;
import net.sourceforge.kolmafia.persistence.ItemFinder.Match;
import net.sourceforge.kolmafia.persistence.ModifierDatabase;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.EquipmentRequest;
import net.sourceforge.kolmafia.session.EffectAvailability;
import net.sourceforge.kolmafia.session.EquipmentManager;
import net.sourceforge.kolmafia.utilities.StringUtilities;

@SuppressWarnings("incomplete-switch")
public class Evaluator {
  @Deprecated public boolean failed;
  boolean exceeded;
  private Evaluator tiebreaker;
  private final CodpieceEvaluator codpieceEvaluator = new CodpieceEvaluator(this);
  private final DoubleModifierCollection weight = new DoubleModifierCollection();
  private final EnumSet<DoubleModifier> explicitScoreModifiers =
      EnumSet.noneOf(DoubleModifier.class);
  private Map<DoubleModifier, Double> min;
  private Map<DoubleModifier, Double> max;
  private List<ScoreTerm> activeScoreModifiers = List.of();
  private boolean predictsDerivedModifiers;
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
  private final Map<AdventureResult, ItemBonus> bonuses = new HashMap<>();
  private final List<BonusFunction> bonusFunc = new ArrayList<>();

  record BonusFunction(Function<AdventureResult, Double> bonusFunction, Double weight) {}

  record ItemBonus(double base, Map<String, Double> modes) {}

  record ScoreTerm(DoubleModifier modifier, double weight, double min, double max) {}

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

  private static Slot toUseSlot(Slot slot) {
    return switch (slot) {
      case /* Evaluator.OFFHAND_MELEE */ ACCESSORY2, /* Evaluator.OFFHAND_RANGED */ ACCESSORY3 ->
          Slot.OFFHAND;
      case /* Evaluator.WEAPON_1H */ STICKER3 -> Slot.WEAPON;
      default -> slot;
    };
  }

  private Evaluator() {
    this.totalMin = Double.NEGATIVE_INFINITY;
    this.totalMax = Double.POSITIVE_INFINITY;
  }

  public Evaluator(String expr) {
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
    tiebreaker.initializeScoreModifiers();

    this.min = new EnumMap<>(tiebreaker.min);
    this.max = new EnumMap<>(tiebreaker.max);
    this.parse(expr);
    this.initializeScoreModifiers();
  }

  private void initializeScoreModifiers() {
    var active = new ArrayList<ScoreTerm>();
    this.predictsDerivedModifiers = false;
    for (var modifier : DoubleModifier.DOUBLE_MODIFIERS) {
      double weight = this.weight.getDouble(modifier);
      double min = this.min.get(modifier);
      if (weight == 0.0 && min == Double.NEGATIVE_INFINITY) {
        continue;
      }

      active.add(new ScoreTerm(modifier, weight, min, this.max.get(modifier)));
      if (modifier == DoubleModifier.MUS
          || modifier == DoubleModifier.MYS
          || modifier == DoubleModifier.MOX
          || modifier == DoubleModifier.HP
          || modifier == DoubleModifier.MP) {
        this.predictsDerivedModifiers = true;
      }
    }
    this.activeScoreModifiers = List.copyOf(active);
  }

  @SuppressWarnings("BooleanMethodIsAlwaysInverted")
  private boolean forceModeable(ItemFinder.ItemWithMode modeable, String mode) {
    String existing = forcedModeables.get(modeable.modeable());
    if (!existing.isEmpty() && !existing.equals(mode)) {
      KoLmafia.updateDisplay(
          MafiaState.ERROR,
          "Conflicting modes requested for "
              + modeable.item().getName()
              + ": "
              + existing
              + " vs "
              + mode);
      return false;
    }
    forcedModeables.put(modeable.modeable(), mode);
    return true;
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
        // If a mode was not specified
        if (forcedModeables.get(Modeable.UMBRELLA).isEmpty()) {
          forcedModeables.put(Modeable.UMBRELLA, "forward-facing");
        }
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
        // If no target is specified, require every equippable surgical item.
        this.surgeonosity =
            (m.end(2) == m.start(2)) ? (KoLCharacter.isTorsoAware() ? 5 : 4) : (int) weight;
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
        if (forcedModeables.get(Modeable.EDPIECE).isEmpty()) {
          // Force Crown of Ed to Fish
          forcedModeables.put(Modeable.EDPIECE, "fish");
        }
        continue;
      }

      if (keyword.startsWith("equip ")) {
        var match =
            ItemFinder.getFirstMatchingItemWithMode(keyword.substring(6).trim(), Match.EQUIP);
        if (match == null) {
          return;
        }
        if (match.modeable() != null && !forceModeable(match, match.mode())) {
          return;
        }
        if (weight > 0.0) {
          this.posEquip.add(match.item());
          equipBeeosity += KoLCharacter.getBeeosity(match.item().getName());
        } else {
          this.negEquip.add(match.item());
        }
        continue;
      }

      if (keyword.startsWith("bonus ")) {
        var match =
            ItemFinder.getFirstMatchingItemWithMode(keyword.substring(6).trim(), Match.EQUIP);
        if (match == null) {
          return;
        }
        // If this item does not require a mode
        if (match.mode() == null) {
          var existing = this.bonuses.get(match.item());
          var modes = existing == null ? new HashMap<String, Double>() : existing.modes();
          // We override the existing base weight as per old behavior, but inherit the modes.
          this.bonuses.put(match.item(), new ItemBonus(weight, modes));
        } else {
          this.bonuses
              .computeIfAbsent(match.item(), k -> new ItemBonus(0.0, new HashMap<>()))
              .modes()
              .put(match.mode(), weight);
        }
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
        AdventureResult item = EquipmentManager.getBestPlumberTool(KoLCharacter.getPrimeIndex());
        if (item == null) {
          // Otherwise, pick best available tool
          // You are guaranteed to have work boots, at least
          item = EquipmentManager.getBestPlumberTool(-1);
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
        AdventureResult item1 = EquipmentManager.getBestPlumberTool(1);
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
        } else if (keyword.startsWith("organ")) {
          keyword = "organ capacity";
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
          case "organ capacity" -> {
            this.weight.set(DoubleModifier.STOMACH_CAPACITY, weight);
            this.weight.set(DoubleModifier.LIVER_CAPACITY, weight);
            this.weight.set(DoubleModifier.SPLEEN_CAPACITY, weight);
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
        } else if (keyword.startsWith("stomach")) {
          index = DoubleModifier.STOMACH_CAPACITY;
        } else if (keyword.startsWith("liver")) {
          index = DoubleModifier.LIVER_CAPACITY;
        } else if (keyword.startsWith("spleen")) {
          index = DoubleModifier.SPLEEN_CAPACITY;
        } else if (keyword.equals("ocrs")) {
          this.noTiebreaker = true;
          this.beeosity = 999;
          index = DoubleModifier.RANDOM_MONSTER_MODIFIERS;
        }
      }

      if (index != null) {
        // We found a match.
        String modifierName = index.getName();
        this.explicitScoreModifiers.add(index);
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
    final double fudge = this.weight.getDouble(source) * 0.0001f;
    if (fudge > 0) {
      for (var extra : extras) {
        this.weight.increment(extra, fudge);
      }
    }
  }

  public double getScore(
      Modifiers mods, Map<Slot, AdventureResult> equipment, Map<Modeable, String> modeables) {
    var outcome = this.evaluate(mods, equipment, modeables);
    this.failed = outcome.failed();
    this.exceeded = outcome.exceeded();
    return outcome.score();
  }

  public EvaluationOutcome evaluate(Modifiers mods) {
    return this.evaluate(mods, Map.of(), Map.of());
  }

  EvaluationOutcome evaluate(
      Modifiers mods, Map<Slot, AdventureResult> equipment, Map<Modeable, String> modeables) {
    var predicted = this.predictsDerivedModifiers ? mods.predict() : null;

    boolean failed = false;
    double score = 0.0;
    for (var scoreModifier : this.activeScoreModifiers) {
      var mod = scoreModifier.modifier();
      double weight = scoreModifier.weight();
      double min = scoreModifier.min();
      double val = scoreValue(mod, mods, predicted);
      double max = scoreModifier.max();
      if (val < min) failed = true;
      score += weight * Math.min(val, max);
    }
    if (this.stinkycheese > 0) {
      int val = mods.getBitmap(BitmapModifier.STINKYCHEESE);
      score += this.stinkycheese * val;
    }
    if (!this.bonuses.isEmpty() || !this.bonusFunc.isEmpty()) {
      for (AdventureResult item : equipment.values()) {
        score += this.getItemScore(item, modeables);
      }
    }
    // Add fudge factor for Rollover Effect
    if (mods.hasString(StringModifier.ROLLOVER_EFFECT)) {
      score += 0.01f;
    }
    if (score < this.totalMin) failed = true;
    boolean exceeded = score >= this.totalMax;
    // Score bitmap objectives 1:1 up to the requested target, which must be reached.
    if (this.clownosity > 0) {
      int osity = mods.getBitmap(BitmapModifier.CLOWNINESS);
      score += Math.min(osity, this.clownosity);
      if (osity < this.clownosity) failed = true;
    }
    if (this.raveosity > 0) {
      int osity = mods.getBitmap(BitmapModifier.RAVEOSITY);
      score += Math.min(osity, this.raveosity);
      if (osity < this.raveosity) failed = true;
    }
    if (this.surgeonosity > 0) {
      int osity = mods.getBitmap(BitmapModifier.SURGEONOSITY);
      score += Math.min(osity, this.surgeonosity);
      if (osity < this.surgeonosity) failed = true;
    }
    if (!failed
        && !this.booleanMask.isEmpty()
        && !mods.getBooleans(this.booleanMask).equals(this.booleanValue)) {
      failed = true;
    }
    return new EvaluationOutcome(score, failed, exceeded);
  }

  public double getScore(Modifiers mods) {
    return this.getScore(mods, Map.of(), Map.of());
  }

  static double scoreValue(
      DoubleModifier modifier, Modifiers modifiers, Map<DerivedModifier, Integer> predicted) {
    return switch (modifier) {
      case MUS -> predicted.get(DerivedModifier.BUFFED_MUS);
      case MYS -> predicted.get(DerivedModifier.BUFFED_MYS);
      case MOX -> predicted.get(DerivedModifier.BUFFED_MOX);
      case HP -> predicted.get(DerivedModifier.BUFFED_HP);
      case MP -> predicted.get(DerivedModifier.BUFFED_MP);
      case FAMILIAR_WEIGHT ->
          (modifiers.getDouble(DoubleModifier.FAMILIAR_WEIGHT)
                  + modifiers.getDouble(DoubleModifier.HIDDEN_FAMILIAR_WEIGHT))
              * (modifiers.getDouble(DoubleModifier.FAMILIAR_WEIGHT_PCT) < 0.0 ? 0.5 : 1.0);
      case INITIATIVE ->
          modifiers.getDouble(DoubleModifier.INITIATIVE)
              + Math.min(0.0, modifiers.getDouble(DoubleModifier.INITIATIVE_PENALTY));
      case MANA_COST ->
          modifiers.getDouble(DoubleModifier.MANA_COST)
              + modifiers.getDouble(DoubleModifier.STACKABLE_MANA_COST);
      case ITEMDROP ->
          modifiers.getDouble(DoubleModifier.ITEMDROP)
              + 100.0
              + Math.min(0.0, modifiers.getDouble(DoubleModifier.ITEMDROP_PENALTY))
              + modifiers.getDouble(DoubleModifier.SPORADIC_ITEMDROP);
      case MEATDROP ->
          modifiers.getDouble(DoubleModifier.MEATDROP)
              + 100.0
              + Math.min(0.0, modifiers.getDouble(DoubleModifier.MEATDROP_PENALTY))
              + modifiers.getDouble(DoubleModifier.SPORADIC_MEATDROP)
              + modifiers.getDouble(DoubleModifier.MEAT_BONUS) / 10000.0;
      case WEAPON_DAMAGE ->
          modifiers.getDouble(DoubleModifier.WEAPON_DAMAGE)
              + modifiers.getDouble(DoubleModifier.WEAPON_DAMAGE_PCT);
      case RANGED_DAMAGE ->
          modifiers.getDouble(DoubleModifier.RANGED_DAMAGE)
              + modifiers.getDouble(DoubleModifier.RANGED_DAMAGE_PCT);
      case SPELL_DAMAGE ->
          modifiers.getDouble(DoubleModifier.SPELL_DAMAGE)
              + modifiers.getDouble(DoubleModifier.SPELL_DAMAGE_PCT);
      case DAMAGE_AURA ->
          modifiers.getDouble(DoubleModifier.DAMAGE_AURA)
              + modifiers.getDouble(DoubleModifier.SPORADIC_DAMAGE_AURA);
      case THORNS ->
          modifiers.getDouble(DoubleModifier.THORNS)
              + modifiers.getDouble(DoubleModifier.SPORADIC_THORNS);
      case COLD_RESISTANCE ->
          resistanceValue(
              modifiers,
              DoubleModifier.COLD_RESISTANCE,
              BooleanModifier.COLD_IMMUNITY,
              BooleanModifier.COLD_VULNERABILITY);
      case HOT_RESISTANCE ->
          resistanceValue(
              modifiers,
              DoubleModifier.HOT_RESISTANCE,
              BooleanModifier.HOT_IMMUNITY,
              BooleanModifier.HOT_VULNERABILITY);
      case SLEAZE_RESISTANCE ->
          resistanceValue(
              modifiers,
              DoubleModifier.SLEAZE_RESISTANCE,
              BooleanModifier.SLEAZE_IMMUNITY,
              BooleanModifier.SLEAZE_VULNERABILITY);
      case SPOOKY_RESISTANCE ->
          resistanceValue(
              modifiers,
              DoubleModifier.SPOOKY_RESISTANCE,
              BooleanModifier.SPOOKY_IMMUNITY,
              BooleanModifier.SPOOKY_VULNERABILITY);
      case STENCH_RESISTANCE ->
          resistanceValue(
              modifiers,
              DoubleModifier.STENCH_RESISTANCE,
              BooleanModifier.STENCH_IMMUNITY,
              BooleanModifier.STENCH_VULNERABILITY);
      case EXPERIENCE -> experienceValue(modifiers);
      default -> modifiers.getDouble(modifier);
    };
  }

  private static double resistanceValue(
      Modifiers modifiers,
      DoubleModifier resistance,
      BooleanModifier immunity,
      BooleanModifier vulnerability) {
    if (modifiers.getBoolean(immunity)) {
      return 100.0;
    }
    double value = modifiers.getDouble(resistance);
    return modifiers.getBoolean(vulnerability) ? value - 100.0 : value;
  }

  private static double experienceValue(Modifiers modifiers) {
    double baseExperience =
        KoLCharacter.estimatedBaseExp(
            modifiers.getDouble(DoubleModifier.MONSTER_LEVEL)
                * (1.0 + modifiers.getDouble(DoubleModifier.MONSTER_LEVEL_PERCENT) / 100.0));
    double experiencePercent = modifiers.getDouble(DoubleModifier.primeStatExpPercent()) / 100.0;
    double experience = modifiers.getDouble(DoubleModifier.primeStatExp());
    return ((baseExperience + experience) * (1.0 + experiencePercent)) / 2.0;
  }

  CodpieceEvaluator.Context codpieceContext() {
    return new CodpieceEvaluator.Context(
        this.activeScoreModifiers,
        this.tiebreaker == null ? List.of() : this.tiebreaker.activeScoreModifiers,
        this.noTiebreaker);
  }

  double getItemScore(AdventureResult item, Map<Modeable, String> modeables) {
    double score = 0.0;
    ItemBonus itemBonus = this.bonuses.get(item);
    if (itemBonus != null) {
      score += itemBonus.base();
      var modeable = Modeable.find(item);
      if (modeable != null) {
        var mode = modeables.get(modeable);
        if (mode != null) {
          score += itemBonus.modes().getOrDefault(mode, 0.0);
        }
      }
    }
    for (BonusFunction func : this.bonusFunc) {
      score += func.bonusFunction.apply(item) * func.weight;
    }
    return score;
  }

  EvaluationOutcome evaluateComplete(
      Modifiers mods,
      Map<Slot, AdventureResult> equipment,
      Map<Modeable, String> modeables,
      boolean resourceLimitExceeded,
      int allowedMutexViolations) {
    var outcome = this.evaluate(mods, equipment, modeables);
    boolean failed = this.failsEquipment(mods, equipment, outcome.failed());
    if (resourceLimitExceeded) {
      failed = true;
    }
    if ((mods.getRawBitmap(BitmapModifier.MUTEX_VIOLATIONS) & ~allowedMutexViolations) != 0) {
      failed = true;
    }
    return new EvaluationOutcome(outcome.score(), failed, outcome.exceeded());
  }

  private boolean failsEquipment(
      Modifiers mods, Map<Slot, AdventureResult> equipment, boolean scoreFailed) {
    boolean failed = scoreFailed;
    boolean outfitSatisfied = this.posOutfits.isEmpty();
    boolean equipSatisfied = this.posEquip.isEmpty();
    if (!failed && !this.posEquip.isEmpty()) {
      equipSatisfied = true;
      for (AdventureResult item : this.posEquip) {
        if (!this.hasEquipped(equipment, item)) {
          equipSatisfied = false;
          break;
        }
      }
    }
    if (!failed && this.negEquip.stream().anyMatch(item -> this.hasEquipped(equipment, item))) {
      failed = true;
    }
    if (!failed) {
      String outfit = mods.getString(StringModifier.OUTFIT);
      if (this.negOutfits.contains(outfit)) {
        failed = true;
      } else {
        outfitSatisfied = this.posOutfits.contains(outfit) || this.posOutfits.isEmpty();
      }
    }
    if (!outfitSatisfied || !equipSatisfied) {
      failed = true;
    }
    return failed;
  }

  int beeosityLimit() {
    return this.beeosity;
  }

  private boolean hasEquipped(Map<Slot, AdventureResult> equipment, AdventureResult item) {
    if (KoLCharacter.hasEquipped(equipment, item)) {
      return true;
    }

    boolean wearingCodpiece =
        KoLCharacter.hasEquipped(
            equipment, ItemPool.get(ItemPool.THE_ETERNITY_CODPIECE), SlotSet.ACCESSORY_SLOTS);
    return wearingCodpiece
        && EquipmentDatabase.isCodpieceGem(item.getItemId())
        && KoLCharacter.hasEquipped(equipment, item, SlotSet.CODPIECE_SLOTS);
  }

  boolean slotEnabled(Slot slot) {
    int threshold = this.slots.values().stream().anyMatch(value -> value > 0) ? 1 : 0;
    return this.slots.getOrDefault(slot, 0) >= threshold;
  }

  double getTiebreaker(Modifiers mods) {
    if (this.noTiebreaker) return 0.0;
    return this.tiebreaker.getScore(mods);
  }

  boolean isUsingTiebreaker() {
    return !this.noTiebreaker;
  }

  boolean areScoreModifiersSaturated(Modifiers modifiers) {
    Map<DerivedModifier, Integer> predicted = null;
    for (ScoreTerm term : this.activeScoreModifiers) {
      DoubleModifier modifier = term.modifier();
      if ((modifier == DoubleModifier.MUS
              || modifier == DoubleModifier.MYS
              || modifier == DoubleModifier.MOX
              || modifier == DoubleModifier.HP
              || modifier == DoubleModifier.MP)
          && predicted == null) {
        predicted = modifiers.predict();
      }
      double value = scoreValue(modifier, modifiers, predicted);
      if ((term.weight() > 0.0 && value < term.max())
          || (term.weight() < 0.0 && value > term.min())) {
        return false;
      }
    }
    return true;
  }

  boolean isWeaponTypeRequired() {
    return this.requireClub
        || this.requireUtensil
        || this.requireSword
        || this.requireKnife
        || this.requireAccordion;
  }

  boolean isShieldRequired() {
    return this.requireShield;
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

  boolean requiresEquipment(AdventureResult item) {
    return this.posEquip.contains(item);
  }

  boolean excludesEquipment(AdventureResult item) {
    return this.negEquip.contains(item);
  }

  boolean currentOnly() {
    return this.current;
  }

  @Deprecated
  public static boolean cannotGainEffect(int effectId) {
    return EffectAvailability.cannotGain(effectId);
  }

  void enumerateEquipment(EquipScope equipScope, int maxPrice, PriceLevel priceLevel)
      throws MaximizerInterruptedException {
    this.enumerateEquipment(equipScope, maxPrice, priceLevel, false);
  }

  void enumerateEquipment(
      EquipScope equipScope, int maxPrice, PriceLevel priceLevel, boolean exhaustive)
      throws MaximizerInterruptedException {
    CharacterSnapshot character = Maximizer.character();
    // Every legal, available candidate, including those rejected by isolated scoring.
    SlotList<CheckedItem> catalog = new SlotList<>(this.familiars.size());
    // Items to be considered based on their score
    SlotList<CheckedItem> ranked = new SlotList<>(this.familiars.size());

    double nullScore = this.getScore(new Modifiers());
    double nullTiebreaker = this.getTiebreaker(new Modifiers());

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
    FamiliarEquipmentCompiler familiarEquipmentCompiler =
        new FamiliarEquipmentCompiler(
            this, this.familiars, catalog, ranked, equipScope, maxPrice, priceLevel, nullScore);

    int id = 0;
    while ((id = EquipmentDatabase.nextEquipmentItemId(id)) != -1) {
      Slot slot = EquipmentManager.itemIdToEquipmentType(id);
      if (slot == Slot.NONE) continue;
      AdventureResult preItem = ItemPool.get(id, 1);
      String name = preItem.getName();
      CheckedItem item;
      if (this.negEquip.contains(preItem)) continue;
      if (character.resourcesExceeded(character.resourceUsage(name))) {
        continue;
      }

      var modeable = Modeable.find(id);

      boolean famCanEquip = KoLCharacter.getFamiliar().canEquip(preItem);
      var familiarResult = familiarEquipmentCompiler.compile(id, preItem, slot, modeable);
      if (familiarResult.rejected()) {
        continue;
      }
      item = familiarResult.item();

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
                if (!EquipmentManager.canEquipChefstaff(gloveAvailable)) {
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
                && this.weight.getDouble(DoubleModifier.ITEMDROP) > 0
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
                && EquipmentManager.canEquipChefstaff(true)
                && !EquipmentManager.canEquipChefstaff(false)) {
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
                && (this.weight.getDouble(DoubleModifier.EXPERIENCE) > 0
                    || this.weight.getDouble(DoubleModifier.MUS_EXPERIENCE) > 0
                    || this.weight.getDouble(DoubleModifier.MYS_EXPERIENCE) > 0
                    || this.weight.getDouble(DoubleModifier.MOX_EXPERIENCE) > 0)
                && Preferences.getInteger("garbageShirtCharge") > 0) {
              // This is always going to be worth including if useful
              item.requiredFlag = true;
              item.automaticFlag = true;
              break gotItem;
            }
            break;
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
            && KoLCharacter.getPath().canUseFamiliars()) {
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
                case ACCESSORY1 ->
                    List.of(
                        this.slots.getOrDefault(Slot.ACCESSORY1, 0),
                        this.slots.getOrDefault(Slot.ACCESSORY2, 0),
                        this.slots.getOrDefault(Slot.ACCESSORY3, 0));
                case OFFHAND ->
                    List.of(
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
            && KoLCharacter.getPath().canUseFamiliars()) {
          break gotItem;
        }

        if (id == ItemPool.CARD_SLEEVE || id == ItemPool.THE_ETERNITY_CODPIECE) {
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
        // Modeable items never reach here (they break gotItem above), so we leave out modes
        double delta = this.getScore(mods, Map.of(Slot.NONE, item), Map.of()) - nullScore;
        if (delta < 0.0) {
          addCandidate(catalog, slot, auxSlot, item);
          continue;
        }
        if (delta == 0.0) {
          if (KoLCharacter.hasEquipped(item) && this.current) break gotItem;
          if (item.availability().initial() == 0 || item.automaticFlag) {
            addCandidate(catalog, slot, auxSlot, item);
            continue;
          }
        }

        if (mods.getRawBitmap(BitmapModifier.MUTEX) != 0) {
          // This item may turn out to be unequippable, so don't
          // count it towards the shortlist length.
          item.conditionalFlag = true;
        }
      }
      // "break gotItem" goes here
      addCandidate(catalog, slot, auxSlot, item);
      addCandidate(ranked, slot, auxSlot, item);
    }

    var codpieceCandidates =
        this.codpieceEvaluator.compileCandidates(
            equipScope, maxPrice, priceLevel, nullScore, nullTiebreaker);
    catalog.get(Slot.CODPIECE1).addAll(codpieceCandidates.catalog());
    ranked.get(Slot.CODPIECE1).addAll(codpieceCandidates.ranked());
    boolean codpieceCanExpandAccessoryPool =
        this.codpieceEvaluator.prepareAccessoryCandidates(
            codpieceCandidates.ranked(),
            ranked.get(Slot.ACCESSORY1),
            SlotSet.CODPIECE_SLOTS.stream().anyMatch(this::slotEnabled));

    var carriedFamiliarSelection =
        CarriedFamiliarSelector.select(
            this.carriedFamiliarsNeeded,
            this.slots.getOrDefault(Slot.CROWNOFTHRONES, 0) < 0,
            this.slots.getOrDefault(Slot.BUDDYBJORN, 0) < 0,
            character,
            equipScope,
            maxPrice,
            priceLevel);
    this.carriedFamiliars.addAll(carriedFamiliarSelection.candidates());
    FamiliarData useCrownFamiliar = carriedFamiliarSelection.lockedCrown();
    FamiliarData useBjornFamiliar = carriedFamiliarSelection.lockedBjorn();

    CheckedItem bestCard =
        CardSleeveSelector.select(this.cardNeeded, equipScope, maxPrice, priceLevel);
    AdventureResult useCard = null;

    Map<Modeable, String> bestModes =
        ModeableSelector.select(modeablesNeeded, forcedModeables, equipScope, maxPrice, priceLevel);
    CandidateSpeculationFactory speculationFactory =
        new CandidateSpeculationFactory(
            this.carriedFamiliarsNeeded, carriedFamiliarSelection, bestCard, bestModes);

    SlotList<MaximizerSpeculation> speculationList = new SlotList<>(this.familiars.size());

    for (var entry : ranked.entries()) {
      List<CheckedItem> checkedItemList = entry.value();

      // If we currently have nothing equipped, also consider leaving nothing equipped
      if ((!entry.isSlot() || entry.slot() != Slot.CODPIECE1)
          && (!entry.isSlot()
              || EquipmentManager.getEquipment(Evaluator.toUseSlot(entry.slot()))
                  == EquipmentRequest.UNEQUIP)) {
        var unequip = new CheckedItem(-1, equipScope, maxPrice, priceLevel);
        checkedItemList.add(unequip);
        catalog.get(entry).add(unequip);
      }

      List<MaximizerSpeculation> specs = speculationList.get(entry);

      for (CheckedItem item : checkedItemList) {
        Slot useSlot;
        FamiliarData familiar = null;
        if (entry.isSlot()) {
          useSlot = Evaluator.toUseSlot(entry.slot());
        } else {
          familiar = this.familiars.get(entry.famIndex());
          useSlot = Slot.FAMILIAR;
        }
        var result = speculationFactory.create(item, useSlot, familiar);
        if (result.card() != null) {
          useCard = result.card();
        }

        specs.add(result.speculation());
      }

      Collections.sort(specs);
    }

    for (var entry : catalog.entries()) {
      if ((!entry.isSlot() || entry.slot() != Slot.CODPIECE1)
          && entry.value().stream().noneMatch(item -> item.getItemId() == -1)) {
        entry.value().add(new CheckedItem(-1, equipScope, maxPrice, priceLevel));
      }
    }
    int catalogCandidateCount =
        catalog.entries().stream().mapToInt(entry -> entry.value().size()).sum();

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

    var shortlist =
        new CandidateShortlistCompiler(
                this.familiars, character, equipScope, maxPrice, priceLevel, this.dump)
            .compile(ranked, speculationList, codpieceCanExpandAccessoryPool);
    SlotList<CheckedItem> automatic = shortlist.candidates();
    Maximizer.recordCandidateCounts(catalogCandidateCount, shortlist.candidateCount());

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
      if (exhaustive) {
        catalog.set(Slot.WEAPON, catalog.get(Evaluator.WEAPON_1H));
      }

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

    MaximizerSpeculation exhaustiveBaseline = exhaustive ? spec.clone() : null;
    Maximizer.startSearch(exhaustive);
    spec.tryAll(
        this.familiars,
        this.carriedFamiliars,
        usefulOutfits,
        outfitPieces,
        automatic,
        useCard,
        useCrownFamiliar,
        useBjornFamiliar);

    if (exhaustive) {
      for (var entry : catalog.entries()) {
        for (var item : entry.value()) {
          item.validate(maxPrice, priceLevel);
        }
      }
      if (exhaustiveBaseline.equipment.get(Slot.OFFHAND) == null) {
        catalog.get(Slot.WEAPON).addAll(catalog.get(Evaluator.WEAPON_1H));
      }
      catalog.get(Evaluator.OFFHAND_MELEE).addAll(catalog.get(Slot.OFFHAND));
      catalog.get(Evaluator.OFFHAND_RANGED).addAll(catalog.get(Slot.OFFHAND));
      exhaustiveBaseline.tryAll(
          this.familiars,
          this.carriedFamiliars,
          usefulOutfits,
          outfitPieces,
          catalog,
          useCard,
          useCrownFamiliar,
          useBjornFamiliar);
    }
  }

  List<CheckedItem> prioritizeCodpieceGems(List<CheckedItem> gems) {
    return this.codpieceEvaluator.prioritize(gems);
  }

  private static void addCandidate(
      SlotList<CheckedItem> candidates, Slot slot, Slot auxSlot, CheckedItem item) {
    if (slot != Slot.NONE) candidates.get(slot).add(item);
    if (auxSlot != Slot.NONE) candidates.get(auxSlot).add(item);
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
