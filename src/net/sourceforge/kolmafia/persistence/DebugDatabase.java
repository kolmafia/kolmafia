package net.sourceforge.kolmafia.persistence;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLConstants.ConsumptionType;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.ModifierExpression;
import net.sourceforge.kolmafia.ModifierType;
import net.sourceforge.kolmafia.MonsterData;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.SpecialOutfit;
import net.sourceforge.kolmafia.StaticEntity;
import net.sourceforge.kolmafia.modifiers.Lookup;
import net.sourceforge.kolmafia.modifiers.ModifierList;
import net.sourceforge.kolmafia.modifiers.ModifierList.ModifierValue;
import net.sourceforge.kolmafia.modifiers.StringModifier;
import net.sourceforge.kolmafia.objectpool.Concoction;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.ConsumablesDatabase.ConsumableQuality;
import net.sourceforge.kolmafia.persistence.ItemDatabase.Attribute;
import net.sourceforge.kolmafia.persistence.MonsterDatabase.Element;
import net.sourceforge.kolmafia.request.ApiRequest;
import net.sourceforge.kolmafia.request.ClosetRequest;
import net.sourceforge.kolmafia.request.ClosetRequest.ClosetRequestType;
import net.sourceforge.kolmafia.request.DisplayCaseRequest;
import net.sourceforge.kolmafia.request.FamiliarRequest;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.request.MonsterManuelRequest;
import net.sourceforge.kolmafia.request.StorageRequest;
import net.sourceforge.kolmafia.request.StorageRequest.StorageRequestType;
import net.sourceforge.kolmafia.request.ZapRequest;
import net.sourceforge.kolmafia.scripts.svn.SVNManager;
import net.sourceforge.kolmafia.session.DisplayCaseManager;
import net.sourceforge.kolmafia.session.EquipmentManager;
import net.sourceforge.kolmafia.session.InventoryManager;
import net.sourceforge.kolmafia.utilities.CharacterEntities;
import net.sourceforge.kolmafia.utilities.FileUtilities;
import net.sourceforge.kolmafia.utilities.HttpUtilities;
import net.sourceforge.kolmafia.utilities.LogStream;
import net.sourceforge.kolmafia.utilities.StringUtilities;
import net.sourceforge.kolmafia.utilities.WikiUtilities;
import net.sourceforge.kolmafia.utilities.WikiUtilities.WikiType;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNURL;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class DebugDatabase {
  private static final Pattern WIKI_PLURAL_PATTERN =
      Pattern.compile("\\(.*?In-game plural</a>: <i>(.*?)</i>\\)", Pattern.DOTALL);
  private static final Pattern WIKI_MONSTER_MEAT_PATTERN =
      Pattern.compile("Meat gained - ([\\d,]+)(?:-([\\d,]+))?");

  private static final Pattern WIKI_MONSTER_ID_PATTERN = Pattern.compile("Monster ID - (\\d+)");

  private static final Pattern WIKI_ELEMENT_ATTACK_PATTERN =
      Pattern.compile("\\(<span class=\"element-[^)]+<b>([^<]+) damage</b>");

  private DebugDatabase() {}

  /** Takes an item name and constructs the likely Wiki equivalent of that item name. */
  private static String readWikiItemData(final String name, final HttpClient client) {
    String url = WikiUtilities.getWikiLocation(name, WikiType.ITEM, false);
    return DebugDatabase.readWikiData(url, client);
  }

  private static String readWikiMonsterData(final MonsterData monster, final HttpClient client) {
    String url = WikiUtilities.getWikiLocation(monster, true);
    return DebugDatabase.readWikiData(url, client);
  }

  private static String readWikiMonster(final MonsterData monster, final HttpClient client) {
    String url = WikiUtilities.getWikiLocation(monster, false);
    return DebugDatabase.readWikiData(url, client);
  }

  private static String readWikiData(String url, final HttpClient client) {
    URI uri;
    try {
      uri = new URI(url.replace("\"", "%22"));
    } catch (URISyntaxException e) {
      return "";
    }

    var request =
        HttpRequest.newBuilder(uri).header("User-Agent", GenericRequest.getUserAgent()).build();
    HttpResponse<String> response;
    try {
      response = client.send(request, BodyHandlers.ofString(StandardCharsets.UTF_8));
    } catch (IOException | InterruptedException e) {
      return "";
    }

    if (response.statusCode() == 200) {
      return response.body();
    } else {
      return "";
    }
  }

  private static String readApiPlural(final int itemId) {
    GenericRequest request = new ApiRequest("item", itemId);
    RequestThread.postRequest(request);
    String plural = "";
    JSONObject json;
    try {
      json = new JSONObject(request.responseText);
      plural = (String) json.get("plural");
    } catch (JSONException ex) {
      KoLmafia.updateDisplay("Exception reading API: " + ex.getMessage());
    }
    return plural;
  }

  private static boolean decodedNamesEqual(String name1, String name2) {
    // Sacr&eacute; Mental
    // Sacré Mental
    return name1.equals(name2)
        || StringUtilities.getEntityDecode(name1)
            .equalsIgnoreCase(StringUtilities.getEntityDecode(name2));
  }

  /** Utility method which searches for the plural version of the item on the KoL wiki. */

  // **********************************************************

  // Support for the "checkitems" command, which compares KoLmafia's
  // internal item data from what can be mined from the item description.

  private static final String ITEM_HTML = "itemhtml.txt";

  private static final String ITEM_DATA = "itemdata.txt";
  private static final Map<Integer, String> rawItems = new HashMap<>();

  private static class ItemMap {
    private final String tag;
    private final ConsumptionType type;
    private final Map<String, String> map;

    public ItemMap(final String tag, final ConsumptionType type) {
      this.tag = tag;
      this.type = type;
      this.map = new TreeMap<>(KoLConstants.ignoreCaseComparator);
    }

    public String getTag() {
      return this.tag;
    }

    public ConsumptionType getType() {
      return this.type;
    }

    public Map<String, String> getMap() {
      return this.map;
    }

    public void clear() {
      this.map.clear();
    }

    public void put(String name, String text) {
      this.map.put(name, text);
    }

    @Override
    public String toString() {
      return this.tag;
    }
  }

  private static final ItemMap[] ITEM_MAPS = {
    new ItemMap("Food", ConsumptionType.EAT),
    new ItemMap("Booze", ConsumptionType.DRINK),
    new ItemMap("Spleen Toxins", ConsumptionType.SPLEEN),
    new ItemMap("Hats", ConsumptionType.HAT),
    new ItemMap("Weapons", ConsumptionType.WEAPON),
    new ItemMap("Off-hand Items", ConsumptionType.OFFHAND),
    new ItemMap("Shirts", ConsumptionType.SHIRT),
    new ItemMap("Pants", ConsumptionType.PANTS),
    new ItemMap("Accessories", ConsumptionType.ACCESSORY),
    new ItemMap("Containers", ConsumptionType.CONTAINER),
    new ItemMap("Familiar Items", ConsumptionType.FAMILIAR_EQUIPMENT),
    new ItemMap("Potions", ConsumptionType.POTION),
    new ItemMap("Avatar Potions", ConsumptionType.AVATAR_POTION),
    new ItemMap("Everything Else", ConsumptionType.UNKNOWN),
  };

  private static ItemMap findItemMap(final ConsumptionType type) {
    ItemMap other = null;
    for (int i = 0; i < DebugDatabase.ITEM_MAPS.length; ++i) {
      ItemMap map = DebugDatabase.ITEM_MAPS[i];
      ConsumptionType mapType = map.getType();
      if (mapType == type) {
        return map;
      }
      if (mapType == ConsumptionType.UNKNOWN) {
        other = map;
      }
    }

    return other;
  }

  public static final void checkItems(final int itemId) {
    RequestLogger.printLine("Loading previous data...");
    DebugDatabase.loadScrapeData(rawItems, ITEM_HTML);

    RequestLogger.printLine("Checking internal data...");

    PrintStream report = DebugDatabase.openReport(ITEM_DATA);

    for (int i = 0; i < DebugDatabase.ITEM_MAPS.length; ++i) {
      ItemMap map = DebugDatabase.ITEM_MAPS[i];
      map.clear();
    }

    // Check item names, desc ID, consumption type

    if (itemId == 0) {
      DebugDatabase.checkItems(report);
    } else {
      DebugDatabase.checkItem(itemId, report);
    }

    // Check level limits, equipment, modifiers

    DebugDatabase.checkConsumableItems(report);
    DebugDatabase.checkEquipment(report);
    DebugDatabase.checkItemModifiers(report);

    report.close();
  }

  private static void checkItems(final PrintStream report) {
    Set<Integer> keys = ItemDatabase.descriptionIdKeySet();
    int lastId = 0;

    for (Integer id : keys) {
      if (id < 1) {
        continue;
      }

      while (++lastId < id) {
        report.println(lastId);
      }

      DebugDatabase.checkItem(id, report);
    }

    DebugDatabase.saveScrapeData(keys.iterator(), rawItems, ITEM_HTML);
  }

  private static void checkItem(final int itemId, final PrintStream report) {
    Integer id = itemId;

    String name = ItemDatabase.getItemDataName(id);
    if (name == null) {
      report.println(itemId);
      return;
    }

    String rawText = DebugDatabase.rawItemDescriptionText(itemId);

    if (rawText == null) {
      report.println("# *** " + name + " (" + itemId + ") has no description.");
      return;
    }

    String text = DebugDatabase.itemDescriptionText(rawText);
    if (text == null) {
      report.println("# *** " + name + " (" + itemId + ") has malformed description text.");
      DebugDatabase.rawItems.put(itemId, null);
      return;
    }

    String descriptionName = DebugDatabase.parseName(text);
    if (itemId == 8955) {
      descriptionName = StringUtilities.globalStringReplace(descriptionName, "  ", " ");
    }

    if (!name.equals(descriptionName) && !decodedNamesEqual(name, descriptionName)) {
      report.println(
          "# *** " + name + " (" + itemId + ") has description of " + descriptionName + ".");
      DebugDatabase.rawItems.put(itemId, null);
      return;
    }

    ConsumptionType type = ItemDatabase.getConsumptionType(itemId);
    String descType = DebugDatabase.parseType(text);
    ConsumptionType descPrimary = DebugDatabase.typeToPrimary(descType, false);
    if (!typesMatch(type, descPrimary)) {
      String primary = ItemDatabase.typeToPrimaryUsage(type);
      report.println(
          "# *** "
              + name
              + " ("
              + itemId
              + ") has primary usage of "
              + primary
              + " but is described as "
              + descType
              + ".");
    }

    EnumSet<Attribute> attrs = ItemDatabase.getAttributes(itemId);
    EnumSet<Attribute> descAttrs =
        DebugDatabase.typeToSecondary(descType, descPrimary, text, false);
    if (!DebugDatabase.attributesMatch(attrs, descAttrs)) {
      String secondary = ItemDatabase.attrsToSecondaryUsage(attrs);
      String descSecondary = ItemDatabase.attrsToSecondaryUsage(descAttrs);
      report.println(
          "# *** "
              + name
              + " ("
              + itemId
              + ") has secondary usage of "
              + secondary
              + " but is described as "
              + descSecondary
              + ".");
    }

    // Adjust crafting attributes to match description
    EnumSet<Attribute> crafting =
        EnumSet.of(
            Attribute.FANCY, Attribute.PASTE, Attribute.SMITH, Attribute.COOK, Attribute.MIX);
    attrs.removeAll(crafting);
    descAttrs.retainAll(crafting);
    attrs.addAll(descAttrs);

    // If description says it's a potion, make that the primary
    // type and move usability and multiusability into attributes
    if (descPrimary == ConsumptionType.POTION) {
      if (type == ConsumptionType.USE) {
        type = ConsumptionType.POTION;
        attrs.add(Attribute.USABLE);
      } else if (type == ConsumptionType.USE_MULTIPLE) {
        type = ConsumptionType.POTION;
        attrs.add(Attribute.MULTIPLE);
      }
    }

    int price = ItemDatabase.getPriceById(itemId);
    int descPrice = DebugDatabase.parsePrice(text);
    if (price != descPrice && (price >= 0 || descPrice != 0)) {
      report.println(
          "# *** "
              + name
              + " ("
              + itemId
              + ") has price of "
              + price
              + " but should be "
              + descPrice
              + ".");
    }

    String access = ItemDatabase.getAccessById(id);
    String descAccess = DebugDatabase.parseAccess(text);
    if (!access.equals(descAccess)) {
      report.println(
          "# *** "
              + name
              + " ("
              + itemId
              + ") has access of "
              + access
              + " but should be "
              + descAccess
              + ".");
    }

    String image = ItemDatabase.getImage(id);
    String descImage = DebugDatabase.parseImage(rawText);
    if (!image.equals(descImage)) {
      report.println(
          "# *** "
              + name
              + " ("
              + itemId
              + ") has image of "
              + image
              + " but should be "
              + descImage
              + ".");
    }

    ItemMap map = DebugDatabase.findItemMap(type);
    map.put(name, text);

    String descId = ItemDatabase.getDescriptionId(id);

    // Intentionally get a null if there is not an explicit plural in the database
    String plural = ItemDatabase.getPluralById(id);

    // In fact, if the plural is simply the name + "s", suppress it.
    if (plural != null && plural.equals(name + "s")) {
      plural = null;
    }

    report.println(
        ItemDatabase.itemString(itemId, name, descId, image, type, attrs, access, price, plural));
  }

  public static final String itemDescriptionText(final int itemId, boolean forceReload) {
    return DebugDatabase.itemDescriptionText(
        DebugDatabase.rawItemDescriptionText(ItemDatabase.getDescriptionId(itemId), forceReload));
  }

  // Public for test access.
  public static final void cacheItemDescriptionText(final int itemId, final String html) {
    DebugDatabase.rawItems.put(itemId, html);
  }

  public static final String cafeItemDescriptionText(final String descId) {
    if (descId == null) {
      return "";
    }

    GenericRequest request = new GenericRequest("desc_item.php");

    request.addFormField("whichitem", descId);
    RequestThread.postRequest(request);
    return request.responseText;
  }

  public static final String rawItemDescriptionText(final int itemId) {
    return DebugDatabase.rawItemDescriptionText(ItemDatabase.getDescriptionId(itemId), false);
  }

  public static final String rawItemDescriptionText(final String descId, boolean forceReload) {
    if (descId == null) {
      return "";
    }
    int itemId = ItemDatabase.getItemIdFromDescription(descId);
    String previous = null;
    if (itemId != -1) {
      previous = DebugDatabase.rawItems.get(itemId);
    }
    if (!forceReload && previous != null && !previous.equals("")) {
      return previous;
    }

    GenericRequest request = new GenericRequest("desc_item.php");
    request.addFormField("whichitem", descId);
    RequestThread.postRequest(request);
    if (itemId == -1) {
      itemId = DebugDatabase.parseItemId(request.responseText);
    }
    cacheItemDescriptionText(itemId, request.responseText);

    return request.responseText;
  }

  private static final Pattern ITEM_DATA_PATTERN =
      Pattern.compile("<div id=\"description\"[^>]*>(.*?)<script", Pattern.DOTALL);

  public static final String itemDescriptionText(final String rawText) {
    if (rawText == null) {
      return null;
    }

    Matcher matcher = DebugDatabase.ITEM_DATA_PATTERN.matcher(rawText);
    return matcher.find() ? matcher.group(1) : null;
  }

  // <!-- itemid: 806 -->
  private static final Pattern ITEMID_PATTERN = Pattern.compile("<!-- itemid: ([\\d]*) -->");

  public static final int parseItemId(final String text) {
    Matcher matcher = DebugDatabase.ITEMID_PATTERN.matcher(text);
    if (!matcher.find()) {
      return 0;
    }

    return StringUtilities.parseInt(matcher.group(1));
  }

  private static final Pattern NAME_PATTERN = Pattern.compile("<b>(.*?)</b>");

  public static final String parseName(final String text) {
    Matcher matcher = DebugDatabase.NAME_PATTERN.matcher(text);
    if (!matcher.find()) {
      return "";
    }

    return matcher.group(1).trim();
  }

  private static final Pattern PRICE_PATTERN =
      Pattern.compile("Selling Price: <b>(\\d+) Meat.</b>");

  public static final int parsePrice(final String text) {
    Matcher matcher = DebugDatabase.PRICE_PATTERN.matcher(text);
    if (!matcher.find()) {
      return 0;
    }

    return StringUtilities.parseInt(matcher.group(1));
  }

  private static StringBuilder appendAccessTypes(StringBuilder accessTypes, Attribute accessType) {
    if (accessTypes.length() > 0) {
      return accessTypes.append(",").append(accessType);
    }
    return accessTypes.append(accessType);
  }

  public static final String parseAccess(final String text) {
    StringBuilder accessTypes = new StringBuilder();

    if (text.contains("Quest Item")
        || text.contains("This item will disappear at the end of the day.")
        || text.contains("May not be moved out of inventory")) {
      accessTypes = appendAccessTypes(accessTypes, Attribute.QUEST);
    }

    // Quest items cannot be gifted or traded
    else if (text.contains("Gift Item") && !text.contains("gift package")) {
      accessTypes = appendAccessTypes(accessTypes, Attribute.GIFT);
    }

    // Gift items cannot be (normally) traded
    else if (!text.contains("Cannot be traded")) {
      accessTypes = appendAccessTypes(accessTypes, Attribute.TRADEABLE);
    }

    // We shouldn't just check for "discarded", in case "discarded" appears somewhere else in the
    // description.
    if (!text.contains("Cannot be discarded") && !text.contains("Cannot be traded or discarded")) {
      accessTypes = appendAccessTypes(accessTypes, Attribute.DISCARDABLE);
    }

    return accessTypes.toString();
  }

  private static final Pattern TYPE_PATTERN = Pattern.compile("Type: <b>(.*?)</b>");

  public static final String parseType(final String text) {
    Matcher matcher = DebugDatabase.TYPE_PATTERN.matcher(text);
    String type = matcher.find() ? matcher.group(1) : "";
    return type.equals("back item") ? "container" : type;
  }

  public static final ConsumptionType typeToPrimary(final String type, final boolean multi) {
    // Type: <b>food <font color=#999999>(crappy)</font></b>
    // Type: <b>food (decent)</b>
    // Type: <b>booze <font color=green>(good)</font></b>
    // Type: <b>food <font color=blue>(awesome)</font></b>
    // Type: <b>food <font color=blueviolet>(EPIC)</font></b>

    if (type.equals("") || type.equals("crafting item")) {
      return ConsumptionType.NONE;
    }
    if (type.startsWith("food") || type.startsWith("beverage")) {
      return ConsumptionType.EAT;
    }
    if (type.startsWith("booze")) {
      return ConsumptionType.DRINK;
    }
    if (type.startsWith("spleen item")) {
      return ConsumptionType.SPLEEN;
    }
    if (type.contains("self or others")) {
      // Curse items are special
      return ConsumptionType.NONE;
    }
    if (type.startsWith("potion")) {
      return ConsumptionType.POTION;
    }
    if (type.startsWith("usable") || type.contains(" usable") || type.equals("gift package")) {
      // We'll assume these are single-usable unless we are
      // explicitly told otherwise in a "rel" string

      return multi ? ConsumptionType.USE_MULTIPLE : ConsumptionType.USE;
    }
    if (type.equals("familiar equipment")) {
      return ConsumptionType.FAMILIAR_EQUIPMENT;
    }
    if (type.startsWith("familiar")) {
      return ConsumptionType.FAMILIAR_HATCHLING;
    }
    if (type.startsWith("accessory")) {
      return ConsumptionType.ACCESSORY;
    }
    if (type.startsWith("container")) {
      return ConsumptionType.CONTAINER;
    }
    if (type.startsWith("hat")) {
      return ConsumptionType.HAT;
    }
    if (type.startsWith("shirt")) {
      return ConsumptionType.SHIRT;
    }
    if (type.startsWith("pants")) {
      return ConsumptionType.PANTS;
    }
    if (type.contains("weapon")) {
      return ConsumptionType.WEAPON;
    }
    if (type.startsWith("off-hand item")) {
      return ConsumptionType.OFFHAND;
    }
    return ConsumptionType.NONE;
  }

  public static final EnumSet<Attribute> typeToSecondary(
      final String type, final ConsumptionType primary, final String text, final boolean multi) {
    EnumSet<Attribute> attributes = EnumSet.noneOf(Attribute.class);
    boolean usable =
        type.startsWith("usable")
            || type.contains(" usable")
            || type.contains("spleen")
            || type.contains("potion")
            || type.equals("gift package");
    if (type.contains("combat") && type.contains("reusable")) {
      attributes.add(Attribute.COMBAT_REUSABLE);
    } else if (type.contains("combat")) {
      attributes.add(Attribute.COMBAT);
    } else if (type.contains("reusable")) {
      attributes.add(Attribute.REUSABLE);
    } else if (type.equals("gift package")) {
      attributes.add(Attribute.PACKAGE);
    }
    if (multi && primary != ConsumptionType.USE_MULTIPLE && usable) {
      attributes.add(Attribute.MULTIPLE);
    }
    if (!multi && primary != ConsumptionType.USE && usable) {
      attributes.add(Attribute.USABLE);
    }
    if (type.contains("self or others")) {
      attributes.add(Attribute.CURSE);
    }
    if (text.contains("(Fancy")) {
      attributes.add(Attribute.FANCY);
    }
    if (text.contains("Meat Pasting component")) {
      attributes.add(Attribute.PASTE);
    }
    if (text.contains("Meatsmithing component")) {
      attributes.add(Attribute.SMITH);
    }
    if (text.contains("Cooking ingredient")) {
      attributes.add(Attribute.COOK);
    }
    if (text.contains("Cocktailcrafting ingredient")) {
      attributes.add(Attribute.MIX);
    }
    if (text.contains("can also be used in combat")) {
      attributes.add(Attribute.COMBAT);
    }
    return attributes;
  }

  private static boolean typesMatch(final ConsumptionType type, final ConsumptionType descType) {
    return switch (type) {
      case NONE, FOOD_HELPER, DRINK_HELPER, STICKER, FOLDER, POKEPILL ->
      // We intentionally disallow certain items from being
      // "used" through the GUI.
      descType == ConsumptionType.NONE || descType == ConsumptionType.USE;
      case EAT,
          DRINK,
          SPLEEN,
          FAMILIAR_HATCHLING,
          FAMILIAR_EQUIPMENT,
          ACCESSORY,
          CONTAINER,
          HAT,
          PANTS,
          SHIRT,
          WEAPON,
          OFFHAND -> descType == type;
      case USE_MESSAGE_DISPLAY, USE, USE_MULTIPLE, USE_INFINITE -> descType == ConsumptionType.USE
          || descType == ConsumptionType.USE_MULTIPLE
          || descType == ConsumptionType.EAT
          || descType == ConsumptionType.DRINK
          || descType == ConsumptionType.AVATAR_POTION
          || descType == ConsumptionType.NONE;
      case POTION, AVATAR_POTION -> descType == ConsumptionType.POTION;
      case CARD, EL_VIBRATO_SPHERE, ZAP -> descType == ConsumptionType.NONE;
      default -> true;
    };
  }

  private static boolean attributesMatch(
      final EnumSet<Attribute> attrs, final EnumSet<Attribute> descAttrs) {
    // If the description says an item is "combat", "(reusable)" or "(on self or others)",
    // our database must mark the item as ATTR_COMBAT, ATTR_COMBAT_REUSABLE, ATTR_CURSE,
    //
    // However, there are quite a few items that we mark with those secondary attributes that are
    // not tagged that way by KoL itself. Assume those are correct.

    if (descAttrs.contains(Attribute.COMBAT)
        && !(attrs.contains(Attribute.COMBAT)
            || attrs.contains(Attribute.COMBAT_REUSABLE)
            || attrs.contains(Attribute.CURSE))) {
      return false;
    }

    if (descAttrs.contains(Attribute.COMBAT_REUSABLE)
        && !attrs.contains(Attribute.COMBAT_REUSABLE)) {
      return false;
    }

    if (descAttrs.contains(Attribute.CURSE) && !attrs.contains(Attribute.CURSE)) {
      return false;
    }

    // If the item is a (Fancy Cooking ingredient) or (Fancy Cocktailcrafting ingredient)
    // we must mark the item with ATTR_FANCY
    if (descAttrs.contains(Attribute.FANCY) != attrs.contains(Attribute.FANCY)) {
      return false;
    }

    // If the item is a Meat Pasting component
    // we must mark the item with ATTR_PASTE
    if (descAttrs.contains(Attribute.PASTE) != attrs.contains(Attribute.PASTE)) {
      return false;
    }

    // If the item is a Meatsmithing component
    // we must mark the item with ATTR_SMITH
    if (descAttrs.contains(Attribute.SMITH) != attrs.contains(Attribute.SMITH)) {
      return false;
    }

    // If the item is a Cooking ingredient
    // we must mark the item with ATTR_COOK
    if (descAttrs.contains(Attribute.COOK) != attrs.contains(Attribute.COOK)) {
      return false;
    }

    // If the item is a Cocktailcrafting ingredient
    // we must mark the item with ATTR_MIX
    return descAttrs.contains(Attribute.MIX) == attrs.contains(Attribute.MIX);
  }

  private static void checkConsumableItems(final PrintStream report) {
    RequestLogger.printLine("Checking level requirements...");

    DebugDatabase.checkConsumableMap(report, DebugDatabase.findItemMap(ConsumptionType.EAT));
    DebugDatabase.checkConsumableMap(report, DebugDatabase.findItemMap(ConsumptionType.DRINK));
    DebugDatabase.checkConsumableMap(report, DebugDatabase.findItemMap(ConsumptionType.SPLEEN));
  }

  private static void checkConsumableMap(final PrintStream report, final ItemMap imap) {
    Map<String, String> map = imap.getMap();
    if (map.size() == 0) {
      return;
    }

    String tag = imap.getTag();
    ConsumptionType type = imap.getType();
    String file =
        type == ConsumptionType.EAT
            ? "fullness"
            : type == ConsumptionType.DRINK ? "inebriety" : "spleenhit";

    RequestLogger.printLine("Checking " + tag + "...");

    report.println();
    report.println("# Level requirements in " + file + ".txt");

    for (Entry<String, String> entry : map.entrySet()) {
      String name = entry.getKey();
      String text = entry.getValue();
      DebugDatabase.checkConsumableDatum(name, type, text, report);
    }
  }

  private static void checkConsumableDatum(
      final String name, final ConsumptionType type, final String text, final PrintStream report) {
    Integer requirement = ConsumablesDatabase.getLevelReqByName(name);
    int level = requirement == null ? 0 : requirement;
    int descLevel = DebugDatabase.parseLevel(text);
    if (level != descLevel) {
      report.println(
          "# *** " + name + " requires level " + level + " but should be " + descLevel + ".");
    }

    int size =
        (type == ConsumptionType.EAT)
            ? ConsumablesDatabase.getFullness(name)
            : (type == ConsumptionType.DRINK)
                ? ConsumablesDatabase.getInebriety(name)
                : (type == ConsumptionType.SPLEEN) ? ConsumablesDatabase.getSpleenHit(name) : 1;

    int descSize = DebugDatabase.parseSize(text);
    if (size != descSize) {
      report.println("# *** " + name + " is size " + size + " but should be " + descSize + ".");
    }

    var quality = ConsumablesDatabase.getQuality(name);
    var descQuality = DebugDatabase.parseQuality(text);
    if (quality != descQuality) {
      report.println(
          "# *** " + name + " is quality " + quality + " but should be " + descQuality + ".");
    }
  }

  private static final Pattern LEVEL_PATTERN = Pattern.compile("Level required: <b>(.*?)</b>");

  public static final int parseLevel(final String text) {
    Matcher matcher = DebugDatabase.LEVEL_PATTERN.matcher(text);
    if (!matcher.find()) {
      return 1;
    }

    return StringUtilities.parseInt(matcher.group(1));
  }

  private static final Pattern SIZE_PATTERN =
      Pattern.compile("(?:Size|Potency|Toxicity): <b>(.*?)</b>");

  public static final int parseSize(final String text) {
    Matcher matcher = DebugDatabase.SIZE_PATTERN.matcher(text);
    if (!matcher.find()) {
      return 1;
    }

    return StringUtilities.parseInt(matcher.group(1));
  }

  public static final int parseConsumableSize(final String text) {
    Matcher matcher = DebugDatabase.SIZE_PATTERN.matcher(text);
    return matcher.find() ? StringUtilities.parseInt(matcher.group(1)) : 0;
  }

  private static void checkEquipment(final PrintStream report) {

    RequestLogger.printLine("Checking equipment...");

    DebugDatabase.checkEquipmentMap(report, DebugDatabase.findItemMap(ConsumptionType.HAT));
    DebugDatabase.checkEquipmentMap(report, DebugDatabase.findItemMap(ConsumptionType.PANTS));
    DebugDatabase.checkEquipmentMap(report, DebugDatabase.findItemMap(ConsumptionType.SHIRT));
    DebugDatabase.checkEquipmentMap(report, DebugDatabase.findItemMap(ConsumptionType.WEAPON));
    DebugDatabase.checkEquipmentMap(report, DebugDatabase.findItemMap(ConsumptionType.OFFHAND));
    DebugDatabase.checkEquipmentMap(report, DebugDatabase.findItemMap(ConsumptionType.ACCESSORY));
    DebugDatabase.checkEquipmentMap(report, DebugDatabase.findItemMap(ConsumptionType.CONTAINER));
  }

  private static void checkEquipmentMap(final PrintStream report, ItemMap imap) {
    Map<String, String> map = imap.getMap();
    if (map.size() == 0) {
      return;
    }

    String tag = imap.getTag();
    RequestLogger.printLine("Checking " + tag + "...");

    report.println();
    report.println("# " + tag + " section of equipment.txt");
    report.println();

    for (Entry<String, String> entry : map.entrySet()) {
      String name = entry.getKey();
      String text = entry.getValue();
      DebugDatabase.checkEquipmentDatum(name, text, report);
    }
  }

  private static void checkEquipmentDatum(
      final String name, final String text, final PrintStream report) {
    String type = DebugDatabase.parseType(text);
    boolean isWeapon = false, isShield = false, hasPower = false;

    if (type.contains("weapon")) {
      isWeapon = true;
    } else if (type.contains("shield")) {
      isShield = true;
    } else if (type.contains("hat") || type.contains("pants") || type.contains("shirt")) {
      hasPower = true;
    }

    int itemId = ItemDatabase.getItemId(name);
    int power;
    if (isWeapon || hasPower) {
      power = DebugDatabase.parsePower(text);
    } else {
      // Until KoL puts off-hand and accessory power into the
      // description, use hand-entered "secret" value.
      power = EquipmentDatabase.getPower(itemId);
    }

    // Now check against what we already have
    int oldPower = EquipmentDatabase.getPower(itemId);
    if (power != oldPower) {
      report.println("# *** " + name + " has power " + oldPower + " but should be " + power + ".");
    }

    String weaponType = isWeapon ? DebugDatabase.parseWeaponType(type) : "";
    String req = DebugDatabase.parseReq(text, type);

    String oldReq = EquipmentDatabase.getEquipRequirement(itemId);
    if (!req.equals(oldReq)) {
      report.println(
          "# *** " + name + " has requirement " + oldReq + " but should be " + req + ".");
    }

    if (isWeapon) {
      int spaceIndex = weaponType.indexOf(" ");
      String oldHanded = EquipmentDatabase.getHands(itemId) + "-handed";

      if (spaceIndex != -1 && !weaponType.startsWith(oldHanded)) {
        String handed = weaponType.substring(0, spaceIndex);
        report.println(
            "# *** " + name + " is marked as " + oldHanded + " but should be " + handed + ".");
      }
    }

    EquipmentDatabase.writeEquipmentItem(report, name, power, req, weaponType, isWeapon, isShield);
  }

  private static final Pattern POWER_PATTERN = Pattern.compile("Power: <b>(\\d+)</b>");
  private static final Pattern DAMAGE_PATTERN_WEAPON =
      Pattern.compile("Damage: <b>[\\d]+ - (\\d+)</b>");

  public static final int parsePower(final String text) {
    Matcher matcher = DebugDatabase.POWER_PATTERN.matcher(text);
    // This should match non-weapon power
    if (matcher.find()) {
      return StringUtilities.parseInt(matcher.group(1));
    }
    // This will match weapon damage and use it to calculate power
    matcher = DebugDatabase.DAMAGE_PATTERN_WEAPON.matcher(text);
    return matcher.find() ? (StringUtilities.parseInt(matcher.group(1)) * 5) : 0;
  }

  private static final Pattern WEAPON_PATTERN = Pattern.compile("weapon [(](.*?)[)]");

  public static final String parseWeaponType(final String text) {
    Matcher matcher = DebugDatabase.WEAPON_PATTERN.matcher(text);
    return matcher.find() ? matcher.group(1) : "";
  }

  private static final Pattern REQ_PATTERN = Pattern.compile("(\\w+) Required: <b>(\\d+)</b>");

  public static final String parseReq(final String text, final String type) {
    Matcher matcher = DebugDatabase.REQ_PATTERN.matcher(text);
    if (matcher.find()) {
      String stat = matcher.group(1);
      if (stat.equals("Muscle")) {
        return "Mus: " + matcher.group(2);
      }
      if (stat.equals("Mysticality")) {
        return "Mys: " + matcher.group(2);
      }
      if (stat.equals("Moxie")) {
        return "Mox: " + matcher.group(2);
      }
    }

    if (type.contains("weapon")) {
      if (type.contains("ranged")) {
        return "Mox: 0";
      } else if (type.contains("utensil")
          || type.contains("saucepan")
          || type.contains("chefstaff")) {
        return "Mys: 0";
      } else {
        return "Mus: 0";
      }
    }

    return "none";
  }

  private static final Pattern FULLNESS_PATTERN = Pattern.compile("Size: <b>(\\d+)</b>");

  public static final Integer parseFullness(final String text) {
    Matcher matcher = DebugDatabase.FULLNESS_PATTERN.matcher(text);
    return matcher.find() ? (StringUtilities.parseInt(matcher.group(1))) : null;
  }

  private static final Pattern INEBRIETY_PATTERN = Pattern.compile("Potency: <b>(\\d+)</b>");

  public static final Integer parseInebriety(final String text) {
    Matcher matcher = DebugDatabase.INEBRIETY_PATTERN.matcher(text);
    return matcher.find() ? (StringUtilities.parseInt(matcher.group(1))) : null;
  }

  private static final Pattern TOXICITY_PATTERN = Pattern.compile("Toxicity: <b>(\\d+)</b>");

  public static final Integer parseToxicity(final String text) {
    Matcher matcher = DebugDatabase.TOXICITY_PATTERN.matcher(text);
    return matcher.find() ? (StringUtilities.parseInt(matcher.group(1))) : null;
  }

  private static final Pattern FAMILIAR_PATTERN = Pattern.compile("Familiar: <b>(.*?)</b>");

  public static final String parseFamiliar(final String text) {
    Matcher matcher = DebugDatabase.FAMILIAR_PATTERN.matcher(text);
    return matcher.find() ? (matcher.group(1)) : "any";
  }

  private static void checkItemModifiers(final PrintStream report) {
    RequestLogger.printLine("Checking modifiers...");

    DebugDatabase.checkItemModifierMap(report, DebugDatabase.findItemMap(ConsumptionType.HAT));
    DebugDatabase.checkItemModifierMap(report, DebugDatabase.findItemMap(ConsumptionType.PANTS));
    DebugDatabase.checkItemModifierMap(report, DebugDatabase.findItemMap(ConsumptionType.SHIRT));
    DebugDatabase.checkItemModifierMap(report, DebugDatabase.findItemMap(ConsumptionType.WEAPON));
    DebugDatabase.checkItemModifierMap(report, DebugDatabase.findItemMap(ConsumptionType.OFFHAND));
    DebugDatabase.checkItemModifierMap(
        report, DebugDatabase.findItemMap(ConsumptionType.ACCESSORY));
    DebugDatabase.checkItemModifierMap(
        report, DebugDatabase.findItemMap(ConsumptionType.CONTAINER));
    DebugDatabase.checkItemModifierMap(
        report, DebugDatabase.findItemMap(ConsumptionType.FAMILIAR_EQUIPMENT));
    DebugDatabase.checkItemModifierMap(
        report, DebugDatabase.findItemMap(ConsumptionType.EAT), false);
    DebugDatabase.checkItemModifierMap(
        report, DebugDatabase.findItemMap(ConsumptionType.DRINK), false);
    DebugDatabase.checkItemModifierMap(
        report, DebugDatabase.findItemMap(ConsumptionType.SPLEEN), false);
    DebugDatabase.checkItemModifierMap(
        report, DebugDatabase.findItemMap(ConsumptionType.POTION), false);
    DebugDatabase.checkItemModifierMap(
        report, DebugDatabase.findItemMap(ConsumptionType.AVATAR_POTION), false);
    DebugDatabase.checkItemModifierMap(
        report, DebugDatabase.findItemMap(ConsumptionType.UNKNOWN), false);
  }

  private static void checkItemModifierMap(final PrintStream report, final ItemMap imap) {
    DebugDatabase.checkItemModifierMap(report, imap, true);
  }

  private static void checkItemModifierMap(
      final PrintStream report, final ItemMap imap, final boolean showAll) {
    Map<String, String> map = imap.getMap();
    if (map.size() == 0) {
      return;
    }

    String tag = imap.getTag();
    RequestLogger.printLine("Checking " + tag + "...");

    report.println();
    report.println("# " + tag + " section of modifiers.txt");
    report.println();

    ConsumptionType type = imap.getType();
    for (Entry<String, String> entry : map.entrySet()) {
      String name = entry.getKey();
      String text = entry.getValue();
      DebugDatabase.checkItemModifierDatum(name, text, type, report, showAll);
    }
  }

  private static void checkItemModifierDatum(
      final String name,
      final String text,
      final ConsumptionType type,
      final PrintStream report,
      final boolean showAll) {
    ModifierList known = new ModifierList();
    ArrayList<String> unknown = new ArrayList<>();

    // Get the known and unknown modifiers from the item description
    DebugDatabase.parseItemEnchantments(text, known, unknown, type);

    // Compare to what is already registered, logging differences
    // and substituting expressions, as appropriate.
    DebugDatabase.checkModifiers(ModifierType.ITEM, name, known, true, report);

    // Print the modifiers in the format modifiers.txt expects.
    if (showAll || known.size() > 0 || unknown.size() > 0) {
      DebugDatabase.logModifierDatum(ModifierType.ITEM, name, known, unknown, report);
    }
  }

  private static void checkModifiers(
      final ModifierType type,
      final String name,
      final ModifierList known,
      final boolean appendCurrent,
      final PrintStream report) {
    // - Keep modifiers in the same order they are listed in the item description
    // - If a modifier is variable (has an expression), evaluate
    //   the expression and compare to the number in the description
    // - List extra modifiers (Familiar Effect, for example) at end
    //   of parsed modifiers in the order they appear in modifiers.txt

    // Get the existing modifiers for the name
    ModifierList existing = ModifierDatabase.getModifierList(new Lookup(type, name));

    // Look at each modifier in known
    for (ModifierValue modifier : known) {
      String key = modifier.getName();
      String value = modifier.getValue();

      ModifierValue current = existing.removeModifier(key);
      if (current != null) {
        String currentValue = current.getValue();
        if (currentValue == null) {
          continue; // No value
        }
        if (value == null) {
          // This is a bug - somewhere - in our parsing.
          //
          // It only seems to happen with Thorns.  That supposedly has a numeric value
          // (a range, actually), but several matching patterns don't have one.
          report.println("# *** modifier " + key + ": " + currentValue + " missing a value");
          continue;
        }

        if (currentValue.contains("[")) {
          int lbracket = currentValue.indexOf("[");
          int rbracket = currentValue.indexOf("]");

          if (ModifierDatabase.isNumericModifier(key)) {
            // Evaluate the expression
            String expression = currentValue.substring(lbracket + 1, rbracket);

            // Kludge: KoL no longer takes Reagent Potion duration
            // into account in item descriptions.
            if (key.equals("Effect Duration") && expression.contains("R")) {
              expression = StringUtilities.singleStringReplace(expression, "R", "5");
            }

            ModifierExpression expr = new ModifierExpression(expression, type, name);
            if (expr.hasErrors()) {
              report.println(expr.getExpressionErrors());
            } else {
              int descValue = StringUtilities.parseInt(value);
              int modValue = (int) expr.eval();
              if (descValue != modValue) {
                report.println(
                    "# *** modifier "
                        + key
                        + ": "
                        + currentValue
                        + " evaluates to "
                        + modValue
                        + " but description says "
                        + descValue);
              }
            }

            // Keep the expression, regardless
            modifier.setValue(currentValue);
            continue;
          }

          if (key.equals("Effect") || key.equals("Rollover Effect")) {
            if (!currentValue.equals(value) && !decodedNamesEqual(currentValue, value)) {
              // Effect does not match
              report.println(
                  "# *** modifier "
                      + key
                      + ": "
                      + currentValue
                      + " should be "
                      + key
                      + ": "
                      + value);
            } else {
              modifier.setValue(currentValue);
            }
            continue;
          }
        }

        // If the value is not an expression, it must match exactly
        // (modulo entity encoding)
        // Some names actually have double spaces.
        // Some names have a single space but are displayed with a double space. Sometimes.
        String normalizedCurrentValue =
            StringUtilities.globalStringReplace(currentValue, "  ", " ").trim();
        String normalizedValue = StringUtilities.globalStringReplace(value, "  ", " ").trim();
        if (!currentValue.equals(value)
            && !normalizedCurrentValue.equals(normalizedValue)
            && !decodedNamesEqual(currentValue, value)
            && !decodedNamesEqual(normalizedCurrentValue, normalizedValue)) {
          report.println(
              "# *** modifier " + key + ": " + currentValue + " should be " + key + ": " + value);
        }
      } else if (value == null) {
        report.println("# *** new enchantment: " + key + " seen");
      } else {
        report.println("# *** new enchantment: " + key + ": " + value + " seen");
      }
    }

    if (appendCurrent) {
      // Add all modifiers in existing list that were not seen in description to "known"
      known.addAll(existing);
    } else {
      for (ModifierValue modifier : existing) {
        String key = modifier.getName();
        String value = modifier.getValue();
        if (value == null) {
          report.println("# *** bogus enchantment: " + key);
        } else {
          report.println("# *** bogus enchantment: " + key + ": " + value);
        }
      }
    }
  }

  private static void logModifierDatum(
      final ModifierType type,
      final String name,
      final ModifierList known,
      final ArrayList<String> unknown,
      final PrintStream report) {
    for (String s : unknown) {
      ModifierDatabase.writeModifierComment(report, name, s);
    }

    if (known.size() == 0) {
      if (unknown.size() == 0) {
        ModifierDatabase.writeModifierComment(report, null, name);
      }
    } else {
      ModifierDatabase.writeModifierString(report, type, name, known.toString());
    }
  }

  private static final Pattern ITEM_ENCHANTMENT_PATTERN =
      Pattern.compile(
          "<font color=\"?blue\"?>(?!\\(awesome\\)|<p>)(.*)(?:<br>)?</font>(?:<br />)?",
          Pattern.DOTALL);

  public static final void parseItemEnchantments(
      String text,
      final ModifierList known,
      final ArrayList<String> unknown,
      final ConsumptionType type) {
    // KoL now includes the enchantments of the effect in the item
    // descriptions. Strip them out.
    int eindex = text.indexOf("Effect:");
    if (eindex != -1) {
      int spanstart = text.indexOf("<span", eindex);
      int spanend = text.indexOf("</span>", eindex);
      if (spanstart != -1 && spanend != -1) {
        String span = text.substring(spanstart, spanend + 7);
        text = StringUtilities.singleStringDelete(text, span);
      }
    }

    DebugDatabase.parseStandardEnchantments(
        text, known, unknown, DebugDatabase.ITEM_ENCHANTMENT_PATTERN);

    // Several modifiers can appear outside the "Enchantments"
    // section of the item description.

    // If we extracted Damage Reduction from the enchantments, we
    // included shield DR as well, but for shields that have no
    // enchantments, get DR here.
    if (!known.containsModifier("Damage Reduction")) {
      DebugDatabase.appendModifier(known, ModifierDatabase.parseDamageReduction(text));
    }

    DebugDatabase.appendModifier(known, ModifierDatabase.parseSkill(text));
    DebugDatabase.appendModifier(known, ModifierDatabase.parseSingleEquip(text));
    DebugDatabase.appendModifier(known, ModifierDatabase.parseSoftcoreOnly(text));
    DebugDatabase.appendModifier(known, ModifierDatabase.parseLastsOneDay(text));
    DebugDatabase.appendModifier(known, ModifierDatabase.parseFreePull(text));
    DebugDatabase.appendModifier(known, ModifierDatabase.parseEffect(text));
    DebugDatabase.appendModifier(known, ModifierDatabase.parseEffectDuration(text));
    DebugDatabase.appendModifier(known, ModifierDatabase.parseSongDuration(text));
    DebugDatabase.appendModifier(known, ModifierDatabase.parseDropsItems(text));

    if (type == ConsumptionType.FAMILIAR_EQUIPMENT) {
      String familiar = DebugDatabase.parseFamiliar(text);
      if (familiar.equals("any")) {
        DebugDatabase.appendModifier(known, "Generic");
      }
    }
  }

  private static final Pattern RESTORE_RANGE_PATTERN =
      Pattern.compile("(\\d+)-(\\d+) (?:HP|MP|Hit Points)");
  private static final Pattern RESTORE_RANGE2_PATTERN =
      Pattern.compile("(\\d+)-(\\d+) HP and (\\d+)-(\\d+) MP");
  private static final Pattern RESTORE_UPTO_PATTERN = Pattern.compile("up to (.*?) (?:HP|MP)");

  public static final void parseRestores(final String name, final String text) {
    Matcher enchantMatcher = DebugDatabase.ITEM_ENCHANTMENT_PATTERN.matcher(text);
    if (!enchantMatcher.find()) {
      return;
    }
    String enchant = enchantMatcher.group(1);
    if (!enchant.contains("Restores") && !enchant.contains("Heals")) {
      return;
    }
    String hpmin = "0";
    String hpmax = "0";
    String mpmin = "0";
    String mpmax = "0";
    Matcher matcher = DebugDatabase.RESTORE_RANGE_PATTERN.matcher(enchant);
    if (matcher.find()) {
      if (enchant.contains("HP") || enchant.contains("Hit Points")) {
        hpmin = matcher.group(1);
        hpmax = matcher.group(2);
      }
      if (enchant.contains("MP")) {
        mpmin = matcher.group(1);
        mpmax = matcher.group(2);
      }
    }
    matcher = DebugDatabase.RESTORE_RANGE2_PATTERN.matcher(enchant);
    if (matcher.find()) {
      hpmin = matcher.group(1);
      hpmax = matcher.group(2);
      mpmin = matcher.group(3);
      mpmax = matcher.group(4);
    }
    matcher = DebugDatabase.RESTORE_UPTO_PATTERN.matcher(enchant);
    if (matcher.find()) {
      if (enchant.contains("HP")) {
        hpmin = hpmax = matcher.group(1);
      }
      if (enchant.contains("MP")) {
        mpmin = mpmax = matcher.group(1);
      }
    }
    if (enchant.contains("all")) {
      if (enchant.contains("HP")) {
        hpmin = hpmax = "[HP]";
      }
      if (enchant.contains("MP")) {
        mpmin = mpmax = "[MP]";
      }
    }
    RestoresDatabase.setValue(name, "item", hpmin, hpmax, mpmin, mpmax, 0, -1, null);
    String printMe = name + "\titem\t" + hpmin + "\t" + hpmax + "\t" + mpmin + "\t" + mpmax + "\t0";
    RequestLogger.printLine(printMe);
    RequestLogger.updateSessionLog(printMe);
  }

  public static final String parseItemEnchantments(
      final String text, final ArrayList<String> unknown, final ConsumptionType type) {
    ModifierList known = new ModifierList();
    DebugDatabase.parseItemEnchantments(text, known, unknown, type);
    return known.toString();
  }

  public static final String parseItemEnchantments(final String text, final ConsumptionType type) {
    ModifierList known = new ModifierList();
    ArrayList<String> unknown = new ArrayList<>();
    DebugDatabase.parseItemEnchantments(text, known, unknown, type);
    return known.toString();
  }

  public static void parseStandardEnchantments(
      final String text,
      final ModifierList known,
      final ArrayList<String> unknown,
      final Pattern pattern) {
    Matcher matcher = pattern.matcher(text);
    if (!matcher.find()) {
      return;
    }

    // The matcher has removed the "blue" <font> tags which
    // surrounds all the enchantments. The enchantments are
    // separated by <br> tags.
    //
    // <font> tags can legitimately appear within enchantments.
    //
    // Compare:
    //
    // 8-billed baseball cap
    //
    // <font color=blue>
    //  Muscle +15%<br>
    //  Combat Initiative +30%<br>
    //  +5 <font color=gray>Spooky Damage</font><br>
    //  +5 <font color=green>Stench Damage</font><br>
    //  +5 <font color=red>Hot Damage</font><br>
    //  +5 <font color=blue>Cold Damage</font><br>
    //  +5 <font color=blueviolet>Sleaze Damage</font><br>
    //  &nbsp;<br>
    // </font>
    //
    // which has font tags embedded within individual enchantments, with:
    //
    // Kremlin's Greatest Briefcase
    //
    // <font color=blue>
    //  All Attributes +10<br>
    //  Maximum HP/MP +25<br>
    //  <font color="blue">
    //    Weapon Damage +25%<br>
    //    +25% Combat Initiative<br>
    //    Regenerate 5-10 MP per Adventure<br>
    //    Regenerate 5-10 HP per Adventure
    //  </font><br>
    //  Lets you banish enemy agents with tranquilizer darts
    // </font>
    //
    // which has a subset of the enchantments embedded within a
    // blue font tags.
    //
    // mime army infiltration glove
    //
    // <font color=blue>
    //  +25% Pickpocket Chance<br>
    //  Combat Initiative -200%<br>
    //  <font color="blue">
    //  </font>
    //  Allows pickpocketing
    // </font>
    //
    // Which is just weird.

    StringBuffer enchantments = new StringBuffer(matcher.group(1));

    StringUtilities.globalStringDelete(
        enchantments,
        "<b>NOTE:</b> Items that reduce the MP cost of skills will not do so by more than 3 points, in total.");
    StringUtilities.globalStringReplace(enchantments, "<br>", "\n");
    StringUtilities.globalStringReplace(enchantments, "<Br>", "\n");
    StringUtilities.globalStringReplace(enchantments, "<br />", "\n");
    // Following from bogus HTML in Two Crazy Random Summer
    StringUtilities.globalStringReplace(enchantments, "</font></b></center>", "\n");
    // Following from bogus HTML for mime army infiltration glove
    StringUtilities.globalStringReplace(enchantments, "<font color=\"blue\"></font>", "");

    String[] mods = enchantments.toString().split("\n+");
    String BLUE_START = "<font color=\"blue\">";
    String BLUE_END = "</font>";

    boolean decemberEvent = false;

    for (String s : mods) {
      String enchantment = s.trim();
      if (enchantment.equals("")) {
        continue;
      }

      // Unfortunately, since KoL has removed any indication
      // other than blue font to indicate what is an
      // enchantment, "awesome" as a food quality matches.
      if (enchantment.equals("(awesome)")) {
        continue;
      }

      // Trim each enchantment to account for embedded blue
      // font blocks.  We don't have actual examples of all
      // of the following, but allow for them.
      //
      // Remove font tags from the following:
      //
      // 1) <font color="blue">The first embedded enchantment
      // 2) The last embedded enchantment</font>
      // 3) <font color="blue">The only embedded enchantment</font>
      //
      // Do not remove font tags from the following:
      //
      // 4) <font color="blue">Cold</font> enchantment
      // 5) +5 <font color="blue">Cold Damage</font>

      // 1, 3, or 4
      if (enchantment.startsWith(BLUE_START)) {
        // 1 or 3
        if (!enchantment.contains(BLUE_END) || enchantment.endsWith(BLUE_END)) {
          enchantment = enchantment.substring(BLUE_START.length());
        }
      }

      // 2, 3, 5
      if (enchantment.endsWith(BLUE_END)) {
        // 2 or 3
        if (!enchantment.contains("<font")) {
          enchantment = enchantment.substring(0, enchantment.length() - BLUE_END.length());
        }
      }

      String mod = ModifierDatabase.parseModifier(enchantment);
      if (mod != null) {
        // Rollover Effect and Rollover Effect Duration come together
        // Modifiers parses the numeric modifier first
        if (mod.startsWith("Rollover Effect Duration")) {
          String effect = ModifierDatabase.parseStringModifier(enchantment);
          if (effect != null) {
            DebugDatabase.appendModifier(known, effect);
          }
        }

        // Damage Reduction can appear in several
        // places. Combine them all.
        else if (mod.startsWith("Damage Reduction")) {
          mod = ModifierDatabase.parseDamageReduction(text);
        } else if (mod.equals("Class: \"December\"")) {
          decemberEvent = true;
          continue;
        }

        DebugDatabase.appendModifier(known, mod);
        continue;
      }

      if (!unknown.contains(enchantment)) {
        unknown.add(enchantment);
      }
    }

    if (decemberEvent) {
      for (ModifierValue m : known) {
        m.setValue("[" + m.getValue() + "*event(December)]");
      }
    }
  }

  private static void appendModifier(final ModifierList known, final String mod) {
    if (mod != null) {
      // If the value contains a quoted string, it can contain commas
      if (mod.contains("\"") || !mod.contains(",")) {
        known.addToModifier(DebugDatabase.makeModifier(mod));
        return;
      }

      // Otherwise, certain modifiers - "All Attributes: +5" - turn into multiple modifiers
      String[] mods = mod.split(",");
      for (String s : mods) {
        known.addToModifier(DebugDatabase.makeModifier(s));
      }
    }
  }

  private static ModifierValue makeModifier(final String mod) {
    int colon = mod.indexOf(":");
    String key = colon == -1 ? mod.trim() : mod.substring(0, colon).trim();
    String value = colon == -1 ? null : mod.substring(colon + 1).trim();
    return new ModifierValue(key, value);
  }

  // **********************************************************

  // Support for the "checkoutfits" command, which compares KoLmafia's
  // internal outfit data from what can be mined from the outfit
  // description.

  private static final String OUTFIT_HTML = "outfithtml.txt";
  private static final String OUTFIT_DATA = "outfitdata.txt";
  private static final Map<Integer, String> rawOutfits = new HashMap<>();
  private static final ItemMap outfits = new ItemMap("Outfits", ConsumptionType.NONE);

  public static final void checkOutfits() {
    RequestLogger.printLine("Loading previous data...");
    DebugDatabase.loadScrapeData(rawOutfits, OUTFIT_HTML);

    RequestLogger.printLine("Checking internal data...");

    PrintStream report = DebugDatabase.openReport(OUTFIT_DATA);

    DebugDatabase.outfits.clear();
    DebugDatabase.checkOutfits(report);
    DebugDatabase.checkOutfitModifierMap(report);

    report.close();
  }

  private static void checkOutfits(final PrintStream report) {
    Set<Integer> keys = EquipmentDatabase.normalOutfits.keySet();
    int lastId = 0;

    for (Integer id : keys) {
      if (id < 1) {
        continue;
      }

      while (++lastId < id) {
        report.println(lastId);
      }

      DebugDatabase.checkOutfit(id, report);
    }

    DebugDatabase.saveScrapeData(keys.iterator(), rawOutfits, OUTFIT_HTML);
  }

  private static void checkOutfit(final int outfitId, final PrintStream report) {
    SpecialOutfit outfit = EquipmentDatabase.normalOutfits.get(outfitId);
    String name = outfit.getName();
    if (name == null) {
      report.println(outfitId);
      return;
    }

    String rawText = DebugDatabase.rawOutfitDescriptionText(outfitId);

    if (rawText == null) {
      report.println("# *** " + name + " (" + outfitId + ") has no description.");
      return;
    }

    String text = DebugDatabase.outfitDescriptionText(rawText);
    if (text == null) {
      report.println("# *** " + name + " (" + outfitId + ") has malformed description text.");
      DebugDatabase.rawOutfits.put(outfitId, null);
      return;
    }

    String image = outfit.getImage();
    String descImage = DebugDatabase.parseImage(rawText);
    if (image != null && !image.equals(descImage)) {
      report.println(
          "# *** "
              + name
              + " ("
              + outfitId
              + ") has image of "
              + image
              + " but should be "
              + descImage
              + ".");
    }

    report.println(EquipmentDatabase.outfitString(outfitId, name, descImage));

    DebugDatabase.outfits.put(name, text);
  }

  public static final String outfitDescriptionText(final int outfitId) {
    return DebugDatabase.outfitDescriptionText(DebugDatabase.rawOutfitDescriptionText(outfitId));
  }

  public static final String readOutfitDescriptionText(final int outfitId) {
    GenericRequest request = new GenericRequest("desc_outfit.php");
    request.addFormField("whichoutfit", String.valueOf(outfitId));
    RequestThread.postRequest(request);
    return request.responseText;
  }

  public static final String rawOutfitDescriptionText(final int outfitId) {
    String previous = DebugDatabase.rawOutfits.get(outfitId);
    if (previous != null && !previous.equals("")) {
      return previous;
    }

    String text = DebugDatabase.readOutfitDescriptionText(outfitId);
    DebugDatabase.rawOutfits.put(outfitId, text);

    return text;
  }

  private static final Pattern OUTFIT_DATA_PATTERN =
      Pattern.compile("<div id=\"description\"[^>]*>(.*?)</div>", Pattern.DOTALL);

  public static final String outfitDescriptionText(final String rawText) {
    if (rawText == null) {
      return null;
    }

    Matcher matcher = DebugDatabase.OUTFIT_DATA_PATTERN.matcher(rawText);
    if (!matcher.find()) {
      return null;
    }

    return matcher.group(1);
  }

  private static void checkOutfitModifierMap(final PrintStream report) {
    Map<String, String> map = DebugDatabase.outfits.getMap();
    if (map.size() == 0) {
      return;
    }

    String tag = DebugDatabase.outfits.getTag();

    report.println();
    report.println("# " + tag + " section of modifiers.txt");
    report.println();

    for (Entry<String, String> entry : map.entrySet()) {
      String name = entry.getKey();
      String text = entry.getValue();
      DebugDatabase.checkOutfitModifierDatum(name, text, report);
    }
  }

  private static void checkOutfitModifierDatum(
      final String name, final String text, final PrintStream report) {
    ModifierList known = new ModifierList();
    ArrayList<String> unknown = new ArrayList<>();

    // Get the known and unknown modifiers from the outfit description
    DebugDatabase.parseOutfitEnchantments(text, known, unknown);

    // Compare to what is already registered.
    // Log differences and substitute formulas, as appropriate.
    DebugDatabase.checkModifiers(ModifierType.OUTFIT, name, known, false, report);

    // Print the modifiers in the format modifiers.txt expects.
    DebugDatabase.logModifierDatum(ModifierType.OUTFIT, name, known, unknown, report);
  }

  private static final Pattern OUTFIT_ENCHANTMENT_PATTERN =
      Pattern.compile("<b><font color=blue>(.*)</font></b>", Pattern.DOTALL);

  public static final void parseOutfitEnchantments(
      final String text, final ModifierList known, final ArrayList<String> unknown) {
    DebugDatabase.parseStandardEnchantments(
        text, known, unknown, DebugDatabase.OUTFIT_ENCHANTMENT_PATTERN);
  }

  public static final String parseOutfitEnchantments(
      final String text, final ArrayList<String> unknown) {
    ModifierList known = new ModifierList();
    DebugDatabase.parseOutfitEnchantments(text, known, unknown);
    return known.toString();
  }

  // **********************************************************

  // Support for the "checkeffects" command, which compares KoLmafia's
  // internal status effect data from what can be mined from the effect
  // description.

  private static final String EFFECT_HTML = "effecthtml.txt";
  private static final String EFFECT_DATA = "effectdata.txt";
  private static final Map<Integer, String> rawEffects = new HashMap<>();
  private static final ItemMap effects = new ItemMap("Status Effects", ConsumptionType.NONE);

  public static final void checkEffects(final int effectId) {
    RequestLogger.printLine("Loading previous data...");
    DebugDatabase.loadScrapeData(rawEffects, EFFECT_HTML);

    RequestLogger.printLine("Checking internal data...");

    PrintStream report = DebugDatabase.openReport(EFFECT_DATA);

    DebugDatabase.effects.clear();

    if (effectId == 0) {
      DebugDatabase.checkEffects(report);
    } else {
      DebugDatabase.checkEffect(effectId, report);
    }

    DebugDatabase.checkEffectModifiers(report);

    report.close();
  }

  private static void checkEffects(final PrintStream report) {
    Set<Integer> keys = EffectDatabase.descriptionIdKeySet();

    for (Integer key : keys) {
      int id = key;
      if (id < 1) {
        continue;
      }

      DebugDatabase.checkEffect(id, report);
    }

    DebugDatabase.saveScrapeData(keys.iterator(), rawEffects, EFFECT_HTML);
  }

  private static void checkEffect(final int effectId, final PrintStream report) {
    String name = EffectDatabase.getEffectName(effectId);
    if (name == null) {
      return;
    }

    String rawText = DebugDatabase.rawEffectDescriptionText(effectId);

    if (rawText == null) {
      report.println("# *** " + name + " (" + effectId + ") has no description.");
      return;
    }

    String text = DebugDatabase.effectDescriptionText(rawText);
    if (text == null) {
      report.println("# *** " + name + " (" + effectId + ") has malformed description text.");
      DebugDatabase.rawEffects.put(effectId, null);
      return;
    }

    int id = DebugDatabase.parseEffectId(text);
    if (id != effectId) {
      report.println("# *** " + name + " (" + effectId + ") should have effectId " + id + ".");
    }

    String descriptionName = DebugDatabase.parseName(text);
    // Kludge to adjust known defective effect descriptions
    if (effectId == 1659) {
      descriptionName = StringUtilities.globalStringReplace(descriptionName, "  ", " ");
    }
    if (!name.equals(descriptionName) && !decodedNamesEqual(name, descriptionName)) {
      report.println(
          "# *** " + name + " (" + effectId + ") has description of " + descriptionName + ".");
      return;
    }

    String descriptionImage = DebugDatabase.parseImage(rawText);
    if (!descriptionImage.equals(EffectDatabase.getImageName(id))) {
      report.println(
          "# *** " + name + " (" + effectId + ") has image of " + descriptionImage + ".");
    }

    DebugDatabase.effects.put(name, text);
  }

  // <!-- effectid: 806 -->
  private static final Pattern EFFECTID_PATTERN = Pattern.compile("<!-- effectid: ([\\d]*) -->");

  public static final int parseEffectId(final String text) {
    Matcher matcher = DebugDatabase.EFFECTID_PATTERN.matcher(text);
    if (!matcher.find()) {
      return 0;
    }

    return StringUtilities.parseInt(matcher.group(1));
  }

  // http://images.kingdomofloathing.com/itemimages/hp.gif
  // http://images.kingdomofloathing.com/otherimages/folders/folder22.gif
  // http://images.kingdomofloathing.com/otherimages/sigils/workouttat.gif
  private static final Pattern IMAGE_PATTERN =
      Pattern.compile("(?:cloudfront.net|images.kingdomofloathing.com)/(.*?\\.gif)");

  public static final String parseImage(final String text) {
    Matcher matcher = DebugDatabase.IMAGE_PATTERN.matcher(text);
    String path = matcher.find() ? matcher.group(1) : "";
    String prefix1 = "itemimages/";
    String prefix2 = "otherimages/sigils/";
    return path.startsWith(prefix1)
        ? path.substring(prefix1.length())
        : path.startsWith(prefix2) ? path.substring(prefix2.length()) : path;
  }

  // href="desc_effect.php?whicheffect=138ba5cbeccb6334a1d473710372e8d6"
  private static final Pattern EFFECT_DESCID_PATTERN = Pattern.compile("whicheffect=(.*?)\"");

  public static final String parseEffectDescid(final String text) {
    Matcher matcher = DebugDatabase.EFFECT_DESCID_PATTERN.matcher(text);
    return matcher.find() ? matcher.group(1) : "";
  }

  public static final String effectDescriptionText(final int effectId) {
    return DebugDatabase.effectDescriptionText(DebugDatabase.rawEffectDescriptionText(effectId));
  }

  public static String readEffectDescriptionText(final int effectId) {
    String descId = EffectDatabase.getDescriptionId(effectId);
    if (descId == null || descId.equals("")) {
      return null;
    }
    return DebugDatabase.readEffectDescriptionText(descId);
  }

  public static final String readEffectDescriptionText(final String descId) {
    GenericRequest request = new GenericRequest("desc_effect.php");
    request.addFormField("whicheffect", descId);
    RequestThread.postRequest(request);
    return request.responseText;
  }

  private static String rawEffectDescriptionText(final int effectId) {
    String descId = EffectDatabase.getDescriptionId(effectId);
    if (descId == null || descId.equals("")) {
      return null;
    }

    String previous = DebugDatabase.rawEffects.get(effectId);
    if (previous != null && !previous.equals("")) {
      return previous;
    }

    String text = DebugDatabase.readEffectDescriptionText(descId);
    DebugDatabase.rawEffects.put(effectId, text);

    return text;
  }

  private static final Pattern EFFECT_DATA_PATTERN =
      Pattern.compile("<div id=\"description\"[^>]*>(.*?)</div>", Pattern.DOTALL);

  private static String effectDescriptionText(final String rawText) {
    if (rawText == null) {
      return null;
    }

    Matcher matcher = DebugDatabase.EFFECT_DATA_PATTERN.matcher(rawText);
    if (!matcher.find()) {
      return null;
    }

    return matcher.group(1);
  }

  private static void checkEffectModifiers(final PrintStream report) {
    RequestLogger.printLine("Checking modifiers...");

    DebugDatabase.checkEffectModifierMap(report);
  }

  private static void checkEffectModifierMap(final PrintStream report) {
    Map<String, String> map = DebugDatabase.effects.getMap();
    if (map.size() == 0) {
      return;
    }

    String tag = DebugDatabase.effects.getTag();

    report.println();
    report.println("# " + tag + " section of modifiers.txt");
    report.println();

    for (Entry<String, String> entry : map.entrySet()) {
      String name = entry.getKey();
      String text = entry.getValue();
      DebugDatabase.checkEffectModifierDatum(name, text, report);
    }
  }

  private static final Pattern EFFECT_ENCHANTMENT_PATTERN =
      Pattern.compile("<font color=blue><b>(.*)</b></font>", Pattern.DOTALL);

  public static final void parseEffectEnchantments(
      final String text, final ModifierList known, final ArrayList<String> unknown) {
    DebugDatabase.parseStandardEnchantments(
        text, known, unknown, DebugDatabase.EFFECT_ENCHANTMENT_PATTERN);
  }

  public static final String parseEffectEnchantments(
      final String text, final ArrayList<String> unknown) {
    ModifierList known = new ModifierList();
    DebugDatabase.parseEffectEnchantments(text, known, unknown);
    return known.toString();
  }

  public static final String parseEffectEnchantments(final String text) {
    ArrayList<String> unknown = new ArrayList<>();
    return DebugDatabase.parseEffectEnchantments(text, unknown);
  }

  private static void checkEffectModifierDatum(
      final String name, final String text, final PrintStream report) {
    ModifierList known = new ModifierList();
    ArrayList<String> unknown = new ArrayList<>();

    // Get the known and unknown modifiers from the effect description
    DebugDatabase.parseEffectEnchantments(text, known, unknown);

    // Compare to what is already registered.
    // Log differences and substitute formulas, as appropriate.
    DebugDatabase.checkModifiers(ModifierType.EFFECT, name, known, true, report);

    // Print the modifiers in the format modifiers.txt expects.
    DebugDatabase.logModifierDatum(ModifierType.EFFECT, name, known, unknown, report);
  }

  // **********************************************************

  // Support for the "checkskills" command, which compares KoLmafia's
  // internal skill data with what can be mined from the skill
  // description.

  private static final String SKILL_HTML = "skillhtml.txt";
  private static final String SKILL_DATA = "skilldata.txt";
  private static final Map<Integer, String> rawSkills = new HashMap<>();
  private static final ItemMap passiveSkills = new ItemMap("Passive Skills", ConsumptionType.NONE);

  public static final void checkSkills(final int skillId) {
    RequestLogger.printLine("Loading previous data...");
    DebugDatabase.loadScrapeData(rawSkills, SKILL_HTML);

    RequestLogger.printLine("Checking internal data...");

    PrintStream report = DebugDatabase.openReport(SKILL_DATA);

    DebugDatabase.passiveSkills.clear();

    if (skillId == 0) {
      DebugDatabase.checkSkills(report);
    } else {
      DebugDatabase.checkSkill(skillId, report);
    }

    DebugDatabase.checkSkillModifiers(report);

    report.close();
  }

  private static void checkSkills(final PrintStream report) {
    Set<Integer> keys = SkillDatabase.idKeySet();
    for (Integer value : keys) {
      int id = value;
      if (id < 1) {
        continue;
      }

      DebugDatabase.checkSkill(id, report);
    }

    DebugDatabase.saveScrapeData(keys.iterator(), rawSkills, SKILL_HTML);
  }

  private static void checkSkill(final int skillId, final PrintStream report) {
    String name = SkillDatabase.getSkillName(skillId);
    if (name == null) {
      return;
    }

    String rawText = DebugDatabase.rawSkillDescriptionText(skillId);

    if (rawText == null) {
      report.println("# *** " + name + " (" + skillId + ") has no description.");
      return;
    }

    String text = DebugDatabase.skillDescriptionText(rawText);
    if (text == null) {
      report.println("# *** " + name + " (" + skillId + ") not found.");
      DebugDatabase.rawSkills.put(skillId, null);
      return;
    }

    String descriptionImage = DebugDatabase.parseImage(rawText);
    if (!descriptionImage.equals(SkillDatabase.getSkillImage(skillId))) {
      report.println("# *** " + name + " (" + skillId + ") has image of " + descriptionImage + ".");
    }

    String type = DebugDatabase.parseSkillType(text);
    if (type.equals("Passive")) {
      DebugDatabase.passiveSkills.put(name, text);
    }
  }

  // Grants Skill: <a class=hand
  // onClick='javascript:poop("desc_skill.php?whichskill=163&self=true","skill", 350,
  // 300)'><b>Gingerbread Mob Hit</b></a>
  private static final Pattern SKILL_ID_PATTERN = Pattern.compile("whichskill=(\\d+)");

  public static final int parseSkillId(final String text) {
    Matcher matcher = DebugDatabase.SKILL_ID_PATTERN.matcher(text);
    return matcher.find() ? StringUtilities.parseInt(matcher.group(1)) : 0;
  }

  private static final Pattern SKILL_TYPE_PATTERN = Pattern.compile("<b>Type:</b> (.*?)<br>");

  public static final String parseSkillType(final String text) {
    Matcher matcher = DebugDatabase.SKILL_TYPE_PATTERN.matcher(text);
    return matcher.find() ? matcher.group(1) : "";
  }

  private static final Pattern SKILL_MP_COST_PATTERN = Pattern.compile("<b>MP Cost:</b> (\\d+)");

  public static final long parseSkillMPCost(final String text) {
    Matcher matcher = DebugDatabase.SKILL_MP_COST_PATTERN.matcher(text);
    return matcher.find() ? StringUtilities.parseLong(matcher.group(1)) : 0;
  }

  // Gives Effect: <b><a class=nounder
  // href="desc_effect.php?whicheffect=69dcf3d8fe46c29e7fb6075d06448c95">Your Fifteen Minutes</a>
  private static final Pattern SKILL_EFFECT_PATTERN =
      Pattern.compile("Gives Effect: .*?whicheffect=([^\">]*).*?>([^<]*)");

  public static final String parseSkillEffectName(final String text) {
    Matcher matcher = DebugDatabase.SKILL_EFFECT_PATTERN.matcher(text);
    return matcher.find() ? matcher.group(2) : "";
  }

  public static final String parseSkillEffectId(final String text) {
    Matcher matcher = DebugDatabase.SKILL_EFFECT_PATTERN.matcher(text);
    return matcher.find() ? matcher.group(1) : "";
  }

  private static final Pattern SKILL_EFFECT_DURATION_PATTERN =
      Pattern.compile("\\((\\d+) Adventures?\\)");

  public static final int parseSkillEffectDuration(final String text) {
    Matcher matcher = DebugDatabase.SKILL_EFFECT_DURATION_PATTERN.matcher(text);
    return matcher.find() ? StringUtilities.parseInt(matcher.group(1)) : 0;
  }

  public static final String skillDescriptionText(final int skillId) {
    return DebugDatabase.skillDescriptionText(DebugDatabase.rawSkillDescriptionText(skillId));
  }

  public static final String readSkillDescriptionText(final int skillId) {
    GenericRequest request = new GenericRequest("desc_skill.php");
    request.addFormField("whichskill", String.valueOf(skillId));
    request.addFormField("self", "true");
    RequestThread.postRequest(request);
    return request.responseText;
  }

  private static String rawSkillDescriptionText(final int skillId) {
    String previous = DebugDatabase.rawSkills.get(skillId);
    if (previous != null && !previous.equals("")) {
      return previous;
    }

    String text = DebugDatabase.readSkillDescriptionText(skillId);
    DebugDatabase.rawSkills.put(skillId, text);

    return text;
  }

  private static final Pattern SKILL_DATA_PATTERN =
      Pattern.compile("<div id=\"description\"[^>]*>(.*)</div>", Pattern.DOTALL);

  private static String skillDescriptionText(final String rawText) {
    if (rawText == null) {
      return null;
    }

    Matcher matcher = DebugDatabase.SKILL_DATA_PATTERN.matcher(rawText);
    if (!matcher.find()) {
      return null;
    }

    return matcher.group(1);
  }

  private static void checkSkillModifiers(final PrintStream report) {
    RequestLogger.printLine("Checking modifiers...");

    DebugDatabase.checkSkillModifierMap(report);
  }

  private static void checkSkillModifierMap(final PrintStream report) {
    Map<String, String> map = DebugDatabase.passiveSkills.getMap();
    if (map.size() == 0) {
      return;
    }

    String tag = DebugDatabase.passiveSkills.getTag();

    report.println();
    report.println("# " + tag + " section of modifiers.txt");
    report.println();

    for (Entry<String, String> entry : map.entrySet()) {
      String name = entry.getKey();
      String text = entry.getValue();
      DebugDatabase.checkSkillModifierDatum(name, text, report);
    }
  }

  private static final Pattern SKILL_ENCHANTMENT_PATTERN =
      Pattern.compile("<font color=blue size=2><b>(.*)</b></font>", Pattern.DOTALL);

  public static final void parseSkillEnchantments(
      final String text, final ModifierList known, final ArrayList<String> unknown) {
    DebugDatabase.parseStandardEnchantments(
        text, known, unknown, DebugDatabase.SKILL_ENCHANTMENT_PATTERN);
  }

  public static final String parseSkillEnchantments(
      final String text, final ArrayList<String> unknown) {
    ModifierList known = new ModifierList();
    DebugDatabase.parseSkillEnchantments(text, known, unknown);
    return known.toString();
  }

  public static final String parseSkillEnchantments(final String text) {
    ArrayList<String> unknown = new ArrayList<>();
    return DebugDatabase.parseSkillEnchantments(text, unknown);
  }

  private static void checkSkillModifierDatum(
      final String name, final String text, final PrintStream report) {
    ModifierList known = new ModifierList();
    ArrayList<String> unknown = new ArrayList<>();

    // Get the known and unknown modifiers from the effect description
    DebugDatabase.parseSkillEnchantments(text, known, unknown);

    // Compare to what is already registered.
    // Log differences and substitute formulas, as appropriate.
    DebugDatabase.checkModifiers(ModifierType.SKILL, name, known, true, report);

    // Print the modifiers in the format modifiers.txt expects.
    if (known.size() > 0 || unknown.size() > 0) {
      DebugDatabase.logModifierDatum(ModifierType.SKILL, name, known, unknown, report);
    }
  }

  // **********************************************************

  // Utilities for dealing with KoL description data

  private static PrintStream openReport(final String fileName) {
    return LogStream.openStream(new File(KoLConstants.DATA_LOCATION, fileName), true);
  }

  private static void loadScrapeData(final Map<Integer, String> stringMap, final String fileName) {
    try {
      File saveData = new File(KoLConstants.DATA_LOCATION, fileName);
      if (!saveData.exists()) {
        return;
      }

      String currentLine;
      StringBuilder currentHTML = new StringBuilder();
      try (BufferedReader reader = FileUtilities.getReader(saveData)) {
        int lines = 0;

        while ((currentLine = reader.readLine()) != null && !currentLine.equals("")) {
          lines += 1;
          currentHTML.setLength(0);
          int currentId = StringUtilities.parseInt(currentLine);

          do {
            currentLine = reader.readLine();
            currentHTML.append(currentLine);
            currentHTML.append(KoLConstants.LINE_BREAK);
          } while (!currentLine.equals("</html>"));
          if (stringMap.getOrDefault(currentId, "").isEmpty()) {
            stringMap.put(currentId, currentHTML.toString());
          }
          reader.readLine();
        }
      }
    } catch (Exception e) {
      // This shouldn't happen, but if it does, go ahead and
      // fall through.  You're done parsing.
    }
  }

  private static void saveScrapeData(
      final Iterator<Integer> it, final Map<Integer, String> stringMap, final String fileName) {
    File file = new File(KoLConstants.DATA_LOCATION, fileName);
    PrintStream livedata = LogStream.openStream(file, true);

    while (it.hasNext()) {
      int id = it.next();
      if (id < 1) {
        continue;
      }

      String description = stringMap.get(id);
      if (description != null && !description.equals("")) {
        livedata.println(id);
        livedata.println(description);
      }
    }

    livedata.close();
  }

  // **********************************************************

  public static final void checkPlurals(final String parameters) {
    var client = HttpUtilities.getClientBuilder().build();

    RequestLogger.printLine("Checking plurals...");
    PrintStream report =
        LogStream.openStream(new File(KoLConstants.DATA_LOCATION, "plurals.txt"), true);
    if (!parameters.contains("-")) {
      int itemId = StringUtilities.parseInt(parameters);
      if (itemId == 0) {
        for (Integer id : ItemDatabase.descriptionIdKeySet()) {
          if (!KoLmafia.permitsContinue()) {
            break;
          }
          if (id < 0) {
            continue;
          }
          while (++itemId < id) {
            report.println(itemId);
          }
          DebugDatabase.checkPlural(id, client, report);
        }
      } else {
        DebugDatabase.checkPlural(itemId, client, report);
      }
    } else {
      String[] points = parameters.split("-");
      // parseInt will return 0 for null input so bother to check split for validity
      int start = StringUtilities.parseInt(points[0]);
      int end = StringUtilities.parseInt(points[1]);
      start = Math.max(0, start);
      end = Math.min(end, ItemDatabase.maxItemId());
      for (int i = start; i < end; i++) {
        DebugDatabase.checkPlural(i, client, report);
      }
    }
    report.close();
  }

  private static void checkPlural(
      final int itemId, final HttpClient client, final PrintStream report) {
    Integer id = itemId;

    String name = ItemDatabase.getItemDataName(id);
    if (name == null) {
      report.println(itemId);
      return;
    }
    String plural = ItemDatabase.getPluralById(itemId);
    if (plural == null) {
      // If we don't have a plural, the default is to simply
      // add an "s".
      plural = "";
    } else if (plural.equals(name + "s")) {
      // If we do explicitly list a plural which is the
      // default, suppress it.
      plural = "";
    }

    String displayPlural = StringUtilities.getDisplayName(plural.equals("") ? name + "s" : plural);

    // Don't bother checking quest items
    String access = ItemDatabase.getAccessById(id);
    boolean logit = false;
    if (access != null && !access.contains(Attribute.QUEST.description)) {
      String otherPlural;
      boolean checkApi = InventoryManager.getCount(itemId) > 1;
      if (checkApi) {
        otherPlural = DebugDatabase.readApiPlural(itemId);
        if (otherPlural.equals("")) {
          otherPlural = name + "s";
        }
        String test = plural;
        if (test.equals("")) {
          test = name + "s";
        }
        if (!test.equals(otherPlural)) {
          RequestLogger.printLine(
              "*** "
                  + name
                  + ": KoLmafia plural = \""
                  + displayPlural
                  + "\", KoL plural = \""
                  + otherPlural
                  + "\"");
          plural = otherPlural;
        }
      } else {
        String wikiData = DebugDatabase.readWikiItemData(name, client);
        Matcher matcher = DebugDatabase.WIKI_PLURAL_PATTERN.matcher(wikiData);
        otherPlural = matcher.find() ? matcher.group(1) : "";
        otherPlural = CharacterEntities.unescape(otherPlural);
        if (otherPlural.equals("")) {
          // The Wiki does not list a plural. If ours is
          // non-default, log discrepancy and keep ours.
          if (!plural.equals("")) {
            logit = true;
          }
        } else if (otherPlural.equalsIgnoreCase("I am a Fish")) {
          RequestLogger.printLine(
              "*** " + name + " has bogus Wiki plural: \"" + otherPlural + "\". Ignoring.");
        } else if (plural.equals("")) {
          // The Wiki has a plural, but ours is the
          // default. If the Wiki's is NOT the default,
          // log it and tentatively accept it
          if (!displayPlural.equals(otherPlural)) {
            logit = true;
            plural = "*** " + otherPlural;
          }
        } else {
          // Both we and the Wiki have plurals. If they
          // do not agree, log it, but keep ours.
          if (!displayPlural.equals(otherPlural)) {
            logit = true;
          }
        }

        if (logit) {
          RequestLogger.printLine(
              "*** "
                  + name
                  + ": KoLmafia plural = \""
                  + displayPlural
                  + "\", Wiki plural = \""
                  + otherPlural
                  + "\"");
        }
      }
    }

    if (plural.equals("")) {
      report.println(itemId + "\t" + name);
    } else {
      report.println(itemId + "\t" + name + "\t" + plural);
    }
  }

  // **********************************************************

  public static final void checkMuseumPlurals(final String parameters) {

    PrintStream report =
        LogStream.openStream(new File(KoLConstants.DATA_LOCATION, "plurals.txt"), true);

    try (report) {
      var array = getMuseumPluralArray();
      var length = array.length();
      for (int i = 0; i < length; i++) {
        var entry = array.getJSONObject(i);
        var id = entry.getInt("itemid");
        var name = entry.getString("name");
        var plural = entry.optString("plural", "");

        if (plural.equals(name + "s")) {
          // make default
          plural = "";
        }

        String mafiaName = ItemDatabase.getItemDataName(id);
        if (mafiaName == null) {
          report.println("Unrecognised item " + id + ": \"" + name + "\"");
          continue;
        }
        if (!mafiaName.equals(name)) {
          report.println(
              "item " + id + " has name \"" + name + "\" but Mafia says \"" + mafiaName + "\"");
        }
        String mafiaPlural = ItemDatabase.getPluralById(id);
        if (plural.isEmpty() && !mafiaPlural.isEmpty()) {
          report.println(
              "Item "
                  + id
                  + ": \""
                  + name
                  + "\" has default plural, but Mafia says \""
                  + mafiaPlural
                  + "\"");
        } else if (mafiaPlural.isEmpty() && !plural.isEmpty()) {
          report.println(
              "Item " + id + ": \"" + name + "\" has plural unknown to Mafia: \"" + plural + "\"");
        } else if (!plural.equals(mafiaPlural)) {
          report.println(
              "Item "
                  + id
                  + ": \""
                  + name
                  + "\" has plural \""
                  + plural
                  + "\" but Mafia says \""
                  + mafiaPlural
                  + "\"");
        }
      }
    }
  }

  private static JSONArray getMuseumPluralArray() {
    var client = HttpUtilities.getClientBuilder().build();
    String url = "https://museum.loathers.net/api/plurals";

    URI uri;
    try {
      uri = new URI(url);
    } catch (URISyntaxException e) {
      return new JSONArray();
    }

    var request =
        HttpRequest.newBuilder(uri).header("User-Agent", GenericRequest.getUserAgent()).build();

    HttpResponse<String> response;
    try {
      response = client.send(request, BodyHandlers.ofString(StandardCharsets.UTF_8));
    } catch (IOException | InterruptedException e) {
      return new JSONArray();
    }

    if (response.statusCode() == 200) {
      String body = response.body();
      return new JSONArray(body);
    } else {
      return new JSONArray();
    }
  }

  // **********************************************************

  private static boolean powerFilter(AdventureResult item) {
    int itemId = item.getItemId();
    ConsumptionType type = ItemDatabase.getConsumptionType(itemId);
    return (type == ConsumptionType.OFFHAND
        || type == ConsumptionType.ACCESSORY
        || type == ConsumptionType.CONTAINER);
  }

  private static boolean unknownPower(AdventureResult item) {
    return (EquipmentDatabase.getPower(item.getItemId()) == 0);
  }

  private static void conditionallyAddItems(
      Collection<AdventureResult> items, Collection<AdventureResult> location, boolean force) {
    // If checking display case, retrieve if necessary
    if (location == KoLConstants.collection) {
      if (!KoLCharacter.hasDisplayCase()) {
        return;
      }
      if (!DisplayCaseManager.collectionRetrieved) {
        RequestThread.postRequest(new DisplayCaseRequest());
      }
    }

    for (AdventureResult item : location) {
      if (!powerFilter(item) || (!force && !unknownPower(item)) || items.contains(item)) {
        continue;
      }
      if (location == KoLConstants.storage) {
        // A potential item that we have only in storage
        // Move a single one to inventory and then to the closet
        AdventureResult toTransfer = item.getInstance(1);
        RequestThread.postRequest(
            new StorageRequest(StorageRequestType.STORAGE_TO_INVENTORY, toTransfer));
        RequestThread.postRequest(
            new ClosetRequest(ClosetRequestType.INVENTORY_TO_CLOSET, toTransfer));
      }
      items.add(item);
    }
  }

  public static final void checkPowers(final String option) {
    // We can check the power of any items in inventory, equipment, closet, or display case.
    // We'll assume that any item with a non-zero power is correct.
    // Off-hand items and accessories don't have visible power and
    // might be 0 in the database. Look them up and fix them.

    if (StringUtilities.isNumeric(option)) {
      DebugDatabase.checkPower(StringUtilities.parseInt(option), true);
      return;
    }

    TreeSet<AdventureResult> items = new TreeSet<>();
    boolean force = option.equals("all");

    DebugDatabase.conditionallyAddItems(items, KoLConstants.inventory, force);
    DebugDatabase.conditionallyAddItems(items, KoLConstants.closet, force);
    DebugDatabase.conditionallyAddItems(items, EquipmentManager.allEquipmentAsCollection(), force);
    DebugDatabase.conditionallyAddItems(items, KoLConstants.collection, force);
    // Storage must be at the end since we will pull things iff they
    // are not present in a more accessible place
    // *** disabled, since I've done it once. :)
    // DebugDatabase.conditionallyAddItems( items, KoLConstants.storage, force );

    DebugDatabase.checkPowers(items, force);
  }

  private static void checkPowers(final Collection<AdventureResult> items, final boolean force) {
    for (AdventureResult item : items) {
      DebugDatabase.checkPower(item.getItemId(), force);
    }
  }

  private static void checkPower(final int itemId, final boolean force) {
    int current = EquipmentDatabase.getPower(itemId);
    if (!force && current != 0) {
      return;
    }

    // Look it up and register it anew
    ApiRequest request = new ApiRequest("item", itemId);
    RequestThread.postRequest(request);

    JSONObject JSON = request.JSON;
    if (JSON == null) {
      AdventureResult item = ItemPool.get(itemId);
      String location =
          KoLConstants.inventory.contains(item)
              ? "inventory"
              : KoLConstants.closet.contains(item)
                  ? "closet"
                  : KoLConstants.collection.contains(item)
                      ? "display case"
                      : KoLConstants.storage.contains(item) ? "storage" : "nowhere";
      KoLmafia.updateDisplay("Could not look up item " + item + " from " + location);
      return;
    }

    try {
      int power = JSON.getInt("power");

      // Yes, some items really are power 0
      if (power == 0 || power == current) {
        return;
      }

      String name = JSON.getString("name");
      String descid = JSON.getString("descid");
      RequestLogger.printLine(
          "Item \"" + name + "\" power incorrect: " + current + " should be " + power);
      ItemDatabase.registerItem(itemId, name, descid, null, power, false);
    } catch (JSONException e) {
      KoLmafia.updateDisplay("Error parsing JSON string!");
      StaticEntity.printStackTrace(e);
    }
  }

  // Helper method to force normalize Concoction comparisons to [-1, 0, 1] before testing
  private static int sgn(int value) {
    return Integer.compare(value, 0);
  }

  // Helper method to append item id
  private static String getIString(Concoction con) {
    return "[" + con.getItemId() + "] " + con;
  }

  public static void checkConcoctions() {
    // Code intended to verify that Concoction.compareTo() meets its contract.  Since the
    // concoctions data is in a file and this is an expensive check, in terms of time,
    // moved out of unit testing to here.
    Concoction[] ids;
    int maxIndex;
    String msg;
    int[][] result;
    List<Concoction> usables =
        ConcoctionDatabase.getUsables().values().stream()
            .flatMap(l -> l.stream())
            .collect(Collectors.toList());
    // size is all elements.  getSize is visible elements.
    maxIndex = usables.size();
    ids = new Concoction[maxIndex];
    int i = 0;
    for (Concoction con : usables) {
      ids[i++] = con;
    }
    result = new int[maxIndex][maxIndex];
    for (i = 0; i < maxIndex; ++i) {
      for (int j = 0; j < maxIndex; ++j) {
        result[i][j] = sgn(ids[i].compareTo(ids[j]));
      }
    }
    // sgn(x.compareTo(y)) == -sgn(y.compareTo(x)
    for (i = 0; i < maxIndex; ++i) {
      for (int j = 0; j < maxIndex; ++j) {
        msg =
            "Failed comparing (quasi symmetry) "
                + getIString(ids[i])
                + " and "
                + getIString(ids[j]);
        if (result[i][j] != -result[j][i]) {
          KoLmafia.updateDisplay(msg);
        }
      }
    }
    // tests the portion of the contract that says (x.compareTo(y)==0) == (x.equals(y))
    for (i = 0; i < maxIndex; ++i) {
      for (int j = 0; j < maxIndex; ++j) {
        if (result[i][j] == 0) {
          msg = "Failed comparing (equality) " + getIString(ids[i]) + " and " + getIString(ids[j]);
          if (!(ids[i].equals(ids[j]))) {
            KoLmafia.updateDisplay(msg);
          }
          msg =
              "Failed comparing (other equality) "
                  + getIString(ids[i])
                  + " and "
                  + getIString(ids[j]);
          if (ids[i] != ids[j]) {
            KoLmafia.updateDisplay(msg);
          }
        }
      }
    }
    // x.compareTo(y)==0 implies that sgn(x.compareTo(z)) == sgn(y.compareTo(z)), for all z.
    for (i = 0; i < maxIndex; ++i) {
      // Don't have to check whole matrix
      for (int j = i; j < maxIndex; ++j) {
        if (result[i][j] == 0) {
          for (int k = 1; k < maxIndex; ++k) {
            msg =
                "Failed comparing (transitive)"
                    + getIString(ids[i])
                    + " and "
                    + getIString(ids[j])
                    + " and "
                    + getIString(ids[k]);
            if (result[i][k] != result[j][k]) {
              KoLmafia.updateDisplay(msg);
            }
          }
        }
      }
    }
  }

  // **********************************************************

  public static final void checkShields() {
    DebugDatabase.checkShields(KoLConstants.inventory);
    DebugDatabase.checkShields(KoLConstants.closet);
    DebugDatabase.checkShields(KoLConstants.storage);
  }

  public static final void checkShields(final Collection<AdventureResult> items) {
    for (AdventureResult item : items) {
      int itemId = item.getItemId();
      if (!EquipmentDatabase.getItemType(itemId).equals("shield")) {
        continue;
      }

      ApiRequest request = new ApiRequest("item", itemId);
      RequestThread.postRequest(request);

      JSONObject JSON = request.JSON;
      if (JSON == null) {
        continue;
      }

      try {
        int oldPower = EquipmentDatabase.getPower(itemId);
        int correctPower = JSON.getInt("power");
        if (oldPower == correctPower) {
          continue;
        }

        String name = JSON.getString("name");
        String descid = JSON.getString("descid");

        RequestLogger.printLine(
            "Shield \"" + name + "\" power incorrect: " + oldPower + " should be " + correctPower);
        ItemDatabase.registerItem(itemId, name, descid, null, correctPower, false);
      } catch (JSONException e) {
        KoLmafia.updateDisplay("Error parsing JSON string!");
        StaticEntity.printStackTrace(e);
      }
    }
  }

  // **********************************************************

  public static final void checkPotions() {
    RequestLogger.printLine("Loading previous data...");
    DebugDatabase.loadScrapeData(rawItems, ITEM_HTML);

    for (Integer id : ItemDatabase.descriptionIdKeySet()) {
      int itemId = id;
      if (itemId < 1 || !ItemDatabase.isUsable(itemId) || ItemDatabase.isEquipment(itemId)) {
        continue;
      }

      // Potions grant an effect. Check for a new effect.
      String itemName = ItemDatabase.getItemDataName(id);
      String effectName =
          ModifierDatabase.getStringModifier(ModifierType.ITEM, itemId, StringModifier.EFFECT);
      if (!effectName.equals("") && EffectDatabase.getEffectId(effectName, true) == -1) {
        String rawText = DebugDatabase.rawItemDescriptionText(itemId);
        String effectDescid = DebugDatabase.parseEffectDescid(rawText);
        EffectDatabase.registerEffect(effectName, effectDescid, "use 1 " + itemName);
      }
    }
  }

  // **********************************************************

  private static final String CONSUMABLE_DATA = "consumables.txt";

  public static final void checkConsumables() {
    RequestLogger.printLine("Loading previous data...");
    DebugDatabase.loadScrapeData(rawItems, ITEM_HTML);
    RequestLogger.printLine("Checking internal data...");
    PrintStream report = DebugDatabase.openReport(CONSUMABLE_DATA);
    DebugDatabase.checkConsumables(report);
    report.close();
  }

  private static void checkConsumables(final PrintStream report) {
    DebugDatabase.checkConsumables(
        report,
        ConsumablesDatabase.allConsumables.stream()
            .filter(consumable -> consumable.getRawFullness() != null)
            .toList(),
        "fullness");
    DebugDatabase.checkConsumables(
        report,
        ConsumablesDatabase.allConsumables.stream()
            .filter(consumable -> consumable.getRawInebriety() != null)
            .toList(),
        "inebriety");
    DebugDatabase.checkConsumables(
        report,
        ConsumablesDatabase.allConsumables.stream()
            .filter(consumable -> consumable.getRawSpleenHit() != null)
            .toList(),
        "spleenhit");
  }

  private static void checkConsumables(
      final PrintStream report, final Collection<Consumable> consumables, final String tag) {
    if (consumables.size() == 0) {
      return;
    }

    report.println();
    report.println("# Consumption data in " + tag + ".txt");
    report.println("#");

    for (var consumable : consumables) {
      DebugDatabase.checkConsumable(report, consumable);
    }
  }

  private static void checkConsumable(final PrintStream report, Consumable consumable) {
    int itemId = consumable.itemId;
    // It is valid for items to have no itemId: sushi, Cafe offerings, and so on
    String text = itemId == -1 ? "" : DebugDatabase.itemDescriptionText(itemId, false);
    if (text == null) {
      return;
    }

    ConsumablesDatabase.writeConsumable(report, consumable);
  }

  // Type: <b>food <font color=#999999>(crappy)</font></b>
  // Type: <b>food (decent)</b>
  // Type: <b>booze <font color=green>(good)</font></b>
  // Type: <b>food <font color=blue>(awesome)</font></b>
  // Type: <b>food <font color=blueviolet>(EPIC)</font></b>

  private static final Pattern QUALITY_PATTERN = Pattern.compile("Type: <b>.*?\\((.*?)\\).*?</b>");

  public static ConsumableQuality parseQuality(final String text) {
    Matcher matcher = DebugDatabase.QUALITY_PATTERN.matcher(text);
    return ConsumableQuality.find(matcher.find() ? matcher.group(1) : "");
  }

  // **********************************************************

  // <tr class="frow " data-stats="1" data-meat="1" data-items="1"><td valign=center><input
  // type=radio name=newfam value=192></td><td valign=center><img
  // src="/images/itemimages/goldmonkey.gif" class="hand fam" onClick='fam(192)'></td><td valign=top
  // style='padding-top: .45em;'><b>Ignominious Uncguary</b>, the 20-pound Golden Monkey (400 exp,
  // 6,107 kills) <font size="1"><br />&nbsp;&nbsp;&nbsp;&nbsp;<a class="fave"
  // href="familiar.php?group=0&action=fave&famid=192&pwd">[unfavorite]</a>&nbsp;&nbsp;<a
  // class="fave" href="familiar.php?&action=newfam&newfam=192&pwd">[take with
  // you]</a></font></td><td valign=center nowrap><center><b>(</b><img
  // src="/images/itemimages/goldbanana.gif" class=hand onClick='descitem(986943479)'
  // align=middle><b>)</b><br><font size=1><a
  // href='familiar.php?pwd&action=unequip&famid=192'>[unequip]</a></font></center></td></tr>

  private static final Pattern FAMILIAR_ROW_PATTERN =
      Pattern.compile("<tr class=\"frow ?\"([^>]*)>.*?onClick='fam\\(([\\d]+)\\)'.*?</tr>");

  public static final void checkFamiliarsInTerrarium(boolean showVariable) {
    FamiliarRequest request = new FamiliarRequest();
    RequestThread.postRequest(request);

    TreeMap<Integer, String> map = new TreeMap<>();

    Matcher matcher = DebugDatabase.FAMILIAR_ROW_PATTERN.matcher(request.responseText);
    while (matcher.find()) {
      int id = StringUtilities.parseInt(matcher.group(2));
      String powers = matcher.group(1).trim();
      map.put(id, powers);
    }

    for (Entry<Integer, String> entry : map.entrySet()) {
      int id = entry.getKey();
      String powers = entry.getValue();
      DebugDatabase.checkTerrariumFamiliar(id, powers, showVariable);
    }
  }

  private static void checkTerrariumFamiliar(int id, String powers, boolean showVariable) {
    // KoL familiar categories
    boolean dataAttack = powers.contains("data-attack");
    boolean dataDefense = powers.contains("data-defense");
    boolean dataHPRestore = powers.contains("data-hp_restore");
    boolean dataItemDrops = powers.contains("data-itemdrops");
    boolean dataItems = powers.contains("data-items");
    boolean dataMeat = powers.contains("data-meat");
    boolean dataMPRestore = powers.contains("data-mp_restore");
    boolean dataOther = powers.contains("data-other");
    boolean dataStats = powers.contains("data-stats");
    boolean dataUnderwater = powers.contains("data-underwater");

    // KoLmafia familiar categories
    boolean block = FamiliarDatabase.isBlockType(id);
    boolean combat0 = FamiliarDatabase.isCombat0Type(id);
    boolean combat1 = FamiliarDatabase.isCombat1Type(id);
    boolean delevel = FamiliarDatabase.isDelevelType(id);
    boolean drop = FamiliarDatabase.isDropType(id);
    boolean hp0 = FamiliarDatabase.isHp0Type(id);
    boolean hp1 = FamiliarDatabase.isHp1Type(id);
    boolean item0 = FamiliarDatabase.isFairyType(id);
    boolean meat0 = FamiliarDatabase.isMeatDropType(id);
    boolean meat1 = FamiliarDatabase.isMeat1Type(id);
    boolean mp0 = FamiliarDatabase.isMp0Type(id);
    boolean mp1 = FamiliarDatabase.isMp1Type(id);
    boolean none = FamiliarDatabase.isNoneType(id);
    boolean other0 = FamiliarDatabase.isOther0Type(id);
    boolean other1 = FamiliarDatabase.isOther1Type(id);
    boolean passive = FamiliarDatabase.isPassiveType(id);
    boolean stat0 = FamiliarDatabase.isVolleyType(id);
    boolean stat1 = FamiliarDatabase.isSombreroType(id);
    boolean stat2 = FamiliarDatabase.isStat2Type(id);
    boolean stat3 = FamiliarDatabase.isStat3Type(id);
    boolean underwater = FamiliarDatabase.isUnderwaterType(id);
    boolean variable = FamiliarDatabase.isVariableType(id);

    String name = FamiliarDatabase.getFamiliarName(id);
    String prefix = "*** familiar #" + id + " (" + name + "): KoL says ";

    // Check KoL categories
    if (dataAttack && !(combat0 || combat1)) {
      String message =
          !variable
              ? "'attack' but we have neither 'combat0' nor 'combat1'"
              : showVariable ? "'attack' but we say 'variable'" : null;
      if (message != null) {
        RequestLogger.printLine(prefix + message);
      }
    }
    if (dataDefense && !(block || delevel || other0)) {
      String message =
          !variable
              ? "'defense' but we have none of 'block', 'delevel', or 'other0'"
              : showVariable ? "'defense' but we say 'variable'" : null;
      if (message != null) {
        RequestLogger.printLine(prefix + message);
      }
    }
    if (dataHPRestore && !(hp0 || hp1)) {
      String message =
          !variable
              ? "'hp_restore' but we have neither 'hp0' nor 'hp1'"
              : showVariable ? "'hp_restore' but we say 'variable'" : null;
      if (message != null) {
        RequestLogger.printLine(prefix + message);
      }
    }
    if (dataItemDrops && !item0) {
      String message =
          !variable
              ? "'itemdrops' but we do not have 'item0'"
              : showVariable ? "'itemdrops' but we say 'variable'" : null;
      if (message != null) {
        RequestLogger.printLine(prefix + message);
      }
    }
    if (dataItems && !drop) {
      String message =
          !variable
              ? "'item' but we do not have 'drop'"
              : showVariable ? "'item' but we say 'variable'" : null;
      if (message != null) {
        RequestLogger.printLine(prefix + message);
      }
    }
    if (dataMeat && !(meat0 || meat1)) {
      String message =
          !variable
              ? "'meat' but we have neither 'meat0' nor 'meat1'"
              : showVariable ? "'meat' but we say 'variable'" : null;
      if (message != null) {
        RequestLogger.printLine(prefix + message);
      }
    }
    if (dataMPRestore && !(mp0 || mp1)) {
      String message =
          !variable
              ? "'mp_restore' but we have neither 'mp0' nor 'mp1'"
              : showVariable ? "'mp_restore' but we say 'variable'" : null;
      if (message != null) {
        RequestLogger.printLine(prefix + message);
      }
    }
    if (dataOther && !(none || other0 || other1 || passive)) {
      String message =
          !variable
              ? "'other' but we have none of 'none', 'other0', 'other1',or 'passive'"
              : showVariable ? "'other' but we say 'variable'" : null;
      if (message != null) {
        RequestLogger.printLine(prefix + message);
      }
    }
    if (dataStats && !(stat0 || stat1 || stat2 || stat3 || passive)) {
      String message =
          !variable
              ? "'stats' but we have none of 'stat0', 'stat1', 'stat2', or 'stat3'"
              : showVariable ? "'stats' but we say 'variable'" : null;
      if (message != null) {
        RequestLogger.printLine(prefix + message);
      }
    }
    if (dataUnderwater && !underwater) {
      String message =
          !variable
              ? "'underwater' but we do not have 'underwater'"
              : showVariable ? "'underwater' but we say 'variable'" : null;
      if (message != null) {
        RequestLogger.printLine(prefix + message);
      }
    }

    // Check KoLmafia categories
    if (block && !dataDefense) {
      RequestLogger.printLine(
          "*** familiar #"
              + id
              + " ("
              + name
              + "): KoLmafia has 'block' but KoL does not say 'defense'");
    }
    if (combat0 && !dataAttack) {
      RequestLogger.printLine(
          "*** familiar #"
              + id
              + " ("
              + name
              + "): KoLmafia has 'combat0' but KoL does not say 'attack'");
    }
    if (combat1 && !dataAttack) {
      RequestLogger.printLine(
          "*** familiar #"
              + id
              + " ("
              + name
              + "): KoLmafia has 'combat1' but KoL does not say 'attack'");
    }
    if (delevel && !dataDefense) {
      RequestLogger.printLine(
          "*** familiar #"
              + id
              + " ("
              + name
              + "): KoLmafia has 'delevel' but KoL does not say 'defense'");
    }
    if (drop && !dataItems) {
      RequestLogger.printLine(
          "*** familiar #"
              + id
              + " ("
              + name
              + "): KoLmafia has 'drop' but KoL does not say 'items'");
    }
    if (hp0 && !dataHPRestore) {
      RequestLogger.printLine(
          "*** familiar #"
              + id
              + " ("
              + name
              + "): KoLmafia has 'hp0' but KoL does not say 'hp_restore'");
    }
    if (hp1 && !dataHPRestore) {
      RequestLogger.printLine(
          "*** familiar #"
              + id
              + " ("
              + name
              + "): KoLmafia has 'hp1' but KoL does not say 'hp_restore'");
    }
    if (item0 && !dataItemDrops) {
      RequestLogger.printLine(
          "*** familiar #"
              + id
              + " ("
              + name
              + "): KoLmafia has 'item0' but KoL does not say 'itemdrops'");
    }
    if (meat0 && !dataMeat) {
      RequestLogger.printLine(
          "*** familiar #"
              + id
              + " ("
              + name
              + "): KoLmafia has 'meat0' but KoL does not say 'meat'");
    }
    if (meat1 && !dataMeat) {
      RequestLogger.printLine(
          "*** familiar #"
              + id
              + " ("
              + name
              + "): KoLmafia has 'meat1' but KoL does not say 'meat'");
    }
    if (mp0 && !dataMPRestore) {
      RequestLogger.printLine(
          "*** familiar #"
              + id
              + " ("
              + name
              + "): KoLmafia has 'mp0' but KoL does not say 'mp_restore'");
    }
    if (mp1 && !dataMPRestore) {
      RequestLogger.printLine(
          "*** familiar #"
              + id
              + " ("
              + name
              + "): KoLmafia has 'mp1' but KoL does not say 'mp_restore'");
    }
    if (none && !dataOther) {
      RequestLogger.printLine(
          "*** familiar #"
              + id
              + " ("
              + name
              + "): KoLmafia has 'none' but KoL does not say 'other'");
    }
    if (other0 && !(dataOther || dataDefense)) {
      RequestLogger.printLine(
          "*** familiar #"
              + id
              + " ("
              + name
              + "): KoLmafia has 'other0' but KoL does not say 'other' or 'defense'");
    }
    if (other1 && !dataOther) {
      RequestLogger.printLine(
          "*** familiar #"
              + id
              + " ("
              + name
              + "): KoLmafia has 'other1' but KoL does not say 'other'");
    }
    if (passive && !dataOther) {
      RequestLogger.printLine(
          "*** familiar #"
              + id
              + " ("
              + name
              + "): KoLmafia has 'passive' but KoL does not say 'other'");
    }
    if (stat0 && !dataStats) {
      RequestLogger.printLine(
          "*** familiar #"
              + id
              + " ("
              + name
              + "): KoLmafia has 'stat0' but KoL does not say 'stats'");
    }
    if (stat1 && !dataStats) {
      RequestLogger.printLine(
          "*** familiar #"
              + id
              + " ("
              + name
              + "): KoLmafia has 'stat1' but KoL does not say 'stats'");
    }
    if (stat2 && !dataStats) {
      RequestLogger.printLine(
          "*** familiar #"
              + id
              + " ("
              + name
              + "): KoLmafia has 'stat2' but KoL does not say 'stats'");
    }
    if (stat3 && !dataStats) {
      RequestLogger.printLine(
          "*** familiar #"
              + id
              + " ("
              + name
              + "): KoLmafia has 'stat3' but KoL does not say 'stats'");
    }
    if (underwater && !dataUnderwater) {
      RequestLogger.printLine(
          "*** familiar #"
              + id
              + " ("
              + name
              + "): KoLmafia has 'underwater' but KoL does not say 'underwater'");
    }
  }

  public static final void checkFamiliarImages() {
    // Get familiar images from the familiar description
    boolean changed = false;
    for (int i = 1; i <= FamiliarDatabase.maxFamiliarId; ++i) {
      changed |= DebugDatabase.checkFamiliarImage(i);
    }

    // FamiliarDatabase.saveDataOverride();
  }

  private static final Pattern FAMILIAR_IMAGE_PATTERN =
      Pattern.compile("images\\.kingdomofloathing\\.com/itemimages/(.*?\\.gif)");

  private static boolean checkFamiliarImage(final int id) {
    String file = "desc_familiar.php?which=" + id;
    GenericRequest request = new GenericRequest(file);
    RequestThread.postRequest(request);
    String text = request.responseText;
    if (text == null) {
      RequestLogger.printLine("*** no description for familiar #" + id);
      return false;
    }

    boolean changed = false;
    Matcher matcher = FAMILIAR_IMAGE_PATTERN.matcher(text);
    if (matcher.find()) {
      String oldImage = FamiliarDatabase.getFamiliarImageLocation(id);
      String newImage = matcher.group(1);
      if (!oldImage.equals(newImage)) {
        RequestLogger.printLine(
            "*** familiar #" + id + " has image " + oldImage + " but KoL says it is " + newImage);
        FamiliarDatabase.setFamiliarImageLocation(id, newImage);
        changed = true;
      }
    }

    return changed;
  }

  // **********************************************************

  public static final void checkConsumptionData() {
    RequestLogger.printLine("Checking consumption data...");

    PrintStream writer =
        LogStream.openStream(new File(KoLConstants.DATA_LOCATION, "consumption.txt"), true);

    DebugDatabase.checkEpicure(writer);
    DebugDatabase.checkMixologist(writer);

    writer.close();
  }

  private static final String EPICURE = "http://kol.coldfront.net/tools/epicure/export_data.php";

  private static void checkEpicure(final PrintStream writer) {
    RequestLogger.printLine("Connecting to Well-Tempered Epicure...");
    Document doc = getXMLDocument(EPICURE);

    if (doc == null) {
      return;
    }

    writer.println(KoLConstants.FULLNESS_VERSION);
    writer.println("# Data provided courtesy of the Garden of Earthly Delights");
    writer.println("# The Well-Tempered Epicure: " + EPICURE);
    writer.println();
    writer.println(
        "# Food"
            + "\t"
            + "Fullness"
            + "\t"
            + "Level Req"
            + "\t"
            + "Adv"
            + "\t"
            + "Musc"
            + "\t"
            + "Myst"
            + "\t"
            + "Moxie");
    writer.println();

    NodeList elements = doc.getElementsByTagName("iteminfo");

    for (int i = 0; i < elements.getLength(); i++) {
      Node element = elements.item(i);
      checkFood(element, writer);
    }
  }

  private static void checkFood(final Node element, final PrintStream writer) {
    String name = "";
    String advs = "";
    String musc = "";
    String myst = "";
    String mox = "";
    String fullness = "";
    String level = "";

    for (Node node = element.getFirstChild(); node != null; node = node.getNextSibling()) {
      String tag = node.getNodeName();
      Node child = node.getFirstChild();

      switch (tag) {
        case "title" -> name = DebugDatabase.getStringValue(child);
        case "advs" -> advs = DebugDatabase.getNumericValue(child);
        case "musc" -> musc = DebugDatabase.getNumericValue(child);
        case "myst" -> myst = DebugDatabase.getNumericValue(child);
        case "mox" -> mox = DebugDatabase.getNumericValue(child);
        case "fullness" -> fullness = DebugDatabase.getNumericValue(child);
        case "level" -> level = DebugDatabase.getNumericValue(child);
      }
    }

    String line =
        name + "\t" + fullness + "\t" + level + "\t" + advs + "\t" + musc + "\t" + myst + "\t"
            + mox;

    int present = ConsumablesDatabase.getFullness(name);

    if (present == 0) {
      writer.println("# Unknown food:");
      writer.print("# ");
    } else {
      String note = ConsumablesDatabase.getNotes(name);
      if (note != null) {
        line = line + "\t" + note;
      }
    }

    writer.println(line);
  }

  private static final String MIXOLOGIST =
      "http://kol.coldfront.net/tools/mixology/export_data.php";

  private static void checkMixologist(final PrintStream writer) {
    RequestLogger.printLine("Connecting to Well-Tempered Mixologist...");
    Document doc = getXMLDocument(MIXOLOGIST);

    if (doc == null) {
      return;
    }

    writer.println(KoLConstants.INEBRIETY_VERSION);
    writer.println("# Data provided courtesy of the Garden of Earthly Delights");
    writer.println("# The Well-Tempered Mixologist: " + MIXOLOGIST);
    writer.println();
    writer.println(
        "# Drink"
            + "\t"
            + "Inebriety"
            + "\t"
            + "Level Req"
            + "\t"
            + "Adv"
            + "\t"
            + "Musc"
            + "\t"
            + "Myst"
            + "\t"
            + "Moxie");
    writer.println();

    NodeList elements = doc.getElementsByTagName("iteminfo");

    for (int i = 0; i < elements.getLength(); i++) {
      Node element = elements.item(i);
      checkBooze(element, writer);
    }
  }

  private static void checkBooze(final Node element, final PrintStream writer) {
    String name = "";
    String advs = "";
    String musc = "";
    String myst = "";
    String mox = "";
    String drunk = "";
    String level = "";

    for (Node node = element.getFirstChild(); node != null; node = node.getNextSibling()) {
      String tag = node.getNodeName();
      Node child = node.getFirstChild();

      switch (tag) {
        case "title" -> name = DebugDatabase.getStringValue(child);
        case "advs" -> advs = DebugDatabase.getNumericValue(child);
        case "musc" -> musc = DebugDatabase.getNumericValue(child);
        case "myst" -> myst = DebugDatabase.getNumericValue(child);
        case "mox" -> mox = DebugDatabase.getNumericValue(child);
        case "drunk" -> drunk = DebugDatabase.getNumericValue(child);
        case "level" -> level = DebugDatabase.getNumericValue(child);
      }
    }

    String line =
        name + "\t" + drunk + "\t" + level + "\t" + advs + "\t" + musc + "\t" + myst + "\t" + mox;

    int present = ConsumablesDatabase.getInebriety(name);

    if (present == 0) {
      writer.println("# Unknown booze:");
      writer.print("# ");
    } else {
      String note = ConsumablesDatabase.getNotes(name);
      if (note != null) {
        line = line + "\t" + note;
      }
    }

    writer.println(line);
  }

  private static String getStringValue(final Node node) {
    return StringUtilities.getEntityEncode(node.getNodeValue().trim());
  }

  private static String getNumericValue(final Node node) {
    String value = node.getNodeValue().trim();

    int sign = value.startsWith("-") ? -1 : 1;
    if (sign == -1) {
      value = value.substring(1);
    }

    int dash = value.indexOf("-");
    if (dash == -1) {
      return String.valueOf(sign * StringUtilities.parseInt(value));
    }

    int first = sign * StringUtilities.parseInt(value.substring(0, dash));
    int second = StringUtilities.parseInt(value.substring(dash + 1));
    return first + "-" + second;
  }

  private static Document getXMLDocument(final String uri) {
    DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
    try {
      DocumentBuilder db = dbf.newDocumentBuilder();
      return db.parse(uri);
    } catch (Exception e) {
      RequestLogger.printLine(
          "Failed to parse XML document from \"" + uri + "\": " + e.getMessage());
    }

    return null;
  }

  public static final void checkPulverizationData() {
    RequestLogger.printLine("Checking pulverization data...");

    PrintStream writer =
        LogStream.openStream(new File(KoLConstants.DATA_LOCATION, "pulvereport.txt"), true);

    DebugDatabase.checkAnvil(writer);

    writer.close();
  }

  private static final String ANVIL = "http://kol.coldfront.net/tools/anvil/export_data.php";

  private static void checkAnvil(final PrintStream writer) {
    RequestLogger.printLine("Connecting to Well-Tempered Anvil...");
    Document doc = getXMLDocument(ANVIL);

    if (doc == null) {
      return;
    }

    writer.println(KoLConstants.PULVERIZE_VERSION);
    writer.println("# Data provided courtesy of the Garden of Earthly Delights");
    writer.println("# The Well-Tempered Anvil: " + ANVIL);
    writer.println();

    NodeList elements = doc.getElementsByTagName("iteminfo");

    HashSet<Integer> seen = new HashSet<>();
    for (int i = 0; i < elements.getLength(); i++) {
      Node element = elements.item(i);
      checkPulverize(element, writer, seen);
    }

    for (int id = 1; id <= ItemDatabase.maxItemId(); ++id) {
      int pulver = EquipmentDatabase.getPulverization(id);
      if (pulver != -1 && !seen.contains(id)) {
        String name = ItemDatabase.getItemName(id);
        writer.println(name + ": not listed in anvil");
      }
    }
  }

  private static void checkPulverize(
      final Node element, final PrintStream writer, HashSet<Integer> seen) {
    String name = "";
    int id = -1;
    int yield = -1;
    boolean cansmash = false;
    boolean confirmed = false;
    boolean twinkly = false;
    boolean hot = false;
    boolean cold = false;
    boolean stench = false;
    boolean spooky = false;
    boolean sleaze = false;

    for (Node node = element.getFirstChild(); node != null; node = node.getNextSibling()) {
      String tag = node.getNodeName();
      Node child = node.getFirstChild();

      switch (tag) {
        case "cansmash" -> cansmash = DebugDatabase.getStringValue(child).equals("y");
        case "confirmed" -> confirmed = DebugDatabase.getStringValue(child).equals("y");
        case "title" -> name = DebugDatabase.getStringValue(child);
        case "kolid" -> {
          id = StringUtilities.parseInt(DebugDatabase.getNumericValue(child));
          seen.add(id);
        }
        case "yield" -> yield = StringUtilities.parseInt(DebugDatabase.getNumericValue(child));
        case "cold" -> cold = !DebugDatabase.getStringValue(child).equals("0");
        case "hot" -> hot = !DebugDatabase.getStringValue(child).equals("0");
        case "sleazy" -> sleaze = !DebugDatabase.getStringValue(child).equals("0");
        case "spooky" -> spooky = !DebugDatabase.getStringValue(child).equals("0");
        case "stinky" -> stench = !DebugDatabase.getStringValue(child).equals("0");
        case "twinkly" -> twinkly = !DebugDatabase.getStringValue(child).equals("0");
      }
    }

    if (id < 1) {
      writer.println(name + ": anvil doesn't know ID, so can't check");
      return;
    }
    int pulver = EquipmentDatabase.getPulverization(id);
    if (!name.equalsIgnoreCase(ItemDatabase.getItemName(id))) {
      writer.println(name + ": doesn't match mafia name: " + ItemDatabase.getItemName(id));
    }
    name = ItemDatabase.getItemName(id);
    if (!confirmed) {
      name = "(unconfirmed) " + name;
    }
    if (pulver == -1) {
      if (cansmash) {
        writer.println(name + ": anvil says this is smashable");
      }
      return;
    }
    if (!cansmash) {
      writer.println(name + ": anvil says this is not smashable");
      return;
    }
    if (pulver == ItemPool.USELESS_POWDER) {
      if (yield != 1 || twinkly || hot || cold || stench || spooky || sleaze) {
        writer.println(name + ": anvil says something other than useless powder");
      }
      return;
    }
    if (yield == 1 && !(twinkly || hot || cold || stench || spooky || sleaze)) {
      writer.println(name + ": anvil says useless powder");
      return;
    }
    if (pulver == ItemPool.EPIC_WAD) {
      if (yield != 10) {
        writer.println(name + ": anvil says something other than epic wad");
      }
      return;
    }
    if (yield == 10) {
      writer.println(name + ": anvil says epic wad");
      return;
    }
    if (pulver == ItemPool.ULTIMATE_WAD) {
      if (yield != 11) {
        writer.println(name + ": anvil says something other than ultimate wad");
      }
      return;
    }
    if (yield == 11) {
      writer.println(name + ": anvil says ultimate wad");
      return;
    }
    if (pulver == ItemPool.SEA_SALT_CRYSTAL) {
      if (yield != 12) {
        writer.println(name + ": anvil says something other than sea salt crystal");
      }
      return;
    }
    if (yield == 12) {
      writer.println(name + ": anvil says sea salt crystal");
      return;
    }
    if (pulver >= 0) {
      writer.println(
          name + ": I don't know how anvil would say " + ItemDatabase.getItemName(pulver));
      return;
    }
    if (yield < 1 || yield > 12) {
      writer.println(name + ": anvil said yield=" + yield + ", wut?");
      return;
    }
    if ((pulver & EquipmentDatabase.ELEM_TWINKLY) != 0) {
      if (!twinkly) {
        writer.println(name + ": anvil didn't say twinkly");
      }
      return;
    } else if (twinkly) {
      writer.println(name + ": anvil said twinkly");
      return;
    }

    if ((pulver & EquipmentDatabase.ELEM_HOT) != 0) {
      if (!hot) {
        writer.println(name + ": anvil didn't say hot");
      }
      return;
    } else if (hot) {
      writer.println(name + ": anvil said hot");
      return;
    }
    if ((pulver & EquipmentDatabase.ELEM_COLD) != 0) {
      if (!cold) {
        writer.println(name + ": anvil didn't say cold");
      }
      return;
    } else if (cold) {
      writer.println(name + ": anvil said cold");
      return;
    }
    if ((pulver & EquipmentDatabase.ELEM_STENCH) != 0) {
      if (!stench) {
        writer.println(name + ": anvil didn't say stench");
      }
      return;
    } else if (stench) {
      writer.println(name + ": anvil said stench");
      return;
    }
    if ((pulver & EquipmentDatabase.ELEM_SPOOKY) != 0) {
      if (!spooky) {
        writer.println(name + ": anvil didn't say spooky");
      }
      return;
    } else if (spooky) {
      writer.println(name + ": anvil said spooky");
      return;
    }
    if ((pulver & EquipmentDatabase.ELEM_SLEAZE) != 0) {
      if (!sleaze) {
        writer.println(name + ": anvil didn't say sleaze");
      }
      return;
    } else if (sleaze) {
      writer.println(name + ": anvil said sleaze");
      return;
    }
    int myyield = 1;
    while ((pulver & EquipmentDatabase.YIELD_1P) == 0) {
      myyield++;
    }
    if (yield != myyield) {
      writer.println(name + ": anvil said yield is " + yield + ", not " + myyield);
    }
  }

  private static final Pattern ZAPGROUP_PATTERN =
      Pattern.compile("Template:ZAP .*?</a>.*?<td>.*?<td>", Pattern.DOTALL);
  private static final Pattern ZAPITEM_PATTERN = Pattern.compile(">([^<]+)</a>");

  public static final void checkZapGroups() {
    var client = HttpUtilities.getClientBuilder().build();

    RequestLogger.printLine("Checking zap groups...");
    PrintStream report =
        LogStream.openStream(new File(KoLConstants.DATA_LOCATION, "zapreport.txt"), true);

    String[] groups =
        DebugDatabase.ZAPGROUP_PATTERN.split(DebugDatabase.readWikiItemData("Zapping", client));
    for (int i = 1; i < groups.length; ++i) {
      String group = groups[i];
      int pos = group.indexOf("</td>");
      if (pos != -1) {
        group = group.substring(0, pos);
      }
      Matcher m = DebugDatabase.ZAPITEM_PATTERN.matcher(group);
      ArrayList<String> items = new ArrayList<>();
      while (m.find()) {
        items.add(m.group(1));
      }
      if (items.size() > 1) {
        DebugDatabase.checkZapGroup(items, report);
      }
    }
    report.close();
  }

  private static void checkZapGroup(ArrayList<String> items, PrintStream report) {
    String firstItem = items.get(0);
    int itemId = ItemDatabase.getItemId(firstItem);

    if (itemId == -1) {
      report.println("Group with unrecognized item: " + firstItem);
      return;
    }
    List<String> zapGroup = ZapRequest.getZapGroup(itemId);
    if (zapGroup.size() == 0) {
      report.println("New group:");
      for (String item : items) {
        report.print(item);
        report.print(", ");
      }
      report.println();
      return;
    }
    ArrayList<String> existing = new ArrayList<>(zapGroup);
    existing.removeAll(items);
    items.removeAll(zapGroup);
    if (items.size() == 0 && existing.size() == 0) {
      report.println("Group OK: " + firstItem);
      return;
    }
    report.println("Modified group: " + firstItem);
    report.println("Added:");
    Iterator<String> i = items.iterator();
    while (i.hasNext()) {
      report.print(i.next());
      report.print(", ");
    }
    report.println();
    report.println("Removed:");
    i = existing.iterator();
    while (i.hasNext()) {
      report.print(i.next());
      report.print(", ");
    }
    report.println();
  }

  // Check Monster Manuel

  public static final void checkManuel() {
    RequestLogger.printLine("Checking Monster Manuel...");
    DebugDatabase.checkManuelPage("a");
    DebugDatabase.checkManuelPage("b");
    DebugDatabase.checkManuelPage("c");
    DebugDatabase.checkManuelPage("d");
    DebugDatabase.checkManuelPage("e");
    DebugDatabase.checkManuelPage("f");
    DebugDatabase.checkManuelPage("g");
    DebugDatabase.checkManuelPage("h");
    DebugDatabase.checkManuelPage("i");
    DebugDatabase.checkManuelPage("j");
    DebugDatabase.checkManuelPage("k");
    DebugDatabase.checkManuelPage("l");
    DebugDatabase.checkManuelPage("m");
    DebugDatabase.checkManuelPage("n");
    DebugDatabase.checkManuelPage("o");
    DebugDatabase.checkManuelPage("p");
    DebugDatabase.checkManuelPage("q");
    DebugDatabase.checkManuelPage("r");
    DebugDatabase.checkManuelPage("s");
    DebugDatabase.checkManuelPage("t");
    DebugDatabase.checkManuelPage("u");
    DebugDatabase.checkManuelPage("v");
    DebugDatabase.checkManuelPage("w");
    DebugDatabase.checkManuelPage("x");
    DebugDatabase.checkManuelPage("y");
    DebugDatabase.checkManuelPage("z");
    DebugDatabase.checkManuelPage("-");
  }

  private static void checkManuelPage(final String page) {
    RequestLogger.printLine("Page " + page.toUpperCase());
    MonsterManuelRequest request = new MonsterManuelRequest(page);
    RequestThread.postRequest(request);
  }

  // Check Monster Meat from wiki

  public static final void checkMeat() {
    var client = HttpUtilities.getClientBuilder().build();

    for (MonsterData monster : MonsterDatabase.valueSet()) {
      if (!KoLmafia.permitsContinue()) {
        break;
      }
      String wikiData = DebugDatabase.readWikiMonsterData(monster, client);
      int meatDrop = monster.getBaseMeat();
      int baseMeat = 0;
      Matcher matcher = WIKI_MONSTER_MEAT_PATTERN.matcher(wikiData);
      if (matcher.find()) {
        int minMeat = StringUtilities.parseInt(matcher.group(1));
        int maxMeat =
            (matcher.group(2)) == null ? minMeat : StringUtilities.parseInt(matcher.group(2));
        baseMeat = (minMeat + maxMeat) / 2;
      }

      if (meatDrop != baseMeat) {
        RequestLogger.printLine(
            "Monster '"
                + monster.getName()
                + "' ("
                + monster.getId()
                + ") drops "
                + meatDrop
                + " but Wiki says "
                + baseMeat);
      }
    }
  }

  // check mapping between wiki monsters and mafia

  public static final void checkWikiMonsters() {
    var client = HttpUtilities.getClientBuilder().build();

    for (MonsterData monster : MonsterDatabase.valueSet()) {
      if (!KoLmafia.permitsContinue()) {
        break;
      }
      String wikiData = DebugDatabase.readWikiMonsterData(monster, client);
      if (wikiData.length() == 0) {
        RequestLogger.printLine("Failed to read wiki page for " + monster.getName());
        continue;
      }
      var mafiaId = monster.getId();
      int wikiId = 0;
      Matcher matcher = WIKI_MONSTER_ID_PATTERN.matcher(wikiData);
      if (matcher.find()) {
        wikiId = StringUtilities.parseInt(matcher.group(1));
      }

      if (wikiId == 0 || mafiaId != wikiId) {
        RequestLogger.printLine(
            "Monster '"
                + monster.getName()
                + "' ("
                + monster.getId()
                + ") has ID "
                + mafiaId
                + " but Wiki says "
                + wikiId);
      }
    }
  }

  public static final void checkWikiMonsterElementalAttacks() {
    var client = HttpUtilities.getClientBuilder().build();

    for (MonsterData monster : MonsterDatabase.valueSet()) {
      if (!KoLmafia.permitsContinue()) {
        break;
      }
      String wikiData = DebugDatabase.readWikiMonster(monster, client);
      if (wikiData.length() == 0) {
        RequestLogger.printLine("Failed to read wiki page for " + monster.getName());
        continue;
      }
      var mafiaAttacks = monster.getAttackElements();
      Matcher matcher = WIKI_ELEMENT_ATTACK_PATTERN.matcher(wikiData);
      var wikiAttacks =
          matcher
              .results()
              .map(m -> m.group(1))
              .map(Element::fromString)
              .collect(
                  Collectors.toCollection(() -> EnumSet.noneOf(MonsterDatabase.Element.class)));
      if (wikiAttacks.contains(Element.NONE)) {
        RequestLogger.printLine("Unrecognised element for monster " + monster.getName());
      }
      if (wikiAttacks.isEmpty()) {
        wikiAttacks = EnumSet.of(Element.NONE);
      }

      if (!mafiaAttacks.equals(wikiAttacks)) {
        RequestLogger.printLine(
            "Monster '"
                + monster.getName()
                + "' ("
                + monster.getId()
                + ") has attacks "
                + mafiaAttacks
                + " but Wiki says "
                + wikiAttacks);
      }
    }
  }

  public static void checkLocalSVNRepository(File root) {
    List<File> theList;
    theList = new LinkedList<>(curseAgain(root));
    Collections.sort(theList);
    RequestLogger.printLine("Found " + theList.size() + " repo files.");
    Map<String, File> seen = new HashMap<>();
    for (File p : theList) {
      String name = p.getName().toLowerCase();
      if (seen.containsKey(name)) {
        RequestLogger.printLine("***");
        RequestLogger.printLine(seen.get(name).toString());
        RequestLogger.printLine(p.toString());
        RequestLogger.printLine("***");
      } else {
        seen.put(name, p);
      }
    }
  }

  private static List<File> curseAgain(File here) {
    List<File> theList = new LinkedList<>();
    File[] contents = here.listFiles();
    if (contents != null) {
      for (File f : contents) {
        if (f.getName().startsWith(".")) continue;
        if (f.getName().equalsIgnoreCase("dependencies.txt")) continue;
        if (f.isDirectory()) {
          theList.addAll(curseAgain(f));
        } else {
          theList.add(f);
        }
      }
    }
    return (theList);
  }

  public static void checkLocalSVNRepositoryForGitHub(File root) {
    File[] contents = root.listFiles();
    if (contents != null) {
      for (File f : contents) {
        if (f.getName().startsWith(".")) continue;
        if (f.isDirectory()) {
          try {
            SVNURL repo = SVNManager.workingCopyToSVNURL(f);
            if (repo == null) {
              RequestLogger.printLine(
                  f.getName() + " does not seem to have a valid remote repository.");
            } else {
              String repoHost = repo.getHost();
              if (repoHost.equalsIgnoreCase("github.com")) {
                RequestLogger.printLine(
                    "Local installation of " + f.getName() + " uses SVN to update from GitHub.");
              }
            }
          } catch (SVNException e) {
            StaticEntity.printStackTrace(e);
          }
        }
      }
    }
  }
}
