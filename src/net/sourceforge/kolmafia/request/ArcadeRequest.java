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

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLAdventure;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.session.ResultProcessor;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class ArcadeRequest
	extends GenericRequest
{
	public static final AdventureResult TOKEN = ItemPool.get( ItemPool.GG_TOKEN, 1 );

	public ArcadeRequest()
	{
		super( "arcade.php" );
	}

	public void reconstructFields()
	{
	}
	public static final int getTurnsUsed( GenericRequest request )
	{
		String action = request.getFormField( "action" );
		if ( action == null || !action.equals( "game" ) )
		{
			return 0;
		}

		String game = request.getFormField( "whichgame" );
		if ( game != null && 
		     ( game.equals( "1" ) || game.equals( "2" ) || game.equals( "4" ) ) )
		{
			return 1;
		}

		return 0;
	}

	private static final Pattern GAME_PATTERN = Pattern.compile( "whichgame=(\\d+)" );

	private static final int getGame( final String urlString )
	{
		Matcher matcher = ArcadeRequest.GAME_PATTERN.matcher( urlString );
		return matcher.find() ? StringUtilities.parseInt( matcher.group(1) ) : 0;
	}

	private static final Pattern ITEM_PATTERN = Pattern.compile( "name=whichitem value=([\\d]+)>.*?descitem.([\\d]+).*?<b>([^<&]*)(?:&nbsp;)*</td>.*?<b>([\\d,]+)</b>", Pattern.DOTALL );

	public static void parseResponse( final String urlString, final String responseText )
	{
		if ( !urlString.startsWith( "arcade.php" ) )
		{
			return;
		}

		if ( urlString.indexOf( "ticketcounter=1" ) != -1 )
		{
			// Learn new trade items by simply visiting Arcade
			Matcher matcher = ITEM_PATTERN.matcher( responseText );
			while ( matcher.find() )
			{
				int id = StringUtilities.parseInt( matcher.group(1) );
				String desc = matcher.group(2);
				String name = matcher.group(3);
				String data = ItemDatabase.getItemDataName( id );
				// String price = matcher.group(4);
				if ( data == null || !data.equals( name ) )
				{
					ItemDatabase.registerItem( id, name, desc );
				}
			}

			KoLmafia.saveDataOverride();
			return;
		}

		String action = GenericRequest.getAction( urlString );
		if ( action == null )
		{
			return;
		}

		if ( action.equals( "skeeball" ) )
		{
			// You don't have any Game Grid tokens, so you can't
			// play Skee-Ball. But don't feel bad. The Skee-Ball
			// machine is broken, so you wouldn't have been able to
			// play Skee-Ball anyway.
			if ( responseText.indexOf( "you can't play Skee-Ball" ) == -1 )
			{
				ResultProcessor.processItem( ItemPool.GG_TOKEN, -1 );
			}
			return;
		}

		if ( !action.equals( "game" ) )
		{
			return;
		}

		int game = ArcadeRequest.getGame( urlString );
		switch ( game )
		{
		case 1: // Space Trip
		case 2:	// DemonStar
		case 4:	// The Fighters of Fighting
			// These games only take tokens, and you don't have any

                        // If we succeed in playing a game, we were redirected
                        // to a choice adventure and never get here. Therefore,
                        // deduct tokens in registerRequest

			if ( responseText.indexOf( "These games only take tokens" ) == -1 )
			{
				// The game also took 5 turns
			}
			break;
		}
	}

	public static final boolean registerRequest( final String urlString )
	{
		if ( !urlString.startsWith( "arcade.php" ) )
		{
			return false;
		}

		String action = GenericRequest.getAction( urlString );
		String message = null;

		if ( action != null )
		{
			if ( action.equals( "redeem" ) )
			{
				// Buy stuff at the ticket counter
				// Let CoinmasterRequest claim this
				return false;
			}

			// Other actions of interest require tokens. Do we have
			// any?

			int count = TOKEN.getCount( KoLConstants.inventory );
			if ( count < 1 )
			{
				return true;
			}

			if ( action.equals( "skeeball" ) )
			{
				message = "Visiting Broken Skee-Ball Machine";
			}
			else if ( action.equals( "game" ) )
			{
				int game = ArcadeRequest.getGame( urlString );
				String name = null;
				switch ( game )
				{
				case 1: // Space Trip
					name = "Space Trip";
					break;
				case 2:	// DemonStar
					name = "DemonStar";
					break;
				case 4:	// The Fighters of Fighting
					name = "The Fighters of Fighting";
					break;
				default:
					return false;
				}

				if ( KoLCharacter.getAdventuresLeft() < 5 )
				{
					return true;
				}

				// We have a token and 5 adventures.
				message = "[" + KoLAdventure.getAdventureCount() + "] " + name;

				// Deduct the token here
				ResultProcessor.processItem( ItemPool.GG_TOKEN, -1 );
			}
		}
		else if ( urlString.indexOf( "ticketcounter=1" ) != -1 )
		{
			message = "Visiting Ticket Redemption Counter";
		}

		if ( message == null )
		{
			return false;
		}

		RequestLogger.updateSessionLog();
		RequestLogger.updateSessionLog( message );

		return true;
	}
}
