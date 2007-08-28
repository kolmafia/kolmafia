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

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TreeMap;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.kolmafia.StaticEntity.TurnCounter;
import net.java.dev.spellcast.utilities.DataUtilities;

public class LocalRelayRequest extends PasswordHashRequest
{
	private static final TreeMap overrideMap = new TreeMap();
	private static final Pattern EMAIL_PATTERN = Pattern.compile( "<table style='border: 1px solid black;' cellpadding=10>.*?</table>", Pattern.DOTALL );
	private static final Pattern WHITESPACE_PATTERN = Pattern.compile( "['\\s-]" );

	private static final Pattern STORE_PATTERN = Pattern.compile( "<tr><td><input name=whichitem type=radio value=(\\d+).*?</tr>", Pattern.DOTALL );

	private static String mainpane = "";
	private static KoLAdventure lastSafety = null;

	private boolean allowOverride;
	public List headers = new ArrayList();
	public byte [] rawByteBuffer = null;
	public String contentType = null;
	public String statusLine = "HTTP/1.1 302 Found";

	public LocalRelayRequest( boolean allowOverride )
	{
		super( "" );
		this.allowOverride = allowOverride && KoLSettings.getBooleanProperty( "relayAllowsOverride" );
	}

	public KoLRequest constructURLString( String newURLString )
	{
		super.constructURLString( newURLString );

		this.rawByteBuffer = null;
		this.headers.clear();

		if ( this.getPath().endsWith( ".css" ) )
			this.contentType = "text/css";
		else if ( this.getPath().endsWith( ".js" ) )
			this.contentType = "text/javascript";
		else if ( this.getPath().endsWith( ".gif" ) )
			this.contentType = "image/gif";
		else if ( this.getPath().endsWith( ".png" ) )
			this.contentType = "image/png";
		else if ( this.getPath().endsWith( ".jpg" ) || this.getPath().endsWith( ".jpeg" ) )
			this.contentType = "image/jpeg";
		else if ( this.getPath().endsWith( ".ico" ) )
			this.contentType = "image/x-icon";
		else
			this.contentType = "text/html";

		return this;
	}

	private static final boolean isJunkItem( int itemId, int price )
	{
		if ( !KoLSettings.getBooleanProperty( "relayHidesJunkMallItems" ) )
			return false;

		if ( price > KoLCharacter.getAvailableMeat() )
			return true;

		if ( NPCStoreDatabase.contains( TradeableItemDatabase.getItemName( itemId ) ) )
			if ( price == 100 || price > TradeableItemDatabase.getPriceById( itemId ) * 2 )
				return true;

		for ( int i = 0; i < junkList.size(); ++i )
			if ( ((AdventureResult)junkList.get(i)).getItemId() == itemId )
				return true;

		return false;
	}

	protected boolean retryOnTimeout()
	{	return true;
	}

