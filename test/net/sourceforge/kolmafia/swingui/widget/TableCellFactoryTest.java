package net.sourceforge.kolmafia.swingui.widget;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import net.java.dev.spellcast.utilities.SortedListModel;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import org.junit.jupiter.api.Test;

class TableCellFactoryTest {
  private String getType(final String itemName) {
    int itemId = ItemDatabase.getItemId(itemName);
    AdventureResult item = ItemPool.get(itemId, 1);
    var model = new SortedListModel<AdventureResult>();

    return (String) TableCellFactory.get(7, model, item, new boolean[] {false, false}, false, true);
  }

  @Test
  void displaysNormalizedTypesForRepresentativeEdgeCases() {
    // Representative edge cases for the normalized, player-facing type mapping.
    assertThat(getType("11-leaf clover"), is("usable"));
    assertThat(getType("seal tooth"), is("combat item"));
    assertThat(getType("abstraction: action"), is("spleen item"));
    assertThat(getType("shock collar"), is("familiar equipment"));
    assertThat(getType("halibut"), is("weapon"));
    assertThat(getType("makeshift cape"), is("back item"));
    assertThat(getType("33398 scroll"), is("combat item"));
  }
}
