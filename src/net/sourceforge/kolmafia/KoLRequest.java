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

import java.net.URL;
import java.net.URLEncoder;
import java.net.URLConnection;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.InetAddress;
import java.net.UnknownHostException;

import java.io.PrintStream;
import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.FileNotFoundException;

import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Most aspects of Kingdom of Loathing are accomplished by submitting
 * forms and their accompanying data.  This abstract class is designed
 * to encapsulate this behavior by providing all descendant classes
 * (which simulate specific aspects of Kingdom of Loathing) with the
 * <code>addFormField()</code> method.  Note that the actual information
 * is sent to the server through the <code>run()</code> method.
 */

public class KoLRequest implements Runnable, KoLConstants
{
	protected static boolean isCompactMode = false;
	protected static boolean isServerFriendly = false;

	private static final String [][] SERVERS =
	{
		{ "www.kingdomofloathing.com", "67.18.115.2" },
		{ "www2.kingdomofloathing.com", "67.18.223.170" },
		{ "www3.kingdomofloathing.com", "67.18.223.194" }
	};

	private static String KOL_HOST = SERVERS[0][0];
	private static String KOL_ROOT = "http://" + SERVERS[0][1] + "/";

	private URL formURL;
	private StringBuffer formURLBuffer;
	private boolean followRedirects;
	private String formURLString;

	private String sessionID;
	private List data;

	protected KoLmafia client;
	protected int responseCode;
	protected String responseText;
	protected boolean isErrorState;
	protected String redirectLocation;
	protected HttpURLConnection formConnection;

	/**
	 * Static method called when <code>KoLRequest</code> is first
	 * instantiated or whenever the settings have changed.  This
	 * initializes the login server to the one stored in the user's
	 * settings, as well as initializes the user's proxy settings.
	 */

	public static void applySettings()
	{
		try
		{
			String proxySet = GLOBAL_SETTINGS.getProperty( "proxySet" );
			String proxyHost = GLOBAL_SETTINGS.getProperty( "http.proxyHost" );
			String proxyUser = GLOBAL_SETTINGS.getProperty( "http.proxyUser" );

			System.setProperty( "proxySet", proxySet );

			// Remove the proxy host from the system properties
			// if one isn't specified, or proxy setting is off.

			if ( proxySet.equals( "false" ) || proxyHost.equals( "" ) )
			{
				System.getProperties().remove( "http.proxyHost" );
				System.getProperties().remove( "http.proxyPort" );
			}
			else
			{
				try
				{	System.setProperty( "http.proxyHost", InetAddress.getByName( proxyHost ).getHostAddress() );
				}
				catch ( UnknownHostException e )
				{	System.setProperty( "http.proxyHost", proxyHost );
				}

				System.setProperty( "http.proxyPort", GLOBAL_SETTINGS.getProperty( "http.proxyPort" ) );
			}

			// Remove the proxy user from the system properties
			// if one isn't specified, or proxy setting is off.

			if ( proxySet.equals( "false" ) || proxyHost.equals( "" ) || proxyUser.equals( "" ) )
			{
				System.getProperties().remove( "http.proxyUser" );
				System.getProperties().remove( "http.proxyPassword" );
			}
			else
			{
				System.setProperty( "http.proxyUser", GLOBAL_SETTINGS.getProperty( "http.proxyUser" ) );
				System.setProperty( "http.proxyPassword", GLOBAL_SETTINGS.getProperty( "http.proxyPassword" ) );
			}

			// Determine the login server that will be used.  It
			// will either be auto-detection, or using the default.

			switch ( Integer.parseInt( GLOBAL_SETTINGS.getProperty( "loginServer" ) ) )
			{
				case 0:
					autoDetectServer();
					break;

				case 1:
					setLoginServer( SERVERS[0][0] );
					break;

				case 2:
					setLoginServer( SERVERS[1][0] );
					break;

				case 3:
					setLoginServer( SERVERS[2][0] );
					break;
			}
		}
		catch ( Exception e )
		{
			// An exception here means that the attempt to set up the proxy
			// server failed or the attempt to set the login server failed.
			// Because these result in default values, pretend nothing
			// happened and carry on with business.
		}
	}

