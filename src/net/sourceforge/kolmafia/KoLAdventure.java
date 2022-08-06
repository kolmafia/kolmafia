package net.sourceforge.kolmafia;

import java.util.Comparator;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.sourceforge.kolmafia.AscensionPath.Path;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLConstants.ZodiacZone;
import net.sourceforge.kolmafia.combat.Macrofier;
import net.sourceforge.kolmafia.listener.NamedListenerRegistry;
import net.sourceforge.kolmafia.moods.RecoveryManager;
import net.sourceforge.kolmafia.objectpool.AdventurePool;
import net.sourceforge.kolmafia.objectpool.EffectPool;
import net.sourceforge.kolmafia.objectpool.FamiliarPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.objectpool.OutfitPool;
import net.sourceforge.kolmafia.persistence.AdventureDatabase;
import net.sourceforge.kolmafia.persistence.EquipmentDatabase;
import net.sourceforge.kolmafia.persistence.HolidayDatabase;
import net.sourceforge.kolmafia.persistence.MonsterDatabase.Element;
import net.sourceforge.kolmafia.persistence.QuestDatabase;
import net.sourceforge.kolmafia.persistence.QuestDatabase.Quest;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.AdventureRequest;
import net.sourceforge.kolmafia.request.BasementRequest;
import net.sourceforge.kolmafia.request.ClanRumpusRequest;
import net.sourceforge.kolmafia.request.DwarfFactoryRequest;
import net.sourceforge.kolmafia.request.EquipmentRequest;
import net.sourceforge.kolmafia.request.FightRequest;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.request.PlaceRequest;
import net.sourceforge.kolmafia.request.PyramidRequest;
import net.sourceforge.kolmafia.request.QuestLogRequest;
import net.sourceforge.kolmafia.request.RichardRequest;
import net.sourceforge.kolmafia.request.SpelunkyRequest;
import net.sourceforge.kolmafia.request.StandardRequest;
import net.sourceforge.kolmafia.request.TavernRequest;
import net.sourceforge.kolmafia.request.UntinkerRequest;
import net.sourceforge.kolmafia.request.UseItemRequest;
import net.sourceforge.kolmafia.session.BatManager;
import net.sourceforge.kolmafia.session.EncounterManager;
import net.sourceforge.kolmafia.session.EquipmentManager;
import net.sourceforge.kolmafia.session.InventoryManager;
import net.sourceforge.kolmafia.session.IslandManager;
import net.sourceforge.kolmafia.session.Limitmode;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class KoLAdventure implements Comparable<KoLAdventure>, Runnable {
  public static final String[][] DEMON_TYPES = {
    {"Summoning Chamber", "Pies"},
    {"Spooky Forest", "Preternatural Greed"},
    {"Sonofa Beach", "Fit To Be Tide"},
    {"Deep Fat Friars' Gate", "Big Flaming Whip"},
    {"Haunted Bathroom", "Demonic Taint"},
    {null, "pile of smoking rags"},
    {null, "Drinks"},
    {"Nemesis' Lair", "Existential Torment"},
    {"Sinister Ancient Tablet", "Burning, Man"},
    {"Strange Cube", "The Pleasures of the Flesh"},
    {"Battlefield", "Infernal Thirst"},
    {null, "Jacked In"},
    {null, "Yeg's Power"},
  };

  public static final AdventureResult BEATEN_UP = EffectPool.get(EffectPool.BEATEN_UP, 4);
  public static final AdventureResult PERFUME = EffectPool.get(EffectPool.KNOB_GOBLIN_PERFUME, 1);

  public static KoLAdventure lastVisitedLocation = null;
  public static boolean locationLogged = false;
  public static String lastLocationName = null;
  public static String lastLocationURL = null;

  private boolean isValidAdventure = false;
  private boolean hasWanderers = false;
  private final String zone, parentZone, adventureId, formSource, adventureName, environment;
  private final int adventureNumber;
  private final int recommendedStat, waterLevel;
  private final String normalString, lowercaseString, parentZoneDescription;

  private final GenericRequest request;
  private final AreaCombatData areaSummary;
  private final boolean isNonCombatsOnly;

  private static final Pattern ADVENTURE_AGAIN =
      Pattern.compile("<a href=\"([^\"]*)\">Adventure Again \\((.*?)\\)</a>");
  private static final HashSet<String> unknownAdventures = new HashSet<>();

  public static final Comparator<KoLAdventure> NameComparator =
      new Comparator<KoLAdventure>() {
        @Override
        public int compare(KoLAdventure v1, KoLAdventure v2) {
          return v1.adventureName.compareTo(v2.adventureName);
        }
      };

  /**
   * Constructs a new <code>KoLAdventure</code> with the given specifications.
   *
   * @param formSource The form associated with the given adventure
   * @param adventureId The identifier for this adventure, relative to its form
   * @param adventureName The string form, or name of this adventure
   */
  public KoLAdventure(
      final String zone,
      final String formSource,
      final String adventureId,
      final String adventureName) {
    this.formSource = formSource;
    this.adventureId = adventureId;
    this.adventureNumber = this.getSnarfblat();

    this.zone = zone;
    this.adventureName = adventureName;

    this.normalString = this.zone + ": " + this.adventureName;
    this.lowercaseString = this.normalString.toLowerCase();

    this.parentZone = AdventureDatabase.PARENT_ZONES.get(zone);
    this.parentZoneDescription = AdventureDatabase.ZONE_DESCRIPTIONS.get(this.parentZone);

    this.environment = AdventureDatabase.getEnvironment(adventureName);

    this.recommendedStat = AdventureDatabase.getRecommendedStat(adventureName);

    this.waterLevel = AdventureDatabase.getWaterLevel(adventureName);

    this.hasWanderers =
        AdventureDatabase.hasWanderers(adventureName, formSource.equals("adventure.php"));

    switch (formSource) {
      case "dwarffactory.php":
        this.request = new DwarfFactoryRequest("ware");
        break;
      case "clan_gym.php":
        this.request = new ClanRumpusRequest(ClanRumpusRequest.RequestType.fromString(adventureId));
        break;
      case "clan_hobopolis.php":
        this.request = new RichardRequest(StringUtilities.parseInt(adventureId));
        break;
      case "basement.php":
        this.request = new BasementRequest(adventureName);
        break;
      default:
        this.request = new AdventureRequest(adventureName, formSource, adventureId);
        break;
    }

    this.areaSummary = AdventureDatabase.getAreaCombatData(adventureName);

    if (adventureId == null) {
      this.isNonCombatsOnly = false;
    } else if (this.areaSummary != null) {
      this.isNonCombatsOnly =
          this.areaSummary.combats() == 0
              && this.areaSummary.getMonsterCount() == 0
              && this.areaSummary.getSuperlikelyMonsterCount() == 0;
    } else {
      this.isNonCombatsOnly = !(this.request instanceof AdventureRequest);
    }
  }

  public String toLowerCaseString() {
    return this.lowercaseString;
  }

  /** Returns the form source for this adventure. */
  public String getFormSource() {
    return this.formSource;
  }

  /** Returns the name where this zone is found. */
  public String getZone() {
    return this.zone;
  }

  public String getParentZone() {
    return this.parentZone;
  }

  public String getParentZoneDescription() {
    return this.parentZoneDescription;
  }

  public String getEnvironment() {
    return this.environment;
  }

  public int getRecommendedStat() {
    return this.recommendedStat;
  }

  public int getWaterLevel() {
    return this.waterLevel;
  }

  public boolean hasWanderers() {
    return this.hasWanderers;
  }

  /** Returns the name of this adventure. */
  public String getAdventureName() {
    return this.adventureName;
  }

  public String getPrettyAdventureName(final String urlString) {
    if (urlString.contains("pyramid_state")) {
      return PyramidRequest.getPyramidLocationString(urlString);
    }
    if (urlString.startsWith("basement.php")) {
      return BasementRequest.getBasementLevelName();
    }
    if (urlString.startsWith("cellar.php")) {
      return TavernRequest.cellarLocationString(urlString);
    }
    // *** could do something with barrel.php here
    return this.adventureName;
  }

  // In chamber <b>#1</b> of the Daily Dungeon, you encounter ...
  // In the <b>5th</b> chamber of the Daily Dungeon, you encounter ...
  private static final Pattern DAILY_DUNGEON_CHAMBER = Pattern.compile("<b>#?([\\d]+)(?:th)?</b>");

  public static String getPrettyAdventureName(
      final String locationName, final String responseText) {
    if (locationName.equals("The Daily Dungeon")) {
      // Parse room number from responseText
      Matcher matcher = KoLAdventure.DAILY_DUNGEON_CHAMBER.matcher(responseText);
      if (matcher.find()) {
        String room = matcher.group(1);
        return locationName + " (Room " + room + ")";
      }
    }
    return locationName;
  }

  /**
   * Returns the adventure Id for this adventure.
   *
   * @return The adventure Id for this adventure
   */
  public String getAdventureId() {
    return this.adventureId;
  }

  public int getAdventureNumber() {
    return this.adventureNumber;
  }

  private int getSnarfblat() {
    if (!this.getFormSource().equals("adventure.php")) {
      return -1;
    }

    try {
      return Integer.parseInt(this.adventureId);
    } catch (NumberFormatException e) {
      return -1;
    }
  }

  public AreaCombatData getAreaSummary() {
    return this.areaSummary;
  }

  public boolean isNonCombatsOnly() {
    return this.isNonCombatsOnly
        && !KoLAdventure.hasWanderingMonsters(this.formSource, this.adventureId);
  }

  public static final boolean hasWanderingMonsters(
      final String urlString, final String adventureId) {
    if (!urlString.startsWith("adventure.php")) {
      return false;
    }

    int advId = Integer.parseInt(adventureId);
    switch (advId) {
      case AdventurePool.THE_SHORE:
      case AdventurePool.TRAINING_SNOWMAN:
      case AdventurePool.DIRE_WARREN:
      case AdventurePool.GINGERBREAD_CIVIC_CENTER:
      case AdventurePool.GINGERBREAD_TRAIN_STATION:
      case AdventurePool.GINGERBREAD_INDUSTRIAL_ZONE:
      case AdventurePool.GINGERBREAD_RETAIL_DISTRICT:
      case AdventurePool.GINGERBREAD_SEWERS:
      case AdventurePool.THE_DRIPPING_TREES:
        return false;
    }

    // Romantic targets.

    String romanticTarget = Preferences.getString("romanticTarget");

    if (romanticTarget != null && !romanticTarget.isEmpty()) {
      return true;
    }

    // Holidays.

    String holiday = HolidayDatabase.getHoliday();

    if (holiday.contains("El Dia De Los Muertos Borrachos")
        || holiday.contains("Feast of Boris")
        || holiday.contains("Talk Like a Pirate Day")) {
      return true;
    }

    // Nemesis assassins.

    if (!InventoryManager.hasItem(ItemPool.VOLCANO_MAP)
        && QuestDatabase.isQuestLaterThan(Quest.NEMESIS, "step16")) {
      return true;
    }

    // Beecore.

    if (KoLCharacter.inBeecore()) {
      return true;
    }

    // Mini-Hipster and Artistic Goth Kid

    FamiliarData familiar = KoLCharacter.getFamiliar();
    if (familiar != null
        && (familiar.getId() == FamiliarPool.HIPSTER
            || familiar.getId() == FamiliarPool.ARTISTIC_GOTH_KID)
        && Preferences.getInteger("_hipsterAdv") < 7) {
      return true;
    }

    return false;
  }

  /**
   * Returns the request associated with this adventure.
   *
   * @return The request for this adventure
   */
  public GenericRequest getRequest() {
    return this.request;
  }

  public void overrideAdventuresUsed(int used) {
    if (this.request instanceof AdventureRequest) {
      ((AdventureRequest) this.request).overrideAdventuresUsed(used);
    }
  }

  // Validation part 1:
  //
  // Determine if you are locked out of reaching a zone or location by level,
  // quest progress quest items, or other things that a betweenBattleScript
  // can't solve for you, like using a daypass, donning an outfit, or getting
  // an effect. After we've given that script a chance to run, part 2 of
  // validation might be able to do some of those things anyway.
  //
  // this.isValidAdventure will be false if there is nothing you can do
  // to go to this location at this time, or true, otherwise

  private void validate1() {
    this.isValidAdventure = this.isCurrentlyAccessible();
  }

  public boolean isCurrentlyAccessible() {
    if (Limitmode.limitAdventure(this)) {
      return false;
    }

    // There are lots of zones from past events (like Crimbos) that are no
    // longer available. AdventureDatabase maintains a handy Set of all such,
    // so it is quick and easy and inexpensive to just eliminate them first.
    if (AdventureDatabase.removedAdventure(this)) {
      return false;
    }

    // Some zones are restricted to a specific Ascension Path.
    Path path = AdventureDatabase.zoneAscensionPath(this.zone);
    if (path != null && path != KoLCharacter.getPath()) {
      return false;
    }
    // Further validation of individual zones happens below.

    // First check non-adventure.php zones.

    // Level 3 quest
    if (this.formSource.equals("cellar.php")) {
      // *** Validate Tavern Quest
      return true;
    }

    // Level 5 quest boss
    if (this.formSource.equals("cobbsknob.php")) {
      return QuestDatabase.isQuestLaterThan(Quest.GOBLIN, QuestDatabase.STARTED)
          && !QuestDatabase.isQuestFinished(Quest.GOBLIN);
    }

    // Level 7 quest boss
    if (this.formSource.equals("cyrpt.php")) {
      // *** Validate Bonerdagon
      return true;
    }

    // Level 8 quest boss
    if (this.adventureId.equals(AdventurePool.SHROUDED_PEAK_ID)) {
      String trapper = QuestDatabase.getQuest(Quest.TRAPPER);
      if (trapper.equals(QuestDatabase.FINISHED)) {
        return false;
      }
      return trapper.equals("step3")
          || trapper.equals("step4")
          || Preferences.getString("peteMotorbikeTires").equals("Snow Tires");
    }

    // Level 11 quest boss -> Lord Spookyraven
    if (this.adventureId.equals(AdventurePool.SUMMONING_CHAMBER_ID)) {
      return QuestDatabase.isQuestLaterThan(Quest.MANOR, "step2");
    }

    // Level 11 quest boss -> Ed
    if (this.adventureId.equals(AdventurePool.LOWER_CHAMBER_ID)) {
      return Preferences.getBoolean("lowerChamberUnlock");
    }

    // Fernswarthy's Basement
    if (this.formSource.equals("basement.php")) {
      // *** Validate Fernswarthy's Basement
      return true;
    }

    /* Removed adventures.
    if (this.adventureId.equals(AdventurePool.ELDRITCH_FISSURE_ID)) {
      return Preferences.getBoolean("eldritchFissureAvailable");
    }

    if (this.adventureId.equals(AdventurePool.ELDRITCH_HORROR_ID)) {
      return Preferences.getBoolean("eldritchHorrorAvailable");
    }
    */

    // Only look at adventure.php locations below this.
    // Further validation for other adventures happens in part2
    if (!this.formSource.startsWith("adventure.php")) {
      return true;
    }

    // Level 2 quest
    // Level 3 quest
    if (this.zone.equals("Woods")) {
      // *** Validate
    }

    // Level 4 quest
    if (this.zone.equals("BatHole")) {
      String progress = QuestDatabase.getQuest(Quest.BAT);
      if (progress.equals(QuestDatabase.UNSTARTED)) {
        return false;
      }
      switch (this.adventureNumber) {
        case AdventurePool.BAT_HOLE_ENTRYWAY:
        case AdventurePool.GUANO_JUNCTION:
          return true;
        case AdventurePool.BATRAT:
        case AdventurePool.BEANBAT:
        case AdventurePool.BOSSBAT:
          {
            int sonarsUsed =
                switch (progress) {
                  case QuestDatabase.STARTED -> 0;
                  case "step1" -> 1;
                  case "step2" -> 2;
                  default -> 3;
                };

            int sonarsForLocation =
                switch (this.adventureNumber) {
                  case AdventurePool.BATRAT -> 1;
                  case AdventurePool.BEANBAT -> 2;
                  default -> 3;
                };

            if (sonarsUsed >= sonarsForLocation) {
              return true;
            }

            int sonarsToUse = sonarsForLocation - sonarsUsed;

            return InventoryManager.hasItem(ItemPool.get(ItemPool.SONAR, sonarsToUse));
          }
      }
    }

    // Level 5 quest
    if (this.zone.equals("Knob")) {
      // *** Validate
      return switch (this.adventureNumber) {
        case AdventurePool.OUTSKIRTS_OF_THE_KNOB -> true;
          // *** Knob opened
        case AdventurePool.COBB_BARRACKS,
            AdventurePool.COBB_KITCHEN,
            AdventurePool.COBB_HAREM,
            AdventurePool.COBB_TREASURY -> true;
        default -> false;
      };
    }

    if (this.zone.equals("Lab")) {
      return InventoryManager.hasItem(ItemPool.get(ItemPool.LAB_KEY, 1));
    }

    if (this.zone.equals("Menagerie")) {
      return InventoryManager.hasItem(ItemPool.get(ItemPool.MENAGERIE_KEY, 1));
    }

    // Level 6 quest
    if (this.zone.equals("Friars")) {
      return switch (this.adventureNumber) {
          // Quest.FRIAR started but not finished
        case AdventurePool.DARK_ELBOW_OF_THE_WOODS,
            AdventurePool.DARK_HEART_OF_THE_WOODS,
            AdventurePool.DARK_NECK_OF_THE_WOODS -> QuestDatabase.isQuestStarted(Quest.FRIAR)
            && !QuestDatabase.isQuestFinished(Quest.FRIAR);
          // Ed the Undying only
        case AdventurePool.PANDAMONIUM -> KoLCharacter.isEd();
        default -> false;
      };
    }

    // The Pandamonium zones are available if you have completed the Friars quest
    if (this.zone.equals("Pandamonium")) {
      return QuestDatabase.isQuestFinished(Quest.FRIAR);
    }

    // Level 7 quest
    if (this.zone.equals("Cyrpt")) {
      if (!QuestDatabase.isQuestStarted(Quest.CYRPT)) {
        return false;
      }
      return switch (this.adventureNumber) {
        case AdventurePool.DEFILED_ALCOVE -> Preferences.getInteger("cyrptAlcoveEvilness") > 0;
        case AdventurePool.DEFILED_CRANNY -> Preferences.getInteger("cyrptCrannyEvilness") > 0;
        case AdventurePool.DEFILED_NICHE -> Preferences.getInteger("cyrptNicheEvilness") > 0;
        case AdventurePool.DEFILED_NOOK -> Preferences.getInteger("cyrptNookEvilness") > 0;
        default -> false;
      };
    }

    // The VERY Unquiet Garves are available if you have completed the Cyrpt quest
    if (this.adventureNumber == AdventurePool.VERY_UNQUIET_GARVES) {
      return QuestDatabase.isQuestFinished(Quest.CYRPT);
    }

    // Level 8 quest
    if (this.zone.equals("McLarge")) {
      return switch (this.adventureNumber) {
        case AdventurePool.ITZNOTYERZITZ_MINE, AdventurePool.GOATLET -> QuestDatabase
            .isQuestLaterThan(Quest.TRAPPER, QuestDatabase.STARTED);
        case AdventurePool.NINJA_SNOWMEN, AdventurePool.EXTREME_SLOPE -> QuestDatabase
            .isQuestLaterThan(Quest.TRAPPER, "step1");
        case AdventurePool.ICY_PEAK -> QuestDatabase.isQuestLaterThan(Quest.TRAPPER, "step4");
        case AdventurePool.MINE_OFFICE -> QuestDatabase.isQuestStarted(Quest.FACTORY);
        default -> false;
      };
    }

    // Level 9 quest
    if (this.zone.equals("Highlands")) {
      return QuestDatabase.isQuestLaterThan(Quest.TOPPING, QuestDatabase.STARTED);
    }

    // Level 10 quest
    if (this.zone.equals("Beanstalk")) {
      switch (this.adventureNumber) {
        case AdventurePool.AIRSHIP:
          // The beanstalk is unlocked when the player has planted a
          // beanstalk -- but, the bean needs to be planted first.
          // We will plant in validate2, if necessary
          return !KoLCharacter.isKingdomOfExploathing() && KoLCharacter.getLevel() >= 10;
        case AdventurePool.CASTLE_BASEMENT:
          // The Castle Basement is unlocked provided the player has the S.O.C.K
          // (legacy: rowboats give access but are no longer creatable)
          return InventoryManager.hasItem(ItemPool.get(ItemPool.SOCK, 1))
              || InventoryManager.hasItem(ItemPool.get(ItemPool.ROWBOAT, 1))
              || KoLCharacter.isKingdomOfExploathing();
        case AdventurePool.CASTLE_GROUND:
          return Preferences.getInteger("lastCastleGroundUnlock") == KoLCharacter.getAscensions();
        case AdventurePool.CASTLE_TOP:
          return Preferences.getInteger("lastCastleTopUnlock") == KoLCharacter.getAscensions();
        case AdventurePool.HOLE_IN_THE_SKY:
          // The Hole in the Sky is unlocked provided the player has a steam-powered rocketship
          // (legacy: rowboats give access but are no longer creatable)
          return KoLCharacter.isKingdomOfExploathing()
              || InventoryManager.hasItem(ItemPool.get(ItemPool.ROCKETSHIP, 1))
              || InventoryManager.hasItem(ItemPool.get(ItemPool.ROWBOAT, 1));
      }
    }

    // Level 11 quest

    if (this.adventureNumber == AdventurePool.BLACK_FOREST) {
      return QuestDatabase.isQuestStarted(Quest.MACGUFFIN);
    }

    // *** Lord Spookyraven

    if (this.zone.equals("Manor0")) {
      return QuestDatabase.isQuestStarted(Quest.MANOR);
    }

    // *** Doctor Awkward

    if (this.adventureNumber == AdventurePool.COPPERHEAD_CLUB) {
      return QuestDatabase.isQuestStarted(Quest.SHEN);
    }

    if (this.zone.equals("The Red Zeppelin's Mooring")) {
      return switch (this.adventureNumber) {
        case AdventurePool.ZEPPELIN_PROTESTORS -> QuestDatabase.isQuestStarted(Quest.RON);
        case AdventurePool.RED_ZEPPELIN -> QuestDatabase.isQuestLaterThan(Quest.RON, "step1");
        default -> false;
      };
    }

    if (this.adventureNumber == AdventurePool.PALINDOME) {
      AdventureResult talisman = ItemPool.get(ItemPool.TALISMAN, 1);
      return KoLCharacter.hasEquipped(talisman) || InventoryManager.hasItem(talisman);
    }

    if (this.adventureNumber == AdventurePool.WHITEYS_GROVE) {
      return QuestDatabase.isQuestStarted(Quest.CITADEL)
          || QuestDatabase.isQuestLaterThan(Quest.PALINDOME, "step2")
          || KoLCharacter.isEd();
    }

    // *** Protector Spectre

    if (this.adventureNumber == AdventurePool.HIDDEN_TEMPLE) {
      return KoLCharacter.isKingdomOfExploathing() || KoLCharacter.getTempleUnlocked();
    }

    if (this.zone.equals("HiddenCity")) {
      if (!QuestDatabase.isQuestLaterThan(Quest.WORSHIP, "step2")) {
        return false;
      }
      return switch (this.adventureNumber) {
        case AdventurePool.HIDDEN_PARK -> true;
        case AdventurePool.NW_SHRINE -> true;
        case AdventurePool.SW_SHRINE -> true;
        case AdventurePool.NE_SHRINE -> true;
        case AdventurePool.SE_SHRINE -> true;
        case AdventurePool.ZIGGURAT -> true;
        case AdventurePool.HIDDEN_APARTMENT -> QuestDatabase.isQuestStarted(Quest.CURSES);
        case AdventurePool.HIDDEN_HOSPITAL -> QuestDatabase.isQuestStarted(Quest.DOCTOR);
        case AdventurePool.HIDDEN_OFFICE -> QuestDatabase.isQuestStarted(Quest.BUSINESS);
        case AdventurePool.HIDDEN_BOWLING_ALLEY -> QuestDatabase.isQuestStarted(Quest.SPARE);
        default -> false;
      };
    }

    // *** Ed the Undying

    if (this.zone.equals("Beach")) {
      if (!KoLCharacter.desertBeachAccessible()) {
        return false;
      }
      switch (this.adventureNumber) {
        case AdventurePool.THE_SHORE:
        case AdventurePool.SOUTH_OF_THE_BORDER:
          return true;
        case AdventurePool.ARID_DESERT:
          // Open after diary read
          return QuestDatabase.isQuestStarted(Quest.DESERT);
        case AdventurePool.OASIS:
          if (!QuestDatabase.isQuestStarted(Quest.DESERT)) {
            return false;
          }
          // Open after 1 desert exploration
          if (Preferences.getInteger("desertExploration") > 0) {
            return true;
          }
          // Legacy: what about accounts who did desert before we tracked it?
          return QuestDatabase.isQuestFinished(Quest.DESERT);
        case AdventurePool.KOKOMO_RESORT:
          // Open with "Tropical Contact High"
          return KoLConstants.activeEffects.contains(
              EffectPool.get(EffectPool.TROPICAL_CONTACT_HIGH));
      }
      return false;
    }

    if (this.zone.equals("Pyramid")) {
      if (!QuestDatabase.isQuestStarted(Quest.PYRAMID)) {
        return false;
      }
      return switch (this.adventureNumber) {
        case AdventurePool.UPPER_CHAMBER -> true;
        case AdventurePool.MIDDLE_CHAMBER -> Preferences.getBoolean("middleChamberUnlock");
        default -> false;
      };
    }

    // Level 12 quest

    // The Mysterious Island on the Verge of War (small island) is a bit odd.
    //
    // When you look at it, the hippy camp and frat house have the usual URLs.
    // You can adventure in either using those URLs, either disguised or not.
    //
    // If you are disguised as an enemy (either normal or wartime outfits), you
    // get combat encounters. The "Next Adventure" links at the end of the
    // battle and the "Last Adventure" link in the charpane will will be to the
    // wartime (Disguised) URL
    //
    // If you are either undisguised or disguised as a friend (either normal or
    // wartime outfits), you get non-combat encounters. The "Next Adventure"
    // links at the end of the battle and the "Last Adventure" link in the
    // charpane will will be to the Wartime not-disguised URL
    //
    // To adventure in the frat house, all of the following work, disguised or not:
    //
    // AdventurePool.FRAT_HOUSE:
    // AdventurePool.WARTIME_FRAT_HOUSE:
    // AdventurePool.WARTIME_FRAT_HOUSE_DISGUISED:
    //
    // To adventure in the hippy camp, all of the following work, disguised or not:
    //
    // AdventurePool.HIPPY_CAMP:
    // AdventurePool.WARTIME_HIPPY_CAMP:
    // AdventurePool.WARTIME_HIPPY_CAMP_DISGUISED:

    if (this.zone.equals("IsleWar")) {
      if (!KoLCharacter.mysteriousIslandAccessible()
          || !QuestDatabase.isQuestStarted(Quest.ISLAND_WAR)) {
        return false;
      }

      // Quest.ISLAND_WAR progresses from "unstarted" -> "started" -> "step1" -> "finished"
      // "unstarted" is the peaceful Mysterious Island
      // "started" is the Verge of War on the Mysterious Island
      // "step1" is the actual war on the Big Island
      // "finished" is the peaceful Big Island

      switch (this.adventureNumber) {
        case AdventurePool.WARTIME_FRAT_HOUSE:
        case AdventurePool.WARTIME_FRAT_HOUSE_DISGUISED:
        case AdventurePool.WARTIME_HIPPY_CAMP:
        case AdventurePool.WARTIME_HIPPY_CAMP_DISGUISED:
          return QuestDatabase.isQuestBefore(Quest.ISLAND_WAR, "step1");

        case AdventurePool.FRAT_UNIFORM_BATTLEFIELD:
        case AdventurePool.HIPPY_UNIFORM_BATTLEFIELD:
          return QuestDatabase.isQuestStep(Quest.ISLAND_WAR, "step1");

        case AdventurePool.THEMTHAR_HILLS:
          // Only available during the war. After the war, you can visit the Nunnery.
          return QuestDatabase.isQuestStep(Quest.ISLAND_WAR, "step1");
      }
      return false;
    }

    if (this.zone.equals("Farm")) {
      if (!QuestDatabase.isQuestStep(Quest.ISLAND_WAR, "step1")) {
        return false;
      }
      // Farm	adventure=137	McMillicancuddy's Barn
      // Farm	adventure=141	McMillicancuddy's Pond
      // Farm	adventure=142	McMillicancuddy's Back 40
      // Farm	adventure=143	McMillicancuddy's Other Back 40
      // Farm	adventure=144	McMillicancuddy's Granary
      // Farm	adventure=145	McMillicancuddy's Bog
      // Farm	adventure=146	McMillicancuddy's Family Plot
      // Farm	adventure=147	McMillicancuddy's Shady Thicket
      // *** validate
      return true;
    }

    if (this.zone.equals("Orchard")) {
      if (!QuestDatabase.isQuestStep(Quest.ISLAND_WAR, "step1")) {
        return false;
      }
      // Orchard	adventure=127	The Hatching Chamber
      // Orchard	adventure=128	The Feeding Chamber
      // Orchard	adventure=129	The Royal Guard Chamber
      // Orchard	adventure=130	The Filthworm Queen's Chamber
      // *** validate
      return true;
    }

    if (this.zone.equals("Junkyard")) {
      if (!QuestDatabase.isQuestStep(Quest.ISLAND_WAR, "step1")) {
        return false;
      }
      // Junkyard	adventure=182	Next to that Barrel with Something Burning in it
      // Junkyard	adventure=183	Near an Abandoned Refrigerator
      // Junkyard	adventure=184	Over Where the Old Tires Are
      // Junkyard	adventure=185	Out by that Rusted-Out Car
      // *** validate
      return true;
    }

    if (this.adventureNumber == AdventurePool.SONOFA_BEACH) {
      // Sonofa Beach is available during the war as a sidequest and also
      // after the war, whether or not it was used as such.
      return QuestDatabase.isQuestLaterThan(Quest.ISLAND_WAR, QuestDatabase.STARTED);
    }

    // Island	adventure=136	Env: outdoor Stat: 170	Sonofa Beach
    // Island	adventure=154	Env: outdoor Stat: 170	Post-War Junkyard
    // Island	adventure=155	Env: outdoor Stat: 170	McMillicancuddy's Farm

    // Spookyraven Manor quests:
    //
    // Quest.SPOOKYRAVEN_NECKLACE	Lady Spookyraven
    // Quest.SPOOKYRAVEN_DANCE		Lady Spookyraven
    // Quest.SPOOKYRAVEN_BABIES		Lady Spookyraven

    if (this.zone.equals("Manor1")) {
      switch (this.adventureNumber) {
        case AdventurePool.HAUNTED_KITCHEN:
        case AdventurePool.HAUNTED_CONSERVATORY:
          return QuestDatabase.isQuestStarted(Quest.SPOOKYRAVEN_NECKLACE);
        case AdventurePool.HAUNTED_LIBRARY:
          return InventoryManager.hasItem(ItemPool.LIBRARY_KEY);
        case AdventurePool.HAUNTED_BILLIARDS_ROOM:
          return InventoryManager.hasItem(ItemPool.BILLIARDS_KEY);
      }
      return true;
    }

    if (this.zone.equals("Manor2")) {
      switch (this.adventureNumber) {
        case AdventurePool.HAUNTED_BATHROOM:
        case AdventurePool.HAUNTED_BEDROOM:
        case AdventurePool.HAUNTED_GALLERY:
          return QuestDatabase.isQuestLaterThan(Quest.SPOOKYRAVEN_DANCE, QuestDatabase.STARTED);
        case AdventurePool.HAUNTED_BALLROOM:
          return QuestDatabase.isQuestLaterThan(Quest.SPOOKYRAVEN_DANCE, "step2");
      }
      return true;
    }

    if (this.zone.equals("Manor3")) {
      switch (this.adventureNumber) {
        case AdventurePool.HAUNTED_LABORATORY:
        case AdventurePool.HAUNTED_NURSERY:
        case AdventurePool.HAUNTED_STORAGE_ROOM:
          return QuestDatabase.isQuestLaterThan(Quest.SPOOKYRAVEN_DANCE, "step3");
      }
      return true;
    }

    // Nemesis Quest
    // ***

    // Plains	adventure=20	The "Fun" House
    // Nemesis Cave	adventure=452	The Fungal Nethers
    // Volcano	adventure=214	The Broodling Grounds
    // Volcano	adventure=215	The Outer Compound
    // Volcano	adventure=217	The Temple Portico
    // Volcano	adventure=218	Convention Hall Lobby
    // Volcano	adventure=219	Outside the Club
    // Volcano	adventure=220	The Island Barracks
    // Volcano	adventure=221	The Nemesis' Lair

    // The Temporal Rift zones have multiple requirements
    if (this.zone.equals("Rift")) {
      boolean ascended = KoLCharacter.getAscensions() > 0;
      int level = KoLCharacter.getLevel();
      boolean keyed = QuestDatabase.isQuestLaterThan(Quest.EGO, QuestDatabase.STARTED);
      return ascended && level >= 4 && level <= 5 && keyed;
    }

    if (this.zone.equals("Degrassi Knoll")) {
      return KoLCharacter.getSignZone() != ZodiacZone.KNOLL;
    }

    if (this.zone.equals("MusSign")) {
      if (KoLCharacter.getSignZone() != ZodiacZone.KNOLL) {
        return false;
      }

      if (this.adventureNumber == AdventurePool.BUGBEAR_PEN) {
        return QuestDatabase.isQuestStarted(Quest.BUGBEAR)
            && !QuestDatabase.isQuestFinished(Quest.BUGBEAR);
      }

      if (this.adventureNumber == AdventurePool.SPOOKY_GRAVY_BURROW) {
        return QuestDatabase.isQuestLaterThan(Quest.BUGBEAR, "step1");
      }

      if (this.adventureNumber == AdventurePool.POST_QUEST_BUGBEAR_PEN) {
        return QuestDatabase.isQuestFinished(Quest.BUGBEAR);
      }
    }

    if (this.zone.equals("Little Canadia")) {
      return KoLCharacter.getSignZone() == ZodiacZone.CANADIA;
    }

    if (this.zone.equals("Le Marais D&egrave;gueulasse")) {
      // This is in Little Canadia
      if (!QuestDatabase.isQuestStarted(Quest.SWAMP)) {
        return false;
      }
      return switch (this.adventureNumber) {
        case AdventurePool.EDGE_OF_THE_SWAMP -> true;
        case AdventurePool.DARK_AND_SPOOKY_SWAMP -> Preferences.getBoolean("maraisDarkUnlock");
        case AdventurePool.CORPSE_BOG -> Preferences.getBoolean("maraisCorpseUnlock");
        case AdventurePool.RUINED_WIZARDS_TOWER -> Preferences.getBoolean("maraisWizardUnlock");
        case AdventurePool.WILDLIFE_SANCTUARRRRRGH -> Preferences.getBoolean(
            "maraisWildlifeUnlock");
        case AdventurePool.WEIRD_SWAMP_VILLAGE -> Preferences.getBoolean("maraisVillageUnlock");
        case AdventurePool.SWAMP_BEAVER_TERRITORY -> Preferences.getBoolean("maraisBeaverUnlock");
        default -> true;
      };
    }

    if (this.zone.equals("MoxSign")) {
      return KoLCharacter.getSignZone() == ZodiacZone.GNOMADS;
    }

    if (this.zone.equals("Island")) {
      // There are several ways to get to the island.
      if (!KoLCharacter.mysteriousIslandAccessible()) {
        // validate2 will use dingy planks + dinghy plans if you have them
        if (!InventoryManager.hasItem(ItemPool.DINGHY_PLANS)
            || !InventoryManager.hasItem(ItemPool.DINGY_PLANKS)) {
          return false;
        }
      }

      // We have a way to get to the island.  Access to individual zones
      // depends on quest state and outfits

      String winner = IslandManager.warWinner();
      // neither, hippies, fratboys

      switch (this.adventureNumber) {
        case AdventurePool.PIRATE_COVE:
          // You cannot visit the pirates during the war
          return !QuestDatabase.isQuestStep(Quest.ISLAND_WAR, "step1");

        case AdventurePool.HIPPY_CAMP:
        case AdventurePool.HIPPY_CAMP_DISGUISED:
          // You can visit the hippy camp before or after the war, unless it
          // has been bombed into the stone age.
          return QuestDatabase.isQuestBefore(Quest.ISLAND_WAR, "step1")
              || (QuestDatabase.isQuestFinished(Quest.ISLAND_WAR) && winner.equals("hippies"));

        case AdventurePool.FRAT_HOUSE:
        case AdventurePool.FRAT_HOUSE_DISGUISED:
          // You can visit the frat house before or after the war, unless it
          // has been bombed into the stone age.
          return QuestDatabase.isQuestBefore(Quest.ISLAND_WAR, "step1")
              || (QuestDatabase.isQuestFinished(Quest.ISLAND_WAR) && winner.equals("fratboys"));

        case AdventurePool.BOMBED_HIPPY_CAMP:
          return QuestDatabase.isQuestFinished(Quest.ISLAND_WAR)
              && (winner.equals("neither") || winner.equals("fratboys"));

        case AdventurePool.BOMBED_FRAT_HOUSE:
          return QuestDatabase.isQuestFinished(Quest.ISLAND_WAR)
              && (winner.equals("neither") || winner.equals("hippies"));

        case AdventurePool.THE_JUNKYARD:
        case AdventurePool.MCMILLICANCUDDYS_FARM:
          return QuestDatabase.isQuestFinished(Quest.ISLAND_WAR);
      }

      return false;
    }

    // Pirate Ship
    if (this.zone.equals("Pirate")) {
      // There are several ways to get to the island.
      // However, the pirates are unavailable during the war.
      if (!KoLCharacter.mysteriousIslandAccessible()
          || QuestDatabase.isQuestStep(Quest.ISLAND_WAR, "step1")) {
        return false;
      }

      boolean haveOutfit = EquipmentManager.hasOutfit(OutfitPool.SWASHBUCKLING_GETUP);
      boolean haveFledges = InventoryManager.hasItem(ItemPool.PIRATE_FLEDGES);
      if (!haveOutfit && !haveFledges) {
        return false;
      }
      // *** Distinguish between areas
      return switch (this.adventureNumber) {
        case AdventurePool.BARRRNEYS_BARRR -> true;
        case AdventurePool.FCLE -> true;
        case AdventurePool.POOP_DECK -> true;
        case AdventurePool.BELOWDECKS -> true;
        default -> false;
      };
    }

    if (this.zone.equals("Dungeon")) {
      return switch (this.adventureNumber) {
          // The Enormous Greater-Than Sign is available if your base
          // mainstate is at least 45 and you have not yet unlocked
          // the Dungeon of Doom
        case AdventurePool.GREATER_THAN_SIGN -> (KoLCharacter.getBaseMainstat() >= 45)
            && !QuestLogRequest.isDungeonOfDoomAvailable();
          // The Dungeons of Doom are only available if you've finished the quest
        case AdventurePool.DUNGEON_OF_DOOM -> QuestLogRequest.isDungeonOfDoomAvailable();
        default -> true;
      };
    }

    // The Valley of Rof L'm Fao is available if you have completed the Highlands quest
    if (this.adventureNumber == AdventurePool.VALLEY_OF_ROF_LM_FAO) {
      return QuestDatabase.isQuestFinished(Quest.TOPPING);
    }

    if (this.adventureNumber == AdventurePool.TOWER_RUINS) {
      if (QuestDatabase.getQuest(Quest.EGO).equals("step2")) {
        // We've received Fernswarthy's key but have not yet ventured into the
        // ruins of Fernswarthy's Tower. Take a look.
        GenericRequest request = new GenericRequest("fernruin.php");
        RequestThread.postRequest(request);
      }
      return QuestDatabase.isQuestLaterThan(Quest.EGO, "step2");
    }

    if (this.zone.equals("The Drip")) {
      return switch (this.adventureNumber) {
        case AdventurePool.THE_DRIPPING_HALL -> Preferences.getBoolean("drippingHallUnlocked");
        default -> true;
      };
    }

    // Item-generated zones. They all come from IOTMs and therefore may be
    // affected by Standard restrictions.
    // *** Validate

    if (this.zone.equals("The Sea")) {
      // *** Validate
      // The Sea	adventure=186	The Briny Deeps
      // The Sea	adventure=187	The Brinier Deepers
      // The Sea	adventure=189	The Briniest Deepests
      // The Sea	adventure=190	An Octopus's Garden
      // The Sea	adventure=191	The Wreck of the Edgar Fitzsimmons
      // The Sea	adventure=194	Madness Reef
      // The Sea	adventure=198	The Mer-Kin Outpost
      // The Sea	adventure=188	The Skate Park
      // The Sea	adventure=195	The Marinara Trench
      // The Sea	adventure=196	Anemone Mine
      // The Sea	adventure=197	The Dive Bar
      // The Sea	adventure=199	The Coral Corral
      // The Sea	adventure=207	Mer-kin Elementary School
      // The Sea	adventure=208	Mer-kin Library
      // The Sea	adventure=209	Mer-kin Gymnasium
      // The Sea	adventure=210	Mer-kin Colosseum
      // The Sea	adventure=337	The Caliginous Abyss
      // The Sea	mining=3	Anemone Mine (Mining)
    }

    // The following zones depend on your clan, your permissions, and the
    // current state of clan dungeons. We're not going to touch them here.
    if (zone.equals("Clan Basement") || zone.equals("Hobopolis") || zone.equals("Dreadsylvania")) {
      return true;
    }

    // Some adventuring areas are available via items.
    AdventureResult item = AdventureDatabase.zoneGeneratingItem(this.zone);
    if (item == null) {
      // If it is not from an item, assume it is fine
      return true;
    }

    // It is from an item. Standard restrictions probably apply.
    if (KoLCharacter.getRestricted() && !StandardRequest.isAllowed("Items", item.getName())) {
      return false;
    }

    // Item is not restricted.
    // *** Validate

    if (this.zone.equals("Dungeon:Video Game")) {
      // If you have a GameInformPowerDailyPro walkthru in inventory, you
      // have (or had) access to The GameInformPowerDailyPro Dungeon.
      // *** Do we track your progress?
      return InventoryManager.hasItem(ItemPool.get(ItemPool.GAMEPRO_WALKTHRU));
    }

    return true;
  }

  // Validation part 2:
  //
  // The zone/location is within reach. If the pre-adventure script
  // donned outfits, bought supplies, or gained effects, cool.
  //
  // If it didn't do what we can here.
  //
  // If we can't, log error and set this.isValidAdventure to false.

  private void validate2() {
    this.isValidAdventure = this.prepareToAdventure();
  }

  public boolean prepareToAdventure() {
    // If we get here, this.isValidAdventure is true.

    if (this.zone.equals("Astral")) {
      // To take a trip to the Astral Plane, you either need
      // to be Half-Astral or have access to an astral mushroom.
      // The betweenBattleScript could have arranged that.

      AdventureResult effect = EffectPool.get(EffectPool.HALF_ASTRAL);
      AdventureResult mushroom = ItemPool.get(ItemPool.ASTRAL_MUSHROOM, 1);

      if (!KoLConstants.activeEffects.contains(effect) && !InventoryManager.hasItem(mushroom)) {
        return false;
      }

      String option =
          switch (this.adventureNumber) {
            case AdventurePool.BAD_TRIP -> "1";
            case AdventurePool.MEDIOCRE_TRIP -> "2";
            case AdventurePool.GREAT_TRIP -> "3";
            default -> null;
          };
      if (option == null) {
        // This should not happen
        return false;
      }
      Preferences.setString("choiceAdventure71", option);

      // To take a trip to the Astral Plane, you either need
      // to be Half-Astral or have access to an astral mushroom.

      if (KoLConstants.activeEffects.contains(effect)) {
        return true;
      }

      if (!InventoryManager.retrieveItem(mushroom)) {
        // This shouldn't fail.
        return false;
      }

      RequestThread.postRequest(UseItemRequest.getInstance(mushroom));

      // This shouldn't fail.
      return KoLConstants.activeEffects.contains(effect);
    }

    // Fighting the Goblin King requires effects
    if (this.formSource.equals("cobbsknob.php")) {
      if (EquipmentManager.isWearingOutfit(OutfitPool.HAREM_OUTFIT)) {
        // Harem girl
        if (!KoLConstants.activeEffects.contains(KoLAdventure.PERFUME)
            && !KoLCharacter.inBeecore()
            && InventoryManager.retrieveItem(ItemPool.KNOB_GOBLIN_PERFUME)) {
          RequestThread.postRequest(UseItemRequest.getInstance(ItemPool.KNOB_GOBLIN_PERFUME));
        }
        return KoLConstants.activeEffects.contains(KoLAdventure.PERFUME);
      }

      if (EquipmentManager.isWearingOutfit(OutfitPool.KNOB_ELITE_OUTFIT)) {
        // Elite Guard
        return InventoryManager.retrieveItem(ItemPool.KNOB_CAKE);
      }

      // If we are in Beecore, we had to adventure to get the effect.
      if (EquipmentManager.hasOutfit(OutfitPool.HAREM_OUTFIT)
          && (KoLConstants.activeEffects.contains(KoLAdventure.PERFUME)
              || (!KoLCharacter.inBeecore()
                  && InventoryManager.retrieveItem(ItemPool.KNOB_GOBLIN_PERFUME)))) {
        SpecialOutfit outfit = EquipmentDatabase.getOutfit(OutfitPool.HAREM_OUTFIT);
        RequestThread.postRequest(new EquipmentRequest(outfit));

        // If we selected the harem girl outfit, use a perfume
        if (!KoLConstants.activeEffects.contains(KoLAdventure.PERFUME)) {
          RequestThread.postRequest(UseItemRequest.getInstance(ItemPool.KNOB_GOBLIN_PERFUME));
        }
        return true;
      }

      if (EquipmentManager.hasOutfit(OutfitPool.KNOB_ELITE_OUTFIT)
          && InventoryManager.retrieveItem(ItemPool.KNOB_CAKE)) {
        // We have the elite guard uniform and have made a cake.
        SpecialOutfit outfit = EquipmentDatabase.getOutfit(OutfitPool.KNOB_ELITE_OUTFIT);
        RequestThread.postRequest(new EquipmentRequest(outfit));
        return true;
      }

      return false;
    }

    if (this.formSource.equals("dwarffactory.php")
        || this.adventureNumber == AdventurePool.MINE_OFFICE) {
      int id1 = OutfitPool.MINING_OUTFIT;
      int id2 = OutfitPool.DWARVISH_UNIFORM;

      if (EquipmentManager.isWearingOutfit(id1) || EquipmentManager.isWearingOutfit(id2)) {
        return true;
      }

      SpecialOutfit outfit =
          EquipmentManager.hasOutfit(id1)
              ? EquipmentDatabase.getOutfit(id1)
              : EquipmentManager.hasOutfit(id2) ? EquipmentDatabase.getOutfit(id2) : null;

      if (outfit == null) {
        return false;
      }

      RequestThread.postRequest(new EquipmentRequest(outfit));
      return true;
    }

    // Disguise zones require outfits
    if (this.adventureNumber != AdventurePool.COLA_BATTLEFIELD
        && (this.adventureName.contains("Disguise") || this.adventureName.contains("Uniform"))) {
      int outfitId = this.getOutfitId();

      if (outfitId == 0 || EquipmentManager.isWearingOutfit(outfitId)) {
        return true;
      }

      SpecialOutfit outfit = EquipmentDatabase.getOutfit(outfitId);
      if (!EquipmentManager.retrieveOutfit(outfit)) {
        return false;
      }

      RequestThread.postRequest(new EquipmentRequest(outfit));
      return true;
    }

    // If the person has a continuum transfunctioner, then find
    // some way of equipping it.  If they do not have one, then
    // acquire one then try to equip it.

    if (this.adventureNumber == AdventurePool.PIXEL_REALM || this.zone.equals("Vanya's Castle")) {
      AdventureResult transfunctioner = ItemPool.get(ItemPool.TRANSFUNCTIONER, 1);
      if (!InventoryManager.hasItem(transfunctioner)) {
        RequestThread.postRequest(new PlaceRequest("forestvillage", "fv_mystic"));
        GenericRequest pixelRequest = new GenericRequest("choice.php?whichchoice=664&option=1");
        // The early steps cannot be skipped
        RequestThread.postRequest(pixelRequest);
        RequestThread.postRequest(pixelRequest);
        RequestThread.postRequest(pixelRequest);
      }

      if (!KoLCharacter.hasEquipped(transfunctioner)) {
        RequestThread.postRequest(new EquipmentRequest(transfunctioner));
      }
      return true;
    }

    if (this.adventureNumber == AdventurePool.PALINDOME) {
      AdventureResult talisman = ItemPool.get(ItemPool.TALISMAN, 1);

      if (!KoLCharacter.hasEquipped(talisman)) {
        // This will pick an empty slot, or accessory1, if all are full
        RequestThread.postRequest(new EquipmentRequest(talisman));
      }
      return true;
    }

    if (this.adventureNumber == AdventurePool.HOBOPOLIS_SEWERS) {
      // Don't auto-adventure unprepared in Hobopolis sewers
      if (!Preferences.getBoolean("requireSewerTestItems")) {
        return true;
      }

      if (KoLCharacter.hasEquipped(ItemPool.get(ItemPool.GATORSKIN_UMBRELLA, 1))
          && KoLCharacter.hasEquipped(ItemPool.get(ItemPool.HOBO_CODE_BINDER, 1))
          && InventoryManager.retrieveItem(ItemPool.SEWER_WAD)
          && InventoryManager.retrieveItem(ItemPool.OOZE_O)
          && InventoryManager.retrieveItem(ItemPool.DUMPLINGS)
          && InventoryManager.retrieveItem(ItemPool.OIL_OF_OILINESS, 3)) {
        return true;
      }

      StringBuilder message = new StringBuilder();
      message.append("requireSewerTestItems is true so: ");
      if (!(KoLCharacter.hasEquipped(ItemPool.get(ItemPool.GATORSKIN_UMBRELLA, 1)))) {
        message.append("Equip a gatorskin umbrella. ");
      }
      if (!(KoLCharacter.hasEquipped(ItemPool.get(ItemPool.HOBO_CODE_BINDER, 1)))) {
        message.append("Equip a hobo code binder. ");
      }
      if (!InventoryManager.retrieveItem(ItemPool.SEWER_WAD)) {
        message.append("Acquire 1 sewer wad. ");
      }
      if (!InventoryManager.retrieveItem(ItemPool.OOZE_O)) {
        message.append("Acquire 1 bottle of Ooze-O. ");
      }
      if (!InventoryManager.retrieveItem(ItemPool.DUMPLINGS)) {
        message.append("Acquire 1 unfortunate dumpling. ");
      }
      if (!InventoryManager.retrieveItem(ItemPool.OIL_OF_OILINESS, 3)) {
        message.append("Acquire 3 oil of oiliness. ");
      }

      KoLmafia.updateDisplay(MafiaState.ERROR, message.toString());
      return false;
    }

    if (this.adventureId.equals(AdventurePool.SHROUDED_PEAK_ID)) {
      if (KoLCharacter.getElementalResistanceLevels(Element.COLD) >= 5) {
        return true;
      }
      KoLmafia.updateDisplay(MafiaState.ERROR, "You need more cold protection");
      return false;
    }

    if (this.adventureNumber == AdventurePool.ICY_PEAK) {
      if (KoLCharacter.getElementalResistanceLevels(Element.COLD) >= 1) {
        return true;
      }
      KoLmafia.updateDisplay(MafiaState.ERROR, "You need more cold protection");
      return false;
    }

    if (this.adventureNumber == AdventurePool.AIRSHIP) {
      if (QuestDatabase.isQuestLaterThan(Quest.GARBAGE, QuestDatabase.STARTED)) {
        return true;
      }

      if (!InventoryManager.retrieveItem(ItemPool.ENCHANTED_BEAN)) {
        return false;
      }

      // Use the enchanted bean by clicking on the coffee grounds.
      RequestThread.postRequest(new PlaceRequest("plains", "garbage_grounds"));
      return true;
    }

    // The casino is unlocked if you have a casino pass in inventory.
    if (this.zone.equals("Casino")) {
      return InventoryManager.retrieveItem(ItemPool.CASINO_PASS);
    }

    if (this.zone.equals("Island")) {
      if (KoLCharacter.mysteriousIslandAccessible()) {
        return true;
      }

      // There are other ways to get there, subsumed in the above
      // If we got here, we have the plans and planks
      RequestThread.postRequest(UseItemRequest.getInstance(ItemPool.DINGHY_PLANS));
      return true;
    }

    if (this.adventureNumber == AdventurePool.GUANO_JUNCTION) {
      if (KoLCharacter.getElementalResistanceLevels(Element.STENCH) >= 1) {
        return true;
      }
      KoLmafia.updateDisplay(MafiaState.ERROR, "You can't stand the stench");
      return false;
    }

    if (this.adventureNumber == AdventurePool.BATRAT
        || this.adventureNumber == AdventurePool.BEANBAT
        || this.adventureNumber == AdventurePool.BOSSBAT) {
      int sonarsUsed =
          switch (QuestDatabase.getQuest(Quest.BAT)) {
            case QuestDatabase.STARTED -> 0;
            case "step1" -> 1;
            case "step2" -> 2;
            default -> 3;
          };

      int sonarsForLocation =
          switch (this.adventureNumber) {
            case AdventurePool.BATRAT -> 1;
            case AdventurePool.BEANBAT -> 2;
            default -> 3;
          };

      if (sonarsUsed >= sonarsForLocation) {
        return true;
      }

      int sonarsToUse = sonarsForLocation - sonarsUsed;
      RequestThread.postRequest(
          UseItemRequest.getInstance(ItemPool.get(ItemPool.SONAR, sonarsToUse)));
      sonarsUsed += sonarsToUse;

      return (sonarsUsed >= sonarsForLocation);
    }

    if (this.zone.equals("The Drip")) {
      AdventureResult harness = ItemPool.get(ItemPool.DRIP_HARNESS, 1);
      if (!InventoryManager.hasItem(harness)) {
        KoLmafia.updateDisplay(MafiaState.ERROR, "You need a Drip harness to go there");
        return false;
      }

      if (!KoLCharacter.hasEquipped(harness)) {
        InventoryManager.retrieveItem(harness);
        RequestThread.postRequest(new EquipmentRequest(harness));
      }

      return true;
    }

    // The following are all things you can get day passes for.
    // The betweenBattleScript might have done that for you.

    if (this.zone.equals("The Glaciest")) {
      boolean unlocked =
          Preferences.getBoolean("coldAirportAlways")
              || Preferences.getBoolean("_coldAirportToday");
      if (!unlocked) {
        // Visit the airport and take a look.
        RequestThread.postRequest(new PlaceRequest("airport"));
        unlocked =
            Preferences.getBoolean("coldAirportAlways")
                || Preferences.getBoolean("_coldAirportToday");
      }

      return unlocked;
    }

    if (this.zone.equals("That 70s Volcano")) {
      boolean unlocked =
          Preferences.getBoolean("hotAirportAlways") || Preferences.getBoolean("_hotAirportToday");
      if (!unlocked) {
        // Visit the airport and take a look.
        RequestThread.postRequest(new PlaceRequest("airport"));
        unlocked =
            Preferences.getBoolean("hotAirportAlways")
                || Preferences.getBoolean("_hotAirportToday");
      }

      return unlocked;
    }

    if (this.zone.equals("Spring Break Beach")) {
      boolean unlocked =
          Preferences.getBoolean("sleazeAirportAlways")
              || Preferences.getBoolean("_sleazeAirportToday");
      if (!unlocked) {
        // Visit the airport and take a look.
        RequestThread.postRequest(new PlaceRequest("airport"));
        unlocked =
            Preferences.getBoolean("sleazeAirportAlways")
                || Preferences.getBoolean("_sleazeAirportToday");
      }

      return unlocked;
    }

    if (this.zone.equals("Conspiracy Island")) {
      boolean unlocked =
          Preferences.getBoolean("spookyAirportAlways")
              || Preferences.getBoolean("_spookyAirportToday");
      if (!unlocked) {
        // Visit the airport and take a look.

        RequestThread.postRequest(new PlaceRequest("airport"));
        unlocked =
            Preferences.getBoolean("spookyAirportAlways")
                || Preferences.getBoolean("_spookyAirportToday");
      }

      return unlocked;
    }

    if (this.zone.equals("Dinseylandfill")) {
      boolean unlocked =
          Preferences.getBoolean("stenchAirportAlways")
              || Preferences.getBoolean("_stenchAirportToday");
      if (!unlocked) {
        // Visit the airport and take a look.

        RequestThread.postRequest(new PlaceRequest("airport"));
        unlocked =
            Preferences.getBoolean("stenchAirportAlways")
                || Preferences.getBoolean("_stenchAirportToday");
      }

      return unlocked;
    }

    if (this.adventureNumber == AdventurePool.SPACEGATE) {
      if (KoLCharacter.isKingdomOfExploathing()) {
        return false;
      }

      boolean unlocked =
          Preferences.getBoolean("spacegateAlways") || Preferences.getBoolean("_spacegateToday");
      if (!unlocked) {
        // Visit the mountains and take a look.
        RequestThread.postRequest(new PlaceRequest("mountains"));
        unlocked = Preferences.getBoolean("_spacegateToday");
      }

      return unlocked;
    }

    if (this.zone.equals("Gingerbread City")) {
      boolean unlocked =
          Preferences.getBoolean("gingerbreadCityAvailable")
              || Preferences.getBoolean("_gingerbreadCityToday");
      if (!unlocked) {
        // Visit the Mountains and take a look.
        RequestThread.postRequest(new PlaceRequest("mountains"));
        unlocked = Preferences.getBoolean("_gingerbreadCityToday");
      }

      return unlocked;
    }

    return true;
  }

  public int getOutfitId() {
    switch (this.adventureNumber) {
      case AdventurePool.FRAT_HOUSE_DISGUISED:
      case AdventurePool.WARTIME_FRAT_HOUSE_DISGUISED:
        // Can be either HIPPY_OUTFIT or WAR_HIPPY_OUTFIT
        return EquipmentManager.hasOutfit(OutfitPool.WAR_HIPPY_OUTFIT)
            ? OutfitPool.WAR_HIPPY_OUTFIT
            : EquipmentManager.hasOutfit(OutfitPool.HIPPY_OUTFIT) ? OutfitPool.HIPPY_OUTFIT : 0;
      case AdventurePool.HIPPY_CAMP_DISGUISED:
      case AdventurePool.WARTIME_HIPPY_CAMP_DISGUISED:
        // Can be either FRAT_OUTFIT or WAR_FRAT_OUTFIT
        return EquipmentManager.hasOutfit(OutfitPool.WAR_FRAT_OUTFIT)
            ? OutfitPool.WAR_FRAT_OUTFIT
            : EquipmentManager.hasOutfit(OutfitPool.FRAT_OUTFIT) ? OutfitPool.FRAT_OUTFIT : 0;
      case AdventurePool.CLOACA_BATTLEFIELD:
        return OutfitPool.CLOACA_UNIFORM;
      case AdventurePool.DYSPEPSI_BATTLEFIELD:
        return OutfitPool.DYSPEPSI_UNIFORM;
      case AdventurePool.FRAT_UNIFORM_BATTLEFIELD:
        return OutfitPool.WAR_FRAT_OUTFIT;
      case AdventurePool.HIPPY_UNIFORM_BATTLEFIELD:
        return OutfitPool.WAR_HIPPY_OUTFIT;
      default:
        return 0;
    }
  }

  /**
   * Retrieves the string form of the adventure contained within this encapsulation, which is
   * generally the name of the adventure.
   *
   * @return The string form of the adventure
   */
  @Override
  public String toString() {
    return this.normalString;
  }

  /**
   * Executes the appropriate <code>GenericRequest</code> for the adventure encapsulated by this
   * <code>KoLAdventure</code>.
   */
  @Override
  public void run() {
    if (RecoveryManager.isRecoveryPossible() && !RecoveryManager.runThresholdChecks()) {
      return;
    }

    // Validate access that a betweenBattleScript cannot help with
    this.validate1();
    if (!this.isValidAdventure) {
      if (KoLmafia.permitsContinue()) {
        // validate1 did not give its own error message
        KoLmafia.updateDisplay(MafiaState.ERROR, "That area is not available.");
      }
      return;
    }

    // Make sure there are enough adventures to run the request
    if (KoLCharacter.getAdventuresLeft() == 0
        || KoLCharacter.getAdventuresLeft() < this.request.getAdventuresUsed()) {
      KoLmafia.updateDisplay(MafiaState.PENDING, "Ran out of adventures.");
      return;
    }

    // The Shore costs Meat to visit
    if (this.adventureNumber == AdventurePool.THE_SHORE
        && KoLCharacter.getAvailableMeat() < (KoLCharacter.inFistcore() ? 5 : 500)) {
      KoLmafia.updateDisplay(MafiaState.ERROR, "Insufficient funds for a shore vacation.");
      return;
    }

    // Make sure that our chosen combat choices will work in the chosen zone.

    if (this.request instanceof AdventureRequest
        && !this.isNonCombatsOnly()
        && !Macrofier.usingCombatFilter()) {
      String action = Preferences.getString("battleAction");

      // Check for dictionaries as a battle strategy, if the
      // person is not adventuring at valley beyond the the chasm.
      if (action.contains("dictionary")) {
        if (!InventoryManager.hasItem(FightRequest.DICTIONARY1)
            && !InventoryManager.hasItem(FightRequest.DICTIONARY2)) {
          KoLmafia.updateDisplay(MafiaState.ERROR, "Sorry, you don't have a dictionary.");
          return;
        }

        if (this.request.getAdventuresUsed() == 1
            && this.adventureNumber != AdventurePool.VALLEY_OF_ROF_LM_FAO
            && !KoLCharacter.getFamiliar().isCombatFamiliar()) {
          KoLmafia.updateDisplay(MafiaState.ERROR, "A dictionary would be useless there.");
          return;
        }
      }

      // If the person doesn't stand a chance of surviving,
      // automatically quit and tell them so.

      if (this.areaSummary != null
          && action.startsWith("attack")
          && !this.areaSummary.willHitSomething()
          && !KoLCharacter.getFamiliar().isCombatFamiliar()) {
        KoLmafia.updateDisplay(MafiaState.ERROR, "You can't hit anything there.");
        return;
      }

      if (FightRequest.isInvalidShieldlessAttack(action)) {
        KoLmafia.updateDisplay(
            MafiaState.ERROR, "Your selected attack skill is useless without a shield.");
        return;
      }
    }

    if (this.areaSummary != null
        && !KoLCharacter.inZombiecore()
        && !KoLCharacter.inNuclearAutumn()
        && !KoLCharacter.inGLover()
        && !KoLCharacter.isKingdomOfExploathing()
        && this.areaSummary.poison() <= Preferences.getInteger("autoAntidote")
        && !KoLCharacter.hasEquipped(ItemPool.get(ItemPool.BEZOAR_RING, 1))) {
      if (!InventoryManager.checkpointedRetrieveItem(ItemPool.ANTIDOTE)) {
        return;
      }
    }

    // Let the betweenBattleScript do anything it wishes
    KoLAdventure.setNextAdventure(this);
    if (RecoveryManager.isRecoveryPossible()) {
      RecoveryManager.runBetweenBattleChecks(!this.isNonCombatsOnly());

      if (!KoLmafia.permitsContinue()) {
        return;
      }
    }

    // Validate things that a betweenBattleScript might have helped with
    this.validate2();
    if (!this.isValidAdventure) {
      if (KoLmafia.permitsContinue()) {
        // validate2 did not give its own error message
        KoLmafia.updateDisplay(MafiaState.ERROR, "That area is not available.");
      }
      return;
    }

    // All is well. Do it to it!
    RequestThread.postRequest(this.request);
  }

  private static final Pattern ADVENTUREID_PATTERN = Pattern.compile("snarfblat=([\\d]+)");
  private static final Pattern MINE_PATTERN = Pattern.compile("mine=([\\d]+)");

  public static final KoLAdventure setLastAdventure(
      String adventureId, final String adventureName, String adventureURL, final String container) {
    KoLAdventure adventure = AdventureDatabase.getAdventureByURL(adventureURL);
    if (adventure == null) {
      int index = adventureURL.indexOf("?");
      String adventurePage;

      if (index != -1) {
        adventurePage = adventureURL.substring(0, index);
      } else {
        adventurePage = adventureURL;
      }

      if (adventurePage.equals("mining.php")) {
        Matcher matcher = KoLAdventure.MINE_PATTERN.matcher(adventureURL);
        adventureId = matcher.find() ? matcher.group(1) : "0";
        adventureURL = adventurePage + "?mine=" + adventureId;
      } else if (adventurePage.equals("adventure.php")) {
        if (adventureId.isEmpty()) {
          Matcher matcher = KoLAdventure.ADVENTUREID_PATTERN.matcher(adventureURL);
          adventureId = matcher.find() ? matcher.group(1) : "0";
        }
      } else if (adventurePage.equals("main.php")) {
        // This is "(none)" after a new ascension
        return null;
      } else if (KoLAdventure.unknownAdventures.contains(adventureName)) {
        // If we've already logged this one, don't do it again
        return null;
      } else {
        // Don't register as an adventure, but save name
        KoLAdventure.unknownAdventures.add(adventureName);
        Preferences.setString("lastAdventure", adventureName);
        RequestLogger.updateSessionLog(
            "Unknown last adventure: id = '"
                + adventureId
                + "' name = '"
                + adventureName
                + "' URL = '"
                + adventureURL
                + "' container = '"
                + container
                + "'");
        return null;
      }

      RequestLogger.printLine("Adding new location: " + adventureName + " - " + adventureURL);

      // We could use "container" to pick the zone the adventure goes in

      // Detach strings from the responseText
      adventure = new KoLAdventure("Override", adventurePage, adventureId, adventureName);
      AdventureDatabase.addAdventure(adventure);
    }

    KoLAdventure.setLastAdventure(adventure);
    return adventure;
  }

  public static final void setLastAdventure(final String adventureName) {
    KoLAdventure adventure = AdventureDatabase.getAdventure(adventureName);
    if (adventure == null) {
      KoLAdventure.lastVisitedLocation = null;
      KoLAdventure.lastLocationName = null;
      KoLAdventure.lastLocationURL = null;
      Preferences.setString("lastAdventure", adventureName);
      KoLCharacter.updateSelectedLocation(null);
      return;
    }
    KoLAdventure.setLastAdventure(adventure);
  }

  public static final void setLastAdventure(final KoLAdventure adventure) {
    if (adventure == null) {
      return;
    }

    String adventureId = adventure.adventureId;
    int adventureNumber = adventure.adventureNumber;
    String adventureName = adventure.adventureName;
    String adventureURL = adventure.formSource;

    KoLAdventure.lastVisitedLocation = adventure;
    KoLAdventure.lastLocationName = adventure.getPrettyAdventureName(adventureURL);
    KoLAdventure.lastLocationURL = adventureURL;
    Preferences.setString("lastAdventure", adventureName);

    // If you were able to access some hidden city areas you must have unlocked them so update quest
    // status
    if (adventureNumber == AdventurePool.HIDDEN_APARTMENT
        && Preferences.getInteger("hiddenApartmentProgress") == 0) {
      Preferences.setInteger("hiddenApartmentProgress", 1);
    } else if (adventureNumber == AdventurePool.HIDDEN_HOSPITAL
        && Preferences.getInteger("hiddenHospitalProgress") == 0) {
      Preferences.setInteger("hiddenHospitalProgress", 1);
    } else if (adventureNumber == AdventurePool.HIDDEN_OFFICE
        && Preferences.getInteger("hiddenOfficeProgress") == 0) {
      Preferences.setInteger("hiddenOfficeProgress", 1);
    } else if (adventureNumber == AdventurePool.HIDDEN_BOWLING_ALLEY
        && Preferences.getInteger("hiddenBowlingAlleyProgress") == 0) {
      Preferences.setInteger("hiddenBowlingAlleyProgress", 1);
    }
  }

  public static final void setNextAdventure(final String adventureName) {
    KoLAdventure adventure = AdventureDatabase.getAdventure(adventureName);
    if (adventure == null) {
      Preferences.setString("nextAdventure", adventureName);
      KoLCharacter.updateSelectedLocation(null);
      return;
    }
    KoLAdventure.setNextAdventure(adventure);
    EncounterManager.registerAdventure(adventureName);
  }

  public static final void setNextAdventure(final KoLAdventure adventure) {
    if (adventure == null) {
      return;
    }

    Preferences.setString("nextAdventure", adventure.adventureName);
    KoLCharacter.updateSelectedLocation(adventure);
    NamedListenerRegistry.fireChange("(koladventure)");
  }

  public static final KoLAdventure lastVisitedLocation() {
    return KoLAdventure.lastVisitedLocation;
  }

  public static final int lastAdventureId() {
    KoLAdventure location = KoLAdventure.lastVisitedLocation;

    return location == null || !StringUtilities.isNumeric(location.adventureId)
        ? 0
        : StringUtilities.parseInt(location.adventureId);
  }

  public static final String lastAdventureIdString() {
    KoLAdventure location = KoLAdventure.lastVisitedLocation;
    return location == null ? "" : location.adventureId;
  }

  public static final boolean recordToSession(final String urlString) {
    // This is the first half of logging an adventure location
    // given only the URL. We try to deduce where the player is
    // adventuring and save it for verification later. We also do
    // some location specific setup.

    if (KoLmafia.isRefreshing()) {
      return false;
    }

    // See if this is a standard "adventure" in adventures.txt
    KoLAdventure adventure = KoLAdventure.findAdventure(urlString);
    if (adventure != null) {
      adventure.prepareToAdventure(urlString);
      KoLAdventure.lastVisitedLocation = adventure;
      KoLAdventure.lastLocationName = adventure.getPrettyAdventureName(urlString);
      KoLAdventure.lastLocationURL = urlString;
      KoLAdventure.locationLogged = false;
      return true;
    }

    // No. See if it's a special "adventure"
    String location = AdventureDatabase.getUnknownName(urlString);
    if (location == null) {
      return false;
    }

    if (!urlString.contains("?")) {
      return true;
    }

    KoLAdventure.lastVisitedLocation = null;
    KoLAdventure.lastLocationName = location;
    KoLAdventure.lastLocationURL = urlString;
    KoLAdventure.locationLogged = false;

    return true;
  }

  private static KoLAdventure findAdventure(final String urlString) {
    if (urlString.equals("barrel.php")) {
      return null;
    }
    if (urlString.startsWith("mining.php") && urlString.contains("intro=1")) {
      return null;
    }
    return AdventureDatabase.getAdventureByURL(urlString);
  }

  private static KoLAdventure findAdventureAgain(final String responseText) {
    // Look for an "Adventure Again" link and return the
    // KoLAdventure that it matches.
    Matcher matcher = ADVENTURE_AGAIN.matcher(responseText);
    if (!matcher.find()) {
      return null;
    }

    return KoLAdventure.findAdventure(matcher.group(1));
  }

  private void prepareToAdventure(final String urlString) {
    // If we are in a drunken stupor, return now.
    if (KoLCharacter.isFallingDown()
        && !urlString.startsWith("trickortreat")
        && !KoLCharacter.hasEquipped(ItemPool.get(ItemPool.DRUNKULA_WINEGLASS, 1))) {
      return;
    }

    int id = 0;

    if (StringUtilities.isNumeric(this.adventureId)) {
      id = StringUtilities.parseInt(this.adventureId);

      switch (id) {
        case AdventurePool.FCLE:
          AdventureResult mop = ItemPool.get(ItemPool.MIZZENMAST_MOP, 1);
          AdventureResult polish = ItemPool.get(ItemPool.BALL_POLISH, 1);
          AdventureResult sham = ItemPool.get(ItemPool.RIGGING_SHAMPOO, 1);
          if (InventoryManager.hasItem(mop)
              && InventoryManager.hasItem(polish)
              && InventoryManager.hasItem(sham)) {
            RequestThread.postRequest(UseItemRequest.getInstance(mop));
            RequestThread.postRequest(UseItemRequest.getInstance(polish));
            RequestThread.postRequest(UseItemRequest.getInstance(sham));
          }
          break;
      }
    }

    if (!(this.getRequest() instanceof AdventureRequest) || this.isValidAdventure) {
      return;
    }

    // Visit the untinker before adventuring at Degrassi Knoll.

    if (id == AdventurePool.DEGRASSI_KNOLL_GARAGE) {
      UntinkerRequest.canUntinker();
    }

    this.isValidAdventure = true;

    // Make sure you're wearing the appropriate equipment
    // for the King's chamber in Cobb's knob.

    if (this.formSource.equals("cobbsknob.php")) {
      this.validate2();
    }
  }

  // Automated adventuring in an area can result in a failure. We go into
  // the ERROR or the PENDING state, depending on whether we should stop
  // a script for attempting the adventure. The default is ERROR. Use
  // PENDING only when the script could not have known that the attempt
  // would fail.

  record AdventureFailure(String responseText, String message, MafiaState severity) {
    public AdventureFailure(String responseText, String message) {
      this(responseText, message, MafiaState.ERROR);
    }
  }

  private static final AdventureFailure[] ADVENTURE_FAILURES = {
    // KoL bug: returning a blank page. This must be index 0.
    new AdventureFailure("", "KoL returned a blank page."),

    // Lots of places.
    new AdventureFailure(
        "It is recommended that you have at least",
        "Your stats are too low for this location.  Adventure manually to acknowledge or disable this warning."),

    // Lots of places.
    new AdventureFailure("You shouldn't be here", "You can't get to that area."),

    // Lots of places.
    new AdventureFailure("not yet be accessible", "You can't get to that area."),

    // Lots of places.
    new AdventureFailure("You can't get there", "You can't get to that area."),

    // Lots of places.
    new AdventureFailure("Seriously.  It's locked.", "You can't get to that area."),

    // 8-bit realm and Vanya's Castle
    new AdventureFailure(
        "You can't get to the 8-bit realm right now", "You can't get to that area."),

    // Out of adventures
    new AdventureFailure(
        "You're out of adventures", "You're out of adventures.", MafiaState.PENDING),

    // Out of adventures in the Daily Dungeon
    new AdventureFailure(
        "You don't have any adventures.", "You're out of adventures.", MafiaState.PENDING),

    // Out of adventures at Shore
    new AdventureFailure(
        "You don't have enough Adventures left", "You're out of adventures.", MafiaState.PENDING),

    // Out of meat at Shore
    new AdventureFailure(
        "You can't afford to go on a vacation", "You can't afford to go on a vacation."),

    // Too drunk at shore
    new AdventureFailure(
        "You're too drunk to go on vacation", "You are too drunk to go on a vacation."),

    // Beaten up at zero HP
    new AdventureFailure(
        "You're way too beaten up to go on an adventure right now",
        "You can't adventure at 0 HP.",
        MafiaState.PENDING),

    // Typical Tavern with less than 100 Meat
    new AdventureFailure(
        "Why go to the Tavern if you can't afford to drink?",
        "You can't afford to go out drinking."),

    // The Road to White Citadel
    new AdventureFailure(
        "You've already found the White Citadel",
        "The Road to the White Citadel is already cleared."),

    // Friar's Ceremony Location without the three items
    new AdventureFailure(
        "You don't appear to have all of the elements necessary to perform the ritual",
        "You don't have everything you need."),

    // You need some sort of stench protection to adventure in there.
    // You're going to need some sort of stench protection if you want to adventure here.
    new AdventureFailure("need some sort of stench protection", "You need stench protection."),

    // You need some sort of protection from the cold if you're
    // going to visit the Icy Peak.
    new AdventureFailure(
        "You need some sort of protection from the cold", "You need cold protection."),

    // You try to enter the Haunted Library, but the door is locked. I guess this particular
    // information doesn't want to be free.
    new AdventureFailure(
        "I guess this particular information doesn't want to be free",
        "You need the Spookyraven library key."),

    // Mining while drunk
    new AdventureFailure(
        "You're too drunk to spelunk, as it were", "You are too drunk to go there."),

    // Pyramid Lower Chamber while drunk
    new AdventureFailure("You're too drunk to screw around", "You are too drunk to go there."),

    // You can't adventure there without some way of breathing underwater...
    new AdventureFailure(
        "without some way of breathing underwater", "You can't breathe underwater."),

    // You can't adventure there now -- Gort wouldn't be able to breathe!
    new AdventureFailure("wouldn't be able to breathe", "Your familiar can't breathe underwater."),

    // It wouldn't be safe to go in there dressed like you are. You should consider a Mer-kin
    // disguise.
    new AdventureFailure(
        "You should consider a Mer-kin disguise.", "You aren't wearing a Mer-kin disguise."),

    // Attempting to enter the Cola Wars Battlefield with level > 5
    new AdventureFailure(
        "The temporal rift in the plains has closed",
        "The temporal rift has closed.",
        MafiaState.PENDING),

    // Out of your mining uniform, you are quickly identified as a
    // stranger and shown the door.
    new AdventureFailure(
        "you are quickly identified as a stranger", "You aren't wearing an appropriate uniform."),

    // You're not properly equipped for that. Get into a uniform.
    new AdventureFailure("Get into a uniform", "You aren't wearing an appropriate uniform."),

    // There are no Frat soldiers left
    new AdventureFailure("There are no Frat soldiers left", "There are no Frat soldiers left."),

    // There are no Hippy soldiers left
    new AdventureFailure("There are no Hippy soldiers left", "There are no Hippy soldiers left."),

    // Spooky Gravy Burrow before told to go there:
    // You should probably stay out of there unless you have a good
    // reason to go in. Like if you were on a quest to find
    // something in there, or something.
    new AdventureFailure(
        "You should probably stay out of there",
        "You have not been given the quest to go there yet.",
        MafiaState.PENDING),

    // Worm Wood while not Absinthe Minded
    new AdventureFailure(
        "For some reason, you can't find your way back there",
        "You need to be Absinthe Minded to go there.",
        MafiaState.PENDING),

    // "You can't take it any more. The confusion, the nostalgia,
    // the inconsistent grammar. You break the bottle on the
    // ground, and stomp it to powder."
    new AdventureFailure(
        "You break the bottle on the ground",
        "You are no longer gazing into the bottle.",
        MafiaState.PENDING),

    // You're in the regular dimension now, and don't remember how
    // to get back there.
    new AdventureFailure(
        "You're in the regular dimension now",
        "You are no longer Half-Astral.",
        MafiaState.PENDING),

    // The Factory has faded back into the spectral mists, and
    // eldritch vapors and such.
    new AdventureFailure(
        "faded back into the spectral mists", "No one may know of its coming or going."),

    // You wander around the farm for a while, but can't find any
    // additional ducks to fight. Maybe some more will come out of
    // hiding by tomorrow.
    new AdventureFailure(
        "can't find any additional ducks", "Nothing more to do here today.", MafiaState.PENDING),

    // There are no more ducks here.
    new AdventureFailure("no more ducks here", "Farm area cleared.", MafiaState.PENDING),

    // You don't know where that place is.
    new AdventureFailure(
        "You don't know where that place is.",
        "Use a \"DRINK ME\" potion before trying to adventure here.",
        MafiaState.PENDING),

    // Orchard failure - You try to enter the feeding chamber, but
    // your way is blocked by a wriggling mass of filthworm drones.
    // Looks like they don't let anything in here if they don't
    // recognize its smell.
    new AdventureFailure(
        "Looks like they don't let anything in here if they don't recognize its smell.",
        "Use a filthworm hatchling scent gland before trying to adventure here."),

    // Orchard failure - You try to enter the royal guards'
    // chamber, but you're immediately shoved back out into the
    // tunnel. Looks like the guards will only let you in here if
    // you smell like food.
    new AdventureFailure(
        "Looks like the guards will only let you in here if you smell like food.",
        "Use a filthworm drone scent gland before trying to adventure here."),

    // Orchard failure - You try to enter the filthworm queen's
    // chamber, but the guards outside the door block the entrance.
    // You must not smell right to 'em.
    new AdventureFailure(
        "You must not smell right to 'em.",
        "Use a filthworm royal guard scent gland before trying to adventure here."),

    // Orchard failure - The filthworm queen has been slain, and the
    // hive lies empty 'neath the orchard.
    new AdventureFailure(
        "The filthworm queen has been slain", "The filthworm queen has been slain."),

    // You've already retrieved all of the stolen Meat
    new AdventureFailure(
        "already retrieved all of the stolen Meat",
        "You already recovered the Nuns' Meat.",
        MafiaState.PENDING),

    // There are no hippy soldiers left -- the way to their camp is clear!
    new AdventureFailure(
        "the way to their camp is clear", "There are no hippy soldiers left.", MafiaState.PENDING),

    // Cobb's Knob King's Chamber after defeating the goblin king.
    new AdventureFailure(
        "You've already slain the Goblin King",
        "You already defeated the Goblin King.",
        MafiaState.PENDING),

    // The Haert of the Cyrpt after defeating the Bonerdagon
    new AdventureFailure(
        "Bonerdagon has been defeated", "You already defeated the Bonerdagon.", MafiaState.PENDING),

    // Any cyrpt area after defeating the sub-boss
    new AdventureFailure("already undefiled", "Cyrpt area cleared.", MafiaState.PENDING),

    // The Summoning Chamber after Lord Spookyraven has been defeated
    //
    // You enter the Summoning Chamber.  The air is heavy with
    // evil, and otherworldly whispers echo melodramatically
    // through your mind
    new AdventureFailure(
        "otherworldly whispers", "You already defeated Lord Spookyraven.", MafiaState.PENDING),

    // Ed the undying defeated
    new AdventureFailure(
        "Ed the Undying sleeps once again",
        "Ed the Undying has already been defeated.",
        MafiaState.PENDING),

    // You probably shouldn't -- you don't trust those rats not to steal your token!
    new AdventureFailure(
        "don't trust those rats not to steal",
        "You don't trust those rats not to steal your token!.",
        MafiaState.PENDING),

    // That's too far to walk, and
    // <a href=clan_dreadsylvania.php?place=carriage>the Carriageman</a>
    // isn't drunk enough to take you there.
    new AdventureFailure(
        "That's too far to walk",
        "The Carriageman isn't drunk enough to take you there.",
        MafiaState.PENDING),

    // The forest is silent, those who stalked it having themselves been stalked.
    new AdventureFailure(
        "The forest is silent",
        "The Dreadsylvanian Woods boss has been defeated.",
        MafiaState.PENDING),

    // The village is now a ghost town in the figurative sense, rather than the literal.
    new AdventureFailure(
        "The village is now a ghost town",
        "The Dreadsylvanian Village boss has been defeated.",
        MafiaState.PENDING),

    // Look upon this castle, ye mighty, and despair, because the king is dead, baby.
    new AdventureFailure(
        "the king is dead, baby",
        "The Dreadsylvanian Castle boss has been defeated.",
        MafiaState.PENDING),

    // This part of the city is awfully unremarkable, now that
    // you've cleared that ancient protector spirit out.
    new AdventureFailure(
        "cleared that ancient protector spirit out",
        "You already defeated the protector spirit in that square."),

    // Now that you've put something in the round depression in the
    // altar, the altar doesn't really do anything but look
    // neat. Those ancient guys really knew how to carve themselves
    // an altar, mmhmm.
    new AdventureFailure(
        "the altar doesn't really do anything but look neat",
        "You already used the altar in that square."),

    // Here's poor Dr. Henry "Dakota" Fanning, Ph.D, R.I.P., lying
    // here in a pile just where you left him.
    new AdventureFailure(
        "lying here in a pile just where you left him",
        "You already looted Dr. Fanning in that square."),

    // You wander into the empty temple and look around. Remember
    // when you were in here before, and tried to gank some old
    // amulet off of that mummy? And then its ghost came out and
    // tried to kill you? But you destroyed it, and won the ancient
    // doohickey?
    //
    // Good times, man. Good times.
    new AdventureFailure(
        "You wander into the empty temple", "You already looted the temple in that square."),

    // You climb the stairs from the castle's basement, but the
    // door at the top is closed and you can't find the doorknob.
    //
    // You'll have to find another way up.
    new AdventureFailure(
        "You'll have to find another way up",
        "You haven't opened the ground floor of the castle yet."),

    // You have to learn to walk before you can learn to fly.
    //
    // Also you can't get to the top floor of a building if you can't get to the ground floor.
    new AdventureFailure(
        "you can't get to the ground floor",
        "You haven't opened the ground floor of the castle yet."),

    // The door at the top of the ground floor stairway is also
    // closed, and you're still too short to reach a doorknob
    // that's forty feet over your head.
    //
    // You'll have to figure out some other way to get upstairs.
    new AdventureFailure(
        "You'll have to figure out some other way to get upstairs",
        "You haven't opened the top floor of the castle yet."),

    // The portal is open! Head home and prepare to save some children!
    new AdventureFailure("prepare to save some children", "The portal is open."),

    // It looks like things are running pretty smoothly at the
    // factory right now -- there's nobody to fight.
    new AdventureFailure("things are running pretty smoothly", "Nothing more to do here today."),

    // You should talk to Edwing before you head back in there, and
    // wait for him to formulate a plan.
    new AdventureFailure("You should talk to Edwing", "Nothing more to do here today."),

    // The compound is abandoned now...
    new AdventureFailure("The compound is abandoned now", "Nothing more to do here today."),

    // Between the wind and the weird spiky bits all over it, you
    // can't make it to the second story of the fortress without
    // some way of escaping gravity.
    new AdventureFailure(
        "some way of escaping gravity", "You are not wearing a warbear hoverbelt."),

    // Your hoverbelt would totally do the trick to get you up
    // there, only it's out of juice.
    new AdventureFailure("it's out of juice", "Your hoverbelt needs a new battery."),

    // You float up to the third story of the fortress, but all you
    // find is a locked door with a keypad next to it, and you
    // don't have a code.
    new AdventureFailure("you don't have a code", "You don't have a warbear badge."),

    // There's nothing left of Ol' Scratch but a crater and a
    // stove.  Burnbarrel Blvd. is still hot, but it's no longer
    // bothered.  Or worth bothering with.
    new AdventureFailure(
        "There's nothing left of Ol' Scratch", "Nothing more to do here.", MafiaState.PENDING),

    // There's nothing left in Exposure Esplanade. All of the snow
    // forts have been crushed or melted, all of the igloos are
    // vacant, and all of the reindeer are off playing games
    // somewhere else.
    new AdventureFailure(
        "There's nothing left in Exposure Esplanade",
        "Nothing more to do here.",
        MafiaState.PENDING),

    // The Heap is empty.  Well, let me rephrase that.  It's still
    // full of garbage, but there's nobody and nothing of interest
    // mixed in with the garbage.
    new AdventureFailure("The Heap is empty", "Nothing more to do here.", MafiaState.PENDING),

    // There's nothing going on here anymore -- the tombs of the
    // Ancient Hobo Burial Ground are all as silent as themselves.
    new AdventureFailure(
        "There's nothing going on here anymore", "Nothing more to do here.", MafiaState.PENDING),

    // There's nothing left in the Purple Light District.  All of
    // the pawn shops and adult bookshops have closed their doors
    // for good.
    new AdventureFailure(
        "There's nothing left in the Purple Light District",
        "Nothing more to do here.",
        MafiaState.PENDING),

    // The Hoboverlord has been defeated, and Hobopolis Town Square
    // lies empty.
    new AdventureFailure(
        "Hobopolis Town Square lies empty", "Nothing more to do here.", MafiaState.PENDING),

    // The bathrooms are empty now -- looks like you've taken care
    // of the elf hobo problem for the time being.
    new AdventureFailure("bathrooms are empty now", "Nothing more to do here.", MafiaState.PENDING),

    // The Skies over Valhalls

    // You poke your head through the slash, and find yourself
    // looking at Valhalla from a dizzying height. Come down now,
    // they'll say, but there's no way you're going all the way
    // through that slash without some sort of transportation.
    new AdventureFailure(
        "there's no way you're going all the way through that slash",
        "You don't have a flying mount.",
        MafiaState.PENDING),

    // You can't do anything without some way of flying.  And
    // before you go pointing at all of the stuff in your inventory
    // that says it lets you fly or float or levitate or whatever,
    // that stuff won't work.  You're gonna need a hideous winged
    // yeti mount, because that's the only thing that can handle
    // this particular kind of flying.  Because of science.
    new AdventureFailure(
        "You can't do anything without some way of flying",
        "You don't have a flying mount.",
        MafiaState.PENDING),

    // There are at least two of everything up there, and you're
    // also worried that you might fall off your yeti. You should
    // maybe come back when you're at least slightly less drunk.
    new AdventureFailure(
        "You should  maybe come back when you're at least slightly less drunk",
        "You are too drunk.",
        MafiaState.PENDING),

    // You don't have the energy to attack a problem this size. Go
    // drink some soda or something.
    new AdventureFailure(
        "You don't have the energy to attack a problem this size",
        "You need at least 20% buffed max MP.",
        MafiaState.PENDING),

    // You're not in good enough shape to deal with a threat this
    // large. Go get some rest, or put on some band-aids or
    // something.
    new AdventureFailure(
        "You're not in good enough shape to deal with a threat this large",
        "You need at least 20% buffed max HP.",
        MafiaState.PENDING),

    // Your El Vibrato portal has run out of power. You should go
    // back to your campsite and charge it back up.
    new AdventureFailure(
        "Your El Vibrato portal has run out of power",
        "Your El Vibrato portal has run out of power",
        MafiaState.PENDING),

    // No longer Transpondent
    new AdventureFailure(
        "you don't know the transporter frequency", "You are no longer Transpondent."),

    // No longer Transpondent
    new AdventureFailure(
        "without the proper transporter frequency", "You are no longer Transpondent."),

    // No longer Dis Abled
    //
    // Remember that devilish folio you read?
    // No, you don't! You don't have it all still in your head!
    // Better find a new one you can read! I swear this:
    // 'Til you do, you can't visit the Suburbs of Dis!
    new AdventureFailure("you can't visit the Suburbs of Dis", "You are no longer Dis Abled."),

    // Abyssal Portals
    //
    // The area around the portal is quiet. Looks like you took
    // care of all of the seals. Maybe check back tomorrow.
    new AdventureFailure("area around the portal is quiet", "The Abyssal Portal is quiet."),

    // The Secret Government Laboratory
    //
    // You can't go in there without wearing a Personal Ventilation
    // Unit. Who knows what would happen if you breathed the air in
    // there.
    new AdventureFailure(
        "Who knows what would happen if you breathed the air",
        "You need to equip your Personal Ventilation Unit."),

    // GameInformPowerPro video game levels
    //
    // You already cleared out this area.
    new AdventureFailure("You already cleared out this area", "You already cleared out this area"),

    // This area is closed.
    new AdventureFailure("This area is closed", "You completed the video game"),

    // You wander around, off the Florida Keys, but can't find anything
    new AdventureFailure("off the Florida Keys", "You need a Tropical Contact High to go there."),

    // You approach the adorable little door, but you can't figure
    // out how to open it. I guess it's a secret door that will
    // only open for gravy fairies.
    new AdventureFailure(
        "only open for gravy fairies", "You need to bring an elemental gravy fairy with you."),

    // You shouldn't be here dressed like that. It's just not safe.
    new AdventureFailure("You shouldn't be here dressed like that", "You can't pass as a pirate."),

    // LOLmec's lair lies lempty. Empty.
    new AdventureFailure("LOLmec's lair lies lempty", "You already beat LOLmec"),

    // Already beat Yomama.
    new AdventureFailure("Already beat Yomama", "You already beat Yomama"),

    // The ghost has arrived. Your time has run out!
    new AdventureFailure("The ghost has arrived", "Your tale of spelunking is over."),

    // Gingerbread City
    new AdventureFailure(
        "The gingerbread city has collapsed.", "The gingerbread city has collapsed."),

    // Hole in the sky

    // You can see it, but you can't get to it without some means of traveling...
    //
    // TO SPACE
    new AdventureFailure("you can't get to it", "You need a way to travel in space."),

    // The spacegate is out of energy for today. You can explore
    // another planet tomorrow. Or the same planet, if you
    // want. The spacegate isn't the boss of you.
    new AdventureFailure("out of energy for today", "The Spacegate is out of energy for today."),

    // ou approach the door to the spire, but you can't bear the
    // silence. Maybe you should try those earplugs Tammy gave you.
    new AdventureFailure("you can't bear the silence", "You are not wearing your anti-earplugs.."),

    // You stomp around the spire for a while, but it looks like
    // this level is all cleared out. They'll probably redeploy
    // more mimes to it by tomorrow.
    new AdventureFailure(
        "redeploy more mimes to it by tomorrow", "There are no more mimes left today."),

    // Your G. E. M. beeps at you, indicating that your time in FantasyRealm has come to an end for
    // today.
    new AdventureFailure(
        "your time in FantasyRealm has come to an end for today",
        "Your time in FantasyRealm is over for today."),

    // Technically the Neverending Party is still going, because the Neverending Party never ends,
    // but right now everybody is passed out, so it's more of a Neverending Nap.
    // It'll probably get rowdy again by tomorrow
    new AdventureFailure(
        "It'll probably get rowdy again by tomorrow", "The Neverending Party is over for today."),

    // You wouldn't be seen dead at the Neverending Party without your PARTY HARD shirt on.
    // Not today, at least, since you've already made an appearance in it.
    new AdventureFailure(
        "without your PARTY HARD shirt on",
        "Cannot adventure at The Neverending Party without your PARTY HARD shirt on."),

    // That isn't a place you can get to the way you're dressed.
    // (Clicking Last Adventure having unequipped Talisman o' Namsilat)
    new AdventureFailure(
        "That isn't a place you can get to the way you're dressed",
        "You're not equipped properly to adventure there."),

    // If you got into a fight right now, you wouldn't be able to attack anything! Equip those boots
    // you found.
    new AdventureFailure(
        "Equip those boots you found", "Plumbers cannot adventure without appropriate gear."),

    // Something tells you the last bit of this is going to take a <i>long</i> time.  Come back when
    // you've got more Adventures to spare.
    new AdventureFailure("going to take a <i>long</i> time", "You need 7 Adventures to fight Ed."),

    // You can't go there because your Drippy Juice supply has run out.
    new AdventureFailure("Drippy Juice supply", "You've run out of Drippy Juice."),

    // Elemental Airport airplanes
    new AdventureFailure("You don't know where that is", "You can't get there from here."),

    // That isn't a place you can go.
    new AdventureFailure("That isn't a place you can go", "You can't get there from here."),

    // Site Alpha Dormitory
    //
    // It's getting colder! Better bundle up.
    // The extreme cold makes it impossible for you to continue...
    new AdventureFailure("Better bundle up", "You need more cold resistance."),
    new AdventureFailure("extreme cold makes it impossible", "You need more cold resistance."),
    new AdventureFailure(
        "This zone is too old to visit on this path.", "That zone is out of Standard."),
  };

  private static Pattern CRIMBO21_COLD_RES =
      Pattern.compile("<b>\\[(\\d+) Cold Resistance Required\\]</b>");

  public static final int findAdventureFailure(String responseText) {
    // KoL is known to sometimes simply return a blank page as a
    // failure to adventure.
    if (responseText.length() == 0) {
      return 0;
    }

    // Sometimes we want to take some action even on a failure
    if (responseText.contains("There are no Hippy soldiers left")) {
      Preferences.setInteger("hippiesDefeated", 1000);
    } else if (responseText.contains("There are no Frat soldiers left")) {
      Preferences.setInteger("fratboysDefeated", 1000);
    } else if (responseText.contains("Drippy Juice supply")) {
      Preferences.setInteger("drippyJuice", 0);
    } else if (responseText.contains("Better bundle up")
        || responseText.contains("extreme cold makes it impossible")) {
      Matcher matcher = CRIMBO21_COLD_RES.matcher(responseText);
      if (matcher.find()) {
        int required = Integer.parseInt(matcher.group(1));
        Preferences.setInteger("_crimbo21ColdResistance", required);
      }
    }

    for (int i = 1; i < ADVENTURE_FAILURES.length; ++i) {
      if (responseText.contains(ADVENTURE_FAILURES[i].responseText)) {
        return i;
      }
    }

    return -1;
  }

  public static final String adventureFailureMessage(int index) {
    if (index >= 0 && index < ADVENTURE_FAILURES.length) {
      return ADVENTURE_FAILURES[index].message;
    }

    return null;
  }

  public static final MafiaState adventureFailureSeverity(int index) {
    if (index >= 0 && index < ADVENTURE_FAILURES.length) {
      return ADVENTURE_FAILURES[index].severity;
    }

    return MafiaState.ERROR;
  }

  public static final boolean recordToSession(final String urlString, final String responseText) {
    // This is the second half of logging an adventure location
    // after we've submitted the URL and gotten a response, after,
    // perhaps, being redirected. Given the old URL, the new URL,
    // and the response, we can often do a better job of figuring
    // out where we REALLY adventured - if anywhere.

    if (KoLmafia.isRefreshing()) {
      return true;
    }

    // Only do this once per adventure attempt.
    if (KoLAdventure.locationLogged) {
      return true;
    }

    String location = KoLAdventure.lastLocationName;
    if (location == null) {
      return false;
    }

    // Only do this once per adventure attempt.
    KoLAdventure.locationLogged = true;

    String lastURL = KoLAdventure.lastLocationURL;

    if (lastURL.equals("basement.php")) {
      return true;
    }

    // See if we've been redirected away from the URL that started
    // us adventuring

    if (urlString.equals(lastURL)) {
      // No. It is possible that we didn't adventure at all
      if (KoLAdventure.findAdventureFailure(responseText) >= 0) {
        return false;
      }

      // See if there is an "adventure again" link, and if
      // so, whether it points to where we thought we went.
      KoLAdventure again = KoLAdventure.findAdventureAgain(responseText);
      if (again != null && again != KoLAdventure.lastVisitedLocation) {
        location = again.getPrettyAdventureName(urlString);
        KoLAdventure.lastVisitedLocation = again;
        KoLAdventure.lastLocationName = location;
        KoLAdventure.lastLocationURL = urlString;
      }
    } else if (urlString.equals("cove.php")) {
      // Redirected from Pirate Cove to the cove map
      return false;
    } else if (urlString.startsWith("mining.php")) {
      // Redirected to a mine
      return false;
    } else if (urlString.startsWith("fight.php")) {
      // Redirected to a fight. We may or may not be
      // adventuring where we thought we were. If your
      // autoattack one-hit-kills the foe, the adventure
      // again link will tell us where you were.
      KoLAdventure again = KoLAdventure.findAdventureAgain(responseText);
      if (again != null && again != lastVisitedLocation) {
        location = again.adventureName;
        KoLAdventure.lastVisitedLocation = again;
        KoLAdventure.lastLocationName = location;
      }
    } else if (urlString.startsWith("choice.php")) {
      // Redirected to a choice. We may or may not be
      // adventuring where we thought we were.
    }

    // Customize location name, perhaps
    location = KoLAdventure.getPrettyAdventureName(location, responseText);

    // Update selected adventure information in order to
    // keep the GUI synchronized.

    KoLAdventure.setLastAdventure(KoLAdventure.lastVisitedLocation);
    KoLAdventure.setNextAdventure(KoLAdventure.lastVisitedLocation);
    KoLAdventure.registerAdventure();
    EncounterManager.registerAdventure(location);

    String limitmode = KoLCharacter.getLimitmode();
    String message = null;
    if (limitmode == Limitmode.SPELUNKY) {
      message = "{" + SpelunkyRequest.getTurnsLeft() + "} " + location;
    } else if (limitmode == Limitmode.BATMAN) {
      message = "{" + BatManager.getTimeLeftString() + "} " + location;
    } else {
      message = "[" + KoLAdventure.getAdventureCount() + "] " + location;
    }
    RequestLogger.printLine();
    RequestLogger.printLine(message);

    RequestLogger.updateSessionLog();
    RequestLogger.updateSessionLog(message);

    String encounter = "";

    if (urlString.startsWith("basement.php")) {
      encounter = BasementRequest.getBasementLevelSummary();
    }

    if (!encounter.isEmpty()) {
      RequestLogger.printLine(encounter);
      RequestLogger.updateSessionLog(encounter);
    }

    return true;
  }

  public static final void registerAdventure() {
    switch (KoLAdventure.lastAdventureId()) {
      case AdventurePool.THE_DRIPPING_TREES:
        Preferences.increment("dripAdventuresSinceAscension");
        Preferences.increment("drippingTreesAdventuresSinceAscension");
        Preferences.decrement("drippyJuice");
        break;
      case AdventurePool.THE_DRIPPING_HALL:
        Preferences.increment("dripAdventuresSinceAscension");
        Preferences.decrement("drippyJuice");
        break;
    }
  }

  public static final int getAdventureCount() {
    return Preferences.getBoolean("logReverseOrder")
        ? KoLCharacter.getAdventuresLeft()
        : KoLCharacter.getCurrentRun() + 1;
  }

  @Override
  public int compareTo(final KoLAdventure o) {
    if (!(o instanceof KoLAdventure)) {
      return 1;
    }

    // Put things with no evade rating at bottom of list.

    int evade1 = this.areaSummary == null ? Integer.MAX_VALUE : this.areaSummary.minEvade();
    int evade2 = o.areaSummary == null ? Integer.MAX_VALUE : o.areaSummary.minEvade();

    if (evade1 == evade2) {
      return this.adventureName.compareTo(o.adventureName);
    }

    return evade1 - evade2;
  }
}
