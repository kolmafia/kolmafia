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
import javax.swing.JComponent;
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
import java.lang.reflect.Method;

import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Collections;
import java.text.DecimalFormat;
import java.text.ParseException;

import net.java.dev.spellcast.utilities.PanelList;
import net.java.dev.spellcast.utilities.PanelListCell;
import net.java.dev.spellcast.utilities.LockableListModel;
import net.java.dev.spellcast.utilities.JComponentUtilities;

/**
 * An extension of <code>KoLFrame</code> which handles all the clan
 * management functionality of Kingdom of Loathing.
 */

public class ClanManageFrame extends KoLFrame
{
	private JTabbedPane tabs;
	private ClanBuffPanel clanBuff;
	private StoragePanel storing;
	private DonationPanel donation;
	private WarfarePanel warfare;
	private MemberSearchPanel search;

	public ClanManageFrame( KoLmafia client )
	{
		super( "KoLmafia: Clan Management", client );

		this.storing = new StoragePanel();
		this.clanBuff = new ClanBuffPanel();
		this.donation = new DonationPanel();
		this.warfare = new WarfarePanel();
		this.search = new MemberSearchPanel();

		this.tabs = new JTabbedPane();

		JPanel karmaPanel = new JPanel();
		karmaPanel.setLayout( new BorderLayout() );
		karmaPanel.add( donation, BorderLayout.NORTH );
		karmaPanel.add( storing, BorderLayout.CENTER );

		tabs.addTab( "Boost Karma", karmaPanel );

		JPanel purchasePanel = new JPanel();
		purchasePanel.setLayout( new BoxLayout( purchasePanel, BoxLayout.Y_AXIS ) );
		purchasePanel.add( clanBuff );
		purchasePanel.add( warfare );

		tabs.addTab( "Purchases", purchasePanel );

		getContentPane().setLayout( new CardLayout( 10, 10 ) );
		getContentPane().add( tabs, "" );
		addWindowListener( new ReturnFocusAdapter() );
		setDefaultCloseOperation( HIDE_ON_CLOSE );

		tabs.addTab( "Member Search", search );
		addMenuBar();
	}

	private void addMenuBar()
	{
		JMenuBar menuBar = new JMenuBar();
		this.setJMenuBar( menuBar );

		JMenu optionsMenu = new JMenu( "Options" );
		optionsMenu.setMnemonic( KeyEvent.VK_O );
		menuBar.add( optionsMenu );

		JMenuItem attackItem = new JMenuItem( "Attack Enemies!", KeyEvent.VK_A );
		attackItem.addActionListener( new ManagerListener( "attackClan" ) );
		optionsMenu.add( attackItem );

		JMenuItem snapItem = new JMenuItem( "Clan Snapshot", KeyEvent.VK_C );
		snapItem.addActionListener( new ManagerListener( "takeSnapshot" ) );
		optionsMenu.add( snapItem );

		JMenuItem stashItem = new JMenuItem( "Save Stash Log", KeyEvent.VK_S );
		stashItem.addActionListener( new ManagerListener( "saveStashLog" ) );
		optionsMenu.add( stashItem );

		JMenu messageMenu = new JMenu( "Messages" );
		messageMenu.setMnemonic( KeyEvent.VK_M );
		menuBar.add( messageMenu );

		JMenuItem boardItem1 = new JMenuItem( "Post to Clan Board" );
		boardItem1.addActionListener( new ManagerListener( "postMessage" ) );
		messageMenu.add( boardItem1 );

		JMenuItem announceItem1 = new JMenuItem( "Post Announcement" );
		announceItem1.addActionListener( new ManagerListener( "postAnnouncement" ) );
		messageMenu.add( announceItem1 );

		JMenuItem boardItem2 = new JMenuItem( "Read Clan Messages" );
		boardItem2.addActionListener( new ManagerListener( "getMessageBoard" ) );
		messageMenu.add( boardItem2 );

		JMenuItem announceItem2 = new JMenuItem( "Read Announcements" );
		announceItem2.addActionListener( new ManagerListener( "getAnnouncements" ) );
		messageMenu.add( announceItem2 );

		addHelpMenu( menuBar );
	}

