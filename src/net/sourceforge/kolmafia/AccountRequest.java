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

public class AccountRequest extends PasswordHashRequest
{
	private static final Pattern AUTOSELL_PATTERN = Pattern.compile( "action=sellstuff\">Switch to (\\S*?) Autosale Mode</a>" );
	private static final Pattern TIMEZONE_PATTERN = Pattern.compile( "<select name=timezone>.*?</select>", Pattern.DOTALL );
	private static final Pattern SELECTED1_PATTERN = Pattern.compile( "selected>(-?\\d*?)</option>" );

	private static final Pattern AUTOATTACK_PATTERN = Pattern.compile( "<select class=small name=whichattack>.*?</select>" );
	private static final Pattern SELECTED2_PATTERN = Pattern.compile( "selected value=(\\d+)>" );

	public AccountRequest()
	{	super( "account.php" );
	}

	public void processResults()
	{
		super.processResults();
		parseAccountData( responseText );
	}

	public static void parseAccountData( String responseText )
	{
		// Parse response text -- make sure you
		// aren't accidentally parsing profiles.

		Matcher matcher = AUTOSELL_PATTERN.matcher( responseText );

		if ( matcher.find() )
		{
			String autosellMode = matcher.group(1).equals( "Compact" ) ? "detailed" : "compact";
			KoLCharacter.setAutosellMode( autosellMode );
		}

		// Consumption restrictions are also found
		// here through the presence of buttons.

		KoLCharacter.setConsumptionRestriction(
			responseText.indexOf( "<input class=button type=submit value=\"Drop Oxygenarian\">" ) != -1 ? AscensionSnapshotTable.OXYGENARIAN :
			responseText.indexOf( "<input class=button type=submit value=\"Drop Boozetafarian\">" ) != -1 ? AscensionSnapshotTable.BOOZETAFARIAN :
			responseText.indexOf( "<input class=button type=submit value=\"Drop Teetotaler\">" ) != -1 ? AscensionSnapshotTable.TEETOTALER :
			AscensionSnapshotTable.NOPATH );

		// Whether or not a player is currently in
		// hardcore is also found here through the
		// presence of buttons.

		KoLCharacter.setHardcore( responseText.indexOf( "<input class=button type=submit value=\"Drop Hardcore\">" ) != -1 );

		// Also parse out the player's current time
		// zone in the process.

		matcher = TIMEZONE_PATTERN.matcher( responseText );

		if ( matcher.find() )
		{
			matcher = SELECTED1_PATTERN.matcher( matcher.group() );
			if ( matcher.find() )
			{
				// You now have the current integer offset
				// for the player's time.  Now, the question
				// is, what should be done with it to actually
				// synchronize timestamps with the server so
				// that all kmail can be processed?

				int timeOffset = StaticEntity.parseInt( matcher.group(1) );
			}
		}

		Matcher selectMatcher = AUTOATTACK_PATTERN.matcher( responseText );
		if ( selectMatcher.find() )
		{
			Matcher optionMatcher = SELECTED2_PATTERN.matcher( selectMatcher.group() );
			if ( optionMatcher.find() )
				StaticEntity.setProperty( "defaultAutoAttack", optionMatcher.group(1) );
		}
	}
}
