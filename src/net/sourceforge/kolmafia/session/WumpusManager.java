/**
 * Copyright (c) 2005-2012, KoLmafia development team
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

import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestLogger;

import net.sourceforge.kolmafia.webui.RelayLoader;

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
	public static Room last;

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
		WumpusManager.last = null;

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
		WumpusManager.last = WumpusManager.current;
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
			KoLmafia.updateDisplay( MafiaState.ERROR, "Unknown room in Wumpus cave: the " + name + " chamber");
			return;
		}

		// If we have already visited this room, nothing more to do
		if ( room.visited )
		{
			WumpusManager.current = room;
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
				KoLmafia.updateDisplay( MafiaState.ERROR, "Internal error: " + i + " exits found in " + WumpusManager.current );
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

		WumpusManager.deduce( room );

		// Doing this may make further deductions possible.

		WumpusManager.deduce( room );

		// I'm not sure if a 3rd deduction is actually possible, but it
		// doesn't hurt to try.

		WumpusManager.deduce( room );
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
		// If we already know there are bats here, punt
		if ( room.bat == 8 )
		{
			return;
		}

		// Set Wumpinator flags for this room
		room.bat = 8;
		room.pit = 9;
		room.wumpus = 9;

		// We know an additional bat room
		WumpusManager.knownHazard( room, WARN_BATS, type );

		// There are exactly two bat rooms per cave
		if ( WumpusManager.bats1 == null )
		{
			// We've just identified the first bat room
			WumpusManager.bats1 = room;
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
		// If we already know there is a pit here, punt
		if ( room.pit == 8 )
		{
			return;
		}

		// Set Wumpinator flags for this room
		room.bat = 9;
		room.pit = 8;
		room.wumpus = 9;

		// We know an additional pit
		WumpusManager.knownHazard( room, WARN_PIT, type );

		// There are exactly two pit rooms per cave
		if ( WumpusManager.pit1 == null )
		{
			// We've just identified the first pit room
			WumpusManager.pit1 = room;
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
		// If we already know the Wumpus is here, punt
		if ( room.wumpus == 8 )
		{
			return;
		}

		// Set Wumpinator flags for this room
		room.bat = 9;
		room.pit = 9;
		room.wumpus = 8;

		// We know the wumpus room
		WumpusManager.knownHazard( room, WARN_WUMPUS, type );

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
			// If we know both bat rooms, no bats in this room.
			if ( WumpusManager.bats1 != null && WumpusManager.bats2 != null )
			{
				warn &= ~WARN_BATS;
			}
			else if ( ++room.bat == 3 )
			{
				WumpusManager.knownBats( room, DEDUCTION );
				return;
			}
		}

		if ( ( warn & WARN_PIT ) != 0 )
		{
			// If we know both pit rooms, no pit in this room.
			if ( WumpusManager.pit1 != null && WumpusManager.pit2 != null )
			{
				warn &= ~WARN_PIT;
			}
			else if ( ++room.pit == 3 )
			{
				WumpusManager.knownPit( room, DEDUCTION );
				return;
			}
		}

		if ( ( warn & WARN_WUMPUS ) != 0 )
		{
			// If we know the Wumpus room, no Wumpus in this room.
			if ( WumpusManager.wumpus != null )
			{
				warn &= ~WARN_WUMPUS;
			}
			else if ( ++room.wumpus == 2 )
			{
				WumpusManager.knownWumpus( room, DEDUCTION );
				return;
			}
		}

		if ( warn == WARN_INDEFINITE )
		{
			// We know there are no hazards in this room
			room.pit = 9;
			room.bat = 9;
			room.wumpus = 9;
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
		// Remember that the room has been visited.
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

		// Look at neighbors and see if what we learned in
		// this one tells us more about other rooms.

		WumpusManager.deduceNeighbors( room );
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

		// Look at neighbors and see if what we learned in
		// this one tells us more about other rooms.

		WumpusManager.deduceNeighbors( room );
	}

	private static void deduceNeighbors( final Room room )
	{
		// We've learned something new about this room.
		// Examine all adjacent rooms that we have visited and
		// heard something from and see if we can deduce
		// anything more.
		Room [] exits = room.getExits();
		for ( int i = 0; i < exits.length; ++i)
		{
			Room neighbor = exits[i];
			if ( neighbor != null &&
			     neighbor.visited &&
			     neighbor.getListen() != WARN_INDEFINITE )
			{
				WumpusManager.deduce( neighbor );
			}
		}
	}

	private static void deduce( final Room room )
	{
		if ( room != null )
		{
			WumpusManager.deduce( room, WARN_BATS );
			WumpusManager.deduce( room, WARN_PIT );
			WumpusManager.deduce( room, WARN_WUMPUS );
		}
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
			KoLmafia.updateDisplay( MafiaState.ERROR, "Internal error: unknown exit #" + decision + " from " + WumpusManager.current );
			return;
		}

		// Unfortunately, the wumpus was nowhere to be seen.
		if ( text.indexOf( "wumpus was nowhere to be seen" ) != -1  )
		{
			WumpusManager.last = WumpusManager.current;
			WumpusManager.eliminateHazard( room, WARN_WUMPUS );
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
			WumpusManager.last = WumpusManager.current;
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
		if ( WumpusManager.current != null )
		{
			int index = buffer.indexOf( "</table></center></td></tr>" );
			if ( index != -1 )
			{
				String link = WumpusManager.getWumpinatorMap();
				// String link = WumpusManager.getWumpinatorLink();
				buffer.insert( index, "<tr><td><center>" + link + "</center></td></tr>" );
			}
		}

		if ( WumpusManager.deductions.length() == 0 )
		{
			return;
		}

		int index = buffer.indexOf( "<center><form name=choiceform1" );
		if ( index == -1 )
		{
			return;
		}

		WumpusManager.deductions.append( "</center><br>" );
		buffer.insert( index, WumpusManager.deductions.toString() );
		WumpusManager.deductions.setLength( 0 );
	}

	public static final void invokeWumpinator()
	{
		String code = WumpusManager.getWumpinatorCode();
		String current = WumpusManager.getCurrentField();
		RelayLoader.openSystemBrowser( "http://www.feesher.com/wumpus/wump_map.php?mapstring=" + code + current );
	}

	private static final Room currentRoom()
	{
		if ( WumpusManager.current != null )
		{
			return WumpusManager.current;
		}
		if ( WumpusManager.last != null )
		{
			return WumpusManager.last;
		}
		return null;
	}

	private static final String getCurrentField()
	{
		return WumpusManager.getCurrentField( WumpusManager.currentRoom() );
	}

	private static final String getCurrentField( final Room room )
	{
		if ( room == null )
		{
			return "";
		}
		return "&current=" + room.getCode();
	}

	private static final String getWumpinatorLink()
	{
		String current = WumpusManager.getCurrentField();
		String map = WumpusManager.getWumpinatorCode();
		return "<a href=http://www.feesher.com/wumpus/wump_map.php?mapstring=" + map + current + " target=_blank>View in Wumpinator</a>";
	}

	private static final String getWumpinatorMap()
	{
		String layout = WumpusManager.getLayout();
		// If we can't generate a map, give a link to Wumpinator
		if ( layout == null )
		{
			return WumpusManager.getWumpinatorLink();
		}
		String litstring = "litstring=" + layout;
		String map = "&map=" + WumpusManager.getWumpinatorCode();
		String current = WumpusManager.getCurrentField();
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
				buffer.append( exit == null ? "0" : exit.getCode() );
			}
			// Append Wumpinator hazard flags
			buffer.append( String.valueOf( room.pit % 10 ) );
			buffer.append( String.valueOf( room.bat % 10 ) );
			buffer.append( String.valueOf( room.wumpus % 10 ) );
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

	private static final int[][][] NODE_PERMUTATIONS =
	{
		// 0 = { 1, 2, 5 }
		{ { 1, 2, 5 }, { 1, 5, 2 },
		  { 2, 1, 5 }, { 2, 5, 1 },
		  { 5, 1, 2 }, { 5, 2, 1 } },
		// 1 = { 0, 3, 6 }
		{ { 0, 3, 6 }, { 0, 6, 3 },
		  { 3, 0, 6 }, { 3, 6, 0 },
		  { 6, 0, 3 }, { 6, 3, 0 } },
		// 2 = { 0, 4, 7 }
		{ { 0, 4, 7 }, { 0, 7, 4 },
		  { 4, 0, 7 }, { 4, 7, 0 },
		  { 7, 0, 4 }, { 7, 4, 0 } },
		// 3 = { 1, 4, 8 }
		{ { 1, 4, 8 }, { 1, 8, 4 },
		  { 4, 1, 8 }, { 4, 8, 1 },
		  { 8, 1, 4 }, { 8, 4, 1 } },
		// 4 = { 2, 3, 9 }
		{ { 2, 3, 9 }, { 2, 9, 3 },
		  { 3, 2, 9 }, { 3, 9, 2 },
		  { 9, 2, 3 }, { 9, 3, 2 } },
		// 5 = { 0, 10, 11 }
		{ { 0, 10, 11 }, { 0, 11, 10 },
		  { 10, 0, 11 }, { 10, 11, 0 },
		  { 11, 0, 10 }, { 11, 10, 0 } },
		// 6 = { 1, 10, 12 }
		{ { 1, 10, 12 }, { 1, 12, 10 },
		  { 10, 1, 12 }, { 10, 12, 1 },
		  { 12, 1, 10 }, { 12, 10, 1 } },
		// 7 = { 2, 11, 13 }
		{ { 2, 11, 13 }, { 2, 13, 11 },
		  { 11, 2, 13 }, { 11, 13, 2 },
		  { 13, 2, 11 }, { 13, 11, 2 } },
		// 8 = { 3, 12, 14 }
		{ { 3, 12, 14 }, { 3, 14, 12 },
		  { 12, 3, 14 }, { 12, 14, 3 },
		  { 14, 3, 12 }, { 14, 12, 3 } },
		// 9 = { 4, 13, 14 }
		{ { 4, 13, 14 }, { 4, 14, 13 },
		  { 13, 4, 14 }, { 13, 14, 4 },
		  { 14, 4, 13 }, { 14, 13, 4 } },
		// 10 = { 5, 6, 15 }
		{ { 5, 6, 15 }, { 5, 15, 6 },
		  { 6, 5, 15 }, { 6, 15, 5 },
		  { 15, 5, 6 }, { 15, 6, 5 } },
		// 11 = { 5, 7, 16 }
		{ { 5, 7, 16 }, { 5, 16, 7 },
		  { 7, 5, 16 }, { 7, 16, 5 },
		  { 16, 5, 7 }, { 16, 7, 5 } },
		// 12 = { 6, 8, 17 }
		{ { 6, 8, 17 }, { 6, 17, 8 },
		  { 8, 6, 17 }, { 8, 17, 6 },
		  { 17, 6, 8 }, { 17, 8, 6 } },
		// 13 = { 7, 9, 18 }
		{ { 7, 9, 18 }, { 7, 18, 9 },
		  { 9, 7, 18 }, { 9, 18, 7 },
		  { 18, 7, 9 }, { 18, 9, 7 } },
		// 14 = { 8, 9, 19 }
		{ { 8, 9, 19 }, { 8, 19, 9 },
		  { 9, 8, 19 }, { 9, 19, 8 },
		  { 19, 8, 9 }, { 19, 9, 8 } },
		// 15 = { 10, 16, 17 }
		{ { 10, 16, 17 }, { 10, 17, 16 },
		  { 16, 10, 17 }, { 16, 17, 10 },
		  { 17, 10, 16 }, { 17, 16, 10 } },
		// 16 = { 11, 15, 18 }
		{ { 11, 15, 18 }, { 11, 18, 15 },
		  { 15, 11, 18 }, { 15, 18, 11 },
		  { 18, 11, 15 }, { 18, 15, 11 } },
		// 17 = { 12, 15, 19 }
		{ { 12, 15, 19 }, { 12, 19, 15 },
		  { 15, 12, 19 }, { 15, 19, 12 },
		  { 19, 12, 15 }, { 19, 15, 12 } },
		// 18 = { 13, 16, 19 }
		{ { 13, 16, 19 }, { 13, 19, 16 },
		  { 16, 13, 19 }, { 16, 19, 13 },
		  { 19, 13, 16 }, { 19, 16, 13 } },
		// 19 = { 14, 17, 18 }
		{ { 14, 17, 18 }, { 14, 18, 17 },
		  { 17, 14, 18 }, { 17, 18, 14 },
		  { 18, 14, 17 }, { 18, 17, 14 } },
	};

	private static Room [] layout = new Room[20];
	private final static String emptyLayout = "00000000000000000000";

	private static final String getLayout()
	{
		Room current = WumpusManager.currentRoom();
		String layout = WumpusManager.getLayout( current );
		if ( !layout.equals( emptyLayout ) )
		{
			return layout;
		}

		// We failed to generate a layout from the specified
		// room. Try again with any visited room.
		Iterator it = WumpusManager.rooms.values().iterator();
		while ( it.hasNext() )
		{
			Room room = (Room) it.next();
			if ( room == current || !room.visited )
			{
				continue;
			}

			layout = WumpusManager.getLayout( room );
			if ( !layout.equals( emptyLayout ) )
			{
				return layout;
			}
		}

		// Sigh.
		return null;
	}

	private static final String getLayout( final Room room )
	{
		// Initialize layout
		for ( int i = 0; i < layout.length; ++i )
		{
			layout[ i ] = null;
		}

		// Calculate layout with specified room in position 0
		WumpusManager.addRoom( 0, room );

		// Generate layout string
		StringBuffer buffer = new StringBuffer();
		for ( int i = 0; i < layout.length; ++i )
		{
			Room node = layout[ i ];
			buffer.append( node == null ? "0" : node.getCode() );
		}

		String string = buffer.toString();
		return string;
	}

	private static final boolean addRoom( final int node, final Room room )
	{
		// Attempt to add a room at a particular node
		if ( layout[ node ] != null )
		{
			// It's OK if this room is already there
			return layout[ node ] == room;
		}

		// If room is already present elsewhere, error
		for ( int i = 0; i < layout.length; ++i )
		{
			if ( layout[ i ] == room )
			{
				return false;
			}
		}

		// Put this room into the specified position
		layout[ node ] = room;

		// Attempt to place the exits in linked nodes
		Room [] exits = room.getExits();

		int [][] permutations = WumpusManager.NODE_PERMUTATIONS[ node ];
		for ( int i = 0; i < permutations.length; ++i )
		{
			// Save a copy of the layout so we can easily unwind
			Room [] copy = (Room [])WumpusManager.layout.clone();

			int [] links = permutations[i];
			boolean success = true;
			for ( int j = 0; j < 3; ++j )
			{
				if ( exits[ j ] == null )
				{
					continue;
				}
				if ( !addRoom( links[ j ], exits[ j ] ) )
				{
					success = false;
					break;
				}
			}

			// If we successfully recursively placed the
			// rooms, return now.
			if ( success )
			{
				return true;
			}

			// Otherwise, restore previous state and try
			// next permutation
			WumpusManager.layout = copy;
		}

		// We failed to place this room.
		layout[ node ] = null;

		return false;
	}

	private static class Room
	{
		public final String name;
		public final String code;

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
			this.code = Character.toString( Character.toUpperCase( name.charAt(0) ) );
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

		public String getCode()
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

		public Room[] getExits()
		{
			return this.exits;
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

		public boolean hasExit( final Room room )
		{
			for ( int index = 0; index < 3; ++index )
			{
				if ( this.exits[ index ] == room )
				{
					return true;
				}
			}

			return false;
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

		@Override
		public String toString()
		{
			return "the " + this.name + " chamber";
		}
	}
}
