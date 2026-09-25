package net.sourceforge.kolmafia.request;

import com.alibaba.fastjson2.JSONException;
import com.alibaba.fastjson2.JSONObject;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.StaticEntity;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.objectpool.SkillPool;
import net.sourceforge.kolmafia.persistence.ConcoctionDatabase;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.session.EquipmentManager;
import net.sourceforge.kolmafia.session.InventoryManager;
import net.sourceforge.kolmafia.session.MallPriceManager;
import net.sourceforge.kolmafia.utilities.LockableListFactory;

public class ApiRequest extends GenericRequest {
  // The 'what' is parsed in the order defined here, status goes first
  public enum What {
    STATUS("status", "character status", ApiRequest::parseStatus),
    INVENTORY("inventory", "inventory", InventoryManager::parseInventory),
    CLOSET("closet", "closet", ClosetRequest::parseCloset),
    STORAGE("storage", "storage", StorageRequest::parseStorage),
    ITEM("item", "item", null),
    MALL_PRICES("mallprices", "mall prices", MallPriceManager::parseMallPrices);

    private final String what;
    private final String message;
    private final Consumer<JSONObject> parser;

    What(final String what, final String message, final Consumer<JSONObject> parser) {
      this.what = what;
      this.message = message;
      this.parser = parser;
    }
  }

  private static final CharPaneRequest CHARPANE = new CharPaneRequest();

  private final String what;
  private String id;
  private boolean silent = false;

  public ApiRequest(final What... whats) {
    super("api.php");
    this.what = Arrays.stream(whats).map(w -> w.what).collect(Collectors.joining(","));
    this.addFormField("what", this.what);
    this.addFormField("for", "KoLmafia");
    this.id = "";
  }

  public ApiRequest(final What what, final int id) {
    this(what);
    this.addFormField("id", String.valueOf(id));
    this.id = String.valueOf(id);
  }

  @Override
  public String getHashField() {
    return null;
  }

  public static void updateStatus() {
    ApiRequest.updateStatus(false);
  }

  private static final AdventureResult TRANSFUNCTIONER = ItemPool.get(ItemPool.TRANSFUNCTIONER);

  public static synchronized void updateStatus(final boolean silent, final What... also) {
    // In certain LimitModes, API status is incomplete, so use Character Pane instead.

    if (KoLCharacter.getLimitMode().requiresCharPane()) {
      ApiRequest.updateStatusFromCharpane();
      // after the charpane, since parsing inventory depends on the limit mode
      if (also.length > 0) {
        ApiRequest.refresh(silent, also);
      }
      return;
    }

    // Request from api both the status, and the other 'what' wanted
    ApiRequest.refresh(
        silent, Stream.concat(Stream.of(What.STATUS), Arrays.stream(also)).toArray(What[]::new));

    // Some paths and items have state that is only surfaced on the Character Pane
    if (KoLCharacter.inNoobcore() // absorbs and enchantments
        || KoLCharacter.inPokefam() // familiar team
        || KoLCharacter.inDisguise() // current mask
        || KoLCharacter.hasEquipped(TRANSFUNCTIONER)) { // 8-bit Score
      ApiRequest.updateStatusFromCharpane();
    }
  }

  public static void updateStatusFromCharpane() {
    ApiRequest.CHARPANE.run();
  }

  public static void refresh(final What... whats) {
    ApiRequest.refresh(false, whats);
  }

  public static void refresh(final boolean silent, final What... whats) {
    var request = new ApiRequest(whats);
    request.silent = silent;
    request.run();
  }

  public static void updateMallPrices(final String category, final String tiers) {
    ApiRequest request = new ApiRequest(What.MALL_PRICES);
    request.addFormField("category", category);
    if (!tiers.isEmpty()) {
      request.addFormField("tiers", tiers);
    }
    request.addFormField(
        "fields",
        "id,"
            + MallPriceManager.STORE_FIELDS.stream()
                .map(s -> "store." + s)
                .collect(Collectors.joining(",")));
    // The backend api returns a subset of X cheapest stores per item, defaulting to 5 when 'count'
    // is omitted, with a maximum of 20.
    // KoLMafia requests N, as we resolve mall prices based on the Nth cheapest listing.
    request.addFormField("count", String.valueOf(MallPriceManager.NTH_CHEAPEST_COUNT));
    RequestThread.postRequest(request);
  }

