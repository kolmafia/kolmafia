/*
 * Copyright (c) 2005-2021, KoLmafia development team
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
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.net.UnknownHostException;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import java.util.zip.GZIPInputStream;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLAdventure;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.KoLmafiaASH;
import net.sourceforge.kolmafia.KoLmafiaCLI;
import net.sourceforge.kolmafia.MonsterData;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.SpecialOutfit.Checkpoint;
import net.sourceforge.kolmafia.StaticEntity;

import net.sourceforge.kolmafia.chat.ChatPoller;
import net.sourceforge.kolmafia.chat.InternalMessage;

import net.sourceforge.kolmafia.listener.PreferenceListenerRegistry;

import net.sourceforge.kolmafia.moods.RecoveryManager;

import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.objectpool.SkillPool;

import net.sourceforge.kolmafia.persistence.AdventureDatabase;
import net.sourceforge.kolmafia.persistence.ConcoctionDatabase;
import net.sourceforge.kolmafia.persistence.EquipmentDatabase;
import net.sourceforge.kolmafia.persistence.MonsterDatabase;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.session.ChoiceManager;
import net.sourceforge.kolmafia.session.EncounterManager;
import net.sourceforge.kolmafia.session.EquipmentManager;
import net.sourceforge.kolmafia.session.EventManager;
import net.sourceforge.kolmafia.session.InventoryManager;
import net.sourceforge.kolmafia.session.LightsOutManager;
import net.sourceforge.kolmafia.session.OceanManager;
import net.sourceforge.kolmafia.session.QuestManager;
import net.sourceforge.kolmafia.session.ResponseTextParser;
import net.sourceforge.kolmafia.session.ResultProcessor;
import net.sourceforge.kolmafia.session.TurnCounter;
import net.sourceforge.kolmafia.session.ValhallaManager;

import net.sourceforge.kolmafia.swingui.RequestSynchFrame;

import net.sourceforge.kolmafia.textui.ScriptRuntime;

import net.sourceforge.kolmafia.textui.parsetree.Value;

import net.sourceforge.kolmafia.utilities.ByteBufferUtilities;
import net.sourceforge.kolmafia.utilities.FileUtilities;
import net.sourceforge.kolmafia.utilities.InputFieldUtilities;
import net.sourceforge.kolmafia.utilities.NaiveSecureSocketLayer;
import net.sourceforge.kolmafia.utilities.PauseObject;
import net.sourceforge.kolmafia.utilities.StringUtilities;

import net.sourceforge.kolmafia.webui.BarrelDecorator;
import net.sourceforge.kolmafia.webui.RelayAgent;
import net.sourceforge.kolmafia.webui.RelayServer;

public class GenericRequest
	implements Runnable
{
	// Used in many requests. Here for convenience and non-duplication
	public static final Pattern ACTION_PATTERN = Pattern.compile( "action=([^&]*)" );
	public static final Pattern PLACE_PATTERN = Pattern.compile( "place=([^&]*)" );
	public static final Pattern WHICHITEM_PATTERN = Pattern.compile( "whichitem=(\\d+)" );
	public static final Pattern HOWMANY_PATTERN = Pattern.compile( "howmany=(\\d+)" );
	public static final Pattern QUANTITY_PATTERN = Pattern.compile( "quantity=(\\d+)" );
	public static final Pattern QTY_PATTERN = Pattern.compile( "qty=(\\d+)" );
	public static final Pattern WHICHROW_PATTERN = Pattern.compile( "whichrow=(\\d+)" );

	private int timeoutCount = 0;
	private static final int TIMEOUT_LIMIT = 3;

	private boolean redirectHandled = false;
	private int redirectCount = 0;
	private static final int REDIRECT_LIMIT = 5;

	private Boolean allowRedirect = null;

	public static final Pattern REDIRECT_PATTERN = Pattern.compile( "([^\\/]*)\\/(login\\.php.*)", Pattern.DOTALL );
	public static final Pattern JS_REDIRECT_PATTERN =
		Pattern.compile( ">\\s*top.mainpane.document.location\\s*=\\s*\"(.*?)\";" );

	protected String encounter = "";

	public static final int MENU_FANCY = 1;
	public static final int MENU_COMPACT = 2;
	public static final int MENU_NORMAL = 3;
	public static int topMenuStyle = 0;

	public static final String[] SERVERS =
	{
		"devproxy.kingdomofloathing.com",
		"www.kingdomofloathing.com"
	};

	public static final String KOL_IP = "69.16.150.211";
	public static String KOL_HOST = GenericRequest.SERVERS[ 1 ];
	public static URL KOL_SECURE_ROOT = null;

	private URL formURL;
	private String currentHost;
	private String formURLString;
	private String baseURLString;

	private boolean hasResult;

	public boolean isExternalRequest = false;
	public boolean isRootsetRequest = false;
	public boolean isTopmenuRequest = false;
	public boolean isChatRequest = false;
	public boolean isChatLaunchRequest = false;
	public boolean isDescRequest = false;
	public boolean isStaticRequest = false;
	public boolean isQuestLogRequest = false;

	protected List<String> data;
	private boolean dataChanged = true;
	private byte[] dataString = null;

	public int responseCode;
	public String responseMessage;
	public String responseText;
	public HttpURLConnection formConnection;
	public String redirectLocation;
	public String redirectMethod;

	// Per-login data

	private static String userAgent = "";
	public static final Set<ServerCookie> serverCookies = new LinkedHashSet<ServerCookie>();
	public static String sessionId = null;
	public static String passwordHash = "";
	public static String passwordHashValue = "";

	// *** static class variables are always suspect
	public static boolean isRatQuest = false;
	public static boolean isBarrelSmash = false;
	public static boolean ascending = false;
	public static String itemMonster = null;
	private static boolean suppressUpdate = false;
	private static boolean ignoreChatRequest = false;

	public static void reset()
	{
		GenericRequest.setUserAgent();
		GenericRequest.serverCookies.clear();
		GenericRequest.sessionId = null;
		GenericRequest.passwordHash = "";
		GenericRequest.passwordHashValue = "";
	}

	public static void setPasswordHash( final String hash )
	{
		GenericRequest.passwordHash = hash;
		GenericRequest.passwordHashValue = "=" + hash;
	}

	/**
	 * static final method called when <code>GenericRequest</code> is first instantiated or whenever the settings have
	 * changed. This initializes the login server to the one stored in the user's settings, as well as initializes the
	 * user's proxy settings.
	 */

	public static final void applySettings()
	{
		Properties systemProperties = System.getProperties();

		systemProperties.put( "java.net.preferIPv4Stack", "true" );

		GenericRequest.applyProxySettings();

		boolean useDevProxyServer = Preferences.getBoolean( "useDevProxyServer" );

		GenericRequest.setLoginServer( GenericRequest.SERVERS[ useDevProxyServer ? 0 : 1 ] );

		// Disable this, since it causes unrecoverable problems in
		// situations of lag, rather than simply slowing things down
		boolean allowSocketTimeout = false; // Preferences.getBoolean( "allowSocketTimeout" );

		if ( allowSocketTimeout )
		{
			systemProperties.put( "sun.net.client.defaultConnectTimeout", "10000" );
			systemProperties.put( "sun.net.client.defaultReadTimeout", "120000" );
		}
		else
		{
			systemProperties.remove( "sun.net.client.defaultConnectTimeout" );
			systemProperties.remove( "sun.net.client.defaultReadTimeout" );
		}

		if ( Preferences.getBoolean( "useNaiveSecureLogin" ) || Preferences.getBoolean( "connectViaAddress" ) )
		{
			NaiveSecureSocketLayer.install();
		}
		else
		{
			NaiveSecureSocketLayer.uninstall();
		}

		systemProperties.put( "http.referer", "https://" + GenericRequest.KOL_HOST + "/game.php" );
	}

	private static void applyProxySettings()
	{
		GenericRequest.applyProxySettings( "http" );
		GenericRequest.applyProxySettings( "https" );
	}

	private static void applyProxySettings( String protocol )
	{
		if ( System.getProperty( "os.name" ).startsWith( "Mac" ) )
		{
			return;
		}

		Properties systemProperties = System.getProperties();

		String proxySet = Preferences.getString( "proxySet" );
		String proxyHost = Preferences.getString( protocol + ".proxyHost" );
		String proxyPort = Preferences.getString( protocol + ".proxyPort" );
		String proxyUser = Preferences.getString( protocol + ".proxyUser" );
		String proxyPassword = Preferences.getString( protocol + ".proxyPassword" );

		// Remove the proxy host from the system properties
		// if one isn't specified, or proxy setting is off.

		if ( proxySet.equals( "false" ) || proxyHost.equals( "" ) )
		{
			systemProperties.remove( protocol + ".proxyHost" );
			systemProperties.remove( protocol + ".proxyPort" );
		}
		else
		{
			try
			{
				proxyHost = InetAddress.getByName( proxyHost ).getHostAddress();
			}
			catch ( UnknownHostException e )
			{
				// This should not happen.  Therefore, print
				// a stack trace for debug purposes.

				StaticEntity.printStackTrace( e, "Error in proxy setup" );
			}

			systemProperties.put( protocol + ".proxyHost", proxyHost );
			systemProperties.put( protocol + ".proxyPort", proxyPort );
		}

		// Remove the proxy user from the system properties
		// if one isn't specified, or proxy setting is off.

		if ( proxySet.equals( "false" ) || proxyHost.equals( "" ) || proxyUser.equals( "" ) )
		{
			systemProperties.remove( protocol + ".proxyUser" );
			systemProperties.remove( protocol + ".proxyPassword" );
		}
		else
		{
			systemProperties.put( protocol + ".proxyUser", proxyUser );
			systemProperties.put( protocol + ".proxyPassword", proxyPassword );
		}
	}

	private static boolean substringMatches( final String a, final String b )
	{
		return a.contains( b ) || b.contains( a );
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
			if ( GenericRequest.substringMatches( server, GenericRequest.SERVERS[ i ] ) )
			{
				GenericRequest.setLoginServer( i );
				return;
			}
		}
	}

	private static void setLoginServer( final int serverIndex )
	{
		GenericRequest.KOL_HOST = GenericRequest.SERVERS[ serverIndex ];

		try
		{
			if ( Preferences.getBoolean( "connectViaAddress" ) )
			{
				GenericRequest.KOL_SECURE_ROOT = new URL( "https", GenericRequest.KOL_IP, 443, "/" );
			}
			else
			{
				GenericRequest.KOL_SECURE_ROOT = new URL( "https", GenericRequest.KOL_HOST, 443, "/" );
			}
		}
		catch ( IOException e )
		{
			StaticEntity.printStackTrace( e );
		}

		Preferences.setString( "loginServerName", GenericRequest.KOL_HOST );
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
	 * Constructs a new GenericRequest which will notify the given client of any changes and will use the given URL for
	 * data submission.
	 * 
	 * @param newURLString The form to be used in posting data
	 */

	public GenericRequest( final String newURLString, final boolean usePostMethod )
	{
		this.data = Collections.synchronizedList( new ArrayList<String>() );
		if ( !newURLString.equals( "" ) )
		{
			this.constructURLString( newURLString, usePostMethod );
		}
	}

	public GenericRequest( final String newURLString )
	{
		this( newURLString, true );
	}

	public static void suppressUpdate( final boolean suppressUpdate )
	{
		GenericRequest.suppressUpdate = suppressUpdate;
	}

	public static boolean updateSuppressed()
	{
		return GenericRequest.suppressUpdate;
	}

	public GenericRequest cloneURLString( final GenericRequest req )
	{
		String newURLString = req.getFullURLString();
		boolean usePostMethod = !req.data.isEmpty();
		boolean encoded = true;
		return this.constructURLString( newURLString, usePostMethod, encoded );
	}

	public GenericRequest constructURLString( final String newURLString )
	{
		return this.constructURLString( newURLString, true, false );
	}

	public GenericRequest constructURLString( final String newURLString, final boolean usePostMethod )
	{
		return this.constructURLString( newURLString, usePostMethod, false );
	}

	public GenericRequest constructURLString( String newURLString, final boolean usePostMethod, final boolean encoded )
	{
		this.responseText = null;
		this.dataChanged = true;
		this.data.clear();

		String oldURLString = this.formURLString;

		int formSplitIndex = newURLString.indexOf( "?" );
		String queryString = null;

		if ( formSplitIndex == -1 )
		{
			this.baseURLString = newURLString;
		}
		else
		{
			this.baseURLString = GenericRequest.decodePath( newURLString.substring( 0, formSplitIndex ) );

			queryString = newURLString.substring( formSplitIndex + 1 );
		}

		while ( this.baseURLString.startsWith( "/" ) || this.baseURLString.startsWith( "." ) )
		{
			this.baseURLString = this.baseURLString.substring( 1 );
		}

		this.isExternalRequest = ( this.baseURLString.startsWith( "http://" ) || this.baseURLString.startsWith( "https://" ) );

		if ( queryString == null )
		{
			this.formURLString = this.baseURLString;
		}
		else if ( !usePostMethod )
		{
			this.formURLString = this.baseURLString + "?" + queryString;
		}
		else
		{
			this.formURLString = this.baseURLString;
			this.addFormFields( queryString, encoded );
		}

		if ( !this.formURLString.equals( oldURLString ) )
		{
			this.currentHost = GenericRequest.KOL_HOST;
			this.formURL = null;
		}

		this.isRootsetRequest = this.formURLString.startsWith( "game.php" );

		this.isTopmenuRequest =
			this.formURLString.startsWith( "topmenu.php" ) ||
			this.formURLString.startsWith( "awesomemenu.php" );

		this.isChatRequest =
			this.formURLString.startsWith( "newchatmessages.php" ) ||
			this.formURLString.startsWith( "submitnewchat.php" );

		this.isChatLaunchRequest = this.formURLString.startsWith( "chatlaunch.php" );

		this.isDescRequest = this.formURLString.startsWith( "desc_" );

		this.isStaticRequest =
			this.formURLString.startsWith( "doc.php" ) ||
			this.formURLString.startsWith( "static.php" );

		this.isQuestLogRequest = this.formURLString.startsWith( "questlog.php" );

		return this;
	}

	/**
	 * Returns the location of the form being used for this URL, in case it's ever needed/forgotten.
	 */

	public String getURLString()
	{
		return this.data.isEmpty() ?
			StringUtilities.singleStringReplace( this.formURLString, GenericRequest.passwordHashValue, "" ) :
			this.formURLString + "?" + this.getDisplayDataString();
	}

	public String getFullURLString()
	{
		return this.data.isEmpty() ?
			this.formURLString :
			this.formURLString + "?" + this.getDataString();
	}

	/**
	 * Clears the data fields so that the descending class can have a fresh set of data fields. This allows requests
	 * with variable numbers of parameters to be reused.
	 */

	public void clearDataFields()
	{
		this.data.clear();
	}

	public void setDataChanged()
	{
		this.dataChanged = true;
	}

	public void addFormFields( final String fields, final boolean encoded )
	{
		if ( !fields.contains( "&" ) )
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
	 * Adds the given form field to the GenericRequest. Descendant classes should use this method if they plan on
	 * submitting forms to Kingdom of Loathing before a call to the <code>super.run()</code> method. Ideally, these
	 * fields can be added at construction time.
	 * 
	 * @param name The name of the field to be added
	 * @param value The value of the field to be added
	 * @param allowDuplicates true if duplicate names are OK
	 */

	public void addFormField( final String name, final String value, final boolean allowDuplicates )
	{
		this.dataChanged = true;

		String charset = this.isChatRequest ? "ISO-8859-1" : "UTF-8";

		String encodedName = name + "=";
		String encodedValue = value == null ? "" : GenericRequest.encodeURL( value, charset );

		// Make sure that when you're adding data fields, you don't
		// submit duplicate fields.

		if ( !allowDuplicates )
		{
			synchronized ( this.data )
			{
				Iterator<String> it = this.data.iterator();
				while ( it.hasNext() )
				{
					if ( it.next().startsWith( encodedName ) )
					{
						it.remove();
					}
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
		// multi-step recipe, you get the following as the path:
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
		// craft.php?mode=cook&steps%5B%5D=2262%2C2263&steps%5B%5D=2264%2C2265&action=craft&qty=1&pwd
		//
		// One additional wrinkle: we now see the following URL:
		//
		// craft.php?mode=combine&steps%5B%5D=118,119&steps%5B%5D=120,121
		//
		// given the following POST data:
		//
		// mode=combine&pwd=5a88021883a86d2b669654f79598101e&action=craft&steps%255B%255D=118%2C119&steps%255B%255D=120%2C121&qty=1
		//
		// Notice that the URL is actually NOT encoded and the POST
		// data IS encoded. So, %255B -> %5B

		int equalIndex = element.indexOf( "=" );
		if ( equalIndex != -1 )
		{
			String name = element.substring( 0, equalIndex ).trim();
			String value = element.substring( equalIndex + 1 ).trim();
			String charset = this.isChatRequest ? "ISO-8859-1" : "UTF-8";

			// The name may or may not be encoded.
			name = GenericRequest.decodeField( name, "UTF-8" );
			value = GenericRequest.decodeField( value, charset );

			// But we want to always submit value encoded.
			value = GenericRequest.encodeURL( value, charset );

			element = name + "=" + value;
		}

		synchronized ( this.data )
		{
			Iterator<String> it = this.data.iterator();
			while ( it.hasNext() )
			{
				if ( it.next().equals( element ) )
				{
					return;
				}
			}
		}

		this.data.add( element );
	}

	public List<String> getFormFields()
	{
		if ( !this.data.isEmpty() )
		{
			return this.data;
		}

		int index = this.formURLString.indexOf( "?" );
		if ( index == -1 )
		{
			return Collections.emptyList();
		}

		String[] tokens = this.formURLString.substring( index + 1 ).split( "&" );
		List<String> fields = new ArrayList<String>();
		for ( int i = 0; i < tokens.length; ++i )
		{
			fields.add( tokens[ i ] );
		}
		return fields;
	}

	public String getFormField( final String key )
	{
		return this.findField( this.getFormFields(), key, true );
	}

	public String getFormField( final String key, final boolean decode )
	{
		return this.findField( this.getFormFields(), key, decode );
	}

	private String findField( final List<String> data, final String key, final boolean decode )
	{
		for ( int i = 0; i < data.size(); ++i )
		{
			String datum = data.get( i );

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

			if ( decode )
			{
				// Chat was encoded as ISO-8859-1, so decode it that way.
				String charset = this.isChatRequest ? "ISO-8859-1" : "UTF-8";
				return GenericRequest.decodeField( value, charset );
			}
			return value;
		}

		return null;
	}

	public static String decodePath( final String urlString )
	{
		if ( urlString == null )
		{
			return null;
		}

		String oldURLString = null;
		String newURLString = urlString;

		try
		{
			do
			{
				oldURLString = newURLString;
				newURLString = URLDecoder.decode( oldURLString, "UTF-8" );
			}
			while ( !oldURLString.equals( newURLString ) );
		}
		catch ( IOException e )
		{
		}

		return newURLString;
	}

	public static String decodeField( final String urlString )
	{
		return GenericRequest.decodeField( urlString, "UTF-8" );
	}

	public static String decodeField( final String value, final String charset )
	{
		if ( value == null )
		{
			return null;
		}

		try
		{
			return URLDecoder.decode( value, charset );
		}
		catch ( IOException e )
		{
			return value;
		}
	}

	public static String encodeURL( final String urlString )
	{
		return GenericRequest.encodeURL( urlString, "UTF-8" );
	}

	public static String encodeURL( final String urlString, final String charset )
	{
		if ( urlString == null )
		{
			return null;
		}

		try
		{
			return URLEncoder.encode( urlString, charset );
		}
		catch ( IOException e )
		{
			return urlString;
		}
	}

	public void removeFormField( final String name )
	{
		if ( name == null )
		{
			return;
		}

		this.dataChanged = true;

		String encodedName = name + "=";

		synchronized ( this.data )
		{
			Iterator<String> it = this.data.iterator();
			while ( it.hasNext() )
			{
				if ( it.next().startsWith( encodedName ) )
				{
					it.remove();
				}
			}
		}
	}

	public String getPath()
	{
		return this.formURLString;
	}

	public String getBasePath()
	{
		String path = this.formURLString;
		if ( path == null )
		{
			return null;
		}
		int quest = path.indexOf( "?" );
		return quest != -1 ? path.substring( 0, quest ) : path;
	}

	public boolean hasResult()
	{
		return this.hasResult( this.getURLString() );
	}

	public boolean hasResult( String location )
	{
		return !this.isExternalRequest && ResponseTextParser.hasResult( location );
	}

	public void setHasResult( final boolean change )
	{
		this.hasResult = change;
	}

	public String getHashField()
	{
		return ( !this.isExternalRequest ? "pwd" : null );
	}

	private String getDataString()
	{
		// This returns the data string as we will submit it to KoL: if
		// the request wants us to include the password hash, we
		// include the actual value

		StringBuilder dataBuffer = new StringBuilder();
		String hashField = this.getHashField();

		synchronized ( this.data )
		{
			for ( int i = 0; i < this.data.size(); ++i )
			{
				String element = this.data.get( i );

				if ( element.equals( "" ) )
				{
					continue;
				}

				if ( hashField != null && element.startsWith( hashField ) )
				{
					int index = element.indexOf( '=' );
					int length = hashField.length();

					// If this is exactly the hashfield, either
					// with or without a value, omit it.
					if ( length == ( index == -1 ? element.length() : length ) )
					{
						continue;
					}
				}

				if ( dataBuffer.length() > 0 )
				{
					dataBuffer.append( '&' );
				}

				dataBuffer.append( element );
			}
		}

		if ( hashField != null && !GenericRequest.passwordHash.equals( "" ) )
		{
			if ( dataBuffer.length() > 0 )
			{
				dataBuffer.append( '&' );
			}

			dataBuffer.append( hashField );
			dataBuffer.append( '=' );
			dataBuffer.append( GenericRequest.passwordHash );
		}

		return dataBuffer.toString();
	}

	private String getDisplayDataString()
	{
		// This returns the data string as we will display it in the
		// logs: omitting the actual boring value of the password hash

		StringBuilder dataBuffer = new StringBuilder();

		synchronized ( this.data )
		{
			for ( int i = 0; i < this.data.size(); ++i )
			{
				String element = this.data.get( i );

				if ( element.equals( "" ) )
				{
					continue;
				}

				if ( !this.isExternalRequest )
				{
					if ( element.startsWith( "pwd=" ) )
					{
						element = "pwd";
					}
					else if ( element.startsWith( "phash=" ) )
					{
						element = "phash";
					}
					else if ( element.startsWith( "password=" ) )
					{
						element = "password";
					}
				}

				if ( dataBuffer.length() > 0 )
				{
					dataBuffer.append( '&' );
				}

				dataBuffer.append( element );
			}
		}

		return dataBuffer.toString();
	}

	public static final String removeField( final String urlString, final String field )
	{
		int start = urlString.indexOf( field );
		if ( start == -1 )
		{
			return urlString;
		}

		int end = urlString.indexOf( "&", start );
		if ( end == -1 )
		{
			String prefix = urlString.substring( 0, start - 1 );
			return prefix;
		}

		String prefix = urlString.substring( 0, start );
		String suffix = urlString.substring( end + 1 );
		return prefix + suffix;
	}

	public static final String extractField( final String urlString, final String field )
	{
		int start = urlString.indexOf( field );
		if ( start == -1 )
		{
			return null;
		}

		int end = urlString.indexOf( "&", start );
		return ( end == -1 ) ?
			urlString.substring( start ) :
			urlString.substring( start, end );
	}

	private boolean shouldUpdateDebugLog()
	{
		return RequestLogger.isDebugging() && ( !this.isChatRequest || Preferences.getBoolean( "logChatRequests" ) );
	}

	public boolean stopForCounters()
	{
		while ( true )
		{
			TurnCounter expired = TurnCounter.getExpiredCounter( this, true );
			while ( expired != null )
			{
				// Process all expiring informational counters
				// first.  This strategy has the best chance of
				// not screwing everything up totally if both
				// informational and aborting counters expire
				// on the same turn.
				KoLmafia.updateDisplay( "(" + expired.getLabel() + " counter expired)" );
				this.invokeCounterScript( expired );
				expired = TurnCounter.getExpiredCounter( this, true );
			}

			expired = TurnCounter.getExpiredCounter( this, false );
			if ( expired == null )
			{
				break;
			}

			int remain = expired.getTurnsRemaining();
			if ( remain < 0 )
			{
				continue;
			}

			TurnCounter also;
			while ( ( also = TurnCounter.getExpiredCounter( this, false ) ) != null )
			{
				if ( also.getTurnsRemaining() < 0 )
				{
					continue;
				}
				if ( also.getLabel().equals( "Fortune Cookie" ) )
				{
					KoLmafia.updateDisplay( "(" + expired.getLabel() + " counter discarded due to conflict)" );
					expired = also;
				}
				else
				{
					KoLmafia.updateDisplay( "(" + also.getLabel() + " counter discarded due to conflict)" );
				}
			}

			if ( this.invokeCounterScript( expired ) )
			{
				// Abort if between battle actions fail
				if ( !KoLmafia.permitsContinue() )
				{
					return true;
				}
				continue;
			}

			String message;
			if ( remain == 0 )
			{
				message = expired.getLabel() + " counter expired.";
			}
			else
			{
				message =
					expired.getLabel() + " counter will expire after " + remain + " more turn" + ( remain == 1 ? "." : "s." );
			}

			if ( expired.getLabel().equals( "Fortune Cookie" ) )
			{
				message += " " + EatItemRequest.lastSemirareMessage();
			}

			else if ( expired.getLabel().equals( "Spookyraven Lights Out" ) )
			{
				message += " " + LightsOutManager.message();
			}

			if ( !Preferences.getBoolean( "dontStopForCounters" ) )
			{
				KoLmafia.updateDisplay( MafiaState.ERROR, message );
				return true;
			}
		}

		return false;
	}

	private boolean invokeCounterScript( final TurnCounter expired )
	{
		String scriptName = Preferences.getString( "counterScript" );
		if ( scriptName.length() == 0 )
		{
			return false;
		}

		String functionName = "main";
		int atsign = scriptName.indexOf( "@" );
		if ( atsign != -1 )
		{
			functionName = scriptName.substring( 0, atsign );
			scriptName = scriptName.substring( atsign + 1 );
		}

		List<File> scriptFiles = KoLmafiaCLI.findScriptFile( scriptName );
		ScriptRuntime interpreter = KoLmafiaASH.getInterpreter( scriptFiles );
		if ( interpreter != null )
		{
			// Clear abort state so counter script and between
			// battle actions are not hindered.
			KoLmafia.forceContinue();

			KoLAdventure current = KoLAdventure.lastVisitedLocation;
			int oldTurns = KoLCharacter.getCurrentRun();

			String[] arguments = new String[] {expired.getLabel(), String.valueOf( expired.getTurnsRemaining() )};
			boolean executeTopLevel = functionName.equals( "main" );

			KoLmafiaASH.logScriptExecution( "Starting counter script: ", scriptName, interpreter );
			Value v = interpreter.execute( functionName, arguments, executeTopLevel );
			KoLmafiaASH.logScriptExecution( "Finished counter script: ", scriptName, interpreter );

			// If the counter script used adventures, we need to
			// run between-battle actions for the next adventure,
			// in order to maintain moods

			if ( KoLCharacter.getCurrentRun() != oldTurns )
			{
				KoLAdventure.setNextAdventure( current );
				RecoveryManager.runBetweenBattleChecks( true );
			}

			return v != null && v.intValue() != 0;
		}

		return false;
	}

	public static String getAction( final String urlString )
	{
		Matcher matcher = GenericRequest.ACTION_PATTERN.matcher( urlString );
		return matcher.find() ? GenericRequest.decodeField( matcher.group( 1 ) ) : null;
	}

	public static String getPlace( final String urlString )
	{
		Matcher matcher = GenericRequest.PLACE_PATTERN.matcher( urlString );
		return matcher.find() ? GenericRequest.decodeField( matcher.group( 1 ) ) : null;
	}

	public static final Pattern HOWMUCH_PATTERN = Pattern.compile( "howmuch=([^&]*)" );
	public static final int getHowMuch( final String urlString )
	{
		return GenericRequest.getNumericField( urlString, GenericRequest.HOWMUCH_PATTERN );
	}

	public static final int getWhichItem( final String urlString )
	{
		return GenericRequest.getNumericField( urlString, GenericRequest.WHICHITEM_PATTERN );
	}

	public static final int getNumericField( final String urlString, final Pattern pattern )
	{
		Matcher matcher = pattern.matcher( urlString );
		if ( matcher.find() )
		{
			// KoL allows any old crap in the input field. It
			// strips out non-numeric characters and treats the
			// rest as an integer.
			String field = GenericRequest.decodeField( matcher.group( 1 ) );
			try
			{
				return StringUtilities.parseIntInternal2( field );
			}
			catch ( NumberFormatException e )
			{
			}
		}
		return -1;
	}

	public void reconstructFields()
	{
	}

	public static final boolean abortIfInFightOrChoice()
	{
		return GenericRequest.abortIfInFightOrChoice( false );
	}

	public static final boolean abortIfInFight( final boolean silent )
	{
		if ( FightRequest.currentRound != 0 )
		{
			if ( !silent )
			{
				KoLmafia.updateDisplay( MafiaState.ERROR, "You are currently in a fight." );
			}
			// StaticEntity.printStackTrace( "You are currently in a fight." );
			return true;
		}

		if ( FightRequest.inMultiFight )
		{
			if ( !silent )
			{
				KoLmafia.updateDisplay( MafiaState.ERROR, "You are currently in a multi-stage fight." );
			}
			// StaticEntity.printStackTrace( "You are currently in a multi-stage fight." );
			return true;
		}

		if ( FightRequest.choiceFollowsFight )
		{
			if ( !silent )
			{
				KoLmafia.updateDisplay( MafiaState.ERROR, "A choice follows this fight immediately." );
			}
			// StaticEntity.printStackTrace( "A choice follows this fight immediately." );
			return true;
		}

		return false;
	}

	public static final boolean abortIfInChoice( final boolean silent )
	{
		if ( ChoiceManager.handlingChoice && !ChoiceManager.canWalkAway() )
		{
			if ( !silent )
			{
				KoLmafia.updateDisplay( MafiaState.ERROR, "You are currently in a choice." );
			}
			// StaticEntity.printStackTrace( "You are currently in a choice." );
			return true;
		}

		return false;
	}

	public static final boolean abortIfInFightOrChoice( final boolean silent )
	{
		return GenericRequest.abortIfInFight( silent) || GenericRequest.abortIfInChoice( silent);
	}

	/**
	 * Runs the thread, which prepares the connection for output, posts the data to the Kingdom of Loathing, and
	 * prepares the input for reading. Because the Kingdom of Loathing has identical page layouts, all page reading and
	 * handling will occur through these method calls.
	 */

	public void run()
	{
		if ( GenericRequest.sessionId == null &&
		     !( this instanceof LoginRequest ) &&
		     !( this instanceof LogoutRequest ) )
		{
			return;
		}

		if ( this.isChatRequest && GenericRequest.ignoreChatRequest )
		{
			return;
		}

		GenericRequest.ignoreChatRequest = false;

		this.timeoutCount = 0;
		this.redirectHandled = false;
		this.redirectCount = 0;
		this.allowRedirect = null;

		String location = this.getURLString();

		if ( StaticEntity.backtraceTrigger != null &&
		     location.contains( StaticEntity.backtraceTrigger ) )
		{
			StaticEntity.printStackTrace( "Backtrace triggered by page load" );
		}

		// Calculate this exactly once, now that we have the URL
		this.hasResult = this.hasResult( location );

		if ( this.hasResult && this.stopForCounters() )
		{
			return;
		}

		if ( this.shouldUpdateDebugLog() )
		{
			RequestLogger.updateDebugLog( this.getClass() );
		}

		if ( this.isExternalRequest )
		{
			this.externalExecute();
		}
		else if ( !this.prepareForURL( location ) )
		{
			return;
		}
		else
		{
			this.execute();
		}

		if ( ( this.responseCode == 200 && this.responseText != null ) ||
		     ( this.responseCode == 302 && this.redirectLocation != null ) )
		{
			// Call central dispatch method for locations that require
			// special handling

			QuestManager.handleQuestChange( this );
		}

		// Normal response?
		if ( this.responseCode == 200 )
		{
			if ( this.responseText == null )
			{
				KoLmafia.updateDisplay(
					MafiaState.ABORT,
					"Server " + GenericRequest.KOL_HOST + " returned a blank page from " + this.getBasePath() + ". Complain to Jick, not us." );
			}
			else
			{
				this.formatResponse();
			}
			QuantumTerrariumRequest.checkCounter( this );
			return;
		}

		// Redirect?
		if ( this.responseCode == 302 )
		{
			if ( this.redirectLocation == null )
			{
				KoLmafia.updateDisplay(
					MafiaState.ABORT,
					"Server " + GenericRequest.KOL_HOST + " returned 302 without a redirect location" );
			}
			else if ( this instanceof RelayRequest )
			{
				// We are letting the browser handle redirects
			}
			else if ( this.redirectHandled )
			{
				// Redirect passed off to another request
			}
			else if ( this.redirectCount >= GenericRequest.REDIRECT_LIMIT )
			{
				KoLmafia.updateDisplay(
					MafiaState.ABORT,
					"Too many server redirects (" + this.redirectCount + "); current redirect location = " + this.redirectLocation );
			}
			else
			{
				if ( !this.redirectLocation.equals( "/game.php" ) &&
				     !this.redirectLocation.equals( "witchess.php" ) &&
				     !this.redirectLocation.equals( "place.php?whichplace=monorail" ) &&
				     !this.redirectLocation.equals( "place.php?whichplace=edunder" ) &&
				     !this.redirectLocation.equals( "/shop.php?whichshop=fwshop" ) )
				{
					// Informational debug message
					KoLmafia.updateDisplay( "Unhandled redirect to " + this.redirectLocation );
				}
			}
			return;
		}

		// Something else
		return;
	}

	private boolean prepareForURL( final String location )
	{
		// This method returns true is we should proceed to submit the URL
		// We attempt to do any setup needed.

		if ( location.startsWith( "hermit.php?auto" ) )
		{
			// auto-buying chewing gum or permits overrides the
			// setting that disables NPC purchases, since the user
			// explicitly requested the purchase.
			boolean old = Preferences.getBoolean( "autoSatisfyWithNPCs" );
			try
			{
				if ( !old )
				{
					Preferences.setBoolean( "autoSatisfyWithNPCs", true );
				}

				// If he wants us to automatically get a worthless item
				// in the sewer, do it.
				if ( location.contains( "autoworthless=on" ) )
				{
					InventoryManager.retrieveItem( HermitRequest.WORTHLESS_ITEM, false );
				}

				// If he wants us to automatically get a hermit permit, if needed, do it.
				// If he happens to have a hermit script, use it and obviate permits
				if ( location.contains( "autopermit=on" ) )
				{
					if ( InventoryManager.hasItem( HermitRequest.HACK_SCROLL ) )
					{
						RequestThread.postRequest( UseItemRequest.getInstance( HermitRequest.HACK_SCROLL ) );
					}
					InventoryManager.retrieveItem( ItemPool.HERMIT_PERMIT, false );
				}
			}
			finally
			{
				if ( !old )
				{
					Preferences.setBoolean( "autoSatisfyWithNPCs", false );
				}
			}

			return true;
		}

		if ( location.startsWith( "casino.php" ) )
		{
			if ( !KoLCharacter.inZombiecore() )
			{
				InventoryManager.retrieveItem( ItemPool.CASINO_PASS );
			}
			return true;
		}

		if ( location.equals( "place.php?whichplace=orc_chasm&action=bridge0" ) ||
		     location.equals( "place.php?whichplace=orc_chasm&action=label1" ))
		{
			InventoryManager.retrieveItem( ItemPool.BRIDGE );
			return true;
		}

		if ( location.startsWith( "place.php?whichplace=desertbeach&action=db_pyramid1" ) ||
		     location.startsWith( "place.php?whichplace=exploathing_beach&action=expl_pyramidpre" ) )
		{
			// This is the normal one, not the one Ed wields
			ResultProcessor.autoCreate( ItemPool.STAFF_OF_ED );
			return true;
		}

		if ( location.startsWith( "pandamonium.php?action=mourn&whichitem=" ) )
		{
			int comedyItemID = GenericRequest.getWhichItem( location );
			if ( comedyItemID == -1 )
			{
				return false;
			}

			String comedy;
			boolean offhand = false;
			switch ( comedyItemID )
			{
			case ItemPool.INSULT_PUPPET:
				comedy = "insult";
				offhand = true;
				break;
			case ItemPool.OBSERVATIONAL_GLASSES:
				comedy = "observe";
				break;
			case ItemPool.COMEDY_PROP:
				comedy = "prop";
				break;
			default:
				KoLmafia.updateDisplay(
					MafiaState.ABORT,
					"\"" + comedyItemID + "\" is not a comedy item number that Mafia recognizes." );
				return false;
			}

			AdventureResult comedyItem = ItemPool.get( comedyItemID, 1 );
			String text = null;

			try ( Checkpoint checkpoint = new Checkpoint() )
			{
				if ( KoLConstants.inventory.contains( comedyItem ) )
				{
					// Unequip any 2-handed weapon before equipping an offhand
					if ( offhand )
					{
						AdventureResult weapon = EquipmentManager.getEquipment( EquipmentManager.WEAPON );
						int hands = EquipmentDatabase.getHands( weapon.getItemId() );
						if ( hands > 1 )
						{
							new EquipmentRequest( EquipmentRequest.UNEQUIP, EquipmentManager.WEAPON ).run();
						}
					}

					new EquipmentRequest( comedyItem ).run();
				}

				if ( KoLmafia.permitsContinue() && KoLCharacter.hasEquipped( comedyItem ) )
				{
					GenericRequest request = new PandamoniumRequest( comedy );
					request.run();
					text = request.responseText;
				}
			}

			if ( text != null )
			{
				this.responseText = text;
				return false;
			}
		}

		return true;
	}

	public void execute()
	{
		String urlString = this.getURLString();

		if ( urlString.startsWith( "adventure.php" ) ||
		     urlString.startsWith( "fambattle.php" ) ||
		     urlString.startsWith( "fight.php" ) ||
		     urlString.startsWith( "choice.php" ) ||
		     urlString.startsWith( "place.php" ) )
		{
			RelayAgent.clearErrorRequest();
		}

		if ( !GenericRequest.isRatQuest )
		{
			GenericRequest.isRatQuest = urlString.startsWith( "cellar.php" );
		}

		if ( GenericRequest.isRatQuest && this.hasResult && !urlString.startsWith( "cellar.php" ) )
		{
			GenericRequest.isRatQuest = urlString.startsWith( "fight.php" ) || urlString.startsWith( "fambattle.php" );
		}

		if ( GenericRequest.isRatQuest )
		{
			TavernRequest.preTavernVisit( this );
		}

		if ( this.hasResult && GenericRequest.isBarrelSmash )
		{
			// Smash has resulted in a mimic.
			// Continue tracking throughout the combat
			GenericRequest.isBarrelSmash = urlString.startsWith( "fight.php" ) || urlString.startsWith( "fambattle.php" );
		}

		if ( urlString.startsWith( "barrel.php?" ) )
		{
			GenericRequest.isBarrelSmash = true;
			BarrelDecorator.beginSmash( urlString );
		}

		// Do this before registering the request now that we have a
		// choice chain that takes a turn per choice
		if ( urlString.startsWith( "choice.php" ) )
		{
			ChoiceManager.preChoice( this );
		}

		if ( this.hasResult )
		{
			RequestLogger.registerRequest( this, urlString );
		}

		if ( urlString.startsWith( "ascend.php" ) && urlString.contains( "action=ascend" ) )
		{
			GenericRequest.ascending = true;
			KoLmafia.forceContinue();
			ValhallaManager.preAscension();
			GenericRequest.ascending = false;

			// If the preAscension script explicitly aborted, don't
			// jump into the gash. Let the user fix the problem.
			if ( KoLmafia.refusesContinue() )
			{
				return;
			}

			// Set preference so we call ValhallaManager.onAscension()
			// when we reach the afterlife.
			Preferences.setInteger( "lastBreakfast", 0 );
		}

		if ( urlString.startsWith( "afterlife.php" ) && Preferences.getInteger( "lastBreakfast" ) != -1 )
		{
			ValhallaManager.onAscension();
		}

		this.externalExecute();

		if ( !LoginRequest.isInstanceRunning() && !this.isChatRequest )
		{
			ConcoctionDatabase.refreshConcoctions( false );
		}
	}

	public void externalExecute()
	{
		do
		{
			if ( !this.prepareConnection() )
			{
				break;
			}
		}
		while ( !this.postClientData() && !this.retrieveServerReply() && this.timeoutCount < GenericRequest.TIMEOUT_LIMIT && this.redirectCount < GenericRequest.REDIRECT_LIMIT );
	}

	public static final boolean shouldIgnore( final GenericRequest request )
	{
		String requestURL = GenericRequest.decodeField( request.formURLString );
		return requestURL == null ||
			// Disallow mall searches
			requestURL.contains( "mall.php" ) ||
			requestURL.contains( "manageprices.php" ) ||
			requestURL.contains( "backoffice.php" ) ||
			// Disallow anything to do with chat
			request.isChatRequest;
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
			RequestLogger.updateDebugLog( "Connecting to " + this.baseURLString + "..." );
		}

		// Make sure that all variables are reset before you reopen
		// the connection.

		this.responseCode = 0;
		this.responseMessage = null;
		this.responseText = null;
		this.redirectLocation = null;
		this.redirectMethod = null;
		this.formConnection = null;

		try
		{
			this.formURL = this.buildURL();
			this.formConnection = (HttpURLConnection) this.formURL.openConnection();
		}
		catch ( IOException e )
		{
			if ( this.shouldUpdateDebugLog() )
			{
				String message = "IOException opening connection (" + this.getURLString() + "). Retrying...";
				StaticEntity.printStackTrace( e, message );
			}

			return false;
		}

		this.formConnection.setDoInput( true );

		this.formConnection.setDoOutput( !this.data.isEmpty() );
		this.formConnection.setUseCaches( false );
		this.formConnection.setInstanceFollowRedirects( false );

		if ( !this.isExternalRequest && GenericRequest.sessionId != null )
		{
			this.formConnection.addRequestProperty( "Cookie", this.getCookies() );
		}

		this.formConnection.setRequestProperty( "User-Agent", GenericRequest.userAgent );
		this.formConnection.setRequestProperty( "Accept-Encoding", "gzip" );

		if ( !this.data.isEmpty() )
		{
			if ( this.dataChanged )
			{
				this.dataChanged = false;
				this.dataString = this.getDataString().getBytes();
			}

			this.formConnection.setRequestProperty( "Content-Type", "application/x-www-form-urlencoded" );
			this.formConnection.setRequestProperty( "Content-Length", String.valueOf( this.dataString.length ) );
		}

		return true;
	}

	public String getCookies()
	{
		return this.getCookies( new StringBuilder() ).toString();
	}

	public StringBuilder getCookies( final StringBuilder cookies )
	{
		String path = this.getBasePath();
		int slash = path.lastIndexOf( "/" );
		if ( slash > 0 )
		{
			path = path.substring( 0, slash - 1 );
		}
		else
		{
			path = "/";
		}

		boolean delim = false;

		synchronized ( GenericRequest.serverCookies )
		{
			for ( ServerCookie cookie : GenericRequest.serverCookies )
			{
				if ( cookie.validPath( path ) )
				{
					if ( delim )
					{
						cookies.append( "; " );
					}
					cookies.append( cookie.toString() );
					delim = true;
				}
			}
		}

		return cookies;
	}

	public void setCookies()
	{
		synchronized ( GenericRequest.serverCookies )
		{
			// Field: Set-Cookie = [PHPSESSID=i9tr5te1hhk7084d7do6s877h3; path=/, AWSALB=1HOUaMRO89JYkb8nBfrsK6maRGcdoJpTOmxa/LEVbQsBnwi1jPq7jvG2jw1m4p1SR7Y35Wq/dUKVBG5RcvMu7Zw89U1RAeBkZlIkGP/8hVnXCmkWUxfEvuveJZfB; Expires=Fri, 16-Sep-2016 15:43:04 GMT; Path=/]

			Map<String,List<String>> headerFields = this.formConnection.getHeaderFields();

			for ( Entry<String,List<String>> entry: headerFields.entrySet() )
			{
				String key = entry.getKey();
				if ( key == null || !key.equals( "Set-Cookie" ) )
				{
					continue;
				}

				List<String> cookies = entry.getValue();
				for ( String cookie : cookies )
				{
					while ( cookie != null & !cookie.equals( "" ) )
					{
						int comma = cookie.indexOf( "," );
						int expires = cookie.toLowerCase().indexOf( "expires=" );
						if ( expires != -1 && expires < comma )
						{
							comma = cookie.indexOf( ",", comma + 1 );
						}

						ServerCookie serverCookie = new ServerCookie( comma == -1 ? cookie : cookie.substring( 0, comma ) );
						String name = serverCookie.getName();

						if ( GenericRequest.specialCookie( name ) )
						{
							// We've defined cookie equality as same name
							// Since the value has changed, remove the old cookie first
							GenericRequest.serverCookies.remove( serverCookie );
							GenericRequest.serverCookies.add( serverCookie );

							if ( name.equals( "PHPSESSID" ) )
							{
								GenericRequest.sessionId = serverCookie.toString();
							}
						}

						if ( comma == -1 )
						{
							break;
						}

						cookie = cookie.substring( comma + 1 );
					}
				}
			}
		}
	}

	public static boolean specialCookie( final String name )
	{
		return name.equals( "PHPSESSID" ) || name.equals( "AWSALB" );
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

		URL context = null;

		if ( !this.isExternalRequest )
		{
			context = GenericRequest.KOL_SECURE_ROOT;
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
		if ( this.shouldUpdateDebugLog() || RequestLogger.isTracing() || ScriptRuntime.isTracing() )
		{
			if ( this.shouldUpdateDebugLog() )
			{
				this.printRequestProperties();
			}
			if ( RequestLogger.isTracing() )
			{
				RequestLogger.trace( "Requesting: " + this.requestURL() );
			}
			if ( ScriptRuntime.isTracing() )
			{
				ScriptRuntime.println( "Requesting: " + this.requestURL() );
			}
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
		catch ( SocketTimeoutException e )
		{
			++this.timeoutCount;

			if ( this.shouldUpdateDebugLog() )
			{
				String message = "Time out during data post (" + this.formURLString + "). This could be bad...";
				RequestLogger.printLine( message );
			}

			return KoLmafia.refusesContinue();
		}
		catch ( IOException e )
		{
			String message = "IOException during data post (" + this.getURLString() + "): " + e.getMessage() + ".";

			if ( this.shouldUpdateDebugLog() )
			{
				StaticEntity.printStackTrace( e, message );
			}

			RequestLogger.printLine( MafiaState.ERROR, message );
			this.timeoutCount = TIMEOUT_LIMIT;
			return true;
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

		try
		{
			istream = this.formConnection.getInputStream();
			if ( "gzip".equals( this.formConnection.getContentEncoding() ) )
			{
				istream = new GZIPInputStream( istream );
			}
			this.responseCode = this.formConnection.getResponseCode();

			//Handle HTTP 3xx Redirections
			if ( this.responseCode > 300 && this.responseCode < 309 )
			{
				this.redirectMethod = this.formConnection.getRequestMethod();
				switch ( this.responseCode )
				{
				case 302: //Treat 302 as a 303, like all modern browsers.
				case 303:
					this.redirectMethod = "GET";
					//FALL THROUGH!
				case 301:
				case 307:
				case 308:
					if ( this instanceof RelayRequest || this.redirectMethod.equals( "GET" ) || this.redirectMethod.equals( "HEAD" ) )
					{
						//RelayRequests are handled later. Allow GET/HEAD, redirects by default.
						this.redirectLocation = this.formConnection.getHeaderField( "Location" );
					}
					else
					{
						// RFC 2616: For requests other than GET or HEAD, the user agent MUST NOT
						// automatically redirect the request unless it can be confirmed by the user.
						if ( this.allowRedirect == null )
						{
							String message =
								"You are being redirected to \"" + this.formConnection.getHeaderField( "Location" ) + "\".\n" + "Would you like KoLmafia to resend the form data?";
							this.allowRedirect = InputFieldUtilities.confirm( message );
						}

						if ( this.allowRedirect.booleanValue() )
						{
							this.redirectLocation =
								this.data.isEmpty() ? this.formConnection.getHeaderField( "Location" ) : this.formConnection.getHeaderField( "Location" ) + "?" + this.getDisplayDataString();
						}
					}
					break;
				default:
					this.redirectLocation = null;
					break;
				}
			}
		}
		catch ( SocketTimeoutException e )
		{
			if ( this.shouldUpdateDebugLog() )
			{
				String message = "Time out retrieving server reply (" + this.formURLString + ").";
				RequestLogger.printLine( message );
			}

			boolean shouldRetry = this.retryOnTimeout();
			if ( !shouldRetry && this.processOnFailure() )
			{
				this.processResponse();
			}

			GenericRequest.forceClose( istream );

			++this.timeoutCount;
			return !shouldRetry || KoLmafia.refusesContinue();
		}
		catch ( IOException e )
		{
			this.responseCode = this.getResponseCode();
			this.responseMessage = this.getResponseMessage();

			if ( this.responseCode == 504 &&
			     ( this.baseURLString.equals( "storage.php" ) || this.baseURLString.equals( "inventory.php" ) ) &&
			     ( this.formURLString.contains( "action=pullall" ) || this.getFormField( "action" ) != null ) )
			{
				// Likely a pullall request that timed out
				PauseObject pauser = new PauseObject();
				KoLmafia.updateDisplay( "Waiting 40 seconds for KoL to finish processing..." );
				pauser.pause( 40 * 1000 );
				StorageRequest.emptyStorage( this.formURLString );
				return true;
			}

			if ( this.responseCode != 0 )
			{
				String message = "Server returned response code " + this.responseCode + " (" + this.responseMessage + ") for " + this.baseURLString;
				KoLmafia.updateDisplay( MafiaState.ERROR, message );
			}

			if ( this.shouldUpdateDebugLog() )
			{
				String message = "IOException retrieving server reply (" + this.getURLString() + ").";
				StaticEntity.printStackTrace( e, message );
			}

			if ( this.processOnFailure() )
			{
				this.responseText = "";
				this.processResponse();
			}

			GenericRequest.forceClose( istream );

			this.timeoutCount = TIMEOUT_LIMIT;
			return true;
		}

		if ( istream == null )
		{
			this.responseCode = 302;
			this.redirectLocation = "main.php";
			return true;
		}

		if ( this.shouldUpdateDebugLog() || RequestLogger.isTracing() || ScriptRuntime.isTracing() )
		{
			if ( this.shouldUpdateDebugLog() )
			{
				this.printHeaderFields();
			}
			if ( this.responseCode != 200 && RequestLogger.isTracing() )
			{
				RequestLogger.trace( "Retrieved: " + this.requestURL() );
			}
			if ( ScriptRuntime.isTracing() )
			{
				ScriptRuntime.println( "Retrieved: " + this.requestURL() );
			}
		}

		// Handle Set-Cookie headers - which can appear on redirects
		if ( !this.isExternalRequest )
		{
			this.setCookies();
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
				shouldStop = ( this.redirectLocation != null ) ? this.handleServerRedirect() : true;
			}
		}
		catch ( IOException e )
		{
			StaticEntity.printStackTrace( e );
			return true;
		}

		istream = null;
		return shouldStop || KoLmafia.refusesContinue();
	}

	private int getResponseCode()
	{
		if ( this.formConnection != null )
		{
			try
			{
				return this.formConnection.getResponseCode();
			}
			catch ( IOException e )
			{
			}
		}

		return 0;
	}

	private String getResponseMessage()
	{
		if ( this.formConnection != null )
		{
			try
			{
				return this.formConnection.getResponseMessage();
			}
			catch ( IOException e )
			{
			}
		}

		return "";
	}

	private static void forceClose( final InputStream stream)
	{
		if ( stream != null )
		{
			try
			{
				stream.close();
			}
			catch ( IOException e )
			{
			}
		}
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

		this.redirectCount++;

		// Let ChoiceManager clean up if you are walking away from a request.
		ChoiceManager.handleWalkingAway( this.formURLString );

		if ( this.redirectLocation.startsWith( "maint.php" ) )
		{
			// If the request was issued from the Relay
			// Browser, follow the redirect and show the
			// user the maintenance page.

			if ( this instanceof RelayRequest )
			{
				return true;
			}

			// Otherwise, inform the user in the status
			// line and abort.

			KoLmafia.updateDisplay( MafiaState.ABORT, "Nightly maintenance. Please restart KoLmafia." );
			GenericRequest.reset();
			return true;
		}

		// If this is a login page redirect, construct the URL string
		// and notify the browser that it should change everything.

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
				String server = matcher.group( 1 );
				if ( !server.equals( "" ) )
				{
					RequestLogger.printLine( "Redirected to " + server + "..." );
					GenericRequest.setLoginServer( server );
				}
				this.constructURLString( matcher.group( 2 ), false );
				return false;
			}

			LoginRequest.processLoginRequest( this );
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

			if ( this.isChatRequest )
			{
				RequestLogger.printLine( "You are logged out.  Chat will no longer update." );
				GenericRequest.ignoreChatRequest = true;
				return false;
			}

			String oldpwd = GenericRequest.passwordHashValue;
			if ( LoginRequest.executeTimeInRequest( this.getURLString(), this.redirectLocation ) )
			{
				if ( this.data.isEmpty() )
				{
					String newpwd = GenericRequest.passwordHashValue;
					this.formURLString = StringUtilities.singleStringReplace( this.formURLString, oldpwd, newpwd );
					this.formURL = null;
				}
				else
				{
					this.dataChanged = true;
				}
				return false;
			}

			return true;
		}

		// If this is a redirect from valhalla, we are reincarnating
		if ( this.formURLString.startsWith( "afterlife.php" ) )
		{
			// Reset all per-ascension counters
			KoLmafia.resetCounters();

			// Certain paths send you into a choice adventure.
			// Defer new-ascension processing until that is done.
			if ( this.redirectLocation.startsWith( "choice.php" ) )
			{
				ChoiceManager.ascendAfterChoice();
			}

			// Otherwise, do post-ascension processing immediately.
			else
			{
				ValhallaManager.postAscension();
			}

			return true;
		}

		if ( this.redirectLocation.startsWith( "fight.php" ) || this.redirectLocation.startsWith( "fambattle.php" ) )
		{
			String location = this.getURLString();

			GenericRequest.checkItemRedirection( location );
			GenericRequest.checkChoiceRedirection( location );
			GenericRequest.checkSkillRedirection( location );
			GenericRequest.checkOtherRedirection( location );

			if ( this instanceof UseItemRequest ||
			     this instanceof CampgroundRequest ||
			     this instanceof CargoCultistShortsRequest ||
			     this instanceof ChateauRequest ||
			     this instanceof DeckOfEveryCardRequest ||
			     this instanceof GenieRequest ||
			     this instanceof NumberologyRequest ||
			     this instanceof UseSkillRequest )
			{
				this.redirectHandled = true;
				FightRequest.INSTANCE.run( this.redirectLocation );

				// Clingy monsters or Eldritch Attunement can lead to a multi-fight
				// Using the Force can leave you in a choice.
				while ( !KoLmafia.refusesContinue() )
				{
					if ( FightRequest.inMultiFight || FightRequest.fightFollowsChoice )
					{
						FightRequest.INSTANCE.run();
						continue;
					}
					if ( FightRequest.choiceFollowsFight )
					{
						RequestThread.postRequest( new GenericRequest( "choice.php" ) );
						// Fall through
					}
					if ( ChoiceManager.handlingChoice )
					{
						ChoiceManager.gotoGoal();
						continue;
					}
					break;
				}
				if ( FightRequest.currentRound == 0 && !FightRequest.inMultiFight && !FightRequest.choiceFollowsFight )
				{
					KoLmafia.executeAfterAdventureScript();
				}
				return !LoginRequest.isInstanceRunning();
			}
		}

		if ( this.redirectLocation.startsWith( "choice.php" ) )
		{
			GenericRequest.checkItemRedirection( this.getURLString() );
		}

		if ( this.redirectLocation.startsWith( "messages.php?results=Message" ) )
		{
			SendMailRequest.parseTransfer( this.getURLString() );
		}

		if ( this instanceof RelayRequest )
		{
			return true;
		}

		if ( this.formURLString.startsWith( "fight.php" ) || this.formURLString.startsWith( "fambattle.php" ) )
		{
			if ( this.redirectLocation.startsWith( "main.php" ) )
			{
				this.constructURLString( this.redirectLocation, false );
				return false;
			}
		}

		if ( this.formURLString.startsWith( "peevpee.php" ) )
		{
			// If you have not set an e-mail address, you get
			// redirected.  This can happen while logging in.
			// In any case, we cannot automate it.
			if ( this.redirectLocation.startsWith( "choice.php" ) )
			{
				return true;
			}
		}

		if ( this.shouldFollowRedirect() )
		{
			// Re-setup this request to follow the redirect
			// desired and rerun the request.

			boolean fromChoice = this.formURLString.startsWith( "choice.php" );
			this.constructURLString( this.redirectLocation, this.redirectMethod.equals( "POST" ) );
			this.hasResult = this.hasResult( this.redirectLocation );
			if ( this.redirectLocation.startsWith( "choice.php" ) )
			{
				ChoiceManager.preChoice( this );
			}
			else if ( this.redirectLocation.startsWith( "fight.php" ) ||
				  this.redirectLocation.startsWith( "fambattle.php" ))
			{
				FightRequest.preFight( fromChoice );
			}
			if ( this.hasResult )
			{
				RequestLogger.registerRequest( this, this.redirectLocation );
			}
			return false;
		}

		if ( this.redirectLocation.startsWith( "adventure.php" ) )
		{
			this.constructURLString( this.redirectLocation, false );
			return false;
		}

		if ( this.redirectLocation.startsWith( "fight.php" ) || this.redirectLocation.startsWith( "fambattle.php" ) )
		{
			if ( LoginRequest.isInstanceRunning() )
			{
				KoLmafia.updateDisplay( MafiaState.ABORT, this.baseURLString + ": redirected to a fight page." );
				FightRequest.initializeAfterFight();
				return true;
			}

			// You have been redirected to a fight! Here, you need
			// to complete the fight before you can continue.

			if ( this == ChoiceManager.CHOICE_HANDLER ||
			     this instanceof AdventureRequest ||
			     this instanceof BasementRequest )
			{
				this.redirectHandled = true;
				FightRequest.INSTANCE.run( this.redirectLocation );
				return !LoginRequest.isInstanceRunning();
			}

			// This is a request which should not have lead to a
			// fight, but it did.  Notify the user.

			KoLmafia.updateDisplay( MafiaState.ABORT, this.baseURLString + ": redirected to a fight page." );
			return true;
		}

		if ( this.redirectLocation.startsWith( "choice.php" ) )
		{
			if ( LoginRequest.isInstanceRunning() )
			{
				KoLmafia.updateDisplay( MafiaState.ABORT, this.baseURLString + ": redirected to a choice page." );
				ChoiceManager.initializeAfterChoice();
				return true;
			}

			this.redirectHandled = true;
			ChoiceManager.processRedirectedChoiceAdventure( this.redirectLocation );
			return true;
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
		return this.getClass() == GenericRequest.class;
	}

	private boolean retrieveServerReply( final InputStream istream )
		throws IOException
	{
		if ( this.shouldUpdateDebugLog() )
		{
			RequestLogger.updateDebugLog( "Retrieving server reply" );
		}

		this.responseText = new String( ByteBufferUtilities.read( istream ), StandardCharsets.UTF_8 );

		if ( this.responseCode == 200 && RequestLogger.isTracing() )
		{
			String buffer = "Retrieved: " + this.requestURL() +
					" (" +
					( this.responseText == null ? "0" : this.responseText.length() ) +
					" bytes)";
			RequestLogger.trace( buffer );
		}

		if ( this.responseText == null )
		{
			if ( this.shouldUpdateDebugLog() )
			{
				RequestLogger.updateDebugLog( "ResponseText is null" );
			}
			return true;
		}

		if ( this.shouldUpdateDebugLog() )
		{
			RequestLogger.updateDebugLog( "ResponseText has " + responseText.length() + " characters." );
		}

		if ( this.responseText.length() < 200 )
		{
			// This may be a JavaScript redirect.
			Matcher m = GenericRequest.JS_REDIRECT_PATTERN.matcher( this.responseText );
			if ( m.find() )
			{
				// Do NOT call processResults for a redirection
				// But do log the redirection
				if ( this.shouldUpdateDebugLog() )
				{
					RequestLogger.updateDebugLog( this.responseText );
				}
				this.redirectLocation = m.group( 1 );
				this.redirectMethod = "GET";
				return this.handleServerRedirect();
			}
		}

		try
		{
			PreferenceListenerRegistry.deferPreferenceListeners( true );
			this.processResponse();
		}
		catch ( Exception e )
		{
			StaticEntity.printStackTrace( e );
		}
		finally
		{
			PreferenceListenerRegistry.deferPreferenceListeners( false );
		}

		return true;
	}

	/**
	 * This method allows classes to process a raw, unfiltered server response.
	 */

	public void processResponse()
	{
		if ( this.responseText == null )
		{
			// KoL or network error
			return;
		}

		if ( this.shouldUpdateDebugLog() )
		{
			String text = this.responseText;
			if ( !Preferences.getBoolean( "logReadableHTML" ) )
			{
				text = KoLConstants.LINE_BREAK_PATTERN.matcher( text ).replaceAll( "" );
			}
			RequestLogger.updateDebugLog( text );
		}

		if ( this.isChatRequest || this.isStaticRequest )
		{
			return;
		}

		if ( this.isDescRequest || this.isQuestLogRequest )
		{
			ResponseTextParser.externalUpdate( this );
			return;
		}

		String urlString = this.getURLString();
		if ( urlString.startsWith( "charpane.php" ) )
		{
			long responseTimestamp =
				this.formConnection.getHeaderFieldDate( "Date", System.currentTimeMillis() );

			if ( !CharPaneRequest.processResults( responseTimestamp, this.responseText ) )
			{
				this.responseCode = 304;
			}

			return;
		}

		else if ( urlString.startsWith( "api.php" ) )
		{
			ApiRequest.parseResponse( urlString, this.responseText );
			return;
		}

		EventManager.checkForNewEvents( this.responseText );

		if ( GenericRequest.isRatQuest )
		{
			TavernRequest.postTavernVisit( this );
			GenericRequest.isRatQuest = false;
		}

		if ( ChoiceManager.handlingChoice )
		{
			// Handle choices BEFORE registering Encounter
			ChoiceManager.postChoice0( urlString, this );
		}

		this.encounter = AdventureRequest.registerEncounter( this );

		if ( ChoiceManager.handlingChoice )
		{
			// Handle choices BEFORE result processing
			ChoiceManager.postChoice1( urlString, this );
		}

		if ( this.hasResult )
		{
			long initialHP = KoLCharacter.getCurrentHP();
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

		if ( ChoiceManager.handlingChoice )
		{
			// Handle choices AFTER result processing
			ChoiceManager.postChoice2( urlString, this );
		}

		// Let clover protection kick in if needed

		if ( ResultProcessor.shouldDisassembleClovers( urlString ) )
		{
			KoLmafia.protectClovers();
		}

		// Perhaps check for random donations in Fistcore
		if ( !ResultProcessor.onlyAutosellDonationsCount && KoLCharacter.inFistcore() )
		{
			ResultProcessor.handleDonations( urlString, this.responseText );
		}

		// Once everything is complete, decide whether or not
		// you should refresh your status.

		if ( !this.hasResult || GenericRequest.suppressUpdate )
		{
			return;
		}

		if ( this instanceof RelayRequest )
		{
			return;
		}

		// Don't bother refreshing status if we are refreshing the
		// session, since none of the requests made while that is
		// happening change anything, even though KoL asks for a
		// charpane refresh for many of them.

		if ( this.responseText.contains( "charpane.php" ) && !KoLmafia.isRefreshing() )
		{
			ApiRequest.updateStatus( true );
			RelayServer.updateStatus();
		}
	}

	public void formatResponse()
	{
	}

	/**
	 * Utility method used to skip the given number of tokens within the provided <code>StringTokenizer</code>. This
	 * method is used in order to clarify what's being done, rather than calling <code>st.nextToken()</code> repeatedly.
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

	private void parseResults()
	{
		String urlString = this.getURLString();

		// Dispatch pages that have special handling

		if ( urlString.startsWith( "mall.php" ) ||
		     urlString.startsWith( "account.php" ) ||
		     urlString.startsWith( "records.php" ) )
		{
			// These pages cannot possibly contain an actual item
			// drop, but may have a bogus "You acquire an item:" as
			// part of a store name, profile quote, familiar name, etc.
			return;
		}

		if ( urlString.startsWith( "afterlife.php" ) )
		{
			AfterLifeRequest.parseResponse( urlString, this.responseText );
			return;
		}

		if ( urlString.startsWith( "arena.php" ) )
		{
			CakeArenaRequest.parseResults( this.responseText );
			return;
		}

		if ( urlString.startsWith( "backoffice.php" ) )
		{
			// ManageStoreRequest.parseResponse will sort this out.
			return;
		}

		if ( urlString.startsWith( "mallstore.php" ) )
		{
			// MallPurchaseRequest.parseResponse will sort this out.
			return;
		}

		if ( urlString.startsWith( "peevpee.php" ) )
		{
			if ( this.getFormField( "lid" ) == null )
			{
				PeeVPeeRequest.parseItems( this.responseText );
			}
			return;
		}

		if ( urlString.startsWith( "raffle.php" ) )
		{
			return;
		}

		if ( urlString.startsWith( "showplayer.php" ) )
		{
			// These pages cannot possibly contain an actual item
			// drop, but may have a bogus "You acquire an item:" as
			// part of a store name, profile quote, familiar name,
			// etc.  They may also have unknown items as equipment,
			// which we want to recognize and register.  And if you
			// are looking at Jick, his psychoses may be available.
			ProfileRequest.parseResponse( urlString, this.responseText );
			return;
		}

		if ( urlString.startsWith( "displaycollection.php" ) )
		{
			// Again, these pages cannot possibly contain an actual
			// item drop, but have a user supplied message.
			DisplayCaseRequest.parseDisplayCase( urlString, this.responseText );
			return;
		}

		// If this is a lucky adventure, then remove a clover
		// from the player's inventory,
		//
		// Most places, this is signaled by the message "Your (or your)
		// ten-leaf clover disappears in a puff of smoke."
		//
		// Some places, it "vanishes in a puff of smoke."
		//
		// The Hippy Camp (In Disguise)'s A Case of the Baskets, the message is
		// "Like the smoke your ten-leaf clover disappears in a puff of"
		//
		// In the Spooky Forest's Lucky, Lucky! encounter, the message is
		// "Your ten-leaf clover disappears into the leprechaun's pocket"
		//
		// The Orcish Frat House:
		// Pretty good timing, it seems. Your ten-leaf clover
		// disappears in a cloud of smoke and alcohol fumes.

		if ( this.responseText.contains( "clover" ) &&
		     ( this.responseText.contains( "disappears in a puff of" ) ||
		       this.responseText.contains( "vanishes in a puff of smoke" ) ||
		       this.responseText.contains( "into the leprechaun's pocket" ) ||
		       this.responseText.contains( "cloud of smoke and alcohol fumes" ) ) )
		{
			ResultProcessor.processItem( ItemPool.TEN_LEAF_CLOVER, -1 );
		}

		if ( this.responseText.contains( "You break the bottle on the ground" ) )
		{
			// You break the bottle on the ground, and stomp it to powder
			ResultProcessor.processItem( ItemPool.EMPTY_AGUA_DE_VIDA_BOTTLE, -1 );
		}

		if ( this.responseText.contains( "FARQUAR" ) ||
		     this.responseText.contains( "Sleeping Near the Enemy" ) )
		{
			// The password to the Dispensary is known!
			Preferences.setInteger( "lastDispensaryOpen", KoLCharacter.getAscensions() );
		}

		if ( urlString.startsWith( "adventure.php" ) )
		{
			ResultProcessor.processResults( true, this.responseText );
			return;
		}

		if ( urlString.startsWith( "fight.php" ) || urlString.startsWith( "fambattle.php" ) )
		{
			FightRequest.processResults( urlString, this.encounter, this.responseText );
			return;
		}

		if ( urlString.startsWith( "main.php" ) )
		{
			FightRequest.currentRound = 0;
			if ( urlString.contains( "fightgodlobster=1" ) && this.responseText.contains( "can't challenge your God Lobster anymore" ) )
			{
				Preferences.setInteger( "_godLobsterFights", 3 );
			}
			return;
		}

		ResultProcessor.processResults( false, this.responseText );
	}

	public void processResults()
	{
		String path = this.getPath();

		if ( ( this.hasResult && !path.startsWith( "fight.php" ) && !path.startsWith( "fambattle.php" ) ) ||
		     path.startsWith( "clan_hall.php" ) ||
		     path.startsWith( "showclan.php" ) )
		{
			ResponseTextParser.externalUpdate( this );
		}
	}

	/*
	 * Method to display the current request in the Fight Frame. If we are synchronizing, show all requests If we are
	 * finishing, show only exceptional requests
	 */

	public void showInBrowser( final boolean exceptional )
	{
		if ( !exceptional && !Preferences.getBoolean( "showAllRequests" ) )
		{
			return;
		}

		// Only show the request if the response code is
		// 200 (not a redirect or error).

		boolean showRequestSync =
			Preferences.getBoolean( "showAllRequests" ) ||
				exceptional && Preferences.getBoolean( "showExceptionalRequests" );

		if ( showRequestSync )
		{
			RequestSynchFrame.showRequest( this );
		}

		if ( exceptional )
		{
			RelayAgent.setErrorRequest( this );

			String linkHTML =
				"<a href=main.php target=mainpane class=error>Click here to continue in the relay browser.</a>";
			InternalMessage message = new InternalMessage( linkHTML, null );
			ChatPoller.addEntry( message );
		}
	}

	private static void checkItemRedirection( final String location )
	{
		// Certain choices lead to fights. We log those in ChoiceManager.
		if ( location.startsWith( "choice.php" ) )
		{
			return;
		}

		// Otherwise, only look for items
		AdventureResult item =
			location.contains( "action=chateau_painting" ) ?
			ChateauRequest.CHATEAU_PAINTING :
			UseItemRequest.extractItem( location );

		GenericRequest.itemMonster = null;

		if ( item == null )
		{
			return;
		}

		int itemId = item.getItemId();
		String itemName = null;
		boolean consumed = false;
		String nextAdventure = null;

		switch ( itemId )
		{
		case ItemPool.BLACK_PUDDING:
			itemName = "Black Pudding";
			consumed = true;
			break;

		case ItemPool.DRUM_MACHINE:
			itemName = "Drum Machine";
			consumed = true;
			break;

		case ItemPool.DOLPHIN_WHISTLE:
			itemName = "Dolphin Whistle";
			consumed = true;
			MonsterData m = MonsterDatabase.findMonster( "rotten dolphin thief" );
			if ( m != null )
			{
				m.clearItems();
				String stolen = Preferences.getString( "dolphinItem" );
				if ( stolen.length() > 0 )
				{
					m.addItem( ItemPool.get( stolen, 100 << 16 | 'n' ) );
				}
				m.doneWithItems();
			}
			Preferences.setString( "dolphinItem", "" );
			break;

		case ItemPool.CARONCH_MAP:
			itemName = "Cap'm Caronch's Map";
			break;

		case ItemPool.FRATHOUSE_BLUEPRINTS:
			itemName = "Orcish Frat House blueprints";
			break;

		case ItemPool.CURSED_PIECE_OF_THIRTEEN:
			itemName = "Cursed Piece of Thirteen";
			break;

		case ItemPool.SPOOKY_PUTTY_MONSTER:
			itemName = "Spooky Putty Monster";
			Preferences.setString( "spookyPuttyMonster", "" );
			ResultProcessor.processItem( ItemPool.SPOOKY_PUTTY_SHEET, 1 );
			consumed = true;
			EncounterManager.ignoreSpecialMonsters();
			break;

		case ItemPool.RAIN_DOH_MONSTER:
			itemName = "Rain-Doh box full of monster";
			Preferences.setString( "rainDohMonster", "" );
			ResultProcessor.processItem( ItemPool.RAIN_DOH_BOX, 1 );
			consumed = true;
			EncounterManager.ignoreSpecialMonsters();
			break;

		case ItemPool.SHAKING_CAMERA:
			itemName = "shaking 4-D camera";
			Preferences.setString( "cameraMonster", "" );
			Preferences.setBoolean( "_cameraUsed", true );
			consumed = true;
			EncounterManager.ignoreSpecialMonsters();
			break;

		case ItemPool.SHAKING_CRAPPY_CAMERA:
			itemName = "Shaking crappy camera";
			Preferences.setString( "crappyCameraMonster", "" );
			Preferences.setBoolean( "_crappyCameraUsed", true );
			consumed = true;
			EncounterManager.ignoreSpecialMonsters();
			break;

		case ItemPool.ICE_SCULPTURE:
			itemName = "ice sculpture";
			Preferences.setString( "iceSculptureMonster", "" );
			Preferences.setBoolean( "_iceSculptureUsed", true );
			consumed = true;
			EncounterManager.ignoreSpecialMonsters();
			break;

		case ItemPool.PHOTOCOPIED_MONSTER:
			itemName = "photocopied monster";
			Preferences.setString( "photocopyMonster", "" );
			Preferences.setBoolean( "_photocopyUsed", true );
			consumed = true;
			EncounterManager.ignoreSpecialMonsters();
			break;

		case ItemPool.WAX_BUGBEAR:
			itemName = "wax bugbear";
			Preferences.setString( "waxMonster", "" );
			consumed = true;
			EncounterManager.ignoreSpecialMonsters();
			break;

		case ItemPool.ENVYFISH_EGG:
			itemName = "envyfish egg";
			Preferences.setString( "envyfishMonster", "" );
			Preferences.setBoolean( "_envyfishEggUsed", true );
			consumed = true;
			EncounterManager.ignoreSpecialMonsters();
			break;

		case ItemPool.CRUDE_SCULPTURE:
			itemName = "crude monster sculpture";
			Preferences.setString( "crudeMonster", "" );
			consumed = true;
			EncounterManager.ignoreSpecialMonsters();
			break;

		case ItemPool.DEPLETED_URANIUM_SEAL:
			itemName = "Infernal Seal Ritual";
			Preferences.increment( "_sealsSummoned", 1 );
			ResultProcessor.processResult( GenericRequest.sealRitualCandles( itemId ) );
			// Why do we count this?
			Preferences.increment( "_sealFigurineUses", 1 );
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
			ResultProcessor.processResult( GenericRequest.sealRitualCandles( itemId ) );
			break;

		case ItemPool.BRICKO_OOZE:
		case ItemPool.BRICKO_BAT:
		case ItemPool.BRICKO_OYSTER:
		case ItemPool.BRICKO_TURTLE:
		case ItemPool.BRICKO_ELEPHANT:
		case ItemPool.BRICKO_OCTOPUS:
		case ItemPool.BRICKO_PYTHON:
		case ItemPool.BRICKO_VACUUM_CLEANER:
		case ItemPool.BRICKO_AIRSHIP:
		case ItemPool.BRICKO_CATHEDRAL:
		case ItemPool.BRICKO_CHICKEN:
			itemName = item.getName();
			Preferences.increment( "_brickoFights", 1 );
			consumed = true;
			break;

		case ItemPool.FOSSILIZED_BAT_SKULL:
			itemName = "Fossilized Bat Skull";
			consumed = true;
			ResultProcessor.processItem( ItemPool.FOSSILIZED_WING, -2 );
			break;

		case ItemPool.FOSSILIZED_BABOON_SKULL:
			itemName = "Fossilized Baboon Skull";
			consumed = true;
			ResultProcessor.processItem( ItemPool.FOSSILIZED_TORSO, -1 );
			ResultProcessor.processItem( ItemPool.FOSSILIZED_LIMB, -4 );
			break;

		case ItemPool.FOSSILIZED_SERPENT_SKULL:
			itemName = "Fossilized Serpent Skull";
			consumed = true;
			ResultProcessor.processItem( ItemPool.FOSSILIZED_SPINE, -3 );
			break;

		case ItemPool.FOSSILIZED_WYRM_SKULL:
			itemName = "Fossilized Wyrm Skull";
			consumed = true;
			ResultProcessor.processItem( ItemPool.FOSSILIZED_TORSO, -1 );
			ResultProcessor.processItem( ItemPool.FOSSILIZED_LIMB, -2 );
			ResultProcessor.processItem( ItemPool.FOSSILIZED_WING, -2 );
			ResultProcessor.processItem( ItemPool.FOSSILIZED_SPINE, -3 );
			break;

		case ItemPool.FOSSILIZED_DEMON_SKULL:
			itemName = "Fossilized Demon Skull";
			consumed = true;
			ResultProcessor.processItem( ItemPool.FOSSILIZED_TORSO, -1 );
			ResultProcessor.processItem( ItemPool.FOSSILIZED_SPIKE, -1 );
			ResultProcessor.processItem( ItemPool.FOSSILIZED_LIMB, -4 );
			ResultProcessor.processItem( ItemPool.FOSSILIZED_WING, -2 );
			ResultProcessor.processItem( ItemPool.FOSSILIZED_SPINE, -1 );
			break;

		case ItemPool.FOSSILIZED_SPIDER_SKULL:
			itemName = "Fossilized Spider Skull";
			consumed = true;
			ResultProcessor.processItem( ItemPool.FOSSILIZED_TORSO, -1 );
			ResultProcessor.processItem( ItemPool.FOSSILIZED_LIMB, -8 );
			ResultProcessor.processItem( ItemPool.FOSSILIZED_SPIKE, -8 );
			break;

		case ItemPool.RONALD_SHELTER_MAP:
		case ItemPool.GRIMACE_SHELTER_MAP:
			itemName = item.getName();
			consumed = true;
			break;

		case ItemPool.WHITE_PAGE:
			itemName = "white page";
			consumed = true;
			nextAdventure = "Whitey's Grove";
			break;

		case ItemPool.XIBLAXIAN_HOLOTRAINING_SIMCODE:
			itemName = "Xiblaxian holo-training simcode";
			consumed = true;
			break;

		case ItemPool.XIBLAXIAN_POLITICAL_PRISONER:
			itemName = "Xiblaxian encrypted political prisoner";
			consumed = true;
			break;

		case ItemPool.D10:
			// Using a single D10 generates a monster.
			if ( item.getCount() != 1 )
			{
				return;
			}
			itemName = "d10";
			// The item IS consumed, but inv_use.php does not
			// redirect to fight.php. Instead, the response text
			// includes Javascript to request fight.php
			consumed = false;
			break;

		case ItemPool.SHAKING_SKULL:
			itemName = "shaking skull";
			consumed = true;
			break;

		case ItemPool.ABYSSAL_BATTLE_PLANS:
			itemName = "abyssal battle plans";
			break;

		case ItemPool.SUSPICIOUS_ADDRESS:
			itemName = "a suspicious address";
			break;

		case ItemPool.CHEF_BOY_BUSINESS_CARD:
			itemName = "Chef Boy, R&D's business card";
			break;

		case ItemPool.RUSTY_HEDGE_TRIMMERS:
			itemName = "rusty hedge trimmers";
			consumed = true;
			nextAdventure = "Twin Peak";
			break;

		case ItemPool.LYNYRD_SNARE:
			itemName = "lynyrd snare";
			consumed = true;
			nextAdventure = "A Mob of Zeppelin Protesters";
			Preferences.increment( "_lynyrdSnareUses" );
			break;

		case ItemPool.CHATEAU_WATERCOLOR:
			itemName = "Chateau Painting";
			consumed = false;
			Preferences.setBoolean( "_chateauMonsterFought", true );
			EncounterManager.ignoreSpecialMonsters();
			break;

		case ItemPool.DECK_OF_EVERY_CARD:
			itemName = "Deck of Every Card";
			// Do not ignore special monsters here. That is handled
			// elsewhere, just for the cases that will be a combat.
			break;

		case ItemPool.GIFT_CARD:
			itemName = "gift card";
			consumed = true;
			// Do not ignore special monsters here. That is handled
			// elsewhere, just for the cases that will be a combat.
			break;

		case ItemPool.BARREL_MAP:
			itemName = "map to the Biggest Barrel";
			consumed = true;
			break;

		case ItemPool.VYKEA_INSTRUCTIONS:
			itemName = "VYKEA instructions";
			break;

		case ItemPool.TONIC_DJINN:
			itemName = "tonic djinn";
			break;

		case ItemPool.SCREENCAPPED_MONSTER:
			itemName = "screencapped monster";
			consumed = true;
			EncounterManager.ignoreSpecialMonsters();
			Preferences.setString( "screencappedMonster", "" );
			break;

		case ItemPool.TIME_RESIDUE:
			itemName = "time residue";
			consumed = true;
			break;

		case ItemPool.TIME_SPINNER:
			itemName = "Time-Spinner";
			break;

		case ItemPool.MEGACOPIA:
			itemName = "megacopia";
			consumed = true;
			break;

		case ItemPool.GENIE_BOTTLE:
		case ItemPool.POCKET_WISH:
			// Do not ignore special monsters here. That is handled
			// elsewhere, just for the cases that will be a combat.

			// Do not consume the item here, since the player can
			// walk away

			// Lastly, do not log item usage with a turn counter,
			// since only combats will use a turn.
			return;

		case ItemPool.CLARIFIED_BUTTER:
			itemName = "Dish of Clarified Butter";
			Preferences.increment( "_godLobsterFights", 1, 3, false );
			consumed = true;
			break;

		case ItemPool.AMORPHOUS_BLOB:
			itemName = "amorphous blob";
			consumed = true;
			break;

		case ItemPool.GIANT_AMORPHOUS_BLOB:
			itemName = "giant amorphous blob";
			consumed = true;
			break;

		case ItemPool.GLITCH_ITEM:
			if ( !location.startsWith( "inv_eat.php" ) )
			{
				return;
			}
			itemName = "[glitch season reward name]";
			break;

		case ItemPool.SIZZLING_DESK_BELL:
		case ItemPool.FROST_RIMED_DESK_BELL:
		case ItemPool.UNCANNY_DESK_BELL:
		case ItemPool.NASTY_DESK_BELL:
		case ItemPool.GREASY_DESK_BELL:
			itemName = item.getName();
			consumed = true;
			break;

		default:
			return;
		}

		if ( consumed )
		{
			ResultProcessor.processResult( item.getInstance( -1 ) );
		}

		if ( nextAdventure == null )
		{
			KoLAdventure.setLastAdventure( "None" );
			KoLAdventure.setNextAdventure( "None" );
		}
		else
		{
			KoLAdventure adventure = AdventureDatabase.getAdventure( nextAdventure );
			KoLAdventure.setLastAdventure( adventure );
			KoLAdventure.setNextAdventure( adventure );
			EncounterManager.registerAdventure( adventure.getAdventureName() );
		}

		String message = "[" + KoLAdventure.getAdventureCount() + "] " + itemName;
		RequestLogger.printLine();
		RequestLogger.printLine( message );

		RequestLogger.updateSessionLog();
		RequestLogger.updateSessionLog( message );

		GenericRequest.itemMonster = itemName;
	}

	private static void checkChoiceRedirection( final String location )
	{
		if ( !location.startsWith( "choice.php" ) )
		{
			return;
		}

		int choice = ChoiceManager.lastChoice;
		String name = null;

		switch ( choice )
		{
		case 1201:
			name = "Dr. Gordon Stuart's Science Tent";
			Preferences.setBoolean( "_eldritchTentacleFought", true );
			break;

		case 1267:
			name = "Genie Wish";
			break;

		case 1420:
			name = "Cargo Cultist Shorts";
			CargoCultistShortsRequest.registerPocketFight( location );
			break;

		default:
			return;
		}

		KoLAdventure.lastVisitedLocation = null;
		KoLAdventure.lastLocationName = null;
		KoLAdventure.lastLocationURL = location;
		KoLAdventure.setLastAdventure( "None" );
		KoLAdventure.setNextAdventure( "None" );

		String message = "[" + KoLAdventure.getAdventureCount() + "] " + name;
		RequestLogger.printLine();
		RequestLogger.printLine( message );

		RequestLogger.updateSessionLog();
		RequestLogger.updateSessionLog( message );
	}

	private static void checkSkillRedirection( final String location )
	{
		if ( !location.startsWith( "runskillz.php" ) )
		{
			return;
		}

		int skillId = UseSkillRequest.getSkillId( location );
		String skillName = null;

		switch ( skillId )
		{
		case SkillPool.RAIN_MAN:
			skillName = "Rain Man";
			break;

		case SkillPool.EVOKE_ELDRITCH_HORROR:
			skillName = "Evoke Eldritch Horror";
			Preferences.setBoolean( "_eldritchHorrorEvoked", true );
			break;

		default:
			return;
		}

		KoLAdventure.lastVisitedLocation = null;
		KoLAdventure.lastLocationName = null;
		KoLAdventure.lastLocationURL = location;
		KoLAdventure.setLastAdventure( "None" );
		KoLAdventure.setNextAdventure( "None" );

		String message = "[" + KoLAdventure.getAdventureCount() + "] " + skillName;
		RequestLogger.printLine();
		RequestLogger.printLine( message );

		RequestLogger.updateSessionLog();
		RequestLogger.updateSessionLog( message );
	}

	private static void checkOtherRedirection( final String location )
	{
		String otherName = null;

		if ( location.startsWith( "main.php" ) )
		{
			if ( location.contains( "fightgodlobster=1" ) )
			{
				Preferences.increment( "_godLobsterFights" );
				otherName = "God Lobster";
			}
		}
		else if ( location.startsWith( "campground.php" ) )
		{
			// A redirection to fight.php from harvesting your Bone
			// Garden is the skulldozer.
			if ( location.contains( "action=garden" ) )
			{
				otherName = "Bone Garden";
			}
		}

		if ( otherName == null )
		{
			return;
		}

		KoLAdventure.lastVisitedLocation = null;
		KoLAdventure.lastLocationName = null;
		KoLAdventure.lastLocationURL = location;
		KoLAdventure.setLastAdventure( "None" );
		KoLAdventure.setNextAdventure( "None" );

		String message = "[" + KoLAdventure.getAdventureCount() + "] " + otherName;
		RequestLogger.printLine();
		RequestLogger.printLine( message );

		RequestLogger.updateSessionLog();
		RequestLogger.updateSessionLog( message );
	}

	private static AdventureResult sealRitualCandles( final int itemId )
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
		case ItemPool.DEPLETED_URANIUM_SEAL:
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
			StringBuilder response = new StringBuilder();

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

	@Override
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
			agent = StaticEntity.getVersion();
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

	public String requestURL()
	{
		return  this.isExternalRequest ?
			this.getURLString() :
			this.formURL.getProtocol() + "://" + GenericRequest.KOL_HOST + "/" + this.getURLString();
	}

	public void printRequestProperties()
	{
		GenericRequest.printRequestProperties( this.requestURL(), this.formConnection );
	}

	public synchronized static void printRequestProperties( final String URL, final HttpURLConnection formConnection )
	{
		RequestLogger.updateDebugLog();
		RequestLogger.updateDebugLog( "Requesting: " + URL );

		Map<String,List<String>> requestProperties = formConnection.getRequestProperties();
		RequestLogger.updateDebugLog( requestProperties.size() + " request properties" );

		for ( Entry<String,List<String>> entry : requestProperties.entrySet() )
		{
			RequestLogger.updateDebugLog( "Field: " + entry.getKey() + " = " + entry.getValue() );
		}

		RequestLogger.updateDebugLog();
	}

	public void printHeaderFields()
	{
		GenericRequest.printHeaderFields( this.requestURL(), this.formConnection );
	}

	public synchronized static void printHeaderFields( final String URL, final HttpURLConnection formConnection )
	{
		RequestLogger.updateDebugLog();
		RequestLogger.updateDebugLog( "Retrieved: " + URL );

		Map<String,List<String>> headerFields = formConnection.getHeaderFields();
		RequestLogger.updateDebugLog( headerFields.size() + " header fields" );

		for ( Entry<String,List<String>> entry : headerFields.entrySet() )
		{
			RequestLogger.updateDebugLog( "Field: " + entry.getKey() + " = " + entry.getValue() );
		}

		RequestLogger.updateDebugLog();
	}

	private static final Pattern DOMAIN_PATTERN = Pattern.compile( "; *domain=(\\.?kingdomofloathing.com)" );
	public static String mungeCookieDomain( final String value )
	{
		Matcher m = DOMAIN_PATTERN.matcher( value );
		return  m.find() ?
			// StringUtilities.globalStringReplace( value, m.group( 1 ), "127.0.0.1:" + RelayServer.getPort() ) :
			StringUtilities.globalStringDelete( value, m.group( 0 ) ) :
			value;
	}

	public class ServerCookie
		implements Comparable<ServerCookie>
	{
		private String name = "";
		private String value = "";
		private String path = "";
		private String stringValue = "";

		public ServerCookie( final String cookie )
		{
			String value = cookie.trim();

			String attributes = "";

			int semi = value.indexOf( ";" );
			if ( semi != -1 )
			{
				attributes = value.substring( semi + 1 ).trim();
				value = value.substring( 0, semi ).trim();
			}

			// Get cookie name & value
			int equals = value.indexOf( "=" );
			if ( equals == -1 )
			{
				// Bogus cookie!
				System.out.println( "Bogus cookie: " + cookie );
				return;
			}

			// If KoL specifies a Domain attribute, we must remove it
			value = GenericRequest.mungeCookieDomain( value );

			this.name = value.substring( 0, equals );
			this.value = value.substring( equals + 1 );
			this.stringValue = value;

			// Process attributes
			while ( !attributes.equals( "" ) )
			{
				String attribute = attributes;
				semi = attributes.indexOf( ";" );
				if ( semi != -1 )
				{
					attribute = attributes.substring( 0, semi ).trim();
					attributes = attributes.substring( semi + 1 ).trim();
				}

				equals = attribute.indexOf( "=" );
				if ( equals == -1 )
				{
					// Secure or HttpOnly
					if ( semi == -1 )
					{
						break;
					}
					continue;
				}

				String attributeName = attribute.substring( 0, equals );
				String attributeValue = attribute.substring( equals + 1 );

				if ( attributeName.equalsIgnoreCase( "path" ) )
				{
					this.path = attributeValue;
				}

				// Expires, Max-Age

				if ( semi == -1)
				{
					break;
				}
			}
		}

		public String getName()
		{
			return this.name;
		}

		public String getValue()
		{
			return this.value;
		}

		public String getPath()
		{
			return this.path;
		}

		public boolean validPath( String path )
		{
			return path.startsWith( this.path );
		}

		@Override
		public String toString()
		{
			return this.stringValue;
		}

		@Override
		public boolean equals( final Object o )
		{
			if ( o instanceof ServerCookie )
			{
				return this.compareTo( (ServerCookie) o ) == 0;
			}
			return false;
		}

		@Override
		public int hashCode()
		{
			return this.name != null ? this.name.hashCode() : 0;
		}

		public int compareTo( final ServerCookie o )
		{
			return o == null ? -1 : this.getName().compareTo( o.getName() );
		}
	}
}
