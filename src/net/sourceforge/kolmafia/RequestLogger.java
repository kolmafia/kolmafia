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

import java.io.PrintStream;
import java.util.Date;

public class RequestLogger extends NullStream implements KoLConstants
{
	public static final RequestLogger INSTANCE = new RequestLogger();

	private static PrintStream outputStream = NullStream.INSTANCE;
	private static PrintStream mirrorStream = NullStream.INSTANCE;

	private static PrintStream sessionStream = NullStream.INSTANCE;
	private static PrintStream debugStream = NullStream.INSTANCE;

	private static String previousUpdateString = "";
	private static boolean wasLastRequestSimple = false;

	private RequestLogger()
	{
	}

	public void println()
	{	printLine();
	}

	public void println( String line )
	{	printLine( line );
	}

	public static void printLine()
	{	printLine( CONTINUE_STATE, "", true );
	}

	public static void printLine( String message )
	{	printLine( CONTINUE_STATE, message, true );
	}

	public static void printLine( String message, boolean addToBuffer )
	{	printLine( CONTINUE_STATE, message, addToBuffer );
	}

	public static void printLine( int state, String message )
	{	printLine( state, message, true );
	}

	public static void printLine( int state, String message, boolean addToBuffer )
	{
		if ( message == null )
			return;

		message = message.trim();

		if ( message.length() == 0 && previousUpdateString.length() == 0 )
			return;

		previousUpdateString = message;

		outputStream.println( message );
		mirrorStream.println( message );
		debugStream.println( message );

		if ( !addToBuffer )
			return;

		StringBuffer colorBuffer = new StringBuffer();

		if ( message.equals( "" ) )
		{
			colorBuffer.append( "<br>" );
		}
		else
		{
			boolean addedColor = false;

			if ( state == ERROR_STATE || state == ABORT_STATE )
			{
				addedColor = true;
				colorBuffer.append( "<font color=red>" );
			}
			else if ( message.startsWith( "> QUEUED" ) )
			{
				addedColor = true;
				colorBuffer.append( " <font color=olive><b>" );
			}
			else if ( message.startsWith( "> " ) )
			{
				addedColor = true;
				colorBuffer.append( " <font color=olive>" );
			}

			colorBuffer.append( StaticEntity.globalStringReplace( message, "\n", "<br>" ) );

			if ( message.startsWith( "> QUEUED" ) )
				colorBuffer.append( "</b>" );

			if ( addedColor )
				colorBuffer.append( "</font><br>" );
			else
				colorBuffer.append( "<br>" );

			if ( message.indexOf( "<" ) == -1 && message.indexOf( LINE_BREAK ) != -1 )
				colorBuffer.append( "</pre>" );

			StaticEntity.globalStringDelete( colorBuffer, "<html>" );
			StaticEntity.globalStringDelete( colorBuffer, "</html>" );
		}

		colorBuffer.append( LINE_BREAK );
		commandBuffer.append( colorBuffer.toString() );
		LocalRelayServer.addStatusMessage( colorBuffer.toString() );
	}

	public static final PrintStream openStream( String filename, PrintStream originalStream, boolean hasLocation )
	{
		if ( !hasLocation && KoLCharacter.getUserName().equals( "" ) )
			return NullStream.INSTANCE;

		// Before doing anything, be sure to close the
		// original stream.

		if ( !(originalStream instanceof NullStream) )
		{
			if ( hasLocation )
				return originalStream;

			originalStream.close();
		}

		return LogStream.openStream( filename, false );
	}

	public static void openStandard()
	{	outputStream = System.out;
	}

	public static void openMirror( String location )
	{	mirrorStream = openStream( location, mirrorStream, true );
	}

	public static void closeMirror()
	{
		mirrorStream.close();
		mirrorStream = NullStream.INSTANCE;
	}

	public static PrintStream getSessionStream()
	{	return sessionStream;
	}

	public static void openSessionLog()
	{
		sessionStream = openStream( SESSIONS_DIRECTORY + StaticEntity.globalStringReplace( KoLCharacter.getUserName(), " ", "_" ) + "_" +
			DATED_FILENAME_FORMAT.format( new Date() ) + ".txt", sessionStream, false );
	}

	public static void closeSessionLog()
	{
		sessionStream.close();
		sessionStream = NullStream.INSTANCE;
	}

	public static void updateSessionLog()
	{	sessionStream.println();
	}

	public static void updateSessionLog( String line )
	{	sessionStream.println( line );
	}

	public static boolean isDebugging()
	{	return debugStream != NullStream.INSTANCE;
	}

	public static PrintStream getDebugStream()
	{	return debugStream;
	}

	public static void openDebugLog()
	{	debugStream = openStream( "DEBUG.txt", debugStream, true );
	}

	public static void closeDebugLog()
	{
		debugStream.close();
		debugStream = NullStream.INSTANCE;
	}

	public static void updateDebugLog()
	{	debugStream.println();
	}

	public static void updateDebugLog( String line )
	{	debugStream.println( line );
	}

	public static void updateDebugLog( Throwable t )
	{	t.printStackTrace( debugStream );
	}

	public static void updateDebugLog( Object o )
	{	debugStream.println( o.toString() );
	}

	public static void registerRequest( KoLRequest request, String urlString )
	{
		try
		{
			if ( BuffBotHome.isBuffBotActive() )
				return;

			doRegister( request, urlString );
		}
		catch ( Exception e )
		{
			StaticEntity.printStackTrace( e );
		}
	}

