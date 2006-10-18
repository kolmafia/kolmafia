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
import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintStream;

import java.util.List;
import java.util.Date;
import java.util.ArrayList;

import java.net.URLEncoder;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import net.java.dev.spellcast.utilities.DataUtilities;

public class LocalRelayRequest extends KoLRequest
{
	private static final Pattern MENU1_PATTERN = Pattern.compile( "<select name=\"loc\".*?</select>", Pattern.DOTALL );
	private static final Pattern MENU2_PATTERN = Pattern.compile( "<select name=location.*?</select>", Pattern.DOTALL );
	private static final Pattern IMAGE_PATTERN = Pattern.compile( "<img src=\"([^\"]*?)\"" );
	private static final Pattern WHITESPACE_PATTERN = Pattern.compile( "['\\s-]" );

	private static final Pattern SEARCHITEM_PATTERN = Pattern.compile( "searchitem=(\\d+)&searchprice=(\\d+)" );
	private static final Pattern STORE_PATTERN = Pattern.compile( "<tr><td><input name=whichitem type=radio value=(\\d+).*?</tr>", Pattern.DOTALL );

	private static boolean isRunningCommand = false;

	private static String lastUsername = "";
	private static LimitedSizeChatBuffer chatLogger = new LimitedSizeChatBuffer( true );

	protected List headers = new ArrayList();
	protected byte [] rawByteBuffer = null;
	protected String contentType = null;

	public LocalRelayRequest( String formURLString )
	{
		super( formURLString );

		if ( this.formURLString.endsWith( ".css" ) )
			this.contentType = "text/css";
		else if ( this.formURLString.endsWith( ".js" ) )
			this.contentType = "text/javascript";
		else if ( this.formURLString.endsWith( ".gif" ) )
			this.contentType = "image/gif";
		else if ( this.formURLString.endsWith( ".png" ) )
			this.contentType = "image/png";
		else if ( this.formURLString.endsWith( ".jpg" ) || this.formURLString.endsWith( ".jpeg" ) )
			this.contentType = "image/jpeg";
		else if ( this.formURLString.endsWith( ".ico" ) )
			this.contentType = "image/x-icon";
		else
			this.contentType = "text/html";
	}

	public String getFullResponse()
	{	return responseText;
	}

	private static final boolean isJunkItem( int itemID, int price, int searchType, boolean ignoreExpensiveItems, boolean ignoreMinpricedItems, boolean ignoreUnrelatedItems )
	{
		boolean shouldIgnore = false;

		// Before you do any other searching, check to see if
		// the item is relevant to what you're searching for.

		if ( ignoreUnrelatedItems )
		{
			int useType = TradeableItemDatabase.getConsumptionType( itemID );
			switch ( searchType )
			{

			// You can't really do added filtering on an item that
			// isn't usable in any way, so leave it.

			case ConsumeItemRequest.NO_CONSUME:
				break;

			// If the person is searching for a familiar or an item
			// related to familiars, then go ahead and filter out
			// everything else.

			case ConsumeItemRequest.GROW_FAMILIAR:
			case ConsumeItemRequest.EQUIP_FAMILIAR:
				shouldIgnore = true;
				break;

			// If searching for a consumable item, then filter out
			// any items which are not consumable.

			case ConsumeItemRequest.CONSUME_EAT:
			case ConsumeItemRequest.CONSUME_DRINK:
			case ConsumeItemRequest.CONSUME_USE:
			case ConsumeItemRequest.CONSUME_MULTIPLE:
			case ConsumeItemRequest.CONSUME_RESTORE:

				switch ( useType )
				{
				case ConsumeItemRequest.CONSUME_EAT:
				case ConsumeItemRequest.CONSUME_DRINK:
				case ConsumeItemRequest.CONSUME_USE:
				case ConsumeItemRequest.CONSUME_MULTIPLE:
				case ConsumeItemRequest.CONSUME_RESTORE:
					break;
				default:
					shouldIgnore = true;
				}

			// Searching for equipment means filtering out anything
			// which is not equipment.

			case ConsumeItemRequest.EQUIP_ACCESSORY:
			case ConsumeItemRequest.EQUIP_HAT:
			case ConsumeItemRequest.EQUIP_PANTS:
			case ConsumeItemRequest.EQUIP_SHIRT:
			case ConsumeItemRequest.EQUIP_WEAPON:
			case ConsumeItemRequest.EQUIP_OFFHAND:

				switch ( useType )
				{
				case ConsumeItemRequest.EQUIP_ACCESSORY:
				case ConsumeItemRequest.EQUIP_HAT:
				case ConsumeItemRequest.EQUIP_PANTS:
				case ConsumeItemRequest.EQUIP_SHIRT:
				case ConsumeItemRequest.EQUIP_WEAPON:
				case ConsumeItemRequest.EQUIP_OFFHAND:
					break;
				default:
					shouldIgnore = true;
				}
			}
		}

		shouldIgnore |= ignoreExpensiveItems && price > KoLCharacter.getAvailableMeat();
		shouldIgnore |= NPCStoreDatabase.contains( TradeableItemDatabase.getItemName( itemID ) );
		shouldIgnore |= ignoreMinpricedItems && price <= TradeableItemDatabase.getPriceByID( itemID ) * 2;
		shouldIgnore |= ignoreMinpricedItems && price == 100;

		return shouldIgnore;
	}

