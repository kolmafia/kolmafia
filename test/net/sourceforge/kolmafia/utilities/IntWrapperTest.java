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
    assertNotEquals(iw.getChoice(), testVal);
    iw.setChoice(testVal);
    assertEquals(iw.getChoice(), testVal);
  }
}
