/**
 * Copyright (c) 2005-2011, KoLmafia development team
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

package net.sourceforge.kolmafia.swingui;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Vector;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;

import net.java.dev.spellcast.utilities.JComponentUtilities;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.StaticEntity;
import net.sourceforge.kolmafia.persistence.ProfileSnapshot;
import net.sourceforge.kolmafia.request.ClanBuffRequest;
import net.sourceforge.kolmafia.request.ClanMembersRequest;
import net.sourceforge.kolmafia.request.ClanStashRequest;
import net.sourceforge.kolmafia.request.ClanWarRequest;
import net.sourceforge.kolmafia.request.EquipmentRequest;
import net.sourceforge.kolmafia.request.ProfileRequest;
import net.sourceforge.kolmafia.session.ClanManager;
import net.sourceforge.kolmafia.swingui.button.RequestButton;
import net.sourceforge.kolmafia.swingui.listener.TableButtonListener;
import net.sourceforge.kolmafia.swingui.listener.ThreadedListener;
import net.sourceforge.kolmafia.swingui.panel.GenericPanel;
import net.sourceforge.kolmafia.swingui.panel.ItemManagePanel;
import net.sourceforge.kolmafia.swingui.panel.LabeledPanel;
import net.sourceforge.kolmafia.swingui.table.ButtonRenderer;
import net.sourceforge.kolmafia.swingui.table.ListWrapperTableModel;
import net.sourceforge.kolmafia.swingui.table.TransparentTable;
import net.sourceforge.kolmafia.swingui.widget.AutoFilterComboBox;
import net.sourceforge.kolmafia.swingui.widget.AutoHighlightTextField;
import net.sourceforge.kolmafia.swingui.widget.GenericScrollPane;
import net.sourceforge.kolmafia.swingui.widget.ListCellRendererFactory;
import net.sourceforge.kolmafia.utilities.InputFieldUtilities;

import com.sun.java.forums.TableSorter;

/**
 * An extension of <code>KoLFrame</code> which handles all the clan management functionality of Kingdom of Loathing.
 */

