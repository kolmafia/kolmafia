package net.sourceforge.kolmafia;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import net.sourceforge.kolmafia.AscensionPath.Path;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLConstants.ZodiacZone;
import net.sourceforge.kolmafia.combat.Macrofier;
import net.sourceforge.kolmafia.equipment.Slot;
import net.sourceforge.kolmafia.listener.NamedListenerRegistry;
import net.sourceforge.kolmafia.modifiers.BooleanModifier;
import net.sourceforge.kolmafia.moods.RecoveryManager;
import net.sourceforge.kolmafia.objectpool.AdventurePool;
import net.sourceforge.kolmafia.objectpool.EffectPool;
import net.sourceforge.kolmafia.objectpool.FamiliarPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.objectpool.OutfitPool;
import net.sourceforge.kolmafia.persistence.AdventureDatabase;
import net.sourceforge.kolmafia.persistence.AdventureDatabase.DifficultyLevel;
import net.sourceforge.kolmafia.persistence.AdventureDatabase.Environment;
import net.sourceforge.kolmafia.persistence.EquipmentDatabase;
import net.sourceforge.kolmafia.persistence.HolidayDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.persistence.MonsterDatabase.Element;
import net.sourceforge.kolmafia.persistence.QuestDatabase;
import net.sourceforge.kolmafia.persistence.QuestDatabase.Quest;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.AdventureRequest;
import net.sourceforge.kolmafia.request.AdventureRequest.ShadowRift;
import net.sourceforge.kolmafia.request.BasementRequest;
import net.sourceforge.kolmafia.request.CampgroundRequest;
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
import net.sourceforge.kolmafia.request.TavernRequest;
import net.sourceforge.kolmafia.request.UntinkerRequest;
import net.sourceforge.kolmafia.request.UseItemRequest;
import net.sourceforge.kolmafia.request.concoction.CreateItemRequest;
import net.sourceforge.kolmafia.session.BatManager;
import net.sourceforge.kolmafia.session.ChoiceManager;
import net.sourceforge.kolmafia.session.CryptManager;
import net.sourceforge.kolmafia.session.EncounterManager;
import net.sourceforge.kolmafia.session.EquipmentManager;
import net.sourceforge.kolmafia.session.InventoryManager;
import net.sourceforge.kolmafia.session.LimitMode;
import net.sourceforge.kolmafia.shop.ShopRequest;
import net.sourceforge.kolmafia.utilities.StringUtilities;

