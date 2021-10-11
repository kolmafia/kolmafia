package net.sourceforge.kolmafia.utilities;

import static org.junit.Assert.*;

import org.junit.Test;

/**
 * This is a simple test for BooleanArray primarily written as an example for testing a single class
 * from KoLmafia. The package and file location were chosen to work with the "ant test" task. The
 * naming convention was used elsewhere to assist in peer review of test code and quick
 * identification of failed tests. This set of tests was chosen, in part, to provide 100% coverage
 * of the class under test in anticipation of a time when KoLmafia is tested with a coverage tool.
 */
public class BooleanArrayTest {

  private BooleanArray testArray;

  @Test
  public void itShouldBeEmpty() {
    // Create the array, check size, check return for index out of bounds
    testArray = new BooleanArray();
    assertEquals(0, testArray.size());
    assertFalse(testArray.get(1));
  }

  @Test
  public void itShouldHaveOneElementSet() {
    testArray = new BooleanArray();
    // Set and test one element
    testArray.set(0, true);
    assertEquals(1, testArray.size());
    assertTrue(testArray.get(0));
  }

  @Test
  public void itShouldToggleAnExistingElement() {
    testArray = new BooleanArray();
    // Set and test one element
    testArray.set(0, true);
    assertEquals(1, testArray.size());
    assertTrue(testArray.get(0));
    // Toggle it
    testArray.set(0, false);
    assertEquals(1, testArray.size());
    assertFalse(testArray.get(0));
  }

  @Test
  public void itShouldSetMissingElementsToFalse() {
    testArray = new BooleanArray();
    testArray.set(2, true);
    assertEquals(3, testArray.size());
    assertFalse(testArray.get(0));
    assertFalse(testArray.get(1));
    assertTrue(testArray.get(2));
  }
}