public class ClanManageFrame
	extends GenericFrame
{
	private static final int MOVE_ALL = 2;
	private static final int MOVE_ALL_BUT = 3;
	private final JTable members;
	private final SnapshotPanel snapshot;
	private final ClanBuffPanel clanBuff;
	private final StoragePanel storing;
	private final WithdrawPanel withdrawal;
	private final DonationPanel donation;
	private final AttackPanel attacks;
	private final WarfarePanel warfare;
	private final MemberSearchPanel search;

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
		adminPanel.add( this.attacks );
		adminPanel.add( this.snapshot );

		this.addTab( "Admin", adminPanel );

		JPanel spendPanel = new JPanel();
		spendPanel.setLayout( new BoxLayout( spendPanel, BoxLayout.Y_AXIS ) );
		spendPanel.add( this.donation );
		spendPanel.add( this.clanBuff );
		spendPanel.add( this.warfare );

		this.addTab( "Coffers", spendPanel );
		this.tabs.addTab( "Deposit", this.storing );
		this.tabs.addTab( "Withdraw", this.withdrawal );

		this.members = new TransparentTable( new MemberTableModel() );
		this.members.setModel( new TableSorter( this.members.getModel(), this.members.getTableHeader() ) );
		this.members.getTableHeader().setReorderingAllowed( false );

		this.members.setRowSelectionAllowed( false );
		this.members.setAutoResizeMode( JTable.AUTO_RESIZE_NEXT_COLUMN );

		this.members.addMouseListener( new TableButtonListener( this.members ) );
		this.members.setDefaultRenderer( JButton.class, new ButtonRenderer() );

		this.members.setShowGrid( false );
		this.members.setIntercellSpacing( new Dimension( 5, 5 ) );
		this.members.setRowHeight( 25 );

		this.members.getColumnModel().getColumn( 0 ).setMinWidth( 30 );
		this.members.getColumnModel().getColumn( 0 ).setMaxWidth( 30 );

		this.members.getColumnModel().getColumn( 1 ).setMinWidth( 120 );
		this.members.getColumnModel().getColumn( 1 ).setMaxWidth( 120 );

		this.members.getColumnModel().getColumn( 3 ).setMinWidth( 120 );
		this.members.getColumnModel().getColumn( 3 ).setMaxWidth( 120 );

		this.members.getColumnModel().getColumn( 4 ).setMinWidth( 45 );
		this.members.getColumnModel().getColumn( 4 ).setMaxWidth( 45 );

		GenericScrollPane results = new GenericScrollPane( this.members );
		JComponentUtilities.setComponentSize( results, 400, 300 );

		JPanel searchPanel = new JPanel( new BorderLayout() );
		searchPanel.add( this.search, BorderLayout.NORTH );

		JPanel resultsPanel = new JPanel( new BorderLayout() );
		resultsPanel.add( this.members.getTableHeader(), BorderLayout.NORTH );
		resultsPanel.add( results, BorderLayout.CENTER );
		searchPanel.add( resultsPanel, BorderLayout.CENTER );

		this.tabs.addTab( "Member Search", searchPanel );

		this.framePanel.setLayout( new CardLayout( 10, 10 ) );
		this.framePanel.add( this.tabs, "" );
	}

	/**
	 * An internal class which represents the panel used for clan buffs in the <code>ClanManageFrame</code>.
	 */

	private class ClanBuffPanel
		extends LabeledPanel
	{
		private final boolean isBuffing;
		private final JComboBox buffField;
		private final AutoHighlightTextField countField;

		public ClanBuffPanel()
		{
			super( "Buy Clan Buffs", "purchase", "take break", new Dimension( 80, 20 ), new Dimension( 240, 20 ) );
			this.isBuffing = false;

			this.buffField = new JComboBox( ClanBuffRequest.getRequestList() );
			this.countField = new AutoHighlightTextField();

			VerifiableElement[] elements = new VerifiableElement[ 2 ];
			elements[ 0 ] = new VerifiableElement( "Clan Buff: ", this.buffField );
			elements[ 1 ] = new VerifiableElement( "# of times: ", this.countField );

			this.setContent( elements );
		}

		public void actionConfirmed()
		{
			StaticEntity.getClient().makeRequest(
				(Runnable) this.buffField.getSelectedItem(), InputFieldUtilities.getValue( this.countField ) );
		}

		public void actionCancelled()
		{
			if ( this.isBuffing )
			{
				KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "Purchase attempts cancelled." );
			}
		}
	}

	/**
	 * An internal class which represents the panel used for clan buffs in the <code>ClanManageFrame</code>.
	 */

	private class AttackPanel
		extends LabeledPanel
	{
		private final JLabel nextAttack;
		private final AutoFilterComboBox enemyList;

		public AttackPanel()
		{
			super( "Loot Another Clan", "attack", "refresh", new Dimension( 80, 20 ), new Dimension( 240, 20 ) );

			this.nextAttack = new JLabel( ClanWarRequest.getNextAttack() );
			this.enemyList = new AutoFilterComboBox( ClanWarRequest.getEnemyClans(), false );

			VerifiableElement[] elements = new VerifiableElement[ 2 ];
			elements[ 0 ] = new VerifiableElement( "Victim: ", this.enemyList );
			elements[ 1 ] = new VerifiableElement( " ", this.nextAttack );
			this.setContent( elements );
		}

		public void actionConfirmed()
		{
			RequestThread.postRequest( (ClanWarRequest) this.enemyList.getSelectedItem() );
		}

		public void actionCancelled()
		{
			RequestThread.postRequest( new ClanWarRequest() );
			this.nextAttack.setText( ClanWarRequest.getNextAttack() );
		}
	}

	private class WarfarePanel
		extends LabeledPanel
	{
		private final AutoHighlightTextField goodies;
		private final AutoHighlightTextField oatmeal, recliners;
		private final AutoHighlightTextField grunts, flyers, archers;

		public WarfarePanel()
		{
			super( "Prepare for WAR!!!", "purchase", "calculate", new Dimension( 120, 20 ), new Dimension( 200, 20 ) );

			this.goodies = new AutoHighlightTextField();
			this.oatmeal = new AutoHighlightTextField();
			this.recliners = new AutoHighlightTextField();
			this.grunts = new AutoHighlightTextField();
			this.flyers = new AutoHighlightTextField();
			this.archers = new AutoHighlightTextField();

			VerifiableElement[] elements = new VerifiableElement[ 6 ];
			elements[ 0 ] = new VerifiableElement( "Goodies: ", this.goodies );
			elements[ 1 ] = new VerifiableElement( "Oatmeal: ", this.oatmeal );
			elements[ 2 ] = new VerifiableElement( "Recliners: ", this.recliners );
			elements[ 3 ] = new VerifiableElement( "Ground Troops: ", this.grunts );
			elements[ 4 ] = new VerifiableElement( "Airborne Troops: ", this.flyers );
			elements[ 5 ] = new VerifiableElement( "La-Z-Archers: ", this.archers );

			this.setContent( elements );
		}

		public void actionConfirmed()
		{
			RequestThread.postRequest( new ClanWarRequest(
				InputFieldUtilities.getValue( this.goodies ), InputFieldUtilities.getValue( this.oatmeal ),
				InputFieldUtilities.getValue( this.recliners ), InputFieldUtilities.getValue( this.grunts ),
				InputFieldUtilities.getValue( this.flyers ), InputFieldUtilities.getValue( this.archers ) ) );
		}

		public void actionCancelled()
		{
			int totalValue =
				InputFieldUtilities.getValue( this.goodies ) * 1000 + InputFieldUtilities.getValue( this.oatmeal ) * 3 + InputFieldUtilities.getValue( this.recliners ) * 1500 + InputFieldUtilities.getValue( this.grunts ) * 300 + InputFieldUtilities.getValue( this.flyers ) * 500 + InputFieldUtilities.getValue( this.archers ) * 500;

			InputFieldUtilities.alert( "This purchase will cost " + totalValue + " meat" );
		}
	}

	/**
	 * An internal class which represents the panel used for donations to the clan coffer.
	 */

	private class DonationPanel
		extends LabeledPanel
	{
		private final AutoHighlightTextField amountField;

		public DonationPanel()
		{
			super( "Fund Your Clan", "donate meat", "loot clan", new Dimension( 80, 20 ), new Dimension( 240, 20 ) );

			this.amountField = new AutoHighlightTextField();
			VerifiableElement[] elements = new VerifiableElement[ 1 ];
			elements[ 0 ] = new VerifiableElement( "Amount: ", this.amountField );
			this.setContent( elements );
		}

		public void actionConfirmed()
		{
			RequestThread.postRequest( new ClanStashRequest( InputFieldUtilities.getValue( this.amountField ) ) );
		}

		public void actionCancelled()
		{
			InputFieldUtilities.alert( "The Hermit beat you to it.  ARGH!" );
		}
	}

	private class StoragePanel
		extends ItemManagePanel
	{
		public StoragePanel()
		{
			super( KoLConstants.inventory );
			this.setButtons( new ActionListener[] { new StorageListener(), new RequestButton(
				"refresh", new EquipmentRequest( EquipmentRequest.REFRESH ) ) } );
		}

		private class StorageListener
			extends ThreadedListener
		{
			protected void execute()
			{
				Object[] items = StoragePanel.this.getDesiredItems( "Deposit" );
				if ( items == null )
				{
					return;
				}

				RequestThread.postRequest( new ClanStashRequest( items, ClanStashRequest.ITEMS_TO_STASH ) );
			}

			public String toString()
			{
				return "add items";
			}
		}
	}

	/**
	 * Internal class used to handle everything related to placing items into the stash.
	 */

	private class WithdrawPanel
		extends ItemManagePanel
	{
		public WithdrawPanel()
		{
			super( ClanManager.getStash() );

			this.setButtons( new ActionListener[] { new WithdrawListener( ClanManageFrame.MOVE_ALL ), new WithdrawListener(
				ClanManageFrame.MOVE_ALL_BUT ), new RequestButton( "refresh", new ClanStashRequest() ) } );
			this.elementList.setCellRenderer( ListCellRendererFactory.getDefaultRenderer() );
		}

		private class WithdrawListener
			extends ThreadedListener
		{
			private final int moveType;

			public WithdrawListener( final int moveType )
			{
				this.moveType = moveType;
			}

			protected void execute()
			{
				if ( !KoLCharacter.canInteract() )
				{
					return;
				}

				Object[] items;

				if ( this.moveType == ClanManageFrame.MOVE_ALL_BUT )
				{
					items = WithdrawPanel.this.elementList.getSelectedValues();
					if ( items.length == 0 )
					{
						items = WithdrawPanel.this.elementModel.toArray();
					}

					if ( items.length == 0 )
					{
						return;
					}

					AdventureResult currentItem;
					int quantity =
						InputFieldUtilities.getQuantity(
							"Maximum number of each item allowed in the stash?", Integer.MAX_VALUE, 100 );

					if ( quantity == 0 )
					{
						return;
					}

					for ( int i = 0; i < items.length; ++i )
					{
						currentItem = (AdventureResult) items[ i ];
						items[ i ] = currentItem.getInstance( Math.max( 0, currentItem.getCount() - quantity ) );
					}
				}
				else
				{
					items = WithdrawPanel.this.getDesiredItems( "Take" );
				}

				if ( items == null )
				{
					return;
				}

				RequestThread.postRequest( new ClanStashRequest( items, ClanStashRequest.STASH_TO_ITEMS ) );
			}

			public String toString()
			{
				return this.moveType == ClanManageFrame.MOVE_ALL_BUT ? "cap stash" : "take items";
			}
		}
	}

	private class MemberSearchPanel
		extends GenericPanel
	{
		private final JComboBox parameterSelect;
		private final JComboBox matchSelect;
		private final AutoHighlightTextField valueField;

		public MemberSearchPanel()
		{
			super( "search clan", "apply changes", new Dimension( 80, 20 ), new Dimension( 240, 20 ) );

			this.parameterSelect = new JComboBox();
			for ( int i = 0; i < ProfileSnapshot.FILTER_NAMES.length; ++i )
			{
				this.parameterSelect.addItem( ProfileSnapshot.FILTER_NAMES[ i ] );
			}

			this.matchSelect = new JComboBox();
			this.matchSelect.addItem( "Less than..." );
			this.matchSelect.addItem( "Equal to..." );
			this.matchSelect.addItem( "Greater than..." );

			this.valueField = new AutoHighlightTextField();

			VerifiableElement[] elements = new VerifiableElement[ 3 ];
			elements[ 0 ] = new VerifiableElement( "Parameter: ", this.parameterSelect );
			elements[ 1 ] = new VerifiableElement( "Constraint: ", this.matchSelect );
			elements[ 2 ] = new VerifiableElement( "Value:", this.valueField );

			this.setContent( elements, true );
		}

		public void actionConfirmed()
		{
			ClanManager.applyFilter(
				this.matchSelect.getSelectedIndex() - 1, this.parameterSelect.getSelectedIndex(),
				this.valueField.getText() );
			KoLmafia.updateDisplay( "Search results retrieved." );
		}

		public void actionCancelled()
		{
			if ( !InputFieldUtilities.finalizeTable( ClanManageFrame.this.members ) )
			{
				return;
			}

			KoLmafia.updateDisplay( "Determining changes..." );

			ArrayList titleChange = new ArrayList();
			ArrayList newTitles = new ArrayList();
			ArrayList boots = new ArrayList();

			for ( int i = 0; i < ClanManageFrame.this.members.getRowCount(); ++i )
			{
				if ( ( (Boolean) ClanManageFrame.this.members.getValueAt( i, 4 ) ).booleanValue() )
				{
					boots.add( ClanManageFrame.this.members.getValueAt( i, 1 ) );
				}

				titleChange.add( ClanManageFrame.this.members.getValueAt( i, 1 ) );
				newTitles.add( ClanManageFrame.this.members.getValueAt( i, 2 ) );
			}

			KoLmafia.updateDisplay( "Applying changes..." );
			RequestThread.postRequest( new ClanMembersRequest(
				titleChange.toArray(), newTitles.toArray(), boots.toArray() ) );
			KoLmafia.updateDisplay( "Changes have been applied." );
		}
	}

	private class MemberTableModel
		extends ListWrapperTableModel
	{
		public MemberTableModel()
		{
			super(
				new String[] { " ", "Name", "Clan Title", "Total Karma", "Boot" },
				new Class[] { JButton.class, String.class, String.class, Integer.class, Boolean.class },
				new boolean[] { false, false, true, false, true }, ProfileSnapshot.getFilteredList() );
		}

		public Vector constructVector( final Object o )
		{
			ProfileRequest p = (ProfileRequest) o;

			Vector value = new Vector();

			JButton profileButton = new JButton( JComponentUtilities.getImage( "icon_warning_sml.gif" ) );
			profileButton.addMouseListener( new ShowProfileListener( p ) );
			JComponentUtilities.setComponentSize( profileButton, 20, 20 );

			value.add( profileButton );
			value.add( p.getPlayerName() );
			value.add( p.getTitle() );
			value.add( p.getKarma() );
			value.add( new Boolean( false ) );

			return value;
		}
	}

	private class ShowProfileListener
		extends ThreadedListener
	{
		private final ProfileRequest profile;

		public ShowProfileListener( final ProfileRequest profile )
		{

			this.profile = profile;
		}

		protected void execute()
		{
            ProfileFrame.showRequest(this.profile);
		}
	}

	private class SnapshotPanel
		extends LabeledPanel
	{
		private final AutoHighlightTextField mostAscensionsBoardSizeField;
		private final AutoHighlightTextField mainBoardSizeField;
		private final AutoHighlightTextField classBoardSizeField;
		private final AutoHighlightTextField maxAgeField;

		private final JCheckBox playerMoreThanOnceOption;
		private final JCheckBox localProfileOption;

		public SnapshotPanel()
		{
			super( "Clan Snapshot", "snapshot", "activity log", new Dimension( 250, 20 ), new Dimension( 50, 20 ) );

			VerifiableElement[] elements = new VerifiableElement[ 7 ];

			this.mostAscensionsBoardSizeField = new AutoHighlightTextField( "20" );
			this.mainBoardSizeField = new AutoHighlightTextField( "10" );
			this.classBoardSizeField = new AutoHighlightTextField( "5" );
			this.maxAgeField = new AutoHighlightTextField( "0" );

			this.playerMoreThanOnceOption = new JCheckBox();
			this.localProfileOption = new JCheckBox();

			elements[ 0 ] = new VerifiableElement( "Most Ascensions Board Size:  ", this.mostAscensionsBoardSizeField );
			elements[ 1 ] = new VerifiableElement( "Fastest Ascensions Board Size:  ", this.mainBoardSizeField );
			elements[ 2 ] = new VerifiableElement( "Class Breakdown Board Size:  ", this.classBoardSizeField );
			elements[ 3 ] = new VerifiableElement( "Maximum Ascension Age (in days):  ", this.maxAgeField );
			elements[ 4 ] = new VerifiableElement();
			elements[ 5 ] = new VerifiableElement( "Add Internal Profile Links:  ", this.localProfileOption );
			elements[ 6 ] = new VerifiableElement( "Allow Multiple Appearances:  ", this.playerMoreThanOnceOption );

			this.setContent( elements, true );
		}

		public void actionConfirmed()
		{
			// Now that you've got everything, go ahead and
			// generate the snapshot.

			int mostAscensionsBoardSize = InputFieldUtilities.getValue( this.mostAscensionsBoardSizeField, Integer.MAX_VALUE );
			int mainBoardSize = InputFieldUtilities.getValue( this.mainBoardSizeField, Integer.MAX_VALUE );
			int classBoardSize = InputFieldUtilities.getValue( this.classBoardSizeField, Integer.MAX_VALUE );
			int maxAge = InputFieldUtilities.getValue( this.maxAgeField, Integer.MAX_VALUE );

			boolean playerMoreThanOnce = this.playerMoreThanOnceOption.isSelected();
			boolean localProfileLink = this.localProfileOption.isSelected();

			// Now that you've got everything, go ahead and
			// generate the snapshot.

			ClanManager.takeSnapshot(
				mostAscensionsBoardSize, mainBoardSize, classBoardSize, maxAge, playerMoreThanOnce, localProfileLink );
		}

		public void actionCancelled()
		{
			ClanManager.saveStashLog();
		}
	}
}
