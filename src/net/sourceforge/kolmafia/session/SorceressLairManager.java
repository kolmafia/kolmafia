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

import net.sourceforge.kolmafia.objectpool.EffectPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;

import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.persistence.MonsterDatabase.Element;
import net.sourceforge.kolmafia.persistence.QuestDatabase;
import net.sourceforge.kolmafia.persistence.QuestDatabase.Quest;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.request.FightRequest;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.request.PlaceRequest;
import net.sourceforge.kolmafia.request.UseItemRequest;

import net.sourceforge.kolmafia.swingui.CouncilFrame;

import net.sourceforge.kolmafia.utilities.StringUtilities;

public abstract class SorceressLairManager
{
	private static final GenericRequest QUEST_HANDLER = new GenericRequest( "" );

	// Patterns for repeated usage.
	private static final Pattern MAP_PATTERN = Pattern.compile( "usemap=\"#(\\w+)\"" );

	// Items for the tower doorway
	private static final AdventureResult DIGITAL_KEY = ItemPool.get( ItemPool.DIGITAL_KEY, 1 );
	private static final AdventureResult STAR_KEY = ItemPool.get( ItemPool.STAR_KEY, 1 );
	private static final AdventureResult SKELETON_KEY = ItemPool.get( ItemPool.SKELETON_KEY, 1 );
	private static final AdventureResult BORIS_KEY = ItemPool.get( ItemPool.BORIS_KEY, 1 );
	private static final AdventureResult JARLSBERG_KEY = ItemPool.get( ItemPool.JARLSBERG_KEY, 1 );
	private static final AdventureResult SNEAKY_PETE_KEY = ItemPool.get( ItemPool.SNEAKY_PETE_KEY, 1 );
	private static final AdventureResult UNIVERSAL_KEY = ItemPool.get( ItemPool.UNIVERSAL_KEY, 1 );

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
	private static final AdventureResult CONFIDENCE = EffectPool.get( "Confidence!", Integer.MAX_VALUE );
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

	private static final Object[][] LOCK_DATA =
	{
		{
			SorceressLairManager.BORIS_KEY,
			"ns_lock1",
		},
		{
			SorceressLairManager.JARLSBERG_KEY,
			"ns_lock2",
		},
		{
			SorceressLairManager.SNEAKY_PETE_KEY,
			"ns_lock3",
		},
		{
			SorceressLairManager.STAR_KEY,
			"ns_lock4",
		},
		{
			SorceressLairManager.SKELETON_KEY,
			"ns_lock5",
		},
		{
			SorceressLairManager.DIGITAL_KEY,
			"ns_lock6",
		},
	};

	public static AdventureResult lockToKey( final String lock )
	{
		for ( Object [] row : SorceressLairManager.LOCK_DATA )
		{
			if ( lock.equals( (String) row[ 1 ] ) )
			{
				return (AdventureResult) row[ 0 ];
			}
		}
		return null;
	}

	public static String keyToLock( final AdventureResult key )
	{
		return SorceressLairManager.keyToLock( key.getName() );
	}

	public static String keyToLock( final String keyName )
	{
		for ( Object [] row : SorceressLairManager.LOCK_DATA )
		{
			AdventureResult key = (AdventureResult) row[ 0 ];
			if ( keyName.equals( key.getName() ) )
			{
				return (String) row[ 1 ];
			}
		}
		return null;
	}

	private static final Pattern RANK_PATTERN = Pattern.compile( "<b>#(\\d+)</b>" );
	private static final Pattern OPTIMISM_PATTERN = Pattern.compile( "You feel (.*?) about your chances in the (.*?) Adventurer contest" );
	private static final Pattern ENTERED_PATTERN = Pattern.compile( "&quot;You already entered the (.*?) Adventurer contest.*?&quot;", Pattern.DOTALL );
	private static final Pattern QUEUED_PATTERN = Pattern.compile( "there are (\\d+) (Adventurers|other Adventurers|of them)" );

	private static String contestToStat( final String contest )
	{
		return  contest.equals( "Strongest" ) ?
			Stat.MUSCLE.toString() :
			contest.equals( "Smartest" ) ?
			Stat.MYSTICALITY.toString() :
			contest.equals( "Smoothest" ) ?
			Stat.MOXIE.toString() :
			null;
	}

