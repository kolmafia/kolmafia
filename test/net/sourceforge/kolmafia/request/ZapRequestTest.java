package net.sourceforge.kolmafia.request;

import static org.junit.Assert.*;

import net.java.dev.spellcast.utilities.LockableListModel;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.session.InventoryManager;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Test;

public class ZapRequestTest {

    @After
    public void after() {
        KoLCharacter.reset(false);}

    //Copied from a Maximizer Test.  Good candidate for a library that supports tests.
    private void loadInventory(String jsonInventory)
    {
        try
        {
            InventoryManager.parseInventory( new JSONObject( jsonInventory ) );
        }
        catch ( JSONException e )
        {
            fail( "Inventory parsing failed." );
        }
    }

    private boolean arrayHas(String[] array, String in) {
        for (String s : array) {
            if (s.equals(in)) return true;
        }
        return false;
    }

    @Test
    public void itShouldBuildARequestUnderSeveralCases() {
        //with null
        ZapRequest zr = new ZapRequest(null);
        assertNotNull(zr);
        //with unzappable item but no wand
        AdventureResult accord = new AdventureResult(42, 1, false);
        zr = new ZapRequest(accord);
        assertNotNull(zr);
        //with Zappable item (baconstone) but no wand
        AdventureResult bacon = new AdventureResult(705, 1, false);
        zr = new ZapRequest(bacon);
        assertNotNull(zr);
        //get a wand so can zap
        loadInventory( "{\"1268\": \"1\"}" );
        zr = new ZapRequest(accord);
        assertNotNull(zr);
        zr = new ZapRequest(bacon);
        assertNotNull(zr);
    }

    @Test
    public void itShouldHaveOtherZapData() {
        //Zap baconstone with wand. both in inventory
        AdventureResult bacon = new AdventureResult(705, 1, false);
        loadInventory("{\"705\": \"1\",\"1268\": \"1\"}");
        LockableListModel<AdventureResult> items = ZapRequest.getZappableItems();
        assertTrue(items.contains(bacon));
        String[] zapg = ZapRequest.getZapGroup(705);
        assertTrue(arrayHas(zapg, "baconstone"));
        assertTrue(arrayHas(zapg, "hamethyst"));
        assertTrue(arrayHas(zapg, "porquoise"));
        assertFalse(arrayHas(zapg, "xyzzy"));
        zapg = ZapRequest.getZapGroup(42);
        assertFalse(arrayHas(zapg, "hermit permit"));
    }
}
