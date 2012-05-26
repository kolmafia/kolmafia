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

package net.sourceforge.kolmafia;

import java.util.ArrayList;

import javax.swing.JTabbedPane;

import net.sourceforge.kolmafia.chat.ChatManager;

import net.sourceforge.kolmafia.persistence.BuffBotDatabase;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.request.CharPaneRequest;
import net.sourceforge.kolmafia.request.ChezSnooteeRequest;
import net.sourceforge.kolmafia.request.ClanStashRequest;
import net.sourceforge.kolmafia.request.ClanWarRequest;
import net.sourceforge.kolmafia.request.ContactListRequest;
import net.sourceforge.kolmafia.request.CrimboCafeRequest;
import net.sourceforge.kolmafia.request.DisplayCaseRequest;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.request.HellKitchenRequest;
import net.sourceforge.kolmafia.request.LoginRequest;
import net.sourceforge.kolmafia.request.ManageStoreRequest;
import net.sourceforge.kolmafia.request.MicroBreweryRequest;
import net.sourceforge.kolmafia.request.PvpRequest;

import net.sourceforge.kolmafia.session.BuffBotManager;
import net.sourceforge.kolmafia.session.ClanManager;
import net.sourceforge.kolmafia.session.ContactManager;
import net.sourceforge.kolmafia.session.DisplayCaseManager;
import net.sourceforge.kolmafia.session.MushroomManager;
import net.sourceforge.kolmafia.session.StoreManager;

import net.sourceforge.kolmafia.swingui.BuffBotFrame;
import net.sourceforge.kolmafia.swingui.BuffRequestFrame;
import net.sourceforge.kolmafia.swingui.CakeArenaFrame;
import net.sourceforge.kolmafia.swingui.CalendarFrame;
import net.sourceforge.kolmafia.swingui.ClanManageFrame;
import net.sourceforge.kolmafia.swingui.ContactListFrame;
import net.sourceforge.kolmafia.swingui.DescriptionFrame;
import net.sourceforge.kolmafia.swingui.FamiliarTrainingFrame;
import net.sourceforge.kolmafia.swingui.ItemManageFrame;
import net.sourceforge.kolmafia.swingui.LoginFrame;
import net.sourceforge.kolmafia.swingui.MuseumFrame;
import net.sourceforge.kolmafia.swingui.MushroomFrame;
import net.sourceforge.kolmafia.swingui.OptionsFrame;
import net.sourceforge.kolmafia.swingui.SendMessageFrame;
import net.sourceforge.kolmafia.swingui.SkillBuffFrame;
import net.sourceforge.kolmafia.swingui.StoreManageFrame;
import net.sourceforge.kolmafia.swingui.SystemTrayFrame;

import net.sourceforge.kolmafia.utilities.FileUtilities;

import net.sourceforge.kolmafia.webui.RelayLoader;
import net.sourceforge.kolmafia.webui.RelayServer;

import tab.CloseTabbedPane;

