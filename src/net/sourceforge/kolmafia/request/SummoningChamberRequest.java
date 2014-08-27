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

import net.sourceforge.kolmafia.KoLAdventure;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.RequestThread;

import net.sourceforge.kolmafia.objectpool.ItemPool;

import net.sourceforge.kolmafia.persistence.ItemDatabase;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.session.ClanManager;
import net.sourceforge.kolmafia.session.InventoryManager;
import net.sourceforge.kolmafia.session.ResultProcessor;

public class SummoningChamberRequest
	extends GenericRequest
{
	// place.php?whichplace=manor4&action=manor4_chamber
	// choice.php?pwd&whichchoice=922&option=1&demonname=Ak'gyxoth

	private static final Pattern DEMON_PATTERN = Pattern.compile( "demonname=([^&]*)" );
	private final String demon;

	public SummoningChamberRequest( final String demon )
	{
		super( "choice.php" );
		this.addFormField( "whichchoice", "922" );
		this.addFormField( "option", "1" );
		this.addFormField( "demonname", demon );
		this.demon = demon;
	}

	@Override
	public void run()
	{
		KoLmafia.updateDisplay( "Summoning " + this.demon + "..." );

		// Go to the Summoning Chamber
		RequestThread.postRequest( new GenericRequest( "place.php?whichplace=manor4&action=manor4_chamber" ) );

		// Submit the choice adventure
		super.run();
	}

	@Override
	public void processResults()
	{
		SummoningChamberRequest.parseResponse( this.getURLString(), this.responseText );
	}

	private static final Pattern BROWN_WORD_PATTERN =
		Pattern.compile( "tell him that the passhword is <font color=brown><b>(.*?)</b></font>" );

	public static final void parseResponse( final String location, final String responseText )
	{
		if ( !location.startsWith( "choice.php" ) || !location.contains( "whichchoice=922" ) || !location.contains( "option=1" ) )
		{
			return;
		}

		Matcher matcher = DEMON_PATTERN.matcher( location );
		if ( !matcher.find() )
		{
			return;
		}

		// You step up to the altar and begin to speak, but then you
		// notice that the air doesn't have that greasy
		// static-electricity feel that you associate with an active
		// magical field. It must take some time for it to recharge
		// after a summoning attempt.
		if ( responseText.contains( "greasy static-electricity feel" ) )
		{
			Preferences.setBoolean( "demonSummoned", true );
		}
		else if ( responseText.contains( "You light three black candles" ) )
		{
			AdventureRequest.registerDemonName( "Summoning Chamber", responseText );
			ResultProcessor.processItem( ItemPool.BLACK_CANDLE, -3 );
			ResultProcessor.processItem( ItemPool.EVIL_SCROLL, -1 );

			// If you see -hic- Gary, tell him that the passhword is <font color=brown><b>oPeNs3saMe</b></font>.
			Matcher brownWordMatcher = SummoningChamberRequest.BROWN_WORD_PATTERN.matcher( responseText );
			if ( brownWordMatcher.find() )
			{
				String brownWord = brownWordMatcher.group( 1 );
				String message = "Infernal Thirst demon Brown Word found: " + brownWord + " in clan " + ClanManager.getClanName( false ) + ".";
				RequestLogger.printLine( "<font color=\"blue\">" + message + "</font>" );
				RequestLogger.updateSessionLog( message );
			}

			if ( !responseText.contains( "some sort of crossed signal" ) &&
			     !responseText.contains( "hum, which eventually cuts off" ) &&
			     !responseText.contains( "get right back to you" ) &&
			     !responseText.contains( "Please check the listing" ) )
			{
				Preferences.setBoolean( "demonSummoned", true );
			}
		}
	}

	public static final boolean registerRequest( final String urlString )
	{
		// place.php?whichplace=manor4&action=manor4_chamber
		if ( urlString.startsWith( "place.php" ) && urlString.contains( "whichplace=manor4" ) && urlString.contains( "action=manor4_chamber" ) )
		{
			String message = "[" + KoLAdventure.getAdventureCount() + "] Summoning Chamber";

			RequestLogger.printLine( "" );
			RequestLogger.printLine( message );

			RequestLogger.updateSessionLog();
			RequestLogger.updateSessionLog( message );

			return true;
		}

		if ( !urlString.startsWith( "choice.php" ) || !urlString.contains( "whichchoice=922" ) || !urlString.contains( "option=1" ) )
		{
			return false;
		}

		Matcher matcher = DEMON_PATTERN.matcher( urlString );
		if ( !matcher.find() )
		{
			return true;
		}

		String demon = GenericRequest.decodeField( matcher.group(1) );

		if ( demon.equals( "" ) ||
		     !InventoryManager.retrieveItem( ItemPool.BLACK_CANDLE, 3 ) ||
		     !InventoryManager.retrieveItem( ItemPool.EVIL_SCROLL ) )
		{
			return true;
		}

		RequestLogger.updateSessionLog( "summon " + demon );

		return true;
	}
}
