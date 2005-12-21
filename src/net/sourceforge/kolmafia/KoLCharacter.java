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

public abstract class KoLCharacter extends StaticEntity
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

	// Equipment constants

	public static final int HAT = 0;
	public static final int WEAPON = 1;
	public static final int OFFHAND = 2;
	public static final int SHIRT = 3;
	public static final int PANTS = 4;
	public static final int ACCESSORY1 = 5;
	public static final int ACCESSORY2 = 6;
	public static final int ACCESSORY3 = 7;
	public static final int FAMILIAR = 8;

	// Ascension sign constants

	public static final int NONE = 0;
	public static final int MUSCLE = 1;
	public static final int MYSTICALITY = 2;
	public static final int MOXIE = 3;

	// General static variables

	private static String username = "";
	private static String avatar = "";
	private static int userID = 0;
	private static String classname = "";
	private static String classtype = "";

	private static int currentHP, maximumHP, baseMaxHP;
	private static int currentMP, maximumMP, baseMaxMP;

	private static int [] adjustedStats = new int[3];
	private static int [] totalSubpoints = new int[3];

	private static LockableListModel equipment = new LockableListModel();
	private static LockableListModel outfits = new LockableListModel();

	static
	{
		for ( int i = 0; i < 8; ++i )
			equipment.add( EquipmentRequest.UNEQUIP );
	}

	private static SortedListModel inventory = new SortedListModel( AdventureResult.class );
	private static SortedListModel closet = new SortedListModel( AdventureResult.class );
	private static SortedListModel collection = new SortedListModel( AdventureResult.class );
	private static SortedListModel storage = new SortedListModel( AdventureResult.class );
	private static SortedListModel usables = new SortedListModel( AdventureResult.class );
	private static SortedListModel sellables = new SortedListModel( AdventureResult.class );

	private static LockableListModel activeEffects = new LockableListModel();
	private static SortedListModel usableSkills = new SortedListModel( UseSkillRequest.class );
	private static LockableListModel availableSkills = new LockableListModel();

	private static LockableListModel battleSkillIDs = new LockableListModel();
	private static LockableListModel battleSkillNames = new LockableListModel();

	private static LockableListModel [] equipmentLists = new LockableListModel[9];
	static
	{
		for ( int i = 0; i < 9; ++i )
			equipmentLists[i] = new SortedListModel();
	}

	// Status pane data which is rendered whenever
	// the user issues a "status" type command.

	private static int availableMeat = 0;
	private static int closetMeat = 0;
	private static int inebriety = 0;
	private static int adventuresLeft = 0;
	private static int totalTurnsUsed = 0;

	// Campground information

	private static boolean hasToaster = false;
	private static boolean hasArches = false;
	private static boolean hasChef = false;
	private static boolean hasBartender = false;

	// Familiar data for reference

	private static SortedListModel familiars = new SortedListModel( FamiliarData.class );
	private static FamiliarData currentFamiliar = FamiliarData.NO_FAMILIAR;

	// Listener-driven container items

	private static List listenerList = new ArrayList();

	// Interesting accomplishments

	private static LockableListModel accomplishments = new LockableListModel();
	public static final String UNTINKER = "You have found the Untinker's screwdriver.";
	public static final String MEATCAR = "You have built your own Bitchin' Meat Car.";
	public static final String FRIARS = "You have cleansed the taint for the Deep Fat Friars.";
	public static final String BARON = "You have helped the Baron Rof L'm Fao.";
	public static final String ICY_PEAK = "You have helped the L337 Tr4pz0r clear the way to the Icy Peak.";
	public static final String BEANSTALK = "You have planted a Beanstalk in the Nearby Plains.";

	private static boolean beanstalkArmed;

	// Ascension-related variables

	private static boolean canInteract = false;
	private static int ascensions = 0;
	private static String ascensionSign = "None";
	private static int ascensionSignType = NONE;
	private static int consumptionRestriction = AscensionSnapshotTable.NOPATH;
	private static int mindControlLevel = 0;


	/**
	 * Constructs a new <code>KoLCharacter</code> with the given name.
	 * All fields are initialized to their default values (nothing),
	 * and it is the responsibility of other methods to initialize
	 * the fields with their real values.
	 *
	 * @param	username	The name of the character this <code>KoLCharacter</code> represents
	 */

	public static final void reset( String username )
	{
		if ( client == null )
			return;

		KoLCharacter.username = username;

		classname = "";
		classtype = "";

		adjustedStats = new int[3];
		totalSubpoints = new int[3];

		equipment.clear();
		for ( int i = 0; i < 8; ++i )
			equipment.add( EquipmentRequest.UNEQUIP );

		outfits.clear();

		inventory.clear();
		closet.clear();
		storage.clear();
		collection.clear();

		activeEffects.clear();
		usableSkills.clear();
		availableSkills.clear();

		canInteract = true;
		hasToaster = false;
		hasArches = false;
		hasChef = false;
		hasBartender = false;

		familiars.clear();
		familiars.add( FamiliarData.NO_FAMILIAR );

		accomplishments.clear();
		beanstalkArmed = false;

		ascensions = 0;
		ascensionSign = "None";
		ascensionSignType = NONE;

		mindControlLevel = 0;

		// Initialize the equipment lists inside
		// of the character data

		for ( int i = 0; i < 9; ++i )
		{
			equipmentLists[i].clear();
			equipmentLists[i].add( EquipmentRequest.UNEQUIP );
		}

		updateEquipmentLists();
	}

	public static boolean isFallingDown()
	{
		return getInebriety() > (KoLCharacter.hasSkill( "Liver of Steel" ) ? 19 : 14);
	}

	/**
	 * Accessor method to retrieve the name of this character.
	 * @return	The name of this character
	 */

	public static String getUsername()
	{	return username;
	}

	/**
	 * Accessor method to set the user ID associated with this character.
	 * @param	userID	The user ID associated with this character
	 */

	public static void setUserID( int userID )
	{	KoLCharacter.userID = userID;
	}

	/**
	 * Accessor method to retrieve the user ID associated with this character.
	 * @return	The user ID associated with this character
	 */

	public static int getUserID()
	{	return userID;
	}

	/**
	 * Accessor method to get the avatar associated with this character.
	 * @param	avatar	The avatar for this character
	 */

	public static void setAvatar( String avatar )
	{	KoLCharacter.avatar = avatar;
	}

	/**
	 * Accessor method to get the avatar associated with this character.
	 * @return	The avatar for this character
	 */

	public static String getAvatar()
	{	return avatar;
	}

	/**
	 * Accessor method to retrieve the index of the prime stat.
	 * @return	The index of the prime stat
	 */

	public static int getPrimeIndex()
	{	return classtype.startsWith( "Se" ) || classtype.startsWith( "Tu" ) ? 0 : classtype.startsWith( "Sa" ) || classtype.startsWith( "Pa" ) ? 1 : 2;
	}

	/**
	 * Accessor method to retrieve the level of this character.
	 * @return	The level of this character
	 */

	public static int getLevel()
	{	return (int) Math.sqrt( calculateBasePoints( getTotalPrime() ) - 4 ) + 1;
	}


	/**
	 * Accessor method to set the character's class.
	 * @param	classname	The name of the character's class
	 */

	public static void setClassName( String classname )
	{
		KoLCharacter.classname = classname;
		KoLCharacter.classtype = getClassType();
	}

	/**
	 * Accessor method to retrieve the name of the character's class.
	 * @return	The name of the character's class
	 */

	public static String getClassName()
	{	return classname;
	}

	/**
	 * Accessor method to retrieve the type of the character's class.
	 * @return	The type of the character's class
	 */

	public static String getClassType()
	{
		return SEAL_CLUBBER.contains( classname ) ? "Seal Clubber" :
			TURTLE_TAMER.contains( classname ) ? "Turtle Tamer" :
			PASTAMANCER.contains( classname ) ? "Pastamancer" :
			SAUCEROR.contains( classname ) ? "Sauceror" :
			DISCO_BANDIT.contains( classname ) ? "Disco Bandit" :
			ACCORDION_THIEF.contains( classname ) ? "Accordion Thief" : "Sauceror";
	}

	/**
	 * Accessor method to retrieve the type of the character's class.
	 * @return	The type of the character's class
	 */

	public static String getClassType( String classname )
	{
		return SEAL_CLUBBER.contains( classname ) ? "Seal Clubber" :
			TURTLE_TAMER.contains( classname ) ? "Turtle Tamer" :
			PASTAMANCER.contains( classname ) ? "Pastamancer" :
			SAUCEROR.contains( classname ) ? "Sauceror" :
			DISCO_BANDIT.contains( classname ) ? "Disco Bandit" :
			ACCORDION_THIEF.contains( classname ) ? "Accordion Thief" : "Sauceror";
	}

	/**
	 * Accessor method to set the character's current health state.
	 * @param	currentHP	The character's current HP value
	 * @param	maximumHP	The character's maximum HP value
	 * @param	baseMaxHP	The base value for the character's maximum HP
	 */

	public static void setHP( int currentHP, int maximumHP, int baseMaxHP )
	{
		KoLCharacter.currentHP = currentHP < 0 ? 0 :currentHP > maximumHP ? maximumHP : currentHP;
		KoLCharacter.maximumHP = maximumHP;
		KoLCharacter.baseMaxHP = baseMaxHP;

		Iterator listenerIterator = listenerList.iterator();
		while ( listenerIterator.hasNext() )
			((KoLCharacterListener)listenerIterator.next()).hpChanged();
	}

	/**
	 * Accessor method to retrieve the character's current HP.
	 * @return	The character's current HP
	 */

	public static int getCurrentHP()
	{	return currentHP;
	}

	/**
	 * Accessor method to retrieve the character's maximum HP.
	 * @return	The character's maximum HP
	 */

	public static int getMaximumHP()
	{	return maximumHP;
	}

	/**
	 * Accessor method to retrieve the base value for the character's maximum HP.
	 * @return	The base value for the character's maximum HP
	 */

	public static int getBaseMaxHP()
	{	return baseMaxHP;
	}

	/**
	 * Accessor method to set the character's current mana limits.
	 * @param	currentMP	The character's current MP value
	 * @param	maximumMP	The character's maximum MP value
	 * @param	baseMaxMP	The base value for the character's maximum MP
	 */

	public static void setMP( int currentMP, int maximumMP, int baseMaxMP )
	{
		KoLCharacter.currentMP = currentMP < 0 ? 0 : currentMP > maximumMP ? maximumMP : currentMP;
		KoLCharacter.maximumMP = maximumMP;
		KoLCharacter.baseMaxMP = baseMaxMP;

		Iterator listenerIterator = listenerList.iterator();
		while ( listenerIterator.hasNext() )
			((KoLCharacterListener)listenerIterator.next()).mpChanged();
	}

	/**
	 * Accessor method to retrieve the character's current MP.
	 * @return	The character's current MP
	 */

	public static int getCurrentMP()
	{	return currentMP;
	}

	/**
	 * Accessor method to retrieve the character's maximum MP.
	 * @return	The character's maximum MP
	 */

	public static int getMaximumMP()
	{	return maximumMP;
	}

	/**
	 * Accessor method to retrieve the base value for the character's maximum MP.
	 * @return	The base value for the character's maximum MP
	 */

	public static int getBaseMaxMP()
	{	return baseMaxMP;
	}

	/**
	 * Accessor method to set the amount of meat in the character's closet.
	 * @param	closetMeat	The amount of meat in the character's closet.
	 */

	public static void setClosetMeat( int closetMeat )
	{
		KoLCharacter.closetMeat = closetMeat;

		Iterator listenerIterator = listenerList.iterator();
		while ( listenerIterator.hasNext() )
			((KoLCharacterListener)listenerIterator.next()).closetMeatChanged();
	}

	/**
	 * Accessor method to retrieve the amount of meat in the character's closet.
	 * @return	The amount of meat in the character's closet.
	 */

	public static int getClosetMeat()
	{	return closetMeat;
	}

	/**
	 * Accessor method to set the character's current available meat for spending
	 * (IE: meat that isn't currently in the character's closet).
	 *
	 * @param	availableMeat	The character's available meat for spending
	 */

	public static void setAvailableMeat( int availableMeat )
	{
		KoLCharacter.availableMeat = availableMeat;

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

	public static int getAvailableMeat()
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

	public static void setStatPoints( int adjustedMuscle, int totalMuscle,
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
	 * Returns the total number of subpoints to the current level.
	 * @return	The total subpoints to the current level
	 */

	public static int calculateLastLevel()
	{
		int level = getLevel() - 1;
		int basePointsNeeded = level * level + 4;
		return basePointsNeeded * basePointsNeeded - 1;
	}

	/**
	 * Returns the total number of subpoints to the next level.
	 * @return	The total subpoints to the next level
	 */

	public static int calculateNextLevel()
	{
		int level = getLevel();
		int basePointsNeeded = level * level + 4;
		return basePointsNeeded * basePointsNeeded - 1;
	}

	/**
	 * Returns the total number of subpoints acquired in the prime stat.
	 * @return	The total subpoints in the prime stat
	 */

	public static int getTotalPrime()
	{	return totalSubpoints[ getPrimeIndex() ];
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

	public static int getBaseMuscle()
	{	return calculateBasePoints( totalSubpoints[0] );
	}

	/**
	 * Accessor method to retrieve the total subpoints accumulted so far
	 * in muscle.
	 *
	 * @return	The total muscle subpoints so far
	 */

	public static int getTotalMuscle()
	{	return totalSubpoints[0];
	}

	/**
	 * Accessor method to retrieve the number of subpoints required
	 * before the character gains another full point of muscle.
	 */

	public static int getMuscleTNP()
	{	return calculateTillNextPoint( totalSubpoints[0] );
	}

	/**
	 * Accessor method to retrieve the character's adjusted value for muscle.
	 * @return	The character's adjusted value for muscle
	 */

	public static int getAdjustedMuscle()
	{	return adjustedStats[0];
	}

	/**
	 * Accessor method to retrieve the character's base value for mysticality.
	 * @return	The character's base value for muscle
	 */

	public static int getBaseMysticality()
	{	return calculateBasePoints( totalSubpoints[1] );
	}

	/**
	 * Accessor method to retrieve the total subpoints accumulted so far
	 * in mysticality.
	 *
	 * @return	The total mysticality subpoints so far
	 */

	public static int getTotalMysticality()
	{	return totalSubpoints[1];
	}

	/**
	 * Accessor method to retrieve the number of subpoints required
	 * before the character gains another full point of mysticality.
	 */

	public static int getMysticalityTNP()
	{	return calculateTillNextPoint( totalSubpoints[1] );
	}

	/**
	 * Accessor method to retrieve the character's adjusted value for mysticality.
	 * @return	The character's adjusted value for mysticality
	 */

	public static int getAdjustedMysticality()
	{	return adjustedStats[1];
	}

	/**
	 * Accessor method to retrieve the character's base value for moxie.
	 * @return	The character's base value for moxie
	 */

	public static int getBaseMoxie()
	{	return calculateBasePoints( totalSubpoints[2] );
	}

	/**
	 * Accessor method to retrieve the total subpoints accumulted so far
	 * in moxie.
	 *
	 * @return	The total moxie subpoints so far
	 */

	public static int getTotalMoxie()
	{	return totalSubpoints[2];
	}

	/**
	 * Accessor method to retrieve the number of subpoints required
	 * before the character gains another full point of moxie.
	 */

	public static int getMoxieTNP()
	{	return calculateTillNextPoint( totalSubpoints[2] );
	}

	/**
	 * Accessor method to retrieve the character's adjusted value for moxie.
	 * @return	The character's adjusted value for moxie
	 */

	public static int getAdjustedMoxie()
	{	return adjustedStats[2];
	}

	/**
	 * Accessor method to set the character's current inebriety (also known as
	 * drunkenness, tipsiness, and various other names).
	 *
	 * @param	inebriety	The character's current inebriety level
	 */

	public static void setInebriety( int inebriety )
	{
		KoLCharacter.inebriety = inebriety;
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

	public static int getInebriety()
	{	return inebriety;
	}

	/**
	 * Accessor method to set the number of adventures the character has left to
	 * spend in this session.
	 *
	 * @param	adventuresLeft	The number of adventures the character has left
	 */

	public static void setAdventuresLeft( int adventuresLeft )
	{
		KoLCharacter.adventuresLeft = adventuresLeft;
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

	public static int getAdventuresLeft()
	{	return adventuresLeft;
	}

	/**
	 * Accessor method to set the total number of turns the character has used
	 * since creation.  This method is only interesting from an averages point of
	 * view, but sometimes, it's interesting to know.
	 *
	 * @param	totalTurnsUsed	The total number of turns used since creation
	 */

	public static void setTotalTurnsUsed( int totalTurnsUsed )
	{
		KoLCharacter.totalTurnsUsed = totalTurnsUsed;
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

	public static int getTotalTurnsUsed()
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

	public static void setEquipment( String [] equipment, List outfits )
	{
		for ( int i = 0; i < KoLCharacter.equipment.size(); ++i )
		{
			if ( i == FAMILIAR )
				continue;

			if ( equipment[i] == null || equipment[i].equals( "none" ) || equipment[i].equals( EquipmentRequest.UNEQUIP ) )
				KoLCharacter.equipment.set( i, EquipmentRequest.UNEQUIP );
			else if ( TradeableItemDatabase.getConsumptionType( equipment[i] ) == ConsumeItemRequest.EQUIP_ACCESSORY )
				KoLCharacter.equipment.set( i, equipment[i].toLowerCase() );
			else
				KoLCharacter.equipment.set( i, equipment[i].toLowerCase() + " (+" + EquipmentDatabase.getPower( equipment[i] ) + ")" );
		}

		if ( equipment.length > FAMILIAR && currentFamiliar != null )
			currentFamiliar.setItem( equipment[FAMILIAR].toLowerCase() );

		if ( !KoLCharacter.outfits.equals( outfits ) )
		{
			KoLCharacter.outfits.clear();
			KoLCharacter.outfits.add( SpecialOutfit.NO_CHANGE );
			KoLCharacter.outfits.add( SpecialOutfit.BIRTHDAY_SUIT );
			KoLCharacter.outfits.addAll( outfits );
		}

		FamiliarData.updateWeightModifier();
		ClassSkillsDatabase.updateManaModifier();
	}

	/**
	 * Accessor method to retrieve the name of the item equipped on the character's familiar.
	 * @return	The name of the item equipped on the character's familiar, <code>none</code> if no such item exists
	 */

	public static String getFamiliarItem()
	{	return currentFamiliar == null ? EquipmentRequest.UNEQUIP : currentFamiliar.getItem();
	}

	/**
	 * Accessor method to retrieve the name of a piece of equipment
	 * @param	type	the type of equipment
	 * @return	The name of the equipment, <code>none</code> if no such item exists
	 */

	public static String getEquipment( int type )
	{
		if ( type >= HAT && type < FAMILIAR )
			return (String) equipment.get( type );

		if ( type == FAMILIAR )
			return getFamiliarItem();

		return EquipmentRequest.UNEQUIP;
	}

	/**
	 * Accessor method to retrieve a piece of equipment
	 * @param	type	the type of equipment
	 * @return	AdventureResult for the equipment, <code>null</code> if no such item exists
	 */

	public static AdventureResult getCurrentEquipment( int type )
	{
		String name = getEquipment( type );

		// If slot not currently equipped, return null
		if ( name.equals( EquipmentRequest.UNEQUIP ))
			return null;

		// Strip off item power
		int paren = name.indexOf( " (" );
		if ( paren >= 0 )
			name = name.substring( 0, paren );

		// Make an AdventureResult describing this item
		return new AdventureResult( name, 1, false );
	}

	/**
	 * Accessor method to retrieve a list of all available items which can be equipped
	 * by familiars.  Note this lists items which the current familiar cannot equip.
	 */

	public static LockableListModel [] getEquipmentLists()
	{	return equipmentLists;
	}

	public static void updateEquipmentLists()
	{
		updateEquipmentList( equipmentLists[HAT], ConsumeItemRequest.EQUIP_HAT, getEquipment( HAT ) );
		updateEquipmentList( equipmentLists[WEAPON], ConsumeItemRequest.EQUIP_WEAPON, getEquipment( WEAPON ) );
		updateEquipmentList( equipmentLists[OFFHAND], ConsumeItemRequest.EQUIP_OFFHAND, getEquipment( OFFHAND ) );
		updateEquipmentList( equipmentLists[SHIRT], ConsumeItemRequest.EQUIP_SHIRT, getEquipment( SHIRT ) );
		updateEquipmentList( equipmentLists[PANTS], ConsumeItemRequest.EQUIP_PANTS, getEquipment( PANTS ) );
		updateEquipmentList( equipmentLists[ACCESSORY1], ConsumeItemRequest.EQUIP_ACCESSORY, getEquipment( ACCESSORY1 ) );
		updateEquipmentList( equipmentLists[ACCESSORY2], ConsumeItemRequest.EQUIP_ACCESSORY, getEquipment( ACCESSORY2 ) );
		updateEquipmentList( equipmentLists[ACCESSORY3], ConsumeItemRequest.EQUIP_ACCESSORY, getEquipment( ACCESSORY3 ) );
		updateEquipmentList( equipmentLists[FAMILIAR], ConsumeItemRequest.EQUIP_FAMILIAR, getEquipment( FAMILIAR ) );
	}

	public static void updateEquipmentList( LockableListModel currentList, int currentFilter, String equippedItem )
	{
		currentList.setSelectedItem( null );
		currentList.clear();

		currentList.addAll( getFilteredItems( currentFilter, equippedItem ) );
		currentList.setSelectedItem( equippedItem );
	}

	private static List getFilteredItems( int filterID, String equippedItem )
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
				FamiliarData familiar = (FamiliarData) familiars.get(i);
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

	public static LockableListModel getOutfits()
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

	public static SortedListModel getInventory()
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

	public static SortedListModel getCloset()
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

	public static SortedListModel getCollection()
	{	return collection;
	}

	/**
	 * Accessor method to retrieve a list of the items contained within the character's storage.
	 * Note that each of the elements within this list is an <code>AdventureResult</code> object
	 * and that any changes to the internal character collection will be reflected in the returned
	 * <code>LockableListModel</code>.
	 *
	 * @return	A <code>SortedListModel</code> of the items in the character's collection
	 */

	public static SortedListModel getStorage()
	{	return storage;
	}

	/**
	 * Accessor method to retrieve all usable items contained within the character's inventory.
	 * Note that each of the elements within this list is an <code>AdventureResult</code> object
	 * and that any changes to the internal character inventory will be reflected in the returned
	 * <code>SortedListModel</code>.
	 *
	 * @return	A <code>SortedListModel</code> of the items in the character's inventory
	 */

	public static SortedListModel getUsables()
	{	return usables;
	}

	/**
	 * Accessor method to retrieve all sellable items contained within the character's inventory.
	 * Note that each of the elements within this list is an <code>AdventureResult</code> object
	 * and that any changes to the internal character inventory will be reflected in the returned
	 * <code>SortedListModel</code>.
	 *
	 * @return	A <code>SortedListModel</code> of the items in the character's inventory
	 */

	public static SortedListModel getSellables()
	{	return sellables;
	}

	/**
	 * Accessor method which indicates whether or not the the beanstalk has been armed this session.
	 * @return	<code>true</code> if the beanstalk has been armed
	 */

	public static boolean beanstalkArmed()
	{	return beanstalkArmed;
	}

	/**
	 * Accessor method to indicate a change in state of the beanstalk
	 */

	public static void armBeanstalk()
	{	KoLCharacter.beanstalkArmed = true;
	}

	/**
	 * Accessor method which indicates whether or not the character has a toaster
	 * @return	<code>true</code> if the character has a toaster
	 */

	public static boolean hasToaster()
	{	return hasToaster;
	}

	/**
	 * Accessor method to indicate a change in state of the toaster.
	 * @param	hasToaster	Whether or not the character currently has a toaster
	 */

	public static void setToaster( boolean hasToaster )
	{	KoLCharacter.hasToaster = hasToaster;
	}

	/**
	 * Accessor method which indicates whether or not the character has golden arches
	 * @return	<code>true</code> if the character has golden arches
	 */

	public static boolean hasArches()
	{	return hasArches;
	}

	/**
	 * Accessor method to indicate a change in state of the golden arches.
	 * @param	hasArches	Whether or not the character currently has golden arches
	 */

	public static void setArches( boolean hasArches )
	{	KoLCharacter.hasArches = hasArches;
	}

	/**
	 * Accessor method which indicates whether or not the character has a bartender-in-the-box.
	 * @return	<code>true</code> if the character has a bartender-in-the-box
	 */

	public static boolean hasBartender()
	{	return hasBartender;
	}

	/**
	 * Accessor method to indicate a change in state of the bartender-in-the-box.
	 * @param	hasBartender	Whether or not the character currently has a bartender
	 */

	public static void setBartender( boolean hasBartender )
	{
		KoLCharacter.hasBartender = hasBartender;
		refreshCalculatedLists();
	}

	/**
	 * Accessor method which indicates whether or not the character has a chef-in-the-box.
	 * @return	<code>true</code> if the character has a chef-in-the-box
	 */

	public static boolean hasChef()
	{	return hasChef;
	}

	/**
	 * Accessor method to indicate a change in state of the chef-in-the-box.
	 * @param	hasChef	Whether or not the character currently has a chef
	 */

	public static void setChef( boolean hasChef )
	{
		KoLCharacter.hasChef = hasChef;
		refreshCalculatedLists();
	}

	/**
	 * Accessor method which tells you if the character can interact
	 * with other players (Ronin or Hardcore players cannot).
	 */

	public static boolean canInteract()
	{	return canInteract;
	}

	/**
	 * Accessor method which tells you if the character can interact
	 * with other players (Ronin or Hardcore players cannot).
	 */

	public static void setInteraction( boolean canInteract )
	{	KoLCharacter.canInteract = canInteract;
	}

	/**
	 * Accessor method for the character's ascension count
	 * @return	String
	 */

	public static int getAscensions()
	{	return ascensions;
	}

	/**
	 * Accessor method for the character's zodiac sign
	 * @return	String
	 */

	public static String getSign()
	{	return ascensionSign;
	}

	/**
	 * Accessor method for the character's zodiac sign stat
	 * @return	int
	 */

	public static int getSignStat()
	{	return ascensionSignType;
	}

	/**
	 * Accessor method to set a character's ascension count
	 * @param	ascensions	the new ascension count
	 */

	public static void setAscensions( int ascensions )
	{	KoLCharacter.ascensions = ascensions;
	}

	/**
	 * Accessor method to set a character's zodiac sign
	 * @param	ascensionSign	the new sign
	 */

	public static void setSign( String ascensionSign )
	{
		if ( ascensionSign.startsWith("The ") )
		     ascensionSign = ascensionSign.substring(4);

		KoLCharacter.ascensionSign = ascensionSign;

		if (ascensionSign.equals("Wallaby") || ascensionSign.equals("Mongoose") || ascensionSign.equals("Vole"))
			ascensionSignType = MUSCLE;
		else if (ascensionSign.equals("Platypus") || ascensionSign.equals("Opossum") || ascensionSign.equals("Marmot"))
			ascensionSignType = MYSTICALITY;
		else if (ascensionSign.equals("Wombat") || ascensionSign.equals("Blender") || ascensionSign.equals("Packrat"))
			ascensionSignType = MOXIE;
		else
			ascensionSignType = NONE;
	}

	public static int getConsumptionRestriction()
	{	return consumptionRestriction;
	}

	public static void setConsumptionRestriction( int consumptionRestriction )
	{	KoLCharacter.consumptionRestriction = consumptionRestriction;
	}

	public static boolean canEat()
	{	return consumptionRestriction == AscensionSnapshotTable.NOPATH || consumptionRestriction == AscensionSnapshotTable.TEETOTALER;
	}

	public static boolean canDrink()
	{	return consumptionRestriction == AscensionSnapshotTable.NOPATH || consumptionRestriction == AscensionSnapshotTable.BOOZETAFARIAN;
	}

	/**
	 * Accessor method for the current mind control setting
	 * @return	String
	 */

	public static int getMindControlLevel()
	{	return mindControlLevel;
	}

	/**
	 * Accessor method to set  the current mind control level
	 * @param	level	the new level
	 */

	public static void setMindControlLevel( int level )
	{
		KoLCharacter.mindControlLevel = level;
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

	public static boolean inMuscleSign()
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

	public static boolean inMysticalitySign()
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

	public static boolean inMoxieSign()
	{	return (ascensionSignType == MOXIE);
	}

	/**
	 * Accessor method to clear the list of active effects.
	 */

	public static void clearEffects()
	{	activeEffects.clear();
	}

	/**
	 * Accessor method to add a listing of the current effects.
	 * @return	A list of current effects
	 */

	public static LockableListModel getEffects()
	{	return activeEffects;
	}

	/**
	 * Method to sort the effects by duration
	 */

	public static void sortEffects()
	{	AdventureResult.sortListByCount( activeEffects );
	}

	/**
	 * Accessor method to set the list of available skills.
	 * @param	availableSkills	The list of the names of available skills
	 */

	public static void setAvailableSkills( List availableSkills )
	{
		KoLCharacter.availableSkills.clear();
		KoLCharacter.usableSkills.clear();
		KoLCharacter.battleSkillIDs.clear();
		KoLCharacter.battleSkillNames.clear();

		// All characters get the option to
		// attack something.

		KoLCharacter.battleSkillIDs.add( "attack" );
		KoLCharacter.battleSkillNames.add( "Normal: Attack with Weapon" );

		// If the player has a dictionary, add it
		// to the available skills list.

		addDictionary();

		// Add in moxious maneuver if the player
		// is of the appropriate class.

		if ( KoLCharacter.getClassType().startsWith( "Ac" ) || KoLCharacter.getClassType().startsWith( "Di" ) )
		{
			battleSkillIDs.add( "moxman" );
			battleSkillNames.add( "Special: Moxious Maneuver" );
		}

		// Check all available skills to see if they
		// qualify to be added as combat or usables.

		Iterator skills = availableSkills.iterator();

		while ( skills.hasNext() )
			addAvailableSkill( (UseSkillRequest) skills.next() );

		// Set the selected combat skill based on
		// the user's current setting.

		battleSkillIDs.setSelectedItem( getProperty( "battleAction" ) );
		if ( battleSkillIDs.getSelectedIndex() != -1 )
			battleSkillNames.setSelectedIndex( battleSkillIDs.getSelectedIndex() );
	}

	public static void addDictionary()
	{
		if ( FightRequest.DICTIONARY1.getCount( KoLCharacter.getInventory() ) >= 1 )
			addDictionary( FightRequest.DICTIONARY1 );
		else if ( FightRequest.DICTIONARY2.getCount( KoLCharacter.getInventory() ) >= 1 )
			addDictionary( FightRequest.DICTIONARY2 );
	}

	public static void addDictionary( AdventureResult dictionary )
	{
		switch ( dictionary.getItemID() )
		{
		case 536:
			// We have the first dictionary. Add attack method to list
			if ( dictionary.getCount() > 0 )
			{
				battleSkillIDs.add( 1, "item0536" );
				battleSkillNames.add( 1, "Item: Use a Dictionary" );
				return;
			}
			break;

		case 1316:
			// We have the second dictionary. Add attack method to list
			if ( dictionary.getCount() > 0 )
			{
				battleSkillIDs.add( 1, "item1316" );
				battleSkillNames.add( 1, "Item: Use a Dictionary" );
				return;
			}
			break;

		default:
			return;
		}

		// We are removing a dictionary from the inventory. Remove tactic.
		battleSkillIDs.remove( 1 );
		battleSkillNames.remove( 1 );
	}

	/**
	 * Adds a single skill to the list of known skills
	 * possessed by this character.
	 */

	public static void addAvailableSkill( UseSkillRequest skill )
	{
		int id = ClassSkillsDatabase.getSkillID( skill.getSkillName() );

		availableSkills.add( skill );

		if ( ClassSkillsDatabase.isNormal( id ) || ClassSkillsDatabase.isBuff( id ) )
			usableSkills.add( skill );

		if ( ClassSkillsDatabase.isCombat( id ) )
		{
			battleSkillIDs.add( String.valueOf( id ) );
			battleSkillNames.add( "Skill: " + skill.getSkillName() );
		}
	}

	/**
	 * Returns a list of the identifiers of all available combat
	 * skills.  The selected index in this list should match
	 * the selected index in the battle skill names list.
	 */

	public static LockableListModel getBattleSkillIDs()
	{	return battleSkillIDs;
	}

	/**
	 * Returns a list of the names of all available combat
	 * skills.  The selected index in this list should match
	 * the selected index in the battle skills list.
	 */

	public static LockableListModel getBattleSkillNames()
	{	return battleSkillNames;
	}

	/**
	 * Accessor method to look up the list of usable skills.
	 * @return	A list of the names of usable skills
	 */

	public static LockableListModel getUsableSkills()
	{	return usableSkills;
	}

	/**
	 * Accessor method to look up whether or not the character can
	 * summon noodles.
	 *
	 * @return	<code>true</code> if noodles can be summoned by this character
	 */

	public static boolean canSummonNoodles()
	{	return hasSkill( "Pastamastery" );
	}

	/**
	 * Accessor method to look up whether or not the character can
	 * summon reagent.
	 *
	 * @return	<code>true</code> if reagent can be summoned by this character
	 */

	public static boolean canSummonReagent()
	{	return hasSkill( "Advanced Saucecrafting" );
	}

	/**
	 * Accessor method to look up whether or not the character can
	 * summon shore-based items.
	 *
	 * @return	<code>true</code> if shore-based items can be summoned by this character
	 */

	public static boolean canSummonShore()
	{	return hasSkill( "Advanced Cocktailcrafting" );
	}

	/**
	 * Accessor method to look up whether or not the character can
	 * smith weapons.
	 *
	 * @return	<code>true</code> if this character can smith advanced weapons
	 */

	public static boolean canSmithWeapons()
	{	return hasSkill( "Super-Advanced Meatsmithing" );
	}

	/**
	 * Accessor method to look up whether or not the character can
	 * smith armor.
	 *
	 * @return	<code>true</code> if this character can smith advanced armor
	 */

	public static boolean canSmithArmor()
	{	return hasSkill( "Armorcraftiness" );
	}

	/**
	 * Accessor method to look up whether or not the character has
	 * Amphibian Sympathy
	 *
	 * @return	<code>true</code> if this character has Amphibian Sympathy
	 */

	public static boolean hasAmphibianSympathy()
	{	return hasSkill( "Amphibian Sympathy" );
	}

	/**
	 * Utility method which looks up whether or not the character
	 * has a skill of the given name.
	 */

	public static boolean hasSkill( String skillName )
	{
		for ( int i = 0; i < availableSkills.size(); ++i )
			if ( ((UseSkillRequest)availableSkills.get(i)).getSkillName().equals( skillName ) )
				return true;
		return false;
	}

	/**
	 * Accessor method to get the current familiar.
	 * @return	familiar
	 */

	public static FamiliarData getFamiliar()
	{	return currentFamiliar;
	}

	/**
	 * Accessor method to set the data for the current familiar.
	 * @param	familiar
	 */

	public static void setFamiliar( FamiliarData familiar )
	{
		currentFamiliar = addFamiliar( familiar );
		familiars.setSelectedItem( currentFamiliar );

		Iterator listenerIterator = listenerList.iterator();
		while ( listenerIterator.hasNext() )
			((KoLCharacterListener)listenerIterator.next()).familiarChanged();
	}

	/**
	 * Accessor method to increment the weight of the current familiar
	 * by one.
	 */

	public static void incrementFamilarWeight()
	{
		if ( currentFamiliar != null )
			currentFamiliar.setWeight( currentFamiliar.getWeight() + 1 );

		Iterator listenerIterator = listenerList.iterator();
		while ( listenerIterator.hasNext() )
			((KoLCharacterListener)listenerIterator.next()).familiarChanged();
	}

	/**
	 * Adds the given familiar to the list of available familiars.
	 * @param	familiar	The ID of the familiar to be added
	 */

	public static FamiliarData addFamiliar( FamiliarData familiar )
	{
		if ( familiar != null )
		{
			int index = familiars.indexOf( familiar );
			if ( index >= 0)
				familiar = (FamiliarData)familiars.get( index );
			else
			{
				familiars.add( familiar );

				// Keep current familiar selected even if new
				// familiar added earlier in list

				familiars.setSelectedItem( currentFamiliar );
			}
		}
		return familiar;
	}

	/**
	 * Returns the list of familiars available to the character.
	 * @return	The list of familiars available to the character
	 */

	public static LockableListModel getFamiliarList()
	{	return familiars;
	}

	/**
	 * Returns the string used on the character pane to detrmine
	 * how many points remain until the character's next level.
	 *
	 * @return	The string indicating the TNP advancement
	 */

	public static String getAdvancement()
	{
		int level = getLevel();
		return df.format( level * level + 4 - calculateBasePoints( getTotalPrime() ) ) + " " + AdventureResult.STAT_NAMES[ getPrimeIndex() ] +
			" until level " + (level + 1);
	}

	/**
	 * Sets the list of accomplishments that the player has
	 * completed.  This value is provided as a complete
	 * string array, where each index is a new accomplishment.
	 *
	 * @param	accomplishments	The accomplishments of this player
	 */

	public static void setAccomplishments( String [] accomplishments )
	{
		KoLCharacter.accomplishments.clear();
		for ( int i = 0; i < accomplishments.length; ++i )
			KoLCharacter.accomplishments.add( accomplishments[i] );
	}

	/**
	 * Adds the given accomplishment to the list of accomplishments.
	 */

	public static void addAccomplishment( String accomplishment )
	{	KoLCharacter.accomplishments.add( accomplishment );
	}

	/**
	 * Returns whether or not the player has the given accomplishment
	 * (the accomplishment being an entire string of text).
	 *
	 * @return	<code>true</code> if the accomplishment exists
	 */

	public static boolean hasAccomplishment( String accomplishment )
	{	return accomplishments.contains( accomplishment );
	}

	/**
	 * Adds a new <code>KoLCharacterListener</code> to the
	 * list of listeners listening to this <code>KoLCharacter</code>.
	 * @param	listener	The listener to be added to the listener list
	 */

	public static void addKoLCharacterListener( KoLCharacterListener listener )
	{
		if ( listener != null && listener.isStatusListener() && !listenerList.contains( listener ) )
			listenerList.add( listener );
	}

	/**
	 * Utility method which forces the update of a group
	 * of results.  This should be called immediately after
	 * the processing of results.
	 */

	public static void refreshCalculatedLists()
	{
		KoLCharacter.updateEquipmentLists();
		ConcoctionsDatabase.refreshConcoctions();

		AdventureResult currentItem;
		sellables.retainAll( inventory );
		usables.retainAll( inventory );

		Iterator itemIterator = inventory.iterator();

		while ( itemIterator.hasNext() )
		{
			currentItem = (AdventureResult) itemIterator.next();

			if ( TradeableItemDatabase.isUsable( currentItem.getName() ) &&
				currentItem.getCount( usables ) != currentItem.getCount() )
			{
				if ( currentItem.getCount( usables ) == 0 )
					usables.add( currentItem );
				else
					usables.set( usables.indexOf( currentItem ), currentItem );
			}

			if ( TradeableItemDatabase.getPriceByID( currentItem.getItemID() ) != 0 &&
				currentItem.getCount( sellables ) != currentItem.getCount() )
			{
				if ( currentItem.getCount( sellables ) == 0 )
					sellables.add( currentItem );
				else
					sellables.set( sellables.indexOf( currentItem ), currentItem );
			}
		}
	}


	/**
	 * Processes a result received through adventuring.
	 * This places items inside of inventories and lots
	 * of other good stuff.
	 */

	public static void processResult( AdventureResult result )
	{
		// If this is a dictionary, adjust battle tactics.

		addDictionary( result );

		// Treat the result as normal from this point forward.
		// Figure out which list the skill should be added to
		// and add it to that list.

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
				AdventureResult [] effectsArray = new AdventureResult[ getEffects().size() ];
				getEffects().toArray( effectsArray );

				for ( int i = effectsArray.length - 1; i >= 0; --i )
				{
					AdventureResult effect = effectsArray[i];
					if ( effect.getCount() <= 0 - result.getCount() )
						getEffects().remove( i );
					else
						getEffects().set( i, effect.getInstance( effect.getCount() + result.getCount() ) );
				}

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

	/**
	 * Returns whether or not the character is wearing an item where
	 * auto-recovery occurs between adventures.
	 */

	public static boolean hasRecoveringEquipment()
	{
		return getEquipment( HAT ).startsWith( "plexiglass" ) || getEquipment( ACCESSORY1 ).startsWith( "ring of half" ) ||
			getEquipment( ACCESSORY2 ).startsWith( "ring of half" ) || getEquipment( ACCESSORY3 ).startsWith( "ring of half" );
	}

	/**
	 * Returns the character's zapping wand, if any
	 */

	public static AdventureResult getZapper()
	{
		// Search for a wand
		Iterator iterator = inventory.iterator();
		while ( iterator.hasNext() )
		{
			AdventureResult item = (AdventureResult)iterator.next();
			if ( TradeableItemDatabase.getConsumptionType( item.getItemID() ) == ConsumeItemRequest.CONSUME_ZAP )
				return item;
		}

		// No wand
		return null;
	}
}
