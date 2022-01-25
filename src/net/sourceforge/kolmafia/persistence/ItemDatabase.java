package net.sourceforge.kolmafia.persistence;

import java.io.BufferedReader;
import java.io.File;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.ImageIcon;
import net.java.dev.spellcast.utilities.JComponentUtilities;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLConstants.CraftingType;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.Modifiers;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.StaticEntity;
import net.sourceforge.kolmafia.VYKEACompanionData;
import net.sourceforge.kolmafia.objectpool.Concoction;
import net.sourceforge.kolmafia.objectpool.ConcoctionPool;
import net.sourceforge.kolmafia.objectpool.EffectPool;
import net.sourceforge.kolmafia.objectpool.IntegerPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.objectpool.SkillPool;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.ApiRequest;
import net.sourceforge.kolmafia.request.ClanLoungeRequest;
import net.sourceforge.kolmafia.request.StandardRequest;
import net.sourceforge.kolmafia.request.SushiRequest;
import net.sourceforge.kolmafia.session.EquipmentManager;
import net.sourceforge.kolmafia.utilities.FileUtilities;
import net.sourceforge.kolmafia.utilities.IntegerArray;
import net.sourceforge.kolmafia.utilities.LogStream;
import net.sourceforge.kolmafia.utilities.StringArray;
import net.sourceforge.kolmafia.utilities.StringUtilities;
import org.json.JSONException;
import org.json.JSONObject;

public class ItemDatabase {
  private static int maxItemId = 0;

  private static String[] canonicalNames = new String[0];
  private static final IntegerArray useTypeById = new IntegerArray();
  private static final IntegerArray attributesById = new IntegerArray();
  private static final IntegerArray priceById = new IntegerArray();
  private static final IntegerArray nameLength = new IntegerArray();
  private static final StringArray pluralById = new StringArray();
  private static final StringArray imageById = new StringArray();

  private static final Map<Integer, String> nameById = new TreeMap<Integer, String>();
  private static final Map<Integer, String> dataNameById = new HashMap<Integer, String>();
  private static final Map<Integer, String> descriptionById = new TreeMap<Integer, String>();
  private static final Map<String, int[]> itemIdSetByName = new HashMap<String, int[]>();
  private static final ArrayList<String> itemAliases = new ArrayList<String>();
  private static final ArrayList<String> pluralAliases = new ArrayList<String>();
  private static final Map<String, Integer> itemIdByPlural = new HashMap<String, Integer>();

  private static final Map<String, Integer> itemIdByDescription = new HashMap<String, Integer>();
  private static final Map<String, FoldGroup> foldGroupsByName = new HashMap<>();

  private static final Map<Integer, int[]> itemSourceByNoobSkillId = new HashMap<Integer, int[]>();
  private static final IntegerArray noobSkillIdByItemSource = new IntegerArray();

  public static final String QUEST_FLAG = "q";
  public static final String GIFT_FLAG = "g";
  public static final String TRADE_FLAG = "t";
  public static final String DISCARD_FLAG = "d";
  public static final String BOGUS_FLAG = "z";

  public static Object[][] PUNCHCARDS = {
    // Verbs
    {IntegerPool.get(3146), "El Vibrato punchcard (115 holes)", "El Vibrato punchcard (ATTACK)"},
    {IntegerPool.get(3147), "El Vibrato punchcard (97 holes)", "El Vibrato punchcard (REPAIR)"},
    {IntegerPool.get(3148), "El Vibrato punchcard (129 holes)", "El Vibrato punchcard (BUFF)"},
    {IntegerPool.get(3149), "El Vibrato punchcard (213 holes)", "El Vibrato punchcard (MODIFY)"},
    {IntegerPool.get(3150), "El Vibrato punchcard (165 holes)", "El Vibrato punchcard (BUILD)"},

    // Objects
    {IntegerPool.get(3151), "El Vibrato punchcard (142 holes)", "El Vibrato punchcard (TARGET)"},
    {IntegerPool.get(3152), "El Vibrato punchcard (216 holes)", "El Vibrato punchcard (SELF)"},
    {IntegerPool.get(3153), "El Vibrato punchcard (88 holes)", "El Vibrato punchcard (FLOOR)"},
    {IntegerPool.get(3154), "El Vibrato punchcard (182 holes)", "El Vibrato punchcard (DRONE)"},
    {IntegerPool.get(3155), "El Vibrato punchcard (176 holes)", "El Vibrato punchcard (WALL)"},
    {IntegerPool.get(3156), "El Vibrato punchcard (104 holes)", "El Vibrato punchcard (SPHERE)"}
  };

  private static final Object[][] ALIASES = {
    {IntegerPool.get(4577), "bugged bonnet"},
    {IntegerPool.get(4578), "bugged meat stabbing club"},
    {IntegerPool.get(4579), "bugged Knob Goblin love potion"},
    {IntegerPool.get(4580), "bugged old school Mafia knickerbockers"},
    {IntegerPool.get(4581), "bugged Talisman of Baio"},
    {IntegerPool.get(-1), "potion of inebriety"},
    {IntegerPool.get(-1), "potion of healing"},
    {IntegerPool.get(-1), "potion of confusion"},
    {IntegerPool.get(-1), "potion of blessing"},
    {IntegerPool.get(-1), "potion of detection"},
    {IntegerPool.get(-1), "potion of sleepiness"},
    {IntegerPool.get(-1), "potion of mental acuity"},
    {IntegerPool.get(-1), "potion of ettin strength"},
    {IntegerPool.get(-1), "potion of teleportitis"},
    {IntegerPool.get(-1), "vial of slime: strong"},
    {IntegerPool.get(-1), "vial of slime: sagacious"},
    {IntegerPool.get(-1), "vial of slime: speedy"},
    {IntegerPool.get(-1), "vial of slime: brawn"},
    {IntegerPool.get(-1), "vial of slime: brains"},
    {IntegerPool.get(-1), "vial of slime: briskness"},
    {IntegerPool.get(-1), "vial of slime: slimeform"},
    {IntegerPool.get(-1), "vial of slime: eyesight"},
    {IntegerPool.get(-1), "vial of slime: intensity"},
    {IntegerPool.get(-1), "vial of slime: muscle"},
    {IntegerPool.get(-1), "vial of slime: mentalism"},
    {IntegerPool.get(-1), "vial of slime: moxiousness"},
  };

  private static final List<String> ACCESS =
      Arrays.asList(QUEST_FLAG, GIFT_FLAG, TRADE_FLAG, DISCARD_FLAG);

  private ItemDatabase() {}

  private static String parseAccess(final String data) {
    if (data.equals("")) {
      return data;
    }

    String[] accessTypes = data.split("\\s*,\\s*");
    for (String accessType : accessTypes) {
      if (!ACCESS.contains(accessType)) {
        return BOGUS_FLAG;
      }
    }
    return data;
  }

  private static final Map<Integer, String> accessById = new HashMap<Integer, String>();

  public static final int ATTR_QUEST = 0x00000001;
  public static final int ATTR_GIFT = 0x00000002;
  public static final int ATTR_TRADEABLE = 0x00000004;
  public static final int ATTR_DISCARDABLE = 0x00000008;
  public static final int ATTR_COMBAT = 0x00000010;
  public static final int ATTR_COMBAT_REUSABLE = 0x00000020;
  public static final int ATTR_USABLE = 0x00000040;
  public static final int ATTR_MULTIPLE = 0x00000080;
  public static final int ATTR_REUSABLE = 0x00000100;
  public static final int ATTR_SINGLE = 0x00000200;
  public static final int ATTR_SOLO = 0x00000400;
  public static final int ATTR_CURSE = 0x00000800;
  public static final int ATTR_BOUNTY = 0x00001000;
  public static final int ATTR_CANDY0 = 0x00002000;
  public static final int ATTR_CANDY1 = 0x00004000;
  public static final int ATTR_CANDY2 = 0x00008000;
  public static final int ATTR_MATCHABLE = 0x00010000;
  public static final int ATTR_FANCY = 0x00020000;
  public static final int ATTR_CHOCOLATE = 0x00040000;
  public static final int ATTR_PASTE = 0x00080000;
  public static final int ATTR_SMITH = 0x00100000;
  public static final int ATTR_COOK = 0x00200000;
  public static final int ATTR_MIX = 0x00400000;

  private static final HashMap<String, Integer> PRIMARY_USE = new HashMap<String, Integer>();
  private static final HashMap<Integer, String> INVERSE_PRIMARY_USE =
      new HashMap<Integer, String>();
  private static final HashMap<String, Integer> SECONDARY_USE = new HashMap<String, Integer>();
  private static final TreeMap<Integer, String> INVERSE_SECONDARY_USE =
      new TreeMap<Integer, String>();

  private static void definePrimaryUse(final String key, final int usage) {
    Integer val = IntegerPool.get(usage);
    PRIMARY_USE.put(key, val);
    INVERSE_PRIMARY_USE.put(val, key);
  }

  private static void defineSecondaryUse(final String key, final int usage) {
    Integer val = IntegerPool.get(usage);
    SECONDARY_USE.put(key, val);
    INVERSE_SECONDARY_USE.put(val, key);
  }

