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
	public static final int NOWHERE = 1;
	public static final int INVENTORY = 2;
	public static final int CREATION = 3;
	public static final int CLOSET = 4;

	protected String previousCommand;

	private String escapeString = "%";
	private PrintStream outputStream;
	private PrintStream mirrorStream;
	private BufferedReader commandStream;
	private KoLmafia scriptRequestor;
	private KoLmafiaCLI lastScript;
	private AdvancedScriptHandler advancedScriptHandler;


	/* Variables for Advanced Scripting */
	private int lineNumber = 0;

	public final static char[] tokenList = {' ', ',', '{', '}', '(', ')', '$', '!', '+', '-', '=', '"', '*', '/', '%', '[', ']', '!', ';'};
	public final static String[] multiCharTokenList = {"==", "!=", "<=", ">=", "||", "&&"};

	public static final int TYPE_VOID = 0;
	public static final int TYPE_BOOLEAN = 1;
	public static final int TYPE_INT = 2;
	public static final int TYPE_STRING = 3;

	public static final int TYPE_ITEM = 100;
	public static final int TYPE_ZODIAC = 101;
	public static final int TYPE_LOCATION = 102;

	public static final int COMMAND_BREAK = 1;
	public static final int COMMAND_CONTINUE = 2;

	public static void main( String [] args )
	{
		try
		{
			String initialScript = null;
			for ( int i = 0; i < args.length; ++i )
				if ( args[i].startsWith( "script=" ) )
					initialScript = args[i].substring( 7 );

			System.out.println();
			System.out.println( " **************************" );
			System.out.println( " *     " + VERSION_NAME + "      *" );
			System.out.println( " * Command Line Interface *" );
			System.out.println( " **************************" );
			System.out.println();

			System.out.println( "Determining server..." );
			KoLRequest.applySettings();
			System.out.println( KoLRequest.getRootHostName() + " selected." );
			System.out.println();

			KoLmafiaCLI session = new KoLmafiaCLI( null, System.in );
			StaticEntity.setClient( session );

			if ( initialScript == null )
			{
				session.attemptLogin();
				session.listenForCommands();
			}
			else
			{
				File script = new File( initialScript );

				if ( script.exists() )
				{
					session.lastScript = new KoLmafiaCLI( session, new FileInputStream( script ) );
					session.lastScript.listenForCommands();
					if ( session.lastScript.previousCommand == null )
						session.lastScript = null;
				}

				session.listenForCommands();
			}
		}
		catch ( IOException e )
		{
			// If an exception occurs, exit with an error code
			// to notify the user that something happened.

			e.printStackTrace( KoLmafia.getLogStream() );
			e.printStackTrace();

			System.exit(-1);
		}
	}

	/**
	 * Utility method to parse an individual adventuring result.
	 * This method determines what the result actually was and
	 * adds it to the tally.  Note that at the current time, it
	 * will ignore anything with the word "points".
	 *
	 * @param	result	String to parse for the result
	 */

	public void parseResult( String result )
	{
		if ( scriptRequestor == this )
		{
			super.parseResult( result );
			if ( !inLoginState() )
				updateDisplay( NORMAL_STATE, (result.startsWith( "You" ) ? " - " : " - Adventure result: ") + result );
		}
		else
			scriptRequestor.parseResult( result );
	}

	/**
	 * Constructs a new <code>KoLmafiaCLI</code> object.  All data fields
	 * are initialized to their default values, the global settings are
	 * loaded from disk.
	 */

	public KoLmafiaCLI( KoLmafia scriptRequestor, LimitedSizeChatBuffer commandBuffer ) throws IOException
	{
		this( scriptRequestor, System.in );
		KoLmafia.commandBuffer = commandBuffer;
	}

	/**
	 * Constructs a new <code>KoLmafiaCLI</code> object.  All data fields
	 * are initialized to their default values, the global settings
	 * are loaded from disk.
	 */

	public KoLmafiaCLI( KoLmafia scriptRequestor, InputStream inputStream ) throws IOException
	{
		this.scriptRequestor = (scriptRequestor == null) ? this : scriptRequestor;
		outputStream = this.scriptRequestor instanceof KoLmafiaCLI ? System.out : NullStream.INSTANCE;
		commandStream = new BufferedReader( new InputStreamReader( inputStream ) );
		mirrorStream = NullStream.INSTANCE;
		advancedScriptHandler = new AdvancedScriptHandler();
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
			scriptRequestor.resetContinueState();
			String username = scriptRequestor.getSettings().getProperty( "autoLogin" );

			if ( username == null || username.equals( "" ) )
			{
				if ( scriptRequestor == this )
				{
					outputStream.println();
					outputStream.print( "username: " );
				}

				username = commandStream.readLine();
			}

			if ( username == null )
				return;

			if ( username.length() == 0 )
			{
				outputStream.println( "Invalid login." );
				return;
			}

			String password = scriptRequestor.getSaveState( username );

			if ( !username.endsWith( "/q" ) )
				username += "/q";


			if ( password == null )
			{
				if ( scriptRequestor == this )
					outputStream.print( "password: " );

				password = commandStream.readLine();
			}

			if ( password == null )
				return;

			if ( password.length() == 0 )
			{
				outputStream.println( "Invalid password." );
				return;
			}

			if ( scriptRequestor == this )
				outputStream.println();

			scriptRequestor.deinitialize();
			(new LoginRequest( scriptRequestor, username, password, true, true )).run();
		}
		catch ( IOException e )
		{
			// Something bad must of happened.  Blow up!
			// Or rather, print the stack trace and exit
			// with an error state.

			e.printStackTrace( KoLmafia.getLogStream() );
			e.printStackTrace();

			System.exit(-1);
		}
	}

	/**
	 * Initializes the <code>KoLmafia</code> session.  Called after
	 * the login has been confirmed to notify the scriptRequestor that the
	 * login was successful, the user-specific settings should be
	 * loaded, and the user can begin adventuring.
	 */

	public void initialize( String loginname, String sessionID, boolean getBreakfast )
	{
		if ( scriptRequestor != this )
			scriptRequestor.initialize( loginname, sessionID, getBreakfast );
		else
			super.initialize( loginname, sessionID, getBreakfast );

		if ( scriptRequestor == this )
		{
			updateDisplay( NORMAL_STATE, "" );
			executeCommand( "moons", "" );
			updateDisplay( NORMAL_STATE, "" );
		}
	}

	/**
	 * A utility method which waits for commands from the user, then
	 * executing each command as it arrives.
	 */

	public void listenForCommands()
	{
		scriptRequestor.resetContinueState();

		if ( scriptRequestor == this )
			outputStream.print( " > " );

		String line = null;

		do
		{
			line = getNextLine();
			if ( line == null )
				return;

			if ( scriptRequestor == this )
				updateDisplay( NORMAL_STATE, "" );

			executeLine( line );

			if ( scriptRequestor == this )
				updateDisplay( NORMAL_STATE, "" );

			if ( scriptRequestor == this )
				outputStream.print( " > " );
		}
		while ( (scriptRequestor.permitsContinue() || scriptRequestor == this) );

		if ( line == null || line.trim().length() == 0 )
		{
			try
			{
				commandStream.close();
				previousCommand = null;
			}
			catch ( IOException e )
			{
				e.printStackTrace( KoLmafia.getLogStream() );
				e.printStackTrace();
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
				lineNumber++;
			}
			while ( line != null && (line.startsWith( escapeString ) || line.trim().length() == 0) );
			return line == null ? null : line.trim();
		}
		catch ( IOException e )
		{
			// If an IOException occurs during the parsing of the
			// command, you should exit from the command with an
			// error state after printing the stack trace.

			e.printStackTrace( KoLmafia.getLogStream() );
			e.printStackTrace();

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
		scriptRequestor.resetContinueState();
		if ( line.trim().length() == 0 )
			return;

		String command = line.trim().split( " " )[0].toLowerCase().trim();
		String parameters = line.substring( command.length() ).trim();

		if ( !command.equals( "repeat" ) )
			previousCommand = line;

		executeCommand( command, parameters );
	}

	/**
	 * A utility command which decides, based on the command
	 * to be executed, what to be done with it.  It can either
	 * delegate this to other functions, or do it itself.
	 */

	private void executeCommand( String command, String parameters )
	{
		// First, handle the wait command, for however
		// many seconds the user would like to wait.

		if ( command.equals( "wait" ) || command.equals( "pause" ) )
		{
			try
			{
				int seconds = df.parse( parameters ).intValue();
				for ( int i = 0; i < seconds && scriptRequestor.permitsContinue(); ++i )
				{
					KoLRequest.delay( 1000 );
					if ( scriptRequestor instanceof KoLmafiaGUI )
						updateDisplay( DISABLE_STATE, "Countdown: " + (seconds - i) + " seconds remaining..." );
					else
						outputStream.print( seconds - i + ", " );
				}
			}
			catch ( Exception e )
			{
				e.printStackTrace( KoLmafia.getLogStream() );
				e.printStackTrace();
			}

			updateDisplay( NORMAL_STATE, "Waiting completed." );
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
			downloadOverrideFiles();
			return;
		}

		// Adding the requested echo command.  I guess this is
		// useful for people who want to echo things...

		if ( command.equals( "echo" ) )
		{
			updateDisplay( NORMAL_STATE, parameters );
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
			if ( scriptRequestor.getSaveState( parameters ) != null )
			{
				if ( !scriptRequestor.inLoginState() )
				{
					updateDisplay( DISABLE_STATE, "Logging out..." );
					(new LogoutRequest( scriptRequestor )).run();
				}

				scriptRequestor.deinitialize();
				(new LoginRequest( scriptRequestor, parameters, scriptRequestor.getSaveState( parameters ), true, true )).run();
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
			if ( !scriptRequestor.inLoginState() )
			{
				updateDisplay( DISABLE_STATE, "Logging out..." );
				(new LogoutRequest( scriptRequestor )).run();
			}

			updateDisplay( DISABLE_STATE, "Exiting KoLmafia..." );
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
			if ( lastScript == null || lastScript.previousCommand == null )
			{
				updateDisplay( ERROR_STATE, "No commands left to continue script." );
				scriptRequestor.cancelRequest();
				return;
			}

			scriptRequestor.resetContinueState();
			lastScript.listenForCommands();

			if ( lastScript.previousCommand == null )
				lastScript = null;

			return;
		}

		// Next, handle repeat commands - these are a
		// carry-over feature which made sense in CLI.

		if ( command.equals( "repeat" ) )
		{
			try
			{
				if ( previousCommand != null )
				{
					int repeatCount = parameters.length() == 0 ? 1 : df.parse( parameters ).intValue();
					for ( int i = 0; i < repeatCount && scriptRequestor.permitsContinue(); ++i )
						executeLine( previousCommand );
				}

				return;
			}
			catch ( Exception e )
			{
				// Notify the scriptRequestor that the command could not be repeated
				// the given number of times.

				updateDisplay( ERROR_STATE, parameters + " is not a number." );
				scriptRequestor.cancelRequest();

				e.printStackTrace( KoLmafia.getLogStream() );
				e.printStackTrace();

				return;
			}
		}

		// Next, print out the moon phase, if the user
		// wishes to know what moon phase it is.

		if ( command.startsWith( "moon" ) )
		{
			updateDisplay( NORMAL_STATE, "Ronald: " + MoonPhaseDatabase.getRonaldPhaseAsString() );
			updateDisplay( NORMAL_STATE, "Grimace: " + MoonPhaseDatabase.getRonaldPhaseAsString() );
			updateDisplay( NORMAL_STATE, "" );

			updateDisplay( NORMAL_STATE, MoonPhaseDatabase.getMoonEffect() );
			updateDisplay( NORMAL_STATE, "" );

			Date today = new Date();

			try
			{
				today = sdf.parse( sdf.format( today ) );
			}
			catch ( Exception e )
			{
				// Should not happen - you're having the parser
				// parse something that it formatted.

				e.printStackTrace( KoLmafia.getLogStream() );
				e.printStackTrace();
			}

			String [] holidayPredictions = MoonPhaseDatabase.getHolidayPredictions( today );
			for ( int i = 0; i < holidayPredictions.length; ++i )
				updateDisplay( NORMAL_STATE, holidayPredictions[i] );

			updateDisplay( NORMAL_STATE, "" );
			updateDisplay( NORMAL_STATE, MoonPhaseDatabase.getHoliday( today ) );
			updateDisplay( NORMAL_STATE, "" );

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
				scriptRequestor.cancelRequest();
			}

			return;
		}

		// Next, handle requests to start or stop
		// debug mode.

		if ( command.startsWith( "mirror" ) )
		{
			if ( parameters.length() == 0 )
			{
				this.mirrorStream.close();
				this.mirrorStream = NullStream.INSTANCE;
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
					// Because you created a file, no I/O errors should
					// occur.  However, since there could still be something
					// bad happening, print an error message.

					updateDisplay( ERROR_STATE, "I/O error in opening file \"" + parameters + "\"" );
					scriptRequestor.cancelRequest();

					e.printStackTrace( KoLmafia.getLogStream() );
					e.printStackTrace();

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

			KoLRequest request = new KoLRequest( scriptRequestor,
				"desc_item.php?whichitem=" + TradeableItemDatabase.getDescriptionID( result.getItemID() ) );

			request.run();

			if ( scriptRequestor instanceof KoLmafiaGUI )
			{
				Object [] params = new Object[2];
				params[0] = scriptRequestor;
				params[1] = request;

				(new CreateFrameRunnable( RequestFrame.class, params )).run();
			}
			else
			{
				showHTML( request.responseText, "Item Description" );
			}

			return;
		}

		// If there's any commands which suggest that the
		// scriptRequestor is in a login state, you should not do
		// any commands listed beyond this point

		if ( scriptRequestor.inLoginState() )
		{
			updateDisplay( ERROR_STATE, "You have not yet logged in." );
			scriptRequestor.cancelRequest();
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
			StrangeLeaflet.robStrangeLeaflet( parameters.equals( "magic" ) );
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

		// Next is the command to handle the retrieval
		// of breakfast, which is not yet documented.

		if ( command.equals( "breakfast" ) )
		{
			scriptRequestor.getBreakfast();
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
			KoLRequest request = new KoLRequest( scriptRequestor, "council.php", true );
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

		// One command available after login is a request
		// to print the current state of the scriptRequestor.  This
		// should be handled in a separate method, since
		// there are many things the scriptRequestor may want to print

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
			if ( parameters.startsWith( "list" ) )
			{
				executePrintCommand( "equipment " + parameters.substring(4).trim() );
				return;
			}
			else if ( parameters.length() == 0 )
			{
				executePrintCommand( "equipment" );
				return;
			}

			AdventureResult match = getFirstMatchingItem( parameters, INVENTORY );
			if ( match != null )
				scriptRequestor.makeRequest( new EquipmentRequest( scriptRequestor, match.getName() ), 1 );
			return;
		}


		// You can remove a specific piece of equipment.

		if ( command.startsWith( "unequip" ) )
		{
			// The following loop removes the first item with
			// the specified name.

			parameters = parameters.toLowerCase();

			for ( int i = 0; i <= KoLCharacter.FAMILIAR; ++i )
			{
				if ( KoLCharacter.getCurrentEquipmentName( i ) != null && KoLCharacter.getCurrentEquipmentName( i ).indexOf( parameters ) != -1 )
				{
					scriptRequestor.makeRequest( new EquipmentRequest( scriptRequestor, EquipmentRequest.UNEQUIP, i ), 1 );
					return;
				}
			}

			scriptRequestor.updateDisplay( ERROR_STATE, "No equipment found matching string \"" + parameters + "\"" );
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
				if ( familiars.get(i).toString().toLowerCase().indexOf( lowerCaseName ) != -1 )
					scriptRequestor.makeRequest( new FamiliarRequest( scriptRequestor, (FamiliarData) familiars.get(i) ), 1 );

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
			(new SearchMallRequest( scriptRequestor, parameters, results )).run();
			printList( results );
			return;
		}

		// Finally, handle command abbreviations - in
		// other words, commands that look like they're
		// their own commands, but are actually commands
		// which are derived from other commands.

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

		if ( command.equals( "toast" ) || command.equals( "rest" ) || command.equals( "relax" ) || command.equals( "arches" ) )
		{
			executeCampgroundRequest( command + " " + parameters );
			return;
		}

		if ( (command.equals( "status" ) || command.equals( "effects" )) && parameters.startsWith( "refresh" ) )
		{
			(new CharsheetRequest( scriptRequestor )).run();
			updateDisplay( NORMAL_STATE, "Status refreshed." );
			parameters = parameters.length() == 7 ? "" : parameters.substring( 7 ).trim();
		}

		if ( command.equals( "inv" ) && parameters.equals( "refresh" ) )
		{
			(new EquipmentRequest( scriptRequestor, EquipmentRequest.CLOSET )).run();
			updateDisplay( NORMAL_STATE, "Status refreshed." );
			parameters = parameters.length() == 7 ? "" : parameters.substring( 7 ).trim();
		}

		if ( command.startsWith( "inv" ) || command.equals( "session" ) || command.equals( "summary" ) ||
			command.equals( "effects" ) || command.startsWith( "status" ) || command.equals( "encounters" ) )
		{
			executePrintCommand( command + " " + parameters );
			return;
		}

		if ( command.equals( "<?advanced"))
		{
			try
			{
				advancedScriptHandler.execute( parameters);
			}
			catch( AdvancedScriptException e)
			{
				updateDisplay( ERROR_STATE, e.getMessage() );
				scriptRequestor.cancelRequest();
				e.printStackTrace( KoLmafia.getLogStream());
			}
			return;
		}

		// If all else fails, then assume that the
		// person was trying to call a script.

		if ( parameters.length() != 0 )
			executeScriptCommand( command + " " + parameters );
		else
			executeScriptCommand( command );
	}

	public void showHTML( String text, String title )
	{
		updateDisplay( NORMAL_STATE,
			       text.
			       replaceAll( "<(br|p|blockquote)>", LINE_BREAK ).
			       replaceAll( "<.*?>", "" ).
			       replaceAll( "&nbsp;", " " ).
			       replaceAll( "&trade;", " [tm]" ).
			       replaceAll( "&ntilde;", "n" ).
			       replaceAll( "&quot;", "\"" ) );
	}

	/**
	 * A special module used to handle the calling of a
	 * script.
	 */

	public void executeScriptCommand( String parameters )
	{
		try
		{
			// Locate the script file.  In order of preference,
			// the script files will either have no extension
			// and be in the scripts directory, have a ".txt"
			// extension and be in the scripts directory, have
			// no extension and not be in the scripts directory,
			// and have a ".txt" extension and not be in the
			// scripts directory.

			int runCount = 1;

			File scriptFile = new File( "scripts" + File.separator + parameters );
			if ( !scriptFile.exists() )
				scriptFile = new File( "scripts" + File.separator + parameters + ".txt" );
			if ( !scriptFile.exists() )
				scriptFile = new File( parameters );
			if ( !scriptFile.exists() )
				scriptFile = new File( parameters + ".txt" );

			if ( !scriptFile.exists() )
			{
				runCount = Integer.parseInt( parameters.split( " " )[0] );
				String scriptName = parameters.substring( parameters.indexOf( " " ) ).trim();

				scriptFile = new File( "scripts" + File.separator + scriptName );
				if ( !scriptFile.exists() )
					scriptFile = new File( "scripts" + File.separator + scriptName + ".txt" );
				if ( !scriptFile.exists() )
					scriptFile = new File( scriptName );
				if ( !scriptFile.exists() )
					scriptFile = new File( scriptName + ".txt" );
			}

			if ( !scriptFile.exists() )
			{
				updateDisplay( ERROR_STATE, "Script file \"" + parameters + "\" could not be found." );
				scriptRequestor.cancelRequest();
				return;
			}

			for ( int i = 0; i < runCount && scriptRequestor.permitsContinue(); ++i )
			{
				lastScript = new KoLmafiaCLI( scriptRequestor, new FileInputStream( scriptFile ) );
				lastScript.commandBuffer = commandBuffer;
				lastScript.listenForCommands();
				if ( lastScript.previousCommand == null )
					lastScript = null;
			}
		}
		catch ( Exception e )
		{
			// Because everything is checked for consistency
			// before being loaded, this should not happen.

			updateDisplay( ERROR_STATE, "Script file \"" + parameters + "\" could not be found." );
			scriptRequestor.cancelRequest();

			e.printStackTrace( KoLmafia.getLogStream() );
			e.printStackTrace();

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
		if ( !scriptRequestor.permitsContinue() )
			return false;

		String operator = parameters.indexOf( "==" ) != -1 ? "==" : parameters.indexOf( "!=" ) != -1 ? "!=" :
			parameters.indexOf( ">=" ) != -1 ? ">=" : parameters.indexOf( "<=" ) != -1 ? "<=" :
			parameters.indexOf( ">" ) != -1 ? ">" : parameters.indexOf( "<" ) != -1 ? "<" : parameters.indexOf( "=" ) != -1 ? "==" : null;

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
			e.printStackTrace( KoLmafia.getLogStream() );
			e.printStackTrace();

			return false;
		}

		return	operator.equals( "==" ) ? leftValue == rightValue :
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

		// Items first for one reason: Knob Goblin perfume
		AdventureResult item = itemParameter( left );

		if ( item != null )
			return item.getCount( KoLCharacter.getInventory() );

		AdventureResult effect = effectParameter( left );

		if ( effect != null )
			return effect.getCount( KoLCharacter.getEffects() );

		return 0;
	}

	private int rvalue( String left, String right ) throws NumberFormatException
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

		try
		{
			return Integer.parseInt( right );
		}
		catch ( NumberFormatException e )
		{
			// Items first for one reason: Knob Goblin perfume

			AdventureResult item = itemParameter( right );

			if ( item != null )
				return item.getCount( KoLCharacter.getInventory() );

			AdventureResult effect = effectParameter( right );

			if ( effect != null )
				return effect.getCount( KoLCharacter.getEffects() );

			throw e;
		}
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
			scriptRequestor.conditions.clear();
			return true;
		}
		else if ( option.equals( "check" ) )
		{
			scriptRequestor.checkRequirements( scriptRequestor.conditions );
			scriptRequestor.conditions.clear();
			scriptRequestor.conditions.addAll( scriptRequestor.missingItems );
			return true;
		}
		else if ( option.equals( "mode" ) )
		{
			String conditionString = parameters.substring( option.length() ).trim();

			if ( conditionString.startsWith( "conjunction" ) )
				scriptRequestor.useDisjunction = false;
			else if ( conditionString.startsWith( "disjunction" ) )
				scriptRequestor.useDisjunction = true;

			if ( scriptRequestor.useDisjunction )
				updateDisplay( NORMAL_STATE, "All conditions will be ORed together." );
			else
				updateDisplay( NORMAL_STATE, "All conditions will be ANDed together." );
		}
		else if ( option.equals( "add" ) )
		{
			String conditionString = parameters.substring( option.length() ).trim();

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

				int previousIndex = scriptRequestor.conditions.indexOf( condition );
				if ( previousIndex != -1 )
				{
					AdventureResult previousCondition = (AdventureResult) scriptRequestor.conditions.get( previousIndex );

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
					// In the event that some exception occurred in
					// parsing, return a null result.

					e.printStackTrace( KoLmafia.getLogStream() );
					e.printStackTrace();

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

					int previousIndex = scriptRequestor.conditions.indexOf( condition );
					if ( previousIndex != -1 )
					{
						AdventureResult previousCondition = (AdventureResult) scriptRequestor.conditions.get( previousIndex );

						for ( int i = 0; i < subpoints.length; ++i )
							if ( subpoints[i] != 0 && previousCondition.getCount(i) != 0 )
								subpoints[i] = Math.max( 0, subpoints[i] - previousCondition.getCount(i) );

						condition = new AdventureResult( AdventureResult.SUBSTATS, subpoints );
					}
				}
				catch ( Exception e )
				{
					// In the event that some exception occurred in
					// parsing, return a null result.

					e.printStackTrace( KoLmafia.getLogStream() );
					e.printStackTrace();

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

					int previousIndex = scriptRequestor.conditions.indexOf( condition );
					if ( previousIndex != -1 )
					{
						AdventureResult previousCondition = (AdventureResult) scriptRequestor.conditions.get( previousIndex );
						condition = condition.getInstance( condition.getCount() - previousCondition.getCount() );
					}
				}
				catch ( Exception e )
				{
					// In the event that some exception occurred in
					// parsing, return a null result.

					e.printStackTrace( KoLmafia.getLogStream() );
					e.printStackTrace();

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
			printList( scriptRequestor.conditions );
			return false;
		}

		if ( condition.getCount() > 0 )
		{
			AdventureResult.addResultToList( scriptRequestor.conditions, condition );
			updateDisplay( NORMAL_STATE, "Condition added." );
			printList( scriptRequestor.conditions );
		}
		else
		{
			updateDisplay( NORMAL_STATE, "Condition already met." );
			printList( scriptRequestor.conditions );
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
			scriptRequestor.makeRequest( new CampgroundRequest( scriptRequestor, parameterList[0] ),
				parameterList.length == 1 ? 1 : df.parse( parameterList[1] ).intValue() );
			scriptRequestor.resetContinueState();
		}
		catch ( Exception e )
		{
			updateDisplay( ERROR_STATE, parameterList[1] + " is not a number." );
			scriptRequestor.cancelRequest();

			e.printStackTrace( KoLmafia.getLogStream() );
			e.printStackTrace();

			return;
		}
	}

	/**
	 * A special module used to handle casting skills on yourself;
	 * note, these skills have to be listed in the table.
	 */

	private void executeCastBuffRequest( String parameters )
	{
		String skillName;
		int buffCount;

		if ( parameters.startsWith( "\"" ) )
		{
			skillName = parameters.substring( 1, parameters.length() - 1 );
			buffCount = 1;
		}
		else
		{
			skillName = getSkillName( parameters.toLowerCase() );

			if ( skillName != null )
			{
				scriptRequestor.makeRequest( new UseSkillRequest( scriptRequestor, skillName, null, 1 ), 1 );
				return;
			}

			String firstParameter = parameters.split( " " )[0].toLowerCase();
			String skillNameString = parameters.substring( firstParameter.length() ).trim().toLowerCase();

			if ( skillNameString.startsWith( "\"" ) )
			{
				skillName = skillNameString.substring( 1, skillNameString.length() - 1 );
			}
			else
			{
				skillName = getSkillName( skillNameString );
				if ( skillName == null )
				{
					scriptRequestor.cancelRequest();
					updateDisplay( ERROR_STATE, "Skill not available" );
					return;
				}
			}

			try
			{
				buffCount = firstParameter.equals( "*" ) ?
					(int) ( KoLCharacter.getCurrentMP() /
						ClassSkillsDatabase.getMPConsumptionByID( ClassSkillsDatabase.getSkillID( skillName ) ) ) :
							df.parse( firstParameter ).intValue();
			}
			catch ( Exception e )
			{
				// Technically, this exception should not be thrown, but if
				// it is, then print an error message and return.

				updateDisplay( ERROR_STATE, firstParameter + " is not a number." );
				scriptRequestor.cancelRequest();

				e.printStackTrace( KoLmafia.getLogStream() );
				e.printStackTrace();

				return;
			}

			if ( buffCount > 0 )
				scriptRequestor.makeRequest( new UseSkillRequest( scriptRequestor, skillName, null, buffCount ), 1 );
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
			scriptRequestor.cancelRequest();
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

			scriptRequestor.cancelRequest();

			e.printStackTrace( KoLmafia.getLogStream() );
			e.printStackTrace();

			return;
		}

		int amountRemaining = amount;
		int eachAmount = amountRemaining / increments;

		updateDisplay( DISABLE_STATE, "Donating " + amount + " to the shrine..." );
		scriptRequestor.makeRequest( new HeroDonationRequest( scriptRequestor, heroID, eachAmount ), increments - 1 );
		amountRemaining -= eachAmount * (increments - 1);

		if ( scriptRequestor.permitsContinue() )
		{
			updateDisplay( DISABLE_STATE, "Request " + increments + " in progress..." );
			scriptRequestor.makeRequest( new HeroDonationRequest( scriptRequestor, heroID, amountRemaining ), 1 );

			if ( scriptRequestor.permitsContinue() )
				updateDisplay( NORMAL_STATE, "Requests complete!" );
		}
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
			scriptRequestor.cancelRequest();
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
				// Because you created a file, no I/O errors should
				// occur.  However, since there could still be something
				// bad happening, print an error message.

				updateDisplay( ERROR_STATE, "I/O error in opening file \"" + parameterList[1] + "\"" );
				scriptRequestor.cancelRequest();

				e.printStackTrace( KoLmafia.getLogStream() );
				e.printStackTrace();

				return;
			}
		}
		else
			desiredOutputStream = outputStream;

		executePrintCommand( parameterList[0].toLowerCase(), filter, desiredOutputStream );

		if ( parameterList.length > 1 && !parameterList[1].equals( "filter" ) )
			updateDisplay( NORMAL_STATE, "Data has been printed to \"" + parameterList[1] + "\"" );
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

		updateDisplay( NORMAL_STATE, (new Date()).toString() );
		updateDisplay( NORMAL_STATE, MoonPhaseDatabase.getMoonEffect() );
		updateDisplay( NORMAL_STATE, "" );

		if ( desiredData.equals( "session" ) )
		{
			updateDisplay( NORMAL_STATE, "Player: " + KoLCharacter.getUsername() );
			updateDisplay( NORMAL_STATE, "Session ID: " + scriptRequestor.getSessionID() );
			updateDisplay( NORMAL_STATE, "Password Hash: " + scriptRequestor.getPasswordHash() );
		}
		else if ( desiredData.startsWith( "stat" ) )
		{
			updateDisplay( NORMAL_STATE, "Lv: " + KoLCharacter.getLevel() );
			updateDisplay( NORMAL_STATE, "HP: " + KoLCharacter.getCurrentHP() + " / " + df.format( KoLCharacter.getMaximumHP() ) );
			updateDisplay( NORMAL_STATE, "MP: " + KoLCharacter.getCurrentMP() + " / " + df.format( KoLCharacter.getMaximumMP() ) );
			updateDisplay( NORMAL_STATE, "" );
			updateDisplay( NORMAL_STATE, "Mus: " + getStatString( KoLCharacter.getBaseMuscle(), KoLCharacter.getAdjustedMuscle(), KoLCharacter.getMuscleTNP() ) );
			updateDisplay( NORMAL_STATE, "Mys: " + getStatString( KoLCharacter.getBaseMysticality(), KoLCharacter.getAdjustedMysticality(), KoLCharacter.getMysticalityTNP() ) );
			updateDisplay( NORMAL_STATE, "Mox: " + getStatString( KoLCharacter.getBaseMoxie(), KoLCharacter.getAdjustedMoxie(), KoLCharacter.getMoxieTNP() ) );
			updateDisplay( NORMAL_STATE, "" );
			updateDisplay( NORMAL_STATE, "Meat: " + df.format( KoLCharacter.getAvailableMeat() ) );
			updateDisplay( NORMAL_STATE, "Drunk: " + KoLCharacter.getInebriety() );
			updateDisplay( NORMAL_STATE, "Adv: " + KoLCharacter.getAdventuresLeft() );

			updateDisplay( NORMAL_STATE, "Fam: " + KoLCharacter.getFamiliar() );
			updateDisplay( NORMAL_STATE, "Item: " + KoLCharacter.getFamiliarItem() );
		}
		else if ( desiredData.startsWith( "equip" ) )
		{
			updateDisplay( NORMAL_STATE, "     Hat: " + KoLCharacter.getEquipment( KoLCharacter.HAT ) );
			updateDisplay( NORMAL_STATE, "  Weapon: " + KoLCharacter.getEquipment( KoLCharacter.WEAPON ) );
			updateDisplay( NORMAL_STATE, "Off-hand: " + KoLCharacter.getEquipment( KoLCharacter.OFFHAND ) );
			updateDisplay( NORMAL_STATE, "   Shirt: " + KoLCharacter.getEquipment( KoLCharacter.SHIRT ) );
			updateDisplay( NORMAL_STATE, "   Pants: " + KoLCharacter.getEquipment( KoLCharacter.PANTS ) );
			updateDisplay( NORMAL_STATE, "  Acc. 1: " + KoLCharacter.getEquipment( KoLCharacter.ACCESSORY1 ) );
			updateDisplay( NORMAL_STATE, "  Acc. 2: " + KoLCharacter.getEquipment( KoLCharacter.ACCESSORY2 ) );
			updateDisplay( NORMAL_STATE, "  Acc. 3: " + KoLCharacter.getEquipment( KoLCharacter.ACCESSORY3 ) );
		}
		else if ( desiredData.startsWith( "encounters" ) )
		{
			updateDisplay( NORMAL_STATE, "Visited Locations: " );
			updateDisplay( NORMAL_STATE, "" );
			printList( scriptRequestor.adventureList );

			updateDisplay( NORMAL_STATE, "" );
			updateDisplay( NORMAL_STATE, "" );

			updateDisplay( NORMAL_STATE, "Encounter Listing: " );
			updateDisplay( NORMAL_STATE, "" );
			printList( scriptRequestor.encounterList );
		}
		else
		{
			List mainList = desiredData.equals( "closet" ) ? KoLCharacter.getCloset() : desiredData.equals( "summary" ) ? scriptRequestor.tally :
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
				scriptRequestor.cancelRequest();
				return null;
			}

			try
			{
				effectName = (String) matchingNames.get(0);
				duration = durationString.equals( "*" ) ? 0 : df.parse( durationString ).intValue();
			}
			catch ( Exception e )
			{
				// Technically, this exception should not be thrown, but if
				// it is, then print an error message and return.

				updateDisplay( ERROR_STATE, durationString + " is not a number." );
				scriptRequestor.cancelRequest();

				e.printStackTrace( KoLmafia.getLogStream() );
				e.printStackTrace();

				return null;
			}
		}

		if ( effectName == null )
		{
			updateDisplay( ERROR_STATE, "[" + parameters + "] does not match anything in the status effect database." );
			scriptRequestor.cancelRequest();
			return null;
		}

		return new AdventureResult( effectName, duration, true );
	}

	/**
	 * Utility method which determines the first item which matches
	 * the given parameter string.  Note that the string may also
	 * specify an item quantity before the string.
	 */

	public AdventureResult getFirstMatchingItem( String parameters, int matchType, int def )
	{
		int itemID = -1;
		int itemCount = 0;

		// First, allow for the person to type without specifying
		// the amount, if the amount is 1.

		List matchingNames = TradeableItemDatabase.getMatchingNames( parameters );

		if ( matchingNames.size() != 0 )
		{
			itemID = TradeableItemDatabase.getItemID( (String) matchingNames.get(0) );
			itemCount = def;
		}
		else
		{
			String itemCountString = parameters.split( " " )[0];
			String itemNameString = parameters.substring( itemCountString.length() ).trim();

			matchingNames = TradeableItemDatabase.getMatchingNames( itemNameString );

			if ( matchingNames.size() == 0 )
			{
				updateDisplay( ERROR_STATE, "[" + itemNameString + "] does not match anything in the item database." );
				scriptRequestor.cancelRequest();
				return null;
			}

			try
			{
				itemID = TradeableItemDatabase.getItemID( (String) matchingNames.get(0) );
				itemCount = itemCountString.equals( "*" ) ? 0 : df.parse( itemCountString ).intValue();
			}
			catch ( Exception e )
			{
				// Technically, this exception should not be thrown, but if
				// it is, then print an error message and return.

				updateDisplay( ERROR_STATE, itemCountString + " is not a number." );
				scriptRequestor.cancelRequest();

				e.printStackTrace( KoLmafia.getLogStream() );
				e.printStackTrace();

				return null;
			}
		}

		if ( itemID == -1 )
		{
			updateDisplay( ERROR_STATE, "[" + parameters + "] does not match anything in the item database." );
			scriptRequestor.cancelRequest();
			return null;
		}

		AdventureResult firstMatch = new AdventureResult( itemID, itemCount );

		// The result also depends on the number of items which
		// are available in the given match area.

		int matchCount;

		if ( matchType == CREATION )
		{
			ConcoctionsDatabase.refreshConcoctions();
			matchCount = ItemCreationRequest.getInstance( scriptRequestor, firstMatch ).getCount( ConcoctionsDatabase.getConcoctions() );
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
			itemCount = matchCount + itemCount;

		// If the number matching is less than the quantity desired,
		// then be sure to throw an exception.

		if ( matchType != NOWHERE && matchType != CREATION && itemCount > matchCount )
		{
			updateDisplay( ERROR_STATE, "Insufficient " + TradeableItemDatabase.getItemName( itemID ) + " to continue." );
			scriptRequestor.cancelRequest();
			return null;
		}

		return itemCount == 0 ? null : firstMatch.getInstance( itemCount );
	}

	private AdventureResult getFirstMatchingItem( String parameters, int matchType )
	{	return getFirstMatchingItem( parameters, matchType, 1 );
	}

	/**
	 * A special module used specifically for properly instantiating
	 * ClanStorageRequests which send things to the clan stash.
	 */

	private void executeStashRequest( String parameters )
	{
		synchronized ( ClanStashRequest.class )
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

			AdventureResult firstMatch = getFirstMatchingItem( parameters, isWithdraw ? NOWHERE : INVENTORY );
			if ( firstMatch == null )
				return;

			if ( isWithdraw )
			{
				// To prevent the potential for stash looting, only
				// people who have administrative privileges can take
				// stuff from the stash using a script with no penalty.

				if ( ClanManager.getRankList().isEmpty() )
				{
					// This makes it so there's no advantage to scripting it
					// over accessing it directly in a browser or through the
					// standard KoLmafia interface.

					if ( !TradeableItemDatabase.isUsable( firstMatch.getName() ) )
					{
						updateDisplay( DISABLE_STATE, "KoLmafia is now wasting some time before accessing the stash..." );
						KoLRequest.delay( 300000 );
					}
				}
			}

			Object [] items = new Object[1];
			items[0] = firstMatch;

			int initialCount = firstMatch.getCount( KoLCharacter.getInventory() );
			scriptRequestor.makeRequest( new ClanStashRequest( scriptRequestor, items, isWithdraw ?
				ClanStashRequest.STASH_TO_ITEMS : ClanStashRequest.ITEMS_TO_STASH ), 1 );

			// If the item amount doesn't match the desired, then cancel
			// script execution.

			if ( isWithdraw && firstMatch.getCount( KoLCharacter.getInventory() ) - initialCount != firstMatch.getCount() )
			{
				updateDisplay( ERROR_STATE, "Unable to acquire " + firstMatch.getCount() + " " + firstMatch.getName() );
				scriptRequestor.cancelRequest();
			}
		}
	}

	/**
	 * Untinkers an item (not specified).  This is generally not
	 * used by the CLI interface, but is defined to override the
	 * abstract method provided in the KoLmafia class.
	 */

	public void makeUntinkerRequest()
	{
		if ( previousCommand.indexOf( " " ) == -1 )
		{
			scriptRequestor.makeRequest( new UntinkerRequest( scriptRequestor ), 1 );
			return;
		}

		String item = previousCommand.substring( previousCommand.split( " " )[0].length() ).trim();
		AdventureResult firstMatch = getFirstMatchingItem( item, INVENTORY );
		if ( firstMatch == null )
			return;

		scriptRequestor.makeRequest( new UntinkerRequest( scriptRequestor, firstMatch.getItemID() ), 1 );
	}

	/**
	 * Set the Canadian Mind Control device to selected setting.
	 */

	public void makeMindControlRequest()
	{
		try
		{
			String [] command = previousCommand.split( " " );

			int setting = df.parse( command[1] ).intValue();
			scriptRequestor.makeRequest( new MindControlRequest( scriptRequestor, setting ), 1 );
		}
		catch ( Exception e )
		{
			updateDisplay( ERROR_STATE, "Invalid setting for device." );

			e.printStackTrace( KoLmafia.getLogStream() );
			e.printStackTrace();
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

		FamiliarTrainingFrame.levelFamiliar( scriptRequestor, goal, type, buffs, false );
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
		{
			for ( int i = 1; i <= 16; ++i )
				MushroomPlot.pickMushroom( i, false );
		}

		String plot = MushroomPlot.getMushroomPlot( false );

		if ( scriptRequestor.permitsContinue() )
		{
			updateDisplay( NORMAL_STATE, "Current:" );
			updateDisplay( NORMAL_STATE, plot );
			updateDisplay( NORMAL_STATE, "" );
			updateDisplay( NORMAL_STATE, "Forecast:" );
			updateDisplay( NORMAL_STATE, MushroomPlot.getForecastedPlot( false ) );
		}
	}

	/**
	 * A special module used specifically for properly instantiating
	 * ItemStorageRequests which pulls things from Hagnk's.
	 */

	private void executeHagnkRequest( String parameters )
	{
		AdventureResult firstMatch = getFirstMatchingItem( parameters, NOWHERE );
		if ( firstMatch == null )
			return;

		Object [] items = new Object[1];
		items[0] = firstMatch;

		scriptRequestor.makeRequest( new ItemStorageRequest( scriptRequestor, ItemStorageRequest.STORAGE_TO_INVENTORY, items ), 1 );
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

		String item = parameters.substring(4).trim();
		AdventureResult firstMatch = getFirstMatchingItem( item, parameters.startsWith( "take" ) ? CLOSET : INVENTORY );
		if ( firstMatch == null )
			return;

		Object [] items = new Object[1];
		items[0] = firstMatch;

		scriptRequestor.makeRequest( new ItemStorageRequest( scriptRequestor,
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
			scriptRequestor.makeRequest( new AutoSellRequest( scriptRequestor, firstMatch,
				df.parse( tokens[ tokens.length - 2 ] ).intValue(), df.parse( tokens[ tokens.length - 1 ] ).intValue() ), 1 );
		}
		catch ( Exception e )
		{
			updateDisplay( ERROR_STATE, "Invalid price/limit for automall request." );
			scriptRequestor.cancelRequest();

			e.printStackTrace( KoLmafia.getLogStream() );
			e.printStackTrace();

			return;
		}
	}

	/**
	 * A special module used specifically for properly instantiating
	 * AutoSellRequests for just autoselling items.
	 */

	private void executeAutoSellRequest( String parameters )
	{
		AdventureResult firstMatch = getFirstMatchingItem( parameters, INVENTORY );
		if ( firstMatch == null )
			return;

		scriptRequestor.makeRequest( new AutoSellRequest( scriptRequestor, firstMatch ), 1 );
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
		ArrayList results = new ArrayList();
		(new SearchMallRequest( scriptRequestor, '\"' + firstMatch.getName() + '\"', 0, results )).run();

		scriptRequestor.makePurchases( results, results.toArray(), firstMatch.getCount() );
	}

	/**
	 * A special module used specifically for properly instantiating
	 * ItemCreationRequests.
	 */

	private void executeItemCreationRequest( String parameters )
	{
		int itemID;  int mixingMethod;  int quantityNeeded;

		AdventureResult firstMatch = getFirstMatchingItem( parameters, CREATION );
		if ( firstMatch == null )
			return;

		ItemCreationRequest irequest = ItemCreationRequest.getInstance( scriptRequestor, firstMatch );
		scriptRequestor.makeRequest( irequest, 1 );

		if ( scriptRequestor.permitsContinue() )
			updateDisplay( NORMAL_STATE, "Successfully created " + irequest.getQuantityNeeded() + " " + irequest.getName() );
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
			scriptRequestor.makeRequest( new ConsumeItemRequest( scriptRequestor, new AdventureResult( itemName, itemCount, false ) ), 1 );
		else
			scriptRequestor.makeRequest( new ConsumeItemRequest( scriptRequestor, new AdventureResult( itemName, 1, false ) ), itemCount );
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
				scriptRequestor.cancelRequest();
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
				scriptRequestor.cancelRequest();

				e.printStackTrace( KoLmafia.getLogStream() );
				e.printStackTrace();

				return;
			}
		}

		updateDisplay( DISABLE_STATE, "Beginning " + adventureCount + " turnips to " + adventure.toString() + "..." );
		scriptRequestor.makeRequest( adventure, adventureCount );
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
			scriptRequestor.cancelRequest();
			return;
		}

		(new EquipmentRequest( scriptRequestor, intendedOutfit )).run();
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
			scriptRequestor.cancelRequest();
			return;
		}

		try
		{
			int buffBotIterations = df.parse( parameters ).intValue();
			scriptRequestor.resetContinueState();

			BuffBotHome.setBuffBotActive( true );
			BuffBotManager.runBuffBot( buffBotIterations );

			updateDisplay( NORMAL_STATE, "BuffBot execution complete." );
			scriptRequestor.cancelRequest();

		}
		catch (Exception e)
		{
			// Technically, this exception should not be thrown, but if
			// it is, then print an error message and return.

			updateDisplay( ERROR_STATE, parameters + " is not a number." );
			scriptRequestor.cancelRequest();

			e.printStackTrace( KoLmafia.getLogStream() );
			e.printStackTrace();

			return;
		}
	}

	/**
	 * Updates the currently active display in the <code>KoLmafia</code>
	 * session.
	 */

	public synchronized void updateDisplay( int state, String message )
	{
		outputStream.println( message );
		mirrorStream.println( message );

		if ( scriptRequestor instanceof KoLmafiaGUI )
			scriptRequestor.updateDisplay( state, message );
		else
			scriptRequestor.getLogStream().println( message );

		// There's a special case to be handled if the login was not
		// successful - in other words, attempt to prompt the user again

		if ( message.equals( "Login failed." ) )
			attemptLogin();
	}

	/**
	 * Attempts to remove the effect specified in the most recent command.
	 * If the string matches multiple effects, all matching effects will
	 * be removed.
	 */

	public void makeUneffectRequest()
	{
		AdventureResult currentEffect;
		String effectToUneffect = previousCommand.trim().substring( previousCommand.split( " " )[0].length() ).trim().toLowerCase();

		AdventureResult [] effects = new AdventureResult[ KoLCharacter.getEffects().size() ];
		KoLCharacter.getEffects().toArray( effects );

		for ( int i = 0; i < effects.length; ++i )
			if ( effects[i].getName().toLowerCase().indexOf( effectToUneffect ) != -1 )
				(new UneffectRequest( scriptRequestor, effects[i] )).run();
	}

	/**
	 * Attempts to zap the specified item with the specified wand
	 */

	public void makeZapRequest()
	{
		AdventureResult wand = KoLCharacter.getZapper();

		if ( wand == null )
		{
			updateDisplay( ERROR_STATE, "You don't have an appropriate wand" );
			scriptRequestor.cancelRequest();
			return;
		}

		String command = previousCommand.split( " " )[0];
		String parameters = previousCommand.substring( command.length() ).trim();
		if ( parameters.length() == 0 )
		{
			updateDisplay( ERROR_STATE, "Zap what?" );
			scriptRequestor.cancelRequest();
			return;
		}

		AdventureResult item = getFirstMatchingItem( parameters, INVENTORY );
		if ( item == null )
			return;

		(new ZapRequest( scriptRequestor, wand, item )).run();
	}

	/**
	 * Retrieves the items specified in the most recent command.  If there
	 * are no clovers available, the request will abort.
	 */

	public void makeHermitRequest()
	{
		if ( scriptRequestor.hermitItems.isEmpty() )
		{
			(new HermitRequest( scriptRequestor )).run();
			if ( !scriptRequestor.permitsContinue() )
				return;
		}

		if ( previousCommand.indexOf( " " ) == -1 )
		{
			boolean clovers = scriptRequestor.hermitItems.contains( "ten-leaf clover" );
			updateDisplay( ENABLE_STATE, "Today is " + ( clovers ? "" : "not " ) + "a clover day." );
			return;
		}

		String command = previousCommand.split( " " )[0];
		String parameters = previousCommand.substring( command.length() ).trim();
		AdventureResult item = getFirstMatchingItem( parameters, NOWHERE, 1 );
		if ( item == null )
			return;

		String name = item.getName();
		if ( !scriptRequestor.hermitItems.contains( name ) )
		{
			updateDisplay( ERROR_STATE, "You can't get a " + name + " from the hermit today." );
			return;
		}

		int itemID = item.getItemID();
		int tradeCount = item.getCount();
		(new HermitRequest( scriptRequestor, itemID, tradeCount )).run();
	}

	/**
	 * Makes a trade with the trapper which exchanges all of your current
	 * furs for the given fur.
	 */

	public void makeTrapperRequest()
	{
		String command = previousCommand.split( " " )[0];
		String parameters = previousCommand.substring( command.length() ).trim();

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
				(new TrapperRequest( scriptRequestor, itemID, tradeCount ) ).run();
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
		if ( scriptRequestor.hunterItems.isEmpty() )
			(new BountyHunterRequest( scriptRequestor )).run();

		if ( previousCommand.indexOf( " " ) == -1 )
		{
			printList( scriptRequestor.hunterItems );
			return;
		}

		String item = previousCommand.substring( previousCommand.indexOf( " " ) ).trim();
		for ( int i = 0; i < scriptRequestor.hunterItems.size(); ++i )
			if ( ((String)scriptRequestor.hunterItems.get(i)).indexOf( item ) != -1 )
				(new BountyHunterRequest( scriptRequestor, TradeableItemDatabase.getItemID( (String) scriptRequestor.hunterItems.get(i) ) )).run();
	}

	/**
	 * Makes a request to the restaurant to purchase a meal.  If the item
	 * is not available, this method does not report an error.
	 */

	public void makeRestaurantRequest()
	{
		List items = scriptRequestor.restaurantItems;
		if ( items.isEmpty() )
			(new RestaurantRequest( scriptRequestor )).run();

		if ( previousCommand.indexOf( " " ) == -1 )
		{
			printList( items );
			return;
		}

		String item = previousCommand.substring( previousCommand.indexOf( " " ) ).trim();
		for ( int i = 0; i < items.size(); ++i )
		{
			String name = (String)items.get(i);
			if ( name.indexOf( item ) != -1 )
			{
				(new RestaurantRequest( scriptRequestor, name )).run();
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
		if ( previousCommand.indexOf( " " ) == -1 )
		{
			List cures = GalaktikRequest.retrieveCures( scriptRequestor );
			printList( cures );
			return;
		}

		// Cure "HP" or "MP"

		String cure = previousCommand.substring( previousCommand.indexOf( " " ) ).trim();

		int type = 0;
		if ( cure.equalsIgnoreCase( "hp" ) )
			type = GalaktikRequest.HP;
		else if ( cure.equalsIgnoreCase( "mp" ) )
			type = GalaktikRequest.MP;
		else
		{
			updateDisplay( NORMAL_STATE, "Unknown Doc Galaktik request" );
			return;
		}

		(new GalaktikRequest( scriptRequestor, type )).run();
	}

	/**
	 * Makes a request to the microbrewery to purchase a drink.  If the
	 * item is not available, this method does not report an error.
	 */

	public void makeMicrobreweryRequest()
	{
		List items = scriptRequestor.microbreweryItems;
		if ( items.isEmpty() )
			(new MicrobreweryRequest( scriptRequestor )).run();

		if ( previousCommand.indexOf( " " ) == -1 )
		{
			printList( items );
			return;
		}

		String item = previousCommand.substring( previousCommand.indexOf( " " ) ).trim();
		for ( int i = 0; i < items.size(); ++i )
		{
			String name = (String)items.get(i);
			if ( name.indexOf( item ) != -1 )
			{
				(new MicrobreweryRequest( scriptRequestor, name )).run();
				return;
			}
		}

		updateDisplay( ERROR_STATE, "The microbrewery isn't selling " + item + " today." );
	}

	/**
	 * Confirms whether or not the user wants to make a drunken
	 * request.  This should be called before doing requests when
	 * the user is in an inebrieted state.
	 *
	 * @return	<code>true</code> if the user wishes to adventure drunk
	 */

	protected boolean confirmDrunkenRequest()
	{	return false;
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
			updateDisplay( NORMAL_STATE, elements[i].toString() );
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

	private int getLineNumber()
	{
		return lineNumber - 1; //Parser saves one extra line for look-ahead
	}

	private class AdvancedScriptHandler
	{
		ScriptScope global;
		String line;
		String nextLine;

		public AdvancedScriptHandler()
		{
			escapeString = "//";
		}

		public void execute( String parameters) throws AdvancedScriptException
		{
			line = parameters;
			nextLine = getNextLine();
			global = parseScope( new ScriptType(TYPE_BOOLEAN), new ScriptVariableList(), null, false);

			if( line != null)
				{
				if(( line.length() >= 2) && ( line.substring(0,2).equals("?>")))
					{
					//File must end with either this or EOF
					}
				else
					{
					//Not every token was read!
					throw new AdvancedScriptException( "Script parsing error at line " + getLineNumber());
					}
				}

			printScope( global, 0);

			//Execute code here

			return;
		}

		private ScriptScope parseScope( ScriptType expectedType, ScriptVariableList variables, ScriptScope parentScope, boolean whileLoop) throws AdvancedScriptException
		{
			ScriptFunction f = null;
			ScriptVariable v = null;
			ScriptCommand c = null;
			ScriptType t = null;

			ScriptScope result = new ScriptScope( variables, parentScope);

			while( true)
			{
				if(( t = parseType()) == null)
					if(( c = parseCommand( expectedType, result, false, whileLoop)) != null)
						{
						result.addCommand( c);
						continue;
						}
					else
						//No type and no call -> done.
						break;

				if(( f = parseFunction( t, result)) != null)
					{
					result.addFunction( f);
					}
				else if(( v = parseVariable( t)) != null)
					{
					result.addVariable( v);
					if( currentToken().equals(";"))
						readToken(); //read ;
					else
						throw new AdvancedScriptException( "';' Expected at line " + getLineNumber());
					}
				else
					//Found a type but no function or variable to tie it to
					throw new AdvancedScriptException( "Script parse error at line " + getLineNumber());
			};
			if( !result.assertReturn())
				{
				if( expectedType.equals( TYPE_VOID))
					result.addCommand( new ScriptReturn( null, new ScriptType( TYPE_VOID)));
				else if( expectedType.equals( TYPE_BOOLEAN))
					result.addCommand( new ScriptReturn( new ScriptValue( new ScriptType( TYPE_BOOLEAN), 1), new ScriptType( TYPE_BOOLEAN)));
				else
					throw new AdvancedScriptException( "Missing return value at line " + getLineNumber());
				}
			return result;
		}

		private ScriptFunction parseFunction( ScriptType t, ScriptScope parentScope) throws AdvancedScriptException
		{
			Identifier			functionName;
			ScriptFunction			result;
			ScriptType			paramType = null;
			ScriptVariable			param = null;
			ScriptVariableList		paramList = null;
			ScriptVariableReference		paramRef = null;

			String lastParam = null;

			try
			{
				functionName = new Identifier( currentToken());
				if(( nextToken() == null) || (!nextToken().equals( "(")))
					return null;
				readToken(); //read Function name
				readToken(); //read (

				paramList = new ScriptVariableList();

				result = new ScriptFunction( functionName, t);
				while( !currentToken().equals( ")"))
					{
					if(( paramType = parseType()) == null)
						throw new AdvancedScriptException( " ')' Expected at line " + getLineNumber());
					if(( param = parseVariable( paramType)) == null)
						throw new AdvancedScriptException( " Identifier expected at line " + getLineNumber());
					if( !currentToken().equals( ")"))
						{
						if( !currentToken().equals( ","))
							throw new AdvancedScriptException( " ')' Expected at line " + getLineNumber());
						readToken(); //read comma
						}
					paramRef = new ScriptVariableReference( param);
					result.addVariableReference( paramRef);
					paramList.addElement( param);
					}
				readToken(); //read )
				if( !currentToken().equals( "{")) //Scope is a single call
					{
					result.setScope( new ScriptScope( parseCommand( t, parentScope, false, false), parentScope));
					for( param = paramList.getFirstVariable(); param != null; param = paramList.getNextVariable( param))
						{
						lastParam = param.getName().toString();
						result.getScope().addVariable( new ScriptVariable( param));
						}
					if( !result.getScope().assertReturn())
						throw new AdvancedScriptException( "Missing return value at line " + getLineNumber());
					}
				else
					{
					readToken(); //read {
					result.setScope( parseScope( t, paramList, parentScope, false));
					if( !currentToken().equals( "}"))
						throw new AdvancedScriptException( " '}' Expected at line " + getLineNumber());
					readToken(); //read }
					}
			}
			catch( IdentifierException e)
			{
				return null;
			}
			return result;
		}

		private ScriptVariable parseVariable( ScriptType t) throws IdentifierException
		{
			ScriptVariable result;

			result = new ScriptVariable( new Identifier( currentToken()), t);
			readToken(); //If creation of Identifier succeeded, go to next token.
			return result;
		}

		private ScriptCommand parseCommand( ScriptType functionType, ScriptScope scope, boolean noElse, boolean whileLoop) throws AdvancedScriptException
		{
			ScriptCommand result;

			if(( currentToken() != null) && ( currentToken().equals( "break")))
				{
				if( !whileLoop)
					throw new AdvancedScriptException( "break outside of loop at line " + getLineNumber());
				result = new ScriptCommand( COMMAND_BREAK);
				readToken(); //break
				}

			else if(( currentToken() != null) && ( currentToken().equals( "continue")))
				{
				if( !whileLoop)
					throw new AdvancedScriptException( "break outside of loop at line " + getLineNumber());
				result = new ScriptCommand( COMMAND_CONTINUE);
				readToken(); //continue
				}


			else if(( result = parseReturn( functionType, scope)) != null)
				;
			else if(( result = parseLoop( functionType, scope, noElse, whileLoop)) != null)
				//loop doesn't have a ; token
				return result;
			else if(( result = parseCall( scope)) != null)
				;
			else if(( result = parseAssignment( scope)) != null)
				;
			else
				return null;

			if(( currentToken() == null) || ( !currentToken().equals(";")))
				throw new AdvancedScriptException( "';' Expected at line " + getLineNumber());
			readToken(); // ;

			return result;
		}

		private ScriptType parseType()
		{
			if( currentToken() == null)
				return null;
			String type = currentToken();
			ScriptType result;
			try
			{
				result = new ScriptType( type);
				readToken();
				return result;
			}
			catch( WrongTypeException e)
			{
				return null;
			}

		}

		private ScriptReturn parseReturn( ScriptType expectedType, ScriptScope parentScope) throws AdvancedScriptException
		{
			
			ScriptExpression expression = null;

			if(( currentToken() == null) || !( currentToken().equals( "return")))
				return null;
			readToken(); //return
			if(( currentToken() != null) && ( currentToken().equals(";")))
			{
				if( expectedType.equals(TYPE_VOID))
				{
					return new ScriptReturn( null, new ScriptType( TYPE_VOID));
				}
				else
					throw new AdvancedScriptException( "Return needs value at line " + getLineNumber());
			}
			else
			{
				if(( expression = parseExpression( parentScope)) == null)
					throw new AdvancedScriptException( "Expression expected at line " + getLineNumber());
				return new ScriptReturn( expression, expectedType);
			}
		}


		private ScriptLoop parseLoop( ScriptType functionType, ScriptScope parentScope, boolean noElse, boolean loop) throws AdvancedScriptException
		{
			ScriptScope		scope;
			ScriptExpression	expression;
			ScriptLoop		result = null;
			ScriptLoop		currentLoop = null;
			ScriptCommand		command = null;
			boolean			repeat = false;
			boolean			elseFound = false;
			boolean			finalElse = false;

			if( currentToken() == null)
				return null;

			if( (currentToken().equals( "while") && ( repeat = true)) || currentToken().equals("if"))
			{

				if(( nextToken() == null) || ( !nextToken().equals("(")))
					throw new AdvancedScriptException( "'(' Expected at line " + getLineNumber());
				readToken(); //if or while
				readToken(); //(
				expression = parseExpression( parentScope);
				if(( currentToken() == null) || ( !currentToken().equals(")")))
					throw new AdvancedScriptException( "')' Expected at line " + getLineNumber());
				readToken(); //)

				do
				{
					

					if(( currentToken() == null) || ( !currentToken().equals( "{"))) //Scope is a single call
					{
						command = parseCommand( functionType, parentScope, !elseFound, (repeat || loop));
						scope = new ScriptScope( command, parentScope);
						if( result == null)
							result = new ScriptLoop( scope, expression, repeat);
					}
					else
					{
						readToken(); //read {
						scope = parseScope( functionType, null, parentScope, (repeat || loop));
						if(( currentToken() == null) || ( !currentToken().equals( "}")))
							throw new AdvancedScriptException( " '}' Expected at line " + getLineNumber());
						readToken(); //read }
						if( result == null)
							result = new ScriptLoop( scope, expression, repeat);
						else
							result.addElseLoop( new ScriptLoop( scope, expression, false));
					}
					if( !repeat && !noElse && ( currentToken() != null) && currentToken().equals( "else"))
					{

						if( finalElse)
							throw new AdvancedScriptException( "Else without if at line " + getLineNumber());

						if(( nextToken() != null) && nextToken().equals( "if"))
						{
							readToken(); //else
							readToken(); //if
							if(( currentToken() == null) || ( !currentToken().equals("(")))
								throw new AdvancedScriptException( "'(' Expected at line " + getLineNumber());
							readToken(); //(
							expression = parseExpression( parentScope);
							if(( currentToken() == null) || ( !currentToken().equals(")")))
								throw new AdvancedScriptException( "')' Expected at line " + getLineNumber());
							readToken(); //)
						}
						else //else without condition
						{
							readToken(); //else
							expression = new ScriptValue( new ScriptType(TYPE_BOOLEAN), 1);
							finalElse = true;
						}
						elseFound = true;
						continue;
					}
					elseFound = false;
				} while( elseFound);
			}
			else
				return null;
			return result;
		}

		private ScriptCall parseCall( ScriptScope scope) throws AdvancedScriptException
		{
			Identifier			name = null;
			Identifier			varName;
			ScriptCall			result;
			ScriptExpressionList		params;
			ScriptExpression		val;

			if(( nextToken() == null) || !nextToken().equals( "("))
				return null;
			try
			{
				name = new Identifier( currentToken());
			}
			catch(IdentifierException e)
			{
				return null;
			}
			readToken(); //name
			readToken(); //(

			params = new ScriptExpressionList();
			while(( currentToken() != null) && (!currentToken().equals( ")")))
			{
				if(( val = parseExpression( scope)) != null)
				{
					params.addElement( val);
				}
				if( !currentToken().equals( ","))
				{
					if( !currentToken().equals( ")"))
						throw new AdvancedScriptException( "')' Expected at line " + getLineNumber());
				}
				else
				{
					readToken();
					if( currentToken().equals( ")"))
						throw new AdvancedScriptException( "Parameter expected at line " + getLineNumber());
				}
			}
			if( !currentToken().equals( ")"))
				throw new AdvancedScriptException( "')' Expected at line " + getLineNumber());
			readToken(); //)
			result = new ScriptCall( name, scope, params);

			return result;
		}

		private ScriptAssignment parseAssignment( ScriptScope scope) throws AdvancedScriptException
		{
			Identifier			name;
			ScriptVariableReference		leftHandSide;
			ScriptExpression		rightHandSide;

			if(( nextToken() == null) || ( !nextToken().equals( "=")))
				return null;

			try
			{
				name = new Identifier( currentToken());
			}
			catch(IdentifierException e)
			{
				return null;
			}
			readToken(); //name
			readToken(); //=
			leftHandSide = new ScriptVariableReference( name, scope);
			rightHandSide = parseExpression( scope);
			return new ScriptAssignment( leftHandSide, rightHandSide);
		}

		private ScriptExpression parseExpression( ScriptScope scope) throws AdvancedScriptException
		{
			return parseExpression( scope, null);
		}
		private ScriptExpression parseExpression( ScriptScope scope, ScriptOperator previousOper) throws AdvancedScriptException
		{
			ScriptExpression	lhs = null;
			ScriptExpression	rhs = null;
			ScriptOperator		oper = null;

			if( currentToken() == null)
				return null;

			if(( !( currentToken() == null)) && currentToken().equals("!"))
				{
				readToken(); // !
				if(( lhs = parseValue( scope)) == null)
					throw new AdvancedScriptException( "Value expected at line " + getLineNumber());
				lhs = new ScriptExpression( lhs, null, new ScriptOperator( "!"));
				}
			else
				{
				if(( lhs = parseValue( scope)) == null)
					return null;
				}

			do
			{
				try
				{
					oper = new ScriptOperator(currentToken());
				}
				catch( OperatorException e)
				{
					return lhs;
				}

				if(( previousOper != null) && ( !oper.precedes( previousOper)))
				{
					return lhs;
				}

				readToken(); //operator

				rhs = parseExpression( scope, oper);
				lhs = new ScriptExpression( lhs, rhs, oper);
			} while( true);
			

			
		}

		private ScriptExpression parseValue( ScriptScope scope) throws AdvancedScriptException
		{
			ScriptExpression	result;
			int			i;

			if( currentToken() == null)
				return null;


			if( currentToken().equals("("))
				{
				readToken();// (
				result = parseExpression( scope);
				if(( currentToken() == null) || (!currentToken().equals(")")))
					throw new AdvancedScriptException( "')' Expected at line " + getLineNumber());
				readToken();// )
				return result;
				}


			//Parse true and false first since they are reserved words.
			if( currentToken().equals( "true"))
				{
				readToken();
				return new ScriptValue( new ScriptType( TYPE_BOOLEAN), 1);
				}
			else if( currentToken().equals( "false"))
				{
				readToken();
				return new ScriptValue( new ScriptType( TYPE_BOOLEAN), 0);
				}

			else if(( result = parseCall( scope)) != null)
				return result;

			else if(( result = parseVariableReference( scope)) != null)
				return result;

			else if(( currentToken().charAt( 0) >= '0') && ( currentToken().charAt( 0) <= '9'))
			{
				int resultInt;

				for( resultInt = 0, i = 0; i < currentToken().length(); i++)
					{
					if( !(( currentToken().charAt(i) >= '0') && ( currentToken().charAt(i) <= '9')))
						throw new AdvancedScriptException( "Digits followed by non-digits at " + getLineNumber());
					resultInt += ( resultInt * 10) + ( currentToken().charAt(i) - '0');
					}
				readToken(); //integer
				return new ScriptValue( new ScriptType( "int"), resultInt);
			}
			else if( currentToken().equals("\""))
			{
				//Directly work with line - ignore any "tokens" you meet until the string is closed
				for( i = 1; ; i++)
				{
					if( i == line.length())
					{
						throw new AdvancedScriptException( "No closing '\"' found at line " + getLineNumber());
					}
					if( line.charAt(i) == '"')
					{
						String resultString = line.substring( 1, i);
						line = line.substring( i + 1); //+ 1 to get rid of '"' token
						return new ScriptValue( new ScriptType( "string"), resultString);
					}
				}
			
			}
			else if( currentToken().equals( "$"))
			{
				ScriptType type;
				readToken();
				try
				{
					type = new ScriptType( currentToken());
				}
				catch( WrongTypeException e)
				{
					throw new AdvancedScriptException( "Unknown type " + currentToken() + " at line " + getLineNumber());
				}
				readToken(); //type
				if( !currentToken().equals("["))
					throw new AdvancedScriptException( "'[' Expected at line " + getLineNumber());
				for( i = 1; ; i++)
				{
					if( i == line.length())
					{
						throw new AdvancedScriptException( "No closing ']' found at line " + getLineNumber());
					}
					if( line.charAt(i) == ']')
					{
						String resultString = line.substring( 1, i);
						line = line.substring( i + 1); //+1 to get rid of ']' token
						return new ScriptValue( type, resultString);
					}
				}
			}
			return null;
		}

		private ScriptOperator parseOperator()
		{
			ScriptOperator result;
			if( currentToken() == null)
				return null;
			try
			{
				result = new ScriptOperator( currentToken());
			}
			catch( OperatorException e)
			{
				return null;
			}
			readToken(); //operator
			return result;
		}

		private ScriptVariableReference parseVariableReference( ScriptScope scope) throws AdvancedScriptException
		{
			ScriptVariableReference result = null;

			if( currentToken() == null)
				return null;

			try
			{
				Identifier name = new Identifier( currentToken());
				result = new ScriptVariableReference( name, scope);

				readToken();
				return result;
			}
			catch( IdentifierException e)
			{
				return null;
			}
		}

		private String currentToken()
		{
			fixLines();
			if( line == null)
				return null;
			return line.substring(0, tokenLength(line));
		}

		private String nextToken()
		{
			String result;

			fixLines();

			if( line == null)
				return null;
			if( tokenLength( line) < line.length())
				result = line.substring( tokenLength( line));
			else
				{
				if( nextLine == null)
					return null;
				return nextLine.substring(0, tokenLength(nextLine));
				}
			if( result.equals( ""))
				{
				if( nextLine == null)
					return null;
				return nextLine.substring(0, tokenLength(nextLine));
				}
			if( result.charAt(0) == ' ')
				result = result.substring( 1);

			return result.substring( 0, tokenLength( result));
		}

		private void readToken()
		{
			if( line == null)
				return;

			fixLines();

			line = line.substring( tokenLength( line));

		}

		private int tokenLength( String s)
		{
			int result;
			if( s == null)
				return 0;

			for( result = 0; result < s.length(); result++)
			{
				if(( result + 1 < s.length()) && tokenString( s.substring( result, result + 2)))
					{
					return result == 0 ? 2 : result;
					}

				if(( result < s.length()) && tokenString( s.substring( result, result + 1)))
					{
					return result == 0 ? 1 : result;
					}
			}
			return result; //== s.length()
		}

		private void fixLines()
		{
			if( line == null)
				return;

			while( line.equals( ""))
			{
				line = nextLine;
				nextLine = getNextLine();
				if( line == null)
					return;
			}
			while( line.charAt( 0) == ' ')
				line = line.substring( 1);

			if( nextLine == null)
				return;
			while( nextLine.equals( ""))
			{
				nextLine = getNextLine();
				if( nextLine == null)
					return;
			}
		}

		private boolean tokenString( String s)
		{
			if(s.length() == 1)
				{
				for(int i = 0; i < java.lang.reflect.Array.getLength( tokenList); i++)
					if( s.charAt( 0) == tokenList[i])
						return true;
				return false;
				}
			else
				{
				for(int i = 0; i < java.lang.reflect.Array.getLength( multiCharTokenList); i++)
					if( s.equals(multiCharTokenList[i]))
						return true;
				return false;
				}
		}
		

		private void printScope( ScriptScope scope, int indent) throws AdvancedScriptException
		{
			ScriptVariable	currentVar;
			ScriptFunction	currentFunc;
			ScriptCommand	currentCommand;


			indentLine( indent);
			try
			{
				KoLmafia.getLogStream().println( "<SCOPE " + scope.getType() + ">");
			}
			catch( MissingReturnException e)
			{
				KoLmafia.getLogStream().println( "<SCOPE (no return)>");
			}
			indentLine( indent + 1);
			KoLmafia.getLogStream().println( "<VARIABLES>");
			for( currentVar = scope.getFirstVariable(); currentVar != null; currentVar = scope.getNextVariable( currentVar))
				printVariable( currentVar, indent + 2);
			indentLine( indent + 1);
			KoLmafia.getLogStream().println( "<FUNCTIONS>");
			for( currentFunc = scope.getFirstFunction(); currentFunc != null; currentFunc = scope.getNextFunction( currentFunc))
				printFunction( currentFunc, indent + 2);
			indentLine( indent + 1);
			KoLmafia.getLogStream().println( "<COMMANDS>");
			for( currentCommand = scope.getFirstCommand(); currentCommand != null; currentCommand = scope.getNextCommand( currentCommand))
				printCommand( currentCommand, indent + 2);
		}

		private void printVariable( ScriptVariable var, int indent)
		{
			indentLine( indent);
			KoLmafia.getLogStream().println( "<VAR " + var.getType().toString() + " " + var.getName().toString() + ">");
		}

		private void printFunction( ScriptFunction func, int indent) throws AdvancedScriptException
		{
			indentLine( indent);
			KoLmafia.getLogStream().println( "<FUNC " + func.getType().toString() + " " + func.getName().toString() + ">");
			for( ScriptVariableReference current = func.getFirstParam(); current != null; current = func.getNextParam( current))
				printVariableReference( current, indent + 1);
			printScope( func.getScope(), indent + 1);
		}

		private void printCommand( ScriptCommand command, int indent) throws AdvancedScriptException
		{
			if( command instanceof ScriptReturn)
				printReturn( ( ScriptReturn) command, indent);
			else if( command instanceof ScriptLoop)
				printLoop( ( ScriptLoop) command, indent);
			else if( command instanceof ScriptCall)
				printCall( ( ScriptCall) command, indent);
			else if( command instanceof ScriptAssignment)
				printAssignment( ( ScriptAssignment) command, indent);
			else
			{
				indentLine( indent);
				KoLmafia.getLogStream().println( "<COMMAND " + command.toString() + ">");
			}
		}

		private void printReturn( ScriptReturn ret, int indent) throws AdvancedScriptException
		{
			indentLine( indent);
			KoLmafia.getLogStream().println( "<RETURN " + ret.getType().toString() + ">");
			if( !ret.getType().equals(TYPE_VOID))
				printExpression( ret.getExpression(), indent + 1);
		}

		private void printLoop( ScriptLoop loop, int indent) throws AdvancedScriptException
		{
			indentLine( indent);
			if( loop.repeats())
				KoLmafia.getLogStream().println( "<WHILE>");
			else
				KoLmafia.getLogStream().println( "<IF>");
			printExpression( loop.getCondition(), indent + 1);
			printScope( loop.getScope(), indent + 1);
			for( ScriptLoop currentElse = loop.getFirstElseLoop(); currentElse != null; currentElse = loop.getNextElseLoop( currentElse))
				printLoop( currentElse, indent + 1);
		}

		private void printCall( ScriptCall call, int indent) throws AdvancedScriptException
		{
			indentLine( indent);
			KoLmafia.getLogStream().println( "<CALL " + call.getTarget().getName().toString() + ">");
			for( ScriptExpression current = call.getFirstParam(); current != null; current = call.getNextParam( current))
				printExpression( current, indent + 1);
		}

		private void printAssignment( ScriptAssignment assignment, int indent) throws AdvancedScriptException
		{
			indentLine( indent);
			KoLmafia.getLogStream().println( "<ASSIGN " + assignment.getLeftHandSide().getName().toString() + ">");
			printExpression( assignment.getRightHandSide(), indent + 1);
			
		}

		private void printExpression( ScriptExpression expression, int indent) throws AdvancedScriptException
		{
			if( expression instanceof ScriptValue)
				printValue(( ScriptValue) expression, indent);
			else
			{
				printOperator( expression.getOperator(), indent);
				printExpression( expression.getLeftHandSide(), indent + 1);
				if( expression.getRightHandSide() != null) // ! operator
					printExpression( expression.getRightHandSide(), indent + 1);
			}
		}

		public void printValue( ScriptValue value, int indent) throws AdvancedScriptException
		{
			if( value instanceof ScriptVariableReference)
				printVariableReference((ScriptVariableReference) value, indent);
			else if( value instanceof ScriptCall)
				printCall((ScriptCall) value, indent);
			else
			{
				indentLine( indent);
				KoLmafia.getLogStream().println( "<VALUE " + value.getType().toString() + " [" + value.toString() + "]>");
			}
		}

		public void printOperator( ScriptOperator oper, int indent)
		{
			indentLine( indent);
			KoLmafia.getLogStream().println( "<OPER " + oper.toString() + ">");
		}

		public void printVariableReference( ScriptVariableReference varRef, int indent)
		{
			indentLine( indent);
			KoLmafia.getLogStream().println( "<VARREF> " + varRef.getName().toString());
		}

		private void indentLine( int indent)
		{
			for(int i = 0; i < indent; i++)
				KoLmafia.getLogStream().print( "   ");
		}

	}

	private class AdvancedScriptException extends Exception
	{
		AdvancedScriptException( String s)
		{
			super( s);
		}
	}

	private class WrongTypeException extends AdvancedScriptException
	{
		WrongTypeException( String s)
		{
			super( s);
		}
	}


	private class IdentifierException extends AdvancedScriptException
	{
		IdentifierException( String s)
		{
			super( s);
		}
	}

	private class OperatorException extends AdvancedScriptException
	{
		OperatorException( String s)
		{
			super( s);
		}
	}

	private class NotACommandException extends AdvancedScriptException
	{
		NotACommandException( String s)
		{
			super( s);
		}
	}

	private class MissingReturnException extends AdvancedScriptException
	{
		MissingReturnException( String s)
		{
			super( s);
		}
	}



	private class ScriptScope extends ScriptListNode
	{
		ScriptFunctionList	functions;
		ScriptVariableList	variables;
		ScriptCommandList	commands;
		ScriptScope		parentScope;

		public ScriptScope( ScriptScope parentScope)
		{
			functions = new ScriptFunctionList();
			variables = new ScriptVariableList();
			commands = new ScriptCommandList();
			this.parentScope = parentScope;
		}

		public ScriptScope( ScriptCommand command, ScriptScope parentScope)
		{
			functions = new ScriptFunctionList();
			variables = new ScriptVariableList();
			commands = new ScriptCommandList( command);
			this.parentScope = parentScope;
		}

		public ScriptScope( ScriptVariableList variables, ScriptScope parentScope)
		{
			functions = new ScriptFunctionList();
			if( variables == null)
				variables = new ScriptVariableList();
			this.variables = variables;
			commands = new ScriptCommandList();
			this.parentScope = parentScope;
		}

		public void addFunction( ScriptFunction f) throws AdvancedScriptException
		{
			functions.addElement( f);
		}

		public void addVariable( ScriptVariable v) throws AdvancedScriptException
		{
			variables.addElement( v);
		}

		public void addCommand( ScriptCommand c) throws AdvancedScriptException
		{
			commands.addElement( c);
		}

		public ScriptScope getParentScope()
		{
			return parentScope;
		}

		public ScriptFunction getFirstFunction()
		{
			return ( ScriptFunction) functions.getFirstElement();
		}

		public ScriptFunction getNextFunction( ScriptFunction current)
		{
			return ( ScriptFunction) functions.getNextElement( current);
		}

		public ScriptVariable getFirstVariable()
		{
			return ( ScriptVariable) variables.getFirstElement();
		}

		public ScriptVariable getNextVariable( ScriptVariable current)
		{
			return ( ScriptVariable) variables.getNextElement( current);
		}

		public ScriptCommand getFirstCommand()
		{
			return ( ScriptCommand) commands.getFirstElement();
		}

		public ScriptCommand getNextCommand( ScriptCommand current)
		{
			return ( ScriptCommand) commands.getNextElement( current);
		}

		public boolean assertReturn()
		{
			ScriptCommand current, previous = null;

			for( current = getFirstCommand(); current != null; previous = current, current = getNextCommand( current))
				;
			if( previous == null)
				return false;
			if( !( previous instanceof ScriptReturn))
				return false;
			return true;
		}

		public ScriptType getType() throws MissingReturnException
		{
			ScriptCommand current = null;
			ScriptCommand previous = null;

			for( current = getFirstCommand(); current != null; previous = current, current = getNextCommand( current))
				;
			if( previous == null)
				throw new MissingReturnException( "Missing return!");
			if( !( previous instanceof ScriptReturn))
				throw new MissingReturnException( "Missing return!");
			return ((ScriptReturn) previous).getType();
		}

	}

	private class ScriptScopeList extends ScriptList
	{
		public void addElement( ScriptListNode n) throws AdvancedScriptException
		{
			addElementSerial( n);
		}
	}

	private class ScriptFunction extends ScriptListNode
	{
		Identifier				name;
		ScriptType				type;
		ScriptVariableReferenceList		variables;
		ScriptScope				scope;

		public ScriptFunction( Identifier name, ScriptType type)
		{
			this.name = name;
			this.type = type;
			this.variables = new ScriptVariableReferenceList();
			this.scope = null;
		}

		public void addVariableReference( ScriptVariableReference v) throws AdvancedScriptException
		{
			variables.addElement( v);
		}

		public void setScope( ScriptScope s)
		{
			scope = s;
		}

		public ScriptScope getScope()
		{
			return scope;
		}

		public int compareTo( Object o) throws ClassCastException
		{
			if(!(o instanceof ScriptFunction))
				throw new ClassCastException();
			return name.compareTo( (( ScriptFunction) o).name);
		}

		public Identifier getName()
		{
			return name;
		}

		public ScriptVariableReference getFirstParam()
		{
			return (ScriptVariableReference) variables.getFirstElement();
		}

		public ScriptVariableReference getNextParam( ScriptVariableReference current)
		{
			return (ScriptVariableReference) variables.getNextElement( current);
		}

		public ScriptType getType()
		{
			return type;
		}
	}

	private class ScriptFunctionList extends ScriptList
	{

	}

	private class ScriptVariable extends ScriptListNode
	{
		Identifier name;
		ScriptType type;

		int contentInt;
		String contentString;

		public ScriptVariable( Identifier name, ScriptType type)
		{
			this.name = name;
			this.type = type;
			this.contentInt = 0;
			this.contentString = "";
		}

		public ScriptVariable( ScriptVariable original)
		{
			name = original.name;
			type = original.type;
			setNext( null);
		}

		public int compareTo( Object o) throws ClassCastException
		{
			if(!(o instanceof ScriptVariable))
				throw new ClassCastException();
			return name.compareTo( (( ScriptVariable) o).name);
	
		}

		public ScriptType getType()
		{
			return type;
		}

		public Identifier getName()
		{
			return name;
		}
	}

	private class ScriptVariableList extends ScriptList
	{
		public ScriptVariable getFirstVariable()
		{
			return ( ScriptVariable) getFirstElement();
		}

		public ScriptVariable getNextVariable( ScriptVariable current)
		{
			return ( ScriptVariable) getNextElement( current);
		}
	}

	private class ScriptVariableReference extends ScriptValue
	{
		ScriptVariable target;

		public ScriptVariableReference( ScriptVariable target) throws AdvancedScriptException
		{
			this.target = target;
			if( !target.getType().equals( getType()))
				throw new AdvancedScriptException( "Cannot apply " + target.getType().toString() + " to " + getType().toString() + " at line " + getLineNumber());
		}

		public ScriptVariableReference( Identifier varName, ScriptScope scope) throws AdvancedScriptException
		{
			target = findVariable( varName, scope);
		}

		private ScriptVariable findVariable( Identifier name, ScriptScope scope) throws AdvancedScriptException
		{
			ScriptVariable current;

			do
			{
				for(current = scope.getFirstVariable(); current != null; current = scope.getNextVariable( current))
				{
					if( current.getName().equals( name))
						{
						return current;
						}
				}
				scope = scope.getParentScope();
			} while( scope != null);
			
			throw new AdvancedScriptException( "Undefined variable " + name + " at line " + getLineNumber());
		}

		public ScriptType getType()
		{
			return target.getType();
		}

		public Identifier getName()
		{
			return target.getName();
		}


		public int compareTo( Object o) throws ClassCastException
		{
			if(!(o instanceof ScriptVariableReference))
				throw new ClassCastException();
			return target.getName().compareTo( (( ScriptVariableReference) o).target.getName());
	
		}

	}

	private class ScriptVariableReferenceList extends ScriptList
	{
		public void addElement( ScriptListNode n) throws AdvancedScriptException
		{
			addElementSerial( n);
		}
	}

	private class ScriptCommand extends ScriptListNode
	{
		int command;


		public ScriptCommand()
		{
			
		}

		public ScriptCommand( String command) throws NotACommandException
		{
			if( command.equals( "break"))
				this.command = COMMAND_BREAK;
			else if( command.equals( "continue"))
				this.command = COMMAND_CONTINUE;
			else
				throw new NotACommandException( command + " is not a command at line " + getLineNumber());
		}

		public ScriptCommand( int command)
		{
			this.command = command;
		}

		public int compareTo( Object o) throws ClassCastException
		{
			if(!(o instanceof ScriptCommand))
				throw new ClassCastException();
			return 0;
	
		}

		public String toString()
		{
			if( this.command == COMMAND_BREAK)
				return "break";
			else if( this.command == COMMAND_CONTINUE)
				return "continue";
			return "<unknown command>";
		}
	}

	private class ScriptCommandList extends ScriptList
	{

		public ScriptCommandList()
			{
			super();
			}

		public ScriptCommandList( ScriptCommand c)
			{
			super( c);
			}

		public void addElement( ScriptListNode n) throws AdvancedScriptException //Call List has to remain in original order, so override addElement
		{
			addElementSerial( n);
		}
	}

	private class ScriptReturn extends ScriptCommand
	{
		private ScriptExpression returnValue;

		public ScriptReturn( ScriptExpression returnValue, ScriptType expectedType) throws AdvancedScriptException
		{
			this.returnValue = returnValue;
			if( !returnValue.getType().equals( expectedType))
				throw new AdvancedScriptException( "Cannot apply " + returnValue.getType().toString() + " to " + expectedType.toString() + " at line " + getLineNumber());
		}

		public ScriptType getType()
		{
			return returnValue.getType();
		}

		public ScriptExpression getExpression()
		{
			return returnValue;
		}
	}


	private class ScriptLoop extends ScriptCommand
	{
		private boolean			repeat;
		private ScriptExpression	condition;
		private ScriptScope		scope;
		private ScriptLoopList		elseLoops;

		public ScriptLoop( ScriptScope scope, ScriptExpression condition, boolean repeat) throws AdvancedScriptException
		{
			this.scope = scope;
			this.condition = condition;
			if( !( condition.getType().equals( TYPE_BOOLEAN)))
				throw new AdvancedScriptException( "Cannot apply " + condition.getType().toString() + " to boolean at line " + getLineNumber());
			this.repeat = repeat;
			elseLoops = new ScriptLoopList();
		}

		public boolean repeats()
		{
			return repeat;
		}

		public ScriptExpression getCondition()
		{
			return condition;
		}

		public ScriptScope getScope()
		{
			return scope;
		}

		public ScriptLoop getFirstElseLoop()
		{
			return ( ScriptLoop) elseLoops.getFirstElement();
		}

		public ScriptLoop getNextElseLoop( ScriptLoop current)
		{
			return ( ScriptLoop) elseLoops.getNextElement( current);
		}


		public void addElseLoop( ScriptLoop elseLoop) throws AdvancedScriptException
		{
			if( repeat == true)
				throw new AdvancedScriptException( "Else without if at line " + getLineNumber());
			elseLoops.addElement( elseLoop);
		}
	}


	private class ScriptLoopList extends ScriptList
	{

		public void addElement( ScriptListNode n) throws AdvancedScriptException
		{
			addElementSerial( n);
		}
	}

	private class ScriptCall extends ScriptValue
	{
		private ScriptFunction				target;
		private ScriptExpressionList			params;

		public ScriptCall( Identifier functionName, ScriptScope scope, ScriptExpressionList params) throws AdvancedScriptException
		{
			target = findFunction( functionName, scope, params);
			this.params = params;
		}

		private ScriptFunction findFunction( Identifier name, ScriptScope scope, ScriptExpressionList params) throws AdvancedScriptException
		{
			ScriptFunction		current;
			ScriptVariableReference	currentParam;
			ScriptExpression	currentValue;
			int			paramIndex;

			if( scope == null)
				throw new AdvancedScriptException( "Undefined reference " + name + " at line " + getLineNumber());
			do
			{
				for( current = scope.getFirstFunction(); current != null; current = scope.getNextFunction( current))
				{
					if( current.getName().equals( name))
					{
						for
						(
							paramIndex = 1, currentParam = current.getFirstParam(), currentValue = (ScriptExpression) params.getFirstElement();
							(currentParam != null) && (currentValue != null);
							paramIndex++, currentParam = current.getNextParam( currentParam), currentValue = ( ScriptExpression) params.getNextElement( currentValue)
						)
						{
							if( !currentParam.getType().equals( currentValue.getType()))
								throw new AdvancedScriptException( "Illegal parameter " + paramIndex + " for function " + name + ", got " + currentValue.getType() + ", need " + currentParam.getType() + " at line " + getLineNumber());
						}
						if(( currentParam != null) || ( currentValue != null))
							throw new AdvancedScriptException( "Illegal amount of parameters for function " + name + " at line " + getLineNumber());
						return current;
					}
				}
				scope = scope.getParentScope();
			} while( scope != null);
			throw new AdvancedScriptException( "Undefined reference " + name + " at line " + getLineNumber());
		}

		public ScriptFunction getTarget()
		{
			return target;
		}

		public ScriptExpression getFirstParam()
		{
			return ( ScriptExpression) params.getFirstElement();
		}

		public ScriptExpression getNextParam( ScriptExpression current)
		{
			return ( ScriptExpression) params.getNextElement( current);
		}

		public ScriptType getType()
		{
			return target.getType();
		}
	}

	private class ScriptAssignment extends ScriptCommand
	{
		private ScriptVariableReference	leftHandSide;
		private ScriptExpression	rightHandSide;

		public ScriptAssignment( ScriptVariableReference leftHandSide, ScriptExpression rightHandSide) throws AdvancedScriptException
		{
			this.leftHandSide = leftHandSide;
			this.rightHandSide = rightHandSide;
			if( !leftHandSide.getType().equals( rightHandSide.getType()))
				throw new AdvancedScriptException( "Cannot apply " + rightHandSide.getType().toString() + " to " + leftHandSide.toString() + " at line " + getLineNumber());
		}

		public ScriptVariableReference getLeftHandSide()
		{
			return leftHandSide;
		}

		public ScriptExpression getRightHandSide()
		{
			return rightHandSide;
		}

		public ScriptType getType()
		{
			return leftHandSide.getType();
		}
	}

	private class ScriptType
	{
		int type;
		public ScriptType( String s) throws WrongTypeException
		{
			if( s.equals( "void"))
				type = TYPE_VOID;
			else if( s.equals( "boolean"))
				type = TYPE_BOOLEAN;
			else if( s.equals( "int"))
				type = TYPE_INT;
			else if( s.equals( "string"))
				type = TYPE_STRING;
			else if( s.equals( "item"))
				type = TYPE_ITEM;
			else if( s.equals( "zodiac"))
				type = TYPE_ZODIAC;
			else if( s.equals( "location"))
				type = TYPE_LOCATION;
			else
				throw new WrongTypeException( "Wrong type identifier " + s + " at line " + getLineNumber());
		}

		public ScriptType( int type)
		{
			this.type = type;
		}

		public boolean equals( ScriptType type)
		{
			if( this.type == type.type)
				return true;
			return false;
		}

		public boolean equals( int type)
		{
			if( this.type == type)
				return true;
			return false;
		}

		public String toString()
		{
			if( type == TYPE_VOID)
				return "void";
			if( type == TYPE_BOOLEAN)
				return "boolean";
			if( type == TYPE_INT)
				return "int";
			if( type == TYPE_STRING)
				return "string";
			if( type == TYPE_ITEM)
				return "item";
			if( type == TYPE_ZODIAC)
				return "zodiac";
			if( type == TYPE_LOCATION)
				return "location";
			return "<unknown type>";
		}

	}

	private class ScriptValue extends ScriptExpression
	{
		ScriptType type;

		int contentInt;
		String contentString;

		public ScriptValue()
		{
			//stub constructor for subclasses
			//should not be called
		}


		public ScriptValue( ScriptType type, int contentInt)
		{
			this.type = type;
			this.contentInt = contentInt;
			contentString = null;
		}

		public ScriptValue( ScriptType type, String contentString)
		{
			this.type = type;
			this.contentString = contentString;
			contentInt = 0;
		}

		public int compareTo( Object o) throws ClassCastException
		{
			if(!(o instanceof ScriptValue))
				throw new ClassCastException();
			return 0;
	
		}

		public ScriptType getType()
		{
			return type;
		}

		public String toString()
		{
			if( contentString != null)
				return contentString;
			else
				return Integer.toString( contentInt);
		}
	}

	private class ScriptExpression extends ScriptCommand
	{
		ScriptExpression	lhs;
		ScriptExpression	rhs;
		ScriptOperator		oper;

		public ScriptExpression(ScriptExpression lhs, ScriptExpression rhs, ScriptOperator oper) throws AdvancedScriptException
		{
			this.lhs = lhs;
			this.rhs = rhs;
			if(( rhs != null) && !lhs.getType().equals( rhs.getType()))
				throw new AdvancedScriptException( "Cannot apply " + lhs.getType().toString() + " to " + rhs.getType().toString() + " at line " + getLineNumber());
			this.oper = oper;
		}


		public ScriptExpression()
		{
			//stub constructor for subclasses
			//should not be called
		}

		public ScriptType getType()
		{
			if( oper.isBool())
				return new ScriptType( TYPE_BOOLEAN);
			else
				return lhs.getType();
			
		}

		public ScriptExpression getLeftHandSide()
		{
			return lhs;
		}

		public ScriptExpression getRightHandSide()
		{
			return rhs;
		}

		public ScriptOperator getOperator()
		{
			return oper;
		}
	}

	private class ScriptExpressionList extends ScriptList
	{
		public void addElement( ScriptListNode n) throws AdvancedScriptException //Call List has to remain in original order, so override addElement
		{
			addElementSerial( n);
		}
	}

	private class ScriptOperator
	{
		String operString;

		public ScriptOperator( String oper) throws OperatorException
		{
			if( oper == null)
				throw new OperatorException( "Internal error in ScriptOperator()");
			else if
			(
				oper.equals( "!") ||
				oper.equals( "*") || oper.equals( "/") || oper.equals( "%") ||
				oper.equals( "+") || oper.equals( "-") ||
				oper.equals( "<") || oper.equals( ">") || oper.equals( "<=") || oper.equals( ">=") ||
				oper.equals( "==") || oper.equals( "!=") || 
				oper.equals( "||") || oper.equals( "&&")
			)
			{
				operString = oper;
			}
			else
				throw new OperatorException( "Illegal operator " + oper + " at line " + getLineNumber());
		}

		public boolean precedes( ScriptOperator oper)
		{
			return operStrength() > oper.operStrength();
		}

		private int operStrength()
		{
			if( operString.equals( "!"))
				return 6;
			if( operString.equals( "*") || operString.equals( "/") || operString.equals( "%"))
				return 5;
			else if( operString.equals( "+") || operString.equals( "-"))
				return 4;
			else if( operString.equals( "<") || operString.equals( ">") || operString.equals( "<=") || operString.equals( ">="))
				return 3;
			else if( operString.equals( "==") || operString.equals( "!="))
				return 2;
			else if( operString.equals( "||") || operString.equals( "&&"))
				return 1;
			else
				return -1;
		}

		public boolean isBool()
		{
			if
			(
				operString.equals( "*") || operString.equals( "/") || operString.equals( "%") ||
				operString.equals( "+") || operString.equals( "-")
			)
				return false;
			else
				return true;
		
		}

		public String toString()
		{
			return operString;
		}
	}


	private class ScriptListNode implements Comparable
	{
		ScriptListNode next;

		public ScriptListNode()
		{
			this.next = null;
		}

		public ScriptListNode getNext()
		{
			return next;
		}

		public void setNext( ScriptListNode node)
		{
			next = node;
		}

		public int compareTo( Object o) throws ClassCastException
		{
			if(!(o instanceof ScriptListNode))
				throw new ClassCastException();
			return 0; //This should not happen since each extending class overrides this function
	
		}

	}

	private class ScriptList
	{
		ScriptListNode firstNode;

		public ScriptList()
		{
			firstNode = null;
		}

		public ScriptList( ScriptListNode node)
		{
			firstNode = node;
		}

		public void addElement( ScriptListNode n) throws AdvancedScriptException
		{
			ScriptListNode current;
			ScriptListNode previous = null;

			if( n.getNext() != null)
				throw new AdvancedScriptException( "Internal error: Element already in list.");

			if( firstNode == null)
				{
				firstNode = n;
				return;
				}
			for( current = firstNode; current != null; previous = current, current = current.getNext())
			{
				if( current.compareTo( n) <= 0)
					break;
			}
			if( current == null)
				previous.setNext( n);
			else
			{
				if( previous == null) //Insert in front of very first element
				{
					firstNode = n;
					firstNode.setNext( current);
				}
				else
				{
					previous.setNext( n);
					n.setNext( current);
				}
			}
		}

		public void addElementSerial( ScriptListNode n) throws AdvancedScriptException //Function for subclasses to override addElement with
		{
			ScriptListNode current;
			ScriptListNode previous = null;

			if( n.getNext() != null)
				throw new AdvancedScriptException( "Internal error: Element already in list.");

			if( firstNode == null)
				{
				firstNode = n;
				return;
				}
			for( current = firstNode; current != null; previous = current, current = current.getNext())
				;
			previous.setNext( n);
			
		}


		public ScriptListNode getFirstElement()
		{
			return firstNode;
		}

		public ScriptListNode getNextElement( ScriptListNode n)
		{
			return n.getNext();
		}

	}

	private class Identifier extends ScriptListNode
	{

		private StringBuffer s;

		public Identifier ( Identifier identifier)
		{
			s = new StringBuffer();
			s = identifier.s; 
		}

		Identifier( String start) throws IdentifierException
		{

			if( start.length() < 1)
				throw new IdentifierException( "Invalid Identifier - Identifier cannot be length 0. At line " + getLineNumber());
	
			if( !Character.isLetter( start.charAt( 0)) && (start.charAt( 0) != '_'))
		    	{
				throw new IdentifierException( "Invalid Identifier - Must start with a letter. At line " + getLineNumber());
			}
			s = new StringBuffer();
			s.append(start.charAt(0));
			for( int i = 1; i < start.length(); i++)
			{
				if( !Character.isLetterOrDigit( start.charAt( i)) && (start.charAt( i) != '_'))
	    			{
					throw new IdentifierException( "Invalid Identifier at position " + i + ": not a letter or digit. At line " + getLineNumber());
				}
				s.append(start.charAt(i));
			}
		}
    
		public void init( char c)
		{
			s = new StringBuffer();
			s.append( c);
    		}

		public void append( char c)
		{
			s.append( c);
    		}

		public char charAt (int index)
		{
			return s.charAt( index);
    		}

		public int aantalElementen()
		{
			return s.length();
		}

		public boolean equals(Object o)
		{
			if(!(o instanceof Identifier))
				return false;

			return (s.toString().equals((( Identifier) o).s.toString()));
		}

		public int compareTo( Object o) throws ClassCastException
		{
			if(!(o instanceof Identifier))
				throw new ClassCastException();

			return (s.toString().compareTo((( Identifier) o).s.toString()));
		}
		

		public Object clone()
		{
			return new Identifier(this);
		}
    
		public String toString()
		{
			return s.toString();
		}
	}

	
}

