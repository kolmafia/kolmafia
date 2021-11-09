package net.sourceforge.kolmafia.objectpool;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashMap;
import java.util.Map;
import net.java.dev.spellcast.utilities.LockableListModel;
import net.sourceforge.kolmafia.persistence.ConcoctionDatabase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/* This test was triggered by a runtime error traced back to sorting usable concoctions that
said "Comparison method violates its general contract!"
 */

public class ConcoctionTest {

  static final Map<Integer, Concoction> idsAndMaps = new HashMap<>();
  static int maxIndex;
  static String msg;
  static int[][] result;

  // Helper method to force normalize Concoction comparisons to [-1, 0, 1] before testing
  private static int sgn(int value) {
    return Integer.compare(value, 0);
  }

  @BeforeAll
  private static void buildIndexAndData() {
    LockableListModel<Concoction> usables = ConcoctionDatabase.getUsables();
    maxIndex = usables.getSize();
    int i = 0;
    for (Concoction con : usables) {
      idsAndMaps.put(i, con);
      i++;
    }
    result = new int[maxIndex][maxIndex];
    for (i = 0; i < maxIndex; i++) {
      for (int j = 0; j < maxIndex; j++) {
        result[i][j] = sgn(idsAndMaps.get(i).compareTo(idsAndMaps.get(j)));
      }
    }
  }

  // This test should never fail but may generate a error if data is introduced that is not
  // properly handled by the compareTo.
  @Test
  public void itShouldSortUsables() {
    LockableListModel<Concoction> usableList = ConcoctionDatabase.getUsables();
    int thing = usableList.getSize();
    usableList.sort();
    assertEquals(usableList.size(), thing);
  }

  // sgn(x.compareTo(y)) == -sgn(y.compareTo(x)
  @Test
  public void compareToShouldBeQuasiSymmetric() {
    for (int i = 0; i < maxIndex; i++) {
      for (int j = 0; j < maxIndex; j++) {
        msg = "comparing (quasi symmetry)" + idsAndMaps.get(i) + "and " + idsAndMaps.get(j);
        assertEquals(sgn(result[i][j]), -sgn(result[j][i]), msg);
      }
    }
  }

  // tests the portion of the contract that says (x.compareTo(y)==0) == (x.equals(y))
  @Test
  public void compareToShouldBeEqualForEquals() {
    for (int i = 0; i < maxIndex; i++) {
      msg = "comparing (equality)" + idsAndMaps.get(i) + "and " + idsAndMaps.get(i);
      assertEquals(0, result[i][i], msg);
      for (int j = 0; j < maxIndex; j++) {
        if (result[i][j] == 0) {
          msg = "comparing (equality)" + idsAndMaps.get(i) + "and " + idsAndMaps.get(j);
          assertEquals(idsAndMaps.get(i), idsAndMaps.get(j), msg);
        }
      }
    }
  }

  // x.compareTo(y)==0 implies
  //	  that sgn(x.compareTo(z)) == sgn(y.compareTo(z)), for all z.
  @Test
  public void itShouldBeTransitive() {
    for (int i = 0; i < maxIndex; i++) {
      // Don't have to check whole matrix
      for (int j = i; j < maxIndex; j++) {
        if (result[i][j] > 0) {
          for (int k = 1; k < maxIndex; k++) {
            if (result[j][k] > 0) {
              msg =
                  "comparing (transitive)"
                      + idsAndMaps.get(i)
                      + "and "
                      + idsAndMaps.get(j)
                      + "and "
                      + idsAndMaps.get(k);
              assertTrue(result[i][k] > 0);
            }
          }
        }
      }
    }
  }
}