	public void processResults()
	{
		this.statusLine = "HTTP/1.1 200 OK";
		String path = this.getPath();

		StringBuffer responseBuffer = new StringBuffer( this.responseText );

		// Fix KoLmafia getting outdated by events happening
		// in the browser by using the sidepane.

		if ( path.equals( "charpane.php" ) )
		{
			CharpaneRequest.processCharacterPane( this.responseText );
		}

		// Allow a way to get from KoL back to the gCLI
		// using the chat launcher.

		else if ( path.equals( "chatlaunch.php" ) )
		{
			if ( KoLSettings.getBooleanProperty( "relayAddsGraphicalCLI" ) )
			{
				int linkIndex = responseBuffer.indexOf( "<a href" );
				if ( linkIndex != -1 )
					responseBuffer.insert( linkIndex, "<a href=\"cli.html\"><b>KoLmafia gCLI</b></a></center><p>Type KoLmafia scripting commands in your browser!</p><center>" );
			}

			if ( KoLSettings.getBooleanProperty( "relayAddsKoLSimulator" ) )
			{
				int linkIndex = responseBuffer.indexOf( "<a href" );
				if ( linkIndex != -1 )
					responseBuffer.insert( linkIndex, "<a href=\"simulator/index.html\" target=\"_blank\"><b>KoL Simulator</b></a></center><p>See what might happen before it happens!</p><center>" );
			}
		}

		// Fix it a little more by making sure that familiar
		// changes and equipment changes are remembered.

		else if ( path.equals( "main.php" ) )
		{
			Matcher emailMatcher = EMAIL_PATTERN.matcher( this.responseText );
			if ( emailMatcher.find() )
				responseBuffer = new StringBuffer( emailMatcher.replaceAll( "" ) );
		}
		// If this is a store, you can opt to remove all the min-priced items from view
		// along with all the items which are priced above affordable levels.

		else if ( path.equals( "mallstore.php" ) )
		{
			int searchItemId = -1;
			int searchPrice = -1;

			searchItemId = StaticEntity.parseInt( getFormField( "searchitem" ) );
			searchPrice = StaticEntity.parseInt( getFormField( "searchprice" ) );

			Matcher itemMatcher = STORE_PATTERN.matcher( this.responseText );

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
		else if ( path.equals( "fight.php" ) )
		{
			String action = this.getFormField( "action" );
			if ( action != null && action.equals( "skill" ) )
			{
				String skillId = this.getFormField( "whichskill" );
				if ( skillId != null && !skillId.equals( "none" ) && !skillId.equals( "3004" ) )
				{
					String testAction;
					int insertIndex = 6;

					for ( int i = 1; i <= 5 && insertIndex == 6; ++i )
					{
						testAction = KoLSettings.getUserProperty( "stationaryButton" + i );
						if ( testAction.equals( "" ) || testAction.equals( "none" ) || testAction.equals( skillId ) )
							insertIndex = i;
					}

					if ( insertIndex == 6 )
					{
						insertIndex = 5;
						for ( int i = 2; i <= 5; ++i )
						{
							KoLSettings.setUserProperty( "stationaryButton" + (i-1),
								KoLSettings.getUserProperty( "stationaryButton" + i ) );
						}
					}

					KoLSettings.setUserProperty( "stationaryButton" + insertIndex, skillId );
				}
			}
		}

		else
		{
			StaticEntity.externalUpdate( this.getURLString(), this.responseText );
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

		if ( KoLSettings.getBooleanProperty( "relayUsesCachedImages" ) )
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
				if ( header.startsWith( "Content" ) || header.startsWith( "Cache" ) || header.equals( "Pragma" ) )
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

				ostream.println( "Cache-Control: no-cache, must-revalidate" );
				ostream.println( "Pragma: no-cache" );
			}
		}
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

		this.headers.add( "Connection: close" );
	}

	private StringBuffer readContents( BufferedReader reader )
	{
		String line = null;
		StringBuffer contentBuffer = new StringBuffer();

		try
		{
			if ( reader == null )
				return contentBuffer;

			while ( (line = reader.readLine()) != null )
			{
				contentBuffer.append( line );
				contentBuffer.append( LINE_BREAK );
			}

			reader.close();
		}
		catch ( IOException e )
		{
		}

		return contentBuffer;
	}

