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

public class RequestLogger
	extends NullStream
	implements KoLConstants
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
	{
		RequestLogger.printLine();
	}

	public void println( final String line )
	{
		RequestLogger.printLine( line );
	}

	public static final void printLine()
	{
		RequestLogger.printLine( KoLConstants.CONTINUE_STATE, "", true );
	}

	public static final void printLine( final String message )
	{
		RequestLogger.printLine( KoLConstants.CONTINUE_STATE, message, true );
	}

	public static final void printLine( final String message, final boolean addToBuffer )
	{
		RequestLogger.printLine( KoLConstants.CONTINUE_STATE, message, addToBuffer );
	}

	public static final void printLine( final int state, final String message )
	{
		RequestLogger.printLine( state, message, true );
	}

	public static final void printLine( final int state, String message, boolean addToBuffer )
	{
		if ( message == null )
		{
			return;
		}

		message = message.trim();

		if ( message.length() == 0 && RequestLogger.previousUpdateString.length() == 0 )
		{
			return;
		}

		RequestLogger.previousUpdateString = message;

		RequestLogger.outputStream.println( message );
		RequestLogger.mirrorStream.println( message );
		RequestLogger.debugStream.println( message );

		if ( !addToBuffer )
		{
			return;
		}

		StringBuffer colorBuffer = new StringBuffer();

		if ( message.equals( "" ) )
		{
			colorBuffer.append( "<br>" );
		}
		else
		{
			boolean addedColor = false;

			if ( state == KoLConstants.ERROR_STATE || state == KoLConstants.ABORT_STATE )
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
			{
				colorBuffer.append( "</b>" );
			}

			if ( addedColor )
			{
				colorBuffer.append( "</font><br>" );
			}
			else
			{
				colorBuffer.append( "<br>" );
			}

			if ( message.indexOf( "<" ) == -1 && message.indexOf( KoLConstants.LINE_BREAK ) != -1 )
			{
				colorBuffer.append( "</pre>" );
			}

			StaticEntity.globalStringDelete( colorBuffer, "<html>" );
			StaticEntity.globalStringDelete( colorBuffer, "</html>" );
		}

		colorBuffer.append( KoLConstants.LINE_BREAK );
		KoLConstants.commandBuffer.append( colorBuffer.toString() );
		LocalRelayServer.addStatusMessage( colorBuffer.toString() );
	}

	public static final PrintStream openStream( final String filename, final PrintStream originalStream,
		boolean hasLocation )
	{
		if ( !hasLocation && KoLCharacter.getUserName().equals( "" ) )
		{
			return NullStream.INSTANCE;
		}

		// Before doing anything, be sure to close the
		// original stream.

		if ( !( originalStream instanceof NullStream ) )
		{
			if ( hasLocation )
			{
				return originalStream;
			}

			originalStream.close();
		}

		return LogStream.openStream( filename, false );
	}

	public static final void openStandard()
	{
		RequestLogger.outputStream = System.out;
	}

	public static final void openMirror( final String location )
	{
		RequestLogger.mirrorStream = RequestLogger.openStream( location, RequestLogger.mirrorStream, true );
	}

	public static final void closeMirror()
	{
		RequestLogger.mirrorStream.close();
		RequestLogger.mirrorStream = NullStream.INSTANCE;
	}

	public static final PrintStream getSessionStream()
	{
		return RequestLogger.sessionStream;
	}

	public static final void openSessionLog()
	{
		RequestLogger.sessionStream =
			RequestLogger.openStream(
				KoLConstants.SESSIONS_DIRECTORY + StaticEntity.globalStringReplace(
					KoLCharacter.getUserName(), " ", "_" ) + "_" + KoLConstants.DAILY_FORMAT.format( new Date() ) + ".txt",
				RequestLogger.sessionStream, false );
	}

	public static final void closeSessionLog()
	{
		RequestLogger.sessionStream.close();
		RequestLogger.sessionStream = NullStream.INSTANCE;
	}

	public static final void updateSessionLog()
	{
		RequestLogger.sessionStream.println();
	}

	public static final void updateSessionLog( final String line )
	{
		RequestLogger.sessionStream.println( line );
	}

	public static final boolean isDebugging()
	{
		return RequestLogger.debugStream != NullStream.INSTANCE;
	}

	public static final PrintStream getDebugStream()
	{
		return RequestLogger.debugStream;
	}

	public static final void openDebugLog()
	{
		RequestLogger.debugStream =
			RequestLogger.openStream(
				"DEBUG_" + KoLConstants.DAILY_FORMAT.format( new Date() ) + ".txt", RequestLogger.debugStream, true );
	}

	public static final void closeDebugLog()
	{
		RequestLogger.debugStream.close();
		RequestLogger.debugStream = NullStream.INSTANCE;
	}

	public static final void updateDebugLog()
	{
		RequestLogger.debugStream.println();
	}

	public static final void updateDebugLog( final String line )
	{
		RequestLogger.debugStream.println( line );
	}

	public static final void updateDebugLog( final Throwable t )
	{
		t.printStackTrace( RequestLogger.debugStream );
	}

	public static final void updateDebugLog( final Object o )
	{
		RequestLogger.debugStream.println( o.toString() );
	}

	public static final void registerRequest( final KoLRequest request, final String urlString )
	{
		try
		{
			if ( BuffBotHome.isBuffBotActive() )
			{
				return;
			}

			RequestLogger.doRegister( request, urlString );
		}
		catch ( Exception e )
		{
			StaticEntity.printStackTrace( e );
		}
	}

	private static final void doRegister( final KoLRequest request, final String urlString )
	{
		boolean isExternal = request.getClass() == KoLRequest.class || request instanceof LocalRelayRequest;

		// There are some adventures which do not post any
		// form fields, so handle them first.

		if ( KoLAdventure.recordToSession( urlString ) )
		{
			RequestLogger.wasLastRequestSimple = false;
			return;
		}

		if ( ( request instanceof FightRequest || isExternal ) && FightRequest.registerRequest( isExternal, urlString ) )
		{
			RequestLogger.wasLastRequestSimple = false;
			return;
		}

		// Anything else that doesn't submit an actual form
		// should not be registered.

		if ( urlString.indexOf( "?" ) == -1 )
		{
			return;
		}

		// Some general URLs which never need to be registered
		// because they don't do anything.

		if ( urlString.startsWith( "choice" ) )
		{
			RequestLogger.updateSessionLog( urlString );

			// Certain choices cost meat when selected

			String choice = request.getFormField( "whichchoice" );
			String decision = request.getFormField( "option" );

			if ( choice == null || decision == null )
			{
				return;
			}

			AdventureResult cost = AdventureDatabase.getCost( choice, decision );
			int costCount = cost == null ? 0 : cost.getCount();

			if ( costCount == 0 )
			{
				return;
			}

			int inventoryCount = cost.getCount( KoLConstants.inventory );
			if ( cost.isItem() && inventoryCount == 0 )
			{
				return;
			}

			if ( costCount > 0 )
			{
				int multiplier = inventoryCount / costCount;
				cost = cost.getInstance( multiplier * costCount * -1 );
			}

			StaticEntity.getClient().processResult( cost );
			return;
		}

		if ( urlString.startsWith( "manor3.php" ) )
		{
			String demon = request.getFormField( "demonname" );
			if ( demon != null && !demon.equals( "" ) && AdventureDatabase.retrieveItem( KoLAdventure.BLACK_CANDLE ) && AdventureDatabase.retrieveItem( KoLAdventure.EVIL_SCROLL ) )
			{
				RequestLogger.updateSessionLog( "summon " + demon );

				StaticEntity.getClient().processResult( KoLAdventure.BLACK_CANDLE.getNegation() );
				StaticEntity.getClient().processResult( KoLAdventure.EVIL_SCROLL.getNegation() );
			}

			return;
		}

		if ( urlString.startsWith( "login" ) || urlString.startsWith( "logout" ) || urlString.startsWith( "charpane" ) )
		{
			return;
		}

		// This is a campground request and so must go here.
		if ( ( request instanceof TelescopeRequest || isExternal ) && TelescopeRequest.registerRequest( urlString ) )
		{
			RequestLogger.wasLastRequestSimple = false;
			return;
		}

		if ( urlString.startsWith( "leaflet" ) || urlString.startsWith( "cave" ) || urlString.startsWith( "lair" ) || urlString.startsWith( "campground" ) )
		{
			return;
		}

		if ( urlString.startsWith( "inventory.php?which" ) || urlString.equals( "knoll.php?place=paster" ) || urlString.equals( "town_right.php?place=untinker" ) )
		{
			return;
		}

		// Check individual cafes
		if ( ( request instanceof MicrobreweryRequest || isExternal ) && MicrobreweryRequest.registerRequest( urlString ) )
		{
			RequestLogger.wasLastRequestSimple = false;
			return;
		}

		if ( ( request instanceof RestaurantRequest || isExternal ) && RestaurantRequest.registerRequest( urlString ) )
		{
			RequestLogger.wasLastRequestSimple = false;
			return;
		}

		// The following lists all the remaining requests in
		// alphabetical order.

		if ( ( request instanceof ArenaRequest || isExternal ) && ArenaRequest.registerRequest( urlString ) )
		{
			RequestLogger.wasLastRequestSimple = false;
			return;
		}

		if ( ( request instanceof AutoSellRequest || isExternal ) && AutoSellRequest.registerRequest( urlString ) )
		{
			RequestLogger.wasLastRequestSimple = false;
			return;
		}

		if ( ( request instanceof CafeRequest || isExternal ) && CafeRequest.registerRequest( urlString ) )
		{
			RequestLogger.wasLastRequestSimple = false;
			return;
		}

		if ( ( request instanceof ClanGymRequest || isExternal ) && ClanGymRequest.registerRequest( urlString ) )
		{
			RequestLogger.wasLastRequestSimple = false;
			return;
		}

		if ( ( request instanceof ClanStashRequest || isExternal ) && ClanStashRequest.registerRequest( urlString ) )
		{
			RequestLogger.wasLastRequestSimple = false;
			return;
		}

		if ( ( request instanceof ConsumeItemRequest || isExternal ) && ConsumeItemRequest.registerRequest( urlString ) )
		{
			RequestLogger.wasLastRequestSimple = false;
			return;
		}

		if ( ( request instanceof EquipmentRequest || isExternal ) && EquipmentRequest.registerRequest( urlString ) )
		{
			RequestLogger.wasLastRequestSimple = false;
			return;
		}

		if ( ( request instanceof FamiliarRequest || isExternal ) && FamiliarRequest.registerRequest( urlString ) )
		{
			RequestLogger.wasLastRequestSimple = false;
			return;
		}

		if ( ( request instanceof FlowerHunterRequest || isExternal ) && FlowerHunterRequest.registerRequest( urlString ) )
		{
			RequestLogger.wasLastRequestSimple = false;
			return;
		}

		if ( ( request instanceof FriarRequest || isExternal ) && FriarRequest.registerRequest( urlString ) )
		{
			RequestLogger.wasLastRequestSimple = false;
			return;
		}

		if ( ( request instanceof GiftMessageRequest || isExternal ) && GiftMessageRequest.registerRequest( urlString ) )
		{
			RequestLogger.wasLastRequestSimple = false;
			return;
		}

		if ( ( request instanceof GreenMessageRequest || isExternal ) && GreenMessageRequest.registerRequest( urlString ) )
		{
			RequestLogger.wasLastRequestSimple = false;
			return;
		}

		if ( ItemCreationRequest.registerRequest( isExternal, urlString ) )
		{
			RequestLogger.wasLastRequestSimple = false;
			return;
		}

		if ( ( request instanceof ItemStorageRequest || isExternal ) && ItemStorageRequest.registerRequest( urlString ) )
		{
			RequestLogger.wasLastRequestSimple = false;
			return;
		}

		if ( ( request instanceof MallPurchaseRequest || isExternal ) && MallPurchaseRequest.registerRequest( urlString ) )
		{
			RequestLogger.wasLastRequestSimple = false;
			return;
		}

		if ( ( request instanceof MindControlRequest || isExternal ) && MindControlRequest.registerRequest( urlString ) )
		{
			RequestLogger.wasLastRequestSimple = false;
			return;
		}

		if ( ( request instanceof MuseumRequest || isExternal ) && MuseumRequest.registerRequest( urlString ) )
		{
			RequestLogger.wasLastRequestSimple = false;
			return;
		}

		if ( ( request instanceof PulverizeRequest || isExternal ) && PulverizeRequest.registerRequest( urlString ) )
		{
			RequestLogger.wasLastRequestSimple = false;
			return;
		}

		if ( ( request instanceof StyxPixieRequest || isExternal ) && StyxPixieRequest.registerRequest( urlString ) )
		{
			RequestLogger.wasLastRequestSimple = false;
			return;
		}

		if ( ( request instanceof UncleCrimboRequest || isExternal ) && UncleCrimboRequest.registerRequest( urlString ) )
		{
			RequestLogger.wasLastRequestSimple = false;
			return;
		}

		if ( ( request instanceof UneffectRequest || isExternal ) && UneffectRequest.registerRequest( urlString ) )
		{
			RequestLogger.wasLastRequestSimple = false;
			return;
		}

		if ( ( request instanceof UntinkerRequest || isExternal ) && UntinkerRequest.registerRequest( urlString ) )
		{
			RequestLogger.wasLastRequestSimple = false;
			return;
		}

		if ( ( request instanceof UseSkillRequest || isExternal ) && UseSkillRequest.registerRequest( urlString ) )
		{
			RequestLogger.wasLastRequestSimple = false;
			return;
		}

		if ( ( request instanceof ZapRequest || isExternal ) && ZapRequest.registerRequest( urlString ) )
		{
			RequestLogger.wasLastRequestSimple = false;
			return;
		}

		// Otherwise, make sure to print the raw URL so that it's
		// at least mentioned in the session log.

		if ( !RequestLogger.wasLastRequestSimple )
		{
			RequestLogger.updateSessionLog();
		}

		RequestLogger.wasLastRequestSimple = true;
		RequestLogger.updateSessionLog( urlString );
	}
}
