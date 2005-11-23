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

// layout
import java.awt.Color;
import java.awt.Dimension;
import java.awt.CardLayout;
import java.awt.GridLayout;
import java.awt.BorderLayout;
import javax.swing.BoxLayout;

// event listeners
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import javax.swing.ListSelectionModel;

// containers
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JComboBox;
import javax.swing.JTabbedPane;
import javax.swing.ButtonGroup;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.BorderFactory;
import javax.swing.JMenuBar;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;

// other imports
import java.util.List;
import java.text.ParseException;

import net.java.dev.spellcast.utilities.SortedListModel;
import net.java.dev.spellcast.utilities.LockableListModel;
import net.java.dev.spellcast.utilities.JComponentUtilities;

/**
 * An extension of <code>KoLFrame</code> which handles all the item
 * management functionality of Kingdom of Loathing.  This ranges from
 * basic transfer to and from the closet to item creation, cooking,
 * item use, and equipment.
 */

public class ItemManageFrame extends KoLFrame
{
	protected ButtonGroup clickGroup;
	protected JRadioButtonMenuItem [] clickOptions;

	private JTabbedPane tabs;
	private ItemManagePanel consume, create, special;
	private MultiButtonPanel inventory, closet;

	/**
	 * Constructs a new <code>ItemManageFrame</code> and inserts all
	 * of the necessary panels into a tabular layout for accessibility.
	 *
	 * @param	client	The client to be notified in the event of error.
	 */

	public ItemManageFrame( KoLmafia client )
	{
		super( client, "Item Management" );

		tabs = new JTabbedPane();
		consume = new ConsumePanel();
		create = new CreateItemPanel();
		inventory = new OutsideClosetPanel();
		closet = new InsideClosetPanel();

		JPanel consumeContainer = new JPanel();
		consumeContainer.setLayout( new BorderLayout() );
		consumeContainer.add( consume, BorderLayout.CENTER );

		// If the player is in a muscle sign, then make sure
		// that the restaurant panel is there.

		special = null;

		if ( client != null )
		{
			if ( !client.getRestaurantItems().isEmpty() )
			{
				special = new SpecialPanel( client.getRestaurantItems() );
				consumeContainer.add( special, BorderLayout.SOUTH );
			}
			else if ( !client.getMicrobreweryItems().isEmpty() )
			{
				special = new SpecialPanel( client.getMicrobreweryItems() );
				consumeContainer.add( special, BorderLayout.SOUTH );
			}
		}

		JPanel createContainer = new JPanel();
		createContainer.setLayout( new BorderLayout() );
		createContainer.add( create, BorderLayout.CENTER );

		tabs.addTab( "Consume", consumeContainer );
		tabs.addTab( "Create", createContainer );
		tabs.addTab( "Inventory", inventory );
		tabs.addTab( "Closet", closet );

		getContentPane().setLayout( new CardLayout( 10, 10 ) );
		getContentPane().add( tabs, "" );

		addMenuBar();
		refreshFilters();
	}

	private void addMenuBar()
	{
		JMenuBar menuBar = new JMenuBar();
		this.setJMenuBar( menuBar );

		JMenu refreshMenu = new JMenu( "Refresh" );
		refreshMenu.add( new ListRefreshMenuItem() );
		menuBar.add( refreshMenu );

		addConsumeMenu( menuBar );

		JMenu clicksMenu = new JMenu( "Transfers" );
		menuBar.add( clicksMenu );

		clickGroup = new ButtonGroup();
		clickOptions = new JRadioButtonMenuItem[4];
		clickOptions[0] = new JRadioButtonMenuItem( "Move all", true );
		clickOptions[1] = new JRadioButtonMenuItem( "Move all but one", false );
		clickOptions[2] = new JRadioButtonMenuItem( "Move multiple", false );
		clickOptions[3] = new JRadioButtonMenuItem( "Move exactly one", false );

		for ( int i = 0; i < clickOptions.length; ++i )
		{
			clickGroup.add( clickOptions[i] );
			clicksMenu.add( clickOptions[i] );
		}

		JMenu creationMenu = new JMenu( "Creatables" );
		creationMenu.add( new CreationDisplayMenuItem( "Use closet as ingredient source", "useClosetForCreation" ) );
		creationMenu.add( new CreationDisplayMenuItem( "Auto-repair box servants on explosion", "autoRepairBoxes" ) );
		creationMenu.add( new CreationDisplayMenuItem( "Use clockwork box servants", "useClockworkBoxes" ) );
		creationMenu.add( new CreationDisplayMenuItem( "Cook or mix without a box servant", "createWithoutBoxServants" ) );
		creationMenu.add( new CreationDisplayMenuItem( "Include post-ascension recipes", "includeAscensionRecipes" ) );
		menuBar.add( creationMenu );
	}

