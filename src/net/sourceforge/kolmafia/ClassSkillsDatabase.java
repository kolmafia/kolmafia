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

import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class ClassSkillsDatabase
	extends KoLDatabase
{
	private static final Map skillById = new TreeMap();
	private static final Map skillByName = new TreeMap();
	private static final Map mpConsumptionById = new TreeMap();
	private static final Map skillTypeById = new TreeMap();
	private static final Map durationById = new TreeMap();

	private static final Map skillsByCategory = new TreeMap();
	private static final Map skillCategoryById = new TreeMap();

	public static final int CASTABLE = -1;
	public static final int PASSIVE = 0;
	public static final int SUMMON = 1;
	public static final int REMEDY = 2;
	public static final int SELF_ONLY = 3;
	public static final int BUFF = 4;
	public static final int COMBAT = 5;

	// Mr. Skills
	public static final int SNOWCONE = 16;
	public static final int HILARIOUS = 17;
	public static final int CANDY_HEART = 18;
	public static final int PARTY_FAVOR = -100;

	private static final String UNCATEGORIZED = "uncategorized";
	private static final String GNOME_SKILLS = "gnome trainer";
	private static final String BAD_MOON = "bad moon";
	private static final String MR_SKILLS = "mr. skills";

	private static final String[] CATEGORIES = new String[]
	{	
		ClassSkillsDatabase.UNCATEGORIZED,
		"seal clubber",
		"turtle tamer",
		"pastamancer",
		"sauceror",
		"disco bandit",
		"accordion thief",
		ClassSkillsDatabase.GNOME_SKILLS,
		ClassSkillsDatabase.MR_SKILLS,
		ClassSkillsDatabase.BAD_MOON
	};

	static
	{
		// This begins by opening up the data file and preparing
		// a buffered reader; once this is done, every line is
		// examined and float-referenced: once in the name-lookup,
		// and again in the Id lookup.

		for ( int i = 0; i < ClassSkillsDatabase.CATEGORIES.length; ++i )
		{
			ClassSkillsDatabase.skillsByCategory.put( ClassSkillsDatabase.CATEGORIES[ i ], new ArrayList() );
		}

		BufferedReader reader = KoLDatabase.getVersionedReader( "classskills.txt", KoLConstants.CLASSSKILLS_VERSION );

		String[] data;

		while ( ( data = KoLDatabase.readData( reader ) ) != null )
		{
			if ( data.length == 5 )
			{
				ClassSkillsDatabase.addSkill(
					Integer.valueOf( data[ 0 ] ), KoLDatabase.getDisplayName( data[ 1 ] ),
					Integer.valueOf( data[ 2 ] ), Integer.valueOf( data[ 3 ] ), Integer.valueOf( data[ 4 ] ) );
			}
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
	}

	private static final void addSkill( final Integer skillId, final String skillName, final Integer skillType,
		final Integer mpConsumption, final Integer duration )
	{
		ClassSkillsDatabase.skillById.put( skillId, skillName );
		ClassSkillsDatabase.skillByName.put( KoLDatabase.getCanonicalName( skillName ), skillId );

		ClassSkillsDatabase.skillTypeById.put( skillId, skillType );

		ClassSkillsDatabase.mpConsumptionById.put( skillId, mpConsumption );
		ClassSkillsDatabase.durationById.put( skillId, duration );

		String category;
		int categoryId = skillId.intValue() / 1000;

		switch ( skillId.intValue() )
		{
		case 3:			// Smile of Mr. A
		case SNOWCONE:		// Summon Snowcone
		case HILARIOUS:		// Summon Hilarious Objects
		case CANDY_HEART:	// Summon Candy Hearts
		case PARTY_FAVOR:	// Summon Party Favor

			category = ClassSkillsDatabase.MR_SKILLS;
			break;

		case 10: // Powers of Observatiogn
		case 11: // Gnefarious Pickpocketing
		case 12: // Torso Awaregness
		case 13: // Gnomish Hardigness
		case 14: // Cosmic Ugnderstanding

			category = ClassSkillsDatabase.GNOME_SKILLS;
			break;

		case 21: // Lust
		case 22: // Gluttony
		case 23: // Greed
		case 24: // Sloth
		case 25: // Wrath
		case 26: // Envy
		case 27: // Pride

			category = ClassSkillsDatabase.BAD_MOON;
			break;

		default:

			// Moxious maneuver has a 7000 id, but
			// it's not gained by equipment.

			if ( categoryId == 7 )
			{
				category = ClassSkillsDatabase.UNCATEGORIZED;
			}
			else
			{
				category = ClassSkillsDatabase.CATEGORIES[ categoryId ];
			}
		}

		ClassSkillsDatabase.skillCategoryById.put( skillId, category );
		( (ArrayList) ClassSkillsDatabase.skillsByCategory.get( category ) ).add( skillName );
	}

	public static final List getSkillsByCategory( String category )
	{
		if ( category == null )
		{
			return new ArrayList();
		}

		category = category.trim().toLowerCase();
		for ( int i = 0; i < ClassSkillsDatabase.CATEGORIES.length; ++i )
		{
			if ( KoLDatabase.substringMatches( ClassSkillsDatabase.CATEGORIES[ i ], category ) )
			{
				category = ClassSkillsDatabase.CATEGORIES[ i ];
			}
		}

		List skills = (List) ClassSkillsDatabase.skillsByCategory.get( category );
		return skills == null ? new ArrayList() : skills;
	}

	/**
	 * Returns a list of all skills which contain the given substring. This is useful for people who are doing lookups
	 * on skills.
	 */

	public static final List getMatchingNames( final String substring )
	{
		return KoLDatabase.getMatchingNames( ClassSkillsDatabase.skillByName, substring );
	}

	/**
	 * Returns the name for an skill, given its Id.
	 *
	 * @param skillId The Id of the skill to lookup
	 * @return The name of the corresponding skill
	 */

	public static final String getSkillName( final int skillId )
	{
		return (String) ClassSkillsDatabase.skillById.get( new Integer( skillId ) );
	}

	/**
	 * Returns the type for an skill, given its Id.
	 *
	 * @param skillId The Id of the skill to lookup
	 * @return The type of the corresponding skill
	 */

	public static final int getSkillType( final int skillId )
	{
		Object skillType = ClassSkillsDatabase.skillTypeById.get( new Integer( skillId ) );
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
		Object skillId = ClassSkillsDatabase.skillByName.get( KoLDatabase.getCanonicalName( skillName ) );
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
		return ClassSkillsDatabase.getMPConsumptionById( skillId, false );
	}

	public static final int getMPConsumptionById( final int skillId, final boolean justCast )
	{
		// Libram skills have a special cost shared by all
		if ( isLibramSkill( skillId ) )
		{
			return libramSkillMPConsumption( justCast );
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

		if ( ClassSkillsDatabase.getSkillType( skillId ) == ClassSkillsDatabase.PASSIVE )
		{
			return 0;
		}

		Object mpConsumption = ClassSkillsDatabase.mpConsumptionById.get( new Integer( skillId ) );
		return mpConsumption == null ? 0 : Math.max(
			( (Integer) mpConsumption ).intValue() + KoLCharacter.getManaCostAdjustment(), 1 );
	}

	/**
	 * Determines if a skill comes from a Libram
	 *
	 * @param skillId The Id of the skill to lookup
	 * @return true if it comes from a Libram
	 */

	public static final boolean isLibramSkill( final int skillId )
	{
		return skillId == CANDY_HEART || skillId == PARTY_FAVOR;
	}

	/**
	 * Determines the cost for next casting of a libram skill
	 *
	 * @param justCast true if just cast this skill
	 * @return the MP cost to cast it
	 */

	public static final int libramSkillMPConsumption( final boolean justCast )
	{
		int count = KoLSettings.getIntegerProperty( "candyHeartSummons" ) - ( justCast ? 1 : 0 );
		return libramSkillMPConsumption( count ) ;
	}

	public static final int libramSkillMPConsumption()
	{
		return libramSkillMPConsumption( false );
	}

	/**
	 * Determines the cost for a specific casting of a libram skill
	 *
	 * @param count which casting
	 * @return the MP cost to cast it
	 */

	public static final int libramSkillMPConsumption( final int count )
	{
		// Old formula: n * (n+1) / 2
		// return Math.max( ( count + 1 ) * ( count + 2 ) / 2 + KoLCharacter.getManaCostAdjustment(), 1 );

		// New formula: 1 + (n * (n-1) / 2)
		return Math.max( 1 + ( count + 1 ) * count / 2 + KoLCharacter.getManaCostAdjustment(), 1 );
	}

	/**
	 * Returns how many rounds of buff are gained by using the skill with the given Id.
	 *
	 * @param skillId The id of the skill to lookup
	 * @return The duration of effect the cast gives
	 */

	public static final int getEffectDuration( final int skillId )
	{
		Object duration = ClassSkillsDatabase.durationById.get( new Integer( skillId ) );
		if ( duration == null )
		{
			return 0;
		}

		int actualDuration = ( (Integer) duration ).intValue();
		if ( actualDuration == 0 || ClassSkillsDatabase.getSkillType( skillId ) != ClassSkillsDatabase.BUFF )
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

		if ( KoLCharacter.hasItem( UseSkillRequest.WIZARD_HAT ) )
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
		Object skillType = ClassSkillsDatabase.skillTypeById.get( new Integer( skillId ) );
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
		return ClassSkillsDatabase.isType( skillId, ClassSkillsDatabase.PASSIVE );
	}

	/**
	 * Returns whether or not the skill is a buff (ie: can be used on others).
	 *
	 * @return <code>true</code> if the skill can target other players
	 */

	public static final boolean isBuff( final int skillId )
	{
		return ClassSkillsDatabase.isType( skillId, ClassSkillsDatabase.BUFF );
	}

	/**
	 * Returns whether or not the skill is a combat skill (ie: can be used while fighting).
	 *
	 * @return <code>true</code> if the skill can be used in combat
	 */

	public static final boolean isCombat( final int skillId )
	{
		return ClassSkillsDatabase.isType( skillId, ClassSkillsDatabase.COMBAT );
	}

	/**
	 * Utility method used to determine if the given skill is of the appropriate type.
	 */

	private static final boolean isType( final int skillId, final int type )
	{
		Object skillType = ClassSkillsDatabase.skillTypeById.get( new Integer( skillId ) );
		return skillType == null ? false : ( (Integer) skillType ).intValue() == type;
	}

	/**
	 * Returns all skills in the database of the given type.
	 */

	public static final List getSkillsByType( final int type )
	{
		ArrayList list = new ArrayList();

		boolean shouldAdd = false;
		Object[] keys = ClassSkillsDatabase.skillTypeById.keySet().toArray();

		for ( int i = 0; i < keys.length; ++i )
		{
			shouldAdd = false;

			Object id = keys[ i ];
			Object value = ClassSkillsDatabase.skillTypeById.get( id );
			if ( value == null )
				continue;
			int skillType = ( (Integer) value ).intValue();
			int skillId = ( (Integer) id ).intValue();

			if ( type == ClassSkillsDatabase.CASTABLE )
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
		return ClassSkillsDatabase.skillByName.containsKey( KoLDatabase.getCanonicalName( skillName ) );
	}

	/**
	 * Returns the set of skills keyed by name
	 *
	 * @return The set of skills keyed by name
	 */

	public static final Set entrySet()
	{
		return ClassSkillsDatabase.skillById.entrySet();
	}

	private static final ArrayList skillNames = new ArrayList();

	public static final void generateSkillList( final StringBuffer buffer, final boolean appendHTML )
	{
		ArrayList uncategorized = new ArrayList();
		ArrayList[] categories = new ArrayList[ ClassSkillsDatabase.CATEGORIES.length ];

		if ( ClassSkillsDatabase.skillNames.isEmpty() )
		{
			ClassSkillsDatabase.skillNames.addAll( ClassSkillsDatabase.skillByName.keySet() );
		}

		for ( int i = 0; i < categories.length; ++i )
		{
			categories[ i ] = new ArrayList();
			categories[ i ].addAll( (ArrayList) ClassSkillsDatabase.skillsByCategory.get( ClassSkillsDatabase.CATEGORIES[ i ] ) );

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

			ClassSkillsDatabase.appendSkillList(
				buffer, appendHTML, ClassSkillsDatabase.toTitleCase( ClassSkillsDatabase.CATEGORIES[ i ] ),
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

		buffer.append( ClassSkillsDatabase.toTitleCase( listName ) );

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
				buffer.append( ClassSkillsDatabase.getSkillId( currentSkill ) );
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
