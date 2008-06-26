/**
 * Copyright (c) 2005-2008, KoLmafia development team
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

package net.sourceforge.kolmafia.request;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.SwingUtilities;

import net.sourceforge.foxtrot.Job;
import net.sourceforge.kolmafia.CreateFrameRunnable;
import net.sourceforge.kolmafia.KoLAdventure;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.KoLmafiaCLI;
import net.sourceforge.kolmafia.LocalRelayServer;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.StaticEntity;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.AscensionSnapshot;
import net.sourceforge.kolmafia.persistence.Preferences;
import net.sourceforge.kolmafia.session.ChatManager;
import net.sourceforge.kolmafia.session.ChoiceManager;
import net.sourceforge.kolmafia.session.ClanManager;
import net.sourceforge.kolmafia.session.InventoryManager;
import net.sourceforge.kolmafia.session.LouvreManager;
import net.sourceforge.kolmafia.session.OceanManager;
import net.sourceforge.kolmafia.session.ResultProcessor;
import net.sourceforge.kolmafia.session.SorceressLairManager;
import net.sourceforge.kolmafia.session.TurnCounter;
import net.sourceforge.kolmafia.session.ValhallaManager;
import net.sourceforge.kolmafia.session.VioletFogManager;
import net.sourceforge.kolmafia.swingui.CouncilFrame;
import net.sourceforge.kolmafia.swingui.RecentEventsFrame;
import net.sourceforge.kolmafia.swingui.RequestFrame;
import net.sourceforge.kolmafia.swingui.RequestSynchFrame;
import net.sourceforge.kolmafia.swingui.SystemTrayFrame;
import net.sourceforge.kolmafia.utilities.FileUtilities;
import net.sourceforge.kolmafia.utilities.StringUtilities;

import com.velocityreviews.forums.HttpTimeoutHandler;

public class GenericRequest
	extends Job
{
	private int timeoutCount = 0;
	private static final int TIMEOUT_LIMIT = 3;

	private static final int INITIAL_CACHE_COUNT = 3;

	private static final ArrayList BYTEFLAGS = new ArrayList();
	private static final ArrayList BYTEARRAYS = new ArrayList();
	private static final ArrayList BYTESTREAMS = new ArrayList();

	static
	{
		for ( int i = 0; i < GenericRequest.INITIAL_CACHE_COUNT; ++i )
		{
			GenericRequest.addAdditionalCache();
		}
	}

	private static final Pattern EVENT_PATTERN =
		Pattern.compile( "bgcolor=orange><b>New Events:</b></td></tr><tr><td style=\"padding: 5px; border: 1px solid orange;\"><center><table><tr><td>(.*?)</td></tr></table>.*?<td height=4></td></tr></table>" );

	public static final Pattern REDIRECT_PATTERN = Pattern.compile( "([^\\/]+)\\/login\\.php", Pattern.DOTALL );

	public static String inventoryCookie = null;
	public static String serverCookie = null;

	public static String passwordHash = "";
	public static boolean isRatQuest = false;
	public static boolean handlingChoices = false;

	public static int lastChoice = 0;
	public static int lastDecision = 0;

	protected String encounter = "";
	public static boolean isCompactMode = false;

	public static final String[][] SERVERS =
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

	public static String KOL_HOST = GenericRequest.SERVERS[ 1 ][ 0 ];
	public static URL KOL_ROOT = null;

	private URL formURL;
	private String currentHost;
	private String formURLString;

	public boolean isChatRequest = false;

	protected List data;
	private boolean dataChanged = true;
	private byte[] dataString = null;

	private boolean hasNoResult;
	public boolean containsUpdate;

	public int responseCode;
	public String responseText;
	public HttpURLConnection formConnection;
	public String redirectLocation;

	/**
	 * static final method called when <code>GenericRequest</code> is first instantiated or whenever the settings have
	 * changed. This initializes the login server to the one stored in the user's settings, as well as initializes the
	 * user's proxy settings.
	 */

	public static final void applySettings()
	{
		GenericRequest.applyProxySettings();

		int defaultLoginServer = Preferences.getInteger( "defaultLoginServer" );
		if ( defaultLoginServer >= GenericRequest.SERVERS.length )
		{
			defaultLoginServer = 0;
		}

		GenericRequest.setLoginServer( GenericRequest.SERVERS[ defaultLoginServer ][ 0 ] );
	}

	private static final void applyProxySettings()
	{
		if ( System.getProperty( "os.name" ).startsWith( "Mac" ) )
		{
			return;
		}

		String proxySet = Preferences.getString( "proxySet" );
		String proxyHost = Preferences.getString( "http.proxyHost" );
		String proxyUser = Preferences.getString( "http.proxyUser" );

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

			System.setProperty( "http.proxyPort", Preferences.getString( "http.proxyPort" ) );
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
			System.setProperty( "http.proxyUser", Preferences.getString( "http.proxyUser" ) );
			System.setProperty( "http.proxyPassword", Preferences.getString( "http.proxyPassword" ) );
		}
	}

	private static final boolean substringMatches( final String a, final String b )
	{
		return a.indexOf( b ) != -1 || b.indexOf( a ) != -1;
	}

	/**
	 * static final method used to manually set the server to be used as the root for all requests by all KoLmafia
	 * clients running on the current JVM instance.
	 *
	 * @param server The hostname of the server to be used.
	 */

	public static final void setLoginServer( final String server )
	{
		if ( server == null )
		{
			return;
		}

		for ( int i = 0; i < GenericRequest.SERVERS.length; ++i )
		{
			if ( GenericRequest.substringMatches( server, GenericRequest.SERVERS[ i ][ 0 ] ) || GenericRequest.substringMatches(
				server, GenericRequest.SERVERS[ i ][ 1 ] ) )
			{
				GenericRequest.setLoginServer( i );
			}
		}
	}

	private static final void setLoginServer( final int serverIndex )
	{
		GenericRequest.KOL_HOST = GenericRequest.SERVERS[ serverIndex ][ 0 ];

		try
		{
			GenericRequest.KOL_ROOT = new URL( "http", GenericRequest.SERVERS[ serverIndex ][ 1 ], 80, "/" );
		}
		catch ( IOException e )
		{
			StaticEntity.printStackTrace( e );
		}

		Preferences.setString( "loginServerName", GenericRequest.KOL_HOST );
		System.setProperty( "http.referer", "http://" + GenericRequest.KOL_HOST + "/main.php" );
	}

	private static int retryServer = 0;

	private static final void chooseNewLoginServer()
	{
		KoLmafia.updateDisplay( "Choosing new login server..." );
		LoginRequest.setIgnoreLoadBalancer( true );

		GenericRequest.retryServer = Math.max( 1, ( GenericRequest.retryServer + 1 ) % GenericRequest.SERVERS.length );
		GenericRequest.setLoginServer( GenericRequest.retryServer );
	}

	/**
	 * static final method used to return the server currently used by this KoLmafia session.
	 *
	 * @return The host name for the current server
	 */

	public static final String getRootHostName()
	{
		return GenericRequest.KOL_HOST;
	}

	/**
	 * Constructs a new GenericRequest which will notify the given client of any changes and will use the given URL for data
	 * submission.
	 *
	 * @param formURLString The form to be used in posting data
	 */

	public GenericRequest( final String newURLString )
	{
		this.data = new ArrayList();
		if ( !newURLString.equals( "" ) )
		{
			this.constructURLString( newURLString );
		}
	}

	public boolean hasNoResult()
	{
		return this.hasNoResult;
	}

	public GenericRequest constructURLString( final String newURLString )
	{
		return this.constructURLString( newURLString, true );
	}

	public GenericRequest constructURLString( String newURLString, boolean usePostMethod )
	{
		if ( this.formURLString == null || !newURLString.startsWith( this.formURLString ) )
		{
			this.currentHost = GenericRequest.KOL_HOST;
			this.formURL = null;
		}

		this.responseText = null;
		this.dataChanged = true;

		this.data.clear();

		if ( newURLString.startsWith( "/" ) )
		{
			newURLString = newURLString.substring( 1 );
		}

		int formSplitIndex = newURLString.indexOf( "?" );

		if ( formSplitIndex == -1 || !usePostMethod )
		{
			this.formURLString = newURLString;
		}
		else
		{
			this.formURLString = newURLString.substring( 0, formSplitIndex );
			this.addEncodedFormFields( newURLString.substring( formSplitIndex + 1 ) );
		}

		this.isChatRequest =
			this.formURLString.equals( "newchatmessages.php" ) || this.formURLString.equals( "submitnewchat.php" );

		this.hasNoResult =
			this.isChatRequest || this.formURLString.startsWith( "http://" ) || this.formURLString.startsWith( "charpane" ) ||
			this.formURLString.startsWith( "quest" ) || this.formURLString.endsWith( "menu.php" ) || this.formURLString.startsWith( "actionbar" ) ||
			this.formURLString.startsWith( "desc" ) || this.formURLString.startsWith( "display" ) || this.formURLString.startsWith( "search" ) ||
			this.formURLString.startsWith( "show" ) || this.formURLString.startsWith( "search" ) || this.formURLString.startsWith( "valhalla" ) ||
			this.formURLString.startsWith( "message" ) || this.formURLString.startsWith( "makeoffer" ) ||
			(this.formURLString.startsWith( "clan" ) && !this.formURLString.startsWith( "clan_stash" ) && !this.formURLString.startsWith( "clan_rumpus" ) ) ||
			this instanceof LoginRequest || this instanceof LogoutRequest;

		return this;
	}

	/**
	 * Returns the location of the form being used for this URL, in case it's ever needed/forgotten.
	 */

	public String getURLString()
	{
		return this.data.isEmpty() ? this.formURLString : this.formURLString + "?" + this.getDataString( false );
	}

	/**
	 * Clears the data fields so that the descending class can have a fresh set of data fields. This allows requests
	 * with variable numbers of parameters to be reused.
	 */

	public void clearDataFields()
	{
		this.data.clear();
	}

	/**
	 * Adds the given form field to the GenericRequest. Descendant classes should use this method if they plan on submitting
	 * forms to Kingdom of Loathing before a call to the <code>super.run()</code> method. Ideally, these fields can be
	 * added at construction time.
	 *
	 * @param name The name of the field to be added
	 * @param value The value of the field to be added
	 * @param allowDuplicates true if duplicate names are OK
	 */

	public void addFormField( final String name, final String value, boolean allowDuplicates )
	{
		this.dataChanged = true;

		String encodedName = name + "=";
		String encodedValue = value == null ? "" : value;

		try
		{
			encodedValue = URLEncoder.encode( encodedValue, this.isChatRequest ? "ISO-8859-1" : "UTF-8" );
		}
		catch ( IOException e )
		{
		}

		// Make sure that when you're adding data fields, you don't
		// submit duplicate fields.

		if ( !allowDuplicates )
		{
			Iterator it = this.data.iterator();
			while ( it.hasNext() )
			{
				if ( ( (String) it.next() ).startsWith( encodedName ) )
				{
					it.remove();
				}
			}
		}

		// If the data did not already exist, then
		// add it to the end of the array.

		this.data.add( encodedName + encodedValue );
	}

	public void addFormField( final String name, final String value )
	{
		this.addFormField( name, value, false );
	}

	/**
	 * Adds the given form field to the GenericRequest.
	 *
	 * @param element The field to be added
	 */

	public void addFormField( final String element )
	{
		int equalIndex = element.indexOf( "=" );
		if ( equalIndex == -1 )
		{
			this.addFormField( element, "", false );
			return;
		}

		String name = element.substring( 0, equalIndex ).trim();
		String value = element.substring( equalIndex + 1 ).trim();
		this.addFormField( name, value, true );
	}

	/**
	 * Adds an already encoded form field to the GenericRequest.
	 *
	 * @param element The field to be added
	 */

	public void addEncodedFormField( final String element )
	{
		if ( element == null )
		{
			return;
		}

		this.data.add( element );
	}

	public void addEncodedFormFields( final String fields )
	{
		if ( fields.indexOf( "&" ) == -1 )
		{
			this.addEncodedFormField( fields );
			return;
		}

		String[] tokens = fields.split( "&" );
		for ( int i = 0; i < tokens.length; ++i )
		{
			if ( tokens[ i ].indexOf( " " ) != -1 )
			{
				this.addFormField( tokens[ i ] );
			}
			else
			{
				this.addEncodedFormField( tokens[ i ] );
			}
		}
	}

	public String getFormField( final String key )
	{
		if ( this.data.isEmpty() )
		{
			return null;
		}

		String datum;

		for ( int i = 0; i < this.data.size(); ++i )
		{
			datum = (String) this.data.get( i );

			int splitIndex = datum.indexOf( "=" );
			if ( splitIndex == -1 )
			{
				continue;
			}

			String name = datum.substring( 0, splitIndex );
			if ( !name.equalsIgnoreCase( key ) )
			{
				continue;
			}

			String value = datum.substring( splitIndex + 1 );

			try
			{
				// Everything was encoded as ISO-8859-1, so go
				// ahead and decode it that way.

				return URLDecoder.decode( value, this.isChatRequest ? "ISO-8859-1" : "UTF-8" );
			}
			catch ( IOException e )
			{
				// This shouldn't happen, but since you did
				// manage to find the key, return the value.

				return value;
			}
		}

		return null;
	}

	public void removeFormField( final String name )
	{
		if ( name == null )
		{
			return;
		}

		this.dataChanged = true;

		String encodedName = name + "=";

		Iterator it = this.data.iterator();
		while ( it.hasNext() )
		{
			if ( ( (String) it.next() ).startsWith( encodedName ) )
			{
				it.remove();
			}
		}
	}

	public String getPath()
	{
		return this.formURLString;
	}

	public String getDataString( final boolean includeHash )
	{
		StringBuffer dataBuffer = new StringBuffer();

		String element;
		for ( int i = 0; i < this.data.size(); ++i )
		{
			if ( i > 0 )
			{
				dataBuffer.append( '&' );
			}

			element = (String) this.data.get( i );

			if ( element.startsWith( "pwd" ) || element.startsWith( "phash" ) )
			{
				int index = element.indexOf( "=" );
				if ( index != -1 )
				{
					element = element.substring( 0, index );
				}

				dataBuffer.append( element );

				if ( includeHash )
				{
					dataBuffer.append( "=" );
					dataBuffer.append( GenericRequest.passwordHash );
				}
			}
			else
			{
				dataBuffer.append( element );
			}
		}

		return dataBuffer.toString();
	}

	private boolean shouldUpdateDebugLog()
	{
		return RequestLogger.isDebugging() && !this.isChatRequest;
	}

	/**
	 * Runs the thread, which prepares the connection for output, posts the data to the Kingdom of Loathing, and
	 * prepares the input for reading. Because the Kingdom of Loathing has identical page layouts, all page reading and
	 * handling will occur through these method calls.
	 */

	public void run()
	{
		if ( GenericRequest.serverCookie == null && !( this instanceof LoginRequest ) )
		{
			return;
		}

		this.timeoutCount = 0;
		this.containsUpdate = false;

		String location = this.getURLString();

		if ( location.indexOf( "clan" ) != -1 )
		{
			if ( location.indexOf( "action=leaveclan" ) != -1 || location.indexOf( "action=joinclan" ) != -1 )
			{
				ClanManager.resetClanId();
			}
		}

		if ( !this.hasNoResult )
		{
			TurnCounter expired = TurnCounter.getExpiredCounter( this );
			if ( expired != null )
			{
				KoLmafia.updateDisplay( KoLConstants.ABORT_STATE, expired.getLabel() + " counter expired." );
				return;
			}
		}

		if ( this.shouldUpdateDebugLog() )
		{
			RequestLogger.updateDebugLog( this.getClass() );
		}

		if ( location.startsWith( "sewer.php" ) )
		{
			InventoryManager.retrieveItem( ItemPool.CHEWING_GUM );
		}
		else if ( location.startsWith( "hermit.php?autopermit=on" ) )
		{
			InventoryManager.retrieveItem( HermitRequest.PERMIT.getInstance( 1 ) );
		}
		else if ( location.startsWith( "casino.php" ) )
		{
			InventoryManager.retrieveItem( ItemPool.CASINO_PASS );
		}

		// To avoid wasting turns, buy a can of hair spray before
		// climbing the tower.  Also, if the person has an NG,
		// make sure to construct it first.  If there are any
		// tower items sitting in the closet or that have not
		// been constructed, pull them out.

		if ( location.startsWith( "lair4.php" ) || location.startsWith( "lair5.php" ) )
		{
			SorceressLairManager.makeGuardianItems();
		}

		this.execute();

		if ( this.responseCode != 200 )
		{
			return;
		}

		// Call central dispatch method for locations that require
		// special handling

		CouncilFrame.handleQuestChange( location, this.responseText );

		if ( this.formURLString.equals( "charpane.php" ) )
		{
			KoLCharacter.recalculateAdjustments();
			RequestFrame.refreshStatus();
			LocalRelayServer.updateStatus();
		}

		this.formatResponse();
		KoLCharacter.updateStatus();
	}

	public void execute()
	{
		String urlString = this.getURLString();

		// If this is the rat quest, then go ahead and pre-set the data
		// to reflect a fight sequence (mini-browser compatibility).

		if ( !GenericRequest.isRatQuest )
		{
			GenericRequest.isRatQuest = urlString.startsWith( "rats.php" );
		}

		if ( !this.hasNoResult && !urlString.startsWith( "rats.php" ) && GenericRequest.isRatQuest )
		{
			GenericRequest.isRatQuest = urlString.startsWith( "fight.php" );
		}

		if ( GenericRequest.isRatQuest )
		{
			KoLmafia.addTavernLocation( this );
		}

		if ( !this.hasNoResult )
		{
			RequestLogger.registerRequest( this, urlString );
		}

		if ( urlString.startsWith( "choice.php" ) )
		{
			this.saveLastChoice();
		}

		// If you're about to fight the Naughty Sorceress,
		// clear your list of effects.

		if ( urlString.startsWith( "lair6.php" ) && urlString.indexOf( "place=5" ) != -1 )
		{
			KoLConstants.activeEffects.clear();
		}

		if ( urlString.startsWith( "lair6.php" ) && urlString.indexOf( "place=6" ) != -1 )
		{
			KoLCharacter.setHardcore( false );
			KoLCharacter.setConsumptionRestriction( AscensionSnapshot.NOPATH );
		}

		if ( urlString.startsWith( "ascend.php" ) )
		{
			ValhallaManager.preAscension();
		}

		if ( urlString.startsWith( "valhalla.php" ) && Preferences.getInteger( "lastBreakfast" ) != -1 )
		{
			ValhallaManager.onAscension();
		}

		do
		{
			if ( !this.prepareConnection() )
			{
				break;
			}
		}
		while ( !this.postClientData() && !this.retrieveServerReply() && this.timeoutCount < GenericRequest.TIMEOUT_LIMIT );
	}

	private void saveLastChoice()
	{
		if ( this.data.isEmpty() )
		{
			return;
		}

		GenericRequest.lastChoice = StringUtilities.parseInt( this.getFormField( "whichchoice" ) );
		GenericRequest.lastDecision = StringUtilities.parseInt( this.getFormField( "option" ) );

		switch ( GenericRequest.lastChoice )
		{
		// Strung-Up Quartet
		case 106:

			Preferences.setInteger( "lastQuartetAscension", KoLCharacter.getAscensions() );
			Preferences.setInteger( "lastQuartetRequest", GenericRequest.lastDecision );

			if ( KoLCharacter.recalculateAdjustments() )
			{
				KoLCharacter.updateStatus();
			}

			break;

		// Wheel In the Sky Keep on Turning: Muscle Position
		case 9:
			Preferences.setString(
				"currentWheelPosition",
				GenericRequest.lastDecision == 1 ? "mysticality" : GenericRequest.lastDecision == 2 ? "moxie" : "muscle" );
			break;

		// Wheel In the Sky Keep on Turning: Mysticality Position
		case 10:
			Preferences.setString(
				"currentWheelPosition",
				GenericRequest.lastDecision == 1 ? "map quest" : GenericRequest.lastDecision == 2 ? "muscle" : "mysticality" );
			break;

		// Wheel In the Sky Keep on Turning: Map Quest Position
		case 11:
			Preferences.setString(
				"currentWheelPosition",
				GenericRequest.lastDecision == 1 ? "moxie" : GenericRequest.lastDecision == 2 ? "mysticality" : "map quest" );
			break;

		// Wheel In the Sky Keep on Turning: Moxie Position
		case 12:
			Preferences.setString(
				"currentWheelPosition",
				GenericRequest.lastDecision == 1 ? "muscle" : GenericRequest.lastDecision == 2 ? "map quest" : "moxie" );
			break;

		// Start the Island War Quest
		case 142:
		case 146:
			if ( GenericRequest.lastDecision == 3 )
			{
				QuestLogRequest.setHippyStoreUnavailable();
			}
			break;
		}
	}

	private void mapCurrentChoice()
	{
		String text = this.responseText;

		// Let the Violet Fog handle this
		if ( VioletFogManager.mapChoice( GenericRequest.lastChoice, text ) )
		{
			return;
		}

		// Let the Louvre handle this
		if ( LouvreManager.mapChoice( GenericRequest.lastChoice, text ) )
		{
			return;
		}

		// Handle the Guy Made of Bees
		if ( this.checkGuyMadeOfBees() )
		{
			return;
		}
	}

	private boolean checkGuyMadeOfBees()
	{
		if ( GenericRequest.lastChoice != 105 )
		{
			return false;
		}

		if ( GenericRequest.lastDecision != 3 )
		{
			return true;
		}

		KoLCharacter.ensureUpdatedGuyMadeOfBees();
		String text = this.responseText;

		String urlString = this.formURLString;
		if ( urlString.startsWith( "fight.php" ) )
		{
			if ( text.indexOf( "guy made of bee pollen" ) != -1 )
			{
				// Record that we beat the guy made of bees.
				Preferences.setBoolean( "guyMadeOfBeesDefeated", true );
			}
		}
		else if ( urlString.startsWith( "choice.php" ) )
		{
			if ( text.indexOf( "that ship is sailed" ) != -1 )
			{
				// For some reason, we didn't notice when we
				// beat the guy made of bees. Record it now.
				Preferences.setBoolean( "guyMadeOfBeesDefeated", true );
			}
			else if ( text.indexOf( "Nothing happens." ) != -1 )
			{
				// Increment the number of times we've
				// called the guy made of bees.
				Preferences.increment( "guyMadeOfBeesCount", 1, 5, true );
			}

		}

		return true;
	}

	public static final int getLastChoice()
	{
		return GenericRequest.lastChoice;
	}

	public static final int getLastDecision()
	{
		return GenericRequest.lastDecision;
	}

	public static final boolean shouldIgnore( final GenericRequest request )
	{
		return request.formURLString.indexOf( "mall" ) != -1 || request.formURLString.indexOf( "chat" ) != -1;
	}

	/**
	 * Utility method used to prepare the connection for input and output (if output is necessary). The method attempts
	 * to open the connection, and then apply the needed settings.
	 *
	 * @return <code>true</code> if the connection was successfully prepared
	 */

	private boolean prepareConnection()
	{
		if ( this.shouldUpdateDebugLog() )
		{
			RequestLogger.updateDebugLog( "Connecting to " + this.formURLString + "..." );
		}

		// Make sure that all variables are reset before you reopen
		// the connection.

		this.responseCode = 0;
		this.responseText = null;
		this.redirectLocation = null;
		this.formConnection = null;

		try
		{
			// For now, because there isn't HTTPS support, just open the
			// connection and directly cast it into an HttpURLConnection

			if ( this.formURL == null || !this.currentHost.equals( GenericRequest.KOL_HOST ) )
			{
				if ( Preferences.getBoolean( "allowSocketTimeout" ) )
				{
					if ( this.formURLString.startsWith( "http:" ) )
						this.formURL = new URL( null, this.formURLString, HttpTimeoutHandler.getInstance() );
					else
						this.formURL = new URL( GenericRequest.KOL_ROOT, this.formURLString, HttpTimeoutHandler.getInstance() );
				}
				else
				{
					if ( this.formURLString.startsWith( "http:" ) )
					{
						this.formURL = new URL( this.formURLString );
					}
					else
					{
						this.formURL = new URL( GenericRequest.KOL_ROOT, this.formURLString );
					}
				}

			}

			this.formConnection = (HttpURLConnection) this.formURL.openConnection();
		}
		catch ( IOException e )
		{
			if ( this.shouldUpdateDebugLog() )
			{
				RequestLogger.updateDebugLog( "Error opening connection (" + this.getURLString() + ").  Retrying..." );
			}

			if ( this instanceof LoginRequest )
			{
				GenericRequest.chooseNewLoginServer();
			}

			return false;
		}

		this.formConnection.setDoInput( true );
		this.formConnection.setDoOutput( !this.data.isEmpty() );
		this.formConnection.setUseCaches( false );
		this.formConnection.setInstanceFollowRedirects( false );

		if ( GenericRequest.serverCookie != null )
		{
			if ( this.formURLString.startsWith( "inventory" ) && GenericRequest.inventoryCookie != null )
			{
				this.formConnection.addRequestProperty(
					"Cookie", GenericRequest.inventoryCookie + "; " + GenericRequest.serverCookie );
			}
			else
			{
				this.formConnection.addRequestProperty( "Cookie", GenericRequest.serverCookie );
			}
		}

		this.formConnection.setRequestProperty( "User-Agent", GenericRequest.getUserAgent() );

		if ( this.dataChanged )
		{
			this.dataChanged = false;
			this.dataString = this.getDataString( true ).getBytes();
		}

		if ( !this.data.isEmpty() )
		{
			this.formConnection.setRequestProperty( "Content-Type", "application/x-www-form-urlencoded" );
			this.formConnection.setRequestProperty( "Content-Length", String.valueOf( this.dataString.length ) );
		}

		return true;
	}

	/**
	 * Utility method used to post the client's data to the Kingdom of Loathing server. The method grabs all form fields
	 * added so far and posts them using the traditional ampersand style of HTTP requests.
	 *
	 * @return <code>true</code> if all data was successfully posted
	 */

	private boolean postClientData()
	{
		if ( this.shouldUpdateDebugLog() )
		{
			this.printRequestProperties();
		}

		// Only attempt to post something if there's actually
		// data to post - otherwise, opening an input stream
		// should be enough

		if ( this.data.isEmpty() )
		{
			return false;
		}

		try
		{
			this.formConnection.setRequestMethod( "POST" );

			OutputStream ostream = this.formConnection.getOutputStream();
			ostream.write( this.dataString );

			ostream.flush();
			ostream.close();

			ostream = null;
			return false;
		}
		catch ( IOException e )
		{
			++this.timeoutCount;

			if ( this.shouldUpdateDebugLog() )
			{
				RequestLogger.printLine( "Time out during data post (" + this.formURLString + ").  This could be bad..." );
			}

			if ( this instanceof LoginRequest )
			{
				GenericRequest.chooseNewLoginServer();
			}

			return KoLmafia.refusesContinue();
		}
	}

	/**
	 * Utility method used to retrieve the server's reply. This method detects the nature of the reply via the response
	 * code provided by the server, and also detects the unusual states of server maintenance and session timeout. All
	 * data retrieved by this method is stored in the instance variables for this class.
	 *
	 * @return <code>true</code> if the data was successfully retrieved
	 */

	private boolean retrieveServerReply()
	{
		InputStream istream = null;

		// In the event of redirects, the appropriate flags should be set
		// indicating whether or not the direct is a normal redirect (ie:
		// one that results in something happening), or an error-type one
		// (ie: maintenance).

		if ( this.shouldUpdateDebugLog() )
		{
			RequestLogger.updateDebugLog( "Retrieving server reply..." );
		}

		this.responseText = "";
		this.redirectLocation = "";

		try
		{
			istream = this.formConnection.getInputStream();
			this.responseCode = this.formConnection.getResponseCode();
			this.redirectLocation = this.responseCode != 302 ? null : this.formConnection.getHeaderField( "Location" );
		}
		catch ( IOException e1 )
		{
			++this.timeoutCount;
			boolean shouldRetry = this.retryOnTimeout();

			if ( !shouldRetry && this.processOnFailure() )
			{
				this.processResults();
			}

			if ( this.shouldUpdateDebugLog() )
			{
				RequestLogger.printLine( "Time out during response (" + this.formURLString + ")." );
			}

			try
			{
				if ( istream != null )
				{
					istream.close();
				}
			}
			catch ( IOException e2 )
			{
				// The input stream was already closed.  Ignore this
				// error and continue.
			}

			if ( this instanceof LoginRequest )
			{
				GenericRequest.chooseNewLoginServer();
			}

			if ( shouldRetry )
			{
				return KoLmafia.refusesContinue();
			}
		}

		if ( istream == null )
		{
			this.responseCode = 302;
			this.redirectLocation = "main.php";
			return true;
		}

		if ( this.shouldUpdateDebugLog() )
		{
			this.printHeaderFields();
		}

		boolean shouldStop = false;

		try
		{
			if ( this.responseCode == 200 )
			{
				shouldStop = this.retrieveServerReply( istream );
				istream.close();
			}
			else
			{
				// If the response code is not 200, then you've read all
				// the information you need.  Close the input stream.

				istream.close();
				shouldStop = this.responseCode == 302 ? this.handleServerRedirect() : true;
			}
		}
		catch ( IOException e )
		{
			return true;
		}

		istream = null;
		return shouldStop || KoLmafia.refusesContinue();
	}

	protected boolean retryOnTimeout()
	{
		return this.formURLString.endsWith( ".php" ) && ( this.data.isEmpty() || this.getClass() == GenericRequest.class );
	}

	protected boolean processOnFailure()
	{
		return false;
	}

	private boolean handleServerRedirect()
	{
		if ( this.redirectLocation == null )
		{
			return true;
		}

		if ( this.redirectLocation.startsWith( "maint.php" ) )
		{
			// If the system is down for maintenance, the user must be
			// notified that they should try again later.

			KoLmafia.updateDisplay( KoLConstants.ABORT_STATE, "Nightly maintenance.  Please restart KoLmafia." );
			GenericRequest.serverCookie = null;
			return true;
		}

		// Check to see if this is a login page redirect.  If it is, then
		// construct the URL string and notify the browser that it should
		// change everything.

		if ( this.formURLString.equals( "login.php" ) )
		{
			if ( this.redirectLocation.startsWith( "login.php" ) )
			{
				this.constructURLString( this.redirectLocation, false );
				return false;
			}

			Matcher matcher = GenericRequest.REDIRECT_PATTERN.matcher( this.redirectLocation );
			if ( matcher.find() )
			{
				GenericRequest.setLoginServer( matcher.group( 1 ) );
				RequestLogger.printLine( "Redirected to " + GenericRequest.KOL_HOST + "..." );
				return false;
			}

			LoginRequest.processLoginRequest( this );
			return true;
		}

		if ( this.redirectLocation.startsWith( "fight.php" ) )
		{
			int itemId = UseItemRequest.currentItemId();
			String name = null;
			boolean consumed = true;

			switch ( itemId )
			{
			case ItemPool.DRUM_MACHINE:
				name = "Drum Machine";
				break;

			case ItemPool.BLACK_PUDDING:
				Preferences.setInteger( "currentFullness", KoLCharacter.getFullness() - 3 );
				name = "Black Pudding";
				break;

			case ItemPool.CARONCH_MAP:
				name = "Cap'm Caronch's Map";
				break;

			case ItemPool.CURSED_PIECE_OF_THIRTEEN:
				name = "Cursed Piece of Thirteen";
				consumed = false;
				break;
			}

			UseItemRequest.resetItemUsed();

			if ( name != null )
			{
				if ( consumed )
				{
					ResultProcessor.processResult( ItemPool.get( itemId, -1 ) );
				}

				RequestLogger.printLine();
				RequestLogger.printLine( "[" + KoLAdventure.getAdventureCount() + "] " + name );

				RequestLogger.updateSessionLog();
				RequestLogger.updateSessionLog( "[" + KoLAdventure.getAdventureCount() + "] " + name );

				if ( this instanceof UseItemRequest )
				{
					FightRequest.INSTANCE.run();
					CharPaneRequest.getInstance().run();
					return !LoginRequest.isInstanceRunning();
				}
			}
		}

		if ( this instanceof RelayRequest )
		{
			return true;
		}

		if ( this.formURLString.startsWith( "fight.php" ) )
		{
			return true;
		}

		if ( this.shouldFollowRedirect() )
		{
			// Re-setup this request to follow the redirect
			// desired and rerun the request.

			this.constructURLString( this.redirectLocation, false );
			return false;
		}

		if ( this.redirectLocation.startsWith( "fight.php" ) )
		{
			// You have been redirected to a fight!  Here, you need
			// to complete the fight before you can continue.

			if ( LoginRequest.isInstanceRunning() || this == ChoiceManager.CHOICE_HANDLER || this instanceof AdventureRequest || this instanceof BasementRequest )
			{
				FightRequest.INSTANCE.run();
				CharPaneRequest.getInstance().run();
				return !LoginRequest.isInstanceRunning();
			}

			// This is a request which should not have lead to a
			// fight, but it did.  Notify the user.

			KoLmafia.updateDisplay( KoLConstants.ABORT_STATE, "Redirected to a fight page." );
			return true;
		}

		if ( this.redirectLocation.startsWith( "login.php" ) )
		{
			return !LoginRequest.executeTimeInRequest( this.getURLString(), this.redirectLocation );
		}

		if ( this.redirectLocation.startsWith( "choice.php" ) )
		{
			GenericRequest.handlingChoices = true;
			ChoiceManager.processChoiceAdventure();
			GenericRequest.handlingChoices = false;

			CharPaneRequest.getInstance().run();
			return true;
		}

		if ( this.redirectLocation.startsWith( "ocean.php" ) )
		{
			OceanManager.processOceanAdventure();
			return true;
		}

		if ( this instanceof AdventureRequest || this.formURLString.startsWith( "choice.php" ) )
		{
			AdventureRequest.handleServerRedirect( this.redirectLocation );
			CharPaneRequest.getInstance().run();
			return true;
		}

		if ( this.shouldUpdateDebugLog() )
		{
			RequestLogger.updateDebugLog( "Redirected: " + this.redirectLocation );
		}

		return true;
	}

	protected boolean shouldFollowRedirect()
	{
		return this != ChoiceManager.CHOICE_HANDLER && this.getClass() == GenericRequest.class;
	}

	private static final void addAdditionalCache()
	{
		synchronized ( GenericRequest.BYTEFLAGS )
		{
			GenericRequest.BYTEFLAGS.add( Boolean.TRUE );
			GenericRequest.BYTEARRAYS.add( new byte[ 8096 ] );
			GenericRequest.BYTESTREAMS.add( new ByteArrayOutputStream( 8096 ) );
		}
	}

	private boolean retrieveServerReply( final InputStream istream )
		throws IOException
	{
		// Find an available byte array in order to buffer the data.  Allow
		// this to scale based on the number of incoming requests in order
		// to reduce the probability that the program hangs.

		int desiredIndex = -1;

		synchronized ( GenericRequest.BYTEFLAGS )
		{
			for ( int i = 0; desiredIndex == -1 && i < GenericRequest.BYTEFLAGS.size(); ++i )
			{
				if ( GenericRequest.BYTEFLAGS.get( i ) == Boolean.FALSE )
				{
					desiredIndex = i;
				}
			}
		}

		if ( desiredIndex == -1 )
		{
			desiredIndex = GenericRequest.BYTEFLAGS.size();
			GenericRequest.addAdditionalCache();
		}
		else
		{
			GenericRequest.BYTEFLAGS.set( desiredIndex, Boolean.TRUE );
		}

		// Read all the data into the static byte array output stream
		// and then convert that string to UTF-8.

		byte[] array = (byte[]) GenericRequest.BYTEARRAYS.get( desiredIndex );
		ByteArrayOutputStream stream = (ByteArrayOutputStream) GenericRequest.BYTESTREAMS.get( desiredIndex );

		int availableBytes = 0;
		while ( ( availableBytes = istream.read( array ) ) != -1 )
		{
			stream.write( array, 0, availableBytes );
		}

		this.responseText = stream.toString( "UTF-8" );
		stream.reset();

		// You are now done with the array.  Go ahead and reset the value
		// to false to let the program know the objects are available to
		// be reused.

		GenericRequest.BYTEFLAGS.set( desiredIndex, Boolean.FALSE );
		this.processResponse();
		return true;
	}

	/**
	 * This method allows classes to process a raw, unfiltered server response.
	 */

	public void processResponse()
	{
		if ( this.responseText == null )
		{
			return;
		}

		if ( this.shouldUpdateDebugLog() )
		{
			RequestLogger.updateDebugLog( KoLConstants.LINE_BREAK_PATTERN.matcher( this.responseText ).replaceAll( "" ) );
		}

		if ( !this.isChatRequest && !this.formURLString.startsWith( "fight.php" ) )
		{
			this.checkForNewEvents();
		}

		if ( GenericRequest.isRatQuest )
		{
			KoLmafia.addTavernLocation( this );
		}

		this.encounter = AdventureRequest.registerEncounter( this );

		if ( this.formURLString.equals( "fight.php" ) )
		{
			FightRequest.updateCombatData( this.encounter, this.responseText );
		}

		int effectCount = KoLConstants.activeEffects.size();

		if ( !this.hasNoResult )
		{
			int initialHP = KoLCharacter.getCurrentHP();
			this.parseResults();

			if ( initialHP != 0 && KoLCharacter.getCurrentHP() == 0 )
			{
				KoLConstants.activeEffects.remove( KoLAdventure.BEATEN_UP );
				KoLConstants.activeEffects.add( KoLAdventure.BEATEN_UP );
			}

			if ( !LoginRequest.isInstanceRunning() && !( this instanceof RelayRequest ) )
			{
				this.showInBrowser( false );
			}
		}

		// Now let the main method of result processing for
		// each request type happen.

		this.processResults();

		// Let the mappers do their work

		this.mapCurrentChoice();

		// Once everything is complete, decide whether or not
		// you should refresh your status.

		if ( this.hasNoResult )
		{
			return;
		}

		if ( this instanceof RelayRequest )
		{
			if ( !this.formURLString.equals( "basement.php" ) )
			{
				return;
			}

			this.containsUpdate = true;
		}

		if ( this.containsUpdate )
		{
			this.containsUpdate = true;
		}
		else if ( effectCount != KoLConstants.activeEffects.size() || this.getAdventuresUsed() > 0 )
		{
			this.containsUpdate = true;
		}
		else if ( RequestFrame.sidebarFrameExists() )
		{
			this.containsUpdate = this.responseText.indexOf( "charpane" ) != -1;
		}

		if ( this.containsUpdate )
		{
			CharPaneRequest.getInstance().run();
		}
	}

	public void formatResponse()
	{
	}

	/**
	 * Utility method used to skip the given number of tokens within the provided <code>StringTokenizer</code>. This
	 * method is used in order to clarify what's being done, rather than calling <code>st.nextToken()</code>
	 * repeatedly.
	 *
	 * @param st The <code>StringTokenizer</code> whose tokens are to be skipped
	 * @param tokenCount The number of tokens to skip
	 */

	public static final void skipTokens( final StringTokenizer st, final int tokenCount )
	{
		for ( int i = 0; i < tokenCount; ++i )
		{
			st.nextToken();
		}
	}

	/**
	 * Utility method used to transform the next token on the given <code>StringTokenizer</code> into an integer.
	 * Because this is used repeatedly in parsing, its functionality is provided globally to all instances of
	 * <code>GenericRequest</code>.
	 *
	 * @param st The <code>StringTokenizer</code> whose next token is to be retrieved
	 * @return The integer token, if it exists, or 0, if the token was not a number
	 */

	public static final int intToken( final StringTokenizer st )
	{
		return GenericRequest.intToken( st, 0 );
	}

	/**
	 * Utility method used to transform the next token on the given <code>StringTokenizer</code> into an integer;
	 * however, this differs in the single-argument version in that only a part of the next token is needed. Because
	 * this is also used repeatedly in parsing, its functionality is provided globally to all instances of
	 * <code>GenericRequest</code>.
	 *
	 * @param st The <code>StringTokenizer</code> whose next token is to be retrieved
	 * @param fromStart The index at which the integer to parse begins
	 * @return The integer token, if it exists, or 0, if the token was not a number
	 */

	public static final int intToken( final StringTokenizer st, final int fromStart )
	{
		String token = st.nextToken().substring( fromStart );
		return StringUtilities.parseInt( token );
	}

	/**
	 * Utility method used to transform part of the next token on the given <code>StringTokenizer</code> into an
	 * integer. This differs from the two-argument in that part of the end of the string is expected to contain
	 * non-numeric values as well. Because this is also repeatedly in parsing, its functionality is provided globally to
	 * all instances of <code>GenericRequest</code>.
	 *
	 * @param st The <code>StringTokenizer</code> whose next token is to be retrieved
	 * @param fromStart The index at which the integer to parse begins
	 * @param fromEnd The distance from the end at which the first non-numeric character is found
	 * @return The integer token, if it exists, or 0, if the token was not a number
	 */

	public static final int intToken( final StringTokenizer st, final int fromStart, final int fromEnd )
	{
		String token = st.nextToken();
		token = token.substring( fromStart, token.length() - fromEnd );
		return StringUtilities.parseInt( token );
	}

	/**
	 * An alternative method to doing adventure calculation is determining how many adventures are used by the given
	 * request, and subtract them after the request is done. This number defaults to <code>zero</code>; overriding
	 * classes should change this value to the appropriate amount.
	 *
	 * @return The number of adventures used by this request.
	 */

	public int getAdventuresUsed()
	{
		return 0;
	}

	private final void parseResults()
	{
		// If this is a lucky adventure, then remove a clover
		// from the player's inventory -- this will occur when
		// you see either "Your ten-leaf clover" or "your
		// ten-leaf clover" (shorten to "our ten-leaf clover"
		// for substring matching)

		if ( this.responseText.indexOf( "our ten-leaf clover" ) != -1 && this.responseText.indexOf( "puff of smoke" ) != -1 )
		{
			ResultProcessor.processItem( ItemPool.TEN_LEAF_CLOVER, -1 );
		}

		if ( this.formURLString.startsWith( "sewer.php" ) && this.responseText.indexOf( "You acquire" ) != -1 )
		{
			ResultProcessor.processItem( ItemPool.CHEWING_GUM, -1 );
		}

		this.containsUpdate = ResultProcessor.processResults( this.responseText );

		if ( ResultProcessor.shouldDisassembleClovers( this.formURLString ) )
		{
			KoLmafiaCLI.DEFAULT_SHELL.executeCommand( "use", "* ten-leaf clover" );
		}
	}

	public void processResults()
	{
	}

	/*
	 * Method to display the current request in the Fight Frame. If we are synchronizing, show all requests If we are
	 * finishing, show only exceptional requests
	 */

	public void showInBrowser( boolean exceptional )
	{
		// Check to see if this request should be showed
		// in a browser.  If you're using a command-line
		// interface, then you should not display the request.

		if ( KoLConstants.existingFrames.isEmpty() )
		{
			return;
		}

		if ( !exceptional && !Preferences.getBoolean( "showAllRequests" ) )
		{
			return;
		}

		// Only show the request if the response code is
		// 200 (not a redirect or error).

		RequestSynchFrame.showRequest( this );
	}

	private void checkForNewEvents()
	{
		// Capture the entire new events table in order to display the
		// appropriate message.

		Matcher eventMatcher = GenericRequest.EVENT_PATTERN.matcher( this.responseText );
		if ( !eventMatcher.find() )
		{
			return;
		}

		// Make an array of events
		String[] events = eventMatcher.group( 1 ).replaceAll( "<br>", "\n" ).split( "\n" );

		for ( int i = 0; i < events.length; ++i )
		{
			if ( events[ i ].indexOf( "/" ) == -1 )
			{
				events[ i ] = null;
			}
		}

		// Remove the events from the response text

		this.responseText = eventMatcher.replaceFirst( "" );

		boolean shouldLoadEventFrame = false;
		boolean isChatRunning = ChatManager.isRunning();

		for ( int i = 0; i < events.length; ++i )
		{
			if ( events[ i ] == null )
			{
				continue;
			}

			if ( events[ i ].indexOf( "logged" ) != -1 )
			{
				continue;
			}

			String event = events[ i ];

			// The event may be marked up with color and links to
			// user profiles. For example:

			// 04/25/06 12:53:54 PM - New message received from <a target=mainpane href='showplayer.php?who=115875'><font color=green>Brianna</font></a>.
			// 04/25/06 01:06:43 PM - <a class=nounder target=mainpane href='showplayer.php?who=115875'><b><font color=green>Brianna</font></b></a> has played a song (The Polka of Plenty) for you.

			// Add in a player Id so that the events can be handled
			// using a ShowDescriptionList.

			event = event.replaceAll( "</a>", "<a>" ).replaceAll( "<[^a].*?>", " " ).replaceAll( "\\s+", " " );
			event = event.replaceAll( "<a[^>]*showplayer\\.php\\?who=(\\d+)[^>]*>(.*?)<a>", "$2 (#$1)" );

			if ( event.indexOf( "/" ) == -1 )
			{
				continue;
			}

			shouldLoadEventFrame = true;
			KoLConstants.eventHistory.add( event );

			// Print everything to the default shell; this way, the
			// graphical CLI is also notified of events.

			RequestLogger.printLine( event );

			// Balloon messages for whenever the person does not have
			// focus on KoLmafia.

			if ( StaticEntity.usesSystemTray() )
			{
				SystemTrayFrame.showBalloon( event );
			}

			if ( isChatRunning )
			{
				int dash = event.indexOf( "-" );
				ChatManager.updateChat( "<font color=green>" + event.substring( dash + 2 ) + "</font>" );
			}
		}

		if ( shouldLoadEventFrame )
		{
			shouldLoadEventFrame = Preferences.getString( "initialFrames" ).indexOf( "RecentEventsFrame" ) != -1;
		}

		// If we're not a GUI and there are no GUI windows open
		// (ie: the GUI loader command wasn't used), quit now.

		if ( KoLConstants.existingFrames.isEmpty() )
		{
			return;
		}

		// If we are not running chat, pop up a RecentEventsFrame to
		// show the events.  Use the standard run method so that you
		// wait for it to finish before calling it again on another
		// event.

		if ( !isChatRunning && shouldLoadEventFrame )
		{
			SwingUtilities.invokeLater( new CreateFrameRunnable( RecentEventsFrame.class ) );
		}
	}

	public final void loadResponseFromFile( final String filename )
	{
		this.loadResponseFromFile( new File( filename ) );
	}

	public final void loadResponseFromFile( final File f )
	{
		BufferedReader buf = FileUtilities.getReader( f );

		try
		{
			String line;
			StringBuffer response = new StringBuffer();

			while ( ( line = buf.readLine() ) != null )
			{
				response.append( line );
			}

			this.responseCode = 200;
			this.responseText = response.toString();
		}
		catch ( IOException e )
		{
			// This means simply that there was no file from which
			// to load the data.  Given that this is run during debug
			// tests, only, we can ignore the error.
		}

		try
		{
			buf.close();
		}
		catch ( IOException e )
		{
		}
	}

	public String toString()
	{
		return this.getURLString();
	}

	public static final String getUserAgent()
	{
		String userAgent = Preferences.getString( "userAgent" );
		return userAgent.equals( "" ) ? KoLConstants.VERSION_NAME : userAgent;
	}

	public void printRequestProperties()
	{
		RequestLogger.updateDebugLog();
		RequestLogger.updateDebugLog( "Requesting: http://" + GenericRequest.KOL_HOST + "/" + this.getURLString() );

		Map requestProperties = this.formConnection.getRequestProperties();
		RequestLogger.updateDebugLog( requestProperties.size() + " request properties" );
		RequestLogger.updateDebugLog();

		Iterator iterator = requestProperties.entrySet().iterator();
		while ( iterator.hasNext() )
		{
			Entry entry = (Entry) iterator.next();
			RequestLogger.updateDebugLog( "Field: " + entry.getKey() + " = " + entry.getValue() );
		}

		RequestLogger.updateDebugLog();
	}

	public void printHeaderFields()
	{
		RequestLogger.updateDebugLog();
		RequestLogger.updateDebugLog( "Retrieved: http://" + GenericRequest.KOL_HOST + "/" + this.getURLString() );
		RequestLogger.updateDebugLog();

		Map headerFields = this.formConnection.getHeaderFields();
		RequestLogger.updateDebugLog( headerFields.size() + " header fields" );

		Iterator iterator = headerFields.entrySet().iterator();
		while ( iterator.hasNext() )
		{
			Entry entry = (Entry) iterator.next();
			RequestLogger.updateDebugLog( "Field: " + entry.getKey() + " = " + entry.getValue() );
		}

		RequestLogger.updateDebugLog();
	}
}