  static {
    ItemDatabase.definePrimaryUse("none", KoLConstants.NO_CONSUME);

    ItemDatabase.definePrimaryUse("food", KoLConstants.CONSUME_EAT);
    ItemDatabase.definePrimaryUse("drink", KoLConstants.CONSUME_DRINK);
    ItemDatabase.definePrimaryUse("spleen", KoLConstants.CONSUME_SPLEEN);

    ItemDatabase.definePrimaryUse("usable", KoLConstants.CONSUME_USE);
    ItemDatabase.definePrimaryUse("multiple", KoLConstants.CONSUME_MULTIPLE);
    ItemDatabase.definePrimaryUse("reusable", KoLConstants.INFINITE_USES);
    ItemDatabase.definePrimaryUse("message", KoLConstants.MESSAGE_DISPLAY);

    ItemDatabase.definePrimaryUse("grow", KoLConstants.GROW_FAMILIAR);

    ItemDatabase.definePrimaryUse("hat", KoLConstants.EQUIP_HAT);
    ItemDatabase.definePrimaryUse("weapon", KoLConstants.EQUIP_WEAPON);
    ItemDatabase.definePrimaryUse("offhand", KoLConstants.EQUIP_OFFHAND);
    ItemDatabase.definePrimaryUse("container", KoLConstants.EQUIP_CONTAINER);
    ItemDatabase.definePrimaryUse("shirt", KoLConstants.EQUIP_SHIRT);
    ItemDatabase.definePrimaryUse("pants", KoLConstants.EQUIP_PANTS);
    ItemDatabase.definePrimaryUse("accessory", KoLConstants.EQUIP_ACCESSORY);
    ItemDatabase.definePrimaryUse("familiar", KoLConstants.EQUIP_FAMILIAR);

    ItemDatabase.definePrimaryUse("sticker", KoLConstants.CONSUME_STICKER);
    ItemDatabase.definePrimaryUse("card", KoLConstants.CONSUME_CARD);
    ItemDatabase.definePrimaryUse("folder", KoLConstants.CONSUME_FOLDER);
    ItemDatabase.definePrimaryUse("bootskin", KoLConstants.CONSUME_BOOTSKIN);
    ItemDatabase.definePrimaryUse("bootspur", KoLConstants.CONSUME_BOOTSPUR);
    ItemDatabase.definePrimaryUse("sixgun", KoLConstants.CONSUME_SIXGUN);

    ItemDatabase.definePrimaryUse("food helper", KoLConstants.CONSUME_FOOD_HELPER);
    ItemDatabase.definePrimaryUse("drink helper", KoLConstants.CONSUME_DRINK_HELPER);
    ItemDatabase.definePrimaryUse("zap", KoLConstants.CONSUME_ZAP);
    ItemDatabase.definePrimaryUse("sphere", KoLConstants.CONSUME_SPHERE);
    ItemDatabase.definePrimaryUse("guardian", KoLConstants.CONSUME_GUARDIAN);
    ItemDatabase.definePrimaryUse("pokepill", KoLConstants.CONSUME_POKEPILL);

    ItemDatabase.definePrimaryUse("potion", KoLConstants.CONSUME_POTION);
    ItemDatabase.definePrimaryUse("avatar", KoLConstants.CONSUME_AVATAR);

    ItemDatabase.defineSecondaryUse("usable", ItemDatabase.ATTR_USABLE);
    ItemDatabase.defineSecondaryUse("multiple", ItemDatabase.ATTR_MULTIPLE);
    ItemDatabase.defineSecondaryUse("reusable", ItemDatabase.ATTR_REUSABLE);

    ItemDatabase.defineSecondaryUse("combat", ItemDatabase.ATTR_COMBAT);
    ItemDatabase.defineSecondaryUse("combat reusable", ItemDatabase.ATTR_COMBAT_REUSABLE);

    ItemDatabase.defineSecondaryUse("single", ItemDatabase.ATTR_SINGLE);
    ItemDatabase.defineSecondaryUse("solo", ItemDatabase.ATTR_SOLO);

    ItemDatabase.defineSecondaryUse("curse", ItemDatabase.ATTR_CURSE);
    ItemDatabase.defineSecondaryUse("bounty", ItemDatabase.ATTR_BOUNTY);
    ItemDatabase.defineSecondaryUse("candy", ItemDatabase.ATTR_CANDY0);
    ItemDatabase.defineSecondaryUse("candy1", ItemDatabase.ATTR_CANDY1);
    ItemDatabase.defineSecondaryUse("candy2", ItemDatabase.ATTR_CANDY2);
    ItemDatabase.defineSecondaryUse("matchable", ItemDatabase.ATTR_MATCHABLE);
    ItemDatabase.defineSecondaryUse("fancy", ItemDatabase.ATTR_FANCY);
    ItemDatabase.defineSecondaryUse("chocolate", ItemDatabase.ATTR_CHOCOLATE);
    ItemDatabase.defineSecondaryUse("paste", ItemDatabase.ATTR_PASTE);
    ItemDatabase.defineSecondaryUse("smith", ItemDatabase.ATTR_SMITH);
    ItemDatabase.defineSecondaryUse("cook", ItemDatabase.ATTR_COOK);
    ItemDatabase.defineSecondaryUse("mix", ItemDatabase.ATTR_MIX);
  }

  private static final Set<Entry<Integer, String>> secondaryUsageEntrySet =
      INVERSE_SECONDARY_USE.entrySet();

  public static boolean newItems = false;

  static {
    ItemDatabase.reset();
  }

  public static void reset() {
    ItemDatabase.newItems = false;

    if (!ItemDatabase.itemIdSetByName.isEmpty()) {
      ItemDatabase.miniReset();
      return;
    }

    ItemDatabase.itemIdSetByName.clear();
    ItemDatabase.itemSourceByNoobSkillId.clear();

    ItemDatabase.readItems();
    ItemDatabase.readFoldGroups();
    ItemDatabase.addPseudoItems();
    ItemDatabase.saveCanonicalNames();
  }

  private static void addIdToName(String canonicalName, int itemId) {
    int[] idSet = ItemDatabase.itemIdSetByName.get(canonicalName);
    int[] newSet;

    if (idSet == null) {
      newSet = new int[1];
    }
    // *** This assumes the array is sorted
    else if (Arrays.binarySearch(idSet, itemId) >= 0) {
      return;
    } else {
      newSet = Arrays.copyOf(idSet, idSet.length + 1);
    }

    newSet[newSet.length - 1] = itemId;
    // *** Make it so
    Arrays.sort(newSet);
    ItemDatabase.itemIdSetByName.put(canonicalName, newSet);
  }

  private static void addIdToNoobSkill(Integer skillId, int itemId) {
    int[] idSources = ItemDatabase.itemSourceByNoobSkillId.get(skillId);
    int[] newSources;

    if (idSources == null) {
      newSources = new int[1];
    }
    // *** This assumes the array is sorted
    else if (Arrays.binarySearch(idSources, itemId) >= 0) {
      return;
    } else {
      newSources = Arrays.copyOf(idSources, idSources.length + 1);
    }

    newSources[newSources.length - 1] = itemId;
    // *** Make it so
    Arrays.sort(newSources);
    ItemDatabase.itemSourceByNoobSkillId.put(skillId, newSources);
  }

  private static void miniReset() {
    BufferedReader reader =
        FileUtilities.getVersionedReader("items.txt", KoLConstants.ITEMS_VERSION);

    String[] data;

    while ((data = FileUtilities.readData(reader)) != null) {
      if (data.length < 7) {
        continue;
      }

      int itemId = StringUtilities.parseInt(data[0]);
      String canonicalName = StringUtilities.getCanonicalName(data[1]);
      ItemDatabase.addIdToName(canonicalName, itemId);
    }

    try {
      reader.close();
    } catch (Exception e) {
      StaticEntity.printStackTrace(e);
    }

    // Remove per-user item aliases
    Iterator<String> it = ItemDatabase.itemAliases.iterator();
    while (it.hasNext()) {
      String canonical = it.next();
      ItemDatabase.itemIdSetByName.remove(canonical);
    }
    ItemDatabase.itemAliases.clear();

    it = ItemDatabase.pluralAliases.iterator();
    while (it.hasNext()) {
      String canonical = it.next();
      ItemDatabase.itemIdByPlural.remove(canonical);
    }
    ItemDatabase.pluralAliases.clear();

    ItemDatabase.addPseudoItems();

    ItemDatabase.saveCanonicalNames();
  }

  private static void readItems() {
    BufferedReader reader =
        FileUtilities.getVersionedReader("items.txt", KoLConstants.ITEMS_VERSION);
    String[] data;

    while ((data = FileUtilities.readData(reader)) != null) {
      if (data.length < 7) {
        continue;
      }

      int itemId = StringUtilities.parseInt(data[0]);
      Integer id = IntegerPool.get(itemId);

      String name = data[1];
      String displayName = StringUtilities.getDisplayName(name);
      String canonicalName = StringUtilities.getCanonicalName(name);

      String descId = data[2];
      if (StringUtilities.isNumeric(descId)) {
        ItemDatabase.descriptionById.put(id, descId);
        ItemDatabase.itemIdByDescription.put(descId, id);
      }

      String image = data[3];
      ItemDatabase.imageById.set(itemId, image);

      String[] usages = data[4].split("\\s*,\\s*");
      String access = ItemDatabase.parseAccess(data[5]);
      int price = StringUtilities.parseInt(data[6]);

      String usage = usages[0];
      Integer useType = ItemDatabase.PRIMARY_USE.get(usage);
      if (useType == null) {
        RequestLogger.printLine("Unknown primary usage for " + name + ": " + usage);
      } else {
        ItemDatabase.useTypeById.set(itemId, useType.intValue());
      }

      int attrs = 0;
      for (int i = 1; i < usages.length; ++i) {
        usage = usages[i];
        useType = ItemDatabase.SECONDARY_USE.get(usage);
        if (useType == null) {
          RequestLogger.printLine("Unknown secondary usage for " + name + ": " + usage);
        } else {
          attrs |= useType.intValue();
          CandyDatabase.registerCandy(id, usage);
        }
      }

      ItemDatabase.priceById.set(itemId, price);
      ItemDatabase.dataNameById.put(id, name);
      ItemDatabase.nameById.put(id, displayName);

      ItemDatabase.accessById.put(id, access);
      attrs |= access.contains(TRADE_FLAG) ? ItemDatabase.ATTR_TRADEABLE : 0;
      attrs |= access.contains(GIFT_FLAG) ? ItemDatabase.ATTR_GIFT : 0;
      attrs |= access.contains(QUEST_FLAG) ? ItemDatabase.ATTR_QUEST : 0;
      attrs |= access.contains(DISCARD_FLAG) ? ItemDatabase.ATTR_DISCARDABLE : 0;
      ItemDatabase.attributesById.set(itemId, attrs);

      if (itemId > ItemDatabase.maxItemId) {
        ItemDatabase.maxItemId = itemId;
      }

      ItemDatabase.addIdToName(canonicalName, itemId);

      ItemDatabase.nameLength.set(itemId, displayName.length());

      if (data.length == 8) {
        String plural = data[7];
        ItemDatabase.pluralById.set(itemId, plural);
        ItemDatabase.itemIdByPlural.put(StringUtilities.getCanonicalName(plural), id);
      }
      // Build Noobcore skill source list
      if ((!ItemDatabase.isEquipment(itemId) || ItemDatabase.isFamiliarEquipment(itemId))
          && ItemDatabase.isDiscardable(itemId)
          && (ItemDatabase.isTradeable(itemId)
              || ItemDatabase.isGiftItem(itemId)
              || itemId == ItemPool.CLOD_OF_DIRT
              || itemId == ItemPool.DIRTY_BOTTLECAP
              || itemId == ItemPool.DISCARDED_BUTTON)) {
        int intDescId = StringUtilities.parseInt(descId);
        int skillId = (intDescId % 125) + 23001;
        // Override Robortender items
        switch (itemId) {
          case ItemPool.NOVELTY_HOT_SAUCE:
            skillId = SkillPool.FROWN_MUSCLES;
            break;
          case ItemPool.COCKTAIL_MUSHROOM:
            skillId = SkillPool.RETRACTABLE_TOES;
            break;
          case ItemPool.GRANOLA_LIQUEUR:
            skillId = SkillPool.INK_GLAND;
            break;
          case ItemPool.GREGNADIGNE:
            skillId = SkillPool.BENDABLE_KNEES;
            break;
          case ItemPool.BABY_OIL_SHOOTER:
            skillId = SkillPool.POWERFUL_VOCAL_CHORDS;
            break;
          case ItemPool.LIMEPATCH:
            skillId = SkillPool.ANGER_GLANDS;
            break;
        }
        ItemDatabase.addIdToNoobSkill(IntegerPool.get(skillId), itemId);
        ItemDatabase.noobSkillIdByItemSource.set(itemId, skillId);
      }
    }

    try {
      reader.close();
    } catch (Exception e) {
      StaticEntity.printStackTrace(e);
    }
  }

