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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.kolmafia.utilities.StringUtilities;

public abstract class WumpusManager
{
	public static HashMap warnings = new HashMap();
	public static final int WARN_SAFE = 0;
	public static final int WARN_BATS = 1;
	public static final int WARN_PIT = 2;
	public static final int WARN_WUMPUS = 4;
	public static final int WARN_INDEFINITE = 8;
	public static final int WARN_ALL = 15;
	public static String[] WARN_STRINGS = new String[] {
		"safe",
		"definite bats",
		"definite pit",
		"ERROR: BATS AND PIT???",
		"definite Wumpus",
		"ERROR: BATS AND WUMPUS???",
		"ERROR: PIT AND WUMPUS???",
		"TOTAL ALGORITHM FAILURE!!!",

		"safe",
		"possible bats",
		"possible pit",
		"possible bats or pit",
		"possible Wumpus",
		"possible bats or Wumpus",
		"possible pit or Wumpus",
		"idunno, could be anything",
	};

	private static final Pattern ROOM_PATTERN = Pattern.compile( ">The (\\w+) Chamber<" );
	private static final Pattern LINK_PATTERN = Pattern.compile( "Enter the (\\w+) chamber" );

	private static int get( String room )
	{
		Integer i = (Integer) WumpusManager.warnings.get( room );
		return i == null ? WARN_ALL : i.intValue();
	}
	
	private static void put( String room, int value )
	{
		WumpusManager.warnings.put( room, new Integer( WumpusManager.get( room ) & value ) );
	}
	
	public static String[] dynamicChoiceOptions( String text )
	{
		if ( text == null )
		{
			return new String[] { "you're clicking too fast - lost a page update" };
		}
		Matcher m = WumpusManager.ROOM_PATTERN.matcher( text );
		if ( !m.find() )
		{	// Must be at the entryway, or perhaps died
			WumpusManager.warnings.clear();
			return new String[ 0 ];
		}
		
		String current = m.group( 1 ).toLowerCase();
		if ( text.indexOf( "Wait for the bats to drop you" ) != -1 )
		{
			WumpusManager.put( current, WARN_BATS );
			return new String[ 0 ];
		}

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
		
		String[] results = new String[ 3 ];
		m = WumpusManager.LINK_PATTERN.matcher( text );
		// Basic logic: assume all rooms have all warnings initially.
		// Remove any warnings from linked rooms that aren't present
		// in the current room.
		for ( int i = 0; i < 3; ++i )
		{
			if ( !m.find() )
			{
				return new String[ 0 ];
			}
			String link = m.group( 1 ).toLowerCase();
			WumpusManager.put( link, warn );
			results[ i ] = link;
		}
		
		// Advanced logic: if only one of the linked rooms has a given
		// warning, promote that to a definite danger, and remove any
		// other warnings from that room.
		WumpusManager.deduce( results );
		// Doing this may make further deductions possible.
		WumpusManager.deduce( results );
		// I'm not sure if a 3rd deduction is actually possible, but it
		// doesn't hurt to try.
		WumpusManager.deduce( results );

		WumpusManager.put( current, WARN_SAFE );
		for ( int i = 0; i < 3; ++i )
		{
			results[ i ] = WumpusManager.WARN_STRINGS[ WumpusManager.get( results[ i ] ) ];
		}
		return results;
	}
	
	private static void deduce( String[] links )
	{
		WumpusManager.deduce( links, WARN_BATS );
		WumpusManager.deduce( links, WARN_PIT );
		WumpusManager.deduce( links, WARN_WUMPUS );
	}

	private static void deduce( String[] links, int mask )
	{
		int which = -1;
		for ( int i = 0; i < 3; ++i )
		{
			if ( (WumpusManager.get( links[ i ] ) & mask) == 0 ) continue;
			if ( which != -1 ) return;	// warning not unique
			which = i;
		}
		if ( which == -1 ) return;	// no unique warning
		WumpusManager.put( links[ which ], mask );
	}
}
