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
import java.util.List;
import java.util.Iterator;
import java.text.DecimalFormat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
	private String previousCommand;
	private PrintStream outputStream;
	private BufferedReader commandStream;
	private KoLmafia scriptRequestor;

	/**
	 * The main method.  Currently, it instantiates a single instance
	 * of the <code>KoLmafia</code> client after setting the default
	 * look and feel of all <code>JFrame</code> objects to decorated.
	 */

	public static void main( String [] args )
	{
		try
		{
			String initialScript = null;
			for ( int i = 0; i < args.length; ++i )
				if ( args[i].startsWith( "script=" ) )
					initialScript = args[i].substring( 7 );

			System.out.println();
			System.out.println( "****************" );
			System.out.println( "* KoLmafia CLI *" );
			System.out.println( "****************" );
			System.out.println();

			System.out.println( "Determining server..." );
			KoLRequest.applySettings();
			System.out.println( KoLRequest.getRootHostName() + " selected." );
			System.out.println();

			KoLmafiaCLI session = new KoLmafiaCLI( null, initialScript );

			if ( initialScript == null )
				session.attemptLogin();
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
		super.parseResult( result );

		if ( result.startsWith( "You" ) )
			outputStream.println( result );
		else
			outputStream.println( "You acquire: " + result );
	}

	/**
	 * Constructs a new <code>KoLmafia</code> object.  All data fields
	 * are initialized to their default values, the global settings
	 * are loaded from disk, and a <code>LoginFrame</code> is created
	 * to allow the user to login.
	 */

	public KoLmafiaCLI( KoLmafia scriptRequestor, String scriptLocation ) throws IOException
	{
		InputStream inputStream = scriptLocation == null ? System.in : new FileInputStream( scriptLocation );

		outputStream = scriptRequestor == null ? System.out : new NullStream();
		commandStream = new BufferedReader( new InputStreamReader( inputStream ) );

		this.scriptRequestor = (scriptRequestor == null) ? this : scriptRequestor;
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
			outputStream.println();

			outputStream.print( "login: " );
			String username = commandStream.readLine();

			if ( username == null || username.length() == 0 )
			{
				outputStream.println( "Invalid login." );
				attemptLogin();
				return;
			}

			outputStream.print( "password: " );
			String password = commandStream.readLine();

			if ( password == null || password.length() == 0 )
			{
				outputStream.println( "Invalid password." );
				attemptLogin();
				return;
			}

			outputStream.print( "breakfast?: " );
			String breakfast = commandStream.readLine();

			boolean getBreakfast = true;
			if ( breakfast == null || breakfast.length() == 0 || breakfast.charAt(0) == 'n' || breakfast.charAt(0) == 'N' )
				getBreakfast = false;

			outputStream.println();
			(new LoginRequest( scriptRequestor, username, password, getBreakfast )).run();
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

	public void initialize( String loginname, String sessionID, boolean getBreakfast )
	{
		if ( scriptRequestor != this )
			scriptRequestor.initialize( loginname, sessionID, getBreakfast );
		else
			super.initialize( loginname, sessionID, getBreakfast );

		outputStream.println();
		executeCommand( "moons", "" );
		outputStream.println();

		listenForCommands();
	}

	/**
	 * A utility method which waits for commands from the user, then
	 * executing each command as it arrives.
	 */

	public void listenForCommands()
	{
		try
		{
			outputStream.print( " > " );
			String line;

			while ( (line = commandStream.readLine()) != null )
			{
				outputStream.println();
				executeLine( line );
				outputStream.println();

				outputStream.print( " > " );
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

	private void executeLine( String line )
	{
		if ( line.trim().length() == 0 )
			return;

		String command = line.split( " " )[0].toLowerCase().trim();
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
		// First, handle any requests to login or relogin.
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
				updateDisplay( KoLFrame.DISABLED_STATE, "Logging out..." );
				(new LogoutRequest( scriptRequestor )).run();
			}

			updateDisplay( KoLFrame.DISABLED_STATE, "Exiting KoLmafia..." );
			System.exit(0);
		}

		// Next, handle any requests for script execution;
		// these can be done at any time (including before
		// login), so they should be handled before a test
		// of login state needed for other commands.

		if ( command.equals( "call" ) || command.equals( "run" ) || command.equals( "exec" ) || command.equals( "load" ) )
		{
			try
			{
				(new KoLmafiaCLI( this, parameters )).listenForCommands();
				return;
			}
			catch ( IOException e )
			{
				// Print a message indicating that the file failed to
				// be loaded, since that's what the error probably was.

				updateDisplay( KoLFrame.ENABLED_STATE, "Script file <" + parameters + "> could not be found." );
				return;
			}
		}

		// Next, handle repeat commands - these are a
		// carry-over feature which made sense in CLI.

		if ( command.equals( "repeat" ) )
		{
			try
			{
				int repeatCount = parameters.length() == 0 ? 1 : df.parse( parameters ).intValue();
				for ( int i = 0; i < repeatCount; ++i )
					executeLine( previousCommand );

				return;
			}
			catch ( Exception e )
			{
				// Notify the client that the command could not be repeated
				// the given number of times.

				updateDisplay( KoLFrame.ENABLED_STATE, parameters + " is not a number." );
				return;
			}
		}

		// Next, print out the moon phase, if the user
		// wishes to know what moon phase it is.

		if ( command.startsWith( "moon" ) )
		{
			outputStream.println( "Ronald: " + MoonPhaseDatabase.getRonaldMoonPhase() );
			outputStream.println( "Grimace: " + MoonPhaseDatabase.getGrimaceMoonPhase() );
			outputStream.println();
			updateDisplay( KoLFrame.ENABLED_STATE, MoonPhaseDatabase.getMoonEffect() );
			return;
		}

		// If there's any commands which suggest that the
		// client is in a login state, you should not do
		// any commands listed beyond this point

		if ( scriptRequestor.inLoginState() )
		{
			updateDisplay( KoLFrame.ENABLED_STATE, "You have not yet logged in." );
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

		if ( command.equals( "create" ) || command.equals( "make" ) || command.equals( "bake" ) || command.equals( "mix" ) || command.equals( "smith" ) )
		{
			executeItemCreationRequest( parameters );
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
		// your current familiar.

		if ( command.equals( "familiar" ) )
		{
			if ( parameters.startsWith( "list" ) || parameters.length() == 0 )
			{
				executePrintCommand( "familiars " + parameters.substring( 4 ).trim() );
				return;
			}

			(new FamiliarRequest( scriptRequestor, parameters )).run();
			return;
		}

		// Yet another popular command involves changing
		// your outfit.

		if ( command.equals( "outfit" ) )
		{
			if ( parameters.startsWith( "list" ) || parameters.length() == 0 )
			{
				executePrintCommand( "outfits " + parameters.substring( 4 ).trim() );
				return;
			}

			executeChangeOutfitCommand( parameters );
			executePrintCommand( "equip" );
			return;
		}

		// Finally, handle command abbreviations - in
		// other words, commands that look like they're
		// their own commands, but are actually commands
		// which are derived from other commands.

		if ( command.equals( "hermit" ) || command.equals( "gym" ) )
		{
			executeAdventureRequest( command + " " + parameters );
			return;
		}

		if ( command.startsWith( "inv" ) || command.equals( "closet" ) || command.equals( "session" ) ||
			command.equals( "outfits" ) || command.equals( "familiars" ) || command.equals( "summary" ) ||
			command.startsWith( "equip" ) || command.equals( "effects" ) || command.startsWith( "stat" ) )
		{
			executePrintCommand( command + " " + parameters );
			return;
		}

		updateDisplay( KoLFrame.ENABLED_STATE, "Unknown command: " + command );
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
			updateDisplay( KoLFrame.ENABLED_STATE, parameters + " is not a statue." );
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
				updateDisplay( KoLFrame.ENABLED_STATE, parameterList[1] + " is not a number." );
			else
				updateDisplay( KoLFrame.ENABLED_STATE, parameterList[2] + " is not a number." );
			return;
		}

		int amountRemaining = amount;
		int eachAmount = amountRemaining / increments;

		updateDisplay( KoLFrame.DISABLED_STATE, "Donating " + amount + " to the shrine..." );
		makeRequest( new HeroDonationRequest( scriptRequestor, heroID, eachAmount ), increments - 1 );
		amountRemaining -= eachAmount * (increments - 1);

		if ( scriptRequestor.permitsContinue() )
		{
			updateDisplay( KoLFrame.DISABLED_STATE, "Request " + increments + " in progress..." );
			(new HeroDonationRequest( scriptRequestor, heroID, amountRemaining )).run();

			if ( scriptRequestor.permitsContinue() )
				updateDisplay( KoLFrame.ENABLED_STATE, "Requests complete!" );
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
			updateDisplay( KoLFrame.ENABLED_STATE, "Print what?" );
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

				desiredOutputStream = new PrintStream( new FileOutputStream( outputFile ), true );
			}
			catch ( IOException e )
			{
				// Because you created a file, no I/O errors should
				// occur.  However, since there could still be something
				// bad happening, print an error message.

				updateDisplay( KoLFrame.ENABLED_STATE, "I/O error in opening file <" + parameterList[1] + ">" );
				return;
			}
		}
		else
			desiredOutputStream = this.outputStream;

		executePrintCommand( parameterList[0].toLowerCase(), desiredOutputStream );

		if ( parameterList.length != 1 )
			updateDisplay( KoLFrame.ENABLED_STATE, "Data has been printed to <" + parameterList[1] + ">" );
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
		KoLCharacter data = scriptRequestor.getCharacterData();

		if ( desiredData.equals( "session" ) )
		{
			outputStream.println( "Player: " + scriptRequestor.getLoginName() );
			outputStream.println( "Session ID: " + scriptRequestor.getSessionID() );
			outputStream.println( "Password Hash: " + scriptRequestor.getPasswordHash() );
			return;
		}

		if ( desiredData.startsWith( "stat" ) )
		{
			outputStream.println( "HP: " + data.getCurrentHP() + " / " + data.getMaximumHP() );
			outputStream.println( "MP: " + data.getCurrentMP() + " / " + data.getMaximumMP() );
			outputStream.println();
			outputStream.println( "Mus: " + getStatString( data.getBaseMuscle(), data.getAdjustedMuscle(), data.getMuscleTNP() ) );
			outputStream.println( "Mys: " + getStatString( data.getBaseMysticality(), data.getAdjustedMysticality(), data.getMysticalityTNP() ) );
			outputStream.println( "Mox: " + getStatString( data.getBaseMoxie(), data.getAdjustedMoxie(), data.getMoxieTNP() ) );
			outputStream.println();
			outputStream.println( "Meat: " + data.getAvailableMeat() );
			outputStream.println( "Drunk: " + data.getInebriety() );
			outputStream.println( "Adv: " + data.getAdventuresLeft() );
			outputStream.println( "Fam: " + data.getFamiliarRace() );
			return;
		}

		if ( desiredData.startsWith( "equip" ) )
		{
			outputStream.println( "       Hat: " + data.getHat() );
			outputStream.println( "    Weapon: " + data.getWeapon() );
			outputStream.println( "     Pants: " + data.getPants() );
			outputStream.println( " Accessory: " + data.getAccessory1() );
			outputStream.println( " Accessory: " + data.getAccessory2() );
			outputStream.println( " Accessory: " + data.getAccessory3() );
			return;
		}

		if ( desiredData.startsWith( "inv" ) )
		{
			printList( scriptRequestor.getInventory(), outputStream );
			return;
		}

		if ( desiredData.equals( "closet" ) )
		{
			printList( scriptRequestor.getCloset(), outputStream );
			return;
		}

		if ( desiredData.equals( "summary" ) )
		{
			printList( scriptRequestor.tally, outputStream );
			return;
		}

		if ( desiredData.equals( "outfits" ) )
		{
			printList( data.getOutfits(), outputStream );
			return;
		}

		if ( desiredData.equals( "familiars" ) )
		{
			printList( data.getFamiliars(), outputStream );
			return;
		}

		if ( desiredData.equals( "effects" ) )
		{
			printList( data.getEffects(), outputStream );
			return;
		}

		updateDisplay( KoLFrame.ENABLED_STATE, "Unknown data type: " + desiredData );
	}

	private static String getStatString( int base, int adjusted, int tnp )
	{
		StringBuffer statString = new StringBuffer();
		statString.append( adjusted );

		if ( base != adjusted )
			statString.append( " (" + base + ")" );

		statString.append( ", tnp = " );
		statString.append( tnp );

		return statString.toString();
	}

	/**
	 * Utility method which determines the first item which matches
	 * the given parameter string.  Note that the string may also
	 * specify an item quantity before the string.
	 */

	private AdventureResult getFirstMatchingItem( String parameters )
	{
		String itemName;  int itemCount;

		// First, allow for the person to type without specifying
		// the amount, if the amount is 1.

		List matchingNames = TradeableItemDatabase.getMatchingNames( parameters );

		if ( matchingNames.size() != 0 )
		{
			itemName = (String) matchingNames.get(0);
			itemCount = 1;
		}
		else
		{
			String itemCountString = parameters.split( " " )[0];
			matchingNames = TradeableItemDatabase.getMatchingNames( parameters.substring( itemCountString.length() ).trim() );

			if ( matchingNames.size() == 0 )
			{
				updateDisplay( KoLFrame.ENABLED_STATE, "[" + parameters + "] does not match anything in the item database." );
				return null;
			}

			try
			{
				itemName = (String) matchingNames.get(0);
				itemCount = df.parse( itemCountString ).intValue();
			}
			catch ( Exception e )
			{
				// Technically, this exception should not be thrown, but if
				// it is, then print an error message and return.

				updateDisplay( KoLFrame.ENABLED_STATE, itemCountString + " is not a number." );
				return null;
			}
		}

		return new AdventureResult( itemName, itemCount );
	}

	/**
	 * A special module used specifically for properly instantiating
	 * ItemCreationRequests.
	 */

	private void executeItemCreationRequest( String parameters )
	{
		int itemID;  int mixingMethod;  int quantityNeeded;

		AdventureResult firstMatch = getFirstMatchingItem( parameters );
		if ( firstMatch == null )
			return;

		itemID = TradeableItemDatabase.getItemID( firstMatch.getName() );
		mixingMethod = ConcoctionsDatabase.getMixingMethod( itemID );
		quantityNeeded = firstMatch.getCount();

		(new ItemCreationRequest( scriptRequestor, itemID, mixingMethod, quantityNeeded )).run();
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

		AdventureResult firstMatch = getFirstMatchingItem( parameters );
		if ( firstMatch == null )
			return;

		itemName = firstMatch.getName();
		itemCount = firstMatch.getCount();

		consumptionType = TradeableItemDatabase.getConsumptionType( itemName );
		String useTypeAsString = (consumptionType == ConsumeItemRequest.CONSUME_EAT) ? "Eating" :
			(consumptionType == ConsumeItemRequest.CONSUME_DRINK) ? "Drinking" : "Using";

		updateDisplay( KoLFrame.DISABLED_STATE, useTypeAsString + " " + itemCount + " " + itemName + "..." );

		if ( itemCount == 1 || consumptionType == ConsumeItemRequest.CONSUME_MULTIPLE )
			(new ConsumeItemRequest( this, consumptionType, itemName, itemCount )).run();
		else
			makeRequest( new ConsumeItemRequest( scriptRequestor, consumptionType, itemName, 1 ), itemCount );
	}

	/**
	 * A special module used specifically for properly instantiating
	 * AdventureRequests, including HermitRequests, if necessary.
	 */

	private void executeAdventureRequest( String parameters )
	{
		String adventureName;  int adventureCount;

		if ( AdventureDatabase.contains( parameters ) )
		{
			adventureName = parameters;
			adventureCount = 1;
		}
		else
		{
			String adventureCountString = parameters.split( " " )[0];
			adventureName = parameters.substring( adventureCountString.length() ).trim();

			if ( !AdventureDatabase.contains( adventureName ) )
			{
				updateDisplay( KoLFrame.ENABLED_STATE, adventureName + " does not exist in the adventure database." );
				return;
			}

			try
			{
				adventureCount = df.parse( adventureCountString ).intValue();
			}
			catch ( Exception e )
			{
				// Technically, this exception should not be thrown, but if
				// it is, then print an error message and return.

				updateDisplay( KoLFrame.ENABLED_STATE, adventureCountString + " is not a number." );
				return;
			}
		}

		KoLAdventure adventure = AdventureDatabase.getAdventure( scriptRequestor, adventureName );
		updateDisplay( KoLFrame.DISABLED_STATE, "Beginning " + adventureCount + " turnips to " + adventure.toString() + "..." );
		makeRequest( adventure, adventureCount );
	}

	/**
	 * Special module used specifically for properly instantiating
	 * requests to change the user's outfit.
	 */

	private void executeChangeOutfitCommand( String parameters )
	{
		Iterator outfitIterator = characterData.getOutfits().iterator();
		SpecialOutfit intendedOutfit = null;
		SpecialOutfit currentOutfit;

		while ( intendedOutfit == null && outfitIterator.hasNext() )
		{
			currentOutfit = (SpecialOutfit) outfitIterator.next();
			if ( currentOutfit.toString().toLowerCase().indexOf( parameters.toLowerCase() ) != -1 )
				intendedOutfit = currentOutfit;
		}

		if ( intendedOutfit == null )
		{
			updateDisplay( KoLFrame.ENABLED_STATE, "You can't wear that outfit." );
			return;
		}

		(new EquipmentRequest( scriptRequestor, intendedOutfit )).run();
	}

	/**
	 * Updates the currently active display in the <code>KoLmafia</code>
	 * session.
	 */

	public void updateDisplay( int state, String message )
	{
		if ( scriptRequestor != this )
			scriptRequestor.updateDisplay( state, message );
		else
		{
			outputStream.println( message );

			// There's a special case to be handled if the login was not
			// successful - in other words, attempt to prompt the user again

			if ( message.equals( "Login failed." ) )
				attemptLogin();
		}
	}

	/**
	 * This does nothing, since requesting focus for a command line
	 * equates to doing nothing.
	 */

	public void requestFocus()
	{
	}

	/**
	 * Makes a request to the hermit, looking for the given number of
	 * items.  This method should prompt the user to determine which
	 * item to retrieve the hermit, if no default has been specified
	 * in the user settings.
	 *
	 * @param	tradeCount	The number of items to request
	 */

	protected void makeHermitRequest( int tradeCount )
	{
		String item = previousCommand.split( " " )[2];
		int itemNumber = -1;

		for ( int i = 0; itemNumber == -1 && i < hermitItemNames.length; ++i )
			if ( hermitItemNames[i].indexOf( item ) != -1 )
				itemNumber = hermitItemNumbers[i];

		if ( itemNumber != -1 )
			settings.setProperty( "hermitTrade", "" + itemNumber );
		else
			settings.remove( "hermitTrade" );

		settings.saveSettings();
		(new HermitRequest( scriptRequestor, tradeCount )).run();
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

	private static void printList( List printing, PrintStream outputStream )
	{
		Iterator printingIterator = printing.iterator();
		while ( printingIterator.hasNext() )
			outputStream.println( printingIterator.next() );
	}
}
