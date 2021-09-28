package net.sourceforge.kolmafia.utilities;

import static org.junit.Assert.*;

import net.java.dev.spellcast.utilities.LockableListModel;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import org.junit.Test;

public class LowerCaseEntryTest {

  @Test
  public void itShouldCreateWhatWasAskedFor() {
    LowerCaseEntry<String, String> lca = new LowerCaseEntry<>("Key", "Value");
    assertEquals(lca.getKey(), "Key");
    assertEquals(lca.getValue(), "Value");
    assertEquals(lca.toString(), "Value (Key)");
    LowerCaseEntry<String, String> lcb = new LowerCaseEntry<>("kEY", "vALUE");
    assertNotEquals(lca, lcb);
    assertEquals(lca.getLowerCase(), lcb.getLowerCase());
    assertNotEquals(lcb.getValue(), "value");
  }

  @Test
  public void itShouldChangeValueWhenAsked() {
    LowerCaseEntry<String, String> lca = new LowerCaseEntry<>("Key", "Value");
    LowerCaseEntry<String, String> lcb = new LowerCaseEntry<>("Key", "Value");
    assertEquals(lca, lcb);
    String sv = lcb.setValue("NotValue");
    assertEquals(sv, "Value");
    assertNotEquals(lca, lcb);
  }

  @Test
  public void itShouldBuildAlist() {
    LockableListModel<LowerCaseEntry<Integer, String>> llm =
        LowerCaseEntry.createListModel(ItemDatabase.entrySet());
    assertNotNull(llm);
    LowerCaseEntry<Integer, String> lce = llm.get(50);
    assertEquals(lce.getLowerCase(), "knob goblin uberpants");
    String z = lce.setValue("Knob Goblin Panties");
    assertEquals(z, "Knob Goblin Uberpants");
    assertEquals(lce.getLowerCase(), "knob goblin panties");
  }
}
