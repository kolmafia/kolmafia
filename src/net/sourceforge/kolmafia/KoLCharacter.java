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

import java.util.regex.Pattern;
import java.util.regex.Matcher;

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
	private static int familiarWeightAdjustment = 0;
	private static int dodecapedeWeightAdjustment = 0;
	private static int familiarItemWeightAdjustment = 0;
	private static int manaCostModifier = 0;
	private static double combatPercentAdjustment = 0.0;
	private static double initiativeAdjustment = 0.0;
	private static double fixedXPAdjustment = 0.0;
	private static double meatDropPercentAdjustment = 0.0;
	private static double itemDropPercentAdjustment = 0.0;
	private static boolean rigatoniActive = false;
	private static int damageAbsorption = 0;
	private static int damageReduction = 0;
	private static double coldResistance = 0;
	private static double hotResistance = 0;
	private static double sleazeResistance = 0;
	private static double spookyResistance = 0;
	private static double stenchResistance = 0;

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
	private static int stillsAvailable = 0;

	// Listener-driven container items

	private static List listenerList = new ArrayList();
	private static boolean beanstalkArmed = false;
	private static boolean spleenLimitReached = false;

	// Ascension-related variables

	private static boolean isHardcore = false;
	private static boolean canInteract = true;

	private static int ascensions = 0;
	private static String ascensionSign = "None";
	private static int ascensionSignType = NONE;
	private static int consumptionRestriction = AscensionSnapshotTable.NOPATH;
	private static int mindControlLevel = 0;

	private static String autosellMode = "";
	private static KoLAdventure nextAdventure = null;

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
		familiarWeightAdjustment = 0;
		dodecapedeWeightAdjustment = 0;
		familiarItemWeightAdjustment = 0;
		manaCostModifier = 0;
		combatPercentAdjustment = 0.0;
		initiativeAdjustment = 0.0;
		fixedXPAdjustment = 0.0;
		meatDropPercentAdjustment = 0.0;
		itemDropPercentAdjustment = 0.0;
		rigatoniActive = false;
		damageAbsorption = 0;
		damageReduction = 0;
		coldResistance = 0;
		hotResistance = 0;
		sleazeResistance = 0;
		spookyResistance = 0;
		stenchResistance = 0;

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
		client.resetBreakfastSummonings();

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

		stillsAvailable = -1;
		beanstalkArmed = false;
		spleenLimitReached = false;

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

		// Clear the violet fog path table
		VioletFog.reset();
	}

	public static KoLAdventure getNextAdventure()
	{	return nextAdventure;
	}

	public static void setNextAdventure( KoLAdventure next )
	{	nextAdventure = next;
	}

	public static boolean isFallingDown()
	{	return getInebriety() > (hasSkill( "Liver of Steel" ) ? 19 : 14);
	}

	/**
	 * Accessor method to retrieve the name of this character.
	 * @return	The name of this character
	 */

	public static String getUsername()
	{	return username;
	}

	public static String baseUserName()
	{	return baseUserName( username);
	}

	public static String baseUserName( String name )
	{	return name.replaceAll( "\\/q", "" ).trim().replaceAll( " ", "_" ).toLowerCase();
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

	public static boolean isMuscleClass()
	{	return SEAL_CLUBBER.contains( classname ) || TURTLE_TAMER.contains( classname );
	}

	public static boolean isMysticalityClass()
	{	return PASTAMANCER.contains( classname ) || SAUCEROR.contains( classname );
	}

	public static boolean isMoxieClass()
	{	return DISCO_BANDIT.contains( classname ) || ACCORDION_THIEF.contains( classname );
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
	 * Accessor method to retrieve the total current familiar weight
	 * adjustment
	 *
	 * @return	Total Current Familiar Weight Adjustment
	 */

	public static int getFamiliarWeightAdjustment()
	{	return familiarWeightAdjustment;
	}

	public static int getDodecapedeWeightAdjustment()
	{	return dodecapedeWeightAdjustment;
	}

	public static int getFamiliarItemWeightAdjustment()
	{	return familiarItemWeightAdjustment;
	}

	public static int getManaCostModifier()
	{	return manaCostModifier;
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
			{
				KoLCharacter.equipment.set( i, EquipmentRequest.UNEQUIP );
				equipmentLists[i].setSelectedItem( EquipmentRequest.UNEQUIP );
			}
			else
			{
				if ( TradeableItemDatabase.getConsumptionType( equipment[i] ) == ConsumeItemRequest.EQUIP_ACCESSORY )
					KoLCharacter.equipment.set( i, equipment[i] );
				else
					KoLCharacter.equipment.set( i, equipment[i] + " (+" + EquipmentDatabase.getPower( equipment[i] ) + ")" );

				for ( int j = 0; j < equipmentLists[i].size(); ++j )
				{
					if ( equipmentLists[i].get(j).toString().startsWith( equipment[i] ) )
					{
						equipmentLists[i].setSelectedIndex(j);
						break;
					}
				}
			}
		}

		addJoybuzzer();

		if ( equipment.length > FAMILIAR && currentFamiliar != FamiliarData.NO_FAMILIAR )
		{
			currentFamiliar.setItem( equipment[FAMILIAR] );
		}

		// Rebuild outfits if given a new list
		if ( customOutfits != null )
		{
			KoLCharacter.customOutfits.clear();
			KoLCharacter.customOutfits.addAll( customOutfits );
			EquipmentDatabase.updateOutfits();
		}

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
		String name = getCurrentEquipmentName( WEAPON );
		return ( name == null) ? 0 : EquipmentDatabase.getHands( name );
	}

	/**
	 * Accessor method to determine if character's weapon is ranged
	 * @return	boolean	true if weapon is ranged
	 */

	public static boolean rangedWeapon()
	{
		String name = getCurrentEquipmentName( WEAPON );
		return name != null && EquipmentDatabase.isRanged( name );
	}

	/**
	 * Accessor method to determine if character is using Spirit of Rigatoni
	 * @return	boolean	true if wielding a staff and has skill
	 */

	public static boolean rigatoniActive()
	{	return rigatoniActive;
	}

	/**
	 * Accessor method to retrieve the total current damage absorption
	 *
	 * @return	Total Current Damage Absorption
	 */

	public static int getDamageAbsorption()
	{	return damageAbsorption;
	}

	/**
	 * Accessor method to retrieve the total current damage reduction
	 *
	 * @return	Total Current Damage Reduction
	 */

	public static int getDamageReduction()
	{	return damageReduction;
	}

	/**
	 * Accessor method to retrieve the total current cold resistance
	 *
	 * @return	Total Current Cold Resistance
	 */

	public static double getColdResistance()
	{	return coldResistance;
	}

	/**
	 * Accessor method to retrieve the total current hot resistance
	 *
	 * @return	Total Current Hot Resistance
	 */

	public static double getHotResistance()
	{	return hotResistance;
	}

	/**
	 * Accessor method to retrieve the total current sleazw resistance
	 *
	 * @return	Total Current Sleaze Resistance
	 */

	public static double getSleazeResistance()
	{	return sleazeResistance;
	}

	/**
	 * Accessor method to retrieve the total current spooky resistance
	 *
	 * @return	Total Current Spooky Resistance
	 */

	public static double getSpookyResistance()
	{	return spookyResistance;
	}

	/**
	 * Accessor method to retrieve the total current stench resistance
	 *
	 * @return	Total Current Stench Resistance
	 */

	public static double getStenchResistance()
	{	return stenchResistance;
	}

	/**
	 * Accessor method to determine if character is currently dual-wielding
	 * @return	boolean	true if character has two weapons equipped
	 */

	public static boolean dualWielding()
	{
		String name = getCurrentEquipmentName( OFFHAND );
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

		currentList.clear();
		currentList.addAll( newItems );
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

	public static boolean spleenLimitReached()
	{	return spleenLimitReached;
	}

	public static void reachSpleenLimit()
	{	KoLCharacter.spleenLimitReached = true;
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
	{	KoLCharacter.autosellMode = mode;
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
		if ( availableSkills == KoLCharacter.availableSkills )
		{
			addDictionary();
			return;
		}

		KoLCharacter.availableSkills.clear();
		KoLCharacter.usableSkills.clear();
		KoLCharacter.combatSkills.clear();
		KoLCharacter.battleSkillIDs.clear();
		KoLCharacter.battleSkillNames.clear();

		// All characters get the option to
		// attack something.

		KoLCharacter.battleSkillIDs.add( "attack" );
		KoLCharacter.battleSkillNames.add( "Normal: Attack with Weapon" );

		if ( KoLCharacter.getClassType().startsWith( "Di" ) || KoLCharacter.getClassType().startsWith( "At" ) )
			addAvailableSkill( new UseSkillRequest( StaticEntity.getClient(), "Moxious Maneuver", "", 1 ) );

		// If the player has a dictionary, add it
		// to the available skills list.

		addDictionary();

		// Add the three permanent starting items
		// which can be used to attack.

		KoLCharacter.battleSkillIDs.add( "item seal tooth" );
		KoLCharacter.battleSkillNames.add( "Item: Use a Seal Tooth" );

		KoLCharacter.battleSkillIDs.add( "item scroll of turtle summoning" );
		KoLCharacter.battleSkillNames.add( "Item: Use a Scroll of Turtle Summoning" );

		KoLCharacter.battleSkillIDs.add( "item spices" );
		KoLCharacter.battleSkillNames.add( "Item: Use Spices" );

		// Check all available skills to see if they
		// qualify to be added as combat or usables.

		UseSkillRequest [] skillArray = new UseSkillRequest[ availableSkills.size() ];
		availableSkills.toArray( skillArray );

		for ( int i = 0; i < skillArray.length; ++i )
			addAvailableSkill( skillArray[i] );

		addJoybuzzer();

		// Superhuman Cocktailcrafting affects # of summons for
		// Advanced Cocktailcrafting
		if ( hasSkill( "Superhuman Cocktailcrafting" ) )
			client.setBreakfastSummonings( KoLmafia.COCKTAILCRAFTING, 5 );

		// Transcendental Noodlecraft affects # of summons for
		// Pastamastery
		if ( hasSkill( "Transcendental Noodlecraft" ) )
			client.setBreakfastSummonings( KoLmafia.PASTAMASTERY, 5 );

		// The Way of Sauce affects # of summons for
		// Advanced Saucecrafting
		if ( hasSkill( "The Way of Sauce" ) )
			client.setBreakfastSummonings( KoLmafia.SAUCECRAFTING, 5 );

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

	public static void addJoybuzzer()
	{
		UseSkillRequest handshake = new UseSkillRequest( StaticEntity.getClient(), "Shake Hands", "", 1 );

		if ( getEquipment( OFFHAND ).startsWith( "joybuzzer" ) )
		{
			if ( !KoLCharacter.battleSkillIDs.contains( "skill shake hands" ) )
			{
				KoLCharacter.battleSkillIDs.add( "skill shake hands" );
				KoLCharacter.battleSkillNames.add( "Skill: Shake Hands" );

				availableSkills.add( handshake );
				combatSkills.add( handshake );
			}
		}
		else
		{
			int index = KoLCharacter.battleSkillIDs.indexOf( "skill shake hands" );
			if ( index != -1 )
			{
				KoLCharacter.battleSkillIDs.remove( index );
				KoLCharacter.battleSkillNames.remove( index );

				availableSkills.remove( handshake );
				combatSkills.remove( handshake );
			}
		}
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
					battleSkillIDs.add( 1, "item dictionary" );
					battleSkillNames.add( 1, "Item: Use a Dictionary" );
					return;
				}
				else if ( dictionary.getCount() == -1 )
				{
					int index = battleSkillIDs.indexOf( "item dictionary" );
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
					battleSkillIDs.add( 1, "item facsimile dictionary" );
					battleSkillNames.add( 1, "Item: Use a Facsimile Dictionary" );
					return;
				}
				else if ( dictionary.getCount() == -1 )
				{
					int index = battleSkillIDs.indexOf( "item facsimile dictionary" );
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
		availableSkills.add( skill );

		switch ( ClassSkillsDatabase.getSkillType( skill.getSkillID() ) )
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
				battleSkillIDs.add( "skill " + skill.getSkillName().toLowerCase() );
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

		// Add to lists
		UseSkillRequest skill = new UseSkillRequest( client, name, "", 1 );

		combatSkills.add( skill );
		battleSkillIDs.add( "skill " + name.toLowerCase() );
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
			if ( ((UseSkillRequest)list.get(i)).getSkillName().equalsIgnoreCase( skillName ) )
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

	public static int getStillsAvailable()
	{
		if ( stillsAvailable == -1 )
		{
			boolean canStill = hasSkill( "Superhuman Cocktailcrafting" ) && isMoxieClass();
			if ( !canStill )
			{
				stillsAvailable = 0;
				return 0;
			}

			KoLRequest request = new KoLRequest( StaticEntity.getClient(), "guild.php?place=still" );
			request.run();

			setStillsAvailable( request.responseText );
		}

		return stillsAvailable;
	}

	public static void setStillsAvailable( String responseText )
	{
		Matcher stillMatcher = Pattern.compile(
			"lack readout with (\\d+) bright green light" ).matcher( responseText );

		if ( stillMatcher.find() )
			stillsAvailable = StaticEntity.parseInt( stillMatcher.group(1) );
		else
			stillsAvailable = 0;
	}

	public static boolean canUseWok()
	{	return hasSkill( "Transcendental Noodlecraft" ) && isMysticalityClass();
	}

	public static boolean canUseMalus()
	{	return hasSkill( "Pulverize" ) && isMuscleClass();
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
		return COMMA_FORMAT.format( level * level + 4 - calculateBasePoints( getTotalPrime() ) ) + " " + AdventureResult.STAT_NAMES[ getPrimeIndex() ] +
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
		{
			AdventureResult.addResultToList( inventory, result );
			refreshCalculatedLists();
		}
		else if ( resultName.equals( AdventureResult.HP ) )
			setHP( getCurrentHP() + result.getCount(), getMaximumHP(), getBaseMaxHP() );
		else if ( resultName.equals( AdventureResult.MP ) )
			setMP( getCurrentMP() + result.getCount(), getMaximumMP(), getBaseMaxMP() );
		else if ( resultName.equals( AdventureResult.MEAT ) )
		{
			setAvailableMeat( getAvailableMeat() + result.getCount() );
			refreshCalculatedLists();
		}
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
		int count = hasEquipped( item ) ? 1 : 0;
		count += item.getCount( getInventory() ) + item.getCount( getCloset() );

		if ( count > 0 && count >= item.getCount() )
			return true;

		if ( shouldCreate )
		{
			ItemCreationRequest creation = ItemCreationRequest.getInstance( client, item.getItemID(), 1 );
			if ( creation == null )
				return false;

			count += creation.getCount( ConcoctionsDatabase.getConcoctions() );
		}

		return count > 0 && count >= item.getCount();
	}

	public static boolean hasEquipped( AdventureResult item )
	{
		String name = item.getName().toLowerCase();
		switch ( TradeableItemDatabase.getConsumptionType( item.getItemID() ) )
		{
			case ConsumeItemRequest.EQUIP_WEAPON:
				return getEquipment( WEAPON ).toLowerCase().startsWith( name ) || getEquipment( OFFHAND ).toLowerCase().startsWith( name );

			case ConsumeItemRequest.EQUIP_OFFHAND:
				return getEquipment( OFFHAND ).toLowerCase().startsWith( name );

			case ConsumeItemRequest.EQUIP_HAT:
				return getEquipment( HAT ).toLowerCase().startsWith( name );

			case ConsumeItemRequest.EQUIP_SHIRT:
				return getEquipment( SHIRT ).toLowerCase().startsWith( name );

			case ConsumeItemRequest.EQUIP_PANTS:
				return getEquipment( PANTS ).toLowerCase().startsWith( name );

			case ConsumeItemRequest.EQUIP_ACCESSORY:
				return getEquipment( ACCESSORY1 ).toLowerCase().startsWith( name ) ||
					getEquipment( ACCESSORY2 ).toLowerCase().startsWith( name ) ||
					getEquipment( ACCESSORY3 ).toLowerCase().startsWith( name );
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

	// Familiars that modify earned XP:
	private static final int VOLLEYBALL = 12;
	private static final int CHESHIRE = 23;
	private static final int JILL = 24;
	private static final int SHAMAN = 39;
	private static final int MONKEY = 42;
	private static final int HARE = 50;
	private static final int HOBO = 52;

	// Items that modify Meat Drops
	public static final int RABBIT_FOOT = 1485;

	// Familiars that modify Meat Drops
	private static final int LEPRECHAUN = 2;
	private static final int TURKEY = 25;

	// Items that modify Item Drops
	private static final int JEKYLLIN = 1291;

	// KoLmafia does not support the "containers" slot.

	// Mr. Container (482): +3%
	// hemp backpack (218): +2%
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

	// Items and skills that make Mysticality the To-Hit stat
	private static final int SAUCE_GLOVE = 531;

	// Items that modify Mana cost of skills
	private static final int BACONSTONE_BRACELET = 717;
	private static final int STAINLESS_SOLITAIRE = 1226;
	private static final int PLEXIGLASS_POCKETWATCH = 1232;
	private static final int WIZARD_HAT = 1653;

	public static boolean recalculateAdjustments( boolean update )
	{
		int newMonsterLevelAdjustment = 0;
		int newFamiliarWeightAdjustment = 0;
		int newDodecapedeWeightAdjustment = 0;
		int newFamiliarItemWeightAdjustment = 0;
		int newManaCostModifier = 0;

		double newCombatPercentAdjustment = 0.0;
		double newInitiativeAdjustment = 0.0;
		double newFixedXPAdjustment = 0.0;
		double newMeatDropPercentAdjustment = 0.0;
		double newItemDropPercentAdjustment = 0.0;

		boolean rigatoniSkill = false;
		boolean hasStaff = false;
		boolean newRigatoniActive = false;

		int newDamageAbsorption = 0;
		int newDamageReduction = 0;
		double newColdResistance = 0;
		double newHotResistance = 0;
		double newSleazeResistance = 0;
		double newSpookyResistance = 0;
		double newStenchResistance = 0;

		int taoFactor = hasSkill( "Tao of the Terrapin" ) ? 2 : 1;

		int familiarID = currentFamiliar.getID();

		// Look at mind control level
		newMonsterLevelAdjustment += getMindControlLevel();

		// Look at items
		for ( int slot = HAT; slot <= FAMILIAR; ++slot )
		{
			AdventureResult item = getCurrentEquipment( slot );
			if ( item == null )
				continue;

			double [] modifiers = StatusEffectDatabase.getModifiers( item.getName() );

			newMonsterLevelAdjustment += modifiers[ StatusEffectDatabase.MONSTER_LEVEL_MODIFIER ];
			newFamiliarWeightAdjustment += modifiers[ StatusEffectDatabase.FAMILIAR_WEIGHT_MODIFIER ];
			newDodecapedeWeightAdjustment += modifiers[ StatusEffectDatabase.FAMILIAR_WEIGHT_MODIFIER ];
			newCombatPercentAdjustment += modifiers[ StatusEffectDatabase.COMBAT_RATE_MODIFIER ];
			newInitiativeAdjustment += modifiers[ StatusEffectDatabase.INITIATIVE_MODIFIER ];
			newFixedXPAdjustment += modifiers[ StatusEffectDatabase.EXPERIENCE_MODIFIER ];
			newMeatDropPercentAdjustment += modifiers[ StatusEffectDatabase.MEATDROP_MODIFIER ];
			newItemDropPercentAdjustment += modifiers[ StatusEffectDatabase.ITEMDROP_MODIFIER ];
			newDamageAbsorption += modifiers[ StatusEffectDatabase.DAMAGE_ABSORPTION_MODIFIER ];
			newDamageReduction += modifiers[ StatusEffectDatabase.DAMAGE_REDUCTION_MODIFIER ];
			newColdResistance += modifiers[ StatusEffectDatabase.COLD_RESISTANCE_MODIFIER ];
			newHotResistance += modifiers[ StatusEffectDatabase.HOT_RESISTANCE_MODIFIER ];
			newSleazeResistance += modifiers[ StatusEffectDatabase.SLEAZE_RESISTANCE_MODIFIER ];
			newSpookyResistance += modifiers[ StatusEffectDatabase.SPOOKY_RESISTANCE_MODIFIER ];
			newStenchResistance += modifiers[ StatusEffectDatabase.STENCH_RESISTANCE_MODIFIER ];

			switch ( slot )
			{
			case FAMILIAR:
				newFamiliarItemWeightAdjustment = FamiliarData.itemWeightModifier( item.getItemID() );
				break;

			case HAT:
			case PANTS:
				newDamageAbsorption += taoFactor * EquipmentDatabase.getPower( item.getItemID() );
				break;

			case SHIRT:
				newDamageAbsorption += EquipmentDatabase.getPower( item.getItemID() );
				break;
			}

			switch ( item.getItemID() )
			{
				case JEKYLLIN:
					newItemDropPercentAdjustment += 15 + MoonPhaseDatabase.getMoonlight() * 5;
					break;

				case SAUCE_GLOVE:
					if ( classtype.startsWith( "Sa" ) )
						rigatoniSkill = true;
					break;

				case 77:	// spooky stick
				case 103:	// spooky staff
				case 108:	// big stick
				case 110:	// basic meat staff
				case 114:	// dripping meat staff
				case 148:	// eXtreme meat staff
				case 228:	// Kentucky-fried meat staff
				case 379:	// linoleum staff
				case 382:	// asbestos staff
				case 385:	// chrome staff
				case 414:	// crowbarrr
				case 659:	// star staff
				case 943:	// bow staff
				case 1246:	// rib of the Bonerdagon
				case 1467:	// 25-meat staff
				case 1680:	// cardboard staff
				case 1682:	// bubblewrap staff
				case 1685:	// styrofoam staff
				case 1707:	// flypaper staff
				case 1713:	// squeaky staff
				case 1716:	// starchy staff
				case 1719:	// poutine pole
				case 1722:	// glistening staff
				case 1725:	// bigger stick
				case 1729:	// dense meat staff
				case 1732:	// smoldering staff
				case 1735:	// meatspout staff
				case 1738:	// hairy staff
				case 1741:	// giant cheesestick
				case 1744:	// savory staff
				case 1747:	// soylent staff
					hasStaff = true;
					break;

				case BACONSTONE_BRACELET:
				case WIZARD_HAT:
					newManaCostModifier -= 1;
					break;

				case STAINLESS_SOLITAIRE:
					newManaCostModifier -= 2;
					break;

				case PLEXIGLASS_POCKETWATCH:
					newManaCostModifier -= 3;
					break;
			}
		}

		// Certain outfits give benefits to the character
		if ( EquipmentDatabase.isWearingOutfit( 6 ) )
		{
			// Hot and Cold Running Ninja Suit
			newColdResistance += 20;
			newHotResistance += 20;
		}
		else if ( EquipmentDatabase.isWearingOutfit( 7 ) )
		{
			// eXtreme Cold-Weather Gear
			newColdResistance += 30;
		}
		else if ( EquipmentDatabase.isWearingOutfit( 25 ) )
		{
			// Arboreal Raiment
			newStenchResistance += 10;
		}

		// Because there are a limited number of passive skills,
		// it is much more efficient to execute one check for
		// each of the known skills.

		if ( hasSkill( "Amphibian Sympathy" ) )
		{
			newFamiliarWeightAdjustment += 5;
			newDodecapedeWeightAdjustment += 5;
		}

		if ( hasSkill( "Cold Blooded Fearlessness" ) )
			newSpookyResistance += 20;

		if ( hasSkill( "Diminished Gag Reflex" ) )
			newStenchResistance += 20;

		if ( hasSkill( "Expert Panhandling" ) )
			newMeatDropPercentAdjustment += 10;

		if ( hasSkill( "Gnefarious Pickpocketing" ) )
			newMeatDropPercentAdjustment += 10;

		if ( hasSkill( "Heart of Polyester" ) )
			newSleazeResistance += 20;

		if ( hasSkill( "Hide of the Otter" ) )
			newDamageAbsorption += 20;

		if ( hasSkill( "Hide of the Walrus" ) )
			newDamageAbsorption += 40;

		if ( hasSkill( "Mad Looting Skillz" ) )
			newItemDropPercentAdjustment += 20;

		if ( hasSkill( "Nimble Fingers" ) )
			newMeatDropPercentAdjustment += 20;

		if ( hasSkill( "Northern Exposure" ) )
			newColdResistance += 20;

		if ( hasSkill( "Overdeveloped Sense of Self Preservation" ) )
			newInitiativeAdjustment += 20;

		if ( hasSkill( "Powers of Observatiogn" ) )
			newItemDropPercentAdjustment += 10;

		if ( hasSkill( "Skin of the Leatherback" ) )
			newDamageReduction += 4;

		if ( hasSkill( "Spirit of Rigatoni" ) )
			rigatoniSkill = true;

		if ( hasSkill( "Tolerance of the Kitchen" ) )
			newHotResistance += 20;

		// For the sake of easier maintenance, execute a lot of extra
		// extra string comparisons when looking at status effects.

		AdventureResult [] effects = new AdventureResult[ KoLCharacter.getEffects().size() ];
		KoLCharacter.getEffects().toArray( effects );

		for ( int i = 0; i < effects.length; ++i )
		{
			double [] modifiers = StatusEffectDatabase.getModifiers( effects[i].getName() );

			newMonsterLevelAdjustment += modifiers[ StatusEffectDatabase.MONSTER_LEVEL_MODIFIER ];
			newFamiliarWeightAdjustment += modifiers[ StatusEffectDatabase.FAMILIAR_WEIGHT_MODIFIER ];
			newDodecapedeWeightAdjustment += modifiers[ StatusEffectDatabase.FAMILIAR_WEIGHT_MODIFIER ];
			newCombatPercentAdjustment += modifiers[ StatusEffectDatabase.COMBAT_RATE_MODIFIER ];
			newInitiativeAdjustment += modifiers[ StatusEffectDatabase.INITIATIVE_MODIFIER ];
			newFixedXPAdjustment += modifiers[ StatusEffectDatabase.EXPERIENCE_MODIFIER ];
			newMeatDropPercentAdjustment += modifiers[ StatusEffectDatabase.MEATDROP_MODIFIER ];
			newItemDropPercentAdjustment += modifiers[ StatusEffectDatabase.ITEMDROP_MODIFIER ];
			newDamageAbsorption += modifiers[ StatusEffectDatabase.DAMAGE_ABSORPTION_MODIFIER ];
			newDamageReduction += modifiers[ StatusEffectDatabase.DAMAGE_REDUCTION_MODIFIER ];
			newColdResistance += modifiers[ StatusEffectDatabase.COLD_RESISTANCE_MODIFIER ];
			newHotResistance += modifiers[ StatusEffectDatabase.HOT_RESISTANCE_MODIFIER ];
			newSleazeResistance += modifiers[ StatusEffectDatabase.SLEAZE_RESISTANCE_MODIFIER ];
			newSpookyResistance += modifiers[ StatusEffectDatabase.SPOOKY_RESISTANCE_MODIFIER ];
			newStenchResistance += modifiers[ StatusEffectDatabase.STENCH_RESISTANCE_MODIFIER ];
		}

		if ( ARIA.getCount( activeEffects ) > 0 )
			newMonsterLevelAdjustment += 2 * getLevel();

		// Now that we have calculated the familiar weight adjustment,
		// look at familiar.

		double modifier = (double)( currentFamiliar.getWeight() + newFamiliarWeightAdjustment + newFamiliarItemWeightAdjustment );
		switch ( familiarID )
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
				newItemDropPercentAdjustment += modifier * 2.5;
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

		// Determine if Mysticality is the current To-hit stat
		newRigatoniActive = rigatoniSkill && hasStaff;

		// Make sure the mana modifier is no more than
		// three, no matter what.

		newManaCostModifier = Math.max( newManaCostModifier, -3 );

		// Determine whether or not data has changed

		boolean changed = false;

		changed |= monsterLevelAdjustment != newMonsterLevelAdjustment;
		monsterLevelAdjustment = newMonsterLevelAdjustment;

		changed |= familiarWeightAdjustment != newFamiliarWeightAdjustment;
		familiarWeightAdjustment = newFamiliarWeightAdjustment;
		dodecapedeWeightAdjustment = newDodecapedeWeightAdjustment;

		changed |= familiarItemWeightAdjustment != newFamiliarItemWeightAdjustment;
		familiarItemWeightAdjustment = newFamiliarItemWeightAdjustment;

		changed |= manaCostModifier != newManaCostModifier;
		manaCostModifier = newManaCostModifier;

		changed |= combatPercentAdjustment != newCombatPercentAdjustment;
		combatPercentAdjustment = newCombatPercentAdjustment;

		changed |= initiativeAdjustment != newInitiativeAdjustment;
		initiativeAdjustment = newInitiativeAdjustment;

		changed |= fixedXPAdjustment != newFixedXPAdjustment;
		fixedXPAdjustment = newFixedXPAdjustment;

		changed |= meatDropPercentAdjustment != newMeatDropPercentAdjustment;
		meatDropPercentAdjustment = newMeatDropPercentAdjustment;

		changed |= itemDropPercentAdjustment != newItemDropPercentAdjustment;
		itemDropPercentAdjustment = newItemDropPercentAdjustment;

		changed |= rigatoniActive != newRigatoniActive;
		rigatoniActive = newRigatoniActive;

		changed |= newDamageAbsorption != damageAbsorption;
		damageAbsorption = newDamageAbsorption;

		changed |= newDamageReduction != damageReduction;
		damageReduction = newDamageReduction;

		changed |= newColdResistance != coldResistance;
		coldResistance = newColdResistance;

		changed |= newHotResistance != hotResistance;
		hotResistance = newHotResistance;

		changed |= newSleazeResistance != sleazeResistance;
		sleazeResistance = newSleazeResistance;

		changed |= newSpookyResistance != spookyResistance;
		spookyResistance = newSpookyResistance;

		changed |= newStenchResistance != stenchResistance;
		stenchResistance = newStenchResistance;

		// If the recalculation requires an update, and there was a
		// change detected, then update.

		if ( changed && update )
			updateStatus();

		return changed;
	}
}
