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

import net.java.dev.spellcast.utilities.SortedListModel;
import net.java.dev.spellcast.utilities.LockableListModel;

/**
 * A container class representing the <code>KoLCharacter</code>.  This
 * class also allows for data listeners that are updated whenever the
 * character changes; ultimately, the purpose of this class is to shift
 * away from the centralized-notification paradigm (inefficient) towards
 * a listener paradigm, which is both cleaner and easier to manage with
 * regards to extensions.  In addition, it loosens the coupling between
 * the various aspects of <code>KoLmafia</code>, leading to extensibility.
 */

public class KoLCharacter
{
	private String username;
	private int userID, level;
	private String classname;

	private int currentHP, maximumHP, baseMaxHP;
	private int currentMP, maximumMP, baseMaxMP;

	private int [] adjustedStats;
	private int [] totalSubpoints;

	private LockableListModel equipment;
	private SortedListModel inventory;
	private SortedListModel closet;

	private int availableMeat;

	private int inebriety;
	private int adventuresLeft;
	private int totalTurnsUsed;

	/**
	 * Constructs a new <code>KoLCharacter</code> with the given name.
	 * All fields are initialized to their default values (nothing),
	 * and it is the responsibility of other methods to initialize
	 * the fields with their real values.
	 *
	 * @param	username	The name of the character this <code>KoLCharacter</code> represents
	 */

	public KoLCharacter( String username )
	{
		this.username = username;
		this.classname = "";

		this.adjustedStats = new int[3];
		this.totalSubpoints = new int[3];

		this.equipment = new LockableListModel();
		this.inventory = new SortedListModel( AdventureResult.class );
		this.closet = new SortedListModel( AdventureResult.class );

		for ( int i = 0; i < 7; ++i )
			equipment.add( "none" );
	}

	/**
	 * Accessor method to retrieve the name of this character.
	 * @return	The name of this character
	 */

	public String getUsername()
	{	return username;
	}

	/**
	 * Accessor method to set the user ID associated with this character.
	 * @param	userID	The user ID associated with this character
	 */

	public void setUserID( int userID )
	{	this.userID = userID;
	}

	/**
	 * Accessor method to retrieve the user ID associated with this character.
	 * @return	The user ID associated with this character
	 */

	public int getUserID()
	{	return userID;
	}

	/**
	 * Accessor method to set the level of this character.
	 * @param	level	The level of this character
	 */

	public void setLevel( int level )
	{	this.level = level;
	}

	/**
	 * Accessor method to retrieve the level of this character.
	 * @return	The level of this character
	 */

	public int getLevel()
	{	return level;
	}

	/**
	 * Accessor method to set the character's class.
	 * @param	classname	The name of the character's class
	 */

	public void setClassName( String classname )
	{	this.classname = classname;
	}

	/**
	 * Accessor method to retrieve the name of the character's class.
	 * @return	The name of the character's class
	 */

	public String getClassName()
	{	return classname;
	}

	/**
	 * Accessor method to set the character's current health state.
	 * @param	currentHP	The character's current HP value
	 * @param	maximumHP	The character's maximum HP value
	 * @param	baseMaxHP	The base value for the character's maximum HP
	 */

	public void setHP( int currentHP, int maximumHP, int baseMaxHP )
	{
		this.currentHP = currentHP;
		this.maximumHP = maximumHP;
		this.baseMaxHP = baseMaxHP;
	}

	/**
	 * Accessor method to retrieve the character's current HP.
	 * @return	The character's current HP
	 */

	public int getCurrentHP()
	{	return currentHP;
	}

	/**
	 * Accessor method to retrieve the character's maximum HP.
	 * @return	The character's maximum HP
	 */

	public int getMaximumHP()
	{	return maximumHP;
	}

	/**
	 * Accessor method to retrieve the base value for the character's maximum HP.
	 * @return	The base value for the character's maximum HP
	 */

	public int getBaseMaxHP()
	{	return baseMaxHP;
	}

	/**
	 * Accessor method to set the character's current mana limits.
	 * @param	currentMP	The character's current MP value
	 * @param	maximumMP	The character's maximum MP value
	 * @param	baseMaxMP	The base value for the character's maximum MP
	 */

	public void setMP( int currentMP, int maximumMP, int baseMaxMP )
	{
		this.currentMP = currentMP;
		this.maximumMP = maximumMP;
		this.baseMaxMP = baseMaxMP;
	}

	/**
	 * Accessor method to retrieve the character's current MP.
	 * @return	The character's current MP
	 */

	public int getCurrentMP()
	{	return currentMP;
	}

	/**
	 * Accessor method to retrieve the character's maximum MP.
	 * @return	The character's maximum MP
	 */

	public int getMaximumMP()
	{	return maximumMP;
	}

	/**
	 * Accessor method to retrieve the base value for the character's maximum MP.
	 * @return	The base value for the character's maximum MP
	 */

