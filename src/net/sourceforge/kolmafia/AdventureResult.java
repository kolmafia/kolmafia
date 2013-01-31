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

package net.sourceforge.kolmafia;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

import net.sourceforge.kolmafia.objectpool.ItemPool;

import net.sourceforge.kolmafia.persistence.EffectDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.persistence.ItemFinder;
import net.sourceforge.kolmafia.persistence.SkillDatabase;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.request.PurchaseRequest;
import net.sourceforge.kolmafia.request.UneffectRequest;

import net.sourceforge.kolmafia.session.GoalManager;
import net.sourceforge.kolmafia.session.InventoryManager;

import net.sourceforge.kolmafia.utilities.StringUtilities;

public class AdventureResult
	implements Comparable<AdventureResult>
{
	public static final String[] STAT_NAMES = { "muscle", "mysticality", "moxie" };

	private int itemId;
	private int count;
	protected String name;
	protected int priority;

	private static final int NO_PRIORITY = 0;
	private static final int ADV_PRIORITY = 1;
	private static final int MEAT_PRIORITY = 2;
	protected static final int SUBSTAT_PRIORITY = 3;
	protected static final int FULLSTAT_PRIORITY = 4;
	private static final int ITEM_PRIORITY = 5;
	private static final int EFFECT_PRIORITY = 6;

	public static final int PSEUDO_ITEM_PRIORITY = 99;

	protected static final int MONSTER_PRIORITY = -1;

	public static final String HP = "HP";
	public static final String MP = "MP";
	public static final String FACTOID = "Factoid";
	public static final String ADV = "Adv";
	public static final String FREE_CRAFT = "Free Craft";
	public static final String CHOICE = "Choice";
	public static final String AUTOSTOP = "Autostop";
	public static final String DRUNK = "Drunk";
	public static final String MEAT = "Meat";
	public static final String MEAT_SPENT = "Meat Spent";
	public static final String PULL = "Pull";
	public static final String PVP = "PvP";
	public static final String STILL = "Still";
	public static final String TOME = "Tome Summon";

	// Sub/full stats have multiple values and should be delegated
	// to AdventureMultiResult.
	public static final String SUBSTATS = "Substats";
	public static final String FULLSTATS = "Fullstats";

	public static final List<String> MUS_SUBSTAT = new ArrayList<String>();
	public static final List<String> MYS_SUBSTAT = new ArrayList<String>();
	public static final List<String> MOX_SUBSTAT = new ArrayList<String>();

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

	public static final int[] SESSION_SUBSTATS = new int[ 3 ];
	public static final AdventureResult SESSION_SUBSTATS_RESULT =
		new AdventureMultiResult( AdventureResult.SUBSTATS, AdventureResult.SESSION_SUBSTATS );

	public static final int[] SESSION_FULLSTATS = new int[ 3 ];
	public static final AdventureResult SESSION_FULLSTATS_RESULT =
		new AdventureMultiResult( AdventureResult.FULLSTATS, AdventureResult.SESSION_FULLSTATS );

	/**
	 * Constructs a new <code>AdventureResult</code> with the given name. The amount of gain will default to zero.
	 *
	 * @param name The name of the result
	 */

	public AdventureResult( final String name )
	{
		this( AdventureResult.choosePriority( name ), name, 0 );
	}

	public AdventureResult( final String name, final int count )
	{
		this( AdventureResult.choosePriority( name ), name, count );
	}

	public AdventureResult( final String name, final int count, final boolean isStatusEffect )
	{
		this( isStatusEffect ? EFFECT_PRIORITY : ITEM_PRIORITY, name, count );
	}

	public AdventureResult( final int subType, final String name )
	{
		this( subType, name, 1 );
	}

	public AdventureResult( final int subType, final String name, final int count )
	{
		this.name = name;
		this.count = count;
		this.priority = subType;
		if ( this.priority == AdventureResult.EFFECT_PRIORITY )
		{
			this.normalizeEffectName();
		}
		else if ( this.priority == AdventureResult.ITEM_PRIORITY )
		{
			this.normalizeItemName();
		}
		else
		{
			// Detach substring from larger text
			this.name = new String( name );
			if ( this.priority == AdventureResult.PSEUDO_ITEM_PRIORITY )
			{	// bypass normalizeItemName()
				this.itemId = -1;
				this.priority = AdventureResult.ITEM_PRIORITY;
			}
		}
	}

	protected static int choosePriority( final String name )
	{
		if ( name.equals( AdventureResult.ADV ) ||
		     name.equals( AdventureResult.CHOICE ) ||
		     name.equals( AdventureResult.AUTOSTOP ) ||
		     name.equals( AdventureResult.PULL ) ||
		     name.equals( AdventureResult.STILL ) ||
		     name.equals( AdventureResult.TOME )||
		     name.equals( AdventureResult.FREE_CRAFT ) )
		{
			return AdventureResult.ADV_PRIORITY;
		}
		if ( name.equals( AdventureResult.MEAT ) || name.equals( AdventureResult.MEAT_SPENT ) )
		{
			return AdventureResult.MEAT_PRIORITY;
		}
		if ( name.equals( AdventureResult.HP ) || name.equals( AdventureResult.MP ) ||
			 name.equals( AdventureResult.DRUNK ) || name.equals( AdventureResult.PVP ) )
		{
			return AdventureResult.NO_PRIORITY;
		}
		if ( name.equals( AdventureResult.SUBSTATS ) )
		{
			return AdventureResult.SUBSTAT_PRIORITY;
		}
		if ( name.equals( AdventureResult.FULLSTATS ) )
		{
			return AdventureResult.FULLSTAT_PRIORITY;
		}
		if ( EffectDatabase.contains( name ) )
		{
			return AdventureResult.EFFECT_PRIORITY;
		}
		return AdventureResult.ITEM_PRIORITY;
	}

	public AdventureResult( final int itemId, final int count )
	{
		String name = ItemDatabase.getItemDataName( itemId );
		this.name = name != null ? name : "(unknown item " + String.valueOf( itemId ) + ")";
		this.itemId = itemId;
		this.count = count;
		this.priority = AdventureResult.ITEM_PRIORITY;
	}

	public void normalizeEffectName()
	{
		this.priority = AdventureResult.EFFECT_PRIORITY;

		if ( this.name == null )
		{
			this.name = "(unknown effect)";
			return;
		}

		int effectId = EffectDatabase.getEffectId( this.name );
		if ( effectId != -1 )
		{
			String name = EffectDatabase.getEffectName( effectId );
			if ( name != null )
			{
				this.name = name;
			}
			else
			{
				RequestLogger.printLine( "Effect database error: id = " + effectId + " name = \"" + this.name + "\"" );
			}
		}
		else
		{
			this.name = new String( this.name );
		}
	}

	public void normalizeItemName()
	{
		this.priority = AdventureResult.ITEM_PRIORITY;

		if ( this.name == null )
		{
			this.name = "(unknown item " + String.valueOf( this.itemId ) + ")";
			return;
		}

		if ( this.name.equals( "(none)" ) || this.name.equals( "-select an item-" ) )
		{
			return;
		}

		this.itemId = ItemDatabase.getItemId( this.name, this.getCount() );

		if ( this.itemId > 0 )
		{
			String name = ItemDatabase.getItemDataName( this.itemId );
			if ( name != null )
			{
				this.name = name;
			}
			else
			{
				RequestLogger.printLine( "Item database error: id = " + this.itemId + " name = \"" + this.name + "\"" );
			}
		}
		else
		{
			this.name = new String( this.name );
			RequestLogger.printLine( "Unknown item found: " + this.name );
		}
	}

	public static final AdventureResult pseudoItem( final String name )
	{
		AdventureResult item = ItemFinder.getFirstMatchingItem( name, false );
		if ( item != null )
		{
			return item;
		}

		// Make a pseudo-item with the required name
		return AdventureResult.tallyItem( name, false );
	}

	public static final AdventureResult tallyItem( final String name )
	{
		return AdventureResult.tallyItem( name, true );
	}

	public static final AdventureResult tallyItem( final String name, final boolean setItemId )
	{
		AdventureResult item = new AdventureResult( AdventureResult.NO_PRIORITY, name );
		item.priority = AdventureResult.ITEM_PRIORITY;
		item.itemId = setItemId ? ItemDatabase.getItemId( name, 1, false ) : -1;
		return item;
	}

	public static final AdventureResult tallyItem( final String name, final int itemId )
	{
		AdventureResult item = new AdventureResult( AdventureResult.NO_PRIORITY, name );
		item.priority = AdventureResult.ITEM_PRIORITY;
		item.itemId = itemId;
		return item;
	}

	public static final AdventureResult tallyItem( final String name, final int count, final boolean setItemId )
	{
		AdventureResult item = AdventureResult.tallyItem( name, setItemId );
		item.count = count;
		return item;
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
		return false;	// overriden in subclass
	}

	/**
	 * Accessor method to determine if this result is a mysticality gain.
	 *
	 * @return <code>true</code> if this result represents mysticality subpoint gain
	 */

	public boolean isMysticalityGain()
	{
		return false;	// overriden in subclass
	}

	/**
	 * Accessor method to determine if this result is a muscle gain.
	 *
	 * @return <code>true</code> if this result represents muscle subpoint gain
	 */

	public boolean isMoxieGain()
	{
		return false;	// overriden in subclass
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

	public boolean isMeat()
	{
		return this.priority == AdventureResult.MEAT_PRIORITY;
	}

	public boolean isMP()
	{
		return this.name.equals( AdventureResult.MP );
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
		case ItemPool.DUSTY_BOTTLE_OF_MERLOT:
		case ItemPool.DUSTY_BOTTLE_OF_PORT:
		case ItemPool.DUSTY_BOTTLE_OF_PINOT_NOIR:
		case ItemPool.DUSTY_BOTTLE_OF_ZINFANDEL:
		case ItemPool.DUSTY_BOTTLE_OF_MARSALA:
		case ItemPool.DUSTY_BOTTLE_OF_MUSCAT:

			return AdventureResult.dustyBottleName( this.itemId );

		case ItemPool.MILKY_POTION:
		case ItemPool.SWIRLY_POTION:
		case ItemPool.BUBBLY_POTION:
		case ItemPool.SMOKY_POTION:
		case ItemPool.CLOUDY_POTION:
		case ItemPool.EFFERVESCENT_POTION:
		case ItemPool.FIZZY_POTION:
		case ItemPool.DARK_POTION:
		case ItemPool.MURKY_POTION:

			return AdventureResult.bangPotionName( this.itemId );

		case ItemPool.VIAL_OF_RED_SLIME:
		case ItemPool.VIAL_OF_YELLOW_SLIME:
		case ItemPool.VIAL_OF_BLUE_SLIME:
		case ItemPool.VIAL_OF_ORANGE_SLIME:
		case ItemPool.VIAL_OF_GREEN_SLIME:
		case ItemPool.VIAL_OF_VIOLET_SLIME:
		case ItemPool.VIAL_OF_VERMILION_SLIME:
		case ItemPool.VIAL_OF_AMBER_SLIME:
		case ItemPool.VIAL_OF_CHARTREUSE_SLIME:
		case ItemPool.VIAL_OF_TEAL_SLIME:
		case ItemPool.VIAL_OF_INDIGO_SLIME:
		case ItemPool.VIAL_OF_PURPLE_SLIME:

			return AdventureResult.slimeVialName( this.itemId );

		case ItemPool.MOSSY_STONE_SPHERE:
		case ItemPool.SMOOTH_STONE_SPHERE:
		case ItemPool.CRACKED_STONE_SPHERE:
		case ItemPool.ROUGH_STONE_SPHERE:

			return AdventureResult.stoneSphereName( this.itemId );

		case ItemPool.PUNCHCARD_ATTACK:
		case ItemPool.PUNCHCARD_REPAIR:
		case ItemPool.PUNCHCARD_BUFF:
		case ItemPool.PUNCHCARD_MODIFY:
		case ItemPool.PUNCHCARD_BUILD:
		case ItemPool.PUNCHCARD_TARGET:
		case ItemPool.PUNCHCARD_SELF:
		case ItemPool.PUNCHCARD_FLOOR:
		case ItemPool.PUNCHCARD_DRONE:
		case ItemPool.PUNCHCARD_WALL:
		case ItemPool.PUNCHCARD_SPHERE:

			return AdventureResult.punchCardName( this.itemId );

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
		return count;
	}

	public int[] getCounts()
	{	// This should be called on multi-valued subclasses only!
		return null;
	}

	/**
	 * Accessor method to retrieve the total value associated with the result stored at the given index of the count
	 * array.
	 *
	 * @return The total value at the given index of the count array
	 */

	public int getCount( final int index )
	{
		return index != 0 ? 0 : this.count;
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
		if ( s.startsWith( "You gain" ) || s.startsWith( "You lose" ) || s.startsWith( "You spent" ) )
		{
			// A stat has been modified - now you figure out which
			// one it was, how much it's been modified by, and
			// return the appropriate value

			StringTokenizer parsedGain = new StringTokenizer( s, " ." );
			parsedGain.nextToken();		// Skip "You"

			// Decide if the quantity increases or decreases
			int sign = parsedGain.nextToken().startsWith( "gain" ) ? 1 : -1;
			// Make sure we are looking at a number
			String val = parsedGain.nextToken();
			if ( !StringUtilities.isNumeric( val ) )
			{
				return null;
			}

			// Yes. It is safe to parse it as an integer
			int modifier = sign * StringUtilities.parseInt( val );

			// Stats actually fall into one of four categories -
			// simply pick the correct one and return the result.

			String statname = parsedGain.nextToken();

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

			if ( statname.startsWith( "addit" ) )
			{
				statname = parsedGain.nextToken();
			}

			if ( statname.startsWith( "PvP" ) )
			{
				return new AdventureResult( AdventureResult.PVP, modifier );
			}

			if ( parsedGain.hasMoreTokens() )
			{
				char identifier = statname.charAt( 0 );
				return new AdventureResult(
					identifier == 'H' || identifier == 'h' ? AdventureResult.HP : AdventureResult.MP, modifier );
			}

			// In the current implementations, all stats gains are
			// located inside of a generic adventure which
			// indicates how much of each substat is gained.

			int[] gained =
				{ AdventureResult.MUS_SUBSTAT.contains( statname ) ? modifier : 0, AdventureResult.MYS_SUBSTAT.contains( statname ) ? modifier : 0, AdventureResult.MOX_SUBSTAT.contains( statname ) ? modifier : 0 };

			return new AdventureMultiResult( AdventureResult.SUBSTATS, gained );
		}

		return AdventureResult.parseItem( s, false );
	}

	public static final AdventureResult parseItem( final String s, final boolean pseudoAllowed )
	{
		StringTokenizer parsedItem = new StringTokenizer( s, "()" );

		StringBuilder nameBuilder = new StringBuilder( parsedItem.nextToken().trim() );
		int count = 1;
		while ( parsedItem.hasMoreTokens() )
		{
			String next = parsedItem.nextToken().trim();
			if ( !parsedItem.hasMoreTokens() && StringUtilities.isNumeric( next ) )
			{
				count = StringUtilities.parseInt( next );
			}
			else if ( !next.equals( "" ) )
			{
				nameBuilder.append( " (" + next + ")" );
			}
		}

		String name = nameBuilder.toString();

		if ( !pseudoAllowed )
		{
			return new AdventureResult( name, count );
		}

		// Hand craft an item Adventure Result, regardless of the name
		AdventureResult item = new AdventureResult( AdventureResult.NO_PRIORITY, name );
		item.priority = AdventureResult.ITEM_PRIORITY;
		item.itemId = ItemDatabase.getItemId( name, 1, false );
		item.count = count;
		if ( item.itemId > 0 )
		{	// normalize name
			item.name = ItemDatabase.getItemDataName( item.itemId );
		}

		return item;
	}

	/**
	 * Converts the <code>AdventureResult</code> to a <code>String</code>. This is especially useful in debug, or
	 * if the <code>AdventureResult</code> is to be displayed in a <code>ListModel</code>.
	 *
	 * @return The string version of this <code>AdventureResult</code>
	 */

	@Override
	public String toString()
	{
		if ( this.name == null )
		{
			return "(Unrecognized result)";
		}

		if ( this.name.equals( AdventureResult.ADV ) )
		{
			return " Advs Used: " + KoLConstants.COMMA_FORMAT.format( this.count );
		}
		
		if ( this.name.equals( AdventureResult.FREE_CRAFT ) )
		{
			return " Free Crafts: " + KoLConstants.COMMA_FORMAT.format( this.count );
		}

		if ( this.name.equals( AdventureResult.MEAT ) )
		{
			return " Meat Gained: " + KoLConstants.COMMA_FORMAT.format( this.count );
		}

		if ( this.name.equals( AdventureResult.MEAT_SPENT ) )
		{
			return " Meat Spent: " + KoLConstants.COMMA_FORMAT.format( this.count );
		}

		if ( this.name.equals( AdventureResult.CHOICE ) )
		{
			return " Choices Left: " + KoLConstants.COMMA_FORMAT.format( this.count );
		}

		if ( this.name.equals( AdventureResult.AUTOSTOP ) )
		{
			return " Autostops Left: " + KoLConstants.COMMA_FORMAT.format( this.count );
		}

		if ( this.name.equals( AdventureResult.PULL ) )
		{
			return " Budgeted Pulls: " + KoLConstants.COMMA_FORMAT.format( this.count );
		}

		if ( this.name.equals( AdventureResult.STILL ) )
		{
			return " Still Usages: " + KoLConstants.COMMA_FORMAT.format( this.count );
		}

		if ( this.name.equals( AdventureResult.TOME ) )
		{
			return " Tome Summons: " + KoLConstants.COMMA_FORMAT.format( this.count );
		}

		if ( this.name.equals( AdventureResult.HP ) || this.name.equals( AdventureResult.MP ) || this.name.equals( AdventureResult.DRUNK ) )
		{
			return " " + this.name + ": " + KoLConstants.COMMA_FORMAT.format( this.count );
		}

		if ( this.priority == AdventureResult.MONSTER_PRIORITY )
		{
			return this.name;
		}

		String name = this.getName();

		if ( this.priority == AdventureResult.EFFECT_PRIORITY )
		{
			if ( name.equals( "On the Trail" ) )
			{
				String monster = Preferences.getString( "olfactedMonster" );
				if ( !monster.equals( "" ) )
				{
					name = name + " [" + monster + "]";
				}
			}
			else
			{
				String skillName = UneffectRequest.effectToSkill( name );
				if ( SkillDatabase.contains( skillName ) )
				{
					int skillId = SkillDatabase.getSkillId( skillName );
					if ( skillId > 6000 && skillId < 7000 )
					{
						name = "\u266B " + name;
					}
				}
			}
		}

		int count = this.count;

		return count == 1 ? name :
			count > Integer.MAX_VALUE/2 ? name + " (\u221E)" :
			count == PurchaseRequest.MAX_QUANTITY ? name + " (unlimited)" :
			name + " (" + KoLConstants.COMMA_FORMAT.format( count ) + ")";
	}

	public String getConditionType()
	{
		if ( this.name == null )
		{
			return "";
		}

		if ( this.priority == AdventureResult.PSEUDO_ITEM_PRIORITY )
		{
			return this.name.toLowerCase();
		}

		if ( this.name.equals( AdventureResult.ADV ) || this.name.equals( AdventureResult.CHOICE ) )
		{
			return "choiceadv";
		}

		if ( this.name.equals( AdventureResult.AUTOSTOP ) )
		{
			return "autostop";
		}

		if ( this.name.equals( AdventureResult.MEAT ) )
		{
			return "meat";
		}

		if ( this.name.equals( AdventureResult.HP ) )
		{
			return "health";
		}

		if ( this.name.equals( AdventureResult.MP ) )
		{
			return "mana";
		}
		if ( this.name.equals( AdventureResult.FACTOID ) )
		{
			return "factoid";
		}

		return "item";
	}

	public String toConditionString()
	{
		if ( this.name == null )
		{
			return "";
		}

		String conditionType = this.getConditionType();

		if ( !conditionType.equals( "item" ) )
		{
			return this.count + " " + conditionType;
		}

		return "+" + this.count + " " + this.name.replaceAll( "[,\"]", "" );
	}

	/**
	 * Compares the <code>AdventureResult</code> with the given object for name equality. Note that this will still
	 * return <code>true</code> if the values do not match; this merely matches on names.
	 *
	 * @param o The <code>Object</code> to be compared with this <code>AdventureResult</code>
	 * @return <code>true</code> if the <code>Object</code> is an <code>AdventureResult</code> and has the same
	 *	   name as this one
	 */

	@Override
	public boolean equals( final Object o )
	{
		if ( !( o instanceof AdventureResult ) )
		{
			return false;
		}

		AdventureResult ar = (AdventureResult) o;
		if ( this.name == null || ar.name == null )
		{
			return false;
		}
		if ( ar instanceof WildcardResult )
		{
			return ar.equals( this );
		}

		return ( !ar.isItem() || this.itemId == ar.itemId ) &&
			this.name.equalsIgnoreCase( ar.name );
	}

	@Override
	public int hashCode()
	{
		if ( this.name == null )
		{
			return 0;
		}

		return this.name.hashCode();
	}

	/**
	 * Compares the <code>AdventureResult</code> with the given object for name equality and priority differences.
	 * Return values are consistent with the rules laid out in {@link java.lang.Comparable#compareTo(Object)}.
	 */

	public int compareTo( final AdventureResult o )
	{
		if ( o == this ) return 0;
		if ( !( o instanceof AdventureResult ) )
		{
			return -1;
		}

		AdventureResult ar = (AdventureResult) o;

		int priorityDifference = this.priority - ar.priority;
		if ( priorityDifference != 0 )
		{
			return priorityDifference;
		}

		// Do not take exceptions if name is null

		if ( this.name == null )
		{
			return 1;
		}

		if ( ar.name == null )
		{
			return -1;
		}

		int nameComparison = this.name.compareToIgnoreCase( ar.name );
		if ( nameComparison == 0 )
		{
			return 0;
		}

		if ( this.isStatusEffect() )
		{
			return this.getCount() - ar.getCount();
		}

		return nameComparison;
	}

	/**
	 * Utility method used for adding a given <code>AdventureResult</code> to a tally of <code>AdventureResult</code>s.
	 *
	 * @param tally The tally accumulating <code>AdventureResult</code>s
	 * @param result The result to add to the tally
	 */

	public static final void addResultToList( final List<AdventureResult> sourceList, final AdventureResult result )
	{
		int index = sourceList.indexOf( result );

		// First, filter out things where it's a simple addition of an
		// item, or something which may not result in a change in the
		// state of the sourceList list.

		if ( index == -1 )
		{
			if ( !result.isItem() )
			{
				sourceList.add( result );
				return;
			}
			int count = result.getCount();
			if ( count == 0 ) return;
			if ( count < 0 && ( sourceList != KoLConstants.tally ||
				!Preferences.getBoolean( "allowNegativeTally" ) ) )
			{
				return;
			}
			sourceList.add( result );
			if ( sourceList == KoLConstants.inventory )
			{
				InventoryManager.fireInventoryChanged( result.getItemId() );
			}
			return;
		}

		// These don't involve any addition -- ignore this entirely
		// for now.

		if ( result == GoalManager.GOAL_SUBSTATS )
		{
			return;
		}

		// Compute the sum of the existing adventure result and the
		// current adventure result, and construct the sum.

		AdventureResult current = (AdventureResult) sourceList.get( index );

		// Modify substats and fullstats in place
		if ( current instanceof AdventureMultiResult &&
			result instanceof AdventureMultiResult )
		{
			((AdventureMultiResult) current).addResultInPlace(
				(AdventureMultiResult) result );
			return;
		}

		AdventureResult sumResult = current.getInstance( current.count + result.count );

		// Check to make sure that the result didn't transform the value
		// to zero - if it did, then remove the item from the list if
		// it's an item (non-items are exempt).

		if ( sumResult.isItem() )
		{
			if ( sumResult.getCount() == 0 )
			{
				sourceList.remove( index );
				if ( sourceList == KoLConstants.inventory )
				{
					InventoryManager.fireInventoryChanged( result.getItemId() );
				}
				return;
			}
			else if ( sumResult.getCount() < 0 && ( sourceList != KoLConstants.tally || !Preferences.getBoolean( "allowNegativeTally" ) ) )
			{
				sourceList.remove( index );
				if ( sourceList == KoLConstants.inventory )
				{
					InventoryManager.fireInventoryChanged( result.getItemId() );
				}
				return;
			}
			sourceList.set( index, sumResult );
			if ( sourceList == KoLConstants.inventory )
			{
				InventoryManager.fireInventoryChanged( result.getItemId() );
			}
			return;
		}
		else if ( sumResult.getCount() == 0 && ( sumResult.isStatusEffect() || sumResult.getName().equals(
			AdventureResult.CHOICE ) || sumResult.getName().equals( AdventureResult.AUTOSTOP ) ) )
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

	public static final void addOrRemoveResultToList( final List<AdventureResult> sourceList, final AdventureResult result )
	{
		int index = sourceList.indexOf( result );

		if ( index == -1 )
		{
			sourceList.add( result );
			return;
		}

		AdventureResult current = (AdventureResult) sourceList.get( index );
		AdventureResult sumResult = current.getInstance( current.count + result.count );

		if ( sumResult.getCount() <= 0 )
		{
			sourceList.remove( index );
			return;
		}


		sourceList.set( index, sumResult );
	}

	public static final void removeResultFromList( final List sourceList, final AdventureResult result )
	{
		int index = sourceList.indexOf( result );
		if ( index != -1 )
		{
			sourceList.remove( index );
		}
	}

	public AdventureResult getNegation()
	{
		if ( this.isItem() && this.itemId != -1 )
		{
			return this.count == 0 ? this : new AdventureResult( this.itemId, 0 - this.count );
		}
		else if ( this.isStatusEffect() )
		{
			return this.count == 0 ? this : new AdventureResult( this.name, 0 - this.count, true );
		}

		return this.getInstance( -this.count );
	}

	public AdventureResult getInstance( final int quantity )
	{
		if ( this.isItem() )
		{
			if ( this.count == quantity )
			{
				return this;
			}

			// Handle pseudo and tally items
			AdventureResult item = new AdventureResult( AdventureResult.NO_PRIORITY, this.name );
			item.priority = AdventureResult.ITEM_PRIORITY;
			item.itemId = this.itemId;
			item.count = quantity;
			return item;
		}

		if ( this.isStatusEffect() )
		{
			return this.count == quantity ? this : new AdventureResult( this.name, quantity, true );
		}

		return new AdventureResult( this.name, quantity );
	}

	public AdventureResult getInstance( final int[] quantity )
	{
		return this.getInstance( quantity[ 0 ] );
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

	public static AdventureResult findItem( final int itemId, final List list )
	{
		Iterator it = list.iterator();
		while ( it.hasNext() )
		{
			AdventureResult item = (AdventureResult)it.next();
			if ( item.getItemId() == itemId )
			{
				return item;
			}
		}
		return null;
	}

	public static final String bangPotionName( final int itemId )
	{
		String itemName = ItemDatabase.getItemDataName( itemId );

		String effect = Preferences.getString( "lastBangPotion" + itemId );
		if ( effect.equals( "" ) )
		{
			return itemName;
		}

		return itemName + " of " + effect;
	}

	public static final String dustyBottleName( final int itemId )
	{
		String itemName = ItemDatabase.getItemDataName( itemId );

		int glyph = Preferences.getInteger( "lastDustyBottle" + itemId );
		if ( glyph == 0 )
		{
			return itemName;
		}

		return StringUtilities.globalStringReplace( itemName, " of", " of " + ItemDatabase.glyphType( glyph ) );
	}

	public static final String slimeVialName( final int itemId )
	{
		String itemName = ItemDatabase.getItemDataName( itemId );

		String effect = Preferences.getString( "lastSlimeVial" + itemId );
		if ( effect.equals( "" ) )
		{
			return itemName;
		}

		return itemName + ": " + effect;
	}

	public final String bangPotionAlias()
	{
		if ( this.itemId >= 819 && this.itemId <= 827 )
		{
			String effect = Preferences.getString( "lastBangPotion" + this.itemId );
			if ( effect.equals( "" ) )
			{
				return this.name;
			}

			return "potion of " + effect;
		}
		if ( this.itemId >= ItemPool.VIAL_OF_RED_SLIME && this.itemId <= ItemPool.VIAL_OF_PURPLE_SLIME )
		{
			String effect = Preferences.getString( "lastSlimeVial" + this.itemId );
			if ( effect.equals( "" ) )
			{
				return this.name;
			}

			return "vial of slime: " + effect;
		}

		return this.name;
	}

	public static final String stoneSphereName( final int itemId )
	{
		String itemName = ItemDatabase.getItemDataName( itemId );

		String effect = Preferences.getString( "lastStoneSphere" + itemId );
		if ( effect.equals( "" ) )
		{
			return itemName;
		}

		return itemName + " of " + effect;
	}

	public static final String punchCardName( final int itemId )
	{
		for ( int i = 0; i < RequestEditorKit.PUNCHCARDS.length; ++i )
		{
			Object [] punchcard = RequestEditorKit.PUNCHCARDS[i];
			if ( ( (Integer) punchcard[0]).intValue() == itemId )
			{
				return (String) punchcard[2];
			}
		}

		return ItemDatabase.getItemDataName( itemId );
	}

	// AdventureMultiResult handles the specific stat-related result types
	// that must store multiple values, rather than having a 1-element count
	// array inside every AdventureResult.
	public static class AdventureMultiResult
		extends AdventureResult
	{
		private int[] counts;
		
		public AdventureMultiResult( final String name, final int[] counts )
		{
			this( AdventureResult.choosePriority( name ), name, counts );
		}

		protected AdventureMultiResult( final int subType, final String name, final int[] counts )
		{
			super( subType, name, counts[ 0 ] );
			this.counts = counts;
		}
		
		/**
		 * Accessor method to determine if this result is a muscle gain.
		 *
		 * @return <code>true</code> if this result represents muscle subpoint gain
		 */
	
		@Override
		public boolean isMuscleGain()
		{
			return this.priority == AdventureResult.SUBSTAT_PRIORITY && this.counts[ 0 ] != 0;
		}
	
		/**
		 * Accessor method to determine if this result is a mysticality gain.
		 *
		 * @return <code>true</code> if this result represents mysticality subpoint gain
		 */
	
		@Override
		public boolean isMysticalityGain()
		{
			return this.priority == AdventureResult.SUBSTAT_PRIORITY && this.counts[ 1 ] != 0;
		}
	
		/**
		 * Accessor method to determine if this result is a muscle gain.
		 *
		 * @return <code>true</code> if this result represents muscle subpoint gain
		 */
	
		@Override
		public boolean isMoxieGain()
		{
			return this.priority == AdventureResult.SUBSTAT_PRIORITY && this.counts[ 2 ] != 0;
		}
	
		/**
		 * Accessor method to retrieve the total value associated with the result. In the event of substat points, this
		 * returns the total subpoints within the <code>AdventureResult</code>; in the event of an item or meat gains,
		 * this will return the total number of meat/items in this result.
		 *
		 * @return The amount associated with this result
		 */
	
		@Override
		public int getCount()
		{
			int totalCount = 0;
			for ( int i = 0; i < this.counts.length; ++i )
			{
				totalCount += this.counts[ i ];
			}
			return totalCount;
		}
	
		@Override
		public int[] getCounts()
		{
			return this.counts;
		}

		/**
		 * Accessor method to retrieve the total value associated with the result stored at the given index of the count
		 * array.
		 *
		 * @return The total value at the given index of the count array
		 */
	
		@Override
		public int getCount( final int index )
		{
			return index < 0 || index >= this.counts.length ? 0 : this.counts[ index ];
		}

		/**
		 * Converts the <code>AdventureResult</code> to a <code>String</code>. This is especially useful in debug, or
		 * if the <code>AdventureResult</code> is to be displayed in a <code>ListModel</code>.
		 *
		 * @return The string version of this <code>AdventureResult</code>
		 */
	
		@Override
		public String toString()
		{
			if ( this.name.equals( AdventureResult.SUBSTATS ) || this.name.equals( AdventureResult.FULLSTATS ) )
			{
				return " " + this.name + ": " + KoLConstants.COMMA_FORMAT.format( this.counts[ 0 ] ) + " / " + KoLConstants.COMMA_FORMAT.format( this.counts[ 1 ] ) + " / " + KoLConstants.COMMA_FORMAT.format( this.counts[ 2 ] );
			}
	
			return "(Unrecognized multi-result)";
		}

		@Override
		public String toConditionString()
		{
			if ( this.name.equals( AdventureResult.SUBSTATS ) )
			{
				StringBuilder stats = new StringBuilder();
	
				if ( this.counts[ 0 ] > 0 )
				{
					stats.append( KoLCharacter.calculateBasePoints( KoLCharacter.getTotalMuscle() + this.counts[ 0 ] ) + " muscle" );
				}
	
				if ( this.counts[ 1 ] > 0 )
				{
					if ( this.counts[ 0 ] > 0 )
					{
						stats.append( ", " );
					}
	
					stats.append( KoLCharacter.calculateBasePoints( KoLCharacter.getTotalMysticality() + this.counts[ 1 ] ) + " mysticality" );
				}
	
				if ( this.counts[ 2 ] > 0 )
				{
					if ( this.counts[ 0 ] > 0 || this.counts[ 1 ] > 0 )
					{
						stats.append( ", " );
					}
	
					stats.append( KoLCharacter.calculateBasePoints( KoLCharacter.getTotalMoxie() + this.counts[ 2 ] ) + " moxie" );
				}
	
				return stats.toString();
			}
	
			return super.toConditionString();
		}
		
		protected void addResultInPlace( AdventureMultiResult result )
		{
			for ( int i = 0; i < this.counts.length; ++i )
			{
				this.counts[ i ] += result.counts[ i ];
			}
		}

		@Override
		public AdventureResult getNegation()
		{
			int[] newcounts = new int[ this.counts.length ];
			for ( int i = 0; i < this.counts.length; ++i )
			{
				newcounts[ i ] = 0 - this.counts[ i ];
			}
	
			return this.getInstance( newcounts );
		}

		@Override
		public AdventureResult getInstance( final int[] quantity )
		{
			if ( this.priority == AdventureResult.SUBSTAT_PRIORITY )
			{
				return new AdventureMultiResult( AdventureResult.SUBSTATS, quantity );
			}
	
			return new AdventureMultiResult( AdventureResult.FULLSTATS, quantity );
		}
	}

	public static class WildcardResult
	extends AdventureResult
	{
		// Note that these objects must not be placed in a sorted list, since they
		// are not meaningfully comparable other than via equals().
		private String match;
		private String[] matches;
		private boolean negated;

		public WildcardResult( String name, int count, String match, boolean negated )
		{
			super( AdventureResult.ITEM_PRIORITY, name, count );

			this.match = match;
			this.matches = match.split( "\\s*[|/]\\s*" );

			for ( int i = 0; i < matches.length; ++i )
			{
				this.matches[ i ] = this.matches[ i ].toLowerCase();
			}

			this.negated = negated;
		}

		@Override
		public AdventureResult getInstance( int count )
		{
			return new WildcardResult( this.getName(), count, this.match, this.negated );
		}

		@Override
		public boolean equals( final Object o )
		{
			if ( !( o instanceof AdventureResult ) )
			{
				return false;
			}

			boolean hasMatch = false;
			AdventureResult ar = (AdventureResult) o;
			String arName = ar.getName().toLowerCase();

			for ( int i = 0; i < this.matches.length && !hasMatch; ++i )
			{
				hasMatch = arName.indexOf( this.matches[ i ] ) != -1;
			}

			return hasMatch ^ this.negated;
		}

		@Override
		public int getCount( final List list )
		{
			int count = 0;
			Iterator i = list.iterator();
			while ( i.hasNext() )
			{
				AdventureResult ar = (AdventureResult) i.next();
				if ( this.equals( ar ) )
				{
					count += ar.getCount();
				}
			}
			return count;
		}

		@Override
		public void normalizeItemName()
		{	// Overridden to avoid "unknown item found" messages.
		}

		public static WildcardResult getInstance( String text )
		{
			if ( text.indexOf( "any" ) == -1 )
			{
				return null;
			}

			String[] pieces = text.split( " ", 2 );
			int count = StringUtilities.isNumeric( pieces[ 0 ] ) ?
				StringUtilities.parseInt( pieces[ 0 ] ) : 0;
			if ( pieces.length > 1 && count != 0 )
			{
				text = pieces[ 1 ];
			}
			else
			{
				count = 1;
			}

			if ( text.startsWith( "any " ) )
			{
				return new WildcardResult( text, count, text.substring( 4 ), false );
			}
			if ( text.startsWith( "anything but " ) )
			{
				return new WildcardResult( text, count, text.substring( 13 ), true );
			}
			return null;
		}
	}
}
