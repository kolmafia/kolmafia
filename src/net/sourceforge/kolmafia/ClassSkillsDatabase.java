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
import java.util.Set;
import java.util.Map;
import java.util.TreeMap;
import java.io.BufferedReader;

/**
 * A static class which retrieves all the tradeable items available in
 * the Kingdom of Loathing and allows the client to do item look-ups.
 * The item list being used is a parsed and resorted list found on
 * Ohayou's Kingdom of Loathing website.  In order to decrease server
 * load, this item list is stored within the JAR archive.
 */

public class ClassSkillsDatabase extends KoLDatabase
{
	private static int manaModifier = 0;

	private static Map skillByID = new TreeMap();
	private static Map skillByName = new TreeMap();
	private static Map mpConsumptionByID = new TreeMap();
	private static Map skillTypeByID = new TreeMap();

	public static final int PASSIVE = 0;
	public static final int SKILL = 1;
	public static final int BUFF = 2;
	public static final int COMBAT = 3;

	static
	{
		// This begins by opening up the data file and preparing
		// a buffered reader; once this is done, every line is
		// examined and double-referenced: once in the name-lookup,
		// and again in the ID lookup.

		BufferedReader reader = getReader( "classskills.dat" );

		String [] data;
		Integer skillID, skillType, mpConsumption;
		String skillName;

		while ( (data = readData( reader )) != null )
		{
			if ( data.length == 4 )
			{
				skillID = Integer.valueOf( data[0] );
				skillType = Integer.valueOf( data[1] );
				mpConsumption = Integer.valueOf( data[2] );
				skillName = getDisplayName( data[3] );

				skillByID.put( skillID, skillName );
				skillByName.put( getCanonicalName( data[3] ), skillID );
				mpConsumptionByID.put( skillID, mpConsumption );
				skillTypeByID.put( skillID, skillType );
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

	/**
	 * Returns a list of all skills which contain the given
	 * substring.  This is useful for people who are doing
	 * lookups on skills.
	 */

	public static final List getMatchingNames( String substring )
	{	return getMatchingNames( skillByName, substring );
	}

	/**
	 * Returns the name for an skill, given its ID.
	 * @param	skillID	The ID of the skill to lookup
	 * @return	The name of the corresponding skill
	 */

	public static final String getSkillName( int skillID )
	{	return (String) skillByID.get( new Integer( skillID ) );
	}

	/**
	 * Returns the type for an skill, given its ID.
	 * @param	skillID	The ID of the skill to lookup
	 * @return	The type of the corresponding skill
	 */

	public static final int getSkillType( int skillID )
	{
		Object skillType = skillTypeByID.get( new Integer( skillID ) );
		return skillType == null ? -1 : ((Integer)skillType).intValue();
	}

	/**
	 * Returns the ID number for an skill, given its name.
	 * @param	skillName	The name of the skill to lookup
	 * @return	The ID number of the corresponding skill
	 */

	public static final int getSkillID( String skillName )
	{
		Object skillID = skillByName.get( getCanonicalName( skillName ) );
		return skillID == null ? -1 : ((Integer)skillID).intValue();
	}

	/**
	 * Returns how much MP is consumed by using the skill
	 * with the given ID.
	 *
	 * @param	skillID	The id of the skill to lookup
	 * @return	The MP consumed by the skill, or 0 if unknown
	 */

	public static final int getMPConsumptionByID( int skillID )
	{
		// Moxious Maneuver has a special mana cost.
		
		if ( skillID == 7008 )
			return Math.max( KoLCharacter.getLevel() + manaModifier, 1 );
		
		Object mpConsumption = mpConsumptionByID.get( new Integer( skillID ) );
		return mpConsumption == null ? 0 : Math.max( ((Integer)mpConsumption).intValue() + manaModifier, 1 );
	}

	/**
	 * Returns whether or not this is a normal skill that can only be
	 * used on the player.
	 *
	 * @return <code>true</code> if the skill is a normal skill
	 */

	public static final boolean isNormal( int skillID )
	{	return isType( skillID, SKILL );
	}

	/**
	 * Returns whether or not the skill is a passive.
	 * @return	<code>true</code> if the skill is passive
	 */

	public static final boolean isPassive( int skillID )
	{	return isType( skillID, PASSIVE );
	}

	/**
	 * Returns whether or not the skill is a buff (ie: can be
	 * used on others).
	 *
	 * @return	<code>true</code> if the skill can target other players
	 */

	public static final boolean isBuff( int skillID )
	{	return isType( skillID, BUFF );
	}

	/**
	 * Returns whether or not the skill is a combat skill (ie: can
	 * be used while fighting).
	 *
	 * @return	<code>true</code> if the skill can be used in combat
	 */

	public static final boolean isCombat( int skillID )
	{	return isType( skillID, COMBAT );
	}

	/**
	 * Utility method used to determine if the given skill is of the
	 * appropriate type.
	 */

	private static final boolean isType( int skillID, int type )
	{
		Object skillType = skillTypeByID.get( new Integer( skillID ) );
		return skillType == null ? false : ((Integer)skillType).intValue() == type;
	}
	
	/**
	 * Returns all skills in the database of the given type.
	 */

	public static final List getSkillsByType( int type )
	{
		ArrayList list = new ArrayList();
		
		Object [] keys = skillTypeByID.keySet().toArray();
		for ( int i = 0; i < keys.length; ++i )
			if ( isType( ((Integer)keys[i]).intValue(), type ) )
				list.add( new UseSkillRequest( client, getSkillName( ((Integer)keys[i]).intValue() ), "", 1 ) );

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

	public static void updateManaModifier()
	{
		manaModifier = 0;

		if ( client == null )
			return;

		int [] accessoryID = new int[3];
		accessoryID[0] = TradeableItemDatabase.getItemID( KoLCharacter.getEquipment( KoLCharacter.ACCESSORY1 ) );
		accessoryID[1] = TradeableItemDatabase.getItemID( KoLCharacter.getEquipment( KoLCharacter.ACCESSORY2 ) );
		accessoryID[2] = TradeableItemDatabase.getItemID( KoLCharacter.getEquipment( KoLCharacter.ACCESSORY3 ) );

		for ( int i = 0; i < 3; ++i )
		{
			switch ( accessoryID[i] )
			{
				case 717:  // baconstone bracelet

					manaModifier -= 1;
					break;

				case 1226: // stainless steel solitaire

					manaModifier -= 2;
					break;

				case 1232: // plexiglass pocketwatch

					manaModifier -= 3;
					break;

				default:
					break;
			}
		}

		// Make sure the modifier is no more than
		// three, no matter what.

		manaModifier = Math.max( manaModifier, -3 );
	}

	/**
	 * Returns the set of skills keyed by name
	 * @return	The set of skills keyed by name
	 */
	public static Set entrySet()
	{	return skillByID.entrySet();
	}
}
