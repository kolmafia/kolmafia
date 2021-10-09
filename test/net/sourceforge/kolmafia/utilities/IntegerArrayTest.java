package net.sourceforge.kolmafia.utilities;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Iterator;
import org.junit.jupiter.api.Test;

public class IntegerArrayTest {

  final IntegerArray testObj = new IntegerArray();

  @Test
  public void variousOperationsShouldWorkWhenEmpty() {
    assertEquals(testObj.size(), 0);
    assertFalse(testObj.contains(1337));
    assertEquals(testObj.get(1), 0);
  }

  @Test
  public void variousOperationsShouldWorkWhenFull() {
    int a, b, counter;
    int[] values;
    Iterator<Integer> it;
    testObj.add(1);
    testObj.add(1337);
    testObj.add(-1);
    assertEquals(testObj.size(), 3);
    assertTrue(testObj.contains(1337));
    assertEquals(testObj.get(0), 1);
    assertEquals(testObj.get(2), -1);
    values = testObj.toArray();
    assertEquals(values.length, 3);
    assertEquals(values[0], 1);
    assertEquals(values[1], 1337);
    assertEquals(values[2], -1);
    it = testObj.iterator();
    counter = 0;
    while (it.hasNext()) {
      a = testObj.get(counter);
      b = it.next();
      assertEquals(a, b);
      counter++;
    }
  }
}
