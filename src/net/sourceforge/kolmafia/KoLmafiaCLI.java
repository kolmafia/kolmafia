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

// input and output
import java.io.InputStream;
import java.io.PrintStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.NumberFormatException;

// utility imports
import java.util.Date;
import java.util.List;
import java.util.ArrayList;
import java.text.DecimalFormat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.java.dev.spellcast.utilities.LockableListModel;
import net.java.dev.spellcast.utilities.DataUtilities;

/**
 * The main class for the <code>KoLmafia</code> package.  This
 * class encapsulates most of the data relevant to any given
 * session of <code>Kingdom of Loathing</code> and currently
 * functions as the blackboard in the architecture.  When data
 * listeners are implemented, it will continue to manage most
 * of the interactions.
 */

public class KoLmafiaCLI extends KoLmafia
{
	private static final ArrayList unrepeatableCommands = new ArrayList();

	public static final int NOWHERE = 1;
	public static final int INVENTORY = 2;
	public static final int CREATION = 3;
	public static final int CLOSET = 4;

	protected String previousLine;

	private PrintStream outputStream = NullStream.INSTANCE;
	private PrintStream mirrorStream = NullStream.INSTANCE;
	private BufferedReader commandStream;

	private KoLmafiaCLI lastScript;
	private KoLmafiaASH advancedHandler;

	public static void main( String [] args )
	{
		StringBuffer initialScript = new StringBuffer();
		
		for ( int i = 1; i < args.length; ++i )
		{
			initialScript.append( args[i] );
			initialScript.append( " " );
		}

		System.out.println();
		System.out.println( " **************************" );
		System.out.println( " *     " + VERSION_NAME + "      *" );
		System.out.println( " * Command Line Interface *" );
		System.out.println( " **************************" );
		System.out.println();

		StaticEntity.setClient( DEFAULT_SHELL );
		DEFAULT_SHELL.outputStream = System.out;

		if ( initialScript.length() == 0 )
		{
			DEFAULT_SHELL.attemptLogin();
			DEFAULT_SHELL.listenForCommands();
		}
		else
		{
			String actualScript = initialScript.toString().trim();
			if ( actualScript.startsWith( "script=" ) )
				actualScript = actualScript.substring( 7 );
			
			DEFAULT_SHELL.executeLine( "call " + actualScript );
			DEFAULT_SHELL.listenForCommands();
		}
	}

	/**
	 * Constructs a new <code>KoLmafiaCLI</code> object.  All data fields
	 * are initialized to their default values, the global settings
	 * are loaded from disk.
	 */

	public KoLmafiaCLI( InputStream inputStream )
	{
		if ( StaticEntity.getClient() instanceof KoLmafiaCLI )
			outputStream = System.out;

		try
		{
			commandStream = new BufferedReader( new InputStreamReader( inputStream ) );
		}
		catch ( Exception e )
		{
			// This should not happen.  Therefore, print
			// a stack trace for debug purposes.
			
			StaticEntity.printStackTrace( e, "Error opening input stream." );
		}

		advancedHandler = new KoLmafiaASH();
	}

	public static void reset()
	{	unrepeatableCommands.clear();
	}

	/**
	 * Utility method used to prompt the user for their login and
	 * password.  Later on, when profiles are added, prompting
	 * for the user will automatically look up a password.
	 */

	private void attemptLogin()
	{
		try
		{
			String username = StaticEntity.getProperty( "autoLogin" );

			if ( username == null || username.length() == 0 )
			{
				outputStream.println();
				outputStream.print( "username: " );
				username = commandStream.readLine();
			}

			if ( username == null || username.length() == 0 )
			{
				outputStream.println( "Invalid login." );
				return;
			}

			username = username.replaceAll( "/q", "" );
			String password = StaticEntity.getClient().getSaveState( username );

			if ( password == null )
			{
				outputStream.print( "password: " );
				password = commandStream.readLine();
			}

			if ( password == null || password.length() == 0 )
			{
				outputStream.println( "Invalid password." );
				return;
			}

			outputStream.println();
			StaticEntity.getClient().deinitialize();
			(new LoginRequest( StaticEntity.getClient(), username, password )).run();
		}
		catch ( IOException e )
		{
			// This should not happen.  Therefore, print
			// a stack trace for debug purposes.
			
			StaticEntity.printStackTrace( e, "Error in login attempt" );
		}
	}

	/**
	 * Initializes the <code>KoLmafia</code> session.  Called after
	 * the login has been confirmed to notify the StaticEntity.getClient() that the
	 * login was successful, the user-specific settings should be
	 * loaded, and the user can begin adventuring.
	 */

	public void initialize( String username, String sessionID )
	{
		if ( StaticEntity.getClient() != this )
			StaticEntity.getClient().initialize( username, sessionID );
		else
			super.initialize( username, sessionID );

		if ( StaticEntity.getClient() == this )
		{
			printBlankLine();
			executeCommand( "moons", "" );
			printBlankLine();
		}
	}

	/**
	 * A utility method which waits for commands from the user, then
	 * executing each command as it arrives.
	 */

	public void listenForCommands()
	{
		if ( StaticEntity.getClient() == this )
			outputStream.print( " > " );

		String line = null;

		while ( (line = getNextLine()) != null && (StaticEntity.getClient().permitsContinue() || StaticEntity.getClient() == this) )
		{
			DEFAULT_SHELL.updateDisplay( "" );
			if ( StaticEntity.getClient() == this )
				printBlankLine();

			executeLine( line );

			if ( StaticEntity.getClient() == this )
			{
				printBlankLine();
				outputStream.print( " > " );
			}
			else if ( StaticEntity.getClient() instanceof KoLmafiaCLI && !StaticEntity.getClient().permitsContinue() )
			{
				outputStream.print( "Continue? [Y/N] > " );
				mirrorStream.print( "Continue? [Y/N]" );

				line = ((KoLmafiaCLI)StaticEntity.getClient()).getNextLine();

				if ( line.startsWith( "y" ) || line.startsWith( "Y" ) )
					updateDisplay( CONTINUE_STATE, "Continuing script..." );
			}
		}

		if ( line == null || line.trim().length() == 0 )
		{
			try
			{
				commandStream.close();
				previousLine = null;
			}
			catch ( IOException e )
			{
				// This should not happen.  Therefore, print
				// a stack trace for debug purposes.
				
				StaticEntity.printStackTrace( e );
			}
		}
	}

	private String getNextLine()
	{
		try
		{
			String line;

			do
			{
				// Read a line from input, and break out of the do-while
				// loop when you've read a valid line (which is a non-comment
				// and a non-blank line) or when you've reached EOF.

				line = commandStream.readLine();
			}
			while ( line != null && (line.trim().length() == 0 || line.trim().startsWith( "#" ) || line.trim().startsWith( "//" ) || line.trim().startsWith( "\'" )) );

			// You will either have reached the end of file, or you will
			// have a valid line -- return it.

			if ( StaticEntity.getClient() == this && line != null && mirrorStream != null )
				mirrorStream.println( " > " + line );

			return line == null ? null : line.trim();
		}
		catch ( IOException e )
		{
			// This should not happen.  Therefore, print
			// a stack trace for debug purposes.
			
			StaticEntity.printStackTrace( e );
			return null;
		}
	}

	/**
	 * A utility method which executes a line input by the user.
	 * This method actually parses the command for the desired
	 * information, and delegates the actual command choice to
	 * yet another method.
	 */

	public synchronized void executeLine( String line )
	{
		if ( line.indexOf( ";" ) != -1 )
		{
			String [] separateLines = line.split( ";" );
			for ( int i = 0; i < separateLines.length; ++i )
				if ( separateLines[i].length() > 0 )
					executeLine( separateLines[i] );

			previousLine = line;
			return;
		}

		// Trim the line, replace all double spaces with
		// single spaces and compare the result against
		// commands which are no longer allowed.

		line = line.replaceAll( "\\s+", " " ).trim();
		if ( unrepeatableCommands.contains( line ) )
		{
			updateDisplay( ERROR_STATE, "Sorry.  You can only do that once per session." );
			return;
		}

		if ( line.length() != 0 )
		{
			String command = line.trim().split( " " )[0].toLowerCase().trim();
			String parameters = line.substring( command.length() ).trim();

			if ( !command.equals( "repeat" ) )
				previousLine = line;

			executeCommand( command, parameters );
		}
	}

	/**
	 * A utility command which decides, based on the command
	 * to be executed, what to be done with it.  It can either
	 * delegate this to other functions, or do it itself.
	 */