	private static String contestToElement( final String contest )
	{
		return  contest.equals( "Hottest" ) ?
			Element.HOT.toString() :
			contest.equals( "Coldest" ) ?
			Element.COLD.toString() :
			contest.equals( "Spookiest" ) ?
			Element.SPOOKY.toString() :
			contest.equals( "Stinkiest" ) ?
			Element.STENCH.toString() :
			contest.equals( "Sleaziest" ) ?
			Element.SLEAZE.toString() :
			null;
	}

	public static void parseContestBooth( final int decision, final String responseText )
	{
		if ( decision == 4 )
		{
			// Claim your prize
			if ( responseText.contains( "World's Best Adventurer sash" ) )
			{
				QuestDatabase.setQuestProgress( Quest.FINAL, "step1" );
			}
			return;
		}

		QuestDatabase.setQuestIfBetter( Quest.FINAL, QuestDatabase.STARTED );

		// Are we entering a contest?
		if ( decision >= 1 && decision <= 3 )
		{
			// According to my evaluation, you qualify to start at rank
			// <b>#N</b> in the <attribute> Adventurer contest.
			//
			// The man wraps a measuring tape around various parts of your
			// body and declares that you are qualified to begin the
			// contest at rank <b>#N</b>.
			//
			// The man peers at you through a magnifying glass for a little
			// while, then writes <b>#N</b> on his clipboard.

			Matcher rankMatcher = SorceressLairManager.RANK_PATTERN.matcher( responseText );
			if ( rankMatcher.find() )
			{
				String setting = "nsContestants" + String.valueOf( decision );
				int value = StringUtilities.parseInt( rankMatcher.group( 1 ) );
				Preferences.setInteger( setting, value - 1 );
			}
			return;
		}

		// You feel <feeling> about your chances in the <attribute> Adventurer contest.

		Matcher optimismMatcher = SorceressLairManager.OPTIMISM_PATTERN.matcher( responseText );
		while ( optimismMatcher.find() )
		{
			String mood = optimismMatcher.group( 1 );
			String contest = optimismMatcher.group( 2 );

			if ( contest.equals( "Fastest" ) )
			{
				continue;
			}

			String stat = SorceressLairManager.contestToStat( contest );
			if ( stat != null )
			{
				Preferences.setString( "nsChallenge1", stat );
				continue;
			}

			String element = SorceressLairManager.contestToElement( contest );
			if ( element != null )
			{
				Preferences.setString( "nsChallenge2", element );
				continue;
			}
		}

		// "You already entered the <attribute> Adventurer contest. You
		// should go get in line and wait for it to start. My clipboard
		// here says that there are X Adventurers in the contest
		// besides you."
		//
		// "You already entered the <attribute> Adventurer contest. You
		// should go wait with the other entrants. According to my
		// clipboard here, there are X other Adventurers waiting."
		//
		// "You already entered the <attribute> Adventurer contest. You
		// should go wait in line with the other Adventurers. It says
		// here that there are X of them besides you"
		//
		// "You already entered the <attribute> Adventurer contest.  You
		// should go get in line and wait for it to start.  It says
		// here that the contest is current you and one other
		// Adventurer.  Hey, a 50/50 chance is pretty good, eh?"
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

		Matcher enteredMatcher = SorceressLairManager.ENTERED_PATTERN.matcher( responseText );
		while ( enteredMatcher.find() )
		{
			String contest = enteredMatcher.group( 1 );
			String text = enteredMatcher.group( 0 );
			Matcher queuedMatcher = SorceressLairManager.QUEUED_PATTERN.matcher( text );
			int queue =
				queuedMatcher.find() ?
				StringUtilities.parseInt( queuedMatcher.group( 1 ) ) :
				text.contains( "you and one other" ) ?
				1 :
				text.contains( "only Adventurer" ) || text.contains( "only entrant" ) ?
				0 :
				-1;

			if ( contest.equals( "Fastest" ) )
			{
				Preferences.setInteger( "nsContestants1", queue );
				continue;
			}

			String stat = SorceressLairManager.contestToStat( contest );
			if ( stat != null )
			{
				Preferences.setInteger( "nsContestants2", queue );
				Preferences.setString( "nsChallenge1", stat );
				continue;
			}

			String element = SorceressLairManager.contestToElement( contest );
			if ( element != null )
			{
				Preferences.setInteger( "nsContestants3", queue );
				Preferences.setString( "nsChallenge2", element );
				continue;
			}
		}
	}

