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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.SwingUtilities;
import net.sourceforge.foxtrot.ConcurrentWorker;
import net.sourceforge.foxtrot.Job;

public class KoLmafiaGUI extends KoLmafia
{
	/**
	 * The main method.  Currently, it instantiates a single instance
	 * of the <code>KoLmafia</code>after setting the default
	 * look and feel of all <code>JFrame</code> objects to decorated.
	 */

	public static final void initialize()
	{
		KoLmafiaGUI session = new KoLmafiaGUI();
		StaticEntity.setClient( session );

		(new CreateFrameRunnable( LoginFrame.class )).run();

		// All that completed, check to see if there is an auto-login
		// which should occur.

		String autoLogin = StaticEntity.getProperty( "autoLogin" );
		if ( !autoLogin.equals( "" ) )
		{
			// Make sure that a password was stored for this
			// character (would fail otherwise):

			String password = getSaveState( autoLogin );
			if ( password != null && !password.equals( "" ) )
				RequestThread.postRequest( new LoginRequest( autoLogin, password ) );
		}
	}

	public static final void checkFrameSettings()
	{
		String frameSetting = StaticEntity.getGlobalProperty( "initialFrames" );
		String desktopSetting = StaticEntity.getGlobalProperty( "initialDesktop" );

		// If this user doesn't have any data, then go
		// ahead and copy the global data instead.

		if ( frameSetting.equals( "" ) && desktopSetting.equals( "" ) )
		{
			StaticEntity.setGlobalProperty( "initialFrames", StaticEntity.getGlobalProperty( "", "initialFrames" ) );
			StaticEntity.setGlobalProperty( "initialDesktop", StaticEntity.getGlobalProperty( "", "initialDesktop" ) );

			frameSetting = StaticEntity.getGlobalProperty( "initialFrames" );
			desktopSetting = StaticEntity.getGlobalProperty( "initialDesktop" );
		}

		// If there is still no data (somehow the global data
		// got emptied), default to relay-browser only).

		if ( frameSetting.equals( "" ) && desktopSetting.equals( "" ) )
		{
			StaticEntity.setGlobalProperty( "initialFrames", "LocalRelayServer" );
			StaticEntity.setGlobalProperty( "initialDesktop", "" );
		}
	}

	/**
	 * Initializes the <code>KoLmafia</code> session.  Called after
	 * the login has been confirmed to notify thethat the
	 * login was successful, the user-specific settings should be
	 * loaded, and the user can begin adventuring.
	 */

	public void initialize( String username )
	{
		super.initialize( username );

		if ( KoLRequest.passwordHash != null )
		{
			if ( StaticEntity.getBooleanProperty( "retrieveContacts" ) )
			{
				RequestThread.postRequest( new ContactListRequest() );
				StaticEntity.setProperty( "retrieveContacts", String.valueOf( !contactList.isEmpty() ) );
			}
		}

		LoginFrame.hideInstance();

		checkFrameSettings();
		String frameSetting = StaticEntity.getGlobalProperty( "initialFrames" );
		String desktopSetting = StaticEntity.getGlobalProperty( "initialDesktop" );

		// Reset all the titles on all existing frames.

		SystemTrayFrame.updateToolTip();
		KoLDesktop.updateTitle();

		// Instantiate the appropriate instance of the
		// frame that should be loaded based on the mode.

		if ( !desktopSetting.equals( "" ) )
		{
			KoLDesktop.getInstance().initializeTabs();
			if ( !StaticEntity.getBooleanProperty( "relayBrowserOnly" ) )
				KoLDesktop.displayDesktop();
		}

		String [] frameArray = frameSetting.split( "," );
		String [] desktopArray = desktopSetting.split( "," );

		ArrayList initialFrameList = new ArrayList();

		if ( !frameSetting.equals( "" ) )
		{
			for ( int i = 0; i < frameArray.length; ++i )
			{
				if ( frameArray[i].equals( "HagnkStorageFrame" ) && KoLCharacter.isHardcore() )
					continue;

				if ( !initialFrameList.contains( frameArray[i] ) )
					initialFrameList.add( frameArray[i] );
			}
		}

		for ( int i = 0; i < desktopArray.length; ++i )
			initialFrameList.remove( desktopArray[i] );

		if ( !initialFrameList.isEmpty() && !StaticEntity.getBooleanProperty( "relayBrowserOnly" ) )
		{
			String [] initialFrames = new String[ initialFrameList.size() ];
			initialFrameList.toArray( initialFrames );

			for ( int i = 0; i < initialFrames.length; ++i )
				if ( !initialFrames[i].equals( "EventsFrame" ) )
					constructFrame( initialFrames[i] );
		}

		// Figure out which user interface is being
		// used -- account for minimalist loadings.

		LoginFrame.disposeInstance();


		if ( KoLMailManager.hasNewMessages() )
		{
			KoLmafia.updateDisplay( "You have new mail." );
		}
		else
		{
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
		}
	}

	public static final void constructFrame( String frameName )
	{
		if ( frameName.equals( "" ) )
			return;

		if ( frameName.equals( "KoLMessenger" ) )
		{
			updateDisplay( "Initializing chat interface..." );

			KoLMessenger.initialize();
			RequestThread.enableDisplayIfSequenceComplete();

			return;
		}

		try
		{
			Class frameClass = Class.forName( "net.sourceforge.kolmafia." + frameName );
			constructFrame( frameClass );
		}
		catch ( Exception e )
		{
			//should not happen.  Therefore, print
			// a stack trace for debug purposes.

			StaticEntity.printStackTrace( e );
		}
	}

