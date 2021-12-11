package net.sourceforge.kolmafia.textui.command;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.GenericRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class GuzzlrCommandTest extends AbstractCommandTestBase {
  @BeforeEach
  public void initEach() {
    KoLCharacter.reset("testUser");
    KoLCharacter.reset(true);
    Preferences.resetToDefault("_guzzlrQuestAbandoned");
    Preferences.resetToDefault("questGuzzlr");
    Preferences.resetToDefault("guzzlrBronzeDeliveries");
    Preferences.resetToDefault("guzzlrGoldDeliveries");

    // Stop requests from actually running
    GenericRequest.sessionId = null;
  }

  public GuzzlrCommandTest() {
    this.command = "guzzlr";
  }

  private static void playerHasGuzzlrTablet() {
    AdventureResult.addResultToList(KoLConstants.inventory, ItemPool.get(ItemPool.GUZZLR_TABLET));
  }

  @Test
  void mustHaveGuzzlrTablet() {
    String output = execute("accept bronze");

    assertErrorState();
    assertThat(output, containsString("You don't have a Guzzlr tablet"));
  }

  @Test
  void mustSpecifyParameters() {
    playerHasGuzzlrTablet();
    String output = execute("");

    assertErrorState();
    assertThat(output, containsString("Use command guzzlr"));
  }

  @Test
  void abandonMustHaveQuest() {
    playerHasGuzzlrTablet();
    Preferences.setString("questGuzzlr", "unstarted");
    String output = execute("abandon");

    assertContinueState();
    assertThat(output, containsString("You don't have a client"));
  }

  @Test
  void cannotAbandonTwiceInADay() {
    playerHasGuzzlrTablet();
    Preferences.setBoolean("_guzzlrQuestAbandoned", true);
    Preferences.setString("questGuzzlr", "started");
    String output = execute("abandon");

    assertErrorState();
    assertThat(output, containsString("already abandoned"));
  }

  @Test
  void abandonAbandons() {
    playerHasGuzzlrTablet();
    Preferences.setString("questGuzzlr", "started");
    String output = execute("abandon");

    assertContinueState();
    assertThat(output, containsString("Abandoning client"));
  }

  @Test
  void acceptRequiresNoClient() {
    playerHasGuzzlrTablet();
    Preferences.setString("questGuzzlr", "started");
    String output = execute("accept bronze");

    assertErrorState();
    assertThat(output, containsString("You already have a client"));
  }

  @Test
  void acceptRequiresValidClient() {
    playerHasGuzzlrTablet();
    String output = execute("accept silver");

    assertErrorState();
    assertThat(output, containsString("Use command 'guzzlr"));
  }

  @Test
  void acceptBronze() {
    playerHasGuzzlrTablet();
    String output = execute("accept bronze");

    assertContinueState();
    assertThat(output, containsString("Accepting a bronze client"));
  }

  @Test
  void acceptGoldRequiresFiveBronze() {
    playerHasGuzzlrTablet();
    String output = execute("accept gold");

    assertErrorState();
    assertThat(
        output, containsString("You need to make 5 bronze deliveries to serve gold clients"));
  }

  @Test
  void acceptGold() {
    playerHasGuzzlrTablet();
    Preferences.setInteger("guzzlrBronzeDeliveries", 5);
    String output = execute("accept gold");

    assertContinueState();
    assertThat(output, containsString("Accepting a gold client"));
  }

  @Test
  void acceptPlatinumRequiresFiveGold() {
    playerHasGuzzlrTablet();
    String output = execute("accept platinum");

    assertErrorState();
    assertThat(
        output, containsString("You need to make 5 gold deliveries to serve platinum clients"));
  }

  @Test
  void acceptPlatinum() {
    playerHasGuzzlrTablet();
    Preferences.setInteger("guzzlrGoldDeliveries", 5);
    String output = execute("accept platinum");

    assertContinueState();
    assertThat(output, containsString("Accepting a platinum client"));
  }
}
