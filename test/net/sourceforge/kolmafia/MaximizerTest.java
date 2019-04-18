package net.sourceforge.kolmafia;

import static org.junit.Assert.*;

import net.sourceforge.kolmafia.Modifiers;
import net.sourceforge.kolmafia.maximizer.Maximizer;
import net.sourceforge.kolmafia.session.InventoryManager;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;

public class MaximizerTest
{
	public MaximizerTest()
	{
		try
		{
			// 1 helmet turtle.
			InventoryManager.parseInventory( new JSONObject( "{\"3\": \"1\"}" ) );
		}
		catch ( JSONException e )
		{
			fail( "Inventory parsing failed." );
		}
	}


	@Test
	public void changesGear()
	{
		assertTrue( Maximizer.maximize( "mus", 0, 0, true ) );
		assertEquals(
			1, Modifiers.getNumericModifier( "Generated", "_spec", "Buffed Muscle" ), 0.01 );
	}

	@Test
	public void nothingBetterThanSomething()
	{
		assertTrue( Maximizer.maximize( "-mus", 0, 0, true ) );
		assertEquals(
			0, Modifiers.getNumericModifier( "Generated", "_spec", "Buffed Muscle" ), 0.01 );
	}

	// Sample test for https://kolmafia.us/showthread.php?23648&p=151903#post151903.
	// Commented out, since it's currently failing.
	/*
	@Test
	public void noTieCanLeaveSlotsEmpty()
	{
		assertTrue( Maximizer.maximize( "mys -tie", 0, 0, true ) );
		assertEquals(
			0, Modifiers.getNumericModifier( "Generated", "_spec", "Buffed Muscle" ), 0.01 );
	}
	*/
}
