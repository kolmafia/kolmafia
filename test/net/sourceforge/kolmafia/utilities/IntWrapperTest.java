package net.sourceforge.kolmafia.utilities;

import static org.junit.Assert.*;

import org.junit.Test;

/** This is a simple test for IntWrapper because sometimes even low hanging fruit is good. */
public class IntWrapperTest {

  private IntWrapper iw;

  @Test
  public void itShouldReturnWhatIsThere() {
    int testVal = 314;
    iw = new IntWrapper();
    assertFalse(iw.getChoice() == testVal);
    iw.setChoice(testVal);
    assertEquals(iw.getChoice(), testVal);
  }
}
