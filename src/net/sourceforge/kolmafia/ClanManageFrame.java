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
import javax.swing.BoxLayout;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.CardLayout;
import java.awt.GridLayout;
import java.awt.BorderLayout;

// event listeners
import java.awt.event.MouseListener;
import java.awt.event.MouseEvent;

// containers
import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JTextField;
import javax.swing.JCheckBox;
import javax.swing.ListSelectionModel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JOptionPane;

import javax.swing.JTable;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import com.sun.java.forums.TableSorter;

// other imports
import java.util.Vector;
import java.util.ArrayList;

import net.java.dev.spellcast.utilities.LockableListModel;
import net.java.dev.spellcast.utilities.JComponentUtilities;

/**
 * An extension of <code>KoLFrame</code> which handles all the clan
 * management functionality of Kingdom of Loathing.
 */

public class ClanManageFrame extends KoLFrame
{
	private JTable members;
	private ClanBuffPanel clanBuff;
	private StoragePanel storing;
	private WithdrawPanel withdrawal;
	private DonationPanel donation;
	private AttackPanel attacks;
	private WarfarePanel warfare;
	private SnapshotPanel snapshot;
	private AscensionPanel ascension;
	private MemberSearchPanel search;

	public ClanManageFrame()
	{
		super( "Clan Management" );

		this.storing = new StoragePanel();
		this.clanBuff = new ClanBuffPanel();
		this.donation = new DonationPanel();
		this.withdrawal = new WithdrawPanel();
		this.attacks = new AttackPanel();
		this.warfare = new WarfarePanel();
		this.snapshot = new SnapshotPanel();
		this.ascension = new AscensionPanel();
		this.search = new MemberSearchPanel();
		this.tabs = new JTabbedPane();

		JPanel stashPanel = new JPanel( new GridLayout( 2, 1, 10, 10 ) );
		stashPanel.add( storing );
		stashPanel.add( withdrawal );

		tabs.addTab( "Stash Manager", stashPanel );

		JPanel snapPanel = new JPanel();
		snapPanel.setLayout( new BoxLayout( snapPanel, BoxLayout.Y_AXIS ) );
		snapPanel.add( snapshot );
		snapPanel.add( ascension );

		tabs.addTab( "Clan Snapshot", snapPanel );

		JPanel warfarePanel = new JPanel();
		warfarePanel.setLayout( new BoxLayout( warfarePanel, BoxLayout.Y_AXIS ) );
		warfarePanel.add( attacks );
		warfarePanel.add( warfare );

		tabs.addTab( "Clan Warfare", warfarePanel );

		JPanel purchasePanel = new JPanel();
		purchasePanel.setLayout( new BoxLayout( purchasePanel, BoxLayout.Y_AXIS ) );
		purchasePanel.add( donation );
		purchasePanel.add( clanBuff );

		tabs.addTab( "Clan Buffs", purchasePanel );

		members = new JTable( new MemberTableModel() );
		members.setModel( new TableSorter( members.getModel(), members.getTableHeader() ) );

		members.setRowSelectionAllowed( false );
		members.setAutoResizeMode( JTable.AUTO_RESIZE_NEXT_COLUMN );
		members.addMouseListener( new ButtonEventListener( members ) );

		members.setShowGrid( false );
		members.setRowHeight( 30 );

		members.getColumnModel().getColumn(0).setMinWidth( 30 );
		members.getColumnModel().getColumn(0).setMaxWidth( 30 );
		members.setDefaultRenderer( JButton.class, new ProfileButtonRenderer() );

		members.getColumnModel().getColumn(1).setMinWidth( 120 );
		members.getColumnModel().getColumn(1).setMaxWidth( 120 );

		members.getColumnModel().getColumn(3).setMinWidth( 120 );
		members.getColumnModel().getColumn(3).setMaxWidth( 120 );

		members.getColumnModel().getColumn(4).setMinWidth( 45 );
		members.getColumnModel().getColumn(4).setMaxWidth( 45 );

		JScrollPane results = new JScrollPane( members,
			JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER );

		JPanel searchPanel = new JPanel( new BorderLayout() );
		searchPanel.add( search, BorderLayout.NORTH );

		JPanel resultsPanel = new JPanel( new BorderLayout() );
		resultsPanel.add( members.getTableHeader(), BorderLayout.NORTH );
		resultsPanel.add( results, BorderLayout.CENTER );
		searchPanel.add( resultsPanel, BorderLayout.CENTER );

		tabs.addTab( "Member Search", searchPanel );

		framePanel.setLayout( new CardLayout( 10, 10 ) );
		framePanel.add( tabs, "" );

		if ( StaticEntity.getClient().shouldMakeConflictingRequest() )
			(new RequestThread( new ClanStashRequest( StaticEntity.getClient() ) )).start();
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
			super( "Buy Clan Buffs", "purchase", "take break", new Dimension( 80, 20 ), new Dimension( 240, 20 ) );
			this.isBuffing = false;

			buffField = new JComboBox( ClanBuffRequest.getRequestList( StaticEntity.getClient() ) );
			countField = new JTextField();

			VerifiableElement [] elements = new VerifiableElement[2];
			elements[0] = new VerifiableElement( "Clan Buff: ", buffField );
			elements[1] = new VerifiableElement( "# of times: ", countField );

			setContent( elements );
		}

