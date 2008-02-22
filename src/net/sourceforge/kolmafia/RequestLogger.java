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

import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.request.CafeRequest;
import net.sourceforge.kolmafia.request.ChezSnooteeRequest;
import net.sourceforge.kolmafia.request.ClanRumpusRequest;
import net.sourceforge.kolmafia.request.ClanStashRequest;
import net.sourceforge.kolmafia.request.ClosetRequest;
import net.sourceforge.kolmafia.request.CoinMasterRequest;
import net.sourceforge.kolmafia.request.CreateItemRequest;
import net.sourceforge.kolmafia.request.Crimbo07Request;
import net.sourceforge.kolmafia.request.DisplayCaseRequest;
import net.sourceforge.kolmafia.request.EquipmentRequest;
import net.sourceforge.kolmafia.request.FamiliarRequest;
import net.sourceforge.kolmafia.request.FightRequest;
import net.sourceforge.kolmafia.request.FriarRequest;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.request.IslandArenaRequest;
import net.sourceforge.kolmafia.request.MallPurchaseRequest;
import net.sourceforge.kolmafia.request.MicroBreweryRequest;
import net.sourceforge.kolmafia.request.MindControlRequest;
import net.sourceforge.kolmafia.request.PortalRequest;
import net.sourceforge.kolmafia.request.PulverizeRequest;
import net.sourceforge.kolmafia.request.PvpRequest;
import net.sourceforge.kolmafia.request.RaffleRequest;
import net.sourceforge.kolmafia.request.RelayRequest;
import net.sourceforge.kolmafia.request.SellStuffRequest;
import net.sourceforge.kolmafia.request.SendGiftRequest;
import net.sourceforge.kolmafia.request.SendMailRequest;
import net.sourceforge.kolmafia.request.StyxPixieRequest;
import net.sourceforge.kolmafia.request.TelescopeRequest;
import net.sourceforge.kolmafia.request.UneffectRequest;
import net.sourceforge.kolmafia.request.UntinkerRequest;
import net.sourceforge.kolmafia.request.UseItemRequest;
import net.sourceforge.kolmafia.request.UseSkillRequest;
import net.sourceforge.kolmafia.request.ZapRequest;
import net.sourceforge.kolmafia.session.ChoiceManager;
import net.sourceforge.kolmafia.session.InventoryManager;