	public void setEnabled( boolean isEnabled )
	{
		if ( clanBuff != null )
			clanBuff.setEnabled( isEnabled );
		if ( storing != null )
			storing.setEnabled( isEnabled );
		if ( donation != null )
			donation.setEnabled( isEnabled );
		if ( warfare != null )
			warfare.setEnabled( isEnabled );
		if ( search != null )
			search.setEnabled( isEnabled );
	}

	/**
	 * An internal class which represents the panel used for clan
	 * buffs in the <code>ClanManageFrame</code>.
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
				int buffCount = getValue( countField );
				Runnable buff = (Runnable) buffField.getSelectedItem();

				client.makeRequest( buff, buffCount );
				isBuffing = false;
			}
		}
	}

	private class WarfarePanel extends LabeledKoLPanel
	{
		private JTextField goodies;
		private JTextField oatmeal, recliners;
		private JTextField ground, airborne, archers;

		public WarfarePanel()
		{
			super( "Prepare for WAR!!!", "purchase", "calculate", new Dimension( 120, 20 ), new Dimension( 200, 20 ) );

			goodies = new JTextField();
			oatmeal = new JTextField();
			recliners = new JTextField();
			ground = new JTextField();
			airborne = new JTextField();
			archers = new JTextField();

			VerifiableElement [] elements = new VerifiableElement[6];
			elements[0] = new VerifiableElement( "Goodies: ", goodies );
			elements[1] = new VerifiableElement( "Oatmeal: ", oatmeal );
			elements[2] = new VerifiableElement( "Recliners: ", recliners );
			elements[3] = new VerifiableElement( "Ground Troops: ", ground );
			elements[4] = new VerifiableElement( "Airborne Troops: ", airborne );
			elements[5] = new VerifiableElement( "La-Z-Archers: ", archers );

			setContent( elements );
		}

		public void setEnabled( boolean isEnabled )
		{
			super.setEnabled( isEnabled );

			goodies.setEnabled( isEnabled );
			oatmeal.setEnabled( isEnabled );
			recliners.setEnabled( isEnabled );
			ground.setEnabled( isEnabled );
			airborne.setEnabled( isEnabled );
			archers.setEnabled( isEnabled );
		}

		public void actionConfirmed()
		{	(new ClanMaterialsThread()).start();
		}

		public void actionCancelled()
		{
			int totalValue = getValue( goodies ) * 1000 + getValue( oatmeal ) * 3 + getValue( recliners ) * 1500 +
				getValue( ground ) * 300 + getValue( airborne ) * 500 + getValue( archers ) * 500;

			JOptionPane.showMessageDialog( null, "This purchase will cost " + totalValue + " meat" );
		}

		private class ClanMaterialsThread extends Thread
		{
			public ClanMaterialsThread()
			{
				super( "Clan-Materials-Thread" );
				setDaemon( true );
			}

			public void run()
			{	(new ClanMaterialsRequest()).run();
			}

			private class ClanMaterialsRequest extends KoLRequest
			{
				public ClanMaterialsRequest()
				{
					super( ClanManageFrame.this.client, "clan_war.php" );
					addFormField( "action", "Yep." );
					addFormField( "goodies", String.valueOf( getValue( goodies ) ) );
					addFormField( "oatmeal", String.valueOf( getValue( oatmeal ) ) );
					addFormField( "recliners", String.valueOf( getValue( recliners ) ) );
					addFormField( "grunts", String.valueOf( getValue( ground ) ) );
					addFormField( "flyers", String.valueOf( getValue( airborne ) ) );
					addFormField( "archers", String.valueOf( getValue( archers ) ) );
				}

				public void run()
				{
					client.updateDisplay( DISABLED_STATE, "Purchasing clan materials..." );

					super.run();

					// Theoretically, there should be a test for error state,
					// but because I'm lazy, that's not happening.

					client.updateDisplay( ENABLED_STATE, "Purchase request processed." );
				}
			}
		}
	}

	/**
	 * An internal class which represents the panel used for donations to
	 * the clan coffer.
	 */

