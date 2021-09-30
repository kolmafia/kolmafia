package net.sourceforge.kolmafia.webui;

import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.Set;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.StaticEntity;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.utilities.FileUtilities;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class RelayServer implements Runnable {
  public static final Set<RelayAgent> agentThreads = new HashSet<RelayAgent>();

  private static long lastStatusMessage = 0;
  private static Thread relayThread = null;

  private ServerSocket serverSocket = null;
  private static int port = 60080;
  private static boolean listening = false;
  private static boolean updateStatus = false;

  private static final RelayServer INSTANCE = new RelayServer();
  private static final StringBuffer statusMessages = new StringBuffer();

  private RelayServer() {
    for (int i = 0; i < KoLConstants.RELAY_FILES.length; ++i) {
      FileUtilities.loadLibrary(
          KoLConstants.RELAY_LOCATION, KoLConstants.RELAY_DIRECTORY, KoLConstants.RELAY_FILES[i]);
    }
    Preferences.setString("lastRelayUpdate", StaticEntity.getVersion());
  }

  public static final void updateStatus() {
    RelayServer.updateStatus = true;
  }

  public static final synchronized void startThread() {
    if (RelayServer.relayThread == null) {
      Thread relayServer = new Thread(RelayServer.INSTANCE, "LocalRelayServer");
      relayServer.start();
      RelayServer.relayThread = relayServer;
    }
  }

  public static final int getPort() {
    return RelayServer.port;
  }

  public static final String trimPrefix(final String location) {
    // Remove prefix that will connect to our own relay socket

    if (!location.startsWith("http://")) {
      return location;
    }

    int pathIndex = location.indexOf("/", 7);
    if (pathIndex == -1) {
      return location;
    }

    int colon = location.indexOf(":", 7);
    if (colon == -1 || colon > pathIndex) {
      return location;
    }

    int port = StringUtilities.parseInt(location.substring(colon + 1, pathIndex));
    if (port != RelayServer.port) {
      return location;
    }

    try {
      String host = location.substring(7, colon);
      InetAddress address = InetAddress.getByName(host);
      if (address.isLoopbackAddress()) {
        return location.substring(pathIndex + 1);
      }
    } catch (UnknownHostException e) {
    }

    return location;
  }

  public static final boolean isRunning() {
    return RelayServer.listening;
  }

  public static final void stop() {
    RelayServer.listening = false;
  }

  public void run() {
    boolean startedSuccessfully = true;
    Integer minPort = 60080;
    Integer maxPort = minPort + 10;
    RelayServer.port = minPort;
    while (!this.openServerSocket()) {
      if (RelayServer.port < maxPort) {
        ++RelayServer.port;
      } else {
        KoLmafia.updateDisplay(
            KoLConstants.MafiaState.ERROR,
            "Failed to find free port in range " + minPort + " to " + maxPort + ".");
        startedSuccessfully = false;
        break;
      }
    }

    if (startedSuccessfully) {
      RelayServer.listening = true;
    }

    while (RelayServer.listening) {
      try {
        this.dispatchAgent(this.serverSocket.accept());
      } catch (Exception e) {
        // If an exception occurs here, that means
        // someone closed the thread; just reset
        // the listening state and fall through.

        RelayServer.listening = false;
      }
    }

    this.closeAgents();

    try {
      if (this.serverSocket != null) {
        this.serverSocket.close();
      }
    } catch (Exception e) {
      // The end result of a socket closing
      // should not throw an exception, but
      // if it does, the socket closes.
    }

    this.serverSocket = null;
    RelayServer.relayThread = null;
  }

  private synchronized boolean openServerSocket() {
    try {
      if (Preferences.getBoolean("relayAllowRemoteAccess")) {
        this.serverSocket = new ServerSocket(RelayServer.port, 25);
      } else {
        this.serverSocket =
            new ServerSocket(RelayServer.port, 25, InetAddress.getByName("127.0.0.1"));
      }

      return true;
    } catch (Exception e) {
      return false;
    }
  }

  private synchronized void closeAgents() {
    for (RelayAgent agent : agentThreads) {
      agent.setSocket(null);
    }
    agentThreads.clear();
  }

  private synchronized void dispatchAgent(final Socket socket) {
    for (RelayAgent agent : agentThreads) {
      if (agent.isWaiting()) {
        agent.setSocket(socket);
        return;
      }
    }

    this.createAgent(socket);
  }

  private synchronized void createAgent(final Socket socket) {
    RelayAgent agent = new RelayAgent(RelayServer.agentThreads.size());
    agent.setSocket(socket);

    RelayServer.agentThreads.add(agent);
    agent.start();
  }

  public static final void addStatusMessage(final String message) {
    if (System.currentTimeMillis() - RelayServer.lastStatusMessage < 4000) {
      RelayServer.statusMessages.append(message);
    }
  }

  public static final String getNewStatusMessages() {
    if (RelayServer.updateStatus) {
      RelayServer.updateStatus = false;
      RelayServer.statusMessages.append("<!-- REFRESH -->");
    }

    String newMessages = RelayServer.statusMessages.toString();
    RelayServer.statusMessages.setLength(0);

    RelayServer.lastStatusMessage = System.currentTimeMillis();
    return newMessages;
  }
}
