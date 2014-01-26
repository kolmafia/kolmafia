/**
 * Copyright (c) 2005-2014, KoLmafia development team
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
import java.util.List;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;

import net.sourceforge.kolmafia.persistence.SkillDatabase;

import net.sourceforge.kolmafia.request.AWOLQuartermasterRequest;
import net.sourceforge.kolmafia.request.AfterLifeRequest;
import net.sourceforge.kolmafia.request.AltarOfBonesRequest;
import net.sourceforge.kolmafia.request.AltarOfLiteracyRequest;
import net.sourceforge.kolmafia.request.ArcadeRequest;
import net.sourceforge.kolmafia.request.ArtistRequest;
import net.sourceforge.kolmafia.request.AutoMallRequest;
import net.sourceforge.kolmafia.request.AutoSellRequest;
import net.sourceforge.kolmafia.request.BeerGardenRequest;
import net.sourceforge.kolmafia.request.BeerPongRequest;
import net.sourceforge.kolmafia.request.BigBrotherRequest;
import net.sourceforge.kolmafia.request.BountyHunterHunterRequest;
import net.sourceforge.kolmafia.request.BURTRequest;
import net.sourceforge.kolmafia.request.BoutiqueRequest;
import net.sourceforge.kolmafia.request.CRIMBCOGiftShopRequest;
import net.sourceforge.kolmafia.request.CafeRequest;
import net.sourceforge.kolmafia.request.CakeArenaRequest;
import net.sourceforge.kolmafia.request.CampgroundRequest;
import net.sourceforge.kolmafia.request.ChezSnooteeRequest;
import net.sourceforge.kolmafia.request.ClanLoungeRequest;
import net.sourceforge.kolmafia.request.ClanLoungeSwimmingPoolRequest;
import net.sourceforge.kolmafia.request.ClanRumpusRequest;
import net.sourceforge.kolmafia.request.ClanStashRequest;
import net.sourceforge.kolmafia.request.ClosetRequest;
import net.sourceforge.kolmafia.request.CreateItemRequest;
import net.sourceforge.kolmafia.request.Crimbo07Request;
import net.sourceforge.kolmafia.request.Crimbo09Request;
import net.sourceforge.kolmafia.request.Crimbo10Request;
import net.sourceforge.kolmafia.request.Crimbo11Request;
import net.sourceforge.kolmafia.request.Crimbo12Request;
import net.sourceforge.kolmafia.request.CrimboCafeRequest;
import net.sourceforge.kolmafia.request.CrimboCartelRequest;
import net.sourceforge.kolmafia.request.CurseRequest;
import net.sourceforge.kolmafia.request.DigRequest;
import net.sourceforge.kolmafia.request.DimemasterRequest;
import net.sourceforge.kolmafia.request.DisplayCaseRequest;
import net.sourceforge.kolmafia.request.DollHawkerRequest;
import net.sourceforge.kolmafia.request.DreadsylvaniaRequest;
import net.sourceforge.kolmafia.request.DwarfContraptionRequest;
import net.sourceforge.kolmafia.request.DwarfFactoryRequest;
import net.sourceforge.kolmafia.request.EquipmentRequest;
import net.sourceforge.kolmafia.request.FamiliarRequest;
import net.sourceforge.kolmafia.request.FDKOLRequest;
import net.sourceforge.kolmafia.request.FightRequest;
import net.sourceforge.kolmafia.request.FloristRequest;
import net.sourceforge.kolmafia.request.FreeSnackRequest;
import net.sourceforge.kolmafia.request.FriarRequest;
import net.sourceforge.kolmafia.request.FudgeWandRequest;
import net.sourceforge.kolmafia.request.GalaktikRequest;
import net.sourceforge.kolmafia.request.GameShoppeRequest;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.request.GourdRequest;
import net.sourceforge.kolmafia.request.GrandmaRequest;
import net.sourceforge.kolmafia.request.GrandpaRequest;
import net.sourceforge.kolmafia.request.GuildRequest;
import net.sourceforge.kolmafia.request.HedgePuzzleRequest;
import net.sourceforge.kolmafia.request.HermitRequest;
import net.sourceforge.kolmafia.request.HeyDezeRequest;
import net.sourceforge.kolmafia.request.IslandRequest;
import net.sourceforge.kolmafia.request.IsotopeSmitheryRequest;
import net.sourceforge.kolmafia.request.JarlsbergRequest;
import net.sourceforge.kolmafia.request.JunkMagazineRequest;
import net.sourceforge.kolmafia.request.KnollRequest;
import net.sourceforge.kolmafia.request.KOLHSRequest;
import net.sourceforge.kolmafia.request.LeafletRequest;
import net.sourceforge.kolmafia.request.LunarLunchRequest;
import net.sourceforge.kolmafia.request.ManageStoreRequest;
import net.sourceforge.kolmafia.request.MicroBreweryRequest;
import net.sourceforge.kolmafia.request.MindControlRequest;
import net.sourceforge.kolmafia.request.MoneyMakingGameRequest;
import net.sourceforge.kolmafia.request.MrStoreRequest;
import net.sourceforge.kolmafia.request.MushroomRequest;
import net.sourceforge.kolmafia.request.NemesisRequest;
import net.sourceforge.kolmafia.request.PandamoniumRequest;
import net.sourceforge.kolmafia.request.PeeVPeeRequest;
import net.sourceforge.kolmafia.request.PixelRequest;
import net.sourceforge.kolmafia.request.PortalRequest;
import net.sourceforge.kolmafia.request.PulverizeRequest;
import net.sourceforge.kolmafia.request.PurchaseRequest;
import net.sourceforge.kolmafia.request.QuartersmasterRequest;
import net.sourceforge.kolmafia.request.RabbitHoleRequest;
import net.sourceforge.kolmafia.request.RaffleRequest;
import net.sourceforge.kolmafia.request.RelayRequest;
import net.sourceforge.kolmafia.request.RichardRequest;
import net.sourceforge.kolmafia.request.SeaMerkinRequest;
import net.sourceforge.kolmafia.request.SendGiftRequest;
import net.sourceforge.kolmafia.request.SendMailRequest;
import net.sourceforge.kolmafia.request.ShoreGiftShopRequest;
import net.sourceforge.kolmafia.request.ShrineRequest;
import net.sourceforge.kolmafia.request.SkateParkRequest;
import net.sourceforge.kolmafia.request.SpaaaceRequest;
import net.sourceforge.kolmafia.request.StarChartRequest;
import net.sourceforge.kolmafia.request.StorageRequest;
import net.sourceforge.kolmafia.request.SuburbanDisRequest;
import net.sourceforge.kolmafia.request.SwaggerShopRequest;
import net.sourceforge.kolmafia.request.TavernRequest;
import net.sourceforge.kolmafia.request.TelescopeRequest;
import net.sourceforge.kolmafia.request.TerrifiedEagleInnRequest;
import net.sourceforge.kolmafia.request.TicketCounterRequest;
import net.sourceforge.kolmafia.request.TrapperRequest;
import net.sourceforge.kolmafia.request.TravelingTraderRequest;
import net.sourceforge.kolmafia.request.TrophyHutRequest;
import net.sourceforge.kolmafia.request.UneffectRequest;
import net.sourceforge.kolmafia.request.UntinkerRequest;
import net.sourceforge.kolmafia.request.UseItemRequest;
import net.sourceforge.kolmafia.request.UseSkillRequest;
import net.sourceforge.kolmafia.request.VendingMachineRequest;
import net.sourceforge.kolmafia.request.VolcanoIslandRequest;
import net.sourceforge.kolmafia.request.VolcanoMazeRequest;
import net.sourceforge.kolmafia.request.WarbearBoxRequest;
import net.sourceforge.kolmafia.request.WineCellarRequest;
import net.sourceforge.kolmafia.request.WinterGardenRequest;
import net.sourceforge.kolmafia.request.ZapRequest;

import net.sourceforge.kolmafia.session.ChoiceManager;

import net.sourceforge.kolmafia.utilities.LogStream;
import net.sourceforge.kolmafia.utilities.NullStream;
import net.sourceforge.kolmafia.utilities.StringUtilities;

import net.sourceforge.kolmafia.webui.RelayServer;

public class RequestLogger
	extends NullStream
{
	public static final RequestLogger INSTANCE = new RequestLogger();

	private static PrintStream outputStream = NullStream.INSTANCE;
	private static PrintStream mirrorStream = NullStream.INSTANCE;

	private static PrintStream sessionStream = NullStream.INSTANCE;
	private static PrintStream debugStream = NullStream.INSTANCE;
	private static PrintStream traceStream = NullStream.INSTANCE;

	private static String lastURLString = "";
	private static String previousUpdateString = "";
	private static boolean wasLastRequestSimple = false;

	private RequestLogger()
	{
	}

	public static String getLastURLString()
	{
		return lastURLString;
	}

	@Override
	public void println()
	{
		RequestLogger.printLine();
	}

	@Override
	public void println( final String line )
	{
		RequestLogger.printLine( line );
	}

	public static final void printList( final List printing, final PrintStream ostream )
	{
		if ( printing == null || ostream == null )
		{
			return;
		}

		StringBuffer buffer = new StringBuffer();

		if ( printing != KoLConstants.availableSkills )
		{
			Object current;
			for ( int i = 0; i < printing.size(); ++i )
			{
				current = printing.get( i );
				if ( current == null )
				{
					continue;
				}

				buffer.append( current.toString() );
				buffer.append( KoLConstants.LINE_BREAK );
			}

			ostream.println( buffer.toString() );
			return;
		}

		SkillDatabase.generateSkillList( buffer, false );

		if ( ostream != INSTANCE )
		{
			ostream.println( buffer.toString() );
			return;
		}

		printLine( buffer.toString(), false );

		buffer.setLength( 0 );
		SkillDatabase.generateSkillList( buffer, true );
		KoLConstants.commandBuffer.append( buffer.toString() );
	}

	public static final void printList( final List printing )
	{
		RequestLogger.printList( printing, INSTANCE );
	}

	public static final void printLine()
	{
		RequestLogger.printLine( MafiaState.CONTINUE, "", true );
	}

	public static final void printLine( final String message )
	{
		RequestLogger.printLine( MafiaState.CONTINUE, message, true );
	}

	public static final void printLine( final String message, final boolean addToBuffer )
	{
		RequestLogger.printLine( MafiaState.CONTINUE, message, addToBuffer );
	}

	public static final void printLine( final MafiaState state, final String message )
	{
		RequestLogger.printLine( state, message, true );
	}

	public static final void printLine( final MafiaState state, String message, boolean addToBuffer )
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
		
		if ( StaticEntity.backtraceTrigger != null &&
			message.indexOf( StaticEntity.backtraceTrigger ) != -1 )
		{
			StaticEntity.printStackTrace( "Backtrace triggered by message" );
		}

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

			if ( state == MafiaState.ERROR || state == MafiaState.ABORT )
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

			colorBuffer.append( StringUtilities.globalStringReplace( message, "\n", "<br>" ) );

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

			StringUtilities.globalStringDelete( colorBuffer, "<html>" );
			StringUtilities.globalStringDelete( colorBuffer, "</html>" );
		}

		colorBuffer.append( KoLConstants.LINE_BREAK );
		KoLConstants.commandBuffer.append( colorBuffer.toString() );
		RelayServer.addStatusMessage( colorBuffer.toString() );
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

			RequestLogger.closeStream( originalStream );
		}

		return LogStream.openStream( filename, false );
	}

	public static final void closeStream( final PrintStream stream )
	{
		try
		{
			stream.close();
		}
		catch ( Exception e )
		{
		}
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
		RequestLogger.closeStream( RequestLogger.mirrorStream );
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
				KoLConstants.SESSIONS_DIRECTORY + StringUtilities.globalStringReplace(
					KoLCharacter.getUserName(), " ", "_" ) + "_" + KoLConstants.DAILY_FORMAT.format( new Date() ) + ".txt",
				RequestLogger.sessionStream, false );
	}

	public static final void closeSessionLog()
	{
		RequestLogger.closeStream( RequestLogger.sessionStream );
		RequestLogger.sessionStream = NullStream.INSTANCE;
	}

	public static final void updateSessionLog()
	{
		RequestLogger.sessionStream.println();
	}

	public static final void updateSessionLog( final String line )
	{
		if ( StaticEntity.backtraceTrigger != null &&
			line.indexOf( StaticEntity.backtraceTrigger ) != -1 )
		{
			StaticEntity.printStackTrace( "Backtrace triggered by session log message" );
		}

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
		RequestLogger.closeStream( RequestLogger.debugStream );
		RequestLogger.debugStream = NullStream.INSTANCE;
	}

	public static final void updateDebugLog()
	{
		RequestLogger.debugStream.println();
	}

	public static final void updateDebugLog( final String line )
	{
		if ( StaticEntity.backtraceTrigger != null &&
			line.indexOf( StaticEntity.backtraceTrigger ) != -1 )
		{
			StaticEntity.printStackTrace( "Backtrace triggered by debug log message" );
		}

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

	public static final boolean isTracing()
	{
		return RequestLogger.traceStream != NullStream.INSTANCE;
	}

	public static final PrintStream getTraceStream()
	{
		return RequestLogger.traceStream;
	}

	public static final void openTraceStream()
	{
		RequestLogger.traceStream =
			RequestLogger.openStream(
				"TRACE_" + KoLConstants.DAILY_FORMAT.format( new Date() ) + ".txt", RequestLogger.traceStream, true );
	}

	public static final void closeTraceStream()
	{
		RequestLogger.closeStream( RequestLogger.traceStream );
		RequestLogger.traceStream = NullStream.INSTANCE;
	}

	private static StringBuilder traceBuffer = new StringBuilder();
	public synchronized static final void trace( String message )
	{
		if ( RequestLogger.isTracing() )
		{
			traceBuffer.setLength( 0 );
			traceBuffer.append( String.valueOf( ( new Date() ).getTime() ) );
			traceBuffer.append( ": " );
			traceBuffer.append( message );
			RequestLogger.traceStream.println( traceBuffer.toString() );
		}
	}

	public static final void registerRequest( final GenericRequest request, final String urlString )
	{
		try
		{
			RequestLogger.doRegister( request, urlString );
		}
		catch ( Exception e )
		{
			StaticEntity.printStackTrace( e );
		}
	}

	private static final void doRegister( final GenericRequest request, final String urlString )
	{
		RequestLogger.lastURLString = urlString;

		// There are some adventures which do not post any
		// form fields, so handle them first.

		if ( KoLAdventure.recordToSession( urlString ) )
		{
			RequestLogger.wasLastRequestSimple = false;
			return;
		}

		// Anything past this is not an "adventure" per se

		boolean isExternal = request.getClass() == GenericRequest.class || request instanceof RelayRequest;

		if ( ( request instanceof FightRequest || isExternal ) && FightRequest.registerRequest( isExternal, urlString ) )
		{
			RequestLogger.wasLastRequestSimple = false;
			return;
		}

		// We want to register simple visits to the Altar of Literacy
		if ( ( request instanceof AltarOfLiteracyRequest || isExternal ) && AltarOfLiteracyRequest.registerRequest( urlString ) )
		{
			RequestLogger.wasLastRequestSimple = false;
			return;
		}

		// We want to register simple visits to the Bounty Hunter Hunter
		if ( ( request instanceof BountyHunterHunterRequest || isExternal ) && BountyHunterHunterRequest.registerRequest( urlString ) )
		{
			RequestLogger.wasLastRequestSimple = false;
			return;
		}

		// We want to register visits to the You're the Fudge Wizard Now, Dog choice adventure before ChoiceManager.
		if ( ( request instanceof FudgeWandRequest || isExternal ) && FudgeWandRequest.registerRequest( urlString ) )
		{
			RequestLogger.wasLastRequestSimple = false;
			return;
		}

		// We want to register simple visits to Doc Galaktik
		if ( ( request instanceof GalaktikRequest || isExternal ) && GalaktikRequest.registerRequest( urlString ) )
		{
			RequestLogger.wasLastRequestSimple = false;
			return;
		}

		// We want to register simple visits to HeyDeze
		if ( ( request instanceof HeyDezeRequest || isExternal ) && HeyDezeRequest.registerRequest( urlString ) )
		{
			RequestLogger.wasLastRequestSimple = false;
			return;
		}

		// We want to register simple visits to Mr. Store
		if ( ( request instanceof MrStoreRequest || isExternal ) && MrStoreRequest.registerRequest( urlString ) )
		{
			RequestLogger.wasLastRequestSimple = false;
			return;
		}

		// We want to register simple visits to Spaaace
		if ( ( request instanceof SpaaaceRequest || isExternal ) && SpaaaceRequest.registerRequest( urlString ) )
		{
			RequestLogger.wasLastRequestSimple = false;
			return;
		}

		// We want to register simple visits to the Volcano Maze
		if ( ( request instanceof VolcanoMazeRequest || isExternal ) && VolcanoMazeRequest.registerRequest( urlString ) )
		{
			RequestLogger.wasLastRequestSimple = false;
			return;
		}

		// The Clan Lounge Swimming Pool is an instance of choice.php
		if ( ( request instanceof ClanLoungeSwimmingPoolRequest || isExternal ) && ClanLoungeSwimmingPoolRequest.registerRequest( urlString ) )
		{
			RequestLogger.wasLastRequestSimple = false;
			return;
		}

		// The Florist is an instance of choice.php
		if ( ( request instanceof FloristRequest || isExternal ) && FloristRequest.registerRequest( urlString ) )
		{
			RequestLogger.wasLastRequestSimple = false;
			return;
		}

		// Some general URLs which never need to be registered
		// because they don't do anything.

		if ( urlString.startsWith( "choice" ) )
		{
			ChoiceManager.registerRequest( urlString );
			return;
		}

		// Anything else that doesn't submit an actual form
		// should not be registered.

		if ( urlString.indexOf( "?" ) == -1 )
		{
			return;
		}

		if ( urlString.startsWith( "login" ) ||
		     urlString.startsWith( "logout" ) ||
		     urlString.startsWith( "account" ) ||
		     urlString.startsWith( "api" ) ||
		     urlString.startsWith( "charpane" ) )
		{
			return;
		}

		// We want to register some visits to the Campground
		if ( ( request instanceof CampgroundRequest || isExternal ) && CampgroundRequest.registerRequest( urlString ) )
		{
			RequestLogger.wasLastRequestSimple = false;
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

		if ( urlString.startsWith( "campground" ) ||
		     urlString.startsWith( "doc.php" ) ||
		     urlString.startsWith( "inventory.php?ajax" ) ||
		     urlString.startsWith( "inventory.php?which=" ) ||
		     urlString.startsWith( "inventory.php?action=message" ) ||
		     urlString.startsWith( "lair" ) ||
		     urlString.startsWith( "mining" ) ||
		     urlString.equals( "forestvillage.php?place=untinker" ) )
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

		if ( ( request instanceof CrimboCafeRequest || isExternal ) && CrimboCafeRequest.registerRequest( urlString ) )
		{
			RequestLogger.wasLastRequestSimple = false;
			return;
		}

		// Check UseItemRequest early, so that lastItemUsed gets
		// cleared when processing anything else.  Otherwise, any
		// non-item-use that redirects to inventory.php?action=message
		// (such as outfit changes) will cause the last item to be
		// processed again.
		
		// However, we have to check CreateItemRequest earlier, so that
		// it can handle single-/multi-use concoctions.
		if ( CreateItemRequest.registerRequest( isExternal, urlString ) )
		{
			RequestLogger.wasLastRequestSimple = false;
			return;
		}

		// Are we finally ready to call UseItemRequest?
		if ( ( request instanceof UseItemRequest || isExternal ) && UseItemRequest.registerRequest( urlString ) )
		{
			RequestLogger.wasLastRequestSimple = false;
			return;
		}

		// The following lists all the remaining requests in
		// alphabetical order.

		if ( ( request instanceof AfterLifeRequest || isExternal ) && AfterLifeRequest.registerRequest( urlString ) )
		{
			RequestLogger.wasLastRequestSimple = false;
			return;
		}

		if ( ( request instanceof AltarOfBonesRequest || isExternal ) && AltarOfBonesRequest.registerRequest( urlString ) )
		{
			RequestLogger.wasLastRequestSimple = false;
			return;
		}

		if ( ( request instanceof ArcadeRequest || request instanceof TicketCounterRequest || isExternal ) && ArcadeRequest.registerRequest( urlString ) )
		{
			RequestLogger.wasLastRequestSimple = false;
			return;
		}

		if ( ( request instanceof ArtistRequest || isExternal ) && ArtistRequest.registerRequest( urlString ) )
		{
			RequestLogger.wasLastRequestSimple = false;
			return;
		}

		if ( ( request instanceof AutoMallRequest || isExternal ) && AutoMallRequest.registerRequest( urlString ) )
		{
			RequestLogger.wasLastRequestSimple = false;
			return;
		}

		if ( ( request instanceof AutoSellRequest || isExternal ) && AutoSellRequest.registerRequest( urlString ) )
		{
			RequestLogger.wasLastRequestSimple = false;
			return;
		}

		if ( ( request instanceof AWOLQuartermasterRequest || isExternal ) && AWOLQuartermasterRequest.registerRequest( urlString ) )
		{
			RequestLogger.wasLastRequestSimple = false;
			return;
		}

		if ( ( request instanceof BeerGardenRequest || isExternal ) && BeerGardenRequest.registerRequest( urlString ) )
		{
			RequestLogger.wasLastRequestSimple = false;
			return;
		}

		if ( ( request instanceof BeerPongRequest || isExternal ) && BeerPongRequest.registerRequest( urlString ) )
		{
			RequestLogger.wasLastRequestSimple = false;
			return;
		}

		if ( ( request instanceof BigBrotherRequest || isExternal ) && BigBrotherRequest.registerRequest( urlString ) )
		{
			RequestLogger.wasLastRequestSimple = false;
			return;
		}

		if ( ( request instanceof BoutiqueRequest || isExternal ) && BoutiqueRequest.registerRequest( urlString ) )
		{
			RequestLogger.wasLastRequestSimple = false;
			return;
		}

		if ( ( request instanceof BURTRequest || isExternal ) && BURTRequest.registerRequest( urlString ) )
		{
			RequestLogger.wasLastRequestSimple = false;
			return;
		}

		if ( ( request instanceof CafeRequest || isExternal ) && CafeRequest.registerRequest( urlString ) )
		{
			RequestLogger.wasLastRequestSimple = false;
			return;
		}

		if ( ( request instanceof CakeArenaRequest || isExternal ) && CakeArenaRequest.registerRequest( urlString ) )
		{
			RequestLogger.wasLastRequestSimple = false;
			return;
		}

		if ( ( request instanceof ClanLoungeRequest || isExternal ) && ClanLoungeRequest.registerRequest( urlString ) )
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

		if ( ( request instanceof ClosetRequest || isExternal ) && ClosetRequest.registerRequest( urlString ) )
		{
			RequestLogger.wasLastRequestSimple = false;
			return;
		}

		if ( ( request instanceof CRIMBCOGiftShopRequest || isExternal ) && CRIMBCOGiftShopRequest.registerRequest( urlString ) )
		{
			RequestLogger.wasLastRequestSimple = false;
			return;
		}

		if ( ( request instanceof Crimbo07Request || isExternal ) && Crimbo07Request.registerRequest( urlString ) )
		{
			RequestLogger.wasLastRequestSimple = false;
			return;
		}

		if ( ( request instanceof Crimbo09Request || isExternal ) && Crimbo09Request.registerRequest( urlString ) )
		{
			RequestLogger.wasLastRequestSimple = false;
			return;
		}

		if ( ( request instanceof Crimbo10Request || isExternal ) && Crimbo10Request.registerRequest( urlString ) )
		{
			RequestLogger.wasLastRequestSimple = false;
			return;
		}

		if ( ( request instanceof Crimbo11Request || isExternal ) && Crimbo11Request.registerRequest( urlString ) )
		{
			RequestLogger.wasLastRequestSimple = false;
			return;
		}

		if ( ( request instanceof Crimbo12Request || isExternal ) && Crimbo12Request.registerRequest( urlString ) )
		{
			RequestLogger.wasLastRequestSimple = false;
			return;
		}

		if ( ( request instanceof CrimboCartelRequest || isExternal ) && CrimboCartelRequest.registerRequest( urlString ) )
		{
			RequestLogger.wasLastRequestSimple = false;
			return;
		}

		if ( ( request instanceof CurseRequest || isExternal ) && CurseRequest.registerRequest( urlString ) )
		{
			RequestLogger.wasLastRequestSimple = false;
			return;
		}

		if ( ( request instanceof DigRequest || isExternal ) && DigRequest.registerRequest( urlString ) )
		{
			RequestLogger.wasLastRequestSimple = false;
			return;
		}

		if ( ( request instanceof DimemasterRequest || isExternal ) && DimemasterRequest.registerRequest( urlString ) )
		{
			RequestLogger.wasLastRequestSimple = false;
			return;
		}

		if ( ( request instanceof DisplayCaseRequest || isExternal ) && DisplayCaseRequest.registerRequest( urlString ) )
		{
			RequestLogger.wasLastRequestSimple = false;
			return;
		}

		if ( ( request instanceof DollHawkerRequest || isExternal ) && DollHawkerRequest.registerRequest( urlString ) )
		{
			RequestLogger.wasLastRequestSimple = false;
			return;
		}

		if ( ( request instanceof DreadsylvaniaRequest || isExternal ) && DreadsylvaniaRequest.registerRequest( urlString ) )
		{
			RequestLogger.wasLastRequestSimple = false;
			return;
		}

		if ( ( request instanceof DwarfContraptionRequest || isExternal ) && DwarfContraptionRequest.registerRequest( urlString ) )
		{
			RequestLogger.wasLastRequestSimple = false;
			return;
		}

		if ( ( request instanceof DwarfFactoryRequest || isExternal ) && DwarfFactoryRequest.registerRequest( urlString ) )
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

		if ( ( request instanceof FDKOLRequest || isExternal ) && FDKOLRequest.registerRequest( urlString, false ) )
		{
			RequestLogger.wasLastRequestSimple = false;
			return;
		}

		if ( ( request instanceof FreeSnackRequest || isExternal ) && FreeSnackRequest.registerRequest( urlString ) )
		{
			RequestLogger.wasLastRequestSimple = false;
			return;
		}

		if ( ( request instanceof FriarRequest || isExternal ) && FriarRequest.registerRequest( urlString ) )
		{
			RequestLogger.wasLastRequestSimple = false;
			return;
		}

		if ( ( request instanceof GameShoppeRequest || isExternal ) && GameShoppeRequest.registerRequest( urlString ) )
		{
			RequestLogger.wasLastRequestSimple = false;
			return;
		}

		if ( ( request instanceof GourdRequest || isExternal ) && GourdRequest.registerRequest( urlString ) )
		{
			RequestLogger.wasLastRequestSimple = false;
			return;
		}

		if ( ( request instanceof GrandmaRequest || isExternal ) && GrandmaRequest.registerRequest( urlString ) )
		{
			RequestLogger.wasLastRequestSimple = false;
			return;
		}

		if ( ( request instanceof GrandpaRequest || isExternal ) && GrandpaRequest.registerRequest( urlString ) )
		{
			RequestLogger.wasLastRequestSimple = false;
			return;
		}

		if ( ( request instanceof GuildRequest || isExternal ) && GuildRequest.registerRequest( urlString ) )
		{
			RequestLogger.wasLastRequestSimple = false;
			return;
		}

		if ( ( request instanceof HedgePuzzleRequest || isExternal ) && HedgePuzzleRequest.registerRequest( urlString ) )
		{
			RequestLogger.wasLastRequestSimple = false;
			return;
		}

		if ( ( request instanceof HermitRequest || isExternal ) && HermitRequest.registerRequest( urlString ) )
		{
			RequestLogger.wasLastRequestSimple = false;
			return;
		}

		if ( ( request instanceof IslandRequest || isExternal ) && IslandRequest.registerRequest( urlString ) )
		{
			RequestLogger.wasLastRequestSimple = false;
			return;
		}

		if ( ( request instanceof IsotopeSmitheryRequest || isExternal ) && IsotopeSmitheryRequest.registerRequest( urlString ) )
		{
			RequestLogger.wasLastRequestSimple = false;
			return;
		}

		if ( ( request instanceof JarlsbergRequest || isExternal ) && JarlsbergRequest.registerRequest( urlString ) )
		{
			RequestLogger.wasLastRequestSimple = false;
			return;
		}

		if ( ( request instanceof JunkMagazineRequest || isExternal ) && JunkMagazineRequest.registerRequest( urlString ) )
		{
			RequestLogger.wasLastRequestSimple = false;
			return;
		}

		if ( ( request instanceof KnollRequest || isExternal ) && KnollRequest.registerRequest( urlString ) )
		{
			RequestLogger.wasLastRequestSimple = false;
			return;
		}

		if ( ( request instanceof KOLHSRequest || isExternal ) && KOLHSRequest.registerRequest( urlString ) )
		{
			RequestLogger.wasLastRequestSimple = false;
			return;
		}

		if ( ( request instanceof LeafletRequest || isExternal ) && LeafletRequest.registerRequest( urlString ) )
		{
			RequestLogger.wasLastRequestSimple = false;
			return;
		}

		if ( ( request instanceof LunarLunchRequest || isExternal ) && LunarLunchRequest.registerRequest( urlString ) )
		{
			RequestLogger.wasLastRequestSimple = false;
			return;
		}

		if ( ( request instanceof ManageStoreRequest || isExternal ) && ManageStoreRequest.registerRequest( urlString ) )
		{
			RequestLogger.wasLastRequestSimple = false;
			return;
		}

		if ( ( request instanceof MindControlRequest || isExternal ) && MindControlRequest.registerRequest( urlString ) )
		{
			RequestLogger.wasLastRequestSimple = false;
			return;
		}

		if ( ( request instanceof MoneyMakingGameRequest || isExternal ) && MoneyMakingGameRequest.registerRequest( urlString ) )
		{
			RequestLogger.wasLastRequestSimple = false;
			return;
		}

		if ( ( request instanceof MushroomRequest || isExternal ) && MushroomRequest.registerRequest( urlString ) )
		{
			RequestLogger.wasLastRequestSimple = false;
			return;
		}

		if ( ( request instanceof NemesisRequest || isExternal ) && NemesisRequest.registerRequest( urlString ) )
		{
			RequestLogger.wasLastRequestSimple = false;
			return;
		}

		if ( ( request instanceof PandamoniumRequest || isExternal ) && PandamoniumRequest.registerRequest( urlString ) )
		{
			RequestLogger.wasLastRequestSimple = false;
			return;
		}

		if ( ( request instanceof PeeVPeeRequest || isExternal ) && PeeVPeeRequest.registerRequest( urlString ) )
		{
			RequestLogger.wasLastRequestSimple = false;
			return;
		}

		if ( ( request instanceof PixelRequest || isExternal ) && PixelRequest.registerRequest( urlString ) )
		{
			RequestLogger.wasLastRequestSimple = false;
			return;
		}

		if ( ( request instanceof PulverizeRequest || isExternal ) && PulverizeRequest.registerRequest( urlString ) )
		{
			RequestLogger.wasLastRequestSimple = false;
			return;
		}

		if ( ( request instanceof PurchaseRequest || isExternal ) && PurchaseRequest.registerRequest( urlString ) )
		{
			RequestLogger.wasLastRequestSimple = false;
			return;
		}

		if ( ( request instanceof QuartersmasterRequest || isExternal ) && QuartersmasterRequest.registerRequest( urlString ) )
		{
			RequestLogger.wasLastRequestSimple = false;
			return;
		}

		if ( ( request instanceof RabbitHoleRequest || isExternal ) && RabbitHoleRequest.registerRequest( urlString ) )
		{
			RequestLogger.wasLastRequestSimple = false;
			return;
		}

		if ( ( request instanceof RaffleRequest || isExternal ) && RaffleRequest.registerRequest( urlString ) )
		{
			RequestLogger.wasLastRequestSimple = false;
			return;
		}

		if ( ( request instanceof RichardRequest || isExternal ) && RichardRequest.registerRequest( urlString ) )
		{
			RequestLogger.wasLastRequestSimple = false;
			return;
		}

		if ( ( request instanceof SeaMerkinRequest || isExternal ) && SeaMerkinRequest.registerRequest( urlString ) )
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

		if ( ( request instanceof ShoreGiftShopRequest || isExternal ) && ShoreGiftShopRequest.registerRequest( urlString ) )
		{
			RequestLogger.wasLastRequestSimple = false;
			return;
		}

		if ( ( request instanceof ShrineRequest || isExternal ) && ShrineRequest.registerRequest( urlString ) )
		{
			RequestLogger.wasLastRequestSimple = false;
			return;
		}

		if ( ( request instanceof SkateParkRequest || isExternal ) && SkateParkRequest.registerRequest( urlString ) )
		{
			RequestLogger.wasLastRequestSimple = false;
			return;
		}

		if ( ( request instanceof StarChartRequest || isExternal ) && StarChartRequest.registerRequest( urlString ) )
		{
			RequestLogger.wasLastRequestSimple = false;
			return;
		}

		if ( ( request instanceof StorageRequest || isExternal ) && StorageRequest.registerRequest( urlString ) )
		{
			RequestLogger.wasLastRequestSimple = false;
			return;
		}

		if ( ( request instanceof SuburbanDisRequest || isExternal ) && SuburbanDisRequest.registerRequest( urlString ) )
		{
			RequestLogger.wasLastRequestSimple = false;
			return;
		}

		if ( ( request instanceof SwaggerShopRequest || isExternal ) && SwaggerShopRequest.registerRequest( urlString ) )
		{
			RequestLogger.wasLastRequestSimple = false;
			return;
		}

		if ( ( request instanceof TavernRequest || isExternal ) && TavernRequest.registerRequest( urlString ) )
		{
			RequestLogger.wasLastRequestSimple = false;
			return;
		}

		if ( ( request instanceof TerrifiedEagleInnRequest || isExternal ) && TerrifiedEagleInnRequest.registerRequest( urlString ) )
		{
			RequestLogger.wasLastRequestSimple = false;
			return;
		}

		if ( ( request instanceof TicketCounterRequest || isExternal ) && TicketCounterRequest.registerRequest( urlString ) )
		{
			RequestLogger.wasLastRequestSimple = false;
			return;
		}

		if ( ( request instanceof TrapperRequest || isExternal ) && TrapperRequest.registerRequest( urlString ) )
		{
			RequestLogger.wasLastRequestSimple = false;
			return;
		}

		if ( ( request instanceof TravelingTraderRequest || isExternal ) && TravelingTraderRequest.registerRequest( urlString ) )
		{
			RequestLogger.wasLastRequestSimple = false;
			return;
		}

		if ( ( request instanceof TrophyHutRequest || isExternal ) && TrophyHutRequest.registerRequest( urlString ) )
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

		if ( ( request instanceof VendingMachineRequest || isExternal ) && VendingMachineRequest.registerRequest( urlString ) )
		{
			RequestLogger.wasLastRequestSimple = false;
			return;
		}

		if ( ( request instanceof VolcanoIslandRequest || isExternal ) && VolcanoIslandRequest.registerRequest( urlString ) )
		{
			RequestLogger.wasLastRequestSimple = false;
			return;
		}

		if ( ( request instanceof WarbearBoxRequest || isExternal ) && WarbearBoxRequest.registerRequest( urlString ) )
		{
			RequestLogger.wasLastRequestSimple = false;
			return;
		}

		if ( ( request instanceof WineCellarRequest || isExternal ) && WineCellarRequest.registerRequest( urlString ) )
		{
			RequestLogger.wasLastRequestSimple = false;
			return;
		}

		if ( ( request instanceof WinterGardenRequest || isExternal ) && WinterGardenRequest.registerRequest( urlString ) )
		{
			RequestLogger.wasLastRequestSimple = false;
			return;
		}

		if ( ( request instanceof ZapRequest || isExternal ) && ZapRequest.registerRequest( urlString ) )
		{
			RequestLogger.wasLastRequestSimple = false;
			return;
		}

		// place.php is now a thing. Don't bother registering simple visits to places.
		if ( urlString.startsWith( "place.php" ) && !urlString.contains( "action=" ) )
		{
			return;
		}

		// Otherwise, print the raw URL so that it's at least mentioned
		// in the session log.

		if ( !RequestLogger.wasLastRequestSimple )
		{
			RequestLogger.updateSessionLog();
		}

		RequestLogger.wasLastRequestSimple = true;
		RequestLogger.updateSessionLog( urlString );
	}

	public static final void registerLastLocation()
	{
		String message = "[" + KoLAdventure.getAdventureCount() + "] " + KoLAdventure.lastLocationName;

		RequestLogger.printLine();
		RequestLogger.printLine( message );

		RequestLogger.updateSessionLog();
		RequestLogger.updateSessionLog( message );
	}
}
