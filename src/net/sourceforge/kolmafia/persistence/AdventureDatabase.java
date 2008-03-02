/**
 * Copyright (c) 2005-2008, KoLmafia development team
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

package net.sourceforge.kolmafia.persistence;

import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.TreeMap;

import net.java.dev.spellcast.utilities.LockableListModel;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.AreaCombatData;
import net.sourceforge.kolmafia.KoLAdventure;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLDatabase;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.StaticEntity;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.session.CustomCombatManager;
import net.sourceforge.kolmafia.utilities.FileUtilities;
import net.sourceforge.kolmafia.utilities.StringArray;
import net.sourceforge.kolmafia.utilities.StringUtilities;
import net.sourceforge.kolmafia.webui.DungeonDecorator;

import net.sourceforge.kolmafia.request.BasementRequest;
import net.sourceforge.kolmafia.request.ClanRumpusRequest;

public class AdventureDatabase
	extends KoLDatabase
{
	private static final LockableListModel adventures = new LockableListModel();
	private static final AdventureDatabase.AdventureArray allAdventures = new AdventureDatabase.AdventureArray();

	public static final ArrayList PARENT_LIST = new ArrayList();
	public static final TreeMap PARENT_ZONES = new TreeMap();
	public static final TreeMap ZONE_DESCRIPTIONS = new TreeMap();

	private static final StringArray[] adventureTable = new StringArray[ 4 ];
	private static final TreeMap areaCombatData = new TreeMap();
	private static final TreeMap adventureLookup = new TreeMap();
	private static final TreeMap cloverLookup = new TreeMap();

	private static final StringArray conditionsById = new StringArray();
	static
	{
		AdventureDatabase.conditionsById.set( 15, "1 mosquito larva" ); // Spooky Forest
		AdventureDatabase.conditionsById.set( 16, "1 fairy gravy" ); // Haiku Dungeon
		AdventureDatabase.conditionsById.set( 17, "3 choiceadv" ); // Hidden Temple
		AdventureDatabase.conditionsById.set( 18, "1 gnollish toolbox, 1 tires" ); // Degrassi Knoll
		AdventureDatabase.conditionsById.set( 19, "1 dungeoneer's dungarees" ); // Limerick Dungeon
		AdventureDatabase.conditionsById.set( 21, "1 smart skull" ); // Pre-Cyrpt Cemetary
		AdventureDatabase.conditionsById.set( 22, "1 disembodied brain" ); // Fernswarthy's Tower
		AdventureDatabase.conditionsById.set( 25, "1 beer goggles" ); // Typical Tavern
		AdventureDatabase.conditionsById.set( 26, "outfit" ); // Hippy Camp
		AdventureDatabase.conditionsById.set( 27, "outfit" ); // Frat House
		AdventureDatabase.conditionsById.set( 30, "1 Pine-Fresh air freshener" ); // Bat Hole Entryway
		AdventureDatabase.conditionsById.set( 31, "3 sonar-in-a-biscuit, 1 broken skull" ); // Guano Junction
		AdventureDatabase.conditionsById.set( 33, "1 enchanted bean" ); // Beanbat Chamber
		AdventureDatabase.conditionsById.set( 38, "1 plus sign" ); // Enormous Greater-Than Sign
		AdventureDatabase.conditionsById.set( 39, "1 dead mimic" ); // Dungeons of Doom
		AdventureDatabase.conditionsById.set( 41, "outfit" ); // Knob Goblin Treasury
		AdventureDatabase.conditionsById.set( 42, "outfit, 1 Knob Goblin perfume" ); // Knob Goblin Harem
		AdventureDatabase.conditionsById.set( 47, "1 annoying pitchfork" ); // Bugbear Pens
		AdventureDatabase.conditionsById.set( 48, "1 inexplicably glowing rock" ); // Spooky Gravy Barrow
		AdventureDatabase.conditionsById.set( 60, "6 goat cheese" ); // Goatlet
		AdventureDatabase.conditionsById.set( 61, "outfit" ); // Itznotyerzitz Mine
		AdventureDatabase.conditionsById.set( 62, "1 frigid ninja stars" ); // Ninja Snowmen
		AdventureDatabase.conditionsById.set( 63, "outfit" ); // eXtreme Slope
		AdventureDatabase.conditionsById.set( 66, "outfit" ); // Pirate Cove
		AdventureDatabase.conditionsById.set( 73, "1 digital key" ); // 8-Bit Realm
		AdventureDatabase.conditionsById.set( 75, "1 wussiness potion, 1 ruby W, 1 dodecagram" ); // Friar's Quest 1
		AdventureDatabase.conditionsById.set( 76, "1 box of birthday candles" ); // Friar's Quest 2
		AdventureDatabase.conditionsById.set( 77, "1 eldritch butterknife" ); // Friar's Quest 3
		AdventureDatabase.conditionsById.set( 79, "1 Azazel's unicorn, 1 Azazel's lollipop, 1 Azazel's tutu" ); 	// Friar's gate
		AdventureDatabase.conditionsById.set( 80, "1 64735 scroll, 1 lowercase N" ); // Valley Beyond Orc Chasm
		AdventureDatabase.conditionsById.set( 81, "1 metallic A, 1 S.O.C.K." ); // Fantasy Airship
		AdventureDatabase.conditionsById.set( 82, "2 choiceadv, castle map items, 1 heavy D" ); // Giant's Castle
		AdventureDatabase.conditionsById.set( 83, "1 Richard's star key, 1 star hat, 1 star crossbow" ); // Hole in the Sky
		AdventureDatabase.conditionsById.set( 85, "outfit" ); // Cola Battlefield
		AdventureDatabase.conditionsById.set( 100, "1 bird rib, 1 lion oil" ); // Whitey's Grove
		AdventureDatabase.conditionsById.set( 102, "1 chef's hat" ); // Haunted Kitchen
		AdventureDatabase.conditionsById.set( 103, "1 Spookyraven gallery key" ); // Haunted Conservatory
		AdventureDatabase.conditionsById.set( 105, "1 pool cue, 1 handful of hand chalk" ); // Haunted Billiards
		AdventureDatabase.conditionsById.set( 107, "1 fancy bath salts" ); // Haunted Bathroom
		AdventureDatabase.conditionsById.set( 108, "1 Lord Spookyraven's spectacles" ); // Haunted Bedroom
		AdventureDatabase.conditionsById.set( 111, "1 sunken eyes, 1 broken wings, 1 black market map" ); // Black Forest
		AdventureDatabase.conditionsById.set( 112, "1 spider web" ); // Sleazy Back Alley
		AdventureDatabase.conditionsById.set( 113, "1 razor-sharp can lid" ); // Haunted Pantry
		AdventureDatabase.conditionsById.set( 114, "1 chef's hat" ); // Knob Outskirts
		AdventureDatabase.conditionsById.set( 119, "1 stunt nuts, 1 I Love Me Vol I" ); // Palindome
		AdventureDatabase.conditionsById.set( 124, "1 carved wooden wheel" );	// The Upper Chamber
		AdventureDatabase.conditionsById.set( 127, "1 filthworm hatchling scent gland" ); // The Hatching Chamber
		AdventureDatabase.conditionsById.set( 128, "1 filthworm drone scent gland" ); // The Feeding Chamber
		AdventureDatabase.conditionsById.set( 129, "1 filthworm royal guard scent gland" ); // The Guards Chamber
		AdventureDatabase.conditionsById.set( 136, "5 barrel of gunpowder" );		// Sonofa Beach
		AdventureDatabase.conditionsById.set( 151, "1 choiceadv" ); // Stately Pleasure Dome
		AdventureDatabase.conditionsById.set( 152, "1 choiceadv" ); // Mouldering Mansion
		AdventureDatabase.conditionsById.set( 153, "1 choiceadv" ); // Rogue Windmill
		AdventureDatabase.conditionsById.set( 158, "1 Mizzenmast mop, 1 ball polish, 1 rigging shampoo" );	// F'c'le
	}

	private static final StringArray bountiesById = new StringArray();
	static
	{
		// First set of bounties
		AdventureDatabase.bountiesById.set( 15, "3 triffid bark" ); // Spooky Forest
		AdventureDatabase.bountiesById.set( 18, "6 oily rag" ); // Degrassi Knoll
		AdventureDatabase.bountiesById.set( 20, "20 empty greasepaint tube" ); // Fun House
		AdventureDatabase.bountiesById.set( 22, "7 wilted lettuce" ); // Fernswarthy's Tower
		AdventureDatabase.bountiesById.set( 30, "14 pink bat eye" ); // Bat Hole Entryway
		AdventureDatabase.bountiesById.set( 112, "13 hobo gristle" ); // Sleazy Back Alley
		AdventureDatabase.bountiesById.set( 113, "13 shredded can label" ); // Haunted Pantry
		AdventureDatabase.bountiesById.set( 114, "8 bloodstained briquette" ); // Knob Outskirts

		// Second set of bounties
		AdventureDatabase.bountiesById.set( 26, "11 greasy dreadlock" ); // Hippy Camp
		AdventureDatabase.bountiesById.set( 27, "34 empty aftershave bottle" ); // Frat House
		AdventureDatabase.bountiesById.set( 41, "17 bundle of receipts" ); // Knob Goblin Treasury
		AdventureDatabase.bountiesById.set( 45, "4 callused fingerbone" ); // South of the Border
		AdventureDatabase.bountiesById.set( 50, "40 broken petri dish" ); // Knob Goblin Laboratory
		AdventureDatabase.bountiesById.set( 66, "5 vial of pirate sweat" ); // Pirate Cove
		AdventureDatabase.bountiesById.set( 100, "8 white lint" ); // Whitey's Grove
		AdventureDatabase.bountiesById.set( 39, "6 worthless piece of yellow glass" ); // Dungeon's of Doom

		// Third set of bounties
		AdventureDatabase.bountiesById.set( 60, "40 billy idol" ); // Goatlet
		AdventureDatabase.bountiesById.set( 62, "40 coal button" ); // Ninja Snowmen
		AdventureDatabase.bountiesById.set( 80, "5 sammich crust" ); // Valley Beyond Orc Chasm
		AdventureDatabase.bountiesById.set( 81, "5 burned-out arcanodiode" ); // Fantasy Airship
		AdventureDatabase.bountiesById.set( 82, "5 discarded pacifier" ); // Giant's Castle
		AdventureDatabase.bountiesById.set( 83, "6 sticky stardust" ); // Hole in the Sky
		AdventureDatabase.bountiesById.set( 106, "11 non-Euclidean hoof" ); // Haunted Gallery
		AdventureDatabase.bountiesById.set( 120, "20 disintegrating cork" ); // Haunted Wine Cellar
	}

	private static final TreeMap locationByBounty = new TreeMap();

	static
	{
		for ( int i = 0; i < AdventureDatabase.adventureTable.length; ++i )
		{
			AdventureDatabase.adventureTable[ i ] = new StringArray();
		}

		AdventureDatabase.refreshZoneTable();
		AdventureDatabase.refreshAdventureTable();
		AdventureDatabase.refreshCombatsTable();
		AdventureDatabase.refreshAdventureList();
	}

	public static final AdventureResult[] WOODS_ITEMS = new AdventureResult[ 12 ];
	static
	{
		for ( int i = 0; i < 12; ++i )
		{
			AdventureDatabase.WOODS_ITEMS[ i ] = new AdventureResult( i + 1, 1 );
		}
	}

	// Some adventures don't actually cost a turn
	public static final String[] FREE_ADVENTURES =
	{
		"Rock-a-bye larva",
		"Cobb's Knob lab key"
	};

	public static final void refreshZoneTable()
	{
		if ( !AdventureDatabase.ZONE_DESCRIPTIONS.isEmpty() )
		{
			return;
		}

		BufferedReader reader = FileUtilities.getVersionedReader( "zonelist.txt", KoLConstants.ZONELIST_VERSION );
		if ( reader == null )
		{
			return;
		}

		String[] data;

		while ( ( data = FileUtilities.readData( reader ) ) != null )
		{
			if ( data.length >= 3 )
			{
				AdventureDatabase.PARENT_ZONES.put( data[ 0 ], data[ 1 ] );

				if ( !AdventureDatabase.PARENT_LIST.contains( data[ 1 ] ) )
				{
					AdventureDatabase.PARENT_LIST.add( data[ 1 ] );
				}

				AdventureDatabase.ZONE_DESCRIPTIONS.put( data[ 0 ], data[ 2 ] );
			}
		}

		try
		{
			reader.close();
		}
		catch ( Exception e )
		{
			// This should not happen.  Therefore, print
			// a stack trace for debug purposes.

			StaticEntity.printStackTrace( e );
		}
	}

	public static final void refreshAdventureTable()
	{
		BufferedReader reader = FileUtilities.getVersionedReader( "adventures.txt", KoLConstants.ADVENTURES_VERSION );
		if ( reader == null )
		{
			return;
		}

		for ( int i = 0; i < AdventureDatabase.adventureTable.length; ++i )
		{
			AdventureDatabase.adventureTable[ i ].clear();
		}

		String[] data;

		while ( ( data = FileUtilities.readData( reader ) ) != null )
		{
			if ( data.length > 3 )
			{
				String zone = data[ 0 ];
				String[] location = data[ 1 ].split( "=" );
				boolean hasCloverAdventure = data[ 2 ].equals( "true" );
				String name = data[ 3 ];

				if ( AdventureDatabase.PARENT_ZONES.get( zone ) == null )
				{
					RequestLogger.printLine( "Adventure area \"" + name + "\" has invalid zone: \"" + zone + "\"" );
					continue;
				}

				AdventureDatabase.adventureTable[ 0 ].add( zone );
				AdventureDatabase.adventureTable[ 1 ].add( location[ 0 ] + ".php" );
				AdventureDatabase.adventureTable[ 2 ].add( location[ 1 ] );
				AdventureDatabase.adventureTable[ 3 ].add( name );

				AdventureDatabase.cloverLookup.put( name, hasCloverAdventure ? Boolean.TRUE : Boolean.FALSE );
			}
		}

		try
		{
			reader.close();
		}
		catch ( Exception e )
		{
			// This should not happen.  Therefore, print
			// a stack trace for debug purposes.

			StaticEntity.printStackTrace( e );
		}
	}

	public static final void refreshCombatsTable()
	{
		AdventureDatabase.areaCombatData.clear();

		BufferedReader reader = FileUtilities.getVersionedReader( "combats.txt", KoLConstants.COMBATS_VERSION );
		if ( reader == null )
		{
			return;
		}

		String[] data;

		String[] adventures = AdventureDatabase.adventureTable[ 3 ].toArray();

		while ( ( data = FileUtilities.readData( reader ) ) != null )
		{
			if ( data.length > 1 )
			{
				if ( !AdventureDatabase.validateAdventureArea( data[ 0 ], adventures ) )
				{
					RequestLogger.printLine( "Invalid adventure area: \"" + data[ 0 ] + "\"" );
					continue;
				}

				int combats = StringUtilities.parseInt( data[ 1 ] );
				// There can be an ultra-rare monster even if
				// there are no other combats
				AreaCombatData combat = new AreaCombatData( combats );
				for ( int i = 2; i < data.length; ++i )
				{
					combat.addMonster( data[ i ] );
				}
				AdventureDatabase.areaCombatData.put( data[ 0 ], combat );
			}
		}

		try
		{
			reader.close();
		}
		catch ( Exception e )
		{
			// This should not happen.  Therefore, print
			// a stack trace for debug purposes.

			StaticEntity.printStackTrace( e );
		}
	}

	public static final void refreshAdventureList()
	{
		AdventureDatabase.adventures.clear();
		AdventureDatabase.allAdventures.clear();
		AdventureDatabase.adventureLookup.clear();

		for ( int i = 0; i < AdventureDatabase.adventureTable[ 0 ].size(); ++i )
		{
			AdventureDatabase.addAdventure( AdventureDatabase.getAdventure( i ) );
		}

		for ( int i = 0; i < AdventureDatabase.bountiesById.size(); ++i )
		{
			String bounty = AdventureDatabase.bountiesById.get( i );
			if ( bounty == null || bounty.equals( "" ) )
			{
				continue;
			}

			bounty = StringUtilities.getCanonicalName( bounty.substring( bounty.indexOf( " " ) + 1 ) );
			AdventureDatabase.locationByBounty.put(
				bounty, AdventureDatabase.getAdventureByURL( "adventure.php?snarfblat=" + i ) );
		}
	}

	public static final void refreshAdventureList( final String desiredZone )
	{
		KoLAdventure location;
		AdventureDatabase.adventures.clear();

		for ( int i = 0; i < AdventureDatabase.adventureTable[ 0 ].size(); ++i )
		{
			location = AdventureDatabase.allAdventures.get( i );
			if ( location.getParentZone().equals( desiredZone ) )
			{
				AdventureDatabase.adventures.add( location );
			}
		}
	}

	public static final void addAdventure( final KoLAdventure location )
	{
		AdventureDatabase.adventures.add( location );
		AdventureDatabase.allAdventures.add( location );

		String url = location.getRequest().getURLString();

		AdventureDatabase.adventureLookup.put( url, location );
		AdventureDatabase.adventureLookup.put( url + "&override=on", location );
		url = StringUtilities.singleStringReplace( url, "snarfblat=", "adv=" );
		AdventureDatabase.adventureLookup.put( url, location );
		AdventureDatabase.adventureLookup.put( url + "&override=on", location );
	}

	public static final boolean validateZone( final String zoneName, final String locationId )
	{
		if ( zoneName == null || locationId == null )
		{
			return true;
		}

		// Special handling of the bat zone.

		return true;
	}

	public static final LockableListModel getAsLockableListModel()
	{
		if ( AdventureDatabase.adventures.isEmpty() )
		{
			AdventureDatabase.refreshAdventureList();
		}

		return AdventureDatabase.adventures;
	}

	public static final KoLAdventure getAdventureByURL( String adventureURL )
	{
		if ( AdventureDatabase.adventureLookup.isEmpty() )
		{
			AdventureDatabase.refreshAdventureList();
		}

		if ( adventureURL.startsWith( "/" ) )
		{
			adventureURL = adventureURL.substring( 1 );
		}

		if ( adventureURL.startsWith( "basement.php" ) )
		{
			return null;
		}

		if ( adventureURL.startsWith( "dungeon.php" ) )
		{
			return null;
		}

		if ( adventureURL.startsWith( "sewer.php" ) )
		{
			return adventureURL.indexOf( "doodit" ) == -1 ?
				(KoLAdventure) AdventureDatabase.adventureLookup.get( "sewer.php" ) :
				(KoLAdventure) AdventureDatabase.adventureLookup.get( "sewer.php?doodit=1" );
		}

		int subAdventureIndex = adventureURL.indexOf( "&subsnarfblat" );
		if ( subAdventureIndex != -1 )
		{
			adventureURL = adventureURL.substring( 0, subAdventureIndex );
		}

		KoLAdventure location = (KoLAdventure) AdventureDatabase.adventureLookup.get( adventureURL );
		return location == null || location.getRequest() instanceof ClanRumpusRequest ? null : location;
	}

	public static final KoLAdventure getAdventure( final String adventureName )
	{
		if ( AdventureDatabase.adventureLookup.isEmpty() )
		{
			AdventureDatabase.refreshAdventureList();
		}

		if ( adventureName == null || adventureName.equals( "" ) )
		{
			return null;
		}

		return AdventureDatabase.allAdventures.find( adventureName.equalsIgnoreCase( "sewer" ) ? "unlucky sewer" : adventureName );

	}

	private static final KoLAdventure getAdventure( final int tableIndex )
	{
		return new KoLAdventure(
			AdventureDatabase.adventureTable[ 0 ].get( tableIndex ),
			AdventureDatabase.adventureTable[ 1 ].get( tableIndex ),
			AdventureDatabase.adventureTable[ 2 ].get( tableIndex ),
			AdventureDatabase.adventureTable[ 3 ].get( tableIndex ) );
	}

	public static final KoLAdventure getBountyLocation( final int itemId )
	{
		return getBountyLocation( ItemDatabase.getItemName( itemId ) );
	}

	public static final KoLAdventure getBountyLocation( final String item )
	{
		return item == null ? null : (KoLAdventure) AdventureDatabase.locationByBounty.get( StringUtilities.getCanonicalName( item ) );
	}

	public static final AdventureResult getBounty( final int itemId )
	{
                String name = ItemDatabase.getItemName( itemId );
		if ( name == null )
			return null;
		KoLAdventure adventure = (KoLAdventure) AdventureDatabase.locationByBounty.get( StringUtilities.getCanonicalName( name ) );
		if ( adventure == null )
			return null;
		int adventureId = StringUtilities.parseInt( adventure.getAdventureId() );
		String bounty = AdventureDatabase.bountiesById.get( adventureId );
		if ( bounty == null )
			return null;

		int count = StringUtilities.parseInt( bounty.substring( 0, bounty.indexOf( " " ) ) );
		return new AdventureResult( name, count, false );
	}

	public static final AdventureResult currentBounty()
	{
		int bountyItem = Preferences.getInteger( "currentBountyItem" );
		return bountyItem == 0 ? null : AdventureDatabase.getBounty( bountyItem );
	}

	public static final String getDefaultConditions( final KoLAdventure adventure )
	{
		if ( adventure == null )
		{
			return "none";
		}

		// If it's not a standard adventure, return whatever
		// is known for that area.

		if ( !adventure.getFormSource().startsWith( "adventure" ) )
		{
			if ( adventure.getFormSource().startsWith( "sewer" ) )
			{
				return "2 worthless item";
			}

			return "none";
		}

		// If you're currently doing a bounty, return
		// the item you need to hunt for.

		int adventureId = StringUtilities.parseInt( adventure.getAdventureId() );
		int bountyId = Preferences.getInteger( "currentBountyItem" );

		if ( bountyId != 0 )
		{
			String bounty = AdventureDatabase.bountiesById.get( adventureId );
			if ( !bounty.equals( "" ) && ItemDatabase.getItemId( bounty.substring( bounty.indexOf( " " ) ).trim() ) == bountyId )
			{
				return bounty;
			}
		}

		// If you're at the Friar's gate, return the steel
		// reward people are most likely looking for.

		if ( adventureId == 79 )
		{
			if ( KoLCharacter.canDrink() )
			{
				return KoLCharacter.getInebrietyLimit() > 15 ? "none" : "1 steel margarita";
			}

			if ( KoLCharacter.canEat() )
			{
				return KoLCharacter.getFullnessLimit() > 15 ? "none" : "1 steel lasagna";
			}

			return KoLCharacter.getSpleenLimit() > 15 ? "none" : "1 steel-scented air freshener";
		}

		// Otherwise, pull the condition out of the existing
		// table and return it.

		String conditions = AdventureDatabase.conditionsById.get( adventureId );
		if ( conditions != null && !conditions.equals( "" ) )
		{
			return conditions;
		}
		return "none";
	}

	public static final boolean isFreeAdventure( final String text )
	{
		for ( int i = 0; i < AdventureDatabase.FREE_ADVENTURES.length; ++i )
		{
			if ( text.indexOf( AdventureDatabase.FREE_ADVENTURES[ i ] ) != -1 )
			{
				return true;
			}
		}
		return false;
	}

	private static final boolean validateAdventureArea( final String area, final String[] areas )
	{
		for ( int i = 0; i < areas.length; ++i )
		{
			if ( area.equals( areas[ i ] ) )
			{
				return true;
			}
		}
		return false;
	}

	public static final AreaCombatData getAreaCombatData( String area )
	{
		// Strip out zone name if present
		int index = area.indexOf( ":" );
		if ( index != -1 )
		{
			area = area.substring( index + 2 );
		}

		// Get the combat data
		return (AreaCombatData) AdventureDatabase.areaCombatData.get( area );
	}

	public static final String getUnknownName( final String urlString )
	{
		if ( urlString.startsWith( "adventure.php" ) && urlString.indexOf( "snarfblat=122" ) != -1 )
		{
			return "Oasis in the Desert";
		}
		else if ( urlString.startsWith( "shore.php" ) && urlString.indexOf( "whichtrip=1" ) != -1 )
		{
			return "Muscle Vacation";
		}
		else if ( urlString.startsWith( "shore.php" ) && urlString.indexOf( "whichtrip=2" ) != -1 )
		{
			return "Mysticality Vacation";
		}
		else if ( urlString.startsWith( "shore.php" ) && urlString.indexOf( "whichtrip=3" ) != -1 )
		{
			return "Moxie Vacation";
		}
		else if ( urlString.startsWith( "guild.php?action=chal" ) )
		{
			return "Guild Challenge";
		}
		else if ( urlString.startsWith( "basement.php" ) )
		{
			return "Fernswarthy's Basement (Level " + BasementRequest.getBasementLevel() + ")";
		}
		else if ( urlString.startsWith( "dungeon.php" ) )
		{
			return "Daily Dungeon (Room " + DungeonDecorator.getDungeonRoom() + ")";
		}
		else if ( urlString.startsWith( "rats.php" ) )
		{
			return "Typical Tavern Quest";
		}
		else if ( urlString.startsWith( "barrel.php" ) )
		{
			return "Barrel Full of Barrels";
		}
		else if ( urlString.startsWith( "mining.php" ) )
		{
			return "Mining (In Disguise)";
		}
		else if ( urlString.startsWith( "arena.php" ) && urlString.indexOf( "action" ) != -1 )
		{
			return "Cake-Shaped Arena";
		}
		else if ( urlString.startsWith( "lair4.php?action=level1" ) )
		{
			return "Sorceress Tower: Level 1";
		}
		else if ( urlString.startsWith( "lair4.php?action=level2" ) )
		{
			return "Sorceress Tower: Level 2";
		}
		else if ( urlString.startsWith( "lair4.php?action=level3" ) )
		{
			return "Sorceress Tower: Level 3";
		}
		else if ( urlString.startsWith( "lair5.php?action=level1" ) )
		{
			return "Sorceress Tower: Level 4";
		}
		else if ( urlString.startsWith( "lair5.php?action=level2" ) )
		{
			return "Sorceress Tower: Level 5";
		}
		else if ( urlString.startsWith( "lair5.php?action=level3" ) )
		{
			return "Sorceress Tower: Level 6";
		}
		else if ( urlString.startsWith( "lair6.php?place=0" ) )
		{
			return "Sorceress Tower: Door Puzzles";
		}
		else if ( urlString.startsWith( "lair6.php?place=2" ) )
		{
			return "Sorceress Tower: Shadow Fight";
		}
		else if ( urlString.startsWith( "lair6.php?place=5" ) )
		{
			return "Sorceress Tower: Naughty Sorceress";
		}
		else if ( urlString.startsWith( "hiddencity.php" ) )
		{
			return "Hidden City: Unexplored Ruins";
		}

		return null;
	}

	/**
	 * Returns whether or not the current user has a ten-leaf clover.
	 *
	 * @return <code>true</code>
	 */

	public static boolean isPotentialCloverAdventure( String adventureName )
	{
		AdventureResult clover = ItemPool.get( ItemPool.TEN_LEAF_CLOVER, 1 );
		return KoLConstants.inventory.contains( clover ) &&
			cloverLookup.get( adventureName ) == Boolean.TRUE;
	}

	public static class AdventureArray
	{
		private final ArrayList nameList = new ArrayList();
		private final ArrayList internalList = new ArrayList();

		public KoLAdventure get( final int index )
		{
			if ( index < 0 || index > this.internalList.size() )
			{
				return null;
			}

			return (KoLAdventure) this.internalList.get( index );
		}

		public void add( final KoLAdventure value )
		{
			this.nameList.add( StringUtilities.getCanonicalName( value.getAdventureName() ) );
			this.internalList.add( value );
		}

		public KoLAdventure find( String adventureName )
		{
			int matchCount = 0;
			int adventureIndex = -1;

			adventureName = StringUtilities.getCanonicalName( CustomCombatManager.encounterKey( adventureName ) );

			// First, prefer adventures which start with the
			// provided substring. That failing, report all
			// matches.

			for ( int i = 0; i < this.size(); ++i )
			{
				if ( ( (String) this.nameList.get( i ) ).equals( adventureName ) )
				{
					return this.get( i );
				}

				if ( ( (String) this.nameList.get( i ) ).startsWith( adventureName ) )
				{
					++matchCount;
					adventureIndex = i;
				}
			}

			if ( matchCount > 1 )
			{
				for ( int i = 0; i < this.size(); ++i )
				{
					if ( ( (String) this.nameList.get( i ) ).startsWith( adventureName ) )
					{
						RequestLogger.printLine( (String) this.nameList.get( i ) );
					}
				}

				KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "Multiple matches against " + adventureName + "." );
				return null;
			}

			if ( matchCount == 1 )
			{
				return this.get( adventureIndex );
			}

			// Next, try substring matches when attempting to
			// find the adventure location. That failing,
			// report all matches.

			matchCount = 0;

			for ( int i = 0; i < this.size(); ++i )
			{
				if ( ( (String) this.nameList.get( i ) ).indexOf( adventureName ) != -1 )
				{
					++matchCount;
					adventureIndex = i;
				}
			}

			if ( matchCount > 1 )
			{
				for ( int i = 0; i < this.size(); ++i )
				{
					if ( ( (String) this.nameList.get( i ) ).indexOf( adventureName ) != -1 )
					{
						RequestLogger.printLine( (String) this.nameList.get( i ) );
					}
				}

				KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "Multiple matches against " + adventureName + "." );
				return null;
			}

			if ( matchCount == 1 )
			{
				return this.get( adventureIndex );
			}

			// Next, try to do fuzzy matching.  If it matches
			// exactly one area, use it.  Otherwise, if it
			// matches more than once, do nothing.

			for ( int i = 0; i < this.size(); ++i )
			{
				if ( StringUtilities.fuzzyMatches( (String) this.nameList.get( i ), adventureName ) )
				{
					++matchCount;
					adventureIndex = i;
				}
			}

			if ( matchCount > 1 )
			{
				for ( int i = 0; i < this.size(); ++i )
				{
					if ( StringUtilities.fuzzyMatches( (String) this.nameList.get( i ), adventureName ) )
					{
						RequestLogger.printLine( (String) this.nameList.get( i ) );
					}
				}

				KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "Multiple matches against " + adventureName + "." );
				return null;
			}

			if ( matchCount == 1 )
			{
				return this.get( adventureIndex );
			}

			return null;
		}

		public void clear()
		{
			this.nameList.clear();
			this.internalList.clear();
		}

		public int size()
		{
			return this.internalList.size();
		}
	}
}
