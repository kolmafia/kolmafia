/**
 * Copyright (c) 2005-2012, KoLmafia development team
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

package net.sourceforge.kolmafia.session;

import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.KoLmafiaCLI;
import net.sourceforge.kolmafia.KoLmafiaGUI;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.SpecialOutfit;
import net.sourceforge.kolmafia.StaticEntity;

import net.sourceforge.kolmafia.chat.ChatManager;

import net.sourceforge.kolmafia.persistence.AdventureDatabase;
import net.sourceforge.kolmafia.persistence.ConcoctionDatabase;
import net.sourceforge.kolmafia.persistence.HolidayDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.request.CampgroundRequest;
import net.sourceforge.kolmafia.request.CharPaneRequest;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.request.HermitRequest;
import net.sourceforge.kolmafia.request.LoginRequest;
import net.sourceforge.kolmafia.request.PasswordHashRequest;

import net.sourceforge.kolmafia.swingui.GenericFrame;

public class LoginManager
{
	public static void login( String username )
	{
		try
		{
			KoLmafia.forceContinue();
			LoginManager.doLogin( username );
		}
		catch ( Exception e )
		{
			// What should we do here?
			StaticEntity.printStackTrace( e, "Error during session initialization" );
		}
	}

	private static void doLogin( String name )
	{
		LoginRequest.isLoggingIn( true );

		try
		{
			ConcoctionDatabase.deferRefresh( true );
			LoginManager.initialize( name );
		}
		finally
		{
			ConcoctionDatabase.deferRefresh( false );
			LoginRequest.isLoggingIn( false );
		}

		// Abort further processing in Valhalla.
		if ( CharPaneRequest.inValhalla() )
		{
			return;
		}

		// Abort further processing if we logged in to a fight or choice
		if ( KoLmafia.isRefreshing() )
		{
			return;
		}

		if ( Preferences.getBoolean( name, "getBreakfast" ) )
		{
			int today = HolidayDatabase.getPhaseStep();
			BreakfastManager.getBreakfast( Preferences.getInteger( "lastBreakfast" ) != today );
			Preferences.setInteger( "lastBreakfast", today );
		}

		if ( Preferences.getBoolean( "sharePriceData" ) )
		{
			KoLmafiaCLI.DEFAULT_SHELL.executeLine( "update prices http://kolmafia.us/scripts/updateprices.php?action=getmap" );
		}

		// Also, do mushrooms, if a mushroom script has already
		// been setup by the user.

		if ( Preferences.getBoolean( "autoPlant" + ( KoLCharacter.canInteract() ? "Softcore" : "Hardcore" ) ) )
		{
			String currentLayout = Preferences.getString( "plantingScript" );
			if ( !currentLayout.equals( "" ) && KoLCharacter.knollAvailable() && !KoLCharacter.inZombiecore() && MushroomManager.ownsPlot() )
			{
				KoLmafiaCLI.DEFAULT_SHELL.executeLine( "call " + KoLConstants.PLOTS_DIRECTORY + currentLayout + ".ash" );
			}
		}

		String scriptSetting = Preferences.getString( "loginScript" );
		if ( !scriptSetting.equals( "" ) )
		{
			KoLmafiaCLI.DEFAULT_SHELL.executeLine( scriptSetting );
		}

		if ( EventManager.hasEvents() )
		{
			KoLmafiaCLI.DEFAULT_SHELL.executeLine( "events" );
		}
	}

	/**
	 * Initializes the <code>KoLmafia</code> session. Called after the
	 * login has been confirmed to notify that the login was successful,
	 * the user-specific settings should be loaded, and the user can begin
	 * adventuring.
	 */

	public static void initialize( final String username )
	{
		// Load the JSON string first, so we can use it, if necessary.
		ActionBarManager.loadJSONString();

		// Initialize the variables to their initial states to avoid
		// null pointers getting thrown all over the place

		// Do this first to reset per-player item aliases
		ItemDatabase.reset();

		KoLCharacter.reset( username );

		// Get rid of cached password hashes in KoLAdventures
		AdventureDatabase.refreshAdventureList();

		// Reset all per-player information

		ChatManager.reset();
		MailManager.clearMailboxes();
		StoreManager.clearCache();
		DisplayCaseManager.clearCache();
		ClanManager.clearCache();

		CampgroundRequest.reset();
		MushroomManager.reset();
		HermitRequest.reset();
		SpecialOutfit.forgetCheckpoints();

		KoLmafia.updateDisplay( "Initializing session for " + username + "..." );
		Preferences.setString( "lastUsername", username );

		// Perform requests to read current character's data

		KoLmafia.refreshSession();

		// Reset the session tally and encounter list

		KoLmafia.resetSession();

		// Open the session log and indicate that we've logged in.

		RequestLogger.openSessionLog();

		if ( Preferences.getBoolean( "logStatusOnLogin" ) )
		{
			KoLmafiaCLI.DEFAULT_SHELL.executeCommand( "log", "snapshot" );
		}

		// If the password hash is non-null, then that means you
		// might be mid-transition.

		if ( GenericRequest.passwordHash.equals( "" ) )
		{
			PasswordHashRequest request = new PasswordHashRequest( "lchat.php" );
			RequestThread.postRequest(  request );
		}

		ContactManager.registerPlayerId( username, String.valueOf( KoLCharacter.getUserId() ) );

		if ( Preferences.getString( "spadingData" ).length() > 10 )
		{
			KoLmafia.updateDisplay( "Some data has been collected that may be of interest " +
				"to others.  Please type `spade' to examine and submit or delete this data." );
		}

		// Rebuild Scripts menu if needed
		GenericFrame.compileScripts();

		if ( StaticEntity.getClient() instanceof KoLmafiaGUI )
		{
			KoLmafiaGUI.intializeMainInterfaces();
		}
		else if ( Preferences.getString( "initialFrames" ).indexOf( "LocalRelayServer" ) != -1 )
		{
			KoLmafiaGUI.constructFrame( "LocalRelayServer" );
		}

		String updateText;

		String holiday = HolidayDatabase.getHoliday( true );
		String moonEffect = HolidayDatabase.getMoonEffect();

		if ( holiday.equals( "" ) )
		{
			updateText = moonEffect;
		}
		else
		{
			updateText = holiday + ", " + moonEffect;
		}

		KoLmafia.updateDisplay( updateText );

		if ( MailManager.hasNewMessages() )
		{
			KoLmafia.updateDisplay( "You have new mail." );
		}
	}
}
