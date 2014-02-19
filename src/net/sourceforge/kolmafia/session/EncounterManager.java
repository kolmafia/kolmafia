/**
 * Copyright (c) 2005-2014, KoLmafia development team
 * http://kolmafia.sourceforge.net/
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *  [1] Redistributions of source code must retain the above copyright
 *      notice, this list of conditions and the following disclaimer.
 *  [2] Redistributions in binary form must reproduce the above copyright
 *      notice, this list of conditions and the following disclaimer in
 *      the documentation and/or other materials provided with the
 *      distribution.
 *  [3] Neither the name "KoLmafia" nor the names of its contributors may
 *      be used to endorse or promote products derived from this software
 *      without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package net.sourceforge.kolmafia.session;

import java.io.BufferedReader;
import java.util.ArrayList;

import net.sourceforge.kolmafia.KoLAdventure;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.AdventureDatabase;
import net.sourceforge.kolmafia.persistence.QuestDatabase;
import net.sourceforge.kolmafia.persistence.QuestDatabase.Quest;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.utilities.FileUtilities;

public abstract class EncounterManager
{
	// Types of special encounters
	public enum EncounterType
	{
		NONE,
		STOP,
		SEMIRARE,
		CLOVER,
		GLYPH,
		TURTLE,
		SEAL,
		FIST,
		BORIS,
		BADMOON,
		BUGBEAR,
	}

	public static class Encounter
	{
		String location;
		EncounterType encounterType;
		String encounter;

		public Encounter( String[] row )
		{
			location = row[ 0 ];
			encounterType = EncounterType.valueOf( row[ 1 ]  );
			encounter = row[ 2 ];
		}

		public String getLocation()
		{
			return this.location;
		}

		public EncounterType getEncounterType()
		{
			return this.encounterType;
		}

		public String getEncounter()
		{
			return this.encounter;
		}
	};

	private static Encounter[] specialEncounters;

	static
	{
		resetEncounters();
	}

	private static void resetEncounters()
	{
		BufferedReader reader = FileUtilities.getVersionedReader( "encounters.txt", KoLConstants.ENCOUNTERS_VERSION );

		ArrayList<Encounter> encounters = new ArrayList<Encounter>();
		String[] data;

		while ( ( data = FileUtilities.readData( reader ) ) != null )
		{
			if ( !AdventureDatabase.validateAdventureArea( data[ 0 ] ) )
			{
				RequestLogger.printLine( "Invalid adventure area: \"" + data[ 0 ] + "\"" );
				continue;
			}
			
			encounters.add( new Encounter( data ) );
		}

		specialEncounters = encounters.toArray( new Encounter[ encounters.size() ] );
	}

	/**
	 * Utility method used to register a given adventure in the running adventure summary.
	 */

	public void registerAdventure( final KoLAdventure adventureLocation )
	{
		EncounterManager.registerAdventure( adventureLocation.getAdventureName() );
	}

	public static void registerAdventure( final String adventureName )
	{
		if ( adventureName == null )
		{
			return;
		}

		RegisteredEncounter previousAdventure = (RegisteredEncounter) KoLConstants.adventureList.lastElement();

		if ( previousAdventure != null && previousAdventure.name.equals( adventureName ) )
		{
			++previousAdventure.encounterCount;
			KoLConstants.adventureList.set( KoLConstants.adventureList.size() - 1, previousAdventure );
		}
		else
		{
			KoLConstants.adventureList.add( new RegisteredEncounter( null, adventureName ) );
		}
	}

	public static final Encounter findEncounter( final String encounterName )
	{
		for ( int i = 0; i < specialEncounters.length; ++i )
		{
			Encounter encounter = specialEncounters[ i ];
			if ( encounterName.equalsIgnoreCase( encounter.encounter ) )
			{
				return encounter;
			}
		}

		return null;
	}

	public static final EncounterType encounterType( final String encounterName )
	{
		Encounter encounter = EncounterManager.findEncounter( encounterName );
		return EncounterManager.encounterType( encounter, encounterName );
	}

	private static final EncounterType encounterType( final Encounter encounter, final String encounterName )
	{
		return  encounter != null ?
			encounter.encounterType :
			BadMoonManager.specialAdventure( encounterName ) ?
			EncounterType.BADMOON :
			EncounterType.NONE;
	}

	public static final String findEncounterForLocation( final String locationName, final EncounterType type )
	{
		for ( int i = 0; i < specialEncounters.length; ++i )
		{
			Encounter encounter = specialEncounters[ i ];
			if ( locationName.equalsIgnoreCase( encounter.location ) && type.equals( encounter.encounterType ) )
			{
				return encounter.encounter;
			}
		}

		return null;
	}

	public static final boolean isAutoStop( final String encounterName )
	{
		if ( encounterName.equals( "Under the Knife" ) && Preferences.getString( "choiceAdventure21" ).equals( "2" ) )
		{
			return false;
		}

		EncounterType encounterType = encounterType( encounterName );
		return encounterType == EncounterType.STOP ||
		       encounterType == EncounterType.GLYPH ||
		       encounterType == EncounterType.BADMOON;
	}

	public static boolean isRomanticEncounter( String responseText )
	{
		return responseText.contains( "hear a wolf whistle" ) ||
		       responseText.contains( "you feel the hairs" );
	}

	// Used to ignore special monsters re-encountered via copying
	public static boolean ignoreSpecialMonsters = false;

	public static void ignoreSpecialMonsters()
	{
		ignoreSpecialMonsters = true;
	}

	private static void recognizeEncounter( final String encounterName, final String responseText )
	{
		Encounter encounter = EncounterManager.findEncounter( encounterName );
		EncounterType encounterType = EncounterManager.encounterType( encounter, encounterName );

		if ( encounterType == EncounterType.BUGBEAR )
		{
			BugbearManager.registerEncounter( encounter, responseText );
			return;
		}

		// You stop for a moment to catch your breath, and possibly a
		// cold, and hear a wolf whistle from behind you. You spin
		// around and see <monster> that looks suspiciously like the
		// ones you shot with a love arrow earlier.

		if ( encounterType == EncounterType.SEMIRARE &&
		     !ignoreSpecialMonsters &&
		     !EncounterManager.isRomanticEncounter( responseText ) )
		{
			KoLCharacter.registerSemirare();
			return;
		}

		if ( encounterType == EncounterType.NONE )
		{
			return;
		}

		if ( encounterType == EncounterType.BADMOON )
		{
			BadMoonManager.registerAdventure( encounterName );
		}

		if ( encounterType == EncounterType.STOP || encounterType == EncounterType.BORIS || encounterType == EncounterType.GLYPH || encounterType == EncounterType.BADMOON )
		{
			GoalManager.checkAutoStop( encounterName );
		}
	}

	/**
	 * Utility. The method used to register a given encounter in the running adventure summary.
	 */

	public static void registerEncounter( String encounterName, final String encounterType, final String responseText )
	{
		encounterName = encounterName.trim();

		handleSpecialEncounter( encounterName, responseText );
		recognizeEncounter( encounterName, responseText );

		RegisteredEncounter[] encounters = new RegisteredEncounter[ KoLConstants.encounterList.size() ];
		KoLConstants.encounterList.toArray( encounters );

		for ( int i = 0; i < encounters.length; ++i )
		{
			if ( encounters[ i ].name.equals( encounterName ) )
			{
				++encounters[ i ].encounterCount;

				// Manually set to force repainting in GUI
				KoLConstants.encounterList.set( i, encounters[ i ] );
				return;
			}
		}

		KoLConstants.encounterList.add( new RegisteredEncounter( encounterType, encounterName ) );
	}

	public static void handleSpecialEncounter( final String encounterName, final String responseText )
	{
		if ( encounterName.equalsIgnoreCase( "Cheetahs Never Lose" ) )
		{
			if ( InventoryManager.hasItem( ItemPool.BAG_OF_CATNIP ) )
			{
				ResultProcessor.processItem( ItemPool.BAG_OF_CATNIP, -1 );
			}
			return;
		}

		if ( encounterName.equalsIgnoreCase( "Summer Holiday" ) )
		{
			if ( InventoryManager.hasItem( ItemPool.HANG_GLIDER ) )
			{
				ResultProcessor.processItem( ItemPool.HANG_GLIDER, -1 );
				QuestDatabase.setQuestProgress( Quest.CITADEL, "step5" );
			}
			return;
		}

		if ( encounterName.equalsIgnoreCase( "Step Up to the Table, Put the Ball in Play" ) )
		{
			if ( InventoryManager.hasItem( ItemPool.CARONCH_DENTURES ) )
			{
				ResultProcessor.processItem( ItemPool.CARONCH_DENTURES, -1 );
				QuestDatabase.setQuestIfBetter( Quest.PIRATE, "step4" );
			}

			if ( InventoryManager.hasItem( ItemPool.FRATHOUSE_BLUEPRINTS ) )
			{
				ResultProcessor.processItem( ItemPool.FRATHOUSE_BLUEPRINTS, -1 );
			}
			return;
		}

		if ( encounterName.equalsIgnoreCase( "Granny, Does Your Dogfish Bite?" ) )
		{
			if ( InventoryManager.hasItem( ItemPool.GRANDMAS_MAP ) )
			{
				ResultProcessor.processItem( ItemPool.GRANDMAS_MAP, -1 );
			}
			return;
		}

		if ( encounterName.equalsIgnoreCase( "Meat For Nothing and the Harem for Free" ) )
		{
			Preferences.setBoolean( "_treasuryEliteMeatCollected", true );
			return;
		}

		if ( encounterName.equalsIgnoreCase( "Finally, the Payoff" ) )
		{
			Preferences.setBoolean( "_treasuryHaremMeatCollected", true );
			return;
		}

		if ( encounterName.equals( "Faction Traction = Inaction" ) )
		{
			Preferences.setInteger( "booPeakProgress", 98 );
			return;
		}
	}

	private static class RegisteredEncounter
		implements Comparable<RegisteredEncounter>
	{
		private final String type;
		private final String name;
		private final String stringform;
		private int encounterCount;

		public RegisteredEncounter( final String type, final String name )
		{
			this.type = type;
			// The name is likely a substring of a page load, so storing it
			// as-is would keep the entire page in memory.
			this.name = new String( name );

			this.stringform = type == null ? name : type + ": " + name;
			this.encounterCount = 1;
		}

		@Override
		public String toString()
		{
			return "<html>" + this.stringform + " (" + this.encounterCount + ")</html>";
		}

		public int compareTo( final RegisteredEncounter o )
		{
			if ( !( o instanceof RegisteredEncounter ) || o == null )
			{
				return -1;
			}

			if ( this.type == null || ( (RegisteredEncounter) o ).type == null || this.type.equals( ( (RegisteredEncounter) o ).type ) )
			{
				return this.name.compareToIgnoreCase( ( (RegisteredEncounter) o ).name );
			}

			return this.type.equals( "Combat" ) ? 1 : -1;
		}
	}
}
