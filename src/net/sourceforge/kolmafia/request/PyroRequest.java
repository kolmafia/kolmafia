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

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.RequestLogger;

import net.sourceforge.kolmafia.objectpool.ItemPool;

import net.sourceforge.kolmafia.session.ResultProcessor;

import net.sourceforge.kolmafia.webui.IslandDecorator;

public class PyroRequest
	extends GenericRequest
{
	public static final AdventureResult GUNPOWDER = ItemPool.get( ItemPool.GUNPOWDER, 1 );

	public PyroRequest()
	{
		super( IslandDecorator.currentIsland() );
		this.addFormField( "place", "lighthouse" );
		this.addFormField( "action", "pyro" );
	}

	public void processResults()
	{
                PyroRequest.parseResponse( this.getURLString(), this.responseText );
	}

	public static final void parseResponse( final String location, final String responseText )
	{
		if ( !location.startsWith( IslandDecorator.currentIsland() ) )
		{
			return;
		}

                // "The Lighthouse Keeper's eyes light up as he sees your
                // gunpowder.<p>&quot;Big boom!  Big big boom!  Give me those,
                // <i>bumpty-bump</i>, and I'll make you the big
                // boom!&quot;<p>He takes the gunpowder into a back room, and
                // returns with an armload of big bombs."

		if ( responseText.indexOf( "eyes light up" ) != -1 )
		{
			int count = PyroRequest.GUNPOWDER.getCount( KoLConstants.inventory );
			ResultProcessor.processItem( ItemPool.GUNPOWDER, -count );
			return;
		}
	}

	public static final String pyroURL()
	{
		return IslandDecorator.currentIsland() + "?place=lighthouse&action=pyro";
	}

	public static final boolean registerRequest( final String urlString )
	{
		if ( !urlString.startsWith( IslandDecorator.currentIsland() ) )
		{
			return false;
		}

		if ( urlString.indexOf( "action=pyro" ) == -1 )
		{
			return false;
		}

		int count = PyroRequest.GUNPOWDER.getCount( KoLConstants.inventory );
		String message = "Visiting the lighthouse keeper with " + count + " barrel" + ( count > 1 ? "s" : "" ) + " of gunpowder.";

		RequestLogger.printLine( message );
		RequestLogger.updateSessionLog( message );

		return true;
	}
}
