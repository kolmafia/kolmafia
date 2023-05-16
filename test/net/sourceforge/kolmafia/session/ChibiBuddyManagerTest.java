package net.sourceforge.kolmafia.session;

import static internal.helpers.Networking.html;
import static internal.helpers.Player.withChoice;
import static internal.helpers.Player.withDaycount;
import static internal.helpers.Player.withHandlingChoice;
import static internal.helpers.Player.withHttpClientBuilder;
import static internal.helpers.Player.withItem;
import static internal.helpers.Player.withProperty;
import static internal.helpers.Player.withoutItem;
import static internal.matchers.Item.isInInventory;
import static internal.matchers.Preference.isSetTo;
import static org.hamcrest.MatcherAssert.assertThat;

import internal.helpers.Cleanups;
import internal.network.FakeHttpClientBuilder;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.request.GenericRequest;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class ChibiBuddyManagerTest {
  @BeforeAll
  public static void beforeAll() {
    KoLCharacter.reset("ChibiBuddyManagerTest");
  }

  @Test
  void canPowerOnChibiBudyy() {
    var builder = new FakeHttpClientBuilder();
    var cleanups =
        new Cleanups(
            withHttpClientBuilder(builder),
            withProperty("_chibiChanged"),
            withProperty("chibiName"),
            withItem(ItemPool.CHIBIBUDDY_OFF),
            withoutItem(ItemPool.CHIBIBUDDY_ON),
            withHandlingChoice(633));
    try (cleanups) {
      builder.client.addResponse(200, html("request/test_chibibuddy_power_on.html"));

      String urlString = "choice.php?pwd&whichchoice=633&option=1&chibiname=maurice";
      var choice = new GenericRequest(urlString);
      choice.run();

      assertThat("chibiName", isSetTo("maurice"));
      assertThat(ItemPool.CHIBIBUDDY_ON, isInInventory());
      assertThat(ItemPool.CHIBIBUDDY_OFF, isInInventory(0));
    }
  }

  @Test
  void canTrackChibiChange() {
    var cleanups =
        new Cleanups(
            withProperty("_chibiChanged"),
            withChoice(627, 5, html("request/test_chibibuddy_acquire_changed.html")));
    try (cleanups) {
      assertThat("_chibiChanged", isSetTo(true));
    }
  }

  @ParameterizedTest
  @CsvSource({"already_changed, true", "main_screen, false"})
  void canAdaptToChibiChangedDesync(final String fixture, final boolean alreadyChanged) {
    var cleanups =
        new Cleanups(
            withProperty("_chibiChanged", !alreadyChanged),
            withChoice(627, html("request/test_chibibuddy_" + fixture + ".html")));
    try (cleanups) {
      assertThat("_chibiChanged", isSetTo(alreadyChanged));
    }
  }

  @Test
  void canTrackChibiAdventures() {
    var cleanups =
        new Cleanups(
            withProperty("_chibiAdventures", 1),
            withProperty("_chibiChanged"),
            withChoice(629, 1, html("request/test_chibibuddy_spend_adventure_entertaining.html")));
    try (cleanups) {
      assertThat("_chibiAdventures", isSetTo(2));
    }
  }

  @Test
  void canTrackStats() {
    var cleanups =
        new Cleanups(
            withProperty("chibiAlignment"),
            withProperty("chibiFitness"),
            withProperty("chibiIntelligence"),
            withProperty("chibiSocialization"),
            withChoice(628, html("request/test_chibibuddy_wine.html")));
    try (cleanups) {
      assertThat("chibiAlignment", isSetTo(4));
      assertThat("chibiIntelligence", isSetTo(5));
      assertThat("chibiFitness", isSetTo(6));
      assertThat("chibiSocialization", isSetTo(4));
    }
  }

  @ParameterizedTest
  @ValueSource(ints = {2, 3})
  void canUpdateBirthday(final int age) {
    var day = 10;
    var cleanups =
        new Cleanups(
            withProperty("chibiBirthday", -1),
            withProperty("chibiLastVisit", -1),
            withDaycount(day),
            withChoice(628, html("request/test_chibibuddy_" + age + "_days_old.html")));
    try (cleanups) {
      assertThat("chibiBirthday", isSetTo(day - age));
      assertThat("chibiLastVisit", isSetTo(day));
    }
  }
}
