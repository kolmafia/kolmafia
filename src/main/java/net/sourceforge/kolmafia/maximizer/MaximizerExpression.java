package net.sourceforge.kolmafia.maximizer;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.FamiliarData;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.Modeable;
import net.sourceforge.kolmafia.Modifiers;
import net.sourceforge.kolmafia.SpecialOutfit;
import net.sourceforge.kolmafia.equipment.Slot;
import net.sourceforge.kolmafia.equipment.SlotSet;
import net.sourceforge.kolmafia.modifiers.BitmapModifier;
import net.sourceforge.kolmafia.modifiers.BooleanModifier;
import net.sourceforge.kolmafia.modifiers.DoubleModifier;
import net.sourceforge.kolmafia.modifiers.Modifier;
import net.sourceforge.kolmafia.modifiers.StringModifier;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.AdventureDatabase;
import net.sourceforge.kolmafia.persistence.FamiliarDatabase;
import net.sourceforge.kolmafia.persistence.ItemFinder;
import net.sourceforge.kolmafia.persistence.ItemFinder.Match;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.EquipmentRequest;
import net.sourceforge.kolmafia.session.EquipmentManager;
import net.sourceforge.kolmafia.session.InventoryManager;
import net.sourceforge.kolmafia.utilities.StringUtilities;

@SuppressWarnings("incomplete-switch")
class MaximizerExpression {
  final Map<Modifier, Double> weight = new HashMap<>();
  final Map<Modifier, Double> min = new HashMap<>();
  final Map<Modifier, Double> max = new HashMap<>();
  double totalMin;
  double totalMax;
  int dump = 0;
  int stinkycheese = 0;
  int beeosity = 2;
  final EnumSet<BooleanModifier> booleanMask = EnumSet.noneOf(BooleanModifier.class);
  final Set<BooleanModifier> booleanValue = EnumSet.noneOf(BooleanModifier.class);
  final List<FamiliarData> familiars = new ArrayList<>();

  // Some modeables are forced based on certain expressions appearing in a maximize call
  // For example, if you request "sea" the Crown of Ed will always pick fish. This does pose
  // an issue if the maximizer would choose the SCUBA gear to provide water-breathing, as it would
  // not consider a different mode for the Crown. e.g. "maximize sea, ml" would not consider the
  // "bear" mode for the hat. Something for someone to fix in the future.
  final Map<Modeable, String> forcedModeables = Modeable.getStringMap(m -> "");

  /** if slots[i] >= 0 then equipment of type i can be considered for maximization */
  final EnumMap<Slot, Integer> slots = new EnumMap<>(Slot.class);

  String weaponType = null;
  int hands = 0;
  int melee = 0; // +/-2 or higher: require, +/-1: disallow other type
  boolean effective = false;
  boolean requireClub = false;
  boolean requireShield = false;
  boolean requireUtensil = false;
  boolean requireSword = false;
  boolean requireKnife = false;
  boolean requireAccordion = false;
  boolean noTiebreaker = false;
  boolean current = !KoLCharacter.canInteract() || Preferences.getBoolean("maximizerAlwaysCurrent");
  final Set<String> posOutfits = new HashSet<>();
  final Set<String> negOutfits = new HashSet<>();
  final Set<AdventureResult> posEquip = new HashSet<>();
  final Set<AdventureResult> negEquip = new HashSet<>();
  final Map<AdventureResult, ItemBonus> bonuses = new HashMap<>();
  final Map<BooleanModifier, Double> modBonuses = new HashMap<>();
  final List<BonusFunction> bonusFunc = new ArrayList<>();

  record BonusFunction(Function<AdventureResult, Double> bonusFunction, Double weight) {}

  record ItemBonus(double base, Map<String, Double> modes) {}

  private record Canonicalization(Pattern pattern, String canonical) {}

  private record ModifierLimits(double minimum, double maximum) {}

  private enum OperandSupport {
    NOT_SUPPORTED,
    OPTIONAL,
    REQUIRED
  }