	private void executeCommand( String command, String parameters )
	{
		// Insert random video game reference command to
		// start things off.

		if ( command.equals( "priphea" ) )
		{
			if ( !KoLDesktop.getInstance().isVisible() )
			{
				KoLDesktop.getInstance().initializeTabs();
				KoLDesktop.getInstance().pack();
				KoLDesktop.getInstance().setVisible( true );
			}

			return;
		}

		// Maybe the person is trying to load a raw URL
		// to test something without creating a brand new
		// KoLRequest object to handle it yet?

		if ( command.indexOf( ".php" ) != -1 )
		{
			KoLRequest desired = new KoLRequest( StaticEntity.getClient(), previousLine, true );
			StaticEntity.getClient().makeRequest( desired, 1 );
			StaticEntity.externalUpdate( desired.getURLString(), desired.responseText );
			return;
		}

		// Maybe the person wants to load up their browser
		// from the KoLmafia CLI?

		if ( command.startsWith( "relay" ) || command.startsWith( "serve" ) )
		{
			StaticEntity.getClient().startRelayServer();
			return;
		}

		// First, handle the wait command, for however
		// many seconds the user would like to wait.

		if ( command.equals( "wait" ) || command.equals( "pause" ) )
		{
			try
			{
				int seconds = parameters.equals( "" ) ? 1 : df.parse( parameters ).intValue();
				StaticEntity.executeCountdown( "Countdown: ", seconds );
			}
			catch ( Exception e )
			{
				// This should not happen.  Therefore, print
				// a stack trace for debug purposes.
				
				StaticEntity.printStackTrace( e );
			}

			updateDisplay( "Waiting completed." );
			return;
		}

		// Preconditions kickass, so they're handled right
		// after the wait command.  (Right)

		if ( command.equals( "conditions" ) )
		{
			executeConditionsCommand( parameters );
			return;
		}

		// Handle the update command.  This downloads stuff
		// from the KoLmafia sourceforge website, so it does
		// not require for you to be online.

		if ( command.equals( "update" ) )
		{
			downloadAdventureOverride();
			updateDisplay( "You will need to restart KoLmafia for the changes to take effect." );
			return;
		}

		if ( command.equals( "abort" ) )
		{
			updateDisplay( ABORT_STATE, parameters.length() == 0 ? "Script abort." : parameters );
			return;
		}

		// Adding the requested echo command.  I guess this is
		// useful for people who want to echo things...

		if ( command.equals( "echo" ) )
		{
			if ( parameters.equalsIgnoreCase( "timestamp" ) )
				updateDisplay( CalendarFrame.TODAY_FORMATTER.format( new Date() ) );
			else if ( parameters.equalsIgnoreCase( "kol-date" ) )
				updateDisplay( MoonPhaseDatabase.getCalendarDayAsString( new Date() ) );
			else
				updateDisplay( parameters );

			return;
		}

		// Adding another undocumented property setting command
		// so people can configure variables in scripts.

		if ( command.equals( "set" ) )
		{
			int splitIndex = parameters.indexOf( "=" );
			StaticEntity.setProperty( parameters.substring( 0, splitIndex ).trim(), parameters.substring( splitIndex + 1 ).trim() );
			return;
		}

		// Handle the if-statement and the while-statement.
		// The while-statement will not get a separate comment
		// because it is unloved.

		if ( command.equals( "if" ) )
		{
			executeIfStatement( parameters );
			return;
		}

		if ( command.equals( "while" ) )
		{
			executeWhileStatement( parameters );
			return;
		}

		// Next, handle any requests to login or relogin.
		// This will be done by calling a utility method.

		if ( command.equals( "login" ) || command.equals( "relogin" ) )
		{
			String password = StaticEntity.getClient().getSaveState( parameters );
			if ( password != null )
			{
				if ( StaticEntity.getClient().getPasswordHash() != null )
				{
					updateDisplay( "Logging out..." );
					(new LogoutRequest( StaticEntity.getClient() )).run();
				}

				StaticEntity.getClient().deinitialize();
				(new LoginRequest( StaticEntity.getClient(), parameters, password )).run();
			}
			else
				updateDisplay( ERROR_STATE, "No password saved for that username." );

			return;
		}

		// Next, handle any requests that request exiting.
		// Note that a logout request should be sent in
		// order to be friendlier to the server, if the
		// character has already logged in.

		if ( command.equals( "exit" ) || command.equals( "quit" ) || command.equals( "logout" ) )
		{
			updateDisplay( "Logging out..." );
			(new LogoutRequest( StaticEntity.getClient() )).run();

			updateDisplay( "Exiting KoLmafia..." );
			System.exit(0);
		}

		// Next, handle any requests for script execution;
		// these can be done at any time (including before
		// login), so they should be handled before a test
		// of login state needed for other commands.

		if ( command.equals( "call" ) || command.equals( "run" ) || command.equals( "exec" ) || command.equals( "load" ) )
		{
			executeScriptCommand( parameters );
			return;
		}

		// Next, handle continue commands, which allow
		// you to resume where you left off in the
		// last executing script.

		if ( command.equals( "continue" ) )
		{
			if ( lastScript == null || lastScript.previousLine == null )
			{
				updateDisplay( ERROR_STATE, "No commands left to continue script." );
				return;
			}

			lastScript.listenForCommands();

			if ( lastScript.previousLine == null )
				lastScript = null;

			return;
		}

		// Next, handle repeat commands - these are a
		// carry-over feature which made sense in CLI.

		if ( command.equals( "repeat" ) )
		{
			try
			{
				if ( previousLine != null )
				{
					int repeatCount = parameters.length() == 0 ? 1 : df.parse( parameters ).intValue();
					for ( int i = 0; i < repeatCount && StaticEntity.getClient().permitsContinue(); ++i )
						executeLine( previousLine );
				}

				return;
			}
			catch ( Exception e )
			{
				// Notify the client that the command could not be repeated
				// the given number of times.

				updateDisplay( ERROR_STATE, parameters + " is not a number." );
				return;
			}
		}

		// Next, print out the moon phase, if the user
		// wishes to know what moon phase it is.

		if ( command.startsWith( "moon" ) )
		{
			updateDisplay( "Ronald: " + MoonPhaseDatabase.getRonaldPhaseAsString() );
			updateDisplay( "Grimace: " + MoonPhaseDatabase.getRonaldPhaseAsString() );
			printBlankLine();

			updateDisplay( MoonPhaseDatabase.getMoonEffect() );
			printBlankLine();

			Date today = new Date();

			try
			{
				today = sdf.parse( sdf.format( today ) );
			}
			catch ( Exception e )
			{
				// This should not happen.  Therefore, print
				// a stack trace for debug purposes.

				StaticEntity.printStackTrace( e );
				return;
			}

			String [] holidayPredictions = MoonPhaseDatabase.getHolidayPredictions( today );
			for ( int i = 0; i < holidayPredictions.length; ++i )
				updateDisplay( holidayPredictions[i] );

			printBlankLine();
			updateDisplay( MoonPhaseDatabase.getHoliday( today ) );
			printBlankLine();

			return;
		}

		// Next, handle requests to start or stop
		// debug mode.

		if ( command.startsWith( "debug" ) )
		{
			if ( parameters.startsWith( "on" ) )
				openDebugLog();
			else if ( parameters.startsWith( "off" ) )
				closeDebugLog();
			else
			{
				updateDisplay( ERROR_STATE, parameters + " is not a valid option." );
			}

			return;
		}

		// Next, handle requests to start or stop
		// the mirror stream.

		if ( command.indexOf( "mirror" ) != -1 )
		{
			if ( command.indexOf( "end" ) != -1 || command.indexOf( "stop" ) != -1 || command.indexOf( "close" ) != -1 ||
				parameters.length() == 0 || parameters.equals( "end" ) || parameters.equals( "stop" ) || parameters.equals( "close" ) )
			{
				this.mirrorStream.close();
				this.mirrorStream = NullStream.INSTANCE;

				updateDisplay( "Mirror stream closed." );
			}
			else
			{
				File outputFile = new File( parameters );
				outputFile = new File( outputFile.getAbsolutePath() );

				// If the output file does not exist, create it first
				// to avoid FileNotFoundExceptions being thrown.

				try
				{
					if ( !outputFile.exists() )
					{
						outputFile.getParentFile().mkdirs();
						outputFile.createNewFile();
					}

					this.mirrorStream = new PrintStream( new FileOutputStream( outputFile, true ), true );
				}
				catch ( IOException e )
				{
					// This should not happen.  Therefore, print
					// a stack trace for debug purposes.
					
					StaticEntity.printStackTrace( e, "Error opening file <" + parameters + ">" );
					return;
				}
			}

			return;
		}

		// Then, the description lookup command so that
		// people can see what the description is for a
		// particular item.

		if ( command.equals( "lookup" ) )
		{
			AdventureResult result = getFirstMatchingItem( parameters, NOWHERE );
			if ( result == null )
			{
				updateDisplay( ERROR_STATE, "No item matching [" + parameters + "] found" );
				return;
			}

			KoLRequest request = new KoLRequest( StaticEntity.getClient(),
				"desc_item.php?whichitem=" + TradeableItemDatabase.getDescriptionID( result.getItemID() ) );

			request.run();

			if ( StaticEntity.getClient() instanceof KoLmafiaGUI )
			{
				Object [] params = new Object[2];
				params[0] = StaticEntity.getClient();
				params[1] = request;

				(new CreateFrameRunnable( RequestFrame.class, params )).run();
			}
			else
			{
				showHTML( request.responseText, "Item Description" );
			}

			return;
		}

		if ( command.equals( "survival" ) )
		{
			showHTML( AdventureDatabase.getAreaCombatData( AdventureDatabase.getAdventure( parameters ).toString() ).toString(), "Survival Lookup" );
			return;
		}
		
		// Look!  It's the command to complete the
		// Sorceress entryway.

		if ( command.equals( "entryway" ) )
		{
			SorceressLair.completeEntryway();
			return;
		}

		// Look!  It's the command to complete the
		// Sorceress hedge maze!  This is placed
		// right after for consistency.

		if ( command.equals( "hedgemaze" ) )
		{
			SorceressLair.completeHedgeMaze();
			return;
		}

		// Look!  It's the command to fight the guardians
		// in the Sorceress's tower!  This is placed
		// right after for consistency.

		if ( command.equals( "guardians" ) )
		{
			SorceressLair.fightTowerGuardians();
			return;
		}

		// Look!  It's the command to complete the Sorceress's
		// Chamber! This is placed right after for consistency.

		if ( command.equals( "chamber" ) )
		{
			SorceressLair.completeSorceressChamber();
			return;
		}

		// Next is the command to rob the strange leaflet.
		// This method invokes the "robStrangeLeaflet" method
		// on the script requestor.

		if ( command.equals( "leaflet" ) )
		{
			StrangeLeaflet.robStrangeLeaflet( !parameters.equals( "nomagic" ) );
			return;
		}

		// Next is the command to face your nemesis.  This
		// method invokes the "faceNemesis" method on the
		// script requestor.

		if ( command.equals( "nemesis" ) )
		{
			Nemesis.faceNemesis();
			return;
		}

		if ( command.equals( "guild" ) )
		{
			StaticEntity.getClient().unlockGuildStore();
			return;
		}

		if ( command.equals( "gourd" ) )
		{
			StaticEntity.getClient().tradeGourdItems();
			return;
		}

		if ( command.equals( "tavern" ) )
		{
			StaticEntity.getClient().locateTavernFaucet();
			return;
		}

		// Next is the command to train your current familiar

		if ( command.equals( "train" ) )
		{
			trainFamiliar( parameters );
			return;
		}

		// Next is the command to visit the council.
		// This prints data to the command line.

		if ( command.equals( "council" ) )
		{
			KoLRequest request = new KoLRequest( StaticEntity.getClient(), "council.php", true );
			request.run();

			showHTML( request.responseText, "Council quests" );
			return;
		}

		// Campground activities are fairly common, so
		// they will be listed first after the above.

		if ( command.equals( "campground" ) )
		{
			executeCampgroundRequest( parameters );
			return;
		}

		// Buffs are pretty neat, too - for now, it's
		// just casts on self

		if ( command.equals( "cast" ) || command.equals( "skill" ) )
		{
			executeCastBuffRequest( parameters );
			return;
		}

		// Uneffect with martians are related to buffs,
		// so listing them next seems logical.

		if ( command.equals( "uneffect" ) || command.equals( "remedy" ) )
		{
			makeUneffectRequest();
			return;
		}

		// Add in item retrieval the way KoLmafia handles
		// it internally.

		if ( command.equals( "retrieve" ) || command.equals( "acquire" ) )
		{
			// Generic handling of retrieval of worthless
			// items is by adventuring in the sewer.

			if ( parameters.equals( "worthless item" ) )
			{
				while ( HermitRequest.getWorthlessItemCount() == 0 )
					executeLine( "buy chewing gum on a string; adventure unlucky" );
			}
			else
			{
				AdventureResult item = getFirstMatchingItem( parameters, NOWHERE );

				if ( item != null )
					AdventureDatabase.retrieveItem( item );
			}

			return;
 		}

		// Adding clan management command options inline
		// in the parsing.

		if ( command.equals( "clan" ) )
		{
			if ( parameters.equals( "snapshot" ) )
				ClanManager.takeSnapshot( 20, 10, 5, 0, false );
			else if ( parameters.equals( "stashlog" ) )
				ClanManager.saveStashLog();

			return;
		}

		// One command available after login is a request
		// to print the current state of the StaticEntity.getClient().  This
		// should be handled in a separate method, since
		// there are many things the StaticEntity.getClient() may want to print

		if ( command.equals( "print" ) || command.equals( "list" ) || command.equals( "show" ) )
		{
			executePrintCommand( parameters );
			return;
		}

		// One command is an item usage request.  These
		// requests are complicated, so delegate to the
		// appropriate utility method.

		if ( command.equals( "eat" ) || command.equals( "drink" ) || command.equals( "use" ) )
		{
			executeConsumeItemRequest( parameters );
			return;
		}

		// Zapping with wands is a type of item usage

		if ( command.equals( "zap" ) )
		{
			makeZapRequest();
			return;
		}

		// Another item-related command is a creation
		// request.  Again, complicated request, so
		// delegate to the appropriate utility method.

		if ( command.equals( "create" ) || command.equals( "make" ) || command.equals( "bake" ) || command.equals( "mix" ) || command.equals( "smith" ) || command.equals( "tinker" ) )
		{
			executeItemCreationRequest( parameters );
			return;
		}

		// Another item-related command is an untinker
		// request.  Again, complicated request, so
		// delegate to the appropriate utility method.

		if ( command.equals( "untinker" ) )
		{
			makeUntinkerRequest();
			return;
		}

		// Another item-related command is the autosell
		// request.  This one's simple, but it still
		// gets its own utility method.

		if ( command.equals( "mallsell" ) || command.equals( "automall" ) )
		{
			executeAutoMallRequest( parameters );
			return;
		}

		// Yay for more item-related commands.  This
		// one is the one that allows you to place
		// things into your clan stash.

		if ( command.equals( "stash" ) )
		{
			executeStashRequest( parameters );
			return;
		}

		// Another w00t for more item-related commands.
		// This one is the one that allows you to pull
		// things from storage.

		if ( command.equals( "hagnk" ) )
		{
			executeHagnkRequest( parameters );
			return;
		}

		// Another item-related command is the autosell
		// request.  This one's simple, but it still
		// gets its own utility method.

		if ( command.equals( "sell" ) || command.equals( "autosell" ) )
		{
			executeAutoSellRequest( parameters );
			return;
		}

		if ( command.equals( "reprice" ) || command.equals( "undercut" ) )
		{
			StaticEntity.getClient().priceItemsAtLowestPrice();
			return;
		}

		// Setting the Canadian mind control machine

		if ( command.equals( "mind-control" ) )
		{
			makeMindControlRequest();
			return;
		}

		// Commands to manipulate the mushroom plot

		if ( command.equals( "field" ) )
		{
			executeMushroomCommand( parameters );
			return;
		}

		// One of the largest commands is adventuring,
		// which (as usual) gets its own module.

		if ( command.equals( "adventure" ) )
		{
			executeAdventureRequest( parameters );
			return;
		}

		// Donations get their own command and module,
		// too, which handles hero donations and basic
		// clan donations.

		if ( command.equals( "donate" ) )
		{
			executeDonateCommand( parameters );
			return;
		}

		// Another popular command involves changing
		// a specific piece of equipment.

		if ( command.startsWith( "equip" ) )
		{
			executeEquipCommand( parameters );
			return;
		}


		// You can remove a specific piece of equipment.

		if ( command.startsWith( "unequip" ) )
		{
			executeUnequipCommand( parameters );
			return;
		}

		// Another popular command involves changing
		// your current familiar.

		if ( command.startsWith( "familiar" ) )
		{
			if ( parameters.startsWith( "list" ) )
			{
				executePrintCommand( "familiars " + parameters.substring(4).trim() );
				return;
			}
			else if ( parameters.length() == 0 )
			{
				executePrintCommand( "familiars" );
				return;
			}

			String lowerCaseName = parameters.toLowerCase();
			List familiars = KoLCharacter.getFamiliarList();

			for ( int i = 0; i < familiars.size(); ++i )
			{
				if ( familiars.get(i).toString().toLowerCase().indexOf( lowerCaseName ) != -1 )
				{
					StaticEntity.getClient().makeRequest( new FamiliarRequest( StaticEntity.getClient(), (FamiliarData) familiars.get(i) ), 1 );
					return;
				}
			}

			DEFAULT_SHELL.updateDisplay( ERROR_STATE, "You don't have that familiar." );
			return;
		}

		// Another popular command involves managing
		// your player's closet!  Which can be fun.

		if ( command.startsWith( "closet" ) )
		{
			if ( parameters.startsWith( "list" ) )
			{
				executePrintCommand( "closet " + parameters.substring(4).trim() );
				return;
			}
			else if ( parameters.length() == 0 )
			{
				executePrintCommand( "closet" );
				return;
			}

			executeClosetManageRequest( parameters );
			return;
		}
		
		// More commands -- this time to put stuff into
		// your display case (or remove them).
		
		if ( command.equals( "display" ) )
		{
			executeDisplayCaseRequest( parameters );
			return;
		}

		// Yet another popular command involves changing
		// your outfit.

		if ( command.startsWith( "outfit" ) )
		{
			if ( parameters.startsWith( "list" ) )
			{
				executePrintCommand( "outfits " + parameters.substring(4).trim() );
				return;
			}
			else if ( parameters.length() == 0 )
			{
				executePrintCommand( "outfits" );
				return;
			}

			executeChangeOutfitCommand( parameters );
			if ( StaticEntity.getClient().permitsContinue() )
				executePrintCommand( "equip" );
			return;
		}

		// Purchases from the mall are really popular,
		// as far as scripts go.  Nobody is sure why,
		// but they're popular, so they're implemented.

		if ( command.equals( "buy" ) || command.equals( "mallbuy" ) )
		{
			executeBuyCommand( parameters );
			return;
		}

		// The BuffBot may never get called from the CLI,
		// but we'll include it here for completeness sake

		if ( command.equals( "buffbot" ))
		{
			executeBuffBotCommand( parameters );
			return;
		}

		// If you just want to see what's in the mall,
		// then execute a search from here.

		if ( command.equals( "searchmall" ) )
		{
			List results = new ArrayList();
			StoreManager.searchMall( parameters, results );
			printList( results );
			return;
		}

		if ( command.equals( "hermit" ) )
		{
			makeHermitRequest();
			return;
		}

		if ( command.equals( "trapper" ) )
		{
			makeTrapperRequest();
			return;
		}

		if ( command.equals( "hunter" ) )
		{
			makeHunterRequest();
			return;
		}

		if ( command.equals( "galaktik" ) )
		{
			makeGalaktikRequest();
			return;
		}

		if ( command.equals( "restaurant" ) )
		{
			makeRestaurantRequest();
			return;
		}

		if ( command.equals( "microbrewery" ) )
		{
			makeMicrobreweryRequest();
			return;
		}

		// Campground commands, like retrieving toast, relaxing
		// at the beanbag, resting at your house/tent, and visiting
		// the evil golden arches.

		if ( command.equals( "toast" ) || command.equals( "rest" ) || command.equals( "relax" ) || command.equals( "arches" ) )
		{
			executeCampgroundRequest( command + " " + parameters );
			return;
		}

		// Because it makes sense to add this command as-is,
		// you now have the ability to request buffs.

		if ( command.equals( "send" ) )
		{
			executeSendRequest( parameters );
			return;
		}

		if ( (command.equals( "status" ) || command.equals( "effects" )) && parameters.startsWith( "refresh" ) )
		{
			(new CharsheetRequest( StaticEntity.getClient() )).run();
			updateDisplay( "Status refreshed." );
			parameters = parameters.length() == 7 ? "" : parameters.substring( 7 ).trim();
		}

		if ( command.startsWith( "inv" ) && parameters.equals( "refresh" ) )
		{
			(new EquipmentRequest( StaticEntity.getClient(), EquipmentRequest.CLOSET )).run();
			updateDisplay( "Status refreshed." );
			return;
		}

		// Finally, handle command abbreviations - in
		// other words, commands that look like they're
		// their own commands, but are actually commands
		// which are derived from other commands.

		if ( command.startsWith( "inv" ) || command.equals( "session" ) || command.equals( "summary" ) ||
			command.equals( "effects" ) || command.startsWith( "status" ) || command.equals( "encounters" ) )
		{
			executePrintCommand( command + " " + parameters );
			return;
		}

		// If all else fails, then assume that the
		// person was trying to call a script.

		executeScriptCommand( previousLine );
	}