	/**
	 * Static method used to auto detect the server to be used as
	 * the root for all requests by all KoLmafia clients running
	 * on the current JVM instance.
	 */

	private static void autoDetectServer()
	{
		try
		{
			// This test uses the Kingdom of Loathing automatic balancing
			// server again to make sure that it's okay with the current
			// login server.

			KoLRequest root = new KoLRequest( null, "index.php" );
			root.run();

			// Once the request is complete, because there was no header
			// indicating who redirected you there, you'll have a redirect
			// location pointing to the correct server.

			String location = root.formConnection.getHeaderField( "Location" );
			if ( location != null )
				setLoginServer( (new URL( location )).getHost() );
		}
		catch ( Exception e )
		{
			// This should never happen, but if it does, then the default
			// root should still be active.  Therefore, do nothing.
		}
	}

	/**
	 * Static method used to manually set the server to be used as
	 * the root for all requests by all KoLmafia clients running
	 * on the current JVM instance.
	 *
	 * @param	server	The hostname of the server to be used.
	 */

	private static void setLoginServer( String server )
	{
		for ( int i = 0; i < SERVERS.length; ++i )
			if ( SERVERS[i][0].equals( server ) )
			{
				KOL_HOST = SERVERS[i][0];
				KOL_ROOT = "http://" + SERVERS[i][1] + "/";
			}
	}

	/**
	 * Static method used to return the server currently used by
	 * this KoLmafia session.
	 *
	 * @return	The host name for the current server
	 */

	public static String getRootHostName()
	{	return KOL_HOST;
	}

	/**
	 * Constructs a new KoLRequest which will notify the given client
	 * of any changes and will use the given URL for data submission.
	 *
	 * @param	client	The client associated with this <code>KoLRequest</code>
	 * @param	formURLString	The form to be used in posting data
	 */

	protected KoLRequest( KoLmafia client, String formURLString )
	{	this( client, formURLString, false );
	}

	/**
	 * Constructs a new KoLRequest which will notify the given client
	 * of any changes and will use the given URL for data submission,
	 * possibly following redirects if the parameter so specifies.
	 *
	 * @param	client	The client associated with this <code>KoLRequest</code>
	 * @param	formURLString	The form to be used in posting data
	 * @param	followRedirects	<code>true</code> if redirects are to be followed
	 */

	protected KoLRequest( KoLmafia client, String formURLString, boolean followRedirects )
	{
		this.client = client;
		this.formURLString = formURLString;
		this.formURLBuffer = new StringBuffer( formURLString );

		this.data = new ArrayList();
		this.isErrorState = true;
		this.followRedirects = followRedirects;
	}

	/**
	 * Returns the location of the form being used for this URL, in case
	 * it's ever needed/forgotten.
	 */

	protected String getURLString()
	{	return formURLBuffer.toString();
	}

	/**
	 * Clears the data fields so that the descending class
	 * can have a fresh set of data fields.  This allows
	 * requests with variable numbers of parameters to be
	 * reused.
	 */

	protected void clearDataFields()
	{	this.data.clear();
	}

	/**
	 * Adds the given form field to the KoLRequest.  Descendant classes
	 * should use this method if they plan on submitting forms to Kingdom
	 * of Loathing before a call to the <code>super.run()</code> method.
	 * Ideally, these fields can be added at construction time.
	 *
	 * @param	name	The name of the field to be added
	 * @param	value	The value of the field to be added
	 * @param	allowDuplicates	true if duplicate names are OK
	 */

