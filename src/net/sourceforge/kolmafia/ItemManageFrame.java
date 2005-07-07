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

// event listeners
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

// containers
import javax.swing.JList;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JComboBox;
import javax.swing.JTabbedPane;
import javax.swing.BorderFactory;
import javax.swing.JMenuBar;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;

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
	private JMenuItem refreshItem;
	private JPanel using, selling, museum, stash;

	/**
	 * Constructs a new <code>ItemManageFrame</code> and inserts all
	 * of the necessary panels into a tabular layout for accessibility.
	 *
	 * @param	client	The client to be notified in the event of error.
	 */

	public ItemManageFrame( KoLmafia client )
	{
		super( client, "KoLmafia: Item Management" );

		tabs = new JTabbedPane();
		using = new ConsumePanel();
		selling = new SellPanel();
		museum = new MuseumStoragePanel();
		stash = new ClanStoragePanel();

		tabs.addTab( "Use", using );
		tabs.addTab( "Sell", selling );
		tabs.addTab( "Dicase", museum );
		tabs.addTab( "Clash", stash );

		getContentPane().setLayout( new CardLayout( 10, 10 ) );
		getContentPane().add( tabs, "" );
		addMenuBar();
	}

	private void addMenuBar()
	{
		JMenuBar menuBar = new JMenuBar();
		this.setJMenuBar( menuBar );

		JMenu fileMenu = new JMenu( "Options" );
		fileMenu.setMnemonic( KeyEvent.VK_O );
		menuBar.add( fileMenu );

		refreshItem = new JMenuItem( "Refresh Lists", KeyEvent.VK_R );
		refreshItem.addActionListener( new ListRefreshListener() );
		fileMenu.add( refreshItem );

		addHelpMenu( menuBar );
	}

	/**
	 * Auxilary method used to enable and disable a frame.  By default,
	 * this attempts to toggle the enable/disable status on all tabs.
	 *
	 * @param	isEnabled	<code>true</code> if the frame is to be re-enabled
	 */

	public void setEnabled( boolean isEnabled )
	{
		super.setEnabled( isEnabled );

		if ( refreshItem != null )
			refreshItem.setEnabled( isEnabled );

		if ( using != null )
			using.setEnabled( isEnabled );

		if ( selling != null )
			selling.setEnabled( isEnabled );

		if ( museum != null )
			museum.setEnabled( isEnabled );

		if ( stash != null )
			stash.setEnabled( isEnabled );
	}

	/**
	 * Internal class used to handle everything related to
	 * using up consumable items.
	 */

	private class ConsumePanel extends JPanel
	{
		private ItemManagePanel consumePanel, createPanel;
		private JList usableItemList;

		public ConsumePanel()
		{
			setLayout( new GridLayout( 2, 1, 10, 10 ) );

			consumePanel = new ConsumeItemPanel();
			createPanel = new CreateItemPanel();

			add( consumePanel );
			add( createPanel );
		}

		public void setEnabled( boolean isEnabled )
		{
			super.setEnabled( isEnabled );
			consumePanel.setEnabled( isEnabled );
			createPanel.setEnabled( isEnabled );
		}

		private class ConsumeItemPanel extends ItemManagePanel
		{
			public ConsumeItemPanel()
			{	super( "Usable Items", "use one", "use multiple", client.getUsableItems() );
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

				int consumptionType, consumptionCount;
				AdventureResult currentItem;

				Runnable [] requests = new Runnable[ items.length ];
				int [] repeatCount = new int[ items.length ];

				for ( int i = 0; i < items.length; ++i )
				{
					try
					{
						currentItem = (AdventureResult) items[i];

						consumptionType = TradeableItemDatabase.getConsumptionType( currentItem.getName() );
						consumptionCount = useMultiple ? df.parse( JOptionPane.showInputDialog(
							"Using multiple " + currentItem.getName() + "..." ) ).intValue() : 1;

						requests[i] = consumptionType == ConsumeItemRequest.CONSUME_MULTIPLE ?
							new ConsumeItemRequest( client, currentItem.getInstance( consumptionCount ) ) :
							new ConsumeItemRequest( client, currentItem.getInstance( 1 ) );

						repeatCount[i] = consumptionType == ConsumeItemRequest.CONSUME_MULTIPLE ? 1 : consumptionCount;
					}
					catch ( Exception e )
					{
						// If the number placed inside of the count list was not
						// an actual integer value, pretend nothing happened.
						// Using exceptions for flow control is bad style, but
						// this will be fixed once we add functionality.
					}
				}

				(new RequestThread( requests, repeatCount )).start();
			}

			public void setEnabled( boolean isEnabled )
			{
				super.setEnabled( isEnabled );
				elementList.setEnabled( isEnabled );
			}
		}
	}

	/**
	 * Internal class used to handle everything related to
	 * selling items, including item creation.
	 */

	private class SellPanel extends JPanel
	{
		private ItemManagePanel sellPanel, createPanel;

		public SellPanel()
		{
			setLayout( new GridLayout( 2, 1, 10, 10 ) );

			sellPanel = new SellItemPanel();
			createPanel = new CreateItemPanel();

			add( sellPanel );
			add( createPanel );
		}

		public void setEnabled( boolean isEnabled )
		{
			super.setEnabled( isEnabled );
			sellPanel.setEnabled( isEnabled );
			createPanel.setEnabled( isEnabled );
		}

		/**
		 * Internal class used to handle everything related to
		 * selling items; this allows autoselling of items as
		 * well as placing item inside of a store.
		 */

		private class SellItemPanel extends ItemManagePanel
		{
			public SellItemPanel()
			{
				super( "Inside Inventory", "autosell", "send to store", client == null ? new SortedListModel() : client.getInventory() );
				elementList.setCellRenderer( AdventureResult.getAutoSellCellRenderer() );
			}

			protected void actionConfirmed()
			{	sell( AutoSellRequest.AUTOSELL );
			}

			protected void actionCancelled()
			{	sell( AutoSellRequest.AUTOMALL );
			}

			public void setEnabled( boolean isEnabled )
			{
				super.setEnabled( isEnabled );
				elementList.setEnabled( isEnabled );
			}

			private void sell( int sellType )
			{
				if ( JOptionPane.NO_OPTION == JOptionPane.showConfirmDialog( null,
					"Are you sure you would like to sell the selected items?",
						"Sell request nag screen!", JOptionPane.YES_NO_OPTION ) )
							return;

				Object [] items = elementList.getSelectedValues();
				Runnable [] requests = new Runnable[ items.length ];

				for ( int i = 0; i < items.length; ++i )
					requests[i] = sellType == AutoSellRequest.AUTOSELL ? new AutoSellRequest( client, (AdventureResult) items[i] ) :
						new AutoSellRequest( client, (AdventureResult) items[i], 999999999 );

				(new RequestThread( requests )).start();
			}
		}
	}

	/**
	 * Internal class used to handle everything related to
	 * placing items into the display case.
	 */

	private class MuseumStoragePanel extends JPanel
	{
		private JList availableList, closetList;
		private ItemManagePanel inventoryPanel, closetPanel;

		public MuseumStoragePanel()
		{
			setLayout( new GridLayout( 2, 1, 10, 10 ) );

			inventoryPanel = new OutsideClosetPanel();
			closetPanel = new InsideClosetPanel();

			add( inventoryPanel );
			add( closetPanel );
		}

		public void setEnabled( boolean isEnabled )
		{
			super.setEnabled( isEnabled );
			inventoryPanel.setEnabled( isEnabled );
			closetPanel.setEnabled( isEnabled );
		}

		private class OutsideClosetPanel extends ItemManagePanel
		{
			public OutsideClosetPanel()
			{
				super( "Inside Inventory", "put in closet", "put in case", client == null ? new SortedListModel() : client.getInventory() );
				availableList = elementList;
			}

			protected void actionConfirmed()
			{	(new RequestThread( new ItemStorageRequest( client, ItemStorageRequest.INVENTORY_TO_CLOSET, availableList.getSelectedValues() ) )).start();
			}

			protected void actionCancelled()
			{	(new RequestThread( new MuseumRequest( client, true, availableList.getSelectedValues() ) )).start();
			}

			public void setEnabled( boolean isEnabled )
			{
				super.setEnabled( isEnabled );
				availableList.setEnabled( isEnabled );
			}
		}

		private class InsideClosetPanel extends ItemManagePanel
		{
			public InsideClosetPanel()
			{
				super( "Inside Closet", "put in bag", "put in case", client == null ? new SortedListModel() : client.getCloset() );
				closetList = elementList;
			}

			protected void actionConfirmed()
			{	(new RequestThread( new ItemStorageRequest( client, ItemStorageRequest.CLOSET_TO_INVENTORY, closetList.getSelectedValues() ) )).start();
			}

			protected void actionCancelled()
			{
				Runnable [] requests = new Runnable[2];
				requests[0] = new ItemStorageRequest( client, ItemStorageRequest.CLOSET_TO_INVENTORY, closetList.getSelectedValues() );
				requests[1] = new MuseumRequest( client, true, closetList.getSelectedValues() );

				(new RequestThread( requests )).start();
			}

			public void setEnabled( boolean isEnabled )
			{
				super.setEnabled( isEnabled );
				closetList.setEnabled( isEnabled );
			}
		}
	}

	/**
	 * Internal class used to handle everything related to
	 * placing items into the closet and the clan stash.
	 */

	private class ClanStoragePanel extends JPanel
	{
		private JList availableList, closetList;
		private ItemManagePanel inventoryPanel, closetPanel;

		public ClanStoragePanel()
		{
			setLayout( new GridLayout( 2, 1, 10, 10 ) );

			inventoryPanel = new OutsideClosetPanel();
			closetPanel = new InsideClosetPanel();

			add( inventoryPanel );
			add( closetPanel );
		}

		public void setEnabled( boolean isEnabled )
		{
			super.setEnabled( isEnabled );
			inventoryPanel.setEnabled( isEnabled );
			closetPanel.setEnabled( isEnabled );
		}

		private class OutsideClosetPanel extends ItemManagePanel
		{
			public OutsideClosetPanel()
			{
				super( "Inside Inventory", "put in closet", "put in stash", client == null ? new SortedListModel() : client.getInventory() );
				availableList = elementList;
			}

			protected void actionConfirmed()
			{	(new RequestThread( new ItemStorageRequest( client, ItemStorageRequest.INVENTORY_TO_CLOSET, availableList.getSelectedValues() ) )).start();
			}

			protected void actionCancelled()
			{	(new RequestThread( new ClanStashRequest( client, availableList.getSelectedValues(), ClanStashRequest.ITEMS_TO_STASH ) )).start();
			}

			public void setEnabled( boolean isEnabled )
			{
				super.setEnabled( isEnabled );
				availableList.setEnabled( isEnabled );
			}
		}

		private class InsideClosetPanel extends ItemManagePanel
		{
			public InsideClosetPanel()
			{
				super( "Inside Closet", "put in bag", "put in stash", client == null ? new SortedListModel() : client.getCloset() );
				closetList = elementList;
			}

			protected void actionConfirmed()
			{	(new RequestThread( new ItemStorageRequest( client, ItemStorageRequest.CLOSET_TO_INVENTORY, closetList.getSelectedValues() ) )).start();
			}

			protected void actionCancelled()
			{
				Runnable [] requests = new Runnable[2];
				requests[0] = new ItemStorageRequest( client, ItemStorageRequest.CLOSET_TO_INVENTORY, closetList.getSelectedValues() );
				requests[1] = new ClanStashRequest( client, closetList.getSelectedValues(), ClanStashRequest.ITEMS_TO_STASH );

				(new RequestThread( requests )).start();
			}

			public void setEnabled( boolean isEnabled )
			{
				super.setEnabled( isEnabled );
				closetList.setEnabled( isEnabled );
			}
		}
	}

	/**
	 * Internal class used to handle everything related to
	 * creating items; this allows creating of items,
	 * which usually get resold in malls.
	 */

	private class CreateItemPanel extends ItemManagePanel
	{
		public CreateItemPanel()
		{	super( "Create an Item", "create one", "create multiple", ConcoctionsDatabase.getConcoctions() );
		}

		protected void actionConfirmed()
		{	create( false );
		}

		public void actionCancelled()
		{	create( true );
		}

		public void setEnabled( boolean isEnabled )
		{
			super.setEnabled( isEnabled );
			elementList.setEnabled( isEnabled );
		}

		private void create( boolean createMultiple )
		{
			client.updateDisplay( DISABLED_STATE, "Verifying ingredients..." );
			Object selected = elementList.getSelectedValue();

			try
			{
				ItemCreationRequest selection = (ItemCreationRequest) selected;

				String itemName = selection.getName();
				int creatable = selection.getQuantityNeeded();
				int creationCount = createMultiple ? df.parse( JOptionPane.showInputDialog(
					"Creating multiple " + itemName + "...", String.valueOf( selection.getQuantityNeeded() ) ) ).intValue() : 1;

				if ( creationCount > creatable )
					creationCount = creatable;

				if ( creationCount > 0 )
				{
					selection.setQuantityNeeded( creationCount );
					(new RequestThread( selection )).start();
				}

			}
			catch ( Exception e )
			{
				// If the number placed inside of the count
				// list was not an actual integer value,
				// pretend nothing happened.  Using exceptions
				// for flow control is bad style, but this will
				// be fixed once we add functionality.
                                client.updateDisplay( ENABLED_STATE, "" );
			}
		}
	}

	private class ListRefreshListener implements ActionListener
	{
		public void actionPerformed( ActionEvent e )
		{	(new RequestThread( new EquipmentRequest( client, EquipmentRequest.CLOSET ) )).start();
		}
	}

	/**
	 * The main method used in the event of testing the way the
	 * user interface looks.  This allows the UI to be tested
	 * without having to constantly log in and out of KoL.
	 */

	public static void main( String [] args )
	{
		KoLFrame uitest = new ItemManageFrame( null );
		uitest.pack();  uitest.setVisible( true );  uitest.requestFocus();
	}
}
