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
import java.util.Iterator;
import java.util.StringTokenizer;
import java.text.ParseException;
import java.text.DecimalFormat;

import java.awt.Color;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JLabel;
import java.awt.Component;
import javax.swing.JList;

/**
 * A container class which encapsulates the results from an adventure and
 * handles the transformation of these results into a string.  At the
 * current time, only monetary gains, stat gains and item gains (and losses)
 * can be encapsulated; hit point, mana point and adventure gains/losses
 * will be encapsulated at a later date.
 */

public class AdventureResult implements Comparable, KoLConstants
{
	private int itemID;
	private int [] count;
	private String name;
	private int priority;

	private static final int HP_PRIORITY = 0;
	private static final int MP_PRIORITY = 1;
	private static final int ADV_PRIORITY = 2;
	private static final int DRUNK_PRIORITY = 3;
	private static final int MEAT_PRIORITY = 4;
	private static final int SUBSTAT_PRIORITY = 5;
	private static final int FULLSTAT_PRIORITY = 6;
	private static final int DIVIDER_PRIORITY = 7;
	private static final int ITEM_PRIORITY = 8;
	private static final int EFFECT_PRIORITY = 9;

	public static final String HP = "HP";
	public static final String MP = "MP";
	public static final String ADV = "Adv";
	public static final String DRUNK = "Drunk";
	public static final String MEAT = "Meat";
	public static final String SUBSTATS = "Substats";
	public static final String FULLSTATS = "Fullstats";
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
	{
		this( name, name.equals(SUBSTATS) ? new int[3] : new int[1] );
		this.itemID = TradeableItemDatabase.getItemID( name );
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

		if ( isStatusEffect() )
			name = StatusEffectDatabase.getEffectName( StatusEffectDatabase.getEffectID( name ) );

		this.count[0] = count;
		this.itemID = TradeableItemDatabase.getItemID( name );
	}

	public AdventureResult( int itemID, int count )
	{
		this( TradeableItemDatabase.getItemName( itemID ), new int[1] );
		this.count[0] = count;
		this.itemID = itemID;
	}

	/**
	 * Constructs a new <code>AdventureResult</code> with the given name
	 * and increase in stat gains.  This method is used internally to
	 * represent stat gains, but if there are any other results which
	 * should be represented this way, this constructor is also accessible.
	 *
	 * @param	name	The name of the result
	 * @param	count	How many of the noted result were gained
	 */

	public AdventureResult( String name, int [] count )
	{
		this( name, count, name == null ? ITEM_PRIORITY :
			name.equals(HP) ? HP_PRIORITY :
			name.equals(MP) ? MP_PRIORITY :
			name.equals(ADV) ? ADV_PRIORITY :
			name.equals(DRUNK) ? DRUNK_PRIORITY :
			name.equals(MEAT) ? MEAT_PRIORITY :
			name.equals(SUBSTATS) ? SUBSTAT_PRIORITY :
			name.equals(FULLSTATS) ? FULLSTAT_PRIORITY :
			name.equals(DIVIDER) ? DIVIDER_PRIORITY :
			!StatusEffectDatabase.contains( name ) ? ITEM_PRIORITY : EFFECT_PRIORITY );
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
		this.name = name == null ? "(unrecognized item)" : name;

		this.count = new int[ count.length ];

		for ( int i = 0; i < count.length; ++i )
			this.count[i] = count[i];

		this.priority = priority;
	}

	/**
	 * Accessor method to determine if this result is a status effect.
	 * @return	<code>true</code> if this result represents a status effect
	 */

	public boolean isStatusEffect()
	{	return priority == EFFECT_PRIORITY;
	}

	/**
	 * Accessor method to determine if this result is a muscle gain.
	 * @return	<code>true</code> if this result represents muscle subpoint gain
	 */

	public boolean isMuscleGain()
	{	return priority == SUBSTAT_PRIORITY && count[0] != 0;
	}

	/**
	 * Accessor method to determine if this result is a mysticality gain.
	 * @return	<code>true</code> if this result represents mysticality subpoint gain
	 */

	public boolean isMysticalityGain()
	{	return priority == SUBSTAT_PRIORITY && count[1] != 0;
	}

	/**
	 * Accessor method to determine if this result is a muscle gain.
	 * @return	<code>true</code> if this result represents muscle subpoint gain
	 */