	protected void processResponse()
	{
		super.processResponse();

		if ( formURLString.startsWith( "http" ) )
			return;

		StringBuffer responseBuffer = new StringBuffer( responseText );

		// If this is a store, you can opt to remove all the min-priced items from view
		// along with all the items which are priced above affordable levels.

		if ( formURLString.indexOf( "mallstore.php" ) != -1 )
		{
			int searchItemID = -1;
			int searchPrice = -1;
			int searchType = ConsumeItemRequest.NO_CONSUME;

			Matcher itemMatcher = SEARCHITEM_PATTERN.matcher( getURLString() );
			if ( itemMatcher.find() )
			{
				searchItemID = StaticEntity.parseInt( itemMatcher.group(1) );
				searchPrice = StaticEntity.parseInt( itemMatcher.group(2) );
				searchType = TradeableItemDatabase.getConsumptionType( searchItemID );
			}

			itemMatcher = STORE_PATTERN.matcher( responseText );

			boolean ignoreExpensiveItems = StaticEntity.getBooleanProperty( "relayRemovesExpensiveItems" );
			boolean ignoreMinpricedItems = StaticEntity.getBooleanProperty( "relayRemovesMinpricedItems" );
			boolean ignoreUnrelatedItems = StaticEntity.getBooleanProperty( "relayRemovesUnrelatedItems" );

			while ( itemMatcher.find() )
			{
				String itemData = itemMatcher.group(1);

				int itemID = StaticEntity.parseInt( itemData.substring( 0, itemData.length() - 9 ) );
				int price = StaticEntity.parseInt( itemData.substring( itemData.length() - 9 ) );

				if ( itemID != searchItemID && isJunkItem( itemID, price, searchType, ignoreExpensiveItems, ignoreMinpricedItems, ignoreUnrelatedItems ) )
					StaticEntity.singleStringDelete( responseBuffer, itemMatcher.group() );
			}

			// Also make sure the item that the person selected when coming into the
			// store is pre-selected.

			if ( searchItemID != -1 )
			{
				String searchString = MallPurchaseRequest.getStoreString( searchItemID, searchPrice );
				StaticEntity.singleStringReplace( responseBuffer, "value=" + searchString, "checked value=" + searchString );
			}
		}

		if ( formURLString.indexOf( "compactmenu.php" ) != -1 )
		{
			// Mafiatize the function menu

			StringBuffer functionMenu = new StringBuffer();
			functionMenu.append( "<select name=\"loc\" onChange=\"goloc();\">" );
			functionMenu.append( "<option value=\"nothing\">- Select -</option>" );

			for ( int i = 0; i < FUNCTION_MENU.length; ++i )
			{
				functionMenu.append( "<option value=\"" );
				functionMenu.append( FUNCTION_MENU[i][1] );
				functionMenu.append( "\">" );
				functionMenu.append( FUNCTION_MENU[i][0] );
				functionMenu.append( "</option>" );
			}

			functionMenu.append( "<option value=\"donatepopup.php?pid=" );
			functionMenu.append( KoLCharacter.getUserID() );
			functionMenu.append( "\">Donate</option>" );
			functionMenu.append( "</select>" );

			Matcher menuMatcher = MENU1_PATTERN.matcher( responseText );
			if ( menuMatcher.find() )
				StaticEntity.singleStringReplace( responseBuffer, menuMatcher.group(), functionMenu.toString() );

			// Mafiatize the goto menu

			StringBuffer gotoMenu = new StringBuffer();
			gotoMenu.append( "<select name=location onChange='move();'>" );

			gotoMenu.append( "<option value=\"nothing\">- Select -</option>" );
			for ( int i = 0; i < GOTO_MENU.length; ++i )
			{
				gotoMenu.append( "<option value=\"" );
				gotoMenu.append( GOTO_MENU[i][1] );
				gotoMenu.append( "\">" );
				gotoMenu.append( GOTO_MENU[i][0] );
				gotoMenu.append( "</option>" );
			}

			String [] bookmarkData = StaticEntity.getProperty( "browserBookmarks" ).split( "\\|" );

			if ( bookmarkData.length > 1 )
			{
				gotoMenu.append( "<option value=\"nothing\"> </option>" );
				gotoMenu.append( "<option value=\"nothing\">- Select -</option>" );

				for ( int i = 0; i < bookmarkData.length; i += 3 )
				{
					gotoMenu.append( "<option value=\"" );
					gotoMenu.append( bookmarkData[i+1] );
					gotoMenu.append( "\">" );
					gotoMenu.append( bookmarkData[i] );
					gotoMenu.append( "</option>" );
				}
			}

			gotoMenu.append( "</select>" );

			menuMatcher = MENU2_PATTERN.matcher( responseText );
			if ( menuMatcher.find() )
				StaticEntity.singleStringReplace( responseBuffer, menuMatcher.group(), gotoMenu.toString() );

			// Now kill off the weird focusing problems inherent in
			// the Javascript.

			StaticEntity.singleStringReplace( responseBuffer, "selectedIndex=0", "selectedIndex=0; parent.mainpane.focus();" );
		}

		// Fix chat javascript problems with relay system

		if ( formURLString.indexOf( "lchat.php" ) != -1 )
		{
			StaticEntity.globalStringReplace( responseBuffer, "cycles++", "cycles = 0" );
			StaticEntity.globalStringReplace( responseBuffer, "window.location.hostname", "\"127.0.0.1:" + LocalRelayServer.getPort() + "\"" );

			int headIndex = responseBuffer.indexOf( "</head>" );
			if ( headIndex != -1 )
				responseBuffer.insert( headIndex, "<script language=\"Javascript\">base = \"http://127.0.0.1:" +  LocalRelayServer.getPort() + "\";</script>" );

			int onLoadIndex = responseBuffer.indexOf( "onLoad='" );
			if ( onLoadIndex != -1 )
				responseBuffer.insert( onLoadIndex + 8, "setInterval( getNewMessages, 8000 ); " );

			// This is a hack to fix KoL chat, as it is handled
			// in Opera.  No guarantees it works, though.

			StaticEntity.singleStringReplace( responseBuffer, "http.onreadystatechange", "executed = false; http.onreadystatechange" );
			StaticEntity.singleStringReplace( responseBuffer, "readyState==4) {", "readyState==4 && !executed) { executed = true;" );
		}

		// Fix KoLmafia getting outdated by events happening
		// in the browser by using the sidepane.

		else if ( formURLString.indexOf( "charpane.php" ) != -1 )
		{
			if ( !isRunningCommand )
				CharpaneRequest.processCharacterPane( responseText );
		}

		// Fix it a little more by making sure that familiar
		// changes and equipment changes are remembered.

		else
			StaticEntity.externalUpdate( getURLString(), responseText );

		// Allow a way to get from KoL back to the gCLI
		// using the chat launcher.

		if ( formURLString.indexOf( "chatlaunch" ) != -1 )
		{
			if ( StaticEntity.getBooleanProperty( "relayAddsGraphicalCLI" ) )
			{
				int linkIndex = responseBuffer.indexOf( "<a href" );
				if ( linkIndex != -1 )
					responseBuffer.insert( linkIndex, "<a href=\"KoLmafia/cli.html\"><b>KoLmafia gCLI</b></a></center><p>Type KoLmafia scripting commands in your browser!</p><center>" );
			}

			if ( StaticEntity.getBooleanProperty( "relayAddsKoLSimulator" ) )
			{
				int linkIndex = responseBuffer.indexOf( "<a href" );
				if ( linkIndex != -1 )
					responseBuffer.insert( linkIndex, "<a href=\"KoLmafia/simulator/index.html\"><b>KoL Simulator</b></a></center><p>See what might happen before it happens!</p><center>" );
			}
		}

		if ( StaticEntity.getBooleanProperty( "relayAddsQuickScripts" ) && formURLString.indexOf( "menu" ) != -1 )
		{
			try
			{
				StringBuffer selectBuffer = new StringBuffer();
				selectBuffer.append( "<td>&nbsp;&nbsp;&nbsp;&nbsp;</td><td><form name=\"gcli\">" );
				selectBuffer.append( "<select id=\"scriptbar\">" );

				String [] scriptList = StaticEntity.getProperty( "scriptList" ).split( " \\| " );
				for ( int i = 0; i < scriptList.length; ++i )
				{
					selectBuffer.append( "<option value=\"" );
					selectBuffer.append( URLEncoder.encode( scriptList[i], "UTF-8" ) );
					selectBuffer.append( "\">" );
					selectBuffer.append( i + 1 );
					selectBuffer.append( ": " );
					selectBuffer.append( scriptList[i] );
					selectBuffer.append( "</option>" );
				}

				selectBuffer.append( "</select></td><td>&nbsp;</td><td>" );
				selectBuffer.append( "<input type=\"button\" class=\"button\" value=\"exec\" onClick=\"" );

				selectBuffer.append( "var script = document.getElementById( 'scriptbar' ).value; " );
				selectBuffer.append( "parent.charpane.location = '/KoLmafia/sideCommand?cmd=' + script; void(0);" );
				selectBuffer.append( "\">" );
				selectBuffer.append( "</form></td>" );

				int lastRowIndex = responseBuffer.lastIndexOf( "</tr>" );
				if ( lastRowIndex != -1 )
					responseBuffer.insert( lastRowIndex, selectBuffer.toString() );
			}
			catch ( Exception e )
			{
				// Something bad happened, let's ignore it for now, because
				// no script bar isn't the end of the world.
			}
		}

		try
		{
			RequestEditorKit.getFeatureRichHTML( formURLString.toString(), responseBuffer );
		}
		catch ( Exception e )
		{
			StaticEntity.printStackTrace( e );
		}

		// Download and link to any Players of Loathing
		// picture pages locally.

		StaticEntity.globalStringReplace( responseBuffer, "http://pics.communityofloathing.com/albums", "/images" );

		// Remove the default frame busting script so that
		// we can detach user interface elements.

		StaticEntity.singleStringReplace( responseBuffer, "frames.length == 0", "frames.length == -1" );

		// If the person is currently caching relay images,
		// then it would be most beneficial to use local
		// file access.

		if ( StaticEntity.getBooleanProperty( "cacheRelayImages" ) )
			StaticEntity.globalStringReplace( responseBuffer, "http://images.kingdomofloathing.com", "/images" );

		// Otherwise, use the standard image server address
		// just in case there is a DNS problem.

		else
			StaticEntity.globalStringReplace( responseBuffer, "images.kingdomofloathing.com", IMAGE_SERVER );

		responseText = responseBuffer.toString();
	}

