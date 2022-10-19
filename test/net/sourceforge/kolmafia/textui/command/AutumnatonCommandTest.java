package net.sourceforge.kolmafia.textui.command;

import static internal.helpers.Networking.html;
import static internal.helpers.Player.withHttpClientBuilder;
import static internal.helpers.Player.withItem;
import static internal.helpers.Player.withNextResponse;
import static internal.helpers.Player.withProperty;
import static internal.helpers.Player.withTurnsPlayed;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;

import internal.helpers.Cleanups;
import internal.network.FakeHttpClientBuilder;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.session.ChoiceManager;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

public class AutumnatonCommandTest extends AbstractCommandTestBase {

  public AutumnatonCommandTest() {
    this.command = "autumnaton";
  }

  @BeforeAll
  public static void beforeAll() {
    KoLCharacter.reset("AutumnatonCommandTest");
    Preferences.reset("AutumnatonCommandTest");
    ChoiceManager.handlingChoice = false;
  }

  private Cleanups hasAutumnaton() {
    return withProperty("hasAutumnaton", true);
  }

  @Test
  void errorsWithNoAutumnaton() {
    String output = execute("");
    assertErrorState();
    assertThat(output, containsString("You need an autumn-aton"));
  }

  @Test
  void errorsWithInvalidCommand() {
    var cleanups = hasAutumnaton();

    try (cleanups) {
      String output = execute("frobnort");
      assertErrorState();
      assertThat(output, containsString("Usage: autumnaton <blank>"));
    }
  }

  @Nested
  class Status {
    @Test
    void noLocationNoAutumnaton() {
      var cleanups = new Cleanups(hasAutumnaton(), withProperty("autumnatonQuestLocation", ""));

      try (cleanups) {
        String output = execute("");
        assertThat(output, containsString("Your autumn-aton is in an unknown location."));
      }
    }

    @Test
    void noLocation() {
      var cleanups =
          new Cleanups(
              hasAutumnaton(),
              withItem(ItemPool.AUTUMNATON),
              withProperty("autumnatonQuestLocation", ""));

      try (cleanups) {
        String output = execute("");
        assertThat(output, containsString("Your autumn-aton is ready to be sent somewhere."));
      }
    }

    @Test
    void noLocationUpgradesAvailable() {
      var cleanups =
          new Cleanups(
              hasAutumnaton(),
              withItem(ItemPool.AUTUMNATON),
              withProperty("autumnatonQuestLocation", ""),
              withNextResponse(
                  200, html("request/test_choice_autumnaton_upgrade_available_one.html")));

      try (cleanups) {
        String output = execute("");
        assertThat(output, containsString("Your autumn-aton is ready to be sent somewhere."));
        assertThat(output, containsString("Your autumn-aton has upgrades available: dual exhaust"));
      }
    }

    @Test
    void locationZeroTurns() {
      var cleanups =
          new Cleanups(
              hasAutumnaton(),
              withItem(ItemPool.AUTUMNATON),
              withProperty("autumnatonQuestLocation", "The Deep Dark Jungle"),
              withTurnsPlayed(1),
              withProperty("autumnatonQuestTurn", 1));

      try (cleanups) {
        String output = execute("");
        assertThat(
            output, containsString("Your autumn-aton is plundering in The Deep Dark Jungle."));
        assertThat(output, containsString("Your autumn-aton will return after your next combat."));
      }
    }

    @Test
    void locationOneTurn() {
      var cleanups =
          new Cleanups(
              hasAutumnaton(),
              withItem(ItemPool.AUTUMNATON),
              withProperty("autumnatonQuestLocation", "The Deep Dark Jungle"),
              withTurnsPlayed(1),
              withProperty("autumnatonQuestTurn", 2));

      try (cleanups) {
        String output = execute("");
        assertThat(
            output, containsString("Your autumn-aton is plundering in The Deep Dark Jungle."));
        assertThat(output, containsString("Your autumn-aton will return after 1 turn."));
      }
    }

    @Test
    void locationManyTurns() {
      var cleanups =
          new Cleanups(
              hasAutumnaton(),
              withItem(ItemPool.AUTUMNATON),
              withProperty("autumnatonQuestLocation", "The Deep Dark Jungle"),
              withTurnsPlayed(1),
              withProperty("autumnatonQuestTurn", 12));

      try (cleanups) {
        String output = execute("");
        assertThat(
            output, containsString("Your autumn-aton is plundering in The Deep Dark Jungle."));
        assertThat(output, containsString("Your autumn-aton will return after 11 turns."));
      }
    }
  }

  @Nested
  class Send {
    @Test
    void errorsWithAbsentAutumnaton() {
      var cleanups = hasAutumnaton();

      try (cleanups) {
        String output = execute("send ");
        assertErrorState();
        assertThat(output, containsString("Your autumn-aton is away"));
      }
    }

    @Test
    void errorsWithNoLocation() {
      var cleanups = new Cleanups(hasAutumnaton(), withItem(ItemPool.AUTUMNATON));

      try (cleanups) {
        String output = execute("send");
        assertErrorState();
        assertThat(output, containsString("Where do you want to send the little guy?"));
      }
    }

