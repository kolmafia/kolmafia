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
import java.util.Iterator;
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
	private static final int NOWHERE = 1;
	private static final int INVENTORY = 2;
	private static final int CREATION = 3;
	private static final int CLOSET = 4;

	protected String previousCommand;
	private PrintStream outputStream;
	private PrintStream mirrorStream;
	private BufferedReader commandStream;
	private KoLmafia scriptRequestor;
	private KoLmafiaCLI lastScript;

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
		this.scriptRequestor.resetContinueState();

		outputStream = this.scriptRequestor instanceof KoLmafiaCLI ? System.out : NullStream.INSTANCE;
		commandStream = new BufferedReader( new InputStreamReader( inputStream ) );
		mirrorStream = NullStream.INSTANCE;
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
			(new LoginRequest( scriptRequestor, username, password, false, true, false )).run();
		}
		catch ( IOException e )
		{
			// Something bad must of happened.  Blow up!
			// Or rather, print the stack trace and exit
			// with an error state.

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

	public void initialize( String loginname, String sessionID, boolean getBreakfast, boolean isQuickLogin )
	{
		if ( scriptRequestor != this )
			scriptRequestor.initialize( loginname, sessionID, getBreakfast, isQuickLogin );
		else
			super.initialize( loginname, sessionID, getBreakfast, isQuickLogin );

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

		if ( line == null || line.trim().length() == 0 || !(scriptRequestor instanceof KoLmafiaCLI) )
		{
			try
			{
				commandStream.close();
				previousCommand = null;
			}
			catch ( IOException e )
			{
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
			while ( line != null && (line.startsWith( "#" ) || line.trim().length() == 0) );
			return line == null ? null : line.trim();
		}
		catch ( IOException e )
		{
			// If an IOException occurs during the parsing of the
			// command, you should exit from the command with an
			// error state after printing the stack trace.

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
				scriptRequestor.deinitialize();
				(new LoginRequest( scriptRequestor, parameters, scriptRequestor.getSaveState( parameters ), false, true, false )).run();
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
			AdventureResult match = getFirstMatchingItem( parameters, INVENTORY );
			if ( match != null )
			{
				String item = match.getName();

				// The following loop removes ALL items with
				// the specified name.

				for ( int i = 0; i <= KoLCharacter.FAMILIAR; ++i )
					if ( KoLCharacter.getEquipment( i ).equals( item ) )
					     scriptRequestor.makeRequest( new EquipmentRequest( scriptRequestor, EquipmentRequest.UNEQUIP, i ), 1 );
			}
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
			       replaceAll( "&ntilde;", "ñ" ).
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
			return;
		}
	}

	/**
	 * Utility method to handle an if-statement.
	 */

	private void executeIfStatement( String parameters )
	{
		String statement = getNextLine();
		if ( testConditional( parameters ) )
			executeLine( statement );
	}

	/**
	 * Utility method to handle a while-statement.
	 */

	private void executeWhileStatement( String parameters )
	{
		String statement = getNextLine();
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

		String operator = parameters.indexOf( "==" ) != -1 ? "==" : parameters.indexOf( "!=" ) != -1 ? "!=" :
			parameters.indexOf( ">=" ) != -1 ? ">=" : parameters.indexOf( "<=" ) != -1 ? "<=" :
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

	private String getSkillName( String substring )
	{
		List skills = KoLCharacter.getUsableSkills();
		for ( int i = 0; i < skills.size(); ++i )
			if ( ((UseSkillRequest)skills.get(i)).getSkillName().toLowerCase().indexOf( substring ) != -1 )
				return ((UseSkillRequest) skills.get(i)).getSkillName();

		return null;
	}

	/**
	 * A special module used specifically for handling donations,
	 * including donations to the statues and donations to the clan.
	 */

	private void executeDonateCommand( String parameters )
	{
		int heroID;  int amount = -1;

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
		}
		catch ( Exception e )
		{
			if ( amount == -1 )
				updateDisplay( ERROR_STATE, parameterList[1] + " is not a number." );
			else
				updateDisplay( ERROR_STATE, parameterList[2] + " is not a number." );

			scriptRequestor.cancelRequest();
			return;
		}

		scriptRequestor.makeRequest( new HeroDonationRequest( scriptRequestor, heroID, amount ), 1 );
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
			Iterator itemIterator = mainList.iterator();

			while ( itemIterator.hasNext() )
			{
				currentItem = itemIterator.next().toString().toLowerCase();
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

	private AdventureResult getFirstMatchingItem( String parameters, int matchType, int def )
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

		if ( matchType != NOWHERE && itemCount > matchCount )
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
		AdventureResult firstMatch = getFirstMatchingItem( parameters, INVENTORY );
		if ( firstMatch == null )
			return;

		Object [] items = new Object[1];
		items[0] = firstMatch;

		scriptRequestor.makeRequest( new ClanStashRequest( scriptRequestor, items, ClanStashRequest.ITEMS_TO_STASH ), 1 );
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
		}
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

		String [] tokens = parameters.split( " " );
		StringBuffer itemName = new StringBuffer();

		itemName.append( '*' );
		for ( int i = 1; i < tokens.length; ++i )
		{
			itemName.append( ' ' );
			itemName.append( tokens[i] );
		}

		AdventureResult firstMatch = getFirstMatchingItem( itemName.toString(), parameters.startsWith( "take" ) ? CLOSET : INVENTORY );
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

		if ( itemCount == 1 || consumptionType == ConsumeItemRequest.CONSUME_MULTIPLE )
			scriptRequestor.makeRequest( new ConsumeItemRequest( scriptRequestor, new AdventureResult( itemName, itemCount ) ), 1 );
		else
			scriptRequestor.makeRequest( new ConsumeItemRequest( scriptRequestor, new AdventureResult( itemName, 1 ) ), itemCount );
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
		Iterator outfitIterator = KoLCharacter.getOutfits().iterator();
		SpecialOutfit intendedOutfit = null;
		Object currentOutfit;

		while ( intendedOutfit == null && outfitIterator.hasNext() )
		{
			currentOutfit = outfitIterator.next();
			if ( currentOutfit instanceof SpecialOutfit && currentOutfit.toString().toLowerCase().indexOf( lowercaseOutfitName ) != -1 )
				intendedOutfit = (SpecialOutfit) currentOutfit;
		}

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

		Iterator effectIterator = KoLCharacter.getEffects().iterator();

		while ( effectIterator.hasNext() )
		{
			currentEffect = (AdventureResult) effectIterator.next();
			if ( currentEffect.getName().toLowerCase().indexOf( effectToUneffect ) != -1 )
				(new UneffectRequest( scriptRequestor, currentEffect )).run();
		}
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
		try
		{
			String [] command = previousCommand.split( " " );

			int tradeCount = df.parse( command[1] ).intValue();
			String item = command[2];
			int itemNumber = -1;

			for ( int i = 0; itemNumber == -1 && i < hermitItemNames.length; ++i )
				if ( hermitItemNames[i].indexOf( item ) != -1 )
					(new HermitRequest( scriptRequestor, hermitItemNumbers[i], tradeCount )).run();
		}
		catch ( Exception e )
		{
		}
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
		Iterator printingIterator = printing.iterator();
		while ( printingIterator.hasNext() )
			updateDisplay( NORMAL_STATE, printingIterator.next().toString() );
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

		// Buffs are pretty neat, too - for now, it's
		// just casts on self

		if ( request instanceof UseSkillRequest )
		{
			commandString.append( "cast " );
			commandString.append( ((UseSkillRequest)request).getBuffCount() );
			commandString.append( ' ' );
			commandString.append( ((UseSkillRequest)request).getSkillName() );
		}

		// One item-related command is an item consumption
		// request.  In other words, eating, drinking, using.

		if ( request instanceof ConsumeItemRequest )
		{
			ConsumeItemRequest crequest = (ConsumeItemRequest) request;
			switch ( crequest.getConsumptionType() )
			{
				case ConsumeItemRequest.CONSUME_EAT:
					commandString.append( "eat " );
					break;

				case ConsumeItemRequest.CONSUME_DRINK:
					commandString.append( "drink " );
					break;

				default:
					commandString.append( "use " );
					break;
			}

			AdventureResult itemUsed = crequest.getItemUsed();
			commandString.append( iterations == 1 ? itemUsed.getCount() : iterations );
			commandString.append( " \"" );
			commandString.append( itemUsed.getName() );
			commandString.append( "\"" );
		}

		// Another item-related command is a creation
		// request.  These could vary based on item,
		// but rather than do that, just using the
		// generic "make" command.

		if ( request instanceof ItemCreationRequest )
		{
			ItemCreationRequest irequest = (ItemCreationRequest) request;

			commandString.append( "create " );
			commandString.append( irequest.getQuantityNeeded() );
			commandString.append( " \"" );
			commandString.append( irequest.getName() );
			commandString.append( "\"" );
		}

		// Item storage script recording is also a
		// little interesting.

		if ( request instanceof ItemStorageRequest )
		{
			ItemStorageRequest isr = (ItemStorageRequest) request;
			int moveType = isr.getMoveType();

			if ( moveType == ItemStorageRequest.INVENTORY_TO_CLOSET || moveType == ItemStorageRequest.CLOSET_TO_INVENTORY )
			{
				List itemList = isr.getItems();
				for ( int i = 0; i < itemList.size(); ++i )
				{
					if ( i != 0 )
						commandString.append( LINE_BREAK );

					commandString.append( moveType == ItemStorageRequest.INVENTORY_TO_CLOSET ? "closet put " : "closet take " );

					commandString.append( '\"' );
					commandString.append( ((AdventureResult)itemList.get(i)).getName() );
					commandString.append( '\"' );
				}
			}
		}

		// Deposits to the clan stash are also interesting
		// for people who like to regularly deposit to the
		// clan stash.

		if ( request instanceof ClanStashRequest )
		{
			ClanStashRequest csr = (ClanStashRequest) request;
			int moveType = csr.getMoveType();

			if ( moveType == ClanStashRequest.ITEMS_TO_STASH )
			{
				List itemList = csr.getItems();
				for ( int i = 0; i < itemList.size(); ++i )
				{
					if ( i != 0 )
						commandString.append( LINE_BREAK );

					commandString.append( "stash " );

					commandString.append( '\"' );
					commandString.append( ((AdventureResult)itemList.get(i)).getName() );
					commandString.append( '\"' );
				}
			}
		}

		// One of the largest commands is adventuring,
		// which (as usual) gets its own module.

		if ( request instanceof KoLAdventure )
		{
			String adventureName = ((KoLAdventure)request).getAdventureName();

			commandString.append( "adventure " );
			commandString.append( iterations );
			commandString.append( ' ' );
			commandString.append( adventureName );
		}

		// Donations get their own command and module,
		// too, which handles hero donations and basic
		// clan donations.

		if ( request instanceof HeroDonationRequest )
		{
			HeroDonationRequest hrequest = (HeroDonationRequest) request;

			commandString.append( "donate " );
			commandString.append( hrequest.getAmount() );
			commandString.append( hrequest.getHero() );
		}

		// Another popular command involves changing
		// your current familiar.

		if ( request instanceof FamiliarRequest )
		{
			String familiarName = ((FamiliarRequest)request).getFamiliarChange();
			if ( familiarName != null )
			{
				commandString.append( "familiar " );
				commandString.append( familiarName );
			}
		}

		// Yet another popular command involves changing
		// your outfit.

		if ( request instanceof EquipmentRequest )
		{
			String outfitName = ((EquipmentRequest)request).getOutfitName();
			if ( outfitName != null )
			{
				commandString.append( "outfit " );
				commandString.append( outfitName );
			}
		}

		if ( commandString.length() > 0 )
			commandString.append( LINE_BREAK );

		return commandString.toString();
	}
}

