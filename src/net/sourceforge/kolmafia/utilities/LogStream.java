package net.sourceforge.kolmafia.utilities;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Date;
import javax.swing.SwingUtilities;
import net.java.dev.spellcast.utilities.DataUtilities;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLDesktop;
import net.sourceforge.kolmafia.StaticEntity;

public class LogStream extends PrintStream implements Runnable {
  private File proxy;

  public static PrintStream openStream(final String filename, final boolean forceNewFile) {
    return LogStream.openStream(new File(KoLConstants.ROOT_LOCATION, filename), forceNewFile);
  }

  public static PrintStream openStream(final File file, final boolean forceNewFile) {
    return LogStream.openStream(file, forceNewFile, "UTF-8");
  }

  public static PrintStream openStream(
      final File file, final boolean forceNewFile, final String encoding) {
    OutputStream ostream = DataUtilities.getOutputStream(file, !forceNewFile);
    PrintStream pstream = openStream(ostream, encoding);

    if (!(pstream instanceof LogStream)) {
      return pstream;
    }

    LogStream newStream = (LogStream) pstream;

    if (file.getName().startsWith("DEBUG")) {
      if (KoLDesktop.instanceExists()) {
        newStream.proxy = file;
        SwingUtilities.invokeLater(newStream);
      }

      newStream.println();
      newStream.println();
      newStream.println("=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=");

      StringBuilder versionData = new StringBuilder();
      versionData.append(StaticEntity.getVersion());
      versionData.append(", ");
      versionData.append(System.getProperty("os.name"));
      versionData.append(", Java ");
      versionData.append(System.getProperty("java.version"));

      int leftIndent = (66 - versionData.length()) / 2;
      for (int i = 0; i < leftIndent; ++i) {
        versionData.insert(0, ' ');
      }

      newStream.println(versionData.toString());

      newStream.println("=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=");
      newStream.println(" Please note: do not post this log in the KoLmafia thread of KoL's");
      newStream.println(" Gameplay-Discussion forum. If you would like the KoLmafia dev team");
      newStream.println(" to look at it, please write a bug report at kolmafia.us. Include");
      newStream.println(" specific information about what you were doing when you made this");
      newStream.println(" and include this log as an attachment.");
      newStream.println("=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=");
      newStream.println(" Timestamp: " + (new Date()).toString());
      newStream.println("=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=");
      newStream.println(" User: " + KoLCharacter.getUserName());
      newStream.println(" Current run: " + KoLCharacter.getCurrentRun());
      newStream.println(" MRU Script: " + KoLConstants.scriptMList.getFirst());
      newStream.println("=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=");
      newStream.println();
      newStream.println();
    }

    return newStream;
  }

  public static PrintStream openStream(final OutputStream ostream, final String encoding) {
    try {
      return new LogStream(ostream, encoding);
    } catch (IOException e) {
      e.printStackTrace();
      return System.out;
    }
  }

  public void run() {
    KoLDesktop.getInstance().getRootPane().putClientProperty("Window.documentFile", this.proxy);
  }

  private LogStream(final OutputStream ostream, final String encoding) throws IOException {
    super(ostream, true, encoding);
  }
}
