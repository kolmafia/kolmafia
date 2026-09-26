package net.sourceforge.kolmafia.session;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class JourneyManagerTest {
  @Test
  public void hasValidItems() {
    // Class initialization loads journeyman.txt.  It does substantial
    // checking to make sure there is no duplicated or missing data.
    //
    // 30 zones * 6 skills * 6 classes = 1080 data points

    // Map keyed by zones
    assertEquals(30, JourneyManager.journeymanData.size());

    // Map keyed by skills
    assertEquals(180, JourneyManager.journeymanSkills.size());
  }
}
