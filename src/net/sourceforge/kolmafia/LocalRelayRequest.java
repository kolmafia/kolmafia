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

import java.net.HttpURLConnection;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.io.BufferedReader;
import java.io.IOException;

import java.util.List;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.ArrayList;
import java.util.Vector;

import java.util.regex.Pattern;
import java.util.regex.Matcher;
import net.java.dev.spellcast.utilities.DataUtilities;

public class LocalRelayRequest extends KoLRequest
{
	protected String fullResponse;
	protected List headers = new ArrayList();

	public LocalRelayRequest( KoLmafia client, String formURLString, boolean followRedirects )
	{	super( client, formURLString, followRedirects );
	}

	public String getFullResponse()
	{	return fullResponse;
	}
	
	protected void processRawResponse( String rawResponse )
	{
		super.processRawResponse( rawResponse );
		this.fullResponse = rawResponse;

		if ( formURLString.startsWith( "http" ) )
			return;
		
		if ( formURLString.indexOf( "compactmenu.php" ) != -1 )
		{
			// Mafiatize the function menu

			fullResponse = fullResponse.replaceAll(
				"<option value=.?inventory.?php.?>Inventory</option>",
				"<option value=\"inventory.php?which=1\">Consumables</option>\n<option value=\"inventory.php?which=2\">Equipment</option>\n<option value=\"inventory.php?which=3\">Miscellaneous</option>" );
			fullResponse = fullResponse.replaceAll(
				"<option value=.?questlog.?php.?>Quests</option>",
				"<option value=\"familiar.php\">Terrarium</option>" );
			fullResponse = fullResponse.replaceAll(
				"<option value=.?messages.?php.?>Read Messages</option>",
				"<option value=\"questlog.php?which=1\">Quest Log</option>\n<option value=\"messages.php\">Read Messages</option>" );

			fullResponse = fullResponse.replaceAll( ">Store</option>", ">Asymmetric Store</option>" );
			fullResponse = fullResponse.replaceAll( ">Donate</option>", ">Donate to KoL</option>" );

			// Remove only the logout option
			// since it might cause problems.
			
			fullResponse = fullResponse.replaceAll( "<option value=.?logout.?php.?>Log Out</option>", "" );

			// Mafiatize the goto menu

			fullResponse = fullResponse.replaceAll(
				"<option value=.?mountains.?php.?>",
				"<option value=\"mclargehuge.php\">Mt. McLargeHuge</option>\n<option value=\"mountains.php\">" );

			fullResponse = fullResponse.replaceAll(
				"Nearby Plains</a>",
				"Nearby Plains</a>\n<option value=\"beanstalk.php\">Above Beanstalk</option>\n" );
		}

		// Fix chat javascript problems with relay system

		if ( formURLString.indexOf( "lchat.php" ) != -1 )
		{
			fullResponse = fullResponse.replaceAll(
				"window.?location.?hostname", 
				"\"127.0.0.1:" + LocalRelayServer.getPort() + "\"" );

			fullResponse = fullResponse.replaceAll(
				"</head>", 
				"<script language=\"Javascript\">base = \"http://127.0.0.1:" +  LocalRelayServer.getPort() + "\";</script></head>" );
		}
		
		// Fix KoLmafia getting outdated by events happening
		// in the browser by using the sidepane.
		
		else if ( formURLString.indexOf( "charpane.php") != -1 )
			CharpaneRequest.processCharacterPane( responseText );

		// Fix it a little more by making sure that familiar
		// changes and equipment changes are remembered.
		
		else
			StaticEntity.externalUpdate( formURLString, responseText );

		// Allow a way to get from KoL back to the gCLI
		// using the chat launcher.
		
		if ( formURLString.indexOf( "chatlaunch" ) != -1 )
		{
			if ( StaticEntity.getProperty( "relayAddsCommandLineLinks" ).equals( "true" ) )
			{
				fullResponse = fullResponse.replaceFirst( "<a href",
					"<a href=\"KoLmafia/cli.html\"><b>KoLmafia gCLI</b></a></center><p>Loads in this frame to allow for manual adventuring.</p><center><a href");
			}
			if ( StaticEntity.getProperty( "relayAddsSimulatorLinks" ).equals( "true" ) )
			{
				fullResponse = fullResponse.replaceFirst( "<a href",
					"<a target=_new href=\"KoLmafia/simulator/index.html\"><b>KoL Simulator</b></a></center><p>Ayvuir's Simulator of Loathing, as found on the forums.</p><center><a href");
			}
		}

		fullResponse = RequestEditorKit.getFeatureRichHTML( fullResponse );
	}

