package net.sourceforge.kolmafia.utilities;

import static org.junit.Assert.*;
import org.junit.Test;

import java.util.HashSet;
import java.util.Map.Entry;

import net.sourceforge.kolmafia.persistence.ItemDatabase;

import net.java.dev.spellcast.utilities.LockableListModel;

public class LowerCaseEntryTest {

    private IntWrapper iw;

    @Test
    public void itShouldCreateWhatWasAskedFor() {
        LowerCaseEntry lca = new LowerCaseEntry("Key", "Value");
        assertEquals(lca.getKey(), "Key");
        assertEquals(lca.getValue(), "Value");
        assertEquals(lca.toString(), "Value (Key)");
        LowerCaseEntry lcb = new LowerCaseEntry("kEY", "vALUE");
        assertFalse(lca.equals(lcb));
        assertEquals(lca.getLowerCase(), lcb.getLowerCase());
        assertFalse(lca.equals("Value"));
    }

    @Test
    public void itShouldChangeValueWhenAsked() {
        LowerCaseEntry lca = new LowerCaseEntry("Key", "Value");
        LowerCaseEntry lcb = new LowerCaseEntry("Key", "Value");
        assertTrue(lca.equals(lcb));
        Object sv = lcb.setValue("NotValue");
        assertEquals(sv, "Value");
        assertFalse(lca.equals(lcb));
    }

    @Test
    public void itShouldBuildAlist() {
        LockableListModel llm = LowerCaseEntry.createListModel(ItemDatabase.entrySet());
        Object e = llm.get(50);
        assertTrue(e instanceof LowerCaseEntry);
        LowerCaseEntry lce = (LowerCaseEntry) e;
        assertEquals(lce.getLowerCase(),"knob goblin uberpants");
        Object z = lce.setValue("Knob Goblin Panties");
        assertEquals(z.toString(), "Knob Goblin Uberpants");
        assertEquals(lce.getLowerCase(), "knob goblin panties");
    }
}
