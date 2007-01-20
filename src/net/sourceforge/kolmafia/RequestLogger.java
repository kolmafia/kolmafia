/**
 * Copyright (c) 2005-2006, KoLmafia development team
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

package net.sourceforge.kolmafia;

public class RequestLogger
{
	private static boolean wasLastRequestSimple = false;

	public static void registerRequest( String urlString )
	{
		// There are some adventures which do not post any
		// form fields, so handle them first.

		if ( KoLAdventure.recordToSession( urlString ) )
		{
			wasLastRequestSimple = false;
			return;
		}

		// Anything else that doesn't submit an actual form
		// should not be registered.

		if ( urlString.indexOf( "?" ) == -1 )
			return;

		// The following lists all the remaining requests in
		// alphabetical order.

		if ( AutoSellRequest.registerRequest( urlString ) )
		{
			wasLastRequestSimple = false;
			return;
		}

		if ( ClanStashRequest.registerRequest( urlString ) )
		{
			wasLastRequestSimple = false;
			return;
		}

		if ( ConsumeItemRequest.registerRequest( urlString ) )
		{
			wasLastRequestSimple = false;
			return;
		}

		if ( EquipmentRequest.registerRequest( urlString ) )
		{
			wasLastRequestSimple = false;
			return;
		}

		if ( FamiliarRequest.registerRequest( urlString ) )
		{
			wasLastRequestSimple = false;
			return;
		}

		if ( FightRequest.registerRequest( urlString ) )
		{
			wasLastRequestSimple = false;
			return;
		}

		if ( GiftMessageRequest.registerRequest( urlString ) )
		{
			wasLastRequestSimple = false;
			return;
		}

		if ( GreenMessageRequest.registerRequest( urlString ) )
		{
			wasLastRequestSimple = false;
			return;
		}

		if ( HermitRequest.registerRequest( urlString ) )
		{
			wasLastRequestSimple = false;
			return;
		}

		if ( ItemCreationRequest.registerRequest( urlString ) )
		{
			wasLastRequestSimple = false;
			return;
		}

		if ( ItemStorageRequest.registerRequest( urlString ) )
		{
			wasLastRequestSimple = false;
			return;
		}

		if ( ProposeTradeRequest.registerRequest( urlString ) )
		{
			wasLastRequestSimple = false;
			return;
		}

		if ( PulverizeRequest.registerRequest( urlString ) )
		{
			wasLastRequestSimple = false;
			return;
		}

		if ( UseSkillRequest.registerRequest( urlString ) )
		{
			wasLastRequestSimple = false;
			return;
		}

		// If it's an inventory page request that wasn't processed
		// in one of the above steps, pretend it doesn't exist.

		if ( urlString.indexOf( "inventory" ) != -1 )
			return;

		// Otherwise, make sure to print the raw URL so that it's
		// at least mentioned in the session log.

		if ( !wasLastRequestSimple )
			KoLmafia.getSessionStream().println();

		wasLastRequestSimple = true;
		KoLmafia.getSessionStream().println( urlString );
	}
}