/**
 * Copyright (c) 2005-2012, KoLmafia development team
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

package net.sourceforge.kolmafia.persistence;

import java.util.regex.Pattern;

import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLDatabase;

/**
 * Provides utility functions for dealing with quests.
 * 
 */
public class QuestDatabase
	extends KoLDatabase
{
	public static final String STARTED = "started";
	public static final String FINISHED = "finished";
	public static final String STEP1 = "step1";
	public static final String STEP2 = "step2";
	public static final String STEP3 = "step3";
	public static final String STEP4 = "step4";
	public static final String STEP5 = "step5";
	public static final String STEP6 = "step6";
	public static final String STEP7 = "step7";
	public static final String STEP8 = "step8";
	public static final String STEP9 = "step9";
	public static final String STEP10 = "step10";
	public static final String STEP11 = "step11";
	public static final String STEP12 = "step12";

	public static final Pattern HTML_WHITESPACE = Pattern.compile( "<[^<]+?>|[\\s\\n]" );

	private static final String[][] questLogData =
	{
		{
			"questL02Larva",
			"Looking for a Larva in All the Wrong Places",
			"The Council of Loathing wants you to bring them a mosquito larva, for some reason. They told you to look for one in the Spooky Forest, in the Distant Woods.<p>How can a woods contain a forest? Suspension of disbelief, that's how.",
			"You delivered a mosquito larva to the Council of Loathing. Nice work!"
		},
		{
			"questL03Rat", "Ooh, I Think I Smell a Rat.", "",
			"You've solved the rat problem at the Typical Tavern. Way to go!",
		},
		{
			"questL04Bat", "Ooh, I Think I Smell a Bat.", "", "You have slain the Boss Bat. Huzzah!"
		},
		{
			"questL05Goblin", "The Goblin Who Wouldn't Be King", "",
			"You have slain the Goblin King. Good job!"
		},
		{
			"questL06Friar", "Trial By Friar", "",
			"You have cleansed the taint of the Deep Fat Friars. Congratulations!"
		},
		{
			"questL07Cyrptic", "Cyrptic Emanations", "",
			"You've undefiled the Cyrpt, and defeated the Bonerdagon. Hip, Hip, Hooray!"
		},
		{
			"questL08Trapper", "Am I my Trapper's Keeper?", "",
			"You have learned how to hunt Yetis from the L337 Tr4pz0r. Shazam!"
		},
		{
			"questL09Lol", "A Quest, LOL", "",
			"You have helped the Baron Rof L'm Fao with his monster problem. w00t!"
		},
		{
			"questL10Garbage", "The Rain on the Plains is Mainly Garbage", "",
			"You have stopped the rain of giant garbage in the Nearby Plains. Slick!"
		},
		{
			"questL11MacGuffin",
			"<Player Name> and the Quest for the Holy MacGuffin",
			"",
			"You've handed the Holy MacGuffin over to the Council, and enjoyed a ticker-tape parade in your honor. That quest was so ridiculous, it wasn't even funny, and now it's over! Hooray!"
		},
		{
			"questL11Worship",
			"Gotta Worship Them All",
			"",
			"You've defeated the ancient ghost of an ancient mummy of an ancient high priest and claimed his ancient amulet! Go you!"
		},
		{
			"questL11Manor", "In a Manor of Spooking", "",
			"You've defeated Lord Spookyraven and claimed the Eye of Ed! Huzzah!"
		},
		{
			"questL11Palindome", "Never Odd Or Even", "",
			"Congratulations, you've recovered the long-lost Staff of Fats!<p>Nice Work!"
		},
		{
			"questL11Pyramid",
			"A Pyramid Scheme",
			"",
			"The mighty Ed the Undying has fallen! You recovered the Holy MacGuffin! Jolly good show, mate! "
		},
		{
			"questL12War", "Make War, Not... Oh, Wait", "",
			"You led the Orcish frat boys to victory in the Great War. For The Horde!"
		},
		{
			"questL13Final", "The Final Ultimate Epic Final Conflict", "",
			"You have defeated the Naughty Sorceress and freed the King! What are you hanging around here for?"
		},

		{
			"questG01Meatcar", "My Other Car Is Made of Meat", "",
			"You've built a new meat car from parts. Impressive!"
		},
		{
			"questG02Whitecastle",
			"<Player Name> and <Familiar Name> Go To White Citadel",
			"You've been charged by your Guild (sort of) with the task of bringing back a delicious meal from the legendary White Citadel. You've been told it's somewhere near Whitey's Grove, in the Distant Woods.",
			"You've delivered a satchel of incredibly greasy food to someone you barely know. Plus, you can now shop at White Citadel whenever you want. Awesome!"
		},
		{
			"questG03Ego",
			"The Wizard of Ego",
			"You've been tasked with digging up the grave of an ancient and powerful wizard and bringing back a key that was buried with him. What could possibly go wrong?",
			"You've turned in the old book, and they said they didn't want it and for you to go away. A bit anticlimactic, but I suppose it still counts as a success. Congratulations!"
		},
		{
			"questG04Nemesis", "Me and My Nemesis", "", ""
		},
		{
			"questG05Dark",
			"A Dark and Dank and Sinister Quest",
			"",
			"Your Nemesis has scuttled away in defeat, leaving you with a sweet Epic Hat and a feeling of smug superiority. Well done you!"
		},
		{
			"questG06Delivery", "<Player Name>'s Delivery Service", "", ""
		},

		{
			"questM01Untinker",
			"Driven Crazy",
			"The Untinker in Seaside Town wants you to find his screwdriver. He thinks he left it at Degrassi Knoll, on the Nearby Plains.",
			"You fetched the Untinker's screwdriver. Nice going!"
		},
		{
			"questM02Artist",
			"Suffering For His Art",
			"The Pretentious Artist, who lives on the Wrong Side of the Tracks in Seaside Town, has lost his palette, his pail of paint, and his paintbrush.<p>He told you that he thinks the palette is in the Haunted Pantry, the pail of paint is somewhere near the Sleazy Back Alley, and the paintbrush was taken by a Knob Goblin.",
			"You helped retrieve the Pretentious Artist's stuff. Excellent!"
		},
		{
			"questM03Bugbear", "A Bugbear of a Problem", "",
			"You've helped Mayor Zapruder of Degrassi Knoll with his spooky gravy fairy problem. Nice going!"
		},
		{
			"questM04Galaktic",
			"What's Up, Doc?",
			"",
			"You found some herbs for Doc Galaktik, and he rewarded you with a permanent discount on Curative Nostrums and Fizzy Invigorating Tonics. Nifty!"
		},
		{
			"questM05Toot", "Toot!", "", "You have completed your training with the Toot Oriole. Groovy!"
		},
		{
			"questM06Gourd", "Out of Your Gourd", "", ""
		},
		{
			"questM07Hammer", "Hammer Time", "", ""
		},
		{
			"questM08Baker", "Baker, Baker", "", ""
		},
		{
			"questM09Rocks", "When Rocks Attack", "", ""
		},
		{
			"questM10Azazel",
			"Angry <Player Name>, this is Azazel in Hell.",
			"",
			"You've found Azazel's unicorn, his lollipop, and his tutu. This peek into the nature of evil is disturbing, but the reward was gratifying. Go you!"
		},
		{
			"questM11Postal", "Going Postal", "", ""
		},
		{
			"questM12Pirate", "I Rate, You Rate", "", ""
		},
		{
			"questM13Escape", "The Pretty Good Escape", "", ""
		},
		{
			"questM14Bounty", "A Bounty Hunter Is You!", "", ""
		},

		{
			"questS01OldGuy", "An Old Guy and The Ocean", "", ""
		},
		{
			"questS02Monkees", "Hey, Hey, They're Sea Monkees", "", ""
		},

		{
			"questF01Primordial", "Primordial Fear", "", ""
		},
		{
			"questF02Hyboria", "Hyboria? I don't even...", "", ""
		},
		{
			"questF03Future", "Future", "", ""
		},
		{
			"questF04Elves", "The Quest for the Legendary Beat", "", ""
		},

		{
			"questI01Scapegoat", "Scapegoat", "", ""
		},
		{
			"questI02Beat", "Repair the Elves' Shield Generator", "", ""
		},
	};

	static
	{
		for ( int i = 0; i < questLogData.length; ++i )
		{
			// Capitalize that name.
			questLogData[ i ][ 1 ] = questLogData[ i ][ 1 ].replaceAll( "<Player\\sName>", KoLCharacter
				.getUserName().substring( 0, 1 ).toUpperCase()
				+ KoLCharacter.getUserName().substring( 1 ) );
		}

	}

	public static String titleToPref( final String title )
	{
		if ( title.indexOf( "White Citadel" ) != -1 )
		{
			// Hard code this quest, for now. The familiar name in the middle of the string is annoying to
			// deal with.
			return "questG02Whitecastle";
		}
		for ( int i = 0; i < questLogData.length; ++i )
		{
			if ( questLogData[ i ][ 1 ].indexOf( title ) != -1 )
			{
				return questLogData[ i ][ 0 ];
			}
		}

		// couldn't find a match
		return "";
	}

	public static String prefToTitle( final String pref )
	{
		for ( int i = 0; i < questLogData.length; ++i )
		{
			if ( questLogData[ i ][ 0 ].indexOf( pref ) != -1 )
			{
				return questLogData[ i ][ 1 ];
			}
		}

		// couldn't find a match
		return "";
	}

	public static int prefToIndex( final String pref )
	{
		for ( int i = 0; i < questLogData.length; ++i )
		{
			if ( questLogData[ i ][ 0 ].indexOf( pref ) != -1 )
			{
				return i;
			}
		}

		// couldn't find a match
		return -1;
	}

	public static String findQuestProgress( String pref, String details )
	{
		// First thing to do is find which quest we're talking about.
		int index = prefToIndex( pref );

		if ( index == -1 )
		{
			return "";
		}

		// Next, find the number of quest steps
		final int steps = questLogData[ index ].length - 2;

		if ( steps < 1 )
		{
			return "";
		}

		// Now, try to see if we can find an exact match for response->step. This is often messed up by
		// whitespace, html, and the like. We'll handle that below.
		int foundAtStep = -1;

		for ( int i = 2; i < questLogData[ index ].length; ++i )
		{
			if ( questLogData[ index ][ i ].indexOf( details ) != -1 )
			{
				foundAtStep = i - 2;
				break;
			}
		}
		if ( foundAtStep != -1 )
		{
			if ( foundAtStep == 0 )
			{
				return QuestDatabase.STARTED;
			}
			else if ( foundAtStep == steps -1 )
			{
				return QuestDatabase.FINISHED;
			}
			else
			{
				return "Step" + foundAtStep;
			}
		}

		// Didn't manage to find an exact match. Now try stripping out all whitespace, newlines, and anything
		// that looks like html from questData and response.
		String cleanedResponse = QuestDatabase.HTML_WHITESPACE.matcher( details ).replaceAll( "" );
		String cleanedQuest = "";
		
		// RequestLogger.printLine( cleanedResponse );

		for ( int i = 2; i < questLogData[ index ].length; ++i )
		{
			cleanedQuest = QuestDatabase.HTML_WHITESPACE.matcher( questLogData[ index ][ i ] ).replaceAll(
				"" );
			//RequestLogger.printLine( cleanedQuest );
			if ( cleanedQuest.indexOf( cleanedResponse ) != -1 )
			{
				foundAtStep = i - 2;
				break;
			}
		}

		if ( foundAtStep != -1 )
		{
			if ( foundAtStep == 0 )
			{
				return QuestDatabase.STARTED;
			}
			else if ( foundAtStep == steps - 1 )
			{
				return QuestDatabase.FINISHED;
			}
			else
			{
				return "Step" + foundAtStep;
			}
		}

		// Well, that didn't work either. Punt.

		return "";
	}

}
