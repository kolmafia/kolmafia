/**
 * Copyright (c) 2005-2011, KoLmafia development team
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

import apple.dts.samplecode.osxadapter.OSXAdapter;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import net.java.dev.spellcast.utilities.LockableListModel;
import net.java.dev.spellcast.utilities.SortedListModel;

import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.session.ClanManager;
import net.sourceforge.kolmafia.session.DisplayCaseManager;
import net.sourceforge.kolmafia.session.EquipmentManager;
import net.sourceforge.kolmafia.session.EventManager;
import net.sourceforge.kolmafia.session.InventoryManager;
import net.sourceforge.kolmafia.session.StoreManager;
import net.sourceforge.kolmafia.session.TurnCounter;
import net.sourceforge.kolmafia.session.VolcanoMazeManager;
import net.sourceforge.kolmafia.session.WumpusManager;
import net.sourceforge.kolmafia.utilities.FileUtilities;

import net.sourceforge.kolmafia.request.CampgroundRequest;
import net.sourceforge.kolmafia.request.CharPaneRequest;
import net.sourceforge.kolmafia.request.ChezSnooteeRequest;
import net.sourceforge.kolmafia.request.DwarfFactoryRequest;
import net.sourceforge.kolmafia.request.EquipmentRequest;
import net.sourceforge.kolmafia.request.FightRequest;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.request.GuildRequest;
import net.sourceforge.kolmafia.request.HellKitchenRequest;
import net.sourceforge.kolmafia.request.MicroBreweryRequest;
import net.sourceforge.kolmafia.request.TelescopeRequest;
import net.sourceforge.kolmafia.request.UseItemRequest;
import net.sourceforge.kolmafia.request.UseSkillRequest;

import net.sourceforge.kolmafia.persistence.AscensionSnapshot;
import net.sourceforge.kolmafia.persistence.ConcoctionDatabase;
import net.sourceforge.kolmafia.persistence.EquipmentDatabase;
import net.sourceforge.kolmafia.persistence.HolidayDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.persistence.MonsterDatabase;
import net.sourceforge.kolmafia.persistence.SkillDatabase;
import net.sourceforge.kolmafia.preferences.PreferenceListenerRegistry;
import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.session.LouvreManager;
import net.sourceforge.kolmafia.session.MoneyMakingGameManager;
import net.sourceforge.kolmafia.session.VioletFogManager;

import net.sourceforge.kolmafia.swingui.AdventureFrame;

import net.sourceforge.kolmafia.webui.DiscoCombatHelper;

/**
 * A container class representing the <code>KoLCharacter</code>. This class also allows for data listeners that are
 * updated whenever the character changes; ultimately, the purpose of this class is to shift away from the
 * centralized-notification paradigm (inefficient) towards a listener paradigm, which is both cleaner and easier to
 * manage with regards to extensions. In addition, it loosens the coupling between the various aspects of
 * <code>KoLmafia</code>, leading to extensibility.
 */

