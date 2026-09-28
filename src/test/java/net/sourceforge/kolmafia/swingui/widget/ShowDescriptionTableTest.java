package net.sourceforge.kolmafia.swingui.widget;

import static internal.helpers.Player.withItem;
import static internal.helpers.Player.withProperty;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import internal.helpers.Cleanups;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import net.java.dev.spellcast.utilities.LockableListModel;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.preferences.Preferences;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class ShowDescriptionTableTest {
  @BeforeEach
  void beforeEach() {
    KoLCharacter.reset("ShowDescriptionTableTest");
    Preferences.reset("ShowDescriptionTableTest");
  }

  @Nested
  class EffectiveAutosellPrices {
    @ParameterizedTest
    @CsvSource({"true, 60", "false, 58"})
    void displaysAndFiltersAdjustedUnitPrices(boolean enabled, int expected) {
      try (var cleanups =
          new Cleanups(
              withProperty("autoSellingShorts", enabled), withItem(ItemPool.SELLING_SHORTS))) {
        var item = ItemPool.get(ItemPool.MOUNTAIN_STREAM_SODA, 2);
        var flags = new boolean[] {false, false};
        var model = new LockableListModel<AdventureResult>();
        assertThat(TableCellFactory.get(1, model, item, flags, false), is(expected + " meat"));
        assertThat(TableCellFactory.get(1, model, item, flags, false, true), is(expected));
        assertThat(AutoFilterTextField.getResultPrice(item), is(expected));
        var label =
            (JLabel)
                new ListCellRendererFactory.DefaultRenderer()
                    .getRenderer(new JLabel(), item, false);
        assertThat(label.getText(), containsString("(" + expected + " meat)"));
      }
    }

    @Test
    void preferenceChangesRefreshWithoutWindowFocus() throws Exception {
      try (var cleanups =
          new Cleanups(
              withProperty("autoSellingShorts", false), withItem(ItemPool.SELLING_SHORTS))) {
        var tableRef = new AtomicReference<ShowDescriptionTable<AdventureResult>>();
        var updates = new AtomicInteger();
        SwingUtilities.invokeAndWait(
            () -> {
              var model = new LockableListModel<AdventureResult>();
              model.add(ItemPool.get(ItemPool.MOUNTAIN_STREAM_SODA, 1));
              var table =
                  new ShowDescriptionTable<>(model, null, 4, 5, new boolean[] {true, false});
              table
                  .getModel()
                  .addTableModelListener(
                      event -> {
                        assertThat(SwingUtilities.isEventDispatchThread(), is(true));
                        updates.incrementAndGet();
                      });
              tableRef.set(table);
            });

        for (boolean enabled : new boolean[] {true, false}) {
          int before = updates.get();
          Preferences.setBoolean("autoSellingShorts", enabled);
          SwingUtilities.invokeAndWait(
              () -> {
                var table = tableRef.get();
                assertThat(table.isShowing(), is(false));
                assertThat(updates.get(), greaterThan(before));
                var label = (JLabel) table.prepareRenderer(table.getCellRenderer(0, 4), 0, 4);
                assertThat(label.getText(), is(enabled ? "60 meat" : "58 meat"));
              });
        }
      }
    }
  }
}
