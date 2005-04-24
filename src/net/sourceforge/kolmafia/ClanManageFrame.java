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
import javax.swing.Box;
import javax.swing.BoxLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.CardLayout;
import java.awt.GridLayout;
import java.awt.BorderLayout;
import javax.swing.BorderFactory;

// event listeners
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import javax.swing.SwingUtilities;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ChangeEvent;

// containers
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JTextField;
import javax.swing.JCheckBox;
import javax.swing.ListSelectionModel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JMenu;
import javax.swing.JOptionPane;

// other imports
import java.util.Iterator;
import java.text.DecimalFormat;
import java.text.ParseException;
import net.java.dev.spellcast.utilities.LockableListModel;
import net.java.dev.spellcast.utilities.JComponentUtilities;

/**
 * An extension of <code>KoLFrame</code> which handles all the item
 * management functionality of Kingdom of Loathing.  This ranges from
 * basic transfer to and from the closet to item creation, cooking,
 * item use, and equipment.
 */

public class ClanManageFrame extends KoLFrame
{
	private JTabbedPane tabs;
	private ClanBuffPanel clanBuff;
	private StoragePanel storing;
	private DonationPanel donation;

	public ClanManageFrame( KoLmafia client )
	{
		super( "KoLmafia: Clan Management", client );

		this.storing = new StoragePanel();
		this.clanBuff = new ClanBuffPanel();
		this.donation = new DonationPanel();

		this.tabs = new JTabbedPane();
		tabs.addTab( "Deposit Items", storing );

		JPanel meatSinkPanel = new JPanel();
		meatSinkPanel.setLayout( new BoxLayout( meatSinkPanel, BoxLayout.Y_AXIS ) );
		meatSinkPanel.add( donation );
		meatSinkPanel.add( clanBuff );

		JScrollPane meatSinkScroller = new JScrollPane( meatSinkPanel, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
			JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED );

		JComponentUtilities.setComponentSize( meatSinkScroller, 500, 300 );
		tabs.addTab( "Meat Sinking", meatSinkScroller );

		getContentPane().setLayout( new CardLayout( 10, 10 ) );
		getContentPane().add( tabs, "" );
		addWindowListener( new ReturnFocusAdapter() );
		setDefaultCloseOperation( HIDE_ON_CLOSE );
	}

	public void setEnabled( boolean isEnabled )
	{
		if ( storing != null )
			storing.setEnabled( isEnabled );
		if ( clanBuff != null )
			clanBuff.setEnabled( isEnabled );
	}

	/**
	 * An internal class which represents the panel used for clan
	 * buffs in the <code>AdventureFrame</code>.
	 */

	private class ClanBuffPanel extends LabeledKoLPanel
	{
		private boolean isBuffing;
		private JComboBox buffField;
		private JTextField countField;

		public ClanBuffPanel()
		{
			super( "Hire Trainers for the Clan", "purchase", "take break", new Dimension( 80, 20 ), new Dimension( 240, 20 ) );
			this.isBuffing = false;

			buffField = new JComboBox( ClanBuffRequest.getRequestList( client ) );
			countField = new JTextField();

			VerifiableElement [] elements = new VerifiableElement[2];
			elements[0] = new VerifiableElement( "Clan Buff: ", buffField );
			elements[1] = new VerifiableElement( "# of times: ", countField );

			setContent( elements );
		}

		public void setEnabled( boolean isEnabled )
		{
			super.setEnabled( isEnabled );
			buffField.setEnabled( isEnabled );
			countField.setEnabled( isEnabled );
		}

		protected void actionConfirmed()
		{
			isBuffing = true;
			contentPanel = clanBuff;
			(new ClanBuffRequestThread()).start();
		}

		protected void actionCancelled()
		{
			if ( isBuffing )
			{
				isBuffing = false;
				contentPanel = clanBuff;
				client.cancelRequest();
				updateDisplay( ENABLED_STATE, "Purchase attempts cancelled." );
			}
		}

		/**
		 * In order to keep the user interface from freezing (or at
		 * least appearing to freeze), this internal class is used
		 * to actually purchase the clan buffs.
		 */

		private class ClanBuffRequestThread extends Thread
		{
			public ClanBuffRequestThread()
			{
				super( "Clan-Buff-Thread" );
				setDaemon( true );
			}

			public void run()
			{
				try
				{
					if ( countField.getText().trim().length() == 0 )
						return;

					int buffCount = df.parse( countField.getText() ).intValue();
					Runnable buff = (Runnable) buffField.getSelectedItem();

					client.makeRequest( buff, buffCount );
					isBuffing = false;
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

	/**
	 * An internal class which represents the panel used for donations to
	 * the statues in the shrine.
	 */

	private class DonationPanel extends LabeledKoLPanel
	{
		private JTextField amountField;

		public DonationPanel()
		{
			super( "Filling the Coffer", "donate meat", "loot clan", new Dimension( 80, 20 ), new Dimension( 240, 20 ) );

			amountField = new JTextField();
			VerifiableElement [] elements = new VerifiableElement[1];
			elements[0] = new VerifiableElement( "Amount: ", amountField );
			setContent( elements, true, true );
		}

		public void setEnabled( boolean isEnabled )
		{
			super.setEnabled( isEnabled );
			amountField.setEnabled( isEnabled );
		}

		protected void actionConfirmed()
		{
			contentPanel = donation;
			(new DonationThread()).start();
		}

		protected void actionCancelled()
		{
			contentPanel = donation;
			updateDisplay( ERROR_STATE, "The Hermit beat you to it.  ARGH!" );
		}

		/**
		 * In order to keep the user interface from freezing (or at
		 * least appearing to freeze), this internal class is used
		 * to actually donate to the statues.
		 */

		private class DonationThread extends Thread
		{
			public DonationThread()
			{
				super( "Donation-Thread" );
				setDaemon( true );
			}

			public void run()
			{
				try
				{
					if ( amountField.getText().trim().length() == 0 )
						return;

					client.makeRequest( new ClanStashRequest( client, df.parse( amountField.getText() ).intValue() ), 1 );
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
				super( "Inside Closet", "put in bag", "put in stash", client == null ? new LockableListModel() : client.getCloset().getMirrorImage() );
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

		private class InventoryStorageThread extends Thread
		{
			private boolean isStash;

			public InventoryStorageThread( boolean isStash )
			{
				super( "Inventory-Storage-Thread" );
				setDaemon( true );
				this.isStash = isStash;
			}

			public void run()
			{
				Object [] items = availableList.getSelectedValues();
				Runnable request = isStash ? (Runnable) new ClanStashRequest( client, items ) :
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

		private class ClosetStorageThread extends Thread
		{
			private boolean isStash;

			public ClosetStorageThread( boolean isStash )
			{
				super( "Closet-Storage-Thread" );
				setDaemon( true );
				this.isStash = isStash;
			}

			public void run()
			{
				Object [] items = closetList.getSelectedValues();
				client.makeRequest( new ItemStorageRequest( client, ItemStorageRequest.CLOSET_TO_INVENTORY, items ), 1 );

				if ( isStash )
					client.makeRequest( new ClanStashRequest( client, items ), 1 );

				client.updateDisplay( ENABLED_STATE, "Items moved." );
			}
		}
	}
}