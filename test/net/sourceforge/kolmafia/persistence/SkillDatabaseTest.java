package net.sourceforge.kolmafia.persistence;

import net.sourceforge.kolmafia.preferences.Preferences;
import org.junit.Test;

import static org.junit.Assert.*;

public class SkillDatabaseTest {
    //This is just one simple test to verify before and after behavior for an implicit narrowing cast
    //that was replaced by an alternative calculation.
    @Test
    public void itShouldCalculateCostCorrectlyAsAFunctionOfCasts() {
        Preferences.setInteger("_stackLumpsUses", 0);
        assertEquals(SkillDatabase.stackLumpsCost(), 11);
        Preferences.setInteger("_stackLumpsUses", 1);
        //assertEquals(SkillDatabase.stackLumpsCost(), 111);
        Preferences.setInteger("_stackLumpsUses", 2);
        //assertEquals(SkillDatabase.stackLumpsCost(), 1111);
    }

}