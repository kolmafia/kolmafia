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
	public static final AdventureResult MAX_HOT = new AdventureResult( "Fireproof Lips", 1, true );
	public static final AdventureResult MAX_COLD = new AdventureResult( "Fever from the Flavor", 1, true );
	public static final AdventureResult MAX_SPOOKY = new AdventureResult( "Hyphemariffic", 1, true );
	public static final AdventureResult MAX_STENCH = new AdventureResult( "Can't Smell Nothin'", 1, true );
	public static final AdventureResult MAX_SLEAZE = new AdventureResult( "Hyperoffended", 1, true );

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

	private static final int BAKULA = 1519;
	private static final int WIZARD_HAT = 1653;

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

	// General static variables

	private static String username = "";
	private static String avatar = "otherimages/discobandit_f.gif";
	private static int userId = 0;
	private static String playerId = "0";
	private static String classname = "";
	private static String classtype = "";
	private static int currentLevel = 1;
	private static int decrementPrime = 0;
	private static int incrementPrime = 0;

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
		for ( int i = 0; i < 9; ++i )
			equipment.add( EquipmentRequest.UNEQUIP );
	}

	public static SortedListModel battleSkillNames = new SortedListModel();

	private static LockableListModel accessories = new SortedListModel();
	private static LockableListModel [] equipmentLists = new LockableListModel[9];

	static
	{
		for ( int i = 0; i < 9; ++i )
		{
			switch ( i )
			{
			case ACCESSORY1:
			case ACCESSORY2:
			case ACCESSORY3:
				equipmentLists[i] = accessories.getMirrorImage();
				break;

			default:
				equipmentLists[i] = new SortedListModel();
				break;
			}
		}
	}

	// Status pane data which is rendered whenever
	// the user issues a "status" type command.

	private static int pvpRank = 0;
	private static int attacksLeft = 0;
	private static int availableMeat = 0;
	private static int storageMeat = 0;
	private static int closetMeat = 0;
	private static int inebriety = 0;
	private static int adventuresLeft = 0;
	private static int currentRun = 0;
	private static boolean isFullnessIncreased = false;

	// Status pane data which is rendered whenever
	// the user changes equipment, effects, and familiar

	private static final Modifiers currentModifiers = new Modifiers();

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

	private static int ascensions = 0;
	private static String ascensionSign = "None";
	private static int ascensionSignType = NONE;
	private static int consumptionRestriction = AscensionSnapshotTable.NOPATH;
	private static int mindControlLevel = 0;
	private static int detunedRadioVolume = 0;
	private static int annoyotronLevel = 0;

	private static String autosellMode = "";

	public static void resetInventory()
	{
		inventory.clear();

		// Initialize the equipment lists inside
		// of the character data

		for ( int i = 0; i < equipmentLists.length; ++i )
			equipmentLists[i].clear();

		accessories.clear();
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

	public static final void reset( String newUserName )
	{
		username = newUserName;
		KoLCharacter.reset();
	}

	public static void reset()
	{
		classname = "";
		classtype = "";

		currentLevel = 1;
		decrementPrime = 0;
		incrementPrime = 0;

		pvpRank = 0;
		attacksLeft = 0;
		adjustedStats = new int[3];
		totalSubpoints = new int[3];

		currentModifiers.reset();

		equipment.clear();
		for ( int i = 0; i < 9; ++i )
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
		battleSkillNames.add( "item turtle totem" );
		battleSkillNames.add( "item spices" );

		battleSkillNames.add( "try to run away" );

		isHardcore = false;
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
		detunedRadioVolume = 0;
		annoyotronLevel = 0;

		autosellMode = "";

		// Clear some of the standard lists so they don't
		// carry over from player to player.

		conditions.clear();
		eventHistory.clear();
		recentEffects.clear();
		activeEffects.clear();

		resetInventory();

		int battleIndex = battleSkillNames.indexOf( StaticEntity.getProperty( "battleAction" ) );
		battleSkillNames.setSelectedIndex( battleIndex == -1 ? 0 : battleIndex );
	}

	public static void setHoliday( String holiday )
	{	isFullnessIncreased = holiday.equals( "Feast of Boris" );
	}

	public static int getFullness()
	{	return StaticEntity.getIntegerProperty( "currentFullness" );
	}

	public static int getFullnessLimit()
	{
		int baseFullness = hasSkill( "Stomach of Steel" ) ? 20 : canEat() ? 15 : 0;
		return baseFullness == 0 ? 0 : isFullnessIncreased ? baseFullness + 15 : baseFullness;
	}

	public static void setInebriety( int inebriety )
	{	KoLCharacter.inebriety = inebriety;
	}

	public static int getInebriety()
	{	return inebriety;
	}

	public static int getInebrietyLimit()
	{	return hasSkill( "Liver of Steel" ) ? 19 : canDrink() ? 14 : 0;
	}

	public static boolean isFallingDown()
	{	return getInebriety() > getInebrietyLimit();
	}

	public static int getSpleenUse()
	{	return StaticEntity.getIntegerProperty( "currentSpleenUse" );
	}

	public static int getSpleenLimit()
	{	return hasSkill( "Spleen of Steel" ) ? 20 : 15;
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
	{
		KoLCharacter.userId = userId;
		KoLCharacter.playerId = String.valueOf( userId );
	}

	/**
	 * Accessor method to retrieve the user Id associated with this character.
	 * @return	The user Id associated with this character
	 */

	public static String getPlayerId()
	{	return playerId;
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
	{
		int totalPrime = getTotalPrime();

		if ( totalPrime <= decrementPrime || totalPrime >= incrementPrime )
		{
			currentLevel = (int) Math.sqrt( calculateBasePoints( getTotalPrime() ) - 4 ) + 1;
			decrementPrime = calculateLastLevel();
			incrementPrime = calculateNextLevel();
		}

		return currentLevel;
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

	public static String getClassTypeAbbreviation()
	{
		String type = getClassType();
		if ( type.equals( SEAL_CLUBBER ) )
			return "SC";
		if ( type.equals( TURTLE_TAMER ) )
			return "TT";
		if ( type.equals( PASTAMANCER ) )
			return "PA";
		if ( type.equals( SAUCEROR ) )
			return "SA";
		if ( type.equals( DISCO_BANDIT ) )
			return "DB";
		if ( type.equals( ACCORDION_THIEF ) )
			return "AT";
		return "??";
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

	public static void setStorageMeat( int storageMeat )
	{	KoLCharacter.storageMeat = storageMeat;
	}

	public static int getStorageMeat()
	{	return storageMeat;
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
		if ( KoLCharacter.availableMeat != availableMeat )
			KoLCharacter.availableMeat = availableMeat;
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
		int level = currentLevel - 1;
		int basePointsNeeded = level * level + 4;
		return basePointsNeeded * basePointsNeeded - 1;
	}

	/**
	 * Returns the total number of subpoints to the next level.
	 * @return	The total subpoints to the next level
	 */

	public static int calculateNextLevel()
	{
		int level = currentLevel;
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
	 * Accessor method to set the number of adventures the character has left to
	 * spend in this session.
	 *
	 * @param	adventuresLeft	The number of adventures the character has left
	 */

	public static void setAdventuresLeft( int adventuresLeft )
	{
		if ( adventuresLeft != KoLCharacter.adventuresLeft )
		{
			KoLCharacter.adventuresLeft = adventuresLeft;
			if ( (canEat() && !hasChef()) || (canDrink() && !hasBartender()) )
				ConcoctionsDatabase.refreshConcoctions();
		}
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

	public static void setCurrentRun( int currentRun )
	{	KoLCharacter.currentRun = currentRun;
	}

	/**
	 * Accessor method to retrieve the total number of turns the character has used
	 * since creation.  This method is only interesting from an averages point of
	 * view, but sometimes, it's interesting to know.
	 */

	public static int getCurrentRun()
	{	return currentRun;
	}

	/**
	 * Accessor method to retrieve the total current monster level
	 * adjustment
	 */

	public static int getMonsterLevelAdjustment()
	{	return (int) currentModifiers.get( Modifiers.MONSTER_LEVEL );
	}

	/**
	 * Accessor method to retrieve the total current familiar weight
	 * adjustment
	 */

	public static int getFamiliarWeightAdjustment()
	{	return (int) currentModifiers.get( Modifiers.FAMILIAR_WEIGHT );
	}

	public static int getManaCostAdjustment()
	{	return (int) currentModifiers.get( Modifiers.MANA_COST );
	}

	/**
	 * Accessor method to retrieve the total current combat percent
	 * adjustment
	 */

	public static float getCombatRateAdjustment()
	{	return currentModifiers.get( Modifiers.COMBAT_RATE );
	}

	/**
	 * Accessor method to retrieve the total current initiative
	 * adjustment
	 */

	public static float getInitiativeAdjustment()
	{	return currentModifiers.get( Modifiers.INITIATIVE );
	}

	/**
	 * Accessor method to retrieve the total current fixed experience
	 * adjustment
	 */

	public static float getExperienceAdjustment()
	{	return currentModifiers.get( Modifiers.EXPERIENCE );
	}

	/**
	 * Accessor method to retrieve the total current meat drop percent
	 * adjustment
	 *
	 * @return	Total Current Meat Drop Percent Adjustment
	 */

	public static float getMeatDropPercentAdjustment()
	{	return currentModifiers.get( Modifiers.MEATDROP );
	}

	/**
	 * Accessor method to retrieve the total current item drop percent
	 * adjustment
	 *
	 * @return	Total Current Item Drop Percent Adjustment
	 */

	public static float getItemDropPercentAdjustment()
	{	return currentModifiers.get( Modifiers.ITEMDROP );
	}

	public static void setEquipment( int slot, AdventureResult item )
	{
		// Accessories are special in terms of testing for existence
		// in equipment lists -- they are all mirrors of accessories.

		switch ( slot )
		{
		case ACCESSORY1:
		case ACCESSORY2:
		case ACCESSORY3:
			if ( !accessories.contains( item ) )
				accessories.add( item );
			break;

		default:
			if ( !equipmentLists[slot].contains( item ) )
				equipmentLists[slot].add( item );
			break;
		}

		equipment.set( slot, item );
		equipmentLists[slot].setSelectedItem( item );

		// Certain equipment slots require special update handling
		// in addition to the above code.

		switch ( slot )
		{
		case WEAPON:
		case OFFHAND:
			GearChangeFrame.updateWeapons();
			break;
		case FAMILIAR:
			if ( currentFamiliar.getId() > 0 )
				currentFamiliar.setItem( item );
			break;
		}

		// Certain items provide additional skills when equipped.
		// Handle the addition of those skills here.

		switch ( item.getItemId() )
		{
		case WIZARD_HAT:
			addAvailableSkill( UseSkillRequest.getInstance( "Magic Missile" ) );
			break;
		case BAKULA:
			addAvailableSkill( UseSkillRequest.getInstance( "Give In To Your Vampiric Urges" ) );
			break;
		}
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
		for ( int i = HAT; i <= FAMILIAR; ++i )
		{
			if ( equipment[i] == null || equipment[i].equals( EquipmentRequest.UNEQUIP ) )
				setEquipment( i, EquipmentRequest.UNEQUIP );
			else
				setEquipment( i, equipment[i] );
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
		if ( type >= 0 && type < equipment.size() )
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
	 * Accessor method to determine character's hit stat
	 * @return	int	MUSCLE, MYSTICALITY, MOXIE
	 */

	public static int equipStat()
	{	return EquipmentDatabase.equipStat( getEquipment( WEAPON ).getName() );
	}

	public static int hitStat()
	{	return rangedWeapon() ? MOXIE : MUSCLE;
	}

	/**
	 * Accessor method to determine character's adjusted hit stat
	 * @return	int	adjusted muscle, mysticality, or moxie
	 */

	public static int getAdjustedHitStat()
	{
		switch ( hitStat() )
		{
		case MOXIE:  return getAdjustedMoxie();
		case MYSTICALITY:  return getAdjustedMysticality();
		default:  return getAdjustedMuscle();
		}
	}

	/**
	 * Accessor method to determine if character's weapon is ranged
	 * @return	boolean	true if weapon is ranged
	 */

	public static boolean rangedWeapon()
	{	return EquipmentDatabase.isRanged( getEquipment( WEAPON ).getName() );
	}

	/**
	 * Accessor method to retrieve the total current damage absorption
	 *
	 * @return	Total Current Damage Absorption
	 */

	public static int getDamageAbsorption()
	{	return (int) currentModifiers.get( Modifiers.DAMAGE_ABSORPTION );
	}

	/**
	 * Accessor method to retrieve the total current damage reduction
	 *
	 * @return	Total Current Damage Reduction
	 */

	public static int getDamageReduction()
	{	return (int) currentModifiers.get( Modifiers.DAMAGE_REDUCTION );
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
			return currentModifiers.get( Modifiers.COLD_RESISTANCE );
		case MonsterDatabase.HEAT:
			return currentModifiers.get( Modifiers.HOT_RESISTANCE );
		case MonsterDatabase.SLEAZE:
			return currentModifiers.get( Modifiers.SLEAZE_RESISTANCE );
		case MonsterDatabase.SPOOKY:
			return currentModifiers.get( Modifiers.SPOOKY_RESISTANCE );
		case MonsterDatabase.STENCH:
			return currentModifiers.get( Modifiers.STENCH_RESISTANCE );
		}

		return 0.0f;
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
		case EQUIP_HAT:
			return HAT;
		case EQUIP_WEAPON:
			return WEAPON;
		case EQUIP_OFFHAND:
			return OFFHAND;
		case EQUIP_SHIRT:
			return SHIRT;
		case EQUIP_PANTS:
			return PANTS;
		case EQUIP_ACCESSORY:
			return ACCESSORY1;
		case EQUIP_FAMILIAR:
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
			return EQUIP_HAT;
		case WEAPON:
			return EQUIP_WEAPON;
		case OFFHAND:
			return EQUIP_OFFHAND;
		case SHIRT:
			return EQUIP_SHIRT;
		case PANTS:
			return EQUIP_PANTS;
		case ACCESSORY1:
		case ACCESSORY2:
		case ACCESSORY3:
			return EQUIP_ACCESSORY;
		case FAMILIAR:
			return EQUIP_FAMILIAR;
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

	public static void updateEquipmentList( int listIndex )
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

			updateEquipmentList( consumeFilter, accessories );
			break;

		case FAMILIAR:

			// If we are looking at familiar items, include those which can
			// be universally equipped, but are currently on another
			// familiar.

			updateEquipmentList( consumeFilter, equipmentLists[ listIndex ] );

			FamiliarData [] familiarList = new FamiliarData[ familiars.size() ];
			familiars.toArray( familiarList );

			for ( int i = 0; i < familiarList.length; ++i )
				if ( !familiarList[i].getItem().equals( EquipmentRequest.UNEQUIP ) )
					AdventureResult.addResultToList( equipmentLists[ FAMILIAR ], familiarList[i].getItem() );

			break;

		default:

			updateEquipmentList( consumeFilter, equipmentLists[ listIndex ] );
			if ( !equipmentLists[ listIndex ].contains( equippedItem ) )
				equipmentLists[ listIndex ].add( equippedItem );

			break;
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
		int equipStat = equipStat();

		for ( int i = 0; i < inventory.size(); ++i )
		{
			AdventureResult currentItem = (AdventureResult) inventory.get(i);
			String currentItemName = currentItem.getName();

			int type = TradeableItemDatabase.getConsumptionType( currentItem.getItemId() );

			// If we are equipping familiar items, make sure
			// current familiar can use this one

			if ( filterId == EQUIP_FAMILIAR && type == EQUIP_FAMILIAR )
			{
				temporary.add( currentItem );
				continue;
			}

			// If we want off-hand items and we can dual wield,
			// allow one-handed weapons of same type

			if ( filterId == EQUIP_OFFHAND && type == EQUIP_WEAPON && dual )
			{
				if ( EquipmentDatabase.getHands( currentItemName ) != 1 || EquipmentDatabase.equipStat( currentItemName ) != equipStat )
					continue;
			}

			// Otherwise, slot and item type must match

			else if ( filterId != type )
				continue;

			// If we are currently dual-wielding, only weapons of
			// the current type are allowed in the main weapon slot.
			// Two-handed weapons are also allowed since they will
			// remove both weapons when equipped

			else if ( filterId == EQUIP_WEAPON && dual )
			{
				if ( EquipmentDatabase.getHands( currentItemName ) == 1 && EquipmentDatabase.equipStat( currentItemName ) != equipStat )
					continue;
			}

			temporary.add( currentItem );
		}

		if ( currentList == accessories )
		{
			if ( !currentList.contains( getEquipment( ACCESSORY1 ) ) )
				currentList.add( getEquipment( ACCESSORY1 ) );
			if ( !currentList.contains( getEquipment( ACCESSORY2 ) ) )
				currentList.add( getEquipment( ACCESSORY2 ) );
			if ( !currentList.contains( getEquipment( ACCESSORY3 ) ) )
				currentList.add( getEquipment( ACCESSORY3 ) );
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
		if ( KoLCharacter.hasBartender != hasBartender )
		{
			KoLCharacter.hasBartender = hasBartender;
			ConcoctionsDatabase.refreshConcoctions();
		}
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
		if ( KoLCharacter.hasChef != hasChef )
		{
			KoLCharacter.hasChef = hasChef;
			ConcoctionsDatabase.refreshConcoctions();
		}
	}

	/**
	 * Accessor method which tells you if the character can interact
	 * with other players (Ronin or Hardcore players cannot).
	 */

	public static boolean canInteract()
	{	return CharpaneRequest.canInteract();
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
	 * Accessor method for the current detuned radio volume
	 * @return	int
	 */

	public static int getDetunedRadioVolume()
	{	return detunedRadioVolume;
	}

	/**
	 * Accessor method to set  the current detuned radio volume
	 * @param	volume	the new level
	 */

	public static void setDetunedRadioVolume( int volume )
	{
		KoLCharacter.detunedRadioVolume = volume;
		recalculateAdjustments();
		updateStatus();
	}

	/**
	 * Accessor method for the current Annoyotron level
	 * @return	int
	 */

	public static int getAnnoyotronLevel()
	{	return annoyotronLevel;
	}

	/**
	 * Accessor method to set  the current Annoyotron level
	 * @param	volume	the new level
	 */

	public static void setAnnoyotronLevel( int level )
	{
		KoLCharacter.annoyotronLevel = level;
		recalculateAdjustments();
		updateStatus();
	}

	/**
	 * Accessor method for the current sign-specific monster level modifier
	 * @return	int
	 */

	public static int getSignedMLAdjustment()
	{
		if ( mindControlLevel > 0 )
			return mindControlLevel;
		if ( detunedRadioVolume > 0 )
			return detunedRadioVolume;
		if ( annoyotronLevel > 0 )
			return annoyotronLevel;
		return 0;
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
				usableSkills.sort();
			}

			break;

		case ClassSkillsDatabase.SELF_ONLY:
		case ClassSkillsDatabase.BUFF:

			usableSkills.add( skill );
			usableSkills.sort();
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
	 * Accessor method to look up whether or not the character can
	 * craft expensive jewelry
	 *
	 * @return	<code>true</code> if this character can smith advanced weapons
	 */

	public static boolean canCraftExpensiveJewelry()
	{	return hasSkill( "Really Expensive Jewelrycrafting" );
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
		if ( !hasSkill( "Superhuman Cocktailcrafting" ) || !isMoxieClass() || KoLRequest.sessionId == null )
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
		equipmentLists[FAMILIAR].setSelectedItem( currentFamiliar.getItem() );

		isUsingStabBat = currentFamiliar.getRace().equals( "Stab Bat" ) || currentFamiliar.getRace().equals( "Scary Death Orb" );

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
		if ( familiar == null )
			return null;

		int index = familiars.indexOf( familiar );
		if ( index >= 0 )
			return (FamiliarData) familiars.get( index );

		familiars.add( familiar );
		if ( !familiar.getItem().equals( EquipmentRequest.UNEQUIP ) )
			AdventureResult.addResultToList( equipmentLists[ FAMILIAR ], familiar.getItem() );

		return familiar;
	}

	/**
	 * Remove the given familiar from the list of available familiars.
	 * @param	familiar	The Id of the familiar to be added
	 */

	public static void removeFamiliar( FamiliarData familiar )
	{
		if ( familiar == null )
			return;

		int index = familiars.indexOf( familiar );
		if ( index < 0 )
			return;

		if ( currentFamiliar == familiar )
		{
			currentFamiliar = FamiliarData.NO_FAMILIAR;
			setEquipment( KoLCharacter.FAMILIAR, EquipmentRequest.UNEQUIP );
		}

		familiars.remove( familiar );
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
				int consumeType = TradeableItemDatabase.getConsumptionType( result.getItemId() );

				if ( consumeType == EQUIP_ACCESSORY )
				{
					AdventureResult.addResultToList( equipmentLists[ ACCESSORY1 ], result );
					AdventureResult.addResultToList( equipmentLists[ ACCESSORY2 ], result );
					AdventureResult.addResultToList( equipmentLists[ ACCESSORY3 ], result );
				}
				else
				{
					int equipmentType = consumeFilterToEquipmentType( consumeType );
					if ( equipmentType != -1 )
						AdventureResult.addResultToList( equipmentLists[ equipmentType ], result );
				}

				if ( EquipmentDatabase.getOutfitWithItem( result.getItemId() ) != -1 )
					EquipmentDatabase.updateOutfits();

				boolean shouldRefresh = false;
				List uses = ConcoctionsDatabase.getKnownUses( result );

				for ( int i = 0; i < uses.size() && !shouldRefresh; ++i )
					shouldRefresh = ConcoctionsDatabase.isPermittedMethod( ConcoctionsDatabase.getMixingMethod( ((AdventureResult)uses.get(i)).getItemId() ) );

				if ( shouldRefresh )
				{
					ConcoctionsDatabase.refreshConcoctions();
					ConcoctionsDatabase.getUsables().applyListFilters();
				}
				else if ( consumeType == CONSUME_EAT || consumeType == CONSUME_DRINK )
				{
					ConcoctionsDatabase.refreshConcoctions();
					ConcoctionsDatabase.getUsables().applyListFilters();
				}
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
					if ( effect.getCount() + result.getCount() <= 0 )
						activeEffects.remove( i );
					else
						activeEffects.set( i, effect.getInstance( effect.getCount() + result.getCount() ) );
				}

				setCurrentRun( currentRun - result.getCount() );
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

	private static final AdventureResult DEAD_MIMIC = new AdventureResult( 1267, 1 );
	private static final AdventureResult [] WANDS = new AdventureResult[] {
		new AdventureResult( 1268, 1 ),  // pine wand
		new AdventureResult( 1269, 1 ),  // ebony wand
		new AdventureResult( 1270, 1 ),  // hexagonal wand
		new AdventureResult( 1271, 1 ),  // aluminum wand
		new AdventureResult( 1272, 1 )   // marble wand
	};

	/**
	 * Returns the character's zapping wand, if any
	 */

	public static AdventureResult getZapper()
	{
		if ( inventory.contains( DEAD_MIMIC ) )
			RequestThread.postRequest( new ConsumeItemRequest( DEAD_MIMIC ) );

		for ( int i = 0; i < WANDS.length; ++i )
			if ( inventory.contains( WANDS[i] ) )
				return WANDS[i];

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
		case EQUIP_HAT:
		case EQUIP_PANTS:
		case EQUIP_FAMILIAR:
		case EQUIP_OFFHAND:
			if ( hasEquipped( item ) )  ++count;
			break;

		case EQUIP_WEAPON:
			if ( hasEquipped( item, WEAPON ) )  ++count;
			if ( hasEquipped( item, OFFHAND ) )  ++count;
			break;

		case EQUIP_ACCESSORY:
			if ( hasEquipped( item, ACCESSORY1 ) )  ++count;
			if ( hasEquipped( item, ACCESSORY2 ) )  ++count;
			if ( hasEquipped( item, ACCESSORY3 ) )  ++count;
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

	public static boolean hasEquipped( AdventureResult item, int equipmentSlot )
	{	return getEquipment( equipmentSlot ).getItemId() == item.getItemId();
	}

	public static boolean hasEquipped( AdventureResult item )
	{
		switch ( TradeableItemDatabase.getConsumptionType( item.getItemId() ) )
		{
		case EQUIP_WEAPON:
			return hasEquipped( item, WEAPON ) || hasEquipped( item, OFFHAND );

		case EQUIP_OFFHAND:
			return hasEquipped( item, OFFHAND );

		case EQUIP_HAT:
			return hasEquipped( item, HAT );

		case EQUIP_SHIRT:
			return hasEquipped( item, SHIRT );

		case EQUIP_PANTS:
			return hasEquipped( item, PANTS );

		case EQUIP_ACCESSORY:
			return hasEquipped( item, ACCESSORY1 ) || hasEquipped( item, ACCESSORY2 ) || hasEquipped( item, ACCESSORY3 );

		case EQUIP_FAMILIAR:
			return hasEquipped( item, FAMILIAR );
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

	// Items that modify based on moon signs
	private static final int JEKYLLIN = 1291;
	private static final int GUAYABERA = 1546;

	public static boolean recalculateAdjustments()
	{
		int taoFactor = hasSkill( "Tao of the Terrapin" ) ? 2 : 1;
		Modifiers newModifiers = new Modifiers();

		// Look at sign-specific adjustments

		newModifiers.add( Modifiers.MONSTER_LEVEL, getSignedMLAdjustment() );

		// Look at items
		for ( int slot = HAT; slot <= FAMILIAR; ++slot )
		{
			AdventureResult item = getEquipment( slot );
			if ( item == null )
				continue;

			newModifiers.add( Modifiers.getModifiers( item.getName() ) );

			switch ( slot )
			{
			case WEAPON:
			case OFFHAND:
			case FAMILIAR:
				break;

			case HAT:
			case PANTS:
				newModifiers.add( Modifiers.DAMAGE_ABSORPTION, taoFactor * EquipmentDatabase.getPower( item.getItemId() ) );
				break;

			case SHIRT:
				newModifiers.add( Modifiers.DAMAGE_ABSORPTION, EquipmentDatabase.getPower( item.getItemId() ) );
				break;
			}

			switch ( item.getItemId() )
			{
			case GUAYABERA:
				newModifiers.add( Modifiers.MONSTER_LEVEL, MoonPhaseDatabase.getGrimaciteEffect() );
				break;

			case JEKYLLIN:
				newModifiers.add( Modifiers.ITEMDROP, 15 + MoonPhaseDatabase.getMoonlight() * 5 );
				break;
			}
		}

		// Certain outfits give benefits to the character
		if ( EquipmentDatabase.isWearingOutfit( 6 ) )
		{
			// Hot and Cold Running Ninja Suit
			newModifiers.add( Modifiers.COLD_RESISTANCE, 20);
			newModifiers.add( Modifiers.HOT_RESISTANCE, 20);
		}
		else if ( EquipmentDatabase.isWearingOutfit( 7 ) )
		{
			// eXtreme Cold-Weather Gear
			newModifiers.add( Modifiers.COLD_RESISTANCE, 30);
		}
		else if ( EquipmentDatabase.isWearingOutfit( 25 ) )
		{
			// Arboreal Raiment
			newModifiers.add( Modifiers.STENCH_RESISTANCE, 10 );
		}
		else if ( EquipmentDatabase.isWearingOutfit( 14 ) )
		{
			// Furry Suit
			newModifiers.add( Modifiers.MONSTER_LEVEL, 5 );
			newModifiers.add( Modifiers.MOX_PCT, -75 );
		}

		// Wearing a serpentine sword and a serpentine shield doubles
		// the effect of the sword.

		if ( getEquipment( WEAPON ).getName().equals( "serpentine sword" ) && getEquipment( OFFHAND ).getName().equals( "snake shield" ) )
			newModifiers.add( Modifiers.MONSTER_LEVEL, 10 );

		// Because there are a limited number of passive skills,
		// it is much more efficient to execute one check for
		// each of the known skills.

		newModifiers.applyPassiveModifiers();

		// For the sake of easier maintenance, execute a lot of extra
		// extra string comparisons when looking at status effects.

		for ( int i = 0; i < activeEffects.size(); ++i )
			newModifiers.add( Modifiers.getModifiers( ((AdventureResult)activeEffects.get(i)).getName() ) );

		if ( ARIA.getCount( activeEffects ) > 0 )
			newModifiers.add( Modifiers.MONSTER_LEVEL, 2 * getLevel() );

		// Now that we have calculated the familiar weight adjustment,
		// look at familiar.

		float modifier = (float)( currentFamiliar.getWeight() + newModifiers.get( Modifiers.FAMILIAR_WEIGHT ) );
		int familiarId = currentFamiliar.getId();

		if ( FamiliarsDatabase.isVolleyType( familiarId ) )
			newModifiers.add( Modifiers.EXPERIENCE, Math.sqrt( modifier ) );
		if ( FamiliarsDatabase.isItemDropType( familiarId ) )
			newModifiers.add( Modifiers.ITEMDROP, modifier * 2.5 );
		if ( FamiliarsDatabase.isMeatDropType( familiarId ) )
			newModifiers.add( Modifiers.MEATDROP, modifier * 5 );

		// Make sure the mana modifier is no more than
		// three, no matter what.  Also make sure that
		// elemental resistance is capped by class.

		newModifiers.set( Modifiers.MANA_COST, Math.max( newModifiers.get( Modifiers.MANA_COST ), -3 ) );

		newModifiers.set( Modifiers.HOT_RESISTANCE, activeEffects.contains( MAX_HOT ) ? 90 :
			Math.max( newModifiers.get( Modifiers.HOT_RESISTANCE ), KoLCharacter.isMysticalityClass() ? 80 : 60 ) );
		newModifiers.set( Modifiers.COLD_RESISTANCE, activeEffects.contains( MAX_COLD ) ? 90 :
			Math.max( newModifiers.get( Modifiers.COLD_RESISTANCE ), KoLCharacter.isMysticalityClass() ? 80 : 60 ) );
		newModifiers.set( Modifiers.SPOOKY_RESISTANCE, activeEffects.contains( MAX_SPOOKY ) ? 90 :
			Math.max( newModifiers.get( Modifiers.SPOOKY_RESISTANCE ), KoLCharacter.isMysticalityClass() ? 80 : 60 ) );
		newModifiers.set( Modifiers.STENCH_RESISTANCE, activeEffects.contains( MAX_STENCH ) ? 90 :
			Math.max( newModifiers.get( Modifiers.STENCH_RESISTANCE ), KoLCharacter.isMysticalityClass() ? 80 : 60 ) );
		newModifiers.set( Modifiers.SLEAZE_RESISTANCE, activeEffects.contains( MAX_SLEAZE ) ? 90 :
			Math.max( newModifiers.get( Modifiers.SLEAZE_RESISTANCE ), KoLCharacter.isMysticalityClass() ? 80 : 60 ) );

		// Add in strung-up quartet.

		if ( KoLCharacter.getAscensions() == StaticEntity.getIntegerProperty( "lastQuartetAscension" ) )
		{
			switch ( StaticEntity.getIntegerProperty( "lastQuartetRequest" ) )
			{
			case 1:
				newModifiers.add( Modifiers.MONSTER_LEVEL, 5 );
				break;
			case 2:
				newModifiers.add( Modifiers.COMBAT_RATE, -5 );
				break;
			case 3:
				newModifiers.add( Modifiers.ITEMDROP, 5 );
				break;
			}
		}

		// Lastly, experience adjustment also implicitly depends on
		// monster level.  Add that information.

		newModifiers.add( Modifiers.EXPERIENCE, newModifiers.get( Modifiers.MONSTER_LEVEL) / 4.0f );

		// Determine whether or not data has changed

		boolean changed = currentModifiers.set( newModifiers );
		return changed;
	}
}