	public void refreshFilters()
	{
		consume.elementList.setCellRenderer( AdventureResult.getConsumableCellRenderer(
			consumeFilter[0].isSelected(), consumeFilter[1].isSelected(), consumeFilter[2].isSelected() ) );

		create.elementList.setCellRenderer( AdventureResult.getCreatableCellRenderer(
			consumeFilter[0].isSelected(), consumeFilter[1].isSelected(), consumeFilter[2].isSelected() ) );
	}

	/**
	 * Auxiliary method used to enable and disable a frame.  By default,
	 * this attempts to toggle the enable/disable status on all tabs.
	 *
	 * @param	isEnabled	<code>true</code> if the frame is to be re-enabled
	 */

	public void setEnabled( boolean isEnabled )
	{
		super.setEnabled( isEnabled );

		if ( consume != null )
			consume.setEnabled( isEnabled );

		if ( create != null )
			create.setEnabled( isEnabled );

		if ( inventory != null )
			inventory.setEnabled( isEnabled );

		if ( closet != null )
			closet.setEnabled( isEnabled );

		if ( special != null )
			special.setEnabled( isEnabled );
	}

	private class MultiButtonPanel extends JPanel
	{
		private JPanel enclosingPanel;
		protected ShowDescriptionList elementList;
		protected JButton [] buttons;

		public MultiButtonPanel( String title, LockableListModel elementModel )
		{
			enclosingPanel = new JPanel( new BorderLayout( 10, 10 ) );
			this.elementList = new ShowDescriptionList( elementModel );

			JPanel centerPanel = new JPanel( new BorderLayout() );
			centerPanel.add( JComponentUtilities.createLabel( title, JLabel.CENTER, Color.black, Color.white ), BorderLayout.NORTH );
			centerPanel.add( new JScrollPane( elementList, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
				JScrollPane.HORIZONTAL_SCROLLBAR_NEVER ), BorderLayout.CENTER );

			enclosingPanel.add( centerPanel, BorderLayout.CENTER );
			setLayout( new CardLayout( 10, 10 ) );
			add( enclosingPanel, "" );
		}

		public void setButtons( String [] buttonLabels, ActionListener [] buttonListeners )
		{
			JPanel containerPanel = new JPanel( new GridLayout( buttonLabels.length, 1, 5, 5 ) );
			buttons = new JButton[ buttonLabels.length ];

			for ( int i = 0; i < buttonLabels.length; ++i )
			{
				buttons[i] = new JButton( buttonLabels[i] );
				buttons[i].addActionListener( buttonListeners[i] );
				containerPanel.add( buttons[i] );
			}

			JPanel eastPanel = new JPanel( new BorderLayout() );
			eastPanel.add( containerPanel, BorderLayout.NORTH );
			enclosingPanel.add( eastPanel, BorderLayout.EAST );
		}

		public void setEnabled( boolean isEnabled )
		{
			elementList.setEnabled( isEnabled );
			for ( int i = 0; i < buttons.length; ++i )
				buttons[i].setEnabled( isEnabled );
		}
	}

	private class ConsumePanel extends ItemManagePanel
	{
		public ConsumePanel()
		{	super( "Usable Items", "use one", "use multiple", KoLCharacter.getUsables() );
		}

		protected void actionConfirmed()
		{	consume( false );
		}

		protected void actionCancelled()
		{	consume( true );
		}

		private void consume( boolean useMultiple )
		{
			Object [] items = elementList.getSelectedValues();
			if ( items.length == 0 )
				return;

			int consumptionType, consumptionCount;
			AdventureResult currentItem;

			Runnable [] requests = new Runnable[ items.length ];
			int [] repeatCount = new int[ items.length ];

			for ( int i = 0; i < items.length; ++i )
			{
				currentItem = (AdventureResult) items[i];

				consumptionType = TradeableItemDatabase.getConsumptionType( currentItem.getName() );
				consumptionCount = useMultiple ? getQuantity( "Using multiple " + currentItem.getName() + "...", currentItem.getCount() ) : 1;

				if ( consumptionCount == 0 )
					return;

				requests[i] = consumptionType == ConsumeItemRequest.CONSUME_MULTIPLE ?
					new ConsumeItemRequest( client, currentItem.getInstance( consumptionCount ) ) :
					new ConsumeItemRequest( client, currentItem.getInstance( 1 ) );

				repeatCount[i] = consumptionType == ConsumeItemRequest.CONSUME_MULTIPLE ? 1 : consumptionCount;
			}

			(new RequestThread( requests, repeatCount )).start();
		}
	}