	public String getHeader( int index )
	{
		if ( headers.isEmpty() )
		{
			// This request was relayed to the server. Respond with those headers.
			headers.add( formConnection.getHeaderField( 0 ) );
			for ( int i = 1; formConnection.getHeaderFieldKey( i ) != null; ++i )
				if ( !formConnection.getHeaderFieldKey( i ).equals( "Transfer-Encoding" ) )
					headers.add( formConnection.getHeaderFieldKey( i ) + ": " + formConnection.getHeaderField( i ) );
		}

		return index >= headers.size() ? null : (String) headers.get( index );
	}
	
	protected void pseudoResponse( String status, String fullResponse )
	{
		this.fullResponse = fullResponse.replaceAll( "<.?--MAFIA_HOST_PORT-->", "127.0.0.1:" + LocalRelayServer.getPort() );
		if ( fullResponse.length() == 0 )
			this.fullResponse = " ";

		headers.clear();
		headers.add( status );
		headers.add( "Date: " + ( new Date() ) );
		headers.add( "Server: " + VERSION_NAME );
		headers.add( "Cache-Control: no-store, no-cache, must-revalidate, post-check=0, pre-check=0" );
		headers.add( "Pragma: no-cache" );
		headers.add( "Content-Length: " + this.fullResponse.length() );
		headers.add( "Connection: close" );
		
		if ( formURLString.endsWith( ".css" ) )
			headers.add( "Content-Type: text/css; charset=UTF-8" );
		else if ( formURLString.endsWith( ".js" ) )
			headers.add( "Content-Type: text/javascript; charset=UTF-8" );
		else
			headers.add( "Content-Type: text/html; charset=UTF-8" );
	}	

