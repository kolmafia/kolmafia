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
	public static final int FAKEHAND = 9;

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
	private static int fakeHands = 0;
	private static LockableListModel customOutfits = new LockableListModel();
	private static LockableListModel outfits = new LockableListModel();

	static
	{
		for ( int i = 0; i < 8; ++i )
			equipment.add( EquipmentRequest.UNEQUIP );
	}

	private static LockableListModel events = new LockableListModel();

	private static SortedListModel inventory = new SortedListModel( AdventureResult.class );
	private static SortedListModel closet = new SortedListModel( AdventureResult.class );
	private static SortedListModel collection = new SortedListModel( AdventureResult.class );
	private static SortedListModel storage = new SortedListModel( AdventureResult.class );
	private static SortedListModel usables = new SortedListModel( AdventureResult.class );
	private static SortedListModel sellables = new SortedListModel( AdventureResult.class );

	private static LockableListModel activeEffects = new LockableListModel();
	private static SortedListModel usableSkills = new SortedListModel( UseSkillRequest.class );
	private static LockableListModel availableSkills = new LockableListModel();
	private static LockableListModel combatSkills = new LockableListModel();

	private static LockableListModel battleSkillIDs = new LockableListModel();
	private static LockableListModel battleSkillNames = new LockableListModel();

	private static SortedListModel [] equipmentLists = new SortedListModel[9];
	static
	{
		for ( int i = 0; i < 9; ++i )
			equipmentLists[i] = new SortedListModel();
	}

	// Status pane data which is rendered whenever
	// the user issues a "status" type command.

	private static int pvpRank = 0;
	private static int availableMeat = 0;
	private static int closetMeat = 0;
	private static int inebriety = 0;
	private static int adventuresLeft = 0;
	private static int totalTurnsUsed = 0;

	// Status pane data which is rendered whenever
	// the user changes equipment, effects, and familiar

	private static int monsterLevelAdjustment = 0;
	private static double combatPercentAdjustment = 0.0;
	private static double initiativeAdjustment = 0.0;
	private static double fixedXPAdjustment = 0.0;
	private static double meatDropPercentAdjustment = 0.0;
	private static double itemDropPercentAdjustment = 0.0;

	// Travel information

	private static boolean hasStore = false;
	private static boolean hasDisplayCase = false;
	private static boolean hasClan = false;

	// Campground information

	private static boolean hasToaster = false;
	private static boolean hasArches = false;
	private static boolean hasChef = false;
	private static boolean hasBartender = false;

	// Familiar data for reference

	private static SortedListModel familiars = new SortedListModel( FamiliarData.class );
	private static FamiliarData currentFamiliar = FamiliarData.NO_FAMILIAR;
	private static int arenaWins = 0;

	// Listener-driven container items

	private static List listenerList = new ArrayList();
	private static boolean beanstalkArmed;

	// Ascension-related variables

	private static boolean isHardcore = false;
	private static boolean canInteract = true;
	private static int ascensions = 0;
	private static String ascensionSign = "None";
	private static int ascensionSignType = NONE;
	private static int consumptionRestriction = AscensionSnapshotTable.NOPATH;
	private static int mindControlLevel = 0;

	private static String autosellMode = "";

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
		KoLCharacter.username = username;

		classname = "";
		classtype = "";

		pvpRank = 0;
		adjustedStats = new int[3];
		totalSubpoints = new int[3];

		monsterLevelAdjustment = 0;
		combatPercentAdjustment = 0.0;
		initiativeAdjustment = 0.0;
		fixedXPAdjustment = 0.0;
		meatDropPercentAdjustment = 0.0;
		itemDropPercentAdjustment = 0.0;

		equipment.clear();
		for ( int i = 0; i < 8; ++i )
			equipment.add( EquipmentRequest.UNEQUIP );
		fakeHands = 0;

		customOutfits.clear();
		outfits.clear();

		events.clear();

		inventory.clear();
		closet.clear();
		storage.clear();
		collection.clear();

		activeEffects.clear();
		usableSkills.clear();
		availableSkills.clear();

		isHardcore = false;
		canInteract = true;
		hasStore = false;
		hasDisplayCase = false;
		hasClan = false;

		hasToaster = false;
		hasArches = false;
		hasChef = false;
		hasBartender = false;

		familiars.clear();
		familiars.add( FamiliarData.NO_FAMILIAR );
		arenaWins = 0;

		beanstalkArmed = false;

		ascensions = 0;
		ascensionSign = "None";
		ascensionSignType = NONE;

		mindControlLevel = 0;

		autosellMode = "";

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
		return getInebriety() > (hasSkill( "Liver of Steel" ) ? 19 : 14);
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
	{
		RequestEditorKit.downloadImage( "http://images.kingdomofloathing.com/" + avatar );
		return avatar;
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

	public static int getPvpRank()
	{	return pvpRank;
	}

	public static void setPvpRank( int pvpRank )
	{	KoLCharacter.pvpRank = pvpRank;
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
	{	KoLCharacter.closetMeat = closetMeat;
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
	{	KoLCharacter.availableMeat = availableMeat;
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
	{	KoLCharacter.inebriety = inebriety;
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
		updateStatus();
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
	{	KoLCharacter.totalTurnsUsed = totalTurnsUsed;
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
	 * Accessor method to retrieve the total current monster level
	 * adjustment
	 *
	 * @return	Total Current Monster Level Adjustment
	 */

	public static int getMonsterLevelAdjustment()
	{	return monsterLevelAdjustment;
	}

	/**
	 * Accessor method to retrieve the total current combat percent
	 * adjustment
	 *
	 * @return	Total Current Combat Percent Adjustment
	 */

	public static double getCombatPercentAdjustment()
	{	return combatPercentAdjustment;
	}

	/**
	 * Accessor method to retrieve the total current initiative
	 * adjustment
	 *
	 * @return	Total Current Initiative Adjustment
	 */

	public static double getInitiativeAdjustment()
	{	return initiativeAdjustment;
	}

	/**
	 * Accessor method to retrieve the total current fixed XP
	 * adjustment
	 *
	 * @return	Total Current Fixed XP Adjustment
	 */

	public static double getFixedXPAdjustment()
	{	return fixedXPAdjustment;
	}

	/**
	 * Accessor method to retrieve the total current meat drop percent
	 * adjustment
	 *
	 * @return	Total Current Meat Drop Percent Adjustment
	 */

	public static double getMeatDropPercentAdjustment()
	{	return meatDropPercentAdjustment;
	}

	/**
	 * Accessor method to retrieve the total current item drop percent
	 * adjustment
	 *
	 * @return	Total Current Item Drop Percent Adjustment
	 */

	public static double getItemDropPercentAdjustment()
	{	return itemDropPercentAdjustment;
	}

	/**
	 * Accessor method to set the equipment the character is currently using.
	 * This does not take into account the power of the item or anything of
	 * that nature; only the item's name is stored.  Note that if no item is
	 * equipped, the value should be <code>none</code>, not <code>null</code>
	 * or the empty string.
	 *
	 * @param	equipment	All of the available equipment, stored in an array index by the constants
	 * @param	customOutfits	A listing of available outfits
	 */

	public static void setEquipment( String [] equipment, List customOutfits )
	{
		for ( int i = 0; i < KoLCharacter.equipment.size(); ++i )
		{
			if ( i == FAMILIAR )
				continue;

			if ( equipment[i] == null || equipment[i].equals( "none" ) || equipment[i].equals( EquipmentRequest.UNEQUIP ) )
				KoLCharacter.equipment.set( i, EquipmentRequest.UNEQUIP );
			else if ( TradeableItemDatabase.getConsumptionType( equipment[i] ) == ConsumeItemRequest.EQUIP_ACCESSORY )
				KoLCharacter.equipment.set( i, equipment[i] );
			else
				KoLCharacter.equipment.set( i, equipment[i] + " (+" + EquipmentDatabase.getPower( equipment[i] ) + ")" );
		}

		if ( equipment.length > FAMILIAR && currentFamiliar != FamiliarData.NO_FAMILIAR )
			currentFamiliar.setItem( equipment[FAMILIAR] );

		// Rebuild outfits if given a new list
		if ( customOutfits != null )
		{
			KoLCharacter.customOutfits.clear();
			KoLCharacter.customOutfits.addAll( customOutfits );
			EquipmentDatabase.updateOutfits();
		}

		FamiliarData.updateWeightModifier();
		ClassSkillsDatabase.updateManaModifier();
		recalculateAdjustments( false );
		updateStatus();
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

	public static int getFakeHands()
	{	return fakeHands;
	}

	public static void setFakeHands( int hands )
	{	fakeHands = hands;
	}

	/**
	 * Accessor method to retrieve the name of a piece of equipment
	 * @param	type	the type of equipment
	 * @return	String	name of the equipped item or null if none
	 */

	public static String getCurrentEquipmentName( int type )
	{
		if ( type == FAKEHAND )
			return fakeHands > 0 ? "fake hand" : null;
		return getEquipmentName( getEquipment( type ) );
	}
	
	/**
	 * Accessor method to retrieve the name of a piece of equipment
	 * @param	equipmentDescription	the description of equipment
	 * @return	String	name of the equipped item or null if none
	 */

	public static String getEquipmentName( String equipmentDescription )
	{
		if ( equipmentDescription == null )
			return null;
		
		// If slot not currently equipped, return null
		if ( equipmentDescription.equals( EquipmentRequest.UNEQUIP ))
			return null;

		// Strip off item power
		int parenIndex = equipmentDescription.indexOf( " (" );
		return parenIndex == -1 ? equipmentDescription : equipmentDescription.substring( 0, parenIndex );
	}

	/**
	 * Accessor method to retrieve a piece of equipment
	 * @param	type	the type of equipment
	 * @return	AdventureResult for the equipment, <code>null</code> if no such item exists
	 */

	public static AdventureResult getCurrentEquipment( int type )
	{
		String name = getCurrentEquipmentName( type );
		return ( name == null) ? null : new AdventureResult( name, 1, false );
	}

	/**
	 * Accessor method to retrieve # of hands character's weapon uses
	 * @return	int	number of hands needed
	 */

	public static int weaponHandedness()
	{
		String name = getEquipmentName( (String) equipmentLists[ WEAPON ].getSelectedItem() );
		return ( name == null) ? 0 : EquipmentDatabase.getHands( name );
	}

	/**
	 * Accessor method to determine if character's weapon is ranged
	 * @return	boolean	true if weapon is ranged
	 */

	public static boolean rangedWeapon()
	{
		String name = getEquipmentName( (String) equipmentLists[ WEAPON ].getSelectedItem() );
		return name != null && EquipmentDatabase.isRanged( name );
	}

	/**
	 * Accessor method to determine if character is currently dual-wielding
	 * @return	boolean	true if character has two weapons equipped
	 */

	public static boolean dualWielding()
	{
		String name = getEquipmentName( (String) equipmentLists[ OFFHAND ].getSelectedItem() );
		return name != null && EquipmentDatabase.getHands( name ) == 1;
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
		EquipmentDatabase.updateOutfits();
		for ( int i = 0; i <= FAMILIAR; ++i )
			updateEquipmentList( i );
	}

	public static void updateEquipmentList( int listIndex )
	{	updateEquipmentList( listIndex, getEquipment( listIndex ) );
	}

	public static void updateEquipmentList( int listIndex, String equippedItem )
	{
		int consumeFilter = 0;
		switch ( listIndex )
		{
			case HAT:
				consumeFilter = ConsumeItemRequest.EQUIP_HAT;
				break;
			case WEAPON:
				consumeFilter = ConsumeItemRequest.EQUIP_WEAPON;
				break;
			case OFFHAND:
				consumeFilter = ConsumeItemRequest.EQUIP_OFFHAND;
				break;
			case SHIRT:
				consumeFilter = ConsumeItemRequest.EQUIP_SHIRT;
				break;
			case PANTS:
				consumeFilter = ConsumeItemRequest.EQUIP_PANTS;
				break;
			case ACCESSORY1:
			case ACCESSORY2:
			case ACCESSORY3:
				consumeFilter = ConsumeItemRequest.EQUIP_ACCESSORY;
				break;
			case FAMILIAR:
				consumeFilter = ConsumeItemRequest.EQUIP_FAMILIAR;
				break;
			default:
				return;
		}
		
		updateEquipmentList( equipmentLists[ listIndex ], consumeFilter, equippedItem );
	}
		
	private static void updateEquipmentList( LockableListModel currentList, int consumeFilter, String equippedItem )
	{
		List newItems = getFilteredItems( consumeFilter, equippedItem );
		if ( currentList.equals( newItems ) )
			return;
		
		if ( currentList.size() < newItems.size() )
		{
			newItems.removeAll( currentList );
			currentList.addAll( newItems );
		}
		else
		{
			currentList.retainAll( newItems );
		}
		
		currentList.setSelectedItem( equippedItem );
	}

	private static List getFilteredItems( int filterID, String equippedItem )
	{
		List items = new ArrayList();

		// If we are looking for off-hand items, the character is
		// currently equipped with a one-handed melee weapon, and the
		// character has the ability to dual-wield weapons, then also
		// search for one-handed weapons.

		boolean dual = ( filterID == ConsumeItemRequest.EQUIP_OFFHAND &&
			weaponHandedness() == 1 && !rangedWeapon() &&
			hasSkill( "Double-Fisted Skull Smashing" ) );

		// If the character is currently dual wielding, only melee
		// weapons are allowed in the main weapon slot

		boolean dualWielding = dualWielding();

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
			String currentItem = ((AdventureResult)inventory.get(i)).getName();
			int type = TradeableItemDatabase.getConsumptionType( currentItem );

			// If we want off-hand items and we can dual wield,
			// allow one-handed melee weapons

			if ( filterID == ConsumeItemRequest.EQUIP_OFFHAND && dual && type == ConsumeItemRequest.EQUIP_WEAPON )
			{
				if ( !EquipmentDatabase.dualWieldable( currentItem ) )
					continue;
			}

			// Otherwise, slot and item type must match

			else if ( filterID != type )
				continue;

			// If we are currently dual-wielding, only melee
			// weapons are allowed in the main weapon slot
			// Two-handed ranged weapons are also allowed since
			// they will remove both weapons when equipped

			else if ( filterID == ConsumeItemRequest.EQUIP_WEAPON && dualWielding && EquipmentDatabase.isRanged( currentItem ) && EquipmentDatabase.getHands( currentItem ) == 1 )
				continue;

			// If we are equipping familiar items, make sure
			// current familiar can use this one

			if ( type == ConsumeItemRequest.EQUIP_FAMILIAR )
			{
				if ( currentFamiliar.canEquip( currentItem ) )
					items.add( currentItem );
				continue;
			}

			// It's a regular item. Make sure we meet requirements

			if ( !EquipmentDatabase.canEquip( currentItem ) )
				continue;

			if ( filterID != ConsumeItemRequest.EQUIP_ACCESSORY )
				items.add( currentItem + " (+" + EquipmentDatabase.getPower( currentItem ) + ")" );
			else
				items.add( currentItem );
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
	 * Accessor method to retrieve a list of the custom outfits available
	 * to this character, based on the last time the equipment screen was
	 * requested.
	 *
	 * @return	A <code>LockableListModel</code> of the available outfits
	 */

	public static LockableListModel getCustomOutfits()
	{	return customOutfits;
	}

	/**
	 * Accessor method to retrieve a list of the all the outfits available
	 * to this character, based on the last time the equipment screen was
	 * requested.
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
	 * Accessor method which indicates whether or not the character has store in the mall
	 * @return	<code>true</code> if the character has a store
	 */

	public static boolean hasStore()
	{	return hasStore;
	}

	/**
	 * Accessor method to indicate a change in state of the mall store.
	 * @param	hasStore	Whether or not the character currently has a store
	 */

	public static void setStore( boolean hasStore )
	{	KoLCharacter.hasStore = hasStore;
	}

	/**
	 * Accessor method which indicates whether or not the character has display case
	 * @return	<code>true</code> if the character has a display case
	 */

	public static boolean hasDisplayCase()
	{	return hasDisplayCase;
	}

	/**
	 * Accessor method to indicate a change in state of the museum display case
	 * @param	hasDisplayCase	Whether or not the character currently has display case
	 */

	public static void setDisplayCase( boolean hasDisplayCase )
	{	KoLCharacter.hasDisplayCase = hasDisplayCase;
	}

	/**
	 * Accessor method which indicates whether or not the character is in a clan
	 * @return	<code>true</code> if the character is in a clan
	 */

	public static boolean hasClan()
	{	return hasClan;
	}

	/**
	 * Accessor method to indicate a change in state of the character's clan membership
	 * @param	hasClan	Whether or not the character currently is in a clan
	 */

	public static void setClan( boolean hasClan )
	{	KoLCharacter.hasClan = hasClan;
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
	 * Returns whether or not the character is currently in hardcore.
	 */

	public static boolean isHardcore()
	{	return isHardcore;
	}

	/**
	 * Accessor method which sets whether or not the player is currently
	 * in hardcore.
	 */

	public static void setHardcore( boolean isHardcore )
	{	KoLCharacter.isHardcore = isHardcore;
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
	 * @return	int
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
		recalculateAdjustments( false );
		updateStatus();
	}

	/**
	 * Accessor method for the current autosell mode
	 * @return	String
	 */

	public static String getAutosellMode()
	{	return autosellMode;
	}

	/**
	 * Accessor method to set the autosellmode
	 * @param	mode	the new mode
	 */

	public static void setAutosellMode( String mode )
	{
		KoLCharacter.autosellMode = mode;
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
	 * Accessor method to add a listing of the current effects.
	 * @return	A list of current effects
	 */

	public static LockableListModel getEffects()
	{	return activeEffects;
	}

	/**
	 * Accessor method to set the list of available skills.
	 * @param	availableSkills	The list of the names of available skills
	 */

	public static void setAvailableSkills( List availableSkills )
	{
		KoLCharacter.availableSkills.clear();
		KoLCharacter.usableSkills.clear();
		KoLCharacter.combatSkills.clear();
		KoLCharacter.battleSkillIDs.clear();
		KoLCharacter.battleSkillNames.clear();

		// All characters get the option to
		// attack something.

		KoLCharacter.battleSkillIDs.add( "attack" );
		KoLCharacter.battleSkillNames.add( "Normal: Attack with Weapon" );

		// If the player has a dictionary, add it
		// to the available skills list.

		addDictionary();

		// Add the three permanent starting items
		// which can be used to attack.

		KoLCharacter.battleSkillIDs.add( "item0002" );
		KoLCharacter.battleSkillNames.add( "Item: Use a Seal Tooth" );

		KoLCharacter.battleSkillIDs.add( "item0004" );
		KoLCharacter.battleSkillNames.add( "Item: Use a Scroll of Turtle Summoning" );

		KoLCharacter.battleSkillIDs.add( "item0008" );
		KoLCharacter.battleSkillNames.add( "Item: Use Spices" );

		// Add in moxious maneuver if the player
		// is of the appropriate class.

		if ( KoLCharacter.getClassType().startsWith( "Ac" ) || KoLCharacter.getClassType().startsWith( "Di" ) )
		{
			battleSkillIDs.add( "moxman" );
			battleSkillNames.add( "Special: Moxious Maneuver" );
		}

		// Check all available skills to see if they
		// qualify to be added as combat or usables.

		UseSkillRequest [] skillArray = new UseSkillRequest[ availableSkills.size() ];
		availableSkills.toArray( skillArray );

		for ( int i = 0; i < skillArray.length; ++i )
			addAvailableSkill( skillArray[i] );

		// Add derived skills based on base skills
		addDerivedSkills();

		// Set the selected combat skill based on
		// the user's current setting.

		KoLCharacter.battleSkillIDs.add( "custom" );
		KoLCharacter.battleSkillNames.add( "Custom: Use Combat Script" );

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

				// We have the first dictionary.

				if ( dictionary.getCount() > 0 )
				{
					battleSkillIDs.add( 1, "item0536" );
					battleSkillNames.add( 1, "Item: Use a Dictionary" );
					return;
				}
				else if ( dictionary.getCount() == -1 )
				{
					int index = battleSkillIDs.indexOf( "item0536" );
					if ( index != -1 )
					{
						battleSkillIDs.remove( index );
						battleSkillNames.remove( index );
					}
				}

				break;

			case 1316:

				// We have the second dictionary.

				if ( dictionary.getCount() > 0 )
				{
					battleSkillIDs.add( 1, "item1316" );
					battleSkillNames.add( 1, "Item: Use a Dictionary" );
					return;
				}
				else if ( dictionary.getCount() == -1 )
				{
					int index = battleSkillIDs.indexOf( "item1316" );
					if ( index != -1 )
					{
						battleSkillIDs.remove( index );
						battleSkillNames.remove( index );
					}
				}

				break;

			default:
				return;
		}
	}

	/**
	 * Adds a single skill to the list of known skills
	 * possessed by this character.
	 */

	public static void addAvailableSkill( UseSkillRequest skill )
	{
		int id = ClassSkillsDatabase.getSkillID( skill.getSkillName() );

		availableSkills.add( skill );

		switch ( ClassSkillsDatabase.getSkillType( id ) )
		{
		case ClassSkillsDatabase.PASSIVE:
			// Flavour of Magic gives you access to five other
			// castable skills
			if ( skill.getSkillName().equals( "Flavour of Magic" ) )
			{
				usableSkills.add( new UseSkillRequest( client, "Spirit of Cayenne", "", 1 ) );
				usableSkills.add( new UseSkillRequest( client, "Spirit of Peppermint", "", 1 ) );
				usableSkills.add( new UseSkillRequest( client, "Spirit of Garlic", "", 1 ) );
				usableSkills.add( new UseSkillRequest( client, "Spirit of Wormwood", "", 1 ) );
				usableSkills.add( new UseSkillRequest( client, "Spirit of Bacon Grease", "", 1 ) );
			}
			break;

		case ClassSkillsDatabase.SKILL:
		case ClassSkillsDatabase.BUFF:
			usableSkills.add( skill );
			break;

		case ClassSkillsDatabase.COMBAT:
			combatSkills.add( skill );
			battleSkillIDs.add( String.valueOf( id ) );
			battleSkillNames.add( "Skill: " + skill.getSkillName() );
			break;
		}

                recalculateAdjustments( true );
	}

	/**
	 * Adds derived skills to appropriate lists
	 */

	public static void addDerivedSkills()
	{
		if ( KoLCharacter.getClassType().startsWith( "Tu" ) )
		{
			boolean head = hasSkill( "Headbutt" );
			boolean knee = hasSkill( "Kneebutt" );
			boolean shield = hasSkill( "Shieldbutt" );

			if ( head && knee )
				addCombatSkill( "Head + Knee Combo" );
			if ( head && shield )
				addCombatSkill( "Head + Shield Combo" );
			if ( knee && shield )
				addCombatSkill( "Knee + Shield Combo" );
			if ( head && knee && shield )
				addCombatSkill( "Head + Knee + Shield Combo" );
		}
	}

	private static void addCombatSkill( String name )
	{
		// Only add the skill once
		if ( hasSkill( name, combatSkills ) )
			return;

		// Find the skill
		int id = ClassSkillsDatabase.getSkillID( name );
		if ( id == -1 )
			return;

		// Add to lists
		UseSkillRequest skill = new UseSkillRequest( client, name, "", 1 );
		combatSkills.add( skill );
		battleSkillIDs.add( String.valueOf( id ) );
		battleSkillNames.add( "Skill: " + name );
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
	 * Accessor method to look up the list of combat skills.
	 * @return	A list of UseSkillRequests of combat skills
	 */

	public static LockableListModel getCombatSkills()
	{	return combatSkills;
	}

	/**
	 * Accessor method to look up the list of usable skills.
	 * @return	A list of UseSkillRequests of usable skills
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
	 * summon snowcones
	 *
	 * @return	<code>true</code> if snowcones can be summoned by this character
	 */

	public static boolean canSummonSnowcones()
	{	return hasSkill( "Summon Snowcone" );
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

	public static boolean hasSkill( int skillID )
	{
		return hasSkill( ClassSkillsDatabase.getSkillName( skillID ) );
	}

	public static boolean hasSkill( String skillName )
 	{	return hasSkill( skillName, availableSkills );
	}

	public static boolean hasSkill( String skillName, LockableListModel list )
	{
		for ( int i = 0; i < list.size(); ++i )
			if ( ((UseSkillRequest)list.get(i)).getSkillName().equals( skillName ) )
				return true;
		return false;
	}

	public static LockableListModel getAvailableSkills()
	{	return availableSkills;
	}

	/**
	 * Accessor method to get the current familiar.
	 * @return	familiar
	 */

	public static FamiliarData getFamiliar()
	{	return currentFamiliar;
	}

	/**
	 * Accessor method to get arena wins
	 * @return	wins
	 */

	public static int getArenaWins()
	{
		// Ensure that the arena opponent list is
		// initialized.
		
		CakeArenaManager.getOpponentList();
		return arenaWins;
	}

	/**
	 * Accessor method to set arena wins
	 * @parameter	wins
	 */

	public static void setArenaWins( int wins )
	{
		arenaWins = wins;
		updateStatus();
	}

	/**
	 * Accessor method to find the specified familiar.
	 * @param	race
	 * @return	familiar
	 */

	public static FamiliarData findFamiliar( String race )
	{
		FamiliarData [] familiarArray = new FamiliarData[ familiars.size() ];
		familiars.toArray( familiarArray );

		for ( int i = 0; i < familiarArray.length; ++i )
			if ( race.equals( familiarArray[i].getRace() ) )
				return familiarArray[i];

		return null;
	}

	/**
	 * Accessor method to set the data for the current familiar.
	 * @param	familiar
	 */

	public static void setFamiliar( FamiliarData familiar )
	{
		currentFamiliar = addFamiliar( familiar );
		familiars.setSelectedItem( currentFamiliar );
		updateEquipmentList( equipmentLists[FAMILIAR], ConsumeItemRequest.EQUIP_FAMILIAR, getFamiliarItem() );
		recalculateAdjustments( false );
		updateStatus();
	}

	/**
	 * Accessor method to increment the weight of the current familiar
	 * by one.
	 */

	public static void incrementFamilarWeight()
	{
		if ( currentFamiliar != null )
		{
			currentFamiliar.setWeight( currentFamiliar.getWeight() + 1 );
			recalculateAdjustments( false );
			updateStatus();
		}
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
	 * Adds a new <code>KoLCharacterListener</code> to the
	 * list of listeners listening to this <code>KoLCharacter</code>.
	 * @param	listener	The listener to be added to the listener list
	 */

	public static void addCharacterListener( KoLCharacterListener listener )
	{
		if ( listener != null && !listenerList.contains( listener ) )
			listenerList.add( listener );
	}

	/**
	 * Removes an existing <code>KoLCharacterListener</code> from the
	 * list of listeners listening to this <code>KoLCharacter</code>.
	 * @param	listener	The listener to be removed from the listener list
	 */

	public static void removeCharacterListener( KoLCharacterListener listener )
	{
		if ( listener != null )
			listenerList.remove( listener );
	}

	/**
	 * Utility method which forces the update of a group
	 * of results.  This should be called immediately after
	 * the processing of results.
	 */

	public static void refreshCalculatedLists()
	{
		if ( username.equals( "" ) )
			return;

		KoLCharacter.updateEquipmentLists();
		ConcoctionsDatabase.refreshConcoctions();

		sellables.retainAll( inventory );
		usables.retainAll( inventory );

		AdventureResult [] items = new AdventureResult[ inventory.size() ];
		inventory.toArray( items );

		for ( int i = 0; i < items.length; ++i )
		{
			if ( TradeableItemDatabase.isUsable( items[i].getName() ) && items[i].getCount( usables ) != items[i].getCount() )
			{
				if ( items[i].getCount( usables ) == 0 )
					usables.add( items[i] );
				else
					usables.set( usables.indexOf( items[i] ), items[i] );
			}

			int price = TradeableItemDatabase.getPriceByID( items[i].getItemID() );
			if ( ( price > 0 || price == -1 ) && items[i].getCount( sellables ) != items[i].getCount() )
			{
				if ( items[i].getCount( sellables ) == 0 )
					sellables.add( items[i] );
				else
					sellables.set( sellables.indexOf( items[i] ), items[i] );
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

				setTotalTurnsUsed( getTotalTurnsUsed() - result.getCount() );
				if ( getTotalTurnsUsed() >= 600 && !KoLCharacter.isHardcore() )
					setInteraction( true );
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
		}

		// Refresh after every result processing.
		refreshCalculatedLists();
	}

	/**
	 * Returns whether or not the character is wearing an item where
	 * auto-recovery occurs between adventures.
	 */

	public static boolean hasRecoveringEquipment()
	{
		// Plexiglass Pith Helmet
		if ( getEquipment( HAT ).startsWith( "plexiglass pith helmet" ) )
			return true;

		for ( int i = ACCESSORY1; i <= ACCESSORY3; ++i )
		{
			String accessory = getEquipment( i );
			if ( accessory.equals( "ring of half-assed regeneration" ) )
			     return true;
			if ( accessory.compareToIgnoreCase( "tiny plastic crimbo wreath" ) == 0 )
			     return true;
		}

		return false;
	}

	/**
	 * Returns the character's zapping wand, if any
	 */

	public static AdventureResult getZapper()
	{
		AdventureResult [] items = new AdventureResult[ inventory.size() ];
		inventory.toArray( items );

		for ( int i = 0; i < items.length; ++i )
			if ( TradeableItemDatabase.getConsumptionType( items[i].getItemID() ) == ConsumeItemRequest.CONSUME_ZAP )
				return items[i];

		// No wand
		return null;
	}

	public static boolean hasItem( AdventureResult item, boolean shouldCreate )
	{
		if ( item.getCount( getInventory() ) > 0 || item.getCount( getCloset() ) > 0 || hasEquipped( item ) )
			return true;

		if ( shouldCreate )
		{
			ItemCreationRequest creation = ItemCreationRequest.getInstance( client, item.getItemID(), 1 );
			return creation != null && creation.getCount( ConcoctionsDatabase.getConcoctions() ) > 0;
		}

		return false;
	}

	public static boolean hasEquipped( AdventureResult item )
	{
		String name = item.getName();
		switch ( TradeableItemDatabase.getConsumptionType( item.getItemID() ) )
		{
			case ConsumeItemRequest.EQUIP_WEAPON:
				return getEquipment( WEAPON ).startsWith( name ) ||
					getEquipment( OFFHAND ).startsWith( name );

			case ConsumeItemRequest.EQUIP_OFFHAND:
				return getEquipment( OFFHAND ).startsWith( name );

			case ConsumeItemRequest.EQUIP_HAT:
				return getEquipment( HAT ).startsWith( name );

			case ConsumeItemRequest.EQUIP_SHIRT:
				return getEquipment( SHIRT ).startsWith( name );

			case ConsumeItemRequest.EQUIP_PANTS:
				return getEquipment( PANTS ).startsWith( name );

			case ConsumeItemRequest.EQUIP_ACCESSORY:
				return getEquipment( ACCESSORY1 ).startsWith( name ) ||
					getEquipment( ACCESSORY2 ).startsWith( name ) ||
					getEquipment( ACCESSORY3 ).startsWith( name );
		}

		return false;
	}

	public static void updateStatus()
	{
		KoLCharacterListener [] listenerArray = new KoLCharacterListener[ listenerList.size() ];
		listenerList.toArray( listenerArray );

		for ( int i = 0; i < listenerArray.length; ++i )
			listenerArray[i].updateStatus();
	}

	public static LockableListModel getEvents()
	{	return events;
	}

	public static void clearEvents()
	{	events.clear();
	}

	// Effects that modify ML:

	private static final AdventureResult ARIA = new AdventureResult( "Ur-Kel's Aria of Annoyance", 0 );

	// Items that modify ML:

	private static final int ICE_SICKLE = 1424;
	private static final int HIPPO_WHIP = 1029;
	private static final int GIANT_NEEDLE = 619;
	private static final int GOTH_KID = 703;
	private static final int HOCKEY_STICK = 1236;
	private static final int SCARF = 1227;
	private static final int AGGRAVATE_MONSTER = 835;
	private static final int PITCHFORK = 1116;

	// Effects that modify Combat Frequency
	private static final AdventureResult CANTATA = new AdventureResult( "Carlweather's Cantata of Confrontation", 0 );
	private static final AdventureResult SONATA = new AdventureResult( "The Sonata of Sneakiness", 0 );
	private static final AdventureResult MUSK = new AdventureResult( "Musk of the Moose", 0 );
	private static final AdventureResult SMOOTH = new AdventureResult( "Smooth Movements", 0 );
	private static final AdventureResult STENCH = new AdventureResult( "Hippy Stench", 0 );
	private static final AdventureResult SCENT = new AdventureResult( "Fresh Scent", 0 );

	// Items that modify Combat Frequency
	private static final int CONFLICT = 1298;
	private static final int BAIT = 1300;

	// Effects that modify Initiative
  	private static final AdventureResult FUSILLI = new AdventureResult( "Springy Fusilli", 0 );
	private static final AdventureResult CANTICLE = new AdventureResult( "Cletus's Canticle of Celerity", 0 );
	private static final AdventureResult TICKING_CLOCK = new AdventureResult( "Ticking Clock", 0 );
	private static final AdventureResult RELAXATION = new AdventureResult( "Extreme Muscle Relaxation", 0 );

	// Skills that modify Initiative
	private static final String SELF_PRESERVATION = "Overdeveloped Sense of Self Preservation";

	// Items that modify Initiative
	private static final int BONERDAGON_NECKLACE = 1248;
	private static final int CRIMBO_PANTS = 650;
	private static final int CHOPSTICKS = 1490;
	private static final int PENGUIN_SHORTS = 396;
	private static final int PENGUIN_KILT = 1281;
	private static final int PENGUIN_SKIRT = 1280;
	private static final int STAR_PANTS = 660;
	private static final int COLD_NINJA_MASK = 348;
	private static final int ICE_SKATES = 1427;
	private static final int LEOTARRD = 415;
	private static final int PIXEL_SWORD = 690;
	private static final int SK8BOARD = 410;
	private static final int CLOCKWORK_PANTS = 1099;
	private static final int CROWBARRR = 414;
	private static final int GNATWING_EARRING = 505;
	private static final int INFERNAL_INSOLES = 477;
	private static final int PLASTIC_FORK = 1151;
	private static final int SHOVEL = 671;

	// Effects that modify earned XP:

	private static final AdventureResult ANTIPHON = new AdventureResult( "Aloysius' Antiphon of Aptitude", 0 );
	private static final AdventureResult BLACK_TONGUE = new AdventureResult( "Black Tongue", 0 );
	private static final AdventureResult ORANGE_TONGUE = new AdventureResult( "Orange Tongue", 0 );
	private static final AdventureResult VEINY = new AdventureResult( "Big Veiny Brain", 0 );
	private static final AdventureResult PEELED = new AdventureResult( "Peeled Eyeballs", 0 );
	private static final AdventureResult WASABI = new AdventureResult( "Wasabi Sinuses", 0 );

	// Items that modify earned XP:

	private static final int ICE_BABY = 1425;
	private static final int WAX_LIPS = 1260;

	// Familiars that modify earned XP:

	private static final int VOLLEYBALL = 12;
	private static final int CHESHIRE = 23;
	private static final int JILL = 24;
	private static final int SHAMAN = 39;
	private static final int MONKEY = 42;
	private static final int HARE = 50;
	private static final int HOBO = 52;

	// Effects that modify Meat Drops
	private static final AdventureResult RED_TONGUE = new AdventureResult( "Red Tongue", 0 );
	private static final AdventureResult TACTICS = new AdventureResult( "Eggs-stortionary Tactics", 0 );
	private static final AdventureResult POLKA = new AdventureResult( "Polka of Plenty", 0 );

	// Skills that modify Meat Drops
	private static final String NIMBLE_FINGERS = "Nimble Fingers";
	private static final String PANHANDLING = "Expert Panhandling";
	private static final String PICKPOCKETING = "Gnefarious Pickpocketing";

	// Items that modify Meat Drops
	private static final int TAM1 = 1040;
	private static final int TAM2 = 1539;
	private static final int MEAT_GEM = 876;
	private static final int STAINLESS_SLACKS = 1228;
	private static final int MONEY_CLIP = 1313;
	private static final int PORQUOISE_NECKLACE = 720;
	private static final int TOY_TRAIN = 1399;
	private static final int TP_HERMIT = 990;
	private static final int PENGUIN_WHIP = 1030;
	private static final int BOX_IN_BOX_IN_BOX = 447;
	private static final int BOX_IN_BOX = 445;
	private static final int BOX = 427;

	// Familiars that modify Meat Drops
	private static final int LEPRECHAUN = 2;
	private static final int TURKEY = 25;

	// Effects that modify Item Drops
	private static final AdventureResult PERCEPTION = new AdventureResult( "Eggs-tra Sensory Perception", 0 );
	private static final AdventureResult BLUE_TONGUE = new AdventureResult( "Blue Tongue", 0 );
	private static final AdventureResult PHAT_LOOT = new AdventureResult( "Fat Leon's Phat Loot Lyric", 0 );
	private static final AdventureResult OBJECT_DETECTION = new AdventureResult( "Object Detection", 0 );

	// Skills that modify Item Drops
	private static final String MAD_LOOTING_SKILLZ = "Mad Looting Skillz";
	private static final String OBSERVATIOGN = "Powers of Observatiogn";

	// Items that modify Item Drops
	private static final int JEKYLLIN = 1291;
	private static final int HYPNODISK = 878;
	private static final int MAYPOLE = 1152;
	private static final int MRAJR = 896;
	private static final int PLEXIGLASS_PANTS = 1234;
	private static final int RICE_BOWL = 1491;
	private static final int ICE_PICK = 1426;
	private static final int MONOCLE = 1312;
	private static final int DUCK = 1396;
	private static final int HELMET = 360;
	private static final int GNAUGA_WHIP = 1032;

	// KoLmafia does not support the "containers" slot.

	// Mr. Container (482): +3%
	// hemp backbacp (218): +2%
	// Newbiesport&trade; backpack (483): +1%

	// Familiars that modify Item Drops
	private static final int BABY_GRAVY_FAIRY = 15;
	private static final int FLAMING_GRAVY_FAIRY = 34;
	private static final int FROZEN_GRAVY_FAIRY = 35;
	private static final int STINKY_GRAVY_FAIRY = 36;
	private static final int SPOOKY_GRAVY_FAIRY = 37;
	private static final int SLEAZY_GRAVY_FAIRY = 49;
	private static final int PIXIE = 22;
	private static final int DEMON = 41;
	private static final int CRIMBO_ELF = 26;

	public static boolean recalculateAdjustments( boolean update )
	{
		int newMonsterLevelAdjustment = 0;
		double newCombatPercentAdjustment = 0.0;
		double newInitiativeAdjustment = 0.0;
		double newFixedXPAdjustment = 0.0;
		double newMeatDropPercentAdjustment = 0.0;
		double newItemDropPercentAdjustment = 0.0;

		// Look at mind control level
		newMonsterLevelAdjustment += getMindControlLevel();

		// Look at items
		for ( int slot = WEAPON; slot <= FAMILIAR; ++slot )
		{
			AdventureResult item = getCurrentEquipment( slot );
			if ( item == null )
				continue;

			switch ( item.getItemID() )
			{
			case HOCKEY_STICK:
				newMonsterLevelAdjustment += 30;
				break;
			case SCARF:
				newMonsterLevelAdjustment += 20;
				break;
			case ICE_SICKLE:
				newMonsterLevelAdjustment += 15;
				break;
			case HIPPO_WHIP:
				newMonsterLevelAdjustment += 10;
				break;
			case GIANT_NEEDLE:
			case GOTH_KID:
			case AGGRAVATE_MONSTER:
			case PITCHFORK:
				newMonsterLevelAdjustment += 5;
				break;
			case BAIT:
				newCombatPercentAdjustment += 5;
				break;
			case CONFLICT:
				newCombatPercentAdjustment -= 5;
				break;
			case BONERDAGON_NECKLACE:
			case CRIMBO_PANTS:
				newInitiativeAdjustment += 30;
				break;
			case CHOPSTICKS:
			case PENGUIN_SHORTS:
			case PENGUIN_KILT:
			case PENGUIN_SKIRT:
				newInitiativeAdjustment += 20;
				break;
			case COLD_NINJA_MASK:
			case LEOTARRD:
			case PIXEL_SWORD:
			case SK8BOARD:
				newInitiativeAdjustment += 15;
				break;
			case CLOCKWORK_PANTS:
			case CROWBARRR:
			case GNATWING_EARRING:
			case INFERNAL_INSOLES:
				newInitiativeAdjustment += 10;
				break;
			case PLASTIC_FORK:
			case SHOVEL:
				newInitiativeAdjustment -= 10;
				break;
			case WAX_LIPS:
				newFixedXPAdjustment += 2.5;
				break;
			case ICE_BABY:
				newFixedXPAdjustment += 1.0;
				break;
			case TAM1:
			case TAM2:
				newMeatDropPercentAdjustment += 50;
				break;
			case MEAT_GEM:
				newMeatDropPercentAdjustment += 25;
				break;
			case STAINLESS_SLACKS:
				newInitiativeAdjustment += 15;
				newMeatDropPercentAdjustment += 20;
				break;
			case ICE_SKATES:
				newInitiativeAdjustment += 15;
				newMeatDropPercentAdjustment += 15;
				break;
			case MONEY_CLIP:
				newMeatDropPercentAdjustment += 15;
				break;
			case PORQUOISE_NECKLACE:
			case TOY_TRAIN:
				newMeatDropPercentAdjustment += 10;
				break;
			case TP_HERMIT:
				newMeatDropPercentAdjustment += 6;
				break;
			case PENGUIN_WHIP:
			case BOX_IN_BOX_IN_BOX:
				newMeatDropPercentAdjustment += 3;
				break;
			case BOX_IN_BOX:
				newMeatDropPercentAdjustment += 2;
				break;
			case BOX:
				newMeatDropPercentAdjustment += 1;
				break;
			case JEKYLLIN:
				// newItemDropPercentAdjustment depends on moon phase
				break;
			case HYPNODISK:
			case MAYPOLE:
			case MRAJR:
				newItemDropPercentAdjustment += 25;
				break;
			case PLEXIGLASS_PANTS:
				newItemDropPercentAdjustment += 20;
				newInitiativeAdjustment += 25;
				break;
			case RICE_BOWL:
				// newItemDropPercentAdjustment += 20;
				// Food items only
				break;
			case ICE_PICK:
				newItemDropPercentAdjustment += 15;
				break;
			case MONOCLE:
				newItemDropPercentAdjustment += 12.5;
				break;
			case DUCK:
			case HELMET:
				newItemDropPercentAdjustment += 5;
				break;
			case GNAUGA_WHIP:
				newItemDropPercentAdjustment += 3;
				break;
			}
		}

		// Look at skills

		// Assume the character knows 50 skills and we have 10 that
		// we're checking.
		//
		// If we iterate over the 50 skills, we make 10 string
		// comparisons per skill, for a total of 500 comparisons
		//
		// If we look up the 10 skills one after the other, depending
		// on how the skills are stored and whether the character knows
		// it, it will take anywhere from 1 to 50 string comparisons
		//
		// Therefore, we'll simply call "hasSkill" on each skill of
		// interest.

		UseSkillRequest [] skills = new UseSkillRequest[ availableSkills.size() ];
		availableSkills.toArray( skills );

		if ( hasSkill( SELF_PRESERVATION ) )
			newInitiativeAdjustment += 20;

		if ( hasSkill( NIMBLE_FINGERS ) )
			newMeatDropPercentAdjustment += 20;

		if ( hasSkill( PANHANDLING ) || hasSkill( PICKPOCKETING ) )
			newMeatDropPercentAdjustment += 10;

		if ( hasSkill( MAD_LOOTING_SKILLZ ) )
			newItemDropPercentAdjustment += 20;

		if ( hasSkill( OBSERVATIOGN ) )
			newItemDropPercentAdjustment += 10;

		// Look at status effects

		// A similar argument holds for why we should simply look up
		// effects rather iterate over the list.

		if ( ARIA.getCount( activeEffects ) > 0 )
			newMonsterLevelAdjustment += 2 * getLevel();

		if ( ANTIPHON.getCount( activeEffects ) > 0 )
			newFixedXPAdjustment += 3;

		if ( PHAT_LOOT.getCount( activeEffects ) > 0 )
			newItemDropPercentAdjustment += 20;

		if ( POLKA.getCount( activeEffects ) > 0 )
			newMeatDropPercentAdjustment += 50;

		if ( CANTATA.getCount( activeEffects ) > 0 )
			newCombatPercentAdjustment += 5;

		if ( SONATA.getCount( activeEffects ) > 0 )
			newCombatPercentAdjustment -= 5;

		if ( CANTICLE.getCount( activeEffects ) > 0 )
			newInitiativeAdjustment += 30;

		if ( FUSILLI.getCount( activeEffects ) > 0 )
			newInitiativeAdjustment += 40;

		if ( TICKING_CLOCK.getCount( activeEffects ) > 0 )
			newInitiativeAdjustment += 20;

		if ( RELAXATION.getCount( activeEffects ) > 0 )
			newInitiativeAdjustment -= 25;

		if ( MUSK.getCount( activeEffects ) > 0 )
			newCombatPercentAdjustment += 5;

		if ( SMOOTH.getCount( activeEffects ) > 0 )
			newCombatPercentAdjustment -= 5;

		if ( STENCH.getCount( activeEffects ) > 0 )
			newCombatPercentAdjustment += 5;

		if ( SCENT.getCount( activeEffects ) > 0 )
			newCombatPercentAdjustment -= 5;

		// Only one snowcone effect can be active at a time

		if ( BLACK_TONGUE.getCount( activeEffects ) > 0 )
		{
			newFixedXPAdjustment += 2.5;
			newItemDropPercentAdjustment += 30;
			newMeatDropPercentAdjustment += 30;
		}
		else if ( RED_TONGUE.getCount( activeEffects ) > 0 )
			newMeatDropPercentAdjustment += 30;
		else if ( BLUE_TONGUE.getCount( activeEffects ) > 0 )
			newItemDropPercentAdjustment += 30;
		else if ( ORANGE_TONGUE.getCount( activeEffects ) > 0 )
			newFixedXPAdjustment += 2.5;

		if ( PERCEPTION.getCount( activeEffects ) > 0 )
			newItemDropPercentAdjustment += 30;

		if ( TACTICS.getCount( activeEffects ) > 0 )
			newMeatDropPercentAdjustment += 50;

		if ( VEINY.getCount( activeEffects ) > 0 )
		{
			newFixedXPAdjustment += 2;
			newMeatDropPercentAdjustment -= 20;
		}

		if ( PEELED.getCount( activeEffects ) > 0 )
		{
			newItemDropPercentAdjustment += 40;
			newMeatDropPercentAdjustment -= 16;
			newFixedXPAdjustment -= 1;
		}

		if ( WASABI.getCount( activeEffects ) > 0 )
		{
			newMeatDropPercentAdjustment += 30;
			newItemDropPercentAdjustment -= 10;
			newFixedXPAdjustment -= 1;
		}

		if ( OBJECT_DETECTION.getCount( activeEffects ) > 0 )
			newItemDropPercentAdjustment += 12.5;

		// Look at familiar
		double modifier = (double)currentFamiliar.getModifiedWeight();
		switch ( currentFamiliar.getID() )
		{
		case BABY_GRAVY_FAIRY:
		case FLAMING_GRAVY_FAIRY:
		case FROZEN_GRAVY_FAIRY:
		case STINKY_GRAVY_FAIRY:
		case SPOOKY_GRAVY_FAIRY:
		case SLEAZY_GRAVY_FAIRY:
		case CRIMBO_ELF:
			// Full gravy fairy equivalent familiar
			newItemDropPercentAdjustment += modifier * 2.5;
			break;

		case PIXIE:
		case DEMON:
			// Full gravy fairy equivalent familiar
			// Full leprechaun equivalent familiar
			newItemDropPercentAdjustment += modifier * 2.5;
			newMeatDropPercentAdjustment += modifier * 5;
			break;

		case VOLLEYBALL:
		case HOBO:
			// Full volleyball equivalent familiar
			newFixedXPAdjustment += modifier / 4.0;
			break;

		case LEPRECHAUN:
		case TURKEY:
			// Full leprechaun equivalent familiar
			newMeatDropPercentAdjustment += modifier * 5;
			break;

		case CHESHIRE:
		case MONKEY:
			// Full volleyball equivalent familiar
			// Full leprechaun equivalent familiar
			newFixedXPAdjustment += modifier / 4.0;
			newMeatDropPercentAdjustment += modifier * 5;
			break;

		case SHAMAN:
			// Full volleyball equivalent familiar
			// Full gravy fairy equivalent familiar
			newFixedXPAdjustment += modifier / 4.0;
			newItemDropPercentAdjustment += modifier / 40.0;
			break;

		case JILL:
			// Half volleyball equivalent familiar
			newFixedXPAdjustment += modifier / 8.0;
			break;

		case HARE:
			// Full volleyball equivalent 1/4 of the time
			newFixedXPAdjustment += modifier / 16.0;
			break;
		}

		boolean changed = false;
		if ( monsterLevelAdjustment != newMonsterLevelAdjustment )
		{
			monsterLevelAdjustment = newMonsterLevelAdjustment;
			changed = true;
		}

		if ( combatPercentAdjustment != newCombatPercentAdjustment )
		{
			combatPercentAdjustment = newCombatPercentAdjustment;
			changed = true;
		}

		if ( initiativeAdjustment != newInitiativeAdjustment )
		{
			initiativeAdjustment = newInitiativeAdjustment;
			changed = true;
		}

		if ( fixedXPAdjustment != newFixedXPAdjustment )
		{
			fixedXPAdjustment = newFixedXPAdjustment;
			changed = true;
		}

		if ( meatDropPercentAdjustment != newMeatDropPercentAdjustment )
		{
			meatDropPercentAdjustment = newMeatDropPercentAdjustment;
			changed = true;
		}

		if ( itemDropPercentAdjustment != newItemDropPercentAdjustment )
		{
			itemDropPercentAdjustment = newItemDropPercentAdjustment;
			changed = true;
		}

		if ( changed && update )
			updateStatus();

		return changed;
	}
}
