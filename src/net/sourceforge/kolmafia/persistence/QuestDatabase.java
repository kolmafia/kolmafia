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

import java.io.BufferedReader;

import java.util.ArrayList;

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
	public static final String UNSTARTED = "unstarted";
	public static final String STARTED = "started";
	public static final String FINISHED = "finished";

	public static final String LARVA = "questL02Larva";
	public static final String RAT = "questL03Rat";
	public static final String BAT = "questL04Bat";
	public static final String GOBLIN = "questL05Goblin";
	public static final String FRIAR = "questL06Friar";
	public static final String CYRPT = "questL07Cyrptic";
	public static final String TRAPPER = "questL08Trapper";
	public static final String LOL = "questL09Lol";
	public static final String GARBAGE = "questL10Garbage";
	public static final String MACGUFFIN = "questL11MacGuffin";
	public static final String WORSHIP = "questL11Worship";
	public static final String MANOR = "questL11Manor";
	public static final String PYRAMID = "questL11Pyramid";
	public static final String PALINDOME = "questL11Palindome";
	public static final String ISLAND_WAR = "questL12War";
	public static final String FINAL = "questL13Final";
	public static final String CITADEL = "questG02Whitecastle";
	public static final String ARTIST = "questM02Artist";
	public static final String GALAKTIK = "questM04Galaktic";
	public static final String AZAZEL = "questM10Azazel";
	public static final String PIRATE = "questM12Pirate";
	public static final String GENERATOR = "questF04Elves";
	public static final String BUGBEAR = "questM03Bugbear";
	public static final String UNTINKER = "questM01Untinker";

	public static final Pattern HTML_WHITESPACE = Pattern.compile( "<[^<]+?>|[\\s\\n]" );

	private static String[][] questLogData = null;
	private static String[][] councilData = null;
	
	static
	{
		reset();
	}
	
	public static void reset()
	{
		BufferedReader reader = FileUtilities.getVersionedReader( "questslog.txt", KoLConstants.QUESTSLOG_VERSION );
		
		ArrayList quests = new ArrayList();
		String[] data;

		while ( ( data = FileUtilities.readData( reader ) ) != null )
		{
			data[ 1 ] = data[ 1 ].replaceAll( "<Player\\sName>",
					KoLCharacter.getUserName() );
			quests.add( data );
		}
		
		questLogData = ( String[][] )quests.toArray( new String[ quests.size() ][] );
		
		try
		{
			reader.close();
		}
		catch ( Exception e )
		{
			StaticEntity.printStackTrace( e );
		}
		
		reader = FileUtilities.getVersionedReader( "questscouncil.txt", KoLConstants.QUESTSCOUNCIL_VERSION );
		
		quests = new ArrayList();

		while ( ( data = FileUtilities.readData( reader ) ) != null )
		{
			quests.add( data );
		}
		
		councilData = ( String[][] )quests.toArray( new String[ quests.size() ][] );
		
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
		if ( title.indexOf( "White Citadel" ) != -1 )
		{
			// Hard code this quest, for now. The familiar name in the middle of the string is annoying to
			// deal with.
			return "questG02Whitecastle";
		}
		for ( int i = 0; i < questLogData.length; ++i )
		{
			if ( questLogData[ i ][ 1 ].toLowerCase().indexOf( title.toLowerCase() ) != -1 )
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
		if ( pref.equals( "questG04Nemesis" ) && details.indexOf( "Demonic Lord of Revenge" ) != -1 )
		{
			// Hard code the end of the nemesis quest, for now. We could eventually programmatically handle
			// the <demon name> in the response.
			return QuestDatabase.FINISHED;
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
			QuestDatabase.setQuestProgress( questLogData[ i ][ 0 ], QuestDatabase.UNSTARTED );
		}
	}

	public static void handleCouncilText( String responseText )
	{
		String cleanedResponse = QuestDatabase.HTML_WHITESPACE.matcher( responseText ).replaceAll( "" )
			.toLowerCase();
		String cleanedQuest = "";

		String pref = "";
		String status = "";

		boolean found = false;
		for ( int i = 0; i < councilData.length && !found; ++i )
		{
			for ( int j = 2; j < councilData[ i ].length && !found; ++j )
			{
				cleanedQuest = QuestDatabase.HTML_WHITESPACE.matcher( councilData[ i ][ j ] )
					.replaceAll( "" ).toLowerCase();
				if ( cleanedResponse.indexOf( cleanedQuest ) != -1 )
				{
					pref = councilData[ i ][ 0 ];
					status = councilData[ i ][ 1 ];
					found = true;
				}
			}
		}

		if ( !found )
		{
			String questStart = "";
			String questEnd = "";

			for ( int i = 0; i < councilData.length && !found; ++i )
			{
				for ( int j = 2; j < councilData[ i ].length && !found; ++j )
				{
					cleanedQuest = QuestDatabase.HTML_WHITESPACE.matcher( councilData[ i ][ j ] )
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
						pref = councilData[ i ][ 0 ];
						status = councilData[ i ][ 1 ];
						found = true;
					}
				}
			}
		}

		if ( found )
		{
			setQuestIfBetter( pref, status );
		}
	}

	public static void setQuestIfBetter( String pref, String status )
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
}