	public String getHeader( int index )
	{
		if ( headers.isEmpty() )
		{
			// This request was relayed to the server. Respond with those headers.
			headers.add( formConnection.getHeaderField( 0 ) );
			for ( int i = 1; formConnection.getHeaderFieldKey( i ) != null; ++i )
			{
				if ( formConnection.getHeaderFieldKey( i ).equals( "Content-Length" ) )
				{
					if ( this.responseText != null )
						headers.add( "Content-Length: " + this.responseText.length() );
				}
				else if ( !formConnection.getHeaderFieldKey( i ).equals( "Transfer-Encoding" ) )
					headers.add( formConnection.getHeaderFieldKey( i ) + ": " + formConnection.getHeaderField( i ) );
			}
		}

		return index >= headers.size() ? null : (String) headers.get( index );
	}

	protected void pseudoResponse( String status, String responseText )
	{
		this.responseText = StaticEntity.globalStringReplace( responseText, "<!--MAFIA_HOST_PORT-->", "127.0.0.1:" + LocalRelayServer.getPort() );
		if ( responseText.length() == 0 )
			this.responseText = " ";

		headers.clear();
		headers.add( status );
		headers.add( "Date: " + ( new Date() ) );
		headers.add( "Server: " + VERSION_NAME );

		if ( status.indexOf( "302" ) != -1 )
		{
			headers.add( "Location: " + responseText );
			this.responseCode = 302;
			this.responseText = "";
		}
		else if ( status.indexOf( "200" ) != -1 )
		{
			headers.add( "Content-Length: " + (this.rawByteBuffer == null ? this.responseText.length() : this.rawByteBuffer.length) );
			this.responseCode = 200;
		}
	}

