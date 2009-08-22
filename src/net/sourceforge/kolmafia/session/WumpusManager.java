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

import java.util.HashMap;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestLogger;
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

	public static HashMap rooms = new HashMap();

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
			// Shouldn't happen: if there is a choice option to
			// take, there must be a room name.
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

		// Initialize the exits from the current room
		WumpusManager.current = room;
		WumpusManager.current.resetLinks();
		m = WumpusManager.LINK_PATTERN.matcher( text );
		for ( int i = 0; i < 3; ++i )
		{
			if ( !m.find() )
			{
				// Should not happen; there are always three
				// exits from a room.
				KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "Internal error: could not find exit #" + i + " from " + WumpusManager.current );
				return;
			}
			WumpusManager.current.setLink( i, m.group( 1 ) );
		}

		WumpusManager.printDeduction( "Exits: " +
					      WumpusManager.current.getLink(0) +
					      ", " +
					      WumpusManager.current.getLink(1) +
					      ", " +
					      WumpusManager.current.getLink(2) );

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
			Room link = WumpusManager.current.getLink( i );
			WumpusManager.possibleHazard( link, warn, LISTEN );
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
		WumpusManager.knownHazard( room, WARN_SAFE, type );
	}

	private static void knownBats( final int type  )
	{
		WumpusManager.knownBats( WumpusManager.current, type );
	}

	private static void knownBats( final Room room, final int type	)
	{
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
		WumpusManager.eliminateHazard( WARN_BATS, WumpusManager.bats1, WumpusManager.bats2 );
	}

	private static void knownPit( final int type  )
	{
		WumpusManager.knownPit( WumpusManager.current, type );
	}

	private static void knownPit( final Room room, final int type )
	{
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
		WumpusManager.eliminateHazard( WARN_PIT, WumpusManager.pit1, WumpusManager.pit2 );
	}

	private static void knownWumpus( final int type	 )
	{
		WumpusManager.knownWumpus( WumpusManager.current, type );
	}

	private static void knownWumpus( final Room room, final int type )
	{
		WumpusManager.knownHazard( room, WARN_WUMPUS, type );

		// There is exactly one wumpus rooms per cave
		if ( WumpusManager.wumpus != null )
		{
			return;
		}

		// We've just identified the wumpus room
		WumpusManager.wumpus = room;

		// Eliminate wumpus from rooms that have only "possible" wumpus
		WumpusManager.eliminateHazard( WARN_WUMPUS, WumpusManager.wumpus, null );
	}
	
	private static void knownHazard( final Room room, int warn, final int type )
	{
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
	
	private static void eliminateHazard( final int hazard, final Room room1, final Room room2 )
	{
		Iterator it = WumpusManager.rooms.values().iterator();
		while ( it.hasNext() )
		{
			Room room = (Room) it.next();

			if ( room == room1 || room == room2 )
			{
				continue;
			}

			WumpusManager.possibleHazard( room, room.getHazards() & ~hazard, DEDUCTION );
		}
	}
	
	private static void possibleHazard( final Room room, int warn, final int type )
	{
		// If we have already positively identified this as a room with
		// a hazard, nothing more to do here.

		if ( room == WumpusManager.bats1 ||
		     room == WumpusManager.bats2 ||
		     room == WumpusManager.pit1 ||
		     room == WumpusManager.pit2 ||
		     room == WumpusManager.wumpus )
		{
			return;
		}

		// If it's a definite hazard, pass it on through
		if ( ( warn & WARN_INDEFINITE ) == 0)
		{
			WumpusManager.knownHazard( room, warn, type );
			return;
		}

		// Otherwise, it's a possible warning.

		if ( ( warn & WARN_BATS ) != 0 &&
		     WumpusManager.bats1 != null &&
		     WumpusManager.bats2 != null )
		{
			warn &= ~WARN_BATS;
		}

		if ( ( warn & WARN_PIT ) != 0 &&
		     WumpusManager.pit1 != null &&
		     WumpusManager.pit2 != null )
		{
			warn &= ~WARN_PIT;
		}

		if ( ( warn & WARN_WUMPUS ) != 0 &&
		     WumpusManager.wumpus != null )
		{
			warn &= ~WARN_WUMPUS;
		}

		// Register possible hazard
		WumpusManager.knownHazard( room, warn, type );
	}

	private static void deduce()
	{
		WumpusManager.deduce( WARN_BATS );
		WumpusManager.deduce( WARN_PIT );
		WumpusManager.deduce( WARN_WUMPUS );
	}

	private static void deduce( int mask )
	{
		Room room = null;

		for ( int i = 0; i < 3; ++i )
		{
			Room link = WumpusManager.current.getLink( i );
			if ( link == null )
			{
				// Internal error
				continue;
			}
			if ( ( link.getHazards() & mask ) != 0 )
			{
				if ( room != null )
				{
					return;	// warning not unique
				}
				room = link;
			}
		}

		if ( room == null )
		{
			return;
		}

		switch ( mask )
		{
		case WARN_BATS:
			WumpusManager.knownBats( room, ELIMINATION );
			break;
		case WARN_PIT:
			WumpusManager.knownPit( room, ELIMINATION );
			break;
		case WARN_WUMPUS:
			WumpusManager.knownWumpus( room, ELIMINATION );
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

		Room room = WumpusManager.current.getLink( decision - 1 );

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
			Room room = WumpusManager.current.getLink( i );
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

	private static class Room
	{
		public final String name;
		public Room[] exits = new Room[3];
		public int listen;
		public int hazards;

		public Room( final String name )
		{
			this.name = name;
			this.reset();
		}

		public void reset()
		{
			this.resetLinks();
			this.listen = WARN_INDEFINITE;
			this.hazards = WARN_ALL;
		}

		public void resetLinks()
		{
			this.exits[ 0 ] = null;
			this.exits[ 1 ] = null;
			this.exits[ 2 ] = null;
		}

		public String getName()
		{
			return this.name;
		}

		public int getListen()
		{
			return this.listen;
		}

		public void setListen( final int listen )
		{
			this.listen = listen;
		}

		public Room getLink( final int index )
		{
			return this.exits[ index ];
		}

		public void setLink( final int index, final String name )
		{
			this.exits[ index ] = (Room) WumpusManager.rooms.get( name );
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
