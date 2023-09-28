package net.sourceforge.kolmafia;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

import java.util.List;
import net.sourceforge.kolmafia.objectpool.EffectPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

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

  @Nested
  class ParseEffectString {
    @ParameterizedTest
    @CsvSource({
      "Sleepy (5), " + EffectPool.SLEEPY + ", 5",
      "Sleepy, " + EffectPool.SLEEPY + ", 0",
      "Educated (Kinda), " + EffectPool.EDUCATED_KINDA + ", 0",
    })
    void canParseEffect(final String effectString, final int effectId, final int count) {
      var result = AdventureResult.parseEffectString(effectString);
      assertThat(result, notNullValue());
      assertThat(result.getEffectId(), is(effectId));
      assertThat(result.getCount(), is(count));
    }

    @Test
    void cannotParseUnknownEffect() {
      var result = AdventureResult.parseEffectString("Peepy (5), Sleepy (forever)");
      assertThat(result, nullValue());
    }
  }

  @Nested
  class ParseItemString {
    @ParameterizedTest
    @CsvSource({
      "toast (5), " + ItemPool.TOAST + ", 5",
      "toast, " + ItemPool.TOAST + ", 1",
      "filet of tangy gnat (&quot;fotelif&quot;), " + ItemPool.FILET_OF_TANGY_GNAT + ", 1",
    })
    void canParseItem(final String itemString, final int itemId, final int count) {
      var result = AdventureResult.parseItemString(itemString);
      assertThat(result, notNullValue());
      assertThat(result.getItemId(), is(itemId));
      assertThat(result.getCount(), is(count));
    }

    @ParameterizedTest
    @ValueSource(strings = {"toast (infinite)", "troast (5)"})
    void cannotParseUnknownItem(final String itemString) {
      var result = AdventureResult.parseItemString(itemString);
      assertThat(result, nullValue());
    }
  }
}
