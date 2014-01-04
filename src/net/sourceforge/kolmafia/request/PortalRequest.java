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

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestLogger;

import net.sourceforge.kolmafia.objectpool.ItemPool;

import net.sourceforge.kolmafia.session.ResultProcessor;

public class PortalRequest
	extends GenericRequest
{
	private static final Pattern WHERE_PATTERN = Pattern.compile( "action=(\\w*)elvibratoportal" );

	private final AdventureResult item;

	/**
	 * Constructs a new <code>PortalRequest</code>
	 */

	public PortalRequest( final AdventureResult item )
	{
		super( "campground.php" );

		this.item = item;

		switch ( item.getItemId() )
		{
		case ItemPool.POWER_SPHERE:
			this.addFormField( "action", "powerelvibratoportal" );
			break;
		case ItemPool.OVERCHARGED_POWER_SPHERE:
			this.addFormField( "action", "overpowerelvibratoportal" );
			break;
		}
	}

	@Override
	public void run()
	{
		int iterations = this.item.getCount();

		for ( int i = 1; i <= iterations && KoLmafia.permitsContinue(); ++i )
		{
			if ( iterations == 1 )
			{
				KoLmafia.updateDisplay( "Charging your El Vibrato portal..." );
			}
			else
			{
				KoLmafia.updateDisplay( "Charging your El Vibrato portal (" + i + " of " + iterations + ")..." );
			}
			super.run();
		}

		if ( KoLmafia.permitsContinue() )
		{
			KoLmafia.updateDisplay( "Finished using " + iterations + " " + this.item.getName() + "." );
		}
	}

	@Override
	public void processResults()
	{
		PortalRequest.parseResponse( this.getURLString(), this.responseText );
	}

	private static final AdventureResult getSphere( final String urlString )
	{
		Matcher matcher = PortalRequest.WHERE_PATTERN.matcher( urlString );
		if ( !matcher.find() )
		{
			return null;
		}

		String action = matcher.group(1);
		if ( action.equals( "power" ) )
		{
			return ItemPool.get( ItemPool.POWER_SPHERE, -1 );
		}
		if ( action.equals( "overpower" ) )
		{
			return ItemPool.get( ItemPool.OVERCHARGED_POWER_SPHERE, -1 );
		}
		return null;
	}

	public static final void parseResponse( final String urlString, final String responseText )
	{
		if ( !urlString.startsWith( "campground.php" ) )
		{
			return;
		}

		AdventureResult item = PortalRequest.getSphere( urlString );

		if ( item == null )
		{
			return;
		}

		ResultProcessor.processResult( item );
	}

	public static final boolean registerRequest( final String urlString )
	{
		if ( !urlString.startsWith( "campground.php" ) )
		{
			return false;
		}

		AdventureResult item = PortalRequest.getSphere( urlString );
		if ( item == null )
		{
			return false;
		}

		RequestLogger.updateSessionLog();
		RequestLogger.updateSessionLog( "use 1 " + item.getName() );

		return true;
	}
}
