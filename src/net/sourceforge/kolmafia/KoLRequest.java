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
import java.net.Socket;
import java.net.URLEncoder;
import java.net.URLDecoder;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.InetAddress;
import java.net.UnknownHostException;

import java.io.InputStream;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;

import java.util.List;
import java.util.ArrayList;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Map;
import java.util.Iterator;
import javax.swing.SwingUtilities;

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
	private static final byte [] CHAT_ARRAY = new byte[ 1024 ];
	private static final byte [] LEFT_ARRAY = new byte[ 2048 ];
	private static final byte [] BYTE_ARRAY = new byte[ 8096 ];

	private static final ByteArrayOutputStream CHAT_BUFFER = new ByteArrayOutputStream();
	private static final ByteArrayOutputStream LEFT_BUFFER = new ByteArrayOutputStream();
	private static final ByteArrayOutputStream BYTE_BUFFER = new ByteArrayOutputStream();

	private static final Pattern ORE_PATTERN = Pattern.compile( "3 chunks of (\\w+) ore" );
	private static final Pattern CHOICE_PATTERN = Pattern.compile( "whichchoice value=(\\d+)" );
	private static final Pattern CHOICE_DECISION_PATTERN = Pattern.compile( "whichchoice=(\\d+).*?option=(\\d+)" );
	private static final Pattern EVENT_PATTERN = Pattern.compile( "<table width=.*?<table><tr><td>(.*?)</td></tr></table>.*?<td height=4></td></tr></table>" );

	protected static final Pattern REDIRECT_PATTERN = Pattern.compile( "([^\\/]+)\\/login\\.php", Pattern.DOTALL );

	protected static String sessionID = null;
	protected static String passwordHash = null;
	private static boolean wasLastRequestSimple = false;

	protected static boolean isRatQuest = false;
	protected static int lastChoice = 0;
	protected static int lastDecision = 0;

	protected String encounter = "";
	private boolean shouldIgnoreResults;

	protected static boolean isCompactMode = false;

	private static final String [][] SERVERS =
	{
		{ "dev.kingdomofloathing.com", "69.16.150.202" },
		{ "www.kingdomofloathing.com", "69.16.150.196" },
		{ "www2.kingdomofloathing.com", "69.16.150.197" },
		{ "www3.kingdomofloathing.com", "69.16.150.198" },
		{ "www4.kingdomofloathing.com", "69.16.150.199" },
		{ "www5.kingdomofloathing.com", "69.16.150.200" },
		{ "www6.kingdomofloathing.com", "69.16.150.205" },
		{ "www7.kingdomofloathing.com", "69.16.150.206" },
		{ "www8.kingdomofloathing.com", "69.16.150.207" }
	};

	public static final int SERVER_COUNT = 8;

	protected static String KOL_HOST = SERVERS[1][0];
	protected static String KOL_ROOT = "http://" + SERVERS[1][1] + "/";

	protected URL formURL;
	protected boolean followRedirects;
	protected String formURLString;
	protected String dataString = null;

	protected boolean isChatRequest = false;
	protected boolean isEquipResult = false;
	protected boolean isConsumeRequest = false;

	private List data;

	protected boolean needsRefresh;
	protected boolean statusChanged;

	private boolean isDelayExempt;
	private int readInputLength;
	private int contentLength;

	protected int responseCode;
	protected String responseText;
	protected HttpURLConnection formConnection;
	protected String redirectLocation;

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
			String proxySet = StaticEntity.getProperty( "proxySet" );
			String proxyHost = StaticEntity.getProperty( "http.proxyHost" );
			String proxyUser = StaticEntity.getProperty( "http.proxyUser" );

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
					// This should not happen.  Therefore, print
					// a stack trace for debug purposes.

					StaticEntity.printStackTrace( e, "Error in proxy setup" );
					System.setProperty( "http.proxyHost", proxyHost );
				}

				System.setProperty( "http.proxyPort", StaticEntity.getProperty( "http.proxyPort" ) );
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
				System.setProperty( "http.proxyUser", StaticEntity.getProperty( "http.proxyUser" ) );
				System.setProperty( "http.proxyPassword", StaticEntity.getProperty( "http.proxyPassword" ) );
			}

			int defaultLoginServer = StaticEntity.getIntegerProperty( "defaultLoginServer" );
			setLoginServer( SERVERS[ defaultLoginServer == 0 ? 0 : 1 ][0] );
		}
		catch ( Exception e )
		{
			// This should not happen.  Therefore, print
			// a stack trace for debug purposes.

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

	public static void setLoginServer( String server )
	{
		if ( server == null )
			return;

		for ( int i = 0; i < SERVERS.length; ++i )
		{
			if ( SERVERS[i][0].indexOf( "server" ) != -1 || server.indexOf( SERVERS[i][0] ) != -1 || SERVERS[i][1].indexOf( server ) != -1 || server.indexOf( SERVERS[i][1] ) != -1 )
			{
				KOL_HOST = SERVERS[i][0];
				KOL_ROOT = "http://" + SERVERS[i][1] + "/";

				StaticEntity.setProperty( "loginServerName", KOL_HOST );
				KoLmafia.updateDisplay( "Redirected to " + KOL_HOST + "..." );
				System.setProperty( "http.referer", "http://" + KOL_HOST + "/main.html" );
			}
		}
	}

	private static void chooseNewLoginServer()
	{
		KoLmafia.updateDisplay( "Choosing new login server..." );
		for ( int i = 0; i < SERVER_COUNT; ++i )
			if ( SERVERS[i][0].equals( KOL_HOST ) )
			{
				int next = ( i + 1 ) % SERVERS.length;
				KOL_HOST = SERVERS[next][0];
				KOL_ROOT = "http://" + SERVERS[next][1] + "/";
				return;
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
	 * @param	client	Theassociated with this <code>KoLRequest</code>
	 * @param	formURLString	The form to be used in posting data
	 */

	protected KoLRequest( String formURLString )
	{	this( formURLString, false );
	}

	/**
	 * Constructs a new KoLRequest which will notify the given client
	 * of any changes and will use the given URL for data submission,
	 * possibly following redirects if the parameter so specifies.
	 *
	 * @param	client	Theassociated with this <code>KoLRequest</code>
	 * @param	formURLString	The form to be used in posting data
	 * @param	followRedirects	<code>true</code> if redirects are to be followed
	 */

	protected KoLRequest( String formURLString, boolean followRedirects )
	{
		this.data = new ArrayList();
		this.followRedirects = followRedirects;

		if ( formURLString.startsWith( "/" ) )
			formURLString = formURLString.substring(1);

		constructURLString( formURLString );
		this.isConsumeRequest = this instanceof ConsumeItemRequest;

		this.isDelayExempt = getClass() == KoLRequest.class || this instanceof LoginRequest || this instanceof ChatRequest ||
			this instanceof CharpaneRequest || this instanceof LocalRelayRequest;
	}

	protected KoLRequest constructURLString( String newURLString )
	{	return constructURLString( newURLString, true );
	}

	protected KoLRequest constructURLString( String newURLString, boolean usePostMethod )
	{
		this.data.clear();
		if ( newURLString.startsWith( "/" ) )
			newURLString = newURLString.substring(1);

		int formSplitIndex = newURLString.indexOf( "?" );

		if ( formSplitIndex == -1 || !usePostMethod )
		{
			this.formURLString = newURLString;
		}
		else
		{
			this.formURLString = newURLString.substring( 0, formSplitIndex );
			addEncodedFormFields( newURLString.substring( formSplitIndex + 1 ) );
		}

		this.isChatRequest = this.formURLString.indexOf( "chat" ) != -1 && this.formURLString.indexOf( "chatlaunch.php" ) == -1 &&
			this.formURLString.indexOf( "lchat.php" ) == -1;

		this.shouldIgnoreResults = isChatRequest || formURLString.startsWith( "message" ) || formURLString.startsWith( "search" ) ||
			formURLString.startsWith( "static" ) || formURLString.startsWith( "desc" ) || formURLString.startsWith( "show" ) || formURLString.startsWith( "doc" );

		return this;
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

		String encodedName = name == null ? "" : name;
		String encodedValue = value == null ? "" : value;

		try
		{
			encodedName = URLEncoder.encode( encodedName, "UTF-8" ) + "=";
			encodedValue = URLEncoder.encode( encodedValue, "UTF-8" );
		}
		catch ( Exception e )
		{
			// This should not happen.  Therefore, print
			// a stack trace for debug purposes.

			StaticEntity.printStackTrace( e );
			return;
		}

		// Make sure that when you're adding data fields, you don't
		// submit duplicate fields.

		if ( !allowDuplicates )
			for ( int i = 0; i < data.size(); ++i )
				if ( ((String)data.get(i)).startsWith( encodedName ) )
					data.remove( i );

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
		int equalIndex = element.indexOf( "=" );
		if ( equalIndex == -1 )
		{
			addFormField( element, "", false );
			return;
		}

		String name = element.substring( 0, equalIndex ).trim();
		String value = element.substring( equalIndex + 1 ).trim();

		if ( name.equals( "pwd" ) || name.equals( "phash" ) )
		{
			// If you were in Valhalla on login, then
			// make sure you discover the password hash
			// in some other way.

			if ( (passwordHash == null || passwordHash.equals( "" )) && value.length() != 0 )
				passwordHash = value;

			addFormField( name, "", false );
		}
		else
		{
			// Otherwise, add the name-value pair as was
			// specified in the original method.

			addFormField( name, value, true );
		}
	}

	/**
	 * Adds an already encoded form field to the KoLRequest.
	 * @param	element	The field to be added
	 */

	protected void addEncodedFormField( String element )
	{
		// Just decode it first
		String decoded = element;

		try
		{
			decoded = URLDecoder.decode( element, "UTF-8" );
		}
		catch ( UnsupportedEncodingException e )
		{
			// This should not happen.  Therefore, print
			// a stack trace for debug purposes.

			StaticEntity.printStackTrace( e );
			return;
		}

		addFormField( decoded );
	}

	protected void addEncodedFormFields( String fields )
	{
		if ( fields.indexOf( "&" ) == -1 )
		{
			addEncodedFormField( fields );
			return;
		}

		String [] tokens = fields.split( "&" );
		for ( int i = 0; i < tokens.length; ++i )
		{
			if ( tokens[i].indexOf( " " ) != -1 )
				addFormField( tokens[i] );
			else
				addEncodedFormField( tokens[i] );
		}
	}

	protected String getFormField( String key )
	{
		String [] fields = null;

		if ( data.isEmpty() )
		{
			int breakIndex = formURLString.indexOf( "?" ) + 1;
			if ( breakIndex != 0 )
				fields = formURLString.substring( breakIndex ).split( "&" );
		}
		else
		{
			fields = new String[ data.size() ];
			data.toArray( fields );
		}

		for ( int i = 0; fields != null && i < fields.length; ++i )
		{
			int splitIndex = fields[i].indexOf( "=" );
			if ( splitIndex != -1 )
			{
				String name = fields[i].substring( 0, splitIndex );
				if ( name.equalsIgnoreCase( key ) )
				{
					String value = fields[i].substring( splitIndex + 1 );

					try
					{
						// Everything was encoded as UTF-8, so go
						// ahead and decode it that way.

						return URLDecoder.decode( value, "UTF-8" );
					}
					catch ( Exception e )
					{
						// This shouldn't happen, but since you did
						// manage to find the key, return the value.

						return value;
					}
				}
			}
		}

		return null;
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
					dataBuffer.append( passwordHash );
				}
			}
			else
				dataBuffer.append( elements[i] );
		}

		if ( dataBuffer.indexOf( "whichskill=moxman" ) != -1 )
			return "action=moxman";

		return dataBuffer.toString();
	}

	private boolean shouldPrintDebug()
	{	return !(KoLmafia.getDebugStream() instanceof NullStream || isChatRequest);
	}

	/**
	 * Runs the thread, which prepares the connection for output, posts the data
	 * to the Kingdom of Loathing, and prepares the input for reading.  Because
	 * the Kingdom of Loathing has identical page layouts, all page reading and
	 * handling will occur through these method calls.
	 */

	public void run()
	{
		if ( shouldPrintDebug() )
			KoLmafia.getDebugStream().println( getClass() );

		if ( formURLString.indexOf( "sewer.php" ) != -1 )
		{
			if ( !isDelayExempt || StaticEntity.getBooleanProperty( "relayAlwaysBuysGum" ) )
			{
				AdventureDatabase.retrieveItem( "chewing gum on a string" );
				if ( isDelayExempt )
					KoLmafia.enableDisplay();
			}
		}

		if ( formURLString.indexOf( "casino.php" ) != -1 )
		{
			AdventureDatabase.retrieveItem( "casino pass" );
			if ( isDelayExempt )
				KoLmafia.enableDisplay();
		}

		// To avoid wasting turns, buy a can of hair spray before
		// climbing the tower.  Also, if the person has an NG,
		// make sure to construct it first.  If there are any
		// tower items sitting in the closet or that have not
		// been constructed, pull them out.

		if ( formURLString.indexOf( "lair4.php" ) != -1 || formURLString.indexOf( "lair5.php" ) != -1 )
		{
			SorceressLair.makeGuardianItems();
			if ( this instanceof LocalRelayRequest )
				KoLmafia.enableDisplay();
		}

		if ( KoLmafia.refusesContinue() && !isDelayExempt )
			return;

		needsRefresh = false;
		String urlString = getURLString();
		execute();

		// If this is the trapper page, make sure to check to
		// see if there's any changes to your inventory.

		if ( urlString.indexOf( "trapper.php" ) != -1 )
		{
			Matcher oreMatcher = ORE_PATTERN.matcher( responseText );
			if ( oreMatcher.find() )
			{
				StaticEntity.setProperty( "trapperOre", oreMatcher.group(1) + " ore" );
			}

			if ( responseText.indexOf( "You acquire" ) != -1 )
			{
				if ( responseText.indexOf( "crossbow" ) != -1 || responseText.indexOf( "staff" ) != -1 || responseText.indexOf( "sword" ) != -1 )
				{
					if ( responseText.indexOf( "asbestos" ) != -1 )
						StaticEntity.getClient().processResult( new AdventureResult( "asbestos ore", -3, false ) );
					else if ( responseText.indexOf( "linoleum" ) != -1 )
						StaticEntity.getClient().processResult( new AdventureResult( "linoleum ore", -3, false ) );
					else
						StaticEntity.getClient().processResult( new AdventureResult( "chrome ore", -3, false ) );
				}
				else if ( responseText.indexOf( "goat cheese pizza" ) != -1 )
				{
					StaticEntity.getClient().processResult( new AdventureResult( "goat cheese", -6, false ) );
				}
			}
		}

		// There are requests to the council which will also
		// decrement your inventory.

		if ( urlString.indexOf( "council.php" ) != -1 )
		{
			if ( responseText.indexOf( "500" ) != -1 )
				StaticEntity.getClient().processResult( new AdventureResult( "mosquito larva", -1, false ) );
			if ( responseText.indexOf( "batskin belt" ) != -1 )
				StaticEntity.getClient().processResult( new AdventureResult( "Boss Bat bandana", -1, false ) );
		}

		// If this is an equipment request, then reprint the
		// player's current equipment information.

		if ( isEquipResult )
			DEFAULT_SHELL.executeLine( "equip" );

		if ( urlString.equals( "main.php?refreshtop=true&noobmessage=true" ) )
			StaticEntity.getClient().handleAscension();
	}

	public void execute()
	{
		// If this is the rat quest, then go ahead and pre-set the data
		// to reflect a fight sequence (mini-browser compatibility).

		isRatQuest |= formURLString.indexOf( "rats.php" ) != -1;
		if ( !isChatRequest && formURLString.indexOf( "charpane" ) == -1 && formURLString.indexOf( "rats.php" ) == -1 )
			isRatQuest &= formURLString.indexOf( "fight.php" ) != -1;

		if ( isRatQuest )
			KoLmafia.addTavernLocation( this );

		readInputLength = 0;
		contentLength = 0;

		registerRequest();
		String urlString = getURLString();

		if ( urlString.indexOf( "choice.php" ) != -1 )
			saveLastChoice( urlString );

		if ( !isDelayExempt )
			StaticEntity.getClient().setCurrentRequest( this );

		// If you're about to fight the Naughty Sorceress,
		// clear your list of effects.

		if ( urlString.endsWith( "lair6.php?place=5" ) )
		{
			activeEffects.clear();
			needsRefresh = true;
		}
		if ( urlString.endsWith( "lair6.php?place=6" ) )
			KoLCharacter.setInteraction( KoLCharacter.getTotalTurnsUsed() >= 600 );

		statusChanged = false;
		while ( !prepareConnection() || !postClientData() || !retrieveServerReply() && !KoLmafia.refusesContinue() );

		StaticEntity.getClient().setCurrentRequest( null );
	}

	private void saveLastChoice( String url )
	{
		Matcher choiceMatcher = CHOICE_DECISION_PATTERN.matcher( url );
		if ( choiceMatcher.find() )
		{
			lastChoice = StaticEntity.parseInt( choiceMatcher.group(1) );
			lastDecision = StaticEntity.parseInt( choiceMatcher.group(2) );
		}
	}

	private void mapCurrentChoice( String text )
	{
		// Let the Violet Fog handle this
		if ( VioletFog.mapChoice( text ) )
			return;

		// Let the Louvre handle this
		if ( Louvre.mapChoice( text ) )
			return;
	}

	public static int getLastChoice()
	{	return lastChoice;
	}

	public static int getLastDecision()
	{	return lastDecision;
	}

	protected void registerRequest()
	{
		// If this is part of the login sequence, then there is no need
		// to register the request.

		if ( LoginRequest.isInstanceRunning() )
			return;

		String urlString = getURLString();

		// Certain kinds of requests do not get processed.  These include
		// pages without form data and player-interaction requests along
		// with the side pane.  The sewer is an example of a page that should
		// get logged without form data, though.

		if ( shouldIgnoreResults )
			return;

		if ( urlString.indexOf( "?" ) == -1 && urlString.indexOf( "sewer.php" ) == -1 )
			return;

		String equivalentCommand = getCommandForm();
		if ( !equivalentCommand.equals( "" ) )
		{
			KoLmafia.getSessionStream().println();
			KoLmafia.getSessionStream().println( equivalentCommand );
			return;
		}

		// If it's a runaway command, then make sure you register it
		// as the correct type of runaway.

		if ( urlString.indexOf( "fight.php" ) != -1 )
		{
			if ( urlString.indexOf( "runaway" ) != -1 )
				constructURLString( "fight.php?action=runaway" );

			FightRequest.processRequest( urlString );
			wasLastRequestSimple = false;
			return;
		}

		// In the event that this is an adventure, assume "snarfblat"
		// instead of "adv" in order to determine the location.

		if ( urlString.indexOf( "adv=" ) != -1 )
			urlString = urlString.replaceFirst( "adv=", "snarfblat=" );

		isEquipResult = urlString.indexOf( "which=2" ) != -1 && urlString.indexOf( "action=message" ) != -1;

		// First, try to match everything to an adventure, and print
		// the appropriate turn count information.

		KoLAdventure matchingLocation = AdventureDatabase.getAdventureByURL( urlString );

		if ( matchingLocation != null )
		{
			wasLastRequestSimple = false;
			KoLmafia.getSessionStream().println();
			matchingLocation.recordToSession();
		}
		else if ( KoLAdventure.recordToSession( urlString ) )
		{
			wasLastRequestSimple = false;
		}

		// Now for the more standard requests, like item consumption
		// or item creation or skill casting.

		else if ( ConsumeItemRequest.processRequest( urlString ) )
		{
			wasLastRequestSimple = false;
			isConsumeRequest = true;
		}
		else if ( ItemCreationRequest.processRequest( urlString ) )
		{
			wasLastRequestSimple = false;
		}
		else if ( UseSkillRequest.processRequest( urlString ) )
		{
			wasLastRequestSimple = false;
		}
		else if ( PulverizeRequest.processRequest( urlString ) )
		{
			wasLastRequestSimple = false;
		}

		// Otherwise, see if it matches one of the standard "changeup"
		// requests, like a familiar request or an equipment request.

		else if ( FamiliarRequest.processRequest( urlString ) )
		{
			wasLastRequestSimple = false;
		}
		else if ( EquipmentRequest.processRequest( urlString ) )
		{
			wasLastRequestSimple = false;
		}

		// Now, all the instances where items are transferred between
		// different locations.

		else if ( this instanceof SendMessageRequest )
		{
			wasLastRequestSimple = false;
		}
		else if ( ItemStorageRequest.processRequest( urlString ) )
		{
			wasLastRequestSimple = false;
		}
		else if ( AutoSellRequest.processRequest( urlString ) )
		{
			wasLastRequestSimple = false;
		}
		else if ( ClanStashRequest.processRequest( urlString ) )
		{
			wasLastRequestSimple = false;
		}
		else if ( GreenMessageRequest.processRequest( urlString ) )
		{
			wasLastRequestSimple = false;
		}
		else if ( GiftMessageRequest.processRequest( urlString ) )
		{
			wasLastRequestSimple = false;
		}
		else if ( ProposeTradeRequest.processRequest( urlString ) )
		{
			wasLastRequestSimple = false;
		}

		// For all other requests, log the URL of the location which
		// was visited.

		else if ( urlString.indexOf( "inventory" ) == -1 )
		{
			if ( !wasLastRequestSimple )
				KoLmafia.getSessionStream().println();

			wasLastRequestSimple = true;
			KoLmafia.getSessionStream().println( urlString );
		}
	}

	public static boolean shouldIgnore( String formURLString )
	{
		return formURLString.indexOf( "search" ) != -1  || formURLString.indexOf( "chat" ) != -1 || formURLString.indexOf( "clan" ) != -1 ||
			formURLString.indexOf( "snarfblat=101" ) != -1 || formURLString.indexOf( "adv=101" ) != -1;
	}

	/**
	 * Utility method which waits for the given duration without
	 * using Thread.sleep() - this means CPU usage can be greatly
	 * reduced.
	 */

	protected static boolean delay( long milliseconds )
	{
		if ( milliseconds == 0 )
			return true;

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
			// This should not happen.  Therefore, print
			// a stack trace for debug purposes.

			StaticEntity.printStackTrace( e );
		}

		return true;
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
		if ( shouldPrintDebug() )
			KoLmafia.getDebugStream().println( "Connecting to " + formURLString + "..." );

		// Make sure that all variables are reset before you reopen
		// the connection.

		responseCode = 0;
		responseText = null;
		redirectLocation = null;
		formConnection = null;

		try
		{
			// For now, because there isn't HTTPS support, just open the
			// connection and directly cast it into an HttpURLConnection

			this.formURL = this.formURLString.startsWith( "http:" ) ?
				new URL( this.formURLString ) : new URL( KOL_ROOT + this.formURLString );

			formConnection = (HttpURLConnection) formURL.openConnection();
		}
		catch ( Exception e )
		{
			// In the event that an Exception is thrown, one can assume
			// that there was a timeout; return false and let the loop
			// attempt to connect again

			if ( shouldPrintDebug() )
				KoLmafia.getDebugStream().println( "Error opening connection.  Retrying..." );

			if ( this instanceof LoginRequest )
				chooseNewLoginServer();

			return false;
		}

		formConnection.setDoInput( true );
		formConnection.setDoOutput( !data.isEmpty() );
		formConnection.setUseCaches( false );
		formConnection.setInstanceFollowRedirects( false );

		if ( sessionID != null )
		{
			if ( formURLString.indexOf( "inventory.php" ) != -1 && !StaticEntity.getProperty( "visibleBrowserInventory" ).equals( "" ) )
				formConnection.addRequestProperty( "Cookie", StaticEntity.getProperty( "visibleBrowserInventory" ) + "; " + sessionID );
			else
				formConnection.addRequestProperty( "Cookie", sessionID );
		}

		dataString = getDataString( true );
		formConnection.setRequestProperty( "Content-Type", "application/x-www-form-urlencoded" );

		if ( !data.isEmpty() )
			formConnection.setRequestProperty( "Content-Length", String.valueOf( dataString.length() ) );
		else
			formConnection.setRequestProperty( "Content-Length", "0" );


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

		try
		{
			formConnection.setRequestMethod( "POST" );

			if ( shouldPrintDebug() )
			{
				KoLmafia.getDebugStream().println( "Submitting data string: " + dataString );
				printRequestProperties();
			}

			BufferedWriter ostream = new BufferedWriter( new OutputStreamWriter( formConnection.getOutputStream() ) );
			ostream.write( dataString );

			ostream.flush();
			ostream.close();
			ostream = null;

			return true;
		}
		catch ( Exception e )
		{
			if ( shouldPrintDebug() )
				KoLmafia.getDebugStream().println( "Connection timed out during post.  Retrying..." );

			if ( this instanceof LoginRequest )
				chooseNewLoginServer();

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
		InputStream istream = null;

		// In the event of redirects, the appropriate flags should be set
		// indicating whether or not the direct is a normal redirect (ie:
		// one that results in something happening), or an error-type one
		// (ie: maintenance).

		if ( shouldPrintDebug() )
			KoLmafia.getDebugStream().println( "Retrieving server reply..." );

		responseText = "";
		redirectLocation = "";

		try
		{
			istream = formConnection.getInputStream();
			responseCode = formConnection.getResponseCode();
			redirectLocation = formConnection.getHeaderField( "Location" );
			contentLength = StaticEntity.parseInt( formConnection.getHeaderField( "Content-Length" ) );
		}
		catch ( Exception e1 )
		{
			if ( shouldPrintDebug() )
				KoLmafia.getDebugStream().println( "Connection timed out during response.  Retrying..." );

			try
			{
				if ( istream != null )
					istream.close();
			}
			catch ( Exception e2 )
			{
				// The input stream was already closed.  Ignore this
				// error and continue.
			}

			if ( this instanceof LoginRequest )
				chooseNewLoginServer();

			return formURLString.startsWith( "http://" );
		}

		if ( shouldPrintDebug() )
			printHeaderFields();

		boolean shouldStop = false;

		try
		{
			if ( responseCode != 200 )
			{
				// If the response code is not 200, then you've read all
				// the information you need.  Close the input stream.

				istream.close();
				shouldStop = responseCode == 302 ? handleServerRedirect() : true;
			}
			else
			{
				shouldStop = retrieveServerReply( istream );
				istream.close();
			}
		}
		catch ( Exception e )
		{
			// Do nothing, you're going to close the input stream
			// and nullify it in the next section.

			e.printStackTrace();
			return true;
		}

		istream = null;
		return shouldStop;
	}

	private boolean handleServerRedirect()
	{
		if ( redirectLocation == null )
			return true;

		// Check to see if this is a login page redirect.  If it is, then
		// construct the URL string and notify the browser that it should
		// change everything.

		Matcher matcher = REDIRECT_PATTERN.matcher( redirectLocation );
		if ( matcher.find() )
		{
			setLoginServer( matcher.group(1) );
			redirectLocation = redirectLocation.substring( redirectLocation.lastIndexOf( "/" ) + 1 );

			constructURLString( redirectLocation, false );
			return false;
		}

		if ( sessionID == null && redirectLocation.indexOf( "login.php" ) != -1 )
		{
			constructURLString( redirectLocation, false );
			return false;
		}

		if ( this instanceof LocalRelayRequest )
		{
			if ( formURLString.indexOf( "login.php" ) != -1 )
				LoginRequest.processLoginRequest( this );

			return true;
		}

		if ( redirectLocation.indexOf( "login.php" ) != -1 )
		{
			KoLmafia.updateDisplay( "Session timed out." );
			if ( StaticEntity.getBooleanProperty( "autoExecuteTimeIn" ) )
			{
				LoginRequest.executeTimeInRequest( false );
				return sessionID == null;
			}

			return true;
		}
		else if ( redirectLocation.indexOf( "maint.php" ) != -1 )
		{
			// If the system is down for maintenance, the user must be
			// notified that they should try again later.

			KoLmafia.updateDisplay( "Nightly maintenance." );

			if ( !LoginRequest.isInstanceRunning() && sessionID != null && StaticEntity.getBooleanProperty( "autoExecuteTimeIn" ) )
			{
				LoginRequest.executeTimeInRequest( true );
				return sessionID == null;
			}

			return true;
		}
		else if ( followRedirects )
		{
			// Re-setup this request to follow the redirect
			// desired and rerun the request.

			constructURLString( redirectLocation, false );
			return false;
		}
		else if ( redirectLocation.indexOf( "choice.php" ) != -1 )
		{
			processChoiceAdventure();
			return true;
		}
		else if ( redirectLocation.indexOf( "valhalla.php" ) != -1 )
		{
			passwordHash = "";
			return true;
		}
		else if ( redirectLocation.indexOf( "fight.php" ) != -1 )
		{
			// You have been redirected to a fight!  Here, you need
			// to complete the fight before you can continue.

			FightRequest.INSTANCE.run();
			return this instanceof AdventureRequest || getClass() == KoLRequest.class ||
				FightRequest.INSTANCE.getAdventuresUsed() == 0;
		}
		else
		{
			if ( shouldPrintDebug() )
				KoLmafia.getDebugStream().println( "Redirected: " + redirectLocation );

			return true;
		}
	}

	private boolean retrieveServerReply( InputStream istream ) throws Exception
	{
		// If you get this far, you know you've got a 200 response.  Therefore,
		// read all the data into the static byte array output stream and then
		// convert that string to UTF-8.

		if ( isChatRequest )
		{
			synchronized ( CHAT_BUFFER )
			{
				int availableBytes = 0;
				while ( (availableBytes = istream.read( CHAT_ARRAY )) != -1 )
					CHAT_BUFFER.write( CHAT_ARRAY, 0, availableBytes );

				this.responseText = CHAT_BUFFER.toString( "UTF-8" );
				CHAT_BUFFER.reset();
			}
		}
		else if ( formURLString.equals( "charpane.php" ) )
		{
			synchronized ( LEFT_BUFFER )
			{
				int availableBytes = 0;
				while ( (availableBytes = istream.read( LEFT_ARRAY )) != -1 )
					LEFT_BUFFER.write( LEFT_ARRAY, 0, availableBytes );

				this.responseText = LEFT_BUFFER.toString( "UTF-8" );
				LEFT_BUFFER.reset();
			}
		}
		else
		{
			synchronized ( BYTE_BUFFER )
			{
				int availableBytes = 0;
				while ( (availableBytes = istream.read( BYTE_ARRAY )) != -1 )
					BYTE_BUFFER.write( BYTE_ARRAY, 0, availableBytes );

				this.responseText = BYTE_BUFFER.toString( "UTF-8" );
				BYTE_BUFFER.reset();
			}
		}

		processResponse();
		return true;
	}

	/**
	 * This method allows classes to process a raw, unfiltered
	 * server response.
	 */

	protected void processResponse()
	{
		if ( shouldPrintDebug() )
			KoLmafia.getDebugStream().println( LINE_BREAK_PATTERN.matcher( responseText ).replaceAll( "" ) );

		statusChanged = responseText.indexOf( "charpane.php" ) != -1;
		if ( statusChanged && !(this instanceof LocalRelayRequest) )
			LocalRelayServer.addStatusMessage( "<!-- REFRESH -->" );

		if ( !isDelayExempt )
			checkForNewEvents();

		if ( isRatQuest )
			KoLmafia.addTavernLocation( this );

		encounter = AdventureRequest.registerEncounter( this );

		if ( !shouldIgnoreResults )
			parseResults();

		if ( formURLString.indexOf( "fight.php" ) != -1 )
			FightRequest.updateCombatData( encounter, responseText );

		if ( !isDelayExempt && formURLString.indexOf( "search" ) == -1 )
			showInBrowser( false );

		// Now let the main method of result processing for
		// each request type happen.

		processResults();

		// Let the mappers do their work

		if ( formURLString.indexOf( "choice.php" ) != -1 )
			mapCurrentChoice( responseText );

		if ( responseText.indexOf( "you look down and notice a ten-leaf clover" ) != -1 )
		{
			DEFAULT_SHELL.executeLine( "use 1 ten-leaf clover" );
			if ( isDelayExempt && !isChatRequest )
				KoLmafia.enableDisplay();
		}

		needsRefresh &= !(this instanceof LocalRelayRequest || this instanceof FightRequest);
		needsRefresh &= formURLString.indexOf( "charpane.php" ) == -1;

		statusChanged &= formURLString.indexOf( "charpane.php" ) == -1;
		StaticEntity.getClient().applyEffects();

		if ( statusChanged && RequestFrame.willRefreshStatus() )
		{
			RequestFrame.refreshStatus();
		}
		else if ( needsRefresh )
		{
			CharpaneRequest.getInstance().run();
		}
		else if ( formURLString.indexOf( "charpane.php" ) != -1 )
		{
			KoLCharacter.recalculateAdjustments();
			KoLCharacter.updateEquipmentLists();
			KoLCharacter.updateStatus();
		}
		else if ( !shouldIgnoreResults && !(this instanceof LocalRelayRequest) && getClass() != KoLRequest.class )
		{
			KoLCharacter.updateStatus();
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
		String token = st.nextToken().substring( fromStart );
		return StaticEntity.parseInt( token );
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
		String token = st.nextToken();
		token = token.substring( fromStart, token.length() - fromEnd );
		return StaticEntity.parseInt( token );
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

	protected void parseResults()
	{
		// If this is a lucky adventure, then remove a clover
		// from the player's inventory -- this will occur when
		// you see either "Your ten-leaf clover" or "your
		// ten-leaf clover" (shorten to "our ten-leaf clover"
		// for substring matching)

		if ( !isConsumeRequest && responseText.indexOf( "our ten-leaf clover" ) != -1 && responseText.indexOf( "puff of smoke" ) != -1 )
			StaticEntity.getClient().processResult( SewerRequest.CLOVER );

		if ( formURLString.indexOf( "sewer.php" ) != -1 && responseText.indexOf( "You acquire" ) != -1 )
			StaticEntity.getClient().processResult( SewerRequest.GUM );

		int previousHP = KoLCharacter.getCurrentHP();
		needsRefresh |= StaticEntity.getClient().processResults( responseText );
		needsRefresh |= getAdventuresUsed() > 0;

		// If the character's health drops below zero, make sure
		// that beaten up is added to the effects.

		if ( previousHP != 0 && KoLCharacter.getCurrentHP() == 0 )
		{
			// Wild hare is exempt from beaten up status if you
			// are beaten up in the middle of a battle.

			if ( !formURLString.equals( "fight.php" ) || responseText.indexOf( "lair6.php" ) != -1 )
				needsRefresh |= StaticEntity.getClient().processResult( KoLAdventure.BEATEN_UP.getInstance( 4 - KoLAdventure.BEATEN_UP.getCount( activeEffects ) ) );
			else if ( KoLCharacter.getFamiliar().getID() != 50 )
				needsRefresh |= StaticEntity.getClient().processResult( KoLAdventure.BEATEN_UP.getInstance( 3 - KoLAdventure.BEATEN_UP.getCount( activeEffects ) ) );
		}
	}

	protected void processResults()
	{
	}

	public void stealRequestData( KoLRequest request )
	{
		this.responseCode = request.responseCode;
		this.responseText = request.responseText;
		this.redirectLocation = request.redirectLocation;
		this.formConnection = request.formConnection;
	}

	/**
	 * Utility method which notifies thethat it needs to process
	 * the given choice adventure.
	 */

	public void processChoiceAdventure()
	{
		// You can no longer simply ignore a choice adventure.	One of
		// the options may have that effect, but we must at least run
		// choice.php to find out which choice it is.

		KoLRequest request = new KoLRequest( redirectLocation );
		request.run();

		String choice = null;
		String option = null;
		String decision = null;

		while ( request.responseText.indexOf( "choice.php" ) != -1 )
		{
			StaticEntity.getClient().processResult( new AdventureResult( AdventureResult.CHOICE, 1 ) );
			Matcher choiceMatcher = CHOICE_PATTERN.matcher( request.responseText );

			if ( !choiceMatcher.find() )
			{
				// choice.php did not offer us any choices. This would
				// be a bug in KoL itself. Bail now and let the user
				// finish by hand.

				KoLmafia.updateDisplay( ABORT_STATE, "Encountered choice adventure with no choices." );
				request.showInBrowser( true );
				return;
			}

			choice = choiceMatcher.group(1);
			option = "choiceAdventure" + choice;
			decision = StaticEntity.getProperty( option );

			// If this happens to be adventure 26 or 27,
			// check against the player's conditions.

			if ( (choice.equals( "26" ) || choice.equals( "27" )) && !conditions.isEmpty() )
			{
				for ( int i = 0; i < 12; ++i )
					if ( AdventureDatabase.WOODS_ITEMS[i].getCount( conditions ) > 0 )
						decision = choice.equals( "26" ) ? String.valueOf( (i / 4) + 1 ) : String.valueOf( ((i % 4) / 2) + 1 );
			}

			// If there is no setting which determines the
			// decision, see if it's in the violet fog

			if ( decision.equals( "" ) )
				decision = VioletFog.handleChoice( choice );

			// If there is no setting which determines the
			// decision, see if it's in the Louvre

			if ( decision.equals( "" ) )
				decision = Louvre.handleChoice( choice );

			// If there is currently no setting which determines the
			// decision, give an error and bail.

			if ( decision.equals( "" ) )
			{
				KoLmafia.updateDisplay( ABORT_STATE, "Unsupported choice adventure #" + choice );
				request.showInBrowser( true );
				return;
			}

			// If the user wants to ignore this specific choice or all
			// choices, see if this choice is ignorable.

			boolean willIgnore = false;

			if ( decision.equals( "0" ) )
			{
				String ignoreChoice = AdventureDatabase.ignoreChoiceOption( option );
				if ( ignoreChoice != null )
				{
					willIgnore = true;
					decision = ignoreChoice;
				}
			}

			// Always change the option whenever it's not an ignore option
			// and remember to store the result.

			if ( !willIgnore )
				decision = pickOutfitChoice( option, decision );

			request.clearDataFields();
			request.addFormField( "pwd" );
			request.addFormField( "whichchoice", choice );
			request.addFormField( "option", decision );
			request.run();

			// Certain choices cost meat when selected

			int meat = AdventureDatabase.consumesMeat( option, decision );
			if ( meat > 0 )
				StaticEntity.getClient().processResult( new AdventureResult( AdventureResult.MEAT, 0 - meat ) );
		}

		// Manually process any adventure usage for choice adventures,
		// since they necessarily consume an adventure.

		if ( AdventureDatabase.consumesAdventure( option, decision ) )
			needsRefresh = !request.needsRefresh;

		stealRequestData( request );
	}

	private String pickOutfitChoice( String option, String decision )
	{
		// Find the options for the choice we've encountered

		String [] possibleDecisions = null;
		for ( int i = 0; i < AdventureDatabase.CHOICE_ADVS.length; ++i )
		{
			if ( AdventureDatabase.CHOICE_ADVS[i].getSetting().equals( option ) )
			{
				possibleDecisions = AdventureDatabase.CHOICE_ADVS[i].getItems();
				break;
			}
		}

		// If it's not in the table (the castle wheel, for example) or
		// isn't an outfit completion choice, return the player's
		// chosen decision.

		if ( possibleDecisions == null )
			return decision.equals( "0" ) ? "1" : decision;

		// Choose an item in the conditions first, if it's available.
		// This allows conditions to override existing choices.

		for ( int i = 0; i < possibleDecisions.length; ++i )
		{
			if ( possibleDecisions[i] != null )
			{
				AdventureResult item = new AdventureResult( StaticEntity.parseInt( possibleDecisions[i] ), 1 );
				if ( conditions.contains( item ) )
					return String.valueOf( i + 1 );
			}
		}

		// If no item is found in the conditions list, and the player
		// has a non-ignore decision, go ahead and use it.

		if ( !decision.equals( "0" ) && StaticEntity.parseInt( decision ) - 1 < possibleDecisions.length )
			return decision;

		// If they have chosen to ignore this adventure, then choose an
		// item the player does not have

		for ( int i = 0; i < possibleDecisions.length; ++i )
		{
			if ( possibleDecisions[i] != null )
			{
				AdventureResult item = new AdventureResult( StaticEntity.parseInt( possibleDecisions[i] ), 1 );
				if ( !KoLCharacter.hasItem( item ) )
					return String.valueOf( i + 1 );
			}
		}

		// If they have everything and it's an ignore choice, then use
		// the first choice no matter what.

		return "1";
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

		if ( existingFrames.isEmpty() )
			return;

		if ( !exceptional && !StaticEntity.getBooleanProperty( "showAllRequests" ) )
			return;

		// Only show the request if the response code is
		// 200 (not a redirect or error).

		FightFrame.showRequest( this );
	}

	public String getCommandForm()
	{	return "";
	}

	private void checkForNewEvents()
	{
		if ( responseText.indexOf( "bgcolor=orange><b>New Events:</b>") == -1 )
			return;

		// Capture the entire new events table in order to display the
		// appropriate message.

		Matcher eventMatcher = EVENT_PATTERN.matcher( responseText );
		if ( !eventMatcher.find() )
			return;

		// Make an array of events
		String [] events = eventMatcher.group(1).replaceAll( "<br>", "\n" ).split( "\n" );

		// Remove the events from the response text
		responseText = eventMatcher.replaceFirst( "" );

		for ( int i = 0; i < events.length; ++i )
		{
			String event = events[i];

			// The event may be marked up with color and links to
			// user profiles. For example:

			// 04/25/06 12:53:54 PM - New message received from <a target=mainpane href='showplayer.php?who=115875'><font color=green>Brianna</font></a>.
			// 04/25/06 01:06:43 PM - <a class=nounder target=mainpane href='showplayer.php?who=115875'><b><font color=green>Brianna</font></b></a> has played a song (The Polka of Plenty) for you.

			// Add in a player ID so that the events can be handled
			// using a ShowDescriptionList.

			event = event.replaceAll( "</a>", "<a>" ).replaceAll( "<[^a].*?>", "" );
			event = event.replaceAll( "<a[^>]*showplayer\\.php\\?who=(\\d+)[^>]*>(.*?)<a>", "$2 (#$1)" );
			event = event.replaceAll( "<.*?>", "" );

			// If it's a song or a buff, must update status

			// <name> has played a song (The Ode to Booze) for you
			// An Elemental Saucesphere has been conjured around you by <name>
			// <name> has imbued you with Reptilian Fortitude
			// <name> has given you the Tenacity of the Snapper
			// <name> has fortified you with Empathy of the Newt

			if ( event.indexOf( " has " ) != -1 )
				needsRefresh = true;

			// Add the event to the event list
			eventHistory.add( event );

			// Print everything to the default shell; this way, the
			// graphical CLI is also notified of events.

			KoLmafiaCLI.printLine( event );

			// Balloon messages for whenever the person does not have
			// focus on KoLmafia.

			if ( StaticEntity.usesSystemTray() )
				SystemTrayFrame.showBalloon( event );
		}

		// If we're not a GUI and there are no GUI windows open
		// (ie: the GUI loader command wasn't used), quit now.

		if ( existingFrames.isEmpty() )
			return;

		// If we are not running chat, pop up an EventsFrame to show
		// the events.  Use the standard run method so that you wait
		// for it to finish before calling it again on another event.

		if ( !KoLMessenger.isRunning() )
		{
			// Don't load the initial desktop frame unless it's
			// already visible before this is run -- this ensures
			// that the restore options are properly reset before
			// the frame reloads.

			boolean shouldLoadEventFrame = StaticEntity.getGlobalProperty( "initialFrames" ).indexOf( "EventsFrame" ) != -1;
			shouldLoadEventFrame |= StaticEntity.getGlobalProperty( "initialDesktop" ).indexOf( "EventsFrame" ) != -1 &&
				KoLDesktop.getInstance().isVisible();

			if ( shouldLoadEventFrame )
				SwingUtilities.invokeLater( new CreateFrameRunnable( EventsFrame.class ) );

			return;
		}

		// Copy the events to chat
		for ( int i = 0; i < events.length; ++i )
		{
			int dash = events[i].indexOf( "-" );
			String event = events[i].substring( dash + 2 );
			KoLMessenger.updateChat( "<font color=green>" + event + "</font>" );
		}
	}

	protected final void loadResponseFromFile( String filename )
	{
		try
		{
			BufferedReader buf = KoLDatabase.getReader( filename );
			String line;  StringBuffer response = new StringBuffer();

			while ( (line = buf.readLine()) != null )
				response.append( line );

			responseText = response.toString();
		}
		catch ( Exception e )
		{
			// This means simply that there was no file from which
			// to load the data.  Given that this is run during debug
			// tests, only, we can ignore the error.
		}
	}

	public String toString()
	{	return getURLString();
	}

	protected void printRequestProperties()
	{
		KoLmafia.getDebugStream().println();
		KoLmafia.getDebugStream().println( "Requesting: http://" + KOL_HOST + "/" + getURLString() );

		Map requestProperties = formConnection.getRequestProperties();
		KoLmafia.getDebugStream().println( requestProperties.size() + " request properties" );
		KoLmafia.getDebugStream().println();

		Iterator iterator = requestProperties.entrySet().iterator();
		while ( iterator.hasNext() )
		{
			Map.Entry entry = (Map.Entry)iterator.next();
			KoLmafia.getDebugStream().println( "Field: " + entry.getKey() + " = " + entry.getValue() );
		}

		KoLmafia.getDebugStream().println();
	}

	protected void printHeaderFields()
	{
		KoLmafia.getDebugStream().println();
		KoLmafia.getDebugStream().println( "Retrieved: http://" + KOL_HOST + "/" + getURLString() );
		KoLmafia.getDebugStream().println();

		Map headerFields = formConnection.getHeaderFields();
		KoLmafia.getDebugStream().println( headerFields.size() + " header fields" );

		Iterator iterator = headerFields.entrySet().iterator();
		while ( iterator.hasNext() )
		{
			Map.Entry entry = (Map.Entry)iterator.next();
			KoLmafia.getDebugStream().println( "Field: " + entry.getKey() + " = " + entry.getValue() );
		}

		KoLmafia.getDebugStream().println();
	}
}
