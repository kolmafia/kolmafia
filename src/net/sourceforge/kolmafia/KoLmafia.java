package net.sourceforge.kolmafia;

import static net.sourceforge.kolmafia.KoLGUIConstants.FLATMAP_DARK_LOOKS;
import static net.sourceforge.kolmafia.KoLGUIConstants.FLATMAP_LIGHT_LOOKS;

import java.awt.Color;
import java.awt.Frame;
import java.awt.Taskbar;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.io.RandomAccessFile;
import java.math.BigInteger;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import net.java.dev.spellcast.utilities.ActionPanel;
import net.java.dev.spellcast.utilities.DataUtilities;
import net.java.dev.spellcast.utilities.JComponentUtilities;
import net.sourceforge.kolmafia.AdventureResult.AdventureLongCountResult;
import net.sourceforge.kolmafia.KoLConstants.CraftingType;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.SpecialOutfit.Checkpoint;
import net.sourceforge.kolmafia.listener.NamedListenerRegistry;
import net.sourceforge.kolmafia.listener.PreferenceListenerRegistry;
import net.sourceforge.kolmafia.moods.RecoveryManager;
import net.sourceforge.kolmafia.objectpool.EffectPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.BountyDatabase;
import net.sourceforge.kolmafia.persistence.ConcoctionDatabase;
import net.sourceforge.kolmafia.persistence.EffectDatabase;
import net.sourceforge.kolmafia.persistence.EquipmentDatabase;
import net.sourceforge.kolmafia.persistence.FamiliarDatabase;
import net.sourceforge.kolmafia.persistence.FlaggedItems;
import net.sourceforge.kolmafia.persistence.HolidayDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.persistence.MonsterDatabase;
import net.sourceforge.kolmafia.persistence.QuestDatabase;
import net.sourceforge.kolmafia.persistence.QuestDatabase.Quest;
import net.sourceforge.kolmafia.persistence.TCRSDatabase;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.ApiRequest;
import net.sourceforge.kolmafia.request.BountyHunterHunterRequest;
import net.sourceforge.kolmafia.request.CafeRequest;
import net.sourceforge.kolmafia.request.CampgroundRequest;
import net.sourceforge.kolmafia.request.CargoCultistShortsRequest;
import net.sourceforge.kolmafia.request.CharPaneRequest;
import net.sourceforge.kolmafia.request.CharSheetRequest;
import net.sourceforge.kolmafia.request.ChateauRequest;
import net.sourceforge.kolmafia.request.ClanLoungeRequest;
import net.sourceforge.kolmafia.request.ClanRumpusRequest;
import net.sourceforge.kolmafia.request.ClosetRequest;
import net.sourceforge.kolmafia.request.CreateItemRequest;
import net.sourceforge.kolmafia.request.CustomOutfitRequest;
import net.sourceforge.kolmafia.request.EdBaseRequest;
import net.sourceforge.kolmafia.request.EquipmentRequest;
import net.sourceforge.kolmafia.request.FalloutShelterRequest;
import net.sourceforge.kolmafia.request.FamTeamRequest;
import net.sourceforge.kolmafia.request.FamiliarRequest;
import net.sourceforge.kolmafia.request.FightRequest;
import net.sourceforge.kolmafia.request.FloristRequest;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.request.HermitRequest;
import net.sourceforge.kolmafia.request.InternalChatRequest;
import net.sourceforge.kolmafia.request.MoonPhaseRequest;
import net.sourceforge.kolmafia.request.PeeVPeeRequest;
import net.sourceforge.kolmafia.request.PurchaseRequest;
import net.sourceforge.kolmafia.request.QuantumTerrariumRequest;
import net.sourceforge.kolmafia.request.QuestLogRequest;
import net.sourceforge.kolmafia.request.RelayRequest;
import net.sourceforge.kolmafia.request.RichardRequest;
import net.sourceforge.kolmafia.request.SpelunkyRequest;
import net.sourceforge.kolmafia.request.StandardRequest;
import net.sourceforge.kolmafia.request.StorageRequest;
import net.sourceforge.kolmafia.request.TrendyRequest;
import net.sourceforge.kolmafia.request.UseItemRequest;
import net.sourceforge.kolmafia.request.WildfireCampRequest;
import net.sourceforge.kolmafia.session.BanishManager;
import net.sourceforge.kolmafia.session.BatManager;
import net.sourceforge.kolmafia.session.ChoiceManager;
import net.sourceforge.kolmafia.session.ClanManager;
import net.sourceforge.kolmafia.session.ConsequenceManager;
import net.sourceforge.kolmafia.session.ContactManager;
import net.sourceforge.kolmafia.session.GoalManager;
import net.sourceforge.kolmafia.session.InventoryManager;
import net.sourceforge.kolmafia.session.IslandManager;
import net.sourceforge.kolmafia.session.LightsOutManager;
import net.sourceforge.kolmafia.session.Limitmode;
import net.sourceforge.kolmafia.session.LogoutManager;
import net.sourceforge.kolmafia.session.ResultProcessor;
import net.sourceforge.kolmafia.session.StoreManager;
import net.sourceforge.kolmafia.session.TurnCounter;
import net.sourceforge.kolmafia.session.ValhallaManager;
import net.sourceforge.kolmafia.session.VoteMonsterManager;
import net.sourceforge.kolmafia.swingui.AdventureFrame;
import net.sourceforge.kolmafia.swingui.DescriptionFrame;
import net.sourceforge.kolmafia.swingui.GearChangeFrame;
import net.sourceforge.kolmafia.swingui.GenericFrame;
import net.sourceforge.kolmafia.swingui.SystemTrayFrame;
import net.sourceforge.kolmafia.swingui.listener.LicenseDisplayListener;
import net.sourceforge.kolmafia.swingui.panel.GenericPanel;
import net.sourceforge.kolmafia.textui.AshRuntime;
import net.sourceforge.kolmafia.utilities.LockableListFactory;
import net.sourceforge.kolmafia.utilities.LogStream;
import net.sourceforge.kolmafia.utilities.StringUtilities;
import net.sourceforge.kolmafia.utilities.SwinglessUIUtils;
import net.sourceforge.kolmafia.webui.RelayServer;

public abstract class KoLmafia {
  private static boolean isRefreshing = false;
  private static boolean isAdventuring = false;
  private static volatile String abortAfter = null;
  public static final String NO_MESSAGE = "";
  public static String lastMessage = NO_MESSAGE;

  static {
    System.setProperty("sun.java2d.noddraw", "true");
    System.setProperty("com.apple.mrj.application.apple.menu.about.name", "KoLmafia");
    System.setProperty("com.apple.mrj.application.live-resize", "true");
    System.setProperty("com.apple.mrj.application.growbox.intrudes", "false");
    System.setProperty("java.net.preferIPv4Stack", "true");

    if (SwinglessUIUtils.isSwingAvailable()) {
      JEditorPane.registerEditorKitForContentType("text/html", RequestEditorKit.class.getName());
    }
    System.setProperty("apple.laf.useScreenMenuBar", "true");
  }

  public static String currentIterationString = "";
  public static boolean tookChoice = false;
  public static boolean redoSkippedAdventures = true;

  public static boolean isMakingRequest = false;
  public static MafiaState displayState = MafiaState.ENABLE;
  private static boolean allowDisplayUpdate = true;

  public static final int[] initialStats = new int[3];

  private static FileLock SESSION_HOLDER = null;
  private static FileChannel SESSION_CHANNEL = null;
  private static File SESSION_FILE = null;
  private static boolean SESSION_ENDING = false;

  public static KoLAdventure currentAdventure;
  public static String statDay = "None";

  private static final String PREFERRED_IMAGE_SERVER = "https://d2uyhvukfffg5a.cloudfront.net";
  private static final String PREFERRED_IMAGE_SERVER_PATH = PREFERRED_IMAGE_SERVER + "/";
  public static final Set<String> IMAGE_SERVER_PATHS =
      Set.of(
          PREFERRED_IMAGE_SERVER_PATH,
          "https://s3.amazonaws.com/images.kingdomofloathing.com/",
          "http://images.kingdomofloathing.com/");

  public static String imageServerPrefix() {
    return PREFERRED_IMAGE_SERVER;
  }

  public static String imageServerPath() {
    return PREFERRED_IMAGE_SERVER_PATH;
  }

