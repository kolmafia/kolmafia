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
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.FileInputStream;
import java.io.PrintStream;
import java.io.IOException;

// utility imports
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
	protected static final DecimalFormat df = new DecimalFormat();

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
			KoLmafiaCLI session = new KoLmafiaCLI( null, null );
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

		outputStream = scriptLocation == null ? System.out : new NullStream();
		commandStream = new BufferedReader( new InputStreamReader( inputStream ) );
		this.scriptRequestor = (scriptRequestor == null) ? this : scriptRequestor;

		if ( scriptRequestor instanceof KoLmafiaGUI )
		{
			listenForCommands();
		}
		else
		{
			outputStream.println();
			outputStream.println( "****************" );
			outputStream.println( "* KoLmafia CLI *" );
			outputStream.println( "****************" );
		}
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
			if ( username == null )
				return;

			outputStream.print( "password: " );
			String password = commandStream.readLine();

			if ( password == null )
				return;

			outputStream.println();

			outputStream.println( "Determining server..." );
			KoLRequest.applySettings();
			outputStream.println( KoLRequest.getRootHostName() + " selected." );

			outputStream.println();
			(new LoginRequest( scriptRequestor, username, password, true )).run();
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
		super.initialize( loginname, sessionID, getBreakfast );
		outputStream.println();
		listenForCommands();
	}

	/**
	 * A utility method which waits for commands from the user, then
	 * executing each command as it arrives.
	 */

	private void listenForCommands()
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
		executeCommand( command, parameters );
	}

	/**
	 * A utility command which decides, based on the command
	 * to be executed, what to be done with it.  It can either
	 * delegate this to other functions, or do it itself.
	 */

	private void executeCommand( String command, String parameters )
	{
		if ( command.equals( "exit" ) || command.equals( "quit" ) || command.equals( "logout" ) )
		{
			outputStream.println( "Exiting KoLmafia..." );
			System.exit(0);
		}

		if ( command.equals( "eat" ) || command.equals( "drink" ) || command.equals( "use" ) )
		{
			executeConsumeItemRequest( parameters );
			return;
		}
	}

	/**
	 * A special module used specifically for properly instantiating
	 * ConsumeItemRequests.
	 */

	private void executeConsumeItemRequest( String parameters )
	{
		int consumptionType;  String itemName;  int itemCount;

		// First, allow for the person to type without specifying
		// the amount, if the amount is 1.

		if ( TradeableItemDatabase.contains( parameters ) )
		{
			itemName = parameters;
			itemCount = 1;
		}

		// Now, handle the instance where the first item is actually
		// the quantity desired, and the next is the amount to use

		else
		{
			String itemCountString = parameters.split( " " )[0];
			itemName = parameters.substring( itemCountString.length() ).trim();

			if ( !TradeableItemDatabase.contains( itemName ) )
			{
				outputStream.println( itemName + " does not exist in the item database." );
				return;
			}

			try
			{
				itemCount = df.parse( itemCountString ).intValue();
			}
			catch ( Exception e )
			{
				// Technically, this exception should not be thrown, but if
				// it is, then print an error message and return.

				outputStream.println( itemCountString + " is not a number." );
				return;
			}
		}

		consumptionType = TradeableItemDatabase.getConsumptionType( itemName );
		String useTypeAsString = (consumptionType == ConsumeItemRequest.CONSUME_EAT) ? "Eating" :
			(consumptionType == ConsumeItemRequest.CONSUME_DRINK) ? "Drinking" : "Using";

		updateDisplay( 0, useTypeAsString + " " + itemCount + " " + itemName + "..." );

		if ( itemCount == 1 || consumptionType == ConsumeItemRequest.CONSUME_MULTIPLE )
			(new ConsumeItemRequest( this, consumptionType, itemName, itemCount )).run();
		else
			makeRequest( new ConsumeItemRequest( scriptRequestor, consumptionType, itemName, 1 ), itemCount );
	}

	/**
	 * Updates the currently active display in the <code>KoLmafia</code>
	 * session.
	 */

	public void updateDisplay( int state, String message )
	{
		if ( scriptRequestor instanceof KoLmafiaGUI )
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
	 * @param	itemCount	The number of items to request
	 */

	protected void makeHermitRequest( int itemCount )
	{
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
}