	public void showHTML( String text, String title )
	{
		updateDisplay( text.replaceAll( "<br>", LINE_BREAK ).replaceAll( "<(p|blockquote)>", LINE_BREAK + LINE_BREAK ).replaceAll(
			LINE_BREAK + "(" + LINE_BREAK + ")+", LINE_BREAK + LINE_BREAK ).replaceAll( "<.*?>", "" ).replaceAll( "&nbsp;", " " ).replaceAll(
			"&trade;", " [tm]" ).replaceAll( "&ntilde;", "n" ).replaceAll( "&quot;", "" ) );
	}

	private void executeSendRequest( String parameters )
	{
		String [] splitParameters = parameters.replaceFirst( " [tT][oO] ", "\n" ).split( "\n" );

		if ( splitParameters.length != 2 )
		{
			updateDisplay( ERROR_STATE, "Invalid send request." );
			return;
		}

		Object [] attachments = getMatchingItemList( splitParameters[0], INVENTORY );
		if ( attachments.length == 0 )
			return;

		unrepeatableCommands.add( "send " + parameters );
		(new GreenMessageRequest( StaticEntity.getClient(), splitParameters[1], "You are awesome.", attachments, 0, false )).run();

		if ( StaticEntity.getClient().permitsContinue() )
			updateDisplay( "Message sent to " + splitParameters[1] );
		else
		{
			List availablePackages = GiftMessageRequest.getPackages();
			int desiredPackageIndex = Math.min( availablePackages.size() - 1, attachments.length );

			// Clear the error state for continuation on the
			// message sending attempt.

			updateDisplay( "" );

			(new GiftMessageRequest( StaticEntity.getClient(), splitParameters[1], "You are awesome.", "You are awesome.",
				availablePackages.get( desiredPackageIndex ), attachments, 0 )).run();

			if ( StaticEntity.getClient().permitsContinue() )
				updateDisplay( "Gift sent to " + splitParameters[1] );
			else
				updateDisplay( ERROR_STATE, "Failed to send message to " + splitParameters[1] );
		}
	}

	/**
	 * A special module used to handle the calling of a
	 * script.
	 */

	private void executeScriptCommand( String parameters )
	{
		parameters = parameters.replaceAll( "\\\"", "" );
		
		try
		{
			// Locate the script file.  In order of preference,
			// the script files will either have no extension
			// and be in the scripts directory, have a ".txt"
			// extension and be in the scripts directory...

			int runCount = 1;

			File scriptFile = new File( "scripts" + File.separator + parameters );
			if ( !scriptFile.exists() )
				scriptFile = new File( "scripts" + File.separator + parameters + ".txt" );
			if ( !scriptFile.exists() )
				scriptFile = new File( "scripts" + File.separator + parameters + ".ash" );
			if ( !scriptFile.exists() )
				scriptFile = new File( parameters );
			if ( !scriptFile.exists() )
				scriptFile = new File( parameters + ".txt" );
			if ( !scriptFile.exists() )
				scriptFile = new File( parameters + ".ash" );

			if ( !scriptFile.exists() )
			{
				String runCountString = parameters.split( " " )[0];

				for ( int i = 0; i < runCountString.length(); ++i )
				{
					if ( !Character.isDigit( runCountString.charAt(i) ) )
					{
						updateDisplay( ERROR_STATE, "[" + parameters + "] does not match a valid script." );
						return;
					}
				}

				runCount = Integer.parseInt( runCountString );
				String scriptName = parameters.substring( parameters.indexOf( " " ) ).trim();

				scriptFile = new File( "scripts" + File.separator + scriptName );
				if ( !scriptFile.exists() )
					scriptFile = new File( "scripts" + File.separator + scriptName + ".txt" );
				if ( !scriptFile.exists() )
					scriptFile = new File( "scripts" + File.separator + scriptName + ".ash" );
				if ( !scriptFile.exists() )
					scriptFile = new File( scriptName );
				if ( !scriptFile.exists() )
					scriptFile = new File( scriptName + ".txt" );
				if ( !scriptFile.exists() )
					scriptFile = new File( scriptName + ".ash" );
			}

			if ( !scriptFile.exists() )
			{
				// Maybe the definition of a custom command?
				// Here you would attempt to invoke the advanced
				// script handler to see what happens.

				updateDisplay( ERROR_STATE, "Script file \"" + parameters + "\" could not be found." );
				return;
			}

			if ( scriptFile.getPath().indexOf( ".ash" ) != -1 )
			{
				for ( int i = 0; i < runCount && StaticEntity.getClient().permitsContinue(); ++i )
					advancedHandler.execute( scriptFile );
			}
			else
			{
				for ( int i = 0; i < runCount && StaticEntity.getClient().permitsContinue(); ++i )
				{
					lastScript = new KoLmafiaCLI( new FileInputStream( scriptFile ) );
					lastScript.commandBuffer = commandBuffer;
					lastScript.listenForCommands();

					if ( lastScript.previousLine == null )
						lastScript = null;
				}
			}
		}
		catch ( Exception e )
		{
			// This should not happen.  Therefore, print
			// a stack trace for debug purposes.
			
			StaticEntity.printStackTrace( e );
			return;
		}
	}

	/**
	 * Utility method to handle an if-statement.  You can now
	 * nest if-statements.
	 */

	private void executeIfStatement( String parameters )
	{
		String statement = getNextLine();

		if ( statement == null )
			return;

		if ( testConditional( parameters ) )
		{
			executeLine( statement );
			return;
		}

		// Skip over every other statement which looks
		// like an if-statement.  In addition to that,
		// skip over the statement which is executed
		// after all of the nesting.

		statement = statement.toLowerCase();
		while ( statement != null && (statement.startsWith( "if" ) || statement.startsWith( "while" )) )
			statement = getNextLine().toLowerCase();
	}

	/**
	 * Utility method to handle a while-statement.  While
	 * statements cannot be nested.
	 */

	private void executeWhileStatement( String parameters )
	{
		String statement = getNextLine();

		if ( statement == null )
			return;

		while ( testConditional( parameters ) )
			executeLine( statement );
	}

	/**
	 * Utility method which tests if the given condition is true.
	 * Note that this only examines level, health, mana, items,
	 * meat and status effects.
	 */

