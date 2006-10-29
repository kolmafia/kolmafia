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

import java.awt.Dimension;
import java.awt.CardLayout;
import java.awt.BorderLayout;

// event listeners
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;

// containers
import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JTextField;
import javax.swing.JCheckBox;
import javax.swing.JTabbedPane;
import javax.swing.JOptionPane;

import javax.swing.JTable;
import com.sun.java.forums.TableSorter;

// other imports
import java.util.Vector;
import java.util.ArrayList;

import javax.swing.SwingUtilities;
import net.java.dev.spellcast.utilities.JComponentUtilities;

/**
 * An extension of <code>KoLFrame</code> which handles all the clan
 * management functionality of Kingdom of Loathing.
 */

public class ClanManageFrame extends KoLFrame
{
	private static final int MOVE_ONE = 1;
	private static final int MOVE_ALL = 2;
	private static final int MOVE_ALL_BUT = 3;
	private static final int MOVE_MULTIPLE = 4;

	private JTable members;
	private SnapshotPanel snapshot;
	private ClanBuffPanel clanBuff;
	private StoragePanel storing;
	private WithdrawPanel withdrawal;
	private DonationPanel donation;
	private AttackPanel attacks;
	private WarfarePanel warfare;
	private MemberSearchPanel search;

	public ClanManageFrame()
	{
		super( "Clan Management" );

		this.snapshot = new SnapshotPanel();
		this.attacks = new AttackPanel();
		this.storing = new StoragePanel();
		this.clanBuff = new ClanBuffPanel();
		this.donation = new DonationPanel();
		this.withdrawal = new WithdrawPanel();
		this.search = new MemberSearchPanel();
		this.warfare = new WarfarePanel();

		this.tabs = new JTabbedPane();
		this.tabs.setTabLayoutPolicy( JTabbedPane.SCROLL_TAB_LAYOUT );

		JPanel adminPanel = new JPanel();
		adminPanel.setLayout( new BoxLayout( adminPanel, BoxLayout.Y_AXIS ) );
		adminPanel.add( attacks );
		adminPanel.add( snapshot );

		addTab( "Admin", adminPanel );

		JPanel spendPanel = new JPanel();
		spendPanel.setLayout( new BoxLayout( spendPanel, BoxLayout.Y_AXIS ) );
		spendPanel.add( donation );
		spendPanel.add( clanBuff );
		spendPanel.add( warfare );

		addTab( "Coffers", spendPanel );
		tabs.addTab( "Deposit", storing );
		tabs.addTab( "Withdraw", withdrawal );

		members = new TransparentTable( new MemberTableModel() );
		members.setModel( new TableSorter( members.getModel(), members.getTableHeader() ) );
		members.getTableHeader().setReorderingAllowed( false );

		members.setRowSelectionAllowed( false );
		members.setAutoResizeMode( JTable.AUTO_RESIZE_NEXT_COLUMN );

		members.addMouseListener( new ButtonEventListener( members ) );
		members.setDefaultRenderer( JButton.class, new ButtonRenderer() );

		members.setShowGrid( false );
		members.setIntercellSpacing( new Dimension( 5, 5 ) );
		members.setRowHeight( 25 );

		members.getColumnModel().getColumn(0).setMinWidth( 30 );
		members.getColumnModel().getColumn(0).setMaxWidth( 30 );

		members.getColumnModel().getColumn(1).setMinWidth( 120 );
		members.getColumnModel().getColumn(1).setMaxWidth( 120 );

		members.getColumnModel().getColumn(3).setMinWidth( 120 );
		members.getColumnModel().getColumn(3).setMaxWidth( 120 );

		members.getColumnModel().getColumn(4).setMinWidth( 45 );
		members.getColumnModel().getColumn(4).setMaxWidth( 45 );

		SimpleScrollPane results = new SimpleScrollPane( members );
		JComponentUtilities.setComponentSize( results, 400, 300 );

		results.setOpaque( false );
		JPanel searchPanel = new JPanel( new BorderLayout() );
		searchPanel.add( search, BorderLayout.NORTH );

		JPanel resultsPanel = new JPanel( new BorderLayout() );
		resultsPanel.add( members.getTableHeader(), BorderLayout.NORTH );
		resultsPanel.add( results, BorderLayout.CENTER );
		searchPanel.add( resultsPanel, BorderLayout.CENTER );

		tabs.addTab( "Member Search", searchPanel );

		framePanel.setLayout( new CardLayout( 10, 10 ) );
		framePanel.add( tabs, "" );
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

			buffField = new JComboBox( ClanBuffRequest.getRequestList() );
			countField = new JTextField();

			VerifiableElement [] elements = new VerifiableElement[2];
			elements[0] = new VerifiableElement( "Clan Buff: ", buffField );
			elements[1] = new VerifiableElement( "# of times: ", countField );

			setContent( elements );
		}

