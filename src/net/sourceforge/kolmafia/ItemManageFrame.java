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
	private JPanel using, selling, storing;
	private SortedListModel inventory, closet, usableItems, concoctions;

	/**
	 * Constructs a new <code>ItemManageFrame</code> and inserts all
	 * of the necessary panels into a tabular layout for accessibility.
	 *
	 * @param	client	The client to be notified in the event of error.
	 */

	public ItemManageFrame( KoLmafia client )
	{
		super( "KoLmafia: Item Management", client );

		this.inventory = client == null ? new SortedListModel() : client.getInventory();
		this.closet = client == null ? new SortedListModel() : client.getCloset();
		this.usableItems = client == null ? new SortedListModel() : client.getUsableItems();
		this.concoctions = new SortedListModel();

		if ( client != null )
			refreshConcoctionsList();

		tabs = new JTabbedPane();
		using = new ConsumePanel();
		selling = new SellPanel();
		storing = new StoragePanel();

		tabs.addTab( "Use & Create", using );
		tabs.addTab( "Sell & Create", selling );
		tabs.addTab( "Closet & Case", storing );

		getContentPane().setLayout( new CardLayout( 10, 10 ) );
		getContentPane().add( tabs, "" );
		addWindowListener( new ReturnFocusAdapter() );
		setDefaultCloseOperation( HIDE_ON_CLOSE );

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
		refreshItem.setEnabled( isEnabled );
		using.setEnabled( isEnabled );
		selling.setEnabled( isEnabled );
		storing.setEnabled( isEnabled );
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
			{	super( "Usable Items", "use one", "use multiple", usableItems );
			}

			protected void actionConfirmed()
			{	(new ConsumeItemRequestThread(false)).start();
			}

			protected void actionCancelled()
			{	(new ConsumeItemRequestThread(true)).start();
			}

			public void setEnabled( boolean isEnabled )
			{
				super.setEnabled( isEnabled );
				elementList.setEnabled( isEnabled );
			}

			/**
			 * In order to keep the user interface from freezing (or at
			 * least appearing to freeze), this internal class is used
			 * to actually autosell the items.
			 */

			private class ConsumeItemRequestThread extends RequestThread
			{
				private boolean useMultiple;

				public ConsumeItemRequestThread( boolean useMultiple )
				{	this.useMultiple = useMultiple;
				}

				public void run()
				{
					Object [] items = elementList.getSelectedValues();

					for ( int i = 0; i < items.length; ++i )
						consumeItem( (AdventureResult) items[i] );
				}

				private void consumeItem( AdventureResult currentItem )
				{
					try
					{
						int consumptionType = TradeableItemDatabase.getConsumptionType( currentItem.getName() );
						int consumptionCount = useMultiple ? df.parse( JOptionPane.showInputDialog(
							"Using multiple " + currentItem.getName() + "..." ) ).intValue() : 1;

						if ( consumptionType == ConsumeItemRequest.CONSUME_MULTIPLE )
							client.makeRequest( new ConsumeItemRequest( client, new AdventureResult( currentItem.getItemID(), consumptionCount ) ), 1 );
						else
							client.makeRequest( new ConsumeItemRequest( client, new AdventureResult( currentItem.getItemID(), 1 ) ), consumptionCount );

					}
					catch ( Exception e )
					{
						// If the number placed inside of the count list was not
						// an actual integer value, pretend nothing happened.
						// Using exceptions for flow control is bad style, but
						// this will be fixed once we add functionality.
					}
				}
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
				super( "Inside Inventory", "autosell", "send to store", inventory );
				elementList.setCellRenderer( AdventureResult.getAutoSellCellRenderer() );
			}

			protected void actionConfirmed()
			{	(new AutoSellRequestThread( AutoSellRequest.AUTOSELL )).start();
			}

			protected void actionCancelled()
			{	(new AutoSellRequestThread( AutoSellRequest.AUTOMALL )).start();
			}

			public void setEnabled( boolean isEnabled )
			{
				super.setEnabled( isEnabled );
				elementList.setEnabled( isEnabled );
			}

			/**
			 * In order to keep the user interface from freezing (or at
			 * least appearing to freeze), this internal class is used
			 * to actually autosell the items.
			 */

			private class AutoSellRequestThread extends RequestThread
			{
				private int sellType;
				private boolean finishedSelling;

				public AutoSellRequestThread( int sellType )
				{
					this.sellType = sellType;
					this.finishedSelling = false;
				}

				public void run()
				{
					if ( JOptionPane.NO_OPTION == JOptionPane.showConfirmDialog( null,
						"Are you sure you would like to sell the selected items?",
							"Sell request nag screen!", JOptionPane.YES_NO_OPTION ) )
								return;

					Object [] items = elementList.getSelectedValues();
					AdventureResult currentItem;

					for ( int i = 0; !finishedSelling && i < items.length; ++i )
						sell( (AdventureResult) items[i] );

					client.updateDisplay( ENABLED_STATE, "Transaction complete." );
				}

				private void sell( AdventureResult currentItem )
				{
					switch ( sellType )
					{
						case AutoSellRequest.AUTOSELL:
							client.makeRequest( new AutoSellRequest( client, currentItem ), 1 );
							break;
						case AutoSellRequest.AUTOMALL:
							client.makeRequest( new AutoSellRequest( client, currentItem, 999999999 ), 1 );
							break;
					}

				}
			}
		}
	}

	/**
	 * Internal class used to handle everything related to
	 * placing items into the closet and taking items from
	 * the closet.
	 */

	private class StoragePanel extends JPanel
	{
		private JList availableList, closetList;
		private ItemManagePanel inventoryPanel, closetPanel;

		public StoragePanel()
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
				super( "Inside Inventory", "put in closet", "put in case", inventory );
				availableList = elementList;
			}

			protected void actionConfirmed()
			{	(new InventoryStorageThread( false )).start();
			}

			protected void actionCancelled()
			{	(new InventoryStorageThread( true )).start();
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
				super( "Inside Closet", "put in bag", "put in case", closet );
				closetList = elementList;
			}

			protected void actionConfirmed()
			{	(new ClosetStorageThread( false )).start();
			}

			protected void actionCancelled()
			{	(new ClosetStorageThread( true )).start();
			}

			public void setEnabled( boolean isEnabled )
			{
				super.setEnabled( isEnabled );
				closetList.setEnabled( isEnabled );
			}
		}

		/**
		 * In order to keep the user interface from freezing (or at
		 * least appearing to freeze), this internal class is used
		 * to actually move items around in the inventory.
		 */

		private class InventoryStorageThread extends RequestThread
		{
			private boolean isDisplayCase;

			public InventoryStorageThread( boolean isDisplayCase )
			{	this.isDisplayCase = isDisplayCase;
			}

			public void run()
			{
				Object [] items = availableList.getSelectedValues();
				Runnable request = isDisplayCase ? (Runnable) new MuseumRequest( client, true, items ) :
					(Runnable) new ItemStorageRequest( client, ItemStorageRequest.INVENTORY_TO_CLOSET, items );

				client.makeRequest( request, 1 );
				client.updateDisplay( ENABLED_STATE, "Items moved." );
			}
		}

		/**
		 * In order to keep the user interface from freezing (or at
		 * least appearing to freeze), this internal class is used
		 * to actually move items around in the inventory.
		 */

		private class ClosetStorageThread extends RequestThread
		{
			private boolean isDisplayCase;

			public ClosetStorageThread( boolean isDisplayCase )
			{	this.isDisplayCase = isDisplayCase;
			}

			public void run()
			{
				Object [] items = closetList.getSelectedValues();
				client.makeRequest( new ItemStorageRequest( client, ItemStorageRequest.CLOSET_TO_INVENTORY, items ), 1 );

				if ( isDisplayCase )
					client.makeRequest( new MuseumRequest( client, true, items ), 1 );

				client.updateDisplay( ENABLED_STATE, "Items moved." );
			}
		}
	}

	public void refreshConcoctionsList()
	{
		concoctions.clear();

		List materialsList = (List) inventory.clone();

		if ( client != null )
		{
			String useClosetForCreationSetting = client.getSettings().getProperty( "useClosetForCreation" );

			if ( useClosetForCreationSetting != null && useClosetForCreationSetting.equals( "true" ) )
			{
				List closetList = (List) client.getCloset();
				for ( int i = 0; i < closetList.size(); ++i )
					AdventureResult.addResultToList( materialsList, (AdventureResult) closetList.get(i) );
			}
		}

		concoctions.addAll( ConcoctionsDatabase.getConcoctions( client, materialsList ) );
	}

	/**
	 * Internal class used to handle everything related to
	 * creating items; this allows creating of items,
	 * which usually get resold in malls.
	 */

	private class CreateItemPanel extends ItemManagePanel
	{
		public CreateItemPanel()
		{	super( "Create an Item", "create one", "create multiple", concoctions );
		}

		protected void actionConfirmed()
		{	(new ItemCreationRequestThread(false)).start();
		}

		public void actionCancelled()
		{	(new ItemCreationRequestThread(true)).start();
		}

		public void setEnabled( boolean isEnabled )
		{
			super.setEnabled( isEnabled );
			elementList.setEnabled( isEnabled );
		}

		private class ItemCreationRequestThread extends RequestThread
		{
			private boolean useMultiple;

			public ItemCreationRequestThread( boolean useMultiple )
			{	this.useMultiple = useMultiple;
			}

			public void run()
			{
				client.updateDisplay( DISABLED_STATE, "Verifying ingredients..." );

				Object selected = elementList.getSelectedValue();

				try
				{
					if ( selected instanceof ItemCreationRequest )
					{
						ItemCreationRequest selection = (ItemCreationRequest) selected;

						String itemName = selection.getName();
						int creationCount = useMultiple ? df.parse( JOptionPane.showInputDialog(
							"Creating multiple " + itemName + "...", String.valueOf( selection.getQuantityNeeded() ) ) ).intValue() : 1;

						if ( creationCount > 0 )
						{
							selection.setQuantityNeeded( creationCount );
							client.makeRequest( selection, 1 );
						}
					}
					else
					{
						StarChartRequest selection = (StarChartRequest) selected;

						String itemName = selection.getName();
						int creationCount = useMultiple ? df.parse( JOptionPane.showInputDialog(
							"Creating multiple " + itemName + "...", String.valueOf( selection.getQuantityNeeded() ) ) ).intValue() : 1;

						if ( creationCount > 0 )
						{
							selection.setQuantityNeeded( creationCount );
							client.makeRequest( selection, 1 );
						}
					}
				}
				catch ( Exception e )
				{
					// If the number placed inside of the count list was not
					// an actual integer value, pretend nothing happened.
					// Using exceptions for flow control is bad style, but
					// this will be fixed once we add functionality.
				}

				if ( client.permitsContinue() )
					client.updateDisplay( ENABLED_STATE, "Items created." );
			}
		}
	}

	private class ListRefreshListener implements ActionListener
	{
		public void actionPerformed( ActionEvent e )
		{	(new ListRefreshThread()).start();
		}

		private class ListRefreshThread extends RequestThread
		{
			public void run()
			{
				(new EquipmentRequest( client, EquipmentRequest.CLOSET )).run();
				refreshConcoctionsList();
				client.updateDisplay( ENABLED_STATE, "Lists refreshed." );
			}
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