	public static void parseMazeTrap( final int choice, final String responseText )
	{
		// If we experienced the effect of a maze trap, remember the
		// element.

		String setting =
			choice == 1005 ?	// 'Allo
			"nsChallenge3" :
			choice == 1008 ?	// Pooling Your Resources
			"nsChallenge4" :
			choice == 1011 ?	// Of Mouseholes and Manholes
			"nsChallenge5" :
			null;

		if ( setting == null )
		{
			return;
		}

		String element =
			responseText.contains( "hot damage" ) ?
			Element.HOT.toString() :
			responseText.contains( "cold damage" ) ?
			Element.COLD.toString() :
			responseText.contains( "spooky damage" ) ?
			Element.SPOOKY.toString() :
			responseText.contains( "stench damage" ) ?
			Element.STENCH.toString() :
			responseText.contains( "sleaze damage" ) ?
			Element.SLEAZE.toString() :
			null;

		if ( element == null )
		{
			return;
		}

		Preferences.setString( setting, element );
	}

	public static void parseTowerResponse( final String action, final String responseText )
	{
		// King Ralph the XI stands before you in all his regal glory.
		// "I'm sorry, adventurer," he says, "but the king is in
		// another castle." Then he breaks into a hearty chuckle. "Well
		// done, adventurer! You laid the smack down on that skank with
		// admirable derring-do and panache. I am eternally in your debt."

		if ( action.equals( "ns_11_prism" ) &&
		     responseText.contains( "King Ralph the XI stands before you in all his regal glory" ) )
		{
			// Freeing the king finishes the quest.
			QuestDatabase.setQuestProgress( Quest.FINAL, QuestDatabase.FINISHED );
			KoLCharacter.liberateKing();
			return;
		}

		// Other "actions" all redirect to a choice or fight, if they
		// are allowed, otherwise simply return the tower image, as
		// does a request to see the tower with no action. Parse the
		// image and see what we can glean.

		SorceressLairManager.parseTower( responseText );
	}

