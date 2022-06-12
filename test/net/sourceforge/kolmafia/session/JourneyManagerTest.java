package net.sourceforge.kolmafia;

import static org.junit.jupiter.api.Assertions.assertTrue;

import net.sourceforge.kolmafia.session.JourneyManager;
import org.junit.jupiter.api.Test;

public class JourneyManagerTest {
  @Test
  public void canReturnTrue() {
    assertTrue(JourneyManager.returnTrue());
  }
}
