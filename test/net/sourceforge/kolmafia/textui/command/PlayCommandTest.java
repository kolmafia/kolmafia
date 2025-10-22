package net.sourceforge.kolmafia.textui.command;

import static internal.helpers.Networking.assertGetRequest;
import static internal.helpers.Networking.assertPostRequest;
import static internal.helpers.Networking.html;
import static internal.helpers.Player.withAdventuresLeft;
import static internal.helpers.Player.withClass;
import static internal.helpers.Player.withEquipped;
import static internal.helpers.Player.withFamiliar;
import static internal.helpers.Player.withFamiliarInTerrariumWithItem;
import static internal.helpers.Player.withGender;
import static internal.helpers.Player.withGuildStoreOpen;
import static internal.helpers.Player.withHP;
import static internal.helpers.Player.withHttpClientBuilder;
import static internal.helpers.Player.withItem;
import static internal.helpers.Player.withPasswordHash;
import static internal.helpers.Player.withProperty;
import static internal.helpers.Player.withSavePreferencesToFile;
import static internal.matchers.Preference.isSetTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.*;

import internal.helpers.Cleanups;
import internal.network.FakeHttpClientBuilder;
import net.sourceforge.kolmafia.AscensionClass;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.StaticEntity;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.request.DeckOfEveryCardRequest;
import net.sourceforge.kolmafia.request.GenericRequest;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;
import java.util.Map;

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
    StaticEntity.setContinuationState(KoLConstants.MafiaState.CONTINUE);
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

  // These tests increase coverage by taking a different path driven by input.  The failure is because
  // the request is not actually run.  The cleanups are to force DeckOfEveryCardRequest to get as far as it
  // can without a faux request.
  @Test
  public void drawRandom() {
    var cleanups = new Cleanups(withItem(ItemPool.DECK_OF_EVERY_CARD),
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
      var cleanups = new Cleanups(withItem(ItemPool.DECK_OF_EVERY_CARD),
        withProperty("_deckCardsDrawn", 0),
        withHP(123,123,123));
      try (cleanups) {
        String output = execute("race");
        assertErrorState();
        assertTrue(output.contains("I/O error"));
      }
  }
  @Test
  public void drawSpecificStat() {
    var cleanups = new Cleanups(withItem(ItemPool.DECK_OF_EVERY_CARD),
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
    var cleanups = new Cleanups(withItem(ItemPool.DECK_OF_EVERY_CARD),
      withProperty("_deckCardsDrawn", 0),
      withHP(123, 123, 123),
      withClass(AscensionClass.ACCORDION_THIEF));
    try (cleanups) {
      String output = execute("stat main");
      assertErrorState();
      assertTrue(output.contains("I/O error"));
    }
  }
  // This test was modified from DeckOfEveryCardRequestTest so that the run request could be triggered
  // by the cheat command and not just by request.run()
  @ParameterizedTest
  @CsvSource({"true, true", "true, false", "false, true", "false, false"})
  public void itShouldRunAndUpdatePreferences(boolean useUpdate, boolean useWrite) {
    //DeckOfEveryCardRequest.EveryCard mickey = getCardById(58);
    var builder = new FakeHttpClientBuilder();
    var client = builder.client;
    client.addResponse(302, Map.of("location", List.of("choice.php?forceoption=0")), "");
    client.addResponse(200, html("request/use_deck_one.html"));
    client.addResponse(200, html("request/use_deck_two.json"));
    client.addResponse(302, Map.of("location", List.of("choice.php?forceoption=0")), "");
    client.addResponse(200, html("request/use_deck_three.html"));
    client.addResponse(200, html("request/use_deck_four.json"));
    client.addResponse(200, html("request/use_deck_five.html"));
    client.addResponse(200, html("request/use_deck_six.json"));
    var cleanups =
      new Cleanups(
        withHttpClientBuilder(builder),
        withItem(ItemPool.DECK_OF_EVERY_CARD),
        withEquipped(ItemPool.GOLD_CROWN),
        withEquipped(ItemPool.CURSED_PIRATE_CUTLASS),
        withEquipped(ItemPool.SILVER_COW_CREAMER),
        withEquipped(ItemPool.BUDDY_BJORN),
        withEquipped(ItemPool.DUCT_TAPE_SHIRT),
        withEquipped(ItemPool.POODLE_SKIRT),
        withEquipped(ItemPool.CURSED_SWASH_BUCKLE),
        withEquipped(ItemPool.RING_OF_THE_SKELETON_LORD),
        withEquipped(ItemPool.INCREDIBLY_DENSE_MEAT_GEM),
        withFamiliarInTerrariumWithItem(1, ItemPool.SOLID_SHIFTING_TIME_WEIRDNESS),
        withFamiliar(1),
        withProperty("_deckCardsDrawn", 0),
        withProperty("_deckCardsSeen", ""),
        withGender(KoLCharacter.Gender.FEMALE),
        withGuildStoreOpen(false),
        withPasswordHash("cafebabe"),
        withProperty("saveSettingsOnSet", useUpdate),
        withAdventuresLeft(100));
    // Because this is a test the assumption is that saving preferences is disabled
    if (useWrite) {
      cleanups.add(withSavePreferencesToFile());
    }
    try (cleanups) {
      String output = execute("1952 Mickey Mantle");
      //new DeckOfEveryCardRequest(mickey).run();
      var requests = builder.client.getRequests();
      assertThat(requests, hasSize(8));
      assertPostRequest(requests.get(0), "/inv_use.php", "whichitem=8382&cheat=1&pwd=cafebabe");
      assertGetRequest(requests.get(1), "/choice.php", "forceoption=0");
      assertPostRequest(requests.get(2), "/api.php", "what=status&for=KoLmafia");
      assertPostRequest(
        requests.get(3), "/choice.php", "whichchoice=1086&option=1&which=58&pwd=cafebabe");
      assertGetRequest(requests.get(4), "/choice.php", "forceoption=0");
      assertPostRequest(requests.get(5), "/api.php", "what=status&for=KoLmafia");
      assertPostRequest(requests.get(6), "/choice.php", "whichchoice=1085&option=1&pwd=cafebabe");
      assertPostRequest(requests.get(7), "/api.php", "what=status&for=KoLmafia");
      assertThat("_deckCardsDrawn", isSetTo(5));
      assertThat("_deckCardsSeen", isSetTo("1952 Mickey Mantle"));
    }
  }
}
