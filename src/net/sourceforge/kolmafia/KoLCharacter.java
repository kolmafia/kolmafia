package net.sourceforge.kolmafia;

import java.awt.Taskbar;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import net.java.dev.spellcast.utilities.LockableListModel;
import net.java.dev.spellcast.utilities.SortedListModel;
import net.sourceforge.kolmafia.AscensionPath.Path;
import net.sourceforge.kolmafia.KoLConstants.ConsumptionType;
import net.sourceforge.kolmafia.KoLConstants.Stat;
import net.sourceforge.kolmafia.KoLConstants.WeaponType;
import net.sourceforge.kolmafia.KoLConstants.ZodiacType;
import net.sourceforge.kolmafia.KoLConstants.ZodiacZone;
import net.sourceforge.kolmafia.chat.ChatManager;
import net.sourceforge.kolmafia.equipment.Slot;
import net.sourceforge.kolmafia.equipment.SlotSet;
import net.sourceforge.kolmafia.listener.CharacterListenerRegistry;
import net.sourceforge.kolmafia.listener.NamedListenerRegistry;
import net.sourceforge.kolmafia.listener.PreferenceListenerRegistry;
import net.sourceforge.kolmafia.modifiers.BitmapModifier;
import net.sourceforge.kolmafia.modifiers.BooleanModifier;
import net.sourceforge.kolmafia.modifiers.DerivedModifier;
import net.sourceforge.kolmafia.modifiers.DoubleModifier;
import net.sourceforge.kolmafia.modifiers.Lookup;
import net.sourceforge.kolmafia.modifiers.Modifier;
import net.sourceforge.kolmafia.modifiers.MultiStringModifier;
import net.sourceforge.kolmafia.modifiers.StringModifier;
import net.sourceforge.kolmafia.moods.HPRestoreItemList;
import net.sourceforge.kolmafia.moods.MPRestoreItemList;
import net.sourceforge.kolmafia.objectpool.EffectPool;
import net.sourceforge.kolmafia.objectpool.FamiliarPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.objectpool.OutfitPool;
import net.sourceforge.kolmafia.objectpool.SkillPool;
import net.sourceforge.kolmafia.persistence.*;
import net.sourceforge.kolmafia.persistence.MonsterDatabase.Element;
import net.sourceforge.kolmafia.persistence.QuestDatabase.Quest;
import net.sourceforge.kolmafia.preferences.PreferenceModifiers;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.CampgroundRequest;
import net.sourceforge.kolmafia.request.CharPaneRequest;
import net.sourceforge.kolmafia.request.CharPaneRequest.Companion;
import net.sourceforge.kolmafia.request.CharSheetRequest;
import net.sourceforge.kolmafia.request.ChezSnooteeRequest;
import net.sourceforge.kolmafia.request.ClanLoungeRequest;
import net.sourceforge.kolmafia.request.DwarfFactoryRequest;
import net.sourceforge.kolmafia.request.EquipmentRequest;
import net.sourceforge.kolmafia.request.EquipmentRequest.EquipmentRequestType;
import net.sourceforge.kolmafia.request.FamiliarRequest;
import net.sourceforge.kolmafia.request.FightRequest;
import net.sourceforge.kolmafia.request.FloristRequest;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.request.GuildRequest;
import net.sourceforge.kolmafia.request.HellKitchenRequest;
import net.sourceforge.kolmafia.request.MicroBreweryRequest;
import net.sourceforge.kolmafia.request.QuantumTerrariumRequest;
import net.sourceforge.kolmafia.request.RelayRequest;
import net.sourceforge.kolmafia.request.StandardRequest;
import net.sourceforge.kolmafia.request.StorageRequest;
import net.sourceforge.kolmafia.request.TelescopeRequest;
import net.sourceforge.kolmafia.request.UseItemRequest;
import net.sourceforge.kolmafia.request.UseSkillRequest;
import net.sourceforge.kolmafia.request.coinmaster.HermitRequest;
import net.sourceforge.kolmafia.session.BanishManager;
import net.sourceforge.kolmafia.session.BatManager;
import net.sourceforge.kolmafia.session.ChoiceManager;
import net.sourceforge.kolmafia.session.ClanManager;
import net.sourceforge.kolmafia.session.ContactManager;
import net.sourceforge.kolmafia.session.CrystalBallManager;
import net.sourceforge.kolmafia.session.DisplayCaseManager;
import net.sourceforge.kolmafia.session.EquipmentManager;
import net.sourceforge.kolmafia.session.EventManager;
import net.sourceforge.kolmafia.session.GoalManager;
import net.sourceforge.kolmafia.session.InventoryManager;
import net.sourceforge.kolmafia.session.LimitMode;
import net.sourceforge.kolmafia.session.LocketManager;
import net.sourceforge.kolmafia.session.ResultProcessor;
import net.sourceforge.kolmafia.session.StoreManager;
import net.sourceforge.kolmafia.session.TrackManager;
import net.sourceforge.kolmafia.session.TurnCounter;
import net.sourceforge.kolmafia.session.VioletFogManager;
import net.sourceforge.kolmafia.session.VolcanoMazeManager;
import net.sourceforge.kolmafia.session.WumpusManager;
import net.sourceforge.kolmafia.session.YouRobotManager;
import net.sourceforge.kolmafia.shop.ShopRequest;
import net.sourceforge.kolmafia.swingui.AdventureFrame;
import net.sourceforge.kolmafia.swingui.MallSearchFrame;
import net.sourceforge.kolmafia.swingui.SkillBuffFrame;
import net.sourceforge.kolmafia.swingui.panel.GearChangePanel;
import net.sourceforge.kolmafia.textui.DataFileCache;
import net.sourceforge.kolmafia.textui.command.EudoraCommand.Correspondent;
import net.sourceforge.kolmafia.utilities.FileUtilities;
import net.sourceforge.kolmafia.utilities.LockableListFactory;
import net.sourceforge.kolmafia.webui.DiscoCombatHelper;

/**
 * A container class representing the <code>KoLCharacter</code>. This class also allows for data
 * listeners that are updated whenever the character changes; ultimately, the purpose of this class
 * is to shift away from the centralized-notification paradigm (inefficient) towards a listener
 * paradigm, which is both cleaner and easier to manage with regards to extensions. In addition, it
 * loosens the coupling between the various aspects of <code>KoLmafia</code>, leading to
 * extensibility.
 */
@SuppressWarnings("incomplete-switch")
public abstract class KoLCharacter {
  public enum TurtleBlessing {
    WAR,
    STORM,
    SHE_WHO_WAS,
  }

  public enum TurtleBlessingLevel {
    PARIAH,
    NONE,
    BLESSING,
    GRAND_BLESSING,
    GLORIOUS_BLESSING,
    AVATAR;

    public boolean isBlessing() {
      return switch (this) {
        case BLESSING, GRAND_BLESSING, GLORIOUS_BLESSING -> true;
        default -> false;
      };
    }

    public int boonDuration() {
      return switch (this) {
        case BLESSING -> 5;
        case GRAND_BLESSING -> 10;
        case GLORIOUS_BLESSING -> 15;
        default -> 0;
      };
    }
  }

  public enum Gender {
    UNKNOWN(0),
    MALE(-1),
    FEMALE(1);

    public final int modifierValue;

    Gender(int modifierValue) {
      this.modifierValue = modifierValue;
    }
  }

  // Create this early before subsequent initializers want to look at it.
  private static final Modifiers currentModifiers = new Modifiers();

  // Things that cannot be changed

  private static String username = "";
  private static int userId = 0;
  private static String playerId = "0";

  // Ascension-related variables

  private static boolean isHardcore = false;
  private static boolean isCasual = false;
  private static boolean inRonin = true;
  private static boolean skillsRecalled = false;
  private static boolean restricted = false;

  private static int ascensions = 0;
  private static ZodiacSign ascensionSign = ZodiacSign.NONE;
  private static Path ascensionPath = Path.NONE;

  // Things which can change over the course of playing

  private static List<String> avatar = Collections.emptyList();
  private static String title = "";
  private static AscensionClass ascensionClass = AscensionClass.UNKNOWN;
  private static Gender gender = Gender.UNKNOWN;
  public static int AWOLtattoo = 0;

  private static int currentLevel = 1;

  private static long currentHP, maximumHP, baseMaxHP;
  private static long currentMP, maximumMP, baseMaxMP;

  // Plumbers only
  private static int currentPP, maximumPP;

  // Robots only
  private static int youRobotEnergy, youRobotScraps;

  // Wildfire only
  private static int wildfireWater;

  private static int[] adjustedStats = new int[3];
  private static long[] totalSubpoints = new long[3];
  private static final long[] triggerSubpoints = new long[3];
  private static final int[] triggerItem = new int[3];

  private static int fury = 0;
  private static int soulsauce = 0;
  private static int disco_momentum = 0;
  private static int audience = 0;
  private static int absorbs = 0;

  private static int thunder = 0;
  private static int rain = 0;
  private static int lightning = 0;

  private static int paradoxicity = 0;

  private static String mask = null;

  private static LimitMode limitMode = LimitMode.NONE;

  public static final int MAX_BASEPOINTS = 65535;

  static {
    resetTriggers();
  }

  public static final SortedListModel<String> battleSkillNames = new SortedListModel<>();

  // Status pane data which is rendered whenever
  // the user issues a "status" type command.

  private static int attacksLeft = 0;
  private static long availableMeat = 0;
  private static long storageMeat = 0;
  private static long closetMeat = 0;
  private static long sessionMeat = 0;
  private static int inebriety = 0;
  private static int fullness = 0;
  private static int spleenUse = 0;
  private static int adventuresLeft = 0;
  private static int daycount = 0;
  private static int turnsPlayed = 0;
  private static int currentRun = 0;
  private static long rollover = 0;
  private static int globalDaycount = 0;

  // Travel information

  private static boolean hasStore = true;
  private static boolean hasDisplayCase = true;
  private static boolean hasClan = true;

  // Campground information

  private static boolean hasBookshelf = false;
  private static int telescopeUpgrades = 0;
  private static boolean hippyStoneBroken = false;

  // Familiar data

  // the only usage of this as a LockableListModel is in FamiliarTrainingPane, so filter to usable
  public static final SortedListModel<FamiliarData> familiars =
      new SortedListModel<>(
          element -> {
            var elt = (FamiliarData) element;
            return KoLCharacter.isUsable(elt);
          });
  public static FamiliarData currentFamiliar = FamiliarData.NO_FAMILIAR;
  public static FamiliarData effectiveFamiliar = FamiliarData.NO_FAMILIAR;
  public static String currentFamiliarImage = null;
  public static FamiliarData currentEnthroned = FamiliarData.NO_FAMILIAR;
  public static FamiliarData currentBjorned = FamiliarData.NO_FAMILIAR;
  private static int arenaWins = 0;
  private static boolean isUsingStabBat = false;
  public static FamiliarData[] currentPokeFam =
      new FamiliarData[] {
        FamiliarData.NO_FAMILIAR, FamiliarData.NO_FAMILIAR, FamiliarData.NO_FAMILIAR
      };

  // Minstrel data (Avatar of Boris)
  public static AdventureResult currentInstrument = null;
  public static int minstrelLevel = 0;
  public static boolean minstrelAttention = false;

  // Companion data (Avatar of Jarlsberg)
  private static Companion companion = null;

  // Pastamancer Pasta Thralls

  public static final LockableListModel<PastaThrallData> pastaThralls = new LockableListModel<>();
  public static PastaThrallData currentPastaThrall = PastaThrallData.NO_THRALL;

  public static int stillsAvailable = 0;
  private static boolean tripleReagent = false;
  private static boolean guildStoreStateKnown = false;

  private static KoLAdventure selectedLocation;

  private static int mindControlLevel = 0;
  private static int radSickness = 0;
  private static int autoAttackAction = 0;
  private static String autosellMode = "";
  private static boolean ignoreZoneWarnings = false;
  private static boolean lazyInventory = false;
  private static boolean unequipFamiliar = false;

  private static Correspondent eudora = Correspondent.NONE;

  // Put things that allocate AdventureResult objects AFTER previous
  // static data has been initialized.

  private static final AdventureResult[] WANDS =
      new AdventureResult[] {
        ItemPool.get(ItemPool.PINE_WAND, 1),
        ItemPool.get(ItemPool.EBONY_WAND, 1),
        ItemPool.get(ItemPool.HEXAGONAL_WAND, 1),
        ItemPool.get(ItemPool.ALUMINUM_WAND, 1),
        ItemPool.get(ItemPool.MARBLE_WAND, 1)
      };

  private static final PreferenceModifiers mummeryMods =
      new PreferenceModifiers("_mummeryMods", ModifierType.MUMMERY);
  private static final PreferenceModifiers voteMods =
      new PreferenceModifiers("_voteModifier", ModifierType.LOCAL_VOTE);

  // Status pane data which is rendered whenever
  // the user changes equipment, effects, and familiar

  public static final void reset(final String newUserName) {
    if (newUserName.equals(KoLCharacter.username)) {
      return;
    }

    // Check that character names contain a narrow range of characters. Note
    // that we explicitly allow empty usernames so we can revert to the initial
    // not-logged-in state.
    if (!newUserName.matches("^[a-zA-Z_ 0-9]{3,30}$") && !newUserName.isEmpty()) {
      return;
    }

    // Apparently the CodeQL security scan requires this as a fix...
    if (newUserName.contains("..")) {
      return;
    }

    KoLCharacter.username = newUserName;
    Preferences.reset(KoLCharacter.username);
    KoLCharacter.reset(true);
  }

  public static final void reset(boolean newCharacter) {
    KoLCharacter.ascensionClass = AscensionClass.UNKNOWN;

    KoLCharacter.gender = Gender.UNKNOWN;
    KoLCharacter.currentLevel = 1;

    KoLCharacter.fury = 0;
    KoLCharacter.soulsauce = 0;
    KoLCharacter.disco_momentum = 0;

    KoLCharacter.thunder = 0;
    KoLCharacter.rain = 0;
    KoLCharacter.lightning = 0;
    KoLCharacter.absorbs = 0;
    KoLCharacter.paradoxicity = 0;

    KoLCharacter.mask = null;

    KoLCharacter.adventuresLeft = 0;

    KoLCharacter.attacksLeft = 0;
    KoLCharacter.adjustedStats = new int[3];
    KoLCharacter.totalSubpoints = new long[3];
    KoLCharacter.resetTriggers();

    KoLCharacter.currentModifiers.reset();
    // TODO: do we need to do this? Can we not just reset the passive skill cache?
    ModifierDatabase.resetModifiers();

    KoLConstants.inventory.clear();
    KoLConstants.closet.clear();
    KoLConstants.storage.clear();
    KoLCharacter.storageMeat = 0;
    KoLConstants.freepulls.clear();
    KoLConstants.collection.clear();
    KoLConstants.pulverizeQueue.clear();
    KoLCharacter.sessionMeat = 0;

    KoLCharacter.resetSkills();

    KoLCharacter.isHardcore = false;
    KoLCharacter.isCasual = false;
    KoLCharacter.inRonin = true;
    KoLCharacter.restricted = false;
    KoLCharacter.inebriety = 0;
    KoLCharacter.skillsRecalled = false;
    KoLCharacter.hasStore = false;
    KoLCharacter.hasDisplayCase = false;
    KoLCharacter.hasClan = false;

    KoLCharacter.hasBookshelf = false;
    KoLCharacter.telescopeUpgrades = 0;
    KoLCharacter.hippyStoneBroken = false;

    KoLCharacter.familiars.clear();
    KoLCharacter.familiars.add(FamiliarData.NO_FAMILIAR);
    KoLCharacter.currentFamiliar = FamiliarData.NO_FAMILIAR;
    KoLCharacter.effectiveFamiliar = FamiliarData.NO_FAMILIAR;
    KoLCharacter.currentEnthroned = FamiliarData.NO_FAMILIAR;
    KoLCharacter.currentBjorned = FamiliarData.NO_FAMILIAR;
    KoLCharacter.arenaWins = 0;
    KoLCharacter.isUsingStabBat = false;
    KoLCharacter.companion = null;

    KoLCharacter.currentPastaThrall = PastaThrallData.NO_THRALL;
    KoLCharacter.pastaThralls.clear();
    KoLCharacter.pastaThralls.add(PastaThrallData.NO_THRALL);

    KoLCharacter.stillsAvailable = -1;
    KoLCharacter.tripleReagent = false;
    KoLCharacter.guildStoreStateKnown = false;
    KoLCharacter.AWOLtattoo = 0;

    KoLCharacter.ascensions = 0;
    KoLCharacter.ascensionSign = ZodiacSign.NONE;
    KoLCharacter.ascensionPath = Path.NONE;

    KoLCharacter.mindControlLevel = 0;
    KoLCharacter.radSickness = 0;

    KoLCharacter.autosellMode = "";
    KoLCharacter.lazyInventory = false;
    KoLCharacter.unequipFamiliar = false;
    KoLCharacter.eudora = Correspondent.NONE;

    // Clear some of the standard lists so they don't
    // carry over from player to player.
    GoalManager.clearGoals();
    KoLConstants.recentEffects.clear();
    KoLConstants.activeEffects.clear();

    // Don't reuse NPC food & drink from a previous login
    ChezSnooteeRequest.reset();
    MicroBreweryRequest.reset();
    HellKitchenRequest.reset();

    CrystalBallManager.reset();
    DisplayCaseManager.clearCache();
    DwarfFactoryRequest.reset();
    EquipmentManager.resetEquipment();
    EquipmentManager.resetCustomOutfits();
    GearChangePanel.clearFamiliarList();
    InventoryManager.resetInventory();
    LocketManager.clear();
    SkillDatabase.resetCasts();
    SpecialOutfit.forgetCheckpoints();
    StorageRequest.resetRoninStoragePulls();
    VolcanoMazeManager.reset();
    VYKEACompanionData.initialize(true);
    WumpusManager.reset();

    CoinmasterRegistry.reset();
    NPCStoreDatabase.reset();
    ConcoctionDatabase.resetQueue();
    ConcoctionDatabase.refreshConcoctions();
    ConsumablesDatabase.setVariableConsumables();
    ConsumablesDatabase.calculateAllAverageAdventures();
    DailyLimitDatabase.reset();

    RelayRequest.reset();

    ModifierDatabase.overrideModifier(
        ModifierType.GENERATED, "_userMods", Preferences.getString("_userMods"));

    // Things that don't need to be reset when you ascend
    if (newCharacter) {
      ContactManager.clearMailContacts();
      DataFileCache.clearCache();
      EventManager.clearEventHistory();
      ChatManager.resetChatLiteracy();
      ClanManager.clearCache(true);
      StoreManager.clearCache();
    }
  }

  public static final void resetSkills() {
    KoLConstants.usableSkills.clear();
    KoLConstants.summoningSkills.clear();
    KoLConstants.remedySkills.clear();
    KoLConstants.selfOnlySkills.clear();
    KoLConstants.buffSkills.clear();
    KoLConstants.songSkills.clear();
    KoLConstants.expressionSkills.clear();
    KoLConstants.walkSkills.clear();
    KoLConstants.availableSkills.clear();
    KoLConstants.availableSkillsSet.clear();
    KoLConstants.availableCombatSkillsSet.clear();
    KoLConstants.combatSkills.clear();

    // All characters get the option to
    // attack something.

    KoLCharacter.battleSkillNames.clear();
    KoLCharacter.battleSkillNames.add("attack with weapon");
    KoLCharacter.battleSkillNames.add("custom combat script");
    KoLCharacter.battleSkillNames.add("delevel and plink");

    FightRequest.addItemActionsWithNoCost();

    KoLCharacter.battleSkillNames.add("try to run away");

    int battleIndex = KoLCharacter.battleSkillNames.indexOf(Preferences.getString("battleAction"));
    KoLCharacter.battleSkillNames.setSelectedIndex(battleIndex == -1 ? 0 : battleIndex);

    SkillBuffFrame.update();
  }

  static final void resetPerAscensionData() {
    // This is called after we have read the Charsheet and know how
    // many ascensions the character has completed.

    // Update all data which changes each ascension

    VioletFogManager.reset();
    KoLCharacter.ensureUpdatedAscensionCounters();
    KoLCharacter.ensureUpdatedDwarfFactory();
    KoLCharacter.ensureUpdatedGuyMadeOfBees();
    KoLCharacter.ensureUpdatedPirateInsults();
    KoLCharacter.ensureUpdatedPotionEffects();
    KoLCharacter.ensureUpdatedSkatePark();
    KoLCharacter.ensureUpdatedCellar();
  }

  public static final void setFullness(final int fullness) {
    KoLCharacter.fullness = Math.max(0, fullness);
  }

  public static final int getFullness() {
    return KoLCharacter.fullness;
  }

  public static boolean canExpandStomachCapacity() {
    if (!KoLCharacter.canEat()) return false;
    // Robots can eat size-0 magical sausages but have no fullness
    if (inRobocore()) return false;
    // Spies can eat size-0 magical sausages but have no fullness
    if (inBondcore()) return false;
    // Grey Goo can "eat" things but they don't go into a stomach.
    if (isGreyGoo()) return false;

    // yojimbos_law sez:
    //
    // "The path sets your max fullness to 5, regardless of other modifiers.
    //  Spice melanges and sour balls each clear 3 fullness (and dieting pills
    //  have no interaction with your fullness), so those work.
    //  Pantsgiving increases your max fullness, which is then set to 5,
    //  so it doesn't work. If you somehow got liver or stomach of steel,
    //  those would similarly not work."
    if (isVampyre()) return false;
    // small similarly cannot expand or reduce fullness
    if (inSmallcore()) return false;

    return true;
  }

  private static int getCapacity(
      final Function<AscensionClass, Integer> classCapacity,
      final Function<Path, Integer> pathCapacity,
      final Modifier capacityModifier) {
    Integer baseCapacity = null;

    if (ascensionClass != null) {
      baseCapacity = classCapacity.apply(ascensionClass);
    }

    if (baseCapacity == null) {
      baseCapacity = pathCapacity.apply(ascensionPath);
    }

    return baseCapacity + (int) KoLCharacter.currentNumericModifier(capacityModifier);
  }

  public static int getStomachCapacity() {
    if (!KoLCharacter.canEat()) {
      return 0;
    }

    return getCapacity(
        AscensionClass::getStomachCapacity,
        Path::getStomachCapacity,
        DoubleModifier.STOMACH_CAPACITY);
  }

  public static final void setInebriety(final int inebriety) {
    KoLCharacter.inebriety = inebriety;
  }

  public static final int getInebriety() {
    return KoLCharacter.inebriety;
  }

  public static boolean canExpandLiverCapacity() {
    if (!KoLCharacter.canDrink()) return false;
    // Grey Goo can "drink" things but they don't go into a liver.
    if (isGreyGoo()) return false;

    // yojimbos_law sez:
    //
    // "The path sets your max fullness to 5, regardless of other modifiers.
    //  Spice melanges and sour balls each clear 3 fullness (and dieting pills
    //  have no interaction with your fullness), so those work.
    //  Pantsgiving increases your max fullness, which is then set to 5,
    //  so it doesn't work. If you somehow got liver or stomach of steel,
    //  those would similarly not work."
    if (isVampyre()) return false;
    // small similarly cannot expand or reduce fullness
    if (inSmallcore()) return false;

    return true;
  }

  public static int getLiverCapacity() {
    if (!KoLCharacter.canDrink()) {
      return 0;
    }

    return getCapacity(
        AscensionClass::getLiverCapacity, Path::getLiverCapacity, DoubleModifier.LIVER_CAPACITY);
  }

  public static boolean isFallingDown() {
    return KoLCharacter.getInebriety() > KoLCharacter.getLiverCapacity();
  }

  public static void setSpleenUse(int spleenUse) {
    int value = Math.max(0, spleenUse);
    if (KoLCharacter.spleenUse != value) {
      KoLCharacter.spleenUse = value;
      KoLCharacter.updateStatus();
    }
  }

  public static int getSpleenUse() {
    return KoLCharacter.spleenUse;
  }

  public static boolean canExpandSpleenCapacity() {
    if (!KoLCharacter.canChew()) return false;
    // Grey Goo can "drink" things but they don't go into a liver.
    if (isGreyGoo()) return false;

    return true;
  }

  public static int getSpleenLimit() {
    if (!canChew()) {
      return 0;
    }

    return getCapacity(
        AscensionClass::getSpleenCapacity, Path::getSpleenCapacity, DoubleModifier.SPLEEN_CAPACITY);
  }

  /**
   * Accessor method to retrieve the name of this character.
   *
   * @return The name of this character
   */
  public static final String getUserName() {
    return KoLCharacter.username;
  }

  public static final String baseUserName() {
    return Preferences.baseUserName(KoLCharacter.username);
  }

  /**
   * Accessor method to set the user Id associated with this character.
   *
   * @param userId The user Id associated with this character
   */
  public static final void setUserId(final int userId) {
    KoLCharacter.userId = userId;
    KoLCharacter.playerId = String.valueOf(userId);
    ContactManager.registerPlayerId(KoLCharacter.username, String.valueOf(userId));
  }

  /**
   * Accessor method to retrieve the user Id associated with this character.
   *
   * @return The user Id associated with this character
   */
  public static final String getPlayerId() {
    return KoLCharacter.playerId;
  }

  /**
   * Accessor method to retrieve the user Id associated with this character.
   *
   * @return The user Id associated with this character
   */
  public static final int getUserId() {
    return KoLCharacter.userId;
  }

  /**
   * Setter method to set the avatar associated with this character.
   *
   * @param avatar The avatar for this character
   */
  public static final void setAvatar(final String avatar) {
    KoLCharacter.setAvatar(Collections.singletonList(avatar));
  }

  public static final void setAvatar(final List<String> images) {
    // Only set the avatar if the set of images has changed.
    //
    // Note that we assume that a "set" will do; the images will be overlaid
    // upon each other, rather than being arranged in a specific order.

    if (KoLCharacter.avatar.size() == images.size()) {
      boolean changed = false;
      Set<String> currentImages = new HashSet<>(KoLCharacter.avatar);
      for (String image : images) {
        if (!currentImages.contains(image)) {
          changed = true;
          break;
        }
      }
      if (!changed) {
        return;
      }
    }

    KoLCharacter.avatar = images;

    String prefix = KoLmafia.imageServerPath();
    boolean female = false;

    for (String image : images) {
      if (image.endsWith("_f.gif")) {
        female = true;
      }
      FileUtilities.downloadImage(prefix + image);
    }

    if (female) {
      KoLCharacter.setGender(Gender.FEMALE);
    } else {
      // Unfortunately, lack of '_f' in the avatar doesn't
      // necessarily indicate a male character - it could be a custom
      // avatar, or a special avatar such as Birdform that's unisex.
      KoLCharacter.setGender();
    }

    NamedListenerRegistry.fireChange("(avatar)");
  }

  /**
   * Accessor method to get the avatar associated with this character.
   *
   * @return The avatar for this character
   */
  public static final List<String> getAvatar() {
    return KoLCharacter.avatar;
  }

  /**
   * Setter method to set the title for this character.
   *
   * @param title The title for this character
   */
  public static void setTitle(final String title) {
    KoLCharacter.title = title;
  }

  /**
   * Accessor method to get the title for this character.
   *
   * @return The title for this character
   */
  public static String getTitle() {
    return KoLCharacter.title;
  }

  private static Gender setGender() {
    // If we already know our gender, are in Valhalla (where gender
    // is meaningless), or are not logged in (ditto), nothing to do
    if (KoLCharacter.gender != Gender.UNKNOWN
        || CharPaneRequest.inValhalla()
        || GenericRequest.passwordHash.isEmpty()) {
      return KoLCharacter.gender;
    }

    // Can't tell? Look at their vinyl boots!
    String descId = ItemDatabase.getDescriptionId(ItemPool.VINYL_BOOTS);
    GenericRequest req = new GenericRequest("desc_item.php?whichitem=" + descId);
    RequestThread.postRequest(req);
    if (req.responseText != null) {
      KoLCharacter.gender = req.responseText.contains("+15%") ? Gender.FEMALE : Gender.MALE;
    }

    return KoLCharacter.gender;
  }