  @Override
  protected boolean retryOnTimeout() {
    return true;
  }

  @Override
  public void run() {
    if (!this.silent) {
      List<String> whats = List.of(this.what.split(","));
      String message =
          Arrays.stream(What.values())
              .filter(w -> whats.contains(w.what))
              .map(this::message)
              .collect(Collectors.joining(", "));

      if (!message.isEmpty()) {
        // Updating closet, inventory, storage...
        // Updating item #123...
        KoLmafia.updateDisplay("Updating " + message + "...");
      }
    }

    super.run();
  }

  public JSONObject getJSON() {
    return ApiRequest.getJSON(this.responseText, this.what);
  }

  private String message(final What what) {
    return what == What.ITEM ? what.message + " #" + this.id : what.message;
  }

  @Override
  public void processResults() {
    if (this.redirectLocation != null) {
      return;
    }

    ApiRequest.parseWhat(this.what, this.responseText);
  }

  private static final Pattern WHAT_PATTERN = Pattern.compile("what=([^&]*)");

  public static final void parseResponse(final String location, final String responseText) {
    Matcher whatMatcher = ApiRequest.WHAT_PATTERN.matcher(location);
    if (!whatMatcher.find()) {
      return;
    }

    ApiRequest.parseWhat(GenericRequest.decodeField(whatMatcher.group(1)), responseText);
  }

  private static void parseWhat(final String requestedWhats, final String responseText) {
    // We can request multiple 'what' by separating them with commas (what=mallprices,status)
    // Most API responses are JSON objects, some are arrays.
    // When requesting multiple 'what', valid requests receive a JSON object
    // {"status":{},"events":[]}
    // Currently all requests we handle are JSON objects, so we ignore everything else.
    if (!responseText.startsWith("{")) {
      return;
    }

    JSONObject json = ApiRequest.getJSON(responseText, requestedWhats);
    if (json == null) {
      return;
    }

    List<String> whats = List.of(requestedWhats.split(","));

    for (What what : What.values()) {
      if (what.parser == null || !whats.contains(what.what)) continue;

      // Multiple uses of 'what' are in the form of {"status":{},"mallprices":{}} etc
      // If we are handling multiple, then we retrieve the object, otherwise use the root
      JSONObject object = whats.size() == 1 ? json : json.getJSONObject(what.what);
      what.parser.accept(object);
    }
  }

