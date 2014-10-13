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

	private static boolean dungeonOfDoomAvailable = false;

	private static final Pattern HEADER_PATTERN = Pattern.compile( "<b>([^<]*?[^>]*?)</b>(?:<p>|)<blockquote>", Pattern.DOTALL );
	private static final Pattern BODY_PATTERN = Pattern.compile( "(?<=<b>)(.*?[^<>]*?)</b><br>(.*?)(?=<p>$|<p><b>|<p></blockquote>)", Pattern.DOTALL );
	private static final Pattern SEAHORSE_PATTERN = Pattern.compile( "You have tamed the mighty seahorse <b>(.*?)</b>", Pattern.DOTALL );

	private static final Object[][] questlogDemons =
	{
		{
		//	"Summoning Chamber",
			Pattern.compile( ";&middot;([^<]*?), Lord of the Pies<br" , Pattern.DOTALL ),
			"demonName1",
		},
		{
		//	"Hoom Hah",
			Pattern.compile( ";&middot;([^<]*?), the Deadest Beat<br" , Pattern.DOTALL ),
			"demonName2",
		},
		{
		//	"Every Seashell Has a Story to Tell If You're Listening",
			Pattern.compile( ";&middot;([^<]*?), the Ancient Fishlord<br" , Pattern.DOTALL ),			
			"demonName3",
		},
		{
		//	"Leavesdropping",
			Pattern.compile( ";&middot;([^<]*?), Duke of the Underworld<br" , Pattern.DOTALL ),
			"demonName4",
		},
		{
		//	"These Pipes... Aren't Clean!",
			Pattern.compile( ";&middot;([^<]*?), the Stankmaster<br" , Pattern.DOTALL ),
			"demonName5",
		},
		{
		//	"Flying In Circles",
			Pattern.compile( ";&middot;([^<]*?), the Demonic Lord of Revenge<br" , Pattern.DOTALL ),
			"demonName8",
		},
		{
		//	"Sinister Ancient Tablet",
			Pattern.compile( ";&middot;([^<]*?), the Smith<br" , Pattern.DOTALL ),
			"demonName9",
		},
		{
		//	"Strange Cube",
			Pattern.compile( ";&middot;([^<]*?[^<]), the Pain Enjoyer<br" , Pattern.DOTALL ),
			"demonName10",
		},
		{
		//	Where Have All The Drunkards Gone?
			Pattern.compile( ";&middot;([^<]*?[^<]), Friend of Gary<br" , Pattern.DOTALL ),
			"demonName11",
		},
	};

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

		return pref.equals( QuestDatabase.FINISHED ) || pref.contains( "step" );
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
		if ( urlString.contains( "which=1" ) || urlString.contains( "which=7" ) )
		{
			parseResponse( responseText, 1 );
		}

		else if ( urlString.contains( "which=2" ) )
		{
			parseResponse( responseText, 2 );

			GalaktikRequest.setDiscount( QuestLogRequest.finishedQuest( Quest.GALAKTIK.getPref() ) );
		}

		else if ( urlString.contains( "which=3" ) )
		{
			ChatManager.setChatLiteracy( responseText.contains( QuestLogRequest.ALTAR_OF_LITERACY ) );
			QuestLogRequest.dungeonOfDoomAvailable = responseText.contains( QuestLogRequest.DUNGEONS_OF_DOOM );
			
			Matcher matcher = QuestLogRequest.SEAHORSE_PATTERN.matcher( responseText );
			if ( matcher.find() )
			{
				Preferences.setString( "seahorseName", new String( matcher.group(1) ) );
			}

			registerDemonName( responseText );
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
			// First time I opened this today it said Miscellaneous quests, now says Other quests, so check for both
			else if ( header.equals( "Other Quests:" ) || header.equals( "Miscellaneous Quests:" ) )
			{
				handleQuestText( cut, source );
			}
			else
			{
				// encountered a section in questlog we don't know how to handle.
			}
		}

		// Some quests vanish when completed but can be inferred by the presence of a new one
		if ( QuestDatabase.isQuestLaterThan( Quest.SPOOKYRAVEN_BABIES, QuestDatabase.UNSTARTED ) )
		{
			QuestDatabase.setQuestProgress( Quest.SPOOKYRAVEN_DANCE, QuestDatabase.FINISHED );
		}
		if ( QuestDatabase.isQuestLaterThan( Quest.SPOOKYRAVEN_DANCE, QuestDatabase.UNSTARTED ) )
		{
			QuestDatabase.setQuestProgress( Quest.SPOOKYRAVEN_NECKLACE, QuestDatabase.FINISHED );
		}
		if ( QuestDatabase.isQuestLaterThan( Quest.MACGUFFIN, "step1" ) )
		{
			QuestDatabase.setQuestProgress( Quest.BLACK, QuestDatabase.FINISHED );
		}
		if ( QuestDatabase.isQuestLaterThan( Quest.WORSHIP, "step3" ) )
		{
			QuestDatabase.setQuestProgress( Quest.CURSES, QuestDatabase.FINISHED );
			QuestDatabase.setQuestProgress( Quest.DOCTOR, QuestDatabase.FINISHED );
			QuestDatabase.setQuestProgress( Quest.BUSINESS, QuestDatabase.FINISHED );
			QuestDatabase.setQuestProgress( Quest.SPARE, QuestDatabase.FINISHED );
		}
		if ( QuestDatabase.isQuestLaterThan( Quest.PYRAMID, QuestDatabase.UNSTARTED ) )
		{
			QuestDatabase.setQuestProgress( Quest.DESERT, QuestDatabase.FINISHED );
		}

		// Set (mostly historical) preferences we can set based on quest status
		if ( QuestDatabase.isQuestLaterThan( Quest.MACGUFFIN, "step1" ) ||
			QuestDatabase.isQuestFinished( Quest.MEATCAR ) )
		{
			Preferences.setInteger( "lastDesertUnlock", KoLCharacter.getAscensions() );
		}
		if ( QuestDatabase.isQuestLaterThan( Quest.ISLAND_WAR, QuestDatabase.STARTED ) ||
			QuestDatabase.isQuestLaterThan( Quest.PIRATE, QuestDatabase.UNSTARTED ) )
		{
			Preferences.setInteger( "lastIslandUnlock", KoLCharacter.getAscensions() );
		}
		if ( QuestDatabase.isQuestFinished( Quest.SPOOKYRAVEN_NECKLACE ) )
		{
			Preferences.setInteger( "lastSecondFloorUnlock", KoLCharacter.getAscensions() );
		}
		if ( QuestDatabase.isQuestLaterThan( Quest.GARBAGE, "step7" ) )
		{
			Preferences.setInteger( "lastCastleGroundUnlock", KoLCharacter.getAscensions() );
		}
		if ( QuestDatabase.isQuestLaterThan( Quest.GARBAGE, "step8" ) )
		{
			Preferences.setInteger( "lastCastleTopUnlock", KoLCharacter.getAscensions() );
		}
		if ( QuestDatabase.isQuestLaterThan( Quest.WORSHIP, "step1" ) )
		{
			Preferences.setInteger( "lastTempleButtonsUnlock", KoLCharacter.getAscensions() );
			Preferences.setInteger( "lastTempleUnlock", KoLCharacter.getAscensions() );
		}
		Preferences.setBoolean( "middleChamberUnlock", 
			QuestDatabase.isQuestLaterThan( Quest.PYRAMID, QuestDatabase.STARTED ) );
		Preferences.setBoolean( "lowerChamberUnlock", 
			QuestDatabase.isQuestLaterThan( Quest.PYRAMID, "step1" ) );
		Preferences.setBoolean( "controlRoomUnlock", 
			QuestDatabase.isQuestLaterThan( Quest.PYRAMID, "step2" ) );
		Preferences.setBoolean( "pyramidBombUsed", 
			QuestDatabase.isQuestFinished( Quest.PYRAMID ) );
		Preferences.setBoolean( "bigBrotherRescued", 
			QuestDatabase.isQuestLaterThan( Quest.SEA_MONKEES, "step1" ) );
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
		return QuestDatabase.isQuestLaterThan( Quest.RAT, QuestDatabase.STARTED );
	}

	public static final boolean registerDemonName( final String responseText )
	{
		for ( int i = 0; i < QuestLogRequest.questlogDemons.length; ++i )
		{
			Object [] demons = QuestLogRequest.questlogDemons[ i ];
			if ( Preferences.getString( (String) demons[ 1 ] ).equals( "" ) )
			{
				Pattern pattern = (Pattern) demons[ 0 ];
				Matcher matcher = pattern.matcher( responseText );

				if ( matcher.find() )
				{
					// We found this demon
					Preferences.setString( (String) demons[ 1 ], matcher.group( 1 ) );
				}
			}
		}
		return true;
	}
}
