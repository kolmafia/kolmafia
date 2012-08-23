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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestLogger;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.swingui.RequestFrame;

import net.sourceforge.kolmafia.webui.IslandDecorator;

public class IslandArenaRequest
	extends GenericRequest
{
	private static final Pattern ID_PATTERN = Pattern.compile( "(bigisland|postwarisland).php.*?action=concert.*?option=(\\d+)" );

	public static final String[][] HIPPY_CONCERTS =
	{
		{ "Moon'd", "+5 Stat(s) Per Fight" },
		{ "Dilated Pupils", "Item Drop +20%" },
		{ "Optimist Primal", "Familiar Weight +5" },
	};

	public static final String[][] FRATBOY_CONCERTS =
	{
		{ "Elvish", "All Attributes +10%" },
		{ "Winklered", "Meat Drop +40%" },
		{ "White-boy Angst", "Initiative +50%" },
	};

	private static String quest = "";
	private int option = 0;
	private String error = null;

	public IslandArenaRequest( final int option )
	{
		super( chooseUrl() );
		if ( this.getPath().equals( "bogus.php" ) )
		{
			setError();
			return;
		}

		this.addFormField( "action", "concert" );

		if ( option < 0 || option > 3 )
		{
			this.error = "Invalid concert selected";
			return;
		}

		this.option = option;
		this.addFormField( "option", String.valueOf( option ) );
	}

	public IslandArenaRequest( final String effect )
	{
		super( chooseUrl() );
		if ( this.getPath().equals( "bogus.php" ) )
		{
			setError();
			return;
		}

		String [][] array = IslandArenaRequest.quest.equals( "hippies" ) ? HIPPY_CONCERTS : FRATBOY_CONCERTS;

		for ( int i = 0; i < array.length; ++i )
		{
			if ( array[i][0].toLowerCase().startsWith( effect.toLowerCase() ) )
			{
				this.option = i + 1;
				break;
			}
		}

		if ( this.option == 0 )
		{
			this.error = "That effect not available to " + IslandArenaRequest.quest;
			return;
		}

		this.addFormField( "action", "concert" );
		this.addFormField( "option", String.valueOf( this.option ) );
	}

	private static String chooseUrl()
	{
		IslandDecorator.ensureUpdatedBigIsland();
		IslandArenaRequest.quest = IslandDecorator.questCompleter( "sidequestArenaCompleted" );
		if ( !IslandArenaRequest.quest.equals( "none" ) )
		{
			String loser = Preferences.getString( "sideDefeated" );
			if ( loser.equals( "neither" ) )
				return "bigisland.php";
			if ( !loser.equals( IslandArenaRequest.quest ) )
				return "postwarisland.php";
		}
		return "bogus.php";
	}

	private void setError()
	{
		// If he has not yet finished the sidequest, say so
		if ( IslandArenaRequest.quest.equals( "none" ) )
		{
			this.error = "Arena not open";
		}
		else
		{
			// Otherwise, he won the war for the wrong side
			this.error = "Arena's fans defeated in war";
		}
	}

	@Override
	protected boolean retryOnTimeout()
	{
		return true;
	}

	@Override
	public void run()
	{
		if ( this.error != null )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, this.error );
			return;
		}

		KoLmafia.updateDisplay( "Visiting the Mysterious Island Arena..." );
		super.run();
	}

	@Override
	public void processResults()
	{
		if ( this.responseText == null || this.responseText.equals( "" ) )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "You can't find the Mysterious Island Arena." );
			return;
		}

		IslandArenaRequest.parseResponse( this.getURLString(), this.responseText );

		// Unfortunately, you think you've pretty much tapped out this
		// event's entertainment potential for today
		//
		// You're all rocked out.

		if ( this.responseText.indexOf( "pretty much tapped out" ) != -1 ||
		     this.responseText.indexOf( "You're all rocked out" ) != -1 )
		{
			KoLmafia.updateDisplay( "You can only visit the Mysterious Island Arena once a day." );
			return;
		}

		KoLmafia.updateDisplay( "A music lover is you." );
		RequestFrame.refreshStatus();
	}

	public static final void parseResponse( final String location, final String responseText )
	{
		if ( location.indexOf( "action=concert" ) != -1 )
		{
			Preferences.setBoolean( "concertVisited", true );
		}
	}

	public static final boolean registerRequest( final String location )
	{
		Matcher matcher = IslandArenaRequest.ID_PATTERN.matcher( location );

		if ( !matcher.find() )
		{
			return false;
		}

		RequestLogger.updateSessionLog( "concert " + matcher.group( 2 ) );
		return true;
	}
}