	private StringBuffer readContents( BufferedReader reader, String filename ) throws IOException
	{
		String line = null;
		StringBuffer contentBuffer = new StringBuffer();

		if ( reader == null )
			return contentBuffer;

		while ( (line = reader.readLine()) != null )
		{
			if ( !filename.endsWith( ".js" ) && line.indexOf( "<img" ) != -1 )
			{
				String directory = (new File( "html/" + filename )).getParent();
				if ( !directory.endsWith( "/" ) )
					directory += "/";

				StringBuffer lineBuffer = new StringBuffer();

				Matcher imageMatcher = IMAGE_PATTERN.matcher( line );
				while ( imageMatcher.find() )
				{
					String location = imageMatcher.group(1);
					if ( location.indexOf( "http://" ) == -1 )
					{
						imageMatcher.appendReplacement( lineBuffer,
							"<img src=\"" + (new File( directory + location )).toURL() + "\"" );
					}
					else
					{
						imageMatcher.appendReplacement( lineBuffer, "$0" );
					}
				}

				imageMatcher.appendTail( lineBuffer );
				line = lineBuffer.toString();
			}

			contentBuffer.append( line );
			contentBuffer.append( LINE_BREAK );
		}

		reader.close();
		return contentBuffer;
	}

	private void sendLocalImage( String filename ) throws IOException
	{
		// The word "KoLmafia" prefixes all of the local
		// images.  Therefore, make sure it's removed.

		BufferedInputStream in = new BufferedInputStream( RequestEditorKit.downloadImage(
			"http://images.kingdomofloathing.com" + filename.substring(6) ).openConnection().getInputStream() );

		ByteArrayOutputStream outbytes = new ByteArrayOutputStream( 4096 );
		byte [] buffer = new byte[4096];

		int offset;
		while ((offset = in.read(buffer)) > 0)
			outbytes.write(buffer, 0, offset);

		in.close();
		outbytes.flush();

		this.rawByteBuffer = outbytes.toByteArray();
		pseudoResponse( "HTTP/1.1 200 OK", "" );
	}

