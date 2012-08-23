/**
 * Copyright (c) 2005-2012, KoLmafia development team
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
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestLogger;

import net.sourceforge.kolmafia.objectpool.ItemPool;

import net.sourceforge.kolmafia.session.InventoryManager;

public class MindControlRequest
	extends GenericRequest
{
	private final int level;
	private final int maxLevel;

	private static final AdventureResult RADIO = ItemPool.get( ItemPool.DETUNED_RADIO, 1 );

	private static final Pattern MCD1_PATTERN = Pattern.compile( "whichlevel=(\\d+)" );
	private static final Pattern MCD2_PATTERN = Pattern.compile( "tuneradio=(\\d+)" );

	public MindControlRequest( final int level )
	{
		super(
			KoLCharacter.canadiaAvailable() ? "canadia.php" : KoLCharacter.gnomadsAvailable() ? "gnomes.php" : "inv_use.php" );

		if ( KoLCharacter.knollAvailable() )
		{
			this.addFormField( "whichitem", String.valueOf( MindControlRequest.RADIO.getItemId() ) );
			this.addFormField( "tuneradio", String.valueOf( level ) );
		}
		else
		{
			this.addFormField( "action", "changedial" );
			this.addFormField( "whichlevel", String.valueOf( level ) );
		}

		this.level = level;
		this.maxLevel = KoLCharacter.canadiaAvailable() ? 11 : 10;
	}

	@Override
	protected boolean retryOnTimeout()
	{
		return true;
	}

	@Override
	public boolean shouldFollowRedirect()
	{
		// Musc sign MCD redirects to a message page, processResults()
		// doesn't get called if the redirect is ignored.
		return true;
	}

	@Override
	public void run()
	{
		// Avoid server hits if user gives an invalid level

		if ( this.level < 0 || this.level > this.maxLevel )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "The dial only goes from 0 to " + this.maxLevel + "." );
			return;
		}

		if ( KoLCharacter.knollAvailable() && !InventoryManager.retrieveItem( MindControlRequest.RADIO ) )
		{
			return;
		}

		KoLmafia.updateDisplay( "Resetting mind control device..." );
		super.run();
	}

	@Override
	public void processResults()
	{
		if ( this.responseText.contains( "the radio" ) || this.responseText.contains( "You switch the dial" ) )
		{
			KoLmafia.updateDisplay( "Mind control device reset." );
			KoLCharacter.setMindControlLevel( this.level );
		}
		else
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "You failed to set the mind control device" );
		}
	}

	public static boolean registerRequest( final String urlString )
	{
		if ( urlString.indexOf( "action=changedial" ) != -1 && urlString.indexOf( "tuneradio" ) == -1 )
		{
			return false;
		}

		Matcher levelMatcher =
			KoLCharacter.knollAvailable() ? MindControlRequest.MCD2_PATTERN.matcher( urlString ) : MindControlRequest.MCD1_PATTERN.matcher( urlString );

		if ( !levelMatcher.find() )
		{
			return false;
		}

		RequestLogger.updateSessionLog( "mcd " + levelMatcher.group( 1 ) );
		return true;
	}
}
