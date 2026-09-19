package net.sourceforge.kolmafia.persistence;

import static internal.helpers.Networking.html;
import static internal.helpers.Player.withNextResponse;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.endsWith;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.hamcrest.Matchers.not;

import internal.helpers.Cleanups;
import internal.helpers.RequestLoggerOutput;
import net.sourceforge.kolmafia.objectpool.EffectPool;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class EffectDatabaseTest {
  @Test
  void returnsExpectedFieldsForKnownStatusEffect() {
    int effectId = EffectPool.LEASH_OF_LINGUINI;

    assertThat(EffectDatabase.getEffectName(effectId), is("Leash of Linguini"));
    assertThat(EffectDatabase.getImageName(effectId), is("string.gif"));
    assertThat(EffectDatabase.getImage(effectId), endsWith("/itemimages/string.gif"));
    assertThat(EffectDatabase.getDescriptionId(effectId), is("2d6d3ab04b40e1523aa9c716a04b3aab"));
    assertThat(EffectDatabase.getQualityDescription(effectId), is("good"));
    assertThat(EffectDatabase.getEffectAttributes(effectId), empty());
    assertThat(EffectDatabase.getActions(effectId), is("cast 1 Leash of Linguini"));
  }

  @Test
  void returnsMultipleAttributesIfPresent() {
    int effectId = EffectPool.EVERYTHING_LOOKS_YELLOW;

    assertThat(
        EffectDatabase.getEffectAttributes(effectId), contains("nohookah", "noremove", "nopvp"));
  }

  @Test
  void registerEffectPrintsExpectedDataLine() {
    var cleanups =
        new Cleanups(
            withNextResponse(200, html("request/test_desc_effect_buzzed_on_distillate.html")));

    try (cleanups) {
      RequestLoggerOutput.startStream();
      EffectDatabase.registerEffect(
          "Buzzed on Distillate", "d64eab33f648e1a77da23ae516353fb2", null);
      var output = RequestLoggerOutput.stopStream();

      assertThat(
          output,
          containsString(
              "2720\tBuzzed on Distillate\tchinsweat.gif\td64eab33f648e1a77da23ae516353fb2\tneutral\tnohookah\tdrink 1 stillsuit distillate"));
    }
  }

  @Nested
  class GoodEffects {
    @Test
    void runsFromTheFirstGoodEffectToTheGivenLatest() {
      var pool = EffectDatabase.getGoodEffects(EffectPool.TIKI_TEMERITY);

      assertThat(pool.get(0), is(EffectPool.FAR_OUT));
      assertThat(pool.get(pool.size() - 1), is(EffectPool.TIKI_TEMERITY));
    }

    @Test
    void stopsAtTheGivenLatestEffect() {
      var pool = EffectDatabase.getGoodEffects(EffectPool.TIKI_TEMERITY);

      assertThat(pool, everyItem(lessThanOrEqualTo(EffectPool.TIKI_TEMERITY)));
      assertThat(
          EffectDatabase.getGoodEffects(EffectPool.LIFTING_WETS), hasItem(EffectPool.FIZZY_FIZZY));
    }

    @Test
    void keepsFishyButNoOtherNohookahEffect() {
      var pool = EffectDatabase.getGoodEffects(EffectPool.TIKI_TEMERITY);

      assertThat(pool, hasItem(EffectPool.FISHY));
      assertThat(pool, not(hasItem(EffectPool.FISHTACULAR_VERNACULAR)));
    }
  }
}
