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

import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.JOptionPane;

/**
 * The main class for the <code>KoLmafia</code> package.  This
 * class encapsulates most of the data relevant to any given
 * session of <code>Kingdom of Loathing</code> and currently
 * functions as the blackboard in the architecture.  When data
 * listeners are implemented, it will continue to manage most
 * of the interactions.
 */

public class KoLmafiaGUI extends KoLmafia
{
	/**
	 * The main method.  Currently, it instantiates a single instance
	 * of the <code>KoLmafia</code> client after setting the default
	 * look and feel of all <code>JFrame</code> objects to decorated.
	 */

	public static void main( String [] args )
	{
		javax.swing.JFrame.setDefaultLookAndFeelDecorated( true );

		if ( StaticEntity.usesSystemTray() )
			SystemTrayFrame.addTrayIcon();

		KoLmafiaGUI session = new KoLmafiaGUI();
		StaticEntity.setClient( session );
		(new CreateFrameRunnable( LoginFrame.class )).run();
	}

	/**
	 * Initializes the <code>KoLmafia</code> session.  Called after
	 * the login has been confirmed to notify the client that the
	 * login was successful, the user-specific settings should be
	 * loaded, and the user can begin adventuring.
	 */

	public void initialize( String username, boolean getBreakfast, boolean isQuickLogin )
	{
		super.initialize( username, getBreakfast, isQuickLogin );
		if ( refusesContinue() )
			return;

		if ( KoLRequest.passwordHash != null && !isQuickLogin )
		{
			if ( StaticEntity.getProperty( "retrieveContacts" ).equals( "true" ) )
			{
				(new ContactListRequest( this )).run();
				StaticEntity.setProperty( "retrieveContacts", String.valueOf( !contactList.isEmpty() ) );
			}
		}

		String frameSetting = StaticEntity.getProperty( "initialFrames" );
		String desktopSetting = StaticEntity.getProperty( "initialDesktop" );

		// Reset all the titles on all existing frames.

		SystemTrayFrame.updateTooltip();
		KoLDesktop.updateTitle();

		// Instantiate the appropriate instance of the
		// frame that should be loaded based on the mode.

		String [] frameArray = frameSetting.split( "," );
		String [] desktopArray = desktopSetting.split( "," );

		ArrayList initialFrameList = new ArrayList();

		if ( !frameSetting.equals( "" ) )
			for ( int i = 0; i < frameArray.length; ++i )
				if ( !initialFrameList.contains( frameArray[i] ) )
					initialFrameList.add( frameArray[i] );

		for ( int i = 0; i < desktopArray.length; ++i )
			initialFrameList.remove( desktopArray[i] );

		if ( !initialFrameList.isEmpty() )
		{
			String [] initialFrames = new String[ initialFrameList.size() ];
			initialFrameList.toArray( initialFrames );

			for ( int i = 0; i < initialFrames.length; ++i )
				if ( !initialFrames[i].equals( "EventsFrame" ) )
					constructFrame( initialFrames[i] );
		}

		if ( !StaticEntity.getProperty( "initialDesktop" ).equals( "" ) )
		{
			KoLDesktop.getInstance().initializeTabs();
			KoLDesktop.getInstance().pack();
			KoLDesktop.getInstance().setVisible( true );
		}

		// Figure out which user interface is being
		// used -- account for minimalist loadings.

		Object [] frames = existingFrames.toArray();
		for ( int i = 0; i < frames.length; ++i )
			if ( frames[i] instanceof LoginFrame )
			{
				((LoginFrame)frames[i]).setVisible( false );
				((LoginFrame)frames[i]).dispose();
			}

		enableDisplay();
	}