	private boolean testConditional( String parameters )
	{
		if ( !StaticEntity.getClient().permitsContinue() )
			return false;

		// Allow checking for moon signs for stat days
		// only.  Allow test for today and tomorrow.

		Matcher dayMatcher = Pattern.compile( "(today|tomorrow) is (.*?) day" ).matcher( parameters );
		if ( dayMatcher.find() )
		{
			String statDayInformation = MoonPhaseDatabase.getMoonEffect().toLowerCase();
			return statDayInformation.startsWith( dayMatcher.group(2) + " " + dayMatcher.group(1) );
		}

		// Check for the bounty hunter's current desired
		// item list.

		if ( parameters.startsWith( "bounty hunter wants " ) )
		{
			if ( StaticEntity.getClient().hunterItems.isEmpty() )
				(new BountyHunterRequest( StaticEntity.getClient() )).run();

			String item = parameters.substring(20).trim().toLowerCase();
			for ( int i = 0; i < StaticEntity.getClient().hunterItems.size(); ++i )
				if ( ((String)StaticEntity.getClient().hunterItems.get(i)).indexOf( item ) != -1 )
					return true;

			return false;
		}

		// Check if the person is looking for whether or
		// not they are a certain class.

		if ( parameters.startsWith( "class is not " ) )
		{
			String className = parameters.substring(13).trim().toLowerCase();
			String actualClassName = KoLCharacter.getClassType().toLowerCase();
			return actualClassName.indexOf( className ) == -1;
		}

		if ( parameters.startsWith( "class is " ) )
		{
			String className = parameters.substring(9).trim().toLowerCase();
			String actualClassName = KoLCharacter.getClassType().toLowerCase();
			return actualClassName.indexOf( className ) != -1;
		}

		// Check if the person has a specific skill
		// in their available skills list.

		if ( parameters.startsWith( "skill list lacks " ) )
			return !KoLCharacter.hasSkill( getSkillName( parameters.substring(17).trim().toLowerCase() ) );

		if ( parameters.startsWith( "skill list contains " ) )
			return KoLCharacter.hasSkill( getSkillName( parameters.substring(20).trim().toLowerCase() ) );

		// Generic tests for numerical comparisons
		// involving left and right values.

		String operator = parameters.indexOf( "==" ) != -1 ? "==" : parameters.indexOf( "!=" ) != -1 ? "!=" :
			parameters.indexOf( ">=" ) != -1 ? ">=" : parameters.indexOf( "<=" ) != -1 ? "<=" :
			parameters.indexOf( "<>" ) != -1 ? "!=" : parameters.indexOf( "=" ) != -1 ? "==" :
			parameters.indexOf( ">" ) != -1 ? ">" : parameters.indexOf( "<" ) != -1 ? "<" : null;

		if ( operator == null )
			return false;

		String [] tokens = parameters.split( "[\\!<>=]" );

		String left = tokens[0].trim();
		String right = tokens[ tokens.length - 1 ].trim();

		int leftValue;
		int rightValue;

		try
		{
			leftValue = lvalue( left );
			rightValue = rvalue( left, right );
		}
		catch ( Exception e )
		{
			// This should not happen.  Therefore, print
			// a stack trace for debug purposes.
			
			updateDisplay( ERROR_STATE, parameters + " is not a valid construct." );
			return false;
		}

		return operator.equals( "==" ) ? leftValue == rightValue :
			operator.equals( "!=" ) ? leftValue != rightValue :
			operator.equals( ">=" ) ? leftValue >= rightValue :
			operator.equals( ">" ) ? leftValue > rightValue :
			operator.equals( "<=" ) ? leftValue <= rightValue :
			operator.equals( "<" ) ? leftValue < rightValue :
			false;
	}

	private int lvalue( String left )
	{
		if ( left.equals( "level" ) )
			return KoLCharacter.getLevel();

		if ( left.equals( "health" ) )
			return KoLCharacter.getCurrentHP();

		if ( left.equals( "mana" ) )
			return KoLCharacter.getCurrentMP();

		if ( left.equals( "meat" ) )
			return KoLCharacter.getAvailableMeat();

		if ( left.equals( "inebriety") || left.equals( "drunkenness") || left.equals( "drunkness"))
			return KoLCharacter.getInebriety();

		// Items first for one reason: Knob Goblin perfume
		AdventureResult item = itemParameter( left );

		if ( item != null )
			return item.getCount( KoLCharacter.getInventory() );

		AdventureResult effect = effectParameter( left );

		if ( effect != null )
			return effect.getCount( KoLCharacter.getEffects() );

		return 0;
	}

	private int rvalue( String left, String right )
	{
		if ( right.endsWith( "%" ) )
		{
			right = right.substring( 0, right.length() - 1 );
			int value = Integer.parseInt( right );

			if ( left.equals( "health" ) )
				return(int) ((double) value * (double)KoLCharacter.getMaximumHP() / 100.0);

			if ( left.equals( "mana" ) )
				return (int) ((double) value * (double)KoLCharacter.getMaximumMP() / 100.0);

			return value;
		}

		for ( int i = 0; i < right.length(); ++i )
		{
			if ( !Character.isDigit( right.charAt(i) ) )
			{
				// Items first for one reason: Knob Goblin perfume
				// Determine which item is being matched.

				AdventureResult item = itemParameter( right );

				if ( item != null )
					return item.getCount( KoLCharacter.getInventory() );

				AdventureResult effect = effectParameter( right );

				if ( effect != null )
					return effect.getCount( KoLCharacter.getEffects() );

				// If it is neither an item nor an effect, report
				// the exception.

				DEFAULT_SHELL.updateDisplay( ERROR_STATE, "Invalid operand [" + right + "] on right side of operator" );
			}
		}

		// If it gets this far, then it must be numeric,
		// so parse the number and return it.

		return Integer.parseInt( right );
	}

	private AdventureResult effectParameter( String parameter )
	{
		List potentialEffects = StatusEffectDatabase.getMatchingNames( parameter );
		if ( potentialEffects.isEmpty() )
			return null;

		return new AdventureResult(  (String) potentialEffects.get(0), 0, true );
	}

	private AdventureResult itemParameter( String parameter )
	{
		List potentialItems = TradeableItemDatabase.getMatchingNames( parameter );
		if ( potentialItems.isEmpty() )
			return null;

		return new AdventureResult( (String) potentialItems.get(0), 0, false );
	}

	/**
	 * A special module used to handle conditions requests.
	 * This determines what the user is planning to do with
	 * the condition, and then parses the condition to be
	 * added, and then adds it to the conditions list.
	 */

	public boolean executeConditionsCommand( String parameters )
	{
		AdventureResult condition = null;
		String option = parameters.split( " " )[0];

		if ( option.equals( "clear" ) )
		{
			StaticEntity.getClient().conditions.clear();
			return true;
		}
		else if ( option.equals( "check" ) )
		{
			StaticEntity.getClient().checkRequirements( StaticEntity.getClient().conditions );
			StaticEntity.getClient().conditions.clear();
			StaticEntity.getClient().conditions.addAll( StaticEntity.getClient().missingItems );

			DEFAULT_SHELL.updateDisplay( "Check complete.  Resuming request..." );

			return true;
		}
		else if ( option.equals( "mode" ) )
		{
			String conditionString = parameters.substring( option.length() ).trim();

			if ( conditionString.startsWith( "conjunction" ) || conditionString.startsWith( "and" ) )
				StaticEntity.getClient().useDisjunction = false;
			else if ( conditionString.startsWith( "disjunction" ) || conditionString.startsWith( "or" ) )
				StaticEntity.getClient().useDisjunction = true;

			if ( StaticEntity.getClient().useDisjunction )
				updateDisplay( "All conditions will be ORed together." );
			else
				updateDisplay( "All conditions will be ANDed together." );
		}
		else if ( option.equals( "add" ) )
		{
			String conditionString = parameters.substring( option.length() ).trim();

			if ( conditionString.length() == 0 )
				return true;

			if ( conditionString.endsWith( "choiceadv" ) )
			{
				// If it's a choice adventure condition, parse out the
				// number of choice adventures the user wishes to do.

				String [] splitCondition = conditionString.split( "\\s+" );
				condition = new AdventureResult( AdventureResult.CHOICE, splitCondition.length > 1 ? Integer.parseInt( splitCondition[0] ) : 1 );
			}
			else if ( conditionString.startsWith( "level" ) )
			{
				// If the condition is a level, then determine how many
				// substat points are required to the next level and
				// add the substat points as a condition.

				String [] splitCondition = conditionString.split( "\\s+" );
				int level = Integer.parseInt( splitCondition[1] );

				int [] subpoints = new int[3];
				int primeIndex = KoLCharacter.getPrimeIndex();

				subpoints[ primeIndex ] = KoLCharacter.calculateSubpoints( (level - 1) * (level - 1) + 4, 0 ) -
					KoLCharacter.getTotalPrime();

				for ( int i = 0; i < subpoints.length; ++i )
					subpoints[i] = Math.max( 0, subpoints[i] );

				condition = new AdventureResult( AdventureResult.SUBSTATS, subpoints );

				// Make sure that if there was a previous substat condition,
				// and it modifies the same stat as this condition, that the
				// greater of the two remains and the two aren't added.

				int previousIndex = StaticEntity.getClient().conditions.indexOf( condition );
				if ( previousIndex != -1 )
				{
					AdventureResult previousCondition = (AdventureResult) StaticEntity.getClient().conditions.get( previousIndex );

					for ( int i = 0; i < subpoints.length; ++i )
						if ( subpoints[i] != 0 && previousCondition.getCount(i) != 0 )
							subpoints[i] = Math.max( 0, subpoints[i] - previousCondition.getCount(i) );

					condition = new AdventureResult( AdventureResult.SUBSTATS, subpoints );
				}
			}
			else if ( conditionString.endsWith( " meat" ) )
			{
				try
				{
					String [] splitCondition = conditionString.split( "\\s+" );
					int amount = df.parse( splitCondition[0] ).intValue();

					condition = new AdventureResult( AdventureResult.MEAT, amount );
				}
				catch ( Exception e )
				{
					updateDisplay( conditionString + " is an invalid condition." );
					condition = null;
				}
			}
			else if ( conditionString.endsWith( "mus" ) || conditionString.endsWith( "muscle" ) || conditionString.endsWith( "moxie" ) ||
				conditionString.endsWith( "mys" ) || conditionString.endsWith( "myst" ) || conditionString.endsWith( "mysticality" ) )
			{
				try
				{
					String [] splitCondition = conditionString.split( "\\s+" );
					int points = df.parse( splitCondition[0] ).intValue();

					int [] subpoints = new int[3];
					int statIndex = conditionString.indexOf( "mus" ) != -1 ? 0 : conditionString.indexOf( "mys" ) != -1 ? 1 : 2;
					subpoints[ statIndex ] = KoLCharacter.calculateSubpoints( points, 0 );

					subpoints[ statIndex ] -= conditionString.indexOf( "mus" ) != -1 ? KoLCharacter.getTotalMuscle() :
						conditionString.indexOf( "mys" ) != -1 ? KoLCharacter.getTotalMysticality() :
						KoLCharacter.getTotalMoxie();

					for ( int i = 0; i < subpoints.length; ++i )
						subpoints[i] = Math.max( 0, subpoints[i] );

					condition = new AdventureResult( AdventureResult.SUBSTATS, subpoints );

					// Make sure that if there was a previous substat condition,
					// and it modifies the same stat as this condition, that the
					// greater of the two remains and the two aren't added.

					int previousIndex = StaticEntity.getClient().conditions.indexOf( condition );
					if ( previousIndex != -1 )
					{
						AdventureResult previousCondition = (AdventureResult) StaticEntity.getClient().conditions.get( previousIndex );

						for ( int i = 0; i < subpoints.length; ++i )
							if ( subpoints[i] != 0 && previousCondition.getCount(i) != 0 )
								subpoints[i] = Math.max( 0, subpoints[i] - previousCondition.getCount(i) );

						condition = new AdventureResult( AdventureResult.SUBSTATS, subpoints );
					}
				}
				catch ( Exception e )
				{
					updateDisplay( conditionString + " is an invalid condition." );
					condition = null;
				}
			}
			else if ( conditionString.endsWith( "health" ) || conditionString.endsWith( "mana" ) )
			{
				try
				{

					String numberString = conditionString.split( "\\s+" )[0];

					int points = df.parse( numberString.endsWith( "%" ) ? numberString.substring( 0, numberString.length() - 1 ) :
						numberString ).intValue();

					if ( numberString.endsWith( "%" ) )
					{
						if ( conditionString.endsWith( "health" ) )
							points = (int) ((double) points * (double)KoLCharacter.getMaximumHP() / 100.0);
						else if ( conditionString.endsWith( "mana" ) )
							points = (int) ((double) points * (double)KoLCharacter.getMaximumMP() / 100.0);
					}

					points -= conditionString.endsWith( "health" ) ? KoLCharacter.getCurrentHP() :
						KoLCharacter.getCurrentMP();

					condition = new AdventureResult( conditionString.endsWith( "health" ) ? AdventureResult.HP : AdventureResult.MP, points );

					int previousIndex = StaticEntity.getClient().conditions.indexOf( condition );
					if ( previousIndex != -1 )
					{
						AdventureResult previousCondition = (AdventureResult) StaticEntity.getClient().conditions.get( previousIndex );
						condition = condition.getInstance( condition.getCount() - previousCondition.getCount() );
					}
				}
				catch ( Exception e )
				{
					updateDisplay( conditionString + " is an invalid condition." );
					condition = null;
				}
			}
			else
			{
				// Otherwise, it's an item or status-effect condition, so parse
				// out which item or effect is desired and set that as the condition.

				condition = getFirstMatchingItem( conditionString, NOWHERE );

				// If no item exists, then it must be a status effect that the
				// player is looking for.

				if ( condition == null )
					condition = getFirstMatchingEffect( conditionString );
			}
		}

		if ( condition == null )
		{
			printList( StaticEntity.getClient().conditions );
			return false;
		}

		if ( condition.getCount() > 0 )
		{
			AdventureResult.addResultToList( StaticEntity.getClient().conditions, condition );
			updateDisplay( "Condition added." );
			printList( StaticEntity.getClient().conditions );
		}
		else
		{
			updateDisplay( "Condition already met." );
			printList( StaticEntity.getClient().conditions );
		}

		return true;
	}