		public void actionConfirmed()
		{	(new RequestThread( (Runnable) buffField.getSelectedItem(), getValue( countField ) )).start();
		}

		public void actionCancelled()
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

		public void actionConfirmed()
		{	(new RequestThread( (Runnable) enemyList.getSelectedItem() )).start();
		}

		public void actionCancelled()
		{	(new RequestThread( new ClanListRequest() )).start();
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
				super( "clan_war.php" );
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

		public void actionConfirmed()
		{	(new RequestThread( new ClanStashRequest( getValue( amountField ) ) )).start();
		}

		public void actionCancelled()
		{	JOptionPane.showMessageDialog( null, "The Hermit beat you to it.  ARGH!" );
		}
	}

	private class StoragePanel extends ItemManagePanel
	{
		private JCheckBox [] filters;

		public StoragePanel()
		{
			super( "", inventory, false );

			setButtons( new String [] { "stash one", "stash all", "stash all but one", "refresh" },
				new ActionListener [] { new StorageListener( MOVE_ONE ), new StorageListener( MOVE_ALL ), new StorageListener( MOVE_ALL_BUT ),
				new RequestButton( "Refresh Items", new EquipmentRequest( EquipmentRequest.CLOSET ) ) } );

			filters = new JCheckBox[5];
			filters[0] = new FilterCheckBox( filters, elementList, "Show food", true );
			filters[1] = new FilterCheckBox( filters, elementList, "Show drink", true );
			filters[2] = new FilterCheckBox( filters, elementList, "Show others", true );
			filters[3] = new FilterCheckBox( filters, elementList, true, "Show no-sell", true );
			filters[4] = new FilterCheckBox( filters, elementList, true, "Show no-trade", true );

			for ( int i = 0; i < filters.length; ++i )
				optionPanel.add( filters[i] );

			elementList.setCellRenderer( AdventureResult.getAutoSellCellRenderer( true, true, true, true, true ) );
		}

		protected AdventureResult [] getDesiredItems( String message )
		{
			filterSelection( filters[0].isSelected(), filters[1].isSelected(), filters[2].isSelected(), filters[3].isSelected(), filters[4].isSelected() );
			return super.getDesiredItems( message );
		}

		private class StorageListener implements ActionListener
		{
			private int moveType;

			public StorageListener( int moveType )
			{	this.moveType = moveType;
			}

			public void actionPerformed( ActionEvent e )
			{
				Object [] items = getDesiredItems( "Deposit" );
				if ( items.length == 0 )
					return;

				if ( moveType != MOVE_ALL )
				{
					AdventureResult currentItem = null;
					for ( int i = 0; i < items.length; ++i )
					{
						currentItem = (AdventureResult) items[i];
						items[i] = currentItem.getInstance( moveType == MOVE_ONE ? 1 : currentItem.getCount() - 1 );
					}
				}

				(new RequestThread( new ClanStashRequest( items, ClanStashRequest.ITEMS_TO_STASH ) )).start();
			}
		}
	}

	/**
	 * Internal class used to handle everything related to
	 * placing items into the stash.
	 */

	private class WithdrawPanel extends ItemManagePanel
	{
		private JCheckBox [] filters;