  public static void writeItems(final File output) {
    RequestLogger.printLine("Writing data override: " + output);
    PrintStream writer = LogStream.openStream(output, true);
    writer.println(KoLConstants.ITEMS_VERSION);

    Iterator<Entry<Integer, String>> it = ItemDatabase.descriptionIdEntrySet().iterator();
    int lastInteger = 1;

    while (it.hasNext()) {
      Entry<Integer, String> entry = it.next();
      Integer nextInteger = entry.getKey();
      int itemId = nextInteger.intValue();

      // Skip pseudo items
      if (itemId == 13 || itemId < 1) {
        continue;
      }

      for (int i = lastInteger; i < itemId; ++i) {
        writer.println(i);
      }

      lastInteger = itemId + 1;
      String descId = entry.getValue();
      String name = ItemDatabase.getItemDataName(nextInteger);
      String image = ItemDatabase.getImage(itemId);
      // Intentionally get a null if there is not an explicit plural in the database
      String plural = ItemDatabase.getPluralById(itemId);
      int type = ItemDatabase.getConsumptionType(itemId);
      int attrs = ItemDatabase.getAttributes(itemId);
      String access = ItemDatabase.getAccessById(nextInteger);
      int price = ItemDatabase.getPriceById(itemId);
      writer.println(
          ItemDatabase.itemString(itemId, name, descId, image, type, attrs, access, price, plural));
    }

    writer.close();
  }

  public static void writeItem() {}

  public static String itemString(
      final int itemId,
      final String name,
      final String descId,
      final String image,
      final int type,
      final int attrs,
      final String access,
      final int autosell,
      final String plural) {
    return itemId
        + "\t"
        + name
        + "\t"
        + descId
        + "\t"
        + image
        + "\t"
        + typeToPrimaryUsage(type)
        + attrsToSecondaryUsage(attrs)
        + "\t"
        + access
        + "\t"
        + autosell
        + (plural == null || plural.equals("") ? "" : "\t" + plural);
  }

  public static class FoldGroup {
    public final int damage;
    public final List<String> names;

    private FoldGroup(final int damage, final List<String> names) {
      this.damage = damage;
      this.names = names;
    }
  }

  private static void readFoldGroups() {
    BufferedReader reader =
        FileUtilities.getVersionedReader("foldgroups.txt", KoLConstants.FOLDGROUPS_VERSION);
    String[] data;

    while ((data = FileUtilities.readData(reader)) != null) {
      if (data.length <= 2) {
        continue;
      }

      int damage = StringUtilities.parseInt(data[0]);
      ArrayList<String> names = new ArrayList<>();
      FoldGroup group = new FoldGroup(damage, names);
      for (int i = 1; i < data.length; ++i) {
        String name = StringUtilities.getCanonicalName(data[i]);
        if (ItemDatabase.itemIdSetByName.get(name) == null) {
          RequestLogger.printLine("Unknown foldable item: " + name);
          continue;
        }
        ItemDatabase.foldGroupsByName.put(name, group);
        names.add(name);
      }
      names.trimToSize();
    }

    try {
      reader.close();
    } catch (Exception e) {
      StaticEntity.printStackTrace(e);
    }
  }

  private static void addPseudoItems() {
    Integer id = IntegerPool.get(13);

    ItemDatabase.dataNameById.put(id, "worthless item");
    ItemDatabase.nameById.put(id, "worthless item");
    ItemDatabase.addIdToName("worthless item", id);

    // Set aliases for the dusty bottles
    for (Object[] dusty : ConsumablesDatabase.DUSTY_BOTTLES) {
      id = (Integer) dusty[0];
      String name = StringUtilities.getCanonicalName((String) dusty[1]);
      String alias = StringUtilities.getCanonicalName((String) dusty[2]);
      String plural = StringUtilities.singleStringReplace(alias, "bottle", "bottles");
      ItemDatabase.addIdToName(alias, id);
      ItemDatabase.itemIdByPlural.put(plural, id);
      ConsumablesDatabase.cloneConsumptionData(name, alias);
    }

    // Set aliases for the El Vibrato punch cards
    for (Object[] punchcard : ItemDatabase.PUNCHCARDS) {
      id = (Integer) punchcard[0];
      String alias = StringUtilities.getCanonicalName((String) punchcard[2]);
      String plural = StringUtilities.singleStringReplace(alias, "punchcard", "punchcards");
      ItemDatabase.addIdToName(alias, id);
      ItemDatabase.itemIdByPlural.put(plural, id);
    }

    // Add names of all the sushi
    id = IntegerPool.get(-1);
    for (String sushi : SushiRequest.SUSHI) {
      String name = StringUtilities.getCanonicalName(sushi);
      ItemDatabase.addIdToName(name, id);
    }

    // Add names of all the VYKEA companions
    id = IntegerPool.get(-1);
    for (String VYKEA : VYKEACompanionData.VYKEA) {
      String name = StringUtilities.getCanonicalName(VYKEA);
      ItemDatabase.addIdToName(name, id);
    }

    // Miscellaneous aliases for untypeable item names
    for (Object[] alias : ItemDatabase.ALIASES) {
      id = (Integer) alias[0];
      String name = StringUtilities.getCanonicalName((String) alias[1]);
      ItemDatabase.addIdToName(name, id);
    }
  }

  private static void saveCanonicalNames() {
    String[] newArray = new String[ItemDatabase.itemIdSetByName.size()];
    ItemDatabase.itemIdSetByName.keySet().toArray(newArray);
    Arrays.sort(newArray);
    ItemDatabase.canonicalNames = newArray;
  }

  /**
   * Temporarily adds an item to the item database. This is used whenever KoLmafia encounters an
   * unknown item in the mall or in the player's inventory.
   */
  private static final Pattern RELSTRING_PATTERN = Pattern.compile("([\\w]+)=([^&]*)&?");

  // "id=588&s=118&q=0&d=1&g=0&t=1&n=50&m=0&u=.&ou=use"
  //   id = item Id
  //   s = sell value
  //   q = quest item
  //   d = discardable
  //   g = gift item
  //   t = transferable
  //   n = number
  //   p = pulverizable
  //   u = how can this be used?
  //	 e = Eatable (food/drink) (inv_eat)
  //	 b = Booze (inv_booze)
  //	 q = eQuipable (inv_equip)
  //	 u = potion/Useable (inv_use/multiuse)
  //	 . = can't (or doesn't fit those types)
  //  ou = "other use text" which is used to make drinks show
  //	 "drink" instead of "eat" and for items which over-ride
  //	 the word "use" in links, like the PYEC or scratch 'n'
  //	 sniff stickers.

  public static final AdventureResult itemFromRelString(final String relString) {
    int itemId = -1;
    int count = 1;

    Matcher matcher = RELSTRING_PATTERN.matcher(relString);
    while (matcher.find()) {
      String tag = matcher.group(1);
      String value = matcher.group(2);
      if (tag.equals("id")) {
        itemId = StringUtilities.parseInt(value);
      } else if (tag.equals("n")) {
        count = StringUtilities.parseInt(value);
      }
    }

    return ItemPool.get(itemId, count);
  }

  public static final String relStringValue(final String relString, final String search) {
    Matcher matcher = RELSTRING_PATTERN.matcher(relString);
    while (matcher.find()) {
      String tag = matcher.group(1);
      String value = matcher.group(2);
      if (tag.equals(search)) {
        return value;
      }
    }

    return null;
  }

  public static final int relStringNumericValue(final String relString, final String search) {
    String value = ItemDatabase.relStringValue(relString, search);
    return value != null ? StringUtilities.parseInt(value) : -1;
  }

  public static final int relStringItemId(final String relString) {
    return ItemDatabase.relStringNumericValue(relString, "id");
  }

  public static final int relStringCount(final String relString) {
    return ItemDatabase.relStringNumericValue(relString, "n");
  }

  public static final boolean relStringMultiusable(final String relString) {
    String value = ItemDatabase.relStringValue(relString, "m");
    return value != null && value.equals("1");
  }

  public static final String extractItemsPlural(final int count, final String items) {
    if (count > 1 && items != null) {
      int space = items.indexOf(" ");
      if (space != -1) {
        String num = items.substring(0, space);
        if (StringUtilities.isNumeric(num) && StringUtilities.parseInt(num) == count) {
          return items.substring(space + 1);
        }
      }
    }
    return null;
  }

  private static final Pattern DESC_PATTERN = Pattern.compile("descitem\\((\\d+)");

  public static final boolean parseNewItems(final String responseText) {
    Matcher m = DESC_PATTERN.matcher(responseText);
    while (m.find()) {
      String descId = m.group(1);
      if (ItemDatabase.getItemIdFromDescription(descId) == -1) {
        ItemDatabase.registerItem(descId);
      }
    }
    return true;
  }

  public static final void registerItem(final int itemId) {
    // This only works for items you own.
    ApiRequest request = new ApiRequest("item", itemId);
    RequestThread.postRequest(request);

    JSONObject JSON = request.JSON;
    if (JSON == null) {
      return;
    }

    // {
    //   "name":"Loathing Legion pizza stone",
    //   "descid":"708831312",
    //   "sellvalue":"0",
    //   "picture":"llpizzastone",
    //   "type":"offhand",
    //   "hands":"1",
    //   "power":"100",
    //   "candiscard":"0",
    //   "cantransfer":"0",
    //   "fancy":"0",
    //   "quest":"0",
    //   "combine":"0",
    //   "smith":"0",
    //   "jewelry":"0",
    //   "cook":"0",
    //   "cocktail":"0",
    //   "unhardcore":"1"
    // }

    try {
      String name = JSON.getString("name");
      String descid = JSON.getString("descid");
      int power = JSON.getInt("power");
      ItemDatabase.registerItem(itemId, name, descid, null, power, false);
    } catch (JSONException e) {
      KoLmafia.updateDisplay("Error parsing JSON string!");
      StaticEntity.printStackTrace(e);
    }
  }

  public static void registerItem(String descId) {
    // Pull the itemName from the item description, which will be cached
    String text =
        DebugDatabase.itemDescriptionText(DebugDatabase.rawItemDescriptionText(descId, true));
    if (text == null) {
      return;
    }

    int itemId = DebugDatabase.parseItemId(text);

    // Link this itemId and descId
    Integer id = IntegerPool.get(itemId);
    ItemDatabase.descriptionById.put(id, descId);
    ItemDatabase.itemIdByDescription.put(descId, id);

    String itemName = DebugDatabase.parseName(text);

    ItemDatabase.registerItem(itemId, itemName, descId, null, 0, false);
  }

  public static final void registerItem(final int itemId, String itemName, String descId) {
    ItemDatabase.registerItem(itemId, itemName, descId, null, 0, false);
  }

  public static final void registerItem(
      final String itemName, final String descId, final String relString, final String boldName) {
    // This works for any item that comes in to inventory accompanied by a relstring.

    int itemId = -1;
    int count = 1;
    boolean multi = false;

    Matcher matcher = RELSTRING_PATTERN.matcher(relString);
    while (matcher.find()) {
      String tag = matcher.group(1);
      String value = matcher.group(2);
      if (tag.equals("id")) {
        itemId = StringUtilities.parseInt(value);
      } else if (tag.equals("n")) {
        count = StringUtilities.parseInt(value);
      } else if (tag.equals("m")) {
        multi = value.equals("1");
      }
    }

    // If we could not find the item id, nothing to do
    if (itemId < 0) {
      return;
    }

    // If we found more than one item and the "boldName" string is not
    // null, we probably have the plural.
    String plural = extractItemsPlural(count, boldName);
    ItemDatabase.registerItem(itemId, itemName, descId, plural, 0, multi);
  }

