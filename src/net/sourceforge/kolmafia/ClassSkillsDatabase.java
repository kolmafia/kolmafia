/**
 * Copyright (c) 2005-2006, KoLmafia development team
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

import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.Map;
import java.util.TreeMap;
import java.io.BufferedReader;

/**
 * A static class which retrieves all the tradeable items available in
 * the Kingdom of Loathing and allows theto do item look-ups.
 * The item list being used is a parsed and resorted list found on
 * Ohayou's Kingdom of Loathing website.  In order to decrease server
 * load, this item list is stored within the JAR archive.
 */

public class ClassSkillsDatabase extends KoLDatabase
{
	private static Map skillById = new TreeMap();
	private static Map skillByName = new TreeMap();
	private static Map mpConsumptionById = new TreeMap();
	private static Map skillTypeById = new TreeMap();
	private static Map durationById = new TreeMap();

	public static final int PASSIVE = 0;
	public static final int SKILL = 1;
	public static final int BUFF = 2;
	public static final int COMBAT = 3;

	static
	{
		// This begins by opening up the data file and preparing
		// a buffered reader; once this is done, every line is
		// examined and float-referenced: once in the name-lookup,
		// and again in the Id lookup.

		BufferedReader reader = getReader( "classskills.txt" );

		String [] data;
		Integer skillId, skillType, mpConsumption, duration;
		String skillName;

		while ( (data = readData( reader )) != null )
		{
			if ( data.length == 5 )
			{
				skillId = Integer.valueOf( data[0] );
				skillType = Integer.valueOf( data[1] );
				mpConsumption = Integer.valueOf( data[2] );
				duration = Integer.valueOf( data[3] );
				skillName = getDisplayName( data[4] );

				skillById.put( skillId, skillName );
				skillByName.put( getCanonicalName( data[4] ), skillId );
				mpConsumptionById.put( skillId, mpConsumption );
				durationById.put( skillId, duration );
				skillTypeById.put( skillId, skillType );
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

			printStackTrace( e );
		}
	}

	/**
	 * Returns a list of all skills which contain the given
	 * substring.  This is useful for people who are doing
	 * lookups on skills.
	 */

	public static final List getMatchingNames( String substring )
	{	return getMatchingNames( skillByName, substring );
	}

	/**
	 * Returns the name for an skill, given its Id.
	 * @param	skillId	The Id of the skill to lookup
	 * @return	The name of the corresponding skill
	 */

	public static final String getSkillName( int skillId )
	{	return (String) skillById.get( new Integer( skillId ) );
	}

	/**
	 * Returns the type for an skill, given its Id.
	 * @param	skillId	The Id of the skill to lookup
	 * @return	The type of the corresponding skill
	 */

	public static final int getSkillType( int skillId )
	{
		Object skillType = skillTypeById.get( new Integer( skillId ) );
		return skillType == null ? -1 : ((Integer)skillType).intValue();
	}

	/**
	 * Returns the Id number for an skill, given its name.
	 * @param	skillName	The name of the skill to lookup
	 * @return	The Id number of the corresponding skill
	 */

	public static final int getSkillId( String skillName )
	{
		Object skillId = skillByName.get( getCanonicalName( skillName ) );
		return skillId == null ? -1 : ((Integer)skillId).intValue();
	}

	/**
	 * Returns how much MP is consumed by using the skill
	 * with the given Id.
	 *
	 * @param	skillId	The id of the skill to lookup
	 * @return	The MP consumed by the skill, or 0 if unknown
	 */

	public static final int getMPConsumptionById( int skillId )
	{
		// Moxious Maneuver has a special mana cost.
		if ( skillId == 7008 )
			return Math.max( KoLCharacter.getLevel() + KoLCharacter.getManaCostModifier(), 1 );

		// Magic Missile has a special mana cost.
		if ( skillId == 7009 )
			return Math.max( Math.min( ( KoLCharacter.getLevel() + 3 ) / 2, 6 ) + KoLCharacter.getManaCostModifier(), 1 );

		if ( getSkillType( skillId ) == PASSIVE )
			return 0;

		Object mpConsumption = mpConsumptionById.get( new Integer( skillId ) );
		return mpConsumption == null ? 0 : Math.max( ((Integer)mpConsumption).intValue() + KoLCharacter.getManaCostModifier(), 1 );
	}

	/**
	 * Returns how many rounds of buff are gained by using
	 * the skill with the given Id.
	 *
	 * @param	skillId	The id of the skill to lookup
	 * @return	The duration of effect the cast gives
	 */

	public static final int getEffectDuration( int skillId )
	{
		Object duration = durationById.get( new Integer( skillId ) );
		if ( duration == null )
			return 0;

		int actualDuration = ((Integer)duration).intValue();
		if ( actualDuration == 0 || getSkillType( skillId ) != BUFF )
			return actualDuration;

		if ( skillId > 6000 && skillId < 7000 && KoLCharacter.hasItem( UseSkillRequest.ROCKNROLL_LEGEND ) )
			actualDuration += 10;

		if ( KoLCharacter.hasItem( UseSkillRequest.WIZARD_HAT ) )
			actualDuration += 5;

		return actualDuration;
	}

	/**
	 * Returns whether or not this is a normal skill that can only be
	 * used on the player.
	 *
	 * @return <code>true</code> if the skill is a normal skill
	 */

	public static final boolean isNormal( int skillId )
	{	return isType( skillId, SKILL );
	}

	/**
	 * Returns whether or not the skill is a passive.
	 * @return	<code>true</code> if the skill is passive
	 */

	public static final boolean isPassive( int skillId )
	{	return isType( skillId, PASSIVE );
	}

	/**
	 * Returns whether or not the skill is a buff (ie: can be
	 * used on others).
	 *
	 * @return	<code>true</code> if the skill can target other players
	 */

	public static final boolean isBuff( int skillId )
	{	return isType( skillId, BUFF );
	}

	/**
	 * Returns whether or not the skill is a combat skill (ie: can
	 * be used while fighting).
	 *
	 * @return	<code>true</code> if the skill can be used in combat
	 */

	public static final boolean isCombat( int skillId )
	{	return isType( skillId, COMBAT );
	}

	/**
	 * Utility method used to determine if the given skill is of the
	 * appropriate type.
	 */

	private static final boolean isType( int skillId, int type )
	{
		Object skillType = skillTypeById.get( new Integer( skillId ) );
		return skillType == null ? false : ((Integer)skillType).intValue() == type;
	}

	/**
	 * Returns all skills in the database of the given type.
	 */

	public static final List getSkillsByType( int type )
	{
		ArrayList list = new ArrayList();

		Object [] keys = skillTypeById.keySet().toArray();
		for ( int i = 0; i < keys.length; ++i )
			if ( isType( ((Integer)keys[i]).intValue(), type ) )
				list.add( UseSkillRequest.getInstance( ((Integer)keys[i]).intValue() ) );

		return list;
	}

	/**
	 * Returns whether or not an item with a given name
	 * exists in the database; this is useful in the
	 * event that an item is encountered which is not
	 * tradeable (and hence, should not be displayed).
	 *
	 * @return	<code>true</code> if the item is in the database
	 */

	public static final boolean contains( String skillName )
	{	return skillByName.containsKey( getCanonicalName( skillName ) );
	}

	/**
	 * Returns the set of skills keyed by name
	 * @return	The set of skills keyed by name
	 */
	public static Set entrySet()
	{	return skillById.entrySet();
	}
}
