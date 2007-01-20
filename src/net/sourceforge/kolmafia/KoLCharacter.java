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

import java.util.ArrayList;
import java.util.List;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.java.dev.spellcast.utilities.LockableListModel;
import net.java.dev.spellcast.utilities.SortedListModel;

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
	private static final Pattern STILLS_PATTERN = Pattern.compile( "with (\\d+) bright" );

	public static final String SEAL_CLUBBER = "Seal Clubber";
	private static final List SEAL_CLUBBER_RANKS = new ArrayList();
	static
	{
		SEAL_CLUBBER_RANKS.add( "Lemming Trampler" );
		SEAL_CLUBBER_RANKS.add( "Tern Slapper" );
		SEAL_CLUBBER_RANKS.add( "Puffin Intimidator" );
		SEAL_CLUBBER_RANKS.add( "Ermine Thumper" );
		SEAL_CLUBBER_RANKS.add( "Penguin Frightener" );
		SEAL_CLUBBER_RANKS.add( "Malamute Basher" );
		SEAL_CLUBBER_RANKS.add( "Narwhal Pummeler" );
		SEAL_CLUBBER_RANKS.add( "Otter Crusher" );
		SEAL_CLUBBER_RANKS.add( "Caribou Smacker" );
		SEAL_CLUBBER_RANKS.add( "Moose Harasser" );
		SEAL_CLUBBER_RANKS.add( "Reindeer Threatener" );
		SEAL_CLUBBER_RANKS.add( "Ox Wrestler" );
		SEAL_CLUBBER_RANKS.add( "Walrus Bludgeoner" );
		SEAL_CLUBBER_RANKS.add( "Whale Boxer" );
		SEAL_CLUBBER_RANKS.add( "Seal Clubber" );
	}

	public static final String TURTLE_TAMER = "Turtle Tamer";
	private static final List TURTLE_TAMER_RANKS = new ArrayList();
	static
	{
		TURTLE_TAMER_RANKS.add( "Toad Coach" );
		TURTLE_TAMER_RANKS.add( "Skink Trainer" );
		TURTLE_TAMER_RANKS.add( "Frog Director" );
		TURTLE_TAMER_RANKS.add( "Gecko Supervisor" );
		TURTLE_TAMER_RANKS.add( "Newt Herder" );
		TURTLE_TAMER_RANKS.add( "Frog Boss" );
		TURTLE_TAMER_RANKS.add( "Iguana Driver" );
		TURTLE_TAMER_RANKS.add( "Salamander Subduer" );
		TURTLE_TAMER_RANKS.add( "Bullfrog Overseer" );
		TURTLE_TAMER_RANKS.add( "Rattlesnake Chief" );
		TURTLE_TAMER_RANKS.add( "Crocodile Lord" );
		TURTLE_TAMER_RANKS.add( "Cobra Commander" );
		TURTLE_TAMER_RANKS.add( "Alligator Subjugator" );
		TURTLE_TAMER_RANKS.add( "Asp Master" );
		TURTLE_TAMER_RANKS.add( "Turtle Tamer" );
	}

	public static final String PASTAMANCER = "Pastamancer";
	private static final List PASTAMANCER_RANKS = new ArrayList();
	static
	{
		PASTAMANCER_RANKS.add( "Dough Acolyte" );
		PASTAMANCER_RANKS.add( "Yeast Scholar" );
		PASTAMANCER_RANKS.add( "Noodle Neophyte" );
		PASTAMANCER_RANKS.add( "Starch Savant" );
		PASTAMANCER_RANKS.add( "Carbohydrate Cognoscenti" );
		PASTAMANCER_RANKS.add( "Spaghetti Sage" );
		PASTAMANCER_RANKS.add( "Macaroni Magician" );
		PASTAMANCER_RANKS.add( "Vermicelli Enchanter" );
		PASTAMANCER_RANKS.add( "Linguini Thaumaturge" );
		PASTAMANCER_RANKS.add( "Ravioli Sorcerer" );
		PASTAMANCER_RANKS.add( "Manicotti Magus" );
		PASTAMANCER_RANKS.add( "Spaghetti Spellbinder" );
		PASTAMANCER_RANKS.add( "Cannelloni Conjurer" );
		PASTAMANCER_RANKS.add( "Angel-Hair Archmage" );
		PASTAMANCER_RANKS.add( "Pastamancer" );
	}

	public static final String SAUCEROR = "Sauceror";
	private static final List SAUCEROR_RANKS = new ArrayList();
	static
	{
		SAUCEROR_RANKS.add( "Allspice Acolyte" );
		SAUCEROR_RANKS.add( "Cilantro Seer" );
		SAUCEROR_RANKS.add( "Parsley Enchanter" );
		SAUCEROR_RANKS.add( "Sage Sage" );
		SAUCEROR_RANKS.add( "Rosemary Diviner" );
		SAUCEROR_RANKS.add( "Thyme Wizard" );
		SAUCEROR_RANKS.add( "Tarragon Thaumaturge" );
		SAUCEROR_RANKS.add( "Oreganoccultist" );
		SAUCEROR_RANKS.add( "Basillusionist" );
		SAUCEROR_RANKS.add( "Coriander Conjurer" );
		SAUCEROR_RANKS.add( "Bay Leaf Brujo" );
		SAUCEROR_RANKS.add( "Sesame Soothsayer" );
		SAUCEROR_RANKS.add( "Marinara Mage" );
		SAUCEROR_RANKS.add( "Alfredo Archmage" );
		SAUCEROR_RANKS.add( "Sauceror" );
	}

	public static final String DISCO_BANDIT = "Disco Bandit";
	private static final List DISCO_BANDIT_RANKS = new ArrayList();
	static
	{
		DISCO_BANDIT_RANKS.add( "Funk Footpad" );
		DISCO_BANDIT_RANKS.add( "Rhythm Rogue" );
		DISCO_BANDIT_RANKS.add( "Chill Crook" );
		DISCO_BANDIT_RANKS.add( "Jiggy Grifter" );
		DISCO_BANDIT_RANKS.add( "Beat Snatcher" );
		DISCO_BANDIT_RANKS.add( "Sample Swindler" );
		DISCO_BANDIT_RANKS.add( "Move Buster" );
		DISCO_BANDIT_RANKS.add( "Jam Horker" );
		DISCO_BANDIT_RANKS.add( "Groove Filcher" );
		DISCO_BANDIT_RANKS.add( "Vibe Robber" );
		DISCO_BANDIT_RANKS.add( "Boogie Brigand" );
		DISCO_BANDIT_RANKS.add( "Flow Purloiner" );
		DISCO_BANDIT_RANKS.add( "Jive Pillager" );
		DISCO_BANDIT_RANKS.add( "Rhymer and Stealer" );
		DISCO_BANDIT_RANKS.add( "Disco Bandit" );
	}

	public static final String ACCORDION_THIEF = "Accordion Thief";
	private static final List ACCORDION_THIEF_RANKS = new ArrayList();
	static
	{
		ACCORDION_THIEF_RANKS.add( "Polka Criminal" );
		ACCORDION_THIEF_RANKS.add( "Mariachi Larcenist" );
		ACCORDION_THIEF_RANKS.add( "Zydeco Rogue" );
		ACCORDION_THIEF_RANKS.add( "Chord Horker" );
		ACCORDION_THIEF_RANKS.add( "Chromatic Crook" );
		ACCORDION_THIEF_RANKS.add( "Squeezebox Scoundrel" );
		ACCORDION_THIEF_RANKS.add( "Concertina Con Artist" );
		ACCORDION_THIEF_RANKS.add( "Button Box Burglar" );
		ACCORDION_THIEF_RANKS.add( "Hurdy-Gurdy Hooligan" );
		ACCORDION_THIEF_RANKS.add( "Sub-Sub-Apprentice Accordion Thief" );
		ACCORDION_THIEF_RANKS.add( "Sub-Apprentice Accordion Thief" );
		ACCORDION_THIEF_RANKS.add( "Pseudo-Apprentice Accordion Thief" );
		ACCORDION_THIEF_RANKS.add( "Hemi-Apprentice Accordion Thief" );
		ACCORDION_THIEF_RANKS.add( "Apprentice Accordion Thief" );
		ACCORDION_THIEF_RANKS.add( "Accordion Thief" );
	}

	private static final AdventureResult JOYBUZZER = new AdventureResult( 1525, 1 );

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
	private static int userId = 0;
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

	public static SortedListModel battleSkillNames = new SortedListModel();
	private static SortedListModel [] equipmentLists = new SortedListModel[9];

	static
	{
		for ( int i = 0; i < 9; ++i )
			equipmentLists[i] = new SortedListModel();
	}

	// Status pane data which is rendered whenever
	// the user issues a "status" type command.

	private static int pvpRank = 0;
	private static int attacksLeft = 0;
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
	private static float combatPercentAdjustment = 0.0f;
	private static float initiativeAdjustment = 0.0f;
	private static float fixedXPAdjustment = 0.0f;
	private static float meatDropPercentAdjustment = 0.0f;
	private static float itemDropPercentAdjustment = 0.0f;
	private static boolean rigatoniActive = false;
	private static int damageAbsorption = 0;
	private static int damageReduction = 0;
	private static float coldResistance = 0;
	private static float hotResistance = 0;
	private static float sleazeResistance = 0;
	private static float spookyResistance = 0;
	private static float stenchResistance = 0;

	// Travel information

	private static boolean hasStore = true;
	private static boolean hasDisplayCase = true;
	private static boolean hasClan = true;

	// Campground information

	private static boolean hasToaster = false;
	private static boolean hasArches = false;
	private static boolean hasChef = false;
	private static boolean hasBartender = false;

	// Familiar data for reference

	private static SortedListModel familiars = new SortedListModel();
	private static boolean isUsingStabBat = false;
	private static FamiliarData currentFamiliar = FamiliarData.NO_FAMILIAR;

	private static int arenaWins = 0;
	private static int stillsAvailable = 0;

	// Listener-driven container items

	private static List listenerList = new ArrayList();
	private static boolean beanstalkArmed = false;

	// Ascension-related variables

	private static boolean isHardcore = false;
	private static boolean canInteract = true;

	private static int ascensions = 0;
	private static String ascensionSign = "None";
	private static int ascensionSignType = NONE;
	private static int consumptionRestriction = AscensionSnapshotTable.NOPATH;
	private static int mindControlLevel = 0;

	private static String autosellMode = "";

	public static void resetInventory()
	{
		inventory.clear();
		ConcoctionsDatabase.refreshConcoctions();

		// Initialize the equipment lists inside
		// of the character data

		for ( int i = 0; i < 9; ++i )
			equipmentLists[i].clear();

		GearChangeFrame.clearWeaponLists();
	}

	/**
	 * Constructs a new <code>KoLCharacter</code> with the given name.
	 * All fields are initialized to their default values (nothing),
	 * and it is the responsibility of other methods to initialize
	 * the fields with their real values.
	 *
	 * @param	newUsername	The name of the character this <code>KoLCharacter</code> represents
	 */

	public static final void reset( String newUsername )
	{
		username = newUsername;
		KoLCharacter.reset();
	}

	public static void reset()
	{
		classname = "";
		classtype = "";

		pvpRank = 0;
		attacksLeft = 0;
		adjustedStats = new int[3];
		totalSubpoints = new int[3];

		monsterLevelAdjustment = 0;
		familiarWeightAdjustment = 0;
		dodecapedeWeightAdjustment = 0;
		familiarItemWeightAdjustment = 0;
		manaCostModifier = 0;
		combatPercentAdjustment = 0.0f;
		initiativeAdjustment = 0.0f;
		fixedXPAdjustment = 0.0f;
		meatDropPercentAdjustment = 0.0f;
		itemDropPercentAdjustment = 0.0f;
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

		closet.clear();
		storage.clear();
		collection.clear();

		usableSkills.clear();
		availableSkills.clear();
		battleSkillNames.clear();

		// All characters get the option to
		// attack something.

		battleSkillNames.add( "attack with weapon" );
		battleSkillNames.add( "custom combat script" );
		battleSkillNames.add( "delevel and plink" );

		battleSkillNames.add( "item dictionary" );
		battleSkillNames.add( "item toy mercenary" );

		battleSkillNames.add( "item seal tooth" );
		battleSkillNames.add( "item scroll of turtle summoning" );
		battleSkillNames.add( "item spices" );

		battleSkillNames.add( "try to run away" );

		getClient().resetBreakfastSummonings();

		isHardcore = false;
		canInteract = false;
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
		isUsingStabBat = false;

		stillsAvailable = -1;
		beanstalkArmed = false;

		ascensions = 0;
		ascensionSign = "None";
		ascensionSignType = NONE;

		mindControlLevel = 0;
		autosellMode = "";

		// Clear some of the standard lists so they don't
		// carry over from player to player.

		conditions.clear();
		eventHistory.clear();
		recentEffects.clear();
		activeEffects.clear();

		resetInventory();

		battleSkillNames.setSelectedItem( StaticEntity.getProperty( "battleAction" ) );
	}

	public static boolean isFallingDown()
	{	return getInebriety() >= getInebrietyLimit();
	}

	public static int getInebrietyLimit()
	{	return (hasSkill( "Liver of Steel" ) ? 20 : 15);
	}

	/**
	 * Accessor method to retrieve the name of this character.
	 * @return	The name of this character
	 */

	public static String getUserName()
	{	return username;
	}

	public static String baseUserName()
	{	return baseUserName( username );
	}

	public static String baseUserName( String name )
	{
		return name == null || name.equals( "" ) ? "GLOBAL" :
			globalStringReplace( globalStringDelete( name, "/q" ), " ", "_" ).trim().toLowerCase();
	}

	/**
	 * Accessor method to set the user Id associated with this character.
	 * @param	userId	The user Id associated with this character
	 */

	public static void setUserId( int userId )
	{	KoLCharacter.userId = userId;
	}

	/**
	 * Accessor method to retrieve the user Id associated with this character.
	 * @return	The user Id associated with this character
	 */

	public static int getUserId()
	{	return userId;
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

	public static int getAttacksLeft()
	{	return attacksLeft;
	}

	public static void setAttacksLeft( int attacksLeft )
	{	KoLCharacter.attacksLeft = attacksLeft;
	}

	/**
	 * Accessor method to set the character's class.
	 * @param	classname	The name of the character's class
	 */

	public static void setClassName( String classname )
	{
		KoLCharacter.classname = classname;
		KoLCharacter.classtype = null;
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
		if ( classtype == null )
		{
			classtype = SEAL_CLUBBER_RANKS.contains( classname ) ? SEAL_CLUBBER : TURTLE_TAMER_RANKS.contains( classname ) ? TURTLE_TAMER :
				PASTAMANCER_RANKS.contains( classname ) ? PASTAMANCER : SAUCEROR_RANKS.contains( classname ) ? SAUCEROR :
				DISCO_BANDIT_RANKS.contains( classname ) ? DISCO_BANDIT : ACCORDION_THIEF_RANKS.contains( classname ) ? ACCORDION_THIEF : SAUCEROR;
		}

		return classtype;
	}

	/**
	 * Accessor method to retrieve the type of the character's class.
	 * @return	The type of the character's class
	 */

	public static String getClassType( String classname )
	{
		return SEAL_CLUBBER_RANKS.contains( classname ) ? SEAL_CLUBBER : TURTLE_TAMER_RANKS.contains( classname ) ? TURTLE_TAMER :
			PASTAMANCER_RANKS.contains( classname ) ? PASTAMANCER : SAUCEROR_RANKS.contains( classname ) ? SAUCEROR :
			DISCO_BANDIT_RANKS.contains( classname ) ? DISCO_BANDIT : ACCORDION_THIEF_RANKS.contains( classname ) ? ACCORDION_THIEF : SAUCEROR;
	}

	public static boolean isMuscleClass()
	{	return classtype.equals( SEAL_CLUBBER ) || classtype.equals( TURTLE_TAMER );
	}

	public static boolean isMysticalityClass()
	{	return classtype.equals( PASTAMANCER ) || classtype.equals( SAUCEROR );
	}

	public static boolean isMoxieClass()
	{	return classtype.equals( DISCO_BANDIT ) || classtype.equals( ACCORDION_THIEF );
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
	{
		KoLCharacter.availableMeat = availableMeat;
		ConcoctionsDatabase.refreshConcoctions();
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
	{	KoLCharacter.adventuresLeft = adventuresLeft;
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

	public static float getCombatPercentAdjustment()
	{	return combatPercentAdjustment;
	}

	/**
	 * Accessor method to retrieve the total current initiative
	 * adjustment
	 *
	 * @return	Total Current Initiative Adjustment
	 */

	public static float getInitiativeAdjustment()
	{	return initiativeAdjustment;
	}

	/**
	 * Accessor method to retrieve the total current fixed XP
	 * adjustment
	 *
	 * @return	Total Current Fixed XP Adjustment
	 */

	public static float getFixedXPAdjustment()
	{	return fixedXPAdjustment;
	}

	/**
	 * Accessor method to retrieve the total current meat drop percent
	 * adjustment
	 *
	 * @return	Total Current Meat Drop Percent Adjustment
	 */

	public static float getMeatDropPercentAdjustment()
	{	return meatDropPercentAdjustment;
	}

	/**
	 * Accessor method to retrieve the total current item drop percent
	 * adjustment
	 *
	 * @return	Total Current Item Drop Percent Adjustment
	 */

	public static float getItemDropPercentAdjustment()
	{	return itemDropPercentAdjustment;
	}

	public static void setEquipment( int slot, AdventureResult item )
	{
		equipment.set( slot, item );
		equipmentLists[ slot ].setSelectedItem( item );

		if ( slot == WEAPON || slot == OFFHAND )
			GearChangeFrame.updateWeapons();

		if ( slot == FAMILIAR )
			currentFamiliar.setItem( item );
	}

	/**
	 * Accessor method to set the equipment the character is currently using.
	 * This does not take into account the power of the item or anything of
	 * that nature; only the item's name is stored.  Note that if no item is
	 * equipped, the value should be <code>none</code>, not <code>null</code>
	 * or the empty string.
	 *
	 * @param	equipment	All of the available equipment, stored in an array index by the constants
	 */

	public static void setEquipment( AdventureResult [] equipment )
	{
		for ( int i = 0; i < KoLCharacter.equipment.size(); ++i )
		{
			if ( equipment[i] == null || equipment[i].equals( EquipmentRequest.UNEQUIP ) )
			{
				KoLCharacter.equipment.set( i, EquipmentRequest.UNEQUIP );
				equipmentLists[i].setSelectedItem( EquipmentRequest.UNEQUIP );
			}
			else
			{
				if ( !equipmentLists[i].contains( equipment[i] ) )
					equipmentLists[i].add( equipment[i] );

				KoLCharacter.equipment.set( i, equipment[i] );
				equipmentLists[i].setSelectedItem( equipment[i] );
			}
		}

		GearChangeFrame.updateWeapons();
		if ( equipment.length > FAMILIAR && currentFamiliar != FamiliarData.NO_FAMILIAR )
		{
			equipmentLists[FAMILIAR].setSelectedItem( equipment[FAMILIAR] );
			currentFamiliar.setItem( equipment[FAMILIAR] );
		}
	}

	public static void setOutfits( List newOutfits )
	{
		// Rebuild outfits if given a new list
		if ( newOutfits != null )
		{
			customOutfits.clear();
			customOutfits.addAll( newOutfits );
		}

		EquipmentDatabase.updateOutfits();
	}

	/**
	 * Accessor method to retrieve the name of the item equipped on the character's familiar.
	 * @return	The name of the item equipped on the character's familiar, <code>none</code> if no such item exists
	 */

	public static AdventureResult getFamiliarItem()
	{	return currentFamiliar == null ? EquipmentRequest.UNEQUIP : currentFamiliar.getItem();
	}

	/**
	 * Accessor method to retrieve the name of a piece of equipment
	 * @param	type	the type of equipment
	 * @return	The name of the equipment, <code>none</code> if no such item exists
	 */

	public static AdventureResult getEquipment( int type )
	{
		if ( type >= HAT && type < FAMILIAR && type < equipment.size() )
			return (AdventureResult) equipment.get( type );

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
	 * Accessor method to retrieve # of hands character's weapon uses
	 * @return	int	number of hands needed
	 */

	public static int weaponHandedness()
	{	return EquipmentDatabase.getHands( getEquipment( WEAPON ).getName() );
	}

	/**
	 * Accessor method to determine if character's weapon is ranged
	 * @return	boolean	true if weapon is ranged
	 */

	public static boolean rangedWeapon()
	{	return EquipmentDatabase.isRanged( getEquipment( WEAPON ).getName() );
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
	 * Accessor method to retrieve the current elemental resistance
	 *
	 * @return	Total Current  Resistance to specified element
	 */

	public static float getElementalResistance( int element )
	{
		switch ( element )
		{
		case MonsterDatabase.COLD:
			return coldResistance;
		case MonsterDatabase.HEAT:
			return hotResistance;
		case MonsterDatabase.SLEAZE:
			return sleazeResistance;
		case MonsterDatabase.SPOOKY:
			return spookyResistance;
		case MonsterDatabase.STENCH:
			return stenchResistance;
		}

		return 0.0f;
	}

	/**
	 * Accessor method to retrieve the total current cold resistance
	 *
	 * @return	Total Current Cold Resistance
	 */

	public static float getColdResistance()
	{	return coldResistance;
	}

	/**
	 * Accessor method to retrieve the total current hot resistance
	 *
	 * @return	Total Current Hot Resistance
	 */

	public static float getHotResistance()
	{	return hotResistance;
	}

	/**
	 * Accessor method to retrieve the total current sleazw resistance
	 *
	 * @return	Total Current Sleaze Resistance
	 */

	public static float getSleazeResistance()
	{	return sleazeResistance;
	}

	/**
	 * Accessor method to retrieve the total current spooky resistance
	 *
	 * @return	Total Current Spooky Resistance
	 */

	public static float getSpookyResistance()
	{	return spookyResistance;
	}

	/**
	 * Accessor method to retrieve the total current stench resistance
	 *
	 * @return	Total Current Stench Resistance
	 */

	public static float getStenchResistance()
	{	return stenchResistance;
	}

	/**
	 * Accessor method to determine if character is currently dual-wielding
	 * @return	boolean	true if character has two weapons equipped
	 */

	public static boolean dualWielding()
	{	return EquipmentDatabase.getHands( getEquipment( OFFHAND ).getName() ) == 1;
	}

	/**
	 * Accessor method to retrieve a list of all available items which can be equipped
	 * by familiars.  Note this lists items which the current familiar cannot equip.
	 */

	public static LockableListModel [] getEquipmentLists()
	{	return equipmentLists;
	}

	public static int consumeFilterToEquipmentType( int consumeFilter )
	{
		switch ( consumeFilter )
		{
		case ConsumeItemRequest.EQUIP_HAT:
			return HAT;
		case ConsumeItemRequest.EQUIP_WEAPON:
			return WEAPON;
		case ConsumeItemRequest.EQUIP_OFFHAND:
			return OFFHAND;
		case ConsumeItemRequest.EQUIP_SHIRT:
			return SHIRT;
		case ConsumeItemRequest.EQUIP_PANTS:
			return PANTS;
		case ConsumeItemRequest.EQUIP_ACCESSORY:
			return ACCESSORY1;
		case ConsumeItemRequest.EQUIP_FAMILIAR:
			return FAMILIAR;
		default:
			return -1;
		}
	}

	public static int equipmentTypeToConsumeFilter( int equipmentType )
	{
		switch ( equipmentType )
		{
		case HAT:
			return ConsumeItemRequest.EQUIP_HAT;
		case WEAPON:
			return ConsumeItemRequest.EQUIP_WEAPON;
		case OFFHAND:
			return ConsumeItemRequest.EQUIP_OFFHAND;
		case SHIRT:
			return ConsumeItemRequest.EQUIP_SHIRT;
		case PANTS:
			return ConsumeItemRequest.EQUIP_PANTS;
		case ACCESSORY1:
		case ACCESSORY2:
		case ACCESSORY3:
			return ConsumeItemRequest.EQUIP_ACCESSORY;
		case FAMILIAR:
			return ConsumeItemRequest.EQUIP_FAMILIAR;
		default:
			return -1;
		}
	}

	public static void updateEquipmentLists()
	{
		EquipmentDatabase.updateOutfits();
		for ( int i = 0; i <= FAMILIAR; ++i )
			updateEquipmentList( i );
	}

	private static void updateEquipmentList( int listIndex )
	{
		int consumeFilter = equipmentTypeToConsumeFilter( listIndex );
		if ( consumeFilter == -1 )
			return;

		AdventureResult equippedItem = getEquipment( listIndex );

		switch ( listIndex )
		{
		case ACCESSORY1:
		case ACCESSORY2:
		case ACCESSORY3:

			updateEquipmentList( consumeFilter, equipmentLists[ ACCESSORY1 ] );
			updateEquipmentList( consumeFilter, equipmentLists[ ACCESSORY2 ] );
			updateEquipmentList( consumeFilter, equipmentLists[ ACCESSORY3 ] );

			if ( !equipmentLists[ ACCESSORY1 ].contains( equippedItem ) )
				equipmentLists[ ACCESSORY1 ].add( equippedItem );
			if ( !equipmentLists[ ACCESSORY2 ].contains( equippedItem ) )
				equipmentLists[ ACCESSORY2 ].add( equippedItem );
			if ( !equipmentLists[ ACCESSORY3 ].contains( equippedItem ) )
				equipmentLists[ ACCESSORY3 ].add( equippedItem );

		default:

			updateEquipmentList( consumeFilter, equipmentLists[ listIndex ] );
			if ( !equipmentLists[ listIndex ].contains( equippedItem ) )
				equipmentLists[ listIndex ].add( equippedItem );
		}


		equipmentLists[ listIndex ].setSelectedItem( equippedItem );
	}

	private static void updateEquipmentList( int filterId, List currentList )
	{
		ArrayList temporary = new ArrayList();
		temporary.add( EquipmentRequest.UNEQUIP );

		// If the character is currently equipped with a one-handed
		// weapon and the character has the ability to dual-wield
		// weapons, then also allow one-handed weapons in the off-hand.

		boolean dual = ( weaponHandedness() == 1 && hasSkill( "Double-Fisted Skull Smashing" ) );
		boolean ranged = rangedWeapon();

		for ( int i = 0; i < inventory.size(); ++i )
		{
			AdventureResult currentItem = (AdventureResult) inventory.get(i);
			String currentItemName = currentItem.getName();

			int type = TradeableItemDatabase.getConsumptionType( currentItem.getItemId() );

			// If we are equipping familiar items, make sure
			// current familiar can use this one

			if ( filterId == ConsumeItemRequest.EQUIP_FAMILIAR && type == ConsumeItemRequest.EQUIP_FAMILIAR )
			{
				temporary.add( currentItem );
				continue;
			}

			// If we want off-hand items and we can dual wield,
			// allow one-handed weapons of same type

			if ( filterId == ConsumeItemRequest.EQUIP_OFFHAND && type == ConsumeItemRequest.EQUIP_WEAPON && dual )
			{
				if ( EquipmentDatabase.getHands( currentItemName ) != 1 || EquipmentDatabase.isRanged( currentItemName ) != ranged )
					continue;
			}

			// Otherwise, slot and item type must match

			else if ( filterId != type )
				continue;

			// If we are currently dual-wielding, only melee
			// weapons are allowed in the main weapon slot
			// Two-handed ranged weapons are also allowed since
			// they will remove both weapons when equipped

			else if ( filterId == ConsumeItemRequest.EQUIP_WEAPON && dual )
			{
				if ( EquipmentDatabase.getHands( currentItemName ) == 1 && EquipmentDatabase.isRanged( currentItemName ) != ranged )
					continue;
			}

			temporary.add( currentItem );
		}

		// If we are looking at familiar items, include those which can
		// be universally equipped, but are currently on another
		// familiar.

		if ( filterId == ConsumeItemRequest.EQUIP_FAMILIAR )
		{
			FamiliarData [] familiarList = new FamiliarData[ familiars.size() ];
			familiars.toArray( familiarList );

			for ( int i = 0; i < familiarList.length; ++i )
				AdventureResult.addResultToList( temporary, familiarList[i].getItem() );
		}

		currentList.retainAll( temporary );
		temporary.removeAll( currentList );
		currentList.addAll( temporary );
	}

	private static int getCount( AdventureResult accessory )
	{
		int available = accessory.getCount( inventory );
		if ( KoLCharacter.getEquipment( ACCESSORY1 ).equals( accessory ) )
			++available;
		if ( KoLCharacter.getEquipment( ACCESSORY2 ).equals( accessory ) )
			++available;
		if ( KoLCharacter.getEquipment( ACCESSORY3 ).equals( accessory ) )
			++available;

		return available;
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
		ConcoctionsDatabase.refreshConcoctions();
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
		ConcoctionsDatabase.refreshConcoctions();
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
		recalculateAdjustments();
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
	 * Accessor method to set the list of available skills.
	 * @param	newSkillSet	The list of the names of available skills
	 */

	public static void setAvailableSkills( List newSkillSet )
	{
		if ( KoLCharacter.isMoxieClass() )
			addAvailableSkill( UseSkillRequest.getInstance( "Moxious Maneuver" ) );

		// Check all available skills to see if they
		// qualify to be added as combat or usables.

		for ( int i = 0; i < newSkillSet.size(); ++i )
			addAvailableSkill( (UseSkillRequest) newSkillSet.get(i) );

		// Superhuman Cocktailcrafting affects # of summons for
		// Advanced Cocktailcrafting
		if ( hasSkill( "Superhuman Cocktailcrafting" ) )
			getClient().setBreakfastSummonings( KoLmafia.COCKTAILCRAFTING, 5 );

		// Transcendental Noodlecraft affects # of summons for
		// Pastamastery
		if ( hasSkill( "Transcendental Noodlecraft" ) )
			getClient().setBreakfastSummonings( KoLmafia.PASTAMASTERY, 5 );

		// The Way of Sauce affects # of summons for
		// Advanced Saucecrafting
		if ( hasSkill( "The Way of Sauce" ) )
			getClient().setBreakfastSummonings( KoLmafia.SAUCECRAFTING, 5 );

		// Add derived skills based on base skills
		addDerivedSkills();
		battleSkillNames.setSelectedItem( StaticEntity.getProperty( "battleAction" ) );
	}

	/**
	 * Adds a single skill to the list of known skills
	 * possessed by this character.
	 */

	public static void addAvailableSkill( UseSkillRequest skill )
	{
		if ( availableSkills.contains( skill ) )
			return;

		availableSkills.add( skill );

		switch ( ClassSkillsDatabase.getSkillType( skill.getSkillId() ) )
		{
		case ClassSkillsDatabase.PASSIVE:

			// Flavour of Magic gives you access to five other
			// castable skills

			if ( skill.getSkillName().equals( "Flavour of Magic" ) )
			{
				usableSkills.add( UseSkillRequest.getInstance( "Spirit of Cayenne" ) );
				usableSkills.add( UseSkillRequest.getInstance( "Spirit of Peppermint" ) );
				usableSkills.add( UseSkillRequest.getInstance( "Spirit of Garlic" ) );
				usableSkills.add( UseSkillRequest.getInstance( "Spirit of Wormwood" ) );
				usableSkills.add( UseSkillRequest.getInstance( "Spirit of Bacon Grease" ) );
			}

			break;

		case ClassSkillsDatabase.SKILL:
		case ClassSkillsDatabase.BUFF:

			usableSkills.add( skill );
			break;

		case ClassSkillsDatabase.COMBAT:

			addCombatSkill( skill.getSkillName() );
			break;
		}
	}

	/**
	 * Adds derived skills to appropriate lists
	 */

	public static void addDerivedSkills()
	{
		if ( classtype.startsWith( "Tu" ) )
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

		UseSkillRequest handshake = UseSkillRequest.getInstance( "Shake Hands" );
		if ( hasItem( JOYBUZZER ) )
			addAvailableSkill( handshake );
	}

	private static void addCombatSkill( String name )
	{
		String skillname = "skill " + name.toLowerCase();
		if ( !battleSkillNames.contains( skillname ) )
			battleSkillNames.add( skillname );
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

	public static boolean hasSkill( int skillId )
	{	return hasSkill( ClassSkillsDatabase.getSkillName( skillId ) );
	}

	public static boolean hasSkill( String skillName )
 	{	return hasSkill( skillName, availableSkills ) || hasSkill( skillName, usableSkills );
	}

	public static boolean hasSkill( String skillName, LockableListModel list )
	{
		for ( int i = 0; i < list.size(); ++i )
			if ( ((UseSkillRequest)list.get(i)).getSkillName().equalsIgnoreCase( skillName ) )
				return true;
		return false;
	}

	/**
	 * Accessor method to get the current familiar.
	 * @return	familiar	The current familiar
	 */

	public static FamiliarData getFamiliar()
	{	return currentFamiliar == null ? FamiliarData.NO_FAMILIAR : currentFamiliar;
	}

	public static boolean isUsingStabBat()
	{	return isUsingStabBat;
	}

	/**
	 * Accessor method to get arena wins
	 * @return	The number of arena wins
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
		if ( !hasSkill( "Superhuman Cocktailcrafting" ) || !isMoxieClass() )
			return 0;

		if ( stillsAvailable == -1 )
		{
			KoLRequest request = new KoLRequest( "guild.php?place=still" );
			RequestThread.postRequest( request );

			Matcher stillMatcher = STILLS_PATTERN.matcher( request.responseText );
			if ( stillMatcher.find() )
				stillsAvailable = parseInt( stillMatcher.group(1) );
			else
				stillsAvailable = 0;
		}

		return stillsAvailable;
	}

	public static void decrementStillsAvailable( int decrementAmount )
	{
		stillsAvailable -= decrementAmount;
		ConcoctionsDatabase.refreshConcoctions();
	}

	public static boolean canUseWok()
	{	return hasSkill( "Transcendental Noodlecraft" ) && isMysticalityClass();
	}

	public static boolean canUseMalus()
	{	return hasSkill( "Pulverize" ) && isMuscleClass();
	}

	/**
	 * Accessor method to set arena wins
	 * @param	wins	The number of arena wins
	 */

	public static void setArenaWins( int wins )
	{	arenaWins = wins;
	}

	/**
	 * Accessor method to find the specified familiar.
	 * @param	race	The race of the familiar to find
	 * @return	familiar	The first familiar matching this race
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
	 * @param	familiar	The new current familiar
	 */

	public static void setFamiliar( FamiliarData familiar )
	{
		currentFamiliar = addFamiliar( familiar );

		familiars.setSelectedItem( currentFamiliar );

		if ( !equipmentLists[FAMILIAR].contains( currentFamiliar.getItem() ) )
			equipmentLists[FAMILIAR].add( currentFamiliar.getItem() );

		equipmentLists[FAMILIAR].setSelectedItem( currentFamiliar.getItem() );
		isUsingStabBat = currentFamiliar.equals( "Stab Bat" ) || currentFamiliar.equals( "Scary Death Orb" );

		recalculateAdjustments();
		updateStatus();
	}

	/**
	 * Accessor method to increment the weight of the current familiar
	 * by one.
	 */

	public static void incrementFamilarWeight()
	{
		if ( currentFamiliar != null )
			currentFamiliar.setWeight( currentFamiliar.getWeight() + 1 );
	}

	/**
	 * Adds the given familiar to the list of available familiars.
	 * @param	familiar	The Id of the familiar to be added
	 */

	public static FamiliarData addFamiliar( FamiliarData familiar )
	{
		if ( familiar != null )
		{
			int index = familiars.indexOf( familiar );
			if ( index >= 0)
				familiar = (FamiliarData) familiars.get( index );
			else
				familiars.add( familiar );
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

	public static void processResult( AdventureResult result )
	{	processResult( result, true );
	}

	/**
	 * Processes a result received through adventuring.
	 * This places items inside of inventories and lots
	 * of other good stuff.
	 */

	public static void processResult( AdventureResult result, boolean updateCalculatedLists )
	{
		// Treat the result as normal from this point forward.
		// Figure out which list the skill should be added to
		// and add it to that list.

		String resultName = result.getName();
		if ( resultName == null )
			return;

		if ( result.isItem() )
		{
			AdventureResult.addResultToList( inventory, result );

			if ( updateCalculatedLists )
			{
				if ( TradeableItemDatabase.getConsumptionType( result.getItemId() ) == ConsumeItemRequest.EQUIP_ACCESSORY )
				{
					AdventureResult.addResultToList( equipmentLists[ ACCESSORY1 ], result );
					AdventureResult.addResultToList( equipmentLists[ ACCESSORY2 ], result );
					AdventureResult.addResultToList( equipmentLists[ ACCESSORY3 ], result );
				}
				else
				{
					int equipmentType = consumeFilterToEquipmentType( TradeableItemDatabase.getConsumptionType( result.getItemId() ) );
					if ( equipmentType != -1 )
						AdventureResult.addResultToList( equipmentLists[ equipmentType ], result );
				}

				if ( EquipmentDatabase.getOutfitWithItem( result.getItemId() ) != -1 )
					EquipmentDatabase.updateOutfits();

				if ( !ConcoctionsDatabase.getKnownUses( result ).isEmpty() )
					ConcoctionsDatabase.refreshConcoctions();
			}
		}
		else if ( resultName.equals( AdventureResult.HP ) )
		{
			setHP( getCurrentHP() + result.getCount(), getMaximumHP(), getBaseMaxHP() );
		}
		else if ( resultName.equals( AdventureResult.MP ) )
		{
			setMP( getCurrentMP() + result.getCount(), getMaximumMP(), getBaseMaxMP() );
		}
		else if ( resultName.equals( AdventureResult.MEAT ) )
		{
			setAvailableMeat( getAvailableMeat() + result.getCount() );
		}
		else if ( resultName.equals( AdventureResult.ADV ) )
		{
			setAdventuresLeft( getAdventuresLeft() + result.getCount() );
			if ( result.getCount() < 0 )
			{
				AdventureResult [] effectsArray = new AdventureResult[ activeEffects.size() ];
				activeEffects.toArray( effectsArray );

				for ( int i = effectsArray.length - 1; i >= 0; --i )
				{
					AdventureResult effect = effectsArray[i];
					if ( effect.getCount() <= 0 - result.getCount() )
						activeEffects.remove( i );
					else
						activeEffects.set( i, effect.getInstance( effect.getCount() + result.getCount() ) );
				}

				setTotalTurnsUsed( getTotalTurnsUsed() - result.getCount() );
				if ( getTotalTurnsUsed() >= 600 && !isHardcore() )
					setInteraction( true );
			}
		}
		else if ( resultName.equals( AdventureResult.DRUNK ) )
		{
			setInebriety( getInebriety() + result.getCount() );
		}

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
			if ( TradeableItemDatabase.getConsumptionType( items[i].getItemId() ) == ConsumeItemRequest.CONSUME_ZAP )
				return items[i];

		// No wand
		return null;
	}

	public static boolean hasItem( AdventureResult item )
	{	return hasItem( item, false );
	}

	public static boolean hasItem( AdventureResult item, boolean shouldCreate )
	{
		if ( item == null )
			return false;

		int count = item.getCount( inventory ) + item.getCount( closet );
		switch ( TradeableItemDatabase.getConsumptionType( item.getItemId() ) )
		{
		case ConsumeItemRequest.EQUIP_HAT:
		case ConsumeItemRequest.EQUIP_PANTS:
		case ConsumeItemRequest.EQUIP_FAMILIAR:
		case ConsumeItemRequest.EQUIP_OFFHAND:
			if ( hasEquipped( item ) )  ++count;
			break;

		case ConsumeItemRequest.EQUIP_WEAPON:
			if ( hasEquipped( item.getName(), WEAPON ) )  ++count;
			if ( hasEquipped( item.getName(), OFFHAND ) )  ++count;
			break;

		case ConsumeItemRequest.EQUIP_ACCESSORY:
			if ( hasEquipped( item.getName(), ACCESSORY1 ) )  ++count;
			if ( hasEquipped( item.getName(), ACCESSORY2 ) )  ++count;
			if ( hasEquipped( item.getName(), ACCESSORY3 ) )  ++count;
			break;
		}

		if ( shouldCreate )
		{
			ItemCreationRequest creation = ItemCreationRequest.getInstance( item.getItemId() );
			if ( creation != null )
				count += creation.getQuantityPossible();
		}

		return count > 0 && count >= item.getCount();
	}

	public static boolean hasEquipped( String itemName, int equipmentSlot )
	{
		AdventureResult itemInSlot = getEquipment( equipmentSlot );
		return KoLDatabase.getCanonicalName( itemInSlot.getName() ).equals( KoLDatabase.getCanonicalName( itemName ) );
	}

	public static boolean hasEquipped( AdventureResult item )
	{
		String name = item.getName();
		switch ( TradeableItemDatabase.getConsumptionType( item.getItemId() ) )
		{
		case ConsumeItemRequest.EQUIP_WEAPON:
			return hasEquipped( name, WEAPON ) || hasEquipped( name, OFFHAND );

		case ConsumeItemRequest.EQUIP_OFFHAND:
			return hasEquipped( name, OFFHAND );

		case ConsumeItemRequest.EQUIP_HAT:
			return hasEquipped( name, HAT );

		case ConsumeItemRequest.EQUIP_SHIRT:
			return hasEquipped( name, SHIRT );

		case ConsumeItemRequest.EQUIP_PANTS:
			return hasEquipped( name, PANTS );

		case ConsumeItemRequest.EQUIP_ACCESSORY:
			return hasEquipped( name, ACCESSORY1 ) || hasEquipped( name, ACCESSORY2 ) || hasEquipped( name, ACCESSORY3 );

		case ConsumeItemRequest.EQUIP_FAMILIAR:
			return hasEquipped( name, FAMILIAR );
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
	private static final int TICK = 58;
	private static final int TROLL = 65;

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
	private static final int JITTERBUG = 57;
	private static final int CRIMBO_ELF = 26;

	// Items and skills that make Mysticality the To-Hit stat
	private static final int SAUCE_GLOVE = 531;

	public static boolean recalculateAdjustments()
	{
		int newMonsterLevelAdjustment = 0;
		int newFamiliarWeightAdjustment = 0;
		int newDodecapedeWeightAdjustment = 0;
		int newFamiliarItemWeightAdjustment = 0;
		int newManaCostModifier = 0;

		float newCombatPercentAdjustment = 0.0f;
		float newInitiativeAdjustment = 0.0f;
		float newFixedXPAdjustment = 0.0f;
		float newMeatDropPercentAdjustment = 0.0f;
		float newItemDropPercentAdjustment = 0.0f;

		boolean rigatoniSkill = false;
		boolean hasStaff = false;
		boolean newRigatoniActive = false;

		int newDamageAbsorption = 0;
		int newDamageReduction = 0;
		float newColdResistance = 0;
		float newHotResistance = 0;
		float newSleazeResistance = 0;
		float newSpookyResistance = 0;
		float newStenchResistance = 0;

		int taoFactor = hasSkill( "Tao of the Terrapin" ) ? 2 : 1;

		int familiarId = currentFamiliar.getId();

		// Look at mind control level
		newMonsterLevelAdjustment += getMindControlLevel();

		// Look at items
		for ( int slot = HAT; slot <= FAMILIAR; ++slot )
		{
			AdventureResult item = getEquipment( slot );
			if ( item == null )
				continue;

			float [] modifiers = StatusEffectDatabase.getModifiers( item.getName() );

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
			newManaCostModifier += modifiers[ StatusEffectDatabase.MANA_COST_MODIFIER ];

			switch ( slot )
			{
			case WEAPON:
				hasStaff = EquipmentDatabase.isStaff( item.getItemId() );
				break;

			case FAMILIAR:
				newFamiliarItemWeightAdjustment = FamiliarData.itemWeightModifier( item.getItemId() );
				break;

			case HAT:
			case PANTS:
				newDamageAbsorption += taoFactor * EquipmentDatabase.getPower( item.getItemId() );
				break;

			case SHIRT:
				newDamageAbsorption += EquipmentDatabase.getPower( item.getItemId() );
				break;
			}

			switch ( item.getItemId() )
			{
			case JEKYLLIN:
				newItemDropPercentAdjustment += 15 + MoonPhaseDatabase.getMoonlight() * 5;
				break;

			case SAUCE_GLOVE:
				if ( classtype.startsWith( "Sa" ) )
					rigatoniSkill = true;
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
		else if ( EquipmentDatabase.isWearingOutfit( 14 ) )
		{
			// Furry Suit
			newMonsterLevelAdjustment += 5;
		}

		// Wearing a serpentine sword and a serpentine shield doubles
		// the effect of the sword.

		if ( getEquipment( WEAPON ).getName().equals( "serpentine sword" ) && getEquipment( OFFHAND ).getName().equals( "snake shield" ) )
			newMonsterLevelAdjustment += 10;

		// Because there are a limited number of passive skills,
		// it is much more efficient to execute one check for
		// each of the known skills.

		if ( hasSkill( "Amphibian Sympathy" ) )
		{
			newFamiliarWeightAdjustment += 5;
			newDodecapedeWeightAdjustment -= 5;
		}

		if ( hasSkill( "Cold-Blooded Fearlessness" ) )
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
			// Varies according to level, somehow
			;

		if ( hasSkill( "Spirit of Rigatoni" ) )
			rigatoniSkill = true;

		if ( hasSkill( "Tolerance of the Kitchen" ) )
			newHotResistance += 20;

		// For the sake of easier maintenance, execute a lot of extra
		// extra string comparisons when looking at status effects.

		AdventureResult [] effects = new AdventureResult[ activeEffects.size() ];
		activeEffects.toArray( effects );

		for ( int i = 0; i < effects.length; ++i )
		{
			float [] modifiers = StatusEffectDatabase.getModifiers( effects[i].getName() );

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
			newManaCostModifier += modifiers[ StatusEffectDatabase.MANA_COST_MODIFIER ];
		}

		if ( ARIA.getCount( activeEffects ) > 0 )
			newMonsterLevelAdjustment += 2 * getLevel();

		// Now that we have calculated the familiar weight adjustment,
		// look at familiar.

		float modifier = (float)( currentFamiliar.getWeight() + newFamiliarWeightAdjustment + newFamiliarItemWeightAdjustment );
		switch ( familiarId )
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
		case JITTERBUG:
			// Full gravy fairy equivalent familiar
			// Full leprechaun equivalent familiar
			newItemDropPercentAdjustment += modifier * 2.5;
			newMeatDropPercentAdjustment += modifier * 5;
			break;

		case VOLLEYBALL:
		case HOBO:
		case TROLL:
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
		case TICK:
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

		return changed;
	}
}
