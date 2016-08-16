/**
 * Copyright (c) 2005-2016, KoLmafia development team
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

import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.RequestThread;

import net.sourceforge.kolmafia.objectpool.ItemPool;

import net.sourceforge.kolmafia.session.ChoiceManager;

import net.sourceforge.kolmafia.utilities.StringUtilities;

public class TerminalRequest
	extends GenericRequest
{
	public TerminalRequest( final String input )
	{
		super( "choice.php" );
		this.addFormField( "whichchoice", "1191" );
		this.addFormField( "option", "1" );
		this.addFormField( "input", input );
	}

	@Override
	public void run()
	{
		if ( !KoLCharacter.inNuclearAutumn() && !KoLConstants.campground.contains( ItemPool.get( ItemPool.SOURCE_TERMINAL ) ) )
		{
			KoLmafia.updateDisplay( KoLConstants.MafiaState.ERROR, "You don't have a Source terminal." );
			return;
		}
		if ( KoLCharacter.inNuclearAutumn() )
		{
			RequestThread.postRequest( new PlaceRequest( "falloutshelter", "vault_term", true ) );
		}
		else
		{
			RequestThread.postRequest( new CampgroundRequest( "terminal" ) );
		}
		super.run();
	}

	@Override
	public void processResults()
	{
		KoLmafia.updateDisplay( "Source Terminal used." );
	}

	public static boolean registerRequest( final String urlString )
	{
		if ( !urlString.startsWith( "choice.php" ) )
		{
			return false;
		}

		int choice = ChoiceManager.extractChoiceFromURL( urlString );

		if ( choice != 1191 )
		{
			return false;
		}
		
		String input = GenericRequest.extractField( urlString, "input" );
		if ( input == null )
		{
			return false;
		}
		input = StringUtilities.globalStringReplace( input.substring( 6 ), "+", " " );

		String message = "Source Terminal: " + input;
		RequestLogger.printLine( message );
		RequestLogger.updateSessionLog( message );
		return true;
	}
}