  private record ParsedTerm(
      double weight, String originalKeyword, String keyword, String operand, Modifier modifier) {
    private static ParsedTerm from(Matcher matcher) {
      double weight =
          StringUtilities.parseDouble(
              matcher.end(2) == matcher.start(2)
                  ? matcher.group(1) + "1"
                  : matcher.group(1) + matcher.group(2));

      String originalKeyword = matcher.group(3).trim();
      if (originalKeyword.startsWith("\"") && originalKeyword.endsWith("\"")) {
        originalKeyword = originalKeyword.substring(1, originalKeyword.length() - 1).trim();
      }

      String keyword = originalKeyword;
      String operand = "";
      int separator = originalKeyword.indexOf(' ');
      String possibleDirective =
          separator == -1 ? originalKeyword : originalKeyword.substring(0, separator);
      OperandSupport operandSupport = DIRECTIVES.get(possibleDirective);
      if (operandSupport != null && operandSupport != OperandSupport.NOT_SUPPORTED) {
        keyword = possibleDirective;
        if (separator != -1) {
          operand = originalKeyword.substring(separator + 1).trim();
        }
      }

      keyword = canonicalize(keyword);
      Modifier modifier = modifierFor(keyword);
      return new ParsedTerm(weight, originalKeyword, keyword, operand, modifier);
    }

    private ParsedTerm withModifier(Modifier modifier) {
      return new ParsedTerm(
          this.weight, this.originalKeyword, this.keyword, this.operand, modifier);
    }
  }

  private static final Map<String, OperandSupport> DIRECTIVES =
      Map.ofEntries(
          Map.entry("min", OperandSupport.NOT_SUPPORTED),
          Map.entry("max", OperandSupport.NOT_SUPPORTED),
          Map.entry("dump", OperandSupport.NOT_SUPPORTED),
          Map.entry("hand", OperandSupport.NOT_SUPPORTED),
          Map.entry("tie", OperandSupport.NOT_SUPPORTED),
          Map.entry("current", OperandSupport.NOT_SUPPORTED),
          Map.entry("type", OperandSupport.REQUIRED),
          Map.entry("club", OperandSupport.NOT_SUPPORTED),
          Map.entry("shield", OperandSupport.NOT_SUPPORTED),
          Map.entry("utensil", OperandSupport.NOT_SUPPORTED),
          Map.entry("sword", OperandSupport.NOT_SUPPORTED),
          Map.entry("knife", OperandSupport.NOT_SUPPORTED),
          Map.entry("accordion", OperandSupport.NOT_SUPPORTED),
          Map.entry("melee", OperandSupport.NOT_SUPPORTED),
          Map.entry("effective", OperandSupport.NOT_SUPPORTED),
          Map.entry("empty", OperandSupport.NOT_SUPPORTED),
          Map.entry("beeosity", OperandSupport.NOT_SUPPORTED),
          Map.entry(BitmapModifier.STINKYCHEESE.getName(), OperandSupport.NOT_SUPPORTED),
          Map.entry("sea", OperandSupport.NOT_SUPPORTED),
          Map.entry("equip", OperandSupport.REQUIRED),
          Map.entry("bonus", OperandSupport.REQUIRED),
          Map.entry("modbonus", OperandSupport.REQUIRED),
          Map.entry("letter", OperandSupport.OPTIONAL),
          Map.entry("number", OperandSupport.NOT_SUPPORTED),
          Map.entry("plumber", OperandSupport.NOT_SUPPORTED),
          Map.entry("cold plumber", OperandSupport.NOT_SUPPORTED),
          Map.entry("outfit", OperandSupport.OPTIONAL),
          Map.entry("switch", OperandSupport.REQUIRED),
          Map.entry("elemental resistance", OperandSupport.NOT_SUPPORTED),
          Map.entry("elemental damage", OperandSupport.NOT_SUPPORTED),
          Map.entry("hp regen", OperandSupport.NOT_SUPPORTED),
          Map.entry("mp regen", OperandSupport.NOT_SUPPORTED),
          Map.entry("passive damage", OperandSupport.NOT_SUPPORTED),
          Map.entry("organ capacity", OperandSupport.NOT_SUPPORTED),
          Map.entry(DoubleModifier.COMBAT_RATE.getName(), OperandSupport.NOT_SUPPORTED),
          Map.entry(DoubleModifier.ADVENTURES.getName(), OperandSupport.NOT_SUPPORTED),
          Map.entry(DoubleModifier.PVP_FIGHTS.getName(), OperandSupport.NOT_SUPPORTED),
          Map.entry(
              DoubleModifier.RANDOM_MONSTER_MODIFIERS.getName(), OperandSupport.NOT_SUPPORTED),
          Map.entry(BitmapModifier.CLOWNINESS.getName(), OperandSupport.NOT_SUPPORTED),
          Map.entry(BitmapModifier.RAVEOSITY.getName(), OperandSupport.NOT_SUPPORTED),
          Map.entry(BitmapModifier.SURGEONOSITY.getName(), OperandSupport.NOT_SUPPORTED));

