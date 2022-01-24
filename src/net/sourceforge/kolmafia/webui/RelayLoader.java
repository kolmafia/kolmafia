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
  public static String currentBrowser = null;

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

  private void waitForSVNUpdateToFinish() {
    // This first loop is to wait for LoginManager to start processing updates, if it is going to
    PauseObject pauser = new PauseObject();
    for (int i = 0; i < 50 && !RelayServer.isRunning(); ++i) {
      pauser.pause(200);
    }
    int triesLeft = 10;
    while ((triesLeft > 0) && LoginManager.isSvnLoginUpdateRunning()) {
      for (int i = 0; i < 50 && !RelayServer.isRunning(); ++i) {
        pauser.pause(200);
      }
      triesLeft--;
    }
  }

  @Override
  public void run() {
    String location = this.location;

    if (location.startsWith("/")) {
      waitForSVNUpdateToFinish();
      RelayLoader.startRelayServer();

      // Wait for 5 seconds before giving up
      // on the relay server.

      PauseObject pauser = new PauseObject();

      for (int i = 0; i < 50 && !RelayServer.isRunning(); ++i) {
        pauser.pause(200);
      }

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