public abstract class KoLCharacter
	extends StaticEntity
{
	public static final String ASTRAL_SPIRIT = "Astral Spirit";

	public static final String SEAL_CLUBBER = "Seal Clubber";
	private static final List SEAL_CLUBBER_RANKS = new ArrayList();
	static
	{
		KoLCharacter.SEAL_CLUBBER_RANKS.add( "Lemming Trampler" );
		KoLCharacter.SEAL_CLUBBER_RANKS.add( "Tern Slapper" );
		KoLCharacter.SEAL_CLUBBER_RANKS.add( "Puffin Intimidator" );
		KoLCharacter.SEAL_CLUBBER_RANKS.add( "Ermine Thumper" );
		KoLCharacter.SEAL_CLUBBER_RANKS.add( "Penguin Frightener" );
		KoLCharacter.SEAL_CLUBBER_RANKS.add( "Malamute Basher" );
		KoLCharacter.SEAL_CLUBBER_RANKS.add( "Narwhal Pummeler" );
		KoLCharacter.SEAL_CLUBBER_RANKS.add( "Otter Crusher" );
		KoLCharacter.SEAL_CLUBBER_RANKS.add( "Caribou Smacker" );
		KoLCharacter.SEAL_CLUBBER_RANKS.add( "Moose Harasser" );
		KoLCharacter.SEAL_CLUBBER_RANKS.add( "Reindeer Threatener" );
		KoLCharacter.SEAL_CLUBBER_RANKS.add( "Ox Wrestler" );
		KoLCharacter.SEAL_CLUBBER_RANKS.add( "Walrus Bludgeoner" );
		KoLCharacter.SEAL_CLUBBER_RANKS.add( "Whale Boxer" );
		KoLCharacter.SEAL_CLUBBER_RANKS.add( "Seal Clubber" );
	}

	public static final String TURTLE_TAMER = "Turtle Tamer";
	private static final List TURTLE_TAMER_RANKS = new ArrayList();
	static
	{
		KoLCharacter.TURTLE_TAMER_RANKS.add( "Toad Coach" );
		KoLCharacter.TURTLE_TAMER_RANKS.add( "Skink Trainer" );
		KoLCharacter.TURTLE_TAMER_RANKS.add( "Frog Director" );
		KoLCharacter.TURTLE_TAMER_RANKS.add( "Gecko Supervisor" );
		KoLCharacter.TURTLE_TAMER_RANKS.add( "Newt Herder" );
		KoLCharacter.TURTLE_TAMER_RANKS.add( "Frog Boss" );
		KoLCharacter.TURTLE_TAMER_RANKS.add( "Iguana Driver" );
		KoLCharacter.TURTLE_TAMER_RANKS.add( "Salamander Subduer" );
		KoLCharacter.TURTLE_TAMER_RANKS.add( "Bullfrog Overseer" );
		KoLCharacter.TURTLE_TAMER_RANKS.add( "Rattlesnake Chief" );
		KoLCharacter.TURTLE_TAMER_RANKS.add( "Crocodile Lord" );
		KoLCharacter.TURTLE_TAMER_RANKS.add( "Cobra Commander" );
		KoLCharacter.TURTLE_TAMER_RANKS.add( "Alligator Subjugator" );
		KoLCharacter.TURTLE_TAMER_RANKS.add( "Asp Master" );
		KoLCharacter.TURTLE_TAMER_RANKS.add( "Turtle Tamer" );
	}

	public static final String PASTAMANCER = "Pastamancer";
	private static final List PASTAMANCER_RANKS = new ArrayList();
	static
	{
		KoLCharacter.PASTAMANCER_RANKS.add( "Dough Acolyte" );
		KoLCharacter.PASTAMANCER_RANKS.add( "Yeast Scholar" );
		KoLCharacter.PASTAMANCER_RANKS.add( "Noodle Neophyte" );
		KoLCharacter.PASTAMANCER_RANKS.add( "Starch Savant" );
		KoLCharacter.PASTAMANCER_RANKS.add( "Carbohydrate Cognoscenti" );
		KoLCharacter.PASTAMANCER_RANKS.add( "Spaghetti Sage" );
		KoLCharacter.PASTAMANCER_RANKS.add( "Macaroni Magician" );
		KoLCharacter.PASTAMANCER_RANKS.add( "Vermicelli Enchanter" );
		KoLCharacter.PASTAMANCER_RANKS.add( "Linguini Thaumaturge" );
		KoLCharacter.PASTAMANCER_RANKS.add( "Ravioli Sorcerer" );
		KoLCharacter.PASTAMANCER_RANKS.add( "Manicotti Magus" );
		KoLCharacter.PASTAMANCER_RANKS.add( "Spaghetti Spellbinder" );
		KoLCharacter.PASTAMANCER_RANKS.add( "Cannelloni Conjurer" );
		KoLCharacter.PASTAMANCER_RANKS.add( "Angel-Hair Archmage" );
		KoLCharacter.PASTAMANCER_RANKS.add( "Pastamancer" );
	}

	public static final String SAUCEROR = "Sauceror";
	private static final List SAUCEROR_RANKS = new ArrayList();
	static
	{
		KoLCharacter.SAUCEROR_RANKS.add( "Allspice Acolyte" );
		KoLCharacter.SAUCEROR_RANKS.add( "Cilantro Seer" );
		KoLCharacter.SAUCEROR_RANKS.add( "Parsley Enchanter" );
		KoLCharacter.SAUCEROR_RANKS.add( "Sage Sage" );
		KoLCharacter.SAUCEROR_RANKS.add( "Rosemary Diviner" );
		KoLCharacter.SAUCEROR_RANKS.add( "Thyme Wizard" );
		KoLCharacter.SAUCEROR_RANKS.add( "Tarragon Thaumaturge" );
		KoLCharacter.SAUCEROR_RANKS.add( "Oreganoccultist" );
		KoLCharacter.SAUCEROR_RANKS.add( "Basillusionist" );
		KoLCharacter.SAUCEROR_RANKS.add( "Coriander Conjurer" );
		KoLCharacter.SAUCEROR_RANKS.add( "Bay Leaf Brujo" );
		KoLCharacter.SAUCEROR_RANKS.add( "Sesame Soothsayer" );
		KoLCharacter.SAUCEROR_RANKS.add( "Marinara Mage" );
		KoLCharacter.SAUCEROR_RANKS.add( "Alfredo Archmage" );
		KoLCharacter.SAUCEROR_RANKS.add( "Sauceror" );
	}

	public static final String DISCO_BANDIT = "Disco Bandit";
	private static final List DISCO_BANDIT_RANKS = new ArrayList();
	static
	{
		KoLCharacter.DISCO_BANDIT_RANKS.add( "Funk Footpad" );
		KoLCharacter.DISCO_BANDIT_RANKS.add( "Rhythm Rogue" );
		KoLCharacter.DISCO_BANDIT_RANKS.add( "Chill Crook" );
		KoLCharacter.DISCO_BANDIT_RANKS.add( "Jiggy Grifter" );
		KoLCharacter.DISCO_BANDIT_RANKS.add( "Beat Snatcher" );
		KoLCharacter.DISCO_BANDIT_RANKS.add( "Sample Swindler" );
		KoLCharacter.DISCO_BANDIT_RANKS.add( "Move Buster" );
		KoLCharacter.DISCO_BANDIT_RANKS.add( "Jam Horker" );
		KoLCharacter.DISCO_BANDIT_RANKS.add( "Groove Filcher" );
		KoLCharacter.DISCO_BANDIT_RANKS.add( "Vibe Robber" );
		KoLCharacter.DISCO_BANDIT_RANKS.add( "Boogie Brigand" );
		KoLCharacter.DISCO_BANDIT_RANKS.add( "Flow Purloiner" );
		KoLCharacter.DISCO_BANDIT_RANKS.add( "Jive Pillager" );
		KoLCharacter.DISCO_BANDIT_RANKS.add( "Rhymer and Stealer" );
		KoLCharacter.DISCO_BANDIT_RANKS.add( "Disco Bandit" );
	}

	public static final String ACCORDION_THIEF = "Accordion Thief";
	private static final List ACCORDION_THIEF_RANKS = new ArrayList();
	static
	{
		KoLCharacter.ACCORDION_THIEF_RANKS.add( "Polka Criminal" );
		KoLCharacter.ACCORDION_THIEF_RANKS.add( "Mariachi Larcenist" );
		KoLCharacter.ACCORDION_THIEF_RANKS.add( "Zydeco Rogue" );
		KoLCharacter.ACCORDION_THIEF_RANKS.add( "Chord Horker" );
		KoLCharacter.ACCORDION_THIEF_RANKS.add( "Chromatic Crook" );
		KoLCharacter.ACCORDION_THIEF_RANKS.add( "Squeezebox Scoundrel" );
		KoLCharacter.ACCORDION_THIEF_RANKS.add( "Concertina Con Artist" );
		KoLCharacter.ACCORDION_THIEF_RANKS.add( "Button Box Burglar" );
		KoLCharacter.ACCORDION_THIEF_RANKS.add( "Hurdy-Gurdy Hooligan" );
		KoLCharacter.ACCORDION_THIEF_RANKS.add( "Sub-Sub-Apprentice Accordion Thief" );
		KoLCharacter.ACCORDION_THIEF_RANKS.add( "Sub-Apprentice Accordion Thief" );
		KoLCharacter.ACCORDION_THIEF_RANKS.add( "Pseudo-Apprentice Accordion Thief" );
		KoLCharacter.ACCORDION_THIEF_RANKS.add( "Hemi-Apprentice Accordion Thief" );
		KoLCharacter.ACCORDION_THIEF_RANKS.add( "Apprentice Accordion Thief" );
		KoLCharacter.ACCORDION_THIEF_RANKS.add( "Accordion Thief" );
	}

	private static final AdventureResult[] WANDS = new AdventureResult[]
   	{
		ItemPool.get( ItemPool.PINE_WAND, 1 ),
		ItemPool.get( ItemPool.EBONY_WAND, 1 ),
		ItemPool.get( ItemPool.HEXAGONAL_WAND, 1 ),
		ItemPool.get( ItemPool.ALUMINUM_WAND, 1 ),
		ItemPool.get( ItemPool.MARBLE_WAND, 1 )
	};

	public static final int MALE = -1;
	public static final int FEMALE = 1;

	private static String username = "";
	private static String avatar = "otherimages/discobandit_f.gif";
	private static int userId = 0;
	private static String playerId = "0";
	private static String classname = "";
	private static String classtype = "";
	private static int currentLevel = 1;
	private static long decrementPrime = 0;
	private static long incrementPrime = 25;
	private static int gender = 0;

	private static int currentHP, maximumHP, baseMaxHP;
	private static int currentMP, maximumMP, baseMaxMP;

	private static int[] adjustedStats = new int[ 3 ];
	private static long[] totalSubpoints = new long[ 3 ];
	private static long[] triggerSubpoints = new long[ 3 ];
	private static int[] triggerItem = new int[ 3 ];
	static { resetTriggers(); }

	public static final SortedListModel battleSkillNames = new SortedListModel();

	// Status pane data which is rendered whenever
	// the user issues a "status" type command.

	private static int pvpRank = 0;
	private static int attacksLeft = 0;
	private static int availableMeat = 0;
	private static int storageMeat = 0;
	private static int closetMeat = 0;
	private static int inebriety = 0;
	private static int adventuresLeft = 0;
	private static int daycount = 0;
	private static int currentRun = 0;
	private static boolean isFullnessIncreased = false;
	private static int holidayManaCostReduction = 0;

	// Status pane data which is rendered whenever
	// the user changes equipment, effects, and familiar

	private static Modifiers currentModifiers = new Modifiers();

	// Travel information

	private static boolean hasStore = true;
	private static boolean hasDisplayCase = true;
	private static boolean hasClan = true;

	// Campground information

	private static boolean hasOven = false;
	private static boolean hasRange = false;
	private static boolean hasChef = false;
	private static boolean hasShaker = false;
	private static boolean hasCocktailKit = false;
	private static boolean hasBartender = false;
	private static boolean hasSushiMat = false;

	private static boolean hasBookshelf = false;
	private static int telescopeUpgrades = 0;

	// Familiar data for reference

	public static final SortedListModel familiars = new SortedListModel();
	private static boolean isUsingStabBat = false;
	public static FamiliarData currentFamiliar = FamiliarData.NO_FAMILIAR;
	public static FamiliarData currentEnthroned = FamiliarData.NO_FAMILIAR;

	private static int arenaWins = 0;
	private static int stillsAvailable = 0;
	private static boolean tripleReagent = false;
	private static boolean guildStoreStateKnown = false;

	// Listener-driven container items

	private static final List listenerList = new ArrayList();
	private static boolean beanstalkArmed = false;
	private static KoLAdventure selectedLocation;

	// Ascension-related variables

	private static boolean isHardcore = false;
	private static boolean inRonin = true;
	private static boolean skillsRecalled = false;

	private static int ascensions = 0;
	private static String ascensionSign = "None";
	private static int ascensionSignType = KoLConstants.NONE;
	private static int consumptionRestriction = AscensionSnapshot.NOPATH;
	private static int mindControlLevel = 0;

	private static int autoAttackAction = 0;
	private static String autosellMode = "";
	private static boolean unequipFamiliar = false;

	public static final Object[][] PASTA_GUARDIANS =
	{
		// Entity type
		// Item that summons it
		// Pattern to find name when first summoned
		// Pattern to find name when summoned in battle
		// .gif file name when in battle
		// (Haiku summoning)
		{
			"Undead Elbow Macaroni",
			new Integer( ItemPool.MACARONI_FRAGMENTS ),
			// "<name>. My name is <name>."
			Pattern.compile( "My name is ([^.]*)\\." ),
			// You focus your thoughts and call out to <name>. He
			// claws his way up from beneath the ground at your
			// feet.
			Pattern.compile( "You focus your thoughts and call out to (.*?)\\." ),
			"macaroni.gif",
		},
		{
			"Spice Ghost",
			new Integer( ItemPool.SCINTILLATING_POWDER ),
			// My name is <name>, and I am in your debt.
			Pattern.compile( "My name is ([^,]*), and I am in your debt\\." ),
			// You conjure up a swirling cloud of spicy dried
			// couscous, and there is a crackle of psychokinetic
			// energy as <name> possesses it.
			Pattern.compile( "crackle of psychokinetic energy as (.*?) possesses it\\." ),
			"spiceghost.gif",
			// You enlist some aid,
			// from a powerful ally.
			// Powerful tasty.
		},
		{
			"Angel Hair Wisp",
			new Integer( ItemPool.SHIMMERING_TENDRILS ),
			// "You must call me <name>. You must give me form. I
			// must live."
			Pattern.compile( "You must call me ([^.]*])\\." ),
			// You concentrate, and summon a mass of writhing angel
			// hair. A chill perm eates the air as <name>'s spirit
			// enters it. "I live..."
			Pattern.compile( "A chill perm ?eates the air as (.*?)'s spirit enters it\\." ),
			"angelwisp.gif",
		},

		{
			"Lasagmbie",
			new Integer( ItemPool.PARANORMAL_RICOTTA ),
			// Okay. See you on the other side, <name>.
			Pattern.compile( "See you on the other side, (.*?)\\." ),
			// You conjure up a good-sized sheet of lasagna, and
			// there is a wet thud as <name>'s spirit lands in it.
			Pattern.compile( "a wet thud as ([^']*)'s spirit lands in it" ),
			"lasagmbie.gif",
		},
		{
			"Vermincelli",
			new Integer( ItemPool.CRUMBLING_RAT_SKULL ),
			Pattern.compile( "I think little (.*?) will be the best helper\\." ),
			// You summon a tangled mass of noodles. There is a
			// rustling sound as <name> chews his way into the
			// world to occupy his new body.
			Pattern.compile( "rustling sound as (.*?) chews his way into the world" ),
			"vermincelli.gif",
		},
		{
			"Boba Fettucini",
			new Integer( ItemPool.TWITCHING_TRIGGER_FINGER ),
			// You decide to name it <name>.
			Pattern.compile( "You decide to name it ([^.]*)\\." ),
			// <i>pew pew pew!&quot;</i> <name> shouts excitedly,
			// drawing a laser pistol from some spiritual dimension
			// of necessity.
			Pattern.compile( "</i> (.*) shouts excitedly" ),
			"bobafett.gif",
		},
		{
			"Penne Dreadful",
			new Integer( ItemPool.WINE_SOAKED_BONE_CHIPS ),
			// "All right, palookah," the private eye says, opening
			// his mouth for the first time, "the name's
			// <name>. I'm a gumshoe. You know, a shamus, a
			// flatfoot, a sleuth.
			Pattern.compile( "the name's ([^.]*)\\." ),
			// You calm your mind, and imagine a skeletal assembly
			// of penne. A lone saxophone breaks the night's
			// stillness as it appears and <name> possesses it.
			Pattern.compile( "it appears and (.*?) possesses it" ),
			"pennedreadful.gif",
		},
		{
			"Vampieroghi",
			new Integer( ItemPool.VAMPIRE_GLITTER ),
			// My name is written in blood across the history of
			// time . . . but you can call me <name>.
			Pattern.compile( "but you can call me ([^.]*)\\." ),
			// You conjure a pieroghi, and there is a hiss as it
			// becomes inflated with <name>'s presence.
			Pattern.compile( "inflated with ([^']*)'s presence" ),
			"vampieroghi.gif",
		},
		{
			"Bow Tie Bat",
			new Integer( ItemPool.SMOKING_TALON ),
			// Ugh. I'll take that guano as a yes? You'll need a
			// name. Let's call you..." You glance around, hoping
			// for some inspiration. "How about... <name>."
			Pattern.compile( "How about... ([^.]*)\\." ),
			// You call out to <name> with your mind, summoning his
			// spirit even as you create a body for him. He lets
			// loose an ear-splitting screech.
			Pattern.compile( "You call out to (.*?) with your mind" ),
			"bowtiebat.gif",
		},
		{
			"Spaghetti Elemental",
			new Integer( ItemPool.DECODED_CULT_DOCUMENTS ),
			// "I guess you need a name, huh?" you reply. "I'll
			// call you... um... SshoKodo. That'll do."
			Pattern.compile( "I'll call you... *um... *([^.]*). * That'll do." ),
			// You close your eyes and reach out across the border
			// between worlds, to the Elemental Plane of
			// Spaghetti. Soon you feel a familiar presence, and
			// pull SshoKodo into the material world.
			Pattern.compile( "and pull (.*?) into the material world\\." ),
			new String[] {
				"spagelem1.gif",
				"spagelem2.gif",
			},
		},
	};

	public static final int findGuardianByImage( final String responseText )
	{
		for ( int i = 0; i < KoLCharacter.PASTA_GUARDIANS.length; ++ i )
		{
			Object [] guardian = KoLCharacter.PASTA_GUARDIANS[i];
			Object images = guardian[4];
			if ( images instanceof String )
			{
				String gif = (String) images;
				if ( responseText.indexOf( gif ) != -1 )
				{
					return i;
				}
			}
			if ( images instanceof String[] )
			{
				String [] array = (String []) images;
				for ( int j = 0; j < array.length; ++j )
				{
					String gif = array[ j ];
					if ( responseText.indexOf( gif ) != -1 )
					{
						return i;
					}
				}
			}
		}

		return -1;
	}

	/**
	 * Constructs a new <code>KoLCharacter</code> with the given name. All
	 * fields are initialized to their default values (nothing), and it is
	 * the responsibility of other methods to initialize the fields with
	 * their real values.
	 *
	 * @param newUsername The name of the character this <code>KoLCharacter</code> represents
	 */

	public static final void reset( final String newUserName )
	{
		if ( newUserName.equals( KoLCharacter.username ) )
		{
			return;
		}

		KoLCharacter.username = newUserName;
		Preferences.reset( KoLCharacter.username );
		KoLCharacter.reset();
	}

	public static final void reset()
	{
		KoLCharacter.classname = "";
		KoLCharacter.classtype = "";

		KoLCharacter.gender = 0;
		KoLCharacter.currentLevel = 1;
		KoLCharacter.decrementPrime = 0L;
		KoLCharacter.incrementPrime = 25L;

		KoLCharacter.pvpRank = 0;
		KoLCharacter.attacksLeft = 0;
		KoLCharacter.adjustedStats = new int[ 3 ];
		KoLCharacter.totalSubpoints = new long[ 3 ];
		KoLCharacter.resetTriggers();

		KoLCharacter.currentModifiers.reset();

		KoLConstants.inventory.clear();
		KoLConstants.closet.clear();
		KoLConstants.storage.clear();
		KoLConstants.freepulls.clear();
		KoLConstants.collection.clear();
		KoLConstants.pulverizeQueue.clear();

		KoLConstants.usableSkills.clear();
		KoLConstants.summoningSkills.clear();
		KoLConstants.remedySkills.clear();
		KoLConstants.selfOnlySkills.clear();
		KoLConstants.buffSkills.clear();
		KoLConstants.availableSkills.clear();
		KoLConstants.availableSkillsMap.clear();
		KoLConstants.combatSkills.clear();

		// All characters get the option to
		// attack something.

		KoLCharacter.battleSkillNames.clear();
		KoLCharacter.battleSkillNames.add( "attack with weapon" );
		KoLCharacter.battleSkillNames.add( "custom combat script" );
		KoLCharacter.battleSkillNames.add( "delevel and plink" );

		FightRequest.addItemActionsWithNoCost();

		KoLCharacter.battleSkillNames.add( "try to run away" );

		KoLCharacter.isHardcore = false;
		KoLCharacter.inRonin = true;
		KoLCharacter.inebriety = 0;
		KoLCharacter.skillsRecalled = false;
		KoLCharacter.hasStore = false;
		KoLCharacter.hasDisplayCase = false;
		KoLCharacter.hasClan = false;

		KoLCharacter.hasOven = false;
		KoLCharacter.hasRange = false;
		KoLCharacter.hasChef = false;
		KoLCharacter.hasShaker = false;
		KoLCharacter.hasCocktailKit = false;
		KoLCharacter.hasBartender = false;
		KoLCharacter.hasSushiMat = false;

		KoLCharacter.hasBookshelf = false;
		KoLCharacter.telescopeUpgrades = 0;

		KoLCharacter.familiars.clear();
		KoLCharacter.familiars.add( FamiliarData.NO_FAMILIAR );

		KoLCharacter.arenaWins = 0;
		KoLCharacter.isUsingStabBat = false;

		KoLCharacter.stillsAvailable = -1;
		KoLCharacter.tripleReagent = false;
		KoLCharacter.guildStoreStateKnown = false;
		KoLCharacter.beanstalkArmed = false;

		KoLCharacter.ascensions = 0;
		KoLCharacter.ascensionSign = "None";
		KoLCharacter.ascensionSignType = KoLConstants.NONE;

		KoLCharacter.mindControlLevel = 0;

		KoLCharacter.autosellMode = "";
		KoLCharacter.unequipFamiliar = false;

		// Clear some of the standard lists so they don't
		// carry over from player to player.

		KoLConstants.conditions.clear();
		EventManager.clearEventHistory();
		KoLConstants.recentEffects.clear();
		KoLConstants.activeEffects.clear();

		// Don't reuse NPC food & drink from a previous login
		ChezSnooteeRequest.reset();
		MicroBreweryRequest.reset();
		HellKitchenRequest.reset();

		DwarfFactoryRequest.reset();
		MoneyMakingGameManager.reset();
		VolcanoMazeManager.reset();
		WumpusManager.reset();
		ClanManager.clearCache();
		DisplayCaseManager.clearCache();
		StoreManager.clearCache();
		InventoryManager.resetInventory();
		EquipmentManager.resetEquipment();
		EquipmentManager.resetOutfits();
		ConcoctionDatabase.refreshConcoctions();
		ItemDatabase.calculateAdventureRanges();

		int battleIndex = KoLCharacter.battleSkillNames.indexOf( Preferences.getString( "battleAction" ) );
		KoLCharacter.battleSkillNames.setSelectedIndex( battleIndex == -1 ? 0 : battleIndex );

		Modifiers.overrideModifier( "_userMods", Preferences.getString( "_userMods" ) );
	}

	public static final void resetPerAscensionData()
	{
		// This is called after we have read the Charsheet and know how
		// many ascensions the character has completed.

		// Update all data which changes each ascension

		VioletFogManager.reset();
		LouvreManager.reset();
		ItemDatabase.getDustyBottles();
		KoLCharacter.ensureUpdatedDwarfFactory();
		KoLCharacter.ensureUpdatedGuyMadeOfBees();
		KoLCharacter.ensureUpdatedPirateInsults();
		KoLCharacter.ensureUpdatedPotionEffects();
		KoLCharacter.ensureUpdatedSemirareCounter();
		KoLCharacter.ensureUpdatedSkatePark();
		KoLCharacter.ensureUpdatedSphereEffects();
		KoLCharacter.ensureUpdatedCellar();
	}

	public static final void setHoliday( final String holiday )
	{
		KoLCharacter.isFullnessIncreased = holiday.equals( "Feast of Boris" );
		KoLCharacter.holidayManaCostReduction = holiday.equals( "Festival of Jarlsberg" ) ? 3 : 0;
	}

	public static final int getFullness()
	{
		return Preferences.getInteger( "currentFullness" );
	}

	public static final int getFullnessLimit()
	{
		if ( !KoLCharacter.canEat() )
		{
			return 0;
		}

		int baseFullness = 15;

		if ( KoLCharacter.inBadMoon() )
		{
			if ( KoLCharacter.hasSkill( "Pride" ) )
			{
				baseFullness -= 1;
			}
			if ( KoLCharacter.hasSkill( "Gluttony" ) )
			{
				baseFullness += 2;
			}
		}
		else if ( KoLCharacter.hasSkill( "Stomach of Steel" ) )
		{
			baseFullness += 5;
		}

		return KoLCharacter.isFullnessIncreased ? baseFullness + 15 : baseFullness;
	}

	public static final void setInebriety( final int inebriety )
	{
		KoLCharacter.inebriety = inebriety;
	}

	public static final int getInebriety()
	{
		return KoLCharacter.inebriety;
	}

	public static final int getInebrietyLimit()
	{
		return KoLCharacter.hasSkill( "Liver of Steel" ) ? 19 : KoLCharacter.canDrink() ? 14 : 0;
	}

	public static final boolean isFallingDown()
	{
		return KoLCharacter.getInebriety() > KoLCharacter.getInebrietyLimit();
	}

	public static final int getSpleenUse()
	{
		return Preferences.getInteger( "currentSpleenUse" );
	}

	public static final int getSpleenLimit()
	{
		return KoLCharacter.hasSkill( "Spleen of Steel" ) ? 20 : 15;
	}

	/**
	 * Accessor method to retrieve the name of this character.
	 *
	 * @return The name of this character
	 */

	public static final String getUserName()
	{
		return KoLCharacter.username;
	}

	public static final String baseUserName()
	{
		return Preferences.baseUserName( KoLCharacter.username );
	}

	/**
	 * Accessor method to set the user Id associated with this character.
	 *
	 * @param userId The user Id associated with this character
	 */

	public static final void setUserId( final int userId )
	{
		KoLCharacter.userId = userId;
		KoLCharacter.playerId = String.valueOf( userId );
	}

	/**
	 * Accessor method to retrieve the user Id associated with this character.
	 *
	 * @return The user Id associated with this character
	 */

	public static final String getPlayerId()
	{
		return KoLCharacter.playerId;
	}

	/**
	 * Accessor method to retrieve the user Id associated with this character.
	 *
	 * @return The user Id associated with this character
	 */

	public static final int getUserId()
	{
		return KoLCharacter.userId;
	}

	/**
	 * Accessor method to get the avatar associated with this character.
	 *
	 * @param avatar The avatar for this character
	 */

	public static final void setAvatar( final String avatar )
	{
		KoLCharacter.avatar = avatar;
		if ( avatar.endsWith( "_f.gif" ) )
		{
			KoLCharacter.setGender( KoLCharacter.FEMALE );
		}
		// Unfortunately, lack of '_f' in the avatar doesn't necessarily
		// indicate a male character - it could be a custom avatar, or a
		// special avatar such as Birdform that's unisex.
	}

	/**
	 * Accessor method to get the avatar associated with this character.
	 *
	 * @return The avatar for this character
	 */

	public static final String getAvatar()
	{
		FileUtilities.downloadImage( "http://images.kingdomofloathing.com/" + KoLCharacter.avatar );
		return KoLCharacter.avatar;
	}

	public static final void setGender( final int gender )
	{
		KoLCharacter.gender = gender;
	}

	public static final int getGender()
	{
		// We can only ask for the gender if we are logged in
		if ( KoLCharacter.gender == 0 &&
		     !GenericRequest.passwordHash.equals( "" ) )
		{	// Can't tell?	Look at their vinyl boots!
			GenericRequest req = new GenericRequest(
				"desc_item.php?whichitem=809051828" );
			RequestThread.postRequest( req );
			KoLCharacter.setGender( req.responseText != null &&
				req.responseText.indexOf( "+15%" ) != -1 ?
					KoLCharacter.FEMALE : KoLCharacter.MALE );
		}
		return KoLCharacter.gender;
	}

	/**
	 * Accessor method to retrieve the index of the prime stat.
	 *
	 * @return The index of the prime stat
	 */

	public static final int getPrimeIndex()
	{
		return KoLCharacter.classtype.startsWith( "Se" ) || KoLCharacter.classtype.startsWith( "Tu" ) ? 0 : KoLCharacter.classtype.startsWith( "Sa" ) || KoLCharacter.classtype.startsWith( "Pa" ) ? 1 : 2;
	}

	/**
	 * Accessor method to retrieve the level of this character.
	 *
	 * @return The level of this character
	 */

	public static final int getLevel()
	{
		long totalPrime = KoLCharacter.getTotalPrime();

		if ( totalPrime < KoLCharacter.decrementPrime || totalPrime >= KoLCharacter.incrementPrime )
		{
			KoLCharacter.currentLevel = KoLCharacter.calculateSubpointLevels( totalPrime );
			KoLCharacter.decrementPrime = KoLCharacter.calculateLastLevel();
			KoLCharacter.incrementPrime = KoLCharacter.calculateNextLevel();
			if ( KoLCharacter.incrementPrime < 0 )
			{	// this will overflow at level 216
				KoLCharacter.incrementPrime = Integer.MAX_VALUE;
			}

			HPRestoreItemList.updateHealthRestored();
			MPRestoreItemList.updateManaRestored();
		}

		return KoLCharacter.currentLevel;
	}

	public static final int getPvpRank()
	{
		return KoLCharacter.pvpRank;
	}

	public static final void setPvpRank( final int pvpRank )
	{
		KoLCharacter.pvpRank = pvpRank;
	}

	public static final int getAttacksLeft()
	{
		return KoLCharacter.attacksLeft;
	}

	public static final void setAttacksLeft( final int attacksLeft )
	{
		KoLCharacter.attacksLeft = attacksLeft;
	}

	/**
	 * Accessor method to set the character's class.
	 *
	 * @param classname The name of the character's class
	 */

	public static final void setClassName( final String classname )
	{
		KoLCharacter.classname = classname;
		KoLCharacter.classtype = null;
		KoLCharacter.getClassType();
		KoLCharacter.tripleReagent = KoLCharacter.classtype.equals( KoLCharacter.SAUCEROR );

		if ( KoLCharacter.classtype.equals( KoLCharacter.ASTRAL_SPIRIT ) )
		{
			return;
		}

		// If we have an actual class, we have a mainstat.
		// Reset concoction mainstat gains to reflect this.
		ConcoctionDatabase.resetConcoctionStatGains();

		// Allow or disallow special fight actions
		FightRequest.initialize();
	}

	public static final int getReagentPotionDuration()
	{
		return 5 +
		       ( KoLCharacter.hasSkill( "Impetuous Sauciness" ) ? 5 : 0 ) +
		       ( KoLCharacter.classtype.equals(KoLCharacter.SAUCEROR) ? 5 : 0 );

	}

	/**
	 * Accessor method to retrieve the name of the character's class.
	 *
	 * @return The name of the character's class
	 */

	public static final String getClassName()
	{
		return KoLCharacter.classname;
	}

	/**
	 * Accessor method to retrieve the type of the character's class.
	 *
	 * @return The type of the character's class
	 */

	public static final String getClassType()
	{
		if ( KoLCharacter.classtype == null )
		{
			KoLCharacter.classtype = KoLCharacter.getClassType( KoLCharacter.classname );
		}

		return KoLCharacter.classtype;
	}

	/**
	 * Accessor method to retrieve the type of the character's class.
	 *
	 * @return The type of the character's class
	 */

	public static final String getClassType( final String classname )
	{
		return KoLCharacter.SEAL_CLUBBER_RANKS.contains( classname ) ? KoLCharacter.SEAL_CLUBBER :
			KoLCharacter.TURTLE_TAMER_RANKS.contains( classname ) ? KoLCharacter.TURTLE_TAMER :
			KoLCharacter.PASTAMANCER_RANKS.contains( classname ) ? KoLCharacter.PASTAMANCER :
			KoLCharacter.SAUCEROR_RANKS.contains( classname ) ? KoLCharacter.SAUCEROR :
			KoLCharacter.DISCO_BANDIT_RANKS.contains( classname ) ? KoLCharacter.DISCO_BANDIT :
			KoLCharacter.ACCORDION_THIEF_RANKS.contains( classname ) ? KoLCharacter.ACCORDION_THIEF :
			KoLCharacter.ASTRAL_SPIRIT;
	}

	public static final boolean isMuscleClass()
	{
		return KoLCharacter.classtype.equals( KoLCharacter.SEAL_CLUBBER ) || KoLCharacter.classtype.equals( KoLCharacter.TURTLE_TAMER );
	}

	public static final boolean isMysticalityClass()
	{
		return KoLCharacter.classtype.equals( KoLCharacter.PASTAMANCER ) || KoLCharacter.classtype.equals( KoLCharacter.SAUCEROR );
	}

	public static final boolean isMoxieClass()
	{
		return KoLCharacter.classtype.equals( KoLCharacter.DISCO_BANDIT ) || KoLCharacter.classtype.equals( KoLCharacter.ACCORDION_THIEF );
	}

	public static final int mainStat()
	{
		return KoLCharacter.isMuscleClass() ? KoLConstants.MUSCLE :
			KoLCharacter.isMysticalityClass() ? KoLConstants.MYSTICALITY :
			KoLCharacter.isMoxieClass() ? KoLConstants.MOXIE :
			KoLConstants.NONE;
	}

	/**
	 * Accessor method to set the character's current health state.
	 *
	 * @param currentHP The character's current HP value
	 * @param maximumHP The character's maximum HP value
	 * @param baseMaxHP The base value for the character's maximum HP
	 */

	public static final void setHP( final int currentHP, final int maximumHP, final int baseMaxHP )
	{
		KoLCharacter.currentHP = currentHP < 0 ? 0 : currentHP > maximumHP ? maximumHP : currentHP;
		KoLCharacter.maximumHP = maximumHP;
		KoLCharacter.baseMaxHP = baseMaxHP;

		KoLCharacter.updateStatus();
	}

	/**
	 * Accessor method to retrieve the character's current HP.
	 *
	 * @return The character's current HP
	 */

	public static final int getCurrentHP()
	{
		return KoLCharacter.currentHP;
	}

	/**
	 * Accessor method to retrieve the character's maximum HP.
	 *
	 * @return The character's maximum HP
	 */

	public static final int getMaximumHP()
	{
		return KoLCharacter.maximumHP;
	}

	/**
	 * Accessor method to retrieve the base value for the character's maximum HP.
	 *
	 * @return The base value for the character's maximum HP
	 */

	public static final int getBaseMaxHP()
	{
		return KoLCharacter.baseMaxHP;
	}

	/**
	 * Accessor method to set the character's current mana limits.
	 *
	 * @param currentMP The character's current MP value
	 * @param maximumMP The character's maximum MP value
	 * @param baseMaxMP The base value for the character's maximum MP
	 */

	public static final void setMP( final int currentMP, final int maximumMP, final int baseMaxMP )
	{
		KoLCharacter.currentMP = currentMP < 0 ? 0 : currentMP > maximumMP ? maximumMP : currentMP;
		KoLCharacter.maximumMP = maximumMP;
		KoLCharacter.baseMaxMP = baseMaxMP;

		KoLCharacter.updateStatus();
	}

	/**
	 * Accessor method to retrieve the character's current MP.
	 *
	 * @return The character's current MP
	 */

	public static final int getCurrentMP()
	{
		return KoLCharacter.currentMP;
	}

	/**
	 * Accessor method to retrieve the character's maximum MP.
	 *
	 * @return The character's maximum MP
	 */

	public static final int getMaximumMP()
	{
		return KoLCharacter.maximumMP;
	}

	/**
	 * Accessor method to retrieve the base value for the character's maximum MP.
	 *
	 * @return The base value for the character's maximum MP
	 */

	public static final int getBaseMaxMP()
	{
		return KoLCharacter.baseMaxMP;
	}

	/**
	 * Accessor method to retrieve the amount of meat in Hagnk's storage.
	 *
	 * @return The amount of meat in storage.
	 */

	public static final int getStorageMeat()
	{
		return KoLCharacter.storageMeat;
	}

	public static final void setStorageMeat( final int storageMeat )
	{
		KoLCharacter.storageMeat = storageMeat;
	}

	public static final void addStorageMeat( final int meat )
	{
		KoLCharacter.storageMeat += meat;
	}

	/**
	 * Accessor method to retrieve the amount of meat in the character's closet.
	 *
	 * @return The amount of meat in the character's closet.
	 */

	public static final int getClosetMeat()
	{
		return KoLCharacter.closetMeat;
	}

	public static final void setClosetMeat( final int closetMeat )
	{
		KoLCharacter.closetMeat = closetMeat;
	}

	/**
	 * Accessor method to set the character's current available meat for spending (IE: meat that isn't currently in the
	 * character's closet).
	 *
	 * @param availableMeat The character's available meat for spending
	 */

	public static final void setAvailableMeat( final int availableMeat )
	{
		if ( KoLCharacter.availableMeat != availableMeat )
		{
			KoLCharacter.availableMeat = availableMeat;
		}
	}

	/**
	 * Accessor method to retrieve the character's current available meat for spending (IE: meat that isn't currently in
	 * the character's closet).
	 *
	 * @return The character's available meat for spending
	 */

	public static final int getAvailableMeat()
	{
		return KoLCharacter.availableMeat;
	}

	/**
	 * Sets the character's current stat values. Each parameter in the list comes in pairs: the adjusted value (based on
	 * equipment and spell effects) and the total number of subpoints acquired through adventuring for that statistic.
	 * This is preferred over the character's current base and/or distance from base as it allows for more accurate
	 * reporting of statistic gains and losses, as statistic losses are not reported by KoL.
	 *
	 * @param adjustedMuscle The adjusted value for the character's muscle
	 * @param totalMuscle The total number of muscle subpoints acquired thus far
	 * @param adjustedMysticality The adjusted value for the character's mysticality
	 * @param totalMysticality The total number of mysticality subpoints acquired thus far
	 * @param adjustedMoxie The adjusted value for the character's moxie
	 * @param totalMoxie The total number of moxie subpoints acquired thus far
	 */

	public static final void setStatPoints( final int adjustedMuscle, final long totalMuscle,
		final int adjustedMysticality, final long totalMysticality, final int adjustedMoxie, final long totalMoxie )
	{
		KoLCharacter.adjustedStats[ 0 ] = adjustedMuscle;
		KoLCharacter.adjustedStats[ 1 ] = adjustedMysticality;
		KoLCharacter.adjustedStats[ 2 ] = adjustedMoxie;

		KoLCharacter.totalSubpoints[ 0 ] = totalMuscle;
		KoLCharacter.totalSubpoints[ 1 ] = totalMysticality;
		KoLCharacter.totalSubpoints[ 2 ] = totalMoxie;

		if ( totalMuscle >= KoLCharacter.triggerSubpoints[ 0 ] )
		{
			KoLCharacter.handleTrigger( KoLCharacter.triggerItem[ 0 ] );
		}

		if ( totalMysticality >= KoLCharacter.triggerSubpoints[ 1 ] )
		{
			KoLCharacter.handleTrigger( KoLCharacter.triggerItem[ 1 ] );
		}

		if ( totalMoxie >= KoLCharacter.triggerSubpoints[ 2 ] )
		{
			KoLCharacter.handleTrigger( KoLCharacter.triggerItem[ 2 ] );
		}
	}

	public static final void resetTriggers()
	{
		KoLCharacter.triggerSubpoints[ 0 ] = Long.MAX_VALUE;
		KoLCharacter.triggerSubpoints[ 1 ] = Long.MAX_VALUE;
		KoLCharacter.triggerSubpoints[ 2 ] = Long.MAX_VALUE;
	}

	public static final void handleTrigger( int itemId )
	{
		KoLmafia.updateDisplay( "You can now equip a " + ItemDatabase.getItemName( itemId )
			+ " (and possibly other things)." );
		EquipmentManager.updateEquipmentLists();
		PreferenceListenerRegistry.firePreferenceChanged( "(equippable)" );
	}

	public static final int getTriggerItem( int stat )
	{
		return KoLCharacter.triggerItem[ stat ];
	}

	public static final int getTriggerPoints( int stat )
	{
		return KoLCharacter.calculateBasePoints(
			KoLCharacter.triggerSubpoints[ stat ] );
	}

	/**
	 * Utility method for calculating how many subpoints are need to reach
	 * a specified full point
	 *
	 * @param basePoints The desired point
	 * @return The calculated subpoints
	 */

	public static final long calculatePointSubpoints( final int basePoints )
	{
		return basePoints * (long) basePoints;
	}

	/**
	 * Utility method for calculating how many actual points are associated
	 * with the given number of subpoints.
	 *
	 * @param subpoints The total number of subpoints accumulated
	 * @return The base points associated with the subpoint value
	 */

	public static final int calculateBasePoints( final long subpoints )
	{
		return (int)Math.sqrt( subpoints );
	}

	/**
	 * Utility method for calculating how many points are need to reach
	 * a specified character level.
	 *
	 * @param level The character level
	 * @return The calculated points
	 */

	public static final int calculateLevelPoints( final int level )
	{
		return ( level == 1 ) ? 0 : ( level - 1 ) * ( level - 1 ) + 4;
	}


	/**
	 * Utility method for calculating how many subpoints are need to reach
	 * a specified character level.
	 *
	 * @param level The character level
	 * @return The calculated subpoints
	 */

	public static final long calculateLevelSubpoints( final int level )
	{
		return KoLCharacter.calculatePointSubpoints( KoLCharacter.calculateLevelPoints( level ) );
	}

	/**
	 * Utility method for calculating what character level is associated
	 * with the given number of points.
	 *
	 * @param points The total number of points accumulated
	 * @return The calculated level
	 */

	public static final int calculatePointLevels( final int points )
	{
		return (int)Math.sqrt( Math.max( points - 4, 0 ) ) + 1;
	}

	/**
	 * Utility method for calculating what character level is associated
	 * with the given number of subpoints.
	 *
	 * @param subpoints The total number of subpoints accumulated
	 * @return The calculated level
	 */

	public static final int calculateSubpointLevels( final long subpoints )
	{
		return KoLCharacter.calculatePointLevels( KoLCharacter.calculateBasePoints( subpoints ) );
	}

	/**
	 * Utility method for calculating how many subpoints have been
	 * accumulated thus far, given the current base point value of the
	 * statistic and how many have been accumulate since the last gain.
	 *
	 * @param baseValue The current base point value
	 * @param sinceLastBase Number of subpoints accumulate since the last base point gain
	 * @return The total number of subpoints acquired since creation
	 */

	public static final long calculateSubpoints( final int baseValue, final int sinceLastBase )
	{
		return KoLCharacter.calculatePointSubpoints( baseValue ) + sinceLastBase;
	}

	/**
	 * Returns the total number of subpoints to the current level.
	 *
	 * @return The total subpoints to the current level
	 */

	public static final long calculateLastLevel()
	{
		return KoLCharacter.calculateLevelSubpoints( KoLCharacter.currentLevel );
	}

	/**
	 * Returns the total number of subpoints to the next level.
	 *
	 * @return The total subpoints to the next level
	 */

	public static final long calculateNextLevel()
	{
		return KoLCharacter.calculateLevelSubpoints( KoLCharacter.currentLevel + 1 );
	}

	/**
	 * Returns the total number of subpoints acquired in the prime stat.
	 *
	 * @return The total subpoints in the prime stat
	 */

	public static final long getTotalPrime()
	{
		return KoLCharacter.totalSubpoints[ KoLCharacter.getPrimeIndex() ];
	}

	/**
	 * Utility method to calculate the "till next point" value, given the total number of subpoints accumulated.
	 */

	private static final int calculateTillNextPoint( final long subpoints )
	{
		return (int) (KoLCharacter.calculatePointSubpoints( KoLCharacter.calculateBasePoints( subpoints ) + 1 ) - subpoints);
	}

	/**
	 * Accessor method to retrieve the character's base value for muscle.
	 *
	 * @return The character's base value for muscle
	 */

	public static final int getBaseMuscle()
	{
		return KoLCharacter.calculateBasePoints( KoLCharacter.totalSubpoints[ 0 ] );
	}

	/**
	 * Accessor method to retrieve the total subpoints accumulted so far in muscle.
	 *
	 * @return The total muscle subpoints so far
	 */

	public static final long getTotalMuscle()
	{
		return KoLCharacter.totalSubpoints[ 0 ];
	}

	public static final void incrementTotalMuscle( int increment )
	{
		KoLCharacter.totalSubpoints[ 0 ] += increment;
		if ( KoLCharacter.totalSubpoints[ 0 ] >= KoLCharacter.triggerSubpoints[ 0 ] )
		{
			KoLCharacter.handleTrigger( KoLCharacter.triggerItem[ 0 ] );
		}
	}

	public static final boolean muscleTrigger( int basepoints, int itemId )
	{
		long points = calculatePointSubpoints( basepoints );
		if ( points < KoLCharacter.triggerSubpoints[ 0 ] )
		{
			KoLCharacter.triggerSubpoints[ 0 ] = points;
			KoLCharacter.triggerItem[ 0 ] = itemId;
		}
		return false;	// for the convenience of the caller
	}

	/**
	 * Accessor method to retrieve the number of subpoints required before the character gains another full point of
	 * muscle.
	 */

	public static final int getMuscleTNP()
	{
		return KoLCharacter.calculateTillNextPoint( KoLCharacter.totalSubpoints[ 0 ] );
	}

	/**
	 * Accessor method to retrieve the character's adjusted value for muscle.
	 *
	 * @return The character's adjusted value for muscle
	 */

	public static final int getAdjustedMuscle()
	{
		return KoLCharacter.adjustedStats[ 0 ];
	}

	/**
	 * Accessor method to retrieve the character's base value for mysticality.
	 *
	 * @return The character's base value for muscle
	 */

	public static final int getBaseMysticality()
	{
		return KoLCharacter.calculateBasePoints( KoLCharacter.totalSubpoints[ 1 ] );
	}

	/**
	 * Accessor method to retrieve the total subpoints accumulted so far in mysticality.
	 *
	 * @return The total mysticality subpoints so far
	 */

	public static final long getTotalMysticality()
	{
		return KoLCharacter.totalSubpoints[ 1 ];
	}

	public static final void incrementTotalMysticality( int increment )
	{
		KoLCharacter.totalSubpoints[ 1 ] += increment;
		if ( KoLCharacter.totalSubpoints[ 1 ] >= KoLCharacter.triggerSubpoints[ 1 ] )
		{
			KoLCharacter.handleTrigger( KoLCharacter.triggerItem[ 1 ] );
		}
	}

	public static final boolean mysticalityTrigger( int basepoints, int itemId )
	{
		long points = calculatePointSubpoints( basepoints );
		if ( points < KoLCharacter.triggerSubpoints[ 1 ] )
		{
			KoLCharacter.triggerSubpoints[ 1 ] = points;
			KoLCharacter.triggerItem[ 1 ] = itemId;
		}
		return false;	// for the convenience of the caller
	}

	/**
	 * Accessor method to retrieve the number of subpoints required before the character gains another full point of
	 * mysticality.
	 */

	public static final int getMysticalityTNP()
	{
		return KoLCharacter.calculateTillNextPoint( KoLCharacter.totalSubpoints[ 1 ] );
	}

	/**
	 * Accessor method to retrieve the character's adjusted value for mysticality.
	 *
	 * @return The character's adjusted value for mysticality
	 */

	public static final int getAdjustedMysticality()
	{
		return KoLCharacter.adjustedStats[ 1 ];
	}

	/**
	 * Accessor method to retrieve the character's base value for moxie.
	 *
	 * @return The character's base value for moxie
	 */

	public static final int getBaseMoxie()
	{
		return KoLCharacter.calculateBasePoints( KoLCharacter.totalSubpoints[ 2 ] );
	}

	/**
	 * Accessor method to retrieve the total subpoints accumulted so far in moxie.
	 *
	 * @return The total moxie subpoints so far
	 */

	public static final long getTotalMoxie()
	{
		return KoLCharacter.totalSubpoints[ 2 ];
	}

	public static final void incrementTotalMoxie( int increment )
	{
		KoLCharacter.totalSubpoints[ 2 ] += increment;
		if ( KoLCharacter.totalSubpoints[ 2 ] >= KoLCharacter.triggerSubpoints[ 2 ] )
		{
			KoLCharacter.handleTrigger( KoLCharacter.triggerItem[ 2 ] );
		}
	}

	public static final boolean moxieTrigger( int basepoints, int itemId )
	{
		long points = calculatePointSubpoints( basepoints );
		if ( points < KoLCharacter.triggerSubpoints[ 2 ] )
		{
			KoLCharacter.triggerSubpoints[ 2 ] = points;
			KoLCharacter.triggerItem[ 2 ] = itemId;
		}
		return false;	// for the convenience of the caller
	}

	/**
	 * Accessor method to retrieve the number of subpoints required before the character gains another full point of
	 * moxie.
	 */

	public static final int getMoxieTNP()
	{
		return KoLCharacter.calculateTillNextPoint( KoLCharacter.totalSubpoints[ 2 ] );
	}

	/**
	 * Accessor method to retrieve the character's adjusted value for moxie.
	 *
	 * @return The character's adjusted value for moxie
	 */

	public static final int getAdjustedMoxie()
	{
		return KoLCharacter.adjustedStats[ 2 ];
	}

	/**
	 * Accessor method to set the number of adventures the character has left to spend in this session.
	 *
	 * @param adventuresLeft The number of adventures the character has left
	 */

	public static final void setAdventuresLeft( final int adventuresLeft )
	{
		if ( adventuresLeft != KoLCharacter.adventuresLeft )
		{
			if ( Preferences.getBoolean( "useDockIconBadge" ) )
			{
				OSXAdapter.setDockIconBadge( String.valueOf( adventuresLeft ) );
			}

			KoLCharacter.adventuresLeft = adventuresLeft;
			if ( KoLCharacter.canEat() && !KoLCharacter.hasChef() ||
			     KoLCharacter.canDrink() && !KoLCharacter.hasBartender() )
			{
				ConcoctionDatabase.refreshConcoctions();
			}
		}
	}

	/**
	 * Accessor method to retrieve the number of adventures the character has left to spend in this session.
	 *
	 * @return The number of adventures the character has left
	 */

	public static final int getAdventuresLeft()
	{
		return KoLCharacter.adventuresLeft;
	}

	/**
	 * Accessor method to retrieve the total number of turns the character
	 * has used this run.
	 */

	public static final int getCurrentRun()
	{
		return KoLCharacter.currentRun;
	}

	public static final void setCurrentRun( final int currentRun )
	{
		KoLCharacter.currentRun = currentRun;
	}

	/**
	 * Accessor method to retrieve the current daycount for this run
	 */

	public static final int getCurrentDays()
	{
		return KoLCharacter.daycount;
	}

	public static final void setCurrentDays( final int daycount )
	{
		KoLCharacter.daycount = daycount;
	}

	/**
	 * Accessor method to record the turn count when a semirare was found.
	 */

	public static final void registerSemirare()
	{
		KoLCharacter.ensureUpdatedSemirareCounter();
		Preferences.setInteger( "semirareCounter", KoLCharacter.currentRun + 1 );
		KoLAdventure location = KoLAdventure.lastVisitedLocation();
		String loc = ( location == null ) ? "" : location.getAdventureName();
		Preferences.setString( "semirareLocation", loc );
		TurnCounter.stopCounting( "Fortune Cookie" );
		TurnCounter.stopCounting( "Semirare window begin" );
		TurnCounter.stopCounting( "Semirare window end" );
		int begin = 100;
		int end = 120;
		if ( KoLCharacter.canEat() || KoLCharacter.canDrink() )
		{
			begin = 160;
			end = 200;
		}
		TurnCounter.startCounting( begin + 1, "Semirare window begin loc=*", "lparen.gif" );
		TurnCounter.startCounting( end + 1, "Semirare window end loc=*", "rparen.gif" );
	}

	/**
	 * Accessor method to return how many turns have passed since the last
	 * semirare was found.
	 */

	public static final int turnsSinceLastSemirare()
	{
		KoLCharacter.ensureUpdatedSemirareCounter();
		int last = Preferences.getInteger( "semirareCounter" );
		return KoLCharacter.currentRun - last;
	}

	public static final int lastSemirareTurn()
	{
		KoLCharacter.ensureUpdatedSemirareCounter();
		return Preferences.getInteger( "semirareCounter" );
	}

	/**
	 * Accessor method to retrieve the current value of a named modifier
	 */

	public static final Modifiers getCurrentModifiers()
	{
		return KoLCharacter.currentModifiers;
	}

	public static final float currentNumericModifier( final String name )
	{
		return KoLCharacter.currentModifiers.get( name );
	}

	public static final float currentNumericModifier( final int index )
	{
		return KoLCharacter.currentModifiers.get( index );
	}

	public static final int currentRawBitmapModifier( final String name )
	{
		return KoLCharacter.currentModifiers.getRawBitmap( name );
	}

	public static final int currentRawBitmapModifier( final int index )
	{
		return KoLCharacter.currentModifiers.getRawBitmap( index );
	}

	public static final int currentBitmapModifier( final String name )
	{
		return KoLCharacter.currentModifiers.getBitmap( name );
	}

	public static final int currentBitmapModifier( final int index )
	{
		return KoLCharacter.currentModifiers.getBitmap( index );
	}

	public static final boolean currentBooleanModifier( final String name )
	{
		return KoLCharacter.currentModifiers.getBoolean( name );
	}

	public static final boolean currentBooleanModifier( final int index )
	{
		return KoLCharacter.currentModifiers.getBoolean( index );
	}

	public static final String currentStringModifier( final String name )
	{
		return KoLCharacter.currentModifiers.getString( name );
	}

	public static final String currentStringModifier( final int index )
	{
		return KoLCharacter.currentModifiers.getString( index );
	}

	/**
	 * Accessor method to retrieve the total current monster level adjustment
	 */

	public static final int getMonsterLevelAdjustment()
	{
		return (int) KoLCharacter.currentModifiers.get( Modifiers.MONSTER_LEVEL );
	}

	/**
	 * Accessor method to retrieve the total current familiar weight adjustment
	 */

	public static final int getFamiliarWeightAdjustment()
	{
		return (int) (KoLCharacter.currentModifiers.get( Modifiers.FAMILIAR_WEIGHT ) +
			KoLCharacter.currentModifiers.get( Modifiers.HIDDEN_FAMILIAR_WEIGHT ));
	}

	public static final int getFamiliarWeightPercentAdjustment()
	{
		return (int) KoLCharacter.currentModifiers.get( Modifiers.FAMILIAR_WEIGHT_PCT );
	}

	public static final int getManaCostAdjustment()
	{
		return KoLCharacter.getManaCostAdjustment( false );
	}

	public static final int getManaCostAdjustment( final boolean combat )
	{
		return (int) KoLCharacter.currentModifiers.get( Modifiers.MANA_COST ) +
			(int) KoLCharacter.currentModifiers.get( Modifiers.STACKABLE_MANA_COST ) +
			( combat ? (int) KoLCharacter.currentModifiers.get( Modifiers.COMBAT_MANA_COST ) : 0 )
			- KoLCharacter.holidayManaCostReduction;
	}

	/**
	 * Accessor method to retrieve the total current combat percent adjustment
	 */

	public static final float getCombatRateAdjustment()
	{
		return KoLCharacter.currentModifiers.get( Modifiers.COMBAT_RATE );
	}

	/**
	 * Accessor method to retrieve the total current initiative adjustment
	 */

	public static final float getInitiativeAdjustment()
	{
		// Penalty is constrained to be non-positive
		return KoLCharacter.currentModifiers.get( Modifiers.INITIATIVE ) +
			Math.min( KoLCharacter.currentModifiers.get( Modifiers.INITIATIVE_PENALTY ), 0.0f );
	}

	/**
	 * Accessor method to retrieve the total current fixed experience adjustment
	 */

	public static final float getExperienceAdjustment()
	{
		return KoLCharacter.currentModifiers.get(
			Modifiers.MUS_EXPERIENCE + KoLCharacter.getPrimeIndex() );
	}

	/**
	 * Accessor method to retrieve the total current meat drop percent adjustment
	 *
	 * @return Total Current Meat Drop Percent Adjustment
	 */

	public static final float getMeatDropPercentAdjustment()
	{
		// Penalty is constrained to be non-positive
		return KoLCharacter.currentModifiers.get( Modifiers.MEATDROP ) +
			Math.min( KoLCharacter.currentModifiers.get( Modifiers.MEATDROP_PENALTY ), 0.0f );
	}

	/**
	 * Accessor method to retrieve the total current item drop percent adjustment
	 *
	 * @return Total Current Item Drop Percent Adjustment
	 */

	public static final float getItemDropPercentAdjustment()
	{
		return KoLCharacter.currentModifiers.get( Modifiers.ITEMDROP ) +
			Math.min( KoLCharacter.currentModifiers.get( Modifiers.ITEMDROP_PENALTY ), 0.0f );
	}

	/**
	 * Accessor method to retrieve the total current damage absorption
	 *
	 * @return Total Current Damage Absorption
	 */

	public static final int getDamageAbsorption()
	{
		return (int) KoLCharacter.currentModifiers.get( Modifiers.DAMAGE_ABSORPTION );
	}

	/**
	 * Accessor method to retrieve the total current damage reduction
	 *
	 * @return Total Current Damage Reduction
	 */

	public static final int getDamageReduction()
	{
		return (int) KoLCharacter.currentModifiers.get( Modifiers.DAMAGE_REDUCTION );
	}

	/**
	 * Accessor method to retrieve the total Hobo Power
	 *
	 * @return Total Hobo Power
	 */

	public static final int getHoboPower()
	{
		return (int) KoLCharacter.currentModifiers.get( Modifiers.HOBO_POWER );
	}

	/**
	 * Accessor method to retrieve the player's Clownosity
	 *
	 * @return Clownosity
	 */

	public static final int getClownosity()
	{
		return KoLCharacter.currentModifiers.getBitmap( Modifiers.CLOWNOSITY );
	}

	public static final int getRestingHP()
	{
		int rv = (int) KoLCharacter.currentModifiers.get( Modifiers.BASE_RESTING_HP );
		float factor = KoLCharacter.currentModifiers.get( Modifiers.RESTING_HP_PCT );
		if ( factor != 0 )
		{
			rv = (int) (rv * (factor + 100.0f) / 100.0f);
		}
		return rv + (int) KoLCharacter.currentModifiers.get( Modifiers.BONUS_RESTING_HP );
	}

	public static final int getRestingMP()
	{
		int rv = (int) KoLCharacter.currentModifiers.get( Modifiers.BASE_RESTING_MP );
		float factor = KoLCharacter.currentModifiers.get( Modifiers.RESTING_MP_PCT );
		if ( factor != 0 )
		{
			rv = (int) (rv * (factor + 100.0f) / 100.0f);
		}
		return rv + (int) KoLCharacter.currentModifiers.get( Modifiers.BONUS_RESTING_MP );
	}

	/**
	 * Accessor method to retrieve the current elemental resistance levels
	 *
	 * @return Total Current Resistance to specified element
	 */

	public static final int getElementalResistanceLevels( final int element )
	{
		switch ( element )
		{
		case MonsterDatabase.COLD:
			return (int) KoLCharacter.currentModifiers.get( Modifiers.COLD_RESISTANCE );
		case MonsterDatabase.HEAT:
			return (int) KoLCharacter.currentModifiers.get( Modifiers.HOT_RESISTANCE );
		case MonsterDatabase.SLEAZE:
			return (int) KoLCharacter.currentModifiers.get( Modifiers.SLEAZE_RESISTANCE );
		case MonsterDatabase.SPOOKY:
			return (int) KoLCharacter.currentModifiers.get( Modifiers.SPOOKY_RESISTANCE );
		case MonsterDatabase.STENCH:
			return (int) KoLCharacter.currentModifiers.get( Modifiers.STENCH_RESISTANCE );
		case MonsterDatabase.SLIME:
			return (int) KoLCharacter.currentModifiers.get( Modifiers.SLIME_RESISTANCE );
		default:
			return 0;
		}
	}


	public static final float elementalResistanceByLevel( final int levels )
	{
		return KoLCharacter.elementalResistanceByLevel( levels, true );
	}

	public static final float elementalResistanceByLevel( final int levels, final boolean mystBonus )
	{
		// salien has a formula which matches my data very nicely:
		// http://jick-nerfed.us/forums/viewtopic.php?t=4526
		// For X > 4: 90 - 50 * (5/6)^(X-4)

		double value;

		if ( levels > 4 )
		{
			value = 90.0 - 50.0 * Math.pow( 5.0 / 6.0, levels - 4 );
		}
		else
		{
			value = levels * 10.0;
		}

		if ( mystBonus && KoLCharacter.isMysticalityClass() )
		{
			value += 5.0;
		}

		return (float)value;
	}

	/**
	 * Accessor method to retrieve the current elemental resistance
	 *
	 * @return Total Current Resistance to specified element
	 */

	public static final float getElementalResistance( final int element )
	{
		if ( element == MonsterDatabase.NONE )
		{
			return 0.0f;
		}
		int levels = KoLCharacter.getElementalResistanceLevels( element );
		return KoLCharacter.elementalResistanceByLevel( levels, element != MonsterDatabase.SLIME );
	}

	/**
	 * Accessor method which indicates whether or not the the beanstalk has been armed this session.
	 *
	 * @return <code>true</code> if the beanstalk has been armed
	 */

	public static final boolean beanstalkArmed()
	{
		return KoLCharacter.beanstalkArmed;
	}

	/**
	 * Accessor method to indicate a change in state of the beanstalk
	 */

	public static final void armBeanstalk()
	{
		KoLCharacter.beanstalkArmed = true;
	}

	/**
	 * Accessor method which indicates whether or not the character has store in the mall
	 *
	 * @return <code>true</code> if the character has a store
	 */

	public static final boolean hasStore()
	{
		return KoLCharacter.hasStore;
	}

	/**
	 * Accessor method to indicate a change in state of the mall store.
	 *
	 * @param hasStore Whether or not the character currently has a store
	 */

	public static final void setStore( final boolean hasStore )
	{
		KoLCharacter.hasStore = hasStore;
	}

	/**
	 * Accessor method which indicates whether or not the character has display case
	 *
	 * @return <code>true</code> if the character has a display case
	 */

	public static final boolean hasDisplayCase()
	{
		return KoLCharacter.hasDisplayCase;
	}

	/**
	 * Accessor method to indicate a change in state of the museum display case
	 *
	 * @param hasDisplayCase Whether or not the character currently has display case
	 */

	public static final void setDisplayCase( final boolean hasDisplayCase )
	{
		KoLCharacter.hasDisplayCase = hasDisplayCase;
	}

	/**
	 * Accessor method which indicates whether or not the character is in a clan
	 *
	 * @return <code>true</code> if the character is in a clan
	 */

	public static final boolean hasClan()
	{
		return KoLCharacter.hasClan;
	}

	/**
	 * Accessor method to indicate a change in state of the character's clan membership
	 *
	 * @param hasClan Whether or not the character currently is in a clan
	 */

	public static final void setClan( final boolean hasClan )
	{
		KoLCharacter.hasClan = hasClan;
	}

	/**
	 * Accessor method which indicates whether or not the character has a shaker
	 *
	 * @return <code>true</code> if the character has a shaker
	 */

	public static final boolean hasShaker()
	{
		return KoLCharacter.hasShaker;
	}

	/**
	 * Accessor method to indicate a change in state of the shaker
	 *
	 * @param hasShaker Whether or not the character currently has a shaker
	 */

	public static final void setShaker( final boolean hasShaker )
	{
		if ( KoLCharacter.hasShaker != hasShaker )
		{
			KoLCharacter.hasShaker = hasShaker;
			ConcoctionDatabase.refreshConcoctions();
			ItemDatabase.calculateAdventureRanges();
		}
	}

	/**
	 * Accessor method which indicates whether or not the character has a cocktail crafting kit
	 *
	 * @return <code>true</code> if the character has a cocktail crafting kit
	 */

	public static final boolean hasCocktailKit()
	{
		return KoLCharacter.hasCocktailKit;
	}

	/**
	 * Accessor method to indicate a change in state of the cocktail crafting kit
	 *
	 * @param hasCocktailKit Whether or not the character currently has a cocktail crafting kit
	 */

	public static final void setCocktailKit( final boolean hasCocktailKit )
	{
		if ( KoLCharacter.hasCocktailKit != hasCocktailKit )
		{
			KoLCharacter.hasCocktailKit = hasCocktailKit;
			ConcoctionDatabase.refreshConcoctions();
			ItemDatabase.calculateAdventureRanges();
		}
	}

	/**
	 * Accessor method which indicates whether or not the character has a bartender-in-the-box.
	 *
	 * @return <code>true</code> if the character has a bartender-in-the-box
	 */

	public static final boolean hasBartender()
	{
		return KoLCharacter.hasBartender;
	}

	/**
	 * Accessor method to indicate a change in state of the bartender-in-the-box.
	 *
	 * @param hasBartender Whether or not the character currently has a bartender
	 */

	public static final void setBartender( final boolean hasBartender )
	{
		if ( KoLCharacter.hasBartender != hasBartender )
		{
			KoLCharacter.hasBartender = hasBartender;
			ConcoctionDatabase.refreshConcoctions();
			ItemDatabase.calculateAdventureRanges();
		}
	}

	/**
	 * Accessor method which indicates whether or not the character has an oven
	 *
	 * @return <code>true</code> if the character has an oven
	 */

	public static final boolean hasOven()
	{
		return KoLCharacter.hasOven;
	}

	/**
	 * Accessor method to indicate a change in state of the oven
	 *
	 * @param hasOven Whether or not the character currently has an oven
	 */

	public static final void setOven( final boolean hasOven )
	{
		if ( KoLCharacter.hasOven != hasOven )
		{
			KoLCharacter.hasOven = hasOven;
			ConcoctionDatabase.refreshConcoctions();
			ItemDatabase.calculateAdventureRanges();
		}
	}

	/**
	 * Accessor method which indicates whether or not the character has a range
	 *
	 * @return <code>true</code> if the character has a range
	 */

	public static final boolean hasRange()
	{
		return KoLCharacter.hasRange;
	}

	/**
	 * Accessor method to indicate a change in state of the range
	 *
	 * @param hasRange Whether or not the character currently has a range
	 */

	public static final void setRange( final boolean hasRange )
	{
		if ( KoLCharacter.hasRange != hasRange )
		{
			KoLCharacter.hasRange = hasRange;
			ConcoctionDatabase.refreshConcoctions();
			ItemDatabase.calculateAdventureRanges();
		}
	}

	/**
	 * Accessor method which indicates whether or not the character has a chef-in-the-box.
	 *
	 * @return <code>true</code> if the character has a chef-in-the-box
	 */

	public static final boolean hasChef()
	{
		return KoLCharacter.hasChef;
	}

	/**
	 * Accessor method to indicate a change in state of the chef-in-the-box.
	 *
	 * @param hasChef Whether or not the character currently has a chef
	 */

	public static final void setChef( final boolean hasChef )
	{
		if ( KoLCharacter.hasChef != hasChef )
		{
			KoLCharacter.hasChef = hasChef;
			ConcoctionDatabase.refreshConcoctions();
			ItemDatabase.calculateAdventureRanges();
		}
	}

	/**
	 * Accessor method which indicates whether or not the character has a sushi rolling mat
	 *
	 * @return <code>true</code> if the character has a sushi rolling mat
	 */

	public static final boolean hasSushiMat()
	{
		return KoLCharacter.hasSushiMat;
	}

	/**
	 * Accessor method to indicate a change in state of the sushi rolling mat
	 *
	 * @param hasSushiMat Whether or not the character currently has a sushi rolling mat
	 */

	public static final void setSushiMat( final boolean hasSushiMat )
	{
		if ( KoLCharacter.hasSushiMat != hasSushiMat )
		{
			KoLCharacter.hasSushiMat = hasSushiMat;
			ConcoctionDatabase.refreshConcoctions();
			ItemDatabase.calculateAdventureRanges();
		}
	}

	/**
	 * Accessor method which indicates whether or not the character has a mystical bookshelf
	 *
	 * @return <code>true</code> if the character has a mystical bookshelf
	 */

	public static final boolean hasBookshelf()
	{
		return KoLCharacter.hasBookshelf;
	}

	/**
	 * Accessor method to indicate a change in state of the mystical bookshelf
	 *
	 * @param hasBookshelf Whether or not the character currently has a bookshelf
	 */

	public static final void setBookshelf( final boolean hasBookshelf )
	{
		boolean refresh = hasBookshelf && KoLCharacter.hasBookshelf != hasBookshelf;
		KoLCharacter.hasBookshelf = hasBookshelf;
		if ( refresh )
		{
			RequestThread.postRequest( new CampgroundRequest( "bookshelf" ) );
		}
	}

	/**
	 * Accessor method which indicates how many times the character has upgraded their telescope
	 *
	 * @return <code>int/code> power of telescope
	 */

	public static final int getTelescopeUpgrades()
	{
		return KoLCharacter.telescopeUpgrades;
	}

	/**
	 * Accessor method to indicate a change in state of the telescope
	 */

	public static final void setTelescopeUpgrades( final int upgrades )
	{
		KoLCharacter.telescopeUpgrades = upgrades;
	}

	/**
	 * Accessor method to indicate a change in state of the telescope
	 */

	public static final void setTelescope( final boolean present )
	{
		KoLCharacter.telescopeUpgrades = Preferences.getInteger( "telescopeUpgrades" );
		// Assume newly detected telescope is basic. We'll look through
		// it when checkTelescope is called.
		if ( present && KoLCharacter.telescopeUpgrades == 0 )
		{
			KoLCharacter.telescopeUpgrades = 1;
		}
	}

	/**
	 * Method to look through the telescope if it hasn't been done yet
	 */

	public static final void checkTelescope()
	{
		if ( KoLCharacter.telescopeUpgrades == 0 )
		{
			return;
		}

		if ( KoLCharacter.inBadMoon() && !KoLCharacter.kingLiberated() )
		{
			return;
		}

		int lastAscension = Preferences.getInteger( "lastTelescopeReset" );
		if ( lastAscension < KoLCharacter.ascensions )
		{
			RequestThread.postRequest( new TelescopeRequest( TelescopeRequest.LOW ) );
		}
	}

	/**
	 * Accessor method which indicates whether or not the character has freed King Ralph
	 *
	 * @return <code>true</code> if the character has freed King Ralph
	 */

	public static final boolean kingLiberated()
	{
		int lastAscension = Preferences.getInteger( "lastKingLiberation" );
		if ( lastAscension < KoLCharacter.ascensions )
		{
			Preferences.setInteger( "lastKingLiberation", KoLCharacter.getAscensions() );
			Preferences.setBoolean( "kingLiberated", false );
			return false;
		}
		return Preferences.getBoolean( "kingLiberated" );
	}

	public static final void liberateKing()
	{
		if ( !KoLCharacter.kingLiberated() )
		{
			Preferences.setBoolean( "kingLiberated", true );
			CharPaneRequest.setInteraction( true );
			KoLConstants.storage.addAll( KoLConstants.freepulls );
			KoLConstants.freepulls.clear();
			// If we are in bad moon, we can use the bookshelf and
			// telescope again.
			if ( KoLCharacter.inBadMoon() )
			{
				RequestThread.postRequest( new CampgroundRequest( "bookshelf" ) );
				KoLCharacter.checkTelescope();
			}
		}
	}

	/**
	 * Accessor method which tells you if the character can interact with other players (Ronin or Hardcore players
	 * cannot).
	 */

	public static final boolean canInteract()
	{
		return CharPaneRequest.canInteract();
	}

	/**
	 * Returns whether or not the character is currently in hardcore.
	 */

	public static final boolean isHardcore()
	{
		return KoLCharacter.isHardcore;
	}

	/**
	 * Accessor method which sets whether or not the player is currently in hardcore.
	 */

	public static final void setHardcore( final boolean isHardcore )
	{
		KoLCharacter.isHardcore = isHardcore;
	}

	/**
	 * Returns whether or not the character is currently in roin.
	 */

	public static final boolean inRonin()
	{
		return KoLCharacter.inRonin;
	}

	public static final void setSkillsRecalled( final boolean skillsRecalled )
	{
		KoLCharacter.skillsRecalled = skillsRecalled;
	}

	public static final boolean skillsRecalled()
	{
		return KoLCharacter.skillsRecalled;
	}

	/**
	 * Accessor method which sets whether or not the player is currently in ronin.
	 */

	public static final void setRonin( final boolean inRonin )
	{
		KoLCharacter.inRonin = inRonin;
	}

	/**
	 * Accessor method for the character's ascension count
	 *
	 * @return String
	 */

	public static final int getAscensions()
	{
		return KoLCharacter.ascensions;
	}

	/**
	 * Accessor method for the character's zodiac sign
	 *
	 * @return String
	 */

	public static final String getSign()
	{
		return KoLCharacter.ascensionSign;
	}

	/**
	 * Accessor method for the character's zodiac sign stat
	 *
	 * @return int
	 */

	public static final int getSignStat()
	{
		return KoLCharacter.ascensionSignType;
	}

	/**
	 * Accessor method to set a character's ascension count
	 *
	 * @param ascensions the new ascension count
	 */

	public static final void setAscensions( final int ascensions )
	{
		KoLCharacter.ascensions = ascensions;
	}

	/**
	 * Accessor method to set a character's zodiac sign
	 *
	 * @param ascensionSign the new sign
	 */

	public static final void setSign( String ascensionSign )
	{
		if ( ascensionSign.startsWith( "The " ) )
		{
			ascensionSign = ascensionSign.substring( 4 );
		}

		KoLCharacter.ascensionSign = ascensionSign;

		if ( ascensionSign.equals( "Wallaby" ) || ascensionSign.equals( "Mongoose" ) || ascensionSign.equals( "Vole" ) )
		{
			KoLCharacter.ascensionSignType = KoLConstants.MUSCLE;
		}
		else if ( ascensionSign.equals( "Platypus" ) || ascensionSign.equals( "Opossum" ) || ascensionSign.equals( "Marmot" ) )
		{
			KoLCharacter.ascensionSignType = KoLConstants.MYSTICALITY;
		}
		else if ( ascensionSign.equals( "Wombat" ) || ascensionSign.equals( "Blender" ) || ascensionSign.equals( "Packrat" ) )
		{
			KoLCharacter.ascensionSignType = KoLConstants.MOXIE;
		}
		else if ( ascensionSign.equals( "Bad Moon" ) )
		{
			KoLCharacter.ascensionSignType = KoLConstants.BAD_MOON;
		}
		else
		{
			KoLCharacter.ascensionSignType = KoLConstants.NONE;
		}
	}

	public static final int getConsumptionRestriction()
	{
		return KoLCharacter.consumptionRestriction;
	}

	public static final void setConsumptionRestriction( final int consumptionRestriction )
	{
		KoLCharacter.consumptionRestriction = consumptionRestriction;
	}

	public static final boolean canEat()
	{
		return KoLCharacter.consumptionRestriction == AscensionSnapshot.NOPATH || KoLCharacter.consumptionRestriction == AscensionSnapshot.TEETOTALER;
	}

	public static final boolean canDrink()
	{
		return KoLCharacter.consumptionRestriction == AscensionSnapshot.NOPATH || KoLCharacter.consumptionRestriction == AscensionSnapshot.BOOZETAFARIAN;
	}

	/**
	 * Accessor method for the current mind control setting
	 *
	 * @return int
	 */

	public static final int getMindControlLevel()
	{
		return KoLCharacter.mindControlLevel;
	}

	/**
	 * Accessor method to set the current mind control level
	 *
	 * @param level the new level
	 */

	public static final void setMindControlLevel( final int level )
	{
		if ( KoLCharacter.mindControlLevel != level )
		{
			KoLCharacter.mindControlLevel = level;
			KoLCharacter.recalculateAdjustments();
			KoLCharacter.updateStatus();
			AdventureFrame.updateSafetyDetails();
		}
	}

	/**
	 * Accessor method for the current auto attack action
	 *
	 * @return String
	 */

	public static final int getAutoAttackAction()
	{
		return KoLCharacter.autoAttackAction;
	}

	/**
	 * Accessor method to set the current auto attack action
	 *
	 * @param autoAttackAction the current auto attack action
	 */

	public static final void setAutoAttackAction( final int autoAttackAction )
	{
		KoLCharacter.autoAttackAction = autoAttackAction;
	}

	/**
	 * Accessor method for the current autosell mode
	 *
	 * @return String
	 */

	public static final String getAutosellMode()
	{
		return KoLCharacter.autosellMode;
	}

	/**
	 * Accessor method to set the autosell mode
	 *
	 * @param mode the new mode
	 */

	public static final void setAutosellMode( final String mode )
	{
		KoLCharacter.autosellMode = mode;
	}

	/**
	 * Accessor method for the current unequip familiar mode
	 *
	 * @return boolean
	 */

	public static final boolean getUnequipFamiliar()
	{
		return KoLCharacter.unequipFamiliar;
	}

	/**
	 * Accessor method to set the unequip familiar mode
	 *
	 * @param mode the new mode
	 */

	public static final void setUnequipFamiliar( final boolean mode )
	{
		KoLCharacter.unequipFamiliar = mode;
	}

	/**
	 * Accessor method which indicates whether the character is in a Muscle sign KoLmafia could/should use this to: -
	 * Allow adventuring in The Bugbear Pens - Provide access to npcstore #4: The Degrassi Knoll Bakery - Provide access
	 * to npcstore #5: The Degrassi Knoll General Store - Train Muscle in The Gym - Smith non-advanced things using
	 * Innabox (no hammer/adventure) - Combine anything using The Plunger (no meat paste)
	 *
	 * @return <code>true</code> if the character is in a Muscle sign
	 */

	public static final boolean inMuscleSign()
	{
		return KoLCharacter.ascensionSignType == KoLConstants.MUSCLE;
	}

	/**
	 * Accessor method which indicates whether the character is in a Mysticality sign KoLmafia could/should use this to: -
	 * Allow adventuring in Outskirts of Camp Logging Camp - Allow adventuring in Camp Logging Camp - Provide access to
	 * npcstore #j: Little Canadia Jewelers - Train Mysticality in The Institute for Canadian Studies
	 *
	 * @return <code>true</code> if the character is in a Mysticality sign
	 */

	public static final boolean inMysticalitySign()
	{
		return KoLCharacter.ascensionSignType == KoLConstants.MYSTICALITY;
	}

	/**
	 * Accessor method which indicates whether the character is in a Moxie sign KoLmafia could/should use this to: -
	 * Allow adventuring in Thugnderdome - Provide access to TINKER recipes - Train Moxie with Gnirf
	 *
	 * @return <code>true</code> if the character is in a Moxie sign
	 */

	public static final boolean inMoxieSign()
	{
		return KoLCharacter.ascensionSignType == KoLConstants.MOXIE;
	}

	/**
	 * Accessor method which indicates whether the character is in Bad Moon KoLmafia could/should use this to: -
	 * Eliminate access to Hagnks - Provide access to Hell's Kitchen - Provide access to Nervewrecker's Store
	 *
	 * @return <code>true</code> if the character is in Bad Moon
	 */

	public static final boolean inBadMoon()
	{
		return KoLCharacter.ascensionSignType == KoLConstants.BAD_MOON;
	}

	/**
	 * Accessor method which indicates whether the MCD is potentially
	 * available
	 *
	 * @return <code>true</code> if the character can potentially change
	 * monster level
	 */

	public static final boolean mcdAvailable()
	{
		switch ( KoLCharacter.ascensionSignType )
		{
		case KoLConstants.MYSTICALITY:
			// Direct access to the Mind Control Device
		case KoLConstants.MUSCLE:
			// detuned radio from Degrassi Knoll General Store
		case KoLConstants.MOXIE:
			// Annoyotron available on beach
			return true;
		}
		return false;
	}

	/**
	 * Accessor method to set the list of available skills.
	 *
	 * @param newSkillSet The list of the names of available skills
	 */

	public static final void setAvailableSkills( final List newSkillSet )
	{
		if ( KoLCharacter.isMoxieClass() )
		{
			KoLCharacter.addAvailableSkill( "Moxious Maneuver" );
		}

		// Check all available skills to see if they
		// qualify to be added as combat or usables.

		for ( int i = 0; i < newSkillSet.size(); ++i )
		{
			KoLCharacter.addAvailableSkill( (UseSkillRequest) newSkillSet.get( i ) );
		}

		// Add derived skills based on base skills

		KoLCharacter.addDerivedSkills();
		KoLConstants.usableSkills.sort();
		KoLConstants.summoningSkills.sort();
		KoLConstants.remedySkills.sort();
		KoLConstants.selfOnlySkills.sort();
		KoLConstants.buffSkills.sort();

		int battleIndex = KoLCharacter.battleSkillNames.indexOf( Preferences.getString( "battleAction" ) );
		KoLCharacter.battleSkillNames.setSelectedIndex( battleIndex == -1 ? 0 : battleIndex );

		DiscoCombatHelper.initialize();
	}

	public static final void setPermedSkills( final List newSkillSet )
	{
		KoLConstants.permedSkills.clear();

		for ( int i = 0; i < newSkillSet.size(); ++i )
		{
			UseSkillRequest skill = (UseSkillRequest) newSkillSet.get( i );
			KoLConstants.permedSkills.add( skill );
		}
	}

	/**
	 * Adds a single skill to the list of known skills possessed by this character.
	 */

	public static final void addAvailableSkill( final UseSkillRequest skill )
	{
		if ( skill == null )
		{
			return;
		}

		if ( KoLConstants.availableSkillsMap.containsKey( skill ) )
		{
			return;
		}

		KoLConstants.availableSkills.add( skill );
		KoLConstants.availableSkillsMap.put( skill, null );

		switch ( SkillDatabase.getSkillType( skill.getSkillId() ) )
		{
		case SkillDatabase.PASSIVE:

			// Flavour of Magic gives you access to five other
			// castable skills

			if ( skill.getSkillName().equals( "Flavour of Magic" ) )
			{
				KoLCharacter.addAvailableSkill( "Spirit of Cayenne" );
				KoLCharacter.addAvailableSkill( "Spirit of Peppermint" );
				KoLCharacter.addAvailableSkill( "Spirit of Garlic" );
				KoLCharacter.addAvailableSkill( "Spirit of Wormwood" );
				KoLCharacter.addAvailableSkill( "Spirit of Bacon Grease" );
			}

			break;

		case SkillDatabase.SUMMON:
			KoLConstants.usableSkills.add( skill );
			KoLConstants.usableSkills.sort();
			KoLConstants.summoningSkills.add( skill );
			KoLConstants.summoningSkills.sort();
			break;

		case SkillDatabase.REMEDY:
			KoLConstants.usableSkills.add( skill );
			KoLConstants.usableSkills.sort();
			KoLConstants.remedySkills.add( skill );
			KoLConstants.remedySkills.sort();
			break;

		case SkillDatabase.SELF_ONLY:
			KoLConstants.usableSkills.add( skill );
			KoLConstants.usableSkills.sort();
			KoLConstants.selfOnlySkills.add( skill );
			KoLConstants.selfOnlySkills.sort();
			break;

		case SkillDatabase.BUFF:

			KoLConstants.usableSkills.add( skill );
			KoLConstants.usableSkills.sort();
			KoLConstants.buffSkills.add( skill );
			KoLConstants.buffSkills.sort();
			break;

		case SkillDatabase.COMBAT:

			KoLCharacter.addCombatSkill( skill.getSkillName() );
			break;
		}
	}

	public static final void addAvailableSkill( final String name )
	{
		KoLCharacter.addAvailableSkill( UseSkillRequest.getInstance( name ) );
	}

	/**
	 * Adds derived skills to appropriate lists
	 */

	public static final void addDerivedSkills()
	{
		if ( KoLCharacter.classtype.startsWith( "Tu" ) )
		{
			boolean head = KoLCharacter.hasSkill( "Headbutt" );
			boolean knee = KoLCharacter.hasSkill( "Kneebutt" );
			boolean shield = KoLCharacter.hasSkill( "Shieldbutt" );

			if ( head && knee )
			{
				KoLCharacter.addAvailableSkill( "Head + Knee Combo" );
			}
			if ( head && shield )
			{
				KoLCharacter.addAvailableSkill( "Head + Shield Combo" );
			}
			if ( knee && shield )
			{
				KoLCharacter.addAvailableSkill( "Knee + Shield Combo" );
			}
			if ( head && knee && shield )
			{
				KoLCharacter.addAvailableSkill( "Head + Knee + Shield Combo" );
			}
		}
	}

	private static final void addCombatSkill( final String name )
	{
		String skillname = "skill " + name.toLowerCase();
		if ( !KoLCharacter.battleSkillNames.contains( skillname ) )
		{
			KoLCharacter.battleSkillNames.add( skillname );
		}
	}

	/**
	 * Returns a list of the names of all available combat skills. The selected index in this list should match the
	 * selected index in the battle skills list.
	 */

	public static final LockableListModel getBattleSkillNames()
	{
		return KoLCharacter.battleSkillNames;
	}

	/**
	 * Accessor method to look up whether or not the character can summon noodles.
	 *
	 * @return <code>true</code> if noodles can be summoned by this character
	 */

	public static final boolean canSummonNoodles()
	{
		return KoLCharacter.hasSkill( "Pastamastery" );
	}

	/**
	 * Accessor method to look up whether or not the character can summon reagent.
	 *
	 * @return <code>true</code> if reagent can be summoned by this character
	 */

	public static final boolean canSummonReagent()
	{
		return KoLCharacter.hasSkill( "Advanced Saucecrafting" );
	}

	/**
	 * Accessor method to look up whether or not the character can summon shore-based items.
	 *
	 * @return <code>true</code> if shore-based items can be summoned by this character
	 */

	public static final boolean canSummonShore()
	{
		return KoLCharacter.hasSkill( "Advanced Cocktailcrafting" );
	}

	/**
	 * Accessor method to look up whether or not the character can summon snowcones
	 *
	 * @return <code>true</code> if snowcones can be summoned by this character
	 */

	public static final boolean canSummonSnowcones()
	{
		return KoLCharacter.hasSkill( "Summon Snowcones" );
	}

	/**
	 * Accessor method to look up whether or not the character can summon stickers
	 *
	 * @return <code>true</code> if stickers can be summoned by this character
	 */

	public static final boolean canSummonStickers()
	{
		return KoLCharacter.hasSkill( "Summon Stickers" );
	}

	/**
	 * Accessor method to look up whether or not the character can smith weapons.
	 *
	 * @return <code>true</code> if this character can smith advanced weapons
	 */

	public static final boolean canSmithWeapons()
	{
		return KoLCharacter.hasSkill( "Super-Advanced Meatsmithing" );
	}

	/**
	 * Accessor method to look up whether or not the character can smith armor.
	 *
	 * @return <code>true</code> if this character can smith advanced armor
	 */

	public static final boolean canSmithArmor()
	{
		return KoLCharacter.hasSkill( "Armorcraftiness" );
	}

	/**
	 * Accessor method to look up whether or not the character can craft expensive jewelry
	 *
	 * @return <code>true</code> if this character can smith advanced weapons
	 */

	public static final boolean canCraftExpensiveJewelry()
	{
		return KoLCharacter.hasSkill( "Really Expensive Jewelrycrafting" );
	}

	/**
	 * Accessor method to look up whether or not the character has Amphibian Sympathy
	 *
	 * @return <code>true</code> if this character has Amphibian Sympathy
	 */

	public static final boolean hasAmphibianSympathy()
	{
		return KoLCharacter.hasSkill( "Amphibian Sympathy" );
	}

	/**
	 * Utility method which looks up whether or not the character has a skill of the given name.
	 */

	public static final boolean hasSkill( final int skillId )
	{
		return KoLCharacter.hasSkill( SkillDatabase.getSkillName( skillId ) );
	}

	public static final boolean hasSkill( final String skillName )
	{
		return KoLCharacter.hasSkill( skillName, KoLConstants.availableSkills );
	}

	public static final boolean hasSkill( final UseSkillRequest skill )
	{
		return KoLCharacter.hasSkill( skill, KoLConstants.availableSkills );
	}

	public static final boolean hasSkill( final String skillName, final LockableListModel list )
	{
		UseSkillRequest skill = UseSkillRequest.getUnmodifiedInstance( skillName );
		return KoLCharacter.hasSkill( skill, list );
	}

	public static final boolean hasSkill( final UseSkillRequest skill, final LockableListModel list )
	{
		if ( list == KoLConstants.availableSkills )
		{
			return KoLConstants.availableSkillsMap.containsKey( skill );
		}
		return list.contains( skill );
	}

	/**
	 * Accessor method to get the current familiar.
	 *
	 * @return familiar The current familiar
	 */

	public static final FamiliarData getFamiliar()
	{
		return KoLCharacter.currentFamiliar == null ? FamiliarData.NO_FAMILIAR : KoLCharacter.currentFamiliar;
	}

	public static final FamiliarData getEnthroned()
	{
		return KoLCharacter.currentEnthroned == null ? FamiliarData.NO_FAMILIAR : KoLCharacter.currentEnthroned;
	}

	public static final boolean isUsingStabBat()
	{
		return KoLCharacter.isUsingStabBat;
	}

	/**
	 * Accessor method to get arena wins
	 *
	 * @return The number of arena wins
	 */

	public static final int getArenaWins()
	{
		// Ensure that the arena opponent list is
		// initialized.

		CakeArenaManager.getOpponentList();
		return KoLCharacter.arenaWins;
	}

	public static final int getStillsAvailable()
	{
		if ( !KoLCharacter.hasSkill( "Superhuman Cocktailcrafting" ) || !KoLCharacter.isMoxieClass() )
		{
			return 0;
		}

		if ( KoLCharacter.stillsAvailable == -1 )
		{
			// Avoid infinite recursion if this request fails, or indirectly
			// calls getStillsAvailable();
			KoLCharacter.stillsAvailable = 0;
			RequestThread.postRequest( new GuildRequest( "still" ) );
		}

		return KoLCharacter.stillsAvailable;
	}

	public static final boolean tripleReagent()
	{
		return KoLCharacter.tripleReagent;
	}

	public static final void setStillsAvailable( final int stillsAvailable )
	{
		if ( KoLCharacter.stillsAvailable != stillsAvailable )
		{
			KoLCharacter.stillsAvailable = stillsAvailable;
			ConcoctionDatabase.refreshConcoctions();
			// Allow Daily Deeds to update when the number of stills changes
			PreferenceListenerRegistry.firePreferenceChanged( "(stills)" );
		}
	}

	public static final void decrementStillsAvailable( final int decrementAmount )
	{
		KoLCharacter.setStillsAvailable( KoLCharacter.stillsAvailable - decrementAmount );
	}

	public static final boolean getGuildStoreOpen()
	{
		if ( KoLCharacter.getAscensions() == Preferences.getInteger( "lastGuildStoreOpen" ) )
		{
			return true;
		}
		if ( KoLCharacter.guildStoreStateKnown )
		{
			return false;
		}
		RequestThread.postRequest( new GuildRequest() );
		return KoLCharacter.getAscensions() == Preferences.getInteger( "lastGuildStoreOpen" );
	}

	public static void setGuildStoreOpen( boolean isOpen )
	{
		if ( isOpen )
		{
			Preferences.setInteger( "lastGuildStoreOpen", KoLCharacter.getAscensions() );
		}
		KoLCharacter.guildStoreStateKnown = true;
	}

	public static final boolean canUseWok()
	{
		return KoLCharacter.hasSkill( "Transcendental Noodlecraft" ) && KoLCharacter.isMysticalityClass();
	}

	public static final boolean canUseMalus()
	{
		return KoLCharacter.hasSkill( "Pulverize" ) && KoLCharacter.isMuscleClass();
	}

	/**
	 * Accessor method to set arena wins
	 *
	 * @param wins The number of arena wins
	 */

	public static final void setArenaWins( final int wins )
	{
		KoLCharacter.arenaWins = wins;
	}

	/**
	 * Accessor method to find the specified familiar.
	 *
	 * @param race The race of the familiar to find
	 * @return familiar The first familiar matching this race
	 */

	public static final FamiliarData findFamiliar( final String race )
	{
		FamiliarData[] familiarArray = new FamiliarData[ KoLCharacter.familiars.size() ];
		KoLCharacter.familiars.toArray( familiarArray );

		for ( int i = 0; i < familiarArray.length; ++i )
		{
			FamiliarData familiar = familiarArray[ i ];
			if ( familiar.getRace().equals( race ) )
			{
				return familiar;
			}
		}

		return null;
	}

	public static final FamiliarData findFamiliar( final int familiarId )
	{
		FamiliarData[] familiarArray = new FamiliarData[ KoLCharacter.familiars.size() ];
		KoLCharacter.familiars.toArray( familiarArray );

		for ( int i = 0; i < familiarArray.length; ++i )
		{
			FamiliarData familiar = familiarArray[ i ];
			if ( familiar.getId() == familiarId )
			{
				return familiar;
			}
		}

		return null;
	}

	/**
	 * Accessor method to set the data for the current familiar.
	 *
	 * @param familiar The new current familiar
	 */

	public static final void setFamiliar( final FamiliarData familiar )
	{
		KoLCharacter.currentFamiliar = KoLCharacter.addFamiliar( familiar );
		if ( KoLCharacter.currentFamiliar.equals( KoLCharacter.currentEnthroned ) )
		{
			KoLCharacter.currentEnthroned = FamiliarData.NO_FAMILIAR;
		}

		KoLCharacter.familiars.setSelectedItem( KoLCharacter.currentFamiliar );
		EquipmentManager.setEquipment( EquipmentManager.FAMILIAR, KoLCharacter.currentFamiliar.getItem() );

		KoLCharacter.isUsingStabBat =
			KoLCharacter.currentFamiliar.getRace().equals( "Stab Bat" ) ||
			KoLCharacter.currentFamiliar.getRace().equals( "Scary Death Orb" );

		EquipmentManager.updateEquipmentList( EquipmentManager.FAMILIAR );
		KoLCharacter.recalculateAdjustments();
		KoLCharacter.updateStatus();
	}

	public static final void setEnthroned( final FamiliarData familiar )
	{
		KoLCharacter.currentEnthroned = KoLCharacter.addFamiliar( familiar );
		KoLCharacter.recalculateAdjustments();
		KoLCharacter.updateStatus();
	}

	/**
	 * Accessor method to increment the weight of the current familiar by one.
	 */

	public static final void incrementFamilarWeight()
	{
		if ( KoLCharacter.currentFamiliar != null )
		{
			KoLCharacter.currentFamiliar.setWeight( KoLCharacter.currentFamiliar.getWeight() + 1 );
		}
	}

	/**
	 * Adds the given familiar to the list of available familiars.
	 *
	 * @param familiar The Id of the familiar to be added
	 */

	public static final FamiliarData addFamiliar( final FamiliarData familiar )
	{
		if ( familiar == null )
		{
			return null;
		}

		int index = KoLCharacter.familiars.indexOf( familiar );
		if ( index >= 0 )
		{
			return (FamiliarData) KoLCharacter.familiars.get( index );
		}

		KoLCharacter.familiars.add( familiar );
		if ( !familiar.getItem().equals( EquipmentRequest.UNEQUIP ) )
		{
			EquipmentManager.processResult( familiar.getItem() );
		}

		return familiar;
	}

	/**
	 * Remove the given familiar from the list of available familiars.
	 *
	 * @param familiar The Id of the familiar to be added
	 */

	public static final void removeFamiliar( final FamiliarData familiar )
	{
		if ( familiar == null )
		{
			return;
		}

		int index = KoLCharacter.familiars.indexOf( familiar );
		if ( index < 0 )
		{
			return;
		}

		if ( KoLCharacter.currentFamiliar == familiar )
		{
			KoLCharacter.currentFamiliar = FamiliarData.NO_FAMILIAR;
			EquipmentManager.setEquipment( EquipmentManager.FAMILIAR, EquipmentRequest.UNEQUIP );
		}

		KoLCharacter.familiars.remove( familiar );
	}

	/**
	 * Returns the list of familiars available to the character.
	 *
	 * @return The list of familiars available to the character
	 */

	public static final LockableListModel getFamiliarList()
	{
		return KoLCharacter.familiars;
	}

	/**
	 * Returns the string used on the character pane to detrmine how many points remain until the character's next
	 * level.
	 *
	 * @return The string indicating the TNP advancement
	 */

	public static final String getAdvancement()
	{
		int level = KoLCharacter.getLevel();
		return KoLConstants.COMMA_FORMAT.format( level * level + 4 - KoLCharacter.calculateBasePoints( KoLCharacter.getTotalPrime() ) ) + " " + AdventureResult.STAT_NAMES[ KoLCharacter.getPrimeIndex() ] + " until level " + ( level + 1 );
	}

	/**
	 * Adds a new <code>KoLCharacterListener</code> to the list of listeners listening to this
	 * <code>KoLCharacter</code>.
	 *
	 * @param listener The listener to be added to the listener list
	 */

	public static final void addCharacterListener( final KoLCharacterListener listener )
	{
		if ( listener != null && !KoLCharacter.listenerList.contains( listener ) )
		{
			KoLCharacter.listenerList.add( listener );
		}
	}

	/**
	 * Removes an existing <code>KoLCharacterListener</code> from the list of listeners listening to this
	 * <code>KoLCharacter</code>.
	 *
	 * @param listener The listener to be removed from the listener list
	 */

	public static final void removeCharacterListener( final KoLCharacterListener listener )
	{
		if ( listener != null )
		{
			KoLCharacter.listenerList.remove( listener );
		}
	}

	/**
	 * Returns the character's zapping wand, if any
	 */

	public static final AdventureResult getZapper()
	{
		// Look for wand

		AdventureResult wand = KoLCharacter.findWand();

		if ( wand != null )
		{
			return wand;
		}

		// None found.  If you've already had a zapper wand this
		// ascension, assume they don't want to use their mimic.

		if ( KoLCharacter.getAscensions() == Preferences.getInteger( "lastZapperWand" ) )
		{
			return null;
		}

		// Use a mimic if one in inventory

		AdventureResult mimic = ItemPool.get( ItemPool.DEAD_MIMIC, 1 );

		if ( !InventoryManager.hasItem( mimic ) )
		{
			return null;
		}

		RequestThread.postRequest( new UseItemRequest( mimic ) );

		// Look for wand again

		return KoLCharacter.findWand();
	}

	public static final AdventureResult findWand()
	{
		for ( int i = 0; i < KoLCharacter.WANDS.length; ++i )
		{
			if ( KoLConstants.inventory.contains( KoLCharacter.WANDS[ i ] ) )
			{
				Preferences.setInteger( "lastZapperWand", KoLCharacter.getAscensions() );
				return KoLCharacter.WANDS[ i ];
			}
		}

		return null;
	}

	public static final boolean hasEquipped( final AdventureResult item, final int equipmentSlot )
	{
		return EquipmentManager.getEquipment( equipmentSlot ).getItemId() == item.getItemId();
	}

	public static final boolean hasEquipped( final AdventureResult item )
	{
		return KoLCharacter.equipmentSlot( item ) != EquipmentManager.NONE;
	}

	public static final boolean hasEquipped( AdventureResult[] equipment, final AdventureResult item, final int equipmentSlot )
	{
		AdventureResult current = equipment[ equipmentSlot ];
		return ( current == null ) ? false : ( current.getItemId() == item.getItemId() );
	}

	public static final boolean hasEquipped( AdventureResult[] equipment, final AdventureResult item )
	{
		switch ( ItemDatabase.getConsumptionType( item.getItemId() ) )
		{
		case KoLConstants.EQUIP_WEAPON:
			return KoLCharacter.hasEquipped( equipment, item, EquipmentManager.WEAPON ) || KoLCharacter.hasEquipped( equipment, item, EquipmentManager.OFFHAND );

		case KoLConstants.EQUIP_OFFHAND:
			return KoLCharacter.hasEquipped( equipment, item, EquipmentManager.OFFHAND );

		case KoLConstants.EQUIP_HAT:
			return KoLCharacter.hasEquipped( equipment, item, EquipmentManager.HAT );

		case KoLConstants.EQUIP_SHIRT:
			return KoLCharacter.hasEquipped( equipment, item, EquipmentManager.SHIRT );

		case KoLConstants.EQUIP_PANTS:
			return KoLCharacter.hasEquipped( equipment, item, EquipmentManager.PANTS );

		case KoLConstants.EQUIP_ACCESSORY:
			return	KoLCharacter.hasEquipped( equipment, item, EquipmentManager.ACCESSORY1 ) ||
				KoLCharacter.hasEquipped( equipment, item, EquipmentManager.ACCESSORY2 ) ||
				KoLCharacter.hasEquipped( equipment, item, EquipmentManager.ACCESSORY3 );

		case KoLConstants.CONSUME_STICKER:
			return	KoLCharacter.hasEquipped( equipment, item, EquipmentManager.STICKER1 ) ||
				KoLCharacter.hasEquipped( equipment, item, EquipmentManager.STICKER2 ) ||
				KoLCharacter.hasEquipped( equipment, item, EquipmentManager.STICKER3 );

		case KoLConstants.EQUIP_FAMILIAR:
			return KoLCharacter.hasEquipped( equipment, item, EquipmentManager.FAMILIAR );
		}

		return false;
	}

	public static final int equipmentSlot( final AdventureResult item )
	{
		switch ( ItemDatabase.getConsumptionType( item.getItemId() ) )
		{
		case KoLConstants.EQUIP_WEAPON:
			return KoLCharacter.hasEquipped( item, EquipmentManager.WEAPON ) ?
				EquipmentManager.WEAPON :
			KoLCharacter.hasEquipped( item, EquipmentManager.OFFHAND ) ?
				EquipmentManager.OFFHAND :
			EquipmentManager.NONE;

		case KoLConstants.EQUIP_OFFHAND:
			return KoLCharacter.hasEquipped( item, EquipmentManager.OFFHAND ) ?
				EquipmentManager.OFFHAND : EquipmentManager.NONE;

		case KoLConstants.EQUIP_HAT:
			return KoLCharacter.hasEquipped( item, EquipmentManager.HAT ) ?
				EquipmentManager.HAT : EquipmentManager.NONE;

		case KoLConstants.EQUIP_SHIRT:
			return KoLCharacter.hasEquipped( item, EquipmentManager.SHIRT ) ?
				EquipmentManager.SHIRT : EquipmentManager.NONE;

		case KoLConstants.EQUIP_PANTS:
			return KoLCharacter.hasEquipped( item, EquipmentManager.PANTS ) ?
				EquipmentManager.PANTS : EquipmentManager.NONE;

		case KoLConstants.EQUIP_ACCESSORY:
			return KoLCharacter.hasEquipped( item, EquipmentManager.ACCESSORY1 ) ?
				EquipmentManager.ACCESSORY1 :
			KoLCharacter.hasEquipped( item, EquipmentManager.ACCESSORY2 ) ?
				EquipmentManager.ACCESSORY2 :
			KoLCharacter.hasEquipped( item, EquipmentManager.ACCESSORY3 ) ?
				EquipmentManager.ACCESSORY3 :
			EquipmentManager.NONE;

		case KoLConstants.CONSUME_STICKER:
			return KoLCharacter.hasEquipped( item, EquipmentManager.STICKER1 ) ?
				EquipmentManager.STICKER1 :
			KoLCharacter.hasEquipped( item, EquipmentManager.STICKER2 ) ?
				EquipmentManager.STICKER2 :
			KoLCharacter.hasEquipped( item, EquipmentManager.STICKER3 ) ?
				EquipmentManager.STICKER3 :
			EquipmentManager.NONE;

		case KoLConstants.EQUIP_FAMILIAR:
			return KoLCharacter.hasEquipped( item, EquipmentManager.FAMILIAR ) ?
				EquipmentManager.FAMILIAR: EquipmentManager.NONE;
		}

		return EquipmentManager.NONE;
	}

	public static final void updateStatus()
	{
		KoLCharacterListener[] listenerArray = new KoLCharacterListener[ KoLCharacter.listenerList.size() ];
		KoLCharacter.listenerList.toArray( listenerArray );

		for ( int i = 0; i < listenerArray.length; ++i )
		{
			listenerArray[ i ].updateStatus();
		}

		// Allow Daily Deeds to change state based on character status
		PreferenceListenerRegistry.firePreferenceChanged( "(character)" );
	}

	public static final void updateSelectedLocation( KoLAdventure location )
	{
		KoLCharacter.selectedLocation = location;
		Modifiers.setLocation( location );
		recalculateAdjustments();
		KoLCharacter.updateStatus();
		PreferenceListenerRegistry.firePreferenceChanged( "(location)" );
	}

	public static final KoLAdventure getSelectedLocation()
	{
		return KoLCharacter.selectedLocation;
	}

	public static final boolean recalculateAdjustments()
	{
		return KoLCharacter.recalculateAdjustments( false );
	}

	public static final boolean recalculateAdjustments( boolean debug )
	{
		return KoLCharacter.currentModifiers.set(
			KoLCharacter.recalculateAdjustments(
				debug,
				KoLCharacter.getMindControlLevel(),
				EquipmentManager.allEquipment(),
				KoLConstants.activeEffects,
				KoLCharacter.currentFamiliar,
				KoLCharacter.currentEnthroned,
				false ) );
	}

	public static final Modifiers recalculateAdjustments( boolean debug, int MCD, AdventureResult[] equipment, List effects, FamiliarData familiar, FamiliarData enthroned,  boolean applyIntrinsics )
	{
		int taoFactor = KoLCharacter.hasSkill( "Tao of the Terrapin" ) ? 2 : 1;

		Modifiers newModifiers = debug ? new DebugModifiers() : new Modifiers();
		Modifiers.setFamiliar( familiar );
		AdventureResult item = equipment[ EquipmentManager.WEAPON ];
		Modifiers.mainhandClass = item == null ? ""
			: EquipmentDatabase.getItemType( item.getItemId() );

		// Area-specific adjustments
		newModifiers.add( Modifiers.getModifiers( "loc:" + Modifiers.currentLocation ) );
		newModifiers.add( Modifiers.getModifiers( "zone:" + Modifiers.currentZone ) );

		// Look at sign-specific adjustments
		newModifiers.add( Modifiers.MONSTER_LEVEL, MCD, "MCD" );

		// Certain outfits give benefits to the character
		// Need to do this before the individual items, so that Hobo Power
		// from the outfit counts towards a Hodgman offhand.
		SpecialOutfit outfit = EquipmentManager.currentOutfit( equipment );
		if ( outfit != null )
		{
			newModifiers.set( Modifiers.OUTFIT, outfit.getName() );
			newModifiers.add( Modifiers.getModifiers( outfit.getName() ) );
			// El Vibrato Relics may have additional benefits based on
			// punchcards inserted into the helmet:
			if ( outfit.getOutfitId() == 41 &&
				Preferences.getInteger( "lastEVHelmetReset" ) == KoLCharacter.getAscensions() )
			{
				int data = Preferences.getInteger( "lastEVHelmetValue" );
				for ( int i = 9; i > 0; --i )
				{
					int level = data % 11;
					data /= 11;
					if ( level > 0 ) switch ( i )
					{
					case 1:
						newModifiers.add( Modifiers.WEAPON_DAMAGE, level * 20, "ATTACK" );
						break;
					case 2:
						newModifiers.add( Modifiers.HP, level * 100, "BUILD" );
						break;
					case 3:
						newModifiers.add( Modifiers.MP, level * 100, "BUFF" );
						break;
					case 4:
						newModifiers.add( Modifiers.MONSTER_LEVEL, level * 10, "MODIFY" );
						break;
					case 5:
						newModifiers.add( Modifiers.HP_REGEN_MIN, level * 16, "REPAIR" );
						newModifiers.add( Modifiers.HP_REGEN_MAX, level * 20, "REPAIR" );
						break;
					case 6:
						newModifiers.add( Modifiers.SPELL_DAMAGE_PCT, level * 10, "TARGET" );
						break;
					case 7:
						newModifiers.add( Modifiers.INITIATIVE, level * 20, "SELF" );
						break;
					case 8:
						if ( Modifiers.currentFamiliar.indexOf( "megadrone" ) != -1 )
						{
							newModifiers.add( Modifiers.FAMILIAR_WEIGHT, level * 10, "DRONE" );
						}
						break;
					case 9:
						newModifiers.add( Modifiers.DAMAGE_REDUCTION, level * 3, "WALL" );
						break;
					}
				}
			}
		}

		// Look at items
		for ( int slot = EquipmentManager.HAT; slot <= EquipmentManager.FAMILIAR + 1; ++slot )
		{
			if ( slot == EquipmentManager.OFFHAND )
			{	// Must do this slot last, since there may be Hobo Power
				// modifiers in all other slots.
				continue;
			}
			else if ( slot == EquipmentManager.FAMILIAR + 1 )
			{	// Deferred offhand
				Modifiers.hoboPower = newModifiers.get( Modifiers.HOBO_POWER );
				item = equipment[ EquipmentManager.OFFHAND ];
			}
			else
			{	// Normal slot
				item = equipment[ slot ];
			}
			if ( item == null || item == EquipmentRequest.UNEQUIP )
			{
				continue;
			}
			int id = item.getItemId();
			boolean onHand = false;
			if ( slot == EquipmentManager.FAMILIAR )
			{
				switch ( ItemDatabase.getConsumptionType( id ) )
				{
				case KoLConstants.EQUIP_HAT:
					// Hatrack hats don't get their normal enchantments
					continue;
				case KoLConstants.EQUIP_WEAPON:
					// Disembodied Hand weapons don't give all enchantments
					onHand = true;
					break;
				}
			}

			String name = item.getName();
			Modifiers imod = Modifiers.getModifiers( name );
			if ( onHand && imod != null )
			{
				Modifiers hand = new Modifiers();
				hand.set( imod );
				hand.set( Modifiers.SLIME_HATES_IT, 0.0f );
				hand.set( Modifiers.BRIMSTONE, 0 );
				hand.set( Modifiers.SYNERGETIC, 0 );
				imod = hand;
				// Possibly cache the modified modifiers?
			}
			if ( applyIntrinsics && imod != null )
			{
				String intrinsic = imod.getString( Modifiers.INTRINSIC_EFFECT );
				if ( intrinsic.length() > 0 )
				{
					newModifiers.add( Modifiers.getModifiers( intrinsic ) );
				}
			}
			newModifiers.add( imod );

			switch ( slot )
			{
			case EquipmentManager.HAT:
				if ( id == ItemPool.HATSEAT )
				{
					newModifiers.add( Modifiers.getModifiers( "Throne:" +
						enthroned.getRace() ) );
				}
				/*FALLTHRU*/
			case EquipmentManager.PANTS:
				newModifiers.add( Modifiers.DAMAGE_ABSORPTION, taoFactor *
					EquipmentDatabase.getPower( item.getItemId() ), "hat/pants power" );
				break;

			case EquipmentManager.SHIRT:
				newModifiers.add( Modifiers.DAMAGE_ABSORPTION,
					EquipmentDatabase.getPower( item.getItemId() ), "shirt power" );
				break;
			}
		}

		// Possibly look at stickers
		if ( EquipmentManager.usingStickerWeapon( equipment ) )
		{
			for ( int slot = EquipmentManager.STICKER1; slot <= EquipmentManager.STICKER3; ++slot )
			{
				item = equipment[ slot ];
				if ( item == null )
				{
					continue;
				}

				newModifiers.add( Modifiers.getModifiers( item.getName() ) );
			}
		}

		int brimstoneMonsterLevel = 1 << newModifiers.getBitmap( Modifiers.BRIMSTONE );
		// Brimstone was believed to affect monster level only if more than
		// one is worn, but this is confirmed to not be true now.
		// Also affects item/meat drop, but only one is needed
		if ( brimstoneMonsterLevel > 1 )
		{
			newModifiers.add( Modifiers.MONSTER_LEVEL, brimstoneMonsterLevel, "brimstone" );
			newModifiers.add( Modifiers.MEATDROP, brimstoneMonsterLevel, "brimstone" );
			newModifiers.add( Modifiers.ITEMDROP, brimstoneMonsterLevel, "brimstone" );
		}

		// Because there are a limited number of passive skills,
		// it is much more efficient to execute one check for
		// each of the known skills.

		newModifiers.applyPassiveModifiers();

		// For the sake of easier maintenance, execute a lot of extra
		// extra string comparisons when looking at status effects.

		for ( int i = 0; i < effects.size(); ++i )
		{
			newModifiers.add( Modifiers.getModifiers(
				( (AdventureResult) effects.get( i ) ).getName() ) );
		}

		// Add modifiers from campground equipment.
		for ( int i = 0; i< KoLConstants.campground.size(); ++i )
		{
			item = (AdventureResult) KoLConstants.campground.get( i );
			// Skip ginormous pumpkin growing in garden
			if ( item.getItemId() == ItemPool.GINORMOUS_PUMPKIN )
			{
				continue;
			}
			String name = item.getName();
			for ( int count = item.getCount(); count > 0; --count )
			{
				newModifiers.add( Modifiers.getModifiers( name ) );
			}
		}

		// Add modifiers from dwelling
		item = CampgroundRequest.getCurrentDwelling();
		newModifiers.add( Modifiers.getModifiers( item.getName() ) );

		if ( KoLConstants.inventory.contains( ItemPool.get( ItemPool.COMFY_BLANKET, 1 ) ) )
		{
			newModifiers.add( Modifiers.getModifiers( "comfy blanket" ) );
		}

		if ( HolidayDatabase.getRonaldPhase() == 5 )
		{
			newModifiers.add( Modifiers.RESTING_MP_PCT, 100, "Ronald full" );
		}

		if ( HolidayDatabase.getGrimacePhase() == 5 )
		{
			newModifiers.add( Modifiers.RESTING_HP_PCT, 100, "Grimace full" );
		}

		// Add other oddball interactions
		newModifiers.applySynergies();

		// Add familiar effects based on calculated weight adjustment.

		newModifiers.applyFamiliarModifiers( familiar,
			equipment[ EquipmentManager.FAMILIAR ] );

		// Add in strung-up quartet.

		if ( KoLCharacter.getAscensions() == Preferences.getInteger( "lastQuartetAscension" ) )
		{
			switch ( Preferences.getInteger( "lastQuartetRequest" ) )
			{
			case 1:
				newModifiers.add( Modifiers.MONSTER_LEVEL, 5, "quartet" );
				break;
			case 2:
				newModifiers.add( Modifiers.COMBAT_RATE, -5, "quartet" );
				break;
			case 3:
				newModifiers.add( Modifiers.ITEMDROP, 5, "quartet" );
				break;
			}
		}

		// Miscellaneous

		newModifiers.add( Modifiers.getModifiers( "_userMods" ) );
		newModifiers.add( Modifiers.getModifiers( "fightMods" ) );

		if ( Modifiers.currentZone.equals( "the slime tube" ) )
		{
			int hatred = (int) newModifiers.get( Modifiers.SLIME_HATES_IT );
			if ( hatred > 0 )
			{
				newModifiers.add( Modifiers.MONSTER_LEVEL,
					Math.min( 1000, 15 * hatred * (hatred + 2) ), "slime hatred" );
			}
		}

		// Lastly, experience adjustment also implicitly depends on
		// monster level.  Add that information.

		float monsterLevel = newModifiers.get( Modifiers.MONSTER_LEVEL );
		newModifiers.add( Modifiers.EXPERIENCE, monsterLevel / 4.0f, "ML/4" );

		float exp = newModifiers.get( Modifiers.EXPERIENCE );
		if ( exp != 0.0f )
		{
			String tuning = newModifiers.getString( Modifiers.STAT_TUNING );
			int prime = KoLCharacter.getPrimeIndex();
			if ( tuning.equals( "Muscle" ) ) prime = 0;
			else if ( tuning.equals( "Mysticality" ) ) prime = 1;
			else if ( tuning.equals( "Moxie" ) ) prime = 2;

			switch ( prime )
			{
			case 0:
				newModifiers.add( Modifiers.MUS_EXPERIENCE, exp / 2.0f, "EXP/2" );
				newModifiers.add( Modifiers.MYS_EXPERIENCE, exp / 4.0f, "EXP/4" );
				newModifiers.add( Modifiers.MOX_EXPERIENCE, exp / 4.0f, "EXP/4" );
				break;
			case 1:
				newModifiers.add( Modifiers.MYS_EXPERIENCE, exp / 2.0f, "EXP/2" );
				newModifiers.add( Modifiers.MUS_EXPERIENCE, exp / 4.0f, "EXP/4" );
				newModifiers.add( Modifiers.MOX_EXPERIENCE, exp / 4.0f, "EXP/4" );
				break;
			case 2:
				newModifiers.add( Modifiers.MOX_EXPERIENCE, exp / 2.0f, "EXP/2" );
				newModifiers.add( Modifiers.MUS_EXPERIENCE, exp / 4.0f, "EXP/4" );
				newModifiers.add( Modifiers.MYS_EXPERIENCE, exp / 4.0f, "EXP/4" );
				break;
			}
		}

		// Determine whether or not data has changed

		if ( debug )
		{
			DebugModifiers.finish();
		}

		return newModifiers;
	}

	// Per-character settings that change each ascension

	public static final void ensureUpdatedDwarfFactory()
	{
		int lastAscension = Preferences.getInteger( "lastDwarfFactoryReset" );
		if ( lastAscension < KoLCharacter.getAscensions() )
		{
			Preferences.setInteger( "lastDwarfFactoryReset", KoLCharacter.getAscensions() );
			Preferences.setString( "lastDwarfDiceRolls", "" );
			Preferences.setString( "lastDwarfDigitRunes", "-------" );
			Preferences.setString( "lastDwarfEquipmentRunes", "" );
			Preferences.setString( "lastDwarfHopper1", "" );
			Preferences.setString( "lastDwarfHopper2", "" );
			Preferences.setString( "lastDwarfHopper3", "" );
			Preferences.setString( "lastDwarfHopper4", "" );
			Preferences.setString( "lastDwarfFactoryItem118", "" );
			Preferences.setString( "lastDwarfFactoryItem119", "" );
			Preferences.setString( "lastDwarfFactoryItem120", "" );
			Preferences.setString( "lastDwarfFactoryItem360", "" );
			Preferences.setString( "lastDwarfFactoryItem361", "" );
			Preferences.setString( "lastDwarfFactoryItem362", "" );
			Preferences.setString( "lastDwarfFactoryItem363", "" );
			Preferences.setString( "lastDwarfFactoryItem364", "" );
			Preferences.setString( "lastDwarfFactoryItem365", "" );
			Preferences.setString( "lastDwarfFactoryItem910", "" );
			Preferences.setString( "lastDwarfFactoryItem3199", "" );
			Preferences.setString( "lastDwarfOfficeItem3208", "" );
			Preferences.setString( "lastDwarfOfficeItem3209", "" );
			Preferences.setString( "lastDwarfOfficeItem3210", "" );
			Preferences.setString( "lastDwarfOfficeItem3211", "" );
			Preferences.setString( "lastDwarfOfficeItem3212", "" );
			Preferences.setString( "lastDwarfOfficeItem3213", "" );
			Preferences.setString( "lastDwarfOfficeItem3214", "" );
			Preferences.setString( "lastDwarfOreRunes", "" );
			DwarfFactoryRequest.reset();
		}
	}

	public static final void ensureUpdatedGuyMadeOfBees()
	{
		int lastAscension = Preferences.getInteger( "lastGuyMadeOfBeesReset" );
		if ( lastAscension < KoLCharacter.getAscensions() )
		{
			Preferences.setInteger( "lastGuyMadeOfBeesReset", KoLCharacter.getAscensions() );
			Preferences.setInteger( "guyMadeOfBeesCount", 0 );
			Preferences.setBoolean( "guyMadeOfBeesDefeated", false );
		}
	}

	public static final void ensureUpdatedSemirareCounter()
	{
		int lastAscension = Preferences.getInteger( "lastSemirareReset" );
		if ( lastAscension < KoLCharacter.getAscensions() )
		{
			Preferences.setInteger( "lastSemirareReset", KoLCharacter.getAscensions() );
			Preferences.setInteger( "semirareCounter", 0 );
		}
	}

	public static final void ensureUpdatedPotionEffects()
	{
		int lastAscension = Preferences.getInteger( "lastBangPotionReset" );
		if ( lastAscension < KoLCharacter.getAscensions() )
		{
			Preferences.setInteger( "lastBangPotionReset", KoLCharacter.getAscensions() );
			for ( int i = 819; i <= 827; ++i )
			{
				Preferences.setString( "lastBangPotion" + i, "" );
			}
			for ( int i = ItemPool.VIAL_OF_RED_SLIME; i <= ItemPool.VIAL_OF_PURPLE_SLIME; ++i )
			{
				Preferences.setString( "lastSlimeVial" + i, "" );
			}
		}

		for ( int i = 819; i <= 827; ++i )
		{
			String testProperty = Preferences.getString( "lastBangPotion" + i );
			if ( !testProperty.equals( "" ) )
			{
				String name = ItemDatabase.getItemName( i );
				String testName = name + " of " + testProperty;
				String testPlural = name + "s of " + testProperty;
				ItemDatabase.registerItemAlias( i, testName, testPlural );
			}
		}

		for ( int i = ItemPool.VIAL_OF_RED_SLIME; i <= ItemPool.VIAL_OF_PURPLE_SLIME; ++i )
		{
			String testProperty = Preferences.getString( "lastSlimeVial" + i );
			if ( !testProperty.equals( "" ) )
			{
				String name = ItemDatabase.getItemName( i );
				String testName = name + ": " + testProperty;
				String testPlural = ItemDatabase.getPluralById( i );
				if ( testPlural != null )
				{
					testPlural = testPlural + ": " + testProperty;
				}
				ItemDatabase.registerItemAlias( i, testName, testPlural );
			}
		}
	}

	private static final void ensureUpdatedSkatePark()
	{
		int lastAscension = Preferences.getInteger( "lastSkateParkReset" );
		if ( lastAscension < KoLCharacter.getAscensions() )
		{
			Preferences.setString( "skateParkStatus", "war" );
			Preferences.setInteger( "lastSkateParkReset", KoLCharacter.getAscensions() );
		}
	}

	public static final void ensureUpdatedSphereEffects()
	{
		int lastAscension = Preferences.getInteger( "lastStoneSphereReset" );
		if ( lastAscension < KoLCharacter.getAscensions() )
		{
			Preferences.setInteger( "lastStoneSphereReset", KoLCharacter.getAscensions() );
			for ( int i = 2174; i <= 2177; ++i )
			{
				Preferences.setString( "lastStoneSphere" + i, "" );
			}
		}

		for ( int i = 2174; i <= 2177; ++i )
		{
			String testProperty = Preferences.getString( "lastStoneSphere" + i );
			if ( !testProperty.equals( "" ) )
			{
				String testName = ItemDatabase.getItemName( i ) + " of " + testProperty;
				ItemDatabase.registerItemAlias( i, testName, null );
			}
		}
	}

	public static final void ensureUpdatedPastaGuardians()
	{
		int lastAscension = Preferences.getInteger( "lastPastamancerGhostReset" );
		if ( lastAscension < KoLCharacter.getAscensions() )
		{
			Preferences.setInteger( "lastPastamancerGhostReset", KoLCharacter.getAscensions() );
			Preferences.setString( "pastamancerGhostType", "" );
			Preferences.setString( "pastamancerGhostName", "" );
			Preferences.setInteger( "pastamancerGhostExperience", 0 );
			Preferences.setString( "pastamancerOrbedType", "" );
			Preferences.setString( "pastamancerOrbedName", "" );
			Preferences.setInteger( "pastamancerOrbedExperience", 0 );
		}
	}

	public static final void ensureUpdatedPirateInsults()
	{
		int lastAscension = Preferences.getInteger( "lastPirateInsultReset" );
		if ( lastAscension < KoLCharacter.getAscensions() )
		{
			Preferences.setInteger( "lastPirateInsultReset", KoLCharacter.getAscensions() );
			Preferences.setBoolean( "lastPirateInsult1", false );
			Preferences.setBoolean( "lastPirateInsult2", false );
			Preferences.setBoolean( "lastPirateInsult3", false );
			Preferences.setBoolean( "lastPirateInsult4", false );
			Preferences.setBoolean( "lastPirateInsult5", false );
			Preferences.setBoolean( "lastPirateInsult6", false );
			Preferences.setBoolean( "lastPirateInsult7", false );
			Preferences.setBoolean( "lastPirateInsult8", false );
		}
	}

	public static final void ensureUpdatedCellar()
	{
		int lastAscension = Preferences.getInteger( "lastCellarReset" );
		if ( lastAscension < KoLCharacter.getAscensions() )
		{
			Preferences.setInteger( "lastCellarReset", KoLCharacter.getAscensions() );
			Preferences.setInteger( "cellarLayout", 0 );
		}
	}
}
