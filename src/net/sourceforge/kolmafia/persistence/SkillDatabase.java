/**
 * Copyright (c) 2005-2009, KoLmafia development team
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

package net.sourceforge.kolmafia.persistence;

import java.io.BufferedReader;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashMap;
import java.util.TreeMap;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLDatabase;
import net.sourceforge.kolmafia.StaticEntity;
import net.sourceforge.kolmafia.request.UseSkillRequest;
import net.sourceforge.kolmafia.session.InventoryManager;
import net.sourceforge.kolmafia.utilities.FileUtilities;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class SkillDatabase
	extends KoLDatabase
{
	private static String [] canonicalNames = new String[0];
	private static final Map skillById = new TreeMap();
	private static final Map skillByName = new TreeMap();
	private static final Map mpConsumptionById = new HashMap();
	private static final Map skillTypeById = new TreeMap();
	private static final Map durationById = new HashMap();
	private static final Map levelById = new HashMap();

	private static final Map skillsByCategory = new HashMap();
	private static final Map skillCategoryById = new HashMap();

	public static final int CASTABLE = -1;
	public static final int PASSIVE = 0;
	public static final int SUMMON = 1;
	public static final int REMEDY = 2;
	public static final int SELF_ONLY = 3;
	public static final int BUFF = 4;
	public static final int COMBAT = 5;

	// Mr. Skills
	public static final int SNOWCONE = 8000;
	public static final int STICKER = 8001;
	public static final int CANDY_HEART = 8100;
	public static final int PARTY_FAVOR = 8101;
	public static final int LOVE_SONG = 8102;
	public static final int HILARIOUS = 8200;
	public static final int TASTEFUL = 8201;

	public static final int RAINBOW = 44;

	private static final String UNCATEGORIZED = "uncategorized";
	private static final String GNOME_SKILLS = "gnome trainer";
	private static final String BAD_MOON = "bad moon";
	private static final String MR_SKILLS = "mr. skills";

	private static final String[] CATEGORIES = new String[]
	{
		SkillDatabase.UNCATEGORIZED,
		"seal clubber",
		"turtle tamer",
		"pastamancer",
		"sauceror",
		"disco bandit",
		"accordion thief",
		SkillDatabase.GNOME_SKILLS,
		SkillDatabase.MR_SKILLS,
		SkillDatabase.BAD_MOON
	};

	static
	{
		// This begins by opening up the data file and preparing
		// a buffered reader; once this is done, every line is
		// examined and float-referenced: once in the name-lookup,
		// and again in the Id lookup.

		for ( int i = 0; i < SkillDatabase.CATEGORIES.length; ++i )
		{
			SkillDatabase.skillsByCategory.put( SkillDatabase.CATEGORIES[ i ], new ArrayList() );
		}

		BufferedReader reader = FileUtilities.getVersionedReader( "classskills.txt", KoLConstants.CLASSSKILLS_VERSION );

		String[] data;

		while ( ( data = FileUtilities.readData( reader ) ) != null )
		{
			if ( data.length < 5 )
			{
				continue;
			}

			Integer id = Integer.valueOf( data[ 0 ] );
			String name = StringUtilities.getDisplayName( data[ 1 ] );
			Integer type = Integer.valueOf( data[ 2 ] );
			Integer mp = Integer.valueOf( data[ 3 ] );
			Integer duration = Integer.valueOf( data[ 4 ] );
			Integer level = ( data.length > 5 ) ? Integer.valueOf( data[ 5 ] ) : null;
			SkillDatabase.addSkill( id, name, type, mp, duration, level );
		}

		try
		{
			reader.close();
		}
		catch ( Exception e )
		{
			// This should not happen.  Therefore, print
			// a stack trace for debug purposes.

			StaticEntity.printStackTrace( e );
		}

		SkillDatabase.canonicalNames = new String[ SkillDatabase.skillByName.size() ];
		SkillDatabase.skillByName.keySet().toArray( SkillDatabase.canonicalNames );
	}

	private static final void addSkill( final Integer skillId, final String skillName, final Integer skillType, final Integer mpConsumption, final Integer duration, final Integer level )
	{
		SkillDatabase.skillById.put( skillId, skillName );
		SkillDatabase.skillByName.put( StringUtilities.getCanonicalName( skillName ), skillId );

		SkillDatabase.skillTypeById.put( skillId, skillType );

		SkillDatabase.mpConsumptionById.put( skillId, mpConsumption );
		SkillDatabase.durationById.put( skillId, duration );
		if ( level != null )
		{
			SkillDatabase.levelById.put( skillId, level );
		}

		String category;
		int categoryId = skillId.intValue() / 1000;

		switch ( skillId.intValue() )
		{
		case 3:			// Smile of Mr. A
		case SNOWCONE:		// Summon Snowcones
		case STICKER:		// Summon Stickers
		case HILARIOUS:		// Summon Hilarious Objects
		case TASTEFUL:		// Summon Tasteful Items
		case CANDY_HEART:	// Summon Candy Hearts
		case PARTY_FAVOR:	// Summon Party Favor
		case LOVE_SONG:		// Summon Love Song

			category = SkillDatabase.MR_SKILLS;
			break;

		case 10: // Powers of Observatiogn
		case 11: // Gnefarious Pickpocketing
		case 12: // Torso Awaregness
		case 13: // Gnomish Hardigness
		case 14: // Cosmic Ugnderstanding

			category = SkillDatabase.GNOME_SKILLS;
			break;

		case 21: // Lust
		case 22: // Gluttony
		case 23: // Greed
		case 24: // Sloth
		case 25: // Wrath
		case 26: // Envy
		case 27: // Pride

			category = SkillDatabase.BAD_MOON;
			break;

		default:

			// Moxious maneuver has a 7000 id, but
			// it's not gained by equipment.

			if ( categoryId == 7 )
			{
				category = SkillDatabase.UNCATEGORIZED;
			}
			else
			{
				category = SkillDatabase.CATEGORIES[ categoryId ];
			}
		}

		SkillDatabase.skillCategoryById.put( skillId, category );
		( (ArrayList) SkillDatabase.skillsByCategory.get( category ) ).add( skillName );
	}

	public static final List getSkillsByCategory( String category )
	{
		if ( category == null )
		{
			return new ArrayList();
		}

		List categoryMatches = StringUtilities.getMatchingNames( SkillDatabase.CATEGORIES, category );

		if ( categoryMatches.size() != 1 )
		{
			return new ArrayList();
		}

		category = (String) categoryMatches.get( 0 );

		List skills = (List) SkillDatabase.skillsByCategory.get( category );

		if ( skills == null )
		{
			return new ArrayList();
		}

		return skills;
	}

	/**
	 * Returns a list of all skills which contain the given substring. This is useful for people who are doing lookups
	 * on skills.
	 */

	public static final List getMatchingNames( final String substring )
	{
		return StringUtilities.getMatchingNames( SkillDatabase.canonicalNames, substring );
	}

	/**
	 * Returns the name for an skill, given its Id.
	 *
	 * @param skillId The Id of the skill to lookup
	 * @return The name of the corresponding skill
	 */

	public static final String getSkillName( final int skillId )
	{
		return (String) SkillDatabase.skillById.get( new Integer( skillId ) );
	}

	/**
	 * Returns the level for an skill, given its Id.
	 *
	 * @param skillId The Id of the skill to lookup
	 * @return The level of the corresponding skill
	 */

	public static final int getSkillLevel( final int skillId )
	{
		Object level = SkillDatabase.levelById.get( new Integer( skillId ) );
		return level == null ? -1 : ( (Integer) level ).intValue();
	}

	public static final int getSkillPurchaseCost( final int skillId )
	{
		int level = SkillDatabase.getSkillLevel( skillId );
		if ( level < 1 )
		{
			return 0;
		}
		if ( level == 1 )
		{
			return 200;
		}
		if ( level == 2 )
		{
			return 800;
		}
		if ( level == 3 )
		{
			return 1800;
		}
		if ( level == 4 )
		{
			return 3200;
		}
		return level * 1000;
	}

	/**
	 * Returns the type for an skill, given its Id.
	 *
	 * @param skillId The Id of the skill to lookup
	 * @return The type of the corresponding skill
	 */

	public static final int getSkillType( final int skillId )
	{
		Object skillType = SkillDatabase.skillTypeById.get( new Integer( skillId ) );
		return skillType == null ? -1 : ( (Integer) skillType ).intValue();
	}

	/**
	 * Returns the Id number for an skill, given its name.
	 *
	 * @param skillName The name of the skill to lookup
	 * @return The Id number of the corresponding skill
	 */

	public static final int getSkillId( final String skillName )
	{
		Object skillId = SkillDatabase.skillByName.get( StringUtilities.getCanonicalName( skillName ) );
		return skillId == null ? -1 : ( (Integer) skillId ).intValue();
	}

	/**
	 * Returns how much MP is consumed by using the skill with the given Id.
	 *
	 * @param skillId The id of the skill to lookup
	 * @return The MP consumed by the skill, or 0 if unknown
	 */

	public static final int getMPConsumptionById( final int skillId )
	{
		if ( isLibramSkill( skillId ) )
		{
			return libramSkillMPConsumption();
		}

		// Moxious Maneuver has a special mana cost.
		if ( skillId == 7008 )
		{
			return Math.max( KoLCharacter.getLevel() + KoLCharacter.getManaCostAdjustment(), 1 );
		}

		// Magic Missile has a special mana cost.
		if ( skillId == 7009 )
		{
			return Math.max(
				Math.min( ( KoLCharacter.getLevel() + 3 ) / 2, 6 ) + KoLCharacter.getManaCostAdjustment(), 1 );
		}

		if ( SkillDatabase.getSkillType( skillId ) == SkillDatabase.PASSIVE )
		{
			return 0;
		}

		Object mpConsumption = SkillDatabase.mpConsumptionById.get( new Integer( skillId ) );

		if ( mpConsumption == null )
		{
			return 0;
		}

		int cost = ( (Integer) mpConsumption ).intValue();

		return cost == 0 ? 0 : Math.max( cost + KoLCharacter.getManaCostAdjustment(), 1 );
	}

	/**
	 * Determines if a skill comes from a Libram
	 *
	 * @param skillId The Id of the skill to lookup
	 * @return true if it comes from a Libram
	 */

	public static final boolean isLibramSkill( final int skillId )
	{
		return skillId == CANDY_HEART || skillId == PARTY_FAVOR || skillId == LOVE_SONG;
	}

	/**
	 * Determines the cost for next casting of a libram skill
	 *
	 * @return the MP cost to cast it
	 */

	public static final int libramSkillMPConsumption()
	{
		int cast = Preferences.getInteger( "libramSummons" );
		return libramSkillMPConsumption( cast );
	}

	public static final void setLibramSkillCasts( int cost )
	{
		// With sufficient mana cost reduction, the first, second, and
		// third libram summons all cost 1 MP. Therefore, we can't
		// necessarily tell how many times librams have been used today
		// by looking at the summoning cost.

		// Heuristic: if the mana cost shown by the bookcase agrees
		// with our current calculated mana cost, assume we have it
		// right. Otherwise, assume that summons have been made outside
		// of KoLmafia and back-calculate from the bookshelf's cost.

		// Get KoLmafia's idea of number of casts
		int casts = Preferences.getInteger( "libramSummons" );

		// If the next cast costs what the bookshelf says it costs,
		// assume we're correct.
		if ( libramSkillMPConsumption( casts + 1 ) == cost )
		{
			return;
		}

		// Otherwise, derive number of casts from unadjusted mana cost
		cost -= KoLCharacter.getManaCostAdjustment();

		// cost = 1 + (n * (n-1) / 2)
		//
		// n^2 - n + (2 - 2cost) = 0
		//
		// Use the quadratic formula
		//
		//    a = 1, b = -1, c = 2-2*cost
		//
		// x = ( 1 + sqrt(8*cost - 7))/2

		int count =  ( 1 + (int)Math.sqrt( 8 * cost - 7 ) ) / 2;

		Preferences.setInteger( "libramSummons", count - 1 );
		KoLConstants.summoningSkills.sort();
		KoLConstants.usableSkills.sort();
	}

	/**
	 * Determines the cost for a specific casting of a libram skill
	 *
	 * @param count which casting
	 * @return the MP cost to cast it
	 */

	public static final int libramSkillMPConsumption( final int cast )
	{
		// Old formula: n * (n+1) / 2
		// return Math.max( ( cast + 1 ) * ( cast + 2 ) / 2 + KoLCharacter.getManaCostAdjustment(), 1 );

		// New formula: 1 + (n * (n-1) / 2)
		return Math.max( 1 + ( cast + 1 ) * cast / 2 + KoLCharacter.getManaCostAdjustment(), 1 );
	}

	/**
	 * Determines the cost for casting a libram skill multiple times
	 *
	 * @param cast	which casting
	 * @param count	how many casts
	 * @return the MP cost to cast it
	 */

	public static final int libramSkillMPConsumption( int cast, int count )
	{
		int total = 0;
		while ( count-- > 0 )
		{
			total += libramSkillMPConsumption( cast++ );
		}
		return total;
	}

	/**
	 * Determines how many times you can cast libram skills with the
	 * specified amount of MP
	 *
	 * @param availableMP	how much MP is available
	 * @return the number of casts
	 */

	public static final int libramSkillCasts( int availableMP )
	{
		return libramSkillCasts( Preferences.getInteger( "libramSummons" ), availableMP );
	}

	/**
	 * Determines how many times you can cast libram skills with the
	 * specified amount of MP starting with specified casting
	 *
	 * @param cast	which casting
	 * @param availableMP	how much MP is available
	 * @return the number of casts
	 */

	public static final int libramSkillCasts( int cast, int availableMP )
	{
		int mpCost = SkillDatabase.libramSkillMPConsumption( cast );
		int count = 0;

		while ( mpCost <= availableMP )
		{
			count++;
			availableMP -= mpCost;
			mpCost = SkillDatabase.libramSkillMPConsumption( ++cast );
		}

		return count;
	}

	/**
	 * Returns how many rounds of buff are gained by using the skill with the given Id.
	 *
	 * @param skillId The id of the skill to lookup
	 * @return The duration of effect the cast gives
	 */

	public static final int getEffectDuration( final int skillId )
	{
		Object duration = SkillDatabase.durationById.get( new Integer( skillId ) );
		if ( duration == null )
		{
			return 0;
		}

		int actualDuration = ( (Integer) duration ).intValue();
		if ( actualDuration == 0 || SkillDatabase.getSkillType( skillId ) != SkillDatabase.BUFF )
		{
			return actualDuration;
		}

		AdventureResult[] weapons = null;

		if ( skillId > 2000 && skillId < 3000 )
		{
			weapons = UseSkillRequest.TAMER_WEAPONS;
		}

		if ( skillId > 4000 && skillId < 5000 )
		{
			weapons = UseSkillRequest.SAUCE_WEAPONS;
		}

		if ( skillId > 6000 && skillId < 7000 )
		{
			weapons = UseSkillRequest.THIEF_WEAPONS;
		}

		if ( weapons != null )
		{
			if ( KoLConstants.inventory.contains( weapons[ 0 ] ) || KoLCharacter.hasEquipped( weapons[ 0 ] ) )
			{
				actualDuration += 10;
			}
			else if ( KoLConstants.inventory.contains( weapons[ 1 ] ) || KoLCharacter.hasEquipped( weapons[ 1 ] ) )
			{
				actualDuration += 5;
			}
			else if ( weapons == UseSkillRequest.THIEF_WEAPONS )
			{
				if ( KoLConstants.inventory.contains( weapons[ 2 ] ) || KoLCharacter.hasEquipped( weapons[ 2 ] ) )
				{
					actualDuration += 2;
				}
				else if ( !KoLConstants.inventory.contains( weapons[ 3 ] ) && !KoLCharacter.hasEquipped( weapons[ 3 ] ) )
				{
					return 0;
				}
			}
			else if ( !KoLConstants.inventory.contains( weapons[ 2 ] ) && !KoLCharacter.hasEquipped( weapons[ 2 ] ) )
			{
				return 0;
			}
		}

		if ( InventoryManager.hasItem( UseSkillRequest.WIZARD_HAT ) )
		{
			actualDuration += 5;
		}

		return actualDuration;
	}

	/**
	 * Returns whether or not this is a normal skill that can only be used on the player.
	 *
	 * @return <code>true</code> if the skill is a normal skill
	 */

	public static final boolean isNormal( final int skillId )
	{
		Object skillType = SkillDatabase.skillTypeById.get( new Integer( skillId ) );
		if ( skillType == null )
			return false;
		int type = ( (Integer) skillType ).intValue();
		return type == SUMMON || type == REMEDY || type == SELF_ONLY;
	}

	/**
	 * Returns whether or not the skill is a passive.
	 *
	 * @return <code>true</code> if the skill is passive
	 */

	public static final boolean isPassive( final int skillId )
	{
		return SkillDatabase.isType( skillId, SkillDatabase.PASSIVE );
	}

	/**
	 * Returns whether or not the skill is a buff (ie: can be used on others).
	 *
	 * @return <code>true</code> if the skill can target other players
	 */

	public static final boolean isBuff( final int skillId )
	{
		return SkillDatabase.isType( skillId, SkillDatabase.BUFF );
	}

	/**
	 * Returns whether or not the skill is a combat skill (ie: can be used while fighting).
	 *
	 * @return <code>true</code> if the skill can be used in combat
	 */

	public static final boolean isCombat( final int skillId )
	{
		return SkillDatabase.isType( skillId, SkillDatabase.COMBAT );
	}

	/**
	 * Utility method used to determine if the given skill is of the appropriate type.
	 */

	private static final boolean isType( final int skillId, final int type )
	{
		Object skillType = SkillDatabase.skillTypeById.get( new Integer( skillId ) );
		return skillType == null ? false : ( (Integer) skillType ).intValue() == type;
	}

	/**
	 * Utility method used to determine if the given skill can be made permanent
	 */

	public static final boolean isPermable( final int skillId )
	{
		switch ( skillId )
		{
		case 1:		// Liver of Steel
		case 5:		// Stomach of Steel
		case 6:		// Spleen of Steel
			// Steel Organs
			return false;

		case 21:	// Lust
		case 22:	// Gluttony
		case 23:	// Greed
		case 24:	// Sloth
		case 25:	// Wrath
		case 26:	// Envy
		case 27:	// Pride
			// Bad Moon skills
			return false;

		case 1022:	// Clobber
		case 2023:	// Toss
		case 3020:	// Spaghetti Spear
		case 4020:	// Salsaball
		case 5020:	// Suckerpunch
		case 6025:	// Sing
			// Class starting combat skills
			return false;

		case 2103:	// Head + Knee Combo
		case 2105:	// Head + Shield Combo
		case 2106:	// Knee + Shield Combo
		case 2107:	// Head + Knee + Shield Combo
		case 3101:	// Spirit of Cayenne
		case 3102:	// Spirit of Peppermint
		case 3103:	// Spirit of Garlic
		case 3104:	// Spirit of Wormwood
		case 3105:	// Spirit of Bacon Grease
			// Derived skills
			return false;

		case 7008:	// Moxious Maneuver
			return false;

		case 8000:	// Summon Snowcones
		case 8001:	// Summon Stickers
		case 8100:	// Summon Candy Hearts
		case 8101:	// Summon Party Favor
		case 8102:	// Summon Love Song
		case 8200:	// Summon Hilarious Objects
		case 8201:	// Summon Tasteful Items
			// Bookshelf skills
			return false;
		}

		if ( skillId / 1000 == 7 )
		{
			// Skills granted by items
			return false;
		}

		return true;
	}

	/**
	 * Returns all skills in the database of the given type.
	 */

	public static final List getSkillsByType( final int type )
	{
		ArrayList list = new ArrayList();

		boolean shouldAdd = false;
		Object[] keys = SkillDatabase.skillTypeById.keySet().toArray();

		for ( int i = 0; i < keys.length; ++i )
		{
			shouldAdd = false;

			Object id = keys[ i ];
			Object value = SkillDatabase.skillTypeById.get( id );
			if ( value == null )
				continue;
			int skillType = ( (Integer) value ).intValue();
			int skillId = ( (Integer) id ).intValue();

			if ( type == SkillDatabase.CASTABLE )
			{
				shouldAdd = skillType == SUMMON || skillType == REMEDY || skillType == SELF_ONLY || skillType == BUFF;
			}
			else if ( skillId == 3009 )
			{
				shouldAdd = type == REMEDY || type == COMBAT;
			}
			else
			{
				shouldAdd = skillType == type;
			}

			if ( shouldAdd )
			{
				list.add( UseSkillRequest.getInstance( ( (Integer) keys[ i ] ).intValue() ) );
			}
		}

		return list;
	}

	private static final String toTitleCase( final String s )
	{
		boolean found = false;
		char[] chars = s.toLowerCase().toCharArray();

		for ( int i = 0; i < chars.length; ++i )
		{
			if ( !found && Character.isLetter( chars[ i ] ) )
			{
				chars[ i ] = Character.toUpperCase( chars[ i ] );
				found = true;
			}
			else if ( Character.isWhitespace( chars[ i ] ) )
			{
				found = false;
			}
		}

		return String.valueOf( chars );
	}

	/**
	 * Returns whether or not an item with a given name exists in the database; this is useful in the event that an item
	 * is encountered which is not tradeable (and hence, should not be displayed).
	 *
	 * @return <code>true</code> if the item is in the database
	 */

	public static final boolean contains( final String skillName )
	{
		return Arrays.binarySearch( SkillDatabase.canonicalNames, StringUtilities.getCanonicalName( skillName ) ) >= 0;
	}

	/**
	 * Returns the set of skills keyed by name
	 *
	 * @return The set of skills keyed by name
	 */

	public static final Set entrySet()
	{
		return SkillDatabase.skillById.entrySet();
	}

	private static final ArrayList skillNames = new ArrayList();

	public static final void generateSkillList( final StringBuffer buffer, final boolean appendHTML )
	{
		ArrayList uncategorized = new ArrayList();
		ArrayList[] categories = new ArrayList[ SkillDatabase.CATEGORIES.length ];

		if ( SkillDatabase.skillNames.isEmpty() )
		{
			SkillDatabase.skillNames.addAll( SkillDatabase.skillByName.keySet() );
		}

		for ( int i = 0; i < categories.length; ++i )
		{
			categories[ i ] = new ArrayList();
			categories[ i ].addAll( (ArrayList) SkillDatabase.skillsByCategory.get( SkillDatabase.CATEGORIES[ i ] ) );

			for ( int j = 0; j < categories[ i ].size(); ++j )
			{
				if ( !KoLConstants.availableSkills.contains( UseSkillRequest.getInstance( (String) categories[ i ].get( j ) ) ) )
				{
					categories[ i ].remove( j-- );
				}
			}
		}

		boolean printedList = false;

		for ( int i = 0; i < categories.length; ++i )
		{
			if ( categories[ i ].isEmpty() )
			{
				continue;
			}

			if ( printedList )
			{
				if ( appendHTML )
				{
					buffer.append( "<br>" );
				}
				else
				{
					buffer.append( KoLConstants.LINE_BREAK );
				}
			}

			SkillDatabase.appendSkillList(
				buffer, appendHTML, SkillDatabase.toTitleCase( SkillDatabase.CATEGORIES[ i ] ),
				categories[ i ] );
			printedList = true;
		}
	}

	private static final void appendSkillList( final StringBuffer buffer, final boolean appendHTML,
		final String listName, final ArrayList list )
	{
		if ( list.isEmpty() )
		{
			return;
		}

		Collections.sort( list );

		if ( appendHTML )
		{
			buffer.append( "<u><b>" );
		}

		buffer.append( SkillDatabase.toTitleCase( listName ) );

		if ( appendHTML )
		{
			buffer.append( "</b></u><br>" );
		}
		else
		{
			buffer.append( KoLConstants.LINE_BREAK );
		}

		String currentSkill;

		for ( int j = 0; j < list.size(); ++j )
		{
			currentSkill = (String) list.get( j );

			if ( appendHTML )
			{
				buffer.append( "<a onClick=\"javascript:skill(" );
				buffer.append( SkillDatabase.getSkillId( currentSkill ) );
				buffer.append( ");\">" );
			}
			else
			{
				buffer.append( " - " );
			}

			buffer.append( currentSkill );

			if ( appendHTML )
			{
				buffer.append( "</a><br>" );
			}
			else
			{
				buffer.append( KoLConstants.LINE_BREAK );
			}
		}
	}
}
