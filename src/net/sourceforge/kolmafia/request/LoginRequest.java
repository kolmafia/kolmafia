package net.sourceforge.kolmafia.request;

import java.math.BigInteger;

import java.security.MessageDigest;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.StaticEntity;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.session.LoginManager;

import net.sourceforge.kolmafia.swingui.AnnouncementFrame;

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
	private static final Pattern ANNOUNCE_PATTERN =
		Pattern.compile( "Announcements:</b></td></tr><tr><td style=\"padding: 5px; border: 1px solid blue;\">" +
		                 "<center><table><tr><td><font size=2>(.*?)There are currently" );

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
	}

	@Override
	protected boolean retryOnTimeout()
	{
		return true;
	}

	@Override
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

		//if ( Preferences.getBoolean( "useSecureLogin" ) )
		if ( true )
		{
			return false;
		}

		KoLmafia.updateDisplay( "Validating login server (" + GenericRequest.KOL_HOST + ")..." );

		GenericRequest.reset();

		this.clearDataFields();

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

		if ( Preferences.getBoolean( "showAnnouncements" ) && !Preferences.getBoolean( "_announcementShown" ) )
		{
			Matcher announceMatcher = LoginRequest.ANNOUNCE_PATTERN.matcher( this.responseText );
			if ( announceMatcher.find() )
			{
				String announcement = announceMatcher.group( 1 );
				if ( announcement.contains( "<img" ) )
				{
					AnnouncementFrame.showRequest( announcement );
				}
			}
		}

		String challenge = challengeMatcher.group( 1 );
		String response = null;

		try
		{
			response = LoginRequest.digestPassword( password, challenge );
		}
		catch ( Exception e )
		{
			// An exception means bad things, so make sure to send the
			// original plaintext password.

			return false;
		}

		this.constructURLString( "login.php" );

		this.addFormField( "password", "" );
		this.addFormField( "challenge", challenge );
		this.addFormField( "response", response );
		this.addFormField( "secure", "1" );

		return true;
	}

	private static String digestPassword( final String password, final String challenge )
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

	private static String getHexString( final byte[] bytes )
	{
		byte[] nonNegativeBytes = new byte[ bytes.length + 1 ];
		System.arraycopy( bytes, 0, nonNegativeBytes, 1, bytes.length );

		StringBuilder hexString = new StringBuilder( 64 );

		hexString.append( "00000000000000000000000000000000" );
		hexString.append( new BigInteger( nonNegativeBytes ).toString( 16 ) );
		hexString.delete( 0, hexString.length() - 32 );

		return hexString.toString();
	}

	@Override
	public boolean shouldFollowRedirect()
	{
		return true;
	}

	/**
	 * Runs the <code>LoginRequest</code>. This method determines whether or not the login was successful, and
	 * updates the display or notifies the as appropriate.
	 */

	@Override
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

		if ( !this.detectChallenge() )
		{
			this.constructURLString( "login.php" );
			this.clearDataFields();

			this.addFormField( "password", this.password );
			this.addFormField( "secure", "0" );
		}

		this.addFormField( "loginname", Preferences.getBoolean( "stealthLogin" ) ? this.username + "/q" : this.username );
		this.addFormField( "loggingin", "Yup." );

		KoLmafia.updateDisplay( "Sending login request..." );

		super.run();

		if ( this.responseCode != 200 )
		{
			return;
		}

		LoginRequest.lastLoginAttempt = 0;

		if ( this.responseText.contains( "Bad password" ) )
		{
			KoLmafia.updateDisplay( MafiaState.ABORT, "Bad password." );
			return;
		}

		if ( this.responseText.contains( "wait fifteen minutes" ) )
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

		if ( this.responseText.contains( "wait a minute" ) ||
		     this.responseText.contains( "wait a couple of minutes" ) )
		{
			StaticEntity.executeCountdown( "Login reattempt in ", 75 );
			this.run();
			return;
		}

		if ( this.responseText.contains( "Too many" ) )
		{
			// Too many bad logins in too short a time span.
			int pos = this.responseText.indexOf("Too many");
			int pos2 = this.responseText.indexOf("<",pos+1);
			KoLmafia.updateDisplay( MafiaState.ABORT, this.responseText.substring(pos,pos2));
			return;
		}

		if ( this.responseText.contains( "do not have the privileges" ) )
		{
			// Can't use dev server without permission. Skip it.
			Preferences.setBoolean( "useDevProxyServer", false );
			this.run();
			return;
		}

		KoLmafia.updateDisplay( MafiaState.ABORT, "Encountered error in login." );
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
			KoLmafia.quit();
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

		request.setCookies();

		// It's possible that KoL will eventually make the redirect
		// the way it used to be, but enforce the redirect.  If this
		// happens, then validate here.

		LoginRequest.completedLogin = true;

		// If login is successful, notify client of success.

		String name = request.getFormField( "loginname" );

		if ( name == null )
		{
			return;
		}

		if ( name.endsWith( "/q" ) )
		{
			name = name.substring( 0, name.length() - 2 ).trim();
		}

		if ( LoginRequest.isTimingIn )
		{
			LoginManager.timein( name );
		}
		else
		{
			LoginManager.login( name );
		}
	}
}
