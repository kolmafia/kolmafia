/**
 * Copyright (c) 2005-2008, KoLmafia development team
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

import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.KoLmafiaCLI;
import net.sourceforge.kolmafia.LocalRelayAgent;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.StaticEntity;
import net.sourceforge.kolmafia.persistence.HolidayDatabase;
import net.sourceforge.kolmafia.persistence.Preferences;
import net.sourceforge.kolmafia.session.ActionBarManager;
import net.sourceforge.kolmafia.session.BreakfastManager;
import net.sourceforge.kolmafia.session.MushroomManager;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class LoginRequest
	extends GenericRequest
{
	private static boolean completedLogin = false;
	private static final Pattern CHALLENGE_PATTERN =
		Pattern.compile( "<input type=hidden name=challenge value=\"([^\"]*?)\">" );

	private static boolean ignoreLoadBalancer = false;
	private static LoginRequest lastRequest = null;
	private static long lastLoginAttempt = 0;
	
	private static boolean isLoggingIn;

	private final String username;
	private final String password;

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
		if ( lowercase.startsWith( "devster" ) || lowercase.equals( "holatuwol" ) || lowercase.equals( "hogulus" ) || lowercase.equals( "armak" ) )
		{
			GenericRequest.setLoginServer( "dev.kingdomofloathing.com" );
		}

		KoLmafia.updateDisplay( "Validating login server (" + GenericRequest.KOL_HOST + ")..." );

		GenericRequest.serverCookie = null;
		GenericRequest.passwordHash = "";

		this.clearDataFields();

		if ( LoginRequest.ignoreLoadBalancer )
		{
			this.constructURLString( "main.php" );
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

		// We got this far, so that means we now have a
		// challenge pattern.

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
		byte[] output = new byte[ bytes.length + 1 ];
		for ( int i = 0; i < bytes.length; ++i )
		{
			output[ i + 1 ] = bytes[ i ];
		}

		StringBuffer result = new StringBuffer( ( new BigInteger( output ) ).toString( 16 ) );
		int desiredLength = bytes.length * 2;

		while ( result.length() < desiredLength )
		{
			result.insert( 0, '0' );
		}

		if ( result.length() > desiredLength )
		{
			result.delete( 0, result.length() - desiredLength );
		}

		return result.toString();
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
		GenericRequest.serverCookie = null;
		LocalRelayAgent.reset();

		if ( Preferences.getBoolean( "saveStateActive" ) )
		{
			KoLmafia.addSaveState( this.username, this.password );
		}

		LoginRequest.lastRequest = this;
		LoginRequest.lastLoginAttempt = System.currentTimeMillis();
		
		KoLmafia.forceContinue();

		if ( this.detectChallenge() )
		{
			this.addFormField( "loginname", this.username + "/q" );
		}
		else
		{
			this.clearDataFields();
			this.addFormField( "loginname", this.username + "/q" );
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

		if ( this.responseText.indexOf( "wait fifteen minutes" ) != -1 )
		{
			StaticEntity.executeCountdown( "Login reattempt in ", 15 * 60 );
			this.run();
			return;
		}

		if ( this.responseText.indexOf( "wait" ) != -1 )
		{
			StaticEntity.executeCountdown( "Login reattempt in ", 75 );
			this.run();
			return;
		}

		if ( this.responseText.indexOf( "Too many" ) != -1 )
		{
			// Too many bad logins in too short a time span.
			KoLmafia.updateDisplay( KoLConstants.ABORT_STATE, "Too many failed login attempts." );
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

	public static final boolean executeTimeInRequest()
	{
		return LoginRequest.executeTimeInRequest( "main.php", "login.php" );
	}

	public static final boolean executeTimeInRequest( final String requestLocation, final String redirectLocation )
	{
		if ( LoginRequest.lastRequest == null )
		{
			return false;
		}

		// If it's been less than 30 seconds since the last
		
		if ( System.currentTimeMillis() - 30000 < LoginRequest.lastLoginAttempt )
		{
			StaticEntity.printStackTrace( "Possible concurrent logins on multiple machines." );
			System.exit( -1 );
		}

		if ( LoginRequest.isInstanceRunning() )
		{
			StaticEntity.printStackTrace( requestLocation + " => " + redirectLocation );
			System.exit( -1 );
		}

		RequestThread.postRequest( LoginRequest.lastRequest );
		return true;
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
		if ( request.redirectLocation.equals( "main_c.html" ) )
		{
			GenericRequest.isCompactMode = true;
		}

		// If the login is successful, you notify the client
		// of success.  But first, if there was a desire to
		// save the password, do so here.

		String name = request.getFormField( "loginname" );
		if ( name.endsWith( "/q" ) )
		{
			name = name.substring( 0, name.length() - 2 ).trim();
		}

		RequestThread.openRequestSequence();
		LoginRequest.isLoggingIn = true;

		KoLCharacter.reset( name );

		ActionBarManager.loadJSONString();
		StaticEntity.getClient().initialize( name );

		LoginRequest.isLoggingIn = false;

		if ( Preferences.getString( name, "getBreakfast" ).equals( "true" ) )
		{
			int today = HolidayDatabase.getPhaseStep();
			BreakfastManager.getBreakfast( true, Preferences.getInteger( "lastBreakfast" ) != today );
			Preferences.setInteger( "lastBreakfast", today );
		}

		// Also, do mushrooms, if a mushroom script has already
		// been setup by the user.

		if ( Preferences.getBoolean( "autoPlant" + ( KoLCharacter.canInteract() ? "Softcore" : "Hardcore" ) ) )
		{
			String currentLayout = Preferences.getString( "plantingScript" );
			if ( !currentLayout.equals( "" ) && KoLCharacter.inMuscleSign() && MushroomManager.ownsPlot() )
			{
				KoLmafiaCLI.DEFAULT_SHELL.executeLine( "call " + KoLConstants.PLOTS_DIRECTORY + currentLayout + ".ash" );
			}
		}

		String scriptSetting = Preferences.getString( "loginScript" );
		if ( !scriptSetting.equals( "" ) )
		{
			KoLmafiaCLI.DEFAULT_SHELL.executeLine( scriptSetting );
		}

		RequestThread.closeRequestSequence();
	}
}
