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

			fullResponse = fullResponse.replaceFirst(
				"<option value=\"inventory\\.php\">Inventory</option>",
				"<option value=\"inventory.php?which=1\">Consumables</option><option value=\"inventory.php?which=2\">Equipment</option><option value=\"inventory.php?which=3\">Miscellaneous</option>" );

			fullResponse = fullResponse.replaceFirst(
				"<option value=\"skills\\.php\">Skills</option>",
				"<option value=\"skills\\.php\">Skills</option><option value=\"familiars.php\">Terrarium</option>" );

			// Remove only the logout option
			// since it might cause problems.

			fullResponse = fullResponse.replaceFirst( "<option value=\"logout\\.php\">Log Out</option>", "" );

			// Mafiatize the goto menu

			fullResponse = fullResponse.replaceFirst(
				"<option value=\"mountains\\.php\">Big Mountains</a>",
				"<option value=\"mclargehuge.php\">Mt. McLargeHuge</option>\n<option value=\"mountains.php\">Big Mountains</option>" );

			fullResponse = fullResponse.replaceFirst(
				"Nearby Plains</a>",
				"Nearby Plains</option><option value=\"beanstalk.php\">Above Beanstalk</option>" );
		}

		// Fix chat javascript problems with relay system

		if ( formURLString.indexOf( "lchat.php" ) != -1 )
		{
			fullResponse = fullResponse.replaceAll( "cycles\\+\\+", "count = 0" ).replaceAll(
				"window.?location.?hostname", "\"127.0.0.1:" + LocalRelayServer.getPort() + "\"" );

			fullResponse = fullResponse.replaceAll(
				"</head>", "<script language=\"Javascript\">base = \"http://127.0.0.1:" +  LocalRelayServer.getPort() + "\";</script></head>" );
		}

		// Fix KoLmafia getting outdated by events happening
		// in the browser by using the sidepane.

		else if ( formURLString.indexOf( "charpane.php") != -1 )
			CharpaneRequest.processCharacterPane( responseText );

		// Fix it a little more by making sure that familiar
		// changes and equipment changes are remembered.

		else
			StaticEntity.externalUpdate( getURLString(), responseText );

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
		this.fullResponse = fullResponse.replaceAll( "<.?--MAFIA_HOST_PORT-->", "127.0.0.1:" + LocalRelayServer.getPort() );
		if ( fullResponse.length() == 0 )
			this.fullResponse = " ";

		headers.clear();
		headers.add( status );
		headers.add( "Date: " + ( new Date() ) );
		headers.add( "Server: " + VERSION_NAME );
		headers.add( "Content-Length: " + this.fullResponse.length() );

		if ( formURLString.endsWith( ".css" ) )
			headers.add( "Content-Type: text/css; charset=UTF-8" );
		else if ( formURLString.endsWith( ".js" ) )
			headers.add( "Content-Type: text/javascript; charset=UTF-8" );
		else
			headers.add( "Content-Type: text/html; charset=UTF-8" );
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

			contentBuffer.append( line );
			contentBuffer.append( LINE_BREAK );
		}

		reader.close();
		return contentBuffer;
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

	private List getAvailableAccessories()
	{
		List available = (List) KoLCharacter.getEquipmentLists()[ KoLCharacter.ACCESSORY1 ].clone();
		if ( !available.contains( KoLCharacter.getEquipment( KoLCharacter.ACCESSORY2 ) ) )
			available.add( KoLCharacter.getEquipment( KoLCharacter.ACCESSORY2 ) );
		if ( !available.contains( KoLCharacter.getEquipment( KoLCharacter.ACCESSORY3 ) ) )
			available.add( KoLCharacter.getEquipment( KoLCharacter.ACCESSORY3 ) );
		return available;
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
			lastIndex = buffer.indexOf( tag );
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

		String familiarEquipment = KoLCharacter.getCurrentEquipmentName( KoLCharacter.FAMILIAR );
		if ( FamiliarData.itemWeightModifier( TradeableItemDatabase.getItemID( familiarEquipment ) ) == 5 )
			replaceTag( scriptBuffer, "/*familiarEquip*/", "familiar-specific +5 lbs." );
		else
			replaceTag( scriptBuffer, "/*familiarEquip*/", familiarEquipment );

		// Change the player's equipment

		replaceTag( scriptBuffer, "/*hat*/", KoLCharacter.getCurrentEquipmentName( KoLCharacter.HAT ) );
		replaceTag( scriptBuffer, "/*weapon*/", KoLCharacter.getCurrentEquipmentName( KoLCharacter.WEAPON ) );
		replaceTag( scriptBuffer, "/*offhand*/", KoLCharacter.getCurrentEquipmentName( KoLCharacter.OFFHAND ) );
		replaceTag( scriptBuffer, "/*shirt*/", KoLCharacter.getCurrentEquipmentName( KoLCharacter.SHIRT ) );
		replaceTag( scriptBuffer, "/*pants*/", KoLCharacter.getCurrentEquipmentName( KoLCharacter.PANTS ) );

		// Change the player's accessories

		replaceTag( scriptBuffer, "/*accessory1*/", KoLCharacter.getCurrentEquipmentName( KoLCharacter.ACCESSORY1 ) );
		replaceTag( scriptBuffer, "/*accessory2*/", KoLCharacter.getCurrentEquipmentName( KoLCharacter.ACCESSORY2 ) );
		replaceTag( scriptBuffer, "/*accessory3*/", KoLCharacter.getCurrentEquipmentName( KoLCharacter.ACCESSORY3 ) );

		// Load up the player's current skillset to figure
		// out what passive skills are available.

		UseSkillRequest [] skills = new UseSkillRequest[ KoLCharacter.getAvailableSkills().size() ];
		KoLCharacter.getAvailableSkills().toArray( skills );

		StringBuffer passiveSkills = new StringBuffer();
		for ( int i = 0; i < skills.length; ++i )
		{
			int skillID = skills[i].getSkillID();
			if ( !( ClassSkillsDatabase.getSkillType( skillID ) == ClassSkillsDatabase.PASSIVE && !(skillID < 10 || (skillID > 14 && skillID < 1000)) ) )
				continue;

			passiveSkills.append( "\t" );
			passiveSkills.append( skills[i].getSkillName().replaceAll( "[' -]", "" ).toLowerCase() );
			passiveSkills.append( "\t" );
		}

		replaceTag( scriptBuffer, "/*passiveSkills*/", passiveSkills.toString() );

		// Also load up the player's current active effects
		// and fill them into the buffs area.

		AdventureResult [] effects = new AdventureResult[ KoLCharacter.getEffects().size() ];
		KoLCharacter.getEffects().toArray( effects );

		String activeEffects = "";
		for ( int i = 0; i < effects.length; ++i )
			activeEffects += "\t" + UneffectRequest.effectToSkill( effects[i].getName() ).replaceAll( "[' -]", "" ).toLowerCase() + "\t";

		replaceTag( scriptBuffer, "/*activeEffects*/", activeEffects );

		if ( KoLCharacter.getInventory().contains( UseSkillRequest.ROCKNROLL_LEGEND ) )
			replaceTag( scriptBuffer, "/*rockAndRoll*/", "true" );
		else
			replaceTag( scriptBuffer, "/*rockAndRoll*/", "false" );

		replaceTag( scriptBuffer, "/*moonPhase*/", (int) ((MoonPhaseDatabase.getGrimacePhase()-1) * 2
			+ Math.round( (MoonPhaseDatabase.getRonaldPhase()-1) / 2.0f - Math.floor( (MoonPhaseDatabase.getRonaldPhase()-1) / 2.0f ) )) );

		replyBuffer.insert( replyBuffer.indexOf( ";GoCalc()" ), ";loadKoLmafiaData()" );
	}

	protected void submitCommand()
	{
		String command = getFormField( "cmd" );
		if ( command == null )
			return;

		KoLmafia.forceContinue();
		DEFAULT_SHELL.executeLine( command );
		KoLmafia.enableDisplay();

		pseudoResponse( "HTTP/1.1 200 OK", LocalRelayServer.getNewStatusMessages() );
	}

	protected void sendNotFound()
	{	pseudoResponse( "HTTP/1.1 404 Not Found", "" );
	}

	public void run()
	{
		if ( formURLString.endsWith( ".gif" ) )
		{
			sendNotFound();
			return;
		}

		try
		{
			if ( formURLString.endsWith( "submitCommand" ) )
				submitCommand();
			else if ( formURLString.endsWith( "getNewMessages" ) )
				pseudoResponse( "HTTP/1.1 200 OK", LocalRelayServer.getNewStatusMessages() );
			else
				sendSharedFile( formURLString );
		}
		catch ( Exception e )
		{
			// This should not happen.  Therefore, print
			// a stack trace for debug purposes.

			StaticEntity.printStackTrace( e );
		}
	}

	private void downloadSimulatorFile( String filename )
	{
		LocalRelayRequest request = new LocalRelayRequest( client, "http://sol.kolmafia.us/" + filename, false );
		request.run();

		File directory = new File( "html/simulator/" );
		directory.mkdirs();

		request.fullResponse = request.fullResponse.replaceAll( "\"images/",
			"\"http://sol.kolmafia.us/images/" );

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