	public static void parseTowerDoorResponse( final String action, final String responseText )
	{
		if ( action == null || action.equals( "" ) )
		{
			SorceressLairManager.parseTowerDoor( responseText );
			return;
		}

		if ( action.equals( "ns_doorknob" ) )
		{
			// You turn the knob and the door vanishes. I guess it was made out of the same material as those weird lock plates.
			if ( responseText.contains( "You turn the knob and the door vanishes" ) )
			{
				QuestDatabase.setQuestProgress( Quest.FINAL, "step4" );
			}
			return;
		}

		AdventureResult lock = SorceressLairManager.lockToKey( action );

		if ( lock == null )
		{
			return;
		}

		AdventureResult key =
			responseText.contains( "universal key" ) ?
			SorceressLairManager.UNIVERSAL_KEY :
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

	public static void parseTowerDoor( String responseText )
	{
		// Based on which locks are absent, deduce which keys have been used.

		StringBuilder buffer = new StringBuilder();
		int keys = 0;

		for ( Object [] row : SorceressLairManager.LOCK_DATA )
		{
			String lock = (String) row[ 1 ];
			if ( !responseText.contains( lock ) )
			{
				AdventureResult key = (AdventureResult) row[ 0 ];
				buffer.append( keys++ > 0 ? "," : "" );
				buffer.append( key.getDataName() );
			}
		}

		Preferences.setString( "nsTowerDoorKeysUsed", buffer.toString() );
	}

	public static boolean registerRequest( final String urlString )
	{
		if ( !urlString.startsWith( "place.php" ) )
		{
			return false;
		}

		String place = GenericRequest.getPlace( urlString );
		if ( place == null )
		{
			return false;
		}

		String message = null;

		if ( place.equals( "nstower" ) )
		{
			String action = GenericRequest.getAction( urlString );

			if ( action == null )
			{
				// Nothing to log for simply looking at the tower.
				return true;
			}

			if ( action.equals( "ns_10_sorcfight" ) )
			{
				// You are about to confront Her Naughtiness
				// Clear effects other than Confidence!
				SorceressLairManager.enterSorceressFight();

				// Let KoLAdventure claim this
				return false;
			}

			if ( action.equals( "ns_03_hedgemaze" ) )
			{
				// The hedgemaze is an adventure location.
				// However, visiting this place/action
				// will redirect to a choice and we
				// will log it with room number.
				//
				// Therefore, claim this and defer logging.
				return true;
			}

			message =
				action.equals( "ns_01_contestbooth" ) ? "Tower: Contest Booth" :
				action.equals( "ns_02_coronation" ) ? "Tower: Closing Ceremony" :
				action.equals( "ns_11_prism" ) ? "Tower: Freeing King Ralph" :
				null;

			if ( message == null )
			{
				// Everything else is a KoLAdventure
				return false;
			}

			RequestLogger.printLine();
			RequestLogger.updateSessionLog();
		}
		else if ( place.equals( "nstower_door" ) )
		{
			String action = GenericRequest.getAction( urlString );
			if ( action == null )
			{
				String prefix = "[" + KoLAdventure.getAdventureCount() + "] ";
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
		else
		{
			// Let any other "place" be claimed by other classes.
			return false;
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
			// Don't log the URL for these; the encounter suffices
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

	public static void enterSorceressFight()
	{
		// We retain (some) intrinsic effects. In particular, Confidence!
		boolean isConfident = KoLConstants.activeEffects.contains( SorceressLairManager.CONFIDENCE );
		KoLConstants.activeEffects.clear();
		if ( isConfident )
		{
			KoLConstants.activeEffects.add( SorceressLairManager.CONFIDENCE );
		}
	}

	private static final String[][] TOWER_DATA =
	{
		{
			"nstower_regdesk.gif",
			QuestDatabase.STARTED,
		},
		{
			"nstower_courtyard.gif",
			"step1",
		},
		{
			"nstower_hedgemaze.gif",
			"step2",
		},
		{
			"nstower_towerdoor.gif",
			"step3",
		},
		{
			"nstower_tower1.gif",
			"step4",
		},
		{
			"nstower_tower2.gif",
			"step5",
		},
		{
			"nstower_tower3.gif",
			"step6",
		},
		{
			"nstower_tower4.gif",
			"step7",
		},
		{
			"nstower_tower5.gif",
			"step8",
		},
		{
			"chamberlabel.gif",
			"step9",
		},
		{
			"kingprism",
			"step10",
		},
		{
			"gash.gif",
			QuestDatabase.FINISHED,
		},
	};

	public static void parseTower( String responseText )
	{
		String step = QuestDatabase.UNSTARTED;

		for ( String[] spot : SorceressLairManager.TOWER_DATA )
		{
			if ( responseText.contains( spot[0] ) )
			{
				step = spot[1];
				break;
			}
		}

		QuestDatabase.setQuestProgress( Quest.FINAL, step );

		// If step is "step1", the following images might or might not
		// be present
		//
		// crowd1.gif
		// crowd2.gif
		// crowd3.gif
		//
		// However, if absent, you may have not yet entered that
		// contest or you may have finished it, and there is no
		// indication of how far you are in the crowd.

		// If we are past the contests, mark them all finished.
		if ( QuestDatabase.isQuestLaterThan( Quest.FINAL, QuestDatabase.STARTED ) )
		{
			Preferences.setInteger( "nsContestants1", 0 );
			Preferences.setInteger( "nsContestants2", 0 );
			Preferences.setInteger( "nsContestants3", 0 );
		}
	}

	// Quest scripts

	public static final int HEDGE_MAZE_TRAPS = 1;
	public static final int HEDGE_MAZE_GOPHER_DUCK = 2;
	public static final int HEDGE_MAZE_CHIHUAHUA_KIWI = 3;
	public static final int HEDGE_MAZE_NUGGLETS = 4;

	public static final void hedgeMazeTrapsScript()
	{
		SorceressLairManager.hedgeMazeScript( SorceressLairManager.HEDGE_MAZE_TRAPS );
	}

	public static final void hedgeMazeGopherDuckScript()
	{
		SorceressLairManager.hedgeMazeScript( SorceressLairManager.HEDGE_MAZE_GOPHER_DUCK );
	}

	public static final void hedgeMazeChihuahuaKiwiScript()
	{
		SorceressLairManager.hedgeMazeScript( SorceressLairManager.HEDGE_MAZE_CHIHUAHUA_KIWI );
	}

	public static final void hedgeMazeNuggletsScript()
	{
		SorceressLairManager.hedgeMazeScript( SorceressLairManager.HEDGE_MAZE_NUGGLETS );
	}

	public static final void hedgeMazeScript( final String tag )
	{
		int mode =
			tag.equals( "traps" ) ?
			SorceressLairManager.HEDGE_MAZE_TRAPS :
			( tag.equals( "gopher" ) || tag.equals( "duck" ) ) ?
			SorceressLairManager.HEDGE_MAZE_GOPHER_DUCK :
			( tag.equals( "chihuahua" ) || tag.equals( "kiwi" ) ) ?
			SorceressLairManager.HEDGE_MAZE_CHIHUAHUA_KIWI :
			tag.equals( "nugglets" ) ?
			SorceressLairManager.HEDGE_MAZE_NUGGLETS :
			0;

		if ( mode == 0 )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "What do you mean by '" + tag + "'?" );
			return;
		}

		SorceressLairManager.hedgeMazeScript( mode );
	}

	private static final void hedgeMazeScript( final int mode )
	{
		// Is the Hedge maze open? Go look at the tower.
		RequestThread.postRequest( new PlaceRequest( "nstower" ) );

		String status = Preferences.getString( Quest.FINAL.getPref() );
		if ( !status.equals( "step2" ) )
		{
			String message =
				status.equals( QuestDatabase.UNSTARTED ) ?
				"You haven't been given the quest to fight the Sorceress!" :
				QuestDatabase.isQuestLaterThan( status, "step2" ) ?
				"You have already completed the Hedge Maze." :
				"You haven't reached the Hedge Maze yet.";

			KoLmafia.updateDisplay( MafiaState.ERROR, message );
			return;
		}

		// The Hedge Maze is available
		switch ( mode )
		{
		case HEDGE_MAZE_TRAPS:
			// This is the expected path, entering in room 1
			Preferences.setInteger( "choiceAdventure1005", 2 );	// 'Allo
			Preferences.setInteger( "choiceAdventure1008", 2 );	// Pooling Your Resources
			Preferences.setInteger( "choiceAdventure1011", 2 );	// Of Mouseholes and Manholes
			Preferences.setInteger( "choiceAdventure1013", 1 );	// Mazel Tov!
			// If the user is already part way into the maze, the
			// following will eventually get him back on track.
			Preferences.setInteger( "choiceAdventure1006", 1 );	// One Small Step For Adventurer
			Preferences.setInteger( "choiceAdventure1007", 1 );	// Twisty Little Passages, All Hedge
			Preferences.setInteger( "choiceAdventure1009", 1 );	// Good Ol' 44% Duck
			Preferences.setInteger( "choiceAdventure1010", 1 );	// Another Day, Another Fork
			Preferences.setInteger( "choiceAdventure1012", 1 );	// The Last Temptation
			break;
		case HEDGE_MAZE_GOPHER_DUCK:
			// This is the expected path, entering in room 1
			Preferences.setInteger( "choiceAdventure1005", 1 );	// 'Allo
			Preferences.setInteger( "choiceAdventure1006", 2 );	// One Small Step For Adventurer
			Preferences.setInteger( "choiceAdventure1008", 1 );	// Pooling Your Resources
			Preferences.setInteger( "choiceAdventure1009", 2 );	// Good Ol' 44% Duck
			Preferences.setInteger( "choiceAdventure1011", 1 );	// Of Mouseholes and Manholes
			Preferences.setInteger( "choiceAdventure1012", 1 );	// The Last Temptation
			Preferences.setInteger( "choiceAdventure1013", 1 );	// Mazel Tov!
			// If the user is already part way into the maze, the
			// following will eventually get him back on track.
			Preferences.setInteger( "choiceAdventure1007", 1 );	// Twisty Little Passages, All Hedge
			Preferences.setInteger( "choiceAdventure1010", 1 );	// Another Day, Another Fork
			break;
		case HEDGE_MAZE_CHIHUAHUA_KIWI:
			// This is the expected path, entering in room 1
			Preferences.setInteger( "choiceAdventure1005", 1 );	// 'Allo
			Preferences.setInteger( "choiceAdventure1006", 1 );	// One Small Step For Adventurer
			Preferences.setInteger( "choiceAdventure1007", 2 );	// Twisty Little Passages, All Hedge
			Preferences.setInteger( "choiceAdventure1009", 2 );	// Good Ol' 44% Duck
			Preferences.setInteger( "choiceAdventure1011", 1 );	// Of Mouseholes and Manholes
			Preferences.setInteger( "choiceAdventure1012", 1 );	// The Last Temptation
			Preferences.setInteger( "choiceAdventure1013", 1 );	// Mazel Tov!
			// If the user is already part way into the maze, the
			// following will eventually get him back on track.
			Preferences.setInteger( "choiceAdventure1008", 1 );	// Pooling Your Resources
			Preferences.setInteger( "choiceAdventure1010", 1 );	// Another Day, Another Fork
			break;
		case HEDGE_MAZE_NUGGLETS:
			// This is the expected path, entering in room 1
			Preferences.setInteger( "choiceAdventure1005", 1 );	// 'Allo
			Preferences.setInteger( "choiceAdventure1006", 1 );	// One Small Step For Adventurer
			Preferences.setInteger( "choiceAdventure1007", 1 );	// Twisty Little Passages, All Hedge
			Preferences.setInteger( "choiceAdventure1008", 1 );	// Pooling Your Resources
			Preferences.setInteger( "choiceAdventure1009", 1 );	// Good Ol' 44% Duck
			Preferences.setInteger( "choiceAdventure1010", 1 );	// Another Day, Another Fork
			Preferences.setInteger( "choiceAdventure1011", 1 );	// Of Mouseholes and Manholes
			Preferences.setInteger( "choiceAdventure1012", 1 );	// The Last Temptation
			Preferences.setInteger( "choiceAdventure1013", 1 );	// Mazel Tov!
			break;
		default:
			KoLmafia.updateDisplay( MafiaState.ERROR, "Internal error: unknown mode (" + mode + ")." );
			return;
		}

		// See if we have enough turns available.
		int currentRoom = Preferences.getInteger( "currentHedgeMazeRoom" );
		int turns = 0;

		int room = 1005 + ( currentRoom >= 1 && currentRoom <= 9 ? ( currentRoom - 1 ) : 0 );
		while ( room <= 1013 )
		{
			// Visiting the current room takes a turn.
			turns++;

			// Going left always takes 1 turn
			if ( Preferences.getInteger( "choiceAdventure" + room ) == 1 )
			{
				room++;
				continue;
			}

			// Going right takes more depending on which room it is
			room += room == 1005 ? 3 :	// Trap 1
				room == 1006 ? 2 :	// topiary gopher
				room == 1007 ? 2 :	// topiary chihuahua herd
				room == 1008 ? 3 :	// Trap 2
				room == 1009 ? 2 :	// topiary duck
				room == 1010 ? 2 :	// topiary kiwi
				room == 1011 ? 2 :	// Trap 3
				1;
		}

		KoLmafia.updateDisplay( "You are currently in room " + currentRoom + " and it will take you " + turns + " to clear the maze." );

		// *** Check turns remaining

		// If it is all traps, assess readiness
		if ( mode == SorceressLairManager.HEDGE_MAZE_TRAPS )
		{
			// First trap takes 90% of maximum HP
			// Second trap takes 80% of maximum HP
			// Third trap takes 70% of maximum HP
			//
			// With no resistances, you will lose 90% + 70% + 50% =
			// 240% of your maximum HP.
			//
			// Elemental resistances ameliorate that. Resistance
			// Level 7 reduces elemental damage by ~61%.
			//    240 * .39 = 93.6%
			//
			// That suffices. Level 6 does not.
			//
			// Note that if you have a telescope (or have failed a
			// trap before) we may know the specific element(s) of
			// the traps.
			//
			// For now, assume that the user has already prepared
			// sufficiently to pass.
		}

		// Unless it's all nugglets, all the time, heal up first.
		if ( mode != SorceressLairManager.HEDGE_MAZE_NUGGLETS )
		{
			RecoveryManager.recoverHP( KoLCharacter.getMaximumHP() );
			if ( !KoLmafia.permitsContinue() )
			{
				return;
			}
		}

		KoLmafia.updateDisplay( "Entering the Hedge Maze..." );

		while ( status.equals( "step2" ) )
		{
			GenericRequest request = new PlaceRequest( "nstower", "ns_03_hedgemaze" );
			RequestThread.postRequest( request );

			if ( !KoLmafia.permitsContinue() )
			{
				// Presumably, we got beaten up by a fight or
				// trap, and an error message was displayed.
				return;
			}

			// *** If we won a fight, will we redirect into the choice?

			// *** What if we ran out of turns?
			if ( request.responseText.contains( "You don't have time" ) )
			{
				KoLmafia.updateDisplay( MafiaState.ERROR, "You're out of adventures." );
				return;
			}

			status = Preferences.getString( Quest.FINAL.getPref() );
		}

		KoLmafia.updateDisplay( "Hedge Maze cleared!" );
	}

	public static final void towerDoorScript()
	{
		// Is the Tower Door open? Go look at the tower.
		RequestThread.postRequest( new PlaceRequest( "nstower" ) );

		String status = Preferences.getString( Quest.FINAL.getPref() );
		if ( !status.equals( "step3" ) )
		{
			String message =
				status.equals( QuestDatabase.UNSTARTED ) ?
				"You haven't been given the quest to fight the Sorceress!" :
				QuestDatabase.isQuestLaterThan( status, "step3" ) ?
				"You have already opened the Tower Door." :
				"You haven't reached the Tower Door yet.";

			KoLmafia.updateDisplay( MafiaState.ERROR, message );
			return;
		}

		// Look at the door to decide what remains to be done
		RequestThread.postRequest( new PlaceRequest( "nstower_door" ) );

		String keys = Preferences.getString( "nsTowerDoorKeysUsed" );

		ArrayList<Object[]> needed = new ArrayList<Object[]>();
		for ( Object[] row : SorceressLairManager.LOCK_DATA )
		{
			AdventureResult key = (AdventureResult) row[ 0 ];
			if ( !keys.contains( key.getName() ) )
			{
				needed.add( row );
			}
		}

		// If we have any locks left to open, acquire the correct key and unlock them
		if ( needed.size() > 0 )
		{
			// First acquire all needed keys
			for ( Object[] row : needed )
			{
				AdventureResult key = (AdventureResult) row[ 0 ];
				if ( !InventoryManager.retrieveItem( key ) )
				{
					KoLmafia.updateDisplay( MafiaState.ERROR, "Failed to acquire " + key );
					return;
				}
			}

			// Then unlock each lock
			for ( Object[] row : needed )
			{
				AdventureResult key = (AdventureResult) row[ 0 ];
				String keyName = key.getName();
				String action = (String) row[ 1 ];
				RequestThread.postRequest( new PlaceRequest( "nstower_door", action ) );
				keys = Preferences.getString( "nsTowerDoorKeysUsed" );
				if ( !keys.contains( keyName ) )
				{
					KoLmafia.updateDisplay( MafiaState.ERROR, "Failed to open lock using " + key );
					return;
				}
			}
		}

		// Now turn the doorknob
		RequestThread.postRequest( new PlaceRequest( "nstower_door", "ns_doorknob", true ) );

		status = Preferences.getString( Quest.FINAL.getPref() );
		if ( status.equals( "step4" ) )
		{
			KoLmafia.updateDisplay( "Tower Door open!" );
		}
	}
}