  public static final void setGender(final Gender gender) {
    KoLCharacter.gender = gender;
  }

  public static final Gender getGender() {
    return KoLCharacter.setGender();
  }

  /**
   * Accessor method to retrieve the index of the prime stat.
   *
   * @return The index of the prime stat
   */
  public static final int getPrimeIndex() {
    return ascensionClass == AscensionClass.UNKNOWN ? 0 : ascensionClass.getPrimeStatIndex();
  }

  /**
   * Accessor method to retrieve the level of this character.
   *
   * @return The level of this character
   */
  public static final int getLevel() {
    return KoLCharacter.currentLevel;
  }

  /** Set the level of this character. */
  public static final void setLevel(int newLevel) {
    int previousLevel = KoLCharacter.currentLevel;
    KoLCharacter.currentLevel = newLevel;
    if (previousLevel != newLevel) {
      HPRestoreItemList.updateHealthRestored();
      MPRestoreItemList.updateManaRestored();
      ConsumablesDatabase.setLevelVariableConsumables();
    }
  }

  public static final int getFury() {
    return KoLCharacter.fury;
  }

  public static final int getFuryLimit() {
    // 0 if not Seal Clubber, 3 with only Wrath of the Wolverine, 5 with Ire of the Orca in addition
    return (ascensionClass != AscensionClass.SEAL_CLUBBER
            || !KoLCharacter.hasSkill(SkillPool.WRATH_OF_THE_WOLVERINE))
        ? 0
        : KoLCharacter.hasSkill(SkillPool.IRE_OF_THE_ORCA) ? 5 : 3;
  }

  public static final void setFury(final int newFury) {
    int furyLimit = KoLCharacter.getFuryLimit();
    KoLCharacter.fury = newFury > furyLimit ? furyLimit : Math.max(newFury, 0);
  }

  public static final void setFuryNoCheck(final int newFury) {
    KoLCharacter.fury = newFury;
  }

  public static final void resetFury() {
    fury = 0;
  }

  public static final void incrementFury(final int incFury) {
    KoLCharacter.setFury(KoLCharacter.fury + incFury);
  }

  public static final void decrementFury(final int decFury) {
    KoLCharacter.setFury(KoLCharacter.fury - decFury);
  }

  public static final TurtleBlessing getBlessingType() {
    if (KoLConstants.activeEffects.contains(EffectPool.get(EffectPool.BLESSING_OF_THE_WAR_SNAPPER))
        || KoLConstants.activeEffects.contains(
            EffectPool.get(EffectPool.GRAND_BLESSING_OF_THE_WAR_SNAPPER))
        || KoLConstants.activeEffects.contains(
            EffectPool.get(EffectPool.GLORIOUS_BLESSING_OF_THE_WAR_SNAPPER))
        || KoLConstants.activeEffects.contains(
            EffectPool.get(EffectPool.AVATAR_OF_THE_WAR_SNAPPER))) {
      return TurtleBlessing.WAR;
    }
    if (KoLConstants.activeEffects.contains(EffectPool.get(EffectPool.BLESSING_OF_SHE_WHO_WAS))
        || KoLConstants.activeEffects.contains(
            EffectPool.get(EffectPool.GRAND_BLESSING_OF_SHE_WHO_WAS))
        || KoLConstants.activeEffects.contains(
            EffectPool.get(EffectPool.GLORIOUS_BLESSING_OF_SHE_WHO_WAS))
        || KoLConstants.activeEffects.contains(EffectPool.get(EffectPool.AVATAR_OF_SHE_WHO_WAS))) {
      return TurtleBlessing.SHE_WHO_WAS;
    }
    if (KoLConstants.activeEffects.contains(
            EffectPool.get(EffectPool.BLESSING_OF_THE_STORM_TORTOISE))
        || KoLConstants.activeEffects.contains(
            EffectPool.get(EffectPool.GRAND_BLESSING_OF_THE_STORM_TORTOISE))
        || KoLConstants.activeEffects.contains(
            EffectPool.get(EffectPool.GLORIOUS_BLESSING_OF_THE_STORM_TORTOISE))
        || KoLConstants.activeEffects.contains(
            EffectPool.get(EffectPool.AVATAR_OF_THE_STORM_TORTOISE))) {
      return TurtleBlessing.STORM;
    }
    return null;
  }

  public static final TurtleBlessingLevel getBlessingLevel() {
    if (KoLConstants.activeEffects.contains(EffectPool.get(EffectPool.BLESSING_OF_THE_WAR_SNAPPER))
        || KoLConstants.activeEffects.contains(EffectPool.get(EffectPool.BLESSING_OF_SHE_WHO_WAS))
        || KoLConstants.activeEffects.contains(
            EffectPool.get(EffectPool.BLESSING_OF_THE_STORM_TORTOISE))) {
      return TurtleBlessingLevel.BLESSING;
    }
    if (KoLConstants.activeEffects.contains(
            EffectPool.get(EffectPool.GRAND_BLESSING_OF_THE_WAR_SNAPPER))
        || KoLConstants.activeEffects.contains(
            EffectPool.get(EffectPool.GRAND_BLESSING_OF_SHE_WHO_WAS))
        || KoLConstants.activeEffects.contains(
            EffectPool.get(EffectPool.GRAND_BLESSING_OF_THE_STORM_TORTOISE))) {
      return TurtleBlessingLevel.GRAND_BLESSING;
    }
    if (KoLConstants.activeEffects.contains(
            EffectPool.get(EffectPool.GLORIOUS_BLESSING_OF_THE_WAR_SNAPPER))
        || KoLConstants.activeEffects.contains(
            EffectPool.get(EffectPool.GLORIOUS_BLESSING_OF_SHE_WHO_WAS))
        || KoLConstants.activeEffects.contains(
            EffectPool.get(EffectPool.GLORIOUS_BLESSING_OF_THE_STORM_TORTOISE))) {
      return TurtleBlessingLevel.GLORIOUS_BLESSING;
    }
    if (KoLConstants.activeEffects.contains(EffectPool.get(EffectPool.AVATAR_OF_THE_WAR_SNAPPER))
        || KoLConstants.activeEffects.contains(EffectPool.get(EffectPool.AVATAR_OF_SHE_WHO_WAS))
        || KoLConstants.activeEffects.contains(
            EffectPool.get(EffectPool.AVATAR_OF_THE_STORM_TORTOISE))) {
      return TurtleBlessingLevel.AVATAR;
    }
    if (KoLConstants.activeEffects.contains(EffectPool.get(EffectPool.SPIRIT_PARIAH))) {
      return TurtleBlessingLevel.PARIAH;
    }
    return TurtleBlessingLevel.NONE;
  }

  public static final int getSoulsauce() {
    return KoLCharacter.soulsauce;
  }

  public static final void setSoulsauce(final int newSoulsauce) {
    KoLCharacter.soulsauce = Math.max(newSoulsauce, 0);
  }

  public static final void resetSoulsauce() {
    KoLCharacter.soulsauce = 0;
  }

  public static final void incrementSoulsauce(final int incSoulsauce) {
    KoLCharacter.setSoulsauce(KoLCharacter.soulsauce + incSoulsauce);
  }

  public static final void decrementSoulsauce(final int decSoulsauce) {
    KoLCharacter.setSoulsauce(KoLCharacter.soulsauce - decSoulsauce);
  }

  public static final int getDiscoMomentum() {
    return KoLCharacter.disco_momentum;
  }

  public static final void setDiscoMomentum(final int newDiscoMomentum) {
    KoLCharacter.disco_momentum = newDiscoMomentum;
  }

  public static final void resetDiscoMomentum() {
    disco_momentum = 0;
  }

  public static final int getMaxSongs() {
    return (currentBooleanModifier(BooleanModifier.FOUR_SONGS) ? 4 : 3)
        + (int) currentNumericModifier(DoubleModifier.ADDITIONAL_SONG);
  }

  public static final int getSongs() {
    return (int)
        KoLConstants.activeEffects.stream()
            .filter(e -> EffectDatabase.isSong(e.getEffectId()))
            .count();
  }

  public static final int getAudience() {
    return KoLCharacter.audience;
  }

  public static final int getAudienceLimit() {
    return (KoLCharacter.hasEquipped(ItemPool.PETE_JACKET, Slot.SHIRT)
            || KoLCharacter.hasEquipped(ItemPool.PETE_JACKET_COLLAR, Slot.SHIRT))
        ? 50
        : 30;
  }

  public static final void setAudience(final int newAudience) {
    int limit = KoLCharacter.getAudienceLimit();
    KoLCharacter.audience = newAudience > limit ? limit : Math.max(newAudience, -limit);
  }

  public static final void incrementAudience(final int incAudience) {
    KoLCharacter.setAudience(KoLCharacter.audience + incAudience);
  }

  public static final void decrementAudience(final int decAudience) {
    KoLCharacter.setAudience(KoLCharacter.audience - decAudience);
  }

  public static final int getAbsorbs() {
    return KoLCharacter.absorbs;
  }

  public static final int getAbsorbsLimit() {
    int level = KoLCharacter.getLevel();
    return level > 12 ? 15 : level + 2;
  }

  public static final void setAbsorbs(final int newAbsorbs) {
    int limit = KoLCharacter.getAbsorbsLimit();
    KoLCharacter.absorbs = newAbsorbs > limit ? limit : Math.max(newAbsorbs, 0);
    // Temporary historical support
    Preferences.setInteger("_noobSkillCount", KoLCharacter.absorbs);
  }

  public static final void incrementAbsorbs(final int incAbsorbs) {
    KoLCharacter.setAbsorbs(KoLCharacter.absorbs + incAbsorbs);
  }

  public static final void decrementAbsorbs(final int decAbsorbs) {
    KoLCharacter.setAbsorbs(KoLCharacter.absorbs - decAbsorbs);
  }

  public static final int getThunder() {
    return KoLCharacter.thunder;
  }

  public static final void setThunder(final int newThunder) {
    KoLCharacter.thunder = newThunder > 100 ? 100 : Math.max(newThunder, 0);
  }

  public static final void resetThunder() {
    KoLCharacter.thunder = 0;
  }

  public static final void incrementThunder(final int incThunder) {
    KoLCharacter.setThunder(KoLCharacter.thunder + incThunder);
  }

  public static final void decrementThunder(final int decThunder) {
    KoLCharacter.setThunder(KoLCharacter.thunder - decThunder);
  }

  public static final int getRain() {
    return KoLCharacter.rain;
  }

  public static final void setRain(final int newRain) {
    KoLCharacter.rain = newRain > 100 ? 100 : Math.max(newRain, 0);
  }

  public static final void incrementRain(final int incRain) {
    KoLCharacter.setRain(KoLCharacter.rain + incRain);
  }

  public static final void decrementRain(final int decRain) {
    KoLCharacter.setRain(KoLCharacter.rain - decRain);
  }

  public static final int getLightning() {
    return KoLCharacter.lightning;
  }

  public static final void setLightning(final int newLightning) {
    KoLCharacter.lightning = newLightning > 100 ? 100 : Math.max(newLightning, 0);
  }

  public static final void incrementLightning(final int incLightning) {
    KoLCharacter.setLightning(KoLCharacter.lightning + incLightning);
  }

  public static final void decrementLightning(final int decLightning) {
    KoLCharacter.setLightning(KoLCharacter.lightning - decLightning);
  }

  public static final String getMask() {
    return KoLCharacter.mask;
  }

  public static final void setMask(final String newMask) {
    KoLCharacter.mask = newMask;
  }

  public static final int getParadoxicity() {
    return KoLCharacter.paradoxicity;
  }

  public static final void setParadoxicity(final int newParadoxicity) {
    KoLCharacter.paradoxicity = newParadoxicity;
  }

  public static final int getAttacksLeft() {
    return KoLCharacter.attacksLeft;
  }

  public static final void setAttacksLeft(final int attacksLeft) {
    KoLCharacter.attacksLeft = attacksLeft;
    KoLCharacter.updateStatus();
  }

  public static final void setEudora(final Correspondent eudora) {
    KoLCharacter.eudora = eudora;
  }

  public static final void setEudora(final int eudoraId) {
    KoLCharacter.eudora = Correspondent.find(eudoraId);
  }

  public static final Correspondent getEudora() {
    return KoLCharacter.eudora;
  }

  /**
   * Accessor method to set the character's class.
   *
   * @param ascensionClass The name of the character's class
   */
  public static final void setAscensionClass(final AscensionClass ascensionClass) {
    if (KoLCharacter.ascensionClass == ascensionClass) {
      return;
    }

    KoLCharacter.ascensionClass = ascensionClass;

    KoLCharacter.tripleReagent = isSauceror();

    if (ascensionClass == AscensionClass.ASTRAL_SPIRIT) {
      return;
    }

    ConcoctionDatabase.resetConcoctionStatGains();

    // Allow or disallow special fight actions
    FightRequest.initialize();
  }

  public static final void setAscensionClass(final int classId) {
    setAscensionClass(AscensionClass.find(classId));
  }

  public static final void setAscensionClass(final String className) {
    setAscensionClass(AscensionClass.find(className));
  }

  static final int getReagentPotionDuration() {
    return 5 + (hasSkill(SkillPool.IMPETUOUS_SAUCINESS) ? 5 : 0) + (isSauceror() ? 5 : 0);
  }

  /**
   * Accessor method to retrieve the name of the character's class.
   *
   * @return The name of the character's class
   */
  public static final AscensionClass getAscensionClass() {
    return ascensionClass;
  }

  public static final String getAscensionClassName() {
    return ascensionClass == null ? "none" : ascensionClass.getName();
  }

  public static final boolean isSealClubber() {
    return ascensionClass == AscensionClass.SEAL_CLUBBER;
  }

  public static final boolean isTurtleTamer() {
    return ascensionClass == AscensionClass.TURTLE_TAMER;
  }

  public static final boolean isSauceror() {
    return ascensionClass == AscensionClass.SAUCEROR;
  }

  public static final boolean isPastamancer() {
    return ascensionClass == AscensionClass.PASTAMANCER;
  }

  public static final boolean isAccordionThief() {
    return ascensionClass == AscensionClass.ACCORDION_THIEF;
  }

  public static final boolean isDiscoBandit() {
    return ascensionClass == AscensionClass.DISCO_BANDIT;
  }

  public static final boolean isMuscleClass() {
    return ascensionClass != null && ascensionClass.getPrimeStatIndex() == 0;
  }

  public static final boolean isAvatarOfBoris() {
    return ascensionClass == AscensionClass.AVATAR_OF_BORIS;
  }

  public static final boolean isZombieMaster() {
    return ascensionClass == AscensionClass.ZOMBIE_MASTER;
  }

  public static final boolean isMysticalityClass() {
    return ascensionClass != null && ascensionClass.getPrimeStatIndex() == 1;
  }

  public static final boolean isVampyre() {
    return ascensionClass == AscensionClass.VAMPYRE;
  }

  public static final boolean isGreyGoo() {
    return ascensionClass == AscensionClass.GREY_GOO;
  }

  public static final boolean isMoxieClass() {
    return ascensionClass != null && ascensionClass.getPrimeStatIndex() == 2;
  }

  public static final boolean isAWoLClass() {
    return ascensionClass == AscensionClass.COW_PUNCHER
        || ascensionClass == AscensionClass.BEANSLINGER
        || ascensionClass == AscensionClass.SNAKE_OILER;
  }

  public static final Stat mainStat() {
    return ascensionClass == null ? Stat.NONE : ascensionClass.getMainStat();
  }

  public static final AdventureResult ASTRAL = EffectPool.get(EffectPool.HALF_ASTRAL);
  public static final AdventureResult DIRTY_PEAR = EffectPool.get(EffectPool.DIRTY_PEAR);
  public static final AdventureResult BENDIN_HELL = EffectPool.get(EffectPool.BENDIN_HELL);
  public static final AdventureResult BOWLEGGED_SWAGGER =
      EffectPool.get(EffectPool.BOWLEGGED_SWAGGER);
  public static final AdventureResult STEELY_EYED_SQUINT =
      EffectPool.get(EffectPool.STEELY_EYED_SQUINT);
  public static final AdventureResult OFFHAND_REMARKABLE =
      EffectPool.get(EffectPool.OFFHAND_REMARKABLE);

  public static void setLimitMode(final LimitMode limitmode) {
    switch (limitmode) {
      case NONE -> {
        // Check for "pseudo" LimitModes - when certain effects are active,
        // some of your options - adventuring zones or combat skills - are
        // restricted

        switch (Preferences.getString("currentLlamaForm")) {
          case "Bird" -> {
            KoLCharacter.limitMode = LimitMode.BIRD;
            return;
          }
          case "Roach" -> {
            KoLCharacter.limitMode = LimitMode.ROACH;
            return;
          }
          case "Mole" -> {
            KoLCharacter.limitMode = LimitMode.MOLE;
            return;
          }
        }

        if (KoLConstants.activeEffects.contains(ASTRAL)) {
          KoLCharacter.limitMode = LimitMode.ASTRAL;
          return;
        }

        // The LimitMode can cleanup after itself without making requests
        KoLCharacter.limitMode.finish();

        // If it does require making requests, can't do it in a fight or choice
        if (KoLCharacter.limitMode.requiresReset()
            && !GenericRequest.abortIfInFightOrChoice(true)) {
          KoLmafia.resetAfterLimitmode();
        }
      }
      case BATMAN -> BatManager.setCombatSkills();
    }

    KoLCharacter.limitMode = limitmode;
  }

  public static void setLimitMode(final String name) {
    setLimitMode(LimitMode.find(name));
  }

  public static LimitMode getLimitMode() {
    return KoLCharacter.limitMode;
  }

  public static void enterLimitmode(final LimitMode limitmode) {
    if (!limitmode.requiresReset()) {
      return;
    }

    KoLCharacter.limitMode = limitmode;

    KoLCharacter.resetSkills();
    EquipmentManager.removeAllEquipment();
    KoLCharacter.familiars.clear();
    KoLCharacter.familiars.add(FamiliarData.NO_FAMILIAR);
    KoLCharacter.currentFamiliar = FamiliarData.NO_FAMILIAR;
    KoLCharacter.effectiveFamiliar = FamiliarData.NO_FAMILIAR;
    KoLCharacter.currentEnthroned = FamiliarData.NO_FAMILIAR;
    KoLCharacter.currentBjorned = FamiliarData.NO_FAMILIAR;
    KoLCharacter.isUsingStabBat = false;
    KoLCharacter.companion = null;
    KoLCharacter.currentPastaThrall = PastaThrallData.NO_THRALL;
    KoLCharacter.pastaThralls.clear();
    KoLCharacter.pastaThralls.add(PastaThrallData.NO_THRALL);
    KoLCharacter.stillsAvailable = -1;
    KoLCharacter.mindControlLevel = 0;
    KoLCharacter.radSickness = 0;
    KoLConstants.recentEffects.clear();
    KoLConstants.activeEffects.clear();
    ChezSnooteeRequest.reset();
    MicroBreweryRequest.reset();
    HellKitchenRequest.reset();
    GearChangePanel.clearFamiliarList();
    InventoryManager.refresh();
    EquipmentManager.resetCustomOutfits();
    SkillBuffFrame.update();

    limitmode.reset();

    KoLCharacter.recalculateAdjustments();
    KoLCharacter.updateStatus();
  }

  /**
   * Accessor method to set the character's current health state.
   *
   * @param currentHP The character's current HP value
   * @param maximumHP The character's maximum HP value
   * @param baseMaxHP The base value for the character's maximum HP
   */
  public static final void setHP(final long currentHP, final long maximumHP, final long baseMaxHP) {
    KoLCharacter.currentHP = currentHP < 0 ? 0 : Math.min(currentHP, maximumHP);
    KoLCharacter.maximumHP = maximumHP;
    KoLCharacter.baseMaxHP = baseMaxHP;

    KoLCharacter.updateStatus();
  }

  /**
   * Accessor method to retrieve the character's current HP.
   *
   * @return The character's current HP
   */
  public static final long getCurrentHP() {
    return KoLCharacter.currentHP;
  }

  /**
   * Accessor method to retrieve the character's maximum HP.
   *
   * @return The character's maximum HP
   */
  public static final long getMaximumHP() {
    return KoLCharacter.maximumHP;
  }

  /**
   * Accessor method to retrieve the base value for the character's maximum HP.
   *
   * @return The base value for the character's maximum HP
   */
  public static final long getBaseMaxHP() {
    return KoLCharacter.baseMaxHP;
  }

  /**
   * Accessor method to set the character's current mana limits.
   *
   * @param currentMP The character's current MP value
   * @param maximumMP The character's maximum MP value
   * @param baseMaxMP The base value for the character's maximum MP
   */
  public static final void setMP(final long currentMP, final long maximumMP, final long baseMaxMP) {
    KoLCharacter.currentMP = currentMP < 0 ? 0 : Math.min(currentMP, maximumMP);
    KoLCharacter.maximumMP = maximumMP;
    KoLCharacter.baseMaxMP = baseMaxMP;

    KoLCharacter.updateStatus();
  }

  /**
   * Accessor method to retrieve the character's current MP.
   *
   * @return The character's current MP
   */
  public static final long getCurrentMP() {
    return KoLCharacter.currentMP;
  }

  /**
   * Accessor method to retrieve the character's maximum MP.
   *
   * @return The character's maximum MP
   */
  public static final long getMaximumMP() {
    return KoLCharacter.maximumMP;
  }

  /**
   * Accessor method to retrieve the base value for the character's maximum MP.
   *
   * @return The base value for the character's maximum MP
   */
  public static final long getBaseMaxMP() {
    return KoLCharacter.baseMaxMP;
  }

  /**
   * Accessor method to set the character's current energy for the "You, Robot" path
   *
   * @param youRobotEnergy The character's current energy
   */
  public static final void setYouRobotEnergy(final int youRobotEnergy) {
    KoLCharacter.youRobotEnergy = youRobotEnergy;

    KoLCharacter.updateStatus();
  }

  /**
   * Accessor method to retrieve the character's current energy for the "You, Robot" path
   *
   * @return The character's energy
   */
  public static final int getYouRobotEnergy() {
    return KoLCharacter.youRobotEnergy;
  }

  /**
   * Accessor method to set the character's current scraps for the "You, Robot" path
   *
   * @param youRobotScraps The character's current scraps
   */
  public static final void setYouRobotScraps(final int youRobotScraps) {
    KoLCharacter.youRobotScraps = youRobotScraps;

    KoLCharacter.updateStatus();
  }

  /**
   * Accessor method to retrieve the character's current scraps for the "You, Robot" path
   *
   * @return The character's scraps
   */
  public static final int getYouRobotScraps() {
    return KoLCharacter.youRobotScraps;
  }

  /**
   * Accessor method to set the character's current power points
   *
   * @param currentPP The character's current PP value
   * @param maximumPP The character's maximum PP value
   */
  public static final void setPP(final int currentPP, final int maximumPP) {
    KoLCharacter.currentPP = currentPP;
    KoLCharacter.maximumPP = maximumPP;

    KoLCharacter.updateStatus();
  }

  /**
   * Accessor method to retrieve the character's current PP.
   *
   * @return The character's current PP
   */
  public static final int getCurrentPP() {
    return KoLCharacter.currentPP;
  }

  /**
   * Accessor method to retrieve the character's maximum PP.
   *
   * @return The character's maximum PP
   */
  public static final int getMaximumPP() {
    return KoLCharacter.maximumPP;
  }

  /**
   * Accessor method to retrieve the character's current water for the "Wildfire" path
   *
   * @return The character's water
   */
  public static final int getWildfireWater() {
    return KoLCharacter.wildfireWater;
  }

  /**
   * Accessor method to set the character's current water for the "Wildfire" path
   *
   * @param wildfireWater The character's current water
   */
  public static final void setWildfireWater(final int wildfireWater) {
    KoLCharacter.wildfireWater = wildfireWater;

    KoLCharacter.updateStatus();
  }

  public static final int calculateMaximumPP() {
    return 1 + (int) KoLCharacter.currentModifiers.getDouble(DoubleModifier.PP);
  }

  public static final void resetCurrentPP() {
    KoLCharacter.currentPP = KoLCharacter.maximumPP = calculateMaximumPP();
  }

  public static final void deltaPP(final int delta) {
    KoLCharacter.currentPP += delta;
    KoLCharacter.maximumPP += delta;
  }

  public static final void spendPP(int pp) {
    KoLCharacter.currentPP -= pp;
  }

  /**
   * Accessor method to retrieve the amount of meat in Hagnk's storage.
   *
   * @return The amount of meat in storage.
   */
  public static final long getStorageMeat() {
    return KoLCharacter.storageMeat;
  }

  public static final void setStorageMeat(final long storageMeat) {
    if (KoLCharacter.storageMeat != storageMeat) {
      KoLCharacter.storageMeat = storageMeat;
      MallSearchFrame.updateMeat();
    }
  }

  public static final void addStorageMeat(final long meat) {
    if (meat != 0) {
      KoLCharacter.storageMeat += meat;
      MallSearchFrame.updateMeat();
    }
  }

  /**
   * Accessor method to retrieve the amount of meat in the character's closet.
   *
   * @return The amount of meat in the character's closet.
   */
  public static final long getClosetMeat() {
    return KoLCharacter.closetMeat;
  }

  public static final void setClosetMeat(final long closetMeat) {
    KoLCharacter.closetMeat = closetMeat;
  }

  /**
   * Accessor method to retrieve the amount of meat gained or lost this session. This will not
   * include meat gained from mall sales or kmail.
   *
   * @return The amount of meat gained or lost this session
   */
  public static final long getSessionMeat() {
    return KoLCharacter.sessionMeat;
  }

  public static final void clearSessionMeat() {
    KoLCharacter.sessionMeat = 0;
  }

  public static final void incrementSessionMeat(final long delta) {
    KoLCharacter.sessionMeat += delta;
  }

  /**
   * Accessor method to set the character's current available meat for spending (IE: meat that isn't
   * currently in the character's closet).
   *
   * @param availableMeat The character's available meat for spending
   */
  public static final void setAvailableMeat(final long availableMeat) {
    if (KoLCharacter.availableMeat != availableMeat) {
      KoLCharacter.availableMeat = availableMeat;
      MallSearchFrame.updateMeat();
    }
  }

  /**
   * Accessor method to retrieve the character's current available meat for spending (IE: meat that
   * isn't currently in the character's closet).
   *
   * @return The character's available meat for spending
   */
  public static final long getAvailableMeat() {
    return KoLCharacter.getLimitMode().limitMeat() ? 0 : KoLCharacter.availableMeat;
  }

  public static int freeRestsAvailable() {
    return (int) KoLCharacter.currentNumericModifier(DoubleModifier.FREE_RESTS);
  }

  public static int freeRestsRemaining() {
    int restsUsed = Preferences.getInteger("timesRested");
    int restsAvailable = KoLCharacter.freeRestsAvailable();
    return Math.max(0, restsAvailable - restsUsed);
  }

  // If there are free rests remaining and KoLmafia thinks there are not, update that value
  // so it will be correct for the next rest at least
  public static void updateFreeRests(final boolean freeRestsRemain) {
    int restsUsed = Preferences.getInteger("timesRested");
    int restsAvailable = KoLCharacter.freeRestsAvailable();
    if (freeRestsRemain && restsUsed >= restsAvailable) {
      if (restsAvailable == 0) {
        RequestLogger.updateSessionLog(
            "You have free rests available but KoLmafia thought you had none.");
        RequestLogger.printLine("You have free rests available but KoLmafia thought you had none.");
      } else {
        Preferences.setInteger("timesRested", restsAvailable - 1);
      }
    }
    if (!freeRestsRemain && restsUsed < restsAvailable) {
      Preferences.setInteger("timesRested", restsAvailable);
    }
  }

  public static void setMuscle(final int adjustedMuscle, final long totalMuscle) {
    KoLCharacter.adjustedStats[0] = adjustedMuscle;
    KoLCharacter.totalSubpoints[0] = totalMuscle;

    if (totalMuscle >= KoLCharacter.triggerSubpoints[0]) {
      KoLCharacter.handleTrigger(KoLCharacter.triggerItem[0]);
    }
  }

