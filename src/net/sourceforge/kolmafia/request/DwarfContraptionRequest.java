/**
 * Copyright (c) 2005-2009, KoLmafia development team
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

import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.persistence.Preferences;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class DwarfContraptionRequest
	extends GenericRequest
{
	public static final Pattern ACTION_PATTERN = Pattern.compile( "action=([^&]*)" );

	public DwarfContraptionRequest()
	{
		super( "dwarfcontraption.php" );
	}

	public DwarfContraptionRequest( final String action)
	{
		this();
		this.addFormField( "action", action );
	}

	private static String getPlaceName( final String action )
	{
		if ( action.equals( "hopper0" ) )
		{
			return "Hopper #1";
		}

		if ( action.equals( "hopper1" ) )
		{
			return "Hopper #2";
		}

		if ( action.equals( "hopper2" ) )
		{
			return "Hopper #3";
		}

		if ( action.equals( "hopper3" ) )
		{
			return "Hopper #4";
		}

		if ( action.equals( "gauges" ) )
		{
			return "Gauges";
		}

		if ( action.equals( "panelleft" ) )
		{
			return "Left Panel";
		}

		if ( action.equals( "panelright" ) )
		{
			return "Right Panel";
		}

		if ( action.equals( "bin" ) )
		{
			return "Bin";
		}

		if ( action.equals( "chamber" ) )
		{
			return "Vacuum Chamber";
		}

		return null;
	}

	public void processResults()
	{
		DwarfContraptionRequest.parseResponse( this.getURLString(), this.responseText );
	}

	public static final void parseResponse( final String urlString, final String responseText )
	{
		if ( !urlString.startsWith( "dwarfcontraption.php" ) )
		{
			return;
		}

		Matcher matcher = ACTION_PATTERN.matcher( urlString );
		String action = matcher.find() ? matcher.group(1) : null;

		if ( action == null )
		{
			return;
		}

		if ( action.equals( "hopper0" ) )
		{
			String rune = DwarfFactoryRequest.getRune( responseText );
			KoLCharacter.ensureUpdatedDwarfFactory();
			Preferences.setString( "lastDwarfHopper1", rune );
			return;
		}

		if ( action.equals( "hopper1" ) )
		{
			String rune = DwarfFactoryRequest.getRune( responseText );
			KoLCharacter.ensureUpdatedDwarfFactory();
			Preferences.setString( "lastDwarfHopper2", rune );
			return;
		}

		if ( action.equals( "hopper2" ) )
		{
			String rune = DwarfFactoryRequest.getRune( responseText );
			KoLCharacter.ensureUpdatedDwarfFactory();
			Preferences.setString( "lastDwarfHopper3", rune );
			return;
		}

		if ( action.equals( "hopper3" ) )
		{
			String rune = DwarfFactoryRequest.getRune( responseText );
			KoLCharacter.ensureUpdatedDwarfFactory();
			Preferences.setString( "lastDwarfHopper4", rune );
			return;
		}

		if ( action.equals( "gauges" ) )
		{
			String rune = DwarfFactoryRequest.getRune( responseText );
			KoLCharacter.ensureUpdatedDwarfFactory();
			Preferences.setString( "lastDwarfGauges", rune );
			return;
		}

		if ( action.equals( "dochamber" ) )
		{
			// parse url to find out itemId and quantity.
			// parse results to decide if it was consumed.
			return;
		}
	}

	public static final boolean registerRequest( final String urlString )
	{
		if ( !urlString.startsWith( "dwarfcontraption.php" ) )
		{
			return false;
		}

		Matcher matcher = ACTION_PATTERN.matcher( urlString );
		String action = matcher.find() ? matcher.group(1) : null;

		if ( action == null )
		{
			return true;
		}

		String place = getPlaceName( action );

		if ( place != null )
		{
			RequestLogger.updateSessionLog();
			RequestLogger.updateSessionLog( "Visiting " + place + " in the Dwarven Factory Machine Room" );
			return true;
		}

		// Other actions in the Machine Room

		// action=dohopper0
		// action=dohopper1
		// action=dohopper2
		// action=dohopper3
		// action=dogauges
		// action=doredbutton
		// action=dochamber

		return false;
	}
}
