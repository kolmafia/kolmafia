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
import java.net.URLDecoder;
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
import java.io.UnsupportedEncodingException;

import java.util.List;
import java.util.ArrayList;
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
		{ "www.kingdomofloathing.com", "69.16.150.196" },
		{ "www2.kingdomofloathing.com", "69.16.150.197" },
		{ "www3.kingdomofloathing.com", "69.16.150.198" },
		{ "www4.kingdomofloathing.com", "69.16.150.199" },
		{ "www5.kingdomofloathing.com", "69.16.150.200" },
	};

	public static final int SERVER_COUNT = SERVERS.length;

	private static String KOL_HOST = SERVERS[0][0];
	private static String KOL_ROOT = "http://" + SERVERS[0][1] + "/";

	private URL formURL;
	private boolean followRedirects;
	private String formURLString;

	private String sessionID;
	private List data;

	protected KoLmafia client;
	protected boolean statusChanged;

	protected int responseCode;
	protected String responseText;
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
				{
					System.setProperty( "http.proxyHost", InetAddress.getByName( proxyHost ).getHostAddress() );
				}
				catch ( UnknownHostException e )
				{
					e.printStackTrace( KoLmafia.getLogStream() );
					e.printStackTrace();

					System.setProperty( "http.proxyHost", proxyHost );
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

			// Determine the login server that will be used.

			int setting = Integer.parseInt( GLOBAL_SETTINGS.getProperty( "loginServer" ) );
			int server = ( setting < 1 || setting > SERVER_COUNT ) ? RNG.nextInt( SERVER_COUNT ) : setting - 1;
			setLoginServer( SERVERS[server][0] );
		}
		catch ( Exception e )
		{
			// An exception here means that the attempt to set up the proxy
			// server failed or the attempt to set the login server failed.
			// Because these result in default values, pretend nothing
			// happened and carry on with business.

			e.printStackTrace( KoLmafia.getLogStream() );
			e.printStackTrace();
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
		this.data = new ArrayList();
		this.followRedirects = followRedirects;

		if ( formURLString.indexOf( "?" ) == -1 )
		{
			this.formURLString = formURLString;
			return;
		}
		
		String [] splitURLString = formURLString.split( "\\?" );
		this.formURLString = splitURLString[0];
		addEncodedFormFields( splitURLString[1] );
	}

	/**
	 * Returns the location of the form being used for this URL, in case
	 * it's ever needed/forgotten.
	 */

	protected String getURLString()
	{	return data.isEmpty() ? formURLString : formURLString + "?" + getDataString( false );
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
		if ( name.equals( "pwd" ) || name.equals( "phash" ) )
		{
			data.add( name );
			return;
		}

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

			e.printStackTrace( KoLmafia.getLogStream() );
			e.printStackTrace();

			return;
		}

		// Make sure that when you're adding data fields, you don't
		// submit duplicate fields.

		if ( !allowDuplicates )
		{
			String [] existingData = new String[ data.size() ];
			data.toArray( existingData );

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
	{
		String [] currentComponent = element.split( "=" );

		if ( currentComponent[0].equals( "pwd" ) || currentComponent[0].equals( "phash" ) )
			addFormField( currentComponent[0], "", false );
		else if ( currentComponent.length == 1 )
			addFormField( currentComponent[0], "", true );
		else
			addFormField( currentComponent[0], currentComponent[1], true );
	}

	/**
	 * Adds an already encoded form field to the KoLRequest.
	 * @param	element	The field to be added
	 */

	protected void addEncodedFormField( String element )
	{
		// Just decode it first
		String decoded;
		try
		{
			decoded = URLDecoder.decode( element, "UTF-8" );
		}
		catch ( UnsupportedEncodingException e )
		{
			// Say what?
			decoded = element;
		}
		addFormField( decoded );
	}

	protected void addEncodedFormFields( String fields )
	{
		if ( fields.indexOf( "&" ) == -1 )
			addEncodedFormField( fields );
		
		String [] tokens = fields.split( "(&)" );
		for ( int i = 0; i < tokens.length; ++i )
			addEncodedFormField( tokens[i] );
	}

	private String getDataString( boolean includeHash )
	{
		StringBuffer dataBuffer = new StringBuffer();
		String [] elements = new String[ data.size() ];
		data.toArray( elements );

		for ( int i = 0; i < elements.length; ++i )
		{
			if ( i > 0 )
				dataBuffer.append( '&' );

			if ( elements[i].equals( "pwd" ) || elements[i].equals( "phash" ) )
			{
				dataBuffer.append( elements[i] );

				if ( includeHash )
				{
					dataBuffer.append( "=" );
					dataBuffer.append( client.getPasswordHash() );
				}
			}
			else
				dataBuffer.append( elements[i] );
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
		// Returning to thread synchronization.  Because requests
		// are forced to occur in separate threads, and the only
		// loop is the time-in loop, this should not cause a deadlock.

		if ( !isDelayExempt() )
		{
			synchronized ( KoLRequest.class )
			{
				KoLRequest.isServerFriendly = getProperty( "serverFriendly" ).equals( "true" );
				execute();
			}
		}
		else
			execute();
	}
	
	public void execute()
	{
		// If you're about to fight the Naughty Sorceress,
		// clear your list of effects.
		
		if ( getURLString().equals( "lair6.php?place=5") )
			KoLCharacter.getEffects().clear();
		
		// You are allowed a maximum of four attempts
		// to run the request.  This prevents KoLmafia
		// from spamming the servers.

		do
		{
			statusChanged = false;

			// Only add in a delay when you're out of login.
			// If you're still doing the login process, ignore
			// the delay to avoid people switching the option
			// off just to avoid login slowdown.

			if ( !isDelayExempt() )
			{
				if ( isServerFriendly )
					KoLRequest.delay();
				else if ( getProperty( "synchronizeFightFrame" ).equals( "true" ) )
					KoLRequest.delay( 1000 );
			}
		}
		while ( !prepareConnection() || !postClientData() || !retrieveServerReply() );

		// If the user wants to show all the requests in the browser, then
		// make sure it's updated.

		if ( responseCode == 200 )
		{
			// Synchronize if requested

			if ( !isDelayExempt() )
			{
				client.setCurrentRequest( this );
				showInBrowser( false );
			}

			processResults();
		}
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
			e.printStackTrace( KoLmafia.getLogStream() );
			e.printStackTrace();
		}
	}

	private boolean isDelayExempt()
	{	return client == null || this instanceof LoginRequest || this instanceof ChatRequest || this instanceof CharpaneRequest;
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

		// Make sure that all variables are reset before you reopen
		// the connection.  Invoke the garbage collector to minimize
		// memory consumption.

		formURL = null;
		responseText = null;
		redirectLocation = null;
		formConnection = null;

		// With that taken care of, determine the actual URL that you
		// are about to request.

		try
		{
			this.formURL = new URL( KOL_ROOT + formURLString );
		}
		catch ( MalformedURLException e )
		{
			DEFAULT_SHELL.updateDisplay( ERROR_STATE, "Error in URL: " + KOL_ROOT + formURLString );

			e.printStackTrace( KoLmafia.getLogStream() );
			e.printStackTrace();

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

			e.printStackTrace( KoLmafia.getLogStream() );
			e.printStackTrace();

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
			String dataString = getDataString( true );

			if ( client != null && client.getPasswordHash() != null )
				KoLmafia.getLogStream().println( dataString.replaceAll( client.getPasswordHash(), "" ) );

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

			if ( e instanceof FileNotFoundException )
			{
				DEFAULT_SHELL.updateDisplay( ERROR_STATE, "Page <" + formURLString + "> not found." );

				if ( client != null )
				{
					KoLmafia.getLogStream().println( e );
					e.printStackTrace( KoLmafia.getLogStream() );
				}

				// In this case, it's like a false redirect, but to
				// a page which no longer exists.  Pretend it's the
				// maintenance page.

				responseCode = 302;
				responseText = "";
				redirectLocation = "maint.php";

				return true;
			}

			KoLmafia.getLogStream().println( "Connection timed out during response.  Retrying..." );

			// Add in an extra delay in the event of a time-out in order
			// to be nicer on the KoL servers.

			KoLRequest.delay();
			return false;
		}

		boolean shouldStop = true;

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

					DEFAULT_SHELL.updateDisplay( ABORT_STATE, "Nightly maintenance." );
					shouldStop = true;
				}
				else if ( redirectLocation.startsWith( "login.php" ) )
				{
					DEFAULT_SHELL.updateDisplay( ABORT_STATE, "Session timed out." );

					if ( !formURLString.equals( "login.php" ) && client.getSettings().getProperty( "forceReconnect" ).equals( "true" ) )
						client.executeTimeInRequest();
					else if ( this instanceof LocalRelayRequest )
						client.executeTimeInRequest();
					else
						shouldStop = true;
				}
				else if ( followRedirects )
				{
					// Re-setup this request to follow the redirect
					// desired and rerun the request.

					this.formURLString = redirectLocation;
					this.data.clear();
					this.followRedirects = followRedirects;

					return false;
				}
				else if ( redirectLocation.equals( "fight.php" ) && !(this instanceof LocalRelayRequest) )
				{
					// You have been redirected to a fight!  Here, you need
					// to complete the fight before you can continue.

					FightRequest battle = new FightRequest( client );
					battle.run();

					return this instanceof AdventureRequest || getClass() == KoLRequest.class;
				}
				else if ( redirectLocation.equals( "choice.php" ) && !(this instanceof LocalRelayRequest) )
				{
					shouldStop = processChoiceAdventure();
				}
				else
				{
					shouldStop = true;
					KoLmafia.getLogStream().println( "Redirected: " + redirectLocation );
				}
			}
			else if ( responseCode == 200 )
			{
				String line = null;
				StringBuffer replyBuffer = new StringBuffer();
				StringBuffer rawBuffer = new StringBuffer();

				try
				{
					line = istream.readLine();

					// There's a chance that there was no content in the reply
					// (header-only reply) - if that's the case, the line will
					// be null and you've hit an error state.

					if ( line == null )
					{
						KoLmafia.getLogStream().println( "No reply content.  Retrying..." );
					}

					// Check for MySQL errors, since those have been getting more
					// frequent, and would cause an I/O Exception to be thrown
					// unnecessarily, when a re-request would do.  I'm not sure
					// how they work right now (which line the MySQL error is
					// printed to), but for now, assume
					// that it's the first line.

					else if ( line.indexOf( "error" ) != -1 )
					{
						KoLmafia.getLogStream().println( "Encountered MySQL error.  Retrying..." );
					}

					// The remaining lines form the rest of the content.  In order
					// to make it easier for string parsing, the line breaks will
					// ultimately be preserved.

					else
					{
						KoLmafia.getLogStream().println( "Reading page content..." );

						// Line breaks bloat the log, but they are important
						// inside <textarea> input fields.

						boolean insideTextArea = false;

						do
						{
							replyBuffer.append( line );
							rawBuffer.append( line );
							rawBuffer.append( LINE_BREAK );

							if ( line.indexOf( "</textarea" ) != -1 )
								insideTextArea = false;
							else if ( line.indexOf( "<textarea" ) != -1 )
								insideTextArea = true;

							if ( insideTextArea )
								replyBuffer.append( LINE_BREAK );
						}
						while ( (line = istream.readLine()) != null );
					}
				}
				catch ( Exception e )
				{
					// An Exception is clearly an error; here it will be reported
					// to the client, but another attempt will be made

					if ( formURLString.indexOf( "chat" ) == -1 )
						KoLmafia.getLogStream().println( "Error reading server reply.  Retrying..." );

					e.printStackTrace( KoLmafia.getLogStream() );
					e.printStackTrace();
				}

				responseText = replyBuffer.toString().replaceAll( "<script.*?</script>", "" );
				processRawResponse( rawBuffer.toString() );

				if ( client != null && client.getPasswordHash() != null )
					KoLmafia.getLogStream().println( responseText.replaceAll( client.getPasswordHash(), "" ) );
				else
					KoLmafia.getLogStream().println(
						responseText.replaceAll( "name=pwd value=\"?[^>]*>", "" ).replaceAll( "pwd=[0-9a-f]+", "" ) );
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

			e.printStackTrace( KoLmafia.getLogStream() );
			e.printStackTrace();
		}

		// Null the pointer to help the garbage collector
		// identify this as discardable data and return
		// from the function call.

		istream = null;
		return shouldStop;
	}

	/**
	 * This method allows classes to process a raw, unfiltered 
	 * server response.
	 */

	protected void processRawResponse( String rawResponse )
	{	statusChanged = formURLString.indexOf( "charpane.php" ) == -1 && rawResponse.indexOf( "charpane.php" ) != -1;
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

	protected void processResults()
	{
		int previousHP = KoLCharacter.getCurrentHP();
		boolean needsRefresh = client.processResults( responseText );

		// If the character's health drops below zero, make sure
		// that beaten up is added to the effects.

		if ( previousHP != 0 && KoLCharacter.getCurrentHP() == 0 )
			client.processResult( KoLAdventure.BEATEN_UP.getInstance( 3 - KoLAdventure.BEATEN_UP.getCount( KoLCharacter.getEffects() ) ) );

		if ( getAdventuresUsed() > 0 )
			client.processResult( new AdventureResult( AdventureResult.ADV, 0 - getAdventuresUsed() ) );

		if ( statusChanged && RequestFrame.willRefreshStatus() )
			RequestFrame.refreshStatus();
		else if ( needsRefresh )
			CharpaneRequest.getInstance().run();

		KoLCharacter.updateStatus();
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

		// Synchronize if requested
		if ( getProperty( "synchronizeFightFrame" ).equals( "false" ) )
			request.showInBrowser( false );

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

			DEFAULT_SHELL.updateDisplay( ERROR_STATE, "Encountered choice adventure with no choices." );

			// Finish in browser if requested
			if ( getProperty( "synchronizeFightFrame" ).equals( "false" ) )
				request.showInBrowser( true );

			return false;
		}

		String choice = choiceMatcher.group(1);
		String option = "choiceAdventure" + choice;
		String decision = getProperty( option );

		// If there is currently no setting which determines the
		// decision, give an error and bail.

		if ( decision == null )
		{
			DEFAULT_SHELL.updateDisplay( ERROR_STATE, "Unsupported choice adventure #" + choice );
			request.showInBrowser( true );

			return false;
		}

		// If the user wants to ignore this specific choice or all
		// choices, see if this choice is ignorable.

		if ( decision.equals( "0" ) )
		{
			String ignoreChoice = AdventureDatabase.ignoreChoiceOption( option );
			if ( ignoreChoice != null )
				decision = ignoreChoice;
		}

		// Make sure that we've resolved to a non-0 choice.

		if ( decision.equals( "0" ) )
		{
			DEFAULT_SHELL.updateDisplay( ERROR_STATE, "Can't ignore choice adventure #" + choice );
			request.showInBrowser( true );

			return false;
		}

		boolean completeOutfit = false;
		String [] possibleDecisions = null;

		int decisionIndex = Integer.parseInt( decision ) - 1;

		for ( int i = 0; i < AdventureDatabase.CHOICE_ADVS.length; ++i )
			if ( AdventureDatabase.CHOICE_ADVS[i][0][0].equals( option ) )
			{
				if ( AdventureDatabase.CHOICE_ADVS[i].length == 4 )
				{
					completeOutfit = AdventureDatabase.CHOICE_ADVS[i][2][ decisionIndex ].equals( "Complete the outfit" );
					possibleDecisions = AdventureDatabase.CHOICE_ADVS[i][3];
				}
			}

		// Only change the decision if the user-specified option
		// will not satisfy something on the conditions list.

		if ( completeOutfit )
		{
			// Here, you have an outfit completion option.  Therefore
			// determine which outfit needs to be completed. Just
			// choose the item that the player does not have, and if
			// they have everything, just make a random choice.

			decision = null;
					
			for ( int i = 0; i < 3; ++i )
				if ( possibleDecisions[i] != null && !KoLCharacter.hasItem( new AdventureResult( Integer.parseInt( possibleDecisions[0] ), 1 ), false ) )
					decision = String.valueOf( i + 1 );

			if ( decision == null )
				decision = String.valueOf( RNG.nextInt( 3 ) + 1 );
		}
		else if ( possibleDecisions != null )
		{
			for ( int i = 0; i < possibleDecisions.length; ++i )
				if ( possibleDecisions[i] != null && client.getConditions().contains( new AdventureResult( Integer.parseInt( possibleDecisions[i] ), 1 ) ) )
					decision = String.valueOf( i + 1 );
		}

		// If there is currently a setting which determines the
		// decision, make that decision and submit the form.

		request = new KoLRequest( client, "choice.php" );
		request.addFormField( "pwd" );
		request.addFormField( "whichchoice", choice );
		request.addFormField( "option", decision );

		request.run();

		// Synchronize if requested
		if ( getProperty( "synchronizeFightFrame" ).equals( "false" ) )
			request.showInBrowser( false );

		// Manually process any adventure usage for choice adventures,
		// since they necessarily consume an adventure.

		if ( AdventureDatabase.consumesAdventure( option, decision ) )
		{
			client.processResult( new AdventureResult( AdventureResult.ADV, -1 ) );
			KoLCharacter.updateStatus();
		}

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

	/*
	 * Method to display the current request in the Fight Frame.
	 *
	 * If we are synchronizing, show all requests
	 * If we are finishing, show only exceptional requests
	 */

	protected void showInBrowser( boolean exceptional )
	{
		// Check to see if this request should be showed
		// in a browser.  If you're using a command-line
		// interface, then you should not display the request.

		if ( client instanceof KoLmafiaCLI )
			return;

		if ( !exceptional && getProperty( "synchronizeFightFrame" ).equals( "false" ) )
			return;

		// Only show the request if the response code is
		// 200 (not a redirect or error).

		if ( responseCode == 200 )
			FightFrame.showRequest( this );
	}

	public String getCommandForm( int iterations )
	{	return "";
	}
}
