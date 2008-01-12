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
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JLabel;
import javax.swing.JList;

import net.sourceforge.kolmafia.ConcoctionsDatabase.Concoction;

public class AdventureResult
	implements Comparable, KoLConstants
{
	public static final int[] SESSION_SUBSTATS = new int[ 3 ];
	public static final int[] SESSION_FULLSTATS = new int[ 3 ];
	public static final int[] CONDITION_SUBSTATS = new int[ 3 ];

	public static final String[] STAT_NAMES = { "muscle", "mysticality", "moxie" };

	private int itemId;
	private int[] count;
	protected String name;
	private int priority;

	private static final int NO_PRIORITY = 0;
	private static final int ADV_PRIORITY = 1;
	private static final int MEAT_PRIORITY = 2;
	private static final int SUBSTAT_PRIORITY = 3;
	private static final int FULLSTAT_PRIORITY = 4;
	private static final int ITEM_PRIORITY = 5;
	private static final int EFFECT_PRIORITY = 6;

	protected static final int MONSTER_PRIORITY = -1;

	public static final String HP = "HP";
	public static final String MP = "MP";
	public static final String ADV = "Adv";
	public static final String CHOICE = "Choice";
	public static final String DRUNK = "Drunk";
	public static final String MEAT = "Meat";
	public static final String SUBSTATS = "Substats";
	public static final String FULLSTATS = "Fullstats";

	private static final List MUS_SUBSTAT = new ArrayList();
	private static final List MYS_SUBSTAT = new ArrayList();
	private static final List MOX_SUBSTAT = new ArrayList();

	static
	{
		AdventureResult.MUS_SUBSTAT.add( "Beefiness" );
		AdventureResult.MUS_SUBSTAT.add( "Fortitude" );
		AdventureResult.MUS_SUBSTAT.add( "Muscleboundness" );
		AdventureResult.MUS_SUBSTAT.add( "Strengthliness" );
		AdventureResult.MUS_SUBSTAT.add( "Strongness" );
		// The following only under Can Has Cyborger
		AdventureResult.MUS_SUBSTAT.add( "muskewlairtees" );

		AdventureResult.MYS_SUBSTAT.add( "Enchantedness" );
		AdventureResult.MYS_SUBSTAT.add( "Magicalness" );
		AdventureResult.MYS_SUBSTAT.add( "Mysteriousness" );
		AdventureResult.MYS_SUBSTAT.add( "Wizardliness" );
		// The following only under Can Has Cyborger
		AdventureResult.MYS_SUBSTAT.add( "mistikkaltees" );

		AdventureResult.MOX_SUBSTAT.add( "Cheek" );
		AdventureResult.MOX_SUBSTAT.add( "Chutzpah" );
		AdventureResult.MOX_SUBSTAT.add( "Roguishness" );
		AdventureResult.MOX_SUBSTAT.add( "Sarcasm" );
		AdventureResult.MOX_SUBSTAT.add( "Smarm" );
		// The following only under Can Has Cyborger
		AdventureResult.MOX_SUBSTAT.add( "mawksees" );
	}

	public static final AdventureResult SESSION_SUBSTATS_RESULT =
		new AdventureResult( AdventureResult.SUBSTATS, AdventureResult.SESSION_SUBSTATS );
	public static final AdventureResult SESSION_FULLSTATS_RESULT =
		new AdventureResult( AdventureResult.FULLSTATS, AdventureResult.SESSION_FULLSTATS );
	public static final AdventureResult CONDITION_SUBSTATS_RESULT =
		new AdventureResult( AdventureResult.SUBSTATS, AdventureResult.CONDITION_SUBSTATS );

	/**
	 * Constructs a new <code>AdventureResult</code> with the given name. The amount of gain will default to zero.
	 * 
	 * @param name The name of the result
	 */

	public AdventureResult( final String name )
	{
		this.name = name;
		this.count = new int[ 1 ];

		this.priority =
			name.equals( AdventureResult.ADV ) ? AdventureResult.ADV_PRIORITY : name.equals( AdventureResult.MEAT ) ? AdventureResult.MEAT_PRIORITY : name.equals( AdventureResult.HP ) || name.equals( AdventureResult.MP ) || name.equals( AdventureResult.DRUNK ) ? AdventureResult.NO_PRIORITY : name.equals( AdventureResult.SUBSTATS ) ? AdventureResult.SUBSTAT_PRIORITY : name.equals( AdventureResult.FULLSTATS ) ? AdventureResult.FULLSTAT_PRIORITY : StatusEffectDatabase.contains( name ) ? AdventureResult.EFFECT_PRIORITY : AdventureResult.ITEM_PRIORITY;

		if ( this.priority == AdventureResult.EFFECT_PRIORITY )
		{
			this.normalizeEffectName();
		}
		else if ( this.priority == AdventureResult.ITEM_PRIORITY )
		{
			this.normalizeItemName();
		}

	}

	protected AdventureResult( final int subType, final String name )
	{
		this.name = name;
		this.count = new int[ 1 ];
		this.count[ 0 ] = 1;

		this.priority = subType;

		if ( this.priority == AdventureResult.EFFECT_PRIORITY )
		{
			this.normalizeEffectName();
		}
		else if ( this.priority == AdventureResult.ITEM_PRIORITY )
		{
			this.normalizeItemName();
		}

	}

	/**
	 * Constructs a new <code>AdventureResult</code> with the given item Id. which increased/decreased by the given
	 * value. This constructor should be used for item-related results.
	 * 
	 * @param itemId The itemId of the result
	 * @param count How many of the noted result were gained
	 */

	public AdventureResult( final int itemId, final int count )
	{
		this.name = TradeableItemDatabase.getItemName( itemId );
		this.count = new int[] { count };
		this.normalizeItemName();
	}

	/**
	 * Constructs a new <code>AdventureResult</code> with the given name which increased/decreased by the given value.
	 * This constructor should be used for most results.
	 * 
	 * @param name The name of the result
	 * @param count How many of the noted result were gained
	 */

	public AdventureResult( final String name, final int count )
	{
		this.name = name;
		this.count = new int[] { count };

		if ( name.equals( AdventureResult.ADV ) || name.equals( AdventureResult.CHOICE ) )
		{
			this.priority = AdventureResult.ADV_PRIORITY;
		}
		else if ( name.equals( AdventureResult.MEAT ) )
		{
			this.priority = AdventureResult.MEAT_PRIORITY;
		}
		else if ( name.equals( AdventureResult.HP ) || name.equals( AdventureResult.MP ) || name.equals( AdventureResult.DRUNK ) )
		{
			this.priority = AdventureResult.NO_PRIORITY;
		}
		else if ( StatusEffectDatabase.contains( name ) )
		{
			this.normalizeEffectName();
		}
		else
		{
			this.normalizeItemName();
		}
	}

	/**
	 * Constructs a new <code>AdventureResult</code> with the given name and increase in stat gains.
	 */

	public AdventureResult( final String name, final int[] count )
	{
		this.name = name;
		this.count = count;

		this.priority =
			name.equals( AdventureResult.ADV ) ? AdventureResult.ADV_PRIORITY : name.equals( AdventureResult.MEAT ) ? AdventureResult.MEAT_PRIORITY : name.equals( AdventureResult.HP ) || name.equals( AdventureResult.MP ) || name.equals( AdventureResult.DRUNK ) ? AdventureResult.NO_PRIORITY : name.equals( AdventureResult.SUBSTATS ) ? AdventureResult.SUBSTAT_PRIORITY : name.equals( AdventureResult.FULLSTATS ) ? AdventureResult.FULLSTAT_PRIORITY : StatusEffectDatabase.contains( name ) ? AdventureResult.EFFECT_PRIORITY : AdventureResult.ITEM_PRIORITY;

		if ( this.priority == AdventureResult.EFFECT_PRIORITY )
		{
			this.normalizeEffectName();
		}
		else if ( this.priority == AdventureResult.ITEM_PRIORITY )
		{
			this.normalizeItemName();
		}
	}

	/**
	 * Constructs a new <code>AdventureResult</code> with the given name and given gains. Note that this should only
	 * be used if you know whether or not this is an item or a status effect.
	 * 
	 * @param name The name of the result
	 * @param count How many of the noted result were gained
	 * @param isStatusEffect <code>true</code> if this is a status effect, <code>false</code> if this is an item
	 */

	public AdventureResult( final String name, final int count, final boolean isStatusEffect )
	{
		this.name = name;
		this.count = new int[] { count };

		if ( isStatusEffect )
		{
			this.normalizeEffectName();
		}
		else
		{
			this.normalizeItemName();
		}
	}

	public void normalizeEffectName()
	{
		this.priority = AdventureResult.EFFECT_PRIORITY;

		int effectId = StatusEffectDatabase.getEffectId( this.name );
		if ( effectId > 0 )
		{
			this.name = StatusEffectDatabase.getEffectName( effectId );
		}
	}

	public void normalizeItemName()
	{
		this.priority = AdventureResult.ITEM_PRIORITY;
		if ( this.name.equals( "(none)" ) || this.name.equals( "-select an item-" ) )
		{
			return;
		}

		this.itemId = TradeableItemDatabase.getItemId( this.name, this.count[ 0 ] );

		if ( this.itemId > 0 )
		{
			this.name = TradeableItemDatabase.getItemName( this.itemId );
		}
		else if ( StaticEntity.getClient() != null )
		{
			RequestLogger.printLine( "Unknown item found: " + this.name );
		}
	}

	public static final int itemId( final String name )
	{
		int id = -1;

		id = AdventureResult.bangPotionId( name );
		if ( id != -1 )
		{
			return id;
		}

		id = AdventureResult.stoneSphereId( name );
		if ( id != -1 )
		{
			return id;
		}

		return TradeableItemDatabase.getItemId( name, 1 );
	}

	public static final AdventureResult bangPotion( final String name )
	{
		// Given "potion of confusion", look it up
		if ( !name.startsWith( "potion of " ) )
		{
			return null;
		}

		int id = AdventureResult.bangPotionId( name );
		if ( id > 0 )
		{
			return new AdventureResult( id, 1 );
		}

		// We don't know which potion makes this effect.
		// Make a pseudo-item with the required name

		AdventureResult item = new AdventureResult( "(none)", 1, false );
		item.name = name;
		return item;
	}

	private static int bangPotionId( final String name )
	{
		int index = name.indexOf( "potion of " );
		if ( index == -1 )
		{
			return -1;
		}

		// Get the effect name;
		String effect = name.substring( index + 10 );

		// Make sure we have potion properties
		KoLCharacter.ensureUpdatedPotionEffects();

		// Look up the effect name
		for ( int i = 819; i <= 827; ++i )
		{
			if ( effect.equals( KoLSettings.getUserProperty( "lastBangPotion" + i ) ) )
			{
				return i;
			}
		}

		return -1;
	}

	private static int stoneSphereId( final String name )
	{
		int index = name.indexOf( "sphere of " );
		if ( index == -1 )
		{
			return -1;
		}

		// Get the effect name;
		String effect = name.substring( index + 10 );

		// Make sure we have sphere properties
		KoLCharacter.ensureUpdatedSphereEffects();

		// Look up the effect name
		for ( int i = 2174; i <= 2177; ++i )
		{
			if ( effect.equals( KoLSettings.getUserProperty( "lastStoneSphere" + i ) ) )
			{
				return i;
			}
		}

		return -1;
	}

	/**
	 * Accessor method to determine if this result is a status effect.
	 * 
	 * @return <code>true</code> if this result represents a status effect
	 */

	public boolean isStatusEffect()
	{
		return this.priority == AdventureResult.EFFECT_PRIORITY;
	}

	/**
	 * Accessor method to determine if this result is a muscle gain.
	 * 
	 * @return <code>true</code> if this result represents muscle subpoint gain
	 */

	public boolean isMuscleGain()
	{
		return this.priority == AdventureResult.SUBSTAT_PRIORITY && this.count[ 0 ] != 0;
	}

	/**
	 * Accessor method to determine if this result is a mysticality gain.
	 * 
	 * @return <code>true</code> if this result represents mysticality subpoint gain
	 */

	public boolean isMysticalityGain()
	{
		return this.priority == AdventureResult.SUBSTAT_PRIORITY && this.count[ 1 ] != 0;
	}

	/**
	 * Accessor method to determine if this result is a muscle gain.
	 * 
	 * @return <code>true</code> if this result represents muscle subpoint gain
	 */

	public boolean isMoxieGain()
	{
		return this.priority == AdventureResult.SUBSTAT_PRIORITY && this.count[ 2 ] != 0;
	}

	/**
	 * Accessor method to determine if this result is an item, as opposed to meat, drunkenness, adventure or substat
	 * gains.
	 * 
	 * @return <code>true</code> if this result represents an item
	 */

	public boolean isItem()
	{
		return this.priority == AdventureResult.ITEM_PRIORITY;
	}

	/**
	 * Accessor method to retrieve the name associated with the result.
	 * 
	 * @return The name of the result
	 */

	public String getName()
	{
		switch ( this.itemId )
		{
		case ConsumeItemRequest.MILKY_POTION:
		case ConsumeItemRequest.SWIRLY_POTION:
		case ConsumeItemRequest.BUBBLY_POTION:
		case ConsumeItemRequest.SMOKY_POTION:
		case ConsumeItemRequest.CLOUDY_POTION:
		case ConsumeItemRequest.EFFERVESCENT_POTION:
		case ConsumeItemRequest.FIZZY_POTION:
		case ConsumeItemRequest.DARK_POTION:
		case ConsumeItemRequest.MURKY_POTION:

			return ConsumeItemRequest.bangPotionName( this.itemId, this.name );

		case FightRequest.MOSSY_STONE_SPHERE:
		case FightRequest.SMOOTH_STONE_SPHERE:
		case FightRequest.CRACKED_STONE_SPHERE:
		case FightRequest.ROUGH_STONE_SPHERE:

			return FightRequest.stoneSphereName( this.itemId, this.name );

		default:
			return this.name;
		}
	}

	/**
	 * Accessor method to retrieve the item Id associated with the result, if this is an item and the item Id is known.
	 * 
	 * @return The item Id associated with this item
	 */

	public int getItemId()
	{
		return this.itemId;
	}

	/**
	 * Accessor method to retrieve the total value associated with the result. In the event of substat points, this
	 * returns the total subpoints within the <code>AdventureResult</code>; in the event of an item or meat gains,
	 * this will return the total number of meat/items in this result.
	 * 
	 * @return The amount associated with this result
	 */

	public int getCount()
	{
		int totalCount = 0;
		for ( int i = 0; i < this.count.length; ++i )
		{
			totalCount += this.count[ i ];
		}
		return totalCount;
	}

	/**
	 * Accessor method to retrieve the total value associated with the result stored at the given index of the count
	 * array.
	 * 
	 * @return The total value at the given index of the count array
	 */

	public int getCount( final int index )
	{
		return index < 0 || index >= this.count.length ? 0 : this.count[ index ];
	}

	/**
	 * A static final method which parses the given string for any content which might be applicable to an
	 * <code>AdventureResult</code>, and returns the resulting <code>AdventureResult</code>.
	 * 
	 * @param s The string suspected of being an <code>AdventureResult</code>
	 * @return An <code>AdventureResult</code> with the appropriate data
	 * @throws NumberFormatException The string was not a recognized <code>AdventureResult</code>
	 * @throws ParseException The value enclosed within parentheses was not a number.
	 */

	public static final AdventureResult parseResult( final String s )
	{
		if ( s.startsWith( "You gain" ) || s.startsWith( "You lose" ) )
		{
			// A stat has been modified - now you figure out which one it was,
			// how much it's been modified by, and return the appropriate value

			StringTokenizer parsedGain = new StringTokenizer( s, " ." );
			parsedGain.nextToken();

			int modifier =
				StaticEntity.parseInt( ( parsedGain.nextToken().startsWith( "gain" ) ? "" : "-" ) + parsedGain.nextToken() );
			String statname = parsedGain.nextToken();

			// Stats actually fall into one of four categories - simply pick the
			// correct one and return the result.

			if ( parsedGain.hasMoreTokens() )
			{
				char identifier = statname.charAt( 0 );
				return new AdventureResult(
					identifier == 'H' || identifier == 'h' ? AdventureResult.HP : AdventureResult.MP, modifier );
			}

			if ( statname.startsWith( "Adv" ) )
			{
				return new AdventureResult( AdventureResult.ADV, modifier );
			}

			if ( statname.startsWith( "Dru" ) )
			{
				return new AdventureResult( AdventureResult.DRUNK, modifier );
			}

			if ( statname.startsWith( "Me" ) )
			{
				// "Meat" or "Meets", if Can Has Cyborger
				return new AdventureResult( AdventureResult.MEAT, modifier );
			}

			// In the current implementations, all stats gains are
			// located inside of a generic adventure which
			// indicates how much of each substat is gained.

			int[] gained =
				{ AdventureResult.MUS_SUBSTAT.contains( statname ) ? modifier : 0, AdventureResult.MYS_SUBSTAT.contains( statname ) ? modifier : 0, AdventureResult.MOX_SUBSTAT.contains( statname ) ? modifier : 0 };

			return new AdventureResult( AdventureResult.SUBSTATS, gained );
		}

		StringTokenizer parsedItem = new StringTokenizer( s, "()" );
		String parsedItemName = parsedItem.nextToken().trim();
		String parsedItemCount = parsedItem.hasMoreTokens() ? parsedItem.nextToken() : "1";

		return new AdventureResult( parsedItemName, StaticEntity.parseInt( parsedItemCount ) );
	}

	/**
	 * Converts the <code>AdventureResult</code> to a <code>String</code>. This is especially useful in debug, or
	 * if the <code>AdventureResult</code> is to be displayed in a <code>ListModel</code>.
	 * 
	 * @return The string version of this <code>AdventureResult</code>
	 */

	public String toString()
	{
		if ( this.name == null )
		{
			return "(Unrecognized result)";
		}

		if ( this.name.equals( AdventureResult.ADV ) )
		{
			return " Advs Used: " + KoLConstants.COMMA_FORMAT.format( this.count[ 0 ] );
		}

		if ( this.name.equals( AdventureResult.MEAT ) )
		{
			return " Meat Gained: " + KoLConstants.COMMA_FORMAT.format( this.count[ 0 ] );
		}

		if ( this.name.equals( AdventureResult.CHOICE ) )
		{
			return " Choices Left: " + KoLConstants.COMMA_FORMAT.format( this.count[ 0 ] );
		}

		if ( this.name.equals( AdventureResult.HP ) || this.name.equals( AdventureResult.MP ) || this.name.equals( AdventureResult.DRUNK ) )
		{
			return " " + this.name + ": " + KoLConstants.COMMA_FORMAT.format( this.count[ 0 ] );
		}

		if ( this.name.equals( AdventureResult.SUBSTATS ) || this.name.equals( AdventureResult.FULLSTATS ) )
		{
			return " " + this.name + ": " + KoLConstants.COMMA_FORMAT.format( this.count[ 0 ] ) + " / " + KoLConstants.COMMA_FORMAT.format( this.count[ 1 ] ) + " / " + KoLConstants.COMMA_FORMAT.format( this.count[ 2 ] );
		}

		if ( this.priority == AdventureResult.MONSTER_PRIORITY )
		{
			return this.name;
		}

		return this.count[ 0 ] == 1 ? this.getName() : this.getName() + " (" + KoLConstants.COMMA_FORMAT.format( this.count[ 0 ] ) + ")";
	}

	public String toConditionString()
	{
		if ( this.name == null )
		{
			return "";
		}

		if ( this.name.equals( AdventureResult.ADV ) || this.name.equals( AdventureResult.CHOICE ) )
		{
			return this.count[ 0 ] + " choiceadv";
		}

		if ( this.name.equals( AdventureResult.MEAT ) )
		{
			return this.count[ 0 ] + " meat";
		}

		if ( this.name.equals( AdventureResult.HP ) )
		{
			return this.count[ 0 ] + " health";
		}

		if ( this.name.equals( AdventureResult.MP ) )
		{
			return this.count[ 0 ] + " mana";
		}

		if ( this.name.equals( AdventureResult.SUBSTATS ) )
		{
			StringBuffer stats = new StringBuffer();

			if ( this.count[ 0 ] > 0 )
			{
				stats.append( KoLCharacter.calculateBasePoints( KoLCharacter.getTotalMuscle() + this.count[ 0 ] ) + " muscle" );
			}

			if ( this.count[ 1 ] > 0 )
			{
				if ( this.count[ 0 ] > 0 )
				{
					stats.append( ", " );
				}

				stats.append( KoLCharacter.calculateBasePoints( KoLCharacter.getTotalMysticality() + this.count[ 1 ] ) + " mysticality" );
			}

			if ( this.count[ 2 ] > 0 )
			{
				if ( this.count[ 0 ] > 0 || this.count[ 1 ] > 0 )
				{
					stats.append( ", " );
				}

				stats.append( KoLCharacter.calculateBasePoints( KoLCharacter.getTotalMoxie() + this.count[ 2 ] ) + " moxie" );
			}

			return stats.toString();
		}

		return this.count[ 0 ] + " " + this.name;
	}

	/**
	 * Compares the <code>AdventureResult</code> with the given object for name equality. Note that this will still
	 * return <code>true</code> if the values do not match; this merely matches on names.
	 * 
	 * @param o The <code>Object</code> to be compared with this <code>AdventureResult</code>
	 * @return <code>true</code> if the <code>Object</code> is an <code>AdventureResult</code> and has the same
	 *         name as this one
	 */

	public boolean equals( final Object o )
	{
		if ( !( o instanceof AdventureResult ) || o == null )
		{
			return false;
		}

		AdventureResult ar = (AdventureResult) o;
		if ( this.name == null || ar.name == null || this.count == null || ar.count == null )
		{
			return false;
		}

		return this.count.length == ar.count.length && ( !ar.isItem() || this.itemId == ar.itemId ) && this.name.equalsIgnoreCase( ar.name );
	}

	/**
	 * Compares the <code>AdventureResult</code> with the given object for name equality and priority differences.
	 * Return values are consistent with the rules laid out in {@link java.lang.Comparable#compareTo(Object)}.
	 */

	public int compareTo( final Object o )
	{
		if ( !( o instanceof AdventureResult ) || o == null )
		{
			return -1;
		}

		AdventureResult ar = (AdventureResult) o;
		if ( this.name.equalsIgnoreCase( ar.name ) )
		{
			return 0;
		}

		int priorityDifference = this.priority - ar.priority;
		if ( priorityDifference != 0 )
		{
			return priorityDifference;
		}

		if ( this.isStatusEffect() )
		{
			return this.getCount() - ar.getCount();
		}

		int nameComparison = this.name.compareToIgnoreCase( ar.name );
		if ( nameComparison != 0 )
		{
			return nameComparison;
		}

		return this.isItem() ? this.itemId - ar.itemId : 0;
	}

	/**
	 * Utility method used for adding a given <code>AdventureResult</code> to a tally of <code>AdventureResult</code>s.
	 * 
	 * @param tally The tally accumulating <code>AdventureResult</code>s
	 * @param result The result to add to the tally
	 */

	public static final void addResultToList( final List sourceList, final AdventureResult result )
	{
		int index = sourceList.indexOf( result );

		// First, filter out things where it's a simple addition of an
		// item, or something which may not result in a change in the
		// state of the sourceList list.

		if ( index == -1 )
		{
			if ( !result.isItem() || result.getCount() != 0 )
			{
				sourceList.add( result );
			}
			return;
		}

		// These don't involve any addition -- ignore this entirely
		// for now.

		if ( result == AdventureResult.SESSION_SUBSTATS_RESULT || result == AdventureResult.SESSION_FULLSTATS_RESULT || result == AdventureResult.CONDITION_SUBSTATS_RESULT )
		{
			return;
		}

		// Compute the sum of the existing adventure result and the
		// current adventure result, and construct the sum.

		AdventureResult current = (AdventureResult) sourceList.get( index );
		AdventureResult sumResult;

		if ( current.count.length == 1 )
		{
			sumResult = current.getInstance( current.count[ 0 ] + result.count[ 0 ] );
		}
		else
		{
			sumResult = current.getInstance( new int[ current.count.length ] );
			for ( int i = 0; i < current.count.length; ++i )
			{
				sumResult.count[ i ] = current.count[ i ] + result.count[ i ];
			}
		}

		// Check to make sure that the result didn't transform the value
		// to zero - if it did, then remove the item from the list if
		// it's an item (non-items are exempt).

		if ( sumResult.isItem() )
		{
			if ( sumResult.getCount() == 0 )
			{
				sourceList.remove( index );
				return;
			}
			else if ( sumResult.getCount() < 0 && ( sourceList == KoLConstants.tally && !KoLSettings.getBooleanProperty( "allowNegativeTally" ) ) )
			{
				sourceList.remove( index );
				return;
			}
		}
		else if ( sumResult.getCount() == 0 && ( sumResult.isStatusEffect() || sumResult.getName().equals(
			AdventureResult.CHOICE ) ) )
		{
			sourceList.remove( index );
			return;
		}
		else if ( sumResult.getCount() < 0 && sumResult.isStatusEffect() )
		{
			sourceList.remove( index );
			return;
		}

		sourceList.set( index, sumResult );
	}

	public static final DefaultListCellRenderer getNameOnlyRenderer()
	{
		return new NameOnlyRenderer();
	}

	public static final DefaultListCellRenderer getDefaultRenderer()
	{
		return AdventureResult.getDefaultRenderer( false );
	}

	public static final DefaultListCellRenderer getDefaultRenderer( final boolean isEquipment )
	{
		return new AdventureResultRenderer( isEquipment );
	}

	public static final DefaultListCellRenderer getCreationQueueRenderer()
	{
		return new ConcoctionRenderer();
	}

	private static class NameOnlyRenderer
		extends DefaultListCellRenderer
	{
		public Component getListCellRendererComponent( final JList list, final Object value, final int index,
			final boolean isSelected, final boolean cellHasFocus )
		{
			return super.getListCellRendererComponent(
				list, value instanceof AdventureResult ? ( (AdventureResult) value ).getName() : value, index,
				isSelected, cellHasFocus );
		}
	}

	private static class ConcoctionRenderer
		extends DefaultListCellRenderer
	{
		public ConcoctionRenderer()
		{
			this.setOpaque( true );
		}

		public Component getListCellRendererComponent( final JList list, final Object value, final int index,
			final boolean isSelected, final boolean cellHasFocus )
		{
			Component defaultComponent =
				this.allowHighlight() ? super.getListCellRendererComponent(
					list, value, index, isSelected, cellHasFocus ) : super.getListCellRendererComponent(
					list, value, index, false, false );

			if ( value == null || !( value instanceof Concoction ) )
			{
				return defaultComponent;
			}

			return this.getRenderer( defaultComponent, (Concoction) value );
		}

		public boolean allowHighlight()
		{
			return false;
		}

		public void appendAmount( final StringBuffer stringForm, final Concoction item )
		{
			stringForm.append( item.getQueued() );
		}

		public Component getRenderer( final Component defaultComponent, final Concoction item )
		{
			StringBuffer stringForm = new StringBuffer();
			boolean meetsRequirement = TradeableItemDatabase.meetsLevelRequirement( item.getName() );

			stringForm.append( "<html>" );

			if ( !meetsRequirement )
			{
				stringForm.append( "<font color=#c0c0c0>" );
			}

			stringForm.append( "<b>" );
			stringForm.append( item.getName() );

			stringForm.append( " (" );
			this.appendAmount( stringForm, item );

			stringForm.append( ")" );
			stringForm.append( "</b><br>&nbsp;" );

			int fullness = TradeableItemDatabase.getFullness( item.getName() );
			int inebriety = TradeableItemDatabase.getInebriety( item.getName() );

			if ( inebriety > 0 )
			{
				stringForm.append( inebriety );
				stringForm.append( " drunk" );
			}
			else
			{
				stringForm.append( fullness );
				stringForm.append( " full" );
			}

			this.appendRange( stringForm, TradeableItemDatabase.getAdventureRange( item.getName() ), "adv" );

			if ( KoLSettings.getBooleanProperty( "showGainsPerUnit" ) )
			{
				if ( inebriety > 0 )
				{
					stringForm.append( " / drunk" );
				}
				else
				{
					stringForm.append( " / full" );
				}
			}

			this.appendRange( stringForm, TradeableItemDatabase.getMuscleRange( item.getName() ), "mus" );
			this.appendRange( stringForm, TradeableItemDatabase.getMysticalityRange( item.getName() ), "mys" );
			this.appendRange( stringForm, TradeableItemDatabase.getMoxieRange( item.getName() ), "mox" );

			if ( !meetsRequirement )
			{
				stringForm.append( "</font>" );
			}

			stringForm.append( "</html>" );

			defaultComponent.setFont( KoLConstants.DEFAULT_FONT );
			( (JLabel) defaultComponent ).setText( stringForm.toString() );
			return defaultComponent;
		}

		private void appendRange( final StringBuffer stringForm, final String range, final String suffix )
		{
			if ( range.equals( "+0.0" ) && !suffix.equals( "adv" ) )
			{
				return;
			}

			stringForm.append( ", " );
			stringForm.append( range );
			stringForm.append( " " );
			stringForm.append( suffix );
		}
	}

	private static class AdventureResultRenderer
		extends ConcoctionRenderer
	{
		private final boolean isEquipment;

		public AdventureResultRenderer( final boolean isEquipment )
		{
			this.setOpaque( true );
			this.isEquipment = isEquipment;
		}

		public boolean allowHighlight()
		{
			return true;
		}

		public Component getListCellRendererComponent( final JList list, final Object value, final int index,
			final boolean isSelected, final boolean cellHasFocus )
		{
			Component defaultComponent =
				super.getListCellRendererComponent( list, value, index, isSelected, cellHasFocus );

			if ( value == null )
			{
				return defaultComponent;
			}

			if ( value instanceof AdventureResult )
			{
				return this.getRenderer( defaultComponent, (AdventureResult) value );
			}

			if ( value instanceof ItemCreationRequest )
			{
				return this.getRenderer( defaultComponent, (ItemCreationRequest) value );
			}

			if ( value instanceof Concoction )
			{
				return this.getRenderer( defaultComponent, (Concoction) value );
			}

			return defaultComponent;
		}

		public Component getRenderer( final Component defaultComponent, final AdventureResult ar )
		{
			if ( !ar.isItem() )
			{
				return defaultComponent;
			}

			StringBuffer stringForm = new StringBuffer();
			stringForm.append( ar.getName() );

			if ( this.isEquipment )
			{
				int power = EquipmentDatabase.getPower( ar.getName() );

				if ( power > 0 )
				{
					stringForm.append( " (+" );
					stringForm.append( power );
					stringForm.append( ")" );
				}
			}
			else
			{
				int value = TradeableItemDatabase.getPriceById( ar.getItemId() );

				if ( value == 0 )
				{
					stringForm.append( " (no-sell)" );
				}
				else
				{
					stringForm.append( " (" );
					stringForm.append( value );
					stringForm.append( " meat)" );
				}
			}

			stringForm.append( " (" );
			stringForm.append( KoLConstants.COMMA_FORMAT.format( ar.count[ 0 ] ) );
			stringForm.append( ")" );

			if ( KoLSettings.getBooleanProperty( "mementoListActive" ) && KoLConstants.mementoList.contains( ar ) )
			{
				stringForm.insert( 0, "<html><font color=olive>" );
				stringForm.append( "</font></html>" );
			}
			else if ( KoLConstants.junkList.contains( ar ) )
			{
				stringForm.insert( 0, "<html><font color=gray>" );
				stringForm.append( "</font></html>" );
			}

			( (JLabel) defaultComponent ).setText( stringForm.toString() );
			return defaultComponent;
		}

		public Component getRenderer( final Component defaultComponent, final ItemCreationRequest icr )
		{
			StringBuffer stringForm = new StringBuffer();
			stringForm.append( icr.getName() );

			if ( this.isEquipment )
			{
				int power = EquipmentDatabase.getPower( icr.getName() );

				if ( power > 0 )
				{
					stringForm.append( " (+" );
					stringForm.append( power );
					stringForm.append( ")" );
				}
			}
			else
			{
				int value = TradeableItemDatabase.getPriceById( icr.getItemId() );

				if ( value == 0 )
				{
					stringForm.append( " (no-sell)" );
				}
				else
				{
					stringForm.append( " (" );
					stringForm.append( value );
					stringForm.append( " meat)" );
				}
			}

			stringForm.append( " (" );
			stringForm.append( KoLConstants.COMMA_FORMAT.format( icr.getQuantityPossible() ) );
			stringForm.append( ")" );

			if ( KoLConstants.junkList.contains( icr.createdItem ) )
			{
				stringForm.insert( 0, "<html><font color=gray>" );
				stringForm.append( "</font></html>" );
			}

			( (JLabel) defaultComponent ).setText( stringForm.toString() );
			return defaultComponent;
		}

		public void appendAmount( final StringBuffer stringForm, final Concoction item )
		{
			if ( item.getItem() != null )
			{
				int modified = item.getTotal();
				int initial = 0;

				if ( item.getItem() != null )
				{
					initial = item.getItem().getCount( KoLConstants.inventory );
				}

				stringForm.append( modified );
				stringForm.append( " possible, " );
				stringForm.append( initial );
				stringForm.append( " current" );
			}
			else
			{
				stringForm.append( item.getPrice() );
				stringForm.append( " meat" );
			}
		}
	}

	public static final DefaultListCellRenderer getEquipmentRenderer()
	{
		return new EquipmentRenderer();
	}

	private static class EquipmentRenderer
		extends DefaultListCellRenderer
	{
		public EquipmentRenderer()
		{
			this.setOpaque( true );
		}

		public Component getListCellRendererComponent( final JList list, final Object value, final int index,
			final boolean isSelected, final boolean cellHasFocus )
		{
			if ( value == null || !( value instanceof AdventureResult ) )
			{
				return super.getListCellRendererComponent( list, value, index, isSelected, cellHasFocus );
			};

			AdventureResult ar = (AdventureResult) value;
			int equipmentType = TradeableItemDatabase.getConsumptionType( ar.getName() );

			int power = EquipmentDatabase.getPower( ar.getName() );
			String stringForm = null;

			if ( equipmentType == KoLConstants.EQUIP_FAMILIAR || ar.equals( EquipmentRequest.UNEQUIP ) )
			{
				if ( ar.equals( EquipmentRequest.UNEQUIP ) )
				{
					stringForm = ar.getName();
				}
				else if ( KoLCharacter.getFamiliar() != null && KoLCharacter.getFamiliar().canEquip( ar ) )
				{
					stringForm = ar.getName();
				}
				else
				{
					stringForm = "<html><font color=gray>" + ar.getName() + "</font></html>";
				}
			}
			else
			{
				if ( equipmentType == KoLConstants.EQUIP_ACCESSORY )
				{
					int count = ar.getCount( KoLConstants.inventory );
					if ( ar.equals( KoLCharacter.getEquipment( KoLCharacter.ACCESSORY1 ) ) )
					{
						++count;
					}
					if ( ar.equals( KoLCharacter.getEquipment( KoLCharacter.ACCESSORY2 ) ) )
					{
						++count;
					}
					if ( ar.equals( KoLCharacter.getEquipment( KoLCharacter.ACCESSORY3 ) ) )
					{
						++count;
					}
					stringForm = ar.getName() + " (" + count + " max)";
				}
				else if ( power > 0 )
				{
					stringForm = ar.getName() + " (+" + KoLConstants.COMMA_FORMAT.format( power ) + ")";
				}
				else
				{
					stringForm = ar.getName();
				}

				// Gray out any equipment that the player cannot currently equip
				// inside of an equipment filter.

				if ( !EquipmentDatabase.canEquip( ar.getName() ) )
				{
					stringForm = "<html><font color=gray>" + stringForm + "</font></html>";
				}
			}

			JLabel defaultComponent =
				(JLabel) super.getListCellRendererComponent( list, value, index, isSelected, cellHasFocus );
			defaultComponent.setText( stringForm );
			return defaultComponent;
		}
	}

	public AdventureResult getNegation()
	{
		if ( this.isItem() )
		{
			return this.count[ 0 ] == 0 ? this : new AdventureResult( this.itemId, 0 - this.count[ 0 ] );
		}
		else if ( this.isStatusEffect() )
		{
			return this.count[ 0 ] == 0 ? this : new AdventureResult( this.name, 0 - this.count[ 0 ], true );
		}

		int[] newcount = new int[ this.count.length ];
		for ( int i = 0; i < this.count.length; ++i )
		{
			newcount[ i ] = 0 - this.count[ i ];
		}

		return this.getInstance( newcount );
	}

	public AdventureResult getInstance( final int quantity )
	{
		if ( this.isItem() )
		{
			return this.count[ 0 ] == quantity ? this : new AdventureResult( this.name, quantity, false );
		}

		if ( this.isStatusEffect() )
		{
			return this.count[ 0 ] == quantity ? this : new AdventureResult( this.name, quantity, true );
		}

		return new AdventureResult( this.name, quantity );
	}

	public AdventureResult getInstance( final int[] quantity )
	{
		if ( this.priority != AdventureResult.SUBSTAT_PRIORITY && this.priority != AdventureResult.FULLSTAT_PRIORITY )
		{
			return this.getInstance( quantity[ 0 ] );
		}

		if ( this.priority == AdventureResult.SUBSTAT_PRIORITY )
		{
			return new AdventureResult( AdventureResult.SUBSTATS, quantity );
		}

		AdventureResult stats = new AdventureResult( AdventureResult.FULLSTATS );
		stats.count = quantity;
		return stats;
	}

	/**
	 * Special method which simplifies the constant use of indexOf and count retrieval. This makes intent more
	 * transparent.
	 */

	public int getCount( final List list )
	{
		int index = list.indexOf( this );
		return index == -1 ? 0 : ( (AdventureResult) list.get( index ) ).getCount();
	}
}
