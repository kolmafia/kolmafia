/**
 * Copyright (c) 2005-2006, KoLmafia development team
 * http://sourceforge.net/
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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.PrintStream;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.StringTokenizer;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.SwingUtilities;
import jline.ConsoleReader;

public class KoLmafiaCLI extends KoLmafia
{
	private static final Pattern HTMLTAG_PATTERN = Pattern.compile( "<.*?>", Pattern.DOTALL );
	private static final Pattern ASHNAME_PATTERN = Pattern.compile( "\\.ash", Pattern.CASE_INSENSITIVE );
	private static final Pattern STATDAY_PATTERN = Pattern.compile( "(today|tomorrow) is (.*?) day" );
	private static final Pattern MEAT_PATTERN = Pattern.compile( "[\\d,]+ meat" );
	private static final Pattern SCRIPT_PATTERN = Pattern.compile( "<script.*?</script>", Pattern.DOTALL );
	private static final Pattern COMMENT_PATTERN = Pattern.compile( "<!--.*?-->", Pattern.DOTALL );

	public static final int NOWHERE = 1;
	public static final int CREATION = 2;

	public static final int USE = 3;
	public static final int FOOD = 4;
	public static final int BOOZE = 5;

	private static boolean isCreationMatch = false;
	private String previousLine;
	private BufferedReader commandStream;

	private KoLmafiaCLI lastScript;
	private static String previousUpdateString = "";
	private static boolean printToSession = false;

	private static boolean isPrompting = false;
	private static boolean isExecutingCheckOnlyCommand = false;
	private static KoLmafiaASH advancedHandler = new KoLmafiaASH();
	private static ConsoleReader CONSOLE = null;

	public static void main( String [] args )
	{
		System.out.println();
		System.out.println();
		System.out.println(  VERSION_NAME );
		System.out.println( "Running on " + System.getProperty( "os.name" ) );
		System.out.println( "Using Java " + System.getProperty( "java.version" ) );
		System.out.println();

		StaticEntity.setClient( DEFAULT_SHELL );
		outputStream = System.out;

		try
		{
			if ( !System.getProperty( "os.name" ).startsWith( "Win" ) )
				CONSOLE = new ConsoleReader();
		}
		catch ( Exception e )
		{
			StaticEntity.printStackTrace( e );
		}
	}

	/**
	 * Constructs a new <code>KoLmafiaCLI</code> object.  All data fields
	 * are initialized to their default values, the global settings
	 * are loaded from disk.
	 */

	public KoLmafiaCLI( InputStream inputStream )
	{
		try
		{
			if ( inputStream != System.in || System.getProperty( "os.name" ).startsWith( "Win" ) )
				commandStream = KoLDatabase.getReader( inputStream );
		}
		catch ( Exception e )
		{
			// This should not happen.  Therefore, print
			// a stack trace for debug purposes.

			StaticEntity.printStackTrace( e, "Error opening input stream." );
		}
	}

	public void getBreakfast( boolean checkSettings )
	{
		if ( this == StaticEntity.getClient() )
			super.getBreakfast( checkSettings );
		else
			StaticEntity.getClient().getBreakfast( checkSettings );
	}

	/**
	 * Utility method used to prompt the user for their login and
	 * password.  Later on, when profiles are added, prompting
	 * for the user will automatically look up a password.
	 */

	public void attemptLogin()
	{
		try
		{
			String username = StaticEntity.getProperty( "autoLogin" );

			if ( username == null || username.length() == 0 )
			{
				outputStream.println();
				outputStream.print( "username: " );
				username = CONSOLE == null ? commandStream.readLine() : CONSOLE.readLine();
			}

			if ( username == null || username.length() == 0 )
			{
				outputStream.println( "Invalid login." );
				return;
			}

			if ( username.startsWith( "login " ) )
				username = username.substring( 6 );

			String password = getSaveState( username );

			if ( password == null )
			{
				outputStream.print( "password: " );
				password = CONSOLE == null ? commandStream.readLine() : CONSOLE.readLine( new Character( '*' ) );
			}

			if ( password == null || password.length() == 0 )
			{
				outputStream.println( "Invalid password." );
				return;
			}

			outputStream.println();
			RequestThread.postRequest( new LoginRequest( username, password ) );
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
	 * the login has been confirmed to notify thethat the
	 * login was successful, the user-specific settings should be
	 * loaded, and the user can begin adventuring.
	 */

	public void initialize( String username )
	{
		if ( StaticEntity.getClient() != this )
		{
			StaticEntity.getClient().initialize( username );
			return;
		}

		super.initialize( username );

		printBlankLine();
		executeCommand( "moons", "" );
		printBlankLine();

		if ( StaticEntity.getGlobalProperty( "initialFrames" ).indexOf( "LocalRelayServer" ) != -1 )
			KoLmafiaGUI.constructFrame( "LocalRelayServer" );
	}

	/**
	 * A utility method which waits for commands from the user, then
	 * executing each command as it arrives.
	 */

	public void listenForCommands()
	{
		forceContinue();

		if ( StaticEntity.getClient() == this )
		{
			isPrompting = true;
			outputStream.print( " > " );
		}

		String line = null;

		while ( (permitsContinue() || StaticEntity.getClient() == this) && (line = getNextLine()) != null )
		{
			isPrompting = false;

			if ( StaticEntity.getClient() == this )
			{
				previousUpdateString = " > " + line;
				printBlankLine();
			}

			forceContinue();
			executeLine( line );

			if ( StaticEntity.getClient() == this )
			{
				printBlankLine();
				isPrompting = true;
				outputStream.print( " > " );
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

				line = DEFAULT_SHELL == this && CONSOLE != null ? CONSOLE.readLine() :
					commandStream.readLine();
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

	public void executeLine( String line )
	{
		if ( refusesContinue() || line.trim().length() == 0 )
			return;

		// If it gets this far, that means the continue
		// state can be reset.

		if ( line.indexOf( ";" ) != -1 && !line.startsWith( "set" ) )
		{
			String [] separateLines = line.split( ";" );
			for ( int i = 0; i < separateLines.length && permitsContinue(); ++i )
				executeLine( separateLines[i] );

			previousLine = line;
			return;
		}

		// Trim the line, replace all float spaces with
		// single spaces and compare the result against
		// commands which are no longer allowed.

		line = line.replaceAll( "\\s+", " " ).trim();

		// Win game sanity check.  This will find its
		// way back into the GUI ... one day.

		if ( line.equalsIgnoreCase( "win game" ) )
		{
			String [] messages = WIN_GAME_TEXT[ RNG.nextInt( WIN_GAME_TEXT.length ) ];

			updateDisplay( "Executing top-secret 'win game' script..." );
			KoLRequest.delay( 3000 );

			for ( int i = 0; i < messages.length - 1; ++i )
			{
				updateDisplay( messages[i] );
				KoLRequest.delay( 3000 );
			}

			updateDisplay( ERROR_STATE, messages[ messages.length - 1 ] );
			return;
		}

		if ( line.length() == 0 )
			return;

		String command = line.trim().split( " " )[0].toLowerCase().trim();
		String parameters = line.substring( command.length() ).trim();

		if ( !command.equals( "repeat" ) )
			previousLine = line;

		if ( command.endsWith( "?" ) )
		{
			isExecutingCheckOnlyCommand = true;
			command = command.substring( 0, command.length() - 1 );
		}

		RequestThread.openRequestSequence();
		executeCommand( command, parameters );
		RequestThread.closeRequestSequence();

		isExecutingCheckOnlyCommand = false;
	}

	/**
	 * A utility command which decides, based on the command
	 * to be executed, what to be done with it.  It can either
	 * delegate this to other functions, or do it itself.
	 */

	public void executeCommand( String command, String parameters )
	{
		// If the command has already been disabled, then return
		// from this function.

		if ( StaticEntity.isDisabled( command ) )
		{
			printLine( "Called disabled command: " + command + " " + parameters );
			return;
		}

		if ( command.equals( "enable" ) )
		{
			StaticEntity.enable( parameters.toLowerCase() );
			return;
		}

		if ( command.equals( "disable" ) )
		{
			StaticEntity.disable( parameters.toLowerCase() );
			return;
		}

		// Insert random video game reference command to
		// start things off.

		if ( command.equals( "priphea" ) )
		{
			if ( !KoLDesktop.instanceExists() )
			{
				KoLmafiaGUI.checkFrameSettings();

				KoLDesktop.getInstance().initializeTabs();
				KoLDesktop.getInstance().pack();
				KoLDesktop.getInstance().setVisible( true );
			}

			return;
		}

		// Allow access to individual frames so you can
		// do things in the GUI.

		if ( parameters.equals( "" ) )
		{
			if ( command.startsWith( "chat" ) )
			{
				KoLmafiaGUI.constructFrame( "KoLMessenger" );
				return;
			}

			if ( command.startsWith( "mail" ) )
			{
				KoLmafiaGUI.constructFrame( "MailboxFrame" );
				return;
			}

			if ( command.startsWith( "event" ) )
			{
				KoLmafiaGUI.constructFrame( "EventsFrame" );
				return;
			}

			if ( command.startsWith( "compose" ) )
			{
				KoLmafiaGUI.constructFrame( "GreenMessageFrame" );
				return;
			}

			if ( command.startsWith( "gift" ) )
			{
				KoLmafiaGUI.constructFrame( "GiftMessageFrame" );
				return;
			}

			if ( command.startsWith( "option" ) )
			{
				KoLmafiaGUI.constructFrame( "OptionsFrame" );
				return;
			}

			if ( command.startsWith( "item" ) )
			{
				KoLmafiaGUI.constructFrame( "ItemManageFrame" );
				return;
			}

			if ( command.startsWith( "clan" ) )
			{
				KoLmafiaGUI.constructFrame( "ClanManageFrame" );
				return;
			}

			if ( command.startsWith( "gear" ) )
			{
				KoLmafiaGUI.constructFrame( "GearChangeFrame" );
				return;
			}

			if ( command.startsWith( "pvp" ) )
			{
				KoLmafiaGUI.constructFrame( "FlowerHunterFrame" );
				return;
			}
		}

		// Maybe the person is trying to load a raw URL
		// to test something without creating a brand new
		// KoLRequest object to handle it yet?

		if ( command.startsWith( "http:" ) || command.indexOf( ".php" ) != -1 )
		{
			if ( KoLRequest.shouldIgnore( previousLine ) )
				return;

			KoLRequest request = new KoLRequest( previousLine, true );
			RequestThread.postRequest( request );

			StaticEntity.externalUpdate( request.getURLString(), request.responseText );
			return;
		}

		// Allow a version which lets you see the resulting
		// text without loading a mini/relay browser window.

		if ( command.equals( "text" ) )
		{
			if ( KoLRequest.shouldIgnore( previousLine ) )
				return;

			KoLRequest request = new KoLRequest( previousLine.substring(4).trim(), true );
			RequestThread.postRequest( request );

			StaticEntity.externalUpdate( request.getURLString(), request.responseText );
			showHTML( request.responseText, request.getURLString() );

			return;

		}

		// Maybe the person wants to load up their browser
		// from the KoLmafia CLI?

		if ( command.equals( "relay" ) || command.equals( "serve" ) )
		{
			StaticEntity.getClient().startRelayServer();
			return;
		}

		if ( command.startsWith( "forum" ) )
		{
			StaticEntity.openSystemBrowser( "http://forums.kingdomofloathing.com/" );
			return;
		}

		// First, handle the wait command, for however
		// many seconds the user would like to wait.

		if ( command.equals( "wait" ) || command.equals( "pause" ) )
		{
			int seconds = parameters.equals( "" ) ? 1 : StaticEntity.parseInt( parameters );
			StaticEntity.executeCountdown( "Countdown: ", seconds );

			updateDisplay( "Waiting completed." );
			return;
		}

		// Preconditions kickass, so they're handled right
		// after the wait command.  (Right)

		if ( command.equals( "conditions" ) || command.equals( "objectives" ) )
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
			return;
		}

		if ( command.equals( "clear" ) || command.equals( "cls" ) )
		{
			commandBuffer.clearBuffer();
			return;
		}

		if ( command.equals( "abort" ) )
		{
			updateDisplay( ABORT_STATE, parameters.length() == 0 ? "Script abort." : parameters );
			return;
		}

		if ( command.equals( "continue" ) )
		{
			if ( lastScript != null && lastScript.previousLine != null && lastScript.previousLine.length() != 0 )
			{
				forceContinue();
				lastScript.listenForCommands();
			}
			else
			{
				printLine( "No script to continue." );
			}

			return;
		}

		// Adding the requested echo command.  I guess this is
		// useful for people who want to echo things...

		if ( command.equals( "echo" ) )
		{
			if ( parameters.equalsIgnoreCase( "timestamp" ) )
				parameters = MoonPhaseDatabase.getCalendarDayAsString( new Date() );

			updateDisplay( ANYTAG_PATTERN.matcher( parameters ).replaceAll( "" ) );
			echoStream.println( parameters );

			return;
		}

		// Adding another undocumented property setting command
		// so people can configure variables in scripts.

		if ( command.equals( "set" ) )
		{
			int splitIndex = parameters.indexOf( "=" );
			if ( splitIndex == -1 )
			{
				if ( !parameters.startsWith( "saveState" ) )
					printLine( StaticEntity.getProperty( parameters ) );

				return;
			}

			String name = parameters.substring( 0, splitIndex ).trim();
			if ( name.startsWith( "saveState" ) )
				return;

			String value = parameters.substring( splitIndex + 1 ).trim();

			if ( name.equals( "battleAction" ) )
			{
				if ( value.indexOf( ";" ) != -1 )
				{
					CombatSettings.setDefaultAction( value );
					value = "custom combat script";
				}

				value = CombatSettings.getLongCombatOptionName( value );

				// Special handling of the battle action property,
				// such that auto-recovery gets reset as needed.

				if ( name.equals( "battleAction" ) && value != null )
					KoLCharacter.getBattleSkillNames().setSelectedItem( value );
			}

			printLine( name + " => " + value );
			StaticEntity.setProperty( name, value );

			if ( name.equals( "battleAction" ) && value.equals( "custom combat script" ) )
				printList( CombatSettings.getDefaultAction() );

			if ( name.equals( "buffBotCasting" ) )
				BuffBotManager.reset();

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

		if ( command.equals( "relogin" ) || command.equals( "timein" ) )
		{
			LoginRequest.executeTimeInRequest();
			return;
		}

		if ( command.equals( "login" ) )
		{
			RequestThread.postRequest( new LogoutRequest() );
			String password = getSaveState( parameters );

			if ( password != null )
				RequestThread.postRequest( new LoginRequest( parameters, password ) );

			else
				updateDisplay( ERROR_STATE, "No password saved for that username." );

			return;
		}

		// Next, handle any requests that request exiting.
		// Note that a logout request should be sent in
		// order to be friendlier to the server, if the
		// character has already logged in.

		if ( command.equals( "logout" ) )
		{
			if ( KoLDesktop.instanceExists() )
				KoLDesktop.getInstance().setVisible( false );

			KoLFrame [] frames = StaticEntity.getExistingFrames();
			for ( int i = 0; i < frames.length; ++i )
				frames[i].setVisible( false );

			if ( StaticEntity.getClient() != DEFAULT_SHELL )
				KoLFrame.createDisplay( LoginFrame.class );

			RequestThread.postRequest( new LogoutRequest() );

			if ( StaticEntity.getClient() == DEFAULT_SHELL )
				DEFAULT_SHELL.attemptLogin();

			return;
		}

		// Now for formal exit commands.

		if ( command.equals( "exit" ) || command.equals( "quit" ) )
			System.exit(0);

		// Next, handle any requests for script execution;
		// these can be done at any time (including before
		// login), so they should be handled before a test
		// of login state needed for other commands.

		if ( command.equals( "using" ) || command.equals( "namespace" ) )
		{
			// Validate the script first.

			executeCommand( "validate", parameters );
			if ( !permitsContinue() )
				return;

			String namespace = StaticEntity.getProperty( "commandLineNamespace" );
			if ( namespace.startsWith( parameters + "," ) || namespace.endsWith( "," + parameters ) || namespace.indexOf( "," + parameters + "," ) != -1 )
				return;

			if ( namespace.toString().equals( "" ) )
				namespace = parameters;
			else
				namespace = namespace + "," + parameters;

			StaticEntity.setProperty( "commandLineNamespace", namespace.toString() );
			return;
		}

		if ( command.equals( "verify" ) || command.equals( "validate" ) || command.equals( "using" ) || command.equals( "namespace" ) || command.equals( "call" ) || command.equals( "run" ) || command.startsWith( "exec" ) || command.equals( "load" ) || command.equals( "start" ) )
		{
			executeScriptCommand( command, parameters );
			return;
		}

		// Next, handle repeat commands - these are a
		// carry-over feature which made sense in CLI.

		if ( command.equals( "repeat" ) )
		{
			if ( previousLine != null )
			{
				int repeatCount = parameters.length() == 0 ? 1 : StaticEntity.parseInt( parameters );
				for ( int i = 0; i < repeatCount && permitsContinue(); ++i )
				{
					printLine( "Repetition of [" + previousLine + "] (" + (i+1) + " of " + repeatCount + ")..." );
					executeLine( previousLine );
				}
			}

			return;
		}

		// Next, print out the moon phase, if the user
		// wishes to know what moon phase it is.

		if ( command.startsWith( "moon" ) )
		{
			updateDisplay( "Ronald: " + MoonPhaseDatabase.getRonaldPhaseAsString() );
			updateDisplay( "Grimace: " + MoonPhaseDatabase.getGrimacePhaseAsString() );
			printBlankLine();

			Date today = new Date();

			try
			{
				today = DATED_FILENAME_FORMAT.parse( DATED_FILENAME_FORMAT.format( today ) );
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
			updateDisplay( MoonPhaseDatabase.getMoonEffect() );
			printBlankLine();

			return;
		}

		// Next, handle requests to start or stop
		// debug mode.

		if ( command.equals( "debug" ) )
		{
			if ( parameters.equals( "off" ) )
				closeDebugStream();
			else
				openDebugStream();

			return;
		}

		// Next, handle requests to start or stop
		// the mirror stream.

		if ( command.indexOf( "mirror" ) != -1 )
		{
			if ( command.indexOf( "end" ) != -1 || command.indexOf( "stop" ) != -1 || command.indexOf( "close" ) != -1 ||
				parameters.length() == 0 || parameters.equals( "end" ) || parameters.equals( "stop" ) || parameters.equals( "close" ) )
			{
				mirrorStream.close();
				mirrorStream = NullStream.INSTANCE;

				echoStream.close();
				echoStream = NullStream.INSTANCE;

				updateDisplay( "Mirror stream closed." );
			}
			else
			{
				if ( !parameters.endsWith( ".txt" ) )
					parameters += ".txt";

				mirrorStream = openStream( parameters, mirrorStream, true );
				echoStream = openStream( parameters + ".echo", echoStream, true );
			}

			return;
		}

		// Then, the description lookup command so that
		// people can see what the description is for a
		// particular item.

		if ( command.equals( "lookup" ) )
		{
			AdventureResult result = getFirstMatchingItem( parameters );
			if ( result == null )
				return;

			KoLRequest request = new KoLRequest(
				"desc_item.php?whichitem=" + TradeableItemDatabase.getDescriptionId( result.getItemId() ) );

			RequestThread.postRequest( request );

			if ( StaticEntity.getClient() instanceof KoLmafiaGUI )
				SwingUtilities.invokeLater( new CreateFrameRunnable( RequestFrame.class, new Object[] { request } ) );
			else
				showHTML( request.responseText, "Item Description" );

			return;
		}

		if ( command.equals( "wiki" ) )
		{
			TradeableItemDatabase.determineWikiData( parameters );
			return;
		}

		if ( command.equals( "survival" ) || command.equals( "getdata" ) )
		{
			showHTML( AdventureDatabase.getAreaCombatData( AdventureDatabase.getAdventure( parameters ).toString() ).toString(), "Survival Lookup" );
			return;
		}

		// Re-adding the breakfast command, just
		// so people can add it in scripting.

		if ( command.equals( "breakfast" ) )
		{
			getBreakfast( false );
			return;
		}

		if ( parameters.equals( "refresh" ) )
		{
			parameters = command;
			command = "refresh";
		}

		if ( command.equals( "refresh" ) )
		{
			if ( parameters.equals( "all" ) )
				StaticEntity.getClient().refreshSession();
			else if ( parameters.equals( "status" ) || parameters.equals( "effects" ) )
				RequestThread.postRequest( CharpaneRequest.getInstance() );
			else if ( parameters.equals( "gear" ) || parameters.startsWith( "equip" ) || parameters.equals( "outfit" ) )
				RequestThread.postRequest( new EquipmentRequest( EquipmentRequest.EQUIPMENT ) );
			else if ( parameters.startsWith( "inv" ) )
				RequestThread.postRequest( new EquipmentRequest( EquipmentRequest.CLOSET ) );
			else if ( parameters.equals( "storage" ) )
				RequestThread.postRequest( new ItemStorageRequest() );
			else if ( parameters.equals( "familiar" ) || parameters.equals( "terrarium" ) )
				RequestThread.postRequest( new FamiliarRequest() );

			executePrintCommand( parameters );
			return;
		}

		// Look!  It's the command to complete the
		// Sorceress entryway.

		if ( command.equals( "entryway" ) )
		{
			SorceressLair.completeCloveredEntryway();
			return;
		}

		// Look!  It's the command to complete the
		// Sorceress hedge maze!  This is placed
		// right after for consistency.

		if ( command.equals( "maze" ) || command.equals( "hedgemaze" ) )
		{
			SorceressLair.completeHedgeMaze();
			return;
		}

		// Look!  It's the command to fight the guardians
		// in the Sorceress's tower!  This is placed
		// right after for consistency.

		if ( command.equals( "tower" ) || command.equals( "guardians" ) || command.equals( "chamber" ) )
		{
			SorceressLair.fightAllTowerGuardians();
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
			KoLRequest request = new KoLRequest( "council.php", true );
			RequestThread.postRequest( request );

			showHTML( StaticEntity.singleStringReplace( request.responseText,
				"<a href=\"town.php\">Back to Seaside Town</a>", "" ), "Available Quests" );

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
			if ( parameters.length() > 0 )
			{
				executeCastBuffRequest( parameters );
				return;
			}
		}

		// Uneffect with martians are related to buffs,
		// so listing them next seems logical.

		if ( command.equals( "uneffect" ) || command.equals( "remedy" ) )
		{
			executeUneffectRequest( parameters );
			return;
		}

		// Add in item retrieval the way KoLmafia handles
		// it internally.

		if ( command.equals( "get" ) || command.equals( "find" ) || command.equals( "acquire" ) || command.equals( "retrieve" ) )
		{
			// Handle lucky and unlucky retrieval of
			// worthless items via the sewer.

			if ( parameters.indexOf( "worthless item" ) != -1 )
			{
				int itemCount = 1;

				if ( !parameters.equals( "worthless item" ) )
					itemCount = StaticEntity.parseInt( parameters.substring( 0, parameters.indexOf( " " ) ) );

				ArrayList temporary = new ArrayList();
				temporary.addAll( conditions );
				conditions.clear();

				if ( parameters.indexOf( "with clover" ) != -1 )
				{
					if ( KoLCharacter.getAdventuresLeft() > 0 )
					{
						AdventureDatabase.retrieveItem( SewerRequest.CLOVER.getInstance( itemCount ) );
						executeLine( "adventure " + itemCount + " sewer with clovers" );
					}
				}
				else
				{
					while ( KoLCharacter.getAdventuresLeft() > 0 && HermitRequest.getWorthlessItemCount() < itemCount && permitsContinue() )
						executeLine( "adventure " + (itemCount - HermitRequest.getWorthlessItemCount()) + " unlucky sewer" );
				}

				conditions.addAll( temporary );
				if ( HermitRequest.getWorthlessItemCount() < itemCount )
				{
					updateDisplay( ERROR_STATE, "Unable to acquire " + itemCount + " worthless items." );
					return;
				}
			}

			// Non-worthless-item requests default to
			// internal retrieveItem calls.

			else
			{
				AdventureResult item = getFirstMatchingItem( parameters );
				if ( item != null )
					AdventureDatabase.retrieveItem( item, true );
			}

			return;
 		}

		// Adding clan management command options inline
		// in the parsing.

		if ( command.equals( "clan" ) )
		{
			if ( parameters.equals( "snapshot" ) )
				ClanManager.takeSnapshot( 20, 10, 5, 0, false, true );
			else if ( parameters.equals( "stashlog" ) )
				ClanManager.saveStashLog();

			return;
		}

		// One command available after login is a request
		// to print the current state of the client.  This
		// should be handled in a separate method, since
		// there are many things themay want to print

		if ( command.equals( "print" ) || command.equals( "list" ) || command.equals( "show" ) )
		{
			executePrintCommand( parameters );
			return;
		}

		// One command is an item usage request.  These
		// requests are complicated, so delegate to the
		// appropriate utility method.

		if ( command.equals( "eat" ) || command.equals( "drink" ) || command.equals( "use" ) || command.equals( "hobodrink" ) )
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

		// Smashing is a type of item usage

		if ( command.equals( "smash" ) || command.equals( "pulverize" ) )
		{
			makePulverizeRequest( parameters );
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
			executeUntinkerRequest( parameters );
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

		if ( command.equals( "hagnk" ) || command.equals( "pull" ) )
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

		if ( command.equals( "mind-control" ) || command.equals( "mcd" ) )
		{
			executeMindControlRequest( parameters );
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

		// To make it easier to run the spice loop in
		// an optimal fashion, make it a command.

		if ( command.equals( "spiceloop" ) )
		{
			executeSpiceLoop( parameters );
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

		if ( command.equals( "equip" ) || command.equals( "wear" ) || command.equals( "wield" ) )
		{
			executeEquipCommand( parameters );
			return;
		}


		// You can remove a specific piece of equipment.

		if ( command.equals( "unequip" ) || command.equals( "remove" ) )
		{
			executeUnequipCommand( parameters );
			return;
		}

		// Another popular command involves changing
		// your current familiar.

		if ( command.equals( "familiar" ) )
		{
			if ( parameters.equals( "list" ) )
			{
				executePrintCommand( "familiars " + parameters.substring(4).trim() );
				return;
			}
			else if ( parameters.length() == 0 )
			{
				executePrintCommand( "familiars" );
				return;
			}
			else if ( parameters.equalsIgnoreCase( "none" ) || parameters.equalsIgnoreCase( "unequip" ) )
			{
				if ( KoLCharacter.getFamiliar() == null || KoLCharacter.getFamiliar().equals( FamiliarData.NO_FAMILIAR ) )
					return;

				RequestThread.postRequest( new FamiliarRequest( FamiliarData.NO_FAMILIAR ) );
				return;
			}
			else if ( parameters.indexOf( "(no change)" ) != -1 )
				return;

			String lowerCaseName = parameters.toLowerCase();
			List familiars = KoLCharacter.getFamiliarList();

			for ( int i = 0; i < familiars.size(); ++i )
			{
				if ( familiars.get(i).toString().toLowerCase().indexOf( lowerCaseName ) != -1 )
				{
					FamiliarData newFamiliar = (FamiliarData) familiars.get(i);
					if ( KoLCharacter.getFamiliar() != null && KoLCharacter.getFamiliar().equals( newFamiliar ) )
						return;

					RequestThread.postRequest( new FamiliarRequest( newFamiliar ) );
					return;
				}
			}

			updateDisplay( ERROR_STATE, "You don't have that familiar." );
			return;
		}

		// Another popular command involves managing
		// your player's closet!  Which can be fun.

		if ( command.equals( "closet" ) )
		{
			if ( parameters.equals( "list" ) )
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

		if ( command.equals( "checkpoint" ) )
		{
			SpecialOutfit.createExplicitCheckpoint();
			return;
		}

		if ( command.equals( "outfit" ) )
		{
			if ( parameters.equals( "list" ) )
			{
				executePrintCommand( "outfits " + parameters.substring(4).trim() );
				return;
			}
			else if ( parameters.length() == 0 )
			{
				executePrintCommand( "outfits" );
				return;
			}
			else if ( parameters.equalsIgnoreCase( "checkpoint" ) )
			{
				SpecialOutfit.restoreExplicitCheckpoint();
				return;
			}

			executeChangeOutfitCommand( parameters );
			return;
		}

		// Purchases from the mall are really popular,
		// as far as scripts go.  Nobody is sure why,
		// but they're popular, so they're implemented.

		if ( command.equals( "buy" ) || command.equals( "mallbuy" ) )
		{
			executeBuyCommand( parameters );
			if ( !isRunningBetweenBattleChecks() )
				SpecialOutfit.restoreImplicitCheckpoint();

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
			int desiredLimit = 0;

			if ( parameters.indexOf( "with limit" ) != -1 )
			{
				String [] splitup = parameters.split( "with limit" );
				parameters = splitup[0];
				desiredLimit = StaticEntity.parseInt( splitup[1] );
			}

			StoreManager.searchMall( parameters, results, desiredLimit, true );
			printList( results );
			return;
		}

		if ( command.equals( "hermit" ) )
		{
			executeHermitRequest( parameters );
			return;
		}

		if ( command.equals( "trapper" ) )
		{
			executeTrapperRequest( parameters );
			return;
		}

		if ( command.equals( "hunter" ) )
		{
			executeHunterRequest( parameters );
			return;
		}

		if ( command.equals( "galaktik" ) )
		{
			executeGalaktikRequest( parameters );
			return;
		}

		if ( command.equals( "mpitems" ) )
		{
			int restores = getRestoreCount();
			printLine( restores + " mana restores remaining." );
			return;
		}

		if ( command.startsWith( "restore" ) || command.startsWith( "recover" ) || command.startsWith( "check" ) )
		{
			if ( parameters.equals( "" ) )
				StaticEntity.getClient().runBetweenBattleChecks( false );
			else if ( parameters.equalsIgnoreCase( "hp" ) || parameters.equalsIgnoreCase( "health" ) )
			{
				float setting = StaticEntity.getFloatProperty( "hpAutoRecoveryTarget" );
				StaticEntity.getClient().recoverHP( (int) (setting * (float) KoLCharacter.getMaximumHP()) );
			}
			else if ( parameters.equalsIgnoreCase( "mp" ) || parameters.equalsIgnoreCase( "mana" ) )
			{
				float setting = StaticEntity.getFloatProperty( "mpAutoRecoveryTarget" );
				StaticEntity.getClient().recoverMP( (int) (setting * (float) KoLCharacter.getMaximumMP()) );
			}

			SpecialOutfit.restoreImplicitCheckpoint();
			return;
		}

		if ( command.startsWith( "trigger" ) )
		{
			if ( parameters.equals( "clear" ) )
			{
				MoodSettings.removeTriggers( MoodSettings.getTriggers().toArray() );
				MoodSettings.saveSettings();
			}
			else if ( parameters.equals( "autofill" ) )
			{
				MoodSettings.autoFillTriggers();
				MoodSettings.saveSettings();
			}

			String [] split = parameters.split( "\\s*,\\s*" );
			if ( split.length == 3 )
			{
				MoodSettings.addTrigger( split[0], split[1], split[2] );
				MoodSettings.saveSettings();
			}

			printList( MoodSettings.getTriggers() );
			return;
		}

		if ( command.startsWith( "mood" ) )
		{
			if ( parameters.equals( "clear" ) )
			{
				MoodSettings.removeTriggers( MoodSettings.getTriggers().toArray() );
				MoodSettings.saveSettings();
				return;
			}
			else if ( parameters.equals( "autofill" ) )
			{
				MoodSettings.autoFillTriggers();
				MoodSettings.saveSettings();

				printList( MoodSettings.getTriggers() );
				return;
			}
			else if ( !parameters.equals( "" ) && !parameters.startsWith( "exec" ) )
				MoodSettings.setMood( parameters );

			MoodSettings.execute( true );
			SpecialOutfit.restoreImplicitCheckpoint();

			printLine( "Mood swing complete." );
			return;
		}

		if ( command.equals( "restaurant" ) )
		{
			makeRestaurantRequest( parameters );
			return;
		}

		if ( command.indexOf( "brewery" ) != -1 )
		{
			makeMicrobreweryRequest( parameters );
			return;
		}

		// Campground commands, like relaxing at the beanbag, or
		// resting at your house/tent.

		if ( command.equals( "rest" ) || command.equals( "relax" ) )
		{
			executeCampgroundRequest( command + " " + parameters );
			return;
		}

		if ( command.equals( "sofa" ) || command.equals( "sleep" ) )
		{
			RequestThread.postRequest( (new ClanGymRequest( ClanGymRequest.SOFA )).setTurnCount( StaticEntity.parseInt( parameters ) ) );
			return;
		}

		// Because it makes sense to add this command as-is,
		// you now have the ability to request buffs.

		if ( command.equals( "send" ) || command.equals( "kmail" ) )
		{
			if ( isRunningBetweenBattleChecks() )
			{
				printLine( "Send request \"" + parameters + "\" ignored in between-battle execution." );
				return;
			}

			executeSendRequest( parameters );
			return;
		}

		// Finally, handle command abbreviations - in
		// other words, commands that look like they're
		// their own commands, but are actually commands
		// which are derived from other commands.

		if ( command.equals( "cast" ) || command.equals( "skill" ) )
			command = "skills";

		if ( command.startsWith( "inv" ) || command.equals( "closet" ) || command.equals( "session" ) || command.equals( "summary" ) ||
			command.equals( "effects" ) || command.equals( "status" ) || command.equals( "encounters" ) || command.equals( "skills" ) )
		{
			executePrintCommand( command + " " + parameters );
			return;
		}

		// If someone wants to add a new adventure on
		// the fly, and it's a valid URL (ie: not a
		// send or search URL), go right ahead.

		if ( command.equals( "location" ) )
		{
			int spaceIndex = parameters.indexOf( " " );

			KoLAdventure adventure = new KoLAdventure( "Holiday", "0", "0",
				"adventure.php", parameters.substring( 0, spaceIndex ), parameters.substring( spaceIndex ).trim() );

			AdventureDatabase.addAdventure( adventure );
			return;
		}

		// If all else fails, then assume that the
		// person was trying to call a script.

		executeScriptCommand( "call", previousLine );
	}

	public void showHTML( String text, String title )
	{
		// Strip out all the new lines found in the source
		// so you don't accidentally add more new lines than
		// necessary.

		String displayText = text.replaceAll( "[\r\n]+", "" );

		// Replace all things symbolizing paragraph breaks
		// with actual new lines.

		displayText = displayText.replaceAll( "<(br|tr)[^>]*>", "\n" ).replaceAll( "<(p|blockquote)[^>]*>", "\n\n" );

		// Replace HTML character entities with something
		// which is more readily printable.

		displayText = HTMLTAG_PATTERN.matcher( displayText ).replaceAll( "" );
		displayText = displayText.replaceAll( "&nbsp;", " " ).replaceAll(
			"&trade;", " [tm]" ).replaceAll( "&ntilde;", "n" ).replaceAll( "&quot;", "" );

		// Allow only one new line at a time in the HTML
		// that is printed.

		displayText = displayText.replaceAll( "\n\n\n+", "\n\n" );
		displayText = SCRIPT_PATTERN.matcher( displayText ).replaceAll( "" );
		displayText = COMMENT_PATTERN.matcher( displayText ).replaceAll( "" );

		printLine( displayText.trim() );
	}

	private void executeSendRequest( String parameters )
	{
		String [] splitParameters = parameters.replaceFirst( " [tT][oO] ", "\n" ).split( "\n" );

		if ( splitParameters.length != 2 )
		{
			updateDisplay( ERROR_STATE, "Invalid send request." );
			return;
		}

		Object [] attachments = getMatchingItemList( splitParameters[0] );
		if ( attachments.length == 0 )
			return;

		SendMessageRequest.setUpdateDisplayOnFailure( false );
		RequestThread.postRequest( new GreenMessageRequest( splitParameters[1], DEFAULT_KMAIL, attachments ) );
		SendMessageRequest.setUpdateDisplayOnFailure( true );

		if ( !SendMessageRequest.hadSendMessageFailure() )
		{
			updateDisplay( "Message sent to " + splitParameters[1] );
		}
		else
		{
			List availablePackages = GiftMessageRequest.getPackages();
			int desiredPackageIndex = Math.min( Math.min( availablePackages.size() - 1, attachments.length ), 5 );

			if ( MoonPhaseDatabase.getHoliday( new Date() ).startsWith( "Valentine's" ) )
				desiredPackageIndex = 0;

			// Clear the error state for continuation on the
			// message sending attempt.

			if ( !refusesContinue() )
				forceContinue();

			RequestThread.postRequest( new GiftMessageRequest( splitParameters[1], "You were in Ronin, so I'm sending you a package!",
				"For your collection.", desiredPackageIndex, attachments ) );

			if ( permitsContinue() )
				updateDisplay( "Gift sent to " + splitParameters[1] );
			else
				updateDisplay( ERROR_STATE, "Failed to send message to " + splitParameters[1] );
		}
	}

	private File findScriptFile( String filename )
	{
		File scriptFile = new File( "scripts/" + filename );
		if ( !scriptFile.exists() )
			scriptFile = new File( "scripts/" + filename + ".txt" );
		if ( !scriptFile.exists() )
			scriptFile = new File( "scripts/" + filename + ".ash" );
		if ( !scriptFile.exists() )
			scriptFile = new File( filename );
		if ( !scriptFile.exists() )
			scriptFile = new File( filename + ".txt" );
		if ( !scriptFile.exists() )
			scriptFile = new File( filename + ".ash" );
		if ( !scriptFile.exists() )
			return null;

		return scriptFile;
	}

	/**
	 * A special module used to handle the calling of a
	 * script.
	 */

	private void executeScriptCommand( String command, String parameters )
	{
		try
		{
			int runCount = 1;
			String [] arguments = null;

			parameters = parameters.trim();
			File scriptFile = findScriptFile( parameters );

			// If still no script was found, perhaps it's the secret invocation
			// of the "#x script" that allows a script to be run multiple times.

			if ( scriptFile == null )
			{
				boolean hasMultipleRuns = true;
				String runCountString = parameters.split( " " )[0];

				for ( int i = 0; i < runCountString.length() - 1; ++i )
					hasMultipleRuns &= Character.isDigit( runCountString.charAt(i) );

				hasMultipleRuns &= runCountString.endsWith( "x" );

				if ( hasMultipleRuns )
				{
					runCount = StaticEntity.parseInt( runCountString );
					parameters = parameters.substring( parameters.indexOf( " " ) ).trim();
					scriptFile = findScriptFile( parameters );
				}
			}

			// If no script was found, perhaps there's parentheses indicating
			// that this is an ASH script invocation.

			if ( scriptFile == null )
			{
				int paren = parameters.indexOf( "(" );
				if ( paren != -1 )
				{
					arguments = parseScriptArguments( parameters.substring( paren + 1 ) );
					if ( arguments == null )
					{
						updateDisplay( ERROR_STATE, "Failed to parse arguments" );
						return;
					}

					parameters = parameters.substring( 0, paren ).trim();
					scriptFile = findScriptFile( parameters );
				}
			}

			// Maybe the more ambiguous invocation of an ASH script which does
			// not use parentheses?

			if ( scriptFile == null )
			{
				int spaceIndex = parameters.indexOf( " " );
				if ( spaceIndex != -1 && arguments == null )
				{
					arguments = parseScriptArguments( parameters.substring( spaceIndex ).trim() );
					parameters = parameters.substring( 0, spaceIndex );
					scriptFile = findScriptFile( parameters );
				}
			}

			// If not even that, perhaps it's the invocation of a function which
			// is defined in the ASH namespace?

			if ( scriptFile == null )
			{
				advancedHandler.execute( null, parameters, arguments );
				return;
			}

			// In theory, you could execute EVERY script in a directory, but instead,
			// let's make it be an error state.

			if ( scriptFile.isDirectory() )
			{
				updateDisplay( ERROR_STATE, scriptFile.getAbsolutePath() + " is a directory." );
				return;
			}

			// Allow the ".ash" to appear anywhere in the filename
			// in a case-insensitive manner.

			if ( ASHNAME_PATTERN.matcher( scriptFile.getPath() ).find() )
			{
				// If there's an alternate namespace being
				// used, then be sure to switch.

				if ( command.equals( "validate" ) || command.equals( "verify" ) )
				{
					advancedHandler.validate( scriptFile );
                    printLine( "Script verification complete." );
					return;
				}

				// If there's an alternate namespace being
				// used, then be sure to switch.

				for ( int i = 0; i < runCount && permitsContinue(); ++i )
					advancedHandler.execute( scriptFile, "main", arguments );
			}
			else
			{
				if ( arguments != null )
				{
					updateDisplay( ERROR_STATE, "You can only specify arguments for an ASH script" );
					return;
				}

				for ( int i = 0; i < runCount && permitsContinue(); ++i )
				{
					lastScript = new KoLmafiaCLI( new FileInputStream( scriptFile ) );
					lastScript.listenForCommands();
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

	private String [] parseScriptArguments( String parameters )
	{
		int rparen = parameters.lastIndexOf( ")" );

		if ( rparen != -1 )
			parameters = parameters.substring( 0, rparen ).trim();

		if ( parameters.indexOf( "," ) != -1 )
		{
			boolean isSingleParameter = parameters.startsWith( "\"" );
			isSingleParameter &= parameters.indexOf( "\"", 1 ) == parameters.length() - 1;

			if ( !isSingleParameter )
				return parseScriptArguments( parameters, "," );
		}

		if ( rparen != -1 || parameters.indexOf( "\"" ) != -1 )
			return new String [] { parameters };

		return parseScriptArguments( parameters, " " );
	}

	private String [] parseScriptArguments( String parameters, String delimiter )
	{
		ArrayList resultList = new ArrayList();

		int quoteIndex = parameters.indexOf( "\"" );
		int delimiterIndex = parameters.indexOf( delimiter );

		while ( !parameters.equals( "" ) )
		{
			if ( quoteIndex != -1 && quoteIndex < delimiterIndex )
			{
				int endQuoteIndex = parameters.indexOf( "\"", quoteIndex + 1 );
				while ( endQuoteIndex != -1 && endQuoteIndex != 0 && parameters.charAt( endQuoteIndex - 1 ) == '\\' )
					endQuoteIndex = parameters.indexOf( "\"", endQuoteIndex + 1 );

				if ( endQuoteIndex == -1 )
					endQuoteIndex = parameters.length() - 1;

				resultList.add( parameters.substring( 1, endQuoteIndex ) );

				delimiterIndex = parameters.indexOf( delimiter, endQuoteIndex );
				parameters = delimiterIndex == -1 ? "" : parameters.substring( delimiterIndex + 1 ).trim();
			}
			else if ( delimiterIndex != -1 )
			{
				resultList.add( parameters.substring( 0, delimiterIndex ).trim() );
				parameters = parameters.substring( delimiterIndex + 1 ).trim();
			}
			else
			{
				resultList.add( parameters );
				parameters = "";
			}

			quoteIndex = parameters.indexOf( "\"" );
			delimiterIndex = parameters.indexOf( delimiter );
		}

		String [] result = new String[ resultList.size() ];
		resultList.toArray( result );
		return result;

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

	public static boolean testConditional( String parameters )
	{
		if ( !permitsContinue() )
			return false;

		// Allow checking for moon signs for stat days
		// only.  Allow test for today and tomorrow.

		Matcher dayMatcher = STATDAY_PATTERN.matcher( parameters );
		if ( dayMatcher.find() )
		{
			String statDayInformation = MoonPhaseDatabase.getMoonEffect().toLowerCase();
			return statDayInformation.indexOf( dayMatcher.group(2) + " bonus" ) != -1 &&
				statDayInformation.indexOf( "not " + dayMatcher.group(1) ) == -1;
		}

		// Check for the bounty hunter's current desired
		// item list.

		if ( parameters.startsWith( "bounty hunter wants " ) )
		{
			if ( hunterItems.isEmpty() )
				RequestThread.postRequest( new BountyHunterRequest() );

			String item = parameters.substring(20).trim().toLowerCase();
			for ( int i = 0; i < hunterItems.size(); ++i )
				if ( ((String)hunterItems.get(i)).indexOf( item ) != -1 )
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

	private static int lvalue( String left )
	{
		if ( left.equals( "level" ) )
			return KoLCharacter.getLevel();

		if ( left.equals( "health" ) )
			return KoLCharacter.getCurrentHP();

		if ( left.equals( "mana" ) )
			return KoLCharacter.getCurrentMP();

		if ( left.equals( "meat" ) )
			return KoLCharacter.getAvailableMeat();

		if ( left.equals( "adventures" ) )
			return KoLCharacter.getAdventuresLeft();

		if ( left.equals( "inebriety") || left.equals( "drunkenness" ) || left.equals( "drunkness" ) )
			return KoLCharacter.getInebriety();

		AdventureResult item = itemParameter( left );
		AdventureResult effect = effectParameter( left );

		// If there is no question you're looking for one or
		// the other, then return the appropriate match.

		if ( item != null && effect == null )
			return item.getCount( inventory );

		if ( item == null && effect != null )
			return effect.getCount( activeEffects );

		// This breaks away from fuzzy matching so that a
		// substring match is preferred over a fuzzy match.
		// Items first for one reason: Knob Goblin perfume.

		if ( item != null && item.getName().toLowerCase().indexOf( left.toLowerCase() ) != -1 )
			return item.getCount( inventory );

		if ( effect != null && effect.getName().toLowerCase().indexOf( left.toLowerCase() ) != -1 )
			return effect.getCount( activeEffects );

		// Now, allow fuzzy match results to return a value.
		// Again, following the previous precident, items are
		// preferred over effects.

		if ( item != null )
			return item.getCount( inventory );

		if ( effect != null )
			return effect.getCount( activeEffects );

		// No match.  The value is zero by default.

		return 0;
	}

	private static int rvalue( String left, String right )
	{
		if ( right.endsWith( "%" ) )
		{
			right = right.substring( 0, right.length() - 1 );
			int value = StaticEntity.parseInt( right );

			if ( left.equals( "health" ) )
				return(int) ((float) value * (float)KoLCharacter.getMaximumHP() / 100.0f);

			if ( left.equals( "mana" ) )
				return (int) ((float) value * (float)KoLCharacter.getMaximumMP() / 100.0f);

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
					return item.getCount( inventory );

				AdventureResult effect = effectParameter( right );

				if ( effect != null )
					return effect.getCount( activeEffects );

				// If it is neither an item nor an effect, report
				// the exception.

				updateDisplay( ERROR_STATE, "Invalid operand [" + right + "] on right side of operator" );
			}
		}

		// If it gets this far, then it must be numeric,
		// so parse the number and return it.

		return StaticEntity.parseInt( right );
	}

	private static AdventureResult effectParameter( String parameter )
	{
		List potentialEffects = StatusEffectDatabase.getMatchingNames( parameter );
		if ( potentialEffects.isEmpty() )
			return null;

		return new AdventureResult( (String) potentialEffects.get(0), 0, true );
	}

	private static AdventureResult itemParameter( String parameter )
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
		String option = parameters.split( " " )[0];

		if ( option.equals( "clear" ) )
		{
			conditions.clear();
			printLine( "Conditions list cleared." );
			return true;
		}
		else if ( option.equals( "check" ) )
		{
			checkRequirements( conditions );
			printLine( "Conditions list validated against available items." );
			return true;
		}
		else if ( option.equals( "mode" ) )
		{
			String conditionString = parameters.substring( option.length() ).trim();

			if ( conditionString.startsWith( "conjunction" ) || conditionString.startsWith( "and" ) )
				useDisjunction = false;
			else if ( conditionString.startsWith( "disjunction" ) || conditionString.startsWith( "or" ) )
				useDisjunction = true;

			if ( useDisjunction )
				printLine( "All non-stat conditions will be ORed together." );
			else
				printLine( "All non-stat conditions will be ANDed together." );

			return true;
		}
		else if ( option.equals( "add" ) )
		{
			AdventureResult condition;
			String [] conditionsList = parameters.substring( option.length() ).toLowerCase().trim().split( "\\s*,\\s*" );

			for ( int i = 0; i < conditionsList.length; ++i )
			{
				condition = extractCondition( conditionsList[i] );
				if ( condition == null )
					continue;

				if ( condition.getCount() > 0 )
				{
					AdventureResult.addResultToList( conditions, condition );
					printLine( "Condition added: " + condition );
				}
				else
				{
					printLine( "Condition already met: " + condition );
				}
			}

			return true;
		}

		printList( conditions );
		return false;
	}

	private AdventureResult extractCondition( String conditionString )
	{
		if ( conditionString.length() == 0 )
			return null;

		AdventureResult condition = null;
		Matcher meatMatcher = MEAT_PATTERN.matcher( conditionString );
		boolean isMeatCondition = meatMatcher.find() ? meatMatcher.group().length() == conditionString.length() : false;

		if ( isMeatCondition )
		{
			String [] splitCondition = conditionString.split( "\\s+" );
			int amount = StaticEntity.parseInt( splitCondition[0] );
			condition = new AdventureResult( AdventureResult.MEAT, amount );
		}
		else if ( conditionString.endsWith( "choiceadv" ) || conditionString.endsWith( "choices" ) || conditionString.endsWith( "choice" ) )
		{
			// If it's a choice adventure condition, parse out the
			// number of choice adventures the user wishes to do.

			String [] splitCondition = conditionString.split( "\\s+" );
			condition = new AdventureResult( AdventureResult.CHOICE, splitCondition.length > 1 ? StaticEntity.parseInt( splitCondition[0] ) : 1 );
		}
		else if ( conditionString.startsWith( "level" ) )
		{
			// If the condition is a level, then determine how many
			// substat points are required to the next level and
			// add the substat points as a condition.

			String [] splitCondition = conditionString.split( "\\s+" );
			int level = StaticEntity.parseInt( splitCondition[1] );

			int [] subpoints = new int[3];
			int primeIndex = KoLCharacter.getPrimeIndex();

			subpoints[ primeIndex ] = KoLCharacter.calculateSubpoints( (level - 1) * (level - 1) + 4, 0 ) -
				KoLCharacter.getTotalPrime();

			for ( int i = 0; i < subpoints.length; ++i )
				subpoints[i] = Math.max( 0, subpoints[i] );

			condition = new AdventureResult( subpoints );

			// Make sure that if there was a previous substat condition,
			// and it modifies the same stat as this condition, that the
			// greater of the two remains and the two aren't added.

			int previousIndex = conditions.indexOf( condition );
			if ( previousIndex != -1 )
			{
				AdventureResult previousCondition = (AdventureResult) conditions.get( previousIndex );

				for ( int i = 0; i < subpoints.length; ++i )
					if ( subpoints[i] != 0 && previousCondition.getCount(i) != 0 )
						subpoints[i] = Math.max( 0, subpoints[i] - previousCondition.getCount(i) );

				condition = new AdventureResult( subpoints );
			}
		}
		else if ( conditionString.endsWith( "mus" ) || conditionString.endsWith( "muscle" ) || conditionString.endsWith( "moxie" ) ||
			conditionString.endsWith( "mys" ) || conditionString.endsWith( "myst" ) || conditionString.endsWith( "mox" ) || conditionString.endsWith( "mysticality" ) )
		{
			String [] splitCondition = conditionString.split( "\\s+" );
			int points = StaticEntity.parseInt( splitCondition[0] );

			int [] subpoints = new int[3];
			int statIndex = conditionString.indexOf( "mus" ) != -1 ? 0 : conditionString.indexOf( "mys" ) != -1 ? 1 : 2;
			subpoints[ statIndex ] = KoLCharacter.calculateSubpoints( points, 0 );

			subpoints[ statIndex ] -= conditionString.indexOf( "mus" ) != -1 ? KoLCharacter.getTotalMuscle() :
				conditionString.indexOf( "mys" ) != -1 ? KoLCharacter.getTotalMysticality() :
				KoLCharacter.getTotalMoxie();

			for ( int i = 0; i < subpoints.length; ++i )
				subpoints[i] = Math.max( 0, subpoints[i] );

			condition = new AdventureResult( subpoints );

			// Make sure that if there was a previous substat condition,
			// and it modifies the same stat as this condition, that the
			// greater of the two remains and the two aren't added.

			int previousIndex = conditions.indexOf( condition );
			if ( previousIndex != -1 )
			{
				AdventureResult previousCondition = (AdventureResult) conditions.get( previousIndex );

				for ( int i = 0; i < subpoints.length; ++i )
					if ( subpoints[i] != 0 && previousCondition.getCount(i) != 0 )
						subpoints[i] = Math.max( 0, subpoints[i] - previousCondition.getCount(i) );

				condition = new AdventureResult( subpoints );
			}
		}
		else if ( conditionString.endsWith( "health" ) || conditionString.endsWith( "mana" ) )
		{
			String numberString = conditionString.split( "\\s+" )[0];
			int points = StaticEntity.parseInt( numberString.endsWith( "%" ) ? numberString.substring( 0, numberString.length() - 1 ) : numberString );

			if ( numberString.endsWith( "%" ) )
			{
				if ( conditionString.endsWith( "health" ) )
					points = (int) ((float) points * (float)KoLCharacter.getMaximumHP() / 100.0f);
				else if ( conditionString.endsWith( "mana" ) )
					points = (int) ((float) points * (float)KoLCharacter.getMaximumMP() / 100.0f);
			}

			points -= conditionString.endsWith( "health" ) ? KoLCharacter.getCurrentHP() :
				KoLCharacter.getCurrentMP();

			condition = new AdventureResult( conditionString.endsWith( "health" ) ? AdventureResult.HP : AdventureResult.MP, points );

			int previousIndex = conditions.indexOf( condition );
			if ( previousIndex != -1 )
			{
				AdventureResult previousCondition = (AdventureResult) conditions.get( previousIndex );
				condition = condition.getInstance( condition.getCount() - previousCondition.getCount() );
			}
		}
		else if ( conditionString.endsWith( "outfit" ) )
		{
			// Usage: conditions add <location> outfit
			String outfitLocation;

			if (conditionString.equals("outfit"))
				outfitLocation = StaticEntity.getProperty("lastAdventure");
			else
				outfitLocation = conditionString.substring(0, conditionString.length() - 7);

			// Try to support outfit names by mapping some outfits to their locations
			if (outfitLocation.equals("guard") || outfitLocation.equals("elite") || outfitLocation.equals("elite guard"))
				outfitLocation = "treasury";

			if (outfitLocation.equals("rift"))
				outfitLocation = "battlefield";

			if (outfitLocation.equals("cloaca-cola") || outfitLocation.equals("cloaca cola"))
				outfitLocation = "cloaca";

			if( outfitLocation.equals("dyspepsi-cola") || outfitLocation.equals("dyspepsi cola"))
				outfitLocation = "dyspepsi";

			KoLAdventure lastAdventure = AdventureDatabase.getAdventure( outfitLocation );

			if ( !(lastAdventure instanceof KoLAdventure) )
				updateDisplay( ERROR_STATE, "Unrecognized location: "+ outfitLocation);

			else if ( !EquipmentDatabase.addOutfitConditions(lastAdventure ))
				updateDisplay( ERROR_STATE, "No outfit corresponds to " + lastAdventure.getAdventureName() + ".");
		}
		else
		{
			// Otherwise, it's an item or status-effect condition, so parse
			// out which item or effect is desired and set that as the condition.

			condition = getFirstMatchingItem( conditionString );
		}

		return condition;
	}

	/**
	 * A special module used to handle campground requests, such
	 * as toast retrieval, resting, relaxing, and the like.
	 */

	private void executeCampgroundRequest( String parameters )
	{
		String [] parameterList = parameters.split( " " );
		StaticEntity.getClient().makeRequest( new CampgroundRequest( parameterList[0] ),
			parameterList.length == 1 ? 1 : StaticEntity.parseInt( parameterList[1] ) );
	}

	/**
	 * A special module used to handle casting skills on yourself or others.
	 * Castable skills must be listed in usableSkills
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

		String [] buffParameters = splitCountAndName( splitParameters[0] );
		String buffCountString = buffParameters[0];
		String skillNameString = buffParameters[1];

		String skillName = getUsableSkillName( skillNameString );
		if ( skillName == null )
		{
			updateDisplay( "You don't have a skill matching \"" + parameters + "\"" );
			return;
		}

		int buffCount = 1;

		if ( buffCountString != null && buffCountString.equals( "*" ) )
		{
			buffCount = (int) ( KoLCharacter.getCurrentMP() /
				ClassSkillsDatabase.getMPConsumptionById( ClassSkillsDatabase.getSkillId( skillName ) ) );
		}
		else if ( buffCountString != null )
		{
			buffCount = StaticEntity.parseInt( buffCountString );
		}

		if ( buffCount > 0 )
			RequestThread.postRequest( UseSkillRequest.getInstance( skillName, splitParameters[1], buffCount ) );
	}

	private String [] splitCountAndName( String parameters )
	{
		String nameString;
		String countString;

		if ( parameters.startsWith( "\"" ) )
		{
			nameString = parameters.substring( 1, parameters.length() - 1 );
			countString = null;
		}
		else if ( parameters.startsWith( "*" ) || (parameters.indexOf( " " ) != -1 && Character.isDigit( parameters.charAt( 0 ) )) )
		{
			countString = parameters.split( " " )[0];
			String rest = parameters.substring( countString.length() ).trim();

			if ( rest.startsWith( "\"" ) )
			{
				nameString = rest.substring( 1, rest.length() - 1 );
			}
			else
			{
				nameString = rest;
			}
		}
		else
		{
			nameString = parameters;
			countString = null;
		}

		return new String [] { countString, nameString };
	}

	/**
	 * Utility method used to retrieve the full name of a skill,
	 * given a substring representing it.
	 */

	public static String getSkillName( String substring, List list )
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
	{	return getSkillName( substring, availableSkills );
	}

	/**
	 * Utility method used to retrieve the full name of a combat skill,
	 * given a substring representing it.
	 */

	public static String getUsableSkillName( String substring )
	{	return getSkillName( substring, usableSkills );
	}

	/**
	 * Utility method used to retrieve the full name of a combat skill,
	 * given a substring representing it.
	 */

	public static String getCombatSkillName( String substring )
	{	return getSkillName( substring, ClassSkillsDatabase.getSkillsByType( ClassSkillsDatabase.COMBAT ) );
	}

	/**
	 * A special module used specifically for handling donations,
	 * including donations to the statues and donations to the clan.
	 */

	private void executeDonateCommand( String parameters )
	{
		int heroId;  int amount = -1;

		String [] parameterList = parameters.split( " " );

		if ( parameterList[0].startsWith( "boris" ) || parameterList[0].startsWith( "mus" ) )
		{
			heroId = HeroDonationRequest.BORIS;
		}
		else if ( parameterList[0].startsWith( "jarl" ) || parameterList[0].startsWith( "mys" ) )
		{
			heroId = HeroDonationRequest.JARLSBERG;
		}
		else if ( parameterList[0].startsWith( "pete" ) || parameterList[0].startsWith( "mox" ) )
		{
			heroId = HeroDonationRequest.PETE;
		}
		else
		{
			updateDisplay( ERROR_STATE, parameters + " is not a statue." );
			return;
		}

		amount = StaticEntity.parseInt( parameterList[1] );
		updateDisplay( "Donating " + amount + " to the shrine..." );
		RequestThread.postRequest( new HeroDonationRequest( heroId, amount ) );
	}

	/**
	 * A special module used specifically for equipping items.
	 */

	private void executeEquipCommand( String parameters )
	{
		parameters = parameters.toLowerCase();

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

		if ( parameters.indexOf( "(no change)" ) != -1 )
			return;

		// Look for name of slot
		String command = parameters.split( " " )[0];
		int slot = EquipmentRequest.slotNumber( command );

		if ( slot != -1 )
			parameters = parameters.substring( command.length() ).trim();

		AdventureResult match = getFirstMatchingItem( parameters );
		if ( match == null )
		{
			// No item exists which matches the given
			// substring - error out.

			updateDisplay( ERROR_STATE, "No item matching substring \"" + parameters + "\"" );
			return;
		}

		// If he didn't specify slot name, decide where this item goes.
		if ( slot == -1 )
		{
			// If it's already equipped anywhere, give up
			for ( int i = 0; i <= KoLCharacter.FAMILIAR; ++i )
			{
				AdventureResult item = KoLCharacter.getEquipment( i );
				if ( item != null && item.getName().toLowerCase().indexOf( parameters ) != -1 )
					return;
			}

			// It's not equipped. Choose a slot for it
			slot = EquipmentRequest.chooseEquipmentSlot( TradeableItemDatabase.getConsumptionType( match.getItemId() ) );

			// If it can't be equipped, give up
			if ( slot == -1 )
			{
				updateDisplay( ERROR_STATE, "You can't equip a	" + match.getName() );
				return;
			}
		}
		else
		{
			// See if desired item is already in selected slot
			if ( KoLCharacter.getEquipment( slot ).equals( match ) )
				return;
		}

		// If you are currently dual-wielding and the new weapon type
		// (melee or ranged) doesn't match the offhand weapon, unequip
		// the off-hand weapon

		if ( KoLCharacter.dualWielding() && ( slot == KoLCharacter.WEAPON || slot == KoLCharacter.OFFHAND ) )
        {
			int itemId = match.getItemId();
			int desiredHands = EquipmentDatabase.getHands( itemId );
			boolean desiredType = EquipmentDatabase.isRanged( itemId );
			boolean currentType = EquipmentDatabase.isRanged( KoLCharacter.getEquipment( KoLCharacter.WEAPON ).getName() );

            // If we are equipping a new weapon, a two-handed
            // weapon will unequip any pair of weapons. But a
            // one-handed weapon much match the type of the
            // off-hand weapon. If it doesn't, unequip the off-hand
            // weapon first

			if ( slot == KoLCharacter.WEAPON )
            {
            	if ( desiredHands < 2 && desiredType != currentType )
            		executeLine( "unequip off-hand" );
            }

            // If we are equipping an off-hand weapon, fail the
            // request if its type does not agree with the type of
            // the main weapon.

			else if ( slot == KoLCharacter.OFFHAND )
			{
				if ( desiredHands == 1 && desiredType != currentType )
				{
				updateDisplay( ERROR_STATE, "You can't wield a " + ( desiredType ? "ranged" : "melee" ) + " weapon in your off-hand with a " + ( currentType ? "ranged" : "melee" ) + " weapon in your main hand." );
				return;
				}
			}
		}

		RequestThread.postRequest( new EquipmentRequest( match, slot ) );
	}

	/**
	 * A special module used specifically for unequipping items.
	 */

	private void executeUnequipCommand( String parameters )
	{
		// Look for name of slot
		String command = parameters.split( " " )[0];
		int slot = EquipmentRequest.slotNumber( command );

		if ( slot != -1 )
		{
			RequestThread.postRequest( new EquipmentRequest( EquipmentRequest.UNEQUIP, slot ) );
			return;
		}

		parameters = parameters.toLowerCase();

		// Allow player to remove all of his fake hands
		if ( parameters.equals( "fake hand" ) )
		{
			if ( KoLCharacter.getFakeHands() == 0 )
				updateDisplay( ERROR_STATE, "You're not wearing any fake hands" );
			else
				RequestThread.postRequest( new EquipmentRequest( EquipmentRequest.UNEQUIP, KoLCharacter.FAKEHAND ) );

			return;
		}

		// The following loop removes all items with the
		// specified name.

		for ( int i = 0; i <= KoLCharacter.FAMILIAR; ++i )
		{
			AdventureResult item = KoLCharacter.getEquipment( i );
			if ( item != null && item.getName().toLowerCase().indexOf( parameters ) != -1 )
				RequestThread.postRequest( new EquipmentRequest( EquipmentRequest.UNEQUIP, i ) );
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
			return;
		}

		parameters = parameters.trim();
		int spaceIndex = parameters.indexOf( " " );

		String list = spaceIndex == -1 ? parameters : parameters.substring( 0, spaceIndex ).trim();
		String filter = spaceIndex == -1 ? "" : parameters.substring( spaceIndex ).trim();

		PrintStream desiredOutputStream = outputStream;

		if ( !filter.equals( "" ) &&
			(parameters.startsWith( "summary" ) || parameters.startsWith( "session" ) || parameters.startsWith( "stat" ) || parameters.startsWith( "equip" ) || parameters.startsWith( "encounters" )) )
		{
			File outputFile = new File( filter );
			filter = "";

			outputFile = new File( outputFile.getAbsolutePath() );
			desiredOutputStream = LogStream.openStream( outputFile, false );
		}

		executePrintCommand( list, filter, desiredOutputStream );
	}

	/**
	 * A special module used specifically for properly printing out
	 * data relevant to the current session.  This method is more
	 * specialized than its counterpart and is used when the data
	 * to be printed is known, as well as the stream to print to.
	 * Usually called by its counterpart to handle specific instances.
	 */

	private void executePrintCommand( String desiredData, String filter, PrintStream desiredStream )
	{
		PrintStream originalStream = outputStream;
		outputStream = desiredStream;

		KoLmafiaCLI.printBlankLine();

		if ( desiredData.equals( "session" ) )
		{
			printLine( "Player: " + KoLCharacter.getUserName() );
			printLine( "Session Id: " + KoLRequest.sessionId );
			printLine( "Password Hash: " + KoLRequest.passwordHash );
			printLine( "Current Server: " + KoLRequest.KOL_HOST );
		}
		else if ( desiredData.startsWith( "stat" ) )
		{
			printToSession = true;

			printLine( "Lv: " + KoLCharacter.getLevel() );
			printLine( "HP: " + KoLCharacter.getCurrentHP() + " / " + COMMA_FORMAT.format( KoLCharacter.getMaximumHP() ) );
			printLine( "MP: " + KoLCharacter.getCurrentMP() + " / " + COMMA_FORMAT.format( KoLCharacter.getMaximumMP() ) );

			printBlankLine();

			printLine( "Mus: " + getStatString( KoLCharacter.getBaseMuscle(), KoLCharacter.getAdjustedMuscle(), KoLCharacter.getMuscleTNP() ) );
			printLine( "Mys: " + getStatString( KoLCharacter.getBaseMysticality(), KoLCharacter.getAdjustedMysticality(), KoLCharacter.getMysticalityTNP() ) );
			printLine( "Mox: " + getStatString( KoLCharacter.getBaseMoxie(), KoLCharacter.getAdjustedMoxie(), KoLCharacter.getMoxieTNP() ) );

			printBlankLine();

			printLine( "Advs: " + KoLCharacter.getAdventuresLeft() );
			printLine( "Meat: " + COMMA_FORMAT.format( KoLCharacter.getAvailableMeat() ) );
			printLine( "Drunk: " + KoLCharacter.getInebriety() );

			printBlankLine();

			printLine( "Pet: " + KoLCharacter.getFamiliar() );
			printLine( "Item: " + KoLCharacter.getFamiliarItem() );

			printToSession = false;
		}
		else if ( desiredData.startsWith( "equip" ) )
		{
			printToSession = true;

			printLine( "Hat: " + KoLCharacter.getEquipment( KoLCharacter.HAT ) );
			printLine( "Weapon: " + KoLCharacter.getEquipment( KoLCharacter.WEAPON ) );

			if ( KoLCharacter.getFakeHands() > 0 )
				printLine( "Fake Hands: " + KoLCharacter.getFakeHands() );

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

			printToSession = false;
		}
		else if ( desiredData.startsWith( "encounters" ) )
		{
			printLine( "Visited Locations: " );
			printBlankLine();

			printList( adventureList );

			printBlankLine();
			printBlankLine();

			updateDisplay( "Encounter Listing: " );

			printBlankLine();

			printList( encounterList );
		}
		else
		{
			List mainList = desiredData.equals( "closet" ) ? closet : desiredData.equals( "summary" ) ? tally :
				desiredData.equals( "outfits" ) ? KoLCharacter.getOutfits() : desiredData.equals( "familiars" ) ? KoLCharacter.getFamiliarList() :
				desiredData.equals( "effects" ) ? activeEffects : desiredData.equals( "skills" ) ? availableSkills :
				desiredData.equals( "closet" ) ? closet : inventory;

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

		KoLmafiaCLI.printBlankLine();

		if ( outputStream != originalStream )
		{
			KoLmafiaCLI.printBlankLine();
			outputStream.close();
		}

		outputStream = originalStream;
	}

	private static String getStatString( int base, int adjusted, int tnp )
	{
		StringBuffer statString = new StringBuffer();
		statString.append( COMMA_FORMAT.format( adjusted ) );

		if ( base != adjusted )
			statString.append( " (" + COMMA_FORMAT.format( base ) + ")" );

		statString.append( ", tnp = " );
		statString.append( COMMA_FORMAT.format( tnp ) );

		return statString.toString();
	}

	/**
	 * Utility method which determines the first effect which matches
	 * the given parameter string.  Note that the string may also
	 * specify an effect duration before the string.
	 */

	public static AdventureResult getFirstMatchingEffect( String parameters )
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

			effectName = (String) matchingNames.get(0);
			duration = durationString.equals( "*" ) ? 0 : StaticEntity.parseInt( durationString );
		}

		if ( effectName == null )
		{
			updateDisplay( ERROR_STATE, "[" + parameters + "] does not match anything in the status effect database." );
			return null;
		}

		return new AdventureResult( effectName, duration, true );
	}

	public static int getFirstMatchingItemId( List nameList )
	{
		if ( nameList.isEmpty() )
			return -1;

		String [] nameArray = new String[ nameList.size() ];
		nameList.toArray( nameArray );

		int lowestId = Integer.MAX_VALUE;
		boolean npcStoreMatch = false;

		int itemId;

		for ( int i = 0; i < nameArray.length; ++i )
		{
			itemId = TradeableItemDatabase.getItemId( nameArray[i] );

			if ( isCreationMatch && ConcoctionsDatabase.getMixingMethod( itemId ) == ItemCreationRequest.NOCREATE &&
				itemId != ItemCreationRequest.MEAT_PASTE && itemId != ItemCreationRequest.MEAT_STACK && itemId != ItemCreationRequest.DENSE_STACK )
					continue;

			if ( NPCStoreDatabase.contains( nameArray[i], false ) )
			{
				if ( !npcStoreMatch || itemId < lowestId )
					lowestId = itemId;

				npcStoreMatch = true;
			}

			if ( !npcStoreMatch && itemId < lowestId )
				lowestId = itemId;
		}

		return lowestId == Integer.MAX_VALUE ? -1 : lowestId;
	}

	/**
	 * Utility method which determines the first item which matches
	 * the given parameter string.  Note that the string may also
	 * specify an item quantity before the string.
	 */

	public static AdventureResult getFirstMatchingItem( String parameters )
	{
		int itemId = -1;
		int itemCount = 1;

		// First, allow for the person to type without specifying
		// the amount, if the amount is 1.

		List matchingNames = new ArrayList();
		matchingNames.addAll( TradeableItemDatabase.getMatchingNames( parameters ) );
		if ( !matchingNames.isEmpty() )
		{
			String itemName = (String) matchingNames.get(0);
			if ( itemName.indexOf( parameters ) == -1 )
				matchingNames.clear();
		}

		if ( matchingNames.isEmpty() && parameters.indexOf( " " ) != -1 )
		{
			boolean isNumeric = parameters.charAt(0) == '-' || Character.isDigit( parameters.charAt(0) );
			for ( int i = 1; i < parameters.length() && parameters.charAt(i) != ' '; ++i )
				isNumeric &= Character.isDigit( parameters.charAt(i) );

			if ( isNumeric )
			{
				itemCount = StaticEntity.parseInt( parameters.substring( 0, parameters.indexOf( " " ) ) );
				String itemName = parameters.substring( parameters.indexOf( " " ) ).trim();
				matchingNames.addAll( TradeableItemDatabase.getMatchingNames( itemName ) );

				if ( matchingNames.isEmpty() )
					itemId = TradeableItemDatabase.getItemId( itemName, itemCount );
			}
			else if ( parameters.charAt(0) == '*' )
			{
				itemCount = 0;
				matchingNames.addAll( TradeableItemDatabase.getMatchingNames( parameters.substring(1).trim() ) );
			}
			else
				matchingNames.addAll( TradeableItemDatabase.getMatchingNames( parameters ) );
		}
		else
			matchingNames.addAll( TradeableItemDatabase.getMatchingNames( parameters ) );

		// Next, check to see if any of the items matching appear
		// in an NPC store.  If so, automatically default to it.

		if ( !matchingNames.isEmpty() && itemId == -1 )
			itemId = getFirstMatchingItemId( matchingNames );

		if ( itemId == -1 )
		{
			updateDisplay( ERROR_STATE, "[" + parameters + "] does not match anything in the item database." );
			return null;
		}

		AdventureResult firstMatch = new AdventureResult( itemId, itemCount );

		// The result also depends on the number of items which
		// are available in the given match area.

		int matchCount;

		if ( isCreationMatch )
		{
			ItemCreationRequest instance = ItemCreationRequest.getInstance( firstMatch.getItemId() );
			matchCount = instance == null ? 0 : instance.getQuantityPossible();
		}

		else
			matchCount = firstMatch.getCount( inventory );

		// In the event that the person wanted all except a certain
		// quantity, be sure to update the item count.

		if ( itemCount <= 0 )
		{
			itemCount = matchCount + itemCount;
			firstMatch = firstMatch.getInstance( itemCount );
		}

		if ( isExecutingCheckOnlyCommand && firstMatch != null )
		{
			printLine( firstMatch.toString() );
			return null;
		}

		return itemCount <= 0 ? null : firstMatch;
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

		Object [] items = getMatchingItemList( parameters );
		if ( items.length == 0 )
			return;

		if ( ClanManager.getStash().isEmpty() )
			RequestThread.postRequest( new ClanStashRequest() );

		RequestThread.postRequest( new ClanStashRequest( items, isWithdraw ?
			ClanStashRequest.STASH_TO_ITEMS : ClanStashRequest.ITEMS_TO_STASH ) );
	}

	/**
	 * Untinkers an item (not specified).  This is generally not
	 * used by the CLI interface, but is defined to override the
	 * abstract method provided in the KoLmafia class.
	 */

	public void executeUntinkerRequest( String parameters )
	{
		if ( parameters.equals( "" ) )
		{
			UntinkerRequest.completeQuest();
			return;
		}

		AdventureResult firstMatch = getFirstMatchingItem( parameters );
		if ( firstMatch == null )
			return;

		RequestThread.postRequest( new UntinkerRequest( firstMatch.getItemId(), firstMatch.getCount() ) );
	}

	/**
	 * Set the Canadian Mind Control device to selected setting.
	 */

	public void executeMindControlRequest( String parameters )
	{
		int setting = StaticEntity.parseInt( parameters );
		RequestThread.postRequest( new MindControlRequest( setting ) );
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
		int goal = StaticEntity.parseInt( goalString );

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
			int square = StaticEntity.parseInt( squareString );

			// Skip past command and square
			parameters = parameters.substring( command.length() ).trim();
			parameters = parameters.substring( squareString.length() ).trim();

			if ( parameters.indexOf( "mushroom" ) == -1 )
				parameters = parameters.trim() + " mushroom";

			int spore = getFirstMatchingItem( parameters ).getItemId();

			if ( spore == -1 )
			{
				updateDisplay( ERROR_STATE, "Unknown spore: " + parameters );
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

			int square = StaticEntity.parseInt( squareString );
			MushroomPlot.pickMushroom( square, true );
		}
		else if ( command.equals( "harvest" ) )
			MushroomPlot.harvestMushrooms();

		String plot = MushroomPlot.getMushroomPlot( false );

		if ( permitsContinue() )
		{
			StringBuffer plotDetails = new StringBuffer();
			plotDetails.append( "Current:" );
			plotDetails.append( LINE_BREAK );
			plotDetails.append( plot );
			plotDetails.append( LINE_BREAK );
			plotDetails.append( "Forecast:" );
			plotDetails.append( LINE_BREAK );
			plotDetails.append( MushroomPlot.getForecastedPlot( false ) );
			plotDetails.append( LINE_BREAK );
			printLine( plotDetails.toString() );
		}
	}

	public Object [] getMatchingItemList( String itemList )
	{
		String [] itemNames = itemList.split( "\\s*,\\s*" );

		AdventureResult firstMatch = null;
		ArrayList items = new ArrayList();

		for ( int i = 0; i < itemNames.length; ++i )
		{
			if ( itemNames[i].endsWith( "meat" ) )
			{
				String amountString = itemNames[i].split( " " )[0];
				int amount = amountString.equals( "*" ) ? 0 : StaticEntity.parseInt( amountString );
				firstMatch = new AdventureResult( AdventureResult.MEAT, amount > 0 ? amount : KoLCharacter.getAvailableMeat() + amount );
			}
			else
				firstMatch = getFirstMatchingItem( itemNames[i] );

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
		Object [] items = getMatchingItemList( parameters );
		if ( items.length == 0 )
			return;

		int meatAttachmentCount = 0;

		for ( int i = 0; i < items.length; ++i )
		{
			if ( ((AdventureResult)items[i]).getName().equals( AdventureResult.MEAT ) )
			{
				RequestThread.postRequest( new ItemStorageRequest(
					((AdventureResult)items[i]).getCount(), ItemStorageRequest.PULL_MEAT_FROM_STORAGE ) );

				items[i] = null;
				++meatAttachmentCount;
			}
		}

		if ( meatAttachmentCount == items.length )
			return;

		RequestThread.postRequest( new ItemStorageRequest(
			ItemStorageRequest.STORAGE_TO_INVENTORY, items ) );
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

		Object [] items = getMatchingItemList( parameters.substring(4).trim() );
		if ( items.length == 0 )
			return;

		int meatAttachmentCount = 0;

		for ( int i = 0; i < items.length; ++i )
		{
			if ( ((AdventureResult)items[i]).getName().equals( AdventureResult.MEAT ) )
			{
				RequestThread.postRequest( new ItemStorageRequest( ((AdventureResult)items[i]).getCount(),
					parameters.startsWith( "take" ) ? ItemStorageRequest.MEAT_TO_INVENTORY : ItemStorageRequest.MEAT_TO_CLOSET ) );

				items[i] = null;
				++meatAttachmentCount;
			}
		}

		if ( meatAttachmentCount == items.length )
			return;

		RequestThread.postRequest( new ItemStorageRequest(
			parameters.startsWith( "take" ) ? ItemStorageRequest.CLOSET_TO_INVENTORY : ItemStorageRequest.INVENTORY_TO_CLOSET, items ) );
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

		AdventureResult firstMatch = getFirstMatchingItem( itemName.toString() );
		if ( firstMatch == null )
			return;

		RequestThread.postRequest( new AutoSellRequest( firstMatch,
			StaticEntity.parseInt( tokens[ tokens.length - 2 ] ), StaticEntity.parseInt( tokens[ tokens.length - 1 ] ) ) );
	}

	/**
	 * A special module used specifically for properly instantiating
	 * AutoSellRequests for just autoselling items.
	 */

	private void executeAutoSellRequest( String parameters )
	{
		Object [] items = getMatchingItemList( parameters );
		if ( items.length == 0 )
			return;

		RequestThread.postRequest( new AutoSellRequest( items, AutoSellRequest.AUTOSELL ) );
	}

	/**
	 * Utility method used to make a purchase from the
	 * Kingdom of Loathing mall.  What this does is
	 * create a mall search request, and buys the
	 * given quantity of items.
	 */

	public void executeBuyCommand( String parameters )
	{
		SpecialOutfit.createImplicitCheckpoint();

		Object [] matches = getMatchingItemList( parameters );

		for ( int i = 0; i < matches.length; ++i )
		{
			AdventureResult match = (AdventureResult) matches[i];

			if ( !KoLCharacter.canInteract() && !NPCStoreDatabase.contains( match.getName() ) )
			{
				updateDisplay( ERROR_STATE, "You are not yet out of ronin." );
				return;
			}

			ArrayList results = new ArrayList();

			StoreManager.searchMall( '\"' + match.getName() + '\"', results, 10, false );
			StaticEntity.getClient().makePurchases( results, results.toArray(), match.getCount() );
		}

		SpecialOutfit.restoreImplicitCheckpoint();
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

		isCreationMatch = true;
		AdventureResult firstMatch = getFirstMatchingItem( parameters );
		isCreationMatch = false;

		if ( firstMatch == null )
			return;


		ItemCreationRequest irequest = ItemCreationRequest.getInstance( firstMatch.getItemId() );
		if ( irequest == null )
		{
			boolean needServant = !StaticEntity.getBooleanProperty( "createWithoutBoxServants" );

			switch ( ConcoctionsDatabase.getMixingMethod( firstMatch.getItemId() ) )
			{
			case ItemCreationRequest.COOK:
			case ItemCreationRequest.COOK_REAGENT:
			case ItemCreationRequest.SUPER_REAGENT:
			case ItemCreationRequest.COOK_PASTA:

				if ( needServant )
					updateDisplay( ERROR_STATE, "You cannot cook without a chef-in-the-box." );
				else if ( !AdventureDatabase.retrieveItem( ItemCreationRequest.OVEN ) )
					return;

				irequest = ItemCreationRequest.getInstance( firstMatch.getItemId() );
				break;

			case ItemCreationRequest.MIX:
			case ItemCreationRequest.MIX_SPECIAL:
			case ItemCreationRequest.MIX_SUPER:

				if ( needServant )
					updateDisplay( ERROR_STATE, "You cannot mix without a bartender-in-the-box." );
				else if ( AdventureDatabase.retrieveItem( ItemCreationRequest.KIT ) )
					return;

				irequest = ItemCreationRequest.getInstance( firstMatch.getItemId() );
				break;

			default:

				updateDisplay( ERROR_STATE, "That item cannot be created." );
				return;
			}
		}

		irequest.setQuantityNeeded( firstMatch.getCount() );
		RequestThread.postRequest( irequest );
	}

	/**
	 * A special module used specifically for properly instantiating
	 * ConsumeItemRequests.
	 */

	private void executeConsumeItemRequest( String parameters )
	{
		if ( previousLine.startsWith( "eat" ) && makeRestaurantRequest( parameters ) )
			return;

		if ( previousLine.startsWith( "drink" ) && makeMicrobreweryRequest( parameters ) )
			return;

		// Now, handle the instance where the first item is actually
		// the quantity desired, and the next is the amount to use

		AdventureResult firstMatch = getFirstMatchingItem( parameters );
		if ( firstMatch == null )
			return;

		if ( previousLine.startsWith( "eat" ) )
		{
			if ( TradeableItemDatabase.getConsumptionType( firstMatch.getItemId() ) != ConsumeItemRequest.CONSUME_EAT )
			{
				updateDisplay( ERROR_STATE, firstMatch.getName() + " cannot be consumed." );
				return;
			}
		}

		if ( previousLine.startsWith( "drink" ) || previousLine.startsWith( "hobodrink" ) )
		{
			if ( TradeableItemDatabase.getConsumptionType( firstMatch.getItemId() ) != ConsumeItemRequest.CONSUME_DRINK )
			{
				updateDisplay( ERROR_STATE, firstMatch.getName() + " is not an alcoholic beverage." );
				return;
			}
		}

		if ( previousLine.startsWith( "use" ) && !StaticEntity.getBooleanProperty( "allowGenericUse" ) )
		{
			switch ( TradeableItemDatabase.getConsumptionType( firstMatch.getItemId() ) )
			{
			case ConsumeItemRequest.CONSUME_EAT:
				updateDisplay( ERROR_STATE, firstMatch.getName() + " must be eaten." );
				return;
			case ConsumeItemRequest.CONSUME_DRINK:
				updateDisplay( ERROR_STATE, firstMatch.getName() + " is an alcoholic beverage." );
				return;
			}
		}

		ConsumeItemRequest request = !previousLine.startsWith( "hobodrink" ) ? new ConsumeItemRequest( firstMatch ) :
			new ConsumeItemRequest( ConsumeItemRequest.CONSUME_HOBO, firstMatch );

		RequestThread.postRequest( request );
	}

	/**
	 * A special module for instantiating display case management requests,
	 * strictly for adding and removing things.
	 */

	private void executeDisplayCaseRequest( String parameters )
	{
		Object [] items = getMatchingItemList( parameters.substring(4).trim() );
		if ( items.length == 0 )
			return;

		RequestThread.postRequest(
			new MuseumRequest( items, !parameters.startsWith( "take" ) ) );
	}

	private void executeSpiceLoop( String parameters )
	{
		int loopsToExecute = parameters.equals( "" ) ? 1 : StaticEntity.parseInt( parameters );
		if ( loopsToExecute < 1 )
			loopsToExecute += KoLCharacter.getAdventuresLeft();

		if ( isLuckyCharacter() )
		{
			executeAdventureRequest( "sewer with clovers" );
			--loopsToExecute;
		}
		else
			executeLine( "acquire worthless item" );

		if ( !HermitRequest.isCloverDay() )
		{
			updateDisplay( ERROR_STATE, "Today is not a clover day." );
			return;
		}

		if ( HermitRequest.neededPermits() && !AdventureDatabase.retrieveItem( HermitRequest.PERMIT.getInstance( loopsToExecute ) ) )
			return;

		int itemCount = HermitRequest.getWorthlessItemCount();
		int cloverCount = SewerRequest.CLOVER.getCount( inventory );

		int loopsExecuted = 0;

		while ( permitsContinue() && loopsExecuted < loopsToExecute )
		{
			itemCount = HermitRequest.getWorthlessItemCount();
			if ( itemCount > 0 )
				executeHermitRequest( itemCount + " ten-leaf clover" );

			cloverCount = Math.min( loopsToExecute - loopsExecuted, SewerRequest.CLOVER.getCount( inventory ) );
			executeAdventureRequest( cloverCount + " sewer with clovers" );
			loopsExecuted += cloverCount;
		}

		itemCount = HermitRequest.getWorthlessItemCount();
		if ( itemCount > 0 )
			executeHermitRequest( itemCount + " ten-leaf clover" );
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

			adventureCount = adventureCountString.equals( "*" ) ? 0 : StaticEntity.parseInt( adventureCountString );

			if ( adventureCount <= 0 && adventure.getFormSource().equals( "shore.php" ) )
				adventureCount += (int) Math.floor( KoLCharacter.getAdventuresLeft() / 3 );
			else if ( adventureCount <= 0 )
				adventureCount += KoLCharacter.getAdventuresLeft();
		}

		if ( isExecutingCheckOnlyCommand )
		{
			printLine( adventure.toString() );
			return;
		}

		StaticEntity.getClient().makeRequest( adventure, adventureCount );
	}

	/**
	 * Special module used specifically for properly instantiating
	 * requests to change the user's outfit.
	 */

	private void executeChangeOutfitCommand( String parameters )
	{
		SpecialOutfit intendedOutfit = getMatchingOutfit( parameters );

		if ( intendedOutfit == null )
		{
			updateDisplay( ERROR_STATE, "You can't wear that outfit." );
			return;
		}

		RequestThread.postRequest( new EquipmentRequest( intendedOutfit ) );
	}

	public static SpecialOutfit getMatchingOutfit( String name )
	{
		String lowercaseOutfitName = name.toLowerCase().trim();
		if ( lowercaseOutfitName.equals( "birthday suit" ) || lowercaseOutfitName.equals( "nothing" ) )
			return SpecialOutfit.BIRTHDAY_SUIT;

		Object [] outfits = new Object[ KoLCharacter.getCustomOutfits().size() ];
		KoLCharacter.getCustomOutfits().toArray( outfits );

		for ( int i = 0; i < outfits.length; ++i )
			if ( outfits[i] instanceof SpecialOutfit && outfits[i].toString().toLowerCase().indexOf( lowercaseOutfitName ) != -1 )
				return (SpecialOutfit) outfits[i];

		for ( int i = 0; i < EquipmentDatabase.getOutfitCount(); ++i )
		{
			SpecialOutfit test = EquipmentDatabase.getOutfit( i );
			if ( test != null && test.toString().toLowerCase().indexOf( lowercaseOutfitName ) != -1 )
				return test;
		}

		return null;
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

		BuffBotHome.setBuffBotActive( true );
		BuffBotManager.runBuffBot( StaticEntity.parseInt( parameters ) );
		updateDisplay( "Buffbot execution complete." );
	}

	/**
	 * Attempts to remove the effect specified in the most recent command.
	 * If the string matches multiple effects, all matching effects will
	 * be removed.
	 */

	public void executeUneffectRequest( String parameters )
	{
		List matchingEffects = StatusEffectDatabase.getMatchingNames( parameters );
		if ( matchingEffects.isEmpty() )
		{
			updateDisplay( ERROR_STATE, "Unknown effect: " + parameters );
			return;
		}
		else if ( matchingEffects.size() > 1 )
		{
			// If there's only one shruggable buff on the list, then
			// that's probably the one the player wants.

			int shruggableCount = 0;
			AdventureResult buffToRemove = null;

			for ( int i = 0; i < matchingEffects.size(); ++i )
				if ( UneffectRequest.isShruggable( ((AdventureResult)matchingEffects.get(i)).getName() ) )
					++shruggableCount;

			if ( shruggableCount == 1 )
			{
				if ( activeEffects.contains( buffToRemove ) )
					RequestThread.postRequest( new UneffectRequest( buffToRemove ) );

				return;
			}

			updateDisplay( ERROR_STATE, "Ambiguous effect name: " + parameters );

			printLine( "This could match any of the following " + matchingEffects.size() + " effects: " );
			printList( matchingEffects );

			return;
		}

		AdventureResult effect = new AdventureResult( parameters, 1, true );
		if ( activeEffects.contains( effect ) )
			RequestThread.postRequest( new UneffectRequest( effect ) );
	}

	/**
	 * Attempts to zap the specified item with the specified wand
	 */

	public void makeZapRequest()
	{
		if ( previousLine == null )
			return;

		if ( !previousLine.startsWith( "zap" ) || previousLine.indexOf( " " ) == -1 )
			return;

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

		AdventureResult item = getFirstMatchingItem( parameters );
		if ( item == null )
			return;

		RequestThread.postRequest( new ZapRequest( wand, item ) );
	}

	/**
	 * Attempts to smash the specified item
	 */

	public void makePulverizeRequest( String parameters )
	{
		AdventureResult item = getFirstMatchingItem( parameters );
		if ( item == null )
			return;

		if ( !TradeableItemDatabase.isTradeable( item.getItemId() ) )
		{
			// Force him to confirm this, somehow? That doesn't
			// work in a script...
			return;
		}

		RequestThread.postRequest( new PulverizeRequest( item ) );
	}

	/**
	 * Retrieves the items specified in the most recent command.  If there
	 * are no clovers available, the request will abort.
	 */

	public void executeHermitRequest( String parameters )
	{
		boolean clovers = HermitRequest.isCloverDay();

		if ( !permitsContinue() )
			return;

		if ( parameters.equals( "" ) )
		{
			updateDisplay( "Today is " + ( clovers ? "" : "not " ) + "a clover day." );
			return;
		}

		AdventureResult item = getFirstMatchingItem( parameters );
		if ( item == null )
			return;

		if ( !hermitItems.contains( item ) )
		{
			updateDisplay( ERROR_STATE, "You can't get " + parameters + " from the hermit today." );
			return;
		}

		RequestThread.postRequest( new HermitRequest( item.getItemId(), item.getCount() ) );
	}

	/**
	 * Makes a trade with the trapper which exchanges all of your current
	 * furs for the given fur.
	 */

	public void executeTrapperRequest( String parameters )
	{
		// If he doesn't specify a number, use number of yeti furs
		AdventureResult item = getFirstMatchingItem( parameters );
		if ( item == null )
			return;

		int itemId = item.getItemId();
		int tradeCount = Character.isDigit( parameters.charAt(0) ) ? item.getCount() :
			TrapperRequest.YETI_FUR.getCount( inventory );

		// Ensure that the requested item is available from the trapper
		for ( int i = 0; i < trapperItemNumbers.length; ++i )
			if ( trapperItemNumbers[i] == itemId )
			{
				RequestThread.postRequest( new TrapperRequest( itemId, tradeCount ) );
				return;
			}
	}

	/**
	 * Makes a request to the hunter which exchanges all of the given item
	 * with the hunter.  If the item is not available, this method does
	 * not report an error.
	 */

	public void executeHunterRequest( String parameters )
	{
		parameters = parameters.toLowerCase();
		if ( hunterItems.isEmpty() )
			RequestThread.postRequest( new BountyHunterRequest() );

		if ( parameters.equals( "" ) )
		{
			printList( hunterItems );
			return;
		}

		for ( int i = 0; i < hunterItems.size(); ++i )
			if ( ((String)hunterItems.get(i)).indexOf( parameters ) != -1 )
				RequestThread.postRequest( new BountyHunterRequest( TradeableItemDatabase.getItemId( (String) hunterItems.get(i) ) ) );
	}

	/**
	 * Makes a request to the restaurant to purchase a meal.  If the item
	 * is not available, this method does not report an error.
	 */

	public boolean makeRestaurantRequest( String parameters )
	{
		if ( restaurantItems.isEmpty() && KoLCharacter.inMysticalitySign() )
			RequestThread.postRequest( new RestaurantRequest() );

		if ( parameters.equals( "" ) )
		{
			printList( restaurantItems );
			return false;
		}

		String [] splitParameters = splitCountAndName( parameters );
		String countString = splitParameters[0];
		String nameString = splitParameters[1];

		for ( int i = 0; i < restaurantItems.size(); ++i )
		{
			String name = (String) restaurantItems.get(i);
			if ( name.toLowerCase().indexOf( nameString ) != -1 )
			{
				int count = countString == null || countString.length() == 0 ? 1 :
					StaticEntity.parseInt( countString );

				for ( int j = 0; j < count; ++j )
					RequestThread.postRequest( new RestaurantRequest( name ) );

				return true;
			}
		}

		return false;
	}

	/**
	 * Makes a request to Doc Galaktik to purchase a cure.  If the
	 * cure is not available, this method does not report an error.
	 */

	public void executeGalaktikRequest( String parameters )
	{
		if ( previousLine == null )
			return;

		if ( !previousLine.startsWith( "galaktik" ) )
			return;

		// Cure "HP" or "MP"

		int type = 0;
		if ( parameters.equalsIgnoreCase( "hp" ) )
			type = GalaktikRequest.HP;
		else if ( parameters.equalsIgnoreCase( "mp" ) )
			type = GalaktikRequest.MP;
		else
		{
			updateDisplay( ERROR_STATE, "Unknown Doc Galaktik request <" + parameters + ">" );
			return;
		}

		RequestThread.postRequest( new GalaktikRequest( type ) );
	}

	/**
	 * Makes a request to the microbrewery to purchase a drink.  If the
	 * item is not available, this method does not report an error.
	 */

	public boolean makeMicrobreweryRequest( String parameters )
	{
		if ( microbreweryItems.isEmpty() && KoLCharacter.inMoxieSign() )
			RequestThread.postRequest( new MicrobreweryRequest() );

		if ( parameters.equals( "" ) )
		{
			printList( microbreweryItems );
			return false;
		}

		String [] splitParameters = splitCountAndName( parameters );
		String countString = splitParameters[0];
		String nameString = splitParameters[1];

		for ( int i = 0; i < microbreweryItems.size(); ++i )
		{
			String name = (String) microbreweryItems.get(i);
			if ( name.toLowerCase().indexOf( nameString ) != -1 )
			{
				int count = countString == null || countString.length() == 0 ? 1 :
					StaticEntity.parseInt( countString );

				for ( int j = 0; j < count; ++j )
					RequestThread.postRequest( new MicrobreweryRequest( name ) );
				return true;
			}
		}

		return false;
	}

	private static final int MAXIMUM_LINE_LENGTH = 96;

	public static void printBlankLine()
	{	printLine( CONTINUE_STATE, " " );
	}

	public static void printLine( String message )
	{	printLine( CONTINUE_STATE, message );
	}

	public static void printLine( int state, String message )
	{
		if ( message == null || message.length() == 0 || (message.trim().length() == 0 && previousUpdateString.length() == 0) )
			return;

		previousUpdateString = message.trim();

		if ( printToSession )
			sessionStream.println( message );

		StringBuffer wordWrappedLine = new StringBuffer();
		StringTokenizer lineTokens = new StringTokenizer( message, LINE_BREAK, true );

		while ( lineTokens.hasMoreTokens() )
		{
			String currentToken = lineTokens.nextToken();
			String remainingString = currentToken;

			while ( remainingString.length() > 0 )
			{
				if ( remainingString.indexOf( " " ) == -1 || remainingString.length() < MAXIMUM_LINE_LENGTH )
				{
					wordWrappedLine.append( remainingString );
					remainingString = "";
				}
				else
				{
					int splitIndex = remainingString.lastIndexOf( " ", MAXIMUM_LINE_LENGTH );
					if ( splitIndex == -1 )
						splitIndex = remainingString.indexOf( " ", MAXIMUM_LINE_LENGTH );

					wordWrappedLine.append( remainingString.substring( 0, splitIndex ) );
					wordWrappedLine.append( LINE_BREAK );

					remainingString = remainingString.substring( splitIndex + 1 );
				}
			}
		}

		if ( !isPrompting )
			outputStream.println( wordWrappedLine.toString() );

		mirrorStream.println( wordWrappedLine.toString() );
		debugStream.println( wordWrappedLine.toString() );

		StringBuffer colorBuffer = new StringBuffer();

		if ( message.trim().equals( "" ) )
		{
			colorBuffer.append( "<br>" );
		}
		else
		{
			if ( message.indexOf( "<" ) != -1 && message.indexOf( "\n" ) != -1 )
				message = StaticEntity.globalStringReplace( message, "<", "&lt;" );

			boolean addedColor = false;

			if ( state == ERROR_STATE || state == ABORT_STATE )
			{
				addedColor = true;
				colorBuffer.append( "<font color=red>" );
			}
			else if ( message.startsWith( " > QUEUED" ) )
			{
				addedColor = true;
				colorBuffer.append( "<font color=olive><b>" );
			}
			else if ( message.startsWith( " > " ) )
			{
				addedColor = true;
				colorBuffer.append( "<font color=olive>" );
			}

			colorBuffer.append( wordWrappedLine.toString().replaceAll( "[" + LINE_BREAK + "]+", "<br>" ) );
			if ( message.startsWith( " > QUEUED" ) )
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
}
