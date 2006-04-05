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

/**
 * An extension of <code>KoLRequest</code> which handles logins.
 * A new instance is created and started for every login attempt,
 * and in the event that it is successful, the client provided
 * at construction time will be notified of the success.
 */

public class LoginRequest extends KoLRequest
{
	private String username;
	private String password;

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
		super( client, "login.php" );

		this.username = username;
		this.password = password;

		addFormField( "loggingin", "Yup." );
		addFormField( "loginname", username );
		addFormField( "password", password );
	}

	/**
	 * Runs the <code>LoginRequest</code>.  This method determines
	 * whether or not the login was successful, and updates the
	 * display or notifies the client, as appropriate.
	 */

	public void run()
	{
		DEFAULT_SHELL.updateDisplay( "Determining login server..." );
		KoLRequest.applySettings();

		DEFAULT_SHELL.updateDisplay( KoLRequest.getRootHostName() + " selected." );
		DEFAULT_SHELL.updateDisplay( "Sending login..." );

		super.run();

		if ( responseCode == 302 && redirectLocation.equals( "maint.php" ) )
		{
		}
		else if ( responseCode == 302 && redirectLocation.startsWith( "main" ) )
		{
			if ( redirectLocation.equals( "main_c.html" ) )
				KoLRequest.isCompactMode = true;

			// If the login is successful, you notify the client
			// of success.  But first, if there was a desire to
			// save the password, do so here.

			client.addSaveState( username.replaceFirst( "/q", "" ), password );
			client.initialize( username.replaceFirst( "/q", "" ), formConnection.getHeaderField( "Set-Cookie" ) );
			client.cachedLogin = client.getPasswordHash() == null ? null :
				new LoginRequest( client, username, password );
		}
		else
		{
			// This means that the login failed.  Therefore, the user should
			// re-input their username and password.

			DEFAULT_SHELL.updateDisplay( ERROR_STATE, "Login failed." );
		}
	}
}