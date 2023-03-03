package net.sourceforge.kolmafia.persistence;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
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
import net.sourceforge.kolmafia.KoLConstants.ConsumptionType;
import net.sourceforge.kolmafia.KoLConstants.CraftingType;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.ModifierType;
import net.sourceforge.kolmafia.Modifiers;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.RestrictedItemType;
import net.sourceforge.kolmafia.StaticEntity;
import net.sourceforge.kolmafia.VYKEACompanionData;
import net.sourceforge.kolmafia.equipment.Slot;
import net.sourceforge.kolmafia.modifiers.ModifierList;
import net.sourceforge.kolmafia.modifiers.StringModifier;
import net.sourceforge.kolmafia.objectpool.Concoction;
import net.sourceforge.kolmafia.objectpool.ConcoctionPool;
import net.sourceforge.kolmafia.objectpool.EffectPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.objectpool.SkillPool;
import net.sourceforge.kolmafia.persistence.ConsumablesDatabase.DustyBottle;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.ApiRequest;
import net.sourceforge.kolmafia.request.ClanLoungeRequest;
import net.sourceforge.kolmafia.request.StandardRequest;
import net.sourceforge.kolmafia.request.SushiRequest;
import net.sourceforge.kolmafia.request.UmbrellaRequest.UmbrellaMode;
import net.sourceforge.kolmafia.session.ElVibratoManager;
import net.sourceforge.kolmafia.session.ElVibratoManager.Punchcard;
import net.sourceforge.kolmafia.session.EquipmentManager;
import net.sourceforge.kolmafia.utilities.FileUtilities;
import net.sourceforge.kolmafia.utilities.LogStream;
import net.sourceforge.kolmafia.utilities.StringUtilities;
import org.json.JSONException;
import org.json.JSONObject;

public class ItemDatabase {
  private static int maxItemId = 0;

  private static String[] canonicalNames = new String[0];
  private static final Map<Integer, ConsumptionType> useTypeById = new HashMap<>();
  private static final Map<Integer, EnumSet<Attribute>> attributesById = new HashMap<>();
  private static final Map<Integer, Integer> priceById = new HashMap<>();
  private static final Map<Integer, Integer> nameLength = new HashMap<>();
  private static final Map<Integer, String> pluralById = new HashMap<>();
  private static final Map<Integer, String> imageById = new HashMap<>();

  private static final Map<Integer, String> nameById = new TreeMap<>();
  private static final Map<Integer, String> dataNameById = new HashMap<>();
  private static final Map<Integer, String> descriptionById = new TreeMap<>();
  private static final Map<String, int[]> itemIdSetByName = new HashMap<>();
  private static final ArrayList<String> itemAliases = new ArrayList<>();
  private static final ArrayList<String> pluralAliases = new ArrayList<>();
  private static final Map<String, Integer> itemIdByPlural = new HashMap<>();

  private static final Map<String, Integer> itemIdByDescription = new HashMap<>();
  private static final Map<String, FoldGroup> foldGroupsByName = new HashMap<>();

  private static final Map<Integer, int[]> itemSourceByNoobSkillId = new HashMap<>();
  private static final Map<Integer, Integer> noobSkillIdByItemSource = new HashMap<>();

  private record Alias(int id, String name) {}

  private static final Alias[] ALIASES = {
    new Alias(ItemPool.BUGGED_BONNET, "bugged bonnet"),
    new Alias(ItemPool.BUGGED_MEAT_CLUB, "bugged meat stabbing club"),
    new Alias(ItemPool.BUGGED_POTION, "bugged Knob Goblin love potion"),
    new Alias(ItemPool.BUGGED_KNICKERBOCKERS, "bugged old school Mafia knickerbockers"),
    new Alias(ItemPool.BUGGED_BAIO, "bugged Talisman of Baio"),
    new Alias(ItemPool.UNBREAKABLE_UMBRELLA, "unbreakable umbrella (broken)"),
    new Alias(ItemPool.UNBREAKABLE_UMBRELLA, "unbreakable umbrella (forward-facing)"),
    new Alias(ItemPool.UNBREAKABLE_UMBRELLA, "unbreakable umbrella (bucket style)"),
    new Alias(ItemPool.UNBREAKABLE_UMBRELLA, "unbreakable umbrella (pitchfork style)"),
    new Alias(ItemPool.UNBREAKABLE_UMBRELLA, "unbreakable umbrella (constantly twirling)"),
    new Alias(ItemPool.UNBREAKABLE_UMBRELLA, "unbreakable umbrella (cocoon)"),
    new Alias(ItemPool.JURASSIC_PARKA, "Jurassic Parka (kachungasaur mode)"),
    new Alias(ItemPool.JURASSIC_PARKA, "Jurassic Parka (dilophosaur mode)"),
    new Alias(ItemPool.JURASSIC_PARKA, "Jurassic Parka (spikolodon mode)"),
    new Alias(ItemPool.JURASSIC_PARKA, "Jurassic Parka (ghostasaurus mode)"),
    new Alias(ItemPool.JURASSIC_PARKA, "Jurassic Parka (pterodactyl mode)"),
    new Alias(ItemPool.BACKUP_CAMERA, "backup camera (meat)"),
    new Alias(ItemPool.BACKUP_CAMERA, "backup camera (init)"),
    new Alias(ItemPool.BACKUP_CAMERA, "backup camera (ml)"),
    new Alias(-1, "potion of inebriety"),
    new Alias(-1, "potion of healing"),
    new Alias(-1, "potion of confusion"),
    new Alias(-1, "potion of blessing"),
    new Alias(-1, "potion of detection"),
    new Alias(-1, "potion of sleepiness"),
    new Alias(-1, "potion of mental acuity"),
    new Alias(-1, "potion of ettin strength"),
    new Alias(-1, "potion of teleportitis"),
    new Alias(-1, "vial of slime: strong"),
    new Alias(-1, "vial of slime: sagacious"),
    new Alias(-1, "vial of slime: speedy"),
    new Alias(-1, "vial of slime: brawn"),
    new Alias(-1, "vial of slime: brains"),
    new Alias(-1, "vial of slime: briskness"),
    new Alias(-1, "vial of slime: slimeform"),
    new Alias(-1, "vial of slime: eyesight"),
    new Alias(-1, "vial of slime: intensity"),
    new Alias(-1, "vial of slime: muscle"),
    new Alias(-1, "vial of slime: mentalism"),
    new Alias(-1, "vial of slime: moxiousness"),
  };

  private static final EnumSet<Attribute> ACCESS =
      EnumSet.of(Attribute.QUEST, Attribute.GIFT, Attribute.TRADEABLE, Attribute.DISCARDABLE);

  private ItemDatabase() {}