  public static void setMysticality(final int adjustedMysticality, final long totalMysticality) {
    KoLCharacter.adjustedStats[1] = adjustedMysticality;
    KoLCharacter.totalSubpoints[1] = totalMysticality;

    if (totalMysticality >= KoLCharacter.triggerSubpoints[1]) {
      KoLCharacter.handleTrigger(KoLCharacter.triggerItem[1]);
    }
  }

  public static void setMoxie(final int adjustedMoxie, final long totalMoxie) {
    KoLCharacter.adjustedStats[2] = adjustedMoxie;
    KoLCharacter.totalSubpoints[2] = totalMoxie;

    if (totalMoxie >= KoLCharacter.triggerSubpoints[2]) {
      KoLCharacter.handleTrigger(KoLCharacter.triggerItem[2]);
    }
  }

  /**
   * Sets the character's current stat values. Each parameter in the list comes in pairs: the
   * adjusted value (based on equipment and spell effects) and the total number of subpoints
   * acquired through adventuring for that statistic. This is preferred over the character's current
   * base and/or distance from base as it allows for more accurate reporting of statistic gains and
   * losses, as statistic losses are not reported by KoL.
   *
   * @param adjustedMuscle The adjusted value for the character's muscle
   * @param totalMuscle The total number of muscle subpoints acquired thus far
   * @param adjustedMysticality The adjusted value for the character's mysticality
   * @param totalMysticality The total number of mysticality subpoints acquired thus far
   * @param adjustedMoxie The adjusted value for the character's moxie
   * @param totalMoxie The total number of moxie subpoints acquired thus far
   */
  public static final void setStatPoints(
      final int adjustedMuscle,
      final long totalMuscle,
      final int adjustedMysticality,
      final long totalMysticality,
      final int adjustedMoxie,
      final long totalMoxie) {
    setMuscle(adjustedMuscle, totalMuscle);
    setMysticality(adjustedMysticality, totalMysticality);
    setMoxie(adjustedMoxie, totalMoxie);
  }

  public static final void resetTriggers() {
    KoLCharacter.triggerSubpoints[0] = Long.MAX_VALUE;
    KoLCharacter.triggerSubpoints[1] = Long.MAX_VALUE;
    KoLCharacter.triggerSubpoints[2] = Long.MAX_VALUE;
  }

  public static final void handleTrigger(int itemId) {
    KoLmafia.updateDisplay(
        "You can now equip a "
            + ItemDatabase.getItemName(itemId)
            + " (and possibly other things).");
    EquipmentManager.updateEquipmentLists();
    PreferenceListenerRegistry.firePreferenceChanged("(equippable)");
  }

  public static final int getTriggerItem(int stat) {
    return KoLCharacter.triggerItem[stat];
  }

  public static final int getTriggerPoints(int stat) {
    return KoLCharacter.calculateBasePoints(KoLCharacter.triggerSubpoints[stat]);
  }

  /**
   * Utility method for calculating how many subpoints are need to reach a specified full point
   *
   * @param basePoints The desired point
   * @return The calculated subpoints
   */
  public static final long calculatePointSubpoints(final int basePoints) {
    return basePoints * (long) basePoints;
  }

  /**
   * Utility method for calculating how many actual points are associated with the given number of
   * subpoints.
   *
   * @param subpoints The total number of subpoints accumulated
   * @return The base points associated with the subpoint value
   */
  public static final int calculateBasePoints(final long subpoints) {
    return Math.min(KoLCharacter.MAX_BASEPOINTS, (int) Math.sqrt(subpoints));
  }

  /**
   * Utility method for calculating how many points are need to reach a specified character level.
   *
   * @param level The character level
   * @return The calculated points
   */
  private static int calculateLevelPoints(final int level) {
    return (level == 1) ? 0 : (level - 1) * (level - 1) + 4;
  }

  /**
   * Utility method for calculating how many subpoints are need to reach a specified character
   * level.
   *
   * @param level The character level
   * @return The calculated subpoints
   */
  private static long calculateLevelSubpoints(final int level) {
    return KoLCharacter.calculatePointSubpoints(KoLCharacter.calculateLevelPoints(level));
  }

  /**
   * Utility method for calculating what character level is associated with the given number of
   * points.
   *
   * @param points The total number of points accumulated
   * @return The calculated level
   */
  private static int calculatePointLevels(final int points) {
    return (int) Math.sqrt(Math.max(points - 4, 0)) + 1;
  }

  /**
   * Utility method for calculating what character level is associated with the given number of
   * subpoints.
   *
   * @param subpoints The total number of subpoints accumulated
   * @return The calculated level
   */
  public static final int calculateSubpointLevels(final long subpoints) {
    return KoLCharacter.calculatePointLevels(KoLCharacter.calculateBasePoints(subpoints));
  }

  /**
   * Utility method for calculating how many subpoints have been accumulated thus far, given the
   * current base point value of the statistic and how many have been accumulate since the last
   * gain.
   *
   * @param baseValue The current base point value
   * @param sinceLastBase Number of subpoints accumulate since the last base point gain
   * @return The total number of subpoints acquired since creation
   */
  public static final long calculateSubpoints(final int baseValue, final int sinceLastBase) {
    return KoLCharacter.calculatePointSubpoints(baseValue) + sinceLastBase;
  }

  /**
   * Returns the total number of subpoints to the current level.
   *
   * @return The total subpoints to the current level
   */
  public static final long calculateLastLevel() {
    return KoLCharacter.calculateLevelSubpoints(KoLCharacter.currentLevel);
  }

  /**
   * Returns the total number of subpoints to the next level.
   *
   * @return The total subpoints to the next level
   */
  public static final long calculateNextLevel() {
    return KoLCharacter.calculateLevelSubpoints(KoLCharacter.currentLevel + 1);
  }

  /**
   * Returns the total number of subpoints acquired in the prime stat.
   *
   * @return The total subpoints in the prime stat
   */
  public static final long getTotalPrime() {
    return KoLCharacter.totalSubpoints[KoLCharacter.getPrimeIndex()];
  }

  /**
   * Utility method to calculate the "till next point" value, given the total number of subpoints
   * accumulated.
   */
  private static int calculateTillNextPoint(final long subpoints) {
    return (int)
        (KoLCharacter.calculatePointSubpoints(KoLCharacter.calculateBasePoints(subpoints) + 1)
            - subpoints);
  }

  /**
   * Accessor method to retrieve the character's base value for muscle.
   *
   * @return The character's base value for muscle
   */
  public static final int getBaseMuscle() {
    return KoLCharacter.calculateBasePoints(KoLCharacter.totalSubpoints[0]);
  }

  /**
   * Accessor method to retrieve the total subpoints accumulated so far in muscle.
   *
   * @return The total muscle subpoints so far
   */
  public static final long getTotalMuscle() {
    return KoLCharacter.totalSubpoints[0];
  }

  public static final void incrementTotalMuscle(int increment) {
    KoLCharacter.totalSubpoints[0] += increment;
    if (KoLCharacter.totalSubpoints[0] >= KoLCharacter.triggerSubpoints[0]) {
      KoLCharacter.handleTrigger(KoLCharacter.triggerItem[0]);
    }
  }

  public static final boolean muscleTrigger(int basepoints, int itemId) {
    long points = calculatePointSubpoints(basepoints);
    if (points < KoLCharacter.triggerSubpoints[0]) {
      KoLCharacter.triggerSubpoints[0] = points;
      KoLCharacter.triggerItem[0] = itemId;
    }
    return false; // for the convenience of the caller
  }

  /**
   * Accessor method to retrieve the number of subpoints required before the character gains another
   * full point of muscle.
   */
  public static final int getMuscleTNP() {
    return KoLCharacter.calculateTillNextPoint(KoLCharacter.totalSubpoints[0]);
  }

  /**
   * Accessor method to retrieve the character's adjusted value for muscle.
   *
   * @return The character's adjusted value for muscle
   */
  public static final int getAdjustedMuscle() {
    return KoLCharacter.adjustedStats[0];
  }

  /**
   * Accessor method to retrieve the character's base value for mysticality.
   *
   * @return The character's base value for mysticality
   */
  public static final int getBaseMysticality() {
    return KoLCharacter.calculateBasePoints(KoLCharacter.totalSubpoints[1]);
  }

  /**
   * Accessor method to retrieve the total subpoints accumulated so far in mysticality.
   *
   * @return The total mysticality subpoints so far
   */
  public static final long getTotalMysticality() {
    return KoLCharacter.totalSubpoints[1];
  }

  public static final void incrementTotalMysticality(int increment) {
    KoLCharacter.totalSubpoints[1] += increment;
    if (KoLCharacter.totalSubpoints[1] >= KoLCharacter.triggerSubpoints[1]) {
      KoLCharacter.handleTrigger(KoLCharacter.triggerItem[1]);
    }
  }

  public static final boolean mysticalityTrigger(int basepoints, int itemId) {
    long points = calculatePointSubpoints(basepoints);
    if (points < KoLCharacter.triggerSubpoints[1]) {
      KoLCharacter.triggerSubpoints[1] = points;
      KoLCharacter.triggerItem[1] = itemId;
    }
    return false; // for the convenience of the caller
  }

  /**
   * Accessor method to retrieve the number of subpoints required before the character gains another
   * full point of mysticality.
   */
  public static final int getMysticalityTNP() {
    return KoLCharacter.calculateTillNextPoint(KoLCharacter.totalSubpoints[1]);
  }

  /**
   * Accessor method to retrieve the character's adjusted value for mysticality.
   *
   * @return The character's adjusted value for mysticality
   */
  public static final int getAdjustedMysticality() {
    return KoLCharacter.adjustedStats[1];
  }

  /**
   * Accessor method to retrieve the character's base value for moxie.
   *
   * @return The character's base value for moxie
   */
  public static final int getBaseMoxie() {
    return KoLCharacter.calculateBasePoints(KoLCharacter.totalSubpoints[2]);
  }

  /**
   * Accessor method to retrieve the total subpoints accumulated so far in moxie.
   *
   * @return The total moxie subpoints so far
   */
  public static final long getTotalMoxie() {
    return KoLCharacter.totalSubpoints[2];
  }

  public static final void incrementTotalMoxie(int increment) {
    KoLCharacter.totalSubpoints[2] += increment;
    if (KoLCharacter.totalSubpoints[2] >= KoLCharacter.triggerSubpoints[2]) {
      KoLCharacter.handleTrigger(KoLCharacter.triggerItem[2]);
    }
  }

  public static final boolean moxieTrigger(int basepoints, int itemId) {
    long points = calculatePointSubpoints(basepoints);
    if (points < KoLCharacter.triggerSubpoints[2]) {
      KoLCharacter.triggerSubpoints[2] = points;
      KoLCharacter.triggerItem[2] = itemId;
    }
    return false; // for the convenience of the caller
  }

  /**
   * Accessor method to retrieve the number of subpoints required before the character gains another
   * full point of moxie.
   */
  public static final int getMoxieTNP() {
    return KoLCharacter.calculateTillNextPoint(KoLCharacter.totalSubpoints[2]);
  }

  /**
   * Accessor method to retrieve the character's adjusted value for moxie.
   *
   * @return The character's adjusted value for moxie
   */
  public static final int getAdjustedMoxie() {
    return KoLCharacter.adjustedStats[2];
  }

  public static final int getAdjustedHighestStat() {
    return Math.max(
        Math.max(KoLCharacter.getAdjustedMuscle(), KoLCharacter.getAdjustedMysticality()),
        KoLCharacter.getAdjustedMoxie());
  }

  public static final int getBaseMainstat() {
    return switch (KoLCharacter.mainStat()) {
      case MUSCLE -> getBaseMuscle();
      case MYSTICALITY -> getBaseMysticality();
      default -> getBaseMoxie();
    };
  }

  public static final int getAdjustedMainstat() {
    return switch (KoLCharacter.mainStat()) {
      case MUSCLE -> getAdjustedMuscle();
      case MYSTICALITY -> getAdjustedMysticality();
      default -> getAdjustedMoxie();
    };
  }

  /**
   * Accessor method to set the number of adventures the character has left to spend in this
   * session.
   *
   * @param adventuresLeft The number of adventures the character has left
   */
  public static final void setAdventuresLeft(final int adventuresLeft) {
    if (adventuresLeft != KoLCharacter.adventuresLeft) {
      if (Taskbar.isTaskbarSupported()) {
        Taskbar taskbar = Taskbar.getTaskbar();
        if ((taskbar.isSupported(Taskbar.Feature.ICON_BADGE_TEXT)
                || taskbar.isSupported(Taskbar.Feature.ICON_BADGE_NUMBER))
            && Preferences.getBoolean("useDockIconBadge")) {
          taskbar.setIconBadge(String.valueOf(adventuresLeft));
        }
      }

      KoLCharacter.adventuresLeft = adventuresLeft;
      if (KoLCharacter.canEat() && !KoLCharacter.hasChef()
          || KoLCharacter.canDrink() && !KoLCharacter.hasBartender()) {
        ConcoctionDatabase.setRefreshNeeded(false);
      }
    }
  }

  /**
   * Accessor method to retrieve the number of adventures the character has left to spend in this
   * session.
   *
   * @return The number of adventures the character has left
   */
  public static final int getAdventuresLeft() {
    return KoLCharacter.adventuresLeft;
  }

  /** Accessor method to retrieve the total number of turns the character has used this run. */
  public static final int getCurrentRun() {
    return KoLCharacter.currentRun;
  }

  public static final void setCurrentRun(final int currentRun) {
    boolean changed =
        KoLCharacter.currentRun != currentRun && KoLCharacter.currentRun != 0 && currentRun != 0;
    KoLCharacter.currentRun = currentRun;
    if (changed) {
      BanishManager.recalculate();
      TrackManager.recalculate();
    }
  }

  /** Accessor method to retrieve the UNIX time of next rollover */
  public static final long getRollover() {
    return KoLCharacter.rollover;
  }

  public static final void setRollover(final long rollover) {
    KoLCharacter.rollover = rollover;
  }

  /**
   * Accessor method to retrieve the total number of turns the character has played across all
   * ascensions.
   */
  public static final int getTurnsPlayed() {
    return KoLCharacter.turnsPlayed;
  }

  public static final void setTurnsPlayed(final int turnsPlayed) {
    KoLCharacter.turnsPlayed = turnsPlayed;
  }

  /** Accessor method to retrieve the current daycount for this run */
  public static final int getCurrentDays() {
    return KoLCharacter.daycount;
  }

  public static final void setCurrentDays(final int daycount) {
    KoLCharacter.daycount = daycount;
  }

  /** Accessor method to retrieve the global daycount */
  public static final int getGlobalDays() {
    return KoLCharacter.globalDaycount;
  }

  public static final void setGlobalDays(final int daycount) {
    KoLCharacter.globalDaycount = daycount;
  }

  /** Accessor method to retrieve the current value of a named modifier */
  public static final Modifiers getCurrentModifiers() {
    return KoLCharacter.currentModifiers;
  }

  public static final double currentNumericModifier(final Modifier modifier) {
    return KoLCharacter.currentModifiers.getNumeric(modifier);
  }

  public static final double currentNumericModifier(final DoubleModifier modifier) {
    return KoLCharacter.currentModifiers.getDouble(modifier);
  }

  public static final double currentDerivedModifier(final DerivedModifier modifier) {
    return KoLCharacter.currentModifiers.getDerived(modifier);
  }

  public static final int currentRawBitmapModifier(final BitmapModifier modifier) {
    return KoLCharacter.currentModifiers.getRawBitmap(modifier);
  }

  public static final int currentBitmapModifier(final BitmapModifier modifier) {
    return KoLCharacter.currentModifiers.getBitmap(modifier);
  }

  public static final boolean currentBooleanModifier(final BooleanModifier mod) {
    return KoLCharacter.currentModifiers.getBoolean(mod);
  }

  public static final String currentStringModifier(final Modifier mod) {
    return KoLCharacter.currentModifiers.getString(mod);
  }

  public static List<String> currentMultiStringModifier(final MultiStringModifier mod) {
    return KoLCharacter.currentModifiers.getStrings(mod);
  }

  /** Accessor method to retrieve the total current monster level adjustment */
  public static final int getMonsterLevelAdjustment() {
    if (KoLCharacter.getLimitMode().limitMCD()) {
      return 0;
    }

    return (int) KoLCharacter.currentModifiers.getDouble(DoubleModifier.MONSTER_LEVEL)
        + KoLCharacter.getWaterLevel() * 10;
  }

  /** Accessor method to retrieve the total current count of random monster modifiers */
  public static final int getRandomMonsterModifiers() {
    return (int) KoLCharacter.currentModifiers.getDouble(DoubleModifier.RANDOM_MONSTER_MODIFIERS);
  }

  /** Accessor method to retrieve the total current familiar weight adjustment */
  public static final int getFamiliarWeightAdjustment() {
    return (int)
        (KoLCharacter.currentModifiers.getDouble(DoubleModifier.FAMILIAR_WEIGHT)
            + KoLCharacter.currentModifiers.getDouble(DoubleModifier.HIDDEN_FAMILIAR_WEIGHT));
  }

  public static final int getFamiliarWeightPercentAdjustment() {
    return (int) KoLCharacter.currentModifiers.getDouble(DoubleModifier.FAMILIAR_WEIGHT_PCT);
  }

  public static final int getManaCostAdjustment() {
    return KoLCharacter.getManaCostAdjustment(false);
  }

  public static final int getManaCostAdjustment(final boolean combat) {
    return (int) KoLCharacter.currentModifiers.getDouble(DoubleModifier.MANA_COST)
        + (int) KoLCharacter.currentModifiers.getDouble(DoubleModifier.STACKABLE_MANA_COST)
        + (combat
            ? (int) KoLCharacter.currentModifiers.getDouble(DoubleModifier.COMBAT_MANA_COST)
            : 0);
  }

  /** Accessor method to retrieve the total current combat percent adjustment */
  public static final double getCombatRateAdjustment() {
    double rate = KoLCharacter.currentModifiers.getDouble(DoubleModifier.COMBAT_RATE);
    if (AdventureDatabase.isUnderwater(Modifiers.currentLocation)) {
      rate += KoLCharacter.currentModifiers.getDouble(DoubleModifier.UNDERWATER_COMBAT_RATE);
    }
    return rate;
  }

  /** Accessor method to retrieve the total current initiative adjustment */
  public static final double getInitiativeAdjustment() {
    // Penalty is constrained to be non-positive
    return KoLCharacter.currentModifiers.getDouble(DoubleModifier.INITIATIVE)
        + Math.min(KoLCharacter.currentModifiers.getDouble(DoubleModifier.INITIATIVE_PENALTY), 0.0);
  }

  /** Accessor method to retrieve the total current fixed experience adjustment */
  public static final double getExperienceAdjustment() {
    var mod =
        switch (KoLCharacter.getPrimeIndex()) {
          case 0 -> DoubleModifier.MUS_EXPERIENCE;
          case 1 -> DoubleModifier.MYS_EXPERIENCE;
          case 2 -> DoubleModifier.MOX_EXPERIENCE;
          default -> null;
        };
    return mod == null ? 0.0 : KoLCharacter.currentModifiers.getDouble(mod);
  }

  /**
   * Accessor method to retrieve the total current meat drop percent adjustment
   *
   * @return Total Current Meat Drop Percent Adjustment
   */
  public static final double getMeatDropPercentAdjustment() {
    // Penalty is constrained to be non-positive
    return KoLCharacter.currentModifiers.getDouble(DoubleModifier.MEATDROP)
        + Math.min(KoLCharacter.currentModifiers.getDouble(DoubleModifier.MEATDROP_PENALTY), 0.0);
  }

  /**
   * Accessor method to retrieve the total current sprinkle drop percent adjustment
   *
   * @return Total Current Sprinkle Drop Percent Adjustment
   */
  public static final double getSprinkleDropPercentAdjustment() {
    return KoLCharacter.currentModifiers.getDouble(DoubleModifier.SPRINKLES);
  }

  /**
   * Accessor method to retrieve the total current item drop percent adjustment
   *
   * @return Total Current Item Drop Percent Adjustment
   */
  public static final double getItemDropPercentAdjustment() {
    return KoLCharacter.currentModifiers.getDouble(DoubleModifier.ITEMDROP)
        + Math.min(KoLCharacter.currentModifiers.getDouble(DoubleModifier.ITEMDROP_PENALTY), 0.0);
  }

  /**
   * Accessor method to retrieve the total current damage absorption
   *
   * @return Total Current Damage Absorption
   */
  public static final int getDamageAbsorption() {
    return (int) KoLCharacter.currentModifiers.getDouble(DoubleModifier.DAMAGE_ABSORPTION);
  }

  /**
   * Accessor method to retrieve the total current damage reduction
   *
   * @return Total Current Damage Reduction
   */
  public static final int getDamageReduction() {
    return (int) KoLCharacter.currentModifiers.getDouble(DoubleModifier.DAMAGE_REDUCTION);
  }

  /**
   * Accessor method to retrieve the player's Pool Skill from equipment/effects
   *
   * @return Pool Skill
   */
  public static final int getPoolSkill() {
    return (int) KoLCharacter.currentModifiers.getDouble(DoubleModifier.POOL_SKILL);
  }

  public static int estimatedPoolSkill(boolean verbose) {
    int drunk = KoLCharacter.getInebriety();
    int drunkBonus = drunk - (drunk > 10 ? (drunk - 10) * 3 : 0);
    int equip = KoLCharacter.getPoolSkill();
    int poolsSharked = Preferences.getInteger("poolSharkCount");
    int poolSharkBonus = 0;
    if (poolsSharked > 25) {
      poolSharkBonus = 10;
    } else if (poolsSharked > 0) {
      poolSharkBonus = (int) Math.floor(2 * Math.sqrt(poolsSharked));
    }
    int training = Preferences.getInteger("poolSkill");
    int poolSkill = equip + training + poolSharkBonus + drunkBonus;

    if (verbose) {
      RequestLogger.printLine("Pool Skill is estimated at : " + poolSkill + ".");
      RequestLogger.printLine(
          equip
              + " from equipment, "
              + drunkBonus
              + " from having "
              + drunk
              + " inebriety, "
              + training
              + " hustling training and "
              + poolSharkBonus
              + " learning from "
              + poolsSharked
              + " sharks.");
    }

    return poolSkill;
  }

  public static int estimatedPoolSkill() {
    return estimatedPoolSkill(false);
  }

  /**
   * Accessor method to retrieve the total Hobo Power
   *
   * @return Total Hobo Power
   */
  public static final int getHoboPower() {
    return (int) KoLCharacter.currentModifiers.getDouble(DoubleModifier.HOBO_POWER);
  }

  /**
   * Accessor method to retrieve the total Smithsness
   *
   * @return Total Smithsness
   */
  public static final int getSmithsness() {
    return (int) KoLCharacter.currentModifiers.getDouble(DoubleModifier.SMITHSNESS);
  }

  /**
   * Accessor method to retrieve the player's Clownosity
   *
   * @return Clownosity
   */
  public static final int getClownosity() {
    return KoLCharacter.currentModifiers.getBitmap(BitmapModifier.CLOWNINESS);
  }

  /**
   * Accessor method to retrieve the player's Bee-osity
   *
   * @return Bee-osity
   */
  public static final int getBeeosity() {
    return KoLCharacter.getBeeosity(EquipmentManager.currentEquipment());
  }

  public static final int getBeeosity(Map<Slot, AdventureResult> equipment) {
    int bees = 0;

    for (var slot : SlotSet.SLOTS) {
      var equip = equipment.get(slot);
      if (equip == null) continue;
      String name = equip.getName();
      bees += KoLCharacter.getBeeosity(name);
    }

    return bees;
  }

  private static final Pattern B_PATTERN = Pattern.compile("[Bb]");

  public static final int getBeeosity(String name) {
    return (int) KoLCharacter.B_PATTERN.matcher(name).results().count();
  }

  public static final boolean hasBeeosity(String name) {
    // Less resource intensive than a matcher for short-enough names
    if (name == null) {
      return false;
    }
    return name.contains("b") || name.contains("B");
  }

  public static final boolean hasGs(String name) {
    if (name == null) {
      return true;
    }
    return name.contains("g") || name.contains("G");
  }

  private static final Pattern I_PATTERN = Pattern.compile("[Ii]");

  public static final int getEyeosity(String name) {
    return (int) KoLCharacter.I_PATTERN.matcher(name).results().count();
  }

  public static final boolean hasEyeosity(String name) {
    if (name == null) {
      return false;
    }
    return name.contains("i") || name.contains("I");
  }

  private static final Pattern U_PATTERN = Pattern.compile("[Uu]");

  public static final int getEweosity(String name) {
    return (int) KoLCharacter.U_PATTERN.matcher(name).results().count();
  }

  public static final boolean hasEweosity(String name) {
    if (name == null) {
      return true;
    }
    return name.contains("u") || name.contains("U");
  }

  public static final int getRestingHP() {
    int rv = (int) KoLCharacter.currentModifiers.getDouble(DoubleModifier.BASE_RESTING_HP);
    double factor = KoLCharacter.currentModifiers.getDouble(DoubleModifier.RESTING_HP_PCT);
    if (factor != 0) {
      rv = (int) (rv * (factor + 100.0) / 100.0);
    }
    return rv + (int) KoLCharacter.currentModifiers.getDouble(DoubleModifier.BONUS_RESTING_HP);
  }

  public static final int getRestingMP() {
    int rv = (int) KoLCharacter.currentModifiers.getDouble(DoubleModifier.BASE_RESTING_MP);
    double factor = KoLCharacter.currentModifiers.getDouble(DoubleModifier.RESTING_MP_PCT);
    if (factor != 0) {
      rv = (int) (rv * (factor + 100.0) / 100.0);
    }
    return rv + (int) KoLCharacter.currentModifiers.getDouble(DoubleModifier.BONUS_RESTING_MP);
  }

  /**
   * Accessor method to retrieve the current elemental resistance levels
   *
   * @return Total Current Resistance to specified element
   */
  public static final int getElementalResistanceLevels(final Element element) {
    return switch (element) {
      case COLD -> (int) KoLCharacter.currentModifiers.getDouble(DoubleModifier.COLD_RESISTANCE);
      case HOT -> (int) KoLCharacter.currentModifiers.getDouble(DoubleModifier.HOT_RESISTANCE);
      case SLEAZE -> (int)
          KoLCharacter.currentModifiers.getDouble(DoubleModifier.SLEAZE_RESISTANCE);
      case SPOOKY -> (int)
          KoLCharacter.currentModifiers.getDouble(DoubleModifier.SPOOKY_RESISTANCE);
      case STENCH -> (int)
          KoLCharacter.currentModifiers.getDouble(DoubleModifier.STENCH_RESISTANCE);
      case SLIME -> (int) KoLCharacter.currentModifiers.getDouble(DoubleModifier.SLIME_RESISTANCE);
      case SUPERCOLD -> (int)
          KoLCharacter.currentModifiers.getDouble(DoubleModifier.SUPERCOLD_RESISTANCE);
      default -> 0;
    };
  }

  public static final double elementalResistanceByLevel(final int levels) {
    return KoLCharacter.elementalResistanceByLevel(levels, true);
  }

  public static final double elementalResistanceByLevel(final int levels, final boolean mystBonus) {
    // salien has a formula which matches my data very nicely:
    // http://jick-nerfed.us/forums/viewtopic.php?t=4526
    // For X > 4: 90 - 50 * (5/6)^(X-4)

    double value;

    if (levels > 4) {
      value = 90.0 - 50.0 * Math.pow(5.0 / 6.0, levels - 4);
    } else {
      value = levels * 10.0;
    }

    if (mystBonus && KoLCharacter.isMysticalityClass()) {
      value += 5.0;
    }

    return value;
  }

  /**
   * Accessor method to retrieve the current elemental resistance
   *
   * @return Total Current Resistance to specified element
   */
  public static final double getElementalResistance(final Element element) {
    if (element == Element.NONE) {
      return 0.0;
    }
    int levels = KoLCharacter.getElementalResistanceLevels(element);
    return KoLCharacter.elementalResistanceByLevel(levels, element != Element.SLIME);
  }

