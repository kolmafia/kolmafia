package net.sourceforge.kolmafia.textui.command;

import static internal.helpers.Networking.assertGetRequest;
import static internal.helpers.Networking.assertPostRequest;
import static internal.helpers.Networking.html;
import static internal.helpers.Player.withAdventuresLeft;
import static internal.helpers.Player.withClass;
import static internal.helpers.Player.withFamiliar;
import static internal.helpers.Player.withFamiliarInTerrariumWithItem;
import static internal.helpers.Player.withGender;
import static internal.helpers.Player.withGuildStoreOpen;
import static internal.helpers.Player.withHP;
import static internal.helpers.Player.withHttpClientBuilder;
import static internal.helpers.Player.withItem;
import static internal.helpers.Player.withPasswordHash;
import static internal.helpers.Player.withProperty;
import static internal.helpers.Player.withStats;
import static internal.helpers.Player.withTurnsPlayed;
import static internal.matchers.Preference.isSetTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.*;

import internal.helpers.Cleanups;
import internal.network.FakeHttpClientBuilder;
import java.util.List;
import java.util.Map;
import net.sourceforge.kolmafia.AscensionClass;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.StaticEntity;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.request.GenericRequest;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PlayCommandTest extends AbstractCommandTestBase {
  public PlayCommandTest() {
    this.command = "cheat";
  }

  @BeforeAll
  public static void beforeAll() {
    KoLCharacter.reset("PlayCommandTestUser");
  }

  @BeforeEach
  public void initializeState() {
    KoLCharacter.reset("PlayCommandTestUser");
    StaticEntity.setContinuationState(KoLConstants.MafiaState.CONTINUE);
    GenericRequest.sessionId = null;
  }

  // These tests check for cases where user input is being checked
  @Test
  public void needsParameter() {
    String output = execute("");
    assertContinueState();
    assertTrue(output.contains("Play what?"));
  }

  @Test
  public void needPhylumParameter() {
    String output = execute("phylum");
    assertErrorState();
    assertTrue(output.contains("Which monster phylum do you want?"));
  }

  @Test
  public void needValidPhylum() {
    String output = execute("phylum not_a_phylum");
    assertErrorState();
    assertTrue(output.contains("What kind of random monster is"));
  }

  @Test
  public void needAvailablePhylum() {
    String output = execute("phylum none");
    assertErrorState();
    assertTrue(output.contains("What kind of random monster is"));
  }

  @Test
  public void needStat() {
    String output = execute("stat");
    assertErrorState();
    assertTrue(output.contains("Which stat do you want?"));
  }

  @Test
  public void needValidStat() {
    String output = execute("stat enisland");
    assertErrorState();
    assertTrue(output.contains("Which stat is"));
  }

  @Test
  public void needUnambiguousStat() {
    String output = execute("stat m");
    assertErrorState();
    assertTrue(output.contains("is an ambiguous stat"));
  }

  @Test
  public void needBuff() {
    String output = execute("buff");
    assertErrorState();
    assertTrue(output.contains("Which buff do you want?"));
  }

  @Test
  public void needValidBuff() {
    String output = execute("buff bongos");
    assertErrorState();
    assertTrue(output.contains("Which buff is"));
  }

  @Test
  public void needUniqueBuff() {
    String output = execute("buff m");
    assertErrorState();
    assertTrue(output.contains("is an ambiguous buff"));
  }

  @Test
  public void needValidCardName() {
    String output = execute("queen");
    assertErrorState();
    assertTrue(output.contains("I don't know how to play"));
  }

  @Test
  public void needUnambiguousCard() {
    String output = execute("X");
    assertErrorState();
    assertTrue(output.contains("is an ambiguous card name"));
  }

  // These tests increase coverage by taking a different path driven by input.  The failure is
  // because the request is not actually run.  The cleanups are to force DeckOfEveryCardRequest
  // to get as far as it can without a faux request.
  @Test
  public void drawRandom() {
    var cleanups =
        new Cleanups(
            withItem(ItemPool.DECK_OF_EVERY_CARD),
            withProperty("_deckCardsDrawn", 0),
            withHP(123, 123, 123));
    try (cleanups) {
      String output = execute("random");
      assertErrorState();
      assertTrue(output.contains("I/O error"));
    }
  }

  @Test
  public void drawNamedCard() {
    var cleanups =
        new Cleanups(
            withItem(ItemPool.DECK_OF_EVERY_CARD),
            withProperty("_deckCardsDrawn", 0),
            withHP(123, 123, 123));
    try (cleanups) {
      String output = execute("race");
      assertErrorState();
      assertTrue(output.contains("I/O error"));
    }
  }

  @Test
  public void drawSpecificStat() {
    var cleanups =
        new Cleanups(
            withItem(ItemPool.DECK_OF_EVERY_CARD),
            withProperty("_deckCardsDrawn", 0),
            withHP(123, 123, 123));
    try (cleanups) {
      String output = execute("stat myst");
      assertErrorState();
      assertTrue(output.contains("I/O error"));
    }
  }

  @Test
  public void drawMainStat() {
    var cleanups =
        new Cleanups(
            withItem(ItemPool.DECK_OF_EVERY_CARD),
            withProperty("_deckCardsDrawn", 0),
            withHP(123, 123, 123),
            withClass(AscensionClass.ACCORDION_THIEF));
    try (cleanups) {
      String output = execute("stat main");
      assertErrorState();
      assertTrue(output.contains("I/O error"));
    }
  }

  // This test was modified from DeckOfEveryCardRequestTest so that the run request could be
  // triggered by the cheat command and not just by request.run()
  @Test
  public void itShouldRunAndDrawCard() {
    var builder = new FakeHttpClientBuilder();
    var client = builder.client;
    client.addResponse(302, Map.of("location", List.of("choice.php?forceoption=0")), "");
    client.addResponse(200, html("request/cheat_1.html"));
    client.addResponse(200, html("request/cheat_2.json"));
    client.addResponse(302, Map.of("location", List.of("choice.php?forceoption=0")), "");
    client.addResponse(200, html("request/cheat_3.html"));
    client.addResponse(200, html("request/cheat_4.json"));
    client.addResponse(200, html("request/cheat_5.html"));
    client.addResponse(200, html("request/cheat_6.json"));
    var cleanups =
        new Cleanups(
            withHttpClientBuilder(builder),
            withStats(26238, 38694, 26255),
            withItem(ItemPool.DECK_OF_EVERY_CARD),
            withFamiliarInTerrariumWithItem(2, ItemPool.SOLID_SHIFTING_TIME_WEIRDNESS),
            withFamiliar(2),
            withProperty("_deckCardsDrawn", 0),
            withProperty("_deckCardsSeen", ""),
            // The absence of these next two triggers additional requests
            withGuildStoreOpen(false),
            withGender(KoLCharacter.Gender.FEMALE),
            withPasswordHash("babe"),
            withTurnsPlayed(2272543),
            withAdventuresLeft(167));
    try (cleanups) {
      String output = execute("Ancestral Recall");
      assertTrue(output.contains("play Ancestral Recall"));
      assertTrue(output.contains("You acquire an item: blue mana"));
      var requests = builder.client.getRequests();
      assertThat(requests, hasSize(8));
      assertPostRequest(requests.get(0), "/inv_use.php", "whichitem=8382&cheat=1&pwd=babe");
      assertGetRequest(requests.get(1), "/choice.php", "forceoption=0");
      assertPostRequest(requests.get(2), "/api.php", "what=status&for=KoLmafia");
      assertPostRequest(
          requests.get(3), "/choice.php", "whichchoice=1086&option=1&which=40&pwd=babe");
      assertGetRequest(requests.get(4), "/choice.php", "forceoption=0");
      assertPostRequest(requests.get(5), "/api.php", "what=status&for=KoLmafia");
      assertPostRequest(requests.get(6), "/choice.php", "whichchoice=1085&option=1&pwd=babe");
      assertPostRequest(requests.get(7), "/api.php", "what=status&for=KoLmafia");
      assertThat("_deckCardsDrawn", isSetTo(5));
      assertThat("_deckCardsSeen", isSetTo("Ancestral Recall"));
    }
  }
}
