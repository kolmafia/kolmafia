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
import java.io.IOException;

import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.StringTokenizer;
import java.text.DecimalFormat;

/**
 * Most aspects of Kingdom of Loathing are accomplished by submitting
 * forms and their accompanying data.  This abstract class is designed
 * to encapsulate this behavior by providing all descendant classes
 * (which simulate specific aspects of Kingdom of Loathing) with the
 * <code>addFormField()</code> method.  Note that the actual information
 * is sent to the server through the <code>run()</code> method.
 */

public class KoLRequest implements Runnable
{
	protected static final DecimalFormat df = new DecimalFormat();
	private static String KOL_ROOT = "http://www.kingdomofloathing.com/";
	static
	{	applySettings();
	}

	private static final int MAX_RETRIES = 4;

	private URL formURL;
	private StringBuffer formURLBuffer;
	private String sessionID;
	private List data;
	private boolean doOutput;

	protected KoLmafia client;
	protected KoLFrame frame;
	protected PrintStream logStream;

	protected int responseCode;
	protected boolean isErrorState;
	protected String redirectLocation;

	protected String replyContent;
	protected HttpURLConnection formConnection;

	/**
	 * Static method called when <code>KoLRequest</code> is first
	 * instantiated or whenever the settings have changed.  This
	 * initializes the login server to the one stored in the user's
	 * settings, as well as initializes the user's proxy settings.
	 */

	public static void applySettings()
	{
		KoLSettings currentSettings = new KoLSettings();

		if ( currentSettings.getProperty( "proxySet" ).equals( "true" ) )
		{
			System.setProperty( "proxySet", "true" );

			String proxyHost = currentSettings.getProperty( "http.proxyHost" );

			try
			{	System.setProperty( "http.proxyHost", InetAddress.getByName( proxyHost ).getHostAddress() );
			}
			catch ( UnknownHostException e )
			{	System.setProperty( "http.proxyHost", proxyHost );
			}

			System.setProperty( "http.proxyPort", currentSettings.getProperty( "http.proxyPort" ) );
			String proxyUser = currentSettings.getProperty( "http.proxyUser" );

			if ( proxyUser != null )
			{
				System.setProperty( "http.proxyUser", proxyUser );
				System.setProperty( "http.proxyPassword", currentSettings.getProperty( "http.proxyPassword" ) );
			}
			else
			{
				System.getProperties().remove( "http.proxyUser" );
				System.getProperties().remove( "http.proxyPassword" );
			}
		}
		else
		{
			System.setProperty( "proxySet", "false" );
			System.getProperties().remove( "http.proxyHost" );
			System.getProperties().remove( "http.proxyPort" );
			System.getProperties().remove( "http.proxyUser" );
			System.getProperties().remove( "http.proxyPassword" );
		}


		switch ( Integer.parseInt( currentSettings.getProperty( "loginServer" ) ) )
		{
			case 0:	autoDetectServer();
			case 1:	setLoginServer( "www.kingdomofloathing.com" );
			case 2:	setLoginServer( "www2.kingdomofloathing.com" );
			case 3:	setLoginServer( "www3.kingdomofloathing.com" );
		}
	}

	/**
	 * Static method used to auto detect the server to be used as
	 * the root for all requests by all KoLmafia clients running
	 * on the current JVM instance.
	 */

	private static void autoDetectServer()
	{
		// This test uses the Kingdom of Loathing automatic balancing
		// server, rather than allowing users to specify the root;
		// usually, this works out to the benefit of everyone.

		KoLRequest root = new KoLRequest( null, "" );
		root.run();
		setLoginServer( root.formConnection.getURL().getHost() );
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

		try
		{	KOL_ROOT = "http://" + InetAddress.getByName( server ).getHostAddress() + "/";
		}
		catch ( UnknownHostException e )
		{	KOL_ROOT = "http://" + server + "/";
		}
	}

	/**
	 * Constructs a new KoLRequest.  The class is not declared abstract so that
	 * the static routine can run without problems, but for all intents and purposes,
	 * a generic KoLRequest will not be supported.
	 *
	 * @param	client	The client associated with this <code>KoLRequest</code>
	 * @param	formURLString	The form to be used in posting data
	 */

