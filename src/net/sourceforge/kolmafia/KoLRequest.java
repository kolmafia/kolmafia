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

/**
 * Most aspects of Kingdom of Loathing are accomplished by submitting
 * forms and their accompanying data.  This abstract class is designed
 * to encapsulate this behavior by providing all descendant classes
 * (which simulate specific aspects of Kingdom of Loathing) with the
 * <code>submitData</code> method.
 */

public class KoLRequest implements Runnable
{
	private static String KOL_ROOT = "http://www.kingdomofloathing.com/";
	private static final int MAX_RETRIES = 4;

	private URL formURL;
	private String sessionID;
	private List data;

	protected KoLmafia client;
	protected KoLFrame frame;
	protected PrintStream logStream;

	protected int responseCode;
	protected boolean isErrorState;
	protected String redirectLocation;

	protected String replyContent;
	protected HttpURLConnection formConnection;

	public static void autoDetectServer()
	{
		// This test uses the Kingdom of Loathing automatic balancing
		// server, rather than allowing users to specify the root;
		// usually, this works out to the benefit of everyone.

		KoLRequest root = new KoLRequest( null, "" );
		root.run();
		KOL_ROOT = "http://" + root.formConnection.getURL().getHost() + "/";
	}

	public static void setLoginServer( String server )
	{	KOL_ROOT = "http://" + server + "/";
	}

	/**
	 * Constructs a new KoLRequest.  The class is not declared abstract so that
	 * the static routine can run without problems, but for all intents and purposes,
	 * a generic KoLRequest will not be supported.
	 */

	protected KoLRequest( KoLmafia client, String formURLString )
	{
		try
		{
			this.formURL = new URL( KOL_ROOT + formURLString );
		}
		catch ( MalformedURLException e )
		{
		}

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
	}

	/**
	 * Adds the given form field to the KoLRequest.  Descendant classes should
	 * use this method if they plan on submitting forms to Kingdom of Loathing
	 * before a call to the <code>super.run()</code> method.  Ideally, these
	 * fields can be added at construction time.
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

	private boolean prepareConnection()
	{
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
		formConnection.setDoOutput( !data.isEmpty() );
		formConnection.setUseCaches( false );
		formConnection.setInstanceFollowRedirects( false );

		formConnection.setRequestProperty( "Content-Type",
			"application/x-www-form-urlencoded" );

		if ( sessionID != null )
			formConnection.addRequestProperty( "Cookie", sessionID );

		return true;
	}

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
					// In this case, there is actual content, and you're not being
					// redirected - therefore, store the server's reply inside of
					// the designated string for this purpose.  Note that the first
					// ten lines on every KoL site contains a script that tells the
					// browser to renest the page in frames (and hence is useless).

					logStream.println( "Skipping frame-nesting Javascript..." );

					for ( int i = 0; i < 10; ++i )
						istream.readLine();

					logStream.println( "Skipping complete.  Reading page content..." );

					// The remaining lines form the rest of the content.  In order
					// to make it easier for string parsing, the line breaks will
					// ultimately be preserved.

					StringBuffer replyBuffer = new StringBuffer();
					String line;

					while ( (line = istream.readLine()) != null )
						replyBuffer.append( line );

					replyContent = replyBuffer.toString();

					logStream.println( "Reply content retrieved." );

					logStream.println(
						"\n ==========================\n" +
						replyContent.replaceAll( "><", "" ).replaceAll( "<[^>]*>", "\n" ) +
						"\n ==========================\n"
					);
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
			// An IOException is clearly an error - maybe it
			// should be reported as one, and the client
			// should be notified

			isErrorState = true;
			if ( frame != null )
				frame.updateDisplay( KoLFrame.ENABLED_STATE, "I/O error.  Please restart." );
			else if ( client != null )
				client.updateAdventure( false, false );
		}
	}

	protected void processResults( String results )
	{
		logStream.println( "Processing results..." );

		StringTokenizer parsedResults = new StringTokenizer( results.replaceAll( "<[^>]*>", "\n" ), "\n" );
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

	protected void skipTokens( StringTokenizer st, int tokenCount )
	{
		for ( int i = 0; i < tokenCount; ++i )
			st.nextToken();
	}

	protected int intToken( StringTokenizer st )
	{	return Integer.parseInt( st.nextToken() );
	}

	protected int intToken( StringTokenizer st, int fromStart )
	{
		String s = st.nextToken();
		return Integer.parseInt( s.substring( fromStart ) );
	}

	protected int intToken( StringTokenizer st, int fromStart, int fromEnd )
	{
		String s = st.nextToken();
		return Integer.parseInt( s.substring( fromStart, s.length() - 1 - fromEnd ) );
	}
}