  /*
    Here's a sample collected on January 8, 2012

    {
      "playerid":"121572",
      "name":"Veracity",
      "hardcore":"0",
      "ascensions":"242",
      "path":"0",
      "sign":"Mongoose",
      "roninleft":"0",
      "casual":"0",
      "drunk":"24",
      "full":"15",
      "turnsplayed":"758794",
      "familiar":"92",
      "hp":"2198",
      "mp":"586",
      "meat":"5231459",
      "adventures":"31",
      "level":"28",
      "rawmuscle":"589584",
      "rawmysticality":"267061",
      "rawmoxie":"267770",
      "basemuscle":"767",
      "basemysticality":"516",
      "basemoxie":"517",
      "familiarexp":"0",
      "class":"1",
      "lastadv":{
        "id":"338",
        "name":"Dreadsylvanian Woods",
        "link":"adventure.php?snarfblat=338",
        "container":"clan_dreadsylvania.php"
      },
      "title":"28",
      "pvpfights":"100",
      "maxhp":2326,
      "maxmp":1409,
      "spleen":"15",
      "muscle":1205,
      "mysticality":811,
      "moxie":709,
      "famlevel":16,
      "locked":false,
      "daysthisrun":"15",
      "equipment":{
        "hat":"5122",
        "shirt":"2586",
        "pants":"4306",
        "weapon":"1325",
        "offhand":"3543",
        "acc1":"5460",
        "acc2":"4309",
        "acc3":"6530",
        "container":"6335",
        "familiarequip":"1325",
        "fakehands":0,
        "cardsleeve":"4968"
      },
      "stickers":[0,0,0],
      "eleronkey":"xxx",
      "flag_config":{
        "lazyinventory":0,
        "compactfights":0,
        "poppvpsearch":0,
        "questtracker":"1",
        "charpanepvp":"1",
        "fffights":"1",
        "compactchar":0,
        "noframesize":0,
        "fullnesscounter":0,
        "nodevdebug":0,
        "noquestnudge":"1",
        "nocalendar":"1",
        "alwaystag":"1",
        "clanlogins":0,
        "quickskills":0,
        "hprestorers":0,
        "hidejacko":0,
        "anchorshelf":0,
        "showoutfit":"1",
        "wowbar":0,
        "swapfam":0,
        "invimages":0,
        "showhandedness":0,
        "acclinks":"1",
        "invadvancedsort":"1",
        "powersort":0,
        "autodiscard":0,
        "unfamequip":0,
        "invclose":0,
        "sellstuffugly":"1",
        "oneclickcraft":0,
        "dontscroll":0,
        "multisume":"1",
        "threecolinv":"1",
        "profanity":"1",
        "tc_updatetitle":0,
        "tc_alwayswho":0,
        "tc_times":0,
        "tc_combineallpublic":0,
        "tc_eventsactive":0,
        "tc_hidebadges":0,
        "tc_colortabs":0,
        "tc_modifierkey":0,
        "tc_tabsonbottom":0,
        "chatversion":"1",
        "aabosses":0,
        "compacteffects":0,
        "slimhpmpdisplay":0,
        "ignorezonewarnings":"1",
        "whichpenpal":"1",
        "compactmanuel":0,
        "hideefarrows":0,
        "autoattack":0,
        "topmenu":0
      },
      "recalledskills":1,
      "freedralph":1,
      "mcd":0,
      "pwd":"638cd7484036dcac3bb4039fef93cb4c",
      "rollover":1378611001,
      "turnsthisrun":6465,
      "familiar_wellfed":0,
      "intrinsics":[],
      "familiarpic":"timesword",
      "pathname":"",
      "effects":{
        "2d6d3ab04b40e1523aa9c716a04b3aab":["Leash of Linguini","14","string","skill:3010","16"],
        "ac32e95f470a7e0999863fa0db58d808":["Empathy","29","empathy","skill:2009","50"],
        "63e73adb3ecfb0cbf544db435eeeaf00":["Fat Leon's Phat Loot Lyric","22","fatleons","skill:6010","67"],
        "626c8ef76cfc003c6ac2e65e9af5fd7a":["Ode to Booze","18","odetobooze","skill:6014","71"],
        "bb44871dd165d4dc9b4d35daa46908ef":["Springy Fusilli","4","fusilli","skill:3015","130"],
        "5c8d3b5b4a6d403f95f27f5d29528c59":["Rage of the Reindeer","4","reindeer","skill:1015","131"],
        "c45d2469bf8cfd8c9bbc953e2a44f3c4":["Starry-Eyed","8","snowflakes","","350"],
        "d75aaa2cc6a8dcfa7cdddef658968e26":["Brother Smothers's Blessing","18","monkhead","","461"],
        "ff0e9ff0189cea375f60b5860463518d":["The Ballad of Richie Thingfinder","4","richie","item:4497","530"],
        "a614dc2ae977f948665b230f5723b567":["Brass Loins","98","flask","item:6428","1273"],
        "1839cbfe882d9243964d9097c1a3b0a6":["Silver Age Secrets","98","flask","item:6429","1274"]
      }
    }
  */

  public static final void parseStatus(final String responseText) {
    ApiRequest.parseStatus(ApiRequest.getJSON(responseText, "status"));
  }

