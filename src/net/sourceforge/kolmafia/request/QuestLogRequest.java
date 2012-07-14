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

package net.sourceforge.kolmafia.request;

import java.util.HashMap;
import java.util.Iterator;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLmafia;

import net.sourceforge.kolmafia.chat.ChatManager;

import net.sourceforge.kolmafia.objectpool.IntegerPool;

import net.sourceforge.kolmafia.persistence.QuestDatabase;
import net.sourceforge.kolmafia.persistence.QuestDatabase.Quest;

import net.sourceforge.kolmafia.preferences.Preferences;


public class QuestLogRequest
	extends GenericRequest
{
	private static final String ALTAR_OF_LITERACY =
		"You have proven yourself literate.";
	private static final String DUNGEONS_OF_DOOM =
		"You have discovered the secret of the Dungeons of Doom.";

	private static String other = "";

	private static boolean dungeonOfDoomAvailable = false;

	private static final Pattern HEADER_PATTERN = Pattern.compile(  "<b>([^<]*?[^>]*?)</b><p><blockquote>", Pattern.DOTALL );
	private static final Pattern BODY_PATTERN = Pattern.compile( "(?<=<b>)(.*?[^<>]*?)</b><br>(.*?)(?=<p>)", Pattern.DOTALL );

	public QuestLogRequest()
	{
		super( "questlog.php" );
	}

	private static final boolean finishedQuest( final String pref )
	{
		return Preferences.getString( pref ).equals( QuestDatabase.FINISHED );
	}

	public static final boolean galaktikCuresAvailable()
	{
		return GalaktikRequest.getDiscount();
	}

	public static final boolean isDungeonOfDoomAvailable()
	{
		return QuestLogRequest.dungeonOfDoomAvailable;
	}

	public static final void setDungeonOfDoomAvailable()
	{
		QuestLogRequest.dungeonOfDoomAvailable = true;
	}

	public static final boolean isWhiteCitadelAvailable()
	{
		String pref = Preferences.getString( Quest.CITADEL.getPref() );
		return pref.equals( QuestDatabase.FINISHED ) || pref.equals( "step5" ) || pref.equals( "step6" );
	}

	public static final boolean areFriarsAvailable()
	{
		return Preferences.getString( Quest.FRIAR.getPref() ).equals( QuestDatabase.FINISHED );
	}

	public static final boolean isBlackMarketAvailable()
	{
		if ( Preferences.getInteger( "lastWuTangDefeated" ) == KoLCharacter.getAscensions() )
		{
			return false;
		}
		String pref = Preferences.getString( Quest.MACGUFFIN.getPref() );

		return pref.equals( QuestDatabase.FINISHED ) || pref.indexOf( "step" ) != -1;
	}

	public static final boolean isHippyStoreAvailable()
	{
		return !Preferences.getString( Quest.ISLAND_WAR.getPref() ).equals( "step1" );
	}

	@Override
	public void run()
	{
		KoLmafia.updateDisplay( "Retrieving quest data..." );
		// When KoL provides a link to the Quest log, it goes to the
		// section you visited last. Therefore, visit all sections but
		// end with page 1.

		this.addFormField( "which", "3" );
		super.run();

		this.addFormField( "which", "2" );
		super.run();

		this.addFormField( "which", "1" );
		super.run();

	}

	@Override
	protected boolean retryOnTimeout()
	{
		return true;
	}

	public static final void registerQuests( final boolean isExternal, final String urlString, final String responseText )
	{
		if ( urlString.indexOf( "which=" ) == -1 )
		{
			for ( int i = 1; i < 4; ++i )
			{
				if ( responseText.indexOf( "questlog.php?which=" + i ) == -1 )
				{
					parseResponse( responseText, i );
					break;
				}
			}
		}

		else if ( urlString.indexOf( "which=1" ) != -1 )
		{
			parseResponse( responseText, 1 );
		}

		else if ( urlString.indexOf( "which=2" ) != -1 )
		{
			parseResponse( responseText, 2 );

			GalaktikRequest.setDiscount( QuestLogRequest.finishedQuest( Quest.GALAKTIK.getPref() ) );
		}

		else if ( urlString.indexOf( "which=3" ) != -1 )
		{
			QuestLogRequest.other = responseText;

			ChatManager.setChatLiteracy( QuestLogRequest.other.indexOf( QuestLogRequest.ALTAR_OF_LITERACY ) != -1 );
			QuestLogRequest.dungeonOfDoomAvailable = QuestLogRequest.other.indexOf( QuestLogRequest.DUNGEONS_OF_DOOM ) != -1;
		}
	}

	private static void parseResponse( final String responseText, final int source )
	{
		Matcher headers = QuestLogRequest.HEADER_PATTERN.matcher( responseText );
		HashMap<Integer, String> map = new HashMap<Integer, String>();

		while ( headers.find() )
		{
			map.put( IntegerPool.get( headers.end() ), headers.group( 1 ) );
		}

		Iterator<Integer> it = map.keySet().iterator();
		while ( it.hasNext() )
		{
			Integer key = it.next();
			String header = map.get( key );
			String cut = responseText.substring( key.intValue() ).split( "</blockquote>" )[ 0 ];

			if ( header.equals( "Council Quests:" ) )
			{
				handleQuestText( cut, source );
			}
			else if ( header.equals( "Guild Quests:" ) )
			{
				handleQuestText( cut, source );
			}
			else if ( header.equals( "Miscellaneous Quests:" ) )
			{
				handleQuestText( cut, source );
			}
			else
			{
				// encountered a section in questlog we don't know how to handle.
			}
		}
	}

	private static void handleQuestText( String response, int source )
	{
		Matcher body = QuestLogRequest.BODY_PATTERN.matcher( response );
		// Form of.. a regex! group(1) now contains the quest title and group(2) has the details.
		while ( body.find() )
		{
			String title = body.group( 1 );
			String details = body.group( 2 );
			String pref = QuestDatabase.titleToPref( title );
			String status = "";

			status = QuestDatabase.findQuestProgress( pref, details );

			// Debugging

			/*if ( !pref.equals( "" ) )
			{
				RequestLogger.printLine( pref + " (" + status + ")" );
			}
			else
			{
				RequestLogger.printLine( "unhandled: " + title );
			}*/

			// Once we've implemented everything, we can do some error checking to make sure we handled everything
			// successfully.

			if ( pref.equals( "" ) )
			{
				/*KoLmafia.updateDisplay( KoLConstants.CONTINUE_STATE,
					"Unknown quest, or something went wrong while parsing questlog.php" );*/
				continue;
			}
			if ( status.equals( "" ) )
			{
				/*KoLmafia.updateDisplay( KoLConstants.CONTINUE_STATE,
					"Unknown quest status found while parsing questlog.php" );*/
				continue;
			}
			/*
			 * if ( source == 2 && !status.equals( "finished" ) )
			 * {
			 * // Probably shouldn't happen. We were parsing the completed quests page but somehow didn't set a quest
			 * to finished.  Possible exception happens during nemesis quest.
			 * KoLmafia.updateDisplay( KoLConstants.ERROR_STATE,
			 * "Something went wrong while parsing completed quests" );
			 * return;
			 * }
			 */

			QuestDatabase.setQuestProgress( pref, status );
		}
	}

	public static boolean isTavernAvailable()
	{
		if ( Preferences.getString( Quest.RAT.getPref() ).equals( QuestDatabase.STARTED )
			|| Preferences.getString( Quest.RAT.getPref() ).equals( QuestDatabase.FINISHED ) )
		{
			return true;
		}
		return false;
	}
}