	protected void addFormField( String name, String value, boolean allowDuplicates )
	{
		String [] existingData = new String[ data.size() ];
		data.toArray( existingData );

		String encodedName = null;
		String encodedValue = null;

		try
		{
			encodedName = URLEncoder.encode( name, "UTF-8" ) + "=";
			encodedValue = URLEncoder.encode( value, "UTF-8" );
		}
		catch ( Exception e )
		{
			// In this case, you failed to encode the appropriate
			// name and value data.	 So, just print this to the
			// appropriate log stream and add in the unencoded
			// data (in case it's fine).

			KoLmafia.getLogStream().println( "Could not encode: " + name + "=" + value );
			data.add( name + "=" + value );
			return;
		}

		// Make sure that when you're adding data fields, you don't
		// submit duplicate fields.

		if ( !allowDuplicates )
		{
			for ( int i = 0; i < existingData.length; ++i )
			{
				if ( existingData[i].startsWith( encodedName ) )
				{
					data.set( i, encodedName + encodedValue );
					return;
				}
			}
		}

		// If the data did not already exist, then
		// add it to the end of the array.

		data.add( encodedName + encodedValue );
	}

	protected void addFormField( String name, String value )
	{	addFormField( name, value, false );
	}

	/**
	 * Adds the given form field to the KoLRequest.
	 * @param	element	The field to be added
	 */

	protected void addFormField( String element )
	{	data.add( element );
	}

	protected String getDataString()
	{
		StringBuffer dataBuffer = new StringBuffer();
		Iterator iterator = data.iterator();

		if ( iterator.hasNext() )
			dataBuffer.append( iterator.next().toString() );

		while ( iterator.hasNext() )
		{
			dataBuffer.append( '&' );
			dataBuffer.append( iterator.next().toString() );
		}

		return dataBuffer.toString();
	}

	/**
	 * Set up this request before a series of run() calls.	Most classes
	 * that extend KoLRequest won't need to provide this method, but if
	 * there is anything which must be done once only at the beginning,
	 * this is the place.
	 */

	public void startRun()
	{
	}

	/**
	 * Runs the thread, which prepares the connection for output, posts the data
	 * to the Kingdom of Loathing, and prepares the input for reading.  Because
	 * the Kingdom of Loathing has identical page layouts, all page reading and
	 * handling will occur through these method calls.
	 */

	public void run()
	{
		// Returning to thread synchronization.  Because requests
		// are forced to occur in separate threads, and the only
		// loop is the time-in loop, this should not cause a deadlock.

		if ( !isDelayExempt() )
		{
			synchronized ( KoLRequest.class )
			{
				KoLRequest.isServerFriendly = getProperty( "serverFriendly" ).equals( "true" );
			}
		}

		// You are allowed a maximum of four attempts
		// to run the request.  This prevents KoLmafia
		// from spamming the servers.

		do
		{
			this.isErrorState = false;

			// Only add in a delay when you're out of login.
			// If you're still doing the login process, ignore
			// the delay to avoid people switching the option
			// off just to avoid login slowdown.

			if ( !isDelayExempt() )
			{
				if ( isServerFriendly )
					KoLRequest.delay();
				else
					KoLRequest.delay( 500 );
			}
		}
		while ( !prepareConnection() || !postClientData() || (retrieveServerReply() && this.isErrorState) );

		// Add the ability to set the current request so that KoLmafia
		// can make use of it, if viewing intermediate results is allowed.

		if ( !(this instanceof ChatRequest) )
			client.setCurrentRequest( this );

		// If the user wants to show all the requests in the browser, then
		// make sure it's updated.

		if ( !isErrorState && getProperty( "synchronizeFightFrame" ).equals( "true" ) &&
			(this instanceof AdventureRequest || this instanceof FightRequest) && this.responseCode == 200 )
			showInBrowser();
	}

	/**
	 * Utility method which waits for the default refresh rate
	 * without using Thread.sleep() - this means CPU usage can
	 * be greatly reduced.  This will always use the server
	 * friendly delay speed.
	 */

	protected static void delay()
	{	delay( 4000 );
	}