  private static boolean acquireFileLock(final String suffix) {
    try {
      KoLmafia.SESSION_FILE = new File(KoLConstants.SESSIONS_LOCATION, "active_session." + suffix);

      if (KoLmafia.SESSION_FILE.exists()) {
        KoLmafia.SESSION_CHANNEL = new RandomAccessFile(KoLmafia.SESSION_FILE, "rw").getChannel();
        KoLmafia.SESSION_HOLDER = KoLmafia.SESSION_CHANNEL.tryLock();
        return KoLmafia.SESSION_HOLDER != null;
      }

      PrintStream ostream = LogStream.openStream(KoLmafia.SESSION_FILE, true);
      ostream.println(StaticEntity.getVersion());
      ostream.close();

      KoLmafia.SESSION_CHANNEL = new RandomAccessFile(KoLmafia.SESSION_FILE, "rw").getChannel();
      KoLmafia.SESSION_HOLDER = KoLmafia.SESSION_CHANNEL.lock();
      return true;
    } catch (Exception e) {
      return false;
    }
  }

  /**
   * The main method. Currently, it instantiates a single instance of the <code>KoLmafiaGUI</code>.
   */
  public static final void main(final String[] args) {
    System.out.println();
    System.out.println(StaticEntity.getVersion());
    System.out.println(StaticEntity.getBuildInfo());
    System.out.println();
    System.out.println("Currently Running on " + System.getProperty("os.name"));

    try {
      System.out.println("Local Directory is " + KoLConstants.ROOT_LOCATION.getCanonicalPath());

    } catch (IOException e) {
      System.out.println(e.getMessage() + " while trying to determine local directory.");
    }
    System.out.println("Using Java " + System.getProperty("java.version"));
    System.out.println();
    StaticEntity.setGUIRequired(true);

    for (int i = 0; i < args.length; ++i) {
      if (args[i].equalsIgnoreCase("--HELP") || args[i].equalsIgnoreCase("/?")) {
        System.out.println("An interface for the online adventure game, The Kingdom of Loathing.");
        System.out.println("Please visit https://kolmafia.us for more information.");
        System.out.println();
        System.out.println("KoLmafia [--Help] [--Version] [--CLI] [--GUI] script");
        System.out.println();
        System.out.println("  --Help        Display this message and exits.");
        System.out.println("  --Version     Display the current version and exits.");
        System.out.println("  --CLI         Run KoLmafia as a command line application.");
        System.out.println(
            "  --GUI         Run KoLmafia with a graphical user interface (Default).");
        System.out.println("  script        Specifies a script to call when starting KoLmafia.");

        System.exit(0);
      } else if (args[i].equalsIgnoreCase("--VERSION")) {
        System.exit(0);
      } else if (args[i].equalsIgnoreCase("--CLI")) {
        StaticEntity.setGUIRequired(false);
      } else if (args[i].equalsIgnoreCase("--GUI")) {
        StaticEntity.setGUIRequired(true);
      }
    }

    // All dates are presented as if the day began at rollover.

    TimeZone koltime = TimeZone.getTimeZone("GMT-0330");

    KoLConstants.DAILY_FORMAT.setTimeZone(koltime);

    // Reload your settings and determine all the different users which
    // are present in your save state list.

    Preferences.setBoolean("useDevProxyServer", false);
    Preferences.setBoolean("relayBrowserOnly", false);

    String actualName;
    String[] pastUsers = StaticEntity.getPastUserList();

    for (int i = 0; i < pastUsers.length; ++i) {
      if (pastUsers[i].startsWith("devster")) {
        continue;
      }

      actualName = Preferences.getString(pastUsers[i], "displayName");
      if (actualName.equals("")) {
        actualName = StringUtilities.globalStringReplace(pastUsers[i], "_", " ");
      }

      KoLConstants.saveStateNames.add(actualName);
    }

    // Set a user agent preemptively.  Workaround to allow https support for file_to_map and price
    // updates to coexist.
    GenericRequest.setUserAgent();

    // Clear out any outdated data files.

    KoLmafia.checkDataOverrides();

    // Create an images directory if necessary
    KoLConstants.IMAGE_LOCATION.mkdirs();

    // Create a script directory if necessary
    KoLConstants.SCRIPT_LOCATION.mkdirs();

    // Clear the image cache for the first time so subsequent image
    // files loaded into it have the right timestamps
    if (Preferences.getLong("lastImageCacheClear") == 0L) {
      RelayRequest.clearImageCache();
    }

    if (SwinglessUIUtils.isSwingAvailable()) {
      KoLmafia.initLookAndFeel();
    }
    if (!KoLmafia.acquireFileLock("1") && !KoLmafia.acquireFileLock("2")) {
      System.out.println("Could not acquire file lock");
      System.exit(-1);
    }

    FlaggedItems.initializeLists();

    // Now run the main routines for each, so that
    // you have an interface.

    if (StaticEntity.isGUIRequired()) {
      KoLmafiaGUI.initialize();
    } else {
      KoLmafiaTUI.initialize();
    }

    // Now, maybe the person wishes to run something
    // on startup, and they associated KoLmafia with
    // some non-ASH file extension. This will run it.

    StringBuilder initialScript = new StringBuilder();

    for (int i = 0; i < args.length; ++i) {
      if (args[i].equalsIgnoreCase("--CLI") || args[i].equalsIgnoreCase("--GUI")) {
        continue;
      }
      // Special case to allow dark menu bar on MacOSX via java
      // This could be reworked to handle any JRE directive if needed.
      if (args[i].equalsIgnoreCase("-NSRequiresAquaSystemAppearance")) {
        i++;
        continue;
      }

      initialScript.append(args[i]);
      initialScript.append(" ");
    }

    if (initialScript.length() != 0) {
      String actualScript = initialScript.toString().trim();
      if (actualScript.startsWith("script=")) {
        actualScript = actualScript.substring(7);
      }

      KoLmafiaCLI.DEFAULT_SHELL.executeLine("call " + actualScript);
    } else if (!StaticEntity.isGUIRequired()) {
      KoLmafiaCLI.DEFAULT_SHELL.attemptLogin("");
    }

    // Check for KoLmafia updates in a separate thread
    // so as to allow for continued execution.

    RequestThread.runInParallel(new UpdateCheckRunnable(), false);

    // Always read input from the command line when you're not
    // in GUI mode.

    if (!StaticEntity.isGUIRequired()) {
      KoLmafiaCLI.DEFAULT_SHELL.listenForCommands();
    }
  }

  private static void initLookAndFeel() {
    // Change the default look and feel to match the player's
    // preferences. Always do this.

    String defaultLookAndFeel;

    // Tell UIManager about Look and Feel files in external jars ( defined in KoLGUIConstants)
    FLATMAP_LIGHT_LOOKS.forEach(
        (lookName, lookClass) -> UIManager.installLookAndFeel(lookName, lookClass));
    FLATMAP_DARK_LOOKS.forEach(
        (lookName, lookClass) -> UIManager.installLookAndFeel(lookName, lookClass));

    if (System.getProperty("os.name").startsWith("Mac")
        || System.getProperty("os.name").startsWith("Win")) {
      defaultLookAndFeel = UIManager.getSystemLookAndFeelClassName();
    } else {
      defaultLookAndFeel = UIManager.getCrossPlatformLookAndFeelClassName();
    }

    String lookAndFeel = Preferences.getString("swingLookAndFeel");

    if (lookAndFeel.equals("")) {
      lookAndFeel = defaultLookAndFeel;
    }

    UIManager.LookAndFeelInfo[] installed = UIManager.getInstalledLookAndFeels();
    String[] installedLooks = new String[installed.length];

    for (int i = 0; i < installedLooks.length; ++i) {
      installedLooks[i] = installed[i].getClassName();
    }

    boolean foundLookAndFeel = false;
    for (int i = 0; i < installedLooks.length && !foundLookAndFeel; ++i) {
      foundLookAndFeel = installedLooks[i].equals(lookAndFeel);
    }

    if (!foundLookAndFeel) {
      lookAndFeel = defaultLookAndFeel;
    }

    try {
      UIManager.setLookAndFeel(lookAndFeel);
      JFrame.setDefaultLookAndFeelDecorated(System.getProperty("os.name").startsWith("Mac"));
    } catch (Exception e) {
      // Should not happen, as we checked to see if
      // the look and feel was installed first.

      JFrame.setDefaultLookAndFeelDecorated(true);
    }

    if (StaticEntity.usesSystemTray()) {
      SystemTrayFrame.addTrayIcon();
    }

    if (Taskbar.isTaskbarSupported()) {
      Taskbar taskbar = Taskbar.getTaskbar();
      if (taskbar.isSupported(Taskbar.Feature.ICON_IMAGE)) {
        taskbar.setIconImage(JComponentUtilities.getImage("limeglass.gif").getImage());
      }
    }

    if (System.getProperty("os.name").startsWith("Win")
        || lookAndFeel.equals(UIManager.getCrossPlatformLookAndFeelClassName())) {
      UIManager.put("ProgressBar.foreground", Color.black);
      UIManager.put("ProgressBar.selectionForeground", Color.lightGray);

      UIManager.put("ProgressBar.background", Color.lightGray);
      UIManager.put("ProgressBar.selectionBackground", Color.black);
    }

    tab.CloseTabPaneEnhancedUI.selectedA =
        DataUtilities.toColor(Preferences.getString("innerTabColor"));
    tab.CloseTabPaneEnhancedUI.selectedB =
        DataUtilities.toColor(Preferences.getString("outerTabColor"));

    tab.CloseTabPaneEnhancedUI.notifiedA =
        DataUtilities.toColor(Preferences.getString("innerChatColor"));
    tab.CloseTabPaneEnhancedUI.notifiedB =
        DataUtilities.toColor(Preferences.getString("outerChatColor"));
  }

