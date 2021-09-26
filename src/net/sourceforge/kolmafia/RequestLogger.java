package net.sourceforge.kolmafia;

import java.io.PrintStream;

import java.util.Date;
import java.util.List;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;

import net.sourceforge.kolmafia.listener.NamedListenerRegistry;
import net.sourceforge.kolmafia.persistence.SkillDatabase;

import net.sourceforge.kolmafia.request.*;

import net.sourceforge.kolmafia.session.ChoiceManager;
import net.sourceforge.kolmafia.session.DvorakManager;
import net.sourceforge.kolmafia.session.SorceressLairManager;

import net.sourceforge.kolmafia.utilities.LogStream;
import net.sourceforge.kolmafia.utilities.NullStream;
import net.sourceforge.kolmafia.utilities.StringUtilities;

import net.sourceforge.kolmafia.webui.RelayServer;

public class RequestLogger
	extends NullStream
{
	public static final RequestLogger INSTANCE = new RequestLogger();

	private static PrintStream outputStream = KoLmafiaTUI.outputStream;
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
			message.contains( StaticEntity.backtraceTrigger ) )
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

			// Temporary workaround for Java bug
			if ( message.startsWith( "/" ) )
			{
				colorBuffer.append( "<span>" );
			}

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

			if ( !message.contains( "<" ) && message.contains( KoLConstants.LINE_BREAK ) )
			{
				colorBuffer.append( "</pre>" );
			}

			// Temporary workaround for Java bug
			if ( message.startsWith( "/" ) )
			{
				colorBuffer.append( "</span>" );
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

	public static final void openCustom( PrintStream out )
	{
		RequestLogger.outputStream = out;
	}

	public static final void closeCustom()
	{
		RequestLogger.closeStream( RequestLogger.outputStream );
		RequestLogger.outputStream = KoLmafiaTUI.outputStream;
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
		     line.contains( StaticEntity.backtraceTrigger ) )
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
		NamedListenerRegistry.fireChange("(debug)");
	}

	public static final void closeDebugLog()
	{
		RequestLogger.closeStream( RequestLogger.debugStream );
		RequestLogger.debugStream = NullStream.INSTANCE;
		NamedListenerRegistry.fireChange("(debug)");
	}

	public static final void updateDebugLog()
	{
		RequestLogger.debugStream.println();
	}

	public static final void updateDebugLog( final String line )
	{
		if ( StaticEntity.backtraceTrigger != null &&
		     line.contains( StaticEntity.backtraceTrigger ) )
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

	private static final StringBuilder traceBuffer = new StringBuilder();
	public synchronized static final void trace( String message )
	{
		if ( RequestLogger.isTracing() )
		{
			traceBuffer.setLength( 0 );
			traceBuffer.append( ( new Date() ).getTime() );
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

	private static void doRegister( final GenericRequest request, final String urlString )
	{
		// If we are in a fight, don't even look at things which are
		// not fight.php, since they will immediately redirect to
		// continue the fight.
		if ( FightRequest.currentRound != 0 && !urlString.startsWith( "fight.php" ) )
		{
			return;
		}

		RequestLogger.lastURLString = urlString;

		if ( urlString.startsWith( "api" ) ||
		     urlString.startsWith( "charpane" ) ||
		     urlString.startsWith( "account" ) ||
		     urlString.startsWith( "login" ) ||
		     urlString.startsWith( "logout" ) )
		{
			return;
		}

		// We want to do special things when we visit locations within
		// the Sorceress' Lair. Those locations which are "adventures"
		// but are not claimed here will be picked up by KoLAdventure

		if ( SorceressLairManager.registerRequest( urlString ) )
		{
			RequestLogger.wasLastRequestSimple = false;
			return;
		}

		// Some adventures do not post any form fields,
		// so handle them first.

		if ( KoLAdventure.recordToSession( urlString ) )
		{
			RequestLogger.wasLastRequestSimple = false;
			return;
		}

		// Anything past this is not an "adventure" per se

		boolean isExternal = request.getClass() == GenericRequest.class || request instanceof RelayRequest || request instanceof PlaceRequest;

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

		// We want to register visits to the Summoning Chamber choice adventure before ChoiceManager.
		if ( ( request instanceof SummoningChamberRequest || isExternal ) && SummoningChamberRequest.registerRequest( urlString ) )
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

		// Burning Newspaper creation is an instance of choice.php
		if ( ( request instanceof BurningNewspaperRequest || isExternal ) && BurningNewspaperRequest.registerRequest( urlString ) )
		{
			RequestLogger.wasLastRequestSimple = false;
			return;
		}

		// metal meteoroid creation is an instance of choice.php
		if ( ( request instanceof MeteoroidRequest || isExternal ) && MeteoroidRequest.registerRequest( urlString ) )
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

		// The Cargo Cultist Shorts is an instance of choice.php
		if ( ( request instanceof CargoCultistShortsRequest || isExternal ) && CargoCultistShortsRequest.registerRequest( urlString ) )
		{
			RequestLogger.wasLastRequestSimple = false;
			return;
		}

		// The Deck of Every Card is an instance of choice.php
		if ( ( request instanceof DeckOfEveryCardRequest || isExternal ) && DeckOfEveryCardRequest.registerRequest( urlString ) )
		{
			RequestLogger.wasLastRequestSimple = false;
			return;
		}

		// Sweet Synthesis is an instance of choice.php
		if ( ( request instanceof SweetSynthesisRequest || isExternal ) && SweetSynthesisRequest.registerRequest( urlString ) )
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

		// Numberology is an instance of choice.php
		if ( ( request instanceof NumberologyRequest || isExternal ) && NumberologyRequest.registerRequest( urlString ) )
		{
			RequestLogger.wasLastRequestSimple = false;
			return;
		}

		// The Potted Tea Tree is an instance of choice.php
		if ( ( request instanceof PottedTeaTreeRequest || isExternal ) && PottedTeaTreeRequest.registerRequest( urlString ) )
		{
			RequestLogger.wasLastRequestSimple = false;
			return;
		}

		// Sausage Grinder creation is an instance of choice.php
		if ( ( request instanceof SausageOMaticRequest || isExternal ) && SausageOMaticRequest.registerRequest( urlString ) )
		{
			RequestLogger.wasLastRequestSimple = false;
			return;
		}

		// The Source Terminal is an instance of choice.php
		if ( ( request instanceof TerminalRequest || isExternal ) && TerminalRequest.registerRequest( urlString ) )
		{
			RequestLogger.wasLastRequestSimple = false;
			return;
		}

		// Wax Glob creation is an instance of choice.php
		if ( ( request instanceof WaxGlobRequest || isExternal ) && WaxGlobRequest.registerRequest( urlString ) )
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

		// We want to register a simple visit to tiles.php
		if ( DvorakManager.registerRequest( urlString ) )
		{
			RequestLogger.wasLastRequestSimple = false;
			return;
		}

		// Anything else that doesn't submit an actual form
		// should not be registered.

		if ( !urlString.contains( "?" ) )
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
		if ( ( request instanceof PizzaCubeRequest || isExternal ) && PizzaCubeRequest.registerRequest( urlString ) )
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

		// This might be on inventory.php

		if ( ( request instanceof EquipmentRequest || isExternal ) && EquipmentRequest.registerRequest( urlString ) )
		{
			RequestLogger.wasLastRequestSimple = false;
			return;
		}

		if ( urlString.startsWith( "campground" ) ||
		     urlString.startsWith( "doc.php" ) ||
		     urlString.startsWith( "inventory.php?ajax" ) ||
		     urlString.startsWith( "inventory.php?which=" ) ||
		     urlString.startsWith( "inventory.php?action=message" ) ||
		     urlString.startsWith( "mining" ) )
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

		// The following is in place.php
		if ( ( request instanceof FalloutShelterRequest || isExternal ) && FalloutShelterRequest.registerRequest( urlString ) )
		{
			RequestLogger.wasLastRequestSimple = false;
			return;
		}

		// Let the "placeholder" for place.php take every otherwise
		// unclaimed call to that URL.

		if ( PlaceRequest.registerRequest( urlString ) )
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

		if ( ( request instanceof AirportRequest || isExternal ) && AirportRequest.registerRequest( urlString ) )
		{
			RequestLogger.wasLastRequestSimple = false;
			return;
		}

		if ( ( request instanceof AltarOfBonesRequest || isExternal ) && AltarOfBonesRequest.registerRequest( urlString ) )
		{
			RequestLogger.wasLastRequestSimple = false;
			return;
		}

		if ( ( request instanceof ArmoryRequest || isExternal ) && ArmoryRequest.registerRequest( urlString ) )
		{
			RequestLogger.wasLastRequestSimple = false;
			return;
		}

		if ( ( request instanceof ArmoryAndLeggeryRequest || isExternal ) && ArmoryAndLeggeryRequest.registerRequest( urlString, false ) )
		{
			RequestLogger.wasLastRequestSimple = false;
			return;
		}

		if ( ( request instanceof AppleStoreRequest || isExternal ) && AppleStoreRequest.registerRequest( urlString ) )
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

		if ( ( request instanceof BatFabricatorRequest || isExternal ) && BatFabricatorRequest.registerRequest( urlString ) )
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

		if ( ( request instanceof BlackMarketRequest || isExternal ) && BlackMarketRequest.registerRequest( urlString, false ) )
		{
			RequestLogger.wasLastRequestSimple = false;
			return;
		}

		if ( ( request instanceof BoutiqueRequest || isExternal ) && BoutiqueRequest.registerRequest( urlString ) )
		{
			RequestLogger.wasLastRequestSimple = false;
			return;
		}

		if ( ( request instanceof BrogurtRequest || isExternal ) && BrogurtRequest.registerRequest( urlString ) )
		{
			RequestLogger.wasLastRequestSimple = false;
			return;
		}

		if ( ( request instanceof BuffJimmyRequest || isExternal ) && BuffJimmyRequest.registerRequest( urlString ) )
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

		if ( ( request instanceof CampAwayRequest || isExternal ) && CampAwayRequest.registerRequest( urlString ) )
		{
			RequestLogger.wasLastRequestSimple = false;
			return;
		}

		if ( ( request instanceof CanteenRequest || isExternal ) && CanteenRequest.registerRequest( urlString ) )
		{
			RequestLogger.wasLastRequestSimple = false;
			return;
		}

		if ( ( request instanceof ChateauRequest || isExternal ) && ChateauRequest.registerRequest( urlString ) )
		{
			RequestLogger.wasLastRequestSimple = false;
			return;
		}

		if ( ( request instanceof ChemiCorpRequest || isExternal ) && ChemiCorpRequest.registerRequest( urlString ) )
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

		if ( ( request instanceof CosmicRaysBazaarRequest || isExternal ) && CosmicRaysBazaarRequest.registerRequest( urlString ) )
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

		if ( ( request instanceof Crimbo14Request || isExternal ) && Crimbo14Request.registerRequest( urlString ) )
		{
			RequestLogger.wasLastRequestSimple = false;
			return;
		}

		if ( ( request instanceof Crimbo16Request || isExternal ) && Crimbo16Request.registerRequest( urlString ) )
		{
			RequestLogger.wasLastRequestSimple = false;
			return;
		}

		if ( ( request instanceof Crimbo17Request || isExternal ) && Crimbo17Request.registerRequest( urlString ) )
		{
			RequestLogger.wasLastRequestSimple = false;
			return;
		}

		if ( ( request instanceof Crimbo20BoozeRequest || isExternal ) && Crimbo20BoozeRequest.registerRequest( urlString ) )
		{
			RequestLogger.wasLastRequestSimple = false;
			return;
		}

		if ( ( request instanceof Crimbo20CandyRequest || isExternal ) && Crimbo20CandyRequest.registerRequest( urlString ) )
		{
			RequestLogger.wasLastRequestSimple = false;
			return;
		}

		if ( ( request instanceof Crimbo20FoodRequest || isExternal ) && Crimbo20FoodRequest.registerRequest( urlString ) )
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

		if ( ( request instanceof DinseyCompanyStoreRequest || isExternal ) && DinseyCompanyStoreRequest.registerRequest( urlString ) )
		{
			RequestLogger.wasLastRequestSimple = false;
			return;
		}

		if ( ( request instanceof DiscoGiftCoRequest || isExternal ) && DiscoGiftCoRequest.registerRequest( urlString ) )
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

		if ( ( request instanceof EdBaseRequest || isExternal ) && EdBaseRequest.registerRequest( urlString ) )
		{
			RequestLogger.wasLastRequestSimple = false;
			return;
		}

		if ( ( request instanceof EdShopRequest || isExternal ) && EdShopRequest.registerRequest( urlString ) )
		{
			RequestLogger.wasLastRequestSimple = false;
			return;
		}

		if ( ( request instanceof FamiliarRequest || isExternal ) && FamiliarRequest.registerRequest( urlString ) )
		{
			RequestLogger.wasLastRequestSimple = false;
			return;
		}

		if ( ( request instanceof FamTeamRequest || isExternal ) && FamTeamRequest.registerRequest( urlString ) )
		{
			RequestLogger.wasLastRequestSimple = false;
			return;
		}

		if ( ( request instanceof FantasyRealmRequest || isExternal ) && FantasyRealmRequest.registerRequest( urlString ) )
		{
			RequestLogger.wasLastRequestSimple = false;
			return;
		}

		if ( ( request instanceof FDKOLRequest || isExternal ) && FDKOLRequest.registerRequest( urlString, false ) )
		{
			RequestLogger.wasLastRequestSimple = false;
			return;
		}

		if ( ( request instanceof FishboneryRequest || isExternal ) && FishboneryRequest.registerRequest( urlString ) )
		{
			RequestLogger.wasLastRequestSimple = false;
			return;
		}

		if ( ( request instanceof FiveDPrinterRequest || isExternal ) && FiveDPrinterRequest.registerRequest( urlString ) )
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

		if ( ( request instanceof FunALogRequest || isExternal ) && FunALogRequest.registerRequest( urlString ) )
		{
			RequestLogger.wasLastRequestSimple = false;
			return;
		}

		if ( ( request instanceof GameShoppeRequest || isExternal ) && GameShoppeRequest.registerRequest( urlString ) )
		{
			RequestLogger.wasLastRequestSimple = false;
			return;
		}

		if ( ( request instanceof GMartRequest || isExternal ) && GMartRequest.registerRequest( urlString ) )
		{
			RequestLogger.wasLastRequestSimple = false;
			return;
		}

		if ( ( request instanceof GourdRequest || isExternal ) && GourdRequest.registerRequest( urlString ) )
		{
			RequestLogger.wasLastRequestSimple = false;
			return;
		}

		if ( ( request instanceof GotporkOrphanageRequest || isExternal ) && GotporkOrphanageRequest.registerRequest( urlString ) )
		{
			RequestLogger.wasLastRequestSimple = false;
			return;
		}

		if ( ( request instanceof GotporkPDRequest || isExternal ) && GotporkPDRequest.registerRequest( urlString ) )
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

		if ( ( request instanceof KringleRequest || isExternal ) && KringleRequest.registerRequest( urlString ) )
		{
			RequestLogger.wasLastRequestSimple = false;
			return;
		}

		if ( ( request instanceof LeafletRequest || isExternal ) && LeafletRequest.registerRequest( urlString ) )
		{
			RequestLogger.wasLastRequestSimple = false;
			return;
		}

		if ( ( request instanceof LTTRequest || isExternal ) && LTTRequest.registerRequest( urlString ) )
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

		if ( ( request instanceof MemeShopRequest || isExternal ) && MemeShopRequest.registerRequest( urlString ) )
		{
			RequestLogger.wasLastRequestSimple = false;
			return;
		}

		if ( ( request instanceof MerchTableRequest || isExternal ) && MerchTableRequest.registerRequest( urlString ) )
		{
			RequestLogger.wasLastRequestSimple = false;
			return;
		}

		if ( ( request instanceof MindControlRequest || isExternal ) && MindControlRequest.registerRequest( urlString ) )
		{
			RequestLogger.wasLastRequestSimple = false;
			return;
		}

		if ( ( request instanceof MomRequest || isExternal ) && MomRequest.registerRequest( urlString ) )
		{
			RequestLogger.wasLastRequestSimple = false;
			return;
		}

		if ( ( request instanceof MonsterManuelRequest || isExternal ) && MonsterManuelRequest.registerRequest( urlString ) )
		{
			RequestLogger.wasLastRequestSimple = false;
			return;
		}

		if ( ( request instanceof MushroomRequest || isExternal ) && MushroomRequest.registerRequest( urlString ) )
		{
			RequestLogger.wasLastRequestSimple = false;
			return;
		}

		if ( ( request instanceof NeandermallRequest || isExternal ) && NeandermallRequest.registerRequest( urlString ) )
		{
			RequestLogger.wasLastRequestSimple = false;
			return;
		}

		if ( ( request instanceof NemesisRequest || isExternal ) && NemesisRequest.registerRequest( urlString ) )
		{
			RequestLogger.wasLastRequestSimple = false;
			return;
		}

		if ( ( request instanceof NinjaStoreRequest || isExternal ) && NinjaStoreRequest.registerRequest( urlString ) )
		{
			RequestLogger.wasLastRequestSimple = false;
			return;
		}

		if ( ( request instanceof NuggletCraftingRequest || isExternal ) && NuggletCraftingRequest.registerRequest( urlString ) )
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

		if ( ( request instanceof PokemporiumRequest || isExternal ) && PokemporiumRequest.registerRequest( urlString ) )
		{
			RequestLogger.wasLastRequestSimple = false;
			return;
		}

		if ( ( request instanceof PrecinctRequest || isExternal ) && PrecinctRequest.registerRequest( urlString ) )
		{
			RequestLogger.wasLastRequestSimple = false;
			return;
		}

		if ( ( request instanceof ProfileRequest || isExternal ) && ProfileRequest.registerRequest( urlString ) )
		{
			RequestLogger.wasLastRequestSimple = false;
			return;
		}

		if ( ( request instanceof PulverizeRequest || isExternal ) && PulverizeRequest.registerRequest( urlString ) )
		{
			RequestLogger.wasLastRequestSimple = false;
			return;
		}

		if ( ( request instanceof QuartersmasterRequest || isExternal ) && QuartersmasterRequest.registerRequest( urlString ) )
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

		if ( ( request instanceof RubeeRequest || isExternal ) && RubeeRequest.registerRequest( urlString ) )
		{
			RequestLogger.wasLastRequestSimple = false;
			return;
		}

		if ( ( request instanceof RumpleRequest || isExternal ) && RumpleRequest.registerRequest( urlString ) )
		{
			RequestLogger.wasLastRequestSimple = false;
			return;
		}

		if ( ( request instanceof ScrapheapRequest || isExternal ) && ScrapheapRequest.registerRequest( urlString ) )
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

		if ( ( request instanceof ShoeRepairRequest || isExternal ) && ShoeRepairRequest.registerRequest( urlString ) )
		{
			RequestLogger.wasLastRequestSimple = false;
			return;
		}

		if ( ( request instanceof SHAWARMARequest || isExternal ) && SHAWARMARequest.registerRequest( urlString ) )
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

		if ( ( request instanceof SliemceRequest || isExternal ) && SliemceRequest.registerRequest( urlString ) )
		{
			RequestLogger.wasLastRequestSimple = false;
			return;
		}

		if ( ( request instanceof SpacegateFabricationRequest || isExternal ) && SpacegateFabricationRequest.registerRequest( urlString ) )
		{
			RequestLogger.wasLastRequestSimple = false;
			return;
		}

		if ( ( request instanceof SpantRequest || isExternal ) && SpantRequest.registerRequest( urlString ) )
		{
			RequestLogger.wasLastRequestSimple = false;
			return;
		}

		if ( ( request instanceof SpinMasterLatheRequest || isExternal ) && SpinMasterLatheRequest.registerRequest( urlString ) )
		{
			RequestLogger.wasLastRequestSimple = false;
			return;
		}

		if ( ( request instanceof StarChartRequest || isExternal ) && StarChartRequest.registerRequest( urlString ) )
		{
			RequestLogger.wasLastRequestSimple = false;
			return;
		}

		if ( ( request instanceof StandardRequest || isExternal ) && StandardRequest.registerRequest( urlString ) )
		{
			RequestLogger.wasLastRequestSimple = false;
			return;
		}

		if ( ( request instanceof StillRequest || isExternal ) && StillRequest.registerRequest( urlString ) )
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

		if ( ( request instanceof SugarSheetRequest || isExternal ) && SugarSheetRequest.registerRequest( urlString ) )
		{
			RequestLogger.wasLastRequestSimple = false;
			return;
		}

		if ( ( request instanceof SwaggerShopRequest || isExternal ) && SwaggerShopRequest.registerRequest( urlString ) )
		{
			RequestLogger.wasLastRequestSimple = false;
			return;
		}

		if ( ( request instanceof TacoDanRequest || isExternal ) && TacoDanRequest.registerRequest( urlString ) )
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

		if ( ( request instanceof ThankShopRequest || isExternal ) && ThankShopRequest.registerRequest( urlString ) )
		{
			RequestLogger.wasLastRequestSimple = false;
			return;
		}

		if ( ( request instanceof TicketCounterRequest || isExternal ) && TicketCounterRequest.registerRequest( urlString ) )
		{
			RequestLogger.wasLastRequestSimple = false;
			return;
		}

		if ( ( request instanceof ToxicChemistryRequest || isExternal ) && ToxicChemistryRequest.registerRequest( urlString ) )
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

		if ( ( request instanceof TutorialRequest || isExternal ) && TutorialRequest.registerRequest( urlString ) )
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

		if ( ( request instanceof WalMartRequest || isExternal ) && WalMartRequest.registerRequest( urlString ) )
		{
			RequestLogger.wasLastRequestSimple = false;
			return;
		}

		if ( ( request instanceof WarbearBoxRequest || isExternal ) && WarbearBoxRequest.registerRequest( urlString ) )
		{
			RequestLogger.wasLastRequestSimple = false;
			return;
		}

		if ( ( request instanceof WildfireCampRequest || isExternal ) && WildfireCampRequest.registerRequest( urlString ) )
		{
			RequestLogger.wasLastRequestSimple = false;
			return;
		}

		if ( ( request instanceof WinterGardenRequest || isExternal ) && WinterGardenRequest.registerRequest( urlString ) )
		{
			RequestLogger.wasLastRequestSimple = false;
			return;
		}

		if ( ( request instanceof XOShopRequest || isExternal ) && XOShopRequest.registerRequest( urlString ) )
		{
			RequestLogger.wasLastRequestSimple = false;
			return;
		}

		if ( ( request instanceof YeNeweSouvenirShoppeRequest || isExternal ) && YeNeweSouvenirShoppeRequest.registerRequest( urlString ) )
		{
			RequestLogger.wasLastRequestSimple = false;
			return;
		}

		if ( ( request instanceof YourCampfireRequest || isExternal ) && YourCampfireRequest.registerRequest( urlString ) )
		{
			RequestLogger.wasLastRequestSimple = false;
			return;
		}

		if ( ( request instanceof ZapRequest || isExternal ) && ZapRequest.registerRequest( urlString ) )
		{
			RequestLogger.wasLastRequestSimple = false;
			return;
		}

		// Do PurchaseRequest after all Coinmaster shops so they can
		// register simple visits, if they so choose.

		if ( ( request instanceof PurchaseRequest || isExternal ) && PurchaseRequest.registerRequest( urlString ) )
		{
			RequestLogger.wasLastRequestSimple = false;
			return;
		}

		// Let PlaceRequest step in and suppress logging for any
		// unclaimed simple visits to a place.php container

		if ( PlaceRequest.unclaimedPlace( urlString ) )
		{
			RequestLogger.wasLastRequestSimple = false;
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

	public static final void registerLocation( final String location )
	{
		String message = "[" + KoLAdventure.getAdventureCount() + "] " + location;

		RequestLogger.printLine();
		RequestLogger.printLine( message );

		RequestLogger.updateSessionLog();
		RequestLogger.updateSessionLog( message );
	}

	public static final void registerLastLocation()
	{
		RequestLogger.registerLocation( KoLAdventure.lastLocationName );
	}
}
