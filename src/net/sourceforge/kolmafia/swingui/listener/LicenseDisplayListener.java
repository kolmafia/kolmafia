package net.sourceforge.kolmafia.swingui.listener;

import javax.swing.SwingUtilities;
import net.java.dev.spellcast.utilities.LicenseDisplay;
import net.sourceforge.kolmafia.CreateFrameRunnable;
import net.sourceforge.kolmafia.swingui.panel.VersionDataPanel;

public class LicenseDisplayListener extends ThreadedListener {
  private static final String[] LICENSE_FILENAME = {
    "kolmafia-license.txt",
    "spellcast-license.txt",
    "centerkey-license.txt",
    "sungraphics-license.txt",
    "jsmooth-license.txt",
    "osxadapter-license.txt",
    "htmlcleaner-license.txt",
    "json.txt",
    "swinglabs-license.txt",
    "LICENSE-SVNKIT.txt",
    "sorttable-license.txt",
    "flatlaf-license.txt",
    "unlicensed.htm"
  };

  private static final String[] LICENSE_NAME = {
    "KoLmafia",
    "Spellcast",
    "BrowserLauncher",
    "Sun Graphics",
    "JSmooth",
    "OSXAdapter",
    "HtmlCleaner",
    "JSON",
    "SwingLabs",
    "SVNKit",
    "Sort Table",
    "FlatMap Look and Feel",
    "Unlicensed"
  };

  @Override
  protected void execute() {
    Object[] parameters = new Object[4];
    parameters[0] = "KoLmafia: Copyright Notices";
    parameters[1] = new VersionDataPanel();
    parameters[2] = LICENSE_FILENAME;
    parameters[3] = LICENSE_NAME;

    SwingUtilities.invokeLater(new CreateFrameRunnable(LicenseDisplay.class, parameters));
  }
}
