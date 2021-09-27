package net.sourceforge.kolmafia.session;

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

import net.sourceforge.kolmafia.persistence.AdventureDatabase;
import net.sourceforge.kolmafia.persistence.MonsterDatabase.Element;
import net.sourceforge.kolmafia.persistence.QuestDatabase;
import net.sourceforge.kolmafia.persistence.QuestDatabase.Quest;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.request.PlaceRequest;

import net.sourceforge.kolmafia.utilities.InputFieldUtilities;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public abstract class SorceressLairManager
{
	private static final GenericRequest QUEST_HANDLER = new GenericRequest( "" );

	// Patterns for repeated usage.
	private static final Pattern MAP_PATTERN = Pattern.compile( "usemap=\"#(\\w+)\"" );

	// Items for the shadow battle
	private static final AdventureResult [] HEALING_ITEMS = new AdventureResult[]
	{
		ItemPool.get( ItemPool.RED_PIXEL_POTION, 1 ),
		ItemPool.get( ItemPool.FILTHY_POULTICE, 1 ),
		ItemPool.get( ItemPool.GAUZE_GARTER, 1 ),
	};

	// Items for the chamber
	private static final AdventureResult CONFIDENCE = EffectPool.get( EffectPool.CONFIDENCE, Integer.MAX_VALUE );
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
			"Coldest Adventurer",
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
				QuestDatabase.setQuestProgress( Quest.FINAL, "step3" );
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
				String setting = "nsContestants" + decision;
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
		// "You already entered the Fastest Adventurer contest.  You
		// should go get in line and wait for it to start.  It says
		// here that the contest is currently you and one other
		// Adventurer.  Hey, a 50/50 chance is pretty good, eh?"
		//
		// "You already entered the <attribute> Adventurer contest.
		// You should go wait in line with the other Adventurers.  It
		// says on my clipboard that only one other Adventurer besides
		// you entered this one.  So you should go wait in line with
		// the other Adventurer, is what I meant to say."
		//
		// "You already entered the <attribute> Adventurer contest.  You
		// should go wait with the other entrants.  It says here that
		// there's only one other person in that contest, actually.  So
		// go wait with the... entrant."
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
				( text.contains( "you and one other" ) ||
				  text.contains( "only one other Adventurer besides you" ) ||
				  text.contains( "only one other person in that contest" ) ) ?
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
				action.equals( "ns_11_prism" ) ? ( "[" + KoLAdventure.getAdventureCount() + "] Freeing King Ralph" ) :
				null;

			if ( message == null )
			{
				// Everything else is a KoLAdventure
				return false;
			}

			RequestLogger.printLine();
			RequestLogger.updateSessionLog();
		}
		else if ( place.equals( "nstower_door" ) || place.equals( "nstower_doorlowkey" ) )
		{
			return TowerDoorManager.registerTowerDoorRequest( urlString );
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
			KoLAdventure.setLastAdventure( AdventureDatabase.getAdventure( "The Hedge Maze" ) );
			QuestDatabase.setQuestProgress( Quest.FINAL, "step4" );
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
			"crowd1.gif",
			"step1",
		},
		{
			"crowd2.gif",
			"step1",
		},
		{
			"crowd3.gif",
			"step1",
		},
		{
			"nstower_regdesk.gif",
			QuestDatabase.STARTED,
		},
		{
			"nstower_courtyard.gif",
			"step3",
		},
		{
			"nstower_hedgemaze.gif",
			"step4",
		},
		{
			"nstower_towerdoor.gif",
			"step5",
		},
		{
			"nstower_tower1.gif",
			"step6",
		},
		{
			"nstower_tower2.gif",
			"step7",
		},
		{
			"nstower_tower3.gif",
			"step8",
		},
		{
			"nstower_tower4.gif",
			"step9",
		},
		{
			"nstower_tower5.gif",
			"step10",
		},
		{
			"chamberlabel.gif",
			"step11",
		},
		{
			"kingprism",
			"step12",
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

		QuestDatabase.setQuestIfBetter( Quest.FINAL, step );

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
		if ( QuestDatabase.isQuestLaterThan( Quest.FINAL, "step1" ) )
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

	private static void hedgeMazeScript( final int mode )
	{
		// Is the Hedge maze open? Go look at the tower.
		RequestThread.postRequest( new PlaceRequest( "nstower" ) );

		String status = Quest.FINAL.getStatus();
		if ( !status.equals( "step4" ) )
		{
			String message =
				status.equals( QuestDatabase.UNSTARTED ) ?
				"You haven't been given the quest to fight the Sorceress!" :
				QuestDatabase.isQuestLaterThan( status, "step4" ) ?
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
			Preferences.setInteger( "choiceAdventure1009", 1 );	// Good Ol' 44% Duck
			Preferences.setInteger( "choiceAdventure1010", 2 );	// Another Day, Another Fork
			Preferences.setInteger( "choiceAdventure1012", 1 );	// The Last Temptation
			Preferences.setInteger( "choiceAdventure1013", 1 );	// Mazel Tov!
			// If the user is already part way into the maze, the
			// following will eventually get him back on track.
			Preferences.setInteger( "choiceAdventure1008", 1 );	// Pooling Your Resources
			Preferences.setInteger( "choiceAdventure1011", 1 );	// Of Mouseholes and Manholes
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

		KoLmafia.updateDisplay( "You are currently in room " + currentRoom + " and it will take you " + turns + " turns to clear the maze." );

		int available = KoLCharacter.getAdventuresLeft();
		int lacking = turns - available;
		if ( lacking > 0 )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "You need " + lacking + " more adventure" + ( lacking > 1 ? "s" : "" ) + " to take that path through the maze." );
			return;
		}

		// This maze looks really complicated, and it might take more
		// Adventures to finish it than you currently have. Are you
		// sure you want to go in?
		//
		// We have already verified that we have enough adventures.
		if ( available < 9 )
		{
			Preferences.setInteger( "choiceAdventure1004", 1 );	// 'This Maze is... Mazelike...
		}

		// If it is all traps, assess readiness
		if ( mode == SorceressLairManager.HEDGE_MAZE_TRAPS )
		{
			// First trap takes 90% of maximum HP
			// Second trap takes 80% of maximum HP
			// Third trap takes 70% of maximum HP
			//
			// With no resistances, you will lose 90% + 80% + 70% =
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

			int hpLost = 0;
			Element trap1 = Element.NONE;
			Element trap2 = Element.NONE;
			Element trap3 = Element.NONE;

			if ( currentRoom <= 1 )
			{
				trap1 = Element.fromString( Preferences.getString( "nsChallenge3" ) );
				// If not known, assume lowest resist
				if ( trap1 == Element.NONE )
				{
					trap1 = Element.COLD;
					if ( KoLCharacter.getElementalResistanceLevels( Element.HOT ) < 
						KoLCharacter.getElementalResistanceLevels( trap1 ) )
					{
						trap1 = Element.HOT;
					}
					if ( KoLCharacter.getElementalResistanceLevels( Element.SLEAZE ) < 
						KoLCharacter.getElementalResistanceLevels( trap1 ) )
					{
						trap1 = Element.SLEAZE;
					}
					if ( KoLCharacter.getElementalResistanceLevels( Element.SPOOKY ) < 
						KoLCharacter.getElementalResistanceLevels( trap1 ) )
					{
						trap1 = Element.SPOOKY;
					}
					if ( KoLCharacter.getElementalResistanceLevels( Element.STENCH ) < 
						KoLCharacter.getElementalResistanceLevels( trap1 ) )
					{
						trap1 = Element.STENCH;
					}
				}
				hpLost = (int) Math.ceil( (double) KoLCharacter.getMaximumHP() * 0.9 * ( 1.0 - KoLCharacter.getElementalResistance( trap1 ) / 100 ) );
			}

			if ( currentRoom <= 4 )
			{
				trap2 = Element.fromString( Preferences.getString( "nsChallenge4" ) );
				// If not known, assume lowest resist
				if ( trap2 == Element.NONE )
				{
					if ( trap1 != Element.COLD && ( KoLCharacter.getElementalResistanceLevels( Element.COLD ) < 
						KoLCharacter.getElementalResistanceLevels( trap2 ) || trap2 == Element.NONE ) )
					{
						trap2 = Element.COLD;
					}
					if ( trap1 != Element.HOT && ( KoLCharacter.getElementalResistanceLevels( Element.HOT ) < 
						KoLCharacter.getElementalResistanceLevels( trap2 ) || trap2 == Element.NONE ) )
					{
						trap2 = Element.HOT;
					}
					if ( trap1 != Element.SLEAZE && KoLCharacter.getElementalResistanceLevels( Element.SLEAZE ) < 
						KoLCharacter.getElementalResistanceLevels( trap2 ) )
					{
						trap2 = Element.SLEAZE;
					}
					if ( trap1 != Element.SPOOKY && KoLCharacter.getElementalResistanceLevels( Element.SPOOKY ) < 
						KoLCharacter.getElementalResistanceLevels( trap2 ) )
					{
						trap2 = Element.SPOOKY;
					}
					if ( trap1 != Element.STENCH && KoLCharacter.getElementalResistanceLevels( Element.STENCH ) < 
						KoLCharacter.getElementalResistanceLevels( trap2 ) )
					{
						trap2 = Element.STENCH;
					}
				}
				hpLost += (int) Math.ceil( (double) KoLCharacter.getMaximumHP() * 0.8 * ( 1.0 - KoLCharacter.getElementalResistance( trap2 ) / 100 ) );
			}

			if ( currentRoom <= 7 )
			{
				trap3 = Element.fromString( Preferences.getString( "nsChallenge5" ) );
				// If not known, assume lowest resist
				if ( trap3 == Element.NONE )
				{
					if ( trap1 != Element.COLD && trap2 != Element.COLD && ( KoLCharacter.getElementalResistanceLevels( Element.COLD ) < 
						KoLCharacter.getElementalResistanceLevels( trap3 ) || trap3 == Element.NONE ) )
					{
						trap3 = Element.COLD;
					}
					if ( trap1 != Element.HOT && trap2 != Element.HOT && ( KoLCharacter.getElementalResistanceLevels( Element.HOT ) < 
						KoLCharacter.getElementalResistanceLevels( trap3 ) || trap3 == Element.NONE ) )
					{
						trap3 = Element.HOT;
					}
					if ( trap1 != Element.SLEAZE && trap2 != Element.SLEAZE && ( KoLCharacter.getElementalResistanceLevels( Element.SLEAZE ) < 
						KoLCharacter.getElementalResistanceLevels( trap3 ) || trap3 == Element.NONE ) )
					{
						trap3 = Element.SLEAZE;
					}
					if ( trap1 != Element.SPOOKY && trap2 != Element.SPOOKY && KoLCharacter.getElementalResistanceLevels( Element.SPOOKY ) < 
						KoLCharacter.getElementalResistanceLevels( trap3 ) )
					{
						trap3 = Element.SPOOKY;
					}
					if ( trap1 != Element.STENCH && trap2 != Element.STENCH && KoLCharacter.getElementalResistanceLevels( Element.STENCH ) < 
						KoLCharacter.getElementalResistanceLevels( trap3 ) )
					{
						trap3 = Element.STENCH;
					}
				}
				hpLost += (int) Math.ceil( (double) KoLCharacter.getMaximumHP() * 0.7 * ( 1.0 - KoLCharacter.getElementalResistance( trap3 ) / 100 ) );
			}

			// If you won't survive, prompt for confirmation
			// *** Cannot heal in Darke Gyffte
			if ( ( ( hpLost >= KoLCharacter.getMaximumHP() ) ||
			       ( ( KoLCharacter.inDarkGyffte() ) &&
				 ( hpLost >= KoLCharacter.getCurrentHP() ) ) ) &&
			     !InputFieldUtilities.confirm( "You won't survive to the end of the Hedge Maze, are you sure ?" ) )
			{	
				return;
			}
		}

		// Unless it's all nugglets, all the time, heal up first.
		// *** Cannot heal in Darke Gyffte. Validated sufficient HP above
		if ( mode != SorceressLairManager.HEDGE_MAZE_NUGGLETS &&
		     !KoLCharacter.inDarkGyffte() )
		{
			RecoveryManager.recoverHP( KoLCharacter.getMaximumHP() );
			if ( !KoLmafia.permitsContinue() )
			{
				return;
			}
		}

		KoLmafia.updateDisplay( "Entering the Hedge Maze..." );

		while ( status.equals( "step4" ) )
		{
			GenericRequest request = new PlaceRequest( "nstower", "ns_03_hedgemaze" );
			RequestThread.postRequest( request );

			if ( !KoLmafia.permitsContinue() )
			{
				// Presumably, we got beaten up by a fight or
				// trap, and an error message was displayed.
				return;
			}

			// We failed an elemental test
			if ( mode == SorceressLairManager.HEDGE_MAZE_TRAPS )
			{
				if ( Preferences.getInteger( "currentHedgeMazeRoom" ) == 1 && !request.responseText.contains( "lucky you survived that" ) )
				{
					return;
				}
				if ( Preferences.getInteger( "currentHedgeMazeRoom" ) == 4 && !request.responseText.contains( "drag yourself out of the opposite end" ) )
				{
					return;
				}
				if ( Preferences.getInteger( "currentHedgeMazeRoom" ) == 7 && !request.responseText.contains( "emerge from the tunnel" ) )
				{
					return;
				}
			}

			// *** If we won a fight, will we redirect into the choice?

			// This shouldn't happen. We checked available turns before we entered the maze.
			if ( request.responseText.contains( "You don't have time" ) )
			{
				KoLmafia.updateDisplay( MafiaState.ERROR, "You're out of adventures." );
				return;
			}

			status = Quest.FINAL.getStatus();
		}

		if ( status.equals( "step5" ) )
		{
			KoLmafia.updateDisplay( "Hedge Maze cleared!" );
		}
		else
		{
			KoLmafia.updateDisplay( "Hedge Maze not complete. Unexpected quest status: " + Quest.FINAL.getPref() + " = " + status );
		}
	}
}