	public int getBaseMaxMP()
	{	return baseMaxMP;
	}

	/**
	 * Accessor method to set the character's current available meat for spending
	 * (IE: meat that isn't currently in the character's closet).
	 *
	 * @param	availableMeat	The character's available meat for spending
	 */

	public void setAvailableMeat( int availableMeat )
	{	this.availableMeat = availableMeat;
	}

	/**
	 * Accessor method to retrieve the character's current available meat for
	 * spending (IE: meat that isn't currently in the character's closet).
	 *
	 * @return	The character's available meat for spending
	 */

	public int getAvailableMeat()
	{	return availableMeat;
	}

	/**
	 * Sets the character's current stat values.  Each parameter in the list comes in
	 * pairs: the adjusted value (based on equipment and spell effects) and the total
	 * number of subpoints acquired through adventuring for that statistic.  This is
	 * preferred over the character's current base and/or distance from base as it
	 * allows for more accurate reporting of statistic gains and losses, as statistic
	 * losses are not reported by KoL.
	 *
	 * @param	adjustedMuscle	The adjusted value for the character's muscle
	 * @param	totalMuscle	The total number of muscle subpoints acquired thus far
	 * @param	adjustedMysticality	The adjusted value for the character's mysticality
	 * @param	totalMysticality	The total number of mysticality subpoints acquired thus far
	 * @param	adjustedMoxie	The adjusted value for the character's moxie
	 * @param	totalMoxie	The total number of moxie subpoints acquired thus far
	 */

	public void setStats( int adjustedMuscle, int totalMuscle,
		int adjustedMysticality, int totalMysticality, int adjustedMoxie, int totalMoxie )
	{
		adjustedStats[0] = adjustedMuscle;
		adjustedStats[1] = adjustedMysticality;
		adjustedStats[2] = adjustedMoxie;

		totalSubpoints[0] = totalMuscle;
		totalSubpoints[1] = totalMysticality;
		totalSubpoints[2] = totalMoxie;
	}

	/**
	 * Utility method for calculating how many subpoints have been accumulated
	 * thus far, given the current base point value of the statistic and how
	 * many have been accumulate since the last gain.
	 *
	 * @param	baseValue	The current base point value
	 * @param	sinceLastBase	Number of subpoints accumulate since the last base point gain
	 * @return	The total number of subpoints acquired since creation
	 */

	public static int calculateSubpoints( int baseValue, int sinceLastBase )
	{	return baseValue * baseValue + sinceLastBase - 1;
	}

	/**
	 * Accessor method to retrieve the character's adjusted value for muscle.
	 * @return	The character's adjusted value for muscle
	 */

	public int getAdjustedMuscle()
	{	return adjustedStats[0];
	}

	/**
	 * Accessor method to retrieve the character's adjusted value for mysticality.
	 * @return	The character's adjusted value for mysticality
	 */

	public int getAdjustedMysticality()
	{	return adjustedStats[1];
	}

	/**
	 * Accessor method to retrieve the character's adjusted value for moxie.
	 * @return	The character's adjusted value for moxie
	 */

	public int getAdjustedMoxie()
	{	return adjustedStats[2];
	}

	/**
	 * Accessor method to set the character's current inebriety (also known as
	 * drunkenness, tipsiness, and various other names).
	 *
	 * @param	inebriety	The character's current inebriety level
	 */

	public void setInebriety( int inebriety )
	{	this.inebriety = inebriety;
	}

	/**
	 * Accessor method to retrieve the character's current inebriety (also known as
	 * drunkenness, tipsiness, and various other names).
	 *
	 * @return	The character's current inebriety level
	 */

	public int getInebriety()
	{	return inebriety;
	}

	/**
	 * Accessor method to set the number of adventures the character has left to
	 * spend in this session.
	 *
	 * @param	adventuresLeft	The number of adventures the character has left
	 */

	public void setAdventuresLeft( int adventuresLeft )
	{	this.adventuresLeft = adventuresLeft;
	}

	/**
	 * Accessor method to retrieve the number of adventures the character has left
	 * to spend in this session.
	 *
	 * @return	The number of adventures the character has left
	 */

	public int getAdventuresLeft()
	{	return adventuresLeft;
	}

	/**
	 * Accessor method to set the total number of turns the character has used
	 * since creation.  This method is only interesting from an averages point of
	 * view, but sometimes, it's interesting to know.
	 *
	 * @param	totalTurnsUsed	The total number of turns used since creation
	 */

	public void setTotalTurnsUsed( int totalTurnsUsed )
	{	this.totalTurnsUsed = totalTurnsUsed;
	}

	/**
	 * Accessor method to retrieve the total number of turns the character has used
	 * since creation.  This method is only interesting from an averages point of
	 * view, but sometimes, it's interesting to know.
	 *
	 * @return	The total number of turns used since creation
	 */