  public static final void registerItem(
      final int itemId,
      String itemName,
      String descId,
      final String plural,
      final int power,
      final boolean multi) {
    if (itemName == null) {
      return;
    }

    // Remember that a new item has been discovered
    ItemDatabase.newItems = true;

    RequestLogger.printLine(
        "Unknown item found: " + itemName + " (" + itemId + ", " + descId + ")");

    if (itemId > ItemDatabase.maxItemId) {
      ItemDatabase.maxItemId = itemId;
    }

    Integer id = IntegerPool.get(itemId);

    ItemDatabase.nameById.put(id, StringUtilities.getDisplayName(itemName));
    ItemDatabase.dataNameById.put(id, itemName);
    ItemDatabase.descriptionById.put(id, descId);
    ItemDatabase.itemIdByDescription.put(descId, id);

    ItemDatabase.addIdToName(StringUtilities.getCanonicalName(itemName), id);
    ItemDatabase.saveCanonicalNames();

    if (plural != null) {
      ItemDatabase.registerPlural(itemId, plural);
    }
    ItemDatabase.parseItemDescription(id, descId, power, multi);

    // Build Noobcore skill source list
    if ((!ItemDatabase.isEquipment(itemId) || ItemDatabase.isFamiliarEquipment(itemId))
        && !ItemDatabase.isQuestItem(itemId)
        && ItemDatabase.isDiscardable(itemId)
        && (ItemDatabase.isTradeable(itemId)
            || ItemDatabase.isGiftItem(itemId)
            || itemId == ItemPool.CLOD_OF_DIRT
            || itemId == ItemPool.DIRTY_BOTTLECAP
            || itemId == ItemPool.DISCARDED_BUTTON)) {
      int intDescId = StringUtilities.parseInt(descId);
      int skillId = (intDescId % 125) + 23001;
      ItemDatabase.addIdToNoobSkill(IntegerPool.get(skillId), itemId);
      ItemDatabase.noobSkillIdByItemSource.set(itemId, skillId);
    }

    // If it is equipment, derive pulverization
    EquipmentDatabase.getPulverization(itemId);

    // Add the new item to the ConcoctionPool
    AdventureResult ar = ItemPool.get(itemId, 1);
    Concoction c = new Concoction(ar, CraftingType.NOCREATE);
    ConcoctionPool.set(c);
    ConcoctionDatabase.addUsableConcoction(c);
  }

  public static final void registerPlural(final int itemId, final String plural) {
    ItemDatabase.pluralById.set(itemId, plural);
    ItemDatabase.itemIdByPlural.put(
        StringUtilities.getCanonicalName(plural), IntegerPool.get(itemId));
  }

  public static final void registerMultiUsability(final int itemId, final boolean multi) {
    int useType = ItemDatabase.useTypeById.get(itemId);
    int attributes = ItemDatabase.getAttributes(itemId);

    if (multi) {
      // We think the item is single usable but it really is multiusable
      if (useType == KoLConstants.CONSUME_USE) {
        ItemDatabase.useTypeById.set(itemId, KoLConstants.CONSUME_MULTIPLE);
      } else {
        ItemDatabase.attributesById.set(itemId, attributes | ItemDatabase.ATTR_MULTIPLE);
      }
    } else {
      // We think the item is multi usable but it really is single usable
      if (useType == KoLConstants.CONSUME_MULTIPLE) {
        ItemDatabase.useTypeById.set(itemId, KoLConstants.CONSUME_USE);
      } else {
        ItemDatabase.attributesById.set(itemId, attributes | ItemDatabase.ATTR_USABLE);
      }
    }
  }

  private static void parseItemDescription(
      final Integer id, final String descId, int power, boolean multi) {
    int itemId = id.intValue();

    String rawText = DebugDatabase.rawItemDescriptionText(itemId);
    String text = DebugDatabase.itemDescriptionText(rawText);
    if (text == null) {
      // Assume defaults
      ItemDatabase.useTypeById.set(itemId, KoLConstants.NO_CONSUME);
      ItemDatabase.attributesById.set(itemId, 0);
      ItemDatabase.accessById.put(id, TRADE_FLAG + "," + DISCARD_FLAG);
      ItemDatabase.priceById.set(itemId, 0);
      return;
    }

    String itemName = DebugDatabase.parseName(text);

    String image = DebugDatabase.parseImage(rawText);
    ItemDatabase.imageById.set(itemId, image);

    // Parse use type, access, and price from description
    String type = DebugDatabase.parseType(text);
    int usage = DebugDatabase.typeToPrimary(type, multi);
    if (text.contains("blue\">Makes you look like")) {
      usage = KoLConstants.CONSUME_AVATAR;
    }
    ItemDatabase.useTypeById.set(itemId, usage);

    String access = DebugDatabase.parseAccess(text);
    ItemDatabase.accessById.put(id, access);

    int attrs = DebugDatabase.typeToSecondary(type, usage, text, multi);
    attrs |= access.contains(TRADE_FLAG) ? ItemDatabase.ATTR_TRADEABLE : 0;
    attrs |= access.contains(GIFT_FLAG) ? ItemDatabase.ATTR_GIFT : 0;
    attrs |= access.contains(QUEST_FLAG) ? ItemDatabase.ATTR_QUEST : 0;
    attrs |= access.contains(DISCARD_FLAG) ? ItemDatabase.ATTR_DISCARDABLE : 0;
    if (multi && usage != KoLConstants.CONSUME_MULTIPLE) {
      attrs |= ItemDatabase.ATTR_MULTIPLE;
    }
    ItemDatabase.attributesById.set(itemId, attrs);

    int price = DebugDatabase.parsePrice(text);
    ItemDatabase.priceById.set(itemId, price);
    // Intentionally get a null if there is not an explicit plural in the database
    String plural = ItemDatabase.getPluralById(itemId);

    String printMe;
    // Print what goes in items.txt
    printMe = "--------------------";
    RequestLogger.printLine(printMe);
    RequestLogger.updateSessionLog(printMe);

    printMe =
        ItemDatabase.itemString(
            itemId, itemName, descId, image, usage, attrs, access, price, plural);
    RequestLogger.printLine(printMe);
    RequestLogger.updateSessionLog(printMe);

    if (EquipmentDatabase.isEquipment(usage)) {
      EquipmentDatabase.newEquipment = true;

      // Get power from description, if otherwise unknown
      if (power == 0) {
        power = DebugDatabase.parsePower(text);
      }

      // Let equipment database do what it wishes with this item
      EquipmentDatabase.registerItem(itemId, itemName, text, power);
    } else if (usage == KoLConstants.CONSUME_EAT
        || usage == KoLConstants.CONSUME_DRINK
        || usage == KoLConstants.CONSUME_SPLEEN) {
      ConsumablesDatabase.registerConsumable(itemName, usage, text);
    }

    // Let modifiers database do what it wishes with this item
    Modifiers.registerItem(itemName, text, usage);

    // Done generating data
    printMe = "--------------------";
    RequestLogger.printLine(printMe);
    RequestLogger.updateSessionLog(printMe);

    // Equipment can be part of an outfit.
    EquipmentDatabase.registerItemOutfit(itemId, text);

    // Skillbooks teach you a skill
    String skillName = Modifiers.getStringModifier("Item", itemId, "Skill");
    if (!skillName.equals("") && SkillDatabase.getSkillId(skillName) == -1) {
      int skillId = DebugDatabase.parseSkillId(rawText);
      SkillDatabase.registerSkill(skillId, skillName);
    }

    // Potions grant an effect. Check for a new effect.
    String effectName = Modifiers.getStringModifier("Item", itemId, "Effect");
    if (!effectName.equals("") && EffectDatabase.getEffectId(effectName, true) == -1) {
      String effectDescid = DebugDatabase.parseEffectDescid(rawText);
      String command =
          usage == KoLConstants.CONSUME_EAT
              ? "eat 1 "
              : usage == KoLConstants.CONSUME_DRINK
                  ? "drink 1 "
                  : usage == KoLConstants.CONSUME_SPLEEN ? "chew 1 " : "use 1 ";
      EffectDatabase.registerEffect(effectName, effectDescid, command + itemName);
    }

    // Equipment can have a Rollover Effect. Check for new effect.
    effectName = Modifiers.getStringModifier("Item", itemId, "Rollover Effect");
    if (!effectName.equals("") && EffectDatabase.getEffectId(effectName, true) == -1) {
      String effectDescid = DebugDatabase.parseEffectDescid(rawText);
      EffectDatabase.registerEffect(effectName, effectDescid, null);
    }

    // Familiar larva mature into familiars.
    if (type.equals("familiar")) {
      FamiliarDatabase.registerFamiliar(id, text);
    }
  }

  public static void registerItemAlias(
      final int itemId, final String itemName, final String plural) {
    Integer id = IntegerPool.get(itemId);

    String canonical = StringUtilities.getCanonicalName(itemName);
    ItemDatabase.addIdToName(canonical, id);
    ItemDatabase.itemAliases.add(canonical);

    if (plural != null) {
      canonical = StringUtilities.getCanonicalName(plural);
      ItemDatabase.itemIdByPlural.put(canonical, id);
      ItemDatabase.pluralAliases.add(canonical);
    }

    ItemDatabase.saveCanonicalNames();
  }

  /**
   * Returns the Id number for an item, given its name.
   *
   * @param itemName The name of the item to lookup
   * @return The Id number of the corresponding item
   */
  public static final int getItemId(final String itemName) {
    return ItemDatabase.getItemId(itemName, 1, true);
  }

  /**
   * Returns the Id number for an item, given its name.
   *
   * @param itemName The name of the item to lookup
   * @param count How many there are
   * @return The Id number of the corresponding item
   */
  public static final int getItemId(final String itemName, final int count) {
    return getItemId(itemName, count, true);
  }

  /**
   * Returns the Id number for an item, given its name.
   *
   * @param itemName The name of the item to lookup
   * @param count How many there are
   * @param substringMatch Whether or not we match against substrings
   * @return The Id number of the corresponding item
   */
  public static final int getItemId(
      final String itemName, final int count, final boolean substringMatch) {
    int[] itemIds = getItemIds(itemName, count, substringMatch);
    int length = itemIds == null ? 0 : itemIds.length;

    if (length == 0) {
      return -1;
    }

    // *** There could be multiple. For backward compatibility,
    // *** return the last one.

    return itemIds[length - 1];
  }

  public static final int getExactItemId(final String itemName) {
    return ItemDatabase.getItemId(itemName, 1, false);
  }

  private static int getBracketedItemId(final String itemName) {
    if (itemName.startsWith("[")) {
      int index = itemName.indexOf("]");
      if (index > 0) {
        String idString = itemName.substring(1, index);
        if (StringUtilities.isNumeric(idString)) {
          int itemId = StringUtilities.parseInt(idString);
          // Allow item Id = 0 to mean "no item"
          if (itemId == 0 || ItemDatabase.getItemName(itemId) != null) {
            return itemId;
          }
        }
      }
    }
    return -1;
  }