  static final Set<BitmapModifier> OSITY_MODIFIERS = supportedBitmapModifiers();

  private static Set<BitmapModifier> supportedBitmapModifiers() {
    var modifiers = EnumSet.allOf(BitmapModifier.class);
    modifiers.removeIf(
        modifier ->
            !DIRECTIVES.containsKey(modifier.getName()) || defaultLimitsFor(modifier) == null);
    return modifiers;
  }

  private static final List<Canonicalization> KEYWORD_CANONICALIZATIONS =
      List.of(
          canonicalization("handed|hands", "hand"),
          canonicalization("tiebreaker", "tie"),
          canonicalization("stinky ?cheese", BitmapModifier.STINKYCHEESE.getName()),
          tokenCanonicalization("mus", "muscle"),
          tokenCanonicalization("mys(t(ical(ity)?)?)?", "mysticality"),
          tokenCanonicalization("mox", "moxie"),
          tokenCanonicalization("ele", "elemental"),
          tokenCanonicalization("res(ist)?", "resistance"),
          tokenCanonicalization("dmg", "damage"),
          tokenCanonicalization("exp", "experience"),
          tokenCanonicalization("perc(ent(age)?)?|pct", "percent"),
          tokenCanonicalization("fam", "familiar"),
          canonicalization("organs?", "organ capacity"),
          canonicalization("any resistance", "elemental resistance"),
          canonicalization("main", "mainstat"),
          canonicalization("com(bat)?", DoubleModifier.COMBAT_RATE.getName()),
          canonicalization("advs?", DoubleModifier.ADVENTURES.getName()),
          canonicalization("fites?", DoubleModifier.PVP_FIGHTS.getName()),
          canonicalization("ocrs", DoubleModifier.RANDOM_MONSTER_MODIFIERS.getName()),
          canonicalization("clownosity", BitmapModifier.CLOWNINESS.getName()),
          canonicalization("init", DoubleModifier.INITIATIVE.getName()),
          canonicalization("hp", DoubleModifier.HP.getName()),
          canonicalization("mp", DoubleModifier.MP.getName()),
          canonicalization("da", DoubleModifier.DAMAGE_ABSORPTION.getName()),
          canonicalization("dr", DoubleModifier.DAMAGE_REDUCTION.getName()),
          canonicalization("ml", DoubleModifier.MONSTER_LEVEL.getName()),
          canonicalization("items?", DoubleModifier.ITEMDROP.getName()),
          canonicalization("booze", DoubleModifier.BOOZEDROP.getName()),
          canonicalization("food", DoubleModifier.FOODDROP.getName()),
          canonicalization("meat", DoubleModifier.MEATDROP.getName()),
          canonicalization("crit(ical)?", DoubleModifier.CRITICAL_PCT.getName()),
          canonicalization("spell crit(ical)?", DoubleModifier.SPELL_CRITICAL_PCT.getName()),
          canonicalization("sprinkle", DoubleModifier.SPRINKLES.getName()),
          canonicalization("stomach", DoubleModifier.STOMACH_CAPACITY.getName()),
          canonicalization("liver", DoubleModifier.LIVER_CAPACITY.getName()),
          canonicalization("spleen", DoubleModifier.SPLEEN_CAPACITY.getName()));

  private static Canonicalization tokenCanonicalization(String pattern, String canonical) {
    return new Canonicalization(Pattern.compile("\\b(?:" + pattern + ")\\b"), canonical);
  }

  private static Canonicalization canonicalization(String pattern, String canonical) {
    return new Canonicalization(Pattern.compile("^(?:" + pattern + ")$"), canonical);
  }