	private class DonationPanel extends NonContentPanel
	{
		private JTextField amountField;

		public DonationPanel()
		{
			super( "donate meat", "loot clan", new Dimension( 80, 20 ), new Dimension( 240, 20 ) );

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
		{	(new DonationThread()).start();
		}

		protected void actionCancelled()
		{	JOptionPane.showMessageDialog( null, "The Hermit beat you to it.  ARGH!" );
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
			{	client.makeRequest( new ClanStashRequest( client, getValue( amountField ) ), 1 );
			}
		}
	}

	/**
	 * Internal class used to handle everything related to
	 * placing items into the stash.
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

	private class MemberSearchPanel extends NonContentPanel
	{
		private final int [] paramKeys = { ClanSnapshotTable.NAME_FILTER, ClanSnapshotTable.ID_FILTER,
			ClanSnapshotTable.LV_FILTER, ClanSnapshotTable.PVP_FILTER, ClanSnapshotTable.MUS_FILTER,
			ClanSnapshotTable.MYS_FILTER, ClanSnapshotTable.MOX_FILTER, ClanSnapshotTable.POWER_FILTER,
			ClanSnapshotTable.CLASS_FILTER, ClanSnapshotTable.RANK_FILTER, ClanSnapshotTable.KARMA_FILTER,
			ClanSnapshotTable.MEAT_FILTER, ClanSnapshotTable.TURN_FILTER, ClanSnapshotTable.LOGIN_FILTER };

		private final String [] paramNames = { "Player name", "KoL User ID", "Player level",
			"PVP Ranking", "Muscle points", "Mysticality points", "Moxie points",
			"Total power points", "Player class", "Rank within clan", "Accumulated karma",
			"Meat on hand", "Turns played", "Number of days idle" };

		private JComboBox parameterSelect;
		private JComboBox matchSelect;
		private JTextField valueField;
		private ClanMemberPanelList results;

		public MemberSearchPanel()
		{
			super( "search", "apply", new Dimension( 80, 20 ), new Dimension( 240, 20 ) );

			parameterSelect = new JComboBox();
			for ( int i = 0; i < paramNames.length; ++i )
				parameterSelect.addItem( paramNames[i] );

			matchSelect = new JComboBox();
			matchSelect.addItem( "Less than..." );
			matchSelect.addItem( "Equal to..." );
			matchSelect.addItem( "Greater than..." );

			valueField = new JTextField();

			VerifiableElement [] elements = new VerifiableElement[3];
			elements[0] = new VerifiableElement( "Parameter: ", parameterSelect );
			elements[1] = new VerifiableElement( "Constraint: ", matchSelect );
			elements[2] = new VerifiableElement( "", valueField );

			setContent( elements );

			results = new ClanMemberPanelList();

			JComponent [] header = new JComponent[4];
			header[0] = new JLabel( "Member Name", JLabel.CENTER );
			header[1] = new JLabel( "Clan Rank", JLabel.CENTER );
			header[2] = new JLabel( "Karma", JLabel.CENTER );
			header[3] = new JButton( JComponentUtilities.getSharedImage( "icon_error_sml.gif" ) );
			((JButton)header[3]).addActionListener( new SelectAllForBootListener() );


			JComponentUtilities.setComponentSize( header[0], 180, 20 );
			JComponentUtilities.setComponentSize( header[1], 210, 20 );
			JComponentUtilities.setComponentSize( header[2], 90, 20 );
			JComponentUtilities.setComponentSize( header[3], 20, 20 );

			JPanel headerPanel = new JPanel();
			headerPanel.setLayout( new BoxLayout( headerPanel, BoxLayout.X_AXIS ) );
			headerPanel.add( Box.createHorizontalStrut( 10 ) );

			for ( int i = 0; i < header.length; ++i )
			{
				headerPanel.add( header[i] );
				headerPanel.add( Box.createHorizontalStrut( 10 ) );
			}

			JPanel centerPanel = new JPanel();
			centerPanel.setLayout( new BorderLayout( 0, 0 ) );

			centerPanel.add( headerPanel, BorderLayout.NORTH );
			centerPanel.add( new JScrollPane( results, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
				JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS ), BorderLayout.CENTER );

			add( centerPanel, BorderLayout.CENTER );
		}

		public void setEnabled( boolean isEnabled )
		{
			super.setEnabled( isEnabled );
			parameterSelect.setEnabled( isEnabled );
			matchSelect.setEnabled( isEnabled );
			valueField.setEnabled( isEnabled );
		}

		protected void actionConfirmed()
		{	(new MemberSearchThread()).start();
		}

		protected void actionCancelled()
		{	(new MemberChangeThread()).start();
		}

		private class SelectAllForBootListener implements ActionListener
		{
			private boolean shouldSelect;

			public SelectAllForBootListener()
			{	shouldSelect = true;
			}

			public void actionPerformed( ActionEvent e )
			{
				Object currentComponent;
				ClanMemberPanel currentMember;

				for ( int i = 0; i < results.getComponentCount(); ++i )
				{
					currentComponent = results.getComponent(i);
					if ( currentComponent instanceof ClanMemberPanel )
					{
						currentMember = (ClanMemberPanel) currentComponent;
						currentMember.bootCheckBox.setSelected( shouldSelect );
					}
				}

				shouldSelect = !shouldSelect;
			}
		}

		/**
		 * In order to keep the user interface from freezing (or at
		 * least appearing to freeze), this internal class is used
		 * to actually donate to the statues.
		 */