	/**
	 * Utility method which waits for the given duration without
	 * using Thread.sleep() - this means CPU usage can be greatly
	 * reduced.
	 */

	protected static void delay( long milliseconds )
	{
		if ( milliseconds == 0 )
			return;

		Object waitObject = new Object();
		try
		{
			synchronized ( waitObject )
			{
				waitObject.wait( milliseconds );
				waitObject.notifyAll();
			}
		}
		catch ( InterruptedException e )
		{
		}
	}

	private boolean isDelayExempt()
	{
		return client == null || client.inLoginState() || getClass() == KoLRequest.class ||
			this instanceof LoginRequest || this instanceof ChatRequest || this instanceof CharpaneRequest;
	}

	/**
	 * Utility method used to prepare the connection for input and output
	 * (if output is necessary).  The method attempts to open the connection,
	 * and then apply the needed settings.
	 *
	 * @return	<code>true</code> if the connection was successfully prepared
	 */

	private boolean prepareConnection()
	{
		KoLmafia.getLogStream().println( "Connecting to " + formURLString + "..." );

		if ( client != null )
			this.sessionID = client.getSessionID();

		// With that taken care of, determine the actual URL that you
		// are about to request.

		try
		{
			this.formURL = new URL( KOL_ROOT + formURLString );
		}
		catch ( MalformedURLException e )
		{
			this.isErrorState = true;
			updateDisplay( ERROR_STATE, "Error in URL: " + KOL_ROOT + formURLString );

			KoLRequest.delay();
			return false;
		}

		try
		{
			// For now, because there isn't HTTPS support, just open the
			// connection and directly cast it into an HttpURLConnection

			KoLmafia.getLogStream().println( "Attempting to establish connection..." );
			formConnection = (HttpURLConnection) formURL.openConnection();
		}
		catch ( Exception e )
		{
			// In the event that an Exception is thrown, one can assume
			// that there was a timeout; return false and let the loop
			// attempt to connect again

			if ( formURLString.indexOf( "chat" ) == -1 && ( client == null || !BuffBotHome.isBuffBotActive() ) )
				KoLmafia.getLogStream().println( "Error opening connection.  Retrying..." );

			KoLRequest.delay();
			return false;
		}

		KoLmafia.getLogStream().println( "Connection established." );

		formConnection.setDoInput( true );
		formConnection.setDoOutput( !data.isEmpty() );
		formConnection.setUseCaches( false );
		formConnection.setInstanceFollowRedirects( false );

		formConnection.setRequestProperty( "Content-Type",
			"application/x-www-form-urlencoded" );

		if ( sessionID != null )
			formConnection.addRequestProperty( "Cookie", sessionID );

		return true;
	}

	/**
	 * Utility method used to post the client's data to the Kingdom of
	 * Loathing server.  The method grabs all form fields added so far
	 * and posts them using the traditional ampersand style of HTTP
	 * requests.
	 *
	 * @return	<code>true</code> if all data was successfully posted
	 */

	private boolean postClientData()
	{
		// Only attempt to post something if there's actually
		// data to post - otherwise, opening an input stream
		// should be enough

		if ( data.isEmpty() )
			return true;

		KoLmafia.getLogStream().println( "Posting form data..." );

		try
		{
			String dataString = getDataString();

			formURLBuffer.setLength(0);
			formURLBuffer.append( formURLString );
			formURLBuffer.append( "?" );
			formURLBuffer.append( dataString );

			if ( client != null && !formURLString.equals( "login.php" ) )
			{
				if ( client.getPasswordHash() == null )
					KoLmafia.getLogStream().println( dataString );
				else
					KoLmafia.getLogStream().println( dataString.replaceAll( client.getPasswordHash(), "" ) );
			}

			formConnection.setRequestMethod( "POST" );
			BufferedWriter ostream = new BufferedWriter( new OutputStreamWriter( formConnection.getOutputStream() ) );

			ostream.write( dataString );
			ostream.flush();
			ostream.close();
			ostream = null;

			KoLmafia.getLogStream().println( "Posting data posted." );
			return true;
		}
		catch ( Exception e )
		{
			if ( formURLString.indexOf( "chat" ) == -1 && ( client == null || !BuffBotHome.isBuffBotActive() ) )
				KoLmafia.getLogStream().println( "Connection timed out during post.  Retrying..." );

			if ( client != null )
			{
				KoLmafia.getLogStream().println( e );
				e.printStackTrace( KoLmafia.getLogStream() );
			}

			KoLRequest.delay();
			return false;
		}
	}

