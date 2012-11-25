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

import net.sourceforge.kolmafia.KoLCharacter;

import net.sourceforge.kolmafia.objectpool.ItemPool;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.session.ResultProcessor;

import net.sourceforge.kolmafia.utilities.StringUtilities;

public class OrcChasmRequest
	extends GenericRequest
{
	private static final Pattern ACTION_PATTERN = Pattern.compile( "action=bridge([^>]*)" );

	public OrcChasmRequest()
	{
		super( "place.php?whichplace=orc_chasm" );
	}

	@Override
	public void processResults()
	{
		if ( !this.getURLString().startsWith( "place.php?whichplace=orc_chasm" ) )
		{
			return;
		}

		OrcChasmRequest.parseResponse( this.getURLString(), this.responseText );
	}

	public static final void parseResponse( final String location, final String responseText )
	{
		Matcher actionMatcher = OrcChasmRequest.ACTION_PATTERN.matcher( responseText );
		String action = actionMatcher.find() ? actionMatcher.group(1) : null;

		if ( action != null )
		{
			if ( action.equals( "_done" ) )
			{
				OrcChasmRequest.setChasmProgress( 30 );
			}
			else
			{
				OrcChasmRequest.setChasmProgress( StringUtilities.parseInt( action ) );
			}
		}

		if ( responseText.contains( "You disassemble it into usable lumber and fasteners." ) )
		{
			ResultProcessor.processItem( ItemPool.BRIDGE, -1 );
		}
	}

	private static final void ensureUpdatedChasm()
	{
		int lastAscension = Preferences.getInteger( "lastChasmReset" );
		if ( lastAscension < KoLCharacter.getAscensions() )
		{
			Preferences.setInteger( "lastChasmReset", KoLCharacter.getAscensions() );
			
			Preferences.setInteger( "chasmBridgeProgress", 0 );
		}
	}

	public static final int getChasmProgress()
	{
		OrcChasmRequest.ensureUpdatedChasm();
		return Preferences.getInteger( "chasmBridgeProgress" );
	}

	public static final void setChasmProgress( int progress )
	{
		OrcChasmRequest.ensureUpdatedChasm();
		Preferences.setInteger( "chasmBridgeProgress", progress );
	}

}