	private void sendSharedFile( String filename ) throws IOException
	{
		boolean isServerRequest = !filename.startsWith( "KoLmafia" );
		if ( !isServerRequest )
			filename = filename.substring( 9 );

		int index = filename.indexOf( "/" );
		boolean writePseudoResponse = !isServerRequest;

		BufferedReader reader = null;
		StringBuffer replyBuffer = new StringBuffer();
		StringBuffer scriptBuffer = new StringBuffer();

		String name = filename.substring( index + 1 );
		String directory = index == -1 ? "html" : "html/" + filename.substring( 0, index );

		reader = DataUtilities.getReader( directory, name );
		if ( reader == null && filename.startsWith( "simulator" ) )
		{
			downloadSimulatorFile( name );
			reader = DataUtilities.getReader( directory, name );
		}

		if ( reader != null )
		{
			// Now that you know the reader exists, read the
			// contents of the reader.

			replyBuffer = readContents( reader, filename );
			writePseudoResponse = true;
		}
		else
		{
			if ( isServerRequest )
			{
				// If there's no override file, go ahead and
				// request the page from the server normally.

				super.run();

				if ( responseCode != 200 )
					return;
			}
			else
			{
				sendNotFound();
				return;
			}
		}

		// Add brand new Javascript to every single page.  Check
		// to see if a reader exists for the file.

		if ( !name.endsWith( ".js" ) )
		{
			reader = DataUtilities.getReader( directory, name.substring( 0, name.lastIndexOf( "." ) ) + ".js" );
			if ( reader != null )
			{
				// Initialize the reply buffer with the contents of
				// the full response if you're a standard KoL request.

				if ( isServerRequest )
				{
					replyBuffer.append( responseText );
					writePseudoResponse = true;
				}

				scriptBuffer = readContents( reader, filename.substring( 0, filename.lastIndexOf( "." ) ) + ".js" );
				if ( filename.equals( "simulator/index.html" ) )
					handleSimulatorIndex( replyBuffer, scriptBuffer );

				int terminalIndex = replyBuffer.lastIndexOf( "</html>" );
				if ( terminalIndex == -1 )
				{
					replyBuffer.append( scriptBuffer.toString() );
					replyBuffer.append( "</html>" );
				}
				else
				{
					scriptBuffer.insert( 0, LINE_BREAK );
					scriptBuffer.insert( 0, LINE_BREAK );
					scriptBuffer.insert( 0, "<script language=\"Javascript\">" );
					scriptBuffer.append( LINE_BREAK );
					scriptBuffer.append( LINE_BREAK );
					scriptBuffer.append( "</script>" );
					replyBuffer.insert( terminalIndex, scriptBuffer.toString() );
				}
			}
		}

		if ( writePseudoResponse )
		{
			// Make sure to print the reply buffer to the
			// response buffer for the local relay server.

			if ( isChatRequest )
				StaticEntity.globalStringReplace( replyBuffer, "<br>", "</font><br>" );

			pseudoResponse( "HTTP/1.1 200 OK", replyBuffer.toString() );
		}
	}

