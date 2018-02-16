/**
 * Copyright (c) 2005-2018, KoLmafia development team
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

import net.sourceforge.kolmafia.FamiliarData;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.RequestThread;

import net.sourceforge.kolmafia.utilities.StringUtilities;

public class FamTeamRequest
	extends GenericRequest
{
	private static final Pattern ACTIVE_FAM_PATTERN = Pattern.compile( "<div class=\"slot full \" data-pos=\"(\\d+)\"><div class=\"fambox\" data-id=\"(\\d+)\">.*? width=150>(.*?) </td>.*?class=tiny>Lv. (\\d+) (.*?)</td>" );
	private static final Pattern BULLPEN_FAM_PATTERN = Pattern.compile( "<div class=\"fambox\" data-id=\"(\\d+)\" .*? width=150>(.*?) </td>.*?class=tiny>Lv. (\\d+) (.*?)</td>" );

	public FamTeamRequest()
	{
		super( "famteam.php" );
	}

	@Override
	public void run()
	{
		if ( GenericRequest.abortIfInFightOrChoice() )
		{
			return;
		}

		KoLmafia.updateDisplay( "Retrieving familiar data..." );
		super.run();
		return;
	}

	@Override
	public void processResults()
	{
		if ( !KoLCharacter.inPokefam() )
		{
			return;
		}

		if ( !FamTeamRequest.parseResponse( this.getURLString(), this.responseText ) )
		{
			// *** Have more specific error message?
			KoLmafia.updateDisplay( MafiaState.ERROR, "Familiar request unsuccessful." );
			return;
		}
	}

	public static final boolean parseResponse( final String urlString, final String responseText )
	{
		if ( !urlString.startsWith( "famteam.php" ) )
		{
			return false;
		}

		Matcher matcher = FamTeamRequest.ACTIVE_FAM_PATTERN.matcher( responseText );
		while ( matcher.find() )
		{
			int id = StringUtilities.parseInt( matcher.group( 2 ) );
			String name = matcher.group( 3 );
			int level = StringUtilities.parseInt( matcher.group( 4 ) );
			FamiliarData familiar = KoLCharacter.findFamiliar( id );
			if ( familiar == null )
			{
				// Add new familiar to list
				familiar = new FamiliarData( id, name, level );
				KoLCharacter.addFamiliar( familiar );
			}
			else
			{
				// Update existing familiar
				familiar.update( name, level );
			}
			KoLCharacter.addFamiliar( familiar );
		}

		matcher = FamTeamRequest.BULLPEN_FAM_PATTERN.matcher( responseText );
		while ( matcher.find() )
		{
			int id = StringUtilities.parseInt( matcher.group( 1 ) );
			String name = matcher.group( 2 );
			int level = StringUtilities.parseInt( matcher.group( 3 ) );
			FamiliarData.registerFamiliar( id, name, level );
		}

		return true;
	}

	public static final boolean registerRequest( final String urlString )
	{
		if ( !urlString.startsWith( "famteam.php" ) )
		{
			return false;
		}

		if ( urlString.equals( "famteam.php" ) )
		{
			// Visiting the terrarium
			return true;
		}

		// If it is something else, just log the URL
		return false;
	}
}