  private static final int[] NO_ITEM_IDS = new int[0];

  public static final int[] getItemIds(
      final String itemName, final int count, final boolean substringMatch) {
    if (itemName == null) {
      return NO_ITEM_IDS;
    }

    // If name starts with [nnnn] then that is explicitly the item id
    if (itemName.startsWith("[")) {
      int itemId = ItemDatabase.getBracketedItemId(itemName);
      if (itemId != -1) {
        int[] ids = new int[1];
        ids[0] = itemId;
        return ids;
      }
    }
    String name = ItemDatabase.getCanonicalName(itemName, count, substringMatch);
    if (name == null) {
      return NO_ITEM_IDS;
    }

    int[] itemIds = ItemDatabase.itemIdSetByName.get(name);
    if (itemIds == null || itemIds.length == 0) {
      return NO_ITEM_IDS;
    }

    return itemIds;
  }

  public static String getDataName(final int itemId) {
    return ItemDatabase.getDataName(IntegerPool.get(itemId));
  }

  public static String getDataName(final Integer itemId) {
    return ItemDatabase.dataNameById.get(itemId);
  }

  public static String getDisplayName(final int itemId) {
    return ItemDatabase.getDisplayName(IntegerPool.get(itemId));
  }

  public static String getDisplayName(final Integer itemId) {
    return ItemDatabase.nameById.get(itemId);
  }

  public static String getCanonicalName(final int itemId) {
    return ItemDatabase.getCanonicalName(IntegerPool.get(itemId));
  }

  public static String getCanonicalName(final Integer itemId) {
    return StringUtilities.getCanonicalName(ItemDatabase.nameById.get(itemId));
  }

  public static final String getCanonicalName(final String itemName) {
    return ItemDatabase.getCanonicalName(itemName, 1);
  }

  public static final String getCanonicalName(final String itemName, final int count) {
    return ItemDatabase.getCanonicalName(itemName, count, true);
  }

  public static final String getCanonicalName(
      final String itemName, final int count, final boolean substringMatch) {
    if (itemName == null || itemName.length() == 0) {
      return null;
    }

    // Get the canonical name of the item, and attempt
    // to parse based on that.

    String canonicalName = StringUtilities.getCanonicalName(itemName);
    int itemId;

    // If name is specified by use of [xxxx], return CanonicalName
    if (itemName.startsWith("[")) {
      itemId = ItemDatabase.getBracketedItemId(itemName);
      if (itemId != -1) {
        return ItemDatabase.getCanonicalName((Integer) itemId);
      }
    }
    // See if it's a weird pluralization with a pattern we can't
    // guess before checking for singles.

    if (count > 1) {
      Integer id = ItemDatabase.itemIdByPlural.get(canonicalName);
      if (id != null) {
        return ItemDatabase.getCanonicalName(id);
      }
      itemId =
          ItemDatabase.getItemId(canonicalName.substring(0, canonicalName.length() - 1), 1, false);
      if (itemId != -1) {
        return ItemDatabase.getCanonicalName(itemId);
      }
    }

    int[] itemIds = ItemDatabase.itemIdSetByName.get(canonicalName);

    // If the name, as-is, exists in the item database, return it

    if (itemIds != null) {
      return canonicalName;
    }

    // Work around specific KoL bugs:

    // "less-than-three- shaped box" -> "less-than-three-shaped box"
    if (canonicalName.equals("less-than-three- shaped box")) {
      return "less-than-three-shaped box";
    }

    if (!substringMatch) {
      return null;
    }

    // It's possible that you're looking for a substring.  In
    // that case, prefer complete versions containing the substring
    // over truncated versions which are plurals.

    List<String> possibilities = ItemDatabase.getMatchingNames(canonicalName);
    int matches = possibilities.size();
    if (matches > 0) {
      String first = possibilities.get(0);

      // If only one match, we found it
      if (matches == 1) {
        return first;
      }

      // More than one match - but since the canonical name table
      // contains aliases, they might all be the same item.

      itemId = ItemDatabase.getExactItemId(first);
      for (int i = 1; i < matches; ++i) {
        int id = ItemDatabase.getExactItemId(possibilities.get(i));
        if (itemId != id) {
          itemId = -1;
          break;
        }
      }

      if (itemId != -1) {
        return first;
      }
    }

    // Abort if it's clearly not going to be a plural,
    // since this might kill off multi-item detection.

    if (count == 1) {
      return null;
    }

    // If it's a snowcone, then reverse the word order
    if (canonicalName.startsWith("snowcones")) {
      return ItemDatabase.getCanonicalName(canonicalName.split(" ")[1] + " snowcone", count);
    }

    // Lo mein has this odd pluralization where there's a dash
    // introduced into the name when no such dash exists in the
    // singular form.

    itemId =
        ItemDatabase.getExactItemId(StringUtilities.singleStringReplace(canonicalName, "-", " "));
    if (itemId != -1) {
      return ItemDatabase.getCanonicalName((Integer) itemId);
    }

    // The word right before the dash may also be pluralized,
    // so make sure the dashed words are recognized.

    itemId =
        ItemDatabase.getExactItemId(StringUtilities.singleStringReplace(canonicalName, "es-", "-"));
    if (itemId != -1) {
      return ItemDatabase.getCanonicalName((Integer) itemId);
    }

    itemId =
        ItemDatabase.getExactItemId(StringUtilities.singleStringReplace(canonicalName, "s-", "-"));
    if (itemId != -1) {
      return ItemDatabase.getCanonicalName((Integer) itemId);
    }

    // If it's a plural form of "tooth", then make
    // sure that it's handled.  Other things which
    // also have "ee" plural forms should be clumped
    // in as well.

    itemId =
        ItemDatabase.getExactItemId(StringUtilities.singleStringReplace(canonicalName, "ee", "oo"));
    if (itemId != -1) {
      return ItemDatabase.getCanonicalName((Integer) itemId);
    }

    // Also handle the plural of vortex, which is
    // "vortices" -- this should only appear in the
    // meat vortex, but better safe than sorry.

    itemId =
        ItemDatabase.getExactItemId(
            StringUtilities.singleStringReplace(canonicalName, "ices", "ex"));
    if (itemId != -1) {
      return ItemDatabase.getCanonicalName((Integer) itemId);
    }

    // Handling of appendices (which is the plural
    // of appendix, not appendex, so it is not caught
    // by the previous test).

    itemId =
        ItemDatabase.getExactItemId(
            StringUtilities.singleStringReplace(canonicalName, "ices", "ix"));
    if (itemId != -1) {
      return ItemDatabase.getCanonicalName((Integer) itemId);
    }

    // Also add in a special handling for knives
    // and other things ending in "ife".

    itemId =
        ItemDatabase.getExactItemId(
            StringUtilities.singleStringReplace(canonicalName, "ives", "ife"));
    if (itemId != -1) {
      return ItemDatabase.getCanonicalName((Integer) itemId);
    }

    // Also add in a special handling for elves
    // and other things ending in "f".

    itemId =
        ItemDatabase.getExactItemId(StringUtilities.singleStringReplace(canonicalName, "ves", "f"));
    if (itemId != -1) {
      return ItemDatabase.getCanonicalName((Integer) itemId);
    }

    // Also add in a special handling for staves
    // and other things ending in "aff".

    itemId =
        ItemDatabase.getExactItemId(
            StringUtilities.singleStringReplace(canonicalName, "aves", "aff"));
    if (itemId != -1) {
      return ItemDatabase.getCanonicalName((Integer) itemId);
    }

    // If it's a pluralized form of something that
    // ends with "y", then return the appropriate
    // item Id for the "y" version.

    if (canonicalName.endsWith("ies")) {
      itemId =
          ItemDatabase.getExactItemId(canonicalName.substring(0, canonicalName.length() - 3) + "y");
      if (itemId != -1) {
        return ItemDatabase.getCanonicalName((Integer) itemId);
      }
    }

    itemId =
        ItemDatabase.getExactItemId(
            StringUtilities.singleStringReplace(canonicalName, "ies ", "y "));
    if (itemId != -1) {
      return ItemDatabase.getCanonicalName((Integer) itemId);
    }

    // If it's a pluralized form of something that
    // ends with "o", then return the appropriate
    // item Id for the "o" version.

    if (canonicalName.endsWith("es")) {
      itemId = ItemDatabase.getExactItemId(canonicalName.substring(0, canonicalName.length() - 2));
      if (itemId != -1) {
        return ItemDatabase.getCanonicalName((Integer) itemId);
      }
    }

    itemId =
        ItemDatabase.getExactItemId(StringUtilities.singleStringReplace(canonicalName, "es ", " "));
    if (itemId != -1) {
      return ItemDatabase.getCanonicalName((Integer) itemId);
    }

    // If it's a pluralized form of something that
    // ends with "an", then return the appropriate
    // item Id for the "en" version.

    itemId =
        ItemDatabase.getExactItemId(
            StringUtilities.singleStringReplace(canonicalName, "en ", "an "));
    if (itemId != -1) {
      return ItemDatabase.getCanonicalName((Integer) itemId);
    }

    // If it's a standard pluralized forms, then
    // return the appropriate item Id.

    itemId = ItemDatabase.getExactItemId(canonicalName.replaceFirst("([A-Za-z])s ", "$1 "));
    if (itemId != -1) {
      return ItemDatabase.getCanonicalName((Integer) itemId);
    }

    // If it's something that ends with 'i', then
    // it might be a singular ending with 'us'.

    if (canonicalName.endsWith("i")) {
      itemId =
          ItemDatabase.getExactItemId(
              canonicalName.substring(0, canonicalName.length() - 1) + "us");
      if (itemId != -1) {
        return ItemDatabase.getCanonicalName((Integer) itemId);
      }
    }

    // Unknown item

    return null;
  }

  public static final int getNameLength(final int itemId) {
    return ItemDatabase.nameLength.get(itemId);
  }

  public static final String getPluralName(final String name) {
    int itemId = ItemDatabase.getItemId(name);
    if (itemId == -1) {
      return name + "s";
    }
    return ItemDatabase.getPluralName(itemId);
  }

  /**
   * Returns the plural for an item, given its Id number
   *
   * @param itemId The Id number of the item to lookup
   * @return The plural name of the corresponding item
   */
  public static final String getPluralName(final int itemId) {
    if (itemId == -1) {
      return "";
    }
    String plural = pluralById.get(itemId);
    if (plural == null || plural.equals("")) {
      plural = ItemDatabase.getItemDataName(itemId) + "s";
      // We could put() the generated plural back in the
      // map. Is that a good idea?
    }
    return plural;
  }

  public static final String getPluralById(final int itemId) {
    return pluralById.get(itemId);
  }

  public static final String getImage(final int itemId) {
    return imageById.get(itemId);
  }

