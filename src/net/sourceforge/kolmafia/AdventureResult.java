/**
 * Copyright (c) 2005, KoLmafia development team
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
 *  [3] Neither the name "KoLmafia development team" nor the names of
 *      its contributors may be used to endorse or promote products
 *      derived from this software without specific prior written
 *      permission.
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

package net.sourceforge.kolmafia;

import java.util.List;
import java.util.ArrayList;
import java.util.StringTokenizer;
import java.text.ParseException;
import java.text.DecimalFormat;

/**
 * A container class which encapsulates the results from an adventure and
 * handles the transformation of these results into a string.  At the
 * current time, only monetary gains, stat gains and item gains (and losses)
 * can be encapsulated; hit point, mana point and adventure gains/losses
 * will be encapsulated at a later date.
 */

public class AdventureResult implements Comparable
{
	private int [] count;
	private String name;
	private int priority;

	private static final int ADV_PRIORITY = 1;
	private static final int MEAT_PRIORITY = 2;
	private static final int SUBSTAT_PRIORITY = 3;
	private static final int DIVIDER_PRIORITY = 4;
	private static final int ITEM_PRIORITY = 5;

	private static final DecimalFormat df = new DecimalFormat();

	public static final String ADV = "Adv";
	public static final String MEAT = "Meat";
	public static final String SUBSTATS = "Substats";
	public static final String DIVIDER = "";

	private static List MUS_SUBSTAT = new ArrayList();
	private static List MYS_SUBSTAT = new ArrayList();
	private static List MOX_SUBSTAT = new ArrayList();

	static
	{
		MUS_SUBSTAT.add( "Beefiness" );  MUS_SUBSTAT.add( "Fortitude" );  MUS_SUBSTAT.add( "Muscleboundness" );  MUS_SUBSTAT.add( "Strengthliness" );  MUS_SUBSTAT.add( "Strongness" );
		MYS_SUBSTAT.add( "Enchantedness" );  MYS_SUBSTAT.add( "Magicalness" );  MYS_SUBSTAT.add( "Mysteriousness" );  MYS_SUBSTAT.add( "Wizardliness" );
		MOX_SUBSTAT.add( "Cheek" );  MOX_SUBSTAT.add( "Chutzpah" );  MOX_SUBSTAT.add( "Roguishness" );  MOX_SUBSTAT.add( "Sarcasm" );  MOX_SUBSTAT.add( "Smarm" );
	}

	/**
	 * Constructs a new <code>AdventureResult</code> with the given name.
	 * The amount of gain will default to zero.  This constructor should
	 * only be used for initializing a field.
	 *
	 * @param	name	The name of the result
	 */

	public AdventureResult( String name )
	{	this( name, name.equals(SUBSTATS) ? new int[3] : new int[1] );
	}

	/**
	 * Constructs a new <code>AdventureResult</code> with the given name
	 * which increased/decreased by the given value.  This constructor
	 * should be used for most results.
	 *
	 * @param	name	The name of the result
	 * @param	count	How many of the noted result were gained
	 */

	public AdventureResult( String name, int count )
	{
		this( name, new int[1] );
		this.count[0] = count;
	}

	/**
	 * Constructs a new <code>AdventureResult</code> with the given name
	 * and increase in stat gains.  This method is used internally to
	 * represent stat gains, and potentially health and mana gains in
	 * future versions.
	 *
	 * @param	name	The name of the result
	 * @param	count	How many of the noted result were gained
	 */

	private AdventureResult( String name, int [] count )
	{
		this( name, count,
			name.equals(ADV) ? ADV_PRIORITY :
			name.equals(MEAT) ? MEAT_PRIORITY :
			name.equals(SUBSTATS) ? SUBSTAT_PRIORITY :
			name.equals(DIVIDER) ? DIVIDER_PRIORITY : ITEM_PRIORITY );
	}

	/**
	 * Constructs a new <code>AdventureResult</code> with the given name
	 * and increase in stat gains.  This also manually sets the priority
	 * of the element to the given value.  This method is used internally.
	 *
	 * @param	name	The name of the result
	 * @param	count	How many of the noted result were gained
	 * @param	priority	The priority of this result
	 */

	private AdventureResult( String name, int [] count, int priority )
	{
		this.name = name;
		this.count = new int[ count.length ];

		for ( int i = 0; i < count.length; ++i )
			this.count[i] = count[i];

		this.priority = priority;
	}

	/**
	 * Accessor method to determine if this result is an item, as opposed
	 * to meat, drunkenness, adventure or substat gains.
	 *
	 * @return	<code>true</code> if this result represents an item
	 */

	public boolean isItem()
	{	return priority == ITEM_PRIORITY;
	}

	/**
	 * Accessor method to retrieve the name associated with the result.
	 * @return	The name of the result
	 */

	public String getName()
	{	return name;
	}

	/**
	 * Accessor method to retrieve the total value associated with the result.
	 * In the event of substat points, this returns the total subpoints within
	 * the <code>AdventureResult</code>; in the event of an item or meat gains,
	 * this will return the total number of meat/items in this result.
	 *
	 * @return	The amount associated with this result
	 */

	public int getCount()
	{
		int totalCount = 0;
		for ( int i = 0; i < count.length; ++i )
			totalCount += count[i];
		return totalCount;
	}

	/**
	 * A static method which parses the given string for any content
	 * which might be applicable to an <code>AdventureResult</code>,
	 * and returns the resulting <code>AdventureResult</code>.
	 *
	 * @param	s	The string suspected of being an <code>AdventureResult</code>
	 * @return	An <code>AdventureResult</code> with the appropriate data
	 * @throws	NumberFormatException	The string was not a recognized <code>AdventureResult</code>
	 * @throws	ParseException	The value enclosed within parentheses was not a number.
	 */