  public static final void parseStatus(final JSONObject json) {
    if (json == null) {
      return;
    }

    // The data in the status is culled from many other KoL
    // pages. Let each page request handler parse the appropriate
    // data from it

    try {
      // Pull out the current ascension count. Do this first.
      // Some later processing depends on this.
      int ascensions = json.getIntValue("ascensions");
      KoLCharacter.setAscensions(ascensions);

      // Other parsers behave differently in Valhalla so do this early
      CharPaneRequest.setInValhalla(json.getIntValue("ascending") == 1);

      // Pull out the current password hash
      String pwd = json.getString("pwd");
      GenericRequest.setPasswordHash(pwd);

      // Many config options are available
      AccountRequest.parseStatus(json);

      // Many things from the Char Sheet are available
      CharSheetRequest.parseStatus(json);

      // It's not possible to tell if some IotMs are bound to
      // the player's account or if they've used a one-day ticket without coolitems
      parseCoolItems(json.getString("coolitems"));

      // Many things from the Char Pane are available
      CharPaneRequest.parseStatus(json);

      KoLCharacter.setClosetMeat(json.getLongValue("closetmeat"));
      KoLCharacter.setStorageMeat(json.getLongValue("storagemeat"));
      // pulls remaining from Hagnk's today; -1 when unlimited
      ConcoctionDatabase.setPullsRemaining(json.getIntValue("pullsleft"));

      var limitmode = KoLCharacter.getLimitMode();
      switch (limitmode) {
        case SPELUNKY:
          // Parse Spelunky equipment
          SpelunkyRequest.parseStatus(json);
          break;
        case BATMAN:
          // Don't mess with equipment
          break;
        default:
          // Parse currently worn equipment
          EquipmentManager.parseStatus(json);
          break;
      }

      // Must be AFTER current familiar is set and equipment is processed
      CharPaneRequest.checkFamiliarWeight(json);

      // UNIX time of next rollover
      long rollover = json.getLong("rollover");
      KoLCharacter.setRollover(rollover);

      // Add the global count of rollovers everyone shares
      int daycount = json.getIntValue("daynumber");
      KoLCharacter.setGlobalDays(daycount);

      // The best place to parse our current graft status if we are a Zootomist
      parseZootomistGrafts(json);
    } catch (JSONException e) {
      ApiRequest.reportParseError("status", json.toString(), e);
    } finally {
      KoLCharacter.recalculateAdjustments();
      KoLCharacter.updateStatus();

      // Mana cost adjustment may have changed
      LockableListFactory.sort(KoLConstants.summoningSkills);
      LockableListFactory.sort(KoLConstants.usableSkills);
    }
  }

  private static final Map<String, Map.Entry<String, String>> PREF_TO_COOL_ITEM =
      Map.ofEntries(
          Map.entry("airport1", Map.entry("sleazeAirportAlways", "_sleazeAirportToday")),
          Map.entry("airport2", Map.entry("spookyAirportAlways", "_spookyAirportToday")),
          Map.entry("airport3", Map.entry("stenchAirportAlways", "_stenchAirportToday")),
          Map.entry("airport4", Map.entry("hotAirportAlways", "_hotAirportToday")),
          Map.entry("airport5", Map.entry("coldAirportAlways", "_coldAirportToday")),
          Map.entry(
              "gingerbreadcity", Map.entry("gingerbreadCityAvailable", "_gingerbreadCityToday")),
          Map.entry("spacegate", Map.entry("spacegateAlways", "_spacegateToday")),
          Map.entry("fantasyrealm", Map.entry("frAlways", "_frToday")),
          Map.entry("piraterealm", Map.entry("prAlways", "_prToday")),
          Map.entry("cyberrealm", Map.entry("crAlways", "_crToday")),
          Map.entry(
              "neverendingparty", Map.entry("neverendingPartyAlways", "_neverendingPartyToday")),
          Map.entry("voterregistered", Map.entry("voteAlways", "_voteToday")),
          Map.entry("boxingdaycare", Map.entry("daycareOpen", "_daycareToday")),
          Map.entry("hascosmicball", Map.entry("hasCosmicBowlingBall", "")),
          Map.entry("maydaykit", Map.entry("hasMaydayContract", "")),
          Map.entry("autumnaton", Map.entry("hasAutumnaton", "")),
          Map.entry("shrunkenhead", Map.entry("hasShrunkenHead", "")),
          Map.entry("interesting", Map.entry("hasInterestingCoin", "")),
          Map.entry("tunnelofloveiotm", Map.entry("loveTunnelAvailable", "_loveTunnelToday")),
          Map.entry("ltt", Map.entry("telegraphOfficeAvailable", "_telegraphOfficeToday")),
          Map.entry("floristfriar", Map.entry("ownsFloristFriar", "")));

  private static void parseCoolItems(final String coolItems) {
    if (coolItems == null) {
      return;
    }

    List<String> owned = Arrays.asList(coolItems.split(","));

    PREF_TO_COOL_ITEM.forEach(
        (coolItem, entry) -> {
          String alwaysPref = entry.getKey();
          String todayPref = entry.getValue();
          boolean haveAccess = owned.contains(coolItem);

          // If there's no such thing as temporary access, then set the always pref directly.
          if (todayPref.isEmpty()) {
            Preferences.setBoolean(alwaysPref, haveAccess);
          } else if (haveAccess) { // If they have access to the iotm
            // If they have used a day pass
            boolean usedDayPass = Preferences.getBoolean(todayPref);

            // If they have used a day pass, they do not always have access
            Preferences.setBoolean(alwaysPref, !usedDayPass);
          } else {
            // No access to the iotm, so set both preferences to false
            Preferences.setBoolean(todayPref, false);
            Preferences.setBoolean(alwaysPref, false);
          }
        });
  }

