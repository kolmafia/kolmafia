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

package net.sourceforge.kolmafia.persistence;

import java.io.BufferedReader;

import java.util.ArrayList;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLDatabase;
import net.sourceforge.kolmafia.StaticEntity;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.utilities.FileUtilities;
import net.sourceforge.kolmafia.utilities.StringUtilities;

/**
 * Provides utility functions for dealing with quests.
 * 
 */
public class QuestDatabase
	extends KoLDatabase
{
	public enum Quest
	{
			LARVA( "questL02Larva" ),
			RAT( "questL03Rat" ),
			BAT( "questL04Bat" ),
			GOBLIN( "questL05Goblin" ),
			FRIAR( "questL06Friar" ),
			CYRPT( "questL07Cyrptic" ),
			TRAPPER( "questL08Trapper" ),
			TOPPING( "questL09Topping" ),
			GARBAGE( "questL10Garbage" ),
			MACGUFFIN( "questL11MacGuffin" ),
			BLACK( "questL11Black" ),
			WORSHIP( "questL11Worship" ),
			MANOR( "questL11Manor" ),
			PYRAMID( "questL11Pyramid" ),
			PALINDOME( "questL11Palindome" ),
			SHEN( "questL11Shen" ),
			RON( "questL11Ron" ),
			CURSES( "questL11Curses" ),
			DOCTOR( "questL11Doctor" ),
			BUSINESS( "questL11Business" ),
			SPARE( "questL11Spare" ),
			DESERT( "questL11Desert" ),
			ISLAND_WAR( "questL12War" ),
			FINAL( "questL13Final" ),
			WAREHOUSE( "questL13Warehouse" ),
			MEATCAR( "questG01Meatcar" ),
			CITADEL( "questG02Whitecastle" ),
			NEMESIS( "questG04Nemesis" ),
			MYST( "questG07Myst" ),
			MOXIE( "questG08Moxie" ),
			MUSCLE( "questG09Muscle" ),
			UNTINKER( "questM01Untinker" ),
			ARTIST( "questM02Artist" ),
			BUGBEAR( "questM03Bugbear" ),
			AZAZEL( "questM10Azazel" ),
			PIRATE( "questM12Pirate" ),
			ESCAPE( "questM13Escape" ),
			LOL( "questM15Lol" ),
			TEMPLE( "questM16Temple" ),
			SPOOKYRAVEN_BABIES( "questM17Babies" ),
			SWAMP( "questM18Swamp" ),
			HIPPY( "questM19Hippy" ),
			SPOOKYRAVEN_NECKLACE( "questM20Necklace" ),
			SPOOKYRAVEN_DANCE( "questM21Dance" ),
			SHIRT( "questM22Shirt" ),
			MEATSMITH( "questM23Meatsmith" ),
			DOC( "questM24Doc" ),
			ARMORER( "questM25Armorer" ),
			GENERATOR( "questF04Elves" ),
			CLANCY( "questF05Clancy" ),
			SEA_OLD_GUY( "questS01OldGuy" ),
			SEA_MONKEES( "questS02Monkees" ),
			JIMMY_MUSHROOM( "questESlMushStash" ),
			JIMMY_CHEESEBURGER( "questESlCheeseburger" ),
			JIMMY_SALT( "questESlSalt" ),
			TACO_DAN_AUDIT( "questESlAudit" ),
			TACO_DAN_COCKTAIL( "questESlCocktail" ),
			TACO_DAN_FISH( "questESlFish" ),
			BRODEN_BACTERIA( "questESlBacteria" ),
			BRODEN_SPRINKLES( "questESlSprinkles" ),
			BRODEN_DEBT( "questESlDebt" ),
			EVE( "questESpEVE" ),
			JUNGLE_PUN( "questESpJunglePun" ),
			GORE( "questESpGore" ),
			CLIPPER( "questESpClipper" ),
			FAKE_MEDIUM( "questESpFakeMedium" ),
			SERUM( "questESpSerum" ),
			SMOKES( "questESpSmokes" ),
			OUT_OF_ORDER( "questESpOutOfOrder" ),
			FISH_TRASH( "questEStFishTrash" ),
			GIVE_ME_FUEL( "questEStGiveMeFuel" ),
			NASTY_BEARS( "questEStNastyBears" ),
			SOCIAL_JUSTICE_I( "questEStSocialJusticeI" ),
			SOCIAL_JUSTICE_II( "questEStSocialJusticeII" ),
			SUPER_LUBER( "questEStSuperLuber" ),
			WORK_WITH_FOOD( "questEStWorkWithFood" ),
			ZIPPITY_DOO_DAH( "questEStZippityDooDah" ),
			BUCKET( "questECoBucket" ),
			TELEGRAM( "questLTTQuestByWire" ),
			ORACLE( "questM26Oracle" ),
			GHOST( "questPAGhost" ),
			;

		private final String pref;

		private Quest( String pref )
		{
			this.pref = pref;
		}

		public String getPref()
		{
			return this.pref;
		}

		public String getStatus()
		{
			return Preferences.getString( this.pref );
		}
	}

	public static final String UNSTARTED = "unstarted";
	public static final String STARTED = "started";
	public static final String FINISHED = "finished";

	public static final Pattern HTML_WHITESPACE = Pattern.compile( "<[^<]+?>|[\\s\\n]" );
	public static final Pattern BOO_PEAK_PATTERN = Pattern.compile( "It is currently (\\d+)%" );
	public static final Pattern OIL_PEAK_PATTERN = Pattern.compile( "The pressure is currently ([\\d\\.]+) microbowies" );
	public static final Pattern COMPETITION_PATTERN = Pattern.compile( "Contest #(\\d): ((\\d+) competitor|(Won!))" );
	public static final Pattern ORACLE_QUEST_PATTERN = Pattern.compile( "<b>(.*?)</b>" );
	public static final Pattern GHOST_QUEST_PATTERN = Pattern.compile( "<b>(.*?)</b>" );

	private static String[][] questLogData = null;
	private static String[][] councilData = null;

	static
	{
		reset();
	}

	public static void reset()
	{
		BufferedReader reader = FileUtilities.getVersionedReader( "questslog.txt", KoLConstants.QUESTSLOG_VERSION );

		ArrayList<String[]> quests = new ArrayList<String[]>();
		String[] data;

		while ( ( data = FileUtilities.readData( reader ) ) != null )
		{
			data[ 1 ] = data[ 1 ].replaceAll( "<Player\\sName>",
					KoLCharacter.getUserName() );
			quests.add( data );
		}

		questLogData = quests.toArray( new String[ quests.size() ][] );

		try
		{
			reader.close();
		}
		catch ( Exception e )
		{
			StaticEntity.printStackTrace( e );
		}

		reader = FileUtilities.getVersionedReader( "questscouncil.txt", KoLConstants.QUESTSCOUNCIL_VERSION );

		quests = new ArrayList<String[]>();

		while ( ( data = FileUtilities.readData( reader ) ) != null )
		{
			quests.add( data );
		}

		councilData = quests.toArray( new String[ quests.size() ][] );

		try
		{
			reader.close();
		}
		catch ( Exception e )
		{
			StaticEntity.printStackTrace( e );
		}
	}

	public static String titleToPref( final String title )
	{
		if ( title.contains( "White Citadel" ) )
		{
			// Hard code this quest, for now. The familiar name in the middle of the string is annoying to
			// deal with.
			return "questG02Whitecastle";
		}
		for ( int i = 0; i < questLogData.length; ++i )
		{
			// The title may contain other text, so check if quest title is contained in it
			if ( title.toLowerCase().contains( questLogData[ i ][ 1 ].toLowerCase() ) )
			{
				return questLogData[ i ][ 0 ];
			}
		}

		// couldn't find a match
		return "";
	}
	
	public static Quest titleToQuest( final String title )
	{
		String pref = titleToPref( title );
		if ( pref.equals( "" ) )
		{
			return null;
		}
		for ( Quest q: Quest.values() )
		{
			if ( q.getPref().equals( pref ) )
			{
				return q;
			}
		}
		return null;
	}

	public static String prefToTitle( final String pref )
	{
		for ( int i = 0; i < questLogData.length; ++i )
		{
			if ( questLogData[ i ][ 0 ].toLowerCase().contains( pref.toLowerCase() ) )
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
			if ( questLogData[ i ][ 0 ].toLowerCase().contains( pref.toLowerCase() ) )
			{
				return i;
			}
		}

		// couldn't find a match
		return -1;
	}

	public static String findQuestProgress( String pref, String details )
	{
		// Special handling due to multiple endings
		if ( pref.equals( Quest.ISLAND_WAR.getPref() ) )
		{
			return handleWarStatus( details );
		}
		if ( pref.equals( Quest.PIRATE.getPref() ) && details.contains( "Oh, and also you've managed to scam your way belowdecks, which is cool" ) )
		{
			// Hard code the end of the pirate quest, as it step 6 matches the final text also.
			return QuestDatabase.FINISHED;
		}
		if ( pref.equals( Quest.TOPPING.getPref() ) && details.contains( "The Highland Lord wants you to light" ) )
		{
			// this is step2.  We need to do some other handling for the three sub-parts.
			return handlePeakStatus( details );
		}
		if ( pref.equals( Quest.FINAL.getPref() ) )
		{
			handleCompetitionStatus( details );
		}
			// Special handling as there are nine sets of descriptions for the one quest
		if ( pref.equals( Quest.TELEGRAM.getPref() ) )
		{
			return handleTelegramStatus( details );
		}
			// Get Oracle quest target
		if ( pref.equals( Quest.ORACLE.getPref() ) )
		{
			Matcher matcher = QuestDatabase.ORACLE_QUEST_PATTERN.matcher( details );
			if ( matcher.find() )
			{
				Preferences.setString( "sourceOracleTarget", matcher.group( 1 ) );
			}
		}
			// Get Ghost quest target
		if ( pref.equals( Quest.GHOST.getPref() ) )
		{
			Matcher matcher = QuestDatabase.GHOST_QUEST_PATTERN.matcher( details );
			if ( matcher.find() )
			{
				Preferences.setString( "ghostLocation", matcher.group( 1 ) );
			}
		}

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
			if ( questLogData[ index ][ i ].contains( details ) )
			{
				foundAtStep = i - 2;
				break;
			}
		}

		if ( foundAtStep == -1 )
		{
			// Didn't manage to find an exact match. Now try stripping out all whitespace, newlines, and
			// anything that looks like html from questData and response. And make everything lower case,
			// because player names can be arbitrarily capitalized.
			String cleanedResponse = QuestDatabase.HTML_WHITESPACE.matcher( details ).replaceAll( "" )
				.toLowerCase();
			String cleanedQuest = "";

			for ( int i = 2; i < questLogData[ index ].length; ++i )
			{
				cleanedQuest = QuestDatabase.HTML_WHITESPACE.matcher( questLogData[ index ][ i ] )
					.replaceAll( "" ).toLowerCase();
				if ( cleanedQuest.contains( cleanedResponse ) )
				{
					foundAtStep = i - 2;
					break;
				}
			}
		}
		
		if ( foundAtStep == -1 )
		{
			// STILL haven't found a match. Try reversing the match, and chopping up the quest data into
			// substrings.
			String cleanedResponse = QuestDatabase.HTML_WHITESPACE.matcher( details ).replaceAll( "" )
				.toLowerCase();
			String cleanedQuest = "";
			String questStart = "";
			String questEnd = "";

			for ( int i = 2; i < questLogData[ index ].length; ++i )
			{
				cleanedQuest = QuestDatabase.HTML_WHITESPACE.matcher( questLogData[ index ][ i ] )
					.replaceAll( "" ).toLowerCase();

				if ( cleanedQuest.length() <= 100 )
				{
					questStart = cleanedQuest;
					questEnd = cleanedQuest;
				}
				else
				{
					questStart = cleanedQuest.substring( 0, 100 );
					questEnd = cleanedQuest.substring( cleanedQuest.length() - 100 );
				}

				if ( cleanedResponse.contains( questStart )
					|| cleanedResponse.contains( questEnd ) )
				{
					foundAtStep = i - 2;
					break;
				}
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
				return "step" + foundAtStep;
			}
		}

		// Well, none of the above worked. Punt.
		return "";
	}

	private static String handlePeakStatus( String details )
	{
		Matcher boo = QuestDatabase.BOO_PEAK_PATTERN.matcher( details );
		// boo peak handling.  100 is started, 0 is complete.
		if ( details.contains( "lit the fire on A-Boo Peak" ) )
		{
			Preferences.setInteger( "booPeakProgress", 0 );
		}
		else if ( details.contains( "check out A-Boo Peak" ) )
		{
			Preferences.setInteger( "booPeakProgress", 100 );
		}
		else if ( boo.find() )
		{
			Preferences.setInteger( "booPeakProgress", StringUtilities.parseInt( boo.group( 1 ) ) );
		}

		// twin peak handling
		// No information is present in the quest log between first starting the quest and completing 3/4 of it.  Boo.
		if ( details.contains( "lit the fire on Twin Peak" ) )
		{
			// twinPeakProgress is a bit field.  15 is complete.
			Preferences.setInteger( "twinPeakProgress", 15 );
		}

		Matcher oil = QuestDatabase.OIL_PEAK_PATTERN.matcher( details );
		// oil peak handling.  310.66 is started, 0 is complete.
		if ( details.contains( "lit the fire on Oil Peak" ) )
		{
			Preferences.setString( "oilPeakProgress", String.valueOf( 0 ) );
		}
		else if ( details.contains( "go to Oil Peak and investigate" ) )
		{
			Preferences.setString( "oilPeakProgress", String.valueOf( 310.66 ) );
		}
		else if ( oil.find() )
		{
			Preferences.setString( "oilPeakProgress", oil.group( 1 ) );
		}

		return "step2";
	}

	private static String handleWarStatus( String details )
	{
		if ( details.contains( "You led the filthy hippies to victory" )
			|| details.contains( "You led the Orcish frat boys to victory" )
			|| details.contains( "You started a chain of events" ) )
		{
			return QuestDatabase.FINISHED;
		}
		else if ( details.contains( "You've managed to get the war between the hippies and frat boys started" ) )
		{
			return "step1";
		}
		else if ( details.contains( "The Council has gotten word of tensions building between the hippies and the frat boys" ) )
		{
			return QuestDatabase.STARTED;
		}

		return "";
	}

	private static void handleCompetitionStatus( String details )
	{
		Matcher competition = QuestDatabase.COMPETITION_PATTERN.matcher( details );
		while ( competition.find() )
		{
			String preference = "nsContestants" + StringUtilities.parseInt( competition.group( 1 ) );
			int left = -1;
			if( competition.group( 2 ).equals( "Won!" ) )
			{
				left = 0;
			}
			else
			{
				left = StringUtilities.parseInt( competition.group( 3 ) );
			}
			Preferences.setInteger( preference, left );
		}
		if ( details.contains( "Ascend the <a href=place.php?whichplace=nstower class=nounder target=mainpane><b>Naughty Sorceress' Tower</b></a>." ) )
		{
			QuestDatabase.setQuestIfBetter( Quest.FINAL, "step6" );
		}
	}

	private static String handleTelegramStatus( String details )
	{
		// It's kludgy, but there are nine different texts for the one quest title, so the existing code can't handle it
		// Missing: Fancy Man
		if ( details.contains( "Ask around the Rough Diamond Saloon to see if anybody has seen Jeff the Fancy Dude." ) )
		{
			Preferences.setString( "lttQuestName", "Missing: Fancy Man" );
			Preferences.setInteger( "lttQuestDifficulty", 1 );
			return "step1";
		}
		if ( details.contains( "Trek across the desert to Jeff's mining claim." ) )
		{
			Preferences.setString( "lttQuestName", "Missing: Fancy Man" );
			Preferences.setInteger( "lttQuestDifficulty", 1 );
			return "step2";
		}
		if ( details.contains( "Delve deeper into Jeff's Fancy Mine." ) )
		{
			Preferences.setString( "lttQuestName", "Missing: Fancy Man" );
			Preferences.setInteger( "lttQuestDifficulty", 1 );
			return "step3";
		}
		if ( details.contains( "Defeat Jeff the Fancy Skeleton." ) )
		{
			Preferences.setString( "lttQuestName", "Missing: Fancy Man" );
			Preferences.setInteger( "lttQuestDifficulty", 1 );
			return "step4";
		}
		// Missing: Pioneer Daughter
		if ( details.contains( "Search for Daisy's homestead." ) )
		{
			Preferences.setString( "lttQuestName", "Missing: Pioneer Daughter" );
			Preferences.setInteger( "lttQuestDifficulty", 1 );
			return "step1";
		}
		if ( details.contains( "Question the cultists in Bloodmilk Cave." ) )
		{
			Preferences.setString( "lttQuestName", "Missing: Pioneer Daughter" );
			Preferences.setInteger( "lttQuestDifficulty", 1 );
			return "step2";
		}
		if ( details.contains( "Fight your way through Daisy's Fortress." ) )
		{
			Preferences.setString( "lttQuestName", "Missing: Pioneer Daughter" );
			Preferences.setInteger( "lttQuestDifficulty", 1 );
			return "step3";
		}
		if ( details.contains( "Defeat Daisy the Unclean." ) )
		{
			Preferences.setString( "lttQuestName", "Missing: Pioneer Daughter" );
			Preferences.setInteger( "lttQuestDifficulty", 1 );
			return "step4";
		}
		// Help!  Desperados!
		if ( details.contains( "Clear some of the criminals out of Spitback." ) )
		{
			Preferences.setString( "lttQuestName", "Help!  Desperados!" );
			Preferences.setInteger( "lttQuestDifficulty", 1 );
			return "step1";
		}
		if ( details.contains( "Find your way to Pecos Dave's hideout." ) )
		{
			Preferences.setString( "lttQuestName", "Help!  Desperados!" );
			Preferences.setInteger( "lttQuestDifficulty", 1 );
			return "step2";
		}
		if ( details.contains( "Find Pecos Dave in his mine hideout." ) )
		{
			Preferences.setString( "lttQuestName", "Help!  Desperados!" );
			Preferences.setInteger( "lttQuestDifficulty", 1 );
			return "step3";
		}
		if ( details.contains( "Defeat Pecos Dave." ) )
		{
			Preferences.setString( "lttQuestName", "Help!  Desperados!" );
			Preferences.setInteger( "lttQuestDifficulty", 1 );
			return "step4";
		}
		// Haunted Boneyard
		if ( details.contains( "Find the pastor in his church." ) )
		{
			Preferences.setString( "lttQuestName", "Haunted Boneyard" );
			Preferences.setInteger( "lttQuestDifficulty", 2 );
			return "step1";
		}
		if ( details.contains( "Investigate the local cemetery." ) )
		{
			Preferences.setString( "lttQuestName", "Haunted Boneyard" );
			Preferences.setInteger( "lttQuestDifficulty", 2 );
			return "step2";
		}
		if ( details.contains( "Clear out the ancient cow burial ground." ) )
		{
			Preferences.setString( "lttQuestName", "Haunted Boneyard" );
			Preferences.setInteger( "lttQuestDifficulty", 2 );
			return "step3";
		}
		if ( details.contains( "Defeat Amoon-Ra Cowtep." ) )
		{
			Preferences.setString( "lttQuestName", "Haunted Boneyard" );
			Preferences.setInteger( "lttQuestDifficulty", 2 );
			return "step4";
		}
		// Big Gambling Tournament Announced
		if ( details.contains( "Fight your way through the crowd at the gambling tournament." ) )
		{
			Preferences.setString( "lttQuestName", "Big Gambling Tournament Announced" );
			Preferences.setInteger( "lttQuestDifficulty", 2 );
			return "step1";
		}
		if ( details.contains( "Escape from the snake pit!" ) )
		{
			Preferences.setString( "lttQuestName", "Big Gambling Tournament Announced" );
			Preferences.setInteger( "lttQuestDifficulty", 2 );
			return "step2";
		}
		if ( details.contains( "Track down Snakeye Glenn at the Great Western hotel." ) )
		{
			Preferences.setString( "lttQuestName", "Big Gambling Tournament Announced" );
			Preferences.setInteger( "lttQuestDifficulty", 2 );
			return "step3";
		}
		if ( details.contains( "Defeat Snakeeye Glenn." ) )
		{
			Preferences.setString( "lttQuestName", "Big Gambling Tournament Announced" );
			Preferences.setInteger( "lttQuestDifficulty", 2 );
			return "step4";
		}
		// Sheriff Wanted
		if ( details.contains( "Fight your way to the sheriff's office and apply for the job." ) )
		{
			Preferences.setString( "lttQuestName", "Sheriff Wanted" );
			Preferences.setInteger( "lttQuestDifficulty", 2 );
			return "step1";
		}
		if ( details.contains( "Head up river to the Placid Lake Gang's hideout." ) )
		{
			Preferences.setString( "lttQuestName", "Sheriff Wanted" );
			Preferences.setInteger( "lttQuestDifficulty", 2 );
			return "step2";
		}
		if ( details.contains( "Search the hideout for the gang's leader." ) )
		{
			Preferences.setString( "lttQuestName", "Sheriff Wanted" );
			Preferences.setInteger( "lttQuestDifficulty", 2 );
			return "step3";
		}
		if ( details.contains( "Defeat Former Sheriff Dan Driscoll." ) )
		{
			Preferences.setString( "lttQuestName", "Sheriff Wanted" );
			Preferences.setInteger( "lttQuestDifficulty", 2 );
			return "step4";
		}
		// Madness at the Mine
		if ( details.contains( "Figure out what's going wrong at the mine." ) )
		{
			Preferences.setString( "lttQuestName", "Madness at the Mine" );
			Preferences.setInteger( "lttQuestDifficulty", 3 );
			return "step1";
		}
		if ( details.contains( "Search the desert for the missing foreman." ) )
		{
			Preferences.setString( "lttQuestName", "Madness at the Mine" );
			Preferences.setInteger( "lttQuestDifficulty", 3 );
			return "step2";
		}
		if ( details.contains( "Find that door in the mine again." ) )
		{
			Preferences.setString( "lttQuestName", "Madness at the Mine" );
			Preferences.setInteger( "lttQuestDifficulty", 3 );
			return "step3";
		}
		if ( details.contains( "Defeat the unusual construct." ) )
		{
			Preferences.setString( "lttQuestName", "Madness at the Mine" );
			Preferences.setInteger( "lttQuestDifficulty", 3 );
			return "step4";
		}
		// Missing: Many Children
		if ( details.contains( "Find out why the children are going missing." ) )
		{
			Preferences.setString( "lttQuestName", "Missing: Many Children" );
			Preferences.setInteger( "lttQuestDifficulty", 3 );
			return "step1";
		}
		if ( details.contains( "Ride the ghost train." ) )
		{
			Preferences.setString( "lttQuestName", "Missing: Many Children" );
			Preferences.setInteger( "lttQuestDifficulty", 3 );
			return "step2";
		}
		if ( details.contains( "Search Cowtown for the missing children." ) )
		{
			Preferences.setString( "lttQuestName", "Missing: Many Children" );
			Preferences.setInteger( "lttQuestDifficulty", 3 );
			return "step3";
		}
		if ( details.contains( "Defeat Clara." ) )
		{
			Preferences.setString( "lttQuestName", "Missing: Many Children" );
			Preferences.setInteger( "lttQuestDifficulty", 3 );
			return "step4";
		}
		// Wagon Train Escort Wanted
		if ( details.contains( "Escort the Hackleton wagon train across the desert." ) )
		{
			Preferences.setString( "lttQuestName", "Wagon Train Escort Wanted" );
			Preferences.setInteger( "lttQuestDifficulty", 3 );
			return "step1";
		}
		if ( details.contains( "Defend the Hackleton wagon train!" ) )
		{
			Preferences.setString( "lttQuestName", "Wagon Train Escort Wanted" );
			Preferences.setInteger( "lttQuestDifficulty", 3 );
			return "step2";
		}
		if ( details.contains( "Defeat the Hackletons." ) )
		{
			Preferences.setString( "lttQuestName", "Wagon Train Escort Wanted" );
			Preferences.setInteger( "lttQuestDifficulty", 3 );
			return "step3";
		}
		if ( details.contains( "Defeat Granny Hackleton." ) ) // Check this works!
		{
			Preferences.setString( "lttQuestName", "Wagon Train Escort Wanted" );
			Preferences.setInteger( "lttQuestDifficulty", 3 );
			return "step4";
		}
		return "";
	}

	public static void setQuestProgress( Quest quest, String progress )
	{
		if ( quest == null )
		{
			return;
		}
		QuestDatabase.setQuestProgress( quest.getPref(), progress );
	}

	public static void setQuestProgress( String pref, String status )
	{
		if ( prefToIndex( pref ) == -1 )
		{
			return;
		}

		if ( !status.equals( QuestDatabase.STARTED ) && !status.equals( QuestDatabase.FINISHED )
			&& !status.contains( "step" ) && !status.equals( QuestDatabase.UNSTARTED ) )
		{
			return;
		}
		Preferences.setString( pref, status );
	}

	public static void resetQuests()
	{
		for ( int i = 0; i < questLogData.length; ++i )
		{
			// Don't reset Spring Beach Break quests
			if ( !questLogData[ i ][ 0 ].startsWith( "questESl" ) )
			{
				QuestDatabase.setQuestProgress( questLogData[ i ][ 0 ], QuestDatabase.UNSTARTED );
			}
		}
		Preferences.resetToDefault( "manorDrawerCount" );
		Preferences.resetToDefault( "poolSkill" );
		Preferences.resetToDefault( "currentExtremity" );
		Preferences.resetToDefault( "chasmBridgeProgress" );
		Preferences.resetToDefault( "oilPeakProgress" );
		Preferences.resetToDefault( "twinPeakProgress" );
		Preferences.resetToDefault( "booPeakProgress" );
		Preferences.resetToDefault( "desertExploration" );
		Preferences.resetToDefault( "zeppelinProtestors" );
		Preferences.resetToDefault( "middleChamberUnlock" );
		Preferences.resetToDefault( "lowerChamberUnlock" );
		Preferences.resetToDefault( "controlRoomUnlock" );
		Preferences.resetToDefault( "hiddenApartmentProgress" );
		Preferences.resetToDefault( "hiddenHospitalProgress" );
		Preferences.resetToDefault( "hiddenOfficeProgress" );
		Preferences.resetToDefault( "hiddenBowlingAlleyProgress" );
		Preferences.resetToDefault( "blackForestProgress" );
		Preferences.resetToDefault( "nsChallenge1" );
		Preferences.resetToDefault( "nsChallenge2" );
		Preferences.resetToDefault( "nsChallenge3" );
		Preferences.resetToDefault( "nsChallenge4" );
		Preferences.resetToDefault( "nsChallenge5" );
		Preferences.resetToDefault( "nsContestants1" );
		Preferences.resetToDefault( "nsContestants2" );
		Preferences.resetToDefault( "nsContestants3" );
		Preferences.resetToDefault( "nsTowerDoorKeysUsed" );
		Preferences.resetToDefault( "maraisDarkUnlock" );
		Preferences.resetToDefault( "maraisWildlifeUnlock" );
		Preferences.resetToDefault( "maraisCorpseUnlock" );
		Preferences.resetToDefault( "maraisWizardUnlock" );
		Preferences.resetToDefault( "maraisBeaverUnlock" );
		Preferences.resetToDefault( "maraisVillageUnlock" );
		Preferences.resetToDefault( "burnoutsDefeated" );
		Preferences.resetToDefault( "corralUnlocked" );
		Preferences.resetToDefault( "kolhsTotalSchoolSpirited" );
		Preferences.resetToDefault( "haciendaLayout" );
		Preferences.resetToDefault( "spookyravenRecipeUsed" );
		Preferences.resetToDefault( "controlPanel1" );
		Preferences.resetToDefault( "controlPanel2" );
		Preferences.resetToDefault( "controlPanel3" );
		Preferences.resetToDefault( "controlPanel4" );
		Preferences.resetToDefault( "controlPanel5" );
		Preferences.resetToDefault( "controlPanel6" );
		Preferences.resetToDefault( "controlPanel7" );
		Preferences.resetToDefault( "controlPanel8" );
		Preferences.resetToDefault( "controlPanel9" );
		Preferences.resetToDefault( "controlPanelOmega" );
		Preferences.resetToDefault( "SHAWARMAInitiativeUnlocked" );
		Preferences.resetToDefault( "canteenUnlocked" );
		Preferences.resetToDefault( "armoryUnlocked" );
		Preferences.resetToDefault( "writingDesksDefeated" );
		Preferences.resetToDefault( "palindomeDudesDefeated" );
		Preferences.resetToDefault( "warehouseProgress" );
		Preferences.resetToDefault( "dinseyAudienceEngagement" );
		Preferences.resetToDefault( "dinseyFilthLevel" );
		Preferences.resetToDefault( "dinseyFunProgress" );
		Preferences.resetToDefault( "dinseyGatorStenchDamage" );
		Preferences.resetToDefault( "dinseyGarbagePirate" );
		Preferences.resetToDefault( "dinseyNastyBearsDefeated" );
		Preferences.resetToDefault( "dinseyRapidPassEnabled" );
		Preferences.resetToDefault( "dinseyRollercoasterNext" );
		Preferences.resetToDefault( "dinseyRollercoasterStats" );
		Preferences.resetToDefault( "dinseySafetyProtocolsLoose" );
		Preferences.resetToDefault( "dinseySocialJusticeIProgress" );
		Preferences.resetToDefault( "dinseySocialJusticeIIProgress" );
		Preferences.resetToDefault( "dinseyTouristsFed" );
		Preferences.resetToDefault( "dinseyToxicMultiplier" );
		Preferences.resetToDefault( "walfordBucketItem" );
		Preferences.resetToDefault( "walfordBucketProgress" );
		Preferences.resetToDefault( "lttQuestDifficulty" );
		Preferences.resetToDefault( "lttQuestName" );
		Preferences.resetToDefault( "lttQuestStageCount" );
	}

	public static void handleCouncilText( String responseText )
	{
		// First, tokenize by <p> (or <P>, if the HTML happened to be coded by a doofus) tags in the responseText, since there can be multiple quests we need to set.
		// This ultimately means that each quest gets set n times when it has n paragraphs - technically weird, but not really an issue other than the minor disk I/O.

		String[] responseTokens = responseText.split( "<[pP]>" );
		String cleanedResponseToken = "";
		String cleanedQuestToken = "";

		for ( String responseToken : responseTokens )
		{
			cleanedResponseToken = QuestDatabase.HTML_WHITESPACE.matcher( responseToken ).replaceAll( "" ).toLowerCase();
			for ( int i = 0; i < councilData.length; ++i )
			{
				for ( int j = 2; j < councilData[ i ].length; ++j )
				{
					// Now, we have to split the councilData entry by <p> tags too.
					// Assume that no two paragraphs are identical, otherwise more loop termination logic is needed.

					String[] councilTokens = councilData[ i ][ j ].split( "<[pP]>" );

					for ( String councilToken : councilTokens )
					{
						cleanedQuestToken = QuestDatabase.HTML_WHITESPACE.matcher( councilToken ).replaceAll( "" ).toLowerCase();

						if ( cleanedResponseToken.contains( cleanedQuestToken ) )
						{
							setQuestIfBetter( councilData[ i ][ 0 ], councilData[ i ][ 1 ] );
							break;
						}
					}
				}
			}
		}
	}

	public static void setQuestIfBetter( Quest quest, String progress )
	{
		if ( quest == null )
		{
			return;
		}
		QuestDatabase.setQuestIfBetter( quest.getPref(), progress );
	}

	private static void setQuestIfBetter( String pref, String status )
	{
		String currentStatus = Preferences.getString( pref );
		boolean shouldSet = false;

		if ( currentStatus.equals( QuestDatabase.UNSTARTED ) )
		{
			shouldSet = true;
		}
		else if ( currentStatus.equals( QuestDatabase.STARTED ) )
		{
			if ( status.startsWith( "step" ) || status.equals( QuestDatabase.FINISHED ) )
			{
				shouldSet = true;
			}
		}
		else if ( currentStatus.startsWith( "step" ) )
		{
			if ( status.equals( QuestDatabase.FINISHED ) )
			{
				shouldSet = true;
			}
			else if ( status.startsWith( "step" ) )
			{
				try
				{
					int currentStep = StringUtilities.parseInt( currentStatus.substring( 4 ) );
					int nextStep = StringUtilities.parseInt( status.substring( 4 ) );

					if ( nextStep > currentStep )
					{
						shouldSet = true;
					}
				}
				catch ( NumberFormatException e )
				{
					shouldSet = true;
				}
			}
		}
		else if ( currentStatus.equals( QuestDatabase.FINISHED ) )
		{
			shouldSet = false;
		}
		else
		{
			// there was something garbled in the preference. overwrite it.
			shouldSet = true;
		}
		
		if ( shouldSet )
		{
			QuestDatabase.setQuestProgress( pref, status );
		}
	}

	public static boolean isQuestStep( Quest quest, String second )
	{
		if ( quest == null )
		{
			return false;
		}
		return Preferences.getString( quest.getPref() ).equals( second );
	}

	public static boolean isQuestLaterThan( Quest quest, String second )
	{
		if ( quest == null )
		{
			return false;
		}
		return QuestDatabase.isQuestLaterThan( Preferences.getString( quest.getPref() ), second );
	}

	public static boolean isQuestLaterThan( String first, String second )
	{
		if ( first.equals( QuestDatabase.UNSTARTED ) )
		{
			return false;
		}
		else if ( first.equals( QuestDatabase.STARTED ) )
		{
			return second.equals( QuestDatabase.UNSTARTED );
		}
		else if ( first.startsWith( "step" ) )
		{
			if ( second.equals( QuestDatabase.FINISHED ) )
			{
				return false;
			}
			else if ( second.startsWith( "step" ) )
			{
				try
				{
					int currentStepInt = StringUtilities.parseInt( first.substring( 4 ) );
					int compareToStepInt = StringUtilities.parseInt( second.substring( 4 ) );

					if ( currentStepInt > compareToStepInt )
					{
						return true;
					}
					else
					{
						// step we're comparing to is equal or greater
						return false;
					}
				}
				catch ( NumberFormatException e )
				{
					return false;
				}
			}
			else
			{
				return true;
			}
		}
		else if ( first.equals( QuestDatabase.FINISHED ) )
		{
			return !second.equals( QuestDatabase.FINISHED );
		}

		return false;
	}

	public static boolean isQuestFinished( Quest quest )
	{
		if ( quest == null )
		{
			return false;
		}
		return Preferences.getString( quest.getPref() ).equals( QuestDatabase.FINISHED );
	}

	public static String questStepAfter( Quest quest, String step )
	{
		if ( quest == null )
		{
			return "";
		}
		return QuestDatabase.questStepAfter( quest.getPref(), step );
	}

	public static String questStepAfter( String pref, String step )
	{
		// First thing to do is find which quest we're talking about.
		int index = prefToIndex( pref );
		if ( index == -1 )
		{
			return "";
		}

		// Next, find the number of quest steps
		final int totalSteps = questLogData[ index ].length - 2;
		if ( totalSteps < 1 )
		{
			return "";
		}

		if ( step.equals( QuestDatabase.UNSTARTED ) )
		{
			return QuestDatabase.STARTED;
		}

		if ( step.equals( QuestDatabase.STARTED ) )
		{
			if ( totalSteps > 2 )
			{
				return "step1";
			}
			else
			{
				return QuestDatabase.FINISHED;
			}
		}

		if ( step.startsWith( "step" ) )
		{
			try
			{
				int currentStep = StringUtilities.parseInt( step.substring( 4 ) );
				int nextStep = currentStep + 1;

				if ( nextStep >= totalSteps )
				{
					return QuestDatabase.FINISHED;
				}
				else
				{
					return "step" + nextStep;
				}
			}
			catch ( NumberFormatException e )
			{
				return "";
			}
		}

		if ( step.equals( QuestDatabase.FINISHED ) )
		{
			return "";
		}

		return "";
	}

	public static void advanceQuest( Quest quest  )
	{
		if ( quest == null )
		{
			return;
		}
		QuestDatabase.advanceQuest( quest.getPref() );
	}

	public static void advanceQuest( String pref )
	{
		String currentStep = Preferences.getString( pref );
		String nextStep = QuestDatabase.questStepAfter( pref, currentStep );
		QuestDatabase.setQuestProgress( pref, nextStep );
	}
}