	/**
	 * Utility method used to retrieve the server's reply.  This method
	 * detects the nature of the reply via the response code provided
	 * by the server, and also detects the unusual states of server
	 * maintenance and session timeout.  All data retrieved by this
	 * method is stored in the instance variables for this class.
	 *
	 * @return	<code>true</code> if the data was successfully retrieved, or retrying is permissible
	 */

	private boolean retrieveServerReply()
	{
		BufferedReader istream;
		boolean shouldContinue = true;
		this.isErrorState = false;

		try
		{
			// In the event of redirects, the appropriate flags should be set
			// indicating whether or not the direct is a normal redirect (ie:
			// one that results in something happening), or an error-type one
			// (ie: maintenance).

			if ( client != null )
				KoLmafia.getLogStream().println( "Retrieving server reply..." );

			responseText = "";
			redirectLocation = "";

			// Store any cookies that might be found in the headers of the
			// reply - there really is only one cookie to worry about, so
			// it will be stored here.

			istream = new BufferedReader( new InputStreamReader( formConnection.getInputStream() ) );
			responseCode = formConnection.getResponseCode();
		}
		catch ( Exception e )
		{
			// Now that the proxy problem has been resolved, FNFEs
			// should only happen if the file does not exist.  In
			// this case, stop retrying.

			this.isErrorState = true;

			if ( e instanceof FileNotFoundException )
			{
				updateDisplay( ERROR_STATE, "Page <" + formURLString + "> not found." );

				if ( client != null )
				{
					KoLmafia.getLogStream().println( e );
					e.printStackTrace( KoLmafia.getLogStream() );
				}

				KoLRequest.delay();
				return false;
			}

			KoLmafia.getLogStream().println( "Connection timed out during response.  Retrying..." );

			if ( client != null )
			{
				KoLmafia.getLogStream().println( e );
				e.printStackTrace( KoLmafia.getLogStream() );
			}

			// Add in an extra delay in the event of a time-out in order
			// to be nicer on the KoL servers.

			KoLRequest.delay();
			return true;
		}

		if ( client != null )
		{
			KoLmafia.getLogStream().println( "Server response code: " + responseCode );

			if ( responseCode >= 300 && responseCode <= 399 )
			{
				// Redirect codes are all the ones that occur between
				// 300 and 399.  All these notify the user of a location
				// to return to; deal with the ones which are errors.

				redirectLocation = formConnection.getHeaderField( "Location" );

				if ( redirectLocation.equals( "maint.php" ) )
				{
					// If the system is down for maintenance, the user must be
					// notified that they should try again later.

					updateDisplay( ERROR_STATE, "Nightly maintenance." );
					this.isErrorState = true;

					if ( !(this instanceof LoginRequest) && client.getSettings().getProperty( "forceReconnect" ).equals( "true" ) )
						client.executeTimeInRequest();
					else
					{
						client.cancelRequest();
						shouldContinue = false;
					}
				}
				else if ( redirectLocation.startsWith( "login.php" ) )
				{
					updateDisplay( ERROR_STATE, "Session timed out." );
					client.cancelRequest();
					this.isErrorState = true;

					if ( !formURLString.equals( "login.php" ) && client.getSettings().getProperty( "forceReconnect" ).equals( "true" ) )
					{
						client.executeTimeInRequest();
						client.resetContinueState();
					}
					else
					{
						client.cancelRequest();
						shouldContinue = false;
					}
				}
				else if ( followRedirects )
				{
					// Re-setup this request to follow the redirect
					// desired and rerun the request.

					this.formURLString = redirectLocation;

					this.formURLBuffer.setLength(0);
					this.formURLBuffer.append( this.formURLString );

					this.data.clear();

					this.isErrorState = true;
					this.followRedirects = followRedirects;
				}
				else if ( redirectLocation.equals( "fight.php" ) )
				{
					// You have been redirected to a fight!  Here, you need
					// to complete the fight before you can continue.

					FightRequest battle = new FightRequest( client );
					battle.run();

					this.isErrorState = !(this instanceof AdventureRequest) || battle.isErrorState;
				}
				else if ( redirectLocation.equals( "choice.php" ) )
				{
					shouldContinue = processChoiceAdventure();
				}
				else
				{
					this.isErrorState = false;
					shouldContinue = false;
					KoLmafia.getLogStream().println( "Redirected: " + redirectLocation );
				}
			}
			else if ( responseCode == 200 )
			{
				String line = null;
				this.isErrorState = false;
				StringBuffer replyBuffer = new StringBuffer();

				try
				{
					line = istream.readLine();

					// There's a chance that there was no content in the reply
					// (header-only reply) - if that's the case, the line will
					// be null and you've hit an error state.

					if ( line == null )
					{
						this.isErrorState = true;
						KoLmafia.getLogStream().println( "No reply content.  Retrying..." );
					}
					else
					{
						// Check for MySQL errors, since those have been getting more
						// frequent, and would cause an I/O Exception to be thrown
						// unnecessarily, when a re-request would do.  I'm not sure
						// how they work right now (which line the MySQL error is
						// printed to), but for now, assume that it's the first line.

						if ( line.indexOf( "error" ) != -1 )
						{
							this.isErrorState = true;
							KoLmafia.getLogStream().println( "Encountered MySQL error.  Retrying..." );
						}
						else
						{
							KoLmafia.getLogStream().println( "Reading page content..." );

							// The remaining lines form the rest of the content.  In order
							// to make it easier for string parsing, the line breaks will
							// ultimately be preserved.

							while ( line != null )
							{
								replyBuffer.append( line );
								line = istream.readLine();
							}
						}
					}
				}
				catch ( Exception e )
				{
					// An Exception is clearly an error; here it will be reported
					// to the client, but another attempt will be made

					this.isErrorState = true;
					if ( formURLString.indexOf( "chat" ) == -1 && ( client == null || !BuffBotHome.isBuffBotActive() ) )
						KoLmafia.getLogStream().println( "Error reading server reply.  Retrying..." );

					if ( client != null )
					{
						KoLmafia.getLogStream().println( e );
						e.printStackTrace( KoLmafia.getLogStream() );
					}
				}

				responseText = replyBuffer.toString().replaceAll( "<script.*?</script>", "" );

				if ( client != null )
				{
					if ( client.getPasswordHash() == null )
						KoLmafia.getLogStream().println( responseText );
					else
						KoLmafia.getLogStream().println( responseText.replaceAll( client.getPasswordHash(), "" ) );
				}
			}
		}

		try
		{
			istream.close();
		}
		catch ( Exception e )
		{
			// Errors equate to an input stream that
			// has already been closed.
		}

		// Null the pointer to help the garbage collector
		// identify this as discardable data and return
		// from the function call.

		istream = null;
		return shouldContinue;
	}

