/**
 * Copyright (c) 2005-2007, KoLmafia development team
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
 *  [3] Neither the name "KoLmafia" nor the names of its contributors may
 *      be used to endorse or promote products derived from this software
 *      without specific prior written permission.
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

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Dimension;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import java.util.ArrayList;
import java.util.Vector;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import tab.CloseTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;

import javax.swing.table.TableModel;
import com.sun.java.forums.TableSorter;
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
		{	StaticEntity.getClient().makeRequest( (Runnable) buffField.getSelectedItem(), getValue( countField ) );
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
		{	RequestThread.postRequest( (Runnable) enemyList.getSelectedItem() );
		}

		public void actionCancelled()
		{	RequestThread.postRequest( new ClanListRequest() );
		}
	}

	private class WarfarePanel extends LabeledKoLPanel
	{
		private JTextField goodies;
		private JTextField oatmeal, recliners;
		private JTextField grunts, flyers, archers;

		public WarfarePanel()
		{
			super( "Prepare for WAR!!!", "purchase", "calculate", new Dimension( 120, 20 ), new Dimension( 200, 20 ) );

			goodies = new JTextField();
			oatmeal = new JTextField();
			recliners = new JTextField();
			grunts = new JTextField();
			flyers = new JTextField();
			archers = new JTextField();

			VerifiableElement [] elements = new VerifiableElement[6];
			elements[0] = new VerifiableElement( "Goodies: ", goodies );
			elements[1] = new VerifiableElement( "Oatmeal: ", oatmeal );
			elements[2] = new VerifiableElement( "Recliners: ", recliners );
			elements[3] = new VerifiableElement( "Ground Troops: ", grunts );
			elements[4] = new VerifiableElement( "Airborne Troops: ", flyers );
			elements[5] = new VerifiableElement( "La-Z-Archers: ", archers );

			setContent( elements );
		}

		public void actionConfirmed()
		{
			RequestThread.postRequest( new ClanMaterialsRequest(
				getValue( goodies ), getValue( oatmeal ), getValue( recliners ),
				getValue( grunts ), getValue( flyers ), getValue( archers ) ) );
		}

		public void actionCancelled()
		{
			int totalValue = getValue( goodies ) * 1000 + getValue( oatmeal ) * 3 + getValue( recliners ) * 1500 +
				getValue( grunts ) * 300 + getValue( flyers ) * 500 + getValue( archers ) * 500;

			JOptionPane.showMessageDialog( null, "This purchase will cost " + totalValue + " meat" );
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
		{	RequestThread.postRequest( new ClanStashRequest( getValue( amountField ) ) );
		}

		public void actionCancelled()
		{	JOptionPane.showMessageDialog( null, "The Hermit beat you to it.  ARGH!" );
		}
	}

	private class StoragePanel extends ItemManagePanel
	{
		public StoragePanel()
		{
			super( inventory );
			setButtons( new ActionListener [] { new StorageListener(), new RequestButton( "refresh", new EquipmentRequest( EquipmentRequest.CLOSET ) ) } );
		}

		private class StorageListener extends ThreadedListener
		{
			public void run()
			{
				Object [] items = getDesiredItems( "Deposit" );
				if ( items.length == 0 )
					return;

				RequestThread.postRequest( new ClanStashRequest( items, ClanStashRequest.ITEMS_TO_STASH ) );
			}

			public String toString()
			{	return "add items";
			}
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
			super( ClanManager.getStash() );

			setButtons( new ActionListener [] { new WithdrawListener( MOVE_ALL ), new WithdrawListener( MOVE_ALL_BUT ), new RequestButton( "refresh", new ClanStashRequest() ) } );
			elementList.setCellRenderer( AdventureResult.getDefaultRenderer() );
		}

		private class WithdrawListener extends ThreadedListener
		{
			private int moveType;

			public WithdrawListener( int moveType )
			{	this.moveType = moveType;
			}

			public void run()
			{
				if ( !KoLCharacter.canInteract() )
					return;

				Object [] items;

				if ( moveType == MOVE_ALL_BUT )
				{
					items = elementList.getSelectedValues();
					if ( items.length == 0 )
						items = elementModel.toArray();

					if ( items.length == 0 )
						return;

					AdventureResult currentItem;
					int quantity = getQuantity( "Maximum number of each item allowed in the stash?", 100 );

					if ( quantity == 0 )
						return;

					for ( int i = 0; i < items.length; ++i )
					{
						currentItem = (AdventureResult) items[i];
						items[i] = currentItem.getInstance( Math.max( 0, currentItem.getCount() - quantity ) );
					}
				}
				else
				{
					items = getDesiredItems( "Take" );
				}

				if ( items == null || items.length == 0 )
					return;

				RequestThread.postRequest( new ClanStashRequest( items, ClanStashRequest.STASH_TO_ITEMS ) );
			}

			public String toString()
			{	return moveType == MOVE_ALL_BUT ? "cap stash" : "take items";
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
		{
			ClanManager.applyFilter( matchSelect.getSelectedIndex() - 1, parameterSelect.getSelectedIndex(), valueField.getText() );
			KoLmafia.updateDisplay( "Search results retrieved." );
		}

		public void actionCancelled()
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
			RequestThread.postRequest( new ClanMembersRequest( titleChange.toArray(), newTitles.toArray(), boots.toArray() ) );
			KoLmafia.updateDisplay( "Changes have been applied." );
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

		public Vector constructVector( Object o )
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

		public SnapshotPanel()
		{
			super( "Clan Snapshot", "snapshot", "clan log", new Dimension( 250, 20 ), new Dimension( 50, 20 ) );

			VerifiableElement [] elements = new VerifiableElement[ 7 ];

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

			setContent( elements );
		}

		public void actionConfirmed()
		{
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

		public void actionCancelled()
		{	ClanManager.saveStashLog();
		}
	}
}