    @Test
    void errorsWithOnlySpacesForLocation() {
      var cleanups = new Cleanups(hasAutumnaton(), withItem(ItemPool.AUTUMNATON));

      try (cleanups) {
        String output = execute("send   ");
        assertErrorState();
        assertThat(output, containsString("Where do you want to send the little guy?"));
      }
    }

    @Test
    void errorsWithBadLocation() {
      var cleanups = new Cleanups(hasAutumnaton(), withItem(ItemPool.AUTUMNATON));

      try (cleanups) {
        String output = execute("send trogdor");
        assertErrorState();
        assertThat(output, containsString("I don't understand where trogdor is."));
      }
    }

    @Test
    void errorsWithNonSnarfblatLocation() {
      var cleanups = new Cleanups(hasAutumnaton(), withItem(ItemPool.AUTUMNATON));

      try (cleanups) {
        String output = execute("send Tavern Cellar");
        assertErrorState();
        assertThat(output, containsString("The Typical Tavern Cellar is not a valid location"));
      }
    }

    @Test
    void succeedsWithAccessibleLocation() {
      var builder = new FakeHttpClientBuilder();
      builder.client.addResponse(200, html("request/test_choice_autumnaton_all_upgrades.html"));
      builder.client.addResponse(200, html("request/test_choice_autumnaton_quest_kitchen.html"));

      var cleanups =
          new Cleanups(
              hasAutumnaton(),
              withItem(ItemPool.AUTUMNATON),
              withHttpClientBuilder(builder),
              withProperty("autumnatonQuestLocation", ""));

      try (cleanups) {
        String output = execute("send noob cave");

        assertThat(output, containsString("Sending autumn-aton to Noob Cave"));
        assertThat(output, containsString("Sent autumn-aton to Noob Cave"));
        assertContinueState();
      }
    }

    @Test
    void failsWithInaccessibleLocation() {
      var builder = new FakeHttpClientBuilder();
      builder.client.addResponse(200, html("request/test_choice_autumnaton_all_upgrades.html"));
      builder.client.addResponse(200, html("request/test_choice_autumnaton_quest_fail.html"));

      var cleanups =
          new Cleanups(
              hasAutumnaton(),
              withItem(ItemPool.AUTUMNATON),
              withHttpClientBuilder(builder),
              withProperty("autumnatonQuestLocation", ""));

      try (cleanups) {
        String output = execute("send Hobopolis Town Square");

        assertThat(output, containsString("Sending autumn-aton to Hobopolis Town Square"));
        assertThat(
            output,
            containsString(
                "Failed to send autumnaton to Hobopolis Town Square. Is it accessible?"));
        assertErrorState();
      }
    }
  }

  @Nested
  class Upgrade {
    @Test
    public void errorsIfNoUpgrades() {
      var cleanups =
          new Cleanups(
              hasAutumnaton(),
              withItem(ItemPool.AUTUMNATON),
              withNextResponse(200, html("request/test_choice_autumnaton_many_upgrades.html")));

      try (cleanups) {
        String output = execute("upgrade");
        assertErrorState();
        assertThat(output, containsString("No upgrades available"));
      }
    }

    @ParameterizedTest
    @CsvSource({
      "one, Added upgrade dual exhaust",
      "two, Added upgrades energy-absorptive hat and dual exhaust",
      "many, Added upgrades energy-absorptive hat and vision extender and dual exhaust"
    })
    public void upgradesIfUpgrades(String filePart, String upgrades) {
      var cleanups =
          new Cleanups(
              hasAutumnaton(),
              withItem(ItemPool.AUTUMNATON),
              withNextResponse(
                  200,
                  html("request/test_choice_autumnaton_upgrade_available_" + filePart + ".html")));

      try (cleanups) {
        String output = execute("upgrade");
        assertContinueState();
        assertThat(output, containsString(upgrades));
      }
    }

    @Nested
    class Locations {
      @Test
      public void showsLocations() {
        var cleanups =
            new Cleanups(
                hasAutumnaton(),
                withItem(ItemPool.AUTUMNATON),
                withProperty("autumnatonUpgrades", ""),
                withNextResponse(200, html("request/test_choice_autumnaton_all_upgrades.html")));

        try (cleanups) {
          String output = execute("locations");
          assertContinueState();
          // Daily Dungeon
          assertThat(output, containsString("<b>Underground</b>"));
          assertThat(
              output,
              containsString(
                  "Mid: gives autumn dollar (potion, +50% meat), collection prow (+1 autumn item)"));
          assertThat(output, containsString("<li>The Daily Dungeon</li>"));
        }
      }

      @Test
      public void doesNotShowUpgradeIfHaveUpgrade() {
        var cleanups =
            new Cleanups(
                hasAutumnaton(),
                withItem(ItemPool.AUTUMNATON),
                withProperty("autumnatonUpgrades", "cowcatcher"),
                withNextResponse(200, html("request/test_choice_autumnaton_all_upgrades.html")));

        try (cleanups) {
          String output = execute("locations");
          assertContinueState();
          // Daily Dungeon
          assertThat(output, containsString("Mid: gives autumn dollar (potion, +50% meat)"));
          assertThat(
              output,
              not(
                  containsString(
                      "Mid: gives autumn dollar (potion, +50% meat), collection prow (+1 autumn item)")));
        }
      }
    }
  }
}
