/**
 * Copyright (c) 2005-2009, KoLmafia development team
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
import java.util.List;
import java.util.HashMap;

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
import net.sourceforge.kolmafia.request.BasementRequest;
import net.sourceforge.kolmafia.request.ClanRumpusRequest;
import net.sourceforge.kolmafia.request.HiddenCityRequest;
import net.sourceforge.kolmafia.request.RichardRequest;
import net.sourceforge.kolmafia.session.InventoryManager;
import net.sourceforge.kolmafia.utilities.FileUtilities;
import net.sourceforge.kolmafia.utilities.StringArray;
import net.sourceforge.kolmafia.utilities.StringUtilities;
import net.sourceforge.kolmafia.webui.DungeonDecorator;

public class AdventureDatabase
	extends KoLDatabase
{
	private static final LockableListModel adventures = new LockableListModel();
	private static final AdventureDatabase.AdventureArray allAdventures = new AdventureDatabase.AdventureArray();

	public static final ArrayList PARENT_LIST = new ArrayList();
	public static final HashMap PARENT_ZONES = new HashMap();
	public static final HashMap ZONE_DESCRIPTIONS = new HashMap();

	private static final StringArray[] adventureTable = new StringArray[ 4 ];
	private static final HashMap areaCombatData = new HashMap();
	private static final HashMap adventureLookup = new HashMap();
	private static final HashMap cloverLookup = new HashMap();
	private static final HashMap zoneLookup = new HashMap();

	private static final StringArray conditionsById = new StringArray();
	private static final StringArray bountiesById = new StringArray();

	private static final HashMap locationByBounty = new HashMap();

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

				AdventureDatabase.zoneLookup.put( name, zone );
				AdventureDatabase.adventureTable[ 0 ].add( zone );
				AdventureDatabase.adventureTable[ 1 ].add( location[ 0 ] + ".php" );
				AdventureDatabase.adventureTable[ 2 ].add( location[ 1 ] );
				AdventureDatabase.adventureTable[ 3 ].add( name );

				AdventureDatabase.cloverLookup.put( name, hasCloverAdventure ? Boolean.TRUE : Boolean.FALSE );
				if ( data.length > 4 )
				{
					int id = StringUtilities.parseInt( location[ 1 ] );
					if ( !data[ 4 ].equals( "" ) )
					{
						AdventureDatabase.conditionsById.set( id, data[ 4 ] );
					}
					if ( data.length > 5 && !data[ 5 ].equals( "" ) )
					{
						AdventureDatabase.bountiesById.set( id, data[ 5 ] );
					}				
				}
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

		AdventureDatabase.locationByBounty.clear();
		for ( int i = 0; i < AdventureDatabase.bountiesById.size(); ++i )
		{
			String bounty = AdventureDatabase.bountiesById.get( i );
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
			KoLAdventure adventure = AdventureDatabase.getAdventureByURL( "adventure.php?snarfblat=" + i + "&pwd" );
			AdventureDatabase.locationByBounty.put( bounty, adventure );
		}
	}

	public static final void addAdventure( final KoLAdventure location )
	{
		AdventureDatabase.adventures.add( location );
		AdventureDatabase.allAdventures.add( location );

		String url = location.getRequest().getURLString();
		int index = url.indexOf( "&pwd" );
		if ( index != -1 )
		{
			url = url.substring( 0, index ) +
				url.substring( index + 4 );
		}

		AdventureDatabase.adventureLookup.put( url, location );
		url = StringUtilities.singleStringReplace( url, "snarfblat=", "adv=" );
		AdventureDatabase.adventureLookup.put( url, location );
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

		int index = adventureURL.indexOf( "&confirm=on" );
		if ( index != -1 )
		{
			adventureURL = adventureURL.substring( 0, index ) +
				adventureURL.substring( index + 11 );
		}
		index = adventureURL.indexOf( "&pwd" );
		if ( index != -1 )
		{
			adventureURL = adventureURL.substring( 0, index ) +
				adventureURL.substring( index + 4 );
		}

		KoLAdventure location = (KoLAdventure) AdventureDatabase.adventureLookup.get( adventureURL );
		return location == null ||
			location.getRequest() instanceof ClanRumpusRequest ||
			location.getRequest() instanceof RichardRequest ? null : location;
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
	
	public static final String getZone( final String location )
	{
		return (String) zoneLookup.get( location );
	}

	public static final KoLAdventure getBountyLocation( final int itemId )
	{
		return AdventureDatabase.getBountyLocation( ItemDatabase.getItemName( itemId ) );
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
		KoLAdventure adventure = AdventureDatabase.getBountyLocation( name );
		if ( adventure == null )
			return null;
                return AdventureDatabase.getBounty( adventure );
	}

	public static final AdventureResult getBounty( final KoLAdventure adventure )
	{
		int adventureId = StringUtilities.parseInt( adventure.getAdventureId() );
		String bounty = AdventureDatabase.bountiesById.get( adventureId );
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
			if ( bounty != null && !bounty.equals( "" ) && ItemDatabase.getItemId( bounty.substring( bounty.indexOf( " " ) ).trim() ) == bountyId )
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

	public static final LockableListModel getDefaultConditionsList( final KoLAdventure adventure, LockableListModel list )
	{
		String [] conditions = AdventureDatabase.getDefaultConditions( adventure ).split( "\\|" );
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
		else if ( urlString.startsWith( "hiddencity.php" ) )
		{
			return null;
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
			if ( urlString.indexOf( "mine=3" ) != -1 )
			{
				return "Anemone Mine (Mining)";
			}
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

		return null;
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

	public static class AdventureArray
	{
		private String[] nameArray = new String[0];
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
			this.nameList.add( value.getAdventureName().toLowerCase() );
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

				KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "Multiple matches against " + adventureName + "." );
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
