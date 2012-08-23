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

import net.sourceforge.kolmafia.RequestLogger;

public class Crimbo10Request
	extends GenericRequest
{
	public Crimbo10Request()
	{
		super( "crimbo10.php" );
	}

	@Override
	public void processResults()
	{
		Crimbo10Request.parseResponse( this.getURLString(), this.responseText );
	}

	public static final void parseResponse( final String location, final String responseText )
	{
		if ( !location.startsWith( "crimbo10.php" ) )
		{
			return;
		}

		String action = GenericRequest.getAction( location );
		if ( action == null || action.equals( "buygift" ) )
		{
			CRIMBCOGiftShopRequest.parseResponse( location, responseText );
			return;
		}
	}

	public static String locationName( final String urlString )
	{
		if ( urlString.indexOf( "place=office" ) != -1 )
		{
			return "Mr. Mination's Office";
		}
		if ( urlString.indexOf( "place=giftshop" ) != -1 )
		{
			return "the Gift Shop";
		}
		return null;
	}

	private static String visitLocation( final String urlString )
	{
		String name = Crimbo10Request.locationName( urlString );
		if ( name != null )
		{
			return "Visiting " + name + " in CRIMBCO Headquarters";
		}
		return null;
	}

	public static final boolean registerRequest( final String urlString )
	{
		if ( !urlString.startsWith( "crimbo10.php" ) )
		{
			return false;
		}

		String action = GenericRequest.getAction( urlString );
		String message = null;

		// We want to log certain simple visits
		if ( action == null )
		{
			message = Crimbo10Request.visitLocation( urlString );
		}

		// Buy stuff in the CRIMBCO Gift Shop
		else if ( action.equals( "buygift" ) )
		{
			// Let CRIMBCOGiftShopRequest claim this
			return CRIMBCOGiftShopRequest.registerRequest( urlString );
		}

		// Unknown action
		else
		{
			return false;
		}

		if ( message == null )
		{
			return true;
		}

		RequestLogger.printLine();
		RequestLogger.updateSessionLog();
		RequestLogger.printLine( message );
		RequestLogger.updateSessionLog( message );

		return true;
	}
}
