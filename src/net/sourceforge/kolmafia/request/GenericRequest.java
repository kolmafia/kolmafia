/**
 * Copyright (c) 2005-2009, KoLmafia development team
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
import java.net.MalformedURLException;
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

import net.sourceforge.foxtrot.Job;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLAdventure;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.KoLmafiaASH;
import net.sourceforge.kolmafia.KoLmafiaCLI;
import net.sourceforge.kolmafia.LocalRelayServer;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.StaticEntity;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.AscensionSnapshot;
import net.sourceforge.kolmafia.persistence.Preferences;
import net.sourceforge.kolmafia.session.ChoiceManager;
import net.sourceforge.kolmafia.session.ClanManager;
import net.sourceforge.kolmafia.session.EventManager;
import net.sourceforge.kolmafia.session.InventoryManager;
import net.sourceforge.kolmafia.session.LouvreManager;
import net.sourceforge.kolmafia.session.OceanManager;
import net.sourceforge.kolmafia.session.ResultProcessor;
import net.sourceforge.kolmafia.session.SorceressLairManager;
import net.sourceforge.kolmafia.session.TurnCounter;
import net.sourceforge.kolmafia.session.ValhallaManager;
import net.sourceforge.kolmafia.session.VioletFogManager;
import net.sourceforge.kolmafia.swingui.CouncilFrame;
import net.sourceforge.kolmafia.swingui.RequestFrame;
import net.sourceforge.kolmafia.swingui.RequestSynchFrame;
import net.sourceforge.kolmafia.textui.Interpreter;
import net.sourceforge.kolmafia.textui.parsetree.Value;
import net.sourceforge.kolmafia.utilities.FileUtilities;
import net.sourceforge.kolmafia.utilities.StringUtilities;
import net.sourceforge.kolmafia.webui.BarrelDecorator;

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

	public static final Pattern REDIRECT_PATTERN = Pattern.compile( "([^\\/]+)\\/(login\\.php.*)", Pattern.DOTALL );

	public static boolean isRatQuest = false;
	public static boolean isBarrelSmash = false;
	public static boolean handlingChoices = false;
	private static boolean choiceHandled = true;
	private static boolean suppressUpdate = false;

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
	};

	public static final int SERVER_COUNT = SERVERS.length - 1;

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
	public String date;

	// Per-login data

	private static String userAgent = "";
	public static String serverCookie = null;
	public static String inventoryCookie = null;
	public static String passwordHash = "";

	public static void reset()
	{
		GenericRequest.setUserAgent();
		GenericRequest.serverCookie = null;
		GenericRequest.inventoryCookie = null;
		GenericRequest.passwordHash = "";
	}

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
			if ( GenericRequest.substringMatches( server, GenericRequest.SERVERS[ i ][ 0 ] ) ||
			     GenericRequest.substringMatches( server, GenericRequest.SERVERS[ i ][ 1 ] ) )
			{
				GenericRequest.setLoginServer( i );
				return;
			}
		}
	}

	private static final void setLoginServer( final int serverIndex )
	{
		GenericRequest.KOL_HOST = GenericRequest.SERVERS[ serverIndex ][ 0 ];
		String root = Preferences.getBoolean( "connectViaAddress" ) ?
			GenericRequest.SERVERS[ serverIndex ][ 1 ] :
			GenericRequest.KOL_HOST;

		try
		{
			GenericRequest.KOL_ROOT = new URL( "http", root, 80, "/" );
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
		LoginRequest.setIgnoreLoadBalancer( true );

		// Don't try to use the dev server
		GenericRequest.retryServer = Math.max( 1, ( GenericRequest.retryServer + 1 ) % GenericRequest.SERVERS.length );
		GenericRequest.setLoginServer( GenericRequest.retryServer );

		KoLmafia.updateDisplay( "Choosing new login server (" + GenericRequest.KOL_HOST + ")..." );
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

	public static void suppressUpdate( final boolean suppressUpdate )
	{
		GenericRequest.suppressUpdate = suppressUpdate;
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
			this.addFormFields( newURLString.substring( formSplitIndex + 1 ), true );
		}

		this.isChatRequest =
			this.formURLString.startsWith( "newchatmessages.php" ) || this.formURLString.startsWith( "submitnewchat.php" );

		this.hasNoResult =
			this.isChatRequest ||
			this.formURLString.startsWith( "http://" ) ||
			this.formURLString.endsWith( "menu.php" ) ||
			this.formURLString.startsWith( "actionbar" ) ||
			this.formURLString.startsWith( "charpane" ) ||
			this.formURLString.startsWith( "desc" ) ||
			this.formURLString.startsWith( "display" ) ||
			this.formURLString.startsWith( "makeoffer" ) ||
			this.formURLString.startsWith( "message" ) ||
			this.formURLString.startsWith( "quest" ) ||
			this.formURLString.startsWith( "search" ) ||
			this.formURLString.startsWith( "show" ) ||
			this.formURLString.startsWith( "valhalla" ) ||
			( this.formURLString.startsWith( "clan" ) &&
			  !this.formURLString.startsWith( "clan_stash" ) &&
			  !this.formURLString.startsWith( "clan_rumpus" ) &&
			  !this.formURLString.startsWith( "clan_viplounge" ) &&
			  !this.formURLString.startsWith( "clan_slimetube" ) &&
			  !this.formURLString.startsWith( "clan_hobopolis" ) ) ||
			this instanceof LoginRequest ||
			this instanceof LogoutRequest;

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

	public void addFormFields( final String fields, final boolean encoded )
	{
		if ( fields.indexOf( "&" ) == -1 )
		{
			this.addFormField( fields, encoded );
			return;
		}

		String[] tokens = fields.split( "&" );
		for ( int i = 0; i < tokens.length; ++i )
		{
			if ( tokens[ i ].length() > 0 )
			{
				this.addFormField( tokens[ i ], encoded );
			}
		}
	}

	public void addFormField( final String element, final boolean encoded )
	{
		if ( encoded )
		{
			this.addEncodedFormField( element );
		}
		else
		{
			this.addFormField( element );
		}
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

	public void addEncodedFormField( String element )
	{
		if ( element == null || element.equals( "" ) )
		{
			return;
		}

		// Browsers are inconsistent about what, exactly, they supply.
		// 
		// When you visit the crafting "Discoveries" page and select a
		// multi-step recipe, you the following as the path:
		//
		// craft.php?mode=cook&steps[]=2262,2263&steps[]=2264,2265
		//
		// If you then confirm that you want to make that recipe, you
		// get the following as your path:
		//
		// craft.php?mode=cook&steps[]=2262,2263&steps[]=2264,2265
		//
		// and the following as your POST data:
		//
		// action=craft&steps%5B%5D=2262%2C2263&steps5B%5D=2264%2C2265&qty=1&pwd
		//
		// URL decoding the latter gives:
		// 
		// action=craft&steps[]=2262,2263&steps[]=2264,2265&qty=1&pwd
		//
		// We have to recognize that the following are identical:
		//
		// steps%5B%5D=2262%2C2263
		// steps[]=2262,2263
		//
		// and not submit duplicates when we post the request. For the
		// above example, when we submit path + form fields, we want to
		// end up with:
		//
		// craft.php?mode=cook&steps[]=2262,2263&steps[]=2264,2265&action=craft&qty=1&pwd
		//
		// or, more correctly, with the data URLencoded:
		//
		// craft.php?mode=cook&steps[]=2262%2C2263&steps[]=2264%2C2265&action=craft&qty=1&pwd

		int equalIndex = element.indexOf( "=" );
		if ( equalIndex != -1 )
		{
			String name = element.substring( 0, equalIndex ).trim();
			String value = element.substring( equalIndex + 1 ).trim();
			try
			{
				String charset = this.isChatRequest ? "ISO-8859-1" : "UTF-8";
				// The name may or may not be encoded.
				name = URLDecoder.decode( name, "UTF-8" );

				// The value may or may not be encoded.
				value = URLDecoder.decode( value, charset );

				// But we want to always submit it encoded.
				value = URLEncoder.encode( value, charset );
			}
			catch ( IOException e )
			{
			}
			element = name + "=" + value;
		}

		Iterator it = this.data.iterator();
		while ( it.hasNext() )
		{
			if ( ( (String) it.next() ).equals( element ) )
			{
				return;
			}
		}

		this.data.add( element );
	}

	public String getFormField( final String key )
	{
		if ( !this.data.isEmpty() )
		{
			return this.findField( this.data, key );
		}

		int index = this.formURLString.indexOf( "?" );
		if ( index == -1 )
		{
			return null;
		}

		String[] tokens = this.formURLString.substring( index + 1 ).split( "&" );
		List fields = new ArrayList();
		for ( int i = 0; i < tokens.length; ++i )
		{
			fields.add( tokens[ i ] );
		}
		return this.findField( fields, key );
	}

	private String findField( final List data, final String key )
	{
		for ( int i = 0; i < data.size(); ++i )
		{
			String datum = (String) data.get( i );

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

	public String getBasePath()
	{
		String path =  this.formURLString;
		int quest = path.indexOf( "?" );
		return quest != -1 ? path.substring( 0, quest ) : path;
	}

	public String getHashField()
	{
		return "pwd";
	}

	private String getDataString( final boolean includeHash )
	{
		StringBuffer dataBuffer = new StringBuffer();
		String hashField = getHashField();

		for ( int i = 0; i < this.data.size(); ++i )
		{
			String element = (String) this.data.get( i );

			if ( element.equals( "" ) )
			{
				continue;
			}

			if ( element.startsWith( "pwd" ) || element.startsWith( "phash" ) )
			{
				int index = element.indexOf( '=' );
				if ( index != -1 )
				{
					element = element.substring( 0, index );
				}

				hashField = element;
			}
			else
			{
				if ( dataBuffer.length() > 0 )
				{
					dataBuffer.append( '&' );
				}
				dataBuffer.append( element );
			}
		}

		if ( !GenericRequest.passwordHash.equals( "" ) )
		{
			if ( dataBuffer.length() > 0 )
			{
				dataBuffer.append( '&' );
			}

			dataBuffer.append( hashField );

			if ( includeHash )
			{
				dataBuffer.append( '=' );
				dataBuffer.append( GenericRequest.passwordHash );
			}
		}

		return dataBuffer.toString();
	}

	private boolean shouldUpdateDebugLog()
	{
		return RequestLogger.isDebugging() && !this.isChatRequest;
	}

	private boolean invokeCounterScript( TurnCounter expired )
	{
		String scriptName = Preferences.getString( "counterScript" );
		if ( scriptName.length() == 0 )
		{
			return false;
		}
		Interpreter interpreter = KoLmafiaASH.getInterpreter(
			KoLmafiaCLI.findScriptFile( scriptName ) );
		if ( interpreter != null )
		{
			Value v = interpreter.execute( "main", new String[]
			{
				expired.getLabel(),
				String.valueOf( expired.getTurnsRemaining() )
			} );
			return v != null && v.intValue() != 0;
		}
		return false;
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
			while ( expired != null )
			{
				int remain = expired.getTurnsRemaining();
				if ( this.invokeCounterScript( expired ) )
				{
					remain = -1;
				}
				String message = null;

				if ( remain == 0 )
				{
					message = expired.getLabel() + " counter expired.";
				}
				else if ( remain > 0 )
				{
					message = expired.getLabel() + " counter will expire after " + remain + " more turn" + ((remain == 1) ? "." : "s.");
				}

				if ( message != null )
				{
					KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, message );
					return;
				}
				
				// In case the counterScript spent some turns, and there is now
				// a different expired counter to deal with:
				expired = TurnCounter.getExpiredCounter( this );
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
		else if ( location.startsWith( "mountains.php?orcs=1" ) )
		{
			InventoryManager.retrieveItem( ItemPool.BRIDGE );
		}
		else if ( location.startsWith( "casino.php" ) )
		{
			InventoryManager.retrieveItem( ItemPool.CASINO_PASS );
		}

		// To avoid wasting turns, buy a can of hair spray before
		// climbing the tower. Also, if the person has an NG, make sure
		// to construct it first.  If there are any tower items sitting
		// in the closet or that have not been constructed, pull them
		// out.

		if ( location.startsWith( "lair4.php" ) || location.startsWith( "lair5.php" ) )
		{
			SorceressLairManager.makeGuardianItems();
		}

		this.execute();

		if ( this.responseCode != 200 )
		{
			return;
		}

		if ( this.responseText == null || this.responseText.length() == 0 )
		{
			KoLmafia.updateDisplay( KoLConstants.ABORT_STATE, "Server returned empty response from " + this.getBasePath() );
			return;
		}

		// Call central dispatch method for locations that require
		// special handling

		CouncilFrame.handleQuestChange( location, this.responseText );

		if ( this.formURLString.equals( "charpane.php" ) )
		{
			KoLCharacter.recalculateAdjustments();
			// Mana cost adjustment may have changed
			KoLConstants.summoningSkills.sort();
			KoLConstants.usableSkills.sort();
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
			KoLmafia.preTavernVisit( this );
		}

		if ( !this.hasNoResult && GenericRequest.isBarrelSmash )
		{
			// Smash has resulted in a mimic.
			// Continue tracking throughout the combat
			GenericRequest.isBarrelSmash = urlString.startsWith( "fight.php" );
		}

		if ( urlString.startsWith( "barrel.php?" ) )
		{
			GenericRequest.isBarrelSmash = true;
			BarrelDecorator.beginSmash( urlString );
		}

		if ( !this.hasNoResult )
		{
			RequestLogger.registerRequest( this, urlString );
		}

		if ( urlString.startsWith( "choice.php" ) )
		{
			GenericRequest.choiceHandled = false;
			ChoiceManager.preChoice( this );
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
		this.date = null;
		this.formConnection = null;

		try
		{
			// For now, because there isn't HTTPS support, just
			// open the connection and directly cast it into an
			// HttpURLConnection

			this.formURL = this.buildURL();
			this.formConnection = (HttpURLConnection) this.formURL.openConnection();
		}
		catch ( IOException e )
		{
			if ( this.shouldUpdateDebugLog() )
			{
				RequestLogger.updateDebugLog( "Error opening connection (" + this.getURLString() + "). Retrying..." );
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

		this.formConnection.setRequestProperty( "User-Agent", GenericRequest.userAgent );

		if ( !this.data.isEmpty() )
		{
			if ( this.dataChanged )
			{
				this.dataChanged = false;
				this.dataString = this.getDataString( true ).getBytes();
			}

			this.formConnection.setRequestProperty( "Content-Type", "application/x-www-form-urlencoded" );
			this.formConnection.setRequestProperty( "Content-Length", String.valueOf( this.dataString.length ) );
		}

		return true;
	}

	private URL buildURL()
		throws MalformedURLException
	{
		if ( this.formURL != null && this.currentHost.equals( GenericRequest.KOL_HOST ) )
		{
			return this.formURL;
		}

		this.currentHost = GenericRequest.KOL_HOST;
		String urlString = this.formURLString;
		URL context = urlString.startsWith( "http:" ) ? null : GenericRequest.KOL_ROOT;

		if ( Preferences.getBoolean( "allowSocketTimeout" ) && !urlString.startsWith( "valhalla.php" ) )
		{
			return new URL( context, urlString, HttpTimeoutHandler.getInstance() );
		}

		return new URL( context, urlString );
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

		// Only attempt to post something if there's actually data to
		// post - otherwise, opening an input stream should be enough

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
			this.date = this.formConnection.getHeaderField( "Date" );
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
				// The input stream was already closed. Ignore
				// this error and continue.
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
				// If the response code is not 200, then you've
				// read all the information you need.  Close
				// the input stream.

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
			// If the system is down for maintenance, the user must
			// be notified that they should try again later.

			KoLmafia.updateDisplay( KoLConstants.ABORT_STATE, "Nightly maintenance. Please restart KoLmafia." );
			GenericRequest.reset();
			return true;
		}

		// Check to see if this is a login page redirect.  If it is,
		// then construct the URL string and notify the browser that it
		// should change everything.

		if ( this.formURLString.startsWith( "login.php" ) )
		{
			if ( this.redirectLocation.startsWith( "login.php" ) )
			{
				this.constructURLString( this.redirectLocation, false );
				return false;
			}

			Matcher matcher = GenericRequest.REDIRECT_PATTERN.matcher( this.redirectLocation );
			if ( matcher.find() )
			{
				RequestLogger.printLine( "Redirected to " + matcher.group(1) + "..." );
				GenericRequest.setLoginServer( matcher.group( 1 ) );
				this.constructURLString( matcher.group(2), false );
				return false;
			}

			LoginRequest.processLoginRequest( this );
			return true;
		}

		if ( this.redirectLocation.startsWith( "fight.php" ) )
		{
			GenericRequest.checkItemRedirection( this.getURLString() );
			if ( this instanceof UseItemRequest )
			{
				FightRequest.INSTANCE.run();
				return !LoginRequest.isInstanceRunning();
			}
		}

		if ( this.redirectLocation.startsWith( "choice.php" ) )
		{
			GenericRequest.checkItemRedirection( this.getURLString() );
		}

		if ( this instanceof RelayRequest )
		{
			return true;
		}

		if ( this.formURLString.startsWith( "fight.php" ) )
		{
			return true;
		}

		if ( this.redirectLocation.startsWith( "login.php" ) )
		{
			if ( this instanceof LoginRequest )
			{
				this.constructURLString( this.redirectLocation, false );
				return false;
			}

			if ( this.formURLString.startsWith( "logout.php" ) )
			{
				return true;
			}

			if ( LoginRequest.executeTimeInRequest( this.getURLString(), this.redirectLocation ) )
			{
				this.dataChanged = true;
				return false;
			}

			return true;
		}

		if ( this.shouldFollowRedirect() )
		{
			// Re-setup this request to follow the redirect
			// desired and rerun the request.

			this.constructURLString( this.redirectLocation, false );
			return false;
		}

		if ( this.redirectLocation.startsWith( "adventure.php" ) )
		{
			this.constructURLString( this.redirectLocation, false );
			return false;
		}

		if ( this.redirectLocation.startsWith( "fight.php" ) )
		{
			// You have been redirected to a fight! Here, you need
			// to complete the fight before you can continue.

			if ( LoginRequest.isInstanceRunning() ||
			     this == ChoiceManager.CHOICE_HANDLER ||
			     this instanceof AdventureRequest ||
			     this instanceof BasementRequest ||
			     this instanceof HiddenCityRequest )
			{
				FightRequest.INSTANCE.run();
				return !LoginRequest.isInstanceRunning();
			}

			// This is a request which should not have lead to a
			// fight, but it did.  Notify the user.

			KoLmafia.updateDisplay( KoLConstants.ABORT_STATE, "Redirected to a fight page." );
			return true;
		}

		if ( this.redirectLocation.startsWith( "choice.php" ) )
		{
			GenericRequest.handlingChoices = true;
			boolean containsUpdate = ChoiceManager.processChoiceAdventure();
			GenericRequest.handlingChoices = false;
			if ( !containsUpdate )
			{
				CharPaneRequest.getInstance().run();
			}
			return !LoginRequest.isInstanceRunning();
		}

		if ( this.redirectLocation.startsWith( "ocean.php" ) )
		{
			OceanManager.processOceanAdventure();
			return true;
		}

		if ( this.formURLString.startsWith( "sellstuff" ) )
		{
			String redirect = this.redirectLocation;
			String newMode = 
				redirect.startsWith( "sellstuff.php" ) ? "compact" :
				redirect.startsWith( "sellstuff_ugly.php" ) ? "detailed" :
				null;

			if ( newMode != null )
			{
				String message = "Autosell mode changed to " + newMode;
				KoLmafia.updateDisplay( message );
				KoLCharacter.setAutosellMode( newMode );
				return true;
			}
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
		// Find an available byte array in order to buffer the data.
		// Allow this to scale based on the number of incoming requests
		// in order to reduce the probability that the program hangs.

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

		// You are now done with the array.  Go ahead and reset the
		// value to false to let the program know the objects are
		// available to be reused.

		GenericRequest.BYTEFLAGS.set( desiredIndex, Boolean.FALSE );

		if ( this.responseText != null )
		{
			this.processResponse();
		}

		return true;
	}

	/**
	 * This method allows classes to process a raw, unfiltered server response.
	 */

	public void processResponse()
	{
		if ( this.shouldUpdateDebugLog() )
		{
			String text = this.responseText;
			if ( !Preferences.getBoolean( "logReadableHTML" ) )
			{
				text = KoLConstants.LINE_BREAK_PATTERN.matcher( text ).replaceAll( "" );
			}
			RequestLogger.updateDebugLog( text );
		}

		if ( !this.isChatRequest && !this.formURLString.startsWith( "fight.php" ) )
		{
			this.responseText = EventManager.checkForNewEvents( this.responseText );
		}

		if ( GenericRequest.isRatQuest )
		{
			KoLmafia.postTavernVisit( this );
		}

		this.encounter = AdventureRequest.registerEncounter( this );

		if ( this.formURLString.startsWith( "fight.php" ) )
		{
			FightRequest.updateCombatData( this.getURLString(), this.encounter, this.responseText );
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

		// Let clover protection kick in if needed

		if ( ResultProcessor.shouldDisassembleClovers( this.getURLString() ) )
		{
			KoLmafiaCLI.DEFAULT_SHELL.executeCommand( "use", "* ten-leaf clover" );
		}

		if ( !GenericRequest.choiceHandled )
		{
			GenericRequest.choiceHandled = true;
			ChoiceManager.postChoice( this );
		}

		// Once everything is complete, decide whether or not
		// you should refresh your status.

		if ( this.hasNoResult || GenericRequest.suppressUpdate )
		{
			return;
		}

		if ( this instanceof RelayRequest )
		{
			if ( !this.formURLString.startsWith( "basement.php" ) )
			{
				return;
			}

			this.containsUpdate = true;
		}
		else if ( effectCount != KoLConstants.activeEffects.size() || this.getAdventuresUsed() > 0 )
		{
			this.containsUpdate = true;
		}
		else
		{
			this.containsUpdate |= this.responseText.indexOf( "charpane.php" ) != -1;
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
		String urlString = this.getURLString();

		// If this is a lucky adventure, then remove a clover
		// from the player's inventory,
		//
		// Most places, this is signaled by the message "Your (or your)
		// ten-leaf clover disappears in a puff of smoke."
		//
		// In the Sorceress's entryway, the message is "You see a puff
		// of smoke come from your sack, and catch a whiff of burnt
		// clover"

		if ( this.responseText.indexOf( "clover" ) != -1 && this.responseText.indexOf( "puff of smoke" ) != -1 )
		{
			ResultProcessor.processItem( ItemPool.TEN_LEAF_CLOVER, -1 );
		}

		if ( urlString.startsWith( "sewer.php" ) &&
		     this.responseText.indexOf( "You acquire" ) != -1 )
		{
			ResultProcessor.processItem( ItemPool.CHEWING_GUM, -1 );
			if ( urlString.indexOf( "doodit=1" ) != -1 ) 
			{
				ResultProcessor.processItem( ItemPool.TEN_LEAF_CLOVER, -1 );
			}
		}

		if ( urlString.startsWith( "dungeon.php" ) )
		{
			// Unfortunately, the key breaks off in the lock.
			if ( this.responseText.indexOf( "key breaks off in the lock" ) != -1 )
			{
				ResultProcessor.processItem( ItemPool.SKELETON_KEY, -1 );
			}
		}

		if ( urlString.startsWith( "mall.php" ) ||
		     urlString.startsWith( "searchmall.php" ) ||
		     urlString.startsWith( "account.php" ) )
		{
			// These pages cannot possibly contain an actual item
			// drop, but may have a bogus "You acquire an item:" as
			// part of a store name or profile quote.
			this.containsUpdate = false;
		}
		else if ( urlString.startsWith( "mallstore.php" ) )
		{
			// Mall stores themselves can only contain processable
			// results when actually buying an item, and then only
			// at the very top of the page.
			this.containsUpdate = this.getFormField( "whichitem" ) != null &&
				ResultProcessor.processResults( false, this.responseText.substring( 0, this.responseText.indexOf( "</table>" ) ) );
		}
		else
		{
			this.containsUpdate = ResultProcessor.processResults( true, this.responseText );
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

		if ( StaticEntity.isHeadless() )
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

	public static final void checkItemRedirection( final String location )
	{
		GenericRequest.checkItemRedirection( UseItemRequest.extractItem( location ) );
	}

	public static final void checkItemRedirection( final AdventureResult item )
	{
		if ( item == null )
		{
			return;
		}

		int itemId = item.getItemId();
		String itemName = null;
		boolean consumed = false;

		switch ( itemId )
		{
		case ItemPool.BLACK_PUDDING:
			itemName = "Black Pudding";
			Preferences.setInteger( "currentFullness", KoLCharacter.getFullness() - 3 );
			consumed = true;
			break;

		case ItemPool.DRUM_MACHINE:
			itemName = "Drum Machine";
			consumed = true;
			break;

		case ItemPool.CARONCH_MAP:
			itemName = "Cap'm Caronch's Map";
			break;

		case ItemPool.FRATHOUSE_BLUEPRINTS:
			itemName = "Orcish Frathouse Blueprints";
			break;

		case ItemPool.CURSED_PIECE_OF_THIRTEEN:
			itemName = "Cursed Piece of Thirteen";
			break;

		case ItemPool.SPOOKY_PUTTY_MONSTER:
			itemName = "Spooky Putty Monster";
			Preferences.setString( "spookyPuttyMonster", "" );
			ResultProcessor.processItem( ItemPool.SPOOKY_PUTTY_SHEET, 1 );
			consumed = true;
			KoLmafia.ignoreSemirare();
			break;

		case ItemPool.WRETCHED_SEAL:
		case ItemPool.CUTE_BABY_SEAL:
		case ItemPool.ARMORED_SEAL:
		case ItemPool.ANCIENT_SEAL:
		case ItemPool.SLEEK_SEAL:
		case ItemPool.SHADOWY_SEAL:
		case ItemPool.STINKING_SEAL:
		case ItemPool.CHARRED_SEAL:
		case ItemPool.COLD_SEAL:
		case ItemPool.SLIPPERY_SEAL:
			itemName = "Infernal Seal Ritual";
			consumed = true;
			Preferences.increment( "_sealsSummoned", 1 );
			ResultProcessor.processResult( sealRitualCandles( itemId ) );
			break;

		default:
			return;
		}

		if ( consumed )
		{
			ResultProcessor.processResult( item.getInstance( -1 ) );
		}

		int adventure = KoLAdventure.getAdventureCount();
		RequestLogger.printLine();
		RequestLogger.printLine( "[" + adventure + "] " + itemName );

		RequestLogger.updateSessionLog();
		RequestLogger.updateSessionLog( "[" + adventure + "] " + itemName );
	}

	private static final AdventureResult sealRitualCandles( final int itemId )
	{
		switch ( itemId )
		{
		case ItemPool.WRETCHED_SEAL:
			return ItemPool.get( ItemPool.SEAL_BLUBBER_CANDLE, -1 );
		case ItemPool.CUTE_BABY_SEAL:
			return ItemPool.get( ItemPool.SEAL_BLUBBER_CANDLE, -5 );
		case ItemPool.ARMORED_SEAL:
			return ItemPool.get( ItemPool.SEAL_BLUBBER_CANDLE, -10 );
		case ItemPool.ANCIENT_SEAL:
			return ItemPool.get( ItemPool.SEAL_BLUBBER_CANDLE, -3 );
		case ItemPool.SLEEK_SEAL:
		case ItemPool.SHADOWY_SEAL:
		case ItemPool.STINKING_SEAL:
		case ItemPool.CHARRED_SEAL:
		case ItemPool.COLD_SEAL:
		case ItemPool.SLIPPERY_SEAL:
			return ItemPool.get( ItemPool.IMBUED_SEAL_BLUBBER_CANDLE, -1 );
		}
		return null;
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

	private static String lastUserAgent = "";

	public static final void saveUserAgent( final String agent )
	{
		if ( !agent.equals( GenericRequest.lastUserAgent ) )
		{
			GenericRequest.lastUserAgent = agent;
			Preferences.setString( "lastUserAgent", agent );
		}
	}

	public static final void setUserAgent()
	{
		String agent = "";
		if ( Preferences.getBoolean( "useLastUserAgent" ) )
		{
			agent = Preferences.getString( "lastUserAgent" );
		}
		if ( agent.equals( "" ) )
		{
			agent = KoLConstants.VERSION_NAME;
		}
		GenericRequest.setUserAgent( agent );
	}

	public static final void setUserAgent( final String agent )
	{
		if ( !agent.equals( GenericRequest.userAgent ) )
		{
			GenericRequest.userAgent = agent;
			System.setProperty( "http.agent", GenericRequest.userAgent );
		}

		// Get rid of obsolete setting
		Preferences.setString( "userAgent", "" );
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
