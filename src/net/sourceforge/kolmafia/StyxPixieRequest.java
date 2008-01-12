/**
 * Copyright (c) 2005-2007, KoLmafia development team
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

package net.sourceforge.kolmafia;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StyxPixieRequest
	extends KoLRequest
{
	private int effectId = 0;
	private String desc = "";

	private static final Pattern ID_PATTERN = Pattern.compile( "whichbuff=(\\d+)" );

	public StyxPixieRequest( final int stat )
	{
		super( "heydeze.php" );

		this.addFormField( "pwd" );
		this.addFormField( "action", "styxbuff" );

		switch ( stat )
		{
		case MUSCLE:
			// Hella Tough
			this.effectId = 446;
			this.desc = "tougher";
			break;
		case MYSTICALITY:
			// Hella Smart
			this.effectId = 447;
			this.desc = "smarter";
			break;
		case MOXIE:
			// Hella Smooth
			this.effectId = 448;
			this.desc = "smoother";
			break;
		}

		if ( this.effectId != 0 )
		{
			this.addFormField( "whichbuff", String.valueOf( this.effectId ) );
		}
	}

	protected boolean retryOnTimeout()
	{
		return true;
	}

	public void run()
	{
		if ( !KoLCharacter.inBadMoon() )
		{
			KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "You can't find the Styx Pixie." );
			return;
		}

		if ( this.effectId == 0 )
		{
			KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "Choose a stat to buff." );
			return;
		}

		KoLmafia.updateDisplay( "Visiting the Styx Pixie..." );
		super.run();
	}

	public void processResults()
	{
		if ( this.responseText == null || this.responseText.equals( "" ) )
		{
			KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "You can't find the Styx Pixie." );
			return;
		}

		// "You already got a buff today"
		if ( this.responseText.indexOf( "already got a buff today" ) != -1 )
		{
			KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "You can only visit the Styx Pixie once a day." );
			return;
		}

		KoLmafia.updateDisplay( "You feel " + this.desc + "." );
		RequestFrame.refreshStatus();
	}

	public static final boolean registerRequest( final String location )
	{
		if ( !location.startsWith( "heydeze.php" ) )
		{
			return false;
		}

		Matcher idMatcher = StyxPixieRequest.ID_PATTERN.matcher( location );

		if ( !idMatcher.find() )
		{
			return true;
		}

		String stat = "";

		switch ( StaticEntity.parseInt( idMatcher.group( 1 ) ) )
		{
		case 446:
			stat = "muscle";
			break;
		case 447:
			stat = "mysticality";
			break;
		case 448:
			stat = "moxie";
			break;
		}

		RequestLogger.updateSessionLog( "styx " + stat );
		return true;
	}
}