		protected void actionConfirmed()
		{	(new RequestThread( (Runnable) buffField.getSelectedItem(), getValue( countField ) )).start();
		}

		protected void actionCancelled()
		{
			if ( isBuffing )
				KoLmafia.updateDisplay( ERROR_STATE, "Purchase attempts cancelled." );
		}
	}

	/**
	 * An internal class which represents the panel used for clan
	 * buffs in the <code>ClanManageFrame</code>.
	 */

	private class AttackPanel extends LabeledKoLPanel
	{
		private JComboBox enemyList;

		public AttackPanel()
		{
			super( "Loot Another Clan", "attack", "refresh", new Dimension( 80, 20 ), new Dimension( 240, 20 ) );
			enemyList = new JComboBox( ClanListRequest.getEnemyClans() );

			VerifiableElement [] elements = new VerifiableElement[1];
			elements[0] = new VerifiableElement( "Victim: ", enemyList );
			setContent( elements );
		}

		protected void actionConfirmed()
		{	(new RequestThread( (Runnable) enemyList.getSelectedItem() )).start();
		}

		protected void actionCancelled()
		{	(new RequestThread( new ClanListRequest( StaticEntity.getClient() ) )).start();
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
				super( StaticEntity.getClient(), "clan_war.php" );
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
				KoLmafia.updateDisplay( "Purchasing clan materials..." );

				super.run();

				// Theoretically, there should be a test for error state,
				// but because I'm lazy, that's not happening.

				KoLmafia.updateDisplay( "Purchase request processed." );
			}
		}
	}

	/**
	 * An internal class which represents the panel used for donations to
	 * the clan coffer.
	 */

	private class DonationPanel extends LabeledKoLPanel
	{
		private JTextField amountField;

		public DonationPanel()
		{
			super( "Fund Your Clan", "donate meat", "loot clan", new Dimension( 80, 20 ), new Dimension( 240, 20 ) );

			amountField = new JTextField();
			VerifiableElement [] elements = new VerifiableElement[1];
			elements[0] = new VerifiableElement( "Amount: ", amountField );
			setContent( elements );
		}

		protected void actionConfirmed()
		{	(new RequestThread( new ClanStashRequest( StaticEntity.getClient(), getValue( amountField ) ) )).start();
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
		{
			super( "Inside Inventory", "put in stash", "put in closet", KoLCharacter.getInventory() );
			elementList.setCellRenderer( AdventureResult.getAutoSellCellRenderer() );
		}

		protected void actionConfirmed()
		{
			Object [] items = getDesiredItems( elementList, "Donate", TAKE_MULTIPLE );
			if ( items == null || items.length == 0 )
				return;
			(new RequestThread( new ClanStashRequest( StaticEntity.getClient(),
				items, ClanStashRequest.ITEMS_TO_STASH ) )).start();
		}

		protected void actionCancelled()
		{
			Object [] items = getDesiredItems( elementList, "Closet", TAKE_MULTIPLE );
			if ( items == null || items.length == 0 )
				return;
			(new RequestThread( new ItemStorageRequest( StaticEntity.getClient(), ItemStorageRequest.INVENTORY_TO_CLOSET,
				items ) )).start();
		}
	}

	/**
	 * Internal class used to handle everything related to
	 * placing items into the stash.
	 */

	private class WithdrawPanel extends ItemManagePanel
	{
		public WithdrawPanel()
		{
			super( "Inside Clan Stash", "put in bag", "refresh", ClanManager.getStash() );
			elementList.setSelectionMode( ListSelectionModel.SINGLE_SELECTION );
		}

		protected void actionConfirmed()
		{
			Object [] items = getDesiredItems( elementList, "Withdraw", TAKE_MULTIPLE );
			if ( items == null || items.length == 0 )
				return;
			(new RequestThread( new ClanStashRequest( StaticEntity.getClient(),
				items, ClanStashRequest.STASH_TO_ITEMS ) )).start();
		}

		protected void actionCancelled()
		{	(new RequestThread( new ClanStashRequest( StaticEntity.getClient() ) )).start();
		}
	}

	private class MemberSearchPanel extends KoLPanel
	{
		private JComboBox parameterSelect;
		private JComboBox matchSelect;
		private JTextField valueField;

		public MemberSearchPanel()
		{
			super( "search clan", "apply changes", new Dimension( 80, 20 ), new Dimension( 240, 20 ) );

			parameterSelect = new JComboBox();
			for ( int i = 0; i < ClanSnapshotTable.FILTER_NAMES.length; ++i )
				parameterSelect.addItem( ClanSnapshotTable.FILTER_NAMES[i] );

			matchSelect = new JComboBox();
			matchSelect.addItem( "Less than..." );
			matchSelect.addItem( "Equal to..." );
			matchSelect.addItem( "Greater than..." );

			valueField = new JTextField();

			VerifiableElement [] elements = new VerifiableElement[3];
			elements[0] = new VerifiableElement( "Parameter: ", parameterSelect );
			elements[1] = new VerifiableElement( "Constraint: ", matchSelect );
			elements[2] = new VerifiableElement( "Value:", valueField );

			setContent( elements, null, null, true, true );
		}

		protected void actionConfirmed()
		{	(new RequestThread( new MemberSearcher() )).start();
		}

		private class MemberSearcher implements Runnable
		{
			public void run()
			{
				ClanManager.applyFilter( matchSelect.getSelectedIndex() - 1, parameterSelect.getSelectedIndex(), valueField.getText() );
				KoLmafia.updateDisplay( "Search results retrieved." );
			}
		}

		protected void actionCancelled()
		{	(new RequestThread( new MemberChanger() )).start();
		}

		private class MemberChanger implements Runnable
		{
			public void run()
			{
				KoLmafia.updateDisplay( "Determining changes..." );
				if ( members.isEditing() )
				{
					int row = members.getEditingRow();
					int col = members.getEditingColumn();
					members.getCellEditor( row, col ).stopCellEditing();

					System.out.println( "Editor: " + members.getValueAt( row, col ) );
				}

				ArrayList titleChange = new ArrayList();
				ArrayList newTitles = new ArrayList();
				ArrayList boots = new ArrayList();

				for ( int i = 0; i < members.getRowCount(); ++i )
				{
					if ( ((Boolean)members.getValueAt( i, 4 )).booleanValue() )
						boots.add( members.getValueAt( i, 1 ) );

					titleChange.add( members.getValueAt( i, 1 ) );
					newTitles.add( members.getValueAt( i, 2 ) );
				}

				KoLmafia.updateDisplay( "Applying changes..." );
				(new ClanMembersRequest( StaticEntity.getClient(), titleChange.toArray(), newTitles.toArray(), boots.toArray() )).run();
				KoLmafia.updateDisplay( "Changes have been applied." );
			}
		}
	}

	private class MemberTableModel extends DefaultTableModel implements ListDataListener
	{
		public MemberTableModel()
		{
			super( 0, 5 );
			ClanSnapshotTable.getFilteredList().addListDataListener( this );
		}

		public String getColumnName( int index )
		{
			switch ( index )
			{
				case 0:  return " ";
				case 1:  return "Name";
				case 2:  return "Clan Title";
				case 3:  return "Total Karma";
				case 4:  return "Boot";
				default:  return "";
			}
		}

		public Class getColumnClass( int column )
		{
			switch ( column )
			{
				case 0:  return JButton.class;
				case 1:  return String.class;
				case 2:  return String.class;
				case 3:  return Integer.class;
				case 4:  return Boolean.class;
				default:  return Object.class;
			}
		}

		private Vector constructVector( ProfileRequest p )
		{
			Vector value = new Vector();

			JButton profileButton = new ShowProfileButton( p );
			JComponentUtilities.setComponentSize( profileButton, 20, 20 );

			value.add( profileButton );
			value.add( p.getPlayerName() );
			value.add( p.getTitle() );
			value.add( p.getKarma() );
			value.add( new Boolean( false ) );

			return value;
		}

		public boolean isCellEditable( int row, int column )
		{
			switch ( column )
			{
				case 0:  return false;
				case 1:  return false;
				case 2:  return true;
				case 3:  return false;
				case 4:  return true;
				default:  return false;
			}
		}

		/**
		 * Called whenever contents have been added to the original list; a
		 * function required by every <code>ListDataListener</code>.
		 *
		 * @param	e	the <code>ListDataEvent</code> that triggered this function call
		 */

		public void intervalAdded( ListDataEvent e )
		{
			LockableListModel source = (LockableListModel) e.getSource();
			int index0 = e.getIndex0();  int index1 = e.getIndex1();

			if ( index1 >= source.size() || source.size() == getRowCount() )
				return;

			for ( int i = index0; i <= index1; ++i )
				insertRow( i, constructVector( (ProfileRequest) source.get(i) ) );
		}

		/**
		 * Called whenever contents have been removed from the original list;
		 * a function required by every <code>ListDataListener</code>.
		 *
		 * @param	e	the <code>ListDataEvent</code> that triggered this function call
		 */

		public void intervalRemoved( ListDataEvent e )
		{
			LockableListModel source = (LockableListModel) e.getSource();
			int index0 = e.getIndex0();  int index1 = e.getIndex1();

			if ( index1 >= getRowCount() || source.size() == getRowCount() )
				return;

			for ( int i = index1; i >= index0; --i )
				removeRow(i);
		}

		/**
		 * Called whenever contents in the original list have changed; a
		 * function required by every <code>ListDataListener</code>.
		 *
		 * @param	e	the <code>ListDataEvent</code> that triggered this function call
		 */

		public void contentsChanged( ListDataEvent e )
		{
			LockableListModel source = (LockableListModel) e.getSource();
			int index0 = e.getIndex0();  int index1 = e.getIndex1();

			if ( index1 >= getRowCount() )
				return;

			for ( int i = index1; i >= index0; --i )
			{
				removeRow(i);
				insertRow( i, constructVector( (ProfileRequest) source.get(i) ) );
			}
		}
	}

	private class ProfileButtonRenderer implements TableCellRenderer
	{
		public Component getTableCellRendererComponent( JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column )
		{	return (JButton) value;
		}
	}

	private class ShowProfileButton extends JButton implements MouseListener
	{
		private ProfileRequest profile;

		public ShowProfileButton( ProfileRequest profile )
		{
			super( JComponentUtilities.getImage( "icon_warning_sml.gif" ) );
			this.profile = profile;
		}

		public void mouseReleased( MouseEvent e )
		{
			Object [] parameters = new Object[2];
			parameters[0] = StaticEntity.getClient();
			parameters[1] = profile;

			(new RequestThread( new CreateFrameRunnable( ProfileFrame.class, parameters ) )).start();
		}

		public void mouseClicked( MouseEvent e )
		{
		}

		public void mouseEntered( MouseEvent e )
		{
		}

		public void mouseExited( MouseEvent e )
		{
		}

		public void mousePressed( MouseEvent e )
		{
		}
	}

	private class SnapshotPanel extends LabeledKoLPanel
	{
		private JCheckBox [] optionBoxes;
		private final String [][] options =
		{
			{ "<td>Lv</td><td>Mus</td><td>Mys</td><td>Mox</td><td>Total</td>", "Progression statistics (level, power, class)" },
			{ "<td>Title</td><td>Rank</td><td>Karma</td>", "Internal clan statistics (title, karma)" },
			{ "<td>Class</td><td>Path</td><td>Turns</td><td>Meat</td>", "Leaderboard (class, path, turns, wealth)" },
			{ "<td>PVP</td><td>Food</td><td>Drink</td>", "Miscellaneous statistics (pvp, food, booze)" },
			{ "<td>Created</td><td>Last Login</td>", "Creation and last login dates" },
		};

		public SnapshotPanel()
		{
			super( "Clan Snapshot", "snapshot", "logshot", new Dimension( 300, 16 ), new Dimension( 20, 16 ) );

			VerifiableElement [] elements = new VerifiableElement[ options.length ];

			optionBoxes = new JCheckBox[ options.length ];
			for ( int i = 0; i < options.length; ++i )
				optionBoxes[i] = new JCheckBox();

			for ( int i = 0; i < options.length; ++i )
				elements[i] = new VerifiableElement( options[i][1], JLabel.LEFT, optionBoxes[i] );

			setContent( elements, false );
			String tableHeaderSetting = getProperty( "clanRosterHeader" );
			for ( int i = 0; i < options.length; ++i )
				optionBoxes[i].setSelected( tableHeaderSetting.indexOf( options[i][0] ) != -1 );
		}

		protected void actionConfirmed()
		{
			// Apply all the settings before generating the
			// needed clan ClanSnapshotTable.

			StringBuffer tableHeaderSetting = new StringBuffer();

			for ( int i = 0; i < options.length; ++i )
				if ( optionBoxes[i].isSelected() )
					tableHeaderSetting.append( options[i][0] );

			setProperty( "clanRosterHeader", tableHeaderSetting.toString() + "<td>Ascensions</td>" );

			// Now that you've got everything, go ahead and
			// generate the snapshot.

			ClanManager.takeSnapshot( 0, 0, 0, 0, false );
		}

		protected void actionCancelled()
		{
			ClanManager.saveStashLog();
		}
	}

	private class AscensionPanel extends LabeledKoLPanel
	{
		private JTextField mostAscensionsBoardSizeField;
		private JTextField mainBoardSizeField;
		private JTextField classBoardSizeField;
		private JTextField maxAgeField;
		private JCheckBox playerMoreThanOnceOption;

		public AscensionPanel()
		{
			super( "Clan Leaderboards", "snapshot", new Dimension( 240, 20 ), new Dimension( 80, 20 ) );

			mostAscensionsBoardSizeField = new JTextField( "20" );
			mainBoardSizeField = new JTextField( "10" );
			classBoardSizeField = new JTextField( "5" );
			maxAgeField = new JTextField( "0" );
			playerMoreThanOnceOption = new JCheckBox();

			VerifiableElement [] elements = new VerifiableElement[5];
			elements[0] = new VerifiableElement( "Most Ascensions Board Size:  ", mostAscensionsBoardSizeField );
			elements[1] = new VerifiableElement( "Fastest Ascensions Board Size:  ", mainBoardSizeField );
			elements[2] = new VerifiableElement( "Class Breakdown Board Size:  ", classBoardSizeField );
			elements[3] = new VerifiableElement( "Maximum Ascension Age (in days):  ", maxAgeField );
			elements[4] = new VerifiableElement( "Allow Multiple Appearances:  ", playerMoreThanOnceOption );

			setContent( elements );
		}

		protected void actionConfirmed()
		{
			int mostAscensionsBoardSize = mostAscensionsBoardSizeField.getText().equals( "" ) ? Integer.MAX_VALUE : StaticEntity.parseInt( mostAscensionsBoardSizeField.getText() );
			int mainBoardSize = mainBoardSizeField.getText().equals( "" ) ? Integer.MAX_VALUE : StaticEntity.parseInt( mainBoardSizeField.getText() );
			int classBoardSize = classBoardSizeField.getText().equals( "" ) ? Integer.MAX_VALUE : StaticEntity.parseInt( classBoardSizeField.getText() );
			int maxAge = maxAgeField.getText().equals( "" ) ? Integer.MAX_VALUE : StaticEntity.parseInt( maxAgeField.getText() );
			boolean playerMoreThanOnce = playerMoreThanOnceOption.isSelected();

			String oldSetting = getProperty( "clanRosterHeader" );
			setProperty( "clanRosterHeader", "<td>Ascensions</td>" );

			// Now that you've got everything, go ahead and
			// generate the snapshot.

			ClanManager.takeSnapshot( mostAscensionsBoardSize, mainBoardSize, classBoardSize, maxAge, playerMoreThanOnce );
			setProperty( "clanRosterHeader", oldSetting );
		}

		protected void actionCancelled()
		{
			ClanManager.saveStashLog();
		}
	}
}
