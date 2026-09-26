package net.sourceforge.kolmafia.session;

import static internal.helpers.Networking.assertPostRequest;
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
import static org.hamcrest.Matchers.is;

import internal.helpers.Cleanups;
import internal.network.FakeHttpClientBuilder;
import java.util.List;
import java.util.Map;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.objectpool.EffectPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.request.GenericRequest;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class ChibiBuddyManagerTest {
  @BeforeAll
  public static void beforeAll() {
    KoLCharacter.reset("ChibiBuddyManagerTest");
  }

  @Nested
  class Tracking {
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
              withChoice(
                  629, 1, html("request/test_chibibuddy_spend_adventure_entertaining.html")));
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

    @ParameterizedTest
    @ValueSource(strings = {"start_of_day_death", "left_too_long"})
    void canDie(final String fileName) {
      var day = 10;
      var cleanups =
          new Cleanups(
              withItem(ItemPool.CHIBIBUDDY_ON),
              withoutItem(ItemPool.CHIBIBUDDY_OFF),
              withProperty("chibiBirthday", day - 2),
              withProperty("chibiLastVisit", day),
              withProperty("chibiName", "maurice"),
              withProperty("chibiAlignment", 6),
              withProperty("chibiFitness", 4),
              withProperty("chibiIntelligence", 6),
              withProperty("chibiSocialization", 4),
              withDaycount(day),
              withChoice(628, html("request/test_chibibuddy_" + fileName + ".html")));
      try (cleanups) {
        assertThat("chibiBirthday", isSetTo(-1));
        assertThat("chibiLastVisit", isSetTo(-1));
        assertThat("chibiName", isSetTo(""));
        assertThat("chibiAlignment", isSetTo(0));
        assertThat("chibiFitness", isSetTo(0));
        assertThat("chibiIntelligence", isSetTo(0));
        assertThat("chibiSocialization", isSetTo(0));

        assertThat(ItemPool.CHIBIBUDDY_ON, isInInventory(0));
        assertThat(ItemPool.CHIBIBUDDY_OFF, isInInventory());
      }
    }
  }

  @Nested
  class Control {
    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void handlesFindingDeadChibi(final boolean hasContacts) {
      var builder = new FakeHttpClientBuilder();

      builder.client.addResponse(302, Map.of("location", List.of("choice.php")), "");
      builder.client.addResponse(200, html("request/test_chibibuddy_start_of_day_death.html"));
      builder.client.addResponse(200, ""); // API
      builder.client.addResponse(200, "request/test_chibibuddy_put_away.html");
      builder.client.addResponse(302, Map.of("location", List.of("choice.php")), "");
      builder.client.addResponse(200, html("request/test_chibibuddy_name.html"));
      builder.client.addResponse(200, ""); // API
      builder.client.addResponse(200, html("request/test_chibibuddy_main_screen.html"));
      builder.client.addResponse(200, ""); // API
      builder.client.addResponse(200, "request/test_chibibuddy_put_away.html");

      if (hasContacts) {
        ContactManager.addMailContact("gausie", "1197090");
      }

      var cleanups =
          new Cleanups(
              withHttpClientBuilder(builder),
              withProperty("_chibiChanged"),
              withProperty("chibiName", "old name"),
              withProperty("chibiBirthday", 40),
              withProperty("chibiLastVisit", 49),
              withProperty("chibiAlignment", 5),
              withProperty("chibiFitness", 5),
              withProperty("chibiIntelligence", 5),
              withProperty("chibiSocialization", 5),
              withDaycount(50),
              withItem(ItemPool.CHIBIBUDDY_ON),
              withoutItem(ItemPool.CHIBIBUDDY_OFF));

      try (cleanups) {
        ChibiBuddyManager.ensureLiveChibi();

        var requests = builder.client.getRequests();

        var expectedName = "Li'l " + (hasContacts ? "gausie" : "ChibiBuddyManagerTest");

        assertPostRequest(
            requests.get(7), "/choice.php", "whichchoice=633&option=1&chibiname=" + expectedName);

        assertThat(ItemPool.CHIBIBUDDY_ON, isInInventory());
        assertThat(ItemPool.CHIBIBUDDY_OFF, isInInventory(0));

        assertThat("chibiBirthday", isSetTo(50));
        assertThat("chibiLastVisit", isSetTo(50));
      }

      ContactManager.reset();
    }

    @Test
    void handlesChibiFromPoweredOff() {
      var builder = new FakeHttpClientBuilder();

      builder.client.addResponse(302, Map.of("location", List.of("choice.php")), "");
      builder.client.addResponse(200, html("request/test_chibibuddy_name.html"));
      builder.client.addResponse(200, ""); // API
      builder.client.addResponse(200, html("request/test_chibibuddy_main_screen.html"));
      builder.client.addResponse(200, ""); // API
      builder.client.addResponse(200, "request/test_chibibuddy_put_away.html");

      var cleanups =
          new Cleanups(
              withHttpClientBuilder(builder),
              withProperty("choiceAdventure633", "1&chibiname=customname"),
              withDaycount(69),
              withProperty("_chibiChanged"),
              withProperty("chibiName"),
              withProperty("chibiBirthday"),
              withProperty("chibiLastVisit"),
              withProperty("chibiAlignment"),
              withProperty("chibiFitness"),
              withProperty("chibiIntelligence"),
              withProperty("chibiSocialization"),
              withItem(ItemPool.CHIBIBUDDY_OFF),
              withoutItem(ItemPool.CHIBIBUDDY_ON));

      try (cleanups) {
        ChibiBuddyManager.ensureLiveChibi();

        var requests = builder.client.getRequests();

        assertPostRequest(
            requests.get(3), "/choice.php", "whichchoice=633&option=1&chibiname=customname");

        assertThat(ItemPool.CHIBIBUDDY_ON, isInInventory());
        assertThat(ItemPool.CHIBIBUDDY_OFF, isInInventory(0));

        assertThat("chibiBirthday", isSetTo(69));
        assertThat("chibiLastVisit", isSetTo(69));
      }
    }

    @Test
    void handlesLivingChibi() {
      var builder = new FakeHttpClientBuilder();

      builder.client.addResponse(302, Map.of("location", List.of("choice.php")), "");
      builder.client.addResponse(200, html("request/test_chibibuddy_main_screen.html"));
      builder.client.addResponse(200, ""); // API
      builder.client.addResponse(200, "request/test_chibibuddy_put_away.html");

      var cleanups =
          new Cleanups(
              withHttpClientBuilder(builder),
              withDaycount(69),
              withProperty("_chibiChanged"),
              withProperty("chibiName", "maurice"),
              withProperty("chibiBirthday", 65),
              withProperty("chibiLastVisit", 68),
              withProperty("chibiAlignment"),
              withProperty("chibiFitness"),
              withProperty("chibiIntelligence"),
              withProperty("chibiSocialization"),
              withItem(ItemPool.CHIBIBUDDY_ON),
              withoutItem(ItemPool.CHIBIBUDDY_OFF));

      try (cleanups) {
        ChibiBuddyManager.ensureLiveChibi();

        var requests = builder.client.getRequests();

        assertThat(ItemPool.CHIBIBUDDY_ON, isInInventory());
        assertThat(ItemPool.CHIBIBUDDY_OFF, isInInventory(0));

        assertThat("chibiBirthday", isSetTo(65));
        assertThat("chibiLastVisit", isSetTo(69));
      }
    }

    @Test
    void chatsWithChibi() {
      var builder = new FakeHttpClientBuilder();

      builder.client.addResponse(302, Map.of("location", List.of("choice.php")), "");
      builder.client.addResponse(200, html("request/test_chibibuddy_main_screen.html"));
      builder.client.addResponse(200, ""); // API
      builder.client.addResponse(200, html("request/test_chibibuddy_acquire_changed.html"));
      builder.client.addResponse(200, ""); // API
      builder.client.addResponse(200, "request/test_chibibuddy_put_away.html");

      var cleanups =
          new Cleanups(
              withHttpClientBuilder(builder),
              withDaycount(10),
              withProperty("_chibiChanged", false),
              withProperty("chibiName"),
              withProperty("chibiBirthday", 10),
              withProperty("chibiLastVisit", 10),
              withProperty("chibiAlignment"),
              withProperty("chibiFitness"),
              withProperty("chibiIntelligence"),
              withProperty("chibiSocialization"),
              withItem(ItemPool.CHIBIBUDDY_ON),
              withoutItem(ItemPool.CHIBIBUDDY_OFF));

      try (cleanups) {
        ChibiBuddyManager.chat();

        assertThat(ItemPool.CHIBIBUDDY_ON, isInInventory());
        assertThat(ItemPool.CHIBIBUDDY_OFF, isInInventory(0));

        assertThat("_chibiChanged", isSetTo(true));
        assertThat(
            KoLConstants.activeEffects.contains(EffectPool.get(EffectPool.CHIBICHANGED)), is(true));
      }
    }
  }
}