		public WithdrawPanel()
		{
			super( "", ClanManager.getStash(), false );

			setButtons( new String [] { "take all", "take all but #", "take multiple", "refresh" },
				new ActionListener [] { new WithdrawListener( MOVE_ALL ), new WithdrawListener( MOVE_ALL_BUT ), new WithdrawListener( MOVE_MULTIPLE ),
				new RequestButton( "Refresh Items", new ClanStashRequest() ) } );

			filters = new JCheckBox[5];
			filters[0] = new FilterCheckBox( filters, elementList, "Show food", true );
			filters[1] = new FilterCheckBox( filters, elementList, "Show drink", true );
			filters[2] = new FilterCheckBox( filters, elementList, "Show others", true );
			filters[3] = new FilterCheckBox( filters, elementList, true, "Show no-sell", true );
			filters[4] = new FilterCheckBox( filters, elementList, true, "Show no-trade", true );

			for ( int i = 0; i < filters.length; ++i )
				optionPanel.add( filters[i] );

			elementList.setCellRenderer(
				AdventureResult.getAutoSellCellRenderer( true, true, true, true, true ) );
		}

		protected AdventureResult [] getDesiredItems( String message )
		{
			filterSelection( filters[0].isSelected(), filters[1].isSelected(), filters[2].isSelected(), filters[3].isSelected(), filters[4].isSelected() );
			return super.getDesiredItems( message );
		}

		private class WithdrawListener implements ActionListener
		{
			private int moveType;

			public WithdrawListener( int moveType )
			{	this.moveType = moveType;
			}

			public void actionPerformed( ActionEvent e )
			{
				Object [] items = getDesiredItems( "Take" );
				if ( items.length == 0 )
					return;

				if ( moveType != MOVE_ALL )
				{
					int quantity = 0;

					if ( moveType == MOVE_ALL_BUT )
						quantity = getQuantity( "Maximum number of each item allowed in the stash?", 100 );

					for ( int i = 0; i < items.length; ++i )
					{
						AdventureResult currentItem = (AdventureResult) items[i];
						if ( moveType == MOVE_MULTIPLE )
						{
							quantity = getQuantity( "Withdrawing multiple " + currentItem.getName() + "...", currentItem.getCount(), currentItem.getCount() );
							if ( quantity == 0 )
								return;

							items[i] = currentItem.getInstance( quantity );
						}
						else
						{
							items[i] = currentItem.getInstance( Math.max( 0, currentItem.getCount() - quantity ) );
						}
					}
				}

				(new RequestThread( new ClanStashRequest( items, ClanStashRequest.STASH_TO_ITEMS ) )).start();
			}
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

			setContent( elements, null, null, true );
		}

		public void actionConfirmed()
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

		public void actionCancelled()
		{	(new RequestThread( new MemberChanger() )).start();
		}

		private class MemberChanger implements Runnable
		{
			public void run()
			{
				if ( !finalizeTable( members ) )
					return;

				KoLmafia.updateDisplay( "Determining changes..." );

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
				(new ClanMembersRequest( titleChange.toArray(), newTitles.toArray(), boots.toArray() )).run();
				KoLmafia.updateDisplay( "Changes have been applied." );
			}
		}
	}

	private class MemberTableModel extends ListWrapperTableModel
	{
		public MemberTableModel()
		{
			super( new String [] { " ", "Name", "Clan Title", "Total Karma", "Boot" },
				new Class [] { JButton.class, String.class, String.class, Integer.class, Boolean.class },
				new boolean [] { false, false, true, false, true }, ClanSnapshotTable.getFilteredList() );
		}

		protected Vector constructVector( Object o )
		{
			ProfileRequest p = (ProfileRequest) o;

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
	}

	private class ShowProfileButton extends NestedInsideTableButton
	{
		private ProfileRequest profile;

		public ShowProfileButton( ProfileRequest profile )
		{
			super( JComponentUtilities.getImage( "icon_warning_sml.gif" ) );
			this.profile = profile;
		}

		public void mouseReleased( MouseEvent e )
		{
			Object [] parameters = new Object[1];
			parameters[0] = profile;
			createDisplay( ProfileFrame.class, parameters );
		}
	}

	private class SnapshotPanel extends LabeledKoLPanel
	{
		private JTextField mostAscensionsBoardSizeField;
		private JTextField mainBoardSizeField;
		private JTextField classBoardSizeField;
		private JTextField maxAgeField;

		private JCheckBox playerMoreThanOnceOption;
		private JCheckBox localProfileOption;

