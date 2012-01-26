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

import net.sourceforge.kolmafia.KoLCharacter;

import net.sourceforge.kolmafia.chat.ChatManager;

import net.sourceforge.kolmafia.preferences.Preferences;

public class QuestLogRequest
	extends GenericRequest
{
	private static final String GALAKTIK = "What's Up, Doc?";
	private static final String CITADEL = "White Citadel";

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
		}

		if ( urlString.indexOf( "which=2" ) != -1 )
		{
			QuestLogRequest.finished = responseText;

			GalaktikRequest.setDiscount( QuestLogRequest.finishedQuest( QuestLogRequest.GALAKTIK ) );
			QuestLogRequest.whiteCitadelAvailable = QuestLogRequest.finishedQuest( QuestLogRequest.CITADEL );
			QuestLogRequest.friarsAvailable = QuestLogRequest.finishedQuest( QuestLogRequest.FRIAR );


			if ( isExternal )
			{
				QuestLogRequest.blackMarketAvailable |=
					QuestLogRequest.finishedQuest( QuestLogRequest.MACGUFFIN );
				QuestLogRequest.hippyStoreAvailable |= QuestLogRequest.finishedQuest( QuestLogRequest.ISLAND_WAR );
			}
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
}