  public static final String getSmallImage(final int itemId) {
    switch (itemId) {
      case ItemPool.FOLDER_01:
      case ItemPool.FOLDER_02:
      case ItemPool.FOLDER_03:
      case ItemPool.FOLDER_04:
      case ItemPool.FOLDER_05:
      case ItemPool.FOLDER_07:
      case ItemPool.FOLDER_09:
      case ItemPool.FOLDER_10:
      case ItemPool.FOLDER_12:
      case ItemPool.FOLDER_13:
      case ItemPool.FOLDER_24:
      case ItemPool.FOLDER_26:
        return "folder2.gif";
      case ItemPool.FOLDER_06:
      case ItemPool.FOLDER_08:
      case ItemPool.FOLDER_11:
      case ItemPool.FOLDER_14:
      case ItemPool.FOLDER_15:
      case ItemPool.FOLDER_16:
      case ItemPool.FOLDER_17:
      case ItemPool.FOLDER_18:
      case ItemPool.FOLDER_19:
      case ItemPool.FOLDER_20:
      case ItemPool.FOLDER_21:
      case ItemPool.FOLDER_22:
      case ItemPool.FOLDER_23:
      case ItemPool.FOLDER_25:
      case ItemPool.FOLDER_27:
      case ItemPool.FOLDER_28:
        return "folder1.gif";
      default:
        return imageById.get(itemId);
    }
  }

  public static final void setImage(final int itemId, final String image) {
    imageById.set(itemId, image);
  }

  public static final String getItemImageLocation(final int itemId) {
    String location = ItemDatabase.getImage(itemId);
    return (location != null) ? location : "debug.gif";
  }

  private static String getItemImagePath(final int itemId) {
    String image = ItemDatabase.getItemImageLocation(itemId);
    return image.contains("/") ? image : ("itemimages/" + image);
  }

  private static void downloadItemImage(final int itemId) {
    String path = ItemDatabase.getItemImagePath(itemId);
    ItemDatabase.downloadItemImage(path);
  }

  private static void downloadItemImage(final String path) {
    FileUtilities.downloadImage(KoLmafia.imageServerPath() + path);
  }

  public static final ImageIcon getItemImage(final int itemId) {
    String path = ItemDatabase.getItemImagePath(itemId);
    ItemDatabase.downloadItemImage(path);
    return JComponentUtilities.getImage(path);
  }

  public static final FoldGroup getFoldGroup(final String name) {
    if (name == null) {
      return null;
    }

    return ItemDatabase.foldGroupsByName.get(StringUtilities.getCanonicalName(name));
  }

  /**
   * Returns the price for the item with the given Id.
   *
   * @return The price associated with the item
   */
  public static final int getPriceById(final int itemId) {
    return ItemDatabase.priceById.get(itemId);
  }

  /**
   * Returns the access for the item with the given Id.
   *
   * @return The access associated with the item
   */
  public static final String getAccessById(final Integer itemId) {
    return ItemDatabase.accessById.get(itemId);
  }

  public static final int getAttributes(int itemId) {
    return ItemDatabase.attributesById.get(itemId);
  }

  public static final String attrsToSecondaryUsage(int attrs) {
    // Mask out attributes which are part of access
    attrs &= ~(ATTR_TRADEABLE | ATTR_GIFT | ATTR_QUEST | ATTR_DISCARDABLE);

    // If there are no other attributes, return empty string
    if (attrs == 0) {
      return "";
    }

    // Otherwise, iterate over bits
    StringBuilder result = new StringBuilder();
    Iterator<Entry<Integer, String>> it = ItemDatabase.secondaryUsageEntrySet.iterator();

    while (it.hasNext()) {
      Entry<Integer, String> entry = it.next();
      Integer bit = entry.getKey();

      if ((attrs & bit.intValue()) != 0) {
        result.append(", ");
        result.append(entry.getValue());
      }
    }

    return result.toString();
  }

  public static final boolean getAttribute(int itemId, int mask) {
    return (ItemDatabase.attributesById.get(itemId) & mask) != 0;
  }

  /**
   * Returns true if the item is a quest item, otherwise false
   *
   * @return true if item is a quest item
   */
  public static final boolean isQuestItem(final int itemId) {
    return ItemDatabase.getAttribute(itemId, ItemDatabase.ATTR_QUEST);
  }

  /**
   * Returns true if the item is a gift item, otherwise false
   *
   * @return true if item is a gift item
   */
  public static final boolean isGiftItem(final int itemId) {
    return ItemDatabase.getAttribute(itemId, ItemDatabase.ATTR_GIFT);
  }

  /**
   * Returns true if the item is a virtual item, as defined below, otherwise false
   *
   * @return true if item is a virtual item
   */
  public static final boolean isVirtualItem(final int itemId) {
    // * For these purposes, a virtual item is an item that has a non-negative
    // * item number in KoL but does not (can not?) exist in inventory.  These
    // * items can be obtained, for example from an NPC store, but they are
    // * "used" immediately.  Presently the only place the attribute is used is the
    // * ash function, is_displayable().  By definition, if it can't exist
    // * in inventory then it cannot be moved to a Display Case.
    switch (itemId) {
      case ItemPool.MADNESS_REEF_MAP:
      case ItemPool.MARINARA_TRENCH_MAP:
      case ItemPool.ANEMONE_MINE_MAP:
      case ItemPool.DIVE_BAR_MAP:
      case ItemPool.SKATE_PARK_MAP:
      case ItemPool.GLASS_OF_MILK:
      case ItemPool.CUP_OF_TEA:
      case ItemPool.THERMOS_OF_WHISKEY:
      case ItemPool.LUCKY_LINDY:
      case ItemPool.BEES_KNEES:
      case ItemPool.SOCKDOLLAGER:
      case ItemPool.ISH_KABIBBLE:
      case ItemPool.HOT_SOCKS:
      case ItemPool.PHONUS_BALONUS:
      case ItemPool.FLIVVER:
      case ItemPool.SLOPPY_JALOPY:
        return true;
    }
    return false;
  }

  public static final boolean haveVirtualItem(final int itemId) {
    switch (itemId) {
      case ItemPool.MADNESS_REEF_MAP:
        return Preferences.getBoolean("mapToMadnessReefPurchased");
      case ItemPool.MARINARA_TRENCH_MAP:
        return Preferences.getBoolean("mapToTheMarinaraTrenchPurchased");
      case ItemPool.ANEMONE_MINE_MAP:
        return Preferences.getBoolean("mapToAnemoneMinePurchased");
      case ItemPool.DIVE_BAR_MAP:
        return Preferences.getBoolean("mapToTheDiveBarPurchased");
      case ItemPool.SKATE_PARK_MAP:
        return Preferences.getBoolean("mapToTheSkateParkPurchased");
    }
    return false;
  }

  /**
   * Returns true if the item is tradeable, otherwise false
   *
   * @return true if item is tradeable
   */
  public static final boolean isTradeable(final int itemId) {
    return ItemDatabase.getAttribute(itemId, ItemDatabase.ATTR_TRADEABLE);
  }

  /**
   * Returns true if the item is giftable, otherwise false
   *
   * @return true if item is giftable
   */
  public static final boolean isGiftable(final int itemId) {
    return ItemDatabase.getAttribute(itemId, ItemDatabase.ATTR_TRADEABLE)
        || ItemDatabase.getAttribute(itemId, ItemDatabase.ATTR_GIFT);
  }

  /**
   * Returns true if the item is discardable, otherwise false
   *
   * @return true if item is discardable
   */
  public static final boolean isDiscardable(final int itemId) {
    return ItemDatabase.getAttribute(itemId, ItemDatabase.ATTR_DISCARDABLE);
  }

  /**
   * Returns true if the item is a bounty, otherwise false
   *
   * @return true if item is a bounty
   */
  public static final boolean isBountyItem(final int itemId) {
    return ItemDatabase.getAttribute(itemId, ItemDatabase.ATTR_BOUNTY);
  }

  /**
   * Returns true if the item is a Meat Pasting Component ingredient, otherwise false
   *
   * @return true if item is a Meat Pasting Component
   */
  public static final boolean isPasteable(final int itemId) {
    return ItemDatabase.getAttribute(itemId, ItemDatabase.ATTR_PASTE);
  }

  /**
   * Returns true if the item is a Meatsmithing Component, otherwise false
   *
   * @return true if item is a Meatsmithing Component
   */
  public static final boolean isSmithable(final int itemId) {
    return ItemDatabase.getAttribute(itemId, ItemDatabase.ATTR_SMITH);
  }

  /**
   * Returns true if the item is a Cooking Ingredient, otherwise false
   *
   * @return true if item is a Cooking Ingredient
   */
  public static final boolean isCookable(final int itemId) {
    return ItemDatabase.getAttribute(itemId, ItemDatabase.ATTR_COOK);
  }

  /**
   * Returns true if the item is a Cocktailcrafting Ingredient, otherwise false
   *
   * @return true if item is a Cocktailcrafting ingredient
   */
  public static final boolean isMixable(int itemId) {
    return ItemDatabase.getAttribute(itemId, ItemDatabase.ATTR_MIX);
  }

  /**
   * Returns true if the item is a fancy ingredient, otherwise false
   *
   * @return true if item is a fancy ingredient
   */
  public static final boolean isFancyItem(final int itemId) {
    return ItemDatabase.getAttribute(itemId, ItemDatabase.ATTR_FANCY);
  }

  /**
   * Returns true if the item is a candy, otherwise false
   *
   * @return true if item is a candy
   */
  public static final boolean isCandyItem(final int itemId) {
    return ItemDatabase.getAttribute(
        itemId, (ItemDatabase.ATTR_CANDY0 | ItemDatabase.ATTR_CANDY1 | ItemDatabase.ATTR_CANDY2));
  }

  /**
   * Returns true if the item is an adventure-granting chocolate, otherwise false
   *
   * @return true if item is a chocolate
   */
  public static final boolean isChocolateItem(final int itemId) {
    return ItemDatabase.getAttribute(itemId, ItemDatabase.ATTR_CHOCOLATE);
  }

  /**
   * Returns the name for an item, given its Id number.
   *
   * @param itemId The Id number of the item to lookup
   * @return The name of the corresponding item
   */
  public static final String getItemName(final int itemId) {
    return ItemDatabase.nameById.get(IntegerPool.get(itemId));
  }

  public static final String getItemDataName(final int itemId) {
    return ItemDatabase.dataNameById.get(IntegerPool.get(itemId));
  }

  public static final String getItemDataName(final Integer itemId) {
    return ItemDatabase.dataNameById.get(itemId);
  }

  public static final Set<Entry<Integer, String>> dataNameEntrySet() {
    return ItemDatabase.dataNameById.entrySet();
  }

  public static final String getItemDisplayName(final String itemName) {
    if (itemName.startsWith("[")) {
      int itemId = ItemDatabase.getBracketedItemId(itemName);
      if (itemId != -1) {
        return getItemName(itemId);
      }
    }
    return itemName;
  }
  /**
   * Returns the name for an item, given its description id number.
   *
   * @param descriptionId The description id number of the item to lookup
   * @return The name of the corresponding item
   */
  public static final String getItemName(final String descriptionId) {
    Integer itemId = ItemDatabase.itemIdByDescription.get(descriptionId);
    return itemId == null ? null : ItemDatabase.getItemName(itemId.intValue());
  }

