package net.sourceforge.kolmafia.moods;

import static internal.helpers.Networking.assertGetRequest;
import static internal.helpers.Networking.assertPostRequest;
import static internal.helpers.Networking.getPostRequestBody;
import static internal.helpers.Networking.html;
import static internal.helpers.Player.*;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import internal.helpers.Cleanups;
import internal.helpers.HttpClientWrapper;
import internal.helpers.RequestLoggerOutput;
import internal.network.FakeHttpClientBuilder;
import internal.network.FakeHttpResponse;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.StaticEntity;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.objectpool.SkillPool;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class MoodTriggerTest {
  @BeforeEach
  public void initializeCharPrefs() {
    KoLCharacter.reset("MoodTriggerTest");
    HttpClientWrapper.setupFakeClient();
  }

  @Test
  void failedCastReturnsError() {
    RequestLoggerOutput.startStream();
    var cleanups = new Cleanups(withSkill(SkillPool.EMPATHY_OF_THE_NEWT), withContinuationState());

    try (cleanups) {
      var trigger =
          MoodTrigger.constructNode("lose_effect empathy => cast 1 Empathy of the Newt ^ Empathy");
      assertThat(trigger, not(nullValue()));
      trigger.execute(1);

      assertEquals(MafiaState.ABORT, StaticEntity.getContinuationState());
      var text = RequestLoggerOutput.stopStream();
      assertThat(text, containsString("Mood failed to cast"));
    }
  }

  @Test
  void shouldCastForcedItemEffects() {
    var builder = new FakeHttpClientBuilder();
    builder.client.setResponseFunc(
        r -> {
          var path = r.uri().getPath();
          if (path.startsWith("/runskillz.php")) {
            return new FakeHttpResponse<>("You meditate. Magicalness courses through your veins.");
          }
          if (path.startsWith("/api.php")) {
            var body = getPostRequestBody(r);
            if (body.startsWith("what=status")) {
              return new FakeHttpResponse<>(html("request/test_status.json"));
            }
          }
          return null;
        });

    var cleanups =
        new Cleanups(
            withSkill(SkillPool.MANICOTTI_MEDITATION),
            withMP(10, 10, 10),
            withItem(ItemPool.APRIL_SHOWER_THOUGHTS_SHIELD),
            withHttpClientBuilder(builder));

    try (cleanups) {
      var trigger =
          MoodTrigger.constructNode(
              "lose_effect tubes of universal meat => cast 1 Manicotti Meditation ^ Tubes of Universal Meat");
      assertThat(trigger, not(nullValue()));
      trigger.execute(1);

      var requests = builder.client.getRequests();
      assertPostRequest(
          requests.get(0),
          "/inv_equip.php",
          "which=2&ajax=1&action=equip&whichitem=" + ItemPool.APRIL_SHOWER_THOUGHTS_SHIELD);
      assertGetRequest(
          requests.get(1), "/runskillz.php", "action=Skillz&whichskill=3000&ajax=1&quantity=1");
    }
  }
}
