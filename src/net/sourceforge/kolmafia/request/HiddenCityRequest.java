/**
 * Copyright (c) 2005-2008, KoLmafia development team
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
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.persistence.Preferences;
import net.sourceforge.kolmafia.session.ResultProcessor;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class HiddenCityRequest
	extends GenericRequest
{
	private static final Pattern WHICH_PATTERN = Pattern.compile( "which=([\\d,]+)" );
	private static final Pattern ROUND_PATTERN = Pattern.compile( "whichitem=([\\d,]+)" );

	private final String action;
	private int square = 0;
	private int itemId = 0;

	public HiddenCityRequest( String action)
	{
		super( "hiddencity.php");
		this.action = action;
	}

	public HiddenCityRequest()
	{
		this( null );
	}

	public HiddenCityRequest( int square )
	{
		this( null );
		this.square = square;
	}

	public HiddenCityRequest( boolean temple )
	{
		this( "trisocket" );
	}

	public HiddenCityRequest( boolean altar, int itemId )
	{
		this( "roundthing" );
		this.itemId = itemId;
	}

	public void reconstructFields()
	{
		this.constructURLString( "hiddencity.php" );

		if ( this.action == null )
		{
			int square = this.square;

			if ( !validSquare( square ) )
			{
				square = Preferences.getInteger( "hiddenCitySquare" );
			}

			if ( !validSquare( square ) )
			{
				KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "You must select a square of the Hidden City to visit." );
				return;
			}

			this.addFormField( "which", String.valueOf( square - 1 ) );
		}
		else
		{
			this.addFormField( "action", this.action );

			if ( this.itemId != 0 )
			{
				this.addFormField( "whichitem", String.valueOf( this.itemId ) );
			}
		}
	}

	public void run()
	{
		this.reconstructFields();

		if ( !KoLmafia.permitsContinue() )
		{
			return;
		}

		super.run();
	}

	private boolean validSquare( int square )
	{
		return square >= 1 && square <= 25;
	}

	public void processResults()
	{
		if ( !this.getURLString().startsWith( "hiddencity.php" ) )
		{
			return;
		}

		if (!HiddenCityRequest.parseResponse( this.getURLString(), this.responseText ) )
		{

			KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "Nothing more to do here." );
		}
	}

	public static final boolean parseResponse( final String location, final String responseText )
	{
		// You carefully socket the four triangular stones into their
		// places in the carving, and step back as the door slowly
		// slides to one side with a loud grinding noise.

		if ( responseText.indexOf( "socket the four triangular stones" ) != -1 )
		{
			ResultProcessor.processItem( ItemPool.TRIANGULAR_STONE, -4 );
			return true;
		}

		Matcher matcher = HiddenCityRequest.ROUND_PATTERN.matcher( location );
		if ( !matcher.find() )
		{
			// We simply visited a square
			return false;
		}

		int itemId = StringUtilities.parseInt( matcher.group( 1 ) );

		// You place the cracked stone sphere in the depression atop
		// the altar. You hear a click, and the sphere sinks into the
		// altar and disappears. There is a loud grinding noise, and a
		// niche opens on the front of the altar, containing an odd
		// triangular stone.

		// You place the object on the altar. There is a pregnant
		// pause, as though your offering is being carefully
		// considered. Eventually, it disappears into the altar, and
		// you feel a minor surge of power throughout your body. You
		// get the impression that it wasn't really what the god was
		// looking for, but that it was close enough to be worth
		// something.

		if ( responseText.indexOf( "sinks into the altar" ) != -1 ||
		     responseText.indexOf( "disappears into the altar" ) != -1 )
		{
			ResultProcessor.processItem( itemId, -1 );
			return true;
		}

		return false;
	}

	public static final int getSquare( final String urlString )
	{
		Matcher matcher = HiddenCityRequest.WHICH_PATTERN.matcher( urlString );
		if ( !matcher.find() )
		{
			return 0;
		}

		return 1 + StringUtilities.parseInt( matcher.group( 1 ) );
	}

	public static final boolean registerRequest( final String urlString )
	{
		if ( urlString.indexOf( "snarfblat=118" ) != 1 )
		{
			return true;
		}

		if ( !urlString.startsWith( "hiddencity.php" ) )
		{
			return false;
		}

		String message;
		if ( urlString.indexOf( "action=trisocket" ) != -1 )
		{
			message = "[" + KoLAdventure.getAdventureCount() + "] Hidden City (Temple)";
		}
		else if ( urlString.indexOf( "action=roundthing" ) != -1 )
		{
			Matcher matcher = HiddenCityRequest.ROUND_PATTERN.matcher( urlString );
			if ( !matcher.find() )
			{
				// We simply visited a square
				return true;
			}

			int itemId = StringUtilities.parseInt( matcher.group( 1 ) );
			String name = ItemDatabase.getItemName( itemId );

			message = "[" + KoLAdventure.getAdventureCount() + "] Hidden City (Altar)" + KoLConstants.LINE_BREAK + "Offering " + name + " at altar";
		}
		else
		{
			int square = HiddenCityRequest.getSquare( urlString );

			if ( square == 0 )
			{
				return true;
			}
			message = "Visiting square " + square + " in the Hidden City";
		}

		RequestLogger.printLine( "" );
		RequestLogger.updateSessionLog();
		RequestLogger.printLine( message );
		RequestLogger.updateSessionLog( message );

		return true;
	}
}