	public int getTotalTurnsUsed()
	{	return totalTurnsUsed;
	}

	/**
	 * Accessor method to set the equipment the character is currently using.
	 * This does not take into account the power of the item or anything of
	 * that nature; only the item's name is stored.  Note that if no item is
	 * equipped, the value should be <code>none</code>, not <code>null</code>
	 * or the empty string.
	 *
	 * @param	hat	The name of the character's equipped hat
	 * @param	weapon	The name of character's equipped weapon
	 * @param	pants	The name of the character's equipped pants
	 * @param	accessory1	The name of the accessory in the first accessory slot
	 * @param	accessory2	The name of the accessory in the first accessory slot
	 * @param	accessory3	The name of the accessory in the first accessory slot
	 * @param	familiarItem	The name of the item equipped on the character's familiar
	 */

	public void setEquipment( String hat, String weapon, String pants, String accessory1, String accessory2, String accessory3, String familiarItem )
	{
		equipment.set( 0, hat );
		equipment.set( 1, weapon );
		equipment.set( 2, pants );
		equipment.set( 3, accessory1 );
		equipment.set( 4, accessory2 );
		equipment.set( 5, accessory3 );
		equipment.set( 6, familiarItem );
	}

	/**
	 * Accessor method to retrieve the name of the hat the character has equipped.
	 * @return	The name of the character's equipped hat, <code>none</code> if no such item exists
	 */

	public String getHat()
	{	return (String) equipment.get( 0 );
	}

	/**
	 * Accessor method to retrieve the name of the weapon the character has equipped.
	 * @return	The name of the character's equipped weapon, <code>none</code> if no such item exists
	 */

	public String getWeapon()
	{	return (String) equipment.get( 1 );
	}

	/**
	 * Accessor method to retrieve the name of the pants the character has equipped.
	 * @return	The name of the character's equipped pants, <code>none</code> if no such item exists
	 */

	public String getPants()
	{	return (String) equipment.get( 2 );
	}

	/**
	 * Accessor method to retrieve the name of the accessory the character has equipped
	 * in their first accessory slot.
	 *
	 * @return	The name of the accessory in the first accessory slot, <code>none</code> if no such item exists
	 */

	public String getAccessory1()
	{	return (String) equipment.get( 3 );
	}

	/**
	 * Accessor method to retrieve the name of the accessory the character has equipped
	 * in their second accessory slot.
	 *
	 * @return	The name of the accessory in the second accessory slot, <code>none</code> if no such item exists
	 */

	public String getAccessory2()
	{	return (String) equipment.get( 4 );
	}

	/**
	 * Accessor method to retrieve the name of the accessory the character has equipped
	 * in their third accessory slot.
	 *
	 * @return	The name of the accessory in the third accessory slot, <code>none</code> if no such item exists
	 */

	public String getAccessory3()
	{	return (String) equipment.get( 5 );
	}

	/**
	 * Accessor method to retrieve the name of the item equipped on the character's familiar.
	 * @return	The name of the item equipped on the character's familiar, <code>none</code> if no such item exists
	 */

	public String getFamiliarItem()
	{	return (String) equipment.get( 6 );
	}

	/**
	 * Accessor method to retrieve a list of the items contained within the character's inventory.
	 * Note that each of the elements within this list is an <code>AdventureResult</code> object
	 * and that any changes to the internal character inventory will be reflected in the returned
	 * <code>LockableListModel</code>.
	 *
	 * @return	A <code>LockableListModel</code> of the items in the character's inventory
	 */

	public LockableListModel getInventory()
	{	return inventory.getMirrorImage();
	}

	/**
	 * Accessor method to add an item to the character's inventory.  Any mirror images will also
	 * reflect the change if they have not already been modified.
	 *
	 * @param	inventoryItem	The item to add to the character's inventory
	 */

	public void addInventoryItem( AdventureResult inventoryItem )
	{	AdventureResult.addResultToList( inventory, inventoryItem );
	}

	/**
	 * Accessor method to retrieve a list of the items contained within the character's closet.
	 * Note that each of the elements within this list is an <code>AdventureResult</code> object
	 * and that any changes to the internal character closet will be reflected in the returned
	 * <code>LockableListModel</code>.
	 *
	 * @return	A <code>LockableListModel</code> of the items in the character's closet
	 */

	public LockableListModel getCloset()
	{	return closet.getMirrorImage();
	}

	/**
	 * Accessor method to add an item to the character's inventory.  Any mirror images will also
	 * reflect the change if they have not already been modified (modification to mirror images
	 * equals bad news).
	 *
	 * @param	closetItem	The item to add to the character's closet
	 */

	public void addClosetItem( AdventureResult closetItem )
	{	AdventureResult.addResultToList( closet, closetItem );
	}
}