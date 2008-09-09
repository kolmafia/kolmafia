/**
 * Copyright (c) 2005-2008, KoLmafia development team
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
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.java.dev.spellcast.utilities.DataUtilities;
import net.java.dev.spellcast.utilities.UtilityConstants;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.AdventureDatabase;
import net.sourceforge.kolmafia.persistence.Aliases;
import net.sourceforge.kolmafia.persistence.BuffBotDatabase;
import net.sourceforge.kolmafia.persistence.ConcoctionDatabase;
import net.sourceforge.kolmafia.persistence.EffectDatabase;
import net.sourceforge.kolmafia.persistence.EquipmentDatabase;
import net.sourceforge.kolmafia.persistence.HolidayDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.persistence.ItemFinder;
import net.sourceforge.kolmafia.persistence.NPCStoreDatabase;
import net.sourceforge.kolmafia.persistence.Preferences;
import net.sourceforge.kolmafia.persistence.SkillDatabase;
import net.sourceforge.kolmafia.request.BasementRequest;
import net.sourceforge.kolmafia.request.CampgroundRequest;
import net.sourceforge.kolmafia.request.CharPaneRequest;
import net.sourceforge.kolmafia.request.ChezSnooteeRequest;
import net.sourceforge.kolmafia.request.ClanRumpusRequest;
import net.sourceforge.kolmafia.request.ClanStashRequest;
import net.sourceforge.kolmafia.request.ClosetRequest;
import net.sourceforge.kolmafia.request.CreateItemRequest;
import net.sourceforge.kolmafia.request.DisplayCaseRequest;
import net.sourceforge.kolmafia.request.EquipmentRequest;
import net.sourceforge.kolmafia.request.FamiliarRequest;
import net.sourceforge.kolmafia.request.FightRequest;
import net.sourceforge.kolmafia.request.FriarRequest;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.request.HellKitchenRequest;
import net.sourceforge.kolmafia.request.HermitRequest;
import net.sourceforge.kolmafia.request.IslandArenaRequest;
import net.sourceforge.kolmafia.request.LoginRequest;
import net.sourceforge.kolmafia.request.LogoutRequest;
import net.sourceforge.kolmafia.request.MicroBreweryRequest;
import net.sourceforge.kolmafia.request.MindControlRequest;
import net.sourceforge.kolmafia.request.ProfileRequest;
import net.sourceforge.kolmafia.request.PulverizeRequest;
import net.sourceforge.kolmafia.request.PvpRequest;
import net.sourceforge.kolmafia.request.RaffleRequest;
import net.sourceforge.kolmafia.request.SellStuffRequest;
import net.sourceforge.kolmafia.request.SendGiftRequest;
import net.sourceforge.kolmafia.request.SendMailRequest;
import net.sourceforge.kolmafia.request.ShrineRequest;
import net.sourceforge.kolmafia.request.StyxPixieRequest;
import net.sourceforge.kolmafia.request.TelescopeRequest;
import net.sourceforge.kolmafia.request.TransferItemRequest;
import net.sourceforge.kolmafia.request.UneffectRequest;
import net.sourceforge.kolmafia.request.UntinkerRequest;
import net.sourceforge.kolmafia.request.UseItemRequest;
import net.sourceforge.kolmafia.request.UseSkillRequest;
import net.sourceforge.kolmafia.request.ZapRequest;
import net.sourceforge.kolmafia.session.BreakfastManager;
import net.sourceforge.kolmafia.session.BuffBotManager;
import net.sourceforge.kolmafia.session.ClanManager;
import net.sourceforge.kolmafia.session.CustomCombatManager;
import net.sourceforge.kolmafia.session.EquipmentManager;
import net.sourceforge.kolmafia.session.EventManager;
import net.sourceforge.kolmafia.session.InventoryManager;
import net.sourceforge.kolmafia.session.LeafletManager;
import net.sourceforge.kolmafia.session.MoodManager;
import net.sourceforge.kolmafia.session.MushroomManager;
import net.sourceforge.kolmafia.session.NemesisManager;
import net.sourceforge.kolmafia.session.PvpManager;
import net.sourceforge.kolmafia.session.SorceressLairManager;
import net.sourceforge.kolmafia.session.StoreManager;
import net.sourceforge.kolmafia.session.TurnCounter;
import net.sourceforge.kolmafia.swingui.BuffRequestFrame;
import net.sourceforge.kolmafia.swingui.CalendarFrame;
import net.sourceforge.kolmafia.swingui.CouncilFrame;
import net.sourceforge.kolmafia.swingui.FamiliarTrainingFrame;
import net.sourceforge.kolmafia.swingui.widget.ShowDescriptionList;
import net.sourceforge.kolmafia.textui.Interpreter;
import net.sourceforge.kolmafia.utilities.ByteArrayStream;
import net.sourceforge.kolmafia.utilities.CharacterEntities;
import net.sourceforge.kolmafia.utilities.FileUtilities;
import net.sourceforge.kolmafia.utilities.PauseObject;
import net.sourceforge.kolmafia.utilities.StringUtilities;
import net.sourceforge.kolmafia.webui.StationaryButtonDecorator;

public class KoLmafiaCLI
	extends KoLmafia
{
	public static final KoLmafiaCLI DEFAULT_SHELL = new KoLmafiaCLI( System.in );

	private static final Pattern HTMLTAG_PATTERN = Pattern.compile( "<.*?>", Pattern.DOTALL );
	private static final Pattern ASHNAME_PATTERN = Pattern.compile( "\\.ash", Pattern.CASE_INSENSITIVE );
	private static final Pattern STATDAY_PATTERN = Pattern.compile( "(today|tomorrow) is (.*?) day" );
	private static final Pattern MEAT_PATTERN = Pattern.compile( "[\\d,]+ meat" );
	private static final Pattern SCRIPT_PATTERN = Pattern.compile( "<script.*?</script>", Pattern.DOTALL );
	private static final Pattern COMMENT_PATTERN = Pattern.compile( "<!--.*?-->", Pattern.DOTALL );

	private String previousLine = null;
	private String currentLine = null;
	private BufferedReader commandStream;

	public static boolean isExecutingCheckOnlyCommand = false;

	public static final void initialize()
	{
		System.out.println();
		System.out.println( StaticEntity.getVersion() );
		System.out.println( "Running on " + System.getProperty( "os.name" ) );
		System.out.println( "Using Java " + System.getProperty( "java.version" ) );
		System.out.println();

		StaticEntity.setClient( KoLmafiaCLI.DEFAULT_SHELL );
		RequestLogger.openStandard();
	}

	/**
	 * Constructs a new <code>KoLmafiaCLI</code> object. All data fields are initialized to their default values, the
	 * global settings are loaded from disk.
	 */

	public KoLmafiaCLI( final InputStream inputStream )
	{
		try
		{
			this.commandStream = FileUtilities.getReader( inputStream );
		}
		catch ( Exception e )
		{
			// This should not happen.  Therefore, print
			// a stack trace for debug purposes.

			StaticEntity.printStackTrace( e, "Error opening input stream." );
		}
	}

	/**
	 * Utility method used to prompt the user for their login and password. Later on, when profiles are added, prompting
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
			{
				username = username.substring( 6 ).trim();
			}

			String password = KoLmafia.getSaveState( username );

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
	 * Initializes the <code>KoLmafia</code> session. Called after the login has been confirmed to notify the client
	 * that the login was successful, the user-specific settings should be loaded, and the user can begin adventuring.
	 */

	public void initialize( final String username )
	{
		super.initialize( username );

		try
		{
			String holiday =
				HolidayDatabase.getHoliday(
					KoLConstants.DAILY_FORMAT.parse( KoLConstants.DAILY_FORMAT.format( new Date() ) ), true );
			KoLmafia.updateDisplay( holiday + ", " + HolidayDatabase.getMoonEffect() );
		}
		catch ( Exception e )
		{
			// Should not happen, you're parsing something that
			// was formatted the same way.

			StaticEntity.printStackTrace( e );
		}

		if ( Preferences.getString( "initialFrames" ).indexOf( "LocalRelayServer" ) != -1 )
		{
			KoLmafiaGUI.constructFrame( "LocalRelayServer" );
		}
	}

	/**
	 * A utility method which waits for commands from the user, then executing each command as it arrives.
	 */

	public void listenForCommands()
	{
		KoLmafia.forceContinue();

		if ( StaticEntity.getClient() == this )
		{
			System.out.print( " > " );
		}

		String line = null;

		while ( KoLmafia.permitsContinue() && ( line = this.getNextLine() ) != null )
		{
			if ( StaticEntity.getClient() == this )
			{
				RequestLogger.printLine();
			}

			this.executeLine( line );

			if ( StaticEntity.getClient() == this )
			{
				RequestLogger.printLine();
				System.out.print( " > " );
			}

			if ( StaticEntity.getClient() == this )
			{
				KoLmafia.forceContinue();
			}
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
				line = this.commandStream.readLine();
				if ( line == null )
				{
					return null;
				}
				line = line.trim();
			}
			while ( line.length() == 0 || line.startsWith( "#" ) || line.startsWith( "//" ) || line.startsWith( "\'" ) );

			// You will either have reached the end of file, or you
			// will have a valid line -- return it.

			return line;
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
		if ( line == null || KoLmafia.refusesContinue() )
		{
			return;
		}

		line = CharacterEntities.unescape( line );

		line = line.replaceAll( "[ \t]+", " " ).trim();
		if ( line.length() == 0 )
		{
			return;
		}

		// First, handle all the aliasing that may be
		// defined by the user.

		this.currentLine = line = Aliases.apply( line );

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
				while ( KoLmafiaCLI.testConditional( condition ) )
				{
					this.executeLine( statement.toString() );
				}
			}
			else if ( KoLmafiaCLI.testConditional( condition ) )
			{
				this.executeLine( statement.toString() );
			}

			this.previousLine = condition + ";" + statement;
			return;
		}

		// Check to see if we can execute the line iteratively, which
		// is possible whenever if-statements aren't involved.

		int splitIndex = line.indexOf( ";" );

		if ( splitIndex != -1 && !line.startsWith( "set" ) && !line.startsWith( "alias" ) )
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

				current = remainder.substring( 0, splitIndex ).trim();
				if ( current.length() > 0 )
				{
					sequenceList.add( current );
				}
				remainder = remainder.substring( splitIndex + 1 ).trim();
				splitIndex = remainder.indexOf( ";" );
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
				String[] sequence = new String[ sequenceList.size() ];
				sequenceList.toArray( sequence );

				boolean canExecuteIteratively = true;

				for ( int i = 0; canExecuteIteratively && i < sequence.length; ++i )
				{
					canExecuteIteratively =
						!sequence[ i ].toLowerCase().startsWith( "if " ) && !sequence[ i ].toLowerCase().startsWith(
							"while " );
				}

				if ( canExecuteIteratively )
				{
					for ( int i = 0; i < sequence.length && KoLmafia.permitsContinue(); ++i )
					{
						this.executeLine( sequence[ i ] );
					}
				}
				else
				{
					// Handle multi-line sequences by executing the first command
					// and using recursion to execute the remainder of the line.
					// This ensures that nested if-statements are preserved.

					String part1 = line.substring( 0, sequence[ 0 ].length() ).trim();
					String part2 = line.substring( sequence[ 0 ].length() + 1 ).trim();

					this.executeLine( part1 );

					if ( KoLmafia.permitsContinue() )
					{
						this.executeLine( part2 );
					}
				}

				this.previousLine = line;
				return;
			}

			// If there are zero or one, then you either do nothing or you
			// continue on with the revised line.

			if ( sequenceList.isEmpty() )
			{
				return;
			}

			line = (String) sequenceList.get( 0 );
		}

		// Win game sanity check.  This will find its
		// way back into the GUI ... one day.

		if ( line.equalsIgnoreCase( "win game" ) )
		{
			String[] messages =
				KoLConstants.WIN_GAME_TEXT[ KoLConstants.RNG.nextInt( KoLConstants.WIN_GAME_TEXT.length ) ];

			PauseObject pauser = new PauseObject();
			KoLmafia.updateDisplay( "Executing top-secret 'win game' script..." );
			pauser.pause( 3000 );

			for ( int i = 0; i < messages.length - 1; ++i )
			{
				KoLmafia.updateDisplay( messages[ i ] );
				pauser.pause( 3000 );
			}

			KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, messages[ messages.length - 1 ] );
			return;
		}

		// Maybe a request to burn excess MP, as generated
		// by the gCLI or the relay browser?

		if ( line.equalsIgnoreCase( "save as mood" ) )
		{
			MoodManager.minimalSet();
			MoodManager.saveSettings();
			return;
		}

		if ( line.equalsIgnoreCase( "burn extra mp" ) )
		{
			this.recoverHP();

			SpecialOutfit.createImplicitCheckpoint();
			MoodManager.burnExtraMana( true );
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
					ostream.write( KoLConstants.LINE_BREAK.getBytes() );
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
				KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "Unterminated inline ASH script." );
				return;
			}

			Interpreter interpreter = new Interpreter();
			interpreter.validate( null, ostream.getByteArrayInputStream() );
			interpreter.execute( "main", null );

			return;
		}

		// Not a special full-line command.  Go ahead and
		// split the command into extra pieces.

		String command = line.trim().split( " " )[ 0 ].toLowerCase();
		String parameters = line.trim().substring( command.length() ).trim();

		if ( command.endsWith( "?" ) )
		{
			KoLmafiaCLI.isExecutingCheckOnlyCommand = true;
			command = command.substring( 0, command.length() - 1 );
		}

		RequestThread.openRequestSequence();
		this.executeCommand( command, parameters );
		RequestThread.closeRequestSequence();

		if ( !command.equals( "repeat" ) )
		{
			this.previousLine = line;
		}

		KoLmafiaCLI.isExecutingCheckOnlyCommand = false;
	}

	/**
	 * A utility command which decides, based on the command to be executed, what to be done with it. It can either
	 * delegate this to other functions, or do it itself.
	 */

	public void executeCommand( String command, String parameters )
	{
		if ( command.equals( "gc" ) )
		{
			System.gc();
			return;
		}

		if ( command.equals( "version" ) )
		{
			RequestLogger.printLine( StaticEntity.getVersion() );
			return;
		}

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

		if ( command.equals( "logecho" ) || command.equals( "logprint" ) )
		{
			if ( parameters.equalsIgnoreCase( "timestamp" ) )
			{
				parameters = HolidayDatabase.getCalendarDayAsString( new Date() );
			}

			parameters = StringUtilities.globalStringDelete( StringUtilities.globalStringDelete( parameters, "\n" ), "\r" );
			parameters = StringUtilities.globalStringReplace( parameters, "<", "&lt;" );

			RequestLogger.getSessionStream().println( " > " + parameters );
		}

		if ( command.equals( "log" ) )
		{
			if ( parameters.equals( "snapshot" ) )
			{
				this.executeCommand( "log", "moon, status, equipment, skills, effects, modifiers" );
				return;
			}

			RequestLogger.updateSessionLog();
			RequestLogger.updateSessionLog( "=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=" );

			RequestLogger.getDebugStream().println();
			RequestLogger.getDebugStream().println( "=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=" );

			StringBuffer title = new StringBuffer( "Player Snapshot" );

			int leftIndent = ( 46 - title.length() ) / 2;
			for ( int i = 0; i < leftIndent; ++i )
			{
				title.insert( 0, ' ' );
			}

			RequestLogger.updateSessionLog( title.toString() );
			RequestLogger.updateSessionLog( "=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=" );

			RequestLogger.getDebugStream().println( title.toString() );
			RequestLogger.getDebugStream().println( "=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=" );

			String[] options = parameters.split( "\\s*,\\s*" );

			for ( int i = 0; i < options.length; ++i )
			{
				RequestLogger.updateSessionLog();
				RequestLogger.updateSessionLog( " > " + options[ i ] );

				this.showData( options[ i ], true );
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
			if ( parameters.length() == 0 )
			{
				Aliases.print();
				return;
			}

			int spaceIndex = parameters.indexOf( " => " );
			if ( spaceIndex == -1 )
			{
				KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "That was not a valid aliasing." );
				return;
			}

			String aliasString = parameters.substring( 0, spaceIndex ).trim();
			String aliasCommand = parameters.substring( spaceIndex + 4 ).trim();
			Aliases.add( aliasString, aliasCommand );

			RequestLogger.printLine( "String successfully aliased." );
			RequestLogger.printLine( aliasString + " => " + aliasCommand );
			return;
		}

		if ( command.equals( "unalias" ) )
		{
			Aliases.remove( parameters );
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

			if ( KoLmafiaCLI.findScriptFile( command ) != null )
			{
				this.executeScript( command );
				return;
			}

			if ( command.equals( "chat" ) )
			{
				KoLmafiaGUI.constructFrame( "ChatManager" );
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
		// GenericRequest object to handle it yet?

		if ( command.startsWith( "http:" ) || command.indexOf( ".php" ) != -1 )
		{
			GenericRequest visitor = new GenericRequest( this.currentLine );
			if ( GenericRequest.shouldIgnore( visitor ) )
			{
				return;
			}

			RequestThread.postRequest( visitor );
			StaticEntity.externalUpdate( visitor.getURLString(), visitor.responseText );
			return;
		}

		// Allow a version which lets you see the resulting
		// text without loading a mini/relay browser window.

		if ( command.equals( "text" ) )
		{
			GenericRequest visitor = new GenericRequest( parameters );
			if ( GenericRequest.shouldIgnore( visitor ) )
			{
				return;
			}

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
			int seconds = parameters.equals( "" ) ? 1 : StringUtilities.parseInt( parameters );
			StaticEntity.executeCountdown( "Countdown: ", seconds );

			KoLmafia.updateDisplay( "Waiting completed." );
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
			ItemDatabase.findItemDescriptions();
			EffectDatabase.findStatusEffects();
			RequestLogger.printLine( "Data tables updated." );
			return;
		}

		if ( command.equals( "checkdata" ) )
		{
			int itemId = StringUtilities.parseInt( parameters );
			ItemDatabase.checkInternalData( itemId );
			RequestLogger.printLine( "Internal data checked." );
			return;
		}

		if ( command.equals( "checkplurals" ) )
		{
			int itemId = StringUtilities.parseInt( parameters );
			ItemDatabase.checkPlurals( itemId );
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
			KoLConstants.commandBuffer.clearBuffer();
			return;
		}

		if ( command.equals( "abort" ) )
		{
			KoLmafia.updateDisplay( KoLConstants.ABORT_STATE, parameters.length() == 0 ? "Script abort." : parameters );
			return;
		}

		// Adding the requested echo command.  I guess this is
		// useful for people who want to echo things...

		if ( command.equals( "cecho" ) || command.equals( "colorecho" ) )
		{
			int spaceIndex = parameters.indexOf( " " );
			String color = "#000000";

			if ( spaceIndex != -1 )
			{
				color = parameters.substring( 0, spaceIndex ).replaceAll( "[\">]", "" );
			}

			parameters = parameters.substring( spaceIndex + 1 );
			RequestLogger.printLine( "<font color=\"" + color + "\">" + StringUtilities.globalStringReplace(
				parameters, "<", "&lt;" ) + "</font>" );

			return;
		}

		if ( command.equals( "events" ) )
		{
			if ( parameters.equals( "clear" ) )
			{
				EventManager.clearEventHistory();
			}
			else
			{
				RequestLogger.printList( EventManager.getEventHistory() );
			}

			return;
		}

		if ( command.equals( "echo" ) || command.equals( "print" ) )
		{
			if ( parameters.equalsIgnoreCase( "timestamp" ) )
			{
				parameters = HolidayDatabase.getCalendarDayAsString( new Date() );
			}

			parameters = StringUtilities.globalStringDelete( StringUtilities.globalStringDelete( parameters, "\n" ), "\r" );
			parameters = StringUtilities.globalStringReplace( parameters, "<", "&lt;" );

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
				if ( Preferences.isUserEditable( parameters ) )
				{
					RequestLogger.printLine( Preferences.getString( parameters ) );
				}

				return;
			}

			String name = parameters.substring( 0, splitIndex ).trim();
			if ( !Preferences.isUserEditable( name ) )
			{
				return;
			}

			String value = parameters.substring( splitIndex + 1 ).trim();
			if ( value.startsWith( "\"" ) )
			{
				value = value.substring( 1, value.endsWith( "\"" ) ? value.length() - 1 : value.length() );
			}

			while ( value.endsWith( ";" ) )
			{
				value = value.substring( 0, value.length() - 1 ).trim();
			}

			if ( name.equals( "battleAction" ) )
			{
				if ( value.indexOf( ";" ) != -1 || value.startsWith( "consult" ) )
				{
					CustomCombatManager.setDefaultAction( value );
					value = "custom combat script";
				}
				else
				{
					value = CustomCombatManager.getLongCombatOptionName( value );
				}

				// Special handling of the battle action property,
				// such that auto-recovery gets reset as needed.

				if ( name.equals( "battleAction" ) && value != null )
				{
					KoLCharacter.getBattleSkillNames().setSelectedItem( value );
				}
			}

			if ( name.equals( "defaultAutoAttack" ) )
			{
				CustomCombatManager.setAutoAttack( value );
				return;
			}

			if ( name.equals( "customCombatScript" ) )
			{
				while ( name.endsWith( ".css" ) )
				{
					name = name.substring( 0, name.length() - 4 );
				}

				CustomCombatManager.setScript( value );
				return;
			}

			if ( name.startsWith( "combatHotkey" ) )
			{
				String desiredValue = CustomCombatManager.getLongCombatOptionName( value );

				if ( !desiredValue.startsWith( "attack" ) || value.startsWith( "attack" ) )
				{
					value = desiredValue;
				}
			}

			if ( Preferences.getString( name ).equals( value ) )
			{
				return;
			}

			RequestLogger.printLine( name + " => " + value );
			Preferences.setString( name, value );

			if ( name.startsWith( "combatHotkey" ) )
			{
				StationaryButtonDecorator.reloadCombatHotkeyMap();
			}

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
			RequestThread.postRequest( new LogoutRequest() );

			this.attemptLogin( parameters );
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
			KoLmafia.quit();
		}

		// Next, handle any requests for script execution;
		// these can be done at any time (including before
		// login), so they should be handled before a test
		// of login state needed for other commands.

		if ( command.equals( "namespace" ) )
		{
			// Validate the script first.

			String[] scripts = Preferences.getString( "commandLineNamespace" ).split( "," );
			for ( int i = 0; i < scripts.length; ++i )
			{
				RequestLogger.printLine( scripts[ i ] );
				File f = KoLmafiaCLI.findScriptFile( scripts[ i ] );
				if ( f == null )
				{
					continue;
				}

				Interpreter interpreter = KoLmafiaASH.getInterpreter( f );
				if ( interpreter != null )
				{
					KoLmafiaASH.showUserFunctions( interpreter, parameters );
				}

				RequestLogger.printLine();
			}

			return;
		}

		if ( command.equals( "ashref" ) )
		{
			KoLmafiaASH.showExistingFunctions( parameters );
			return;
		}

		if ( command.equals( "using" ) )
		{
			// Validate the script first.

			this.executeCommand( "validate", parameters );
			if ( !KoLmafia.permitsContinue() )
			{
				return;
			}

			String namespace = Preferences.getString( "commandLineNamespace" );
			if ( namespace.startsWith( parameters + "," ) || namespace.endsWith( "," + parameters ) || namespace.indexOf( "," + parameters + "," ) != -1 )
			{
				return;
			}

			if ( namespace.toString().equals( "" ) )
			{
				namespace = parameters;
			}
			else
			{
				namespace = namespace + "," + parameters;
			}

			Preferences.setString( "commandLineNamespace", namespace.toString() );
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
				int repeatCount = parameters.length() == 0 ? 1 : StringUtilities.parseInt( parameters );
				for ( int i = 0; i < repeatCount && KoLmafia.permitsContinue(); ++i )
				{
					RequestLogger.printLine( "Repetition " + ( i + 1 ) + " of " + repeatCount + "..." );
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
			{
				RequestLogger.closeDebugLog();
			}
			else
			{
				RequestLogger.openDebugLog();
			}

			return;
		}

		// Next, handle requests to start or stop
		// the mirror stream.

		if ( command.indexOf( "mirror" ) != -1 )
		{
			if ( command.indexOf( "end" ) != -1 || command.indexOf( "stop" ) != -1 || command.indexOf( "close" ) != -1 || parameters.length() == 0 || parameters.equals( "end" ) || parameters.equals( "stop" ) || parameters.equals( "close" ) )
			{
				RequestLogger.closeMirror();
				KoLmafia.updateDisplay( "Mirror stream closed." );
			}
			else
			{
				if ( !parameters.endsWith( ".txt" ) )
				{
					parameters += ".txt";
				}

				RequestLogger.openMirror( parameters );
			}

			return;
		}

		if ( command.equals( "wiki" ) || command.equals( "lookup" ) )
		{
			if ( command.equals( "lookup" ) )
			{
				List names = EffectDatabase.getMatchingNames( parameters );
				if ( names.size() == 1 )
				{
					AdventureResult result = new AdventureResult( (String) names.get( 0 ), 1, true );
					ShowDescriptionList.showWikiDescription( result );
					return;
				}

				AdventureResult result = ItemFinder.getFirstMatchingItem( parameters, ItemFinder.ANY_MATCH );
				if ( result != null )
				{
					ShowDescriptionList.showWikiDescription( result );
					return;
				}
			}

			StaticEntity.openSystemBrowser( "http://kol.coldfront.net/thekolwiki/index.php/Special:Search?search=" +
				StringUtilities.getURLEncode( parameters ) + "&go=Go" );

			return;
		}

		if ( command.equals( "safe" ) )
		{
			KoLAdventure location = AdventureDatabase.getAdventure( parameters );
			if ( location == null )
			{
				return;
			}

			AreaCombatData data = AdventureDatabase.getAreaCombatData( location.toString() );
			if ( data == null )
			{
				return;
			}

			StringBuffer buffer = new StringBuffer();

			buffer.append( "<html>" );
			data.getSummary( buffer, false );
			buffer.append( "</html>" );

			this.showHTML( "", buffer.toString() );
			return;
		}

		if ( command.equals( "monsters" ) )
		{
			KoLAdventure location = AdventureDatabase.getAdventure( parameters );
			if ( location == null )
			{
				return;
			}

			AreaCombatData data = AdventureDatabase.getAreaCombatData( location.toString() );
			if ( data == null )
			{
				return;
			}

			StringBuffer buffer = new StringBuffer();

			buffer.append( "<html>" );
			data.getMonsterData( buffer, false );
			buffer.append( "</html>" );

			this.showHTML( "", buffer.toString() );
			return;
		}

		// Re-adding the breakfast command, just
		// so people can add it in scripting.

		if ( command.equals( "breakfast" ) )
		{
			BreakfastManager.getBreakfast( false, true );
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
			{
				StaticEntity.getClient().refreshSession();
			}
			else if ( parameters.equals( "status" ) || parameters.equals( "effects" ) )
			{
				RequestThread.postRequest( CharPaneRequest.getInstance() );
			}
			else if ( parameters.equals( "gear" ) || parameters.startsWith( "equip" ) || parameters.equals( "outfit" ) )
			{
				RequestThread.postRequest( new EquipmentRequest( EquipmentRequest.EQUIPMENT ) );
			}
			else if ( parameters.startsWith( "inv" ) )
			{
				RequestThread.postRequest( new EquipmentRequest( EquipmentRequest.CLOSET ) );
			}
			else if ( parameters.equals( "storage" ) )
			{
				RequestThread.postRequest( new ClosetRequest() );
			}
			else if ( parameters.equals( "familiar" ) || parameters.equals( "terrarium" ) )
			{
				RequestThread.postRequest( new FamiliarRequest() );
			}

			this.showData( parameters );
			return;
		}

		// Look!  It's the command to complete the
		// Sorceress entryway.

		if ( command.equals( "entryway" ) )
		{
			SorceressLairManager.completeCloverlessEntryway();
			return;
		}

		// Look!  It's the command to complete the
		// Sorceress hedge maze!  This is placed
		// right after for consistency.

		if ( command.equals( "maze" ) || command.startsWith( "hedge" ) )
		{
			SorceressLairManager.completeHedgeMaze();
			return;
		}

		// Look!  It's the command to fight the guardians
		// in the Sorceress's tower!  This is placed
		// right after for consistency.

		if ( command.equals( "tower" ) || command.equals( "guardians" ) || command.equals( "chamber" ) )
		{
			SorceressLairManager.fightAllTowerGuardians();
			return;
		}

		// Next is the command to rob the strange leaflet.
		// This method invokes the "robLeafletManager" method
		// on the script requestor.

		if ( command.equals( "leaflet" ) )
		{
			LeafletManager.robStrangeLeaflet( !parameters.equals( "nomagic" ) );
			return;
		}

		// Next is the command to face your nemesis.  This
		// method invokes the "faceNemesisManager" method on the
		// script requestor.

		if ( command.equals( "nemesis" ) )
		{
			NemesisManager.faceNemesisManager();
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

			this.showHTML( "council.php", StringUtilities.singleStringReplace(
				CouncilFrame.COUNCIL_VISIT.responseText, "<a href=\"town.php\">Back to Seaside Town</a>", "" ) );

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
				String[] effects = parameters.split( "\\s*,\\s*" );
				for ( int i = 0; i < effects.length; ++i )
				{
					KoLmafiaCLI.DEFAULT_SHELL.executeCommand( "up", effects[ i ] );
				}

				return;
			}

			int effectId = EffectDatabase.getEffectId( parameters );
			if ( effectId != -1 )
			{
				String effect = EffectDatabase.getEffectName( effectId );
				String action = MoodManager.getDefaultAction( "lose_effect", effect );
				if ( action.equals( "" ) )
				{
					KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "No booster known: " + effect );
					return;
				}

				KoLmafiaCLI.DEFAULT_SHELL.executeLine( action );
				return;
			}

			List names = EffectDatabase.getMatchingNames( parameters );
			if ( names.isEmpty() )
			{
				KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "Unknown effect: " + parameters );
				return;
			}

			KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "Ambiguous effect name: " + parameters );
			RequestLogger.printList( names );
			return;
		}

		// Uneffect with martians are related to buffs,
		// so listing them next seems logical.

		if ( command.equals( "shrug" ) || command.equals( "uneffect" ) || command.equals( "remedy" ) )
		{
			if ( parameters.indexOf( "," ) != -1 )
			{
				String[] effects = parameters.split( "\\s*,\\s*" );
				for ( int i = 0; i < effects.length; ++i )
				{
					KoLmafiaCLI.DEFAULT_SHELL.executeCommand( "uneffect", effects[ i ] );
				}

				return;
			}

			this.executeUneffectRequest( parameters );
			return;
		}

		// Add in item retrieval the way KoLmafia handles
		// it internally.

		if ( command.equals( "find" ) || command.equals( "acquire" ) || command.equals( "retrieve" ) )
		{
			AdventureResult item = ItemFinder.getFirstMatchingItem( parameters, ItemFinder.ANY_MATCH );
			if ( item != null )
			{
				InventoryManager.retrieveItem( item, false );
			}

			return;
		}

		// Adding clan management command options inline
		// in the parsing.

		if ( command.equals( "clan" ) )
		{
			if ( parameters.equals( "snapshot" ) )
			{
				ClanManager.takeSnapshot( 20, 10, 5, 0, false, true );
			}
			else if ( parameters.equals( "stashlog" ) )
			{
				ClanManager.saveStashLog();
			}

			return;
		}

		if ( command.equals( "!" ) || command.equals( "bang" ) )
		{
			for ( int i = 819; i <= 827; ++i )
			{
				String potion = ItemDatabase.getItemName( i );
				potion = potion.substring( 0, potion.length() - 7 );
				RequestLogger.printLine( potion + ": " + Preferences.getString( "lastBangPotion" + i ) );
			}

			return;
		}

		if ( command.equals( "insults" ) )
		{
			KoLCharacter.ensureUpdatedPirateInsults();

			RequestLogger.printLine();
			RequestLogger.printLine( "Known insults:" );

			int count = 0;
			for ( int i = 1; i <= 8; ++i )
			{
				String retort = FightRequest.findPirateRetort( i );
				if ( retort != null )
				{
					if ( count == 0 )
					{
						RequestLogger.printLine();
					}
					count += 1;

					RequestLogger.printLine( retort );
				}
			}

			float odds = FightRequest.pirateInsultOdds( count ) * 100.0f;

			if ( count == 0 )
			{
				RequestLogger.printLine( "None." );
			}

			RequestLogger.printLine();
			RequestLogger.printLine( "Since you know " + count + " insult" + ( count == 1 ? "" : "s" ) + ", you have a " + KoLConstants.FLOAT_FORMAT.format( odds ) + "% chance of winning at Insult Beer Pong." );

			return;
		}

		if ( command.equals( "dusty" ) )
		{
			for ( int i = 2271; i <= 2276; ++i )
			{
				String bottle = ItemDatabase.getItemName( i );
				String type = ItemDatabase.dustyBottleType( i );
				RequestLogger.printLine( bottle + ": " + type );
			}

			return;
		}

		if ( command.equals( "demons" ) )
		{
			for ( int i = 0; i < KoLAdventure.DEMON_TYPES.length; ++i )
			{
				String index = String.valueOf( i + 1 );

				RequestLogger.printLine( index + ": " + Preferences.getString( "demonName" + index ) );
				if ( KoLAdventure.DEMON_TYPES[ i ][ 0 ] != null )
				{
					RequestLogger.printLine( " => Found in the " + KoLAdventure.DEMON_TYPES[ i ][ 0 ] );
				}
				RequestLogger.printLine( " => Gives " + KoLAdventure.DEMON_TYPES[ i ][ 1 ] );
			}

			return;
		}

		if ( command.equals( "summon" ) )
		{
			if ( parameters.length() == 0 )
			{
				return;
			}

			if ( !InventoryManager.retrieveItem( ItemPool.BLACK_CANDLE, 3 ) )
			{
				return;
			}

			if ( !InventoryManager.retrieveItem( ItemPool.EVIL_SCROLL ) )
			{
				return;
			}

			String demon = parameters;
			if ( Character.isDigit( parameters.charAt( 0 ) ) )
			{
				demon = Preferences.getString( "demonName" + parameters );
			}
			else
			{
				for ( int i = 0; i < KoLAdventure.DEMON_TYPES.length; ++i )
				{
					if ( parameters.equalsIgnoreCase( KoLAdventure.DEMON_TYPES[ i ][ 0 ] ) )
					{
						demon = Preferences.getString( "demonName" + ( i + 1 ) );
					}
					else if ( parameters.equalsIgnoreCase( KoLAdventure.DEMON_TYPES[ i ][ 1 ] ) )
					{
						demon = Preferences.getString( "demonName" + ( i + 1 ) );
					}
					else if ( parameters.equalsIgnoreCase( Preferences.getString( "demonName" + ( i + 1 ) ) ) )
					{
						demon = Preferences.getString( "demonName" + ( i + 1 ) );
					}
				}
			}

			GenericRequest demonSummon = new GenericRequest( "manor3.php" );
			demonSummon.addFormField( "action", "summon" );
			demonSummon.addFormField( "demonname", demon );

			KoLmafia.updateDisplay( "Summoning " + demon + "..." );
			RequestThread.postRequest( demonSummon );
			RequestThread.enableDisplayIfSequenceComplete();

			return;
		}

		// One command is an item usage request.  These
		// requests are complicated, so delegate to the
		// appropriate utility method.

		if ( command.equals( "eat" ) || command.equals( "drink" ) || command.equals( "use" ) || command.equals( "chew" ) || command.equals( "hobo" ) || command.equals( "ghost" ) )
		{
			SpecialOutfit.createImplicitCheckpoint();
			this.executeUseItemRequest( command, parameters );
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
			this.executeSellStuffRequest( parameters );
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
				this.showData( "familiars " + parameters.substring( 4 ).trim() );
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
				{
					return;
				}

				RequestThread.postRequest( new FamiliarRequest( FamiliarData.NO_FAMILIAR ) );
				return;
			}
			else if ( parameters.indexOf( "(no change)" ) != -1 )
			{
				return;
			}

			List familiarList = KoLCharacter.getFamiliarList();

			String[] familiars = new String[ familiarList.size() ];
			for ( int i = 0; i < familiarList.size(); ++i )
			{
				FamiliarData familiar = (FamiliarData)familiarList.get( i );
				familiars[ i ] = StringUtilities.getCanonicalName( familiar.getRace() );
			}

			List matchList = StringUtilities.getMatchingNames( familiars, parameters );

			if ( matchList.size() > 1 )
			{
				RequestLogger.printList( matchList );
				RequestLogger.printLine();

				KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "[" + parameters + "] has too many matches." );
			}
			else if ( matchList.size() == 1 )
			{
				String race = (String) matchList.get( 0 );
				FamiliarData change = null;
				for ( int i = 0; i < familiars.length; ++i )
				{
					if ( race.equals( familiars[ i] ) )
					{
						change = (FamiliarData)familiarList.get( i );
						break;
					}
				}

				if ( KoLmafiaCLI.isExecutingCheckOnlyCommand )
				{
					RequestLogger.printLine( change.toString() );
				}
				else if ( KoLCharacter.getFamiliar() != null && !KoLCharacter.getFamiliar().equals( change ) )
				{
					RequestThread.postRequest( new FamiliarRequest( change ) );
				}
			}
			else
			{
				KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "You don't have a " + parameters + " for a familiar." );
			}

			return;
		}

		// Another popular command involves managing
		// your player's closet!  Which can be fun.

		if ( command.equals( "closet" ) )
		{
			if ( parameters.equals( "list" ) )
			{
				this.showData( "closet " + parameters.substring( 4 ).trim() );
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
				this.showData( "outfits " + parameters.substring( 4 ).trim() );
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

		if ( command.equals( "buffbot" ) )
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
				String[] splitup = parameters.split( "with limit" );
				parameters = splitup[ 0 ];
				desiredLimit = StringUtilities.parseInt( splitup[ 1 ] );
			}

			StoreManager.searchMall( parameters, results, desiredLimit, true );
			RequestLogger.printList( results );
			return;
		}

		if ( command.equals( "pvp" ) || command.equals( "attack" ) )
		{
			RequestThread.openRequestSequence();
			int stance = 0;

			if ( KoLCharacter.getBaseMuscle() >= KoLCharacter.getBaseMysticality() && KoLCharacter.getBaseMuscle() >= KoLCharacter.getBaseMoxie() )
			{
				stance = 1;
			}
			else if ( KoLCharacter.getBaseMysticality() >= KoLCharacter.getBaseMuscle() && KoLCharacter.getBaseMysticality() >= KoLCharacter.getBaseMoxie() )
			{
				stance = 2;
			}
			else
			{
				stance = 3;
			}

			String[] names = parameters.split( "\\s*,\\s*" );
			ProfileRequest[] targets = new ProfileRequest[ names.length ];

			for ( int i = 0; i < names.length; ++i )
			{
				String playerId = KoLmafia.getPlayerId( names[ i ] );
				if ( !playerId.equals( names[ i ] ) )
				{
					continue;
				}

				BuffRequestFrame.ONLINE_VALIDATOR.addFormField( "playerid", String.valueOf( KoLCharacter.getUserId() ) );
				BuffRequestFrame.ONLINE_VALIDATOR.addFormField( "pwd" );
				BuffRequestFrame.ONLINE_VALIDATOR.addFormField( "graf", "/whois " + names[ i ] );

				RequestThread.postRequest( BuffRequestFrame.ONLINE_VALIDATOR );
				Matcher idMatcher =
					Pattern.compile( "\\(#(\\d+)\\)" ).matcher( BuffRequestFrame.ONLINE_VALIDATOR.responseText );

				if ( idMatcher.find() )
				{
					KoLmafia.registerPlayer( names[ i ], idMatcher.group( 1 ) );
				}
				else
				{
					names[ i ] = null;
				}
			}

			for ( int i = 0; i < names.length; ++i )
			{
				if ( names[ i ] == null )
				{
					continue;
				}

				KoLmafia.updateDisplay( "Retrieving player data for " + names[ i ] + "..." );
				targets[ i ] = new ProfileRequest( names[ i ] );
				targets[ i ].run();
			}

			KoLmafia.updateDisplay( "Determining current rank..." );
			RequestThread.postRequest( new PvpRequest() );

			PvpManager.executeFlowerHuntRequest( targets, new PvpRequest(
				parameters, stance, KoLCharacter.canInteract() ? "dignity" : "flowers" ) );

			RequestThread.closeRequestSequence();
			return;
		}

		if ( command.equals( "flowers" ) )
		{
			PvpManager.executeFlowerHuntRequest();
			return;
		}

		if ( command.startsWith( "pvplog" ) )
		{
			PvpManager.summarizeFlowerHunterData();
			return;
		}

		if ( command.equals( "hermit" ) )
		{
			this.executeHermitRequest( parameters );
			return;
		}

		if ( command.equals( "raffle" ) )
		{
			this.executeRaffleRequest( parameters );
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
			int restores = KoLmafia.getRestoreCount();
			RequestLogger.printLine( restores + " mana restores remaining." );
			return;
		}

		if ( command.startsWith( "restore" ) || command.startsWith( "recover" ) || command.startsWith( "check" ) )
		{
			boolean wasRecoveryActive = KoLmafia.isRunningBetweenBattleChecks();
			KoLmafia.recoveryActive = true;

			if ( parameters.equalsIgnoreCase( "hp" ) || parameters.equalsIgnoreCase( "health" ) )
			{
				StaticEntity.getClient().recoverHP( KoLCharacter.getCurrentHP() + 1 );
			}
			else if ( parameters.equalsIgnoreCase( "mp" ) || parameters.equalsIgnoreCase( "mana" ) )
			{
				StaticEntity.getClient().recoverMP( KoLCharacter.getCurrentMP() + 1 );
			}

			KoLmafia.recoveryActive = wasRecoveryActive;

			return;
		}

		if ( command.startsWith( "trigger" ) )
		{
			if ( parameters.equals( "clear" ) )
			{
				MoodManager.removeTriggers( MoodManager.getTriggers().toArray() );
				MoodManager.saveSettings();
			}
			else if ( parameters.equals( "autofill" ) )
			{
				MoodManager.maximalSet();
				MoodManager.saveSettings();
			}

			String[] split = parameters.split( "\\s*,\\s*" );
			if ( split.length == 3 )
			{
				MoodManager.addTrigger( split[ 0 ], split[ 1 ], split[ 2 ] );
				MoodManager.saveSettings();
			}
			else if ( split.length == 2 )
			{
				MoodManager.addTrigger( split[ 0 ], split[ 1 ], MoodManager.getDefaultAction( split[ 0 ], split[ 1 ] ) );
				MoodManager.saveSettings();
			}
			else if ( split.length == 1 )
			{
				MoodManager.addTrigger( "lose_effect", split[ 0 ], MoodManager.getDefaultAction(
					"lose_effect", split[ 0 ] ) );
				MoodManager.saveSettings();
			}

			RequestLogger.printList( MoodManager.getTriggers() );
			return;
		}

		if ( command.startsWith( "mood" ) )
		{
			parameters = parameters.toLowerCase();

			if ( parameters.equals( "clear" ) )
			{
				MoodManager.removeTriggers( MoodManager.getTriggers().toArray() );
				MoodManager.saveSettings();
			}
			else if ( parameters.equals( "autofill" ) )
			{
				MoodManager.maximalSet();
				MoodManager.saveSettings();
				RequestLogger.printList( MoodManager.getTriggers() );
			}
			else if ( parameters.equals( "execute" ) )
			{
				if ( KoLmafia.isRunningBetweenBattleChecks() )
				{
					return;
				}

				SpecialOutfit.createImplicitCheckpoint();
				MoodManager.execute( 0 );
				SpecialOutfit.restoreImplicitCheckpoint();
				RequestLogger.printLine( "Mood swing complete." );
			}
			else if ( parameters.startsWith( "repeat" ) )
			{
				if ( KoLmafia.isRunningBetweenBattleChecks() )
				{
					return;
				}

				int multiplicity = 0;
				int spaceIndex = parameters.lastIndexOf( " " );
				if ( spaceIndex != -1 )
				{
					multiplicity = StringUtilities.parseInt( parameters.substring( spaceIndex + 1 ) );
				}

				SpecialOutfit.createImplicitCheckpoint();
				MoodManager.execute( multiplicity );
				SpecialOutfit.restoreImplicitCheckpoint();
				RequestLogger.printLine( "Mood swing complete." );
			}
			else
			{
				int multiplicity = 0;
				int spaceIndex = parameters.lastIndexOf( " " );
				if ( spaceIndex != -1 )
				{
					multiplicity = StringUtilities.parseInt( parameters.substring( spaceIndex + 1 ) );
					parameters = parameters.substring( 0, spaceIndex );
				}

				String previousMood = Preferences.getString( "currentMood" );
				MoodManager.setMood( parameters );

				this.executeCommand( "mood", "repeat " + multiplicity );

				if ( multiplicity > 0 )
				{
					MoodManager.setMood( previousMood );
				}
			}

			return;
		}

		if ( command.equals( "restaurant" ) )
		{
			this.makeChezSnooteeRequest( parameters );
			return;
		}

		if ( command.indexOf( "brewery" ) != -1 )
		{
			this.makeMicroBreweryRequest( parameters );
			return;
		}

		if ( command.indexOf( "kitchen" ) != -1 )
		{
			this.makeHellKitchenRequest( parameters );
			return;
		}

		// Campground commands, like resting at your house/tent.

		if ( command.equals( "rest" ) )
		{
			this.executeCampgroundRequest( command + " " + parameters );
			return;
		}

		if ( command.equals( "sofa" ) || command.equals( "sleep" ) )
		{
			RequestThread.postRequest( ( new ClanRumpusRequest( ClanRumpusRequest.SOFA ) ).setTurnCount( StringUtilities.parseInt( parameters ) ) );
			return;
		}

		// Because it makes sense to add this command as-is,
		// you now have the ability to request buffs.

		if ( command.equals( "send" ) || command.equals( "kmail" ) )
		{
			if ( KoLmafia.isRunningBetweenBattleChecks() )
			{
				RequestLogger.printLine( "Send request \"" + parameters + "\" ignored in between-battle execution." );
				return;
			}

			this.executeSendRequest( parameters, false );
			return;
		}

		if ( command.equals( "csend" ) )
		{
			this.executeSendRequest( parameters, true );
			return;
		}

		// Finally, handle command abbreviations - in
		// other words, commands that look like they're
		// their own commands, but are actually commands
		// which are derived from other commands.

		if ( command.startsWith( "skill" ) )
		{
			command = "skills";
		}

		if ( command.startsWith( "cast" ) || command.startsWith( "buff" ) || command.startsWith( "pass" ) || command.startsWith( "self" ) || command.startsWith( "combat" ) )
		{
			parameters = command;
			command = "skills";
		}

		if ( command.equals( "counters" ) )
		{
			if ( parameters.equalsIgnoreCase( "clear" ) )
			{
				TurnCounter.clearCounters();
				return;
			}

			this.showData( "counters" );
			return;
		}

		if ( command.startsWith( "inv" ) ||
		     command.equals( "closet" ) ||
		     command.equals( "storage" ) ||
		     command.equals( "session" ) ||
		     command.equals( "summary" ) ||
		     command.equals( "effects" ) ||
		     command.equals( "status" ) ||
		     command.equals( "skills" ) ||
		     command.equals( "locations" ) ||
		     command.equals( "encounters" ) ||
		     command.startsWith( "moon" ) )
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
			{
				return;
			}

			KoLAdventure adventure =
				new KoLAdventure(
					"Holiday", "adventure.php", parameters.substring( 0, spaceIndex ),
					parameters.substring( spaceIndex ).trim() );

			AdventureDatabase.addAdventure( adventure );
			return;
		}

		// If all else fails, then assume that the
		// person was trying to call a script.

		this.executeScript( this.currentLine );
	}

	public void showHTML( final String location, final String text )
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

		displayText = KoLmafiaCLI.HTMLTAG_PATTERN.matcher( displayText ).replaceAll( "" );
		displayText =
			displayText.replaceAll( "&nbsp;", " " ).replaceAll( "&trade;", " [tm]" ).replaceAll( "&ntilde;", "n" ).replaceAll(
				"&quot;", "" );

		// Allow only one new line at a time in the HTML
		// that is printed.

		displayText = displayText.replaceAll( "\n\n\n+", "\n\n" );
		displayText = KoLmafiaCLI.SCRIPT_PATTERN.matcher( displayText ).replaceAll( "" );
		displayText = KoLmafiaCLI.COMMENT_PATTERN.matcher( displayText ).replaceAll( "" );

		RequestLogger.printLine( displayText.trim() );
	}

	private void executeSendRequest( final String parameters, boolean isConvertible )
	{
		String[] splitParameters = parameters.replaceFirst( " [tT][oO] ", " => " ).split( " => " );

		if ( splitParameters.length != 2 )
		{
			KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "Invalid send request." );
			return;
		}

		String message = KoLConstants.DEFAULT_KMAIL;

		int separatorIndex = splitParameters[ 1 ].indexOf( "||" );
		if ( separatorIndex != -1 )
		{
			message = splitParameters[ 1 ].substring( separatorIndex + 2 ).trim();
			splitParameters[ 1 ] = splitParameters[ 1 ].substring( 0, separatorIndex );
		}

		splitParameters[ 0 ] = splitParameters[ 0 ].trim();
		splitParameters[ 1 ] = splitParameters[ 1 ].trim();

		System.out.println( splitParameters[ 0 ] );

		Object[] attachments = ItemFinder.getMatchingItemList( KoLConstants.inventory, splitParameters[ 0 ] );

		if ( attachments.length == 0 )
		{
			return;
		}

		int meatAmount = 0;
		List attachmentList = new ArrayList();

		for ( int i = 0; i < attachments.length; ++i )
		{
			if ( ((AdventureResult)attachments[i]).getName().equals( AdventureResult.MEAT ) )
			{
				meatAmount += ( (AdventureResult) attachments[i] ).getCount();
				attachments[i] = null;
			}
			else
			{
				AdventureResult.addResultToList( attachmentList, (AdventureResult) attachments[i] );
			}
		}

		if ( !isConvertible && meatAmount > 0 )
		{
			KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "Please use 'csend' if you need to transfer meat." );
			return;
		}

		// Validate their attachments.  If they happen to be
		// scripting a philanthropic buff request, then figure
		// out if there's a corresponding full-price buff.

		if ( meatAmount > 0 )
		{
			meatAmount = BuffBotDatabase.getOffering( splitParameters[ 1 ], meatAmount );
			AdventureResult.addResultToList( attachmentList, new AdventureResult( AdventureResult.MEAT, meatAmount ) );
		}

		this.executeSendRequest( splitParameters[ 1 ], message, attachmentList.toArray(), false, true );
	}

	public void executeSendRequest( final String recipient, final String message, final Object[] attachments,
		boolean usingStorage, final boolean isInternal )
	{
		if ( !usingStorage )
		{
			TransferItemRequest.setUpdateDisplayOnFailure( false );
			RequestThread.postRequest( new SendMailRequest( recipient, message, attachments, isInternal ) );
			TransferItemRequest.setUpdateDisplayOnFailure( true );

			if ( !TransferItemRequest.hadSendMessageFailure() )
			{
				KoLmafia.updateDisplay( "Message sent to " + recipient );
				return;
			}
		}

		List availablePackages = SendGiftRequest.getPackages();
		int desiredPackageIndex = Math.min( Math.min( availablePackages.size() - 1, attachments.length ), 5 );

		if ( HolidayDatabase.getHoliday( new Date() ).startsWith( "Valentine's" ) )
		{
			desiredPackageIndex = 0;
		}

		// Clear the error state for continuation on the
		// message sending attempt.

		if ( !KoLmafia.refusesContinue() )
		{
			KoLmafia.forceContinue();
		}

		RequestThread.postRequest( new SendGiftRequest(
			recipient, message, desiredPackageIndex, attachments, usingStorage ) );

		if ( KoLmafia.permitsContinue() )
		{
			KoLmafia.updateDisplay( "Gift sent to " + recipient );
		}
		else
		{
			KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "Failed to send message to " + recipient );
		}
	}

	public static final File findScriptFile( final String filename )
	{
		File scriptFile = new File( filename );
		if ( scriptFile.exists() )
		{
			return scriptFile.isDirectory() ? null : scriptFile;
		}

		scriptFile = new File( UtilityConstants.ROOT_LOCATION, filename );
		if ( scriptFile.exists() )
		{
			return scriptFile.isDirectory() ? null : scriptFile;
		}

		if ( KoLConstants.SCRIPT_LOCATION.exists() )
		{
			scriptFile = KoLmafiaCLI.findScriptFile( KoLConstants.SCRIPT_LOCATION, filename, false );
			if ( scriptFile != null )
			{
				return scriptFile.isDirectory() ? null : scriptFile;
			}
		}

		if ( KoLConstants.PLOTS_LOCATION.exists() )
		{
			scriptFile = new File( KoLConstants.PLOTS_LOCATION, filename );
			if ( scriptFile.exists() )
			{
				return scriptFile.isDirectory() ? null : scriptFile;
			}
		}

		if ( KoLConstants.RELAY_LOCATION.exists() )
		{
			scriptFile = KoLmafiaCLI.findScriptFile( KoLConstants.RELAY_LOCATION, filename, false );
			if ( scriptFile != null )
			{
				return scriptFile.isDirectory() ? null : scriptFile;
			}
		}

		return null;
	}

	public static final File findScriptFile( final File directory, final String filename )
	{
		return KoLmafiaCLI.findScriptFile( directory, filename, false );
	}

	private static final File findScriptFile( final File directory, final String filename, boolean isFallback )
	{
		File scriptFile = new File( directory, filename );

		if ( scriptFile.exists() )
		{
			return scriptFile;
		}

		if ( !isFallback )
		{
			scriptFile = KoLmafiaCLI.findScriptFile( directory, filename + ".cli", true );
			if ( scriptFile != null )
			{
				return scriptFile;
			}

			scriptFile = KoLmafiaCLI.findScriptFile( directory, filename + ".txt", true );
			if ( scriptFile != null )
			{
				return scriptFile;
			}

			scriptFile = KoLmafiaCLI.findScriptFile( directory, filename + ".ash", true );
			if ( scriptFile != null )
			{
				return scriptFile;
			}
		}

		File[] contents = DataUtilities.listFiles( directory );
		for ( int i = 0; i < contents.length; ++i )
		{
			if ( contents[ i ].isDirectory() )
			{
				scriptFile = KoLmafiaCLI.findScriptFile( contents[ i ], filename, false );
				if ( scriptFile != null )
				{
					return scriptFile;
				}
			}
		}

		return null;
	}

	public void executeScript( final String script )
	{
		this.executeScriptCommand( "call", script );
	}

	/**
	 * A special module used to handle the calling of a script.
	 */

	private void executeScriptCommand( final String command, String parameters )
	{
		try
		{
			int runCount = 1;
			String[] arguments = null;

			parameters = parameters.trim();
			File scriptFile = KoLmafiaCLI.findScriptFile( parameters );

			// If still no script was found, perhaps it's the secret invocation
			// of the "#x script" that allows a script to be run multiple times.

			if ( scriptFile == null )
			{
				String runCountString = parameters.split( " " )[ 0 ];
				boolean hasMultipleRuns = runCountString.endsWith( "x" );

				for ( int i = 0; i < runCountString.length() - 1 && hasMultipleRuns; ++i )
				{
					hasMultipleRuns = Character.isDigit( runCountString.charAt( i ) );
				}

				if ( hasMultipleRuns )
				{
					runCount = StringUtilities.parseInt( runCountString );
					parameters = parameters.substring( parameters.indexOf( " " ) ).trim();
					scriptFile = KoLmafiaCLI.findScriptFile( parameters );
				}
			}

			// Maybe the more ambiguous invocation of an ASH script which does
			// not use parentheses?

			if ( scriptFile == null )
			{
				int spaceIndex = parameters.indexOf( " " );
				if ( spaceIndex != -1 )
				{
					arguments = new String[] { parameters.substring( spaceIndex + 1 ).trim() };
					parameters = parameters.substring( 0, spaceIndex );
					scriptFile = KoLmafiaCLI.findScriptFile( parameters );
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
				KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, scriptFile.getAbsolutePath() + " is a directory." );
				return;
			}

			// Allow the ".ash" to appear anywhere in the filename
			// in a case-insensitive manner.

			if ( KoLmafiaCLI.ASHNAME_PATTERN.matcher( scriptFile.getPath() ).find() )
			{
				// If there's an alternate namespace being
				// used, then be sure to switch.

				if ( command.equals( "validate" ) || command.equals( "verify" ) || command.equals( "check" ) )
				{
					Interpreter interpreter = KoLmafiaASH.getInterpreter( scriptFile );
					if ( interpreter != null )
					{
						RequestLogger.printLine();
						KoLmafiaASH.showUserFunctions( interpreter, "" );

						RequestLogger.printLine();
						RequestLogger.printLine( "Script verification complete." );
					}

					return;
				}

				// If there's an alternate namespace being
				// used, then be sure to switch.

				Interpreter interpreter = KoLmafiaASH.getInterpreter( scriptFile );
				if ( interpreter != null )
				{
					for ( int i = 0; i < runCount && KoLmafia.permitsContinue(); ++i )
					{
						interpreter.execute( "main", arguments );
					}
				}
			}
			else
			{
				if ( arguments != null )
				{
					KoLmafia.updateDisplay(
						KoLConstants.ERROR_STATE, "You can only specify arguments for an ASH script" );
					return;
				}

				for ( int i = 0; i < runCount && KoLmafia.permitsContinue(); ++i )
				{
					( new KoLmafiaCLI( DataUtilities.getInputStream( scriptFile ) ) ).listenForCommands();
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
	 * Utility method which tests if the given condition is true. Note that this only examines level, health, mana,
	 * items, meat and status effects.
	 */

	public static final boolean testConditional( final String parameters )
	{
		if ( !KoLmafia.permitsContinue() )
		{
			return false;
		}

		// Allow checking for moon signs for stat days
		// only.  Allow test for today and tomorrow.

		Matcher dayMatcher = KoLmafiaCLI.STATDAY_PATTERN.matcher( parameters );
		if ( dayMatcher.find() )
		{
			String statDayToday = HolidayDatabase.getMoonEffect().toLowerCase();
			String statDayTest = dayMatcher.group( 2 ).substring( 0, 3 ).toLowerCase();

			return statDayToday.indexOf( statDayTest ) != -1 && statDayToday.indexOf( "bonus" ) != -1 && statDayToday.indexOf( "not " + dayMatcher.group( 1 ) ) == -1;
		}

		// Check if the person is looking for whether or
		// not they are a certain class.

		if ( parameters.startsWith( "class is not " ) )
		{
			String className = parameters.substring( 13 ).trim().toLowerCase();
			String actualClassName = KoLCharacter.getClassType().toLowerCase();
			return actualClassName.indexOf( className ) == -1;
		}

		if ( parameters.startsWith( "class is " ) )
		{
			String className = parameters.substring( 9 ).trim().toLowerCase();
			String actualClassName = KoLCharacter.getClassType().toLowerCase();
			return actualClassName.indexOf( className ) != -1;
		}

		// Check if the person has a specific skill
		// in their available skills list.

		if ( parameters.startsWith( "skill list lacks " ) )
		{
			return !KoLCharacter.hasSkill( KoLmafiaCLI.getSkillName( parameters.substring( 17 ).trim().toLowerCase() ) );
		}

		if ( parameters.startsWith( "skill list contains " ) )
		{
			return KoLCharacter.hasSkill( KoLmafiaCLI.getSkillName( parameters.substring( 20 ).trim().toLowerCase() ) );
		}

		// Generic tests for numerical comparisons
		// involving left and right values.

		String operator =
			parameters.indexOf( "==" ) != -1 ? "==" : parameters.indexOf( "!=" ) != -1 ? "!=" : parameters.indexOf( ">=" ) != -1 ? ">=" : parameters.indexOf( "<=" ) != -1 ? "<=" : parameters.indexOf( "<>" ) != -1 ? "!=" : parameters.indexOf( "=" ) != -1 ? "==" : parameters.indexOf( ">" ) != -1 ? ">" : parameters.indexOf( "<" ) != -1 ? "<" : null;

		if ( operator == null )
		{
			return false;
		}

		String[] tokens = parameters.split( "[\\!<>=]" );

		String left = tokens[ 0 ].trim();
		String right = tokens[ tokens.length - 1 ].trim();

		int leftValue;
		int rightValue;

		try
		{
			leftValue = KoLmafiaCLI.lvalue( left );
			rightValue = KoLmafiaCLI.rvalue( left, right );
		}
		catch ( Exception e )
		{
			// This should not happen.  Therefore, print
			// a stack trace for debug purposes.

			KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, parameters + " is not a valid construct." );
			return false;
		}

		return operator.equals( "==" ) ? leftValue == rightValue : operator.equals( "!=" ) ? leftValue != rightValue : operator.equals( ">=" ) ? leftValue >= rightValue : operator.equals( ">" ) ? leftValue > rightValue : operator.equals( "<=" ) ? leftValue <= rightValue : operator.equals( "<" ) ? leftValue < rightValue : false;
	}

	private static final int lvalue( final String left )
	{
		if ( left.equals( "level" ) )
		{
			return KoLCharacter.getLevel();
		}

		if ( left.equals( "health" ) )
		{
			return KoLCharacter.getCurrentHP();
		}

		if ( left.equals( "mana" ) )
		{
			return KoLCharacter.getCurrentMP();
		}

		if ( left.equals( "meat" ) )
		{
			return KoLCharacter.getAvailableMeat();
		}

		if ( left.equals( "adventures" ) )
		{
			return KoLCharacter.getAdventuresLeft();
		}

		if ( left.equals( "inebriety" ) || left.equals( "drunkenness" ) || left.equals( "drunkness" ) )
		{
			return KoLCharacter.getInebriety();
		}

		if ( left.equals( "muscle" ) )
		{
			return KoLCharacter.getBaseMuscle();
		}

		if ( left.equals( "mysticality" ) )
		{
			return KoLCharacter.getBaseMysticality();
		}

		if ( left.equals( "moxie" ) )
		{
			return KoLCharacter.getBaseMoxie();
		}

		if ( left.equals( "worthless item" ) )
		{
			return HermitRequest.getWorthlessItemCount();
		}

		AdventureResult item = KoLmafiaCLI.itemParameter( left );
		AdventureResult effect = KoLmafiaCLI.effectParameter( left );

		// If there is no question you're looking for one or
		// the other, then return the appropriate match.

		if ( item != null && effect == null )
		{
			return item.getCount( KoLConstants.inventory );
		}

		if ( item == null && effect != null )
		{
			return effect.getCount( KoLConstants.activeEffects );
		}

		// This breaks away from fuzzy matching so that a
		// substring match is preferred over a fuzzy match.
		// Items first for one reason: Knob Goblin perfume.

		if ( item != null && item.getName().toLowerCase().indexOf( left.toLowerCase() ) != -1 )
		{
			return item.getCount( KoLConstants.inventory );
		}

		if ( effect != null && effect.getName().toLowerCase().indexOf( left.toLowerCase() ) != -1 )
		{
			return effect.getCount( KoLConstants.activeEffects );
		}

		// Now, allow fuzzy match results to return a value.
		// Again, following the previous precident, items are
		// preferred over effects.

		if ( item != null )
		{
			return item.getCount( KoLConstants.inventory );
		}

		if ( effect != null )
		{
			return effect.getCount( KoLConstants.activeEffects );
		}

		// No match.  The value is zero by default.

		return 0;
	}

	private static final int rvalue( final String left, String right )
	{
		if ( right.endsWith( "%" ) )
		{
			right = right.substring( 0, right.length() - 1 );
			int value = StringUtilities.parseInt( right );

			if ( left.equals( "health" ) )
			{
				return (int) ( (float) value * (float) KoLCharacter.getMaximumHP() / 100.0f );
			}

			if ( left.equals( "mana" ) )
			{
				return (int) ( (float) value * (float) KoLCharacter.getMaximumMP() / 100.0f );
			}

			return value;
		}

		for ( int i = 0; i < right.length(); ++i )
		{
			if ( !Character.isDigit( right.charAt( i ) ) )
			{
				// Items first for one reason: Knob Goblin perfume
				// Determine which item is being matched.

				AdventureResult item = KoLmafiaCLI.itemParameter( right );

				if ( item != null )
				{
					return item.getCount( KoLConstants.inventory );
				}

				AdventureResult effect = KoLmafiaCLI.effectParameter( right );

				if ( effect != null )
				{
					return effect.getCount( KoLConstants.activeEffects );
				}

				// If it is neither an item nor an effect, report
				// the exception.

				if ( i == 0 && right.charAt( 0 ) == '-' )
				{
					continue;
				}

				KoLmafia.updateDisplay(
					KoLConstants.ERROR_STATE, "Invalid operand [" + right + "] on right side of operator" );
			}
		}

		// If it gets this far, then it must be numeric,
		// so parse the number and return it.

		return StringUtilities.parseInt( right );
	}

	private static final AdventureResult effectParameter( final String parameter )
	{
		List potentialEffects = EffectDatabase.getMatchingNames( parameter );
		if ( potentialEffects.isEmpty() )
		{
			return null;
		}

		return new AdventureResult( (String) potentialEffects.get( 0 ), 0, true );
	}

	private static final AdventureResult itemParameter( final String parameter )
	{
		List potentialItems = ItemDatabase.getMatchingNames( parameter );
		if ( potentialItems.isEmpty() )
		{
			return null;
		}

		return new AdventureResult( (String) potentialItems.get( 0 ), 0, false );
	}

	/**
	 * A special module used to handle conditions requests. This determines what the user is planning to do with the
	 * condition, and then parses the condition to be added, and then adds it to the conditions list.
	 */

	public boolean executeConditionsCommand( final String parameters )
	{
		String option = parameters.split( " " )[ 0 ];

		if ( option.equals( "clear" ) )
		{
			KoLConstants.conditions.clear();
			RequestLogger.printLine( "Conditions list cleared." );
			return true;
		}
		else if ( option.equals( "check" ) )
		{
			KoLmafia.checkRequirements( KoLConstants.conditions );
			RequestLogger.printLine( "Conditions list validated against available items." );
			return true;
		}
		else if ( option.equals( "add" ) || option.equals( "set" ) )
		{
			AdventureResult condition;
			String[] conditionsList = parameters.substring( option.length() ).toLowerCase().trim().split( "\\s*,\\s*" );

			for ( int i = 0; i < conditionsList.length; ++i )
			{
				if ( conditionsList[ i ].equalsIgnoreCase( "castle map items" ) )
				{
					this.addItemCondition( "set", new AdventureResult( 616, 1 ) ); // furry fur
					this.addItemCondition( "set", new AdventureResult( 619, 1 ) ); // giant needle
					this.addItemCondition( "set", new AdventureResult( 622, 1 ) ); // awful poetry journal
				}
				else
				{
					condition = this.extractCondition( conditionsList[ i ] );
					if ( condition != null )
					{
						this.addItemCondition( option, condition );
					}
				}
			}

			return true;
		}

		RequestLogger.printList( KoLConstants.conditions );
		return false;
	}

	private void addItemCondition( final String option, final AdventureResult condition )
	{
		if ( condition.isItem() && option.equals( "set" ) )
		{
			int currentAmount =
				condition.getItemId() == HermitRequest.WORTHLESS_ITEM.getItemId() ? HermitRequest.getWorthlessItemCount() : condition.getCount( KoLConstants.inventory ) + condition.getCount( KoLConstants.closet );

			for ( int j = 0; j < EquipmentManager.FAMILIAR; ++j )
			{
				if ( EquipmentManager.getEquipment( j ).equals( condition ) )
				{
					++currentAmount;
				}
			}

			if ( condition.getCount( KoLConstants.conditions ) >= condition.getCount() )
			{
				RequestLogger.printLine( "Condition already exists: " + condition );
			}
			else if ( currentAmount >= condition.getCount() )
			{
				RequestLogger.printLine( "Condition already met: " + condition );
			}
			else
			{
				AdventureResult.addResultToList(
					KoLConstants.conditions, condition.getInstance( condition.getCount() - currentAmount ) );
				RequestLogger.printLine( "Condition set: " + condition );
			}
		}
		else if ( condition.getCount() > 0 )
		{
			AdventureResult.addResultToList( KoLConstants.conditions, condition );
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
		{
			return null;
		}

		conditionString = conditionString.toLowerCase();

		AdventureResult condition = null;
		Matcher meatMatcher = KoLmafiaCLI.MEAT_PATTERN.matcher( conditionString );
		boolean isMeatCondition = meatMatcher.find() ? meatMatcher.group().length() == conditionString.length() : false;

		if ( isMeatCondition )
		{
			String[] splitCondition = conditionString.split( "\\s+" );
			int amount = StringUtilities.parseInt( splitCondition[ 0 ] );
			condition = new AdventureResult( AdventureResult.MEAT, amount );
		}
		else if ( conditionString.endsWith( "choiceadv" ) || conditionString.endsWith( "choices" ) || conditionString.endsWith( "choice" ) )
		{
			// If it's a choice adventure condition, parse out the
			// number of choice adventures the user wishes to do.

			String[] splitCondition = conditionString.split( "\\s+" );
			condition =
				new AdventureResult(
					AdventureResult.CHOICE,
					splitCondition.length > 1 ? StringUtilities.parseInt( splitCondition[ 0 ] ) : 1 );
		}
		else if ( conditionString.startsWith( "level" ) )
		{
			// If the condition is a level, then determine how many
			// substat points are required to the next level and
			// add the substat points as a condition.

			String[] splitCondition = conditionString.split( "\\s+" );
			int level = StringUtilities.parseInt( splitCondition[ 1 ] );

			int primeIndex = KoLCharacter.getPrimeIndex();

			AdventureResult.CONDITION_SUBSTATS[ primeIndex ] =
				KoLCharacter.calculateSubpoints( ( level - 1 ) * ( level - 1 ) + 4, 0 ) - KoLCharacter.getTotalPrime();

			condition = AdventureResult.CONDITION_SUBSTATS_RESULT;
		}
		else if ( conditionString.endsWith( "mus" ) || conditionString.endsWith( "muscle" ) || conditionString.endsWith( "moxie" ) || conditionString.endsWith( "mys" ) || conditionString.endsWith( "myst" ) || conditionString.endsWith( "mox" ) || conditionString.endsWith( "mysticality" ) )
		{
			String[] splitCondition = conditionString.split( "\\s+" );

			int points = StringUtilities.parseInt( splitCondition[ 0 ] );
			int statIndex = conditionString.indexOf( "mus" ) != -1 ? 0 : conditionString.indexOf( "mys" ) != -1 ? 1 : 2;

			AdventureResult.CONDITION_SUBSTATS[ statIndex ] = KoLCharacter.calculateSubpoints( points, 0 );
			AdventureResult.CONDITION_SUBSTATS[ statIndex ] =
				Math.max(
					0,
					AdventureResult.CONDITION_SUBSTATS[ statIndex ] - ( conditionString.indexOf( "mus" ) != -1 ? KoLCharacter.getTotalMuscle() : conditionString.indexOf( "mys" ) != -1 ? KoLCharacter.getTotalMysticality() : KoLCharacter.getTotalMoxie() ) );

			condition = AdventureResult.CONDITION_SUBSTATS_RESULT;
		}
		else if ( conditionString.endsWith( "health" ) || conditionString.endsWith( "mana" ) )
		{
			String numberString = conditionString.split( "\\s+" )[ 0 ];
			int points =
				StringUtilities.parseInt( numberString.endsWith( "%" ) ? numberString.substring(
					0, numberString.length() - 1 ) : numberString );

			if ( numberString.endsWith( "%" ) )
			{
				if ( conditionString.endsWith( "health" ) )
				{
					points = (int) ( (float) points * (float) KoLCharacter.getMaximumHP() / 100.0f );
				}
				else if ( conditionString.endsWith( "mana" ) )
				{
					points = (int) ( (float) points * (float) KoLCharacter.getMaximumMP() / 100.0f );
				}
			}

			points -= conditionString.endsWith( "health" ) ? KoLCharacter.getCurrentHP() : KoLCharacter.getCurrentMP();

			condition =
				new AdventureResult(
					conditionString.endsWith( "health" ) ? AdventureResult.HP : AdventureResult.MP, points );

			int previousIndex = KoLConstants.conditions.indexOf( condition );
			if ( previousIndex != -1 )
			{
				AdventureResult previousCondition = (AdventureResult) KoLConstants.conditions.get( previousIndex );
				condition = condition.getInstance( condition.getCount() - previousCondition.getCount() );
			}
		}
		else if ( conditionString.endsWith( "outfit" ) )
		{
			// Usage: conditions add <location> outfit
			String outfitLocation;

			if ( conditionString.equals( "outfit" ) )
			{
				outfitLocation = Preferences.getString( "lastAdventure" );
			}
			else
			{
				outfitLocation = conditionString.substring( 0, conditionString.length() - 7 );
			}

			// Try to support outfit names by mapping some outfits to their locations
			if ( outfitLocation.equals( "guard" ) || outfitLocation.equals( "elite" ) || outfitLocation.equals( "elite guard" ) )
			{
				outfitLocation = "treasury";
			}

			if ( outfitLocation.equals( "rift" ) )
			{
				outfitLocation = "battlefield";
			}

			if ( outfitLocation.equals( "cloaca-cola" ) || outfitLocation.equals( "cloaca cola" ) )
			{
				outfitLocation = "cloaca";
			}

			if ( outfitLocation.equals( "dyspepsi-cola" ) || outfitLocation.equals( "dyspepsi cola" ) )
			{
				outfitLocation = "dyspepsi";
			}

			KoLAdventure lastAdventure = AdventureDatabase.getAdventure( outfitLocation );

			if ( !EquipmentManager.addOutfitConditions( lastAdventure ) )
			{
				KoLmafia.updateDisplay(
					KoLConstants.ERROR_STATE, "No outfit corresponds to " + lastAdventure.getAdventureName() + "." );
			}
		}
		else
		{
			condition = ItemFinder.getFirstMatchingItem( conditionString );
		}

		return condition;
	}

	/**
	 * A special module used to handle campground requests, such as toast retrieval, resting, and the like.
	 */

	private void executeCampgroundRequest( final String parameters )
	{
		String[] parameterList = parameters.split( " " );
		StaticEntity.getClient().makeRequest(
			new CampgroundRequest( parameterList[ 0 ] ),
			parameterList.length == 1 ? 1 : StringUtilities.parseInt( parameterList[ 1 ] ) );
	}

	/**
	 * A special module used to handle casting skills on yourself or others. Castable skills must be listed in
	 * usableSkills
	 */

	private void executeCastBuffRequest( final String parameters )
	{
		String[] buffs = parameters.split( "\\s*,\\s*" );

		for ( int i = 0; i < buffs.length; ++i )
		{
			String[] splitParameters = buffs[ i ].replaceFirst( " [oO][nN] ", " => " ).split( " => " );

			if ( splitParameters.length == 1 )
			{
				splitParameters = new String[ 2 ];
				splitParameters[ 0 ] = buffs[ i ];
				splitParameters[ 1 ] = null;
			}

			String[] buffParameters = this.splitCountAndName( splitParameters[ 0 ] );
			String buffCountString = buffParameters[ 0 ];
			String skillNameString = buffParameters[ 1 ];

			String skillName = KoLmafiaCLI.getUsableSkillName( skillNameString );
			if ( skillName == null )
			{
				KoLmafia.updateDisplay( "You don't have a skill matching \"" + parameters + "\"" );
				return;
			}

			int buffCount = 1;

			if ( buffCountString != null && buffCountString.equals( "*" ) )
			{
				buffCount = 0;
			}
			else if ( buffCountString != null )
			{
				buffCount = StringUtilities.parseInt( buffCountString );
			}

			if ( KoLmafiaCLI.isExecutingCheckOnlyCommand )
			{
				RequestLogger.printLine( skillName + " (x" + buffCount + ")" );
				return;
			}

			RequestThread.postRequest( UseSkillRequest.getInstance( skillName, splitParameters[ 1 ], buffCount ) );
		}
	}

	private String[] splitCountAndName( final String parameters )
	{
		String nameString;
		String countString;

		if ( parameters.startsWith( "\"" ) )
		{
			nameString = parameters.substring( 1, parameters.length() - 1 );
			countString = null;
		}
		else if ( parameters.startsWith( "*" ) || parameters.indexOf( " " ) != -1 && Character.isDigit( parameters.charAt( 0 ) ) )
		{
			countString = parameters.split( " " )[ 0 ];
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

		return new String[] { countString, nameString };
	}

	/**
	 * Utility method used to retrieve the full name of a skill, given a substring representing it.
	 */

	public static final String getSkillName( final String substring, final List list )
	{
		UseSkillRequest[] skills = new UseSkillRequest[ list.size() ];
		list.toArray( skills );

		String name = substring.toLowerCase();

		int skillIndex = -1;
		int substringIndex = Integer.MAX_VALUE;

		int currentIndex;

		for ( int i = 0; i < skills.length; ++i )
		{
			String skill = skills[ i ].getSkillName();
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
	 * Utility method used to retrieve the full name of a skill, given a substring representing it.
	 */

	public static final String getSkillName( final String substring )
	{
		return KoLmafiaCLI.getSkillName( substring, KoLConstants.availableSkills );
	}

	/**
	 * Utility method used to retrieve the full name of a combat skill, given a substring representing it.
	 */

	public static final String getUsableSkillName( final String substring )
	{
		return KoLmafiaCLI.getSkillName( substring, KoLConstants.usableSkills );
	}

	/**
	 * Utility method used to retrieve the full name of a combat skill, given a substring representing it.
	 */

	public static final String getCombatSkillName( final String substring )
	{
		return KoLmafiaCLI.getSkillName( substring, SkillDatabase.getSkillsByType( SkillDatabase.COMBAT ) );
	}

	/**
	 * A special module used specifically for handling donations, including donations to the statues and donations to
	 * the clan.
	 */

	private void executeDonateCommand( final String parameters )
	{
		int heroId;
		int amount = -1;

		String[] parameterList = parameters.split( " " );

		if ( parameterList[ 0 ].startsWith( "boris" ) || parameterList[ 0 ].startsWith( "mus" ) )
		{
			heroId = ShrineRequest.BORIS;
		}
		else if ( parameterList[ 0 ].startsWith( "jarl" ) || parameterList[ 0 ].startsWith( "mys" ) )
		{
			heroId = ShrineRequest.JARLSBERG;
		}
		else if ( parameterList[ 0 ].startsWith( "pete" ) || parameterList[ 0 ].startsWith( "mox" ) )
		{
			heroId = ShrineRequest.PETE;
		}
		else
		{
			KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, parameters + " is not a statue." );
			return;
		}

		amount = StringUtilities.parseInt( parameterList[ 1 ] );
		KoLmafia.updateDisplay( "Donating " + amount + " to the shrine..." );
		RequestThread.postRequest( new ShrineRequest( heroId, amount ) );
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
			this.showData( "equipment " + parameters.substring( 4 ).trim() );
			return;
		}

		if ( parameters.indexOf( "(no change)" ) != -1 )
		{
			return;
		}

		// Look for name of slot
		String command = parameters.split( " " )[ 0 ];
		int slot = EquipmentRequest.slotNumber( command );

		if ( slot != -1 )
		{
			parameters = parameters.substring( command.length() ).trim();
		}

		AdventureResult match = ItemFinder.getFirstMatchingItem( parameters );
		if ( match == null )
		{
			return;
		}

		// If he didn't specify slot name, decide where this item goes.
		if ( slot == -1 )
		{
			// If it's already equipped anywhere, give up
			for ( int i = 0; i <= EquipmentManager.FAMILIAR; ++i )
			{
				AdventureResult item = EquipmentManager.getEquipment( i );
				if ( item != null && item.getName().toLowerCase().indexOf( parameters ) != -1 )
				{
					return;
				}
			}

			// It's not equipped. Choose a slot for it
			slot = EquipmentRequest.chooseEquipmentSlot( ItemDatabase.getConsumptionType( match.getItemId() ) );

			// If it can't be equipped, give up
			if ( slot == -1 )
			{
				KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "You can't equip a	" + match.getName() );
				return;
			}
		}
		else // See if desired item is already in selected slot
		if ( EquipmentManager.getEquipment( slot ).equals( match ) )
		{
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
		String command = parameters.split( " " )[ 0 ];
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
			if ( EquipmentManager.getFakeHands() == 0 )
			{
				KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "You're not wearing any fake hands" );
			}
			else
			{
				RequestThread.postRequest( new EquipmentRequest( EquipmentRequest.UNEQUIP, EquipmentManager.FAKEHAND ) );
			}

			return;
		}

		// The following loop removes all items with the
		// specified name.

		for ( int i = 0; i <= EquipmentManager.FAMILIAR; ++i )
		{
			AdventureResult item = EquipmentManager.getEquipment( i );
			if ( item != null && item.getName().toLowerCase().indexOf( parameters ) != -1 )
			{
				RequestThread.postRequest( new EquipmentRequest( EquipmentRequest.UNEQUIP, i ) );
			}
		}
	}

	private void showData( final String parameters )
	{
		this.showData( parameters, false );
	}

	/**
	 * A special module used specifically for properly printing out data relevant to the current session.
	 */

	private void showData( String parameters, boolean sessionPrint )
	{
		if ( parameters.length() == 0 )
		{
			KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "Print what?" );
			return;
		}

		parameters = parameters.trim();
		int spaceIndex = parameters.indexOf( " " );

		String list = spaceIndex == -1 ? parameters : parameters.substring( 0, spaceIndex ).trim();
		String filter =
			spaceIndex == -1 ? "" : StringUtilities.getCanonicalName( parameters.substring( spaceIndex ).trim() );

		PrintStream desiredOutputStream = sessionPrint ? RequestLogger.getSessionStream() : RequestLogger.INSTANCE;

		if ( !filter.equals( "" ) && ( parameters.startsWith( "summary" ) || parameters.startsWith( "session" ) || parameters.equals( "status" ) || parameters.startsWith( "equip" ) || parameters.startsWith( "encounters" ) || parameters.startsWith( "locations" ) ) )
		{
			desiredOutputStream = LogStream.openStream( new File( UtilityConstants.ROOT_LOCATION, filter ), false );
		}

		this.showData( list, filter, desiredOutputStream );

		if ( sessionPrint && RequestLogger.isDebugging() )
		{
			this.showData( list, filter, RequestLogger.getDebugStream() );
		}

		if ( !sessionPrint )
		{
			desiredOutputStream.close();
		}
	}

	/**
	 * A special module used specifically for properly printing out data relevant to the current session. This method is
	 * more specialized than its counterpart and is used when the data to be printed is known, as well as the stream to
	 * print to. Usually called by its counterpart to handle specific instances.
	 */

	private void showData( final String desiredData, String filter, final PrintStream desiredStream )
	{
		desiredStream.println();

		if ( desiredData.startsWith( "moon" ) )
		{
			Date today = new Date();

			try
			{
				today = KoLConstants.DAILY_FORMAT.parse( KoLConstants.DAILY_FORMAT.format( today ) );
			}
			catch ( Exception e )
			{
				// This should not happen.  Therefore, print
				// a stack trace for debug purposes.

				StaticEntity.printStackTrace( e );
			}

			desiredStream.println( CalendarFrame.LONG_FORMAT.format( today ) + " - " + HolidayDatabase.getCalendarDayAsString( today ) );
			desiredStream.println();

			desiredStream.println( "Ronald: " + HolidayDatabase.getRonaldPhaseAsString() );
			desiredStream.println( "Grimace: " + HolidayDatabase.getGrimacePhaseAsString() );
			desiredStream.println();

			String[] holidayPredictions = HolidayDatabase.getHolidayPredictions( today );
			for ( int i = 0; i < holidayPredictions.length; ++i )
			{
				desiredStream.println( holidayPredictions[ i ] );
			}

			desiredStream.println();
			desiredStream.println( HolidayDatabase.getHoliday( today ) );
			desiredStream.println( HolidayDatabase.getMoonEffect() );
			desiredStream.println();
		}
		else if ( desiredData.equals( "session" ) )
		{
			desiredStream.println( "Player: " + KoLCharacter.getUserName() );
			desiredStream.println( "Session Id: " + GenericRequest.serverCookie );
			desiredStream.println( "Password Hash: " + GenericRequest.passwordHash );
			desiredStream.println( "Current Server: " + GenericRequest.KOL_HOST );
			desiredStream.println();
		}
		else if ( desiredData.equals( "status" ) )
		{
			desiredStream.println( "Name: " + KoLCharacter.getUserName() );
			desiredStream.println( "Class: " + KoLCharacter.getClassType() );
			desiredStream.println();

			desiredStream.println( "Lv: " + KoLCharacter.getLevel() );
			desiredStream.println( "HP: " + KoLCharacter.getCurrentHP() + " / " + KoLConstants.COMMA_FORMAT.format( KoLCharacter.getMaximumHP() ) );
			desiredStream.println( "MP: " + KoLCharacter.getCurrentMP() + " / " + KoLConstants.COMMA_FORMAT.format( KoLCharacter.getMaximumMP() ) );

			desiredStream.println();

			desiredStream.println( "Mus: " + KoLmafiaCLI.getStatString(
				KoLCharacter.getBaseMuscle(), KoLCharacter.getAdjustedMuscle(), KoLCharacter.getMuscleTNP() ) );
			desiredStream.println( "Mys: " + KoLmafiaCLI.getStatString(
				KoLCharacter.getBaseMysticality(), KoLCharacter.getAdjustedMysticality(),
				KoLCharacter.getMysticalityTNP() ) );
			desiredStream.println( "Mox: " + KoLmafiaCLI.getStatString(
				KoLCharacter.getBaseMoxie(), KoLCharacter.getAdjustedMoxie(), KoLCharacter.getMoxieTNP() ) );

			desiredStream.println();

			desiredStream.println( "Advs: " + KoLCharacter.getAdventuresLeft() );
			desiredStream.println( "Meat: " + KoLConstants.COMMA_FORMAT.format( KoLCharacter.getAvailableMeat() ) );
			desiredStream.println( "Drunk: " + KoLCharacter.getInebriety() );

			desiredStream.println();
		}
		else if ( desiredData.equals( "modifiers" ) )
		{
			desiredStream.println( "ML: " + KoLConstants.MODIFIER_FORMAT.format( KoLCharacter.getMonsterLevelAdjustment() ) );
			desiredStream.println( "Enc: " + KoLConstants.ROUNDED_MODIFIER_FORMAT.format( KoLCharacter.getCombatRateAdjustment() ) + "%" );
			desiredStream.println( "Init: " + KoLConstants.ROUNDED_MODIFIER_FORMAT.format( KoLCharacter.getInitiativeAdjustment() ) + "%" );

			desiredStream.println();

			desiredStream.println( "Exp: " + KoLConstants.ROUNDED_MODIFIER_FORMAT.format( KoLCharacter.getExperienceAdjustment() ) );
			desiredStream.println( "Meat: " + KoLConstants.ROUNDED_MODIFIER_FORMAT.format( KoLCharacter.getMeatDropPercentAdjustment() ) + "%" );
			desiredStream.println( "Item: " + KoLConstants.ROUNDED_MODIFIER_FORMAT.format( KoLCharacter.getItemDropPercentAdjustment() ) + "%" );

			desiredStream.println();
		}
		else if ( desiredData.startsWith( "equip" ) )
		{
			desiredStream.println( "Hat: " + EquipmentManager.getEquipment( EquipmentManager.HAT ) );
			desiredStream.println( "Weapon: " + EquipmentManager.getEquipment( EquipmentManager.WEAPON ) );

			if ( EquipmentManager.getFakeHands() > 0 )
			{
				desiredStream.println( "Fake Hands: " + EquipmentManager.getFakeHands() );
			}

			desiredStream.println( "Off-hand: " + EquipmentManager.getEquipment( EquipmentManager.OFFHAND ) );
			desiredStream.println( "Shirt: " + EquipmentManager.getEquipment( EquipmentManager.SHIRT ) );
			desiredStream.println( "Pants: " + EquipmentManager.getEquipment( EquipmentManager.PANTS ) );

			desiredStream.println();

			desiredStream.println( "Acc. 1: " + EquipmentManager.getEquipment( EquipmentManager.ACCESSORY1 ) );
			desiredStream.println( "Acc. 2: " + EquipmentManager.getEquipment( EquipmentManager.ACCESSORY2 ) );
			desiredStream.println( "Acc. 3: " + EquipmentManager.getEquipment( EquipmentManager.ACCESSORY3 ) );

			desiredStream.println();

			desiredStream.println( "Pet: " + KoLCharacter.getFamiliar() );
			desiredStream.println( "Item: " + EquipmentManager.getFamiliarItem() );
		}
		else if ( desiredData.equals( "encounters" ) )
		{
			desiredStream.println( "Encounter Listing: " );

			desiredStream.println();
			RequestLogger.printList( KoLConstants.encounterList, desiredStream );
		}
		else if ( desiredData.equals( "locations" ) )
		{
			desiredStream.println( "Visited Locations: " );
			desiredStream.println();

			RequestLogger.printList( KoLConstants.adventureList, desiredStream );
		}
		else if ( desiredData.equals( "counters" ) )
		{
			KoLCharacter.ensureUpdatedSemirareCounter();
			int turns = Preferences.getInteger( "semirareCounter" );
			if ( turns == 0 )
			{
				desiredStream.println( "No semirare found yet this run." );
			}
			else
			{
				int current = KoLCharacter.getCurrentRun();
				String location = Preferences.getString( "semirareLocation" );
				String loc = location.equals( "" ) ? "" : ( " in " + location );
				desiredStream.println( "Last semirare found " + ( current - turns ) + " turns ago (on turn " + turns + ")" + loc );
			}

			String counters = TurnCounter.getUnexpiredCounters();
			desiredStream.println();
			if ( counters.equals( "" ) )
			{
				desiredStream.println( "No active counters." );
			}
			else
			{
				desiredStream.println( "Unexpired counters:" );
				desiredStream.println( counters );
			}
			desiredStream.println();
		}
		else
		{
			List mainList =
				desiredData.equals( "closet" ) ? KoLConstants.closet :
				desiredData.equals( "summary" ) ? KoLConstants.tally :
				desiredData.equals( "storage" ) ? KoLConstants.storage :
				desiredData.equals( "display" ) ? KoLConstants.collection :
				desiredData.equals( "outfits" ) ? EquipmentManager.getOutfits() :
				desiredData.equals( "familiars" ) ? KoLCharacter.getFamiliarList() :
				desiredData.equals( "effects" ) ? KoLConstants.activeEffects :
				KoLConstants.inventory;

			if ( desiredData.startsWith( "skills" ) )
			{
				mainList = KoLConstants.availableSkills;
				filter = filter.toLowerCase();

				if ( filter.startsWith( "cast" ) )
				{
					mainList = new ArrayList();
					mainList.addAll( KoLConstants.availableSkills );

					List intersect = SkillDatabase.getSkillsByType( SkillDatabase.CASTABLE );
					mainList.retainAll( intersect );
					filter = "";
				}

				if ( filter.startsWith( "pass" ) )
				{
					mainList = new ArrayList();
					mainList.addAll( KoLConstants.availableSkills );

					List intersect = SkillDatabase.getSkillsByType( SkillDatabase.PASSIVE );
					mainList.retainAll( intersect );
					filter = "";
				}

				if ( filter.startsWith( "self" ) )
				{
					mainList = new ArrayList();
					mainList.addAll( KoLConstants.availableSkills );

					List intersect = SkillDatabase.getSkillsByType( SkillDatabase.SELF_ONLY );
					mainList.retainAll( intersect );
					filter = "";
				}

				if ( filter.startsWith( "buff" ) )
				{
					mainList = new ArrayList();
					mainList.addAll( KoLConstants.availableSkills );

					List intersect = SkillDatabase.getSkillsByType( SkillDatabase.BUFF );
					mainList.retainAll( intersect );
					filter = "";
				}

				if ( filter.startsWith( "combat" ) )
				{
					mainList = new ArrayList();
					mainList.addAll( KoLConstants.availableSkills );

					List intersect = SkillDatabase.getSkillsByType( SkillDatabase.COMBAT );
					mainList.retainAll( intersect );
					filter = "";
				}
			}

			if ( filter.equals( "" ) )
			{
				RequestLogger.printList( mainList, desiredStream );
			}
			else
			{
				String currentItem;
				List resultList = new ArrayList();

				Object[] items = new Object[ mainList.size() ];
				mainList.toArray( items );

				for ( int i = 0; i < items.length; ++i )
				{
					currentItem = StringUtilities.getCanonicalName( items[ i ].toString() );
					if ( currentItem.indexOf( filter ) != -1 )
					{
						resultList.add( items[ i ] );
					}
				}

				RequestLogger.printList( resultList, desiredStream );
			}
		}
	}

	private static final String getStatString( final int base, final int adjusted, final int tnp )
	{
		StringBuffer statString = new StringBuffer();
		statString.append( KoLConstants.COMMA_FORMAT.format( adjusted ) );

		if ( base != adjusted )
		{
			statString.append( " (" + KoLConstants.COMMA_FORMAT.format( base ) + ")" );
		}

		statString.append( ", tnp = " );
		statString.append( KoLConstants.COMMA_FORMAT.format( tnp ) );

		return statString.toString();
	}

	/**
	 * Utility method which determines the first effect which matches the given parameter string. Note that the string
	 * may also specify an effect duration before the string.
	 */

	public static final AdventureResult getFirstMatchingEffect( final String parameters )
	{
		String effectName = null;
		int duration = 0;

		// First, allow for the person to type without specifying
		// the amount, if the amount is 1.

		List matchingNames = EffectDatabase.getMatchingNames( parameters );

		if ( matchingNames.size() != 0 )
		{
			effectName = (String) matchingNames.get( 0 );
			duration = 1;
		}
		else
		{
			String durationString = parameters.split( " " )[ 0 ];
			String effectNameString = parameters.substring( durationString.length() ).trim();

			matchingNames = EffectDatabase.getMatchingNames( effectNameString );

			if ( matchingNames.size() == 0 )
			{
				KoLmafia.updateDisplay(
					KoLConstants.ERROR_STATE,
					"[" + effectNameString + "] does not match anything in the status effect database." );
				return null;
			}

			effectName = (String) matchingNames.get( 0 );
			duration = durationString.equals( "*" ) ? 0 : StringUtilities.parseInt( durationString );
		}

		if ( effectName == null )
		{
			KoLmafia.updateDisplay(
				KoLConstants.ERROR_STATE, "[" + parameters + "] does not match anything in the status effect database." );
			return null;
		}

		return new AdventureResult( effectName, duration, true );
	}

	/**
	 * A special module used specifically for properly instantiating ClanStorageRequests which send things to the clan
	 * stash.
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
			{
				parameters = parameters.substring( 3 ).trim();
			}
		}

		Object[] itemList = ItemFinder.getMatchingItemList( ClanManager.getStash(), parameters );
		if ( itemList.length == 0 )
		{
			return;
		}

		if ( ClanManager.getStash().isEmpty() && isWithdraw )
		{
			RequestThread.postRequest( new ClanStashRequest() );
		}

		RequestThread.postRequest( new ClanStashRequest(
			itemList, isWithdraw ? ClanStashRequest.STASH_TO_ITEMS : ClanStashRequest.ITEMS_TO_STASH ) );
	}

	/**
	 * Untinkers an item (not specified). This is generally not used by the CLI interface, but is defined to override
	 * the abstract method provided in the KoLmafia class.
	 */

	public void executeUntinkerRequest( final String parameters )
	{
		if ( parameters.equals( "" ) )
		{
			UntinkerRequest.completeQuest();
			return;
		}

		ItemFinder.setMatchType( ItemFinder.UNTINKER_MATCH );
		Object[] itemList = ItemFinder.getMatchingItemList( KoLConstants.inventory, parameters );
		ItemFinder.setMatchType( ItemFinder.ANY_MATCH );

		RequestThread.openRequestSequence();

		for ( int i = 0; i < itemList.length; ++i )
		{
			if ( itemList[ i ] != null )
			{
				RequestThread.postRequest( new UntinkerRequest(
					( (AdventureResult) itemList[ i ] ).getItemId(), ( (AdventureResult) itemList[ i ] ).getCount() ) );
			}
		}

		RequestThread.closeRequestSequence();
	}

	/**
	 * Set the Canadian Mind Control device to selected setting.
	 */

	public void executeMindControlRequest( final String parameters )
	{
		int setting = StringUtilities.parseInt( parameters );
		RequestThread.postRequest( new MindControlRequest( setting ) );
	}

	/**
	 * Train the player's current familiar
	 */

	private void trainFamiliar( final String parameters )
	{
		// train (base | buffed | turns) <goal>
		String[] split = parameters.split( " " );

		if ( split.length < 2 || split.length > 3 )
		{
			KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "Syntax: train type goal" );
			return;
		}

		String typeString = split[ 0 ].toLowerCase();

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
			KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "Unknown training type: " + typeString );
			return;
		}

		FamiliarTrainingFrame.levelFamiliar( StringUtilities.parseInt( split[ 1 ] ), type, false );
	}

	/**
	 * Show the current state of the player's mushroom plot
	 */

	private void executeMushroomCommand( String parameters )
	{
		String[] split = parameters.split( " " );
		String command = split[ 0 ];

		if ( command.equals( "plant" ) )
		{
			if ( split.length < 3 )
			{
				KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "Syntax: field plant square spore" );
				return;
			}

			String squareString = split[ 1 ];
			int square = StringUtilities.parseInt( squareString );

			// Skip past command and square
			parameters = parameters.substring( command.length() ).trim();
			parameters = parameters.substring( squareString.length() ).trim();

			if ( parameters.indexOf( "mushroom" ) == -1 )
			{
				parameters = parameters.trim() + " mushroom";
			}

			int spore = ItemFinder.getFirstMatchingItem( parameters ).getItemId();

			if ( spore == -1 )
			{
				KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "Unknown spore: " + parameters );
				return;
			}

			MushroomManager.plantMushroom( square, spore );
		}
		else if ( command.equals( "pick" ) )
		{
			if ( split.length < 2 )
			{
				KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "Syntax: field pick square" );
				return;
			}

			String squareString = split[ 1 ];

			int square = StringUtilities.parseInt( squareString );
			MushroomManager.pickMushroom( square, true );
		}
		else if ( command.equals( "harvest" ) )
		{
			MushroomManager.harvestMushrooms();
		}

		String plot = MushroomManager.getMushroomManager( false );

		if ( KoLmafia.permitsContinue() )
		{
			StringBuffer plotDetails = new StringBuffer();
			plotDetails.append( "Current:" );
			plotDetails.append( KoLConstants.LINE_BREAK );
			plotDetails.append( plot );
			plotDetails.append( KoLConstants.LINE_BREAK );
			plotDetails.append( "Forecast:" );
			plotDetails.append( KoLConstants.LINE_BREAK );
			plotDetails.append( MushroomManager.getForecastedPlot( false ) );
			plotDetails.append( KoLConstants.LINE_BREAK );
			RequestLogger.printLine( plotDetails.toString() );
		}
	}

	/**
	 * Play with the telescope
	 */

	private void executeTelescopeRequest( final String parameters )
	{
		if ( KoLCharacter.inBadMoon() )
		{
			KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "Your telescope is unavailable in Bad Moon" );
			return;
		}

		// Find out how good our telescope is.
		KoLCharacter.setTelescope( false );

		int upgrades = KoLCharacter.getTelescopeUpgrades();
		if ( upgrades < 1 )
		{
			KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "You don't have a telescope." );
			return;
		}

		String[] split = parameters.split( " " );
		String command = split[ 0 ];

		if ( command.equals( "look" ) )
		{
			if ( split.length < 2 )
			{
				KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "Syntax: telescope [look] high|low" );
				return;
			}

			command = split[ 1 ];
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
		else
		{
			// Make sure we've looked through the telescope since we last ascended
			KoLCharacter.checkTelescope();
		}

		// Display what you saw through the telescope
		RequestLogger.printLine( "You have a telescope with " + ( upgrades - 1 ) + " additional upgrades" );

		// Every telescope shows you the gates.
		String gates = Preferences.getString( "telescope1" );
		String[] desc = SorceressLairManager.findGateByDescription( gates );
		if ( desc != null )
		{
			String name = SorceressLairManager.gateName( desc );
			String effect = SorceressLairManager.gateEffect( desc );
			String remedy = desc[ 3 ];
			RequestLogger.printLine( "Outer gate: " + name + " (" + effect + "/" + remedy + ")" );
		}
		else
		{
			RequestLogger.printLine( "Outer gate: " + gates + " (unrecognized)" );
		}

		// Upgraded telescopes can show you tower monsters
		for ( int i = 1; i < upgrades; ++i )
		{
			String prop = Preferences.getString( "telescope" + ( i + 1 ) );
			desc = SorceressLairManager.findGuardianByDescription( prop );
			if ( desc != null )
			{
				String name = SorceressLairManager.guardianName( desc );
				String item = SorceressLairManager.guardianItem( desc );
				RequestLogger.printLine( "Tower Guardian #" + i + ": " + name + " (" + item + ")" );
			}
			else
			{
				RequestLogger.printLine( "Tower Guardian #" + i + ": " + prop + " (unrecognized)" );
			}
		}
	}

	/**
	 * A special module used specifically for properly instantiating ClosetRequests which pulls things from
	 * Hagnk's.
	 */

	private void executeHagnkRequest( final String parameters )
	{
		if ( KoLCharacter.inBadMoon() && !KoLCharacter.canInteract() )
		{
			KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "Hagnk's Storage is not available in Bad Moon until you free King Ralph." );
			return;
		}

		Object[] items = ItemFinder.getMatchingItemList( KoLConstants.storage, parameters );

		if ( items.length == 0 )
		{
			return;
		}

		int meatAttachmentCount = 0;

		for ( int i = 0; i < items.length; ++i )
		{
			if ( ( (AdventureResult) items[ i ] ).getName().equals( AdventureResult.MEAT ) )
			{
				RequestThread.postRequest( new ClosetRequest(
					( (AdventureResult) items[ i ] ).getCount(), ClosetRequest.PULL_MEAT_FROM_STORAGE ) );

				items[ i ] = null;
				++meatAttachmentCount;
			}
		}

		if ( meatAttachmentCount == items.length )
		{
			return;
		}

		// Double check to make sure you have all items on hand
		// since a failure to get something from Hagnk's is bad.

		int storageCount;
		AdventureResult item;

		for ( int i = 0; i < items.length; ++i )
		{
			item = (AdventureResult) items[ i ];
			storageCount = item.getCount( KoLConstants.storage );

			if ( items[ i ] != null && storageCount < item.getCount() )
			{
				KoLmafia.updateDisplay(
					KoLConstants.ERROR_STATE,
					"You only have " + storageCount + " " + item.getName() + " in storage (you wanted " + item.getCount() + ")" );
			}
		}

		RequestThread.postRequest( new ClosetRequest( ClosetRequest.STORAGE_TO_INVENTORY, items ) );
	}

	/**
	 * A special module used specifically for properly instantiating ItemManageRequests which manage the closet.
	 */

	private void executeClosetManageRequest( final String parameters )
	{
		if ( parameters.length() <= 4 )
		{
			RequestLogger.printList( KoLConstants.closet );
			return;
		}

		if ( !parameters.startsWith( "take" ) && !parameters.startsWith( "put" ) )
		{
			KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "Invalid closet command." );
			return;
		}

		boolean isTake = parameters.startsWith( "take" );
		Object[] itemList = ItemFinder.getMatchingItemList(
			isTake ? KoLConstants.closet : KoLConstants.inventory, parameters.substring( 4 ).trim() );

		if ( itemList.length == 0 )
		{
			return;
		}

		int meatAttachmentCount = 0;

		for ( int i = 0; i < itemList.length; ++i )
		{
			if ( ( (AdventureResult) itemList[ i ] ).getName().equals( AdventureResult.MEAT ) )
			{
				RequestThread.postRequest( new ClosetRequest(
					isTake ? ClosetRequest.MEAT_TO_INVENTORY : ClosetRequest.MEAT_TO_CLOSET,
					( (AdventureResult) itemList[ i ] ).getCount() ) );

				itemList[ i ] = null;
				++meatAttachmentCount;
			}
		}

		if ( meatAttachmentCount == itemList.length )
		{
			return;
		}

		RequestThread.postRequest( new ClosetRequest(
			parameters.startsWith( "take" ) ? ClosetRequest.CLOSET_TO_INVENTORY : ClosetRequest.INVENTORY_TO_CLOSET,
			itemList ) );
	}

	/**
	 * A special module used specifically for properly instantiating SellStuffRequests which send things to the mall.
	 */

	private void executeMallSellRequest( final String parameters )
	{
		String[] itemNames = parameters.split( "\\s*,\\s*" );

		AdventureResult[] items = new AdventureResult[ itemNames.length ];
		int[] prices = new int[ itemNames.length ];
		int[] limits = new int[ itemNames.length ];

		int separatorIndex;
		String description;

		for ( int i = 0; i < itemNames.length; ++i )
		{
			separatorIndex = itemNames[ i ].indexOf( "@" );

			if ( separatorIndex != -1 )
			{
				description = itemNames[ i ].substring( separatorIndex + 1 ).trim();
				itemNames[ i ] = itemNames[ i ].substring( 0, separatorIndex );

				separatorIndex = description.indexOf( "limit" );

				if ( separatorIndex != -1 )
				{
					limits[ i ] = StringUtilities.parseInt( description.substring( separatorIndex + 5 ) );
					description = description.substring( 0, separatorIndex ).trim();
				}

				prices[ i ] = StringUtilities.parseInt( description );
			}

			items[ i ] = ItemFinder.getFirstMatchingItem( itemNames[ i ], false );

			if ( items[ i ] == null )
			{
				int spaceIndex = itemNames[ i ].lastIndexOf( " " );
				if ( spaceIndex == -1 )
				{
					continue;
				}

				prices[ i ] = StringUtilities.parseInt( parameters.substring( spaceIndex + 1 ) );
				itemNames[ i ] = itemNames[ i ].substring( 0, spaceIndex ).trim();
				items[ i ] = ItemFinder.getFirstMatchingItem( itemNames[ i ], false );
			}

			if ( itemNames[ i ] == null )
			{
				int spaceIndex = itemNames[ i ].lastIndexOf( " " );
				if ( spaceIndex == -1 )
				{
					return;
				}

				limits[ i ] = prices[ i ];
				prices[ i ] = StringUtilities.parseInt( itemNames[ i ].substring( spaceIndex + 1 ) );
				itemNames[ i ] = itemNames[ i ].substring( 0, spaceIndex ).trim();

				items[ i ] = ItemFinder.getFirstMatchingItem( itemNames[ i ], false );
			}

			if ( items[ i ] == null )
			{
				continue;
			}

			int inventoryCount = items[ i ].getCount( KoLConstants.inventory );

			if ( items[ i ].getCount() == 1 && !itemNames[ i ].startsWith( "1" ) )
			{
				items[ i ] = items[ i ].getInstance( inventoryCount );
			}
			else if ( items[ i ].getCount() != inventoryCount )
			{
				items[ i ] = items[ i ].getInstance( Math.min( items[ i ].getCount(), inventoryCount ) );
			}
		}

		RequestThread.postRequest( new SellStuffRequest( items, prices, limits, SellStuffRequest.AUTOMALL ) );
	}

	/**
	 * A special module used specifically for properly instantiating SellStuffRequests for just autoselling items.
	 */

	private void executeSellStuffRequest( final String parameters )
	{
		Object[] items = ItemFinder.getMatchingItemList( KoLConstants.inventory, parameters );
		if ( items.length == 0 )
		{
			return;
		}

		RequestThread.postRequest( new SellStuffRequest( items, SellStuffRequest.AUTOSELL ) );
	}

	/**
	 * Utility method used to make a purchase from the Kingdom of Loathing mall. What this does is create a mall search
	 * request, and buys the given quantity of items.
	 */

	private void executeBuyCommand( final String parameters )
	{
		Object[] itemList = ItemFinder.getMatchingItemList( KoLConstants.inventory, parameters );

		for ( int i = 0; i < itemList.length; ++i )
		{
			AdventureResult match = (AdventureResult) itemList[ i ];

			if ( !KoLCharacter.canInteract() && !NPCStoreDatabase.contains( match.getName() ) )
			{
				KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "You are not yet out of ronin." );
				return;
			}

			ArrayList results = new ArrayList();

			StoreManager.searchMall( '\"' + ItemDatabase.getItemName( match.getItemId() ) + '\"', results, 10, false );
			StaticEntity.getClient().makePurchases( results, results.toArray(), match.getCount(), false );
		}
	}

	/**
	 * A special module used specifically for properly instantiating ItemCreationRequests.
	 */

	private void executeItemCreationRequest( final String parameters )
	{
		if ( parameters.equals( "" ) )
		{
			RequestLogger.printList( ConcoctionDatabase.getCreatables() );
			return;
		}

		ItemFinder.setMatchType( ItemFinder.CREATE_MATCH );
		Object[] itemList = ItemFinder.getMatchingItemList( KoLConstants.inventory, parameters );
		ItemFinder.setMatchType( ItemFinder.ANY_MATCH );

		AdventureResult currentMatch;
		CreateItemRequest irequest;

		for ( int i = 0; i < itemList.length; ++i )
		{
			currentMatch = (AdventureResult) itemList[ i ];
			if ( itemList[ i ] == null )
			{
				continue;
			}

			irequest = CreateItemRequest.getInstance( currentMatch.getItemId() );

			if ( irequest == null )
			{
				switch ( ConcoctionDatabase.getMixingMethod( currentMatch.getItemId() ) )
				{
				case KoLConstants.COOK:
				case KoLConstants.COOK_REAGENT:
				case KoLConstants.SUPER_REAGENT:
				case KoLConstants.COOK_PASTA:

					KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "You cannot cook without a chef-in-the-box." );
					return;

				case KoLConstants.MIX:
				case KoLConstants.MIX_SPECIAL:
				case KoLConstants.MIX_SUPER:

					KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "You cannot mix without a bartender-in-the-box." );
					return;

				default:

					KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "That item cannot be created." );
					return;
				}
			}

			irequest.setQuantityNeeded( currentMatch.getCount() );
			RequestThread.postRequest( irequest );
		}
	}

	/**
	 * A special module used specifically for properly instantiating UseItemRequests.
	 */

	private void executeUseItemRequest( final String command, final String parameters )
	{
		if ( command.equals( "eat" ) )
		{
			if ( this.makeHellKitchenRequest( parameters ) )
			{
				return;
			}
			if ( this.makeChezSnooteeRequest( parameters ) )
			{
				return;
			}
		}

		if ( command.equals( "drink" ) )
		{
			if ( this.makeHellKitchenRequest( parameters ) )
			{
				return;
			}
			if ( this.makeMicroBreweryRequest( parameters ) )
			{
				return;
			}
		}

		// Now, handle the instance where the first item is actually
		// the quantity desired, and the next is the amount to use

		AdventureResult currentMatch;

		if ( command.equals( "eat" ) || command.equals( "ghost" ) )
		{
			ItemFinder.setMatchType( ItemFinder.FOOD_MATCH );
		}
		else if ( command.equals( "drink" ) || command.equals( "hobo" ) )
		{
			ItemFinder.setMatchType( ItemFinder.BOOZE_MATCH );
		}
		else
		{
			ItemFinder.setMatchType( ItemFinder.USE_MATCH );
		}

		Object[] itemList = ItemFinder.getMatchingItemList( KoLConstants.inventory, parameters );

		ItemFinder.setMatchType( ItemFinder.ANY_MATCH );

		for ( int i = 0; i < itemList.length; ++i )
		{
			currentMatch = (AdventureResult) itemList[ i ];

			if ( command.equals( "eat" ) || command.equals( "ghost" ) )
			{
				if ( ItemDatabase.getConsumptionType( currentMatch.getItemId() ) != KoLConstants.CONSUME_EAT )
				{
					KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, currentMatch.getName() + " cannot be consumed." );
					return;
				}
			}

			if ( command.equals( "drink" ) || command.equals( "hobo" ) )
			{
				if ( ItemDatabase.getConsumptionType( currentMatch.getItemId() ) != KoLConstants.CONSUME_DRINK )
				{
					KoLmafia.updateDisplay(
						KoLConstants.ERROR_STATE, currentMatch.getName() + " is not an alcoholic beverage." );
					return;
				}
			}

			if ( command.equals( "use" ) )
			{
				switch ( ItemDatabase.getConsumptionType( currentMatch.getItemId() ) )
				{
				case KoLConstants.CONSUME_EAT:
					KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, currentMatch.getName() + " must be eaten." );
					return;
				case KoLConstants.CONSUME_DRINK:
					KoLmafia.updateDisplay(
						KoLConstants.ERROR_STATE, currentMatch.getName() + " is an alcoholic beverage." );
					return;
				}
			}

			UseItemRequest request =
				command.equals( "hobo" ) ? new UseItemRequest( KoLConstants.CONSUME_HOBO, currentMatch ) :
				command.equals( "ghost" ) ? new UseItemRequest( KoLConstants.CONSUME_GHOST, currentMatch ) :
				new UseItemRequest( currentMatch );

			RequestThread.postRequest( request );
		}
	}

	/**
	 * A special module for instantiating display case management requests, strictly for adding and removing things.
	 */

	private void executeDisplayCaseRequest( final String parameters )
	{
		if ( KoLConstants.collection.isEmpty() )
		{
			RequestThread.postRequest( new DisplayCaseRequest() );
		}

		if ( parameters.length() == 0 )
		{
			RequestLogger.printList( KoLConstants.collection );
			return;
		}

		if ( !parameters.startsWith( "put" ) && !parameters.startsWith( "take" ) )
		{
			this.showData( "display " + parameters );
			return;
		}

		boolean isTake = parameters.startsWith( "take" );

		Object[] items = ItemFinder.getMatchingItemList(
			isTake ? KoLConstants.collection : KoLConstants.inventory, parameters.substring( 4 ).trim() );

		if ( items.length == 0 )
		{
			return;
		}

		RequestThread.postRequest( new DisplayCaseRequest( items, !isTake ) );
	}

	/**
	 * A special module used specifically for properly instantiating AdventureRequests, including HermitRequests, if
	 * necessary.
	 */

	private void executeAdventureRequest( final String parameters )
	{
		int adventureCount;
		KoLAdventure adventure =
			AdventureDatabase.getAdventure( parameters.equalsIgnoreCase( "last" ) ? Preferences.getString( "lastAdventure" ) : parameters );

		if ( adventure != null )
		{
			adventureCount = 1;
		}
		else
		{
			String adventureCountString = parameters.split( " " )[ 0 ];
			adventureCount = adventureCountString.equals( "*" ) ? 0 : StringUtilities.parseInt( adventureCountString );

			if ( adventureCount == 0 && !adventureCountString.equals( "0" ) && !adventureCountString.equals( "*" ) )
			{
				KoLmafia.updateDisplay(
					KoLConstants.ERROR_STATE, parameters + " does not exist in the adventure database." );
				return;
			}

			String adventureName = parameters.substring( adventureCountString.length() ).trim();
			adventure = AdventureDatabase.getAdventure( adventureName );

			if ( adventure == null )
			{
				KoLmafia.updateDisplay(
					KoLConstants.ERROR_STATE, parameters + " does not exist in the adventure database." );
				return;
			}

			if ( adventureCount <= 0 && adventure.getFormSource().equals( "shore.php" ) )
			{
				adventureCount += (int) Math.floor( KoLCharacter.getAdventuresLeft() / 3 );
			}
			else if ( adventureCount <= 0 )
			{
				adventureCount += KoLCharacter.getAdventuresLeft();
			}
		}

		if ( KoLmafiaCLI.isExecutingCheckOnlyCommand )
		{
			RequestLogger.printLine( adventure.toString() );
			return;
		}

		StaticEntity.getClient().makeRequest( adventure, adventureCount );
	}

	/**
	 * Special module used specifically for properly instantiating requests to change the user's outfit.
	 */

	private void executeChangeOutfitCommand( final String parameters )
	{
		SpecialOutfit intendedOutfit = KoLmafiaCLI.getMatchingOutfit( parameters );

		if ( intendedOutfit == null )
		{
			KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "You can't wear that outfit." );
			return;
		}

		EquipmentManager.retrieveOutfit( intendedOutfit.getOutfitId() );
		RequestThread.postRequest( new EquipmentRequest( intendedOutfit ) );
	}

	public static final SpecialOutfit getMatchingOutfit( final String name )
	{
		String lowercaseName = name.toLowerCase().trim();

		if ( lowercaseName.equals( "birthday suit" ) || lowercaseName.equals( "nothing" ) )
		{
			return SpecialOutfit.BIRTHDAY_SUIT;
		}

		List customOutfitList = EquipmentManager.getCustomOutfits();
		int customOutfitCount = customOutfitList.size();
		int normalOutfitCount = EquipmentDatabase.getOutfitCount();

		// Check for exact matches.

		for ( int i = 0; i < customOutfitCount; ++i )
		{
			SpecialOutfit outfit = (SpecialOutfit) customOutfitList.get( i );

			if ( lowercaseName.equals( outfit.toString().toLowerCase() ) )
			{
				return outfit;
			}
		}

		for ( int i = 0; i < normalOutfitCount; ++i )
		{
			SpecialOutfit outfit = EquipmentDatabase.getOutfit( i );

			if ( outfit != null && lowercaseName.equals( outfit.toString().toLowerCase() ) )
			{
				return outfit;
			}
		}

		// Check for substring matches.

		for ( int i = 0; i < customOutfitCount; ++i )
		{
			SpecialOutfit outfit = (SpecialOutfit) customOutfitList.get( i );

			if ( outfit.toString().toLowerCase().indexOf( lowercaseName ) != -1 )
			{
				return outfit;
			}
		}

		for ( int i = 0; i < normalOutfitCount; ++i )
		{
			SpecialOutfit outfit = EquipmentDatabase.getOutfit( i );

			if ( outfit != null && outfit.toString().toLowerCase().indexOf( lowercaseName ) != -1 )
			{
				return outfit;
			}
		}

		return null;
	}

	/**
	 * A special module used specifically for properly instantiating the BuffBot and running it
	 */

	private void executeBuffBotCommand( final String parameters )
	{
		BuffBotManager.loadSettings();
		BuffBotManager.runBuffBot( StringUtilities.parseInt( parameters ) );
		KoLmafia.updateDisplay( "Buffbot execution complete." );
	}

	/**
	 * Attempts to remove the effect specified in the most recent command. If the string matches multiple effects, all
	 * matching effects will be removed.
	 */

	public void executeUneffectRequest( final String parameters )
	{
		List matchingEffects = EffectDatabase.getMatchingNames( parameters );
		if ( matchingEffects.isEmpty() )
		{
			KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "Unknown effect: " + parameters );
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
				buffToCheck = (String) matchingEffects.get( i );
				if ( UneffectRequest.isShruggable( buffToCheck ) )
				{
					++shruggableCount;
					buffToRemove = new AdventureResult( buffToCheck, 1, true );
				}
			}

			if ( shruggableCount == 1 )
			{
				if ( KoLConstants.activeEffects.contains( buffToRemove ) )
				{
					if ( KoLmafiaCLI.isExecutingCheckOnlyCommand )
					{
						RequestLogger.printLine( buffToRemove.toString() );
						return;
					}

					RequestThread.postRequest( new UneffectRequest( buffToRemove ) );
				}

				return;
			}

			KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "Ambiguous effect name: " + parameters );
			RequestLogger.printList( matchingEffects );

			return;
		}

		AdventureResult effect = new AdventureResult( (String) matchingEffects.get( 0 ), 1, true );

		if ( KoLmafiaCLI.isExecutingCheckOnlyCommand )
		{
			RequestLogger.printLine( effect.toString() );
			return;
		}

		if ( KoLConstants.activeEffects.contains( effect ) )
		{
			RequestThread.postRequest( new UneffectRequest( effect ) );
		}
	}

	/**
	 * Attempts to zap the specified item with the specified wand
	 */

	public void makeZapRequest()
	{
		if ( this.currentLine == null )
		{
			return;
		}

		if ( !this.currentLine.startsWith( "zap" ) || this.currentLine.indexOf( " " ) == -1 )
		{
			return;
		}

		String command = this.currentLine.split( " " )[ 0 ];
		String parameters = this.currentLine.substring( command.length() ).trim();

		if ( parameters.length() == 0 )
		{
			KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "Zap what?" );
			return;
		}

		Object[] itemList = ItemFinder.getMatchingItemList( KoLConstants.inventory, parameters );

		for ( int i = 0; i < itemList.length; ++i )
		{
			RequestThread.postRequest( new ZapRequest( (AdventureResult) itemList[ i ] ) );
		}
	}

	/**
	 * Attempts to smash the specified item
	 */

	public void makePulverizeRequest( final String parameters )
	{
		Object[] itemList = ItemFinder.getMatchingItemList( KoLConstants.inventory, parameters );

		for ( int i = 0; i < itemList.length; ++i )
		{
			RequestThread.postRequest( new PulverizeRequest( (AdventureResult) itemList[ i ] ) );
		}
	}

	/**
	 * Attempts to visit the Styx Pixie
	 */

	public void executeStyxRequest( final String parameters )
	{
		if ( !KoLCharacter.inBadMoon() )
		{
			KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "You can't find the Styx unless you are in Bad Moon." );
			return;
		}

		String[] split = parameters.split( " " );
		String command = split[ 0 ];

		if ( command.equalsIgnoreCase( "muscle" ) )
		{
			RequestThread.postRequest( new StyxPixieRequest( KoLConstants.MUSCLE ) );
		}
		else if ( command.equalsIgnoreCase( "mysticality" ) )
		{
			RequestThread.postRequest( new StyxPixieRequest( KoLConstants.MYSTICALITY ) );
		}
		else if ( command.equalsIgnoreCase( "moxie" ) )
		{
			RequestThread.postRequest( new StyxPixieRequest( KoLConstants.MOXIE ) );
		}
		else
		{
			KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "You can only buff muscle, mysticality, or moxie." );
			return;
		}

	}

	/**
	 * Attempts to visit the Raffle House
	 */

	public void executeRaffleRequest( final String parameters )
	{
		String[] split = parameters.split( " " );
		int count = StringUtilities.parseInt( split[ 0 ] );

		if ( split.length == 1 )
		{
			RequestThread.postRequest( new RaffleRequest( count ) );
			return;
		}

		int source;

		if ( split[1].equals( "inventory" ) )
		{
			source = RaffleRequest.INVENTORY;
		}
		else if ( split[1].equals( "storage" ) )
		{
			source = RaffleRequest.STORAGE;
		}
		else
		{
			KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "You can only get meat from inventory or storage." );
			return;
		}

		RequestThread.postRequest( new RaffleRequest( count, source ) );
	}

	/**
	 * Attempts to listen to a concert at the Arena
	 */

	public void executeArenaRequest( final String parameters )
	{
		IslandArenaRequest request = null;

		if ( Character.isDigit( parameters.charAt( 0 ) ) )
		{
			request = new IslandArenaRequest( StringUtilities.parseInt( parameters ) );
		}
		else
		{
			request = new IslandArenaRequest( parameters );
		}

		RequestThread.postRequest( request );
	}

	/**
	 * Attempts to get a blessing from the Deep Fat Friars
	 */

	public void executeFriarRequest( final String parameters )
	{
		String[] split = parameters.split( " " );
		String command = split[ 0 ];

		if ( command.equals( "blessing" ) )
		{
			if ( split.length < 2 )
			{
				KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "Syntax: friars [blessing] food|familiar|booze" );
				return;
			}

			command = split[ 1 ];
		}

		int action = 0;

		if ( Character.isDigit( command.charAt( 0 ) ) )
		{
			action = StringUtilities.parseInt( command );
		}
		else
		{
			for ( int i = 0; i < FriarRequest.BLESSINGS.length; ++i )
			{
				if ( command.equalsIgnoreCase( FriarRequest.BLESSINGS[ i ] ) )
				{
					action = i + 1;
					break;
				}
			}
		}

		if ( action < 1 || action > 3 )
		{
			KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "Syntax: friars [blessing] food|familiar|booze" );
			return;
		}

		RequestThread.postRequest( new FriarRequest( action ) );
	}

	/**
	 * Retrieves the items specified in the most recent command. If there are no clovers available, the request will
	 * abort.
	 */

	public void executeHermitRequest( String parameters )
	{
		boolean clovers = HermitRequest.isCloverDay();

		if ( !KoLmafia.permitsContinue() )
		{
			return;
		}

		if ( parameters.equals( "" ) )
		{
			KoLmafia.updateDisplay( "Today is " + ( clovers ? "" : "not " ) + "a clover day." );
			return;
		}

		int count = 1;

		if ( Character.isDigit( parameters.charAt( 0 ) ) )
		{
			int spaceIndex = parameters.indexOf( " " );
			count = StringUtilities.parseInt( parameters.substring( 0, spaceIndex ) );
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

		for ( int i = 0; i < KoLConstants.hermitItems.size() && itemId == -1; ++i )
		{
			if ( KoLConstants.hermitItems.get( i ).toString().toLowerCase().indexOf( parameters ) != -1 )
			{
				if ( KoLmafiaCLI.isExecutingCheckOnlyCommand )
				{
					RequestLogger.printLine( ( (AdventureResult) KoLConstants.hermitItems.get( i ) ).getName() );
					return;
				}

				itemId = ( (AdventureResult) KoLConstants.hermitItems.get( i ) ).getItemId();
				if ( itemId == ItemPool.TEN_LEAF_CLOVER && count != 1 )
				{
					count = Math.min( count, ( (AdventureResult) KoLConstants.hermitItems.get( i ) ).getCount() );
				}
			}
		}

		if ( itemId == -1 )
		{
			KoLmafia.updateDisplay(
				KoLConstants.ERROR_STATE, "You can't get a " + parameters + " from the hermit today." );
			return;
		}

		RequestThread.postRequest( new HermitRequest( itemId, count ) );
	}

	/**
	 * Makes a request to Hell's Kitchen to purchase an item. If the item is not available, this method does not report
	 * an error.
	 */

	public boolean makeHellKitchenRequest( final String parameters )
	{
		if ( !KoLCharacter.inBadMoon() )
		{
			return false;
		}

		if ( KoLConstants.kitchenItems.isEmpty() )
		{
			HellKitchenRequest.getMenu();
		}

		if ( parameters.equals( "" ) )
		{
			return false;
		}

		String[] splitParameters = this.splitCountAndName( parameters );
		String countString = splitParameters[ 0 ];
		String nameString = splitParameters[ 1 ];

		for ( int i = 0; i < KoLConstants.kitchenItems.size(); ++i )
		{
			String name = (String) KoLConstants.kitchenItems.get( i );

			if ( !StringUtilities.substringMatches( name.toLowerCase(), nameString, false ) )
			{
				continue;
			}

			if ( KoLmafiaCLI.isExecutingCheckOnlyCommand )
			{
				RequestLogger.printLine( name );
				return true;
			}

			int count = countString == null || countString.length() == 0 ? 1 : StringUtilities.parseInt( countString );

			if ( count == 0 )
			{
				if ( name.equals( "Imp Ale" ) )
				{
					int inebriety = ItemDatabase.getInebriety( name );
					if ( inebriety > 0 )
					{
						count = ( KoLCharacter.getInebrietyLimit() - KoLCharacter.getInebriety() ) / inebriety;
					}
				}
				else
				{
					int fullness = ItemDatabase.getFullness( name );
					if ( fullness > 0 )
					{
						count = ( KoLCharacter.getFullnessLimit() - KoLCharacter.getFullness() ) / fullness;
					}
				}
			}

			for ( int j = 0; j < count; ++j )
			{
				RequestThread.postRequest( new HellKitchenRequest( name ) );
			}

			return true;
		}

		return false;
	}

	/**
	 * Makes a request to the restaurant to purchase a meal. If the item is not available, this method does not report
	 * an error.
	 */

	public boolean makeChezSnooteeRequest( final String parameters )
	{
		if ( !KoLCharacter.inMysticalitySign() )
		{
			return false;
		}

		if ( KoLConstants.restaurantItems.isEmpty() )
		{
			ChezSnooteeRequest.getMenu();
		}

		if ( parameters.equals( "" ) )
		{
			RequestLogger.printLine( "Today's Special: " + ChezSnooteeRequest.getDailySpecial() );
			return false;
		}

		String[] splitParameters = this.splitCountAndName( parameters );
		String countString = splitParameters[ 0 ];
		String nameString = splitParameters[ 1 ].toLowerCase();

		if ( nameString.equalsIgnoreCase( "daily special" ) )
		{
			nameString = ChezSnooteeRequest.getDailySpecial().getName().toLowerCase();
		}

		for ( int i = 0; i < KoLConstants.restaurantItems.size(); ++i )
		{
			String name = (String) KoLConstants.restaurantItems.get( i );

			if ( !StringUtilities.substringMatches( name.toLowerCase(), nameString, false ) )
			{
				continue;
			}

			if ( KoLmafiaCLI.isExecutingCheckOnlyCommand )
			{
				RequestLogger.printLine( name );
				return true;
			}

			int count = countString == null || countString.length() == 0 ? 1 : StringUtilities.parseInt( countString );

			if ( count == 0 )
			{
				int fullness = ItemDatabase.getFullness( name );
				if ( fullness > 0 )
				{
					count = ( KoLCharacter.getFullnessLimit() - KoLCharacter.getFullness() ) / fullness;
				}
			}

			for ( int j = 0; j < count; ++j )
			{
				RequestThread.postRequest( new ChezSnooteeRequest( name ) );
			}

			return true;
		}

		return false;
	}

	/**
	 * Makes a request to the microbrewery to purchase a drink. If the item is not available, this method does not
	 * report an error.
	 */

	public boolean makeMicroBreweryRequest( final String parameters )
	{
		if ( !KoLCharacter.inMoxieSign() )
		{
			return false;
		}

		if ( KoLConstants.microbreweryItems.isEmpty() )
		{
			MicroBreweryRequest.getMenu();
		}

		if ( parameters.equals( "" ) )
		{
			RequestLogger.printLine( "Today's Special: " + MicroBreweryRequest.getDailySpecial() );
			return false;
		}

		String[] splitParameters = this.splitCountAndName( parameters );
		String countString = splitParameters[ 0 ];
		String nameString = splitParameters[ 1 ].toLowerCase();

		if ( nameString.equalsIgnoreCase( "daily special" ) )
		{
			nameString = MicroBreweryRequest.getDailySpecial().getName().toLowerCase();
		}

		for ( int i = 0; i < KoLConstants.microbreweryItems.size(); ++i )
		{
			String name = (String) KoLConstants.microbreweryItems.get( i );

			if ( !StringUtilities.substringMatches( name.toLowerCase(), nameString, false ) )
			{
				continue;
			}

			if ( KoLmafiaCLI.isExecutingCheckOnlyCommand )
			{
				RequestLogger.printLine( name );
				return true;
			}

			int count = countString == null || countString.length() == 0 ? 1 : StringUtilities.parseInt( countString );

			if ( count == 0 )
			{
				int inebriety = ItemDatabase.getInebriety( name );
				if ( inebriety > 0 )
				{
					count = ( KoLCharacter.getInebrietyLimit() - KoLCharacter.getInebriety() ) / inebriety;
				}
			}

			for ( int j = 0; j < count; ++j )
			{
				RequestThread.postRequest( new MicroBreweryRequest( name ) );
			}

			return true;
		}

		return false;
	}
}
