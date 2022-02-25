package net.sourceforge.kolmafia;

import com.sun.java.forums.CloseableTabbedPane;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import javax.swing.Box;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JTabbedPane;
import javax.swing.JToolBar;
import javax.swing.SwingConstants;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.swingui.AdventureFrame;
import net.sourceforge.kolmafia.swingui.ChatFrame;
import net.sourceforge.kolmafia.swingui.GenericFrame;
import net.sourceforge.kolmafia.swingui.SendMessageFrame;
import net.sourceforge.kolmafia.swingui.button.DisplayFrameButton;
import net.sourceforge.kolmafia.swingui.button.InvocationButton;
import net.sourceforge.kolmafia.swingui.button.RelayBrowserButton;
import net.sourceforge.kolmafia.swingui.listener.TabFocusingListener;
import net.sourceforge.kolmafia.utilities.PauseObject;
import net.sourceforge.kolmafia.webui.RelayLoader;

public class KoLDesktop extends GenericFrame {
  private final MinimizeListener minimizeListener = new MinimizeListener();
  private static KoLDesktop INSTANCE = null;
  private static boolean isInitializing = false;

  private final List<GenericFrame> tabListing = new ArrayList<>();

  public JPanel compactPane;

  private JProgressBar memoryUsageLabel;
  public JLabel levelLabel, roninLabel, mcdLabel;
  public JLabel musLabel, mysLabel, moxLabel, drunkLabel;
  public JLabel hpLabel, mpLabel, meatLabel, advLabel;
  public JLabel familiarLabel;

  private KoLDesktop(final String title) {
    super(StaticEntity.getVersion() + " Main Interface");

    if (StaticEntity.usesSystemTray()) {
      this.addWindowListener(minimizeListener);
    }

    this.tabs.setTabPlacement(SwingConstants.TOP);
    this.setCenterComponent(this.tabs);

    this.addCompactPane();

    this.getToolbar();
    this.addScriptPane();

    KoLDesktop.INSTANCE = this;
    new MemoryUsageMonitor().start();
  }

  @Override
  public boolean showInWindowMenu() {
    return false;
  }

  @Override
  public boolean shouldAddStatusBar() {
    return false;
  }

  @Override
  public JTabbedPane getTabbedPane() {
    return Preferences.getBoolean("allowCloseableDesktopTabs")
        ? new CloseableTabbedPane()
        : new JTabbedPane();
  }

  private void initializeTabs() {
    if (KoLDesktop.isInitializing) {
      return;
    }

    KoLDesktop.isInitializing = true;
    KoLmafiaGUI.checkFrameSettings();

    String interfaceSetting = Preferences.getString("initialDesktop");
    for (String frameName : interfaceSetting.split("\\s*,\\s*")) {
      if (frameName.equals("LocalRelayServer")) {
        RelayLoader.startRelayServer();
        continue;
      }

      KoLmafiaGUI.constructFrame(frameName);
    }

    this.pack();

    this.tabs.addChangeListener(new TabFocusingListener());

    KoLDesktop.isInitializing = false;
  }

  public static final boolean isInitializing() {
    return KoLDesktop.isInitializing;
  }

  @Override
  public void dispose() {
    StaticEntity.unregisterPanels(this.tabs);

    this.removeWindowListener(minimizeListener);

    if (Preferences.getBoolean("rememberDesktopSize")) {
      Dimension tempDim = this.getSize();
      Preferences.setInteger("desktopHeight", (int) tempDim.getHeight());
      Preferences.setInteger("desktopWidth", (int) tempDim.getWidth());
    }

    Iterator<GenericFrame> tabIterator = this.tabListing.iterator();

    while (tabIterator.hasNext()) {
      GenericFrame gframe = tabIterator.next();

      gframe.dispose();

      tabIterator.remove();
    }

    this.tabs.removeAll();

    KoLDesktop.INSTANCE = null;

    super.dispose();
  }

  public static final boolean instanceExists() {
    return KoLDesktop.INSTANCE != null;
  }

