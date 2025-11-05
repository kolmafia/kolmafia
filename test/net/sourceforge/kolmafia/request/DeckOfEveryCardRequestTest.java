package net.sourceforge.kolmafia.request;

import static internal.helpers.Networking.assertGetRequest;
import static internal.helpers.Networking.assertPostRequest;
import static internal.helpers.Networking.html;
import static internal.helpers.Player.withAdventuresLeft;
import static internal.helpers.Player.withGender;
import static internal.helpers.Player.withGuildStoreOpen;
import static internal.helpers.Player.withHttpClientBuilder;
import static internal.helpers.Player.withItem;
import static internal.helpers.Player.withPasswordHash;
import static internal.helpers.Player.withPath;
import static internal.helpers.Player.withProperty;
import static internal.helpers.Player.withSavePreferencesToFile;
import static internal.matchers.Preference.isSetTo;
import static net.sourceforge.kolmafia.request.DeckOfEveryCardRequest.RACING;
import static net.sourceforge.kolmafia.request.DeckOfEveryCardRequest.buffToCard;
import static net.sourceforge.kolmafia.request.DeckOfEveryCardRequest.canonicalNameToCard;
import static net.sourceforge.kolmafia.request.DeckOfEveryCardRequest.getCardById;
import static net.sourceforge.kolmafia.request.DeckOfEveryCardRequest.getMatchingNames;
import static net.sourceforge.kolmafia.request.DeckOfEveryCardRequest.phylumToCard;
import static net.sourceforge.kolmafia.request.DeckOfEveryCardRequest.statToCard;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import internal.helpers.Cleanups;
import internal.network.FakeHttpClientBuilder;
import java.util.List;
import java.util.Map;
import net.sourceforge.kolmafia.AscensionPath;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.MonsterDatabase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class DeckOfEveryCardRequestTest {
  private static final String USERNAME = "DeckOfEveryCardRequestTest";

  @BeforeAll
  static void beforeAll() {
    KoLCharacter.reset(USERNAME);
  }

  @Nested
  class StaticsCoverage {
    // These tests increase coverage but don't necessarily exercise functionality
    @Test
    public void testMatchingNames() {
      List<String> results = getMatchingNames("not a real card name");
      assertTrue(results.isEmpty());
      results = getMatchingNames(" of ");
      assertEquals(13, results.size());
      assertTrue(results.contains("x of spades"));
      assertTrue(results.contains("x of hearts"));
      assertTrue(results.contains("x of diamonds"));
      assertTrue(results.contains("x of clubs"));
    }

    @Test
    public void testPhylumToCard() {
      DeckOfEveryCardRequest.EveryCard card = phylumToCard(MonsterDatabase.Phylum.ELF);
      assertEquals(card, getCardById(28));
      card = phylumToCard(MonsterDatabase.Phylum.PENGUIN);
      assertEquals(card, getCardById(27));
    }

    @Test
    public void testCanonicalName() {
      String fName = "X of Spades";
      List<String> results = getMatchingNames(fName);
      assertEquals(1, results.size());
      DeckOfEveryCardRequest.EveryCard card = canonicalNameToCard(results.getFirst());
      assertEquals(card, getCardById(4));
    }

    @Test
    public void testInvalidInputGetCardById() {
      DeckOfEveryCardRequest.EveryCard card = getCardById(-1);
      assertNull(card);
      card = getCardById(999);
      assertNull(card);
    }

    @Test
    public void testGetStatCard() {
      DeckOfEveryCardRequest.EveryCard card = statToCard(KoLConstants.Stat.MOXIE);
      assertEquals(card, getCardById(69));
      card = statToCard(KoLConstants.Stat.MUSCLE);
      assertEquals(card, getCardById(68));
      card = statToCard(KoLConstants.Stat.MYSTICALITY);
      assertEquals(card, getCardById(70));
    }

    @Test
    public void testBuffToCard() {
      DeckOfEveryCardRequest.EveryCard card = buffToCard(RACING);
      assertEquals(card, getCardById(48));
    }

    @Test
    public void testRequestFields() {
      DeckOfEveryCardRequest req = new DeckOfEveryCardRequest();
      assertNull(req.getRequestCard());
      req = new DeckOfEveryCardRequest(getCardById(58));
      assertEquals(58, req.getRequestCard().id);
    }

    @Test
    public void testSomeEveryCardOverrides() {
      DeckOfEveryCardRequest.EveryCard mickey = getCardById(58);
      DeckOfEveryCardRequest.EveryCard notMickey = getCardById(59);
      DeckOfEveryCardRequest.EveryCard copyMickey =
          new DeckOfEveryCardRequest.EveryCard(mickey.id, mickey.name);
      // Not simplifying the assertion because this makes it explicit that EveryCard.equals is being
      // tested
      assertFalse(mickey.equals(null));
      assertFalse(mickey.equals(notMickey));
      assertTrue(mickey.equals(mickey));
      assertTrue(mickey.equals(copyMickey));
      assertEquals("1952 Mickey Mantle (58)", mickey.toString());
    }

    @Test
    public void testItShouldFollowRedirect() {
      DeckOfEveryCardRequest req = new DeckOfEveryCardRequest();
      assertTrue(req.shouldFollowRedirect());
    }

    @Test
    public void testGetAdventuresUsed() {
      DeckOfEveryCardRequest noCard = new DeckOfEveryCardRequest();
      assertEquals(1, noCard.getAdventuresUsed());
      DeckOfEveryCardRequest notMonster = new DeckOfEveryCardRequest(getCardById(58));
      assertEquals(0, notMonster.getAdventuresUsed());
      DeckOfEveryCardRequest monster = new DeckOfEveryCardRequest(getCardById(27));
      assertEquals(1, monster.getAdventuresUsed());
    }
  }

  @ParameterizedTest
  @CsvSource({"true, true", "true, false", "false, true", "false, false"})
  public void itShouldRunAndUpdatePreferences(boolean useUpdate, boolean useWrite) {
    DeckOfEveryCardRequest.EveryCard mickey = getCardById(58);
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
      new DeckOfEveryCardRequest(mickey).run();
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

  @Test
  public void permitsContinueWhenUsingReplica() {
    DeckOfEveryCardRequest.EveryCard mickey = getCardById(58);
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
            withPath(AscensionPath.Path.LEGACY_OF_LOATHING),
            withItem(ItemPool.REPLICA_DECK_OF_EVERY_CARD),
            withProperty("_deckCardsDrawn", 0),
            withProperty("_deckCardsSeen", ""),
            withGender(KoLCharacter.Gender.FEMALE),
            withGuildStoreOpen(false),
            withPasswordHash("cafebabe"),
            withAdventuresLeft(100));
    try (cleanups) {
      new DeckOfEveryCardRequest(mickey).run();
      var requests = builder.client.getRequests();
      assertThat(requests, hasSize(8));
      assertPostRequest(requests.get(0), "/inv_use.php", "whichitem=11230&cheat=1&pwd=cafebabe");
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
    assertTrue(KoLmafia.permitsContinue());
  }
}
