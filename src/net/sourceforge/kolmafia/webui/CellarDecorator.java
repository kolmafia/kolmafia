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

package net.sourceforge.kolmafia.webui;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLConstants;

import net.sourceforge.kolmafia.objectpool.ItemPool;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.session.GoalManager;

public abstract class CellarDecorator
{
	// Data format: there are 6 possible dusty wines found in 4 possible locations,
	// so only 24 bits are needed to represent the complete state.  The bit that
	// indicates a specific wine drop is (counting from 0 as the LSB):
	//	(wine ID - 2271) * DUSTYSHIFT + (area ID - 178)
	private static final int DUSTYSHIFT = 4;
	private static final int MERL = 1 << (DUSTYSHIFT * 0);
	private static final int PORT = 1 << (DUSTYSHIFT * 1);
	private static final int PINOT = 1 << (DUSTYSHIFT * 2);
	private static final int ZINF = 1 << (DUSTYSHIFT * 3);
	private static final int MARS = 1 << (DUSTYSHIFT * 4);
	private static final int MUSC = 1 << (DUSTYSHIFT * 5);
	private static final int DUSTYMASK = MERL | PORT | PINOT | ZINF | MARS | MUSC;
	private static final int CORNERMASK = (1 << DUSTYSHIFT) - 1;
	// Only four distinct sets of the wines drop in any corner:
	private static final int A = MERL | PORT | PINOT;
	private static final int B = PINOT | ZINF | MARS;
	private static final int C = MARS | MUSC | MERL;
	private static final int D = PORT | ZINF | MUSC;

	private static final int PAT( final int a, final int b, final int c, final int d )
	{
		return a | (b << 1) | (c << 2) | (d << 3);
	}
	// There are 24 possible permutations of the 4 patterns in the 4 corners.
	// Test against all of them for now; hopefully it will turn out that some
	// are never actually generated, which would allow the actual pattern to
	// be recognized from fewer drops.
	private static final int[] DUSTYPATTERNS = new int[] {
		PAT( A, B, C, D ) /*  0 */, PAT( A, B, D, C ) /*  1 */,
		PAT( A, C, B, D ) /*  2 */, PAT( A, C, D, B ) /*  3 */,
		PAT( A, D, B, C ) /*  4 */, PAT( A, D, C, B ) /*  5 */,
		PAT( B, A, C, D ) /*  6 */, PAT( B, A, D, C ) /*  7 */,
		PAT( B, C, A, D ) /*  8 */, PAT( B, C, D, A ) /*  9 */,
		PAT( B, D, A, C ) /* 10 */, PAT( B, D, C, A ) /* 11 */,
		PAT( C, A, B, D ) /* 12 */, PAT( C, A, D, B ) /* 13 */,
		PAT( C, B, A, D ) /* 14 */, PAT( C, B, D, A ) /* 15 */,
		PAT( C, D, A, B ) /* 16 */, PAT( C, D, B, A ) /* 17 */,
		PAT( D, A, B, C ) /* 18 */, PAT( D, A, C, B ) /* 19 */,
		PAT( D, B, A, C ) /* 20 */, PAT( D, B, C, A ) /* 21 */,
		PAT( D, C, A, B ) /* 22 */, PAT( D, C, B, A ) /* 23 */,
	};

	private static final Pattern TABLEROW = Pattern.compile(
		"<tr.*?(</tr>|<tr|<table|</table>)" );
	private static final Pattern THREECELLS = Pattern.compile(
		"<td.*?<td.*?/(\\w+).gif.*?<td.*?</td>" );
	private static final String[] SHORTNAMES = new String[] {
		"Merlot", "Port", "Pinot Noir", "Zinfandel", "Marsala", "Muscat"
	};
	private static final String[] GLYPHNAMES = new String[] {
		"average", "vinegar", "spooky", "great", "glassy", "bad"
	};
	private static final AdventureResult[] DUSTYWINES = new AdventureResult[] {
		ItemPool.get( ItemPool.DUSTY_BOTTLE_OF_MERLOT, 1 ),
		ItemPool.get( ItemPool.DUSTY_BOTTLE_OF_PORT, 1 ),
		ItemPool.get( ItemPool.DUSTY_BOTTLE_OF_PINOT_NOIR, 1 ),
		ItemPool.get( ItemPool.DUSTY_BOTTLE_OF_ZINFANDEL, 1 ),
		ItemPool.get( ItemPool.DUSTY_BOTTLE_OF_MARSALA, 1 ),
		ItemPool.get( ItemPool.DUSTY_BOTTLE_OF_MUSCAT, 1 )
	};

