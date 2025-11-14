package net.sourceforge.kolmafia.utilities;

import static org.junit.jupiter.api.Assertions.*;

import net.java.dev.spellcast.utilities.LockableListModel;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import org.junit.jupiter.api.Test;

public class LowerCaseEntryTest {

  @Test
  public void itShouldCreateWhatWasAskedFor() {
    LowerCaseEntry<String, String> lca = new LowerCaseEntry<>("Key", "Value");
    assertEquals("Key", lca.getKey());
    assertEquals("Value", lca.getValue());
    assertEquals("Value (Key)", lca.toString());
    LowerCaseEntry<String, String> lcb = new LowerCaseEntry<>("kEY", "vALUE");
    assertNotEquals(lca, lcb);
    assertEquals(lca.getLowerCase(), lcb.getLowerCase());
    assertNotEquals("value", lcb.getValue());
  }

  @Test
  public void itShouldChangeValueWhenAsked() {
    LowerCaseEntry<String, String> lca = new LowerCaseEntry<>("Key", "Value");
    LowerCaseEntry<String, String> lcb = new LowerCaseEntry<>("Key", "Value");
    assertEquals(lca, lcb);
    String sv = lcb.setValue("NotValue");
    assertEquals("Value", sv);
    assertNotEquals(lca, lcb);
  }

  @Test
  public void itShouldBuildAlist() {
    LockableListModel<LowerCaseEntry<Integer, String>> llm =
        LowerCaseEntry.createListModel(ItemDatabase.entrySet());
    assertNotNull(llm);
    LowerCaseEntry<Integer, String> lce = llm.get(50);
    assertEquals("knob goblin uberpants", lce.getLowerCase());
    String z = lce.setValue("Knob Goblin Panties");
    assertEquals("Knob Goblin Uberpants", z);
    assertEquals("knob goblin panties", lce.getLowerCase());
  }
}