  /**
   * Accessor method to retrieve the current bonus damage
   *
   * @return Total Current Resistance to specified element
   */
  public static final int currentBonusDamage() {
    int weaponDamage = (int) KoLCharacter.currentModifiers.getDouble(DoubleModifier.WEAPON_DAMAGE);
    int rangedDamage = (int) KoLCharacter.currentModifiers.getDouble(DoubleModifier.RANGED_DAMAGE);
    return weaponDamage
        + (EquipmentManager.getWeaponType() == WeaponType.RANGED ? rangedDamage : 0);
  }

  /**
   * Accessor method to retrieve the current prismatic damage
   *
   * @return Total Current Resistance to specified element
   */
  public static final int currentPrismaticDamage() {
    return (int) KoLCharacter.currentModifiers.getDouble(DoubleModifier.PRISMATIC_DAMAGE);
  }

  public static final int getWaterLevel() {
    if (!KoLCharacter.inRaincore()) {
      return 0;
    }

    int WL = 1;
    if (KoLCharacter.selectedLocation != null) {
      WL = KoLCharacter.selectedLocation.getWaterLevel();
      // Return 0 if underwater
      if (WL == 0) {
        return 0;
      }
    }

    WL += (int) KoLCharacter.currentModifiers.getDouble(DoubleModifier.WATER_LEVEL);

    return WL < 1 ? 1 : Math.min(WL, 6);
  }

  /**
   * Accessor method which indicates whether or not the character has store in the mall
   *
   * @return <code>true</code> if the character has a store
   */
  public static final boolean hasStore() {
    return KoLCharacter.hasStore;
  }

  /**
   * Accessor method to indicate a change in state of the mall store.
   *
   * @param hasStore Whether or not the character currently has a store
   */
  public static final void setStore(final boolean hasStore) {
    KoLCharacter.hasStore = hasStore;
  }

  /**
   * Accessor method which indicates whether or not the character has display case
   *
   * @return <code>true</code> if the character has a display case
   */
  public static final boolean hasDisplayCase() {
    return KoLCharacter.hasDisplayCase;
  }

  /**
   * Accessor method to indicate a change in state of the museum display case
   *
   * @param hasDisplayCase Whether or not the character currently has display case
   */
  public static final void setDisplayCase(final boolean hasDisplayCase) {
    KoLCharacter.hasDisplayCase = hasDisplayCase;
  }

  /**
   * Accessor method which indicates whether or not the character is in a clan
   *
   * @return <code>true</code> if the character is in a clan
   */
  public static final boolean hasClan() {
    return KoLCharacter.hasClan;
  }

  /**
   * Accessor method to indicate a change in state of the character's clan membership
   *
   * @param hasClan Whether or not the character currently is in a clan
   */
  public static final void setClan(final boolean hasClan) {
    KoLCharacter.hasClan = hasClan;
  }

  /**
   * Accessor method which indicates whether or not the character has a shaker
   *
   * @return <code>true</code> if the character has a shaker
   */
  public static final boolean hasShaker() {
    return Preferences.getBoolean("hasShaker");
  }

  /**
   * Accessor method to indicate a change in state of the shaker
   *
   * @param hasShaker Whether or not the character currently has a shaker
   */
  public static final void setShaker(final boolean hasShaker) {
    if (Preferences.getBoolean("hasShaker") != hasShaker) {
      Preferences.setBoolean("hasShaker", hasShaker);
      ConcoctionDatabase.setRefreshNeeded(true);
    }
  }

  /**
   * Accessor method which indicates whether or not the character has a cocktail crafting kit
   *
   * @return <code>true</code> if the character has a cocktail crafting kit
   */
  public static final boolean hasCocktailKit() {
    return Preferences.getBoolean("hasCocktailKit");
  }

  /**
   * Accessor method to indicate a change in state of the cocktail crafting kit
   *
   * @param hasCocktailKit Whether or not the character currently has a cocktail crafting kit
   */
  public static final void setCocktailKit(final boolean hasCocktailKit) {
    if (Preferences.getBoolean("hasCocktailKit") != hasCocktailKit) {
      Preferences.setBoolean("hasCocktailKit", hasCocktailKit);
      ConcoctionDatabase.setRefreshNeeded(true);
    }
  }

  /**
   * Accessor method which indicates whether or not the character has a bartender-in-the-box.
   *
   * @return <code>true</code> if the character has a bartender-in-the-box
   */
  public static final boolean hasBartender() {
    return Preferences.getBoolean("hasBartender");
  }

  /**
   * Accessor method to indicate a change in state of the bartender-in-the-box.
   *
   * @param hasBartender Whether or not the character currently has a bartender
   */
  public static final void setBartender(final boolean hasBartender) {
    if (Preferences.getBoolean("hasBartender") != hasBartender) {
      Preferences.setBoolean("hasBartender", hasBartender);
      ConcoctionDatabase.setRefreshNeeded(true);
    }
  }

  /**
   * Accessor method which indicates whether or not the character has an oven
   *
   * @return <code>true</code> if the character has an oven
   */
  public static final boolean hasOven() {
    return Preferences.getBoolean("hasOven")
        || KoLCharacter.isEd()
        || KoLCharacter.inNuclearAutumn();
  }

  /**
   * Accessor method to indicate a change in state of the oven
   *
   * @param hasOven Whether or not the character currently has an oven
   */
  public static final void setOven(final boolean hasOven) {
    if (Preferences.getBoolean("hasOven") != hasOven) {
      Preferences.setBoolean("hasOven", hasOven);
      ConcoctionDatabase.setRefreshNeeded(true);
      ConsumablesDatabase.calculateAllAverageAdventures();
    }
  }

  /**
   * Accessor method which indicates whether or not the character has a range
   *
   * @return <code>true</code> if the character has a range
   */
  public static final boolean hasRange() {
    return Preferences.getBoolean("hasRange");
  }

  /**
   * Accessor method to indicate a change in state of the range
   *
   * @param hasRange Whether or not the character currently has a range
   */
  public static final void setRange(final boolean hasRange) {
    if (Preferences.getBoolean("hasRange") != hasRange) {
      Preferences.setBoolean("hasRange", hasRange);
      ConcoctionDatabase.setRefreshNeeded(true);
    }
  }

  /**
   * Accessor method which indicates whether or not the character has a chef-in-the-box.
   *
   * @return <code>true</code> if the character has a chef-in-the-box
   */
  public static final boolean hasChef() {
    return Preferences.getBoolean("hasChef");
  }

  /**
   * Accessor method to indicate a change in state of the chef-in-the-box.
   *
   * @param hasChef Whether or not the character currently has a chef
   */
  public static final void setChef(final boolean hasChef) {
    if (Preferences.getBoolean("hasChef") != hasChef) {
      Preferences.setBoolean("hasChef", hasChef);
      ConcoctionDatabase.setRefreshNeeded(true);
    }
  }

  /**
   * Accessor method which indicates whether or not the character has a sushi rolling mat
   *
   * @return <code>true</code> if the character has a sushi rolling mat
   */
  public static final boolean hasSushiMat() {
    return Preferences.getBoolean("hasSushiMat");
  }

  /**
   * Accessor method to indicate a change in state of the sushi rolling mat
   *
   * @param hasSushiMat Whether or not the character currently has a sushi rolling mat
   */
  public static final void setSushiMat(final boolean hasSushiMat) {
    if (Preferences.getBoolean("hasSushiMat") != hasSushiMat) {
      Preferences.setBoolean("hasSushiMat", hasSushiMat);
      ConcoctionDatabase.setRefreshNeeded(true);
    }
  }

  /**
   * Accessor method which indicates whether or not the character has a mystical bookshelf
   *
   * @return <code>true</code> if the character has a mystical bookshelf
   */
  public static final boolean hasBookshelf() {
    return KoLCharacter.hasBookshelf;
  }

  /**
   * Accessor method to indicate a change in state of the mystical bookshelf
   *
   * @param hasBookshelf Whether or not the character currently has a bookshelf
   */
  public static final void setBookshelf(final boolean hasBookshelf) {
    boolean refresh = hasBookshelf && KoLCharacter.hasBookshelf != hasBookshelf;
    KoLCharacter.hasBookshelf = hasBookshelf;
    if (refresh) {
      RequestThread.postRequest(new CampgroundRequest("bookshelf"));
    }
  }

  /**
   * Accessor method which indicates how many times the character has upgraded their telescope
   *
   * @return <code>int/code> power of telescope
   */
  public static final int getTelescopeUpgrades() {
    return KoLCharacter.telescopeUpgrades;
  }

  /** Accessor method to indicate a change in state of the telescope */
  public static final void setTelescopeUpgrades(final int upgrades) {
    KoLCharacter.telescopeUpgrades = upgrades;
  }

  /** Accessor method to indicate a change in state of the telescope */
  public static final void setTelescope(final boolean present) {
    KoLCharacter.telescopeUpgrades = Preferences.getInteger("telescopeUpgrades");
    // Assume newly detected telescope is basic. We'll look through
    // it when checkTelescope is called.
    if (present && KoLCharacter.telescopeUpgrades == 0) {
      KoLCharacter.telescopeUpgrades = 1;
    }
  }

  /** Method to look through the telescope if it hasn't been done yet */
  public static final void checkTelescope() {
    if (KoLCharacter.telescopeUpgrades == 0) {
      return;
    }

    if (KoLCharacter.inBadMoon() && !KoLCharacter.kingLiberated()) {
      return;
    }

    int lastAscension = Preferences.getInteger("lastTelescopeReset");
    if (lastAscension < KoLCharacter.ascensions) {
      RequestThread.postRequest(new TelescopeRequest(TelescopeRequest.LOW));
    }
  }

  public static final boolean getHippyStoneBroken() {
    return KoLCharacter.hippyStoneBroken;
  }

  public static final void setHippyStoneBroken(boolean broken) {
    KoLCharacter.hippyStoneBroken = broken;
  }

  /**
   * Accessor method which indicates whether or not the character has freed King Ralph
   *
   * @return <code>true</code> if the character has freed King Ralph
   */
  public static final boolean kingLiberated() {
    int lastAscension = Preferences.getInteger("lastKingLiberation");
    if (lastAscension < KoLCharacter.ascensions) {
      Preferences.setInteger("lastKingLiberation", KoLCharacter.getAscensions());
      Preferences.setBoolean("kingLiberated", false);
      return false;
    }
    return Preferences.getBoolean("kingLiberated");
  }

  // Mark whether api.php says we've liberated King Ralph. This is done
  // very early during character initialization, so simply set the
  // preference and let later processing use that.
  public static final void setKingLiberated(boolean liberated) {
    // Call kingLiberated to deal with lastKingLiberation
    if (KoLCharacter.kingLiberated() != liberated) {
      Preferences.setBoolean("kingLiberated", liberated);
    }
  }

  public static final void liberateKing() {
    if (KoLCharacter.kingLiberated()) {
      return;
    }

    Path oldPath = KoLCharacter.ascensionPath;
    boolean wasInHardcore = KoLCharacter.isHardcore;
    int points = wasInHardcore ? 2 : 1;
    boolean restricted = KoLCharacter.getRestricted();

    Preferences.setBoolean("kingLiberated", true);

    switch (oldPath) {
      case AVATAR_OF_WEST_OF_LOATHING -> {
        final String pref =
            switch (ascensionClass) {
              case BEANSLINGER -> "awolPointsBeanslinger";
              case COW_PUNCHER -> "awolPointsCowpuncher";
              case SNAKE_OILER -> "awolPointsSnakeoiler";
              default -> null;
            };
        if (pref != null) {
          Preferences.increment(pref, points, 10, false);
        }
      }
      case SHADOWS_OVER_LOATHING -> {
        final String pref =
            switch (ascensionClass) {
              case PIG_SKINNER -> "asolPointsPigSkinner";
              case CHEESE_WIZARD -> "asolPointsCheeseWizard";
              case JAZZ_AGENT -> "asolPointsJazzAgent";
              default -> null;
            };
        if (pref != null) {
          Preferences.increment(pref, points, 11, false);
        }
      }
      case GLOVER -> {
        Preferences.increment("garlandUpgrades", 1, 10, false);
        oldPath.incrementPoints(points);
      }
      case UNDER_THE_SEA -> {
        // Points incremented when you defeat the Nautical Seaceress,
        // not when you free the King
      }
      default -> {
        oldPath.incrementPoints(points);
      }
    }

    // We are no longer in Hardcore
    KoLCharacter.setHardcore(false);

    // Ronin is lifted and we can interact freely with the Kingdom
    KoLCharacter.setRonin(false);

    // Reset interaction and restriction
    CharPaneRequest.liberateKing();

    // We are no longer subject to consumption restrictions
    KoLCharacter.setPath(Path.NONE);

    // Storage is freely available
    KoLConstants.storage.addAll(KoLConstants.freepulls);
    KoLConstants.storage.addAll(KoLConstants.nopulls);
    KoLConstants.freepulls.clear();
    KoLConstants.nopulls.clear();
    ConcoctionDatabase.setPullsRemaining(-1);

    // The mall now uses Meat from inventory, not storage
    MallSearchFrame.updateMeat();

    // We may want to re-run breakfast, for various reasons
    Preferences.setBoolean("breakfastCompleted", false);

    // Reset modifiers in case we had modifiers that no longer apply
    // TODO: should actually be KoLCharacter.currentModifiers.reset(); + skill cache unless in TCRS?
    ModifierDatabase.resetModifiers();

    // If leaving a path with a unique class, finish when player picks a new class.
    // We can't interrupt choice.php with (most) requests.
    if (oldPath.isAvatar()) {
      return;
    }

    // If we are in Bad Moon, we can use the bookshelf and
    // telescope again.
    if (KoLCharacter.inBadMoon()) {
      RequestThread.postRequest(new CampgroundRequest("bookshelf"));
      KoLCharacter.checkTelescope();
    }

    // If we were in a path that grants skills only while on the path, reset them
    if (oldPath == Path.HEAVY_RAINS
        || oldPath == Path.NUCLEAR_AUTUMN
        || oldPath == Path.YOU_ROBOT) {
      KoLCharacter.resetSkills();
    }

    // Reset Legacy of Loathing stuff
    if (oldPath == Path.LEGACY_OF_LOATHING) {
      Preferences.resetToDefault("replicaChateauAvailable");
      Preferences.resetToDefault("replicaNeverendingPartyAlways");
      Preferences.resetToDefault("replicaWitchessSetAvailable");

      // we lose set enquiry, even if we have a terminal
      Preferences.resetToDefault("sourceTerminalEducate1");
      Preferences.resetToDefault("sourceTerminalEducate2");
      Preferences.resetToDefault("sourceTerminalEnquiry");

      // if replica emotion chipped
      KoLCharacter.resetSkills();

      // we lose replica familiars and items, but keep non-replica items equipped on those familiars
      // resetting everything is easier
      // we reset familiars a bit later
      InventoryManager.refresh();
      EquipmentManager.resetEquipment();
      RequestThread.postRequest(new EquipmentRequest(EquipmentRequestType.EQUIPMENT));
      KoLCharacter.currentModifiers.reset();

      // we lose DNA lab and maybe source terminal / witchess
      CampgroundRequest.reset();
      RequestThread.postRequest(new CampgroundRequest("workshed"));
    }

    // Scale up contents of our stomach and liver in Small
    if (oldPath == Path.SMALL) {
      KoLCharacter.setFullness(KoLCharacter.getFullness() * 10);
      KoLCharacter.setInebriety(KoLCharacter.getInebriety() * 10);
    }

    // If we were in Hardcore or a path that alters skills, automatically recall skills
    if (restricted
        || wasInHardcore
        || oldPath == Path.TRENDY
        || oldPath == Path.CLASS_ACT
        || oldPath == Path.SURPRISING_FIST
        || oldPath == Path.CLASS_ACT_II
        || oldPath == Path.HEAVY_RAINS
        || oldPath == Path.PICKY
        || oldPath == Path.NUCLEAR_AUTUMN
        || oldPath == Path.YOU_ROBOT
        || oldPath == Path.JOURNEYMAN) {
      RequestThread.postRequest(new CharSheetRequest());
      InventoryManager.checkSkillGrantingEquipment();
    }

    if (restricted
        || oldPath == Path.TRENDY
        || oldPath == Path.HEAVY_RAINS
        || oldPath == Path.NUCLEAR_AUTUMN
        || oldPath == Path.YOU_ROBOT) {
      // Retrieve the bookshelf
      RequestThread.postRequest(new CampgroundRequest("bookshelf"));
    }
    if (restricted
        || oldPath == Path.LICENSE_TO_ADVENTURE
        || oldPath == Path.YOU_ROBOT
        || oldPath == Path.QUANTUM
        || oldPath == Path.LEGACY_OF_LOATHING) {
      // Clear out any erroneous familiars (e.g. Quantum Terrarium adds any familiars you see)
      familiars.clear();

      // All familiars can now be used
      RequestThread.postRequest(new FamiliarRequest());
      GearChangePanel.updateFamiliars();
    }

    // If we haven't previously seen our campground, visit it.
    if (restricted
        || oldPath == Path.NUCLEAR_AUTUMN
        || oldPath == Path.YOU_ROBOT
        || oldPath == Path.SMALL) {
      CampgroundRequest.reset();
      RequestThread.postRequest(new CampgroundRequest("inspectdwelling"));
      RequestThread.postRequest(new CampgroundRequest("inspectkitchen"));
      RequestThread.postRequest(new CampgroundRequest("workshed"));
      KoLCharacter.checkTelescope();
    }

    if (restricted) {
      // Available stuff in Clan may have changed, so check clan
      ClanLoungeRequest.updateLounge();
    }

    // Stop expecting Path-related Wandering Monsters
    if (oldPath == Path.BEES_HATE_YOU) {
      TurnCounter.stopCounting("Bee window begin");
      TurnCounter.stopCounting("Bee window end");
    } else if (oldPath == Path.HEAVY_RAINS) {
      TurnCounter.stopCounting("Rain Monster window begin");
      TurnCounter.stopCounting("Rain Monster window end");
    } else if (oldPath == Path.AVATAR_OF_WEST_OF_LOATHING) {
      TurnCounter.stopCounting("WoL Monster window begin");
      TurnCounter.stopCounting("WoL Monster window end");
    } else if (oldPath == Path.QUANTUM) {
      TurnCounter.stopCounting(QuantumTerrariumRequest.FAMILIAR_COUNTER);
      TurnCounter.stopCounting(QuantumTerrariumRequest.COOLDOWN_COUNTER);
    }

    // Available hermit items and clover numbers may have changed
    // They depend on character class, so ex-avatars check after
    // they choose a new class.
    HermitRequest.initialize();

    // Check the Florist
    FloristRequest.reset();
    RequestThread.postRequest(new FloristRequest());

    KoLCharacter.recalculateAdjustments();

    // If you have the ItemManagerFrame open, there are four panels which
    // display food, booze, spleen items, and potions. These enable buttons and
    // filter what is shown based on character state: whether your character
    // can eat or drink, whether items are out of standard, and so on.
    //
    // All of that is moot, now. Signal the various GUI panels to update.
    NamedListenerRegistry.fireChange("(food)");
    NamedListenerRegistry.fireChange("(booze)");
    NamedListenerRegistry.fireChange("(spleen)");
    NamedListenerRegistry.fireChange("(potions)");

    // Run a user-supplied script
    KoLmafiaCLI.DEFAULT_SHELL.executeLine(Preferences.getString("kingLiberatedScript"));
  }

  private static void removePlumberQuestItems() {
    // When you free Princess Ralph, all special "quest" items from
    // this path vanish from inventory and your equipment.
    for (int itemId = ItemPool.FIRST_PLUMBER_QUEST_ITEM;
        itemId <= ItemPool.LAST_PLUMBER_QUEST_ITEM;
        ++itemId) {
      EquipmentManager.discardEquipment(itemId);
      int count = InventoryManager.getCount(itemId);
      if (count > 0) {
        AdventureResult item = ItemPool.get(itemId, -count);
        ResultProcessor.processResult(item);
      }
    }
  }

  /**
   * Accessor method which tells you if the character can interact with other players (Ronin or
   * Hardcore players cannot).
   */
  public static final boolean canInteract() {
    return CharPaneRequest.canInteract();
  }

  /** Returns whether or not the character is currently in hardcore. */
  public static final boolean isHardcore() {
    return KoLCharacter.isHardcore;
  }

  /** Accessor method which sets whether or not the player is currently in hardcore. */
  public static final void setHardcore(final boolean isHardcore) {
    if (KoLCharacter.isHardcore != isHardcore) {
      KoLCharacter.isHardcore = isHardcore;
      NamedListenerRegistry.fireChange("(hardcore)");
    }
  }

  /** Returns whether or not the character is currently in casual. */
  public static final boolean isCasual() {
    return KoLCharacter.isCasual;
  }

  /** Accessor method which sets whether or not the player is currently in casual. */
  public static final void setCasual(final boolean isCasual) {
    KoLCharacter.isCasual = isCasual;
  }

  /** Returns whether or not the character is currently in ronin. */
  public static final boolean inRonin() {
    return KoLCharacter.inRonin;
  }

  public static int initialRonin() {
    return KoLCharacter.inGoocore() ? 10000 : 1000;
  }

  public static int roninLeft() {
    return KoLCharacter.inRonin ? (KoLCharacter.initialRonin() - KoLCharacter.getCurrentRun()) : 0;
  }

  public static final void setSkillsRecalled(final boolean skillsRecalled) {
    if (KoLCharacter.skillsRecalled != skillsRecalled) {
      ConcoctionDatabase.setRefreshNeeded(true);
    }
    KoLCharacter.skillsRecalled = skillsRecalled;
  }

  public static final boolean skillsRecalled() {
    return KoLCharacter.skillsRecalled;
  }

  /** Accessor method which sets whether or not the player is currently in ronin. */
  public static final void setRonin(final boolean inRonin) {
    if (KoLCharacter.inRonin != inRonin) {
      KoLCharacter.inRonin = inRonin;
      NamedListenerRegistry.fireChange("(ronin)");
      KoLCharacter.recalculateAdjustments();
    }
  }

  /**
   * Accessor method for the character's ascension count
   *
   * @return String
   */
  public static final int getAscensions() {
    return KoLCharacter.ascensions;
  }

  /**
   * Accessor method for the character's zodiac sign
   *
   * @return String
   */
  public static final ZodiacSign getSign() {
    return KoLCharacter.ascensionSign;
  }

  /**
   * Accessor method for the character's zodiac sign index
   *
   * @return String
   */
  public static final int getSignIndex() {
    return KoLCharacter.ascensionSign.getId();
  }

  /**
   * Accessor method for the character's zodiac sign stat
   *
   * @return int
   */
  public static final ZodiacType getSignStat() {
    return KoLCharacter.ascensionSign.getType();
  }

  /**
   * Accessor method for the character's zodiac sign zone
   *
   * @return int
   */
  public static final ZodiacZone getSignZone() {
    return KoLCharacter.ascensionSign.getZone();
  }

  /**
   * Accessor method to set a character's ascension count
   *
   * @param ascensions the new ascension count
   */
  public static final void setAscensions(final int ascensions) {
    KoLCharacter.ascensions = ascensions;
  }

  public static final void setRestricted(final boolean restricted) {
    KoLCharacter.restricted = restricted;
  }

  public static final boolean getRestricted() {
    return KoLCharacter.restricted;
  }

  public static final boolean inFight() {
    return FightRequest.currentRound != 0 || FightRequest.inMultiFight;
  }

  public static final boolean inChoice() {
    return ChoiceManager.handlingChoice || FightRequest.choiceFollowsFight;
  }

  public static final boolean inFightOrChoice() {
    return inFight() || inChoice();
  }

  /**
   * Accessor method to set a character's zodiac sign
   *
   * @param ascensionSign the new sign
   */
  public static final void setSign(String ascensionSign) {
    if (ascensionSign.startsWith("The ")) {
      ascensionSign = ascensionSign.substring(4);
    }

    KoLCharacter.ascensionSign = ZodiacSign.find(ascensionSign);
  }

  /**
   * Accessor method to set a character's zodiac sign
   *
   * @param ascensionSign the new sign
   */
  public static final void setSign(ZodiacSign ascensionSign) {
    KoLCharacter.ascensionSign = ascensionSign;
  }

  /**
   * Accessor method for the character's path
   *
   * @return String
   */
  public static final Path getPath() {
    return KoLCharacter.ascensionPath;
  }

  public static final boolean inBeecore() {
    // All Beecore restrictions are lifted once you free the King
    return !KoLCharacter.kingLiberated() && KoLCharacter.ascensionPath == Path.BEES_HATE_YOU;
  }

  public static final boolean inFistcore() {
    // All Fistcore restrictions are lifted once you free the King
    return !KoLCharacter.kingLiberated() && KoLCharacter.ascensionPath == Path.SURPRISING_FIST;
  }

  public static final boolean isTrendy() {
    // All Trendy restrictions are lifted once you free the King
    return !KoLCharacter.kingLiberated() && KoLCharacter.ascensionPath == Path.TRENDY;
  }

  public static final boolean inAxecore() {
    // Which, if any, Axecore restrictions are lifted when you free the king?
    return KoLCharacter.ascensionPath == Path.AVATAR_OF_BORIS;
  }

  public static final boolean inBugcore() {
    // Which, if any, Bugbear Invasion restrictions are lifted when you free the king?
    return KoLCharacter.ascensionPath == Path.BUGBEAR_INVASION;
  }

  public static final boolean inZombiecore() {
    // Which, if any, Zombiecore restrictions are lifted when you free the king?
    return KoLCharacter.ascensionPath == Path.ZOMBIE_SLAYER;
  }

  public static final boolean inClasscore() {
    return KoLCharacter.ascensionPath == Path.CLASS_ACT;
  }

  public static final boolean isJarlsberg() {
    return KoLCharacter.ascensionPath == Path.AVATAR_OF_JARLSBERG;
  }

  public static final boolean inBigcore() {
    return KoLCharacter.ascensionPath == Path.BIG;
  }

  public static final boolean inHighschool() {
    return KoLCharacter.ascensionPath == Path.KOLHS;
  }

  public static final boolean inClasscore2() {
    return KoLCharacter.ascensionPath == Path.CLASS_ACT_II;
  }

  public static final boolean isSneakyPete() {
    return KoLCharacter.ascensionPath == Path.AVATAR_OF_SNEAKY_PETE;
  }

  public static final boolean inSlowcore() {
    return KoLCharacter.ascensionPath == Path.SLOW_AND_STEADY;
  }

  public static final boolean inRaincore() {
    return KoLCharacter.ascensionPath == Path.HEAVY_RAINS;
  }

  public static final boolean isPicky() {
    return KoLCharacter.ascensionPath == Path.PICKY;
  }

  public static final boolean isEd() {
    return KoLCharacter.ascensionPath == Path.ACTUALLY_ED_THE_UNDYING;
  }

  public static final boolean isCrazyRandom() {
    return KoLCharacter.ascensionPath == Path.CRAZY_RANDOM_SUMMER;
  }

  public static final boolean isCrazyRandomTwo() {
    return KoLCharacter.ascensionPath == Path.CRAZY_RANDOM_SUMMER_TWO;
  }

  public static final boolean isCommunityService() {
    return KoLCharacter.ascensionPath == Path.COMMUNITY_SERVICE;
  }

  public static final boolean isWestOfLoathing() {
    return KoLCharacter.ascensionPath == Path.AVATAR_OF_WEST_OF_LOATHING;
  }

  public static final boolean inTheSource() {
    return KoLCharacter.ascensionPath == Path.THE_SOURCE;
  }

  public static final boolean inNuclearAutumn() {
    return KoLCharacter.ascensionPath == Path.NUCLEAR_AUTUMN;
  }

  public static final boolean inNoobcore() {
    return KoLCharacter.ascensionPath == Path.GELATINOUS_NOOB;
  }

