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

import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;

import net.sourceforge.kolmafia.persistence.HolidayDatabase;

import net.sourceforge.kolmafia.utilities.StringUtilities;

public class MoonPhaseRequest
	extends GenericRequest
{
	private static final Pattern MOONS_PATTERN = Pattern.compile( "moon(.)[ab]?\\.gif.*moon(.)[ab]?\\.gif" );

	/**
	 * The phases of the moons can be retrieved from the top menu, which
	 * varies based on whether or not the player is using compact mode.
	 */

	public MoonPhaseRequest()
	{
		super( "topmenu.php" );
	}

	@Override
	protected boolean retryOnTimeout()
	{
		return true;
	}

	@Override
	protected boolean shouldFollowRedirect()
	{
		return true;
	}

	/**
	 * Runs the moon phase request, updating as appropriate.
	 */

	@Override
	public void run()
	{
		KoLmafia.updateDisplay( "Synchronizing moon data..." );
		super.run();
	}

	@Override
	public void processResults()
	{
		String text = this.responseText;

		// We can no longer count on knowing the menu style from api.php
		GenericRequest.topMenuStyle= 
			text.indexOf( "awesomemenu.php" ) != -1 ?
			GenericRequest.MENU_FANCY :
			text.indexOf( "Function:" ) != -1 ?
			GenericRequest.MENU_COMPACT :
			GenericRequest.MENU_NORMAL;

		// Get current phase of Ronald and Grimace
		if ( text.indexOf( "minimoon" ) != -1 )
		{
			text = text.replaceAll( "minimoon", "" );
		}

		Matcher moonMatcher = MoonPhaseRequest.MOONS_PATTERN.matcher( text );
		if ( moonMatcher.find() )
		{
			HolidayDatabase.setMoonPhases(
				StringUtilities.parseInt( moonMatcher.group( 1 ) ) - 1,
				StringUtilities.parseInt( moonMatcher.group( 2 ) ) - 1 );
		}

		// The following is not accurate for GenericRequest.MENU_FANCY,
		// since the config section includes an icon for the clan hall
		KoLCharacter.setClan( this.responseText.contains( "clan_hall.php" ) );
	}
}
