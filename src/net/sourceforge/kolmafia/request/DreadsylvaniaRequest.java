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

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLAdventure;
import net.sourceforge.kolmafia.RequestLogger;

import net.sourceforge.kolmafia.session.ResultProcessor;

import net.sourceforge.kolmafia.utilities.StringUtilities;

public class DreadsylvaniaRequest
	extends GenericRequest
{
	private static final Pattern LOC_PATTERN = Pattern.compile( "loc=([\\d]+)" );
	private static final Pattern WHICHBOOZE_PATTERN = Pattern.compile( "whichbooze=([\\d]+)" );
	private static final Pattern BOOZEQUANTITY_PATTERN = Pattern.compile( "boozequantity=([\\d]+)" );

	public DreadsylvaniaRequest()
	{
		super( "clan_dreadsylvania.php");
	}

	private static String getAdventureZone( final String urlString )
	{
		Matcher matcher = DreadsylvaniaRequest.LOC_PATTERN.matcher( urlString );
		if ( !matcher.find() )
		{
			return null;
		}
		int loc = StringUtilities.parseInt( matcher.group( 1 ) );
		return
			loc < 1 || loc > 9 ? null :
			loc <= 3 ? "Dreadsylvanian Woods" :
			loc <= 6 ? "Dreadsylvanian Village" :
			"Dreadsylvanian Castle";
	}

	private static AdventureResult getBooze( final String urlString )
	{
		Matcher matcher = DreadsylvaniaRequest.WHICHBOOZE_PATTERN.matcher( urlString );
		if ( !matcher.find() )
		{
			return null;
		}
		int itemId = StringUtilities.parseInt( matcher.group( 1 ) );
		matcher = DreadsylvaniaRequest.BOOZEQUANTITY_PATTERN.matcher( urlString );
		int count = matcher.find() ? StringUtilities.parseInt( matcher.group( 1 ) ) : 1;
		return new AdventureResult( itemId, count );
	}

	@Override
	public int getAdventuresUsed()
	{
		return 1;
	}

	@Override
	public void processResults()
	{
		DreadsylvaniaRequest.parseResponse( this.getURLString(), this.responseText );
	}

	public static final void parseResponse( final String urlString, final String responseText )
	{
		String action = GenericRequest.getAction( urlString );
		if ( action == null )
		{
			return;
		}

		if ( action.equals( "feedbooze" ) )
		{
			AdventureResult booze = DreadsylvaniaRequest.getBooze( urlString );
			if ( booze == null )
			{
				return;
			}
			// Should check for success
			ResultProcessor.processItem( booze.getItemId(), -1 * booze.getCount() );
		}
	}

	public static final boolean registerRequest( final String urlString )
	{
		if ( !urlString.startsWith( "clan_dreadsylvania.php" ) )
		{
			return false;
		}

		String action = GenericRequest.getAction( urlString );
		if ( action == null )
		{
			return false;
		}

		String message;

		// clan_dreadsylvania.php?action=forceloc&loc=1
		// Dreadsylvanian Woods -> Cabin in the Woods
		if ( action.equals( "forceloc" ) )
		{
			String zone = DreadsylvaniaRequest.getAdventureZone( urlString );
			if ( zone == null )
			{
				return false;
			}
			message = "[" + KoLAdventure.getAdventureCount() + "] " + zone;
		}

		// Giving booze to the coachman. 
		// clan_dreadsylvania.php?action=feedbooze&whichbooze=xxx&boozequantity=yyy
		else if ( action.equals( "feedbooze" ) )
		{
			AdventureResult booze = DreadsylvaniaRequest.getBooze( urlString );
			if ( booze == null )
			{
				return false;
			}
			int count = booze.getCount();
			message = "Feeding " + count + " " + booze.getPluralName( count ) + " to the coachman";
		}

		else
		{
			return false;
		}

		RequestLogger.printLine( "" );
		RequestLogger.printLine( message );

		RequestLogger.updateSessionLog();
		RequestLogger.updateSessionLog( message );

		return true;
	}
}