	protected void sendSharedFile( String filename ) throws IOException
	{
		int index = filename.indexOf( "/" );
		String name = filename.substring( index + 1 );

		StringBuffer replyBuffer = new StringBuffer();
		BufferedReader reader = DataUtilities.getReader(
			index == -1 ? "html" : "html/" + filename.substring( 0, index ), name );

		if ( reader == null )
		{
			if ( filename.startsWith( "simulator" ) )
			{
				downloadSimulatorFile( name );
				reader = DataUtilities.getReader( "html", filename );
			}
			
			if ( reader == null )
			{
				sendNotFound();
				return;
			}
		}

		String line = null;
		while ( (line = reader.readLine()) != null )
		{
			if ( line.indexOf( "<img" ) != -1 )
			{
				String directory = (new File( "html" + File.separator + filename )).getParent();
				if ( !directory.endsWith( File.separator ) )
					directory += File.separator;

				StringBuffer lineBuffer = new StringBuffer();

				Matcher imageMatcher = Pattern.compile( "<img src=\"([^\"]*?)\"" ).matcher( line );
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

			replyBuffer.append( line );
			replyBuffer.append( LINE_BREAK );
		}
		
		reader.close();
		pseudoResponse( "HTTP/1.1 200 OK", filename.equals( "simulator/index.html" ) ?
			handleSimulatorIndex( replyBuffer.toString() ) : replyBuffer.toString() );
	}

	private List getAvailableAccessories()
	{
		List available = (List) KoLCharacter.getEquipmentLists()[ KoLCharacter.ACCESSORY1 ].clone();
		if ( !available.contains( KoLCharacter.getEquipment( KoLCharacter.ACCESSORY2 ) ) )
			available.add( KoLCharacter.getEquipment( KoLCharacter.ACCESSORY2 ) );
		if ( !available.contains( KoLCharacter.getEquipment( KoLCharacter.ACCESSORY3 ) ) )
			available.add( KoLCharacter.getEquipment( KoLCharacter.ACCESSORY3 ) );
		return available;
	}
	
	private String handleSimulatorIndex( String responseText )
	{
		// This is the simple Javascript which can be added
		// arbitrarily to the end without having to modify
		// the underlying HTML.
		
		StringBuffer loaderScript = new StringBuffer();
		loaderScript.append( "<script language=\"Javascript\"> " ); 

		String className = KoLCharacter.getClassType().toLowerCase();

		int classIndex = -1;
		for ( int i = 0; i < KoLmafiaASH.CLASSES.length; ++i )
			if ( KoLmafiaASH.CLASSES[i].equals( className ) )
				classIndex = i;

		// Basic additions of player stats.  Includes
		// everything on left-hand side of simulator.
		
		loaderScript.append( "function loadKoLmafiaData() { " );
		loaderScript.append( "document.character.charclass.selectedIndex = " + classIndex + "; " ); 
		loaderScript.append( "document.character.basemuscle.value = " + KoLCharacter.getBaseMuscle() + "; " ); 
		loaderScript.append( "document.character.basemuscle.value = " + KoLCharacter.getBaseMuscle() + "; " ); 
		loaderScript.append( "document.character.basemysticality.value = " + KoLCharacter.getBaseMysticality() + "; " ); 
		loaderScript.append( "document.character.basemoxie.value = " + KoLCharacter.getBaseMoxie() + "; " ); 
		loaderScript.append( "document.character.mcd.selectedIndex = " + KoLCharacter.getMindControlLevel() + "; " ); 
		loaderScript.append( "document.character.weight.value = " + KoLCharacter.getFamiliar().getWeight() + "; " ); 

		// Change the player's familiar to the current
		// familiar using a cheap loop hack.

		loaderScript.append( "for ( i = 0; i < document.character.familiar.options.length; ++i ) if ( document.character.familiar.options[i].innerHTML.toLowerCase() == \"" +
			KoLCharacter.getFamiliar().getName().toLowerCase() + "\" ) document.character.familiar.selectedIndex = i; " );
		loaderScript.append( "for ( i = 0; i < document.equipment.familiarequip.options.length; ++i ) if ( document.equipment.familiarequip.options[i].value.toLowerCase() == \"" );

		if ( FamiliarData.itemWeightModifier( TradeableItemDatabase.getItemID( KoLCharacter.getCurrentEquipmentName( KoLCharacter.FAMILIAR ) ) ) == 5 )
			loaderScript.append( "familiar-specific +5 lbs." );
		else
			loaderScript.append( KoLCharacter.getCurrentEquipmentName( KoLCharacter.FAMILIAR ).toLowerCase() );

		loaderScript.append( "\" ) document.equipment.familiarequip.selectedIndex = i; " );

		// Change the player's equipment around using
		// a cheap loop hack.

		loaderScript.append( "for ( i = 0; i < numberofitemchoices[0]; ++i ) if ( equipment[0][i].name.toLowerCase() == \"" +
			KoLCharacter.getCurrentEquipmentName( KoLCharacter.HAT ).toLowerCase() + "\" ) document.equipment.hat.selectedIndex = i; " );
		loaderScript.append( "for ( i = 0; i < numberofitemchoices[1]; ++i ) if ( equipment[1][i].name.toLowerCase() == \"" +
			KoLCharacter.getCurrentEquipmentName( KoLCharacter.WEAPON ).toLowerCase() + "\" ) document.equipment.weapon.selectedIndex = i; " );
		loaderScript.append( "for ( i = 0; i < numberofitemchoices[2]; ++i ) if ( equipment[2][i].name.toLowerCase() == \"" +
			KoLCharacter.getCurrentEquipmentName( KoLCharacter.OFFHAND ).toLowerCase() + "\" ) document.equipment.offhand.selectedIndex = i; " );
		loaderScript.append( "for ( i = 0; i < numberofitemchoices[3]; ++i ) if ( equipment[3][i].name.toLowerCase() == \"" +
			KoLCharacter.getCurrentEquipmentName( KoLCharacter.SHIRT ).toLowerCase() + "\" ) document.equipment.shirt.selectedIndex = i; " );
		loaderScript.append( "for ( i = 0; i < numberofitemchoices[4]; ++i ) if ( equipment[4][i].name.toLowerCase() == \"" +
			KoLCharacter.getCurrentEquipmentName( KoLCharacter.PANTS ).toLowerCase() + "\" ) document.equipment.pants.selectedIndex = i; " );
		loaderScript.append( "for ( i = 0; i < numberofitemchoices[5]; ++i ) if ( equipment[5][i].name.toLowerCase() == \"" +
			KoLCharacter.getCurrentEquipmentName( KoLCharacter.ACCESSORY1 ).toLowerCase() + "\" ) document.equipment.acc1.selectedIndex = i; " );
		loaderScript.append( "for ( i = 0; i < numberofitemchoices[6]; ++i ) if ( equipment[6][i].name.toLowerCase() == \"" +
			KoLCharacter.getCurrentEquipmentName( KoLCharacter.ACCESSORY2 ).toLowerCase() + "\" ) document.equipment.acc2.selectedIndex = i; " );
		loaderScript.append( "for ( i = 0; i < numberofitemchoices[7]; ++i ) if ( equipment[7][i].name.toLowerCase() == \"" +
			KoLCharacter.getCurrentEquipmentName( KoLCharacter.ACCESSORY3 ).toLowerCase() + "\" ) document.equipment.acc3.selectedIndex = i; " );

		// Load up the player's current skillset to figure
		// out what passive skills are available.
		
		UseSkillRequest [] skills = new UseSkillRequest[ KoLCharacter.getAvailableSkills().size() ];
		KoLCharacter.getAvailableSkills().toArray( skills );
		
		for ( int i = 0; i < skills.length; ++i )
		{
			int skillID = skills[i].getSkillID();
			
			if ( ClassSkillsDatabase.getSkillType( skillID ) == ClassSkillsDatabase.PASSIVE && !(skillID < 10 || (skillID > 14 && skillID < 1000)) )
			{
				loaderScript.append( "document." );
				if ( skillID < 1000 )
					loaderScript.append( "gnome" );
				else if ( skillID < 2000 )
					loaderScript.append( "scpassive" );
				else if ( skillID < 3000 )
					loaderScript.append( "ttpassive" );
				else if ( skillID < 4000 )
					loaderScript.append( "ppassive" );
				else if ( skillID < 5000 )
					loaderScript.append( "spassive" );
				else
					loaderScript.append( "dbpassive" );
				
				loaderScript.append( "." );
				loaderScript.append( skills[i].getSkillName().replaceAll( " ", "" ).toLowerCase() );
				loaderScript.append( ".checked = true; " );
			}
		}
		
		// Also load up the player's current active effects
		// and fill them into the buffs area.
		
		AdventureResult [] effects = new AdventureResult[ KoLCharacter.getEffects().size() ];
		KoLCharacter.getEffects().toArray( effects );
		
		for ( int i = 0; i < effects.length; ++i )
		{			
			String name = UneffectRequest.effectToSkill( effects[i].getName() ).replaceAll( " ", "" ).toLowerCase();
			
			if ( name.indexOf( "snowcone" ) == -1 )
			{
				loaderScript.append( "effect = document.getElementsByName( \"" + name + "\" ); " );
				loaderScript.append( "if ( effect.length > 0 ) effect[0].checked = true; " );
			}
			else
			{
				// This is strongly dependent on the ordering of
				// snowcones on the page, but there really is no
				// other way at this point.
				
				loaderScript.append( "document.snowcones[" );

				if ( name.startsWith( "black" ) )
					loaderScript.append( 1 );
				else if ( name.startsWith( "blue" ) )
					loaderScript.append( 2 );
				else if ( name.startsWith( "red" ) )
					loaderScript.append( 3 );
				else if ( name.startsWith( "orange" ) )
					loaderScript.append( 4 );
				else if ( name.startsWith( "green" ) )
					loaderScript.append( 5 );
				else
					loaderScript.append( 6 );

				loaderScript.append( "].checked = true; " );
			}
		}
		
		// Load up the Rock 'n Roll legend input box
		// with whether or not you have one.
		
		if ( KoLCharacter.getInventory().contains( UseSkillRequest.ROCKNROLL_LEGEND ) )
			loaderScript.append( "document.miscinput.rockandroll.checked = true; " );
		
		// End script.  Everything should be properly
		// selected at this point.

		loaderScript.append( " } " );
		loaderScript.append( "document.getElementsByTagName( \"body\" )[0].onLoad += \"initializeKoLmafiaData(); GoCalc();\"" );
		loaderScript.append( "</script></html>" ); 
		return responseText.replaceFirst( "</html>", loaderScript.toString() );		
	}
	
	protected void submitCommand()
	{
		String command = getFormField( "cmd" );
		if ( command == null )
			return;

		DEFAULT_SHELL.executeLine( command );
		client.enableDisplay();

		pseudoResponse( "HTTP/1.1 200 OK", LocalRelayServer.getNewStatusMessages() );
	}
	
	protected void sendNotFound()
	{	pseudoResponse( "HTTP/1.1 404 Not Found", "" );
	}
	
	public void run()
	{
		if ( formURLString.indexOf( ".gif" ) != -1 )
		{
			sendNotFound();
			return;
		}

		if ( formURLString.indexOf( "KoLmafia" ) == -1 )
		{
			super.run();
			return;
		}

		try
		{
			String specialRequest = formURLString.substring( 9 );
			
			if ( specialRequest.equals( "submitCommand" ) )
				submitCommand();
			else if ( specialRequest.equals( "getNewMessages" ) )
				pseudoResponse( "HTTP/1.1 200 OK", LocalRelayServer.getNewStatusMessages() );
			else
				sendSharedFile( specialRequest );

			// Update the response text with the appropriate
			// information on the relay port.

			if ( fullResponse == null )
				pseudoResponse( "HTTP/1.1 200 OK", "" );
		}
		catch ( Exception e )
		{
			// This should not happen.  Therefore, print
			// a stack trace for debug purposes.
			
			StaticEntity.printStackTrace( e );
		}
		finally
		{
			if ( headers.isEmpty() )
				sendNotFound();
		}
	}
	
	private void downloadSimulatorFile( String filename )
	{
		LocalRelayRequest request = new LocalRelayRequest( client, "http://cif.rochester.edu/~code/kol/" + filename, false );
		request.run();

		File directory = new File( "html/simulator/" );
		directory.mkdirs();

		request.fullResponse = request.fullResponse.replaceAll( "\"images/",
			"\"http://cif.rochester.edu/~code/kol/images/" );

		try
		{
			LogStream writer = new LogStream( "html/simulator/" + filename );
			writer.print( request.fullResponse );
			writer.close();
		}
		catch ( Exception e )
		{
			StaticEntity.printStackTrace( e, "Failed to create cached simulator file." );
		}
	}
}