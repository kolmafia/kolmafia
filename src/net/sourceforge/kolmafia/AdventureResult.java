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
import java.util.WeakHashMap;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.StringTokenizer;
import java.text.ParseException;
import java.text.DecimalFormat;
import java.lang.ref.WeakReference;

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
	public static final String [] STAT_NAMES = { "muscle", "mysticality", "moxie" };

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
	public static final String CHOICE = "Choice";
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
	{	this( name, name.equals( SUBSTATS ) ? new int[3] : new int[1] );
	}

	/**
	 * Constructs a new <code>AdventureResult</code> with the given item ID.
	 * which increased/decreased by the given value.  This constructor
	 * should be used for item-related results.
	 *
	 * @param	itemID	The itemID of the result
	 * @param	count	How many of the noted result were gained
	 */

	public AdventureResult( int itemID, int count )
	{	this( TradeableItemDatabase.getItemName( itemID ), count, false );
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
	 * represent stat gains, but if there are any other results which
	 * should be represented this way, this constructor is also accessible.
	 *
	 * @param	name	The name of the result
	 * @param	count	How many of the noted result were gained
	 */

	public AdventureResult( String name, int [] count )
	{
		this( name, count, name == null ? ITEM_PRIORITY :
			name.equals(HP) ? HP_PRIORITY : name.equals(MP) ? MP_PRIORITY :
			name.equals(ADV) ? ADV_PRIORITY : name.equals(CHOICE) ? ADV_PRIORITY :
			name.equals(DRUNK) ? DRUNK_PRIORITY : name.equals(MEAT) ? MEAT_PRIORITY :
			name.equals(SUBSTATS) ? SUBSTAT_PRIORITY : name.equals(FULLSTATS) ? FULLSTAT_PRIORITY :
			name.equals(DIVIDER) ? DIVIDER_PRIORITY : StatusEffectDatabase.contains( name ) ? EFFECT_PRIORITY : ITEM_PRIORITY );
	}

	/**
	 * Constructs a new <code>AdventureResult</code> with the given name
	 * and given gains.  Note that this should only be used if you know
	 * whether or not this is an item or a status effect -- using
	 *
	 * @param	name	The name of the result
	 * @param	count	How many of the noted result were gained
	 * @param	isStatusEffect	<code>true</code> if this is a status effect, <code>false</code> if this is an item
	 */

	public AdventureResult( String name, int count, boolean isStatusEffect )
	{
		this( name, new int[1], isStatusEffect ? EFFECT_PRIORITY : ITEM_PRIORITY );
		this.count[0] = count;
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

		if ( priority == EFFECT_PRIORITY )
		{
			this.itemID = StatusEffectDatabase.getEffectID( this.name );
			if ( this.itemID > 0 )
				this.name = StatusEffectDatabase.getEffectName( this.itemID );
		}
		else if ( priority == ITEM_PRIORITY )
		{
			this.itemID = TradeableItemDatabase.getItemID( name, this.count[0] );
			if ( this.itemID > 0 )
				this.name = TradeableItemDatabase.getItemName( this.itemID );
		}
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
	 * Accessor method to retrieve the total value associated with the result
	 * stored at the given index of the count array.
	 *
	 * @return	The total value at the given index of the count array
	 */

	public int getCount( int index )
	{	return index < 0 || index >= count.length ? 0 : count[ index ];
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

	public static AdventureResult parseResult( String s )
	{
		if ( s.startsWith("You gain") || s.startsWith("You lose") )
		{
			// A stat has been modified - now you figure out which one it was,
			// how much it's been modified by, and return the appropriate value

			StringTokenizer parsedGain = new StringTokenizer( s, " ." );
			parsedGain.nextToken();

			int modifier = StaticEntity.parseInt( (parsedGain.nextToken().startsWith("gain") ? "" : "-") + parsedGain.nextToken() );
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

		return new AdventureResult( parsedItemName, StaticEntity.parseInt( parsedItemCount ) );
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
		if ( name == null )
			return "(Unrecognized result)";

		if ( name.equals(ADV) )
			return " Advs Used: " + COMMA_FORMAT.format(count[0]);

		if ( name.equals(MEAT) )
			return " Meat Gained: " + COMMA_FORMAT.format(count[0]);

		if ( name.equals(CHOICE) )
			return " Choices Left: " + COMMA_FORMAT.format(count[0]);

		if ( name.equals(HP) || name.equals(MP) || name.equals(DRUNK) )
			return " " + name + ": " + COMMA_FORMAT.format(count[0]);

		if ( name.equals(SUBSTATS) || name.equals(FULLSTATS) )
			return " " + name + ": " + COMMA_FORMAT.format(count[0]) + " / " + COMMA_FORMAT.format(count[1]) + " / " + COMMA_FORMAT.format(count[2]);

		if ( name.equals(DIVIDER) )
			return DIVIDER;

		if ( priority == ITEM_PRIORITY && count[0] == 1 )
			return name;

		return name + " (" + COMMA_FORMAT.format(count[0]) + ")";
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

		AdventureResult ar = (AdventureResult) o;
		if ( name == null || ar.name == null || count == null || ar.count == null )
			return false;

		return count.length == ar.count.length && (!ar.isItem() || (itemID == ar.itemID)) && name.equalsIgnoreCase( ar.name );
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
		if ( priorityDifference != 0 )
			return priorityDifference;

		if ( isStatusEffect() && getCount() != ar.getCount() )
			return getCount() - ar.getCount();

		int nameComparison = name.compareToIgnoreCase( ar.name );
		if ( nameComparison != 0 )
			return nameComparison;

		return isItem() ? itemID - ar.itemID : 0;
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

		// Compute the sum of the existing adventure result and the
		// current adventure result, and construct the sum.

		AdventureResult current = (AdventureResult) tally.get( index );

		int [] sumCount = new int[ current.count.length ];
		for ( int i = 0; i < sumCount.length; ++i )
			sumCount[i] = current.count[i] + result.count[i];

		AdventureResult sumResult = current.getInstance( sumCount );

		// Check to make sure that the result didn't transform the value
		// to zero - if it did, then remove the item from the list if
		// it's an item (non-items are exempt).

		if ( sumResult.getCount() == 0 )
		{
			if ( sumResult.isItem() || sumResult.isStatusEffect() )
			{
				tally.remove( index );
				return;
			}
		}
		else if ( sumResult.getCount() < 0 )
		{
			if ( sumResult.isStatusEffect() )
			{
				tally.remove( index );
				return;
			}
		}

		if ( sumResult.getName().equals( AdventureResult.ADV ) && sumResult.getCount() < 0 )
			sumResult = new AdventureResult( AdventureResult.ADV, 0 );

		tally.set( index, sumResult );
	}

	public static DefaultListCellRenderer getAutoSellCellRenderer()
	{	return getAutoSellCellRenderer( true, true, true, true, false );
	}

	public static DefaultListCellRenderer getAutoSellCellRenderer( boolean food, boolean booze, boolean other, boolean nosell, boolean notrade )
	{	return new AutoSellCellRenderer( food, booze, other, nosell, notrade );
	}

	private static class AutoSellCellRenderer extends DefaultListCellRenderer
	{
		private boolean food, booze, other, nosell, notrade;

		public AutoSellCellRenderer( boolean food, boolean booze, boolean other, boolean nosell, boolean notrade )
		{
			setOpaque( true );

			this.food = food;
			this.booze = booze;
			this.other = other;
			this.nosell = nosell;
			this.notrade = notrade;
		}

		public Component getListCellRendererComponent( JList list, Object value, int index, boolean isSelected, boolean cellHasFocus )
		{
			Component defaultComponent = super.getListCellRendererComponent( list, value, index, isSelected, cellHasFocus );

			if ( value == null || !(value instanceof AdventureResult) )
				return defaultComponent;

			AdventureResult ar = (AdventureResult) value;
			if ( !isVisibleWithFilter( ar, food, booze, other, notrade, nosell ) )
				return BLANK_LABEL;

			int autoSellValue = TradeableItemDatabase.getPriceByID( ar.getItemID() );

			StringBuffer stringForm = new StringBuffer();
			stringForm.append( ar.getName() );
			stringForm.append( " (" );

			if ( autoSellValue == -1 )
				stringForm.append( "no-autosell" );
			else if ( autoSellValue == 0 )
				stringForm.append( "no-trade" );
			else if ( autoSellValue < -1 )
				stringForm.append( COMMA_FORMAT.format( -autoSellValue ) + " meat, no-trade" );
			else
				stringForm.append( COMMA_FORMAT.format( autoSellValue ) + " meat" );

			stringForm.append( ")" );

			if ( ar.count[0] > 1 )
			{
				stringForm.append( " (" );
				stringForm.append( COMMA_FORMAT.format( ar.count[0] ) );
				stringForm.append( ")" );
			}

			((JLabel) defaultComponent).setText( stringForm.toString() );
			return defaultComponent;
		}
	}

	public static DefaultListCellRenderer getConsumableCellRenderer( boolean food, boolean booze, boolean other )
	{	return new ConsumableCellRenderer( food, booze, other );
	}

	private static class ConsumableCellRenderer extends DefaultListCellRenderer
	{
		private boolean food, booze, other;

		public ConsumableCellRenderer( boolean food, boolean booze, boolean other )
		{
			setOpaque( true );

			this.food = food;
			this.booze = booze;
			this.other = other;
		}

		public Component getListCellRendererComponent( JList list, Object value, int index, boolean isSelected, boolean cellHasFocus )
		{
			JLabel defaultComponent = (JLabel) super.getListCellRendererComponent( list, value, index, isSelected, cellHasFocus );

			return value == null ? defaultComponent : value instanceof AdventureResult || value instanceof ItemCreationRequest ?
				getRendererComponent( defaultComponent, value ) : defaultComponent;
		}

		public Component getRendererComponent( JLabel defaultComponent, Object value )
		{
			if ( !isVisibleWithFilter( value, food, booze, other, true, true ) )
				return BLANK_LABEL;

			String name = value instanceof AdventureResult ? ((AdventureResult)value).getName() : ((ItemCreationRequest)value).getName();
			int count = value instanceof AdventureResult ? ((AdventureResult)value).getCount() : ((ItemCreationRequest)value).getQuantityNeeded();

			String stringForm = name + " (" + COMMA_FORMAT.format( count ) + ")";
			defaultComponent.setText( stringForm );
			return defaultComponent;
		}
	}

	public static boolean isVisibleWithFilter( Object value, boolean food, boolean booze, boolean other, boolean nosell, boolean notrade )
	{
		boolean isVisibleWithFilter = true;
		String name = value instanceof AdventureResult ? ((AdventureResult)value).getName() : ((ItemCreationRequest)value).getName();
		int itemID = TradeableItemDatabase.getItemID( name );

		switch ( TradeableItemDatabase.getConsumptionType( itemID ) )
		{
			case ConsumeItemRequest.CONSUME_EAT:
				isVisibleWithFilter = food;
				break;

			case ConsumeItemRequest.CONSUME_DRINK:
				isVisibleWithFilter = booze;
				break;

			default:

				if ( value instanceof AdventureResult )
				{
					// Milk of magnesium is marked as food, as are
					// munchies pills; all others are marked as expected.

					isVisibleWithFilter = name.equals( "milk of magnesium" ) || name.equals( "munchies pills" ) ? food : other;
				}
				else
				{
					switch ( ConcoctionsDatabase.getMixingMethod( itemID ) )
					{
						case ItemCreationRequest.COOK:
						case ItemCreationRequest.COOK_REAGENT:
						case ItemCreationRequest.SUPER_REAGENT:
						case ItemCreationRequest.COOK_PASTA:
						case ItemCreationRequest.WOK:
							isVisibleWithFilter = food;
							break;

						case ItemCreationRequest.MIX:
						case ItemCreationRequest.MIX_SPECIAL:
						case ItemCreationRequest.STILL_BOOZE:
						case ItemCreationRequest.STILL_MIXER:
						case ItemCreationRequest.MIX_SUPER:
							isVisibleWithFilter = booze;
							break;

						default:
							isVisibleWithFilter = other;
							break;
					}
				}
		}

		int autoSellValue = TradeableItemDatabase.getPriceByID( itemID );

		if ( autoSellValue == -1 )
			isVisibleWithFilter &= nosell;
		else if ( autoSellValue == 0 )
			isVisibleWithFilter &= nosell && notrade;
		else if ( autoSellValue < -1 )
			isVisibleWithFilter &= notrade;

		return isVisibleWithFilter;
	}

	public static DefaultListCellRenderer getEquipmentCellRenderer( boolean weapon, boolean offhand, boolean hat, boolean shirt, boolean pants, boolean accessory, boolean familiar )
	{	return new EquipmentCellRenderer( weapon, offhand, hat, shirt, pants, accessory, familiar );
	}

	private static class EquipmentCellRenderer extends DefaultListCellRenderer
	{
		private boolean weapon, offhand, hat, shirt, pants, accessory, familiar;

		public EquipmentCellRenderer( boolean weapon, boolean offhand, boolean hat, boolean shirt, boolean pants, boolean accessory, boolean familiar )
		{
			setOpaque( true );

			this.weapon = weapon;
			this.offhand = offhand;
			this.hat = hat;
			this.shirt = shirt;
			this.pants = pants;
			this.accessory = accessory;
			this.familiar = familiar;
		}

		public Component getListCellRendererComponent( JList list, Object value, int index, boolean isSelected, boolean cellHasFocus )
		{
			if ( value == null || !(value instanceof AdventureResult) )
				return super.getListCellRendererComponent( list, value, index, isSelected, cellHasFocus );;

			AdventureResult ar = (AdventureResult) value;

			switch ( TradeableItemDatabase.getConsumptionType( ar.getName() ) )
			{
				case ConsumeItemRequest.EQUIP_WEAPON:
					if ( !weapon )
						return BLANK_LABEL;
					break;

				case ConsumeItemRequest.EQUIP_OFFHAND:
					if ( !offhand )
						return BLANK_LABEL;
					break;

				case ConsumeItemRequest.EQUIP_HAT:
					if ( !hat )
						return BLANK_LABEL;
					break;

				case ConsumeItemRequest.EQUIP_SHIRT:
					if ( !shirt )
						return BLANK_LABEL;
					break;

				case ConsumeItemRequest.EQUIP_PANTS:
					if ( !pants )
						return BLANK_LABEL;
					break;

				case ConsumeItemRequest.EQUIP_ACCESSORY:
					if ( !accessory )
						return BLANK_LABEL;
					break;

				case ConsumeItemRequest.EQUIP_FAMILIAR:
					if ( !familiar )
						return BLANK_LABEL;
					break;

				default:
					return BLANK_LABEL;
			}

			int power = EquipmentDatabase.getPower( ar.getName() );

			String stringForm = ar.getName() + " (+" + COMMA_FORMAT.format(power) + ")";

			JLabel defaultComponent = (JLabel) super.getListCellRendererComponent( list, value, index, isSelected, cellHasFocus );
			defaultComponent.setText( stringForm );
			return defaultComponent;
		}
	}

	public AdventureResult getNegation()
	{
		// Allow for negation of substats as well.

		if ( isItem() )
			return new AdventureResult( itemID, 0 - getCount() );

		else if ( isStatusEffect() )
			return new AdventureResult( name, 0 - getCount(), true );

		int [] newcount = new int[ count.length ];
		for ( int i = 0; i < count.length; ++i )
			newcount[i] = 0 - count[i];

		return new AdventureResult( name, newcount );
	}

	public AdventureResult getInstance( int quantity )
	{
		return isItem() ? new AdventureResult( name, quantity, false ) :
			isStatusEffect() ? new AdventureResult( name, quantity, true ) :
				new AdventureResult( name, quantity );
	}

	private AdventureResult getInstance( int [] count )
	{
		return isItem() ? new AdventureResult( name, count[0], false ) :
			isStatusEffect() ? new AdventureResult( name, count[0], true ) :
				new AdventureResult( name, count );
	}

	/**
	 * Special method which simplifies the constant use of indexOf and
	 * count retrieval.  This makes intent more transparent.
	 */

	public int getCount( List list )
	{
		int index = list.indexOf( this );
		return index == -1 ? 0 : ((AdventureResult)list.get( index )).getCount();
	}
}
