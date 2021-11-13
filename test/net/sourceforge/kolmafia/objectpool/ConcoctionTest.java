package net.sourceforge.kolmafia.objectpool;

import static org.junit.jupiter.api.Assertions.*;

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
}
