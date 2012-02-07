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

import net.sourceforge.kolmafia.chat.ChatManager;

import net.sourceforge.kolmafia.persistence.QuestDatabase;

import net.sourceforge.kolmafia.preferences.Preferences;

public class QuestLogRequest
	extends GenericRequest
{
	private static final String GALAKTIK = "What's Up, Doc?";
	private static final String CITADEL1 = "You have discovered the legendary White Citadel";
	private static final String CITADEL2 = "You've got the Satisfaction Satchel";
	private static final String CITADEL3 = "you can now shop at White Citadel";

	private static final String FRIAR = "Trial By Friar";

	private static final String BLACK_MARKET_STRING_1 =
		"now to hit the Travel Agency and get yourself on a slow boat to China";
	private static final String BLACK_MARKET_STRING_2 =
		"You've picked up your father's diary, and things just got a whole lot more complicated";
	private static final String MACGUFFIN = "Quest for the Holy MacGuffin";

	private static final String ISLAND_WAR = "Make War, Not...";
	private static final String ISLAND_WAR_STRING =
		"You've managed to get the war between the hippies and frat boys started, and now the Council wants you to finish it.";

	private static final String ALTAR_OF_LITERACY =
		"You have proven yourself literate.";
	private static final String DUNGEONS_OF_DOOM =
		"You have discovered the secret of the Dungeons of Doom.";
	private static final String HAX0R =
		"You have summoned the UB3r 31337 HaX0R";

	private static String started = "";
	private static String finished = "";
	private static String other = "";

	private static boolean dungeonOfDoomAvailable = false;

	private static boolean whiteCitadelAvailable = false;
	private static boolean friarsAvailable = false;
	private static boolean blackMarketAvailable = false;
	private static boolean hippyStoreAvailable = false;
	
	private static final Pattern HEADER_PATTERN = Pattern.compile(  "<b>([^<]*?[^>]*?)</b><p><blockquote>", Pattern.DOTALL );
	private static final Pattern BODY_PATTERN = Pattern.compile( "(?<=<b>)(.*?[^<>]*?)</b><br>(.*?)(?=<p>)", Pattern.DOTALL );

	public QuestLogRequest()
	{
		super( "questlog.php" );
	}

	private static final boolean startedQuest( final String quest )
	{
		return QuestLogRequest.started.indexOf( quest ) != -1;
	}

	private static final boolean finishedQuest( final String quest )
	{
		return QuestLogRequest.finished.indexOf( quest ) != -1;
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
		return QuestLogRequest.whiteCitadelAvailable;
	}

	public static final void setWhiteCitadelAvailable()
	{
		QuestLogRequest.whiteCitadelAvailable = true;
	}

	public static final boolean areFriarsAvailable()
	{
		return QuestLogRequest.friarsAvailable;
	}

	public static final void setFriarsAvailable()
	{
		QuestLogRequest.friarsAvailable = true;
	}

	public static final boolean isBlackMarketAvailable()
	{
		if ( Preferences.getInteger( "lastWuTangDefeated" ) == KoLCharacter.getAscensions() )
		{
			QuestLogRequest.blackMarketAvailable = false;
		}

		return QuestLogRequest.blackMarketAvailable;
	}

	public static final void setBlackMarketAvailable()
	{
		QuestLogRequest.blackMarketAvailable = true;
	}

	public static final boolean isHippyStoreAvailable()
	{
		return QuestLogRequest.hippyStoreAvailable;
	}

	public static final void setHippyStoreAvailability( final boolean available )
	{
		QuestLogRequest.hippyStoreAvailable = available;
	}

	public void run()
	{
		// When KoL provides a link to the Quest log, it goes to the
		// section you visited last. Therefore, visit all sections but
		// end with page 1.

		this.addFormField( "which", "3" );
		super.run();

		if ( this.responseText != null )
		{
			QuestLogRequest.registerQuests( false, this.getURLString(), this.responseText );
		}

		this.addFormField( "which", "2" );
		super.run();

		if ( this.responseText != null )
		{
			QuestLogRequest.registerQuests( false, this.getURLString(), this.responseText );
		}

		this.addFormField( "which", "1" );
		super.run();

		if ( this.responseText != null )
		{
			QuestLogRequest.registerQuests( false, this.getURLString(), this.responseText );
		}

		QuestLogRequest.blackMarketAvailable =
			QuestLogRequest.startedQuest( QuestLogRequest.BLACK_MARKET_STRING_1 ) ||
			QuestLogRequest.startedQuest( QuestLogRequest.BLACK_MARKET_STRING_2 ) ||
			QuestLogRequest.finishedQuest( QuestLogRequest.MACGUFFIN );
		QuestLogRequest.hippyStoreAvailable =
			!QuestLogRequest.startedQuest( QuestLogRequest.ISLAND_WAR_STRING ) ||
			QuestLogRequest.finishedQuest( QuestLogRequest.ISLAND_WAR );

		QuestLogRequest.friarsAvailable = QuestLogRequest.finishedQuest( QuestLogRequest.FRIAR );
	}

	protected boolean retryOnTimeout()
	{
		return true;
	}

	public static final void registerQuests( final boolean isExternal, final String urlString, final String responseText )
	{
		if ( urlString.indexOf( "which=1" ) != -1 )
		{
			QuestLogRequest.started = responseText;

			if ( isExternal )
			{
				QuestLogRequest.blackMarketAvailable |=
					QuestLogRequest.startedQuest( QuestLogRequest.BLACK_MARKET_STRING_1 ) ||
					QuestLogRequest.startedQuest( QuestLogRequest.BLACK_MARKET_STRING_2 );
				QuestLogRequest.hippyStoreAvailable &=
					!QuestLogRequest.startedQuest( QuestLogRequest.ISLAND_WAR_STRING );
			}
			
			parseResponse( responseText, 1 );
		}

		if ( urlString.indexOf( "which=2" ) != -1 )
		{
			QuestLogRequest.finished = responseText;

			GalaktikRequest.setDiscount( QuestLogRequest.finishedQuest( QuestLogRequest.GALAKTIK ) );
			QuestLogRequest.whiteCitadelAvailable = 
				QuestLogRequest.finishedQuest( QuestLogRequest.CITADEL3 ) || 
				QuestLogRequest.finishedQuest( QuestLogRequest.CITADEL2 ) || 
				QuestLogRequest.finishedQuest( QuestLogRequest.CITADEL1 );
			QuestLogRequest.friarsAvailable = QuestLogRequest.finishedQuest( QuestLogRequest.FRIAR );


			if ( isExternal )
			{
				QuestLogRequest.blackMarketAvailable |=
					QuestLogRequest.finishedQuest( QuestLogRequest.MACGUFFIN );
				QuestLogRequest.hippyStoreAvailable |= QuestLogRequest.finishedQuest( QuestLogRequest.ISLAND_WAR );
			}
			
			parseResponse( responseText, 2 );
		}

		if ( urlString.indexOf( "which=3" ) != -1 )
		{
			QuestLogRequest.other = responseText;

			ChatManager.setChatLiteracy( QuestLogRequest.other.indexOf( QuestLogRequest.ALTAR_OF_LITERACY ) != -1 );
			QuestLogRequest.dungeonOfDoomAvailable = QuestLogRequest.other.indexOf( QuestLogRequest.DUNGEONS_OF_DOOM ) != -1;
			HermitRequest.ensureUpdatedHermit();
			Preferences.setBoolean( "hermitHax0red", QuestLogRequest.other.indexOf( QuestLogRequest.HAX0R ) != -1 );
			HermitRequest.resetConcoctions();
		}
	}

	private static void parseResponse( final String responseText, final int source )
	{
		Matcher headers = QuestLogRequest.HEADER_PATTERN.matcher( responseText );
		HashMap map = new HashMap();

		while ( headers.find() )
		{
			map.put( new Integer( headers.end() ), headers.group( 1 ) );
		}
		
		Iterator it = map.keySet().iterator();
		while ( it.hasNext() )
		{
			Integer key = (Integer) it.next();
			String header = (String) map.get( key );
			String cut = responseText.substring( key.intValue() ).split( "</blockquote>" )[ 0 ];

			if ( header.equals( "Council Quests:" ) )
			{
				handleCouncilQuestText( cut, source );
			}
			else if ( header.equals( "Guild Quests:" ) )
			{
				handleGuildQuestText( cut, source );
			}
			else if ( header.equals( "Miscellaneous Quests:" ) )
			{
				// Handling for Misc. here!

			}
			else
			{
				// encountered a section in questlog we don't know how to handle.
			}
		}
	}

	private static void handleCouncilQuestText( String response, int source )
	{
		Matcher body = QuestLogRequest.BODY_PATTERN.matcher( response );
		// Form of.. a regex! group(1) now contains the quest title and group(2) has the details.
		while ( body.find() )
		{
			String title = body.group( 1 );
			String details =  body.group( 2 );
			String pref = QuestDatabase.titleToPreference( title );
			String status = "";
			/*
			 * questL02Larva
			 * Looking for a Larva in All the Wrong Places
			 * [Current] The Council of Loathing wants you to bring them a mosquito larva, for some reason.
			 * They told you to look for one in the Spooky Forest, in the Distant Woods.
			 * 
			 * How can a woods contain a forest? Suspension of disbelief, that's how.
			 * 
			 * [Completed] You delivered a mosquito larva to the Council of Loathing. Nice work!
			 */
			if ( pref.equals( "questL02Larva" ) )
			{
				if ( source == 2)
				{
					status = QuestDatabase.FINISHED;
				}
				else if ( details.indexOf( "wants you to bring them a mosquito" ) != -1 )
				{
					status = QuestDatabase.STARTED;
				}
				else if ( details.indexOf( "delivered a mosquito larva" ) != -1 )
				{
					status = QuestDatabase.FINISHED;
				}
			}
			

			// questL03Rat
			// else if...

			// questL04Bat
			// else if...

			// questL05Goblin

			// questL06Friar
			
			// questL07Cyrptic
			
			// questL08Trapper
			
			// questL09Lol
			
			// questL10Garbage
			
			// questL11MacGuffin
			
			// questL11Worship
			
			// questL11Manor
			
			// questL11Palindome
			
			// questL11Pyramid
			
			// questL12War
			
			// questL13Final
			
			// When we've implemented everything, do some error checking to make sure we handled everything
			// successfully.
			
/*			if ( pref.equals( "" ) || status.equals( "" ) )
			{
				KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "Something went wrong while parsing questlog.php" );
				return;
			}
			if ( source == 2 && !status.equals( "finished" ) )
			{
				//shouldn't happen.  We were parsing the completed quests page but somehow didn't set a quest to finished.
				KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "Something went wrong while parsing completed quests" );
				return;
			}*/
			
			// Debugging
			
			/*if ( !pref.equals( "" ) )
			{
				RequestLogger.printLine( pref + " (" + status + ")" );
			}
			else
			{
				RequestLogger.printLine( "unhandled: " + title );
			}*/			
			// Finally, set preference.
			// Preferences.setString( pref, status );
		}
	}
	
	private static void handleGuildQuestText( String response, int source )
	{
		Matcher body = QuestLogRequest.BODY_PATTERN.matcher( response );
		while ( body.find() )
		{
			String title = body.group( 1 );
			String details =  body.group( 2 );
			String pref = QuestDatabase.titleToPreference( title );
			String status = "";
			
			// questG01Meatcar
			
			/*
			 * questG02Whitecastle
			 * <Player Name> and <Familiar Name>/Kumar Go To White Citadel
			 * 
			 * [Current] You've been charged by your Guild (sort of) with the task of bringing back a
			 * delicious meal from the legendary White Citadel. You've been told it's somewhere near
			 * Whitey's Grove, in the Distant Woods.
			 * (In between) You've discovered the road from Whitey's Grove to the legendary White Citadel.
			 * You should explore it and see if you can find your way.
			 * (In between) You're progressing down the road towards the White Citadel, but you'll need to
			 * find something that can help you get past that stupid cheetah if you're going to make it any
			 * further. Keep looking around.
			 * (In between) You've made your way further down the Road to the White Citadel, but you still
			 * haven't found it. Keep looking!
			 * (In between) You've found the White Citadel, but it's at the bottom of a huge cliff. You
			 * should keep messing around on the Road until you find a way to get down the cliff.
			 * (In between) You have discovered the legendary White Citadel. You should probably go in there
			 * and get the carryout order you were trying to get in the first place. Funny how things spiral
			 * out of control, isn't it?
			 * (In between) You've got the Satisfaction Satchel. Take it to your contact in your Guild for a
			 * reward.
			 * [Completed] You've delivered a satchel of incredibly greasy food to someone you barely know.
			 * Plus, you can now shop at White Citadel whenever you want. Awesome!
			 */
			if ( pref.equals( "questG02Whitecastle" ) )
			{
				if ( source == 2)
				{
					status = QuestDatabase.FINISHED;
				}
				else if ( details.indexOf( "bringing back a delicious meal" ) != -1 )
				{
					status = QuestDatabase.STARTED;
				}
				else if ( details.indexOf( "You've discovered the road from Whitey's Grove"  ) != -1 )
				{
					status = QuestDatabase.STEP1;
				}
				else if ( details.indexOf( "You're progressing down the road towards the White Citadel"  ) != -1 )
				{
					status = QuestDatabase.STEP2;
				}
				else if ( details.indexOf( "You've made your way further down the Road to the White Citadel"  ) != -1 )
				{
					status = QuestDatabase.STEP3;
				}
				else if ( details.indexOf( "You've found the White Citadel, but it's at"  ) != -1 )
				{
					status = QuestDatabase.STEP4;
				}
				else if ( details.indexOf( "You have discovered the legendary White Citadel"  ) != -1 )
				{
					status = QuestDatabase.STEP5;
				}
				else if ( details.indexOf( "You've got the Satisfaction Satchel"  ) != -1 )
				{
					status = QuestDatabase.STEP7;
				}
				else if ( details.indexOf( "delivered a mosquito larva" ) != -1 )
				{
					status = QuestDatabase.FINISHED;
				}
			}
			
			
			// questG03Ego

			// questG04Nemesis

			// questG05Dark

			// questG06Delivery
			
			// When we've implemented everything, do some error checking to make sure we handled everything
			// successfully.
			
/*			if ( pref.equals( "" ) || status.equals( "" ) )
			{
				KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "Something went wrong while parsing questlog.php" );
				return;
			}
			if ( source == 2 && !status.equals( "finished" ) )
			{
				//shouldn't happen.  We were parsing the completed quests page but somehow didn't set a quest to finished.
				KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "Something went wrong while parsing completed quests" );
				return;
			}*/
			
			// Debugging
			
			/*if ( !pref.equals( "" ) )
			{
				RequestLogger.printLine( pref + " (" + status + ")" );
			}
			else
			{
				RequestLogger.printLine( "unhandled: " + title );
			}*/
			
			// Finally, set preference.
			// Preferences.setString( pref, status );
		}
	}
}