	private static void doRegister( KoLRequest request, String urlString )
	{
		if ( urlString.startsWith( "council" ) )
		{
			StaticEntity.setProperty( "lastCouncilVisit", String.valueOf( KoLCharacter.getLevel() ) );
			return;
		}

		boolean isExternal = request.getClass() == KoLRequest.class || request instanceof LocalRelayRequest;

		// There are some adventures which do not post any
		// form fields, so handle them first.

		if ( KoLAdventure.recordToSession( urlString ) )
		{
			wasLastRequestSimple = false;
			return;
		}

		if ( (request instanceof FightRequest || isExternal) && FightRequest.registerRequest( urlString ) )
		{
			wasLastRequestSimple = false;
			return;
		}

		// Anything else that doesn't submit an actual form
		// should not be registered.

		if ( urlString.indexOf( "?" ) == -1 )
			return;

		// Some general URLs which never need to be registered
		// because they don't do anything.

		if ( urlString.startsWith( "choice" ) )
		{
			updateSessionLog( urlString );
			return;
		}

		if ( urlString.startsWith( "login" ) || urlString.startsWith( "logout" ) || urlString.startsWith( "charpane" ) )
			return;

		if ( urlString.startsWith( "leaflet" ) || urlString.startsWith( "cave" ) || urlString.startsWith( "lair" ) || urlString.startsWith( "campground" ) )
			return;

		if ( urlString.startsWith( "inventory.php?which" ) || urlString.equals( "knoll.php?place=paster" ) || urlString.equals( "town_right.php?place=untinker" ) || urlString.startsWith( "clan_rumpus" ) )
			return;

		// The following lists all the remaining requests in
		// alphabetical order.

		if ( (request instanceof AutoSellRequest || isExternal) && AutoSellRequest.registerRequest( urlString ) )
		{
			wasLastRequestSimple = false;
			return;
		}

		if ( (request instanceof ClanStashRequest || isExternal) && ClanStashRequest.registerRequest( urlString ) )
		{
			wasLastRequestSimple = false;
			return;
		}

		if ( (request instanceof ConsumeItemRequest || isExternal) && ConsumeItemRequest.registerRequest( urlString ) )
		{
			wasLastRequestSimple = false;
			return;
		}

		if ( (request instanceof EquipmentRequest || isExternal) && EquipmentRequest.registerRequest( urlString ) )
		{
			wasLastRequestSimple = false;
			return;
		}

		if ( (request instanceof FamiliarRequest || isExternal) && FamiliarRequest.registerRequest( urlString ) )
		{
			wasLastRequestSimple = false;
			return;
		}

		if ( (request instanceof FlowerHunterRequest || isExternal) && FlowerHunterRequest.registerRequest( urlString ) )
		{
			wasLastRequestSimple = false;
			return;
		}

		if ( (request instanceof GiftMessageRequest || isExternal) && GiftMessageRequest.registerRequest( urlString ) )
		{
			wasLastRequestSimple = false;
			return;
		}

		if ( (request instanceof GreenMessageRequest || isExternal) && GreenMessageRequest.registerRequest( urlString ) )
		{
			wasLastRequestSimple = false;
			return;
		}

		if ( request instanceof ItemCreationRequest )
		{
			ItemCreationRequest irequest = (ItemCreationRequest) request;

			updateSessionLog();

			if ( irequest.getAdventuresUsed() == 0 )
				updateSessionLog( "make " + irequest.getQuantityNeeded() + " " + irequest.getName() );
			else
				updateSessionLog( "[" + KoLAdventure.getAdventureCount() + "] Create " + irequest.getQuantityNeeded() + " " + irequest.getName() );

			wasLastRequestSimple = false;
			return;
		}

		if ( isExternal && ItemCreationRequest.registerRequest( urlString ) )
		{
			wasLastRequestSimple = false;
			return;
		}

		if ( (request instanceof ItemStorageRequest || isExternal) && ItemStorageRequest.registerRequest( urlString ) )
		{
			wasLastRequestSimple = false;
			return;
		}

		if ( (request instanceof MallPurchaseRequest || isExternal) && MallPurchaseRequest.registerRequest( urlString ) )
		{
			wasLastRequestSimple = false;
			return;
		}

		if ( (request instanceof MicrobreweryRequest || isExternal) && MicrobreweryRequest.registerRequest( urlString ) )
		{
			wasLastRequestSimple = false;
			return;
		}

		if ( (request instanceof MuseumRequest || isExternal) && MuseumRequest.registerRequest( urlString ) )
		{
			wasLastRequestSimple = false;
			return;
		}

		if ( (request instanceof ProposeTradeRequest || isExternal) && ProposeTradeRequest.registerRequest( urlString ) )
		{
			wasLastRequestSimple = false;
			return;
		}

		if ( (request instanceof PulverizeRequest || isExternal) && PulverizeRequest.registerRequest( urlString ) )
		{
			wasLastRequestSimple = false;
			return;
		}

		if ( (request instanceof RestaurantRequest || isExternal) && RestaurantRequest.registerRequest( urlString ) )
		{
			wasLastRequestSimple = false;
			return;
		}

		if ( (request instanceof UntinkerRequest || isExternal) && UntinkerRequest.registerRequest( urlString ) )
		{
			wasLastRequestSimple = false;
			return;
		}

		if ( (request instanceof UseSkillRequest || isExternal) && UseSkillRequest.registerRequest( urlString ) )
		{
			wasLastRequestSimple = false;
			return;
		}

		// Otherwise, make sure to print the raw URL so that it's
		// at least mentioned in the session log.

		if ( !wasLastRequestSimple )
			updateSessionLog();

		wasLastRequestSimple = true;
		updateSessionLog( urlString );
	}
}