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

import java.util.ArrayList;
import java.util.Date;

import javax.swing.SwingUtilities;

import net.sourceforge.foxtrot.ConcurrentWorker;
import net.sourceforge.foxtrot.Job;

import net.sourceforge.kolmafia.session.BuffBotManager;
import net.sourceforge.kolmafia.session.ChatManager;
import net.sourceforge.kolmafia.session.ClanManager;
import net.sourceforge.kolmafia.session.DisplayCaseManager;
import net.sourceforge.kolmafia.session.MailManager;
import net.sourceforge.kolmafia.session.MushroomManager;
import net.sourceforge.kolmafia.session.StoreManager;

import net.sourceforge.kolmafia.request.ChezSnooteeRequest;
import net.sourceforge.kolmafia.request.ClanStashRequest;
import net.sourceforge.kolmafia.request.ClanWarRequest;
import net.sourceforge.kolmafia.request.ContactListRequest;
import net.sourceforge.kolmafia.request.DisplayCaseRequest;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.request.HellKitchenRequest;
import net.sourceforge.kolmafia.request.LoginRequest;
import net.sourceforge.kolmafia.request.MailboxRequest;
import net.sourceforge.kolmafia.request.ManageStoreRequest;
import net.sourceforge.kolmafia.request.MicroBreweryRequest;
import net.sourceforge.kolmafia.request.PvpRequest;

