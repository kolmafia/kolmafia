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

import java.math.BigInteger;
import java.security.MessageDigest;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;

/**
 * An extension of <code>KoLRequest</code> which handles logins.
 * A new instance is created and started for every login attempt,
 * and in the event that it is successful, will attempt to spawn
 * another thread which will retrieve the password hash.
 */

public class LoginRequest extends KoLRequest
{
	private String loginname;
	private String password;

	public LoginRequest( KoLmafia client, String loginname, String password )
	{
		super( client, "login.php" );

		this.loginname = loginname;
		this.password = password;
		client.setSessionID( null );

		addFormField( "loggingin", "Yup." );
		addFormField( "loginname", loginname );
		addFormField( "password", password );
	}

	public void run()
	{
		super.run();

		if ( responseCode == 302 && !isErrorState )
		{
			// If the login is successful, you set the password hash so that
			// it can be reused by other parts of the KoLmafia session

			setPasswordHash();
		}
		else if ( !isErrorState && replyContent.indexOf( "Login failed." ) == -1 )
		{
			// In this case, it's just a timeout of an old session, so you
			// can go ahead and try again after waiting for about ten seconds
			// for the old session ID to clear.

			frame.updateDisplay( LoginFrame.PRE_LOGIN_STATE, "Deactivating old session..." );

			try
			{
				this.sleep( 10000 );
				frame.updateDisplay( LoginFrame.SENDING_LOGIN_STATE, "Sending login..." );
				(new LoginRequest( client, loginname, password )).start();
			}
			catch ( InterruptedException e )
			{
			}
		}
		else if ( !isErrorState )
		{
			// This means that the login failed.  Therefore, the user should
			// re-input their username and password.

			frame.updateDisplay( LoginFrame.PRE_LOGIN_STATE, "Login failed." );
		}
	}

	private void setPasswordHash()
	{
		// When the login is successful, you temporarily update the
		// login card to reflect this fact and calculate the appropriate
		// password hash to send in for other activities

		frame.updateDisplay( LoginFrame.LOGGED_IN_STATE, "Calculating password hash..." );

		client.setLoginName( loginname );
		client.setPassword( password );
		client.setSessionID( formConnection.getHeaderField( "Set-Cookie" ) );

		// The password hash is an MD5 digest of the actual value, as
		// evidenced in KwiKoL's documentation; therefore, compute the
		// MD5 sum rather than going to a different page to find it.

		try
		{
			MessageDigest md5 = MessageDigest.getInstance( "MD5" );
			md5.update( client.getPassword().getBytes( "ISO-8859-1" ) );
			client.setPasswordHash( new BigInteger( md5.digest() ).toString( 16 ) );
		}
		catch ( NoSuchAlgorithmException e1 )
		{
		}
		catch ( UnsupportedEncodingException e2 )
		{
		}

		// Update the client to show that the password hash calculation
		// is complete and that the client is now formally initialized

		frame.updateDisplay( LoginFrame.LOGGED_IN_STATE, "Hash calculation complete." );
		client.initialize();
	}
}