package net.sourceforge.kolmafia.utilities;

import static org.junit.Assert.*;
import org.junit.Test;

import net.sourceforge.kolmafia.persistence.ItemDatabase;

import net.java.dev.spellcast.utilities.LockableListModel;

public class LowerCaseEntryTest {

    @Test
    public void itShouldCreateWhatWasAskedFor() {
        LowerCaseEntry lca = new LowerCaseEntry("Key", "Value");
        assertEquals(lca.getKey(), "Key");
        assertEquals(lca.getValue(), "Value");
        assertEquals(lca.toString(), "Value (Key)");
        LowerCaseEntry lcb = new LowerCaseEntry("kEY", "vALUE");
        assertNotEquals(lca, lcb);
        assertEquals(lca.getLowerCase(), lcb.getLowerCase());
        assertNotEquals(lcb.getValue(), "value");
    }

    @Test
    public void itShouldChangeValueWhenAsked() {
        LowerCaseEntry lca = new LowerCaseEntry("Key", "Value");
        LowerCaseEntry lcb = new LowerCaseEntry("Key", "Value");
        assertEquals(lca, lcb);
        Object sv = lcb.setValue("NotValue");
        assertEquals(sv, "Value");
        assertNotEquals(lca, lcb);
    }

    @Test
    public void itShouldBuildAlist() {
        LockableListModel llm = LowerCaseEntry.createListModel(ItemDatabase.entrySet());
        assertNotNull(llm);
        Object e = llm.get(50);
        assertTrue(e instanceof LowerCaseEntry);
        LowerCaseEntry lce = (LowerCaseEntry) e;
        assertEquals(lce.getLowerCase(),"knob goblin uberpants");
        Object z = lce.setValue("Knob Goblin Panties");
        assertEquals(z.toString(), "Knob Goblin Uberpants");
        assertEquals(lce.getLowerCase(), "knob goblin panties");
    }
}