	private void handleSimulatorIndex( StringBuffer replyBuffer, StringBuffer scriptBuffer ) throws IOException
	{
		// This is the simple Javascript which can be added
		// arbitrarily to the end without having to modify
		// the underlying HTML.

		int classIndex = -1;
		for ( int i = 0; i < KoLmafiaASH.CLASSES.length; ++i )
			if ( KoLmafiaASH.CLASSES[i].equalsIgnoreCase( KoLCharacter.getClassType() ) )
				classIndex = i;

		// Basic additions of player state info

		StaticEntity.globalStringReplace( scriptBuffer, "/*classIndex*/", classIndex );
		StaticEntity.globalStringReplace( scriptBuffer, "/*baseMuscle*/", KoLCharacter.getBaseMuscle() );
		StaticEntity.globalStringReplace( scriptBuffer, "/*baseMysticality*/", KoLCharacter.getBaseMysticality() );
		StaticEntity.globalStringReplace( scriptBuffer, "/*baseMoxie*/", KoLCharacter.getBaseMoxie() );
		StaticEntity.globalStringReplace( scriptBuffer, "/*mindControl*/", KoLCharacter.getMindControlLevel() );

		// Change the player's familiar to the current
		// familiar.  Input the weight and change the
		// familiar equipment.

		StaticEntity.globalStringReplace( scriptBuffer, "/*familiar*/",  KoLCharacter.getFamiliar().getRace() );
		StaticEntity.globalStringReplace( scriptBuffer, "/*familiarWeight*/", KoLCharacter.getFamiliar().getWeight() );

		String familiarEquipment = KoLCharacter.getEquipment( KoLCharacter.FAMILIAR ).getName();
		if ( FamiliarData.itemWeightModifier( TradeableItemDatabase.getItemID( familiarEquipment ) ) == 5 )
			StaticEntity.globalStringReplace( scriptBuffer, "/*familiarEquip*/", "familiar-specific +5 lbs." );
		else
			StaticEntity.globalStringReplace( scriptBuffer, "/*familiarEquip*/", familiarEquipment );

		// Change the player's equipment

		StaticEntity.globalStringReplace( scriptBuffer, "/*hat*/", KoLCharacter.getEquipment( KoLCharacter.HAT ).getName() );
		StaticEntity.globalStringReplace( scriptBuffer, "/*weapon*/", KoLCharacter.getEquipment( KoLCharacter.WEAPON ).getName() );
		StaticEntity.globalStringReplace( scriptBuffer, "/*offhand*/", KoLCharacter.getEquipment( KoLCharacter.OFFHAND ).getName() );
		StaticEntity.globalStringReplace( scriptBuffer, "/*shirt*/", KoLCharacter.getEquipment( KoLCharacter.SHIRT ).getName() );
		StaticEntity.globalStringReplace( scriptBuffer, "/*pants*/", KoLCharacter.getEquipment( KoLCharacter.PANTS ).getName() );

		// Change the player's accessories

		StaticEntity.globalStringReplace( scriptBuffer, "/*accessory1*/", KoLCharacter.getEquipment( KoLCharacter.ACCESSORY1 ).getName() );
		StaticEntity.globalStringReplace( scriptBuffer, "/*accessory2*/", KoLCharacter.getEquipment( KoLCharacter.ACCESSORY2 ).getName() );
		StaticEntity.globalStringReplace( scriptBuffer, "/*accessory3*/", KoLCharacter.getEquipment( KoLCharacter.ACCESSORY3 ).getName() );

		// Load up the player's current skillset to figure
		// out what passive skills are available.

		UseSkillRequest [] skills = new UseSkillRequest[ availableSkills.size() ];
		availableSkills.toArray( skills );

		StringBuffer passiveSkills = new StringBuffer();
		for ( int i = 0; i < skills.length; ++i )
		{
			int skillID = skills[i].getSkillID();
			if ( !( ClassSkillsDatabase.getSkillType( skillID ) == ClassSkillsDatabase.PASSIVE && !(skillID < 10 || (skillID > 14 && skillID < 1000)) ) )
				continue;

			passiveSkills.append( "\t" );
			passiveSkills.append( WHITESPACE_PATTERN.matcher( skills[i].getSkillName() ).replaceAll( "" ).toLowerCase() );
			passiveSkills.append( "\t" );
		}

		StaticEntity.globalStringReplace( scriptBuffer, "/*passiveSkills*/", passiveSkills.toString() );

		// Also load up the player's current active effects
		// and fill them into the buffs area.

		AdventureResult [] effects = new AdventureResult[ activeEffects.size() ];
		activeEffects.toArray( effects );

		String activeEffects = "";
		for ( int i = 0; i < effects.length; ++i )
			activeEffects += "\t" + WHITESPACE_PATTERN.matcher( UneffectRequest.effectToSkill( effects[i].getName() ) ).replaceAll( "" ).toLowerCase() + "\t";

		StaticEntity.globalStringReplace( scriptBuffer, "/*activeEffects*/", activeEffects );

		if ( inventory.contains( UseSkillRequest.ROCKNROLL_LEGEND ) )
			StaticEntity.globalStringReplace( scriptBuffer, "/*rockAndRoll*/", "true" );
		else
			StaticEntity.globalStringReplace( scriptBuffer, "/*rockAndRoll*/", "false" );

		StaticEntity.globalStringReplace( scriptBuffer, "/*moonPhase*/", (int) ((MoonPhaseDatabase.getGrimacePhase()-1) * 2
			+ Math.round( (MoonPhaseDatabase.getRonaldPhase()-1) / 2.0f - Math.floor( (MoonPhaseDatabase.getRonaldPhase()-1) / 2.0f ) )) );

		replyBuffer.insert( replyBuffer.indexOf( ";GoCalc()" ), ";loadKoLmafiaData()" );
	}

