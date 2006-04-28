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
import java.io.BufferedReader;
import java.io.IOException;

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
			fullResponse = fullResponse.replaceAll( "<script.*?></script>", "" );
			fullResponse = fullResponse.replaceFirst( "<a href",
				"<a href=\"KoLmafia/cli.html\"><b>KoLmafia gCLI</b></a></center><p>NOTE: The graphical CLI will load in this frame to allow for manual adventuring.</p><center><a href");
		}
		
		// Now, for a little fun HTML manipulation.  See
		// if there's an item present, and if so, modify
		// it so that you get a use link.
		
		StringBuffer linkedResponse = new StringBuffer();
		Matcher useLinkMatcher = Pattern.compile( "You acquire(.*?)</td>" ).matcher( fullResponse );

		while ( useLinkMatcher.find() )
		{
			String itemName = useLinkMatcher.group(1);
			int itemCount = itemName.indexOf( ":" ) != -1 ? 1 : 2;

			if ( itemCount == 1 )
				itemName = itemName.substring( itemName.indexOf( ":" ) + 1 ).replaceAll( "<.*?>", "" ).trim();
			else
			{
				itemName = itemName.replaceAll( "<.*?>", "" );
				itemName = itemName.substring( itemName.indexOf( " " ) + 1 ).trim();
			}

			int itemID = TradeableItemDatabase.getItemID( itemName, itemCount );
			String useType = null;
			String useLocation = null;

			switch ( TradeableItemDatabase.getConsumptionType( itemID ) )
			{
				case ConsumeItemRequest.CONSUME_EAT:
					useType = KoLCharacter.canEat() ? "eat" : null;
					useLocation = "inv_eat.php?pwd=&which=1&whichitem=";
					break;

				case ConsumeItemRequest.CONSUME_DRINK:
					useType = KoLCharacter.canDrink() ? "drink" : null;
					useLocation = "inv_booze.php?pwd=&which=1&whichitem=";
					break;

				case ConsumeItemRequest.CONSUME_MULTIPLE:
					useType = "use";
					useLocation = itemCount != 1 ? "multiuse.php?passitem=" : "inv_use.php?pwd=&which=1&whichitem=";
					break;

				case ConsumeItemRequest.CONSUME_RESTORE:
					useType = "skills";
					useLocation = "skills.php";
					break;

				case ConsumeItemRequest.CONSUME_USE:
					useType = "use";
					useLocation = "inv_use.php?pwd=&which=3&whichitem=";

				case ConsumeItemRequest.EQUIP_HAT:
				case ConsumeItemRequest.EQUIP_PANTS:
				case ConsumeItemRequest.EQUIP_SHIRT:
					useType = "equip";
					useLocation = "inv_equip.php?pwd=&which=2&action=equip&whichitem=";
					break;
			}
			
			if ( useType != null && useLocation != null )
			{
				useLinkMatcher.appendReplacement( linkedResponse,
					"You acquire$1 [<font size=1><a href=\"" + useLocation.toString() + itemID + "\">" + useType + "</a>]</font></td>" );
			}
			else
			{
				useLinkMatcher.appendReplacement( linkedResponse, "$0" );
			}
		}

		useLinkMatcher.appendTail( linkedResponse );
		fullResponse = linkedResponse.toString();
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
		StringBuffer replyBuffer = new StringBuffer();
		BufferedReader reader = DataUtilities.getReader( "html", filename );

		if ( reader == null )
		{
			sendNotFound();
			return;
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
		pseudoResponse( "HTTP/1.1 200 OK", replyBuffer.toString() );		
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
}