	/**
	 * Utility method used to process the results of any adventure
	 * in the Kingdom of Loathing.  This method searches for items,
	 * stat gains, and losses within the provided string.
	 *
	 * @param	results	The string containing the results of the adventure
	 */

	protected final void processResults( String results )
	{	client.processResults( results );
	}

	/**
	 * Utility method used to skip the given number of tokens within
	 * the provided <code>StringTokenizer</code>.  This method is used
	 * in order to clarify what's being done, rather than calling
	 * <code>st.nextToken()</code> repeatedly.
	 *
	 * @param	st	The <code>StringTokenizer</code> whose tokens are to be skipped
	 * @param	tokenCount	The number of tokens to skip
	 */

	protected static final void skipTokens( StringTokenizer st, int tokenCount )
	{
		for ( int i = 0; i < tokenCount; ++i )
			st.nextToken();
	}

	/**
	 * Utility method used to transform the next token on the given
	 * <code>StringTokenizer</code> into an integer.  Because this
	 * is used repeatedly in parsing, its functionality is provided
	 * globally to all instances of <code>KoLRequest</code>.
	 *
	 * @param	st	The <code>StringTokenizer</code> whose next token is to be retrieved
	 * @return	The integer token, if it exists, or 0, if the token was not a number
	 */

	protected static final int intToken( StringTokenizer st )
	{	return intToken( st, 0 );
	}

