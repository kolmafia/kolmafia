/**
 * Copyright (c) 2005-2007, KoLmafia development team
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

import java.math.BigInteger;
import java.security.MessageDigest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LoginRequest extends KoLRequest
{
	private static final Pattern SESSIONID_COOKIE_PATTERN = Pattern.compile( "PHPSESSID=([^\\;]+)" );
	private static final Pattern FAILURE_PATTERN = Pattern.compile( "<p><b>(.*?)</b>" );
	private static final Pattern CHALLENGE_PATTERN = Pattern.compile( "<input type=hidden name=challenge value=\"([^\"]*?)\">" );

	private static boolean ignoreLoadBalancer = false;
	private static String lastUsername;
	private static String lastPassword;
	private static boolean isLoggingIn;

	private static long lastLoginAttempt = 0;

	private static final long WAIT_THRESHOLD = 300000;
	private static final int STANDARD_WAIT = 75;
	private static final int TOO_MANY_WAIT = 960;
	private static final int BAD_CHALLENGE_WAIT = 15;

	private static int waitTime = STANDARD_WAIT;

	private String username;
	private String password;
	private boolean runCountdown;

	public LoginRequest( String username, String password )
	{
		super( "login.php" );

		this.username = username == null ? "" : StaticEntity.globalStringReplace( username, "/q", "" );
		StaticEntity.setGlobalProperty( this.username, "displayName", this.username );

		this.password = password;
		if ( StaticEntity.getClient() instanceof KoLmafiaCLI )
			StaticEntity.setProperty( "saveStateActive", "true" );
	}

	public static final void setIgnoreLoadBalancer( boolean ignoreLoadBalancer )
	{	LoginRequest.ignoreLoadBalancer = ignoreLoadBalancer;
	}

	protected boolean retryOnTimeout()
	{	return true;
	}

	/**
	 * Handles the challenge in order to send the password securely
	 * via KoL.
	 */

	private boolean detectChallenge()
	{
		KoLmafia.updateDisplay( "Validating login server..." );

		// Setup the login server in order to ensure that
		// the initial try is randomized.  Or, in the case
		// of a devster, the developer server.

		KoLRequest.applySettings();

		if ( this.username.startsWith( "devster" ) || this.username.equals( "holatuwol" ) )
			setLoginServer( "dev.kingdomofloathing.com" );

		this.clearDataFields();

		if ( ignoreLoadBalancer )
			this.constructURLString( "main.php" );

		super.run();

		if ( KoLmafia.refusesContinue() )
			return false;

		// If the pattern is not found, then do not submit
		// the challenge version.

		Matcher challengeMatcher = CHALLENGE_PATTERN.matcher( this.responseText );
		if ( !challengeMatcher.find() )
			return false;

		// We got this far, so that means we now have a
		// challenge pattern.

		try
		{
			this.constructURLString( "login.php" );
			String challenge = challengeMatcher.group(1);

			this.addFormField( "secure", "1" );
			this.addFormField( "password", "" );
			this.addFormField( "challenge", challenge );
			this.addFormField( "response", digestPassword( this.password, challenge ) );

			return true;
		}
		catch ( Exception e )
		{
			// An exception means bad things, so make sure to send the
			// original plaintext password.

			return false;
		}
	}

	private static final String digestPassword( String password, String challenge ) throws Exception
	{
		// KoL now makes use of a HMAC-MD5 in order to preprocess the
		// password so that we aren't submitting plaintext passwords
		// all the time.  Here is the implementation.  Note that the
		// password is processed two times.

		MessageDigest digester = MessageDigest.getInstance( "MD5" );
		String hash1 = getHexString( digester.digest( password.getBytes() ) );
		digester.reset();

		String hash2 = getHexString( digester.digest( (hash1 + ":" + challenge).getBytes() ) );
		digester.reset();

		return hash2;
	}

	private static final String getHexString( byte [] bytes )
	{
		byte [] output = new byte[ bytes.length + 1 ];
		for ( int i = 0; i < bytes.length; ++i )
			output[i+1] = bytes[i];

		StringBuffer result = new StringBuffer( (new BigInteger( output )).toString( 16 ) );
		int desiredLength = bytes.length * 2;

		while ( result.length() < desiredLength )
			result.insert( 0, '0' );

		if ( result.length() > desiredLength )
			result.delete( 0, result.length() - desiredLength );

		return result.toString();
	}

	/**
	 * Runs the <code>LoginRequest</code>.  This method determines
	 * whether or not the login was successful, and updates the
	 * display or notifies the as appropriate.
	 */

	public void run()
	{
		this.runCountdown = true;
		StaticEntity.getClient().setCurrentRequest( null );

		lastUsername = this.username;
		lastPassword = this.password;
		KoLmafia.forceContinue();

		try
		{
			this.runCountdown = true;

			if ( this.executeLogin() && this.runCountdown )
			{
				StaticEntity.executeCountdown( "Next login attempt in ", waitTime );
				if ( !KoLmafia.refusesContinue() && this.executeLogin() )
					forceLoginAbort();
			}
		}
		catch ( Exception e )
		{
			// It's possible that all the login hangups are due
			// to an exception in executeLogin().  Let's try to
			// catch it.

			StaticEntity.printStackTrace( e );
		}
	}

	private static final void forceLoginAbort()
	{
		StaticEntity.printStackTrace( new Exception(), "Concurrent logins detected" );
		System.exit(-1);
	}

	public static final void executeTimeInRequest()
	{
		if ( System.currentTimeMillis() - lastLoginAttempt < WAIT_THRESHOLD )
			forceLoginAbort();

		sessionId = null;
		waitTime = STANDARD_WAIT;

		(new LoginRequest( lastUsername, lastPassword )).run();

		if ( sessionId != null )
			KoLmafia.updateDisplay( "Session timed-in." );

	}

	public boolean executeLogin()
	{
		sessionId = null;
		lastLoginAttempt = System.currentTimeMillis();

		if ( waitTime == BAD_CHALLENGE_WAIT || !this.runCountdown || !this.detectChallenge() )
		{
			this.clearDataFields();
			this.addFormField( "loginname", this.username );
			this.addFormField( "password", this.password );
		}
		else
		{
			this.addFormField( "loginname", this.username + "/q" );
		}

		this.addFormField( "loggingin", "Yup." );
		waitTime = STANDARD_WAIT;

		sessionId = null;

		if ( KoLmafia.refusesContinue() )
			return false;

		KoLmafia.updateDisplay( "Sending login request..." );
		super.run();

		if ( KoLmafia.refusesContinue() )
			return false;

		if ( this.responseCode == 302 && this.redirectLocation.equals( "maint.php" ) )
		{
			// Nightly maintenance, so KoLmafia should not bother
			// retrying.  Let the user do it manually later.

			KoLmafia.updateDisplay( ABORT_STATE, "Nightly maintenance." );
			return false;
		}
		else if ( this.responseCode == 302 && this.redirectLocation.startsWith( "main" ) )
		{
			processLoginRequest( this );
			return false;
		}
		else if ( this.responseCode == 302 )
		{
			// It's possible that KoL will eventually make the redirect
			// the way it used to be, but enforce the redirect.  If this
			// happens, then validate here.

			Matcher matcher = REDIRECT_PATTERN.matcher( this.redirectLocation );
			if ( matcher.find() )
			{
				this.runCountdown = false;
				setLoginServer( matcher.group(1) );
				return true;
			}
		}
		else if ( this.responseText.indexOf( "wait fifteen minutes" ) != -1 )
		{
			// Ooh, logged in too fast.  KoLmafia should recognize this and
			// try again automatically in 1000 seconds.

			waitTime = TOO_MANY_WAIT;
			return true;
		}
		else if ( this.responseText.indexOf( "wait" ) != -1 )
		{
			// Ooh, logged in too fast.  KoLmafia should recognize this and
			// try again automatically in 75 seconds.

			waitTime = STANDARD_WAIT;
			return true;
		}
		else if ( this.responseText.indexOf( "login.php" ) != -1 )
		{
			// KoL sometimes switches servers while logging in. It returns a hidden form
			// with responseCode 200.

			// <html>
			//   <body>
			//     <form name=formredirect method=post action="http://www.kingdomofloathing.com/login.php">
			//	 <input type=hidden name=loginname value="xxx">
			//	 <input type=hidden name=loggingin value="Yup.">
			//	 <input type=hidden name=password value="xxx">
			//     </form>
			//   </body>
			// </html>Redirecting to www.

			this.runCountdown = false;
			Matcher matcher = REDIRECT_PATTERN.matcher( this.responseText );

			if ( matcher.find() )
			{
				setLoginServer( matcher.group(1) );
				return true;
			}
		}
		else if ( this.responseText.indexOf( "Too many" ) != -1 )
		{
			// Too many bad logins in too short a time span.
			// Notify the user that something bad happened.

			KoLmafia.updateDisplay( ABORT_STATE, "Too many failed login attempts." );
			return false;
		}

		Matcher failureMatcher = FAILURE_PATTERN.matcher( this.responseText );
		if ( failureMatcher.find() )
			KoLmafia.updateDisplay( ERROR_STATE, failureMatcher.group(1) );
		else
			KoLmafia.updateDisplay( ABORT_STATE, "Encountered error in login." );

		waitTime = BAD_CHALLENGE_WAIT;
		return true;
	}

	public static final boolean isInstanceRunning()
	{	return isLoggingIn;
	}

	public static final void processLoginRequest( KoLRequest request )
	{
		if ( request.redirectLocation == null || request.redirectLocation.startsWith( "maint" ) || !request.redirectLocation.startsWith( "main" ) )
			return;

		if ( request.redirectLocation.equals( "main_c.html" ) )
			KoLRequest.isCompactMode = true;

		// If the login is successful, you notify the client
		// of success.  But first, if there was a desire to
		// save the password, do so here.

		String serverCookie = request.formConnection.getHeaderField( "Set-Cookie" );
		if ( serverCookie != null )
		{
			Matcher sessionMatcher = SESSIONID_COOKIE_PATTERN.matcher( serverCookie );
			if ( sessionMatcher.find() )
				KoLRequest.sessionId = "PHPSESSID=" + sessionMatcher.group(1) + "; path=/";
		}

		String name = request.getFormField( "loginname" );
		if ( name.endsWith( "/q" ) )
			name = name.substring( 0, name.length() - 2 ).trim();

		RequestThread.openRequestSequence();

		isLoggingIn = true;
		StaticEntity.getClient().initialize( name );

		isLoggingIn = false;

		String scriptSetting = StaticEntity.getProperty( "loginScript" );
		if ( !scriptSetting.equals( "" ) )
			DEFAULT_SHELL.executeLine( scriptSetting );

		RequestThread.postRequest( new KoLRequest( "chatlaunch.php" ) );
		RequestThread.closeRequestSequence();

		if ( StaticEntity.getBooleanProperty( "saveStateActive" ) && request instanceof LoginRequest )
			KoLmafia.addSaveState( lastUsername, lastPassword );
	}
}