  /**
   * Returns the id for an item, given its description id number.
   *
   * @param descriptionId The description id number of the item to lookup
   * @return The item id of the corresponding item
   */
  public static final int getItemIdFromDescription(final String descriptionId) {
    Integer itemId = ItemDatabase.itemIdByDescription.get(descriptionId);
    return itemId == null ? -1 : itemId.intValue();
  }

  /**
   * Returns a list of all items which contain the given substring. This is useful for people who
   * are doing lookups on items.
   */
  public static final List<String> getMatchingNames(final String substring) {
    return StringUtilities.getMatchingNames(ItemDatabase.canonicalNames, substring);
  }

  /**
   * Returns whether or not an item with a given name exists in the database; this is useful in the
   * event that an item is encountered which is not tradeable (and hence, should not be displayed).
   *
   * @return <code>true</code> if the item is in the database
   */
  public static final boolean contains(final String itemName) {
    return ItemDatabase.getItemId(itemName) != -1;
  }

  public static final boolean containsExactly(final String itemName) {
    return ItemDatabase.getExactItemId(itemName) != -1;
  }

  /**
   * Returns whether or not the item with the given name is usable (this includes edibility).
   *
   * @return <code>true</code> if the item is usable
   */
  public static final boolean isUsable(final int itemId) {
    // Anything that you can manipulate with inv_use.php

    int useType = ItemDatabase.useTypeById.get(itemId);
    int attributes = ItemDatabase.getAttributes(itemId);

    switch (useType) {
        // Explicit "use"
      case KoLConstants.CONSUME_USE:
      case KoLConstants.MESSAGE_DISPLAY:
      case KoLConstants.INFINITE_USES:
        // Multi-use
      case KoLConstants.CONSUME_MULTIPLE:
        // Grow is a type of use
      case KoLConstants.GROW_FAMILIAR:
        // Any potion
      case KoLConstants.CONSUME_POTION:
      case KoLConstants.CONSUME_AVATAR:
        return true;
      default:
        return (attributes
                & (ItemDatabase.ATTR_USABLE
                    | ItemDatabase.ATTR_MULTIPLE
                    | ItemDatabase.ATTR_REUSABLE))
            != 0;
    }
  }

  public static final boolean isPotion(final AdventureResult item) {
    if (item == null) {
      return false;
    }
    return ItemDatabase.isPotion(item.getItemId());
  }

  public static final boolean isPotion(final int itemId) {
    int useType = ItemDatabase.useTypeById.get(itemId);
    return (useType == KoLConstants.CONSUME_POTION || useType == KoLConstants.CONSUME_AVATAR);
  }

  public static final boolean isEquipment(final int itemId) {
    int useType = ItemDatabase.useTypeById.get(itemId);
    switch (useType) {
      case KoLConstants.EQUIP_ACCESSORY:
      case KoLConstants.EQUIP_CONTAINER:
      case KoLConstants.EQUIP_HAT:
      case KoLConstants.EQUIP_SHIRT:
      case KoLConstants.EQUIP_PANTS:
      case KoLConstants.EQUIP_WEAPON:
      case KoLConstants.EQUIP_OFFHAND:
      case KoLConstants.EQUIP_FAMILIAR:
        return true;
    }
    return false;
  }

  public static final boolean isFood(final int itemId) {
    int useType = ItemDatabase.useTypeById.get(itemId);
    return useType == KoLConstants.CONSUME_EAT;
  }

  public static final boolean isBooze(final int itemId) {
    int useType = ItemDatabase.useTypeById.get(itemId);
    return useType == KoLConstants.CONSUME_DRINK;
  }

  public static final boolean isHat(final int itemId) {
    int useType = ItemDatabase.useTypeById.get(itemId);
    return useType == KoLConstants.EQUIP_HAT;
  }

  public static final boolean isWeapon(final int itemId) {
    int useType = ItemDatabase.useTypeById.get(itemId);
    return useType == KoLConstants.EQUIP_WEAPON;
  }

  public static final boolean isOffHand(final int itemId) {
    int useType = ItemDatabase.useTypeById.get(itemId);
    return useType == KoLConstants.EQUIP_OFFHAND;
  }

  public static final boolean isShirt(final int itemId) {
    int useType = ItemDatabase.useTypeById.get(itemId);
    return useType == KoLConstants.EQUIP_SHIRT;
  }

  public static final boolean isPants(final int itemId) {
    int useType = ItemDatabase.useTypeById.get(itemId);
    return useType == KoLConstants.EQUIP_PANTS;
  }

  public static final boolean isAccessory(final int itemId) {
    int useType = ItemDatabase.useTypeById.get(itemId);
    return useType == KoLConstants.EQUIP_ACCESSORY;
  }

  public static final boolean isFamiliarEquipment(final int itemId) {
    int useType = ItemDatabase.useTypeById.get(itemId);
    return useType == KoLConstants.EQUIP_FAMILIAR;
  }

  public static final boolean isMultiUsable(final int itemId) {
    // Anything that you can manipulate with multiuse.php

    int useType = ItemDatabase.useTypeById.get(itemId);
    int attributes = ItemDatabase.getAttributes(itemId);

    switch (useType) {
      case KoLConstants.CONSUME_MULTIPLE:
        return true;
      case KoLConstants.CONSUME_POTION:
      case KoLConstants.CONSUME_AVATAR:
      case KoLConstants.CONSUME_SPLEEN:
        return (attributes & ItemDatabase.ATTR_USABLE) == 0;
      default:
        return (attributes & ItemDatabase.ATTR_MULTIPLE) != 0;
    }
  }

  public static final boolean isReusable(final int itemId) {
    int useType = ItemDatabase.useTypeById.get(itemId);
    int attributes = ItemDatabase.getAttributes(itemId);
    return useType == KoLConstants.INFINITE_USES || (attributes & ItemDatabase.ATTR_REUSABLE) != 0;
  }

  /**
   * Returns whether or not the item with the given name is made of grimacite and is thus affected
   * by the moon phases.
   *
   * @return <code>true</code> if the item is grimacite
   */
  public static final boolean isGrimacite(int itemId) {
    switch (itemId) {
        // Grimacite Generation 1
      case ItemPool.GRIMACITE_GALOSHES:
      case ItemPool.GRIMACITE_GARTER:
      case ItemPool.GRIMACITE_GORGET:
      case ItemPool.GRIMACITE_GREAVES:
      case ItemPool.GRIMACITE_GUAYABERA:
      case ItemPool.GRIMACITE_GOGGLES:
      case ItemPool.GRIMACITE_GLAIVE:
        // Grimacite Generation 2
      case ItemPool.GRIMACITE_GASMASK:
      case ItemPool.GRIMACITE_GAT:
      case ItemPool.GRIMACITE_GIRDLE:
      case ItemPool.GRIMACITE_GO_GO_BOOTS:
      case ItemPool.GRIMACITE_GAUNTLETS:
      case ItemPool.GRIMACITE_GAITERS:
      case ItemPool.GRIMACITE_GOWN:
        // Depleted Grimacite
      case ItemPool.GRIMACITE_HAMMER:
      case ItemPool.GRIMACITE_GRAVY_BOAT:
      case ItemPool.GRIMACITE_WEIGHTLIFTING_BELT:
      case ItemPool.GRIMACITE_GRAPPLING_HOOK:
      case ItemPool.GRIMACITE_NINJA_MASK:
      case ItemPool.GRIMACITE_SHINGUARDS:
      case ItemPool.GRIMACITE_ASTROLABE:
      case ItemPool.GRIMACITE_KNEECAPPING_STICK:
        return true;
    }

    return false;
  }

  public static final boolean isSealFigurine(final int itemId) {
    switch (itemId) {
      case ItemPool.WRETCHED_SEAL:
      case ItemPool.CUTE_BABY_SEAL:
      case ItemPool.ARMORED_SEAL:
      case ItemPool.ANCIENT_SEAL:
      case ItemPool.SLEEK_SEAL:
      case ItemPool.SHADOWY_SEAL:
      case ItemPool.STINKING_SEAL:
      case ItemPool.CHARRED_SEAL:
      case ItemPool.COLD_SEAL:
      case ItemPool.SLIPPERY_SEAL:
      case ItemPool.DEPLETED_URANIUM_SEAL:
        return true;
    }
    return false;
  }

  public static final boolean isBRICKOMonster(final int itemId) {
    switch (itemId) {
      case ItemPool.BRICKO_OOZE:
      case ItemPool.BRICKO_BAT:
      case ItemPool.BRICKO_OYSTER:
      case ItemPool.BRICKO_TURTLE:
      case ItemPool.BRICKO_ELEPHANT:
      case ItemPool.BRICKO_OCTOPUS:
      case ItemPool.BRICKO_PYTHON:
      case ItemPool.BRICKO_VACUUM_CLEANER:
      case ItemPool.BRICKO_AIRSHIP:
      case ItemPool.BRICKO_CATHEDRAL:
      case ItemPool.BRICKO_CHICKEN:
        return true;
    }
    return false;
  }

  public static final boolean isStinkyCheeseItem(final int itemId) {
    switch (itemId) {
      case ItemPool.STINKY_CHEESE_SWORD:
      case ItemPool.STINKY_CHEESE_DIAPER:
      case ItemPool.STINKY_CHEESE_WHEEL:
      case ItemPool.STINKY_CHEESE_EYE:
      case ItemPool.STINKY_CHEESE_STAFF:
        return true;
    }
    return false;
  }

  /**
   * Returns the kind of consumption associated with an item
   *
   * @return The consumption associated with the item
   */
  public static final int getConsumptionType(final int itemId) {
    return itemId <= 0 ? KoLConstants.NO_CONSUME : ItemDatabase.useTypeById.get(itemId);
  }

  public static final int getConsumptionType(final AdventureResult item) {
    return ItemDatabase.getConsumptionType(item.getItemId());
  }

  public static final String typeToPrimaryUsage(final int type) {
    return ItemDatabase.INVERSE_PRIMARY_USE.get(IntegerPool.get(type));
  }

  /**
   * Returns the item description Id used by the given item, given its item Id.
   *
   * @return The description Id associated with the item
   */
  public static final String getDescriptionId(final int itemId) {
    return ItemDatabase.getDescriptionId(IntegerPool.get(itemId));
  }

  public static final String getDescriptionId(final Integer itemId) {
    return ItemDatabase.descriptionById.get(itemId);
  }

  public static final Set<Integer> nameByIdKeySet() {
    return ItemDatabase.nameById.keySet();
  }

  public static final Set<Integer> descriptionIdKeySet() {
    return ItemDatabase.descriptionById.keySet();
  }

  public static final Set<Entry<Integer, String>> descriptionIdEntrySet() {
    return ItemDatabase.descriptionById.entrySet();
  }

  /**
   * Returns the set of item names keyed by id
   *
   * @return The set of item names keyed by id
   */
  public static final Set<Entry<Integer, String>> entrySet() {
    return ItemDatabase.nameById.entrySet();
  }

  /**
   * Returns the largest item ID
   *
   * @return The largest item ID
   */
  public static final int maxItemId() {
    return ItemDatabase.maxItemId;
  }

