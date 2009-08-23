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

package net.sourceforge.kolmafia.session;

import java.util.Iterator;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.StaticEntity;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public abstract class WumpusManager
{
	public static final String[] CHAMBERS =
	{
		"acrid",
		"breezy",
		"creepy",
		"dripping",
		"echoing",
		"fetid",
		"gloomy",
		"howling",
		"immense",
		null,	// j
		null,	// k
		"long",
		"moaning",
		"narrow",
		"ordinary",
		"pillared",
		"quiet",
		"round",
		"sparkling",
		null,	// t
		"underground",
		"vaulted",
		"windy",
		null,	// x
		null,	// y
		null,	// z
	};

	public static TreeMap rooms = new TreeMap();

	static
	{
		// Initialize all rooms to unknown
		for ( int i = 0; i < CHAMBERS.length; ++i )
		{
			String name = CHAMBERS[ i ];
			if ( name == null )
			{
				continue;
			}
			Room room = new Room( name );
			WumpusManager.rooms.put( name, room );
		}
	};

	// Current room
	public static Room current;

	// Locations of hazards
	public static Room bats1 = null;
	public static Room bats2 = null;
	public static Room pit1 = null;
	public static Room pit2 = null;
	public static Room wumpus = null;

	public static final int WARN_SAFE = 0;
	public static final int WARN_BATS = 1;
	public static final int WARN_PIT = 2;
	public static final int WARN_WUMPUS = 4;
	public static final int WARN_INDEFINITE = 8;
	public static final int WARN_ALL = 15;

	public static String[] WARN_STRINGS = new String[]
	{
		"safe",
		"definite bats",
		"definite pit",
		"ERROR: BATS AND PIT",
		"definite Wumpus",
		"ERROR: BATS AND WUMPUS",
		"ERROR: PIT AND WUMPUS",
		"ERROR: BATS, PIT, AND WUMPUS",

		"safe and unvisited",
		"possible bats",
		"possible pit",
		"possible bats or pit",
		"possible Wumpus",
		"possible bats or Wumpus",
		"possible pit or Wumpus",
		"possible bats, pit, or Wumpus",
	};

	public static String[] ELIMINATE_STRINGS = new String[]
	{
		"",
		"no bats",
		"no pit",
		"",
		"no Wumpus",
		"",
		"",
		"",
	};

	private static final Pattern ROOM_PATTERN = Pattern.compile( ">The (\\w+) Chamber<" );
	private static final Pattern LINK_PATTERN = Pattern.compile( "Enter the (\\w+) chamber" );

	public static void reset()
	{
		// Reset all rooms to initial state
		Iterator it = WumpusManager.rooms.values().iterator();
		while ( it.hasNext() )
		{
			Room room = (Room) it.next();
			room.reset();
		}

		// We are not currently in a room
		WumpusManager.current = null;

		// We don't know where any of the hazards are
		WumpusManager.bats1 = null;
		WumpusManager.bats2 = null;
		WumpusManager.pit1 = null;
		WumpusManager.pit2 = null;
		WumpusManager.wumpus = null;
	}

	// Deductions we made from visiting this room
	public static final StringBuffer deductions = new StringBuffer();

	// Types of deductions
	public static final int NONE = 0;
	public static final int VISIT = 1;
	public static final int LISTEN = 2;
	public static final int ELIMINATION = 3;
	public static final int DEDUCTION = 4;

	public static String[] DEDUCTION_STRINGS = new String[]
	{
		"None",
		"Visit",
		"Listen",
		"Elimination",
		"Deduction"
	};

	public static void visitChoice( String text )
	{
		WumpusManager.current = null;
		WumpusManager.deductions.setLength( 0 );

		if ( text == null )
		{
			return;
		}

		Matcher m = WumpusManager.ROOM_PATTERN.matcher( text );
		if ( !m.find() )
		{
			return;
		}
		
		String name = m.group( 1 ).toLowerCase();
		Room room = (Room) WumpusManager.rooms.get( name );
		if ( room == null )
		{
			// Internal error: unknown room name
			KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "Unknown room in Wumpus cave: the " + name + " chamber");
			return;
		}

		// If we have already visited this room, nothing more to do
		if ( room.visited )
		{
			// Re-check adjacent rooms, based on
			// discoveries since we last visited
			WumpusManager.deduce( room );
			return;
		}

		if ( text.indexOf( "Wait for the bats to drop you" ) != -1 )
		{
			WumpusManager.knownBats( room, VISIT );
			return;
		}

		if ( text.indexOf( "Thump" ) != -1 )
		{
			WumpusManager.knownPit( room, VISIT );
			return;
		}

		// Remember the room we are in. If we leave it and
		// find the Wumpus, we will not be on a choice page
		// and will need it in order to make deductions.
		WumpusManager.current = room;

		// Initialize the exits from the current room
		m = WumpusManager.LINK_PATTERN.matcher( text );
		for ( int i = 0; i < 3; ++i )
		{
			if ( !m.find() )
			{
				// Should not happen; there are always three
				// exits from a room.
				KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "Internal error: " + i + " exits found in " + WumpusManager.current );
				return;
			}
			String ename = m.group( 1 ).toLowerCase();
			Room exit = (Room) WumpusManager.rooms.get( ename );
			WumpusManager.current.setExit( i, exit );
			exit.addExit( WumpusManager.current );
		}

		WumpusManager.printDeduction( "Exits: " + WumpusManager.current.exitString() );

		WumpusManager.knownSafe( VISIT );

		// Basic logic: assume all rooms have all warnings initially.
		// Remove any warnings from linked rooms that aren't present
		// in the current room.

		int warn = WARN_INDEFINITE;
		if ( text.indexOf( "squeaking coming from somewhere nearby" ) != -1 )
		{
			warn |= WARN_BATS;
		}
		if ( text.indexOf( "hear a low roaring sound nearby" ) != -1 )
		{
			warn |= WARN_PIT;
		}
		if ( text.indexOf( "the Wumpus must be nearby" ) != -1 )
		{
			warn |= WARN_WUMPUS;
		}

		WumpusManager.current.setListen( warn );

		for ( int i = 0; i < 3; ++i )
		{
			Room exit = WumpusManager.current.getExit( i );
			WumpusManager.possibleHazard( exit, warn );
		}
		
		// Advanced logic: if only one of the linked rooms has a given
		// warning, promote that to a definite danger, and remove any
		// other warnings from that room.

		WumpusManager.deduce();

		// Doing this may make further deductions possible.

		WumpusManager.deduce();

		// I'm not sure if a 3rd deduction is actually possible, but it
		// doesn't hurt to try.

		WumpusManager.deduce();
	}

	private static void knownSafe( final int type  )
	{
		WumpusManager.knownSafe( WumpusManager.current, type );
	}

	private static void knownSafe( final Room room, final int type )
	{
		// Set Wumpinator flags for this room
		room.bat = 9;
		room.pit = 9;
		room.wumpus = 9;

		WumpusManager.knownHazard( room, WARN_SAFE, type );
	}

	private static void knownBats( final int type  )
	{
		WumpusManager.knownBats( WumpusManager.current, type );
	}

	private static void knownBats( final Room room, final int type	)
	{
		// Set Wumpinator flags for this room
		room.bat = 8;
		room.pit = 9;
		room.wumpus = 9;

		WumpusManager.knownHazard( room, WARN_BATS, type );

		// There are exactly two bat rooms per cave
		if ( WumpusManager.bats1 == null )
		{
			// We've just identified the first bat room
			WumpusManager.bats1 = room;
			return;
		}

		if ( WumpusManager.bats1 == room || WumpusManager.bats2 != null )
		{
			return;
		}

		// We've just identified the second bat room
		WumpusManager.bats2 = room;

		// Eliminate bats from rooms that have only "possible" bats
		WumpusManager.eliminateHazard( WARN_BATS );
	}

	private static void knownPit( final int type  )
	{
		WumpusManager.knownPit( WumpusManager.current, type );
	}

	private static void knownPit( final Room room, final int type )
	{
		// Set Wumpinator flags for this room
		room.bat = 9;
		room.pit = 8;
		room.wumpus = 9;

		WumpusManager.knownHazard( room, WARN_PIT, type );

		// There are exactly two pit rooms per cave
		if ( WumpusManager.pit1 == null )
		{
			// We've just identified the first pit room
			WumpusManager.pit1 = room;
			return;
		}

		if ( WumpusManager.pit1 == room || WumpusManager.pit2 != null )
		{
			return;
		}

		// We've just identified the second pit room
		WumpusManager.pit2 = room;

		// Eliminate pits from rooms that have only "possible" pit
		WumpusManager.eliminateHazard( WARN_PIT );
	}

	private static void knownWumpus( final int type	 )
	{
		WumpusManager.knownWumpus( WumpusManager.current, type );
	}

	private static void knownWumpus( final Room room, final int type )
	{
		// Set Wumpinator flags for this room
		room.bat = 9;
		room.pit = 9;
		room.wumpus = 8;

		WumpusManager.knownHazard( room, WARN_WUMPUS, type );

		// There is exactly one wumpus rooms per cave
		if ( WumpusManager.wumpus != null )
		{
			return;
		}

		// We've just identified the wumpus room
		WumpusManager.wumpus = room;

		// Eliminate wumpus from rooms that have only "possible" wumpus
		WumpusManager.eliminateHazard( WARN_WUMPUS );
	}
	
	private static void possibleHazard( final Room room, int warn )
	{
		// If we have already positively identified this room,
		// leave it alone
		if ( ( room.getHazards() & WARN_INDEFINITE ) == 0 )
		{
			return;
		}

		// We hear various sounds from an adjacent room. Mark
		// this room as a possible source. This is currently
		// only used to generate the Wumpinator string.

		if ( ( warn & WARN_BATS ) != 0 )
		{
			room.bat++;

			// If we know both bat rooms, no bats in this room.
			if ( WumpusManager.bats1 != null && WumpusManager.bats2 != null )
			{
				warn &= ~WARN_BATS;
			}
		}

		if ( ( warn & WARN_PIT ) != 0 )
		{
			room.pit++;

			// If we know both pit rooms, no pit in this room.
			if ( WumpusManager.pit1 != null && WumpusManager.pit2 != null )
			{
				warn &= ~WARN_PIT;
			}
		}

		if ( ( warn & WARN_WUMPUS ) != 0 )
		{
			room.wumpus++;

			// If we know the Wumpus room, no Wumpus in this room.
			if ( WumpusManager.wumpus != null )
			{
				warn &= ~WARN_WUMPUS;
			}
		}

		// Register possible hazard
		int oldStatus = room.setHazards( warn );
		int newStatus = room.getHazards();
		if ( oldStatus == newStatus )
		{
			return;
		}

		// New deduction
		String warnString = WumpusManager.WARN_STRINGS[ newStatus ];
		WumpusManager.addDeduction( "Listen: " + warnString + " in " + room );
	}
	
	private static void knownHazard( final Room room, int warn, final int type )
	{
		// If we have visited this room before, hazards are known
		if ( room.visited )
		{
			return;
		}

		// If we are visiting the room for the first time,
		// remember that the room has been visited.
		if ( type == VISIT )
		{
			room.visited = true;
		}

		int oldStatus = room.setHazards( warn );
		int newStatus = room.getHazards();
		if ( oldStatus == newStatus )
		{
			return;
		}

		// New deduction
		String idString = WumpusManager.DEDUCTION_STRINGS[ type ];
		String warnString = WumpusManager.WARN_STRINGS[ newStatus ];

		WumpusManager.addDeduction( idString + ": " + warnString + " in " + room );
	}

	private static void eliminateHazard( final int hazard )
	{
		Iterator it = WumpusManager.rooms.values().iterator();
		while ( it.hasNext() )
		{
			Room room = (Room) it.next();
			WumpusManager.eliminateHazard( room, hazard );
		}
	}
	
	private static void eliminateHazard( final Room room, int hazard )
	{
		// If we've already positively identified this room,
		// leave it alone
		int warn = room.getHazards();
		if ( ( warn & WARN_INDEFINITE ) == 0 )
		{
			return;
		}

		if ( ( hazard & WARN_PIT ) != 0 )
		{
			room.pit = 9;
		}

		if ( ( hazard & WARN_BATS ) != 0 )
		{
			room.bat = 9;
		}

		if ( ( hazard & WARN_WUMPUS ) != 0 )
		{
			room.wumpus = 9;
		}

		int oldStatus = room.setHazards( warn & ~hazard );
		int newStatus = room.getHazards();
		if ( oldStatus == newStatus )
		{
			return;
		}

		// New deduction
		String warnString = WumpusManager.ELIMINATE_STRINGS[ hazard ];

		WumpusManager.addDeduction( "Deduction: " + warnString + " in " + room );
	}

	private static void deduce( final Room room)
	{
		// If this room has a hazard, no exits
		if ( room.getHazards() != 0 )
		{
			return;
		}

		// Otherwise, save this room and check adjacent rooms again.
		WumpusManager.current = room;
		WumpusManager.deduce();
	}

	private static void deduce()
	{
		WumpusManager.deduce( WumpusManager.current, WARN_BATS );
		WumpusManager.deduce( WumpusManager.current, WARN_PIT );
		WumpusManager.deduce( WumpusManager.current, WARN_WUMPUS );
	}

	private static void deduce( final Room room, int mask )
	{
		Room exit = null;

		for ( int i = 0; i < 3; ++i )
		{
			Room link = room.getExit( i );
			if ( link == null )
			{
				// Internal error
				continue;
			}
			if ( ( link.getHazards() & mask ) != 0 )
			{
				if ( exit != null )
				{
					return;	// warning not unique
				}
				exit = link;
			}
		}

		if ( exit == null )
		{
			return;
		}

		switch ( mask )
		{
		case WARN_BATS:
			WumpusManager.knownBats( exit, ELIMINATION );
			break;
		case WARN_PIT:
			WumpusManager.knownPit( exit, ELIMINATION );
			break;
		case WARN_WUMPUS:
			WumpusManager.knownWumpus( exit, ELIMINATION );
			break;
		}
	}
	
	public static void takeChoice( int decision, String text )
	{
		if ( WumpusManager.current == null )
		{
			return;
		}

		// There can be 6 decisions - stroll into 3 rooms or charge
		// into 3 rooms.
		if ( decision > 3 )
		{
			decision -= 3;
		}

		Room room = WumpusManager.current.getExit( decision - 1 );

		if ( room == null )
		{
			// Internal error
			KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "Internal error: unknown exit #" + decision + " from " + WumpusManager.current );
			return;
		}

		// Unfortunately, the wumpus was nowhere to be seen.
		if ( text.indexOf( "wumpus was nowhere to be seen" ) != -1  )
		{
			WumpusManager.knownSafe( room, VISIT );
			return;
		}

		// You stroll nonchalantly into the cavern chamber and find,
		// unexpectedly, a wumpus.
		//  or
		// Now that you have successfully snuck up and surprised the
		// wumpus, it doesn't seem to really know how to react.

		if ( text.indexOf( "unexpectedly, a wumpus" ) != -1 ||
		     text.indexOf( "surprised the wumpus" ) != -1 )
		{
			WumpusManager.knownWumpus( room, VISIT );
			return;
		}
	}

	public static String[] dynamicChoiceOptions( String text )
	{
		if ( WumpusManager.current == null )
		{
			return new String[ 0 ];
		}

		String[] results = new String[ 6 ];
		for ( int i = 0; i < 3; ++i )
		{
			Room room = WumpusManager.current.getExit( i );
			if ( room == null )
			{
				// Internal error
				continue;
			}
			String warning = WumpusManager.WARN_STRINGS[ room.getHazards() ];
			results[ i ] = warning;
			results[ i + 3 ] = warning;
		}

		return results;
	}

	private static void printDeduction( final String text )
	{
		RequestLogger.printLine( text );
		RequestLogger.updateSessionLog( text );
	}

	private static void addDeduction( final String text )
	{
		// Print the string to the CLI and session log
		WumpusManager.printDeduction( text );

		if ( WumpusManager.deductions.length() == 0 )
		{
			WumpusManager.deductions.append( "<center>" );
		}
		else
		{
			WumpusManager.deductions.append( "<br>" );
		}

		WumpusManager.deductions.append( text ); 
	}

	public static final void decorate( final StringBuffer buffer )
	{
		// <img border=0 src=wump_graphic3.php?litstring=xxx&map=xxx&current=xxx>
		int index = buffer.indexOf( "</table></center></td></tr>" );
		if ( index != -1 )
		{
			// String link = WumpusManager.getWumpinatorMap();
			String link = WumpusManager.getWumpinatorLink();
			buffer.insert( index, "<tr><td><center>" + link + "</center></td></tr>" );
		}

		if ( WumpusManager.deductions.length() == 0 )
		{
			return;
		}

		index = buffer.indexOf( "<center><form name=choiceform1" );
		if ( index == -1 )
		{
			return;
		}

		WumpusManager.deductions.append( "</center><br>" );
		buffer.insert( index, WumpusManager.deductions.toString() );
		WumpusManager.deductions.setLength( 0 );
	}

	private static final String getWumpinatorLink()
	{
		String map = WumpusManager.getWumpinatorCode();
		return "<a href=http://www.feesher.com/wumpus/wump_map.php?mapstring=" + map + " target=_blank>View in Wumpinator</a>";
	}

	private static final String getWumpinatorMap()
	{
		String litstring = "litstring=00000000000000000000";
		String map = "&map=" + WumpusManager.getWumpinatorCode();
		String current = WumpusManager.current == null ? "" : ( "&current=" + WumpusManager.current.getCode() );
		return "<tr><td><center><img border=0 src=http://www.feesher.com/wumpus/wump_graphic3.php?" + litstring + map + current + "></center></td></tr>";
	}

	public static final void printStatus()
	{
		// Since we use a TreeMap, rooms are in alphabetical order
		Iterator it = WumpusManager.rooms.values().iterator();
		while ( it.hasNext() )
		{
			Room room = (Room) it.next();
			String name = room.toString();
			String exits = room.shortExitString();
			String pit = room.pit == 9 ? "no pit" : room.pit == 8 ? "PIT" : String.valueOf( room.pit );
			String bats = room.bat == 9 ? "no bats" : room.bat == 8 ? "BATS" : String.valueOf( room.bat );
			String wumpus = room.wumpus == 9 ? "no wumpus" : room.wumpus == 8 ? "WUMPUS" : String.valueOf( room.wumpus );

			RequestLogger.printLine( name + ": exits = " + exits + ": " + pit + ", " + bats + ", " + wumpus );

		}
	}

	// Here's how it's set up:

	// * It starts with 20 blocks of 6 characters each, each block
	//   corresponding to a particular room. Room "A" is the first block,
	//   room "B" is the second block, etc.
	// * After the basic block, there's a "::P" delimiter, followed by a set
	//   of characters used to indicate the various "pit groups" that have
	//   been found. Then a "::B" delimiter, followed by a set of characters
	//   used to indicate the various "bat groups". 
	// * Within a room block, the 6 characters are as follows:
	// - First path destination (0 if unknown, room letter otherwise)
	// - Second path destination (ditto)
	// - Third path destination (ditto)
	// - Pit flag for this room
	// - Bat flag for this room
	// - Wumpus flag for this room
	//
	// The flags are set as: 0=unknown, 8=hazard confirmed, 9=confirmed
	// clear, other # = number of potential clues pointing at the hazard so
	// far.
	//
	// Bat groups and pit groups are always separated by colons. Each group
	// shows the three rooms that were flagged as destinations from a room
	// with a roaring or squeaking sound.
	//
	// So, for example, if you know that room A maps to B, C, and W, and you
	// can hear a roaring sound from room A, the mapstring looks like this:
	//
	// BCW999A00199A0019900 00000000000000000000 00000000000000000000
	// 00000000000000000000 00000000000000000000 00000000000000A00199
	// ::P:BCW::B
	// 
	// Room A has been visited, so all its hazard flags are 9 (not
	// found). Rooms B, C, and W have "1" in the pit hazard flag (meaning
	// that one adjacent room flags it as a possible pit) and "9" in the bat
	// and wumpus flags (no chance of either hazard in these rooms). The
	// only pit group is BCW, and there is no data entered for bat groups.
	// 
	// If we then add data for room D, mapping to C, E, and F, and with both
	// bats AND pits detected, the mapstring looks like this:
	// 
	// BCW999A00199AD0299CE F999D00119D001190000 00000000000000000000
	// 00000000000000000000 00000000000000000000 00000000000000A00199
	// ::P:BCW:CEF::B:CEF

	public static final String getWumpinatorCode()
	{
		StringBuffer buffer = new StringBuffer();

		// Since we use a TreeMap, rooms are in alphabetical order
		Iterator it = WumpusManager.rooms.values().iterator();
		while ( it.hasNext() )
		{
			Room room = (Room) it.next();
			// Append code letters for each exit
			for ( int i = 0; i < 3; ++i )
			{
				Room exit = room.getExit( i );
				buffer.append( exit == null ? '0' : exit.getCode() );
			}
			// Append Wumpinator hazard flags
			buffer.append( String.valueOf( room.pit ) );
			buffer.append( String.valueOf( room.bat ) );
			buffer.append( String.valueOf( room.wumpus ) );
		}

		// Append pit groups
		buffer.append( "::P" );
		it = WumpusManager.rooms.values().iterator();
		while ( it.hasNext() )
		{
			Room room = (Room) it.next();
			if ( ( room.getListen() & WARN_PIT ) == 0 )
			{
				continue;
			}
			buffer.append( ":" );
			for ( int i = 0; i < 3; ++i )
			{
				Room exit = room.getExit( i );
				buffer.append( exit.getCode() );
			}
		}

		// Append bat groups
		buffer.append( "::B" );
		it = WumpusManager.rooms.values().iterator();
		while ( it.hasNext() )
		{
			Room room = (Room) it.next();
			if ( ( room.getListen() & WARN_BATS ) == 0 )
			{
				continue;
			}
			buffer.append( ":" );
			for ( int i = 0; i < 3; ++i )
			{
				Room exit = room.getExit( i );
				buffer.append( exit.getCode() );
			}
		}

		return buffer.toString();
	}

	public static final void invokeWumpinator()
	{
		String code = WumpusManager.getWumpinatorCode();
		StaticEntity.openSystemBrowser( "http://www.feesher.com/wumpus/wump_map.php?mapstring=" + code );
	}

	private static class Room
	{
		public final String name;
		public final char code;

		public boolean visited;
		public Room[] exits = new Room[3];

		// Our flags
		public int listen;
		public int hazards;

		// Wumpinator flags
		public int pit;
		public int bat;
		public int wumpus;

		public Room( final String name )
		{
			this.name = name;
			this.code = Character.toUpperCase( name.charAt(0) );
			this.reset();
		}

		public void reset()
		{
			this.visited = false;
			this.exits[ 0 ] = null;
			this.exits[ 1 ] = null;
			this.exits[ 2 ] = null;
			this.listen = WARN_INDEFINITE;
			this.hazards = WARN_ALL;
			this.pit = 0;
			this.bat = 0;
			this.wumpus = 0;
		}

		public String getName()
		{
			return this.name;
		}

		public char getCode()
		{
			return this.code;
		}

		public int getListen()
		{
			return this.listen;
		}

		public void setListen( final int listen )
		{
			this.listen = listen;
		}

		public Room getExit( final int index )
		{
			return this.exits[ index ];
		}

		public void setExit( final int index, final Room room )
		{
			this.exits[ index ] = room;
		}

		public void addExit( final Room room )
		{
			for ( int index = 0; index < 3; ++index )
			{
				Room exit = this.exits[ index ];
				if ( exit == room )
				{
					return;
				}
				if ( exit == null )
				{
					this.exits[ index ] = room;
					return;
				}
			}
		}

		public String  exitString()
		{
			String exit1 = this.exits[0] == null ? "unknown" : this.exits[0].toString();
			String exit2 = this.exits[1] == null ? "unknown" : this.exits[1].toString();
			String exit3 = this.exits[2] == null ? "unknown" : this.exits[2].toString();
			return exit1 + ", " + exit2 + ", " + exit3;
		}

		public String  shortExitString()
		{
			String exit1 = this.exits[0] == null ? "unknown" : this.exits[0].getName();
			String exit2 = this.exits[1] == null ? "unknown" : this.exits[1].getName();
			String exit3 = this.exits[2] == null ? "unknown" : this.exits[2].getName();
			return exit1 + ", " + exit2 + ", " + exit3;
		}

		public int getHazards()
		{
			return this.hazards;
		}

		public int setHazards( final int hazards )
		{
			// Only set the hazards for rooms we've not
			// actually visited.
			int old = this.hazards;
			if ( ( old & WARN_INDEFINITE ) != 0 )
			{
				this.hazards = old & hazards;
			}
			return old;
		}

		public String toString()
		{
			return "the " + this.name + " chamber";
		}
	}
}
