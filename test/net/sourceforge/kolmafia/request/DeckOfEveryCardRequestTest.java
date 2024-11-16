package net.sourceforge.kolmafia.request;

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
      List<String> results = DeckOfEveryCardRequest.getMatchingNames("not a real card name");
      assertTrue(results.isEmpty());
      results = DeckOfEveryCardRequest.getMatchingNames(" of ");
      assertEquals(13, results.size());
      assertTrue(results.contains("x of spades"));
      assertTrue(results.contains("x of hearts"));
      assertTrue(results.contains("x of diamonds"));
      assertTrue(results.contains("x of clubs"));
    }

    @Test
    public void testPhylumToCard() {
      DeckOfEveryCardRequest.EveryCard card =
          DeckOfEveryCardRequest.phylumToCard(MonsterDatabase.Phylum.ELF);
      assertEquals(card.name, "Christmas Card");
      card = DeckOfEveryCardRequest.phylumToCard(MonsterDatabase.Phylum.PENGUIN);
      assertEquals(card.name, "Suit Warehouse Discount Card");
    }
  }
}
