/**
 * Copyright (c) 2005, KoLmafia development team
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
 *  [3] Neither the name "KoLmafia development team" nor the names of
 *      its contributors may be used to endorse or promote products
 *      derived from this software without specific prior written
 *      permission.
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

public abstract class StaticEntity implements KoLConstants
{
	static
	{
		// In order to minimize error internally, KoLmafia will
		// use the same time zone as KoL for calculations.

		System.setProperty( "user.timezone", "America/Halifax" );
		java.util.TimeZone.setDefault( java.util.TimeZone.getTimeZone( "America/Halifax" ) );
	}

	protected static KoLmafia client;

	public static final void setClient( KoLmafia client )
	{	StaticEntity.client = client;
	}

	public static KoLmafia getClient()
	{	return client;
	}

	public static void closeSession()
	{
		if ( client == null )
			return;

		Object [] frames = existingFrames.toArray();

		for ( int i = 0; i < frames.length; ++i )
			((KoLFrame)frames[i]).dispose();

		BuffBotHome.setBuffBotActive( false );
		client.closeMacroStream();

		KoLCharacter.reset( "" );
		(new RequestThread( new LogoutRequest( client ) )).start();
	}

	protected static final KoLSettings getSettings()
	{	return client == null ? GLOBAL_SETTINGS : client.getSettings();
	}

	protected static final void setProperty( String name, String value )
	{
		if ( client != null )
			client.getSettings().setProperty( name, value );
		else
			GLOBAL_SETTINGS.setProperty( name, value );
	}

	protected static final String getProperty( String name )
	{	return client == null ? GLOBAL_SETTINGS.getProperty( name ) : client.getSettings().getProperty( name );
	}
}