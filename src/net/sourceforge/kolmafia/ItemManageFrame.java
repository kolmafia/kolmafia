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
		tabs.addTab( "Sell", new SellItemPanel() );
		tabs.addTab( "Create", new CreationPanel() );
		tabs.addTab( "Put Away", new StoragePanel() );

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
	}

	/**
	 * Internal class used to handle everything related to
	 * selling items; this allows autoselling of items as
	 * well as placing item inside of a store.
	 */

	private class SellItemPanel extends NonContentPanel
	{
		private LockableListModel available;
		private JList availableList;

		public SellItemPanel()
		{
			super( "autosell", "automall" );
			setContent( null, null, null, null, true, true );

			available = client == null ? new LockableListModel() : client.getInventory().getMirrorImage();
			availableList = new JList( available );
			availableList.setSelectionMode( ListSelectionModel.MULTIPLE_INTERVAL_SELECTION );
			availableList.setPrototypeCellValue( "ABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890@#$%^&*" );

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
				try
				{
					SellItemPanel.this.setEnabled( false );
					Object [] items = availableList.getSelectedValues();
					AdventureResult currentItem;

					for ( int i = 0; i < items.length; ++i )
					{
						currentItem = (AdventureResult) items[i];

						switch ( sellType )
						{
							case AutoSellRequest.AUTOSELL:
								updateDisplay( DISABLED_STATE, "Autoselling " + currentItem.getResultName() + "..." );
								break;

							case AutoSellRequest.AUTOMALL:
								updateDisplay( DISABLED_STATE, "Placing " + currentItem.getResultName() + " in the mall..." );
								break;
						}

						(new AutoSellRequest( client, sellType, currentItem )).run();
					}

					updateDisplay( ENABLED_STATE, "Requests complete." );
					SellItemPanel.this.setEnabled( true );
				}
				catch ( NumberFormatException e )
				{
					// If the number placed inside of the count list was not
					// an actual integer value, pretend nothing happened.
					// Using exceptions for flow control is bad style, but
					// this will be fixed once we add functionality.
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
		private JList availableList;
		private JList closetList;

		public StoragePanel()
		{
			JPanel panel = new JPanel();
			panel.setLayout( new BorderLayout( 10, 10 ) );
			panel.add( new OutsideClosetPanel(), BorderLayout.NORTH );
			panel.add( new InsideClosetPanel(), BorderLayout.SOUTH );

			setLayout( new CardLayout( 10, 10 ) );
			add( panel, "" );
		}

		private class OutsideClosetPanel extends NonContentPanel
		{
			private LockableListModel inventory;

			public OutsideClosetPanel()
			{
				super( "closet", "stash" );
				setContent( null );

				inventory = client == null ? new LockableListModel() : client.getInventory().getMirrorImage();
				availableList = new JList( inventory );
				availableList.setSelectionMode( ListSelectionModel.MULTIPLE_INTERVAL_SELECTION );
				availableList.setPrototypeCellValue( "ABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890@#$%^&*" );
				availableList.setVisibleRowCount( 7 );

				add( JComponentUtilities.createLabel( "Inside Inventory", JLabel.CENTER,
					Color.black, Color.white ), BorderLayout.NORTH );
				add( new JScrollPane( availableList, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
					JScrollPane.HORIZONTAL_SCROLLBAR_NEVER ), BorderLayout.WEST );
			}

			public void clear()
			{
			}

			protected void actionConfirmed()
			{	(new ItemStorageRequestThread( ItemStorageRequest.MOVE_TO_CLOSET )).run();
			}

			protected void actionCancelled()
			{
			}
		}

		private class InsideClosetPanel extends NonContentPanel
		{
			private LockableListModel closet;

			public InsideClosetPanel()
			{
				super( "option 1", "option 2" );
				setContent( null );

				closet = client == null ? new LockableListModel() : client.getCloset().getMirrorImage();
				closetList = new JList( closet );
				closetList.setSelectionMode( ListSelectionModel.MULTIPLE_INTERVAL_SELECTION );
				closetList.setPrototypeCellValue( "ABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890@#$%^&*" );
				closetList.setVisibleRowCount( 7 );

				add( JComponentUtilities.createLabel( "Inside Closet", JLabel.CENTER,
					Color.black, Color.white ), BorderLayout.NORTH );
				add( new JScrollPane( closetList, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
					JScrollPane.HORIZONTAL_SCROLLBAR_NEVER ), BorderLayout.WEST );
			}

			public void clear()
			{
			}

			protected void actionConfirmed()
			{
			}

			protected void actionCancelled()
			{
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

			public ItemStorageRequestThread( int moveType )
			{
				super( "Closet-Request-Thread" );
				setDaemon( true );
				this.moveType = moveType;
			}

			public void run()
			{
				try
				{
					StoragePanel.this.setEnabled( false );
					updateDisplay( DISABLED_STATE, "Moving items..." );
					Object [] items =
						moveType == ItemStorageRequest.MOVE_TO_CLOSET ? availableList.getSelectedValues() :
						moveType == ItemStorageRequest.MOVE_TO_INVENTORY ? closetList.getSelectedValues() :
						moveType == ItemStorageRequest.MOVE_TO_STASH ? availableList.getSelectedValues() : null;

					updateDisplay( DISABLED_STATE, "Items successfully moved." );
					(new ItemStorageRequest( client, moveType, items )).run();
					StoragePanel.this.setEnabled( true );
				}
				catch ( NumberFormatException e )
				{
					// If the number placed inside of the count list was not
					// an actual integer value, pretend nothing happened.
					// Using exceptions for flow control is bad style, but
					// this will be fixed once we add functionality.
				}
			}
		}
	}


	/**
	 * Internal class used to handle everything related to
	 * item creation.
	 */

	private class CreationPanel extends JPanel
	{
		public CreationPanel()
		{
			setLayout( new BorderLayout( 10, 10 ) );
			add( new CombineItemPanel(), BorderLayout.NORTH );
			add( new MeatManagementPanel(), BorderLayout.CENTER );
		}

		/**
		 * An internal class used to handle item creation for
		 * the inventory screen.
		 */

		private class CombineItemPanel extends NonContentPanel
		{
			private JComboBox createField;
			private JTextField countField;

			public CombineItemPanel()
			{
				super( "make", "refresh", new Dimension( 100, 20 ), new Dimension( 250, 20 ) );

				createField = new JComboBox();
				countField = new JTextField();

				VerifiableElement [] elements = new VerifiableElement[2];
				elements[0] = new VerifiableElement( "Item to Make: ", createField );
				elements[1] = new VerifiableElement( "Quantity: ", countField );

				setContent( elements );
			}

			public void clear()
			{
				createField.setSelectedIndex( 0 );
				countField.setText( "" );
			}

			protected void actionConfirmed()
			{
			}

			protected void actionCancelled()
			{
			}
		}

		/**
		 * An internal class used to handle meat management for
		 * the inventory screen.
		 */

		private class MeatManagementPanel extends NonContentPanel
		{
			private JLabel availableLabel;
			private JLabel insideClosetLabel;
			private JComboBox actionField;
			private JTextField countField;
			private LockableListModel actions;

			public MeatManagementPanel()
			{
				super( "execute", "refresh", new Dimension( 100, 20 ), new Dimension( 250, 20 ) );

				LockableListModel actions = new LockableListModel();
				actions.add( "Make Meat Paste" );
				actions.add( "Make Meat Stack" );
				actions.add( "Make Dense Stack" );

				actionField = new JComboBox( actions );
				countField = new JTextField();

				VerifiableElement [] elements = new VerifiableElement[2];
				elements[0] = new VerifiableElement( "Action: ", actionField );
				elements[1] = new VerifiableElement( "Amount: ", countField );

				setContent( elements );
			}

			protected void setContent( VerifiableElement [] elements )
			{
				super.setContent( elements );

				JPanel labelPanel = new JPanel();
				labelPanel.setLayout( new GridLayout( 2, 1 ) );
				availableLabel = new JLabel( "You have [x] meat available.", JLabel.CENTER );
				insideClosetLabel = new JLabel( "You have [x] meat inside your closet.", JLabel.CENTER );
				labelPanel.add( availableLabel );
				labelPanel.add( insideClosetLabel );

				this.add( labelPanel, BorderLayout.NORTH );
			}

			public void clear()
			{
				actionField.setSelectedIndex( 0 );
				countField.setText( "" );
				requestFocus();
			}

			protected void actionConfirmed()
			{
				try
				{
					int quantity = df.parse( countField.getText() ).intValue();

					switch ( actionField.getSelectedIndex() )
					{
						case 0:
							break;
						case 1:
							break;
						case 2:
							break;
						case 3:
							break;
						case 4:
							break;
					}
				}
				catch ( ParseException e )
				{
					// If the number placed inside of the count list was not
					// an actual integer value, pretend nothing happened.
					// Using exceptions for flow control is bad style, but
					// this will be fixed once we add functionality.
				}
			}

			protected void actionCancelled()
			{
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