	public boolean isMoxieGain()
	{	return priority == SUBSTAT_PRIORITY && count[2] != 0;
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
	 * Accessor method to retrieve the item ID associated with the result,
	 * if this is an item and the item ID is known.
	 *
	 * @return	The item ID associated with this item
	 */

	public int getItemID()
	{	return itemID;
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
		try
		{
			if ( s.startsWith("You gain") || s.startsWith("You lose") )
			{
				// A stat has been modified - now you figure out which one it was,
				// how much it's been modified by, and return the appropriate value

				StringTokenizer parsedGain = new StringTokenizer( s, " ." );
				parsedGain.nextToken();

				int modifier = df.parse(
					(parsedGain.nextToken().startsWith("gain") ? "" : "-") + parsedGain.nextToken() ).intValue();
				String statname = parsedGain.nextToken();

				// Stats actually fall into one of four categories - simply pick the
				// correct one and return the result.

				if ( parsedGain.hasMoreTokens() )
				{
					char identifier = statname.charAt(0);
					return new AdventureResult( ( identifier == 'H' || identifier == 'h' ) ? HP : MP, modifier );
				}

				if ( statname.startsWith( "Adv" ) )
					return new AdventureResult( ADV, modifier );
				else if ( statname.startsWith( "Dru" ) )
					return new AdventureResult( DRUNK, modifier );
				else if ( statname.startsWith( "Mea" ) )
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
			String parsedItemName = parsedItem.nextToken().trim();
			String parsedItemCount = parsedItem.hasMoreTokens() ? parsedItem.nextToken() : "1";

			return new AdventureResult( parsedItemName, df.parse( parsedItemCount ).intValue() );
		}
		catch ( Exception e )
		{
			// If some weird exception occurs somewhere inbetween,
			// simply return null.  Strangely, this exception
			// should never occur (like all other parsed exceptions),
			// but is caught as a matter of formality.

			return null;
		}
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
		if ( name.equals(HP) || name.equals(MP) || name.equals(ADV) || name.equals(DRUNK) || name.equals(MEAT) )
			return "" + name + ": " + df.format(count[0]);

		if ( name.equals(SUBSTATS) || name.equals(FULLSTATS) )
			return "" + name + ": " + df.format(count[0]) + " / " + df.format(count[1]) + " / " + df.format(count[2]);

		if ( name.equals(DIVIDER) )
			return DIVIDER;

		if ( itemID == 0 )
			return "" + name + ((count[0] == 1) ? "" : (" (" + df.format(count[0]) + ")"));

		String stringName = itemID == 41 ? "ice-cold beer - Shiltz" : itemID == 81 ? "ice-cold beer - Willer" :
			name.replaceAll( "&ntilde;", "ñ" ).replaceAll( "&trade;", "©" );

		return stringName + " (" + df.format(count[0]) + ")";
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

		return name.equalsIgnoreCase( ((AdventureResult)o).name ) &&
			(((AdventureResult)o).isItem() ? itemID == ((AdventureResult)o).itemID : true);
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
		int nameComparison = name.compareToIgnoreCase( ar.name );
		return priorityDifference != 0 ? priorityDifference : nameComparison != 0 ? nameComparison : isItem() ? itemID - ar.itemID : 0;
	}

	/**
	 * Utility method for decrementing every value of the list by the
	 * given amount.  This is used specifically by effect management,
	 * but can be applied elsewhere.
	 *
	 * @param	tally	The tally accumulating <code>AdventureResult</code>s
	 * @param	reductionAmount	The amount to reduce the tally by
	 */

	public static void reduceTally( List tally, int reductionAmount )
	{
		Object [] tallyAsArray = tally.toArray();
		for ( int i = 0; i < tallyAsArray.length; ++i )
			addResultToList( tally, new AdventureResult( ((AdventureResult)tallyAsArray[i]).name, reductionAmount ) );
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

		// First, filter out things where it's a simple addition of an
		// item, or something which may not result in a change in the
		// state of the tally list.

		if ( index == -1 )
		{
			if ( !result.isItem() || result.getCount() != 0 )
				tally.add( result );
			return;
		}

		AdventureResult actualResult = add( result, (AdventureResult) tally.get( index ) );

		// Check to make sure that the result didn't transform the value
		// to zero - if it did, then remove the item from the list if
		// it's an item (non-items are exempt).

		if ( actualResult.getCount() == 0 )
		{
			if ( actualResult.isItem() )
			{
				tally.remove( result );
				return;
			}
			else if ( actualResult.isStatusEffect() )
			{
				tally.remove( result );
				return;
			}
		}
		else if ( actualResult.getCount() < 0 )
		{
			if ( actualResult.isStatusEffect() )
			{
				tally.remove( result );
				return;
			}
		}

		if ( actualResult.getName().equals( AdventureResult.ADV ) && actualResult.getCount() < 0 )
			actualResult = new AdventureResult( AdventureResult.ADV, 0 );

		tally.set( index, actualResult );
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

		int [] totals = new int[ left.count.length ];
		for ( int i = 0; i < left.count.length; ++i )
			totals[i] = left.count[i] + right.count[i];

		return left.isItem() && TradeableItemDatabase.contains( left.name ) ?
			new AdventureResult( left.itemID, totals[0] ) : new AdventureResult( left.name, totals );
	}

	public static AutoSellCellRenderer getAutoSellCellRenderer()
	{	return new AutoSellCellRenderer();
	}

	private static class AutoSellCellRenderer extends DefaultListCellRenderer
	{
		public AutoSellCellRenderer()
		{	setOpaque( true );
		}

		public Component getListCellRendererComponent( JList list, Object value, int index, boolean isSelected, boolean cellHasFocus )
		{
			Component defaultComponent = super.getListCellRendererComponent( list, value, index, isSelected, cellHasFocus );

			if ( value == null || !(value instanceof AdventureResult) )
				return defaultComponent;

			AdventureResult ar = (AdventureResult) value;

			String stringName = ar.itemID == 41 ? "ice-cold beer (Schlitz)" : ar.itemID == 81 ? "ice-cold beer (Willer)" : ar.name;

			int autoSellValue = TradeableItemDatabase.getPriceByID( ar.itemID );
			String stringForm = "" + stringName + ((autoSellValue == 0) ? "" : (" (" + df.format(autoSellValue) + " meat)")) +
				((ar.count[0] == 1) ? "" : (" (" + df.format(ar.count[0]) + ")"));

			((JLabel) defaultComponent).setText( stringForm );
			return defaultComponent;
		}
	}
}