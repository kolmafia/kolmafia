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
	private int [] resultCount;
	private String resultName;
	private int resultPriority;

	private static final DecimalFormat df = new DecimalFormat();

	public static final String MEAT = "Meat";
	public static final String SUBSTATS = "Stats";
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
	 * @param	resultName	The name of the result
	 */

	public AdventureResult( String resultName )
	{	this( resultName, resultName.equals(SUBSTATS) ? new int[3] : new int[1] );
	}

	/**
	 * Constructs a new <code>AdventureResult</code> with the given name
	 * which increased/decreased by the given value.  This constructor
	 * should be used for most results.
	 *
	 * @param	resultName	The name of the result
	 * @param	resultCount	How many of the noted result were gained
	 */

	public AdventureResult( String resultName, int resultCount )
	{
		this.resultName = resultName;
		this.resultCount = new int[1];
		this.resultCount[0] = resultCount;
		this.resultPriority = resultName.equals(MEAT) ? 0 : resultName.equals(SUBSTATS) ? 1 : resultName.equals(DIVIDER) ? 2 : 3;
	}

	/**
	 * Constructs a new <code>AdventureResult</code> with the given name
	 * and increase in stat gains.  This method is used internally to
	 * represent stat gains, and potentially health and mana gains in
	 * future versions.
	 */

	private AdventureResult( String resultName, int [] resultCount )
	{
		this.resultName = resultName;
		this.resultCount = new int[ resultCount.length ];

		for ( int i = 0; i < resultCount.length; ++i )
			this.resultCount[i] = resultCount[i];

		this.resultPriority = resultName.equals(MEAT) ? 0 : resultName.equals(SUBSTATS) ? 1 : resultName.equals(DIVIDER) ? 2 : 3;
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

			if ( statname.equals( MEAT ) )
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
		return resultName.equals(MEAT) ? " Meat: " + df.format(resultCount[0]) :
			resultName.equals(SUBSTATS) ? " Substats: " + df.format(resultCount[0]) + " / " + df.format(resultCount[1]) + " / " + df.format(resultCount[2]) :
			resultName.equals(DIVIDER) ? DIVIDER :
			" " + resultName + " (" + df.format(resultCount[0]) + ")";
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

		return resultName.equals( ((AdventureResult)o).resultName );
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

		int priorityDifference = resultPriority - ar.resultPriority;
		return priorityDifference != 0 ? priorityDifference : resultName.compareToIgnoreCase( ar.resultName );
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
			tally.add( result );
		else
			tally.set( index, add( result, (AdventureResult) tally.get( index ) ) );
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

		if ( !left.resultName.equals( right.resultName ) )
			return null;

		if ( left.resultCount.length == 1 )
			return new AdventureResult( left.resultName, left.resultCount[0] + right.resultCount[0] );
		else
		{
			int [] totals = new int[3];
			for ( int i = 0; i < 3; ++i )
				totals[i] = left.resultCount[i] + right.resultCount[i];
			return new AdventureResult( left.resultName, totals );
		}
	}
}