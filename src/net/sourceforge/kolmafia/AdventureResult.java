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

import java.text.DecimalFormat;
import java.text.ParseException;

public class AdventureResult implements Comparable
{
	private int resultCount;
	private String resultName;
	private int resultPriority;

	private static DecimalFormat df = new DecimalFormat();

	public static final String MEAT = "Meat";
	public static final String MUS = "Mus";
	public static final String MYS = "Mys";
	public static final String MOX = "Mox";
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

	public AdventureResult( String resultName )
	{	this( resultName, 0 );
	}

	public AdventureResult( String resultName, int resultCount )
	{
		this.resultName = resultName;
		this.resultCount = resultCount;

		this.resultPriority = resultName.equals( MEAT ) ? 0 : resultName.equals( MUS ) ? 1 :
			resultName.equals( MYS ) ? 2 : resultName.equals( MOX ) ? 3 : 4;
	}

	public static AdventureResult parseResult( String s )
	{
		if ( s.startsWith("You gain") || s.startsWith("You lose") )
		{
			// A stat has been modified - now you figure out which one it was,
			// how much it's been modified by, and return the appropriate value

			StringTokenizer parsedGain = new StringTokenizer( s, " ." );
			parsedGain.nextToken();

			try
			{
				int modifier = Integer.parseInt(
					(parsedGain.nextToken().equals("gain") ? "" : "-") + parsedGain.nextToken() );
				String statname = parsedGain.nextToken();

				// Stats actually fall into one of four categories - simply pick the
				// correct one and return the result.

				if ( statname.equals( MEAT ) )
					return new AdventureResult( MEAT, modifier );

				else if ( MUS_SUBSTAT.contains( statname ) )
					return new AdventureResult( MUS, modifier );

				else if ( MYS_SUBSTAT.contains( statname ) )
					return new AdventureResult( MYS, modifier );

				else if ( MOX_SUBSTAT.contains( statname ) )
					return new AdventureResult( MOX, modifier );
			}
			catch ( NumberFormatException e )
			{
				// If any integer parsing was ruined as a result of the
				// initial parse, let it fall through - it was probably
				// a strange result, but you might as well return whatever
				// adventure result object would have been returned, had
				// it been an item.
			}

			// If for some bizarre reason it's gotten this far, it was probably
			// some brand-new item whose name started with "You gain" or even
			// "You lose," has a number as its third token, and lots of other
			// bizarre things - let it fall through.
		}

		try
		{
			StringTokenizer parsedItem = new StringTokenizer( s, "()" );
			return new AdventureResult( parsedItem.nextToken().trim(),
				parsedItem.hasMoreTokens() ? df.parse(parsedItem.nextToken()).intValue() : 1 );
		}
		catch ( ParseException e )
		{
			// If a parse still manages to fail (because a non-numeric number
			// is enclosed in parenthesis in the item's name), pretend the
			// whole thing (including parenthesis) is the item's name.

			return new AdventureResult( s, 1 );
		}
	}

	public void clear()
	{	resultCount = 0;
	}

	public static AdventureResult add( AdventureResult left, AdventureResult right )
	{
		if ( left == null )
			return right;
		if ( right == null )
			return left;

		return !left.resultName.equals( right.resultName ) ? null :
			new AdventureResult( left.resultName, left.resultCount + right.resultCount );
	}

	public String toString()
	{
		return (resultName.equals(MEAT) || resultName.equals(MUS) || resultName.equals(MYS) || resultName.equals(MOX)) ?
			resultName + ": " + resultCount :
				resultName.equals(DIVIDER) ? "--------------------------------" :
					resultName + "(" + resultCount + ")";
	}

	public boolean equals( Object o )
	{
		if ( !(o instanceof AdventureResult) || o == null )
			return false;

		return resultName.equals( ((AdventureResult)o).resultName );
	}

	public int compareTo( Object o )
	{
		if ( !(o instanceof AdventureResult) || o == null )
			return -1;

		AdventureResult ar = (AdventureResult) o;

		int priorityDifference = resultPriority - ar.resultPriority;
		return priorityDifference != 0 ? priorityDifference : resultName.compareToIgnoreCase( ar.resultName );
	}
}