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
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLAdventure;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.RequestThread;

import net.sourceforge.kolmafia.objectpool.ItemPool;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.session.Limitmode;

public class CampAwayRequest
	extends PlaceRequest
{
	public CampAwayRequest()
	{
		super( "campaway" );
	}

	public CampAwayRequest( final String action )
	{
		super( "campaway", action );
	}

	@Override
	public void processResults()
	{
		CampAwayRequest.parseResponse( this.getURLString(), this.responseText );
	}

	private static final Pattern EFFECT_PATTERN = Pattern.compile( "You acquire an effect: <b>(.*?)</b>" );
	public static final void parseResponse( final String urlString, final String responseText )
	{
		String action = GenericRequest.getAction( urlString );

		// Nothing more to do for a simple visit
		if ( action == null )
		{
			return;
		}

		// There are two divs shown for the tent, each with a link
		//
		// When it is a free rest:
		//
		// place.php?whichplace=campaway&action=campaway_tent
		// place.php?whichplace=campaway&action=campaway_tentclick
		//
		// When it takes a turn:
		// 
		// place.php?whichplace=campaway&action=campaway_tentturn
		// place.php?whichplace=campaway&action=campaway_tentclick

		if ( action.startsWith( "campaway_tent" ) )
		{
			Preferences.increment( "timesRested" );
		}
		else if ( action.equals( "campaway_sky" ) )
		{
			Matcher m = EFFECT_PATTERN.matcher( responseText );
			if ( m.find() )
			{
				String effect = m.group( 1 );
				if ( effect.contains( "Smile" ) )
				{
					Preferences.increment( "_campawaySmileBuffs" );
				}
				else if ( effect.contains( "Cloud-Talk" ) )
				{
					Preferences.increment( "_campawayCloudBuffs" );
				}
			}
		}
	}

	public static final boolean registerRequest( final String urlString )
	{
		if ( !urlString.startsWith( "place.php" ) || !urlString.contains( "whichplace=campaway" ) )
		{
			return false;
		}

		String action = GenericRequest.getAction( urlString );
		if ( action == null )
		{
			// Nothing to log for simple visits
			return true;
		}

		String message = null;

		if ( action.equals( "campaway_sky" ) )
		{
			message = "Gazing at the Stars";
		}
		else if ( action.startsWith( "campaway_tent" ) )
		{
			message = "[" + KoLAdventure.getAdventureCount() + "] Rest in your campaway tent";
		}

		if ( message == null )
		{
			// Log URL for anything else
			return false;
		}

		RequestLogger.printLine();
		RequestLogger.printLine( message );

		RequestLogger.updateSessionLog();
		RequestLogger.updateSessionLog( message );

		return true;
	}

	public static boolean campAwayTentRestUsable()
	{	return Preferences.getBoolean( "restUsingCampAwayTent" ) &&
		       Preferences.getBoolean( "getawayCampsiteUnlocked" ) &&
		       StandardRequest.isAllowed( "Items", "Distant Woods Getaway Brochure" ) &&
		       !Limitmode.limitZone( "Woods" ) &&
		       !KoLCharacter.inBadMoon();
	}
}
