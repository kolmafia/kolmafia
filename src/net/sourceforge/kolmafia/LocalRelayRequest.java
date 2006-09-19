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

	protected List headers = new ArrayList();
	protected byte [] rawByteBuffer = null;

	public LocalRelayRequest( KoLmafia client, String formURLString )
	{	super( client, formURLString );
	}

	public String getFullResponse()
	{	return fullResponse;
	}

	private static final boolean isJunkItem( int itemID, int price, boolean ignoreExpensiveItems, boolean ignoreMinpricedItems )
	{
		boolean shouldIgnore = false;

		shouldIgnore |= ignoreExpensiveItems && price > KoLCharacter.getAvailableMeat();
		shouldIgnore |= ignoreMinpricedItems && NPCStoreDatabase.contains( TradeableItemDatabase.getItemName( itemID ) );
		shouldIgnore |= ignoreMinpricedItems && price <= TradeableItemDatabase.getPriceByID( itemID ) * 2 + 1;
		shouldIgnore |= ignoreMinpricedItems && price == 100;

		return shouldIgnore;
	}

	protected void processRawResponse( String rawResponse )
	{
		super.processRawResponse( rawResponse );

		if ( formURLString.startsWith( "http" ) )
			return;

		// If this is a store, you can opt to remove all the min-priced items from view
		// along with all the items which are priced above affordable levels.

		if ( KoLCharacter.canInteract() && formURLString.indexOf( "mallstore.php" ) != -1 )
		{
			int searchItemID = -1;
			int searchPrice = -1;

			Matcher itemMatcher = SEARCHITEM_PATTERN.matcher( getURLString() );
			if ( itemMatcher.find() )
			{
				searchItemID = StaticEntity.parseInt( itemMatcher.group(1) );
				searchPrice = StaticEntity.parseInt( itemMatcher.group(2) );
			}

			itemMatcher = STORE_PATTERN.matcher( fullResponse );

			boolean ignoreExpensiveItems = StaticEntity.getBooleanProperty( "relayRemovesExpensiveItems" );
			boolean ignoreMinpricedItems = StaticEntity.getBooleanProperty( "relayRemovesMinpricedItems" );

			StringBuffer cleanBuffer = new StringBuffer( fullResponse );

			while ( itemMatcher.find() )
			{
				String itemData = itemMatcher.group(1);

				int itemID = StaticEntity.parseInt( itemData.substring( 0, itemData.length() - 9 ) );
				int price = StaticEntity.parseInt( itemData.substring( itemData.length() - 9 ) );

				if ( itemID != searchItemID && isJunkItem( itemID, price, ignoreExpensiveItems, ignoreMinpricedItems ) )
					replaceTag( cleanBuffer, itemMatcher.group(), "" );
			}

			// Also make sure the item that the person selected when coming into the
			// store is pre-selected.

			if ( searchItemID != -1 )
			{
				String searchString = MallPurchaseRequest.getStoreString( searchItemID, searchPrice );
				replaceTag( cleanBuffer, "value=" + searchString, "checked value=" + searchString );
			}

			fullResponse = cleanBuffer.toString();
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

			fullResponse = MENU1_PATTERN.matcher( fullResponse ).replaceFirst( functionMenu.toString() );

			// Mafiatize the goto menu

			StringBuffer gotoMenu = new StringBuffer();
			gotoMenu.append( "<select name=location onChange='move();'>" );

			String [] bookmarkData = StaticEntity.getProperty( "browserBookmarks" ).split( "\\|" );

			if ( bookmarkData.length > 1 )
			{
				gotoMenu.append( "<option value=\"nothing\">- Bookmarks -</option>" );

				for ( int i = 0; i < bookmarkData.length; i += 3 )
				{
					gotoMenu.append( "<option value=\"" );
					gotoMenu.append( bookmarkData[i+1] );
					gotoMenu.append( "\">" );
					gotoMenu.append( bookmarkData[i] );
					gotoMenu.append( "</option>" );
				}
			}

			gotoMenu.append( "<option value=\"nothing\">- Select -</option>" );
			for ( int i = 0; i < GOTO_MENU.length; ++i )
			{
				gotoMenu.append( "<option value=\"" );
				gotoMenu.append( GOTO_MENU[i][1] );
				gotoMenu.append( "\">" );
				gotoMenu.append( GOTO_MENU[i][0] );
				gotoMenu.append( "</option>" );
			}

			gotoMenu.append( "</select>" );
			fullResponse = MENU2_PATTERN.matcher( fullResponse ).replaceFirst( gotoMenu.toString() );
		}

		// Fix chat javascript problems with relay system

		if ( formURLString.indexOf( "lchat.php" ) != -1 )
		{
			fullResponse = StaticEntity.simpleStringReplace( fullResponse, "cycles++", "cycles = 0" );
			fullResponse = StaticEntity.simpleStringReplace( fullResponse, "window.location.hostname", "\"127.0.0.1:" + LocalRelayServer.getPort() + "\"" );

			int headIndex = fullResponse.indexOf( "</head>" );
			int onLoadIndex = fullResponse.indexOf( "onLoad='" ) + 8;

			if ( headIndex != -1 && onLoadIndex != 7 )
			{
				fullResponse = fullResponse.substring( 0, headIndex ) +
					"<script language=\"Javascript\">base = \"http://127.0.0.1:" +  LocalRelayServer.getPort() + "\";</script>" +
					fullResponse.substring( headIndex, onLoadIndex ) + "setInterval( getNewMessages, 8000 ); " + fullResponse.substring( onLoadIndex );
			}
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
			int linkTagIndex = fullResponse.indexOf( "<a href" );
			if ( linkTagIndex != -1 )
			{
				fullResponse = fullResponse.substring( 0, linkTagIndex ) +
					"<a href=\"KoLmafia/cli.html\"><b>KoLmafia gCLI</b></a></center><p>Type KoLmafia scripting commands in your browser!</p><center>" +
					fullResponse.substring( linkTagIndex );
			}
		}

		if ( StaticEntity.getBooleanProperty( "relayAddsQuickScripts" ) && formURLString.indexOf( "menu" ) != -1 )
		{
			try
			{
				StringBuffer selectBuffer = new StringBuffer();

				String [] scriptList = StaticEntity.getProperty( "scriptList" ).split( " \\| " );
				for ( int i = 0; i < scriptList.length; ++i )
				{
					if ( scriptList[i].startsWith( "restore" ) || scriptList[i].equals( "mood execute" ) )
						continue;

					if ( selectBuffer.length() > 0 )
						selectBuffer.append( "&nbsp;|&nbsp;" );

					selectBuffer.append( "<a style=\"text-decoration:none\" target=\"charpane\" href=\"/KoLmafia/sideCommand?cmd=" );
					selectBuffer.append( URLEncoder.encode( scriptList[i], "UTF-8" ) );
					selectBuffer.append( "\" title=\"" );
					selectBuffer.append( scriptList[i] );
					selectBuffer.append( "\">" );
					selectBuffer.append( i + 1 );
					selectBuffer.append( "</a>" );
				}

				int lastRowIndex = fullResponse.lastIndexOf( "</tr>" );
				if ( lastRowIndex != -1 )
				{
					fullResponse = fullResponse.substring( 0, lastRowIndex ) + "<td>&nbsp;&nbsp;</td><td align=right><font size=2>" +
						selectBuffer.toString() + "</font></td>";
				}
			}
			catch ( Exception e )
			{
				// Well, this is an odd error.  For now, ignore it
				// since it means the system can't handle UTF-8.
			}
		}

		fullResponse = RequestEditorKit.getFeatureRichHTML( formURLString.toString(), fullResponse );

		// Download and link to any Players of Loathing
		// picture pages locally.

		int playerPicsIndex = fullResponse.indexOf( "http://pics.communityofloathing.com" );
		if ( playerPicsIndex != -1 )
		{
			int albumIndex = fullResponse.indexOf( "albums", playerPicsIndex ) + 6;
			fullResponse = fullResponse.substring( 0, playerPicsIndex ) + "/images" + fullResponse.substring( albumIndex );
		}

		// Remove the default frame busting script so that
		// we can detach user interface elements.

		fullResponse = StaticEntity.simpleStringReplace( fullResponse, "frames.length == 0", "frames.length == -1" );

		// If the person is currently caching relay images,
		// then it would be most beneficial to use local
		// file access.

		if ( StaticEntity.getBooleanProperty( "cacheRelayImages" ) )
			fullResponse = StaticEntity.simpleStringReplace( fullResponse, "http://images.kingdomofloathing.com", "/images" );

		// Otherwise, use the standard image server address
		// just in case there is a DNS problem.

		else
			fullResponse = StaticEntity.simpleStringReplace( fullResponse, "images.kingdomofloathing.com", IMAGE_SERVER );
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
					if ( this.fullResponse != null )
						headers.add( "Content-Length: " + this.fullResponse.length() );
				}
				else if ( !formConnection.getHeaderFieldKey( i ).equals( "Transfer-Encoding" ) )
					headers.add( formConnection.getHeaderFieldKey( i ) + ": " + formConnection.getHeaderField( i ) );
			}
		}

		return index >= headers.size() ? null : (String) headers.get( index );
	}

	protected void pseudoResponse( String status, String fullResponse )
	{
		this.fullResponse = StaticEntity.simpleStringReplace( fullResponse, "<!--MAFIA_HOST_PORT-->", "127.0.0.1:" + LocalRelayServer.getPort() );
		if ( fullResponse.length() == 0 )
			this.fullResponse = " ";

		headers.clear();
		headers.add( status );
		headers.add( "Date: " + ( new Date() ) );
		headers.add( "Server: " + VERSION_NAME );

		if ( status.indexOf( "302" ) != -1 )
		{
			headers.add( "Location: " + fullResponse );
			this.fullResponse = "";
		}
		else
		{
			headers.add( "Content-Length: " + (this.rawByteBuffer == null ? this.fullResponse.length() : this.rawByteBuffer.length) );

			String contentType = null;

			if ( formURLString.endsWith( ".css" ) )
				contentType = "text/css; charset=UTF-8";
			else if ( formURLString.endsWith( ".js" ) )
				contentType = "text/javascript; charset=UTF-8";
			else if ( formURLString.endsWith( ".php" ) || formURLString.endsWith( ".htm" ) || formURLString.endsWith( ".html" ) )
				contentType = "text/html; charset=UTF-8";
			else if ( formURLString.endsWith( ".txt" ) )
				contentType = "text/plain; charset=UTF-8";
			else if ( formURLString.endsWith( ".gif" ) )
				contentType = "image/gif";
			else if ( formURLString.endsWith( ".png" ) )
				contentType = "image/png";
			else if ( formURLString.endsWith( ".jpg" ) || formURLString.endsWith( ".jpeg" ) )
				contentType = "image/jpeg";
			else if ( formURLString.endsWith( ".ico" ) )
				contentType = "image/x-icon";

			if ( contentType != null )
				headers.add( "Content-Type: " + contentType );
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
					replyBuffer.append( fullResponse );
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

			pseudoResponse( "HTTP/1.1 200 OK", replyBuffer.toString() );
		}
	}

	private void replaceTag( StringBuffer buffer, String tag, int replaceWith )
	{	replaceTag( buffer, tag, String.valueOf( replaceWith ) );
	}

	private void replaceTag( StringBuffer buffer, String tag, String replaceWith )
	{
		if ( replaceWith == null )
			replaceWith = "";

		// Using a regular expression, while faster, results
		// in a lot of String allocation overhead.  So, use
		// a statically-allocated StringBuffers.

		int lastIndex = buffer.indexOf( tag );
		while ( lastIndex != -1 )
		{
			buffer.replace( lastIndex, lastIndex + tag.length(), replaceWith );
			lastIndex = buffer.indexOf( tag, lastIndex + replaceWith.length() );
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

		replaceTag( scriptBuffer, "/*classIndex*/", classIndex );
		replaceTag( scriptBuffer, "/*baseMuscle*/", KoLCharacter.getBaseMuscle() );
		replaceTag( scriptBuffer, "/*baseMysticality*/", KoLCharacter.getBaseMysticality() );
		replaceTag( scriptBuffer, "/*baseMoxie*/", KoLCharacter.getBaseMoxie() );
		replaceTag( scriptBuffer, "/*mindControl*/", KoLCharacter.getMindControlLevel() );

		// Change the player's familiar to the current
		// familiar.  Input the weight and change the
		// familiar equipment.

		replaceTag( scriptBuffer, "/*familiar*/",  KoLCharacter.getFamiliar().getRace() );
		replaceTag( scriptBuffer, "/*familiarWeight*/", KoLCharacter.getFamiliar().getWeight() );

		String familiarEquipment = KoLCharacter.getEquipment( KoLCharacter.FAMILIAR ).getName();
		if ( FamiliarData.itemWeightModifier( TradeableItemDatabase.getItemID( familiarEquipment ) ) == 5 )
			replaceTag( scriptBuffer, "/*familiarEquip*/", "familiar-specific +5 lbs." );
		else
			replaceTag( scriptBuffer, "/*familiarEquip*/", familiarEquipment );

		// Change the player's equipment

		replaceTag( scriptBuffer, "/*hat*/", KoLCharacter.getEquipment( KoLCharacter.HAT ).getName() );
		replaceTag( scriptBuffer, "/*weapon*/", KoLCharacter.getEquipment( KoLCharacter.WEAPON ).getName() );
		replaceTag( scriptBuffer, "/*offhand*/", KoLCharacter.getEquipment( KoLCharacter.OFFHAND ).getName() );
		replaceTag( scriptBuffer, "/*shirt*/", KoLCharacter.getEquipment( KoLCharacter.SHIRT ).getName() );
		replaceTag( scriptBuffer, "/*pants*/", KoLCharacter.getEquipment( KoLCharacter.PANTS ).getName() );

		// Change the player's accessories

		replaceTag( scriptBuffer, "/*accessory1*/", KoLCharacter.getEquipment( KoLCharacter.ACCESSORY1 ).getName() );
		replaceTag( scriptBuffer, "/*accessory2*/", KoLCharacter.getEquipment( KoLCharacter.ACCESSORY2 ).getName() );
		replaceTag( scriptBuffer, "/*accessory3*/", KoLCharacter.getEquipment( KoLCharacter.ACCESSORY3 ).getName() );

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

		replaceTag( scriptBuffer, "/*passiveSkills*/", passiveSkills.toString() );

		// Also load up the player's current active effects
		// and fill them into the buffs area.

		AdventureResult [] effects = new AdventureResult[ activeEffects.size() ];
		activeEffects.toArray( effects );

		String activeEffects = "";
		for ( int i = 0; i < effects.length; ++i )
			activeEffects += "\t" + WHITESPACE_PATTERN.matcher( UneffectRequest.effectToSkill( effects[i].getName() ) ).replaceAll( "" ).toLowerCase() + "\t";

		replaceTag( scriptBuffer, "/*activeEffects*/", activeEffects );

		if ( inventory.contains( UseSkillRequest.ROCKNROLL_LEGEND ) )
			replaceTag( scriptBuffer, "/*rockAndRoll*/", "true" );
		else
			replaceTag( scriptBuffer, "/*rockAndRoll*/", "false" );

		replaceTag( scriptBuffer, "/*moonPhase*/", (int) ((MoonPhaseDatabase.getGrimacePhase()-1) * 2
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
		pseudoResponse( "HTTP/1.1 200 OK", "" );
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
				delay( 1000 );
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
			(new Thread( new CommandRunnable() )).start();
	}

	protected void sendNotFound()
	{	pseudoResponse( "HTTP/1.1 404 Not Found", "" );
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
		if ( formURLString.endsWith( "missing.gif" ) || formURLString.endsWith( "robots.txt" ) )
		{
			sendNotFound();
			return;
		}

		String graf = getFormField( "graf" );
		if ( graf != null && graf.startsWith( "/run" ) )
		{
			pseudoResponse( "HTTP/1.1 200 OK", "<br/><font color=olive> &gt; " + graf.substring( 5 ) + "</font><br/><br/>" );
			runCommand( graf.substring( 5 ) );

			return;
		}

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
				sendLocalImage( formURLString );
			else
				sendSharedFile( formURLString );
		}
		catch ( Exception e )
		{
			// This should not happen.  However, if it does,
			// that means the browser is asking for a bad URL.
			// Go ahead and return a 404 in this case.

			sendNotFound();
		}
	}

	private void downloadSimulatorFile( String filename )
	{
		LocalRelayRequest request = new LocalRelayRequest( client, "http://sol.kolmafia.us/" + filename );
		request.run();

		File directory = new File( "html/simulator/" );
		directory.mkdirs();

		request.fullResponse = StaticEntity.simpleStringReplace( request.fullResponse, "images/", "\"http://sol.kolmafia.us/images/" );

		try
		{
			PrintStream writer = new LogStream( "html/simulator/" + filename );
			writer.print( request.fullResponse );
			writer.close();
		}
		catch ( Exception e )
		{
			StaticEntity.printStackTrace( e, "Failed to create cached simulator file." );
		}
	}
}