	public static void constructFrame( String frameName )
	{
		// Now, test to see if any requests need to be run before
		// you fall into the event dispatch thread.

		if ( frameName.equals( "LocalRelayServer" ) )
		{
			StaticEntity.getClient().startRelayServer();
			return;
		}
		else if ( frameName.equals( "KoLMessenger" ) )
		{
			if ( StaticEntity.getClient().shouldMakeConflictingRequest() )
				(new ChannelColorsRequest()).run();

			KoLMessenger.initialize();
			(new ChatRequest( StaticEntity.getClient(), null, "/listen" )).run();

			return;
		}
		else if ( frameName.equals( "MailboxFrame" ) )
		{
			if ( !StaticEntity.getClient().shouldMakeConflictingRequest() )
			{
				updateDisplay( "You are currently adventuring." );
				return;
			}

			(new MailboxRequest( StaticEntity.getClient(), "Inbox" )).run();
			if ( KoLMailManager.getMessages( "Inbox" ).isEmpty() )
			{
				updateDisplay( "Your mailbox is empty." );
				return;
			}
		}
		else if ( frameName.equals( "BuffRequestFrame" ) )
		{
			BuffBotDatabase.configureBuffBots();

			int buffCount = BuffBotDatabase.buffCount();
			int actualBuffCount = 0;

			for ( int i = 0; i < buffCount; ++i )
				if ( BuffBotDatabase.getBuffOfferingCount(i) > 0 )
					++actualBuffCount;

			if ( actualBuffCount == 0 )
			{
				updateDisplay( "No buffs found to purchase." );
				enableDisplay();
				return;
			}
		}
		else if ( frameName.equals( "CakeArenaFrame" ) || frameName.equals( "FamiliarTrainingFrame" ) )
		{
			if ( !StaticEntity.getClient().shouldMakeConflictingRequest() )
			{
				updateDisplay( "You can't do that while adventuring." );
				return;
			}

			CakeArenaManager.getOpponentList();
		}
		else if ( frameName.equals( "ItemManageFrame" ) )
		{
			if ( StaticEntity.getClient().shouldMakeConflictingRequest() )
			{
				// If the person is in a mysticality sign, make sure
				// you retrieve information from the restaurant.

				if ( KoLCharacter.canEat() && KoLCharacter.inMysticalitySign() )
					if ( StaticEntity.getClient().getRestaurantItems().isEmpty() )
						(new RestaurantRequest( StaticEntity.getClient() )).run();

				// If the person is in a moxie sign and they have completed
				// the beach quest, then retrieve information from the
				// microbrewery.

				if ( KoLCharacter.canDrink() && KoLCharacter.inMoxieSign() )
					if ( StaticEntity.getClient().getMicrobreweryItems().isEmpty() )
						(new MicrobreweryRequest( StaticEntity.getClient() )).run();
			}
		}
		else if ( frameName.equals( "StoreManageFrame" ) )
		{
			if ( StaticEntity.getClient().shouldMakeConflictingRequest() )
			{
				(new StoreManageRequest( StaticEntity.getClient() )).run();
				(new StoreManageRequest( StaticEntity.getClient(), true )).run();
			}
		}
		else if ( frameName.equals( "RestoreOptionsFrame" ) )
		{
			frameName = "OptionsFrame";
		}

		try
		{
			Class associatedClass = Class.forName( "net.sourceforge.kolmafia." + frameName );
			(new CreateFrameRunnable( associatedClass )).run();
			enableDisplay();
		}
		catch ( ClassNotFoundException e )
		{
			// This should not happen.  Therefore, print
			// a stack trace for debug purposes.

			StaticEntity.printStackTrace( e );
		}
	}

	public void showHTML( String text, String title )
	{
		KoLRequest request = new KoLRequest( this, "" );
		request.responseText = text;
		FightFrame.showRequest( request );
	}

	/**
	 * Makes a request which attempts to remove the given effect.
	 * This method should prompt the user to determine which effect
	 * the player would like to remove.
	 */

	public void makeUneffectRequest()
	{
		Object selectedValue = JOptionPane.showInputDialog(
			null, "I want to remove this effect...", "It's Soft Green Martian Time!", JOptionPane.INFORMATION_MESSAGE, null,
			KoLCharacter.getEffects().toArray(), KoLCharacter.getEffects().get(0) );

		if ( selectedValue == null )
			return;

		(new RequestThread( new UneffectRequest( this, (AdventureResult) selectedValue ) )).run();
	}

	/**
	 * Makes a request which attempts to zap the chosen item
	 */

	public void makeZapRequest()
	{
		AdventureResult wand = KoLCharacter.getZapper();

		if ( wand == null )
			return;

		Object selectedValue = JOptionPane.showInputDialog(
			null, "I want to zap this item...", "Zzzzzzzzzap!", JOptionPane.INFORMATION_MESSAGE, null,
			KoLCharacter.getInventory().toArray(), KoLCharacter.getInventory().get(0) );

		if ( selectedValue == null )
			return;

		(new RequestThread( new ZapRequest( this, wand, (AdventureResult) selectedValue ) )).run();
	}

	/**
	 * Makes a request which attempts to smash the chosen item
	 */

	public void makePulverizeRequest()
	{
		Object selectedValue = JOptionPane.showInputDialog(
			null, "I want to pulverize this item...", "Smash!", JOptionPane.INFORMATION_MESSAGE, null,
			KoLCharacter.getInventory().toArray(), KoLCharacter.getInventory().get(0) );

		if ( selectedValue == null )
			return;

		AdventureResult item = (AdventureResult)selectedValue;
		int available = item.getCount( KoLCharacter.getInventory() );
		int smashCount = ( available > 1 ) ? KoLFrame.getQuantity( "How many " + item.getName() + " to smash?", available ) : 1;

		if ( smashCount == 0 )
			return;

		if ( !TradeableItemDatabase.isTradeable( item.getItemID() ) &&
		     JOptionPane.NO_OPTION == JOptionPane.showConfirmDialog( null,
						"Are you sure you would like to smash that untradeable item?",
						"Smash request nag screen!", JOptionPane.YES_NO_OPTION ) )
			return;

		(new RequestThread( new PulverizeRequest( this, item.getInstance( smashCount ) ) )).run();
	}

