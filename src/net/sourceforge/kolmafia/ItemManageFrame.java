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
import javax.swing.JCheckBox;
import javax.swing.JRadioButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JComboBox;
import javax.swing.JTabbedPane;
import javax.swing.ButtonGroup;
import javax.swing.BorderFactory;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JToolBar;

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
	private JTabbedPane tabs;
	private MultiButtonPanel inventory, closet, consume, create, special;

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
		special = null;

		// If the player is in a muscle sign, then make sure
		// that the restaurant panel is there.

		tabs.addTab( "Consume", consume );

		if ( client != null )
		{
			// If the person is in a mysticality sign, make sure
			// you retrieve information from the restaurant.

			if ( KoLCharacter.canEat() && KoLCharacter.inMysticalitySign() )
			{
				special = new SpecialPanel( client.getRestaurantItems() );
				tabs.add( "Restaurant", special );

				if ( client.getRestaurantItems().isEmpty() )
					(new RequestThread( new RestaurantRequest( client ) )).start();
			}

			// If the person is in a moxie sign and they have completed
			// the beach quest, then retrieve information from the
			// microbrewery.

			if ( KoLCharacter.canDrink() && KoLCharacter.inMoxieSign() && KoLCharacter.getInventory().contains( ConcoctionsDatabase.CAR ) )
			{
				special = new SpecialPanel( client.getMicrobreweryItems() );
				tabs.add( "Microbrewery", special );

				if ( client.getMicrobreweryItems().isEmpty() )
					(new RequestThread( new MicrobreweryRequest( client ) )).start();
			}
		}

		tabs.addTab( "Create", create );
		tabs.addTab( "Inventory", inventory );
		tabs.addTab( "Closet", closet );

		framePanel.setLayout( new CardLayout( 10, 10 ) );
		framePanel.add( tabs, "" );

		toolbarPanel.add( new DisplayFrameButton( "Store Manager", "mall.gif", StoreManageFrame.class ) );
		toolbarPanel.add( new DisplayFrameButton( "Display Case", "museum.gif", MuseumFrame.class ) );
		toolbarPanel.add( new DisplayFrameButton( "Hagnk's Storage", "hagnk.gif", HagnkStorageFrame.class ) );

		toolbarPanel.add( new JToolBar.Separator() );

		toolbarPanel.add( new DisplayFrameButton( "Preferences", "preferences.gif", OptionsFrame.class ) );
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

	private class ConsumePanel extends MultiButtonPanel
	{
		public ConsumePanel()
		{
			super( "Usable Items", KoLCharacter.getUsables(), false );
			elementList.setCellRenderer( AdventureResult.getConsumableCellRenderer( KoLCharacter.canEat(), KoLCharacter.canDrink(), true ) );
			setButtons( new String [] { "use one", "use multiple", "refresh list" },
				new ActionListener [] { new ConsumeListener( false ), new ConsumeListener( true ),
				new RequestButton( "Refresh Items", new EquipmentRequest( client, EquipmentRequest.CLOSET ) ) } );

			JCheckBox [] filters = new JCheckBox[3];
			filters[0] = new FilterCheckBox( filters, elementList, "Show food", KoLCharacter.canEat() );
			filters[1] = new FilterCheckBox( filters, elementList, "Show drink", KoLCharacter.canDrink() );
			filters[2] = new FilterCheckBox( filters, elementList, "Show other", true );

			for ( int i = 0; i < 3; ++i )
				optionPanel.add( filters[i] );
		}

		private class ConsumeListener implements ActionListener
		{
			private boolean useMultiple;

			public ConsumeListener( boolean useMultiple )
			{	this.useMultiple = useMultiple;
			}

			public void actionPerformed( ActionEvent e )
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

					if ( consumptionType == ConsumeItemRequest.CONSUME_MULTIPLE || consumptionType == ConsumeItemRequest.CONSUME_RESTORE )
					{
						requests[i] = new ConsumeItemRequest( client, currentItem.getInstance( consumptionCount ) );
						repeatCount[i] = 1;
					}
					else
					{
						requests[i] = new ConsumeItemRequest( client, currentItem.getInstance( 1 ) );
						repeatCount[i] = consumptionCount;
					}
				}

				(new RequestThread( requests, repeatCount )).start();
			}
		}
	}

	private class SpecialPanel extends MultiButtonPanel
	{
		public SpecialPanel( LockableListModel items )
		{
			super( "Sign-Specific Stuffs", items, true );
			setButtons( new String [] { "buy one", "buy multiple" },
				new ActionListener [] { new BuyListener( false ), new BuyListener( true ) } );
		}

		private class BuyListener implements ActionListener
		{
			private boolean purchaseMultiple;

			public BuyListener( boolean purchaseMultiple )
			{	this.purchaseMultiple = purchaseMultiple;
			}

			public void actionPerformed( ActionEvent e )
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
	}

	private class ClosetManagePanel extends MultiButtonPanel
	{
		public ClosetManagePanel( String title, LockableListModel elementModel, boolean useFilters )
		{	super( title, elementModel, true );
		}

		protected abstract class TransferListener implements ActionListener
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
				Object [] items = getDesiredItems( description );
				this.requests = new Runnable[ !retrieveFromClosetFirst || description.equals( "Bagging" ) ? 1 : 2 ];

				if ( retrieveFromClosetFirst )
					requests[0] = new ItemStorageRequest( client, ItemStorageRequest.CLOSET_TO_INVENTORY, items );

				return items;
			}

			public void initializeTransfer()
			{	(new RequestThread( requests )).start();
			}
		}

		protected class PutInClosetListener extends TransferListener
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

		protected class AutoSellListener extends TransferListener
		{
			private int sellType;

			public AutoSellListener( boolean retrieveFromClosetFirst, int sellType, ShowDescriptionList elementList )
			{
				super( sellType == AutoSellRequest.AUTOSELL ? "Autoselling" : "Automalling", retrieveFromClosetFirst, elementList );
				this.sellType = sellType;
			}

			public void actionPerformed( ActionEvent e )
			{
				if ( sellType == AutoSellRequest.AUTOMALL && !KoLCharacter.hasStore() )
				{
					client.updateDisplay( ERROR_STATE, "You don't own a store in the mall.");
					return;
				}

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

		protected class GiveToClanListener extends TransferListener
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

		protected class PutOnDisplayListener extends TransferListener
		{
			public PutOnDisplayListener( boolean retrieveFromClosetFirst, ShowDescriptionList elementList )
			{	super( "Showcasing", retrieveFromClosetFirst, elementList );
			}

			public void actionPerformed( ActionEvent e )
			{
				Object [] items = initialSetup();
				if ( items == null )
					return;

				if ( !KoLCharacter.hasDisplayCase() )
				{
					client.updateDisplay( ERROR_STATE, "You don't own a display case in the Cannon Museum.");
					return;
				}

				requests[ requests.length - 1 ] = new MuseumRequest( client, items, true );
				initializeTransfer();
			}
		}
	}

	private class OutsideClosetPanel extends ClosetManagePanel
	{
		public OutsideClosetPanel()
		{
			super( "Inside Inventory", KoLCharacter.getInventory(), false );
			elementList.setCellRenderer( AdventureResult.getAutoSellCellRenderer() );
			setButtons( new String [] { "closet", "autosell", "automall", "museum", "stash" },
				new ActionListener [] {
					new PutInClosetListener( false, elementList ),
					new AutoSellListener( false, AutoSellRequest.AUTOSELL, elementList ),
					new AutoSellListener( false, AutoSellRequest.AUTOMALL, elementList ),
					new PutOnDisplayListener( false, elementList ),
					new GiveToClanListener( false, elementList ) } );
		}
	}

	private class InsideClosetPanel extends ClosetManagePanel
	{
		public InsideClosetPanel()
		{
			super( "Inside Closet", KoLCharacter.getCloset(), false );
			elementList.setCellRenderer( AdventureResult.getAutoSellCellRenderer() );
			setButtons( new String [] { "backpack", "autosell", "automall", "museum", "stash" },
				new ActionListener [] {
					new PutInClosetListener( true, elementList ),
					new AutoSellListener( true, AutoSellRequest.AUTOSELL, elementList ),
					new AutoSellListener( true, AutoSellRequest.AUTOMALL, elementList ),
					new PutOnDisplayListener( true, elementList ),
					new GiveToClanListener( true, elementList ) } );
		}
	}

	/**
	 * Internal class used to handle everything related to
	 * creating items; this allows creating of items,
	 * which usually get resold in malls.
	 */

	private class CreateItemPanel extends MultiButtonPanel
	{
		public CreateItemPanel()
		{
			super( "Create an Item", ConcoctionsDatabase.getConcoctions(), false );

			elementList.setSelectionMode( ListSelectionModel.SINGLE_SELECTION );
			elementList.setCellRenderer( AdventureResult.getConsumableCellRenderer( KoLCharacter.canEat(), KoLCharacter.canDrink(), true ) );
			setButtons( new String [] { "create one", "create multiple", "refresh list" },
				new ActionListener [] { new CreateListener( false ), new CreateListener( true ),
				new RequestButton( "Refresh Items", new EquipmentRequest( client, EquipmentRequest.CLOSET ) ) } );

			JCheckBox [] filters = new JCheckBox[3];
			filters[0] = new FilterCheckBox( filters, elementList, "Show food", KoLCharacter.canEat() );
			filters[1] = new FilterCheckBox( filters, elementList, "Show drink", KoLCharacter.canDrink() );
			filters[2] = new FilterCheckBox( filters, elementList, "Show other", true );

			for ( int i = 0; i < 3; ++i )
				optionPanel.add( filters[i] );
		}

		private class CreateListener implements ActionListener
		{
			private boolean createMultiple;

			public CreateListener( boolean createMultiple )
			{	this.createMultiple = createMultiple;
			}

			public void actionPerformed( ActionEvent e )
			{
				Object selected = elementList.getSelectedValue();

				if ( selected == null )
					return;

				client.updateDisplay( DISABLE_STATE, "Verifying ingredients..." );
				ItemCreationRequest selection = (ItemCreationRequest) selected;
				selection.setQuantityNeeded( createMultiple ? getQuantity( "Creating multiple " + selection.getName() + "...", selection.getQuantityNeeded() ) : 1 );

				(new RequestThread( selection )).start();
			}
		}
	}

	protected class FilterCheckBox extends JCheckBox implements ActionListener
	{
		private JCheckBox [] filters;
		private ShowDescriptionList elementList;

		public FilterCheckBox( JCheckBox [] filters, ShowDescriptionList elementList, String label, boolean isSelected )
		{
			super( label, isSelected );
			addActionListener( this );

			this.filters = filters;
			this.elementList = elementList;
		}

		public void actionPerformed( ActionEvent e )
		{	elementList.setCellRenderer( AdventureResult.getConsumableCellRenderer( filters[0].isSelected(), filters[1].isSelected(), filters[2].isSelected() ) );
		}
	}

	/**
	 * The main method used in the event of testing the way the
	 * user interface looks.  This allows the UI to be tested
	 * without having to constantly log in and out of KoL.
	 */

	public static void main( String [] args )
	{	(new CreateFrameRunnable( ItemManageFrame.class )).run();
	}
}
