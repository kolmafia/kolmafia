package net.sourceforge.kolmafia.utilities;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import org.junit.jupiter.api.Test;

/**
 * A RollingLinkedList is bounded above in size. Items are added to the end of the list until the
 * size is reached, at which point the first element is deleted. The update method is used as an
 * alternative to add for the case where the caller needs to know what element was deleted to make
 * room for the added element. addAll is a convenience method that iterates over a list and adds the
 * items.
 */
public class RollingLinkedListTest {

  private RollingLinkedList<String> rll;
  private final String[] testValues = {"a", "b", "c", "d"};

  @Test
  public void itShouldHandleNegativeSize() {
    // A negative size is effectively a RollingLinkedList of unbounded size.  While a negative size
    // is perhaps an implementation error since a LinkedList is effectively the same thing, there
    // is no particular reason to prohibit a negative size.
    rll = new RollingLinkedList<>(-1);
    assertNotNull(rll, "Negative size list null.");
    assertEquals(0, rll.size(), "Unexpected list size.");
    rll.add(testValues[0]);
    assertEquals(1, rll.size(), "Unexpected list size.");
    rll.add(testValues[1]);
    assertEquals(2, rll.size(), "Unexpected list size.");
  }

  @Test
  public void itShouldAddToTheEnd() {
    rll = new RollingLinkedList<>(4);
    for (int i = 0; i < 4; i++) {
      rll.add(testValues[i]);
      assertEquals(testValues[i], rll.getLast(), "New element not last.");
      assertEquals(i + 1, rll.size(), "Unexpected size.");
    }
  }

  @Test
  public void itAllowsDuplicates() {
    rll = new RollingLinkedList<>(4);
    for (int i = 0; i < 4; i++) {
      rll.add(testValues[0]);
      assertEquals(testValues[0], rll.getLast(), "New element not last.");
      assertEquals(i + 1, rll.size(), "Unexpected size.");
    }
  }

  @Test
  public void itWillDeleteFromTheBeginningToPreserveSize() {
    rll = new RollingLinkedList<>(3);
    for (int i = 0; i < 3; i++) {
      rll.add(testValues[i]);
      assertEquals(testValues[i], rll.getLast(), "New element not last.");
      assertEquals(i + 1, rll.size(), "Unexpected size.");
    }
    rll.add(testValues[3]);
    assertEquals(testValues[3], rll.getLast(), "New element not last.");
    assertEquals(3, rll.size(), "Unexpected size.");
    for (int i = 1; i < 4; i++) {
      assertEquals(testValues[i], rll.get(i - 1), "Add did not shift.");
    }
  }

  @Test
  public void itWillReturnTheDeletedElementForUpdates() {
    String removedValue;
    rll = new RollingLinkedList<>(3);
    for (int i = 0; i < 3; i++) {
      removedValue = rll.update(testValues[i]);
      assertNull(removedValue, "Undeleted element returned");
    }
    removedValue = rll.update(testValues[3]);
    assertEquals(testValues[0], removedValue, "Unexpected value for delete element.");
  }

  @Test
  public void addAllAndIteratedAddsShouldDoTheSameThing() {
    RollingLinkedList<String> adds = new RollingLinkedList<>(3);
    for (int i = 0; i < 4; i++) {
      adds.add(testValues[i]);
    }
    RollingLinkedList<String> addsAll = new RollingLinkedList<>(3);
    addsAll.addAll(Arrays.asList(testValues));
    assertEquals(adds, addsAll, "addAll not the same.");
  }
}
