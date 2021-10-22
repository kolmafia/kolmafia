package net.sourceforge.kolmafia.persistence;

import java.io.BufferedReader;
import java.io.File;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLAdventure;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLConstants.Stat;
import net.sourceforge.kolmafia.KoLConstants.WeaponType;
import net.sourceforge.kolmafia.Modifiers;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.SpecialOutfit;
import net.sourceforge.kolmafia.StaticEntity;
import net.sourceforge.kolmafia.objectpool.AdventurePool;
import net.sourceforge.kolmafia.objectpool.EffectPool;
import net.sourceforge.kolmafia.objectpool.IntegerPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.objectpool.OutfitPool;
import net.sourceforge.kolmafia.session.EquipmentManager;
import net.sourceforge.kolmafia.utilities.FileUtilities;
import net.sourceforge.kolmafia.utilities.IntegerArray;
import net.sourceforge.kolmafia.utilities.LogStream;
import net.sourceforge.kolmafia.utilities.StringArray;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class EquipmentDatabase {
  private static final IntegerArray power = new IntegerArray();
  private static final IntegerArray hands = new IntegerArray();
  private static final StringArray itemTypes = new StringArray();
  private static final StringArray statRequirements = new StringArray();

  private static final HashMap<Integer, Integer> outfitPieces = new HashMap<Integer, Integer>();
  public static final SpecialOutfitArray normalOutfits = new SpecialOutfitArray();
  private static final Map<Integer, String> outfitById = new TreeMap<Integer, String>();
  public static final SpecialOutfitArray weirdOutfits = new SpecialOutfitArray();

  private static final IntegerArray pulverize = new IntegerArray();
  // Values in pulverize are one of:
  //	0 - not initialized yet
  //	positive - ID of special-case pulverize result (worthless powder, epic wad, etc.)
  //	-1 - not pulverizable (quest item, Mr. Store item, etc.)
  //	other negative - bitmap, some combination of PULVERIZE_BITS, YIELD_x, ELEM_x
  public static final int PULVERIZE_BITS = 0x80000000; // makes value negative
  public static final int YIELD_UNCERTAIN = 0x001;
  public static final int YIELD_1P = 0x002;
  public static final int YIELD_2P = 0x004;
  public static final int YIELD_3P = 0x008;
  public static final int YIELD_4P_1N = 0x010;
  public static final int YIELD_1N3P_2N = 0x020;
  public static final int YIELD_3N = 0x040;
  public static final int YIELD_4N_1W = 0x080;
  public static final int YIELD_1W3N_2W = 0x100;
  public static final int YIELD_3W = 0x200;
  public static final int YIELD_1C = 0x400;
  public static final int MASK_YIELD = 0x7FF;
  public static final int ELEM_TWINKLY = 0x01000;
  public static final int ELEM_HOT = 0x02000;
  public static final int ELEM_COLD = 0x04000;
  public static final int ELEM_STENCH = 0x08000;
  public static final int ELEM_SPOOKY = 0x10000;
  public static final int ELEM_SLEAZE = 0x20000;
  public static final int ELEM_OTHER = 0x40000;
  public static final int MASK_ELEMENT = 0x7F000;
  public static final int MALUS_UPGRADE = 0x100000;

  public static final int[] IMPLICATIONS = {
    Modifiers.COLD_RESISTANCE, ELEM_HOT | ELEM_SPOOKY,
    Modifiers.HOT_RESISTANCE, ELEM_STENCH | ELEM_SLEAZE,
    Modifiers.SLEAZE_RESISTANCE, ELEM_COLD | ELEM_SPOOKY,
    Modifiers.SPOOKY_RESISTANCE, ELEM_HOT | ELEM_STENCH,
    Modifiers.STENCH_RESISTANCE, ELEM_COLD | ELEM_SLEAZE,
    Modifiers.COLD_DAMAGE, ELEM_COLD,
    Modifiers.HOT_DAMAGE, ELEM_HOT,
    Modifiers.SLEAZE_DAMAGE, ELEM_SLEAZE,
    Modifiers.SPOOKY_DAMAGE, ELEM_SPOOKY,
    Modifiers.STENCH_DAMAGE, ELEM_STENCH,
    Modifiers.COLD_SPELL_DAMAGE, ELEM_COLD,
    Modifiers.HOT_SPELL_DAMAGE, ELEM_HOT,
    Modifiers.SLEAZE_SPELL_DAMAGE, ELEM_SLEAZE,
    Modifiers.SPOOKY_SPELL_DAMAGE, ELEM_SPOOKY,
    Modifiers.STENCH_SPELL_DAMAGE, ELEM_STENCH,
  };

  public static boolean newEquipment = false;

  static {
    EquipmentDatabase.reset();
  }

  public static void reset() {
    EquipmentDatabase.newEquipment = false;

    BufferedReader reader =
        FileUtilities.getVersionedReader("equipment.txt", KoLConstants.EQUIPMENT_VERSION);

    String[] data;
    int itemId;

    while ((data = FileUtilities.readData(reader)) != null) {
      if (data.length < 3) {
        continue;
      }

      itemId = ItemDatabase.getItemId(data[0]);
      if (itemId < 0) {
        continue;
      }

      EquipmentDatabase.power.set(itemId, StringUtilities.parseInt(data[1]));

      String reqs = data[2];
      EquipmentDatabase.statRequirements.set(itemId, reqs);

      int hval = 0;
      String tval = null;

      if (data.length >= 4) {
        String str = data[3];
        int index1 = str.indexOf("-handed");
        if (index1 > 0) {
          hval = StringUtilities.parseInt(str.substring(0, index1));
          String type = str.substring(index1 + 7).trim();
          tval = type.equals("") ? "weapon" : type;
        } else {
          tval = str;
        }
      }

      EquipmentDatabase.hands.set(itemId, hval);
      EquipmentDatabase.itemTypes.set(itemId, tval);
    }

    try {
      reader.close();
    } catch (Exception e) {
      // This should not happen.  Therefore, print
      // a stack trace for debug purposes.

      StaticEntity.printStackTrace(e);
    }

    reader = FileUtilities.getVersionedReader("outfits.txt", KoLConstants.OUTFITS_VERSION);

    int outfitId, arrayIndex;
    SpecialOutfitArray outfitList;

    while ((data = FileUtilities.readData(reader)) != null) {
      if (data.length >= 4) {
        outfitId = StringUtilities.parseInt(data[0]);

        if (outfitId == 0) {
          arrayIndex = EquipmentDatabase.weirdOutfits.size();
          outfitList = EquipmentDatabase.weirdOutfits;
        } else {
          arrayIndex = outfitId;
          outfitList = EquipmentDatabase.normalOutfits;
        }

        String name = data[1];
        SpecialOutfit outfit = new SpecialOutfit(outfitId, name);
        outfitList.set(arrayIndex, outfit);

        String image = data[2];
        outfit.setImage(image);

        String[] pieces = data[3].split("\\s*,\\s*");

        if (data.length >= 5) {
          String[] treats = data[4].split("\\s*,\\s*");
          for (String treat : treats) {
            if (treat.equals("none")) {
              break;
            }

            int treatId = ItemDatabase.getItemId(treat);
            if (treatId != -1) {
              outfit.addTreat(ItemPool.get(treatId));
            } else {
              RequestLogger.printLine(
                  "Outfit \"" + name + "\" has an invalid treat: \"" + treat + "\"");
            }
          }
        }

        Integer id = IntegerPool.get(outfitId);

        EquipmentDatabase.outfitById.put(id, name);

        for (String piece : pieces) {
          int pieceId = ItemDatabase.getItemId(piece);
          if (pieceId != -1) {
            EquipmentDatabase.outfitPieces.put(IntegerPool.get(pieceId), id);
            outfit.addPiece(ItemPool.get(pieceId));
          }
        }
      }
    }

    try {
      reader.close();
    } catch (Exception e) {
      // This should not happen.  Therefore, print
      // a stack trace for debug purposes.

      StaticEntity.printStackTrace(e);
    }

    reader = FileUtilities.getVersionedReader("pulverize.txt", KoLConstants.PULVERIZE_VERSION);

    while ((data = FileUtilities.readData(reader)) != null) {
      if (data.length < 2) {
        continue;
      }

      itemId = ItemDatabase.getItemId(data[0]);
      if (itemId < 0) {
        continue;
      }

      String spec = data[1];
      int result =
          spec.equals("nosmash")
              ? -1
              : spec.equals("upgrade")
                  ? EquipmentDatabase.deriveUpgrade(data[0])
                  : StringUtilities.isNumeric(spec)
                      ? (PULVERIZE_BITS | StringUtilities.parseInt(spec))
                      : spec.endsWith("cluster")
                          ? EquipmentDatabase.deriveCluster(spec)
                          : ItemDatabase.getItemId(spec);

      EquipmentDatabase.pulverize.set(itemId, result);
    }

    try {
      reader.close();
    } catch (Exception e) {
      // This should not happen.  Therefore, print
      // a stack trace for debug purposes.

      StaticEntity.printStackTrace(e);
    }
  }

  public static boolean isEquipment(final int type) {
    switch (type) {
      case KoLConstants.EQUIP_ACCESSORY:
      case KoLConstants.EQUIP_CONTAINER:
      case KoLConstants.EQUIP_HAT:
      case KoLConstants.EQUIP_SHIRT:
      case KoLConstants.EQUIP_PANTS:
      case KoLConstants.EQUIP_WEAPON:
      case KoLConstants.EQUIP_OFFHAND:
        return true;
    }

    return false;
  }

  public static void writeEquipment(final File output) {
    RequestLogger.printLine("Writing data override: " + output);

    // One map per equipment category
    Map<String, Integer> hats = new TreeMap<String, Integer>();
    Map<String, Integer> weapons = new TreeMap<String, Integer>();
    Map<String, Integer> offhands = new TreeMap<String, Integer>();
    Map<String, Integer> shirts = new TreeMap<String, Integer>();
    Map<String, Integer> pants = new TreeMap<String, Integer>();
    Map<String, Integer> accessories = new TreeMap<String, Integer>();
    Map<String, Integer> containers = new TreeMap<String, Integer>();

    // Iterate over all items and assign item id to category
    Iterator<Entry<Integer, String>> it = ItemDatabase.dataNameEntrySet().iterator();
    while (it.hasNext()) {
      Entry<Integer, String> entry = it.next();
      Integer key = entry.getKey();
      String name = entry.getValue();
      int type = ItemDatabase.getConsumptionType(key.intValue());

      switch (type) {
        case KoLConstants.EQUIP_HAT:
          hats.put(name, key);
          break;
        case KoLConstants.EQUIP_PANTS:
          pants.put(name, key);
          break;
        case KoLConstants.EQUIP_SHIRT:
          shirts.put(name, key);
          break;
        case KoLConstants.EQUIP_WEAPON:
          weapons.put(name, key);
          break;
        case KoLConstants.EQUIP_OFFHAND:
          offhands.put(name, key);
          break;
        case KoLConstants.EQUIP_ACCESSORY:
          accessories.put(name, key);
          break;
        case KoLConstants.EQUIP_CONTAINER:
          containers.put(name, key);
          break;
      }
    }

    // Open the output file
    PrintStream writer = LogStream.openStream(output, true);
    writer.println(KoLConstants.EQUIPMENT_VERSION);

    // For each equipment category, write the map entries
    EquipmentDatabase.writeEquipmentCategory(writer, hats, "Hats");
    writer.println();
    EquipmentDatabase.writeEquipmentCategory(writer, pants, "Pants");
    writer.println();
    EquipmentDatabase.writeEquipmentCategory(writer, shirts, "Shirts");
    writer.println();
    EquipmentDatabase.writeEquipmentCategory(writer, weapons, "Weapons");
    writer.println();
    EquipmentDatabase.writeEquipmentCategory(writer, offhands, "Off-hand");
    writer.println();
    EquipmentDatabase.writeEquipmentCategory(writer, accessories, "Accessories");
    writer.println();
    EquipmentDatabase.writeEquipmentCategory(writer, containers, "Containers");

    writer.close();
  }

  private static void writeEquipmentCategory(
      final PrintStream writer, final Map<String, Integer> map, final String tag) {
    writer.println("# " + tag + " section of equipment.txt");
    writer.println();

    String[] keys = map.keySet().toArray(new String[0]);
    for (int i = 0; i < keys.length; ++i) {
      String name = keys[i];
      Integer val = map.get(name);
      int itemId = val.intValue();
      int power = EquipmentDatabase.getPower(itemId);
      String req = EquipmentDatabase.getEquipRequirement(itemId);
      int usage = ItemDatabase.getConsumptionType(itemId);
      boolean isWeapon = usage == KoLConstants.EQUIP_WEAPON;
      String type = EquipmentDatabase.itemTypes.get(itemId);
      boolean isShield = type != null && type.equals("shield");
      String weaponType = "";
      if (isWeapon) {
        int hands = EquipmentDatabase.hands.get(itemId);
        weaponType = hands + "-handed " + type;
      }
      EquipmentDatabase.writeEquipmentItem(
          writer, name, power, req, weaponType, isWeapon, isShield);
    }
  }

  public static void writeEquipmentItem(
      final PrintStream writer,
      final String name,
      final int power,
      final String req,
      final String weaponType,
      final boolean isWeapon,
      final boolean isShield) {
    if (isShield && power == 0) {
      writer.println("# *** " + name + " is a shield of unknown power.");
    }
    writer.println(
        EquipmentDatabase.equipmentString(name, power, req, weaponType, isWeapon, isShield));
  }

  public static String equipmentString(
      final String name,
      final int power,
      final String req,
      final String weaponType,
      final boolean isWeapon,
      final boolean isShield) {
    if (isWeapon) {
      return name + "\t" + power + "\t" + req + "\t" + weaponType;
    } else if (isShield) {
      return name + "\t" + power + "\t" + req + "\tshield";
    } else {
      return name + "\t" + power + "\t" + req;
    }
  }

  private static final Pattern WEAPON_TYPE_PATTERN = Pattern.compile("\\(((\\d)-handed (.*?))\\)");

  public static final void registerItem(
      final int itemId, final String itemName, final String text) {
    int power = DebugDatabase.parsePower(text);
    EquipmentDatabase.registerItem(itemId, itemName, text, power);
  }

  public static final void registerItem(
      final int itemId, final String itemName, final String text, final int power) {
    // A new item has been detected. Examine the item description
    // and decide what it is.
    String type = DebugDatabase.parseType(text);
    String req = DebugDatabase.parseReq(text, type);

    EquipmentDatabase.power.set(itemId, power);
    EquipmentDatabase.statRequirements.set(itemId, req);

    boolean isWeapon = false, isShield = false;
    String weaponType = "";

    if (type.indexOf("weapon") != -1) {
      Matcher matcher = WEAPON_TYPE_PATTERN.matcher(type);
      int hval;
      String tval;
      if (matcher.find()) {
        weaponType = matcher.group(1);
        hval = StringUtilities.parseInt(matcher.group(2));
        tval = matcher.group(3);
      } else {
        weaponType = type;
        hval = 0;
        tval = type;
      }
      EquipmentDatabase.hands.set(itemId, hval);
      EquipmentDatabase.itemTypes.set(itemId, tval);
      isWeapon = true;
    } else if (type.indexOf("shield") != -1) {
      isShield = true;
      weaponType = "shield";
      EquipmentDatabase.itemTypes.set(itemId, weaponType);
    }

    String printMe =
        EquipmentDatabase.equipmentString(itemName, power, req, weaponType, isWeapon, isShield);
    RequestLogger.printLine(printMe);
    RequestLogger.updateSessionLog(printMe);
  }

  // <tr><td valign=top>Outfit:&nbsp;</td><td valign=top><b><span style='cursor: hand;
  // cursor:pointer;'
  // onClick='javascript:window.open("desc_outfit.php?whichoutfit=112","","height=200,width=300")'>Topiaria</span></b><br>(3&nbsp;items)</td></tr>

  private static final Pattern OUTFIT_PATTERN =
      Pattern.compile("Outfit:.*?<span.*?whichoutfit=(\\d+).*?>(.*?)</span>");

  public static final void registerItemOutfit(final int itemId, final String text) {
    Matcher matcher = OUTFIT_PATTERN.matcher(text);
    if (!matcher.find()) {
      return;
    }

    int outfitId = StringUtilities.parseInt(matcher.group(1));
    String outfitName = matcher.group(2);

    // If this is either a new outfit or this piece is not known to
    // be on an outfit, register it.

    if (EquipmentDatabase.normalOutfits.get(outfitId) == null
        || EquipmentDatabase.outfitPieces.get(IntegerPool.get(itemId)) == null) {
      EquipmentDatabase.registerOutfit(outfitId, outfitName, itemId);
    }
  }

  public static final void registerOutfit(
      final int outfitId, final String outfitName, final int itemId) {
    SpecialOutfit outfit = EquipmentDatabase.normalOutfits.get(outfitId);
    Integer id = IntegerPool.get(outfitId);

    if (outfit == null) {
      outfit = new SpecialOutfit(outfitId, outfitName);
      EquipmentDatabase.normalOutfits.set(outfitId, outfit);
      EquipmentDatabase.outfitById.put(id, outfitName);
    }

    if (itemId != -1) {
      EquipmentDatabase.outfitPieces.put(IntegerPool.get(itemId), id);
      outfit.addPiece(ItemPool.get(itemId));
    }

    String rawText = DebugDatabase.rawOutfitDescriptionText(outfitId);
    String outfitImage = rawText != null ? DebugDatabase.parseImage(rawText) : "";
    outfit.setImage(outfitImage);

    String printMe;

    printMe = "--------------------";
    RequestLogger.printLine(printMe);
    RequestLogger.updateSessionLog(printMe);

    printMe = EquipmentDatabase.outfitString(outfitId, outfitName, outfitImage);
    RequestLogger.printLine(printMe);
    RequestLogger.updateSessionLog(printMe);

    // Let modifiers database do what it wishes with this outfit
    Modifiers.registerOutfit(outfitName, rawText);

    // Done generating data
    printMe = "--------------------";
    RequestLogger.printLine(printMe);
    RequestLogger.updateSessionLog(printMe);
  }

  public static final int nextEquipmentItemId(int prevId) {
    int limit = ItemDatabase.maxItemId();
    while (++prevId <= limit) {
      String req = EquipmentDatabase.statRequirements.get(prevId);
      if ((req != null && req.length() > 0)
          || ItemDatabase.getConsumptionType(prevId) == KoLConstants.EQUIP_FAMILIAR
          || ItemDatabase.getConsumptionType(prevId) == KoLConstants.CONSUME_SIXGUN) {
        return prevId;
      }
    }
    return -1;
  }

  public static final int getOutfitWithItem(final int itemId) {
    if (itemId < 0) {
      return -1;
    }

    Integer result = EquipmentDatabase.outfitPieces.get(IntegerPool.get(itemId));
    return result == null ? -1 : result.intValue();
  }

  public static final int getOutfitCount() {
    return EquipmentDatabase.normalOutfits.size();
  }

  public static final String outfitString(
      final int outfitId, final String name, final String outfitImage) {
    StringBuilder buffer = new StringBuilder();

    buffer.append(outfitId);
    buffer.append("\t");
    buffer.append(name);
    buffer.append("\t");
    buffer.append(outfitImage);

    SpecialOutfit outfit = EquipmentDatabase.getOutfit(outfitId);
    if (outfit != null) {
      AdventureResult[] pieces = outfit.getPieces();
      boolean first = true;
      for (AdventureResult piece : pieces) {
        buffer.append(first ? "\t" : ", ");
        first = false;
        buffer.append(piece.getName());
      }
    }

    return buffer.toString();
  }

  public static final boolean contains(final int itemId) {
    return itemId > 0 && EquipmentDatabase.statRequirements.get(itemId) != null;
  }

  public static final int getPower(final int itemId) {
    return EquipmentDatabase.power.get(itemId);
  }

  public static final void setPower(final int itemId, final int power) {
    EquipmentDatabase.power.set(itemId, power);
  }

  public static final int getHands(final int itemId) {
    return EquipmentDatabase.hands.get(itemId);
  }

  public static final String getEquipRequirement(final int itemId) {
    String req = EquipmentDatabase.statRequirements.get(itemId);

    if (req != null) {
      return req;
    }

    return "none";
  }

  public static final String getItemType(final int itemId) {
    switch (ItemDatabase.getConsumptionType(itemId)) {
      case KoLConstants.CONSUME_EAT:
        return "food";
      case KoLConstants.CONSUME_DRINK:
        return "booze";
      case KoLConstants.CONSUME_SPLEEN:
        return "spleen item";
      case KoLConstants.CONSUME_FOOD_HELPER:
        return "food helper";
      case KoLConstants.CONSUME_DRINK_HELPER:
        return "drink helper";
      case KoLConstants.CONSUME_STICKER:
        return "sticker";
      case KoLConstants.CONSUME_CARD:
        return "card";
      case KoLConstants.CONSUME_FOLDER:
        return "folder";
      case KoLConstants.CONSUME_BOOTSKIN:
        return "bootskin";
      case KoLConstants.CONSUME_BOOTSPUR:
        return "bootspur";
      case KoLConstants.CONSUME_SIXGUN:
        return "sixgun";
      case KoLConstants.CONSUME_POTION:
        return "potion";
      case KoLConstants.CONSUME_AVATAR:
        return "avatar potion";
      case KoLConstants.GROW_FAMILIAR:
        return "familiar larva";
      case KoLConstants.CONSUME_ZAP:
        return "zap wand";
      case KoLConstants.EQUIP_FAMILIAR:
        return "familiar equipment";
      case KoLConstants.EQUIP_ACCESSORY:
        return "accessory";
      case KoLConstants.EQUIP_HAT:
        return "hat";
      case KoLConstants.EQUIP_PANTS:
        return "pants";
      case KoLConstants.EQUIP_SHIRT:
        return "shirt";
      case KoLConstants.EQUIP_WEAPON:
        {
          String type = EquipmentDatabase.itemTypes.get(itemId);
          return type != null ? type : "weapon";
        }
      case KoLConstants.EQUIP_OFFHAND:
        {
          String type = EquipmentDatabase.itemTypes.get(itemId);
          return type != null ? type : "offhand";
        }
      case KoLConstants.EQUIP_CONTAINER:
        return "container";
      case KoLConstants.CONSUME_GUARDIAN:
        return "pasta guardian";
      default:
        return "";
    }
  }

  public static final boolean isAccordion(final int itemId) {
    return (EquipmentDatabase.getItemType(itemId).contains("accordion"));
  }

  public static final boolean isSpecialAccordion(final int itemId) {
    return (EquipmentDatabase.isAccordion(itemId)
        && itemId != ItemPool.TOY_ACCORDION
        && itemId != ItemPool.ANTIQUE_ACCORDION
        && itemId != ItemPool.AEROGEL_ACCORDION);
  }

  public static final Stat getWeaponStat(final int itemId) {
    int consumptionType = ItemDatabase.getConsumptionType(itemId);

    if (consumptionType != KoLConstants.EQUIP_WEAPON) {
      return Stat.NONE;
    }

    String req = EquipmentDatabase.getEquipRequirement(itemId);

    if (req.startsWith("Mox:")) {
      return Stat.MOXIE;
    }

    if (req.startsWith("Mys:")) {
      return Stat.MYSTICALITY;
    }

    return Stat.MUSCLE;
  }

  public static final WeaponType getWeaponType(final int itemId) {
    switch (EquipmentDatabase.getWeaponStat(itemId)) {
      case NONE:
        return WeaponType.NONE;
      case MOXIE:
        return WeaponType.RANGED;
      default:
        return WeaponType.MELEE;
    }
  }

  public static final boolean isChefStaff(final AdventureResult item) {
    return EquipmentDatabase.isChefStaff(item.getItemId());
  }

  public static final boolean isChefStaff(final int itemId) {
    return EquipmentDatabase.getItemType(itemId).equals("chefstaff");
  }

  public static final boolean isCanOfBeans(final AdventureResult item) {
    return EquipmentDatabase.isCanOfBeans(item.getItemId());
  }

  public static final boolean isCanOfBeans(final int itemId) {
    return EquipmentDatabase.getItemType(itemId).equals("can of beans");
  }

  public static final AdventureResult IRON_PALMS = EffectPool.get(EffectPool.IRON_PALMS);

  public static final boolean isClub(final int itemId) {
    String type = EquipmentDatabase.getItemType(itemId);
    return type.equals("club")
        || (KoLConstants.activeEffects.contains(EquipmentManager.IRON_PALMS)
            && type.equals("sword"));
  }

  public static final boolean isKnife(final int itemId) {
    return EquipmentDatabase.getItemType(itemId).equals("knife");
  }

  public static final boolean isSword(final int itemId) {
    return EquipmentDatabase.getItemType(itemId).equals("sword");
  }

  public static final boolean isGun(final int itemId) {
    return EquipmentDatabase.getItemType(itemId).equals("gun");
  }

  public static final boolean isPistol(final int itemId) {
    return EquipmentDatabase.getItemType(itemId).equals("pistol");
  }

  public static final boolean isRifle(final int itemId) {
    return EquipmentDatabase.getItemType(itemId).equals("rifle");
  }

  public static final boolean isUtensil(final int itemId) {
    return EquipmentDatabase.getItemType(itemId).equals("utensil");
  }

  public static final boolean isShield(final int itemId) {
    return EquipmentDatabase.getItemType(itemId).equals("shield");
  }

  public static final boolean isHat(final AdventureResult item) {
    return EquipmentDatabase.getItemType(item.getItemId()).equals("hat");
  }

  public static final boolean isShirt(final AdventureResult item) {
    return ItemDatabase.getConsumptionType(item.getItemId()) == KoLConstants.EQUIP_SHIRT;
  }

  public static final boolean isContainer(final AdventureResult item) {
    return ItemDatabase.getConsumptionType(item.getItemId()) == KoLConstants.EQUIP_CONTAINER;
  }

  public static final boolean isMainhandOnly(final AdventureResult item) {
    return EquipmentDatabase.isMainhandOnly(item.getItemId());
  }

  public static final boolean isMainhandOnly(final int itemId) {
    return EquipmentDatabase.isChefStaff(itemId)
        || EquipmentDatabase.isAccordion(itemId)
        || EquipmentDatabase.getHands(itemId) != 1;
  }

  public static void initializePulverization() {
    for (Integer id : ItemDatabase.descriptionIdKeySet()) {
      if (EquipmentDatabase.isPulverizable(id)) {
        EquipmentDatabase.getPulverization(id);
      }
    }
  }

  public static boolean isPulverizable(final int id) {
    if (id < 0) {
      return false;
    }

    switch (ItemDatabase.getConsumptionType(id)) {
      case KoLConstants.EQUIP_ACCESSORY:
      case KoLConstants.EQUIP_HAT:
      case KoLConstants.EQUIP_PANTS:
      case KoLConstants.EQUIP_SHIRT:
      case KoLConstants.EQUIP_WEAPON:
      case KoLConstants.EQUIP_OFFHAND:
      case KoLConstants.EQUIP_CONTAINER:
        break;

      case KoLConstants.EQUIP_FAMILIAR:
      default:
        return false;
    }

    if (ItemDatabase.isQuestItem(id)) {
      return false;
    }

    return true;
  }

  public static final int getPulverization(final int id) {
    if (id < 0) {
      return -1;
    }
    int pulver = EquipmentDatabase.pulverize.get(id);
    if (pulver == 0) {
      pulver = EquipmentDatabase.derivePulverization(id);
      EquipmentDatabase.pulverize.set(id, pulver);
    }
    return pulver;
  }

  private static int derivePulverization(final int id) {
    if (!EquipmentDatabase.isPulverizable(id)) {
      return -1;
    }

    if (ItemDatabase.isGiftable(id) && !ItemDatabase.isTradeable(id)) { // gift item
      return ItemPool.USELESS_POWDER;
    }

    if (NPCStoreDatabase.contains(id, false)) {
      return ItemPool.USELESS_POWDER;
    }

    int pulver = PULVERIZE_BITS | ELEM_TWINKLY;
    Modifiers mods = Modifiers.getItemModifiers(id);
    if (mods == null) { // Apparently no enchantments at all, which would imply that this
      // item pulverizes to useless powder.  However, there are many items
      // with enchantments that don't correspond to a KoLmafia modifier
      // (the "They do nothing!" enchantment of beer goggles, for example),
      // so this can't safely be assumed, so for now all truly unenchanted
      // items will have to be explicitly listed in pulverize.txt.
      pulver |= EquipmentDatabase.ELEM_TWINKLY;
    } else {
      for (int i = 0; i < IMPLICATIONS.length; i += 2) {
        if (mods.get(IMPLICATIONS[i]) > 0.0f) {
          pulver |= IMPLICATIONS[i + 1];
        }
      }
    }

    int power = EquipmentDatabase.power.get(id);
    if (power <= 0) {
      // power is unknown, derive from requirement (which isn't always accurate)
      pulver |= YIELD_UNCERTAIN;
      String req = EquipmentDatabase.statRequirements.get(id);

      if (req == null || req.equals("none")) {
        power = 0;
      } else {
        int colonIndex = req.indexOf(":");

        if (colonIndex != -1) {
          String reqValue = req.substring(colonIndex + 1).trim();
          power = StringUtilities.parseInt(reqValue) * 2 + 30;
        }
      }
    }
    if (power >= 180) {
      pulver |= YIELD_3W;
    } else if (power >= 160) {
      pulver |= YIELD_1W3N_2W;
    } else if (power >= 140) {
      pulver |= YIELD_4N_1W;
    } else if (power >= 120) {
      pulver |= YIELD_3N;
    } else if (power >= 100) {
      pulver |= YIELD_1N3P_2N;
    } else if (power >= 80) {
      pulver |= YIELD_4P_1N;
    } else if (power >= 60) {
      pulver |= YIELD_3P;
    } else if (power >= 40) {
      pulver |= YIELD_2P;
    } else {
      pulver |= YIELD_1P;
    }

    return pulver;
  }

  private static int deriveUpgrade(final String name) {
    int pulver = PULVERIZE_BITS | MALUS_UPGRADE | YIELD_4N_1W;
    if (name.endsWith("powder")) {
      pulver |= YIELD_4P_1N;
    }

    if (name.startsWith("twinkly")) {
      pulver |= ELEM_TWINKLY;
    } else if (name.startsWith("hot")) {
      pulver |= ELEM_HOT;
    } else if (name.startsWith("cold")) {
      pulver |= ELEM_COLD;
    } else if (name.startsWith("stench")) {
      pulver |= ELEM_STENCH;
    } else if (name.startsWith("spook")) {
      pulver |= ELEM_SPOOKY;
    } else if (name.startsWith("sleaz")) {
      pulver |= ELEM_SLEAZE;
    } else {
      pulver |= ELEM_OTHER;
    }
    return pulver;
  }

  private static int deriveCluster(final String name) {
    int pulver = PULVERIZE_BITS | YIELD_1C;

    if (name.startsWith("hot")) {
      pulver |= ELEM_HOT;
    } else if (name.startsWith("cold")) {
      pulver |= ELEM_COLD;
    } else if (name.startsWith("stench")) {
      pulver |= ELEM_STENCH;
    } else if (name.startsWith("spook")) {
      pulver |= ELEM_SPOOKY;
    } else if (name.startsWith("sleaz")) {
      pulver |= ELEM_SLEAZE;
    } else {
      pulver = ItemDatabase.getItemId(name);
    }
    return pulver;
  }

  public static final Set<Entry<Integer, String>> outfitEntrySet() {
    return EquipmentDatabase.outfitById.entrySet();
  }

  public static final SpecialOutfit getOutfit(final int id) {
    return EquipmentDatabase.normalOutfits.get(id);
  }

  public static final SpecialOutfit getAvailableOutfit(final int id) {
    SpecialOutfit outfit = EquipmentDatabase.normalOutfits.get(id);
    return EquipmentManager.getOutfits().contains(outfit) ? outfit : null;
  }

  public static final int getOutfitId(final KoLAdventure adventure) {
    int adventureId = Integer.parseInt(adventure.getAdventureId());

    switch (adventureId) {
      case AdventurePool.COBB_BARRACKS:
        return OutfitPool.KNOB_ELITE_OUTFIT;

      case AdventurePool.COBB_HAREM:
        return OutfitPool.HAREM_OUTFIT;

      case AdventurePool.ITZNOTYERZITZ_MINE:
        return OutfitPool.MINING_OUTFIT;

      case AdventurePool.EXTREME_SLOPE:
        return OutfitPool.EXTREME_COLD_WEATHER_GEAR;

      case AdventurePool.HIPPY_CAMP:
      case AdventurePool.HIPPY_CAMP_DISGUISED:
        return OutfitPool.HIPPY_OUTFIT;

      case AdventurePool.FRAT_HOUSE:
      case AdventurePool.FRAT_HOUSE_DISGUISED:
        return OutfitPool.FRAT_OUTFIT;

      case AdventurePool.PIRATE_COVE:
        return OutfitPool.SWASHBUCKLING_GETUP;

        // Choose the uniform randomly
      case AdventurePool.COLA_BATTLEFIELD:
        return KoLConstants.RNG.nextInt(2) == 0
            ? OutfitPool.CLOACA_UNIFORM
            : OutfitPool.DYSPEPSI_UNIFORM;

      case AdventurePool.CLOACA_BATTLEFIELD:
        return OutfitPool.CLOACA_UNIFORM;

      case AdventurePool.DYSPEPSI_BATTLEFIELD:
        return OutfitPool.DYSPEPSI_UNIFORM;

        // No outfit existed for this area
      default:
        return -1;
    }
  }

  /**
   * Internal class which functions exactly like an array of SpecialOutfits, except it uses "sets"
   * and "gets" like a list. This could be done with generics (Java 1.5) but is done like this so
   * that we get backwards compatibility.
   */
  public static class SpecialOutfitArray implements Iterable<SpecialOutfit> {
    private final ArrayList<SpecialOutfit> internalList = new ArrayList<SpecialOutfit>();
    private final TreeSet<Integer> internalSet = new TreeSet<Integer>();

    public Iterator<SpecialOutfit> iterator() {
      return this.internalList.iterator();
    }

    public SpecialOutfit get(final int index) {
      return index < 0 || index >= this.internalList.size() ? null : this.internalList.get(index);
    }

    public void set(final int index, final SpecialOutfit value) {
      for (int i = this.internalList.size(); i <= index; ++i) {
        this.internalList.add(null);
      }

      this.internalList.set(index, value);
      this.internalSet.add(Integer.valueOf(index));
    }

    public int size() {
      return this.internalList.size();
    }

    public List<SpecialOutfit> toList() {
      return this.internalList;
    }

    public Set<Integer> keySet() {
      return this.internalSet;
    }
  }
}
