package net.sourceforge.kolmafia.persistence;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.is;

import java.util.Map;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.AscensionClass;
import net.sourceforge.kolmafia.AscensionPath.Path;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import org.junit.jupiter.api.Test;

public class PortableLaughingStockDatabaseTest {
  @Test
  void fruitListsAreValid() {
    assertThat(PortableLaughingStockDatabase.BASIC_FRUIT.size(), is(19));
    for (AdventureResult fruit : PortableLaughingStockDatabase.BASIC_FRUIT) {
      assertThat(fruit.getItemId(), greaterThan(0));
    }

    assertThat(PortableLaughingStockDatabase.ADVANCED_FRUIT.size(), is(3));
    for (AdventureResult fruit : PortableLaughingStockDatabase.ADVANCED_FRUIT) {
      assertThat(fruit.getItemId(), greaterThan(0));
    }
  }

  @Test
  void generatesCorrectDrops() {
    Map<Integer, AdventureResult> drops =
        PortableLaughingStockDatabase.getLaughingStockDrops(
            AscensionClass.DISCO_BANDIT, Path.STANDARD, 3, 11, 183);

    assertThat(drops.size(), is(8));
    assertThat(drops, hasEntry(11, ItemPool.get("antique watermelon")));
    assertThat(drops, hasEntry(16, ItemPool.get("pear")));
    assertThat(drops, hasEntry(22, ItemPool.get("lime")));
    assertThat(drops, hasEntry(29, ItemPool.get("quince")));
    assertThat(drops, hasEntry(37, ItemPool.get("cactus fruit")));
    assertThat(drops, hasEntry(46, ItemPool.get("quince")));
    assertThat(drops, hasEntry(56, ItemPool.get("raspberry")));
    assertThat(drops, hasEntry(183, ItemPool.get("blackberry")));
  }

  @Test
  void doesNotPredictDayFive() {
    Map<Integer, AdventureResult> drops =
        PortableLaughingStockDatabase.getLaughingStockDrops(
            AscensionClass.DISCO_BANDIT, Path.STANDARD, 5, 11, 183);
    assertThat(drops.size(), is(0));
  }
}