		private class MemberSearchThread extends Thread
		{
			public MemberSearchThread()
			{
				super( "Member-Search-Thread" );
				setDaemon( true );
			}

			public void run()
			{
				client.getClanManager().applyFilter( matchSelect.getSelectedIndex() - 1, paramKeys[ parameterSelect.getSelectedIndex() ], valueField.getText() );
				client.updateDisplay( ENABLED_STATE, "Search results retrieved." );
			}
		}

		private class MemberChangeThread extends Thread
		{
			public MemberChangeThread()
			{
				super( "Member-Change-Thread" );
				setDaemon( true );
			}

			public void run()
			{
				client.updateDisplay( DISABLED_STATE, "Determining changes..." );

				List ranks = new ArrayList();
				List rankValues = new ArrayList();
				List boots = new ArrayList();

				Object currentComponent;
				ClanMemberPanel currentMember;
				Object desiredRank;

				for ( int i = 0; i < results.getComponentCount(); ++i )
				{
					currentComponent = results.getComponent(i);
					if ( currentComponent instanceof ClanMemberPanel )
					{
						currentMember = (ClanMemberPanel) currentComponent;
						if ( currentMember.bootCheckBox.isSelected() )
							boots.add( currentMember.memberName.getText() );

						desiredRank = currentMember.rankSelect.getSelectedItem();
						if ( desiredRank != null && !desiredRank.equals( currentMember.initialRank ) )
						{
							ranks.add( currentMember.memberName.getText() );
							rankValues.add( String.valueOf( currentMember.rankSelect.getSelectedIndex() ) );
							currentMember.profile.setRank( (String) desiredRank );
						}
					}
				}

				client.updateDisplay( DISABLED_STATE, "Applying changes..." );
				(new ClanMembersRequest( client, ranks.toArray(), rankValues.toArray(), boots.toArray() )).run();
				client.updateDisplay( ENABLED_STATE, "Changes have been applied." );
			}
		}
	}

	private class ManagerListener implements ActionListener
	{
		private Method method;

		public ManagerListener( String methodName )
		{
			try
			{	this.method = client.getClanManager().getClass().getDeclaredMethod( methodName, null );
			}
			catch ( Exception e )
			{
			}
		}

		public void actionPerformed( ActionEvent e )
		{	(new ManagerThread()).start();
		}

		private class ManagerThread extends Thread
		{
			public void run()
			{
				try
				{	method.invoke( client.getClanManager(), null );
				}
				catch ( Exception e )
				{
				}
			}
		}
	}

