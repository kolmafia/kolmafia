package net.sourceforge.kolmafia.persistence;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class AdventureDatabaseTest {

  @Nested
  class GetAdventure {
    @Test
    public void getAdventureHandlesErrorCases() {
      assertThat(AdventureDatabase.getAdventure(null), is(nullValue()));
      assertThat(AdventureDatabase.getAdventure(""), is(nullValue()));
    }

    @Test
    public void getAdventureHandlesRequiredExactMatch() {
      var adventure = AdventureDatabase.getAdventure("\"Summoning Chamber\"");
      assertThat(adventure.getAdventureName(), is("Summoning Chamber"));
    }

    @Test
    public void getAdventureHandlesSinglePartialMatch() {
      var adventure = AdventureDatabase.getAdventure("Summoning Chambe");
      assertThat(adventure.getAdventureName(), is("Summoning Chamber"));
    }

    @Test
    public void getAdventureReturnsNullOnNoMatch() {
      var adventure = AdventureDatabase.getAdventure("  what??? ");
      assertThat(adventure, is(nullValue()));
    }

    @Test
    public void getAdventureReturnsNullOnManyMatch() {
      var adventure = AdventureDatabase.getAdventure("S");
      assertThat(adventure, is(nullValue()));
    }
  }

  @Nested
  class ValidateAdventure {
    @Test
    public void validatesCorrectArea() {
      assertTrue(AdventureDatabase.validateAdventureArea("Burnbarrel Blvd."));
    }

    @Test
    public void validatesInvalidArea() {
      assertFalse(AdventureDatabase.validateAdventureArea("Honest John's Shop"));
    }
  }
}