	private static final int[] compute()
	{
		int layout = Preferences.getInteger( "cellarLayout" );
		int nMatches = 0;
		int whichMatch = -1;
		int commonBits = -1;
		// Return value is an array with the wine patterns for each corner,
		// and the total number of matching patterns in the last element.
		int[] wines = new int[] { 0, 0, 0, 0, 0 };
		for ( int i = 0; i < DUSTYPATTERNS.length; ++i )
		{
			int patt = DUSTYPATTERNS[ i ];
			if ( (patt & layout) != layout )
			{
				continue;	// didn't match this pattern
			}
			whichMatch = i;
			++nMatches;
			commonBits &= patt;
			// Now total up the possible wines in each corner:
			wines[ 0 ] += patt & DUSTYMASK;
			wines[ 1 ] += (patt >> 1) & DUSTYMASK;
			wines[ 2 ] += (patt >> 2) & DUSTYMASK;
			wines[ 3 ] += (patt >> 3) & DUSTYMASK;
		}

		if ( nMatches > 0 )
		{
			layout |= commonBits;
			Preferences.setInteger( "cellarLayout", layout );
		}
		wines[4] = nMatches;
		return wines;
	}

	public static final void decorate( final StringBuffer buffer )
	{
		int[] wines = compute();

		String[] names = new String[ 6 ];
		for ( int i = 0; i < 6; ++i )
		{
			int glyph = Preferences.getInteger( "lastDustyBottle" + (2271 + i) );
			String tag = ( glyph >= 1 && glyph <= 6 ) ?
				GLYPHNAMES[ glyph - 1 ] + " " + SHORTNAMES[ i ] :
				SHORTNAMES[ i ];
			names[ i ] = "<span>" + tag + "</span>";
		}

		Matcher row = TABLEROW.matcher( buffer.toString() );
		buffer.setLength( 0 );
		while ( row.find() )
		{
			if ( !row.group( 1 ).equals( "</tr>" ) )
			{
				continue;
			}
			Matcher cell = THREECELLS.matcher( row.group( 0 ) );
			if ( !cell.find() || cell.group( 1 ).indexOf( "glyph" ) != -1 )
			{
				continue;
			}
			String centerImg = cell.group( 1 );
			String lContent = "";
			String rContent = "";
			if ( centerImg.startsWith( "cellar2" ) )
			{
				lContent = getCornerData( wines[ 0 ], wines[ 4 ], names );
				rContent = getCornerData( wines[ 1 ], wines[ 4 ], names );
			}
			else if ( centerImg.equals( "cellar5" ) )
			{
				lContent = getCornerData( wines[ 2 ], wines[ 4 ], names );
				rContent = getCornerData( wines[ 3 ], wines[ 4 ], names );
			}
			row.appendReplacement( buffer,
					       "<tr><td width=120 height=100 align=right valign=center>" +
					       lContent + "</td>" +
					       cell.group( 0 ) +
					       "<td width=120 height=100 align=left valign=center>" +
					       rContent + "</td></tr>" );
		}
		row.appendTail( buffer );
	}

	private static final String getCornerData( int data, int total, String[] names )
	{
		// data contains the number of matching patterns that include each wine,
		// in six DUSTYSHIFT-wide bitfields.  Unpack it:
		int[] counts = new int[6];
		for ( int i = 0; i < 6; ++i )
		{
			counts[ i ] = data & CORNERMASK;
			data >>= DUSTYSHIFT;
		}

		StringBuffer buffer = new StringBuffer( "<small>" );
		boolean first = true;
		for ( int i = 0; i < 6; ++i )
		{
			int count = counts[ i ];
			if ( count == 0 )
			{
				continue;
			}
			if ( !first )
			{
				buffer.append( ",<br>" );
			}
			first = false;
			boolean wanted = GoalManager.hasGoal( DUSTYWINES[ i ] );
			if ( wanted )
			{
				buffer.append( "<b>" );
			}
			buffer.append( names[ i ] );
			if ( count != total )	// don't bother showing "100%" for known items
			{
				buffer.append( "&nbsp;" + (count * 100 / total) + "%" );
			}
			if ( wanted )
			{
				buffer.append( "</b>" );
			}
		}
		buffer.append( "</small>" );
		return buffer.toString();
	}

	// Recommend one of the corners, based on the current conditions.
	// Return a random corner if there are no relevant conditions.
	public static final int recommendCorner()
	{
		int[] data = compute();
		int[] matches = new int[] { 0, 0, 0, 0 };
		int max = 0;
		for ( int i = 0; i < 6; ++i )
		{
			if ( !GoalManager.hasGoal( DUSTYWINES[ i ] ) )
			{
				continue;
			}
			for ( int j = 0; j < 4; ++j )
			{
				matches[j] += (data[j] >> (i * DUSTYSHIFT)) & CORNERMASK;
				if ( matches[ j ] > max )
				{
					max = matches[ j ];
				}
			}
		}
		// Choose from among all elements with values equal to max -
		// there may be only one, or they may all be equal.
		while ( true )
		{
			int idx = KoLConstants.RNG.nextInt( 4 );
			if ( matches[ idx ] == max )
			{
				return idx + 178;
			}
		}
	}

	public static final void gainItem( int adv, AdventureResult item )
	{
		int id = item.getItemId();
		if ( adv < 178 || adv > 181 || id < 2271 || id > 2276 )
		{
			return;
		}
		int layout = Preferences.getInteger( "cellarLayout" );
		layout |= 1 << ((id - 2271) * DUSTYSHIFT + adv - 178);
		Preferences.setInteger( "cellarLayout", layout );
		compute();
	}
}
