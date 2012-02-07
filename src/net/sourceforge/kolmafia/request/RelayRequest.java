/**
 * Copyright (c) 2005-2012, KoLmafia development team
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
import java.io.PrintStream;

import java.net.HttpURLConnection;
import java.net.URLDecoder;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.java.dev.spellcast.utilities.DataUtilities;
import net.java.dev.spellcast.utilities.UtilityConstants;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.AreaCombatData;
import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.FamiliarData;
import net.sourceforge.kolmafia.KoLAdventure;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.KoLmafiaASH;
import net.sourceforge.kolmafia.RequestEditorKit;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.SpecialOutfit;
import net.sourceforge.kolmafia.StaticEntity;

import net.sourceforge.kolmafia.chat.ChatFormatter;
import net.sourceforge.kolmafia.chat.ChatPoller;
import net.sourceforge.kolmafia.chat.ChatSender;
import net.sourceforge.kolmafia.chat.HistoryEntry;

import net.sourceforge.kolmafia.moods.RecoveryManager;

import net.sourceforge.kolmafia.objectpool.FamiliarPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;

import net.sourceforge.kolmafia.persistence.AdventureDatabase;
import net.sourceforge.kolmafia.persistence.CustomItemDatabase;
import net.sourceforge.kolmafia.persistence.EffectDatabase;
import net.sourceforge.kolmafia.persistence.EquipmentDatabase;
import net.sourceforge.kolmafia.persistence.FamiliarDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.persistence.NPCStoreDatabase;
import net.sourceforge.kolmafia.persistence.SkillDatabase;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.session.EquipmentManager;
import net.sourceforge.kolmafia.session.InventoryManager;
import net.sourceforge.kolmafia.session.SorceressLairManager;
import net.sourceforge.kolmafia.session.TavernManager;
import net.sourceforge.kolmafia.session.TurnCounter;

import net.sourceforge.kolmafia.swingui.AdventureFrame;
import net.sourceforge.kolmafia.swingui.CommandDisplayFrame;

import net.sourceforge.kolmafia.swingui.widget.ShowDescriptionList;

import net.sourceforge.kolmafia.utilities.ByteBufferUtilities;
import net.sourceforge.kolmafia.utilities.FileUtilities;
import net.sourceforge.kolmafia.utilities.PauseObject;
import net.sourceforge.kolmafia.utilities.StringUtilities;

import net.sourceforge.kolmafia.webui.IslandDecorator;
import net.sourceforge.kolmafia.webui.RelayServer;
import net.sourceforge.kolmafia.webui.StationaryButtonDecorator;

public class RelayRequest
	extends PasswordHashRequest
{
	private final PauseObject pauser = new PauseObject();

	private static final HashMap overrideMap = new HashMap();

	private static final Pattern WHITESPACE_PATTERN = Pattern.compile( "['\\s-]" );
	private static final Pattern STORE_PATTERN =
		Pattern.compile( "<tr><td><input name=whichitem type=radio value=(\\d+).*?</tr>", Pattern.DOTALL );

	private static final Pattern ITEMID_PATTERN = Pattern.compile( "whichitem=(\\d+)" );

	private static KoLAdventure lastSafety = null;

	private final boolean allowOverride;
	public List headers = new ArrayList();
	public byte[] rawByteBuffer = null;
	public String contentType = null;
	public String statusLine = "HTTP/1.1 302 Found";

	public static String specialCommandResponse = "";
	public static String specialCommandStatus = "";
	public static String redirectedCommandURL = "";

	public RelayRequest( final boolean allowOverride )
	{
		super( "" );
		this.allowOverride = allowOverride && Preferences.getBoolean( "relayAllowsOverrides" );
	}

	public String getHashField()
	{
		// Do not automatically include the password hash on requests
		// relayed from the browser.
		return null;
	}

	public GenericRequest constructURLString( final String newURLString, final boolean usePostMethod, final boolean encoded )
	{
		super.constructURLString( newURLString, usePostMethod, encoded );

		this.rawByteBuffer = null;
		this.headers.clear();

		String path = this.getBasePath();

		if ( path.endsWith( ".css" ) )
		{
			this.contentType = "text/css";
		}
		else if ( path.endsWith( ".js" ) )
		{
			this.contentType = "text/javascript";
		}
		else if ( path.endsWith( ".gif" ) )
		{
			this.contentType = "image/gif";
		}
		else if ( path.endsWith( ".png" ) )
		{
			this.contentType = "image/png";
		}
		else if ( path.endsWith( ".jpg" ) || path.endsWith( ".jpeg" ) )
		{
			this.contentType = "image/jpeg";
		}
		else if ( path.endsWith( ".ico" ) )
		{
			this.contentType = "image/x-icon";
		}
		else if ( path.endsWith( ".php" ) || path.endsWith( ".html" ) || path.endsWith( ".ash" ) )
		{
			this.contentType = "text/html";
		}
		else
		{
			this.contentType = "text/plain";
		}

		return this;
	}

	private static final boolean isJunkItem( final int itemId, final int price )
	{
		if ( !Preferences.getBoolean( "relayHidesJunkMallItems" ) )
		{
			return false;
		}

		if ( price > KoLCharacter.getAvailableMeat() )
		{
			return true;
		}

		if ( NPCStoreDatabase.contains( ItemDatabase.getItemName( itemId ) ) )
		{
			if ( price == 100 || price > Math.abs( ItemDatabase.getPriceById( itemId ) ) * 2 )
			{
				return true;
			}
		}

		for ( int i = 0; i < KoLConstants.junkList.size(); ++i )
		{
			if ( ( (AdventureResult) KoLConstants.junkList.get( i ) ).getItemId() == itemId )
			{
				return true;
			}
		}

		return false;
	}

	protected boolean retryOnTimeout()
	{
		return false;
	}

	public void formatResponse()
	{
		this.statusLine = "HTTP/1.1 200 OK";
		String path = this.getBasePath();
		StringBuffer responseBuffer = new StringBuffer( this.responseText );

		// Fix KoLmafia getting outdated by events happening
		// in the browser by using the sidepane.

		if ( path.equals( "chatlaunch.php" ) )
		{
			if ( Preferences.getBoolean( "relayAddsGraphicalCLI" ) )
			{
				int linkIndex = responseBuffer.indexOf( "<a href" );
				if ( linkIndex != -1 )
				{
					responseBuffer.insert(
						linkIndex,
						"<a href=\"cli.html\"><b>KoLmafia gCLI</b></a></center><p>Type KoLmafia scripting commands in your browser!</p><center>" );
				}
			}

			if ( Preferences.getBoolean( "relayUsesIntegratedChat" ) )
			{
				StringUtilities.singleStringReplace( responseBuffer, "lchat.php", "chat.html" );
			}
		}

		// If this is a store, you can opt to remove all the min-priced
		// items from view along with all the items which are priced
		// above affordable levels.

		else if ( path.startsWith( "mallstore.php" ) )
		{
			int searchItemId = -1;
			int searchPrice = -1;

			searchItemId = StringUtilities.parseInt( this.getFormField( "searchitem" ) );
			searchPrice = StringUtilities.parseInt( this.getFormField( "searchprice" ) );

			Matcher itemMatcher = RelayRequest.STORE_PATTERN.matcher( this.responseText );

			while ( itemMatcher.find() )
			{
				String itemData = itemMatcher.group( 1 );

				int itemId = StringUtilities.parseInt( itemData.substring( 0, itemData.length() - 9 ) );
				int price = StringUtilities.parseInt( itemData.substring( itemData.length() - 9 ) );

				if ( itemId != searchItemId && RelayRequest.isJunkItem( itemId, price ) )
				{
					StringUtilities.singleStringDelete( responseBuffer, itemMatcher.group() );
				}
			}

			// Also make sure the item that the person selected
			// when coming into the store is pre-selected.

			if ( searchItemId != -1 )
			{
				String searchString = MallPurchaseRequest.getStoreString( searchItemId, searchPrice );
				StringUtilities.singleStringReplace(
					responseBuffer, "value=" + searchString, "checked value=" + searchString );
			}
		}
		else if ( path.startsWith( "fight.php" ) )
		{
			String action = this.getFormField( "action" );
			if ( action != null && action.equals( "skill" ) )
			{
				String skillId = this.getFormField( "whichskill" );
				String category = SkillDatabase.getSkillCategory( StringUtilities.parseInt( skillId ) );
				if ( !category.equals( "conditional" ) )
				{
					StationaryButtonDecorator.addButton( skillId );
				}
			}
		}

		try
		{
			RequestEditorKit.getFeatureRichHTML( this.getURLString(), responseBuffer, true );
		}
		catch ( Exception e )
		{
			StaticEntity.printStackTrace( e );
		}

		// Load image files locally to reduce bandwidth
		// and improve mini-browser performance.

		if ( Preferences.getBoolean( "relayUsesCachedImages" ) )
		{
			StringUtilities.globalStringReplace( responseBuffer, "http://images.kingdomofloathing.com", "/images" );
		}
		else
		{
			StringUtilities.globalStringReplace(
				responseBuffer, "http://images.kingdomofloathing.com/scripts", "/images/scripts" );
		}

		// Download and link to any Players of Loathing
		// picture pages locally.

		StringUtilities.globalStringReplace( responseBuffer, "http://pics.communityofloathing.com/albums", "/images" );

		// Remove the default frame busting script so that
		// we can detach user interface elements.

		StringUtilities.singleStringReplace( responseBuffer, "frames.length == 0", "frames.length == -1" );
		StringUtilities.globalStringReplace( responseBuffer, " name=adv ", " name=snarfblat " );

		this.responseText = responseBuffer.toString();

		CustomItemDatabase.linkCustomItem( this );
	}

	public void printHeaders( final PrintStream ostream )
	{
		if ( !this.headers.isEmpty() )
		{
			for ( int i = 0; i < this.headers.size(); ++i )
			{
				ostream.println( this.headers.get( i ) );
			}
		}
		else if ( this.formConnection != null )
		{
			HttpURLConnection connection = this.formConnection;

			String header;

			for ( int i = 0; ( header = connection.getHeaderFieldKey( i ) ) != null; ++i )
			{
				if ( header.startsWith( "Content" ) || header.startsWith( "Cache" ) || header.equals( "Pragma" ) || header.equals( "Set-Cookie" ) )
				{
					continue;
				}

				ostream.print( header );
				ostream.print( ": " );
				ostream.println( connection.getHeaderField( i ) );
			}

			if ( this.responseCode == 200 && this.rawByteBuffer != null )
			{
				ostream.print( "Content-Type: " );
				ostream.print( this.contentType );

				if ( this.contentType.startsWith( "text" ) )
				{
					ostream.print( "; charset=UTF-8" );
				}

				ostream.println();

				ostream.print( "Content-Length: " );
				ostream.print( this.rawByteBuffer.length );
				ostream.println();

				ostream.println( "Cache-Control: no-cache, must-revalidate" );
				ostream.println( "Pragma: no-cache" );
			}
		}
	}

	public void pseudoResponse( final String status, final String responseText )
	{
		this.statusLine = status;

		this.headers.add( "Date: " + new Date() );
		this.headers.add( "Server: " + KoLConstants.VERSION_NAME );

		if ( status.indexOf( "302" ) != -1 )
		{
			this.headers.add( "Location: " + responseText );

			this.responseCode = 302;
			this.responseText = "";
		}
		else if ( status.indexOf( "200" ) != -1 )
		{
			this.responseCode = 200;

			if ( responseText.length() == 0 )
			{
				this.responseText = " ";
			}
			else
			{
				this.rawByteBuffer = null;
				this.responseText = responseText;
			}

			if ( this.contentType.equals( "text/html" ) )
			{
				this.headers.add( "Content-Type: text/html; charset=UTF-8" );
				this.headers.add( "Cache-Control: no-cache, must-revalidate" );
				this.headers.add( "Pragma: no-cache" );
			}
			else
			{
				this.headers.add( "Content-Type: " + this.contentType );
			}
		}
		else
		{
			this.responseText = " ";
		}

		this.headers.add( "Connection: close" );
	}

	private StringBuffer readContents( final BufferedReader reader )
	{
		StringBuffer contentBuffer = new StringBuffer();
		if ( reader == null )
		{
			return contentBuffer;
		}

		try
		{
			String line;
			while ( ( line = reader.readLine() ) != null )
			{
				contentBuffer.append( line );
				contentBuffer.append( KoLConstants.LINE_BREAK );
			}

			reader.close();
		}
		catch ( IOException e )
		{
		}

		return contentBuffer;
	}

	private static final String [] IMAGES = new String[]
	{
		// Alternating path, original, replacement
		"images/adventureimages/hellion.gif",
		"http://images.kingdomofloathing.com/adventureimages/hellion.gif\" width=100 height=100",
		"/images/adventureimages/hellion.gif\" width=60 height=100",
	};

	public static final void overrideImages( final StringBuffer buffer )
	{
		if ( !Preferences.getBoolean( "relayOverridesImages" ) )
		{
			return;
		}

		for ( int i = 0; i < IMAGES.length; i += 3 )
		{
			String find = IMAGES[ i + 1 ] ;
			String replace = IMAGES[ i + 2 ] ;
			StringUtilities.globalStringReplace( buffer, find, replace );
		}
	}

	public static final void flushOverrideImages()
	{
		for ( int i = 0; i < IMAGES.length; i += 3 )
		{
			// Copy built-in override file to images
			String filename = IMAGES[ i ];
			File cachedFile = new File( UtilityConstants.ROOT_LOCATION, filename );
			cachedFile.delete();
		}
	}

	private static final String OVERRIDE_DIRECTORY = "images/overrides/";

	private void sendOverrideImage( final String filename )
	{
		if ( Preferences.getBoolean( "relayOverridesImages" ) )
		{
			for ( int i = 0; i < IMAGES.length; i += 3 )
			{
				if ( filename.equals( IMAGES[ i ] ) )
				{
					// Copy built-in override file to images
					int index = filename.lastIndexOf( "/" );
					File cachedFile = new File( UtilityConstants.ROOT_LOCATION, filename.substring( 0, index ) );
					String localname = filename.substring( index + 1 );
					FileUtilities.loadLibrary( cachedFile, RelayRequest.OVERRIDE_DIRECTORY, localname );
					break;
				}
			}
		}
		this.sendLocalImage( filename );
	}

	private void sendLocalImage( final String filename )
	{
		File imageFile = FileUtilities.downloadImage( "http://images.kingdomofloathing.com" + filename.substring( 6 ) );

		if ( imageFile == null )
		{
			this.sendNotFound();
			return;
		}

		this.rawByteBuffer = ByteBufferUtilities.read( imageFile );
		this.pseudoResponse( "HTTP/1.1 200 OK", "" );
	}

	private void sendLocalFile( final String filename )
	{
		if ( !RelayRequest.overrideMap.containsKey( filename ) )
		{
			RelayRequest.overrideMap.put( filename, new File( KoLConstants.RELAY_LOCATION, filename ) );
		}

		File override = (File) RelayRequest.overrideMap.get( filename );

		if ( override == null || !override.exists() )
		{
			this.sendNotFound();
			return;
		}

		try
		{
			String overridePath = override.getCanonicalPath();
			String relayPath = KoLConstants.RELAY_LOCATION.getCanonicalPath();

			if ( !overridePath.startsWith( relayPath ) )
			{
				this.sendNotFound();
				return;
			}
		}
		catch ( IOException e )
		{

		}

		StringBuffer replyBuffer = this.readContents( DataUtilities.getReader( override ) );

		if ( filename.equals( "chat.html" ) )
		{
			StringUtilities.singleStringReplace(
				replyBuffer, "CHATAUTH",
				"playerid=" + KoLCharacter.getPlayerId() + "&pwd=" + GenericRequest.passwordHash );
		}

		StringUtilities.globalStringReplace( replyBuffer, "MAFIAHIT", "pwd=" + GenericRequest.passwordHash );

		// Print the reply buffer to the response buffer for the local
		// relay server.

		if ( this.isChatRequest )
		{
			StringUtilities.globalStringReplace( replyBuffer, "<br>", "</font><br>" );
		}

		if ( filename.endsWith( "chat.html" ) )
		{
			RequestEditorKit.addChatFeatures( replyBuffer );
		}

		this.pseudoResponse( "HTTP/1.1 200 OK", replyBuffer.toString() );
	}

	public void insertAfterEnd( final StringBuffer replyBuffer, final String contents )
	{
		int terminalIndex = replyBuffer.lastIndexOf( "</html>" );
		if ( terminalIndex == -1 )
		{
			replyBuffer.append( contents );
			replyBuffer.append( "</html>" );
		}
		else
		{
			replyBuffer.insert( terminalIndex, contents );
		}
	}

	public void sendNotFound()
	{
		this.pseudoResponse( "HTTP/1.1 404 Not Found", "" );
		this.responseCode = 404;
	}

	private boolean sendBattlefieldWarning( final String urlString, final KoLAdventure adventure )
	{
		// If user has already confirmed he wants to go there, accept it
		if ( this.getFormField( "confirm" ) != null )
		{
			return false;
		}

		// If visiting big island, see if trying to enter a camp
		if ( urlString.startsWith( "bigisland.php" ) )
		{
			return checkCampVisit( urlString );
		}

		if ( adventure == null )
		{
			return false;
		}

		String location = adventure.getAdventureId();

		// If he's not going on to the battlefield, no problem
		if ( !location.equals( "132" ) && !location.equals( "140" ) )
		{
			return false;
		}

		// You can't tell which uniform is being worn from the URL;
		// the player can adventure in one uniform, change uniform,
		// and click on the Last Adventure link

		SpecialOutfit outfit = EquipmentManager.currentOutfit();

		// If he's not in a uniform, no battle
		if ( outfit == null )
		{
			return false;
		}

		int outfitId = outfit.getOutfitId();
		switch ( outfitId )
		{
		case 32:
			// War Hippy Fatigues
		case 33:
			// Frat Warrior Fatigues
			return checkBattle( outfitId );
		}

		return false;
	}

	private boolean checkCampVisit( final String urlString )
	{
		CoinmasterData data = IslandDecorator.findCampMaster( urlString );

		// If he's not attempting to enter a camp, no problem.
		if ( data == null )
		{
			return false;
		}

		SpecialOutfit outfit = EquipmentManager.currentOutfit();

		// If he's not in a uniform, no visit
		if ( outfit == null )
		{
			return false;
		}

		switch ( outfit.getOutfitId() )
		{
		case 32:
			// War Hippy Fatigues
			if ( data == DimemasterRequest.HIPPY )
			{
				return false;
			}
			break;

		case 33:
			// Frat Warrior Fatigues
			if ( data == QuartersmasterRequest.FRATBOY )
			{
				return false;
			}
			break;

		default:
			return false;
		}

		// He is attempting to visit the opposing camp in uniform.
		// This will prompt the final confrontation.
		// Offer a chance to cash in dimes and quarters.

		this.sendCoinMasterWarning( data );
		return true;
	}

	private boolean checkBattle( final int outfitId )
	{
		int fratboysDefeated = IslandDecorator.fratboysDefeated();
		int hippiesDefeated = IslandDecorator.hippiesDefeated();

		if ( fratboysDefeated == 999 && hippiesDefeated == 999 )
		{
			this.sendCoinMasterWarning( null );
			return true;
		}

		if ( fratboysDefeated == 999 && outfitId == 32 )
		{
			// In hippy uniform and about to defeat last fratboy.
			int factor = IslandDecorator.hippiesDefeatedPerBattle();
			if ( hippiesDefeated < 999 && ( 999 - hippiesDefeated ) % factor == 0 )
			{
				this.sendWossnameWarning( QuartersmasterRequest.FRATBOY );
				return true;
			}
		}

		if ( hippiesDefeated == 999 && outfitId == 33 )
		{
			// In fratboy uniform and about to defeat last hippy.
			int factor = IslandDecorator.fratboysDefeatedPerBattle();
			if ( fratboysDefeated < 999 && ( 999 - fratboysDefeated ) % factor == 0 )
			{
				this.sendWossnameWarning( DimemasterRequest.HIPPY );
				return true;
			}
		}

		return false;
	}

	private void sendCoinMasterWarning( final CoinmasterData camp )
	{
		String message;

		if ( camp == null )
		{
			message = "You are about to enter the final confrontation with the two bosses.";
		}
		else
		{
			String master = ( camp == DimemasterRequest.HIPPY ? "hippy" : "fratboy" );
			message = "You are about to enter the " + master + " camp and confront the boss.";
		}

		message =
			message + " Before you do so, you might want to redeem war loot for dimes and quarters and buy equipment. Click on the image to enter battle, once you are ready.";

		this.sendGeneralWarning( "lucre.gif", message );
	}

	private boolean sendInfernalSealWarning( final String urlString )
	{
		// If user has already confirmed he wants to do it, accept it
		if ( this.getFormField( "confirm" ) != null )
		{
			return false;
		}

		// If he's not using an item, nothing to worry about
		if ( !urlString.startsWith( "inv_use.php" ) )
		{
			return false;
		}

		Matcher matcher = RelayRequest.ITEMID_PATTERN.matcher( urlString );
		if ( !matcher.find() )
		{
			return false;
		}

		// If it's not a seal figurine, no problem
		if ( !ItemDatabase.isSealFigurine( StringUtilities.parseInt( matcher.group(1) ) ) )
		{
			return false;
		}

		// If he's wielding a club, life is wonderful. ish.
		if ( EquipmentManager.wieldingClub() )
		{
			return false;
		}

		String message;

		message =
			"You are trying to summon an infernal seal, but you are not wielding a club. You are either incredibly puissant or incredibly foolish. If you are sure you want to do this, click on the image and proceed to your doom.";

		this.sendGeneralWarning( "iblubbercandle.gif", message, "checked=1" );

		return true;
	}

	private void sendWossnameWarning( final CoinmasterData camp )
	{
		String message;
		String side1 = ( camp == DimemasterRequest.HIPPY ? "hippy" : "fratboy" );
		String side2 = ( camp == DimemasterRequest.HIPPY ? "fratboys" : "hippies" );

		message =
			"You are about to defeat the last " + side1 + " and open the way to their camp. However, you have not yet finished with the " + side2 + ". If you are sure you don't want the Order of the Silver Wossname, click on the image and proceed.";

		this.sendGeneralWarning( "wossname.gif", message );
	}

	private void sendFamiliarWarning()
	{
		StringBuffer warning = new StringBuffer();

		warning.append( "<html><head><script language=Javascript src=\"/basics.js\"></script>" );

		warning.append( "<link rel=\"stylesheet\" type=\"text/css\" href=\"http://images.kingdomofloathing.com/styles.css\"></head>" );
		warning.append( "<body><center><table width=95%	 cellspacing=0 cellpadding=0><tr><td style=\"color: white;\" align=center bgcolor=blue><b>Results:</b></td></tr><tr><td style=\"padding: 5px; border: 1px solid blue;\"><center><table><tr><td><center>" );

		warning.append( "<table><tr>" );

		String url = this.getURLString();

		// Proceed with clover
		warning.append( "<td align=center valign=center><div id=\"lucky\" style=\"padding: 4px 4px 4px 4px\"><a style=\"text-decoration: none\" href=\"" );
		warning.append( url );
		warning.append( url.indexOf( "?" ) == -1 ? "?" : "&" );
		warning.append( "confirm=on\"><img src=\"http://images.kingdomofloathing.com/itemimages/");
		warning.append( FamiliarDatabase.getFamiliarImageLocation( KoLCharacter.getFamiliar().getId() ) );
		warning.append( "\" width=30 height=30 border=0>" );
		warning.append( "</a></div></td>" );

		warning.append( "<td>&nbsp;&nbsp;&nbsp;&nbsp;</td>" );

		// Protect clover
		warning.append( "<td align=center valign=center><div id=\"unlucky\" style=\"padding: 4px 4px 4px 4px\">" );

		warning.append( "<a style=\"text-decoration: none\" href=\"#\" onClick=\"singleUse('familiar.php', 'action=newfam&ajax=1&newfam=" );
		warning.append( FamiliarData.getSingleFamiliarRun() );
		warning.append( "'); void(0);\">" );
		warning.append( "<img src=\"http://images.kingdomofloathing.com/itemimages/");
		warning.append( FamiliarDatabase.getFamiliarImageLocation( FamiliarData.getSingleFamiliarRun() ) );
		warning.append( "\" width=30 height=30 border=0>" );
		warning.append( "</a></div></td>" );

		warning.append( "</tr></table></center><blockquote>KoLmafia has detected that you may be doing a 100% familiar run.  If you are sure you wish to deviate from this path, click on the familiar on the left.  If this was an accident, please click on the familiar on the right to switch to your 100% familiar." );
		warning.append( "</blockquote></td></tr></table></center></td></tr></table></center></body></html>" );

		this.pseudoResponse( "HTTP/1.1 200 OK", warning.toString() );
	}

	public void sendBossWarning( final String name, final String image, final int mcd1, final String item1,
		final int mcd2, final String item2 )
	{
		int mcd0 = KoLCharacter.getMindControlLevel();

		StringBuffer warning = new StringBuffer();

		warning.append( "<html><head><script language=Javascript src=\"/basics.js\"></script>" );

		warning.append( "<script language=Javascript> " );
		warning.append( "var default0 = " + mcd0 + "; " );
		warning.append( "var default1 = " + mcd1 + "; " );
		warning.append( "var default2 = " + mcd2 + "; " );
		warning.append( "var current = " + mcd0 + "; " );
		warning.append( "function switchLinks( id ) { " );
		warning.append( "if ( id == \"mcd1\" ) { " );
		warning.append( "current = (current == default0) ? default1 : default0; " );
		warning.append( "} else { " );
		warning.append( "current = (current == default0) ? default2 : default0; " );
		warning.append( "} " );
		warning.append( "getObject(\"mcd1\").style.border = (current == default1) ? \"1px dashed blue\" : \"1px dashed white\"; " );
		warning.append( "getObject(\"mcd2\").style.border = (current == default2) ? \"1px dashed blue\" : \"1px dashed white\"; " );
		warning.append( "top.charpane.location.href = \"/KoLmafia/sideCommand?cmd=mcd+\" + current + \"&pwd=" );
		warning.append( GenericRequest.passwordHash );
		warning.append( "\"; } </script>" );

		warning.append( "<link rel=\"stylesheet\" type=\"text/css\" href=\"http://images.kingdomofloathing.com/styles.css\"></head>" );
		warning.append( "<body><center><table width=95%	 cellspacing=0 cellpadding=0><tr><td style=\"color: white;\" align=center bgcolor=blue><b>Results:</b></td></tr><tr><td style=\"padding: 5px; border: 1px solid blue;\"><center><table><tr><td><center>" );

		warning.append( "<table><tr>" );

		warning.append( "<td align=center valign=center><div id=\"mcd1\" style=\"padding: 4px 4px 4px 4px" );

		if ( mcd0 == mcd1 )
		{
			warning.append( "; border: 1px dashed blue" );
		}

		warning.append( "\"><a id=\"link1\" style=\"text-decoration: none\" onClick=\"switchLinks('mcd1'); void(0);\" href=\"#\"><img src=\"http://images.kingdomofloathing.com/itemimages/" );
		warning.append( item1 );
		warning.append( "\" width=30 height=30><br /><font size=1>MCD " );
		warning.append( mcd1 );
		warning.append( "</font></a></div></td>" );

		warning.append( "<td>&nbsp;&nbsp;&nbsp;&nbsp;</td>" );

		warning.append( "<td valign=center><a href=\"" );
		warning.append( this.getURLString() );
		warning.append( "&confirm=on" );
		warning.append( "\"><img src=\"http://images.kingdomofloathing.com/adventureimages/" );
		warning.append( image );
		warning.append( "\"></a></td>" );

		warning.append( "<td>&nbsp;&nbsp;&nbsp;&nbsp;</td>" );

		warning.append( "<td align=center valign=center><div id=\"mcd2\" style=\"padding: 4px 4px 4px 4px" );

		if ( KoLCharacter.getMindControlLevel() == mcd2 )
		{
			warning.append( "; border: 1px dashed blue" );
		}

		warning.append( "\"><a id=\"link2\" style=\"text-decoration: none\" onClick=\"switchLinks('mcd2'); void(0);\" href=\"#\"><img src=\"http://images.kingdomofloathing.com/itemimages/" );
		warning.append( item2 );
		warning.append( "\" width=30 height=30><br /><font size=1>MCD " );
		warning.append( mcd2 );
		warning.append( "</font></a></div></td>" );

		warning.append( "</tr></table></center><blockquote>" );
		warning.append( name );

		warning.append( " drops special rewards based on your mind-control level.  If you'd like a special reward, click on one of the items above to set your mind-control device appropriately.  Click on it again to reset the MCD back to your old setting.	 Click on " );
		warning.append( name );

		warning.append( " once you've decided to proceed.</blockquote></td></tr></table></center></td></tr></table></center></body></html>" );

		this.pseudoResponse( "HTTP/1.1 200 OK", warning.toString() );
	}

	public void sendGeneralWarning( final String image, final String message )
	{
		this.sendGeneralWarning( image, message, null );
	}

	public void sendGeneralWarning( final String image, final String message, final String extra )
	{
		StringBuffer warning = new StringBuffer();

		warning.append( "<html><head><link rel=\"stylesheet\" type=\"text/css\" href=\"http://images.kingdomofloathing.com/styles.css\"><script language=\"Javascript\" src=\"/basics.js\"></script></head><body><center><table width=95%  cellspacing=0 cellpadding=0><tr><td style=\"color: white;\" align=center bgcolor=blue><b>Results:</b></td></tr><tr><td style=\"padding: 5px; border: 1px solid blue;\"><center><table><tr><td>" );

		if ( image != null && !image.equals( "" ) )
		{
			String url = this.getURLString();

			warning.append( "<center><a href=\"" );
			warning.append( url );
			warning.append( url.indexOf( "?" ) == -1 ? "?" : "&" );
			warning.append( "confirm=on" );
			if ( extra != null )
			{
				warning.append( "&" );
				warning.append( extra );
			}
			warning.append( "\"><img id=\"warningImage\" src=\"http://images.kingdomofloathing.com/itemimages/" );
			warning.append( image );
			warning.append( "\" width=30 height=30></a><br></center>" );
		}

		warning.append( "<blockquote>" );
		warning.append( message );
		warning.append( "</blockquote></td></tr></table></center></td></tr></table></center></body></html>" );

		this.pseudoResponse( "HTTP/1.1 200 OK", warning.toString() );
	}

	public void sendCloverWarning()
	{
		StringBuffer warning = new StringBuffer();
		boolean beeCore = KoLCharacter.inBeecore();

		warning.append( "<html><head><script language=Javascript src=\"/basics.js\"></script>" );

		warning.append( "<link rel=\"stylesheet\" type=\"text/css\" href=\"http://images.kingdomofloathing.com/styles.css\"></head>" );
		warning.append( "<body><center><table width=95%	 cellspacing=0 cellpadding=0><tr><td style=\"color: white;\" align=center bgcolor=blue><b>Results:</b></td></tr><tr><td style=\"padding: 5px; border: 1px solid blue;\"><center><table><tr><td><center>" );

		warning.append( "<table><tr>" );

		String url = this.getURLString();

		// Proceed with clover
		warning.append( "<td align=center valign=center><div id=\"lucky\" style=\"padding: 4px 4px 4px 4px\"><a style=\"text-decoration: none\" href=\"" );
		warning.append( url );
		warning.append( url.indexOf( "?" ) == -1 ? "?" : "&" );
		warning.append( "confirm=on\"><img src=\"http://images.kingdomofloathing.com/itemimages/clover.gif\" width=30 height=30 border=0>" );
		warning.append( "</a></div></td>" );

		warning.append( "<td>&nbsp;&nbsp;&nbsp;&nbsp;</td>" );

		// Protect clover
		warning.append( "<td align=center valign=center><div id=\"unlucky\" style=\"padding: 4px 4px 4px 4px\">" );
		if ( beeCore )
		{
			// fillcloset.php?action=closetpush&whichitem=24&qty=all&pwd&ajax=1
			warning.append( "<a style=\"text-decoration: none\" href=\"#\" onClick=\"inlineLoad('fillcloset.php', 'action=closetpush&whichitem=" );
			warning.append( ItemPool.TEN_LEAF_CLOVER );
			warning.append( "&qty=all&pwd=" );
			warning.append( GenericRequest.passwordHash );
			warning.append( "&ajax=1', " );
			warning.append( ItemPool.TEN_LEAF_CLOVER );
			warning.append( "); void(0);\">" );
			warning.append( "<img src=\"/images/closet.gif\" width=30 height=30 border=0>" );
		}
		else
		{
			warning.append( "<a style=\"text-decoration: none\" href=\"#\" onClick=\"multiUse('multiuse.php', " );
			warning.append( ItemPool.TEN_LEAF_CLOVER );
			warning.append( ", " );
			warning.append( InventoryManager.getCount( ItemPool.TEN_LEAF_CLOVER ) );
			warning.append( "); void(0);\">" );
			warning.append( "<img src=\"http://images.kingdomofloathing.com/itemimages/disclover.gif\" width=30 height=30 border=0>" );
		}
		warning.append( "</a></div></td>" );

		warning.append( "</tr></table></center><blockquote>KoLmafia has detected a ten-leaf clover in your inventory.  If you are sure you wish to use it, click on the assembled clover on the left.  If this was an accident, please click on the " );
		if ( beeCore )
		{
			warning.append( "closet on the right to closet" );
		}
		else
		{
			warning.append( "disassembled clover on the right to disassemble" );
		}
		warning.append( " your clovers first.  To disable this warning, please check your preferences and disable clover protection.</blockquote></td></tr></table></center></td></tr></table></center></body></html>" );

		this.pseudoResponse( "HTTP/1.1 200 OK", warning.toString() );
	}

	private void handleSafety()
	{
		if ( RelayRequest.lastSafety == null )
		{
			this.pseudoResponse( "HTTP/1.1 200 OK", "" );
			return;
		}

		AreaCombatData combat = RelayRequest.lastSafety.getAreaSummary();

		if ( combat == null )
		{
			this.pseudoResponse( "HTTP/1.1 200 OK", "" );
			return;
		}

		StringBuffer buffer = new StringBuffer();
		buffer.append( "<table width=\"100%\"><tr><td align=left valign=top><font size=3>" );
		buffer.append( RelayRequest.lastSafety.getAdventureName() );
		buffer.append( "</font></td><td align=right valign=top><font size=2>" );

		buffer.append( "<a style=\"text-decoration: none\" href=\"javascript: " );

		buffer.append( "var safety; if ( document.getElementById ) " );
		buffer.append( "safety = top.chatpane.document.getElementById( 'safety' ); " );
		buffer.append( "else if ( document.all ) " );
		buffer.append( "safety = top.chatpane.document.all[ 'safety' ]; " );

		buffer.append( "safety.closed = true;" );
		buffer.append( "safety.active = false;" );
		buffer.append( "safety.style.display = 'none'; " );
		buffer.append( "var nodes = top.chatpane.document.body.childNodes; " );
		buffer.append( "for ( var i = 0; i < nodes.length; ++i ) " );
		buffer.append( "if ( nodes[i].style && nodes[i].id != 'safety' ) " );
		buffer.append( "nodes[i].style.display = nodes[i].unsafety; " );

		buffer.append( "void(0);\">x</a></font></td></tr></table>" );
		buffer.append( "<br/><font size=2>" );

		String combatData = combat.toString( true );
		combatData = combatData.substring( 6, combatData.length() - 7 );
		buffer.append( combatData );

		buffer.append( "</font>" );
		this.pseudoResponse( "HTTP/1.1 200 OK", buffer.toString() );
	}

	private void handleCommand()
	{
		// None of the above checks wound up happening. So, do some
		// special handling, catching any exceptions that happen to
		// popup along the way.

		String path = this.getBasePath();
		if ( path.endsWith( "submitCommand" ) )
		{
			submitCommand( this.getFormField( "cmd" ) );
			this.pseudoResponse( "HTTP/1.1 200 OK", "" );
		}
		else if ( path.endsWith( "redirectedCommand" ) )
		{
			submitCommand( this.getFormField( "cmd" ) );
			this.pseudoResponse( "HTTP/1.1 302 Found", RelayRequest.redirectedCommandURL );
		}
		else if ( path.endsWith( "sideCommand" ) )
		{
			submitCommand( this.getFormField( "cmd" ), true );
			this.pseudoResponse( "HTTP/1.1 302 Found", "/charpane.php" );
		}
		else if ( path.endsWith( "specialCommand" ) ||
			  path.endsWith( "parameterizedCommand" ) )
		{
			String cmd = this.getFormField( "cmd" );
			if ( !cmd.equals( "wait" ) )
			{
				RelayRequest.specialCommandResponse = "";
				RelayRequest.specialCommandStatus = "";
				if ( path.endsWith( "parameterizedCommand" ) )
				{
					String URL =  this.getURLString();
					int pwdStart = URL.indexOf( "pwd" );
					int pwdEnd = URL.indexOf( "&", pwdStart );
					String parameters = pwdEnd == -1 ? "" : URL.substring( pwdEnd + 1 );
					submitCommand( cmd + " " + parameters );
				}
				else
				{
					submitCommand( cmd, false, false );
				}
			}
			this.contentType = "text/html";
			if ( CommandDisplayFrame.hasQueuedCommands() )
			{
				String URL = "/KoLmafia/specialCommand?cmd=wait&pwd=" + GenericRequest.passwordHash;

				StringBuffer buffer = new StringBuffer();
				buffer.append( "<html><head>" );
				buffer.append( "<meta http-equiv=\"refresh\" content=\"1; URL=" );
				buffer.append( URL );
				buffer.append( "\">" );
				buffer.append( "</head><body>" );
				buffer.append( "<a href=\"" );
				buffer.append( URL );
				buffer.append( "\">" );
				buffer.append( "Automating (see CLI for details, click to refresh)..." );
				buffer.append( "</a><p>" );
				buffer.append( RelayRequest.specialCommandStatus );
				buffer.append( "</body></html>" );

				this.pseudoResponse( "HTTP/1.1 200 OK",	 buffer.toString() );
			}
			else if ( RelayRequest.specialCommandResponse.length() > 0 )
			{
				this.pseudoResponse( "HTTP/1.1 200 OK", RelayRequest.specialCommandResponse );
				RelayRequest.specialCommandResponse = "";
				RelayRequest.specialCommandStatus = "";
			}
			else
			{
				// specialCommand invoked for command that doesn't
				// specifically support it - we have no page to display.
				this.pseudoResponse( "HTTP/1.1 200 OK",
					"<html><body>Automation complete.</body></html>)" );
			}
		}
		else if ( path.endsWith( "logout" ) )
		{
			submitCommand( "logout" );
			this.pseudoResponse( "HTTP/1.1 302 Found", "/loggedout.php" );
		}
		else if ( path.endsWith( "messageUpdate" ) )
		{
			this.pseudoResponse( "HTTP/1.1 200 OK", RelayServer.getNewStatusMessages() );
		}
		else if ( path.endsWith( "lookupLocation" ) )
		{
			RelayRequest.lastSafety =
				AdventureDatabase.getAdventureByURL( "adventure.php?snarfblat=" + this.getFormField( "snarfblat" ) );
			AdventureFrame.updateSelectedAdventure( RelayRequest.lastSafety );
			this.handleSafety();
		}
		else if ( path.endsWith( "updateLocation" ) )
		{
			this.handleSafety();
		}
		else
		{
			this.pseudoResponse( "HTTP/1.1 200 OK", "" );
		}
	}

	private void submitCommand( String command )
	{
		submitCommand( command, false, true );
	}

	private void submitCommand( String command, boolean suppressUpdate )
	{
		submitCommand( command, suppressUpdate, true );
	}

	private void submitCommand( String command, boolean suppressUpdate, boolean waitForCompletion )
	{
		try
		{
			command = URLDecoder.decode( command, "UTF-8" );
		}
		catch ( Exception e )
		{
		}

		GenericRequest.suppressUpdate( suppressUpdate );
		CommandDisplayFrame.executeCommand( command );

		while ( waitForCompletion && CommandDisplayFrame.hasQueuedCommands() )
		{
			this.pauser.pause( 500 );
		}
		GenericRequest.suppressUpdate( false );
	}

	private void handleChat()
	{
		String chatText;

		if ( this.getPath().startsWith( "newchatmessages.php" ) )
		{
			StringBuffer chatResponse = new StringBuffer();

			long lastSeen = StringUtilities.parseLong( this.getFormField( "lasttime" ) );

			List chatMessages = ChatPoller.getEntries( lastSeen, true );
			Iterator messageIterator = chatMessages.iterator();

			boolean needsLineBreak = false;

			while ( messageIterator.hasNext() )
			{
				HistoryEntry chatMessage = (HistoryEntry) messageIterator.next();

				String content = chatMessage.getContent();

				if ( content != null && content.length() > 0 )
				{
					if ( needsLineBreak )
					{
						chatResponse.append( "<br>" );
					}

					needsLineBreak = !content.endsWith( "<br>" ) && !content.endsWith( "<br/>" ) && !content.endsWith( "</br>" );

					chatResponse.append( content );
				}

				lastSeen = Math.max( lastSeen, chatMessage.getLocalLastSeen() );
			}

			chatResponse.append( "<!--lastseen:" );
			chatResponse.append( KoLConstants.CHAT_LASTSEEN_FORMAT.format( lastSeen ) );
			chatResponse.append( "-->" );

			chatText = chatResponse.toString();
		}
		else
		{
			chatText = ChatSender.sendMessage( new LinkedList(), this.getFormField( "graf" ), true, false );
		}

		if ( Preferences.getBoolean( "relayFormatsChatText" ) )
		{
			chatText = ChatFormatter.formatExternalMessage( chatText );
		}

		this.pseudoResponse( "HTTP/1.1 200 OK", chatText );
	}

	public void handleSimple()
	{
		// If there is an attempt to view the error page, or if
		// there is an attempt to view the robots file, neither
		// are available on KoL, so return.

		String path = this.getBasePath();

		if ( path.equals( "missingimage.gif" ) || path.endsWith( "robots.txt" ) || path.endsWith( "favicon.ico" ) )
		{
			this.sendNotFound();
			return;
		}

		// If this is a command from the browser, handle it before
		// moving on to anything else.

		if ( path.startsWith( "KoLmafia/" ) )
		{
			this.handleCommand();
			return;
		}

		// Check to see if it's a request from the local images folder.
		// If it is, go ahead and send it.

		if ( path.startsWith( "images/playerpics/" ) )
		{
			FileUtilities.downloadImage( "http://pics.communityofloathing.com/albums/" + path.substring( path.indexOf( "playerpics" ) ) );

			this.sendLocalImage( path );
			return;
		}

		if ( path.startsWith( "images/" ) )
		{
			// We can override specific images.
			this.sendOverrideImage( path );
			return;
		}

		// If it's an ASH override script, handle it

		if ( path.endsWith( ".ash" ) )
		{
			KoLmafia.forceContinue();
			if ( !KoLmafiaASH.getClientHTML( this ) )
			{
				this.sendNotFound();
			}

			return;
		}

		// Local files never have form fields.	Remove them, because
		// they're probably just used for data tracking purposes
		// client-side.

		this.data.clear();
		this.sendLocalFile( path );
	}

	public void run()
	{
		String path = this.getBasePath();

		if ( path.startsWith( "http" ) )
		{
			super.run();
			return;
		}

		if ( !path.endsWith( ".php" ) )
		{
			this.handleSimple();
			return;
		}

		// If it's a chat request, handle it right away and return.
		// Otherwise, continue on.

		if ( this.isChatRequest )
		{
			this.handleChat();
			return;
		}

		// If it gets this far, consider firing a relay browser
		// override for it.

		if ( this.allowOverride && KoLmafiaASH.getClientHTML( this ) )
		{
			return;
		}

		if ( path.startsWith( "game.php" ) )
		{
			super.run();

			String mainpane = this.getFormField( "mainpane" );

			if ( mainpane != null )
			{
				if ( mainpane.indexOf( ".php" ) == -1 )
				{
					mainpane = mainpane + ".php";
				}

				this.responseText = StringUtilities.singleStringReplace(
					this.responseText, "main.php", mainpane );
			}

			return;
		}

		if ( path.startsWith( "lchat.php" ) )
		{
			super.run();

			this.responseText = StringUtilities.globalStringReplace( this.responseText, "<p>", "<br><br>" );
			this.responseText = StringUtilities.globalStringReplace( this.responseText, "<P>", "<br><br>" );
			this.responseText = StringUtilities.singleStringDelete( this.responseText, "</span>" );

			return;
		}

		// Load custom items from OneTonTomato's script if they
		// are currently being requested.
		if ( path.startsWith( "desc_item.php" ) )
		{
			String item = this.getFormField( "whichitem" );
			if ( item != null && item.startsWith( "custom" ) )
			{
				this.pseudoResponse( "HTTP/1.1 200 OK", CustomItemDatabase.retrieveCustomItem( item.substring( 6 ) ) );
				return;
			}

			if ( showWikiLink( item ) )
			{
				String location = ShowDescriptionList.getWikiLocation( ItemDatabase.getItemName( item ) );
				if ( location != null )
				{
					this.pseudoResponse( "HTTP/1.1 302 Found", location );
					return;
				}
			}
		}

		TurnCounter expired = TurnCounter.getExpiredCounter( this, true );
		while ( expired != null )
		{ // Read and discard expired informational counters
			expired = TurnCounter.getExpiredCounter( this, true );
		}

		StringBuffer msg = null;
		String image = null;
		boolean cookie = false;
		expired = TurnCounter.getExpiredCounter( this, false );
		while ( expired != null )
		{
			int remain = expired.getTurnsRemaining();
			if ( remain < 0 )
			{
				expired = TurnCounter.getExpiredCounter( this, false );
				continue;
			}
			if ( msg == null )
			{
				msg = new StringBuffer();
			}
			else
			{
				msg.append( "<br>" );
			}
			image = expired.getImage();
			if ( expired.getLabel().equals( "Fortune Cookie" ) )
			{
				cookie = true;
			}
			msg.append( "The " );
			msg.append( expired.getLabel() );
			switch ( remain )
			{
			case 0:
				msg.append( " counter has expired, so you may wish to adventure somewhere else at this time." );
				break;
			case 1:
				msg.append( " counter will expire after 1 more turn, so you may wish to adventure somewhere else that takes 1 turn." );
				break;
			default:
				msg.append( " counter will expire after " );
				msg.append( remain );
				msg.append( " more turns, so you may wish to adventure somewhere else for those turns." );
				break;
			}
			expired = TurnCounter.getExpiredCounter( this, false );
		}

		if ( msg != null )
		{
			msg.append( "<br>If you are certain that this is where you'd like to adventure, click on the image to proceed." );
			if ( cookie )
			{
				msg.append( "<br><br>" );
				msg.append( EatItemRequest.lastSemirareMessage() );
			}
			this.sendGeneralWarning( image, msg.toString() );
			return;
		}

		if ( path.startsWith( "desc_effect.php" ) && Preferences.getBoolean( "relayAddsWikiLinks" ) )
		{
			String effect = this.getFormField( "whicheffect" );
			String location =
				ShowDescriptionList.getWikiLocation( new AdventureResult(
					EffectDatabase.getEffectName( effect ), 1, true ) );

			if ( location != null )
			{
				this.pseudoResponse( "HTTP/1.1 302 Found", location );
				return;
			}
		}

		String urlString = this.getURLString();

		// Do some checks fighting infernal seals
		// - make sure player is wielding a club

		if ( this.sendInfernalSealWarning( urlString ) )
		{
			return;
		}

		KoLAdventure adventure = AdventureDatabase.getAdventureByURL( urlString );

		// Do some checks for the battlefield:
		// - make sure player doesn't lose a Wossname by accident
		// - give player chance to cash in dimes and quarters

		if ( this.sendBattlefieldWarning( urlString, adventure ) )
		{
			return;
		}

		String adventureName = adventure != null ?
			adventure.getAdventureName() :
			AdventureDatabase.getUnknownName( urlString );

		String nextAdventure = 
			adventureName != null ?
			adventureName :
			UseItemRequest.getAdventuresUsed( urlString ) > 0 ?
			"None" :
			null;

		if ( nextAdventure != null && RecoveryManager.isRecoveryPossible() )
		{
			KoLmafia.forceContinue();

			Preferences.setString( "lastAdventure", nextAdventure );
			RecoveryManager.runBetweenBattleChecks(
				Preferences.getBoolean( "relayRunsBeforeBattleScript" ),
				Preferences.getBoolean( "relayMaintainsEffects" ),
				Preferences.getBoolean( "relayMaintainsHealth" ),
				Preferences.getBoolean( "relayMaintainsMana" ) );

			if ( !KoLmafia.permitsContinue() )
			{
				this.sendGeneralWarning( null, "Between battle actions failed." );
				return;
			}
		}

		if ( nextAdventure != null || EquipmentRequest.isEquipmentChange( path ) )
		{
			// Wait until any restoration scripts finish running
			// before allowing an adventuring request to continue.

			this.waitForRecoveryToComplete();
		}

		if ( adventureName != null && this.getFormField( "confirm" ) == null )
		{
			AreaCombatData areaSummary = AdventureDatabase.getAreaCombatData( adventureName );

			// Check for a 100% familiar run if the current familiar
			// has zero combat experience.

			boolean isPossibleCombatLocation =
				this.data.isEmpty() && ( areaSummary == null || areaSummary.combats() > 0 || areaSummary.combats() == -1 );

			if ( KoLCharacter.getFamiliar().isUnexpectedFamiliar() && isPossibleCombatLocation &&
				 ( !KoLCharacter.kingLiberated() || KoLCharacter.getFamiliar().getId() == FamiliarPool.BLACK_CAT ) )
			{
				this.sendFamiliarWarning();
				return;
			}

			// Check for clovers as well so that people don't
			// accidentally use up a clover in the middle of a bad
			// moon run.

			if ( AdventureDatabase.isPotentialCloverAdventure( adventureName ) && Preferences.getBoolean( "cloverProtectActive" ) )
			{
				this.sendCloverWarning();
				return;
			}

			// Sometimes, people want the MCD rewards from various
			// boss monsters.  Let's help out.

			// This one's for the Boss Bat, who has special items at 4 and 8.
			if ( path.startsWith( "adventure.php" ) )
			{
				String location = adventure == null ? null : adventure.getAdventureId();

				if ( location != null && location.equals( "34" ) && KoLCharacter.mcdAvailable() )
				{
					this.sendBossWarning( "The Boss Bat", "bossbat.gif", 4, "batpants.gif", 8, "batbling.gif" );
					return;
				}
			}

			// This one is for the Knob Goblin King, who has special items at 3 and 7.
			else if ( path.startsWith( "cobbsknob.php" ) )
			{
				String action = this.getFormField( "action" );
				if ( action != null && action.equals( "throneroom" ) && KoLCharacter.mcdAvailable() )
				{
					this.sendBossWarning( "The Knob Goblin King", "goblinking.gif", 3, "glassballs.gif", 7, "batcape.gif" );
					return;
				}
			}

			// This one is for the Bonerdagon, who has special items at 5 and 10.
			else if ( path.startsWith( "crypt.php" ) )
			{
				String action = this.getFormField( "action" );
				if ( action != null && action.equals( "heart" ) && KoLCharacter.mcdAvailable() )
				{
					this.sendBossWarning( "The Bonerdagon", "bonedragon.gif", 5, "rib.gif", 10, "vertebra.gif" );
					return;
				}
			}

			// This one's for Baron von Ratsworth, who has special items at 2 and 9.
			else if ( path.startsWith( "cellar.php" ) )
			{
				String action = this.getFormField( "action" );
				String square = this.getFormField( "whichspot" );
				String baron = TavernManager.baronSquare();
				if ( action != null && action.equals( "explore" ) && square != null && square.equals( baron ) && KoLCharacter.mcdAvailable() )
				{
					this.sendBossWarning( "Baron von Ratsworth", "ratsworth.gif", 2, "moneyclip.gif", 9, "tophat.gif" );
					return;
				}
			}

			// If the person is visiting the sorceress and they
			// forgot to make the Wand, remind them.

			else if ( path.startsWith( "lair6.php" ) )
			{
				// As of NS 13,they need not have it
				// equipped. In fact, there are far better
				// weapons to equip for the battle. But, just
				// in case, check current equipment as well as
				// inventory.

				String place = this.getFormField( "place" );

				if ( place != null && place.equals( "5" ) )
				{
					StringBuffer warning =
						new StringBuffer(
							"It's possible there is something very important you're forgetting to do.<br>You lack:" );

					// You need the Antique hand mirror in Beecore
					if ( KoLCharacter.inBeecore() )
					{
						if ( !InventoryManager.retrieveItem( ItemPool.ANTIQUE_HAND_MIRROR ) )
						{
							warning.append( " <span title=\"Bedroom\">Antique hand mirror</span>" );
							this.sendGeneralWarning( "handmirror.gif", warning.toString() );
							return;
						}
					}
					else if ( !KoLCharacter.hasEquipped( SorceressLairManager.NAGAMAR ) && !InventoryManager.retrieveItem( SorceressLairManager.NAGAMAR ) )
					{
						if ( !InventoryManager.hasItem( ItemPool.WA ) )
						{
							if ( !InventoryManager.hasItem( ItemPool.RUBY_W ) )
							{
								warning.append( " <span title=\"Friar's Gate\">W</span>" );
							}
							if ( !InventoryManager.hasItem( ItemPool.METALLIC_A ) )
							{
								warning.append( " <span title=\"Airship\">A</span>" );
							}
						}
						if ( !InventoryManager.hasItem( ItemPool.ND ) )
						{
							if ( !InventoryManager.hasItem( ItemPool.LOWERCASE_N ) )
							{
								if ( !InventoryManager.hasItem( ItemPool.NG ) )
								{
									warning.append( " <span title=\"Orc Chasm\">N</span>" );
								}
								else
								{
									warning.append( " N (untinker your NG)" );
								}
							}
							if ( !InventoryManager.hasItem( ItemPool.HEAVY_D ) )
							{
								warning.append( " <span title=\"Castle\">D</span>" );
							}
						}
						this.sendGeneralWarning( "wand.gif", warning.toString() );
						return;
					}
				}
			}
		}

		else if ( path.startsWith( "ascend.php" ) )
		{
			if ( this.getFormField( "action" ) != null )
			{
				RequestThread.postRequest( new EquipmentRequest( SpecialOutfit.BIRTHDAY_SUIT ) );
				RequestThread.postRequest( new EquipmentRequest( EquipmentRequest.UNEQUIP, EquipmentManager.FAMILIAR ) );
			}
		}

		else if ( path.startsWith( "arcade.php" ) &&
			this.getFormField( "confirm" ) == null )
		{
			String action = this.getFormField( "action" );
			String game = this.getFormField( "whichgame" );
			int power = EquipmentDatabase.getPower(
				EquipmentManager.getEquipment( EquipmentManager.WEAPON ).getItemId() );
			if ( action != null && action.equals( "game" ) &&
				game != null && game.equals( "2" ) &&
				(power <= 50 || power >= 150 ||
					EquipmentManager.getEquipment( EquipmentManager.HAT ) == EquipmentRequest.UNEQUIP ||
					EquipmentManager.getEquipment( EquipmentManager.PANTS ) == EquipmentRequest.UNEQUIP) )
			{
				this.sendGeneralWarning( "ggtoken.gif", "You might not be properly equipped to play this game.<br>Click the token if you'd like to continue anyway." );
				return;
			}
		}

		else if ( path.startsWith( "choice.php" ) &
			  this.getFormField( "confirm" ) == null &&
			  // *** This can't work as coded: if you are in a
			  // choice, you must choose an option before doing
			  // anything else - like setting MCD.
			  false )
		{
			String choice = this.getFormField( "whichchoice" );
			String option = this.getFormField( "option" );

			// The Baron has different rewards depending on the MCD
			if ( choice != null && choice.equals( "511" ) &&
			     option != null && option.equals( "1" ) &&
			     KoLCharacter.mcdAvailable() )
			{
				this.sendBossWarning( "Baron von Ratsworth", "ratsworth.gif", 2, "moneyclip.gif", 9, "tophat.gif" );
				return;
			}
		}

		// If it gets this far, it's a normal file.  Go ahead and
		// process it accordingly.

		super.run();

		if ( this.responseCode == 302 )
		{
			this.pseudoResponse( "HTTP/1.1 302 Found", this.redirectLocation );
		}
		else if ( this.responseCode != 200 )
		{
			this.sendNotFound();
		}
	}

	private void waitForRecoveryToComplete()
	{
		while ( RecoveryManager.isRecoveryActive() )
		{
			this.pauser.pause( 200 );
		}
	}

	private static boolean showWikiLink( final String item )
	{
		if ( !Preferences.getBoolean( "relayAddsWikiLinks" ) )
			return false;

		switch ( ItemDatabase.getItemIdFromDescription( item ) )
		{
		case 2271: // dusty bottle of Merlot
		case 2272: // dusty bottle of Port
		case 2273: // dusty bottle of Pinot Noir
		case 2274: // dusty bottle of Zinfandel
		case 2275: // dusty bottle of Marsala
		case 2276: // dusty bottle of Muscat
			return false;
		}
		return true;
	}
}
