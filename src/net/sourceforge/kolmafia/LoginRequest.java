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
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * An extension of <code>KoLRequest</code> which handles logins.
 * A new instance is created and started for every login attempt,
 * and in the event that it is successful, the client provided
 * at construction time will be notified of the success.
 */

public class LoginRequest extends KoLRequest
{
	private static String lastUsername;
	private static String lastPassword;

	private static boolean isRollover = false;
	private static boolean instanceRunning = false;

	private String username;
	private String password;
	private boolean savePassword;
	private boolean getBreakfast;
	private boolean isQuickLogin;

	/**
	 * Constructs a new <code>LoginRequest</code>.  The given
	 * client will be notified in the event of success.
	 *
	 * @param	client	The client associated with this <code>LoginRequest</code>
	 * @param	loginname	The name of the player to be logged in
	 * @param	password	The password to be used in the login attempt
	 * @param	getBreakfast	Whether or not the client should retrieve breakfast after login
	 */

	public LoginRequest( KoLmafia client, String username, String password )
	{
		this( client, username, password, true,
				StaticEntity.getProperty( "getBreakfast." + username ) != null &&
				StaticEntity.getProperty( "getBreakfast." + username ).equals( "true" ), false );
	}

	public LoginRequest( KoLmafia client, String username, String password, boolean savePassword, boolean getBreakfast, boolean isQuickLogin )
	{
		super( client, "login.php" );

		this.username = username.replaceFirst( "/[qQ]", "" );
		this.password = password;
		this.savePassword = savePassword;
		this.getBreakfast = getBreakfast;
		this.isQuickLogin = isQuickLogin;

		addFormField( "loggingin", "Yup." );
		addFormField( "loginname", this.username + "/q" );
		addFormField( "password", password );
	}

	/**
	 * Runs the <code>LoginRequest</code>.  This method determines
	 * whether or not the login was successful, and updates the
	 * display or notifies the client, as appropriate.
	 */

	public void run()
	{
		if ( instanceRunning )
			return;

		instanceRunning = true;
		lastUsername = username;
		lastPassword = password;

		KoLRequest.applySettings();
		if ( executeLogin() )
		{
			KoLmafia.forceContinue();
			while ( executeLogin() )
			{
				KoLmafia.forceContinue();
				isRollover |= responseCode != 200 && redirectLocation != null &&
					redirectLocation.indexOf( "maint.php" ) != -1;

				StaticEntity.executeCountdown( "Next login attempt in ", isRollover ? 3600 : 75 );
			}
		}

		isRollover = false;
		instanceRunning = false;
	}

	public static void executeTimeInRequest()
	{	executeTimeInRequest( false );
	}

	public static void executeTimeInRequest( boolean isRollover )
	{
		sessionID = null;
		LoginRequest.isRollover = isRollover;

		LoginRequest loginAttempt = new LoginRequest( StaticEntity.getClient(), lastUsername, lastPassword, false, false, true );
		loginAttempt.run();

		if ( sessionID != null )
			KoLmafia.updateDisplay( "Session timed-in." );

	}

	public static boolean isInstanceRunning()
	{	return instanceRunning;
	}

	public boolean executeLogin()
	{
		sessionID = null;
		KoLmafia.updateDisplay( "Sending login request..." );

		super.run();

		if ( responseCode == 302 && redirectLocation.equals( "maint.php" ) )
		{
			// Nightly maintenance, so KoLmafia should retry.
			// Therefore, return true.

			isRollover = true;
			return true;
		}
		else if ( responseCode == 302 && redirectLocation.startsWith( "main" ) )
		{
			if ( redirectLocation.equals( "main_c.html" ) )
				KoLRequest.isCompactMode = true;

			// If the login is successful, you notify the client
			// of success.  But first, if there was a desire to
			// save the password, do so here.

			if ( this.savePassword )
				client.addSaveState( username, password );

			sessionID = formConnection.getHeaderField( "Set-Cookie" );
			client.initialize( username, this.getBreakfast, this.isQuickLogin );

			return false;
		}
		else if ( responseText.indexOf( "Please wait a minute" ) != -1 )
		{
			// Ooh, logged in too fast.  KoLmafia should recognize this and
			// try again automatically in 75 seconds.

			return true;
		}
		else if ( responseText.indexOf( "login.php" ) != -1 )
		{
			// KoL sometimes switches servers while logging in. It returns a hidden form
			// with responseCode 200.

			// <html>
			//   <body>
			//     <form name=formredirect method=post action="http://www.kingdomofloathing.com/login.php">
			//       <input type=hidden name=loginname value="xxx">
			//       <input type=hidden name=loggingin value="Yup.">
			//       <input type=hidden name=password value="xxx">
			//     </form>
			//   </body>
			// </html>Redirecting to www.

			Matcher matcher = Pattern.compile( "http://(www\\d?\\.kingdomofloathing\\.com)/login\\.php" ).matcher( responseText );
			if ( matcher.find() )
			{
				redirectLocation = matcher.group();
				setLoginServer( matcher.group(1) );
				return true;
			}
		}

		// This means that the login failed.  Therefore, the user should
		// re-input their username and password.

		KoLmafia.updateDisplay( ERROR_STATE, "Login failed." );
		return false;
	}
}