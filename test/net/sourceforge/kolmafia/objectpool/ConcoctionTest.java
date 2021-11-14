package net.sourceforge.kolmafia.objectpool;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.java.dev.spellcast.utilities.LockableListModel;
import net.sourceforge.kolmafia.persistence.ConcoctionDatabase;
import org.junit.jupiter.api.Test;

/* This test was triggered by a runtime error traced back to sorting usable concoctions that
said "Comparison method violates its general contract!"  But it has been replaced by the cli
command checkconcoctions for the contract checking portion.
 */

public class ConcoctionTest {

  // This test should never fail but may generate a error if data is introduced that is not
  // properly handled by the compareTo.
  @Test
  public void itShouldSortUsables() {
    LockableListModel<Concoction> usableList = ConcoctionDatabase.getUsables();
    int thing = usableList.getSize();
    usableList.sort();
    assertEquals(usableList.size(), thing);
  }

  @Test
  public void exerciseSameNames() {
    // Exercise compareTo and equals for Concoctions with the same name
    // Lazy way to get two Concoctions with the same name
    LockableListModel<Concoction> usables = ConcoctionDatabase.getUsables();
    List<Concoction> Eds = new ArrayList<>();
    Iterator<Concoction> ui = usables.iterator();
    while (ui.hasNext()) {
      Concoction x = ui.next();
      if (x.toString().contains("Eye of Ed")) {
        Eds.add(x);
      }
    }
    assertEquals(2, Eds.size());
    Concoction e1 = Eds.get(0);
    Concoction e2 = Eds.get(1);
    int c1 = e1.compareTo(e2);
    int c2 = e2.compareTo(e1);
    boolean b1 = e1.equals(e2);
    boolean b2 = e2.equals(e1);
    // they should not be equal by compareTo
    assertNotEquals(0, c1);
    // but they should be quasi symmetric
    assertEquals(c1, -c2);
    // they should not be equal by equals
    assertFalse(b1);
    assertEquals(b1, b2);
    assertFalse(e1 == e2);
  }
}