  protected static void parseZootomistGrafts(final JSONObject json) {
    if (!KoLCharacter.inZootomist()) return;

    var grafts = json.getJSONObject("grafts");

    if (grafts == null) return;

    Preferences.setInteger("zootGraftedHeadFamiliar", grafts.getIntValue("1", 0));
    Preferences.setInteger("zootGraftedShoulderLeftFamiliar", grafts.getIntValue("2", 0));
    Preferences.setInteger("zootGraftedShoulderRightFamiliar", grafts.getIntValue("3", 0));
    var leftHand = grafts.getIntValue("4", 0);
    if (leftHand != 0 && !KoLCharacter.hasCombatSkill(SkillPool.LEFT_PUNCH)) {
      KoLCharacter.addAvailableCombatSkill(SkillPool.LEFT_PUNCH);
    }
    Preferences.setInteger("zootGraftedHandLeftFamiliar", leftHand);
    var rightHand = grafts.getIntValue("5", 0);
    if (rightHand != 0 && !KoLCharacter.hasCombatSkill(SkillPool.RIGHT_PUNCH)) {
      KoLCharacter.addAvailableCombatSkill(SkillPool.RIGHT_PUNCH);
    }
    Preferences.setInteger("zootGraftedHandRightFamiliar", rightHand);
    var rightNipple = grafts.getIntValue("6", 0);
    if (rightNipple != 0 && !KoLCharacter.hasSkill(SkillPool.DRINK_THE_MILK_OF_KINDNESS)) {
      KoLCharacter.addAvailableSkill(SkillPool.DRINK_THE_MILK_OF_KINDNESS);
    }
    Preferences.setInteger("zootGraftedNippleRightFamiliar", rightNipple);
    var leftNipple = grafts.getIntValue("7", 0);
    if (leftNipple != 0 && !KoLCharacter.hasSkill(SkillPool.DRINK_THE_MILK_OF_CRUELTY)) {
      KoLCharacter.addAvailableSkill(SkillPool.DRINK_THE_MILK_OF_CRUELTY);
    }
    Preferences.setInteger("zootGraftedNippleLeftFamiliar", leftNipple);
    Preferences.setInteger("zootGraftedButtCheekLeftFamiliar", grafts.getIntValue("8", 0));
    Preferences.setInteger("zootGraftedButtCheekRightFamiliar", grafts.getIntValue("9", 0));
    var leftFoot = grafts.getIntValue("10", 0);
    if (leftFoot != 0 && !KoLCharacter.hasCombatSkill(SkillPool.LEFT_KICK)) {
      KoLCharacter.addAvailableCombatSkill(SkillPool.LEFT_KICK);
    }
    Preferences.setInteger("zootGraftedFootLeftFamiliar", leftFoot);
    var rightFoot = grafts.getIntValue("11", 0);
    if (rightFoot != 0 && !KoLCharacter.hasCombatSkill(SkillPool.RIGHT_KICK)) {
      KoLCharacter.addAvailableCombatSkill(SkillPool.RIGHT_KICK);
    }
    Preferences.setInteger("zootGraftedFootRightFamiliar", rightFoot);
  }

  public static final JSONObject getJSON(final String text, final String what) {
    // Parse the string into a JSON object
    try {
      String str = ApiRequest.getJSONString(text);
      return str == null ? null : com.alibaba.fastjson2.JSON.parseObject(str);
    } catch (JSONException e) {
      ApiRequest.reportParseError(what, text, e);
    }

    return null;
  }

  private static String getJSONString(String responseText) {
    if (responseText == null) {
      return null;
    }

    int pos = responseText.indexOf("{");
    return switch (pos) {
      case -1 -> null;
      case 0 -> responseText;
      default -> responseText.substring(pos);
    };
  }

  public static final void reportParseError(
      final String what, final String responseText, final JSONException e) {
    KoLmafia.updateDisplay("api.php?what=" + what + " parse error: " + e.getMessage());
    StaticEntity.printStackTrace(e, responseText);
  }
}
