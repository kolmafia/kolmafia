/**
 * Copyright (c) 2005-2009, KoLmafia development team
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
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.java.dev.spellcast.utilities.DataUtilities;
import net.java.dev.spellcast.utilities.UtilityConstants;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.AdventureDatabase;
import net.sourceforge.kolmafia.persistence.Aliases;
import net.sourceforge.kolmafia.persistence.BuffBotDatabase;
import net.sourceforge.kolmafia.persistence.ConcoctionDatabase;
import net.sourceforge.kolmafia.persistence.DebugDatabase;
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
import net.sourceforge.kolmafia.request.ClanLoungeRequest;
import net.sourceforge.kolmafia.request.ClanRumpusRequest;
import net.sourceforge.kolmafia.request.ClanStashRequest;
import net.sourceforge.kolmafia.request.ClosetRequest;
import net.sourceforge.kolmafia.request.CreateItemRequest;
import net.sourceforge.kolmafia.request.DisplayCaseRequest;
import net.sourceforge.kolmafia.request.DwarfContraptionRequest;
import net.sourceforge.kolmafia.request.DwarfFactoryRequest;
import net.sourceforge.kolmafia.request.EquipmentRequest;
import net.sourceforge.kolmafia.request.FamiliarRequest;
import net.sourceforge.kolmafia.request.FightRequest;
import net.sourceforge.kolmafia.request.FriarRequest;
import net.sourceforge.kolmafia.request.GalaktikRequest;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.request.GrandpaRequest;
import net.sourceforge.kolmafia.request.HellKitchenRequest;
import net.sourceforge.kolmafia.request.HermitRequest;
import net.sourceforge.kolmafia.request.HiddenCityRequest;
import net.sourceforge.kolmafia.request.IslandArenaRequest;
import net.sourceforge.kolmafia.request.LoginRequest;
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
import net.sourceforge.kolmafia.request.StorageRequest;
import net.sourceforge.kolmafia.request.StyxPixieRequest;
import net.sourceforge.kolmafia.request.TelescopeRequest;
import net.sourceforge.kolmafia.request.TransferItemRequest;
import net.sourceforge.kolmafia.request.UneffectRequest;
import net.sourceforge.kolmafia.request.UntinkerRequest;
import net.sourceforge.kolmafia.request.UseItemRequest;
import net.sourceforge.kolmafia.request.UseSkillRequest;
import net.sourceforge.kolmafia.request.WineCellarRequest;
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
import net.sourceforge.kolmafia.session.ResultProcessor;
import net.sourceforge.kolmafia.session.SorceressLairManager;
import net.sourceforge.kolmafia.session.StoreManager;
import net.sourceforge.kolmafia.session.TurnCounter;
import net.sourceforge.kolmafia.swingui.BuffRequestFrame;
import net.sourceforge.kolmafia.swingui.CalendarFrame;
import net.sourceforge.kolmafia.swingui.CouncilFrame;
import net.sourceforge.kolmafia.swingui.FamiliarTrainingFrame;
import net.sourceforge.kolmafia.swingui.ItemManageFrame;
import net.sourceforge.kolmafia.swingui.widget.ShowDescriptionList;
import net.sourceforge.kolmafia.textui.Interpreter;
import net.sourceforge.kolmafia.textui.parsetree.Value;
import net.sourceforge.kolmafia.textui.parsetree.CompositeValue;
import net.sourceforge.kolmafia.utilities.ByteArrayStream;
import net.sourceforge.kolmafia.utilities.CharacterEntities;
import net.sourceforge.kolmafia.utilities.FileUtilities;
import net.sourceforge.kolmafia.utilities.InputFieldUtilities;
import net.sourceforge.kolmafia.utilities.PauseObject;
import net.sourceforge.kolmafia.utilities.PrefixMap;
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
	private static final Pattern HEAD_PATTERN = Pattern.compile( "<head>.*?</head>", Pattern.DOTALL );
	private static final Pattern COMMENT_PATTERN = Pattern.compile( "<!--.*?-->", Pattern.DOTALL );

	private String previousLine = null;
	private String currentLine = null;
	private BufferedReader commandStream;
	private boolean elseValid = false;
	private boolean elseRuns = false;

	public static boolean isExecutingCheckOnlyCommand = false;

	// Flag values for Commands:
	public static int FULL_LINE_CMD = 1;
	public static int FLOW_CONTROL_CMD = 2;

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
			String holiday = HolidayDatabase.getHoliday( true );
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
		String origLine = line;
		line = CharacterEntities.unescape( line );

		line = line.replaceAll( "[ \t]+", " " ).trim();
		if ( line.length() == 0 )
		{
			return;
		}

		// First, handle all the aliasing that may be
		// defined by the user.

		this.currentLine = line = Aliases.apply( line );
		if ( !line.startsWith( "repeat" ) )
		{
			this.previousLine = line;
		}

		while ( KoLmafia.permitsContinue() & line.length() > 0 )
		{
			String command, parameters;
			int splitIndex = line.indexOf( ";" );
			if ( splitIndex != -1 )
			{
				parameters = line.substring( 0, splitIndex ).trim();
				line = line.substring( splitIndex + 1 ).trim();
			}
			else
			{
				parameters = line;
				line = "";
			}
			if ( parameters.length() == 0 )
			{
				continue;
			}
			
			splitIndex = parameters.indexOf( " " );
			// First, check for parameterless commands.
			// Multi-word commands can only match here.
			command = parameters.toLowerCase();
			if ( splitIndex == -1 || (Command.lookup.get( command ) != null &&
				Command.lookup.getKeyType( command ) == PrefixMap.EXACT_KEY) )
			{
				parameters = "";
			}
			else
			{
				command = command.substring( 0, splitIndex );
				parameters = parameters.substring( splitIndex + 1 ).trim();
			}
			
			if ( command.endsWith( "?" ) )
			{
				KoLmafiaCLI.isExecutingCheckOnlyCommand = true;
				command = command.substring( 0, command.length() - 1 );
			}
			Command handler = (Command) Command.lookup.get( command );
			int flags = handler == null ? 0 : handler.flags;
			if ( flags == KoLmafiaCLI.FULL_LINE_CMD && !line.equals( "" ) )
			{
				parameters = parameters + " ; " + line;
				line = "";
			}
			
			if ( flags == KoLmafiaCLI.FLOW_CONTROL_CMD )
			{
				String continuation = this.getContinuation( line );
				handler.continuation = continuation;
				handler.CLI = this;
				RequestThread.openRequestSequence();
				handler.run( command, parameters );
				RequestThread.closeRequestSequence();
				handler.CLI = null;
				KoLmafiaCLI.isExecutingCheckOnlyCommand = false;
				this.previousLine = command + " " + parameters + ";" + continuation;
				return;
			}
			
			RequestThread.openRequestSequence();
			this.executeCommand( command, parameters );
			RequestThread.closeRequestSequence();
			KoLmafiaCLI.isExecutingCheckOnlyCommand = false;
		}
		
		if ( KoLmafia.permitsContinue() )
		{	// Notify user-entered Daily Deeds that the command was successful.
			Preferences.firePreferenceChanged( origLine );
		}
	}

	private String getContinuation( String line )
	{
		StringBuffer block = new StringBuffer( line );
		boolean seenCmd = false, needAnotherCmd = false;
		while ( true )
		{
			while ( line.length() > 0 )
			{
				String command;
				int splitIndex = line.indexOf( ";" );
				if ( splitIndex == -1 )
				{
					command = line.toLowerCase();
					line = "";
				}
				else
				{
					command = line.substring( 0, splitIndex ).toLowerCase();
					line = line.substring( splitIndex + 1 ).trim();
				}
				if ( command.equals( "" ) )
				{
					continue;
				}
				seenCmd = true;
				needAnotherCmd = false;
				command = command.split( " " )[ 0 ];
				if ( command.endsWith( "?" ) )
				{
					command = command.substring( 0, command.length() - 1 );
				}
				Command handler = (Command) Command.lookup.get( command );
				int flags = handler == null ? 0 : handler.flags;
				if ( flags == KoLmafiaCLI.FULL_LINE_CMD )
				{
					line = "";
					break;
				}
				if ( flags == KoLmafiaCLI.FLOW_CONTROL_CMD )
				{
					needAnotherCmd = true;
				}
			}
			if ( seenCmd && !needAnotherCmd )
			{
				return block.toString();
			}
			
			line = this.getNextLine();
			if ( line == null )
			{
				KoLmafia.updateDisplay( KoLConstants.ERROR_STATE,
					"Unterminated conditional statement." );
				return "";
			}
			block.append( ";" );
			block.append( line );
		}
	}

	/**
	 * A utility command which decides, based on the command to be executed, what to be done with it. It can either
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

		if ( parameters.equals( "refresh" ) )
		{
			parameters = command;
			command = "refresh";
		}

		Command handler = (Command) Command.lookup.get( command );
		if ( handler == null )
		{
			handler = Command.getSubstringMatch( command );
		}
		if ( handler != null )
		{
			if ( command.endsWith( "*" ) )
			{
				RequestLogger.printLine( "(A * after a command name indicates that it can be "
					+ "typed in a longer form.  There's no need to type the asterisk!)" );
			}
			handler.CLI = this;
			handler.run( command, parameters );
			handler.CLI = null;
			return;
		}

		// If all else fails, then assume that the
		// person was trying to call a script.

		this.executeScript( command + " " + parameters );
	}

	public static class Command
	{
		// Assign 'flags' in an instance initializer if the command needs one of these:
		// KoLmafiaCLI.FULL_LINE_CMD - the command's parameters are the entire remainder
		//	of the line, semicolons do not end the command.
		// KoLmafiaCLI.FLOW_CONTROL_CMD - the remainder of the command line, plus additional
		//	lines as needed to ensure that at least one command is included, and that the
		//	final command is not itself flagged as FLOW_CONTROL_CMD, are made available to
		//	this command via its 'continuation' field, rather than being executed.  The
		//	command can examine and modify the continuation, and execute it zero or more
		//	times by calling CLI.executeLine(continuation).
		public int flags = 0;
		
		// Assign 'usage' in an instance initializer to set the help usage text.
		// If usage is null, this command won't be shown in the command list.
		public String usage = " - no help available.";
		// Usage strings should start with a space, or [?] if they support the
		// isExecutingCheckOnlyCommand flag, followed by any parameters (with placeholder
		// names enclosed in angle brackets - they'll be italicized in HTML output).
		// There should then be a dash and a brief description of the command.
		
		// Or, override getUsage(cmd) to dynamically construct the usage text
		// (but it would probably be better to have separate commands in that case).
		public String getUsage( String cmd )
		{
			return this.usage;
		}
		
		// Override one of run(cmd, parameters), run(cmd, parameters[]), or run(cmd)
		// to specify the command's action, with different levels of parameter processing.
		public void run( String cmd, String parameters )
		{
			String[] splits = parameters.split( "\\s+" );
			this.run( cmd, splits );
		}
		
		public void run( String cmd, String[] parameters )
		{
			if ( parameters.length > 1 || parameters[ 0 ].length() > 0 )
			{
				KoLmafia.updateDisplay( "(" + cmd + " does not use any parameters)" );
			}
			this.run( cmd );
		}
		
		public void run( String cmd )
		{
			KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, cmd + " is not implemented!" );
		}
		
		// 'CLI' is provided as a reference back to the invoking instance of KoLmafiaCLI,
		// for convenience if the command needs to call any of its non-static methods.
		// Note that this reference can become invalid if another CLI instance is recursively
		// invoked, and happens to execute the same command; any command that uses 'CLI' more
		// than once should put it in a local variable first.
		KoLmafiaCLI CLI;
		
		// FLOW_CONTROL_CMDs will have the command line they're to operate on stored here:
		String continuation;		
		
		// Each command class must be instantiated (probably in a static initializer), and
		// at least one of these methods called on it to add it to the command table.  These
		// methods return 'this', for easy chaining.
		public Command register( String name )
		{	// For commands that must be typed with an exact name
			lookup.putExact( name.toLowerCase(), this );
			this.registerFlags( name );
			return this;
		}
		
		public Command registerPrefix( String prefix )
		{	// For commands that are parsed as startsWith(...)
			lookup.putPrefix( prefix.toLowerCase(), this );
			this.registerFlags( prefix );
			return this;
		}
		
		public Command registerSubstring ( String substring )
		{	// For commands that are parsed as indexOf(...)!=-1.  Use sparingly!
			substring = substring.toLowerCase();
			substringLookup.add( substring );
			substringLookup.add( this );
			// Make it visible in the normal lookup map:
			lookup.putExact( "*" + substring + "*", this );
			this.registerFlags( substring );
			return this;
		}
		
		// Internal implementation thingies:
		static final PrefixMap lookup = new PrefixMap();
		static final ArrayList substringLookup = new ArrayList();
		static private String fullLineCmds = "";
		static private String flowControlCmds = "";
		
		private void registerFlags( String name )
		{
			if ( this.flags == KoLmafiaCLI.FULL_LINE_CMD )
			{
				fullLineCmds += (fullLineCmds.length() == 0) ? name : ", " + name;
			}
			if ( this.flags == KoLmafiaCLI.FLOW_CONTROL_CMD )
			{
				flowControlCmds += (flowControlCmds.length() == 0) ? name : ", " + name;
			}
		}
		
		static Command getSubstringMatch( String cmd )
		{
			for ( int i = 0; i < substringLookup.size(); i += 2 )
			{
				if ( cmd.indexOf( (String) substringLookup.get( i ) ) != -1 )
				{
					return (Command) substringLookup.get( i + 1 );
				}
			}
			return null;
		}
	}

	/**
	 *	Sets whether a following "else" command should be executed.
	 */
	public void elseRuns( boolean shouldRun )
	{
		this.elseRuns = shouldRun;
		this.elseValid = true;
	}

	/**
	 *	Indicates that a following "else" command is not valid here.
	 */
	public void elseInvalid()
	{
		this.elseValid = false;
	}

	/**
	 *	Tests whether a "else" command should be executed, and mark further "else"s
	 *	as invalid.  If this "else" is invalid, generate an error and return false.
	 */
	public boolean elseRuns()
	{
		if ( !this.elseValid )
		{
			KoLmafia.updateDisplay( KoLConstants.ERROR_STATE,
				"'else' must follow a conditional command, and both must be at the outermost level." );
			return false;
		}
		this.elseValid = false;
		return this.elseRuns;
	}

	public static class ConditionalCommand
		extends Command
	{
		{ flags = KoLmafiaCLI.FLOW_CONTROL_CMD; }
	}

	static { new Try().register( "try" ); }
	public static class Try
		extends ConditionalCommand
	{
		{ usage = " ; <commands> - do commands, and continue even if an error occurs."; }
		public void run( String command )
		{
			KoLmafiaCLI CLI = this.CLI;
			CLI.elseInvalid();
			CLI.executeLine( this.continuation );
			if ( KoLmafia.permitsContinue() )
			{
				CLI.elseRuns( false );
			}
			else
			{
				KoLmafia.forceContinue();
				CLI.elseRuns( true );
			}
		}
	}

	static { new Else().register( "else" ); }
	public static class Else
		extends ConditionalCommand
	{
		{ usage = " ; <commands> - do commands if preceding if/while/try didn't execute."; }
		public void run( String command )
		{
			KoLmafiaCLI CLI = this.CLI;
			if ( CLI.elseRuns() )
			{
				CLI.executeLine( this.continuation );
			}
		}
	}

	static { new ElseIf().register( "elseif" ); }
	public static class ElseIf
		extends ConditionalCommand
	{
		{ usage = " <condition>; <commands> - do if condition is true but preceding condition was false."; }
		public void run( String command, String parameters )
		{
			KoLmafiaCLI CLI = this.CLI;
			if ( !CLI.elseRuns() )
			{
				CLI.elseRuns( false );
			}
			else if ( CLI.testConditional( parameters ) )
			{
				CLI.executeLine( this.continuation );
				CLI.elseRuns( false );
			}
			else
			{
				CLI.elseRuns( true );
			}
		}
	}

	static { new If().register( "if" ); }
	public static class If
		extends ConditionalCommand
	{
		{ usage = " <condition>; <commands> - do commands once if condition is true (see condref)."; }
		public void run( String command, String parameters )
		{
			KoLmafiaCLI CLI = this.CLI;
			if ( CLI.testConditional( parameters ) )
			{
				CLI.elseInvalid();
				CLI.executeLine( this.continuation );
				CLI.elseRuns( false );
			}
			else
			{
				CLI.elseRuns( true );
			}
		}
	}

	static { new While().register( "while" ); }
	public static class While
		extends ConditionalCommand
	{
		{ usage = " <condition>; <commands> - do commands repeatedly while condition is true."; }
		public void run( String command, String parameters )
		{
			// must make local copies since the executed commands could overwrite these
			KoLmafiaCLI CLI = this.CLI;
			String continuation = this.continuation;
			
			CLI.elseRuns( true );
			while ( CLI.testConditional( parameters ) )
			{
				CLI.elseInvalid();
				CLI.executeLine( continuation );
				CLI.elseRuns( false );
			}
		}
	}

	static { new CondRef().register( "condref" ); }
	public static class CondRef
		extends Command
	{
		{ usage = " - list <condition>s usable with if/while commands."; }
		public void run( String cmd )
		{
			RequestLogger.printLine( "<table border=2>" +
				"<tr><td colspan=3>today | tomorrow is mus | mys | mox day</td></tr>" +
				"<tr><td colspan=3>class is [not] sauceror | <i>etc.</i></td></tr>" +
				"<tr><td colspan=3>skill list contains | lacks <i>skill</i></td></tr>" +
				"<tr><td>level<br>health<br>mana<br>meat<br>adventures<br>" +
				"inebriety | drunkenness<br>muscle<br>mysticality<br>moxie<br>" +
				"worthless item<br>stickers<br><i>item</i><br><i>effect</i></td>" +
				"<td>=<br>==<br>&lt;&gt;<br>!=<br>&lt;<br>&lt;=<br>&gt;<br>&gt;=</td>" +
				"<td><i>number</i><br><i>number</i>%&nbsp;(health/mana only)<br>" +
				"<i>item</i> (qty in inventory)<br><i>effect</i> (turns remaining)</td>" +
				"</tr></table>" );
		}
	}

	// Command names with embedded spaces can only match the full line, and cannot currently
	// use the isExecutingCheckOnlyCommand, FULL_LINE_CMD, or FLOW_CONTROL_CMD features.

	static { new WinGame().register( "win game" ); }
	public static class WinGame
		extends Command
	{
		{ usage = " - I'm as surprised as you!  I didn't think it was possible."; }
		public void run( String cmd )
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
		}
	}

	static { new SaveAsMood().register( "save as mood" ); }
	public static class SaveAsMood
		extends Command
	{
		{ usage = " - add your current effects to the mood."; }
		public void run( String cmd )
		{
			MoodManager.minimalSet();
			MoodManager.saveSettings();
		}
	}

	static { new BurnExtraMP().register( "burn extra mp" ); }
	public static class BurnExtraMP
		extends Command
	{
		{ usage = " - use some MP for buff extension and summons."; }
		public void run( String cmd )
		{
			CLI.recoverHP();

			SpecialOutfit.createImplicitCheckpoint();
			MoodManager.burnExtraMana( true );
			SpecialOutfit.restoreImplicitCheckpoint();
		}
	}

	static { new ASH().register( "ash" ); }
	public static class ASH
		extends Command
	{
		{ flags = KoLmafiaCLI.FULL_LINE_CMD; }
		{ usage = " <statement> - test a line of ASH code without having to edit a script."; }
		public void run( String cmd, String parameters )
		{
			if ( !parameters.endsWith( ";" ) )
			{
				parameters += ";";
			}
			ByteArrayInputStream istream = new ByteArrayInputStream(
				(parameters + KoLConstants.LINE_BREAK).getBytes() );

			Interpreter interpreter = new Interpreter();
			interpreter.validate( null, istream );
			Value rv = interpreter.execute( "main", null );
			KoLmafia.updateDisplay( "Returned: " + rv );
			if ( rv instanceof CompositeValue )
			{
				dump( (CompositeValue) rv, "" );
			}
		}
		
		private void dump( CompositeValue obj, String indent )
		{
			Value[] keys = obj.keys();
			for ( int i = 0; i < keys.length; ++i )
			{
				Value v = obj.aref( keys[ i ] );
				RequestLogger.printLine( indent + keys[ i ] + " => " + v );
				if ( v instanceof CompositeValue )
				{
					dump( (CompositeValue) v, indent + "&nbsp;&nbsp;" );
				}
			}
		}
	}

	static { new InlineASHScript().register( "<inline-ash-script>" ); }
	public static class InlineASHScript
		extends Command
	{
		{ usage = " - embed an ASH script in a CLI script."; }
		public void run( String cmd )
		{
			ByteArrayStream ostream = new ByteArrayStream();

			String currentLine = CLI.getNextLine();

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

				currentLine = CLI.getNextLine();
			}

			if ( currentLine == null )
			{
				KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "Unterminated inline ASH script." );
				return;
			}

			Interpreter interpreter = new Interpreter();
			interpreter.validate( null, ostream.getByteArrayInputStream() );
			interpreter.execute( "main", null );
		}
	}

	static { new GC().register( "gc" ); }
	public static class GC
		extends Command
	{
		{ usage = " - force Java garbage collection."; }
		public void run( String cmd )
		{
			System.gc();
		}
	}

	static { new Version().register( "version" ); }
	public static class Version
		extends Command
	{
		{ usage = " - display KoLmafia version."; }
		public void run( String cmd )
		{
			RequestLogger.printLine( StaticEntity.getVersion() );
		}
	}

	static { new Gong().register( "gong" ); }
	public static class Gong
		extends Command
	{
		{ usage = " [buy | set] manual | bird | mole | roach [<effect> [<stat> [<stat>]]]"; }
		
		// These are all the possible paths that start with The Gong Has Been Bung.
		public static final String[] GONG_PATHS = {
			"show in browser",
			"bird",
			"mole",
			"roach (in browser)",
			"musc, musc, +30% musc",
			"musc, mox, +30% musc",
			"musc, MP, +30% musc",
			"musc, myst, +30% myst",
			"myst, myst, +30% myst",
			"myst, MP, +30% myst",
			"myst, mox, +30% mox",
			"mox, mox, +30% mox",
			"mox, MP, +30% mox",
			"musc, musc, +10% all",
			"musc, myst, +10% all",
			"musc, mox, +10% all",
			"myst, myst, +10% all",
			"mox, mox, +10% all",
			"mox, MP, +10% all",
			"musc, musc, +50% items",
			"musc, myst, +50% items",
			"musc, MP, +50% items",
			"myst, mox, +50% items",
			"myst, MP, +50% items",
			"mox, MP, +50% items",
			"musc, mox, +30 ML",
			"musc, MP, +30 ML",
			"myst, myst, +30 ML",
			"myst, mox, +30 ML",
			"myst, MP, +30 ML",
			"mox, mox, +30 ML",
		};
		
		// These are the choice adventure settings corresponding to GONG_PATHS.
		// Encoding: 2 bits per choice adv, starting with #276 in the low bits,
		// continuing thru #290 for a total of 30 bits used.
		private static final int[] GONG_CHOICES = {
			0x00000004, 0x00000007, 0x00000006, 0x00000005, 0x00004095, 0x00001055,
			0x000100d5, 0x00300225, 0x00040125, 0x00800325, 0x08000835, 0x02000435, 
			0x10000c35, 0x00008095, 0x00200225, 0x00002055, 0x000c0125, 0x03000435, 
			0x20000c35, 0x0000c095, 0x00100225, 0x000200d5, 0x0c000835, 0x00c00325, 
			0x30000c35, 0x00003055, 0x000300d5, 0x00080125, 0x04000835, 0x00400325, 
			0x01000435
		};
		
		// These are the precalculated roach paths for achieving the closest possible
		// approximation to any desired effect & stat boosts.
		// Index: primary desired stat (musc=0, myst, mox, MP)
		//	+ 4 * secondary desired stat (musc=0, myst, mox, MP)
		//	+ 16 * mainstat (musc=0, myst, mox) as a tiebreaker
		// Values: six 5-bit fields, containing indexes into the arrays above for each
		//	possible effect (+mus%=low bits, +mys%, +mox%, +all%, +item, +ML)
		private static final int[] GONG_SEARCH = {
			0x3336a8e4, 0x374728e4, 0x3367ace5, 0x35593126, 
			0x334728e4, 0x37482904, 0x3967a8e5, 0x3b793126, 
			0x3337ace5, 0x396728e5, 0x3d68ace5, 0x35893126, 
			0x3556b0e6, 0x3b772926, 0x33893125, 0x35593126, 
			
			0x3336a8e4, 0x374728e4, 0x3367a8e5, 0x35593126, 
			0x334728e4, 0x37482904, 0x3968a905, 0x3b793126, 
			0x3347a8e5, 0x39682905, 0x3d68ad05, 0x3b893126, 
			0x355730e6, 0x3b782926, 0x39893125, 0x3b793126, 
			
			0x3336ace4, 0x394728e5, 0x3367ace5, 0x35593126, 
			0x334728e5, 0x37682905, 0x3968a905, 0x3b793126, 
			0x3337ace5, 0x39682905, 0x3d68ace5, 0x35893126, 
			0x3557b0e6, 0x3b782926, 0x3d893125, 0x35893126
		};
		
		public void run( String command, String parameters[] )
		{
			int pos = 0;
			int len = parameters.length;
			boolean buy = false;
			boolean set = false;
			if ( pos < len && parameters[ pos ].equalsIgnoreCase( "buy" ) )
			{
				buy = true;
				++pos;
			}
			else if ( pos < len && parameters[ pos ].equalsIgnoreCase( "set" ) )
			{
				set = true;
				++pos;
			}
			
			if ( pos >= len || parameters[ pos ].equals( "" ) )
			{
				RequestLogger.printLine( "Usage: gong" + this.usage );
				RequestLogger.printLine( "buy - use a gong even if you have to buy one." );
				RequestLogger.printLine( "set - don't use a gong, just set choices." );
				RequestLogger.printLine( "manual - show choices in browser." );
				RequestLogger.printLine( "bird | mole | roach - path to take." );
				RequestLogger.printLine( "'roach' can be followed by 1, 2, or 3 of:" );
				RequestLogger.printLine( "mus | mys | mox | all | item | ML - effect to get (20 turns)." );
				RequestLogger.printLine( "(You can also use the first word of the effect name.)" );
				RequestLogger.printLine( "mus | mys | mox | MP - stat to boost." );
				RequestLogger.printLine( "mus | mys | mox | MP - another stat to boost." );
				RequestLogger.printLine( "(If a stat is not specified, or is impossible due to other choices, your mainstat will be boosted if possible." );
				return;
			}
			
			int path = this.parse( parameters, pos++, "manual bird mole roach" );
			if ( path == 3 && pos < len )
			{
				int effect = this.parse( parameters, pos++,
					"mus ack mys alc mox rad all new item ext ml unp" ) / 2;
				int primary, secondary, main;
				primary = secondary = main = KoLCharacter.getPrimeIndex();
				if ( pos < len )
				{
					primary = this.parse( parameters, pos++, "mus mys mox mp" );
				}
				if ( pos < len )
				{
					secondary = this.parse( parameters, pos++, "mus mys mox mp" );
				}
				path = this.GONG_SEARCH[ primary + 4*secondary + 16*main ];
				path = (path >> (5*effect)) & 0x1F;
			}
			if ( !KoLmafia.permitsContinue() )
			{
				return;
			}
			if ( pos < len )
			{
				KoLmafia.updateDisplay( KoLConstants.ERROR_STATE,
					"Unexpected text after command, starting with: " + parameters[ pos ] );
				return;
			}
			KoLmafia.updateDisplay( "Gong path: " + this.GONG_PATHS[ path ] );			
			this.setPath( path );
			if ( set )
			{
				return;
			}
			AdventureResult gong = ItemPool.get( ItemPool.GONG, 1 );
			if ( buy && !InventoryManager.hasItem( gong ) )
			{
				CLI.executeBuyCommand( "1 llama lama gong" );
			}
			RequestThread.postRequest( new UseItemRequest( gong ) );
		}
		
		private static int parse( String[] parameters, int pos, String optionString )
		{
			if ( pos >= parameters.length )
			{
				KoLmafia.updateDisplay( KoLConstants.ERROR_STATE,
					"Expected one of: " + optionString );
				return 0;
			}
			String[] options = optionString.split( " " );
			String param = parameters[ pos ].toLowerCase();
			for ( int i = 0; i < options.length; ++i )
			{
				if ( param.startsWith( options[ i ] ) )
				{
					return i;
				}
			}
			KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "Found '" + param +
				"', but expected one of: " + optionString );
			return 0;
		}
		
		public static void setPath( int path )
		{
			if ( path < 0 || path > GONG_PATHS.length )
			{
				return;
			}
			Preferences.setInteger( "gongPath", path );
			path = GONG_CHOICES[ path ];
			for( int i = 276; i <= 290; ++i )
			{
				Preferences.setString( "choiceAdventure" + i,
					String.valueOf( path & 0x03 ) );
				path >>= 2;
			}
		}
		
	}

	static { new CCS().register( "ccs" ); }
	public static class CCS
		extends Command
	{
		{ usage = " [<script>] - show [or select] Custom Combat Script in use."; }
		public void run( String command, String parameters )
		{
			if ( parameters.length() > 0 )
			{
				parameters = parameters.toLowerCase();
				for ( Iterator i = CustomCombatManager.getAvailableScripts().iterator();
					i.hasNext(); )
				{
					String script = (String) i.next();
					if ( script.toLowerCase().indexOf( parameters ) != -1 )
					{
						Preferences.setString( "customCombatScript", script );
						Preferences.setString( "battleAction", "custom combat script" );
						KoLmafia.updateDisplay( "CCS set to " + script );
						return;
					}
				}
				KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "No matching CCS found!" );
				return;
			}
			KoLmafia.updateDisplay( "CCS is " +
				Preferences.getString( "customCombatScript" ) );
			if ( !Preferences.getString( "battleAction" ).startsWith( "custom" ) )
			{
				KoLmafia.updateDisplay( "(but isn't currently being used becase a different combat action is specified.)" );
			}
		}
	}

	static { new Holiday().register( "holiday" ); }
	public static class Holiday
		extends Command
	{
		{ usage = " <HolidayName> - enable special processing for unpredicted holidays."; }
		public void run( String command, String parameters )
		{
			KoLCharacter.setHoliday( parameters );
			KoLCharacter.updateStatus();
		}
	}

	static { new Enable().register( "enable" ).register( "disable" ); }
	public static class Enable
		extends Command
	{
		{ usage = " all | <command> [, <command>]... - allow/deny CLI commands."; }
		public void run( String command, String parameters )
		{
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
		}
	}

	static { new Log().register( "log" ); }
	public static class Log
		extends Command
	{
		{ usage = " [status],[equipment],[effects],[<etc>.] - record data, \"log snapshot\" for all common data."; }
		public void run( String cmd, String parameters )
		{
			if ( parameters.equals( "snapshot" ) )
			{
				CLI.executeCommand( "log", "moon, status, equipment, skills, effects, modifiers" );
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

				CLI.showData( options[ i ], true );
			}

			RequestLogger.updateSessionLog();
			RequestLogger.updateSessionLog( "=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=" );

			RequestLogger.getDebugStream().println();
			RequestLogger.getDebugStream().println( "=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=" );

			RequestLogger.updateSessionLog();
			RequestLogger.updateSessionLog();

			RequestLogger.getDebugStream().println();
			RequestLogger.getDebugStream().println();
		}
	}

	static { new Alias().register( "alias" ); }
	public static class Alias
		extends Command
	{
		{ usage = " [ <word> => <expansion> ] - list or create CLI abbreviations."; }
		{ flags = KoLmafiaCLI.FULL_LINE_CMD; }
		public void run( String cmd, String parameters )
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
		}
	}

	static { new Unalias().register( "unalias" ); }
	public static class Unalias
		extends Command
	{
		{ usage = " <word> - remove a CLI abbreviation."; }
		public void run( String cmd, String parameters )
		{
			Aliases.remove( parameters );
		}
	}

	static { new Priphea().register( "priphea" ); }
	public static class Priphea
		extends Command
	{
		{ usage = " - launch KoLmafia GUI."; }
		public void run( String cmd )
		{
			if ( !KoLDesktop.instanceExists() )
			{
				KoLmafiaGUI.checkFrameSettings();
				KoLDesktop.getInstance().initializeTabs();
			}

			KoLDesktop.displayDesktop();
		}
	}

	static { new Basement().register( "basement" ); }
	public static class Basement
		extends Command
	{
		{ usage = " - check Fernswarthy's Basement status."; }
		public void run( String cmd )
		{
			BasementRequest.checkBasement();
		}
	}

	static { new Window().register( "chat" ).register( "mail" ).registerPrefix( "opt" )
		.register( "item" ).register( "gear" ).register( "radio" ); }
	public static class Window
		extends Command
	{
		{ usage = " - switch to tab or open window"; }
		public void run( String command )
		{
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

			if ( command.equals( "gear" ) )
			{
				KoLmafiaGUI.constructFrame( "GearChangeFrame" );
				return;
			}

			if ( command.equals( "radio" ) )
			{
				CLI.launchRadioKoL();
				return;
			}
		}
	}

	static { new HTTP().registerPrefix( "http:" ).registerSubstring( ".php" ); }
	public static class HTTP
		extends Command
	{
		{ usage = " - visit URL without showing results."; }
		public void run( String cmd )
		{
			GenericRequest visitor = new GenericRequest( cmd );
			if ( GenericRequest.shouldIgnore( visitor ) )
			{
				return;
			}

			RequestThread.postRequest( visitor );
			StaticEntity.externalUpdate( visitor.getURLString(), visitor.responseText );
		}
	}

	static { new Text().register( "text" ); }
	public static class Text
		extends Command
	{
		{ usage = " <URL> - show text results from visiting URL."; }
		public void run( String cmd, String parameters )
		{
			GenericRequest visitor = new GenericRequest( parameters );
			if ( GenericRequest.shouldIgnore( visitor ) )
			{
				return;
			}

			RequestThread.postRequest( visitor );
			StaticEntity.externalUpdate( visitor.getURLString(), visitor.responseText );

			CLI.showHTML( visitor.getURLString(), visitor.responseText );
		}
	}

	static { new Relay().register( "relay" ); }
	public static class Relay
		extends Command
	{
		{ usage = " - open the relay browser."; }
		public void run( String cmd )
		{
			StaticEntity.getClient().openRelayBrowser();
		}
	}

	static { new Forum().registerPrefix( "forum" ); }
	public static class Forum
		extends Command
	{
		{ usage = " - visit the official KoL forums."; }
		public void run( String cmd )
		{
			StaticEntity.openSystemBrowser( "http://forums.kingdomofloathing.com/" );
		}
	}

	static { new Wait().register( "wait" ).register( "pause" ) ; }
	public static class Wait
		extends Command
	{
		{ usage = " [<seconds>] - pause script execution (default 1 second)."; }
		public void run( String cmd, String parameters )
		{
			int seconds = parameters.equals( "" ) ? 1 : StringUtilities.parseInt( parameters );
			StaticEntity.executeCountdown( "Countdown: ", seconds );

			KoLmafia.updateDisplay( "Waiting completed." );
		}
	}

	static { new Junk().register( "junk" ).register( "cleanup" ); }
	public static class Junk
		extends Command
	{
		{ usage = " - use, pulverize, or autosell your junk items."; }
		public void run( String cmd )
		{
			CLI.makeJunkRemovalRequest();
		}
	}

	static { new Factory().register( "factory" ); }
	public static class Factory
		extends Command
	{
		{ usage = " report <digits> - Given a string of 7 dwarven digits, report on factory."; }
		public void run( String cmd, String parameters )
		{
			String[] tokens = parameters.split( "\\s+" );
			if ( tokens.length < 1 )
			{
				return;
			}

			String option = tokens[0];

			if ( option.equals( "vacuum" ) )
			{
				String itemString = parameters.substring( 6 ).trim();
				AdventureResult item = ItemFinder.getFirstMatchingItem( itemString, ItemFinder.ANY_MATCH, true );
				if ( item == null )
				{
					return;
				}

				DwarfContraptionRequest request = new DwarfContraptionRequest( "dochamber" );
				request.addFormField( "howmany", String.valueOf( item.getCount() ) );
				request.addFormField( "whichitem", String.valueOf( item.getItemId() ) );
				RequestThread.postRequest( request );

				return;
			}

			if ( option.equals( "check" ) )
			{
				DwarfFactoryRequest.check( false );
				return;
			}

			if ( option.equals( "report" ) )
			{
				if ( tokens.length >= 2 )
				{
					String digits = tokens[1].trim().toUpperCase();
					DwarfFactoryRequest.report( digits );
				}
				else
				{
					DwarfFactoryRequest.report();
				}
				return;
			}

			if ( option.equals( "setdigits" ) )
			{
				String digits = "";
				if ( tokens.length >= 2 )
				{
					digits = tokens[1].trim().toUpperCase();
				}

				if ( digits.length() != 7 )
				{
					KoLmafia.updateDisplay( KoLConstants.ERROR_STATE,
								"Must supply a 7 character digit string" );
					return;
				}
				DwarfFactoryRequest.setDigits( digits );
				return;
			}

			if ( option.equals( "solve" ) )
			{
				DwarfFactoryRequest.solve();
				return;
			}
		}
	}

	static { new ModTrace().register( "modtrace" ); }
	public static class ModTrace
		extends Command
	{
		{ usage = " <filter> - list everything that adds to modifiers matching filter."; }
		public void run( String cmd, String parameters )
		{
			int count = DebugModifiers.setup( parameters.toLowerCase() );
			if ( count == 0 )
			{
				KoLmafia.updateDisplay( KoLConstants.ERROR_STATE,
					"No matching modifiers - use 'modref' to list." );
				return;
			}
			else if ( count > 10 )
			{
				KoLmafia.updateDisplay( KoLConstants.ERROR_STATE,
					"Too many matching modifiers - use 'modref' to list." );
				return;
			}
			KoLCharacter.recalculateAdjustments( true );
		}
	}

	static { new ModRef().register( "modref" ); }
	public static class ModRef
		extends Command
	{
		{ usage = " [<object>] - list all modifiers, show values for player [and object]."; }
		public void run( String cmd, String parameters )
		{
			Modifiers mods = Modifiers.getModifiers( parameters );
			StringBuffer buf = new StringBuffer( "<table border=2>" +
				"<tr><td colspan=2>NUMERIC MODIFIERS</td></tr>" );
			String mod;
			int i = 0;
			while ( (mod = Modifiers.getModifierName( i++ )) != null )
			{	
				buf.append( "<tr><td>" );
				buf.append( mod );
				buf.append( "</td><td>" );
				buf.append( KoLCharacter.currentNumericModifier( mod ) );
				if ( mods != null )
				{
					buf.append( "</td><td>" );
					buf.append( mods.get( mod ) );
				}
				buf.append( "</td></tr>" );
			}
			buf.append( "<tr><td colspan=2>BOOLEAN MODIFIERS</td></tr>" );
			i = 0;
			while ( (mod = Modifiers.getBooleanModifierName( i++ )) != null )
			{	
				buf.append( "<tr><td>" );
				buf.append( mod );
				buf.append( "</td><td>" );
				buf.append( KoLCharacter.currentBooleanModifier( mod ) );
				if ( mods != null )
				{
					buf.append( "</td><td>" );
					buf.append( mods.getBoolean( mod ) );
				}
				buf.append( "</td></tr>" );
			}
			buf.append( "<tr><td colspan=2>STRING MODIFIERS</td></tr>" );
			i = 0;
			while ( (mod = Modifiers.getStringModifierName( i++ )) != null )
			{	
				buf.append( "<tr><td>" );
				buf.append( mod );
				buf.append( "</td><td>N/A" );
				// There are currently no string modifiers at the player level.
				// buf.append( KoLCharacter.currentStringModifier( mod ) );
				if ( mods != null )
				{
					buf.append( "</td><td>" );
					buf.append( mods.getString( mod ) );
				}
				buf.append( "</td></tr>" );
			}
			buf.append( "</table>" );
			RequestLogger.printLine( buf.toString() );
		}
	}

	static { new MoleRef().register( "moleref" ); }
	public static class MoleRef
		extends Command
	{
		{ usage = " - Path of the Mole spoilers."; }
		public void run( String cmd )
		{
			RequestLogger.printLine( "<table border=2>" +
			"<tr><td>9</td><td rowspan=6></td><td rowspan=3></td><td>+30% all stats</td></tr>" +
			"<tr><td>8</td><td rowspan=5>+10 fam weight</td></tr>" +
			"<tr><td>7</td></tr>" +
			"<tr><td>6</td><td>MP</td></tr>" +
			"<tr><td>5</td><td rowspan=6>food</td></tr>" +
			"<tr><td>4</td></tr>" +
			"<tr><td>3</td><td>HP</td><td rowspan=7>+3 stats/fight</td></tr>" +
			"<tr><td>2</td><td rowspan=5>+meat</td></tr>" +
			"<tr><td>1</td></tr>" +
			"<tr><td>0</td></tr>" +
			"<tr><td>-1</td><td rowspan=5>booze</td></tr>" +
			"<tr><td>-2</td></tr>" +
			"<tr><td>-3</td><td>stats</td></tr>" +
			"<tr><td>-4</td><td rowspan=6></td><td rowspan=5>regenerate</td></tr>" +
			"<tr><td>-5</td></tr>" +
			"<tr><td>-6</td><td>-3MP/skill</td></tr>" +
			"<tr><td>-7</td><td rowspan=3></td></tr>" +
			"<tr><td>-8</td></tr>" +
			"<tr><td>-9</td><td>+30 ML</td></tr>" +
			"</table>" );
		}
	}

	static { new Olfact().registerPrefix( "olfact" ).register( "putty" ); }
	public static class Olfact
		extends Command
	{
		{ usage = " ( none | monster <name> | [item] <list> | goals ) [abort] - tag next monster [that drops all items in list, or your goals]."; }
		public void run( String cmd, String parameters )
		{
			String pref = cmd.equals( "putty" ) ? "autoPutty" : "autoOlfact";
			parameters = parameters.toLowerCase();
			if ( parameters.equals( "none" ) )
			{
				Preferences.setString( pref, "" );
			}
			else if ( !parameters.equals( "" ) )
			{
				boolean isAbort = false, isItem = false, isMonster = false;
				boolean isGoals = false;
				if ( parameters.endsWith( " abort" ) )
				{
					isAbort = true;
					parameters = parameters.substring( 0, parameters.length() - 6 ).trim();
				}
				if ( parameters.startsWith( "item " ) )
				{
					parameters = parameters.substring( 5 ).trim();
				}
				else if ( parameters.startsWith( "monster " ) )
				{
					isMonster = true;
					parameters = parameters.substring( 8 ).trim();
				}
				else if ( parameters.equals( "goals" ) )
				{
					isGoals = true;
				}
				StringBuffer result = new StringBuffer();
				if ( isGoals )
				{
					result.append( "goals" );
				}
				if ( !isGoals && !isMonster )
				{
					Object[] items = ItemFinder.getMatchingItemList(
						KoLConstants.inventory, parameters );
					if ( items != null && items.length > 0 )
					{
						result.append( "item " );
						for ( int i = 0; i < items.length; ++i )
						{
							if ( i != 0 ) result.append( ", " );
							result.append( ((AdventureResult) items[ i ]).getName() );
						}
						isItem = true;
					}
				}
				if ( !isGoals && !isItem && parameters.length() >= 1 )
				{
					result.append( "monster " );
					result.append( parameters );
					isMonster = true;
				}
				if ( !isGoals && !isItem && !isMonster )
				{
					KoLmafia.updateDisplay( KoLConstants.ERROR_STATE,
						"Unable to interpret your conditions!" );
					return;
				}
			
				if ( isAbort ) result.append( " abort" );
				Preferences.setString( pref, result.toString() );
			}
			String option = Preferences.getString( pref );
			if ( option.equals( "" ) )
			{
				KoLmafia.updateDisplay( pref + " is disabled." );
			}
			else
			{
				KoLmafia.updateDisplay( pref + ": " + option.replaceFirst(
					"^goals", "first monster that can drop your remaining goals" )
					.replaceFirst( " abort$", ", and then abort adventuring" ) );
			}
		}
	}

	static { new Conditions().registerPrefix( "goal" ).registerPrefix( "condition" )
		.registerPrefix( "objective" ); }
	public static class Conditions
		extends Command
	{
		{ usage = " clear | check | add <condition> | set <condition> - modify your adventuring goals."; }
		public void run( String cmd, String parameters )
		{
			CLI.executeConditionsCommand( parameters );
		}
	}

	static { new Update().register( "update" ); }
	public static class Update
		extends Command
	{
		{ usage = " data | clear - download most recent data files, or revert to built-in data."; }
		public void run( String cmd, String parameters )
		{
			if ( parameters.equalsIgnoreCase( "clear" ) )
			{
				CLI.deleteAdventureOverride();
			}
			else if ( parameters.equalsIgnoreCase( "data" ) )
			{
				CLI.downloadAdventureOverride();
			}
			else
			{
				KoLmafia.updateDisplay( KoLConstants.ABORT_STATE, 
					"\"update\" doesn't do what you think it does.  Please visit kolmafia.us and download a daily build if you'd like to keep KoLmafia up-to-date between major releases." );
			}
		}
	}

	static { new FakeItem().register( "fakeitem" ); }
	public static class FakeItem
		extends Command
	{
		{ usage = null; }
		public void run( String cmd, String parameters )
		{
			AdventureResult item = ItemFinder.getFirstMatchingItem( parameters, ItemFinder.ANY_MATCH );
			if ( item != null )
			{
				ResultProcessor.processResult( item );
			}
		}
	}

	static { new RemoveItem().register( "removeitem" ); }
	public static class RemoveItem
		extends Command
	{
		{ usage = null; }
		public void run( String cmd, String parameters )
		{
			AdventureResult item = ItemFinder.getFirstMatchingItem( parameters, ItemFinder.ANY_MATCH );
			if ( item != null )
			{
				ResultProcessor.processResult( item.getNegation() );
			}
		}
	}

	static { new Developer().register( "newdata" )
		.register( "checkitems" ).register( "checkeffects" )
		.register( "checkplurals" ).register( "checkmodifiers" )
		.register( "checkconsumption" ).register( "checkprofile" )
		.register( "checkpulverization" ); }
	public static class Developer
		extends Command
	{
		{ usage = null; }
		public void run( String command, String parameters )
		{
			if ( command.equals( "newdata" ) )
			{
				DebugDatabase.findItemDescriptions();
				EffectDatabase.findStatusEffects();
				RequestLogger.printLine( "Data tables updated." );
				return;
			}

			if ( command.equals( "checkitems" ) )
			{
				int itemId = StringUtilities.parseInt( parameters );
				DebugDatabase.checkItems( itemId );
				RequestLogger.printLine( "Internal item data checked." );
				return;
			}

			if ( command.equals( "checkeffects" ) )
			{
				DebugDatabase.checkEffects();
				RequestLogger.printLine( "Internal status effect data checked." );
				return;
			}

			if ( command.equals( "checkplurals" ) )
			{
				int itemId = StringUtilities.parseInt( parameters );
				DebugDatabase.checkPlurals( itemId );
				RequestLogger.printLine( "Plurals checked." );
				return;
			}

			if ( command.equals( "checkmodifiers" ) )
			{
				Modifiers.checkModifiers();
				RequestLogger.printLine( "Modifiers checked." );
				return;
			}

			if ( command.equals( "checkconsumption" ) )
			{
				DebugDatabase.checkConsumptionData();
				RequestLogger.printLine( "Consumption data checked." );
				return;
			}

			if ( command.equals( "checkpulverization" ) )
			{
				DebugDatabase.checkPulverizationData();
				RequestLogger.printLine( "Pulverization data checked." );
				return;
			}

			if ( command.equals( "checkprofile" ) )
			{
				String playerId = KoLmafia.getPlayerId( parameters );
				if ( playerId.equals( parameters ) )
				{
                                        String text = KoLmafia.whoisPlayer( playerId );
					Matcher idMatcher =
						Pattern.compile( "\\(#(\\d+)\\)" ).matcher( text );

					if ( idMatcher.find() )
					{
						KoLmafia.registerPlayer( parameters, idMatcher.group( 1 ) );
					}
					else
					{
						RequestLogger.printLine( "no such player" );
						return;
					}
				}
				ProfileRequest prof = new ProfileRequest( parameters );
				prof.run();
				RequestLogger.printLine( "name [" + prof.getPlayerName() + "]" );
				RequestLogger.printLine( "id [" + prof.getPlayerId() + "]" );
				RequestLogger.printLine( "level [" + prof.getPlayerLevel() + "]" );
				RequestLogger.printLine( "class [" + prof.getClassType() + "]" );
				RequestLogger.printLine( "clan [" + prof.getClanName() + "]" );
				RequestLogger.printLine( "restrict [" + prof.getRestriction() + "]" );
				return;
			}
		}
	}

	static { new Clear().register( "clear" ).register( "cls" ); }
	public static class Clear
		extends Command
	{
		{ usage = " - clear CLI window."; }
		public void run( String cmd )
		{
			KoLConstants.commandBuffer.clearBuffer();
		}
	}

	static { new Abort().register( "abort" ); }
	public static class Abort
		extends Command
	{
		{ usage = " [message] - stop current script or automated task."; }
		public void run( String cmd, String parameters )
		{
			KoLmafia.updateDisplay( KoLConstants.ABORT_STATE, parameters.length() == 0 ? "Script abort." : parameters );
		}
	}

	static { new Events().register( "events" ); }
	public static class Events
		extends Command
	{
		{ usage = " [clear] - clear or show recent events."; }
		public void run( String cmd, String parameters )
		{
			if ( parameters.equals( "clear" ) )
			{
				EventManager.clearEventHistory();
			}
			else
			{
				RequestLogger.printList( EventManager.getEventHistory() );
			}
		}
	}

	static { new ColorEcho().register( "colorecho" ).register( "cecho" ); }
	public static class ColorEcho
		extends Command
	{
		{ usage = " <color> <text> - show text using color (specified by name or #RRGGBB)."; }
		public void run( String cmd, String parameters )
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
		}
	}

	static { new LogEcho().register( "logecho" ).register( "logprint" ); }
	public static class LogEcho
		extends Command
	{
		{ usage = " timestamp | <text> - include timestamp or text in the session log only."; }
		public void run( String cmd, String parameters )
		{
			if ( parameters.equalsIgnoreCase( "timestamp" ) )
			{
				parameters = HolidayDatabase.getCalendarDayAsString( new Date() );
			}

			parameters = StringUtilities.globalStringDelete( StringUtilities.globalStringDelete( parameters, "\n" ), "\r" );
			parameters = StringUtilities.globalStringReplace( parameters, "<", "&lt;" );

			RequestLogger.getSessionStream().println( " > " + parameters );
		}
	}

	static { new Echo().register( "echo" ).register( "print" ); }
	public static class Echo
		extends Command
	{
		{ usage = " timestamp | <text> - include timestamp or text in the session log."; }
		public void run( String cmd, String parameters )
		{
			if ( parameters.equalsIgnoreCase( "timestamp" ) )
			{
				parameters = HolidayDatabase.getCalendarDayAsString( new Date() );
			}

			parameters = StringUtilities.globalStringDelete( StringUtilities.globalStringDelete( parameters, "\n" ), "\r" );
			parameters = StringUtilities.globalStringReplace( parameters, "<", "&lt;" );

			RequestLogger.printLine( parameters );
			RequestLogger.getSessionStream().println( " > " + parameters );
		}

	}

	static { new Autoattack().register( "aa" ).register( "autoattack" ); }
	public static class Autoattack
		extends Command
	{
		{ usage = " <skill> - set default attack method."; }
		public void run( String cmd, String parameters )
		{
			CustomCombatManager.setAutoAttack( parameters );
		}

	}

	static { new Get().register( "get" ).register( "set" ); }
	public static class Get
		extends Command
	{
		{ usage = " <preference> [ = <value> ] - show/change preference settings"; }
		{ flags = KoLmafiaCLI.FULL_LINE_CMD; }
		public void run( String cmd, String parameters )
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
				while ( name.endsWith( ".ccs" ) )
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
		}
	}

	static { new Login().register( "login" ); }
	public static class Login
		extends Command
	{
		{ usage = " <username> - logout then log back in as username."; }
		public void run( String cmd, String parameters )
		{
			KoLmafia.logout();
			CLI.attemptLogin( parameters );
		}
	}

	static { new Logout().register( "logout" ); }
	public static class Logout
		extends Command
	{
		{ usage = " - logout and return to login window."; }
		public void run( String cmd )
		{
			KoLmafia.logout();
		}
	}

	static { new Exit().register( "exit" ).register( "quit" ); }
	public static class Exit
		extends Command
	{
		{ usage = " - logout and exit KoLmafia."; }
		public void run( String cmd )
		{
			KoLmafia.quit();
		}
	}

	static { new Namespace().register( "namespace" ); }
	public static class Namespace
		extends Command
	{
		{ usage = " [<filter>] - list namespace scripts and the functions they define."; }
		public void run( String cmd, String parameters )
		{
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
		}
	}

	static { new ASHref().register( "ashref" ); }
	public static class ASHref
		extends Command
	{
		{ usage = " [<filter>] - summarize ASH built-in functions [matching filter]."; }
		public void run( String cmd, String parameters )
		{
			KoLmafiaASH.showExistingFunctions( parameters );
		}
	}

	static { new Using().register( "using" ); }
	public static class Using
		extends Command
	{
		{ usage = " <filename> - add ASH script to namespace."; }
		public void run( String cmd, String parameters )
		{
			// Validate the script first.

			CLI.executeCommand( "validate", parameters );
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
		}
	}

	static { new Call().register( "verify" ).register( "validate" ).register( "check" )
		.register( "call" ).register( "run" ).registerPrefix( "exec" )
		.register( "load" ).register( "start" ); }
	public static class Call
		extends Command
	{
		{ usage = " [<number>x] <filename> | <function> [<parameters>] - check/run script."; }
		public void run( String command, String parameters )
		{
			CLI.executeScriptCommand( command, parameters );
		}
	}

	static { new Repeat().register( "repeat" ); }
	public static class Repeat
		extends Command
	{
		{ usage = " [<number>] - repeat previous line [number times]."; }
		public void run( String cmd, String parameters )
		{
			KoLmafiaCLI CLI = this.CLI;
			String previousLine = CLI.previousLine;
			if ( previousLine != null )
			{
				int repeatCount = parameters.length() == 0 ? 1 : StringUtilities.parseInt( parameters );
				for ( int i = 0; i < repeatCount && KoLmafia.permitsContinue(); ++i )
				{
					RequestLogger.printLine( "Repetition " + ( i + 1 ) + " of " + repeatCount + "..." );
					CLI.executeLine( previousLine );
				}
			}
		}
	}

	static { new Debug().register( "debug" ); }
	public static class Debug
		extends Command
	{
		{ usage = " [on] | off - start or stop logging of debugging data."; }
		public void run( String cmd, String parameters )
		{
			if ( parameters.equals( "off" ) )
			{
				RequestLogger.closeDebugLog();
			}
			else
			{
				RequestLogger.openDebugLog();
			}
		}
	}

	static { new Mirror().registerSubstring( "mirror" ); }
	public static class Mirror
		extends Command
	{
		{ usage = " [<filename>] - stop [or start] logging to an additional file."; }
		public void run( String command, String parameters )
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
		}
	}

	static { new Wiki().register( "wiki" ); }
	public static class Wiki
		extends Command
	{
		{ usage = " <searchText> - perform search on KoL Wiki."; }
		public void run( String cmd, String parameters )
		{
			StaticEntity.openSystemBrowser( "http://kol.coldfront.net/thekolwiki/index.php/Special:Search?search=" +
				StringUtilities.getURLEncode( parameters ) + "&go=Go" );
		}
	}

	static { new Lookup().register( "lookup" ); }
	public static class Lookup
		extends Command
	{
		{ usage = " <item> | <effect> - go to appropriate KoL Wiki page."; }
		public void run( String command, String parameters )
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
		}
	}

	static { new Safe().register( "safe" ); }
	public static class Safe
		extends Command
	{
		{ usage = " <location> - show summary data for the specified area."; }
		public void run( String cmd, String parameters )
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

			CLI.showHTML( "", buffer.toString() );
		}
	}

	static { new Monsters().register( "monsters" ); }
	public static class Monsters
		extends Command
	{
		{ usage = " <location> - show combat details for the specified area."; }
		public void run( String cmd, String parameters )
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

			CLI.showHTML( "", buffer.toString() );
		}
	}

	static { new Breakfast().register( "breakfast" ); }
	public static class Breakfast
		extends Command
	{
		{ usage = " - perform start-of-day activities."; }
		public void run( String cmd )
		{
			BreakfastManager.getBreakfast( true );
		}
	}

	static { new Refresh().register( "refresh" ); }
	public static class Refresh
		extends Command
	{
		{ usage = " all | status | equip | inv | storage | familiar | stickers - resynchronize with KoL."; }
		public void run( String cmd, String parameters )
		{
			if ( parameters.equals( "all" ) )
			{
				StaticEntity.getClient().refreshSession();
				return;
			}
			else if ( parameters.equals( "status" ) || parameters.equals( "effects" ) )
			{
				RequestThread.postRequest( CharPaneRequest.getInstance() );
			}
			else if ( parameters.equals( "gear" ) || parameters.startsWith( "equip" ) || parameters.equals( "outfit" ) )
			{
				parameters = "equip";
				RequestThread.postRequest( new EquipmentRequest( EquipmentRequest.EQUIPMENT ) );
			}
			else if ( parameters.startsWith( "stick" ) )
			{
				RequestThread.postRequest( new EquipmentRequest( EquipmentRequest.BEDAZZLEMENTS ) );
			}
			else if ( parameters.startsWith( "inv" ) )
			{
				RequestThread.postRequest( new EquipmentRequest( EquipmentRequest.CLOSET ) );
				return;
			}
			else if ( parameters.startsWith( "camp" ) )
			{
				RequestThread.postRequest( new CampgroundRequest() );
				return;
			}
			else if ( parameters.equals( "storage" ) )
			{
				RequestThread.postRequest( new StorageRequest() );
				return;
			}
			else if ( parameters.startsWith( "familiar" ) || parameters.equals( "terrarium" ) )
			{
				parameters = "familiars";
				RequestThread.postRequest( new FamiliarRequest() );
			}

			CLI.showData( parameters );
		}
	}

	static { new Entryway().register( "entryway" ); }
	public static class Entryway
		extends Command
	{
		{ usage = " [clover] - automatically complete quest [using a clover]."; }
		public void run( String cmd, String parameters )
		{
			if ( parameters.equalsIgnoreCase( "clover" ) )
			{
				SorceressLairManager.completeCloveredEntryway();
			}
			else
			{
				SorceressLairManager.completeCloverlessEntryway();
			}
		}
	}

	static { new Quest().register( "maze" ).registerPrefix( "hedge" )
		.register( "tower" ).register( "guardians" ).register( "chamber" )
		.register( "nemesis" )
		.register( "guild" ).register( "gourd" ).register( "tavern" ); }
	public static class Quest
		extends Command
	{
		{ usage = " - automatically complete quest."; }
		public void run( String command )
		{
			if ( command.equals( "maze" ) || command.startsWith( "hedge" ) )
			{
				SorceressLairManager.completeHedgeMaze();
				return;
			}

			if ( command.equals( "tower" ) || command.equals( "guardians" ) || command.equals( "chamber" ) )
			{
				SorceressLairManager.fightAllTowerGuardians();
				return;
			}

			if ( command.equals( "nemesis" ) )
			{
				NemesisManager.faceNemesis();
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
			KoLmafia.updateDisplay( "What... is your quest?  [internal error]" );
		}
	}

	static { new Leaflet().register( "leaflet" ); }
	public static class Leaflet
		extends Command
	{
		{ usage = "  [nomagic] | location | command  - complete leaflet quest [without using magic words]."; }
		public void run( String cmd, String parameters )
		{
			if ( parameters.equals( "" ) || parameters.equals( "stats" ) )
			{
				LeafletManager.robStrangeLeaflet( true );
				return;
			}
			if ( parameters.equals( "nomagic" ) )
			{
				LeafletManager.robStrangeLeaflet( false );
				return;
			}
			if ( parameters.equals( "location" ) )
			{
				String location = LeafletManager.locationName();
				KoLmafia.updateDisplay( "Current leaflet location: " + location );
				return;
			}
		}
	}

	static { new Train().register( "train" ); }
	public static class Train
		extends Command
	{
		{ usage = " base <weight> | buffed <weight> | turns <number> - train familiar."; }
		public void run( String cmd, String parameters )
		{
			CLI.trainFamiliar( parameters );
		}
	}

	static { new Council().register( "council" ); }
	public static class Council
		extends Command
	{
		{ usage = " - visit the Council to advance quest progress."; }
		public void run( String cmd )
		{
			RequestThread.postRequest( CouncilFrame.COUNCIL_VISIT );

			CLI.showHTML( "council.php", StringUtilities.singleStringReplace(
				CouncilFrame.COUNCIL_VISIT.responseText, "<a href=\"town.php\">Back to Seaside Town</a>", "" ) );
		}
	}

	static { new Cast().register( "cast" ).register( "skill" ); }
	public static class Cast
		extends Command
	{
		{ usage = "[?] [ [<count>] <skill> [on <player>] ] - list spells, or use one."; }
		public void run( String command, String parameters )
		{
			if ( parameters.length() > 0 )
			{
				SpecialOutfit.createImplicitCheckpoint();
				CLI.executeCastBuffRequest( parameters );
				SpecialOutfit.restoreImplicitCheckpoint();
				return;
			}
			CLI.showData( "skills" + (command.equals( "cast" ) ? " cast" : "") );
		}
	}

	static { new Up().register( "up" ); }
	public static class Up
		extends Command
	{
		{ usage = " <effect> [, <effect>]... - extend duration of effects."; }
		public void run( String cmd, String parameters )
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
		}
	}

	static { new Shrug().register( "shrug" ).register( "uneffect" ).register( "remedy" ); }
	public static class Shrug
		extends Command
	{
		{ usage = "[?] <effect> [, <effect>]... - remove effects using appropriate means."; }
		public void run( String cmd, String parameters )
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

			CLI.executeUneffectRequest( parameters );
		}
	}

	static { new Acquire().register( "find" ).register( "acquire" ).register( "retrieve" ); }
	public static class Acquire
		extends Command
	{
		{ usage = " <item> - ensure that you have item, creating or buying it if needed."; }
		public void run( String cmd, String parameters )
		{
			AdventureResult item = ItemFinder.getFirstMatchingItem( parameters, ItemFinder.ANY_MATCH );
			if ( item != null )
			{
				SpecialOutfit.createImplicitCheckpoint();
				InventoryManager.retrieveItem( item, false );
				SpecialOutfit.restoreImplicitCheckpoint();
			}
		}
	}

	static { new Fold().register( "fold" ).register( "squeeze" ); }
	public static class Fold
		extends Command
	{
		{ usage = "[?] <item> - produce item by using another form, repeated as needed."; }
		public void run( String cmd, String parameters )
		{
			AdventureResult item = ItemFinder.getFirstMatchingItem( parameters, ItemFinder.ANY_MATCH );
			if ( item == null )
			{
				return;
			}
			ArrayList group = ItemDatabase.getFoldGroup( item.getName() );
			if ( group == null )
			{
				KoLmafia.updateDisplay( KoLConstants.ERROR_STATE,
					"That's not a transformable item!" );
				return;
			}
			int damage = ((Integer) group.get( 0 )).intValue();
			damage = damage == 0 ? 0 : KoLCharacter.getMaximumHP() * damage / 100 + 2;
			int tries = 0;
			SpecialOutfit.createImplicitCheckpoint();
		try1:
			while ( ++tries <= 20 && KoLmafia.permitsContinue() &&
				!InventoryManager.hasItem( item ) )
			{
				for ( int i = 1; i < group.size(); ++i )
				{
					AdventureResult otherForm = new AdventureResult(
						(String) group.get( i ), 1 );
					if ( InventoryManager.hasItem( otherForm ) )
					{
						if ( KoLmafiaCLI.isExecutingCheckOnlyCommand )
						{
							RequestLogger.printLine( otherForm + " => " + item );
							break try1;
						}
						int hp = KoLCharacter.getCurrentHP();
						if ( hp > 0 && hp < damage )
						{
							StaticEntity.getClient().recoverHP( damage );
						}
						RequestThread.postRequest( new UseItemRequest( otherForm ) );
						continue try1;
					}
				}
				KoLmafia.updateDisplay( KoLConstants.ERROR_STATE,
					"You don't have anything transformable into that item!" );
				break;
			}
			SpecialOutfit.restoreImplicitCheckpoint();
		}
	}

	static { new Clan().register( "clan" ); }
	public static class Clan
		extends Command
	{
		{ usage = " [ snapshot | stashlog ] - clan management."; }
		public void run( String cmd, String parameters )
		{
			if ( parameters.equals( "" ) )
			{
				KoLmafiaGUI.constructFrame( "ClanManageFrame" );
				return;
			}
			else if ( parameters.equals( "snapshot" ) )
			{
				ClanManager.takeSnapshot( 20, 10, 5, 0, false, true );
			}
			else if ( parameters.equals( "stashlog" ) )
			{
				ClanManager.saveStashLog();
			}
		}
	}

	static { new Bang().register( "!" ).register( "bang" ); }
	public static class Bang
		extends Command
	{
		{ usage = " - list the Dungeons of Doom potions you've identified."; }
		public void run( String cmd )
		{
			for ( int i = 819; i <= 827; ++i )
			{
				String potion = ItemDatabase.getItemName( i );
				potion = potion.substring( 0, potion.length() - 7 );
				RequestLogger.printLine( potion + ": " + Preferences.getString( "lastBangPotion" + i ) );
			}
		}
	}

	static { new Insults().register( "insults" ); }
	public static class Insults
		extends Command
	{
		{ usage = " - list the pirate insult comebacks you know."; }
		public void run( String cmd )
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
		}
	}

	static { new Dusty().register( "dusty" ); }
	public static class Dusty
		extends Command
	{
		{ usage = " - list the dusty bottles of wine you've identified."; }
		public void run( String cmd )
		{
			ItemDatabase.getDustyBottles();
			for ( int i = 2271; i <= 2276; ++i )
			{
				String bottle = ItemDatabase.getItemName( i );
				String type = ItemDatabase.dustyBottleType( i );
				RequestLogger.printLine( bottle + ": " + type );
			}
		}
	}

	static { new Entity().register( "entity" ); }
	public static class Entity
		extends Command
	{
		{ usage = " - give details of your current pastamancer combat entity."; }
		public void run( String cmd )
		{
			if ( KoLCharacter.getClassType() != KoLCharacter.PASTAMANCER )
			{
				KoLmafia.updateDisplay(
					KoLConstants.ERROR_STATE,
					"Only Pastamancers can summon a combat entity" );
				return;
			}

			KoLCharacter.ensureUpdatedPastamancerGhost();
			String name = Preferences.getString( "pastamancerGhostName" );
			if ( name.equals( "" ) )
			{
				RequestLogger.printLine( "You have not summoned a combat entity yet." );
				return;
			}

			String type = Preferences.getString( "pastamancerGhostType" );
			int experience = Preferences.getInteger( "pastamancerGhostExperience" );
			int summons = Preferences.getInteger( "pastamancerGhostSummons" );
			RequestLogger.printLine( "You've summoned " + name + " the " + type + " (" + experience + " exp) " + summons + " time" + ( summons != 1 ? "s" : "" ) + " today." );
		}
	}

	static { new Demons().register( "demons" ); }
	public static class Demons
		extends Command
	{
		{ usage = " - list the demon names you know."; }
		public void run( String cmd )
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
		}
	}

	static { new Summon().register( "summon" ); }
	public static class Summon
		extends Command
	{
		{ usage = " <demonName> | <effect> | <location> | <number> - use the Summoning Chamber."; }
		public void run( String cmd, String parameters )
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

			WineCellarRequest demonSummon = new WineCellarRequest( demon );

			KoLmafia.updateDisplay( "Summoning " + demon + "..." );
			RequestThread.postRequest( demonSummon );
			RequestThread.enableDisplayIfSequenceComplete();
		}
	}

	static { new Use().register( "eat" ).register( "drink" ).register( "use" )
		.register( "chew" ).register( "hobo" ).register( "ghost" ).register( "overdrink" ); }
	public static class Use
		extends Command
	{
		{ usage = "[?] [either] <item> [, <item>]... - use/consume items"; }
		public void run( String command, String parameters )
		{
			if ( command.equals( "overdrink" ) )
			{
				UseItemRequest.permitOverdrink();
				command = "drink";
			}
			SpecialOutfit.createImplicitCheckpoint();
			CLI.executeUseItemRequest( command, parameters );
			SpecialOutfit.restoreImplicitCheckpoint();
		}
	}

	static { new Zap().register( "zap" ); }
	public static class Zap
		extends Command
	{
		{ usage = " <item> [, <item>]... - transform items with your wand."; }
		public void run( String cmd, String parameters )
		{
			CLI.currentLine = cmd + " " + parameters;	// this is weird!
			CLI.makeZapRequest();
		}
	}

	static { new Smash().register( "smash" ).register( "pulverize" ); }
	public static class Smash
		extends Command
	{
		{ usage = " <item> [, <item>]... - pulverize specified items"; }
		public void run( String cmd, String parameters )
		{
			CLI.makePulverizeRequest( parameters );
		}
	}

	static { new Create().register( "create" ).register( "make" ).register( "bake" )
		.register( "mix" ).register( "smith" ).register( "tinker" ).register( "ply" ); }
	public static class Create
		extends Command
	{
		{ usage = " [ <item>... ] - list creatables, or create specified items."; }
		public void run( String cmd, String parameters )
		{
			CLI.executeItemCreationRequest( parameters );
		}
	}

	static { new Untinker().register( "untinker" ); }
	public static class Untinker
		extends Command
	{
		{ usage = " [ <item>... ] - complete quest, or untinker items."; }
		public void run( String cmd, String parameters )
		{
			CLI.executeUntinkerRequest( parameters );
		}
	}

	static { new Automall().register( "automall" ); }
	public static class Automall
		extends Command
	{
		{ usage = " - dump all profitable, non-memento items into the Mall."; }
		public void run( String cmd )
		{
			CLI.makeAutoMallRequest();
		}
	}

	static { new Mallsell().register( "mallsell" ); }
	public static class Mallsell
		extends Command
	{
		{ usage = " <item> [[@] <price> [[limit] <num>]] [, <another>]... - sell in Mall."; }
		public void run( String cmd, String parameters )
		{
			CLI.executeMallSellRequest( parameters );
		}
	}

	static { new Stash().register( "stash" ); }
	public static class Stash
		extends Command
	{
		{ usage = " [put] <item>... | take <item>... - exchange items with clan stash"; }
		public void run( String cmd, String parameters )
		{
			CLI.executeStashRequest( parameters );
		}
	}

	static { new Pull().register( "hagnk" ).register( "pull" ); }
	public static class Pull
		extends Command
	{
		{ usage = " outfit <name> | <item> [, <item>]... - pull items from Hagnk's storage."; }
		public void run( String cmd, String parameters )
		{
			CLI.executeHagnkRequest( parameters );
		}
	}

	static { new Sell().register( "sell" ).register( "autosell" ); }
	public static class Sell
		extends Command
	{
		{ usage = " <item> [, <item>]... - autosell items."; }
		public void run( String cmd, String parameters )
		{
			CLI.executeSellStuffRequest( parameters );
		}
	}

	static { new Reprice().register( "reprice" ).register( "undercut" ); }
	public static class Reprice
		extends Command
	{
		{ usage = " - price all max-priced items at or below current Mall minimum price."; }
		public void run( String cmd )
		{
			StaticEntity.getClient().priceItemsAtLowestPrice( true );
		}
	}

	static { new MCD().register( "mind-control" ).register( "mcd" ); }
	public static class MCD
		extends Command
	{
		{ usage = " <number> - set mind control device (or equivalent) to new value."; }
		public void run( String cmd, String parameters )
		{
			CLI.executeMindControlRequest( parameters );
		}
	}

	static { new Mushroom().register( "field" ); }
	public static class Mushroom
		extends Command
	{
		{ usage = " [ plant <square> <type> | pick <square> | harvest ] - view or use your mushroom plot"; }
		public void run( String cmd, String parameters )
		{
			CLI.executeMushroomCommand( parameters );
		}
	}

	static { new Telescope().register( "telescope" ); }
	public static class Telescope
		extends Command
	{
		{ usage = " [look] high | low - get daily buff, or Lair hints from your telescope."; }
		public void run( String cmd, String parameters )
		{
			CLI.executeTelescopeRequest( parameters );
		}
	}

	static { new Adv().registerPrefix( "adv" ); }
	public static class Adv
		extends Command
	{
		{ usage = "[?] last | [<count>] <location> - spend your turns."; }
		public void run( String cmd, String parameters )
		{
			CLI.executeAdventureRequest( parameters );
		}
	}

	static { new Donate().register( "donate" ); }
	public static class Donate
		extends Command
	{
		{ usage = " boris | mus | jarl | mys | pete | mox <amount> - donate in Hall of Legends."; }
		public void run( String cmd, String parameters )
		{
			CLI.executeDonateCommand( parameters );
		}
	}

	static { new Stickers().registerPrefix( "sticker" ); }
	public static class Stickers
		extends Command
	{
		{ usage = " <sticker1> [, <sticker2> [, <sticker3>]] - replace worn stickers."; }
		public void run( String cmd, String parameters )
		{
			String[] stickers = parameters.split( "\\s*,\\s*" );
			for ( int i = 0; i < 3; ++i )
			{
				if ( EquipmentManager.getEquipment( EquipmentManager.STICKER1 + i )
					== EquipmentRequest.UNEQUIP && i < stickers.length )
				{
					String item = stickers[ i ].toLowerCase();
					if ( item.indexOf( "stick" ) == -1 )
					{
						item = item + " sticker";
					}
					CLI.executeEquipCommand( "st" + (i + 1) + " " + item );
				}
			}
		}
	}

	static {
		new Equip().register( "equip" ).register( "wear" ).register( "wield" );
		new AliasCmd( "equip", "off-hand" ).register( "second" ).register( "hold" )
			.register( "dualwield" );
	}
	public static class Equip
		extends Command
	{
		{ usage = " [list <filter>] | [<slot>] <item> - show equipment, or equip item [in slot]."; }
		public void run( String cmd, String parameters )
		{
			CLI.executeEquipCommand( parameters );
		}
	}

	static { new Unequip().register( "unequip" ).register( "remove" ); }
	public static class Unequip
		extends Command
	{
		{ usage = " <slot> | <name> - remove equipment in slot, or that matches name"; }
		public void run( String cmd, String parameters )
		{
			CLI.executeUnequipCommand( parameters );
		}
	}

	static { new Familiar().register( "familiar" ); }
	public static class Familiar
		extends Command
	{
		{ usage = "[?] [list <filter>] | lock | unlock | <species> | none - list or change familiar types"; }
		public void run( String cmd, String parameters )
		{
			if ( parameters.startsWith( "list" ) )
			{
				CLI.showData( "familiars " + parameters.substring( 4 ).trim() );
				return;
			}
			else if ( parameters.length() == 0 )
			{
				CLI.showData( "familiars" );
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
			else if ( parameters.equalsIgnoreCase( "lock" ) )
			{
				if ( EquipmentManager.familiarItemLocked() )
				{
					KoLmafia.updateDisplay( "Familiar item already locked." );
					return;
				}
				if ( !EquipmentManager.familiarItemLockable() )
				{
					KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "Familiar item can't be locked." );
					return;
				}
				RequestThread.postRequest( new FamiliarRequest( true ) );
				return;
			}
			else if ( parameters.equalsIgnoreCase( "unlock" ) )
			{
				if ( !EquipmentManager.familiarItemLocked() )
				{
					KoLmafia.updateDisplay( "Familiar item already unlocked." );
					return;
				}
				RequestThread.postRequest( new FamiliarRequest( true ) );
				return;
			}
			else if ( parameters.indexOf( "(no change)" ) != -1 )
			{
				return;
			}

			boolean unequip = false;
			if ( parameters.startsWith( "naked " ) )
			{
				unequip = true;
				parameters = parameters.substring( 6 );
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
					RequestThread.postRequest( new FamiliarRequest( change, unequip ) );
				}
			}
			else
			{
				KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "You don't have a " + parameters + " for a familiar." );
			}
		}
	}

	static { new Closet().register( "closet" ); }
	public static class Closet
		extends Command
	{
		{ usage = " list <filter> | put <item>... | take <item>... - list or manipulate your closet."; }
		public void run( String cmd, String parameters )
		{
			if ( parameters.startsWith( "list" ) )
			{
				CLI.showData( "closet " + parameters.substring( 4 ).trim() );
				return;
			}
			else if ( parameters.length() == 0 )
			{
				CLI.showData( "closet" );
				return;
			}

			CLI.executeClosetManageRequest( parameters );
		}
	}

	static { new Display().register( "display" ); }
	public static class Display
		extends Command
	{
		{ usage = " [<filter>] | put <item>... | take <item>... - list or manipulate your display case."; }
		public void run( String cmd, String parameters )
		{
			CLI.executeDisplayCaseRequest( parameters );
		}
	}

	static { new Checkpoint().register( "checkpoint" ); }
	public static class Checkpoint
		extends Command
	{
		{ usage = " - remembers current equipment, use \"outfit checkpoint\" to restore."; }
		public void run( String cmd )
		{
			SpecialOutfit.createExplicitCheckpoint();
			KoLmafia.updateDisplay( "Internal checkpoint created." );
			RequestThread.enableDisplayIfSequenceComplete();
		}
	}

	static { new Outfit().register( "outfit" ); }
	public static class Outfit
		extends Command
	{
		{ usage = " [list <filter>] | checkpoint | <name> - list, restore, or change outfits."; }
		public void run( String cmd, String parameters )
		{
			if ( parameters.startsWith( "list" ) )
			{
				CLI.showData( "outfits " + parameters.substring( 4 ).trim() );
				return;
			}
			else if ( parameters.length() == 0 )
			{
				CLI.showData( "outfits" );
				return;
			}
			else if ( parameters.equalsIgnoreCase( "checkpoint" ) )
			{
				SpecialOutfit.restoreExplicitCheckpoint();
				return;
			}

			CLI.executeChangeOutfitCommand( parameters );
		}
	}

	static { new Buy().register( "buy" ).register( "mallbuy"); }
	public static class Buy
		extends Command
	{
		{ usage = " <item> [@ <limit>] [, <another>]... - buy from NPC store or the Mall."; }
		public void run( String cmd, String parameters )
		{
			SpecialOutfit.createImplicitCheckpoint();
			CLI.executeBuyCommand( parameters );
			SpecialOutfit.restoreImplicitCheckpoint();
		}
	}

	static { new Buffbot().register( "buffbot" ); }
	public static class Buffbot
		extends Command
	{
		{ usage = " <number> - run buffbot for number iterations."; }
		public void run( String cmd, String parameters )
		{
			CLI.executeBuffBotCommand( parameters );
		}
	}

	static { new SearchMall().register( "searchmall" ); }
	public static class SearchMall
		extends Command
	{
		{ usage = " <item> [ with limit <number> ] - search the Mall."; }
		public void run( String cmd, String parameters )
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
		}
	}

	static { new ComparisonShop().register( "cheapest" ).register( "expensive" ); }
	public static class ComparisonShop
		extends Command
		implements Comparator
	{
		{ usage = "[?] [+]<item> [,[-]item]... [; <cmds>] - compare prices, do cmds with \"it\" replaced with best."; }
		{ flags = KoLmafiaCLI.FULL_LINE_CMD; }
		public void run( String cmd, String parameters )
		{
			boolean expensive = cmd.equals( "expensive" );
			String commands = null;
			int pos = parameters.indexOf( ";" );
			if ( pos != -1 )
			{
				commands = parameters.substring( pos + 1 ).trim();
				parameters = parameters.substring( 0, pos ).trim();
			}
			String[] pieces = parameters.split( "\\s*,\\s*" );
			TreeSet names = new TreeSet();
			for ( int i = 0; i < pieces.length ; ++i )
			{
				String piece = pieces[ i ];
				if ( piece.startsWith( "+" ) )
				{
					AdventureResult item = ItemFinder.getFirstMatchingItem(
						piece.substring( 1 ).trim() );
					if ( item == null )
					{
						return;
					}
					names.addAll( Arrays.asList(
						ZapRequest.getZapGroup( item.getItemId() ) ) );
				}
				else if ( piece.startsWith( "-" ) )
				{
					names.removeAll( ItemDatabase.getMatchingNames(
						piece.substring( 1 ).trim() ) );
				}
				else
				{
					names.addAll( ItemDatabase.getMatchingNames( piece ) );
				}
			}
			if ( names.size() == 0 )
			{
				KoLmafia.updateDisplay( KoLConstants.ERROR_STATE,
					"No matching items!" );
				return;
			}
			if ( KoLmafiaCLI.isExecutingCheckOnlyCommand )
			{
				RequestLogger.printList( Arrays.asList( names.toArray() ) );
				return;
			}
			ArrayList results = new ArrayList();
			Iterator i = names.iterator();
			while ( i.hasNext() )
			{
				AdventureResult item = new AdventureResult( (String) i.next() );
				if ( !ItemDatabase.isTradeable( item.getItemId() ) ||
					StoreManager.getMallPrice( item ) <= 0 )
				{
					continue;
				}
				if ( !KoLmafia.permitsContinue() )
				{
					return;
				}
				results.add( item );
			}
			if ( results.size() == 0 )
			{
				KoLmafia.updateDisplay( KoLConstants.ERROR_STATE,
					"No tradeable items!" );
				return;
			}
			Collections.sort( results, (Comparator) this );
			if ( expensive )
			{
				Collections.reverse( results );
			}
			if ( commands != null )
			{
				CLI.executeLine( commands.replaceAll( "\\bit\\b",
					((AdventureResult) results.get( 0 )).getName() ) );
				return;
			}
			i = results.iterator();
			while ( i.hasNext() )
			{
				AdventureResult item = (AdventureResult) i.next();
				RequestLogger.printLine( item.getName() + " @ " +
					StoreManager.getMallPrice( item ) );
			}
		}
		
		public int compare( Object o1, Object o2 )
		{
			return StoreManager.getMallPrice( (AdventureResult) o1 ) -
				StoreManager.getMallPrice( (AdventureResult) o2 );
		}
	}

	static { new Quark().register( "quark" ); }
	public static class Quark
		extends Command
		implements Comparator
	{
		{ usage = "[?] [<itemList>...] - gain MP by pasting unstable quark with best item from itemList (or your junk list)."; }
		public void run( String cmd, String parameters )
		{
			if ( ItemPool.get( ItemPool.UNSTABLE_QUARK, 1 ).getCount(
				KoLConstants.inventory ) < 1 )
			{
				KoLmafia.updateDisplay( "You have no unstable quarks." );
				return;
			}
			if ( !KoLCharacter.inMuscleSign() )
			{
				AdventureResult paste = ItemPool.get( ItemPool.MEAT_PASTE, 1 );

				if ( !InventoryManager.retrieveItem( paste ) )
				{
					KoLmafia.updateDisplay( "Can't afford gluons." );
					return;
				}
			}			
			
			List items = KoLConstants.junkList;
			if ( !parameters.equals( "" ) )
			{
				items = Arrays.asList( ItemFinder.getMatchingItemList(
					KoLConstants.inventory, parameters ) );
				if ( items.size() == 0 )
				{
					return;
				}
			}

			ArrayList usables = new ArrayList();
			Iterator i = items.iterator();
			while ( i.hasNext() )
			{
				AdventureResult item = (AdventureResult) i.next();
				if ( item.getCount( KoLConstants.inventory ) < 
					( KoLConstants.singletonList.contains( item ) ? 2 : 1 ) )
				{
					continue;
				}
				int price = ItemDatabase.getPriceById( item.getItemId() );
				if ( price < 20 || KoLCharacter.getCurrentMP() + price > KoLCharacter.getMaximumMP() )
				{
					continue;
				}
				if ( this.isPasteable( item ) )
				{
					usables.add( item.getInstance( price ) );
				}
			}
			if ( usables.size() == 0 )
			{
				KoLmafia.updateDisplay( "No suitable quark-pasteable items found." );
				return;
			}

			Collections.sort( usables, (Comparator) this );
			if ( KoLmafiaCLI.isExecutingCheckOnlyCommand )
			{
				RequestLogger.printLine( usables.get( 0 ).toString() );
				return;
			}
			
			AdventureResult item = (AdventureResult) usables.get( 0 );
			RequestLogger.printLine( "Pasting unstable quark with " + item );
			GenericRequest visitor = new GenericRequest( 
				"craft.php?action=craft&mode=combine&ajax=1&pwd&qty=1&a=3743&b="
				+ item.getItemId() );
			RequestThread.postRequest( visitor );
			StaticEntity.externalUpdate( visitor.getURLString(),
				visitor.responseText );
		}
		
		private boolean isPasteable( AdventureResult item )
		{
			Iterator i = ConcoctionDatabase.getKnownUses( item ).iterator();
			while ( i.hasNext() )
			{
				AdventureResult use = (AdventureResult) i.next();
				if ( ConcoctionDatabase.getMixingMethod( use.getItemId() )
					== KoLConstants.COMBINE )
				{
					return true;
				}
			}
			return false;
		}
		
		public int compare( Object o1, Object o2 )
		{
			return ( (AdventureResult) o2 ).getCount() -
				( (AdventureResult) o1 ).getCount();
		}
	}

	static { new Attack().register( "pvp" ).register( "attack" ); }
	public static class Attack
		extends Command
	{
		{ usage = " [ <target> [, <target>]... ] - PvP for dignity or flowers"; }
		public void run( String cmd, String parameters )
		{
			if ( parameters.equals( "" ) )
			{
				KoLmafiaGUI.constructFrame( "FlowerHunterFrame" );
				return;
			}
			
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

				String text = KoLmafia.whoisPlayer( playerId );
				Matcher idMatcher =
					Pattern.compile( "\\(#(\\d+)\\)" ).matcher( text );

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
		}
	}

	static { new Flowers().register( "flowers" ); }
	public static class Flowers
		extends Command
	{
		{ usage = " - commit random acts of PvP."; }
		public void run( String cmd )
		{
			PvpManager.executeFlowerHuntRequest();
		}
	}

	static { new PvPLog().registerPrefix( "pvplog" ); }
	public static class PvPLog
		extends Command
	{
		{ usage = " - summarize PvP results."; }
		public void run( String cmd )
		{
			PvpManager.summarizeFlowerHunterData();
		}
	}

	static { new Galaktik().register( "galaktik" ); }
	public static class Galaktik
		extends Command
	{
		{ usage = "(hp|mp) [<amount>] - restore some or all hp or mp"; }
		public void run( String cmd, String parameters )
		{
			CLI.executeGalaktikRequest( parameters );
		}
	}

	static { new Grandpa().register( "grandpa" ); }
	public static class Grandpa
		extends Command
	{
		{ usage = " <query> - Ask Grandpa about something."; }
		public void run( String cmd, String parameters )
		{
			CLI.executeGrandpaRequest( parameters );
		}
	}

	static { new Hermit().register( "hermit" ); }
	public static class Hermit
		extends Command
	{
		{ usage = "[?] [<item>] - get clover status, or trade for item."; }
		public void run( String cmd, String parameters )
		{
			CLI.executeHermitRequest( parameters );
		}
	}

	static { new HiddenCity().register( "hiddencity" ); }
	public static class HiddenCity
		extends Command
	{
		{ usage = " <square> [temple|altar <item>] - set Hidden City square [and perform an action there]."; }
		public void run( String cmd, String parameters )
		{
			CLI.executeHiddenCityRequest( parameters );
		}
	}

	static { new Raffle().register( "raffle" ); }
	public static class Raffle
		extends Command
	{
		{ usage = " <ticketsToBuy> [ inventory | storage ] - buy raffle tickets"; }
		public void run( String cmd, String parameters )
		{
			CLI.executeRaffleRequest( parameters );
		}
	}

	static { new Styx().register( "styx" ); }
	public static class Styx
		extends Command
	{
		{ usage = " muscle | mysticality | moxie - get daily Styx Pixie buff."; }
		public void run( String cmd, String parameters )
		{
			CLI.executeStyxRequest( parameters );
		}
	}

	static { new Concert().register( "concert" ); }
	public static class Concert
		extends Command
	{
		{ usage = " m[oon'd] | d[ilated pupils] | o[ptimist primal] | e[lvish] | wi[nklered] | wh[ite-boy angst]"; }
		public void run( String cmd, String parameters )
		{
			CLI.executeArenaRequest( parameters );
		}
	}

	static { new Friars().register( "friars" ); }
	public static class Friars
		extends Command
	{
		{ usage = " [blessing] food | familiar | booze - get daily blessing."; }
		public void run( String cmd, String parameters )
		{
			CLI.executeFriarRequest( parameters );
		}
	}

	static { new Nuns().register( "nuns" ); }
	public static class Nuns
		extends Command
	{
		{ usage = " [mp] - visit the Nunnery for restoration [but only if MP is restored]."; }
		public void run( String cmd, String parameters )
		{
			CLI.executeNunsRequest( parameters );
		}
	}

	static { new MPItems().register( "mpitems" ); }
	public static class MPItems
		extends Command
	{
		{ usage = " - counts MP restoratives in inventory."; }
		public void run( String cmd )
		{
			int restores = KoLmafia.getRestoreCount();
			RequestLogger.printLine( restores + " mana restores remaining." );
		}
	}

	static { new Recover().registerPrefix( "restore" ).registerPrefix( "recover" )
		.registerPrefix( "check" ); }
	public static class Recover
		extends Command
	{
		{ usage = " hp | health | mp | mana | both - attempt to regain some HP or MP."; }
		public void run( String cmd, String parameters )
		{
			boolean wasRecoveryActive = KoLmafia.isRunningBetweenBattleChecks();
			KoLmafia.recoveryActive = true;

			SpecialOutfit.createImplicitCheckpoint();
			int target;
			if ( parameters.equalsIgnoreCase( "hp" ) ||
				parameters.equalsIgnoreCase( "health" ) || 
				parameters.equalsIgnoreCase( "both" ) )
			{
				target = (int) (Preferences.getFloat( "hpAutoRecoveryTarget" )
					* (float) KoLCharacter.getMaximumHP());
				StaticEntity.getClient().recoverHP( Math.max( target,
					KoLCharacter.getCurrentHP() + 1 ) );
			}
			if ( parameters.equalsIgnoreCase( "mp" ) ||
				parameters.equalsIgnoreCase( "mana" ) || 
				parameters.equalsIgnoreCase( "both" ) )
			{
				target = (int) (Preferences.getFloat( "mpAutoRecoveryTarget" )
					* (float) KoLCharacter.getMaximumMP());
				StaticEntity.getClient().recoverMP( Math.max( target,
					KoLCharacter.getCurrentMP() + 1 ) );
			}
			SpecialOutfit.restoreImplicitCheckpoint();

			KoLmafia.recoveryActive = wasRecoveryActive;
		}
	}

	static { new Trigger().registerPrefix( "trigger" ); }
	public static class Trigger
		extends Command
	{
		{ usage = " clear | autofill | [<type>,] <effect> [, <action>] - edit current mood"; }
		public void run( String cmd, String parameters )
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
		}
	}

	static { new Mood().registerPrefix( "mood" ); }
	public static class Mood
		extends Command
	{
		{ usage = " clear | autofill | execute | repeat [<numTimes>] | <moodName> [<numTimes>] - mood management."; }
		public void run ( String cmd, String parameters )
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

				if ( multiplicity > 0 )
				{
					CLI.executeCommand( "mood", "repeat " + multiplicity );
					MoodManager.setMood( previousMood );
				}
			}
		}
	}

	static { new Restaurant().register( "restaurant" ).registerSubstring( "brewery" ); }
	public static class Restaurant
		extends Command
	{
		{ usage = "[?] [ daily special | <item> ] - show daily special [or consume it or other restaurant item]."; }
		public void run( String cmd, String parameters )
		{
			if ( cmd.equals( "restaurant" ) )
			{
				CLI.makeChezSnooteeRequest( parameters );
			}
			else
			{
				CLI.makeMicroBreweryRequest( parameters );
			}
		}
	}

	static { new Kitchen().registerSubstring( "kitchen" ); }
	public static class Kitchen
		extends Command
	{
		{ usage = "[?] <item> - consumes item at Hell's Kitchen, if available."; }
		public void run( String cmd, String parameters )
		{
			CLI.makeHellKitchenRequest( parameters );
		}
	}

	static {
		new Camp().registerPrefix( "camp" ); 
		new AliasCmd( "campground", "rest" ).register( "rest" );
	}
	public static class Camp
		extends Command
	{
		{ usage = " rest | <etc.> [<numTimes>] - perform campground actions."; }
		public void run( String cmd, String[] parameters )
		{
			StaticEntity.getClient().makeRequest(
				new CampgroundRequest( parameters[ 0 ] ),
				parameters.length == 1 ? 1 : StringUtilities.parseInt( parameters[ 1 ] ) );
		}
	}

	static { new Sofa().register( "sofa" ).register( "sleep" ); }
	public static class Sofa
		extends Command
	{
		{ usage = " <number> - rest on your clan sofa for number turns."; }
		public void run( String cmd, String parameters )
		{
			RequestThread.postRequest( ( new ClanRumpusRequest( ClanRumpusRequest.SOFA ) ).setTurnCount( StringUtilities.parseInt( parameters ) ) );
		}
	}

	static { new HotTub().register( "hottub" ).register( "soak" ); }
	public static class HotTub
		extends Command
	{
		{ usage = " - soak in your clan's hot tub"; }
		public void run( String cmd, String parameters )
		{
			RequestThread.postRequest( new ClanLoungeRequest( ClanLoungeRequest.HOTTUB ) );
		}
	}

	static { new Send().register( "send" ).register( "kmail" ).register( "csend" ); }
	public static class Send
		extends Command
	{
		{ usage = " <item> [, <item>]... to <recipient> [ || <message> ] - send kmail"; }
		public void run( String cmd, String parameters )
		{
			if ( KoLmafia.isRunningBetweenBattleChecks() )
			{
				RequestLogger.printLine( "Send request \"" + parameters + "\" ignored in between-battle execution." );
				return;
			}

			CLI.executeSendRequest( parameters, cmd.equals( "csend" ) );
		}
	}

	static {
		new AliasCmd( "skills", "buff" ).registerPrefix( "buff" );
		new AliasCmd( "skills", "passive" ).registerPrefix( "pass" );
		new AliasCmd( "skills", "self" ).registerPrefix( "self" );
		new AliasCmd( "skills", "combat" ).registerPrefix( "combat" );
	}
	public static class AliasCmd
		extends Command
	{
		private String actualCmd, actualParams;
		public AliasCmd( String actualCmd, String actualParams )
		{
			super();
			this.actualCmd = actualCmd;
			this.actualParams = actualParams;
			this.usage = " => " + actualCmd + " " + actualParams;
		}
		
		public void run( String cmd, String parameters )
		{
			CLI.executeCommand( this.actualCmd, (this.actualParams + " " + parameters).trim() );
		}
	}

	static { new Counters().register( "counters" ); }
	public static class Counters
		extends Command
	{
		{ usage = " [ clear | add <number> [<title> <img>] ] - show, clear, or add to current turn counters."; }
		public void run( String cmd, String parameters )
		{
			if ( parameters.equalsIgnoreCase( "clear" ) )
			{
				TurnCounter.clearCounters();
				return;
			}

			if ( parameters.startsWith( "deletehash " ) )
			{
				TurnCounter.deleteByHash(
					StringUtilities.parseInt( parameters.substring( 11 ) ) );
				return;
			}

			if ( parameters.startsWith( "add " ) )
			{
				String title = "Manual";
				String image = "watch.gif";
				parameters = parameters.substring( 4 ).trim();
				if ( parameters.endsWith( ".gif" ) )
				{
					int lastSpace = parameters.lastIndexOf( " " );
					image = parameters.substring( lastSpace + 1 );
					parameters = parameters.substring( 0, lastSpace + 1 ).trim();
				}
				int spacePos = parameters.indexOf( " " );
				if ( spacePos != -1 )
				{
					title = parameters.substring( spacePos + 1 );
					parameters = parameters.substring( 0, spacePos ).trim();
				}
				
				TurnCounter.startCounting( StringUtilities.parseInt( parameters ),
					title, image );
			}

			CLI.showData( "counters" );
		}
	}

	static { new ShowData().registerPrefix( "inv" ).register( "storage" )
		.register( "session" ).register( "summary" ).register( "effects" ).register( "status" )
		.register( "skills" ).register( "locations" ).register( "encounters" )
		.registerPrefix( "moon" ); }
	public static class ShowData
		extends Command
	{
		{ usage = " [<param>] - list indicated type of data, possibly filtered by param."; }
		public void run( String cmd, String parameters )
		{
			CLI.showData( cmd + " " + parameters );
		}
	}

	static { new Location().register( "location" ); }
	public static class Location
		extends Command
	{
		{ usage = null; }
		public void run( String cmd, String parameters )
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
		}
	}

	static { new Help().register( "help" ); }
	public static class Help
		extends Command
	{
		{ usage = " [<filter>] - list CLI commands [that match filter]."; }
		
		static private Pattern PLACEHOLDER = Pattern.compile( "<(.+?)>" );
		
		public void run( String cmd, String filter )
		{
			filter = filter.toLowerCase();
			if ( filter.equals( "help" ) )
			{
				RequestLogger.printLine( "Square brackets [ ] enclose optional elements of " +
				"commands.  In command descriptions, they may also enclose the effects of  " +
				"using those optional elements." );
				RequestLogger.printLine();
				RequestLogger.printLine( "Vertical bars | separate alternative elements -  " +
				"choose any one.  (But note that || is an actual part of a few commands.)" );
				RequestLogger.printLine();
				RequestLogger.printLine( "An ellipsis ... after an element means that it  " +
				"can be repeated as many times as needed." );
				RequestLogger.printLine();
				RequestLogger.printLine( "Elements in <i>italics</i> are placeholders -  " +
				"replace them with an actual name you want the command to operate on." );
				RequestLogger.printLine();
				RequestLogger.printLine( "Commands with an asterisk * after the name are " +
				"abbreviations - you can type them in a longer form if desired." );
				RequestLogger.printLine();
				RequestLogger.printLine( "Some command names can be followed by a question  " +
				"mark (shown as [?] ), in which case the command will just display what it  " +
				"would do, rather than actually doing it." );
				RequestLogger.printLine();
				RequestLogger.printLine( "When adventuring, or using an item or skill, the  " +
				"name can be preceded by a number specifying how many times to do it.  An  " +
				"asterisk in place of this number means \"as many as possible\" or \"the  " +
				"current quantity in inventory\", depending on context.  Negative numbers  " +
				"mean to do that many less than the maximum." );
				RequestLogger.printLine();
				RequestLogger.printLine( "Usually, multiple commands can be given on the  " +
				"same line, separated by semicolons.  The exceptions (" + Command.fullLineCmds +
				") treat the entire remainder of the line as a parameter." );
				RequestLogger.printLine();
				RequestLogger.printLine( "A few commands (" + Command.flowControlCmds + 
				") treat at least one following command as a block that is executed  " +
				"conditionally or repetitively.  The block consists of the remainder of the  " +
				"line, or the entire next line if that's empty.  The block is extended by " +
				"additional lines if it would otherwise end with one of these special  " +
				"commands." );
				return;
			}
			boolean anymatches = false;
			HashMap alreadySeen = new HashMap();	// usage => name of cmd already printed out
			Iterator i = Command.lookup.entrySet().iterator();
			while ( i.hasNext() )
			{
				Map.Entry e = (Map.Entry) i.next();
				String name = (String) e.getKey();
				int type = lookup.getKeyType( name );
				if ( type == lookup.NOT_A_KEY )
				{
					continue;
				}
				Command handler = (Command) e.getValue();
				if ( handler == null )	// shouldn't happen
				{
					continue;
				}
				String usage = handler.getUsage( name );
				if ( usage == null || (name.indexOf( filter ) == -1 &&
					usage.toLowerCase().indexOf( filter ) == -1) )
				{
					continue;
				}
				if ( type == lookup.PREFIX_KEY )
				{
					name += "*";
				}
				if ( KoLmafiaCLI.isExecutingCheckOnlyCommand )
				{
					RequestLogger.printLine( DataUtilities.convertToHTML( name ) + " @ " +
						handler.getClass().getName() );
					anymatches = true;
					continue;
				}
				String previouslySeen = (String) alreadySeen.get( usage );
				if ( previouslySeen == null )
				{
					// This isn't turning out very useful
					// alreadySeen.put( usage, name );
				}
				else
				{
					usage = " => " + previouslySeen;
				}
				anymatches = true;
				Matcher m = PLACEHOLDER.matcher( usage );
				while ( m.find() )
				{
					usage = Pattern.compile( "<?(\\Q" + m.group( 1 ) + "\\E)>?" )
						.matcher( usage ).replaceAll( "<i>$1</i>" );
				}
				RequestLogger.printLine( DataUtilities.convertToHTML( name ) + usage );
			}
			if ( !anymatches )
			{
				KoLmafia.updateDisplay( "No matching commands found!" );
			}
		}
	}

	static { new Spade().register( "spade" ); }
	public static class Spade
		extends Command
	{
		{ usage = " - examine and submit or delete any automatically gathered data."; }
		public void run( String cmd )
		{
			String[] data = Preferences.getString( "spadingData" ).split( "\\|" );
			if ( data.length < 3 )
			{
				KoLmafia.updateDisplay( "No spading data has been collected yet. " +
					"Please try again later." );
				return;
			}
			for ( int i = 0; i < data.length-2; i += 3 )
			{
				String contents = data[ i ];
				String recipient = data[ i+1 ];
				String explanation = data[ i+2 ];
				if ( InputFieldUtilities.confirm ( 
					"Would you like to send the data \"" + contents + "\" to " +
					recipient + "?\nThis information will be used " + explanation ) )
				{
					RequestThread.postRequest( new SendMailRequest( recipient, contents ) );
				}
			}
			Preferences.setString( "spadingData", "" );
		}
	}

	static { new Budget().register( "budget" ); }
	public static class Budget
		extends Command
	{
		{ usage = " [<number>] - show [or set] the number of budgeted Hagnk's pulls."; }
		public void run( String cmd, String parameters )
		{
			if ( parameters.length() > 0 )
			{
				ItemManageFrame.setPullsBudgeted( StringUtilities.parseInt( parameters ) );
			}
			KoLmafia.updateDisplay( ItemManageFrame.getPullsBudgeted() +
				" pulls budgeted for automatic use, " + ItemManageFrame.getPullsRemaining()
				+ " pulls remaining." );
		}
	}

	public void showHTML( final String location, final String text )
	{
		// Remove HTML header and comments.
		String displayText = KoLmafiaCLI.HEAD_PATTERN.matcher( text ).replaceAll( "" );
		displayText = KoLmafiaCLI.COMMENT_PATTERN.matcher( displayText ).replaceAll( "" );

		// Strip out all the new lines found in the source
		// so you don't accidentally add more new lines than
		// necessary.

		displayText = displayText.replaceAll( "[\r\n]+", "" );

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

		RequestLogger.printLine( displayText.trim() );
	}

	private void executeSendRequest( final String parameters, boolean isConvertible )
	{
		String[] splitParameters = parameters.replaceFirst( "(?:^| )[tT][oO] ", " => " ).split( " => " );

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

		// String.split() is weird!  An empty string, split on commas, produces
		// a 1-element array containing an empty string.  However, a string
		// containing just one or more commas produces a 0-element array???!!!
		splitParameters[ 0 ] = splitParameters[ 0 ].trim() + ",";
		splitParameters[ 1 ] = splitParameters[ 1 ].trim();

		Object[] attachments = ItemFinder.getMatchingItemList( KoLConstants.inventory, splitParameters[ 0 ] );

		if ( attachments.length == 0 && ( splitParameters[ 0 ].length() > 1 ||
			message == KoLConstants.DEFAULT_KMAIL ) )
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

		if ( HolidayDatabase.getHoliday().startsWith( "Valentine's" ) )
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
		
		if ( left.equals( "stickers" ) )
		{
			int count = 0;
			for ( int i = EquipmentManager.STICKER1; i <= EquipmentManager.STICKER3; ++i )
			{
				AdventureResult item = EquipmentManager.getEquipment( i ) ;
				if ( !EquipmentRequest.UNEQUIP.equals( item ) )
				{
					++count;
				}
			}
			return count;
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

		if ( option.equals( "check" ) )
		{
			KoLmafia.checkRequirements( KoLConstants.conditions );
			RequestLogger.printLine( "Conditions list validated against available items." );
			return true;
		}

		if ( option.equals( "add" ) || option.equals( "set" ) )
		{
			String[] conditionsList = parameters.substring( option.length() ).toLowerCase().trim().split( "\\s*,\\s*" );

			for ( int i = 0; i < conditionsList.length; ++i )
			{
				String condition = conditionsList[ i ];

				if ( condition.equalsIgnoreCase( "castle map items" ) )
				{
					this.addItemCondition( "set", ItemPool.get( ItemPool.FURRY_FUR, 1 ) );
					this.addItemCondition( "set", ItemPool.get( ItemPool.GIANT_NEEDLE, 1 ) );
					this.addItemCondition( "set", ItemPool.get( ItemPool.AWFUL_POETRY_JOURNAL, 1 ) );
					continue;
				}

				AdventureResult ar = this.extractCondition( condition );
				if ( ar != null )
				{
					this.addItemCondition( option, ar );
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

		Matcher meatMatcher = KoLmafiaCLI.MEAT_PATTERN.matcher( conditionString );
		boolean isMeatCondition = meatMatcher.find() ? meatMatcher.group().length() == conditionString.length() : false;

		if ( isMeatCondition )
		{
			String[] splitCondition = conditionString.split( "\\s+" );
			int amount = StringUtilities.parseInt( splitCondition[ 0 ] );
			return new AdventureResult( AdventureResult.MEAT, amount );
		}

		if ( conditionString.endsWith( "choiceadv" ) || conditionString.endsWith( "choices" ) || conditionString.endsWith( "choice" ) )
		{
			// If it's a choice adventure condition, parse out the
			// number of choice adventures the user wishes to do.

			String[] splitCondition = conditionString.split( "\\s+" );
			int count = splitCondition.length > 1 ? StringUtilities.parseInt( splitCondition[ 0 ] ) : 1;
			return new AdventureResult( AdventureResult.CHOICE, count );
		}

		if ( conditionString.startsWith( "level" ) )
		{
			// If the condition is a level, then determine how many
			// substat points are required to the next level and
			// add the substat points as a condition.

			String[] splitCondition = conditionString.split( "\\s+" );
			int level = StringUtilities.parseInt( splitCondition[ 1 ] );

			int primeIndex = KoLCharacter.getPrimeIndex();

			AdventureResult.CONDITION_SUBSTATS[ primeIndex ] = (int)
				(KoLCharacter.calculateSubpoints( ( level - 1 ) * ( level - 1 ) + 4, 0 ) - KoLCharacter.getTotalPrime());

			return AdventureResult.CONDITION_SUBSTATS_RESULT;
		}

		if ( conditionString.endsWith( "mus" ) || conditionString.endsWith( "muscle" ) || conditionString.endsWith( "moxie" ) || conditionString.endsWith( "mys" ) || conditionString.endsWith( "myst" ) || conditionString.endsWith( "mox" ) || conditionString.endsWith( "mysticality" ) )
		{
			String[] splitCondition = conditionString.split( "\\s+" );

			int points = StringUtilities.parseInt( splitCondition[ 0 ] );
			int statIndex = conditionString.indexOf( "mus" ) != -1 ? 0 : conditionString.indexOf( "mys" ) != -1 ? 1 : 2;

			AdventureResult.CONDITION_SUBSTATS[ statIndex ] = (int) KoLCharacter.calculateSubpoints( points, 0 );
			AdventureResult.CONDITION_SUBSTATS[ statIndex ] =
				Math.max(
					0,
					AdventureResult.CONDITION_SUBSTATS[ statIndex ] - (int) ( conditionString.indexOf( "mus" ) != -1 ? KoLCharacter.getTotalMuscle() : conditionString.indexOf( "mys" ) != -1 ? KoLCharacter.getTotalMysticality() : KoLCharacter.getTotalMoxie() ) );

			return AdventureResult.CONDITION_SUBSTATS_RESULT;
		}

		if ( conditionString.endsWith( "health" ) || conditionString.endsWith( "mana" ) )
		{
			String type;
			int max, current;

			if ( conditionString.endsWith( "health" ) )
			{
				type = AdventureResult.HP;
				max = KoLCharacter.getMaximumHP();
				current = KoLCharacter.getCurrentHP();
			}
			else
			{
				type = AdventureResult.MP;
				max = KoLCharacter.getMaximumMP();
				current = KoLCharacter.getCurrentMP();
			}

			String numberString = conditionString.split( "\\s+" )[ 0 ];
			int points;

			if ( numberString.endsWith( "%" ) )
			{
				int num = StringUtilities.parseInt( numberString.substring( 0, numberString.length() - 1 ) );
				points = (int) ( (float) num * (float) max / 100.0f );
			}
			else
			{
				points = StringUtilities.parseInt( numberString );
			}

			points -= current;

			AdventureResult condition = new AdventureResult( type, points );

			int previousIndex = KoLConstants.conditions.indexOf( condition );
			if ( previousIndex != -1 )
			{
				AdventureResult previousCondition = (AdventureResult) KoLConstants.conditions.get( previousIndex );
				condition = condition.getInstance( condition.getCount() - previousCondition.getCount() );
			}

			return condition;
		}

		if ( conditionString.endsWith( "outfit" ) )
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

			return null;
		}

		AdventureResult rv = AdventureResult.WildcardResult.getInstance( conditionString );
		return rv != null ? rv : ItemFinder.getFirstMatchingItem( conditionString );
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

		AdventureResult match = ItemFinder.getFirstMatchingItem( parameters,
			ItemFinder.EQUIP_MATCH );
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
				KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "You can't equip a " + match.getName() );
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

		for ( int i = 0; i <= EquipmentManager.STICKER3; ++i )
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
			AdventureResult st1 = EquipmentManager.getEquipment( EquipmentManager.STICKER1 );
			AdventureResult st2 = EquipmentManager.getEquipment( EquipmentManager.STICKER2 );
			AdventureResult st3 = EquipmentManager.getEquipment( EquipmentManager.STICKER3 );
			if ( st1 != EquipmentRequest.UNEQUIP || st2 != EquipmentRequest.UNEQUIP
				|| st3 != EquipmentRequest.UNEQUIP )
			{
				desiredStream.println();
				desiredStream.println( "Sticker 1: " + getStickerText( st1,
					EquipmentManager.getTurns( EquipmentManager.STICKER1 ) ) );
				desiredStream.println( "Sticker 2: " + getStickerText( st2,
					EquipmentManager.getTurns( EquipmentManager.STICKER2 ) ) );
				desiredStream.println( "Sticker 3: " + getStickerText( st3,
					EquipmentManager.getTurns( EquipmentManager.STICKER3 ) ) );
			}
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
				KoLConstants.inventory;

			if ( desiredData.equals( "effects" ) )
			{
				mainList = KoLConstants.activeEffects;
				AdventureResult[] effects = new AdventureResult[ mainList.size() ];
				mainList.toArray( effects );

				int nBuffs = 0;

				for ( int i = 0; i < effects.length; ++i )
				{
					String skillName = UneffectRequest.effectToSkill( effects[ i ].getName() );
					if ( SkillDatabase.contains( skillName ) )
					{
						int skillId = SkillDatabase.getSkillId( skillName );
						if ( skillId > 6000 && skillId < 7000 )
						{
							++nBuffs;
						}
					}
				}

				desiredStream.println( nBuffs + " of " + UseSkillRequest.songLimit() +
					" AT buffs active." );
			}

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

	private static final String getStickerText( AdventureResult item, int turns )
	{
		if ( !item.equals( EquipmentRequest.UNEQUIP ) )
		{
			item = item.getInstance( turns );
		}
		return item.toString();
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
		List list = null;

		int space = parameters.indexOf( " " );
		if ( space != -1 )
		{
			String command = parameters.substring( 0, space );
			if ( command.equals( "take" ) )
			{
				isWithdraw = true;
				parameters = parameters.substring( 4 ).trim();
				list = ClanManager.getStash();
				if ( list.isEmpty() )
				{
					RequestThread.postRequest( new ClanStashRequest() );
				}
			}
			else if ( command.equals( "put" ) )
			{
				parameters = parameters.substring( 3 ).trim();
				list = KoLConstants.inventory;
			}
		}

		if ( list == null )
		{
			return;
		}

		Object[] itemList = ItemFinder.getMatchingItemList( list, parameters );
		if ( itemList.length == 0 )
		{
			return;
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
			String remedy = locateItem( desc[ 3 ] );
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
				String item = locateItem( SorceressLairManager.guardianItem( desc ) );
				RequestLogger.printLine( "Tower Guardian #" + i + ": " + name + " (" + item + ")" );
			}
			else
			{
				RequestLogger.printLine( "Tower Guardian #" + i + ": " + prop + " (unrecognized)" );
			}
		}
	}

	private String locateItem( String name )
	{
		AdventureResult item = ItemPool.get( name, 1 );
		boolean closet = KoLConstants.closet.contains( item );
		if ( KoLConstants.inventory.contains( item ) )
		{
			if ( closet )
			{
				return name + " - have &amp; in closet";
			}
			else
			{
				return name + " - have";
			}
		}
		else if ( closet )
		{
			return name + " - in closet";
		}
		else
		{
			return name + " - NEED";
		}
	}

	/**
	 * A special module used specifically for properly instantiating
	 * StorageRequests which pulls things from Hagnk's.
	 */

	private void executeHagnkRequest( final String parameters )
	{
		if ( KoLCharacter.inBadMoon() && !KoLCharacter.canInteract() )
		{
			KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "Hagnk's Storage is not available in Bad Moon until you free King Ralph." );
			return;
		}

		Object[] items;
		if ( parameters.startsWith( "outfit " ) )
		{
			SpecialOutfit outfit = KoLmafiaCLI.getMatchingOutfit(
				parameters.substring( 7 ).trim() );
			if ( outfit == null )
			{
				KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "No such outfit." );
				return;
			}
			AdventureResult[] pieces = outfit.getPieces();
			ArrayList needed = new ArrayList();
			for ( int i = 0; i < pieces.length; ++i )
			{
				if ( !InventoryManager.hasItem( pieces[ i ] ) )
				{
					needed.add( pieces[ i ] );
				}
			}
			items = needed.toArray();
		}
		else
		{		
			items = ItemFinder.getMatchingItemList( KoLConstants.storage, parameters );
		}

		if ( items.length == 0 )
		{
			return;
		}

		int meatAttachmentCount = 0;

		for ( int i = 0; i < items.length; ++i )
		{
			if ( ( (AdventureResult) items[ i ] ).getName().equals( AdventureResult.MEAT ) )
			{
				RequestThread.postRequest( new StorageRequest(
					StorageRequest.PULL_MEAT_FROM_STORAGE,
					( (AdventureResult) items[ i ] ).getCount() ) );

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

		RequestThread.postRequest( new StorageRequest( StorageRequest.STORAGE_TO_INVENTORY, items ) );
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
					KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "[" + itemNames[ i ] + "] has no matches." );
					return;
				}

				prices[ i ] = StringUtilities.parseInt( itemNames[ i ].substring( spaceIndex + 1 ) );
				itemNames[ i ] = itemNames[ i ].substring( 0, spaceIndex ).trim();
				items[ i ] = ItemFinder.getFirstMatchingItem( itemNames[ i ], false );
			}

			if ( items[ i ] == null )
			{
				int spaceIndex = itemNames[ i ].lastIndexOf( " " );
				if ( spaceIndex == -1 )
				{
					KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "[" + itemNames[ i ] + "] has no matches." );
					return;
				}

				limits[ i ] = prices[ i ];
				prices[ i ] = StringUtilities.parseInt( itemNames[ i ].substring( spaceIndex + 1 ) );
				itemNames[ i ] = itemNames[ i ].substring( 0, spaceIndex ).trim();

				items[ i ] = ItemFinder.getFirstMatchingItem( itemNames[ i ], false );
			}

			if ( items[ i ] == null )
			{
				KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "[" + itemNames[ i ] + "] has no matches." );
				return;
			}

			int inventoryCount = items[ i ].getCount( KoLConstants.inventory );

			if ( items[ i ].getCount() > inventoryCount )
			{
				items[ i ] = items[ i ].getInstance( inventoryCount );
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
		String[] itemNames = parameters.split( "\\s*,\\s*" );

		for ( int i = 0; i < itemNames.length; ++i )
		{
			String[] pieces = itemNames[ i ].split( "@" );
			AdventureResult match = ItemFinder.getFirstMatchingItem( pieces[ 0 ] );
			if ( match == null )
			{
				return;
			}
			int priceLimit = pieces.length < 2 ? 0 : StringUtilities.parseInt( pieces[ 1 ] );

			if ( !KoLCharacter.canInteract() && !NPCStoreDatabase.contains( match.getName() ) )
			{
				KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "You are not yet out of ronin." );
				return;
			}

			ArrayList results = StoreManager.searchMall( match );
			StaticEntity.getClient().makePurchases( results, results.toArray(), match.getCount(), false, priceLimit );
			StoreManager.updateMallPrice( match, results );
		}
	}

	/**
	 * A special module used specifically for properly instantiating ItemCreationRequests.
	 */

	private void executeItemCreationRequest( final String parameters )
	{
		ConcoctionDatabase.refreshConcoctions();

		if ( parameters.equals( "" ) )
		{
			RequestLogger.printList( ConcoctionDatabase.getCreatables() );
			return;
		}

		ItemFinder.setMatchType( ItemFinder.CREATE_MATCH );
		Object[] itemList = ItemFinder.getMatchingItemList( null, parameters );
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

			irequest = CreateItemRequest.getInstance( currentMatch );

			if ( irequest == null )
			{
				if ( Preferences.getBoolean( "unknownRecipe" + currentMatch.getItemId() ) )
				{
					KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "That item requires a recipe.  If you've already learned it, visit the crafting discoveries page in the relay browser to let KoLmafia know about it." );
					return;
				}
				
				switch ( ConcoctionDatabase.getMixingMethod( currentMatch ) )
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
				case KoLConstants.MIX_SALACIOUS:

					KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "You cannot mix without a bartender-in-the-box." );
					return;

				case KoLConstants.SUSHI:
					KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "You cannot make sushi without a sushi-rolling mat." );
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

	private void executeUseItemRequest( final String command, String parameters )
	{
		boolean either = parameters.startsWith( "either " );
		if ( either )
		{
			parameters = parameters.substring( 7 ).trim();
		}

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

		for ( int level = either ? 0 : 2; level <= 2; ++level )
		{	// level=0: use only items in inventory, exit on first success
			// level=1: buy/make as needed, exit on first success
			// level=2: use all items in list, buy/make as needed
			for ( int i = 0; i < itemList.length; ++i )
			{
				AdventureResult currentMatch = (AdventureResult) itemList[ i ];
				int consumpt = ItemDatabase.getConsumptionType( currentMatch.getItemId() );

				if ( command.equals( "eat" ) && consumpt == KoLConstants.CONSUME_FOOD_HELPER )
				{	// allowed
				}
				else if ( command.equals( "eat" ) || command.equals( "ghost" ) )
				{
					if ( consumpt != KoLConstants.CONSUME_EAT )
					{
						KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, currentMatch.getName() + " cannot be consumed." );
						return;
					}
				}

				if ( command.equals( "drink" ) && consumpt == KoLConstants.CONSUME_DRINK_HELPER )
				{	// allowed
				}
				else if ( command.equals( "drink" ) || command.equals( "hobo" ) )
				{
					if ( consumpt != KoLConstants.CONSUME_DRINK )
					{
						KoLmafia.updateDisplay(
							KoLConstants.ERROR_STATE, currentMatch.getName() + " is not an alcoholic beverage." );
						return;
					}
				}

				if ( command.equals( "use" ) )
				{
					switch ( consumpt )
					{
					case KoLConstants.CONSUME_EAT:
					case KoLConstants.CONSUME_FOOD_HELPER:
						KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, currentMatch.getName() + " must be eaten." );
						return;
					case KoLConstants.CONSUME_DRINK:
					case KoLConstants.CONSUME_DRINK_HELPER:
						KoLmafia.updateDisplay(
							KoLConstants.ERROR_STATE, currentMatch.getName() + " is an alcoholic beverage." );
						return;
					}
				}

				int have = currentMatch.getCount( KoLConstants.inventory );
				if ( level > 0 || have > 0 )
				{
					if ( level == 0 && have < currentMatch.getCount() )
					{
						currentMatch = currentMatch.getInstance( have );
					}
					if ( KoLmafiaCLI.isExecutingCheckOnlyCommand )
					{
						RequestLogger.printLine( currentMatch.toString() );
					}
					else
					{
						UseItemRequest request =
							command.equals( "hobo" ) ? new UseItemRequest( KoLConstants.CONSUME_HOBO, currentMatch ) :
							command.equals( "ghost" ) ? new UseItemRequest( KoLConstants.CONSUME_GHOST, currentMatch ) :
							new UseItemRequest( currentMatch );
						RequestThread.postRequest( request );
					}
					
					if ( level < 2 )
					{
						return;
					}
				}
			}
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

		// Check for exact matches. Skip "No Change" entry at index 0.

		for ( int i = 1; i < customOutfitCount; ++i )
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

		for ( int i = 1; i < customOutfitCount; ++i )
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
		String command = null;

		if ( split.length == 2 && split[ 0 ].equals( "blessing" ) )
		{
			command = split[ 1 ];
		}
		else if (split.length == 1 && !split[ 0 ].equals( "" ) )
		{
			command = split[ 0 ];
		}
		else
		{
			KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "Syntax: friars [blessing] food|familiar|booze" );
			return;
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
	 * Attempts to get HP or HP/MP restoration from the Nuns at Our Lady of Perpetual Indecision
	 */

	public static void executeNunsRequest( final String parameters )
	{
		if ( Preferences.getInteger( "nunsVisits" ) >= 3 ) {
			KoLmafia.updateDisplay( KoLConstants.ERROR_STATE,
				"Nun of the nuns are available right now." );
			return;
		}
		String side = Preferences.getString( "sidequestNunsCompleted" );
		if ( !side.equals( "fratboy" ) && !side.equals( "hippy" ) )
		{
			KoLmafia.updateDisplay( KoLConstants.ERROR_STATE,
				"You have not opened the Nunnery yet." );
			return;
		}
		if ( side.equals( "hippy" ) && parameters.equalsIgnoreCase( "mp" ) )
		{
			KoLmafia.updateDisplay( KoLConstants.ERROR_STATE,
				"Only HP restoration is available from the nuns." );
			return;
		}
		String url = Preferences.getString( "warProgress" ).equals( "finished" ) ?
			"postwarisland.php" : "bigisland.php";

		KoLmafia.updateDisplay( "Get thee to a nunnery!" );
		RequestThread.postRequest( new GenericRequest( url + "?place=nunnery&pwd&action=nuns" ) );
	}

	/**
	 * Makes a request to Doc Galaktik to purchase a cure.  If the
	 * cure is not available, this method does not report an error.
	 */

	public void executeGalaktikRequest( String parameters )
	{
		String[] split = parameters.split( " " );

		// Cure "HP" or "MP"

		String typeString = split[ 0 ];
		String type;

		if ( typeString.equalsIgnoreCase( "hp" ) )
			type = GalaktikRequest.HP;
		else if ( typeString.equalsIgnoreCase( "mp" ) )
			type = GalaktikRequest.MP;
		else
		{
			updateDisplay( KoLConstants.ERROR_STATE, "Unknown Doc Galaktik request <" + parameters + ">" );
			return;
		}

		int amount = split.length == 1 ? 0 : StringUtilities.parseInt( split[ 1 ] );

		RequestThread.postRequest( new GalaktikRequest( type, amount ) );
	}

	/**
	 * Makes a request to Grandpa to ask him a question about something.
	 */

	public void executeGrandpaRequest( String parameters )
	{
		RequestThread.postRequest( new GrandpaRequest( parameters ) );
	}

	/**
	 * Makes a request to visit the hidden city
	 */

	public void executeHiddenCityRequest( String parameters )
	{
		String[] split = parameters.split( " " );

		int square = StringUtilities.parseInt( split[ 0 ] );

		if ( split.length == 1 )
		{
			Preferences.setInteger( "hiddenCitySquare", square );
			KoLmafia.updateDisplay( "Hidden City adventure square set to " + square );

			return;
		}

		HiddenCityRequest request1 = null;
		HiddenCityRequest request2 = null;

		String type = split[1];

		if ( type.equalsIgnoreCase( "temple" ) )
		{
			request1 = new HiddenCityRequest( square );
			request2 = new HiddenCityRequest( true );
		}
		else if ( type.equalsIgnoreCase( "altar" ) && split.length < 3 )
		{
			AdventureResult result = ItemFinder.getFirstMatchingItem( split[2], ItemFinder.ANY_MATCH );
			request1 = new HiddenCityRequest( square );
			request2 = new HiddenCityRequest( true, result.getItemId() );
		}
		else
		{
			updateDisplay( KoLConstants.ERROR_STATE, "Unknown Hidden City request <" + parameters + ">" );
			return;
		}

		RequestThread.openRequestSequence();
		RequestThread.postRequest( request1 );
		RequestThread.postRequest( request2 );
		RequestThread.closeRequestSequence();
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
