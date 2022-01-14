package net.sourceforge.kolmafia.persistence;

import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.java.dev.spellcast.utilities.LockableListModel;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.AreaCombatData;
import net.sourceforge.kolmafia.KoLAdventure;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLConstants.Stat;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.MonsterData;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.StaticEntity;
import net.sourceforge.kolmafia.objectpool.AdventurePool;
import net.sourceforge.kolmafia.objectpool.IntegerPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.MonsterDatabase.Element;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.ClanRumpusRequest;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.request.RelayRequest;
import net.sourceforge.kolmafia.request.RichardRequest;
import net.sourceforge.kolmafia.utilities.FileUtilities;
import net.sourceforge.kolmafia.utilities.StringArray;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class AdventureDatabase {
  private static final Pattern SNARF_PATTERN = Pattern.compile("snarfblat=(\\d+)");
  private static final Pattern MINE_PATTERN = Pattern.compile("mine=(\\d+)");
  private static final LockableListModel<KoLAdventure> adventures = new LockableListModel<>();
  private static final AdventureArray allAdventures = new AdventureArray();

  public static final List<String> PARENT_LIST = new ArrayList<>();
  public static final Map<String, String> PARENT_ZONES = new HashMap<>();
  public static final Map<String, String> ZONE_DESCRIPTIONS = new HashMap<>();

  private static final StringArray[] adventureTable = new StringArray[4];
  private static final Map<String, AreaCombatData> areaCombatData = new HashMap<>();
  private static final Map<String, KoLAdventure> adventureByURL = new HashMap<>();
  private static final Map<String, KoLAdventure> adventureByName = new HashMap<>();
  private static final Map<String, String> environmentLookup = new HashMap<>();
  private static final Map<String, String> zoneLookup = new HashMap<>();
  private static final Map<String, String> conditionLookup = new HashMap<>();
  private static final Map<String, String> bountyLookup = new HashMap<>();
  private static final Map<String, Integer> statLookup = new HashMap<>();
  private static final Map<String, Integer> waterLevelLookup = new HashMap<>();
  private static final Map<String, Boolean> wandererLookup = new HashMap<>();

  static {
    for (int i = 0; i < AdventureDatabase.adventureTable.length; ++i) {
      AdventureDatabase.adventureTable[i] = new StringArray();
    }

    AdventureDatabase.refreshZoneTable();
    AdventureDatabase.refreshAdventureTable();
    AdventureDatabase.refreshCombatsTable();
    AdventureDatabase.refreshAdventureList();
  }

  public static final AdventureResult[] WOODS_ITEMS = new AdventureResult[12];

  static {
    for (int i = 0; i < 12; ++i) {
      AdventureDatabase.WOODS_ITEMS[i] = ItemPool.get(i + 1);
    }
  }

  // Some adventures don't actually cost a turn
  public static final String[] FREE_ADVENTURES = {"Rock-a-bye larva", "Cobb's Knob lab key"};

  public static final void refreshZoneTable() {
    if (!AdventureDatabase.ZONE_DESCRIPTIONS.isEmpty()) {
      return;
    }

    BufferedReader reader =
        FileUtilities.getVersionedReader("zonelist.txt", KoLConstants.ZONELIST_VERSION);
    if (reader == null) {
      return;
    }

    String[] data;

    while ((data = FileUtilities.readData(reader)) != null) {
      if (data.length >= 3) {
        String zone = data[0];
        String parent = data[1];
        String description = data[2];

        AdventureDatabase.PARENT_ZONES.put(zone, parent);
        if (!AdventureDatabase.PARENT_LIST.contains(parent)) {
          AdventureDatabase.PARENT_LIST.add(parent);
        }

        AdventureDatabase.ZONE_DESCRIPTIONS.put(zone, description);
      }
    }

    try {
      reader.close();
    } catch (Exception e) {
      // This should not happen.  Therefore, print
      // a stack trace for debug purposes.

      StaticEntity.printStackTrace(e);
    }
  }

  public static final void refreshAdventureTable() {
    BufferedReader reader =
        FileUtilities.getVersionedReader("adventures.txt", KoLConstants.ADVENTURES_VERSION);
    if (reader == null) {
      return;
    }

    for (int i = 0; i < AdventureDatabase.adventureTable.length; ++i) {
      AdventureDatabase.adventureTable[i].clear();
    }

    String[] data;

    while ((data = FileUtilities.readData(reader)) != null) {
      if (data.length <= 3) {
        continue;
      }

      String zone = data[0];
      String[] location = data[1].split("=");

      String environment = null;
      int stat = -1;
      int waterLevel = -1;
      boolean hasWanderers = true;
      StringTokenizer tokens = new StringTokenizer(data[2], " ");
      while (tokens.hasMoreTokens()) {
        String option = tokens.nextToken();
        if (option.equals("Env:")) {
          environment = tokens.nextToken();
        } else if (option.equals("Stat:")) {
          stat = StringUtilities.parseInt(tokens.nextToken());
        } else if (option.equals("Level:")) {
          waterLevel = StringUtilities.parseInt(tokens.nextToken());
        } else if (option.equals("nowander")) {
          hasWanderers = false;
        }
      }

      String name = data[3];

      if (environment == null) {
        RequestLogger.printLine("Adventure area \"" + name + "\" is missing environment data");
      }

      if (AdventureDatabase.PARENT_ZONES.get(zone) == null) {
        RequestLogger.printLine(
            "Adventure area \"" + name + "\" has invalid zone: \"" + zone + "\"");
        continue;
      }

      AdventureDatabase.zoneLookup.put(name, zone);
      AdventureDatabase.adventureTable[0].add(zone);
      AdventureDatabase.adventureTable[1].add(location[0] + ".php");
      AdventureDatabase.adventureTable[2].add(location[1]);
      AdventureDatabase.adventureTable[3].add(name);
      AdventureDatabase.environmentLookup.put(name, environment);

      AdventureDatabase.statLookup.put(name, stat);

      hasWanderers = hasWanderers && location[0].equals("adventure");
      AdventureDatabase.wandererLookup.put(name, hasWanderers);

      // Build base water level if not specified
      if (waterLevel == -1) {
        if (environment == null || environment.equals("outdoor") || environment.equals("none")) {
          waterLevel = 1;
        } else if (environment.equals("indoor")) {
          waterLevel = 3;
        } else if (environment.equals("underground")) {
          waterLevel = 5;
        }
        if (stat >= 40) {
          waterLevel++;
        }
        if ("underwater".equals(environment)) {
          waterLevel = 0;
        }
      }

      AdventureDatabase.waterLevelLookup.put(name, waterLevel);

      if (data.length <= 4) {
        continue;
      }

      if (!data[4].equals("")) {
        AdventureDatabase.conditionLookup.put(name, data[4]);
      }
    }

    try {
      reader.close();
    } catch (Exception e) {
      // This should not happen.  Therefore, print
      // a stack trace for debug purposes.

      StaticEntity.printStackTrace(e);
    }
  }

  public static final void refreshCombatsTable() {
    AdventureDatabase.areaCombatData.clear();

    BufferedReader reader =
        FileUtilities.getVersionedReader("combats.txt", KoLConstants.COMBATS_VERSION);
    if (reader == null) {
      return;
    }

    String[] data;

    while ((data = FileUtilities.readData(reader)) != null) {
      if (data.length > 1) {
        String area = data[0];
        if (!AdventureDatabase.validateAdventureArea(area)) {
          RequestLogger.printLine("Invalid adventure area: \"" + area + "\"");
          continue;
        }

        int combats = StringUtilities.parseInt(data[1]);
        // There can be an ultra-rare monster even if
        // there are no other combats
        AreaCombatData combat = new AreaCombatData(area, combats);
        for (int i = 2; i < data.length; ++i) {
          String monsterName = data[i];
          if (!combat.addMonster(monsterName)) {
            KoLmafia.updateDisplay("(In area '" + area + "')");
          }
          // Does it drop a bounty, if so add it to the bounty lookup by area
          // Trim any trailing ":" and following text
          int colonIndex = data[i].indexOf(":");
          if (colonIndex > 0) {
            monsterName = monsterName.substring(0, colonIndex);
          }
          String bountyName = BountyDatabase.getNameByMonster(monsterName);
          if (bountyName != null) {
            AdventureDatabase.bountyLookup.put(area, bountyName);
          }
        }

        AdventureDatabase.areaCombatData.put(area, combat);
      }
    }

    try {
      reader.close();
    } catch (Exception e) {
      // This should not happen.  Therefore, print
      // a stack trace for debug purposes.

      StaticEntity.printStackTrace(e);
    }
  }

  public static final void refreshAdventureList() {
    AdventureDatabase.adventures.clear();
    AdventureDatabase.allAdventures.clear();
    AdventureDatabase.adventureByURL.clear();
    AdventureDatabase.adventureByName.clear();

    for (int i = 0; i < AdventureDatabase.adventureTable[0].size(); ++i) {
      AdventureDatabase.addAdventure(AdventureDatabase.getAdventure(i));
    }
  }

  public static final boolean isPirateRealmIsland(final String url) {
    return url.contains("snarfblat=531");
  }

  public static final String pirateRealmIslandName() {
    String island = Preferences.getString("_LastPirateRealmIsland");
    return island.isEmpty() ? "PirateRealm Island" : island;
  }

  public static final void addAdventure(final KoLAdventure location) {
    AdventureDatabase.adventures.add(location);
    AdventureDatabase.allAdventures.add(location);
    AdventureDatabase.adventureByName.put(location.getAdventureName(), location);

    GenericRequest request = location.getRequest();

    // This will force the URLstring to be reconstructed and the
    // correct password hash inserted when the request is run
    request.removeFormField("pwd");

    String url = request.getURLString();

    // All PirateRealm Island adventures have the same URL, so we
    // can't look them up that way. Don't add them to the hash table
    if (isPirateRealmIsland(url)) {
      return;
    }

    AdventureDatabase.adventureByURL.put(url, location);

    if (url.contains("snarfblat=")) {
      // The map of the Bat Hole has a bogus URL for the Boss Bat's lair
      if (url.contains("snarfblat=34")) {
        AdventureDatabase.adventureByURL.put(url + ";", location);
      }

      url = StringUtilities.singleStringReplace(url, "snarfblat=", "adv=");
      AdventureDatabase.adventureByURL.put(url, location);
    }
  }

  public static final LockableListModel<KoLAdventure> getAsLockableListModel() {
    if (AdventureDatabase.adventures.isEmpty()) {
      AdventureDatabase.refreshAdventureList();
    }

    return AdventureDatabase.adventures;
  }

  public static final KoLAdventure getAdventureByURL(String adventureURL) {
    if (AdventureDatabase.adventureByURL.isEmpty()) {
      AdventureDatabase.refreshAdventureList();
    }

    if (adventureURL.startsWith("/")) {
      adventureURL = adventureURL.substring(1);
    }

    // Barrel smashes count as adventures.
    if (adventureURL.startsWith("barrel.php")) {
      return AdventureDatabase.adventureByURL.get("barrel.php");
    }

    // Visiting the basement counts as an adventure
    if (adventureURL.startsWith("basement.php")) {
      return AdventureDatabase.adventureByURL.get("basement.php");
    }

    // Visiting the tavern cellar might count as an adventure
    if (adventureURL.startsWith("cellar.php")) {
      // action=explore or action=autofaucet
      String action = GenericRequest.extractField(adventureURL, "action");
      if (action == null) {
        return null;
      }
      return AdventureDatabase.adventureByURL.get("cellar.php");
    }

    // Mining in disguise count as adventures.
    if (adventureURL.startsWith("mining.php")) {
      String mine = GenericRequest.extractField(adventureURL, "mine");
      if (mine == null) {
        return null;
      }
      return AdventureDatabase.adventureByURL.get("mining.php?" + mine);
    }

    if (adventureURL.startsWith("place.php")) {
      // Adventuring in the Lower Chamber
      if (adventureURL.contains("action=pyramid_state")) {
        return AdventureDatabase.getAdventureByName("The Lower Chambers");
      }

      if (adventureURL.contains("whichplace=nstower")) {
        if (adventureURL.contains("action=ns_01_crowd2")) {
          String stat = Preferences.getString("nsChallenge1");
          String adventure =
              stat.equals(Stat.MUSCLE.toString())
                  ? "Strongest Adventurer Contest"
                  : stat.equals(Stat.MYSTICALITY.toString())
                      ? "Smartest Adventurer Contest"
                      : stat.equals(Stat.MOXIE.toString())
                          ? "Smoothest Adventurer Contest"
                          : "A Crowd of (Stat) Adventurers";
          return AdventureDatabase.getAdventureByName(adventure);
        }
        if (adventureURL.contains("action=ns_01_crowd3")) {
          String element = Preferences.getString("nsChallenge2");
          String adventure =
              element.equals(Element.HOT.toString())
                  ? "Hottest Adventurer Contest"
                  : element.equals(Element.COLD.toString())
                      ? "Coldest Adventurer Contest"
                      : element.equals(Element.SPOOKY.toString())
                          ? "Spookiest Adventurer Contest"
                          : element.equals(Element.STENCH.toString())
                              ? "Stinkiest Adventurer Contest"
                              : element.equals(Element.SLEAZE.toString())
                                  ? "Sleaziest Adventurer Contest"
                                  : "A Crowd of (Element) Adventurers";
          return AdventureDatabase.getAdventure(adventure);
        }
      }

      // place.php?whichplace=manor4&action=manor4_chamber
      // place.php?whichplace=manor4&action=manor4_chamberboss
      // place.php?whichplace=manor4&action=manor4_chamberwall
      // Adventuring in the Summoning Chamber
      if (adventureURL.contains("action=manor4_chamberboss")) {
        return AdventureDatabase.getAdventure("Summoning Chamber");
      }

      // place.php?whichplace=beanstalk&action=stalk_eincursion
      // place.php?whichplace=bordertown&action=bordertown_eincursion
      // place.php?whichplace=bordertown&action=bordertown_eincursion2
      // place.php?whichplace=cemetery&action=cem_eincursion
      // place.php?whichplace=desertbeach&action=db_eincursion
      // place.php?whichplace=forestvillage&action=fv_eincursion
      // place.php?whichplace=giantcastle&action=castle_eincursion
      // place.php?whichplace=manor1&action=manor1_eincursion
      // place.php?whichplace=mclargehuge&action=mlh_eincurions
      // place.php?whichplace=mountains&action=mts_eincursion
      // place.php?whichplace=plains&action=plains_eincursion
      // place.php?whichplace=thesea&action=thesea_zenicursio
      // place.php?whichplace=town&action=town_eincursion
      // place.php?whichplace=town&action=town_eincursion2
      // place.php?whichplace=town&action=town_eincursion3
      // place.php?whichplace=town_market&action=townmarket_eincursion
      // place.php?whichplace=town_wrong&action=townrwong_eincursion
      // place.php?whichplace=woods&action=woods_eincursion
      if (adventureURL.contains("action=town_eincursion")
      // Main town fissure first, others alphabetically
      // Disable all the secondary fissures for now
      /*
      || adventureURL.contains( "action=bordertown_eincursion" )
      || adventureURL.contains( "action=bordertown_eincursion2" )
      || adventureURL.contains( "action=castle_eincursion" )
      || adventureURL.contains( "action=cem_eincursion" )
      || adventureURL.contains( "action=db_eincursion" )
      || adventureURL.contains( "action=fv_eincursion" )
      || adventureURL.contains( "action=manor1_eincursion" )
      || adventureURL.contains( "action=mlh_eincurions" )
      || adventureURL.contains( "action=mts_eincursion" )
      || adventureURL.contains( "action=plains_eincursion" )
      || adventureURL.contains( "action=stalk_eincursion" )
      || adventureURL.contains( "action=thesea_zenicursio" )
      || adventureURL.contains( "action=town_eincursion2" )
      || adventureURL.contains( "action=town_eincursion3" )
      || adventureURL.contains( "action=townmarket_eincursion" )
      || adventureURL.contains( "action=townrwong_eincursion" )
      || adventureURL.contains( "action=woods_eincursion" )
      */
      ) {
        return AdventureDatabase.getAdventure("An Eldritch Fissure");
      }

      if (adventureURL.contains("action=townwrong_tunnel")) {
        return AdventureDatabase.getAdventure("The Tunnel of L.O.V.E.");
      }

      if (adventureURL.contains("action=town_eicfight2")) {
        return AdventureDatabase.getAdventure("An Eldritch Horror");
      }

      // place.php?whichplace=ioty2014_wolf&action=wolf_houserun
      if (adventureURL.contains("action=wolf_houserun")) {
        return AdventureDatabase.getAdventure("Unleash Your Inner Wolf");
      }
    }

    // Adventuring in the barracks after the Nemesis has been defeated
    if (adventureURL.startsWith("volcanoisland.php") && adventureURL.contains("action=tuba")) {
      return AdventureDatabase.getAdventure("The Island Barracks");
    }

    adventureURL = RelayRequest.removeConfirmationFields(adventureURL);
    adventureURL = GenericRequest.removeField(adventureURL, "pwd");
    adventureURL = GenericRequest.removeField(adventureURL, "blech");
    adventureURL =
        StringUtilities.singleStringReplace(
            adventureURL, "action=ignorewarning&whichzone", "snarfblat");

    KoLAdventure location = AdventureDatabase.adventureByURL.get(adventureURL);
    if (location != null) {
      // *** Why exclude these?
      return location.getRequest() instanceof ClanRumpusRequest
              || location.getRequest() instanceof RichardRequest
          ? null
          : location;
    }

    if (isPirateRealmIsland(adventureURL)) {
      return getAdventure(pirateRealmIslandName());
    }

    return null;
  }

  public static final KoLAdventure getAdventureByName(String name) {
    // Exact match, as supplied by KoL
    if (name == null || name.equals("")) {
      return null;
    }

    if (AdventureDatabase.adventureByName.isEmpty()) {
      AdventureDatabase.refreshAdventureList();
    }

    return AdventureDatabase.adventureByName.get(name);
  }

  public static final KoLAdventure getAdventure(final String adventureName) {
    // Fuzzy matching
    if (adventureName == null || adventureName.equals("")) {
      return null;
    }

    if (AdventureDatabase.allAdventures.isEmpty()) {
      AdventureDatabase.refreshAdventureList();
    }

    return AdventureDatabase.allAdventures.find(adventureName);
  }

  private static KoLAdventure getAdventure(final int tableIndex) {
    return new KoLAdventure(
        AdventureDatabase.adventureTable[0].get(tableIndex),
        AdventureDatabase.adventureTable[1].get(tableIndex),
        AdventureDatabase.adventureTable[2].get(tableIndex),
        AdventureDatabase.adventureTable[3].get(tableIndex));
  }

  public static final String getZone(final String location) {
    return zoneLookup.get(location);
  }

  public static final String getParentZone(final String zone) {
    return PARENT_ZONES.get(zone);
  }

  public static final AdventureResult getBounty(final KoLAdventure adventure) {
    String adventureName = adventure.getAdventureName();
    String bounty = AdventureDatabase.bountyLookup.get(adventureName);
    if (bounty == null || bounty.equals("")) {
      return null;
    }
    int count = BountyDatabase.getNumber(bounty);
    return new AdventureResult(bounty, count);
  }

  public static final String getDefaultConditions(final KoLAdventure adventure) {
    if (adventure == null) {
      return "none";
    }

    // If you're currently doing a bounty, +1 filthy lucre.

    String adventureName = adventure.getAdventureName();
    String bounty = AdventureDatabase.bountyLookup.get(adventureName);

    if (bounty != null && !bounty.equals("")) {
      String easyBountyId = Preferences.getString("currentEasyBountyItem");
      if (!easyBountyId.equals("")) {
        if (bounty.equals(easyBountyId.substring(0, easyBountyId.indexOf(":")))) {
          return "+1 filthy lucre";
        }
      }

      String hardBountyId = Preferences.getString("currentHardBountyItem");
      if (!hardBountyId.equals("")) {
        if (bounty.equals(hardBountyId.substring(0, hardBountyId.indexOf(":")))) {
          return "+1 filthy lucre";
        }
      }

      String specialBountyId = Preferences.getString("currentSpecialBountyItem");
      if (!specialBountyId.equals("")) {
        if (bounty.equals(specialBountyId.substring(0, specialBountyId.indexOf(":")))) {
          return "+1 filthy lucre";
        }
      }
    }

    String def = "none";

    // Pull the condition out of the table and return it.

    String conditions = AdventureDatabase.conditionLookup.get(adventureName);
    if (conditions == null || conditions.equals("")) {
      return def;
    }

    if (!def.equals("none")) {
      conditions = def + "|" + conditions;
    }

    return conditions;
  }

  public static final LockableListModel<String> getDefaultConditionsList(
      final KoLAdventure adventure, LockableListModel<String> list) {
    String string = AdventureDatabase.getDefaultConditions(adventure);
    String[] conditions = string.split("\\|");
    if (list == null) {
      list = new LockableListModel<String>();
    } else {
      list.clear();
    }

    for (int i = 0; i < conditions.length; ++i) {
      list.add(conditions[i]);
    }

    return list;
  }

  public static final boolean isFreeAdventure(final String text) {
    for (int i = 0; i < AdventureDatabase.FREE_ADVENTURES.length; ++i) {
      if (text.contains(AdventureDatabase.FREE_ADVENTURES[i])) {
        return true;
      }
    }
    return false;
  }

  public static final boolean validateAdventureArea(final String area) {
    StringArray areas = AdventureDatabase.adventureTable[3];

    for (int i = 0; i < areas.size(); ++i) {
      if (area.equals(areas.get(i))) {
        return true;
      }
    }
    return false;
  }

  public static final AreaCombatData getAreaCombatData(String area) {
    // Strip out zone name if present
    int index = area.indexOf(":");
    if (index != -1) {
      area = area.substring(index + 2);
    }

    // Get the combat data
    return AdventureDatabase.areaCombatData.get(area);
  }

  public static final ArrayList<String> getAreasWithMonster(MonsterData monster) {
    ArrayList<String> zones = new ArrayList<String>();

    for (Entry<String, AreaCombatData> entry : AdventureDatabase.areaCombatData.entrySet()) {
      AreaCombatData area = entry.getValue();
      if (area.hasMonster(monster)) {
        zones.add(entry.getKey());
      }
    }

    return zones;
  }

  public static final String getUnknownName(final String urlString) {
    if (urlString.startsWith("adventure.php")) {
      Matcher matcher = AdventureDatabase.SNARF_PATTERN.matcher(urlString);
      if (matcher.find()) {
        String name = "Unknown Adventure #" + matcher.group(1);
        String message = name + " = " + urlString;
        RequestLogger.printLine(message);
        RequestLogger.updateSessionLog(message);
        return name;
      }
      return null;
    }

    if (urlString.startsWith("cave.php")) {
      if (urlString.contains("action=sanctum")) {
        return "Nemesis Cave: Inner Sanctum";
      }
      return null;
    }

    if (urlString.startsWith("guild.php?action=chal")) {
      return "Guild Challenge";
    }

    if (urlString.startsWith("mining.php")) {
      if (urlString.contains("intro=1")) {
        return null;
      }
      Matcher matcher = AdventureDatabase.MINE_PATTERN.matcher(urlString);
      return matcher.find() ? "Unknown Mine #" + matcher.group(1) : null;
    }

    if (urlString.startsWith("place.php")) {
      if (urlString.contains("whichplace=ioty2014_rumple")) {
        if (urlString.contains("action=workshop")) {
          return "Rumpelstiltskin's Workshop";
        }
        return null;
      }

      return null;
    }

    if (urlString.startsWith("sea_merkin.php")) {
      if (urlString.contains("action=temple")) {
        return "Mer-kin Temple";
      }
      return null;
    }

    if (urlString.startsWith("town.php")) {
      if (urlString.contains("action=trickortreat")) {
        return "Trick-or-Treating";
      }
      return null;
    }

    return null;
  }

  public static final Object[][] FISTCORE_SCROLLS = {
    // Adventure Zone
    // Adventure ID
    // Setting
    {
      "The Haiku Dungeon",
      IntegerPool.get(AdventurePool.HAIKU_DUNGEON),
      "fistTeachingsHaikuDungeon",
    },
    {
      "The Poker Room", IntegerPool.get(AdventurePool.POKER_ROOM), "fistTeachingsPokerRoom",
    },
    {
      "A Barroom Brawl", IntegerPool.get(AdventurePool.BARROOM_BRAWL), "fistTeachingsBarroomBrawl",
    },
    {
      "The Haunted Conservatory",
      IntegerPool.get(AdventurePool.HAUNTED_CONSERVATORY),
      "fistTeachingsConservatory",
    },
    {
      "The Bat Hole Entrance",
      IntegerPool.get(AdventurePool.BAT_HOLE_ENTRYWAY),
      "fistTeachingsBatHole",
    },
    {
      "The \"Fun\" House", IntegerPool.get(AdventurePool.FUN_HOUSE), "fistTeachingsFunHouse",
    },
    {
      "Cobb's Knob Menagerie Level 2",
      IntegerPool.get(AdventurePool.MENAGERIE_LEVEL_2),
      "fistTeachingsMenagerie",
    },
    {
      "Pandamonium Slums", IntegerPool.get(AdventurePool.PANDAMONIUM_SLUMS), "fistTeachingsSlums",
    },
    {
      "Frat House", IntegerPool.get(AdventurePool.FRAT_HOUSE), "fistTeachingsFratHouse",
    },
    {
      "Road to the White Citadel",
      IntegerPool.get(AdventurePool.ROAD_TO_WHITE_CITADEL),
      "fistTeachingsRoad",
    },
    {
      "Lair of the Ninja Snowmen",
      IntegerPool.get(AdventurePool.NINJA_SNOWMEN),
      "fistTeachingsNinjaSnowmen",
    },
  };

  private static int fistcoreDataLocation(final Object[] data) {
    return (data == null) ? -1 : ((Integer) data[1]).intValue();
  }

  private static String fistcoreDataSetting(final Object[] data) {
    return (data == null) ? null : ((String) data[2]);
  }

  private static Object[] fistcoreLocationToData(final int location) {
    for (int i = 0; i < FISTCORE_SCROLLS.length; ++i) {
      Object[] data = FISTCORE_SCROLLS[i];
      int loc = fistcoreDataLocation(data);
      if (location == loc) {
        return data;
      }
    }
    return null;
  }

  public static String fistcoreLocationToSetting(final int location) {
    return fistcoreDataSetting(fistcoreLocationToData(location));
  }

  public static final String getEnvironment(String adventureName) {
    String env = AdventureDatabase.environmentLookup.get(adventureName);
    return env == null ? "none" : env;
  }

  public static final int getRecommendedStat(String adventureName) {
    Integer stat = AdventureDatabase.statLookup.get(adventureName);
    if (stat == null) {
      return -1;
    }
    return stat;
  }

  public static final int getWaterLevel(String adventureName) {
    Integer waterLevel = AdventureDatabase.waterLevelLookup.get(adventureName);
    if (waterLevel == null) {
      return -1;
    }
    return waterLevel;
  }

  public static final boolean hasWanderers(final String adventureName, final boolean adv) {
    Boolean hasWanderers = AdventureDatabase.wandererLookup.get(adventureName);
    if (hasWanderers == null) {
      return adv;
    }

    return hasWanderers;
  }

  public static class AdventureArray {
    private String[] nameArray = new String[0];
    private final ArrayList<String> nameList = new ArrayList<String>();
    private final ArrayList<KoLAdventure> internalList = new ArrayList<KoLAdventure>();

    public KoLAdventure get(final int index) {
      if (index < 0 || index > this.internalList.size()) {
        return null;
      }

      return this.internalList.get(index);
    }

    public void add(final KoLAdventure value) {
      this.nameList.add(StringUtilities.getCanonicalName(value.getAdventureName()));
      this.internalList.add(value);
    }

    public KoLAdventure find(String adventureName) {
      if (nameArray.length != nameList.size()) {
        nameArray = new String[nameList.size()];
        nameList.toArray(nameArray);
        Arrays.sort(nameArray);
      }

      List<String> matchingNames = StringUtilities.getMatchingNames(nameArray, adventureName);

      if (matchingNames.size() > 1) {
        for (int i = 0; i < matchingNames.size(); ++i) {
          RequestLogger.printLine(matchingNames.get(i));
        }

        KoLmafia.updateDisplay(MafiaState.ERROR, "Multiple matches against " + adventureName + ".");
        return null;
      }

      if (matchingNames.size() == 1) {
        String match = matchingNames.get(0);
        return this.get(nameList.indexOf(match));
      }

      return null;
    }

    public void clear() {
      this.nameList.clear();
      this.internalList.clear();
    }

    public int size() {
      return this.internalList.size();
    }

    public boolean isEmpty() {
      return this.internalList.size() == 0;
    }
  }
}