	protected void submitCommand()
	{
		runCommand( getFormField( "cmd" ) );
		pseudoResponse( "HTTP/1.1 200 OK", LocalRelayServer.getNewStatusMessages() );
	}

	protected void executeCommand()
	{
		runCommand( getFormField( "cmd" ) );
		pseudoResponse( "HTTP/1.1 200 OK", LocalRelayServer.getNewStatusMessages() );
	}

	protected void sideCommand()
	{
		KoLmafia.forceContinue();
		if ( commandQueue.isEmpty() )
		{
			commandQueue.add( getFormField( "cmd" ) );
			(new CommandRunnable()).run();
		}
		else
		{
			commandQueue.add( getFormField( "cmd" ) );
			while ( !commandQueue.isEmpty() )
				delay( 500 );
		}

		pseudoResponse( "HTTP/1.1 302 Found", "/charpane.php" );
	}

	private void runCommand( String command )
	{
		if ( command.equals( "abort" ) )
		{
			KoLmafia.declareWorldPeace();
			commandQueue.clear();
			return;
		}

		boolean shouldStartThread = commandQueue.isEmpty();
		commandQueue.add( command );

		if ( shouldStartThread )
			(new CommandRunnable()).run();
	}

	protected void sendNotFound()
	{
		pseudoResponse( "HTTP/1.1 404 Not Found", "" );
		this.responseCode = 404;
	}

	private class CommandRunnable implements Runnable
	{
		public void run()
		{
			isRunningCommand = true;

			while ( !commandQueue.isEmpty() )
			{
				String command = (String) commandQueue.get(0);
				if ( command == null )
					return;

				try
				{
					KoLmafia.forceContinue();
					DEFAULT_SHELL.executeLine( command );
				}
				catch ( Exception e )
				{
					// This is usually a result of a user error.
					// Therefore, fall through.
				}

				try
				{
					if ( !commandQueue.isEmpty() )
						commandQueue.remove(0);
				}
				catch ( Exception e )
				{
					// This is only due to a race condition
					// and should not happen.
				}
			}

			KoLmafia.enableDisplay();
			isRunningCommand = false;
		}

	}