		private JCheckBox [] optionBoxes;
		private final String [][] options =
		{
			{ "<td>Lv</td><td>Mus</td><td>Mys</td><td>Mox</td><td>Total</td>", "Progression statistics (level, power, class):  " },
			{ "<td>Title</td><td>Rank</td><td>Karma</td>", "Internal clan statistics (title, karma):  " },
			{ "<td>Class</td><td>Path</td><td>Turns</td><td>Meat</td>", "Leaderboard (class, path, turns, wealth):  " },
			{ "<td>PVP</td><td>Food</td><td>Drink</td>", "Miscellaneous statistics (pvp, food, booze):  " },
			{ "<td>Created</td><td>Last Login</td>", "Creation and last login dates:  " },
		};

		public SnapshotPanel()
		{
			super( "Clan Snapshot", "snapshot", "logshot", new Dimension( 300, 20 ), new Dimension( 50, 20 ) );

			VerifiableElement [] elements = new VerifiableElement[ options.length + 7 ];

			mostAscensionsBoardSizeField = new JTextField( "20" );
			mainBoardSizeField = new JTextField( "10" );
			classBoardSizeField = new JTextField( "5" );
			maxAgeField = new JTextField( "0" );

			playerMoreThanOnceOption = new JCheckBox();
			localProfileOption = new JCheckBox();

			elements[0] = new VerifiableElement( "Most Ascensions Board Size:  ", mostAscensionsBoardSizeField );
			elements[1] = new VerifiableElement( "Fastest Ascensions Board Size:  ", mainBoardSizeField );
			elements[2] = new VerifiableElement( "Class Breakdown Board Size:  ", classBoardSizeField );
			elements[3] = new VerifiableElement( "Maximum Ascension Age (in days):  ", maxAgeField );
			elements[4] = new VerifiableElement();
			elements[5] = new VerifiableElement( "Add Internal Profile Links:  ", localProfileOption );
			elements[6] = new VerifiableElement( "Allow Multiple Appearances:  ", playerMoreThanOnceOption );

			optionBoxes = new JCheckBox[ options.length ];
			for ( int i = 0; i < options.length; ++i )
				optionBoxes[i] = new JCheckBox();

			for ( int i = 0; i < options.length; ++i )
				elements[i + 7] = new VerifiableElement( options[i][1], optionBoxes[i] );

			setContent( elements );
			String tableHeaderSetting = StaticEntity.getProperty( "clanRosterHeader" );
			for ( int i = 0; i < options.length; ++i )
				optionBoxes[i].setSelected( tableHeaderSetting.indexOf( options[i][0] ) != -1 );
		}

		public void actionConfirmed()
		{	(new RequestThread( new SnapshotRunnable() )).start();
		}

		public void actionCancelled()
		{	(new RequestThread( new StashlogRunnable() )).start();
		}

		private class StashlogRunnable implements Runnable
		{
			public void run()
			{	ClanManager.saveStashLog();
			}
		}

		private class SnapshotRunnable implements Runnable
		{
			public void run()
			{
				// Apply all the settings before generating the
				// needed clan ClanSnapshotTable.

				StringBuffer tableHeaderSetting = new StringBuffer();

				for ( int i = 0; i < options.length; ++i )
					if ( optionBoxes[i].isSelected() )
						tableHeaderSetting.append( options[i][0] );

				StaticEntity.setProperty( "clanRosterHeader", tableHeaderSetting.toString() + "<td>Ascensions</td>" );

				// Now that you've got everything, go ahead and
				// generate the snapshot.

				int mostAscensionsBoardSize = getValue( mostAscensionsBoardSizeField, Integer.MAX_VALUE );
				int mainBoardSize = getValue( mainBoardSizeField, Integer.MAX_VALUE );
				int classBoardSize = getValue( classBoardSizeField, Integer.MAX_VALUE );
				int maxAge = getValue( maxAgeField, Integer.MAX_VALUE );

				boolean playerMoreThanOnce = playerMoreThanOnceOption.isSelected();
				boolean localProfileLink = localProfileOption.isSelected();

				// Now that you've got everything, go ahead and
				// generate the snapshot.

				ClanManager.takeSnapshot( mostAscensionsBoardSize, mainBoardSize, classBoardSize, maxAge, playerMoreThanOnce, localProfileLink );
			}
		}

	}
}