  private static void checkDataOverrides() {
    String lastVersion = Preferences.getString("previousUpdateVersion");
    String currentVersion = StaticEntity.getVersion();

    int currentRevision = StaticEntity.getRevision();

    String message = null;

    if (lastVersion == null || lastVersion.equals("")) {
      message = "Clearing data overrides: initializing from " + currentVersion;
    } else if (!lastVersion.equals(currentVersion)) {
      message = "Clearing data overrides: upgrade from " + lastVersion + " to " + currentVersion;
    }

    // Save revision, just for fun, but do not clear override files
    // for minor version upgrades.

    Preferences.setString("previousUpdateVersion", currentVersion);
    Preferences.setInteger("previousUpdateRevision", currentRevision);

    if (message == null) {
      return;
    }

    RequestLogger.printLine(message);

    for (int i = 0; i < KoLConstants.OVERRIDE_DATA.length; ++i) {
      File outdated = new File(KoLConstants.DATA_LOCATION, KoLConstants.OVERRIDE_DATA[i]);
      if (outdated.exists()) {
        outdated.delete();
      }
    }
  }

  public static final boolean isSessionEnding() {
    return KoLmafia.SESSION_ENDING;
  }

  public static final String getLastMessage() {
    return KoLmafia.lastMessage;
  }

  /** Updates the currently active display in the <code>KoLmafia</code> session. */
  public static final void updateDisplay(final String message) {
    KoLmafia.updateDisplay(MafiaState.CONTINUE, message);
  }

  /** Updates the currently active display in the <code>KoLmafia</code> session. */
  public static final void updateDisplay(final MafiaState state, final String message) {
    if (StaticEntity.getContinuationState() == MafiaState.ABORT && state != MafiaState.ABORT) {
      return;
    }

    if (StaticEntity.getContinuationState() != MafiaState.PENDING || state == MafiaState.ABORT) {
      StaticEntity.setContinuationState(state);
    }

    RequestLogger.printLine(state, message);

    if (KoLmafia.allowDisplayUpdate) {
      SystemTrayFrame.updateToolTip(message);
    }

    KoLmafia.lastMessage = message;

    if (!message.contains(KoLConstants.LINE_BREAK)) {
      KoLmafia.updateDisplayState(state, message);
    }
  }

  private static void updateDisplayState(final MafiaState state, final String message) {
    // Relay threads don't get to change the display state
    if (StaticEntity.isRelayThread()) return;

    // Update all panels and frames with the message.

    if (KoLmafia.allowDisplayUpdate) {
      String unicodeMessage = StringUtilities.getEntityDecode(message, false);
      ActionPanel[] panels = StaticEntity.getExistingPanels();

      for (int i = 0; i < panels.length; ++i) {
        if (panels[i] instanceof GenericPanel) {
          ((GenericPanel) panels[i]).setStatusMessage(unicodeMessage);
        }

        panels[i].setEnabled(state != MafiaState.CONTINUE);
      }

      Frame[] frames = Frame.getFrames();
      for (int i = 0; i < frames.length; ++i) {
        if (frames[i] instanceof GenericFrame) {
          GenericFrame frame = (GenericFrame) frames[i];

          frame.setStatusMessage(unicodeMessage);
          frame.updateDisplayState(state);
        }
      }

      if (KoLDesktop.instanceExists()) {
        KoLDesktop.getInstance().updateDisplayState(state);
      }
    }

    KoLmafia.displayState = state;
  }

  public static final void enableDisplay() {
    if (StaticEntity.getContinuationState() == MafiaState.ABORT
        || StaticEntity.getContinuationState() == MafiaState.ERROR) {
      KoLmafia.updateDisplayState(MafiaState.ERROR, "");
    } else {
      KoLmafia.updateDisplayState(MafiaState.ENABLE, "");
    }

    StaticEntity.setContinuationState(MafiaState.CONTINUE);
  }

  public static final void resetCounters() {
    Preferences.setLong("lastCounterDay", KoLCharacter.getRollover());

    // Reset kolhsTotalSchoolSpirited to 0 if _kolhsSchoolSpirited wasn't set yesterday
    if (!Preferences.getBoolean("_kolhsSchoolSpirited")) {
      Preferences.setInteger("kolhsTotalSchoolSpirited", 0);
    }

    // Unused cat burglar heists carry over
    int charge = Preferences.getInteger("_catBurglarCharge");
    int minChargeCost = 10;
    int totalHeists = 0;
    while (charge >= minChargeCost) {
      totalHeists++;
      charge -= minChargeCost;
      minChargeCost *= 2;
    }
    int heistsComplete = Preferences.getInteger("_catBurglarHeistsComplete");
    int bankHeists = Preferences.getInteger("catBurglarBankHeists");

    Preferences.setInteger("catBurglarBankHeists", bankHeists + totalHeists - heistsComplete);

    Preferences.resetDailies();
    VYKEACompanionData.initialize(false);
    ConsequenceManager.updateOneDesc();

    // Make sure Banishes are loaded before removing them
    BanishManager.loadBanishedMonsters();
    BanishManager.resetRollover();

    // Libram summoning skills now costs 1 MP again
    LockableListFactory.sort(KoLConstants.summoningSkills);
    LockableListFactory.sort(KoLConstants.usableSkills);

    // Remove Wandering Monster counters
    TurnCounter.stopCounting("Romantic Monster window begin");
    TurnCounter.stopCounting("Romantic Monster window end");
    TurnCounter.stopCounting("Digitize Monster");
    TurnCounter.stopCounting("Holiday Monster window begin");
    TurnCounter.stopCounting("Holiday Monster window end");
    TurnCounter.stopCounting("Event Monster window begin");
    TurnCounter.stopCounting("Event Monster window end");
    TurnCounter.stopCounting("Taco Elf window begin");
    TurnCounter.stopCounting("Taco Elf window end");
    TurnCounter.stopCounting("Latte Monster");

    // Adjust Mmmmmmayonnaise counters for next run
    for (TurnCounter counter : TurnCounter.getCounters()) {
      if (counter.getLabel().startsWith("Mmmmmmayonnaise window ")) {
        counter.resetForRun();
      }
    }
  }

  public static void refreshSession() {
    KoLmafia.setIsRefreshing(true);

    // Start out fetching the status using the KoL API. This
    // provides data from a lot of different standard pages

    // We are in Valhalla if this redirects to afterlife.php
    String redirection = ApiRequest.updateStatus();
    if (redirection != null && redirection.startsWith("afterlife.php")) {
      // In Valhalla, ApiRequest parsed the charpane for us.
      KoLmafia.updateDisplay("Welcome to Valhalla!");
      KoLmafia.setIsRefreshing(false);
      return;
    }

    // If api.php did not redirect, we've loaded a lot of data,
    // including ascension status

    // Load saved counters before any requests are made, since both
    // charpane and charsheet requests can set them.

    CharPaneRequest.reset();
    KoLCharacter.setCurrentRun(0);
    TurnCounter.loadCounters();

    boolean shouldResetCounters = false;
    boolean shouldResetGlobalCounters = false;
    // Assume if rollover has changed by an hour, it is a new rollover. Time varies slightly between
    // servers by a few seconds.
    shouldResetCounters = KoLCharacter.getRollover() - Preferences.getLong("lastCounterDay") > 3600;
    shouldResetGlobalCounters =
        KoLCharacter.getRollover() - Preferences.getLong("lastGlobalCounterDay") > 3600;

    int ascensions = KoLCharacter.getAscensions();
    int knownAscensions = Preferences.getInteger("knownAscensions");

    if (ascensions != 0 && knownAscensions != -1 && knownAscensions != ascensions) {
      Preferences.setInteger("knownAscensions", ascensions);
      ValhallaManager.resetPerAscensionCounters();
      shouldResetCounters = true;
      KoLCharacter.setGuildStoreOpen(false);
    } else if (knownAscensions == -1) {
      Preferences.setInteger("knownAscensions", ascensions);
    }

    if (shouldResetCounters) {
      KoLmafia.resetCounters();
    }

    if (shouldResetGlobalCounters) {
      Preferences.resetGlobalDailies();
    }

    // No spurious adventure logging
    KoLAdventure.locationLogged = true;

    KoLmafia.refreshSessionData();

    AdventureFrame.updateFromPreferences();

    // It would be nice to not have to do this
    IslandManager.ensureUpdatedBigIsland();

    KoLmafia.setIsRefreshing(false);
  }

