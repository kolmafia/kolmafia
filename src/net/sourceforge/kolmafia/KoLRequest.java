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
	protected static final int REFRESH_RATE = 500;
	protected static boolean isCompactMode = false;

	private static final String [] HOSTNAMES = {
		"www.kingdomofloathing.com", "www2.kingdomofloathing.com", "www3.kingdomofloathing.com" };

	private static final String [] DNS_NAMES = {
		"67.18.115.2", "67.18.223.170", "67.18.223.194" };

	private static String KOL_HOST = HOSTNAMES[0];
	private static String KOL_ROOT = "http://" + DNS_NAMES[0] + "/";

	static
	{	applySettings();
	}

	private URL formURL;
	private StringBuffer formURLBuffer;

	private String formURLString;
	private String sessionID;
	private List data;

	protected KoLmafia client;
	protected PrintStream logStream;

	private boolean hadTimeout;
	protected int responseCode;
	protected boolean isErrorState;
	protected boolean followRedirects;
	protected String redirectLocation;

	protected String responseText;
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
			KoLSettings currentSettings = new KoLSettings();

			String proxySet = currentSettings.getProperty( "proxySet" );
			String proxyHost = currentSettings.getProperty( "http.proxyHost" );
			String proxyUser = currentSettings.getProperty( "http.proxyUser" );

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

				System.setProperty( "http.proxyPort", currentSettings.getProperty( "http.proxyPort" ) );
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
				System.setProperty( "http.proxyUser", currentSettings.getProperty( "http.proxyUser" ) );
				System.setProperty( "http.proxyPassword", currentSettings.getProperty( "http.proxyPassword" ) );
			}

			// Determine the login server that will be used.  It
			// will either be auto-detection, or using the default.

			switch ( Integer.parseInt( currentSettings.getProperty( "loginServer" ) ) )
			{
				case 0:
					autoDetectServer();
					break;

				case 1:
					setLoginServer( HOSTNAMES[0] );
					break;

				case 2:
					setLoginServer( HOSTNAMES[1] );
					break;

				case 3:
					setLoginServer( HOSTNAMES[2] );
					break;
			}
		}
		catch ( Exception e )
		{
			// An exception here means that the attempt to set up the proxy
			// server failed or the attempt to set the login server failed.
			// Because these result in default values,, pretend nothing
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

			KoLRequest root = new KoLRequest( null, "login.php" );
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
		for ( int i = 0; i < HOSTNAMES.length; ++i )
			if ( HOSTNAMES[i].equals( server ) )
			{
				KOL_HOST = HOSTNAMES[i];
				KOL_ROOT = "http://" + DNS_NAMES[i] + "/";
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

		this.logStream = new NullStream();
		data = new ArrayList();

		this.isErrorState = true;
		this.followRedirects = followRedirects;
	}

	/**
	 * Returns the location of the form being used for this URL, in case
	 * it's ever needed/forgotten.
	 */

	protected String getURLString()
	{	return formURLString;
	}

	/**
	 * Adds the given form field to the KoLRequest.  Descendant classes should
	 * use this method if they plan on submitting forms to Kingdom of Loathing
	 * before a call to the <code>super.run()</code> method.  Ideally, these
	 * fields can be added at construction time.
	 *
	 * @param	name	The name of the field to be added
	 * @param	value	The value of the field to be added
	 */

	protected void addFormField( String name, String value )
	{
		try
		{
			data.add( URLEncoder.encode( name, "UTF-8" ) + "=" + URLEncoder.encode( value, "UTF-8" ) );
		}
		catch ( Exception e )
		{
			// In this case, you failed to encode the appropriate
			// name and value data.  So, just print this to the
			// appropriate log stream and add in the unencoded
			// data (in case it's fine).

			logStream.println( "Could not encode: " + name + "=" + value );
		}
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
	 * Runs the thread, which prepares the connection for output, posts the data
	 * to the Kingdom of Loathing, and prepares the input for reading.  Because
	 * the Kingdom of Loathing has identical page layouts, all page reading and
	 * handling will occur through these method calls.
	 */

	public void run()
	{
		if ( this instanceof ChatRequest )
		{
			// Chat requests can run without problems concurrently
			// with other requests.  Therefore, there is no thread
			// synchronization done on chat requests.

			execute();
		}
		else
		{
			// However, all other requests cannot run concurrently
			// with other requests without problems potentially
			// arising.  Therefore, synchronize on something that
			// is static before running the request.

			synchronized ( KoLmafia.class )
			{
				execute();
			}
		}
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

	/**
	 * Utility method used to retry requests - this allows the KoLRequest
	 * itself to rerun without calling the run method that instantiated
	 * this request.
	 */

	private void execute()
	{
		this.hadTimeout = false;
		this.logStream = client == null || formURLString.indexOf( "chat" ) != -1 ? new NullStream() : client.getLogStream();
		logStream.println( "Connecting to " + formURLString + "..." );

		do
		{
			this.isErrorState = false;
			delay( REFRESH_RATE );
		}
		while ( !prepareConnection() || !postClientData() || (retrieveServerReply() && this.isErrorState) );

		// In the event that you have a timeout during a situation
		// where the display does not update afterwards, be sure
		// you clear the display.

		if ( this.hadTimeout && (followRedirects || formURLString.startsWith( "chat" ) || client.isBuffBotActive()) )
			client.updateDisplay( NOCHANGE, "Retry successful." );
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
		if ( client != null )
			this.sessionID = client.getSessionID();

		// With that taken care of, determine the actual URL that you
		// are about to request.

		try
		{
			this.formURL = new URL( KOL_ROOT + formURLBuffer.toString() );
		}
		catch ( MalformedURLException e )
		{
			this.isErrorState = true;
			updateDisplay( ERROR_STATE, "Error in URL: " + KOL_ROOT + formURLBuffer.toString() );

			delay( REFRESH_RATE );
			return false;
		}

		try
		{
			// For now, because there isn't HTTPS support, just open the
			// connection and directly cast it into an HttpURLConnection

			logStream.println( "Attempting to establish connection..." );
			formConnection = (HttpURLConnection) formURL.openConnection();
		}
		catch ( Exception e )
		{
			this.hadTimeout = true;

			// In the event that an Exception is thrown, one can assume
			// that there was a timeout; return false and let the loop
			// attempt to connect again

			if ( formURLString.indexOf( "chat" ) == -1 && ( client == null || !client.isBuffBotActive() ) )
				updateDisplay( NOCHANGE, "Error opening connection.  Retrying..." );

			delay( REFRESH_RATE );
			return false;
		}

		logStream.println( "Connection established." );

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

		logStream.println( "Posting form data..." );

		try
		{
			String dataString = getDataString();

			if ( client != null && !formURLString.equals( "login.php" ) )
			{
				if ( client.getPasswordHash() == null )
					logStream.println( dataString );
				else
					logStream.println( dataString.replaceAll( client.getPasswordHash(), "" ) );
			}

			formConnection.setRequestMethod( "POST" );
			BufferedWriter ostream = new BufferedWriter( new OutputStreamWriter( formConnection.getOutputStream() ) );

			ostream.write( dataString );
			ostream.flush();
			ostream.close();
			ostream = null;

			logStream.println( "Posting data posted." );
			return true;
		}
		catch ( Exception e )
		{
			this.hadTimeout = true;

			if ( formURLString.indexOf( "chat" ) == -1 && ( client == null || !client.isBuffBotActive() ) )
				updateDisplay( NOCHANGE, "Connection timed out.  Retrying..." );

			if ( client != null )
			{
				logStream.println( e );
				e.printStackTrace( logStream );
			}

			delay( REFRESH_RATE );
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
		this.isErrorState = true;

		try
		{
			// In the event of redirects, the appropriate flags should be set
			// indicating whether or not the direct is a normal redirect (ie:
			// one that results in something happening), or an error-type one
			// (ie: maintenance).

			logStream.println( "Retrieving server reply..." );

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
			this.hadTimeout = true;

			// Now that the proxy problem has been resolved, FNFEs
			// should only happen if the file does not exist.  In
			// this case, stop retrying.

			if ( e instanceof FileNotFoundException )
			{
				updateDisplay( ERROR_STATE, "Page <" + formURLString + "> not found." );

				if ( client != null )
				{
					logStream.println( e );
					e.printStackTrace( logStream );
				}

				delay( REFRESH_RATE );
				return false;
			}

			if ( formURLString.indexOf( "chat" ) == -1 && ( client == null || !client.isBuffBotActive() ) )
				updateDisplay( NOCHANGE, "Connection timed out.  Retrying..." );

			if ( client != null )
			{
				logStream.println( e );
				e.printStackTrace( logStream );
			}

			// Add in an extra delay in the event of a time-out in order
			// to be nicer on the KoL servers.

			delay( REFRESH_RATE );
			return true;
		}

		if ( client != null )
		{
			logStream.println( "Server response code: " + responseCode );

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
					isErrorState = true;

					if ( client.getSettings().getProperty( "forceReconnect" ).equals( "true" ) )
					{
						client.executeTimeInRequest();
						return true;
					}

					client.cancelRequest();
					return false;
				}
				else if ( redirectLocation.startsWith( "login.php" ) )
				{
					updateDisplay( ERROR_STATE, "Session timed out." );
					client.cancelRequest();
					isErrorState = true;

					if ( !formURLString.equals( "login.php" ) && client.getSettings().getProperty( "forceReconnect" ).equals( "true" ) )
					{
						client.executeTimeInRequest();
						return true;
					}

					client.cancelRequest();
					return false;
				}
				else if ( followRedirects )
				{
					// You're no longer caught with error code!  However,
					// this is set to follow redirects automatically.
					// Therefore, re-setup this request to follow the
					// redirect desired and rerun the request.

					this.formURLString = redirectLocation;
					this.formURLBuffer = new StringBuffer( this.formURLString );

					this.data.clear();

					this.isErrorState = true;
					this.followRedirects = followRedirects;

					return true;
				}
				else if ( redirectLocation.equals( "fight.php" ) )
				{
					// You have been redirected to a fight!  Here, you need
					// to complete the fight before you can continue.

					(new FightRequest( client )).run();

					// If it's not a straightforward adventure, then you
					// need to re-run the request to get the correct data.

					if ( client.inLoginState() )
						return true;
				}
				else
				{
					this.isErrorState = false;
					logStream.println( "Redirected: " + redirectLocation );
					return false;
				}
			}
			else if ( responseCode != 200 )
			{
				// Other error states occur when the server does not return
				// an "OK" reply.  For example, there is a reported issue
				// where the server returns 500.

				return true;
			}
			else
			{
				String line;
				StringBuffer replyBuffer = new StringBuffer();

				try
				{
					if ( formURL.getPath().indexOf( "chat" ) == -1 )
					{
						// In this case, there is actual content, and you're not being
						// redirected - therefore, store the server's reply inside of
						// the designated string for this purpose.  Note that the first
						// ten lines on every KoL site contains a script that tells the
						// browser to renest the page in frames (and hence is useless).

						line = istream.readLine();

						// There's a chance that there was no content in the reply
						// (header-only reply) - if that's the case, the line will
						// be null and you've hit an error state.

						if ( line == null )
						{
							isErrorState = true;
							logStream.println( "No reply content.  Retrying..." );
							return true;
						}

						// Check for MySQL errors, since those have been getting more
						// frequent, and would cause an I/O Exception to be thrown
						// unnecessarily, when a re-request would do.  I'm not sure
						// how they work right now (which line the MySQL error is
						// printed to), but for now, assume that it's the first line.

						if ( line.indexOf( "error" ) != -1 )
						{
							isErrorState = true;
							logStream.println( "Encountered MySQL error.  Retrying..." );
							return true;
						}

						replyBuffer.append( line );
						logStream.println( "Reading page content..." );
					}

					// The remaining lines form the rest of the content.  In order
					// to make it easier for string parsing, the line breaks will
					// ultimately be preserved.

					while ( (line = istream.readLine()) != null )
						replyBuffer.append( line );
				}
				catch ( Exception e )
				{
					// An Exception is clearly an error; here it will be reported
					// to the client, but another attempt will be made

					if ( formURLString.indexOf( "chat" ) == -1 && ( client == null || !client.isBuffBotActive() ) )
						updateDisplay( NOCHANGE, "Error reading server reply.  Retrying..." );

					if ( client != null )
					{
						logStream.println( e );
						e.printStackTrace( logStream );
					}

					return true;
				}

				responseText = replyBuffer.toString().replaceAll( "<script.*?</script>", "" );

				if ( client != null )
				{
					if ( client.getPasswordHash() == null )
						logStream.println( responseText );
					else
						logStream.println( responseText.replaceAll( client.getPasswordHash(), "" ) );
				}
			}
		}

		try
		{
			// Now that you're done, close the stream,
			// making sure to catch the exception if
			// it happens.

			istream.close();
		}
		catch ( Exception e )
		{
			// An Exception here is unusual, but it means that
			// something happened which disallowed closing of the
			// input stream.  Print the error to the log and
			// pretend nothing happened.

			if ( client != null )
			{
				logStream.println( e );
				e.printStackTrace( logStream );
			}
		}

		// Null the pointer to help the garbage collector
		// identify this as discardable data and return
		// from the function call.

		istream = null;
		this.isErrorState = false;
		return true;
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
			System.out.println( message );
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
}