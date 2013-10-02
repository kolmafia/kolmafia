/**
 * Copyright (c) 2005-2013, KoLmafia development team
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

import net.sourceforge.kolmafia.persistence.QuestDatabase;
import net.sourceforge.kolmafia.persistence.QuestDatabase.Quest;

import net.sourceforge.kolmafia.preferences.Preferences;

public class FriarRequest
	extends GenericRequest
{
	private int option = 0;

	private static final Pattern ID_PATTERN = Pattern.compile( "action=buffs.*?bro=(\\d+)" );

	public static final String[] BLESSINGS = { "food", "familiar", "booze", };

	public FriarRequest( final int option )
	{
		super( "friars.php" );

		this.addFormField( "action", "buffs" );
		if ( option >= 1 && option <= 3 )
		{
			this.option = option;
			this.addFormField( "bro", String.valueOf( option ) );
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
		if ( this.option == 0 )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "Decide which friar to visit." );
			return;
		}

		KoLmafia.updateDisplay( "Visiting the Deep Fat Friars..." );
		super.run();
	}

	@Override
	public void processResults()
	{
		if ( this.responseText == null || this.responseText.equals( "" ) )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "You can't find the Deep Fat Friars." );
			return;
		}

		FriarRequest.parseResponse( this.getURLString(), this.responseText );

		if ( this.responseText.indexOf( "one of those per day." ) != -1 )
		{
			KoLmafia.updateDisplay( "You can only get one blessing a day from the Deep Fat Friars." );
			return;
		}

		KoLmafia.updateDisplay( "You've been blessed." );
	}

	public static final void parseResponse( final String location, final String responseText )
	{
		// No, seriously, you can only get one of those per day.
		// Brother <name> smiles and rubs some ashes on your face.
		if ( responseText.indexOf( "one of those per day." ) != -1 ||
		     responseText.indexOf( "smiles and rubs some ashes" ) != -1 )
		{
			Preferences.setBoolean( "friarsBlessingReceived", true );
			Preferences.setInteger( "lastFriarCeremonyAscension", Preferences.getInteger( "knownAscensions" ));
			QuestDatabase.setQuestProgress( Quest.FRIAR, QuestDatabase.FINISHED );
		}
	}

	public static final boolean registerRequest( final String location )
	{
		if ( !location.startsWith( "friars.php" ) )
		{
			return false;
		}

		Matcher matcher = FriarRequest.ID_PATTERN.matcher( location );

		if ( !matcher.find() )
		{
			return true;
		}

		RequestLogger.updateSessionLog( "friars blessing " + matcher.group( 1 ) );
		return true;
	}
}
