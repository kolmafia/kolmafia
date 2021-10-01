package net.sourceforge.kolmafia.utilities;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;
import net.sourceforge.kolmafia.KoLConstants;
import org.junit.Test;

public class KoLDatabaseTest {

  List<String> theList = new ArrayList<String>();
  List<Integer> theOtherList = new ArrayList<Integer>();

  @Test
  public void itShouldReturnEOLForEmptyList() {
    assertEquals(KoLConstants.LINE_BREAK, KoLDatabase.getBreakdown(null));
    assertEquals(KoLConstants.LINE_BREAK, KoLDatabase.getBreakdown(theList));
  }

  @Test
  public void itShouldConvertListToString() {
    String lb = KoLConstants.LINE_BREAK;
    theList.clear();
    theList.add("Apple");
    theList.add("Apple");
    theList.add("Banana");
    theList.add("Orange");
    String retVal = KoLDatabase.getBreakdown(theList);
    String expect =
        lb
            + "<ul>"
            + "<li><nobr>Apple: 2</nobr></li>"
            + lb
            + "<li><nobr>Banana: 1</nobr></li>"
            + lb
            + "<li><nobr>Orange: 1</nobr></li>"
            + lb
            + "</ul>"
            + lb;
    assertEquals(retVal, expect);
    theList.clear();
    theList.add(null);
    theList.add(null);
    theList.add("Apple");
    theList.add("Apple");
    theList.add("Banana");
    theList.add(null);
    theList.add("Orange");
    theList.add(null);
    theList.add(null);
    assertEquals(retVal, expect);
  }

  @Test
  public void itShouldAddAListOfOnlyIntegers() {
    theOtherList.clear();
    theOtherList.add(Integer.valueOf(3));
    theOtherList.add(Integer.valueOf(1));
    theOtherList.add(Integer.valueOf(2));
    assertEquals(KoLDatabase.calculateTotal(theOtherList), 6);
    theOtherList.clear();
    theOtherList.add(null);
    theOtherList.add(Integer.valueOf(3));
    theOtherList.add(Integer.valueOf(1));
    theOtherList.add(null);
    theOtherList.add(Integer.valueOf(2));
    assertEquals(KoLDatabase.calculateTotal(theOtherList), 6);
  }

  @Test
  public void itShouldAverageAListOfOnlyIntegers() {
    theOtherList.clear();
    theOtherList.add(Integer.valueOf(3));
    theOtherList.add(Integer.valueOf(1));
    theOtherList.add(Integer.valueOf(2));
    assertEquals(KoLDatabase.calculateAverage(theOtherList), 2.0, 0.0);
  }
}
