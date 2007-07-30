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

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;

import java.net.URLEncoder;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.kolmafia.StaticEntity.TurnCounter;
import net.java.dev.spellcast.utilities.DataUtilities;

public class LocalRelayRequest extends PasswordHashRequest
{
	private static final Pattern EMAIL_PATTERN = Pattern.compile( "<table style='border: 1px solid black;' cellpadding=10>.*?</table>", Pattern.DOTALL );

	private static final Pattern MENU1_PATTERN = Pattern.compile( "<select name=\"loc\".*?</select>", Pattern.DOTALL );
	private static final Pattern MENU2_PATTERN = Pattern.compile( "<select name=location.*?</select>", Pattern.DOTALL );
	private static final Pattern WHITESPACE_PATTERN = Pattern.compile( "['\\s-]" );

	private static final Pattern SEARCHITEM_PATTERN = Pattern.compile( "searchitem=(\\d+)&searchprice=(\\d+)" );
	private static final Pattern STORE_PATTERN = Pattern.compile( "<tr><td><input name=whichitem type=radio value=(\\d+).*?</tr>", Pattern.DOTALL );

	private static String mainpane = "";

	public List headers = new ArrayList();
	public byte [] rawByteBuffer = null;
	public String contentType = null;
	public String statusLine = "HTTP/1.1 302 Found";

	public LocalRelayRequest()
	{	super( "" );
	}

	public KoLRequest constructURLString( String newURLString )
	{
		super.constructURLString( newURLString );

		this.rawByteBuffer = null;
		this.headers.clear();

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

		return this;
	}

	private static final boolean isJunkItem( int itemId, int price )
	{
		if ( !StaticEntity.getBooleanProperty( "relayHidesJunkMallItems" ) )
			return false;

		if ( price > KoLCharacter.getAvailableMeat() )
			return true;

		if ( NPCStoreDatabase.contains( TradeableItemDatabase.getItemName( itemId ) ) )
			if ( price == 100 || price > TradeableItemDatabase.getPriceById( itemId ) * 2 )
				return true;

		for ( int i = 0; i < preRoninJunkList.size(); ++i )
			if ( ((AdventureResult)preRoninJunkList.get(i)).getItemId() == itemId )
				return true;

		for ( int i = 0; i < postRoninJunkList.size(); ++i )
			if ( ((AdventureResult)postRoninJunkList.get(i)).getItemId() == itemId )
				return true;

		return false;
	}

	protected boolean retryOnTimeout()
	{	return true;
	}

	public void processResponse()
	{
		this.statusLine = "HTTP/1.1 200 OK";
		super.processResponse();

		if ( this.formURLString.startsWith( "http" ) )
			return;

		StringBuffer responseBuffer = new StringBuffer( this.responseText );

		// If this is a store, you can opt to remove all the min-priced items from view
		// along with all the items which are priced above affordable levels.

		if ( this.formURLString.indexOf( "mallstore.php" ) != -1 )
		{
			int searchItemId = -1;
			int searchPrice = -1;

			Matcher itemMatcher = SEARCHITEM_PATTERN.matcher( this.getURLString() );
			if ( itemMatcher.find() )
			{
				searchItemId = StaticEntity.parseInt( itemMatcher.group(1) );
				searchPrice = StaticEntity.parseInt( itemMatcher.group(2) );
			}

			itemMatcher = STORE_PATTERN.matcher( this.responseText );

			while ( itemMatcher.find() )
			{
				String itemData = itemMatcher.group(1);

				int itemId = StaticEntity.parseInt( itemData.substring( 0, itemData.length() - 9 ) );
				int price = StaticEntity.parseInt( itemData.substring( itemData.length() - 9 ) );

				if ( itemId != searchItemId && isJunkItem( itemId, price ) )
					StaticEntity.singleStringDelete( responseBuffer, itemMatcher.group() );
			}

			// Also make sure the item that the person selected when coming into the
			// store is pre-selected.

			if ( searchItemId != -1 )
			{
				String searchString = MallPurchaseRequest.getStoreString( searchItemId, searchPrice );
				StaticEntity.singleStringReplace( responseBuffer, "value=" + searchString, "checked value=" + searchString );
			}
		}

		if ( this.formURLString.indexOf( "compactmenu.php" ) != -1 )
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
			functionMenu.append( KoLCharacter.getUserId() );
			functionMenu.append( "\">Donate</option>" );
			functionMenu.append( "</select>" );

			Matcher menuMatcher = MENU1_PATTERN.matcher( this.responseText );
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

			menuMatcher = MENU2_PATTERN.matcher( this.responseText );
			if ( menuMatcher.find() )
				StaticEntity.singleStringReplace( responseBuffer, menuMatcher.group(), gotoMenu.toString() );

			// Now kill off the weird focusing problems inherent in
			// the Javascript.

			StaticEntity.globalStringReplace( responseBuffer, "selectedIndex=0;", "selectedIndex=0; if ( parent && parent.mainpane ) parent.mainpane.focus();" );
		}