  public static final KoLDesktop getInstance() {
    if (KoLDesktop.INSTANCE == null) {
      new KoLDesktop(StaticEntity.getVersion());
      KoLDesktop.INSTANCE.initializeTabs();

      if (Preferences.getBoolean("rememberDesktopSize")) {
        int width = Preferences.getInteger("desktopWidth");
        int height = Preferences.getInteger("desktopHeight");

        KoLDesktop.INSTANCE.setSize(width, height);
      }

      KoLDesktop.INSTANCE.setVisible(true);
    }

    return KoLDesktop.INSTANCE;
  }

  static final void addTab(final GenericFrame content) {
    if (KoLDesktop.INSTANCE == null) {
      return;
    }

    int tabIndex = KoLDesktop.INSTANCE.tabListing.indexOf(content);
    if (tabIndex == -1) {
      KoLDesktop.INSTANCE.tabListing.add(content);
      KoLDesktop.INSTANCE.tabs.addTab(content.getLastTitle(), content.getCenterComponent());

      if (content.tabs != null && !KoLDesktop.isInversionExempt(content)) {
        content.tabs.setTabPlacement(SwingConstants.BOTTOM);
      }
    } else {
      KoLDesktop.INSTANCE.tabs.setSelectedIndex(tabIndex);
    }
  }

  private static boolean isInversionExempt(final GenericFrame content) {
    return content instanceof AdventureFrame || content instanceof SendMessageFrame;
  }

  @Override
  public void pack() {
    super.pack();
  }

  static final boolean showComponent(final GenericFrame content) {
    if (KoLDesktop.INSTANCE == null) {
      return false;
    }

    int tabIndex = KoLDesktop.INSTANCE.tabListing.indexOf(content);
    if (tabIndex == -1) {
      return false;
    }

    int currentTabIndex = KoLDesktop.INSTANCE.tabs.getSelectedIndex();
    if (tabIndex == currentTabIndex) {
      JComponent selected = (JComponent) KoLDesktop.INSTANCE.tabs.getSelectedComponent();
      selected.requestFocus();
    } else {
      KoLDesktop.INSTANCE.tabs.setSelectedIndex(tabIndex);
    }

    return true;
  }

  public static final void setTitle(final GenericFrame content, final String newTitle) {
    if (KoLDesktop.INSTANCE == null) {
      return;
    }

    int tabIndex = KoLDesktop.INSTANCE.tabListing.indexOf(content);
    if (tabIndex != -1) {
      KoLDesktop.INSTANCE.tabs.setTitleAt(tabIndex, newTitle);
    }
  }

  static final void updateTitle() {
    if (KoLDesktop.INSTANCE != null) {
      KoLDesktop.INSTANCE.setTitle(KoLDesktop.INSTANCE.lastTitle);
    }

    Arrays.stream(Frame.getFrames())
        .filter(GenericFrame.class::isInstance)
        .forEach(f -> f.setTitle(((GenericFrame) f).getLastTitle()));
  }