	public void run()
	{
		// Abort request if the person is attempting to stasis
		// mine, even if it's done manually.

		if ( formURLString.indexOf( "aventure.php" ) != -1 && KoLCharacter.getFamiliar().isThiefFamiliar() && KoLCharacter.canInteract() )
		{
			String location = getFormField( "snarfblat" );
			if ( location == null )
				location = getFormField( "adv" );

			if ( location != null && location.equals( "101" ) )
			{
				pseudoResponse( "HTTP/1.1 200 OK", "<html><body><h1>Please reconsider your meat farming strategy.</h1></body></html>" );
				return;
			}
		}

		// If there is an attempt to view the error page, or if
		// there is an attempt to view the robots file, neither
		// are available on KoL, so return.

		if ( formURLString.endsWith( "missing.gif" ) || formURLString.endsWith( "robots.txt" ) )
		{
			sendNotFound();
			return;
		}

		// If the person is visiting the sorceress and they forgot
		// to equip the Wand, remind them.

		if ( formURLString.indexOf( "lair6.php" ) != -1 && getFormField( "place" ) != null && getFormField( "place" ).equals( "5" ) )
		{
			if ( !KoLCharacter.hasEquipped( SorceressLair.NAGAMAR ) )
			{
				pseudoResponse( "HTTP/1.1 200 OK", "<html><body><h1>Did you forget to equip something?</h1></body></html>" );
				return;
			}
		}

		// If you are in chat, and the person submitted a command
		// via chat, check to see if it's a CLI command.

		String graf = getFormField( "graf" );

		if ( graf != null && graf.startsWith( "/run" ) )
		{
			pseudoResponse( "HTTP/1.1 200 OK", "<br/><font color=olive> &gt; " + graf.substring( 5 ) + "</font><br/><br/>" );
			runCommand( graf.substring( 5 ) );

			return;
		}

		// None of the above checks wound up happening.  So, do some
		// special handling, catching any exceptions that happen to
		// popup along the way.

		try
		{
			if ( formURLString.endsWith( "favicon.ico" ) )
			{
				StaticEntity.loadLibrary( "KoLmelion.ico" );
				sendLocalImage( "KoLmelion.ico" );
			}
			else if ( formURLString.endsWith( "submitCommand" ) )
				submitCommand();
			else if ( formURLString.endsWith( "executeCommand" ) )
				executeCommand();
			else if ( formURLString.endsWith( "sideCommand" ) )
				sideCommand();
			else if ( formURLString.endsWith( "getNewMessages" ) )
				pseudoResponse( "HTTP/1.1 200 OK", LocalRelayServer.getNewStatusMessages() );
			else if ( formURLString.indexOf( "images/playerpics/" ) != -1 )
			{
				RequestEditorKit.downloadImage( "http://pics.communityofloathing.com/albums/" +
					formURLString.substring( formURLString.indexOf( "playerpics" ) ) );

				sendLocalImage( formURLString );
			}
			else if ( formURLString.indexOf( "images/" ) != -1 )
			{
				sendLocalImage( formURLString );
			}
			else
			{
				sendSharedFile( formURLString );

				if ( isChatRequest )
				{
					if ( !KoLCharacter.getUsername().equals( lastUsername ) )
						chatLogger.setActiveLogFile( KoLMessenger.getChatLogName( "[ALL]" ) );

					if ( responseText.length() > 0 && responseText.indexOf( "<img" ) == -1 )
						chatLogger.append( StaticEntity.globalStringReplace( responseText, "<br>", "</font><br>" ) );
				}
			}
		}
		catch ( Exception e )
		{
			// This should not happen.  However, if it does,
			// that means the browser is asking for a bad URL.
			// Go ahead and return a 404 in this case.

			e.printStackTrace( KoLmafia.getDebugStream() );
			sendNotFound();
		}
	}

	private void downloadSimulatorFile( String filename )
	{
		try
		{
			BufferedReader reader = DataUtilities.getReader( "http://sol.kolmafia.us/" + filename );

			String line;
			StringBuffer contents = new StringBuffer();

			while ( (line = reader.readLine()) != null )
			{
				contents.append( line );
				contents.append( LINE_BREAK );
			}

			File directory = new File( "html/simulator/" );
			directory.mkdirs();

			StaticEntity.globalStringReplace( contents, "images/", "http://sol.kolmafia.us/images/" );

			PrintStream writer = KoLmafia.openStream( "html/simulator/" + filename, NullStream.INSTANCE, true );
			writer.println( contents.toString() );
			writer.close();
		}
		catch ( Exception e )
		{
			StaticEntity.printStackTrace( e, "Failed to create cached simulator file." );
		}
	}
}