package net.sourceforge.kolmafia.session;

import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.KoLmafiaCLI;
import net.sourceforge.kolmafia.KoLmafiaGUI;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.SpecialOutfit;
import net.sourceforge.kolmafia.StaticEntity;
import net.sourceforge.kolmafia.chat.ChatManager;
import net.sourceforge.kolmafia.persistence.AdventureDatabase;
import net.sourceforge.kolmafia.persistence.AdventureQueueDatabase;
import net.sourceforge.kolmafia.persistence.AdventureSpentDatabase;
import net.sourceforge.kolmafia.persistence.ConcoctionDatabase;
import net.sourceforge.kolmafia.persistence.HolidayDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.persistence.MallPriceDatabase;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.ApiRequest;
import net.sourceforge.kolmafia.request.CampgroundRequest;
import net.sourceforge.kolmafia.request.CharPaneRequest;
import net.sourceforge.kolmafia.request.ChateauRequest;
import net.sourceforge.kolmafia.request.FalloutShelterRequest;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.request.HermitRequest;
import net.sourceforge.kolmafia.request.LoginRequest;
import net.sourceforge.kolmafia.request.MallPurchaseRequest;
import net.sourceforge.kolmafia.request.PasswordHashRequest;
import net.sourceforge.kolmafia.request.RelayRequest;
import net.sourceforge.kolmafia.svn.SVNManager;
import net.sourceforge.kolmafia.swingui.GenericFrame;

public class LoginManager {
  public static void login(String username) {
    try {
      KoLmafia.forceContinue();
      LoginManager.doLogin(username);
    } catch (Exception e) {
      // What should we do here?
      StaticEntity.printStackTrace(e, "Error during session initialization");
    }
  }

  public static final void timein(final String username) {
    // Save the current user settings to disk
    Preferences.reset(null);

    // Reload the current user's preferences
    Preferences.reset(username);

    // Close existing session log and reopen it
    RequestLogger.closeSessionLog();
    RequestLogger.openSessionLog();

    // The password hash changes for each session
    RequestThread.postRequest(new PasswordHashRequest("lchat.php"));

    // See if we are timing in across rollover.
    // api.php has rollover time in it
    String redirection = ApiRequest.updateStatus();
    if (redirection != null && redirection.startsWith("afterlife.php")) {
      return;
    }

    // Assume if rollover has changed by an hour, it is a new rollover.
    // Time varies slightly between servers by a few seconds.
    long lastRollover = Preferences.getLong("lastCounterDay");
    long newRollover = KoLCharacter.getRollover();
    if ((newRollover - lastRollover) > 3600) {
      // This is the first login after rollover.
      // Treat it as a full login
      LoginManager.login(username);
      return;
    }

    // Some things aren't properly set by KoL until main.php is loaded
    KoLmafia.makeMainRequest();
  }

  private static void doLogin(String username) {
    LoginRequest.isLoggingIn(true);

    try {
      ConcoctionDatabase.deferRefresh(true);
      LoginManager.initialize(username);
    } finally {
      ConcoctionDatabase.deferRefresh(false);
      LoginRequest.isLoggingIn(false);
    }

    // Abort further processing in Valhalla.
    if (CharPaneRequest.inValhalla()) {
      return;
    }

    // Abort further processing if we logged in to a fight or choice
    if (KoLmafia.isRefreshing()) {
      return;
    }

    if (Preferences.getBoolean("svnUpdateOnLogin") && !Preferences.getBoolean("_svnUpdated")) {
      SVNManager.doUpdate();
    }

    if (Preferences.getBoolean(username, "getBreakfast")) {
      int today = HolidayDatabase.getPhaseStep();
      BreakfastManager.getBreakfast(Preferences.getInteger("lastBreakfast") != today);
      Preferences.setInteger("lastBreakfast", today);
    }

    if (Preferences.getBoolean("sharePriceData")) {
      MallPriceDatabase.updatePricesInParallel(
          "https://kolmafia.us/scripts/updateprices.php?action=getmap");
    }

    // Also, do mushrooms, if a mushroom script has already
    // been setup by the user.

    if (Preferences.getBoolean(
        "autoPlant" + (KoLCharacter.canInteract() ? "Softcore" : "Hardcore"))) {
      String currentLayout = Preferences.getString("plantingScript");
      if (!currentLayout.equals("")
          && KoLCharacter.knollAvailable()
          && !KoLCharacter.inZombiecore()
          && MushroomManager.ownsPlot()) {
        KoLmafiaCLI.DEFAULT_SHELL.executeLine(
            "call " + KoLConstants.PLOTS_DIRECTORY + currentLayout + ".ash");
      }
    }

    String scriptSetting = Preferences.getString("loginScript");
    if (!scriptSetting.equals("")) {
      KoLmafiaCLI.DEFAULT_SHELL.executeLine(scriptSetting);
    }

    if (EventManager.hasEvents()) {
      KoLmafiaCLI.DEFAULT_SHELL.executeLine("events");
    }
  }

