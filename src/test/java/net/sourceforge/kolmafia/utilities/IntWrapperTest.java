package net.sourceforge.kolmafia.utilities;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

/** This is a simple test for IntWrapper because sometimes even low hanging fruit is good. */
public class IntWrapperTest {

  private IntWrapper iw;

  @Test
  public void itShouldReturnWhatIsThere() {
    int testVal = 314;
    iw = new IntWrapper();
    assertNotEquals(testVal, iw.getChoice());
    iw.setChoice(testVal);
    assertEquals(testVal, iw.getChoice());
  }
}