	/**
	 * A special module used to handle campground requests, such
	 * as toast retrieval, resting, relaxing, and the like.
	 */

	private void executeCampgroundRequest( String parameters )
	{
		String [] parameterList = parameters.split( " " );

		try
		{
			StaticEntity.getClient().makeRequest( new CampgroundRequest( StaticEntity.getClient(), parameterList[0] ),
				parameterList.length == 1 ? 1 : df.parse( parameterList[1] ).intValue() );
		}
		catch ( Exception e )
		{
			updateDisplay( ERROR_STATE, parameterList[1] + " is not a number." );
			return;
		}
	}

	/**
	 * A special module used to handle casting skills on yourself or others.
	 * Castable skills must be listed in KoLCharacter.getUsableSkills()
	 */

	private void executeCastBuffRequest( String parameters )
	{
		String [] splitParameters = parameters.replaceFirst( " [oO][nN] ", "\n" ).split( "\n" );

		if ( splitParameters.length == 1 )
		{
			splitParameters = new String[2];
			splitParameters[0] = parameters;
			splitParameters[1] = null;
		}

		String skillNameString;
		String buffCountString;

		if ( splitParameters[0].startsWith( "\"" ) )
		{
			skillNameString = splitParameters[0].substring( 1, splitParameters[0].length() - 1 );
			buffCountString = null;
		}
		else if ( splitParameters[0].startsWith( "*" ) ||
			  Character.isDigit( splitParameters[0].charAt( 0 ) ) )
		{
			buffCountString = splitParameters[0].split( " " )[0];
			String rest = splitParameters[0].substring( buffCountString.length() ).trim();

			if ( rest.startsWith( "\"" ) )
			{
				skillNameString = rest.substring( 1, rest.length() - 1 );
			}
			else
			{
				skillNameString = rest;
			}
		}
		else
		{
			skillNameString = splitParameters[0];
			buffCountString = null;
		}

		String skillName = getUsableSkillName( skillNameString );
		if ( skillName == null )
		{
			String error;
			if ( getCombatSkillName( skillNameString ) != null )
				error = "Skill not available outside of combat";
			else if ( getSkillName( skillNameString ) != null )
				error = "Skill not castable";
			else
				error = "Skill not available";

			updateDisplay( ERROR_STATE, error );
			return;
		}

		int buffCount;
		try
		{
			if ( buffCountString == null || buffCountString.length() == 0 )
				buffCount = 1;
			else if ( buffCountString.equals( "*" ) )
				buffCount = (int) ( KoLCharacter.getCurrentMP() /
						    ClassSkillsDatabase.getMPConsumptionByID( ClassSkillsDatabase.getSkillID( skillName ) ) );
			else
				buffCount = df.parse( buffCountString ).intValue();
		}
		catch ( Exception e )
		{
			updateDisplay( ERROR_STATE, buffCountString + " is not a number." );
			return;
		}

		if ( buffCount > 0 )
		{
			if ( splitParameters[1] != null )
				unrepeatableCommands.add( "cast " + parameters );

			StaticEntity.getClient().makeRequest( new UseSkillRequest( StaticEntity.getClient(), skillName, splitParameters[1], buffCount ), 1 );
		}
	}

	/**
	 * Utility method used to retrieve the full name of a skill,
	 * given a substring representing it.
	 */

	public static String getSkillName( String substring, LockableListModel list )
	{
		UseSkillRequest [] skills = new UseSkillRequest[ list.size() ];
		list.toArray( skills );

		String name = substring.toLowerCase();
		for ( int i = 0; i < skills.length; ++i )
		{
			String skill = skills[i].getSkillName();
			if ( skill.toLowerCase().indexOf( name ) != -1 )
				return skill;
		}

		return null;
	}

	/**
	 * Utility method used to retrieve the full name of a skill,
	 * given a substring representing it.
	 */

	public static String getSkillName( String substring )
	{	return getSkillName( substring, KoLCharacter.getAvailableSkills() );
	}

	/**
	 * Utility method used to retrieve the full name of a combat skill,
	 * given a substring representing it.
	 */

	public static String getUsableSkillName( String substring )
	{	return getSkillName( substring, KoLCharacter.getUsableSkills() );
	}

	/**
	 * Utility method used to retrieve the full name of a combat skill,
	 * given a substring representing it.
	 */

	public static String getCombatSkillName( String substring )
	{	return getSkillName( substring, KoLCharacter.getCombatSkills() );
	}

	/**
	 * A special module used specifically for handling donations,
	 * including donations to the statues and donations to the clan.
	 */

	private void executeDonateCommand( String parameters )
	{
		int heroID;  int amount = -1;  int increments;

		String [] parameterList = parameters.split( " " );

		if ( parameterList[0].startsWith( "boris" ) || parameterList[0].startsWith( "mus" ) )
			heroID = HeroDonationRequest.BORIS;
		else if ( parameterList[0].startsWith( "jarl" ) || parameterList[0].startsWith( "mys" ) )
			heroID = HeroDonationRequest.JARLSBERG;
		else if ( parameterList[0].startsWith( "pete" ) || parameterList[0].startsWith( "mox" ) )
			heroID = HeroDonationRequest.PETE;
		else
		{
			updateDisplay( ERROR_STATE, parameters + " is not a statue." );
			return;
		}

		try
		{
			amount = df.parse( parameterList[1] ).intValue();
			increments = parameterList.length > 2 ? df.parse( parameterList[2] ).intValue() : 1;
		}
		catch ( Exception e )
		{
			if ( amount == -1 )
				updateDisplay( ERROR_STATE, parameterList[1] + " is not a number." );
			else
				updateDisplay( ERROR_STATE, parameterList[2] + " is not a number." );

			return;
		}

		int amountRemaining = amount;
		int eachAmount = amountRemaining / increments;

		updateDisplay( "Donating " + amount + " to the shrine..." );
		StaticEntity.getClient().makeRequest( new HeroDonationRequest( StaticEntity.getClient(), heroID, eachAmount ), increments - 1 );
		amountRemaining -= eachAmount * (increments - 1);

		if ( StaticEntity.getClient().permitsContinue() )
		{
			updateDisplay( "Request " + increments + " in progress..." );
			StaticEntity.getClient().makeRequest( new HeroDonationRequest( StaticEntity.getClient(), heroID, amountRemaining ), 1 );

			if ( StaticEntity.getClient().permitsContinue() )
				updateDisplay( "Requests complete!" );
		}
	}

	/**
	 * A special module used specifically for equipping items.
	 */

	private void executeEquipCommand( String parameters )
	{
		if ( parameters.length() == 0 )
		{
			executePrintCommand( "equipment" );
			return;
		}

		if ( parameters.startsWith( "list" ) )
		{
			executePrintCommand( "equipment " + parameters.substring(4).trim() );
			return;
		}

		// Look for name of slot
		int slot = -1;

		String command = parameters.split( " " )[0];
		for ( int i = 0; i < EquipmentRequest.slotNames.length; ++i )
			if ( command.equals( EquipmentRequest.slotNames[i] ) )
			{
				parameters = parameters.substring( command.length() ).trim();
				slot = i;
				break;
			}

		AdventureResult match = getFirstMatchingItem( parameters, NOWHERE );
		if ( match == null )
		{
			// No item exists which matches the given
			// substring - error out.

			updateDisplay( ERROR_STATE, "No item matching substring \"" + match + "\"" );
			return;
		}

		// Take advantage of KoLmafia's built-in familiar item
		// switching.

		StaticEntity.getClient().makeRequest( new EquipmentRequest( StaticEntity.getClient(), match.getName(), slot ), 1 );
	}

	/**
	 * A special module used specifically for  unequipping items.
	 */

	private void executeUnequipCommand( String parameters )
	{
		// Look for name of slot
		String command = parameters.split( " " )[0];

		for ( int i = 0; i < EquipmentRequest.slotNames.length; ++i )
			if ( command.equals( EquipmentRequest.slotNames[i] ) )
			{
				StaticEntity.getClient().makeRequest( new EquipmentRequest( StaticEntity.getClient(), EquipmentRequest.UNEQUIP, i ), 1 );
				return;
			}

		parameters = parameters.toLowerCase();

		// Allow player to remove all of his fake hands
		if ( parameters.equals( "fake hand" ) )
		{
			if ( KoLCharacter.getFakeHands() == 0 )
				DEFAULT_SHELL.updateDisplay( ERROR_STATE, "You're not wearing any fake hands" );
			else
				StaticEntity.getClient().makeRequest( new EquipmentRequest( StaticEntity.getClient(), EquipmentRequest.UNEQUIP, KoLCharacter.FAKEHAND ), 1 );
			return;
		}

		// The following loop removes the first item with
		// the specified name.

		for ( int i = 0; i <= KoLCharacter.FAMILIAR; ++i )
		{
			if ( KoLCharacter.getCurrentEquipmentName( i ) != null && KoLCharacter.getCurrentEquipmentName( i ).indexOf( parameters ) != -1 )
			{
				StaticEntity.getClient().makeRequest( new EquipmentRequest( StaticEntity.getClient(), EquipmentRequest.UNEQUIP, i ), 1 );
				return;
			}
		}

		DEFAULT_SHELL.updateDisplay( ERROR_STATE, "No equipment found matching string \"" + parameters + "\"" );
		return;
	}

	/**
	 * A special module used specifically for properly printing out
	 * data relevant to the current session.
	 */

	private void executePrintCommand( String parameters )
	{
		if ( parameters.length() == 0 )
		{
			updateDisplay( ERROR_STATE, "Print what?" );
			return;
		}

		String [] parameterList = parameters.split( " " );

		String filter = "";
		PrintStream desiredOutputStream;

		if ( parameterList.length > 1 && parameterList[1].equals( "filter" ) )
		{
			filter = parameters.substring( parameters.indexOf( "filter" ) + 6 ).toLowerCase().trim();
			desiredOutputStream = outputStream;
		}
		else if ( parameterList.length > 1 )
		{
			File outputFile = new File( parameterList[1] );
			outputFile = new File( outputFile.getAbsolutePath() );

			// If the output file does not exist, create it first
			// to avoid FileNotFoundExceptions being thrown.

			try
			{
				if ( !outputFile.exists() )
				{
					outputFile.getParentFile().mkdirs();
					outputFile.createNewFile();
				}

				desiredOutputStream = new PrintStream( new FileOutputStream( outputFile, true ), true );
			}
			catch ( IOException e )
			{
				// This should not happen.  Therefore, print
				// a stack trace for debug purposes.
				
				StaticEntity.printStackTrace( e, "Error opening file <" + parameterList[1] + ">" );
				return;
			}
		}
		else
			desiredOutputStream = outputStream;

		executePrintCommand( parameterList[0].toLowerCase(), filter, desiredOutputStream );

		if ( parameterList.length > 1 && !parameterList[1].equals( "filter" ) )
			updateDisplay( "Data has been printed to \"" + parameterList[1] + "\"" );
	}

	/**
	 * A special module used specifically for properly printing out
	 * data relevant to the current session.  This method is more
	 * specialized than its counterpart and is used when the data
	 * to be printed is known, as well as the stream to print to.
	 * Usually called by its counterpart to handle specific instances.
	 */

