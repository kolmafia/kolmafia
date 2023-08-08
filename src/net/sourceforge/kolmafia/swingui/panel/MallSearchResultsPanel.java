package net.sourceforge.kolmafia.swingui.panel;

import java.awt.BorderLayout;
import java.awt.Color;
import javax.swing.JPanel;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import net.java.dev.spellcast.utilities.JComponentUtilities;
import net.java.dev.spellcast.utilities.LockableListModel;
import net.java.dev.spellcast.utilities.LockableListModel.ListElementFilter;
import net.sourceforge.kolmafia.listener.Listener;
import net.sourceforge.kolmafia.listener.NamedListenerRegistry;
import net.sourceforge.kolmafia.listener.PreferenceListenerRegistry;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.MallPurchaseRequest;
import net.sourceforge.kolmafia.request.PurchaseRequest;
import net.sourceforge.kolmafia.swingui.widget.GenericScrollPane;
import net.sourceforge.kolmafia.swingui.widget.ShowDescriptionList;

/**
 * A class which represents the panel used for tallying the results of the mall search request. Note
 * that all of the tallying functionality is handled by the <code>LockableListModel
 * </code> provided, so this functions as a container for that list model.
 */
public class MallSearchResultsPanel extends JPanel {

  private final LockableListModel<PurchaseRequest> results;
  private final ShowDescriptionList<PurchaseRequest> resultsList;

  public MallSearchResultsPanel(LockableListModel<PurchaseRequest> results) {
    super(new BorderLayout());

    JPanel resultsPanel = new JPanel(new BorderLayout());
    resultsPanel.add(
        JComponentUtilities.createLabel(
            "Search Results", SwingConstants.CENTER, Color.black, Color.white),
        BorderLayout.NORTH);

    this.results = results;
    this.resultsList = new ShowDescriptionList<>(results, new MallShopFilter(results));
    resultsList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
    ((ShowDescriptionList) resultsList).setPrototypeCellValue("ABCDEFGHIJKLMNOPQRSTUVWXYZ");
    resultsList.setVisibleRowCount(11);

    this.add(new GenericScrollPane(resultsList), BorderLayout.CENTER);
  }

  public ShowDescriptionList<PurchaseRequest> getResultsList() {
    return this.resultsList;
  }

  private static class MallShopFilter implements ListElementFilter, Listener {
    private final LockableListModel<PurchaseRequest> results;
    private boolean showForbidden = Preferences.getBoolean("showForbiddenStores");
    private boolean showIgnoring = Preferences.getBoolean("showIgnoringStorePrices");

    public MallShopFilter(LockableListModel<PurchaseRequest> results) {
      this.results = results;
      NamedListenerRegistry.registerNamedListener("(disabledStore)", this);
      NamedListenerRegistry.registerNamedListener("(ignoringStore)", this);
      PreferenceListenerRegistry.registerPreferenceListener("showIgnoringStorePrices", this);
      PreferenceListenerRegistry.registerPreferenceListener("forbiddenStores", this);
      PreferenceListenerRegistry.registerPreferenceListener("showForbiddenStores", this);
    }

    // Listener
    public void update() {
      this.showForbidden = Preferences.getBoolean("showForbiddenStores");
      this.showIgnoring = Preferences.getBoolean("showIgnoringStorePrices");
      this.results.updateFilter(true);
    }

    // ListElementFilter
    public boolean isVisible(final Object element) {
      if (element instanceof MallPurchaseRequest mpr) {
        int shopId = mpr.getShopId();
        boolean forbidden = mpr.isForbidden(shopId);
        boolean ignoring = mpr.isIgnoring(shopId);
        if (!forbidden && !ignoring) {
          return true;
        }
        if (forbidden && !showForbidden) {
          return false;
        }
        if (ignoring && !showIgnoring) {
          return false;
        }
      }

      return true;
    }
  }
}
