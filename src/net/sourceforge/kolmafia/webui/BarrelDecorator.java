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

package net.sourceforge.kolmafia.webui;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.RequestThread;

import net.sourceforge.kolmafia.persistence.ItemDatabase;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.request.AdventureRequest;
import net.sourceforge.kolmafia.request.BarrelRequest;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public abstract class BarrelDecorator
{
	private static final Pattern UNSMASHED = Pattern.compile( "smash=(\\d+)&pwd=(\\w+)'><img src='http://images.kingdomofloathing.com/otherimages/mountains/smallbarrel.gif'.*?>" );
	private static long unsmashedSquares = 0;
	private static int unsmashedUser = -1;
		
	private static final int B = 1;			// flag for booze items found in barrels
	private static final int RSHIFT = 8;
	private static final int R = 1 << RSHIFT;	// flag for restorative items
	private static final int SSHIFT = 16;
	private static final int S = 1 << SSHIFT;	// flag for spleen/stat-gain items
	private static final int MASK = 0xFF;
	
	// AFH spading (see // http://www.alliancefromhell.com/forum/viewtopic.php?t=949)
	// revealed that there are only 9 possible layouts for the barrels,
	// each of which consists of 3 consecutive rows from a wrap-around list
	// of 9 row layouts.  I've repeated the first two rows at the end here,
	// rather than doing modulo math on every access to this array.

	private static final int[] BARRELROLL =
	{
		B,S,R, B,R,S, R,S,B, S,B,R, B,R,S, R,B,S, S,R,B, B,R,S, R,S,B,
		B,S,R, B,R,S
	};

	public static final int barrelToQuad( int barrel )	// 1..36 => 0..8
	{
		int x = ( ( barrel - 1 ) % 6 ) / 2;
		int y = ( barrel - 1 ) / 12;
		return y * 3 + x;
	}

	public static final int quadToBarrel( int quad )	// 0..8 => 1..36
	{	// Corresponding barrels are at N, N+1, N+6, and N+7
		int x = quad % 3;
		int y = quad / 3;
		return y * 12 + x * 2 + 1;
	}

	private static int[] compute()
	{
		String layout = Preferences.getString("barrelLayout");

		if ( layout.length() < 9 )
		{
			layout = "?????????";
		}

		int [] knowns = new int[9];
		for ( int i = 0; i < 9; ++i )
		{
			int val = -1;
			switch ( layout.charAt(i) )
			{
			case 'B':
			case 'b':
				val = B;
				break;
			case 'R':
			case 'r':
				val = R;
				break;
			case 'S':
			case 's':
				val = S;
			}
			knowns[i] = val;
		}

		int [] possibles = { 0,0,0, 0,0,0, 0,0,0 };
	tryOnePattern:
		for ( int pattern = 0; pattern <= BARRELROLL.length - 9; pattern += 3 )
		{
			for ( int i = 0; i < 9; ++i )
			{
				if ( (BARRELROLL[pattern + i] & knowns[i]) == 0 ) continue tryOnePattern;
			}
			// This pattern matches all the known results
			for ( int i = 0; i < 9; ++i )
			{
				possibles[i] += BARRELROLL[pattern + i];
			}
		}
		
		return possibles;
	}

	public static final void decorate( final StringBuffer buffer )
	{
		int [] possibles = compute();

		Matcher m = UNSMASHED.matcher( buffer.toString() );
		buffer.setLength( 0 );
		
		while ( m.find() )
		{
			int square = StringUtilities.parseInt( m.group(1) );
			int quad = barrelToQuad( square );
			if ( quad < 0 || quad >= 9 )
			{
				continue;	// shouldn't happen
			}
			
			int possible = possibles[quad];
			if ( possible == 0 )
			{
				continue;	// no patterns matched - shouldn't happen
			}

			int b = possible & MASK;	// split into individual types
			int r = (possible >> RSHIFT) & MASK;
			int s = (possible >> SSHIFT) & MASK;
			int total = b + r + s;
			String tooltip = "An Unsmashed Barrel (1) " + (b * 100 / total) + "% booze, " +
				(r * 100 / total) + "% restores, " + (s * 100 / total) + "% spleen";
				
			// Now, generate a filename for the replacement image,
			// from the letters b, r, & s.	Letters grouped
			// together have the same probability of appearing.
			// Groups separated by dashes have descending
			// probability as you go right.	 It turns out that only
			// 19 such patterns can ever be generated:
			//
			//	b, b-r, b-r-s, b-rs, b-s, br, brs, bs, r, r-b,
			//	r-bs, r-s, r-s-b, rs, s, s-b, s-b-r, s-br, and
			//	s-r.

			StringBuffer filename = new StringBuffer();
			while ( true )
			{
				int max = b;
				if ( r > max )
				{
					max = r;
				}
				if ( s > max )
				{
					max = s;
				}
				if ( b == max )
				{
					filename.append( "b" );
					b = 0;
				}
				if ( r == max )
				{
					filename.append( "r" );
					r = 0;
				}
				if ( s == max )
				{
					filename.append( "s" );
					s = 0;
				}
				if ( b + r + s == 0 )
				{
					break;
				}
				filename.append( "-" );
			}
			
			if ( Preferences.getBoolean( "relayShowSpoilers" ) )
			{
				m.appendReplacement(
					buffer,
					"smash=$1&pwd=$2'>" + "<img src='/images/otherimages/barrels/" + filename + ".gif' " + "border=0 alt=\"" + tooltip + "\" title=\"" + tooltip + "\">" );
			}
		}
		if ( Preferences.getBoolean( "relayShowSpoilers" ) )
			m.appendTail( buffer );
	}

	private static final Pattern SMASH_PATTERN = Pattern.compile( "smash=(\\d+)" );

	public static final void beginSmash( String url )
	{
		Matcher m = SMASH_PATTERN.matcher( url );
		int square = 0;
		if ( m.find() )
		{
			square = StringUtilities.parseInt( m.group( 1 ) );
			AdventureRequest.setNameOverride( "Mimic", square <= 12 ? "Mimic (Top 2 Rows)" :
				square <= 24 ? "Mimic (Middle 2 Rows)" : "Mimic (Bottom 2 Rows)" );
		}
		Preferences.setInteger( "lastBarrelSmashed", square );
	}
	
	public static final void gainItem( AdventureResult item )
	{
		int square = Preferences.getInteger( "lastBarrelSmashed" );
		if ( (square < 1) || (square > 36) )
		{
			return;
		}

		// Potential problem here: certain familiars and skills can
		// cause unrelated items to be acquired during combat with a
		// barrel mimic.  The worst possibility appears to be Summon
		// Hobo Underling -> Ask the Hobo For a Drink.	I'm hoping that
		// in all such cases, the unrelated item will be processed
		// before the actual barrel item, and therefore the layout will
		// end up in the correct state.	 It can still get confused if
		// the player is defeated by the mimic (or runs away) after an
		// unrelated item drop; but it's hard to imagine that happening
		// to anyone who has the Summon Hobo Underling skill available.
		
		// Also a possible problem: Oyster Egg Day drops!

		char type;
		String name = item.getName();
		if ( ItemDatabase.getInebriety( name ) > 0 )
		{
			type = 'B';
		}
		else if ( ItemDatabase.getSpleenHit( name ) == 1 &&
			!name.endsWith( "egg" ) )
		{
			type = 'S';
		}
		// Restorative items aren't nearly so easy to recognize!
		else if ( name.startsWith( "Doc Galaktik" ) || name.endsWith( "seltzer" ) ||
			name.endsWith( "Cola" ) || name.endsWith( "water" ) )
		{
			type = 'R';
		}
		else
		{
			return;	// clover, or unrelated item - doesn't identify the barrel
		}

		StringBuffer layout = new StringBuffer( Preferences.getString( "barrelLayout" ) );
		while ( layout.length() < 9 )
		{
			layout.append( '?' );
		}
		layout.setCharAt( barrelToQuad( square ), type );
		Preferences.setString( "barrelLayout", layout.toString() );
	}
	
	public static final int recommendSquare()
	{
		if ( unsmashedSquares == 0L || unsmashedUser != KoLCharacter.getUserId() )
		{	// need to visit the page to determine which barrels are available
			RequestThread.postRequest( new BarrelRequest() );
		}
		
		int rows = Preferences.getInteger( "barrelGoal" );	
		int square = 0;
		int[] possibles = compute();
		if ( (rows & 1) != 0 )
		{
			square = recommendRow( possibles, 0 ) ;
		}
		if ( (rows & 2) != 0 && square == 0 )
		{
			square = recommendRow( possibles, 3 ) ;
		}
		if ( (rows & 4) != 0 && square == 0 )
		{
			square = recommendRow( possibles, 6 ) ;
		}

		return square;
	}
	
	private static final int recommendRow( final int[] possibles, final int startQuad )
	{
		int maxprob = -1;
		int quad = -1;
		for ( int i = startQuad; i < startQuad + 3; ++i )
		{
			int prob = possibles[ i ] & MASK;
			if ( prob > maxprob )
			{
				maxprob = prob;
				quad = i;
			}
		}
		
		int square = quadToBarrel( quad );
		if ( (unsmashedSquares & (1L << (square))) != 0L )
		{
			return square;
		}
		if ( (unsmashedSquares & (1L << (square + 1))) != 0L )
		{
			return square + 1;
		}
		if ( (unsmashedSquares & (1L << (square + 6))) != 0L )
		{
			return square + 6;
		}
		if ( (unsmashedSquares & (1L << (square + 7))) != 0L )
		{
			return square + 7;
		}
		return 0;		
	}

	public static void parseResponse( String urlString, String responseText )
	{
		Matcher m = UNSMASHED.matcher( responseText );
		unsmashedSquares = 1L;	// make value non-zero, even if no barrels remain unsmashed
		unsmashedUser = KoLCharacter.getUserId();

		while ( m.find() )
		{
			int square = StringUtilities.parseInt( m.group(1) );
			int quad = barrelToQuad( square );
			if ( quad < 0 || quad >= 9 )
			{
				continue;	// shouldn't happen
			}
			unsmashedSquares |= 1L << square;
		}
	}
}