	protected KoLRequest( KoLmafia client, String formURLString )
	{	this( client, formURLString, true );
	}

	/**
	 * Constructs a new KoLRequest.  The class is not declared abstract so that
	 * the static routine can run without problems, but for all intents and purposes,
	 * a generic KoLRequest will not be supported.
	 *
	 * @param	client	The client associated with this <code>KoLRequest</code>
	 * @param	formURLString	The form to be used in posting data
	 * @param	doOutput	Whether or not this will post data
	 */

	protected KoLRequest( KoLmafia client, String formURLString, boolean doOutput )
	{
		this.formURLBuffer = new StringBuffer( formURLString );

		if ( client != null )
		{
			this.client = client;
			this.frame = client.getActiveFrame();
			this.sessionID = client.getSessionID();
			this.logStream = client.getLogStream();
		}
		else
			this.logStream = new NullStream();

		data = new ArrayList();
		this.doOutput = doOutput;
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
	{	data.add( name + "=" + value );
	}

	/**
	 * Runs the thread, which prepares the connection for output, posts the data
	 * to the Kingdom of Loathing, and prepares the input for reading.  Because
	 * the Kingdom of Loathing has identical page layouts, all page reading and
	 * handling will occur through these method calls.
	 */

	public void run()
	{
		boolean connectSuccess = false;
		for ( int i = 0; i < MAX_RETRIES && !connectSuccess; ++i )
			connectSuccess = prepareConnection();

		// If the maximum number of retries elapsed and the connection was
		// still unsuccessful, notify the display and return; continuing
		// will likely cause some bizarre exception

		if ( !connectSuccess )
		{
			frame.updateDisplay( LoginFrame.ENABLED_STATE, "Connection timed out." );
			return;
		}

		postClientData();
		retrieveServerReply();
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
		// Here, if you weren't doing output, translate the form data
		// into a GET request.

		if ( !doOutput && !data.isEmpty() )
		{
			formURLBuffer.append( '?' );
			Iterator iterator = data.iterator();
			formURLBuffer.append( iterator.next().toString() );

			while ( iterator.hasNext() )
			{
				formURLBuffer.append( '&' );
				formURLBuffer.append( iterator.next().toString() );
			}
		}

		// With that taken care of, determine the actual URL that you
		// are about to request.

		try
		{
			this.formURL = new URL( KOL_ROOT + formURLBuffer.toString() );
		}
		catch ( MalformedURLException e )
		{
		}

		try
		{
			// For now, because there isn't HTTPS support, just open the
			// connection and directly cast it into an HttpURLConnection

			logStream.println( "Attempting to establish KoL connection..." );
			formConnection = (HttpURLConnection) formURL.openConnection();
		}
		catch ( IOException e )
		{
			// In the event that an IOException is thrown, one can assume
			// that there was a timeout; return false and let the loop
			// attempt to connect again

			return false;
		}

		logStream.println( "Connection established." );

		formConnection.setDoInput( true );
		formConnection.setDoOutput( !data.isEmpty() && doOutput );
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

		if ( data.isEmpty() || !doOutput )
			return true;

		logStream.println( "Posting form data..." );

		try
		{
			BufferedWriter ostream =
				new BufferedWriter( new OutputStreamWriter(
					formConnection.getOutputStream() ) );

			Iterator iterator = data.iterator();

			if ( iterator.hasNext() )
				ostream.write( iterator.next().toString() );

			while ( iterator.hasNext() )
			{
				ostream.write( "&" );
				ostream.write( iterator.next().toString() );
			}

			ostream.flush();
			ostream.close();
			ostream = null;

			logStream.println( "Posting data posted." );

			return true;
		}
		catch ( IOException e )
		{
			return false;
		}
	}

	/**
	 * Utility method used to retrieve the server's reply.  This method
	 * detects the nature of the reply via the response code provided
	 * by the server, and also detects the unusual states of server
	 * maintenance and session timeout.  All data retrieved by this
	 * method is stored in the instance variables for this class.
	 */

	private void retrieveServerReply()
	{
		try
		{
			// In the event of redirects, the appropriate flags should be set
			// indicating whether or not the direct is a normal redirect (ie:
			// one that results in something happening), or an error-type one
			// (ie: maintenance).

			logStream.println( "Retrieving server reply..." );

			isErrorState = false;
			responseCode = formConnection.getResponseCode();
			replyContent = "";
			redirectLocation = "";

			// Store any cookies that might be found in the headers of the
			// reply - there really is only one cookie to worry about, so
			// it will be stored here.

			BufferedReader istream =
				new BufferedReader( new InputStreamReader(
					formConnection.getInputStream() ) );

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

						frame.updateDisplay( KoLFrame.ENABLED_STATE, "Nightly maintenance." );
						isErrorState = true;
					}
					else if ( redirectLocation.startsWith( "login.php" ) )
					{
						frame.updateDisplay( KoLFrame.ENABLED_STATE, "Session timed out." );
						isErrorState = true;
					}
					else if ( redirectLocation.equals( "fight.php" ) )
					{
						// You have been redirected to a fight!  Here, you need
						// to complete the fight before you can continue

						isErrorState = false;
						(new FightRequest( client )).run();
					}
				}
				else if ( responseCode != 200 )
				{
					// Other error states occur when the server does not return
					// an "OK" reply.  For example, there is a reported issue
					// where the server returns 500.

					isErrorState = true;
				}
				else
				{
					String line;

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
							return;
						}

						// Check for MySQL errors, since those have been getting more
						// frequent, and would cause an I/O Exception to be thrown
						// unnecessarily, when a re-request would do.  I'm not sure
						// how they work right now (which line the MySQL error is
						// printed to), but for now, assume that it's the first line.

						if ( line.indexOf( "()" ) != -1 )
						{
							logStream.println( "MySQL error.  Repeating request..." );
							this.run();  return;
						}

						logStream.println( "Skipping frame-nesting Javascript..." );

						for ( int i = 0; i < 9; ++i )
							istream.readLine();

						logStream.println( "Reading page content..." );
					}

