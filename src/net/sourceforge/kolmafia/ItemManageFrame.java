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
import javax.swing.ListSelectionModel;

// containers
import javax.swing.JList;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JComboBox;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.BorderFactory;

// other imports
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

		setResizable( false );

		tabs = new JTabbedPane();
		using = new ConsumePanel();
		selling = new SellPanel();
		storing = new StoragePanel();

		tabs.addTab( "Use Items", using );
		tabs.addTab( "Sell & Create", selling );
		tabs.addTab( "Closet & Stash", storing );

		getContentPane().setLayout( new CardLayout( 10, 10 ) );
		getContentPane().add( tabs, "" );
		addWindowListener( new ReturnFocusAdapter() );
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
		for ( int i = 0; i < tabs.getTabCount(); ++i )
			tabs.setEnabledAt( i, isEnabled );

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
		private NonContentPanel foodPanel, miscPanel;
		private JList edibleItemList, usableItemList;

		public ConsumePanel()
		{
			JPanel panel = new JPanel();
			panel.setLayout( new BorderLayout( 10, 10 ) );

			foodPanel = new ConsumeFoodPanel();
			miscPanel = new ConsumeMiscPanel();

			panel.add( foodPanel, BorderLayout.NORTH );
			panel.add( miscPanel, BorderLayout.SOUTH );

			setLayout( new CardLayout( 10, 10 ) );
			add( panel, "" );
		}

		public void setEnabled( boolean isEnabled )
		{
			super.setEnabled( isEnabled );
			foodPanel.setEnabled( isEnabled );
			miscPanel.setEnabled( isEnabled );
		}

		/**
		 * Internal class used to handle everything related to
		 * selling items; this allows autoselling of items as
		 * well as placing item inside of a store.
		 */

		private class ConsumeFoodPanel extends NonContentPanel
		{
			private LockableListModel edibleItems;

			public ConsumeFoodPanel()
			{
				super( "use one", "use all" );
				setContent( null, null, null, null, true, true );

				edibleItems = client == null ? new LockableListModel() : client.getEdibleItems().getMirrorImage();
				edibleItemList = new JList( edibleItems );
				edibleItemList.setSelectionMode( ListSelectionModel.MULTIPLE_INTERVAL_SELECTION );
				edibleItemList.setPrototypeCellValue( "ABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890@#$%^&*" );
				edibleItemList.setVisibleRowCount( 8 );

				add( JComponentUtilities.createLabel( "Edible Items", JLabel.CENTER,
					Color.black, Color.white ), BorderLayout.NORTH );
				add( new JScrollPane( edibleItemList, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
					JScrollPane.HORIZONTAL_SCROLLBAR_NEVER ), BorderLayout.WEST );
			}

			public void clear()
			{
			}

			protected void actionConfirmed()
			{	(new ConsumeItemRequestThread(true, false)).start();
			}

			protected void actionCancelled()
			{	(new ConsumeItemRequestThread(true, true)).start();
			}

			public void setEnabled( boolean isEnabled )
			{
				super.setEnabled( isEnabled );
				edibleItemList.setEnabled( isEnabled );
			}
		}

		private class ConsumeMiscPanel extends NonContentPanel
		{
			private LockableListModel usableItems;

			public ConsumeMiscPanel()
			{
				super( "use one", "use all" );
				setContent( null, null, null, null, true, true );

				usableItems = client == null ? new LockableListModel() : client.getUsableItems().getMirrorImage();
				usableItemList = new JList( usableItems );
				usableItemList.setSelectionMode( ListSelectionModel.MULTIPLE_INTERVAL_SELECTION );
				usableItemList.setPrototypeCellValue( "ABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890@#$%^&*" );
				usableItemList.setVisibleRowCount( 8 );

				add( JComponentUtilities.createLabel( "Usable Items", JLabel.CENTER,
					Color.black, Color.white ), BorderLayout.NORTH );
				add( new JScrollPane( usableItemList, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
					JScrollPane.HORIZONTAL_SCROLLBAR_NEVER ), BorderLayout.WEST );
			}

			public void clear()
			{
			}

			protected void actionConfirmed()
			{	(new ConsumeItemRequestThread(false, false)).start();
			}

			protected void actionCancelled()
			{	(new ConsumeItemRequestThread(false, true)).start();
			}

			public void setEnabled( boolean isEnabled )
			{
				super.setEnabled( isEnabled );
				usableItemList.setEnabled( isEnabled );
			}
		}

		/**
		 * In order to keep the user interface from freezing (or at
		 * least appearing to freeze), this internal class is used
		 * to actually autosell the items.
		 */

		private class ConsumeItemRequestThread extends Thread
		{
			private boolean isEdibleItemRequest, useAll;

			public ConsumeItemRequestThread( boolean isEdibleItemRequest, boolean useAll )
			{
				super( "Consume-Request-Thread" );
				setDaemon( true );
				this.isEdibleItemRequest = isEdibleItemRequest;
				this.useAll = useAll;
			}

			public void run()
			{
				ItemManageFrame.this.setEnabled( false );
				Object [] items = (isEdibleItemRequest ? edibleItemList : usableItemList).getSelectedValues();
				AdventureResult currentItem;  Runnable request;

				for ( int i = 0; i < items.length; ++i )
				{
					currentItem = (AdventureResult) items[i];
					request = new ConsumeItemRequest( client, TradeableItemDatabase.getConsumptionType( currentItem.getName() ), currentItem );
					client.makeRequest( request, useAll ? currentItem.getCount() : 1 );
				}

				refreshConcoctionsList();
				updateDisplay( ENABLED_STATE, "" );
				ItemManageFrame.this.setEnabled( true );
			}
		}
	}

	/**
	 * Internal class used to handle everything related to
	 * selling items, including item creation.
	 */

	private class SellPanel extends JPanel
	{
		private NonContentPanel sellPanel, createPanel;
		private JList availableList;
		private JList concoctionsList;

		public SellPanel()
		{
			JPanel panel = new JPanel();
			panel.setLayout( new BorderLayout( 10, 10 ) );

			sellPanel = new SellItemPanel();
			createPanel = new CreateItemPanel();

			panel.add( sellPanel, BorderLayout.NORTH );
			panel.add( createPanel, BorderLayout.SOUTH );

			setLayout( new CardLayout( 10, 10 ) );
			add( panel, "" );
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

		private class SellItemPanel extends NonContentPanel
		{
			private LockableListModel available;

			public SellItemPanel()
			{
				super( "autosell", "send to store" );
				setContent( null, null, null, null, true, true );

				available = client == null ? new LockableListModel() : client.getInventory().getMirrorImage();
				availableList = new JList( available );
				availableList.setSelectionMode( ListSelectionModel.MULTIPLE_INTERVAL_SELECTION );
				availableList.setPrototypeCellValue( "ABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890@#$%^&*" );
				availableList.setVisibleRowCount( 8 );

				add( JComponentUtilities.createLabel( "Tradeable Items", JLabel.CENTER,
					Color.black, Color.white ), BorderLayout.NORTH );
				add( new JScrollPane( availableList, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
					JScrollPane.HORIZONTAL_SCROLLBAR_NEVER ), BorderLayout.WEST );
			}

			public void clear()
			{
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
				availableList.setEnabled( isEnabled );
			}

			/**
			 * In order to keep the user interface from freezing (or at
			 * least appearing to freeze), this internal class is used
			 * to actually autosell the items.
			 */

			private class AutoSellRequestThread extends Thread
			{
				private int sellType;

				public AutoSellRequestThread( int sellType )
				{
					super( "AutoSell-Request-Thread" );
					setDaemon( true );
					this.sellType = sellType;
				}

				public void run()
				{
					ItemManageFrame.this.setEnabled( false );
					Object [] items = availableList.getSelectedValues();
					AdventureResult currentItem;

					for ( int i = 0; i < items.length; ++i )
					{
						currentItem = (AdventureResult) items[i];

						switch ( sellType )
						{
							case AutoSellRequest.AUTOSELL:
								updateDisplay( DISABLED_STATE, "Autoselling " + currentItem.getName() + "..." );
								break;

							case AutoSellRequest.AUTOMALL:
								updateDisplay( DISABLED_STATE, "Placing " + currentItem.getName() + " in the mall..." );
								break;
						}

						(new AutoSellRequest( client, sellType, currentItem )).run();
					}

					refreshConcoctionsList();
					updateDisplay( ENABLED_STATE, "" );
					ItemManageFrame.this.setEnabled( true );
				}
			}
		}

		/**
		 * Internal class used to handle everything related to
		 * creating items; this allows creating of items,
		 * which usually get resold in malls.
		 */

		private class CreateItemPanel extends NonContentPanel
		{
			public CreateItemPanel()
			{
				super( "create", "refresh list" );
				setContent( null, null, null, null, true, true );

				concoctions = client == null ? new SortedListModel() : ConcoctionsDatabase.getConcoctions( client, client.getInventory() );
				concoctionsList = new JList( concoctions );
				concoctionsList.setSelectionMode( ListSelectionModel.SINGLE_SELECTION );
				concoctionsList.setPrototypeCellValue( "ABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890@#$%^&*" );
				concoctionsList.setVisibleRowCount( 8 );

				add( JComponentUtilities.createLabel( "Create an Item", JLabel.CENTER,
					Color.black, Color.white ), BorderLayout.NORTH );
				add( new JScrollPane( concoctionsList, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
					JScrollPane.HORIZONTAL_SCROLLBAR_NEVER ), BorderLayout.WEST );
			}

			public void clear()
			{
			}

			protected void actionConfirmed()
			{	(new ItemCreationRequestThread()).start();
			}

			public void actionCancelled()
			{	(new RefreshItemCreationListThread()).start();
			}

			public void setEnabled( boolean isEnabled )
			{
				super.setEnabled( isEnabled );
				concoctionsList.setEnabled( isEnabled );
			}

			private class ItemCreationRequestThread extends Thread
			{
				public void run()
				{
					updateDisplay( DISABLED_STATE, "Creating item..." );
					ItemManageFrame.this.setEnabled( false );

					Object selection = concoctionsList.getSelectedValue();

					if ( selection != null )
						((Runnable)selection).run();

					refreshConcoctionsList();
					updateDisplay( ENABLED_STATE, "" );
					ItemManageFrame.this.setEnabled( true );
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
		private NonContentPanel inventoryPanel, closetPanel;
		private JList availableList;
		private JList closetList;

		public StoragePanel()
		{
			JPanel panel = new JPanel();
			panel.setLayout( new BorderLayout( 10, 10 ) );
			inventoryPanel = new OutsideClosetPanel();
			closetPanel = new InsideClosetPanel();

			panel.add( inventoryPanel, BorderLayout.NORTH );
			panel.add( closetPanel, BorderLayout.SOUTH );

			setLayout( new CardLayout( 10, 10 ) );
			add( panel, "" );
		}

		public void setEnabled( boolean isEnabled )
		{
			super.setEnabled( isEnabled );
			inventoryPanel.setEnabled( isEnabled );
			closetPanel.setEnabled( isEnabled );
		}

		private class OutsideClosetPanel extends NonContentPanel
		{
			private LockableListModel inventory;

			public OutsideClosetPanel()
			{
				super( "put in closet", "put in stash" );
				setContent( null, null, null, null, true, true );

				inventory = client == null ? new LockableListModel() : client.getInventory().getMirrorImage();
				availableList = new JList( inventory );
				availableList.setSelectionMode( ListSelectionModel.MULTIPLE_INTERVAL_SELECTION );
				availableList.setPrototypeCellValue( "ABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890@#$%^&*" );
				availableList.setVisibleRowCount( 8 );

				add( JComponentUtilities.createLabel( "Inside Inventory", JLabel.CENTER,
					Color.black, Color.white ), BorderLayout.NORTH );
				add( new JScrollPane( availableList, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
					JScrollPane.HORIZONTAL_SCROLLBAR_NEVER ), BorderLayout.WEST );
			}

			public void clear()
			{
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

		private class InsideClosetPanel extends NonContentPanel
		{
			private LockableListModel closet;

			public InsideClosetPanel()
			{
				super( "take out", "put in stash" );
				setContent( null, null, null, null, true, true );

				closet = client == null ? new LockableListModel() : client.getCloset().getMirrorImage();
				closetList = new JList( closet );
				closetList.setSelectionMode( ListSelectionModel.MULTIPLE_INTERVAL_SELECTION );
				closetList.setPrototypeCellValue( "ABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890@#$%^&*" );
				closetList.setVisibleRowCount( 8 );

				add( JComponentUtilities.createLabel( "Inside Closet", JLabel.CENTER,
					Color.black, Color.white ), BorderLayout.NORTH );
				add( new JScrollPane( closetList, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
					JScrollPane.HORIZONTAL_SCROLLBAR_NEVER ), BorderLayout.WEST );
			}

			public void clear()
			{
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
				updateDisplay( DISABLED_STATE, "Moving items..." );
				ItemManageFrame.this.setEnabled( false );

				Object [] items =
					moveType == ItemStorageRequest.INVENTORY_TO_CLOSET ? availableList.getSelectedValues() :
					moveType == ItemStorageRequest.CLOSET_TO_INVENTORY ? closetList.getSelectedValues() :
					moveType == ItemStorageRequest.INVENTORY_TO_STASH ? availableList.getSelectedValues() : null;

				if ( moveType == CLOSET_TO_STASH )
				{
					items = closetList.getSelectedValues();
					(new ItemStorageRequest( client, ItemStorageRequest.CLOSET_TO_INVENTORY, items )).run();
					(new ItemStorageRequest( client, ItemStorageRequest.INVENTORY_TO_STASH, items )).run();
				}
				else
					(new ItemStorageRequest( client, moveType, items )).run();

				refreshConcoctionsList();
				updateDisplay( ENABLED_STATE, "" );
				ItemManageFrame.this.setEnabled( true );
			}
		}
	}

	public void refreshConcoctionsList()
	{	(new RefreshItemCreationListThread()).run();
	}

	/**
	 * In order to keep the user interface from freezing (or at
	 * least appearing to freeze), this internal class is used
	 * to refresh the items that can be created.
	 */

	private class RefreshItemCreationListThread extends Thread
	{
		public void run()
		{
			concoctions.clear();
			concoctions.addAll( ConcoctionsDatabase.getConcoctions( client, client.getInventory() ) );
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