  private static String parseAccess(final String data) {
    if (data.equals("")) {
      return "";
    }

    String[] accessTypes = data.split("\\s*,\\s*");
    for (String accessType : accessTypes) {
      if (!ACCESS.contains(Attribute.byDescription(accessType))) {
        throw new IllegalStateException("Data file contained unrecognised flag");
      }
    }
    return data;
  }

  private static final Map<Integer, String> accessById = new HashMap<>();

  public enum Attribute {
    QUEST("q"),
    GIFT("g"),
    TRADEABLE("t"),
    DISCARDABLE("d"),

    COMBAT("combat"),
    COMBAT_REUSABLE("combat reusable"),

    USABLE("usable"),
    MULTIPLE("multiple"),
    REUSABLE("reusable"),

    SINGLE("single"),
    SOLO("solo"),

    CURSE("curse"),
    BOUNTY("bounty"),
    CANDY0("candy"),
    CANDY1("candy1"),
    CANDY2("candy2"),
    MATCHABLE("matchable"),
    FANCY("fancy"),
    CHOCOLATE("chocolate"),
    PASTE("paste"),
    SMITH("smith"),
    COOK("cook"),
    MIX("mix");

    public final String description;
    private static final Map<String, Attribute> attributeByDescription = new HashMap<>();

    Attribute(String description) {
      this.description = description;
    }

    public static Attribute byDescription(String description) {
      var lookup = attributeByDescription.get(description);
      if (lookup != null) return lookup;
      var search =
          Arrays.stream(Attribute.values())
              .filter(x -> x.description.equals(description))
              .findAny();
      search.ifPresent(x -> attributeByDescription.put(description, x));
      return search.orElse(null);
    }

