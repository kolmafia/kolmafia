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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.SwingUtilities;

public class KoLmafiaGUI extends KoLmafia
{
	/**
	 * The main method.  Currently, it instantiates a single instance
	 * of the <code>KoLmafia</code>after setting the default
	 * look and feel of all <code>JFrame</code> objects to decorated.
	 */

	public static void main( String [] args )
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

	public static void checkFrameSettings()
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
		String originalName = KoLCharacter.getUserName();
		super.initialize( username );

		if ( refusesContinue() || originalName.equalsIgnoreCase( username ) )
			return;

		if ( KoLRequest.passwordHash != null )
		{
			if ( StaticEntity.getBooleanProperty( "retrieveContacts" ) )
			{
				RequestThread.postRequest( new ContactListRequest() );
				StaticEntity.setProperty( "retrieveContacts", String.valueOf( !contactList.isEmpty() ) );
			}
		}

		KoLFrame [] frames = StaticEntity.getExistingFrames();
		LoginFrame login = null;

		for ( int i = 0; i < frames.length; ++i )
			if ( frames[i] instanceof LoginFrame )
				login = (LoginFrame) frames[i];

		if ( login != null )
			login.setVisible( false );

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

		if ( login != null )
			login.dispose();

		if ( KoLMailManager.hasNewMessages() )
			KoLmafia.updateDisplay( "You have new mail." );
	}

	public static void constructFrame( String frameName )
	{	displayFrame( frameName );
	}

	private static void displayFrame( String frameName )
	{
		if ( frameName.equals( "" ) )
			return;

		// Now, test to see if any requests need to be run before
		// you fall into the event dispatch thread.

		if ( frameName.equals( "BuffBotFrame" ) )
		{
			BuffBotManager.loadSettings();
		}
		else if ( frameName.equals( "BuffRequestFrame" ) )
		{
			if ( !BuffBotDatabase.hasOfferings() )
			{
				updateDisplay( "No buffs found to purchase." );
				RequestThread.enableDisplayIfSequenceComplete();
				return;
			}
		}
		else if ( frameName.equals( "CakeArenaFrame" ) )
		{
			CakeArenaManager.getOpponentList();
		}
		else if ( frameName.equals( "CalendarFrame" ) )
		{
			String base = "http://images.kingdomofloathing.com/otherimages/bikini/";
			for ( int i = 1; i < CalendarFrame.CALENDARS.length; ++i )
				RequestEditorKit.downloadImage( base + CalendarFrame.CALENDARS[i] + ".gif" );
			base = "http://images.kingdomofloathing.com/otherimages/beefcake/";
			for ( int i = 1; i < CalendarFrame.CALENDARS.length; ++i )
				RequestEditorKit.downloadImage( base + CalendarFrame.CALENDARS[i] + ".gif" );
		}
		else if ( frameName.equals( "ClanManageFrame" ) )
		{
			if ( ClanManager.getStash().isEmpty() )
			{
				KoLmafia.updateDisplay( "Retrieving clan stash contents..." );
				RequestThread.postRequest( new ClanStashRequest() );
			}
		}
		else if ( frameName.equals( "FamiliarTrainingFrame" ) )
		{
			CakeArenaManager.getOpponentList();
		}
		else if ( frameName.equals( "FlowerHunterFrame" ) )
		{
			KoLmafia.updateDisplay( "Determining number of attacks remaining..." );
			RequestThread.postRequest( new FlowerHunterRequest() );
		}
		else if ( frameName.equals( "HagnkStorageFrame" ) )
		{
			if ( storage.isEmpty() )
			{
				KoLmafia.updateDisplay( "You have nothing in storage." );
				RequestThread.enableDisplayIfSequenceComplete();
				return;
			}
		}
		else if ( frameName.equals( "ItemManageFrame" ) )
		{
			// If the person is in a mysticality sign, make sure
			// you retrieve information from the restaurant.

			if ( KoLCharacter.canEat() && KoLCharacter.inMysticalitySign() )
				if ( restaurantItems.isEmpty() )
					RequestThread.postRequest( new RestaurantRequest() );

			// If the person is in a moxie sign and they have completed
			// the beach quest, then retrieve information from the
			// microbrewery.

			if ( KoLCharacter.canDrink() && KoLCharacter.inMoxieSign() )
				if ( microbreweryItems.isEmpty() )
					RequestThread.postRequest( new MicrobreweryRequest() );

			if ( StaticEntity.getBooleanProperty( "showStashIngredients" ) && KoLCharacter.canInteract() && KoLCharacter.hasClan() )
				if ( !ClanManager.isStashRetrieved() )
					RequestThread.postRequest( new ClanStashRequest() );
		}
		else if ( frameName.equals( "KoLMessenger" ) )
		{
			updateDisplay( "Retrieving chat color preferences..." );
			RequestThread.postRequest( new ChannelColorsRequest() );

			KoLMessenger.initialize();

			RequestThread.postRequest( new ChatRequest( null, "/listen" ) );
			updateDisplay( "Color preferences retrieved.  Chat started." );
			RequestThread.enableDisplayIfSequenceComplete();

			return;
		}
		else if ( frameName.equals( "LocalRelayServer" ) )
		{
			StaticEntity.getClient().startRelayServer();
			return;
		}
		else if ( frameName.equals( "MailboxFrame" ) )
		{
			RequestThread.postRequest( new MailboxRequest( "Inbox" ) );
			if ( LoginRequest.isInstanceRunning() )
				return;
		}
		else if ( frameName.equals( "MoneyMakingGameFrame" ) )
		{
			updateDisplay( "Retrieving MMG bet history..." );
			RequestThread.postRequest( new MoneyMakingGameRequest() );

			if ( MoneyMakingGameRequest.getBetSummary().isEmpty() )
			{
				updateDisplay( "You have no bet history to summarize." );
				RequestThread.closeRequestSequence();

				return;
			}

			updateDisplay( "MMG bet history retrieved." );
			RequestThread.enableDisplayIfSequenceComplete();
		}
		else if ( frameName.equals( "MuseumFrame" ) )
		{
			RequestThread.postRequest( new MuseumRequest() );
		}
		else if ( frameName.equals( "MushroomFrame" ) )
		{
			for ( int i = 0; i < MushroomPlot.MUSHROOMS.length; ++i )
				RequestEditorKit.downloadImage( "http://images.kingdomofloathing.com/itemimages/" + MushroomPlot.MUSHROOMS[i][1] );
		}
		else if ( frameName.equals( "RestoreOptionsFrame" ) )
		{
			frameName = "OptionsFrame";
		}
		else if ( frameName.equals( "StoreManageFrame" ) )
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

		try
		{
			Class associatedClass = Class.forName( "net.sourceforge.kolmafia." + frameName );
			Runnable creator = new CreateFrameRunnable( associatedClass );

			if ( SwingUtilities.isEventDispatchThread() )
				creator.run();
			else
				SwingUtilities.invokeAndWait( creator );
		}
		catch ( Exception e )
		{
			//should not happen.  Therefore, print
			// a stack trace for debug purposes.

			StaticEntity.printStackTrace( e );
		}
	}

	public void showHTML( String text, String title )
	{
		KoLRequest request = new KoLRequest( "" );
		request.responseText = text;
		FightFrame.showRequest( request, title );
	}

	private static final Pattern GENERAL_PATTERN = Pattern.compile( "<td>([^<]*?)&nbsp;&nbsp;&nbsp;&nbsp;</td>.*?<option value=(\\d+) selected>" );
	private static final Pattern SELF_PATTERN = Pattern.compile( "<select name=chatcolorself>.*?<option value=(\\d+) selected>" );
	private static final Pattern CONTACTS_PATTERN = Pattern.compile( "<select name=chatcolorcontacts>.*?<option value=(\\d+) selected>" );
	private static final Pattern OTHER_PATTERN = Pattern.compile( "<select name=chatcolorothers>.*?<option value=(\\d+) selected>" );

	private static class ChannelColorsRequest extends KoLRequest
	{
		public ChannelColorsRequest()
		{	super( "account_chatcolors.php", true );
		}

		public void run()
		{
			super.run();

			// First, add in all the colors for all of the
			// channel tags (for people using standard KoL
			// chatting mode).

			Matcher colorMatcher = GENERAL_PATTERN.matcher( responseText );
			while ( colorMatcher.find() )
				KoLMessenger.setColor( "/" + colorMatcher.group(1).toLowerCase(), StaticEntity.parseInt( colorMatcher.group(2) ) );

			// Add in other custom colors which are available
			// in the chat options.

			colorMatcher = SELF_PATTERN.matcher( responseText );
			if ( colorMatcher.find() )
				KoLMessenger.setColor( "chatcolorself", StaticEntity.parseInt( colorMatcher.group(1) ) );

			colorMatcher = CONTACTS_PATTERN.matcher( responseText );
			if ( colorMatcher.find() )
				KoLMessenger.setColor( "chatcolorcontacts", StaticEntity.parseInt( colorMatcher.group(1) ) );

			colorMatcher = OTHER_PATTERN.matcher( responseText );
			if ( colorMatcher.find() )
				KoLMessenger.setColor( "chatcolorothers", StaticEntity.parseInt( colorMatcher.group(1) ) );
		}
	}
}
