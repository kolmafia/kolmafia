/**
 * Copyright (c) 2005-2013, KoLmafia development team
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
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.java.dev.spellcast.utilities.LockableListModel;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.AreaCombatData;
import net.sourceforge.kolmafia.KoLAdventure;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLDatabase;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.StaticEntity;

import net.sourceforge.kolmafia.objectpool.AdventurePool;
import net.sourceforge.kolmafia.objectpool.IntegerPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.request.BasementRequest;
import net.sourceforge.kolmafia.request.CharPaneRequest;
import net.sourceforge.kolmafia.request.ClanRumpusRequest;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.request.RelayRequest;
import net.sourceforge.kolmafia.request.RichardRequest;
import net.sourceforge.kolmafia.request.TavernRequest;

import net.sourceforge.kolmafia.session.InventoryManager;

import net.sourceforge.kolmafia.utilities.FileUtilities;
import net.sourceforge.kolmafia.utilities.StringArray;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class AdventureDatabase
	extends KoLDatabase
{
	private static final Pattern MINE_PATTERN = Pattern.compile( "mine=(\\d+)" );
	private static final LockableListModel adventures = new LockableListModel();
	private static final AdventureArray allAdventures = new AdventureArray();

	public static final ArrayList<String> PARENT_LIST = new ArrayList<String>();
	public static final HashMap<String, String> PARENT_ZONES = new HashMap<String, String>();
	public static final HashMap<String, String> ZONE_DESCRIPTIONS = new HashMap<String, String>();

	private static final StringArray[] adventureTable = new StringArray[ 4 ];
	private static final HashMap<String, AreaCombatData> areaCombatData = new HashMap<String, AreaCombatData>();
	private static final HashMap<String, KoLAdventure> adventureLookup = new HashMap<String, KoLAdventure>();
	private static final HashMap<String, Boolean> cloverLookup = new HashMap<String, Boolean>();
	private static final HashMap<String, String> environmentLookup = new HashMap<String, String>();
	private static final HashMap<String, String> zoneLookup = new HashMap<String, String>();
	private static final HashMap<String, String> conditionLookup = new HashMap<String, String>();
	private static final HashMap<String, String> bountyLookup = new HashMap<String, String>();

	private static final HashMap<String, KoLAdventure> locationByBounty = new HashMap<String, KoLAdventure>();

	// This should be removed eventually
	private static final String[][] OLD_LOCATIONS =
	{
		
		{
			"Desert (Unhydrated)",
			"The Arid, Extra-Dry Desert"
		},
		{
			"Desert (Ultrahydrated)",
			"The Arid, Extra-Dry Desert"
		},
		// Everything above here added to 16.1, remove after 16.2
	};

	// These should be removed eventually
	private static final ArrayList<String> convertOldNameList = new ArrayList<String>();
	private static final ArrayList<String> convertNewNameList = new ArrayList<String>();
	private static String[] convertOldNameArray = null;
	private static String[] convertNewNameArray = null;

	static
	{
		for ( int i = 0; i < OLD_LOCATIONS.length; i++ )
		{
			AdventureDatabase.convertOldNameList.add( i, StringUtilities.getCanonicalName( OLD_LOCATIONS[i][0] ) );
			AdventureDatabase.convertNewNameList.add( i, StringUtilities.getCanonicalName( OLD_LOCATIONS[i][1] ) );
		}
		convertOldNameArray = convertOldNameList.toArray( new String[ convertOldNameList.size() ] );
		convertNewNameArray = convertNewNameList.toArray( new String[ convertNewNameList.size() ] );
	}

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
				String zone = new String( data[ 0 ] );
				String parent = new String( data[ 1 ] );
				String description = new String( data[ 2 ] );

				AdventureDatabase.PARENT_ZONES.put( zone, parent );
				if ( !AdventureDatabase.PARENT_LIST.contains( parent ) )
				{
					AdventureDatabase.PARENT_LIST.add( parent );
				}

				AdventureDatabase.ZONE_DESCRIPTIONS.put( zone, description );
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
			if ( data.length <= 3 )
			{
				continue;
			}

			String zone = new String( data[ 0 ] );
			String[] location = data[ 1 ].split( "=" );
			boolean hasCloverAdventure = data[ 2 ].equals( "true" );
			String environment = new String( data[ 3 ] );
			String name = new String( data[ 4 ] );

			if ( AdventureDatabase.PARENT_ZONES.get( zone ) == null )
			{
				RequestLogger.printLine( "Adventure area \"" + name + "\" has invalid zone: \"" + zone + "\"" );
				continue;
			}

			AdventureDatabase.zoneLookup.put( name, zone );
			AdventureDatabase.adventureTable[ 0 ].add( zone );
			AdventureDatabase.adventureTable[ 1 ].add( location[ 0 ] + ".php" );
			AdventureDatabase.adventureTable[ 2 ].add( new String( location[ 1 ] ) );
			AdventureDatabase.adventureTable[ 3 ].add( name );

			AdventureDatabase.cloverLookup.put( name, hasCloverAdventure ? Boolean.TRUE : Boolean.FALSE );

			AdventureDatabase.environmentLookup.put( name, environment );

			if ( data.length <= 5 )
			{
				continue;
			}

			if ( !data[ 5 ].equals( "" ) )
			{
				AdventureDatabase.conditionLookup.put( name, new String( data[ 5 ] ) );
			}

			if ( data.length <= 6 )
			{
				continue;
			}

			if ( !data[ 6 ].equals( "" ) )
			{
				AdventureDatabase.bountyLookup.put( name, new String( data[ 6 ] ) );
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

		while ( ( data = FileUtilities.readData( reader ) ) != null )
		{
			if ( data.length > 1 )
			{
				if ( !AdventureDatabase.validateAdventureArea( data[ 0 ] ) )
				{
					RequestLogger.printLine( "Invalid adventure area: \"" + data[ 0 ] + "\"" );
					continue;
				}

				int combats = StringUtilities.parseInt( data[ 1 ] );
				// There can be an ultra-rare monster even if
				// there are no other combats
				AreaCombatData combat = new AreaCombatData( data[0], combats );
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

		AdventureDatabase.locationByBounty.clear();

		Iterator bountyIterator = AdventureDatabase.bountyLookup.entrySet().iterator();

		while ( bountyIterator.hasNext() )
		{
			Entry bountyEntry = (Entry) bountyIterator.next();

			String bounty = (String) bountyEntry.getValue();
			if ( bounty == null || bounty.equals( "" ) )
			{
				continue;
			}

			bounty = StringUtilities.getCanonicalName( bounty.substring( bounty.indexOf( " " ) + 1 ) );
			if ( AdventureDatabase.locationByBounty.get( bounty ) != null )
			{
				// Only store the first location
				continue;
			}

			String adventureName = (String) bountyEntry.getKey();
			KoLAdventure adventure = AdventureDatabase.getAdventure( adventureName );
			AdventureDatabase.locationByBounty.put( bounty, adventure );
		}
	}

	public static final void addAdventure( final KoLAdventure location )
	{
		AdventureDatabase.adventures.add( location );
		AdventureDatabase.allAdventures.add( location );

		GenericRequest request = location.getRequest();

		// This will force the URLstring to be reconstructed and the
		// correct password hash inserted when the request is run
		request.removeFormField( "pwd" );

		String url = request.getURLString();

		AdventureDatabase.adventureLookup.put( url, location );

		if ( url.contains( "snarfblat=" ) )
		{
			// The map of the Bat Hole has a bogus URL for the Boss Bat's lair
			if ( url.contains( "snarfblat=34" ) )
			{
				AdventureDatabase.adventureLookup.put( url + ";", location );
			}

			url = StringUtilities.singleStringReplace( url, "snarfblat=", "adv=" );
			AdventureDatabase.adventureLookup.put( url, location );
		}
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

		// Barrel smashes count as adventures.
		if ( adventureURL.startsWith( "barrel.php" ) )
		{
			return AdventureDatabase.adventureLookup.get( "barrel.php" );
		}

		// Mining in disguise count as adventures.
		if ( adventureURL.startsWith( "mining.php" ) )
		{
			String mine = AdventureDatabase.extractField( adventureURL, "mine" );
			if ( mine == null )
			{
				return null;
			}
			return AdventureDatabase.adventureLookup.get( "mining.php?" + mine );
		}

		adventureURL = RelayRequest.removeConfirmationFields( adventureURL );
		adventureURL = AdventureDatabase.removeField( adventureURL, "pwd" );
		adventureURL = StringUtilities.singleStringReplace( adventureURL, "action=ignorewarning&whichzone", "snarfblat" );

		KoLAdventure location = AdventureDatabase.adventureLookup.get( adventureURL );
		if ( location == null )
		{
			return null;
		}

		// *** Why exclude these?
		return  location.getRequest() instanceof ClanRumpusRequest ||
			location.getRequest() instanceof RichardRequest
			? null : location;
	}

	public static final String removeField( final String urlString, final String field )
	{
		int start = urlString.indexOf( field );
		if ( start == -1 )
		{
			return urlString;
		}

		int end = urlString.indexOf( "&", start );
		if ( end == -1 )
		{
			String prefix = urlString.substring( 0, start - 1 );
			return prefix;
		}

		String prefix = urlString.substring( 0, start );
		String suffix = urlString.substring( end + 1 );
		return prefix + suffix;
	}

	public static final String extractField( final String urlString, final String field )
	{
		int start = urlString.indexOf( field );
		if ( start == -1 )
		{
			return null;
		}

		int end = urlString.indexOf( "&", start );
		return ( end == -1 ) ?
			urlString.substring( start ) :
			urlString.substring( start, end );
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

		return AdventureDatabase.allAdventures.find( adventureName );

	}

	private static final KoLAdventure getAdventure( final int tableIndex )
	{
		return new KoLAdventure(
			AdventureDatabase.adventureTable[ 0 ].get( tableIndex ),
			AdventureDatabase.adventureTable[ 1 ].get( tableIndex ),
			AdventureDatabase.adventureTable[ 2 ].get( tableIndex ),
			AdventureDatabase.adventureTable[ 3 ].get( tableIndex ) );
	}

	public static final String getZone( final String location )
	{
		return zoneLookup.get( location );
	}

	public static final KoLAdventure getBountyLocation( final int itemId )
	{
		return AdventureDatabase.getBountyLocation( ItemDatabase.getItemName( itemId ) );
	}

	public static final KoLAdventure getBountyLocation( final String item )
	{
		return item == null ? null : AdventureDatabase.locationByBounty.get( StringUtilities.getCanonicalName( item ) );
	}

	public static final AdventureResult getBounty( final int itemId )
	{
		String name = ItemDatabase.getItemName( itemId );
		if ( name == null )
			return null;
		KoLAdventure adventure = AdventureDatabase.getBountyLocation( name );
		if ( adventure == null )
			return null;
		return AdventureDatabase.getBounty( adventure );
	}

	public static final AdventureResult getBounty( final KoLAdventure adventure )
	{
		String adventureName = adventure.getAdventureName();
		String bounty = AdventureDatabase.bountyLookup.get( adventureName );
		if ( bounty == null || bounty.equals( "" ) )
		{
			return null;
		}

		int space = bounty.indexOf( " " );
		int count = StringUtilities.parseInt( bounty.substring( 0, space ) );
		String name = bounty.substring( space + 1 );
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

		// If you're currently doing a bounty, return
		// the item you need to hunt for.

		String adventureName = adventure.getAdventureName();
		int bountyId = Preferences.getInteger( "currentBountyItem" );

		if ( bountyId != 0 )
		{
			String bounty = AdventureDatabase.bountyLookup.get( adventureName );
			if ( bounty != null && !bounty.equals( "" ) && ItemDatabase.getItemId( bounty.substring( bounty.indexOf( " " ) ).trim() ) == bountyId )
			{
				return bounty;
			}
		}

		String def = "none";

		// Pull the condition out of the table and return it.

		String conditions = AdventureDatabase.conditionLookup.get( adventureName );
		if ( conditions == null || conditions.equals( "" ) )
		{
			return def;
		}

		if ( !def.equals( "none" ) )
		{
			conditions = def + "|" + conditions;
		}

		return conditions;
	}

	public static final LockableListModel getDefaultConditionsList( final KoLAdventure adventure, LockableListModel list )
	{
		String string = AdventureDatabase.getDefaultConditions( adventure );
		String [] conditions = string.split( "\\|" );
		if ( list == null )
		{
			list = new LockableListModel();
		}
		else
		{
			list.clear();
		}

		for ( int i = 0; i < conditions.length; ++i )
		{
			list.add( conditions[i] );
		}

		return list;
	}

	public static final boolean isFreeAdventure( final String text )
	{
		for ( int i = 0; i < AdventureDatabase.FREE_ADVENTURES.length; ++i )
		{
			if ( text.contains( AdventureDatabase.FREE_ADVENTURES[ i ] ) )
			{
				return true;
			}
		}
		return false;
	}

	public static final boolean validateAdventureArea( final String area )
	{
		StringArray areas = AdventureDatabase.adventureTable[ 3 ];
		
		for ( int i = 0; i < areas.size(); ++i )
		{
			if ( area.equals( areas.get( i ) ) )
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
		return AdventureDatabase.areaCombatData.get( area );
	}

	public static final String getUnknownName( final String urlString )
	{
		if ( urlString.startsWith( "adventure.php" ) )
		{
			if ( urlString.contains( "snarfblat=" ) )
			{
				return "Unknown adventure";
			}
			return null;
		}
		else if ( urlString.startsWith( "cave.php" ) )
		{
			if ( urlString.contains( "action=sanctum" ) )
			{
				return "Nemesis Cave: Inner Sanctum";
			}
			return null;
		}
		else if ( urlString.startsWith( "dwarffactory.php" ) )
		{
			if ( urlString.contains( "action=ware" ) )
			{
				return "Dwarven Factory Warehouse";
			}
			return null;
		}
		else if ( urlString.startsWith( "guild.php?action=chal" ) )
		{
			return "Guild Challenge";
		}
		else if ( urlString.startsWith( "lair4.php" ) )
		{
			if ( urlString.contains( "action=level1" ) )
			{
				return "Sorceress Tower: Level 1";
			}
			else if ( urlString.contains( "action=level2" ) )
			{
				return "Sorceress Tower: Level 2";
			}
			else if ( urlString.contains( "action=level3" ) )
			{
				return "Sorceress Tower: Level 3";
			}
			return null;
		}
		else if ( urlString.startsWith( "lair5.php" ) )
		{
			if ( urlString.contains( "action=level1" ) )
			{
				return "Sorceress Tower: Level 4";
			}
			else if ( urlString.contains( "action=level2" ) )
			{
				return "Sorceress Tower: Level 5";
			}
			else if ( urlString.contains( "action=level3" ) )
			{
				return "Sorceress Tower: Level 6";
			}
			return null;
		}
		else if ( urlString.startsWith( "lair6.php" ) )
		{
			if ( urlString.contains( "place=0" ) )
			{
				return "Sorceress Tower: Door Puzzles";
			}
			else if ( urlString.contains( "place=2" ) )
			{
				return "Sorceress Tower: Shadow Fight";
			}
			else if ( urlString.contains( "place=5" ) )
			{
				return "Sorceress Tower: Naughty Sorceress";
			}
		}
		else if ( urlString.startsWith( "mining.php" ) )
		{
			if ( urlString.contains( "intro=1" ) )
			{
				return null;
			}
			Matcher matcher = AdventureDatabase.MINE_PATTERN.matcher( urlString );
			return matcher.find() ? "Unknown Mine #" + matcher.group(1) : null;
		}
		else if ( urlString.startsWith( "sea_merkin.php" ) )
		{
			if ( urlString.contains( "action=temple" ) )
			{
				return "Mer-kin Temple";
			}
		}
		else if ( urlString.startsWith( "town.php" ) )
		{
			if ( urlString.contains( "action=trickortreat" ) )
			{
				return "Trick-or-Treating";
			}
		}

		return null;
	}

	public static final Object [][] FISTCORE_SCROLLS =
	{
		// Adventure Zone
		// Adventure ID
		// Setting
		{
			"The Haiku Dungeon",
			IntegerPool.get( AdventurePool.HAIKU_DUNGEON ),
			"fistTeachingsHaikuDungeon",
		},
		{
			"The Poker Room",
			IntegerPool.get( AdventurePool.POKER_ROOM ),
			"fistTeachingsPokerRoom",
		},
		{
			"A Barroom Brawl",
			IntegerPool.get( AdventurePool.BARROOM_BRAWL ),
			"fistTeachingsBarroomBrawl",
		},
		{
			"Haunted Conservatory",
			IntegerPool.get( AdventurePool.HAUNTED_CONSERVATORY ),
			"fistTeachingsConservatory",
		},
		{
			"The Bat Hole Entrance",
			IntegerPool.get( AdventurePool.BAT_HOLE_ENTRYWAY ),
			"fistTeachingsBatHole",
		},
		{
			"The \"Fun\" House",
			IntegerPool.get( AdventurePool.FUN_HOUSE ),
			"fistTeachingsFunHouse",
		},
		{
			"Cobb's Knob Menagerie Level 2",
			IntegerPool.get( AdventurePool.MENAGERIE_LEVEL_2 ),
			"fistTeachingsMenagerie",
		},
		{
			"Pandamonium Slums",
			IntegerPool.get( AdventurePool.PANDAMONIUM_SLUMS ),
			"fistTeachingsSlums",
		},
		{
			"Frat House",
			IntegerPool.get( AdventurePool.FRAT_HOUSE ),
			"fistTeachingsFratHouse",
		},
		{
			"Road to the White Citadel",
			IntegerPool.get( AdventurePool.ROAD_TO_WHITE_CITADEL ),
			"fistTeachingsRoad",
		},
		{
			"Lair of the Ninja Snowmen",
			IntegerPool.get( AdventurePool.NINJA_SNOWMEN ),
			"fistTeachingsNinjaSnowmen",
		},
	};

	private static int fistcoreDataLocation( final Object[] data )
	{
		return ( data == null ) ? -1 : ((Integer) data[1] ).intValue();
	}

	private static String fistcoreDataSetting( final Object[] data )
	{
		return ( data == null ) ? null : ((String) data[2] );
	}

	private static Object[] fistcoreLocationToData( final int location )
	{
		for ( int i = 0; i < FISTCORE_SCROLLS.length; ++i )
		{
			Object [] data = FISTCORE_SCROLLS[i];
			int loc = fistcoreDataLocation( data );
			if ( location == loc )
			{
				return data;
			}
		}
		return null;
	}

	public static String fistcoreLocationToSetting( final int location )
	{
		return fistcoreDataSetting( fistcoreLocationToData( location ) );
	}

	/**
	 * Returns whether or not the user has a ten-leaf clover in inventory.
	 *
	 * @return <code>true</code>
	 */

	private static boolean hasClover()
	{
		return InventoryManager.getCount(ItemPool.TEN_LEAF_CLOVER ) > 0;
	}

	public static boolean isPotentialCloverAdventure( String adventureName )
	{
		return AdventureDatabase.hasClover() &&
		       cloverLookup.get( adventureName ) == Boolean.TRUE;
	}

	public static final String getEnvironment( String adventureName )
	{
		return AdventureDatabase.environmentLookup.get( adventureName );
	}

	public static class AdventureArray
	{
		private String[] nameArray = new String[0];
		private final ArrayList<String> nameList = new ArrayList<String>();
		private final ArrayList<KoLAdventure> internalList = new ArrayList<KoLAdventure>();

		public KoLAdventure get( final int index )
		{
			if ( index < 0 || index > this.internalList.size() )
			{
				return null;
			}

			return this.internalList.get( index );
		}

		public void add( final KoLAdventure value )
		{
			this.nameList.add( StringUtilities.getCanonicalName( value.getAdventureName() ) );
			this.internalList.add( value );
		}

		public KoLAdventure find( String adventureName )
		{
			if ( nameArray.length != nameList.size() )
			{
				nameArray = new String[ nameList.size() ];
				nameList.toArray( nameArray );
				Arrays.sort( nameArray );
			}

			List matchingNames = StringUtilities.getMatchingNames( nameArray, adventureName );

			// Beginning of block to remove when location name transition is done
			if ( matchingNames.isEmpty() )
			{
				matchingNames = StringUtilities.getMatchingNames( AdventureDatabase.convertOldNameArray, adventureName );
				if ( matchingNames.size() == 1 )
				{
					int index = AdventureDatabase.convertOldNameList.indexOf( (String) matchingNames.get( 0 ) );
					matchingNames.remove( 0 );
					String newName = AdventureDatabase.convertNewNameArray[ index ];
					matchingNames.add( newName.toLowerCase() );
					RequestLogger.printLine( "The string \"" + adventureName + "\" no longer "
						+ "matches a location name; use \"" + newName + "\" instead" );
				}
			}
			// End of block to remove when location name transition is done

			if ( matchingNames.size() > 1 )
			{
				for ( int i = 0; i < matchingNames.size(); ++i )
				{
					RequestLogger.printLine( (String) matchingNames.get( i ) );
				}

				KoLmafia.updateDisplay( MafiaState.ERROR, "Multiple matches against " + adventureName + "." );
				return null;
			}

			if ( matchingNames.size() == 1 )
			{
				String match = (String) matchingNames.get( 0 );
				return this.get( nameList.indexOf( match ) );
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
