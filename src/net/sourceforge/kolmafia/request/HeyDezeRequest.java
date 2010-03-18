/**
 * Copyright (c) 2005-2010, KoLmafia development team
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

import net.sourceforge.kolmafia.KoLAdventure;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.persistence.Preferences;
import net.sourceforge.kolmafia.swingui.RequestFrame;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class HeyDezeRequest
	extends GenericRequest
{
	private int effectId = 0;
	private String desc = "";

	private static final Pattern ID_PATTERN = Pattern.compile( "whichbuff=(\\d+)" );

	public HeyDezeRequest( final int stat )
	{
		super( "heydeze.php" );

		this.addFormField( "action", "styxbuff" );

		switch ( stat )
		{
		case KoLConstants.MUSCLE:
			// Hella Tough
			this.effectId = 446;
			this.desc = "tougher";
			break;
		case KoLConstants.MYSTICALITY:
			// Hella Smart
			this.effectId = 447;
			this.desc = "smarter";
			break;
		case KoLConstants.MOXIE:
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

	private static String styxStatString( final String urlString )
	{

		Matcher matcher = HeyDezeRequest.ID_PATTERN.matcher( urlString );

		if ( !matcher.find() )
		{
			return null;
		}

		switch ( StringUtilities.parseInt( matcher.group( 1 ) ) )
		{
		case 446:
			return "muscle";
		case 447:
			return "mysticality";
		case 448:
			return "moxie";
		}

		return null;
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

	public static String locationName( final String urlString )
	{
		if ( urlString.indexOf( "place=styx" ) != -1 )
		{
			return "The Styx Pixie";
		}
		if ( urlString.indexOf( "place=heartbreaker" ) != -1 )
		{
			return "Heartbreaker's Hotel";
		}
		if ( urlString.indexOf( "place=meansucker" ) != -1 )
		{
			return "Meansucker's House";
		}
		return null;
	}

	private static String visitLocation( final String urlString )
	{
		String name = HeyDezeRequest.locationName( urlString );
		if ( name != null )
		{
			return "Visiting " + name + " in Hey Deze";
		}
		return null;
	}

	public static final boolean registerRequest( final String urlString )
	{
		if ( !urlString.startsWith( "heydeze.php" ) )
		{
			return false;
		}

		String action = GenericRequest.getAction( urlString );
		String message = null;

		// We want to log certain simple visits
		if ( action == null )
		{
			message = HeyDezeRequest.visitLocation( urlString );
		}

		// Visit the Styx Pixie
		else if ( action.equals( "styxbuff" ) )
		{
			String stat = HeyDezeRequest.styxStatString( urlString );
			if ( stat == null )
			{
				return false;
			}

			message = "styx " + stat;
			Preferences.setBoolean( "styxPixieVisited", true );
		}

		// Take the Hellevator
		else if ( action.equals( "elevator" ) )
		{
			message = "[" + KoLAdventure.getAdventureCount() + "] Heartbreaker's Hotel";
		}

		// Unknown action
		else
		{
			return false;
		}

		if ( message == null )
		{
			return true;
		}

		RequestLogger.printLine();
		RequestLogger.updateSessionLog();
		RequestLogger.printLine( message );
		RequestLogger.updateSessionLog( message );

		return true;
	}
}
