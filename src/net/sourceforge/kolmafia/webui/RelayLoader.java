package net.sourceforge.kolmafia.webui;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import net.sourceforge.kolmafia.KoLmafia;
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

  @Override
  public void run() {
    String location = this.location;

    if (location.startsWith("/")) {
      RelayLoader.startRelayServer();

      // Wait for 5 seconds before giving up
      // on the relay server.

      PauseObject pauser = new PauseObject();

      for (int i = 0; i < 50 && !RelayServer.isRunning(); ++i) {
        pauser.pause(200);
      }

      location = "http://127.0.0.1:" + RelayServer.getPort() + this.location;
    }

    URI uri = URI.create(location);

    try {
      Desktop.getDesktop().browse(uri);
    } catch (IOException e) {
    }
  }

  public static final synchronized void startRelayServer() {
    if (RelayServer.isRunning()) {
      return;
    }

    RelayServer.startThread();
  }

  public static final void openRelayBrowser() {
    KoLmafia.forceContinue();
    openSystemBrowser("game.php", true);
  }

  public static final void openSystemBrowser(final File file) {
    try {
      String location = file.getCanonicalPath();
      RelayLoader.openSystemBrowser("file://" + location, false);
    } catch (IOException e) {
    }
  }

  public static final void openSystemBrowser(final String location) {
    boolean isRelayLocation = !location.startsWith("http://") && !location.startsWith("https://");

    RelayLoader.openSystemBrowser(location, isRelayLocation);
  }

  public static final void openSystemBrowser(final String location, boolean isRelayLocation) {
    new RelayLoader(location, isRelayLocation).start();
  }
}
