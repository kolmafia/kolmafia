package net.sourceforge.kolmafia.session;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

public class ChoiceOptionTest {
  @Test
  public void thatInterviewWithYouHasOptions() {
    // Choice 546 is Interview With You
    //
    // The ChoiceAdventure is defined in ChoiceAdventures.java
    // The ChoiceOptions are defined in VampoutManager.java
    //
    // If the ChoiceOption class is in ChoiceAdventures, there can be a
    // circular dependency.
    //
    // Lets reproduce it and fix it.

    // Force ChoiceManager to be loaded first
    String result = ChoiceManager.specialChoiceDecision1(546, "13", 0, "");
    assertEquals(result, "0");
    // Now load ChoiceAdventures
    var adventure = ChoiceAdventures.choiceToChoiceAdventure.get(546);
    assertNotNull(adventure);
    var options = adventure.getOptions();
    assertNotNull(options);
    assertEquals(13, options.length);
  }
}