  private static final Pattern COT_PATTERN =
      Pattern.compile("Current Occupant:.*?<b>.* the (.*?)</b>");

  public static void parseCrownOfThrones(final String desc) {
    Matcher matcher = ItemDatabase.COT_PATTERN.matcher(desc);
    if (matcher.find()) {
      String race = matcher.group(1);
      KoLCharacter.setEnthroned(KoLCharacter.findFamiliar(race));
    }
  }

  public static void parseBuddyBjorn(final String desc) {
    // COT_PATTERN works for this
    Matcher matcher = ItemDatabase.COT_PATTERN.matcher(desc);
    if (matcher.find()) {
      String race = matcher.group(1);
      KoLCharacter.setBjorned(KoLCharacter.findFamiliar(race));
    }
  }

  public static void parseSaber(final String desc) {
    if (desc.contains("15-20 MP")) {
      Preferences.setInteger("_saberMod", 1);
    } else if (desc.contains("Monster Level")) {
      Preferences.setInteger("_saberMod", 2);
    } else if (desc.contains("Serious")) {
      Preferences.setInteger("_saberMod", 3);
    } else if (desc.contains("Familiar Weight")) {
      Preferences.setInteger("_saberMod", 4);
    }
  }

  public static void resetVampireVintnerWine() {
    Preferences.setString("vintnerWineName", "");
    Preferences.setString("vintnerWineEffect", "");
    Preferences.setInteger("vintnerWineLevel", 0);
    Preferences.setString("vintnerWineType", "");
  }

  public static void parseVampireVintnerWine() {
    // Call desc_item.php for 1950 Vampire Vintner wine
    String idesc = DebugDatabase.itemDescriptionText(ItemPool.VAMPIRE_VINTNER_WINE, true);

    // GenericRequest calls ResponseTextParser which makes the following call.
    // No reason to parse the response text twice!

    // ItemDatabase.parseVampireVintnerWine(idesc);
  }

  public static void parseVampireVintnerWine(final String idesc) {
    String iEnchantments =
        DebugDatabase.parseItemEnchantments(
            idesc, new ArrayList<String>(), KoLConstants.CONSUME_DRINK);
    String iname = DebugDatabase.parseName(idesc);
    Modifiers imods = Modifiers.parseModifiers(iname, iEnchantments);

    // Validate this by seeing what effect this wine grants.
    String effectName = imods.getString("Effect");
    int effectId = EffectDatabase.getEffectId(effectName);

    // If it doesn't grant one, this is the generic 1950 Vampire Vintner wine
    if (effectId == -1) {
      ItemDatabase.resetVampireVintnerWine();
      return;
    }

    // The damage type that created this wine is implied by the effect the wine grants.
    String type = "";

    switch (effectId) {
      case EffectPool.WINE_FORTIFIED:
        type = "physical";
        break;
      case EffectPool.WINE_HOT:
        type = "hot";
        break;
      case EffectPool.WINE_COLD:
        type = "cold";
        break;
      case EffectPool.WINE_DARK:
        type = "spooky";
        break;
      case EffectPool.WINE_BEFOULED:
        type = "stench";
        break;
      case EffectPool.WINE_FRISKY:
        type = "sleaze";
        break;
      case EffectPool.WINE_FRIENDLY:
        type = "familiar";
        break;
    }

    Preferences.setString("vintnerWineName", iname);
    Preferences.setString("vintnerWineEffect", effectName);
    Preferences.setString("vintnerWineType", type);

    // Look up the description of the the effect. ResponseTextParser will
    // examine it and set the vintnerWineLevel property
    DebugDatabase.readEffectDescriptionText(effectId);

    // Override the modifiers for the 1950 Vampire Vintner wine to include the
    // effect that drinking this one will provide.
    Modifiers.overrideModifier(Modifiers.getLookupName("Item", "1950 Vampire Vintner wine"), imods);
  }

  public static int parseYearbookCamera(final String desc) {
    int upgrades;
    if (desc.contains("Blinding")) {
      upgrades = 21;
    } else if (desc.contains("Viewfinder")) {
      upgrades = 20;
    } else if (desc.contains("Light")) {
      upgrades = 19;
    } else if (desc.contains("Shutter")) {
      upgrades = 18;
    } else if (desc.contains("Polarizing")) {
      upgrades = 17;
    } else if (desc.contains("Case")) {
      upgrades = 16;
    } else if (desc.contains("Tilt")) {
      upgrades = 15;
    } else if (desc.contains("Release")) {
      upgrades = 14;
    } else if (desc.contains("Batteries")) {
      upgrades = 13;
    } else if (desc.contains("Tripod")) {
      upgrades = 12;
    } else if (desc.contains("Strap")) {
      upgrades = 11;
    } else if (desc.contains("Grip")) {
      upgrades = 10;
    } else if (desc.contains("800mm")) {
      upgrades = 9;
    } else if (desc.contains("20mm")) {
      upgrades = 8;
    } else if (desc.contains("2:1")) {
      upgrades = 7;
    } else if (desc.contains("400mm")) {
      upgrades = 6;
    } else if (desc.contains("24mm")) {
      upgrades = 5;
    } else if (desc.contains("1:1")) {
      upgrades = 4;
    } else if (desc.contains("200m")) {
      upgrades = 3;
    } else if (desc.contains("28mm")) {
      upgrades = 2;
    } else if (desc.contains("1:2")) {
      upgrades = 1;
    } else {
      upgrades = 0;
    }

    Preferences.setInteger("yearbookCameraUpgrades", upgrades);

    return upgrades;
  }

  public static void parseRetroCape(final String desc) {
    String superhero = null;

    if (desc.contains("Vampire Slicer")) {
      superhero = "vampire";
    } else if (desc.contains("Heck General")) {
      superhero = "heck";
    } else if (desc.contains("Robot Police")) {
      superhero = "robot";
    }

    String washingInstructions = null;

    if (desc.contains("Hold Me")) {
      washingInstructions = "hold";
    } else if (desc.contains("Thrill Me")) {
      washingInstructions = "thrill";
    } else if (desc.contains("Kiss Me")) {
      washingInstructions = "kiss";
    } else if (desc.contains("Kill Me")) {
      washingInstructions = "kill";
    }

    if (superhero != null) {
      Preferences.setString("retroCapeSuperhero", superhero);
    }

    if (washingInstructions != null) {
      Preferences.setString("retroCapeWashingInstructions", washingInstructions);
    }

    ItemDatabase.setCapeSkills();
  }

  public static void setCapeSkills() {
    // Assume no skills are available
    KoLCharacter.removeAvailableSkill("Smooch of the Daywalker");
    KoLCharacter.removeAvailableSkill("Slay the Dead");
    KoLCharacter.removeAvailableSkill("Unleash the Devil's Kiss");
    KoLCharacter.removeAvailableSkill("Deploy Robo-Handcuffs");
    KoLCharacter.removeAvailableSkill("Blow a Robo-Kiss");
    KoLCharacter.removeAvailableSkill("Precision Shot");

    // If the cape is not equipped, that is correct
    if (!KoLCharacter.hasEquipped(
        ItemPool.KNOCK_OFF_RETRO_SUPERHERO_CAPE, EquipmentManager.CONTAINER)) {
      return;
    }

    String superhero = Preferences.getString("retroCapeSuperhero");
    String instructions = Preferences.getString("retroCapeWashingInstructions");

    // It is equipped. Add available skills
    switch (superhero) {
      case "vampire":
        // Add Vampire Slicer skills
        if (instructions.equals("kiss")) {
          KoLCharacter.addAvailableSkill("Smooch of the Daywalker");
        }

        if (instructions.equals("kill") && EquipmentManager.wieldingSword()) {
          KoLCharacter.addAvailableSkill("Slay the Dead");
        }
        break;
      case "heck":
        // Add Heck General skills
        if (instructions.equals("kiss")) {
          KoLCharacter.addAvailableSkill("Unleash the Devil's Kiss");
        }
        break;
      case "robot":
        // Add Robot Police skills
        if (instructions.equals("hold")) {
          KoLCharacter.addAvailableSkill("Deploy Robo-Handcuffs");
        }
        if (instructions.equals("kiss")) {
          KoLCharacter.addAvailableSkill("Blow a Robo-Kiss");
        }
        if (instructions.equals("kill") && EquipmentManager.wieldingGun()) {
          KoLCharacter.addAvailableSkill("Precision Shot");
        }
        break;
    }
  }

  public static boolean unusableInBeecore(final int itemId) {
    switch (itemId) {
      case ItemPool.BALL_POLISH:
      case ItemPool.FRATHOUSE_BLUEPRINTS:
      case ItemPool.COBBS_KNOB_MAP:
      case ItemPool.BINDER_CLIP:
        // These "B" items ARE usable in Beecore.
      case ItemPool.ICE_BABY:
      case ItemPool.JUGGLERS_BALLS:
      case ItemPool.EYEBALL_PENDANT:
      case ItemPool.SPOOKY_PUTTY_BALL:
      case ItemPool.LOATHING_LEGION_ABACUS:
      case ItemPool.LOATHING_LEGION_DEFIBRILLATOR:
      case ItemPool.LOATHING_LEGION_DOUBLE_PRISM:
      case ItemPool.LOATHING_LEGION_ROLLERBLADES:
        // And so are these IOTM foldables
      case ItemPool.ENCHANTED_BEAN:
        // "using" this is really planting
        return false;
    }

    return KoLCharacter.hasBeeosity(ItemDatabase.getItemName(itemId));
  }

  public static boolean unusableInGLover(final int itemId) {
    switch (itemId) {
        // More stuff might belong here
      case ItemPool.COBBS_KNOB_MAP:
      case ItemPool.ENCHANTED_BEAN:
      case ItemPool.PALINDROME_BOOK_1:
      case ItemPool.PALINDROME_BOOK_2:
        return false;
    }
    String itemName = ItemDatabase.getItemName(itemId);

    // Can eat/drink things in cafes, even if they have G's*
    if (KoLConstants.restaurantItems.contains(itemName)
        || KoLConstants.microbreweryItems.contains(itemName)
        || KoLConstants.cafeItems.contains(itemName)
        || ClanLoungeRequest.isSpeakeasyDrink(itemName)) {
      return false;
    }
    return !KoLCharacter.hasGs(ItemDatabase.getItemName(itemId));
  }

  public static boolean usableOnlyAsPlumber(final int itemId) {
    switch (itemId) {
      case ItemPool.MUSHROOM:
      case ItemPool.DELUXE_MUSHROOM:
      case ItemPool.SUPER_DELUXE_MUSHROOM:
        return true;
      default:
        return false;
    }
  }

  public static boolean isAllowed(final int itemId) {
    return StandardRequest.isAllowed("Items", ItemDatabase.getDataName(itemId));
  }

  public static boolean isAllowedInStandard(final int itemId) {
    return StandardRequest.isAllowedInStandard("Items", ItemDatabase.getDataName(itemId));
  }

  public static int getNoobSkillId(final int itemId) {
    return ItemDatabase.noobSkillIdByItemSource.get(itemId);
  }

  public static int[] getItemListByNoobSkillId(final int skillId) {
    return ItemDatabase.itemSourceByNoobSkillId.get(skillId);
  }
}
