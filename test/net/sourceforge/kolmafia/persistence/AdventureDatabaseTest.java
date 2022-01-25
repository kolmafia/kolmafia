package net.sourceforge.kolmafia.persistence;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import org.junit.jupiter.api.Test;

public class AdventureDatabaseTest {

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