  private static void refreshSessionData() {
    KoLmafia.updateDisplay("Refreshing session data...");

    // Some things aren't properly set by KoL until main.php is loaded

    KoLmafia.makeMainRequest();

    // Get current moon phases

    RequestThread.postRequest(new MoonPhaseRequest());
    KoLCharacter.setHoliday(HolidayDatabase.getHoliday());

    // Forget what is trendy
    TrendyRequest.reset();

    // Initialize pasta thralls & Ed servants, regardless of
    // character class
    PastaThrallData.initialize();
    EdServantData.initialize();

    // Initialize pulverization data from original item enchantments
    EquipmentDatabase.initializePulverization();

    // Reset monsters that depend on player name. Do this before we
    // look at the char sheet; we'll bail early if we are in a
    // fight - and we want to recognize aliased monsters
    MonsterDatabase.saveAliases();

    // Retrieve the character sheet. It's necessary to do this
    // before concoctions have a chance to get refreshed.

    GenericRequest request = new CharSheetRequest();
    RequestThread.postRequest(request);

    // If you get redirected on the request for the character sheet,
    // don't make any more requests.

    if (request.redirectLocation != null) {
      return;
    }

    // Now that we know the character's ascension count, reset
    // anything that depends on that.

    KoLCharacter.resetPerAscensionData();

    // If we in Two Crazy Random Summer, this is a good time to
    // load all the modified item data. Reset to KoL defaults first.
    TCRSDatabase.resetModifiers();
    TCRSDatabase.loadTCRSData();

    // Hermit items depend on character class
    HermitRequest.initialize();

    // Retrieve the contents of inventory.
    InventoryManager.refresh();

    // Retrieve the contents of the closet.
    ClosetRequest.refresh();

    // Load Banished monsters
    BanishManager.loadBanishedMonsters();

    // Retrieve Custom Outfit list
    if (!Limitmode.limitOutfits()) {
      RequestThread.postRequest(new CustomOutfitRequest());
    }

    // Look at the Quest Log
    RequestThread.postRequest(new QuestLogRequest());

    // if the Cyrpt quest is active, force evilometer refresh
    // (if we don't know evil levels already)
    if (QuestDatabase.isQuestStep(Quest.CYRPT, QuestDatabase.STARTED)) {
      if (Preferences.getInteger("cyrptTotalEvilness") == 0) {
        RequestThread.postRequest(UseItemRequest.getInstance(ItemPool.EVILOMETER));
      }
    }

    // Path-related stuff
    if (KoLCharacter.isEd()) {
      // Inspect your servants
      RequestThread.postRequest(new EdBaseRequest("edbase_door", true));
    } else if (KoLCharacter.inPokefam()) {
      RequestThread.postRequest(new FamTeamRequest());
    } else if (KoLCharacter.inQuantum()) {
      RequestThread.postRequest(new QuantumTerrariumRequest());
    } else if (KoLCharacter.isPlumber()) {
      KoLCharacter.resetCurrentPP();
    } else if (KoLCharacter.inRobocore()) {
      RequestThread.postRequest(
          new GenericRequest("place.php?whichplace=scrapheap&action=sh_configure"));
      RequestThread.postRequest(new GenericRequest("choice.php?whichchoice=1445&show=cpus"));
    }

    // Refresh fire levels
    WildfireCampRequest.refresh();

    if (!(KoLCharacter.inAxecore()
        || KoLCharacter.isJarlsberg()
        || KoLCharacter.isSneakyPete()
        || KoLCharacter.inBondcore()
        || KoLCharacter.isVampyre()
        || KoLCharacter.isEd()
        || KoLCharacter.inPokefam()
        || KoLCharacter.inQuantum())) {
      // Retrieve the Terrarium
      RequestThread.postRequest(new FamiliarRequest());
    }

    ChateauRequest.refresh();

    // Retrieve campground data to see if the user has box servants
    // or a bookshelf

    if (!Limitmode.limitCampground()
        && !KoLCharacter.isEd()
        && !KoLCharacter.inNuclearAutumn()
        && !KoLCharacter.inRobocore()) {
      KoLmafia.updateDisplay("Retrieving campground data...");
      CampgroundRequest.reset();
      if (!KoLCharacter.isVampyre()) {
        RequestThread.postRequest(new CampgroundRequest("inspectdwelling"));
      }
      RequestThread.postRequest(new CampgroundRequest("inspectkitchen"));
      RequestThread.postRequest(new CampgroundRequest("workshed"));
      KoLCharacter.checkTelescope();
    }

    // Retrieve current Cafe menus if we haven't done so today
    // These affect available concoctions
    ConcoctionDatabase.retrieveCafeMenus();

    if (!Limitmode.limitCampground() && KoLCharacter.inNuclearAutumn()) {
      KoLmafia.updateDisplay("Retrieving fallout shelter data...");
      FalloutShelterRequest.reset();
      RequestThread.postRequest(new FalloutShelterRequest());
    }

    RequestThread.postRequest(new PeeVPeeRequest("fight"));

    if (Preferences.getInteger("lastEmptiedStorage") != KoLCharacter.getAscensions()) {
      StorageRequest.refresh();
      CafeRequest.pullLARPCard();
    }

    if (KoLConstants.inventory.contains(ItemPool.get(ItemPool.KEYOTRON, 1))
        && Preferences.getInteger("lastKeyotronUse") != KoLCharacter.getAscensions()) {
      RequestThread.postRequest(UseItemRequest.getInstance(ItemPool.KEYOTRON));
    }

    // If we have a Crown of Thrones and/or Buddy Bjorn available and it's not
    // equipped, see which familiar is sitting in it, if any.
    InventoryManager.checkCrownOfThrones();
    InventoryManager.checkBuddyBjorn();

    // Items that need to be checked every time
    InventoryManager.checkKGB();
    InventoryManager.checkBirdOfTheDay();
    CargoCultistShortsRequest.loadPockets();

    // Check items that vary per person
    // These won't actually generate a server hit if the item
    // has been seen at its current modifiers
    InventoryManager.checkNoHat();
    InventoryManager.checkJickSword();
    InventoryManager.checkPantogram();
    InventoryManager.checkLatte();
    InventoryManager.checkSaber();
    InventoryManager.checkCoatOfPaint();

    // Items that conditionally grant skills
    InventoryManager.checkPowerfulGlove();

    // Check Horsery if we haven't today
    if (Preferences.getBoolean("horseryAvailable")
        && Preferences.getString("_horseryCrazyMox").length() == 0) {
      RequestThread.postRequest(
          new GenericRequest("place.php?whichplace=town_right&action=town_horsery"));
    }

    // Refresh familiar stuff
    FamiliarData.reset();

    // Make sure that we know about the easy to see Golden Mr. A's, at least
    InventoryManager.countGoldenMrAccesories();

    // Look up the current clan
    ClanManager.resetClanId();
    ClanManager.getClanName(true);

    // Update your mail contacts
    ContactManager.clearMailContacts();
    ContactManager.updateMailContacts();

    // Get current list of restricted items
    StandardRequest.initialize(true);

    KoLmafia.updateDisplay("Session data refreshed.");

    // Inventory may have changed
    NamedListenerRegistry.fireChange("(coinmaster)");

    ConcoctionDatabase.refreshConcoctions();

    // Check the Florist to see what is planted
    FloristRequest.reset();
    RequestThread.postRequest(new FloristRequest());

    // Check some things that are not (yet) in api.php
    EquipmentRequest.checkCowboyBoots();
    EquipmentRequest.checkHolster();

    // Ensure turn based counters are active
    LightsOutManager.checkCounter();
    VoteMonsterManager.checkCounter();
  }

  public static final void makeMainRequest() {
    GenericRequest mainRequest = new GenericRequest("main.php");
    RequestThread.postRequest(mainRequest);
    String response = mainRequest.responseText;
    // Your potato alarm clock has been going off for 5 minutes now!
    if (response != null && response.contains("Your potato alarm clock")) {
      String message = "Your potato alarm clock gave you 5 extra adventures";
      KoLmafia.updateDisplay(message);
      RequestLogger.updateSessionLog(message);
      Preferences.setBoolean("_potatoAlarmClockUsed", true);
    }
  }