  public static final boolean inBondcore() {
    return KoLCharacter.ascensionPath == Path.LICENSE_TO_ADVENTURE;
  }

  public static final boolean inLAR() {
    return KoLCharacter.ascensionPath == Path.LIVE_ASCEND_REPEAT;
  }

  public static final boolean inPokefam() {
    return KoLCharacter.ascensionPath == Path.POKEFAM;
  }

  public static final boolean inGLover() {
    return KoLCharacter.ascensionPath == Path.GLOVER;
  }

  public static final boolean inDisguise() {
    return KoLCharacter.ascensionPath == Path.DISGUISES_DELIMIT;
  }

  public static final boolean inDarkGyffte() {
    return KoLCharacter.ascensionPath == Path.DARK_GYFFTE;
  }

  public static final boolean isKingdomOfExploathing() {
    return KoLCharacter.ascensionPath == Path.KINGDOM_OF_EXPLOATHING;
  }

  public static final boolean isPlumber() {
    return KoLCharacter.ascensionPath == Path.PATH_OF_THE_PLUMBER;
  }

  public static final boolean isLowkey() {
    return KoLCharacter.ascensionPath == Path.LOWKEY;
  }

  public static final boolean inGoocore() {
    return KoLCharacter.ascensionPath == Path.GREY_GOO;
  }

  public static final boolean inRobocore() {
    return KoLCharacter.ascensionPath == Path.YOU_ROBOT;
  }

  public static final boolean inQuantum() {
    return KoLCharacter.ascensionPath == Path.QUANTUM;
  }

  public static final boolean inFirecore() {
    return KoLCharacter.ascensionPath == Path.WILDFIRE;
  }

  public static final boolean inGreyYou() {
    return KoLCharacter.ascensionPath == Path.GREY_YOU;
  }

  public static final boolean inDinocore() {
    return KoLCharacter.ascensionPath == Path.DINOSAURS;
  }

  public static final boolean inShadowsOverLoathing() {
    return KoLCharacter.ascensionPath == Path.SHADOWS_OVER_LOATHING;
  }

  public static final boolean inLegacyOfLoathing() {
    return KoLCharacter.ascensionPath == Path.LEGACY_OF_LOATHING;
  }

  public static final boolean inSmallcore() {
    return KoLCharacter.ascensionPath == Path.SMALL;
  }

  public static final boolean inWereProfessor() {
    return KoLCharacter.ascensionPath == Path.WEREPROFESSOR;
  }

  public static final boolean inAvantGuard() {
    return KoLCharacter.ascensionPath == Path.AVANT_GUARD;
  }

  public static final boolean isMildManneredProfessor() {
    return KoLConstants.activeEffects.contains(EffectPool.get(EffectPool.MILD_MANNERED_PROFESSOR));
  }

  public static final boolean isSavageBeast() {
    return KoLConstants.activeEffects.contains(EffectPool.get(EffectPool.SAVAGE_BEAST));
  }

  public static final boolean inElevenThingIHateAboutU() {
    return KoLCharacter.ascensionPath == Path.ELEVEN_THINGS;
  }

  public static final boolean inZootomist() {
    return KoLCharacter.ascensionPath == Path.Z_IS_FOR_ZOOTOMIST;
  }

  public static final boolean inHatTrick() {
    return KoLCharacter.ascensionPath == Path.HAT_TRICK;
  }

  public static final boolean inSeaPath() {
    return KoLCharacter.ascensionPath == Path.UNDER_THE_SEA;
  }

  public static final boolean noExperience() {
    return inZootomist();
  }

  public static final boolean isUnarmed() {
    AdventureResult weapon = EquipmentManager.getEquipment(Slot.WEAPON);
    AdventureResult offhand = EquipmentManager.getEquipment(Slot.OFFHAND);
    return weapon == EquipmentRequest.UNEQUIP && offhand == EquipmentRequest.UNEQUIP;
  }

  public static final void makeCharitableDonation(final int amount) {
    if (amount > 0) {
      String message =
          "You donate " + KoLConstants.COMMA_FORMAT.format(amount) + " Meat to charity";
      RequestLogger.printLine(message);
      RequestLogger.updateSessionLog(message);
      Preferences.increment("charitableDonations", amount);
      Preferences.increment("totalCharitableDonations", amount);
    }
  }

  public static final void setPath(final Path path) {
    ascensionPath = path;
  }

  public static boolean canEat() {
    if (KoLCharacter.getLimitMode().limitEating()) {
      return false;
    }

    if (KoLCharacter.isEd() && !KoLCharacter.hasSkill(SkillPool.REPLACEMENT_STOMACH)) {
      return false;
    }

    if (KoLCharacter.inNoobcore()) {
      return false;
    }

    if (ascensionPath == Path.OXYGENARIAN || ascensionPath == Path.BOOZETAFARIAN) {
      return false;
    }

    return true;
  }

  public static boolean canDrink() {
    if (KoLCharacter.getLimitMode().limitDrinking()) {
      return false;
    }

    if (KoLCharacter.isEd() && !KoLCharacter.hasSkill(SkillPool.REPLACEMENT_LIVER)) {
      return false;
    }

    if (KoLCharacter.inNoobcore() || KoLCharacter.isPlumber() || KoLCharacter.inRobocore()) {
      return false;
    }

    if (ascensionPath == Path.OXYGENARIAN || ascensionPath == Path.TEETOTALER) {
      return false;
    }

    return true;
  }

  public static boolean canChew() {
    if (KoLCharacter.getLimitMode().limitSpleening()) {
      return false;
    }

    if (KoLCharacter.inNoobcore() || KoLCharacter.inRobocore()) {
      return false;
    }

    return true;
  }

  public static final boolean canUsePotions() {
    if (KoLCharacter.inRobocore()) {
      return YouRobotManager.canUsePotions();
    }

    return true;
  }

  /**
   * Accessor method for the current mind control setting
   *
   * @return int
   */
  public static final int getMindControlLevel() {
    return KoLCharacter.mindControlLevel;
  }

  /**
   * Accessor method to set the current mind control level
   *
   * @param level the new level
   */
  public static final void setMindControlLevel(final int level) {
    if (KoLCharacter.mindControlLevel != level) {
      KoLCharacter.mindControlLevel = level;
      KoLCharacter.recalculateAdjustments();
      KoLCharacter.updateStatus();
      AdventureFrame.updateSafetyDetails();
    }
  }

  public static final int getRadSickness() {
    return KoLCharacter.radSickness;
  }

  public static final void setRadSickness(final int rads) {
    if (KoLCharacter.radSickness != rads) {
      KoLCharacter.radSickness = rads;
      KoLCharacter.recalculateAdjustments();
      KoLCharacter.updateStatus();
      AdventureFrame.updateSafetyDetails();
    }
  }

  /**
   * Accessor method for the current auto attack action
   *
   * @return String
   */
  public static final int getAutoAttackAction() {
    return KoLCharacter.autoAttackAction;
  }

  /**
   * Accessor method to set the current auto attack action
   *
   * @param autoAttackAction the current auto attack action
   */
  public static final void setAutoAttackAction(final int autoAttackAction) {
    KoLCharacter.autoAttackAction = autoAttackAction;
  }

  public static final void setIgnoreZoneWarnings(boolean ignore) {
    KoLCharacter.ignoreZoneWarnings = ignore;
  }

  public static final boolean getIgnoreZoneWarnings() {
    return KoLCharacter.ignoreZoneWarnings;
  }

  /**
   * Accessor method for the current autosell mode
   *
   * @return String
   */
  public static final String getAutosellMode() {
    return KoLCharacter.autosellMode;
  }

  /**
   * Accessor method to set the autosell mode
   *
   * @param mode the new mode
   */
  public static final void setAutosellMode(final String mode) {
    KoLCharacter.autosellMode = mode;
  }

  /**
   * Accessor method for the current lazy inventory mode
   *
   * @return boolean
   */
  public static final boolean getLazyInventory() {
    return KoLCharacter.lazyInventory;
  }

  /**
   * Accessor method to set the lazy inventory mode
   *
   * @param mode the new mode
   */
  public static final void setLazyInventory(final boolean mode) {
    KoLCharacter.lazyInventory = mode;
  }

  /**
   * Accessor method for the current unequip familiar mode
   *
   * @return boolean
   */
  public static final boolean getUnequipFamiliar() {
    return KoLCharacter.unequipFamiliar;
  }

  /**
   * Accessor method to set the unequip familiar mode
   *
   * @param mode the new mode
   */
  public static final void setUnequipFamiliar(final boolean mode) {
    KoLCharacter.unequipFamiliar = mode;
  }

  /**
   * Accessor method which indicates whether the character is in a Muscle sign KoLmafia could/should
   * use this to: - Allow adventuring in The Bugbear Pens - Provide access to npcstore #4: The
   * Degrassi Knoll Bakery and Hardware Store - Train Muscle in The Gym - Smith non-advanced things
   * using Innabox (no hammer/adventure) - Combine anything using The Plunger (no meat paste)
   *
   * @return <code>true</code> if the character is in a Muscle sign
   */
  public static final boolean inMuscleSign() {
    return KoLCharacter.getSignStat() == ZodiacType.MUSCLE;
  }

  /**
   * Accessor method which indicates whether the character is in a Mysticality sign KoLmafia
   * could/should use this to: - Allow adventuring in Outskirts of Camp Logging Camp - Allow
   * adventuring in Camp Logging Camp - Provide access to npcstore #j: Little Canadia Jewelers -
   * Train Mysticality in The Institute for Canadian Studies
   *
   * @return <code>true</code> if the character is in a Mysticality sign
   */
  public static final boolean inMysticalitySign() {
    return KoLCharacter.getSignStat() == ZodiacType.MYSTICALITY;
  }

  /**
   * Accessor method which indicates whether the character is in a Moxie sign KoLmafia could/should
   * use this to: - Allow adventuring in Thugnderdome - Provide access to TINKER recipes - Train
   * Moxie with Gnirf
   *
   * @return <code>true</code> if the character is in a Moxie sign
   */
  public static final boolean inMoxieSign() {
    return KoLCharacter.getSignStat() == ZodiacType.MOXIE;
  }

  /**
   * Accessor method which indicates whether the character is in Bad Moon KoLmafia could/should use
   * this to: - Eliminate access to Hagnks - Provide access to Hell's Kitchen - Provide access to
   * Nervewrecker's Store
   *
   * @return <code>true</code> if the character is in Bad Moon
   */
  public static final boolean inBadMoon() {
    return KoLCharacter.getSignStat() == ZodiacType.BAD_MOON;
  }

  /**
   * Accessor method which indicates whether the character can go inside Degrassi Knoll.
   *
   * <p>KoLmafia could/should use this to: - Allow adventuring in The Bugbear Pens - Provide access
   * to npcstore #4: The Degrassi Knoll Bakery - Provide access to npcstore #5: The Degrassi Knoll
   * General Store - Train Muscle in The Gym - Smith non-advanced things using Innabox (no
   * hammer/adventure) - Combine anything using The Plunger (no meat paste)
   *
   * @return <code>true</code> if the character Can go inside Degrassi Knoll
   */
  public static final boolean knollAvailable() {
    return KoLCharacter.getSignZone() == ZodiacZone.KNOLL
        && !KoLCharacter.getLimitMode().limitZone("MusSign")
        && !KoLCharacter.isKingdomOfExploathing()
        && !KoLCharacter.inGoocore();
  }

  /**
   * Accessor method which indicates whether the character can go to Little Canadia
   *
   * <p>KoLmafia could/should use this to: - Allow adventuring in Outskirts of Camp Logging Camp -
   * Allow adventuring in Camp Logging Camp - Provide access to npcstore #j: Little Canadia Jewelers
   * - Train Mysticality in The Institute for Canadian Studies
   *
   * @return <code>true</code> if the character can go to Little Canadia
   */
  public static final boolean canadiaAvailable() {
    return KoLCharacter.getSignZone() == ZodiacZone.CANADIA
        && !KoLCharacter.getLimitMode().limitZone("Little Canadia")
        && !KoLCharacter.isKingdomOfExploathing();
  }

  /**
   * Accessor method which indicates whether the character can go to the Gnomish Gnomads Camp
   *
   * <p>KoLmafia could/should use this to: - Allow adventuring in Thugnderdome - Provide access to
   * TINKER recipes - Train Moxie with Gnirf
   *
   * @return <code>true</code> if the character can go to the Gnomish Gnomads Camp
   */
  public static final boolean gnomadsAvailable() {
    return (KoLCharacter.getSignZone() == ZodiacZone.GNOMADS)
        && KoLCharacter.desertBeachAccessible()
        && !KoLCharacter.isKingdomOfExploathing();
  }

  /**
   * Accessor method which indicates whether the MCD is potentially available
   *
   * @return <code>true</code> if the character can potentially change monster level
   */
  public static final boolean mcdAvailable() {
    switch (KoLCharacter.getSignZone()) {
      case CANADIA:
        // Direct access to the Mind Control Device
        return KoLCharacter.canadiaAvailable();
      case KNOLL:
        // detuned radio from Degrassi Knoll General Store
        // Unusable in G-Lover
        return KoLCharacter.knollAvailable() && !KoLCharacter.inGLover();
      case GNOMADS:
        // Annoyotron available on beach
        return KoLCharacter.desertBeachAccessible();
      default:
        break;
    }
    return false;
  }

  public static final boolean desertBeachAccessible() {
    // Temporary code to allow Mafia to catch up with the fact that unlock is a flag
    if (Preferences.getInteger("lastDesertUnlock") != KoLCharacter.getAscensions()) {
      if (InventoryManager.getCount(ItemPool.BITCHIN_MEATCAR) > 0
          || InventoryManager.getCount(ItemPool.DESERT_BUS_PASS) > 0
          || InventoryManager.getCount(ItemPool.PUMPKIN_CARRIAGE) > 0
          || InventoryManager.getCount(ItemPool.TIN_LIZZIE) > 0
          || Preferences.getString("peteMotorbikeGasTank").equals("Large Capacity Tank")
          || QuestDatabase.isQuestFinished(Quest.MEATCAR)
          || KoLCharacter.kingLiberated()
          || KoLCharacter.isEd()
          || KoLCharacter.isKingdomOfExploathing()) {
        Preferences.setInteger("lastDesertUnlock", KoLCharacter.getAscensions());
      }
    }
    return Preferences.getInteger("lastDesertUnlock") == KoLCharacter.getAscensions()
        && !KoLCharacter.getLimitMode().limitZone("Beach");
  }

  public static final void setDesertBeachAvailable() {
    if (Preferences.getInteger("lastDesertUnlock") != KoLCharacter.getAscensions()) {
      Preferences.setInteger("lastDesertUnlock", KoLCharacter.getAscensions());
      if (KoLCharacter.gnomadsAvailable()) {
        MicroBreweryRequest.getMenu();
      }
    }
  }

  public static final boolean mysteriousIslandAccessible() {
    if (KoLCharacter.isKingdomOfExploathing()) {
      return false;
    }

    // Temporary code to allow Mafia to catch up with the fact that unlock is a flag
    if (Preferences.getInteger("lastIslandUnlock") != KoLCharacter.getAscensions()) {
      if (InventoryManager.hasItem(ItemPool.DINGY_DINGHY)
          || InventoryManager.hasItem(ItemPool.SKIFF)
          || QuestDatabase.isQuestFinished(Quest.HIPPY)
          || Preferences.getString("peteMotorbikeGasTank").equals("Extra-Buoyant Tank")
          || InventoryManager.hasItem(ItemPool.YELLOW_SUBMARINE)) {
        Preferences.setInteger("lastIslandUnlock", KoLCharacter.getAscensions());
      }
    }
    return Preferences.getInteger("lastIslandUnlock") == KoLCharacter.getAscensions()
        && !KoLCharacter.getLimitMode().limitZone("Island");
  }

  /**
   * Accessor method to set the list of available skills.
   *
   * @param newSkillSet The list of the names of available skills
   */
  public static final void setAvailableSkills(final List<UseSkillRequest> newSkillSet) {
    for (UseSkillRequest skill : newSkillSet) {
      KoLCharacter.addAvailableSkill(skill);
    }

    int battleIndex = KoLCharacter.battleSkillNames.indexOf(Preferences.getString("battleAction"));
    KoLCharacter.battleSkillNames.setSelectedIndex(battleIndex == -1 ? 0 : battleIndex);

    DiscoCombatHelper.initialize();

    SkillBuffFrame.update();
  }

  public static final void setPermedSkills(final List<UseSkillRequest> newSkillSet) {
    KoLConstants.permedSkills.clear();

    KoLConstants.permedSkills.addAll(newSkillSet);
  }

  public static final void setHardcorePermedSkills(final Set<Integer> newSkillSet) {
    KoLConstants.hardcorePermedSkills.clear();

    KoLConstants.hardcorePermedSkills.addAll(newSkillSet);
  }

  /** Adds a single skill to the list of known skills possessed by this character. */
  public static final void addAvailableSkill(final int skillId) {
    KoLCharacter.addAvailableSkill(skillId, false);
  }

  public static final void addAvailableSkill(final int skillId, final boolean checkTrendy) {
    KoLCharacter.addAvailableSkill(UseSkillRequest.getUnmodifiedInstance(skillId), checkTrendy);
  }

  public static final void addAvailableSkill(final String skillName) {
    // *** Skills can have ambiguous names. Best to use the methods that deal with skill id
    KoLCharacter.addAvailableSkill(skillName, false);
  }

  public static final void addAvailableSkill(final String skillName, final boolean checkTrendy) {
    // *** Skills can have ambiguous names. Best to use the methods that deal with skill id
    KoLCharacter.addAvailableSkill(SkillDatabase.getSkillId(skillName), checkTrendy);
  }

  public static final void addAvailableSkill(final UseSkillRequest skill) {
    KoLCharacter.addAvailableSkill(skill, false);
  }

  private static void addAvailableSkill(final UseSkillRequest skill, final boolean checkAllowed) {
    if (skill == null) {
      return;
    }

    int skillId = skill.getSkillId();
    if (KoLConstants.availableSkillsSet.contains(skillId)) {
      return;
    }

    if (KoLCharacter.getLimitMode().limitSkill(skill)) {
      return;
    }

    if (checkAllowed && (KoLCharacter.isTrendy() || KoLCharacter.getRestricted())) {
      boolean isAllowed;
      String skillName = skill.getSkillName();
      if (SkillDatabase.isBookshelfSkill(skillName)) {
        int itemId = SkillDatabase.skillToBook(skillName);
        skillName = ItemDatabase.getItemName(itemId);
        isAllowed = StandardRequest.isAllowed(RestrictedItemType.BOOKSHELF_BOOKS, skillName);
      } else {
        isAllowed = StandardRequest.isAllowed(RestrictedItemType.SKILLS, skillName);
      }

      if (!isAllowed) {
        return;
      }
    }

    KoLConstants.availableSkills.add(skill);
    KoLConstants.availableSkillsSet.add(skillId);
    PreferenceListenerRegistry.firePreferenceChanged("(skill)");
    Modifiers.availableSkillsChanged();

    // passives that grant other skills

    switch (skillId) {
      case SkillPool.FLAVOUR_OF_MAGIC:
        // Flavour of Magic gives you access to five other
        // castable skills
        KoLCharacter.addAvailableSkill(SkillPool.SPIRIT_CAYENNE);
        KoLCharacter.addAvailableSkill(SkillPool.SPIRIT_PEPPERMINT);
        KoLCharacter.addAvailableSkill(SkillPool.SPIRIT_GARLIC);
        KoLCharacter.addAvailableSkill(SkillPool.SPIRIT_WORMWOOD);
        KoLCharacter.addAvailableSkill(SkillPool.SPIRIT_BACON);
        KoLCharacter.addAvailableSkill(SkillPool.SPIRIT_NOTHING);
        break;

      case SkillPool.SOUL_SAUCERY:
        // Soul Saucery gives you access to six other skills if a Sauceror
        if (isSauceror()) {
          KoLCharacter.addAvailableSkill(SkillPool.SOUL_BUBBLE);
          KoLCharacter.addAvailableSkill(SkillPool.SOUL_FINGER);
          KoLCharacter.addAvailableSkill(SkillPool.SOUL_BLAZE);
          KoLCharacter.addAvailableSkill(SkillPool.SOUL_FOOD);
          KoLCharacter.addAvailableSkill(SkillPool.SOUL_ROTATION);
          KoLCharacter.addAvailableSkill(SkillPool.SOUL_FUNK);
        }
        break;

        // Plumber passive skills that grant Plumber
        // combat skills with the same name
      case SkillPool.HAMMER_THROW:
        KoLCharacter.addAvailableCombatSkill(SkillPool.HAMMER_THROW_COMBAT);
        KoLCharacter.addCombatSkill(skill.getSkillName());
        break;
      case SkillPool.ULTRA_SMASH:
        KoLCharacter.addAvailableCombatSkill(SkillPool.ULTRA_SMASH_COMBAT);
        KoLCharacter.addCombatSkill(skill.getSkillName());
        break;
      case SkillPool.JUGGLE_FIREBALLS:
        KoLCharacter.addAvailableCombatSkill(SkillPool.JUGGLE_FIREBALLS_COMBAT);
        KoLCharacter.addCombatSkill(skill.getSkillName());
        break;
      case SkillPool.FIREBALL_BARRAGE:
        KoLCharacter.addAvailableCombatSkill(SkillPool.FIREBALL_BARRAGE_COMBAT);
        KoLCharacter.addCombatSkill(skill.getSkillName());
        break;
      case SkillPool.SPIN_JUMP:
        KoLCharacter.addAvailableCombatSkill(SkillPool.SPIN_JUMP_COMBAT);
        KoLCharacter.addCombatSkill(skill.getSkillName());
        break;
      case SkillPool.MULTI_BOUNCE:
        KoLCharacter.addAvailableCombatSkill(SkillPool.MULTI_BOUNCE_COMBAT);
        KoLCharacter.addCombatSkill(skill.getSkillName());
        break;

        // Comprehensive Cartography grants Map the Monsters
      case SkillPool.COMPREHENSIVE_CARTOGRAPHY:
        KoLCharacter.addAvailableSkill(SkillPool.MAP_THE_MONSTERS);
        break;

      case SkillPool.EMOTIONALLY_CHIPPED:
      case SkillPool.REPLICA_EMOTIONALLY_CHIPPED:
        KoLCharacter.addAvailableSkill(SkillPool.FEEL_DISAPPOINTED);
        KoLCharacter.addAvailableSkill(SkillPool.FEEL_ENVY);
        KoLCharacter.addAvailableSkill(SkillPool.FEEL_EXCITEMENT);
        KoLCharacter.addAvailableSkill(SkillPool.FEEL_HATRED);
        KoLCharacter.addAvailableSkill(SkillPool.FEEL_LONELY);
        KoLCharacter.addAvailableSkill(SkillPool.FEEL_LOST);
        KoLCharacter.addAvailableSkill(SkillPool.FEEL_NERVOUS);
        KoLCharacter.addAvailableSkill(SkillPool.FEEL_NOSTALGIC);
        KoLCharacter.addAvailableSkill(SkillPool.FEEL_PEACEFUL);
        KoLCharacter.addAvailableSkill(SkillPool.FEEL_PRIDE);
        KoLCharacter.addAvailableSkill(SkillPool.FEEL_SUPERIOR);
        break;
    }

    if (SkillDatabase.isCombat(skillId)) {
      KoLCharacter.addCombatSkill(skill.getSkillName());
    }

    if (SkillDatabase.isNonCombat(skillId)) {
      KoLConstants.usableSkills.add(skill);
      LockableListFactory.sort(KoLConstants.usableSkills);
    }

    if (SkillDatabase.isSummon(skillId)) {
      KoLConstants.summoningSkills.add(skill);
      LockableListFactory.sort(KoLConstants.summoningSkills);
    }

    if (SkillDatabase.isRemedy(skillId)) {
      KoLConstants.remedySkills.add(skill);
      LockableListFactory.sort(KoLConstants.remedySkills);
    }

    // effects: not shared
    if (SkillDatabase.isSong(skillId)) {
      KoLConstants.songSkills.add(skill);
      LockableListFactory.sort(KoLConstants.songSkills);
    } else if (SkillDatabase.isExpression(skillId)) {
      KoLConstants.expressionSkills.add(skill);
      LockableListFactory.sort(KoLConstants.expressionSkills);
    } else if (SkillDatabase.isWalk(skillId)) {
      KoLConstants.walkSkills.add(skill);
      LockableListFactory.sort(KoLConstants.walkSkills);
    } else if (SkillDatabase.isSelfOnly(skillId)) {
      KoLConstants.selfOnlySkills.add(skill);
      LockableListFactory.sort(KoLConstants.selfOnlySkills);
    } else if (SkillDatabase.isBuff(skillId)) {
      KoLConstants.buffSkills.add(skill);
      LockableListFactory.sort(KoLConstants.buffSkills);
    }
  }

  /** Adds a single skill to the list of skills temporarily possessed by this character. */
  public static final void addAvailableCombatSkill(final int skillId) {
    KoLConstants.availableCombatSkillsSet.add(skillId);
  }

  public static final void addAvailableCombatSkill(final String skillName) {
    // *** Skills can have ambiguous names. Best to use the methods that deal with skill id
    KoLCharacter.addAvailableCombatSkill(SkillDatabase.getSkillId(skillName));
  }

  public static final void removeAvailableCombatSkill(final int skillId) {
    KoLConstants.availableCombatSkillsSet.remove(skillId);
  }

  public static final void removeAvailableCombatSkill(final String skillName) {
    // *** Skills can have ambiguous names. Best to use the methods that deal with skill id
    KoLCharacter.removeAvailableCombatSkill(SkillDatabase.getSkillId(skillName));
  }

  private static void addCombatSkill(final String name) {
    String skillname = "skill " + name.toLowerCase();
    if (!KoLCharacter.battleSkillNames.contains(skillname)) {
      KoLCharacter.battleSkillNames.add(skillname);
    }
  }

  public static final void removeAvailableSkill(final String skillName) {
    // *** Skills can have ambiguous names. Best to use the methods that deal with skill id
    KoLCharacter.removeAvailableSkill(SkillDatabase.getSkillId(skillName));
  }

  public static final void removeAvailableSkill(final int skillId) {
    if (skillId == -1) {
      return;
    }

    UseSkillRequest skill = UseSkillRequest.getUnmodifiedInstance(skillId);
    KoLConstants.availableSkills.remove(skill);
    KoLConstants.availableSkillsSet.remove(skillId);
    KoLConstants.usableSkills.remove(skill);
    KoLConstants.summoningSkills.remove(skill);
    KoLConstants.remedySkills.remove(skill);
    KoLConstants.selfOnlySkills.remove(skill);
    KoLConstants.buffSkills.remove(skill);
    KoLConstants.songSkills.remove(skill);
    KoLConstants.expressionSkills.remove(skill);
    KoLConstants.walkSkills.remove(skill);
    KoLCharacter.battleSkillNames.remove("skill " + skill.getSkillName().toLowerCase());
    KoLCharacter.updateStatus();
    ConcoctionDatabase.setRefreshNeeded(true);
    PreferenceListenerRegistry.firePreferenceChanged("(skill)");
  }

  /**
   * Returns a list of the names of all available combat skills. The selected index in this list
   * should match the selected index in the battle skills list.
   */
  public static final LockableListModel<String> getBattleSkillNames() {
    return KoLCharacter.battleSkillNames;
  }

  /**
   * Accessor method to look up whether or not the character can summon noodles.
   *
   * @return <code>true</code> if noodles can be summoned by this character
   */
  public static final boolean canSummonNoodles() {
    return KoLCharacter.hasSkill(SkillPool.PASTAMASTERY);
  }

  /**
   * Accessor method to look up whether or not the character can summon reagent.
   *
   * @return <code>true</code> if reagent can be summoned by this character
   */
  public static final boolean canSummonReagent() {
    return KoLCharacter.hasSkill(SkillPool.ADVANCED_SAUCECRAFTING);
  }