import net.sourceforge.kolmafia.persistence.BuffBotDatabase;
import net.sourceforge.kolmafia.persistence.HolidayDatabase;

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

		KoLmafiaGUI.constructFrame( LoginFrame.class );

		if ( KoLSettings.getUserProperty( "useDecoratedTabs" ).equals( "" ) )
		{
			KoLSettings.setUserProperty(
				"useDecoratedTabs", String.valueOf( !System.getProperty( "os.name" ).startsWith( "Mac" ) ) );
		}

		if ( !KoLSettings.getBooleanProperty( "customizedTabs" ) )
		{
			KoLmafiaGUI.constructFrame( OptionsFrame.class );
			KoLSettings.setUserProperty( "customizedTabs", "true" );
		}

		// All that completed, check to see if there is an auto-login
		// which should occur.

		String autoLogin = KoLSettings.getUserProperty( "autoLogin" );
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
		String frameSetting = KoLSettings.getUserProperty( "initialFrames" );
		String desktopSetting = KoLSettings.getUserProperty( "initialDesktop" );

		// If there is still no data (somehow the global data
		// got emptied), default to relay-browser only).

		if ( desktopSetting.equals( "" ) )
		{
			KoLSettings.setUserProperty( "initialDesktop", "AdventureFrame,CommandDisplayFrame,GearChangeFrame" );
		}
	}

	/**
	 * Initializes the <code>KoLmafia</code> session. Called after the login has been confirmed to notify thethat the
	 * login was successful, the user-specific settings should be loaded, and the user can begin adventuring.
	 */

	public void initialize( final String username )
	{
		super.initialize( username );

		if ( GenericRequest.passwordHash != null )
		{
			if ( KoLSettings.getBooleanProperty( "retrieveContacts" ) )
			{
				RequestThread.postRequest( new ContactListRequest() );
				KoLSettings.setUserProperty( "retrieveContacts", String.valueOf( !KoLConstants.contactList.isEmpty() ) );
			}
		}

		LoginFrame.hideInstance();

		if ( StaticEntity.getExistingFrames().length > 0 )
		{
			LoginFrame.disposeInstance();
			return;
		}

		KoLmafiaGUI.checkFrameSettings();
		String frameSetting = KoLSettings.getUserProperty( "initialFrames" );
		String desktopSetting = KoLSettings.getUserProperty( "initialDesktop" );

		// Reset all the titles on all existing frames.

		SystemTrayFrame.updateToolTip();
		KoLDesktop.updateTitle();

		// Instantiate the appropriate instance of the
		// frame that should be loaded based on the mode.

		if ( !desktopSetting.equals( "" ) )
		{
			KoLDesktop.getInstance().initializeTabs();
			if ( !KoLSettings.getBooleanProperty( "relayBrowserOnly" ) )
			{
				KoLDesktop.displayDesktop();
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

		if ( !initialFrameList.isEmpty() && !KoLSettings.getBooleanProperty( "relayBrowserOnly" ) )
		{
			String[] initialFrames = new String[ initialFrameList.size() ];
			initialFrameList.toArray( initialFrames );

			for ( int i = 0; i < initialFrames.length; ++i )
			{
				if ( !initialFrames[ i ].equals( "EventsFrame" ) || !KoLConstants.eventHistory.isEmpty() )
				{
					KoLmafiaGUI.constructFrame( initialFrames[ i ] );
				}
			}
		}

		// Figure out which user interface is being
		// used -- account for minimalist loadings.

		LoginFrame.disposeInstance();

		if ( MailManager.hasNewMessages() )
		{
			KoLmafia.updateDisplay( "You have new mail." );
		}
		else
		{
			try
			{
				String holiday =
					HolidayDatabase.getHoliday(
						KoLConstants.DAILY_FORMAT.parse( KoLConstants.DAILY_FORMAT.format( new Date() ) ), true );
				KoLmafia.updateDisplay( KoLDatabase.getDisplayName( holiday + ", " + HolidayDatabase.getMoonEffect() ) );
			}
			catch ( Exception e )
			{
				// Should not happen, you're parsing something that
				// was formatted the same way.

				StaticEntity.printStackTrace( e );
			}
		}
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
			RequestThread.enableDisplayIfSequenceComplete();

			return;
		}

		try
		{
			Class frameClass = Class.forName( "net.sourceforge.kolmafia." + frameName );
			KoLmafiaGUI.constructFrame( frameClass );
		}
		catch ( Exception e )
		{
			//should not happen.  Therefore, print
			// a stack trace for debug purposes.

			StaticEntity.printStackTrace( e );
		}
	}

	public static final void constructFrame( final Class frameClass )
	{
		try
		{
			FrameConstructor maker = new FrameConstructor( frameClass );

			if ( SwingUtilities.isEventDispatchThread() )
			{
				ConcurrentWorker.post( maker );
			}
			else
			{
				maker.run();
			}
		}
		catch ( Exception e )
		{
			// Should not happen.  Therefore, print
			// a stack trace for debug purposes.

			StaticEntity.printStackTrace( e );
		}
	}

	private static class FrameConstructor
		extends Job
	{
		public Class frameClass;

		public FrameConstructor( final Class frameClass )
		{
			this.frameClass = frameClass;
		}

		public void run()
		{
			// Now, test to see if any requests need to be run before
			// you fall into the event dispatch thread.

			if ( this.frameClass == BuffBotFrame.class )
			{
				BuffBotManager.loadSettings();
			}
			else if ( this.frameClass == BuffRequestFrame.class )
			{
				if ( !BuffBotDatabase.hasOfferings() )
				{
					KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "No buffs found to purchase." );
					RequestThread.enableDisplayIfSequenceComplete();
					return;
				}
			}
			else if ( this.frameClass == CakeArenaFrame.class )
			{
				if ( CakeArenaManager.getOpponentList().isEmpty() )
				{
					KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "Equip a familiar first." );
					RequestThread.enableDisplayIfSequenceComplete();
					return;
				}
			}
			else if ( this.frameClass == CalendarFrame.class )
			{
				String base = "http://images.kingdomofloathing.com/otherimages/bikini/";
				for ( int i = 1; i < CalendarFrame.CALENDARS.length; ++i )
				{
					RequestEditorKit.downloadImage( base + CalendarFrame.CALENDARS[ i ] + ".gif" );
				}
				base = "http://images.kingdomofloathing.com/otherimages/beefcake/";
				for ( int i = 1; i < CalendarFrame.CALENDARS.length; ++i )
				{
					RequestEditorKit.downloadImage( base + CalendarFrame.CALENDARS[ i ] + ".gif" );
				}
			}
			else if ( this.frameClass == ClanManageFrame.class )
			{
				if ( KoLSettings.getBooleanProperty( "clanAttacksEnabled" ) )
				{
					RequestThread.postRequest( new ClanWarRequest() );
				}

				if ( KoLSettings.getBooleanProperty( "autoSatisfyWithStash" ) && ClanManager.getStash().isEmpty() )
				{
					KoLmafia.updateDisplay( "Retrieving clan stash contents..." );
					RequestThread.postRequest( new ClanStashRequest() );
				}
			}
			else if ( this.frameClass == ContactListFrame.class )
			{
				if ( KoLConstants.contactList.isEmpty() )
				{
					RequestThread.postRequest( new ContactListRequest() );
				}

				if ( KoLFrame.appearsInTab( "ContactListFrame" ) )
				{
					return;
				}
			}
			else if ( this.frameClass == FamiliarTrainingFrame.class )
			{
				if ( CakeArenaManager.getOpponentList().isEmpty() )
				{
					KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "Equip a familiar first." );
					RequestThread.enableDisplayIfSequenceComplete();
					return;
				}
			}
			else if ( this.frameClass == FlowerHunterFrame.class )
			{
				KoLmafia.updateDisplay( "Determining number of attacks remaining..." );
				RequestThread.postRequest( new PvpRequest() );

				if ( KoLmafia.refusesContinue() )
				{
					return;
				}
			}
			else if ( this.frameClass == ItemManageFrame.class )
			{
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

				if ( KoLCharacter.canEat() && KoLCharacter.inMysticalitySign() )
				{
					if ( KoLConstants.restaurantItems.isEmpty() )
					{
						ChezSnooteeRequest.getMenu();
					}
				}

				// If the person is in a moxie sign and they
				// have completed the beach quest, then
				// retrieve information from the microbrewery.

				if ( KoLCharacter.canDrink() && KoLCharacter.inMoxieSign() && KoLConstants.microbreweryItems.isEmpty() )
				{
					GenericRequest beachCheck = new GenericRequest( "main.php" );
					RequestThread.postRequest( beachCheck );

					if ( beachCheck.responseText.indexOf( "beach.php" ) != -1 )
					{
						MicroBreweryRequest.getMenu();
					}
				}

				if ( KoLSettings.getBooleanProperty( "autoSatisfyWithStash" ) && KoLCharacter.canInteract() && KoLCharacter.hasClan() )
				{
					if ( !ClanManager.isStashRetrieved() )
					{
						RequestThread.postRequest( new ClanStashRequest() );
					}
				}

			}
			else if ( this.frameClass == LocalRelayServer.class )
			{
				StaticEntity.getClient().openRelayBrowser();
				return;
			}
			else if ( this.frameClass == MailboxFrame.class )
			{
				RequestThread.postRequest( new MailboxRequest( "Inbox" ) );
				if ( LoginRequest.isInstanceRunning() )
				{
					return;
				}
			}
			else if ( this.frameClass == MuseumFrame.class )
			{
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
			else if ( this.frameClass == MushroomFrame.class )
			{
				for ( int i = 0; i < MushroomManager.MUSHROOMS.length; ++i )
				{
					RequestEditorKit.downloadImage( "http://images.kingdomofloathing.com/itemimages/" + MushroomManager.MUSHROOMS[ i ][ 1 ] );
				}
			}
			else if ( this.frameClass == StoreManageFrame.class )
			{
				if ( !KoLCharacter.hasStore() )
				{
					KoLmafia.updateDisplay( "You don't own a store in the Mall of Loathing." );
					RequestThread.enableDisplayIfSequenceComplete();
					return;
				}

				RequestThread.openRequestSequence();

				StoreManager.clearCache();
				RequestThread.postRequest( new ManageStoreRequest( false ) );

				RequestThread.closeRequestSequence();
			}

			( new CreateFrameRunnable( this.frameClass ) ).run();
		}
	}

	public void showHTML( final String location, final String text )
	{
		GenericRequest request = new GenericRequest( location );
		request.responseText = text;
		DescriptionFrame.showRequest( request );
	}
}