  @Override
  public JToolBar getToolbar() {
    JToolBar toolbarPanel = super.getToolbar();
    if (toolbarPanel == null) {
      return null;
    }

    String iconSetPrefix;
    // instead of this the final version will get the prefix from a variable (or none for "classic"
    // (current) icons).
    // it would be really nice if it were a
    iconSetPrefix = KoLmafiaGUI.isDarkTheme() ? "themes/dark/" : "";

    toolbarPanel.add(Box.createVerticalStrut(50));

    toolbarPanel.add(Box.createHorizontalStrut(5));

    toolbarPanel.add(
        new DisplayFrameButton("Council", iconSetPrefix + "council.gif", "CouncilFrame"));
    toolbarPanel.add(
        new RelayBrowserButton("Load in Web Browser", iconSetPrefix + "browser.gif", null));

    toolbarPanel.add(
        new DisplayFrameButton(
            "Graphical CLI", iconSetPrefix + "command.gif", "CommandDisplayFrame"));

    toolbarPanel.add(Box.createHorizontalStrut(10));

    toolbarPanel.add(
        new DisplayFrameButton("KoLmafia Chat", iconSetPrefix + "chat.gif", "ChatManager"));
    toolbarPanel.add(
        new DisplayFrameButton("Clan Manager", iconSetPrefix + "clan.gif", "ClanManageFrame"));

    toolbarPanel.add(Box.createHorizontalStrut(10));

    toolbarPanel.add(
        new DisplayFrameButton("Daily Deeds", iconSetPrefix + "hp.gif", "CharSheetFrame"));
    toolbarPanel.add(
        new DisplayFrameButton("Item Manager", iconSetPrefix + "inventory.gif", "ItemManageFrame"));
    toolbarPanel.add(
        new DisplayFrameButton(
            "Equipment Manager", iconSetPrefix + "equipment.gif", "GearChangeFrame"));
    toolbarPanel.add(
        new DisplayFrameButton("Store Manager", iconSetPrefix + "mall.gif", "StoreManageFrame"));
    toolbarPanel.add(
        new DisplayFrameButton("Coin Masters", iconSetPrefix + "coin.gif", "CoinmastersFrame"));

    toolbarPanel.add(Box.createHorizontalStrut(10));

    toolbarPanel.add(
        new DisplayFrameButton("Purchase Buffs", iconSetPrefix + "buff.gif", "BuffRequestFrame"));
    toolbarPanel.add(
        new DisplayFrameButton(
            "Modifier Maximizer", iconSetPrefix + "uparrow.gif", "MaximizerFrame"));
    toolbarPanel.add(
        new DisplayFrameButton(
            "Sweet Synthesis", iconSetPrefix + "candypile.gif", "SynthesizeFrame"));
    toolbarPanel.add(
        new DisplayFrameButton(
            "Familiar Trainer", iconSetPrefix + "arena.gif", "FamiliarTrainingFrame"));

    toolbarPanel.add(Box.createHorizontalStrut(10));

    toolbarPanel.add(
        new DisplayFrameButton("Preferences", iconSetPrefix + "preferences.gif", "OptionsFrame"));

    toolbarPanel.add(Box.createHorizontalStrut(10));
    toolbarPanel.add(Box.createHorizontalGlue());

    this.memoryUsageLabel = new JProgressBar(JProgressBar.HORIZONTAL);
    this.memoryUsageLabel.setStringPainted(true);

    toolbarPanel.add(this.memoryUsageLabel);
    toolbarPanel.add(Box.createHorizontalStrut(10));
    toolbarPanel.add(
        new InvocationButton(
            "Collect Garbage", iconSetPrefix + "trashield.gif", KoLmafia.class, "gc"));

    toolbarPanel.add(Box.createHorizontalStrut(5));

    return toolbarPanel;
  }

  static final void removeExtraTabs() {
    if (KoLDesktop.INSTANCE == null) {
      return;
    }

    String setting = Preferences.getString("initialDesktop");
    for (int i = 0; i < KoLDesktop.INSTANCE.tabListing.size(); ++i) {
      GenericFrame frame = KoLDesktop.INSTANCE.tabListing.get(i);
      if (!(frame instanceof ChatFrame) && !setting.contains(frame.getFrameName())) {
        frame.dispose();
      }
    }
  }

  private class MinimizeListener extends WindowAdapter {
    @Override
    public void windowIconified(final WindowEvent e) {
      KoLDesktop.this.setVisible(false);
    }
  }

  private static class DisplayDesktopFocusRunnable implements Runnable {
    @Override
    public void run() {
      KoLDesktop.getInstance().setVisible(true);
      KoLDesktop.getInstance().requestFocus();
    }
  }

  private class MemoryUsageMonitor extends Thread {
    private final PauseObject pauser;

    public MemoryUsageMonitor() {
      this.pauser = new PauseObject();

      this.setDaemon(true);
    }

    @Override
    public void run() {
      while (KoLDesktop.INSTANCE == KoLDesktop.this) {
        this.pauser.pause(2000);

        Runtime runtime = Runtime.getRuntime();

        int maxMemory = (int) (runtime.maxMemory() >> 10);
        int heapMemory = (int) (runtime.totalMemory() >> 10);
        int usedMemory = (int) (heapMemory - (runtime.freeMemory() >> 10));

        KoLDesktop.this.memoryUsageLabel.setMaximum(maxMemory);
        KoLDesktop.this.memoryUsageLabel.setValue(usedMemory);

        KoLDesktop.this.memoryUsageLabel.setString(
            usedMemory / 1024 + " MB / " + maxMemory / 1024 + " MB");
      }
    }
  }
}
