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

import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.RequestLogger;

import net.sourceforge.kolmafia.session.MushroomManager;
import net.sourceforge.kolmafia.session.ResultProcessor;

import net.sourceforge.kolmafia.utilities.StringUtilities;

public class MushroomRequest
	extends GenericRequest
{
	private static final Pattern SQUARE_PATTERN = Pattern.compile( "pos=([\\d,]+)" );
	private static final Pattern SPORE_PATTERN = Pattern.compile( "whichspore=([\\d,]+)" );

	public MushroomRequest()
	{
		super( "knoll_mushrooms.php" );
	}

	public MushroomRequest( final int square )
	{
		this();
		this.addFormField( "action", "click" );
		this.addFormField( "pos", String.valueOf( square - 1 ) );
	}

	public MushroomRequest( final int square, final int spore )
	{
		this();
		this.addFormField( "action", "plant" );
		this.addFormField( "pos", String.valueOf( square - 1 ) );
		this.addFormField( "whichspore", String.valueOf( spore ) );
	}

	@Override
	public void processResults()
	{
		MushroomRequest.parseResponse( this.getURLString(), this.responseText );
	}

	public static final void parseResponse( final String urlString, final String responseText )
	{
		if ( !urlString.startsWith( "knoll_mushrooms.php" ) )
		{
			return;
		}

		if ( urlString.indexOf( "action=plant" ) != -1 )
		{
			// We are planting. If we succeeded, pay for it
			int sporeIndex = MushroomRequest.getSpore( urlString );
			int [] data = MushroomManager.getSporeDataByIndex( sporeIndex );
			if (  data != null && responseText.indexOf( "You plant the spore" ) != -1 )
			{
				int price = MushroomManager.getSporePrice( data );
				ResultProcessor.processMeat( -price );
			}
		}
		else if ( urlString.indexOf( "action=buyplot" ) != -1 )
		{
			// Thanks!  It's all yours.  You can buy spores from us
			// to get your mushroom garden started.  Click a spot
			// in your plot to buy a spore there.

			if ( responseText.indexOf( "It's all yours." ) != -1 )
			{
				ResultProcessor.processMeat( -5000 );
			}
		}

		MushroomManager.parsePlot( responseText );
	}

	private static boolean validSquare( int square )
	{
		return square >= 1 && square <= 16;
	}

	private static final int getSquare( final String urlString )
	{
		Matcher matcher = MushroomRequest.SQUARE_PATTERN.matcher( urlString );
		if ( !matcher.find() )
		{
			return 0;
		}

		return 1 + StringUtilities.parseInt( matcher.group( 1 ) );
	}

	private static final int getSpore( final String urlString )
	{
		Matcher matcher = MushroomRequest.SPORE_PATTERN.matcher( urlString );
		if ( !matcher.find() )
		{
			return 0;
		}

		return StringUtilities.parseInt( matcher.group( 1 ) );
	}

	public static final boolean registerRequest( final String urlString )
	{
		if ( !urlString.startsWith( "knoll_mushrooms.php" ) )
		{
			return false;
		}

		String message;
		if ( urlString.indexOf( "action=click" ) != -1 )
		{
			int square = MushroomRequest.getSquare( urlString );
			if ( square == 0 )
			{
				return true;
			}
			message = "pick " + square;
		}
		else if ( urlString.indexOf( "action=plant" ) != -1 )
		{
			int square = MushroomRequest.getSquare( urlString );
			int sporeIndex = MushroomRequest.getSpore( urlString );
			int [] data = MushroomManager.getSporeDataByIndex( sporeIndex );
			if ( square == 0 || data == null )
			{
				return true;
			}

			int price = MushroomManager.getSporePrice( data );
			if ( KoLCharacter.getAvailableMeat() < price )
			{
				return true;
			}

			String name = MushroomManager.getSporeName( data );
			message = "plant " + square + " " + name;
		}
		else if ( urlString.indexOf( "action=buyplot" ) != -1 )
		{
			if ( KoLCharacter.getAvailableMeat() < 5000 )
			{
				return true;
			}
			message = "Buying a mushroom plot";
		}
		else
		{
			return true;
		}

		RequestLogger.printLine( "" );
		RequestLogger.printLine( message );

		RequestLogger.updateSessionLog();
		RequestLogger.updateSessionLog( message );

		return true;
	}
}