  public static final boolean isRefreshing() {
    return KoLmafia.isRefreshing;
  }

  private static void setIsRefreshing(final boolean isRefreshing) {
    if (KoLmafia.isRefreshing != isRefreshing) {
      KoLmafia.isRefreshing = isRefreshing;
      PreferenceListenerRegistry.deferPreferenceListeners(isRefreshing);
    }
  }

  public static final void resetAfterAvatar() {
    KoLmafia.setIsRefreshing(true);

    // Set this first to prevent duplicate skill refreshing
    KoLCharacter.setRestricted(false);

    // Start out fetching the status using the KoL API. This
    // provides data from a lot of different standard pages

    ApiRequest.updateStatus();

    // Retrieve the character sheet. We must do this before
    // concoctions have a chance to get refreshed.

    // Clear skills first, since we no longer know Avatar skills
    KoLCharacter.resetSkills();
    RequestThread.postRequest(new CharSheetRequest());
    InventoryManager.checkPowerfulGlove();

    // Clear preferences
    Preferences.setString("banishingShoutMonsters", "");
    Preferences.setString("peteMotorbikeTires", "");
    Preferences.setString("peteMotorbikeGasTank", "");
    Preferences.setString("peteMotorbikeHeadlight", "");
    Preferences.setString("peteMotorbikeCowling", "");
    Preferences.setString("peteMotorbikeMuffler", "");
    Preferences.setString("peteMotorbikeSeat", "");
    BanishManager.resetAvatar();

    // Hermit items depend on character class
    HermitRequest.initialize();

    // Retrieve inventory contents, since quest items may disappear.
    InventoryManager.refresh();

    // Retrieve the Terrarium
    RequestThread.postRequest(new FamiliarRequest());
    GearChangeFrame.updateFamiliars();

    // Available stuff in Clan may have changed, so check clan
    ClanLoungeRequest.updateLounge();

    // Check the campground
    CampgroundRequest.reset();
    RequestThread.postRequest(new CampgroundRequest("inspectdwelling"));
    RequestThread.postRequest(new CampgroundRequest("inspectkitchen"));
    RequestThread.postRequest(new CampgroundRequest("workshed"));
    RequestThread.postRequest(new CampgroundRequest("bookshelf"));
    KoLCharacter.checkTelescope();

    // Finally, update available concoctions
    ConcoctionDatabase.resetQueue();
    ConcoctionDatabase.refreshConcoctions();

    KoLmafia.setIsRefreshing(false);

    // Check the Florist
    FloristRequest.reset();
    RequestThread.postRequest(new FloristRequest());

    // Run a user-supplied script
    KoLmafiaCLI.DEFAULT_SHELL.executeLine(Preferences.getString("kingLiberatedScript"));
  }

  public static final void resetAfterLimitmode() {
    KoLmafia.setIsRefreshing(true);

    // Clear Spelunky preferences & items
    SpelunkyRequest.reset();

    // Clear Batfellow preferences & items
    BatManager.end();

    // Set this first to prevent duplicate skill refreshing
    KoLCharacter.setRestricted(false);

    // Start out fetching the status using the KoL API. This
    // provides data from a lot of different standard pages
    ApiRequest.updateStatus();

    // Retrieve the character sheet. It's necessary to do this
    // before concoctions have a chance to get refreshed.

    // Clear skills first, since we no longer know Limitmode skills
    KoLCharacter.resetSkills();
    RequestThread.postRequest(new CharSheetRequest());
    InventoryManager.checkPowerfulGlove();

    // Retrieve inventory contents, since quest items may disappear.
    InventoryManager.refresh();

    // Retrieve Custom Outfit list, since outfits contain limited items
    RequestThread.postRequest(new CustomOutfitRequest());

    // Retrieve the Terrarium
    RequestThread.postRequest(new FamiliarRequest());

    // If we logged in during limitmode, we may not have seen the Campground
    if (!KoLCharacter.isEd()) {
      CampgroundRequest.reset();
      if (!KoLCharacter.isVampyre()) {
        RequestThread.postRequest(new CampgroundRequest("inspectdwelling"));
      }
      RequestThread.postRequest(new CampgroundRequest("inspectkitchen"));
      RequestThread.postRequest(new CampgroundRequest("workshed"));
      RequestThread.postRequest(new CampgroundRequest("bookshelf"));
      KoLCharacter.checkTelescope();
    }

    // Finally, update available concoctions
    ConcoctionDatabase.resetQueue();
    ConcoctionDatabase.refreshConcoctions();

    KoLmafia.setIsRefreshing(false);

    // Ensure Gear Changer accurate
    GearChangeFrame.validateSelections();
  }

  /** Used to reset the session tally to its original values. */
  public static void resetSession() {
    KoLConstants.encounterList.clear();
    KoLConstants.adventureList.clear();

    KoLmafia.initialStats[0] = KoLCharacter.calculateBasePoints(KoLCharacter.getTotalMuscle());
    KoLmafia.initialStats[1] = KoLCharacter.calculateBasePoints(KoLCharacter.getTotalMysticality());
    KoLmafia.initialStats[2] = KoLCharacter.calculateBasePoints(KoLCharacter.getTotalMoxie());

    AdventureResult.SESSION_FULLSTATS[0] = 0;
    AdventureResult.SESSION_FULLSTATS[1] = 0;
    AdventureResult.SESSION_FULLSTATS[2] = 0;

    AdventureResult.SESSION_SUBSTATS[0] = 0;
    AdventureResult.SESSION_SUBSTATS[1] = 0;
    AdventureResult.SESSION_SUBSTATS[2] = 0;

    KoLConstants.tally.clear();
    KoLConstants.tally.add(new AdventureResult(AdventureResult.ADV));
    KoLConstants.tally.add(new AdventureLongCountResult(AdventureResult.MEAT));
    KoLConstants.tally.add(AdventureResult.SESSION_SUBSTATS_RESULT);
    KoLConstants.tally.add(AdventureResult.SESSION_FULLSTATS_RESULT);

    // We could clear this here. However, it's useful for ASH
    // scripts to know this value regardless of whether the user
    // cleared the tally via the menu.
    //
    // KoLCharacter.clearSessionMeat();
  }

  public static final void saveDataOverride() {
    if (ItemDatabase.newItems) {
      ItemDatabase.writeItems(new File(KoLConstants.DATA_LOCATION, "items.txt"));
    }

    if (EquipmentDatabase.newEquipment) {
      EquipmentDatabase.writeEquipment(new File(KoLConstants.DATA_LOCATION, "equipment.txt"));
    }

    if (EffectDatabase.newEffects) {
      EffectDatabase.writeEffects(new File(KoLConstants.DATA_LOCATION, "statuseffects.txt"));
    }

    if (ItemDatabase.newItems || EquipmentDatabase.newEquipment || EffectDatabase.newEffects) {
      Modifiers.writeModifiers(new File(KoLConstants.DATA_LOCATION, "modifiers.txt"));
    }

    if (FamiliarDatabase.newFamiliars) {
      FamiliarDatabase.writeFamiliars(new File(KoLConstants.DATA_LOCATION, "familiars.txt"));
    }
  }

  /**
   * Adds the recent effects accumulated so far to the actual effects. This should be called after
   * the previous effects were decremented, if adventuring took place.
   */
  public static final void applyEffects() {
    boolean concoctionRefreshNeeded = false;
    boolean updatePPNeeded = false;

    int oldCount = KoLConstants.activeEffects.size();

    for (int j = 0; j < KoLConstants.recentEffects.size(); ++j) {
      AdventureResult effect = KoLConstants.recentEffects.get(j);
      AdventureResult.addResultToList(KoLConstants.activeEffects, effect);

      int effectId = effect.getEffectId();
      if (effectId == EffectPool.INIGOS || effectId == EffectPool.CRAFT_TEA) {
        concoctionRefreshNeeded = true;
      } else if (effectId == EffectPool.FIZZY_FIZZY) {
        updatePPNeeded = true;
      } else if (effectId == EffectPool.COWRRUPTION) {
        if (KoLConstants.activeEffects.contains(effect)
            && KoLCharacter.getAscensionClass() == AscensionClass.COWPUNCHER) {
          KoLCharacter.addAvailableSkill("Absorb Cowrruption");
        } else {
          KoLCharacter.removeAvailableSkill("Absorb Cowrruption");
        }
      }
    }

    KoLConstants.recentEffects.clear();
    LockableListFactory.sort(KoLConstants.activeEffects);

    if (oldCount != KoLConstants.activeEffects.size()) {
      KoLCharacter.updateStatus();
    }

    if (updatePPNeeded) {
      // Gaining or losing this effect will add or subtract 1 PP
      KoLCharacter.recalculateAdjustments();
      KoLCharacter.resetCurrentPP();
    }

    if (concoctionRefreshNeeded) {
      ConcoctionDatabase.setRefreshNeeded(true);
    }
  }

