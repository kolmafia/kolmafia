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

package net.sourceforge.kolmafia.persistence;

import java.io.BufferedReader;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.StringTokenizer;

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

import net.sourceforge.kolmafia.persistence.BountyDatabase;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.request.ClanRumpusRequest;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.request.RelayRequest;
import net.sourceforge.kolmafia.request.RichardRequest;

import net.sourceforge.kolmafia.session.EncounterManager;
import net.sourceforge.kolmafia.session.EncounterManager.EncounterType;
import net.sourceforge.kolmafia.session.InventoryManager;

import net.sourceforge.kolmafia.utilities.FileUtilities;
import net.sourceforge.kolmafia.utilities.StringArray;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class AdventureDatabase
	extends KoLDatabase
{
	private static final Pattern SNARF_PATTERN = Pattern.compile( "snarfblat=(\\d+)" );
	private static final Pattern MINE_PATTERN = Pattern.compile( "mine=(\\d+)" );
	private static final LockableListModel<KoLAdventure> adventures = new LockableListModel<KoLAdventure>();
	private static final AdventureArray allAdventures = new AdventureArray();

	public static final ArrayList<String> PARENT_LIST = new ArrayList<String>();
	public static final HashMap<String, String> PARENT_ZONES = new HashMap<String, String>();
	public static final HashMap<String, String> ZONE_DESCRIPTIONS = new HashMap<String, String>();

	private static final StringArray[] adventureTable = new StringArray[ 4 ];
	private static final HashMap<String, AreaCombatData> areaCombatData = new HashMap<String, AreaCombatData>();
	private static final HashMap<String, KoLAdventure> adventureLookup = new HashMap<String, KoLAdventure>();
	private static final HashMap<String, String> environmentLookup = new HashMap<String, String>();
	private static final HashMap<String, String> zoneLookup = new HashMap<String, String>();
	private static final HashMap<String, String> conditionLookup = new HashMap<String, String>();
	private static final HashMap<String, String> bountyLookup = new HashMap<String, String>();
	private static final HashMap<String, Integer> statLookup = new HashMap<String, Integer>();
	private static final HashMap<String, Integer> depthLookup = new HashMap<String, Integer>();

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

			String environment = null;
			int stat = -1;
			int depth = -1;
			StringTokenizer tokens = new StringTokenizer( data[ 2 ], " " );
			while ( tokens.hasMoreTokens() )
			{
				String option = tokens.nextToken();
				if ( option.equals( "Env:" ) )
				{
					environment = tokens.nextToken();
				}
				else if ( option.equals( "Stat:" ) )
				{
					stat = StringUtilities.parseInt( tokens.nextToken() );
				}
				else if ( option.equals( "Depth:" ) )
				{
					depth = StringUtilities.parseInt( tokens.nextToken() );
				}
			}

			String name = new String( data[ 3 ] );

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

			if ( environment == null )
			{
				RequestLogger.printLine( name + " is missing environment data" );
			}
			AdventureDatabase.environmentLookup.put( name, environment );

			AdventureDatabase.statLookup.put( name, stat );

			AdventureDatabase.depthLookup.put( name, depth );

			if ( data.length <= 4 )
			{
				continue;
			}

			if ( !data[ 4 ].equals( "" ) )
			{
				AdventureDatabase.conditionLookup.put( name, new String( data[ 4 ] ) );
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
					String monsterName = data[ i ];
					combat.addMonster( monsterName );
					// Does it drop a bounty, if so add it to the bounty lookup by area
					// Trim any trailing ":" and following text
					int colonIndex = data[ i ].indexOf( ":" );
					if ( colonIndex > 0 )
					{
						monsterName = monsterName.substring( 0, colonIndex );
					}
					String bountyName = BountyDatabase.getNameByMonster( monsterName );
					if ( bountyName != null )
					{
						AdventureDatabase.bountyLookup.put( data[ 0 ], bountyName );
					}
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

	public static final LockableListModel<KoLAdventure> getAsLockableListModel()
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

		// Visiting the basement counts as an adventure
		if ( adventureURL.startsWith( "basement.php" ) )
		{
			return AdventureDatabase.adventureLookup.get( "basement.php" );
		}

		// Visiting the tavern cellar might count as an adventure
		if ( adventureURL.startsWith( "cellar.php" ) )
		{
			String explore = GenericRequest.extractField( adventureURL, "explore" );
			if ( explore == null )
			{
				return null;
			}
			return AdventureDatabase.adventureLookup.get( "cellar.php" );
		}

		// Mining in disguise count as adventures.
		if ( adventureURL.startsWith( "mining.php" ) )
		{
			String mine = GenericRequest.extractField( adventureURL, "mine" );
			if ( mine == null )
			{
				return null;
			}
			return AdventureDatabase.adventureLookup.get( "mining.php?" + mine );
		}

		// Adventuring in the barracks after the Nemesis has been defeated
		if ( adventureURL.startsWith( "volcanoisland.php" ) && adventureURL.contains( "action=tuba" ) )
		{
			return AdventureDatabase.getAdventure( "The Island Barracks" );
		}

		// Adventuring in the Lower Chamber
		if ( adventureURL.contains( "action=pyramid_state" ) )
		{
			return AdventureDatabase.getAdventure( "The Lower Chambers" );
		}

		// Adventuring in the Summoning Chamber
		if ( adventureURL.contains( "action=manor4_chamber" ) )
		{
			return AdventureDatabase.getAdventure( "Summoning Chamber" );
		}

		adventureURL = RelayRequest.removeConfirmationFields( adventureURL );
		adventureURL = GenericRequest.removeField( adventureURL, "pwd" );
		adventureURL = GenericRequest.removeField( adventureURL, "blech" );
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

	public static final AdventureResult getBounty( final KoLAdventure adventure )
	{
		String adventureName = adventure.getAdventureName();
		String bounty = AdventureDatabase.bountyLookup.get( adventureName );
		if ( bounty == null || bounty.equals( "" ) )
		{
			return null;
		}
		int count = BountyDatabase.getNumber( bounty );
		return new AdventureResult( bounty, count );
	}

	public static final String getDefaultConditions( final KoLAdventure adventure )
	{
		if ( adventure == null )
		{
			return "none";
		}

		// If you're currently doing a bounty, +1 filthy lucre.

		String adventureName = adventure.getAdventureName();
		String bounty = AdventureDatabase.bountyLookup.get( adventureName );

		if ( bounty != null && !bounty.equals( "" ) )
		{
			String easyBountyId = Preferences.getString( "currentEasyBountyItem" );
			if ( !easyBountyId.equals( "" ) )
			{
				if ( bounty.equals( easyBountyId.substring( 0, easyBountyId.indexOf( ":" ) ) ) )
				{
					return "+1 filthy lucre";
				}
			}

			String hardBountyId = Preferences.getString( "currentHardBountyItem" );
			if ( !hardBountyId.equals( "" ) )
			{
				if ( bounty.equals( hardBountyId.substring( 0, hardBountyId.indexOf( ":" ) ) ) )
				{
					return "+1 filthy lucre";
				}
			}

			String specialBountyId = Preferences.getString( "currentSpecialBountyItem" );
			if ( !specialBountyId.equals( "" ) )
			{
				if ( bounty.equals( specialBountyId.substring( 0, specialBountyId.indexOf( ":" ) ) ) )
				{
					return "+1 filthy lucre";
				}
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

	public static final LockableListModel<String> getDefaultConditionsList( final KoLAdventure adventure, LockableListModel<String> list )
	{
		String string = AdventureDatabase.getDefaultConditions( adventure );
		String [] conditions = string.split( "\\|" );
		if ( list == null )
		{
			list = new LockableListModel<String>();
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
			Matcher matcher = AdventureDatabase.SNARF_PATTERN.matcher( urlString );
			return matcher.find() ? "Unknown Adventure #" + matcher.group(1) : null;
		}
		else if ( urlString.startsWith( "cave.php" ) )
		{
			if ( urlString.contains( "action=sanctum" ) )
			{
				return "Nemesis Cave: Inner Sanctum";
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
		else if ( urlString.startsWith( "place.php" ) )
		{
			if ( urlString.contains( "whichplace=ioty2014_wolf" ) && 
				urlString.contains( "action=wolf_houserun" ) )
			{
				return "Unleash Your Inner Wolf";
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
			"The Haunted Conservatory",
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
		String encounter = EncounterManager.findEncounterForLocation( adventureName, EncounterType.CLOVER );
		return AdventureDatabase.hasClover() && encounter != null;
	}

	public static final String getEnvironment( String adventureName )
	{
		return AdventureDatabase.environmentLookup.get( adventureName );
	}

	public static final int getRecommendedStat( String adventureName )
	{
		return AdventureDatabase.statLookup.get( adventureName );
	}

	public static final int getWaterDepth( String adventureName )
	{
		return AdventureDatabase.depthLookup.get( adventureName );
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
