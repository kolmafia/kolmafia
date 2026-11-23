package net.sourceforge.kolmafia.swingui.panel;

import java.awt.Container;
import java.util.ArrayList;
import java.util.Iterator;
import net.java.dev.spellcast.utilities.ActionPanel;

/** The panels whose status message and enabled state follow KoLmafia's display state. */
public class ActionPanelRegistry {
  public static final ArrayList<ActionPanel> existingPanels = new ArrayList<>();
  private static ActionPanel[] panelArray = new GenericPanel[0];

  private ActionPanelRegistry() {}

  public static final void registerPanel(final ActionPanel panel) {
    synchronized (ActionPanelRegistry.existingPanels) {
      ActionPanelRegistry.existingPanels.add(panel);
      ActionPanelRegistry.getExistingPanels();
    }
  }

  public static final void unregisterPanel(final ActionPanel panel) {
    synchronized (ActionPanelRegistry.existingPanels) {
      ActionPanelRegistry.existingPanels.remove(panel);
      ActionPanelRegistry.getExistingPanels();
    }
  }

  public static final void unregisterPanels(final Container container) {
    boolean removedPanel = false;

    synchronized (ActionPanelRegistry.existingPanels) {
      Iterator<ActionPanel> panelIterator = ActionPanelRegistry.existingPanels.iterator();

      while (panelIterator.hasNext()) {
        ActionPanel panel = panelIterator.next();

        if (container.isAncestorOf(panel)) {
          panel.dispose();
          panelIterator.remove();
          removedPanel = true;
        }
      }
    }

    if (removedPanel) {
      ActionPanelRegistry.getExistingPanels();
    }
  }

  public static final ActionPanel[] getExistingPanels() {
    synchronized (ActionPanelRegistry.existingPanels) {
      boolean needsRefresh =
          ActionPanelRegistry.panelArray.length != ActionPanelRegistry.existingPanels.size();

      if (!needsRefresh) {
        for (int i = 0; i < ActionPanelRegistry.panelArray.length && !needsRefresh; ++i) {
          needsRefresh =
              ActionPanelRegistry.panelArray[i] != ActionPanelRegistry.existingPanels.get(i);
        }
      }

      if (needsRefresh) {
        ActionPanelRegistry.panelArray = new ActionPanel[ActionPanelRegistry.existingPanels.size()];
        ActionPanelRegistry.existingPanels.toArray(ActionPanelRegistry.panelArray);
      }

      return ActionPanelRegistry.panelArray;
    }
  }
}