public class RequestLogger
	extends NullStream
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

	public static final void registerRequest( final GenericRequest request, final String urlString )
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

	private static final void doRegister( final GenericRequest request, final String urlString )
	{
		boolean isExternal = request.getClass() == GenericRequest.class || request instanceof RelayRequest;

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

		// We want to register simple visits to the Bounty Hunter Hunter
		if ( ( request instanceof CoinMasterRequest || isExternal ) && CoinMasterRequest.registerRequest( urlString ) )
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

			AdventureResult cost = ChoiceManager.getCost( choice, decision );
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
			if ( demon != null && !demon.equals( "" ) && InventoryManager.retrieveItem( ItemPool.BLACK_CANDLE, 3 ) && InventoryManager.retrieveItem( ItemPool.EVIL_SCROLL ) )
			{
				RequestLogger.updateSessionLog( "summon " + demon );

				StaticEntity.getClient().processResult( ItemPool.BLACK_CANDLE, -3 );
				StaticEntity.getClient().processResult( ItemPool.EVIL_SCROLL, -1 );
			}

			return;
		}

		if ( urlString.startsWith( "login" ) || urlString.startsWith( "logout" ) || urlString.startsWith( "charpane" ) )
		{
			return;
		}

		// This is a campground request and so must go here.
		if ( ( request instanceof PortalRequest || isExternal ) && PortalRequest.registerRequest( urlString ) )
		{
			RequestLogger.wasLastRequestSimple = false;
			return;
		}

		// This is a campground request and so must go here.
		if ( ( request instanceof TelescopeRequest || isExternal ) && TelescopeRequest.registerRequest( urlString ) )
		{
			RequestLogger.wasLastRequestSimple = false;
			return;
		}

		// This might be a campground request and so must go here.
		if ( ( request instanceof UseSkillRequest || isExternal ) && UseSkillRequest.registerRequest( urlString ) )
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
		if ( ( request instanceof MicroBreweryRequest || isExternal ) && MicroBreweryRequest.registerRequest( urlString ) )
		{
			RequestLogger.wasLastRequestSimple = false;
			return;
		}

		if ( ( request instanceof ChezSnooteeRequest || isExternal ) && ChezSnooteeRequest.registerRequest( urlString ) )
		{
			RequestLogger.wasLastRequestSimple = false;
			return;
		}

		// The following lists all the remaining requests in
		// alphabetical order.

		if ( ( request instanceof IslandArenaRequest || isExternal ) && IslandArenaRequest.registerRequest( urlString ) )
		{
			RequestLogger.wasLastRequestSimple = false;
			return;
		}

		if ( ( request instanceof SellStuffRequest || isExternal ) && SellStuffRequest.registerRequest( urlString ) )
		{
			RequestLogger.wasLastRequestSimple = false;
			return;
		}

		if ( ( request instanceof CafeRequest || isExternal ) && CafeRequest.registerRequest( urlString ) )
		{
			RequestLogger.wasLastRequestSimple = false;
			return;
		}

		if ( ( request instanceof ClanRumpusRequest || isExternal ) && ClanRumpusRequest.registerRequest( urlString ) )
		{
			RequestLogger.wasLastRequestSimple = false;
			return;
		}

		if ( ( request instanceof ClanStashRequest || isExternal ) && ClanStashRequest.registerRequest( urlString ) )
		{
			RequestLogger.wasLastRequestSimple = false;
			return;
		}

		if ( ( request instanceof UseItemRequest || isExternal ) && UseItemRequest.registerRequest( urlString ) )
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

		if ( ( request instanceof PvpRequest || isExternal ) && PvpRequest.registerRequest( urlString ) )
		{
			RequestLogger.wasLastRequestSimple = false;
			return;
		}

		if ( ( request instanceof FriarRequest || isExternal ) && FriarRequest.registerRequest( urlString ) )
		{
			RequestLogger.wasLastRequestSimple = false;
			return;
		}

		if ( ( request instanceof SendGiftRequest || isExternal ) && SendGiftRequest.registerRequest( urlString ) )
		{
			RequestLogger.wasLastRequestSimple = false;
			return;
		}

		if ( ( request instanceof SendMailRequest || isExternal ) && SendMailRequest.registerRequest( urlString ) )
		{
			RequestLogger.wasLastRequestSimple = false;
			return;
		}

		if ( CreateItemRequest.registerRequest( isExternal, urlString ) )
		{
			RequestLogger.wasLastRequestSimple = false;
			return;
		}

		if ( ( request instanceof ClosetRequest || isExternal ) && ClosetRequest.registerRequest( urlString ) )
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

		if ( ( request instanceof DisplayCaseRequest || isExternal ) && DisplayCaseRequest.registerRequest( urlString ) )
		{
			RequestLogger.wasLastRequestSimple = false;
			return;
		}

		if ( ( request instanceof PulverizeRequest || isExternal ) && PulverizeRequest.registerRequest( urlString ) )
		{
			RequestLogger.wasLastRequestSimple = false;
			return;
		}

		if ( ( request instanceof RaffleRequest || isExternal ) && RaffleRequest.registerRequest( urlString ) )
		{
			RequestLogger.wasLastRequestSimple = false;
			return;
		}

		if ( ( request instanceof StyxPixieRequest || isExternal ) && StyxPixieRequest.registerRequest( urlString ) )
		{
			RequestLogger.wasLastRequestSimple = false;
			return;
		}

		if ( ( request instanceof Crimbo07Request || isExternal ) && Crimbo07Request.registerRequest( urlString ) )
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