	public class ClanMemberPanelList extends PanelList
	{
		public ClanMemberPanelList()
		{	super( 12, 550, 25, client == null ? new LockableListModel() : client.getClanManager().getFilteredList() );
		}

		protected synchronized PanelListCell constructPanelListCell( Object value, int index )
		{
			ClanMemberPanel toConstruct = new ClanMemberPanel( (ProfileRequest) value );
			toConstruct.updateDisplay( this, value, index );
			return toConstruct;
		}
	}

	public class ClanMemberPanel extends PanelListCell
	{
		private JLabel memberName;
		private JComboBox rankSelect;
		private JLabel clanKarma;
		private JCheckBox bootCheckBox;

		private String initialRank;
		private ProfileRequest profile;

		public ClanMemberPanel( ProfileRequest value )
		{
			this.profile = value;
			memberName = new JLabel( value.getPlayerName(), JLabel.CENTER );
			LockableListModel rankList = client.getClanManager().getRankList();
			rankSelect = rankList.isEmpty() ? new JComboBox() : new JComboBox( rankList );

			// In the event that they were just searching for fun purposes,
			// there will be no ranks.  So it still looks like something,
			// add the rank manually.

			if ( rankList.isEmpty() )
				rankSelect.addItem( value.getRank() );

			initialRank = value.getRank();
			rankSelect.setSelectedItem( initialRank );
			bootCheckBox = new JCheckBox();

			clanKarma = new JLabel( df.format( Integer.parseInt( value.getKarma() ) ), JLabel.CENTER );

			JButton profileButton = new JButton( JComponentUtilities.getSharedImage( "icon_warning_sml.gif" ) );
			profileButton.addActionListener( new ShowProfileListener() );

			JComponentUtilities.setComponentSize( profileButton, 20, 20 );
			JComponentUtilities.setComponentSize( memberName, 150, 20 );
			JComponentUtilities.setComponentSize( rankSelect, 210, 20 );
			JComponentUtilities.setComponentSize( clanKarma, 90, 20 );
			JComponentUtilities.setComponentSize( bootCheckBox, 20, 20 );

			JPanel corePanel = new JPanel();
			corePanel.setLayout( new BoxLayout( corePanel, BoxLayout.X_AXIS ) );
			corePanel.add( Box.createHorizontalStrut( 10 ) );
			corePanel.add( profileButton ); corePanel.add( Box.createHorizontalStrut( 10 ) );
			corePanel.add( memberName ); corePanel.add( Box.createHorizontalStrut( 10 ) );
			corePanel.add( rankSelect ); corePanel.add( Box.createHorizontalStrut( 10 ) );
			corePanel.add( clanKarma ); corePanel.add( Box.createHorizontalStrut( 10 ) );
			corePanel.add( bootCheckBox ); corePanel.add( Box.createHorizontalStrut( 10 ) );

			setLayout( new BoxLayout( this, BoxLayout.Y_AXIS ) );
			add( Box.createVerticalStrut( 5 ) );
			add( corePanel );
		}

		public synchronized void updateDisplay( PanelList list, Object value, int index )
		{
			profile = (ProfileRequest) value;
			memberName.setText( profile.getPlayerName() );
			rankSelect.setSelectedItem( profile.getRank() );
			clanKarma.setText( df.format( Integer.parseInt( profile.getKarma() ) ) );
		}

		private class ShowProfileListener implements ActionListener
		{
			public void actionPerformed( ActionEvent e )
			{
				ProfileFrame frame = profile.getCleanHTML().equals( "" ) ? new ProfileFrame( client, memberName.getText() ) :
					new ProfileFrame( client, memberName.getText(), profile );

				frame.pack();  frame.setVisible( true );  frame.requestFocus();
				existingFrames.add( frame );
			}
		}
	}

	public static void main( String [] args )
	{
		System.setProperty( "SHARED_MODULE_DIRECTORY", "net/sourceforge/kolmafia/" );
		KoLFrame uitest = new ClanManageFrame( null );
		uitest.pack();  uitest.setVisible( true );  uitest.requestFocus();
	}
}