  private static String canonicalize(String keyword) {
    for (var canonicalization : KEYWORD_CANONICALIZATIONS) {
      keyword =
          canonicalization.pattern().matcher(keyword).replaceAll(canonicalization.canonical());
    }
    if (keyword.equals("mainstat")) {
      return DoubleModifier.primeStat().getName();
    }
    Modifier modifier = modifierFor(keyword);
    return modifier == null ? keyword : modifier.getName();
  }

  private static Modifier modifierFor(String keyword) {
    Modifier modifier = DoubleModifier.byCaselessName(keyword);
    if (modifier != null) {
      return modifier;
    }
    BitmapModifier bitmapModifier = BitmapModifier.byCaselessName(keyword);
    return bitmapModifier != null
            && DIRECTIVES.containsKey(bitmapModifier.getName())
            && defaultLimitsFor(bitmapModifier) != null
        ? bitmapModifier
        : null;
  }

  private static ModifierLimits defaultLimitsFor(Modifier modifier) {
    return switch (modifier) {
      case BitmapModifier.CLOWNINESS -> new ModifierLimits(100.0, 100.0);
      case BitmapModifier.RAVEOSITY -> new ModifierLimits(7.0, 7.0);
      case BitmapModifier.SURGEONOSITY -> new ModifierLimits(1.0, 5.0);
      case null -> null;
      default -> null;
    };
  }

  static final String TIEBREAKER =
      "1 familiar weight, 1 familiar experience, 1 initiative, 5 exp, 1 item, 1 meat, 0.1 DA 1000 max, 1 DR, 0.5 all res, -10 mana cost, 1.0 mus, 0.5 mys, 1.0 mox, 1.5 mainstat, 1 HP, 1 MP, 1 weapon damage, 1 ranged damage, 1 spell damage, 1 cold damage, 1 hot damage, 1 sleaze damage, 1 spooky damage, 1 stench damage, 1 cold spell damage, 1 hot spell damage, 1 sleaze spell damage, 1 spooky spell damage, 1 stench spell damage, -1 fumble, 1 HP regen max, 3 MP regen max, 1 critical hit percent, 0.1 food drop, 0.1 booze drop, 0.1 hat drop, 0.1 weapon drop, 0.1 offhand drop, 0.1 shirt drop, 0.1 pants drop, 0.1 accessory drop, 1 DB combat damage, 0.1 sixgun damage";
  private static final Pattern KEYWORD_PATTERN =
      Pattern.compile(
          "\\G\\s*(\\+|-|)([\\d.]*)\\s*(\"[^\"]+\"|(?:[^-+,0-9]|(?<! )[-+0-9])+),?\\s*");

  private static List<ParsedTerm> parseTerms(String expression) {
    expression = expression.trim().toLowerCase();
    Matcher matcher = KEYWORD_PATTERN.matcher(expression);
    List<ParsedTerm> terms = new ArrayList<>();
    boolean seenNonLimitTerm = false;
    int position = 0;

    while (position < expression.length()) {
      if (!matcher.find()) {
        KoLmafia.updateDisplay(
            MafiaState.ERROR, "Unable to interpret: " + expression.substring(position));
        return null;
      }
      position = matcher.end();
      ParsedTerm term = ParsedTerm.from(matcher);

      if (DIRECTIVES.get(term.keyword()) == OperandSupport.REQUIRED && term.operand().isEmpty()) {
        KoLmafia.updateDisplay(
            MafiaState.ERROR, "Directive '" + term.keyword() + "' requires an operand");
        return null;
      }

      String keyword = term.keyword();
      if (!DIRECTIVES.containsKey(keyword)
          && term.modifier() == null
          && !SlotSet.ALL_SLOTS.contains(EquipmentRequest.slotNumber(keyword))
          && BooleanModifier.byCaselessName(keyword) == null) {
        KoLmafia.updateDisplay(MafiaState.ERROR, "Unrecognized keyword: " + term.originalKeyword());
        return null;
      }

      if (term.keyword().equals("min") || term.keyword().equals("max")) {
        ParsedTerm previousTerm = terms.isEmpty() ? null : terms.getLast();
        if (previousTerm != null && previousTerm.modifier() != null) {
          term = term.withModifier(previousTerm.modifier());
        } else if (seenNonLimitTerm) {
          KoLmafia.updateDisplay(
              MafiaState.ERROR,
              term.keyword()
                  + " must follow a modifier or appear at the start of the expression; preceding term was '"
                  + previousTerm.originalKeyword()
                  + "'");
          return null;
        }
      } else {
        seenNonLimitTerm = true;
      }

      terms.add(term);
    }

    return terms;
  }