  /**
   * Makes the given request for the given number of iterations, or until continues are no longer
   * possible, either through user cancellation or something occuring which prevents the requests
   * from resuming.
   *
   * @param request The request made by the user
   * @param iterations The number of times the request should be repeated
   */
  public static void makeRequest(final Runnable request, final int iterations) {
    // This will only be true if this method is recursively
    // called via a script: an afterAdventureScript calling
    // "adventure", for example

    boolean wasAdventuring = KoLmafia.isAdventuring;

    Checkpoint checkpoint = null;
    try {
      if (request instanceof KoLAdventure) {
        KoLmafia.currentAdventure = (KoLAdventure) request;

        if (KoLmafia.currentAdventure.getRequest() instanceof ClanRumpusRequest) {
          RequestThread.postRequest(
              ((ClanRumpusRequest) KoLmafia.currentAdventure.getRequest())
                  .setTurnCount(iterations));
          return;
        }

        if (KoLmafia.currentAdventure.getRequest() instanceof RichardRequest) {
          RequestThread.postRequest(
              ((RichardRequest) KoLmafia.currentAdventure.getRequest()).setTurnCount(iterations));
          return;
        }

        if (KoLCharacter.getCurrentHP() == 0 && !KoLmafia.currentAdventure.isNonCombatsOnly()) {
          RecoveryManager.recoverHP();
        }

        if (!KoLmafia.permitsContinue()) {
          return;
        }

        if (!wasAdventuring) {
          KoLmafia.isAdventuring = true;
          NamedListenerRegistry.fireChange("(adventuring)");
          checkpoint = new Checkpoint();
        }
      }

      KoLmafia.executeRequest(request, iterations, wasAdventuring);
    } catch (Exception e) {
      // This should not happen. Therefore, print
      // a stack trace for debug purposes.

      StaticEntity.printStackTrace(e);
    } finally {
      if (request instanceof KoLAdventure && !wasAdventuring) {
        KoLmafia.isAdventuring = false;
        NamedListenerRegistry.fireChange("(adventuring)");
        if (RecoveryManager.isRecoveryPossible()) {
          RecoveryManager.recoverHP();
        }
        if (checkpoint != null) {
          checkpoint.close();
        }
      }
    }
  }

  private static void executeRequest(
      final Runnable request, final int totalIterations, final boolean wasAdventuring) {
    AshRuntime.forgetPendingState();

    // Begin the adventuring process, or the request execution
    // process (whichever is applicable).

    boolean isAdventure = request instanceof KoLAdventure;

    List<AdventureResult> goals = GoalManager.getGoals();

    boolean deferConcoctionRefresh = true;

    AdventureResult[] items = new AdventureResult[goals.size()];
    CreateItemRequest[] creatables = new CreateItemRequest[goals.size()];

    for (int i = 0; i < goals.size(); ++i) {
      AdventureResult goal = goals.get(i);
      items[i] = goal;
      creatables[i] = CreateItemRequest.getInstance(goal);

      if (deferConcoctionRefresh
          && ConcoctionDatabase.getMixingMethod(goal) != CraftingType.NOCREATE) {
        deferConcoctionRefresh = false;
      }
    }

    KoLmafia.forceContinue();
    KoLmafia.abortAfter = null;

    if (deferConcoctionRefresh) {
      ConcoctionDatabase.deferRefresh(true);
    }

    int currentIteration = 0;

    while (KoLmafia.permitsContinue() && ++currentIteration <= totalIterations) {
      int runBeforeRequest = KoLCharacter.getCurrentRun();
      KoLmafia.tookChoice = false;

      KoLmafia.executeRequestOnce(
          request, currentIteration, totalIterations, items, creatables, wasAdventuring);

      // If updates are suppressed, turn counter doesn't change, so we get stuck in an infinite loop
      // Avoid an API update in that case.
      if (GenericRequest.updateSuppressed()) {
        ApiRequest.updateStatus(true);
      }

      if (isAdventure
          && KoLmafia.redoSkippedAdventures
          && runBeforeRequest == KoLCharacter.getCurrentRun()) {
        --currentIteration;
      }

      // Check if bounties completed, and hand in if so
      boolean completeBounty = false;
      completeBounty |= BountyDatabase.checkBounty("currentEasyBountyItem");
      completeBounty |= BountyDatabase.checkBounty("currentHardBountyItem");
      completeBounty |= BountyDatabase.checkBounty("currentSpecialBountyItem");

      if (completeBounty) {
        RequestThread.postRequest(new BountyHunterHunterRequest());
      }
    }

    if (deferConcoctionRefresh) {
      ConcoctionDatabase.deferRefresh(false);
    }

    if (isAdventure) {
      AdventureFrame.updateRequestMeter(1, 1);
    }

    // If you've completed the requests, make sure to update
    // the display.

    if (KoLmafia.permitsContinue() && RecoveryManager.isRecoveryPossible()) {
      if (isAdventure && GoalManager.hasGoals()) {
        KoLmafia.updateDisplay(
            MafiaState.ERROR,
            "Conditions not satisfied after "
                + (currentIteration - 1)
                + (currentIteration == 2 ? " adventure." : " adventures."));
      }
    } else if (StaticEntity.getContinuationState() == MafiaState.PENDING) {
      AshRuntime.rememberPendingState();
      KoLmafia.forceContinue();
    }
  }

  private static void executeRequestOnce(
      final Runnable request,
      final int currentIteration,
      final int totalIterations,
      final AdventureResult[] items,
      final CreateItemRequest[] creatables,
      final boolean wasAdventuring) {
    if (request instanceof KoLAdventure) {
      KoLmafia.executeAdventureOnce(
          (KoLAdventure) request,
          currentIteration,
          totalIterations,
          items,
          creatables,
          wasAdventuring);
      return;
    }

    if (request instanceof CampgroundRequest) {
      KoLmafia.updateDisplay(
          "Campground request " + currentIteration + " of " + totalIterations + " in progress...");
    }

    RequestLogger.printLine();
    RequestThread.postRequest((GenericRequest) request);
    RequestLogger.printLine();
  }

  private static void executeAdventureOnce(
      final KoLAdventure adventure,
      final int currentIteration,
      final int totalIterations,
      final AdventureResult[] items,
      final CreateItemRequest[] creatables,
      final boolean wasAdventuring) {
    if (KoLCharacter.getAdventuresLeft() == 0) {
      KoLmafia.updateDisplay(MafiaState.PENDING, "Ran out of adventures.");
      return;
    }

    if (KoLmafia.handleConditions(items, creatables)) {
      KoLmafia.updateDisplay(
          MafiaState.PENDING, "Conditions satisfied after " + currentIteration + " adventures.");
      return;
    }

    if (KoLCharacter.isFallingDown()) {
      String holiday = HolidayDatabase.getHoliday();
      String adventureName = adventure.getAdventureName();

      if (KoLCharacter.hasEquipped(ItemPool.get(ItemPool.DRUNKULA_WINEGLASS, 1))) {
        // The wine glass allows you to adventure while falling down drunk
      } else if (KoLCharacter.getLimitmode() == Limitmode.SPELUNKY) {
        // You're allowed to Spelunk even while falling down drunk
      } else if (KoLCharacter.getLimitmode() == Limitmode.BATMAN) {
        // You're allowed to Batfellow even while falling down drunk
      } else if (adventureName.equals("An Eldritch Fissure")
          || adventureName.equals("An Eldritch Horror")
          || adventureName.equals("Trick-or-Treating")
          || adventureName.equals("The Tunnel of L.O.V.E.")) {
        // There are a few adventures you can do even while falling
        // down drunk
      } else if (!holiday.contains("St. Sneaky Pete's Day") && !holiday.contains("Drunksgiving")) {
        KoLmafia.updateDisplay(MafiaState.ERROR, "You are too drunk to continue.");
        return;
      } else if (KoLCharacter.getInebriety() <= 25) {
        KoLmafia.updateDisplay(MafiaState.ERROR, "You are not drunk enough to continue.");
        return;
      }
    }

    if (KoLmafia.abortAfter != null) {
      KoLmafia.updateDisplay(MafiaState.PENDING, KoLmafia.abortAfter);
      return;
    }

    // Otherwise, disable the display and update the user
    // and the current request number. Different requests
    // have different displays. They are handled here.

    if (totalIterations > 1) {
      KoLmafia.currentIterationString =
          "Request "
              + currentIteration
              + " of "
              + totalIterations
              + " ("
              + adventure.toString()
              + ") in progress...";
    } else {
      KoLmafia.currentIterationString = "Visit to " + adventure.toString() + " in progress...";
    }

    if (!wasAdventuring) {
      AdventureFrame.updateRequestMeter(currentIteration - 1, totalIterations);
    }

    RequestLogger.printLine();
    RequestThread.postRequest(adventure);
    while (!KoLmafia.refusesContinue()) {
      if (FightRequest.inMultiFight || FightRequest.fightFollowsChoice) {
        RequestThread.postRequest(FightRequest.INSTANCE);
        continue;
      }
      if (FightRequest.choiceFollowsFight) {
        RequestThread.postRequest(new GenericRequest("choice.php"));
        // Fall through
      }
      if (ChoiceManager.handlingChoice) {
        ChoiceManager.gotoGoal();
        continue;
      }
      break;
    }
    RequestLogger.printLine();

    KoLmafia.currentIterationString = "";

    KoLmafia.executeAfterAdventureScript();

    if (KoLmafia.handleConditions(items, creatables)) {
      KoLmafia.updateDisplay(
          MafiaState.PENDING, "Conditions satisfied after " + currentIteration + " adventures.");
      return;
    }
  }

