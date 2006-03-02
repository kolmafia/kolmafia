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

import java.awt.Component;
import javax.swing.JEditorPane;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import java.lang.ref.WeakReference;

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
	static
	{
		System.setProperty( "com.apple.mrj.application.apple.menu.about.name", "KoLmafia" );
		System.setProperty( "com.apple.mrj.application.live-resize", "true" );
		System.setProperty( "com.apple.mrj.application.growbox.intrudes", "false" );

		JEditorPane.registerEditorKitForContentType( "text/html", "net.sourceforge.kolmafia.RequestEditorKit" );
	}

	private CreateFrameRunnable displayer;
	private LimitedSizeChatBuffer buffer;

	/**
	 * The main method.  Currently, it instantiates a single instance
	 * of the <code>KoLmafia</code> client after setting the default
	 * look and feel of all <code>JFrame</code> objects to decorated.
	 */

	public static void main( String [] args )
	{
		javax.swing.JFrame.setDefaultLookAndFeelDecorated( true );

		if ( System.getProperty( "os.name" ).startsWith( "Windows" ) && GLOBAL_SETTINGS.getProperty( "useSystemTrayIcon" ).equals( "true" ) )
			SystemTrayFrame.addTrayIcon();

		KoLmafiaGUI session = new KoLmafiaGUI();
		StaticEntity.setClient( session );

		if ( args.length == 0 )
		{
			String login = session.settings.getProperty( "autoLogin" );
			String password = session.getSaveState( login );

			if ( password != null )
				(new LoginRequest( session, login, password, true, true )).run();
		}
	}

	/**
	 * Updates the currently active display in the <code>KoLmafia</code>
	 * session.
	 */

	public void updateDisplay( int state, String message )
	{
		super.updateDisplay( state, message );

		if ( BuffBotHome.isBuffBotActive() )
			BuffBotHome.updateStatus( message );

		// Next, update all of the panels with the
		// desired update message.

		WeakReference [] references = new WeakReference[ existingPanels.size() ];
		existingPanels.toArray( references );

		for ( int i = 0; i < references.length; ++i )
		{
			if ( references[i].get() != null )
			{
				if ( references[i].get() instanceof KoLPanel )
					((KoLPanel) references[i].get()).setStatusMessage( state, message );

				switch ( state )
				{
					case DISABLE_STATE:
						((Component)references[i].get()).setEnabled( false );
						break;

					case NORMAL_STATE:
						break;

					default:
						((Component)references[i].get()).setEnabled( true );
						break;
				}
			}
		}

		// Finally, update all of the existing frames
		// with the appropriate state.

		KoLFrame [] frames = new KoLFrame[ existingFrames.size() ];
		existingFrames.toArray( frames );

		for ( int i = 0; i < frames.length; ++i )
			frames[i].updateDisplayState( state );
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

		// Reset all the titles on all existing frames.

		KoLFrame [] frames = new KoLFrame[ existingFrames.size() ];
		existingFrames.toArray( frames );

		SystemTrayFrame.updateTooltip();
		for ( int i = 0; i < frames.length; ++i )
			frames[i].updateTitle();

		// If you've already loaded an adventure frame,
		// or the login failed, then there's nothing left
		// to do.  Return from the method.

		if ( isLoggingIn || displayer == null || displayer.getCreation() instanceof AdventureFrame )
		{
			enableDisplay();
			return;
		}

		// Figure out which user interface is being
		// used -- account for minimalist loadings.

		CreateFrameRunnable previousDisplayer = displayer;

		Class frameClass = INTERFACE_MODES[ Integer.parseInt( GLOBAL_SETTINGS.getProperty( "userInterfaceMode" ) ) ];

		// Instantiate the appropriate instance of the
		// frame that should be loaded based on the mode.

		if ( frameClass == ChatFrame.class )
		{
			KoLMessenger.initialize();
		}
		else
		{
			displayer = new CreateFrameRunnable( frameClass );
			displayer.run();
		}

		((KoLFrame)previousDisplayer.getCreation()).setVisible( false );
		((KoLFrame)previousDisplayer.getCreation()).dispose();
		enableDisplay();
	}

	/**
	 * Deinitializes the <code>KoLmafia</code> session.  Called after
	 * the user has logged out.
	 */

	public void deinitialize()
	{
		super.deinitialize();

		if ( displayer == null )
		{
			Object [] parameters = new Object[2];
			parameters[0] = this;
			parameters[1] = saveStateNames;

			displayer = new CreateFrameRunnable( LoginFrame.class, parameters );
			displayer.run();
		}
	}

	public void showHTML( String text, String title )
	{	JOptionPane.showMessageDialog( null, RequestEditorKit.getDisplayHTML( text ), title, JOptionPane.PLAIN_MESSAGE );
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

		(new RequestThread( new UneffectRequest( this, (AdventureResult) selectedValue ) )).start();
		// Do a start() rather than a run() since this is invoked by
		// the ItemManageFrame, not the menu. Let that frame enable the
		// display and allow our thread to put up a status message.
	}

	/**
	 * Makes a request which attempts to zap the chosen item
	 * This method should prompt the user to determine which effect
	 * the player would like to remove.
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

		(new RequestThread( new ZapRequest( this, wand, (AdventureResult) selectedValue ) )).start();
		// Do a start() rather than a run() since this is invoked by
		// the ItemManageFrame, not the menu. Let that frame enable the
		// display and allow our thread to put up a status message.
	}

	/**
	 * Makes a request to the hermit, looking for the given number of
	 * items.  This method should prompt the user to determine which
	 * item to retrieve the hermit.
	 */

	public void makeHermitRequest()
	{
		if ( hermitItems.isEmpty() )
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

		int worthlessItems = HermitRequest.TRINKET.getCount( KoLCharacter.getInventory() ) +
			HermitRequest.GEWGAW.getCount( KoLCharacter.getInventory() ) +
			HermitRequest.KNICK_KNACK.getCount( KoLCharacter.getInventory() );

		int tradeCount = KoLFrame.getQuantity( "How many " + selectedValue + " to get?", worthlessItems );

		if ( tradeCount == 0 )
			return;

		(new RequestThread( new HermitRequest( this, selected, tradeCount ) )).start();
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
			updateDisplay( NORMAL_STATE, "You don't have any yeti furs to trade." );
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

		(new RequestThread( new TrapperRequest( this, selected, tradeCount ) )).start();
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
			updateDisplay( NORMAL_STATE, "You don't have any " + selectedValue + "." );
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

			(new RequestThread( sequence )).start();
		}
		else
			(new RequestThread( new BountyHunterRequest( this, TradeableItemDatabase.getItemID( selectedValue ) ) )).start();
	}

	/**
	 * Makes a request to Doc Galaktik, looking for a cure.
	 */

	public void makeGalaktikRequest()
	{
		Object [] cureArray = GalaktikRequest.retrieveCures( this ).toArray();

		if ( cureArray.length == 0 )
		{
			updateDisplay( NORMAL_STATE, "You don't need any cures." );
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

		(new RequestThread( new GalaktikRequest( this, type ) )).start();
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

		(new RequestThread( new UntinkerRequest( this, selectedValue.getItemID() ) )).start();
	}

	/**
	 * Set the Canadian Mind Control device to selected setting.
	 */

	public void makeMindControlRequest()
	{
		try
		{
			// Make sure we know current setting

			if ( !CharpaneRequest.wasRunOnce() )
				(new CharpaneRequest( this )).run();

			String [] levelArray = new String[12];
			for ( int i = 0; i < 12; ++i )
				levelArray[i] = "Level " + i;

			String selectedLevel = (String) JOptionPane.showInputDialog(
				null, "Set the device to what level?", "Change mind control device from level " + KoLCharacter.getMindControlLevel(),
					JOptionPane.INFORMATION_MESSAGE, null, levelArray, levelArray[ KoLCharacter.getMindControlLevel() ] );

			(new RequestThread( new MindControlRequest( this, df.parse( selectedLevel.split( " " )[1] ).intValue() ) )).start();
		}
		catch ( Exception e )
		{
			e.printStackTrace( KoLmafia.getLogStream() );
			e.printStackTrace();
		}
	}

	/**
	 * Confirms whether or not the user wants to make a drunken
	 * request.  This should be called before doing requests when
	 * the user is in an inebrieted state.
	 *
	 * @return	<code>true</code> if the user wishes to adventure drunk
	 */

	protected boolean confirmDrunkenRequest()
	{
		return JOptionPane.YES_OPTION == JOptionPane.showConfirmDialog( null,
			" You see flying penguins and a dancing hermit!\nThe mafia has stolen your shoes!\n(KoLmafia thinks you're too drunk!)\nContinue adventuring anyway?\n",
			"You're not drunk!?", JOptionPane.YES_NO_OPTION );
	}


	public void removeAllItemsFromStore()
	{
		(new StoreManageRequest( this )).run();

		// Now determine the desired prices on items.
		// If the value of an item is currently 100,
		// then remove the item from the store.

		StoreManager.SoldItem [] sold = new StoreManager.SoldItem[ StoreManager.getSoldItemList().size() ];
		StoreManager.getSoldItemList().toArray( sold );

		for ( int i = 0; i < sold.length && permitsContinue(); ++i )
			StoreManager.takeItem( sold[i].getItemID() );

		updateDisplay( ENABLE_STATE, "Store emptying complete." );
	}

	/**
	 * Hosts a massive sale on the items currently in your store.
	 * Utilizes the "minimum meat" principle.
	 */

	public void makeEndOfRunSaleRequest()
	{
		if ( !KoLCharacter.canInteract() )
		{
			updateDisplay( ERROR_STATE, "You are not yet out of ronin." );
			return;
		}

		if ( JOptionPane.NO_OPTION == JOptionPane.showConfirmDialog( null,
			"Are you sure you'd like to host an end-of-run sale?", "MASSIVE SALE", JOptionPane.YES_NO_OPTION ) )
				return;

		// Find all tradeable items.  Tradeable items
		// are marked by an autosell value of nonzero.

		AdventureResult [] items = new AdventureResult[ KoLCharacter.getInventory().size() ];
		KoLCharacter.getInventory().toArray( items );

		ArrayList autosell = new ArrayList();
		ArrayList automall = new ArrayList();

		// Only place items in the mall which are not
		// sold in NPC stores -- everything else, make
		// sure you autosell.

		for ( int i = 0; i < items.length; ++i )
		{
			if ( items[i].getCount() < 100 && TradeableItemDatabase.getPriceByID( items[i].getItemID() ) != -1 )
			{
				if ( TradeableItemDatabase.getPriceByID( items[i].getItemID() ) != 0 )
				{
					if ( NPCStoreDatabase.contains( items[i].getName() ) )
						autosell.add( items[i] );
					else
						automall.add( items[i] );
				}
			}
		}

		// Now, place all the items in the mall at the
		// maximum possible price.  This allows KoLmafia
		// to determine the minimum price.

		if ( autosell.size() > 0 && permitsContinue() )
			(new AutoSellRequest( this, autosell.toArray(), AutoSellRequest.AUTOSELL )).run();

		if ( automall.size() > 0 && permitsContinue() )
			(new AutoSellRequest( this, automall.toArray(), AutoSellRequest.AUTOMALL )).run();

		// Now, remove all the items that you intended
		// to remove from the store due to pricing issues.

		if ( permitsContinue() )
			priceItemsAtLowestPrice();

		updateDisplay( ENABLE_STATE, "Undercutting sale complete." );
	}

	/**
	 * Utility method used to print a list to the given output
	 * stream.  If there's a need to print to the current output
	 * stream, simply pass the output stream to this method.
	 */

	protected void printList( List printing )
	{
		if ( printing.isEmpty() )
			return;

		if ( printing.size() == 1 )
		{
			updateDisplay( ERROR_STATE, "You need " + printing.get(0).toString() + " before continuing." );
			return;
		}

		JOptionPane.showInputDialog( null, "The following items are still missing...", "Oops, you did it again!",
			JOptionPane.INFORMATION_MESSAGE, null, printing.toArray(), null );
	}
}
