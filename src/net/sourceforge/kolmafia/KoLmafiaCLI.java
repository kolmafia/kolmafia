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

import java.net.URLEncoder;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.PrintStream;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.TreeMap;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.velocityreviews.forums.HttpTimeoutClient;

public class KoLmafiaCLI extends KoLmafia
{
	public static final KoLmafiaCLI DEFAULT_SHELL = new KoLmafiaCLI( System.in );
	private static final KoLRequest AUTO_ATTACKER = new KoLRequest( "account.php?action=autoattack" );

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

	private static List matchList = inventory;

	private static boolean isFoodMatch = false;
	private static boolean isBoozeMatch = false;
	private static boolean isUsageMatch = false;
	private static boolean isCreationMatch = false;
	private static boolean isUntinkerMatch = false;
	private static boolean isExecutingCheckOnlyCommand = false;

	private String previousLine = null;
	private String currentLine = null;
	private BufferedReader commandStream;

	private static final TreeMap ALIASES = new TreeMap();
	static
	{
		String [] data;
		BufferedReader reader = KoLDatabase.getReader( new File( SETTINGS_LOCATION, "aliases_GLOBAL.txt" ) );

		if ( reader != null )
		{
			while ( (data = KoLDatabase.readData( reader )) != null )
				if ( data.length >= 2 )
					ALIASES.put( data[0].toLowerCase(), data[1] );

			try
			{
				reader.close();
			}
			catch ( Exception e )
			{
				StaticEntity.printStackTrace( e );
			}
		}
	}