  /**
   * Accessor method to look up whether or not the character can summon shore-based items.
   *
   * @return <code>true</code> if shore-based items can be summoned by this character
   */
  public static final boolean canSummonShore() {
    return KoLCharacter.hasSkill(SkillPool.ADVANCED_COCKTAIL);
  }

  /**
   * Accessor method to look up whether or not the character can summon snowcones
   *
   * @return <code>true</code> if snowcones can be summoned by this character
   */
  public static final boolean canSummonSnowcones() {
    return KoLCharacter.hasSkill(SkillPool.SNOWCONE);
  }

  /**
   * Accessor method to look up whether or not the character can summon stickers
   *
   * @return <code>true</code> if stickers can be summoned by this character
   */
  public static final boolean canSummonStickers() {
    return KoLCharacter.hasSkill(SkillPool.STICKER);
  }

  /**
   * Accessor method to look up whether or not the character can summon clip art
   *
   * @return <code>true</code> if clip art can be summoned by this character
   */
  public static final boolean canSummonClipArt() {
    return KoLCharacter.hasSkill(SkillPool.CLIP_ART);
  }

  /**
   * Accessor method to look up whether or not the character can summon rad libs
   *
   * @return <code>true</code> if clip art can be summoned by this character
   */
  public static final boolean canSummonRadLibs() {
    return KoLCharacter.hasSkill(SkillPool.RAD_LIB);
  }

  /**
   * Accessor method to look up whether or not the character can smith weapons.
   *
   * @return <code>true</code> if this character can smith advanced weapons
   */
  public static final boolean canSmithWeapons() {
    return KoLCharacter.hasSkill(SkillPool.SUPER_ADVANCED_MEATSMITHING);
  }

  /**
   * Accessor method to look up whether or not the character can smith armor.
   *
   * @return <code>true</code> if this character can smith advanced armor
   */
  public static final boolean canSmithArmor() {
    return KoLCharacter.hasSkill(SkillPool.ARMORCRAFTINESS);
  }

  /**
   * Accessor method to look up whether or not the character can craft expensive jewelry
   *
   * @return <code>true</code> if this character can smith advanced weapons
   */
  public static final boolean canCraftExpensiveJewelry() {
    return KoLCharacter.hasSkill(SkillPool.REALLY_EXPENSIVE_JEWELRYCRAFTING);
  }

  /**
   * Accessor method to look up whether or not the character has Amphibian Sympathy
   *
   * @return <code>true</code> if this character has Amphibian Sympathy
   */
  public static final boolean hasAmphibianSympathy() {
    return KoLCharacter.hasSkill(SkillPool.AMPHIBIAN_SYMPATHY);
  }

  /** Utility methods which looks up whether or not the character has a particular skill. */
  public static boolean hasSkill(final int skillId) {
    return KoLConstants.availableSkillsSet.contains(skillId);
  }

  public static boolean hasSkill(final UseSkillRequest skill) {
    return KoLConstants.availableSkillsSet.contains(skill.getSkillId());
  }

  public static boolean hasSkill(final String skillName) {
    // *** Skills can have ambiguous names. Best to use the methods that deal with skill id
    int skillId = SkillDatabase.getSkillId(skillName);
    return KoLCharacter.hasSkill(skillId);
  }

  public static final boolean hasCombatSkill(final int skillId) {
    return KoLConstants.availableCombatSkillsSet.contains(skillId);
  }

  public static final Set<Integer> getAvailableSkillIds() {
    return Collections.unmodifiableSet(KoLConstants.availableSkillsSet);
  }

  /**
   * Accessor method to get the current familiar.
   *
   * @return familiar The current familiar
   */
  public static final FamiliarData getFamiliar() {
    return KoLCharacter.currentFamiliar == null
        ? FamiliarData.NO_FAMILIAR
        : KoLCharacter.currentFamiliar;
  }

  public static final FamiliarData getEffectiveFamiliar() {
    return KoLCharacter.effectiveFamiliar == null
        ? FamiliarData.NO_FAMILIAR
        : KoLCharacter.effectiveFamiliar;
  }

  public static final String getFamiliarImage() {
    return KoLCharacter.currentFamiliarImage == null
        ? "debug.gif"
        : KoLCharacter.currentFamiliarImage;
  }

  public static final void setFamiliarImage() {
    KoLCharacter.setFamiliarImage(
        FamiliarDatabase.getFamiliarImageLocation(KoLCharacter.currentFamiliar.getId()));
  }

  public static final void setFamiliarImage(final String path) {
    String directory = "itemimages";
    String image = path;
    if (path != null) {
      int slash = path.indexOf("/");
      if (slash != -1) {
        directory = path.substring(0, slash);
        image = path.substring(slash + 1);
      }
    }
    KoLCharacter.setFamiliarImage(directory, image);
  }

  public static final void setFamiliarImage(final String directory, final String image) {
    String path =
        (directory == null || directory.equals("itemimages")) ? image : (directory + "/" + image);
    KoLCharacter.currentFamiliarImage = path;
    FamiliarDatabase.setFamiliarImageLocation(KoLCharacter.getFamiliar().getId(), path);
    NamedListenerRegistry.fireChange("(familiar image)");
  }

  public static final FamiliarData getEnthroned() {
    return KoLCharacter.currentEnthroned == null
        ? FamiliarData.NO_FAMILIAR
        : KoLCharacter.currentEnthroned;
  }

  public static final FamiliarData getBjorned() {
    return KoLCharacter.currentBjorned == null
        ? FamiliarData.NO_FAMILIAR
        : KoLCharacter.currentBjorned;
  }

  public static final boolean isUsingStabBat() {
    return KoLCharacter.isUsingStabBat;
  }

  public static final FamiliarData getPokeFam(final int slot) {
    if (slot < 0 || slot > 2) {
      return FamiliarData.NO_FAMILIAR;
    }
    return KoLCharacter.currentPokeFam[slot] == null
        ? FamiliarData.NO_FAMILIAR
        : KoLCharacter.currentPokeFam[slot];
  }

  public static final FamiliarData[] getPokeTeam() {
    return KoLCharacter.currentPokeFam;
  }

  public static final void setPokeFam(final int slot, final FamiliarData familiar) {
    if (slot < 0 || slot > 2) {
      return;
    }
    KoLCharacter.currentPokeFam[slot] = familiar;
  }

  /**
   * Accessor method to get Clancy's current instrument
   *
   * @return AdventureResult The current instrument
   */
  public static final AdventureResult getCurrentInstrument() {
    return KoLCharacter.currentInstrument;
  }

  public static final void setCurrentInstrument(AdventureResult instrument) {
    KoLCharacter.currentInstrument = instrument;
    KoLCharacter.recalculateAdjustments();
    KoLCharacter.updateStatus();
  }

  public static final int getMinstrelLevel() {
    return KoLCharacter.minstrelLevel;
  }

  public static final void setMinstrelLevel(int minstrelLevel) {
    KoLCharacter.minstrelLevel = minstrelLevel;
    KoLCharacter.recalculateAdjustments();
    KoLCharacter.updateStatus();
  }

  public static final int getMinstrelLevelAdjustment() {
    return (int) KoLCharacter.currentModifiers.getDouble(DoubleModifier.MINSTREL_LEVEL);
  }

  public static final void setClancy(
      final int level, final AdventureResult instrument, final boolean attention) {
    KoLCharacter.minstrelLevel = level;
    KoLCharacter.currentInstrument = instrument;
    KoLCharacter.minstrelAttention = attention;
    KoLCharacter.recalculateAdjustments();
    KoLCharacter.updateStatus();
  }

  public static final Companion getCompanion() {
    return KoLCharacter.companion;
  }

  public static final void setCompanion(Companion companion) {
    KoLCharacter.companion = companion;
    KoLCharacter.recalculateAdjustments();
    KoLCharacter.updateStatus();
  }

  /**
   * Accessor method to get arena wins
   *
   * @return The number of arena wins
   */
  public static final int getArenaWins() {
    // Ensure that the arena opponent list is
    // initialized.

    CakeArenaManager.getOpponentList();
    return KoLCharacter.arenaWins;
  }

  public static final int getStillsAvailable() {
    if ((!KoLCharacter.hasSkill(SkillPool.SUPER_COCKTAIL)
            && !KoLCharacter.hasSkill(SkillPool.MIXOLOGIST))
        || !KoLCharacter.isMoxieClass()) {
      return 0;
    }

    if (!KoLCharacter.getGuildStoreOpen() && !KoLCharacter.isSneakyPete()) {
      // If we haven't unlocked the guild, the still isn't available.
      return 0;
    }

    if (KoLCharacter.stillsAvailable == -1) {
      // Avoid infinite recursion if this request fails, or indirectly
      // calls getStillsAvailable();
      KoLCharacter.stillsAvailable = 0;
      RequestThread.postRequest(new ShopRequest("still"));
    }

    return KoLCharacter.stillsAvailable;
  }

  public static final boolean tripleReagent() {
    return KoLCharacter.tripleReagent;
  }

  public static final void setStillsAvailable(final int stillsAvailable) {
    if (KoLCharacter.stillsAvailable != stillsAvailable) {
      KoLCharacter.stillsAvailable = stillsAvailable;
      ConcoctionDatabase.setRefreshNeeded(false);
      // Allow Daily Deeds to update when the number of stills changes
      PreferenceListenerRegistry.firePreferenceChanged("(stills)");
    }
  }

  public static final void decrementStillsAvailable(final int decrementAmount) {
    KoLCharacter.setStillsAvailable(KoLCharacter.stillsAvailable - decrementAmount);
  }

  public static final boolean getDispensaryOpen() {
    return KoLCharacter.getAscensions() == Preferences.getInteger("lastDispensaryOpen")
        && InventoryManager.hasItem(ItemPool.LAB_KEY);
  }

  public static final boolean getTempleUnlocked() {
    return KoLCharacter.getAscensions() == Preferences.getInteger("lastTempleUnlock");
  }

  public static final void setTempleUnlocked() {
    if (KoLCharacter.getAscensions() != Preferences.getInteger("lastTempleUnlock")) {
      QuestDatabase.setQuestProgress(Quest.TEMPLE, QuestDatabase.FINISHED);
      Preferences.setInteger("lastTempleUnlock", KoLCharacter.getAscensions());
      // If quest Gotta Worship Them All is started, this completes step 1
      if (QuestDatabase.isQuestStarted(Quest.WORSHIP)) {
        QuestDatabase.setQuestProgress(Quest.WORSHIP, "step1");
      }
    }
  }

  public static final boolean getTrapperQuestCompleted() {
    return KoLCharacter.getAscensions() == Preferences.getInteger("lastTr4pz0rQuest");
  }

  public static final boolean getGuildStoreOpen() {
    if (KoLCharacter.inNuclearAutumn() || KoLCharacter.inPokefam()) {
      return false;
    }

    if (KoLCharacter.getAscensions() == Preferences.getInteger("lastGuildStoreOpen")) {
      return true;
    }

    if (KoLCharacter.guildStoreStateKnown) {
      return false;
    }

    // Only the six original character classes can join a guild
    if (!ascensionClass.isStandard()) {
      KoLCharacter.guildStoreStateKnown = true;
      return false;
    }

    RequestThread.postRequest(new GuildRequest());
    return KoLCharacter.getAscensions() == Preferences.getInteger("lastGuildStoreOpen");
  }

  public static void setGuildStoreOpen(boolean isOpen) {
    if (isOpen) {
      Preferences.setInteger("lastGuildStoreOpen", KoLCharacter.getAscensions());
    }
    KoLCharacter.guildStoreStateKnown = true;
  }

  public static final boolean canUseWok() {
    return KoLCharacter.hasSkill(SkillPool.TRANSCENDENTAL_NOODLECRAFTING)
        && KoLCharacter.isMysticalityClass();
  }

  public static final boolean canUseMalus() {
    return KoLCharacter.hasSkill(SkillPool.PULVERIZE)
        && KoLCharacter.isMuscleClass()
        && KoLCharacter.getGuildStoreOpen();
  }

  public static final boolean canPickpocket() {
    return !KoLCharacter.getLimitMode().limitPickpocket()
        && (ascensionClass == AscensionClass.DISCO_BANDIT
            || ascensionClass == AscensionClass.ACCORDION_THIEF
            || ascensionClass == AscensionClass.AVATAR_OF_SNEAKY_PETE
            || ascensionClass == AscensionClass.GELATINOUS_NOOB
            || KoLCharacter.getLimitMode() == LimitMode.BIRD
            || KoLCharacter.hasEquipped(ItemPool.FOCUSED_MAGNETRON_PISTOL, Slot.WEAPON)
            || KoLCharacter.hasEquipped(ItemPool.TINY_BLACK_HOLE, Slot.OFFHAND)
            || KoLCharacter.hasEquipped(ItemPool.MIME_ARMY_INFILTRATION_GLOVE));
  }

  public static final boolean isTorsoAware() {
    return KoLCharacter.hasSkill(SkillPool.TORSO) || KoLCharacter.hasSkill(SkillPool.BEST_DRESSED);
  }

  /**
   * Accessor method to set arena wins
   *
   * @param wins The number of arena wins
   */
  public static final void setArenaWins(final int wins) {
    KoLCharacter.arenaWins = wins;
  }

  /**
   * Accessor method to find the specified usable familiar.
   *
   * @param race The race of the familiar to find
   * @return familiar The first familiar matching this race
   */
  public static FamiliarData usableFamiliar(final String race) {
    return usableFamiliar(f -> f.getRace().equalsIgnoreCase(race));
  }

  /**
   * Accessor method to find the specified usable familiar.
   *
   * @param familiarId The id of the familiar to find
   * @return familiar The first familiar matching this id
   */
  public static FamiliarData usableFamiliar(final int familiarId) {
    return usableFamiliar(f -> f.getId() == familiarId);
  }

  private static FamiliarData usableFamiliar(final Predicate<FamiliarData> familiarFilter) {
    // Quick check against NO_FAMILIAR
    if (familiarFilter.test(FamiliarData.NO_FAMILIAR)) return FamiliarData.NO_FAMILIAR;

    // Don't even look if you are an Avatar
    if (!KoLCharacter.getPath().canUseFamiliars()) return null;

    // In Quantum Terrarium the player only has the familiar that is with them
    if (KoLCharacter.inQuantum()) {
      return familiarFilter.test(KoLCharacter.currentFamiliar)
          ? KoLCharacter.currentFamiliar
          : null;
    }

    return KoLCharacter.familiars.stream()
        .filter(familiarFilter)
        .filter(KoLCharacter::isUsable)
        .findAny()
        .orElse(null);
  }

  private static Stream<Integer> graftedFamiliars() {
    return Stream.of(
            "zootGraftedButtCheekLeftFamiliar",
            "zootGraftedButtCheekRightFamiliar",
            "zootGraftedFootLeftFamiliar",
            "zootGraftedFootRightFamiliar",
            "zootGraftedHandLeftFamiliar",
            "zootGraftedHandRightFamiliar",
            "zootGraftedHeadFamiliar",
            "zootGraftedNippleLeftFamiliar",
            "zootGraftedNippleRightFamiliar",
            "zootGraftedShoulderLeftFamiliar",
            "zootGraftedShoulderRightFamiliar")
        .map(pref -> Preferences.getInteger(pref))
        .filter(id -> id > 0);
  }

  private static boolean isUsable(FamiliarData f) {
    if (f == FamiliarData.NO_FAMILIAR) return !KoLCharacter.inQuantum();

    if (KoLCharacter.inPokefam()) {
      // pokefam-only familiars are usable
      if (FamiliarDatabase.isPokefamType(f.getId())) {
        return true;
      }
    }

    return StandardRequest.isAllowed(f)
        && (!KoLCharacter.inZombiecore() || f.isUndead())
        && (!KoLCharacter.inBeecore() || !KoLCharacter.hasBeeosity(f.getRace()))
        && (!KoLCharacter.inGLover() || KoLCharacter.hasGs(f.getRace()))
        && (!KoLCharacter.inZootomist()
            || !KoLCharacter.graftedFamiliars().anyMatch(id -> id == f.getId()));
  }

  /**
   * Accessor method to find the specified owned familiar.
   *
   * @param race The race of the familiar to find
   * @return familiar The first familiar matching this race
   */
  public static Optional<FamiliarData> ownedFamiliar(final String race) {
    return ownedFamiliar(f -> f.getRace().equalsIgnoreCase(race));
  }

  /**
   * Accessor method to find the specified owned familiar.
   *
   * @param familiarId The id of the familiar to find
   * @return familiar The first familiar matching this id
   */
  public static Optional<FamiliarData> ownedFamiliar(final int familiarId) {
    return ownedFamiliar(f -> f.getId() == familiarId);
  }

  private static Optional<FamiliarData> ownedFamiliar(
      final Predicate<FamiliarData> familiarFilter) {
    // Quick check against NO_FAMILIAR
    if (familiarFilter.test(FamiliarData.NO_FAMILIAR)) return Optional.of(FamiliarData.NO_FAMILIAR);

    return KoLCharacter.familiars.stream().filter(familiarFilter).findAny();
  }

  public static final boolean canUseFamiliar(final int familiarId) {
    return KoLCharacter.usableFamiliar(familiarId) != null;
  }

  private static AdventureResult STILLSUIT = ItemPool.get(ItemPool.STILLSUIT);

  /**
   * Accessor method to set the data for the current familiar.
   *
   * @param familiar The new current familiar
   */
  public static final void setFamiliar(final FamiliarData familiar) {
    if (KoLCharacter.currentFamiliar.equals(familiar)) {
      return;
    }

    // In Quantum Terrarium, when the next familiar comes up it keeps the
    // previous familiar's item unless it cannot equip it, in which case it is
    // returned to the player's inventory.
    if (KoLCharacter.inQuantum()) {
      FamiliarRequest.handleFamiliarChange(familiar);
      EquipmentManager.updateEquipmentList(Slot.FAMILIAR);
    }

    if (KoLCharacter.currentFamiliar != FamiliarData.NO_FAMILIAR) {
      KoLCharacter.currentFamiliar.deactivate();
    }

    var previousFamiliar = KoLCharacter.currentFamiliar;
    KoLCharacter.currentFamiliar = KoLCharacter.addFamiliar(familiar);

    if (previousFamiliar.getItem().equals(STILLSUIT)) {
      var stillsuitFamiliar =
          KoLCharacter.usableFamiliar(Preferences.getString("stillsuitFamiliar"));
      if (stillsuitFamiliar != null
          && stillsuitFamiliar != familiar
          && stillsuitFamiliar != previousFamiliar) {
        KoLmafia.updateDisplay("Giving your tiny stillsuit to your stillsuit familiar...");
        RequestThread.postRequest(new FamiliarRequest(previousFamiliar, EquipmentRequest.UNEQUIP));
        RequestThread.postRequest(new FamiliarRequest(stillsuitFamiliar, STILLSUIT));
      }
    }

    if (KoLCharacter.currentFamiliar != FamiliarData.NO_FAMILIAR) {
      KoLCharacter.currentFamiliar.activate();
    }

    if (KoLCharacter.currentFamiliar.equals(KoLCharacter.currentEnthroned)) {
      KoLCharacter.currentEnthroned = FamiliarData.NO_FAMILIAR;
    }

    if (KoLCharacter.currentFamiliar.equals(KoLCharacter.currentBjorned)) {
      KoLCharacter.currentBjorned = FamiliarData.NO_FAMILIAR;
    }

    KoLCharacter.familiars.setSelectedItem(KoLCharacter.currentFamiliar);
    EquipmentManager.setEquipment(Slot.FAMILIAR, KoLCharacter.currentFamiliar.getItem());

    KoLCharacter.isUsingStabBat =
        KoLCharacter.currentFamiliar.getRace().equals("Stab Bat")
            || KoLCharacter.currentFamiliar.getRace().equals("Scary Death Orb");

    EquipmentManager.updateEquipmentList(Slot.FAMILIAR);
    GearChangePanel.updateFamiliars();

    KoLCharacter.effectiveFamiliar = familiar;

    // Set the default image for this familiar. A subsequent
    // charpane update may change it.
    KoLCharacter.setFamiliarImage();

    KoLCharacter.recalculateAdjustments();
    KoLCharacter.updateStatus();
  }

  public static final void resetEffectiveFamiliar() {
    KoLCharacter.setEffectiveFamiliar(KoLCharacter.currentFamiliar);
  }

  public static final void setEffectiveFamiliar(final FamiliarData familiar) {
    KoLCharacter.effectiveFamiliar = familiar;
    KoLCharacter.recalculateAdjustments();
    KoLCharacter.updateStatus();
  }

  public static final void setEnthroned(final FamiliarData familiar) {
    KoLCharacter.currentEnthroned =
        familiar == null ? FamiliarData.NO_FAMILIAR : KoLCharacter.addFamiliar(familiar);
    KoLCharacter.recalculateAdjustments();
    KoLCharacter.updateStatus();
    NamedListenerRegistry.fireChange("(throne)");
  }

  public static final void setBjorned(final FamiliarData familiar) {
    KoLCharacter.currentBjorned =
        familiar == null ? FamiliarData.NO_FAMILIAR : KoLCharacter.addFamiliar(familiar);
    KoLCharacter.recalculateAdjustments();
    KoLCharacter.updateStatus();
    NamedListenerRegistry.fireChange("(bjorn)");
  }

  /** Accessor method to increment the weight of the current familiar by one. */
  public static final void incrementFamiliarWeight() {
    if (KoLCharacter.currentFamiliar != null) {
      KoLCharacter.currentFamiliar.setWeight(KoLCharacter.currentFamiliar.getWeight() + 1);
    }
  }

  /**
   * Adds the given familiar to the list of owned familiars.
   *
   * @param familiar The Id of the familiar to be added
   */
  public static final FamiliarData addFamiliar(final FamiliarData familiar) {
    if (familiar == null) {
      return null;
    }

    int index = KoLCharacter.familiars.indexOf(familiar);
    if (index >= 0) {
      return KoLCharacter.familiars.get(index);
    }

    KoLCharacter.familiars.add(familiar);
    if (!familiar.getItem().equals(EquipmentRequest.UNEQUIP)) {
      EquipmentManager.processResult(familiar.getItem());
    }

    GearChangePanel.updateFamiliars();

    return familiar;
  }

  /**
   * Remove the given familiar from the list of owned familiars.
   *
   * @param familiar The Id of the familiar to be removed
   */
  public static final void removeFamiliar(final FamiliarData familiar) {
    if (familiar == null) {
      return;
    }

    int index = KoLCharacter.familiars.indexOf(familiar);
    if (index < 0) {
      return;
    }

    if (KoLCharacter.currentFamiliar == familiar) {
      KoLCharacter.currentFamiliar = FamiliarData.NO_FAMILIAR;
      EquipmentManager.setEquipment(Slot.FAMILIAR, EquipmentRequest.UNEQUIP);
    }

    KoLCharacter.familiars.remove(familiar);
    GearChangePanel.updateFamiliars();
  }

  /**
   * Returns the list of familiars usable by the character, as a LockableListModel.
   *
   * @return The list of familiars usable by the character
   */
  public static final LockableListModel<FamiliarData> getFamiliarList() {
    return KoLCharacter.familiars;
  }

  /**
   * Returns the list of familiars owned by the character.
   *
   * @return The list of familiars owned by the character
   */
  public static List<FamiliarData> ownedFamiliars() {
    return KoLCharacter.familiars;
  }

  /**
   * Returns the list of familiars usable by the character.
   *
   * @return The list of familiars usable by the character
   */
  public static List<FamiliarData> usableFamiliars() {
    if (!KoLCharacter.getPath().canUseFamiliars()) return List.of(FamiliarData.NO_FAMILIAR);

    if (KoLCharacter.inQuantum()) return List.of(currentFamiliar);

    return KoLCharacter.familiars.stream().filter(KoLCharacter::isUsable).toList();
  }

  /*
   * Pasta Thralls
   */

  public static LockableListModel<PastaThrallData> getPastaThrallList() {
    return KoLCharacter.pastaThralls;
  }

  public static PastaThrallData currentPastaThrall() {
    return KoLCharacter.currentPastaThrall;
  }

  public static PastaThrallData findPastaThrall(final String type) {
    if (ascensionClass != AscensionClass.PASTAMANCER) {
      return null;
    }

    // Some paths don't allow the Pastamancer access to their normal skills
    if (KoLCharacter.inNuclearAutumn()) {
      return null;
    }

    if (PastaThrallData.NO_THRALL.getType().equals(type)) {
      return PastaThrallData.NO_THRALL;
    }

    return KoLCharacter.pastaThralls.stream()
        .filter(t -> t.getType().equals(type))
        .findFirst()
        .orElse(null);
  }

  public static PastaThrallData findPastaThrall(final int thrallId) {
    if (ascensionClass != AscensionClass.PASTAMANCER) {
      return null;
    }

    // Some paths don't allow the Pastamancer access to their normal skills
    if (KoLCharacter.inNuclearAutumn()) {
      return null;
    }

    if (thrallId == 0) {
      return PastaThrallData.NO_THRALL;
    }

    return KoLCharacter.pastaThralls.stream()
        .filter(t -> t.getId() == thrallId)
        .findAny()
        .orElse(null);
  }

  public static void setPastaThrall(final PastaThrallData thrall) {
    if (KoLCharacter.currentPastaThrall == thrall) {
      return;
    }

    if (thrall == PastaThrallData.NO_THRALL) {
      int skillId = SkillPool.DISMISS_PASTA_THRALL;
      UseSkillRequest skill = UseSkillRequest.getUnmodifiedInstance(skillId);
      KoLConstants.availableSkills.remove(skill);
      KoLConstants.availableSkillsSet.remove(skillId);
      KoLConstants.usableSkills.remove(skill);
      KoLConstants.summoningSkills.remove(skill);
    } else if (KoLCharacter.currentPastaThrall == PastaThrallData.NO_THRALL) {
      int skillId = SkillPool.DISMISS_PASTA_THRALL;
      UseSkillRequest skill = UseSkillRequest.getUnmodifiedInstance(skillId);
      KoLConstants.availableSkills.add(skill);
      KoLConstants.availableSkillsSet.add(skillId);
      KoLConstants.usableSkills.add(skill);
      LockableListFactory.sort(KoLConstants.usableSkills);
      KoLConstants.summoningSkills.add(skill);
      LockableListFactory.sort(KoLConstants.summoningSkills);
    }

    KoLCharacter.currentPastaThrall = thrall;
  }

  /**
   * Returns the string used on the character pane to detrmine how many points remain until the
   * character's next level.
   *
   * @return The string indicating the TNP advancement
   */
  public static final String getAdvancement() {
    int level = KoLCharacter.getLevel();
    return KoLConstants.COMMA_FORMAT.format(
            level * level + 4 - KoLCharacter.calculateBasePoints(KoLCharacter.getTotalPrime()))
        + " "
        + AdventureResult.STAT_NAMES[KoLCharacter.getPrimeIndex()]
        + " until level "
        + (level + 1);
  }

  /** Returns the character's zapping wand, if any */
  public static final AdventureResult getZapper() {
    // Look for wand

    AdventureResult wand = KoLCharacter.findWand();

    if (wand != null) {
      return wand;
    }

    // None found.  If you've already had a zapper wand this
    // ascension, assume they don't want to use their mimic.

    if (KoLCharacter.getAscensions() == Preferences.getInteger("lastZapperWand")) {
      return null;
    }

    // Use a mimic if one in inventory

    AdventureResult mimic = ItemPool.get(ItemPool.DEAD_MIMIC, 1);

    if (!InventoryManager.hasItem(mimic)) {
      return null;
    }

    RequestThread.postRequest(UseItemRequest.getInstance(mimic));

    // Look for wand again

    return KoLCharacter.findWand();
  }

  public static AdventureResult findWand() {
    return Arrays.stream(KoLCharacter.WANDS)
        .filter(KoLConstants.inventory::contains)
        .peek((w) -> Preferences.setInteger("lastZapperWand", KoLCharacter.getAscensions()))
        .findAny()
        .orElse(null);
  }

