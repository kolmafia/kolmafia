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

import jline.ConsoleReader;
import net.sourceforge.kolmafia.MonsterDatabase.Monster;

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

	private static boolean isUsageMatch = false;
	private static boolean isCreationMatch = false;
	private static boolean isUntinkerMatch = false;

	private String previousLine = null;
	private String currentLine = null;
	private BufferedReader commandStream;

	private static boolean isExecutingCheckOnlyCommand = false;
	private static ConsoleReader CONSOLE = null;

	private static TreeMap ALIASES = new TreeMap();
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

	public static void main( String [] args )
	{
		System.out.println();
		System.out.println();
		System.out.println(  VERSION_NAME );
		System.out.println( "Running on " + System.getProperty( "os.name" ) );
		System.out.println( "Using Java " + System.getProperty( "java.version" ) );
		System.out.println();

		StaticEntity.setClient( DEFAULT_SHELL );
		RequestLogger.openStandard();

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

	public void attemptLogin()
	{
		try
		{
			String username = StaticEntity.getProperty( "autoLogin" );

			if ( username == null || username.length() == 0 )
			{
				System.out.println();
				System.out.print( "username: " );
				username = CONSOLE == null ? this.commandStream.readLine() : CONSOLE.readLine();
			}

			if ( username == null || username.length() == 0 )
			{
				System.out.println( "Invalid login." );
				return;
			}

			if ( username.startsWith( "login " ) )
				username = username.substring( 6 );

			String password = getSaveState( username );

			if ( password == null )
			{
				System.out.print( "password: " );
				password = CONSOLE == null ? this.commandStream.readLine() : CONSOLE.readLine( new Character( '*' ) );
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
			String holiday = MoonPhaseDatabase.getHoliday( DATED_FILENAME_FORMAT.parse( DATED_FILENAME_FORMAT.format( new Date() ) ), true );
			updateDisplay( holiday + ", " + MoonPhaseDatabase.getMoonEffect() );
		}
		catch ( Exception e )
		{
			// Should not happen, you're parsing something that
			// was formatted the same way.

			StaticEntity.printStackTrace( e );
		}

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

				line = DEFAULT_SHELL == this && CONSOLE != null ? CONSOLE.readLine() :
					this.commandStream.readLine();
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

			if ( command.equals( "events" ) )
			{
				KoLmafiaGUI.constructFrame( "EventsFrame" );
				return;
			}

			if ( command.equals( "compose" ) )
			{
				KoLmafiaGUI.constructFrame( "GreenMessageFrame" );
				return;
			}

			if ( command.equals( "gift" ) )
			{
				KoLmafiaGUI.constructFrame( "GiftMessageFrame" );
				return;
			}

			if ( command.equals( "options" ) )
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
			if ( KoLRequest.shouldIgnore( this.currentLine ) )
				return;

			KoLRequest request = new KoLRequest( this.currentLine, true );
			RequestThread.postRequest( request );

			StaticEntity.externalUpdate( request.getURLString(), request.responseText );
			return;
		}

		// Allow a version which lets you see the resulting
		// text without loading a mini/relay browser window.

		if ( command.equals( "text" ) )
		{
			if ( KoLRequest.shouldIgnore( this.currentLine ) )
				return;

			KoLRequest request = new KoLRequest( this.currentLine.substring(4).trim(), true );
			RequestThread.postRequest( request );

			StaticEntity.externalUpdate( request.getURLString(), request.responseText );
			this.showHTML( request.responseText );

			return;

		}

		// Maybe the person wants to load up their browser
		// from the KoLmafia CLI?

		if ( command.equals( "relay" ) || command.equals( "serve" ) )
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

		if ( command.equals( "checklist" ) )
		{
			if ( KoLCharacter.hasItem( KoLAdventure.MEATCAR ) )
				RequestThread.postRequest( new UntinkerRequest( KoLAdventure.MEATCAR.getItemId() ) );

			conditions.clear();
			conditions.addAll( ascensionCheckList );

			this.executeConditionsCommand( "check" );
			return;
		}

		if ( command.equals( "junk" ) || command.equals( "cleanup" ) )
		{
			this.makeJunkRemovalRequest();
			return;
		}

		// Preconditions kickass, so they're handled right
		// after the wait command.  (Right)

		if ( command.equals( "conditions" ) || command.equals( "objectives" ) )
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

		if ( command.equals( "echo" ) )
		{
			if ( parameters.equalsIgnoreCase( "timestamp" ) )
				parameters = MoonPhaseDatabase.getCalendarDayAsString( new Date() );

			RequestLogger.printLine( StaticEntity.globalStringReplace( parameters, "<", "&lt;" ) );
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
					RequestLogger.printLine( StaticEntity.getProperty( parameters ) );

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

				value = CombatSettings.getLongCombatOptionName( value );

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

				if ( !StaticEntity.getProperty( "defaultAutoAttack" ).equals( value ) )
					RequestThread.postRequest( new KoLRequest( "account.php?action=autoattack&whichattack=" + value ) );
			}

			if ( StaticEntity.getProperty( name ).equals( value ) )
				return;

			RequestLogger.printLine( name + " => " + value );
			StaticEntity.setProperty( name, value );

			if ( name.equals( "buffBotCasting" ) )
				BuffBotManager.loadSettings();

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
			this.executeCommand( "logout", "" );
			String password = getSaveState( parameters );

			if ( password != null )
				RequestThread.postRequest( new LoginRequest( parameters, password ) );
			else if ( StaticEntity.getClient() == DEFAULT_SHELL )
				DEFAULT_SHELL.attemptLogin();
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

			if ( !KoLCharacter.getUserName().equals( "" ) )
				RequestThread.postRequest( new LogoutRequest() );

			return;
		}

		// Now for formal exit commands.

		if ( command.equals( "exit" ) || command.equals( "quit" ) )
		{
			executeCommand( "logout", "" );
			System.exit(0);
		}

		// Next, handle any requests for script execution;
		// these can be done at any time (including before
		// login), so they should be handled before a test
		// of login state needed for other commands.

		if ( command.equals( "namespace" ) )
		{
			// Validate the script first.

			String [] scripts = StaticEntity.getProperty( "commandLineNamespace" ).split( "," );
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

		if ( command.startsWith( "func" ) )
		{
			File f = findScriptFile( parameters );
			if ( f == null )
			{
				KoLmafia.updateDisplay( ERROR_STATE, "No script located named " + parameters + "." );
				return;
			}

			KoLmafiaASH interpreter = KoLmafiaASH.getInterpreter( f );
			if ( interpreter == null )
			{
				KoLmafia.updateDisplay( ERROR_STATE, "Invalid script " + f.getAbsolutePath() + "." );
				return;
			}

			interpreter.showUserFunctions( "" );
			return;
		}

		if ( command.equals( "ashref" ) )
		{
			NAMESPACE_INTERPRETER.showExistingFunctions( parameters );
			return;
		}

		if ( command.equals( "using" ) )
		{
			// Validate the script first.

			this.executeCommand( "validate", parameters );
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

		if ( command.equals( "verify" ) || command.equals( "validate" ) || command.equals( "check" ) || command.equals( "using" ) || command.equals( "call" ) || command.equals( "run" ) || command.startsWith( "exec" ) || command.equals( "load" ) || command.equals( "start" ) )
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

		// Next, print out the moon phase, if the user
		// wishes to know what moon phase it is.

		if ( command.startsWith( "moon" ) )
		{
			updateDisplay( "Ronald: " + MoonPhaseDatabase.getRonaldPhaseAsString() );
			updateDisplay( "Grimace: " + MoonPhaseDatabase.getGrimacePhaseAsString() );
			RequestLogger.printLine();

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

			RequestLogger.printLine();
			updateDisplay( MoonPhaseDatabase.getHoliday( today ) );
			updateDisplay( MoonPhaseDatabase.getMoonEffect() );
			RequestLogger.printLine();

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
				FightFrame.showRequest( request );
			else
				this.showHTML( request.responseText );

			return;
		}

		if ( command.equals( "wiki" ) )
		{
			TradeableItemDatabase.determineWikiData( parameters );
			return;
		}

		if ( command.equals( "survival" ) || command.equals( "locdata" ) )
		{
			this.showHTML( AdventureDatabase.getAreaCombatData( AdventureDatabase.getAdventure( parameters ).toString() ).toString() );
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

			this.executePrintCommand( parameters );
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
			this.trainFamiliar( parameters );
			return;
		}

		// Next is the command to visit the council.
		// This prints data to the command line.

		if ( command.equals( "council" ) )
		{
			KoLRequest request = new KoLRequest( "council.php", true );
			RequestThread.postRequest( request );

			this.showHTML( StaticEntity.singleStringReplace( request.responseText,
				"<a href=\"town.php\">Back to Seaside Town</a>", "" ) );

			return;
		}

		// Campground activities are fairly common, so
		// they will be listed first after the above.

		if ( command.equals( "campground" ) )
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

		// Uneffect with martians are related to buffs,
		// so listing them next seems logical.

		if ( command.equals( "shrug" ) || command.equals( "shrugoff" ) || command.equals( "uneffect" ) || command.equals( "remedy" ) )
		{
			this.executeUneffectRequest( parameters );
			return;
		}

		// Add in item retrieval the way KoLmafia handles
		// it internally.

		if ( command.equals( "find" ) || command.equals( "acquire" ) || command.equals( "retrieve" ) )
		{
			// Handle lucky and unlucky retrieval of
			// worthless items via the sewer.

			if ( parameters.indexOf( "worthless item" ) != -1 )
			{
				int itemCount = 1;
				int maximumCount = KoLCharacter.getAdventuresLeft();

				if ( parameters.indexOf( " in " ) != -1 )
				{
					maximumCount = StaticEntity.parseInt( parameters.substring( parameters.lastIndexOf( " " ) ) );
					if ( maximumCount < 0 )
						maximumCount += KoLCharacter.getAdventuresLeft();
				}

				if ( !parameters.startsWith( "worthless item" ) )
					itemCount = StaticEntity.parseInt( parameters.substring( 0, parameters.indexOf( " " ) ) );

				ArrayList temporary = new ArrayList();
				temporary.addAll( conditions );
				conditions.clear();

				if ( parameters.indexOf( "with clover" ) != -1 )
				{
					if ( KoLCharacter.getAdventuresLeft() > 0 )
					{
						AdventureDatabase.retrieveItem( SewerRequest.CLOVER.getInstance( itemCount ) );
						this.executeAdventureRequest( itemCount + " sewer with clovers" );
					}
				}
				else
				{
					while ( maximumCount > 0 && HermitRequest.getWorthlessItemCount() < itemCount && permitsContinue() )
					{
						int adventuresToUse = Math.min( maximumCount, itemCount - HermitRequest.getWorthlessItemCount() );
						this.executeAdventureRequest( adventuresToUse + " unlucky sewer" );
						maximumCount -= adventuresToUse;
					}
				}

				conditions.addAll( temporary );
				if ( HermitRequest.getWorthlessItemCount() < itemCount )
				{
					if ( KoLmafia.permitsContinue() )
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
			this.executePrintCommand( parameters );
			return;
		}


		if ( command.equals( "!" ) )
		{
			for ( int i = 819; i <= 827; ++i )
			{
				String potion = TradeableItemDatabase.getItemName( i );
				potion = potion.substring( 0, potion.length() - 7 );
				RequestLogger.printLine( potion + ": " + StaticEntity.getProperty( "lastBangPotion" + i ) );
			}

			return;
		}

		if ( command.equals( "logprint" ) )
		{
			this.executePrintCommand( parameters, true );
			return;
		}

		// One command is an item usage request.  These
		// requests are complicated, so delegate to the
		// appropriate utility method.

		if ( command.equals( "eat" ) || command.equals( "drink" ) || command.equals( "use" ) || command.equals( "hobodrink" ) )
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

		// Another item-related command is the autosell
		// request.  This one's simple, but it still
		// gets its own utility method.

		if ( command.equals( "mallsell" ) || command.equals( "automall" ) )
		{
			this.executeAutoMallRequest( parameters );
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
			StaticEntity.getClient().priceItemsAtLowestPrice();
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
				this.executePrintCommand( "familiars " + parameters.substring(4).trim() );
				return;
			}
			else if ( parameters.length() == 0 )
			{
				this.executePrintCommand( "familiars" );
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

			FamiliarData newFamiliar = null;

			// First, try substring matching against the list of
			// familiars.

			for ( int i = 0; i < familiars.size() && newFamiliar == null; ++i )
				if ( KoLDatabase.substringMatches( familiars.get(i).toString(), lowerCaseName, true ) )
					newFamiliar = (FamiliarData) familiars.get(i);

			for ( int i = 0; i < familiars.size() && newFamiliar == null; ++i )
				if ( KoLDatabase.substringMatches( familiars.get(i).toString(), lowerCaseName, false ) )
					newFamiliar = (FamiliarData) familiars.get(i);

			// Boo, no matches.  Now try fuzzy matching, because the
			// end-user might be abbreviating.

			for ( int i = 0; i < familiars.size() && newFamiliar == null; ++i )
				if ( KoLDatabase.fuzzyMatches( familiars.get(i).toString(), lowerCaseName ) )
					newFamiliar = (FamiliarData) familiars.get(i);

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
				this.executePrintCommand( "closet " + parameters.substring(4).trim() );
				return;
			}
			else if ( parameters.length() == 0 )
			{
				this.executePrintCommand( "closet" );
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
				this.executePrintCommand( "outfits " + parameters.substring(4).trim() );
				return;
			}
			else if ( parameters.length() == 0 )
			{
				this.executePrintCommand( "outfits" );
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

			if ( parameters.equals( "" ) )
			{
				StaticEntity.getClient().runBetweenBattleChecks( false );
			}
			else if ( parameters.equalsIgnoreCase( "hp" ) || parameters.equalsIgnoreCase( "health" ) )
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
			else if ( parameters.equals( "" ) || parameters.startsWith( "exec" ) )
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

				String previousMood = StaticEntity.getProperty( "currentMood" );
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
			command.equals( "effects" ) || command.equals( "status" ) || command.equals( "skills" ) || command.equals( "locations" ) || command.equals( "encounters" ) )
		{
			this.executePrintCommand( command + " " + parameters );
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

	public void showHTML( String text )
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
		File resultFolder = new File( ROOT_LOCATION, "attacks" );

		if ( !resultFolder.exists() )
			return;

		File [] attackLogs = resultFolder.listFiles();

		TreeMap minis = new TreeMap();
		updateDisplay( "Scanning attack logs..." );

		for ( int i = 0; i < attackLogs.length; ++i )
		{
			if ( !attackLogs[i].getName().endsWith( "__spreadsheet.txt" ) )
				this.registerFlowerHunterData( minis, KoLDatabase.getReader( attackLogs[i] ) );
		}

		LogStream spreadsheet = LogStream.openStream( new File( resultFolder, "__spreadsheet.txt" ), true );

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
				results = new ProfileRequest[ search.getSearchResults().size() ];
				search.getSearchResults().toArray( results );
			}

			executeFlowerHuntRequest( results, request );

			if ( !KoLmafia.refusesContinue() )
				KoLmafia.forceContinue();
		}

		if ( KoLmafia.permitsContinue() )
			updateDisplay( "You have " + KoLCharacter.getAttacksLeft() + " attacks remaining." );

		RequestThread.closeRequestSequence();
	}

	public static void executeFlowerHuntRequest( ProfileRequest [] targets, FlowerHunterRequest request )
	{
		for ( int i = 0; i < targets.length && KoLmafia.permitsContinue() && KoLCharacter.getAttacksLeft() > 0; ++i )
		{
			if ( KoLCharacter.getPvpRank() - 50 > targets[i].getPvpRank().intValue() )
				continue;

			if ( StaticEntity.getProperty( "currentPvpVictories" ).indexOf( targets[i].getPlayerName() ) != -1 )
				continue;

			if ( ClanManager.getClanName().equals( targets[i].getClanName() ) )
				continue;

			KoLmafia.updateDisplay( "Attacking " + targets[i].getPlayerName() + "..." );
			request.setTarget( targets[i].getPlayerName() );
			RequestThread.postRequest( request );

			if ( request.responseText.indexOf( "Your PvP Ranking decreased by" ) != -1 )
				updateDisplay( ERROR_STATE, "You lost to " + targets[i].getPlayerName() + "." );
			else
				StaticEntity.setProperty( "currentPvpVictories", StaticEntity.getProperty( "currentPvpVictories" ) + targets[i].getPlayerName() + "," );

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

		this.executeSendRequest( splitParameters[1], message, attachments, false );
	}

	public void executeSendRequest( String recipient, String message, Object [] attachments, boolean usingStorage )
	{
		if ( !usingStorage )
		{
			SendMessageRequest.setUpdateDisplayOnFailure( false );
			RequestThread.postRequest( new GreenMessageRequest( recipient, message, attachments, false ) );
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

	public static File findScriptFile( String filename )
	{
		File scriptFile = new File( filename );
		if ( scriptFile.exists() )
			return scriptFile.getAbsoluteFile();

		scriptFile = new File( ROOT_LOCATION, filename );
		if ( scriptFile.exists() )
			return scriptFile.getAbsoluteFile();

		if ( scriptFile.exists() )
			return scriptFile.getAbsoluteFile();

		return findScriptFile( SCRIPT_LOCATION, filename, false );
	}

	private static File findScriptFile( File directory, String filename )
	{	return findScriptFile( directory, filename, false );
	}

	private static File findScriptFile( File directory, String filename, boolean isFallback )
	{
		File scriptFile = new File( directory, filename );

		if ( scriptFile.exists() )
			return scriptFile.getAbsoluteFile();

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

			// If no script was found, perhaps there's parentheses indicating
			// that this is an ASH script invocation.

			if ( scriptFile == null )
			{
				int paren = parameters.indexOf( "(" );
				if ( paren != -1 )
				{
					arguments = this.parseScriptArguments( parameters.substring( paren + 1 ) );
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
					arguments = this.parseScriptArguments( parameters.substring( spaceIndex ).trim() );
					parameters = parameters.substring( 0, spaceIndex );
					scriptFile = findScriptFile( parameters );
				}
			}

			// If not even that, perhaps it's the invocation of a function which
			// is defined in the ASH namespace?

			if ( scriptFile == null )
			{
				NAMESPACE_INTERPRETER.execute( parameters, arguments );
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
	                    RequestLogger.printLine( "Script verification complete." );

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
				return this.parseScriptArguments( parameters, "," );
		}

		if ( rparen != -1 || parameters.indexOf( "\"" ) != -1 )
			return new String [] { parameters };

		return this.parseScriptArguments( parameters, " " );
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

				if ( i == 0 && right.charAt(0) == '-' )
					continue;

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
				condition = this.extractCondition( conditionsList[i] );
				if ( condition == null )
					continue;

				if ( condition.isItem() && option.equals( "set" ) )
				{
					int currentAmount = condition.getCount( inventory ) + condition.getCount( closet );
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

			return true;
		}

		printList( conditions );
		return false;
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
			condition = getFirstMatchingItem( conditionString );

			if ( condition == null )
			{
				condition = MonsterDatabase.findMonster( conditionString );
				if ( condition != null )
					FightRequest.searchForMonster( (Monster) condition );
			}
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
		String [] splitParameters = parameters.replaceFirst( " [oO][nN] ", " => " ).split( " => " );

		if ( splitParameters.length == 1 )
		{
			splitParameters = new String[2];
			splitParameters[0] = parameters;
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
			this.executePrintCommand( "equipment" );
			return;
		}

		if ( parameters.startsWith( "list" ) )
		{
			this.executePrintCommand( "equipment " + parameters.substring(4).trim() );
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

	private void executePrintCommand( String parameters )
	{	this.executePrintCommand( parameters, false );
	}

	/**
	 * A special module used specifically for properly printing out
	 * data relevant to the current session.
	 */

	private void executePrintCommand( String parameters, boolean sessionPrint )
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

		PrintStream desiredOutputStream = sessionPrint ? RequestLogger.getSessionStream() : RequestLogger.INSTANCE;

		if ( !filter.equals( "" ) &&
			(parameters.startsWith( "summary" ) || parameters.startsWith( "session" ) || parameters.startsWith( "stat" ) || parameters.startsWith( "equip" ) || parameters.startsWith( "encounters" ) || parameters.startsWith( "locations" ) ) )
		{
			desiredOutputStream = LogStream.openStream( new File( ROOT_LOCATION, filter ), false );
		}

		this.executePrintCommand( list, filter, desiredOutputStream );

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

	private void executePrintCommand( String desiredData, String filter, PrintStream desiredStream )
	{
		desiredStream.println();

		if ( desiredData.equals( "session" ) )
		{
			desiredStream.println( "Player: " + KoLCharacter.getUserName() );
			desiredStream.println( "Session Id: " + KoLRequest.sessionId );
			desiredStream.println( "Password Hash: " + KoLRequest.passwordHash );
			desiredStream.println( "Current Server: " + KoLRequest.KOL_HOST );
		}
		else if ( desiredData.startsWith( "stat" ) )
		{
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

			desiredStream.println( "Pet: " + KoLCharacter.getFamiliar() );
			desiredStream.println( "Item: " + KoLCharacter.getFamiliarItem() );
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
		else if ( desiredData.startsWith( "encounters" ) )
		{
			desiredStream.println( "Encounter Listing: " );

			desiredStream.println();
			printList( encounterList, desiredStream );
		}
		else if ( desiredData.startsWith( "locations" ) )
		{
			desiredStream.println( "Visited Locations: " );
			desiredStream.println();

			printList( adventureList, desiredStream );
		}
		else
		{
			List mainList = desiredData.equals( "closet" ) ? closet : desiredData.equals( "summary" ) ? tally :
				desiredData.equals( "storage" ) ? storage : desiredData.equals( "outfits" ) ? KoLCharacter.getOutfits() :
				desiredData.equals( "familiars" ) ? KoLCharacter.getFamiliarList() :
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
					currentItem = items[i].toString().toLowerCase();
					if ( currentItem.indexOf( filter ) != -1 )
						resultList.add( items[i] );
				}

				printList( resultList, desiredStream );
			}
		}

		desiredStream.println();
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
		if ( nameList == null )
			return -1;

		if ( nameList.isEmpty() )
			return -1;

		if ( nameList.size() == 1 )
			return TradeableItemDatabase.getItemId( (String) nameList.get(0) );

		int actualCount = nameList.size();
		int lowestId = Integer.MAX_VALUE;

		boolean npcStoreMatch = false;
		boolean isRestoreMatch = false;

		String itemName;
		int itemId, useType;
		MallPurchaseRequest npcstore;

		for ( int i = 0; i < nameList.size(); ++i )
		{
			itemName = (String) nameList.get(i);
			itemId = TradeableItemDatabase.getItemId( itemName );
			useType = TradeableItemDatabase.getConsumptionType( itemId );

			if ( isUsageMatch )
			{
				switch ( useType )
				{
				case CONSUME_EAT:
				case CONSUME_DRINK:
				case CONSUME_USE:
				case CONSUME_MULTIPLE:
				case HP_RESTORE:
				case MP_RESTORE:
					break;

				default:
					--actualCount;
					continue;
				}
			}

			if ( (isCreationMatch || isUntinkerMatch) && ConcoctionsDatabase.getMixingMethod( itemId ) == NOCREATE )
			{
				if ( itemId != MEAT_PASTE && itemId != MEAT_STACK && itemId != DENSE_STACK )
				{
					--actualCount;
					continue;
				}
			}

			npcstore = NPCStoreDatabase.getPurchaseRequest( itemName );

			if ( npcstore != null )
			{
				if ( useType == MP_RESTORE )
				{
					if ( !isRestoreMatch || itemId < lowestId )
						lowestId = itemId;

					isRestoreMatch = true;
					npcStoreMatch = true;
				}
				else if ( useType == HP_RESTORE )
				{
					if ( !isRestoreMatch || itemId < lowestId )
						lowestId = itemId;

					isRestoreMatch = true;
					npcStoreMatch = true;
				}
				else if ( isRestoreMatch )
				{
				}
				else if ( !npcstore.getURLString().startsWith( "town_gift" ) )
				{
					if ( !npcStoreMatch || TradeableItemDatabase.getPriceById( itemId ) < TradeableItemDatabase.getPriceById( lowestId ) )
						lowestId = itemId;

					npcStoreMatch = true;
				}
			}
			else if ( !npcStoreMatch )
			{
				if ( itemId < lowestId )
					lowestId = itemId;
			}
		}

		if ( lowestId == Integer.MAX_VALUE )
			return -1;

		if ( npcStoreMatch || actualCount == 1 )
			return lowestId;

		if ( !isUsageMatch )
			return 0;

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

	private static List getMatchingItemNames( String itemName )
	{	return TradeableItemDatabase.getMatchingNames( itemName );
	}

	/**
	 * Utility method which determines the first item which matches
	 * the given parameter string.  Note that the string may also
	 * specify an item quantity before the string.
	 */

	public static AdventureResult getFirstMatchingItem( String parameters )
	{	return getFirstMatchingItem( parameters, true );
	}

	public static AdventureResult getFirstMatchingItem( String parameters, boolean errorOnFailure )
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
			else if ( TradeableItemDatabase.contains( parameters ) )
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

		itemId = TradeableItemDatabase.getItemId( parameters );

		if ( itemId == -1 )
		{
			List matchingNames = getMatchingItemNames( parameters );

			// Next, check to see if any of the items matching appear
			// in an NPC store.  If so, automatically default to it.

			if ( !matchingNames.isEmpty() )
				itemId = getFirstMatchingItemId( matchingNames );

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
			matchCount = firstMatch.getCount( inventory );

		// In the event that the person wanted all except a certain
		// quantity, be sure to update the item count.

		if ( itemCount <= 0 )
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

		if ( KoLCharacter.inMysticalitySign() )
			RequestThread.postRequest( new MindControlRequest( setting ) );
		else if ( KoLCharacter.inMuscleSign() )
			RequestThread.postRequest( new DetunedRadioRequest( setting ) );
		else if ( KoLCharacter.inMoxieSign() )
			RequestThread.postRequest( new AnnoyotronRequest( setting ) );

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
		Object [] items = this.getMatchingItemList( parameters );
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

		Object [] itemList = this.getMatchingItemList( parameters.substring(4).trim() );
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

	private void executeAutoMallRequest( String parameters )
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
			StaticEntity.getClient().makePurchases( results, results.toArray(), match.getCount() );
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
		if ( this.currentLine.startsWith( "eat" ) && this.makeRestaurantRequest( parameters ) )
			return;

		if ( this.currentLine.startsWith( "drink" ) && this.makeMicrobreweryRequest( parameters ) )
			return;

		// Now, handle the instance where the first item is actually
		// the quantity desired, and the next is the amount to use

		AdventureResult currentMatch;

		isUsageMatch = true;
		Object [] itemList = this.getMatchingItemList( parameters );
		isUsageMatch = false;

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

			if ( this.currentLine.startsWith( "drink" ) || this.currentLine.startsWith( "hobodrink" ) )
			{
				if ( TradeableItemDatabase.getConsumptionType( currentMatch.getItemId() ) != CONSUME_DRINK )
				{
					updateDisplay( ERROR_STATE, currentMatch.getName() + " is not an alcoholic beverage." );
					return;
				}
			}

			if ( this.currentLine.startsWith( "use" ) && !StaticEntity.getBooleanProperty( "allowGenericUse" ) )
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

			ConsumeItemRequest request = !this.currentLine.startsWith( "hobodrink" ) ? new ConsumeItemRequest( currentMatch ) :
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

		if ( parameters.length() <= 4 )
		{
			printList( collection );
			return;
		}

		Object [] items = this.getMatchingItemList( parameters.substring(4).trim() );
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
		KoLAdventure adventure = AdventureDatabase.getAdventure( parameters.equalsIgnoreCase( "last" ) ? StaticEntity.getProperty( "lastAdventure" ) : parameters );

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

		RequestThread.postRequest( new EquipmentRequest( intendedOutfit ) );
	}

	public static SpecialOutfit getMatchingOutfit( String name )
	{
		String lowercaseOutfitName = name.toLowerCase().trim();
		if ( lowercaseOutfitName.equals( "birthday suit" ) || lowercaseOutfitName.equals( "nothing" ) )
			return SpecialOutfit.BIRTHDAY_SUIT;

		Object [] outfits = new Object[ KoLCharacter.getOutfits().size() ];
		KoLCharacter.getOutfits().toArray( outfits );

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
		BuffBotManager.loadSettings();

		if ( BuffBotManager.getBuffCostTable().isEmpty() )
		{
			updateDisplay( ERROR_STATE, "No sellable buffs defined." );
			return;
		}

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

			RequestLogger.printLine( "This could match any of the following " + matchingEffects.size() + " effects: " );
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
	 * Makes a request to the restaurant to purchase a meal.  If the item
	 * is not available, this method does not report an error.
	 */

	public boolean makeRestaurantRequest( String parameters )
	{
		if ( restaurantItems.isEmpty() && KoLCharacter.inMysticalitySign() )
			RequestThread.postRequest( new RestaurantRequest() );

		if ( parameters.equals( "" ) )
		{
			RequestLogger.printLine( "Today's Special: " + RestaurantRequest.getDailySpecial() );
			return false;
		}

		String [] splitParameters = this.splitCountAndName( parameters );
		String countString = splitParameters[0];
		String nameString = splitParameters[1];

		if ( nameString.equalsIgnoreCase( "daily special" ) )
			nameString = RestaurantRequest.getDailySpecial().getName();

		for ( int i = 0; i < restaurantItems.size(); ++i )
		{
			String name = (String) restaurantItems.get(i);

			if ( !KoLDatabase.substringMatches( name, nameString ) )
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
		if ( microbreweryItems.isEmpty() && KoLCharacter.inMoxieSign() )
			RequestThread.postRequest( new MicrobreweryRequest() );

		if ( parameters.equals( "" ) )
		{
			RequestLogger.printLine( "Today's Special: " + MicrobreweryRequest.getDailySpecial() );
			return false;
		}

		String [] splitParameters = this.splitCountAndName( parameters );
		String countString = splitParameters[0];
		String nameString = splitParameters[1];

		if ( nameString.equalsIgnoreCase( "daily special" ) )
			nameString = MicrobreweryRequest.getDailySpecial().getName();

		for ( int i = 0; i < microbreweryItems.size(); ++i )
		{
			String name = (String) microbreweryItems.get(i);

			if ( !KoLDatabase.substringMatches( name, nameString ) )
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
