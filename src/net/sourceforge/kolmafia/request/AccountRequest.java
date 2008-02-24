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

package net.sourceforge.kolmafia.request;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.StaticEntity;

import net.sourceforge.kolmafia.persistence.AscensionSnapshot;
import net.sourceforge.kolmafia.persistence.Preferences;

public class AccountRequest
	extends PasswordHashRequest
{
	private static final Pattern AUTOSELL_PATTERN =
		Pattern.compile( "Turn On (\\S*?) Autosale Mode" );
	private static final Pattern AUTOATTACK_PATTERN =
		Pattern.compile( "<select class=small name=whichattack>.*?</select>", Pattern.DOTALL );

	private static final Pattern SELECTED1_PATTERN = Pattern.compile( "value=(\\d+) selected>" );
	private static final Pattern SELECTED2_PATTERN = Pattern.compile( "selected value=(\\d+)>" );

	public AccountRequest()
	{
		super( "account.php" );
	}

	protected boolean retryOnTimeout()
	{
		return true;
	}

	public void processResults()
	{
		super.processResults();
		AccountRequest.parseAccountData( this.responseText );
	}

	public static final void parseAccountData( final String responseText )
	{
		// Parse response text -- make sure you
		// aren't accidentally parsing profiles.

		Matcher matcher = AccountRequest.AUTOSELL_PATTERN.matcher( responseText );

		if ( matcher.find() )
		{
			String autosellMode = matcher.group( 1 ).equals( "Compact" ) ? "detailed" : "compact";
			KoLCharacter.setAutosellMode( autosellMode );
		}

		// Consumption restrictions are also found
		// here through the presence of buttons.

		if ( responseText.indexOf( "<input class=button type=submit value=\"Drop Oxygenarian\">" ) != -1 )
		{
			KoLCharacter.setConsumptionRestriction( AscensionSnapshot.OXYGENARIAN );
		}
		else if ( responseText.indexOf( "<input class=button type=submit value=\"Drop Boozetafarian\">" ) != -1 )
		{
			KoLCharacter.setConsumptionRestriction( AscensionSnapshot.BOOZETAFARIAN );
		}
		else if ( responseText.indexOf( "<input class=button type=submit value=\"Drop Teetotaler\">" ) != -1 )
		{
			KoLCharacter.setConsumptionRestriction( AscensionSnapshot.TEETOTALER );
		}
		else
		{
			KoLCharacter.setConsumptionRestriction( AscensionSnapshot.NOPATH );
		}

		// Whether or not a player is currently in Bad Moon or hardcore
		// is also found here through the presence of buttons.

		if ( responseText.indexOf( "<input class=button type=submit value=\"Drop Bad Moon\">" ) != -1 )
		{
			KoLCharacter.setSign( "Bad Moon" );
			KoLCharacter.setHardcore( true );
		}
		else
		{
			if ( KoLCharacter.getSignStat() == KoLConstants.BAD_MOON )
			{
				KoLCharacter.setSign( "None" );
			}
			KoLCharacter.setHardcore( responseText.indexOf( "<input class=button type=submit value=\"Drop Hardcore\">" ) != -1 );
		}

		int skillId = 0;
		Matcher selectMatcher = AccountRequest.AUTOATTACK_PATTERN.matcher( responseText );
		if ( selectMatcher.find() )
		{
			Matcher optionMatcher = AccountRequest.SELECTED1_PATTERN.matcher( selectMatcher.group() );
			if ( optionMatcher.find() )
			{
				skillId = StaticEntity.parseInt( optionMatcher.group( 1 ) );
			}
			else
			{
				optionMatcher = AccountRequest.SELECTED2_PATTERN.matcher( selectMatcher.group() );

				if ( optionMatcher.find() )
				{
					skillId = StaticEntity.parseInt( optionMatcher.group( 1 ) );
				}
			}
		}

		Preferences.setInteger( "defaultAutoAttack", skillId );
	}
}