	private void executePrintCommand( String desiredData, String filter, PrintStream outputStream )
	{
		PrintStream originalStream = this.outputStream;
		this.outputStream = outputStream;

		if ( desiredData.equals( "session" ) )
		{
			printLine( "Player: " + KoLCharacter.getUsername() );
			printLine( "Session ID: " + StaticEntity.getClient().getSessionID() );
			printLine( "Password Hash: " + StaticEntity.getClient().getPasswordHash() );
		}
		else if ( desiredData.startsWith( "stat" ) )
		{
			printLine( "Lv: " + KoLCharacter.getLevel() );
			printLine( "HP: " + KoLCharacter.getCurrentHP() + " / " + df.format( KoLCharacter.getMaximumHP() ) );
			printLine( "MP: " + KoLCharacter.getCurrentMP() + " / " + df.format( KoLCharacter.getMaximumMP() ) );

			printBlankLine();

			printLine( "Mus: " + getStatString( KoLCharacter.getBaseMuscle(), KoLCharacter.getAdjustedMuscle(), KoLCharacter.getMuscleTNP() ) );
			printLine( "Mys: " + getStatString( KoLCharacter.getBaseMysticality(), KoLCharacter.getAdjustedMysticality(), KoLCharacter.getMysticalityTNP() ) );
			printLine( "Mox: " + getStatString( KoLCharacter.getBaseMoxie(), KoLCharacter.getAdjustedMoxie(), KoLCharacter.getMoxieTNP() ) );

			printBlankLine();

			printLine( "Advs: " + KoLCharacter.getAdventuresLeft() );
			printLine( "Meat: " + df.format( KoLCharacter.getAvailableMeat() ) );
			printLine( "Drunk: " + KoLCharacter.getInebriety() );

			printBlankLine();

			printLine( "Pet: " + KoLCharacter.getFamiliar() );
			printLine( "Item: " + KoLCharacter.getFamiliarItem() );
		}
		else if ( desiredData.startsWith( "equip" ) )
		{
			printLine( "Hat: " + KoLCharacter.getEquipment( KoLCharacter.HAT ) );
			printLine( "Weapon: " + KoLCharacter.getEquipment( KoLCharacter.WEAPON ) );
			int fakeHands = KoLCharacter.getFakeHands();
			for ( int i = 0; i < fakeHands; ++i )
				printLine( "Off-hand: fake hand" );
			printLine( "Off-hand: " + KoLCharacter.getEquipment( KoLCharacter.OFFHAND ) );
			printLine( "Shirt: " + KoLCharacter.getEquipment( KoLCharacter.SHIRT ) );
			printLine( "Pants: " + KoLCharacter.getEquipment( KoLCharacter.PANTS ) );

			printBlankLine();

			printLine( "Acc. 1: " + KoLCharacter.getEquipment( KoLCharacter.ACCESSORY1 ) );
			printLine( "Acc. 2: " + KoLCharacter.getEquipment( KoLCharacter.ACCESSORY2 ) );
			printLine( "Acc. 3: " + KoLCharacter.getEquipment( KoLCharacter.ACCESSORY3 ) );

			printBlankLine();

			printLine( "Pet: " + KoLCharacter.getFamiliar() );
			printLine( "Item: " + KoLCharacter.getFamiliarItem() );
		}
		else if ( desiredData.startsWith( "encounters" ) )
		{
			printLine( "Visited Locations: " );
			printBlankLine();

			printList( StaticEntity.getClient().adventureList );

			printBlankLine();
			printBlankLine();

			updateDisplay( "Encounter Listing: " );

			printBlankLine();

			printList( StaticEntity.getClient().encounterList );
		}
		else
		{
			List mainList = desiredData.equals( "closet" ) ? KoLCharacter.getCloset() : desiredData.equals( "summary" ) ? StaticEntity.getClient().tally :
				desiredData.equals( "outfits" ) ? KoLCharacter.getOutfits() : desiredData.equals( "familiars" ) ? KoLCharacter.getFamiliarList() :
				desiredData.equals( "effects" ) ? KoLCharacter.getEffects() : KoLCharacter.getInventory();

			String currentItem;
			List resultList = new ArrayList();
			Object [] items = new Object[ mainList.size() ];
			mainList.toArray( items );

			for ( int i = 0; i < items.length; ++i )
			{
				currentItem = items[i].toString().toLowerCase();
				if ( currentItem.indexOf( filter ) != -1 )
					resultList.add( currentItem );
			}

			printList( resultList );
		}

		if ( this.outputStream != originalStream )
		{
			this.outputStream.println();
			this.outputStream.println();
			this.outputStream.close();
		}

		this.outputStream = originalStream;
	}

	private static String getStatString( int base, int adjusted, int tnp )
	{
		StringBuffer statString = new StringBuffer();
		statString.append( df.format( adjusted ) );

		if ( base != adjusted )
			statString.append( " (" + df.format( base ) + ")" );

		statString.append( ", tnp = " );
		statString.append( df.format( tnp ) );

		return statString.toString();
	}

	/**
	 * Utility method which determines the first effect which matches
	 * the given parameter string.  Note that the string may also
	 * specify an effect duration before the string.
	 */

	private AdventureResult getFirstMatchingEffect( String parameters )
	{
		String effectName = null;
		int duration = 0;

		// First, allow for the person to type without specifying
		// the amount, if the amount is 1.

		List matchingNames = StatusEffectDatabase.getMatchingNames( parameters );

		if ( matchingNames.size() != 0 )
		{
			effectName = (String) matchingNames.get(0);
			duration = 1;
		}
		else
		{
			String durationString = parameters.split( " " )[0];
			String effectNameString = parameters.substring( durationString.length() ).trim();

			matchingNames = StatusEffectDatabase.getMatchingNames( effectNameString );

			if ( matchingNames.size() == 0 )
			{
				updateDisplay( ERROR_STATE, "[" + effectNameString + "] does not match anything in the status effect database." );
				return null;
			}

			try
			{
				effectName = (String) matchingNames.get(0);
				duration = durationString.equals( "*" ) ? 0 : df.parse( durationString ).intValue();
			}
			catch ( Exception e )
			{
				updateDisplay( ERROR_STATE, durationString + " is not a number." );
				return null;
			}
		}

		if ( effectName == null )
		{
			updateDisplay( ERROR_STATE, "[" + parameters + "] does not match anything in the status effect database." );
			return null;
		}

		return new AdventureResult( effectName, duration, true );
	}

	public static int getFirstMatchingItemID( List nameList, int matchType )
	{
		if ( nameList.isEmpty() )
			return -1;

		List source = null;

		switch ( matchType )
		{
			case NOWHERE:
			case CREATION:
				return TradeableItemDatabase.getItemID( (String) nameList.get(0) );

			case INVENTORY:
				source = KoLCharacter.getInventory();
				break;

			case CLOSET:
				source = KoLCharacter.getCloset();
				break;
		}

		AdventureResult currentItem = null;
		for ( int i = 0; i < nameList.size(); ++i )
		{
			currentItem = new AdventureResult( (String) nameList.get(i), 0, false );
			if ( source.contains( currentItem ) )
				return currentItem.getItemID();
		}

		return currentItem.getItemID();
	}

	/**
	 * Utility method which determines the first item which matches
	 * the given parameter string.  Note that the string may also
	 * specify an item quantity before the string.
	 */

	public static AdventureResult getFirstMatchingItem( String parameters, int matchType, int defaultCount )
	{
		int itemID = -1;
		int itemCount = 0;

		// First, allow for the person to type without specifying
		// the amount, if the amount is 1.

		List matchingNames = TradeableItemDatabase.getMatchingNames( parameters );

		// Next, check to see if any of the items matching appear
		// in an NPC store.  If so, automatically default to it.

		String [] matchingNamesArray = new String[ matchingNames.size() ];
		matchingNames.toArray( matchingNamesArray );

		for ( int i = 0; i < matchingNamesArray.length; ++i )
		{
			if ( NPCStoreDatabase.contains( matchingNamesArray[i] ) )
			{
				matchingNames.clear();
				matchingNames.add( matchingNamesArray[i] );
				break;
			}
		}

		if ( matchingNames.size() != 0 )
		{
			itemID = getFirstMatchingItemID( matchingNames, matchType );
			itemCount = defaultCount;
		}
		else
		{
			String itemCountString = parameters.split( " " )[0];
			String itemNameString = parameters.substring( itemCountString.length() ).trim();

			matchingNames = TradeableItemDatabase.getMatchingNames( itemNameString );

			if ( matchingNames.size() == 0 )
			{
				DEFAULT_SHELL.updateDisplay( ERROR_STATE, "[" + itemNameString + "] does not match anything in the item database." );
				return null;
			}

			itemID = getFirstMatchingItemID( matchingNames, matchType );

			// Make sure what you're attempting to parse is a
			// number -- if it's not, then the person was trying
			// to match the full substring.

			if ( itemCountString.equals( "*" ) )
			{
				itemCount = 0;
			}
			else
			{
				for ( int i = 0; i < itemCountString.length(); ++i )
				{
					char c = itemCountString.charAt(i);
					if ( !Character.isDigit( c ) && c != '-' && c != '+' )
					{
						DEFAULT_SHELL.updateDisplay( ERROR_STATE, "[" + parameters + "] does not match anything in the item database." );
						return null;
					}
				}

				try
				{
					itemCount = df.parse( itemCountString ).intValue();
				}
				catch ( Exception e )
				{
					DEFAULT_SHELL.updateDisplay( ERROR_STATE, itemCountString + " is not a number." );
					return null;
				}
			}
		}

		if ( itemID == -1 )
		{
			DEFAULT_SHELL.updateDisplay( ERROR_STATE, "[" + parameters + "] does not match anything in the item database." );
			return null;
		}

		AdventureResult firstMatch = new AdventureResult( itemID, itemCount );

		// The result also depends on the number of items which
		// are available in the given match area.

		int matchCount;

		if ( matchType == CREATION )
		{
			ConcoctionsDatabase.refreshConcoctions();
			ItemCreationRequest request = ItemCreationRequest.getInstance( StaticEntity.getClient(), firstMatch );
			matchCount = request == null ? 0 : request.getCount( ConcoctionsDatabase.getConcoctions() );
		}
		else if ( matchType == CLOSET )
			matchCount = firstMatch.getCount( KoLCharacter.getCloset() );
		else if ( matchType == INVENTORY )
			matchCount = firstMatch.getCount( KoLCharacter.getInventory() );
		else
			matchCount = 0;

		// In the event that the person wanted all except a certain
		// quantity, be sure to update the item count.

		if ( itemCount <= 0 )
		{
			itemCount = matchCount + itemCount;
			firstMatch = firstMatch.getInstance( itemCount );
		}

		// Allow for attempts to purchase missing items as well as
		// attempts to create missing items, if the desired location
		// is the player's inventory.

		if ( matchType == INVENTORY )
		{
			AdventureDatabase.retrieveItem( firstMatch );
			if ( !StaticEntity.getClient().permitsContinue() )
				return null;
		}

		// If the number matching is less than the quantity desired,
		// then be sure to create an error state.

		else if ( matchType == CLOSET && matchCount < itemCount )
		{
			DEFAULT_SHELL.updateDisplay( ERROR_STATE, "Insufficient " + TradeableItemDatabase.getItemName( itemID ) + " to continue." );
			return null;
		}

		return itemCount <= 0 ? null : firstMatch;
	}

	public static AdventureResult getFirstMatchingItem( String parameters, int matchType )
	{	return getFirstMatchingItem( parameters, matchType, 1 );
	}

	public static AdventureResult getFirstMatchingItem( String parameters )
	{	return getFirstMatchingItem( parameters, INVENTORY, 1 );
	}

	/**
	 * A special module used specifically for properly instantiating
	 * ClanStorageRequests which send things to the clan stash.
	 */

	private void executeStashRequest( String parameters )
	{
		boolean isWithdraw = false;

		int space = parameters.indexOf( " " );
		if ( space != -1 )
		{
			String command = parameters.substring( 0, space );
			if ( command.equals( "take" ) )
			{
				isWithdraw = true;
				parameters = parameters.substring( 4 ).trim();
			}
			else if ( command.equals( "put" ) )
				parameters = parameters.substring( 3 ).trim();
		}

		Object [] items = getMatchingItemList( parameters, isWithdraw ? NOWHERE : INVENTORY );
		if ( items.length == 0 )
			return;

		StaticEntity.getClient().makeRequest( new ClanStashRequest( StaticEntity.getClient(), items, isWithdraw ?
			ClanStashRequest.STASH_TO_ITEMS : ClanStashRequest.ITEMS_TO_STASH ), 1 );
	}

	/**
	 * Untinkers an item (not specified).  This is generally not
	 * used by the CLI interface, but is defined to override the
	 * abstract method provided in the KoLmafia class.
	 */

	public void makeUntinkerRequest()
	{
		if ( previousLine.indexOf( " " ) == -1 )
		{
			StaticEntity.getClient().makeRequest( new UntinkerRequest( StaticEntity.getClient() ), 1 );
			return;
		}

		String item = previousLine.substring( previousLine.split( " " )[0].length() ).trim();
		AdventureResult firstMatch = getFirstMatchingItem( item, INVENTORY );
		if ( firstMatch == null )
			return;

		StaticEntity.getClient().makeRequest( new UntinkerRequest( StaticEntity.getClient(), firstMatch.getItemID() ), 1 );
	}

	/**
	 * Set the Canadian Mind Control device to selected setting.
	 */

	public void makeMindControlRequest()
	{
		try
		{
			String [] command = previousLine.split( " " );

			int setting = df.parse( command[1] ).intValue();
			StaticEntity.getClient().makeRequest( new MindControlRequest( StaticEntity.getClient(), setting ), 1 );
		}
		catch ( Exception e )
		{
			updateDisplay( ERROR_STATE, "Invalid setting for device." );
		}
	}