  public static boolean hasEquipped(final AdventureResult item, final Slot equipmentSlot) {
    if (KoLCharacter.inHatTrick() && equipmentSlot == Slot.HAT) {
      return EquipmentManager.hasHatTrickHat(item.getItemId());
    }
    return EquipmentManager.getEquipment(equipmentSlot).getItemId() == item.getItemId();
  }

  public static boolean hasEquipped(final int itemId, final Slot equipmentSlot) {
    if (KoLCharacter.inHatTrick() && equipmentSlot == Slot.HAT) {
      return EquipmentManager.hasHatTrickHat(itemId);
    }
    return EquipmentManager.getEquipment(equipmentSlot).getItemId() == itemId;
  }

  public static boolean hasEquipped(final AdventureResult item) {
    if (KoLCharacter.inHatTrick()
        && ItemDatabase.getConsumptionType(item.getItemId()) == ConsumptionType.HAT) {
      return EquipmentManager.hasHatTrickHat(item.getItemId());
    }
    return KoLCharacter.equipmentSlot(item) != Slot.NONE;
  }

  public static boolean hasEquipped(final int itemId) {
    return KoLCharacter.hasEquipped(ItemPool.get(itemId, 1));
  }

  public static boolean hasEquipped(
      Map<Slot, AdventureResult> equipment, final AdventureResult item, final Slot equipmentSlot) {
    AdventureResult current = equipment.get(equipmentSlot);
    return current != null && (current.getItemId() == item.getItemId());
  }

  public static boolean hasEquipped(
      Map<Slot, AdventureResult> equipment, final AdventureResult item, Set<Slot> equipmentSlots) {
    return equipmentSlots.stream().anyMatch(s -> KoLCharacter.hasEquipped(equipment, item, s));
  }

  public static boolean hasEquipped(
      Map<Slot, AdventureResult> equipment, final AdventureResult item) {
    return switch (ItemDatabase.getConsumptionType(item.getItemId())) {
      case WEAPON -> KoLCharacter.hasEquipped(
          equipment, item, EnumSet.of(Slot.WEAPON, Slot.OFFHAND));
      case OFFHAND -> KoLCharacter.hasEquipped(
          equipment, item, EnumSet.of(Slot.OFFHAND, Slot.FAMILIAR));
      case HAT -> KoLCharacter.hasEquipped(equipment, item, Slot.HAT);
      case SHIRT -> KoLCharacter.hasEquipped(equipment, item, Slot.SHIRT);
      case PANTS -> KoLCharacter.hasEquipped(equipment, item, Slot.PANTS);
      case CONTAINER -> KoLCharacter.hasEquipped(equipment, item, Slot.CONTAINER);
      case ACCESSORY -> KoLCharacter.hasEquipped(equipment, item, SlotSet.ACCESSORY_SLOTS);
      case STICKER -> KoLCharacter.hasEquipped(equipment, item, SlotSet.STICKER_SLOTS);
      case CARD -> KoLCharacter.hasEquipped(equipment, item, Slot.CARDSLEEVE);
      case FOLDER -> KoLCharacter.hasEquipped(equipment, item, SlotSet.FOLDER_SLOTS);
      case FAMILIAR_EQUIPMENT -> KoLCharacter.hasEquipped(equipment, item, Slot.FAMILIAR);
      default -> false;
    };
  }

  private static Slot equipmentSlotFromSubset(final AdventureResult item, final Set<Slot> slots) {
    return slots.stream()
        .filter(s -> KoLCharacter.hasEquipped(item, s))
        .findFirst()
        .orElse(Slot.NONE);
  }

  private static Slot equipmentSlotFromSubset(final AdventureResult item, final Slot slot) {
    return KoLCharacter.hasEquipped(item, slot) ? slot : Slot.NONE;
  }

  public static final Slot equipmentSlot(final AdventureResult item) {
    return switch (ItemDatabase.getConsumptionType(item.getItemId())) {
      case WEAPON -> equipmentSlotFromSubset(item, EnumSet.of(Slot.WEAPON, Slot.OFFHAND));
      case OFFHAND -> equipmentSlotFromSubset(item, EnumSet.of(Slot.OFFHAND, Slot.FAMILIAR));
      case HAT -> equipmentSlotFromSubset(item, EnumSet.of(Slot.HAT, Slot.FAMILIAR));
      case SHIRT -> equipmentSlotFromSubset(item, Slot.SHIRT);
      case PANTS -> equipmentSlotFromSubset(item, Slot.PANTS);
      case CONTAINER -> equipmentSlotFromSubset(item, Slot.CONTAINER);
      case ACCESSORY -> equipmentSlotFromSubset(item, SlotSet.ACCESSORY_SLOTS);
      case STICKER -> equipmentSlotFromSubset(item, SlotSet.STICKER_SLOTS);
      case CARD -> equipmentSlotFromSubset(item, Slot.CARDSLEEVE);
      case FOLDER -> equipmentSlotFromSubset(item, SlotSet.FOLDER_SLOTS);
      case FAMILIAR_EQUIPMENT -> equipmentSlotFromSubset(item, Slot.FAMILIAR);
      default -> Slot.NONE;
    };
  }

  public static final void updateStatus() {
    CharacterListenerRegistry.updateStatus();

    // Allow Daily Deeds to change state based on character status
    PreferenceListenerRegistry.firePreferenceChanged("(character)");
  }

  public static final void updateSelectedLocation(KoLAdventure location) {
    if (location != KoLCharacter.selectedLocation) {
      KoLCharacter.selectedLocation = location;
      Modifiers.setLocation(location);
      KoLCharacter.recalculateAdjustments();
      KoLCharacter.updateStatus();
      PreferenceListenerRegistry.firePreferenceChanged("(location)");
    }
  }

  public static final KoLAdventure getSelectedLocation() {
    return KoLCharacter.selectedLocation;
  }

  public static final double estimatedBaseExp(double monsterLevel) {
    // 0.25 stats per monster ML + 0.33 stats per bonus ML, rounded to 2dp

    double baseStats = (Modifiers.getCurrentML() / 4.0);
    double bonusStats = monsterLevel / ((monsterLevel > 0) ? 3.0 : 4.0);
    return Math.round((baseStats + bonusStats) * 100d) / 100d;
  }

  public static final boolean recalculateAdjustments() {
    return KoLCharacter.recalculateAdjustments(false);
  }

  public static final boolean recalculateAdjustments(boolean debug) {
    return KoLCharacter.currentModifiers.set(
        KoLCharacter.recalculateAdjustments(
            debug,
            KoLCharacter.getMindControlLevel(),
            EquipmentManager.allEquipment(),
            KoLConstants.activeEffects,
            KoLCharacter.effectiveFamiliar,
            KoLCharacter.currentEnthroned,
            KoLCharacter.currentBjorned,
            null,
            Preferences.getString("_horsery"),
            Preferences.getString("boomBoxSong"),
            Modeable.getStateMap(),
            false));
  }

  public static final Modifiers recalculateAdjustments(
      boolean debug,
      int MCD,
      Map<Slot, AdventureResult> equipment,
      List<AdventureResult> effects,
      FamiliarData familiar,
      FamiliarData enthroned,
      FamiliarData bjorned,
      String custom,
      String horsery,
      String boomBox,
      Map<Modeable, String> modeables,
      boolean speculation) {
    int taoFactor = KoLCharacter.hasSkill(SkillPool.TAO_OF_THE_TERRAPIN) ? 2 : 1;

    Modifiers newModifiers = debug ? new DebugModifiers() : new Modifiers();
    Modifiers.setFamiliar(familiar);
    AdventureResult weapon = equipment.get(Slot.WEAPON);
    Modifiers.mainhandClass =
        weapon == null ? "" : EquipmentDatabase.getItemType(weapon.getItemId());
    AdventureResult offhand = equipment.get(Slot.OFFHAND);
    Modifiers.unarmed =
        (weapon == null || weapon == EquipmentRequest.UNEQUIP)
            && (offhand == null || offhand == EquipmentRequest.UNEQUIP);

    // Area-specific adjustments
    newModifiers.add(ModifierDatabase.getModifiers(ModifierType.LOC, Modifiers.currentLocation));
    newModifiers.add(ModifierDatabase.getModifiers(ModifierType.ZONE, Modifiers.currentZone));

    // Look at sign-specific adjustments
    newModifiers.addDouble(
        DoubleModifier.MONSTER_LEVEL, MCD, ModifierType.MCD, "Monster Control Device");
    newModifiers.add(
        ModifierDatabase.getModifiers(ModifierType.SIGN, KoLCharacter.ascensionSign.getName()));

    // Certain outfits give benefits to the character
    // Need to do this before the individual items, so that Hobo Power
    // from the outfit counts towards a Hodgman offhand.
    SpecialOutfit outfit = EquipmentManager.currentOutfit(equipment);
    if (outfit != null) {
      newModifiers.setString(StringModifier.OUTFIT, outfit.getName());
      newModifiers.add(ModifierDatabase.getModifiers(ModifierType.OUTFIT, outfit.getName()));
      // El Vibrato Relics may have additional benefits based on
      // punchcards inserted into the helmet:
      if (outfit.getOutfitId() == OutfitPool.VIBRATO_RELICS
          && Preferences.getInteger("lastEVHelmetReset") == KoLCharacter.getAscensions()) {
        int data = Preferences.getInteger("lastEVHelmetValue");
        for (int i = 9; i > 0; --i) {
          int level = data % 11;
          data /= 11;
          if (level > 0)
            switch (i) {
              case 1 -> newModifiers.addDouble(
                  DoubleModifier.WEAPON_DAMAGE, level * 20, ModifierType.EL_VIBRATO, "ATTACK");
              case 2 -> newModifiers.addDouble(
                  DoubleModifier.HP, level * 100, ModifierType.EL_VIBRATO, "BUILD");
              case 3 -> newModifiers.addDouble(
                  DoubleModifier.MP, level * 100, ModifierType.EL_VIBRATO, "BUFF");
              case 4 -> newModifiers.addDouble(
                  DoubleModifier.MONSTER_LEVEL, level * 10, ModifierType.EL_VIBRATO, "MODIFY");
              case 5 -> {
                newModifiers.addDouble(
                    DoubleModifier.HP_REGEN_MIN, level * 16, ModifierType.EL_VIBRATO, "REPAIR");
                newModifiers.addDouble(
                    DoubleModifier.HP_REGEN_MAX, level * 20, ModifierType.EL_VIBRATO, "REPAIR");
              }
              case 6 -> newModifiers.addDouble(
                  DoubleModifier.SPELL_DAMAGE_PCT, level * 10, ModifierType.EL_VIBRATO, "TARGET");
              case 7 -> newModifiers.addDouble(
                  DoubleModifier.INITIATIVE, level * 20, ModifierType.EL_VIBRATO, "SELF");
              case 8 -> {
                if (Modifiers.currentFamiliar.contains("megadrone")) {
                  newModifiers.addDouble(
                      DoubleModifier.FAMILIAR_WEIGHT, level * 10, ModifierType.EL_VIBRATO, "DRONE");
                }
              }
              case 9 -> newModifiers.addDouble(
                  DoubleModifier.DAMAGE_REDUCTION, level * 3, ModifierType.EL_VIBRATO, "WALL");
            }
        }
      }
    }

    // We need to compute and store smithsness before equipment that use it

    // Temporary custom modifier (e.g. Gel Noob absorbed equipment / skills)
    if (custom != null) {
      newModifiers.add(ModifierDatabase.parseModifiers(ModifierType.GENERATED, "custom", custom));
    }

    // Store some modifiers as statics
    Modifiers.smithsness = KoLCharacter.getSmithsnessModifier(equipment, effects);

    // Look at items
    for (var slot : SlotSet.SLOTS) {
      AdventureResult item = equipment.get(slot);
      if (item == EquipmentRequest.UNEQUIP) {
        continue;
      }
      KoLCharacter.addItemAdjustment(
          newModifiers,
          slot,
          item,
          equipment,
          enthroned,
          bjorned,
          modeables,
          speculation,
          taoFactor);
    }
    for (var hat : EquipmentManager.getHatTrickHats()) {
      AdventureResult item = ItemPool.get(hat);
      if (item == EquipmentRequest.UNEQUIP) {
        continue;
      }
      KoLCharacter.addItemAdjustment(
          newModifiers,
          Slot.HATS,
          item,
          equipment,
          enthroned,
          bjorned,
          modeables,
          speculation,
          taoFactor);
    }
    if (KoLCharacter.inHatTrick()) {
      EquipmentManager.getHatTrickHats().stream()
          .max(Comparator.comparingInt(EquipmentDatabase::getPower))
          .ifPresent(h -> addHatPower(newModifiers, h, taoFactor));
    }

    // Consider fake hands
    int fakeHands = EquipmentManager.getFakeHands();
    if (fakeHands > 0) {
      newModifiers.addDouble(
          DoubleModifier.WEAPON_DAMAGE,
          -1 * fakeHands,
          ModifierType.FAKE_HANDS,
          "fake hand (" + fakeHands + ")");
    }

    int brimstoneMonsterLevel = 1 << newModifiers.getBitmap(BitmapModifier.BRIMSTONE);
    // Brimstone was believed to affect monster level only if more than
    // one is worn, but this is confirmed to not be true now.
    // Also affects item/meat drop, but only one is needed
    if (brimstoneMonsterLevel > 1) {
      newModifiers.addDouble(
          DoubleModifier.MONSTER_LEVEL, brimstoneMonsterLevel, ModifierType.OUTFIT, "Brimstone");
      newModifiers.addDouble(
          DoubleModifier.MEATDROP, brimstoneMonsterLevel, ModifierType.OUTFIT, "Brimstone");
      newModifiers.addDouble(
          DoubleModifier.ITEMDROP, brimstoneMonsterLevel, ModifierType.OUTFIT, "Brimstone");
    }

    int cloathingLevel = 1 << newModifiers.getBitmap(BitmapModifier.CLOATHING);
    // Cloathing gives item/meat drop and all stats.
    if (cloathingLevel > 1) {
      newModifiers.addDouble(
          DoubleModifier.MOX_PCT, cloathingLevel, ModifierType.OUTFIT, "Cloathing");
      newModifiers.addDouble(
          DoubleModifier.MUS_PCT, cloathingLevel, ModifierType.OUTFIT, "Cloathing");
      newModifiers.addDouble(
          DoubleModifier.MYS_PCT, cloathingLevel, ModifierType.OUTFIT, "Cloathing");
      newModifiers.addDouble(
          DoubleModifier.MEATDROP, cloathingLevel, ModifierType.OUTFIT, "Cloathing");
      newModifiers.addDouble(
          DoubleModifier.ITEMDROP, cloathingLevel / 2, ModifierType.OUTFIT, "Cloathing");
    }

    int mcHugeLargeLevel = getMcHugeLargeLevel(newModifiers);
    if (mcHugeLargeLevel > 0) {
      newModifiers.addDouble(
          DoubleModifier.COLD_RESISTANCE, mcHugeLargeLevel, ModifierType.OUTFIT, "McHugeLarge");
      newModifiers.addDouble(
          DoubleModifier.HOT_DAMAGE, 5 * mcHugeLargeLevel, ModifierType.OUTFIT, "McHugeLarge");
      newModifiers.addDouble(
          DoubleModifier.INITIATIVE, 10 * mcHugeLargeLevel, ModifierType.OUTFIT, "McHugeLarge");
    }

    // Add modifiers from Passive Skills
    newModifiers.applyPassiveModifiers(debug);

    // For the sake of easier maintenance, execute a lot of extra
    // string comparisons when looking at status effects.

    for (AdventureResult effect : effects) {
      newModifiers.add(ModifierDatabase.getEffectModifiers(effect.getEffectId()));
    }

    // Add modifiers from campground equipment.
    for (AdventureResult item : KoLConstants.campground) {
      // Skip ginormous pumpkin growing in garden
      if (item.getItemId() == ItemPool.GINORMOUS_PUMPKIN) {
        continue;
      }
      for (int count = item.getCount(); count > 0; --count) {
        newModifiers.add(ModifierDatabase.getItemModifiers(item.getItemId()));
      }
    }

    // Add modifiers from Chateau
    for (AdventureResult item : KoLConstants.chateau) {
      newModifiers.add(ModifierDatabase.getItemModifiers(item.getItemId()));
    }

    // Add modifiers from dwelling
    AdventureResult dwelling = CampgroundRequest.getCurrentDwelling();
    newModifiers.add(ModifierDatabase.getItemModifiers(dwelling.getItemId()));

    if (InventoryManager.getCount(ItemPool.COMFY_BLANKET) > 0) {
      newModifiers.add(ModifierDatabase.getItemModifiers(ItemPool.COMFY_BLANKET));
    }

    if (HolidayDatabase.getRonaldPhase() == 5) {
      newModifiers.addDouble(
          DoubleModifier.RESTING_MP_PCT, 100, ModifierType.EVENT, "Moons (Ronald full)");
    }

    if (HolidayDatabase.getGrimacePhase() == 5) {
      newModifiers.addDouble(
          DoubleModifier.RESTING_HP_PCT, 100, ModifierType.EVENT, "Moons (Grimace full)");
    }

    // Add other oddball interactions
    newModifiers.applySynergies();

    // Add familiar effects based on calculated weight adjustment.
    newModifiers.applyFamiliarModifiers(familiar, equipment.get(Slot.FAMILIAR));

    // Add Pasta Thrall effects
    if (ascensionClass == AscensionClass.PASTAMANCER) {
      PastaThrallData thrall = KoLCharacter.currentPastaThrall;
      if (thrall != PastaThrallData.NO_THRALL) {
        newModifiers.add(ModifierDatabase.getModifiers(ModifierType.THRALL, thrall.getType()));
      }
    }

    // Add in strung-up quartet.
    if (KoLCharacter.getAscensions() == Preferences.getInteger("lastQuartetAscension")) {
      switch (Preferences.getInteger("lastQuartetRequest")) {
        case 1 -> newModifiers.addDouble(
            DoubleModifier.MONSTER_LEVEL, 5, ModifierType.BALLROOM, "ML");
        case 2 -> newModifiers.addDouble(
            DoubleModifier.COMBAT_RATE, -5, ModifierType.BALLROOM, "Combat");
        case 3 -> newModifiers.addDouble(DoubleModifier.ITEMDROP, 5, ModifierType.BALLROOM, "Item");
      }
    }

    // Mummery
    newModifiers.add(mummeryMods.get());

    // Add modifiers from inventory
    if (InventoryManager.getCount(ItemPool.FISHING_POLE) > 0) {
      newModifiers.addDouble(
          DoubleModifier.FISHING_SKILL, 20, ModifierType.INVENTORY_ITEM, "fishin' pole");
    }
    if (InventoryManager.getCount(ItemPool.ANTIQUE_TACKLEBOX) > 0) {
      newModifiers.addDouble(
          DoubleModifier.FISHING_SKILL, 5, ModifierType.INVENTORY_ITEM, "antique tacklebox");
    }

    // Boombox, no check for having one so it can work with Maximizer "show things you don't have"
    newModifiers.add(ModifierDatabase.getModifiers(ModifierType.BOOM_BOX, boomBox));

    // Apply variable location modifiers
    newModifiers.applyAutumnatonModifiers();
    newModifiers.applyFloristModifiers();

    // Horsery
    newModifiers.add(ModifierDatabase.getModifiers(ModifierType.HORSERY, horsery));

    // Voting Booth
    newModifiers.add(voteMods.get());

    // Miscellaneous
    newModifiers.add(ModifierDatabase.getModifiers(ModifierType.GENERATED, "_userMods"));
    Modifiers fightMods = ModifierDatabase.getModifiers(ModifierType.GENERATED, "fightMods");
    newModifiers.add(fightMods);

    // Store some modifiers as statics
    Modifiers.hoboPower = newModifiers.getDouble(DoubleModifier.HOBO_POWER);

    if (Modifiers.currentLocation.equals("The Slime Tube")) {
      int hatred = (int) newModifiers.getDouble(DoubleModifier.SLIME_HATES_IT);
      if (hatred > 0) {
        newModifiers.addDouble(
            DoubleModifier.MONSTER_LEVEL,
            Math.min(1000, 15 * hatred * (hatred + 2)),
            ModifierType.OUTFIT,
            "Slime Hatred");
      }
    }

    var seadentWaveZone = Preferences.getString("_seadentWaveZone");
    if (!seadentWaveZone.isEmpty() && Modifiers.currentLocation.equals(seadentWaveZone)) {
      // this is a bonus of +30 to item, probably the same for meat + init
      newModifiers.addDouble(DoubleModifier.ITEMDROP, 30, ModifierType.LOC, "Summon a Wave");
      newModifiers.addDouble(DoubleModifier.MEATDROP, 30, ModifierType.LOC, "Summon a Wave");
      newModifiers.addDouble(DoubleModifier.INITIATIVE, 30, ModifierType.LOC, "Summon a Wave");
    }

    if (AdventureDatabase.isUnderwater(Modifiers.currentLocation)) {
      if (!Preferences.getString("seahorseName").isEmpty()) {
        newModifiers.addDouble(
            DoubleModifier.INITIATIVE, 100, ModifierType.SEAHORSE, "Tamed seahorse");
      }
    }

    // Add modifiers from Current Path
    newModifiers.add(
        ModifierDatabase.getModifiers(ModifierType.PATH, KoLCharacter.ascensionPath.toString()));

    // Add modifiers from Current Class
    newModifiers.add(
        ModifierDatabase.getModifiers(ModifierType.CLASS, KoLCharacter.getAscensionClassName()));

    // Add modifiers from today's events (Holidays, stat days etc)
    for (var event : HolidayDatabase.getEvents()) {
      newModifiers.add(ModifierDatabase.getModifiers(ModifierType.EVENT, event));
    }

    newModifiers.applyMotorbikeModifiers();

    // If in Nuclear Autumn, add Radiation Sickness

    if (KoLCharacter.inNuclearAutumn() && KoLCharacter.getRadSickness() > 0) {
      newModifiers.addDouble(
          DoubleModifier.MUS, -KoLCharacter.getRadSickness(), ModifierType.PATH, "Rads");
      newModifiers.addDouble(
          DoubleModifier.MYS, -KoLCharacter.getRadSickness(), ModifierType.PATH, "Rads");
      newModifiers.addDouble(
          DoubleModifier.MOX, -KoLCharacter.getRadSickness(), ModifierType.PATH, "Rads");
    }

    if (KoLCharacter.inAxecore() && KoLCharacter.currentInstrument != null) {
      newModifiers.applyMinstrelModifiers(
          KoLCharacter.minstrelLevel, KoLCharacter.currentInstrument);
    }

    if (KoLCharacter.isJarlsberg() && KoLCharacter.companion != null) {
      newModifiers.applyCompanionModifiers(KoLCharacter.companion);
    }

    if (KoLCharacter.isEd() && EdServantData.currentServant() != null) {
      newModifiers.applyServantModifiers(EdServantData.currentServant());
    }

    if (KoLCharacter.inNoobcore()) {
      newModifiers.add(
          ModifierDatabase.getModifiers(ModifierType.GENERATED, "Enchantments Absorbed"));
    }

    if (KoLCharacter.inDisguise() && KoLCharacter.getMask() != null) {
      newModifiers.add(ModifierDatabase.getModifiers(ModifierType.MASK, KoLCharacter.getMask()));
    }

    if (KoLCharacter.isVampyre()) {
      MonsterData ensorcelee = MonsterDatabase.findMonster(Preferences.getString("ensorcelee"));
      if (ensorcelee != null) {
        newModifiers.add(
            ModifierDatabase.getModifiers(
                ModifierType.ENSORCEL, ensorcelee.getPhylum().toString()));
      }
    }

    if (KoLCharacter.inRobocore()) {
      YouRobotManager.addRobotModifiers(newModifiers);
    }

    if (VYKEACompanionData.currentCompanion() != VYKEACompanionData.NO_COMPANION) {
      newModifiers.applyCompanionModifiers(VYKEACompanionData.currentCompanion());
    }

    // add additional rollover adventures
    newModifiers.applyAdditionalRolloverAdventureModifiers();

    // Organ capacity
    newModifiers.applyAdditionalStomachCapacityModifiers();
    newModifiers.applyAdditionalSpleenCapacityModifiers();

    // free rests
    newModifiers.applyAdditionalFreeRestModifiers();

    // Lastly, experience adjustment also implicitly depends on
    // monster level.  Add that information.

    // Water level impacts experience adjustment.
    if (KoLCharacter.inRaincore()) {
      int WL = 1;
      if (Modifiers.currentLocation != null) {
        KoLAdventure location = AdventureDatabase.getAdventure(Modifiers.currentLocation);
        if (location != null) {
          WL = location.getWaterLevel();
        }
      }
      if (WL > 0) {
        WL += (int) KoLCharacter.currentModifiers.getDouble(DoubleModifier.WATER_LEVEL);
        WL = WL < 1 ? 1 : Math.min(WL, 6);
        newModifiers.addDouble(
            DoubleModifier.EXPERIENCE,
            (double) WL * 10 / 3.0,
            ModifierType.PATH,
            "Water Level*10/3");
      }
    }

    double baseExp =
        KoLCharacter.estimatedBaseExp(newModifiers.getDouble(DoubleModifier.MONSTER_LEVEL));

    double exp = newModifiers.getDouble(DoubleModifier.EXPERIENCE);

    if (KoLCharacter.inTheSource()) {
      // 1/3 base exp and exp when in The Source path
      baseExp = baseExp / 3;
      exp = exp / 3;
    }

    if (exp != 0.0) {
      String tuning = newModifiers.getString(StringModifier.STAT_TUNING);
      int prime = KoLCharacter.getPrimeIndex();
      if (tuning.startsWith("Muscle")) prime = 0;
      else if (tuning.startsWith("Mysticality")) prime = 1;
      else if (tuning.startsWith("Moxie")) prime = 2;

      boolean all = tuning.endsWith("(all)");

      // Experience percentage modifiers
      record StatExp(DoubleModifier exp, DoubleModifier pct) {}
      var MUS = new StatExp(DoubleModifier.MUS_EXPERIENCE, DoubleModifier.MUS_EXPERIENCE_PCT);
      var MYS = new StatExp(DoubleModifier.MYS_EXPERIENCE, DoubleModifier.MYS_EXPERIENCE_PCT);
      var MOX = new StatExp(DoubleModifier.MOX_EXPERIENCE, DoubleModifier.MOX_EXPERIENCE_PCT);

      double finalBaseExp = baseExp;
      double finalExp = exp;
      var mods =
          switch (prime) {
            case 0 -> List.of(MUS, MYS, MOX);
            case 1 -> List.of(MYS, MOX, MUS);
            case 2 -> List.of(MOX, MUS, MYS);
            default -> throw new IllegalStateException("Unexpected value: " + prime);
          };
      Function<DoubleModifier, Double> calc =
          (DoubleModifier statPct) ->
              (finalBaseExp + finalExp) * (1 + newModifiers.getDouble(statPct) / 100.0);

      if (all) {
        var mod = mods.get(0);
        newModifiers.addDouble(mod.exp, 1 + calc.apply(mod.pct), ModifierType.CLASS, "EXP");
      } else {
        // Adjust for prime stat
        // The base +1 Exp for mainstat IS tuned
        var mod = mods.get(0);
        newModifiers.addDouble(mod.exp, 1 + calc.apply(mod.pct) / 2.0, ModifierType.CLASS, "EXP/2");
        mod = mods.get(1);
        newModifiers.addDouble(mod.exp, calc.apply(mod.pct) / 4.0, ModifierType.CLASS, "EXP/4");
        mod = mods.get(2);
        newModifiers.addDouble(mod.exp, calc.apply(mod.pct) / 4.0, ModifierType.CLASS, "EXP/4");
      }
    }

    // These depend on the modifiers from everything else, so they must be done last
    if (effects.contains(KoLCharacter.BENDIN_HELL)) {
      for (DoubleModifier modifier :
          List.of(
              DoubleModifier.HOT_DAMAGE,
              DoubleModifier.COLD_DAMAGE,
              DoubleModifier.STENCH_DAMAGE,
              DoubleModifier.SPOOKY_DAMAGE,
              DoubleModifier.SLEAZE_DAMAGE,
              DoubleModifier.HOT_SPELL_DAMAGE,
              DoubleModifier.COLD_SPELL_DAMAGE,
              DoubleModifier.STENCH_SPELL_DAMAGE,
              DoubleModifier.SPOOKY_SPELL_DAMAGE,
              DoubleModifier.SLEAZE_SPELL_DAMAGE)) {
        newModifiers.addDouble(
            modifier,
            newModifiers.getAccumulator(modifier),
            ModifierType.EFFECT,
            EffectPool.BENDIN_HELL);
      }
    }
    if (effects.contains(KoLCharacter.DIRTY_PEAR)) {
      for (DoubleModifier modifier :
          List.of(DoubleModifier.SLEAZE_DAMAGE, DoubleModifier.SLEAZE_SPELL_DAMAGE)) {
        newModifiers.addDouble(
            modifier,
            newModifiers.getAccumulator(modifier),
            ModifierType.EFFECT,
            EffectPool.DIRTY_PEAR);
      }
    }
    if (effects.contains(KoLCharacter.BOWLEGGED_SWAGGER)) {
      newModifiers.addDouble(
          DoubleModifier.INITIATIVE,
          newModifiers.getAccumulator(DoubleModifier.INITIATIVE),
          ModifierType.EFFECT,
          EffectPool.BOWLEGGED_SWAGGER);
      // Add "Physical Damage" here, when that is properly defined
    }
    if (effects.contains(KoLCharacter.OFFHAND_REMARKABLE) && !KoLCharacter.inGLover()) {
      addOffhandRemarkable(equipment, newModifiers);
    }
    if (equipment.get(Slot.SHIRT).getItemId() == ItemPool.MAKESHIFT_GARBAGE_SHIRT
        && (Preferences.getInteger("garbageShirtCharge") > 0
            || (speculation && !Preferences.getBoolean("_garbageItemChanged")))) {
      for (DoubleModifier modifier :
          List.of(
              DoubleModifier.EXPERIENCE,
              DoubleModifier.MUS_EXPERIENCE,
              DoubleModifier.MYS_EXPERIENCE,
              DoubleModifier.MOX_EXPERIENCE,
              DoubleModifier.MUS_EXPERIENCE_PCT,
              DoubleModifier.MYS_EXPERIENCE_PCT,
              DoubleModifier.MOX_EXPERIENCE_PCT)) {
        newModifiers.addDouble(
            modifier,
            newModifiers.getAccumulator(modifier),
            ModifierType.ITEM,
            ItemPool.MAKESHIFT_GARBAGE_SHIRT);
      }
    }

    // Some things are doubled by Squint and not champagne bottle, like Otoscope. So do champagne
    // first and then add in any that aren't doubled by champagne (should just be fightMods).
    // TOOD: double-check mummery, friar plants, meteor post-combat, crystal ball post-combat.
    if ((equipment.get(Slot.OFFHAND).getItemId() == ItemPool.BROKEN_CHAMPAGNE
            || equipment.get(Slot.WEAPON).getItemId() == ItemPool.BROKEN_CHAMPAGNE
            || equipment.get(Slot.FAMILIAR).getItemId() == ItemPool.BROKEN_CHAMPAGNE)
        && (Preferences.getInteger("garbageChampagneCharge") > 0
            || (speculation && !Preferences.getBoolean("_garbageItemChanged")))) {
      newModifiers.addDouble(
          DoubleModifier.ITEMDROP,
          newModifiers.getAccumulator(DoubleModifier.ITEMDROP),
          ModifierType.ITEM,
          ItemPool.BROKEN_CHAMPAGNE);
    }
    if (effects.contains(KoLCharacter.STEELY_EYED_SQUINT) && !KoLCharacter.inGLover()) {
      newModifiers.addDouble(
          DoubleModifier.ITEMDROP,
          newModifiers.getAccumulator(DoubleModifier.ITEMDROP),
          ModifierType.EFFECT,
          EffectPool.STEELY_EYED_SQUINT);
      // Add in fightMods to double Otoscope, since it's not otherwise included in extras.
      if (fightMods != null) {
        newModifiers.addDouble(
            DoubleModifier.ITEMDROP,
            fightMods.getDouble(DoubleModifier.ITEMDROP),
            ModifierType.ITEM,
            EffectPool.STEELY_EYED_SQUINT);
      }
    }
    if (Modifiers.currentZone.equals("Shadow Rift")) {
      newModifiers.addDouble(
          DoubleModifier.ITEMDROP,
          // It includes your current familiar
          newModifiers.getDouble(DoubleModifier.ITEMDROP) * -0.8,
          ModifierType.ZONE,
          "Shadow Rift");
    }

    // Determine whether or not data has changed
    if (debug) {
      DebugModifiers.finish();
    }

    return newModifiers;
  }

