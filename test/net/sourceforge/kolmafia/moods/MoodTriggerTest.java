package net.sourceforge.kolmafia.moods;

import static internal.helpers.HttpClientWrapper.getRequests;
import static internal.helpers.Networking.assertGetRequest;
import static internal.helpers.Networking.assertPostRequest;
import static internal.helpers.Player.*;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import internal.helpers.Cleanups;
import internal.helpers.HttpClientWrapper;
import net.sourceforge.kolmafia.KoLCharacter;
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
  void shouldCastForcedItemEffects() {
    var cleanups =
        new Cleanups(
            withSkill(SkillPool.MANICOTTI_MEDITATION),
            withMP(10, 10, 10),
            withItem(ItemPool.APRIL_SHOWER_THOUGHTS_SHIELD));

    try (cleanups) {
      var trigger =
          MoodTrigger.constructNode(
              "lose_effect tubes of universal meat => cast 1 Manicotti Meditation ^ Tubes of Universal Meat");
      assertThat(trigger, not(nullValue()));
      trigger.execute(1);

      var requests = getRequests();
      assertPostRequest(
          requests.get(0),
          "/inv_equip.php",
          "which=2&ajax=1&action=equip&whichitem=" + ItemPool.APRIL_SHOWER_THOUGHTS_SHIELD);
      assertGetRequest(
          requests.get(1), "/runskillz.php", "action=Skillz&whichskill=3000&ajax=1&quantity=1");
    }
  }
}
