package net.sourceforge.kolmafia.utilities;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * As implemented RollingLinkedList will fail if passed an initial length that is negative. addAll
 * is not implementd and so the list can be longer than the declared length.
 */
public class RollingLinkedListTest {

  private RollingLinkedList<String> rll;

  @Test
  public void itShouldAlwaysReplaceTheOnlyElement() {
    rll = new RollingLinkedList<>(1);
    rll.add("Alpha");
    assertTrue(rll.contains("Alpha"));
    assertFalse(rll.contains("Beta"));
    rll.add("Beta");
    assertFalse(rll.contains("Alpha"));
    assertTrue(rll.contains("Beta"));
  }

  @Test
  public void itShouldAddToTheEnd() {
    rll = new RollingLinkedList<>(2);
    rll.add("Alpha");
    assertEquals(rll.size(), 1);
    assertEquals(rll.indexOf("Alpha"), 0);
    rll.add("Beta");
    assertEquals(rll.size(), 2);
    assertEquals(rll.indexOf("Alpha"), 0);
    assertEquals(rll.indexOf("Beta"), 1);
    rll.add("Gamma");
    assertEquals(rll.size(), 2);
    assertEquals(rll.indexOf("Alpha"), -1);
    assertEquals(rll.indexOf("Beta"), 0);
    assertEquals(rll.indexOf("Gamma"), 1);
  }

  @Test
  public void itShouldUpdateByMovingToTheEnd() {
    rll = new RollingLinkedList<>(2);
    rll.add("Alpha");
    rll.add("Beta");
    assertEquals(rll.indexOf("Alpha"), 0);
    assertEquals(rll.indexOf("Beta"), 1);
    rll.update("Alpha");
    assertEquals(rll.indexOf("Alpha"), 1);
    assertEquals(rll.indexOf("Beta"), 0);
  }

  @Test
  public void itShouldUpdateANewItem() {
    rll = new RollingLinkedList<>(2);
    rll.add("Alpha");
    rll.add("Beta");
    assertEquals(rll.indexOf("Alpha"), 0);
    assertEquals(rll.indexOf("Beta"), 1);
    rll.update("Gamma");
    assertEquals(rll.indexOf("Alpha"), -1);
    assertEquals(rll.indexOf("Beta"), 0);
    assertEquals(rll.indexOf("Gamma"), 1);
  }

  @Test
  @Disabled("Class under test needs to be augmented")
  public void itShouldBehaveForABulkAdd() {
    rll = new RollingLinkedList<>(2);
    rll.add("Alpha");
    rll.add("Beta");
    assertEquals(rll.indexOf("Alpha"), 0);
    assertEquals(rll.indexOf("Beta"), 1);
    assertEquals(rll.size(), 2);
    ArrayList<String> collection = new ArrayList<>(3);
    collection.add("A");
    collection.add("B");
    collection.add("C");
    rll.addAll(collection);
    assertEquals(rll.size(), 2);
    assertEquals(rll.indexOf("Alpha"), -1);
    assertEquals(rll.indexOf("Beta"), -1);
    assertEquals(rll.indexOf("A"), -1);
    assertEquals(rll.indexOf("B"), 0);
    assertEquals(rll.indexOf("C"), 1);
  }
}