  public static boolean executeAfterAdventureScript() {
    return KoLmafia.executeScript(Preferences.getString("afterAdventureScript"));
  }

  public static boolean executeBeforePVPScript() {
    return KoLmafia.executeScript(Preferences.getString("beforePVPScript"));
  }

  public static boolean executeScript(final String scriptPath) {
    if (!scriptPath.equals("")) {
      KoLmafiaCLI.DEFAULT_SHELL.executeLine(scriptPath);
      return true;
    }
    return false;
  }

  private static boolean handleConditions(
      final AdventureResult[] items, final CreateItemRequest[] creatables) {
    if (items.length == 0) {
      return false;
    }

    if (!GoalManager.hasGoals()) {
      return true;
    }

    boolean shouldCreate = false;

    for (int i = 0; i < creatables.length && !shouldCreate; ++i) {
      shouldCreate =
          creatables[i] != null && creatables[i].getQuantityPossible() >= items[i].getCount();
    }

    // In theory, you could do a real validation by doing a full
    // dependency search. While that's technically better, it's
    // also not very useful.

    for (int i = 0; i < creatables.length && shouldCreate; ++i) {
      shouldCreate =
          creatables[i] == null || creatables[i].getQuantityPossible() >= items[i].getCount();
    }

    // Create any items which are creatable.

    if (shouldCreate) {
      for (int i = creatables.length - 1; i >= 0; --i) {
        if (creatables[i] != null && creatables[i].getQuantityPossible() >= items[i].getCount()) {
          creatables[i].setQuantityNeeded(items[i].getCount());
          // Don't autocreate items here as well as in ResultProcessor
          switch (creatables[i].getItemId()) {
            case ItemPool.REASSEMBLED_BLACKBIRD:
            case ItemPool.RECONSTITUTED_CROW:
            case ItemPool.BATSKIN_BELT:
            case ItemPool.BADASS_BELT:
            case ItemPool.BONERDAGON_NECKLACE:
            case ItemPool.TALISMAN:
            case ItemPool.MCCLUSKY_FILE:
              if (!Preferences.getBoolean("autoCraft")) {
                RequestThread.postRequest(creatables[i]);
              }
              break;
            default:
              RequestThread.postRequest(creatables[i]);
              break;
          }
          creatables[i] = null;
        }
      }
    }

    // If the conditions existed and have been satisfied,
    // then you should stop.

    return !GoalManager.hasGoals();
  }

  public static void abortAfter(String msg) {
    KoLmafia.abortAfter = msg;
  }

  public static void protectClovers() {
    // If we are in a multifight or a choice follows a fight, defer
    // this until we are free of those
    if (GenericRequest.abortIfInFightOrChoice(true)) {
      // That didn't actually abort.
      ResultProcessor.deferClover();
      return;
    }

    ResultProcessor.undeferClover();

    if (KoLCharacter.inBeecore() || KoLCharacter.inGLover()) {
      KoLmafiaCLI.DEFAULT_SHELL.executeCommand("closet", "put * ten-leaf clover");
    } else {
      KoLmafiaCLI.DEFAULT_SHELL.executeCommand("use", "* ten-leaf clover");
    }
  }

  /** Show an HTML string to the user */
  public static void showHTML(String location, String text) {
    if (!GenericFrame.instanceExists()) {
      KoLmafiaCLI.showHTML(text);
      return;
    }

    GenericRequest request = new GenericRequest(location);
    request.responseText = text;
    DescriptionFrame.showRequest(request);
  }

  /**
   * Retrieves whether or not continuation of an adventure or request is permitted by the or by
   * current circumstances in-game.
   *
   * @return <code>true</code> if requests are allowed to continue
   */
  public static final boolean permitsContinue() {
    return StaticEntity.getContinuationState() == MafiaState.CONTINUE;
  }

  /**
   * Retrieves whether or not continuation of an adventure or request will be denied by the
   * regardless of continue state reset, until the display is enable (ie: in an abort state).
   *
   * @return <code>true</code> if requests are allowed to continue
   */
  public static final boolean refusesContinue() {
    return StaticEntity.userAborted || StaticEntity.getContinuationState() == MafiaState.ABORT;
  }

  /**
   * Forces a continue state. This should only be called when there is no doubt that a continue
   * should occur.
   *
   * @return <code>true</code> if requests are allowed to continue
   */
  public static final void forceContinue() {
    StaticEntity.setContinuationState(MafiaState.CONTINUE);
    StaticEntity.userAborted = false;
  }

  /**
   * Utility. This method used to decode a saved password. This should be called whenever a new
   * password intends to be stored in the global file.
   */
  public static final void addSaveState(final String username, final String password) {
    String utfString = StringUtilities.getURLEncode(password);

    StringBuilder encodedString = new StringBuilder();
    char currentCharacter;
    for (int i = 0; i < utfString.length(); ++i) {
      currentCharacter = utfString.charAt(i);
      switch (currentCharacter) {
        case '-':
          encodedString.append("2D");
          break;
        case '.':
          encodedString.append("2E");
          break;
        case '*':
          encodedString.append("2A");
          break;
        case '_':
          encodedString.append("5F");
          break;
        case '+':
          encodedString.append("20");
          break;

        case '%':
          encodedString.append(utfString.charAt(++i));
          encodedString.append(utfString.charAt(++i));
          break;

        default:
          encodedString.append(Integer.toHexString(currentCharacter).toUpperCase());
          break;
      }
    }

    Preferences.setString(
        username, "saveState", (new BigInteger(encodedString.toString(), 36)).toString(10));
    if (!KoLConstants.saveStateNames.contains(username)) {
      KoLConstants.saveStateNames.add(username);
    }
  }

  public static final void removeSaveState(final String loginname) {
    if (loginname == null) {
      return;
    }

    KoLConstants.saveStateNames.remove(loginname);
    Preferences.setString(loginname, "saveState", "");
  }

  /**
   * Utility. The method used to decode a saved password. This should be called whenever a new
   * password intends to be stored in the global file.
   */
  public static final String getSaveState(final String loginname) {
    String password = Preferences.getString(loginname, "saveState");
    if (password == null || password.length() == 0 || password.contains("/")) {
      return null;
    }

    String hexString = (new BigInteger(password, 10)).toString(36);
    StringBuilder utfString = new StringBuilder();
    for (int i = 0; i < hexString.length(); ++i) {
      utfString.append('%');
      utfString.append(hexString.charAt(i));
      utfString.append(hexString.charAt(++i));
    }

    return StringUtilities.getURLDecode(utfString.toString());
  }

  public static final boolean checkRequirements(final List<AdventureResult> requirements) {
    return KoLmafia.checkRequirements(requirements, true);
  }

  public static final boolean checkRequirements(
      final List<AdventureResult> requirements, final boolean retrieveItem) {
    AdventureResult[] requirementsArray = new AdventureResult[requirements.size()];
    requirements.toArray(requirementsArray);

    long actualCount = 0;

    // Check the items required for this quest,
    // retrieving any items which might be inside
    // of a closet somewhere.

    for (int i = 0; i < requirementsArray.length; ++i) {
      if (requirementsArray[i] == null) {
        continue;
      }

      if (requirementsArray[i].isItem() && retrieveItem) {
        InventoryManager.retrieveItem(requirementsArray[i]);
      }

      if (requirementsArray[i].isItem()) {
        actualCount = requirementsArray[i].getCount(KoLConstants.inventory);
      } else if (requirementsArray[i].isStatusEffect()) {
        actualCount = requirementsArray[i].getCount(KoLConstants.activeEffects);
      } else if (requirementsArray[i].getName().equals(AdventureResult.MEAT)) {
        actualCount = KoLCharacter.getAvailableMeat();
      }

      if (actualCount >= requirementsArray[i].getCount()) {
        requirements.remove(requirementsArray[i]);
      } else if (actualCount > 0) {
        AdventureResult.addResultToList(
            requirements, requirementsArray[i].getInstance(0 - actualCount));
      }
    }

    // If there are any missing requirements
    // be sure to return false. Otherwise,
    // you managed to get everything.

    return requirements.isEmpty();
  }