		// Fix chat javascript problems with relay system

		else if ( this.formURLString.indexOf( "lchat.php" ) != -1 )
		{
			StaticEntity.globalStringDelete( responseBuffer, "spacing: 0px;" );
			StaticEntity.globalStringReplace( responseBuffer, "cycles++", "cycles = 0" );
			StaticEntity.globalStringReplace( responseBuffer, "location.hostname", "location.host" );

			StaticEntity.singleStringReplace( responseBuffer, "if (postedgraf", "if (postedgraf == \"/exit\") { document.location.href = \"chatlaunch.php\"; return true; } if (postedgraf" );

			// This is a hack to fix KoL chat, as it is handled
			// in Opera.  No guarantees it works, though.

			StaticEntity.singleStringReplace( responseBuffer, "http.onreadystatechange", "executed = false; http.onreadystatechange" );
			StaticEntity.singleStringReplace( responseBuffer, "readyState==4) {", "readyState==4 && !executed) { executed = true;" );
		}

		// Fix KoLmafia getting outdated by events happening
		// in the browser by using the sidepane.

		else if ( this.formURLString.indexOf( "charpane.php" ) != -1 )
		{
			CharpaneRequest.processCharacterPane( this.responseText );
		}

		// Fix it a little more by making sure that familiar
		// changes and equipment changes are remembered.

		else if ( this.formURLString.indexOf( "main.php" ) != -1 )
		{
			Matcher emailMatcher = EMAIL_PATTERN.matcher( this.responseText );
			if ( emailMatcher.find() )
				responseBuffer = new StringBuffer( emailMatcher.replaceAll( "" ) );
		}
		else
			StaticEntity.externalUpdate( this.getURLString(), this.responseText );

		// Allow a way to get from KoL back to the gCLI
		// using the chat launcher.

