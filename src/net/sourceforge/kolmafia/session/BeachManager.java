/**
 * Copyright (c) 2005-2018, KoLmafia development team
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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.RequestLogger;

import net.sourceforge.kolmafia.objectpool.IntegerPool;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.utilities.StringUtilities;

public class BeachManager
{
	// main.php?comb=1
	// choice.php?forceoption=0
	// choice.php?whichchoice=1388&pwd&option=1&minutes=420
	// choice.php?whichchoice=1388&pwd&option=3&buff=3
	// choice.php?whichchoice=1388&pwd&option=4&coords=8%2C4197
	// choice.php?whichchoice=1388&pwd&option=5

	// You walk for 420 minutes and find a nice stretch of beach.  Now...  Where to comb?
	private static final Pattern MINUTES_PATTERN = Pattern.compile( "You walk for ([\\d,]+) minutes and find a nice stretch of beach" );

	// (You have 11 free walks down the beach left today.)
	private static final Pattern FREE_WALK_PATTERN = Pattern.compile( "\\(You have ([\\d]+) free walks? down the beach left today.\\)" );

	// Visit Beach Head #10
	private static final Pattern BEACH_HEAD_PATTERN = Pattern.compile( "Visit Beach Head #([\\d]+)" );

	// You acquire an effect: <b>A Brush with Grossness</b>
	private static final Pattern EFFECT_PATTERN = Pattern.compile( "You acquire an effect: <b>([^<]+)</b>" );

	public static class BeachHead
	{
		public final int id;
		public final String effect;
		public final int beach;
		public final String desc;

		public BeachHead( int id, String effect, int beach, final String desc )
		{
			this.id = id;
			this.effect = effect;
			this.beach = beach;
			this.desc = desc;
		}
	}

	public static final BeachHead [] BEACH_HEADS = 
	{
		new BeachHead( 1, "Hot-Headed", 420, "hot" ),
		new BeachHead( 2, "Cold as Nice", 2323, "cold" ),
		new BeachHead( 3, "A Brush with Grossness", 4242, "stench" ),
		new BeachHead( 4, "Does It Have a Skull In There??", 6969, "spooky" ),
		new BeachHead( 5, "Oiled, Slick", 8888, "sleaze" ),
		new BeachHead( 6, "Lack of Body-Building", 37, "muscle" ),
		new BeachHead( 7, "We're All Made of Starfish", 3737, "mysticality" ),
		new BeachHead( 8, "Pomp and Circumsands", 7114, "moxie" ),
		new BeachHead( 9, "Resting Beach Face", 5555, "initiative" ),
		new BeachHead( 10, "Do I Know You From Somewhere?", 1111, "familiar" ),
		new BeachHead( 11, "You Learned Something Maybe!", 9696, "experience" ),
	};

	public static final Map<Integer, BeachHead> idToBeachHead = new TreeMap<Integer, BeachHead>();
	public static final Map<String, BeachHead> effectToBeachHead = new TreeMap<String, BeachHead>();
	public static final List<String> beachHeadDescs = new ArrayList<String>();

	static
	{
		for ( BeachHead head : BEACH_HEADS )
		{
			idToBeachHead.put( head.id, head );
			effectToBeachHead.put( head.effect, head );
			beachHeadDescs.add( head.desc );
		}
	};

	public static Set<Integer> getBeachHeadPreference( String property )
	{
		Set<Integer> beachHeads = new TreeSet<Integer>();
		for ( String iword : Preferences.getString( property ).split( " *, *" ) )
		{
			if ( !iword.equals( "" ) )
			{
				beachHeads.add( IntegerPool.get( StringUtilities.parseInt( iword ) ) );
			}
		}
		return beachHeads;
	}

	public static String setBeachHeadPreference( String property, Set<Integer> beachHeads )
	{
		StringBuilder buf = new StringBuilder();
		for ( Integer id : beachHeads )
		{
			if ( buf.length() > 0 )
			{
				buf.append( "," );
			}
			buf.append( String.valueOf( id ) );
		}
		String value = buf.toString();
		Preferences.setString( property, value );
		return value;
	}

	// Choice when using the Beach Comb or after combing, if you have adventures left
	public static final boolean parseCombUsage( final String text )
	{
		// You grab your comb and head to the start of the beach to find a good spot.
		// You grab your comb and head back to the start of the beach to find another good spot.
		if ( !text.contains( "to the start of the beach to find" ) )
		{
			return false;
		}

		Matcher matcher = BeachManager.FREE_WALK_PATTERN.matcher( text );
		int walksAvailable = matcher.find() ? StringUtilities.parseInt( matcher.group( 1 ) ) : 0;
		Preferences.setInteger( "_freeBeachWalksUsed", 11 - walksAvailable );

		// Parse the beach head shortcuts and see what we can deduce about them.

		// Start by retrieving what we know are unlocked
		Set<Integer> unlocked = getBeachHeadPreference( "beachHeadsUnlocked" );

		// Find which beach heads have available shortcuts
		Set<Integer> available = new TreeSet<Integer>();

		matcher = BeachManager.BEACH_HEAD_PATTERN.matcher( text );
		while ( matcher.find() )
		{
			available.add( StringUtilities.parseInt( matcher.group( 1 ) ) );
		}

		// All visible beach head shortcuts are unlocked
		Set<Integer> allUnlocked = new TreeSet<Integer>( unlocked );
		allUnlocked.addAll( available );
		setBeachHeadPreference( "beachHeadsUnlocked", allUnlocked );

		// All visible beach head shortcuts have not been visited today
		Set<Integer> visited = new TreeSet<Integer>( allUnlocked );
		visited.removeAll( available );
		setBeachHeadPreference( "_beachHeadsUsed", visited );

		return true;
	}

	// Choice when using the Beach Comb or after combing, if you have adventures left
	public static final boolean parseBeachHeadCombing( final String text )
	{
		// You return to the beach head and comb it once again, still trying to
		// not think too hard about what it <i>is</i>. It gives you some kind
		// of magical blessing as a tip.
		//
		// You already combed that head today.
		if ( !text.contains( "some kind of magical blessing" ) )
		{
			return false;
		}

		// You acquire an effect: <b>A Brush with Grossness</b>
		Matcher matcher = BeachManager.EFFECT_PATTERN.matcher( text );
		if ( !matcher.find() )
		{
			return false;
		}

		BeachHead beachHead = effectToBeachHead.get( matcher.group( 1 ) );
		if ( beachHead == null )
		{
			return false;
		}

		Integer id = IntegerPool.get( beachHead.id );

		// Add this beach head to set of unlocked (if not already present)
		Set<Integer> unlocked = getBeachHeadPreference( "beachHeadsUnlocked" );
		if ( !unlocked.contains( id ) )
		{
			unlocked.add( id );
			setBeachHeadPreference( "beachHeadsUnlocked", unlocked );
		}

		// Add this beach head to set of visited (if not already
		// present - which might be true if we parsed the Beach Comb first)
		Set<Integer> visited = getBeachHeadPreference( "_beachHeadsUsed" );
		if ( !visited.contains( id ) )
		{
			visited.add( id );
			setBeachHeadPreference( "_beachHeadsUsed", visited );
		}

		return true;
	}

	// Beach Layout:
	//
	//     (XXXX minutes down the beach)
	//              columns
	//     0  1  2  3  4  5  6  7  8  9
	// 10  x  x  x  x  x  x  x  x  x  x
	//  9  x  x  x  x  x  x  x  x  x  x
	//  8  x  x  x  x  x  x  x  x  x  x
	//  7  x  x  x  x  x  x  x  x  x  x
	//  6  x  x  x  x  x  x  x  x  x  x
	//  5  x  x  x  x  x  x  x  x  x  x
	//  4  x  x  x  x  x  x  x  x  x  x
	//  3  x  x  x  x  x  x  x  x  x  x
	//  2  x  x  x  x  x  x  x  x  x  x
	// (1) wave washed squares
	//
	// Coordinates: <row>,(minutes*10-column)
	//
	// I've seen a report that the number of rows on the beach changes from day to day. Tides?

	private static final Pattern MAP_PATTERN = Pattern.compile( "name=\"coords\" value=\"([\\d]+),([\\d]+)\".*?title=\"([^\"]*).*?otherimages/beachcomb/(.*?).png", Pattern.DOTALL );

	// Known "title" names
	//
	// rough sand
	// combed sand
	// a beach head
	// a sand castle
	//
	// There are others. whale.png is ... what? "a beached whale"?

	// Settings to hold current map:
	//
	// _beachLayout
	// _beachMinutes
	//
	// Unspaded, as far as I know, whether what's at a particular coordinate changes from day to day.
	// "rough sand" can change to "combed sand" via actions of other players.
	// Presumably all "combed sand" periodically becomes "rough sand" (at rollover?)
	// Presumably, "beach head" squares do not change
	//
	// So, we'll use "_" preferences, for now.

	public static final void parseBeachMap( final String text )
	{
		Matcher matcher = BeachManager.MINUTES_PATTERN.matcher( text );
		if ( !matcher.find() )
		{
			return;
		}

		int minutes = StringUtilities.parseInt( matcher.group(1) );

		StringBuilder layout = new StringBuilder();

		// Since number of rows vary, make a map to hold the layout for each row
		Map<Integer, String> rowLayout = new TreeMap<Integer, String>();
		int currentRow = -1;

		matcher = BeachManager.MAP_PATTERN.matcher( text );
		while ( matcher.find() )
		{
			int row = StringUtilities.parseInt( matcher.group(1) );
			int col = StringUtilities.parseInt( matcher.group(2) );
			String type = matcher.group(3);
			String image = matcher.group(4);

			if ( row != currentRow )
			{
				if ( currentRow != -1 )
				{
					rowLayout.put( currentRow, layout.toString() );
				}
				layout.setLength( 0 );
				currentRow = row;
			}

			char ch =
				type.equals( "rough sand" ) ? 'r' :
				type.equals( "combed sand" ) ? 'c' :
				type.equals( "a beach head" ) ? 'H' :
				type.equals( "a sand castle" ) ? 'C' :
				image.equals( "whale" ) ?  'W':
				'?';

			if ( ch == '?' )
			{
				logText( "Unknown beach square at " + minutes + ":" + row + "," + col + ": text = '" + text + "' image = '" + image + "'." );
			}

			layout.append( ch );
		}

		if ( currentRow != -1 )
		{
			rowLayout.put( currentRow, layout.toString() );
		}

		layout.setLength( 0 );

		for ( Entry<Integer, String> entry : rowLayout.entrySet() )
		{
			int row = entry.getKey();
			String cols = entry.getValue();
			if ( layout.length() > 0 )
			{
				layout.append( "," );
			}
			layout.append( String.valueOf( row ) );
			layout.append( ':' );
			layout.append( cols );
		}

		Preferences.setInteger( "_beachMinutes", minutes );
		Preferences.setString( "_beachLayout", layout.toString() );
	}

	private static final void logText( final String text )
	{
		RequestLogger.printLine( text );
		RequestLogger.updateSessionLog( text );
	}
}
