package net.sourceforge.kolmafia.persistence;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.SortedMap;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.java.dev.spellcast.utilities.LockableListModel;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.AreaCombatData;
import net.sourceforge.kolmafia.AscensionPath;
import net.sourceforge.kolmafia.AscensionPath.Path;
import net.sourceforge.kolmafia.KoLAdventure;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLConstants.Stat;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.MonsterData;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.StaticEntity;
import net.sourceforge.kolmafia.objectpool.AdventurePool;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.MonsterDatabase.Element;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.ClanRumpusRequest;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.request.RelayRequest;
import net.sourceforge.kolmafia.request.RichardRequest;
import net.sourceforge.kolmafia.utilities.FileUtilities;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class AdventureDatabase {
  private static final Pattern SNARF_PATTERN = Pattern.compile("snarfblat=(\\d+)");
  private static final Pattern MINE_PATTERN = Pattern.compile("mine=(\\d+)");
  private static final LockableListModel<KoLAdventure> adventures = new LockableListModel<>();
  private static final AdventureArray allAdventures = new AdventureArray();

  public static final List<String> PARENT_LIST = new ArrayList<>();
  public static final Map<String, String> PARENT_ZONES = new HashMap<>();
  public static final Map<String, String> ZONE_DESCRIPTIONS = new HashMap<>();

  private record Adventure(String zone, String formSource, String id, String name) {}

  private static final List<Adventure> adventureTable = new ArrayList<>();

  private static final Map<String, AreaCombatData> areaCombatData = new HashMap<>();
  private static final Map<String, KoLAdventure> adventureByURL = new HashMap<>();
  private static final Map<String, KoLAdventure> adventureByName = new HashMap<>();
  private static final Map<Integer, KoLAdventure> adventureById = new HashMap<>();
  private static final Map<String, DifficultyLevel> diffLevelLookup = new HashMap<>();
  private static final Map<String, Environment> environmentLookup = new HashMap<>();
  private static final Map<String, String> zoneLookup = new HashMap<>();
  private static final Map<String, String> conditionLookup = new HashMap<>();
  private static final Map<String, String> bountyLookup = new HashMap<>();
  private static final Map<String, Integer> statLookup = new HashMap<>();
  private static final Map<String, Integer> waterLevelLookup = new HashMap<>();
  private static final Map<String, Boolean> wandererLookup = new HashMap<>();
  private static final Map<String, Boolean> overdrunkLookup = new HashMap<>();
  private static final Map<String, Path> ascensionPathZones = new HashMap<>();
  private static final Map<String, AdventureResult> itemGeneratedZones = new HashMap<>();

  static {
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

  private AdventureDatabase() {}

  public enum Environment {
    UNKNOWN,
    NONE,
    INDOOR,
    OUTDOOR,
    UNDERGROUND,
    UNDERWATER;

    @Override
    public String toString() {
      return this.name().toLowerCase();
    }

    public String toTitle() {
      return StringUtilities.capitalize(this.toString());
    }

    public static Optional<Environment> fromString(String text) {
      if (text == null || text.isEmpty()) return Optional.empty();
      return Arrays.stream(values()).filter(e -> e.name().equalsIgnoreCase(text)).findAny();
    }

    public boolean isUnderwater() {
      return this == Environment.UNDERWATER;
    }
  }

  public enum DifficultyLevel {
    UNKNOWN,
    NONE,
    LOW,
    MID,
    HIGH;

    @Override
    public String toString() {
      return this.name().toLowerCase();
    }

    public String toTitle() {
      return StringUtilities.capitalize(this.toString());
    }

    public static Optional<DifficultyLevel> fromString(String text) {
      if (text == null || text.isEmpty()) return Optional.empty();
      return Arrays.stream(values()).filter(e -> e.name().equalsIgnoreCase(text)).findAny();
    }
  }

  public static final void refreshZoneTable() {
    if (!AdventureDatabase.ZONE_DESCRIPTIONS.isEmpty()) {
      return;
    }

    try (BufferedReader reader =
        FileUtilities.getVersionedReader("zonelist.txt", KoLConstants.ZONELIST_VERSION)) {
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

          if (data.length == 3) {
            // Perhaps inherit from parent zone
            Path apath = ascensionPathZones.getOrDefault(parent, Path.NONE);
            if (apath != Path.NONE) {
              ascensionPathZones.put(zone, apath);
            }
            AdventureResult item = itemGeneratedZones.get(parent);
            if (item != null) {
              itemGeneratedZones.put(zone, item);
            }
            continue;
          }

          String source = data[3];

          // See if it is an Ascension Path
          Path path = AscensionPath.nameToPath(source);
          if (path != Path.NONE) {
            ascensionPathZones.put(zone, path);
            continue;
          }

          // See if it is an Item name
          int itemId = ItemDatabase.getItemId(source);
          if (itemId > 0) {
            itemGeneratedZones.put(zone, ItemPool.get(itemId));
            continue;
          }
          RequestLogger.printLine(
              "Adventure zone \"" + zone + "\" has unrecognizable source: \"" + source + "\"");
        }
      }
    } catch (IOException e) {
      StaticEntity.printStackTrace(e);
    }
  }

  public static final void refreshAdventureTable() {
    try (BufferedReader reader =
        FileUtilities.getVersionedReader("adventures.txt", KoLConstants.ADVENTURES_VERSION)) {
      if (reader == null) {
        return;
      }

      AdventureDatabase.adventureTable.clear();

      String[] data;

      while ((data = FileUtilities.readData(reader)) != null) {
        if (data.length <= 3) {
          continue;
        }

        String zone = data[0];
        String[] location = data[1].split("=");

        String diffLevel = null;
        String environment = null;
        int stat = -1;
        int waterLevel = -1;
        boolean hasWanderers = true;
        boolean canAdventureWhileOverdrunk = false;
        StringTokenizer tokens = new StringTokenizer(data[2], " ");
        while (tokens.hasMoreTokens()) {
          String option = tokens.nextToken();
          switch (option) {
            case "DiffLevel:" -> diffLevel = tokens.nextToken();
            case "Env:" -> environment = tokens.nextToken();
            case "Stat:" -> stat = StringUtilities.parseInt(tokens.nextToken());
            case "Level:" -> waterLevel = StringUtilities.parseInt(tokens.nextToken());
            case "nowander" -> hasWanderers = false;
            case "overdrunk" -> canAdventureWhileOverdrunk = true;
          }
        }

        String name = data[3];

        if (AdventureDatabase.getParentZone(zone) == null) {
          RequestLogger.printLine(
              "Adventure area \"" + name + "\" has invalid zone: \"" + zone + "\"");
          continue;
        }

        AdventureDatabase.zoneLookup.put(name, zone);
        AdventureDatabase.adventureTable.add(
            new Adventure(zone, location[0] + ".php", location[1], name));
        var dl = DifficultyLevel.fromString(diffLevel);
        if (dl.isPresent()) {
          AdventureDatabase.diffLevelLookup.put(name, dl.get());
        } else {
          RequestLogger.printLine("Adventure area \"" + name + "\" has invalid difflevel data");
        }
        var env = Environment.fromString(environment);
        if (env.isPresent()) {
          AdventureDatabase.environmentLookup.put(name, env.get());
        } else {
          RequestLogger.printLine("Adventure area \"" + name + "\" has invalid environment data");
        }

        AdventureDatabase.statLookup.put(name, stat);

        hasWanderers = hasWanderers && location[0].equals("adventure");
        AdventureDatabase.wandererLookup.put(name, hasWanderers);

        AdventureDatabase.overdrunkLookup.put(name, canAdventureWhileOverdrunk);

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
    } catch (IOException e) {
      StaticEntity.printStackTrace(e);
    }
  }

  public static final void refreshCombatsTable() {
    AdventureDatabase.areaCombatData.clear();

    try (BufferedReader reader =
        FileUtilities.getVersionedReader("combats.txt", KoLConstants.COMBATS_VERSION)) {
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
    } catch (IOException e) {
      StaticEntity.printStackTrace(e);
    }
  }

  public static final void refreshAdventureList() {
    AdventureDatabase.adventures.clear();
    AdventureDatabase.allAdventures.clear();
    AdventureDatabase.adventureByURL.clear();
    AdventureDatabase.adventureByName.clear();
    AdventureDatabase.adventureById.clear();

    for (Adventure adv : AdventureDatabase.adventureTable) {
      AdventureDatabase.addAdventure(AdventureDatabase.getAdventure(adv));
    }

    // Backwards compatibility for changed adventure names
    addSynonym("Hippy Camp (Hippy Disguise)", "Hippy Camp in Disguise");
    addSynonym("Frat House (Frat Disguise)", "Frat House in Disguise");
    addSynonym("The Junkyard", "Post-War Junkyard");
  }

  // For looking up adventures by legacy name
  private static final void addSynonym(String name, String synonym) {
    KoLAdventure location = AdventureDatabase.getAdventure(name);
    AdventureDatabase.allAdventures.addSynonym(synonym, location);
    AdventureDatabase.adventureByName.put(synonym, location);
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

    if (location.hasSnarfblat()) {
      AdventureDatabase.adventureById.put(location.getSnarfblat(), location);
    }

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

  public static final Path zoneAscensionPath(String zone) {
    return AdventureDatabase.ascensionPathZones.getOrDefault(zone, Path.NONE);
  }

  public static final AdventureResult zoneGeneratingItem(String zone) {
    return AdventureDatabase.itemGeneratedZones.get(zone);
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
    if (adventureURL.startsWith("volcanoisland.php")) {
      if (adventureURL.contains("action=tuba")) {
        return AdventureDatabase.getAdventure("The Island Barracks");
      }
      if (adventureURL.contains("action=tniat")) {
        return AdventureDatabase.getAdventure("The Nemesis' Lair");
      }
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

  private static KoLAdventure getAdventure(final Adventure adv) {
    return new KoLAdventure(adv.zone, adv.formSource, adv.id, adv.name);
  }

  public static KoLAdventure getAdventure(final int snarfblat) {
    return adventureById.get(snarfblat);
  }

  public static String getZone(final String location) {
    return zoneLookup.get(location);
  }

  public static String getRootZone(final String zoneName) {
    return getRootZone(zoneName, List.of());
  }

  public static String getRootZone(String zoneName, final List<String> stopAtZones) {
    while (true) {
      if (stopAtZones.contains(zoneName)) {
        return zoneName;
      }

      String parent = getParentZone(zoneName);

      if (parent == null || parent.equals(zoneName)) {
        return zoneName;
      }

      zoneName = parent;
    }
  }

  public static String getParentZone(final String zone) {
    return PARENT_ZONES.get(zone);
  }

  public static AdventureResult getBounty(final KoLAdventure adventure) {
    String adventureName = adventure.getAdventureName();
    String bounty = AdventureDatabase.bountyLookup.getOrDefault(adventureName, "");
    if (bounty.equals("")) {
      return null;
    }
    int count = BountyDatabase.getNumber(bounty);
    return new AdventureResult(bounty, count);
  }

  public static String getDefaultConditions(final KoLAdventure adventure) {
    if (adventure == null) {
      return "none";
    }

    // If you're currently doing a bounty, +1 filthy lucre.

    String adventureName = adventure.getAdventureName();
    String bounty = AdventureDatabase.bountyLookup.getOrDefault(adventureName, "");

    if (!bounty.equals("")) {
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

    // Pull the condition out of the table and return it.

    String conditions = AdventureDatabase.conditionLookup.getOrDefault(adventureName, "");
    if (conditions.equals("")) {
      return "none";
    }

    return conditions;
  }

  public static LockableListModel<String> getDefaultConditionsList(
      final KoLAdventure adventure, LockableListModel<String> list) {
    String string = AdventureDatabase.getDefaultConditions(adventure);
    String[] conditions = string.split("\\|");
    if (list == null) {
      list = new LockableListModel<>();
    } else {
      list.clear();
    }

    list.addAll(Arrays.asList(conditions));

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
    return AdventureDatabase.adventureTable.stream().anyMatch(x -> area.equals(x.name));
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

  private record FistcoreScroll(String zone, int adventureId, String setting) {}

  private static final FistcoreScroll[] FISTCORE_SCROLLS = {
    new FistcoreScroll(
        "The Haiku Dungeon", AdventurePool.HAIKU_DUNGEON, "fistTeachingsHaikuDungeon"),
    new FistcoreScroll("The Poker Room", AdventurePool.POKER_ROOM, "fistTeachingsPokerRoom"),
    new FistcoreScroll("A Barroom Brawl", AdventurePool.BARROOM_BRAWL, "fistTeachingsBarroomBrawl"),
    new FistcoreScroll(
        "The Haunted Conservatory",
        AdventurePool.HAUNTED_CONSERVATORY,
        "fistTeachingsConservatory"),
    new FistcoreScroll(
        "The Bat Hole Entrance", AdventurePool.BAT_HOLE_ENTRYWAY, "fistTeachingsBatHole"),
    new FistcoreScroll("The \"Fun\" House", AdventurePool.FUN_HOUSE, "fistTeachingsFunHouse"),
    new FistcoreScroll(
        "Cobb's Knob Menagerie Level 2", AdventurePool.MENAGERIE_LEVEL_2, "fistTeachingsMenagerie"),
    new FistcoreScroll("Pandamonium Slums", AdventurePool.PANDAMONIUM_SLUMS, "fistTeachingsSlums"),
    new FistcoreScroll("Frat House", AdventurePool.FRAT_HOUSE, "fistTeachingsFratHouse"),
    new FistcoreScroll(
        "Road to the White Citadel", AdventurePool.ROAD_TO_WHITE_CITADEL, "fistTeachingsRoad"),
    new FistcoreScroll(
        "Lair of the Ninja Snowmen", AdventurePool.NINJA_SNOWMEN, "fistTeachingsNinjaSnowmen"),
  };

  private static String fistcoreDataSetting(final FistcoreScroll data) {
    return (data == null) ? null : data.setting;
  }

  private static FistcoreScroll fistcoreLocationToData(final int location) {
    return Arrays.stream(FISTCORE_SCROLLS)
        .filter(Objects::nonNull)
        .filter(s -> location == s.adventureId)
        .findFirst()
        .orElse(null);
  }

  public static String fistcoreLocationToSetting(final int location) {
    return fistcoreDataSetting(fistcoreLocationToData(location));
  }

  public static DifficultyLevel getDifficultyLevel(String adventureName) {
    return AdventureDatabase.diffLevelLookup.getOrDefault(adventureName, DifficultyLevel.UNKNOWN);
  }

  public static Environment getEnvironment(String adventureName) {
    return AdventureDatabase.environmentLookup.getOrDefault(adventureName, Environment.NONE);
  }

  public static int getRecommendedStat(String adventureName) {
    return AdventureDatabase.statLookup.getOrDefault(adventureName, -1);
  }

  public static int getWaterLevel(String adventureName) {
    return AdventureDatabase.waterLevelLookup.getOrDefault(adventureName, -1);
  }

  public static boolean hasWanderers(final String adventureName, final boolean adv) {
    return AdventureDatabase.wandererLookup.getOrDefault(adventureName, adv);
  }

  public static boolean canAdventureWhileOverdrunk(final String adventureName) {
    return AdventureDatabase.overdrunkLookup.getOrDefault(adventureName, false);
  }

  private static class AdventureArray {
    private final SortedMap<String, KoLAdventure> internalList = new TreeMap<>();

    public void add(final KoLAdventure value) {
      this.internalList.put(StringUtilities.getCanonicalName(value.getAdventureName()), value);
    }

    public void addSynonym(final String synonym, final KoLAdventure value) {
      this.internalList.put(StringUtilities.getCanonicalName(synonym), value);
    }

    public KoLAdventure find(String adventureName) {
      var names = internalList.keySet().toArray(new String[0]);
      List<String> matchingNames = StringUtilities.getMatchingNames(names, adventureName);

      if (matchingNames.size() > 1) {
        for (String matchingName : matchingNames) {
          RequestLogger.printLine(matchingName);
        }

        KoLmafia.updateDisplay(MafiaState.ERROR, "Multiple matches against " + adventureName + ".");
        return null;
      }

      if (matchingNames.size() == 1) {
        String match = matchingNames.get(0);
        return this.internalList.get(match);
      }

      return null;
    }

    public void clear() {
      this.internalList.clear();
    }

    public boolean isEmpty() {
      return this.internalList.size() == 0;
    }
  }
}