@SuppressWarnings("incomplete-switch")
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
    {null, "Demon in Combat"}
  };

  public static final AdventureResult BEATEN_UP = EffectPool.get(EffectPool.BEATEN_UP, 4);

  public static KoLAdventure lastVisitedLocation = null;
  public static boolean locationLogged = false;
  // [Last Adventure] name as reported by KoL, unedited by mafia
  public static String lastZoneName = null;
  public static String lastLocationName = null;
  public static String lastLocationURL = null;

  private final boolean hasWanderers;
  private final String zone, parentZone, rootZone;
  private final String adventureId, formSource, adventureName;
  private final DifficultyLevel difficultyLevel;
  private final Environment environment;
  private final int adventureNumber;
  private final int recommendedStat, waterLevel, forceNoncombat;
  private final String normalString, lowercaseString, parentZoneDescription;

  private final GenericRequest request;
  private final AreaCombatData areaSummary;
  private final boolean isNonCombatsOnly;

  private static final HashSet<String> unknownAdventures = new HashSet<>();

  public static final Comparator<KoLAdventure> NameComparator =
      Comparator.comparing(v -> v.adventureName);

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

    this.normalString = StringUtilities.getEntityDecode(this.zone + ": " + this.adventureName);
    this.lowercaseString = this.normalString.toLowerCase();

    this.parentZone = AdventureDatabase.getParentZone(zone);
    this.parentZoneDescription = AdventureDatabase.ZONE_DESCRIPTIONS.get(this.parentZone);

    this.rootZone = AdventureDatabase.getRootZone(this.parentZone);

    this.difficultyLevel = AdventureDatabase.getDifficultyLevel(adventureName);
    this.environment = AdventureDatabase.getEnvironment(adventureName);

    this.recommendedStat = AdventureDatabase.getRecommendedStat(adventureName);

    this.waterLevel = AdventureDatabase.getWaterLevel(adventureName);

    this.forceNoncombat = AdventureDatabase.getForceNoncombat(adventureName);

    this.hasWanderers =
        AdventureDatabase.hasWanderers(adventureName, formSource.equals("adventure.php"));

    this.request =
        switch (formSource) {
          case "dwarffactory.php" -> new DwarfFactoryRequest("ware");
          case "clan_gym.php" -> new ClanRumpusRequest(
              ClanRumpusRequest.RequestType.fromString(adventureId));
          case "clan_hobopolis.php" -> new RichardRequest(StringUtilities.parseInt(adventureId));
          case "basement.php" -> new BasementRequest(adventureName);
          default -> new AdventureRequest(adventureName, formSource, adventureId);
        };

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

  public String getRootZone() {
    return this.rootZone;
  }

  public DifficultyLevel getDifficultyLevel() {
    return this.difficultyLevel;
  }

  public Environment getEnvironment() {
    return this.environment;
  }

  public int getRecommendedStat() {
    return this.recommendedStat;
  }

  public int getWaterLevel() {
    return this.waterLevel;
  }

  public int getForceNoncombat() {
    return this.forceNoncombat;
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
    return this.adventureName;
  }

  // In chamber <b>#1</b> of the Daily Dungeon, you encounter ...
  // In the <b>5th</b> chamber of the Daily Dungeon, you encounter ...
  private static final Pattern DAILY_DUNGEON_CHAMBER = Pattern.compile("<b>#?(\\d+)(?:th)?</b>");

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
   * Returns the adventure id for this adventure.
   *
   * @return The adventure id for this adventure
   */
  public String getAdventureId() {
    return this.adventureId;
  }

  public int getAdventureNumber() {
    return this.adventureNumber;
  }

  public boolean hasSnarfblat() {
    return this.getFormSource().equals("adventure.php");
  }

  public int getSnarfblat() {
    if (this.adventureId.equals("shadow_rift")) {
      // Going through a Shadow Rift redirects to
      // adventure.php?snarfblat=567
      return AdventurePool.SHADOW_RIFT;
    }

    if (!hasSnarfblat()) {
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

  public static boolean hasWanderingMonsters(final String urlString, final String adventureId) {
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

  // Validation part 0:
  //
  // If you want to adventure in a zone or location which is permanently
  // unlocked by an IOTM - which might also provide a "day pass" for one-day
  // access to (some of) the features, if we didn't see you use the item which
  // granted daily access, we can usually visit a map area and take a look.
  //
  // This returns false if the only way to visit an area is to use a (possibly
  // expensive) day pass - which you might not even own

  private boolean validate0() {
    return this.preValidateAdventure();
  }

  // Validation part 1:
  //
  // Determine if you are locked out of reaching a zone or location by level,
  // quest progress quest items, or other things that a betweenBattleScript
  // can't solve for you, like using a daypass, donning an outfit, or getting
  // an effect. After we've given that script a chance to run, part 2 of
  // validation might be able to do some of those things anyway.
  //
  // This returns false if there is nothing you can do to go to this location
  // at this time, or true, otherwise

  private boolean validate1() {
    return this.canAdventure();
  }

  // Validation part 2:
  //
  // The zone/location is within reach. If the pre-adventure script
  // donned outfits, bought supplies, or gained effects, cool.
  //
  // If it didn't do what we can here.
  //
  // If we can't, log error and return false.

  private boolean validate2() {
    return this.prepareForAdventure();
  }

  // AdventureResults used during validation
  private static final AdventureResult SPOOKYRAVEN_TELEGRAM =
      ItemPool.get(ItemPool.SPOOKYRAVEN_TELEGRAM);
  private static final AdventureResult LIBRARY_KEY = ItemPool.get(ItemPool.LIBRARY_KEY);
  private static final AdventureResult BILLIARDS_KEY = ItemPool.get(ItemPool.BILLIARDS_KEY);
  private static final AdventureResult SPOOKYRAVEN_NECKLACE =
      ItemPool.get(ItemPool.SPOOKYRAVEN_NECKLACE);
  private static final AdventureResult POWDER_PUFF = ItemPool.get(ItemPool.POWDER_PUFF);
  private static final AdventureResult FINEST_GOWN = ItemPool.get(ItemPool.FINEST_GOWN);
  private static final AdventureResult DANCING_SHOES = ItemPool.get(ItemPool.DANCING_SHOES);
  private static final AdventureResult LOOSENING_POWDER = ItemPool.get(ItemPool.LOOSENING_POWDER);
  private static final AdventureResult POWDERED_CASTOREUM =
      ItemPool.get(ItemPool.POWDERED_CASTOREUM);
  private static final AdventureResult DRAIN_DISSOLVER = ItemPool.get(ItemPool.DRAIN_DISSOLVER);
  private static final AdventureResult TRIPLE_DISTILLED_TURPENTINE =
      ItemPool.get(ItemPool.TRIPLE_DISTILLED_TURPENTINE);
  private static final AdventureResult DETARTRATED_ANHYDROUS_SUBLICALC =
      ItemPool.get(ItemPool.DETARTRATED_ANHYDROUS_SUBLICALC);
  private static final AdventureResult TRIATOMACEOUS_DUST =
      ItemPool.get(ItemPool.TRIATOMACEOUS_DUST);
  private static final AdventureResult WINE_BOMB = ItemPool.get(ItemPool.WINE_BOMB);
  private static final AdventureResult ENCRYPTION_KEY = ItemPool.get(ItemPool.ENCRYPTION_KEY);
  private static final AdventureResult COBBS_KNOB_MAP = ItemPool.get(ItemPool.COBBS_KNOB_MAP);
  private static final AdventureResult LAB_KEY = ItemPool.get(ItemPool.LAB_KEY);
  private static final AdventureResult MENAGERIE_KEY = ItemPool.get(ItemPool.MENAGERIE_KEY);
  private static final AdventureResult KNOB_GOBLIN_PERFUME =
      ItemPool.get(ItemPool.KNOB_GOBLIN_PERFUME);
  private static final AdventureResult KNOB_CAKE = ItemPool.get(ItemPool.KNOB_CAKE);
  private static final AdventureResult TRANSFUNCTIONER = ItemPool.get(ItemPool.TRANSFUNCTIONER);
  private static final AdventureResult SONAR = ItemPool.get(ItemPool.SONAR);
  private static final AdventureResult DODECAGRAM = ItemPool.get(ItemPool.DODECAGRAM);
  private static final AdventureResult CANDLES = ItemPool.get(ItemPool.CANDLES);
  private static final AdventureResult BUTTERKNIFE = ItemPool.get(ItemPool.BUTTERKNIFE);
  private static final AdventureResult TALISMAN = ItemPool.get(ItemPool.TALISMAN);
  private static final AdventureResult DINGHY_PLANS = ItemPool.get(ItemPool.DINGHY_PLANS);
  private static final AdventureResult DINGY_PLANKS = ItemPool.get(ItemPool.DINGY_PLANKS);
  private static final AdventureResult ENCHANTED_BEAN = ItemPool.get(ItemPool.ENCHANTED_BEAN);
  private static final AdventureResult PIRATE_FLEDGES = ItemPool.get(ItemPool.PIRATE_FLEDGES);
  private static final AdventureResult DRIP_HARNESS = ItemPool.get(ItemPool.DRIP_HARNESS, 1);
  private static final AdventureResult FANTASY_REALM_GEM = ItemPool.get(ItemPool.FANTASY_REALM_GEM);
  private static final AdventureResult BONE_WITH_A_PRICE_TAG =
      ItemPool.get(ItemPool.BONE_WITH_A_PRICE_TAG);
  private static final AdventureResult BOOZE_MAP = ItemPool.get(ItemPool.BOOZE_MAP);
  private static final AdventureResult HYPNOTIC_BREADCRUMBS =
      ItemPool.get(ItemPool.HYPNOTIC_BREADCRUMBS);
  // Items that grant an effect to give access
  private static final AdventureResult DRINK_ME_POTION = ItemPool.get(ItemPool.DRINK_ME_POTION);
  private static final AdventureResult DEVILISH_FOLIO = ItemPool.get(ItemPool.DEVILISH_FOLIO);
  private static final AdventureResult ABSINTHE = ItemPool.get(ItemPool.ABSINTHE);
  private static final AdventureResult TRANSPONDER = ItemPool.get(ItemPool.TRANSPORTER_TRANSPONDER);
  private static final AdventureResult MACHINE_SNOWGLOBE = ItemPool.get(ItemPool.MACHINE_SNOWGLOBE);
  // Items that grant an effect and require configuration to give access
  private static final AdventureResult ASTRAL_MUSHROOM = ItemPool.get(ItemPool.ASTRAL_MUSHROOM);
  private static final AdventureResult GONG = ItemPool.get(ItemPool.GONG);
  private static final AdventureResult MILK_CAP = ItemPool.get(ItemPool.MILK_CAP);
  private static final AdventureResult OPEN_PORTABLE_SPACEGATE =
      ItemPool.get(ItemPool.OPEN_PORTABLE_SPACEGATE);
  private static final AdventureResult TRAPEZOID = ItemPool.get(ItemPool.TRAPEZOID);
  private static final AdventureResult EMPTY_AGUA_DE_VIDA_BOTTLE =
      ItemPool.get(ItemPool.EMPTY_AGUA_DE_VIDA_BOTTLE);
  private static final AdventureResult BLACK_GLASS = ItemPool.get(ItemPool.BLACK_GLASS);

  private static final AdventureResult PERFUME = EffectPool.get(EffectPool.KNOB_GOBLIN_PERFUME, 1);
  private static final AdventureResult TROPICAL_CONTACT_HIGH =
      EffectPool.get(EffectPool.TROPICAL_CONTACT_HIGH);
  private static final AdventureResult DOWN_THE_RABBIT_HOLE =
      EffectPool.get(EffectPool.DOWN_THE_RABBIT_HOLE);
  private static final AdventureResult DIS_ABLED = EffectPool.get(EffectPool.DIS_ABLED);
  private static final AdventureResult ABSINTHE_MINDED = EffectPool.get(EffectPool.ABSINTHE);
  private static final AdventureResult TRANSPONDENT = EffectPool.get(EffectPool.TRANSPONDENT);
  private static final AdventureResult INSIDE_THE_SNOWGLOBE =
      EffectPool.get(EffectPool.INSIDE_THE_SNOWGLOBE, 1);
  private static final AdventureResult HALF_ASTRAL = EffectPool.get(EffectPool.HALF_ASTRAL);
  private static final AdventureResult FILTHWORM_LARVA_STENCH =
      EffectPool.get(EffectPool.FILTHWORM_LARVA_STENCH);
  private static final AdventureResult FILTHWORM_DRONE_STENCH =
      EffectPool.get(EffectPool.FILTHWORM_DRONE_STENCH);
  private static final AdventureResult FILTHWORM_GUARD_STENCH =
      EffectPool.get(EffectPool.FILTHWORM_GUARD_STENCH);

  private static final Map<String, String> grimstoneZones =
      Map.ofEntries(
          Map.entry("A Deserted Stretch of I-911", "hare"),
          Map.entry("Skid Row", "wolf"),
          Map.entry("The Prince's Ball", "stepmother"),
          Map.entry("Rumpelstiltskin's Home For Children", "gnome"),
          Map.entry("The Candy Witch and the Relentless Child Thieves", "witch"));

  private static final Map<String, String> holidayAdventures =
      Map.ofEntries(
          Map.entry("St. Sneaky Pete's Day Stupor", "St. Sneaky Pete's Day"),
          Map.entry("The Yuletide Bonfire", "Yuletide"),
          Map.entry("The Arrrboretum", "Arrrbor Day"),
          Map.entry("Generic Summer Holiday Swimming!", "Generic Summer Holiday"),
          Map.entry("The Spectral Pickle Factory", "April Fool's Day"),
          Map.entry("Drunken Stupor", ""));

  public boolean tooDrunkToAdventure() {
    if (!KoLCharacter.isFallingDown()) {
      return false;
    }

    // The wine glass allows you to adventure in snarfblat zones while falling down drunk
    // There may be some non-snarfblat zones coded to respect the wineglass, but I've not
    // been able to find any.
    if (KoLCharacter.hasEquipped(ItemPool.get(ItemPool.DRUNKULA_WINEGLASS))) {
      if (this.hasSnarfblat()) {
        return false;
      }
      // Shadow Rift locations are place.php redirecting to adventure.php?snarfblat=567
      if (this.zone.equals("Shadow Rift")) {
        return false;
      }
    }

    // There are some limit modes that allow adventuring even while falling down drunk
    switch (KoLCharacter.getLimitMode()) {
      case SPELUNKY, BATMAN -> {
        return false;
      }
    }

    // There are some adventure locations that allow adventuring even while falling down drunk
    if (AdventureDatabase.canAdventureWhileOverdrunk(adventureName)) {
      return false;
    }

    return true;
  }

  public static boolean woodsOpen() {
    return QuestDatabase.isQuestStarted(Quest.LARVA) || QuestDatabase.isQuestStarted(Quest.CITADEL);
  }

  private static boolean cemetaryOpen() {
    return QuestDatabase.isQuestStarted(Quest.CYRPT)
        || QuestDatabase.isQuestStarted(Quest.EGO)
        || QuestDatabase.isQuestStarted(Quest.NEMESIS);
  }

  private static Set<String> invalidExploathingPlaces =
      Set.of("town", "airport", "plains", "woods", "mountains", "desertbeach");

  // Validation part 0:
  private boolean checkZone(String alwaysPref, String todayPref, String place) {
    // Kingdom of Exploathing does not have any the top-level container
    // zones available. Certain lower level containers - town_wrong,
    // town_right, monorail - do exist and can be checked.
    if (KoLCharacter.isKingdomOfExploathing() && invalidExploathingPlaces.contains(place)) {
      return false;
    }

    // If we have permanent access, cool.
    if (alwaysPref != null && Preferences.getBoolean(alwaysPref)) {
      return true;
    }

    // If there is no day pass, looking at map might induce QuestManager to
    // detect permanent access
    if (todayPref == null) {
      var request = new PlaceRequest(place);
      RequestThread.postRequest(request);
      return Preferences.getBoolean(alwaysPref);
    }

    // If we don't know we have daily access, looking at the map
    // will induce QuestManager to detect it.
    if (!Preferences.getBoolean(todayPref)) {
      var request = new PlaceRequest(place);
      RequestThread.postRequest(request);
    }

    return Preferences.getBoolean(todayPref);
  }

  public boolean preValidateAdventure() {
    if (tooDrunkToAdventure()) {
      KoLmafia.updateDisplay(MafiaState.ERROR, "You are too drunk to continue.");
      return false;
    }

    switch (this.zone) {
      case "Spring Break Beach":
        // Unlimited adventuring if available
        return checkZone("sleazeAirportAlways", "_sleazeAirportToday", "airport");
      case "Conspiracy Island":
        // Unlimited adventuring if available
        return checkZone("spookyAirportAlways", "_spookyAirportToday", "airport");
      case "Dinseylandfill":
        // Unlimited adventuring if available
        return checkZone("stenchAirportAlways", "_stenchAirportToday", "airport");
      case "That 70s Volcano":
        // Unlimited adventuring if available
        return checkZone("hotAirportAlways", "_hotAirportToday", "airport");
      case "The Glaciest":
        // Unlimited adventuring if available
        return checkZone("coldAirportAlways", "_coldAirportToday", "airport");
      case "Gingerbread City":
        // One daily visit if available
        return checkZone("gingerbreadCityAvailable", "_gingerbreadCityToday", "mountains");
      case "LT&T":
        // One quest from a day pass, geometric cost for permanent
        return checkZone("telegraphOfficeAvailable", "_telegraphOfficeToday", "town_right");
      case "Neverending Party":
        // Unlimited adventuring if available
        if (KoLCharacter.inLegacyOfLoathing()) {
          if (Preferences.getBoolean("replicaNeverendingPartyAlways")) {
            return true;
          }
        }
        return checkZone("neverendingPartyAlways", "_neverendingPartyToday", "town_wrong");
      case "FantasyRealm":
        // One daily visit if available
        return checkZone("frAlways", "_frToday", "monorail");
      case "PirateRealm":
        // One daily visit if available
        return checkZone("prAlways", "_prToday", "monorail");
      case "Server Room":
        // One daily visit if available
        return checkZone("crAlways", "_crToday", "monorail");
      case "Tunnel of L.O.V.E.":
        return checkZone("loveTunnelAvailable", "_loveTunnelToday", "town_wrong");
      case "Twitch":
        // There is no permanent access to the Time Twitching Tower; it's
        // always day by day.
        return checkZone(null, "timeTowerAvailable", "town");
      case "Speakeasy":
        // It's in the Wrong Side of the Tracks.
        // You can get a quest item from an NPC in it.
        if (InventoryManager.hasItem(MILK_CAP)) {
          Preferences.setBoolean("ownsSpeakeasy", true);
          return true;
        }
        // Otherwise, look at the map
        return checkZone("ownsSpeakeasy", null, "town_wrong");
      case "The Spacegate":
        {
          // Through the Spacegate

          // It's in the mountains and Exploded Loathing does not have that zone.
          if (KoLCharacter.isKingdomOfExploathing()) {
            return false;
          }

          if (Preferences.getBoolean("spacegateAlways")
              || Preferences.getBoolean("_spacegateToday")) {
            return true;
          }

          if (!Preferences.getBoolean("_spacegateToday")) {
            if (InventoryManager.hasItem(OPEN_PORTABLE_SPACEGATE)) {
              Preferences.setBoolean("_spacegateToday", true);
              // There is no way to tell how many turns you have
              // left today in an open portable spacegate.
              // I think.
              Preferences.setInteger("_spacegateTurnsLeft", 20);
              return true;
            }
          }

          // Take a look at the mountains.
          var request = new PlaceRequest("mountains");
          RequestThread.postRequest(request);

          return Preferences.getBoolean("spacegateAlways");
        }
      case "The Sea Floor":
        {
          // There are 10 adventuring areas available in this zone.
          //
          // Some open via quest progress, some by purchasing maps from big
          // brother sea monkee, and some through other mechanisms.
          //
          // If you have done any of those things outside of the watchful eye of
          // KoLmafia, visiting the map will update everything.

          // Unless we have talked to the old guy, we cannot enter the sea.
          if (!QuestDatabase.isQuestStarted(Quest.SEA_OLD_GUY)) {
            return false;
          }

          // If we know the zone is available, no need to visit the map
          if (this.seaFloorZoneAvailable()) {
            return true;
          }

          // The Caliginous Abyss is enabled via item. No need to go to map.
          if (this.adventureNumber == AdventurePool.CALIGINOUS_ABYSS) {
            return false;
          }

          // Take a look at The Sea Floor .
          var request = new GenericRequest("seafloor.php");
          RequestThread.postRequest(request);

          return this.seaFloorZoneAvailable();
        }
    }

    return true;
  }

  // Validation part 1:

  public boolean canAdventure() {
    // If we get here via automation, preValidateAdventure() returned true

    if (KoLCharacter.getLimitMode().limitAdventure(this)) {
      return false;
    }

    // There are lots of areas from past events (like Crimbos) that are no
    // longer available. While they were active, we sorted them in "Events"
    // (or similar), but once they are done, they go into "Removed".
    if (this.rootZone.equals("Removed")) {
      return false;
    }

    // Some zones are restricted to a specific Ascension Path.
    Path path = AdventureDatabase.zoneAscensionPath(this.zone);
    if (path != Path.NONE && path != KoLCharacter.getPath()) {
      return false;
    }

    // Some zones/areas are available via items.
    AdventureResult item = AdventureDatabase.zoneGeneratingItem(this.zone);
    // If it is from an item, Standard restrictions may apply.
    if (item != null && KoLCharacter.getRestricted() && !ItemDatabase.isAllowed(item)) {
      return false;
    }

    // Further validation of individual zones happens below.

    // First check non-adventure.php zones.

    // Level 3 quest
    if (this.formSource.equals("cellar.php")) {
      // When the Council tells us to go to the Tavern, it is "started"
      // When we talk to Bart, he unlocks the cellar and it is "step1"
      // prepareForAdventure will talk to him.
      return QuestDatabase.isQuestStarted(Quest.RAT);
    }

    // Level 5 quest boss
    if (this.formSource.equals("cobbsknob.php")) {
      // If we have not opened the interior of the Knob or have already
      // killed the king, no can do
      if (!QuestDatabase.isQuestLaterThan(Quest.GOBLIN, QuestDatabase.STARTED)
          || QuestDatabase.isQuestFinished(Quest.GOBLIN)) {
        return false;
      }

      // Otherwise, you have two options for approaching the King:

      // In the Knob Goblin Harem Girl Disguise with Knob Goblin perfume effect
      // (With Knob Goblin perfume in inventory, we will use to get the effect)

      boolean haveHaremOutfit = EquipmentManager.hasOutfit(OutfitPool.HAREM_OUTFIT);
      boolean haveEffect = KoLConstants.activeEffects.contains(PERFUME);
      boolean havePerfume =
          !KoLCharacter.inBeecore() && InventoryManager.hasItem(KNOB_GOBLIN_PERFUME);

      // prepareForAdventure will use Knob Goblin perfume, if necessary.
      if (haveHaremOutfit && (haveEffect || havePerfume)) {
        return true;
      }

      // In the Knob Goblin Elite Guard Uniform with a Knob cake
      // (With ingredients in inventory, we will cook it)
      boolean haveEliteOutfit = EquipmentManager.hasOutfit(OutfitPool.KNOB_ELITE_OUTFIT);
      boolean haveCake = InventoryManager.hasItem(KNOB_CAKE);
      CreateItemRequest creator = CreateItemRequest.getInstance(KNOB_CAKE);
      boolean canBakeCake = creator != null && creator.getQuantityPossible() > 0;

      // prepareForAdventure will create Knob cake, if necessary.
      if (haveEliteOutfit && (haveCake || canBakeCake)) {
        return true;
      }

      return false;
    }

    // Level 7 quest boss
    if (this.formSource.equals("crypt.php")) {
      int evilness = Preferences.getInteger("cyrptTotalEvilness");
      return QuestDatabase.isQuestStep(Quest.CYRPT, QuestDatabase.STARTED)
          && (evilness == 999 || evilness == 0);
    }

    // Level 8 quest boss
    if (this.adventureId.equals(AdventurePool.SHROUDED_PEAK_ID)) {
      String trapper = QuestDatabase.getQuest(Quest.TRAPPER);
      if (trapper.equals(QuestDatabase.FINISHED)) {
        return false;
      }
      return (trapper.equals("step3")
              || trapper.equals("step4")
              || Preferences.getString("peteMotorbikeTires").equals("Snow Tires"))
          && KoLCharacter.getElementalResistanceLevels(Element.COLD) >= 5;
    }

    // Level 11 quest boss -> Lord Spookyraven
    if (this.adventureId.equals(AdventurePool.SUMMONING_CHAMBER_ID)) {
      return switch (QuestDatabase.getQuest(Quest.MANOR)) {
        case "step3" -> true;
        default -> InventoryManager.hasItem(WINE_BOMB)
            || (InventoryManager.hasItem(LOOSENING_POWDER)
                && InventoryManager.hasItem(POWDERED_CASTOREUM)
                && InventoryManager.hasItem(DRAIN_DISSOLVER)
                && InventoryManager.hasItem(TRIPLE_DISTILLED_TURPENTINE)
                && InventoryManager.hasItem(DETARTRATED_ANHYDROUS_SUBLICALC)
                && InventoryManager.hasItem(TRIATOMACEOUS_DUST));
      };
    }

    // Level 11 quest boss -> Ed
    if (this.adventureId.equals(AdventurePool.LOWER_CHAMBER_ID)) {
      return Preferences.getBoolean("lowerChamberUnlock");
    }

    // Fernswarthy's Basement
    if (this.formSource.equals("basement.php")) {
      return QuestDatabase.isQuestLaterThan(Quest.EGO, "step4");
    }

    // Mer-kin Temple
    if (this.formSource.equals("sea_merkin.php")) {
      // If you have a seahorse, you can get to the Mer-Kin Deepcity.
      // Whether or not you can enter the temple is a separate question.
      return this.deepCityZoneAvailable();
    }

    // Dwarven Factory Warehouse
    if (this.formSource.equals("dwarffactory.php")) {
      return QuestDatabase.isQuestStarted(Quest.FACTORY) && hasRequiredOutfit();
    }

    // The Tunnel of L.O.V.E.
    if (this.adventureId.equals(AdventurePool.TUNNEL_OF_LOVE_ID)) {
      return (Preferences.getBoolean("loveTunnelAvailable")
              || Preferences.getBoolean("_loveTunnelToday"))
          && !Preferences.getBoolean("_loveTunnelUsed");
    }

    // Unleash Your Inner Wolf
    if (this.adventureId.equals(AdventurePool.INNER_WOLF_ID)) {
      // Grimstone path must be "wolf"
      if (!Preferences.getString("grimstoneMaskPath").equals("wolf")) {
        return false;
      }
      // It takes three turns to Release Your Inner Wolf.
      // On turns 0-24, you may do it.
      // On turns 25-26, you are directed to train more in the Gym
      // On turn 27, you must do it.
      int turnsUsed = Preferences.getInteger("wolfTurnsUsed");
      return turnsUsed < 25 || turnsUsed == 27;
    }

    // Crimbo Train (Locomotive)
    if (this.adventureId.equals(AdventurePool.LOCOMOTIVE_ID)) {
      return !Preferences.getBoolean("superconductorDefeated");
    }

    /* Removed adventures.
    if (this.adventureId.equals(AdventurePool.ELDRITCH_FISSURE_ID)) {
      return Preferences.getBoolean("eldritchFissureAvailable");
    }

    if (this.adventureId.equals(AdventurePool.ELDRITCH_HORROR_ID)) {
      return Preferences.getBoolean("eldritchHorrorAvailable");
    }
    */

    if (this.zone.equals("Shadow Rift")) {
      if (!InventoryManager.hasItem(ItemPool.CLOSED_CIRCUIT_PAY_PHONE)
          && !KoLCharacter.inShadowsOverLoathing()) {
        // need payphone in inventory or to be in ASoL
        return false;
      }

      if (KoLCharacter.isKingdomOfExploathing()) {
        // KoE gets no access to it regardless for some reason known only to KoL devs.
        return false;
      }

      // These are "place.php" visits.
      ShadowRift rift = ShadowRift.findAdventureName(this.adventureName);
      String ingress = Preferences.getString("shadowRiftIngress");
      if (rift == null) {
        // Requesting generic Shadow Rift, which will go straight to
        // adventure.php as if through the most recent rift you entered.
        //
        // If we haven't yet gone through a Shadow Rift this ascension,
        // you'll find nothing but tumbleweeds, which is unlikely to be
        // what the user wants.
        return !ingress.equals("");
      }
      // If the desired rift is through the current ingress, cool.
      if (rift.getPlace().equals(ingress)) {
        return true;
      }
      // Otherwise, we want to adventure through a specific rift.
      // Ensure that we can reach it.
      return switch (rift) {
          // Right Side of the Tracks and The Nearby Plains
        case TOWN, PLAINS -> true;
          // The Distant Woods, Forest Village, The 8-Bit Realm
        case WOODS, VILLAGE, REALM -> KoLAdventure.woodsOpen();
          // Spookyraven Manor Third Floor
        case MANOR -> QuestDatabase.isQuestLaterThan(Quest.SPOOKYRAVEN_DANCE, "step3");
          // The Misspelled Cemetary
        case CEMETARY -> KoLAdventure.cemetaryOpen();
          // Desert Beach
        case BEACH -> KoLCharacter.desertBeachAccessible();
          // Mt. McLargeHuge
        case MCLARGEHUGE -> QuestDatabase.isQuestStarted(Quest.TRAPPER);
          // Somewhere Over the Beanstalk
        case BEANSTALK -> !KoLCharacter.isKingdomOfExploathing()
            && (QuestDatabase.isQuestLaterThan(Quest.GARBAGE, QuestDatabase.STARTED)
                || (QuestDatabase.isQuestStarted(Quest.GARBAGE)
                    && InventoryManager.hasItem(ENCHANTED_BEAN)));
          // The Castle in the Clouds in the Sky
        case CASTLE -> KoLCharacter.isKingdomOfExploathing()
            // Kingdom of Exploathing aftercore retains access. Check quest
            || QuestDatabase.isQuestFinished(Quest.GARBAGE)
            || InventoryManager.hasItem(ItemPool.get(ItemPool.SOCK, 1));
          // The Hidden City
        case CITY -> QuestDatabase.isQuestLaterThan(Quest.WORSHIP, "step2");
          // The Ancient Buried Pyramid
        case PYRAMID -> QuestDatabase.isQuestStarted(Quest.PYRAMID);
      };
    }

    // Only look at adventure.php locations below this.
    if (!this.formSource.startsWith("adventure.php")) {
      return true;
    }

    // Top-level map areas.

    // Open at level one, with a subset of eventual zones
    if (this.zone.equals("Town")) {
      return switch (this.adventureNumber) {
          // We can start the three market quests, if necessary
        case AdventurePool.SLEAZY_BACK_ALLEY -> true;
        case AdventurePool.OVERGROWN_LOT -> Preferences.getBoolean("overgrownLotAvailable")
            || InventoryManager.hasItem(BOOZE_MAP)
            || !KoLCharacter.isKingdomOfExploathing();
        case AdventurePool.MADNESS_BAKERY -> Preferences.getBoolean("madnessBakeryAvailable")
            || InventoryManager.hasItem(HYPNOTIC_BREADCRUMBS)
            || !KoLCharacter.isKingdomOfExploathing();
        case AdventurePool.SKELETON_STORE -> Preferences.getBoolean("skeletonStoreAvailable")
            || InventoryManager.hasItem(BONE_WITH_A_PRICE_TAG)
            || !KoLCharacter.isKingdomOfExploathing();
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

    if (this.zone.equals("Campground")) {
      if (!KoLCharacter.hasCampground()) {
        return false;
      }
      if (this.adventureNumber == AdventurePool.YOUR_MUSHROOM_GARDEN) {
        AdventureResult crop = CampgroundRequest.getCrop();
        return crop != null
            && CampgroundRequest.getCropType(crop) == CampgroundRequest.CropType.MUSHROOM;
      }
      return true;
    }

    if (this.parentZone.equals("Manor")) {
      // Quest.MANOR			Lord Spookyraven
      if (this.zone.equals("Manor0")) {
        return QuestDatabase.isQuestLaterThan(Quest.MANOR, QuestDatabase.STARTED);
      }

      // Quest.SPOOKYRAVEN_NECKLACE	Lady Spookyraven
      // KMail from Lady Spookyraven at start of ascension or at level 5
      // if unascended contains telegram from Lady Spookyraven
      // -> reading telegram starts quest and opens Haunted Kitchen
      // -> talking to her on first floor after getting necklace removes
      //    necklace, grants ghost necklace, and ends quest.
      if (this.zone.equals("Manor1")) {
        int neededLevel = KoLCharacter.getAscensions() > 0 ? 0 : 5;
        return switch (this.adventureNumber) {
          case AdventurePool.HAUNTED_PANTRY -> true;
          case AdventurePool.HAUNTED_KITCHEN, AdventurePool.HAUNTED_CONSERVATORY -> QuestDatabase
                  .isQuestStarted(Quest.SPOOKYRAVEN_NECKLACE)
              || InventoryManager.hasItem(SPOOKYRAVEN_TELEGRAM)
              || KoLCharacter.getLevel() >= neededLevel;
          case AdventurePool.HAUNTED_LIBRARY -> InventoryManager.hasItem(LIBRARY_KEY);
          case AdventurePool.HAUNTED_BILLIARDS_ROOM -> InventoryManager.hasItem(BILLIARDS_KEY);
          default -> true;
        };
      }

      // Quest.SPOOKYRAVEN_DANCE	Lady Spookyraven
      // KMail from Lady Spookyraven (immediately after ending necklace
      // quest or at level 7 if unascended) invites you to 2nd floor.
      // -> Talking to her starts quest and opens Haunted Gallery,
      //    Haunted Bathroom, and Haunted Bedroom
      // -> Talking to her after acquiring dancing gear opens Haunted
      //    Ballroom
      // -> Adventuring in Haunted Ballroom ends quest
      if (this.zone.equals("Manor2")) {
        int neededLevel = KoLCharacter.getAscensions() > 0 ? 0 : 7;
        return switch (this.adventureNumber) {
          case AdventurePool.HAUNTED_BATHROOM,
              AdventurePool.HAUNTED_BEDROOM,
              AdventurePool.HAUNTED_GALLERY ->
          // Already started this quest
          QuestDatabase.isQuestLaterThan(Quest.SPOOKYRAVEN_DANCE, QuestDatabase.STARTED)
              || (KoLCharacter.getLevel() >= neededLevel
                  && (
                  // Not finished the last quest, but you can (so prepareForAdventure will)
                  InventoryManager.hasItem(SPOOKYRAVEN_NECKLACE)
                      ||
                      // Finished the last quest, prepareForAdventure will start this one
                      QuestDatabase.isQuestFinished(Quest.SPOOKYRAVEN_NECKLACE)));
          case AdventurePool.HAUNTED_BALLROOM -> QuestDatabase.isQuestLaterThan(
                  Quest.SPOOKYRAVEN_DANCE, "step2")
              || (InventoryManager.hasItem(POWDER_PUFF)
                  && InventoryManager.hasItem(FINEST_GOWN)
                  && InventoryManager.hasItem(DANCING_SHOES));
          default -> true;
        };
      }

      // Quest.SPOOKYRAVEN_BABIES	Lady Spookyraven
      // KMail from Lady Spookyraven (immediately after ending dancing
      // quest or at level 9 if unascended) invites you to 3d floor.
      // -> Visiting third floor opens Haunted Storage Room, Haunted
      //   Nursery, and Haunted Laboratory
      if (this.zone.equals("Manor3")) {
        int neededLevel = KoLCharacter.getAscensions() > 0 ? 0 : 9;
        return switch (this.adventureNumber) {
          case AdventurePool.HAUNTED_LABORATORY,
              AdventurePool.HAUNTED_NURSERY,
              AdventurePool.HAUNTED_STORAGE_ROOM -> QuestDatabase.isQuestLaterThan(
                  Quest.SPOOKYRAVEN_DANCE, "step3")
              && (KoLCharacter.getLevel() >= neededLevel);
          default -> true;
        };
      }

      // You can't get there from here.
      return true;
    }

    // Open at level one, with a subset of eventual zones
    if (this.zone.equals("Mountain")) {
      // There are no "mountains" in Kingdom of Exploathing. However, the
      // smut orcs are unlocked from the start.
      if (KoLCharacter.isKingdomOfExploathing()) {
        return this.adventureNumber == AdventurePool.SMUT_ORC_LOGGING_CAMP;
      }

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
      if (this.adventureNumber == AdventurePool.PALINDOME) {
        // If you have created the Talisman o' Namsilat, the Palindome is available
        if (KoLCharacter.hasEquipped(TALISMAN) || InventoryManager.hasItem(TALISMAN)) {
          return true;
        }

        // If you have the components to create the Talisman, we will make it
        CreateItemRequest creator = CreateItemRequest.getInstance(TALISMAN);
        return creator != null && creator.getQuantityPossible() > 0;
      }

      if (KoLCharacter.isKingdomOfExploathing()) {
        return false;
      }

      return switch (this.adventureNumber) {
        case AdventurePool.FUN_HOUSE -> QuestDatabase.isQuestLaterThan(Quest.NEMESIS, "step4");
        case AdventurePool.UNQUIET_GARVES -> KoLAdventure.cemetaryOpen();
        case AdventurePool.VERY_UNQUIET_GARVES -> QuestDatabase.isQuestFinished(Quest.CYRPT);
          // Allow future "Plains" zones
        default -> true;
      };
    }

    // Opens at level two with first council quest
    if (this.zone.equals("Woods")) {
      if (KoLCharacter.isKingdomOfExploathing()) {
        return switch (this.adventureNumber) {
          case AdventurePool.SPOOKY_FOREST,
              AdventurePool.BARROOM_BRAWL,
              AdventurePool.HIDDEN_TEMPLE,
              AdventurePool.BLACK_FOREST -> true;
          default -> false;
        };
      }

      // With the exception of a few challenge paths, the Woods open when
      // the Council asks for a mosquito larva at level two.
      // In Community Service and Grey Goo (or at any other time, I suppose)
      // the Woods open when Whitey's Grove is opened by the Citadel quest.
      // This logic is handled in KoLAdventure.woodsOpen()
      return switch (this.adventureNumber) {
        case AdventurePool.BARROOM_BRAWL -> QuestDatabase.isQuestStarted(Quest.RAT);
          // prepareForAdventure will visit the Crackpot Mystic to get one, if needed
        case AdventurePool.PIXEL_REALM -> KoLAdventure.woodsOpen()
            || InventoryManager.hasItem(TRANSFUNCTIONER);
        case AdventurePool.HIDDEN_TEMPLE -> KoLCharacter.getTempleUnlocked()
            // Kingdom of Exploathing aftercore retains access. Check quest
            || QuestDatabase.isQuestFinished(Quest.WORSHIP);
        case AdventurePool.WHITEYS_GROVE -> KoLCharacter.isEd()
            || QuestDatabase.isQuestStarted(Quest.CITADEL)
            || QuestDatabase.isQuestLaterThan(Quest.PALINDOME, "step2");
        case AdventurePool.BLACK_FOREST -> QuestDatabase.isQuestStarted(Quest.MACGUFFIN);
        case AdventurePool.ROAD_TO_WHITE_CITADEL -> QuestDatabase.isQuestLaterThan(
            Quest.CITADEL, QuestDatabase.STARTED);
        case AdventurePool.OLD_LANDFILL -> QuestDatabase.isQuestStarted(Quest.HIPPY);
          // Allow future "Woods" zones
        default -> KoLAdventure.woodsOpen();
      };
    }

    // Opens when you build a bitchin' Meat car or the equivalent
    if (this.zone.equals("Beach")) {
      if (KoLCharacter.isKingdomOfExploathing()) {
        return switch (this.adventureNumber) {
          case AdventurePool.ARID_DESERT -> QuestDatabase.isQuestStarted(Quest.DESERT);
          case AdventurePool.OASIS -> Preferences.getBoolean("oasisAvailable")
              || QuestDatabase.isQuestFinished(Quest.DESERT);
          default -> false;
        };
      }

      if (!KoLCharacter.desertBeachAccessible()) {
        return false;
      }

      return switch (this.adventureNumber) {
        case AdventurePool.THE_SHORE, AdventurePool.SOUTH_OF_THE_BORDER -> true;
          // Open after diary read
        case AdventurePool.ARID_DESERT -> QuestDatabase.isQuestStarted(Quest.DESERT);
          // Opens after 1st desert exploration or - legacy - desert quest is finished
        case AdventurePool.OASIS -> Preferences.getBoolean("oasisAvailable")
            || QuestDatabase.isQuestFinished(Quest.DESERT);
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

      if (this.adventureNumber == AdventurePool.SONOFA_BEACH) {
        // Sonofa Beach is available during the war as a sidequest and also
        // after the war, whether or not it was used as such.
        return QuestDatabase.isQuestLaterThan(Quest.ISLAND_WAR, QuestDatabase.STARTED);
      }

      // If the war is in-progress, no "peaceful" areas are available
      if (QuestDatabase.isQuestStep(Quest.ISLAND_WAR, "step1")) {
        return false;
      }

      String loser = Preferences.getString("sideDefeated");

      switch (this.adventureNumber) {
        case AdventurePool.PIRATE_COVE:
          return true;

        case AdventurePool.HIPPY_CAMP:
          // on the verge of war, only the wartime camps are available
          if (QuestDatabase.isQuestStep(Quest.ISLAND_WAR, QuestDatabase.STARTED)) {
            return false;
          }
          // You can visit the hippy camp before or after the war, unless it
          // has been bombed into the stone age.
          if (QuestDatabase.isQuestFinished(Quest.ISLAND_WAR)) {
            return !loser.equals("hippies") && !loser.equals("both");
          }
          return true;

        case AdventurePool.FRAT_HOUSE:
          // on the verge of war, only the wartime camps are available
          if (QuestDatabase.isQuestStep(Quest.ISLAND_WAR, QuestDatabase.STARTED)) {
            return false;
          }
          // You can visit the frat house before or after the war, unless it
          // has been bombed into the stone age.
          if (QuestDatabase.isQuestFinished(Quest.ISLAND_WAR)) {
            return !loser.equals("fratboys") && !loser.equals("both");
          }
          return true;

        case AdventurePool.HIPPY_CAMP_DISGUISED:
          // on the verge of war, only the wartime camps are available
          if (QuestDatabase.isQuestStep(Quest.ISLAND_WAR, QuestDatabase.STARTED)) {
            return false;
          }
          // No disguises in bombed Hippy Camp
          if (QuestDatabase.isQuestFinished(Quest.ISLAND_WAR)
              && (loser.equals("hippies") || loser.equals("both"))) {
            return false;
          }
          return hasRequiredOutfit();

        case AdventurePool.FRAT_HOUSE_DISGUISED:
          // on the verge of war, only the wartime camps are available
          if (QuestDatabase.isQuestStep(Quest.ISLAND_WAR, QuestDatabase.STARTED)) {
            return false;
          }
          // No disguises in bombed Frat Camp
          if (QuestDatabase.isQuestFinished(Quest.ISLAND_WAR)
              && (loser.equals("fratboys") || loser.equals("both"))) {
            return false;
          }
          return hasRequiredOutfit();

        case AdventurePool.BOMBED_HIPPY_CAMP:
          return QuestDatabase.isQuestFinished(Quest.ISLAND_WAR)
              && (loser.equals("hippies") || loser.equals("both"));

        case AdventurePool.BOMBED_FRAT_HOUSE:
          return QuestDatabase.isQuestFinished(Quest.ISLAND_WAR)
              && (loser.equals("fratboys") || loser.equals("both"));

          // The Junkyard and the Farm are sidequest zones during the war, but
          // are available as single adventuring areas after the war is done.
        case AdventurePool.THE_JUNKYARD:
        case AdventurePool.MCMILLICANCUDDYS_FARM:
          return QuestDatabase.isQuestFinished(Quest.ISLAND_WAR);
      }

      // Allow future "Island" zones
      return true;
    }

    // Level 4 quest
    if (this.zone.equals("BatHole")) {
      String progress = QuestDatabase.getQuest(Quest.BAT);
      if (progress.equals(QuestDatabase.UNSTARTED)) {
        return false;
      }
      switch (this.adventureNumber) {
        case AdventurePool.BAT_HOLE_ENTRYWAY:
          return true;
        case AdventurePool.GUANO_JUNCTION:
          return KoLCharacter.getElementalResistanceLevels(Element.STENCH) >= 1;
        case AdventurePool.BATRAT:
        case AdventurePool.BEANBAT:
        case AdventurePool.BOSSBAT:
          {
            // Cannot adventure in the Boss Bat's Lair after completing the quest.
            if (this.adventureNumber == AdventurePool.BOSSBAT
                && QuestDatabase.isQuestFinished(Quest.BAT)) {
              return false;
            }

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

            // prepareForAdventure will use sonars-in-a-biscuit, if necessary.
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
                Quest.GOBLIN, QuestDatabase.STARTED)
            || (InventoryManager.hasItem(ENCRYPTION_KEY)
                && InventoryManager.hasItem(COBBS_KNOB_MAP));
        default -> false;
      };
    }

    if (this.zone.equals("Lab")) {
      return InventoryManager.hasItem(LAB_KEY);
    }

    if (this.zone.equals("Menagerie")) {
      return InventoryManager.hasItem(MENAGERIE_KEY)
          &&
          // You can fight a Knob Goblin Very Mad Scientist and get the Cobb's
          // Knob Menagerie key before opening Cobb's Knob.
          QuestDatabase.isQuestLaterThan(Quest.GOBLIN, QuestDatabase.STARTED);
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
      return QuestDatabase.isQuestStarted(Quest.AZAZEL)
          || QuestDatabase.isQuestFinished(Quest.FRIAR)
          || (InventoryManager.hasItem(DODECAGRAM)
              && InventoryManager.hasItem(CANDLES)
              && InventoryManager.hasItem(BUTTERKNIFE));
    }

    // Level 7 quest
    if (this.zone.equals("Cyrpt")) {
      if (!QuestDatabase.isQuestStarted(Quest.CYRPT)) {
        return false;
      }
      String property = CryptManager.evilZoneProperty(this.adventureNumber);
      return property != null ? Preferences.getInteger(property) > 0 : false;
    }

    // Level 8 quest
    if (this.zone.equals("McLarge")) {
      return switch (this.adventureNumber) {
        case AdventurePool.ITZNOTYERZITZ_MINE, AdventurePool.GOATLET -> QuestDatabase
            .isQuestStarted(Quest.TRAPPER);
        case AdventurePool.NINJA_SNOWMEN, AdventurePool.EXTREME_SLOPE -> QuestDatabase
            .isQuestLaterThan(Quest.TRAPPER, "step1");
        case AdventurePool.ICY_PEAK -> QuestDatabase.isQuestLaterThan(Quest.TRAPPER, "step4")
            && KoLCharacter.getElementalResistanceLevels(Element.COLD) >= 1;
        case AdventurePool.MINE_OFFICE -> QuestDatabase.isQuestStarted(Quest.FACTORY)
            && hasRequiredOutfit();
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
          // prepareForAdventure will plant an enchanted bean, if necessary.
        case AdventurePool.AIRSHIP -> !KoLCharacter.isKingdomOfExploathing()
            && (QuestDatabase.isQuestLaterThan(Quest.GARBAGE, QuestDatabase.STARTED)
                || (QuestDatabase.isQuestStarted(Quest.GARBAGE)
                    && InventoryManager.hasItem(ENCHANTED_BEAN)));
          // The Castle Basement is unlocked provided the player has the S.O.C.K
          // (legacy: rowboats give access but are no longer creatable)
        case AdventurePool.CASTLE_BASEMENT -> KoLCharacter.isKingdomOfExploathing()
            // Kingdom of Exploathing aftercore retains access. Check quest
            || QuestDatabase.isQuestFinished(Quest.GARBAGE)
            || InventoryManager.hasItem(ItemPool.get(ItemPool.SOCK, 1))
            || InventoryManager.hasItem(ItemPool.get(ItemPool.ROWBOAT, 1));
        case AdventurePool.CASTLE_GROUND -> Preferences.getInteger("lastCastleGroundUnlock")
                == KoLCharacter.getAscensions()
            // Kingdom of Exploathing aftercore retains access. Check quest
            || QuestDatabase.isQuestFinished(Quest.GARBAGE);
        case AdventurePool.CASTLE_TOP -> Preferences.getInteger("lastCastleTopUnlock")
                == KoLCharacter.getAscensions()
            // Kingdom of Exploathing aftercore retains access. Check quest
            || QuestDatabase.isQuestFinished(Quest.GARBAGE);
          // The Hole in the Sky is unlocked provided the player has a steam-powered rocketship
          // (legacy: rowboats give access but are no longer creatable)
        case AdventurePool.HOLE_IN_THE_SKY -> KoLCharacter.isKingdomOfExploathing()
            || InventoryManager.hasItem(ItemPool.get(ItemPool.ROCKETSHIP, 1))
            || InventoryManager.hasItem(ItemPool.get(ItemPool.ROWBOAT, 1));
        default -> false;
      };
    }

    // Level 11 quest

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
        case AdventurePool.HIDDEN_PARK,
            AdventurePool.NW_SHRINE,
            AdventurePool.SW_SHRINE,
            AdventurePool.NE_SHRINE,
            AdventurePool.SE_SHRINE,
            AdventurePool.ZIGGURAT -> true;
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
    // battle and the "Last Adventure" link in the charpane will be to the
    // wartime (Disguised) URL
    //
    // If you are either undisguised or disguised as a friend (either normal or
    // wartime outfits), you get non-combat encounters. The "Next Adventure"
    // links at the end of the battle and the "Last Adventure" link in the
    // charpane will be to the Wartime not-disguised URL
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

      // Quest.ISLAND_WAR progresses from "unstarted" -> "started" -> "step1" -> "finished"
      // "unstarted" is the peaceful Mysterious Island
      // "started" is the Verge of War on the Mysterious Island
      // "step1" is the actual war on the Big Island
      // "finished" is the peaceful Big Island

      if (!QuestDatabase.isQuestStarted(Quest.ISLAND_WAR)
          || QuestDatabase.isQuestFinished(Quest.ISLAND_WAR)) {
        return false;
      }

      switch (this.adventureNumber) {
        case AdventurePool.WARTIME_FRAT_HOUSE:
        case AdventurePool.WARTIME_HIPPY_CAMP:
          return QuestDatabase.isQuestStep(Quest.ISLAND_WAR, QuestDatabase.STARTED);

        case AdventurePool.WARTIME_FRAT_HOUSE_DISGUISED:
        case AdventurePool.WARTIME_HIPPY_CAMP_DISGUISED:
          if (!QuestDatabase.isQuestStep(Quest.ISLAND_WAR, QuestDatabase.STARTED)) {
            return false;
          }
          return hasRequiredOutfit();

        case AdventurePool.FRAT_UNIFORM_BATTLEFIELD:
        case AdventurePool.HIPPY_UNIFORM_BATTLEFIELD:
          if (!QuestDatabase.isQuestStep(Quest.ISLAND_WAR, "step1")) {
            return false;
          }
          return hasRequiredOutfit();

        case AdventurePool.THEMTHAR_HILLS:
          // Available only during the war. After the war, you can visit the Nunnery.
          return QuestDatabase.isQuestStep(Quest.ISLAND_WAR, "step1");

        default:
          return false;
      }
    }

    if (this.zone.equals("Farm")) {
      if (!QuestDatabase.isQuestStep(Quest.ISLAND_WAR, "step1")
          || !Preferences.getString("sidequestFarmCompleted").equals("none")) {
        return false;
      }

      String selected = Preferences.getString("duckAreasSelected");
      String cleared = Preferences.getString("duckAreasCleared");

      if (selected.indexOf(",") == selected.lastIndexOf(",")) {
        // True only if zero, one, or two duck areas have been selected.
        return this.adventureNumber == AdventurePool.THE_BARN;
      }

      // Three have been selected
      return selected.contains(this.adventureId) && !cleared.contains(this.adventureId);
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
          // prepareForAdventure will use the appropriate filthworm gland, if necessary
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
      // Next to that Barrel with Something Burning in it
      // Near an Abandoned Refrigerator
      // Over Where the Old Tires Are
      // Out by that Rusted-Out Car

      // You can visit any of the zones both before or after getting the tool -
      // and even after turning them all in.
      return QuestDatabase.isQuestStep(Quest.ISLAND_WAR, "step1");
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
      if (!ascended || level < 4 || level > 5 || !keyed) {
        return false;
      }

      return switch (this.adventureNumber) {
        case AdventurePool.CLOACA_BATTLEFIELD,
            AdventurePool.DYSPEPSI_BATTLEFIELD -> hasRequiredOutfit();
        default -> true;
      };
    }

    if (this.zone.equals("Degrassi Knoll")) {
      if (KoLCharacter.getSignZone() == ZodiacZone.KNOLL) {
        return false;
      }
      // There is no Degrassi Knoll in Kingdom of Exploathing
      if (KoLCharacter.isKingdomOfExploathing()) {
        return false;
      }
      // Either Paco or the Untinker will open the Knoll
      // We can accept the Untinker's quest if the woods are open
      return QuestDatabase.isQuestStarted(Quest.MEATCAR)
          || QuestDatabase.isQuestStarted(Quest.UNTINKER)
          || QuestDatabase.isQuestStarted(Quest.LARVA);
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
      // There is no Little Canadia in Kingdom of Exploathing
      return KoLCharacter.getSignZone() == ZodiacZone.CANADIA
          && !KoLCharacter.isKingdomOfExploathing();
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
      // There is no Gnomads Camp in Kingdom of Exploathing
      return KoLCharacter.getSignZone() == ZodiacZone.GNOMADS
          && !KoLCharacter.isKingdomOfExploathing();
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
          EquipmentManager.canEquip(PIRATE_FLEDGES) && InventoryManager.hasItem(PIRATE_FLEDGES);
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
      if (KoLCharacter.isKingdomOfExploathing()) {
        return adventureNumber == AdventurePool.THE_DAILY_DUNGEON;
      }
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

    if (this.zone.equals("The 8-Bit Realm")) {
      if (KoLCharacter.isKingdomOfExploathing()) {
        return false;
      }
      return KoLAdventure.woodsOpen() || InventoryManager.hasItem(TRANSFUNCTIONER);
    }

    if (this.zone.equals("The Drip")) {
      if (!InventoryManager.hasItem(DRIP_HARNESS)) {
        return false;
      }
      return switch (this.adventureNumber) {
        case AdventurePool.THE_DRIPPING_HALL -> Preferences.getBoolean("drippingHallUnlocked");
        default -> true;
      };
    }

    if (this.rootZone.equals("The Sea")) {
      // The Sea opens when you speak to The Old Man
      if (!QuestDatabase.isQuestStarted(Quest.SEA_OLD_GUY)) {
        return false;
      }

      return switch (this.zone) {
          // The Briny Deeps, The Brinier Deepers, The Briniest Deepests
        case "The Sea" -> true;
        case "The Sea Floor" -> this.seaFloorZoneAvailable();
        case "The Mer-Kin Deepcity" -> this.deepCityZoneAvailable();
          // There are currently no more adventuring areas in The Sea
        default -> false;
      };
    }

    // The following zones depend on your clan, your permissions, and the
    // current state of clan dungeons. We're not going to touch them here.
    if (zone.equals("Clan Basement") || zone.equals("Hobopolis") || zone.equals("Dreadsylvania")) {
      return true;
    }

    // Holiday zones
    if (holidayAdventures.containsKey(this.adventureName)) {
      var holiday = holidayAdventures.get(this.adventureName);
      var today = HolidayDatabase.getHolidays(false);
      return switch (this.adventureNumber) {
        case AdventurePool.DRUNKEN_STUPOR -> KoLCharacter.isFallingDown();
        case AdventurePool.SSPD_STUPOR -> {
          if (!today.contains(holiday)) yield false;
          yield KoLCharacter.getInebriety() >= 26;
        }
        default -> holiday == null || today.contains(holiday);
      };
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
      //
      // There are nine zones: three each on levels one, two, and three
      //
      // Each level one zone is unlocked by defeating 3 wandering bugbeares
      // Each level two zone is unlocked by defeating 6 wandering bugbeares
      // Each level three zone is unlocked by defeating 9 wandering bugbeares
      //
      // You must clear level one before level two is open.
      // You must clear level two before level three is open.
      //
      // Therefore, each zone goes from 0-{3,6,9), unlocked, open, cleared
      //
      // Zone are available for adventure only when they are "open"

      String property =
          switch (this.adventureNumber) {
            case AdventurePool.MEDBAY -> "statusMedbay";
            case AdventurePool.WASTE_PROCESSING -> "statusWasteProcessing";
            case AdventurePool.SONAR -> "statusSonar";
            case AdventurePool.SCIENCE_LAB -> "statusScienceLab";
            case AdventurePool.MORGUE -> "statusMorgue";
            case AdventurePool.SPECIAL_OPS -> "statusSpecialOps";
            case AdventurePool.ENGINEERING -> "statusEngineering";
            case AdventurePool.NAVIGATION -> "statusNavigation";
            case AdventurePool.GALLEY -> "statusGalley";
            default -> "";
          };

      return Preferences.getString(property).equals("open");
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

    if (this.zone.equals("Crimbo23")) {
      return Preferences.getBoolean(
          "crimbo23"
              + switch (this.adventureNumber) {
                case AdventurePool.CRIMBO23_ARMORY -> "Armory";
                case AdventurePool.CRIMBO23_BAR -> "Bar";
                case AdventurePool.CRIMBO23_CAFE -> "Cafe";
                case AdventurePool.CRIMBO23_COTTAGE -> "Cottage";
                case AdventurePool.CRIMBO23_FOUNDRY -> "Foundry";
                default -> "Unknown";
              }
              + "AtWar");
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

    if (this.parentZone.equals("Batfellow Area")) {
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

      // prepareForAdventure will use an astral mushroom, if necessary.
      return KoLCharacter.getLimitMode() == LimitMode.ASTRAL
          || InventoryManager.hasItem(ASTRAL_MUSHROOM);
    }

    if (this.zone.equals("Shape of Mole")) {
      // llama lama gong lets you choose to be a mole.
      // This grants 12 turns of Shape of...Mole!
      // You cannot use another gong if you are in Form of...Bird!
      // You cannot adventure anywhere except in Mt. Molehill while that effect is active.
      return KoLCharacter.getLimitMode() == LimitMode.MOLE || InventoryManager.hasItem(GONG);
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

    if (this.zone.equals("LT&T")) {
      // Only available if you have accepted a quest
      return QuestDatabase.isQuestStarted(Quest.TELEGRAM);
    }

    if (this.zone.equals("Neverending Party")) {
      // The Neverending Party
      return Preferences.getBoolean("neverendingPartyAlways")
          || Preferences.getBoolean("_neverendingPartyToday")
          || KoLCharacter.inLegacyOfLoathing()
              && Preferences.getBoolean("replicaNeverendingPartyAlways");
    }

    if (this.zone.equals("Video Game Dungeon")) {
      // If you have a GameInformPowerDailyPro walkthru in inventory, you
      // have (or had) access to The GameInformPowerDailyPro Dungeon.
      // *** Do we track your progress?
      return InventoryManager.hasItem(item);
    }

    if (this.zone.equals("Portal")) {
      // El Vibrato Island
      return (Preferences.getInteger("currentPortalEnergy") > 0)
          || InventoryManager.hasItem(TRAPEZOID);
    }

    if (this.zone.equals("Rabbit Hole")) {
      // A "DRINK ME" potion grants 20 turns of Down the Rabbit Hole.
      // Having the item or the effect will suffice.
      // prepareForAdventure will use a (cheap) potion if necessary.
      return KoLConstants.activeEffects.contains(DOWN_THE_RABBIT_HOLE)
          || InventoryManager.hasItem(item);
    }

    if (this.zone.equals("Suburbs")) {
      // devilish folio grants 30 turns of Dis Abled.
      // Having the item or the effect will suffice.
      return KoLConstants.activeEffects.contains(DIS_ABLED) || InventoryManager.hasItem(item);
    }

    if (this.zone.equals("Wormwood")) {
      // tiny bottle of absinthe grants 15 turns of Absinthe-Minded.
      // This opens up the Wormwood.
      // You can choose to adventure there or not.
      return KoLConstants.activeEffects.contains(ABSINTHE_MINDED) || InventoryManager.hasItem(item);
    }

    if (this.zone.equals("Spaaace")) {
      // transporter transponder grants 30 turns of Transpondent.
      // Having the item or the effect will suffice.
      return KoLConstants.activeEffects.contains(TRANSPONDENT) || InventoryManager.hasItem(item);
    }

    if (this.zone.equals("Deep Machine Tunnels")) {
      // The Deep Machine Tunnels
      // Deep Machine Tunnels snowglobe gives 57 turns of Inside The Snowglobe
      return KoLCharacter.canUseFamiliar(FamiliarPool.MACHINE_ELF)
          || KoLConstants.activeEffects.contains(INSIDE_THE_SNOWGLOBE)
          || InventoryManager.hasItem(item);
    }

    if (this.zone.equals("Memories")) {
      // You can gaze into an empty agua de vida bottle
      // and access the zones within
      return InventoryManager.hasItem(item);
    }

    if (this.parentZone.equals("Antique Maps")) {
      // The maps are all quest items. Therefore, you can't ascend and
      // keep them, but you can use them as much as you want until then.
      return InventoryManager.hasItem(item);
    }

    if (this.parentZone.equals("Psychoses")) {
      // You can only have one jar of psychoses open at once.
      // Each type of jar gives you access to a zone
      // _psychoJarUsed - if a psycho jar is in use
      // item - the item needed to activate this zone
      // That item is in the Campground if it is currently active
      if (KoLConstants.campground.contains(item)) {
        // There is a quest associated with each jar
        // After you finish, you can no longer adventure there.
        // We do not track those quests.
        // *** Add quest tracking and return false if quest is complete.
        return true;
      }

      // prepareForAdventure will NOT use a jar of psychoses.
      return false;
    }

    // You can only have one grimstone mask in use at a time.
    if (this.parentZone.equals("Grimstone")) {
      if (KoLCharacter.isEd()
          || KoLCharacter.inDarkGyffte()
          || KoLCharacter.isSneakyPete()
          || KoLCharacter.isWestOfLoathing()) {
        return false;
      }

      // One path at a time.
      String tale = grimstoneZones.get(this.zone);
      String current = Preferences.getString("grimstoneMaskPath");
      if (tale.equals(current)) {
        // See if the tale is finished
        switch (tale) {
          case "hare" -> {
            // 30 turns to adventure in A Deserted Stretch of I-911.
            // The zone closes when you finish.
            //
            // Note that you will be given turns of the Hare-Brained effect
            // when you attempt to adventure in that zone.  The duration of the
            // effect equals the number of turns remaining to adventure.
            return Preferences.getInteger("hareTurnsUsed") < 30;
          }
          case "wolf" -> {
            // 30 turns to adventure in Skid Row.
            // At turn 27, you must Release Your Inner Wolf.
            // (Handled above, as that is not adventure.php)
            // The zone closes when you finish.

            return Preferences.getInteger("wolfTurnsUsed") < 27;
          }
          case "stepmother" -> {
            // 30 turns to adventure at The Prince's Ball. The zone closes when
            // you finish, although odd silver coins (not quest items) can
            // still be spent.
            return Preferences.getInteger("cinderellaMinutesToMidnight") > 0;
          }
          case "gnome" -> {
            // 30 turns to adventure in Rumpelstiltskin's Home For Children.
            // The zone remains open when you finish, although, you can only
            // visit Rumplestiltskin's Workshop to craft things that use the
            // RUMPLE crafting method to use up your crafting materials before
            // starting another mini-game. ConcoctionsDatabase tracks this via
            // the property.
            return Preferences.getInteger("rumpelstiltskinTurnsUsed") < 30;
          }
          case "witch" -> {
            // 30 turns to adventure in The Candy Witch and the Relentless
            // Child Thieves. The zone closes when you finish.
            return Preferences.getInteger("candyWitchTurnsUsed") < 30;
          }
        }
      }

      // prepareForAdventure will NOT use a grimstone mask
      return false;
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

      // If you have permanent access, you must have activated the Spacegate
      // and chosen a planet to visit. prepareForAdventure will acquire and
      // equip the necessary protective gear, if you have not already done so.
      if (Preferences.getBoolean("spacegateAlways")) {
        return !Preferences.getString("_spacegateCoordinates").isBlank()
            && Preferences.getInteger("_spacegateTurnsLeft") > 0;
      }

      // If you don't have permanent access, you must have used a portable
      // spacegate. You don't get to choose which planet you go to, but you
      // have been given the necessary protective gear. prepareForAdventure
      // will equip it, if you have not already done so.
      if (Preferences.getBoolean("_spacegateToday")) {
        return Preferences.getInteger("_spacegateTurnsLeft") > 0;
      }

      return false;
    }

    if (this.zone.equals("Gingerbread City")) {
      if (!Preferences.getBoolean("gingerbreadCityAvailable")
          && !Preferences.getBoolean("_gingerbreadCityToday")) {
        return false;
      }

      int turnsAvailable = 20;
      if (Preferences.getBoolean("gingerExtraAdventures")) turnsAvailable += 10;
      if (Preferences.getBoolean("_gingerbreadClockAdvanced")) turnsAvailable -= 5;
      int turnsUsed = Preferences.getInteger("_gingerbreadCityTurns");

      if (turnsUsed >= turnsAvailable) {
        return false;
      }

      return switch (this.adventureNumber) {
        case AdventurePool.GINGERBREAD_SEWERS -> Preferences.getBoolean("gingerSewersUnlocked");
        case AdventurePool.GINGERBREAD_RETAIL_DISTRICT -> Preferences.getBoolean(
            "gingerRetailUnlocked");
        default -> true;
      };
    }

    if (this.zone.equals("FantasyRealm")) {
      return (Preferences.getBoolean("frAlways") || Preferences.getBoolean("_frToday"))
          && Preferences.getInteger("_frHoursLeft") >= 1
          && Preferences.getString("_frAreasUnlocked").contains(this.adventureName);
    }

    if (this.zone.startsWith("PirateRealm")) {
      // *** You have limited turns available per day.
      if (!Preferences.getBoolean("prAlways") && !Preferences.getBoolean("_prToday")) {
        return false;
      }
      if (this.adventureName.equals("PirateRealm Island")
          || this.adventureName.equals("Sailing the PirateRealm Seas")) {
        return true;
      }
      return Preferences.getString("_lastPirateRealmIsland").equals(this.adventureName);
    }

    if (this.zone.equals("Server Room")) {
      if (Preferences.getBoolean("crAlways") || Preferences.getBoolean("_crToday")) {
        String property =
            switch (this.adventureNumber) {
              case AdventurePool.CYBER_ZONE_1 -> "_cyberZone1Turns";
              case AdventurePool.CYBER_ZONE_2 -> "_cyberZone2Turns";
              case AdventurePool.CYBER_ZONE_3 -> "_cyberZone3Turns";
              default -> null;
            };
        if (property != null) {
          return Preferences.getInteger(property) < 20;
        }
      }
      return false;
    }

    if (this.zone.equals("Speakeasy")) {
      return (Preferences.getBoolean("ownsSpeakeasy"));
    }

    // Assume that any areas we did not call out above are available
    return true;
  }

  private boolean seaFloorZoneAvailable() {
    // We track individual aspects of the various quests.
    return switch (this.adventureNumber) {
        // Initially open: Little Brother
      case AdventurePool.AN_OCTOPUS_GARDEN -> true;
        // Big Brother
      case AdventurePool.THE_WRECK_OF_THE_EDGAR_FITZSIMMONS -> QuestDatabase.isQuestLaterThan(
          Quest.SEA_MONKEES, QuestDatabase.STARTED);
        // Grandpa
        // Free for Muscle classes. Otherwise, must buy map.
      case AdventurePool.ANEMONE_MINE -> (QuestDatabase.isQuestLaterThan(Quest.SEA_MONKEES, "step3")
              && KoLCharacter.isMuscleClass())
          || ItemDatabase.haveVirtualItem(ItemPool.ANEMONE_MINE_MAP);
        // Free for Mysticality classes Otherwise, must buy map.
      case AdventurePool.MARINARA_TRENCH -> (QuestDatabase.isQuestLaterThan(
                  Quest.SEA_MONKEES, "step3")
              && KoLCharacter.isMysticalityClass())
          || ItemDatabase.haveVirtualItem(ItemPool.MARINARA_TRENCH_MAP);
        // Free for Moxie classes Otherwise, must buy map.
      case AdventurePool.DIVE_BAR -> (QuestDatabase.isQuestLaterThan(Quest.SEA_MONKEES, "step3")
              && KoLCharacter.isMoxieClass())
          || ItemDatabase.haveVirtualItem(ItemPool.DIVE_BAR_MAP);
        // Grandma. Open when ask grandpa about Grandma.
      case AdventurePool.MERKIN_OUTPOST -> QuestDatabase.isQuestLaterThan(
          Quest.SEA_MONKEES, "step5");
        // Currents (seahorse). Open when ask Grandpa about currents.
      case AdventurePool.THE_CORAL_CORRAL -> Preferences.getBoolean("corralUnlocked");
        // Mom. Open when you have black glass - which you must equip
      case AdventurePool.CALIGINOUS_ABYSS -> InventoryManager.hasItem(BLACK_GLASS);
        // Optional maps you can purchase from Big Brother.
      case AdventurePool.MADNESS_REEF -> ItemDatabase.haveVirtualItem(ItemPool.MADNESS_REEF_MAP);
      case AdventurePool.THE_SKATE_PARK -> ItemDatabase.haveVirtualItem(ItemPool.SKATE_PARK_MAP)
          && !Preferences.getString("skateParkStatus").equals("peace");
        // That's all. If a new zone appears, assume you can get to it.
      default -> true;
    };
  }

  private boolean deepCityZoneAvailable() {
    if (Preferences.getString("seahorseName").isEmpty()) {
      return false;
    }

    switch (this.adventureName) {
      case "Mer-kin Elementary School", "Mer-kin Gymnasium" -> {
        // Requires any Mer-kin disguise
        return hasRequiredOutfit();
      }
      case "Mer-kin Library" -> {
        // Requires Mer-kin Scholar's Vestments
        return hasRequiredOutfit();
      }
      case "Mer-kin Colosseum" -> {
        // Requires Mer-kin Gladiatorial Gear
        return hasRequiredOutfit();
      }
      case "Mer-kin Temple" -> {
        if (KoLCharacter.inSeaPath()) {
          return false;
        }
        switch (Preferences.getString("merkinQuestPath")) {
          case "none" -> {
            // You can fight Dad Sea Monkee wearing Clothing of Loathing
            return availableOutfitId(OutfitPool.CLOTHING_OF_LOATHING) != 0;
          }
          case "gladiator", "scholar" -> {
            // If you are Gladiator Champion or High Priest, must still have outfit
            return hasRequiredOutfit();
          }
          case "done" -> {
            // If you have defeated the boss, nothing more to do.
            return false;
          }
        }
        return false;
      }
      case "Mer-kin Temple (Left Door)" -> {
        if (!KoLCharacter.inSeaPath()) {
          return false;
        }
        // Must be Gladiator Champion
        if (!Preferences.getBoolean("isMerkinGladiatorChampion")) {
          return false;
        }
        // Must not have defeated Shub-Jigguwatt
        if (Preferences.getBoolean("shubJigguwattDefeated")) {
          return false;
        }
        // Requires Mer-kin Gladiatorial Gear
        // You needed it to adventure in the Colosseum, but you can disassemble it...
        return hasRequiredOutfit();
      }
      case "Mer-kin Temple (Right Door)" -> {
        if (!KoLCharacter.inSeaPath()) {
          return false;
        }
        // Must be High Priest
        if (!Preferences.getBoolean("isMerkinHighPriest")) {
          return false;
        }
        // Must not have defeated Yog-Urt
        if (Preferences.getBoolean("yogUrtDefeated")) {
          return false;
        }
        // Requires Mer-kin Scholar's Vestments
        // You needed it to adventure in the Library, but you can disassemble it...
        return hasRequiredOutfit();
      }
      case "Mer-kin Temple (Center Door)" -> {
        if (!KoLCharacter.inSeaPath()) {
          return false;
        }
        // Must have defeated Yog-Urt and Shub-Jugguwatt
        if (!Preferences.getBoolean("shubJigguwattDefeated")
            || !Preferences.getBoolean("yogUrtDefeated")) {
          return false;
        }
        // Otherwise, no specific outfit required
        return true;
      }
    }
    // No more adventuring zones in the Deepcity
    return false;
  }

  private boolean hasRequiredOutfit() {
    return this.getOutfitId() != 0;
  }

  private int firstAvailableOutfitId(int... ids) {
    // If one of the outfits is currently worn, either the user specifically
    // chose it, or we chose it on a previous call.
    // Don't override user decisions, so, return that one.
    int outfitId =
        Arrays.stream(ids).filter(EquipmentManager::isWearingOutfit).findFirst().orElse(0);
    if (outfitId != 0) {
      return outfitId;
    }
    // No user selection. The outfits are assumed to be ordered by "goodness".
    // Pick the first available. (And equippable; hasOutfit enforces that.)
    return Arrays.stream(ids).filter(EquipmentManager::hasOutfit).findFirst().orElse(0);
  }

  private int availableOutfitId(int id) {
    // Checks if the outfit is currently worn or equippable.
    return EquipmentManager.hasOutfit(id) ? id : 0;
  }

  // Building a dingy dinghy is something that prepareForAdventure can do for us.
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

  public boolean prepareForAdventure() {
    // If we get here via automation, canAdventure() returned true

    // Certain Shadow Rifts can be unlocked just like certain zones.
    ShadowRift rift = ShadowRift.findAdventureName(this.adventureName);

    if (this.formSource.equals("cellar.php")) {
      if (QuestDatabase.isQuestLaterThan(Quest.RAT, QuestDatabase.STARTED)) {
        return true;
      }
      // When we talk to Bart, he will open the cellar for us.
      RequestThread.postRequest(new GenericRequest("tavern.php?place=barkeep"));
      return QuestDatabase.isQuestLaterThan(Quest.RAT, QuestDatabase.STARTED);
    }

    if (this.zone.equals("Astral")) {
      // To take a trip to the Astral Plane, you either need to be in
      // LimitMode.ASTRAL (Half-Astral is active) or have access to an astral
      // mushroom. You also cannot be in a competing LimitMode.
      // canAdventure ensured that one or the other is true

      if (KoLCharacter.getLimitMode() != LimitMode.ASTRAL) {
        // We will use a mushroom and take the trip you requested
        if (!InventoryManager.retrieveItem(ASTRAL_MUSHROOM)) {
          // This shouldn't fail.
          return false;
        }

        RequestThread.postRequest(UseItemRequest.getInstance(ASTRAL_MUSHROOM));

        if (KoLCharacter.getLimitMode() != LimitMode.ASTRAL) {
          // This shouldn't fail.
          return false;
        }
      }

      // We are Half-Astral. If we have not selected a trip, now is the time.

      if (Preferences.getString("currentAstralTrip").equals("")) {
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

        // Visit this KoLAdventure. You will not actually go there, but will be
        // redirected to the choice where you pick your zone.
        Preferences.setString("choiceAdventure71", option);
        RequestThread.postRequest(this.getRequest());

        if (Preferences.getString("currentAstralTrip").equals("")) {
          // This should not happen
          return false;
        }
      }

      return !LimitMode.ASTRAL.limitAdventure(this);
    }

    if (this.parentZone.equals("Grimstone")) {
      // Could use grimstone mask and select correct path.
      // We choose to not do that; you must already have done that.
      return true;
    }

    if (this.zone.equals("Shape of Mole")) {
      // To take a trip to Mt. Molehill, LimitMode.MOLE (Shape of...Mole!) must
      // be active or you have access to a llama lama gong. You also cannot be
      // in a competing LimitMode.  canAdventure ensured those requirements.

      if (KoLCharacter.getLimitMode() != LimitMode.MOLE) {
        // We will use a gong and choose to be a mole.
        if (!InventoryManager.retrieveItem(GONG)) {
          // This shouldn't fail.
          return false;
        }

        Preferences.setInteger("choiceAdventure276", 2);
        RequestThread.postRequest(UseItemRequest.getInstance(GONG));
        return true;
      }

      return KoLCharacter.getLimitMode() == LimitMode.MOLE;
    }

    if (this.zone.equals("The Spacegate")) {
      String neededGear = Preferences.getString("_spacegateGear");
      // If we don't know what gear we need, this must be a portable
      // spacegate. Visit Through the Spacegate to see what it gives us.
      if (neededGear.equals("")) {
        var request = new GenericRequest("adventure.php?snarfblat=494");
        RequestThread.postRequest(request);
        neededGear = Preferences.getString("_spacegateGear");
      }
      // We now know what gear we need.
      Set<AdventureResult> gear =
          Arrays.stream(neededGear.split("\\|"))
              .map(item -> new AdventureResult(item, 1, false))
              .collect(Collectors.toSet());
      // Do we have it all? If not, some, but not all, were manually checked out
      // from Equipment Requisition. Visit Through the Spacegate to get the
      // rest.
      boolean haveAllGear = gear.stream().allMatch(InventoryManager::hasItem);
      if (!haveAllGear) {
        var request = new GenericRequest("adventure.php?snarfblat=494");
        RequestThread.postRequest(request);
      }
      // Equip any gear that is not yet equipped.  Note that there are two
      // possible accessories. We can't unequip one to equip the other.
      // *** how to do that?
      for (AdventureResult item : gear) {
        if (!KoLCharacter.hasEquipped(item)) {
          RequestThread.postRequest(new EquipmentRequest(item));
        }
      }

      return true;
    }

    if (this.zone.equals("Degrassi Knoll")) {
      // Either Paco or the Untinker open the Knoll
      if (QuestDatabase.isQuestStarted(Quest.MEATCAR)
          || QuestDatabase.isQuestStarted(Quest.UNTINKER)) {
        return true;
      }

      // We can accept the Untinker's quest if the woods are open.
      if (KoLAdventure.woodsOpen()) {
        var request = new PlaceRequest("forestvillage", "fv_untinker_quest", true);
        request.addFormField("preaction", "screwquest");
        request.run();
      }
      return QuestDatabase.isQuestStarted(Quest.UNTINKER);
    }

    // Level 5 quest

    if (this.zone.equals("Knob")) {
      // Throne room
      if (this.formSource.equals("cobbsknob.php")) {
        if (EquipmentManager.isWearingOutfit(OutfitPool.HAREM_OUTFIT)) {
          // Harem girl
          if (KoLConstants.activeEffects.contains(PERFUME)) {
            return true;
          }
          // This shouldn't fail.
          if (InventoryManager.retrieveItem(KNOB_GOBLIN_PERFUME)) {
            RequestThread.postRequest(UseItemRequest.getInstance(KNOB_GOBLIN_PERFUME));
          }
          return true;
        }

        if (EquipmentManager.isWearingOutfit(OutfitPool.KNOB_ELITE_OUTFIT)) {
          // Elite Guard

          // This shouldn't fail. It will craft it if necessary.
          InventoryManager.retrieveItem(KNOB_CAKE);
          return true;
        }

        // If we are in Beecore, we had to adventure to get the effect.
        if (EquipmentManager.hasOutfit(OutfitPool.HAREM_OUTFIT)
            && (KoLConstants.activeEffects.contains(PERFUME)
                || InventoryManager.hasItem(KNOB_GOBLIN_PERFUME))) {
          // Harem girl

          wearOutfit(OutfitPool.HAREM_OUTFIT);

          if (KoLConstants.activeEffects.contains(PERFUME)) {
            return true;
          }

          // This shouldn't fail.
          if (InventoryManager.retrieveItem(KNOB_GOBLIN_PERFUME)) {
            RequestThread.postRequest(UseItemRequest.getInstance(KNOB_GOBLIN_PERFUME));
          }
          return true;
        }

        if (EquipmentManager.hasOutfit(OutfitPool.KNOB_ELITE_OUTFIT)) {
          // Elite Guard

          wearOutfit(OutfitPool.KNOB_ELITE_OUTFIT);

          // This shouldn't fail. It will craft it if necessary.
          InventoryManager.retrieveItem(KNOB_CAKE);
          return true;
        }

        return false;
      }

      // Outskirts of the Knob are always available
      if (this.adventureNumber == AdventurePool.OUTSKIRTS_OF_THE_KNOB) {
        return true;
      }

      // If we have opened the interior of the Knob, good
      if (QuestDatabase.isQuestLaterThan(Quest.GOBLIN, QuestDatabase.STARTED)) {
        return true;
      }

      // Otherwise, we must decrypt the map
      var request = UseItemRequest.getInstance(COBBS_KNOB_MAP);
      request.run();

      return QuestDatabase.isQuestLaterThan(Quest.GOBLIN, QuestDatabase.STARTED);
    }

    // Level 6 quest
    if (this.zone.equals("Pandamonium")) {
      // If we have completed the ritual and visited Pandammonium, Azazel's
      // quest is started and the areas in that zone are available.
      if (QuestDatabase.isQuestStarted(Quest.AZAZEL)) {
        return true;
      }

      // If we get here but have not finished the ritual, we must perform it.
      if (!QuestDatabase.isQuestFinished(Quest.FRIAR)) {
        var request = new GenericRequest("friars.php?action=ritual&pwd");
        request.run();
      }

      // If the quest is finished, visit Pandamonium to start Azazel's quest
      if (QuestDatabase.isQuestFinished(Quest.FRIAR)) {
        var request = new GenericRequest("pandamonium.php", false);
        request.run();
      }

      return QuestDatabase.isQuestStarted(Quest.AZAZEL);
    }

    if (this.formSource.equals("dwarffactory.php")
        || this.adventureNumber == AdventurePool.MINE_OFFICE) {

      int outfitId = this.getOutfitId();
      if (outfitId > 0) {
        return wearOutfit(outfitId);
      }

      return true;
    }

    // Level 8 quest
    if (this.zone.equals("McLarge")) {
      if (QuestDatabase.isQuestLaterThan(Quest.TRAPPER, QuestDatabase.STARTED)) {
        return true;
      }

      // If we get here, we need to talk to the Trapper
      var request = new PlaceRequest("mclargehuge", "trappercabin");
      request.run();
      return QuestDatabase.isQuestLaterThan(Quest.TRAPPER, QuestDatabase.STARTED);
    }

    if (this.zone.equals("Island") || this.zone.equals("IsleWar")) {
      // If canAdventure expected us to build dinghy, do it.
      if (!buildDinghy()) {
        // This should not fail.
        return false;
      }

      // If this is a disguise zone, wear an outfit
      int outfitId = this.getOutfitId();
      if (outfitId > 0) {
        return wearOutfit(outfitId);
      }

      return true;
    }

    if (this.zone.equals("Rift")) {
      // If this is a disguise zone, wear an outfit
      int outfitId = this.getOutfitId();
      if (outfitId > 0) {
        return wearOutfit(outfitId);
      }

      // Can't adventure in Battlefield (No Uniform) if we are wearing a Cola
      // War Uniform.  Remove the shield.
      if (EquipmentManager.isWearingOutfit(OutfitPool.CLOACA_UNIFORM)
          || EquipmentManager.isWearingOutfit(OutfitPool.DYSPEPSI_UNIFORM)) {
        RequestThread.postRequest(new EquipmentRequest(EquipmentRequest.UNEQUIP, Slot.OFFHAND));
      }

      return true;
    }

    // If the person has a continuum transfunctioner, then find
    // some way of equipping it.  If they do not have one, then
    // acquire one then try to equip it.

    if (this.zone.equals("The 8-Bit Realm")
        || this.zone.equals("Vanya's Castle")
        || rift == ShadowRift.REALM) {
      // You don't need a transfunctioner to go to the Shadow Rift in
      // The 8-bit Realm if that is the current ingress.
      if (rift == ShadowRift.REALM && Preferences.getString("shadowRiftIngress").equals("8bit")) {
        return true;
      }

      if (!InventoryManager.hasItem(TRANSFUNCTIONER)) {
        RequestThread.postRequest(new PlaceRequest("forestvillage", "fv_mystic"));
        // This redirects to choice.php&forceoption=0.
        //
        // The user might have configured choice 664 to automate.  If so,
        // ChoiceHandler will do the whole thing and we will end up with the
        // transfunctioner in inventory and not in a choice.
        GenericRequest pixelRequest = new GenericRequest("choice.php?whichchoice=664&option=1");
        while (ChoiceManager.handlingChoice) {
          RequestThread.postRequest(pixelRequest);
        }
      }

      if (!KoLCharacter.hasEquipped(TRANSFUNCTIONER)) {
        RequestThread.postRequest(new EquipmentRequest(TRANSFUNCTIONER));
      }
      return true;
    }

    if (this.parentZone.equals("Manor")) {
      if (this.zone.equals("Manor0")) {
        // Level 11 quest boss -> Lord Spookyraven
        if (this.adventureId.equals(AdventurePool.SUMMONING_CHAMBER_ID)) {
          if (QuestDatabase.getQuest(Quest.MANOR).equals("step3")) {
            return true;
          }
          // If we got here, we must have either a wine bomb or the six
          // ingredients for the mortar dissolving solution.
          //
          // Visiting the suspicious masonry will use whichever we have.
          var request = new PlaceRequest("manor4", "manor4_chamberwall", true);
          RequestThread.postRequest(request);
          return QuestDatabase.getQuest(Quest.MANOR).equals("step3");
        }
        return true;
      }

      if (this.zone.equals("Manor1")) {
        switch (this.adventureNumber) {
          case AdventurePool.HAUNTED_KITCHEN, AdventurePool.HAUNTED_CONSERVATORY -> {
            if (!QuestDatabase.isQuestStarted(Quest.SPOOKYRAVEN_NECKLACE)) {
              // If we have ascended at least once, we started with telegram in inventory.
              // Otherwise, it comes in KMail at level 5.
              if (!InventoryManager.hasItem(SPOOKYRAVEN_TELEGRAM)) {
                InventoryManager.refresh();
                InventoryManager.retrieveItem(SPOOKYRAVEN_TELEGRAM);
              }
              if (InventoryManager.hasItem(SPOOKYRAVEN_TELEGRAM)) {
                RequestThread.postRequest(UseItemRequest.getInstance(SPOOKYRAVEN_TELEGRAM));
              }
            }
            return QuestDatabase.isQuestStarted(Quest.SPOOKYRAVEN_NECKLACE);
          }
        }
        return true;
      }

      if (this.zone.equals("Manor2")) {
        switch (this.adventureNumber) {
          case AdventurePool.HAUNTED_BATHROOM,
              AdventurePool.HAUNTED_BEDROOM,
              AdventurePool.HAUNTED_GALLERY -> {
            if (!QuestDatabase.isQuestLaterThan(Quest.SPOOKYRAVEN_DANCE, QuestDatabase.STARTED)) {
              if (InventoryManager.hasItem(SPOOKYRAVEN_NECKLACE)) {
                // Talk to Lady Spookyraven on 1st floor
                var request = new GenericRequest("place.php?whichplace=manor1&action=manor1_ladys");
                RequestThread.postRequest(request);
              }
              if (QuestDatabase.isQuestFinished(Quest.SPOOKYRAVEN_NECKLACE)) {
                // Talk to Lady Spookyraven on 2nd floor
                var request = new GenericRequest("place.php?whichplace=manor2&action=manor2_ladys");
                RequestThread.postRequest(request);
              }
            }
            return QuestDatabase.isQuestLaterThan(Quest.SPOOKYRAVEN_DANCE, QuestDatabase.STARTED);
          }
          case AdventurePool.HAUNTED_BALLROOM -> {
            if (!QuestDatabase.isQuestLaterThan(Quest.SPOOKYRAVEN_DANCE, "step2")) {
              // These should not fail
              InventoryManager.retrieveItem(POWDER_PUFF);
              InventoryManager.retrieveItem(FINEST_GOWN);
              InventoryManager.retrieveItem(DANCING_SHOES);
              // Talk to Lady Spookyraven on 2nd floor
              var request = new GenericRequest("place.php?whichplace=manor2&action=manor2_ladys");
              RequestThread.postRequest(request);
            }
            return QuestDatabase.isQuestLaterThan(Quest.SPOOKYRAVEN_DANCE, "step2");
          }
        }
        return true;
      }

      return true;
    }

    if (this.adventureNumber == AdventurePool.PALINDOME) {
      if (KoLCharacter.hasEquipped(TALISMAN)) {
        return true;
      }

      // This shouldn't fail. It will craft it if necessary.
      InventoryManager.retrieveItem(TALISMAN);

      // This will pick an empty slot, or accessory1, if all are full
      RequestThread.postRequest(new EquipmentRequest(TALISMAN));

      return true;
    }

    if (this.rootZone.equals("The Sea")) {
      // If the zone requires an outfit, assume it allows water breathing.
      int outfitId = getOutfitId();

      if (outfitId != 0) {
        wearOutfit(outfitId);
      } else if (!KoLCharacter.currentBooleanModifier(BooleanModifier.ADVENTURE_UNDERWATER)) {
        // In theory, we could choose equipment or effects.
        // It's complicated. Let the user do that.
        KoLmafia.updateDisplay(MafiaState.ERROR, "You can't breathe underwater.");
        return false;
      }

      if (!KoLCharacter.currentBooleanModifier(BooleanModifier.UNDERWATER_FAMILIAR)) {
        // In theory, we could choose equipment or effects or even another familiar.
        // It's complicated. Let the user do that.
        KoLmafia.updateDisplay(MafiaState.ERROR, "Your familiar can't breathe underwater.");
        return false;
      }

      if (this.adventureNumber == AdventurePool.CALIGINOUS_ABYSS
          && !KoLCharacter.hasEquipped(BLACK_GLASS)) {
        // We could equip black glass in an accessory slot.  It's complicated,
        // since we can't unequip an item which lets us breathe underwater.
        // Let the user do it.
        KoLmafia.updateDisplay(MafiaState.ERROR, "Equip your black glass in order to go there.");
        return false;
      }

      return true;
    }

    if (this.zone.equals("Portal")) {
      // El Vibrato Island
      if (InventoryManager.hasItem(TRAPEZOID)) {
        // Use the El Vibrato trapezoid to open a portal
        RequestThread.postRequest(UseItemRequest.getInstance(TRAPEZOID));
      }
      return Preferences.getInteger("currentPortalEnergy") > 0;
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
      // canAdventure checks this already. If the betweenBattle script
      // should have a chance to fix it, make that method return true.
      if (KoLCharacter.getElementalResistanceLevels(Element.COLD) >= 5) {
        return true;
      }
      KoLmafia.updateDisplay(MafiaState.ERROR, "You need more cold protection");
      return false;
    }

    if (this.adventureNumber == AdventurePool.ICY_PEAK) {
      // canAdventure checks this already. If the betweenBattle script
      // should have a chance to fix it, make that method return true.
      if (KoLCharacter.getElementalResistanceLevels(Element.COLD) >= 1) {
        return true;
      }
      KoLmafia.updateDisplay(MafiaState.ERROR, "You need more cold protection");
      return false;
    }

    if (this.adventureNumber == AdventurePool.AIRSHIP || rift == ShadowRift.BEANSTALK) {
      if (QuestDatabase.isQuestLaterThan(Quest.GARBAGE, QuestDatabase.STARTED)) {
        return true;
      }

      // This should not fail; canAdventure verified we had one.
      if (!InventoryManager.retrieveItem(ENCHANTED_BEAN)) {
        return false;
      }

      // Use the enchanted bean by clicking on the coffee grounds.
      // This should not fail
      RequestThread.postRequest(new PlaceRequest("plains", "garbage_grounds"));
      return true;
    }

    // The casino is unlocked if you have a casino pass in inventory.
    if (this.zone.equals("Casino")) {
      return InventoryManager.retrieveItem(ItemPool.CASINO_PASS);
    }

    if (this.zone.equals("Pirate")) {
      // If canAdventure expected us to build dinghy, do it.
      buildDinghy();

      // If we are already acceptably garbed as pirate, cool.
      if (KoLCharacter.hasEquipped(PIRATE_FLEDGES)
          || EquipmentManager.isWearingOutfit(OutfitPool.SWASHBUCKLING_GETUP)) {
        return true;
      }

      // If we have the pirate fledges, that is the best choice.
      if (EquipmentManager.canEquip(PIRATE_FLEDGES) && InventoryManager.hasItem(PIRATE_FLEDGES)) {
        // This will pick an empty slot, or accessory1, if all are full
        RequestThread.postRequest(new EquipmentRequest(PIRATE_FLEDGES));
        return true;
      }

      // Otherwise, we must have the Swashbucking Getup. Don it.
      wearOutfit(OutfitPool.SWASHBUCKLING_GETUP);

      return true;
    }

    if (this.zone.equals("BatHole")) {
      switch (this.adventureNumber) {
        case AdventurePool.GUANO_JUNCTION -> {
          // canAdventure checks this already. If the betweenBattle script
          // should have a chance to fix it, make that method return true.
          if (KoLCharacter.getElementalResistanceLevels(Element.STENCH) >= 1) {
            return true;
          }
          KoLmafia.updateDisplay(MafiaState.ERROR, "You can't stand the stench");
          return false;
        }
        case AdventurePool.BATRAT, AdventurePool.BEANBAT, AdventurePool.BOSSBAT -> {
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
      }
      return true;
    }

    if (this.zone.equals("Rabbit Hole")) {
      if (!KoLConstants.activeEffects.contains(DOWN_THE_RABBIT_HOLE)) {
        if (!InventoryManager.retrieveItem(DRINK_ME_POTION)) {
          // This shouldn't fail as it is guaranteed in canAdventure()
          return false;
        }

        RequestThread.postRequest(UseItemRequest.getInstance(DRINK_ME_POTION));
      }

      return true;
    }

    if (this.zone.equals("Suburbs")) {
      if (!KoLConstants.activeEffects.contains(DIS_ABLED)) {
        if (!InventoryManager.retrieveItem(DEVILISH_FOLIO)) {
          // This shouldn't fail as it is guaranteed in canAdventure()
          return false;
        }

        RequestThread.postRequest(UseItemRequest.getInstance(DEVILISH_FOLIO));
      }

      return true;
    }

    if (this.zone.equals("Wormwood")) {
      if (!KoLConstants.activeEffects.contains(ABSINTHE_MINDED)) {
        if (!InventoryManager.retrieveItem(ABSINTHE)) {
          // This shouldn't fail as it is guaranteed in canAdventure()
          return false;
        }

        RequestThread.postRequest(UseItemRequest.getInstance(ABSINTHE));
      }

      return true;
    }

    if (this.zone.equals("Spaaace")) {
      if (!KoLConstants.activeEffects.contains(TRANSPONDENT)) {
        if (!InventoryManager.retrieveItem(TRANSPONDER)) {
          // This shouldn't fail as it is guaranteed in canAdventure()
          return false;
        }

        RequestThread.postRequest(UseItemRequest.getInstance(TRANSPONDER));
      }

      return true;
    }

    if (this.zone.equals("Deep Machine Tunnels")) {
      // I think you can use the snowglobe even if you have the familiar,
      // as long as it is in the terrarium
      if (KoLConstants.activeEffects.contains(INSIDE_THE_SNOWGLOBE)) {
        // With active effect, nothing more to do.
        return true;
      }

      // If you don't have the effect but do have a Machine Elf, prefer that.
      FamiliarData machineElf = KoLCharacter.usableFamiliar(FamiliarPool.MACHINE_ELF);
      if (machineElf != null) {
        // If the Machine Elf is at your side, good to go.
        if (KoLCharacter.getFamiliar().getId() == FamiliarPool.MACHINE_ELF) {
          return true;
        }
        // Otherwise, remove from terrarium
        new FamiliarRequest(machineElf).run();
        return true;
      }

      // Need to use a snowglobe
      if (!InventoryManager.retrieveItem(MACHINE_SNOWGLOBE)) {
        // This shouldn't fail as it is guaranteed in canAdventure()
        return false;
      }

      RequestThread.postRequest(UseItemRequest.getInstance(MACHINE_SNOWGLOBE));

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

    if (this.zone.equals("Memories")) {
      // We know that an empty agua de vida bottle is accessible to us.
      // Make sure it is in inventory.
      InventoryManager.retrieveItem(EMPTY_AGUA_DE_VIDA_BOTTLE);
      return InventoryManager.getCount(EMPTY_AGUA_DE_VIDA_BOTTLE) > 0;
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
        RequestThread.postRequest(new ShopRequest("meatsmith"));
        RequestThread.postRequest(new ShopRequest("meatsmith", "talk"));
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
        RequestThread.postRequest(new ShopRequest("armory"));
        RequestThread.postRequest(new ShopRequest("armory", "talk"));
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
        RequestThread.postRequest(new ShopRequest("doc"));
        RequestThread.postRequest(new ShopRequest("doc", "talk"));
        RequestThread.postRequest(new GenericRequest("choice.php?whichchoice=1064&option=1"));
      }

      return Preferences.getBoolean("overgrownLotAvailable");
    }

    // Holiday zones
    if (this.adventureNumber == AdventurePool.SSPD_STUPOR) {
      if (KoLCharacter.getInebriety() < 26) {
        // canAdventure checks this, but let's print a reason now.
        KoLmafia.updateDisplay(MafiaState.ERROR, "You are not drunk enough to continue.");
        return false;
      }
    }

    return true;
  }

  private boolean wearOutfit(int outfitId) {
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

  public int getOutfitId() {
    if (this.formSource.equals("dwarffactory.php")
        || this.adventureNumber == AdventurePool.MINE_OFFICE) {
      return firstAvailableOutfitId(OutfitPool.DWARVISH_UNIFORM, OutfitPool.MINING_OUTFIT);
    }

    if (this.formSource.equals("sea_merkin.php")) {
      switch (this.adventureName) {
        case "Mer-kin Temple" -> {
          // What about Dad Sea Monkee?
          // - Requires Clothing of Loathing
          // - Cannot have defeated Yog-Urt or Shub-Jugguwatt
          return switch (Preferences.getString("merkinQuestPath")) {
            case "gladiator" -> OutfitPool.MER_KIN_GLADIATORIAL_GEAR;
            case "scholar" -> OutfitPool.MER_KIN_SCHOLARS_VESTMENTS;
            default -> 0;
          };
        }
        case "Mer-kin Temple (Left Door)" -> {
          return OutfitPool.MER_KIN_GLADIATORIAL_GEAR;
        }
        case "Mer-kin Temple (Right Door)" -> {
          return OutfitPool.MER_KIN_SCHOLARS_VESTMENTS;
        }
        case "Mer-kin Temple (Center Door)" -> {
          // Must have defeated Yog-Urt and Shub-Jugguwatt
          // No special outfit needed for Her Naughtiness
          return 0;
        }
      }
      return 0;
    }

    return switch (this.adventureNumber) {
      case AdventurePool.FRAT_HOUSE_DISGUISED -> QuestDatabase.isQuestStep(
              Quest.ISLAND_WAR, QuestDatabase.STARTED)
          ?
          // Verge of War
          firstAvailableOutfitId(OutfitPool.WAR_HIPPY_OUTFIT, OutfitPool.HIPPY_OUTFIT)
          :
          // Before or after war
          firstAvailableOutfitId(OutfitPool.WAR_FRAT_OUTFIT, OutfitPool.FRAT_OUTFIT);

      case AdventurePool.WARTIME_FRAT_HOUSE_DISGUISED -> firstAvailableOutfitId(
          OutfitPool.WAR_HIPPY_OUTFIT, OutfitPool.HIPPY_OUTFIT);

      case AdventurePool.HIPPY_CAMP_DISGUISED -> QuestDatabase.isQuestStep(
              Quest.ISLAND_WAR, QuestDatabase.STARTED)
          ?
          // Verge of War
          firstAvailableOutfitId(OutfitPool.WAR_FRAT_OUTFIT, OutfitPool.FRAT_OUTFIT)
          :
          // Before or after war
          firstAvailableOutfitId(OutfitPool.WAR_HIPPY_OUTFIT, OutfitPool.HIPPY_OUTFIT);

      case AdventurePool.WARTIME_HIPPY_CAMP_DISGUISED -> firstAvailableOutfitId(
          OutfitPool.WAR_FRAT_OUTFIT, OutfitPool.FRAT_OUTFIT);

      case AdventurePool.CLOACA_BATTLEFIELD -> availableOutfitId(OutfitPool.CLOACA_UNIFORM);
      case AdventurePool.DYSPEPSI_BATTLEFIELD -> availableOutfitId(OutfitPool.DYSPEPSI_UNIFORM);
      case AdventurePool.FRAT_UNIFORM_BATTLEFIELD -> availableOutfitId(OutfitPool.WAR_FRAT_OUTFIT);
      case AdventurePool.HIPPY_UNIFORM_BATTLEFIELD -> availableOutfitId(
          OutfitPool.WAR_HIPPY_OUTFIT);

      case AdventurePool.MERKIN_ELEMENTARY_SCHOOL,
          AdventurePool.MERKIN_GYMNASIUM -> firstAvailableOutfitId(
          OutfitPool.CRAPPY_MER_KIN_DISGUISE,
          OutfitPool.MER_KIN_SCHOLARS_VESTMENTS,
          OutfitPool.MER_KIN_GLADIATORIAL_GEAR);
      case AdventurePool.MERKIN_LIBRARY -> availableOutfitId(OutfitPool.MER_KIN_SCHOLARS_VESTMENTS);
      case AdventurePool.MERKIN_COLOSSEUM -> availableOutfitId(
          OutfitPool.MER_KIN_GLADIATORIAL_GEAR);

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

    // Check that adventuring in the zone does not require consuming
    // expensive resources
    if (!this.validate0()) {
      if (KoLmafia.permitsContinue()) {
        // validate0 did not give its own error message
        KoLmafia.updateDisplay(MafiaState.ERROR, "That area is not available.");
      }
      return;
    }

    // Check that adventuring the zone is open to us, given level or quest
    // progress, possibly by using inexpensive resources we have on hand
    // (planting a beanstalk, building a dingy dinghy, and so on.)
    if (!this.validate1()) {
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
      // person is not adventuring at valley beyond the chasm.
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
          && !KoLCharacter.currentBooleanModifier(BooleanModifier.ATTACKS_CANT_MISS)
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

    // Perform any of the simple things that we are capable of (beanstalk,
    // dinghy, etc.) that are needed to adventure in the zone.
    if (!this.validate2()) {
      if (KoLmafia.permitsContinue()) {
        // validate2 did not give its own error message
        KoLmafia.updateDisplay(MafiaState.ERROR, "That area is not available.");
      }
      return;
    }

    // All is well. Do it to it!
    RequestThread.postRequest(this.request);
  }

  private static final Pattern ADVENTUREID_PATTERN = Pattern.compile("snarfblat=(\\d+)");
  private static final Pattern MINE_PATTERN = Pattern.compile("mine=(\\d+)");

  public static KoLAdventure setLastAdventure(
      String adventureId, final String adventureName, String adventureURL, final String container) {
    if (container != null) {
      Preferences.setString("lastAdventureContainer", container);
    }

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

  public static void setLastAdventure(final String adventureName) {
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

  public static void setLastAdventure(final KoLAdventure adventure) {
    if (adventure == null) {
      return;
    }

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

  public static void setNextAdventure(final String adventureName) {
    KoLAdventure adventure = AdventureDatabase.getAdventure(adventureName);
    if (adventure == null) {
      Preferences.setString("nextAdventure", adventureName);
      KoLCharacter.updateSelectedLocation(null);
      return;
    }
    KoLAdventure.setNextAdventure(adventure);
    EncounterManager.registerAdventure(adventureName);
  }

  public static void setNextAdventure(final KoLAdventure adventure) {
    if (adventure == null) {
      return;
    }

    Preferences.setString("nextAdventure", adventure.adventureName);
    KoLCharacter.updateSelectedLocation(adventure);
    NamedListenerRegistry.fireChange("(koladventure)");
  }

  public static void clearLocation() {
    KoLAdventure.setLastAdventure("None");
    KoLAdventure.setNextAdventure("None");
  }

  public static KoLAdventure lastVisitedLocation() {
    return KoLAdventure.lastVisitedLocation;
  }

  public static int lastAdventureId() {
    KoLAdventure location = KoLAdventure.lastVisitedLocation;
    if (location == null) {
      return 0;
    }
    // adventure.php locations
    if (location.adventureNumber != -1) {
      return location.adventureNumber;
    }
    // Mines, for example
    if (StringUtilities.isNumeric(location.adventureId)) {
      return StringUtilities.parseInt(location.adventureId);
    }
    // Anything else
    return 0;
  }

  public static String lastAdventureIdString() {
    KoLAdventure location = KoLAdventure.lastVisitedLocation;
    return location == null ? "" : location.adventureId;
  }

  public static boolean recordToSession(final String urlString) {
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
      if (adventure.adventureId.equals("shadow_rift") && urlString.startsWith("place.php")) {
        String place = GenericRequest.getPlace(urlString);
        ShadowRift rift = ShadowRift.findPlace(place);
        if (rift != null) {
          String message = "Entering the Shadow Rift via " + rift.getContainer();
          RequestLogger.printLine(message);
          RequestLogger.updateSessionLog();
          RequestLogger.updateSessionLog(message);
          Preferences.setString("shadowRiftIngress", place);
        }
      }

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

  private static final AdventureResult mop = ItemPool.get(ItemPool.MIZZENMAST_MOP, 1);
  private static final AdventureResult polish = ItemPool.get(ItemPool.BALL_POLISH, 1);
  private static final AdventureResult sham = ItemPool.get(ItemPool.RIGGING_SHAMPOO, 1);

  private void prepareToAdventure(final String urlString) {

    // RequestLogger.registerRequest -> KoLAdventure.recordToSession -> (here)
    //
    // If we are automating we've already been through validate0
    // (preValidateadventure), validate1 (canAdventure), and validate2
    // (prepareForAdventure) for this adventure location.
    //
    // If we are adventuring in the Relay Browser, the user has clicked on
    // something which results in an adventure URL and none of the validation
    // steps have been executed.

    // If we are too drunk adventure, return now.
    if (tooDrunkToAdventure()) return;

    // Unleash Your Inner Wolf redirects to a fight chain that takes 3 turns,
    // regardless of how many fights are actually fought.
    if (this.adventureId.equals("ioty2014_wolf")) {
      Preferences.increment("wolfTurnsUsed", 3);
    }

    switch (this.adventureNumber) {
      case AdventurePool.FCLE:
        // If can complete Cap'n Caronch's chore, do so and get pirate fledges
        if (InventoryManager.hasItem(mop)
            && InventoryManager.hasItem(polish)
            && InventoryManager.hasItem(sham)) {
          RequestThread.postRequest(UseItemRequest.getInstance(mop));
          RequestThread.postRequest(UseItemRequest.getInstance(polish));
          RequestThread.postRequest(UseItemRequest.getInstance(sham));
        }
        break;
      case AdventurePool.DEGRASSI_KNOLL_GARAGE:
        // Visit the untinker before adventuring at Degrassi Knoll.
        UntinkerRequest.canUntinker();
    }

    // If we are automating, we have already validated this adventure.  If we
    // are in the Relay Browser, we clicked an adventure location, so clearly
    // we can go there, but we have not validated it per se.

    // Wear the appropriate equipment for the King's chamber in Cobb's knob, if
    // you can.

    if (this.formSource.equals("cobbsknob.php") && this.canAdventure()) {
      this.prepareForAdventure();
    }
  }

  private static KoLAdventure findAdventure(final String urlString) {
    if (urlString.startsWith("mining.php") && urlString.contains("intro=1")) {
      return null;
    }
    return AdventureDatabase.getAdventureByURL(urlString);
  }

  private static final Pattern ADVENTURE_AGAIN =
      Pattern.compile("<a href=\"([^\"]*)\">Adventure Again \\((.*?)\\)</a>");

  private static KoLAdventure findAdventureAgain(final String responseText) {
    // Look for an "Adventure Again" link and return the
    // KoLAdventure that it matches.
    Matcher matcher = ADVENTURE_AGAIN.matcher(responseText);
    if (!matcher.find()) {
      return null;
    }

    return KoLAdventure.findAdventure(matcher.group(1));
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

    // Mushroom Garden in Kingdom of Exploathing. For some reason.
    new AdventureFailure("You can't go there right now", "You're not allowed to go there."),

    // Site Alpha Dormitory
    //
    // It's getting colder! Better bundle up.
    // The extreme cold makes it impossible for you to continue...
    new AdventureFailure("Better bundle up", "You need more cold resistance."),
    new AdventureFailure("extreme cold makes it impossible", "You need more cold resistance."),
    new AdventureFailure(
        "This zone is too old to visit on this path.", "That zone is out of Standard."),

    // Mer-kin Temple
    // Looks like you've gotta be somebody special to get in there.
    // Even as High Priest of the Mer-kin, they're not gonna let you in dressed like this.
    // Even as the Champion of the Mer-kin Colosseum, they're not gonna let you in dressed like
    // this.
    // The temple is empty.
    new AdventureFailure("you've gotta be somebody special", "You're not allowed to go there."),
    new AdventureFailure(
        "they're not gonna let you in dressed like this", "You're not dressed appropriately."),
    new AdventureFailure("The temple is empty", "Nothing more to do here.", MafiaState.PENDING),

    // Looks like you've gotta be especially gladitorial to get in there.
    // The guards at the temple main door point their spears at you and gesture pointedly with their
    // eyefins at the doors to your left and right.
    // Looks like you've gotta be somebody especially pious to get in there.
    new AdventureFailure(
        "you've gotta be especially gladitorial",
        "You need to wear the Mer-kin Gladiatorial Gear."),
    new AdventureFailure(
        "you've gotta be somebody especially pious",
        "You need to wear the Mer-kin Scholar's Vestments."),
    new AdventureFailure(
        "gesture pointedly with their eyefins at the doors to your left and right",
        "You must defeat the Elder Gods of Hatred and Violence first."),

    // You've already defeated the Trainbot boss.
    new AdventureFailure(
        "You've already defeated the Trainbot boss.",
        "Nothing more to do here.",
        MafiaState.PENDING),
    new AdventureFailure(
        "Looks like peace has broken out in this area",
        "The balance of power has shifted, you can no longer fight here",
        MafiaState.PENDING),

    // CyberRealm zones
    new AdventureFailure(
        "You've already hacked this system.", "Nothing more to do here.", MafiaState.PENDING),

    // Axis HQ
    //
    // If you want Axis HQ access codes, they are the exclusive province of the ladies in the crypto
    // pool. Good luck.
    new AdventureFailure(
        "If you want Axis HQ access codes", "You do not have the Current Axis Codes."),
  };

  private static final Pattern CRIMBO21_COLD_RES =
      Pattern.compile("<b>\\[(\\d+) Cold Resistance Required]</b>");

  public static int findAdventureFailure(String responseText) {
    // KoL is known to sometimes simply return a blank page as a
    // failure to adventure.
    if (responseText.isEmpty()) {
      return 0;
    }

    // Sometimes we want to take some action even on a failure
    if (responseText.contains("There are no Hippy soldiers left")) {
      Preferences.setInteger("hippiesDefeated", 1000);
    } else if (responseText.contains("There are no Frat soldiers left")) {
      Preferences.setInteger("fratboysDefeated", 1000);
    } else if (responseText.contains("Drippy Juice supply")) {
      Preferences.setInteger("drippyJuice", 0);
    } else if (responseText.contains("El Vibrato portal")) {
      Preferences.setInteger("currentPortalEnergy", 0);
      CampgroundRequest.updateElVibratoPortal();
    } else if (responseText.contains("spacegate is out of energy")) {
      Preferences.setInteger("_spacegateTurnsLeft", 0);
    } else if (responseText.contains("Better bundle up")
        || responseText.contains("extreme cold makes it impossible")) {
      Matcher matcher = CRIMBO21_COLD_RES.matcher(responseText);
      if (matcher.find()) {
        int required = Integer.parseInt(matcher.group(1));
        Preferences.setInteger("_crimbo21ColdResistance", required);
      }
    } else if (responseText.contains("Looks like peace has broken out in this area.")) {
      RequestThread.postRequest(new GenericRequest("place.php?whichplace=crimbo23"));
    }

    for (int i = 1; i < ADVENTURE_FAILURES.length; ++i) {
      if (responseText.contains(ADVENTURE_FAILURES[i].responseText)) {
        return i;
      }
    }

    return -1;
  }

  public static String adventureFailureMessage(int index) {
    if (index >= 0 && index < ADVENTURE_FAILURES.length) {
      return ADVENTURE_FAILURES[index].message;
    }

    return null;
  }

  public static MafiaState adventureFailureSeverity(int index) {
    if (index >= 0 && index < ADVENTURE_FAILURES.length) {
      return ADVENTURE_FAILURES[index].severity;
    }

    return MafiaState.ERROR;
  }

  public static boolean recordToSession(final String urlString, final String responseText) {
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

    String message =
        switch (KoLCharacter.getLimitMode()) {
          case SPELUNKY -> "{" + SpelunkyRequest.getTurnsLeft() + "} " + location;
          case BATMAN -> "{" + BatManager.getTimeLeftString() + "} " + location;
          default -> "[" + KoLAdventure.getAdventureCount() + "] " + location;
        };
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

  public static void registerAdventure() {
    switch (KoLAdventure.lastAdventureId()) {
      case AdventurePool.THE_DRIPPING_TREES -> {
        Preferences.increment("dripAdventuresSinceAscension");
        Preferences.increment("drippingTreesAdventuresSinceAscension");
        Preferences.decrement("drippyJuice");
      }
      case AdventurePool.THE_DRIPPING_HALL -> {
        Preferences.increment("dripAdventuresSinceAscension");
        Preferences.decrement("drippyJuice");
      }
    }
  }

  public static int getAdventureCount() {
    return Preferences.getBoolean("logReverseOrder")
        ? KoLCharacter.getAdventuresLeft()
        : KoLCharacter.getCurrentRun() + 1;
  }

  public boolean isUnderwater() {
    return AdventureDatabase.isUnderwater(adventureName);
  }

  @Override
  public int compareTo(final KoLAdventure o) {
    if (o == null) {
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
