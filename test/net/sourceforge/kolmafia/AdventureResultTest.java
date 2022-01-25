package net.sourceforge.kolmafia;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import java.util.List;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import org.junit.jupiter.api.Test;

public class AdventureResultTest {

  @Test
  public void getCountReturnsZeroIfNotInList() {
    var bean = AdventureResult.tallyItem("enchanted bean");
    var count = bean.getCount(List.of());
    assertThat(count, equalTo(0));
  }

  @Test
  public void getCountReturnsNumberIfInList() {
    var helm = ItemPool.get(ItemPool.SEAL_HELMET, 1);
    var count = helm.getCount(List.of(AdventureResult.tallyItem("seal-skull helmet", 5, true)));
    assertThat(count, equalTo(5));
  }
}
