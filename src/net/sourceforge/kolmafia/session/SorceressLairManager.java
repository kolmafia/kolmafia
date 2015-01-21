/**
 * Copyright (c) 2005-2015, KoLmafia development team
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

package net.sourceforge.kolmafia.session;

import java.util.ArrayList;
import java.util.List;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLAdventure;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLConstants.Stat;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.RequestThread;

import net.sourceforge.kolmafia.moods.RecoveryManager;

import net.sourceforge.kolmafia.objectpool.ItemPool;

import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.persistence.MonsterDatabase.Element;
import net.sourceforge.kolmafia.persistence.QuestDatabase;
import net.sourceforge.kolmafia.persistence.QuestDatabase.Quest;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.request.FightRequest;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.request.UseItemRequest;

import net.sourceforge.kolmafia.swingui.CouncilFrame;

import net.sourceforge.kolmafia.utilities.StringUtilities;

public abstract class SorceressLairManager
{
	private static final GenericRequest QUEST_HANDLER = new GenericRequest( "" );

	// Patterns for repeated usage.
	private static final Pattern MAP_PATTERN = Pattern.compile( "usemap=\"#(\\w+)\"" );

	// Items for the tower doorway
	private static final AdventureResult DIGITAL = ItemPool.get( ItemPool.DIGITAL_KEY, 1 );
	private static final AdventureResult STAR_KEY = ItemPool.get( ItemPool.STAR_KEY, 1 );
	private static final AdventureResult SKELETON = ItemPool.get( ItemPool.SKELETON_KEY, 1 );
	private static final AdventureResult BORIS = ItemPool.get( ItemPool.BORIS_KEY, 1 );
	private static final AdventureResult JARLSBERG = ItemPool.get( ItemPool.JARLSBERG_KEY, 1 );
	private static final AdventureResult SNEAKY_PETE = ItemPool.get( ItemPool.SNEAKY_PETE_KEY, 1 );
	private static final AdventureResult UNIVERSAL = ItemPool.get( ItemPool.UNIVERSAL_KEY, 1 );

	private static final AdventureResult KEY_RING = ItemPool.get( ItemPool.SKELETON_KEY_RING, 1 );
	private static final AdventureResult BALLOON = ItemPool.get( ItemPool.BALLOON_MONKEY, 1 );

	// Items for the shadow battle
	private static final AdventureResult [] HEALING_ITEMS = new AdventureResult[]
	{
		ItemPool.get( ItemPool.RED_PIXEL_POTION, 1 ),
		ItemPool.get( ItemPool.FILTHY_POULTICE, 1 ),
		ItemPool.get( ItemPool.GAUZE_GARTER, 1 ),
	};

	// Items for the chamber
	public static final AdventureResult NAGAMAR = ItemPool.get( ItemPool.WAND_OF_NAGAMAR, 1 );

	private static final String[][] CROWD2_DATA =
	{
		// The second crowd: Stat tests
		{
			"standing around flexing their muscles and using grip exercisers",
			"Strongest Adventurer",
			Stat.MUSCLE.toString(),
		},
		{
			"sitting around playing chess and solving complicated-looking logic puzzles",
			"Smartest Adventurer",
			Stat.MYSTICALITY.toString(),
		},
		{
			"all wearing sunglasses and dancing",
			"Smoothest Adventurer",
			Stat.MOXIE.toString(),
		},
	};

	private static final String[][] CROWD3_DATA =
	{
		// The third crowd: Elemental tests
		{
			"people, all of whom appear to be on fire",
			"Hottest Adventurer",
			Element.HOT.toString(),
		},
		{
			"people, clustered around a group of igloos",
			"Coolest Adventurer",
			Element.COLD.toString(),
		},
		{
			"people, surrounded by a cloud of eldritch mist",
			"Spookiest Adventurer",
			Element.SPOOKY.toString(),
		},
		{
			"people, surrounded by garbage and clouds of flies",
			"Stinkiest Adventurer",
			Element.STENCH.toString(),
		},
		{
			"greasy-looking people furtively skulking around",
			"Sleaziest Adventurer",
			Element.SLEAZE.toString(),
		},
	};

	private static final String[][] MAZE_TRAP1_DATA =
	{
		// The first maze trap
		{
			"smoldering bushes on the outskirts of a hedge maze",
			"Hot Damage",
			Element.HOT.toString(),
		},
		{
			"frost-rimed bushes on the outskirts of a hedge maze",
			"Cold Damage",
			Element.COLD.toString(),
		},
		{
			"creepy-looking black bushes on the outskirts of a hedge maze",
			"Spooky Damage",
			Element.SPOOKY.toString(),
		},
		{
			"nasty-looking, dripping green bushes on the outskirts of a hedge maze",
			"Stench Damage",
			Element.STENCH.toString(),
		},
		{
			"purplish, greasy-looking hedges",
			"Sleaze Damage",
			Element.SLEAZE.toString(),
		},
	};

	private static final String[][] MAZE_TRAP2_DATA =
	{
		// The second maze trap
		{
			"smoke rising from deeper within the maze",
			"Hot Damage",
			Element.HOT.toString(),
		},
		{
			"wintry mists rising from deeper within the maze",
			"Cold Damage",
			Element.COLD.toString(),
		},
		{
			"a miasma of eldritch vapors rising from deeper within the maze",
			"Spooky Damage",
			Element.SPOOKY.toString(),
		},
		{
			"a cloud of green gas hovering over the maze",
			"Stench Damage",
			Element.STENCH.toString(),
		},
		{
			"a greasy purple cloud hanging over the center of the maze",
			"Sleaze Damage",
			Element.SLEAZE.toString(),
		},
	};

	private static final String[][] MAZE_TRAP3_DATA =
	{
		// The third maze trap
		{
			"with lava slowly oozing out of it",
			"Hot Damage",
			Element.HOT.toString(),
		},
		{
			"occasionally disgorging a bunch of ice cubes",
			"Cold Damage",
			Element.COLD.toString(),
		},
		{
			"surrounded by creepy black mist",
			"Spooky Damage",
			Element.SPOOKY.toString(),
		},
		{
			"disgorging a really surprising amount of sewage",
			"Stench Damage",
			Element.STENCH.toString(),
		},
		{
			"that occasionally vomits out a greasy ball of hair",
			"Sleaze Damage",
			Element.SLEAZE.toString(),
		},
	};

	public static final int CROWD1 = 0;
	public static final int CROWD2 = 1;
	public static final int CROWD3 = 2;
	public static final int TRAP1 = 3;
	public static final int TRAP2 = 4;
	public static final int TRAP3 = 5;

	public static String[][] challengeToData( final int challenge )
	{
		switch ( challenge )
		{
		case SorceressLairManager.CROWD2:
			return SorceressLairManager.CROWD2_DATA;
		case SorceressLairManager.CROWD3:
			return SorceressLairManager.CROWD3_DATA;
		case SorceressLairManager.TRAP1:
			return SorceressLairManager.MAZE_TRAP1_DATA;
		case SorceressLairManager.TRAP2:
			return SorceressLairManager.MAZE_TRAP2_DATA;
		case SorceressLairManager.TRAP3:
			return SorceressLairManager.MAZE_TRAP3_DATA;
		default:
			return null;
		}
	}
	
	public static void parseChallenge( final int challenge, final String test, final String setting1, final String setting2 )
	{
		Preferences.setString( setting1, test );
		Preferences.setString( setting2, "none" );

		String[][] data = SorceressLairManager.challengeToData( challenge );
		if ( data == null )
		{
			return;
		}

		for ( String [] entry : data )
		{
			if ( test.equals( entry[ 0 ] ) )
			{
				Preferences.setString( setting2, entry[ 2 ] );
				return;
			}
		}
	}

	public static String getChallengeName( final int challenge )
	{
		switch ( challenge )
		{
		case SorceressLairManager.CROWD1:
			return "Crowd #1";
		case SorceressLairManager.CROWD2:
			return "Crowd #2";
		case SorceressLairManager.CROWD3:
			return "Crowd #3";
		case SorceressLairManager.TRAP1:
			return "Maze Trap #1";
		case SorceressLairManager.TRAP2:
			return "Maze Trap #2";
		case SorceressLairManager.TRAP3:
			return "Maze Trap #3";
		default:
			return "Unknown Challenge";
		}
	}
	
	public static String getChallengeDescription( final int challenge, final String test )
	{
		if ( challenge == SorceressLairManager.CROWD1)
		{
			return "Fastest Adventurer";
		}

		String[][] data = SorceressLairManager.challengeToData( challenge );
		if ( data == null )
		{
			return "(bogus)";
		}

		for ( String [] entry : data )
		{
			if ( test.equals( entry[ 2 ] ) )
			{
				return entry[ 1 ];
			}
		}

		return "(" + test + ")";
	}

	private static final Pattern STAT_ADVENTURER_PATTERN = Pattern.compile( "(Strongest|Smartest|Smoothest) Adventurer contest" );
	private static final Pattern ELEMENT_ADVENTURER_PATTERN = Pattern.compile( "(Hottest|Coldest|Spookiest|Stinkiest|Sleaziest) Adventurer contest" );

	public static void parseContestBooth( final String responseText )
	{
		// You feel <feeling> about your chances in the <attribute> Adventurer contest.
		//
		// According to my evaluation, you qualify to start at rank <b>#N</b> in the <attribute> Adventurer contest.
		//
		// The man wraps a measuring tape around various parts of your body and declares that you are qualified to begin the contest at rank <b>#N</b>.
		//
		// The man peers at you through a magnifying glass for a little while, then writes <b>#N</b> on his clipboard.
		//
		// "You already entered the <attribute> Adventurer contest. You
		// should go get in line and wait for it to start. My clipboard
		// here says that there are X Adventurers in the contest
		// besides you."
		//
		// "You already entered the <attribute> Adventurer contest. You
		// should go get in line and wait for it to start. Wait -- my
		// clipboard says that you're the only Adventurer who
		// entered. That can't be right, can it? Well, if it's true,
		// then I guess you're definitely going to win!"
		//
		// "You already entered the <attribute> Adventurer contest. You
		// should go wait in line with the other Adventurers. Actually,
		// wait -- it says here on my clipboard that you're the only
		// entrant, so I guess you win that one by default."

		// Look at the responseText and set nsChallenge1 to the "stat"
		// and nsChallenge2 to the "element"

		Matcher matcher = SorceressLairManager.STAT_ADVENTURER_PATTERN.matcher( responseText );
		if ( matcher.find() )
		{
			String stat = matcher.group( 1 );
			String  value =
				stat.equals( "Strongest" ) ?
				Stat.MUSCLE.toString() :
				stat.equals( "Smartest" ) ?
				Stat.MYSTICALITY.toString() :
				stat.equals( "Smoothest" ) ?
				Stat.MOXIE.toString() :
				"none";
			Preferences.setString( "nsChallenge1", value );
		}

		matcher = SorceressLairManager.ELEMENT_ADVENTURER_PATTERN.matcher( responseText );
		if ( matcher.find() )
		{
			String element = matcher.group( 1 );
			String value =
				element.equals( "Hottest" ) ?
				Element.HOT.toString() :
				element.equals( "Coldest" ) ?
				Element.COLD.toString() :
				element.equals( "Spookiest" ) ?
				Element.SPOOKY.toString() :
				element.equals( "Stinkiest" ) ?
				Element.STENCH.toString() :
				element.equals( "Sleaziest" ) ?
				Element.SLEAZE.toString() :
				"none";
			Preferences.setString( "nsChallenge2", value );
		}
	}

	public static void parseDoorResponse( final String location, final String responseText )
	{
		String action = GenericRequest.getAction( location );

		if ( action == null )
		{
			return;
		}

		AdventureResult lock =
			action.equals( "ns_lock1" ) ?
			SorceressLairManager.BORIS :
			action.equals( "ns_lock2" ) ?
			SorceressLairManager.JARLSBERG :
			action.equals( "ns_lock3" ) ?
			SorceressLairManager.SNEAKY_PETE :
			action.equals( "ns_lock4" ) ?
			SorceressLairManager.STAR_KEY :
			action.equals( "ns_lock5" ) ?
			SorceressLairManager.SKELETON :
			action.equals( "ns_lock6" ) ?
			SorceressLairManager.DIGITAL :
			null;

		if ( lock == null )
		{
			return;
		}

		AdventureResult key =
			responseText.contains( "universal key" ) ?
			SorceressLairManager.UNIVERSAL :
			lock;

		// You place Boris's key in the lock and turn it. You hear a
		// jolly bellowing in the distance as the lock vanishes, along
		// with the metal plate it was attached to. Huh.

		// You put Jarlsberg's key in the lock and turn it. You hear a
		// nasal, sort of annoying laugh in the distance as the lock
		// vanishes in a puff of rotten-egg-smelling smoke.

		// You put the key in the lock and hear the roar of a
		// motorcycle behind you. By the time you turn around to check
		// out the cool motorcycle guy he's gone, but when you turn
		// back to the lock it is <i>also</i> gone.

		// You put the key in and turn it. There is a flash of
		// brilliant starlight accompanied by a competent but not
		// exceptional drum solo, and when both have faded, the lock is
		// gone.

		// You put the skeleton key in the lock and turn it. The key,
		// the lock, and the metal plate the lock is attached to all
		// crumble to dust. And rust, in the case of the metal.

		// You put the digital key in the lock and turn it. A familiar
		// sequence of eight tones plays as the lock disappears.

		if ( responseText.contains( "the lock vanishes" ) ||
		     responseText.contains( "turn back to the lock" ) ||
		     responseText.contains( "the lock is gone" ) ||
		     responseText.contains( "crumble to dust" ) ||
		     responseText.contains( "the lock disappears" ) )
		{
			ResultProcessor.processResult( key.getNegation() );
			String keys = Preferences.getString( "nsTowerDoorKeysUsed" );
			Preferences.setString( "nsTowerDoorKeysUsed", keys + ( keys.equals( "" ) ? "" : "," ) + lock.getDataName() );
		}
	}

	public static boolean registerRequest( final String urlString )
	{
		if ( !urlString.startsWith( "place.php" ) )
		{
			return false;
		}

		String prefix = "[" + KoLAdventure.getAdventureCount() + "] ";
		String message = null;

		if ( urlString.contains( "whichplace=nstower_door" ) )
		{
			String action = GenericRequest.getAction( urlString );
			if ( action == null )
			{
				RequestLogger.printLine();
				RequestLogger.updateSessionLog();
				message =  prefix + "Tower Door";
			}
			else
			{
				message =
					action.equals( "ns_lock1" ) ? "Tower Door: Boris's lock" :
					action.equals( "ns_lock2" ) ? "Tower Door: Jarlsberg's lock" :
					action.equals( "ns_lock3" ) ? "Tower Door: Sneaky Pete's's lock" :
					action.equals( "ns_lock4" ) ? "Tower Door: star lock" :
					action.equals( "ns_lock5" ) ? "Tower Door: skeleton's lock" :
					action.equals( "ns_lock6" ) ? "Tower Door: digital's lock" :
					action.equals( "ns_doorknob" ) ? "Tower Door: doorknob" :
					null;
			}
		}
		else if ( urlString.contains( "whichplace=nstower" ) )
		{
			String action = GenericRequest.getAction( urlString );
			if ( action != null )
			{
				message =
					action.equals( "ns_01_contestbooth" ) ? "Tower: Contest Booth" :
					action.equals( "ns_02_coronation" ) ? "Tower: Closing Ceremony" :
					action.equals( "ns_11_prism" ) ? "Tower: Freeing King Ralph" :
					null;

				if ( message == null )
				{
					return false;
				}

				RequestLogger.printLine();
				RequestLogger.updateSessionLog();
			}
		}
		else if ( urlString.startsWith( "choice.php" ) )
		{
		}

		if ( message == null )
		{
			return false;
		}

		RequestLogger.printLine( message );
		RequestLogger.updateSessionLog( message );

		return true;
	}

	public static boolean registerChoice( final int choice, final String urlString )
	{
		Matcher matcher = ChoiceManager.URL_OPTION_PATTERN.matcher( urlString );
		int option =  matcher.find() ? StringUtilities.parseInt( matcher.group( 1 ) ) : 0;
		String message = null;

		switch ( choice )
		{
		case 1003:	// Test Your Might And Also Test Other Things
			if ( option >= 1 && option <= 3 )
			{
				int challenge = option - 1;
				String test = challenge == 0 ? "" : Preferences.getString( "nsChallenge" + challenge );
				String description = SorceressLairManager.getChallengeDescription( challenge, test );
				message = "Registering for the " + description + " Contest";
			}
			else if ( option == 4 )
			{
				message = "Claiming your prize";
			}
			else
			{
				return true;
			}
			RequestLogger.printLine( message );
			RequestLogger.updateSessionLog( message );
			return true;
		case 1015:	// The Mirror in the Tower has the View that is True
		case 1020:	// Closing Ceremony
		case 1021:	// Meet Frank
		case 1022:	// Meet Frank
			return true;
		case 1005:	// 'Allo
		case 1006:	// One Small Step For Adventurer
		case 1007:	// Twisty Little Passages, All Hedge
		case 1008:	// Pooling Your Resources
		case 1009:	// Good Ol' 44% Duck
		case 1010:	// Another Day, Another Fork
		case 1011:	// Of Mouseholes and Manholes
		case 1012:	// The Last Temptation
		case 1013:	// Mazel Tov!
			// Don't log the URL for these; the encounter suffices
			return true;
		}
		return false;
	}

	public static void visitChoice( final int choice, final String responseText )
	{
		switch ( choice )
		{
		case 1005:	// 'Allo
		case 1006:	// One Small Step For Adventurer
		case 1007:	// Twisty Little Passages, All Hedge
		case 1008:	// Pooling Your Resources
		case 1009:	// Good Ol' 44% Duck
		case 1010:	// Another Day, Another Fork
		case 1011:	// Of Mouseholes and Manholes
		case 1012:	// The Last Temptation
		case 1013:	// Mazel Tov!
			Preferences.setInteger( "currentHedgeMazeRoom", choice - 1004 );
			break;
		}
	}

	// *** Here follow obsolete methods
	
	public static final void completeEntryway()
	{
	}

	public static final void completeHedgeMaze()
	{
	}

	public static final int fightAllTowerGuardians()
	{
		return -1;
	}

	public static final int fightMostTowerGuardians()
	{
		return -1;
	}

	private static final int fightTowerGuardians( boolean fightShadow )
	{
		// Disable automation while Form of... Bird! is active,
		// as it disables item usage.

		if ( KoLConstants.activeEffects.contains( FightRequest.BIRDFORM ) )
		{
			return -1;
		}

		// You must have at least 70 in all stats before you can enter
		// the chamber.

		if ( KoLCharacter.getBaseMuscle() < 70 || KoLCharacter.getBaseMysticality() < 70 || KoLCharacter.getBaseMoxie() < 70 )
		{
			KoLmafia.updateDisplay(
				MafiaState.ERROR, "You can't enter the chamber unless all base stats are 70 or higher." );
			return -1;
		}

		return -1;
	}

	private static final void fightShadow()
	{
		RecoveryManager.recoverHP( KoLCharacter.getMaximumHP() );
		if ( !KoLmafia.permitsContinue() )
		{
			return;
		}

		int itemCount = 0;

		for ( int i = 0; i < SorceressLairManager.HEALING_ITEMS.length; ++i )
		{
			itemCount += SorceressLairManager.HEALING_ITEMS[ i ].getCount( KoLConstants.inventory );
		}

		if ( itemCount < 6 )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "Insufficient healing items to continue." );
			return;
		}

		KoLmafia.updateDisplay( "Fighting your shadow..." );

		// Start the battle!

		RequestThread.postRequest( SorceressLairManager.QUEST_HANDLER.constructURLString( "lair6.php?place=2" ) );
		if ( !KoLmafia.permitsContinue() )
		{
			return;
		}

		if ( SorceressLairManager.QUEST_HANDLER.responseText.indexOf( "You don't have time to mess around up here." ) != -1 )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "You're out of adventures." );
			return;
		}

		String continueString = Preferences.getBoolean( "serverAddsCustomCombat" ) ? "(show old combat form)" : "action=fight.php";
		int itemIndex = 0;

		do
		{
			SorceressLairManager.QUEST_HANDLER.constructURLString( "fight.php" );

			while ( !KoLConstants.inventory.contains( SorceressLairManager.HEALING_ITEMS[ itemIndex ] ) )
			{
				++itemIndex;
			}

			SorceressLairManager.QUEST_HANDLER.addFormField( "action", "useitem" );
			SorceressLairManager.QUEST_HANDLER.addFormField( "whichitem", String.valueOf( SorceressLairManager.HEALING_ITEMS[ itemIndex ].getItemId() ) );

			if ( KoLCharacter.hasSkill( "Ambidextrous Funkslinging" ) )
			{
				boolean needsIncrement =
					!KoLConstants.inventory.contains( SorceressLairManager.HEALING_ITEMS[ itemIndex ] ) || SorceressLairManager.HEALING_ITEMS[ itemIndex ].getCount( KoLConstants.inventory ) < 2;

				if ( needsIncrement )
				{
					++itemIndex;
					while ( !KoLConstants.inventory.contains( SorceressLairManager.HEALING_ITEMS[ itemIndex ] ) )
					{
						++itemIndex;
					}
				}

				SorceressLairManager.QUEST_HANDLER.addFormField(
					"whichitem2", String.valueOf( SorceressLairManager.HEALING_ITEMS[ itemIndex ].getItemId() ) );
			}

			RequestThread.postRequest( SorceressLairManager.QUEST_HANDLER );
		}
		while ( SorceressLairManager.QUEST_HANDLER.responseText.contains( continueString ) );

		if ( SorceressLairManager.QUEST_HANDLER.responseText.contains( "<!--WINWINWIN-->" ) )
		{
			KoLmafia.updateDisplay( "Your shadow has been defeated." );
		}
		else
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "Unable to defeat your shadow." );
		}
	}

	/*
	 * Methods to decorate lair pages for the Relay Browser
	 */

	public static void handleQuestChange( String location, String responseText )
	{
		// lair.php and lair1-6.php all can check for the same things.
		// Work backwards from the end to see what zones are unlocked.
		// King deprismed
		if ( responseText.contains( "gash.gif" ) )
		{
			QuestDatabase.setQuestProgress( Quest.FINAL, QuestDatabase.FINISHED );
		}
		// Naughty Sorceress defeated
		else if ( responseText.contains( "kingprism1.gif" ) )
		{
			QuestDatabase.setQuestIfBetter( Quest.FINAL, "step16" );
		}

		// step13 and step14 were the 2 familiars contests. Those are
		// now gone. We could remove those steps from questslog.txt and
		// make the final battle with Her Naughtiness be step13 and the
		// emprismed king be step14, but for backward compatibility
		// (for now), defeating the shadow takes you to step15.
		//
		// Comment: In Avatar of Boris, Clancy replaces the familiars -
		// and he still exists, even though the familiars are gone. I
		// don't think we have the questlog text for that step.

		// Shadow (or Clancy?) defeated
		else if ( responseText.contains( "chamber5.gif" ) )
		{
			QuestDatabase.setQuestIfBetter( Quest.FINAL, "step15" );
		}
		// Deflect energy with mirror shard
		else if ( responseText.contains( "chamber2.gif" ) )
		{
			QuestDatabase.setQuestIfBetter( Quest.FINAL, "step12" );
		}
		// Open Heavy door
		else if ( responseText.contains( "chamber1.gif" ) )
		{
			QuestDatabase.setQuestIfBetter( Quest.FINAL, "step11" );
		}
		// Lower Monster 6 defeated
		else if ( responseText.contains( "chamber0.gif" ) )
		{
			QuestDatabase.setQuestIfBetter( Quest.FINAL, "step10" );
		}
		// Lower Monster 5 defeated
		else if ( responseText.contains( "tower6.gif" ) )
		{
			QuestDatabase.setQuestIfBetter( Quest.FINAL, "step9" );
		}
		// Lower Monster 4 defeated
		else if ( responseText.contains( "tower5.gif" ) )
		{
			QuestDatabase.setQuestIfBetter( Quest.FINAL, "step8" );
		}
		// Lower Monster 3 defeated
		else if ( responseText.contains( "tower4.gif" ) )
		{
			QuestDatabase.setQuestIfBetter( Quest.FINAL, "step7" );
		}
		// Lower Monster 2 defeated
		else if ( responseText.contains( "tower3.gif" ) )
		{
			QuestDatabase.setQuestIfBetter( Quest.FINAL, "step6" );
		}
		// Lower Monster 1 defeated
		else if ( responseText.contains( "tower2.gif" ) )
		{
			QuestDatabase.setQuestIfBetter( Quest.FINAL, "step5" );
		}
		// Maze completed
		else if ( responseText.contains( "gate squeaks open" ) || responseText.contains( "tower1.gif" ) )
		{
			QuestDatabase.setQuestIfBetter( Quest.FINAL, "step4" );
		}
		// Cave done
		else if ( responseText.contains( "cave22done.gif" ) )
		{
			QuestDatabase.setQuestIfBetter( Quest.FINAL, "step3" );
		}
		// Huge mirror broken
		else if ( responseText.contains( "cave1mirrordone" ) )
		{
			QuestDatabase.setQuestIfBetter( Quest.FINAL, "step2" );
		}
		// Passed the three gates
		else if ( responseText.contains( "cave1mirror.gif" ) )
		{
			QuestDatabase.setQuestIfBetter( Quest.FINAL, "step1" );
		}
	}
}
