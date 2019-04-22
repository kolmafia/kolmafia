package net.sourceforge.kolmafia;

import static org.junit.Assert.*;

import net.sourceforge.kolmafia.Modifiers;
import net.sourceforge.kolmafia.maximizer.Maximizer;
import net.sourceforge.kolmafia.session.InventoryManager;
import net.sourceforge.kolmafia.session.EquipmentManager;

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

	@Test
	public void clubModifierDoesntAffectOffhand()
	{
		KoLCharacter.addAvailableSkill( "Double-Fisted Skull Smashing" );
		// 15 base + buffed mus.
		KoLCharacter.setStatPoints( 15, 225, 0, 0, 0, 0 );
		try
		{
			// 2 flaming crutch, 2 white sword, 1 dense meat sword.
			// Max required muscle to equip any of these is 15.
			InventoryManager.parseInventory( new JSONObject( "{\"473\": \"2\", \"269\": \"2\", \"1728\": \"1\"}" ) );
		}
		catch ( JSONException e )
		{
			fail( "Inventory parsing failed." );
		}
		assertTrue( "Can equip white sword", EquipmentManager.canEquip(269) );
		assertTrue( "Can equip flaming crutch", EquipmentManager.canEquip(473) );
		assertTrue( Maximizer.maximize( "mus, club", 0, 0, true ) );
		// Should equip 1 flaming crutch, 1 white sword.
		assertEquals( "Muscle as expected.",
					  2, Modifiers.getNumericModifier( "Generated", "_spec", "Muscle" ), 0.01 );
		assertEquals( "Hot damage as expected.",
					  3, Modifiers.getNumericModifier( "Generated", "_spec", "Hot Damage" ), 0.01 );
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
