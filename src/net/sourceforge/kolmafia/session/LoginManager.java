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
import net.sourceforge.kolmafia.request.LoginRequest;
import net.sourceforge.kolmafia.request.LogoutRequest;
import net.sourceforge.kolmafia.request.MallPurchaseRequest;
import net.sourceforge.kolmafia.request.PasswordHashRequest;
import net.sourceforge.kolmafia.request.RelayRequest;
import net.sourceforge.kolmafia.request.coinmaster.HermitRequest;
import net.sourceforge.kolmafia.scripts.git.GitManager;
import net.sourceforge.kolmafia.scripts.svn.SVNManager;
import net.sourceforge.kolmafia.session.PingManager.PingAbortTrigger;
import net.sourceforge.kolmafia.session.PingManager.PingTest;
import net.sourceforge.kolmafia.swingui.GenericFrame;
import net.sourceforge.kolmafia.utilities.InputFieldUtilities;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class LoginManager {

  // This exists to delay launch of the relay browser at startup until after SVN updates
  // have completed.
  private static boolean svnLoginUpdateNotFinished = true;

  // A password hash for when we time you out, for use when timing in.
  public static final String BOGUS_PASSWORD_HASH = "ThisIsAnEntirelyBogusPasswordHash";

  private LoginManager() {}

  public static boolean ping() {
    try {
      return ping(++LoginRequest.loginPingAttempt);
    } finally {
      --LoginRequest.loginPingAttempt;
    }
  }

  private static boolean ping(int attempt) {
    // Optionally run a ping test and check connection speed.
    // Return true if the connection is acceptable.

    // If user does not want to measure ping speed, good enough
    if (!Preferences.getBoolean("pingLogin")) {
      return true;
    }

    // If user is using the dev server, they're stuck with the ping they get
    if (KoLmafia.usingDevServer()) {
      return true;
    }

    // The user wants to measure ping speed.
    var result = PingManager.runPingTest(true);

    // If the ping test failed, give up; error already logged.
    if (result.getAverage() == 0) {
      return true;
    }

    KoLmafia.updateDisplay(
        "Ping test: average delay is "
            + KoLConstants.FLOAT_FORMAT.format(result.getAverage())
            + " msecs.");

    // See if the Ping tested a suitable page
    if (!result.isSaveable()) {
      // Perhaps we redirected to afterlife.php or something.
      // We can only compare averages vs. "normal" pages.
      return true;
    }

    // See whether user wants to check ping speed.
    double average = result.getAverage();
    String checkType = Preferences.getString("pingLoginCheck");
    String error = "";
    switch (checkType) {
      case "goal" -> {
        // The user has set a specific goal, presumably based on observation.
        int goal = Preferences.getInteger("pingLoginGoal");
        if (goal > 0) {
          if (average <= goal) {
            return true;
          }
          error = "you want no more than " + String.valueOf(goal) + " msec";
        }
        // Either no goal is set or this connection is too slow.
        // Alert the user.
      }
      case "threshold" -> {
        // The user wants to be "close" to the best ping seen.
        // Get the shortest ping test time we've seen. If the ping test we
        // just ran is the first, that will be it.
        double threshold = 1.0 + Preferences.getDouble("pingLoginThreshold");
        var shortest = PingTest.parseProperty("pingShortest");
        double desired = threshold * shortest.getAverage();
        if (average <= desired) {
          return true;
        }
        // Either no threshold is set or this connection is too slow.
        error = "you want no more than " + KoLConstants.FLOAT_FORMAT.format(desired) + " msec";
        // Alert the user.
      }
      default -> {
        // The user is happy with any connection
        return true;
      }
    }

    // If the ping test aborted because times exceeded a user-defined
    // trigger, log that.
    PingAbortTrigger trigger = result.getTrigger();
    if (trigger != null) {
      StringBuilder buf = new StringBuilder();
      buf.append("Ping test aborted because ");
      int count = trigger.getCount();
      buf.append(String.valueOf(count));
      buf.append(" ping");
      if (count != 1) {
        buf.append("s");
      }
      buf.append(" exceeded ");
      var shortest = PingTest.parseProperty("pingShortest");
      double limit = trigger.getFactor() * shortest.getAverage();
      buf.append(KoLConstants.FLOAT_FORMAT.format(limit));
      buf.append(" msec.");
      KoLmafia.updateDisplay(buf.toString());
    }

    // Perhaps the user wants to automatically retry for a certain
    // number of connections.
    int allowed = Preferences.getInteger("pingLoginCount");
    if (allowed > 0) {
      if (attempt < allowed) {
        return LoginRequest.relogin();
      }
      // We've reached our limit
      StringBuilder buf = new StringBuilder();
      buf.append("We've tried ");
      buf.append(String.valueOf(attempt));
      buf.append(" times to get a fast connection");
      if (!error.equals("")) {
        buf.append(" - ");
        buf.append(error);
        buf.append(" - but failed");
      }
      buf.append(".");
      KoLmafia.updateDisplay(buf.toString());
      switch (Preferences.getString("pingLoginFail")) {
        case "logout" -> {
          // The user wants to give up in despair
          KoLmafia.updateDisplay("Giving up and logging out.");
          RequestThread.postRequest(new LogoutRequest());
          GenericRequest.passwordHash = BOGUS_PASSWORD_HASH;
          return false;
        }
        case "confirm" -> {
          // The user wants to make a manual decision.
          StringBuilder dialog = reportFailure(average, error);
          dialog.append(" Perhaps you are on a different (slower) network than usual.");
          switch (checkType) {
            case "goal" -> {
              dialog.append(" Your goal may not be achievable.");
              dialog.append(" Press 'OK' to accept this connection and finish logging in;");
              dialog.append(
                  " you can adjust your settings at Preferences/General/Connection Options.");
              InputFieldUtilities.alert(dialog.toString());
            }
            case "threshold" -> {
              dialog.append(
                  " Your threshold is likely still usable, but your historic ping data no longer applies.");
              dialog.append(" Press 'Yes' to clear your historic ping data.");
              dialog.append(" Press 'No' to leave your historic ping data intact.");
              dialog.append(" In either case, accept this connection and finish logging in;");
              dialog.append(
                  " you can adjust your settings at Preferences/General/Connection Options.");
              boolean clear = InputFieldUtilities.confirm(dialog.toString());
              if (clear) {
                Preferences.setString("pingLongest", "");
                Preferences.setString("pingShortest", "");
                result.save();
              }
            }
          }
          // Fall through to finish login
          break;
        }
      }
      KoLmafia.updateDisplay(
          "Accepting the last attempt of " + KoLConstants.FLOAT_FORMAT.format(average) + " msec.");
      return true;
    }

    // InputFieldUtilities.confirm works either headless or with a Swing dialog

    // The user set a goal or threshold which was not fulfilled.
    // Ask them if they want to accept it, try again, or give up.

    StringBuilder buf = reportFailure(average, error);

    buf.append(" Press 'Yes' if you are satisfied with the current connection.");
    buf.append(" Press 'No' to log out and back in to try for a better connection.");
    buf.append(" Press 'Cancel' to simply log out.");

    Boolean confirmed = InputFieldUtilities.yesNoCancelDialog(buf.toString());

    // If the user canceled, log out
    if (confirmed == null) {
      RequestThread.postRequest(new LogoutRequest());
      // If this was from a timein, we still have a GUI and can submit URLs which
      // need a pwd. Save a bogus pwd which will be replaced by a real one if we
      // eventually time in and accept a ping.
      GenericRequest.passwordHash = BOGUS_PASSWORD_HASH;
      return false;
    }

    // If the user said "Yes", accept it.
    if (confirmed) {
      return true;
    }

    // The user finds the ping time unacceptable.
    return LoginRequest.relogin();
  }

  private static StringBuilder reportFailure(double average, String error) {
    // Report a ping failure. The caller will decide how to craft a
    // dialog to report it to the user.

    StringBuilder buf = new StringBuilder();
    buf.append("This connection has an average ping time of ");
    buf.append(KoLConstants.FLOAT_FORMAT.format(average));
    buf.append(" msec");
    if (!error.equals("")) {
      buf.append(", but ");
      buf.append(error);
    }
    buf.append(".");
    return buf;
  }

  public static void login(String username) {
    try {
      if (!KoLmafia.acquireFileLock(Preferences.baseUserName(username))) {
        // acquireFileLock should call updateDisplay with a more detailed error message.
        return;
      }
      KoLmafia.forceContinue();
      LoginManager.doLogin(username);
    } catch (Exception e) {
      // What should we do here?
      StaticEntity.printStackTrace(e, "Error during session initialization");
    }
  }

  public static void timein(final String username) {
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
    svnLoginUpdateNotFinished = false;

    if (Preferences.getBoolean("gitUpdateOnLogin") && !Preferences.getBoolean("_gitUpdated")) {
      GitManager.updateAll();
    }

    if (Preferences.getBoolean(username, "getBreakfast")) {
      int today = HolidayDatabase.getPhaseStep();
      BreakfastManager.getBreakfast(Preferences.getInteger("lastBreakfast") != today);
      Preferences.setInteger("lastBreakfast", today);
    }

    if (Preferences.getBoolean("sharePriceData") || !MallPriceDatabase.PRICE_FILE.exists()) {
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

    // Log when a session is started

    RequestLogger.updateSessionLog();
    RequestLogger.updateSessionLog("Initializing session for " + username + "...");

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

    printWarningMessages();
  }

  public static String getCurrentHoliday() {
    var holidaySummary = HolidayDatabase.getHolidaySummary();
    var moonEffect = HolidayDatabase.getMoonEffect();
    var text = (holidaySummary.isEmpty()) ? moonEffect : holidaySummary + ", " + moonEffect;
    return StringUtilities.getEntityDecode(text);
  }

  public static void showCurrentHoliday() {
    KoLmafia.updateDisplay(getCurrentHoliday());
  }

  public static boolean isSvnLoginUpdateUnfinished() {
    return svnLoginUpdateNotFinished;
  }

  private static void printWarningMessages() {
    var version = Runtime.version();
    if (version.feature() < 21) {
      KoLmafia.updateDisplay("Java versions lower than 21 will stop being supported by KoLMafia.");
      KoLmafia.updateDisplay(
          "You are running a version of Java lower than 21. Visit https://adoptium.net/ to download a newer version of Java.");
    }
  }
}