	/**
	 * Train the player's current familiar
	 */

	private void trainFamiliar( String parameters )
	{
		// train (base | buffed | turns) <goal> yes?
		String [] split = parameters.split( " " );

		if ( split.length < 2 || split.length > 3 )
		{
			updateDisplay( ERROR_STATE, "Syntax: train type goal buff?" );
			return;
		}

		String typeString = split[0];
		int type;

		if ( typeString.equals( "base" ) )
			type = FamiliarTrainingFrame.BASE;
		else if ( typeString.equals( "buffed" ) )
			type = FamiliarTrainingFrame.BUFFED;
		else if ( typeString.equals( "turns" ) )
			type = FamiliarTrainingFrame.TURNS;
		else
		{
			updateDisplay( ERROR_STATE, "Unknown training type: " + typeString );
			return;
		}

		String goalString = split[1];
		int goal = Integer.parseInt( goalString );

		boolean buffs = ( split.length == 3 && split[2].equals( "yes" ) );

		FamiliarTrainingFrame.levelFamiliar( goal, type, buffs, false );
	}

	/**
	 * Show the current state of the player's mushroom plot
	 */

	private void executeMushroomCommand( String parameters )
	{
		String [] split = parameters.split( " " );
		String command = split[0];

		if ( command.equals( "plant" ) )
		{
			if ( split.length < 3 )
			{
				updateDisplay( ERROR_STATE, "Syntax: field plant square spore" );
				return;
			}

			String squareString = split[1];
			int square;

			try
			{
				square = df.parse( squareString ).intValue();
			}
			catch  ( Exception e )
			{
				updateDisplay( ERROR_STATE, squareString + " is not a number." );

				e.printStackTrace( KoLmafia.getLogStream() );
				e.printStackTrace();

				return;
			}

			// Skip past command and square
			parameters = parameters.substring( command.length() ).trim();
			parameters = parameters.substring( squareString.length() ).trim();

			int spore = TradeableItemDatabase.getItemID( parameters );

			if ( spore == -1 )
			{
				updateDisplay( ERROR_STATE, "Unknown spore: " + parameters);
				return;
			}

			MushroomPlot.plantMushroom( square, spore );
		}
		else if ( command.equals( "pick" ) )
		{
			if ( split.length < 2 )
			{
				updateDisplay( ERROR_STATE, "Syntax: field pick square" );
				return;
			}

			String squareString = split[1];

			try
			{
				int square = df.parse( squareString ).intValue();
				MushroomPlot.pickMushroom( square, true );
			}
			catch  ( Exception e )
			{
				updateDisplay( ERROR_STATE, squareString + " is not a number." );

				e.printStackTrace( KoLmafia.getLogStream() );
				e.printStackTrace();

				return;
			}
		}
		else if ( command.equals( "harvest" ) )
			MushroomPlot.harvestMushrooms();

		String plot = MushroomPlot.getMushroomPlot( false );

		if ( StaticEntity.getClient().permitsContinue() )
		{
			updateDisplay( "Current:" );
			updateDisplay( plot );
			updateDisplay( " " );
			updateDisplay( "Forecast:" );
			updateDisplay( MushroomPlot.getForecastedPlot( false ) );
			updateDisplay( " " );
		}
	}

	public Object [] getMatchingItemList( String itemList, int location )
	{
		String [] itemNames = itemList.split( "\\s*,\\s*" );

		AdventureResult firstMatch = null;
		ArrayList items = new ArrayList();

		for ( int i = 0; i < itemNames.length; ++i )
		{
			try
			{
				if ( itemNames[i].endsWith( "meat" ) )
				{
					String amountString = itemNames[i].split( " " )[0];
					int amount = amountString.equals( "*" ) ? 0 : df.parse( amountString ).intValue();
					firstMatch = new AdventureResult( AdventureResult.MEAT, amount > 0 ? amount : KoLCharacter.getAvailableMeat() + amount );

				}
				else
					firstMatch = getFirstMatchingItem( itemNames[i], location );
			}
			catch ( Exception e )
			{
				updateDisplay( ERROR_STATE, itemNames[i] + " is not a valid list element." );
				firstMatch = null;
			}

			if ( firstMatch != null )
				AdventureResult.addResultToList( items, firstMatch );
		}

		return items.toArray();
	}

	/**
	 * A special module used specifically for properly instantiating
	 * ItemStorageRequests which pulls things from Hagnk's.
	 */

	private void executeHagnkRequest( String parameters )
	{
		Object [] items = getMatchingItemList( parameters, NOWHERE );
		if ( items.length == 0 )
			return;

		StaticEntity.getClient().makeRequest( new ItemStorageRequest( StaticEntity.getClient(),
			ItemStorageRequest.STORAGE_TO_INVENTORY, items ), 1 );
	}

	/**
	 * A special module used specifically for properly instantiating
	 * ItemManageRequests which manage the closet.
	 */

	private void executeClosetManageRequest( String parameters )
	{
		if ( !parameters.startsWith( "take" ) && !parameters.startsWith( "put" ) )
		{
			updateDisplay( ERROR_STATE, "Invalid closet command." );
			return;
		}

		Object [] items = getMatchingItemList( parameters.substring(4).trim(), parameters.startsWith( "take" ) ? CLOSET : INVENTORY );
		if ( items.length == 0 )
			return;

		StaticEntity.getClient().makeRequest( new ItemStorageRequest( StaticEntity.getClient(),
			parameters.startsWith( "take" ) ? ItemStorageRequest.CLOSET_TO_INVENTORY : ItemStorageRequest.INVENTORY_TO_CLOSET, items ), 1 );
	}

	/**
	 * A special module used specifically for properly instantiating
	 * AutoSellRequests which send things to the mall.
	 */

	private void executeAutoMallRequest( String parameters )
	{
		String [] tokens = parameters.split( " " );
		StringBuffer itemName = new StringBuffer();

		itemName.append( '*' );
		for ( int i = 0; i < tokens.length - 2; ++i )
		{
			itemName.append( ' ' );
			itemName.append( tokens[i] );
		}

		AdventureResult firstMatch = getFirstMatchingItem( itemName.toString(), INVENTORY );
		if ( firstMatch == null )
			return;

		try
		{
			StaticEntity.getClient().makeRequest( new AutoSellRequest( StaticEntity.getClient(), firstMatch,
				df.parse( tokens[ tokens.length - 2 ] ).intValue(), df.parse( tokens[ tokens.length - 1 ] ).intValue() ), 1 );
		}
		catch ( Exception e )
		{
			updateDisplay( ERROR_STATE, "Invalid price/limit for automall request." );
			return;
		}
	}

	/**
	 * A special module used specifically for properly instantiating
	 * AutoSellRequests for just autoselling items.
	 */

	private void executeAutoSellRequest( String parameters )
	{
		Object [] items = getMatchingItemList( parameters, INVENTORY );
		if ( items.length == 0 )
			return;

		StaticEntity.getClient().makeRequest( new AutoSellRequest( StaticEntity.getClient(), items, AutoSellRequest.AUTOSELL ), 1 );
	}

	/**
	 * Utility method used to make a purchase from the
	 * Kingdom of Loathing mall.  What this does is
	 * create a mall search request, and buys the
	 * given quantity of items.
	 */

	public void executeBuyCommand( String parameters )
	{
		AdventureResult firstMatch = getFirstMatchingItem( parameters, NOWHERE );
		if ( firstMatch == null )
			updateDisplay( ERROR_STATE, "No item specified for purchase." );

		if ( !NPCStoreDatabase.contains( firstMatch.getName() ) && !KoLCharacter.canInteract() )
		{
			updateDisplay( ERROR_STATE, "You are not yet out of ronin." );
			return;
		}

		ArrayList results = new ArrayList();
		(new SearchMallRequest( StaticEntity.getClient(), '\"' + firstMatch.getName() + '\"', 0, results )).run();
		StaticEntity.getClient().makePurchases( results, results.toArray(), firstMatch.getCount() );
	}

	/**
	 * A special module used specifically for properly instantiating
	 * ItemCreationRequests.
	 */

	private void executeItemCreationRequest( String parameters )
	{
		if ( parameters.equals( "" ) )
		{
			printList( ConcoctionsDatabase.getConcoctions() );
			return;
		}

		int itemID;  int mixingMethod;  int quantityNeeded;

		AdventureResult firstMatch = getFirstMatchingItem( parameters, CREATION );
		if ( firstMatch == null )
			return;

		ItemCreationRequest irequest = ItemCreationRequest.getInstance( StaticEntity.getClient(), firstMatch );
		if ( irequest == null )
		{
			boolean needServant = StaticEntity.getProperty( "createWithoutBoxServants" ).equals( "false" );

			switch ( ConcoctionsDatabase.getMixingMethod( firstMatch.getItemID() ) )
			{
				case ItemCreationRequest.COOK:
				case ItemCreationRequest.COOK_REAGENT:
				case ItemCreationRequest.COOK_PASTA:

					if ( needServant )
						updateDisplay( ERROR_STATE, "You cannot cook without a chef-in-the-box." );
					else
						updateDisplay( ERROR_STATE, "You cannot cook without an oven." );

					break;

				case ItemCreationRequest.MIX:
				case ItemCreationRequest.MIX_SPECIAL:

					if ( needServant )
						updateDisplay( ERROR_STATE, "You cannot mix without a bartender-in-the-box." );
					else
						updateDisplay( ERROR_STATE, "You cannot mix without a cocktail crafting kit." );

					break;

				default:

					updateDisplay( ERROR_STATE, "That item cannot be created." );
					break;
			}

			return;
		}

		StaticEntity.getClient().makeRequest( irequest, 1 );
	}

	/**
	 * A special module used specifically for properly instantiating
	 * ConsumeItemRequests.
	 */

	private void executeConsumeItemRequest( String parameters )
	{
		int consumptionType;  String itemName;  int itemCount;

		// Now, handle the instance where the first item is actually
		// the quantity desired, and the next is the amount to use

		AdventureResult firstMatch = getFirstMatchingItem( parameters, INVENTORY );
		if ( firstMatch == null )
			return;

		itemName = firstMatch.getName();
		itemCount = firstMatch.getCount();

		consumptionType = TradeableItemDatabase.getConsumptionType( itemName );

		if ( itemCount == 1 || consumptionType == ConsumeItemRequest.CONSUME_MULTIPLE || consumptionType == ConsumeItemRequest.CONSUME_RESTORE )
			StaticEntity.getClient().makeRequest( new ConsumeItemRequest( StaticEntity.getClient(), new AdventureResult( itemName, itemCount, false ) ), 1 );
		else
			StaticEntity.getClient().makeRequest( new ConsumeItemRequest( StaticEntity.getClient(), new AdventureResult( itemName, 1, false ) ), itemCount );
	}

	/**
	 * A special module for instantiating display case management requests,
	 * strictly for adding and removing things.
	 */
	
	private void executeDisplayCaseRequest( String parameters )
	{
		Object [] items = getMatchingItemList( parameters.substring(4).trim(), parameters.startsWith( "take" ) ? NOWHERE : INVENTORY );
		if ( items.length == 0 )
			return;

		StaticEntity.getClient().makeRequest( new MuseumRequest( StaticEntity.getClient(), items, !parameters.startsWith( "take" ) ), 1 );
	}

	/**
	 * A special module used specifically for properly instantiating
	 * AdventureRequests, including HermitRequests, if necessary.
	 */

	private void executeAdventureRequest( String parameters )
	{
		int adventureCount;
		KoLAdventure adventure = AdventureDatabase.getAdventure( parameters );

		if ( adventure != null )
			adventureCount = 1;
		else
		{
			String adventureCountString = parameters.split( " " )[0];
			String adventureName = parameters.substring( adventureCountString.length() ).trim();
			adventure = AdventureDatabase.getAdventure( adventureName );

			if ( adventure == null )
			{
				updateDisplay( ERROR_STATE, parameters + " does not exist in the adventure database." );
				return;
			}

			try
			{
				adventureCount = adventureCountString.equals( "*" ) ? 0 : df.parse( adventureCountString ).intValue();

				if ( adventureCount <= 0 && adventure.getFormSource().equals( "shore.php" ) )
					adventureCount += (int) Math.floor( KoLCharacter.getAdventuresLeft() / 3 );
				else if ( adventureCount <= 0 )
					adventureCount += KoLCharacter.getAdventuresLeft();
			}
			catch ( Exception e )
			{
				// Technically, this exception should not be thrown, but if
				// it is, then print an error message and return.

				updateDisplay( ERROR_STATE, adventureCountString + " is not a number." );
				return;
			}
		}

		updateDisplay( "Beginning " + adventureCount + " turnips to " + adventure.toString() + "..." );
		StaticEntity.getClient().makeRequest( adventure, adventureCount );
	}

