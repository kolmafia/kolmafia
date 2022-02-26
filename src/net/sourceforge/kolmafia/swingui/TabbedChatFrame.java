package net.sourceforge.kolmafia.swingui;

import com.sun.java.forums.CloseableTabbedPane;
import com.sun.java.forums.CloseableTabbedPaneListener;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.chat.ChatManager;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.swingui.listener.TabFocusingListener;
import net.sourceforge.kolmafia.swingui.panel.CommandDisplayPanel;

public class TabbedChatFrame extends ChatFrame implements CloseableTabbedPaneListener {
  public TabbedChatFrame() {
    super(null);

    this.setTitle("Loathing Chat");

    if (Preferences.getBoolean("addChatCommandLine")) {
      this.tabs.addTab("[gcli]", new CommandDisplayPanel());
    }

    this.tabs.addChangeListener(new TabFocusingListener());
  }

  @Override
  public JTabbedPane getTabbedPane() {
    return new CloseableTabbedPane();
  }

  /**
   * Utility method called to initialize the frame. This method should be overridden, should a
   * different means of initializing the content of the frame be needed.
   */
  @Override
  public void initialize(final String associatedContact) {
    ((CloseableTabbedPane) this.tabs).addCloseableTabbedPaneListener(this);
    this.setCenterComponent(this.tabs);
  }

  @Override
  public boolean closeTab(final int tabIndexToClose) {
    if (tabIndexToClose == -1) {
      return false;
    }

    String closedTab = this.tabs.getTitleAt(tabIndexToClose);

    RequestThread.runInParallel(new CloseWindowRunnable(closedTab));

    return true;
  }

  /**
   * Adds a new tab to represent the given name. Note that this will not shift tab focus; however,
   * if it is the first tab added, the name of the contact will be reset.
   */
  public void addTab(final String tabName) {
    for (int i = 0; i < this.tabs.getTabCount(); ++i) {
      if (this.tabs.getTitleAt(i).trim().equals(tabName)) {
        return;
      }
    }

    try {
      TabAdder add = new TabAdder(tabName);

      if (SwingUtilities.isEventDispatchThread()) {
        add.run();
      } else {
        SwingUtilities.invokeAndWait(add);
      }
    } catch (Exception e) {
      // This should not happen.  However, skip it
      // since nothing bad really happened.
    }
  }

  public void highlightTab(final String tabName) {
    if (tabName == null) {
      return;
    }

    for (int i = 0; i < this.tabs.getTabCount(); ++i) {
      if (tabName.equals(this.tabs.getTitleAt(i).trim())) {
        SwingUtilities.invokeLater(new TabHighlighter(i));
        return;
      }
    }
  }

  private class TabAdder implements Runnable {
    private final String tabName;

    private TabAdder(final String tabName) {
      this.tabName = tabName;
    }

    @Override
    public void run() {
      JTabbedPane tabs = TabbedChatFrame.this.tabs;
      ChatPanel createdPanel = new ChatPanel(this.tabName);

      int tabOrder = this.getTabOrder(this.tabName);

      int tabCount = tabs.getTabCount();
      int tabIndex = tabCount;

      for (int i = 0; i < tabCount; ++i) {
        String currentTabName = tabs.getTitleAt(i).trim();

        int currentTabOrder = this.getTabOrder(currentTabName);

        if (tabOrder < currentTabOrder) {
          tabIndex = i;
          break;
        }
      }

      tabs.insertTab(this.tabName, null, createdPanel, "", tabIndex);
    }

    private int getTabOrder(final String tabName) {
      if (tabName.startsWith("[")) {
        return 2;
      }

      if (tabName.startsWith("/")) {
        return 0;
      }

      return 1;
    }
  }

  private class TabHighlighter implements Runnable {
    private final int tabIndex;

    public TabHighlighter(final int tabIndex) {
      this.tabIndex = tabIndex;
    }

    @Override
    public void run() {
      if (TabbedChatFrame.this.tabs.getSelectedIndex() == this.tabIndex) {
        return;
      }

      ((CloseableTabbedPane) TabbedChatFrame.this.tabs).highlightTab(this.tabIndex);
    }
  }

  private static class CloseWindowRunnable implements Runnable {
    private final String closedTab;

    public CloseWindowRunnable(String closedTab) {
      this.closedTab = closedTab;
    }

    @Override
    public void run() {
      ChatManager.closeWindow(this.closedTab);
    }
  }
}