	private void sendLocalImage( String filename )
	{
		try
		{
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
		catch ( Exception e )
		{
			this.sendNotFound();
		}
	}

	public static final void setNextMain( String mainpane )
	{	LocalRelayRequest.mainpane = mainpane;
	}

	private void handleMain()
	{
		if ( this.responseText == null )
			super.run();

		if ( mainpane.equals( "" ) )
			return;

		// If it's a relay browser request based on a
		// menu item, then change the middle panel.

		this.responseText = MAINPANE_PATTERN.matcher( this.responseText ).replaceFirst(
			"name=mainpane src=\"" + mainpane + "\"" );

		mainpane = "";
	}

	private static final Pattern MAINPANE_PATTERN = Pattern.compile( "name=mainpane src=\"(.*?)\"", Pattern.DOTALL );

	private void sendLocalFile( String filename )
	{
		StringBuffer replyBuffer = new StringBuffer();

		if ( !overrideMap.containsKey( filename ) )
			overrideMap.put( filename, new File( RELAY_DIRECTORY, filename ) );

		File override = (File) overrideMap.get( filename );
		if ( !override.exists() )
		{
			if ( filename.equals( "main.html" ) || filename.equals( "main_c.html" ) )
			{
				this.handleMain();
				return;
			}

			if ( !filename.startsWith( "simulator/" ) )
			{
				this.sendNotFound();
				return;
			}

			this.downloadSimulatorFile( filename.substring( filename.lastIndexOf( "/" ) + 1 ) );
		}

		replyBuffer = this.readContents( DataUtilities.getReader( override ) );

		// Add Javascript to the KoL simulator pages so that
		// everything loads correctly the first time.

		if ( filename.endsWith( "simulator/index.html" ) )
			this.handleSimulatorIndex( replyBuffer );

		// Make sure to print the reply buffer to the
		// response buffer for the local relay server.

		if ( this.isChatRequest )
			StaticEntity.globalStringReplace( replyBuffer, "<br>", "</font><br>" );

		if ( filename.endsWith( "chat.html" ) )
			RequestEditorKit.addChatFeatures( replyBuffer );

		this.pseudoResponse( "HTTP/1.1 200 OK", replyBuffer.toString() );

		if ( filename.equals( "main.html" ) || filename.equals( "main_c.html" ) )
		{
			this.handleMain();
			return;
		}
	}

	private static final String getSimulatorEffectName( String effectName )
	{	return WHITESPACE_PATTERN.matcher( effectName ).replaceAll( "" ).toLowerCase();
	}

	private static final String getSimulatorEquipName( int equipmentSlot )
	{
		AdventureResult item = KoLCharacter.getEquipment( equipmentSlot );

		if ( equipmentSlot == KoLCharacter.FAMILIAR && item.getName().equals( FamiliarsDatabase.getFamiliarItem( KoLCharacter.getFamiliar().getId() ) ) )
			return "familiar-specific +5 lbs.";

		if ( item == EquipmentRequest.UNEQUIP )
			return "(None)";

		return item.getName();
	}

	private void handleSimulatorIndex( StringBuffer replyBuffer )
	{
		StringBuffer scriptBuffer = new StringBuffer();
		scriptBuffer.append( LINE_BREAK );

		scriptBuffer.append( "<script language=\"Javascript\">" );
		scriptBuffer.append( LINE_BREAK );
		scriptBuffer.append( LINE_BREAK );
		scriptBuffer.append( "LoadKingdomStateLong( { " );


		// This is the simple Javascript which can be added
		// arbitrarily to the end without having to modify
		// the underlying HTML.

		int classIndex = -1;
		for ( int i = 0; i < KoLmafiaASH.CLASSES.length; ++i )
			if ( KoLmafiaASH.CLASSES[i].equalsIgnoreCase( KoLCharacter.getClassType() ) )
				classIndex = i;

		// Player state

		scriptBuffer.append( "charclass: " );
		scriptBuffer.append( classIndex );
		scriptBuffer.append( ", mus: " );
		scriptBuffer.append( KoLCharacter.getBaseMuscle() );
		scriptBuffer.append( ", mys: " );
		scriptBuffer.append( KoLCharacter.getBaseMysticality() );
		scriptBuffer.append( ", mox: " );
		scriptBuffer.append( KoLCharacter.getBaseMoxie() );
		scriptBuffer.append( ", " );

		// Current equipment

		scriptBuffer.append( "hat: \"" );
		scriptBuffer.append( getSimulatorEquipName( KoLCharacter.HAT ) );
		scriptBuffer.append( "\", weapon: \"" );
		scriptBuffer.append( getSimulatorEquipName( KoLCharacter.WEAPON ) );
		scriptBuffer.append( "\", offhand: \"" );
		scriptBuffer.append( getSimulatorEquipName( KoLCharacter.OFFHAND ) );
		scriptBuffer.append( "\", shirt: \"" );
		scriptBuffer.append( getSimulatorEquipName( KoLCharacter.SHIRT ) );
		scriptBuffer.append( "\", pants: \"" );
		scriptBuffer.append( getSimulatorEquipName( KoLCharacter.PANTS ) );
		scriptBuffer.append( "\", acc1: \"" );
		scriptBuffer.append( getSimulatorEquipName( KoLCharacter.ACCESSORY1 ) );
		scriptBuffer.append( "\", acc2: \"" );
		scriptBuffer.append( getSimulatorEquipName( KoLCharacter.ACCESSORY2 ) );
		scriptBuffer.append( "\", acc3: \"" );
		scriptBuffer.append( getSimulatorEquipName( KoLCharacter.ACCESSORY3 ) );
		scriptBuffer.append( "\", " );

		// Current familiar

		scriptBuffer.append( "familiar: \"" );
		scriptBuffer.append( KoLCharacter.getFamiliar().getRace() );
		scriptBuffer.append( "\", weight: " );
		scriptBuffer.append( KoLCharacter.getFamiliar().getWeight() );
		scriptBuffer.append( ", fameq: \"" );

		String familiarEquipment = getSimulatorEquipName( KoLCharacter.FAMILIAR );
		if ( FamiliarData.itemWeightModifier( TradeableItemDatabase.getItemId( familiarEquipment ) ) == 5 )
			scriptBuffer.append( "familiar-specific +5 lbs." );
		else
			scriptBuffer.append( familiarEquipment );

		// Status effects

		scriptBuffer.append( "\", rockandroll: false, effects: [ " );
		int effectCount = 0;

		for ( int i = 0; i < availableSkills.size(); ++i )
		{
			UseSkillRequest current = (UseSkillRequest) availableSkills.get(i);
			int skillId = current.getSkillId();

			if ( ClassSkillsDatabase.getSkillType( skillId ) != ClassSkillsDatabase.PASSIVE )
				continue;

			if ( effectCount > 0 )
				scriptBuffer.append( ',' );

			++effectCount;
			scriptBuffer.append( '\"' );
			scriptBuffer.append( getSimulatorEffectName( current.getSkillName() ) );
			scriptBuffer.append( '\"' );
		}

		for ( int i = 0; i < activeEffects.size(); ++i )
		{
			AdventureResult current = (AdventureResult) activeEffects.get(i);
			if ( effectCount > 0 )
				scriptBuffer.append( ',' );

			++effectCount;
			scriptBuffer.append( '\"' );
			scriptBuffer.append( getSimulatorEffectName( current.getName() ) );
			scriptBuffer.append( '\"' );
		}

		scriptBuffer.append( " ], " );

		// Adventure modifiers

		scriptBuffer.append( "zone: \"" );
		scriptBuffer.append( KoLSettings.getUserProperty( "lastAdventure" ) );
		scriptBuffer.append( "\", monster: \"" );
		scriptBuffer.append( FightRequest.getCurrentKey() );
		scriptBuffer.append( "\", mcd: " );
		scriptBuffer.append( KoLCharacter.getMindControlLevel() );
		scriptBuffer.append( ", " );

		scriptBuffer.append( "moonphase: " );
		scriptBuffer.append( (int) ((MoonPhaseDatabase.getGrimacePhase()-1) * 2
			+ Math.round( (MoonPhaseDatabase.getRonaldPhase()-1) / 2.0f - Math.floor( (MoonPhaseDatabase.getRonaldPhase()-1) / 2.0f ) )) );

		scriptBuffer.append( ", moonminiphase: " );
		scriptBuffer.append( MoonPhaseDatabase.getHamburglarPosition( new Date() ) );
		scriptBuffer.append( " } );" );

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

	public void sendNotFound()
	{
		this.pseudoResponse( "HTTP/1.1 404 Not Found", "" );
		this.responseCode = 404;
	}

	public void sendBossWarning( String name, String image, int mcd1, String item1, int mcd2, String item2 )
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

	private void handleSafety()
	{
		if ( lastSafety == null )
		{
			this.pseudoResponse( "HTTP/1.1 200 OK", "" );
			return;
		}

		AreaCombatData combat = lastSafety.getAreaSummary();

		if ( combat == null )
		{
			this.pseudoResponse( "HTTP/1.1 200 OK", "" );
			return;
		}

		StringBuffer buffer = new StringBuffer();
		buffer.append( "<table width=\"100%\"><tr><td align=left valign=top><font size=3>" );
		buffer.append( lastSafety.getAdventureName() );
		buffer.append( "</font></td><td align=right valign=top><font size=2>" );

		buffer.append( "<a style=\"text-decoration: none\" href=\"javascript: " );

		buffer.append( "var safety; if ( document.getElementById ) " );
			buffer.append( "safety = top.chatpane.document.getElementById( 'safety' ); " );
		buffer.append( "else if ( document.all ) " );
			buffer.append( "safety = top.chatpane.document.all[ 'safety' ]; " );

		buffer.append( "safety.closed = true;" );
		buffer.append( "safety.style.display = 'none'; " );
		buffer.append( "var nodes = top.chatpane.document.body.childNodes; " );
		buffer.append( "for ( var i = 0; i < nodes.length; ++i ) " );
			buffer.append( "if ( nodes[i].style && nodes[i].id != 'safety' ) " );
				buffer.append( "nodes[i].style.display = 'inline'; " );


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
		// None of the above checks wound up happening.  So, do some
		// special handling, catching any exceptions that happen to
		// popup along the way.

		if ( this.getPath().endsWith( "submitCommand" ) )
		{
			CommandDisplayFrame.executeCommand( this.getFormField( "cmd" ) );
			this.pseudoResponse( "HTTP/1.1 200 OK", "" );
		}
		else if ( this.getPath().endsWith( "executeCommand" ) )
		{
			CommandDisplayFrame.executeCommand( this.getFormField( "cmd" ) );
			this.pseudoResponse( "HTTP/1.1 200 OK", "" );
		}
		else if ( this.getPath().endsWith( "sideCommand" ) )
		{
			CommandDisplayFrame.executeCommand( this.getFormField( "cmd" ) );
			while ( CommandDisplayFrame.hasQueuedCommands() )
				delay( 500 );

			this.pseudoResponse( "HTTP/1.1 302 Found", "/charpane.php" );
		}
		else if ( this.getPath().endsWith( "messageUpdate" ) )
		{
			this.pseudoResponse( "HTTP/1.1 200 OK", LocalRelayServer.getNewStatusMessages() );
		}
		else if ( this.getPath().endsWith( "lookupLocation" ) )
		{
			lastSafety = AdventureDatabase.getAdventureByURL( "adventure.php?snarfblat=" + this.getFormField( "snarfblat" ) );
			AdventureFrame.updateSelectedAdventure( lastSafety );
			this.handleSafety();
		}
		else if ( this.getPath().endsWith( "updateLocation" ) )
		{
			this.handleSafety();
		}
		else if ( this.getPath().endsWith( "basementSpoiler" ) )
		{
			BasementRequest.reCheckBasement();
			this.pseudoResponse( "HTTP/1.1 200 OK", BasementRequest.getRequirement() );
		}
		else
		{
			this.pseudoResponse( "HTTP/1.1 200 OK", "" );
		}
	}

	private void handleChat()
	{
		// If you are in chat, and the person submitted a command
		// via chat, check to see if it's a CLI command.  Otherwise,
		// run it as normal.

		String chatResponse = ChatRequest.executeChatCommand( this.getFormField( "graf" ) );
		if ( chatResponse != null )
		{
			this.pseudoResponse( "HTTP/1.1 200 OK",
				"<font color=\"blue\"><b><a target=\"mainpane\" href=\"showplayer.php?who=458968\" style=\"color:blue\">" +
				VERSION_NAME + "</a> (private)</b>: " + chatResponse + "</font><br>" );

			return;
		}

		super.run();

		if ( !KoLMessenger.isRunning() || this.getPath().indexOf( "submitnewchat.php" ) != -1 )
			KoLMessenger.updateChat( this.responseText );

		if ( KoLSettings.getBooleanProperty( "relayFormatsChatText" ) )
			this.responseText = KoLMessenger.getNormalizedContent( this.responseText, false );

	}

	public void handleSimple()
	{
		// If there is an attempt to view the error page, or if
		// there is an attempt to view the robots file, neither
		// are available on KoL, so return.

		if ( this.getPath().equals( "missingimage.gif" ) || this.getPath().endsWith( "robots.txt" ) || this.getPath().endsWith( "favicon.ico" ) )
		{
			this.sendNotFound();
			return;
		}

		// If this is a command from the browser, handle it before
		// moving on to anything else.

		if ( this.getPath().startsWith( "KoLmafia/" ) )
		{
			this.handleCommand();
			return;
		}

		// Check to see if it's a request from the local images folder.
		// If it is, go ahead and send it.

		if ( this.getPath().startsWith( "images/playerpics/" ) )
		{
			RequestEditorKit.downloadImage( "http://pics.communityofloathing.com/albums/" +
				this.getPath().substring( this.getPath().indexOf( "playerpics" ) ) );

			this.sendLocalImage( this.getPath() );
			return;
		}

		if ( this.getPath().startsWith( "images/" ) )
		{
			this.sendLocalImage( this.getPath() );
			return;
		}

		if ( this.getPath().endsWith( ".css" ) || this.getPath().endsWith( ".js" ) )
		{
			this.sendLocalFile( this.getPath() );
			return;
		}

		// If it's an ASH override script, make sure to handle it
		// exactly as it should.

		if ( this.getPath().endsWith( ".ash" ) )
		{
			if ( !KoLmafiaASH.getClientHTML( this ) )
				this.sendNotFound();

			return;
		}

		// HTML files never have form fields.  Remove them, because
		// they're probably just used for data tracking purposes
		// client-side.

		if ( this.getPath().endsWith( ".html" ) )
		{
			this.data.clear();
			this.sendLocalFile( this.getPath() );
			return;
		}

		this.sendNotFound();
	}

	public void run()
	{
		if ( this.getPath().startsWith( "http" ) )
		{
			super.run();
			return;
		}

		if ( !this.getPath().endsWith( ".php" ) )
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

		if ( allowOverride && KoLmafiaASH.getClientHTML( this ) )
			return;

		if ( this.getPath().equals( "lchat.php" ) )
		{
			if ( KoLSettings.getBooleanProperty( "relayUsesIntegratedChat" ) )
			{
				this.sendLocalFile( "chat.html" );
				return;
			}

			super.run();

			this.responseText = StaticEntity.globalStringReplace( this.responseText, "<p>", "<br><br>" );
			this.responseText = StaticEntity.globalStringReplace( this.responseText, "<P>", "<br><br>" );
			this.responseText = StaticEntity.singleStringDelete( this.responseText, "</span>" );

			return;
		}

		// Load custom items from OneTonTomato's script if they
		// are currently being requested.

		if ( this.getPath().equals( "desc_item.php" ) )
		{
			String item = this.getFormField( "whichitem" );
			if ( item != null && item.startsWith( "custom" ) )
			{
				this.pseudoResponse( "HTTP/1.1 200 OK", CustomItemDatabase.retrieveCustomItem( item.substring(6) ) );
				return;
			}

			if ( KoLSettings.getBooleanProperty( "relayAddsWikiLinks" ) )
			{
				String location = ShowDescriptionList.getWikiLocation( TradeableItemDatabase.getItemName( item ) );
				if ( location != null )
				{
					this.pseudoResponse( "HTTP/1.1 302 Found", location );
					return;
				}
			}
		}

		if ( this.getPath().equals( "desc_effect.php" ) && KoLSettings.getBooleanProperty( "relayAddsWikiLinks" ) )
		{
			String effect = this.getFormField( "whicheffect" );
			String location = ShowDescriptionList.getWikiLocation( new AdventureResult( StatusEffectDatabase.getEffectName( effect ), 1, true ) );

			if ( location != null )
			{
				this.pseudoResponse( "HTTP/1.1 302 Found", location );
				return;
			}
		}

		if ( AdventureDatabase.getAdventureByURL( this.getURLString() ) != null )
		{
			TurnCounter expired = StaticEntity.getExpiredCounter(
				this.getPath().equals( "adventure.php" ) ? this.getFormField( "snarfblat" ) : "" );

			if ( expired != null )
			{
				this.sendGeneralWarning( expired.getImage(),
					"The indicated counter has expired, so may wish to adventure somewhere else at this time.  " +
					"If you are certain that this is where you'd like to adventure, click <a href=\"" + this.getURLString() + "\">here</a> to proceed." );

				return;
			}

			// Special handling of adventuring locations before it's
			// registered internally with KoLmafia.

			if ( this.getPath().equals( "adventure.php" ) )
			{
				String location = this.getFormField( "snarfblat" );
				if ( location == null )
					location = this.getFormField( "adv" );

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

			else if ( this.getPath().equals( "knob.php" ) )
			{
				if ( this.getFormField( "king" ) != null && this.getFormField( "override" ) == null )
				{
					this.sendBossWarning( "Knob Goblin King", "goblinking.gif", 3, "glassballs.gif", 7, "batcape.gif" );
					return;
				}
			}

			// More MCD rewards.  This one is for the Bonerdagon, who has
			// special items at 5 and 10.

			else if ( this.getPath().equals( "cyrpt.php" ) )
			{
				if ( this.getFormField( "action" ) != null && this.getFormField( "override" ) == null )
				{
					this.sendBossWarning( "Bonerdagon", "bonedragon.gif", 5, "rib.gif", 10, "vertebra.gif" );
					return;
				}
			}
		}

		// If the person is visiting the sorceress and they forgot
		// to make the Wand, remind them.

		else if ( this.getPath().equals( "lair6.php" ) )
		{
			// As of NS 13,they need not have it equipped. In fact,
			// there are far better weapons to equip for the
			// battle. But, just in case, check current equipment
			// as well as inventory.

			if ( this.getFormField( "place" ) != null && this.getFormField( "place" ).equals( "5" ) &&
				this.getFormField( "override" ) == null && !KoLCharacter.hasEquipped( SorceressLair.NAGAMAR ) &&
				!AdventureDatabase.retrieveItem( SorceressLair.NAGAMAR ) )
			{
				this.sendGeneralWarning( "wand.gif", "It's possible there is something very important you're forgetting to do." );
				return;
			}
		}

		else if ( this.getPath().equals( "ascend.php" ) )
		{
			if ( this.getFormField( "action" ) != null )
				RequestThread.postRequest( new EquipmentRequest( SpecialOutfit.BIRTHDAY_SUIT ) );
		}

		// If it gets this far, it's a normal file.  Go ahead and
		// process it accordingly.

		super.run();

		if ( this.responseCode == 302 )
			this.pseudoResponse( "HTTP/1.1 302 Found", this.redirectLocation );
		else if ( this.responseCode != 200 )
			this.sendNotFound();

		return;
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

			File directory = new File( RELAY_LOCATION, "simulator" );
			directory.mkdirs();

			StaticEntity.globalStringReplace( contents, "images/", "http://sol.kolmafia.us/images/" );

			PrintStream writer = RequestLogger.openStream( "relay/simulator/" + filename, NullStream.INSTANCE, true );
			writer.println( contents.toString() );
			writer.close();
		}
		catch ( Exception e )
		{
			KoLmafia.updateDisplay( ERROR_STATE, "Failed to create simulator file <" + filename + ">" );
		}
	}
}