	public static final void initialize()
	{
		System.out.println();
		System.out.println( StaticEntity.getVersion() );
		System.out.println( "Running on " + System.getProperty( "os.name" ) );
		System.out.println( "Using Java " + System.getProperty( "java.version" ) );
		System.out.println();

		StaticEntity.setClient( DEFAULT_SHELL );
		RequestLogger.openStandard();
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
			this.commandStream = KoLDatabase.getReader( inputStream );
		}
		catch ( Exception e )
		{
			// This should not happen.  Therefore, print
			// a stack trace for debug purposes.

			StaticEntity.printStackTrace( e, "Error opening input stream." );
		}
	}

	/**
	 * Utility method used to prompt the user for their login and
	 * password.  Later on, when profiles are added, prompting
	 * for the user will automatically look up a password.
	 */

	public void attemptLogin( String username )
	{
		try
		{
			if ( username == null || username.length() == 0 )
			{
				System.out.println();
				System.out.print( "username: " );
				username = this.commandStream.readLine();
			}

			if ( username == null || username.length() == 0 )
			{
				System.out.println( "Invalid login." );
				return;
			}

			if ( username.startsWith( "login " ) )
				username = username.substring( 6 ).trim();

			String password = getSaveState( username );

			if ( password == null )
			{
				System.out.print( "password: " );
				password = this.commandStream.readLine();
			}

			if ( password == null || password.length() == 0 )
			{
				System.out.println( "Invalid password." );
				return;
			}

			System.out.println();
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
	 * the login has been confirmed to notify the client that the
	 * login was successful, the user-specific settings should be
	 * loaded, and the user can begin adventuring.
	 */

	public void initialize( String username )
	{
		super.initialize( username );

		try
		{
			String holiday = MoonPhaseDatabase.getHoliday( DAILY_FORMAT.parse( DAILY_FORMAT.format( new Date() ) ), true );
			updateDisplay( holiday + ", " + MoonPhaseDatabase.getMoonEffect() );
		}
		catch ( Exception e )
		{
			// Should not happen, you're parsing something that
			// was formatted the same way.

			StaticEntity.printStackTrace( e );
		}

		if ( KoLSettings.getGlobalProperty( "initialFrames" ).indexOf( "LocalRelayServer" ) != -1 )
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
			System.out.print( " > " );
		}

		String line = null;

		while ( permitsContinue() && (line = this.getNextLine()) != null )
		{
			if ( StaticEntity.getClient() == this )
				RequestLogger.printLine();

			this.executeLine( line );

			if ( StaticEntity.getClient() == this )
			{
				RequestLogger.printLine();
				System.out.print( " > " );
			}

			if ( StaticEntity.getClient() == this )
				forceContinue();
		}

		try
		{
			this.commandStream.close();
			this.currentLine = null;
		}
		catch ( IOException e )
		{
			// This should not happen.  Therefore, print
			// a stack trace for debug purposes.

			StaticEntity.printStackTrace( e );
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

				line = this.commandStream.readLine();
			}
			while ( line != null && (line.trim().length() == 0 || line.trim().startsWith( "#" ) || line.trim().startsWith( "//" ) || line.trim().startsWith( "\'" )) );

			// You will either have reached the end of file, or you will
			// have a valid line -- return it.

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
		if ( line == null || refusesContinue() )
			return;

		line = line.replaceAll( "[ \t]+", " " ).trim();
		if ( line.length() == 0 )
			return;

		this.currentLine = line;

		// Handle if-statements in a special way right
		// here.  Nesting is handled explicitly by
		// reading until the last statement in the line
		// is no longer an if-statement.

		String lowercase = line.toLowerCase();

		if ( lowercase.startsWith( "if " ) || lowercase.startsWith( "while " ) )
		{
			int splitIndex = line.indexOf( ";" );
			boolean isWhile = lowercase.startsWith( "while" );

			String condition;
			StringBuffer statement = new StringBuffer();

			// Attempt to construct the entire string which will be involved
			// in the test.  All dangling 'if' statements at the end of a line
			// are to include the entire next line be part of this line.

			if ( splitIndex != -1 )
			{
				condition = line.substring( 0, splitIndex ).trim();
				statement.append( line.substring( splitIndex + 1 ).trim() );
			}
			else
			{
				condition = line;
				statement.append( this.getNextLine() );
			}

			String lastTest = "";

			// Here, we search for dangling if-statements, and while they
			// exist, we add them to the full statement.  Note, however,
			// that users sometimes end lines with ";" for no good reason
			// at all, even for if-statements, so acknowledge and find
			// the real end statement in spite of this.

			splitIndex = statement.lastIndexOf( ";" );
			lastTest = statement.substring( splitIndex + 1 ).toLowerCase().trim();

			while ( lastTest.length() == 0 )
			{
				statement.delete( splitIndex, statement.length() );
				splitIndex = statement.lastIndexOf( ";" );
				lastTest = statement.substring( splitIndex + 1 ).toLowerCase().trim();
			}

			while ( lastTest.startsWith( "if " ) || lastTest.startsWith( "while " ) )
			{
				statement.append( this.getNextLine() );
				lastTest = statement.substring( statement.lastIndexOf( ";" ) + 1 ).toLowerCase().trim();

				while ( lastTest.length() == 0 )
				{
					statement.delete( splitIndex, statement.length() );
					splitIndex = statement.lastIndexOf( ";" );
					lastTest = statement.substring( splitIndex + 1 ).toLowerCase().trim();
				}
			}

			// Now that we finally have the complete if-statement, we find
			// the condition string, test for it, and execute the line if
			// applicable.  Otherwise, we skip the entire line.

			condition = condition.substring( condition.indexOf( " " ) + 1 );

			if ( isWhile )
			{
				while ( testConditional( condition ) )
					this.executeLine( statement.toString() );
			}
			else
			{
				if ( testConditional( condition ) )
					this.executeLine( statement.toString() );
			}

			this.previousLine = condition + ";" + statement;
			return;
		}

		// Check to see if we can execute the line iteratively, which
		// is possible whenever if-statements aren't involved.

		int splitIndex = line.indexOf( ";" );

		if ( splitIndex != -1 && !line.startsWith( "alias" ) )
		{
			// Determine all the individual statements which need
			// to be executed based on the existence of the 'set'
			// command, which may wrap things in quotes.

			ArrayList sequenceList = new ArrayList();
			String remainder = line.trim();

			do
			{
				String current = remainder.toLowerCase();

				// Allow argument to "set" command to be a
				// quoted string

				if ( current.startsWith( "set" ) )
				{
					int quoteIndex = current.indexOf( "\"" );
					if ( quoteIndex != -1 && quoteIndex < splitIndex )
					{
						quoteIndex = current.indexOf( "\"", quoteIndex + 1 );
						if ( quoteIndex != -1 )
							splitIndex = current.indexOf( ";", quoteIndex );
					}
				}

				if ( splitIndex != -1 )
				{
					current = remainder.substring( 0, splitIndex ).trim();
					if ( current.length() > 0 )
						sequenceList.add( current );
					remainder = remainder.substring( splitIndex + 1 ).trim();
										splitIndex = remainder.indexOf( ";" );
				}
			}
			while ( splitIndex != -1 );

			sequenceList.add( remainder );

			// If there are multiple statements to be executed,
			// then check if there are any conditional statements.
			// If there are, you will need to run everything
			// recursively.	 Otherwise, an iterative approach works
			// best.

			if ( sequenceList.size() > 1 )
			{
				String [] sequence = new String[ sequenceList.size() ];
				sequenceList.toArray( sequence );

				boolean canExecuteIteratively = true;

				for ( int i = 0; canExecuteIteratively && i < sequence.length; ++i )
					canExecuteIteratively = !sequence[i].toLowerCase().startsWith( "if " ) && !sequence[i].toLowerCase().startsWith( "while " );

				if ( canExecuteIteratively )
				{
					// Handle multi-line sequences by executing them one after
					// another.  This is ideal, but not always possible.

					for ( int i = 0; i < sequence.length && permitsContinue(); ++i )
						this.executeLine( sequence[i] );
				}
				else
				{
					// Handle multi-line sequences by executing the first command
					// and using recursion to execute the remainder of the line.
					// This ensures that nested if-statements are preserved.

					String part1 = line.substring( 0, sequence[0].length() ).trim();
					String part2 = line.substring( sequence[0].length() + 1 ).trim();

					this.executeLine( part1 );

					if ( permitsContinue() )
						this.executeLine( part2 );
				}

				this.previousLine = line;
				return;
			}

			// If there are zero or one, then you either do nothing or you
			// continue on with the revised line.

			if ( sequenceList.isEmpty() )
				return;

			line = (String) sequenceList.get(0);
		}

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

		// Maybe a request to burn excess MP, as generated
		// by the gCLI or the relay browser?

		if ( line.equalsIgnoreCase( "save as mood" ) )
		{
			MoodSettings.minimalSet();
			MoodSettings.saveSettings();
			return;
		}

		if ( line.equalsIgnoreCase( "burn extra mp" ) )
		{
			this.recoverHP();

			SpecialOutfit.createImplicitCheckpoint();
			MoodSettings.burnExtraMana( true );
			SpecialOutfit.restoreImplicitCheckpoint();

			return;
		}

		if ( line.equalsIgnoreCase( "<inline-ash-script>" ) )
		{
			ByteArrayStream ostream = new ByteArrayStream();

			String currentLine = this.getNextLine();

			while ( currentLine != null && !currentLine.equals( "</inline-ash-script>" ) )
			{
				try
				{
					ostream.write( currentLine.getBytes() );
					ostream.write( LINE_BREAK.getBytes() );
				}
				catch ( Exception e )
				{
					// Byte array output streams do not throw errors,
					// other than out of memory errors.

					StaticEntity.printStackTrace( e );
				}

				currentLine = this.getNextLine();
			}

			if ( currentLine == null )
			{
				updateDisplay( ERROR_STATE, "Unterminated inline ASH script." );
				return;
			}

			KoLmafiaASH interpreter = new KoLmafiaASH();
			interpreter.validate( ostream.getByteArrayInputStream() );
			interpreter.execute( "main", null );

			return;
		}

		// Not a special full-line command.  Go ahead and
		// split the command into extra pieces.

		String command = line.trim().split( " " )[0].toLowerCase().trim();
		String parameters = line.substring( command.length() ).trim();

		if ( command.endsWith( "?" ) )
		{
			isExecutingCheckOnlyCommand = true;
			command = command.substring( 0, command.length() - 1 );
		}

		RequestThread.openRequestSequence();
		this.executeCommand( command, parameters );
		RequestThread.closeRequestSequence();

		if ( !command.equals( "repeat" ) )
			this.previousLine = line;

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

		if ( command.equals( "version" ) )
		{
			RequestLogger.printLine( StaticEntity.getVersion() );
			return;
		}

		if ( StaticEntity.isDisabled( command ) )
		{
			RequestLogger.printLine( "Called disabled command: " + command + " " + parameters );
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

		if ( command.equals( "logecho" ) || command.equals( "logprint" ) )
		{
			if ( parameters.equalsIgnoreCase( "timestamp" ) )
				parameters = MoonPhaseDatabase.getCalendarDayAsString( new Date() );

			parameters = StaticEntity.globalStringDelete( StaticEntity.globalStringDelete( parameters, "\n" ), "\r" );
			parameters = StaticEntity.globalStringReplace( parameters, "<", "&lt;" );

			RequestLogger.getSessionStream().println( " > " + parameters );
		}

		if ( command.equals( "log" ) )
		{
			if ( parameters.equals( "snapshot" ) )
			{
				executeCommand( "log", "moon, status, equipment, skills, effects, modifiers" );
				return;
			}

			RequestLogger.updateSessionLog();
			RequestLogger.updateSessionLog( "=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=" );

			RequestLogger.getDebugStream().println();
			RequestLogger.getDebugStream().println( "=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=" );

			StringBuffer title = new StringBuffer( "Player Snapshot" );

			int leftIndent = (46 - title.length()) / 2;
			for ( int i = 0; i < leftIndent; ++i )
				title.insert( 0, ' ' );

			RequestLogger.updateSessionLog( title.toString() );
			RequestLogger.updateSessionLog( "=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=" );

			RequestLogger.getDebugStream().println( title.toString() );
			RequestLogger.getDebugStream().println( "=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=" );

			String [] options = parameters.split( "\\s*,\\s*" );

			for ( int i = 0; i < options.length; ++i )
			{
				RequestLogger.updateSessionLog();
				RequestLogger.updateSessionLog( " > " + options[i] );

				this.showData( options[i], true );
			}

			RequestLogger.updateSessionLog();
			RequestLogger.updateSessionLog( "=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=" );

			RequestLogger.getDebugStream().println();
			RequestLogger.getDebugStream().println( "=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=" );

			RequestLogger.updateSessionLog();
			RequestLogger.updateSessionLog();

			RequestLogger.getDebugStream().println();
			RequestLogger.getDebugStream().println();

			return;
		}

		if ( command.equals( "alias" ) )
		{
			int spaceIndex = parameters.indexOf( " " );
			if ( spaceIndex != -1 )
			{
				LogStream aliasStream = LogStream.openStream( new File( SETTINGS_LOCATION, "aliases_GLOBAL.txt" ), false );

				String aliasString = parameters.substring( 0, spaceIndex ).toLowerCase().trim();
				String aliasCommand = parameters.substring( spaceIndex ).trim();

				aliasStream.println( aliasString + "\t" + aliasCommand );
				aliasStream.close();

				ALIASES.put( aliasString, aliasCommand );
				RequestLogger.printLine( "Command successfully aliased." );
				RequestLogger.printLine( aliasString + " => " + aliasCommand );
			}
			else
			{
				updateDisplay( ERROR_STATE, "That was not a valid aliasing." );
			}

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
			}

			KoLDesktop.displayDesktop();
			return;
		}

		// Allow access to individual frames so you can
		// do things in the GUI.

		if ( parameters.equals( "" ) )
		{
			if ( command.equals( "basement" ) )
			{
				BasementRequest.checkBasement();
				return;
			}

			if ( findScriptFile( command ) != null )
			{
				this.executeScript( command );
				return;
			}

			if ( command.equals( "chat" ) )
			{
				KoLmafiaGUI.constructFrame( "KoLMessenger" );
				return;
			}

			if ( command.equals( "mail" ) )
			{
				KoLmafiaGUI.constructFrame( "MailboxFrame" );
				return;
			}

			if ( command.startsWith( "opt" ) )
			{
				KoLmafiaGUI.constructFrame( "OptionsFrame" );
				return;
			}

			if ( command.equals( "item" ) )
			{
				KoLmafiaGUI.constructFrame( "ItemManageFrame" );
				return;
			}

			if ( command.equals( "clan" ) )
			{
				KoLmafiaGUI.constructFrame( "ClanManageFrame" );
				return;
			}

			if ( command.equals( "gear" ) )
			{
				KoLmafiaGUI.constructFrame( "GearChangeFrame" );
				return;
			}

			if ( command.equals( "pvp" ) )
			{
				KoLmafiaGUI.constructFrame( "FlowerHunterFrame" );
				return;
			}

			if ( command.equals( "radio" ) )
			{
				this.launchRadioKoL();
				return;
			}
		}

		// Maybe the person is trying to load a raw URL
		// to test something without creating a brand new
		// KoLRequest object to handle it yet?

		if ( command.startsWith( "http:" ) || command.indexOf( ".php" ) != -1 )
		{
			KoLRequest visitor = new KoLRequest( this.currentLine );
			if ( KoLRequest.shouldIgnore( visitor ) )
				return;

			RequestThread.postRequest( visitor );
			StaticEntity.externalUpdate( visitor.getURLString(), visitor.responseText );
			return;
		}

		// Allow a version which lets you see the resulting
		// text without loading a mini/relay browser window.

		if ( command.equals( "text" ) )
		{
			KoLRequest visitor = new KoLRequest( this.currentLine );
			if ( KoLRequest.shouldIgnore( visitor ) )
				return;

			RequestThread.postRequest( visitor );
			StaticEntity.externalUpdate( visitor.getURLString(), visitor.responseText );

			this.showHTML( visitor.getURLString(), visitor.responseText );
			return;
		}

		// Maybe the person wants to load up their browser
		// from the KoLmafia CLI?

		if ( command.equals( "relay" ) )
		{
			StaticEntity.getClient().openRelayBrowser();
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

		if ( command.equals( "junk" ) || command.equals( "cleanup" ) )
		{
			this.makeJunkRemovalRequest();
			return;
		}

		// Preconditions kickass, so they're handled right
		// after the wait command.  (Right)

		if ( command.startsWith( "goal" ) || command.startsWith( "condition" ) || command.startsWith( "objective" ) )
		{
			this.executeConditionsCommand( parameters );
			return;
		}

		// Handle the update command.  This downloads stuff
		// from the KoLmafia sourceforge website, so it does
		// not require for you to be online.

		if ( command.equals( "update" ) )
		{
			this.downloadAdventureOverride();
			return;
		}

		if ( command.equals( "newdata" ) )
		{
			TradeableItemDatabase.findItemDescriptions();
			StatusEffectDatabase.findStatusEffects();
			RequestLogger.printLine( "Data tables updated." );
			return;
		}

		if ( command.equals( "checkdata" ) )
		{
			int itemId = StaticEntity.parseInt( parameters );
			TradeableItemDatabase.checkInternalData( itemId );
			RequestLogger.printLine( "Internal data checked." );
			return;
		}

		if ( command.equals( "checkplurals" ) )
		{
			int itemId = StaticEntity.parseInt( parameters );
			TradeableItemDatabase.checkPlurals( itemId );
			RequestLogger.printLine( "Plurals checked." );
			return;
		}

		if ( command.equals( "checkmodifiers" ) )
		{
			Modifiers.checkModifiers();
			RequestLogger.printLine( "Modifiers checked." );
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

		// Adding the requested echo command.  I guess this is
		// useful for people who want to echo things...

		if ( command.equals( "cecho" ) || command.equals( "colorecho" ) )
		{
			int spaceIndex = parameters.indexOf( " " );
			String color = "#000000";

			if ( spaceIndex != -1 )
				color = parameters.substring( 0, spaceIndex ).replaceAll( "[\">]", "" );

			parameters = parameters.substring( spaceIndex + 1 );
			RequestLogger.printLine( "<font color=\"" + color + "\">" + StaticEntity.globalStringReplace( parameters, "<", "&lt;" ) + "</font>" );

			return;
		}

		if ( command.equals( "events" ) )
		{
			if ( parameters.equals( "clear" ) )
				eventHistory.clear();
			else
				printList( eventHistory );

			return;
		}

		if ( command.equals( "echo" ) || command.equals( "print" ) )
		{
			if ( parameters.equalsIgnoreCase( "timestamp" ) )
				parameters = MoonPhaseDatabase.getCalendarDayAsString( new Date() );

			parameters = StaticEntity.globalStringDelete( StaticEntity.globalStringDelete( parameters, "\n" ), "\r" );
			parameters = StaticEntity.globalStringReplace( parameters, "<", "&lt;" );

			RequestLogger.printLine( parameters );
			RequestLogger.getSessionStream().println( " > " + parameters );

			return;
		}

		// Adding another undocumented property setting command
		// so people can configure variables in scripts.

		if ( command.equals( "get" ) || command.equals( "set" ) )
		{
			int splitIndex = parameters.indexOf( "=" );
			if ( splitIndex == -1 )
			{
				if ( KoLSettings.isUserEditable( parameters ) )
					RequestLogger.printLine( KoLSettings.getUserProperty( parameters ) );

				return;
			}

			String name = parameters.substring( 0, splitIndex ).trim();
			if ( !KoLSettings.isUserEditable( name ) )
				return;

			String value = parameters.substring( splitIndex + 1 ).trim();
			if ( value.startsWith( "\"" ) )
				value = value.substring( 1, value.endsWith( "\"" ) ? value.length() - 1 : value.length() );

			if ( name.equals( "battleAction" ) )
			{
				if ( value.indexOf( ";" ) != -1 || value.startsWith( "consult" ) )
				{
					CombatSettings.setDefaultAction( value );
					value = "custom combat script";
				}
				else
				{
					value = CombatSettings.getLongCombatOptionName( value );
				}

				// Special handling of the battle action property,
				// such that auto-recovery gets reset as needed.

				if ( name.equals( "battleAction" ) && value != null )
					KoLCharacter.getBattleSkillNames().setSelectedItem( value );
			}

			if ( name.equals( "defaultAutoAttack" ) )
			{
				if ( value.indexOf( "disabled" ) != -1 )
				{
					value = "0";
				}
				else if ( value.indexOf( "attack" ) != -1 )
				{
					value = "1";
				}
				else if ( !Character.isDigit( value.charAt(0) ) )
				{
					String skillName = getSkillName( value, ClassSkillsDatabase.getSkillsByType( ClassSkillsDatabase.COMBAT ) );
					if ( skillName == null )
						return;

					value = String.valueOf( ClassSkillsDatabase.getSkillId( skillName ) );
				}

				if ( !KoLSettings.getUserProperty( "defaultAutoAttack" ).equals( value ) )
				{
					AUTO_ATTACKER.addFormField( "whichattack", value );
					RequestThread.postRequest( AUTO_ATTACKER );
				}
			}

			if ( name.equals( "customCombatScript" ) )
			{
				CombatSettings.setScript( parameters );
				return;
			}

			if ( KoLSettings.getUserProperty( name ).equals( value ) )
				return;

			if ( name.equals( "http.socketTimeout" ) )
			{
				int timeout = Math.max( 100, StaticEntity.parseInt( value ) );
				KoLSettings.setUserProperty( name, String.valueOf( timeout ) );
				HttpTimeoutClient.setHttpTimeout( timeout );
			}

			RequestLogger.printLine( name + " => " + value );
			KoLSettings.setUserProperty( name, value );
			return;
		}

		// Next, handle any requests to login or relogin.
		// This will be done by calling a utility method.

		if ( command.equals( "timein" ) )
		{
			LoginRequest.executeTimeInRequest();
			return;
		}

		if ( command.equals( "login" ) )
		{
			attemptLogin( parameters );
			return;
		}

		if ( command.equals( "logout" ) )
		{
			RequestThread.postRequest( new LogoutRequest() );
			return;
		}

		// Now for formal exit commands.

		if ( command.equals( "exit" ) || command.equals( "quit" ) )
		{
			RequestThread.postRequest( new LogoutRequest() );
			System.exit(0);
		}

		// Next, handle any requests for script execution;
		// these can be done at any time (including before
		// login), so they should be handled before a test
		// of login state needed for other commands.

		if ( command.equals( "namespace" ) )
		{
			// Validate the script first.

			String [] scripts = KoLSettings.getUserProperty( "commandLineNamespace" ).split( "," );
			for ( int i = 0; i < scripts.length; ++i )
			{
				RequestLogger.printLine( scripts[i] );
				File f = findScriptFile( scripts[i] );
				if ( f == null )
					continue;

				KoLmafiaASH interpreter = KoLmafiaASH.getInterpreter( f );
				if ( interpreter != null )
					interpreter.showUserFunctions( parameters );

				RequestLogger.printLine();
			}

			return;
		}

		if ( command.equals( "ashref" ) )
		{
			KoLmafiaASH.NAMESPACE_INTERPRETER.showExistingFunctions( parameters );
			return;
		}

		if ( command.equals( "using" ) )
		{
			// Validate the script first.

			this.executeCommand( "validate", parameters );
			if ( !permitsContinue() )
				return;

			String namespace = KoLSettings.getUserProperty( "commandLineNamespace" );
			if ( namespace.startsWith( parameters + "," ) || namespace.endsWith( "," + parameters ) || namespace.indexOf( "," + parameters + "," ) != -1 )
				return;

			if ( namespace.toString().equals( "" ) )
				namespace = parameters;
			else
				namespace = namespace + "," + parameters;

			KoLSettings.setUserProperty( "commandLineNamespace", namespace.toString() );
			return;
		}

		if ( command.equals( "verify" ) || command.equals( "validate" ) || command.equals( "check" ) || command.equals( "call" ) || command.equals( "run" ) || command.startsWith( "exec" ) || command.equals( "load" ) || command.equals( "start" ) )
		{
			this.executeScriptCommand( command, parameters );
			return;
		}

		// Next, handle repeat commands - these are a
		// carry-over feature which made sense in CLI.

		if ( command.equals( "repeat" ) )
		{
			if ( this.previousLine != null )
			{
				int repeatCount = parameters.length() == 0 ? 1 : StaticEntity.parseInt( parameters );
				for ( int i = 0; i < repeatCount && permitsContinue(); ++i )
				{
					RequestLogger.printLine( "Repetition " + (i+1) + " of " + repeatCount + "..." );
					this.executeLine( this.previousLine );
				}
			}

			return;
		}

		// Next, handle requests to start or stop
		// debug mode.

		if ( command.equals( "debug" ) )
		{
			if ( parameters.equals( "off" ) )
				RequestLogger.closeDebugLog();
			else
				RequestLogger.openDebugLog();

			return;
		}

		// Next, handle requests to start or stop
		// the mirror stream.

		if ( command.indexOf( "mirror" ) != -1 )
		{
			if ( command.indexOf( "end" ) != -1 || command.indexOf( "stop" ) != -1 || command.indexOf( "close" ) != -1 ||
				parameters.length() == 0 || parameters.equals( "end" ) || parameters.equals( "stop" ) || parameters.equals( "close" ) )
			{
				RequestLogger.closeMirror();
				updateDisplay( "Mirror stream closed." );
			}
			else
			{
				if ( !parameters.endsWith( ".txt" ) )
					parameters += ".txt";

				RequestLogger.openMirror( parameters );
			}

			return;
		}

		if ( command.equals( "wiki" ) )
		{
			List names = StatusEffectDatabase.getMatchingNames( parameters );
			if ( names.size() == 1 )
			{
				AdventureResult result = new AdventureResult( (String) names.get(0), 1, true );
				ShowDescriptionList.showWikiDescription( result );
				return;
			}

			AdventureResult result = getFirstMatchingItem( parameters );
			if ( result != null )
			{
				ShowDescriptionList.showWikiDescription( result );
				return;
			}

			try
			{
				StaticEntity.openSystemBrowser( "http://kol.coldfront.net/thekolwiki/index.php/Special:Search?search=" +
					URLEncoder.encode( parameters, "UTF-8" ) + "&go=Go" );
			}
			catch ( Exception e )
			{
				StaticEntity.openSystemBrowser( "http://kol.coldfront.net/thekolwiki/index.php/Special:Search?search=" +
					StaticEntity.globalStringReplace( parameters, " ", "+" ) + "&go=Go" );
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

			KoLRequest visitor = new KoLRequest( "desc_item.php?whichitem=" +
				TradeableItemDatabase.getDescriptionId( result.getItemId() ) );

			RequestThread.postRequest( visitor );

			if ( StaticEntity.getClient() instanceof KoLmafiaGUI )
				DescriptionFrame.showRequest( visitor );
			else
				this.showHTML( visitor.getURLString(), visitor.responseText );

			return;
		}

		if ( command.equals( "safe" ) )
		{
			this.showHTML( "", AdventureDatabase.getAreaCombatData( AdventureDatabase.getAdventure( parameters ).toString() ).toString() );
			return;
		}

		// Re-adding the breakfast command, just
		// so people can add it in scripting.

		if ( command.equals( "breakfast" ) )
		{
			StaticEntity.getClient().getBreakfast( false, true );
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

			this.showData( parameters );
			return;
		}

		// Look!  It's the command to complete the
		// Sorceress entryway.

		if ( command.equals( "entryway" ) )
		{
			if ( KoLCharacter.hasItem( SewerRequest.CLOVER.getInstance(1) ) || KoLCharacter.canInteract() )
				SorceressLair.completeCloveredEntryway();
			else
				SorceressLair.completeCloverlessEntryway();

			return;
		}

		// Look!  It's the command to complete the
		// Sorceress hedge maze!  This is placed
		// right after for consistency.

		if ( command.equals( "maze" ) || command.startsWith( "hedge" ) )
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
			this.trainFamiliar( parameters );
			return;
		}

		// Next is the command to visit the council.
		// This prints data to the command line.

		if ( command.equals( "council" ) )
		{
			RequestThread.postRequest( CouncilFrame.COUNCIL_VISIT );

			this.showHTML( "council.php", StaticEntity.singleStringReplace( CouncilFrame.COUNCIL_VISIT.responseText,
				"<a href=\"town.php\">Back to Seaside Town</a>", "" ) );

			return;
		}

		// Campground activities are fairly common, so
		// they will be listed first after the above.

		if ( command.startsWith( "camp" ) )
		{
			this.executeCampgroundRequest( parameters );
			return;
		}

		// Buffs are pretty neat, too - for now, it's
		// just casts on self

		if ( command.equals( "cast" ) || command.equals( "skill" ) )
		{
			if ( parameters.length() > 0 )
			{
				SpecialOutfit.createImplicitCheckpoint();
				this.executeCastBuffRequest( parameters );
				SpecialOutfit.restoreImplicitCheckpoint();
				return;
			}
		}

		if ( command.equals( "up" ) )
		{
			if ( parameters.indexOf( "," ) != -1 )
			{
				String [] effects = parameters.split( "\\s*,\\s*" );
				for ( int i = 0; i < effects.length; ++i )
					DEFAULT_SHELL.executeCommand( "up", effects[i] );

				return;
			}

			int effectId = StatusEffectDatabase.getEffectId( parameters );
			if ( effectId != -1 )
			{
				String effect = StatusEffectDatabase.getEffectName( effectId );
				String action = MoodSettings.getDefaultAction( "lose_effect", effect );
				if ( action.equals( "" ) )
				{
					KoLmafia.updateDisplay( ERROR_STATE, "No booster known: " + effect );
					return;
				}

				DEFAULT_SHELL.executeLine( action );
				return;
			}

			List names = StatusEffectDatabase.getMatchingNames( parameters );
			if ( names.isEmpty() )
			{
				KoLmafia.updateDisplay( ERROR_STATE, "Unknown effect: " + parameters );
				return;
			}

			KoLmafia.updateDisplay( ERROR_STATE, "Ambiguous effect name: " + parameters );
			printList( names );
			return;
		}

		// Uneffect with martians are related to buffs,
		// so listing them next seems logical.

		if ( command.equals( "shrug" ) || command.equals( "uneffect" ) || command.equals( "remedy" ) )
		{
			if ( parameters.indexOf( "," ) != -1 )
			{
				String [] effects = parameters.split( "\\s*,\\s*" );
				for ( int i = 0; i < effects.length; ++i )
					DEFAULT_SHELL.executeCommand( "uneffect", effects[i] );

				return;
			}

			this.executeUneffectRequest( parameters );
			return;
		}

		// Add in item retrieval the way KoLmafia handles
		// it internally.

		if ( command.equals( "find" ) || command.equals( "acquire" ) || command.equals( "retrieve" ) )
		{
			AdventureResult item = getFirstMatchingItem( parameters );
			if ( item != null )
				AdventureDatabase.retrieveItem( item, false );

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

		if ( command.equals( "!" ) || command.equals( "bang" ) )
		{
			for ( int i = 819; i <= 827; ++i )
			{
				String potion = TradeableItemDatabase.getItemName( i );
				potion = potion.substring( 0, potion.length() - 7 );
				RequestLogger.printLine( potion + ": " + KoLSettings.getUserProperty( "lastBangPotion" + i ) );
			}

			return;
		}

		if ( command.equals( "dusty" ) )
		{
			for ( int i = 2271; i <= 2276; ++i )
			{
				String bottle = TradeableItemDatabase.getItemName( i );
				String type = TradeableItemDatabase.dustyBottleType( i );
				RequestLogger.printLine( bottle + ": " + type );
			}

			return;
		}

		if ( command.equals( "demons" ) )
		{
			for ( int i = 0; i < KoLAdventure.DEMON_TYPES.length; ++i )
			{
				RequestLogger.printLine( (i+1) + ": " + KoLSettings.getUserProperty( "demonName" + (i+1) ) );
				RequestLogger.printLine( " => Found in the " + KoLAdventure.DEMON_TYPES[i][0] );
				RequestLogger.printLine( " => Gives " + KoLAdventure.DEMON_TYPES[i][1] );
			}

			return;
		}

		if ( command.equals( "summon" ) )
		{
			if ( parameters.length() == 0 )
				return;

			AdventureDatabase.retrieveItem( KoLAdventure.BLACK_CANDLE );
			if ( !KoLmafia.permitsContinue() )
				return;

			AdventureDatabase.retrieveItem( KoLAdventure.EVIL_SCROLL );
			if ( !KoLmafia.permitsContinue() )
				return;

			String demon = parameters;
			if ( Character.isDigit( parameters.charAt(0) ) )
			{
				demon = KoLSettings.getUserProperty( "demonName" + parameters );
			}
			else
			{
				for ( int i = 0; i < KoLAdventure.DEMON_TYPES.length; ++i )
				{
					if ( parameters.equalsIgnoreCase( KoLAdventure.DEMON_TYPES[i][0] ) )
						demon = KoLSettings.getUserProperty( "demonName" + (i+1) );
					else if ( parameters.equalsIgnoreCase( KoLAdventure.DEMON_TYPES[i][1] ) )
						demon = KoLSettings.getUserProperty( "demonName" + (i+1) );
					else if ( parameters.equalsIgnoreCase( KoLSettings.getUserProperty( "demonName" + (i+1) ) ) )
						demon = KoLSettings.getUserProperty( "demonName" + (i+1) );
				}
			}

			KoLRequest demonSummon = new KoLRequest( "manor3.php" );
			demonSummon.addFormField( "action", "summon" );
			demonSummon.addFormField( "demonname", demon );

			updateDisplay( "Summoning " + demon + "..." );
			RequestThread.postRequest( demonSummon );
			RequestThread.enableDisplayIfSequenceComplete();

			return;
		}

		// One command is an item usage request.  These
		// requests are complicated, so delegate to the
		// appropriate utility method.

		if ( command.equals( "eat" ) || command.equals( "drink" ) || command.equals( "use" ) || command.equals( "chew" ) || command.equals( "hobo" ) )
		{
			SpecialOutfit.createImplicitCheckpoint();
			this.executeConsumeItemRequest( parameters );
			SpecialOutfit.restoreImplicitCheckpoint();
			return;
		}

		// Zapping with wands is a type of item usage

		if ( command.equals( "zap" ) )
		{
			this.makeZapRequest();
			return;
		}

		// Smashing is a type of item usage

		if ( command.equals( "smash" ) || command.equals( "pulverize" ) )
		{
			this.makePulverizeRequest( parameters );
			return;
		}

		// Another item-related command is a creation
		// request.  Again, complicated request, so
		// delegate to the appropriate utility method.

		if ( command.equals( "create" ) || command.equals( "make" ) || command.equals( "bake" ) || command.equals( "mix" ) || command.equals( "smith" ) || command.equals( "tinker" ) )
		{
			this.executeItemCreationRequest( parameters );
			return;
		}

		// Another item-related command is an untinker
		// request.  Again, complicated request, so
		// delegate to the appropriate utility method.

		if ( command.equals( "untinker" ) )
		{
			this.executeUntinkerRequest( parameters );
			return;
		}

		if ( command.equals( "automall" ) )
		{
			this.makeAutoMallRequest();
			return;
		}

		// Another item-related command is the autosell
		// request.  This one's simple, but it still
		// gets its own utility method.

		if ( command.equals( "mallsell" ) )
		{
			this.executeMallSellRequest( parameters );
			return;
		}

		// Yay for more item-related commands.  This
		// one is the one that allows you to place
		// things into your clan stash.

		if ( command.equals( "stash" ) )
		{
			this.executeStashRequest( parameters );
			return;
		}

		// Another w00t for more item-related commands.
		// This one is the one that allows you to pull
		// things from storage.

		if ( command.equals( "hagnk" ) || command.equals( "pull" ) )
		{
			this.executeHagnkRequest( parameters );
			return;
		}

		// Another item-related command is the autosell
		// request.  This one's simple, but it still
		// gets its own utility method.

		if ( command.equals( "sell" ) || command.equals( "autosell" ) )
		{
			this.executeAutoSellRequest( parameters );
			return;
		}

		if ( command.equals( "reprice" ) || command.equals( "undercut" ) )
		{
			StaticEntity.getClient().priceItemsAtLowestPrice( true );
			return;
		}

		// Setting the Canadian mind control machine

		if ( command.equals( "mind-control" ) || command.equals( "mcd" ) )
		{
			this.executeMindControlRequest( parameters );
			return;
		}

		// Commands to manipulate the mushroom plot

		if ( command.equals( "field" ) )
		{
			this.executeMushroomCommand( parameters );
			return;
		}

		// Commands to manipulate the tlescope

		if ( command.equals( "telescope" ) )
		{
			this.executeTelescopeRequest( parameters );
			return;
		}

		// One of the largest commands is adventuring,
		// which (as usual) gets its own module.

		if ( command.startsWith( "adv" ) )
		{
			this.executeAdventureRequest( parameters );
			return;
		}

		// Donations get their own command and module,
		// too, which handles hero donations and basic
		// clan donations.

		if ( command.equals( "donate" ) )
		{
			this.executeDonateCommand( parameters );
			return;
		}

		// Another popular command involves changing
		// a specific piece of equipment.

		if ( command.equals( "equip" ) || command.equals( "wear" ) || command.equals( "wield" ) )
		{
			this.executeEquipCommand( parameters );
			return;
		}

		if ( command.equals( "second" ) || command.equals( "hold" ) || command.equals( "dualwield" ) )
		{
			this.executeEquipCommand( "off-hand " + parameters );
			return;
		}

		// You can remove a specific piece of equipment.

		if ( command.equals( "unequip" ) || command.equals( "remove" ) )
		{
			this.executeUnequipCommand( parameters );
			return;
		}

		// Another popular command involves changing
		// your current familiar.

		if ( command.equals( "familiar" ) )
		{
			if ( parameters.equals( "list" ) )
			{
				this.showData( "familiars " + parameters.substring(4).trim() );
				return;
			}
			else if ( parameters.length() == 0 )
			{
				this.showData( "familiars" );
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
			List familiarList = KoLCharacter.getFamiliarList();

			String [] familiars = new String[ familiarList.size() ];
			for ( int i = 0; i < familiarList.size(); ++i )
				familiars[i] = familiarList.get(i).toString().toLowerCase();

			FamiliarData newFamiliar = null;

			// First, try substring matching against the list of
			// familiars.

			for ( int i = 0; i < familiars.length && newFamiliar == null; ++i )
				if ( familiars[i].equals( lowerCaseName ) )
					newFamiliar = (FamiliarData) familiarList.get(i);

			for ( int i = 0; i < familiars.length && newFamiliar == null; ++i )
				if ( KoLDatabase.substringMatches( familiars[i], lowerCaseName, true ) )
					newFamiliar = (FamiliarData) familiarList.get(i);

			for ( int i = 0; i < familiars.length && newFamiliar == null; ++i )
				if ( KoLDatabase.substringMatches( familiars[i], lowerCaseName, false ) )
					newFamiliar = (FamiliarData) familiarList.get(i);

			// Boo, no matches.  Now try fuzzy matching, because the
			// end-user might be abbreviating.

			for ( int i = 0; i < familiars.length && newFamiliar == null; ++i )
				if ( KoLDatabase.fuzzyMatches( familiars[i], lowerCaseName ) )
					newFamiliar = (FamiliarData) familiarList.get(i);

			if ( newFamiliar != null )
			{
				if ( isExecutingCheckOnlyCommand )
				{
					RequestLogger.printLine( newFamiliar.toString() );
					return;
				}

				if ( KoLCharacter.getFamiliar() != null && KoLCharacter.getFamiliar().equals( newFamiliar ) )
					return;

				RequestThread.postRequest( new FamiliarRequest( newFamiliar ) );
				return;
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
				this.showData( "closet " + parameters.substring(4).trim() );
				return;
			}
			else if ( parameters.length() == 0 )
			{
				this.showData( "closet" );
				return;
			}

			this.executeClosetManageRequest( parameters );
			return;
		}

		// More commands -- this time to put stuff into
		// your display case (or remove them).

		if ( command.equals( "display" ) )
		{
			this.executeDisplayCaseRequest( parameters );
			return;
		}

		// Yet another popular command involves changing
		// your outfit.

		if ( command.equals( "checkpoint" ) )
		{
			SpecialOutfit.createExplicitCheckpoint();
			KoLmafia.updateDisplay( "Internal checkpoint created." );
			RequestThread.enableDisplayIfSequenceComplete();

			return;
		}

		if ( command.equals( "outfit" ) )
		{
			if ( parameters.equals( "list" ) )
			{
				this.showData( "outfits " + parameters.substring(4).trim() );
				return;
			}
			else if ( parameters.length() == 0 )
			{
				this.showData( "outfits" );
				return;
			}
			else if ( parameters.equalsIgnoreCase( "checkpoint" ) )
			{
				SpecialOutfit.restoreExplicitCheckpoint();
				return;
			}

			this.executeChangeOutfitCommand( parameters );
			return;
		}

		// Purchases from the mall are really popular,
		// as far as scripts go.  Nobody is sure why,
		// but they're popular, so they're implemented.

		if ( command.equals( "buy" ) || command.equals( "mallbuy" ) )
		{
			SpecialOutfit.createImplicitCheckpoint();
			this.executeBuyCommand( parameters );
			SpecialOutfit.restoreImplicitCheckpoint();
			return;
		}

		// The BuffBot may never get called from the CLI,
		// but we'll include it here for completeness sake

		if ( command.equals( "buffbot" ))
		{
			this.executeBuffBotCommand( parameters );
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

		if ( command.equals( "pvp" ) || command.equals( "attack" ) )
		{
			RequestThread.openRequestSequence();
			int stance = 0;

			if ( KoLCharacter.getBaseMuscle() >= KoLCharacter.getBaseMysticality() && KoLCharacter.getBaseMuscle() >= KoLCharacter.getBaseMoxie() )
				stance = 1;
			else if ( KoLCharacter.getBaseMysticality() >= KoLCharacter.getBaseMuscle() && KoLCharacter.getBaseMysticality() >= KoLCharacter.getBaseMoxie() )
				stance = 2;
			else
				stance = 3;

			String [] names = parameters.split( "\\s*,\\s*" );
			ProfileRequest [] targets = new ProfileRequest[ names.length ];

			for ( int i = 0; i < names.length; ++i )
			{
				String playerId = getPlayerId( names[i] );
				if ( !playerId.equals( names[i] ) )
					continue;

				BuffRequestFrame.ONLINE_VALIDATOR.addFormField( "playerid", String.valueOf( KoLCharacter.getUserId() ) );
				BuffRequestFrame.ONLINE_VALIDATOR.addFormField( "pwd" );
				BuffRequestFrame.ONLINE_VALIDATOR.addFormField( "graf", "/whois " + names[i] );

				RequestThread.postRequest( BuffRequestFrame.ONLINE_VALIDATOR );
				Matcher idMatcher = Pattern.compile( "\\(#(\\d+)\\)" ).matcher( BuffRequestFrame.ONLINE_VALIDATOR.responseText );

				if ( idMatcher.find() )
					registerPlayer( names[i], idMatcher.group(1) );
				else
					names[i] = null;
			}

			for ( int i = 0; i < names.length; ++i )
			{
				if ( names[i] == null )
					continue;

				updateDisplay( "Retrieving player data for " + names[i] + "..." );
				targets[i] = new ProfileRequest( names[i] );
				targets[i].run();
			}

			updateDisplay( "Determining current rank..." );
			RequestThread.postRequest( new FlowerHunterRequest() );

			executeFlowerHuntRequest( targets, new FlowerHunterRequest( parameters, stance, KoLCharacter.canInteract() ? "dignity" : "flowers", "", "" ) );

			RequestThread.closeRequestSequence();
			return;
		}

		if ( command.equals( "flowers" ) )
		{
			this.executeFlowerHuntRequest();
			return;
		}

		if ( command.startsWith( "pvplog" ) )
		{
			this.summarizeFlowerHunterData();
			return;
		}

		if ( command.equals( "hermit" ) )
		{
			this.executeHermitRequest( parameters );
			return;
		}

		if ( command.equals( "styx" ) )
		{
			this.executeStyxRequest( parameters );
			return;
		}

		if ( command.equals( "concert" ) )
		{
			this.executeArenaRequest( parameters );
			return;
		}

		if ( command.equals( "friars" ) )
		{
			this.executeFriarRequest( parameters );
			return;
		}

		if ( command.equals( "mpitems" ) )
		{
			int restores = getRestoreCount();
			RequestLogger.printLine( restores + " mana restores remaining." );
			return;
		}

		if ( command.startsWith( "restore" ) || command.startsWith( "recover" ) || command.startsWith( "check" ) )
		{
			boolean wasRecoveryActive = KoLmafia.isRunningBetweenBattleChecks();
			SpecialOutfit.createImplicitCheckpoint();

			if ( parameters.equalsIgnoreCase( "hp" ) || parameters.equalsIgnoreCase( "health" ) )
			{
				recoveryActive = true;
				StaticEntity.getClient().recoverHP( KoLCharacter.getCurrentHP() + 1 );
				recoveryActive = wasRecoveryActive;
			}
			else if ( parameters.equalsIgnoreCase( "mp" ) || parameters.equalsIgnoreCase( "mana" ) )
			{
				recoveryActive = true;
				StaticEntity.getClient().recoverMP( KoLCharacter.getCurrentMP() + 1 );
				recoveryActive = wasRecoveryActive;
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
				MoodSettings.maximalSet();
				MoodSettings.saveSettings();
			}

			String [] split = parameters.split( "\\s*,\\s*" );
			if ( split.length == 3 )
			{
				MoodSettings.addTrigger( split[0], split[1], split.length == 2 ? MoodSettings.getDefaultAction( split[0], split[1] ) : split[2] );
				MoodSettings.saveSettings();
			}
			else if ( split.length == 2 )
			{
				MoodSettings.addTrigger( split[0], split[1], MoodSettings.getDefaultAction( split[0], split[1] ) );
				MoodSettings.saveSettings();
			}
			else if ( split.length == 1 )
			{
				MoodSettings.addTrigger( "lose_effect", split[0], MoodSettings.getDefaultAction( "lose_effect", split[0] ) );
				MoodSettings.saveSettings();
			}

			printList( MoodSettings.getTriggers() );
			return;
		}

		if ( command.startsWith( "mood" ) )
		{
			parameters = parameters.toLowerCase();

			if ( parameters.equals( "clear" ) )
			{
				MoodSettings.removeTriggers( MoodSettings.getTriggers().toArray() );
				MoodSettings.saveSettings();
			}
			else if ( parameters.equals( "autofill" ) )
			{
				MoodSettings.maximalSet();
				MoodSettings.saveSettings();
				printList( MoodSettings.getTriggers() );
			}
			else if ( parameters.equals( "execute" ) )
			{
				if ( isRunningBetweenBattleChecks() )
					return;

				SpecialOutfit.createImplicitCheckpoint();
				MoodSettings.execute( 0 );
				SpecialOutfit.restoreImplicitCheckpoint();
				RequestLogger.printLine( "Mood swing complete." );
			}
			else if ( parameters.startsWith( "repeat" ) )
			{
				if ( isRunningBetweenBattleChecks() )
					return;

				int multiplicity = 0;
				int spaceIndex = parameters.lastIndexOf( " " );
				if ( spaceIndex != -1 )
					multiplicity = StaticEntity.parseInt( parameters.substring( spaceIndex + 1 ) );

				SpecialOutfit.createImplicitCheckpoint();
				MoodSettings.execute( multiplicity );
				SpecialOutfit.restoreImplicitCheckpoint();
				RequestLogger.printLine( "Mood swing complete." );
			}
			else
			{
				int multiplicity = 0;
				int spaceIndex = parameters.lastIndexOf( " " );
				if ( spaceIndex != -1 )
				{
					multiplicity = StaticEntity.parseInt( parameters.substring( spaceIndex + 1 ) );
					parameters = parameters.substring( 0, spaceIndex );
				}

				String previousMood = KoLSettings.getUserProperty( "currentMood" );
				MoodSettings.setMood( parameters );

				executeCommand( "mood", "repeat " + multiplicity );

				if ( multiplicity > 0 )
					MoodSettings.setMood( previousMood );
			}

			return;
		}

		if ( command.equals( "restaurant" ) )
		{
			this.makeRestaurantRequest( parameters );
			return;
		}

		if ( command.indexOf( "brewery" ) != -1 )
		{
			this.makeMicrobreweryRequest( parameters );
			return;
		}

		if ( command.indexOf( "kitchen" ) != -1 )
		{
			this.makeKitchenRequest( parameters );
			return;
		}

		// Campground commands, like relaxing at the beanbag, or
		// resting at your house/tent.

		if ( command.equals( "rest" ) || command.equals( "relax" ) )
		{
			this.executeCampgroundRequest( command + " " + parameters );
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
				RequestLogger.printLine( "Send request \"" + parameters + "\" ignored in between-battle execution." );
				return;
			}

			this.executeSendRequest( parameters, false );
			return;
		}

		if ( command.equals( "csend" ) )
		{
			if ( isRunningBetweenBattleChecks() )
			{
				RequestLogger.printLine( "Send request \"" + parameters + "\" ignored in between-battle execution." );
				return;
			}

			this.executeSendRequest( parameters, true );
			return;
		}

		// Finally, handle command abbreviations - in
		// other words, commands that look like they're
		// their own commands, but are actually commands
		// which are derived from other commands.

		if ( command.startsWith( "skill" ) )
			command = "skills";

		if ( command.startsWith( "cast" ) || command.startsWith( "buff" ) || command.startsWith( "pass" ) || command.startsWith( "self" ) || command.startsWith( "combat" ) )
		{
			parameters = command;
			command = "skills";
		}

		if ( command.startsWith( "inv" ) || command.equals( "closet" ) || command.equals( "storage" ) || command.equals( "session" ) || command.equals( "summary" ) ||
			command.equals( "effects" ) || command.equals( "status" ) || command.equals( "skills" ) || command.equals( "locations" ) || command.equals( "encounters" ) || command.equals( "counters" ) || command.startsWith( "moon" ) )
		{
			this.showData( command + " " + parameters );
			return;
		}

		// If someone wants to add a new adventure on
		// the fly, and it's a valid URL (ie: not a
		// send or search URL), go right ahead.

		if ( command.equals( "location" ) )
		{
			int spaceIndex = parameters.indexOf( " " );
			if ( spaceIndex == -1 )
				return;

			KoLAdventure adventure = new KoLAdventure( "Holiday",
				"adventure.php", parameters.substring( 0, spaceIndex ), parameters.substring( spaceIndex ).trim() );

			AdventureDatabase.addAdventure( adventure );
			return;
		}

		if ( ALIASES.containsKey( command ) )
		{
			String aliasLine = (String) ALIASES.get( command );
			if ( aliasLine.indexOf( "%%" ) != -1 )
				aliasLine = StaticEntity.singleStringReplace( aliasLine, "%%", parameters );
			else if ( aliasLine.endsWith( "=" ) )
				aliasLine += parameters;
			else
				aliasLine += " " + parameters;

			this.executeLine( aliasLine );
			return;
		}

		// If all else fails, then assume that the
		// person was trying to call a script.

		this.executeScript( this.currentLine );
	}

	public void showHTML( String location, String text )
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

		RequestLogger.printLine( displayText.trim() );
	}

	private void summarizeFlowerHunterData()
	{
		FlowerHunterRequest.processDefenseContests();

		if ( !ATTACKS_LOCATION.exists() )
			return;

		File [] attackLogs = ATTACKS_LOCATION.listFiles();

		TreeMap minis = new TreeMap();
		updateDisplay( "Scanning attack logs..." );

		for ( int i = 0; i < attackLogs.length; ++i )
		{
			if ( !attackLogs[i].getName().endsWith( "__spreadsheet.txt" ) )
				this.registerFlowerHunterData( minis, KoLDatabase.getReader( attackLogs[i] ) );
		}

		LogStream spreadsheet = LogStream.openStream( new File( ATTACKS_LOCATION, "__spreadsheet.txt" ), true );

		spreadsheet.println( "Name\tTattoos\t\tTrophies\t\tFlowers\t\tCanadians" );
		spreadsheet.println( "\tLow\tHigh\tLow\tHigh\tLow\tHigh\tLow\tHigh" );

		Iterator keys = minis.keySet().iterator();

		while ( keys.hasNext() )
		{
			Object key = keys.next();
			Object [] value = (Object []) minis.get( key );

			boolean shouldPrint = false;
			for ( int i = 0; i < value.length; i += 2 )
				shouldPrint |= value[i] != null;

			if ( !shouldPrint )
				continue;

			spreadsheet.print( key );

			for ( int i = 0; i < value.length; i += 2 )
			{
				spreadsheet.print( "\t" );
				spreadsheet.print( value[i] == null ? "" : value[i] );
			}

			spreadsheet.println();
		}

		spreadsheet.close();
		updateDisplay( "Spreadsheet generated." );

		RequestThread.enableDisplayIfSequenceComplete();
	}

	private void registerFlowerHunterData( TreeMap minis, BufferedReader attackLog )
	{
		String line;
		while ( (line = KoLDatabase.readLine( attackLog )) != null )
		{
			// First, try to figure out whose data is being registered in
			// this giant spreadsheet.

			Matcher versusMatcher = FlowerHunterRequest.VERSUS_PATTERN.matcher( line );
			if ( !versusMatcher.find() )
			{
				line = KoLDatabase.readLine( attackLog );
				versusMatcher = FlowerHunterRequest.VERSUS_PATTERN.matcher( line );

				if ( !versusMatcher.find() )
					return;
			}

			String opponent = versusMatcher.group(2).equals( "you" ) ? versusMatcher.group(1) : versusMatcher.group(2);

			line = KoLDatabase.readLine( attackLog );
			Matcher minisMatcher = FlowerHunterRequest.MINIS_PATTERN.matcher( line );

			if ( !minisMatcher.find() )
				return;

			// Next, make sure that you have all the information needed to
			// generate a row in the spreadsheet.

			Integer [] yourData = new Integer[4];
			for ( int i = 0; i < yourData.length; ++i )
				yourData[i] = Integer.valueOf( minisMatcher.group(i + 1) );

			if ( !minis.containsKey( opponent ) )
				minis.put( opponent, new Object[16] );

			// There are seven minis to handle.  You can discard the first
			// three because they're attack minis.

			KoLDatabase.readLine( attackLog );
			KoLDatabase.readLine( attackLog );
			KoLDatabase.readLine( attackLog );

			Object [] theirData = (Object []) minis.get( opponent );

			this.registerFlowerContestData( yourData, theirData, KoLDatabase.readLine( attackLog ) );
			this.registerFlowerContestData( yourData, theirData, KoLDatabase.readLine( attackLog ) );
			this.registerFlowerContestData( yourData, theirData, KoLDatabase.readLine( attackLog ) );
			this.registerFlowerContestData( yourData, theirData, KoLDatabase.readLine( attackLog ) );

			// With all that information registered, go ahead and store the
			// attack information back into the tree map.

			minis.put( opponent, theirData );
		}
	}

	private void registerFlowerContestData( Integer [] yourData, Object [] theirData, String currentAttack )
	{
		int baseIndex = -1;
		boolean wonContest = currentAttack.endsWith( "You won." );

		if ( currentAttack.startsWith( "Tattoo Contest" ) )
			baseIndex = 0;

		if ( currentAttack.startsWith( "Trophy Contest" ) )
			baseIndex = 1;

		if ( currentAttack.startsWith( "Flower Picking Contest" ) )
			baseIndex = 2;

		if ( currentAttack.startsWith( "Canadianity Contest" ) )
			baseIndex = 3;

		if ( baseIndex < 0 )
			return;

		if ( wonContest )
		{
			if ( theirData[4 * baseIndex] == null || yourData[baseIndex].intValue() < ((Integer)theirData[4 * baseIndex]).intValue() )
			{
				theirData[4 * baseIndex] = yourData[baseIndex];
			}
		}
		else
		{
			if ( theirData[4 * baseIndex + 2] == null || yourData[baseIndex].intValue() > ((Integer)theirData[4 * baseIndex + 2]).intValue() )
			{
				theirData[4 * baseIndex + 2] = yourData[baseIndex];
			}
		}
	}

	private void executeFlowerHuntRequest()
	{
		RequestThread.openRequestSequence();

		updateDisplay( "Determining current rank..." );
		RequestThread.postRequest( new FlowerHunterRequest() );

		int fightsLeft = 0;
		int stance = 0;

		if ( KoLCharacter.getBaseMuscle() >= KoLCharacter.getBaseMysticality() && KoLCharacter.getBaseMuscle() >= KoLCharacter.getBaseMoxie() )
			stance = 1;
		else if ( KoLCharacter.getBaseMysticality() >= KoLCharacter.getBaseMuscle() && KoLCharacter.getBaseMysticality() >= KoLCharacter.getBaseMoxie() )
			stance = 2;
		else
			stance = 3;

		int lastSearch = 0, desiredRank;

		ProfileRequest [] results = null;
		FlowerHunterRequest request = new FlowerHunterRequest( "", stance, "flowers", "", "" );

		while ( !KoLmafia.refusesContinue() && fightsLeft != KoLCharacter.getAttacksLeft() && KoLCharacter.getAttacksLeft() > 0 )
		{
			fightsLeft = KoLCharacter.getAttacksLeft();
			desiredRank = Math.max( 10, KoLCharacter.getPvpRank() - 50 + Math.min( 11, fightsLeft ) );

			if ( lastSearch != desiredRank )
			{
				updateDisplay( "Determining targets at rank " + desiredRank + "..." );
				FlowerHunterRequest search = new FlowerHunterRequest( "", String.valueOf( desiredRank ) );
				RequestThread.postRequest( search );

				lastSearch = desiredRank;
				results = new ProfileRequest[ FlowerHunterRequest.getSearchResults().size() ];
				FlowerHunterRequest.getSearchResults().toArray( results );
			}

			executeFlowerHuntRequest( results, request );

			if ( !KoLmafia.refusesContinue() )
				KoLmafia.forceContinue();
		}

		if ( KoLmafia.permitsContinue() )
			updateDisplay( "You have " + KoLCharacter.getAttacksLeft() + " attacks remaining." );

		RequestThread.closeRequestSequence();
	}

	public static final void executeFlowerHuntRequest( ProfileRequest [] targets, FlowerHunterRequest request )
	{
		for ( int i = 0; i < targets.length && KoLmafia.permitsContinue() && KoLCharacter.getAttacksLeft() > 0; ++i )
		{
			if ( targets[i] == null )
				continue;

			if ( KoLCharacter.getPvpRank() - 50 > targets[i].getPvpRank().intValue() )
				continue;

			if ( KoLSettings.getUserProperty( "currentPvpVictories" ).indexOf( targets[i].getPlayerName() ) != -1 )
				continue;

			if ( targets[i].getPlayerName().toLowerCase().startsWith( "devster" ) )
				continue;

			if ( ClanManager.getClanName().equals( targets[i].getClanName() ) )
				continue;

			KoLmafia.updateDisplay( "Attacking " + targets[i].getPlayerName() + "..." );
			request.setTarget( targets[i].getPlayerName() );
			RequestThread.postRequest( request );

			if ( request.responseText.indexOf( "Your PvP Ranking increased by" ) != -1 )
				KoLSettings.setUserProperty( "currentPvpVictories", KoLSettings.getUserProperty( "currentPvpVictories" ) + targets[i].getPlayerName() + "," );
			else
				updateDisplay( ERROR_STATE, "You lost to " + targets[i].getPlayerName() + "." );
		}
	}

	private void executeSendRequest( String parameters, boolean isConvertible )
	{
		String [] splitParameters = parameters.replaceFirst( " [tT][oO] ", " => " ).split( " => " );

		if ( splitParameters.length != 2 )
		{
			updateDisplay( ERROR_STATE, "Invalid send request." );
			return;
		}

		String message = DEFAULT_KMAIL;

		int separatorIndex = splitParameters[1].indexOf( "||" );
		if ( separatorIndex != -1 )
		{
			message = splitParameters[1].substring( separatorIndex + 2 ).trim();
			splitParameters[1] = splitParameters[1].substring( 0, separatorIndex );
		}

		splitParameters[0] = splitParameters[0].trim();
		splitParameters[1] = splitParameters[1].trim();

		Object [] attachments = this.getMatchingItemList( splitParameters[0] );
		if ( attachments.length == 0 )
			return;

		// Validate their attachments.  If they happen to be
		// scripting a philanthropic buff request, then figure
		// out if there's a corresponding full-price buff.

		if ( !isConvertible )
		{
			int attachmentCount = attachments.length;
			for ( int i = 0; i < attachments.length; ++i )
			{
				if ( ((AdventureResult)attachments[0]).getName().equals( AdventureResult.MEAT ) )
				{
					--attachmentCount;
					attachments[i] = null;
				}
			}

			if ( attachmentCount == 0 )
				return;
		}
		else
		{
			int amount = BuffBotDatabase.getOffering( splitParameters[1],
				((AdventureResult)attachments[0]).getCount() );

			if ( amount == 0 )
				return;

			attachments[0] = new AdventureResult( AdventureResult.MEAT, amount );
		}

		this.executeSendRequest( splitParameters[1], message, attachments, false, true );
	}

	public void executeSendRequest( String recipient, String message, Object [] attachments, boolean usingStorage, boolean isInternal )
	{
		if ( !usingStorage )
		{
			SendMessageRequest.setUpdateDisplayOnFailure( false );
			RequestThread.postRequest( new GreenMessageRequest( recipient, message, attachments, isInternal ) );
			SendMessageRequest.setUpdateDisplayOnFailure( true );

			if ( !SendMessageRequest.hadSendMessageFailure() )
			{
				updateDisplay( "Message sent to " + recipient );
				return;
			}
		}

		List availablePackages = GiftMessageRequest.getPackages();
		int desiredPackageIndex = Math.min( Math.min( availablePackages.size() - 1, attachments.length ), 5 );

		if ( MoonPhaseDatabase.getHoliday( new Date() ).startsWith( "Valentine's" ) )
			desiredPackageIndex = 0;

		// Clear the error state for continuation on the
		// message sending attempt.

		if ( !refusesContinue() )
			forceContinue();

		RequestThread.postRequest( new GiftMessageRequest( recipient, message, desiredPackageIndex, attachments, usingStorage ) );

		if ( permitsContinue() )
			updateDisplay( "Gift sent to " + recipient );
		else
			updateDisplay( ERROR_STATE, "Failed to send message to " + recipient );
	}

	public static final File findScriptFile( String filename )
	{
		File scriptFile = new File( filename );
		if ( scriptFile.exists() )
			return scriptFile.isDirectory() ? null : scriptFile;

		scriptFile = new File( ROOT_LOCATION, filename );
		if ( scriptFile.exists() )
			return scriptFile.isDirectory() ? null : scriptFile;

		if ( SCRIPT_LOCATION.exists() )
		{
			scriptFile = findScriptFile( SCRIPT_LOCATION, filename, false );
			if ( scriptFile != null )
				return scriptFile.isDirectory() ? null : scriptFile;
		}

		if ( PLOTS_LOCATION.exists() )
		{
			scriptFile = new File( PLOTS_LOCATION, filename );
			if ( scriptFile.exists() )
				return scriptFile.isDirectory() ? null : scriptFile;
		}

		if ( RELAY_LOCATION.exists() )
		{
			scriptFile = findScriptFile( RELAY_LOCATION, filename, false );
			if ( scriptFile != null )
				return scriptFile.isDirectory() ? null : scriptFile;
		}

		return null;
	}

	public static final File findScriptFile( File directory, String filename )
	{	return findScriptFile( directory, filename, false );
	}

	private static final File findScriptFile( File directory, String filename, boolean isFallback )
	{
		File scriptFile = new File( directory, filename );

		if ( scriptFile.exists() )
			return scriptFile;

		if ( !isFallback )
		{
			scriptFile = findScriptFile( directory, filename + ".cli", true );
			if ( scriptFile != null )
				return scriptFile;

			scriptFile = findScriptFile( directory, filename + ".txt", true );
			if ( scriptFile != null )
				return scriptFile;

			scriptFile = findScriptFile( directory, filename + ".ash", true );
			if ( scriptFile != null )
				return scriptFile;
		}

		File [] contents = directory.listFiles();
		for ( int i = 0; i < contents.length; ++i )
		{
			if ( contents[i].isDirectory() )
			{
				scriptFile = findScriptFile( contents[i], filename, false );
				if ( scriptFile != null )
					return scriptFile;
			}
		}

		return null;
	}

	public void executeScript( String script )
	{	this.executeScriptCommand( "call", script );
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

			// Maybe the more ambiguous invocation of an ASH script which does
			// not use parentheses?

			if ( scriptFile == null )
			{
				int spaceIndex = parameters.indexOf( " " );
				if ( spaceIndex != -1 && arguments == null )
				{
					arguments = new String [] { parameters.substring( spaceIndex + 1 ).trim() };
					parameters = parameters.substring( 0, spaceIndex );
					scriptFile = findScriptFile( parameters );
				}
			}

			// If not even that, perhaps it's the invocation of a function which
			// is defined in the ASH namespace?

			if ( scriptFile == null )
			{
				KoLmafiaASH.NAMESPACE_INTERPRETER.execute( parameters, arguments );
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

				if ( command.equals( "validate" ) || command.equals( "verify" ) || command.equals( "check" ) )
				{
					KoLmafiaASH interpreter = KoLmafiaASH.getInterpreter( scriptFile );
					if ( interpreter != null )
					{
						RequestLogger.printLine();
						interpreter.showUserFunctions( "" );

						RequestLogger.printLine();
						RequestLogger.printLine( "Script verification complete." );
					}

					return;
				}

				// If there's an alternate namespace being
				// used, then be sure to switch.

				KoLmafiaASH interpreter = KoLmafiaASH.getInterpreter( scriptFile );
				if ( interpreter != null )
					for ( int i = 0; i < runCount && permitsContinue(); ++i )
						interpreter.execute( "main", arguments );
			}
			else
			{
				if ( arguments != null )
				{
					updateDisplay( ERROR_STATE, "You can only specify arguments for an ASH script" );
					return;
				}

				for ( int i = 0; i < runCount && permitsContinue(); ++i )
					(new KoLmafiaCLI( new FileInputStream( scriptFile ) )).listenForCommands();
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
	 * Utility method which tests if the given condition is true.
	 * Note that this only examines level, health, mana, items,
	 * meat and status effects.
	 */

	public static final boolean testConditional( String parameters )
	{
		if ( !permitsContinue() )
			return false;

		// Allow checking for moon signs for stat days
		// only.  Allow test for today and tomorrow.

		Matcher dayMatcher = STATDAY_PATTERN.matcher( parameters );
		if ( dayMatcher.find() )
		{
			String statDayToday = MoonPhaseDatabase.getMoonEffect().toLowerCase();
			String statDayTest = dayMatcher.group(2).substring( 0, 3 ).toLowerCase();

			return statDayToday.indexOf( statDayTest ) != -1 && statDayToday.indexOf( "bonus" ) != -1 &&
				statDayToday.indexOf( "not " + dayMatcher.group(1) ) == -1;
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

	private static final int lvalue( String left )
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

		if ( left.equals( "muscle" ) )
			return KoLCharacter.getBaseMuscle();

		if ( left.equals( "mysticality" ) )
			return KoLCharacter.getBaseMysticality();

		if ( left.equals( "moxie" ) )
			return KoLCharacter.getBaseMoxie();

		if ( left.equals( "worthless item" ) )
			return HermitRequest.getWorthlessItemCount();

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

	private static final int rvalue( String left, String right )
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

				if ( i == 0 && right.charAt(0) == '-' )
					continue;

				updateDisplay( ERROR_STATE, "Invalid operand [" + right + "] on right side of operator" );
			}
		}

		// If it gets this far, then it must be numeric,
		// so parse the number and return it.

		return StaticEntity.parseInt( right );
	}

	private static final AdventureResult effectParameter( String parameter )
	{
		List potentialEffects = StatusEffectDatabase.getMatchingNames( parameter );
		if ( potentialEffects.isEmpty() )
			return null;

		return new AdventureResult( (String) potentialEffects.get(0), 0, true );
	}

	private static final AdventureResult itemParameter( String parameter )
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
			RequestLogger.printLine( "Conditions list cleared." );
			return true;
		}
		else if ( option.equals( "check" ) )
		{
			checkRequirements( conditions );
			RequestLogger.printLine( "Conditions list validated against available items." );
			return true;
		}
		else if ( option.equals( "add" ) || option.equals( "set" ) )
		{
			AdventureResult condition;
			String [] conditionsList = parameters.substring( option.length() ).toLowerCase().trim().split( "\\s*,\\s*" );

			for ( int i = 0; i < conditionsList.length; ++i )
			{
				if ( conditionsList[i].equalsIgnoreCase( "castle map items" ) )
				{
					addItemCondition( "set", new AdventureResult( 616, 1 ) );  // furry fur
					addItemCondition( "set", new AdventureResult( 619, 1 ) );  // giant needle
					addItemCondition( "set", new AdventureResult( 622, 1 ) );  // awful poetry journal
				}
				else
				{
					condition = this.extractCondition( conditionsList[i] );
					if ( condition != null )
						addItemCondition( option, condition );
				}
			}

			return true;
		}

		printList( conditions );
		return false;
	}

	private void addItemCondition( String option, AdventureResult condition )
	{
		if ( condition.isItem() && option.equals( "set" ) )
		{
			int currentAmount = condition.getItemId() == HermitRequest.WORTHLESS_ITEM.getItemId() ? HermitRequest.getWorthlessItemCount() :
				condition.getCount( inventory ) + condition.getCount( closet );

			for ( int j = 0; j < KoLCharacter.FAMILIAR; ++j )
				if ( KoLCharacter.getEquipment( j ).equals( condition ) )
					++currentAmount;

			if ( condition.getCount( conditions ) >= condition.getCount() )
			{
				RequestLogger.printLine( "Condition already exists: " + condition );
			}
			else if ( currentAmount >= condition.getCount() )
			{
				RequestLogger.printLine( "Condition already met: " + condition );
			}
			else
			{
				AdventureResult.addResultToList( conditions, condition.getInstance( condition.getCount() - currentAmount ) );
				RequestLogger.printLine( "Condition set: " + condition );
			}
		}
		else if ( condition.getCount() > 0 )
		{
			AdventureResult.addResultToList( conditions, condition );
			RequestLogger.printLine( "Condition added: " + condition );
		}
		else
		{
			RequestLogger.printLine( "Condition already met: " + condition );
		}
	}

	private AdventureResult extractCondition( String conditionString )
	{
		if ( conditionString.length() == 0 )
			return null;

		conditionString = conditionString.toLowerCase();

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

			int primeIndex = KoLCharacter.getPrimeIndex();

			AdventureResult.CONDITION_SUBSTATS[ primeIndex ] = KoLCharacter.calculateSubpoints( (level - 1) * (level - 1) + 4, 0 ) -
				KoLCharacter.getTotalPrime();

			condition = AdventureResult.CONDITION_SUBSTATS_RESULT;
		}
		else if ( conditionString.endsWith( "mus" ) || conditionString.endsWith( "muscle" ) || conditionString.endsWith( "moxie" ) ||
			conditionString.endsWith( "mys" ) || conditionString.endsWith( "myst" ) || conditionString.endsWith( "mox" ) || conditionString.endsWith( "mysticality" ) )
		{
			String [] splitCondition = conditionString.split( "\\s+" );

			int points = StaticEntity.parseInt( splitCondition[0] );
			int statIndex = conditionString.indexOf( "mus" ) != -1 ? 0 : conditionString.indexOf( "mys" ) != -1 ? 1 : 2;

			AdventureResult.CONDITION_SUBSTATS[ statIndex ] = KoLCharacter.calculateSubpoints( points, 0 );
			AdventureResult.CONDITION_SUBSTATS[ statIndex ] = Math.max( 0, AdventureResult.CONDITION_SUBSTATS[ statIndex ] -
				(conditionString.indexOf( "mus" ) != -1 ? KoLCharacter.getTotalMuscle() :
				conditionString.indexOf( "mys" ) != -1 ? KoLCharacter.getTotalMysticality() : KoLCharacter.getTotalMoxie()) );

			condition = AdventureResult.CONDITION_SUBSTATS_RESULT;
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
				outfitLocation = KoLSettings.getUserProperty("lastAdventure");
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
		String [] buffs = parameters.split( "\\s*,\\s*" );

		for ( int i = 0; i < buffs.length; ++i )
		{
			String [] splitParameters = buffs[i].replaceFirst( " [oO][nN] ", " => " ).split( " => " );

			if ( splitParameters.length == 1 )
			{
				splitParameters = new String[2];
				splitParameters[0] = buffs[i];
				splitParameters[1] = null;
			}

			String [] buffParameters = this.splitCountAndName( splitParameters[0] );
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
				buffCount = 0;
			else if ( buffCountString != null )
				buffCount = StaticEntity.parseInt( buffCountString );

			if ( isExecutingCheckOnlyCommand )
			{
				RequestLogger.printLine( skillName + " (x" + buffCount + ")" );
				return;
			}

			RequestThread.postRequest( UseSkillRequest.getInstance( skillName, splitParameters[1], buffCount ) );
		}
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

	public static final String getSkillName( String substring, List list )
	{
		UseSkillRequest [] skills = new UseSkillRequest[ list.size() ];
		list.toArray( skills );

		String name = substring.toLowerCase();

		int skillIndex = -1;
		int substringIndex = Integer.MAX_VALUE;

		int currentIndex;

		for ( int i = 0; i < skills.length; ++i )
		{
			String skill = skills[i].getSkillName();
			currentIndex = skill.toLowerCase().indexOf( name );

			if ( currentIndex != -1 && currentIndex < substringIndex )
			{
				skillIndex = i;
				substringIndex = currentIndex;
			}
		}

		return skillIndex == -1 ? null : skills[ skillIndex ].getSkillName();
	}

	/**
	 * Utility method used to retrieve the full name of a skill,
	 * given a substring representing it.
	 */

	public static final String getSkillName( String substring )
	{	return getSkillName( substring, availableSkills );
	}

	/**
	 * Utility method used to retrieve the full name of a combat skill,
	 * given a substring representing it.
	 */

	public static final String getUsableSkillName( String substring )
	{	return getSkillName( substring, usableSkills );
	}

	/**
	 * Utility method used to retrieve the full name of a combat skill,
	 * given a substring representing it.
	 */

	public static final String getCombatSkillName( String substring )
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
			this.showData( "equipment" );
			return;
		}

		if ( parameters.startsWith( "list" ) )
		{
			this.showData( "equipment " + parameters.substring(4).trim() );
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
			return;

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

		// If the new weapon type doesn't match the offhand weapon,
		// unequip the off-hand weapon.

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

	private void showData( String parameters )
	{	this.showData( parameters, false );
	}

	/**
	 * A special module used specifically for properly printing out
	 * data relevant to the current session.
	 */

	private void showData( String parameters, boolean sessionPrint )
	{
		if ( parameters.length() == 0 )
		{
			updateDisplay( ERROR_STATE, "Print what?" );
			return;
		}

		parameters = parameters.trim();
		int spaceIndex = parameters.indexOf( " " );

		String list = spaceIndex == -1 ? parameters : parameters.substring( 0, spaceIndex ).trim();
		String filter = spaceIndex == -1 ? "" : KoLDatabase.getCanonicalName( parameters.substring( spaceIndex ).trim() );

		PrintStream desiredOutputStream = sessionPrint ? RequestLogger.getSessionStream() : RequestLogger.INSTANCE;

		if ( !filter.equals( "" ) &&
			(parameters.startsWith( "summary" ) || parameters.startsWith( "session" ) || parameters.equals( "status" ) || parameters.startsWith( "equip" ) || parameters.startsWith( "encounters" ) || parameters.startsWith( "locations" ) ) )
		{
			desiredOutputStream = LogStream.openStream( new File( ROOT_LOCATION, filter ), false );
		}

		this.showData( list, filter, desiredOutputStream );

		if ( sessionPrint && RequestLogger.isDebugging() )
			showData( list, filter, RequestLogger.getDebugStream() );

		if ( !sessionPrint )
			desiredOutputStream.close();
	}

	/**
	 * A special module used specifically for properly printing out
	 * data relevant to the current session.  This method is more
	 * specialized than its counterpart and is used when the data
	 * to be printed is known, as well as the stream to print to.
	 * Usually called by its counterpart to handle specific instances.
	 */

	private void showData( String desiredData, String filter, PrintStream desiredStream )
	{
		desiredStream.println();

		if ( desiredData.startsWith( "moon" ) )
		{
			Date today = new Date();

			try
			{
				today = DAILY_FORMAT.parse( DAILY_FORMAT.format( today ) );
			}
			catch ( Exception e )
			{
				// This should not happen.  Therefore, print
				// a stack trace for debug purposes.

				StaticEntity.printStackTrace( e );
			}

			desiredStream.println( CalendarFrame.LONG_FORMAT.format( today ) + " - " + MoonPhaseDatabase.getCalendarDayAsString( today ) );
			desiredStream.println();

			desiredStream.println( "Ronald: " + MoonPhaseDatabase.getRonaldPhaseAsString() );
			desiredStream.println( "Grimace: " + MoonPhaseDatabase.getGrimacePhaseAsString() );
			desiredStream.println();

			String [] holidayPredictions = MoonPhaseDatabase.getHolidayPredictions( today );
			for ( int i = 0; i < holidayPredictions.length; ++i )
				desiredStream.println( holidayPredictions[i] );

			desiredStream.println();
			desiredStream.println( MoonPhaseDatabase.getHoliday( today ) );
			desiredStream.println( MoonPhaseDatabase.getMoonEffect() );
			desiredStream.println();
		}
		else if ( desiredData.equals( "session" ) )
		{
			desiredStream.println( "Player: " + KoLCharacter.getUserName() );
			desiredStream.println( "Session Id: " + KoLRequest.serverCookie );
			desiredStream.println( "Password Hash: " + KoLRequest.passwordHash );
			desiredStream.println( "Current Server: " + KoLRequest.KOL_HOST );
			desiredStream.println();
		}
		else if ( desiredData.equals( "status" ) )
		{
			desiredStream.println( "Name: " + KoLCharacter.getUserName() );
			desiredStream.println( "Class: " + KoLCharacter.getClassType() );
			desiredStream.println();

			desiredStream.println( "Lv: " + KoLCharacter.getLevel() );
			desiredStream.println( "HP: " + KoLCharacter.getCurrentHP() + " / " + COMMA_FORMAT.format( KoLCharacter.getMaximumHP() ) );
			desiredStream.println( "MP: " + KoLCharacter.getCurrentMP() + " / " + COMMA_FORMAT.format( KoLCharacter.getMaximumMP() ) );

			desiredStream.println();

			desiredStream.println( "Mus: " + getStatString( KoLCharacter.getBaseMuscle(), KoLCharacter.getAdjustedMuscle(), KoLCharacter.getMuscleTNP() ) );
			desiredStream.println( "Mys: " + getStatString( KoLCharacter.getBaseMysticality(), KoLCharacter.getAdjustedMysticality(), KoLCharacter.getMysticalityTNP() ) );
			desiredStream.println( "Mox: " + getStatString( KoLCharacter.getBaseMoxie(), KoLCharacter.getAdjustedMoxie(), KoLCharacter.getMoxieTNP() ) );

			desiredStream.println();

			desiredStream.println( "Advs: " + KoLCharacter.getAdventuresLeft() );
			desiredStream.println( "Meat: " + COMMA_FORMAT.format( KoLCharacter.getAvailableMeat() ) );
			desiredStream.println( "Drunk: " + KoLCharacter.getInebriety() );

			desiredStream.println();
		}
		else if ( desiredData.equals( "modifiers" ) )
		{
			desiredStream.println( "ML: " + MODIFIER_FORMAT.format( KoLCharacter.getMonsterLevelAdjustment() ) );
			desiredStream.println( "Enc: " + ROUNDED_MODIFIER_FORMAT.format( KoLCharacter.getCombatRateAdjustment() ) + "%" );
			desiredStream.println( "Init: " + ROUNDED_MODIFIER_FORMAT.format( KoLCharacter.getInitiativeAdjustment() ) + "%" );

			desiredStream.println();

			desiredStream.println( "Exp: " + ROUNDED_MODIFIER_FORMAT.format( KoLCharacter.getExperienceAdjustment() ) );
			desiredStream.println( "Meat: " + ROUNDED_MODIFIER_FORMAT.format( KoLCharacter.getMeatDropPercentAdjustment() ) + "%" );
			desiredStream.println( "Item: " + ROUNDED_MODIFIER_FORMAT.format( KoLCharacter.getItemDropPercentAdjustment() ) + "%" );

			desiredStream.println();
		}
		else if ( desiredData.startsWith( "equip" ) )
		{
			desiredStream.println( "Hat: " + KoLCharacter.getEquipment( KoLCharacter.HAT ) );
			desiredStream.println( "Weapon: " + KoLCharacter.getEquipment( KoLCharacter.WEAPON ) );

			if ( KoLCharacter.getFakeHands() > 0 )
				desiredStream.println( "Fake Hands: " + KoLCharacter.getFakeHands() );

			desiredStream.println( "Off-hand: " + KoLCharacter.getEquipment( KoLCharacter.OFFHAND ) );
			desiredStream.println( "Shirt: " + KoLCharacter.getEquipment( KoLCharacter.SHIRT ) );
			desiredStream.println( "Pants: " + KoLCharacter.getEquipment( KoLCharacter.PANTS ) );

			desiredStream.println();

			desiredStream.println( "Acc. 1: " + KoLCharacter.getEquipment( KoLCharacter.ACCESSORY1 ) );
			desiredStream.println( "Acc. 2: " + KoLCharacter.getEquipment( KoLCharacter.ACCESSORY2 ) );
			desiredStream.println( "Acc. 3: " + KoLCharacter.getEquipment( KoLCharacter.ACCESSORY3 ) );

			desiredStream.println();

			desiredStream.println( "Pet: " + KoLCharacter.getFamiliar() );
			desiredStream.println( "Item: " + KoLCharacter.getFamiliarItem() );
		}
		else if ( desiredData.equals( "encounters" ) )
		{
			desiredStream.println( "Encounter Listing: " );

			desiredStream.println();
			printList( encounterList, desiredStream );
		}
		else if ( desiredData.equals( "locations" ) )
		{
			desiredStream.println( "Visited Locations: " );
			desiredStream.println();

			printList( adventureList, desiredStream );
		}
		else if ( desiredData.equals( "counters" ) )
		{
			desiredStream.println( "Unexpired counters: " );
			desiredStream.println();

			desiredStream.println( StaticEntity.getUnexpiredCounters() );
			desiredStream.println();
		}
		else
		{
			List mainList = desiredData.equals( "closet" ) ? closet :desiredData.equals( "summary" ) ? tally :
				desiredData.equals( "storage" ) ? storage : desiredData.equals( "display" ) ? collection :
				desiredData.equals( "outfits" ) ? KoLCharacter.getOutfits() : desiredData.equals( "familiars" ) ? KoLCharacter.getFamiliarList() :
				desiredData.equals( "effects" ) ? activeEffects : inventory;

			if ( desiredData.startsWith( "skills" ) )
			{
				mainList = availableSkills;
				filter = filter.toLowerCase();

				if ( filter.startsWith( "cast" ) )
				{
					mainList = new ArrayList();
					mainList.addAll( availableSkills );

					List intersect = ClassSkillsDatabase.getSkillsByType( ClassSkillsDatabase.CASTABLE );
					mainList.retainAll( intersect );
					filter = "";
				}

				if ( filter.startsWith( "pass" ) )
				{
					mainList = new ArrayList();
					mainList.addAll( availableSkills );

					List intersect = ClassSkillsDatabase.getSkillsByType( ClassSkillsDatabase.PASSIVE );
					mainList.retainAll( intersect );
					filter = "";
				}

				if ( filter.startsWith( "self" ) )
				{
					mainList = new ArrayList();
					mainList.addAll( availableSkills );

					List intersect = ClassSkillsDatabase.getSkillsByType( ClassSkillsDatabase.SELF_ONLY );
					mainList.retainAll( intersect );
					filter = "";
				}

				if ( filter.startsWith( "buff" ) )
				{
					mainList = new ArrayList();
					mainList.addAll( availableSkills );

					List intersect = ClassSkillsDatabase.getSkillsByType( ClassSkillsDatabase.BUFF );
					mainList.retainAll( intersect );
					filter = "";
				}

				if ( filter.startsWith( "combat" ) )
				{
					mainList = new ArrayList();
					mainList.addAll( availableSkills );

					List intersect = ClassSkillsDatabase.getSkillsByType( ClassSkillsDatabase.COMBAT );
					mainList.retainAll( intersect );
					filter = "";
				}
			}

			if ( filter.equals( "" ) )
			{
				printList( mainList, desiredStream );
			}
			else
			{
				String currentItem;
				List resultList = new ArrayList();

				Object [] items = new Object[ mainList.size() ];
				mainList.toArray( items );

				for ( int i = 0; i < items.length; ++i )
				{
					currentItem = KoLDatabase.getCanonicalName( items[i].toString() );
					if ( currentItem.indexOf( filter ) != -1 )
						resultList.add( items[i] );
				}

				printList( resultList, desiredStream );
			}
		}
	}

	private static final String getStatString( int base, int adjusted, int tnp )
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

	public static final AdventureResult getFirstMatchingEffect( String parameters )
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

	public static final int getFirstMatchingItemId( List nameList )
	{	return getFirstMatchingItemId( nameList, null );
	}

	public static final int getFirstMatchingItemId( List nameList, String searchString )
	{
		if ( nameList == null )
			return -1;

		if ( nameList.isEmpty() )
			return -1;

		if ( nameList.size() == 1 )
			return TradeableItemDatabase.getItemId( (String) nameList.get(0) );

		String itemName;
		int itemId, useType;

		// First, if it's a usage match, then iterate through the
		// different names and remove any item which is not usable.
		// Also, prefer HP/MP restoratives over all items -- if any
		// wind up matching, remove non-restorative items.

		ArrayList restoreList = new ArrayList();

		for ( int i = 0; i < nameList.size(); ++i )
		{
			itemName = (String) nameList.get(i);
			itemId = TradeableItemDatabase.getItemId( itemName );
			useType = TradeableItemDatabase.getConsumptionType( itemId );

			switch ( useType )
			{
			case CONSUME_EAT:
			case CONSUME_DRINK:

				if ( isUsageMatch )
				{
					nameList.remove(i--);
					continue;
				}

				break;

			case CONSUME_USE:
			case MESSAGE_DISPLAY:
			case INFINITE_USES:
			case CONSUME_MULTIPLE:

				break;

			case HP_RESTORE:
			case MP_RESTORE:
			case HPMP_RESTORE:

				restoreList.add( itemName );
				break;

			default:

				if ( isUsageMatch )
				{
					nameList.remove(i--);
					continue;
				}
			}
		}

		if ( !restoreList.isEmpty() )
		{
			nameList.clear();
			nameList.addAll( restoreList );
		}

		for ( int i = 0; i < nameList.size(); ++i )
		{
			itemName = (String) nameList.get(i);
			itemId = TradeableItemDatabase.getItemId( itemName );
			useType = TradeableItemDatabase.getConsumptionType( itemId );

			// If this is a food match, and the item you're looking at is
			// not a food item, then skip it.

			if ( isFoodMatch && useType != CONSUME_EAT )
			{
				nameList.remove(i--);
				continue;
			}

			// If this is a booze match, and the item you're looking at is
			// not a booze item, then skip it.

			if ( isBoozeMatch && useType != CONSUME_DRINK )
			{
				nameList.remove(i--);
				continue;
			}

			// If this is a creatable match, and the item you're looking at is
			// not a creatable item, then skip it.

			if ( (isCreationMatch || isUntinkerMatch) && ConcoctionsDatabase.getMixingMethod( itemId ) == NOCREATE )
			{
				if ( itemId != MEAT_PASTE && itemId != MEAT_STACK && itemId != DENSE_STACK )
				{
					nameList.remove(i--);
					continue;
				}
			}
		}

		// If there were no matches, or there was an exact match,
		// then return from this method.

		if ( nameList.size() == 1 )
			return TradeableItemDatabase.getItemId( (String) nameList.get(0) );

		// Always prefer items which start with the search string
		// over items where the name appears in the middle.

		if ( searchString != null )
		{
			int matchCount = 0;
			String bestMatch = null;

			for ( int i = 0; i < nameList.size(); ++i )
			{
				itemName = (String) nameList.get(i);
				if ( itemName.toLowerCase().startsWith( searchString ) )
				{
					++matchCount;
					bestMatch = itemName;
				}
			}

			if ( matchCount == 1 )
				return TradeableItemDatabase.getItemId( bestMatch );
		}

		// If you have a usage match, the message display items are
		// not likely to be what you're looking for.

		if ( isUsageMatch )
		{
			for ( int i = 0; i < nameList.size(); ++i )
			{
				itemName = (String) nameList.get(i);
				itemId = TradeableItemDatabase.getItemId( itemName );
				useType = TradeableItemDatabase.getConsumptionType( itemId );

				if ( useType == MESSAGE_DISPLAY )
					nameList.remove(i--);
			}

			if ( nameList.size() == 1 )
				return TradeableItemDatabase.getItemId( (String) nameList.get(0) );
		}

		if ( !isUsageMatch && !isFoodMatch && !isBoozeMatch )
			return 0;

		// Candy hearts, snowcones and cupcakes take precedence over
		// all the other items in the game.

		for ( int i = 0; i < nameList.size(); ++i )
		{
			itemName = (String) nameList.get(i);
			if ( !itemName.startsWith( "pix" ) && itemName.endsWith( "candy heart" ) )
				return TradeableItemDatabase.getItemId( itemName );
		}

		for ( int i = 0; i < nameList.size(); ++i )
		{
			itemName = (String) nameList.get(i);
			if ( !itemName.startsWith( "abo" ) && !itemName.startsWith( "yel" ) && itemName.endsWith( "snowcone" ) )
				return TradeableItemDatabase.getItemId( itemName );
		}

		for ( int i = 0; i < nameList.size(); ++i )
		{
			itemName = (String) nameList.get(i);
			if ( itemName.endsWith( "cupcake" ) )
				return TradeableItemDatabase.getItemId( itemName );
		}

		return 0;
	}

	private static final List getMatchingItemNames( String itemName )
	{	return TradeableItemDatabase.getMatchingNames( itemName );
	}

	/**
	 * Utility method which determines the first item which matches
	 * the given parameter string.  Note that the string may also
	 * specify an item quantity before the string.
	 */

	public static final AdventureResult getFirstMatchingItem( String parameters )
	{	return getFirstMatchingItem( parameters, true );
	}

	public static final AdventureResult getFirstMatchingItem( String parameters, boolean errorOnFailure )
	{
		int itemId = -1;
		int itemCount = 1;

		// First, allow for the person to type without specifying
		// the amount, if the amount is 1.

		if ( parameters.indexOf( " " ) != -1 )
		{
			if ( parameters.charAt(0) == '*' )
			{
				itemCount = 0;
				parameters = parameters.substring(1).trim();
			}
			else if ( AdventureResult.itemId( parameters ) != -1 )
			{
				itemCount = 1;
			}
			else
			{
				boolean isNumeric = parameters.charAt(0) == '-' || Character.isDigit( parameters.charAt(0) );
				for ( int i = 1; i < parameters.length() && parameters.charAt(i) != ' '; ++i )
					isNumeric &= Character.isDigit( parameters.charAt(i) );

				if ( isNumeric )
				{
					itemCount = StaticEntity.parseInt( parameters.substring( 0, parameters.indexOf( " " ) ) );
					parameters = parameters.substring( parameters.indexOf( " " ) + 1 ).trim();
				}
			}
		}

		itemId = AdventureResult.itemId( parameters );

		if ( itemId == -1 )
		{
			List matchingNames = getMatchingItemNames( parameters );

			// Next, check to see if any of the items matching appear
			// in an NPC store.  If so, automatically default to it.

			if ( !matchingNames.isEmpty() )
			{
				itemId = getFirstMatchingItemId( matchingNames, parameters );
			}
			else
			{
				String testName, testProperty;
				ConsumeItemRequest.ensureUpdatedPotionEffects();

				for ( int i = 819; i <= 827 && itemId == -1; ++i )
				{
					testProperty = KoLSettings.getUserProperty( "lastBangPotion" + i );
					if ( !testProperty.equals( "" ) )
					{
						testName = TradeableItemDatabase.getItemName( i ) + " of " + testProperty;
						if ( testName.equalsIgnoreCase( parameters ) )
							itemId = i;
					}
				}
			}

			if ( itemId == 0 )
			{
				if ( errorOnFailure )
				{
					printList( matchingNames );
					RequestLogger.printLine();

					updateDisplay( ERROR_STATE, "[" + parameters + "] has too many matches." );
				}

				return null;
			}

			if ( itemId == -1 )
			{
				if ( errorOnFailure )
					updateDisplay( ERROR_STATE, "[" + parameters + "] does not match anything in the item database." );

				return null;
			}
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
			matchCount = firstMatch.getCount( matchList );

		// In the event that the person wanted all except a certain
		// quantity, be sure to update the item count.

		if ( matchList == storage && KoLCharacter.canInteract() )
		{
			itemCount = matchCount;
			firstMatch = firstMatch.getInstance( itemCount );
		}
		else if ( itemCount <= 0 )
		{
			itemCount = matchCount + itemCount;
			firstMatch = firstMatch.getInstance( itemCount );
		}

		if ( isExecutingCheckOnlyCommand )
		{
			RequestLogger.printLine( firstMatch == null ? "No match" : firstMatch.toString() );
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

		Object [] itemList = this.getMatchingItemList( parameters );
		if ( itemList.length == 0 )
			return;

		if ( ClanManager.getStash().isEmpty() && isWithdraw )
			RequestThread.postRequest( new ClanStashRequest() );

		RequestThread.postRequest( new ClanStashRequest( itemList, isWithdraw ?
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

		isUntinkerMatch = true;
		Object [] itemList = this.getMatchingItemList( parameters );
		isUntinkerMatch = false;

		RequestThread.openRequestSequence();

		for ( int i = 0; i < itemList.length; ++i )
			if ( itemList[i] != null )
				RequestThread.postRequest( new UntinkerRequest( ((AdventureResult)itemList[i]).getItemId(), ((AdventureResult)itemList[i]).getCount() ) );

		RequestThread.closeRequestSequence();
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
		// train (base | buffed | turns) <goal>
		String [] split = parameters.split( " " );

		if ( split.length < 2 || split.length > 3 )
		{
			updateDisplay( ERROR_STATE, "Syntax: train type goal" );
			return;
		}

		String typeString = split[0].toLowerCase();

		int type;

		if ( typeString.equals( "base" ) )
		{
			type = FamiliarTrainingFrame.BASE;
		}
		else if ( typeString.startsWith( "buff" ) )
		{
			type = FamiliarTrainingFrame.BUFFED;
		}
		else if ( typeString.equals( "turns" ) )
		{
			type = FamiliarTrainingFrame.TURNS;
		}
		else
		{
			updateDisplay( ERROR_STATE, "Unknown training type: " + typeString );
			return;
		}

		FamiliarTrainingFrame.levelFamiliar( StaticEntity.parseInt( split[1] ), type, false );
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
			RequestLogger.printLine( plotDetails.toString() );
		}
	}

	/**
	 * Play with the telescope
	 */

	private void executeTelescopeRequest( String parameters )
	{
		if ( KoLCharacter.inBadMoon() )
		{
			updateDisplay( ERROR_STATE, "Your telescope is unavailable in Bad Moon" );
			return;
		}

		// Find out how good our telescope is.
		KoLCharacter.setTelescope( false );

		int upgrades = KoLCharacter.getTelescopeUpgrades();
		if ( upgrades < 1 )
		{
			updateDisplay( ERROR_STATE, "You don't have a telescope." );
			return;
		}

		String [] split = parameters.split( " " );
		String command = split[0];

		if ( command.equals( "look" ) )
		{
			if ( split.length < 2 )
			{
				updateDisplay( ERROR_STATE, "Syntax: telescope [look] high|low" );
				return;
			}

			command = split[1];
		}

		if ( command.equals( "high" ) )
		{
			RequestThread.postRequest( new TelescopeRequest( TelescopeRequest.HIGH ) );
			return;
		}

		if ( command.equals( "low" ) )
		{
			RequestThread.postRequest( new TelescopeRequest( TelescopeRequest.LOW ) );
			upgrades = KoLCharacter.getTelescopeUpgrades();
		}

		// Display what you saw through the telescope
		RequestLogger.printLine( "You have a telescope with " + (upgrades - 1) + " additional upgrades" );

		// Every telescope shows you the gates.
		String gates = KoLSettings.getUserProperty( "telescope1" );
		String [] desc = SorceressLair.findGateByDescription( gates );
		if ( desc != null )
		{
			String name = SorceressLair.gateName( desc );
			String effect = SorceressLair.gateEffect( desc );
			String remedy = desc[3];
			RequestLogger.printLine( "Outer gate: " + name + " (" + effect + "/" + remedy + ")");
		}
		else
		{
			RequestLogger.printLine( "Outer gate: " + gates + " (unrecognized)");
		}

		// Upgraded telescopes can show you tower monsters
		for ( int i = 1; i < upgrades; ++i )
		{
			String prop = KoLSettings.getUserProperty( "telescope" + (i + 1) );
			desc = SorceressLair.findGuardianByDescription( prop );
			if ( desc != null )
			{
				String name = SorceressLair.guardianName( desc );
				String item = SorceressLair.guardianItem( desc );
				RequestLogger.printLine( "Tower Guardian #" + i + ": " + name + " (" + item + ")");
			}
			else
			{
				RequestLogger.printLine( "Tower Guardian #" + i + ": " + prop + " (unrecognized)");
			}
		}
	}

	public Object [] getMatchingItemList( String itemList )
	{
		String [] itemNames = itemList.split( "\\s*,\\s*" );

		boolean isMeatMatch = false;
		AdventureResult firstMatch = null;
		ArrayList items = new ArrayList();

		for ( int i = 0; i < itemNames.length; ++i )
		{
			isMeatMatch = false;

			if ( itemNames[i].endsWith( "meat" ) )
			{
				String amountString = itemNames[i].split( " " )[0];

				isMeatMatch = amountString.charAt(0) == '*' || amountString.charAt(0) == '-' || Character.isDigit( amountString.charAt(0) );
				for ( int j = 1; j < amountString.length() && isMeatMatch; ++j )
					isMeatMatch &= Character.isDigit( amountString.charAt(j) );

				if ( isMeatMatch )
				{
					int amount = amountString.equals( "*" ) ? 0 : StaticEntity.parseInt( amountString );
					firstMatch = new AdventureResult( AdventureResult.MEAT, amount > 0 ? amount : KoLCharacter.getAvailableMeat() + amount );
				}
			}

			if ( !isMeatMatch )
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
		if ( KoLCharacter.inBadMoon() )
		{
			KoLmafia.updateDisplay( ERROR_STATE, "Hagnk's Storage is not available in Bad Moon" );
			return;
		}

		matchList = storage;
		Object [] items = this.getMatchingItemList( parameters );
		matchList = inventory;

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

		// Double check to make sure you have all items on hand
		// since a failure to get something from Hagnk's is bad.

		int storageCount;
		AdventureResult item;

		for ( int i = 0; i < items.length; ++i )
		{
			item = (AdventureResult) items[i];
			storageCount = item.getCount( storage );

			if ( items[i] != null && storageCount < item.getCount() )
			{
				updateDisplay( ERROR_STATE, "You only have " + storageCount + " " + item.getName() +
					" in storage (you wanted " + item.getCount() + ")" );
			}
		}

		RequestThread.postRequest( new ItemStorageRequest(
			ItemStorageRequest.STORAGE_TO_INVENTORY, items ) );
	}

	/**
	 * A special module used specifically for properly instantiating
	 * ItemManageRequests which manage the closet.
	 */

	private void executeClosetManageRequest( String parameters )
	{
		if ( parameters.length() <= 4 )
		{
			printList( closet );
			return;
		}

		if ( !parameters.startsWith( "take" ) && !parameters.startsWith( "put" ) )
		{
			updateDisplay( ERROR_STATE, "Invalid closet command." );
			return;
		}

		if ( parameters.startsWith( "take" ) )
			matchList = closet;

		Object [] itemList = this.getMatchingItemList( parameters.substring(4).trim() );
		matchList = inventory;

		if ( itemList.length == 0 )
			return;

		int meatAttachmentCount = 0;

		for ( int i = 0; i < itemList.length; ++i )
		{
			if ( ((AdventureResult)itemList[i]).getName().equals( AdventureResult.MEAT ) )
			{
				RequestThread.postRequest( new ItemStorageRequest(
					parameters.startsWith( "take" ) ? ItemStorageRequest.MEAT_TO_INVENTORY : ItemStorageRequest.MEAT_TO_CLOSET, ((AdventureResult)itemList[i]).getCount() ) );

				itemList[i] = null;
				++meatAttachmentCount;
			}
		}

		if ( meatAttachmentCount == itemList.length )
			return;

		RequestThread.postRequest( new ItemStorageRequest(
			parameters.startsWith( "take" ) ? ItemStorageRequest.CLOSET_TO_INVENTORY : ItemStorageRequest.INVENTORY_TO_CLOSET, itemList ) );
	}

	/**
	 * A special module used specifically for properly instantiating
	 * AutoSellRequests which send things to the mall.
	 */

	private void executeMallSellRequest( String parameters )
	{
		String [] itemNames = parameters.split( "\\s*,\\s+" );

		AdventureResult [] items = new AdventureResult[ itemNames.length ];
		int [] prices = new int[ itemNames.length ];
		int [] limits = new int[ itemNames.length ];

		int separatorIndex;
		String description;

		for ( int i = 0; i < itemNames.length; ++i )
		{
			separatorIndex = itemNames[i].indexOf( "@" );

			if ( separatorIndex != -1 )
			{
				description = itemNames[i].substring( separatorIndex + 1 ).trim();
				itemNames[i] = itemNames[i].substring( 0, separatorIndex );

				separatorIndex = description.indexOf( "limit" );

				if ( separatorIndex != -1 )
				{
					limits[i] = StaticEntity.parseInt( description.substring( separatorIndex + 5 ) );
					description = description.substring( 0, separatorIndex ).trim();
				}

				prices[i] = StaticEntity.parseInt( description );
			}

			items[i] = getFirstMatchingItem( itemNames[i], false );

			if ( items[i] == null )
			{
				int spaceIndex = itemNames[i].lastIndexOf( " " );
				if ( spaceIndex == -1 )
					continue;

				prices[i] = StaticEntity.parseInt( parameters.substring( spaceIndex + 1 ) );
				itemNames[i] = itemNames[i].substring( 0, spaceIndex ).trim();
				items[i] = getFirstMatchingItem( itemNames[i], false );
			}

			if ( itemNames[i] == null )
			{
				int spaceIndex = itemNames[i].lastIndexOf( " " );
				if ( spaceIndex == -1 )
					return;

				limits[i] = prices[i];
				prices[i] = StaticEntity.parseInt( itemNames[i].substring( spaceIndex + 1 ) );
				itemNames[i] = itemNames[i].substring( 0, spaceIndex ).trim();

				items[i] = getFirstMatchingItem( itemNames[i], false );
			}

			if ( items[i] == null )
				continue;

			int inventoryCount = items[i].getCount( inventory );

			if ( items[i].getCount() == 1 && !itemNames[i].startsWith( "1" ) )
				items[i] = items[i].getInstance( inventoryCount );
			else if ( items[i].getCount() != inventoryCount )
				items[i] = items[i].getInstance( Math.min( items[i].getCount(), inventoryCount ) );
		}

		RequestThread.postRequest( new AutoSellRequest( items, prices, limits, AutoSellRequest.AUTOMALL ) );
	}

	/**
	 * A special module used specifically for properly instantiating
	 * AutoSellRequests for just autoselling items.
	 */

	private void executeAutoSellRequest( String parameters )
	{
		Object [] items = this.getMatchingItemList( parameters );
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

	private void executeBuyCommand( String parameters )
	{
		Object [] itemList = this.getMatchingItemList( parameters );

		for ( int i = 0; i < itemList.length; ++i )
		{
			AdventureResult match = (AdventureResult) itemList[i];

			if ( !KoLCharacter.canInteract() && !NPCStoreDatabase.contains( match.getName() ) )
			{
				updateDisplay( ERROR_STATE, "You are not yet out of ronin." );
				return;
			}

			ArrayList results = new ArrayList();

			StoreManager.searchMall( '\"' + match.getName() + '\"', results, 10, false );
			StaticEntity.getClient().makePurchases( results, results.toArray(), match.getCount(), false );
		}
	}

	/**
	 * A special module used specifically for properly instantiating
	 * ItemCreationRequests.
	 */

	private void executeItemCreationRequest( String parameters )
	{
		if ( parameters.equals( "" ) )
		{
			printList( ConcoctionsDatabase.getCreatables() );
			return;
		}

		isCreationMatch = true;
		Object [] itemList = this.getMatchingItemList( parameters );
		isCreationMatch = false;

		AdventureResult currentMatch;
		ItemCreationRequest irequest;

		for ( int i = 0; i < itemList.length; ++i )
		{
			currentMatch = (AdventureResult) itemList[i];
			if ( itemList[i] == null )
				continue;

			irequest = ItemCreationRequest.getInstance( currentMatch.getItemId() );

			if ( irequest == null )
			{
				switch ( ConcoctionsDatabase.getMixingMethod( currentMatch.getItemId() ) )
				{
				case COOK:
				case COOK_REAGENT:
				case SUPER_REAGENT:
				case COOK_PASTA:

					updateDisplay( ERROR_STATE, "You cannot cook without a chef-in-the-box." );
					return;

				case MIX:
				case MIX_SPECIAL:
				case MIX_SUPER:

					updateDisplay( ERROR_STATE, "You cannot mix without a bartender-in-the-box." );
					return;

				default:

					updateDisplay( ERROR_STATE, "That item cannot be created." );
					return;
				}
			}

			irequest.setQuantityNeeded( currentMatch.getCount() );
			RequestThread.postRequest( irequest );
		}
	}

	/**
	 * A special module used specifically for properly instantiating
	 * ConsumeItemRequests.
	 */

	private void executeConsumeItemRequest( String parameters )
	{
		if ( this.currentLine.startsWith( "eat" ) )
		{
			if ( this.makeKitchenRequest( parameters ) )
				return;
			if ( this.makeRestaurantRequest( parameters ) )
				return;
		}

		if ( this.currentLine.startsWith( "drink" ) )
		{
			if ( this.makeKitchenRequest( parameters ) )
				return;
			if ( this.makeMicrobreweryRequest( parameters ) )
				return;
		}

		// Now, handle the instance where the first item is actually
		// the quantity desired, and the next is the amount to use

		AdventureResult currentMatch;

		isFoodMatch = this.currentLine.startsWith( "eat" );
		isBoozeMatch = this.currentLine.startsWith( "drink" ) || this.currentLine.startsWith( "hobo" );
		isUsageMatch = !isFoodMatch && !isBoozeMatch;

		Object [] itemList = this.getMatchingItemList( parameters );

		isUsageMatch = false;
		isBoozeMatch = false;
		isFoodMatch = false;

		for ( int i = 0; i < itemList.length; ++i )
		{
			currentMatch = (AdventureResult) itemList[i];

			if ( this.currentLine.startsWith( "eat" ) )
			{
				if ( TradeableItemDatabase.getConsumptionType( currentMatch.getItemId() ) != CONSUME_EAT )
				{
					updateDisplay( ERROR_STATE, currentMatch.getName() + " cannot be consumed." );
					return;
				}
			}

			if ( this.currentLine.startsWith( "drink" ) || this.currentLine.startsWith( "hobo" ) )
			{
				if ( TradeableItemDatabase.getConsumptionType( currentMatch.getItemId() ) != CONSUME_DRINK )
				{
					updateDisplay( ERROR_STATE, currentMatch.getName() + " is not an alcoholic beverage." );
					return;
				}
			}

			if ( this.currentLine.startsWith( "use" ) )
			{
				switch ( TradeableItemDatabase.getConsumptionType( currentMatch.getItemId() ) )
				{
				case CONSUME_EAT:
					updateDisplay( ERROR_STATE, currentMatch.getName() + " must be eaten." );
					return;
				case CONSUME_DRINK:
					updateDisplay( ERROR_STATE, currentMatch.getName() + " is an alcoholic beverage." );
					return;
				}
			}

			ConsumeItemRequest request = !this.currentLine.startsWith( "hobo" ) ? new ConsumeItemRequest( currentMatch ) :
				new ConsumeItemRequest( CONSUME_HOBO, currentMatch );

			RequestThread.postRequest( request );
		}
	}

	/**
	 * A special module for instantiating display case management requests,
	 * strictly for adding and removing things.
	 */

	private void executeDisplayCaseRequest( String parameters )
	{
		if ( collection.isEmpty() )
			RequestThread.postRequest( new MuseumRequest() );

		if ( parameters.length() == 0 )
		{
			printList( collection );
			return;
		}

		if ( !parameters.startsWith( "put" ) && !parameters.startsWith( "take" ) )
		{
			showData( "display " + parameters );
			return;
		}

		if ( parameters.startsWith( "take" ) )
			matchList = collection;

		Object [] items = this.getMatchingItemList( parameters.substring(4).trim() );
		matchList = inventory;

		if ( items.length == 0 )
			return;

		RequestThread.postRequest( new MuseumRequest( items, !parameters.startsWith( "take" ) ) );
	}

	/**
	 * A special module used specifically for properly instantiating
	 * AdventureRequests, including HermitRequests, if necessary.
	 */

	private void executeAdventureRequest( String parameters )
	{
		int adventureCount;
		KoLAdventure adventure = AdventureDatabase.getAdventure( parameters.equalsIgnoreCase( "last" ) ? KoLSettings.getUserProperty( "lastAdventure" ) : parameters );

		if ( adventure != null )
			adventureCount = 1;
		else
		{
			String adventureCountString = parameters.split( " " )[0];
			adventureCount = adventureCountString.equals( "*" ) ? 0 : StaticEntity.parseInt( adventureCountString );

			if ( adventureCount == 0 && !adventureCountString.equals( "0" ) && !adventureCountString.equals( "*" ) )
			{
				updateDisplay( ERROR_STATE, parameters + " does not exist in the adventure database." );
				return;
			}

			String adventureName = parameters.substring( adventureCountString.length() ).trim();
			adventure = AdventureDatabase.getAdventure( adventureName );

			if ( adventure == null )
			{
				updateDisplay( ERROR_STATE, parameters + " does not exist in the adventure database." );
				return;
			}

			if ( adventureCount <= 0 && adventure.getFormSource().equals( "shore.php" ) )
				adventureCount += (int) Math.floor( KoLCharacter.getAdventuresLeft() / 3 );
			else if ( adventureCount <= 0 )
				adventureCount += KoLCharacter.getAdventuresLeft();
		}

		if ( isExecutingCheckOnlyCommand )
		{
			RequestLogger.printLine( adventure.toString() );
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

		EquipmentDatabase.retrieveOutfit( intendedOutfit.getOutfitId() );
		RequestThread.postRequest( new EquipmentRequest( intendedOutfit ) );
	}

	public static final SpecialOutfit getMatchingOutfit( String name )
	{
		String lowercaseOutfitName = name.toLowerCase().trim();
		if ( lowercaseOutfitName.equals( "birthday suit" ) || lowercaseOutfitName.equals( "nothing" ) )
			return SpecialOutfit.BIRTHDAY_SUIT;

		Object currentTest;

		for ( int i = 0; i < KoLCharacter.getCustomOutfits().size(); ++i )
		{
			currentTest = KoLCharacter.getCustomOutfits().get(i);
			if ( currentTest instanceof SpecialOutfit && currentTest.toString().toLowerCase().indexOf( lowercaseOutfitName ) != -1 )
				return (SpecialOutfit) currentTest;
		}

		for ( int i = 0; i < EquipmentDatabase.getOutfitCount(); ++i )
		{
			currentTest = EquipmentDatabase.getOutfit( i );
			if ( currentTest != null && currentTest.toString().toLowerCase().indexOf( lowercaseOutfitName ) != -1 )
				return (SpecialOutfit) currentTest;
		}

		return null;
	}

	/**
	 * A special module used specifically for properly instantiating
	 * the BuffBot and running it
	 */

	private void executeBuffBotCommand( String parameters )
	{
		BuffBotManager.loadSettings();
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

			String buffToCheck;
			AdventureResult buffToRemove = null;

			for ( int i = 0; i < matchingEffects.size(); ++i )
			{
				buffToCheck = (String) matchingEffects.get(i);
				if ( UneffectRequest.isShruggable( buffToCheck ) )
				{
					++shruggableCount;
					buffToRemove = new AdventureResult( buffToCheck, 1, true );
				}
			}

			if ( shruggableCount == 1 )
			{
				if ( activeEffects.contains( buffToRemove ) )
				{
					if ( isExecutingCheckOnlyCommand )
					{
						RequestLogger.printLine( buffToRemove.toString() );
						return;
					}

					RequestThread.postRequest( new UneffectRequest( buffToRemove ) );
				}

				return;
			}

			updateDisplay( ERROR_STATE, "Ambiguous effect name: " + parameters );
			printList( matchingEffects );

			return;
		}

		AdventureResult effect = new AdventureResult( (String) matchingEffects.get(0), 1, true );

		if ( isExecutingCheckOnlyCommand )
		{
			RequestLogger.printLine( effect.toString() );
			return;
		}

		if ( activeEffects.contains( effect ) )
			RequestThread.postRequest( new UneffectRequest( effect ) );
	}

	/**
	 * Attempts to zap the specified item with the specified wand
	 */

	public void makeZapRequest()
	{
		if ( this.currentLine == null )
			return;

		if ( !this.currentLine.startsWith( "zap" ) || this.currentLine.indexOf( " " ) == -1 )
			return;

		String command = this.currentLine.split( " " )[0];
		String parameters = this.currentLine.substring( command.length() ).trim();

		if ( parameters.length() == 0 )
		{
			updateDisplay( ERROR_STATE, "Zap what?" );
			return;
		}

		Object [] itemList = this.getMatchingItemList( parameters );

		for ( int i = 0; i < itemList.length; ++i )
			RequestThread.postRequest( new ZapRequest( (AdventureResult) itemList[i] ) );
	}

	/**
	 * Attempts to smash the specified item
	 */

	public void makePulverizeRequest( String parameters )
	{
		Object [] itemList = this.getMatchingItemList( parameters );

		for ( int i = 0; i < itemList.length; ++i )
			RequestThread.postRequest( new PulverizeRequest( (AdventureResult) itemList[i] ) );
	}

	/**
	 * Attempts to visit the Styx Pixie
	 */

	public void executeStyxRequest( String parameters )
	{
		if ( !KoLCharacter.inBadMoon() )
		{
			updateDisplay( ERROR_STATE, "You can't find the Styx unless you are in Bad Moon." );
			return;
		}

		String [] split = parameters.split( " " );
		String command = split[0];
		int stat = NONE;

		if ( command.equalsIgnoreCase( "muscle" ) )
			stat = MUSCLE;
		else if ( command.equalsIgnoreCase( "mysticality" ) )
			stat = MYSTICALITY;
		else if ( command.equalsIgnoreCase( "moxie" ) )
			stat = MOXIE;

		if ( stat == NONE )
		{
			updateDisplay( ERROR_STATE, "You can only buff muscle, mysticality, or moxie." );
			return;
		}

		RequestThread.postRequest( new StyxPixieRequest( stat ) );
	}

	/**
	 * Attempts to listen to a concert at the Arena
	 */

	public void executeArenaRequest( String parameters )
	{
		String [] split = parameters.split( " " );
		String actionString = split[0];
		int action = StaticEntity.parseInt( actionString );

		if ( action < 1 || action > 3 )
		{
			updateDisplay( ERROR_STATE, "Pick action 1, 2, or 3." );
			return;
		}

		RequestThread.postRequest( new ArenaRequest( action ) );
	}

	/**
	 * Attempts to get a blessing from the Deep Fat Friars
	 */

	public void executeFriarRequest( String parameters )
	{
		String [] split = parameters.split( " " );
		String command = split[0];

		if ( command.equals( "blessing" ) )
		{
			if ( split.length < 2 )
			{
				updateDisplay( ERROR_STATE, "Syntax: friars [blessing] food|familiar|booze" );
				return;
			}

			command = split[1];
		}

		int action = 0;

		if ( Character.isDigit( command.charAt(0) ) )
		{
			action = StaticEntity.parseInt( command );
		}
		else
		{
			for ( int i = 0; i < FriarRequest.BLESSINGS.length; ++i )
			{
				if ( command.equalsIgnoreCase( FriarRequest.BLESSINGS[i] ) )
				{
					action = i + 1;
					break;
				}
			}
		}

		if ( action < 1 || action > 3 )
		{
			updateDisplay( ERROR_STATE, "Syntax: friars [blessing] food|familiar|booze" );
			return;
		}

		RequestThread.postRequest( new FriarRequest( action ) );
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

		int count = 1;

		if ( Character.isDigit( parameters.charAt( 0 ) ) )
		{
			int spaceIndex = parameters.indexOf( " " );
			count = StaticEntity.parseInt( parameters.substring( 0, spaceIndex ) );
			parameters = parameters.substring( spaceIndex ).trim();
		}
		else if ( parameters.charAt( 0 ) == '*' )
		{
			int spaceIndex = parameters.indexOf( " " );
			count = HermitRequest.getWorthlessItemCount();
			parameters = parameters.substring( spaceIndex ).trim();
		}

		int itemId = -1;
		parameters = parameters.toLowerCase();

		for ( int i = 0; i < hermitItems.size() && itemId == -1; ++i )
		{
			if ( hermitItems.get(i).toString().toLowerCase().indexOf( parameters ) != -1 )
			{
				if ( isExecutingCheckOnlyCommand )
				{
					RequestLogger.printLine( ((AdventureResult)hermitItems.get(i)).getName() );
					return;
				}

				itemId = ((AdventureResult)hermitItems.get(i)).getItemId();
				if ( itemId == SewerRequest.TEN_LEAF_CLOVER && count != 1 )
					count = Math.min( count, ((AdventureResult)hermitItems.get(i)).getCount() );
			}
		}

		if ( itemId == -1 )
		{
			updateDisplay( ERROR_STATE, "You can't get a " + parameters + " from the hermit today." );
			return;
		}

		RequestThread.postRequest( new HermitRequest( itemId, count ) );
	}

	/**
	 * Makes a request to Hell's Kitchen to purchase an item.  If the item
	 * is not available, this method does not report an error.
	 */

	public boolean makeKitchenRequest( String parameters )
	{
		if ( !KoLCharacter.inBadMoon() )
			return false;

		if ( kitchenItems.isEmpty() )
			KitchenRequest.getMenu();

		if ( parameters.equals( "" ) )
			return false;

		String [] splitParameters = this.splitCountAndName( parameters );
		String countString = splitParameters[0];
		String nameString = splitParameters[1];

		for ( int i = 0; i < kitchenItems.size(); ++i )
		{
			String name = (String) kitchenItems.get(i);

			if ( !KoLDatabase.substringMatches( name.toLowerCase(), nameString ) )
				continue;

			if ( isExecutingCheckOnlyCommand )
			{
				RequestLogger.printLine( name );
				return true;
			}

			int count = countString == null || countString.length() == 0 ? 1 :
				StaticEntity.parseInt( countString );

			if ( count == 0 )
			{
				if ( name.equals( "Imp Ale" ) )
				{
					int inebriety = TradeableItemDatabase.getInebriety( name );
					if ( inebriety > 0 )
						count = (KoLCharacter.getInebrietyLimit() - KoLCharacter.getInebriety()) / inebriety;
				}
				else
				{
					int fullness = TradeableItemDatabase.getFullness( name );
					if ( fullness > 0 )
						count = (KoLCharacter.getFullnessLimit() - KoLCharacter.getFullness()) / fullness;
				}
			}

			for ( int j = 0; j < count; ++j )
				RequestThread.postRequest( new KitchenRequest( name ) );

			return true;
		}

		return false;
	}

	/**
	 * Makes a request to the restaurant to purchase a meal.  If the item
	 * is not available, this method does not report an error.
	 */

	public boolean makeRestaurantRequest( String parameters )
	{
		if ( !KoLCharacter.inMysticalitySign() )
			return false;

		if ( restaurantItems.isEmpty() )
			RequestThread.postRequest( new RestaurantRequest() );

		if ( parameters.equals( "" ) )
		{
			RequestLogger.printLine( "Today's Special: " + RestaurantRequest.getDailySpecial() );
			return false;
		}

		String [] splitParameters = this.splitCountAndName( parameters );
		String countString = splitParameters[0];
		String nameString = splitParameters[1].toLowerCase();

		if ( nameString.equalsIgnoreCase( "daily special" ) )
			nameString = RestaurantRequest.getDailySpecial().getName().toLowerCase();

		for ( int i = 0; i < restaurantItems.size(); ++i )
		{
			String name = (String) restaurantItems.get(i);

			if ( !KoLDatabase.substringMatches( name.toLowerCase(), nameString ) )
				continue;

			if ( isExecutingCheckOnlyCommand )
			{
				RequestLogger.printLine( name );
				return true;
			}

			int count = countString == null || countString.length() == 0 ? 1 :
				StaticEntity.parseInt( countString );

			if ( count == 0 )
			{
				int fullness = TradeableItemDatabase.getFullness( name );
				if ( fullness > 0 )
					count = (KoLCharacter.getFullnessLimit() - KoLCharacter.getFullness()) / fullness;
			}

			for ( int j = 0; j < count; ++j )
				RequestThread.postRequest( new RestaurantRequest( name ) );

			return true;
		}

		return false;
	}

	/**
	 * Makes a request to the microbrewery to purchase a drink.  If the
	 * item is not available, this method does not report an error.
	 */

	public boolean makeMicrobreweryRequest( String parameters )
	{
		if ( !KoLCharacter.inMoxieSign() )
			return false;

		if ( microbreweryItems.isEmpty() )
			RequestThread.postRequest( new MicrobreweryRequest() );

		if ( parameters.equals( "" ) )
		{
			RequestLogger.printLine( "Today's Special: " + MicrobreweryRequest.getDailySpecial() );
			return false;
		}

		String [] splitParameters = this.splitCountAndName( parameters );
		String countString = splitParameters[0];
		String nameString = splitParameters[1].toLowerCase();

		if ( nameString.equalsIgnoreCase( "daily special" ) )
			nameString = MicrobreweryRequest.getDailySpecial().getName().toLowerCase();

		for ( int i = 0; i < microbreweryItems.size(); ++i )
		{
			String name = (String) microbreweryItems.get(i);

			if ( !KoLDatabase.substringMatches( name.toLowerCase(), nameString ) )
				continue;

			if ( isExecutingCheckOnlyCommand )
			{
				RequestLogger.printLine( name );
				return true;
			}

			int count = countString == null || countString.length() == 0 ? 1 :
				StaticEntity.parseInt( countString );

			if ( count == 0 )
			{
				int inebriety = TradeableItemDatabase.getInebriety( name );
				if ( inebriety > 0 )
					count = (KoLCharacter.getInebrietyLimit() - KoLCharacter.getInebriety()) / inebriety;
			}

			for ( int j = 0; j < count; ++j )
				RequestThread.postRequest( new MicrobreweryRequest( name ) );

			return true;
		}

		return false;
	}
}