	/**
	 * Utility method used to transform the next token on the given
	 * <code>StringTokenizer</code> into an integer; however, this
	 * differs in the single-argument version in that only a part
	 * of the next token is needed.  Because this is also used
	 * repeatedly in parsing, its functionality is provided globally
	 * to all instances of <code>KoLRequest</code>.
	 *
	 * @param	st	The <code>StringTokenizer</code> whose next token is to be retrieved
	 * @param	fromStart	The index at which the integer to parse begins
	 * @return	The integer token, if it exists, or 0, if the token was not a number
	 */

	protected static final int intToken( StringTokenizer st, int fromStart )
	{
		try
		{
			String token = st.nextToken().substring( fromStart );
			return (token.indexOf(",") == -1) ? Integer.parseInt( token ) : df.parse( token ).intValue();
		}
		catch ( Exception e )
		{	return 0;
		}
	}

	/**
	 * Updates the display.
	 */

	protected final void updateDisplay( int displayState, String message )
	{
		if ( client != null )
			client.updateDisplay( displayState, message );
		else
			KoLmafia.getLogStream().println( message );
	}

	/**
	 * Utility method used to transform part of the next token on the
	 * given <code>StringTokenizer</code> into an integer.  This differs
	 * from the two-argument in that part of the end of the string is
	 * expected to contain non-numeric values as well.  Because this is
	 * also repeatedly in parsing, its functionality is provided globally
	 * to all instances of <code>KoLRequest</code>.
	 *
	 * @param	st	The <code>StringTokenizer</code> whose next token is to be retrieved
	 * @param	fromStart	The index at which the integer to parse begins
	 * @param	fromEnd	The distance from the end at which the first non-numeric character is found
	 * @return	The integer token, if it exists, or 0, if the token was not a number
	 */

	protected static final int intToken( StringTokenizer st, int fromStart, int fromEnd )
	{
		try
		{
			String token = st.nextToken();
			token = token.substring( fromStart, token.length() - fromEnd );
			return (token.indexOf(",") == -1) ? Integer.parseInt( token ) : df.parse( token ).intValue();
		}
		catch ( Exception e )
		{	return 0;
		}
	}

	/**
	 * An alternative method to doing adventure calculation is determining
	 * how many adventures are used by the given request, and subtract
	 * them after the request is done.  This number defaults to <code>zero</code>;
	 * overriding classes should change this value to the appropriate
	 * amount.
	 *
	 * @return	The number of adventures used by this request.
	 */

	public int getAdventuresUsed()
	{	return 0;
	}

	protected final void setProperty( String name, String value )
	{	StaticEntity.setProperty( name, value );
	}

	protected final String getProperty( String name )
	{	return StaticEntity.getProperty( name );
	}

	/**
	 * Utility method which notifies the client that it needs to process
	 * the given choice adventure.
	 */

	public boolean processChoiceAdventure()
	{
		// You can no longer simply ignore a choice adventure.	One of
		// the options may have that effect, but we must at least run
		// choice.php to find out which choice it is.

		KoLRequest request = new KoLRequest( client, "choice.php" );
		request.run();

		request.showInBrowser();
		return handleChoiceResponse( request );
	}