	public static AdventureResult parseResult( String s ) throws NumberFormatException, ParseException
	{
		if ( s.startsWith("You gain") || s.startsWith("You lose") )
		{
			// A stat has been modified - now you figure out which one it was,
			// how much it's been modified by, and return the appropriate value

			StringTokenizer parsedGain = new StringTokenizer( s, " ." );
			parsedGain.nextToken();

			int modifier = Integer.parseInt(
				(parsedGain.nextToken().equals("gain") ? "" : "-") + parsedGain.nextToken() );
			String statname = parsedGain.nextToken();

			// Stats actually fall into one of four categories - simply pick the
			// correct one and return the result.

			if ( statname.toLowerCase().startsWith( "adv" ) )
				return new AdventureResult( ADV, modifier );
			else if ( statname.equals( MEAT ) )
				return new AdventureResult( MEAT, modifier );

			else
			{
				// In the current implementations, all stats gains are located
				// inside of a generic adventure which indicates how much of
				// each substat is gained.

				int [] gained =
				{
					MUS_SUBSTAT.contains( statname ) ? modifier : 0,
					MYS_SUBSTAT.contains( statname ) ? modifier : 0,
					MOX_SUBSTAT.contains( statname ) ? modifier : 0
				};

				return new AdventureResult( SUBSTATS, gained );
			}
		}

		StringTokenizer parsedItem = new StringTokenizer( s, "()" );
		return new AdventureResult( parsedItem.nextToken().trim(),
			parsedItem.hasMoreTokens() ? df.parse(parsedItem.nextToken()).intValue() : 1 );
	}

	/**
	 * Converts the <code>AdventureResult</code> to a <code>String</code>.  This is
	 * especially useful in debug, or if the <code>AdventureResult</code> is to
	 * be displayed in a <code>ListModel</code>.
	 *
	 * @return	The string version of this <code>AdventureResult</code>
	 */

	public String toString()
	{
		return
			name.equals(ADV) || name.equals(MEAT) ? " " + name + ": " + df.format(count[0]) :
			name.equals(SUBSTATS) ? " Substats: " + df.format(count[0]) + " / " + df.format(count[1]) + " / " + df.format(count[2]) :
			name.equals(DIVIDER) ? DIVIDER :
			" " + name.replaceAll( "&ntilde;", "ñ" ).replaceAll( "&trade;", "©" ) +
				((count[0] == 1) ? "" : (" (" + df.format(count[0]) + ")"));
	}

	/**
	 * Compares the <code>AdventureResult</code> with the given object for name
	 * equality.  Note that this will still return <code>true</code> if the values
	 * do not match; this merely matches on names.
	 *
	 * @param	o	The <code>Object</code> to be compared with this <code>AdventureResult</code>
	 * @return	<code>true</code> if the <code>Object</code> is an <code>AdventureResult</code>
	 *			and has the same name as this one
	 */

	public boolean equals( Object o )
	{
		if ( !(o instanceof AdventureResult) || o == null )
			return false;

		return name.equals( ((AdventureResult)o).name );
	}

	/**
	 * Compares the <code>AdventureResult</code> with the given object for name
	 * equality and priority differences.  Return values are consistent with the
	 * rules laid out in {@link java.lang.Comparable#compareTo(Object)}.
	 */

	public int compareTo( Object o )
	{
		if ( !(o instanceof AdventureResult) || o == null )
			return -1;

		AdventureResult ar = (AdventureResult) o;

		int priorityDifference = priority - ar.priority;
		return priorityDifference != 0 ? priorityDifference : name.compareToIgnoreCase( ar.name );
	}

	/**
	 * Utility method used for adding a given <code>AdventureResult</code> to a
	 * tally of <code>AdventureResult</code>s.
	 *
	 * @param	tally	The tally accumulating <code>AdventureResult</code>s
	 * @param	result	The result to add to the tally
	 */

	public static void addResultToList( List tally, AdventureResult result )
	{
		int index = tally.indexOf( result );

		if ( index == -1 )
		{
			index = tally.size();
			tally.add( result );
		}
		else
			tally.set( index, add( result, (AdventureResult) tally.get( index ) ) );

		// Check to make sure that the result didn't transform the value
		// to zero - if it did, then remove the item from the list.

		AdventureResult netWorth = (AdventureResult) tally.get( index );
		if ( netWorth.isItem() && netWorth.getCount() == 0 )
			tally.remove( netWorth );
	}

	/**
	 * A static method which adds the two <code>AdventureResult</code>s together to
	 * produce a new <code>AdventureResult</code> containing the sum of the results.
	 * Because addition is commutative, it doesn't matter which one is left or right;
	 * it is named simply for convenience.  Note that if the left and right operands
	 * do not have the same name, this method returns <code>null</code>; if either
	 * operand is null, the method returns the other operand.
	 *
	 * @param	left	The left operand
	 * @param	right	The right operand
	 * @return	An <code>AdventureResult</code> containing the sum of the left and right operands
	 */

	private static AdventureResult add( AdventureResult left, AdventureResult right )
	{
		if ( left == null )
			return right;
		if ( right == null )
			return left;

		if ( !left.name.equals( right.name ) )
			return null;

		if ( left.count.length == 1 )
		{
			int totalCount = left.count[0] + right.count[0];

			if ( left.name.equals( ADV ) && totalCount < 0 )
				totalCount = 0;

			return new AdventureResult( left.name, totalCount );
		}
		else
		{
			int [] totals = new int[3];
			for ( int i = 0; i < 3; ++i )
				totals[i] = left.count[i] + right.count[i];
			return new AdventureResult( left.name, totals );
		}
	}
}