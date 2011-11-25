/**
 * Copyright (c) 2005-2011, KoLmafia development team
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

import java.math.BigInteger;

import java.security.MessageDigest;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.KoLmafiaCLI;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.StaticEntity;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.session.LoginManager;
import net.sourceforge.kolmafia.utilities.StringUtilities;

import net.sourceforge.kolmafia.webui.RelayAgent;

public class LoginRequest
	extends GenericRequest
{
	private static boolean completedLogin = false;
	private static final Pattern CHALLENGE_PATTERN =
		Pattern.compile( "<input type=hidden name=challenge value=\"([^\"]*?)\">" );
	private static final Pattern PLAYERS_PATTERN =
		Pattern.compile( "There are currently <b>(.*?)</b> players logged in." );
	private static final Pattern GRIMBO_PATTERN =
		Pattern.compile( "(Elves Defeated:)</b></td><td class=small>([0-9,]+)</td></tr><tr><td class=small><b>(Elves Cured:)</b></td><td class=small>([0-9,]+)</td></tr><tr><td colspan=2 height=2 bgcolor=black></td></tr><tr><td class=small><b>(Penguins Defeated:)&nbsp;&nbsp;&nbsp;&nbsp;</b></td><td class=small>([0-9,]+)</td></tr><tr><td class=small><b>(Penguins Bribed:)</b></td><td class=small>([0-9,]+)" );

	private static boolean ignoreLoadBalancer = false;
	private static LoginRequest lastRequest = null;
	private static long lastLoginAttempt = 0;

	private static boolean isLoggingIn;
	private static boolean isTimingIn = false;

	private final String username;
	private final String password;

	public static int playersOnline = 0;

	public LoginRequest( final String username, final String password )
	{
		super( "login.php" );

		this.username = username == null ? "" : StringUtilities.globalStringReplace( username, "/q", "" );
		Preferences.setString( this.username, "displayName", this.username );

		this.password = password;
		if ( StaticEntity.getClient() instanceof KoLmafiaCLI )
		{
			Preferences.setBoolean( "saveStateActive", true );
		}
	}

	public static final void setIgnoreLoadBalancer( final boolean ignoreLoadBalancer )
	{
		LoginRequest.ignoreLoadBalancer = ignoreLoadBalancer;
	}

	protected boolean retryOnTimeout()
	{
		return true;
	}

	public String getURLString()
	{
		return "login.php";
	}

	/**
	 * Handles the challenge in order to send the password securely via KoL.
	 */

	private boolean detectChallenge()
	{
		// Setup the login server in order to ensure that
		// the initial try is randomized.  Or, in the case
		// of a devster, the developer server.

		GenericRequest.applySettings();

		String lowercase = this.username.toLowerCase();

		// This used to automatically force a "whitelist" of other
		// developers - holatuwol, hogulus, armak - to the dev server
		// as well, but when the code running on the dev server was
		// sufficiently changed that KoLmafia couldn't parse the
		// inventory anymore, Armak called for help.

		if ( lowercase.startsWith( "devster" ) )
		{
			GenericRequest.setLoginServer( "dev.kingdomofloathing.com" );
		}

		KoLmafia.updateDisplay( "Validating login server (" + GenericRequest.KOL_HOST + ")..." );

		GenericRequest.reset();

		this.clearDataFields();

		if ( LoginRequest.ignoreLoadBalancer )
		{
			this.constructURLString( "game.php" );
		}

		super.run();

		if ( KoLmafia.refusesContinue() )
		{
			return false;
		}

		// If the pattern is not found, then do not submit
		// the challenge version.

		Matcher challengeMatcher = LoginRequest.CHALLENGE_PATTERN.matcher( this.responseText );
		if ( !challengeMatcher.find() )
		{
			return false;
		}

		// We got this far, so that means we now have a challenge
		// pattern.

		Matcher playersMatcher = LoginRequest.PLAYERS_PATTERN.matcher( this.responseText );
		if ( playersMatcher.find() )
		{
			LoginRequest.playersOnline = StringUtilities.parseInt( playersMatcher.group( 1 ) );
			KoLmafia.updateDisplay( LoginRequest.playersOnline + " players online." );
		}

		/*
		playersMatcher = LoginRequest.GRIMBO_PATTERN.matcher( this.responseText );
		if ( playersMatcher.find() )
		{
			KoLmafia.updateDisplay( playersMatcher.group( 1 ) + " " +
				playersMatcher.group( 2 ) );
			KoLmafia.updateDisplay( playersMatcher.group( 3 ) + " " +
				playersMatcher.group( 4 ) );
			KoLmafia.updateDisplay( playersMatcher.group( 5 ) + " " +
				playersMatcher.group( 6 ) );
			KoLmafia.updateDisplay( playersMatcher.group( 7 ) + " " +
				playersMatcher.group( 8 ) );
		}
		*/

		try
		{
			this.constructURLString( "login.php" );
			String challenge = challengeMatcher.group( 1 );

			this.addFormField( "secure", "1" );
			this.addFormField( "password", "" );
			this.addFormField( "challenge", challenge );
			this.addFormField( "response", LoginRequest.digestPassword( this.password, challenge ) );

			return true;
		}
		catch ( Exception e )
		{
			// An exception means bad things, so make sure to send the
			// original plaintext password.

			return false;
		}
	}

	private static final String digestPassword( final String password, final String challenge )
		throws Exception
	{
		// KoL now makes use of a HMAC-MD5 in order to preprocess the
		// password so that we aren't submitting plaintext passwords
		// all the time.  Here is the implementation.  Note that the
		// password is processed two times.

		MessageDigest digester = MessageDigest.getInstance( "MD5" );
		String hash1 = LoginRequest.getHexString( digester.digest( password.getBytes() ) );
		digester.reset();

		String hash2 = LoginRequest.getHexString( digester.digest( ( hash1 + ":" + challenge ).getBytes() ) );
		digester.reset();

		return hash2;
	}

	private static final String getHexString( final byte[] bytes )
	{
		byte[] nonNegativeBytes = new byte[ bytes.length + 1 ];
		System.arraycopy( bytes, 0, nonNegativeBytes, 1, bytes.length );

		StringBuffer hexString = new StringBuffer( 64 );

		hexString.append( "00000000000000000000000000000000" );
		hexString.append( new BigInteger( nonNegativeBytes ).toString( 16 ) );
		hexString.delete( 0, hexString.length() - 32 );

		return hexString.toString();
	}

	public boolean shouldFollowRedirect()
	{
		return true;
	}

	/**
	 * Runs the <code>LoginRequest</code>. This method determines whether or not the login was successful, and
	 * updates the display or notifies the as appropriate.
	 */

	public void run()
	{
		LoginRequest.completedLogin = false;

		GenericRequest.reset();
		RelayAgent.reset();

		if ( Preferences.getBoolean( "saveStateActive" ) )
		{
			KoLmafia.addSaveState( this.username, this.password );
		}

		LoginRequest.lastRequest = this;
		LoginRequest.lastLoginAttempt = System.currentTimeMillis();

		KoLmafia.forceContinue();

		String loginName = Preferences.getBoolean( "stealthLogin" ) ? this.username + "/q" : this.username;
		if ( this.detectChallenge() )
		{
			this.addFormField( "loginname", loginName );
		}
		else
		{
			this.clearDataFields();
			this.addFormField( "loginname", loginName );
			this.addFormField( "password", this.password );
		}

		this.addFormField( "loggingin", "Yup." );
		KoLmafia.updateDisplay( "Sending login request..." );

		super.run();

		if ( this.responseCode != 200 )
		{
			return;
		}

		LoginRequest.lastLoginAttempt = 0;

		if ( this.responseText.indexOf( "Bad password" ) != -1 )
		{
			KoLmafia.updateDisplay( KoLConstants.ABORT_STATE, "Bad password." );
			return;
		}

		if ( this.responseText.indexOf( "wait fifteen minutes" ) != -1 )
		{
			StaticEntity.executeCountdown( "Login reattempt in ", 15 * 60 );
			this.run();
			return;
		}

		// Too many login attempts in too short a span of time.	 Please
		// wait a minute (Literally, like, one minute.	Sixty seconds.)
		// and try again.

		// Whoops -- it looks like you had a recent session open that
		// didn't get logged out of properly.  We apologize for the
		// inconvenience, but you'll need to wait a couple of minutes
		// before you can log in again.

		if ( this.responseText.indexOf( "wait a minute" ) != -1 ||
                     this.responseText.indexOf( "wait a couple of minutes" ) != -1 )
		{
			StaticEntity.executeCountdown( "Login reattempt in ", 75 );
			this.run();
			return;
		}

		if ( this.responseText.indexOf( "Too many" ) != -1 )
		{
			// Too many bad logins in too short a time span.
			int pos = this.responseText.indexOf("Too many");
			int pos2 = this.responseText.indexOf("<",pos+1);
			KoLmafia.updateDisplay( KoLConstants.ABORT_STATE, this.responseText.substring(pos,pos2));
			return;
		}

		if ( GenericRequest.KOL_HOST.equals( "dev.kingdomofloathing.com" ) &&
			this.responseText.indexOf( "do not have the privileges" ) != -1)
		{
			// Can't use dev server without permission. Skip it.
			Preferences.setInteger( "defaultLoginServer", 1 );
			this.run();
			return;
		}

		KoLmafia.updateDisplay( KoLConstants.ABORT_STATE, "Encountered error in login." );
	}

	public static final boolean executeTimeInRequest( final String requestLocation, final String redirectLocation )
	{
		if ( LoginRequest.lastRequest == null || LoginRequest.isTimingIn )
		{
			return false;
		}

		// If it's been less than 30 seconds since the last login
		// attempt, we could be responding to the flurry of login.php
		// redirects KoL gives us when the Relay Browser tries to open
		// game.php, topmenu.php, chatlaunch.php, etc.

		if ( System.currentTimeMillis() - 30000 < LoginRequest.lastLoginAttempt )
		{
			return LoginRequest.completedLogin;
		}

		if ( LoginRequest.isInstanceRunning() )
		{
			StaticEntity.printStackTrace( requestLocation + " => " + redirectLocation );
			System.exit( -1 );
		}

		LoginRequest.isTimingIn = true;
		RequestThread.postRequest( LoginRequest.lastRequest );
		LoginRequest.isTimingIn = false;

		return LoginRequest.completedLogin;
	}

	public static final void isLoggingIn( final boolean isLoggingIn )
	{
		LoginRequest.isLoggingIn = isLoggingIn;
	}

	public static final boolean isInstanceRunning()
	{
		return LoginRequest.isLoggingIn;
	}

	public static final boolean completedLogin()
	{
		return LoginRequest.completedLogin;
	}

	public static final void processLoginRequest( final GenericRequest request )
	{
		if ( request.redirectLocation == null )
		{
			return;
		}

		String serverCookie = request.formConnection.getHeaderField( "Set-Cookie" );
		if ( serverCookie != null )
		{
			int semiIndex = serverCookie.indexOf( ";" );
			if ( semiIndex != -1 )
			{
				GenericRequest.serverCookie = serverCookie.substring( 0, semiIndex );
			}
			else
			{
				GenericRequest.serverCookie = serverCookie;
			}
		}

		// It's possible that KoL will eventually make the redirect
		// the way it used to be, but enforce the redirect.  If this
		// happens, then validate here.

		LoginRequest.completedLogin = true;

		// If login is successful, notify client of success.

		String name = request.getFormField( "loginname" );
		if ( name.endsWith( "/q" ) )
		{
			name = name.substring( 0, name.length() - 2 ).trim();
		}

		if ( LoginRequest.isTimingIn )
		{
			StaticEntity.getClient().timein( name );
		}
		else
		{
			LoginManager.login( name );
		}
	}
}
