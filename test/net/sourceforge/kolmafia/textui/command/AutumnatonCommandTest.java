package net.sourceforge.kolmafia.textui.command;

import static internal.helpers.Networking.html;
import static internal.helpers.Player.withHttpClientBuilder;
import static internal.helpers.Player.withItem;
import static internal.helpers.Player.withProperty;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

import internal.helpers.Cleanups;
import internal.network.FakeHttpClientBuilder;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.session.ChoiceManager;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class AutumnatonCommandTest extends AbstractCommandTestBase {

  public AutumnatonCommandTest() {
    this.command = "autumnaton";
  }

  @BeforeAll
  public static void beforeAll() {
    KoLCharacter.reset("AutumnatonCommandTest");
    ChoiceManager.handlingChoice = false;
  }

  @Test
  void errorsWithNoAutumnaton() {
    String output = execute("");
    assertErrorState();
    assertThat(output, containsString("You need an autumn-aton"));
  }

  @Nested
  class Send {
    @Test
    void errorsWithNoLocation() {
      var cleanups = withItem(ItemPool.AUTUMNATON);

      try (cleanups) {
        String output = execute("send ");
        assertErrorState();
        assertThat(output, containsString("Where do you want to send the little guy?"));
      }
    }

    @Test
    void errorsWithBadLocation() {
      var cleanups = withItem(ItemPool.AUTUMNATON);

      try (cleanups) {
        String output = execute("send trogdor");
        assertErrorState();
        assertThat(output, containsString("I don't understand where trogdor is."));
      }
    }

    @Test
    void errorsWithNonSnarfblatLocation() {
      var cleanups = withItem(ItemPool.AUTUMNATON);

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
              withHttpClientBuilder(builder),
              withProperty("autumnatonQuestLocation", ""),
              withItem(ItemPool.AUTUMNATON));

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
              withHttpClientBuilder(builder),
              withProperty("autumnatonQuestLocation", ""),
              withItem(ItemPool.AUTUMNATON));

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
}
