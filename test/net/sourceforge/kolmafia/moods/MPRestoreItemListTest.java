package net.sourceforge.kolmafia.moods;

import static internal.helpers.Player.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.persistence.RestoresDatabase;
import org.junit.jupiter.api.Test;

public class MPRestoreItemListTest {
  @Test
  public void itemsHaveCorrectMana() {
    var cleanups = withLevel(13);
    try (cleanups) {
      MPRestoreItemList.updateManaRestored();
      for (var entry : ItemDatabase.entrySet()) {
        var itemId = entry.getKey();
        var name = entry.getValue();

        if (!RestoresDatabase.isRestore(itemId)
            || !MPRestoreItemList.contains(ItemPool.get(itemId, 1))) continue;

        // Skip items which don't currently restore any MP (e.g. delicious shimmering moth)
        if ((int) RestoresDatabase.getMPMin(name) == 0) continue;

        var expected =
            itemId == ItemPool.EXPRESS_CARD
                ? Integer.MAX_VALUE
                : (int) RestoresDatabase.getMPAverage(name);

        assertThat(
            "MP equal for [" + name + "]",
            MPRestoreItemList.getManaRestored(name),
            equalTo(expected));
      }
    }
  }
}
