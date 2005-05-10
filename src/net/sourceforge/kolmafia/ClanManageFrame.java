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
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Collections;
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

		JPanel karmaPanel = new JPanel();
		karmaPanel.setLayout( new BorderLayout() );
		karmaPanel.add( donation, BorderLayout.NORTH );
		karmaPanel.add( storing, BorderLayout.CENTER );

		tabs.addTab( "Boost Karma", karmaPanel );

		JPanel purchasePanel = new JPanel();
		purchasePanel.setLayout( new BoxLayout( purchasePanel, BoxLayout.Y_AXIS ) );
		purchasePanel.add( clanBuff );

		tabs.addTab( "Purchases", purchasePanel );

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

		JMenu optionsMenu = new JMenu( "Options" );
		optionsMenu.setMnemonic( KeyEvent.VK_O );
		menuBar.add( optionsMenu );

		JMenuItem attackItem = new JMenuItem( "Attack Clan...", KeyEvent.VK_A );
		attackItem.addActionListener( new ClanListListener() );
		optionsMenu.add( attackItem );

		addHelpMenu( menuBar );
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
			super( "Hire Trainers", "purchase", "take break", new Dimension( 80, 20 ), new Dimension( 240, 20 ) );
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
			super( "", "donate meat", "loot clan", new Dimension( 80, 20 ), new Dimension( 240, 20 ) );

			amountField = new JTextField();
			VerifiableElement [] elements = new VerifiableElement[1];
			elements[0] = new VerifiableElement( "Amount: ", amountField );
			setContent( elements );
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

	private class StoragePanel extends ItemManagePanel
	{
		public StoragePanel()
		{	super( "", "put in stash", "put in closet", client == null ? new LockableListModel() : client.getInventory().getMirrorImage() );
		}

		protected void actionConfirmed()
		{	(new InventoryStorageThread( true )).start();
		}

		protected void actionCancelled()
		{	(new InventoryStorageThread( false )).start();
		}

		public void setEnabled( boolean isEnabled )
		{
			super.setEnabled( isEnabled );
			elementList.setEnabled( isEnabled );
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
				Object [] items = elementList.getSelectedValues();
				Runnable request = isStash ? (Runnable) new ClanStashRequest( client, items ) :
					(Runnable) new ItemStorageRequest( client, ItemStorageRequest.INVENTORY_TO_CLOSET, items );

				client.makeRequest( request, 1 );
				client.updateDisplay( ENABLED_STATE, "Items moved." );
			}
		}
	}

	private class ClanListListener implements ActionListener
	{
		public void actionPerformed( ActionEvent e )
		{	(new ClanListThread()).start();
		}

		private class ClanListThread extends Thread
		{
			public ClanListThread()
			{
				super( "Clan-List-Thread" );
				setDaemon( true );
			}

			public void run()
			{	(new ClanListRequest()).run();
			}
		}

		private class ClanListRequest extends KoLRequest
		{
			public ClanListRequest()
			{	super( ClanManageFrame.this.client, "clan_attack.php" );
			}

			public void run()
			{
				client.updateDisplay( DISABLED_STATE, "Retrieving list of attackable clans..." );

				super.run();

				if ( isErrorState )
					return;

				List enemyClans = new ArrayList();
				Matcher clanMatcher = Pattern.compile( "name=whichclan value=(\\d+)></td><td><b>(.*?)</td><td>(.*?)</td>" ).matcher( replyContent );
				int lastMatchIndex = 0;

				while ( clanMatcher.find( lastMatchIndex ) )
				{
					lastMatchIndex = clanMatcher.end();
					enemyClans.add( new ClanAttackRequest( clanMatcher.group(1), clanMatcher.group(2), Integer.parseInt( clanMatcher.group(3) ) ) );
				}

				Collections.sort( enemyClans );
				Object [] enemies = enemyClans.toArray();
				ClanAttackRequest enemy = (ClanAttackRequest) JOptionPane.showInputDialog( null,
					"Attack the following clan...", "Clans With Goodies", JOptionPane.INFORMATION_MESSAGE, null, enemies, enemies[0] );

				if ( enemy == null )
				{
					client.updateDisplay( ENABLED_STATE, "" );
					return;
				}

				enemy.run();
			}

			private class ClanAttackRequest extends KoLRequest implements Comparable
			{
				private String name;
				private int goodies;

				public ClanAttackRequest( String id, String name, int goodies )
				{
					super( ClanManageFrame.this.client, "clan_attack.php" );
					addFormField( "whichclan", id );

					this.name = name;
					this.goodies = goodies;
				}

				public void run()
				{
					client.updateDisplay( DISABLED_STATE, "Attacking " + name + "..." );

					super.run();

					// Theoretically, there should be a test for error state,
					// but because I'm lazy, that's not happening.

					client.updateDisplay( ENABLED_STATE, "Attack request processed." );
				}

				public String toString()
				{	return name + " (" + goodies + " " + (goodies == 1 ? "bag" : "bags") + ")";
				}

				public int compareTo( Object o )
				{	return o == null || !(o instanceof ClanAttackRequest) ? -1 : compareTo( (ClanAttackRequest) o );
				}

				public int compareTo( ClanAttackRequest car )
				{
					int goodiesDifference = car.goodies - goodies;
					return goodiesDifference != 0 ? goodiesDifference : name.compareToIgnoreCase( car.name );
				}
			}

		}
	}

	public static void main( String [] args )
	{
		KoLFrame uitest = new ClanManageFrame( null );
		uitest.pack();  uitest.setVisible( true );  uitest.requestFocus();
	}
}