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

public class StarChartRequest extends KoLRequest implements Comparable
{
	private String name;
	private int stars, lines;
	private int quantityNeeded;

	public static final StarChartRequest BUCKLER = new StarChartRequest( "star buckler", 4, 6 );
	public static final StarChartRequest CROSSBOW = new StarChartRequest( "star crossbow", 5, 6 );
	public static final StarChartRequest HAT = new StarChartRequest( "star hat", 5, 3 );
	public static final StarChartRequest PANTS = new StarChartRequest( "star pants", 7, 7 );
	public static final StarChartRequest STAFF = new StarChartRequest( "star staff", 6, 5 );
	public static final StarChartRequest STARFISH = new StarChartRequest( "star starfish", 6, 4 );
	public static final StarChartRequest SWORD = new StarChartRequest( "star sword", 7, 4 );
	public static final StarChartRequest THROWING = new StarChartRequest( "star throwing star", 4, 2 );
	public static final StarChartRequest STARKEY = new StarChartRequest( "Richard's star key", 8, 7 );

	private static final StarChartRequest [] STAR_ITEMS =
	{
		BUCKLER, CROSSBOW, HAT, PANTS, STAFF, STARFISH, SWORD, THROWING, STARKEY
	};

	private StarChartRequest( String name, int stars, int lines )
	{
		super( null, "starchart.php" );
		this.name = name;
		this.stars = stars;
		this.lines = lines;
	}

	public StarChartRequest( KoLmafia client, StarChartRequest baseRequest, int quantityNeeded )
	{
		super( client, "starchart.php" );
		addFormField( "action", "makesomething" );
		addFormField( "pwd", client.getPasswordHash() );
		addFormField( "numstars", String.valueOf( baseRequest.stars ) );
		addFormField( "numlines", String.valueOf( baseRequest.lines ) );

		this.name = baseRequest.name;
		this.stars = baseRequest.stars;
		this.lines = baseRequest.lines;
		this.quantityNeeded = quantityNeeded;
	}

	public int compareTo( Object o )
	{
		return o == null ? -1 :
			this.toString().compareToIgnoreCase( o.toString() );
	}

	public static List getPossibleCombinations( KoLmafia client )
	{
		List inventory = client.getInventory();

		int chartIndex = inventory.indexOf( new AdventureResult( "star chart", 0 ) );
		int starsIndex = inventory.indexOf( new AdventureResult( "star", 0 ) );
		int linesIndex = inventory.indexOf( new AdventureResult( "line", 0 ) );

		int chartValue = chartIndex == -1 ? 0 : ((AdventureResult) inventory.get( chartIndex )).getCount();
		int starsValue = starsIndex == -1 ? 0 : ((AdventureResult) inventory.get( starsIndex )).getCount();
		int linesValue = linesIndex == -1 ? 0 : ((AdventureResult) inventory.get( linesIndex )).getCount();

		List results = new ArrayList();
		for ( int i = 0; i < STAR_ITEMS.length; ++i )
		{
			int maximumPossible = Math.min( Math.min( chartValue, starsValue / STAR_ITEMS[i].stars ), linesValue / STAR_ITEMS[i].lines );
			if ( maximumPossible > 0 )
				results.add( new StarChartRequest( client, STAR_ITEMS[i], maximumPossible ) );
		}

		return results;
	}

	public String getName()
	{	return name;
	}

	public String toString()
	{	return name + " (" + quantityNeeded + ")";
	}

	public void setQuantityNeeded( int quantityNeeded )
	{	this.quantityNeeded = Math.min( this.quantityNeeded, quantityNeeded );
	}

	public int getQuantityNeeded()
	{	return quantityNeeded;
	}

	public void run()
	{
		for ( int i = 0; i < quantityNeeded; ++i )
		{
			updateDisplay( DISABLED_STATE, "Creating " + name + " (" + (i+1) + " of " + quantityNeeded + ")..." );
			makeConstellation();
		}
	}

	private void makeConstellation()
	{
		super.run();
		client.processResult( new AdventureResult( "star chart", -1 ) );
		client.processResult( new AdventureResult( "star", 0 - stars ) );
		client.processResult( new AdventureResult( "line", 0 - lines ) );
		client.processResult( new AdventureResult( getName(), 1 ) );
	}
}