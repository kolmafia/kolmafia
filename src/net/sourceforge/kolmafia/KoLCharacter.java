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
	private static List SEAL_CLUBBER = new ArrayList();
	static
	{
		SEAL_CLUBBER.add( "Lemming Trampler" );
		SEAL_CLUBBER.add( "Tern Slapper" );
		SEAL_CLUBBER.add( "Puffin Intimidator" );
		SEAL_CLUBBER.add( "Ermine Thumper" );
		SEAL_CLUBBER.add( "Penguin Frightener" );
		SEAL_CLUBBER.add( "Malamute Basher" );
		SEAL_CLUBBER.add( "Narwhal Pummeler" );
		SEAL_CLUBBER.add( "Otter Crusher" );
		SEAL_CLUBBER.add( "Caribou Smacker" );
		SEAL_CLUBBER.add( "Moose Harasser" );
		SEAL_CLUBBER.add( "Reindeer Threatener" );
		SEAL_CLUBBER.add( "Ox Wrestler" );
		SEAL_CLUBBER.add( "Walrus Bludgeoner" );
		SEAL_CLUBBER.add( "Whale Boxer" );
		SEAL_CLUBBER.add( "Seal Clubber" );
	}

	private static List TURTLE_TAMER = new ArrayList();
	static
	{
		TURTLE_TAMER.add( "Toad Coach" );
		TURTLE_TAMER.add( "Skink Trainer" );
		TURTLE_TAMER.add( "Frog Director" );
		TURTLE_TAMER.add( "Gecko Supervisor" );
		TURTLE_TAMER.add( "Newt Herder" );
		TURTLE_TAMER.add( "Frog Boss" );
		TURTLE_TAMER.add( "Iguana Driver" );
		TURTLE_TAMER.add( "Salamander Subduer" );
		TURTLE_TAMER.add( "Bullfrog Overseer" );
		TURTLE_TAMER.add( "Rattlesnake Chief" );
		TURTLE_TAMER.add( "Crocodile Lord" );
		TURTLE_TAMER.add( "Cobra Commander" );
		TURTLE_TAMER.add( "Alligator Subjugator" );
		TURTLE_TAMER.add( "Asp Master" );
		TURTLE_TAMER.add( "Turtle Tamer" );
	}

	private static List PASTAMANCER = new ArrayList();
	static
	{
		PASTAMANCER.add( "Dough Acolyte" );
		PASTAMANCER.add( "Yeast Scholar" );
		PASTAMANCER.add( "Noodle Neophyte" );
		PASTAMANCER.add( "Starch Savant" );
		PASTAMANCER.add( "Carbohydrate Cognoscenti" );
		PASTAMANCER.add( "Spaghetti Sage" );
		PASTAMANCER.add( "Macaroni Magician" );
		PASTAMANCER.add( "Vermicelli Enchanter" );
		PASTAMANCER.add( "Linguini Thaumaturge" );
		PASTAMANCER.add( "Ravioli Sorcerer" );
		PASTAMANCER.add( "Manicotti Magus" );
		PASTAMANCER.add( "Spaghetti Spellbinder" );
		PASTAMANCER.add( "Canneloni Conjurer" );
		PASTAMANCER.add( "Angel-Hair Archmage" );
		PASTAMANCER.add( "Pastamancer" );
	}

	private static List SAUCEROR = new ArrayList();
	static
	{
		SAUCEROR.add( "Allspice Acolyte" );
		SAUCEROR.add( "Cilantro Seer" );
		SAUCEROR.add( "Parsley Enchanter" );
		SAUCEROR.add( "Sage Sage" );
		SAUCEROR.add( "Rosemary Diviner" );
		SAUCEROR.add( "Thyme Wizard" );
		SAUCEROR.add( "Tarragon Thaumaturge" );
		SAUCEROR.add( "Oreganoccultist" );
		SAUCEROR.add( "Basillusionist" );
		SAUCEROR.add( "Coriander Conjurer" );
		SAUCEROR.add( "Bay Leaf Brujo" );
		SAUCEROR.add( "Sesame Soothsayer" );
		SAUCEROR.add( "Marinara Mage" );
		SAUCEROR.add( "Alfredo Archmage" );
		SAUCEROR.add( "Sauceror" );
	}

	private static List DISCO_BANDIT = new ArrayList();
	static
	{
		DISCO_BANDIT.add( "Funk Footpad" );
		DISCO_BANDIT.add( "Rhythm Rogue" );
		DISCO_BANDIT.add( "Chill Crook" );
		DISCO_BANDIT.add( "Jiggy Grifter" );
		DISCO_BANDIT.add( "Beat Snatcher" );
		DISCO_BANDIT.add( "Sample Swindler" );
		DISCO_BANDIT.add( "Move Buster" );
		DISCO_BANDIT.add( "Jam Horker" );
		DISCO_BANDIT.add( "Groove Filcher" );
		DISCO_BANDIT.add( "Vibe Robber" );
		DISCO_BANDIT.add( "Boogie Brigand" );
		DISCO_BANDIT.add( "Flow Purloiner" );
		DISCO_BANDIT.add( "Jive Pillager" );
		DISCO_BANDIT.add( "Rhymer and Stealer" );
		DISCO_BANDIT.add( "Disco Bandit" );
	}

	private static List ACCORDION_THIEF = new ArrayList();
	static
	{
		ACCORDION_THIEF.add( "Polka Criminal" );
		ACCORDION_THIEF.add( "Mariachi Larcenist" );
		ACCORDION_THIEF.add( "Zydeco Rogue" );
		ACCORDION_THIEF.add( "Chord Horker" );
		ACCORDION_THIEF.add( "Chromatic Crook" );
		ACCORDION_THIEF.add( "Squeezebox Scoundrel" );
		ACCORDION_THIEF.add( "Concertina Con Artist" );
		ACCORDION_THIEF.add( "Button Box Burglar" );
		ACCORDION_THIEF.add( "Hurdy-Gurdy Hooligan" );
		ACCORDION_THIEF.add( "Sub-Sub-Apprentice Accordion Thief" );
		ACCORDION_THIEF.add( "Sub-Apprentice Accordion Thief" );
		ACCORDION_THIEF.add( "Pseudo-Apprentice Accordion Thief" );
		ACCORDION_THIEF.add( "Hemi-Apprentice Accordion Thief" );
		ACCORDION_THIEF.add( "Apprentice Accordion Thief" );
		ACCORDION_THIEF.add( "Accordion Thief" );
	}

	private String username;
	private boolean isMale;
	private int userID;
	private String classname, classtype;

	private int currentHP, maximumHP, baseMaxHP;
	private int currentMP, maximumMP, baseMaxMP;

	private int [] adjustedStats;
	private int [] totalSubpoints;

	private LockableListModel equipment;
	private LockableListModel outfits;

	private SortedListModel inventory;
	private SortedListModel closet;
	private SortedListModel collection;

	private LockableListModel activeEffects;

	private LockableListModel usableSkills;
	private LockableListModel availableSkills;

	public static final int HAT = 0;
	public static final int WEAPON = 1;
	public static final int SHIRT = 2;
	public static final int PANTS = 3;
	public static final int ACCESSORY1 = 4;
	public static final int ACCESSORY2 = 5;
	public static final int ACCESSORY3 = 6;
	public static final int FAMILIAR = 7;

	private LockableListModel [] equipmentLists;

	private int availableMeat;
	private int closetMeat;

	private int inebriety;
	private int adventuresLeft;
	private int totalTurnsUsed;

	private boolean hasToaster;
	private boolean hasArches;
	private boolean hasChef;
	private boolean hasBartender;

	private SortedListModel familiars;
	private FamiliarData currentFamiliar;
	private List listenerList;

	private boolean canInteract;
	private int ascensions;
	private String ascensionSign;
	private int ascensionSignType;

	public static final int NONE = 0;
	public static final int MUSCLE = 1;
	public static final int MYSTICALITY = 2;
	public static final int MOXIE = 3;

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
		this.classtype = "";

		this.adjustedStats = new int[3];
		this.totalSubpoints = new int[3];

		this.equipment = new LockableListModel();
		this.outfits = new LockableListModel();

		this.inventory = new SortedListModel( AdventureResult.class );
		this.closet = new SortedListModel( AdventureResult.class );
		this.collection = new SortedListModel( AdventureResult.class );

		this.activeEffects = new LockableListModel();

		this.usableSkills = new LockableListModel();
		this.availableSkills = new LockableListModel();

		for ( int i = 0; i < 7; ++i )
			equipment.add( EquipmentRequest.UNEQUIP );

		this.canInteract = true;
		this.hasToaster = false;
		this.hasArches = false;
		this.hasChef = false;
		this.hasBartender = false;

		this.familiars = new SortedListModel( FamiliarData.class );
		this.familiars.add( FamiliarData.NO_FAMILIAR );
		this.listenerList = new ArrayList();

		this.ascensions = 0;
		this.ascensionSign = "None";
		this.ascensionSignType = NONE;

		// Initialize the equipment lists inside
		// of the character data

		equipmentLists = new LockableListModel[8];
		for ( int i = 0; i < 8; ++i )
			equipmentLists[i] = new SortedListModel();

		updateEquipmentLists();
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
	 * Accessor method to get the gender associated with this character.
	 * @param	isMale	The gender of this character
	 */

	public void setGender( boolean isMale )
	{	this.isMale = isMale;
	}

	/**
	 * Accessor method to get the gender associated with this character.
	 * @return	<code>true</code> if the player is male
	 */

	public boolean isMale()
	{	return isMale;
	}

	/**
	 * Accessor method to retrieve the level of this character.
	 * @return	The level of this character
	 */

	public int getLevel()
	{
		int currentPrime = classtype.startsWith( "Se" ) || classtype.startsWith( "Tu" ) ? calculateBasePoints( totalSubpoints[0] ) :
			classtype.startsWith( "Sa" ) || classtype.startsWith( "Pa" ) ? calculateBasePoints( totalSubpoints[1] ) :
				calculateBasePoints( totalSubpoints[2] );

		return (int) Math.sqrt( currentPrime - 4 ) + 1;
	}


	/**
	 * Accessor method to set the character's class.
	 * @param	classname	The name of the character's class
	 */

	public void setClassName( String classname )
	{
		this.classname = classname;

		if ( SEAL_CLUBBER.contains( classname ) )
			classtype = "Seal Clubber";
		else if ( TURTLE_TAMER.contains( classname ) )
			classtype = "Turtle Tamer";
		else if ( PASTAMANCER.contains( classname ) )
			classtype = "Pastamancer";
		else if ( SAUCEROR.contains( classname ) )
			classtype = "Sauceror";
		else if ( DISCO_BANDIT.contains( classname ) )
			classtype = "Disco Bandit";
		else if ( ACCORDION_THIEF.contains( classname ) )
			classtype = "Accordion Thief";
		else
			classtype = "";
	}

	/**
	 * Accessor method to retrieve the name of the character's class.
	 * @return	The name of the character's class
	 */

	public String getClassName()
	{	return classname;
	}

	/**
	 * Accessor method to retrieve the type of the character's class.
	 * @return	The type of the character's class
	 */

	public String getClassType()
	{	return classtype;
	}

	/**
	 * Accessor method to set the character's current health state.
	 * @param	currentHP	The character's current HP value
	 * @param	maximumHP	The character's maximum HP value
	 * @param	baseMaxHP	The base value for the character's maximum HP
	 */

	public void setHP( int currentHP, int maximumHP, int baseMaxHP )
	{
		this.currentHP = currentHP < 0 ? 0 :currentHP > maximumHP ? maximumHP : currentHP;
		this.maximumHP = maximumHP;
		this.baseMaxHP = baseMaxHP;

		Iterator listenerIterator = listenerList.iterator();
		while ( listenerIterator.hasNext() )
			((KoLCharacterListener)listenerIterator.next()).hpChanged();
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
		this.currentMP = currentMP < 0 ? 0 : currentMP > maximumMP ? maximumMP : currentMP;
		this.maximumMP = maximumMP;
		this.baseMaxMP = baseMaxMP;

		Iterator listenerIterator = listenerList.iterator();
		while ( listenerIterator.hasNext() )
			((KoLCharacterListener)listenerIterator.next()).mpChanged();
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
	 * Accessor method to set the amount of meat in the character's closet.
	 * @param	closetMeat	The amount of meat in the character's closet.
	 */

	public void setClosetMeat( int closetMeat )
	{
		this.closetMeat = closetMeat;

		Iterator listenerIterator = listenerList.iterator();
		while ( listenerIterator.hasNext() )
			((KoLCharacterListener)listenerIterator.next()).closetMeatChanged();
	}

	/**
	 * Accessor method to retrieve the amount of meat in the character's closet.
	 * @return	The amount of meat in the character's closet.
	 */

	public int getClosetMeat()
	{	return closetMeat;
	}

	/**
	 * Accessor method to set the character's current available meat for spending
	 * (IE: meat that isn't currently in the character's closet).
	 *
	 * @param	availableMeat	The character's available meat for spending
	 */

	public void setAvailableMeat( int availableMeat )
	{
		this.availableMeat = availableMeat;

		Iterator listenerIterator = listenerList.iterator();
		while ( listenerIterator.hasNext() )
			((KoLCharacterListener)listenerIterator.next()).availableMeatChanged();
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

	public void setStatPoints( int adjustedMuscle, int totalMuscle,
		int adjustedMysticality, int totalMysticality, int adjustedMoxie, int totalMoxie )
	{
		adjustedStats[0] = adjustedMuscle;
		adjustedStats[1] = adjustedMysticality;
		adjustedStats[2] = adjustedMoxie;

		totalSubpoints[0] = totalMuscle;
		totalSubpoints[1] = totalMysticality;
		totalSubpoints[2] = totalMoxie;

		Iterator listenerIterator = listenerList.iterator();
		while ( listenerIterator.hasNext() )
			((KoLCharacterListener)listenerIterator.next()).statusPointsChanged();
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
	{	return baseValue * baseValue - 1 + sinceLastBase;
	}

	/**
	 * Utility method for calculating how many actual points are associated
	 * with the given number of subpoints.
	 *
	 * @param	totalSubpoints	The total number of subpoints accumulated
	 * @return	The base points associated with the subpoint value
	 */

	public static int calculateBasePoints( int totalSubpoints )
	{	return (int) Math.floor( Math.sqrt( totalSubpoints + 1 ) );
	}

	/**
	 * Utility method to calculate the "till next point" value, given
	 * the total number of subpoints accumulated.
	 */

	private static int calculateTillNextPoint( int totalSubpoints )
	{
		int basePoints = calculateBasePoints( totalSubpoints ) + 1;
		return basePoints * basePoints - totalSubpoints - 1;
	}

	/**
	 * Accessor method to retrieve the character's base value for muscle.
	 * @return	The character's base value for muscle
	 */

	public int getBaseMuscle()
	{	return calculateBasePoints( totalSubpoints[0] );
	}

	/**
	 * Accessor method to retrieve the total subpoints accumulted so far
	 * in muscle.
	 *
	 * @return	The total muscle subpoints so far
	 */

	public int getTotalMuscle()
	{	return totalSubpoints[0];
	}

	/**
	 * Accessor method to retrieve the number of subpoints required
	 * before the character gains another full point of muscle.
	 */

	public int getMuscleTNP()
	{	return calculateTillNextPoint( totalSubpoints[0] );
	}

	/**
	 * Accessor method to retrieve the character's adjusted value for muscle.
	 * @return	The character's adjusted value for muscle
	 */

	public int getAdjustedMuscle()
	{	return adjustedStats[0];
	}

	/**
	 * Accessor method to retrieve the character's base value for mysticality.
	 * @return	The character's base value for muscle
	 */

	public int getBaseMysticality()
	{	return calculateBasePoints( totalSubpoints[1] );
	}

	/**
	 * Accessor method to retrieve the total subpoints accumulted so far
	 * in mysticality.
	 *
	 * @return	The total mysticality subpoints so far
	 */

	public int getTotalMysticality()
	{	return totalSubpoints[1];
	}

	/**
	 * Accessor method to retrieve the number of subpoints required
	 * before the character gains another full point of mysticality.
	 */

	public int getMysticalityTNP()
	{	return calculateTillNextPoint( totalSubpoints[1] );
	}

	/**
	 * Accessor method to retrieve the character's adjusted value for mysticality.
	 * @return	The character's adjusted value for mysticality
	 */

	public int getAdjustedMysticality()
	{	return adjustedStats[1];
	}

	/**
	 * Accessor method to retrieve the character's base value for moxie.
	 * @return	The character's base value for moxie
	 */

	public int getBaseMoxie()
	{	return calculateBasePoints( totalSubpoints[2] );
	}

	/**
	 * Accessor method to retrieve the total subpoints accumulted so far
	 * in moxie.
	 *
	 * @return	The total moxie subpoints so far
	 */

	public int getTotalMoxie()
	{	return totalSubpoints[2];
	}

	/**
	 * Accessor method to retrieve the number of subpoints required
	 * before the character gains another full point of moxie.
	 */

	public int getMoxieTNP()
	{	return calculateTillNextPoint( totalSubpoints[2] );
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
	{
		this.inebriety = inebriety;
		Iterator listenerIterator = listenerList.iterator();
		while ( listenerIterator.hasNext() )
			((KoLCharacterListener)listenerIterator.next()).inebrietyChanged();
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
	{
		this.adventuresLeft = adventuresLeft;
		Iterator listenerIterator = listenerList.iterator();
		while ( listenerIterator.hasNext() )
			((KoLCharacterListener)listenerIterator.next()).adventuresLeftChanged();
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
	{
		this.totalTurnsUsed = totalTurnsUsed;
		Iterator listenerIterator = listenerList.iterator();
		while ( listenerIterator.hasNext() )
			((KoLCharacterListener)listenerIterator.next()).totalTurnsChanged();
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
	 * @param	equipment	All of the available equipment, stored in an array index by the constants
	 * @param	outfits	A listing of available outfits
	 */

	public void setEquipment( String [] equipment, List outfits )
	{
		for ( int i = 0; i < this.equipment.size(); ++i )
		{
			if ( i == FAMILIAR )
				continue;

			if ( equipment[i] == null || equipment[i].equals( "none" ) || equipment[i].equals( EquipmentRequest.UNEQUIP ) )
				this.equipment.set( i, EquipmentRequest.UNEQUIP );
			else if ( TradeableItemDatabase.getConsumptionType( equipment[i] ) == ConsumeItemRequest.EQUIP_ACCESSORY )
				this.equipment.set( i, equipment[i].toLowerCase() );
			else
				this.equipment.set( i, equipment[i].toLowerCase() + " (+" + EquipmentDatabase.getPower( equipment[i] ) + ")" );
		}

		if ( equipment.length > FAMILIAR && currentFamiliar != null )
			currentFamiliar.setItem( equipment[FAMILIAR].toLowerCase() );

		if ( !outfits.isEmpty() )
			this.outfits.clear();

		if ( this.outfits.isEmpty() )
			this.outfits.add( SpecialOutfit.BIRTHDAY_SUIT );

		this.outfits.addAll( outfits );
		FamiliarData.updateWeightModifier();
	}

	/**
	 * Accessor method to retrieve the name of the item equipped on the character's familiar.
	 * @return	The name of the item equipped on the character's familiar, <code>none</code> if no such item exists
	 */

	public String getFamiliarItem()
	{	return currentFamiliar == null ? EquipmentRequest.UNEQUIP : currentFamiliar.getItem();
	}

	/**
	 * Accessor method to retrieve the name of a piece of equipment
	 * @param	type	the type of equipment
	 * @return	The name of the equipment, <code>none</code> if no such item exists
	 */

	public String getEquipment( int type )
	{
		if ( type >= HAT && type < FAMILIAR )
			return (String) equipment.get( type );

		if ( type == FAMILIAR )
			return getFamiliarItem();

		return EquipmentRequest.UNEQUIP;
	}

	/**
	 * Accessor method to retrieve a list of all available items which can be equipped
	 * by familiars.  Note this lists items which the current familiar cannot equip.
	 */

	public LockableListModel [] getEquipmentLists()
	{	return equipmentLists;
	}

	public void updateEquipmentLists()
	{
		updateEquipmentList( equipmentLists[HAT], ConsumeItemRequest.EQUIP_HAT, getEquipment( HAT ) );
		updateEquipmentList( equipmentLists[WEAPON], ConsumeItemRequest.EQUIP_WEAPON, getEquipment( WEAPON ) );
		updateEquipmentList( equipmentLists[SHIRT], ConsumeItemRequest.EQUIP_SHIRT, getEquipment( SHIRT ) );
		updateEquipmentList( equipmentLists[PANTS], ConsumeItemRequest.EQUIP_PANTS, getEquipment( PANTS ) );
		updateEquipmentList( equipmentLists[ACCESSORY1], ConsumeItemRequest.EQUIP_ACCESSORY, getEquipment( ACCESSORY1 ) );
		updateEquipmentList( equipmentLists[ACCESSORY2], ConsumeItemRequest.EQUIP_ACCESSORY, getEquipment( ACCESSORY2 ) );
		updateEquipmentList( equipmentLists[ACCESSORY3], ConsumeItemRequest.EQUIP_ACCESSORY, getEquipment( ACCESSORY3 ) );
		updateEquipmentList( equipmentLists[FAMILIAR], ConsumeItemRequest.EQUIP_FAMILIAR, getEquipment( FAMILIAR ) );
	}

	public void updateEquipmentList( LockableListModel currentList, int currentFilter, String equippedItem )
	{
		currentList.setSelectedItem( null );
		currentList.clear();
		currentList.addAll( getFilteredItems( currentFilter, equippedItem ) );
		currentList.setSelectedItem( equippedItem );
	}

	private List getFilteredItems( int filterID, String equippedItem )
	{
		String currentItem;
		List items = new ArrayList();

		// If we are looking for familiar items, but we don't
		// have a familiar, then no familiar items can actually
		// be equipped.  So, return the blank list now.

		if ( filterID == ConsumeItemRequest.EQUIP_FAMILIAR && currentFamiliar == null )
		{
			items.add( EquipmentRequest.UNEQUIP );
			return items;
		}

		for ( int i = 0; i < inventory.size(); ++i )
		{
			currentItem = ((AdventureResult)inventory.get(i)).getName().toLowerCase();

			if ( TradeableItemDatabase.getConsumptionType( currentItem ) == filterID )
			{
				// If it's a familiar item, make sure it's OK
				// for current familiar.

				if ( filterID == ConsumeItemRequest.EQUIP_FAMILIAR )
				{
					if ( currentFamiliar.canEquip( currentItem ) )
						items.add( currentItem );
				}

				// Otherwise, see if we have the necessary stat
				// to equip the item.

				else if ( EquipmentDatabase.canEquip( currentItem ) )
				{
					if ( filterID != ConsumeItemRequest.EQUIP_ACCESSORY )
						items.add( currentItem + " (+" + EquipmentDatabase.getPower( currentItem ) + ")" );
					else
						items.add( currentItem );
				}
			}
		}

		// If we are looking at familiar items, include those which can
		// be universally equipped, but are currently on another
		// familiar.
		if ( filterID == ConsumeItemRequest.EQUIP_FAMILIAR )
		{
			for ( int i = 0; i < familiars.size(); ++i )
			{
				FamiliarData familiar = (FamiliarData)familiars.get(i);
				String item = familiar.getItem();
				if ( item != null && !items.contains( item ) && currentFamiliar.canEquip( item ) )
					items.add( item );
			}
		}

		if ( !items.contains( equippedItem ) )
			items.add( equippedItem );

		if ( !items.contains( EquipmentRequest.UNEQUIP ) )
			items.add( EquipmentRequest.UNEQUIP );

		return items;
	}

	/**
	 * Accessor method to retrieve a list of the outfits available to this character, based
	 * on the last time the equipment screen was requested.  Note that this list may be outdated
	 * or outright wrong because of changes to the character's status.
	 *
	 * @return	A <code>LockableListModel</code> of the available outfits
	 */

	public LockableListModel getOutfits()
	{	return outfits;
	}

	/**
	 * Accessor method to retrieve a list of the items contained within the character's inventory.
	 * Note that each of the elements within this list is an <code>AdventureResult</code> object
	 * and that any changes to the internal character inventory will be reflected in the returned
	 * <code>SortedListModel</code>.
	 *
	 * @return	A <code>SortedListModel</code> of the items in the character's inventory
	 */

	public SortedListModel getInventory()
	{	return inventory;
	}

	/**
	 * Accessor method to retrieve a list of the items contained within the character's closet.
	 * Note that each of the elements within this list is an <code>AdventureResult</code> object
	 * and that any changes to the internal character closet will be reflected in the returned
	 * <code>LockableListModel</code>.
	 *
	 * @return	A <code>SortedListModel</code> of the items in the character's closet
	 */

	public SortedListModel getCloset()
	{	return closet;
	}

	/**
	 * Accessor method to retrieve a list of the items contained within the character's collection.
	 * Note that each of the elements within this list is an <code>AdventureResult</code> object
	 * and that any changes to the internal character collection will be reflected in the returned
	 * <code>LockableListModel</code>.
	 *
	 * @return	A <code>SortedListModel</code> of the items in the character's collection
	 */

	public SortedListModel getCollection()
	{	return collection;
	}

	/**
	 * Accessor method which indicates whether or not the character has a toaster
	 * @return	<code>true</code> if the character has a toaster
	 */

	public boolean hasToaster()
	{	return hasToaster;
	}

	/**
	 * Accessor method to indicate a change in state of the toaster.
	 * @param	hasToaster	Whether or not the character currently has a toaster
	 */

	public void setToaster( boolean hasToaster )
	{	this.hasToaster = hasToaster;
	}

	/**
	 * Accessor method which indicates whether or not the character has golden arches
	 * @return	<code>true</code> if the character has golden arches
	 */

	public boolean hasArches()
	{	return hasArches;
	}

	/**
	 * Accessor method to indicate a change in state of the golden arches.
	 * @param	hasArches	Whether or not the character currently has golden arches
	 */

	public void setArches( boolean hasArches )
	{	this.hasArches = hasArches;
	}

	/**
	 * Accessor method which indicates whether or not the character has a bartender-in-the-box.
	 * @return	<code>true</code> if the character has a bartender-in-the-box
	 */

	public boolean hasBartender()
	{	return hasBartender;
	}

	/**
	 * Accessor method to indicate a change in state of the bartender-in-the-box.
	 * @param	hasBartender	Whether or not the character currently has a bartender
	 */

	public void setBartender( boolean hasBartender )
	{	this.hasBartender = hasBartender;
	}

	/**
	 * Accessor method which indicates whether or not the character has a chef-in-the-box.
	 * @return	<code>true</code> if the character has a chef-in-the-box
	 */

	public boolean hasChef()
	{	return hasChef;
	}

	/**
	 * Accessor method to indicate a change in state of the chef-in-the-box.
	 * @param	hasChef	Whether or not the character currently has a chef
	 */

	public void setChef( boolean hasChef )
	{	this.hasChef = hasChef;
	}

	/**
	 * Accessor method which tells you if the character can interact
	 * with other players (Ronin or Hardcore players cannot).
	 */

	public boolean canInteract()
	{	return canInteract;
	}

	/**
	 * Accessor method which tells you if the character can interact
	 * with other players (Ronin or Hardcore players cannot).
	 */

	public void setInteraction( boolean canInteract )
	{	this.canInteract = canInteract;
	}

	/**
	 * Accessor method for the character's ascension count
	 * @return	String
	 */

	public int getAscensions()
	{	return ascensions;
	}

	/**
	 * Accessor method for the character's zodiac sign
	 * @return	String
	 */

	public String getSign()
	{	return ascensionSign;
	}

	/**
	 * Accessor method for the character's zodiac sign stat
	 * @return	int
	 */

	public int getSignStat()
	{	return ascensionSignType;
	}

	/**
	 * Accessor method to set a character's ascension count
	 * @param	ascensions	the new ascension count
	 */

	public void setAscensions( int ascensions )
	{
		this.ascensions = ascensions;
	}

	/**
	 * Accessor method to set a character's zodiac sign
	 * @param	ascensionSign	the new sign
	 */

	public void setSign( String ascensionSign )
	{
		if ( ascensionSign.startsWith("The ") )
		     ascensionSign = ascensionSign.substring(4);

		this.ascensionSign = ascensionSign;

		if (ascensionSign.equals("Wallaby") || ascensionSign.equals("Mongoose") || ascensionSign.equals("Vole"))
			ascensionSignType = MUSCLE;
		else if (ascensionSign.equals("Platypus") || ascensionSign.equals("Opossum") || ascensionSign.equals("Marmot"))
			ascensionSignType = MYSTICALITY;
		else if (ascensionSign.equals("Wombat") || ascensionSign.equals("Blender") || ascensionSign.equals("Packrat"))
			ascensionSignType = MOXIE;
		else
			ascensionSignType = NONE;
	}

	/**
	 * Accessor method which indicates whether the character is in a
	 * Muscle sign
	 *
	 * KoLmafia could/should use this to:
	 *
	 * - Allow adventuring in The Bugbear Pens
	 * - Provide access to npcstore #4: The Degrassi Knoll Bakery
	 * - Provide access to npcstore #5: The Degrassi Knoll General Store
	 * - Train Muscle in The Gym
	 * - Smith non-advanced things using Innabox (no hammer/adventure)
	 * - Combine anything using The Plunger (no meat paste)
	 *
	 * @return	<code>true</code> if the character is in a Muscle sign
	 */

	public boolean inMuscleSign()
	{	return (ascensionSignType == MUSCLE);
	}

	/**
	 * Accessor method which indicates whether the character is in a
	 * Mysticality sign
	 *
	 * KoLmafia could/should use this to:
	 *
	 * - Allow adventuring in Outskirts of Camp Logging Camp
	 * - Allow adventuring in Camp Logging Camp
	 * - Provide access to npcstore #j: Little Canadia Jewelers
	 * - Train Mysticality in The Institute for Canadian Studies
	 *
	 * @return	<code>true</code> if the character is in a Mysticality sign
	 */

	public boolean inMysticalitySign()
	{	return (ascensionSignType == MYSTICALITY);
	}

	/**
	 * Accessor method which indicates whether the character is in a
	 * Moxie sign
	 *
	 * KoLmafia could/should use this to:
	 *
	 * - Allow adventuring in Thugnderdome
	 * - Provide access to TINKER recipes
	 * - Train Moxie with Gnirf
	 *
	 * @return	<code>true</code> if the character is in a Moxie sign
	 */

	public boolean inMoxieSign()
	{	return (ascensionSignType == MOXIE);
	}

	/**
	 * Accessor method to clear the list of active effects.
	 */

	public void clearEffects()
	{	activeEffects.clear();
	}

	/**
	 * Accessor method to add a listing of the current effects.
	 * @return	A list of current effects
	 */

	public LockableListModel getEffects()
	{	return activeEffects;
	}

	/**
	 * Accessor method to set the list of available skills.
	 * @param	availableSkills	The list of the names of available skills
	 */

	public void setAvailableSkills( List availableSkills )
	{
		this.availableSkills.clear();
		this.availableSkills.addAll( availableSkills );

		int currentSkillID;
		UseSkillRequest currentSkill;

		this.usableSkills.clear();
		Iterator skills = availableSkills.iterator();

		while ( skills.hasNext() )
		{
			currentSkill = (UseSkillRequest) skills.next();
			currentSkillID = ClassSkillsDatabase.getSkillID( currentSkill.getSkillName() );

			if ( ClassSkillsDatabase.isNormal( currentSkillID ) || ClassSkillsDatabase.isBuff( currentSkillID ) )
				usableSkills.add( currentSkill );
		}
	}

	/**
	 * Accessor method to look up the list of available skills.
	 * @return	A list of the names of available skills
	 */

	public LockableListModel getAvailableSkills()
	{	return availableSkills;
	}

	/**
	 * Accessor method to look up the list of usable skills.
	 * @return	A list of the names of usable skills
	 */

	public LockableListModel getUsableSkills()
	{	return usableSkills;
	}

	/**
	 * Accessor method to look up whether or not the character can
	 * summon noodles.
	 *
	 * @return	<code>true</code> if noodles can be summoned by this character
	 */

	public boolean canSummonNoodles()
	{	return hasSkill( "Pastamastery" );
	}

	/**
	 * Accessor method to look up whether or not the character can
	 * summon reagent.
	 *
	 * @return	<code>true</code> if reagent can be summoned by this character
	 */

	public boolean canSummonReagent()
	{	return hasSkill( "Advanced Saucecrafting" );
	}

	/**
	 * Accessor method to look up whether or not the character can
	 * summon shore-based items.
	 *
	 * @return	<code>true</code> if shore-based items can be summoned by this character
	 */

	public boolean canSummonShore()
	{	return hasSkill( "Advanced Cocktailcrafting" );
	}

	/**
	 * Accessor method to look up whether or not the character can
	 * smith weapons.
	 *
	 * @return	<code>true</code> if this character can smith advanced weapons
	 */

	public boolean canSmithWeapons()
	{	return hasSkill( "Super-Advanced Meatsmithing" );
	}

	/**
	 * Accessor method to look up whether or not the character can
	 * smith armor.
	 *
	 * @return	<code>true</code> if this character can smith advanced armor
	 */

	public boolean canSmithArmor()
	{	return hasSkill( "Armorcraftiness" );
	}

	/**
	 * Accessor method to look up whether or not the character has
	 * Amphibian Sympathy
	 *
	 * @return	<code>true</code> if this character has Amphibian Sympathy
	 */

	public boolean hasAmphibianSympathy()
	{	return hasSkill( "Amphibian Sympathy" );
	}

	/**
	 * Utility method which looks up whether or not the character
	 * has a skill of the given name.
	 */

	public boolean hasSkill( String skillName )
	{
		for ( int i = 0; i < availableSkills.size(); ++i )
			if ( ((UseSkillRequest)availableSkills.get(i)).getSkillName().equals( skillName ) )
				return true;
		return false;
	}

	/**
	 * Accessor method to set the data for the current familiar.
	 * @param	familiar
	 */

	public void setFamiliar( FamiliarData familiar )
	{
		currentFamiliar = familiar;
		addFamiliar( currentFamiliar );
		familiars.setSelectedItem( currentFamiliar );
	}

	/**
	 * Accessor method to increment the weight of the current familiar
	 * by one.
	 */

	public void incrementFamilarWeight()
	{
		if ( currentFamiliar != null )
			currentFamiliar.setWeight( currentFamiliar.getWeight() + 1 );
	}

	/**
	 * Adds the given familiar to the list of available familiars.
	 * @param	familiar	The ID of the familiar to be added
	 */

	public void addFamiliar( FamiliarData familiar )
	{
		if ( familiar == null )
			return;

		if ( familiars.contains( familiar ) )
			familiars.remove( familiars.indexOf( familiar ) );

		familiars.add( familiar );
	}

	/**
	 * Returns the list of familiars available to the character.
	 * @return	The list of familiars available to the character
	 */

	public LockableListModel getFamiliarList()
	{	return familiars;
	}

	/**
	 * Returns the string used on the character pane to detrmine
	 * how many points remain until the character's next level.
	 * @return	The string indicating the TNP advancement
	 */

	public String getAdvancement()
	{
		int currentPrime = classtype.startsWith( "Se" ) || classtype.startsWith( "Tu" ) ? calculateBasePoints( totalSubpoints[0] ) :
			classtype.startsWith( "Sa" ) || classtype.startsWith( "Pa" ) ? calculateBasePoints( totalSubpoints[1] ) :
				calculateBasePoints( totalSubpoints[2] );

		String primeStat = classtype.startsWith( "Se" ) || classtype.startsWith( "Tu" ) ? "Muscle" :
			classtype.startsWith( "Sa" ) || classtype.startsWith( "Pa" ) ? "Mysticality" : "Moxie";

		int level = getLevel();
		return (level * level + 4 - currentPrime) + " " + primeStat + " until level " + (level + 1);
	}

	/**
	 * Adds a new <code>KoLCharacterListener</code> to the
	 * list of listeners listening to this <code>KoLCharacter</code>.
	 * @param	listener	The listener to be added to the listener list
	 */

	public void addKoLCharacterListener( KoLCharacterListener listener )
	{
		if ( listener != null && listener.isStatusListener() && !listenerList.contains( listener ) )
			listenerList.add( listener );
	}

	/**
	 * Processes a result.
	 */

	public void processResult( AdventureResult result )
	{
		String resultName = result.getName();

		if ( result.isItem() )
			AdventureResult.addResultToList( inventory, result );
		else if ( resultName.equals( AdventureResult.HP ) )
			setHP( getCurrentHP() + result.getCount(), getMaximumHP(), getBaseMaxHP() );
		else if ( resultName.equals( AdventureResult.MP ) )
			setMP( getCurrentMP() + result.getCount(), getMaximumMP(), getBaseMaxMP() );
		else if ( resultName.equals( AdventureResult.MEAT ) )
			setAvailableMeat( getAvailableMeat() + result.getCount() );
		else if ( resultName.equals( AdventureResult.ADV ) )
		{
			setAdventuresLeft( getAdventuresLeft() + result.getCount() );
			if ( result.getCount() < 0 )
			{
				AdventureResult.reduceTally( getEffects(), result.getCount() );
				setTotalTurnsUsed( getTotalTurnsUsed() + result.getCount() );
			}

		}
		else if ( resultName.equals( AdventureResult.DRUNK ) )
			setInebriety( getInebriety() + result.getCount() );

		// Now, if it's an actual stat gain, be sure to update the
		// list to reflect the current value of stats so far.

		else if ( resultName.equals( AdventureResult.SUBSTATS ) )
		{
			if ( result.isMuscleGain() )
				totalSubpoints[0] += result.getCount();
			else if ( result.isMysticalityGain() )
				totalSubpoints[1] += result.getCount();
			else if ( result.isMoxieGain() )
				totalSubpoints[2] += result.getCount();

			Iterator listenerIterator = listenerList.iterator();
			while ( listenerIterator.hasNext() )
				((KoLCharacterListener)listenerIterator.next()).statusPointsChanged();

		}
	}
}