  /**
   * Initializes the <code>KoLmafia</code> session. Called after the login has been confirmed to
   * notify that the login was successful, the user-specific settings should be loaded, and the user
   * can begin adventuring.
   */
  public static void initialize(final String username) {
    // Load the JSON string first, so we can use it, if necessary.
    ActionBarManager.loadJSONString();

    // Initialize the variables to their initial states to avoid
    // null pointers getting thrown all over the place

    // Do this first to reset per-player item aliases
    ItemDatabase.reset();

    KoLCharacter.reset(username);

    // Get rid of cached password hashes in KoLAdventures
    AdventureDatabase.refreshAdventureList();

    // Clear and/or load overridden image cache
    RelayRequest.loadOverrideImages(Preferences.getBoolean("relayOverridesImages"));

    // Load (or reset) adventure queue
    AdventureQueueDatabase.deserialize();
    AdventureSpentDatabase.deserialize();

    // Reset all per-player information

    ChatManager.reset();
    MailManager.clearMailboxes();
    StoreManager.clearCache();
    DisplayCaseManager.clearCache();
    ClanManager.clearCache(true);
    BanishManager.clearCache();

    CampgroundRequest.reset();
    ChateauRequest.reset();
    HermitRequest.reset();
    FalloutShelterRequest.reset();
    MallPurchaseRequest.reset();
    MonsterManuelManager.reset();
    SpecialOutfit.forgetCheckpoints();

    KoLmafia.updateDisplay("Initializing session for " + username + "...");
    Preferences.setString("lastUsername", username);

    // Open the session log

    RequestLogger.openSessionLog();

    // Perform requests to read current character's data

    KoLmafia.refreshSession();

    // Reset the session tally and encounter list

    KoLmafia.resetSession();

    // If desired, show status in session log

    if (Preferences.getBoolean("logStatusOnLogin")) {
      KoLmafiaCLI.DEFAULT_SHELL.executeCommand("log", "snapshot");
    }

    // If the password hash is empty, then that means you
    // might be mid-transition.

    if (GenericRequest.passwordHash.equals("")) {
      PasswordHashRequest request = new PasswordHashRequest("lchat.php");
      RequestThread.postRequest(request);
    }

    if (Preferences.getString("spadingData").length() > 10) {
      KoLmafia.updateDisplay(
          "Some data has been collected that may be of interest to others. "
              + "Please type `spade' to examine and optionally submit the data or `spade autoconfirm'"
              + " to submit all of the spaded data. Either way the data will be deleted whether shared"
              + " or not.");
    }

    // Rebuild Scripts menu if needed
    GenericFrame.compileScripts();

    if (StaticEntity.isGUIRequired()) {
      KoLmafiaGUI.intializeMainInterfaces();
    } else if (Preferences.getString("initialFrames").contains("LocalRelayServer")) {
      KoLmafiaGUI.constructFrame("LocalRelayServer");
    }

    LoginManager.showCurrentHoliday();

    if (MailManager.hasNewMessages()) {
      KoLmafia.updateDisplay("You have new mail.");
    }
  }

  public static void showCurrentHoliday() {
    String holiday = HolidayDatabase.getHoliday(true);
    String moonEffect = HolidayDatabase.getMoonEffect();
    String updateText = (holiday.equals("")) ? moonEffect : holiday + ", " + moonEffect;

    KoLmafia.updateDisplay(updateText);
  }
}