	private class SpecialPanel extends ItemManagePanel
	{
		public SpecialPanel( LockableListModel items )
		{	super( "Sign-Specific Stuffs", "buy one", "buy multiple", items );
		}

		protected void actionConfirmed()
		{	purchase( false );
		}

		protected void actionCancelled()
		{	purchase( true );
		}

		private void purchase( boolean purchaseMultiple )
		{
			Object [] items = elementList.getSelectedValues();
			if ( items.length == 0 )
				return;

			String currentItem;
			int consumptionCount;

			Runnable [] requests = new Runnable[ items.length ];
			int [] repeatCount = new int[ items.length ];

			for ( int i = 0; i < items.length; ++i )
			{
				currentItem = (String) items[i];
				consumptionCount = purchaseMultiple ? getQuantity( "Buying multiple " + currentItem + "...", Integer.MAX_VALUE, 1 ) : 1;

				if ( consumptionCount == 0 )
					return;

				requests[i] = elementList.getModel() == client.getRestaurantItems() ?
					(KoLRequest) (new RestaurantRequest( client, currentItem )) : (KoLRequest) (new MicrobreweryRequest( client, currentItem ));

				repeatCount[i] = consumptionCount;
			}

			(new RequestThread( requests, repeatCount )).start();
		}
	}

	private class OutsideClosetPanel extends MultiButtonPanel
	{
		public OutsideClosetPanel()
		{
			super( "Inside Inventory", KoLCharacter.getInventory() );
			setButtons( new String [] { "put in closet", "sell to npcs", "send to store", "donate to clan" },
				new ActionListener [] { new PutInClosetListener( false, elementList ), new AutoSellListener( false, AutoSellRequest.AUTOSELL, elementList ), new AutoSellListener( false, AutoSellRequest.AUTOMALL, elementList ), new GiveToClanListener( false, elementList ) } );
		}
	}

	private class InsideClosetPanel extends MultiButtonPanel
	{
		public InsideClosetPanel()
		{
			super( "Inside Closet", KoLCharacter.getCloset() );
			setButtons( new String [] { "put in bag", "sell to npcs", "send to store", "donate to clan" },
				new ActionListener [] { new PutInClosetListener( true, elementList ), new AutoSellListener( true, AutoSellRequest.AUTOSELL, elementList ), new AutoSellListener( true, AutoSellRequest.AUTOMALL, elementList ), new GiveToClanListener( true, elementList ) } );
		}
	}

	private abstract class TransferListener implements ActionListener
	{
		protected String description;
		protected boolean retrieveFromClosetFirst;

		protected Runnable [] requests;
		protected ShowDescriptionList elementList;

		public TransferListener( String description, boolean retrieveFromClosetFirst, ShowDescriptionList elementList )
		{
			this.description = description;
			this.retrieveFromClosetFirst = retrieveFromClosetFirst;
			this.elementList = elementList;
		}

		public Object [] initialSetup()
		{
			Object [] items = getDesiredItems( elementList, description );
			this.requests = new Runnable[ !retrieveFromClosetFirst || description.equals( "Bagging" ) ? 1 : 2 ];

			if ( retrieveFromClosetFirst )
				requests[0] = new ItemStorageRequest( client, ItemStorageRequest.CLOSET_TO_INVENTORY, items );

			return items;
		}

		public void initializeTransfer()
		{	(new RequestThread( requests )).start();
		}
	}

	private class PutInClosetListener extends TransferListener
	{
		public PutInClosetListener( boolean retrieveFromClosetFirst, ShowDescriptionList elementList )
		{	super( retrieveFromClosetFirst ? "Bagging" : "Closeting", retrieveFromClosetFirst, elementList );
		}

		public void actionPerformed( ActionEvent e )
		{
			Object [] items = initialSetup();
			if ( items == null )
				return;

			if ( !retrieveFromClosetFirst )
				requests[0] = new ItemStorageRequest( client, ItemStorageRequest.INVENTORY_TO_CLOSET, items );

			initializeTransfer();
		}
	}

	private class AutoSellListener extends TransferListener
	{
		private int sellType;

		public AutoSellListener( boolean retrieveFromClosetFirst, int sellType, ShowDescriptionList elementList )
		{
			super( sellType == AutoSellRequest.AUTOSELL ? "Autoselling" : "Automalling", retrieveFromClosetFirst, elementList );
			this.sellType = sellType;
		}

