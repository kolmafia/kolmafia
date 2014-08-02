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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestLogger;

import net.sourceforge.kolmafia.persistence.QuestDatabase;
import net.sourceforge.kolmafia.persistence.QuestDatabase.Quest;

import net.sourceforge.kolmafia.preferences.Preferences;

public class MomRequest
	extends GenericRequest
{
	private int option = 0;

	private static final Pattern ID_PATTERN = Pattern.compile( "action=mombuff.*?whichbuff=(\\d+)" );

	public static final String[] FOOD = { "hot", "cold", "stench", "spooky", "sleaze", "critical", "stats", };

	public MomRequest( final int option )
	{
		super( "monkeycastle.php" );

		this.addFormField( "action", "mombuff" );
		if ( option >= 1 && option <= 7 )
		{
			this.option = option;
			this.addFormField( "whichbuff", String.valueOf( option ) );
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
			KoLmafia.updateDisplay( MafiaState.ERROR, "Decide which food to get." );
			return;
		}

		KoLmafia.updateDisplay( "Visiting Mom..." );
		super.run();
	}

	@Override
	public void processResults()
	{
		if ( this.responseText == null || this.responseText.equals( "" ) )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "You can't get to Mom Sea Monkee" );
			return;
		}

		MomRequest.parseResponse( this.getURLString(), this.responseText );

		if ( this.responseText.indexOf( "one of those per day." ) != -1 )
		{
			KoLmafia.updateDisplay( "You can only get one food a day from Mom Sea Monkee." );
			return;
		}

		KoLmafia.updateDisplay( "You've had some of Mom's food." );
	}

	public static final void parseResponse( final String location, final String responseText )
	{
		// She looks up at you, and you begin to sweat.
		// You look down at it in horror and break out in a cold sweat as you back away.
		// You feel gross.
		// You feel... wrong.
		// You begin to sweat with anxiety.
		// As the blood spreads out around it, she leans toward you and kisses you on the cheek.
		// The lullaby echoes in your head. You've heard it before. Where? 
		if ( responseText.contains( "begin to sweat" ) ||
		     responseText.contains( "break out in a cold sweat" ) ||
		     responseText.contains( "feel gross" ) ||
		     responseText.contains( "feel... wrong" ) ||
		     responseText.contains( "begin to sweat with anxiety" ) ||
		     responseText.contains( "blood spreads out around" ) ||
		     responseText.contains( "heard it before" ) )
		{
			Preferences.setBoolean( "_momFoodReceived", true );
			QuestDatabase.setQuestProgress( Quest.SEA_MONKEES, QuestDatabase.FINISHED );
		}
	}

	public static final boolean registerRequest( final String location )
	{
		if ( !location.startsWith( "monkeycastle.php" ) )
		{
			return false;
		}

		Matcher matcher = MomRequest.ID_PATTERN.matcher( location );

		if ( !matcher.find() )
		{
			return true;
		}

		RequestLogger.updateSessionLog( "mom food " + matcher.group( 1 ) );
		return true;
	}
}