	/**
	 * Makes a request to the hermit, looking for the given number of
	 * items.  This method should prompt the user to determine which
	 * item to retrieve the hermit.
	 */

	public void makeHermitRequest()
	{
		if ( !hermitItems.contains( "ten-leaf clover" ) )
			(new HermitRequest( this )).run();

		if ( !permitsContinue() )
			return;

		Object [] hermitItemArray = hermitItems.toArray();
		Object selectedValue = JOptionPane.showInputDialog(
			null, "I want this from the hermit...", "Mugging Hermit for...", JOptionPane.INFORMATION_MESSAGE, null,
			hermitItemArray, null );

		if ( selectedValue == null )
			return;

		int selected = TradeableItemDatabase.getItemID( (String)selectedValue );
		int tradeCount = KoLFrame.getQuantity( "How many " + selectedValue + " to get?", HermitRequest.getWorthlessItemCount() );

		if ( tradeCount == 0 )
			return;

		(new RequestThread( new HermitRequest( this, selected, tradeCount ) )).run();
	}

	/**
	 * Makes a request to the trapper, looking for the given number of
	 * items.  This method should prompt the user to determine which
	 * item to retrieve from the trapper.
	 */

	public void makeTrapperRequest()
	{
		int furs = TrapperRequest.YETI_FUR.getCount( KoLCharacter.getInventory() );

		if ( furs == 0 )
		{
			updateDisplay( ERROR_STATE, "You don't have any yeti furs to trade." );
			return;
		}

		Object selectedValue = JOptionPane.showInputDialog(
			null, "I want this from the trapper...", "1337ing Trapper for...", JOptionPane.INFORMATION_MESSAGE, null,
			trapperItemNames, trapperItemNames[0] );

		if ( selectedValue == null )
			return;

		int selected = -1;
		for ( int i = 0; i < trapperItemNames.length; ++i )
			if ( selectedValue.equals( trapperItemNames[i] ) )
			{
				selected = trapperItemNumbers[i];
				break;
			}

		// Should not be possible...
		if ( selected == -1 )
			return;

		int tradeCount = KoLFrame.getQuantity( "How many " + selectedValue + " to get?", furs );
		if ( tradeCount == 0 )
			return;

		(new RequestThread( new TrapperRequest( this, selected, tradeCount ) )).run();
	}

	/**
	 * Makes a request to the hunter, looking to sell a given type of
	 * item.  This method should prompt the user to determine which
	 * item to sell to the hunter.
	 */

	public void makeHunterRequest()
	{
		if ( hunterItems.isEmpty() )
			(new BountyHunterRequest( this )).run();

		Object [] hunterItemArray = hunterItems.toArray();

		String selectedValue = (String) JOptionPane.showInputDialog(
			null, "I want to sell this to the hunter...", "The Quilted Thicker Picker Upper!", JOptionPane.INFORMATION_MESSAGE, null,
			hunterItemArray, hunterItemArray[0] );

		if ( selectedValue == null )
			return;

		AdventureResult selected = new AdventureResult( selectedValue, 0, false );
		int available = selected.getCount( KoLCharacter.getInventory() );

		if ( available == 0 )
		{
			updateDisplay( ERROR_STATE, "You don't have any " + selectedValue + "." );
			return;
		}

		int tradeCount = KoLFrame.getQuantity( "How many " + selectedValue + " to sell?", available );
		if ( tradeCount == 0 )
			return;

		// If we're not selling all of the item, closet the rest
		if ( tradeCount < available )
		{
			Object [] items = new Object[1];
			items[0] = selected.getInstance( available - tradeCount );

			Runnable [] sequence = new Runnable[3];
			sequence[0] = new ItemStorageRequest( this, ItemStorageRequest.INVENTORY_TO_CLOSET, items );
			sequence[1] = new BountyHunterRequest( this, selected.getItemID() );
			sequence[2] = new ItemStorageRequest( this, ItemStorageRequest.CLOSET_TO_INVENTORY, items );

			(new RequestThread( sequence )).run();
		}
		else
			(new RequestThread( new BountyHunterRequest( this, TradeableItemDatabase.getItemID( selectedValue ) ) )).run();
	}

	/**
	 * Makes a request to Doc Galaktik, looking for a cure.
	 */

