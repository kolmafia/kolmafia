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

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import javax.swing.JOptionPane;

import net.java.dev.spellcast.utilities.LockableListModel;
import net.java.dev.spellcast.utilities.SortedListModel;
import net.java.dev.spellcast.utilities.UtilityConstants;

/**
 * The main class for the <code>KoLmafia</code> package.  This
 * class encapsulates most of the data relevant to any given
 * session of <code>Kingdom of Loathing</code> and currently
 * functions as the blackboard in the architecture.  When data
 * listeners are implemented, it will continue to manage most
 * of the interactions.
 */

public class KoLmafiaGUI extends KoLmafia implements UtilityConstants
{
	private KoLFrame activeFrame;

	/**
	 * The main method.  Currently, it instantiates a single instance
	 * of the <code>KoLmafia</code> client after setting the default
	 * look and feel of all <code>JFrame</code> objects to decorated.
	 */

	public static void main( String [] args )
	{
		javax.swing.JFrame.setDefaultLookAndFeelDecorated( true );
    	KoLmafiaGUI session = new KoLmafiaGUI();
	}

	/**
	 * Constructs a new <code>KoLmafia</code> object.  All data fields
	 * are initialized to their default values, the global settings
	 * are loaded from disk, and a <code>LoginFrame</code> is created
	 * to allow the user to login.
	 */

	public KoLmafiaGUI()
	{
		activeFrame = new LoginFrame( this );
		activeFrame.pack();  activeFrame.setVisible( true );
		activeFrame.requestFocus();
	}

	/**
	 * Updates the currently active display in the <code>KoLmafia</code>
	 * session.
	 */

	public void updateDisplay( int state, String message )
	{	activeFrame.updateDisplay( state, message );
	}

	public void requestFocus()
	{	activeFrame.requestFocus();
	}

	/**
	 * Initializes the <code>KoLmafia</code> session.  Called after
	 * the login has been confirmed to notify the client that the
	 * login was successful, the user-specific settings should be
	 * loaded, and the user can begin adventuring.
	 */

	public void initialize( String loginname, String sessionID )
	{
		super.initialize( loginname, sessionID );

		activeFrame.setVisible( false );
		activeFrame.dispose();
		activeFrame = null;

		activeFrame = new AdventureFrame( this, AdventureDatabase.getAsLockableListModel( this ), tally );
		activeFrame.pack();  activeFrame.setVisible( true );
		activeFrame.updateDisplay( KoLFrame.ENABLED_STATE, MoonPhaseDatabase.getMoonEffect() );
		activeFrame.requestFocus();
	}

	/**
	 * Deinitializes the <code>KoLmafia</code> session.  Called after
	 * the user has logged out.
	 */

	public void deinitialize()
	{
		super.deinitialize();
		activeFrame = null;
	}

	/**
	 * Makes the given request for the given number of iterations,
	 * or until continues are no longer possible, either through
	 * user cancellation or something occuring which prevents the
	 * requests from resuming.  Because this method does not create
	 * new threads, any GUI invoking this method should create a
	 * separate thread for calling it.
	 *
	 * @param	request	The request made by the user
	 * @param	iterations	The number of times the request should be repeated
	 */

	public void makeRequest( Runnable request, int iterations )
	{
		try
		{
			this.permitContinue = true;
			int iterationsRemaining = iterations;

			if ( request.toString().equals( "The Hermitage" ) )
			{
				// Prompt the user to select which item they want from the hermit
				// because it's more intuitive this way.

				Object selectedValue = JOptionPane.showInputDialog(
					null, "I want this from the hermit...", "Hermit Trade!", JOptionPane.INFORMATION_MESSAGE, null,
					hermitItemNames, hermitItemNames[0] );

				int selected = -1;
				for ( int i = 0; i < hermitItemNames.length; ++i )
				{
					if ( selectedValue.equals( hermitItemNames[i] ) )
					{
						settings.setProperty( "hermitTrade", "" + selected );
						settings.saveSettings();
						break;
					}
				}

				updateDisplay( KoLFrame.DISABLED_STATE, "Robbing the hermit..." );
				(new HermitRequest( this, iterations )).run();

				if ( permitContinue )
					updateDisplay( KoLFrame.ENABLED_STATE, "Hermit successfully looted!" );
			}
			else if ( request.toString().startsWith( "Gym" ) )
			{
				updateDisplay( KoLFrame.DISABLED_STATE, "Beginning workout..." );
				(new ClanGymRequest( this, Integer.parseInt( ((KoLAdventure)request).getAdventureID() ), iterations )).run();
				updateDisplay( KoLFrame.ENABLED_STATE, "Workout completed." );
			}
			else
			{
				if ( request instanceof KoLAdventure && request.toString().indexOf( "Campground" ) == -1 && characterData.getInebriety() > 19 )
					permitContinue = JOptionPane.YES_OPTION == JOptionPane.showConfirmDialog( null,
						"The mafia has stolen your shoes!  Continue adventuring anyway?",
						"You're not drunk!  You see flying penguins and a dancing hermit!", JOptionPane.YES_NO_OPTION );

				for ( int i = 1; permitContinue && iterationsRemaining > 0; ++i )
				{
					updateDisplay( KoLFrame.DISABLED_STATE, "Request " + i + " in progress..." );
					request.run();

					// Make sure you only decrement iterations if the
					// continue was permitted.  This resolves the issue
					// of incorrectly updating the client if something
					// occurred on the last iteration.

					if ( permitContinue )
						--iterationsRemaining;
				}

				if ( permitContinue && iterationsRemaining <= 0 )
					updateDisplay( KoLFrame.ENABLED_STATE, "Requests completed!" );
			}
		}
		catch ( RuntimeException e )
		{
			// In the event that an exception occurs during the
			// request processing, catch it here, print it to
			// the logger (whatever it may be), and notify the
			// user that an error was encountered.

			logStream.println( e );
			updateDisplay( KoLFrame.ENABLED_STATE, "Unexpected error." );
		}

		this.permitContinue = true;
	}
}
