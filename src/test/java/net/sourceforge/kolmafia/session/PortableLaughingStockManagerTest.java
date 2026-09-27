package net.sourceforge.kolmafia.session;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.is;

import java.util.Map;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.AscensionClass;
import net.sourceforge.kolmafia.AscensionPath.Path;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import org.junit.jupiter.api.Test;

public class PortableLaughingStockManagerTest {
  @Test
  void generatesCorrectDrops() {
    Map<Integer, AdventureResult> drops =
        PortableLaughingStockManager.getLaughingStockDrops(
            AscensionClass.DISCO_BANDIT, Path.STANDARD, 3, 11, 183);

    assertThat(drops.size(), is(8));
    assertThat(drops, hasEntry(11, ItemPool.get(ItemPool.ANTIQUE_WATERMELON)));
    assertThat(drops, hasEntry(16, ItemPool.get(ItemPool.PEAR)));
    assertThat(drops, hasEntry(22, ItemPool.get(ItemPool.LIME)));
    assertThat(drops, hasEntry(29, ItemPool.get(ItemPool.QUINCE)));
    assertThat(drops, hasEntry(37, ItemPool.get(ItemPool.CACTUS_FRUIT)));
    assertThat(drops, hasEntry(46, ItemPool.get(ItemPool.QUINCE)));
    assertThat(drops, hasEntry(56, ItemPool.get(ItemPool.RASPBERRY)));
    assertThat(drops, hasEntry(183, ItemPool.get(ItemPool.BLACKBERRY)));
  }

  @Test
  void doesNotPredictDayFive() {
    Map<Integer, AdventureResult> drops =
        PortableLaughingStockManager.getLaughingStockDrops(
            AscensionClass.DISCO_BANDIT, Path.STANDARD, 5, 11, 183);
    assertThat(drops.size(), is(0));
  }
}
