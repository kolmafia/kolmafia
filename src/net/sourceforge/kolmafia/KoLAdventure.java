package net.sourceforge.kolmafia;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
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
import net.sourceforge.kolmafia.request.FamiliarRequest;
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
    this.isValidAdventure = this.canAdventure();
  }

  // AdventureResults used during validation
  private static final AdventureResult KNOB_GOBLIN_PERFUME =
      ItemPool.get(ItemPool.KNOB_GOBLIN_PERFUME);
  private static final AdventureResult KNOB_CAKE = ItemPool.get(ItemPool.KNOB_CAKE);
  private static final AdventureResult TRANSFUNCTIONER = ItemPool.get(ItemPool.TRANSFUNCTIONER);
  private static final AdventureResult SONAR = ItemPool.get(ItemPool.SONAR);
  private static final AdventureResult TALISMAN = ItemPool.get(ItemPool.TALISMAN);
  private static final AdventureResult DINGY_DINGHY = ItemPool.get(ItemPool.DINGY_DINGHY);
  private static final AdventureResult DINGHY_PLANS = ItemPool.get(ItemPool.DINGHY_PLANS);
  private static final AdventureResult DINGY_PLANKS = ItemPool.get(ItemPool.DINGY_PLANKS);
  private static final AdventureResult ENCHANTED_BEAN = ItemPool.get(ItemPool.ENCHANTED_BEAN);
  private static final AdventureResult DRINK_ME_POTION = ItemPool.get(ItemPool.DRINK_ME_POTION);
  private static final AdventureResult DEVILISH_FOLIO = ItemPool.get(ItemPool.DEVILISH_FOLIO);
  private static final AdventureResult ASTRAL_MUSHROOM = ItemPool.get(ItemPool.ASTRAL_MUSHROOM);
  private static final AdventureResult TRANSPONDER = ItemPool.get(ItemPool.TRANSPORTER_TRANSPONDER);
  private static final AdventureResult PIRATE_FLEDGES = ItemPool.get(ItemPool.PIRATE_FLEDGES);
  private static final AdventureResult DRIP_HARNESS = ItemPool.get(ItemPool.DRIP_HARNESS, 1);
  private static final AdventureResult FANTASY_REALM_GEM = ItemPool.get(ItemPool.FANTASY_REALM_GEM);
  private static final AdventureResult BONE_WITH_A_PRICE_TAG =
      ItemPool.get(ItemPool.BONE_WITH_A_PRICE_TAG);
  private static final AdventureResult BOOZE_MAP = ItemPool.get(ItemPool.BOOZE_MAP);
  private static final AdventureResult HYPNOTIC_BREADCRUMBS =
      ItemPool.get(ItemPool.HYPNOTIC_BREADCRUMBS);

  private static final AdventureResult PERFUME = EffectPool.get(EffectPool.KNOB_GOBLIN_PERFUME, 1);
  private static final AdventureResult TROPICAL_CONTACT_HIGH =
      EffectPool.get(EffectPool.TROPICAL_CONTACT_HIGH);
  private static final AdventureResult DOWN_THE_RABBIT_HOLE =
      EffectPool.get(EffectPool.DOWN_THE_RABBIT_HOLE);
  private static final AdventureResult DIS_ABLED = EffectPool.get(EffectPool.DIS_ABLED);
  private static final AdventureResult HALF_ASTRAL = EffectPool.get(EffectPool.HALF_ASTRAL);
  private static final AdventureResult SHAPE_OF_MOLE = EffectPool.get(EffectPool.SHAPE_OF_MOLE);
  private static final AdventureResult FORM_OF_BIRD = EffectPool.get(EffectPool.FORM_OF_BIRD);
  private static final AdventureResult ABSINTHE_MINDED = EffectPool.get(EffectPool.ABSINTHE);
  private static final AdventureResult TRANSPONDENT = EffectPool.get(EffectPool.TRANSPONDENT);
  private static final AdventureResult FILTHWORM_LARVA_STENCH =
      EffectPool.get(EffectPool.FILTHWORM_LARVA_STENCH);
  private static final AdventureResult FILTHWORM_DRONE_STENCH =
      EffectPool.get(EffectPool.FILTHWORM_DRONE_STENCH);
  private static final AdventureResult FILTHWORM_GUARD_STENCH =
      EffectPool.get(EffectPool.FILTHWORM_GUARD_STENCH);

  private static final Set<String> antiqueMapZones = new HashSet<>();
  private static final Set<String> psychosesZones = new HashSet<>();
  private static final Set<String> batfellowZones = new HashSet<>();
  private static final Map<String, String> grimstoneZones = new HashMap<>();
  private static final Map<String, String> holidayAdventures = new HashMap<>();

  static {
    antiqueMapZones.add("Landscaper");
    antiqueMapZones.add("Jacking");
    antiqueMapZones.add("Vanya's Castle");
    antiqueMapZones.add("Kegger");
    antiqueMapZones.add("Magic Commune");
    antiqueMapZones.add("Ellsbury's Claim");
    psychosesZones.add("The Crackpot Mystic's Psychoses");
    psychosesZones.add("The Meatsmith's Brainspace");
    psychosesZones.add("The Pretentious Artist's Obsession");
    psychosesZones.add("The Suspicious-Looking Guy's Shady Past");
    psychosesZones.add("The Captain of the Gourd's Psychoses");
    psychosesZones.add("Jick's Obsessions");
    psychosesZones.add("The Old Man's Past");
    batfellowZones.add("Bat-Cavern");
    batfellowZones.add("Center Park (Low Crime)");
    batfellowZones.add("Slums (Moderate Crime)");
    batfellowZones.add("Industrial District (High Crime)");
    batfellowZones.add("Downtown");
    grimstoneZones.put("A Deserted Stretch of I-911", "hare");
    grimstoneZones.put("Skid Row", "wolf");
    grimstoneZones.put("The Prince's Ball", "stepmother");
    grimstoneZones.put("Rumpelstiltskin's Home For Children", "gnome");
    grimstoneZones.put("The Candy Witch and the Relentless Child Thieves", "witch");
    holidayAdventures.put("St. Sneaky Pete's Day Stupor", "St. Sneaky Pete's Day");
    holidayAdventures.put("The Yuletide Bonfire", "Yuletide");
    holidayAdventures.put("The Arrrboretum", "Arrrbor Day");
    holidayAdventures.put("Generic Summer Holiday Swimming!", "Generic Summer Holiday");
    holidayAdventures.put("Drunken Stupor", null);
    holidayAdventures.put("The Spectral Pickle Factory", null);
    holidayAdventures.put("Spectral Salad Factory", null);
  }

  public boolean canAdventure() {
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

    // Some zones/areas are available via items.
    AdventureResult item = AdventureDatabase.zoneGeneratingItem(this.zone);
    // If it is from an item, Standard restrictions may apply.
    if (item != null
        && KoLCharacter.getRestricted()
        && !StandardRequest.isAllowed("Items", item.getName())) {
      return false;
    }

    // Further validation of individual zones happens below.

    // First check non-adventure.php zones.

    // Level 3 quest
    if (this.formSource.equals("cellar.php")) {
      return QuestDatabase.isQuestStarted(Quest.RAT);
    }

    // Level 5 quest boss
    if (this.formSource.equals("cobbsknob.php")) {
      return QuestDatabase.isQuestLaterThan(Quest.GOBLIN, QuestDatabase.STARTED)
          && !QuestDatabase.isQuestFinished(Quest.GOBLIN);
    }

    // Level 7 quest boss
    if (this.formSource.equals("crypt.php")) {
      return QuestDatabase.isQuestStep(Quest.CYRPT, QuestDatabase.STARTED)
          && Preferences.getInteger("cyrptTotalEvilness") == 0;
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
      return QuestDatabase.isQuestLaterThan(Quest.EGO, "step4");
    }

    // The Tunnel of L.O.V.E.
    if (this.adventureId.equals(AdventurePool.TUNNEL_OF_LOVE_ID)) {
      // LOV Entrance Pass is the one-day pass.
      // *** Not supported yet!
      return Preferences.getBoolean("loveTunnelAvailable")
          && !Preferences.getBoolean("_loveTunnelUsed");
    }

    // The Barrel Full of Barrels
    if (this.adventureId.equals(AdventurePool.BARREL_ID)) {
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
    if (!this.formSource.startsWith("adventure.php")) {
      return true;
    }

    // Top-level map areas.

    // Open at level one, with a subset of eventual zones
    if (this.zone.equals("Town")) {
      return switch (this.adventureNumber) {
        case AdventurePool.SLEAZY_BACK_ALLEY -> true;
          // We can start the three market quests, if necessary
        case AdventurePool.SKELETON_STORE -> true;
        case AdventurePool.MADNESS_BAKERY -> true;
        case AdventurePool.OVERGROWN_LOT -> true;
          // Shen is available once you've read the diary and been told to talk to him.
        case AdventurePool.COPPERHEAD_CLUB -> QuestDatabase.isQuestStarted(Quest.SHEN);
          // Only one of the four Lair locations is in Town; two are in the
          // Mountains and one is in the Plains. But, we sorted it into Town...
        case AdventurePool.SUPER_VILLAIN_LAIR -> KoLCharacter.getPath()
            == Path.LICENSE_TO_ADVENTURE;
          // Allow future "Town" zones
        default -> true;
      };
    }

    // Open at level one, with a subset of eventual zones
    if (this.zone.equals("Mountain")) {
      return switch (this.adventureNumber) {
        case AdventurePool.NOOB_CAVE, AdventurePool.DIRE_WARREN -> true;
          // The Smut Orc Logging Camp is available if you have started the Highlands quest
        case AdventurePool.SMUT_ORC_LOGGING_CAMP -> QuestDatabase.isQuestStarted(Quest.TOPPING);
          // The Valley of Rof L'm Fao is available if you have completed the Highlands quest
        case AdventurePool.VALLEY_OF_ROF_LM_FAO -> QuestDatabase.isQuestFinished(Quest.TOPPING);
          // The Thinknerd Warehouse: Unlocks when reading (receiving?) Letter for Melvign the Gnome
        case AdventurePool.THINKNERD_WAREHOUSE -> QuestDatabase.isQuestStarted(Quest.SHIRT);
          // The Secret Council Warehouse is near the end of an Ed the Undying run
        case AdventurePool.SECRET_COUNCIL_WAREHOUSE -> KoLCharacter.isEd()
            && KoLCharacter.getLevel() >= 13;
          // Allow future "Mountain" zones
        default -> true;
      };
    }

    // Open at level one, with a subset of eventual zones
    if (this.zone.equals("Plains")) {
      return switch (this.adventureNumber) {
        case AdventurePool.FUN_HOUSE -> QuestDatabase.isQuestLaterThan(Quest.NEMESIS, "step4");
        case AdventurePool.UNQUIET_GARVES -> QuestDatabase.isQuestStarted(Quest.CYRPT)
            || QuestDatabase.isQuestStarted(Quest.EGO);
        case AdventurePool.VERY_UNQUIET_GARVES -> QuestDatabase.isQuestFinished(Quest.CYRPT);
        case AdventurePool.PALINDOME -> KoLCharacter.hasEquipped(TALISMAN)
            || InventoryManager.hasItem(TALISMAN);
          // Allow future "Plains" zones
        default -> true;
      };
    }

    // Opens at level two with first council quest
    if (this.zone.equals("Woods")) {
      // With the exception of a few challenge paths, the Woods open when
      // the Council asks for a mosquito larva at level two.
      return switch (this.adventureNumber) {
        case AdventurePool.BARROOM_BRAWL -> QuestDatabase.isQuestStarted(Quest.RAT);
          // validate2 will visit the Crackpot Mystic to get one, if needed
        case AdventurePool.PIXEL_REALM -> QuestDatabase.isQuestStarted(Quest.LARVA)
            || InventoryManager.hasItem(TRANSFUNCTIONER);
        case AdventurePool.HIDDEN_TEMPLE -> KoLCharacter.isKingdomOfExploathing()
            || KoLCharacter.getTempleUnlocked();
        case AdventurePool.WHITEYS_GROVE -> KoLCharacter.isEd()
            || QuestDatabase.isQuestStarted(Quest.CITADEL)
            || QuestDatabase.isQuestLaterThan(Quest.PALINDOME, "step2");
        case AdventurePool.BLACK_FOREST -> QuestDatabase.isQuestStarted(Quest.MACGUFFIN);
        case AdventurePool.ROAD_TO_WHITE_CITADEL -> QuestDatabase.isQuestLaterThan(
            Quest.CITADEL, QuestDatabase.STARTED);
        case AdventurePool.OLD_LANDFILL -> QuestDatabase.isQuestStarted(Quest.HIPPY);
          // Allow future "Woods" zones
        default -> QuestDatabase.isQuestStarted(Quest.LARVA);
      };
    }

    // Opens when you build a bitchin' Meat car or the equivalent
    if (this.zone.equals("Beach")) {
      if (!KoLCharacter.desertBeachAccessible()) {
        return false;
      }
      return switch (this.adventureNumber) {
        case AdventurePool.THE_SHORE, AdventurePool.SOUTH_OF_THE_BORDER -> true;
          // Open after diary read
        case AdventurePool.ARID_DESERT -> QuestDatabase.isQuestStarted(Quest.DESERT);
          // Open after 1 desert exploration or - legacy - desert quest is finished
        case AdventurePool.OASIS -> QuestDatabase.isQuestStarted(Quest.DESERT)
            && (Preferences.getInteger("desertExploration") > 0
                || QuestDatabase.isQuestFinished(Quest.DESERT));
          // Open with "Tropical Contact High"
        case AdventurePool.KOKOMO_RESORT -> KoLConstants.activeEffects.contains(
            TROPICAL_CONTACT_HIGH);
          // Allow future "Beach" zones
        default -> true;
      };
    }

    // Opens when you build a dingy dinghy or the equivalent
    if (this.zone.equals("Island")) {
      // There are several ways to get to the island.
      if (!KoLCharacter.mysteriousIslandAccessible() && !canBuildDinghy()) {
        return false;
      }

      // We have a way to get to the island.  Access to individual zones
      // depends on quest state and outfits

      String winner = IslandManager.warWinner();
      // neither, hippies, fratboys

      return switch (this.adventureNumber) {
          // You cannot visit the pirates during the war
        case AdventurePool.PIRATE_COVE -> !QuestDatabase.isQuestStep(Quest.ISLAND_WAR, "step1");

          // You can visit the hippy camp before or after the war, unless it
          // has been bombed into the stone age.
        case AdventurePool.HIPPY_CAMP, AdventurePool.HIPPY_CAMP_DISGUISED -> QuestDatabase
                .isQuestBefore(Quest.ISLAND_WAR, "step1")
            || (QuestDatabase.isQuestFinished(Quest.ISLAND_WAR) && winner.equals("hippies"));

          // You can visit the frat house before or after the war, unless it
          // has been bombed into the stone age.
        case AdventurePool.FRAT_HOUSE, AdventurePool.FRAT_HOUSE_DISGUISED -> QuestDatabase
                .isQuestBefore(Quest.ISLAND_WAR, "step1")
            || (QuestDatabase.isQuestFinished(Quest.ISLAND_WAR) && winner.equals("fratboys"));

        case AdventurePool.BOMBED_HIPPY_CAMP -> QuestDatabase.isQuestFinished(Quest.ISLAND_WAR)
            && (winner.equals("neither") || winner.equals("fratboys"));

        case AdventurePool.BOMBED_FRAT_HOUSE -> QuestDatabase.isQuestFinished(Quest.ISLAND_WAR)
            && (winner.equals("neither") || winner.equals("hippies"));

          // Sonofa Beach is available during the war as a sidequest and also
          // after the war, whether or not it was used as such.
        case AdventurePool.SONOFA_BEACH -> QuestDatabase.isQuestLaterThan(
            Quest.ISLAND_WAR, QuestDatabase.STARTED);

          // The Junkyard and the Farm are sidequest zones during the war, but
          // are available as single adventuring areas after the war is done.
        case AdventurePool.THE_JUNKYARD, AdventurePool.MCMILLICANCUDDYS_FARM -> QuestDatabase
            .isQuestFinished(Quest.ISLAND_WAR);

          // Allow future "Island" zones
        default -> true;
      };
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

            return InventoryManager.hasItem(SONAR.getInstance(sonarsToUse));
          }
      }
      return false;
    }

    // Level 5 quest
    if (this.zone.equals("Knob")) {
      return switch (this.adventureNumber) {
        case AdventurePool.OUTSKIRTS_OF_THE_KNOB -> true;
        case AdventurePool.COBB_BARRACKS,
            AdventurePool.COBB_KITCHEN,
            AdventurePool.COBB_HAREM,
            AdventurePool.COBB_TREASURY -> QuestDatabase.isQuestLaterThan(
            Quest.GOBLIN, QuestDatabase.STARTED);
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
      return switch (this.adventureNumber) {
          // The beanstalk is unlocked when the player has planted a
          // beanstalk -- but, the bean needs to be planted first.
          // We will plant in validate2, if necessary
        case AdventurePool.AIRSHIP -> !KoLCharacter.isKingdomOfExploathing()
            && (QuestDatabase.isQuestLaterThan(Quest.GARBAGE, QuestDatabase.STARTED)
                || (QuestDatabase.isQuestStarted(Quest.GARBAGE)
                    && InventoryManager.hasItem(ENCHANTED_BEAN)));
          // The Castle Basement is unlocked provided the player has the S.O.C.K
          // (legacy: rowboats give access but are no longer creatable)
        case AdventurePool.CASTLE_BASEMENT -> InventoryManager.hasItem(
                ItemPool.get(ItemPool.SOCK, 1))
            || InventoryManager.hasItem(ItemPool.get(ItemPool.ROWBOAT, 1))
            || KoLCharacter.isKingdomOfExploathing();
        case AdventurePool.CASTLE_GROUND -> Preferences.getInteger("lastCastleGroundUnlock")
            == KoLCharacter.getAscensions();
        case AdventurePool.CASTLE_TOP -> Preferences.getInteger("lastCastleTopUnlock")
            == KoLCharacter.getAscensions();
          // The Hole in the Sky is unlocked provided the player has a steam-powered rocketship
          // (legacy: rowboats give access but are no longer creatable)
        case AdventurePool.HOLE_IN_THE_SKY -> KoLCharacter.isKingdomOfExploathing()
            || InventoryManager.hasItem(ItemPool.get(ItemPool.ROCKETSHIP, 1))
            || InventoryManager.hasItem(ItemPool.get(ItemPool.ROWBOAT, 1));
        default -> false;
      };
    }

    // Level 11 quest

    // *** Lord Spookyraven

    if (this.zone.equals("Manor0")) {
      return QuestDatabase.isQuestStarted(Quest.MANOR);
    }

    // *** Doctor Awkward

    if (this.zone.equals("The Red Zeppelin's Mooring")) {
      return switch (this.adventureNumber) {
        case AdventurePool.ZEPPELIN_PROTESTORS -> QuestDatabase.isQuestStarted(Quest.RON);
        case AdventurePool.RED_ZEPPELIN -> QuestDatabase.isQuestLaterThan(Quest.RON, "step1");
        default -> false;
      };
    }

    // *** Protector Spectre

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
      if (!KoLCharacter.mysteriousIslandAccessible() && !canBuildDinghy()) {
        return false;
      }

      if (!QuestDatabase.isQuestStarted(Quest.ISLAND_WAR)) {
        return false;
      }

      // Quest.ISLAND_WAR progresses from "unstarted" -> "started" -> "step1" -> "finished"
      // "unstarted" is the peaceful Mysterious Island
      // "started" is the Verge of War on the Mysterious Island
      // "step1" is the actual war on the Big Island
      // "finished" is the peaceful Big Island

      return switch (this.adventureNumber) {
        case AdventurePool.WARTIME_FRAT_HOUSE,
            AdventurePool.WARTIME_FRAT_HOUSE_DISGUISED,
            AdventurePool.WARTIME_HIPPY_CAMP,
            AdventurePool.WARTIME_HIPPY_CAMP_DISGUISED -> QuestDatabase.isQuestBefore(
            Quest.ISLAND_WAR, "step1");

        case AdventurePool.FRAT_UNIFORM_BATTLEFIELD,
            AdventurePool.HIPPY_UNIFORM_BATTLEFIELD -> QuestDatabase.isQuestStep(
            Quest.ISLAND_WAR, "step1");

          // available during the war. After the war, you can visit the Nunnery.
        case AdventurePool.THEMTHAR_HILLS -> QuestDatabase.isQuestStep(Quest.ISLAND_WAR, "step1");
        default -> false;
      };
    }

    if (this.zone.equals("Farm")) {
      if (!QuestDatabase.isQuestStep(Quest.ISLAND_WAR, "step1")) {
        return false;
      }
      // McMillicancuddy's Barn
      // McMillicancuddy's Pond
      // McMillicancuddy's Back 40
      // McMillicancuddy's Other Back 40
      // McMillicancuddy's Granary
      // McMillicancuddy's Bog
      // McMillicancuddy's Family Plot
      // McMillicancuddy's Shady Thicket

      // *** validate:
      // - only  three duck zones that were selected in the barn
      // - done when all three duck zones have been cleared
      return true;
    }

    if (this.zone.equals("Orchard")) {
      if (!QuestDatabase.isQuestStep(Quest.ISLAND_WAR, "step1")) {
        return false;
      }

      // Once Filthworm Queen is defeated none of the zones can be accessed
      if (InventoryManager.hasItem(ItemPool.FILTHWORM_QUEEN_HEART)
          || !Preferences.getString("sidequestOrchardCompleted").equals("none")) {
        return false;
      }

      return switch (this.adventureNumber) {
        case AdventurePool.FILTHWORM_HATCHING_CHAMBER -> true;
        case AdventurePool.FILTHWORM_FEEDING_CHAMBER -> KoLConstants.activeEffects.contains(
                FILTHWORM_LARVA_STENCH)
            || InventoryManager.hasItem(ItemPool.FILTHWORM_HATCHLING_GLAND);
        case AdventurePool.FILTHWORM_GUARDS_CHAMBER -> KoLConstants.activeEffects.contains(
                FILTHWORM_DRONE_STENCH)
            || InventoryManager.hasItem(ItemPool.FILTHWORM_DRONE_GLAND);
        case AdventurePool.FILTHWORM_QUEENS_CHAMBER -> KoLConstants.activeEffects.contains(
                FILTHWORM_GUARD_STENCH)
            || InventoryManager.hasItem(ItemPool.FILTHWORM_GUARD_GLAND);
        default -> false;
      };
    }

    if (this.zone.equals("Junkyard")) {
      if (!QuestDatabase.isQuestStep(Quest.ISLAND_WAR, "step1")) {
        return false;
      }

      // Next to that Barrel with Something Burning in it
      // Near an Abandoned Refrigerator
      // Over Where the Old Tires Are
      // Out by that Rusted-Out Car

      // You can visit any of the zones both before or after getting the tool -
      // and even after turning them all in.
      return true;
    }

    // Spookyraven Manor quests:
    //
    // Quest.SPOOKYRAVEN_NECKLACE	Lady Spookyraven
    // Quest.SPOOKYRAVEN_DANCE		Lady Spookyraven
    // Quest.SPOOKYRAVEN_BABIES		Lady Spookyraven

    if (this.zone.equals("Manor1")) {
      return switch (this.adventureNumber) {
        case AdventurePool.HAUNTED_KITCHEN, AdventurePool.HAUNTED_CONSERVATORY -> QuestDatabase
            .isQuestStarted(Quest.SPOOKYRAVEN_NECKLACE);
        case AdventurePool.HAUNTED_LIBRARY -> InventoryManager.hasItem(ItemPool.LIBRARY_KEY);
        case AdventurePool.HAUNTED_BILLIARDS_ROOM -> InventoryManager.hasItem(
            ItemPool.BILLIARDS_KEY);
        default -> true;
      };
    }

    if (this.zone.equals("Manor2")) {
      return switch (this.adventureNumber) {
        case AdventurePool.HAUNTED_BATHROOM,
            AdventurePool.HAUNTED_BEDROOM,
            AdventurePool.HAUNTED_GALLERY -> QuestDatabase.isQuestLaterThan(
            Quest.SPOOKYRAVEN_DANCE, QuestDatabase.STARTED);
        case AdventurePool.HAUNTED_BALLROOM -> QuestDatabase.isQuestLaterThan(
            Quest.SPOOKYRAVEN_DANCE, "step2");
        default -> true;
      };
    }

    if (this.zone.equals("Manor3")) {
      return switch (this.adventureNumber) {
        case AdventurePool.HAUNTED_LABORATORY,
            AdventurePool.HAUNTED_NURSERY,
            AdventurePool.HAUNTED_STORAGE_ROOM -> QuestDatabase.isQuestLaterThan(
            Quest.SPOOKYRAVEN_DANCE, "step3");
        default -> true;
      };
    }

    // Nemesis Quest

    if (this.zone.equals("Nemesis Cave")) {
      return switch (this.adventureNumber) {
        case AdventurePool.FUNGAL_NETHERS -> QuestDatabase.isQuestLaterThan(
            Quest.NEMESIS, "step11");
        default -> true;
      };
    }

    if (this.zone.equals("Volcano")) {
      if (QuestDatabase.isQuestBefore(Quest.NEMESIS, "step25")) {
        return false;
      }

      return switch (this.adventureNumber) {
        case AdventurePool.BROODLING_GROUNDS -> KoLCharacter.isSealClubber();
        case AdventurePool.OUTER_COMPOUND -> KoLCharacter.isTurtleTamer();
        case AdventurePool.TEMPLE_PORTICO -> KoLCharacter.isPastamancer();
        case AdventurePool.CONVENTION_HALL_LOBBY -> KoLCharacter.isSauceror();
        case AdventurePool.OUTSIDE_THE_CLUB -> KoLCharacter.isDiscoBandit();
        case AdventurePool.ISLAND_BARRACKS -> KoLCharacter.isAccordionThief();
          // You can always try to get in, but will be rebuffed if not ready
        case AdventurePool.NEMESIS_LAIR -> true;
        default -> true;
      };
    }

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

      return switch (this.adventureNumber) {
        case AdventurePool.BUGBEAR_PEN -> QuestDatabase.isQuestStarted(Quest.BUGBEAR)
            && !QuestDatabase.isQuestFinished(Quest.BUGBEAR);
        case AdventurePool.SPOOKY_GRAVY_BURROW -> QuestDatabase.isQuestLaterThan(
            Quest.BUGBEAR, "step1");
        case AdventurePool.POST_QUEST_BUGBEAR_PEN -> QuestDatabase.isQuestFinished(Quest.BUGBEAR);
        default -> true;
      };
    }

    if (this.zone.equals("Little Canadia")) {
      return KoLCharacter.getSignZone() == ZodiacZone.CANADIA;
    }

    if (this.zone.equals("Le Marais D&egrave;gueulasse")) {
      // This is a subzone of Little Canadia
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

    // Pirate Ship
    if (this.zone.equals("Pirate")) {
      // There are several ways to get to the island.
      if (!KoLCharacter.mysteriousIslandAccessible() && !canBuildDinghy()) {
        return false;
      }

      // However, the pirates are unavailable during the war.
      if (QuestDatabase.isQuestStep(Quest.ISLAND_WAR, "step1")) {
        return false;
      }

      boolean haveOutfit = EquipmentManager.hasOutfit(OutfitPool.SWASHBUCKLING_GETUP);
      boolean haveFledges =
          EquipmentManager.canEquip(PIRATE_FLEDGES)
              && InventoryManager.getAccessibleCount(PIRATE_FLEDGES) > 0;
      if (!haveOutfit && !haveFledges) {
        return false;
      }
      return switch (this.adventureNumber) {
        case AdventurePool.BARRRNEYS_BARRR -> true;
        case AdventurePool.FCLE -> QuestDatabase.isQuestLaterThan(Quest.PIRATE, "step4");
        case AdventurePool.POOP_DECK -> QuestDatabase.isQuestLaterThan(Quest.PIRATE, "step5");
        case AdventurePool.BELOWDECKS -> QuestDatabase.isQuestFinished(Quest.PIRATE);
        default -> false;
      };
    }

    if (this.zone.equals("Dungeon")) {
      return switch (this.adventureNumber) {
        case AdventurePool.LIMERICK_DUNGEON -> KoLCharacter.getBaseMainstat() >= 19;
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

    if (this.zone.equals("The Sea")) {
      // The Sea opens when you speak to The Old Man
      if (!QuestDatabase.isQuestStarted(Quest.SEA_OLD_GUY)) {
        return false;
      }

      // We track individual aspects of the various quests.
      // Perhaps we could deduce what is and is not unlocked.
      // Just assume everything is open, for now.

      // The Briny Deeps
      // The Brinier Deepers
      // The Briniest Deepests
      // An Octopus's Garden
      // The Wreck of the Edgar Fitzsimmons
      // Madness Reef
      // The Mer-Kin Outpost
      // The Skate Park
      // The Marinara Trench
      // Anemone Mine
      // The Dive Bar
      // The Coral Corral
      // Mer-kin Elementary School
      // Mer-kin Library
      // Mer-kin Gymnasium
      // Mer-kin Colosseum
      // The Caliginous Abyss
      // Anemone Mine (Mining)

      return true;
    }

    // The following zones depend on your clan, your permissions, and the
    // current state of clan dungeons. We're not going to touch them here.
    if (zone.equals("Clan Basement") || zone.equals("Hobopolis") || zone.equals("Dreadsylvania")) {
      return true;
    }

    // Holiday zones
    if (holidayAdventures.containsKey(this.zone)) {
      // Holiday zones include "Drunken Stupor" and "St. Sneaky Pete's Day Stupor"
      // Holiday zones include "The Spectral Pickle Factory!" and "Spectral Salad Factory!"
      String holiday = holidayAdventures.get(this.adventureName);
      if (holiday == null) {
        // I dunno.
        return true;
      }
      return HolidayDatabase.getHoliday().contains(holiday);
    }

    if (this.zone.equals("Twitch")) {
      // This gets set if we visit town and see "town_tower"
      return Preferences.getBoolean("timeTowerAvailable");
    }

    // *** Validate path-restricted zones
    // We have already rejected zones that do not match your path.

    if (this.zone.equals("BadMoon")) {
      return KoLCharacter.inBadMoon();
    }

    if (this.zone.equals("Mothership")) {
      // We already confirmed that you are on the BugBear Invasion path.

      // There are conditions by which the various areas become available.
      // Do we track them?

      // Medbay
      // Waste Processing
      // Sonar
      // Science Lab
      // Morgue
      // Special Ops
      // Engineering
      // Navigation
      // Galley

      // Assume those zones are always available.
      return true;
    }

    if (this.zone.equals("KOL High School")) {
      // We already confirmed that you are on the KOLHS path.

      // The Hallowed Halls
      // Shop Class
      // Chemistry Class
      // Art Class

      // All of those zones are always available.
      return true;
    }

    if (this.zone.equals("Exploathing")) {
      // We already confirmed that you are on the Kingdom of Exploathing path.

      // The Exploaded Battlefield
      // The Invader

      // Assume those zones are always available.
      return true;
    }

    if (this.zone.equals("The Grey Goo Impact Site")) {
      // We already confirmed that you are on the Grey Goo path.

      // The Goo Fields
      // The Goo-Choked Fun House
      // The Goo-Coated Knob
      // The Goo-Spewing Bat Hole
      // The Goo-Bedecked Beanstalk
      // The Goo-Shrouded Palindome
      // The Goo-Girded Garves
      // The Goo-Splattered Tower Ruins

      // Assume those zones are always available.
      return true;
    }

    // The rest of this method validates item-generated zones.
    // We have already rejected items restricted by Standard.

    if (item == null) {
      // Assume that any non-item areas we did validate are available
      return true;
    }

    if (this.zone.equals("Spelunky Area")) {
      // No Avatar of Boris or Ed
      // *** LimitMode
      return true;
    }

    if (batfellowZones.contains(this.zone)) {
      // *** LimitMode
      return true;
    }

    if (this.zone.equals("Astral")) {
      // astral mushroom grants 5 turns of Half-Astral
      // You can choose the type of trip to take.
      // You cannot adventure anywhere else until it expires.
      //
      // An Incredibly Strange Place (Bad Trip)
      // An Incredibly Strange Place (Mediocre Trip)
      // An Incredibly Strange Place (Great Trip)
      //
      // *** This should be a LimitMode

      return InventoryManager.hasItem(item) || KoLConstants.activeEffects.contains(HALF_ASTRAL);
    }

    if (this.zone.equals("Shape of Mole")) {
      // llama lama gong lets you choose to be a mole.
      // This grants 12 turns of Shape of...Mole!
      // You cannot use another gong if you are in Form of...Bird!
      // You cannot adventure anywhere except in Mt. Molehill while that effect is active.
      //
      // *** This should be a LimitMode
      return KoLConstants.activeEffects.contains(SHAPE_OF_MOLE)
          || (InventoryManager.hasItem(item) && !KoLConstants.activeEffects.contains(FORM_OF_BIRD));
    }

    if (this.zone.equals("Spring Break Beach")) {
      // Unlimited adventuring if available
      return Preferences.getBoolean("sleazeAirportAlways")
          || Preferences.getBoolean("_sleazeAirportToday");
    }

    if (this.zone.equals("Conspiracy Island")) {
      // Unlimited adventuring if available
      return Preferences.getBoolean("spookyAirportAlways")
          || Preferences.getBoolean("_spookyAirportToday");
    }

    if (this.zone.equals("Dinseylandfill")) {
      // Unlimited adventuring if available
      return Preferences.getBoolean("stenchAirportAlways")
          || Preferences.getBoolean("_stenchAirportToday");
    }

    if (this.zone.equals("That 70s Volcano")) {
      // Unlimited adventuring if available
      return Preferences.getBoolean("hotAirportAlways")
          || Preferences.getBoolean("_hotAirportToday");
    }

    if (this.zone.equals("The Glaciest")) {
      // Unlimited adventuring if available
      return Preferences.getBoolean("coldAirportAlways")
          || Preferences.getBoolean("_coldAirportToday");
    }

    if (this.zone.equals("Video Game Dungeon")) {
      // If you have a GameInformPowerDailyPro walkthru in inventory, you
      // have (or had) access to The GameInformPowerDailyPro Dungeon.
      // *** Do we track your progress?
      return InventoryManager.hasItem(item);
    }

    if (this.zone.equals("Rabbit Hole")) {
      // A "DRINK ME" potion grants 20 turns of Down the Rabbit Hole.
      // Having the item or the effect will suffice.
      return InventoryManager.hasItem(item)
          || KoLConstants.activeEffects.contains(DOWN_THE_RABBIT_HOLE);
    }

    if (this.zone.equals("Suburbs")) {
      // devilish folio grants 30 turns of Dis Abled.
      // Having the item or the effect will suffice.
      return InventoryManager.hasItem(item) || KoLConstants.activeEffects.contains(DIS_ABLED);
    }

    if (this.zone.equals("Portal")) {
      // El Vibrato Island
      // *** validate
      return true;
    }

    if (this.zone.equals("Deep Machine Tunnels")) {
      // The Deep Machine Tunnels
      // Deep Machine Tunnels snowglobe is the one day pass.
      // *** Not supported yet!
      return KoLCharacter.findFamiliar(FamiliarPool.MACHINE_ELF) != null;
    }

    if (this.zone.equals("LT&T")) {
      // Telegram quests
      // inflatable LT&T telegraph office is the one-day pass.
      // *** Not supported yet!
      return Preferences.getBoolean("telegraphOfficeAvailable");
    }

    if (this.zone.equals("Neverending Party")) {
      // The Neverending Party
      return Preferences.getBoolean("neverendingPartyAlways")
          || Preferences.getBoolean("_neverendingPartyToday");
    }

    if (this.zone.equals("Wormwood")) {
      // tiny bottle of absinthe grants 15 turns of Absinthe-Minded.
      // This opens up the Wormwood.
      // You can choose to adventure there or not.
      return InventoryManager.hasItem(item) || KoLConstants.activeEffects.contains(ABSINTHE_MINDED);
    }

    if (this.zone.equals("Memories")) {
      // You can gaze into an empty agua de vida bottle
      // and access the zones within
      return InventoryManager.hasItem(item);
    }

    if (this.zone.equals("Spaaace")) {
      // transporter transponder grants 30 turns of Transpondent.
      // Having the item or the effect will suffice.
      return InventoryManager.hasItem(item) || KoLConstants.activeEffects.contains(TRANSPONDENT);
    }

    if (antiqueMapZones.contains(this.zone)) {
      // The maps are all quest items. Therefore, you can't ascend and
      // keep them, but you can use them as much as you want until then.
      return InventoryManager.hasItem(item);
    }

    // You can only have one jar of psychoses open at once.
    // Each type of jar gives you access to a zone
    // There is a quest associated with each.
    // After you finish, you can no longer adventure there.
    // We do not track those quests.
    if (psychosesZones.contains(this.zone)) {
      // AdventureResult item - the item needed to activate this zone
      // That item is in the Campground if it is currently active
      // _psychoJarUsed - if a psycho jar is in use
      return KoLConstants.campground.contains(item) || InventoryManager.hasItem(item);
    }

    // You can only use one grimstone mask in use at a time.
    if (grimstoneZones.containsKey(this.zone)) {
      if (KoLCharacter.isEd()
          || KoLCharacter.inDarkGyffte()
          || KoLCharacter.isSneakyPete()
          || KoLCharacter.isWestOfLoathing()) {
        return false;
      }

      // One path at a time.
      String tale = grimstoneZones.get(this.zone);
      String current = Preferences.getString("grimstoneMaskPath");
      return tale.equals(current) || InventoryManager.hasItem(item);
    }

    if (this.zone.equals("The Snojo")) {
      // The X-32-F Combat Training Snowman
      return Preferences.getBoolean("snojoAvailable");
    }

    if (this.zone.equals("The Spacegate")) {
      // Through the Spacegate
      if (KoLCharacter.isKingdomOfExploathing()) {
        return false;
      }

      return (Preferences.getBoolean("spacegateAlways")
              || Preferences.getBoolean("_spacegateToday"))
          && !Preferences.getString("_spacegateCoordinates").isBlank()
          && Preferences.getInteger("_spacegateTurnsLeft") > 0;
    }

    if (this.zone.equals("Gingerbread City")) {
      // *** You have limited turns available per day.
      return Preferences.getBoolean("gingerbreadCityAvailable")
          || Preferences.getBoolean("_gingerbreadCityToday");
    }

    if (this.zone.equals("FantasyRealm")) {
      if (!Preferences.getBoolean("frAlways") && !Preferences.getBoolean("_frToday")) {
        return false;
      }

      if (Preferences.getInteger("_frHoursLeft") < 1) return false;

      return (Preferences.getString("_frAreasUnlocked").contains(this.adventureName));
    }

    if (this.zone.startsWith("PirateRealm")) {
      // *** You have limited turns available per day.
      return Preferences.getBoolean("prAlways") || Preferences.getBoolean("_prToday");
    }

    // Assume that any areas we did not call out above are available
    return true;
  }

  // Building a dingy dinghy is something that validate2 can do for us.
  private boolean canBuildDinghy() {
    return InventoryManager.hasItem(DINGHY_PLANS) && InventoryManager.hasItem(DINGY_PLANKS);
  }

  private boolean buildDinghy() {
    if (!KoLCharacter.mysteriousIslandAccessible()) {
      // There are other ways to get there, subsumed in the above
      // If we got here, we have the plans and planks
      RequestThread.postRequest(UseItemRequest.getInstance(ItemPool.DINGHY_PLANS));
    }
    return KoLCharacter.mysteriousIslandAccessible();
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
    this.isValidAdventure = this.prepareForAdventure();
  }

  public boolean prepareForAdventure() {
    // If we get here, this.isValidAdventure is true.

    if (this.zone.equals("Astral")) {
      // To take a trip to the Astral Plane, you either need
      // to be Half-Astral or have access to an astral mushroom.
      // validate1 ensured that one or the other is true

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

      if (KoLConstants.activeEffects.contains(HALF_ASTRAL)) {
        return true;
      }

      if (!InventoryManager.retrieveItem(ASTRAL_MUSHROOM)) {
        // This shouldn't fail.
        return false;
      }

      RequestThread.postRequest(UseItemRequest.getInstance(ASTRAL_MUSHROOM));

      // This shouldn't fail.
      return KoLConstants.activeEffects.contains(HALF_ASTRAL);
    }

    // Fighting the Goblin King requires effects
    if (this.formSource.equals("cobbsknob.php")) {
      if (EquipmentManager.isWearingOutfit(OutfitPool.HAREM_OUTFIT)) {
        // Harem girl
        if (!KoLConstants.activeEffects.contains(PERFUME)
            && !KoLCharacter.inBeecore()
            && InventoryManager.retrieveItem(KNOB_GOBLIN_PERFUME)) {
          RequestThread.postRequest(UseItemRequest.getInstance(KNOB_GOBLIN_PERFUME));
        }
        return KoLConstants.activeEffects.contains(PERFUME);
      }

      if (EquipmentManager.isWearingOutfit(OutfitPool.KNOB_ELITE_OUTFIT)) {
        // Elite Guard
        return InventoryManager.retrieveItem(KNOB_CAKE);
      }

      // If we are in Beecore, we had to adventure to get the effect.
      if (EquipmentManager.hasOutfit(OutfitPool.HAREM_OUTFIT)
          && (KoLConstants.activeEffects.contains(PERFUME)
              || (!KoLCharacter.inBeecore()
                  && InventoryManager.retrieveItem(KNOB_GOBLIN_PERFUME)))) {
        SpecialOutfit outfit = EquipmentDatabase.getOutfit(OutfitPool.HAREM_OUTFIT);
        RequestThread.postRequest(new EquipmentRequest(outfit));

        // If we selected the harem girl outfit, use a perfume
        if (!KoLConstants.activeEffects.contains(PERFUME)) {
          RequestThread.postRequest(UseItemRequest.getInstance(KNOB_GOBLIN_PERFUME));
        }
        return true;
      }

      if (EquipmentManager.hasOutfit(OutfitPool.KNOB_ELITE_OUTFIT)
          && InventoryManager.retrieveItem(KNOB_CAKE)) {
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
    int outfitId = this.getOutfitId();
    if (outfitId > 0) {
      if (EquipmentManager.isWearingOutfit(outfitId)) {
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
      if (!InventoryManager.hasItem(TRANSFUNCTIONER)) {
        RequestThread.postRequest(new PlaceRequest("forestvillage", "fv_mystic"));
        GenericRequest pixelRequest = new GenericRequest("choice.php?whichchoice=664&option=1");
        // The early steps cannot be skipped
        RequestThread.postRequest(pixelRequest);
        RequestThread.postRequest(pixelRequest);
        RequestThread.postRequest(pixelRequest);
      }

      if (!KoLCharacter.hasEquipped(TRANSFUNCTIONER)) {
        RequestThread.postRequest(new EquipmentRequest(TRANSFUNCTIONER));
      }
      return true;
    }

    if (this.adventureNumber == AdventurePool.PALINDOME) {
      if (!KoLCharacter.hasEquipped(TALISMAN)) {
        // This will pick an empty slot, or accessory1, if all are full
        RequestThread.postRequest(new EquipmentRequest(TALISMAN));
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

      // This should not fail; validate1 verified we had one.
      if (!InventoryManager.retrieveItem(ENCHANTED_BEAN)) {
        return false;
      }

      // Use the enchanted bean by clicking on the coffee grounds.
      // This should not fail
      RequestThread.postRequest(new PlaceRequest("plains", "garbage_grounds"));
      return QuestDatabase.isQuestLaterThan(Quest.GARBAGE, QuestDatabase.STARTED);
    }

    // The casino is unlocked if you have a casino pass in inventory.
    if (this.zone.equals("Casino")) {
      return InventoryManager.retrieveItem(ItemPool.CASINO_PASS);
    }

    if (this.zone.equals("Island")) {
      // If validate1 expected us to build dinghy, do it.
      return buildDinghy();
    }

    if (this.zone.equals("Pirate")) {
      // If validate1 expected us to build dinghy, do it.
      buildDinghy();

      // *** Equip Swashbucking getup or pirate fledges

      return true;
    }

    if (this.zone.equals("IsleWar")) {
      // If validate1 expected us to build a dinghy, do it.
      buildDinghy();

      // *** Equip an outfit, if expected

      return true;
    }

    if (this.zone.equals("BatHole")) {
      switch (this.adventureNumber) {
        case AdventurePool.GUANO_JUNCTION:
          if (KoLCharacter.getElementalResistanceLevels(Element.STENCH) >= 1) {
            return true;
          }
          KoLmafia.updateDisplay(MafiaState.ERROR, "You can't stand the stench");
          return false;
        case AdventurePool.BATRAT:
        case AdventurePool.BEANBAT:
        case AdventurePool.BOSSBAT:
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
          RequestThread.postRequest(UseItemRequest.getInstance(SONAR.getInstance(sonarsToUse)));
          sonarsUsed += sonarsToUse;

          return (sonarsUsed >= sonarsForLocation);
      }
      return true;
    }

    if (this.zone.equals("Rabbit Hole")) {
      if (!KoLConstants.activeEffects.contains(DOWN_THE_RABBIT_HOLE)) {
        AdventureResult item = AdventureDatabase.zoneGeneratingItem(this.zone);

        if (!InventoryManager.retrieveItem(item)) {
          // This shouldn't fail as it is guaranteed in canAdventure()
          return false;
        }

        RequestThread.postRequest(UseItemRequest.getInstance(item));
      }

      return true;
    }

    if (this.zone.equals("The Drip")) {
      if (!InventoryManager.hasItem(DRIP_HARNESS)) {
        KoLmafia.updateDisplay(MafiaState.ERROR, "You need a Drip harness to go there");
        return false;
      }

      if (!KoLCharacter.hasEquipped(DRIP_HARNESS)) {
        InventoryManager.retrieveItem(DRIP_HARNESS);
        RequestThread.postRequest(new EquipmentRequest(DRIP_HARNESS));
      }

      return true;
    }

    if (this.zone.equals("FantasyRealm")) {
      if (!InventoryManager.hasItem(FANTASY_REALM_GEM)) {
        KoLmafia.updateDisplay(MafiaState.ERROR, "You need a FantasyRealm G. E. M. to go there");
        return false;
      }

      // Must have FantasyRealm GEM equipped
      if (!KoLCharacter.hasEquipped(FANTASY_REALM_GEM)) {
        RequestThread.postRequest(new EquipmentRequest(FANTASY_REALM_GEM));
      }

      // Cannot bring a familiar
      if (KoLCharacter.getFamiliar() != FamiliarData.NO_FAMILIAR) {
        RequestThread.postRequest(new FamiliarRequest(null));
      }

      return true;
    }

    if (this.zone.equals("Orchard")) {
      var item =
          switch (this.adventureNumber) {
            case AdventurePool.FILTHWORM_FEEDING_CHAMBER -> KoLConstants.activeEffects.contains(
                    FILTHWORM_LARVA_STENCH)
                ? null
                : ItemPool.FILTHWORM_HATCHLING_GLAND;
            case AdventurePool.FILTHWORM_GUARDS_CHAMBER -> KoLConstants.activeEffects.contains(
                    FILTHWORM_DRONE_STENCH)
                ? null
                : ItemPool.FILTHWORM_DRONE_GLAND;
            case AdventurePool.FILTHWORM_QUEENS_CHAMBER -> KoLConstants.activeEffects.contains(
                    FILTHWORM_GUARD_STENCH)
                ? null
                : ItemPool.FILTHWORM_GUARD_GLAND;
            default -> null;
          };

      if (item != null) {
        if (!InventoryManager.hasItem(item)) {
          return false;
        }

        RequestThread.postRequest(UseItemRequest.getInstance(item));
      }

      return true;
    }

    if (this.adventureNumber == AdventurePool.SKELETON_STORE) {
      if (Preferences.getBoolean("skeletonStoreAvailable")
          || QuestDatabase.isQuestStarted(Quest.MEATSMITH)) {
        return true;
      }

      // If we have a bone with a price tag on it, use it
      if (InventoryManager.hasItem(BONE_WITH_A_PRICE_TAG)) {
        RequestThread.postRequest(UseItemRequest.getInstance(BONE_WITH_A_PRICE_TAG));
      } else {
        // Otherwise, visit the Meatsmith and start the quest.
        RequestThread.postRequest(new GenericRequest("shop.php?whichshop=meatsmith"));
        RequestThread.postRequest(new GenericRequest("shop.php?whichshop=meatsmith&action=talk"));
        RequestThread.postRequest(new GenericRequest("choice.php?whichchoice=1059&option=1"));
      }

      return Preferences.getBoolean("skeletonStoreAvailable");
    }

    if (this.adventureNumber == AdventurePool.MADNESS_BAKERY) {
      if (Preferences.getBoolean("madnessBakeryAvailable")
          || QuestDatabase.isQuestStarted(Quest.ARMORER)) {
        return true;
      }

      // If we have hypnotic breadcrumbs on it, use it
      if (InventoryManager.hasItem(HYPNOTIC_BREADCRUMBS)) {
        RequestThread.postRequest(UseItemRequest.getInstance(HYPNOTIC_BREADCRUMBS));
      } else {
        // Otherwise, visit the Armorer and start the quest.
        RequestThread.postRequest(new GenericRequest("shop.php?whichshop=armory"));
        RequestThread.postRequest(new GenericRequest("shop.php?whichshop=armory&action=talk"));
        RequestThread.postRequest(new GenericRequest("choice.php?whichchoice=1065&option=1"));
      }

      return Preferences.getBoolean("madnessBakeryAvailable");
    }

    if (this.adventureNumber == AdventurePool.OVERGROWN_LOT) {
      if (Preferences.getBoolean("overgrownLotAvailable")
          || QuestDatabase.isQuestStarted(Quest.DOC)) {
        return true;
      }

      // If we have a map to a hidden booze cache on it, use it
      if (InventoryManager.hasItem(BOOZE_MAP)) {
        RequestThread.postRequest(UseItemRequest.getInstance(BOOZE_MAP));
      } else {
        // Otherwise, visit Doc Galaktik and start the quest.
        RequestThread.postRequest(new GenericRequest("shop.php?whichshop=doc"));
        RequestThread.postRequest(new GenericRequest("shop.php?whichshop=doc&action=talk"));
        RequestThread.postRequest(new GenericRequest("choice.php?whichchoice=1064&option=1"));
      }

      return Preferences.getBoolean("overgrownLotAvailable");
    }

    return true;
  }

  private int getFirstAvailableOutfitId(int... ids) {
    return Arrays.stream(ids).filter(EquipmentManager::hasOutfit).findFirst().orElse(0);
  }

  public int getOutfitId() {
    return switch (this.adventureNumber) {
        // Can be either FRAT_OUTFIT or WAR_FRAT_OUTFIT
      case AdventurePool.FRAT_HOUSE_DISGUISED,
          AdventurePool.WARTIME_HIPPY_CAMP_DISGUISED -> getFirstAvailableOutfitId(
          OutfitPool.WAR_FRAT_OUTFIT, OutfitPool.FRAT_OUTFIT);
        // Can be either HIPPY_OUTFIT or WAR_HIPPY_OUTFIT
      case AdventurePool.WARTIME_FRAT_HOUSE_DISGUISED,
          AdventurePool.HIPPY_CAMP_DISGUISED -> getFirstAvailableOutfitId(
          OutfitPool.WAR_HIPPY_OUTFIT, OutfitPool.HIPPY_OUTFIT);
      case AdventurePool.CLOACA_BATTLEFIELD -> OutfitPool.CLOACA_UNIFORM;
      case AdventurePool.DYSPEPSI_BATTLEFIELD -> OutfitPool.DYSPEPSI_UNIFORM;
      case AdventurePool.FRAT_UNIFORM_BATTLEFIELD -> OutfitPool.WAR_FRAT_OUTFIT;
      case AdventurePool.HIPPY_UNIFORM_BATTLEFIELD -> OutfitPool.WAR_HIPPY_OUTFIT;
      default -> 0;
    };
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