	public void makeGalaktikRequest()
	{
		Object [] cureArray = GalaktikRequest.retrieveCures( this ).toArray();

		if ( cureArray.length == 0 )
		{
			updateDisplay( ERROR_STATE, "You don't need any cures." );
			return;
		}

		String selectedValue = (String) JOptionPane.showInputDialog(
			null, "Cure me, Doc!", "Doc Galaktik", JOptionPane.INFORMATION_MESSAGE, null,
			cureArray, cureArray[0] );

		if ( selectedValue == null )
			return;

		int type = 0;
		if ( selectedValue.indexOf( "HP" ) != -1 )
			type = GalaktikRequest.HP;
		else if ( selectedValue.indexOf( "MP" ) != -1 )
			type = GalaktikRequest.MP;
		else
			return;

		(new RequestThread( new GalaktikRequest( this, type ) )).run();
	}

	/**
	 * Makes a request to the hunter, looking for the given number of
	 * items.  This method should prompt the user to determine which
	 * item to retrieve the hunter.
	 */

	public void makeUntinkerRequest()
	{
		List untinkerItems = new ArrayList();

		for ( int i = 0; i < KoLCharacter.getInventory().size(); ++i )
		{
			AdventureResult currentItem = (AdventureResult) KoLCharacter.getInventory().get(i);
			int itemID = currentItem.getItemID();

			// Ignore silly fairy gravy + meat from yesterday recipe
			if ( itemID == ItemCreationRequest.MEAT_STACK )
				continue;

			// Otherwise, accept any COMBINE recipe
			if ( ConcoctionsDatabase.getMixingMethod( itemID ) == ItemCreationRequest.COMBINE )
				untinkerItems.add( currentItem );
		}

		if ( untinkerItems.isEmpty() )
		{
			updateDisplay( ERROR_STATE, "You don't have any untinkerable items." );
			return;
		}

		Object [] untinkerItemArray = untinkerItems.toArray();
		Arrays.sort( untinkerItemArray );

		AdventureResult selectedValue = (AdventureResult) JOptionPane.showInputDialog(
			null, "I want to untinker this item...", "You can unscrew meat paste?", JOptionPane.INFORMATION_MESSAGE, null,
			untinkerItemArray, untinkerItemArray[0] );

		if ( selectedValue == null )
			return;

		(new RequestThread( new UntinkerRequest( this, selectedValue.getItemID() ) )).run();
	}

	/**
	 * Set the Canadian Mind Control device to selected setting.
	 */

	public void makeMindControlRequest()
	{
		String [] levelArray = new String[12];
		for ( int i = 0; i < 12; ++i )
			levelArray[i] = "Level " + i;

		String selectedLevel = (String) JOptionPane.showInputDialog(
			null, "Set the device to what level?", "Change mind control device from level " + KoLCharacter.getMindControlLevel(),
				JOptionPane.INFORMATION_MESSAGE, null, levelArray, levelArray[ KoLCharacter.getMindControlLevel() ] );

		if ( selectedLevel == null )
			return;

		(new RequestThread( new MindControlRequest( this, StaticEntity.parseInt( selectedLevel.split( " " )[1] ) ) )).run();
	}

	private static class ChannelColorsRequest extends KoLRequest
	{
		public ChannelColorsRequest()
		{	super( StaticEntity.getClient(), "account_chatcolors.php", true );
		}

		public void run()
		{
			super.run();

			// First, add in all the colors for all of the
			// channel tags (for people using standard KoL
			// chatting mode).

			Matcher colorMatcher = Pattern.compile( "<td>(.*?)&nbsp;&nbsp;&nbsp;&nbsp;</td>.*?<option value=(\\d+) selected>" ).matcher( responseText );
			while ( colorMatcher.find() )
				KoLMessenger.setColor( colorMatcher.group(1).toLowerCase(), StaticEntity.parseInt( colorMatcher.group(2) ) );

			// Add in other custom colors which are available
			// in the chat options.

			colorMatcher = Pattern.compile( "<select name=chatcolorself>.*?<option value=(\\d+) selected>" ).matcher( responseText );
			if ( colorMatcher.find() )
				KoLMessenger.setColor( "chatcolorself", StaticEntity.parseInt( colorMatcher.group(1) ) );

			colorMatcher = Pattern.compile( "<select name=chatcolorcontacts>.*?<option value=(\\d+) selected>" ).matcher( responseText );
			if ( colorMatcher.find() )
				KoLMessenger.setColor( "chatcolorcontacts", StaticEntity.parseInt( colorMatcher.group(1) ) );

			colorMatcher = Pattern.compile( "<select name=chatcolorothers>.*?<option value=(\\d+) selected>" ).matcher( responseText );
			if ( colorMatcher.find() )
				KoLMessenger.setColor( "chatcolorothers", StaticEntity.parseInt( colorMatcher.group(1) ) );
		}
	}
}