    @Override
    public String toString() {
      return this.description;
    }
  }

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
    try (BufferedReader reader =
        FileUtilities.getVersionedReader("items.txt", KoLConstants.ITEMS_VERSION)) {

      String[] data;

      while ((data = FileUtilities.readData(reader)) != null) {
        if (data.length < 7) {
          continue;
        }

        int itemId = StringUtilities.parseInt(data[0]);
        String canonicalName = StringUtilities.getCanonicalName(data[1]);
        ItemDatabase.addIdToName(canonicalName, itemId);
      }
    } catch (IOException e) {
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
    try (BufferedReader reader =
        FileUtilities.getVersionedReader("items.txt", KoLConstants.ITEMS_VERSION)) {
      String[] data;

      while ((data = FileUtilities.readData(reader)) != null) {
        if (data.length < 7) {
          continue;
        }

        int itemId = StringUtilities.parseInt(data[0]);
        Integer id = itemId;

        String name = data[1];
        String displayName = StringUtilities.getDisplayName(name);
        String canonicalName = StringUtilities.getCanonicalName(name);

        String descId = data[2];
        if (StringUtilities.isNumeric(descId)) {
          ItemDatabase.descriptionById.put(id, descId);
          ItemDatabase.itemIdByDescription.put(descId, id);
        }

        String image = data[3];
        ItemDatabase.imageById.put(itemId, image);

        String[] usages = data[4].split("\\s*,\\s*");
        String access = ItemDatabase.parseAccess(data[5]);
        int price = StringUtilities.parseInt(data[6]);

        String usage = usages[0];
        ConsumptionType useType = ConsumptionType.byDescription(usage);
        if (useType == null) {
          RequestLogger.printLine("Unknown primary usage for " + name + ": " + usage);
        } else {
          ItemDatabase.useTypeById.put(itemId, useType);
        }

        EnumSet<Attribute> attrs = EnumSet.noneOf(Attribute.class);
        for (int i = 1; i < usages.length; ++i) {
          usage = usages[i];
          Attribute secUse = Attribute.byDescription(usage);
          if (secUse == null) {
            RequestLogger.printLine("Unknown secondary usage for " + name + ": " + usage);
          } else {
            attrs.add(secUse);
            CandyDatabase.registerCandy(id, secUse);
          }
        }

        ItemDatabase.priceById.put(itemId, price);
        ItemDatabase.dataNameById.put(id, name);
        ItemDatabase.nameById.put(id, displayName);

        ItemDatabase.accessById.put(id, access);
        for (Attribute a : ACCESS) {
          if (access.contains(a.description)) attrs.add(a);
        }

        ItemDatabase.attributesById.put(itemId, attrs);

        if (itemId > ItemDatabase.maxItemId) {
          ItemDatabase.maxItemId = itemId;
        }

        ItemDatabase.addIdToName(canonicalName, itemId);

        ItemDatabase.nameLength.put(itemId, displayName.length());

        if (data.length == 8) {
          String plural = data[7];
          ItemDatabase.pluralById.put(itemId, plural);
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
          int skillId =
              switch (itemId) {
                  // Override Robortender items
                case ItemPool.NOVELTY_HOT_SAUCE -> SkillPool.FROWN_MUSCLES;
                case ItemPool.COCKTAIL_MUSHROOM -> SkillPool.RETRACTABLE_TOES;
                case ItemPool.GRANOLA_LIQUEUR -> SkillPool.INK_GLAND;
                case ItemPool.GREGNADIGNE -> SkillPool.BENDABLE_KNEES;
                case ItemPool.BABY_OIL_SHOOTER -> SkillPool.POWERFUL_VOCAL_CHORDS;
                case ItemPool.LIMEPATCH -> SkillPool.ANGER_GLANDS;
                default -> (intDescId % 125) + 23001;
              };
          ItemDatabase.addIdToNoobSkill(skillId, itemId);
          ItemDatabase.noobSkillIdByItemSource.put(itemId, skillId);
        }
      }
    } catch (IOException e) {
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
      ConsumptionType type = ItemDatabase.getConsumptionType(itemId);
      EnumSet<Attribute> attrs = ItemDatabase.getAttributes(itemId);
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
      final ConsumptionType type,
      final EnumSet<Attribute> attrs,
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
    try (BufferedReader reader =
        FileUtilities.getVersionedReader("foldgroups.txt", KoLConstants.FOLDGROUPS_VERSION)) {
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
    } catch (IOException e) {
      StaticEntity.printStackTrace(e);
    }
  }

  private static void addPseudoItems() {
    Integer id = 13;

    ItemDatabase.dataNameById.put(id, "worthless item");
    ItemDatabase.nameById.put(id, "worthless item");
    ItemDatabase.addIdToName("worthless item", id);

    // Set aliases for the dusty bottles
    for (DustyBottle dusty : ConsumablesDatabase.DUSTY_BOTTLES) {
      id = dusty.id();
      String name = StringUtilities.getCanonicalName(dusty.name());
      String alias = StringUtilities.getCanonicalName(dusty.alias());
      String plural = StringUtilities.singleStringReplace(alias, "bottle", "bottles");
      ItemDatabase.addIdToName(alias, id);
      ItemDatabase.itemIdByPlural.put(plural, id);
      ConsumablesDatabase.cloneConsumptionData(name, alias);
    }

    // Set aliases for the El Vibrato punch cards
    for (Punchcard punchcard : ElVibratoManager.PUNCHCARDS) {
      id = punchcard.id();
      String alias = StringUtilities.getCanonicalName(punchcard.alias());
      String plural = StringUtilities.singleStringReplace(alias, "punchcard", "punchcards");
      ItemDatabase.addIdToName(alias, id);
      ItemDatabase.itemIdByPlural.put(plural, id);
    }

    // Add names of all the sushi
    id = -1;
    for (String sushi : SushiRequest.SUSHI) {
      String name = StringUtilities.getCanonicalName(sushi);
      ItemDatabase.addIdToName(name, id);
    }

    // Add names of all the VYKEA companions
    id = -1;
    for (String VYKEA : VYKEACompanionData.VYKEA) {
      String name = StringUtilities.getCanonicalName(VYKEA);
      ItemDatabase.addIdToName(name, id);
    }

    // Miscellaneous aliases for untypeable item names
    for (Alias alias : ItemDatabase.ALIASES) {
      id = alias.id;
      String name = StringUtilities.getCanonicalName(alias.name);
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
      // This will register new items if they are unknown
      ItemDatabase.lookupItemIdFromDescription(m.group(1));
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

  public static int registerItem(String descId) {
    // Pull the itemId and itemName from the item description, which will be cached
    String text =
        DebugDatabase.itemDescriptionText(DebugDatabase.rawItemDescriptionText(descId, true));
    if (text == null) {
      return -1;
    }

    int itemId = DebugDatabase.parseItemId(text);

    // Link this itemId and descId
    Integer id = itemId;
    ItemDatabase.descriptionById.put(id, descId);
    ItemDatabase.itemIdByDescription.put(descId, id);

    String itemName = DebugDatabase.parseName(text);

    ItemDatabase.registerItem(itemId, itemName, descId, null, 0, false);

    return itemId;
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
      switch (tag) {
        case "id" -> itemId = StringUtilities.parseInt(value);
        case "n" -> count = StringUtilities.parseInt(value);
        case "m" -> multi = value.equals("1");
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

    Integer id = itemId;

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
      ItemDatabase.addIdToNoobSkill(skillId, itemId);
      ItemDatabase.noobSkillIdByItemSource.put(itemId, skillId);
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
    ItemDatabase.pluralById.put(itemId, plural);
    ItemDatabase.itemIdByPlural.put(StringUtilities.getCanonicalName(plural), itemId);
  }

  public static final void registerMultiUsability(final int itemId, final boolean multi) {
    ConsumptionType useType = ItemDatabase.useTypeById.getOrDefault(itemId, ConsumptionType.NONE);
    EnumSet<Attribute> attributes = ItemDatabase.getAttributes(itemId);

    if (multi) {
      // We think the item is single usable but it really is multiusable
      if (useType == ConsumptionType.USE) {
        ItemDatabase.useTypeById.put(itemId, ConsumptionType.USE_MULTIPLE);
      } else {
        attributes.add(Attribute.MULTIPLE);
        ItemDatabase.attributesById.put(itemId, attributes);
      }
    } else {
      // We think the item is multi usable but it really is single usable
      if (useType == ConsumptionType.USE_MULTIPLE) {
        ItemDatabase.useTypeById.put(itemId, ConsumptionType.USE);
      } else {
        attributes.add(Attribute.USABLE);
        ItemDatabase.attributesById.put(itemId, attributes);
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
      ItemDatabase.useTypeById.put(itemId, ConsumptionType.NONE);
      ItemDatabase.attributesById.put(itemId, EnumSet.noneOf(Attribute.class));
      ItemDatabase.accessById.put(id, Attribute.TRADEABLE + "," + Attribute.DISCARDABLE);
      ItemDatabase.priceById.put(itemId, 0);
      return;
    }

    String itemName = DebugDatabase.parseName(text);

    String image = DebugDatabase.parseImage(rawText);
    ItemDatabase.imageById.put(itemId, image);

    // Parse use type, access, and price from description
    String type = DebugDatabase.parseType(text);
    ConsumptionType usage = DebugDatabase.typeToPrimary(type, multi);
    if (text.contains("blue\">Makes you look like")) {
      usage = ConsumptionType.AVATAR_POTION;
    }
    ItemDatabase.useTypeById.put(itemId, usage);

    String access = DebugDatabase.parseAccess(text);
    ItemDatabase.accessById.put(id, access);

    EnumSet<Attribute> attrs = DebugDatabase.typeToSecondary(type, usage, text, multi);
    for (Attribute a : ACCESS) {
      if (access.contains(a.description)) attrs.add(a);
    }
    if (multi && usage != ConsumptionType.USE_MULTIPLE) {
      attrs.add(Attribute.MULTIPLE);
    }
    ItemDatabase.attributesById.put(itemId, attrs);

    int price = DebugDatabase.parsePrice(text);
    ItemDatabase.priceById.put(itemId, price);
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

    if (KoLConstants.isEquipmentType(usage, false)) {
      EquipmentDatabase.newEquipment = true;

      // Get power from description, if otherwise unknown
      if (power == 0) {
        power = DebugDatabase.parsePower(text);
      }

      // Let equipment database do what it wishes with this item
      EquipmentDatabase.registerItem(itemId, itemName, text, power);
    } else if (usage == ConsumptionType.EAT
        || usage == ConsumptionType.DRINK
        || usage == ConsumptionType.SPLEEN) {
      ConsumablesDatabase.registerConsumable(itemName, usage, text);
    }

    // Let modifiers database do what it wishes with this item
    ModifierDatabase.registerItem(itemName, text, usage);

    // Done generating data
    printMe = "--------------------";
    RequestLogger.printLine(printMe);
    RequestLogger.updateSessionLog(printMe);

    // Equipment can be part of an outfit.
    EquipmentDatabase.registerItemOutfit(itemId, text);

    // Skillbooks teach you a skill
    String skillName =
        ModifierDatabase.getStringModifier(ModifierType.ITEM, itemId, StringModifier.SKILL);
    if (!skillName.equals("") && SkillDatabase.getSkillId(skillName) == -1) {
      int skillId = DebugDatabase.parseSkillId(rawText);
      SkillDatabase.registerSkill(skillId, skillName);
    }

    // Potions grant an effect. Check for a new effect.
    String effectName =
        ModifierDatabase.getStringModifier(ModifierType.ITEM, itemId, StringModifier.EFFECT);
    if (!effectName.equals("") && EffectDatabase.getEffectId(effectName, true) == -1) {
      String effectDescid = DebugDatabase.parseEffectDescid(rawText);
      String command =
          switch (usage) {
            case EAT -> "eat 1 ";
            case DRINK -> "drink 1 ";
            case SPLEEN -> "chew 1 ";
            default -> "use 1 ";
          };
      EffectDatabase.registerEffect(effectName, effectDescid, command + itemName);
    }

    // Equipment can have a Rollover Effect. Check for new effect.
    effectName =
        ModifierDatabase.getStringModifier(
            ModifierType.ITEM, itemId, StringModifier.ROLLOVER_EFFECT);
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
    Integer id = itemId;

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

  public static String getDataName(final Integer itemId) {
    return ItemDatabase.dataNameById.get(itemId);
  }

  public static String getDisplayName(final Integer itemId) {
    return ItemDatabase.nameById.get(itemId);
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
        return ItemDatabase.getCanonicalName(itemId);
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
      return ItemDatabase.getCanonicalName(itemId);
    }

    // The word right before the dash may also be pluralized,
    // so make sure the dashed words are recognized.

    itemId =
        ItemDatabase.getExactItemId(StringUtilities.singleStringReplace(canonicalName, "es-", "-"));
    if (itemId != -1) {
      return ItemDatabase.getCanonicalName(itemId);
    }

    itemId =
        ItemDatabase.getExactItemId(StringUtilities.singleStringReplace(canonicalName, "s-", "-"));
    if (itemId != -1) {
      return ItemDatabase.getCanonicalName(itemId);
    }

    // If it's a plural form of "tooth", then make
    // sure that it's handled.  Other things which
    // also have "ee" plural forms should be clumped
    // in as well.

    itemId =
        ItemDatabase.getExactItemId(StringUtilities.singleStringReplace(canonicalName, "ee", "oo"));
    if (itemId != -1) {
      return ItemDatabase.getCanonicalName(itemId);
    }

    // Also handle the plural of vortex, which is
    // "vortices" -- this should only appear in the
    // meat vortex, but better safe than sorry.

    itemId =
        ItemDatabase.getExactItemId(
            StringUtilities.singleStringReplace(canonicalName, "ices", "ex"));
    if (itemId != -1) {
      return ItemDatabase.getCanonicalName(itemId);
    }

    // Handling of appendices (which is the plural
    // of appendix, not appendex, so it is not caught
    // by the previous test).

    itemId =
        ItemDatabase.getExactItemId(
            StringUtilities.singleStringReplace(canonicalName, "ices", "ix"));
    if (itemId != -1) {
      return ItemDatabase.getCanonicalName(itemId);
    }

    // Also add in a special handling for knives
    // and other things ending in "ife".

    itemId =
        ItemDatabase.getExactItemId(
            StringUtilities.singleStringReplace(canonicalName, "ives", "ife"));
    if (itemId != -1) {
      return ItemDatabase.getCanonicalName(itemId);
    }

    // Also add in a special handling for elves
    // and other things ending in "f".

    itemId =
        ItemDatabase.getExactItemId(StringUtilities.singleStringReplace(canonicalName, "ves", "f"));
    if (itemId != -1) {
      return ItemDatabase.getCanonicalName(itemId);
    }

    // Also add in a special handling for staves
    // and other things ending in "aff".

    itemId =
        ItemDatabase.getExactItemId(
            StringUtilities.singleStringReplace(canonicalName, "aves", "aff"));
    if (itemId != -1) {
      return ItemDatabase.getCanonicalName(itemId);
    }

    // If it's a pluralized form of something that
    // ends with "y", then return the appropriate
    // item Id for the "y" version.

    if (canonicalName.endsWith("ies")) {
      itemId =
          ItemDatabase.getExactItemId(canonicalName.substring(0, canonicalName.length() - 3) + "y");
      if (itemId != -1) {
        return ItemDatabase.getCanonicalName(itemId);
      }
    }

    itemId =
        ItemDatabase.getExactItemId(
            StringUtilities.singleStringReplace(canonicalName, "ies ", "y "));
    if (itemId != -1) {
      return ItemDatabase.getCanonicalName(itemId);
    }

    // If it's a pluralized form of something that
    // ends with "o", then return the appropriate
    // item Id for the "o" version.

    if (canonicalName.endsWith("es")) {
      itemId = ItemDatabase.getExactItemId(canonicalName.substring(0, canonicalName.length() - 2));
      if (itemId != -1) {
        return ItemDatabase.getCanonicalName(itemId);
      }
    }

    itemId =
        ItemDatabase.getExactItemId(StringUtilities.singleStringReplace(canonicalName, "es ", " "));
    if (itemId != -1) {
      return ItemDatabase.getCanonicalName(itemId);
    }

    // If it's a pluralized form of something that
    // ends with "an", then return the appropriate
    // item Id for the "en" version.

    itemId =
        ItemDatabase.getExactItemId(
            StringUtilities.singleStringReplace(canonicalName, "en ", "an "));
    if (itemId != -1) {
      return ItemDatabase.getCanonicalName(itemId);
    }

    // If it's a standard pluralized forms, then
    // return the appropriate item Id.

    itemId = ItemDatabase.getExactItemId(canonicalName.replaceFirst("([A-Za-z])s ", "$1 "));
    if (itemId != -1) {
      return ItemDatabase.getCanonicalName(itemId);
    }

    // If it's something that ends with 'i', then
    // it might be a singular ending with 'us'.

    if (canonicalName.endsWith("i")) {
      itemId =
          ItemDatabase.getExactItemId(
              canonicalName.substring(0, canonicalName.length() - 1) + "us");
      if (itemId != -1) {
        return ItemDatabase.getCanonicalName(itemId);
      }
    }

    // Unknown item

    return null;
  }

  public static final int getNameLength(final int itemId) {
    return ItemDatabase.nameLength.getOrDefault(itemId, 0);
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
    return pluralById.getOrDefault(itemId, "");
  }

  public static final String getImage(final int itemId) {
    return imageById.getOrDefault(itemId, "");
  }

  public static final String getSmallImage(final int itemId) {
    return switch (itemId) {
      case ItemPool.FOLDER_01,
          ItemPool.FOLDER_02,
          ItemPool.FOLDER_03,
          ItemPool.FOLDER_04,
          ItemPool.FOLDER_05,
          ItemPool.FOLDER_07,
          ItemPool.FOLDER_09,
          ItemPool.FOLDER_10,
          ItemPool.FOLDER_12,
          ItemPool.FOLDER_13,
          ItemPool.FOLDER_24,
          ItemPool.FOLDER_26 -> "folder2.gif";
      case ItemPool.FOLDER_06,
          ItemPool.FOLDER_08,
          ItemPool.FOLDER_11,
          ItemPool.FOLDER_14,
          ItemPool.FOLDER_15,
          ItemPool.FOLDER_16,
          ItemPool.FOLDER_17,
          ItemPool.FOLDER_18,
          ItemPool.FOLDER_19,
          ItemPool.FOLDER_20,
          ItemPool.FOLDER_21,
          ItemPool.FOLDER_22,
          ItemPool.FOLDER_23,
          ItemPool.FOLDER_25,
          ItemPool.FOLDER_27,
          ItemPool.FOLDER_28 -> "folder1.gif";
      default -> imageById.getOrDefault(itemId, "");
    };
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
    return ItemDatabase.priceById.getOrDefault(itemId, 0);
  }

  /**
   * Returns the access for the item with the given Id.
   *
   * @return The access associated with the item
   */
  public static final String getAccessById(final Integer itemId) {
    return ItemDatabase.accessById.get(itemId);
  }

  public static final EnumSet<Attribute> getAttributes(int itemId) {
    return EnumSet.copyOf(
        ItemDatabase.attributesById.getOrDefault(itemId, EnumSet.noneOf(Attribute.class)));
  }

  public static final String attrsToSecondaryUsage(EnumSet<Attribute> attrs) {
    StringBuilder result = new StringBuilder();

    for (var attr : attrs) {
      switch (attr) {
        case TRADEABLE:
        case GIFT:
        case QUEST:
        case DISCARDABLE:
          continue;
        default:
          result.append(", ");
          result.append(attr.description);
      }
    }

    return result.toString();
  }

  public static boolean getAttribute(int itemId, Attribute mask) {
    var attrs = ItemDatabase.attributesById.getOrDefault(itemId, EnumSet.noneOf(Attribute.class));
    return attrs.contains(mask);
  }

  public static boolean getAttribute(int itemId, EnumSet<Attribute> mask) {
    var attrs = ItemDatabase.attributesById.getOrDefault(itemId, EnumSet.noneOf(Attribute.class));
    for (var attr : mask) {
      if (attrs.contains(attr)) return true;
    }
    return false;
  }

  /**
   * Returns true if the item is a quest item, otherwise false
   *
   * @return true if item is a quest item
   */
  public static final boolean isQuestItem(final int itemId) {
    return ItemDatabase.getAttribute(itemId, Attribute.QUEST);
  }

  /**
   * Returns true if the item is a gift item, otherwise false
   *
   * @return true if item is a gift item
   */
  public static final boolean isGiftItem(final int itemId) {
    return ItemDatabase.getAttribute(itemId, Attribute.GIFT);
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
    return switch (itemId) {
      case ItemPool.MADNESS_REEF_MAP,
          ItemPool.MARINARA_TRENCH_MAP,
          ItemPool.ANEMONE_MINE_MAP,
          ItemPool.DIVE_BAR_MAP,
          ItemPool.SKATE_PARK_MAP,
          ItemPool.GLASS_OF_MILK,
          ItemPool.CUP_OF_TEA,
          ItemPool.THERMOS_OF_WHISKEY,
          ItemPool.LUCKY_LINDY,
          ItemPool.BEES_KNEES,
          ItemPool.SOCKDOLLAGER,
          ItemPool.ISH_KABIBBLE,
          ItemPool.HOT_SOCKS,
          ItemPool.PHONUS_BALONUS,
          ItemPool.FLIVVER,
          ItemPool.SLOPPY_JALOPY -> true;
      default -> false;
    };
  }

  public static final boolean haveVirtualItem(final int itemId) {
    return switch (itemId) {
      case ItemPool.MADNESS_REEF_MAP -> Preferences.getBoolean("mapToMadnessReefPurchased");
      case ItemPool.MARINARA_TRENCH_MAP -> Preferences.getBoolean(
          "mapToTheMarinaraTrenchPurchased");
      case ItemPool.ANEMONE_MINE_MAP -> Preferences.getBoolean("mapToAnemoneMinePurchased");
      case ItemPool.DIVE_BAR_MAP -> Preferences.getBoolean("mapToTheDiveBarPurchased");
      case ItemPool.SKATE_PARK_MAP -> Preferences.getBoolean("mapToTheSkateParkPurchased");
      default -> false;
    };
  }

  /**
   * Returns true if the item is tradeable, otherwise false
   *
   * @return true if item is tradeable
   */
  public static final boolean isTradeable(final int itemId) {
    return ItemDatabase.getAttribute(itemId, Attribute.TRADEABLE);
  }

  /**
   * Returns true if the item is giftable, otherwise false
   *
   * @return true if item is giftable
   */
  public static final boolean isGiftable(final int itemId) {
    return ItemDatabase.getAttribute(itemId, EnumSet.of(Attribute.TRADEABLE, Attribute.GIFT));
  }

  /**
   * Returns true if the item is discardable, otherwise false
   *
   * @return true if item is discardable
   */
  public static final boolean isDiscardable(final int itemId) {
    return ItemDatabase.getAttribute(itemId, Attribute.DISCARDABLE);
  }

  /**
   * Returns true if the item is a bounty, otherwise false
   *
   * @return true if item is a bounty
   */
  public static final boolean isBountyItem(final int itemId) {
    return ItemDatabase.getAttribute(itemId, Attribute.BOUNTY);
  }

  /**
   * Returns true if the item is a Meat Pasting Component ingredient, otherwise false
   *
   * @return true if item is a Meat Pasting Component
   */
  public static final boolean isPasteable(final int itemId) {
    return ItemDatabase.getAttribute(itemId, Attribute.PASTE);
  }

  /**
   * Returns true if the item is a Meatsmithing Component, otherwise false
   *
   * @return true if item is a Meatsmithing Component
   */
  public static final boolean isSmithable(final int itemId) {
    return ItemDatabase.getAttribute(itemId, Attribute.SMITH);
  }

  /**
   * Returns true if the item is a Cooking Ingredient, otherwise false
   *
   * @return true if item is a Cooking Ingredient
   */
  public static final boolean isCookable(final int itemId) {
    return ItemDatabase.getAttribute(itemId, Attribute.COOK);
  }

  /**
   * Returns true if the item is a Cocktailcrafting Ingredient, otherwise false
   *
   * @return true if item is a Cocktailcrafting ingredient
   */
  public static final boolean isMixable(int itemId) {
    return ItemDatabase.getAttribute(itemId, Attribute.MIX);
  }

  /**
   * Returns true if the item is a fancy ingredient, otherwise false
   *
   * @return true if item is a fancy ingredient
   */
  public static final boolean isFancyItem(final int itemId) {
    return ItemDatabase.getAttribute(itemId, Attribute.FANCY);
  }

  /**
   * Returns true if the item is a candy, otherwise false
   *
   * @return true if item is a candy
   */
  public static final boolean isCandyItem(final int itemId) {
    return ItemDatabase.getAttribute(
        itemId, EnumSet.of(Attribute.CANDY0, Attribute.CANDY1, Attribute.CANDY2));
  }

  /**
   * Returns true if the item is an adventure-granting chocolate, otherwise false
   *
   * @return true if item is a chocolate
   */
  public static final boolean isChocolateItem(final int itemId) {
    return ItemDatabase.getAttribute(itemId, Attribute.CHOCOLATE);
  }

  /**
   * Returns the name for an item, given its Id number.
   *
   * @param itemId The Id number of the item to lookup
   * @return The name of the corresponding item
   */
  public static final String getItemName(final int itemId) {
    return ItemDatabase.nameById.get(itemId);
  }

  public static final String getItemDataName(final int itemId) {
    return ItemDatabase.dataNameById.get(itemId);
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
   * Returns the id for an item, given its description id. If the item is unknown, register a new
   * item from the description text
   *
   * @param descId The description id of the item to lookup
   * @return The item id of the corresponding item
   */
  public static final int lookupItemIdFromDescription(final String descId) {
    if (descId.equals("")) {
      return -1;
    }
    // See if we know the id already
    Integer itemId = ItemDatabase.itemIdByDescription.get(descId);
    if (itemId == null) {
      // No. register a new item.
      return ItemDatabase.registerItem(descId);
    }
    return itemId;
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
  public static boolean isUsable(final int itemId) {
    // Anything that you can manipulate with inv_use.php

    ConsumptionType useType = ItemDatabase.useTypeById.getOrDefault(itemId, ConsumptionType.NONE);
    EnumSet<Attribute> attributes = ItemDatabase.getAttributes(itemId);

    return switch (useType) {
      case
          // Explicit "use"
          USE,
          USE_MESSAGE_DISPLAY,
          USE_INFINITE,
          // Multi-use
          USE_MULTIPLE,
          // Grow is a type of use
          FAMILIAR_HATCHLING,
          // Any potion
          POTION,
          AVATAR_POTION -> true;
      default -> attributes.contains(Attribute.USABLE)
          || attributes.contains(Attribute.MULTIPLE)
          || attributes.contains(Attribute.REUSABLE);
    };
  }

  public static final boolean isPotion(final AdventureResult item) {
    if (item == null) {
      return false;
    }
    return ItemDatabase.isPotion(item.getItemId());
  }

  public static final boolean isPotion(final int itemId) {
    ConsumptionType useType = ItemDatabase.useTypeById.getOrDefault(itemId, ConsumptionType.NONE);
    return (useType == ConsumptionType.POTION || useType == ConsumptionType.AVATAR_POTION);
  }

  public static final boolean isEquipment(final int itemId) {
    ConsumptionType useType = ItemDatabase.useTypeById.getOrDefault(itemId, ConsumptionType.NONE);
    return KoLConstants.isEquipmentType(useType, true);
  }

  public static final boolean isFood(final int itemId) {
    ConsumptionType useType = ItemDatabase.useTypeById.getOrDefault(itemId, ConsumptionType.NONE);
    return useType == ConsumptionType.EAT;
  }

  public static final boolean isBooze(final int itemId) {
    ConsumptionType useType = ItemDatabase.useTypeById.getOrDefault(itemId, ConsumptionType.NONE);
    return useType == ConsumptionType.DRINK;
  }

  public static final boolean isHat(final int itemId) {
    ConsumptionType useType = ItemDatabase.useTypeById.getOrDefault(itemId, ConsumptionType.NONE);
    return useType == ConsumptionType.HAT;
  }

  public static final boolean isWeapon(final int itemId) {
    ConsumptionType useType = ItemDatabase.useTypeById.getOrDefault(itemId, ConsumptionType.NONE);
    return useType == ConsumptionType.WEAPON;
  }

  public static final boolean isOffHand(final int itemId) {
    ConsumptionType useType = ItemDatabase.useTypeById.getOrDefault(itemId, ConsumptionType.NONE);
    return useType == ConsumptionType.OFFHAND;
  }

  public static final boolean isShirt(final int itemId) {
    ConsumptionType useType = ItemDatabase.useTypeById.getOrDefault(itemId, ConsumptionType.NONE);
    return useType == ConsumptionType.SHIRT;
  }

  public static final boolean isPants(final int itemId) {
    ConsumptionType useType = ItemDatabase.useTypeById.getOrDefault(itemId, ConsumptionType.NONE);
    return useType == ConsumptionType.PANTS;
  }

  public static final boolean isAccessory(final int itemId) {
    ConsumptionType useType = ItemDatabase.useTypeById.getOrDefault(itemId, ConsumptionType.NONE);
    return useType == ConsumptionType.ACCESSORY;
  }

  public static final boolean isFamiliarEquipment(final int itemId) {
    ConsumptionType useType = ItemDatabase.useTypeById.getOrDefault(itemId, ConsumptionType.NONE);
    return useType == ConsumptionType.FAMILIAR_EQUIPMENT;
  }

  public static final boolean isMultiUsable(final int itemId) {
    // Anything that you can manipulate with multiuse.php

    ConsumptionType useType = ItemDatabase.useTypeById.getOrDefault(itemId, ConsumptionType.NONE);
    EnumSet<Attribute> attributes = ItemDatabase.getAttributes(itemId);

    return switch (useType) {
      case USE_MULTIPLE -> true;
      case POTION, AVATAR_POTION, SPLEEN -> !attributes.contains(Attribute.USABLE);
      default -> attributes.contains(Attribute.MULTIPLE);
    };
  }

  public static final boolean isReusable(final int itemId) {
    ConsumptionType useType = ItemDatabase.useTypeById.getOrDefault(itemId, ConsumptionType.NONE);
    if (useType == ConsumptionType.USE_INFINITE) return true;
    EnumSet<Attribute> attributes = ItemDatabase.getAttributes(itemId);
    return attributes.contains(Attribute.REUSABLE);
  }

  /**
   * Returns whether or not the item with the given name is made of grimacite and is thus affected
   * by the moon phases.
   *
   * @return <code>true</code> if the item is grimacite
   */
  public static final boolean isGrimacite(int itemId) {
    return switch (itemId) {
        // Grimacite Generation 1
      case ItemPool.GRIMACITE_GOGGLES,
          ItemPool.GRIMACITE_GLAIVE,
          ItemPool.GRIMACITE_GREAVES,
          ItemPool.GRIMACITE_GARTER,
          ItemPool.GRIMACITE_GALOSHES,
          ItemPool.GRIMACITE_GORGET,
          ItemPool.GRIMACITE_GUAYABERA,
          // Grimacite Generation 2
          ItemPool.GRIMACITE_GASMASK,
          ItemPool.GRIMACITE_GAT,
          ItemPool.GRIMACITE_GAITERS,
          ItemPool.GRIMACITE_GAUNTLETS,
          ItemPool.GRIMACITE_GO_GO_BOOTS,
          ItemPool.GRIMACITE_GIRDLE,
          ItemPool.GRIMACITE_GOWN,
          // Depleted Grimacite
          ItemPool.GRIMACITE_HAMMER,
          ItemPool.GRIMACITE_GRAVY_BOAT,
          ItemPool.GRIMACITE_WEIGHTLIFTING_BELT,
          ItemPool.GRIMACITE_GRAPPLING_HOOK,
          ItemPool.GRIMACITE_NINJA_MASK,
          ItemPool.GRIMACITE_SHINGUARDS,
          ItemPool.GRIMACITE_ASTROLABE,
          ItemPool.GRIMACITE_KNEECAPPING_STICK -> true;
      default -> false;
    };
  }

  public static final boolean isSealFigurine(final int itemId) {
    return switch (itemId) {
      case ItemPool.WRETCHED_SEAL,
          ItemPool.CUTE_BABY_SEAL,
          ItemPool.ARMORED_SEAL,
          ItemPool.ANCIENT_SEAL,
          ItemPool.SLEEK_SEAL,
          ItemPool.SHADOWY_SEAL,
          ItemPool.STINKING_SEAL,
          ItemPool.CHARRED_SEAL,
          ItemPool.COLD_SEAL,
          ItemPool.SLIPPERY_SEAL,
          ItemPool.DEPLETED_URANIUM_SEAL -> true;
      default -> false;
    };
  }

  public static final boolean isBRICKOMonster(final int itemId) {
    return switch (itemId) {
      case ItemPool.BRICKO_OOZE,
          ItemPool.BRICKO_BAT,
          ItemPool.BRICKO_OYSTER,
          ItemPool.BRICKO_TURTLE,
          ItemPool.BRICKO_ELEPHANT,
          ItemPool.BRICKO_OCTOPUS,
          ItemPool.BRICKO_PYTHON,
          ItemPool.BRICKO_VACUUM_CLEANER,
          ItemPool.BRICKO_AIRSHIP,
          ItemPool.BRICKO_CATHEDRAL,
          ItemPool.BRICKO_CHICKEN -> true;
      default -> false;
    };
  }

  public static final boolean isStinkyCheeseItem(final int itemId) {
    return switch (itemId) {
      case ItemPool.STINKY_CHEESE_SWORD,
          ItemPool.STINKY_CHEESE_DIAPER,
          ItemPool.STINKY_CHEESE_WHEEL,
          ItemPool.STINKY_CHEESE_EYE,
          ItemPool.STINKY_CHEESE_STAFF -> true;
      default -> false;
    };
  }

  /**
   * Returns the kind of consumption associated with an item
   *
   * @return The consumption associated with the item
   */
  public static final ConsumptionType getConsumptionType(final int itemId) {
    return itemId <= 0
        ? ConsumptionType.NONE
        : ItemDatabase.useTypeById.getOrDefault(itemId, ConsumptionType.NONE);
  }

  public static final ConsumptionType getConsumptionType(final AdventureResult item) {
    return ItemDatabase.getConsumptionType(item.getItemId());
  }

  public static final String typeToPrimaryUsage(final ConsumptionType type) {
    return type.description;
  }

  /**
   * Returns the item description Id used by the given item, given its item Id.
   *
   * @return The description Id associated with the item
   */
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
      KoLCharacter.setEnthroned(KoLCharacter.usableFamiliar(race));
    }
  }

  public static void parseBuddyBjorn(final String desc) {
    // COT_PATTERN works for this
    Matcher matcher = ItemDatabase.COT_PATTERN.matcher(desc);
    if (matcher.find()) {
      String race = matcher.group(1);
      KoLCharacter.setBjorned(KoLCharacter.usableFamiliar(race));
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

  public static void parseUmbrella(final String desc) {
    if (desc.contains("Monster Level")) {
      UmbrellaMode.BROKEN.set();
    } else if (desc.contains("Damage Reduction")) {
      UmbrellaMode.FORWARD.set();
    } else if (desc.contains("Item Drops")) {
      UmbrellaMode.BUCKET.set();
    } else if (desc.contains("Weapon Damage")) {
      UmbrellaMode.PITCHFORK.set();
    } else if (desc.contains("Spell Damage")) {
      UmbrellaMode.TWIRL.set();
    } else if (desc.contains("much less attracted")) {
      UmbrellaMode.COCOON.set();
    }
  }

  public static void parseCleaver(final String desc) {
    String[] elements = {"Hot", "Cold", "Spooky", "Stench", "Sleaze"};
    for (String element : elements) {
      String preference = "_juneCleaver" + element;
      Pattern damagePattern = Pattern.compile(element + " Damage: \\+(?<value>\\d+)");
      Matcher m = damagePattern.matcher(desc);
      if (m.find()) {
        int qty = Integer.parseInt(m.group("value"));
        Preferences.setInteger(preference, qty);
      }
    }
  }

  public static void parseDesignerSweatpants(final String desc) {
    if (desc.contains("Your sweatpants are currently")) {
      return;
    }

    Preferences.setInteger("sweat", 0);
  }

  public static void parsePowerfulGlove(final String desc) {
    if (desc.contains("The Glove's battery is currently fully charged.")) {
      Preferences.setInteger("_powerfulGloveBatteryPowerUsed", 0);
    } else if (desc.contains("The Glove's battery is fully depleted.")) {
      Preferences.setInteger("_powerfulGloveBatteryPowerUsed", 100);
    }
  }

  public static void parseRing(final String desc) {
    ArrayList<String> unknown = new ArrayList<>();
    ModifierList known = new ModifierList();

    DebugDatabase.parseItemEnchantments(desc, known, unknown, ConsumptionType.ACCESSORY);
    ModifierDatabase.overrideModifier(ModifierType.ITEM, ItemPool.RING, known.toString());
  }

  public static void resetVampireVintnerWine() {
    Preferences.setString("vintnerWineName", "");
    Preferences.setString("vintnerWineEffect", "");
    Preferences.setInteger("vintnerWineLevel", 0);
    Preferences.setString("vintnerWineType", "");
    ConsumablesDatabase.updateConsumableNotes("1950 Vampire Vintner wine", "Unspaded, WINE");
  }

  public static void parseVampireVintnerWine() {
    // Submit desc_item.php for 1950 Vampire Vintner wine
    DebugDatabase.itemDescriptionText(ItemPool.VAMPIRE_VINTNER_WINE, true);
    // GenericRequest calls ResponseTextParser which makes the following call.
  }

  public static void parseVampireVintnerWine(final String idesc) {
    String iEnchantments =
        DebugDatabase.parseItemEnchantments(idesc, new ArrayList<>(), ConsumptionType.DRINK);
    String iname = DebugDatabase.parseName(idesc);
    Modifiers imods =
        ModifierDatabase.parseModifiers(
            ModifierType.ITEM, ItemPool.VAMPIRE_VINTNER_WINE, iEnchantments);

    // Validate this by seeing what effect this wine grants.
    String effectName = imods.getString(StringModifier.EFFECT);
    int effectId = EffectDatabase.getEffectId(effectName);

    // If it doesn't grant one, this is the generic 1950 Vampire Vintner wine
    if (effectId == -1) {
      ItemDatabase.resetVampireVintnerWine();
      return;
    }

    // The damage type that created this wine is implied by the effect the wine grants.
    String type =
        switch (effectId) {
          case EffectPool.WINE_FORTIFIED -> "physical";
          case EffectPool.WINE_HOT -> "hot";
          case EffectPool.WINE_COLD -> "cold";
          case EffectPool.WINE_DARK -> "spooky";
          case EffectPool.WINE_BEFOULED -> "stench";
          case EffectPool.WINE_FRISKY -> "sleaze";
          case EffectPool.WINE_FRIENDLY -> "familiar";
          default -> "";
        };

    Preferences.setString("vintnerWineName", iname);
    Preferences.setString("vintnerWineEffect", effectName);
    Preferences.setString("vintnerWineType", type);

    // Look up the description of the the effect. ResponseTextParser will
    // examine it and set the vintnerWineLevel property
    DebugDatabase.readEffectDescriptionText(effectId);

    // Override the modifiers for the 1950 Vampire Vintner wine to include the
    // effect that drinking this one will provide.
    ModifierDatabase.overrideModifier(ModifierType.ITEM, ItemPool.VAMPIRE_VINTNER_WINE, imods);

    // Update the consumable note
    Modifiers emods = ModifierDatabase.getEffectModifiers(effectId);
    String modifierString = emods.getString(StringModifier.MODIFIERS);
    String notes = "12 turns of " + effectName + " (" + modifierString + "), WINE";
    ConsumablesDatabase.updateConsumableNotes("1950 Vampire Vintner wine", notes);
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
    KoLCharacter.removeAvailableSkill(SkillPool.SMOOCH_OF_THE_DAYWALKER);
    KoLCharacter.removeAvailableSkill(SkillPool.SLAY_THE_DEAD);
    KoLCharacter.removeAvailableSkill(SkillPool.UNLEASH_THE_DEVILS_KISS);
    KoLCharacter.removeAvailableSkill(SkillPool.DEPLOY_ROBO_HANDCUFFS);
    KoLCharacter.removeAvailableSkill(SkillPool.BLOW_A_ROBO_KISS);
    KoLCharacter.removeAvailableSkill(SkillPool.PRECISION_SHOT);

    // If the cape is not equipped, that is correct
    if (!KoLCharacter.hasEquipped(ItemPool.KNOCK_OFF_RETRO_SUPERHERO_CAPE, Slot.CONTAINER)) {
      return;
    }

    String superhero = Preferences.getString("retroCapeSuperhero");
    String instructions = Preferences.getString("retroCapeWashingInstructions");

    // It is equipped. Add available skills
    switch (superhero) {
      case "vampire":
        // Add Vampire Slicer skills
        if (instructions.equals("kiss")) {
          KoLCharacter.addAvailableSkill(SkillPool.SMOOCH_OF_THE_DAYWALKER);
        }

        if (instructions.equals("kill") && EquipmentManager.wieldingSword()) {
          KoLCharacter.addAvailableSkill(SkillPool.SLAY_THE_DEAD);
        }
        break;
      case "heck":
        // Add Heck General skills
        if (instructions.equals("kiss")) {
          KoLCharacter.addAvailableSkill(SkillPool.UNLEASH_THE_DEVILS_KISS);
        }
        break;
      case "robot":
        // Add Robot Police skills
        if (instructions.equals("hold")) {
          KoLCharacter.addAvailableSkill(SkillPool.DEPLOY_ROBO_HANDCUFFS);
        }
        if (instructions.equals("kiss")) {
          KoLCharacter.addAvailableSkill(SkillPool.BLOW_A_ROBO_KISS);
        }
        if (instructions.equals("kill") && EquipmentManager.wieldingGun()) {
          KoLCharacter.addAvailableSkill(SkillPool.PRECISION_SHOT);
        }
        break;
    }
  }

  public static boolean unusableInBeecore(final int itemId) {
    return switch (itemId) {
      case
          // These "B" items ARE usable in Beecore.
          ItemPool.BALL_POLISH,
          ItemPool.FRATHOUSE_BLUEPRINTS,
          ItemPool.COBBS_KNOB_MAP,
          ItemPool.BINDER_CLIP,
          // And so are these IOTM foldables
          ItemPool.ICE_BABY,
          ItemPool.JUGGLERS_BALLS,
          ItemPool.EYEBALL_PENDANT,
          ItemPool.SPOOKY_PUTTY_BALL,
          ItemPool.LOATHING_LEGION_ABACUS,
          ItemPool.LOATHING_LEGION_DEFIBRILLATOR,
          ItemPool.LOATHING_LEGION_DOUBLE_PRISM,
          ItemPool.LOATHING_LEGION_ROLLERBLADES,
          // "using" this is really planting

          ItemPool.ENCHANTED_BEAN -> false;
      default -> KoLCharacter.hasBeeosity(ItemDatabase.getItemName(itemId));
    };
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
    return switch (itemId) {
      case ItemPool.MUSHROOM, ItemPool.DELUXE_MUSHROOM, ItemPool.SUPER_DELUXE_MUSHROOM -> true;
      default -> false;
    };
  }

  public static boolean isAllowed(final int itemId) {
    return StandardRequest.isAllowed(RestrictedItemType.ITEMS, ItemDatabase.getDataName(itemId));
  }

  public static boolean isAllowedInStandard(final int itemId) {
    return StandardRequest.isAllowedInStandard(
        RestrictedItemType.ITEMS, ItemDatabase.getDataName(itemId));
  }

  public static int getNoobSkillId(final int itemId) {
    return ItemDatabase.noobSkillIdByItemSource.getOrDefault(itemId, 0);
  }

  public static int[] getItemListByNoobSkillId(final int skillId) {
    return ItemDatabase.itemSourceByNoobSkillId.get(skillId);
  }
}