	/**
	 * Special module used specifically for properly instantiating
	 * requests to change the user's outfit.
	 */

	private void executeChangeOutfitCommand( String parameters )
	{
		String lowercaseOutfitName = parameters.toLowerCase().trim();
		Object [] outfits = new Object[ KoLCharacter.getOutfits().size() ];
		KoLCharacter.getOutfits().toArray( outfits );
		SpecialOutfit intendedOutfit = null;

		for ( int i = 0; intendedOutfit == null && i < outfits.length; ++i )
			if ( outfits[i] instanceof SpecialOutfit && outfits[i].toString().toLowerCase().indexOf( lowercaseOutfitName ) != -1 )
				intendedOutfit = (SpecialOutfit) outfits[i];

		if ( intendedOutfit == null )
		{
			updateDisplay( ERROR_STATE, "You can't wear that outfit." );
			return;
		}

		(new EquipmentRequest( StaticEntity.getClient(), intendedOutfit )).run();
	}
	/**
	 * A special module used specifically for properly instantiating
	 * the BuffBot and running it
	 */

	private void executeBuffBotCommand( String parameters )
	{
		BuffBotHome.reset();
		BuffBotManager.reset();

		if ( BuffBotManager.getBuffCostTable().isEmpty() )
		{
			updateDisplay( ERROR_STATE, "No sellable buffs defined." );
			return;
		}

		try
		{
			BuffBotHome.setBuffBotActive( true );
			BuffBotManager.runBuffBot( df.parse( parameters ).intValue() );
			updateDisplay( "Buffbot execution complete." );
		}
		catch ( Exception e )
		{
			// This should not happen.  Therefore, print
			// a stack trace for debug purposes.
			
			StaticEntity.printStackTrace( e, "Error opening file <" + parameters + ">" );
			return;
		}
	}

	/**
	 * Attempts to remove the effect specified in the most recent command.
	 * If the string matches multiple effects, all matching effects will
	 * be removed.
	 */

	public void makeUneffectRequest()
	{
		AdventureResult currentEffect;
		String effectToUneffect = previousLine.trim().substring( previousLine.split( " " )[0].length() ).trim().toLowerCase();

		AdventureResult [] effects = new AdventureResult[ KoLCharacter.getEffects().size() ];
		KoLCharacter.getEffects().toArray( effects );

		for ( int i = 0; i < effects.length; ++i )
			if ( effects[i].getName().toLowerCase().indexOf( effectToUneffect ) != -1 )
				(new UneffectRequest( StaticEntity.getClient(), effects[i] )).run();
	}

	/**
	 * Attempts to zap the specified item with the specified wand
	 */

	public void makeZapRequest()
	{
		AdventureResult wand = KoLCharacter.getZapper();

		if ( wand == null )
		{
			updateDisplay( ERROR_STATE, "You don't have an appropriate wand." );
			return;
		}

		String command = previousLine.split( " " )[0];
		String parameters = previousLine.substring( command.length() ).trim();
		if ( parameters.length() == 0 )
		{
			updateDisplay( ERROR_STATE, "Zap what?" );
			return;
		}

		AdventureResult item = getFirstMatchingItem( parameters, INVENTORY );
		if ( item == null )
			return;

		(new ZapRequest( StaticEntity.getClient(), wand, item )).run();
	}

	public boolean isCloverDay()
	{
		if ( !StaticEntity.getClient().hermitItems.contains( "ten-leaf clover" ) )
			(new HermitRequest( StaticEntity.getClient() )).run();

		return StaticEntity.getClient().hermitItems.contains( "ten-leaf clover" );
	}

	/**
	 * Retrieves the items specified in the most recent command.  If there
	 * are no clovers available, the request will abort.
	 */

	public void makeHermitRequest()
	{
		String oldLine = previousLine;
		boolean clovers = isCloverDay();

		if ( !StaticEntity.getClient().permitsContinue() )
			return;

		if ( previousLine.indexOf( " " ) == -1 )
		{
			updateDisplay( "Today is " + ( clovers ? "" : "not " ) + "a clover day." );
			return;
		}

		String command = oldLine.split( " " )[0];
		String parameters = oldLine.substring( command.length() ).trim();

		int itemID = -1;
		int tradeCount = 1;

		List matchingNames = TradeableItemDatabase.getMatchingNames( parameters );
		if ( matchingNames.isEmpty() )
		{
			String itemCountString = parameters.split( " " )[0];
			String itemNameString = parameters.substring( itemCountString.length() ).trim();
			matchingNames = TradeableItemDatabase.getMatchingNames( itemNameString );

			if ( matchingNames.isEmpty() )
			{
				updateDisplay( ERROR_STATE, "[" + itemNameString + "] does not match anything in the item database." );
				return;
			}

			try
			{
				tradeCount = df.parse( itemCountString ).intValue();
			}
			catch ( Exception e )
			{
				updateDisplay( ERROR_STATE, itemCountString + " is not a number." );
				return;
			}
		}

		for ( int i = 0; i < matchingNames.size(); ++i )
		{
			if ( StaticEntity.getClient().hermitItems.contains( KoLDatabase.getDisplayName( (String) matchingNames.get(i) ) ) )
				itemID = TradeableItemDatabase.getItemID( (String) matchingNames.get(i) );
		}

		if ( itemID == -1 )
		{
			updateDisplay( ERROR_STATE, "You can't get " + parameters + " from the hermit today." );
			return;
		}

		(new HermitRequest( StaticEntity.getClient(), itemID, tradeCount )).run();
	}

	/**
	 * Makes a trade with the trapper which exchanges all of your current
	 * furs for the given fur.
	 */

	public void makeTrapperRequest()
	{
		String command = previousLine.split( " " )[0];
		String parameters = previousLine.substring( command.length() ).trim();

		int furs = TrapperRequest.YETI_FUR.getCount( KoLCharacter.getInventory() );

		// If he doesn't specify a number, use number of yeti furs
		AdventureResult item = getFirstMatchingItem( parameters, NOWHERE, furs );
		if ( item == null )
			return;

		int itemID = item.getItemID();
		int tradeCount = item.getCount();

		// Ensure that the requested item is available from the trapper
		for ( int i = 0; i < trapperItemNumbers.length; ++i )
			if ( trapperItemNumbers[i] == itemID )
			{
				(new TrapperRequest( StaticEntity.getClient(), itemID, tradeCount ) ).run();
				return;
			}
	}

	/**
	 * Makes a request to the hunter which exchanges all of the given item
	 * with the hunter.  If the item is not available, this method does
	 * not report an error.
	 */

	public void makeHunterRequest()
	{
		if ( StaticEntity.getClient().hunterItems.isEmpty() )
			(new BountyHunterRequest( StaticEntity.getClient() )).run();

		if ( previousLine.indexOf( " " ) == -1 )
		{
			printList( StaticEntity.getClient().hunterItems );
			return;
		}

		String item = previousLine.substring( previousLine.indexOf( " " ) ).trim();
		for ( int i = 0; i < StaticEntity.getClient().hunterItems.size(); ++i )
			if ( ((String)StaticEntity.getClient().hunterItems.get(i)).indexOf( item ) != -1 )
				(new BountyHunterRequest( StaticEntity.getClient(), TradeableItemDatabase.getItemID( (String) StaticEntity.getClient().hunterItems.get(i) ) )).run();
	}

	/**
	 * Makes a request to the restaurant to purchase a meal.  If the item
	 * is not available, this method does not report an error.
	 */

	public void makeRestaurantRequest()
	{
		List items = StaticEntity.getClient().restaurantItems;
		if ( items.isEmpty() )
			(new RestaurantRequest( StaticEntity.getClient() )).run();

		if ( previousLine.indexOf( " " ) == -1 )
		{
			printList( items );
			return;
		}

		String item = previousLine.substring( previousLine.indexOf( " " ) ).trim();
		for ( int i = 0; i < items.size(); ++i )
		{
			String name = (String)items.get(i);
			if ( name.indexOf( item ) != -1 )
			{
				(new RestaurantRequest( StaticEntity.getClient(), name )).run();
				return;
			}
		}

		updateDisplay( ERROR_STATE, "The restaurant isn't selling " + item + " today." );
	}

	/**
	 * Makes a request to Doc Galaktik to purchase a cure.  If the
	 * cure is not available, this method does not report an error.
	 */

	public void makeGalaktikRequest()
	{
		if ( previousLine.indexOf( " " ) == -1 )
		{
			List cures = GalaktikRequest.retrieveCures( StaticEntity.getClient() );
			printList( cures );
			return;
		}

		// Cure "HP" or "MP"

		String cure = previousLine.substring( previousLine.indexOf( " " ) ).trim();

		int type = 0;
		if ( cure.equalsIgnoreCase( "hp" ) )
			type = GalaktikRequest.HP;
		else if ( cure.equalsIgnoreCase( "mp" ) )
			type = GalaktikRequest.MP;
		else
		{
			updateDisplay( ERROR_STATE, "Unknown Doc Galaktik request <" + cure + ">" );
			return;
		}

		(new GalaktikRequest( StaticEntity.getClient(), type )).run();
	}

	/**
	 * Makes a request to the microbrewery to purchase a drink.  If the
	 * item is not available, this method does not report an error.
	 */

	public void makeMicrobreweryRequest()
	{
		List items = StaticEntity.getClient().microbreweryItems;
		if ( items.isEmpty() )
			(new MicrobreweryRequest( StaticEntity.getClient() )).run();

		if ( previousLine.indexOf( " " ) == -1 )
		{
			printList( items );
			return;
		}

		String item = previousLine.substring( previousLine.indexOf( " " ) ).trim();
		for ( int i = 0; i < items.size(); ++i )
		{
			String name = (String)items.get(i);
			if ( name.indexOf( item ) != -1 )
			{
				(new MicrobreweryRequest( StaticEntity.getClient(), name )).run();
				return;
			}
		}

		updateDisplay( ERROR_STATE, "The microbrewery isn't selling " + item + " today." );
	}

	/**
	 * Utility method used to print a list to the given output
	 * stream.  If there's a need to print to the current output
	 * stream, simply pass the output stream to this method.
	 */

	protected void printList( List printing )
	{
		Object [] elements = new Object[ printing.size() ];
		printing.toArray( elements );

		for ( int i = 0; i < elements.length; ++i )
			printLine( elements[i].toString() );
	}

	public void printBlankLine()
	{	printLine( " " );
	}


	public void printLine( String line )
	{
		outputStream.println( line );
		mirrorStream.println( line );
		KoLmafia.getLogStream().println( line );

		StringBuffer colorBuffer = new StringBuffer();
		colorBuffer.append( "<font color=black>" );
		colorBuffer.append( line );
		colorBuffer.append( "</font><br>" );
		colorBuffer.append( LINE_BREAK );

		LocalRelayServer.addStatusMessage( colorBuffer.toString() );
		commandBuffer.append( colorBuffer.toString() );
	}

	/**
	 * Updates the currently active display in the <code>KoLmafia</code>
	 * session.
	 */

	public void updateDisplay( int state, String message )
	{
		if ( StaticEntity.getClient() == null )
			return;

		// If it's the enableDisplay() called from the KoLmafia
		// initializer, then outputStream and mirrorStream will
		// be null -- check this before attempting to print.

		if ( !message.equals( "" ) )
		{
			outputStream.println( message );
			mirrorStream.println( message );
		}

		if ( StaticEntity.getClient() instanceof KoLmafiaGUI )
			StaticEntity.getClient().updateDisplay( state, message );
		else
		{
			super.updateDisplay( state, message );
			if ( message.equals( "Login failed." ) )
				attemptLogin();
		}
	}

	/**
	 * A utility command which determines, based on the
	 * request, what KoLmafiaCLI command is used to redo
	 * the execution of it.  Note that only post-login
	 * scripts have derived commands.
	 */

	public static final String deriveCommand( Runnable request, int iterations )
	{
		StringBuffer commandString = new StringBuffer();

		if ( request instanceof KoLRequest )
			commandString.append( ((KoLRequest)request).getCommandForm( iterations ) );

		if ( request instanceof KoLAdventure )
			commandString.append( "adventure " + iterations + " " + ((KoLAdventure)request).getAdventureName() );

		if ( commandString.length() > 0 )
			commandString.append( LINE_BREAK );

		return commandString.toString();
	}
}