	/**
	 * Utility method to handle the response for a specific choice
	 * adventure.
	 */

	private boolean handleChoiceResponse( KoLRequest request )
	{
		String text = request.responseText;
		Matcher encounterMatcher = Pattern.compile( "<b>(.*?)</b>" ).matcher( text );
		if ( encounterMatcher.find() )
			client.registerEncounter( encounterMatcher.group(1) );

		Matcher choiceMatcher = Pattern.compile( "whichchoice value=(\\d+)" ).matcher( text );
		if ( !choiceMatcher.find() )
		{
			// choice.php did not offer us any choices. This would
			// be a bug in KoL itself. Bail now and let the user
			// finish by hand.

			updateDisplay( ERROR_STATE, "Encountered choice adventure with no choices." );
			isErrorState = true;
			client.cancelRequest();
			return false;
		}

		String choice = choiceMatcher.group(1);
		String option = "choiceAdventure" + choice;
		String decision = getProperty( option );

		// If there is currently no setting which determines the
		// decision, give an error and bail.

		if ( decision == null )
		{
			updateDisplay( ERROR_STATE, "Unsupported choice adventure #" + choice );
			isErrorState = true;
			client.cancelRequest();
			return false;
		}

		// If the user wants to ignore this specific choice or all
		// choices, see if this choice is ignorable.

		if ( decision.equals( "0" ) || getProperty( "ignoreChoiceAdventures" ).equals( "true" ) )
		{
			String ignoreChoice = AdventureDatabase.ignoreChoiceOption( option );
			if ( ignoreChoice != null )
				decision = ignoreChoice;
		}

		// Make sure that we've resolved to a non-0 choice.

		if ( decision.equals( "0" ) )
		{
			updateDisplay( ERROR_STATE, "Can't ignore choice adventure #" + choice );
			isErrorState = true;
			client.cancelRequest();
			return false;
		}

		// If there is currently a setting which determines the
		// decision, make that decision and submit the form.

		request = new KoLRequest( client, "choice.php" );
		request.addFormField( "pwd", client.getPasswordHash() );
		request.addFormField( "whichchoice", choice );
		request.addFormField( "option", decision );

		request.run();
		request.showInBrowser();

		// Handle any items or stat gains resulting from the adventure

		client.processResults( request.responseText );

		// Manually process any adventure usage for choice adventures,
		// since they necessarily consume an adventure.

		if ( AdventureDatabase.consumesAdventure( option, decision ) )
			client.processResult( new AdventureResult( AdventureResult.ADV, -1 ) );

		// Certain choices cost meat when selected

		int meat = AdventureDatabase.consumesMeat( option, decision );
		if ( meat > 0 )
			client.processResult( new AdventureResult( AdventureResult.MEAT, 0 - meat ) );

		AdventureResult loseAdventure = new AdventureResult( AdventureResult.CHOICE, -1 );

		if ( loseAdventure.getCount( client.getConditions() ) > 0 )
		{
			AdventureResult.addResultToList( client.getConditions(), loseAdventure );
			if ( loseAdventure.getCount( client.getConditions() ) == 0 )
				client.getConditions().remove( client.getConditions().indexOf( loseAdventure ) );
		}

		// Choice adventures can lead to other choice adventures
		// without a redirect. Detect this and recurse, as needed.

		if ( request.responseText.indexOf( "action=choice.php" ) != -1 )
			return handleChoiceResponse( request );

		return true;
	}

	protected void showInBrowser()
	{
		// Check to see if this request should be showed
		// in a browser.  If you're using a command-line
		// interface, then you should not display the request.

		if ( client instanceof KoLmafiaCLI )
			return;

		if ( getProperty( "finishInBrowser" ).equals( "true" ) || getProperty( "synchronizeFightFrame" ).equals( "true" ) )
			FightFrame.showRequest( this );
	}
}
