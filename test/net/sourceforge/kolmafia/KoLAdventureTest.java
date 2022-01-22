package net.sourceforge.kolmafia;

import static internal.helpers.CompareContractValidator.checkForContractViolations;
import static org.junit.jupiter.api.Assertions.*;

import internal.helpers.CompareContractValidator;
import java.util.List;
import net.java.dev.spellcast.utilities.LockableListModel;
import net.sourceforge.kolmafia.persistence.AdventureDatabase;
import org.junit.jupiter.api.Test;

class KoLAdventureTest {

  @Test
  public void comparableShouldHonorContract() {
    LockableListModel<KoLAdventure> kolAdventures = AdventureDatabase.getAsLockableListModel();
    List<CompareContractValidator.Violator> badOnes = checkForContractViolations(kolAdventures);
    assertEquals(0, badOnes.size(), "Contract not met.");
  }
}