		if ( this.formURLString.indexOf( "chatlaunch" ) != -1 )
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
					responseBuffer.insert( linkIndex, "<a href=\"KoLmafia/simulator/index.html\" target=\"_blank\"><b>KoL Simulator</b></a></center><p>See what might happen before it happens!</p><center>" );
			}
		}

		if ( StaticEntity.getBooleanProperty( "relayAddsQuickScripts" ) && this.formURLString.indexOf( "menu" ) != -1 )
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
			RequestEditorKit.getFeatureRichHTML( this.formURLString.toString(), responseBuffer, true );
		}
		catch ( Exception e )
		{
			StaticEntity.printStackTrace( e );
		}

		// Load image files locally to reduce bandwidth
		// and improve mini-browser performance.

		if ( StaticEntity.getBooleanProperty( "relayUsesCachedImages" ) )
			StaticEntity.globalStringReplace( responseBuffer, "http://images.kingdomofloathing.com", "/images" );
		else
			StaticEntity.globalStringReplace( responseBuffer, "http://images.kingdomofloathing.com/scripts", "/images/scripts" );

		// Download and link to any Players of Loathing
		// picture pages locally.

		StaticEntity.globalStringReplace( responseBuffer, "http://pics.communityofloathing.com/albums", "/images" );

		// Remove the default frame busting script so that
		// we can detach user interface elements.

		StaticEntity.singleStringReplace( responseBuffer, "frames.length == 0", "frames.length == -1" );
		this.responseText = responseBuffer.toString();
		CustomItemDatabase.linkCustomItem( this );
	}

	public void printHeaders( PrintStream ostream )
	{
		if ( !this.headers.isEmpty() )
		{
			for ( int i = 0; i < headers.size(); ++i )
				ostream.println( headers.get(i) );
		}
		else
		{
			String header;

			for ( int i = 0; (header = this.formConnection.getHeaderFieldKey( i )) != null; ++i )
			{
				if ( header.equals( "Content-Type" ) || header.equals( "Content-Length" ) || header.equals( "Connection" ) || header.equals( "Transfer-Encoding" ) )
					continue;

				ostream.print( header );
				ostream.print( ": " );
				ostream.println( this.formConnection.getHeaderField( i ) );
			}

			if ( this.responseCode == 200 && this.rawByteBuffer != null )
			{
				ostream.print( "Content-Type: " );
				ostream.print( this.contentType );

				if ( this.contentType.startsWith( "text" ) )
					ostream.print( "; charset=UTF-8" );

				ostream.println();

				ostream.print( "Content-Length: " );
				ostream.print( this.rawByteBuffer.length );
				ostream.println();
			}
		}


		ostream.println( "Connection: close" );
	}

	public void pseudoResponse( String status, String responseText )
	{
		this.statusLine = status;

		this.headers.add( "Date: " + ( new Date() ) );
		this.headers.add( "Server: " + VERSION_NAME );

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
				this.headers.add( "Content-Type: " + this.contentType );
		}
		else
		{
			this.responseText = " ";
		}
	}

	private StringBuffer readContents( BufferedReader reader ) throws IOException
	{
		String line = null;
		StringBuffer contentBuffer = new StringBuffer();

		if ( reader == null )
			return contentBuffer;

		while ( (line = reader.readLine()) != null )
		{
			contentBuffer.append( line );
			contentBuffer.append( LINE_BREAK );
		}

		reader.close();
		return contentBuffer;
	}

	private void sendLocalImage( String filename )
	{
		try
		{
			this.sendLocalImageHelper( filename );
		}
		catch ( Exception e )
		{
			this.sendNotFound();
		}
	}

	private void sendLocalImageHelper( String filename ) throws Exception
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
		this.pseudoResponse( "HTTP/1.1 200 OK", "" );
	}

	private void sendSharedFile( String filename )
	{
		try
		{
			this.sendSharedFileHelper( filename );
		}
		catch ( Exception e )
		{
			this.sendNotFound();
		}
	}

	public static void setNextMain( String mainpane )
	{	LocalRelayRequest.mainpane = mainpane;
	}

	private static final Pattern MAINPANE_PATTERN = Pattern.compile( "name=mainpane src=\"(.*?)\"", Pattern.DOTALL );

	private void sendSharedFileHelper( String filename ) throws Exception
	{
		boolean isServerRequest = !filename.startsWith( "KoLmafia" );
		if ( !isServerRequest )
			filename = filename.substring( 9 );

		int index = filename.indexOf( "/" );
		boolean writePseudoResponse = !isServerRequest;

		StringBuffer replyBuffer = new StringBuffer();
		String name = filename.substring( index + 1 );
		String directory = index <= 0 ? "html" : "html/" + filename.substring( 0, index );
		BufferedReader reader = DataUtilities.getReader( directory, name );

		if ( reader == null && filename.startsWith( "simulator" ) )
		{
			this.downloadSimulatorFile( name );
			reader = DataUtilities.getReader( directory, name );
		}

		if ( reader != null )
		{
			// Now that you know the reader exists, read the
			// contents of the reader.

			replyBuffer = this.readContents( reader );
			writePseudoResponse = true;
		}
		else if ( isServerRequest )
		{
			// If there's no override file, go ahead and
			// request the page from the server normally.

			super.run();

			if ( this.responseCode == 302 )
			{
				this.pseudoResponse( "HTTP/1.1 302 Found", this.redirectLocation );
				return;
			}
			else if ( this.responseCode != 200 )
			{
				this.sendNotFound();
				return;
			}
		}
		else
		{
			this.sendNotFound();
			return;
		}

		if ( (filename.equals( "main.html" ) || filename.equals( "main_c.html" )) && !mainpane.equals( "" ) )
		{
			if ( writePseudoResponse )
			{
				this.pseudoResponse( "HTTP/1.1 200 OK", replyBuffer.toString() );
				writePseudoResponse = false;
			}

			this.responseText = MAINPANE_PATTERN.matcher( this.responseText ).replaceFirst(
				"name=mainpane src=\"" + mainpane + "\"" );

			mainpane = "";
		}

		// Add brand new Javascript to every single page.  Check
		// to see if a reader exists for the file.

		if ( filename.endsWith( "simulator/index.html" ) )
		{
			writePseudoResponse = true;
			this.handleSimulatorIndex( replyBuffer );
		}

		if ( writePseudoResponse )
		{
			// Make sure to print the reply buffer to the
			// response buffer for the local relay server.

			if ( this.isChatRequest )
				StaticEntity.globalStringReplace( replyBuffer, "<br>", "</font><br>" );

			if ( filename.endsWith( "chat.html" ) )
				RequestEditorKit.addChatFeatures( replyBuffer );

			this.pseudoResponse( "HTTP/1.1 200 OK", replyBuffer.toString() );
		}
	}

	private static String getSimulatorName( int equipmentSlot )
	{
		AdventureResult item = KoLCharacter.getEquipment( equipmentSlot );

		if ( equipmentSlot == KoLCharacter.FAMILIAR && item.getName().equals( FamiliarsDatabase.getFamiliarItem( KoLCharacter.getFamiliar().getId() ) ) )
			return "familiar-specific +5 lbs.";

		if ( item == EquipmentRequest.UNEQUIP )
			return "(None)";

		return item.getName();
	}

	private void handleSimulatorIndex( StringBuffer replyBuffer ) throws IOException
	{
		StringBuffer scriptBuffer = this.readContents( DataUtilities.getReader( "html/simulator", "index.js" ) );

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

		String familiarEquipment = getSimulatorName( KoLCharacter.FAMILIAR );
		if ( FamiliarData.itemWeightModifier( TradeableItemDatabase.getItemId( familiarEquipment ) ) == 5 )
			StaticEntity.globalStringReplace( scriptBuffer, "/*familiarEquip*/", "familiar-specific +5 lbs." );
		else
			StaticEntity.globalStringReplace( scriptBuffer, "/*familiarEquip*/", familiarEquipment );

		// Change the player's equipment

		StaticEntity.globalStringReplace( scriptBuffer, "/*hat*/", getSimulatorName( KoLCharacter.HAT ) );
		StaticEntity.globalStringReplace( scriptBuffer, "/*weapon*/", getSimulatorName( KoLCharacter.WEAPON ) );
		StaticEntity.globalStringReplace( scriptBuffer, "/*offhand*/", getSimulatorName( KoLCharacter.OFFHAND ) );
		StaticEntity.globalStringReplace( scriptBuffer, "/*shirt*/", getSimulatorName( KoLCharacter.SHIRT ) );
		StaticEntity.globalStringReplace( scriptBuffer, "/*pants*/", getSimulatorName( KoLCharacter.PANTS ) );

		// Change the player's accessories

		StaticEntity.globalStringReplace( scriptBuffer, "/*accessory1*/", getSimulatorName( KoLCharacter.ACCESSORY1 ) );
		StaticEntity.globalStringReplace( scriptBuffer, "/*accessory2*/", getSimulatorName( KoLCharacter.ACCESSORY2 ) );
		StaticEntity.globalStringReplace( scriptBuffer, "/*accessory3*/", getSimulatorName( KoLCharacter.ACCESSORY3 ) );

		// Load up the player's current skillset to figure
		// out what passive skills are available.

		UseSkillRequest [] skills = new UseSkillRequest[ availableSkills.size() ];
		availableSkills.toArray( skills );

		StringBuffer passiveSkills = new StringBuffer();
		for ( int i = 0; i < skills.length; ++i )
		{
			int skillId = skills[i].getSkillId();
			if ( !( ClassSkillsDatabase.getSkillType( skillId ) == ClassSkillsDatabase.PASSIVE && !(skillId < 10 || (skillId > 14 && skillId < 1000)) ) )
				continue;

			passiveSkills.append( "\"" );
			passiveSkills.append( WHITESPACE_PATTERN.matcher( skills[i].getSkillName() ).replaceAll( "" ).toLowerCase() );
			passiveSkills.append( "\"," );
		}

		StaticEntity.globalStringReplace( scriptBuffer, "/*passiveSkills*/", passiveSkills.toString() );

		// Also load up the player's current active effects
		// and fill them into the buffs area.

		AdventureResult [] effects = new AdventureResult[ activeEffects.size() ];
		activeEffects.toArray( effects );

		StringBuffer activeEffects = new StringBuffer();
		for ( int i = 0; i < effects.length; ++i )
		{
			activeEffects.append( "\"" );
			activeEffects.append( WHITESPACE_PATTERN.matcher( effects[i].getName() ).replaceAll( "" ).replaceAll( "\u00f1", "n" ).toLowerCase() );
			activeEffects.append( "\"," );
		}

		StaticEntity.globalStringReplace( scriptBuffer, "/*activeEffects*/", activeEffects.toString() );
		StaticEntity.globalStringReplace( scriptBuffer, "/*rockAndRoll*/", "false" );

		StaticEntity.globalStringReplace( scriptBuffer, "/*lastZone*/", StaticEntity.getProperty( "lastAdventure" ) );
		StaticEntity.globalStringReplace( scriptBuffer, "/*lastMonster*/", FightRequest.getCurrentKey() );

		StaticEntity.globalStringReplace( scriptBuffer, "/*moonPhase*/", (int) ((MoonPhaseDatabase.getGrimacePhase()-1) * 2
			+ Math.round( (MoonPhaseDatabase.getRonaldPhase()-1) / 2.0f - Math.floor( (MoonPhaseDatabase.getRonaldPhase()-1) / 2.0f ) )) );

		StaticEntity.globalStringReplace( scriptBuffer, "/*minimoonPhase*/", String.valueOf( MoonPhaseDatabase.getHamburglarPosition( new Date() ) ) );

		scriptBuffer.insert( 0, LINE_BREAK );
		scriptBuffer.insert( 0, LINE_BREAK );
		scriptBuffer.insert( 0, "<script language=\"Javascript\">" );
		scriptBuffer.append( LINE_BREAK );
		scriptBuffer.append( LINE_BREAK );
		scriptBuffer.append( "</script>" );

		this.insertAfterEnd( replyBuffer, scriptBuffer.toString() );
	}

	public void insertAfterEnd( StringBuffer replyBuffer, String contents )
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

	public void submitCommand()
	{
		CommandDisplayFrame.executeCommand( this.getFormField( "cmd" ) );
		this.pseudoResponse( "HTTP/1.1 200 OK", "" );
	}

	public void executeCommand()
	{
		CommandDisplayFrame.executeCommand( this.getFormField( "cmd" ) );
		this.pseudoResponse( "HTTP/1.1 200 OK", "" );
	}

	public void sideCommand()
	{
		CommandDisplayFrame.executeCommand( this.getFormField( "cmd" ) );
		while ( CommandDisplayFrame.hasQueuedCommands() )
			delay( 500 );

		this.pseudoResponse( "HTTP/1.1 302 Found", "/charpane.php" );
	}

	public void sendNotFound()
	{
		this.pseudoResponse( "HTTP/1.1 404 Not Found", "" );
		this.responseCode = 404;
	}

	public void sendBossWarning( String name, String image, int mcd1, String item1, int mcd2, String item2 )
	{
		int mcd0 = KoLCharacter.getMindControlLevel();

		StringBuffer warning = new StringBuffer();

		warning.append( "<html><head><script language=Javascript src=\"/KoLmafia/basics.js\"></script>" );

		warning.append( "<script language=Javascript> " );
		warning.append( "var default0 = " + mcd0 + "; " );
		warning.append( "var default1 = " + mcd1 + "; " );
		warning.append( "var default2 = " + mcd2 + "; " );
		warning.append( "var current = " + mcd0 + "; " );
		warning.append( "function switchLinks( id ) { " );
		warning.append( "if ( id == 'mcd1' ) { " );
		warning.append( "current = (current == default0) ? default1 : default0; " );
		warning.append( "} else { " );
		warning.append( "current = (current == default0) ? default2 : default0; " );
		warning.append( "} " );
		warning.append( "getObject('mcd1').style.border = (current == default1) ? '1px dashed blue' : '1px dashed white'; " );
		warning.append( "getObject('mcd2').style.border = (current == default2) ? '1px dashed blue' : '1px dashed white'; " );
		warning.append( "top.charpane.location.href = '/KoLmafia/sideCommand?cmd=mcd+' + current; " );
		warning.append( "} </script>" );

		warning.append( "<link rel=\"stylesheet\" type=\"text/css\" href=\"http://images.kingdomofloathing.com/styles.css\"></head>" );
		warning.append( "<body><center><table width=95%  cellspacing=0 cellpadding=0><tr><td style=\"color: white;\" align=center bgcolor=blue><b>Results:</b></td></tr><tr><td style=\"padding: 5px; border: 1px solid blue;\"><center><table><tr><td><center>" );

		warning.append( "<table><tr>" );

		warning.append( "<td align=center valign=center><div id=\"mcd1\" style=\"padding: 4px 4px 4px 4px" );

		if ( mcd0 == mcd1 )
			warning.append( "; border: 1px dashed blue" );

		warning.append( "\"><a id=\"link1\" style=\"text-decoration: none\" onClick=\"switchLinks('mcd1'); void(0);\" href=\"#\"><img src=\"http://images.kingdomofloathing.com/itemimages/" );
		warning.append( item1 );
		warning.append( "\" width=30 height=30><br /><font size=1>MCD " );
		warning.append( mcd1 );
		warning.append( "</font></a></div></td>" );

		warning.append( "<td>&nbsp;&nbsp;&nbsp;&nbsp;</td>" );

		warning.append( "<td valign=center><a href=\"" );
		warning.append( this.getURLString() );
		warning.append( "&override=on" );
		warning.append( "\"><img src=\"http://images.kingdomofloathing.com/adventureimages/" );
		warning.append( image );
		warning.append( "\"></a></td>" );

		warning.append( "<td>&nbsp;&nbsp;&nbsp;&nbsp;</td>" );

		warning.append( "<td align=center valign=center><div id=\"mcd2\" style=\"padding: 4px 4px 4px 4px" );

		if ( KoLCharacter.getMindControlLevel() == mcd2 )
			warning.append( "; border: 1px dashed blue" );

		warning.append( "\"><a id=\"link2\" style=\"text-decoration: none\" onClick=\"switchLinks('mcd2'); void(0);\" href=\"#\"><img src=\"http://images.kingdomofloathing.com/itemimages/" );
		warning.append( item2 );
		warning.append( "\" width=30 height=30><br /><font size=1>MCD " );
		warning.append( mcd2 );
		warning.append( "</font></a></div></td>" );

		warning.append( "</tr></table></center><blockquote>The " );
		warning.append( name );

		warning.append( " drops special rewards based on your mind-control level.  If you'd like a special reward, click on one of the items above to set your mind-control device appropriately.  Click on it again to reset the MCD back to your old setting.  Click on the " );
		warning.append( name );

		warning.append( " once you've decided to proceed.</blockquote></td></tr></table></center></td></tr></table></center></body></html>" );

		this.pseudoResponse( "HTTP/1.1 200 OK", warning.toString() );
	}

	public void sendGeneralWarning( String image, String message )
	{
		StringBuffer warning = new StringBuffer();

		warning.append( "<html><head><link rel=\"stylesheet\" type=\"text/css\" href=\"http://images.kingdomofloathing.com/styles.css\"></head><body><center><table width=95%  cellspacing=0 cellpadding=0><tr><td style=\"color: white;\" align=center bgcolor=blue><b>Results:</b></td></tr><tr><td style=\"padding: 5px; border: 1px solid blue;\"><center><table><tr><td>" );

		if ( image != null && !image.equals( "" ) )
		{
			warning.append( "<center><img src=\"http://images.kingdomofloathing.com/itemimages/" );
			warning.append( image );
			warning.append( "\" width=30 height=30><br></center>" );
		}

		warning.append( "<blockquote>" );
		warning.append( message );
		warning.append( "</blockquote></td></tr></table></center></td></tr></table></center></body></html>" );

		this.pseudoResponse( "HTTP/1.1 200 OK", warning.toString() );
	}

	public void run()
	{
		// If there is an attempt to view the error page, or if
		// there is an attempt to view the robots file, neither
		// are available on KoL, so return.

		if ( this.formURLString.equals( "missingimage.gif" ) || this.formURLString.endsWith( "robots.txt" ) || this.formURLString.endsWith( "favicon.ico" ) )
		{
			this.sendNotFound();
			return;
		}

		// If this is a script request, then run the script and
		// return the client HTML.

		if ( RELAY_LOCATION.exists() && KoLmafiaASH.getClientHTML( this ) )
			return;

		boolean isWebPage = this.formURLString.endsWith( ".php" ) || this.formURLString.endsWith( ".html" );
		boolean isCommand = this.formURLString.startsWith( "KoLmafia/" );
		boolean isImage = this.formURLString.startsWith( "images/" ) || this.formURLString.endsWith( ".css" ) || this.formURLString.endsWith( ".js" );

		if ( !isWebPage && !isCommand && !isImage )
		{
			this.sendNotFound();
			return;
		}

		// Load custom items from OneTonTomato's script if they
		// are currently being requested.

		if ( this.formURLString.equals( "desc_item.php" ) )
		{
			String item = this.getFormField( "whichitem" );
			if ( item != null && item.startsWith( "custom" ) )
			{
				this.pseudoResponse( "HTTP/1.1 200 OK", CustomItemDatabase.retrieveCustomItem( item.substring(6) ) );
				return;
			}
		}

		// Special handling of adventuring locations before it's
		// registered internally with KoLmafia.

		if ( this.formURLString.indexOf( "adventure.php" ) != -1 )
		{
			String location = this.getFormField( "snarfblat" );
			if ( location == null )
				location = this.getFormField( "adv" );

			TurnCounter expired = StaticEntity.getExpiredCounter( location );

			if ( expired != null )
			{
				this.sendGeneralWarning( expired.getImage(),
					"The indicated counter has expired, so may wish to adventure somewhere else at this time.  " +
					"If you are certain that this is where you'd like to adventure, click <a href=\"" + this.getURLString() + "\">here</a> to proceed." );

				return;
			}

			// Sometimes, people want the MCD rewards from various boss
			// monsters.  Let's help out.  This one's for the Boss Bat,
			// who has special items at 4 and 8.

			if ( location != null && location.equals( "34" ) && this.getFormField( "override" ) == null )
			{
				this.sendBossWarning( "Boss Bat", "bossbat.gif", 4, "batpants.gif", 8, "batbling.gif" );
				return;
			}
		}

		// More MCD rewards.  This one is for the Knob Goblin King,
		// who has special items at 3 and 7.

		if ( this.formURLString.indexOf( "knob.php" ) != -1 &&
			this.getFormField( "king" ) != null && this.getFormField( "override" ) == null )
		{
			this.sendBossWarning( "Knob Goblin King", "goblinking.gif", 3, "glassballs.gif", 7, "batcape.gif" );
			return;
		}

		// More MCD rewards.  This one is for the Bonerdagon, who has
		// special items at 5 and 10.

		if ( this.formURLString.indexOf( "cyrpt.php" ) != -1 &&
			this.getFormField( "action" ) != null && this.getFormField( "override" ) == null )
		{
			this.sendBossWarning( "Bonerdagon", "bonedragon.gif", 5, "rib.gif", 10, "vertebra.gif" );
			return;
		}

		// If the person is visiting the sorceress and they forgot
		// to make the Wand, remind them.

		if ( this.formURLString.indexOf( "lair6.php" ) != -1 && this.getFormField( "place" ) != null &&
			this.getFormField( "place" ).equals( "5" ) && this.getFormField( "override" ) == null )
		{
			// As of NS 13,they need not have it equipped. In fact,
			// there are far better weapons to equip for the
			// battle. But, just in case, check current equipment
			// as well as inventory.

			if ( !KoLCharacter.hasEquipped( SorceressLair.NAGAMAR ) && !AdventureDatabase.retrieveItem( SorceressLair.NAGAMAR ) )
			{
				this.sendGeneralWarning( "wand.gif", "It's possible there is something very important you're forgetting to do." );
				return;
			}
		}

		if ( this.formURLString.indexOf( "ascend.php" ) != -1 && this.getFormField( "action" ) != null )
			RequestThread.postRequest( new EquipmentRequest( SpecialOutfit.BIRTHDAY_SUIT ) );

		// If you are in chat, and the person submitted a command
		// via chat, check to see if it's a CLI command.

		String chatResponse = ChatRequest.executeChatCommand( this.getFormField( "graf" ) );
		if ( chatResponse != null )
		{
			this.pseudoResponse( "HTTP/1.1 200 OK",
				"<font color=\"blue\"><b><a target=\"mainpane\" href=\"showplayer.php?who=458968\" style=\"color:blue\">" +
				VERSION_NAME + "</a> (private)</b>: " + chatResponse + "</font><br>" );

			return;
		}

		// None of the above checks wound up happening.  So, do some
		// special handling, catching any exceptions that happen to
		// popup along the way.

		if ( this.formURLString.endsWith( "submitCommand" ) )
		{
			this.submitCommand();
		}
		else if ( this.formURLString.endsWith( "executeCommand" ) )
		{
			this.executeCommand();
		}
		else if ( this.formURLString.endsWith( "sideCommand" ) )
		{
			this.sideCommand();
		}
		else if ( this.formURLString.endsWith( "messageUpdate" ) )
		{
			this.pseudoResponse( "HTTP/1.1 200 OK", LocalRelayServer.getNewStatusMessages() );
		}
		else if ( this.formURLString.indexOf( "images/playerpics/" ) != -1 )
		{
			RequestEditorKit.downloadImage( "http://pics.communityofloathing.com/albums/" +
				this.formURLString.substring( this.formURLString.indexOf( "playerpics" ) ) );

			this.sendLocalImage( this.formURLString );
		}
		else if ( this.formURLString.indexOf( "images/" ) != -1 )
		{
			this.sendLocalImage( this.formURLString );
		}
		else if ( this.formURLString.indexOf( "lchat.php" ) != -1 )
		{
			if ( StaticEntity.getBooleanProperty( "relayUsesIntegratedChat" ) )
			{
				this.sendSharedFile( "chat.html" );
			}
			else
			{
				this.sendSharedFile( this.formURLString );
				this.responseText = StaticEntity.globalStringReplace( this.responseText, "<p>", "<br><br>" );
				this.responseText = StaticEntity.globalStringReplace( this.responseText, "<P>", "<br><br>" );
				this.responseText = StaticEntity.singleStringDelete( this.responseText, "</span>" );
			}
		}
		else
		{
			if ( this.formURLString.endsWith( ".html" ) )
				this.data.clear();

			this.sendSharedFile( this.formURLString );

			if ( this.isChatRequest )
			{
				if ( !KoLMessenger.isRunning() || this.formURLString.indexOf( "submitnewchat.php" ) != -1 )
					KoLMessenger.updateChat( this.responseText );

				if ( StaticEntity.getBooleanProperty( "relayFormatsChatText" ) )
					this.responseText = KoLMessenger.getNormalizedContent( this.responseText, false );
			}
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

			File directory = new File( ROOT_LOCATION, "html/simulator/" );
			directory.mkdirs();

			StaticEntity.globalStringReplace( contents, "images/", "http://sol.kolmafia.us/images/" );

			PrintStream writer = RequestLogger.openStream( "html/simulator/" + filename, NullStream.INSTANCE, true );
			writer.println( contents.toString() );
			writer.close();
		}
		catch ( Exception e )
		{
			KoLmafia.updateDisplay( ERROR_STATE, "Failed to create simulator file <" + filename + ">" );
		}
	}
}