public class KoLmafiaGUI
	extends KoLmafia
{
	/**
	 * The main method. Currently, it instantiates a single instance of the <code>KoLmafia</code>after setting the
	 * default look and feel of all <code>JFrame</code> objects to decorated.
	 */

	public static final void initialize()
	{
		KoLmafiaGUI session = new KoLmafiaGUI();
		StaticEntity.setClient( session );

		KoLmafiaGUI.initializeLoginInterface();

		// All that completed, check to see if there is an auto-login
		// which should occur.

		String autoLogin = Preferences.getString( "autoLogin" );
		if ( !autoLogin.equals( "" ) )
		{
			// Make sure that a password was stored for this
			// character (would fail otherwise):

			String password = KoLmafia.getSaveState( autoLogin );
			if ( password != null && !password.equals( "" ) )
			{
				RequestThread.postRequest( new LoginRequest( autoLogin, password ) );
			}
		}
	}

	public static final void checkFrameSettings()
	{
		String frameSetting = Preferences.getString( "initialFrames" );
		String desktopSetting = Preferences.getString( "initialDesktop" );

		// If there is still no data (somehow the global data
		// got emptied), default to relay-browser only).

		if ( desktopSetting.equals( "" ) && frameSetting.equals( "" ) )
		{
			Preferences.setString( "initialDesktop", "AdventureFrame,CommandDisplayFrame,GearChangeFrame" );
		}
	}

	public static void initializeLoginInterface()
	{
		KoLmafiaGUI.constructFrame( LoginFrame.class );

		if ( Preferences.getString( "useDecoratedTabs" ).equals( "" ) )
		{
			Preferences.setBoolean(
				"useDecoratedTabs", !System.getProperty( "os.name" ).startsWith( "Mac" ) );
		}

		if ( !Preferences.getBoolean( "customizedTabs" ) )
		{
			KoLmafiaGUI.constructFrame( OptionsFrame.class );
			Preferences.setBoolean( "customizedTabs", true );
		}
	}

	public static void intializeMainInterfaces()
	{
		LoginFrame.hideInstance();

		KoLmafiaGUI.checkFrameSettings();
		String frameSetting = Preferences.getString( "initialFrames" );
		String desktopSetting = Preferences.getString( "initialDesktop" );

		// Reset all the titles on all existing frames.

		SystemTrayFrame.updateToolTip();
		KoLDesktop.updateTitle();

		// Instantiate the appropriate instance of the
		// frame that should be loaded based on the mode.

		if ( !desktopSetting.equals( "" ) )
		{
			if ( !Preferences.getBoolean( "relayBrowserOnly" ) )
			{
				KoLDesktop.getInstance().setVisible( true );
			}
		}

		String[] frameArray = frameSetting.split( "," );
		String[] desktopArray = desktopSetting.split( "," );

		ArrayList initialFrameList = new ArrayList();

		if ( !frameSetting.equals( "" ) )
		{
			for ( int i = 0; i < frameArray.length; ++i )
			{
				if ( !initialFrameList.contains( frameArray[ i ] ) )
				{
					initialFrameList.add( frameArray[ i ] );
				}
			}
		}

		for ( int i = 0; i < desktopArray.length; ++i )
		{
			initialFrameList.remove( desktopArray[ i ] );
		}

		if ( !initialFrameList.isEmpty() && !Preferences.getBoolean( "relayBrowserOnly" ) )
		{
			String[] initialFrames = new String[ initialFrameList.size() ];
			initialFrameList.toArray( initialFrames );

			for ( int i = 0; i < initialFrames.length; ++i )
			{
				KoLmafiaGUI.constructFrame( initialFrames[ i ] );
			}
		}

		// Figure out which user interface is being
		// used -- account for minimalist loadings.

		LoginFrame.disposeInstance();
	}

	public static final void constructFrame( final String frameName )
	{
		if ( frameName.equals( "" ) )
		{
			return;
		}

		if ( frameName.equals( "ChatManager" ) )
		{
			KoLmafia.updateDisplay( "Initializing chat interface..." );
			ChatManager.initialize();

			return;
		}

		if ( frameName.equals( "LocalRelayServer" ) )
		{
			RelayLoader.openRelayBrowser();
			return;
		}

		try
		{
			Class frameClass = Class.forName( "net.sourceforge.kolmafia.swingui." + frameName );
			KoLmafiaGUI.constructFrame( frameClass );
		}
		catch ( ClassNotFoundException e )
		{
			// Can happen if preference file made by an earlier
			// version of KoLmafia and the frame has been renamed.

			// We don't need a full stack trace, but an informative
			// message would be nice.
		}
		catch ( Exception e )
		{
			// Should not happen.  Therefore, print
			// a stack trace for debug purposes.

			StaticEntity.printStackTrace( e );
		}
	}

	public static final void constructFrame( final Class frameClass )
	{
		// Now, test to see if any requests need to be run before
		// you fall into the event dispatch thread.

		if ( frameClass == BuffBotFrame.class )
		{
			BuffBotManager.loadSettings();
		}
		else if ( frameClass == BuffRequestFrame.class )
		{
			if ( !BuffBotDatabase.hasOfferings() )
			{
				KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "No buffs found to purchase." );
				return;
			}
		}
		else if ( frameClass == CakeArenaFrame.class )
		{
			if ( CakeArenaManager.getOpponentList().isEmpty() )
			{
				KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "Equip a familiar first." );
				return;
			}
		}
		else if ( frameClass == CalendarFrame.class )
		{
			String base = "http://images.kingdomofloathing.com/otherimages/bikini/";
			for ( int i = 1; i < CalendarFrame.CALENDARS.length; ++i )
			{
				FileUtilities.downloadImage( base + CalendarFrame.CALENDARS[ i ] + ".gif" );
			}
			base = "http://images.kingdomofloathing.com/otherimages/beefcake/";
			for ( int i = 1; i < CalendarFrame.CALENDARS.length; ++i )
			{
				FileUtilities.downloadImage( base + CalendarFrame.CALENDARS[ i ] + ".gif" );
			}
		}
		else if ( frameClass == ClanManageFrame.class )
		{
			if ( Preferences.getBoolean( "clanAttacksEnabled" ) )
			{
				RequestThread.postRequest( new ClanWarRequest() );
			}

			if ( Preferences.getBoolean( "autoSatisfyWithStash" ) && ClanManager.getStash().isEmpty() )
			{
				KoLmafia.updateDisplay( "Retrieving clan stash contents..." );
				RequestThread.postRequest( new ClanStashRequest() );
			}
		}
		else if ( frameClass == ContactListFrame.class )
		{
			if ( ContactManager.getMailContacts().isEmpty() && !CharPaneRequest.inValhalla() )
			{
				RequestThread.postRequest( new ContactListRequest() );
			}
		}
		else if ( frameClass == FamiliarTrainingFrame.class )
		{
			if ( CakeArenaManager.getOpponentList().isEmpty() )
			{
				KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "Equip a familiar first." );
				return;
			}
		}
		else if ( frameClass == ItemManageFrame.class )
		{
			// The Crimbo Cafe is open

			if ( KoLConstants.cafeItems.isEmpty() )
			{
				CrimboCafeRequest.getMenu();
			}

			// If the person is in Bad Moon, retrieve
			// information from Hell's Kitchen.

			if ( KoLCharacter.inBadMoon() )
			{
				if ( KoLConstants.kitchenItems.isEmpty() )
				{
					HellKitchenRequest.getMenu();
				}
			}

			// If the person is in a mysticality sign, make
			// sure you retrieve information from the
			// restaurant.

			if ( KoLCharacter.canEat() && KoLCharacter.canadiaAvailable() )
			{
				if ( KoLConstants.restaurantItems.isEmpty() )
				{
					ChezSnooteeRequest.getMenu();
				}
			}

			// If the person is in a moxie sign and they
			// have completed the beach quest, then
			// retrieve information from the microbrewery.

			if ( KoLCharacter.canDrink() && KoLCharacter.gnomadsAvailable() && KoLConstants.microbreweryItems.isEmpty() )
			{
				GenericRequest beachCheck = new GenericRequest( "main.php" );
				RequestThread.postRequest( beachCheck );

				if ( beachCheck.responseText.indexOf( "beach.php" ) != -1 )
				{
					MicroBreweryRequest.getMenu();
				}
			}

			if ( Preferences.getBoolean( "autoSatisfyWithStash" ) && KoLCharacter.canInteract() && KoLCharacter.hasClan() )
			{
				if ( !ClanManager.isStashRetrieved() )
				{
					RequestThread.postRequest( new ClanStashRequest() );
				}
			}

		}
		else if ( frameClass == RelayServer.class )
		{
			RelayLoader.openRelayBrowser();
			return;
		}
		else if ( frameClass == MuseumFrame.class )
		{
			if ( CharPaneRequest.inValhalla() )
			{
				KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "You can't find your display case in Valhalla." );
				return;
			}

			if ( !KoLCharacter.hasDisplayCase() )
			{
				KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "Sorry, you don't have a display case." );
				return;
			}

			if ( DisplayCaseManager.getHeaders().isEmpty() )
			{
				RequestThread.postRequest( new DisplayCaseRequest() );
			}
		}
		else if ( frameClass == MushroomFrame.class )
		{
			for ( int i = 0; i < MushroomManager.MUSHROOMS.length; ++i )
			{
				FileUtilities.downloadImage( "http://images.kingdomofloathing.com/itemimages/" + MushroomManager.MUSHROOMS[ i ][ 1 ] );
			}

			MushroomManager.initialize();
		}
		else if ( frameClass == SendMessageFrame.class )
		{
			if ( ContactManager.getMailContacts().isEmpty() && !CharPaneRequest.inValhalla() )
			{
				RequestThread.postRequest( new ContactListRequest() );
			}
		}
		else if ( frameClass == SkillBuffFrame.class )
		{
			if ( ContactManager.getMailContacts().isEmpty() && !CharPaneRequest.inValhalla() )
			{
				RequestThread.postRequest( new ContactListRequest() );
			}
		}
		else if ( frameClass == StoreManageFrame.class )
		{
			if ( !KoLCharacter.hasStore() )
			{
				KoLmafia.updateDisplay( "You don't own a store in the Mall of Loathing." );
				return;
			}

			StoreManager.clearCache();
			RequestThread.postRequest( new ManageStoreRequest( false ) );
		}

		( new CreateFrameRunnable( frameClass ) ).run();
	}

	public void showHTML( final String location, final String text )
	{
		GenericRequest request = new GenericRequest( location );
		request.responseText = text;
		DescriptionFrame.showRequest( request );
	}

	public static JTabbedPane getTabbedPane()
	{
		return Preferences.getBoolean( "useDecoratedTabs" ) ? new CloseTabbedPane() : new JTabbedPane();
	}
}
