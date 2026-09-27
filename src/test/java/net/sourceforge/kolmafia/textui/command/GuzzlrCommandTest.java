package net.sourceforge.kolmafia.textui.command;

import static internal.helpers.Player.withItem;
import static internal.helpers.Player.withProperty;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

import internal.helpers.Cleanups;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.preferences.Preferences;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class GuzzlrCommandTest extends AbstractCommandTestBase {
  @BeforeAll
  public static void init() {
    KoLCharacter.reset("testUser");
    Preferences.reset("testUser");
  }

  public GuzzlrCommandTest() {
    this.command = "guzzlr";
  }

  @Test
  void mustHaveGuzzlrTablet() {
    String output = execute("accept bronze");

    assertErrorState();
    assertThat(output, containsString("You don't have a Guzzlr tablet"));
  }

  @Test
  void mustSpecifyParameters() {
    var cleanups = withItem(ItemPool.GUZZLR_TABLET);
    try (cleanups) {
      String output = execute("");

      assertErrorState();
      assertThat(output, containsString("Use command guzzlr"));
    }
  }

  @Test
  void abandonMustHaveQuest() {
    var cleanups =
        new Cleanups(withItem(ItemPool.GUZZLR_TABLET), withProperty("questGuzzlr", "unstarted"));
    try (cleanups) {
      String output = execute("abandon");

      assertContinueState();
      assertThat(output, containsString("You don't have a client"));
    }
  }

  @Test
  void cannotAbandonTwiceInADay() {
    var cleanups =
        new Cleanups(
            withItem(ItemPool.GUZZLR_TABLET),
            withProperty("_guzzlrQuestAbandoned", true),
            withProperty("questGuzzlr", "started"));
    try (cleanups) {
      String output = execute("abandon");

      assertErrorState();
      assertThat(output, containsString("already abandoned"));
    }
  }

  @Test
  void abandonAbandons() {
    var cleanups =
        new Cleanups(withItem(ItemPool.GUZZLR_TABLET), withProperty("questGuzzlr", "started"));
    try (cleanups) {
      String output = execute("abandon");

      assertContinueState();
      assertThat(output, containsString("Abandoning client"));
    }
  }

  @Test
  void acceptRequiresNoClient() {
    var cleanups =
        new Cleanups(withItem(ItemPool.GUZZLR_TABLET), withProperty("questGuzzlr", "started"));
    try (cleanups) {
      String output = execute("accept bronze");

      assertErrorState();
      assertThat(output, containsString("You already have a client"));
    }
  }

  @Test
  void acceptRequiresValidClient() {
    var cleanups = withItem(ItemPool.GUZZLR_TABLET);
    try (cleanups) {
      String output = execute("accept silver");

      assertErrorState();
      assertThat(output, containsString("Use command 'guzzlr"));
    }
  }

  @Test
  void acceptBronze() {
    var cleanups = withItem(ItemPool.GUZZLR_TABLET);
    try (cleanups) {
      String output = execute("accept bronze");

      assertContinueState();
      assertThat(output, containsString("Accepting a bronze client"));
    }
  }

  @Test
  void acceptGoldRequiresFiveBronze() {
    var cleanups = withItem(ItemPool.GUZZLR_TABLET);
    try (cleanups) {
      String output = execute("accept gold");

      assertErrorState();
      assertThat(
          output, containsString("You need to make 5 bronze deliveries to serve gold clients"));
    }
  }

  @Test
  void acceptGold() {
    var cleanups =
        new Cleanups(withItem(ItemPool.GUZZLR_TABLET), withProperty("guzzlrBronzeDeliveries", 5));
    try (cleanups) {
      String output = execute("accept gold");

      assertContinueState();
      assertThat(output, containsString("Accepting a gold client"));
    }
  }

  @Test
  void acceptPlatinumRequiresFiveGold() {
    var cleanups = withItem(ItemPool.GUZZLR_TABLET);
    try (cleanups) {
      String output = execute("accept platinum");

      assertErrorState();
      assertThat(
          output, containsString("You need to make 5 gold deliveries to serve platinum clients"));
    }
  }

  @Test
  void acceptPlatinum() {
    var cleanups =
        new Cleanups(withItem(ItemPool.GUZZLR_TABLET), withProperty("guzzlrGoldDeliveries", 5));
    try (cleanups) {
      String output = execute("accept platinum");

      assertContinueState();
      assertThat(output, containsString("Accepting a platinum client"));
    }
  }
}
