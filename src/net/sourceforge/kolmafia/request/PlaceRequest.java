/**
 * Copyright (c) 2005-2015, KoLmafia development team
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

import net.sourceforge.kolmafia.RequestLogger;

import net.sourceforge.kolmafia.objectpool.ItemPool;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.session.ResultProcessor;
import net.sourceforge.kolmafia.session.SorceressLairManager;

public class PlaceRequest
	extends GenericRequest
{
	public PlaceRequest()
	{
		super( "place.php" );
	}

	public PlaceRequest( final String place )
	{
		this();
		this.addFormField( "whichplace", place );
	}

	public PlaceRequest( final String place, final String action )
	{
		this( place );
		this.addFormField( "action", action );
	}

	@Override
	public void processResults()
	{
		PlaceRequest.parseResponse( this.getURLString(), this.responseText );
	}

	public static final void parseResponse( final String urlString, final String responseText )
	{
		String place = GenericRequest.getPlace( urlString );
		if ( place == null )
		{
			return;
		}

		String action = GenericRequest.getAction( urlString );
		if ( action == null )
		{
			action = "";
		}

		if ( place.equals( "arcade" ) )
		{
			ArcadeRequest.parseResponse( urlString, responseText );
		}
		else if ( place.equals( "chateau" ) )
		{
			ChateauRequest.parseResponse( urlString, responseText );
		}
		else if ( place.equals( "forestvillage" ) )
		{
			if ( action.equals( "fv_untinker" ) )
			{
				UntinkerRequest.parseResponse( urlString, responseText );
			}
		}
		else if ( place.equals( "junggate" ) )
		{
			UseItemRequest.parseConsumption( responseText, false );
		}
		else if ( place.equals( "knoll_friendly" ) )
		{
			KnollRequest.parseResponse( urlString, responseText );
		}
		else if ( place.equals( "mountains" ) )
		{
			if ( responseText.contains( "chateau" ) )
			{
				Preferences.setBoolean( "chateauAvailable", true );
			}
		}
		else if ( place.equals( "nstower_door" ) )
		{
			SorceressLairManager.parseDoorResponse( urlString, responseText );
		}
		else if ( place.equals( "orc_chasm" ) )
		{
			OrcChasmRequest.parseResponse( urlString, responseText );
		}
		else if ( place.equals( "rabbithole" ) )
		{
			RabbitHoleRequest.parseResponse( urlString, responseText );
		}
		else if ( place.equals( "town_wrong" ) )
		{
			if ( action.equals( "townwrong_artist_quest" ) )
			{
				ArtistRequest.parseResponse( urlString, responseText );
			}
		}
		else if ( place.equals( "twitch" ) )
		{
			if ( action.equals( "twitch_bank" ) && responseText.contains( "Thanks fer bringin' the money back" ) )
			{
				ResultProcessor.removeItem( ItemPool.BIG_BAG_OF_MONEY );
			}
		}
	}

	public static final boolean registerRequest( final String urlString )
	{
		if ( !urlString.startsWith( "place.php" ) )
		{
			return false;
		}

		String place = GenericRequest.getPlace( urlString );
		if ( place == null )
		{
			return true;
		}

		String action = GenericRequest.getAction( urlString );
		if ( action == null )
		{
			action = "";
		}

		String message = null;

		if ( place.equals( "desertbeach" ) )
		{
			if ( action.equals( "db_gnasir" ) )
			{
				message = "Talking to Gnasir";
			}
			else if ( action.equals( "db_nukehouse" ) )
			{
				message = "Visiting the Ruined House";
			}
			else if ( action.equals( "db_pyramid1" ) )
			{
				// message = "Visiting the Small Pyramid";
			}
		}
		else if ( place.equals( "forestvillage" ) )
		{
			if ( action.equals( "fv_mystic" ) )
			{
				message = "Talking to the Crackpot Mystic";
			}
		}
		else if ( place.equals( "highlands" ) )
		{
			if ( action.equals( "highlands_dude" ) )
			{
				message = "Talking to the Highland Lord";
			}
		}
		else if ( place.equals( "manor1" ) )
		{
			if ( action.equals( "manor1lock_kitchen" ) ||
			     action.equals( "manor1lock_library" ) ||
			     action.equals( "manor1lock_stairsup" ) )
			{
				return true;
			}
			if ( action.equals( "manor1_ladys" ) )
			{
				message = "Talking to Lady Spookyraven";
			}
		}
		else if ( place.equals( "manor2" ) )
		{
			if ( action.equals( "manor2lock_ballroom" ) ||
			     action.equals( "manor2lock_stairsup" ) )
			{
				return true;
			}
			if ( action.equals( "manor2_ladys" ) )
			{
				message = "Talking to Lady Spookyraven";
			}
		}
		else if ( place.equals( "manor3" ) )
		{
			if ( action.equals( "manor3_ladys" ) )
			{
				message = "Talking to Lady Spookyraven";
			}
		}
		else if ( place.equals( "orc_chasm" ) )
		{
			if ( action.startsWith( "bridge" ) || action.equals( "label2" ) )
			{
				// Building the bridge. Do we need to log anything?
				return true;
			}
		}
		else if ( place.equals( "palindome" ) )
		{
			if ( action.equals( "pal_drlabel" ) || action.equals( "pal_droffice" ) )
			{
				message = "Visiting Dr. Awkward's office";
			}
			else if ( action.equals( "pal_mrlabel" ) || action.equals( "pal_mroffice" ) )
			{
				message = "Visiting Mr. Alarm's's office";
			}
		}
		else if ( place.equals( "town_wrong" ) )
		{
			if ( action.equals( "townwrong_artist_noquest" ) || action.equals( "townwrong_artist_quest" ) )
			{
				message = "Visiting the Pretentious Artist";
			}
		}

		if ( message == null )
		{
			// Let another class take responsibility if it wishes
			return false;
		}

		RequestLogger.printLine();
		RequestLogger.printLine( message );

		RequestLogger.updateSessionLog();
		RequestLogger.updateSessionLog( message );

		return true;
	}
}
