/**
 * Copyright (c) 2005-2007, KoLmafia development team
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
 *  [3] Neither the name "KoLmafia" nor the names of its contributors may
 *      be used to endorse or promote products derived from this software
 *      without specific prior written permission.
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

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.io.Writer;

import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.net.URLEncoder;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;

import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.SwingUtilities;

import net.sourceforge.foxtrot.Job;
import com.velocityreviews.forums.HttpTimeoutHandler;

public class KoLRequest extends Job implements KoLConstants
{
	private static final int INITIAL_CACHE_COUNT = 3;
	private static final Object WAIT_OBJECT = new Object();

	private static final ArrayList BYTEFLAGS = new ArrayList();
	private static final ArrayList BYTEARRAYS = new ArrayList();
	private static final ArrayList BYTESTREAMS = new ArrayList();

	static
	{
		for ( int i = 0; i < INITIAL_CACHE_COUNT; ++i )
			addAdditionalCache();
	}

	private static final AdventureResult MAIDEN_EFFECT = new AdventureResult( "Dreams and Lights", 1, true );
	private static final AdventureResult BALLROOM_KEY = new AdventureResult( 1766, 1 );

	private static final Pattern ORE_PATTERN = Pattern.compile( "3 chunks of (\\w+) ore" );
	private static final Pattern CHOICE_PATTERN = Pattern.compile( "whichchoice value=(\\d+)" );
	private static final Pattern CHOICE_DECISION_PATTERN = Pattern.compile( "whichchoice=(\\d+).*?option=(\\d+)" );
	private static final Pattern EVENT_PATTERN = Pattern.compile( "<table width=.*?<table><tr><td>(.*?)</td></tr></table>.*?<td height=4></td></tr></table>" );
	private static final Pattern STEEL_PATTERN = Pattern.compile( "emerge with a (.*?) of Steel" );

	public static final Pattern REDIRECT_PATTERN = Pattern.compile( "([^\\/]+)\\/login\\.php", Pattern.DOTALL );

	public static String sessionId = null;
	public static String passwordHash = null;
	private static boolean wasLastRequestSimple = false;

	public static boolean isRatQuest = false;
	public static boolean handlingChoices = false;

	public static int lastChoice = 0;
	public static int lastDecision = 0;

	public String encounter = "";
	private boolean shouldIgnoreResult;

	public static boolean isCompactMode = false;

	public static final String [][] SERVERS =
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

	public static String KOL_HOST = SERVERS[1][0];
	public static String KOL_ROOT = "http://" + SERVERS[1][1] + "/";

	public URL formURL;
	public boolean followRedirects;
	public String formURLString;

	public boolean isChatRequest = false;

	private List data;
	private boolean dataChanged = true;
	private byte [] dataString = null;

	public boolean needsRefresh;
	public boolean statusChanged;

	private boolean isDelayExempt;
	public int responseCode;
	public String responseText;
	public HttpURLConnection formConnection;
	public String redirectLocation;

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

	private static boolean substringMatches( String a, String b )
	{	return a.indexOf( b ) != -1 || b.indexOf( a ) != -1;
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
			if ( !substringMatches( server, SERVERS[i][0] ) && !substringMatches( server, SERVERS[i][1] ) )
				continue;

			KOL_HOST = SERVERS[i][0];
			KOL_ROOT = "http://" + SERVERS[i][1] + "/";

			StaticEntity.setProperty( "loginServerName", KOL_HOST );

			RequestLogger.printLine( "Redirected to " + KOL_HOST + "..." );
			System.setProperty( "http.referer", "http://" + KOL_HOST + "/main.php" );
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
	 * @param	formURLString	The form to be used in posting data
	 */

	public KoLRequest( String formURLString )
	{	this( formURLString, false );
	}

	/**
	 * Constructs a new KoLRequest which will notify the given client
	 * of any changes and will use the given URL for data submission,
	 * possibly following redirects if the parameter so specifies.
	 *
	 * @param	formURLString	The form to be used in posting data
	 * @param	followRedirects	<code>true</code> if redirects are to be followed
	 */

	public KoLRequest( String formURLString, boolean followRedirects )
	{
		this.data = new ArrayList();
		this.followRedirects = followRedirects;

		if ( formURLString.startsWith( "/" ) )
			formURLString = formURLString.substring(1);

		constructURLString( formURLString );

		this.isDelayExempt = getClass() == KoLRequest.class || this instanceof LoginRequest || this instanceof LogoutRequest ||
			this instanceof ChatRequest || this instanceof CharpaneRequest || this instanceof LocalRelayRequest;
	}

	public boolean isDelayExempt()
	{	return isDelayExempt;
	}

	public KoLRequest constructURLString( String newURLString )
	{
		return constructURLString( newURLString, true );
	}

	public KoLRequest constructURLString( String newURLString, boolean usePostMethod )
	{
		dataChanged = true;
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

		this.isChatRequest = this.formURLString.indexOf( "chat" ) != -1 && !this.formURLString.startsWith( "chatlaunch.php" ) &&
			!this.formURLString.startsWith( "lchat.php" ) && !this.formURLString.startsWith( "devchat.php" );

		this.shouldIgnoreResult = isChatRequest || formURLString.startsWith( "message" ) || formURLString.startsWith( "ascension" ) || formURLString.startsWith( "search" ) ||
			formURLString.startsWith( "static" ) || formURLString.startsWith( "desc" ) || formURLString.startsWith( "show" ) || formURLString.startsWith( "doc" ) ||
			(formURLString.startsWith( "clan" ) && !formURLString.startsWith( "clan_stash" ) && !formURLString.startsWith( "clan_rumpus" ));

		return this;
	}

	public boolean ignoreResult()
	{	return shouldIgnoreResult;
	}

	/**
	 * Returns the location of the form being used for this URL, in case
	 * it's ever needed/forgotten.
	 */

	public String getURLString()
	{	return data.isEmpty() ? formURLString : formURLString + "?" + getDataString( false );
	}

	/**
	 * Clears the data fields so that the descending class
	 * can have a fresh set of data fields.  This allows
	 * requests with variable numbers of parameters to be
	 * reused.
	 */

	public void clearDataFields()
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

	public void addFormField( String name, String value, boolean allowDuplicates )
	{
		dataChanged = true;

		if ( name.equals( "pwd" ) || name.equals( "phash" ) )
		{
			data.add( name );
			return;
		}

		if ( name.equals( "playerid" ) && value.equals( "" ) )
		{
			data.add( "playerid=" + KoLCharacter.getUserId() );
			return;
		}

		String encodedName = name == null ? "" : name;
		String encodedValue = value == null ? "" : value;

		try
		{
			encodedName = URLEncoder.encode( encodedName, "ISO-8859-1" ) + "=";
			encodedValue = URLEncoder.encode( encodedValue, "ISO-8859-1" );
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

	public void addFormField( String name, String value )
	{	addFormField( name, value, false );
	}

	/**
	 * Adds the given form field to the KoLRequest.
	 * @param	element	The field to be added
	 */

	public void addFormField( String element )
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

	public void addEncodedFormField( String element )
	{
		if ( element == null )
			return;

		if ( element.equals( "pwd=" ) )
			element = "pwd";
		else if ( element.equals( "phash=" ) )
			element = "phash";

		data.add( element );
	}

	public void addEncodedFormFields( String fields )
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

	public String getFormField( String key )
	{
		if ( data.isEmpty() )
			return null;

		for ( int i = 0; i < data.size(); ++i )
		{
			int splitIndex = ((String)data.get(i)).indexOf( "=" );
			if ( splitIndex == -1 )
				continue;

			String name = ((String)data.get(i)).substring( 0, splitIndex );
			if ( !name.equalsIgnoreCase( key ) )
				continue;

			String value = ((String)data.get(i)).substring( splitIndex + 1 ) ;

			try
			{
				// Everything was encoded as ISO-8859-1, so go
				// ahead and decode it that way.

				return URLDecoder.decode( value, "ISO-8859-1" );
			}
			catch ( Exception e )
			{
				// This shouldn't happen, but since you did
				// manage to find the key, return the value.

				return value;
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

		return dataBuffer.toString();
	}

	private boolean shouldUpdateDebugLog()
	{	return RequestLogger.isDebugging() && !isChatRequest;
	}

	/**
	 * Runs the thread, which prepares the connection for output, posts the data
	 * to the Kingdom of Loathing, and prepares the input for reading.  Because
	 * the Kingdom of Loathing has identical page layouts, all page reading and
	 * handling will occur through these method calls.
	 */

	public void run()
	{
		if ( sessionId == null && !(this instanceof LoginRequest || this instanceof LogoutRequest || this instanceof LocalRelayRequest) )
			return;

		String location = getURLString();

		if ( shouldUpdateDebugLog() )
			RequestLogger.updateDebugLog( getClass() );

		if ( location.startsWith( "sewer.php" ) )
		{
			if ( StaticEntity.getBooleanProperty( "relayAlwaysBuysGum" ) )
				DEFAULT_SHELL.executeLine( "acquire chewing gum on a string" );
		}
		else if ( location.startsWith( "hermit.php?autopermit=on" ) )
		{
			DEFAULT_SHELL.executeLine( "acquire hermit permit" );
		}

		else if ( location.startsWith( "casino.php" ) )
		{
			DEFAULT_SHELL.executeLine( "acquire casino pass" );
		}

		// To avoid wasting turns, buy a can of hair spray before
		// climbing the tower.  Also, if the person has an NG,
		// make sure to construct it first.  If there are any
		// tower items sitting in the closet or that have not
		// been constructed, pull them out.

		if ( location.startsWith( "lair4.php" ) || location.startsWith( "lair5.php" ) )
			SorceressLair.makeGuardianItems();

		needsRefresh = false;
		execute();

		if ( responseCode != 200 )
			return;

		// When following redirects, you will get different URL
		// strings, so make sure you update.

		if ( followRedirects )
			location = getURLString();

		// If this is the trapper page, make sure to check to
		// see if there's any changes to your inventory.

		if ( location.startsWith( "trapper.php" ) )
		{
			Matcher oreMatcher = ORE_PATTERN.matcher( responseText );
			if ( oreMatcher.find() )
				StaticEntity.setProperty( "trapperOre", oreMatcher.group(1) + " ore" );

			// If you receive items from the trapper, then you
			// lose some items already in your inventory.

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

		// The Deep Fat Friars' Ceremony Location

		if ( location.startsWith( "friars.php" ) )
		{
			// "Thank you, Adventurer."

			if ( responseText.indexOf( "Thank you" ) != -1 )
			{
				StaticEntity.getClient().processResult( AdventureRequest.DODECAGRAM );
				StaticEntity.getClient().processResult( AdventureRequest.CANDLES );
				StaticEntity.getClient().processResult( AdventureRequest.BUTTERKNIFE );

				if ( this instanceof AdventureRequest )
					KoLmafia.updateDisplay( PENDING_STATE, "Taint cleansed." );

				Matcher learnedMatcher = STEEL_PATTERN.matcher( responseText );
				if ( learnedMatcher.find() )
					KoLCharacter.addAvailableSkill( UseSkillRequest.getInstance( learnedMatcher.group(1) + " of Steel" ) );

				statusChanged = true;
			}
		}

		// There are requests to the council which will also
		// decrement your inventory.

		if ( location.startsWith( "council.php" ) )
		{
			if ( responseText.indexOf( "500" ) != -1 )
				StaticEntity.getClient().processResult( new AdventureResult( "mosquito larva", -1, false ) );
			if ( responseText.indexOf( "batskin belt" ) != -1 )
				StaticEntity.getClient().processResult( new AdventureResult( "Boss Bat bandana", -1, false ) );
		}

		// The white citadel quest will also decrement your
		// inventory once.

		if ( KoLCharacter.hasItem( KoLmafia.SATCHEL ) && location.startsWith( "guild.php" ) && location.indexOf( "place=paco" ) != -1 )
			StaticEntity.getClient().processResult( KoLmafia.SATCHEL.getNegation() );

		// On SSPD you can trade a button for a glowstick

		if ( KoLCharacter.hasEquipped( KoLmafia.NOVELTY_BUTTON ) &&
			responseText.indexOf( "You hand him your button and take his glowstick" ) != -1 )
		{
			String name = KoLmafia.NOVELTY_BUTTON.getName();
			if ( KoLCharacter.hasEquipped( name, KoLCharacter.ACCESSORY1 ) )
				KoLCharacter.setEquipment( KoLCharacter.ACCESSORY1, EquipmentRequest.UNEQUIP );
			else if ( KoLCharacter.hasEquipped( name, KoLCharacter.ACCESSORY2 ) )
				KoLCharacter.setEquipment( KoLCharacter.ACCESSORY2, EquipmentRequest.UNEQUIP );
			else
				KoLCharacter.setEquipment( KoLCharacter.ACCESSORY3, EquipmentRequest.UNEQUIP );

			// Maintain session tally: "unequip" the button and
			// discard it.
			AdventureResult.addResultToList( inventory, KoLmafia.NOVELTY_BUTTON );
			StaticEntity.getClient().processResult( KoLmafia.NOVELTY_BUTTON.getNegation() );
		}

		// If this is an ascension, make sure to refresh the
		// session, be it relay or mini-browser.

		if ( location.equals( "main.php?refreshtop=true&noobmessage=true" ) )
			StaticEntity.getClient().handleAscension();

		// Once everything is complete, decide whether or not
		// you should refresh your status.

		if ( needsRefresh || statusChanged )
		{
			if ( RequestFrame.instanceExists() )
				RequestFrame.refreshStatus();
			else
				CharpaneRequest.getInstance().run();
		}
		else if ( formURLString.startsWith( "charpane.php" ) )
		{
			KoLCharacter.recalculateAdjustments();
			KoLCharacter.updateStatus();
		}
		else if ( !shouldIgnoreResult )
		{
			KoLCharacter.updateStatus();
		}
	}

	public void execute()
	{
		// If this is the rat quest, then go ahead and pre-set the data
		// to reflect a fight sequence (mini-browser compatibility).

		String urlString = getURLString();

		isRatQuest |= urlString.startsWith( "rats.php" );
		if ( !isChatRequest && !urlString.startsWith( "charpane.php" ) && !urlString.startsWith( "rats.php" ) )
			isRatQuest &= urlString.startsWith( "fight.php" );

		if ( isRatQuest )
			KoLmafia.addTavernLocation( this );

		if ( !shouldIgnoreResult )
			RequestLogger.registerRequest( this, urlString );

		if ( urlString.startsWith( "choice.php" ) )
			saveLastChoice( urlString );

		if ( !isDelayExempt )
			StaticEntity.getClient().setCurrentRequest( this );

		// If you're about to fight the Naughty Sorceress,
		// clear your list of effects.

		if ( urlString.startsWith( "lair6.php" ) && urlString.indexOf( "place=5" ) != -1 )
		{
			activeEffects.clear();
			needsRefresh = true;
		}

		if ( urlString.startsWith( "lair6.php" ) && urlString.indexOf( "place=6" ) != -1 )
		{
			KoLCharacter.setHardcore( false );
			KoLCharacter.setConsumptionRestriction( AscensionSnapshotTable.NOPATH );
			needsRefresh = true;
		}

		if ( urlString.startsWith( "ascend.php" ) )
		{
			if ( KoLCharacter.hasItem( KoLAdventure.MEATCAR ) )
				(new UntinkerRequest( KoLAdventure.MEATCAR.getItemId() )).run();

			ItemCreationRequest belt = ItemCreationRequest.getInstance( 677 );
			if ( belt != null && belt.getQuantityPossible() > 0 )
			{
				belt.setQuantityNeeded( belt.getQuantityPossible() );
				belt.run();
			}
		}

		statusChanged = false;

		do
		{
			if ( !prepareConnection() && KoLmafia.refusesContinue() )
				break;
		}
		while ( !postClientData() || !retrieveServerReply() );
	}

	private void saveLastChoice( String url )
	{
		Matcher choiceMatcher = CHOICE_DECISION_PATTERN.matcher( url );
		if ( choiceMatcher.find() )
		{
			lastChoice = StaticEntity.parseInt( choiceMatcher.group(1) );
			lastDecision = StaticEntity.parseInt( choiceMatcher.group(2) );

			switch ( lastChoice )
			{
			// Strung-Up Quartet
			case 106:

				if ( lastDecision != 4 )
				{
					StaticEntity.setProperty( "lastQuartetAscension", String.valueOf( KoLCharacter.getAscensions() ) );
					StaticEntity.setProperty( "lastQuartetRequest", String.valueOf( lastDecision ) );

					if ( KoLCharacter.recalculateAdjustments() )
						KoLCharacter.updateStatus();
				}

				break;

			// Wheel In the Sky Keep on Turning: Muscle Position
			case 9:
				StaticEntity.setProperty( "currentWheelPosition",
					String.valueOf( lastDecision == 1 ? "mysticality" : lastDecision == 2 ? "moxie" : "muscle" ) );
				break;

			// Wheel In the Sky Keep on Turning: Mysticality Position
			case 10:
				StaticEntity.setProperty( "currentWheelPosition",
					String.valueOf( lastDecision == 1 ? "map quest" : lastDecision == 2 ? "muscle" : "mysticality" ) );
				break;

			// Wheel In the Sky Keep on Turning: Map Quest Position
			case 11:
				StaticEntity.setProperty( "currentWheelPosition",
					String.valueOf( lastDecision == 1 ? "moxie" : lastDecision == 2 ? "mysticality" : "map quest" ) );
				break;

			// Wheel In the Sky Keep on Turning: Moxie Position
			case 12:
				StaticEntity.setProperty( "currentWheelPosition",
					String.valueOf( lastDecision == 1 ? "muscle" : lastDecision == 2 ? "map quest" : "moxie" ) );
				break;
			}
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

	public static boolean shouldIgnore( String formURLString )
	{	return formURLString.indexOf( "mall" ) != -1 || formURLString.indexOf( "chat" ) != -1;
	}

	/**
	 * Utility method which waits for the given duration without
	 * using Thread.sleep() - this means CPU usage can be greatly
	 * reduced.
	 */

	public static boolean delay( long milliseconds )
	{
		if ( milliseconds == 0 )
			return true;

		try
		{
			synchronized ( WAIT_OBJECT )
			{
				WAIT_OBJECT.wait( milliseconds );
				WAIT_OBJECT.notifyAll();
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
		if ( shouldUpdateDebugLog() )
			RequestLogger.updateDebugLog( "Connecting to " + formURLString + "..." );

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

			this.formURL = null;

			if ( StaticEntity.getBooleanProperty( "testSocketTimeout" ) )
			{
				if ( this.formURLString.startsWith( "http:" ) )
					this.formURL = new URL( null, this.formURLString, HttpTimeoutHandler.getInstance() );
				else
					this.formURL = new URL( null, KOL_ROOT + this.formURLString, HttpTimeoutHandler.getInstance() );
			}
			else
			{
				if ( this.formURLString.startsWith( "http:" ) )
					this.formURL = new URL( this.formURLString );
				else
					this.formURL = new URL( KOL_ROOT + this.formURLString );
			}

			this.formConnection = (HttpURLConnection) formURL.openConnection();
		}
		catch ( Exception e )
		{
			// In the event that an Exception is thrown, one can assume
			// that there was a timeout; return false and let the loop
			// attempt to connect again

			if ( shouldUpdateDebugLog() )
				RequestLogger.updateDebugLog( "Error opening connection.  Retrying..." );

			if ( this instanceof LoginRequest )
				chooseNewLoginServer();

			return false;
		}

		formConnection.setDoInput( true );
		formConnection.setDoOutput( !data.isEmpty() );
		formConnection.setUseCaches( false );
		formConnection.setInstanceFollowRedirects( false );

		if ( sessionId != null )
		{
			if ( formURLString.startsWith( "inventory.php" ) && !StaticEntity.getProperty( "visibleBrowserInventory" ).equals( "" ) )
				formConnection.addRequestProperty( "Cookie", StaticEntity.getProperty( "visibleBrowserInventory" ) + "; " + sessionId );
			else
				formConnection.addRequestProperty( "Cookie", sessionId );
		}

		formConnection.setRequestProperty( "User-Agent", VERSION_NAME );

		if ( dataChanged )
		{
			dataChanged = false;
			dataString = getDataString( true ).getBytes();
		}

		formConnection.setRequestProperty( "Content-Type", "application/x-www-form-urlencoded" );

		if ( !data.isEmpty() )
			formConnection.setRequestProperty( "Content-Length", String.valueOf( dataString.length ) );
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
		if ( shouldUpdateDebugLog() )
			printRequestProperties();

		// Only attempt to post something if there's actually
		// data to post - otherwise, opening an input stream
		// should be enough

		if ( data.isEmpty() )
			return true;

		try
		{
			formConnection.setRequestMethod( "POST" );

			if ( shouldUpdateDebugLog() )
				printRequestProperties();

			OutputStream ostream = formConnection.getOutputStream();
			ostream.write( dataString );

			ostream.flush();
			ostream.close();

			ostream = null;
			return true;
		}
		catch ( Exception e )
		{
			if ( shouldUpdateDebugLog() )
				RequestLogger.updateDebugLog( "Connection timed out during post.  Retrying..." );

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

		if ( shouldUpdateDebugLog() )
			RequestLogger.updateDebugLog( "Retrieving server reply..." );

		responseText = "";
		redirectLocation = "";

		try
		{
			istream = formConnection.getInputStream();
			responseCode = formConnection.getResponseCode();
			redirectLocation = formConnection.getHeaderField( "Location" );
		}
		catch ( Exception e1 )
		{
			if ( shouldUpdateDebugLog() )
				RequestLogger.updateDebugLog( "Connection timed out during response.  Retrying..." );

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

		if ( shouldUpdateDebugLog() )
			printHeaderFields();

		boolean shouldStop = false;

		try
		{
			if ( responseCode == 200 )
			{
				shouldStop = retrieveServerReply( istream );
				istream.close();
			}
			else
			{
				// If the response code is not 200, then you've read all
				// the information you need.  Close the input stream.

				istream.close();
				shouldStop = responseCode == 302 ? handleServerRedirect() : true;
			}
		}
		catch ( Exception e )
		{
			// Do nothing, you're going to close the input stream
			// and nullify it in the next section.

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

		int lastSlashIndex = redirectLocation.lastIndexOf( "/" );

		if ( lastSlashIndex != -1 )
			redirectLocation = redirectLocation.substring( lastSlashIndex + 1 );

		if ( matcher.find() )
		{
			setLoginServer( matcher.group(1) );
			constructURLString( redirectLocation, false );
			return false;
		}

		if ( sessionId == null && redirectLocation.startsWith( "login.php" ) )
		{
			constructURLString( redirectLocation, false );
			return false;
		}

		if ( this instanceof LocalRelayRequest )
		{
			if ( formURLString.startsWith( "login.php" ) )
				LoginRequest.processLoginRequest( this );

			return true;
		}

		if ( formURLString.startsWith( "fight.php" ) )
		{
			FightRequest.updateCombatData( encounter, "" );
			return true;
		}

		if ( followRedirects )
		{
			// Re-setup this request to follow the redirect
			// desired and rerun the request.

			constructURLString( redirectLocation, false );
			return false;
		}

		if ( redirectLocation.startsWith( "fight.php" ) )
		{
			if ( this instanceof UseSkillRequest || this instanceof ConsumeItemRequest )
				return true;

			// You have been redirected to a fight!  Here, you need
			// to complete the fight before you can continue.

			FightRequest.INSTANCE.run();
			return this instanceof AdventureRequest;
		}

		if ( redirectLocation.startsWith( "login.php" ) )
		{
			LoginRequest.executeTimeInRequest();
			return sessionId == null;
		}

		if ( redirectLocation.startsWith( "maint.php" ) )
		{
			// If the system is down for maintenance, the user must be
			// notified that they should try again later.

			KoLmafia.updateDisplay( ABORT_STATE, "Nightly maintenance." );
			return true;
		}

		if ( redirectLocation.startsWith( "choice.php" ) )
		{
			handlingChoices = true;
			processChoiceAdventure();
			handlingChoices = false;

			return true;
		}

		if ( redirectLocation.startsWith( "valhalla.php" ) )
		{
			passwordHash = "";
			return true;
		}

		if ( shouldUpdateDebugLog() )
			RequestLogger.updateDebugLog( "Redirected: " + redirectLocation );

		return true;
	}

	private static void addAdditionalCache()
	{
		synchronized ( BYTEFLAGS )
		{
			BYTEFLAGS.add( Boolean.TRUE );
			BYTEARRAYS.add( new byte[ 8096 ] );
			BYTESTREAMS.add( new ByteArrayOutputStream( 8096 ) );
		}
	}

	private boolean retrieveServerReply( InputStream istream ) throws Exception
	{
		// Find an available byte array in order to buffer the data.  Allow
		// this to scale based on the number of incoming requests in order
		// to reduce the probability that the program hangs.

		int desiredIndex = -1;

		synchronized ( BYTEFLAGS )
		{
			for ( int i = 0; desiredIndex == -1 && i < BYTEFLAGS.size(); ++i )
				if ( BYTEFLAGS.get(i) == Boolean.FALSE )
					desiredIndex = i;
		}

		if ( desiredIndex == -1 )
		{
			desiredIndex = BYTEFLAGS.size();
			addAdditionalCache();
		}
		else
		{
			BYTEFLAGS.set( desiredIndex, Boolean.TRUE );
		}

		// Read all the data into the static byte array output stream and then
		// convert that string to UTF-8.

		byte [] array = (byte []) BYTEARRAYS.get( desiredIndex );
		ByteArrayOutputStream stream = (ByteArrayOutputStream) BYTESTREAMS.get( desiredIndex );

		int availableBytes = 0;
		while ( (availableBytes = istream.read( array )) != -1 )
			stream.write( array, 0, availableBytes );

		this.responseText = stream.toString( "UTF-8" );
		stream.reset();

		// You are now done with the array.  Go ahead and reset the value
		// to false to let the program know the objects are available to
		// be reused.

		BYTEFLAGS.set( desiredIndex, Boolean.FALSE );
		processResponse();

		return true;
	}

	/**
	 * This method allows classes to process a raw, unfiltered
	 * server response.
	 */

	public void processResponse()
	{
		if ( responseText == null )
			return;

		if ( shouldUpdateDebugLog() )
			RequestLogger.updateDebugLog( LINE_BREAK_PATTERN.matcher( responseText ).replaceAll( "" ) );

		statusChanged = responseText.indexOf( "charpane.php" ) != -1;
		if ( statusChanged && !(this instanceof LocalRelayRequest) )
			LocalRelayServer.addStatusMessage( "<!-- REFRESH -->" );

		if ( !isDelayExempt )
			checkForNewEvents();

		if ( isRatQuest )
			KoLmafia.addTavernLocation( this );

		encounter = AdventureRequest.registerEncounter( this );

		if ( formURLString.equals( "fight.php" ) )
			FightRequest.updateCombatData( encounter, responseText );

		if ( !shouldIgnoreResult )
			parseResults();

		if ( !LoginRequest.isInstanceRunning() && !(this instanceof LocalRelayRequest) && !(this instanceof CharpaneRequest) && !isChatRequest && formURLString.indexOf( "search" ) == -1 )
			showInBrowser( false );

		// Now let the main method of result processing for
		// each request type happen.

		processResults();

		// Let the mappers do their work
		mapCurrentChoice( responseText );

		if ( AdventureRequest.useMarmotClover( formURLString, responseText ) || HermitRequest.useHermitClover( formURLString ) )
			DEFAULT_SHELL.executeLine( "use * ten-leaf clover" );

		needsRefresh &= !(getClass() == KoLRequest.class || this instanceof LocalRelayRequest || this instanceof FightRequest);
		needsRefresh &= !formURLString.startsWith( "charpane.php" );

		statusChanged &= !formURLString.startsWith( "charpane.php" );
		KoLmafia.applyEffects();
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

	public static final void skipTokens( StringTokenizer st, int tokenCount )
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

	public static final int intToken( StringTokenizer st )
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

	public static final int intToken( StringTokenizer st, int fromStart )
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

	public static final int intToken( StringTokenizer st, int fromStart, int fromEnd )
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
	{	return formURLString.startsWith( "choice.php" ) ? 1 : 0;
	}

	public void parseResults()
	{
		// If this is a lucky adventure, then remove a clover
		// from the player's inventory -- this will occur when
		// you see either "Your ten-leaf clover" or "your
		// ten-leaf clover" (shorten to "our ten-leaf clover"
		// for substring matching)

		if ( responseText.indexOf( "our ten-leaf clover" ) != -1 && responseText.indexOf( "puff of smoke" ) != -1 )
			StaticEntity.getClient().processResult( SewerRequest.CLOVER );

		if ( formURLString.startsWith( "sewer.php" ) && responseText.indexOf( "You acquire" ) != -1 )
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
			else if ( KoLCharacter.getFamiliar().getId() != 50 )
				needsRefresh |= StaticEntity.getClient().processResult( KoLAdventure.BEATEN_UP.getInstance( 3 - KoLAdventure.BEATEN_UP.getCount( activeEffects ) ) );
		}
	}

	public void processResults()
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

		StaticEntity.getClient().processResult( new AdventureResult( AdventureResult.CHOICE, 1 ) );

		KoLRequest request = new KoLRequest( redirectLocation );
		request.run();

		String choice = null;
		String option = null;
		String decision = null;

		while ( request.responseText.indexOf( "choice.php" ) != -1 )
		{
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

			// If the player is looking for the ballroom key,
			// then update their preferences so that KoLmafia
			// automatically switches things for them.

			if ( choice.equals( "85" ) && conditions.contains( BALLROOM_KEY ) )
				StaticEntity.setProperty( option, decision.equals( "1" ) ? "2" : "1" );

			// Certain choices should always be taken.  These
			// choices are handled here.

			else if ( choice.equals( "7" ) )
			{
				decision = "1";
			}

			// Sometimes, the choice adventure for the louvre
			// loses track of whether to ignore the louvre or not.

			else if ( choice.equals( "91" ) )
			{
				decision = StaticEntity.getIntegerProperty( "louvreDesiredGoal" ) != 0 ? "1" : "2";
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

				StaticEntity.printRequestData( request );
				return;
			}

			// If the user wants to ignore this specific choice or all
			// choices, see if this choice is ignorable.

			boolean willIgnore = false;

			// But first, handle the maidens adventure in a less random
			// fashion that's actually useful.

			if ( choice.equals( "89" ) )
			{
				willIgnore = true;

				switch ( StaticEntity.parseInt( decision ) )
				{
				case 0:
					decision = String.valueOf( RNG.nextInt(2) + 1 );
					break;
				case 1:
				case 2:
					break;
				case 3:
					decision = activeEffects.contains( MAIDEN_EFFECT ) ? String.valueOf( RNG.nextInt(2) + 1 ) : "3";
					break;
				case 4:
					decision = activeEffects.contains( MAIDEN_EFFECT ) ? "1" : "3";
					break;
				case 5:
					decision = activeEffects.contains( MAIDEN_EFFECT ) ? "2" : "3";
					break;
				}
			}
			else if ( decision.equals( "0" ) )
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

		// Choose a null choice if no conditions match what you're
		// trying to look for.

		for ( int i = 0; i < possibleDecisions.length; ++i )
			if ( possibleDecisions[i] == null )
				return String.valueOf( i + 1 );

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

	public void showInBrowser( boolean exceptional )
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
		boolean shouldLoadEventFrame = false;

		for ( int i = 0; i < events.length; ++i )
		{
			if ( events[i].indexOf( "logged" ) != -1 )
				continue;

			shouldLoadEventFrame = true;
			String event = events[i];

			// The event may be marked up with color and links to
			// user profiles. For example:

			// 04/25/06 12:53:54 PM - New message received from <a target=mainpane href='showplayer.php?who=115875'><font color=green>Brianna</font></a>.
			// 04/25/06 01:06:43 PM - <a class=nounder target=mainpane href='showplayer.php?who=115875'><b><font color=green>Brianna</font></b></a> has played a song (The Polka of Plenty) for you.

			// Add in a player Id so that the events can be handled
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

			RequestLogger.printLine( event );

			// Balloon messages for whenever the person does not have
			// focus on KoLmafia.

			if ( StaticEntity.usesSystemTray() )
				SystemTrayFrame.showBalloon( event );
		}

		shouldLoadEventFrame &= StaticEntity.getGlobalProperty( "initialFrames" ).indexOf( "EventsFrame" ) != -1;

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

	public final void loadResponseFromFile( String filename )
	{
		try
		{
			BufferedReader buf = KoLDatabase.getReader( filename );
			String line;  StringBuffer response = new StringBuffer();

			while ( (line = buf.readLine()) != null )
				response.append( line );

			responseText = response.toString();
			processResponse();
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

	public void printRequestProperties()
	{
		RequestLogger.updateDebugLog();
		RequestLogger.updateDebugLog( "Requesting: http://" + KOL_HOST + "/" + getURLString() );

		Map requestProperties = formConnection.getRequestProperties();
		RequestLogger.updateDebugLog( requestProperties.size() + " request properties" );
		RequestLogger.updateDebugLog();

		Iterator iterator = requestProperties.entrySet().iterator();
		while ( iterator.hasNext() )
		{
			Entry entry = (Entry)iterator.next();
			RequestLogger.updateDebugLog( "Field: " + entry.getKey() + " = " + entry.getValue() );
		}

		RequestLogger.updateDebugLog();
	}

	public void printHeaderFields()
	{
		RequestLogger.updateDebugLog();
		RequestLogger.updateDebugLog( "Retrieved: http://" + KOL_HOST + "/" + getURLString() );
		RequestLogger.updateDebugLog();

		Map headerFields = formConnection.getHeaderFields();
		RequestLogger.updateDebugLog( headerFields.size() + " header fields" );

		Iterator iterator = headerFields.entrySet().iterator();
		while ( iterator.hasNext() )
		{
			Entry entry = (Entry)iterator.next();
			RequestLogger.updateDebugLog( "Field: " + entry.getKey() + " = " + entry.getValue() );
		}

		RequestLogger.updateDebugLog();
	}
}