  /**
   * Utility method used to purchase the given number of items from the mall using the given
   * purchase requests.
   */
  public static void makePurchases(
      final List<PurchaseRequest> results,
      final PurchaseRequest[] purchases,
      final int maxPurchases,
      final boolean isAutomated,
      final int priceLimit) {
    int firstIndex = 0;

    if (isAutomated) {
      // PC stores can be cheaper than NPC stores.  If we are
      // not allowed to purchase from the mall, skip through
      // requests until we find an NPC seller, if any.

      if (!Preferences.getBoolean("autoSatisfyWithMall")) {
        while (firstIndex < purchases.length) {
          PurchaseRequest currentRequest = purchases[firstIndex];
          if (currentRequest.getQuantity() == PurchaseRequest.MAX_QUANTITY) {
            break;
          }

          firstIndex++;
        }
      }

      // If we are allowed to purchase from the mall, make
      // sure that the price limit for automated purchases
      // makes sense.

      else if (Preferences.getInteger("autoBuyPriceLimit") == 0) {
        // this is probably due to an out-of-date defaults.txt
        Preferences.setInteger("autoBuyPriceLimit", 20000);
      }
    }

    if (firstIndex == purchases.length) {
      return;
    }

    PurchaseRequest firstRequest = purchases[firstIndex];

    List<AdventureResult> destination =
        // Only NPC stores have an infinite supply
        (!KoLCharacter.canInteract() && firstRequest.getQuantity() != PurchaseRequest.MAX_QUANTITY)
            ? KoLConstants.storage
            : KoLConstants.inventory;

    int remaining = maxPurchases;
    int itemId = 0;

    for (int i = firstIndex;
        i < purchases.length && remaining > 0 && KoLmafia.permitsContinue();
        ++i) {
      PurchaseRequest currentRequest = purchases[i];
      AdventureResult item = currentRequest.getItem();
      itemId = item.getItemId();

      if (itemId == ItemPool.TEN_LEAF_CLOVER
          && destination == KoLConstants.inventory
          && InventoryManager.cloverProtectionActive()
          && !KoLCharacter.inBeecore()
          && !KoLCharacter.inGLover()) {
        // Clover protection will miraculously turn ten-leaf
        // clovers into disassembled clovers as soon as they
        // come into inventory

        item = ItemPool.get(ItemPool.DISASSEMBLED_CLOVER, item.getCount());
      }

      int initialCount = item.getCount(destination);
      int currentCount = initialCount;
      int desiredCount =
          remaining == Integer.MAX_VALUE ? Integer.MAX_VALUE : initialCount + remaining;

      int currentPrice = currentRequest.getPrice();

      if ((priceLimit > 0 && currentPrice > priceLimit)
          || (isAutomated && currentPrice > Preferences.getInteger("autoBuyPriceLimit"))) {
        // KoLmafia.updateDisplay( MafiaState.ERROR,
        // "Stopped purchasing " + currentRequest.getItemName() + " @ " +
        // KoLConstants.COMMA_FORMAT.format( currentPrice ) + "." );

        // If we are purchasing multiple different items, the next item might be affordable.
        continue;
      }

      int previousLimit = currentRequest.getLimit();
      int toPurchase =
          Math.min(
              (int) Math.min(Integer.MAX_VALUE, currentRequest.getAvailableMeat() / currentPrice),
              Math.min(previousLimit, desiredCount - currentCount));
      currentRequest.setLimit(toPurchase);

      RequestThread.postRequest(currentRequest);

      // Update how many of the item we have post-purchase
      int purchased = item.getCount(destination) - currentCount;
      remaining -= purchased;

      // We've purchased as many as we will from this store

      // Restore original limit
      currentRequest.setLimit(previousLimit);

      // If purchase succeeded.
      if (KoLmafia.permitsContinue()) {
        // If original limit was less than original quantity, we have purchased some of our daily
        // limit
        if (previousLimit < currentRequest.getQuantity()) {
          currentRequest.setLimit(previousLimit - purchased);

          // If we have purchased the store's daily limit, done with store today.
          if (previousLimit == purchased) {
            currentRequest.setCanPurchase(false);
          }
        }

        // If this is not an NPC store, remove purchased items
        if (currentRequest.getQuantity() != PurchaseRequest.MAX_QUANTITY) {
          currentRequest.setQuantity(currentRequest.getQuantity() - purchased);
        }

        // If store is now empty. remove from result list
        if (currentRequest.getQuantity() == 0) {
          results.remove(currentRequest);
        }
      }
    }

    if (remaining == 0 || maxPurchases == Integer.MAX_VALUE) {
      KoLmafia.updateDisplay("Purchases complete.");
    } else {
      KoLmafia.updateDisplay(
          "Desired purchase quantity not reached (wanted "
              + maxPurchases
              + ", got "
              + (maxPurchases - remaining)
              + ")");
      StoreManager.flushCache(itemId);
    }
  }

  public static final void deleteAdventureOverride() {
    for (int i = 0; i < KoLConstants.OVERRIDE_DATA.length; ++i) {
      File dest = new File(KoLConstants.DATA_LOCATION, KoLConstants.OVERRIDE_DATA[i]);
      if (dest.exists()) {
        dest.delete();
      }
    }

    KoLmafia.updateDisplay("Please restart KoLmafia to complete the update.");
  }

  public static void gc() {
    int mem1 = (int) (Runtime.getRuntime().freeMemory() >> 10);
    System.gc();
    int mem2 = (int) (Runtime.getRuntime().freeMemory() >> 10);
    RequestLogger.printLine("Reclaimed " + (mem2 - mem1) + " KB of memory");
  }

  public static final boolean isAdventuring() {
    return KoLmafia.isAdventuring;
  }

  public static String whoisPlayer(final String player) {
    InternalChatRequest request = new InternalChatRequest("/whois " + player);
    RequestThread.postRequest(request);
    return request.responseText;
  }

  public static boolean isPlayerOnline(final String player) {
    // This player is currently online.
    // This player is currently online in channel clan.
    // This player is currently away from KoL in channel trade and listening to clan.
    String text = KoLmafia.whoisPlayer(player);
    return text != null && text.contains("This player is currently");
  }

  private static class UpdateCheckRunnable implements Runnable {
    public void run() {
      // TODO: Check for new version on jenkins\github after migration is complete. See revision
      // history for old release update check.
    }
  }

  public static void about() {
    new LicenseDisplayListener().run();
  }

  public static void quit() {
    if (KoLmafia.SESSION_ENDING) {
      return;
    }

    KoLmafia.SESSION_ENDING = true;

    LogoutManager.prepare();

    QuitRunnable quitRunnable = new QuitRunnable();

    if (SwingUtilities.isEventDispatchThread()) {
      KoLmafia.updateDisplay("Logout in progress (interface will be unresponsive)...");
      KoLmafia.allowDisplayUpdate = false;

      Thread quitThread = new Thread(quitRunnable);
      quitThread.start();

      try {
        quitThread.join();
      } catch (InterruptedException e) {
        System.out.println(e.getMessage() + " while trying to quit.");
      }
    } else {
      quitRunnable.run();
    }

    System.exit(0);
  }

  private static class QuitRunnable implements Runnable {
    public void run() {
      LogoutManager.logout();

      Preferences.reset(null);
      FlaggedItems.saveFlaggedItemList();

      RequestLogger.closeSessionLog();
      RequestLogger.closeDebugLog();
      RequestLogger.closeMirror();

      SystemTrayFrame.removeTrayIcon();
      RelayServer.stop();

      try {
        KoLmafia.SESSION_HOLDER.release();
        KoLmafia.SESSION_CHANNEL.close();
        KoLmafia.SESSION_FILE.delete();
      } catch (Exception e) {
        // That means the file either doesn't exist or
        // the session holder was somehow closed.
        // Ignore and fall through.
      }
    }
  }

  public static void preferences() {
    KoLmafiaGUI.constructFrame("OptionsFrame");
  }

  public static void preferencesThreaded() {
    RequestThread.runInParallel(new PreferencesRunnable());
  }

  private static class PreferencesRunnable implements Runnable {
    public void run() {
      KoLmafiaGUI.constructFrame("OptionsFrame");
    }
  }
}