  MaximizerExpression() {
    this.totalMin = Double.NEGATIVE_INFINITY;
    this.totalMax = Double.POSITIVE_INFINITY;
    for (var modifier : DoubleModifier.DOUBLE_MODIFIERS) {
      this.min.put(modifier, Double.NEGATIVE_INFINITY);
      this.max.put(modifier, Double.POSITIVE_INFINITY);
    }
    for (var modifier : OSITY_MODIFIERS) {
      this.min.put(modifier, Double.NEGATIVE_INFINITY);
      this.max.put(modifier, Double.POSITIVE_INFINITY);
    }
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

  void parse(String expr) {
    List<ParsedTerm> terms = parseTerms(expr);
    if (terms == null) {
      return;
    }

    boolean hadFamiliar = false;
    boolean forceCurrent = false;

    int equipBeeosity = 0;
    int outfitBeeosity = 0;

    for (ParsedTerm term : terms) {
      double weight = term.weight();
      String originalKeyword = term.originalKeyword();
      String keyword = term.keyword();
      String operand = term.operand();

      switch (keyword) {
        case "min" -> {
          if (term.modifier() != null) {
            this.min.put(term.modifier(), weight);
          } else {
            this.totalMin = weight;
          }
        }
        case "max" -> {
          if (term.modifier() != null) {
            this.max.put(term.modifier(), weight);
          } else {
            this.totalMax = weight;
          }
        }
        case "dump" -> this.dump = (int) weight;
        case "hand" -> this.hands = (int) weight;
        case "tie" -> this.noTiebreaker = weight < 0.0;
        case "current" -> {
          this.current = weight > 0.0;
          forceCurrent = true;
        }
        case "type" -> this.weaponType = operand;
        case "club" -> this.requireClub = weight > 0.0;
        case "shield" -> {
          this.requireShield = weight > 0.0;
          if (forcedModeables.get(Modeable.UMBRELLA).isEmpty()) {
            forcedModeables.put(Modeable.UMBRELLA, "forward-facing");
          }
          this.hands = 1;
        }
        case "utensil" -> this.requireUtensil = weight > 0.0;
        case "sword" -> this.requireSword = weight > 0.0;
        case "knife" -> this.requireKnife = weight > 0.0;
        case "accordion" -> this.requireAccordion = weight > 0.0;
        case "melee" -> this.melee = (int) (weight * 2.0);
        case "effective" -> this.effective = weight > 0.0;
        case "empty" -> {
          for (var slot : SlotSet.ALL_SLOTS) {
            this.slots.merge(
                slot,
                ((int) weight)
                    * (EquipmentManager.getEquipment(slot).equals(EquipmentRequest.UNEQUIP)
                        ? 1
                        : -1),
                Integer::sum);
          }
        }
        case "beeosity" -> this.beeosity = (int) weight;
        case "Stinky Cheese" -> this.stinkycheese = (int) weight;
        case "sea" -> {
          var adventureUnderwater =
              EnumSet.of(BooleanModifier.ADVENTURE_UNDERWATER, BooleanModifier.UNDERWATER_FAMILIAR);
          this.booleanMask.addAll(adventureUnderwater);
          this.booleanValue.addAll(adventureUnderwater);
          if (forcedModeables.get(Modeable.EDPIECE).isEmpty()) {
            // Force Crown of Ed to Fish
            forcedModeables.put(Modeable.EDPIECE, "fish");
          }
        }
        case "equip" -> {
          var match = ItemFinder.getFirstMatchingItemWithMode(operand, Match.EQUIP);
          if (match == null) {
            return;
          }
          if (match.modeable() != null && !forceModeable(match, match.mode())) {
            return;
          }
          if (weight > 0.0) {
            if (this.posEquip.add(match.item())) {
              equipBeeosity += KoLCharacter.getBeeosity(match.item().getName());
            }
          } else {
            this.negEquip.add(match.item());
          }
        }
        case "bonus" -> {
          var match = ItemFinder.getFirstMatchingItemWithMode(operand, Match.EQUIP);
          if (match == null) {
            return;
          }
          if (match.mode() == null) {
            var existing = this.bonuses.get(match.item());
            var modes = existing == null ? new HashMap<String, Double>() : existing.modes();
            this.bonuses.put(match.item(), new ItemBonus(weight, modes));
          } else {
            this.bonuses
                .computeIfAbsent(match.item(), k -> new ItemBonus(0.0, new HashMap<>()))
                .modes()
                .put(match.mode(), weight);
          }
        }
        case "modbonus" -> {
          BooleanModifier mod = BooleanModifier.byCaselessName(operand);
          if (mod == null) {
            KoLmafia.updateDisplay(MafiaState.ERROR, "No boolean modifier found for: " + operand);
            return;
          }
          this.modBonuses.put(mod, weight);
        }
        case "letter" -> {
          if (operand.isEmpty()) {
            this.bonusFunc.add(new BonusFunction(LetterBonus::letterBonus, weight));
          } else {
            this.bonusFunc.add(
                new BonusFunction(ar -> LetterBonus.letterBonus(ar, operand), weight));
          }
        }
        case "number" -> this.bonusFunc.add(new BonusFunction(LetterBonus::numberBonus, weight));
        case "plumber" -> {
          if (!KoLCharacter.isPlumber()) {
            KoLmafia.updateDisplay(MafiaState.ERROR, "You are not a Plumber");
            return;
          }
          AdventureResult item = pickPlumberTool(KoLCharacter.getPrimeIndex());
          this.posEquip.add(item == null ? pickPlumberTool(-1) : item);
        }
        case "cold plumber" -> {
          if (!KoLCharacter.isPlumber()) {
            KoLmafia.updateDisplay(MafiaState.ERROR, "You are not a Plumber");
            return;
          }
          AdventureResult item = pickPlumberTool(1);
          if (item == null) {
            KoLmafia.updateDisplay(
                MafiaState.ERROR, "You don't have an appropriate flower to wield");
            return;
          }
          this.posEquip.add(item);
          this.posEquip.add(ItemPool.get(ItemPool.FROSTY_BUTTON));
        }
        case "outfit" -> {
          String outfitName =
              operand.isEmpty()
                  ? KoLCharacter.currentStringModifier(StringModifier.OUTFIT)
                  : operand;
          SpecialOutfit outfit = EquipmentManager.getMatchingOutfit(outfitName);
          if (outfit == null || outfit.getOutfitId() <= 0) {
            KoLmafia.updateDisplay(MafiaState.ERROR, "Unknown or custom outfit: " + outfitName);
            return;
          }
          if (weight > 0.0) {
            this.posOutfits.add(outfit.getName());
            int bees = 0;
            for (AdventureResult piece : outfit.getPieces()) {
              bees += KoLCharacter.getBeeosity(piece.getName());
            }
            outfitBeeosity = Math.max(outfitBeeosity, bees);
          } else {
            this.negOutfits.add(outfit.getName());
          }
        }
        case "switch" -> {
          if (!KoLCharacter.inPokefam()) {
            int id = FamiliarDatabase.getFamiliarId(operand);
            if (id == -1) {
              KoLmafia.updateDisplay(MafiaState.ERROR, "Unknown familiar: " + operand);
              return;
            }
            if (!hadFamiliar || weight >= 0.0) {
              FamiliarData familiar = KoLCharacter.usableFamiliar(id);
              hadFamiliar = familiar != null;
              if (familiar != null
                  && !familiar.equals(KoLCharacter.getFamiliar())
                  && familiar.canEquip()
                  && !this.familiars.contains(familiar)) {
                this.familiars.add(familiar);
              }
            }
          }
        }
        case "elemental resistance" -> {
          this.weight.put(DoubleModifier.COLD_RESISTANCE, weight);
          this.weight.put(DoubleModifier.HOT_RESISTANCE, weight);
          this.weight.put(DoubleModifier.SLEAZE_RESISTANCE, weight);
          this.weight.put(DoubleModifier.SPOOKY_RESISTANCE, weight);
          this.weight.put(DoubleModifier.STENCH_RESISTANCE, weight);
        }
        case "elemental damage" -> {
          this.weight.put(DoubleModifier.COLD_DAMAGE, weight);
          this.weight.put(DoubleModifier.HOT_DAMAGE, weight);
          this.weight.put(DoubleModifier.SLEAZE_DAMAGE, weight);
          this.weight.put(DoubleModifier.SPOOKY_DAMAGE, weight);
          this.weight.put(DoubleModifier.STENCH_DAMAGE, weight);
        }
        case "hp regen" -> {
          this.weight.put(DoubleModifier.HP_REGEN_MIN, weight / 2);
          this.weight.put(DoubleModifier.HP_REGEN_MAX, weight / 2);
        }
        case "mp regen" -> {
          this.weight.put(DoubleModifier.MP_REGEN_MIN, weight / 2);
          this.weight.put(DoubleModifier.MP_REGEN_MAX, weight / 2);
        }
        case "passive damage" -> {
          this.weight.put(DoubleModifier.DAMAGE_AURA, weight);
          this.weight.put(DoubleModifier.THORNS, weight);
        }
        case "organ capacity" -> {
          this.weight.put(DoubleModifier.STOMACH_CAPACITY, weight);
          this.weight.put(DoubleModifier.LIVER_CAPACITY, weight);
          this.weight.put(DoubleModifier.SPLEEN_CAPACITY, weight);
        }
        default -> {
          Slot slot = EquipmentRequest.slotNumber(keyword);
          Modifier modifier = term.modifier();
          BooleanModifier booleanModifier =
              modifier == null ? BooleanModifier.byCaselessName(keyword) : null;

          if (SlotSet.ALL_SLOTS.contains(slot)) {
            this.slots.merge(slot, (int) weight, Integer::sum);
          } else if (booleanModifier != null) {
            this.booleanMask.add(booleanModifier);
            if (weight > 0.0) {
              this.booleanValue.add(booleanModifier);
            }
          } else if (modifier != null) {
            if (modifier == DoubleModifier.COMBAT_RATE
                && AdventureDatabase.isUnderwater(Modifiers.currentLocation)) {
              this.weight.put(DoubleModifier.UNDERWATER_COMBAT_RATE, weight);
            }
            switch (originalKeyword) {
              case "adv", "fites" -> this.beeosity = 999;
              case "ocrs" -> {
                this.noTiebreaker = true;
                this.beeosity = 999;
              }
            }

            this.weight.put(modifier, weight);
            ModifierLimits defaultLimits = defaultLimitsFor(modifier);
            if (defaultLimits != null) {
              this.min.put(modifier, defaultLimits.minimum());
              this.max.put(modifier, defaultLimits.maximum());
            }
          } else {
            KoLmafia.updateDisplay(MafiaState.ERROR, "Unrecognized keyword: " + originalKeyword);
            return;
          }
        }
      }
    }

    if (!forceCurrent && this.noTiebreaker) {
      this.current = true;
    }

    this.beeosity = Math.max(Math.max(this.beeosity, equipBeeosity), outfitBeeosity);

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
    final double fudge = this.weight.getOrDefault(source, 0.0) * 0.0001f;
    if (fudge > 0) {
      for (var extra : extras) {
        this.weight.merge(extra, fudge, Double::sum);
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

    return switch (primeIndex) {
      case 0 -> haveHeavyHammer ? heavyHammer : haveHammer ? hammer : null;
      case 1 -> haveBonfireFlower ? bonfireFlower : haveFireFlower ? fireFlower : null;
      case 2 -> haveFancyBoots ? fancyBoots : haveWorkBoots ? workBoots : null;
      default ->
          haveHeavyHammer
              ? heavyHammer
              : haveBonfireFlower
                  ? bonfireFlower
                  : haveFancyBoots
                      ? fancyBoots
                      : haveHammer ? hammer : haveFireFlower ? fireFlower : workBoots;
    };
  }
}
