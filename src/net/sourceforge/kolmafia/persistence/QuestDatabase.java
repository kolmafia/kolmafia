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

	private static final String[][] questNames =
	{
		{
			"questL02Larva", "Looking for a Larva in All the Wrong Places"
		},
		{
			"questL03Rat", "Ooh, I Think I Smell a Rat."
		},
		{
			"questL04Bat", "Ooh, I Think I Smell a Bat."
		},
		{
			"questL05Goblin", "The Goblin Who Wouldn't Be King"
		},
		{
			"questL06Friar", "Trial By Friar"
		},
		{
			"questL07Cyrptic", "Cyrptic Emanations"
		},
		{
			"questL08Trapper", "Am I my Trapper's Keeper?"
		},
		{
			"questL09Lol", "A Quest, LOL"
		},
		{
			"questL10Garbage", "The Rain on the Plains is Mainly Garbage"
		},
		{
			"questL11MacGuffin", "<Player Name> and the Quest for the Holy MacGuffin"
		},
		{
			"questL11Worship", "Gotta Worship Them All"
		},
		{
			"questL11Manor", "In a Manor of Spooking"
		},
		{
			"questL11Palindome", "Never Odd Or Even"
		},
		{
			"questL11Pyramid", "A Pyramid Scheme"
		},
		{
			"questL12War", "Make War, Not... Oh, Wait"
		},
		{
			"questL13Final", "The Final Ultimate Epic Final Conflict"
		},

		{
			"questG01Meatcar", "My Other Car Is Made of Meat"
		},
		{
			"questG02Whitecastle", "<Player Name> and <Familiar Name> Go To White Citadel"
		},
		{
			"questG03Ego", "The Wizard of Ego"
		},
		{
			"questG04Nemesis", "Me and My Nemesis"
		},
		{
			"questG05Dark", "A Dark and Dank and Sinister Quest"
		},
		{
			"questG06Delivery", "<Player Name>'s Delivery Service"
		},

		{
			"questM01Untinker", "Driven Crazy"
		},
		{
			"questM02Artist", "Suffering For His Art"
		},
		{
			"questM03Bugbear", "A Bugbear of a Problem"
		},
		{
			"questM04Galaktic", "What's Up, Doc?"
		},
		{
			"questM05Toot", "Toot!"
		},
		{
			"questM06Gourd", "Out of Your Gourd"
		},
		{
			"questM07Hammer", "Hammer Time"
		},
		{
			"questM08Baker", "Baker, Baker"
		},
		{
			"questM09Rocks", "When Rocks Attack"
		},
		{
			"questM10Azazel", "Angry <Player Name>, this is Azazel in Hell."
		},
		{
			"questM11Postal", "Going Postal"
		},
		{
			"questM12Pirate", "I Rate, You Rate"
		},
		{
			"questM13Escape", "The Pretty Good Escape"
		},
		{
			"questM14Bounty", "A Bounty Hunter Is You!"
		},

		{
			"questS01OldGuy", "An Old Guy and The Ocean"
		},
		{
			"questS02Monkees", "Hey, Hey, They're Sea Monkees"
		},

		{
			"questF01Primordial", "Primordial Fear"
		},
		{
			"questF02Hyboria", "Hyboria? I don't even..."
		},
		{
			"questF03Future", "Future"
		},
		{
			"questF04Elves", "The Quest for the Legendary Beat"
		},

		{
			"questI01Scapegoat", "Scapegoat"
		},
		{
			"questI02Beat", "Repair the Elves' Shield Generator"
		},
	};

	static
	{
		for ( int i = 0; i < questNames.length; ++i )
		{
			// Capitalize that name.
			questNames[ i ][ 1 ] = questNames[ i ][ 1 ].replaceAll( "<Player\\sName>", KoLCharacter
				.getUserName().substring( 0, 1 ).toUpperCase()
				+ KoLCharacter.getUserName().substring( 1 ) );
		}

	}

	public static String titleToPreference( final String title )
	{
		if ( title.indexOf( "White Citadel" ) != -1 )
		{
			// Hard code this quest, for now. The familiar name in the middle of the string is annoying to
			// deal with.
			return "questG02Whitecastle";
		}
		for ( int i = 0; i < questNames.length; ++i )
		{
			if ( questNames[ i ][ 1 ].indexOf( title ) != -1 )
			{
				return questNames[ i ][ 0 ];
			}
		}

		// couldn't find a match
		return "";
	}

	public static int prefToIndex( final String pref )
	{
		for ( int i = 0; i < questNames.length; ++i )
		{
			if ( questNames[ i ][ 0 ].indexOf( pref ) != -1 )
			{
				return i;
			}
		}

		// couldn't find a match
		return -1;
	}

}
