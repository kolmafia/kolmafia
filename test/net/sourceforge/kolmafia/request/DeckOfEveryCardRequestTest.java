package net.sourceforge.kolmafia.request;

import static net.sourceforge.kolmafia.request.DeckOfEveryCardRequest.RACING;
import static net.sourceforge.kolmafia.request.DeckOfEveryCardRequest.buffToCard;
import static net.sourceforge.kolmafia.request.DeckOfEveryCardRequest.canonicalNameToCard;
import static net.sourceforge.kolmafia.request.DeckOfEveryCardRequest.getCardById;
import static net.sourceforge.kolmafia.request.DeckOfEveryCardRequest.getMatchingNames;
import static net.sourceforge.kolmafia.request.DeckOfEveryCardRequest.phylumToCard;
import static net.sourceforge.kolmafia.request.DeckOfEveryCardRequest.statToCard;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.persistence.MonsterDatabase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

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
      DeckOfEveryCardRequest.EveryCard card = canonicalNameToCard(results.get(0));
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
      assertEquals(req.getRequestCard().id, 58);
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
      assertEquals(mickey.toString(), "1952 Mickey Mantle (58)");
    }

    @Test
    public void testItShouldFollowRedirect() {
      DeckOfEveryCardRequest req = new DeckOfEveryCardRequest();
      assertTrue(req.shouldFollowRedirect());
    }

    @Test
    public void testGetAdventuresUsed() {
      DeckOfEveryCardRequest noCard = new DeckOfEveryCardRequest();
      assertEquals(noCard.getAdventuresUsed(), 1);
      DeckOfEveryCardRequest notMonster = new DeckOfEveryCardRequest(getCardById(58));
      assertEquals(notMonster.getAdventuresUsed(), 0);
      DeckOfEveryCardRequest monster = new DeckOfEveryCardRequest(getCardById(27));
      assertEquals(monster.getAdventuresUsed(), 1);
    }
  }
}
