/**
 * Copyright (c) 2005-2014, KoLmafia development team
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
import net.sourceforge.kolmafia.KoLmafia;
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
			MYST( "questG07Myst" ),
			MEATCAR( "questG01Meatcar" ),
			CITADEL( "questG02Whitecastle" ),
			ARTIST( "questM02Artist" ),
			GALAKTIK( "questM04Galaktic" ),
			AZAZEL( "questM10Azazel" ),
			PIRATE( "questM12Pirate" ),
			GENERATOR( "questF04Elves" ),
			BUGBEAR( "questM03Bugbear" ),
			UNTINKER( "questM01Untinker" ),
			LOL( "questM15Lol" ),
			SPOOKYRAVEN_NECKLACE( "questM20Necklace" ),
			SPOOKYRAVEN_DANCE( "questM21Dance" ),
			SPOOKYRAVEN_BABIES( "questM17Babies" ),
			SWAMP( "questM18Swamp" ),
			HIPPY( "questM19Hippy" ),
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
			BRODEN_DEBT( "questESlDebt" );

		private String pref;

		private Quest( String pref )
		{
			this.pref = pref;
		}

		public String getPref()
		{
			return pref;
		}
	}

	public static final String UNSTARTED = "unstarted";
	public static final String STARTED = "started";
	public static final String FINISHED = "finished";

	public static final Pattern HTML_WHITESPACE = Pattern.compile( "<[^<]+?>|[\\s\\n]" );
	public static final Pattern BOO_PEAK_PATTERN = Pattern.compile( "It is currently (\\d+)%" );
	public static final Pattern OIL_PEAK_PATTERN = Pattern.compile( "The pressure is currently ([\\d\\.]+) microbowies" );

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
			if ( questLogData[ i ][ 0 ].toLowerCase().indexOf( pref.toLowerCase() ) != -1 )
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
			if ( questLogData[ i ][ 0 ].toLowerCase().indexOf( pref.toLowerCase() ) != -1 )
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
		if ( pref.equals( "questL12War" ) )
		{
			return handleWarStatus( details );
		}
		if ( pref.equals( "questG04Nemesis" ) && details.contains( "Demonic Lord of Revenge" ) )
		{
			// Hard code the end of the nemesis quest, for now. We could eventually programmatically handle
			// the <demon name> in the response.
			return QuestDatabase.FINISHED;
		}
		if ( pref.equals( "questM12Pirate" ) && details.contains( "Oh, and also you've managed to scam your way belowdecks, which is cool" ) )
		{
			// Hard code the end of the pirate quest, as it step 6 matches the final text also.
			return QuestDatabase.FINISHED;
		}
		if ( pref.equals( Quest.TOPPING.getPref() ) && details.contains( "The Highland Lord wants you to light" ) )
		{
			// this is step2.  We need to do some other handling for the three sub-parts.
			return handlePeakStatus( details );
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
			if ( questLogData[ index ][ i ].indexOf( details ) != -1 )
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
				if ( cleanedQuest.indexOf( cleanedResponse ) != -1 )
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

				if ( cleanedResponse.indexOf( questStart ) != -1
					|| cleanedResponse.indexOf( questEnd ) != -1 )
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
		if ( details.indexOf( "You led the filthy hippies to victory" ) != -1
			|| details.indexOf( "You led the Orcish frat boys to victory" ) != -1
			|| details.indexOf( "You started a chain of events" ) != -1 )
		{
			return QuestDatabase.FINISHED;
		}
		else if ( details.indexOf( "You've managed to get the war between the hippies and frat boys started" ) != -1 )
		{
			return "step1";
		}
		else if ( details
			.indexOf( "The Council has gotten word of tensions building between the hippies and the frat boys" ) != -1 )
		{
			return QuestDatabase.STARTED;
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
			&& status.indexOf( "step" ) == -1 && !status.equals( QuestDatabase.UNSTARTED ) )
		{
			return;
		}
		Preferences.setString( pref, status );
	}

	public static void resetQuests()
	{
		for ( int i = 0; i < questLogData.length; ++i )
		{
			// Don't reset Elemental Plane quests
			if ( !questLogData[ i ][ 0 ].startsWith( "questE" ) )
			{
				QuestDatabase.setQuestProgress( questLogData[ i ][ 0 ], QuestDatabase.UNSTARTED );
			}
		}
		Preferences.resetToDefault( "manorDrawerCount" );
		Preferences.resetToDefault( "poolSkill" );
		Preferences.resetToDefault( "currentExtremity" );
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
		Preferences.resetToDefault( "maraisDarkUnlock" );
		Preferences.resetToDefault( "maraisWildlifeUnlock" );
		Preferences.resetToDefault( "maraisCorpseUnlock" );
		Preferences.resetToDefault( "maraisWizardUnlock" );
		Preferences.resetToDefault( "maraisBeaverUnlock" );
		Preferences.resetToDefault( "maraisVillageUnlock" );
		Preferences.resetToDefault( "corralUnlocked" );
		Preferences.resetToDefault( "kolhsTotalSchoolSpirited" );
		Preferences.resetToDefault( "haciendaLayout" );
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

						if ( cleanedResponseToken.indexOf( cleanedQuestToken ) != -1 )
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
