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

/**
 * Copyright (c) 2003, Spellcast development team
 * http://spellcast.dev.java.net/
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
 *  [3] Neither the name "Spellcast development team" nor the names of
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
import javax.swing.ListSelectionModel;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

// containers
import javax.swing.Box;
import javax.swing.JList;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JButton;
import javax.swing.JTextField;
import javax.swing.JComboBox;
import javax.swing.JScrollPane;
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
	private JPanel using, selling, storing;
	private SortedListModel concoctions;

	/**
	 * Constructs a new <code>ItemManageFrame</code> and inserts all
	 * of the necessary panels into a tabular layout for accessibility.
	 *
	 * @param	client	The client to be notified in the event of error.
	 */

	public ItemManageFrame( KoLmafia client )
	{
		super( "KoLmafia: " + ((client == null) ? "UI Test" : client.getLoginName()) +
			" (Item Management)", client );

		concoctions = new SortedListModel();

		if ( client != null )
			refreshConcoctionsList();

		tabs = new JTabbedPane();
		using = new ConsumePanel();
		selling = new SellPanel();
		storing = new StoragePanel();

		tabs.addTab( "Use & Create", using );
		tabs.addTab( "Sell & Create", selling );
		tabs.addTab( "Closet & Stash", storing );

		getContentPane().setLayout( new CardLayout( 10, 10 ) );
		getContentPane().add( tabs, " " );
		addWindowListener( new ReturnFocusAdapter() );
		setDefaultCloseOperation( HIDE_ON_CLOSE );

		addMenuBar();
	}

	private void addMenuBar()
	{
		JMenuBar menuBar = new JMenuBar();
		this.setJMenuBar( menuBar );

		JMenu fileMenu = new JMenu("File");
		fileMenu.setMnemonic( KeyEvent.VK_F );
		menuBar.add( fileMenu );

		JMenuItem refreshItem = new JMenuItem( "Refresh Lists", KeyEvent.VK_R );
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
			{	super( "Usable Items", "use one", "use multiple", client == null ? new LockableListModel() : client.getUsableItems().getMirrorImage() );
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

			private class ConsumeItemRequestThread extends Thread
			{
				private boolean useMultiple;

				public ConsumeItemRequestThread( boolean useMultiple )
				{
					super( "Consume-Request-Thread" );
					setDaemon( true );
					this.useMultiple = useMultiple;
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
							client.makeRequest( new ConsumeItemRequest( client, consumptionType,
								new AdventureResult( currentItem.getItemID(), consumptionCount ) ), 1 );
						else
							client.makeRequest( new ConsumeItemRequest( client, consumptionType,
								new AdventureResult( currentItem.getItemID(), 1 ) ), consumptionCount );

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
			{	super( "Tradeable Items", "autosell", "send to store", client == null ? new LockableListModel() : client.getInventory().getMirrorImage() );
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

			private class AutoSellRequestThread extends Thread
			{
				private int sellType;
				private boolean finishedSelling;

				public AutoSellRequestThread( int sellType )
				{
					super( "AutoSell-Request-Thread" );
					setDaemon( true );
					this.sellType = sellType;
					this.finishedSelling = false;
				}

				public void run()
				{
					Object [] items = elementList.getSelectedValues();
					AdventureResult currentItem;

					for ( int i = 0; !finishedSelling && i < items.length; ++i )
						sell( (AdventureResult) items[i] );

					client.updateDisplay( ENABLED_STATE, " " );
				}

				private void sell( AdventureResult currentItem )
				{
					switch ( sellType )
					{
						case AutoSellRequest.AUTOSELL:
							client.makeRequest( new AutoSellRequest( client, currentItem ), 1 );
							break;
						case AutoSellRequest.AUTOMALL:
						{
							try
							{
								String promptForPriceString = client.getSettings().getProperty( "promptForPrice" );
								boolean promptForPrice = promptForPriceString == null ? true :
									Boolean.valueOf( promptForPriceString ).booleanValue();

								int desiredPrice = promptForPrice ? df.parse( JOptionPane.showInputDialog(
									"Price for " + currentItem.getName() + "?" ) ).intValue() : 999999999;

								if ( desiredPrice >= 10 )
									client.makeRequest( new AutoSellRequest( client, currentItem, desiredPrice ), 1 );
								else
									finishedSelling = true;
							}
							catch ( Exception e )
							{
								// If the number placed inside of the count list was not
								// an actual integer value, then the user is probably
								// trying to break the input so that they can stop the
								// sell process.  Therefore, assume they're done.

								finishedSelling = true;
							}

							break;
						}
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
				super( "Inside Inventory", "put in closet", "put in stash", client == null ? new LockableListModel() : client.getInventory().getMirrorImage() );
				availableList = elementList;
			}

			protected void actionConfirmed()
			{	(new ItemStorageRequestThread( ItemStorageRequest.INVENTORY_TO_CLOSET )).start();
			}

			protected void actionCancelled()
			{	(new ItemStorageRequestThread( ItemStorageRequest.INVENTORY_TO_STASH )).start();
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
				super( "Inside Closet", "take out", "put in stash", client == null ? new LockableListModel() : client.getCloset().getMirrorImage() );
				closetList = elementList;
			}

			protected void actionConfirmed()
			{	(new ItemStorageRequestThread( ItemStorageRequest.CLOSET_TO_INVENTORY )).start();
			}

			protected void actionCancelled()
			{	(new ItemStorageRequestThread( ItemStorageRequestThread.CLOSET_TO_STASH )).start();
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
		 * to actually autosell the item
		 */

		private class ItemStorageRequestThread extends Thread
		{
			private int moveType;
			public static final int CLOSET_TO_STASH = Integer.MAX_VALUE;

			public ItemStorageRequestThread( int moveType )
			{
				super( "Closet-Request-Thread" );
				setDaemon( true );
				this.moveType = moveType;
			}

			public void run()
			{
				Object [] items =
					moveType == ItemStorageRequest.INVENTORY_TO_CLOSET ? availableList.getSelectedValues() :
					moveType == ItemStorageRequest.CLOSET_TO_INVENTORY ? closetList.getSelectedValues() :
					moveType == ItemStorageRequest.INVENTORY_TO_STASH ? availableList.getSelectedValues() : null;

				if ( moveType == CLOSET_TO_STASH )
				{
					items = closetList.getSelectedValues();
					client.makeRequest( new ItemStorageRequest( client, ItemStorageRequest.CLOSET_TO_INVENTORY, items ), 1 );
					client.makeRequest( new ItemStorageRequest( client, ItemStorageRequest.INVENTORY_TO_STASH, items ), 1 );
				}
				else
					client.makeRequest( new ItemStorageRequest( client, moveType, items ), 1 );

				client.updateDisplay( ENABLED_STATE, " " );
			}
		}
	}

	public void refreshConcoctionsList()
	{
		concoctions.clear();

		List materialsList = (List) client.getInventory().clone();
		String useClosetForCreationSetting = client.getSettings().getProperty( "useClosetForCreation" );

		if ( useClosetForCreationSetting != null && useClosetForCreationSetting.equals( "true" ) )
		{
			List closetList = (List) client.getCloset();
			for ( int i = 0; i < closetList.size(); ++i )
				AdventureResult.addResultToList( materialsList, (AdventureResult) closetList.get(i) );
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
		{	super( "Create an Item", "create one", "create multiple", concoctions.getMirrorImage() );
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

		private class ItemCreationRequestThread extends Thread
		{
			private boolean useMultiple;

			public ItemCreationRequestThread( boolean useMultiple )
			{
				super( "Create-Item-Thread" );
				setDaemon( true );
				this.useMultiple = useMultiple;
			}

			public void run()
			{
				Object selected = elementList.getSelectedValue();

				try
				{
					if ( selected instanceof ItemCreationRequest )
					{
						ItemCreationRequest selection = (ItemCreationRequest) selected;

						String itemName = selection.getName();
						int creationCount = useMultiple ? df.parse( JOptionPane.showInputDialog(
							"Creating multiple " + itemName + "...", "" + selection.getQuantityNeeded() ) ).intValue() : 1;

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
							"Creating multiple " + itemName + "...", "" + selection.getQuantityNeeded() ) ).intValue() : 1;

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

				client.updateDisplay( ENABLED_STATE, " " );
			}
		}
	}

	private class ListRefreshListener implements ActionListener
	{
		public void actionPerformed( ActionEvent e )
		{	(new ListRefreshThread()).start();
		}

		private class ListRefreshThread extends Thread
		{
			public ListRefreshThread()
			{
				super( "List-Refresh-Thread" );
				setDaemon( true );
			}

			public void run()
			{
				(new EquipmentRequest( client, EquipmentRequest.CLOSET )).run();
				refreshConcoctionsList();
				client.updateDisplay( ENABLED_STATE, " " );
			}
		}
	}

	/**
	 * An internal class which creates a panel which manages items.
	 * This is done because most of the item management displays
	 * are replicated.  Note that a lot of this code was borrowed
	 * directly from the ActionVerifyPanel class in the utilities
	 * package for Spellcast.
	 */

	private abstract class ItemManagePanel extends JPanel
	{
		protected JList elementList;

		public ItemManagePanel( String title, String confirmedText, String cancelledText, LockableListModel elements )
		{
			elementList = new JList( elements );
			elementList.setSelectionMode( ListSelectionModel.MULTIPLE_INTERVAL_SELECTION );
			elementList.setPrototypeCellValue( "ABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890@#$%^&*" );
			elementList.setVisibleRowCount( 8 );

			JPanel centerPanel = new JPanel();
			centerPanel.setLayout( new BorderLayout() );

			centerPanel.add( JComponentUtilities.createLabel( title, JLabel.CENTER,
				Color.black, Color.white ), BorderLayout.NORTH );
			centerPanel.add( new JScrollPane( elementList, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
				JScrollPane.HORIZONTAL_SCROLLBAR_NEVER ), BorderLayout.CENTER );

			JPanel actualPanel = new JPanel();
			actualPanel.setLayout( new BorderLayout( 20, 10 ) );
			actualPanel.add( centerPanel, BorderLayout.CENTER );
			actualPanel.add( new VerifyButtonPanel( confirmedText, cancelledText ), BorderLayout.EAST );

			setLayout( new CardLayout( 10, 10 ) );
			add( actualPanel, " " );
		}

		protected abstract void actionConfirmed();
		protected abstract void actionCancelled();

		private class VerifyButtonPanel extends JPanel
		{
			private JButton confirmedButton;
			private JButton cancelledButton;

			public VerifyButtonPanel( String confirmedText, String cancelledText )
			{
				setLayout( new BoxLayout( this, BoxLayout.Y_AXIS ) );

				// add the "confirmed" button
				confirmedButton = new JButton( confirmedText );
				confirmedButton.addActionListener(
					new ActionListener() {
						public void actionPerformed( ActionEvent e ) {
							actionConfirmed();
						}
					} );

				addButton( confirmedButton );
				add( Box.createVerticalStrut( 4 ) );

				// add the "cancelled" button
				cancelledButton = new JButton( cancelledText );
				cancelledButton.addActionListener(
					new ActionListener() {
						public void actionPerformed( ActionEvent e ) {
							actionCancelled();
						}
					} );
				addButton( cancelledButton );

				JComponentUtilities.setComponentSize( this, 120, 100 );
			}

			private void addButton( JButton buttonToAdd )
			{
				JPanel container = new JPanel();
				container.setLayout( new GridLayout() );
				container.add( buttonToAdd );
				container.setMaximumSize( new Dimension( Integer.MAX_VALUE, 24 ) );
				add( container );
			}

			public void setEnabled( boolean isEnabled )
			{
				confirmedButton.setEnabled( isEnabled );
				cancelledButton.setEnabled( isEnabled );
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