  public static void addItemAdjustment(
      Modifiers newModifiers,
      Slot slot,
      AdventureResult item,
      Map<Slot, AdventureResult> equipment,
      FamiliarData enthroned,
      FamiliarData bjorned,
      Map<Modeable, String> modeables,
      boolean speculation,
      int taoFactor) {
    if (item == null || item == EquipmentRequest.UNEQUIP) {
      return;
    }

    int itemId = item.getItemId();
    ConsumptionType consume = ItemDatabase.getConsumptionType(itemId);

    if (slot == Slot.FAMILIAR
        && (consume == ConsumptionType.HAT || consume == ConsumptionType.PANTS)) {
      // Hatrack hats don't get their normal enchantments
      // Scarecrow pants don't get their normal enchantments
      return;
    }

    Modifiers imod;

    if (slot == Slot.FAMILIAR
        && (consume == ConsumptionType.WEAPON || consume == ConsumptionType.OFFHAND)) {
      imod = ModifierDatabase.getItemModifiersInFamiliarSlot(itemId);

      if (consume == ConsumptionType.WEAPON) {
        addWeaponPower(newModifiers, itemId);
      }
    } else {
      imod = ModifierDatabase.getItemModifiers(itemId);
    }

    if (imod != null) {
      if (speculation) {
        String intrinsic = imod.getString(StringModifier.INTRINSIC_EFFECT);
        if (intrinsic.length() > 0) {
          newModifiers.add(ModifierDatabase.getModifiers(ModifierType.EFFECT, intrinsic));
        }
      }

      if (KoLCharacter.inNoobcore()
          || (KoLCharacter.inGLover() && !KoLCharacter.hasGs(item.getName()))) {
        // Remove MOST Numeric Modifiers from Items in Noobcore
        // and in G Lover if they don't contain G's
        Modifiers iModCopy = new Modifiers(imod);
        for (var mod : DoubleModifier.DOUBLE_MODIFIERS) {
          switch (mod) {
            case SLIME_HATES_IT:
              continue;
          }
          iModCopy.setDouble(mod, 0.0);
        }
        newModifiers.add(iModCopy);
      } else {
        newModifiers.add(imod);
      }
    }

    // Do appropriate things for specific items in Noobcore
    if (KoLCharacter.inNoobcore()) {
      switch (itemId) {
        case ItemPool.LATTE_MUG -> newModifiers.add(ModifierDatabase.getItemModifiers(itemId));
      }
    }

    // Do appropriate things for specific items
    if (!KoLCharacter.inNoobcore()
        && (!KoLCharacter.inGLover() || KoLCharacter.hasGs(item.getName()))) {
      switch (itemId) {
        case ItemPool.STICKER_SWORD, ItemPool.STICKER_CROSSBOW ->
        // Apply stickers
        SlotSet.STICKER_SLOTS.stream()
            .map(equipment::get)
            .filter(s -> s != null && s != EquipmentRequest.UNEQUIP)
            .map(AdventureResult::getItemId)
            .forEach((id) -> newModifiers.add(ModifierDatabase.getItemModifiers(id)));
        case ItemPool.CARD_SLEEVE -> {
          // Apply card
          AdventureResult card = equipment.get(Slot.CARDSLEEVE);
          if (card != null && card != EquipmentRequest.UNEQUIP) {
            newModifiers.add(ModifierDatabase.getItemModifiers(card.getItemId()));
          }
        }
        case ItemPool.FOLDER_HOLDER, ItemPool.REPLICA_FOLDER_HOLDER ->
        // Apply folders
        SlotSet.FOLDER_SLOTS.stream()
            .map(equipment::get)
            .filter(f -> f != null && f != EquipmentRequest.UNEQUIP)
            .map(AdventureResult::getItemId)
            .forEach((id) -> newModifiers.add(ModifierDatabase.getItemModifiers(id)));
        case ItemPool.COWBOY_BOOTS -> {
          AdventureResult skin = equipment.get(Slot.BOOTSKIN);
          AdventureResult spur = equipment.get(Slot.BOOTSPUR);
          if (skin != null && skin != EquipmentRequest.UNEQUIP) {
            newModifiers.add(ModifierDatabase.getItemModifiers(skin.getItemId()));
          }
          if (spur != null && spur != EquipmentRequest.UNEQUIP) {
            newModifiers.add(ModifierDatabase.getItemModifiers(spur.getItemId()));
          }
        }
        case ItemPool.HATSEAT ->
        // Apply enthroned familiar
        newModifiers.add(ModifierDatabase.getModifiers(ModifierType.THRONE, enthroned.getRace()));
        case ItemPool.BUDDY_BJORN ->
        // Apply bjorned familiar
        newModifiers.add(ModifierDatabase.getModifiers(ModifierType.BJORN, bjorned.getRace()));
        case ItemPool.VAMPYRIC_CLOAKE -> newModifiers.applyVampyricCloakeModifiers();
        case ItemPool.PRISMATIC_BERET -> newModifiers.applyPrismaticBeretModifiers(
            getTotalPower(equipment::get));
        default -> {
          var modeable = Modeable.find(itemId);
          if (modeable != null) {
            newModifiers.add(
                ModifierDatabase.getModifiers(modeable.getModifierType(), modeables.get(modeable)));
          }
        }
      }
    }

    // Add modifiers that depend on equipment power
    switch (slot) {
      case OFFHAND:
        if (consume != ConsumptionType.WEAPON) {
          break;
        }
        /*FALLTHRU*/
      case WEAPON:
        addWeaponPower(newModifiers, itemId);
        break;

      case HAT:
        addHatPower(newModifiers, itemId, taoFactor);
        break;

      case PANTS:
        addPantsPower(newModifiers, itemId, taoFactor);
        break;

      case SHIRT:
        addShirtPower(newModifiers, itemId);
        break;
    }

    if (inElevenThingIHateAboutU() && item.priority == AdventureResult.Priority.ITEM) {
      Lookup source = new Lookup(ModifierType.PATH, Path.ELEVEN_THINGS.getName());
      int eyes = getEyeosity(item.getName());
      int ewes = getEweosity(item.getName());
      BiConsumer<DoubleModifier, Integer> applyModifierIfNotEmpty =
          (mod, value) -> {
            if (value == 0) {
              return;
            }

            newModifiers.addDouble(mod, value, source);
          };

      switch (slot) {
        case HAT:
          applyModifierIfNotEmpty.accept(DoubleModifier.INITIATIVE, eyes * 25);
          applyModifierIfNotEmpty.accept(DoubleModifier.INITIATIVE, ewes * -50);
          break;
        case CONTAINER:
          applyModifierIfNotEmpty.accept(DoubleModifier.HP, eyes * 10);
          applyModifierIfNotEmpty.accept(DoubleModifier.MP, eyes * 5);
          applyModifierIfNotEmpty.accept(DoubleModifier.HP, ewes * -10);
          applyModifierIfNotEmpty.accept(DoubleModifier.MP, ewes * -5);
          break;
        case WEAPON:
          applyModifierIfNotEmpty.accept(DoubleModifier.WEAPON_DAMAGE, eyes * 10);
          applyModifierIfNotEmpty.accept(DoubleModifier.FUMBLE, ewes * 11);
          break;
        case OFFHAND:
          applyModifierIfNotEmpty.accept(DoubleModifier.ITEMDROP, eyes * 10);
          applyModifierIfNotEmpty.accept(DoubleModifier.MEATDROP, ewes * -10);
          break;
        case PANTS:
          applyModifierIfNotEmpty.accept(DoubleModifier.HOT_RESISTANCE, eyes);
          applyModifierIfNotEmpty.accept(DoubleModifier.COLD_RESISTANCE, eyes);
          applyModifierIfNotEmpty.accept(DoubleModifier.SPOOKY_RESISTANCE, eyes);
          applyModifierIfNotEmpty.accept(DoubleModifier.STENCH_RESISTANCE, eyes);
          applyModifierIfNotEmpty.accept(DoubleModifier.SLEAZE_RESISTANCE, eyes);
          applyModifierIfNotEmpty.accept(DoubleModifier.MEATDROP, ewes * -10);
          break;
        case ACCESSORY1:
        case ACCESSORY2:
        case ACCESSORY3:
          applyModifierIfNotEmpty.accept(DoubleModifier.MEATDROP, eyes * 10);
          applyModifierIfNotEmpty.accept(DoubleModifier.ITEMDROP, ewes * -10);
          break;
        case FAMILIAR:
          applyModifierIfNotEmpty.accept(DoubleModifier.FAMILIAR_WEIGHT, eyes * 5);
          applyModifierIfNotEmpty.accept(DoubleModifier.FAMILIAR_WEIGHT, ewes * -5);
          break;
      }
    }
  }

  private static final AdventureResult HAMMERTIME = EffectPool.get(EffectPool.HAMMERTIME);

  public static int getTotalPower() {
    return getTotalPower(EquipmentManager::getEquipment);
  }

  private static int getTotalPower(Function<Slot, AdventureResult> getEquipment) {
    int tao = KoLCharacter.hasSkill(SkillPool.TAO_OF_THE_TERRAPIN) ? 1 : 0;
    int hammertime = KoLConstants.activeEffects.contains(HAMMERTIME) ? 3 : 0;

    int hat =
        (KoLCharacter.inHatTrick()
                ? EquipmentManager.getHatTrickHats().stream()
                : Stream.of(getEquipment.apply(Slot.HAT).getItemId()))
            .mapToInt(EquipmentDatabase::getPower)
            .sum();

    int pants = EquipmentDatabase.getPower(getEquipment.apply(Slot.PANTS).getItemId());
    int shirt = EquipmentDatabase.getPower(getEquipment.apply(Slot.SHIRT).getItemId());

    return hat * (1 + tao) + pants * (1 + tao + hammertime) + shirt;
  }

  private static void addWeaponPower(Modifiers newModifiers, int itemId) {
    newModifiers.addDouble(
        DoubleModifier.WEAPON_DAMAGE,
        EquipmentDatabase.getPower(itemId) * 0.15,
        ModifierType.EQUIPMENT_POWER,
        "15% weapon power");
  }

  private static void addHatPower(Modifiers newModifiers, int itemId, int taoFactor) {
    addEquipPower("hat power", newModifiers, itemId, taoFactor);
  }

  private static void addPantsPower(Modifiers newModifiers, int itemId, int taoFactor) {
    addEquipPower("pants power", newModifiers, itemId, taoFactor);
  }

  private static void addShirtPower(Modifiers newModifiers, int itemId) {
    addEquipPower("shirt power", newModifiers, itemId, 1);
  }

  private static void addEquipPower(
      String equip, Modifiers newModifiers, int itemId, int taoFactor) {
    newModifiers.addDouble(
        DoubleModifier.DAMAGE_ABSORPTION,
        taoFactor * EquipmentDatabase.getPower(itemId),
        ModifierType.EQUIPMENT_POWER,
        equip);
  }

  private static void addOffhandRemarkable(
      Map<Slot, AdventureResult> equipment, Modifiers newModifiers) {
    var offhand = equipment.get(Slot.OFFHAND);
    addOffhandRemarkable(equipment, offhand, newModifiers);
    // and also offhand items that are equipped on the familiar
    var famItem = equipment.get(Slot.FAMILIAR);
    addOffhandRemarkable(equipment, famItem, newModifiers);
  }

  private static void addOffhandRemarkable(
      Map<Slot, AdventureResult> equipment, AdventureResult item, Modifiers newModifiers) {
    if (item == null
        || item == EquipmentRequest.UNEQUIP
        || ItemDatabase.getConsumptionType(item.id) != ConsumptionType.OFFHAND
        || item.id == ItemPool.LATTE_MUG
        // hobo items that convert hobo power don't have that conversion doubled
        || item.id >= ItemPool.HODGMANS_VARCOLAC_PAW && item.id <= ItemPool.HODGMANS_CANE) {
      return;
    }

    if (item.id == ItemPool.HODGMANS_HAMSTER) {
      // hamster has the stat bonus doubled but not the conversion
      var mods = ModifierDatabase.getItemModifiers(item.id);
      if (mods == null) {
        // argh wtf
        return;
      }
      mods.setDouble(DoubleModifier.ITEMDROP, 0);
      mods.setDouble(DoubleModifier.MEATDROP, 0);
      mods.setDouble(DoubleModifier.HP_REGEN_MIN, 0);
      mods.setDouble(DoubleModifier.HP_REGEN_MAX, 0);
      mods.setDouble(DoubleModifier.MP_REGEN_MAX, 0);
      mods.setDouble(DoubleModifier.MP_REGEN_MAX, 0);
      addModifiersWithOffHandRemarkable(newModifiers, mods);
      return;
    }

    if (item.id == ItemPool.MCHUGELARGE_LEFT_POLE) {
      // we implement the bonus as though it were an outfit bonus, but it is properly on the item
      int totalItems = newModifiers.getBitmap(BitmapModifier.MCHUGELARGE);

      if (totalItems > 0) {
        int mcHugeLargeLevel = getMcHugeLargeLevel(newModifiers);
        var mods = new Modifiers();
        addMcHugeLargeModifiers(mods, mcHugeLargeLevel / totalItems);
        addModifiersWithOffHandRemarkable(newModifiers, mods);
      }
    }

    // use sleeved card as source of modifiers if applicable
    if (item.id == ItemPool.CARD_SLEEVE) {
      item = equipment.get(Slot.CARDSLEEVE);
    }

    var mods = ModifierDatabase.getItemModifiers(item.id);
    addModifiersWithOffHandRemarkable(newModifiers, mods);
    for (var modeable : Modeable.values()) {
      if (item.id == modeable.getItemId()) {
        mods = ModifierDatabase.getModifiers(modeable.getModifierType(), modeable.getState());
        addModifiersWithOffHandRemarkable(newModifiers, mods);
      }
    }
  }

  private static void addModifiersWithOffHandRemarkable(Modifiers newModifiers, Modifiers mods) {
    var copyMods = new Modifiers(mods);
    copyMods.setLookup(new Lookup(ModifierType.EFFECT, EffectPool.OFFHAND_REMARKABLE));
    newModifiers.add(copyMods);
  }

  public static int applyInebrietyModifiers(Consumable consumable) {
    if (KoLCharacter.inElevenThingIHateAboutU()) {
      return consumable.getInebriety() + getEweosity(consumable.name);
    }
    return consumable.getInebriety();
  }

  public static final double getSmithsnessModifier(
      Map<Slot, AdventureResult> equipment, List<AdventureResult> effects) {
    double smithsness = 0;

    for (var slot : SlotSet.SLOTS) {
      AdventureResult item = equipment.get(slot);
      if (item != null) {
        int itemId = item.getItemId();
        smithsness += getSmithsnessModifier(itemId, slot);
      }
    }
    if (KoLCharacter.inHatTrick()) {
      for (var hat : EquipmentManager.getHatTrickHats()) {
        smithsness += getSmithsnessModifier(hat, Slot.HAT);
      }
    }

    for (AdventureResult effect : effects) {
      int effectId = effect.getEffectId();
      // Same as above - we know all effects that give smithsness, so manually check the ranges
      // those effect ids are in.
      if (effectId != EffectPool.VIDEO_GAMES
          && (effectId < EffectPool.MERRY_SMITHSNESS || effectId > EffectPool.SMITHSNESS_CHEER))
        continue;
      Modifiers emod = ModifierDatabase.getEffectModifiers(effect.getEffectId());
      if (emod != null) {
        smithsness += emod.getDouble(DoubleModifier.SMITHSNESS);
      }
    }
    return smithsness;
  }

  private static double getSmithsnessModifier(int itemId, Slot slot) {
    // We know all items that give smithsness, and this code needs to be performant, so just
    // check whether the id is between the first and last item.
    if (itemId < ItemPool.WORK_IS_A_FOUR_LETTER_SWORD
        || itemId > ItemPool.SHAKESPEARES_SISTERS_ACCORDION) return 0;
    Modifiers imod = ModifierDatabase.getItemModifiers(itemId);
    if (imod != null) {
      AscensionClass classType = AscensionClass.find(imod.getString(StringModifier.CLASS));
      if (classType == null
          || classType == ascensionClass
              && (slot != Slot.FAMILIAR
                  || KoLCharacter.getFamiliar().getId() == FamiliarPool.HAND)) {
        return imod.getDouble(DoubleModifier.SMITHSNESS);
      }
    }
    return 0;
  }

  // Per-character settings that change each ascension

  public static final void ensureUpdatedDwarfFactory() {
    int lastAscension = Preferences.getInteger("lastDwarfFactoryReset");
    if (lastAscension < KoLCharacter.getAscensions()) {
      Preferences.setInteger("lastDwarfFactoryReset", KoLCharacter.getAscensions());
      Preferences.setString("lastDwarfDiceRolls", "");
      Preferences.setString("lastDwarfDigitRunes", "-------");
      Preferences.setString("lastDwarfEquipmentRunes", "");
      Preferences.setString("lastDwarfHopper1", "");
      Preferences.setString("lastDwarfHopper2", "");
      Preferences.setString("lastDwarfHopper3", "");
      Preferences.setString("lastDwarfHopper4", "");
      Preferences.setString("lastDwarfFactoryItem118", "");
      Preferences.setString("lastDwarfFactoryItem119", "");
      Preferences.setString("lastDwarfFactoryItem120", "");
      Preferences.setString("lastDwarfFactoryItem360", "");
      Preferences.setString("lastDwarfFactoryItem361", "");
      Preferences.setString("lastDwarfFactoryItem362", "");
      Preferences.setString("lastDwarfFactoryItem363", "");
      Preferences.setString("lastDwarfFactoryItem364", "");
      Preferences.setString("lastDwarfFactoryItem365", "");
      Preferences.setString("lastDwarfFactoryItem910", "");
      Preferences.setString("lastDwarfFactoryItem3199", "");
      Preferences.setString("lastDwarfOfficeItem3208", "");
      Preferences.setString("lastDwarfOfficeItem3209", "");
      Preferences.setString("lastDwarfOfficeItem3210", "");
      Preferences.setString("lastDwarfOfficeItem3211", "");
      Preferences.setString("lastDwarfOfficeItem3212", "");
      Preferences.setString("lastDwarfOfficeItem3213", "");
      Preferences.setString("lastDwarfOfficeItem3214", "");
      Preferences.setString("lastDwarfOreRunes", "");
      DwarfFactoryRequest.reset();
    }
  }

  public static final void ensureUpdatedGuyMadeOfBees() {
    int lastAscension = Preferences.getInteger("lastGuyMadeOfBeesReset");
    if (lastAscension < KoLCharacter.getAscensions()) {
      Preferences.setInteger("lastGuyMadeOfBeesReset", KoLCharacter.getAscensions());
      Preferences.setInteger("guyMadeOfBeesCount", 0);
      Preferences.setBoolean("guyMadeOfBeesDefeated", false);
    }
  }

  public static final void ensureUpdatedAscensionCounters() {
    int lastAscension = Preferences.getInteger("beeCounter");
    if (lastAscension < KoLCharacter.getAscensions()) {
      Preferences.setInteger("beeCounter", 0);
    }
  }

  public static final void ensureUpdatedPotionEffects() {
    int lastAscension = Preferences.getInteger("lastBangPotionReset");
    if (lastAscension < KoLCharacter.getAscensions()) {
      Preferences.setInteger("lastBangPotionReset", KoLCharacter.getAscensions());
      for (int i = 819; i <= 827; ++i) {
        Preferences.setString("lastBangPotion" + i, "");
      }
      for (int i = ItemPool.VIAL_OF_RED_SLIME; i <= ItemPool.VIAL_OF_PURPLE_SLIME; ++i) {
        Preferences.setString("lastSlimeVial" + i, "");
      }
    }

    for (int i = 819; i <= 827; ++i) {
      String testProperty = Preferences.getString("lastBangPotion" + i);
      if (!testProperty.isEmpty()) {
        String name = ItemDatabase.getItemName(i);
        String testName = name + " of " + testProperty;
        String testPlural = name + "s of " + testProperty;
        ItemDatabase.registerItemAlias(i, testName, testPlural);
        // Update generic alias too
        testName = "potion of " + testProperty;
        ItemDatabase.registerItemAlias(i, testName, null);
      }
    }

    for (int i = ItemPool.VIAL_OF_RED_SLIME; i <= ItemPool.VIAL_OF_PURPLE_SLIME; ++i) {
      String testProperty = Preferences.getString("lastSlimeVial" + i);
      if (!testProperty.isEmpty()) {
        String name = ItemDatabase.getItemName(i);
        String testName = name + ": " + testProperty;
        String testPlural = ItemDatabase.getPluralName(i) + testProperty;
        ItemDatabase.registerItemAlias(i, testName, testPlural);
        // Update generic alias too
        testName = "vial of slime: " + testProperty;
        ItemDatabase.registerItemAlias(i, testName, null);
      }
    }
  }

  private static void ensureUpdatedSkatePark() {
    int lastAscension = Preferences.getInteger("lastSkateParkReset");
    if (lastAscension < KoLCharacter.getAscensions()) {
      Preferences.setString("skateParkStatus", "war");
      Preferences.setInteger("lastSkateParkReset", KoLCharacter.getAscensions());
    }
  }

  public static final void ensureUpdatedPirateInsults() {
    int lastAscension = Preferences.getInteger("lastPirateInsultReset");
    if (lastAscension < KoLCharacter.getAscensions()) {
      Preferences.setInteger("lastPirateInsultReset", KoLCharacter.getAscensions());
      Preferences.setBoolean("lastPirateInsult1", false);
      Preferences.setBoolean("lastPirateInsult2", false);
      Preferences.setBoolean("lastPirateInsult3", false);
      Preferences.setBoolean("lastPirateInsult4", false);
      Preferences.setBoolean("lastPirateInsult5", false);
      Preferences.setBoolean("lastPirateInsult6", false);
      Preferences.setBoolean("lastPirateInsult7", false);
      Preferences.setBoolean("lastPirateInsult8", false);
    }
  }

  public static final void ensureUpdatedCellar() {
    int lastAscension = Preferences.getInteger("lastCellarReset");
    if (lastAscension < KoLCharacter.getAscensions()) {
      Preferences.setInteger("lastCellarReset", KoLCharacter.getAscensions());
      Preferences.setInteger("cellarLayout", 0);
    }
  }

  private static int getMcHugeLargeLevel(Modifiers mods) {
    if (KoLCharacter.inNoobcore()) {
      return 0;
    }
    int totalItems = mods.getBitmap(BitmapModifier.MCHUGELARGE);
    var itemLevel =
        switch (totalItems) {
          case 0, 1 -> 0;
          case 2, 3 -> 1;
          case 4 -> 2;
          case 5 -> 3;
          default -> 0;
        };
    return itemLevel * totalItems;
  }

  private static void addMcHugeLargeModifiers(Modifiers mods, int level) {
    mods.addDouble(DoubleModifier.COLD_RESISTANCE, level, ModifierType.OUTFIT, "McHugeLarge");
    mods.addDouble(DoubleModifier.HOT_DAMAGE, 5 * level, ModifierType.OUTFIT, "McHugeLarge");
    mods.addDouble(DoubleModifier.INITIATIVE, 10 * level, ModifierType.OUTFIT, "McHugeLarge");
  }

  public static boolean hasCampground() {
    return switch (KoLCharacter.ascensionPath) {
      case ACTUALLY_ED_THE_UNDYING, YOU_ROBOT, NUCLEAR_AUTUMN, SMALL, WEREPROFESSOR -> false;
      default -> true;
    };
  }
}
