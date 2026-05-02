package net.sourceforge.kolmafia.persistence;

import static internal.helpers.Networking.html;
import static internal.helpers.Player.withNextResponse;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.endsWith;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;

import internal.helpers.Cleanups;
import internal.helpers.RequestLoggerOutput;
import net.sourceforge.kolmafia.objectpool.EffectPool;
import org.junit.jupiter.api.Test;

public class EffectDatabaseTest {
  @Test
  void returnsExpectedFieldsForKnownStatusEffect() {
    int effectId = EffectPool.LEASH_OF_LINGUINI;

    assertThat(EffectDatabase.getEffectName(effectId), is("Leash of Linguini"));
    assertThat(EffectDatabase.getImage(effectId), endsWith("/string.gif"));
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
}