		public void actionPerformed( ActionEvent e )
		{
			if ( JOptionPane.NO_OPTION == JOptionPane.showConfirmDialog( null,
				"Are you sure you would like to sell the selected items?",
					"Sell request nag screen!", JOptionPane.YES_NO_OPTION ) )
						return;

			Object [] items = initialSetup();
			if ( items == null )
				return;

			requests[ requests.length - 1 ] = new AutoSellRequest( client, items, sellType );
			initializeTransfer();
		}
	}

	private class GiveToClanListener extends TransferListener
	{
		public GiveToClanListener( boolean retrieveFromClosetFirst, ShowDescriptionList elementList )
		{	super( "Stashing", retrieveFromClosetFirst, elementList );
		}

		public void actionPerformed( ActionEvent e )
		{
			Object [] items = initialSetup();
			if ( items == null )
				return;

			requests[ requests.length - 1 ] = new ClanStashRequest( client, items, ClanStashRequest.ITEMS_TO_STASH );
			initializeTransfer();
		}
	}

	private Object [] getDesiredItems( ShowDescriptionList elementList, String message )
	{
		Object [] items = elementList.getSelectedValues();
		if ( items.length == 0 )
			return null;

		int neededSize = items.length;
		AdventureResult currentItem;

		for ( int i = 0; i < items.length; ++i )
		{
			currentItem = (AdventureResult) items[i];

			int quantity = clickOptions[0].isSelected() ? currentItem.getCount() : clickOptions[1].isSelected() ?
				currentItem.getCount() - 1 : clickOptions[2].isSelected() ?
				getQuantity( message + " " + currentItem.getName() + "...", currentItem.getCount() ) : 1;

			// If the user manually enters zero, return from
			// this, since they probably wanted to cancel.

			if ( quantity == 0 && clickOptions[2].isSelected() )
				return null;

			// Otherwise, if it was not a manual entry, then reset
			// the entry to null so that it can be re-processed.

			if ( quantity == 0 )
			{
				items[i] = null;
				--neededSize;
			}
			else
			{
				items[i] = currentItem.getInstance( quantity );
			}
		}

		// If none of the array entries were nulled,
		// then return the array as-is.

		if ( neededSize == items.length )
			return items;

		// Otherwise, shrink the array which will be
		// returned so that it removes any nulled values.

		Object [] desiredItems = new Object[ neededSize ];
		neededSize = 0;

		for ( int i = 0; i < items.length; ++i )
			if ( items[i] != null )
				desiredItems[ neededSize++ ] = items[i];

		return desiredItems;
	}

	/**
	 * Internal class used to handle everything related to
	 * creating items; this allows creating of items,
	 * which usually get resold in malls.
	 */

	private class CreateItemPanel extends ItemManagePanel
	{
		public CreateItemPanel()
		{
			super( "Create an Item", "create one", "create multiple", ConcoctionsDatabase.getConcoctions() );
			elementList.setSelectionMode( ListSelectionModel.SINGLE_SELECTION );
		}

		protected void actionConfirmed()
		{	create( false );
		}

		public void actionCancelled()
		{	create( true );
		}

		private void create( boolean createMultiple )
		{
			Object selected = elementList.getSelectedValue();

			if ( selected == null )
				return;

			client.updateDisplay( DISABLED_STATE, "Verifying ingredients..." );
			ItemCreationRequest selection = (ItemCreationRequest) selected;
			selection.setQuantityNeeded( createMultiple ? getQuantity( "Creating multiple " + selection.getName() + "...", selection.getQuantityNeeded() ) : 1 );

			(new RequestThread( selection )).start();
		}
	}

	private class ListRefreshMenuItem extends JMenuItem implements ActionListener
	{
		public ListRefreshMenuItem()
		{
			super( "Refresh Lists" );
			addActionListener( this );
		}

		public void actionPerformed( ActionEvent e )
		{	(new RequestThread( new EquipmentRequest( client, EquipmentRequest.CLOSET ) )).start();
		}
	}

	private class CreationDisplayMenuItem extends SettingChangeMenuItem implements ActionListener
	{
		public CreationDisplayMenuItem( String title, String property )
		{	super( title, property );
		}

		public void actionPerformed( ActionEvent e )
		{
			super.actionPerformed( e );
			ConcoctionsDatabase.refreshConcoctions();
		}
	}

	/**
	 * The main method used in the event of testing the way the
	 * user interface looks.  This allows the UI to be tested
	 * without having to constantly log in and out of KoL.
	 */

	public static void main( String [] args )
	{
		Object [] parameters = new Object[1];
		parameters[0] = null;

		(new CreateFrameRunnable( ItemManageFrame.class, parameters )).run();
	}
}
