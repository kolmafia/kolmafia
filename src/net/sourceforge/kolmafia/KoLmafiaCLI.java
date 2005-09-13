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
			KoLDatabase.client = session;

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
				updateDisplay( NOCHANGE, (result.startsWith( "You" ) ? " - " : " - Adventure result: ") + result );
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

		outputStream = this.scriptRequestor instanceof KoLmafiaCLI ? System.out : new NullStream();
		commandStream = new BufferedReader( new InputStreamReader( inputStream ) );
		mirrorStream = new NullStream();
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
			if ( scriptRequestor == this )
			{
				outputStream.println();
				outputStream.print( "username: " );
			}

			String username = commandStream.readLine();

			if ( username == null )
				return;

			if ( username.length() == 0 )
			{
				outputStream.println( "Invalid login." );
				return;
			}

			if ( !username.endsWith( "/q" ) )
				username += "/q";

			if ( scriptRequestor == this )
				outputStream.print( "password: " );

			String password = commandStream.readLine();

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
			(new LoginRequest( scriptRequestor, username, password, false, false, false )).run();
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
	 * the login has been confirmed to notify the client that the
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
			updateDisplay( NOCHANGE, "" );
			executeCommand( "moons", "" );
			updateDisplay( NOCHANGE, "" );
		}
	}

	/**
	 * A utility method which waits for commands from the user, then
	 * executing each command as it arrives.
	 */

	public void listenForCommands()
	{
		try
		{
			if ( scriptRequestor == this )
				outputStream.print( " > " );

			String line = null;
			scriptRequestor.resetContinueState();

			while ( (scriptRequestor.permitsContinue() || scriptRequestor == this) && (line = commandStream.readLine()) != null )
			{
				// Skip comment lines

				while ( line != null && (line.startsWith( "#" ) || line.trim().length() == 0) )
					line = commandStream.readLine();

				if ( line == null )
					return;

				if ( scriptRequestor == this )
					updateDisplay( NOCHANGE, "" );

				executeLine( line.trim() );

				if ( scriptRequestor == this )
					updateDisplay( NOCHANGE, "" );

				if ( scriptRequestor == this )
					outputStream.print( " > " );
			}

			if ( line == null || line.trim().length() == 0 || !(scriptRequestor instanceof KoLmafiaCLI) )
			{
				commandStream.close();
				previousCommand = null;
			}
		}
		catch ( IOException e )
		{
			// If an IOException occurs during the parsing of the
			// command, you should exit from the command with an
			// error state after printing the stack trace.

			e.printStackTrace();
			System.exit( -1 );
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
						scriptRequestor.updateDisplay( DISABLED_STATE, "Countdown: " + (seconds - i) + " seconds remaining..." );
					else
						outputStream.print( seconds - i + ", " );
				}
			}
			catch ( Exception e )
			{
			}

			updateDisplay( ENABLED_STATE, "Waiting completed." );
			return;
		}

		// Preconditions kickass, so they're handled right
		// after the wait command.  (Right)

		if ( command.equals( "conditions" ) )
		{
			executeConditionsCommand( parameters );
			return;
		}

		// Next, handle any requests to login or relogin.
		// This will be done by calling a utility method.

		if ( command.equals( "login" ) || command.equals( "relogin" ) )
		{
			scriptRequestor.deinitialize();
			attemptLogin();
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
				scriptRequestor.updateDisplay( DISABLED_STATE, "Logging out..." );
				(new LogoutRequest( scriptRequestor )).run();
			}

			scriptRequestor.updateDisplay( DISABLED_STATE, "Exiting KoLmafia..." );
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
				scriptRequestor.updateDisplay( ERROR_STATE, "No commands left to continue script." );
				scriptRequestor.cancelRequest();
				return;
			}

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
					for ( int i = 0; i < repeatCount; ++i )
						executeLine( previousCommand );
				}

				return;
			}
			catch ( Exception e )
			{
				// Notify the client that the command could not be repeated
				// the given number of times.

				scriptRequestor.updateDisplay( ERROR_STATE, parameters + " is not a number." );
				scriptRequestor.cancelRequest();
				return;
			}
		}

		// Next, print out the moon phase, if the user
		// wishes to know what moon phase it is.

		if ( command.startsWith( "moon" ) )
		{
			updateDisplay( NOCHANGE, "Ronald: " + MoonPhaseDatabase.getRonaldMoonPhase() );
			updateDisplay( NOCHANGE, "Grimace: " + MoonPhaseDatabase.getGrimaceMoonPhase() );
			updateDisplay( NOCHANGE, "" );
			updateDisplay( ENABLED_STATE, MoonPhaseDatabase.getMoonEffect() );

			return;
		}

		// Next, handle requests to start or stop
		// debug mode.

		if ( command.startsWith( "debug" ) )
		{
			if ( parameters.startsWith( "on" ) )
				scriptRequestor.initializeLogStream();
			else if ( parameters.startsWith( "off" ) )
				scriptRequestor.deinitializeLogStream();
			else
			{
				scriptRequestor.updateDisplay( ERROR_STATE, parameters + " is not a valid option." );
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
				this.mirrorStream = new NullStream();
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

					scriptRequestor.updateDisplay( ERROR_STATE, "I/O error in opening file \"" + parameters + "\"" );
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
				Object [] parameters = new Object[2];
				parameters[0] = scriptRequestor;
				parameters[1] = request;

				(new CreateFrameRunnable( RequestFrame.class, parameters )).run();
			}
			else
			{
				updateDisplay( NOCHANGE, request.responseText.substring( request.responseText.indexOf( "<p>" ) + 3 ).replaceAll(
					"<(br|p|blockquote)>", System.getProperty( "line.separator" ) ).replaceAll( "<.*?>", "" ).replaceAll(
						"&nbsp;", " " ).replaceAll( "&trade;", " [tm]" ).replaceAll( "&ntilde;", "ñ" ).replaceAll( "&quot;", "\"" ) );
			}

			return;
		}

		// If there's any commands which suggest that the
		// client is in a login state, you should not do
		// any commands listed beyond this point

		if ( scriptRequestor.inLoginState() )
		{
			scriptRequestor.updateDisplay( ERROR_STATE, "You have not yet logged in." );
			scriptRequestor.cancelRequest();
			return;
		}

		// Look!  It's the command to pwn clan Otori.
		// This is handled first because I'm lazy.

		if ( command.equals( "otori" ) )
		{
			scriptRequestor.pwnClanOtori();
			return;
		}

		// Look!  It's the command to complete the
		// Sorceress entryway!  There is no reason
		// why I put this after the otori command.

		if ( command.equals( "entryway" ) )
		{
			scriptRequestor.completeEntryway();
			printList( SorceressLair.getMissingItems() );
			return;
		}

		// Look!  It's the command to complete the
		// Sorceress hedge maze!  This is placed
		// right after for consistency.

		if ( command.equals( "hedgemaze" ) )
		{
			scriptRequestor.completeHedgeMaze();
			printList( SorceressLair.getMissingItems() );
			return;
		}

		// Look!  It's the command to fight the guardians
		// in the Sorceress's tower!  This is placed
		// right after for consistency.

		if ( command.equals( "guardians" ) )
		{
			scriptRequestor.fightTowerGuardians();
			printList( SorceressLair.getMissingItems() );
			return;
		}

		// Look!  It's the command to complete the Sorceress's
		// Chamber! This is placed right after for consistency.

		if ( command.equals( "chamber" ) )
		{
			scriptRequestor.completeSorceressChamber();
			printList( SorceressLair.getMissingItems() );
			return;
		}

		// Next is the command to rob the strange leaflet.
		// This method invokes the "robStrangeLeaflet" method
		// on the script requestor.

		if ( command.equals( "leaflet" ) )
		{
			scriptRequestor.robStrangeLeaflet();
			return;
		}

		// Next is the command to face your nemesis.  This
		// method invokes the "faceNemesis" method on the
		// script requestor.

		if ( command.equals( "nemesis" ) )
		{
			scriptRequestor.faceNemesis();
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

			if ( scriptRequestor instanceof KoLmafiaGUI )
			{
				Object [] parameters = new Object[2];
				parameters[0] = scriptRequestor;
				parameters[1] = request;

				(new CreateFrameRunnable( RequestFrame.class, parameters )).run();
			}
			else
			{
				updateDisplay( NOCHANGE, request.responseText.replaceAll(
					"<(br|p|blockquote)>", System.getProperty( "line.separator" ) ).replaceAll( "<.*?>", "" ).replaceAll(
						"&nbsp;", " " ).replaceAll( "&trade;", " [tm]" ).replaceAll( "&ntilde;", "ñ" ).replaceAll( "&quot;", "\"" ) );
			}

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
		// to print the current state of the client.  This
		// should be handled in a separate method, since
		// there are many things the client may want to print

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
				KoLCharacter data = scriptRequestor.getCharacterData();

				// The following loop removes ALL items with
				// the specified name.

				for ( int i = 0; i < KoLCharacter.FAMILIAR; ++i )
					if ( data.getEquipment( i ).equals( item ) )
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
			List familiars = scriptRequestor.getCharacterData().getFamiliarList();

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

		if ( command.equals( "gym" ) )
		{
			executeAdventureRequest( parameters );
			return;
		}

		if ( command.equals( "toast" ) || command.equals( "rest" ) || command.equals( "relax" ) || command.equals( "arches" ) )
		{
			executeCampgroundRequest( command + " " + parameters );
			return;
		}

		if ( command.startsWith( "inv" ) || command.equals( "session" ) || command.equals( "summary" ) ||
			command.equals( "effects" ) || command.startsWith( "status" ) )
		{
			executePrintCommand( command + " " + parameters );
			return;
		}

		// If all else fails, then assume that the
		// person was trying to call a script.

		executeScriptCommand( command + " " + parameters );
	}

	/**
	 * A special module used to handle the calling of a
	 * script.
	 */

	public void executeScriptCommand( String parameters )
	{
		try
		{
			// First, assume that it's inside of the scripts
			// directory and make an attempt to retrieve it
			// from there.

			lastScript = null;
			File scriptFile = new File( "scripts" + File.separator + parameters );

			if ( scriptFile.exists() )
				lastScript = new KoLmafiaCLI( scriptRequestor, new FileInputStream( scriptFile ) );
			else
			{
				scriptFile = new File( parameters );
				if ( scriptFile.exists() )
					lastScript = new KoLmafiaCLI( scriptRequestor, new FileInputStream( scriptFile ) );
			}

			if ( lastScript == null )
			{
				scriptRequestor.updateDisplay( ERROR_STATE, "Script file \"" + parameters + "\" could not be found." );
				scriptRequestor.cancelRequest();
				return;
			}

			lastScript.commandBuffer = commandBuffer;
			lastScript.listenForCommands();
			if ( lastScript.previousCommand == null )
				lastScript = null;
		}
		catch ( Exception e )
		{
			// Because everything is checked for consistency
			// before being loaded, this should not happen.

			scriptRequestor.updateDisplay( ERROR_STATE, "Script file \"" + parameters + "\" could not be found." );
			scriptRequestor.cancelRequest();
			return;
		}
	}

	/**
	 * A special module used to handle conditions requests.
	 * This determines what the user is planning to do with
	 * the condition, and then parses the condition to be
	 * added, and then adds it to the conditions list.
	 */

	private void executeConditionsCommand( String parameters )
	{
		String option = parameters.split( " " )[0];

		if ( option.equals( "clear" ) )
		{
			scriptRequestor.conditions.clear();
			return;
		}
		else if ( option.equals( "add" ) )
		{
			String conditionString = parameters.substring( option.length() ).trim();
			AdventureResult condition;

			if ( conditionString.endsWith( "choiceadv" ) )
			{
				// If it's a choice adventure condition, parse out the
				// number of choice adventures the user wishes to do.

				String [] splitCondition = conditionString.split( "\\s+" );
				condition = new AdventureResult( AdventureResult.ADV, splitCondition.length > 1 ? Integer.parseInt( splitCondition[0] ) : 1 );
			}
			else if ( conditionString.startsWith( "level" ) )
			{
				// If the condition is a level, then determine how many
				// substat points are required to the next level and
				// add the substat points as a condition.

				String [] splitCondition = conditionString.split( "\\s+" );
				int level = Integer.parseInt( splitCondition[1] );

				int [] subpoints = new int[3];
				int primeIndex = scriptRequestor.getCharacterData().getPrimeIndex();

				subpoints[ primeIndex ] = KoLCharacter.calculateSubpoints( (level - 1) * (level - 1) + 4, 0 ) -
					scriptRequestor.getCharacterData().getTotalPrime();

				for ( int i = 0; i < subpoints.length; ++i )
					subpoints[i] = Math.max( 0, subpoints[i] );

				condition = new AdventureResult( AdventureResult.SUBSTATS, subpoints );
				if ( condition.getCount() == 0 )
					condition = null;
			}
			else if ( conditionString.endsWith( "muscle" ) || conditionString.endsWith( "mysticality" ) || conditionString.endsWith( "moxie" ) )
			{
				try
				{
					String [] splitCondition = conditionString.split( "\\s+" );
					int points = df.parse( splitCondition[0] ).intValue();

					int [] subpoints = new int[3];
					int statIndex = conditionString.endsWith( "muscle" ) ? 0 : conditionString.endsWith( "mysticality" ) ? 1 : 2;
					subpoints[ statIndex ] = KoLCharacter.calculateSubpoints( points, 0 );

					subpoints[ statIndex ] -= conditionString.endsWith( "muscle" ) ? scriptRequestor.getCharacterData().getTotalMuscle() :
						conditionString.endsWith( "mysticality" ) ? scriptRequestor.getCharacterData().getTotalMysticality() :
						scriptRequestor.getCharacterData().getTotalMoxie();

					for ( int i = 0; i < subpoints.length; ++i )
						subpoints[i] = Math.max( 0, subpoints[i] );

					condition = new AdventureResult( AdventureResult.SUBSTATS, subpoints );
					if ( condition.getCount() == 0 )
						condition = null;
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
				// Otherwise, it's an item condition, so parse out which
				// item is desired and set that as the condition.

				condition = getFirstMatchingItem( conditionString, NOWHERE );
			}

			if ( condition != null )
			{
				AdventureResult.addResultToList( scriptRequestor.conditions, condition );
				updateDisplay( NOCHANGE, "Condition added: " + condition.toString() );
			}
		}

		printList( scriptRequestor.conditions );
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
			scriptRequestor.updateDisplay( ERROR_STATE, parameterList[1] + " is not a number." );
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
					scriptRequestor.updateDisplay( ERROR_STATE, "Skill not available" );
					return;
				}
			}

			try
			{
				buffCount = firstParameter.equals( "*" ) ?
					(int) ( scriptRequestor.getCharacterData().getCurrentMP() /
						ClassSkillsDatabase.getMPConsumptionByID( ClassSkillsDatabase.getSkillID( skillName ) ) ) :
							df.parse( firstParameter ).intValue();
			}
			catch ( Exception e )
			{
				// Technically, this exception should not be thrown, but if
				// it is, then print an error message and return.

				scriptRequestor.updateDisplay( ERROR_STATE, firstParameter + " is not a number." );
				scriptRequestor.cancelRequest();
				return;
			}

			scriptRequestor.makeRequest( new UseSkillRequest( scriptRequestor, skillName, null, buffCount ), 1 );
		}
	}

	/**
	 * Utility method used to retrieve the full name of a skill,
	 * given a substring representing it.
	 */

	private String getSkillName( String substring )
	{
		List skills = scriptRequestor.getCharacterData().getAvailableSkills();
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
			scriptRequestor.updateDisplay( ERROR_STATE, parameters + " is not a statue." );
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
				scriptRequestor.updateDisplay( ERROR_STATE, parameterList[1] + " is not a number." );
			else
				scriptRequestor.updateDisplay( ERROR_STATE, parameterList[2] + " is not a number." );

			scriptRequestor.cancelRequest();
			return;
		}

		int amountRemaining = amount;
		int eachAmount = amountRemaining / increments;

		scriptRequestor.updateDisplay( DISABLED_STATE, "Donating " + amount + " to the shrine..." );
		scriptRequestor.makeRequest( new HeroDonationRequest( scriptRequestor, heroID, eachAmount ), increments - 1 );
		amountRemaining -= eachAmount * (increments - 1);

		if ( scriptRequestor.permitsContinue() )
		{
			scriptRequestor.updateDisplay( DISABLED_STATE, "Request " + increments + " in progress..." );
			scriptRequestor.makeRequest( new HeroDonationRequest( scriptRequestor, heroID, amountRemaining ), 1 );

			if ( scriptRequestor.permitsContinue() )
				scriptRequestor.updateDisplay( ENABLED_STATE, "Requests complete!" );
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
			scriptRequestor.updateDisplay( ERROR_STATE, "Print what?" );
			scriptRequestor.cancelRequest();
			return;
		}

		String [] parameterList = parameters.split( " " );
		PrintStream desiredOutputStream;

		if ( parameterList.length != 1 )
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

				scriptRequestor.updateDisplay( ERROR_STATE, "I/O error in opening file \"" + parameterList[1] + "\"" );
				scriptRequestor.cancelRequest();
				return;
			}
		}
		else
			desiredOutputStream = outputStream;

		executePrintCommand( parameterList[0].toLowerCase(), desiredOutputStream );

		if ( parameterList.length != 1 )
			scriptRequestor.updateDisplay( ENABLED_STATE, "Data has been printed to \"" + parameterList[1] + "\"" );
	}

	/**
	 * A special module used specifically for properly printing out
	 * data relevant to the current session.  This method is more
	 * specialized than its counterpart and is used when the data
	 * to be printed is known, as well as the stream to print to.
	 * Usually called by its counterpart to handle specific instances.
	 */

	private void executePrintCommand( String desiredData, PrintStream outputStream )
	{
		PrintStream originalStream = this.outputStream;
		this.outputStream = outputStream;

		updateDisplay( NOCHANGE, (new Date()).toString() );
		updateDisplay( NOCHANGE, MoonPhaseDatabase.getMoonEffect() );
		updateDisplay( NOCHANGE, "" );

		KoLCharacter data = scriptRequestor.getCharacterData();

		if ( desiredData.equals( "session" ) )
		{
			updateDisplay( NOCHANGE, "Player: " + scriptRequestor.getLoginName() );
			updateDisplay( NOCHANGE, "Session ID: " + scriptRequestor.getSessionID() );
			updateDisplay( NOCHANGE, "Password Hash: " + scriptRequestor.getPasswordHash() );
		}
		else if ( desiredData.startsWith( "stat" ) )
		{
			updateDisplay( NOCHANGE, "Lv: " + data.getLevel() );
			updateDisplay( NOCHANGE, "HP: " + data.getCurrentHP() + " / " + df.format( data.getMaximumHP() ) );
			updateDisplay( NOCHANGE, "MP: " + data.getCurrentMP() + " / " + df.format( data.getMaximumMP() ) );
			updateDisplay( NOCHANGE, "" );
			updateDisplay( NOCHANGE, "Mus: " + getStatString( data.getBaseMuscle(), data.getAdjustedMuscle(), data.getMuscleTNP() ) );
			updateDisplay( NOCHANGE, "Mys: " + getStatString( data.getBaseMysticality(), data.getAdjustedMysticality(), data.getMysticalityTNP() ) );
			updateDisplay( NOCHANGE, "Mox: " + getStatString( data.getBaseMoxie(), data.getAdjustedMoxie(), data.getMoxieTNP() ) );
			updateDisplay( NOCHANGE, "" );
			updateDisplay( NOCHANGE, "Meat: " + df.format( data.getAvailableMeat() ) );
			updateDisplay( NOCHANGE, "Drunk: " + data.getInebriety() );
			updateDisplay( NOCHANGE, "Adv: " + data.getAdventuresLeft() );

			updateDisplay( NOCHANGE, "Fam: " + data.getFamiliarList().getSelectedItem() );
			updateDisplay( NOCHANGE, "Item: " + data.getFamiliarItem() );
		}
		else if ( desiredData.startsWith( "equip" ) )
		{
			updateDisplay( NOCHANGE, "    Hat: " + data.getEquipment( KoLCharacter.HAT ) );
			updateDisplay( NOCHANGE, " Weapon: " + data.getEquipment( KoLCharacter.WEAPON ) );
			updateDisplay( NOCHANGE, "  Shirt: " + data.getEquipment( KoLCharacter.SHIRT ) );
			updateDisplay( NOCHANGE, "  Pants: " + data.getEquipment( KoLCharacter.PANTS ) );
			updateDisplay( NOCHANGE, " Acc. 1: " + data.getEquipment( KoLCharacter.ACCESSORY1 ) );
			updateDisplay( NOCHANGE, " Acc. 2: " + data.getEquipment( KoLCharacter.ACCESSORY2 ) );
			updateDisplay( NOCHANGE, " Acc. 3: " + data.getEquipment( KoLCharacter.ACCESSORY3 ) );
		}
		else if ( desiredData.startsWith( "inv" ) )
		{
			printList( scriptRequestor.getInventory() );
		}
		else if ( desiredData.equals( "closet" ) )
		{
			printList( scriptRequestor.getCloset() );
		}
		else if ( desiredData.equals( "summary" ) )
		{
			printList( scriptRequestor.tally );
		}
		else if ( desiredData.equals( "outfits" ) )
		{
			printList( data.getOutfits() );
		}
		else if ( desiredData.equals( "familiars" ) )
		{
			printList( data.getFamiliarList() );
		}
		else if ( desiredData.equals( "effects" ) )
		{
			printList( data.getEffects() );
		}
		else
		{
			updateDisplay( ERROR_STATE, "Unknown data type: " + desiredData );
			if ( scriptRequestor != this )
				scriptRequestor.cancelRequest();
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
	 * Utility method which determines the first item which matches
	 * the given parameter string.  Note that the string may also
	 * specify an item quantity before the string.
	 */

	private AdventureResult getFirstMatchingItem( String parameters, int matchType )
	{
		int itemID = -1;
		int itemCount = 0;

		// First, allow for the person to type without specifying
		// the amount, if the amount is 1.

		List matchingNames = TradeableItemDatabase.getMatchingNames( parameters );

		if ( matchingNames.size() != 0 )
		{
			itemID = TradeableItemDatabase.getItemID( (String) matchingNames.get(0) );
			itemCount = 1;
		}
		else
		{
			String itemCountString = parameters.split( " " )[0];
			String itemNameString = parameters.substring( itemCountString.length() ).trim();

			matchingNames = TradeableItemDatabase.getMatchingNames( itemNameString );

			if ( matchingNames.size() == 0 )
			{
				scriptRequestor.updateDisplay( ERROR_STATE, "[" + itemNameString + "] does not match anything in the item database." );
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

				scriptRequestor.updateDisplay( ERROR_STATE, itemCountString + " is not a number." );
				scriptRequestor.cancelRequest();
				return null;
			}
		}

		if ( itemID == -1 )
		{
			scriptRequestor.updateDisplay( ERROR_STATE, "[" + parameters + "] does not match anything in the item database." );
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
			matchCount = firstMatch.getCount( scriptRequestor.getCloset() );
		else if ( matchType == INVENTORY )
			matchCount = firstMatch.getCount( scriptRequestor.getInventory() );
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
			scriptRequestor.updateDisplay( ERROR_STATE, "Insufficient " + TradeableItemDatabase.getItemName( itemID ) + " to continue." );
			scriptRequestor.cancelRequest();
			return null;
		}

		return itemCount == 0 ? null : firstMatch.getInstance( itemCount );
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
		String item = previousCommand.substring( previousCommand.split( " " )[0].length() );
		AdventureResult firstMatch = getFirstMatchingItem( item, INVENTORY );
		if ( firstMatch == null )
			return;

		scriptRequestor.makeRequest( new UntinkerRequest( scriptRequestor, firstMatch.getItemID() ), 1 );
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

	private void executeBuyCommand( String parameters )
	{
		AdventureResult firstMatch = getFirstMatchingItem( parameters, NOWHERE );
		ArrayList results = new ArrayList();
		(new SearchMallRequest( scriptRequestor, '\"' + firstMatch.getName() + '\"', 0, results )).run();

		Object [] purchases = results.toArray();

		MallPurchaseRequest currentRequest;
		scriptRequestor.resetContinueState();

		int maxPurchases = firstMatch.getCount();

		for ( int i = 0; i < purchases.length && maxPurchases > 0 && scriptRequestor.permitsContinue(); ++i )
		{
			if ( purchases[i] instanceof MallPurchaseRequest )
			{
				currentRequest = (MallPurchaseRequest) purchases[i];

				// Keep track of how many of the item you had before
				// you run the purchase request

				AdventureResult oldResult = new AdventureResult( currentRequest.getItemName(), 0 );
				int oldResultIndex = scriptRequestor.getInventory().indexOf( oldResult );

				if ( oldResultIndex != -1 )
					oldResult = (AdventureResult) scriptRequestor.getInventory().get( oldResultIndex );

				currentRequest.setLimit( maxPurchases );
				currentRequest.run();

				// Calculate how many of the item you have now after
				// you run the purchase request

				int newResultIndex = scriptRequestor.getInventory().indexOf( oldResult );
				if ( newResultIndex != -1 )
				{
					AdventureResult newResult = (AdventureResult) scriptRequestor.getInventory().get( newResultIndex );
					maxPurchases -= newResult.getCount() - oldResult.getCount();
				}

				// Remove the purchase from the list!  Because you
				// have already made a purchase from the store

				if ( scriptRequestor.permitsContinue() )
					results.remove( purchases[i] );
			}
		}

		scriptRequestor.updateDisplay( ENABLED_STATE, "Purchases complete." );
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

		scriptRequestor.makeRequest( ItemCreationRequest.getInstance( scriptRequestor, firstMatch ), 1 );
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
				scriptRequestor.updateDisplay( ERROR_STATE, parameters + " does not exist in the adventure database." );
				scriptRequestor.cancelRequest();
				return;
			}

			try
			{
				adventureCount = adventureCountString.equals( "*" ) ? 0 : df.parse( adventureCountString ).intValue();

				if ( adventureCount <= 0 && adventure.getZone().equals( "Shore" ) )
					adventureCount += (int) Math.floor( scriptRequestor.getCharacterData().getAdventuresLeft() / 3 );
				else if ( adventureCount <= 0 )
					adventureCount += scriptRequestor.getCharacterData().getAdventuresLeft();
			}
			catch ( Exception e )
			{
				// Technically, this exception should not be thrown, but if
				// it is, then print an error message and return.

				scriptRequestor.updateDisplay( ERROR_STATE, adventureCountString + " is not a number." );
				scriptRequestor.cancelRequest();
				return;
			}
		}

		scriptRequestor.updateDisplay( DISABLED_STATE, "Beginning " + adventureCount + " turnips to " + adventure.toString() + "..." );
		scriptRequestor.makeRequest( adventure, adventureCount );
	}

	/**
	 * Special module used specifically for properly instantiating
	 * requests to change the user's outfit.
	 */

	private void executeChangeOutfitCommand( String parameters )
	{
		String lowercaseOutfitName = parameters.toLowerCase().trim();
		Iterator outfitIterator = scriptRequestor.characterData.getOutfits().iterator();
		SpecialOutfit intendedOutfit = null;
		SpecialOutfit currentOutfit;

		while ( intendedOutfit == null && outfitIterator.hasNext() )
		{
			currentOutfit = (SpecialOutfit) outfitIterator.next();
			if ( currentOutfit.toString().toLowerCase().indexOf( lowercaseOutfitName ) != -1 )
				intendedOutfit = currentOutfit;
		}

		if ( intendedOutfit == null )
		{
			scriptRequestor.updateDisplay( ERROR_STATE, "You can't wear that outfit." );
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
		LockableListModel buffCostTable = new LockableListModel();

		scriptRequestor.initializeBuffBot();
		BuffBotHome buffbotLog = scriptRequestor.getBuffBotLog();
		BuffBotManager currentManager = new BuffBotManager( scriptRequestor, buffCostTable );
		scriptRequestor.setBuffBotManager( currentManager );

		if ( buffCostTable.isEmpty() )
		{
			scriptRequestor.updateDisplay( ERROR_STATE, "No sellable buffs defined." );
			scriptRequestor.cancelRequest();
			return;
		}

		try
		{
			int buffBotIterations = df.parse( parameters ).intValue();

			(new CharsheetRequest( scriptRequestor )).run();
			scriptRequestor.resetContinueState();
			scriptRequestor.setBuffBotActive( true );
			currentManager.runBuffBot( buffBotIterations );
			scriptRequestor.updateDisplay( ENABLED_STATE, "BuffBot execution complete." );
			scriptRequestor.cancelRequest();

		}
		catch (Exception e)
		{
			// Technically, this exception should not be thrown, but if
			// it is, then print an error message and return.

			scriptRequestor.updateDisplay( ERROR_STATE, parameters + " is not a number." );
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
		scriptRequestor.getLogStream().println( message );

		if ( scriptRequestor instanceof KoLmafiaGUI )
			scriptRequestor.updateDisplay( state, message );

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

		Iterator effectIterator = scriptRequestor.getCharacterData().getEffects().iterator();

		while ( effectIterator.hasNext() )
		{
			currentEffect = (AdventureResult) effectIterator.next();
			if ( currentEffect.getName().toLowerCase().indexOf( effectToUneffect ) != -1 )
				(new UneffectRequest( scriptRequestor, currentEffect )).run();
		}
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
		String item = previousCommand.split( " " )[1];

		for ( int i = 0; i < trapperItemNames.length; ++i )
			if ( trapperItemNames[i].indexOf( item ) != -1 )
			{
				(new TrapperRequest( scriptRequestor, trapperItemNumbers[i] )).run();
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
		for ( int i = 0; i < hunterItems.size(); ++i )
			if ( ((String)hunterItems.get(i)).indexOf( item ) != -1 )
				(new BountyHunterRequest( scriptRequestor, TradeableItemDatabase.getItemID( (String) scriptRequestor.hunterItems.get(i) ) )).run();
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

	private void printList( List printing )
	{
		Iterator printingIterator = printing.iterator();
		while ( printingIterator.hasNext() )
			updateDisplay( NOCHANGE, printingIterator.next().toString() );
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

		// Another item-related command is the autosell
		// request.  This one's simple, but it still
		// gets its own utility method.

		if ( request instanceof AutoSellRequest )
		{
			AutoSellRequest asr = (AutoSellRequest) request;

			if ( asr.getSellType() == AutoSellRequest.AUTOSELL )
				commandString.append( "autosell * \"" );
			else
				commandString.append( "mallsell \"" );

			commandString.append( ((AutoSellRequest)request).getName() );
			commandString.append( "\"" );

			if ( asr.getSellType() == AutoSellRequest.AUTOMALL )
			{
				commandString.append( ' ' );
				commandString.append( df.format( asr.getPrice() ) );
				commandString.append( ' ' );
				commandString.append( df.format( asr.getLimit() ) );
			}
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
						commandString.append( System.getProperty( "line.separator" ) );

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
						commandString.append( System.getProperty( "line.separator" ) );

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

			if ( !request.toString().startsWith( "Gym" ) )
				commandString.append( "adventure " );
			else
				commandString.append( "gym " );

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
			commandString.append( System.getProperty( "line.separator" ) );

		return commandString.toString();
	}
}

