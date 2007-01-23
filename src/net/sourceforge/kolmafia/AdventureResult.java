/**
 * Copyright (c) 2005-2007, KoLmafia development team
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

package net.sourceforge.kolmafia;

import java.awt.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JLabel;
import javax.swing.JList;

public class AdventureResult implements Comparable, KoLConstants
{
	public static final String [] STAT_NAMES = { "muscle", "mysticality", "moxie" };

	private int itemId;
	private int [] count;
	private String name;
	private int priority;

	private static final int NO_PRIORITY = 0;
	private static final int ADV_PRIORITY = 1;
	private static final int MEAT_PRIORITY = 2;
	private static final int SUBSTAT_PRIORITY = 3;
	private static final int FULLSTAT_PRIORITY = 4;
	private static final int ITEM_PRIORITY = 5;
	private static final int EFFECT_PRIORITY = 6;

	public static final String HP = "HP";
	public static final String MP = "MP";
	public static final String ADV = "Adv";
	public static final String CHOICE = "Choice";
	public static final String DRUNK = "Drunk";
	public static final String MEAT = "Meat";
	public static final String SUBSTATS = "Substats";
	public static final String FULLSTATS = "Fullstats";

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
		this.name = name;
		this.count = name.equals( SUBSTATS ) || name.equals( FULLSTATS ) ? new int[3] : new int[1];

		this.priority = name.equals(ADV) ? ADV_PRIORITY : name.equals(MEAT) ? MEAT_PRIORITY :
			name.equals(HP) || name.equals(MP) || name.equals(DRUNK) ? NO_PRIORITY :
			name.equals(SUBSTATS) ? SUBSTAT_PRIORITY : name.equals(FULLSTATS) ? FULLSTAT_PRIORITY :
			StatusEffectDatabase.contains( name ) ? EFFECT_PRIORITY : ITEM_PRIORITY;

		if ( this.priority == EFFECT_PRIORITY )
			normalizeEffectName();
		else if ( this.priority == ITEM_PRIORITY )
			normalizeItemName();

	}

	/**
	 * Constructs a new <code>AdventureResult</code> with the given item Id.
	 * which increased/decreased by the given value.  This constructor
	 * should be used for item-related results.
	 *
	 * @param	itemId	The itemId of the result
	 * @param	count	How many of the noted result were gained
	 */

	public AdventureResult( int itemId, int count )
	{
		this.name = TradeableItemDatabase.getItemName( itemId );
		this.count = new int[] { count };
		normalizeItemName();
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
		this.name = name;
		this.count = new int[] { count };

		if ( name.equals(ADV) || name.equals(CHOICE) )
			this.priority = ADV_PRIORITY;
		else if ( name.equals(MEAT) )
			this.priority = MEAT_PRIORITY;
		else if ( name.equals(HP) || name.equals(MP) || name.equals(DRUNK) )
			this.priority = NO_PRIORITY;
		else if ( StatusEffectDatabase.contains( name ) )
			normalizeEffectName();
		else
			normalizeItemName();
	}

	/**
	 * Constructs a new <code>AdventureResult</code> with the given name
	 * and increase in stat gains.
	 */

	public AdventureResult( int [] count )
	{
		this.name = SUBSTATS;
		this.count = count;
		this.itemId = -1;
		this.priority = SUBSTAT_PRIORITY;
	}

	/**
	 * Constructs a new <code>AdventureResult</code> with the given name
	 * and given gains.  Note that this should only be used if you know
	 * whether or not this is an item or a status effect.
	 *
	 * @param	name	The name of the result
	 * @param	count	How many of the noted result were gained
	 * @param	isStatusEffect	<code>true</code> if this is a status effect, <code>false</code> if this is an item
	 */

	public AdventureResult( String name, int count, boolean isStatusEffect )
	{
		this.name = name;
		this.count = new int[] { count };

		if ( isStatusEffect )
			normalizeEffectName();
		else
			normalizeItemName();
	}

	public void normalizeEffectName()
	{
		this.priority = EFFECT_PRIORITY;

		int effectId = StatusEffectDatabase.getEffectId( this.name );
		if ( effectId > 0 )
			this.name = StatusEffectDatabase.getEffectName( effectId );
	}

	public void normalizeItemName()
	{
		if ( name.equals( "(none)" ) || name.equals( "-select an item-" ) )
			return;

		this.priority = ITEM_PRIORITY;
		this.itemId = TradeableItemDatabase.getItemId( name, this.count[0] );

		if ( this.itemId > 0 )
		{
			this.name = TradeableItemDatabase.getItemName( this.itemId );
		}
		else if ( StaticEntity.getClient() != null )
		{
			KoLmafia.updateDisplay( "Unknown item found: " + name );
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
	 * Accessor method to retrieve the item Id associated with the result,
	 * if this is an item and the item Id is known.
	 *
	 * @return	The item Id associated with this item
	 */

	public int getItemId()
	{	return itemId;
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

				return new AdventureResult( gained );
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

		if ( priority == ITEM_PRIORITY && count[0] == 1 )
			return name;

		return name + " (" + COMMA_FORMAT.format(count[0]) + ")";
	}

	public String toConditionString()
	{
		if ( name == null )
			return "";

		if ( name.equals(ADV) || name.equals(CHOICE) )
			return count[0] + " choiceadv";

		if ( name.equals(MEAT) )
			return count[0] + " meat";

		if ( name.equals(HP) )
			return count[0] + " health";

		if ( name.equals(MP) )
			return count[0] + " mana";

		if ( name.equals(SUBSTATS) )
		{
			StringBuffer stats = new StringBuffer();

			if ( count[0] > 0 )
				stats.append( KoLCharacter.calculateBasePoints( KoLCharacter.getTotalMuscle() + count[0] ) + " muscle" );

			if ( count[1] > 0 )
			{
				if ( count[0] > 0 )
					stats.append( ", " );

				stats.append( KoLCharacter.calculateBasePoints( KoLCharacter.getTotalMysticality() + count[1] ) + " mysticality" );
			}

			if ( count[2] > 0 )
			{
				if ( count[0] > 0 || count[1] > 0 )
					stats.append( ", " );

				stats.append( KoLCharacter.calculateBasePoints( KoLCharacter.getTotalMysticality() + count[1] ) + " moxie" );
			}

			return stats.toString();
		}

		return count[0] + " " + name;
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

		return count.length == ar.count.length && (!ar.isItem() || (itemId == ar.itemId)) && name.equalsIgnoreCase( ar.name );
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
		if ( name.equalsIgnoreCase( ar.name ) )
			return 0;

		int priorityDifference = priority - ar.priority;
		if ( priorityDifference != 0 )
			return priorityDifference;

		if ( isStatusEffect() )
			return getCount() - ar.getCount();

		int nameComparison = name.compareToIgnoreCase( ar.name );
		if ( nameComparison != 0 )
			return nameComparison;

		return isItem() ? itemId - ar.itemId : 0;
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
		AdventureResult sumResult;

		if ( current.count.length == 1 )
			sumResult = current.getInstance( 0 );
		else
			sumResult = current.getInstance( new int[ current.count.length ] );

		for ( int i = 0; i < current.count.length; ++i )
			sumResult.count[i] = current.count[i] + result.count[i];

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

		tally.set( index, sumResult );
	}

	public static DefaultListCellRenderer getDefaultRenderer()
	{	return new AdventureResultRenderer();
	}

	private static class AdventureResultRenderer extends DefaultListCellRenderer
	{
		public AdventureResultRenderer()
		{
			setOpaque( true );
		}

		public Component getListCellRendererComponent( JList list, Object value, int index, boolean isSelected, boolean cellHasFocus )
		{
			Component defaultComponent = super.getListCellRendererComponent( list, value, index, isSelected, cellHasFocus );

			if ( value == null )
				return defaultComponent;

			if ( value instanceof AdventureResult )
				return getRenderer( defaultComponent, (AdventureResult) value );

			if ( value instanceof ItemCreationRequest )
				return getRenderer( defaultComponent, (ItemCreationRequest) value );

			return defaultComponent;
		}

		public Component getRenderer( Component defaultComponent, AdventureResult ar )
		{
			if ( !ar.isItem() )
				return defaultComponent;

			int autoSellValue = TradeableItemDatabase.getPriceById( ar.getItemId() );

			StringBuffer stringForm = new StringBuffer();
			stringForm.append( ar.getName() );
			stringForm.append( " (" );

			if ( autoSellValue == 0 )
				stringForm.append( "no-sell" );
			else
				stringForm.append( COMMA_FORMAT.format( autoSellValue ) + " meat" );

			stringForm.append( ")" );

			if ( ar.count[0] != 1 )
			{
				stringForm.append( " (" );
				stringForm.append( COMMA_FORMAT.format( ar.count[0] ) );
				stringForm.append( ")" );
			}

			if ( junkItemList.contains( ar ) )
			{
				stringForm.insert( 0, "<html><font color=gray>" );
				stringForm.append( "</font></html>" );
			}

			((JLabel) defaultComponent).setText( stringForm.toString() );
			return defaultComponent;
		}

		public Component getRenderer( Component defaultComponent, ItemCreationRequest icr )
		{
			StringBuffer stringForm = new StringBuffer();
			stringForm.append( icr.getName() );

			if ( icr.getQuantityPossible() > 1 )
			{
				stringForm.append( " (" );
				stringForm.append( COMMA_FORMAT.format( icr.getQuantityPossible() ) );
				stringForm.append( ")" );
			}

			if ( junkItemList.contains( icr.createdItem ) )
			{
				stringForm.insert( 0, "<html><font color=gray>" );
				stringForm.append( "</font></html>" );
			}

			((JLabel) defaultComponent).setText( stringForm.toString() );
			return defaultComponent;
		}
	}

	public static DefaultListCellRenderer getEquipmentRenderer()
	{	return new EquipmentRenderer();
	}

	private static class EquipmentRenderer extends DefaultListCellRenderer
	{
		public EquipmentRenderer()
		{	setOpaque( true );
		}

		public Component getListCellRendererComponent( JList list, Object value, int index, boolean isSelected, boolean cellHasFocus )
		{
			if ( value == null || !(value instanceof AdventureResult) )
				return super.getListCellRendererComponent( list, value, index, isSelected, cellHasFocus );;

			AdventureResult ar = (AdventureResult) value;
			int equipmentType = TradeableItemDatabase.getConsumptionType( ar.getName() );

			int power = EquipmentDatabase.getPower( ar.getName() );
			String stringForm = null;

			if ( equipmentType == ConsumeItemRequest.EQUIP_FAMILIAR || ar.equals( EquipmentRequest.UNEQUIP ) )
			{
				if ( ar.equals( EquipmentRequest.UNEQUIP ) )
					stringForm = ar.getName();
				else if ( KoLCharacter.getFamiliar() != null && KoLCharacter.getFamiliar().canEquip( ar ) )
					stringForm = ar.getName();
				else
					stringForm = "<html><font color=gray>" + ar.getName() + "</font></html>";
			}
			else
			{
				if ( equipmentType == ConsumeItemRequest.EQUIP_ACCESSORY )
				{
					int count = ar.getCount( inventory );
					if ( ar.equals( KoLCharacter.getEquipment( KoLCharacter.ACCESSORY1 ) ) )
						++count;
					if ( ar.equals( KoLCharacter.getEquipment( KoLCharacter.ACCESSORY2 ) ) )
						++count;
					if ( ar.equals( KoLCharacter.getEquipment( KoLCharacter.ACCESSORY3 ) ) )
						++count;
					stringForm = ar.getName() + " (" + count + " max)";
				}
				else
				{
					stringForm = ar.getName() + " (+" + COMMA_FORMAT.format(power) + ")";
				}

				// Gray out any equipment that the player cannot currently equip
				// inside of an equipment filter.

				if ( !EquipmentDatabase.canEquip( ar.getName() ) )
					stringForm = "<html><font color=gray>" + stringForm + "</font></html>";
			}

			JLabel defaultComponent = (JLabel) super.getListCellRendererComponent( list, value, index, isSelected, cellHasFocus );
			defaultComponent.setText( stringForm );
			return defaultComponent;
		}
	}

	public AdventureResult getNegation()
	{
		// Allow for negation of substats as well.

		if ( isItem() )
			return new AdventureResult( itemId, 0 - getCount() );

		else if ( isStatusEffect() )
			return new AdventureResult( name, 0 - getCount(), true );

		int [] newcount = new int[ count.length ];
		for ( int i = 0; i < count.length; ++i )
			newcount[i] = 0 - count[i];

		return getInstance( newcount );
	}

	public AdventureResult getInstance( int quantity )
	{
		return isItem() ? new AdventureResult( name, quantity, false ) :
			isStatusEffect() ? new AdventureResult( name, quantity, true ) :
				new AdventureResult( name, quantity );
	}

	public AdventureResult getInstance( int [] quantity )
	{
		if ( this.priority != SUBSTAT_PRIORITY && this.priority != FULLSTAT_PRIORITY )
			return getInstance( quantity[0] );

		if ( this.priority == SUBSTAT_PRIORITY )
			return new AdventureResult( quantity );

		AdventureResult stats = new AdventureResult( FULLSTATS );
		stats.count = quantity;
		return stats;
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
