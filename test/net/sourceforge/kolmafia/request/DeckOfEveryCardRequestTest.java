package net.sourceforge.kolmafia.request;

import static net.sourceforge.kolmafia.request.DeckOfEveryCardRequest.canonicalNameToCard;
import static net.sourceforge.kolmafia.request.DeckOfEveryCardRequest.getMatchingNames;
import static net.sourceforge.kolmafia.request.DeckOfEveryCardRequest.phylumToCard;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.persistence.MonsterDatabase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class DeckOfEveryCardRequestTest {
  private static final String USERNAME = "DeckOfEvertCardRequestTest";

  @BeforeAll
  static void beforeAll() {
    KoLCharacter.reset(USERNAME);
  }

  @Nested
  class StaticsCoverage {
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
      assertEquals(card.name, "Christmas Card");
      card = phylumToCard(MonsterDatabase.Phylum.PENGUIN);
      assertEquals(card.name, "Suit Warehouse Discount Card");
    }

    @Test
    public void testCanonicalName() {
      String cName = "x of spades";
      String fName = "X of Spades";
      DeckOfEveryCardRequest.EveryCard card = canonicalNameToCard(cName);
      assertEquals(card.name, fName);
    }

    @Test
    public void testGetStatCard() {}
  }
}
