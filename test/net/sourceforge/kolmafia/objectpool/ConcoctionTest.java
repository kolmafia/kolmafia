package net.sourceforge.kolmafia.objectpool;

import static org.junit.Assert.*;

import net.java.dev.spellcast.utilities.LockableListModel;
import net.sourceforge.kolmafia.persistence.ConcoctionDatabase;
import org.junit.Ignore;
import org.junit.Test;

/* This test was triggered by a runtime error traced back to sorting usable concoctions that
said "Comparison method violates its general contract!"  It is likely that the error was caused by
dynamic data, i.e. something that changed because a character left Ronin, but it can't hurt to verify
the comparison against static data.
 */

public class ConcoctionTest {

  // Helper method to force normalize Concoction comparisons to [-1, 0, 1] before testing
  private int sgn(int value) {
    return Integer.compare(value, 0);
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

  // tests the portion of the contract that says sgn(x.compareTo(y)) == -sgn(y.compareTo(x) and
  // (x.compareTo(y)==0) == (x.equals(y))
  @Test
  @Ignore("Nested for-loops are slow...")
  public void itShouldBeSymmetric() {
    LockableListModel<Concoction> first = ConcoctionDatabase.getUsables();
    LockableListModel<Concoction> second = ConcoctionDatabase.getUsables();
    for (Concoction acon : first) {
      for (Concoction bcon : second) {
        int x = acon.compareTo(bcon);
        int y = bcon.compareTo(acon);
        String msg = acon.toString() + " * " + bcon.toString();
        assertEquals(msg, sgn(x), -sgn(y));
        if (x == 0) assertEquals(msg, acon, bcon);
      }
    }
  }

  // x.compareTo(y)==0 implies
  //	  that sgn(x.compareTo(z)) == sgn(y.compareTo(z)), for all z.
  @Test
  @Ignore("Nested for-loops are slow...")
  public void itShouldBePreserveEquality() {
    LockableListModel<Concoction> first = ConcoctionDatabase.getUsables();
    LockableListModel<Concoction> second = ConcoctionDatabase.getUsables();
    LockableListModel<Concoction> third = ConcoctionDatabase.getUsables();
    for (Concoction acon : first) {
      for (Concoction bcon : second) {
        if (acon.compareTo(bcon) == 0) {
          for (Concoction ccon : third) {
            String msg = acon.toString() + " * " + bcon.toString() + " * " + ccon.toString();
            int x = sgn(acon.compareTo(ccon));
            int y = sgn(bcon.compareTo(ccon));
            assertEquals(msg, sgn(x), sgn(y));
          }
        }
      }
    }
  }

  // (x.compareTo(y)>0 && y.compareTo(z)>0) implies x.compareTo(z)>0.
  @Test
  @Ignore("Test takes too much resources.  Needs optimization")
  public void isBT() {
    LockableListModel<Concoction> first = ConcoctionDatabase.getUsables();
    first.sort();
    Concoction[] cons = first.toArray(new Concoction[0]);
    for (Concoction acon : cons) {
      for (Concoction bcon : cons) {
        int x = sgn(acon.compareTo(bcon));
        if (x > 0) {
          for (Concoction ccon : cons) {
            String msg = acon.toString() + " * " + bcon.toString() + " *" + ccon.toString();
            int y = sgn(bcon.compareTo(ccon));
            if (y > 0) assertTrue(msg, sgn(acon.compareTo(ccon)) > 0);
          }
        }
      }
    }
  }
}
