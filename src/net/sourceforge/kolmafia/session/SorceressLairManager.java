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
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLConstants.Stat;
import net.sourceforge.kolmafia.KoLmafia;
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

	private static final boolean checkPrerequisites( final int min, final int max )
	{
		KoLmafia.updateDisplay( "Checking prerequisites..." );

		// Make sure he's been given the quest

		RequestThread.postRequest( SorceressLairManager.QUEST_HANDLER.constructURLString( "main.php" ) );

		if ( SorceressLairManager.QUEST_HANDLER.responseText.indexOf( "lair.php" ) == -1 )
		{
			// Visit the council to see if the quest can be
			// unlocked, but only if you've reached level 13.

			boolean unlockedQuest = false;
			if ( KoLCharacter.getLevel() >= 13 )
			{
				// We should theoretically be able to figure out
				// whether or not the quest is unlocked from the
				// HTML in the council request, but for now, use
				// this inefficient workaround.

				RequestThread.postRequest( CouncilFrame.COUNCIL_VISIT );
				RequestThread.postRequest( SorceressLairManager.QUEST_HANDLER.constructURLString( "main.php" ) );
				unlockedQuest = SorceressLairManager.QUEST_HANDLER.responseText.indexOf( "lair.php" ) != -1;
			}

			if ( !unlockedQuest )
			{
				KoLmafia.updateDisplay(
					MafiaState.ERROR, "You haven't been given the quest to fight the Sorceress!" );
				return false;
			}
		}

		// Make sure he can get to the desired area

		// Deduce based on which image map is used:
		//
		// NoMap = lair1
		// Map = lair1, lair3
		// Map2 = lair1, lair3, lair4
		// Map3 = lair1, lair3, lair4, lair5
		// Map4 = lair1, lair3, lair4, lair5, lair6

		RequestThread.postRequest( SorceressLairManager.QUEST_HANDLER.constructURLString( "lair.php" ) );
		Matcher mapMatcher = SorceressLairManager.MAP_PATTERN.matcher( SorceressLairManager.QUEST_HANDLER.responseText );

		if ( mapMatcher.find() )
		{
			String map = mapMatcher.group( 1 );
			int reached;

			if ( map.equals( "NoMap" ) )
			{
				reached = 1;
			}
			else if ( map.equals( "Map" ) )
			{
				reached = 3;
			}
			else if ( map.equals( "Map2" ) )
			{
				reached = 4;
			}
			else if ( map.equals( "Map3" ) )
			{
				reached = 5;
			}
			else if ( map.equals( "Map4" ) )
			{
				reached = 6;
			}
			else
			{
				reached = 0;
			}

			if ( reached < min )
			{
				switch ( min )
				{
				case 0:
				case 1:
					KoLmafia.updateDisplay( MafiaState.ERROR, "The sorceress quest has not yet unlocked." );
					return false;
				case 2:
				case 3:
					KoLmafia.updateDisplay( MafiaState.ERROR, "You must complete the entryway first." );
					return false;
				case 4:
				case 5:
					KoLmafia.updateDisplay( MafiaState.ERROR, "You must complete the hedge maze first." );
					return false;
				case 6:
					KoLmafia.updateDisplay( MafiaState.ERROR, "You must complete the tower first." );
					return false;
				}
			}

			if ( reached > max )
			{
				KoLmafia.updateDisplay( "You're already past this script." );
				return false;
			}
		}

		// Otherwise, they've passed all the standard checks
		// on prerequisites.  Return true.

		return true;
	}

	public static final void completeEntryway()
	{
		if ( !SorceressLairManager.checkPrerequisites( 1, 2 ) )
		{
			return;
		}
	}

	public static final void completeHedgeMaze()
	{
		if ( !SorceressLairManager.checkPrerequisites( 3, 3 ) )
		{
			return;
		}
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
		if ( !SorceressLairManager.checkPrerequisites( 4, 6 ) )
		{
			return -1;
		}

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

	public static final Pattern WHICHKEY_PATTERN = Pattern.compile( "whichkey=(\\d+)" );
	public static final Pattern ACQUIRE_PATTERN = Pattern.compile( "You acquire an item: <b>(.*?)</b>" );

	public static final void decorateKey( final String location, final StringBuffer buffer )
	{
		if ( !Preferences.getBoolean( "relayShowSpoilers" ) )
		{
			return;
		}

		Matcher matcher = SorceressLairManager.WHICHKEY_PATTERN.matcher( location );
		if ( !matcher.find() )
		{
			return;
		}

		int key = StringUtilities.parseInt( matcher.group( 1 ) );

		if ( key == ItemPool.UNIVERSAL_KEY )
		{
			// The key instantly morphs into another shape.
			//   You acquire an item: <b>xxx</b>
			//
			// or
			// 
			// You can't figure out anything else to do with your universal key.

			if ( buffer.indexOf( "You can't figure out anything else to do with your universal key" ) != -1 )
			{
				return;
			}

			matcher = SorceressLairManager.ACQUIRE_PATTERN.matcher( buffer );
			if ( !matcher.find() )
			{
				return;
			}

			key = ItemDatabase.getItemId( matcher.group( 1 ) );
		}
	}

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
