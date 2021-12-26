package net.sourceforge.kolmafia.maximizer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.FamiliarData;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLConstants.WeaponType;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.Modifiers;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.SpecialOutfit;
import net.sourceforge.kolmafia.objectpool.EffectPool;
import net.sourceforge.kolmafia.objectpool.FamiliarPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.AdventureDatabase;
import net.sourceforge.kolmafia.persistence.EquipmentDatabase;
import net.sourceforge.kolmafia.persistence.FamiliarDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase.FoldGroup;
import net.sourceforge.kolmafia.persistence.ItemFinder;
import net.sourceforge.kolmafia.persistence.ItemFinder.Match;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.EquipmentRequest;
import net.sourceforge.kolmafia.request.StandardRequest;
import net.sourceforge.kolmafia.session.EquipmentManager;
import net.sourceforge.kolmafia.session.InventoryManager;
import net.sourceforge.kolmafia.textui.command.BackupCameraCommand;
import net.sourceforge.kolmafia.textui.command.EdPieceCommand;
import net.sourceforge.kolmafia.textui.command.RetroCapeCommand;
import net.sourceforge.kolmafia.textui.command.SnowsuitCommand;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class Evaluator {
  public boolean failed;
  boolean exceeded;
  private Evaluator tiebreaker;
  private final double[] weight = new double[Modifiers.DOUBLE_MODIFIERS];
  private double[] min;
  private double[] max;
  private double totalMin, totalMax;
  private int dump = 0;
  private int clownosity = 0;
  private int raveosity = 0;
  private int surgeonosity = 0;
  private int beeosity = 2;
  private int booleanMask, booleanValue;
  private final List<FamiliarData> familiars = new ArrayList<>();
  private final List<FamiliarData> carriedFamiliars = new ArrayList<>();
  private int carriedFamiliarsNeeded = 0;
  private boolean cardNeeded = false;
  private boolean edPieceNeeded = false;
  private String edPieceDecided = null;
  private boolean snowsuitNeeded = false;
  private boolean retroCapeNeeded = false;
  private boolean backupCameraNeeded = false;

  private final int[] slots = new int[EquipmentManager.ALL_SLOTS];
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
  private final Set<AdventureResult> uniques = new HashSet<>();
  private final Map<AdventureResult, Double> bonuses = new HashMap<>();

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
  static final int OFFHAND_MELEE = EquipmentManager.ACCESSORY2;
  static final int OFFHAND_RANGED = EquipmentManager.ACCESSORY3;
  static final int WATCHES = EquipmentManager.STICKER2;
  static final int WEAPON_1H = EquipmentManager.STICKER3;
  // Slots starting with EquipmentManager.ALL_SLOTS are equipment
  // for other familiars being considered.

  private static int relevantSkill(String skill) {
    return KoLCharacter.hasSkill(skill) ? 1 : 0;
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

  private int maxUseful(int slot) {
    switch (slot) {
      case Evaluator.WEAPON_1H:
        return 1
            + relevantSkill("Double-Fisted Skull Smashing")
            + this.relevantFamiliar(FamiliarPool.HAND);
      case EquipmentManager.OFFHAND:
        return 1 + this.relevantFamiliar(FamiliarPool.LEFT_HAND);
      case EquipmentManager.ACCESSORY1:
        return 3;
      case EquipmentManager.FAMILIAR:
        // Familiar items include weapons, hats and pants, make sure we have enough to consider for
        // other slots
        return 1
            + this.relevantFamiliar(FamiliarPool.SCARECROW)
            + this.relevantFamiliar(FamiliarPool.HAND)
            + this.relevantFamiliar(FamiliarPool.HATRACK);
    }
    return 1;
  }

  private static int toUseSlot(int slot) {
    int useSlot = slot;
    switch (slot) {
      case Evaluator.OFFHAND_MELEE:
      case Evaluator.OFFHAND_RANGED:
        useSlot = EquipmentManager.OFFHAND;
        break;
      case Evaluator.WATCHES:
        useSlot = EquipmentManager.ACCESSORY1;
        break;
      case Evaluator.WEAPON_1H:
        useSlot = EquipmentManager.WEAPON;
        break;
    }
    return useSlot;
  }

  private Evaluator() {
    this.totalMin = Double.NEGATIVE_INFINITY;
    this.totalMax = Double.POSITIVE_INFINITY;
  }

  Evaluator(String expr) {
    this();

    Evaluator tiebreaker = new Evaluator();
    this.tiebreaker = tiebreaker;
    tiebreaker.min = new double[Modifiers.DOUBLE_MODIFIERS];
    tiebreaker.max = new double[Modifiers.DOUBLE_MODIFIERS];
    Arrays.fill(tiebreaker.min, Double.NEGATIVE_INFINITY);
    Arrays.fill(tiebreaker.max, Double.POSITIVE_INFINITY);
    tiebreaker.parse(Evaluator.TIEBREAKER);

    this.min = tiebreaker.min.clone();
    this.max = tiebreaker.max.clone();
    this.parse(expr);
  }

  private void addUniqueItems(String name) {
    Set<String> itemNames = Modifiers.getUniques(name);
    if (itemNames != null) {
      for (String itemName : itemNames) {
        this.uniques.add(ItemPool.get(itemName, 1));
      }
    }
  }

  private void parse(String expr) {
    expr = expr.trim().toLowerCase();
    Matcher m = KEYWORD_PATTERN.matcher(expr);
    boolean hadFamiliar = false;
    boolean forceCurrent = false;
    int pos = 0;
    int index = -1;

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
        if (index >= 0) {
          this.min[index] = weight;
        } else {
          this.totalMin = weight;
        }
        continue;
      }

      if (keyword.equals("max")) {
        if (index >= 0) {
          this.max[index] = weight;
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
        for (int i = 0; i < EquipmentManager.ALL_SLOTS; ++i) {
          this.slots[i] +=
              ((int) weight)
                  * (EquipmentManager.getEquipment(i).equals(EquipmentRequest.UNEQUIP) ? 1 : -1);
        }
        continue;
      }

      if (keyword.equals("clownosity")) {
        // If no weight specified, assume 4
        this.clownosity = (m.end(2) == m.start(2)) ? 4 : (int) weight;

        // Clownosity is built on Clowniness and has
        // same unique items requirement.
        this.addUniqueItems("Clowniness");
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
        this.addUniqueItems("Surgeonosity");
        continue;
      }

      if (keyword.equals("beeosity")) {
        this.beeosity = (int) weight;
        continue;
      }

      if (keyword.equals("sea")) {
        this.booleanMask |=
            (1 << Modifiers.ADVENTURE_UNDERWATER) | (1 << Modifiers.UNDERWATER_FAMILIAR);
        this.booleanValue |=
            (1 << Modifiers.ADVENTURE_UNDERWATER) | (1 << Modifiers.UNDERWATER_FAMILIAR);
        index = -1;
        // Force Crown of Ed to Fish
        this.edPieceDecided = "fish";
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

      if (keyword.equals("plumber")) {
        if (!KoLCharacter.isPlumber()) {
          KoLmafia.updateDisplay(MafiaState.ERROR, "You are not a Plumber");
          return;
        }
        // Pick a tool that matches your prime stat
        AdventureResult item = pickPlumberTool(KoLCharacter.getPrimeIndex(), true);
        if (item == null) {
          // Otherwise, pick best available tool
          // You are guaranteed to have work boots, at least
          item = pickPlumberTool(-1, true);
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
        AdventureResult item1 = pickPlumberTool(1, true);
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
        if (keyword.equals("")) { // allow "+outfit" to mean "keep the current outfit on"
          keyword = KoLCharacter.currentStringModifier(Modifiers.OUTFIT);
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
        FamiliarData fam = KoLCharacter.findFamiliar(id);
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

      int slot = EquipmentRequest.slotNumber(keyword);
      if (slot >= 0 && slot < EquipmentManager.ALL_SLOTS) {
        this.slots[slot] += (int) weight;
        continue;
      }

      index = Modifiers.findName(keyword);

      // Adjust for generic abbreviations
      if (index < 0) {
        if (keyword.endsWith(" res")) {
          keyword += "istance";
        } else if (keyword.endsWith(" dmg")) {
          keyword = keyword.substring(0, keyword.length() - 3) + "damage";
        } else if (keyword.endsWith(" dmg percent")) {
          keyword = keyword.substring(0, keyword.length() - 11) + "damage percent";
        } else if (keyword.endsWith(" exp")) {
          keyword = keyword.substring(0, keyword.length() - 3) + "experience";
        }
        index = Modifiers.findName(keyword);
      }

      if (index < 0) {
        int boolIndex = Modifiers.findBooleanName(keyword);
        if (boolIndex >= 0) {
          this.booleanMask |= 1 << boolIndex;
          if (weight > 0.0) {
            this.booleanValue |= 1 << boolIndex;
          }
          continue;
        }
      }

      // Match keyword with multiple modifiers
      if (index < 0) {
        if (keyword.equals("all resistance")) {
          this.weight[Modifiers.COLD_RESISTANCE] = weight;
          this.weight[Modifiers.HOT_RESISTANCE] = weight;
          this.weight[Modifiers.SLEAZE_RESISTANCE] = weight;
          this.weight[Modifiers.SPOOKY_RESISTANCE] = weight;
          this.weight[Modifiers.STENCH_RESISTANCE] = weight;
          continue;
        }
        if (keyword.equals("elemental damage")) {
          this.weight[Modifiers.COLD_DAMAGE] = weight;
          this.weight[Modifiers.HOT_DAMAGE] = weight;
          this.weight[Modifiers.SLEAZE_DAMAGE] = weight;
          this.weight[Modifiers.SPOOKY_DAMAGE] = weight;
          this.weight[Modifiers.STENCH_DAMAGE] = weight;
          continue;
        }
        if (keyword.equals("hp regen")) {
          this.weight[Modifiers.HP_REGEN_MIN] = weight / 2;
          this.weight[Modifiers.HP_REGEN_MAX] = weight / 2;
          continue;
        }
        if (keyword.equals("mp regen")) {
          this.weight[Modifiers.MP_REGEN_MIN] = weight / 2;
          this.weight[Modifiers.MP_REGEN_MAX] = weight / 2;
          continue;
        }
      }

      // Match keyword with specific abbreviations
      if (index < 0) {
        if (keyword.equals("init")) {
          index = Modifiers.INITIATIVE;
        } else if (keyword.equals("hp")) {
          index = Modifiers.HP;
        } else if (keyword.equals("mp")) {
          index = Modifiers.MP;
        } else if (keyword.equals("da")) {
          index = Modifiers.DAMAGE_ABSORPTION;
        } else if (keyword.equals("dr")) {
          index = Modifiers.DAMAGE_REDUCTION;
        } else if (keyword.equals("ml")) {
          index = Modifiers.MONSTER_LEVEL;
        } else if (keyword.startsWith("mus")) {
          index = Modifiers.MUS;
        } else if (keyword.startsWith("mys")) {
          index = Modifiers.MYS;
        } else if (keyword.startsWith("mox")) {
          index = Modifiers.MOX;
        } else if (keyword.startsWith("main")) {
          switch (KoLCharacter.getPrimeIndex()) {
            case 0:
              index = Modifiers.MUS;
              break;
            case 1:
              index = Modifiers.MYS;
              break;
            case 2:
              index = Modifiers.MOX;
              break;
          }
        } else if (keyword.startsWith("com")) {
          index = Modifiers.COMBAT_RATE;
          if ("underwater".equals(AdventureDatabase.getEnvironment(Modifiers.currentLocation))) {
            this.weight[Modifiers.UNDERWATER_COMBAT_RATE] = weight;
          }
        } else if (keyword.startsWith("item")) {
          index = Modifiers.ITEMDROP;
        } else if (keyword.startsWith("meat")) {
          index = Modifiers.MEATDROP;
        } else if (keyword.startsWith("adv")) {
          this.beeosity = 999;
          index = Modifiers.ADVENTURES;
        } else if (keyword.startsWith("fites")) {
          this.beeosity = 999;
          index = Modifiers.PVP_FIGHTS;
        } else if (keyword.startsWith("exp")) {
          index = Modifiers.EXPERIENCE;
        } else if (keyword.startsWith("crit")) {
          index = Modifiers.CRITICAL_PCT;
        } else if (keyword.startsWith("spell crit")) {
          index = Modifiers.SPELL_CRITICAL_PCT;
        } else if (keyword.startsWith("sprinkle")) {
          index = Modifiers.SPRINKLES;
        } else if (keyword.equals("ocrs")) {
          this.noTiebreaker = true;
          this.beeosity = 999;
          index = Modifiers.RANDOM_MONSTER_MODIFIERS;
        }
      }

      if (index >= 0) {
        // We found a match. If only the first instance
        // of particular equipped items provide this
        // modifier, add them to the "uniques" list.
        String modifierName = Modifiers.getModifierName(index);
        this.addUniqueItems(modifierName);
        this.weight[index] = weight;
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
    double fudge = this.weight[Modifiers.EXPERIENCE] * 0.0001f;
    this.weight[Modifiers.MONSTER_LEVEL] += fudge;
    this.weight[Modifiers.MUS_EXPERIENCE] += fudge;
    this.weight[Modifiers.MYS_EXPERIENCE] += fudge;
    this.weight[Modifiers.MOX_EXPERIENCE] += fudge;
    this.weight[Modifiers.MUS_EXPERIENCE_PCT] += fudge;
    this.weight[Modifiers.MYS_EXPERIENCE_PCT] += fudge;
    this.weight[Modifiers.MOX_EXPERIENCE_PCT] += fudge;
    this.weight[Modifiers.VOLLEYBALL_WEIGHT] += fudge;
    this.weight[Modifiers.SOMBRERO_WEIGHT] += fudge;
    this.weight[Modifiers.VOLLEYBALL_EFFECTIVENESS] += fudge;
    this.weight[Modifiers.SOMBRERO_EFFECTIVENESS] += fudge;
    this.weight[Modifiers.SOMBRERO_BONUS] += fudge;

    fudge = this.weight[Modifiers.ITEMDROP] * 0.0001f;
    this.weight[Modifiers.FOODDROP] += fudge;
    this.weight[Modifiers.BOOZEDROP] += fudge;
    this.weight[Modifiers.HATDROP] += fudge;
    this.weight[Modifiers.WEAPONDROP] += fudge;
    this.weight[Modifiers.OFFHANDDROP] += fudge;
    this.weight[Modifiers.SHIRTDROP] += fudge;
    this.weight[Modifiers.PANTSDROP] += fudge;
    this.weight[Modifiers.ACCESSORYDROP] += fudge;
    this.weight[Modifiers.CANDYDROP] += fudge;
    this.weight[Modifiers.GEARDROP] += fudge;
    this.weight[Modifiers.FAIRY_WEIGHT] += fudge;
    this.weight[Modifiers.FAIRY_EFFECTIVENESS] += fudge;
    this.weight[Modifiers.SPORADIC_ITEMDROP] += fudge;
    this.weight[Modifiers.PICKPOCKET_CHANCE] += fudge;

    fudge = this.weight[Modifiers.MEATDROP] * 0.0001f;
    this.weight[Modifiers.LEPRECHAUN_WEIGHT] += fudge;
    this.weight[Modifiers.LEPRECHAUN_EFFECTIVENESS] += fudge;
    this.weight[Modifiers.SPORADIC_MEATDROP] += fudge;
    this.weight[Modifiers.MEAT_BONUS] += fudge;
  }

  private AdventureResult pickPlumberTool(int primeIndex, boolean have) {
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
    switch (primeIndex) {
      case 0: // Muscle
        return !have ? heavyHammer : haveHeavyHammer ? heavyHammer : haveHammer ? hammer : null;
      case 1: // Mysticality
        return !have
            ? bonfireFlower
            : haveBonfireFlower ? bonfireFlower : haveFireFlower ? fireFlower : null;
      case 2: // Moxie
        return !have ? fancyBoots : haveFancyBoots ? fancyBoots : haveWorkBoots ? workBoots : null;
    }

    // If you don't care about stat, pick the best item you own.
    return haveHeavyHammer
        ? heavyHammer
        : haveBonfireFlower
            ? bonfireFlower
            : haveFancyBoots
                ? fancyBoots
                : haveHammer ? hammer : haveFireFlower ? fireFlower : workBoots;
  }

  public double getScore(Modifiers mods, AdventureResult[] equipment) {
    this.failed = false;
    this.exceeded = false;
    int[] predicted = mods.predict();

    double score = 0.0;
    for (int i = 0; i < Modifiers.DOUBLE_MODIFIERS; ++i) {
      double weight = this.weight[i];
      double min = this.min[i];
      if (weight == 0.0 && min == Double.NEGATIVE_INFINITY) continue;
      double val = mods.get(i);
      double max = this.max[i];
      switch (i) {
        case Modifiers.MUS:
          val = predicted[Modifiers.BUFFED_MUS];
          break;
        case Modifiers.MYS:
          val = predicted[Modifiers.BUFFED_MYS];
          break;
        case Modifiers.MOX:
          val = predicted[Modifiers.BUFFED_MOX];
          break;
        case Modifiers.FAMILIAR_WEIGHT:
          val += mods.get(Modifiers.HIDDEN_FAMILIAR_WEIGHT);
          if (mods.get(Modifiers.FAMILIAR_WEIGHT_PCT) < 0.0) {
            val *= 0.5f;
          }
          break;
        case Modifiers.MANA_COST:
          val += mods.get(Modifiers.STACKABLE_MANA_COST);
          break;
        case Modifiers.INITIATIVE:
          val += Math.min(0.0, mods.get(Modifiers.INITIATIVE_PENALTY));
          break;
        case Modifiers.MEATDROP:
          val +=
              100.0
                  + Math.min(0.0, mods.get(Modifiers.MEATDROP_PENALTY))
                  + mods.get(Modifiers.SPORADIC_MEATDROP)
                  + mods.get(Modifiers.MEAT_BONUS) / 10000.0;
          break;
        case Modifiers.ITEMDROP:
          val +=
              100.0
                  + Math.min(0.0, mods.get(Modifiers.ITEMDROP_PENALTY))
                  + mods.get(Modifiers.SPORADIC_ITEMDROP);
          break;
        case Modifiers.HP:
          val = predicted[Modifiers.BUFFED_HP];
          break;
        case Modifiers.MP:
          val = predicted[Modifiers.BUFFED_MP];
          break;
        case Modifiers.WEAPON_DAMAGE:
          // Incorrect - needs to estimate base damage
          val += mods.get(Modifiers.WEAPON_DAMAGE_PCT);
          break;
        case Modifiers.RANGED_DAMAGE:
          // Incorrect - needs to estimate base damage
          val += mods.get(Modifiers.RANGED_DAMAGE_PCT);
          break;
        case Modifiers.SPELL_DAMAGE:
          // Incorrect - base damage depends on spell used
          val += mods.get(Modifiers.SPELL_DAMAGE_PCT);
          break;
        case Modifiers.COLD_RESISTANCE:
        case Modifiers.HOT_RESISTANCE:
        case Modifiers.SLEAZE_RESISTANCE:
        case Modifiers.SPOOKY_RESISTANCE:
        case Modifiers.STENCH_RESISTANCE:
          if (mods.getBoolean(i - Modifiers.COLD_RESISTANCE + Modifiers.COLD_IMMUNITY)) {
            val = 100.0;
          } else if (mods.getBoolean(
              i - Modifiers.COLD_RESISTANCE + Modifiers.COLD_VULNERABILITY)) {
            val -= 100.0;
          }
          break;
        case Modifiers.EXPERIENCE:
          double baseExp = KoLCharacter.estimatedBaseExp(mods.get(Modifiers.MONSTER_LEVEL));
          double expPct =
              mods.get(Modifiers.MUS_EXPERIENCE_PCT + KoLCharacter.getPrimeIndex()) / 100.0f;
          double exp = mods.get(Modifiers.MUS_EXPERIENCE + KoLCharacter.getPrimeIndex());

          val = ((baseExp + exp) * (1 + expPct)) / 2.0f;
          break;
      }
      if (val < min) this.failed = true;
      score += weight * Math.min(val, max);
    }
    if (!this.bonuses.isEmpty()) {
      for (AdventureResult item : equipment) {
        if (this.bonuses.containsKey(item)) {
          score += this.bonuses.get(item);
        }
      }
    }
    // Add fudge factor for Rollover Effect
    if (mods.getString(Modifiers.ROLLOVER_EFFECT).length() > 0) {
      score += 0.01f;
    }
    if (score < this.totalMin) this.failed = true;
    if (score >= this.totalMax) this.exceeded = true;
    // special handling for -osity:
    // The "weight" specified is actually the desired -osity.
    // Allow partials to contribute to the score (1:1 ratio) up to the desired value.
    // Similar to setting a max.
    if (this.clownosity > 0) {
      int osity = ((int) mods.get(Modifiers.CLOWNINESS)) / 25;
      score += Math.min(osity, this.clownosity);
      if (osity < this.clownosity) this.failed = true;
    }
    if (this.raveosity > 0) {
      int osity = mods.getBitmap(Modifiers.RAVEOSITY);
      score += Math.min(osity, this.raveosity);
      if (osity < this.raveosity) this.failed = true;
    }
    if (this.surgeonosity > 0) {
      int osity = (int) mods.get(Modifiers.SURGEONOSITY);
      score += Math.min(osity, this.surgeonosity);
      if (osity < this.surgeonosity) this.failed = true;
    }
    if (!this.failed
        && this.booleanMask != 0
        && (mods.getRawBitmap(0) & this.booleanMask) != this.booleanValue) {
      this.failed = true;
    }
    return score;
  }

  public double getScore(Modifiers mods) {
    return this.getScore(mods, new AdventureResult[0]);
  }

  void checkEquipment(Modifiers mods, AdventureResult[] equipment, int beeosity) {
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
      String outfit = mods.getString(Modifiers.OUTFIT);
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

  int checkConstraints(Modifiers mods) {
    // Return value:
    //	-1: item violates a constraint, don't use it
    //	0: item not relevant to any constraints
    //	1: item meets a constraint, give it special handling
    if (mods == null) return 0;
    int bools = mods.getRawBitmap(0) & this.booleanMask;
    if ((bools & ~this.booleanValue) != 0) return -1;
    if (bools != 0) return 1;
    return 0;
  }

  public static boolean checkEffectConstraints(int effectId) {
    // Return true if effect cannot be gained due to current other effects or class
    switch (effectId) {
      case EffectPool.NEARLY_SILENT_HUNTING:
        return KoLCharacter.isSealClubber();

      case EffectPool.SILENT_HUNTING:
      case EffectPool.BARREL_CHESTED:
        return !KoLCharacter.isSealClubber();

      case EffectPool.BOON_OF_SHE_WHO_WAS:
        return KoLCharacter.getBlessingType() != KoLCharacter.SHE_WHO_WAS_BLESSING
            || KoLCharacter.getBlessingLevel() == 4;

      case EffectPool.BOON_OF_THE_STORM_TORTOISE:
        return KoLCharacter.getBlessingType() != KoLCharacter.STORM_BLESSING
            || KoLCharacter.getBlessingLevel() == 4;

      case EffectPool.BOON_OF_THE_WAR_SNAPPER:
        return KoLCharacter.getBlessingType() != KoLCharacter.WAR_BLESSING
            || KoLCharacter.getBlessingLevel() == 4;

      case EffectPool.AVATAR_OF_SHE_WHO_WAS:
        return KoLCharacter.getBlessingType() != KoLCharacter.SHE_WHO_WAS_BLESSING
            || KoLCharacter.getBlessingLevel() != 3;

      case EffectPool.AVATAR_OF_THE_STORM_TORTOISE:
        return KoLCharacter.getBlessingType() != KoLCharacter.STORM_BLESSING
            || KoLCharacter.getBlessingLevel() != 3;

      case EffectPool.AVATAR_OF_THE_WAR_SNAPPER:
        return KoLCharacter.getBlessingType() != KoLCharacter.WAR_BLESSING
            || KoLCharacter.getBlessingLevel() != 3;

      case EffectPool.BLESSING_OF_SHE_WHO_WAS:
        return !KoLCharacter.isTurtleTamer()
            || KoLCharacter.getBlessingType() == KoLCharacter.SHE_WHO_WAS_BLESSING
            || KoLCharacter.getBlessingLevel() == -1
            || KoLCharacter.getBlessingLevel() == 4;

      case EffectPool.BLESSING_OF_THE_STORM_TORTOISE:
        return !KoLCharacter.isTurtleTamer()
            || KoLCharacter.getBlessingType() == KoLCharacter.STORM_BLESSING
            || KoLCharacter.getBlessingLevel() == -1
            || KoLCharacter.getBlessingLevel() == 4;

      case EffectPool.BLESSING_OF_THE_WAR_SNAPPER:
        return !KoLCharacter.isTurtleTamer()
            || KoLCharacter.getBlessingType() == KoLCharacter.WAR_BLESSING
            || KoLCharacter.getBlessingLevel() == -1
            || KoLCharacter.getBlessingLevel() == 4;

      case EffectPool.DISDAIN_OF_SHE_WHO_WAS:
      case EffectPool.DISDAIN_OF_THE_STORM_TORTOISE:
      case EffectPool.DISDAIN_OF_THE_WAR_SNAPPER:
        return KoLCharacter.isTurtleTamer();

      case EffectPool.BARREL_OF_LAUGHS:
        return !KoLCharacter.isTurtleTamer();

      case EffectPool.FLIMSY_SHIELD_OF_THE_PASTALORD:
      case EffectPool.BLOODY_POTATO_BITS:
      case EffectPool.SLINKING_NOODLE_GLOB:
      case EffectPool.WHISPERING_STRANDS:
      case EffectPool.MACARONI_COATING:
      case EffectPool.PENNE_FEDORA:
      case EffectPool.PASTA_EYEBALL:
      case EffectPool.SPICE_HAZE:
        return KoLCharacter.isPastamancer();

      case EffectPool.SHIELD_OF_THE_PASTALORD:
      case EffectPool.PORK_BARREL:
        return !KoLCharacter.isPastamancer();

      case EffectPool.BLOOD_SUGAR_SAUCE_MAGIC:
      case EffectPool.SOULERSKATES:
      case EffectPool.WARLOCK_WARSTOCK_WARBARREL:
        return !KoLCharacter.isSauceror();

      case EffectPool.BLOOD_SUGAR_SAUCE_MAGIC_LITE:
        return KoLCharacter.isSauceror();

      case EffectPool.DOUBLE_BARRELED:
        return !KoLCharacter.isDiscoBandit();

      case EffectPool.BEER_BARREL_POLKA:
        return !KoLCharacter.isAccordionThief();

      case EffectPool.UNMUFFLED:
        return !Preferences.getString("peteMotorbikeMuffler").equals("Extra-Loud Muffler");

      case EffectPool.MUFFLED:
        return !Preferences.getString("peteMotorbikeMuffler").equals("Extra-Quiet Muffler");
    }
    return false;
  }

  void enumerateEquipment(int equipScope, int maxPrice, int priceLevel)
      throws MaximizerInterruptedException {
    int slots = EquipmentManager.ALL_SLOTS + this.familiars.size();
    // Items automatically considered regardless of their score -
    // synergies, hobo power, brimstone, etc.
    List<List<CheckedItem>> automatic = new ArrayList<>(slots);
    // Items to be considered based on their score
    List<List<CheckedItem>> ranked = new ArrayList<>(slots);
    for (int i = 0; i < slots; ++i) {
      automatic.add(new ArrayList<CheckedItem>());
      ranked.add(new ArrayList<CheckedItem>());
    }

    double nullScore = this.getScore(new Modifiers());

    Map<Integer, Boolean> usefulOutfits = new HashMap<>();
    Map<AdventureResult, AdventureResult> outfitPieces = new HashMap<>();
    for (int i = 1; i < EquipmentDatabase.normalOutfits.size(); ++i) {
      SpecialOutfit outfit = EquipmentDatabase.normalOutfits.get(i);
      if (outfit == null) continue;
      if (this.negOutfits.contains(outfit.getName())) continue;
      if (this.posOutfits.contains(outfit.getName())) {
        usefulOutfits.put(i, true);
        continue;
      }

      Modifiers mods = Modifiers.getModifiers("Outfit", outfit.getName());
      if (mods == null) continue;

      switch (this.checkConstraints(mods)) {
        case -1:
          continue;
        case 0:
          // intentionally not including outfit.getPieces() because this is
          // only rating whether the outfit itself is useful, not its pieces
          double delta = this.getScore(mods) - nullScore;
          if (delta <= 0.0) continue;
          break;
      }
      usefulOutfits.put(i, true);
    }

    int usefulSynergies = 0;
    for (Entry<String, Integer> entry : Modifiers.getSynergies()) {
      Modifiers mods = Modifiers.getModifiers("Synergy", entry.getKey());
      int value = entry.getValue().intValue();
      if (mods == null) continue;
      double delta = this.getScore(mods) - nullScore;
      if (delta > 0.0) usefulSynergies |= value;
    }

    boolean hoboPowerUseful = false;
    {
      Modifiers mods = Modifiers.getModifiers("MaxCat", "_hoboPower");
      if (mods != null && this.getScore(mods) - nullScore > 0.0) {
        hoboPowerUseful = true;
      }
    }

    boolean smithsnessUseful = false;
    {
      Modifiers mods = Modifiers.getModifiers("MaxCat", "_smithsness");
      if (mods != null && this.getScore(mods) - nullScore > 0.0) {
        smithsnessUseful = true;
      }
    }

    boolean brimstoneUseful = false;
    {
      Modifiers mods = Modifiers.getModifiers("MaxCat", "_brimstone");
      if (mods != null && this.getScore(mods) - nullScore > 0.0) {
        brimstoneUseful = true;
      }
    }

    boolean cloathingUseful = false;
    {
      Modifiers mods = Modifiers.getModifiers("MaxCat", "_cloathing");
      if (mods != null && this.getScore(mods) - nullScore > 0.0) {
        cloathingUseful = true;
      }
    }

    boolean slimeHateUseful = false;
    {
      Modifiers mods = Modifiers.getModifiers("MaxCat", "_slimeHate");
      if (mods != null && this.getScore(mods) - nullScore > 0.0) {
        slimeHateUseful = true;
      }
    }

    // This relies on the special sauce glove having a lower ID
    // than any chefstaff.
    boolean gloveAvailable = false;

    int id = 0;
    while ((id = EquipmentDatabase.nextEquipmentItemId(id)) != -1) {
      int slot = EquipmentManager.itemIdToEquipmentType(id);
      if (slot < 0 || slot >= EquipmentManager.ALL_SLOTS) continue;
      AdventureResult preItem = ItemPool.get(id, 1);
      String name = preItem.getName();
      CheckedItem item = null;
      if (this.negEquip.contains(preItem)) continue;
      if (KoLCharacter.inBeecore()
          && KoLCharacter.getBeeosity(name) > this.beeosity) { // too beechin' all by itself!
        continue;
      }
      boolean famCanEquip = KoLCharacter.getFamiliar().canEquip(preItem);
      if (famCanEquip && slot != EquipmentManager.FAMILIAR) {
        // Modifiers when worn by Hatrack or Scarecrow
        Modifiers familiarMods = new Modifiers();
        int familiarId = KoLCharacter.getFamiliar().getId();
        if ((familiarId == FamiliarPool.HATRACK && slot == EquipmentManager.HAT)
            || (familiarId == FamiliarPool.SCARECROW && slot == EquipmentManager.PANTS)) {
          familiarMods.applyFamiliarModifiers(KoLCharacter.getFamiliar(), preItem);
        }
        // Normal item modifiers when used by Disembodied Hand and Left-Hand
        else {
          familiarMods = Modifiers.getItemModifiersInFamiliarSlot(id);

          // Some items work differently with the Left Hand
          if (familiarId == FamiliarPool.LEFT_HAND) {
            switch (id) {
              case ItemPool.KOL_COL_13_SNOWGLOBE:
              case ItemPool.GLOWING_ESCA:
                familiarMods = null;
                break;
            }
          }
        }

        // no enchantments
        if (familiarMods == null) {
          familiarMods = new Modifiers();
        }

        item = new CheckedItem(id, equipScope, maxPrice, priceLevel);

        switch (this.checkConstraints(familiarMods)) {
          case -1:
            continue;
          case 1:
            item.automaticFlag = true;
        }

        if (item.getCount() != 0
            && (this.getScore(familiarMods) - nullScore > 0.0 || item.automaticFlag == true)) {
          ranked.get(EquipmentManager.FAMILIAR).add(item);
        }
      }
      for (int f = this.familiars.size() - 1; f >= 0; --f) {
        FamiliarData fam = this.familiars.get(f);
        if (!fam.canEquip(preItem)) continue;
        // Modifiers when worn by Hatrack or Scarecrow
        Modifiers familiarMods = new Modifiers();
        int familiarId = fam.getId();
        if ((familiarId == FamiliarPool.HATRACK && slot == EquipmentManager.HAT)
            || (familiarId == FamiliarPool.SCARECROW && slot == EquipmentManager.PANTS)) {
          familiarMods.applyFamiliarModifiers(fam, preItem);
        } else {
          // Normal item modifiers when used by Disembodied Hand
          familiarMods = Modifiers.getItemModifiers(id);
          if (familiarMods == null) { // no enchantments
            familiarMods = new Modifiers();
          }
        }
        if (item == null) {
          item = new CheckedItem(id, equipScope, maxPrice, priceLevel);
        }

        switch (this.checkConstraints(familiarMods)) {
          case -1:
            continue;
          case 1:
            item.automaticFlag = true;
        }

        if (item.getCount() != 0
            && (this.getScore(familiarMods) - nullScore > 0.0 || item.automaticFlag == true)) {
          ranked.get(EquipmentManager.ALL_SLOTS + f).add(item);
        }
      }

      if (!EquipmentManager.canEquip(id) && !KoLCharacter.hasEquipped(id)) continue;
      if (item == null) {
        item = new CheckedItem(id, equipScope, maxPrice, priceLevel);
      }

      if (item.getCount() == 0) {
        continue;
      }

      int auxSlot = -1;
      gotItem:
      {
        switch (slot) {
          case EquipmentManager.FAMILIAR:
            if (!famCanEquip) continue;
            break;

          case EquipmentManager.WEAPON:
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
            if (this.weaponType != null && type.indexOf(this.weaponType) == -1) {
              continue;
            }
            if (hands == 1) {
              slot = Evaluator.WEAPON_1H;
              if (type.equals("chefstaff")) { // Don't allow chefstaves to displace other
                // 1H weapons from the shortlist if you can't
                // equip them anyway.
                if (!KoLCharacter.hasSkill("Spirit of Rigatoni")
                    && !KoLCharacter.isJarlsberg()
                    && !(KoLCharacter.isSauceror() && gloveAvailable)) {
                  continue;
                }
                // In any case, don't put this in an aux slot.
              } else if (!this.requireShield && !EquipmentDatabase.isMainhandOnly(id)) {
                switch (weaponType) {
                  case MELEE:
                    auxSlot = Evaluator.OFFHAND_MELEE;
                    break;
                  case RANGED:
                    auxSlot = Evaluator.OFFHAND_RANGED;
                    break;
                  case NONE:
                  default:
                    break;
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
              if (id != ItemPool.FOURTH_SABER) {
                // Always uses best stat, so always considered effective
                if (KoLCharacter.getAdjustedMoxie() >= KoLCharacter.getAdjustedMuscle()
                    && weaponType != WeaponType.RANGED
                    && (!EquipmentDatabase.isKnife(id)
                        || !KoLCharacter.hasSkill("Tricky Knifework"))) {
                  slot = auxSlot;
                }
                if (KoLCharacter.getAdjustedMoxie() < KoLCharacter.getAdjustedMuscle()
                    && weaponType != WeaponType.MELEE) {
                  slot = auxSlot;
                }
              }
            }
            if (id == ItemPool.BROKEN_CHAMPAGNE
                && this.weight[Modifiers.ITEMDROP] > 0
                && (Preferences.getInteger("garbageChampagneCharge") > 0
                    || !Preferences.getBoolean("_garbageItemChanged"))) {
              // This is always going to be worth including if useful
              item.requiredFlag = true;
              item.automaticFlag = true;
              break gotItem;
            }
            break;

          case EquipmentManager.OFFHAND:
            if (this.requireShield && !EquipmentDatabase.isShield(id)) {
              continue;
            }
            if (hoboPowerUseful && name.startsWith("Hodgman's")) {
              Modifiers.hoboPower = 100.0;
              item.automaticFlag = true;
            }
            break;

          case EquipmentManager.ACCESSORY1:
            if (id == ItemPool.SPECIAL_SAUCE_GLOVE
                && KoLCharacter.isSauceror()
                && !KoLCharacter.hasSkill("Spirit of Rigatoni")) {
              item.validate(maxPrice, priceLevel);

              if (item.getCount() == 0) {
                continue;
              }

              item.automaticFlag = true;
              gloveAvailable = true;
              break gotItem;
            }
            break;
          case EquipmentManager.SHIRT:
            if (id == ItemPool.MAKESHIFT_GARBAGE_SHIRT
                && (this.weight[Modifiers.EXPERIENCE] > 0
                    || this.weight[Modifiers.MUS_EXPERIENCE] > 0
                    || this.weight[Modifiers.MYS_EXPERIENCE] > 0
                    || this.weight[Modifiers.MOX_EXPERIENCE] > 0)
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

        Modifiers mods = Modifiers.getItemModifiers(id);
        if (mods == null) { // no enchantments
          mods = new Modifiers();
        }

        boolean wrongClass = false;
        String classType = mods.getString(Modifiers.CLASS);
        if (classType != "" && !classType.equals(KoLCharacter.getAscensionClassName())) {
          wrongClass = true;
        }

        if (mods.getBoolean(Modifiers.SINGLE)) {
          item.singleFlag = true;
        }

        // If we are maximizing for a modifier that
        // only counts one of a particular item,
        // pretend that item is single equip
        if (this.uniques.contains(preItem)) {
          item.singleFlag = true;
        }

        // If you have a familiar carrier, we'll need to check 1 or 2 Familiars best carried
        // unless you specified not to change them

        if (((id == ItemPool.HATSEAT && this.slots[EquipmentManager.CROWNOFTHRONES] >= 0)
                || (id == ItemPool.BUDDY_BJORN && this.slots[EquipmentManager.BUDDYBJORN] >= 0))
            && !KoLCharacter.isSneakyPete()
            && !KoLCharacter.inAxecore()
            && !KoLCharacter.isJarlsberg()) {
          this.carriedFamiliarsNeeded++;
        }

        if (id == ItemPool.CARD_SLEEVE && this.slots[EquipmentManager.CARDSLEEVE] >= 0) {
          this.cardNeeded = true;
        }

        if (id == ItemPool.CROWN_OF_ED && this.slots[EquipmentManager.HAT] >= 0) {
          this.edPieceNeeded = true;
        }

        if (id == ItemPool.SNOW_SUIT && this.slots[EquipmentManager.FAMILIAR] >= 0) {
          this.snowsuitNeeded = true;
        }

        if (id == ItemPool.KNOCK_OFF_RETRO_SUPERHERO_CAPE
            && this.slots[EquipmentManager.CONTAINER] >= 0) {
          this.retroCapeNeeded = true;
        }

        if (id == ItemPool.BACKUP_CAMERA
            && (this.slots[EquipmentManager.ACCESSORY1]
                    + this.slots[EquipmentManager.ACCESSORY2]
                    + this.slots[EquipmentManager.ACCESSORY3])
                >= 0) {
          this.backupCameraNeeded = true;
        }

        if (id == ItemPool.VAMPYRIC_CLOAKE) {
          mods = new Modifiers(mods);
          mods.applyVampyricCloakeModifiers();
        }

        if (mods.getBoolean(Modifiers.NONSTACKABLE_WATCH)) {
          slot = Evaluator.WATCHES;
        }

        if (this.posEquip.contains(item)) {
          item.automaticFlag = true;
          item.requiredFlag = true;
          break gotItem;
        }

        switch (this.checkConstraints(mods)) {
          case -1:
            continue;
          case 1:
            item.automaticFlag = true;
            break gotItem;
        }

        if ((hoboPowerUseful && mods.get(Modifiers.HOBO_POWER) > 0.0)
            || (smithsnessUseful && !wrongClass && mods.get(Modifiers.SMITHSNESS) > 0.0)
            || (brimstoneUseful && mods.getRawBitmap(Modifiers.BRIMSTONE) != 0)
            || (cloathingUseful && mods.getRawBitmap(Modifiers.CLOATHING) != 0)
            || (slimeHateUseful && mods.get(Modifiers.SLIME_HATES_IT) > 0.0)
            || (this.clownosity > 0 && mods.get(Modifiers.CLOWNINESS) != 0)
            || (this.raveosity > 0 && mods.getRawBitmap(Modifiers.RAVEOSITY) != 0)
            || (this.surgeonosity > 0 && mods.get(Modifiers.SURGEONOSITY) != 0)
            || ((mods.getRawBitmap(Modifiers.SYNERGETIC) & usefulSynergies) != 0)) {
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

        if (id == ItemPool.CROWN_OF_ED) {
          if (this.edPieceDecided != null) {
            // Currently this means +sea specified, so meets that requirement
            item.automaticFlag = true;
          }
          break gotItem;
        }

        String intrinsic = mods.getString(Modifiers.INTRINSIC_EFFECT);
        if (intrinsic.length() > 0) {
          Modifiers newMods = new Modifiers();
          newMods.add(mods);
          newMods.add(Modifiers.getModifiers("Effect", intrinsic));
          mods = newMods;
        }
        double delta = this.getScore(mods, new AdventureResult[] {item}) - nullScore;
        if (delta < 0.0) continue;
        if (delta == 0.0) {
          if (KoLCharacter.hasEquipped(item) && this.current) break gotItem;
          if (item.initial == 0) continue;
          if (item.automaticFlag) continue;
        }

        if (mods.getBoolean(Modifiers.UNARMED)
            || mods.getRawBitmap(Modifiers.MUTEX)
                != 0) { // This item may turn out to be unequippable, so don't
          // count it towards the shortlist length.
          item.conditionalFlag = true;
        }
      }
      // "break gotItem" goes here
      if (slot != -1) ranked.get(slot).add(item);
      if (auxSlot != -1) ranked.get(auxSlot).add(item);
    }

    // Get best Familiars for Crown of Thrones and Buddy Bjorn
    // Assume current ones are best if in use
    FamiliarData bestCarriedFamiliar = FamiliarData.NO_FAMILIAR;
    FamiliarData secondBestCarriedFamiliar = FamiliarData.NO_FAMILIAR;
    FamiliarData useBjornFamiliar = FamiliarData.NO_FAMILIAR;
    FamiliarData useCrownFamiliar = FamiliarData.NO_FAMILIAR;

    // If we're not allowed to change the current familiar, lock it
    if (this.slots[EquipmentManager.BUDDYBJORN] < 0) {
      useBjornFamiliar = KoLCharacter.getBjorned();
    } else {
      bestCarriedFamiliar = KoLCharacter.getBjorned();
    }

    // If we're not allowed to change the current familiar, lock it
    if (this.slots[EquipmentManager.CROWNOFTHRONES] < 0) {
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
      best.equipment[EquipmentManager.HAT] = secondBest.equipment[EquipmentManager.HAT] = item;
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
      best.equipment[EquipmentManager.HAT] = secondBest.equipment[EquipmentManager.HAT] = item;
      best.setEnthroned(bestCarriedFamiliar);
      secondBest.setEnthroned(secondBestCarriedFamiliar);

      // Check each familiar in hat to see if they are worthwhile
      List<FamiliarData> familiarList = KoLCharacter.getFamiliarList();
      for (FamiliarData familiar : familiarList) {
        if (familiar != null
            && familiar != FamiliarData.NO_FAMILIAR
            && familiar.canCarry()
            && StandardRequest.isAllowed("Familiars", familiar.getRace())
            && !familiar.equals(KoLCharacter.getFamiliar())
            && !this.carriedFamiliars.contains(familiar)
            && !familiar.equals(useCrownFamiliar)
            && !familiar.equals(useBjornFamiliar)
            && !familiar.equals(bestCarriedFamiliar)
            && !(KoLCharacter.inBeecore() && KoLCharacter.getBeeosity(familiar.getRace()) > 0)) {
          MaximizerSpeculation spec = new MaximizerSpeculation();
          spec.attachment = item;
          spec.equipment[EquipmentManager.HAT] = item;
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
        AdventureResult equippedCard = EquipmentManager.getEquipment(EquipmentManager.CARDSLEEVE);
        if (card.getCount() > 0 || (equippedCard != null && c == equippedCard.getItemId())) {
          MaximizerSpeculation spec = new MaximizerSpeculation();
          CheckedItem sleeve =
              new CheckedItem(ItemPool.CARD_SLEEVE, equipScope, maxPrice, priceLevel);
          spec.attachment = sleeve;
          spec.equipment[EquipmentManager.OFFHAND] = sleeve;
          spec.equipment[EquipmentManager.CARDSLEEVE] = card;
          if (spec.compareTo(best) > 0) {
            best = spec.clone();
            bestCard = card;
          }
        }
      }
    }

    String bestEdPiece = null;

    if (this.edPieceNeeded) {
      // Is Crown of Ed forced to a particular choice ?
      if (this.edPieceDecided != null) {
        bestEdPiece = this.edPieceDecided;
      } else {
        // Assume best is current edPiece
        MaximizerSpeculation best = new MaximizerSpeculation();
        CheckedItem edPiece =
            new CheckedItem(ItemPool.CROWN_OF_ED, equipScope, maxPrice, priceLevel);
        best.attachment = edPiece;
        bestEdPiece = Preferences.getString("edPiece");
        best.equipment[EquipmentManager.HAT] = edPiece;
        best.setEdPiece(bestEdPiece);

        // Check each animal in Crown of Ed to see if they are worthwhile
        for (String[] ANIMAL : EdPieceCommand.ANIMAL) {
          String animal = ANIMAL[0];
          if (animal.equals(bestEdPiece)) {
            // Don't bother if we've already done it for best
            continue;
          }
          MaximizerSpeculation spec = new MaximizerSpeculation();
          spec.attachment = edPiece;
          spec.equipment[EquipmentManager.HAT] = edPiece;
          spec.setEdPiece(animal);
          if (spec.compareTo(best) > 0) {
            best = spec.clone();
            bestEdPiece = animal;
          }
        }
      }
    }

    String bestSnowsuit = null;

    if (this.snowsuitNeeded) {
      // Assume best is current Snowsuit
      MaximizerSpeculation best = new MaximizerSpeculation();
      CheckedItem snowsuit = new CheckedItem(ItemPool.SNOW_SUIT, equipScope, maxPrice, priceLevel);
      best.attachment = snowsuit;
      bestSnowsuit = Preferences.getString("snowsuit");
      best.equipment[EquipmentManager.FAMILIAR] = snowsuit;
      best.setSnowsuit(bestSnowsuit);

      // Check each decoration in Snowsuit to see if they are worthwhile
      for (String[] DECORATION : SnowsuitCommand.DECORATION) {
        String decoration = DECORATION[0];
        if (decoration.equals(bestSnowsuit)) {
          // Don't bother if we've already done it for best
          continue;
        }
        MaximizerSpeculation spec = new MaximizerSpeculation();
        spec.attachment = snowsuit;
        spec.equipment[EquipmentManager.FAMILIAR] = snowsuit;
        spec.setSnowsuit(decoration);
        if (spec.compareTo(best) > 0) {
          best = spec.clone();
          bestSnowsuit = decoration;
        }
      }
    }

    String bestRetroCape = null;

    if (this.retroCapeNeeded) {
      // Assume best is current retro cape
      MaximizerSpeculation best = new MaximizerSpeculation();
      CheckedItem retroCape =
          new CheckedItem(
              ItemPool.KNOCK_OFF_RETRO_SUPERHERO_CAPE, equipScope, maxPrice, priceLevel);
      best.attachment = retroCape;
      bestRetroCape =
          Preferences.getString("retroCapeSuperhero")
              + " "
              + Preferences.getString("retroCapeWashingInstructions");
      best.equipment[EquipmentManager.CONTAINER] = retroCape;
      best.setRetroCape(bestRetroCape);

      // Check each combination of cape settings to see if they are worthwhile
      for (String superhero : RetroCapeCommand.SUPERHEROS) {
        for (String washingInstruction : RetroCapeCommand.WASHING_INSTRUCTIONS) {
          String config = superhero + " " + washingInstruction;

          if (config.equals(bestRetroCape)) {
            // Don't bother if we've already done it for best
            continue;
          }

          MaximizerSpeculation spec = new MaximizerSpeculation();
          spec.attachment = retroCape;
          spec.equipment[EquipmentManager.CONTAINER] = retroCape;
          spec.setRetroCape(config);
          if (spec.compareTo(best) > 0) {
            best = spec.clone();
            bestRetroCape = config;
          }
        }
      }
    }

    String bestBackupCamera = null;

    if (this.backupCameraNeeded) {
      // Assume best is current backup camera mode
      MaximizerSpeculation best = new MaximizerSpeculation();
      CheckedItem backupCamera =
          new CheckedItem(ItemPool.BACKUP_CAMERA, equipScope, maxPrice, priceLevel);
      best.attachment = backupCamera;
      bestBackupCamera = Preferences.getString("backupCameraMode");
      best.equipment[EquipmentManager.ACCESSORY3] = backupCamera;
      best.setBackupCamera(bestBackupCamera);

      // Check each mode to see if it is worthwhile
      for (String[] MODE : BackupCameraCommand.MODE) {
        String mode = MODE[0];
        if (mode.equals(bestBackupCamera)) {
          // Don't bother if we've already done it for best
          continue;
        }

        MaximizerSpeculation spec = new MaximizerSpeculation();
        spec.attachment = backupCamera;
        spec.equipment[EquipmentManager.ACCESSORY3] = backupCamera;
        spec.setBackupCamera(mode);
        if (spec.compareTo(best) > 0) {
          best = spec.clone();
          bestBackupCamera = mode;
        }
      }
    }

    List<List<MaximizerSpeculation>> speculationList = new ArrayList<>(ranked.size());
    for (int i = 0; i < ranked.size(); ++i) {
      speculationList.add(new ArrayList<MaximizerSpeculation>());
    }

    for (int slot = 0; slot < ranked.size(); ++slot) {
      List<CheckedItem> checkedItemList = ranked.get(slot);

      // If we currently have nothing equipped, also consider leaving nothing equipped
      if (EquipmentManager.getEquipment(Evaluator.toUseSlot(slot)) == EquipmentRequest.UNEQUIP) {
        ranked.get(slot).add(new CheckedItem(-1, equipScope, maxPrice, priceLevel));
      }

      for (CheckedItem item : checkedItemList) {
        MaximizerSpeculation spec = new MaximizerSpeculation();
        spec.attachment = item;
        int useSlot = Evaluator.toUseSlot(slot);
        if (slot >= EquipmentManager.ALL_SLOTS) {
          spec.setFamiliar(this.familiars.get(slot - EquipmentManager.ALL_SLOTS));
          useSlot = EquipmentManager.FAMILIAR;
        }
        spec.equipment[useSlot] = item;
        int itemId = item.getItemId();
        if (itemId == ItemPool.HATSEAT) {
          if (this.slots[EquipmentManager.CROWNOFTHRONES] < 0) {
            spec.setEnthroned(useCrownFamiliar);
          } else if (this.carriedFamiliarsNeeded > 1) {
            item.automaticFlag = true;
            spec.setEnthroned(secondBestCarriedFamiliar);
          } else {
            spec.setEnthroned(bestCarriedFamiliar);
          }
        } else if (itemId == ItemPool.BUDDY_BJORN) {
          if (this.slots[EquipmentManager.BUDDYBJORN] < 0) {
            spec.setBjorned(useBjornFamiliar);
          } else if (this.carriedFamiliarsNeeded > 1) {
            item.automaticFlag = true;
            spec.setBjorned(secondBestCarriedFamiliar);
          } else {
            spec.setBjorned(bestCarriedFamiliar);
          }
        } else if (EquipmentManager.isStickerWeapon(item)) {
          MaximizerSpeculation current = new MaximizerSpeculation();
          spec.equipment[EquipmentManager.STICKER1] = current.equipment[EquipmentManager.STICKER1];
          spec.equipment[EquipmentManager.STICKER2] = current.equipment[EquipmentManager.STICKER2];
          spec.equipment[EquipmentManager.STICKER3] = current.equipment[EquipmentManager.STICKER3];
        } else if (itemId == ItemPool.CARD_SLEEVE) {
          MaximizerSpeculation current = new MaximizerSpeculation();
          if (bestCard != null) {
            spec.equipment[EquipmentManager.CARDSLEEVE] = bestCard;
            useCard = bestCard;
          } else {
            spec.equipment[EquipmentManager.CARDSLEEVE] =
                current.equipment[EquipmentManager.CARDSLEEVE];
            useCard = current.equipment[EquipmentManager.CARDSLEEVE];
          }
        } else if (itemId == ItemPool.FOLDER_HOLDER) {
          MaximizerSpeculation current = new MaximizerSpeculation();
          spec.equipment[EquipmentManager.FOLDER1] = current.equipment[EquipmentManager.FOLDER1];
          spec.equipment[EquipmentManager.FOLDER2] = current.equipment[EquipmentManager.FOLDER2];
          spec.equipment[EquipmentManager.FOLDER3] = current.equipment[EquipmentManager.FOLDER3];
          spec.equipment[EquipmentManager.FOLDER4] = current.equipment[EquipmentManager.FOLDER4];
          spec.equipment[EquipmentManager.FOLDER5] = current.equipment[EquipmentManager.FOLDER5];
        } else if (itemId == ItemPool.CROWN_OF_ED) {
          if (bestEdPiece != null) {
            spec.setEdPiece(bestEdPiece);
          }
        } else if (itemId == ItemPool.SNOW_SUIT) {
          if (bestSnowsuit != null) {
            spec.setSnowsuit(bestSnowsuit);
          }
        } else if (itemId == ItemPool.KNOCK_OFF_RETRO_SUPERHERO_CAPE) {
          if (bestRetroCape != null) {
            spec.setRetroCape(bestRetroCape);
          }
        } else if (itemId == ItemPool.BACKUP_CAMERA) {
          if (bestBackupCamera != null) {
            spec.setBackupCamera(bestBackupCamera);
          }
        } else if (itemId == ItemPool.COWBOY_BOOTS) {
          MaximizerSpeculation current = new MaximizerSpeculation();
          spec.equipment[EquipmentManager.BOOTSKIN] = current.equipment[EquipmentManager.BOOTSKIN];
          spec.equipment[EquipmentManager.BOOTSPUR] = current.equipment[EquipmentManager.BOOTSPUR];
        }
        spec.getScore(); // force evaluation
        spec.failed = false; // individual items are not expected
        // to fulfill all requirements

        speculationList.get(slot).add(spec);
      }

      Collections.sort(speculationList.get(slot));
    }

    // Compare sets which improve with the number of items equipped with the best items in the same
    // spots

    // Compare synergies with best items in the same spots, and remove automatic flag if not better
    for (Entry<String, Integer> entry : Modifiers.getSynergies()) {
      String synergy = entry.getKey();
      int mask = entry.getValue().intValue();
      int index = synergy.indexOf("/");
      String itemName1 = synergy.substring(0, index);
      String itemName2 = synergy.substring(index + 1);
      int itemId1 = ItemDatabase.getItemId(itemName1);
      int itemId2 = ItemDatabase.getItemId(itemName2);
      int slot1 = EquipmentManager.itemIdToEquipmentType(itemId1);
      int slot2 = EquipmentManager.itemIdToEquipmentType(itemId2);
      CheckedItem item1 = null;
      CheckedItem item2 = null;

      // The only times the slots will be wrong for looking at speculation lists for current
      // synergies are 1 handed swords
      // They are always item 1
      int hands = EquipmentDatabase.getHands(itemId1);
      WeaponType weaponType = EquipmentDatabase.getWeaponType(itemId1);
      int slot1SpecLookup = slot1;
      if (hands == 1 && weaponType == WeaponType.MELEE) {
        slot1SpecLookup = Evaluator.WEAPON_1H;
      }

      if (slot1 == -1 || slot2 == -1) {
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

      int newSlot1 = slot1;
      int compareItemNo =
          slot1 == EquipmentManager.ACCESSORY1
              ? speculationList.get(slot1SpecLookup).size() - 3
              : speculationList.get(slot1SpecLookup).size() - 1;
      do {
        CheckedItem compareItem =
            speculationList.get(slot1SpecLookup).get(compareItemNo).attachment;
        if (compareItem.conditionalFlag) {
          compareItemNo--;
        } else {
          compareSpec.equipment[newSlot1] =
              speculationList.get(slot1SpecLookup).get(compareItemNo).attachment;
          break;
        }
        if (compareItemNo < 0) {
          compareSpec.equipment[newSlot1] = EquipmentRequest.UNEQUIP;
          break;
        }
      } while (compareItemNo >= 0);
      if (slot1 == EquipmentManager.ACCESSORY1) {
        accCompared++;
      }
      synergySpec.equipment[newSlot1] = item1;

      int newSlot2 = slot2 + (slot2 == EquipmentManager.ACCESSORY1 ? accCompared : 0);
      compareItemNo =
          slot2 == EquipmentManager.ACCESSORY1
              ? speculationList.get(slot2).size() - 2
              : speculationList.get(slot2).size() - 1;
      do {
        CheckedItem compareItem = speculationList.get(slot2).get(compareItemNo).attachment;
        if (compareItem.conditionalFlag
            || compareItem.getName().equals(compareSpec.equipment[newSlot1].getName())) {
          compareItemNo--;
        } else {
          compareSpec.equipment[newSlot2] =
              speculationList.get(slot2).get(compareItemNo).attachment;
          break;
        }
        if (compareItemNo < 0) {
          compareSpec.equipment[newSlot2] = EquipmentRequest.UNEQUIP;
          break;
        }
      } while (compareItemNo >= 0);
      synergySpec.equipment[newSlot2] = item2;

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
      int itemId1 = -1;
      int itemId2 = -1;
      int itemId3 = -1;
      CheckedItem item1 = null;
      CheckedItem item2 = null;
      CheckedItem item3 = null;
      int slot = EquipmentManager.ACCESSORY1;

      if (count == 0) {
        itemId1 = ItemPool.MONSTROUS_MONOCLE;
        itemId2 = ItemPool.MUSTY_MOCCASINS;
        itemId3 = ItemPool.MOLTEN_MEDALLION;
        count++;
      } else {
        itemId1 = ItemPool.BRAZEN_BRACELET;
        itemId2 = ItemPool.BITTER_BOWTIE;
        itemId3 = ItemPool.BEWITCHING_BOOTS;
        count++;
      }

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
      compareSpec.equipment[slot] = EquipmentRequest.UNEQUIP;
      compareSpec.equipment[slot + 1] = EquipmentRequest.UNEQUIP;
      compareSpec.equipment[slot + 2] = EquipmentRequest.UNEQUIP;
      int newSlot = slot;
      do {
        CheckedItem compareItem = speculationList.get(slot).get(compareItemNo).attachment;
        if (!compareItem.conditionalFlag) {
          compareSpec.equipment[newSlot] = speculationList.get(slot).get(compareItemNo).attachment;
          newSlot++;
        }
        compareItemNo--;
      } while (compareItemNo >= 0 && newSlot < slot + 3);
      synergySpec.equipment[slot] = item1;
      synergySpec.equipment[slot + 1] = item2;
      synergySpec.equipment[slot + 2] = item3;

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
          int slot = EquipmentManager.itemIdToEquipmentType(outfitItemId);
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
          int newSlot = slot + (slot == EquipmentManager.ACCESSORY1 ? accCount : 0);
          int compareItemNo = speculationList.get(slot).size() - 1;
          int accSkip = slot == EquipmentManager.ACCESSORY1 ? 2 - accCount : 0;
          while (compareItemNo >= 0) {
            CheckedItem compareItem = speculationList.get(slot).get(compareItemNo).attachment;
            if (compareItem.conditionalFlag) {
              compareItemNo--;
            } else if (accSkip > 0) {
              // Valid item, but we're looking for 2nd or 3rd best non-conditional
              compareItemNo--;
              accSkip--;
            } else {
              compareSpec.equipment[newSlot] = compareItem;
              break;
            }
            if (compareItemNo < 0) {
              compareSpec.equipment[newSlot] = EquipmentRequest.UNEQUIP;
              break;
            }
          }
          CheckedItem outfitItem = new CheckedItem(outfitItemId, equipScope, maxPrice, priceLevel);
          outfitSpec.equipment[newSlot] = outfitItem;
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

    for (int slot = 0; slot < ranked.size(); ++slot) {
      List<CheckedItem> checkedItemList = ranked.get(slot);

      if (this.dump > 0) {
        RequestLogger.printLine("SLOT " + slot);
      }

      if (this.dump > 1) {
        RequestLogger.printLine(speculationList.get(slot).toString());
      }

      // Do we have any required items for the slot?
      int total = 0;
      for (CheckedItem item : checkedItemList) {
        if (item.requiredFlag) {
          automatic.get(slot).add(item);
          // Don't increase total if it's one of the required flagged foldables by Evaluator rather
          // than user
          int itemId = item.getItemId();
          if (itemId != ItemPool.BROKEN_CHAMPAGNE && itemId != ItemPool.MAKESHIFT_GARBAGE_SHIRT) {
            ++total;
          }
        }
      }

      int useful = this.maxUseful(slot);

      // If slots already handled by required items, we're done with the slot
      if (useful > total) {
        ListIterator<MaximizerSpeculation> speculationIterator =
            speculationList.get(slot).listIterator(speculationList.get(slot).size());

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
            foldItemsNeeded += Math.max(item.getCount(), this.maxUseful(slot));
            // How many times have we already used this fold item?
            for (int checkSlot = 0; checkSlot < slot; ++checkSlot) {
              List<CheckedItem> checkItemList = automatic.get(checkSlot);
              if (checkItemList != null) {
                for (CheckedItem checkItem : checkItemList) {
                  FoldGroup checkGroup = ItemDatabase.getFoldGroup(checkItem.getName());
                  if (checkGroup != null && group.names.get(0).equals(checkGroup.names.get(0))) {
                    foldItemsNeeded += Math.max(checkItem.getCount(), this.maxUseful(checkSlot));
                  }
                }
              }
            }
            // And how many times do we expect to use them for the rest of the slots?
            for (int checkSlot = slot + 1; checkSlot < ranked.size(); checkSlot++) {
              ListIterator<MaximizerSpeculation> checkIterator =
                  speculationList
                      .get(checkSlot)
                      .listIterator(speculationList.get(checkSlot).size());
              int usefulCheckCount = this.maxUseful(checkSlot);
              while (checkIterator.hasPrevious()) {
                CheckedItem checkItem = checkIterator.previous().attachment;
                FoldGroup checkGroup = ItemDatabase.getFoldGroup(checkItem.getName());
                if (checkGroup != null && group.names.get(0).equals(checkGroup.names.get(0))) {
                  if (usefulCheckCount > 0 || checkItem.requiredFlag) {
                    foldItemsNeeded += Math.max(checkItem.getCount(), this.maxUseful(checkSlot));
                  }
                } else if (checkItem.automaticFlag || !checkItem.conditionalFlag) {
                  usefulCheckCount--;
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
              if (!automatic.get(slot).contains(item)) {
                automatic.get(slot).add(item);
              }
              beeotches += item.getCount();
              beeosity += b * item.getCount();
            } else if (total < useful && beeotches < useful && beeosity < this.beeosity) {
              if (!automatic.get(slot).contains(item)) {
                automatic.get(slot).add(item);
              }
              beeotches += item.getCount();
              beeosity += b * item.getCount();
            }
          } else if (item.automaticFlag) {
            if (!automatic.get(slot).contains(item)) {
              automatic.get(slot).add(item);
              if (!item.conditionalFlag && item.getCount() >= foldItemsNeeded) {
                total += item.getCount();
              }
            }
          } else if (total < useful) {
            if (!automatic.get(slot).contains(item)) {
              automatic.get(slot).add(item);
              if (!item.conditionalFlag && item.getCount() >= foldItemsNeeded) {
                total += item.getCount();
              }
            }
          }
        }
      }

      // Blunt object fix for only having a foldable that might be needed elsewhere
      if (automatic.get(slot).size() == 1
          && ItemDatabase.getFoldGroup(automatic.get(slot).get(0).getName()) != null) {
        automatic.get(slot).add(new CheckedItem(-1, equipScope, maxPrice, priceLevel));
      }

      if (this.dump > 0) {
        RequestLogger.printLine(automatic.get(slot).toString());
      }
    }

    automatic.get(EquipmentManager.ACCESSORY1).addAll(automatic.get(Evaluator.WATCHES));
    automatic.get(EquipmentManager.WEAPON).addAll(automatic.get(Evaluator.WEAPON_1H));
    automatic.get(Evaluator.OFFHAND_MELEE).addAll(automatic.get(EquipmentManager.OFFHAND));
    automatic.get(Evaluator.OFFHAND_RANGED).addAll(automatic.get(EquipmentManager.OFFHAND));

    MaximizerSpeculation spec = new MaximizerSpeculation();
    // The threshold in the slots array that indicates that a slot
    // should be considered will be either >= 1 or >= 0, depending
    // on whether inclusive or exclusive slot specs were used.
    for (int thresh = 1; ; --thresh) {
      if (thresh < 0) return; // no slots enabled
      boolean anySlots = false;
      for (int i = 0; i <= EquipmentManager.FAMILIAR; ++i) {
        if (this.slots[i] >= thresh) {
          spec.equipment[i] = null;
          anySlots = true;
        }
      }
      if (anySlots) break;
    }

    if (spec.equipment[EquipmentManager.OFFHAND] != null) {
      this.hands = 1;
      automatic.set(EquipmentManager.WEAPON, automatic.get(Evaluator.WEAPON_1H));

      Iterator<AdventureResult> i = outfitPieces.keySet().iterator();
      while (i.hasNext()) {
        id = i.next().getItemId();
        if (EquipmentManager.itemIdToEquipmentType(id) == EquipmentManager.WEAPON
            && EquipmentDatabase.getHands(id) > 1) {
          i.remove();
        }
      }
    }

    if (spec.equipment[EquipmentManager.HAT] == null) {
      spec.setEdPiece(bestEdPiece);
    }

    if (spec.equipment[EquipmentManager.CONTAINER] == null) {
      spec.setRetroCape(bestRetroCape);
    }

    if (spec.equipment[EquipmentManager.ACCESSORY3] == null) {
      spec.setBackupCamera(bestBackupCamera);
    }

    if (spec.equipment[EquipmentManager.FAMILIAR] == null) {
      spec.setSnowsuit(bestSnowsuit);
    }

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
}