					// The remaining lines form the rest of the content.  In order
					// to make it easier for string parsing, the line breaks will
					// ultimately be preserved.

					StringBuffer replyBuffer = new StringBuffer();

					while ( (line = istream.readLine()) != null )
						replyBuffer.append( line );

					replyContent = replyBuffer.toString();
					logStream.println( replyContent );
				}

				// If you've encountered an error state, then make sure the
				// client knows that the request has been cancelled

				if ( isErrorState )
					client.updateAdventure( false, false );
			}

			// Now that you're done, close the stream and prepare it for
			// garbage collection by setting it to null

			istream.close();
			istream = null;
		}
		catch ( IOException e )
		{
			// An IOException is clearly an error; here it will be reported
			// to the client, but another attempt will be made

			isErrorState = true;
			if ( frame != null )
				frame.updateDisplay( KoLFrame.ENABLED_STATE, "I/O error.  Retrying..." );
			else if ( client != null )
				logStream.println( e );

			this.run();
		}
	}

	/**
	 * Utility method used to process the results of any adventure
	 * in the Kingdom of Loathing.  This method searches for items,
	 * stat gains, and losses within the provided string.
	 *
	 * @param	results	The string containing the results of the adventure
	 */

	protected final void processResults( String results )
	{
		logStream.println( "Processing results..." );

		String plainTextResult = results.replaceAll( "<.*?>", "\n" );
		StringTokenizer parsedResults = new StringTokenizer( plainTextResult, "\n" );
		String lastToken = null;

		while ( parsedResults.hasMoreTokens() )
		{
			lastToken = parsedResults.nextToken();

			// Skip effect acquisition - it's followed by a boldface
			// which makes the parser think it's found an item.

			if ( lastToken.startsWith( "You acquire an effect:" ) )
				parsedResults.nextToken();
			else if ( lastToken.startsWith( "You acquire" ) )
				client.parseResult( parsedResults.nextToken() );
			else if ( (lastToken.startsWith( "You gain" ) || lastToken.startsWith( "You lose" )) && !lastToken.endsWith( "Drunkenness." ) )
				client.parseResult( lastToken );
		}
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
}