	public static final void constructFrame( Class frameClass )
	{
		try
		{
			FrameConstructor maker = new FrameConstructor( frameClass );

			if ( SwingUtilities.isEventDispatchThread() )
				ConcurrentWorker.post( maker );
			else
				maker.run();
		}
		catch ( Exception e )
		{
			// Should not happen.  Therefore, print
			// a stack trace for debug purposes.

			StaticEntity.printStackTrace( e );
		}
	}

	private static class FrameConstructor extends Job
	{
		public Class frameClass;

		public FrameConstructor( Class frameClass )
		{	this.frameClass = frameClass;
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
					updateDisplay( ERROR_STATE, "No buffs found to purchase." );
					RequestThread.enableDisplayIfSequenceComplete();
					return;
				}
			}
			else if ( this.frameClass == CakeArenaFrame.class )
			{
				if ( CakeArenaManager.getOpponentList().isEmpty() )
				{
					updateDisplay( ERROR_STATE, "Equip a familiar first." );
					RequestThread.enableDisplayIfSequenceComplete();
					return;
				}
			}
			else if ( this.frameClass == CalendarFrame.class )
			{
				String base = "http://images.kingdomofloathing.com/otherimages/bikini/";
				for ( int i = 1; i < CalendarFrame.CALENDARS.length; ++i )
					RequestEditorKit.downloadImage( base + CalendarFrame.CALENDARS[i] + ".gif" );
				base = "http://images.kingdomofloathing.com/otherimages/beefcake/";
				for ( int i = 1; i < CalendarFrame.CALENDARS.length; ++i )
					RequestEditorKit.downloadImage( base + CalendarFrame.CALENDARS[i] + ".gif" );
			}
			else if ( this.frameClass == ClanManageFrame.class )
			{
				if ( ClanManager.getStash().isEmpty() )
				{
					KoLmafia.updateDisplay( "Retrieving clan stash contents..." );
					RequestThread.postRequest( new ClanStashRequest() );
				}
			}
			else if ( this.frameClass == ContactListFrame.class )
			{
				if ( contactList.isEmpty() )
					RequestThread.postRequest( new ContactListRequest() );

				if ( StaticEntity.getGlobalProperty( "initialDesktop" ).indexOf( "ContactListFrame" ) != -1 )
					return;
			}
			else if ( this.frameClass == FamiliarTrainingFrame.class )
			{
				if ( CakeArenaManager.getOpponentList().isEmpty() )
				{
					updateDisplay( ERROR_STATE, "Equip a familiar first." );
					RequestThread.enableDisplayIfSequenceComplete();
					return;
				}
			}
			else if ( this.frameClass == FlowerHunterFrame.class )
			{
				KoLmafia.updateDisplay( "Determining number of attacks remaining..." );
				RequestThread.postRequest( new FlowerHunterRequest() );

				if ( KoLmafia.refusesContinue() )
					return;
			}
			else if ( this.frameClass == ItemManageFrame.class )
			{
				// If the person is in a mysticality sign, make sure
				// you retrieve information from the restaurant.

				if ( KoLCharacter.canEat() && KoLCharacter.inMysticalitySign() )
					if ( restaurantItems.isEmpty() )
						RequestThread.postRequest( new RestaurantRequest() );

				// If the person is in a moxie sign and they have completed
				// the beach quest, then retrieve information from the
				// microbrewery.

				if ( KoLCharacter.canDrink() && KoLCharacter.inMoxieSign() && microbreweryItems.isEmpty() )
				{
					KoLRequest.VISITOR.constructURLString( "main.php" );
					RequestThread.postRequest( KoLRequest.VISITOR );

					if ( KoLRequest.VISITOR.responseText.indexOf( "beach.php" ) != -1 )
						RequestThread.postRequest( new MicrobreweryRequest() );
				}

				if ( StaticEntity.getBooleanProperty( "autoSatisfyWithStash" ) && KoLCharacter.canInteract() && KoLCharacter.hasClan() )
					if ( !ClanManager.isStashRetrieved() )
						RequestThread.postRequest( new ClanStashRequest() );
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
					return;
			}
			else if ( this.frameClass == MuseumFrame.class )
			{
				if ( !KoLCharacter.hasDisplayCase() )
				{
					updateDisplay( ERROR_STATE, "Sorry, you don't have a display case." );
					return;
				}

				if ( collection.isEmpty() )
					RequestThread.postRequest( new MuseumRequest() );
			}
			else if ( this.frameClass == MushroomFrame.class )
			{
				for ( int i = 0; i < MushroomPlot.MUSHROOMS.length; ++i )
					RequestEditorKit.downloadImage( "http://images.kingdomofloathing.com/itemimages/" + MushroomPlot.MUSHROOMS[i][1] );
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
				RequestThread.postRequest( new StoreManageRequest( true ) );
				RequestThread.postRequest( new StoreManageRequest( false ) );

				RequestThread.closeRequestSequence();
			}

			(new CreateFrameRunnable( frameClass )).run();
		}
	}

	public void showHTML( String text )
	{
		KoLRequest.VISITOR.responseText = text;
		DescriptionFrame.showRequest( KoLRequest.VISITOR );
	}
}
