package net.sourceforge.kolmafia.webui;

import java.awt.Desktop;
import java.awt.Desktop.Action;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.session.LoginManager;
import net.sourceforge.kolmafia.utilities.PauseObject;

public class RelayLoader extends Thread {

  private final String location;

  protected RelayLoader(final String location, final boolean isRelayLocation) {
    super("RelayLoader@" + location);

    if (isRelayLocation) {
      StringBuilder locationBuffer = new StringBuilder();

      if (!location.startsWith("/")) {
        locationBuffer.append("/");
      }

      if (location.endsWith("main.php")) {
        locationBuffer.append("game.php");
      } else {
        locationBuffer.append(location);
      }

      this.location = locationBuffer.toString();
    } else {
      this.location = location;
    }
  }

  private void pauseAndWaitForRelayAction(int timeInMilliseconds, boolean waitForStart) {
    // Pause for input value of seconds, waking up to check relay server
    // boolean so can be used to wait for shutdown.
    boolean relayInDesiredStatus = false;
    boolean relayServerIsRunning;
    final int pauseDurationInMilliseconds = 200;
    int waitCount = 0;
    PauseObject pauseExecutor = new PauseObject();
    while ((waitCount < timeInMilliseconds) && !relayInDesiredStatus) {
      pauseExecutor.pause(pauseDurationInMilliseconds);
      waitCount = waitCount + pauseDurationInMilliseconds;
      relayServerIsRunning = RelayServer.isRunning();
      relayInDesiredStatus = waitForStart == relayServerIsRunning;
    }
  }

  private void waitForSVNUpdateToFinish() {
    int triesLeft = 5;
    while ((triesLeft > 0) && LoginManager.isSvnLoginUpdateUnfinished()) {
      pauseAndWaitForRelayAction(1000, true);
      triesLeft--;
    }
  }

  @Override
  public void run() {
    String location = this.location;

    if (location.startsWith("/")) {
      waitForSVNUpdateToFinish();
      RelayLoader.startRelayServer();

      // Wait for 5 seconds before giving up on the relay server.
      pauseAndWaitForRelayAction(5000, true);
      location = "http://127.0.0.1:" + RelayServer.getPort() + this.location;
    }

    if (!Desktop.isDesktopSupported() || !Desktop.getDesktop().isSupported(Action.BROWSE)) {
      KoLmafia.updateDisplay(
          MafiaState.ERROR,
          "Cannot launch a browser in this environment. "
              + "Please visit "
              + location
              + " manually");
      return;
    }

    URI uri = URI.create(location);

    try {
      Desktop.getDesktop().browse(uri);
    } catch (IOException e) {
      KoLmafia.updateDisplay(
          "Exception: " + e.getMessage() + " for location " + location + " in browser.");
    }
  }

  public static synchronized void startRelayServer() {
    if (RelayServer.isRunning()) {
      return;
    }

    RelayServer.startThread();
  }

  public static void openRelayBrowser() {
    KoLmafia.forceContinue();
    openSystemBrowser("game.php", true);
  }

  public static void openSystemBrowser(final File file) {
    try {
      String location = file.getCanonicalPath();
      RelayLoader.openSystemBrowser("file://" + location, false);
    } catch (IOException e) {
      KoLmafia.updateDisplay(
          "Exception: " + e.getMessage() + " for location " + file.getName() + " in browser.");
    }
  }

  public static void openSystemBrowser(final String location) {
    boolean isRelayLocation = !location.startsWith("http://") && !location.startsWith("https://");

    RelayLoader.openSystemBrowser(location, isRelayLocation);
  }

  public static void openSystemBrowser(final String location, boolean isRelayLocation) {
    new RelayLoader(location, isRelayLocation).start();
  }
}
