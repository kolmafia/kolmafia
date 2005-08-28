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
import javax.swing.JSeparator;

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
	private LockableListModel rankList;

	private JTabbedPane tabs;
	private ClanBuffPanel clanBuff;
	private StoragePanel storing;
	private WithdrawPanel withdrawal;
	private DonationPanel donation;
	private WarfarePanel warfare;
	private MemberSearchPanel search;

	public ClanManageFrame( KoLmafia client )
	{
		super( client, "Clan Management" );

		this.rankList = new LockableListModel();

		if ( client != null )
			(new RequestThread( new ClanStashRequest( client ) )).start();

		this.storing = new StoragePanel();
		this.clanBuff = new ClanBuffPanel();
		this.donation = new DonationPanel();
		this.withdrawal = new WithdrawPanel();
		this.warfare = new WarfarePanel();
		this.search = new MemberSearchPanel();

		this.tabs = new JTabbedPane();

		JPanel karmaPanel = new JPanel();
		karmaPanel.setLayout( new BorderLayout() );
		karmaPanel.add( donation, BorderLayout.NORTH );

		JPanel stashPanel = new JPanel();
		stashPanel.setLayout( new GridLayout( 2, 1, 10, 10 ) );
		stashPanel.add( storing );
		stashPanel.add( withdrawal );
		karmaPanel.add( stashPanel, BorderLayout.CENTER );

		tabs.addTab( "Adjust Karma", karmaPanel );

		JPanel purchasePanel = new JPanel();
		purchasePanel.setLayout( new BoxLayout( purchasePanel, BoxLayout.Y_AXIS ) );
		purchasePanel.add( clanBuff );
		purchasePanel.add( warfare );

		tabs.addTab( "Purchases", purchasePanel );

		getContentPane().setLayout( new CardLayout( 10, 10 ) );
		getContentPane().add( tabs, "" );

		tabs.addTab( "Member Search", search );
		addMenuBar();
	}

	private void addMenuBar()
	{
		JMenuBar menuBar = new JMenuBar();
		this.setJMenuBar( menuBar );

		JMenu optionsMenu = addOptionsMenu( menuBar );

		optionsMenu.add( new JSeparator() );
		optionsMenu.add( new ManagerMenuItem( "Attack Enemies!", KeyEvent.VK_A, "attackClan" ) );
		optionsMenu.add( new ManagerMenuItem( "Clan Snapshot", KeyEvent.VK_C, "takeSnapshot" ) );
		optionsMenu.add( new ManagerMenuItem( "Save Stash Log", KeyEvent.VK_S, "saveStashLog" ) );

		JMenu messageMenu = new JMenu( "Messages" );
		messageMenu.setMnemonic( KeyEvent.VK_M );
		menuBar.add( messageMenu );

		messageMenu.add( new ManagerMenuItem( "Post to Clan Board", KeyEvent.KEY_LOCATION_UNKNOWN, "postMessage" ) );
		messageMenu.add( new ManagerMenuItem( "Post Announcement", KeyEvent.KEY_LOCATION_UNKNOWN, "postAnnouncement" ) );
		messageMenu.add( new ManagerMenuItem( "Read Clan Messages", KeyEvent.KEY_LOCATION_UNKNOWN, "getMessageBoard" ) );
		messageMenu.add( new ManagerMenuItem( "Read Announcements", KeyEvent.KEY_LOCATION_UNKNOWN, "getAnnouncements" ) );

		addHelpMenu( menuBar );
	}

	public void setEnabled( boolean isEnabled )
	{
		if ( clanBuff != null )
			clanBuff.setEnabled( isEnabled );
		if ( storing != null )
			storing.setEnabled( isEnabled );
		if ( withdrawal != null )
			withdrawal.setEnabled( isEnabled );
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
			contentPanel = clanBuff;
			(new RequestThread( (Runnable) buffField.getSelectedItem(), getValue( countField ) )).start();
		}

		protected void actionCancelled()
		{
			if ( isBuffing )
			{
				contentPanel = clanBuff;
				client.cancelRequest();
				updateDisplay( ENABLED_STATE, "Purchase attempts cancelled." );
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
		{	(new RequestThread( new ClanMaterialsRequest() )).start();
		}

		public void actionCancelled()
		{
			int totalValue = getValue( goodies ) * 1000 + getValue( oatmeal ) * 3 + getValue( recliners ) * 1500 +
				getValue( ground ) * 300 + getValue( airborne ) * 500 + getValue( archers ) * 500;

			JOptionPane.showMessageDialog( null, "This purchase will cost " + totalValue + " meat" );
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

	/**
	 * An internal class which represents the panel used for donations to
	 * the clan coffer.
	 */

	private class DonationPanel extends KoLPanel
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
		{	(new RequestThread( new ClanStashRequest( client, getValue( amountField ) ) )).start();
		}

		protected void actionCancelled()
		{	JOptionPane.showMessageDialog( null, "The Hermit beat you to it.  ARGH!" );
		}
	}

	/**
	 * Internal class used to handle everything related to
	 * placing items into the stash.
	 */

	private class StoragePanel extends ItemManagePanel
	{
		public StoragePanel()
		{	super( "Inside Inventory", "put in stash", "put in closet", client == null ? new LockableListModel() : client.getInventory() );
		}

		protected void actionConfirmed()
		{	(new RequestThread( new ClanStashRequest( client, elementList.getSelectedValues(), ClanStashRequest.ITEMS_TO_STASH ) )).start();
		}

		protected void actionCancelled()
		{	(new RequestThread( new ItemStorageRequest( client, ItemStorageRequest.INVENTORY_TO_CLOSET, elementList.getSelectedValues() ) )).start();
		}

		public void setEnabled( boolean isEnabled )
		{
			super.setEnabled( isEnabled );
			elementList.setEnabled( isEnabled );
		}
	}

	/**
	 * Internal class used to handle everything related to
	 * placing items into the stash.
	 */

	private class WithdrawPanel extends ItemManagePanel
	{
		public WithdrawPanel()
		{	super( "Inside Clan Stash", "put in bag", "refresh", client == null ? new LockableListModel() : client.getClanManager().getStash() );
		}

		protected void actionConfirmed()
		{
			// Check the rank list to see if you're one
			// of the clan administrators.

			if ( rankList.isEmpty() )
			{
				rankList = client.getClanManager().getRankList();

				// If it's been double-confirmed that you're
				// not a clan administrator, then tell them
				// they can't do anything with the stash.

				if ( rankList.isEmpty() )
				{
					JOptionPane.showMessageDialog( null, "Look, but don't touch." );
					return;
				}
			}

			Object [] items = elementList.getSelectedValues();
			AdventureResult selection;
			int itemID, quantity;

			try
			{
				for ( int i = 0; i < items.length; ++i )
				{
					selection = (AdventureResult) items[i];
					itemID = selection.getItemID();
					quantity = df.parse( JOptionPane.showInputDialog(
						"Retrieving " + selection.getName() + " from the stash...", String.valueOf( selection.getCount() ) ) ).intValue();

					items[i] = new AdventureResult( itemID, quantity );

				}
			}
			catch ( Exception e )
			{
				// If an exception occurs somewhere along the way
				// then return from the thread.

				return;
			}

			(new RequestThread( new ClanStashRequest( client, items, ClanStashRequest.STASH_TO_ITEMS ) )).start();
		}

		protected void actionCancelled()
		{	(new RequestThread( new ClanStashRequest( client ) )).start();
		}

		public void setEnabled( boolean isEnabled )
		{
			super.setEnabled( isEnabled );
			elementList.setEnabled( isEnabled );
		}
	}

	private class MemberSearchPanel extends KoLPanel
	{
		private final int [] paramKeys = { ClanSnapshotTable.NAME_FILTER, ClanSnapshotTable.ID_FILTER,
			ClanSnapshotTable.LV_FILTER, ClanSnapshotTable.PVP_FILTER, ClanSnapshotTable.MUS_FILTER,
			ClanSnapshotTable.MYS_FILTER, ClanSnapshotTable.MOX_FILTER, ClanSnapshotTable.POWER_FILTER,
			ClanSnapshotTable.CLASS_FILTER, ClanSnapshotTable.RANK_FILTER, ClanSnapshotTable.KARMA_FILTER,
			ClanSnapshotTable.MEAT_FILTER, ClanSnapshotTable.TURN_FILTER, ClanSnapshotTable.LOGIN_FILTER,
			ClanSnapshotTable.ASCENSION_FILTER };

		private final String [] paramNames = { "Player name", "KoL User ID", "Player level",
			"PVP Ranking", "Muscle points", "Mysticality points", "Moxie points",
			"Total power points", "Player class", "Rank within clan", "Accumulated karma",
			"Meat on hand", "Turns played", "Number of days idle", "Number of ascensions" };

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
			header[0] = new JLabel( "Member Name", JLabel.LEFT );
			header[1] = new JLabel( "Clan Rank", JLabel.CENTER );
			header[2] = new JLabel( "Karma", JLabel.LEFT );
			header[3] = new SelectAllForBootButton();

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
			setDefaultButton( confirmedButton );
		}

		public void setEnabled( boolean isEnabled )
		{
			super.setEnabled( isEnabled );
			parameterSelect.setEnabled( isEnabled );
			matchSelect.setEnabled( isEnabled );
			valueField.setEnabled( isEnabled );
		}

		protected void actionConfirmed()
		{
			client.getClanManager().applyFilter( matchSelect.getSelectedIndex() - 1, paramKeys[ parameterSelect.getSelectedIndex() ], valueField.getText() );
			client.updateDisplay( ENABLED_STATE, "Search results retrieved." );
		}

		protected void actionCancelled()
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
					}
				}
			}

			client.updateDisplay( DISABLED_STATE, "Applying changes..." );
			(new ClanMembersRequest( client, ranks.toArray(), rankValues.toArray(), boots.toArray() )).run();
			client.updateDisplay( ENABLED_STATE, "Changes have been applied." );
		}

		private class SelectAllForBootButton extends JButton implements ActionListener
		{
			private boolean shouldSelect;

			public SelectAllForBootButton()
			{
				super( JComponentUtilities.getSharedImage( "icon_error_sml.gif" ) );
				addActionListener( this );
				setToolTipText( "Boot" );
				shouldSelect = true;
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
	}

	private class ManagerMenuItem extends InvocationMenuItem
	{
		public ManagerMenuItem( String title, int mnemonic, String methodName )
		{	super( title, mnemonic, client == null ? new ClanManager( null ) : client.getClanManager(), methodName );
		}
	}

	public class ClanMemberPanelList extends PanelList
	{
		public ClanMemberPanelList()
		{	super( 12, 550, 30, client == null ? new LockableListModel() : client.getClanManager().getFilteredList() );
		}

		protected synchronized PanelListCell constructPanelListCell( Object value, int index )
		{
			ClanMemberPanel toConstruct = new ClanMemberPanel( (ProfileRequest) value );
			toConstruct.updateDisplay( this, value, index );
			return toConstruct;
		}
	}

	public class ClanMemberPanel extends JPanel implements PanelListCell
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
			memberName = new JLabel( value.getPlayerName(), JLabel.LEFT );

			if ( rankList.isEmpty() )
				rankList = client.getClanManager().getRankList();

			rankSelect = rankList.isEmpty() ? new JComboBox() : new JComboBox( (LockableListModel) rankList.clone() );

			// In the event that they were just searching for fun purposes,
			// there will be no ranks.  So it still looks like something,
			// add the rank manually.

			if ( rankList.isEmpty() )
				rankSelect.addItem( value.getRank() );

			initialRank = value.getRank();
			rankSelect.setSelectedItem( initialRank );
			bootCheckBox = new JCheckBox();

			clanKarma = new JLabel( df.format( value.getKarma() ), JLabel.LEFT );

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
			add( Box.createVerticalStrut( 3 ) );
			add( corePanel );
			add( Box.createVerticalStrut( 2 ) );
		}

		public synchronized void updateDisplay( PanelList list, Object value, int index )
		{
			profile = (ProfileRequest) value;
			memberName.setText( profile.getPlayerName() );
			rankSelect.setSelectedItem( profile.getRank() );
			clanKarma.setText( df.format( profile.getKarma() ) );
		}

		private class ShowProfileListener implements ActionListener
		{
			public void actionPerformed( ActionEvent e )
			{
				Object [] parameters = new Object[2];
				parameters[0] = client;
				parameters[1] = profile;

				(new CreateFrameRunnable( ProfileFrame.class, parameters )).run();
			}
		}
	}

	public static void main( String [] args )
	{
		Object [] parameters = new Object[1];
		parameters[0] = null;

		(new CreateFrameRunnable( ClanManageFrame.class, parameters )).run();
	}
}
