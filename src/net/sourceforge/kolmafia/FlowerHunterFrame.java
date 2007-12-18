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
import java.awt.Color;
import java.awt.Dimension;
import java.util.Arrays;
import java.util.Vector;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.ToolTipManager;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;

import com.sun.java.forums.TableSorter;

public class FlowerHunterFrame extends KoLFrame implements ListSelectionListener
{
	private boolean isSimple;
	private CardLayout resultCards;
	private JPanel resultCardPanel;
	private AttackPanel attackPanel;

	private AutoHighlightField rankEntry;

	private Vector rankLabels = new Vector();
	private JTable [] resultsTable = new JTable[2];
	private TableSorter [] sortedModel = new TableSorter[2];
	private DefaultTableModel [] resultsModel = new DefaultTableModel[2];

	private ProfileRequest [] results;

	public FlowerHunterFrame()
	{
		super( "Flower Hunter" );

		this.tabs.add( "Search", new SearchPanel() );

		this.attackPanel = new AttackPanel();
		this.tabs.add( "Attack", this.attackPanel );

		this.tabs.add( "Profiler", new ClanPanel() );

		this.updateRank();
		this.framePanel.setLayout( new BorderLayout() );
		this.framePanel.add( this.tabs, BorderLayout.NORTH );

		this.results = new ProfileRequest[0];

		this.constructTableModel( 0, new String [] { "Name", "Clan", "Class", "Level", "Rank" } );
		this.constructTableModel( 1, new String [] { "Name", "Class", "Path", "Level", "Rank", "Drink", "Fashion", "Turns", "Login" } );

		SimpleScrollPane [] resultsScroller = new SimpleScrollPane[2];
		resultsScroller[0] = new SimpleScrollPane( this.resultsTable[0] );
		resultsScroller[1] = new SimpleScrollPane( this.resultsTable[1] );

		this.resultCards = new CardLayout();
		this.resultCardPanel = new JPanel( this.resultCards );
		this.resultCardPanel.add( resultsScroller[0], "0" );
		this.resultCardPanel.add( resultsScroller[1], "1" );

		this.framePanel.add( this.resultCardPanel, BorderLayout.CENTER );

		this.isSimple = true;
		this.resultCards.show( this.resultCardPanel, "0" );

		ToolTipManager.sharedInstance().unregisterComponent( this.resultsTable[0] );
		ToolTipManager.sharedInstance().unregisterComponent( this.resultsTable[1] );
	}

	private void constructTableModel( int index, String [] headers )
	{
		this.resultsModel[ index ] = new SearchResultsTableModel( headers );

		if ( this.resultsTable[ index ] == null )
			this.resultsTable[ index ] = new SearchResultsTable( this.resultsModel[ index ] );
		else
			this.resultsTable[ index ].setModel( this.resultsModel[ index ] );

		this.sortedModel[ index ] = new TableSorter( this.resultsModel[ index ], this.resultsTable[ index ].getTableHeader() );
		this.resultsTable[ index ].setModel( this.sortedModel[ index ] );
		this.resultsTable[ index ].getSelectionModel().addListSelectionListener( this );
		this.resultsTable[ index ].setPreferredScrollableViewportSize(
			new Dimension( (int) this.resultsTable[ index ].getPreferredScrollableViewportSize().getWidth(), 200 ) );
	}

	public void valueChanged( ListSelectionEvent e )
	{
		JTable table = this.resultsTable[ this.isSimple ? 0 : 1 ];
		int selectedIndex = table.getSelectionModel().isSelectionEmpty() ? 0 : 1;

		this.tabs.setSelectedIndex( selectedIndex );

		if ( selectedIndex == 1 )
		{
			int opponentCount = table.getSelectedRowCount();
			if ( opponentCount == 1 )
				this.attackPanel.setStatusMessage( "1 opponent selected." );
			else
				this.attackPanel.setStatusMessage( opponentCount + " opponents selected." );
		}
	}

	private JPanel getRankLabel()
	{
		JPanel rankPanel = new JPanel( new BorderLayout() );
		JLabel rankLabel = new JLabel( " ", JLabel.CENTER );

		this.rankLabels.add( rankLabel );
		rankPanel.add( rankLabel, BorderLayout.SOUTH );
		return rankPanel;
	}

	private void updateRank()
	{
		int equipmentPower = EquipmentDatabase.getPower( KoLCharacter.getEquipment( KoLCharacter.HAT ).getItemId() ) +
			EquipmentDatabase.getPower( KoLCharacter.getEquipment( KoLCharacter.PANTS ).getItemId() ) +
			EquipmentDatabase.getPower( KoLCharacter.getEquipment( KoLCharacter.SHIRT ).getItemId() );

		JLabel [] rankLabels = new JLabel[ this.rankLabels.size() ];
		this.rankLabels.toArray( rankLabels );

		for ( int i = 0; i < rankLabels.length; ++i )
			rankLabels[i].setText( "<html><center>Rank " + KoLCharacter.getPvpRank() + "<br>Fashion " + equipmentPower +
				"<br>Attacks " + KoLCharacter.getAttacksLeft() + "</center></html>" );

		this.rankEntry.setText( String.valueOf( Math.max( 10,
			KoLCharacter.getPvpRank() - 50 + Math.min( 11, KoLCharacter.getAttacksLeft() ) ) ) );
	}

	private class SearchPanel extends KoLPanel
	{
		private AutoHighlightField levelEntry;
		private AutoHighlightField limitEntry;

		public SearchPanel()
		{
			super( "search", "flowers" );

			this.levelEntry = new AutoHighlightField();
			FlowerHunterFrame.this.rankEntry = new AutoHighlightField();
			this.limitEntry = new AutoHighlightField();

			VerifiableElement [] elements = new VerifiableElement[3];
			elements[0] = new VerifiableElement( "Player Level: ", this.levelEntry );
			elements[1] = new VerifiableElement( "Max Rank: ", FlowerHunterFrame.this.rankEntry );
			elements[2] = new VerifiableElement( "Max Results: ", this.limitEntry );

			this.setContent( elements, true );
			this.eastContainer.add( getRankLabel(), BorderLayout.SOUTH );
		}

		public void actionConfirmed()
		{
			int index = 0;
			int resultLimit = getValue( this.limitEntry, 100 );

			FlowerHunterFrame.this.resultCards.show( FlowerHunterFrame.this.resultCardPanel, String.valueOf( index ) );
			KoLmafia.updateDisplay( "Conducting search..." );

			while ( !FlowerHunterFrame.this.resultsModel[ index ].getDataVector().isEmpty() )
			{
				FlowerHunterFrame.this.resultsModel[ index ].removeRow( 0 );
				FlowerHunterFrame.this.resultsModel[ index ].fireTableRowsDeleted( 0, 0 );
			}

			FlowerHunterRequest search = new FlowerHunterRequest( this.levelEntry.getText(), FlowerHunterFrame.this.rankEntry.getText() );
			RequestThread.postRequest( search );

			FlowerHunterFrame.this.results = new ProfileRequest[ FlowerHunterRequest.getSearchResults().size() ];
			FlowerHunterRequest.getSearchResults().toArray( FlowerHunterFrame.this.results );

			for ( int i = 0; i < resultLimit && i < FlowerHunterFrame.this.results.length && KoLmafia.permitsContinue(); ++i )
			{
				FlowerHunterFrame.this.resultsModel[ index ].addRow( this.getRow( FlowerHunterFrame.this.results[i] ) );
				FlowerHunterFrame.this.resultsModel[ index ].fireTableRowsInserted( i - 1, i - 1 );
			}

			KoLmafia.updateDisplay( "Search completed." );
			RequestThread.enableDisplayIfSequenceComplete();
		}

		public void actionCancelled()
		{
			KoLmafiaCLI.DEFAULT_SHELL.executeCommand( "flowers", "" );
			FlowerHunterFrame.this.updateRank();
		}

		public Object [] getRow( ProfileRequest result )
		{
			return new Object [] { result.getPlayerName(), result.getClanName(), result.getClassType(),
				result.getPlayerLevel(), result.getPvpRank() };
		}
	}

	private class ClanPanel extends KoLPanel
	{
		private AutoHighlightField clanId;

		public ClanPanel()
		{
			super( "profile", true );

			this.clanId = new AutoHighlightField();

			VerifiableElement [] elements = new VerifiableElement[1];
			elements[0] = new VerifiableElement( "Clan Id: ", this.clanId );

			this.setContent( elements, true );
			this.eastContainer.add( getRankLabel(), BorderLayout.SOUTH );
		}

		public void actionConfirmed()
		{
			FlowerHunterFrame.this.isSimple = false;

			FlowerHunterFrame.this.resultCards.show( FlowerHunterFrame.this.resultCardPanel, "1" );
			KoLmafia.updateDisplay( "Conducting search..." );

			while ( !FlowerHunterFrame.this.resultsModel[1].getDataVector().isEmpty() )
			{
				FlowerHunterFrame.this.resultsModel[1].removeRow( 0 );
				FlowerHunterFrame.this.resultsModel[1].fireTableRowsDeleted( 0, 0 );
			}

			FlowerHunterRequest search = new FlowerHunterRequest( this.clanId.getText() );
			RequestThread.postRequest( search );

			FlowerHunterFrame.this.results = new ProfileRequest[ FlowerHunterRequest.getSearchResults().size() ];
			FlowerHunterRequest.getSearchResults().toArray( FlowerHunterFrame.this.results );

			for ( int i = 0; i < FlowerHunterFrame.this.results.length; ++i )
			{
				FlowerHunterFrame.this.resultsModel[1].addRow( this.getRow( FlowerHunterFrame.this.results[i] ) );
				FlowerHunterFrame.this.resultsModel[1].fireTableRowsInserted( i - 1, i - 1 );
			}

			KoLmafia.updateDisplay( "Search completed." );
			RequestThread.enableDisplayIfSequenceComplete();
		}

		public void actionCancelled()
		{
		}

		public Object [] getRow( ProfileRequest result )
		{
			KoLmafia.updateDisplay( "Retrieving profile for " + result.getPlayerName() + "..." );

			return new Object [] { result.getPlayerName(), result.getClassType(), result.getRestriction(), result.getPlayerLevel(),
				result.getPvpRank(), result.getDrink(), result.getEquipmentPower(), result.getCurrentRun(), result.getLastLogin() };
		}
	}

	private class AttackPanel extends KoLPanel
	{
		private AutoHighlightField winMessage;
		private AutoHighlightField lossMessage;

		private JComboBox stanceSelect;
		private JComboBox victorySelect;

		public AttackPanel()
		{
			super( "attack", "profile" );

			this.winMessage = new AutoHighlightField( KoLSettings.getUserProperty( "defaultFlowerWinMessage" ) );
			this.lossMessage = new AutoHighlightField( KoLSettings.getUserProperty( "defaultFlowerLossMessage" ) );

			this.stanceSelect = new JComboBox();
			this.stanceSelect.addItem( "Bully your opponent" );
			this.stanceSelect.addItem( "Burninate your opponent" );
			this.stanceSelect.addItem( "Backstab your opponent" );

			this.victorySelect = new JComboBox();
			this.victorySelect.addItem( "Steal a pretty flower" );
			this.victorySelect.addItem( "Fight for leaderboard rank" );

			if ( KoLCharacter.canInteract() )
				this.victorySelect.addItem( "Nab yourself some dignity" );

			VerifiableElement [] elements = new VerifiableElement[4];
			elements[0] = new VerifiableElement( "Fight Using: ", this.stanceSelect );
			elements[1] = new VerifiableElement( "PvP Objective: ", this.victorySelect );
			elements[2] = new VerifiableElement( "Win Message: ", this.winMessage );
			elements[3] = new VerifiableElement( "Loss Message: ", this.lossMessage );

			if ( KoLCharacter.getBaseMuscle() >= KoLCharacter.getBaseMysticality() && KoLCharacter.getBaseMuscle() >= KoLCharacter.getBaseMoxie() )
				this.stanceSelect.setSelectedIndex( 0 );
			else if ( KoLCharacter.getBaseMysticality() >= KoLCharacter.getBaseMuscle() && KoLCharacter.getBaseMysticality() >= KoLCharacter.getBaseMoxie() )
				this.stanceSelect.setSelectedIndex( 1 );
			else
				this.stanceSelect.setSelectedIndex( 2 );

			this.setContent( elements, true );
			this.eastContainer.add( getRankLabel(), BorderLayout.SOUTH );
		}

		public void actionConfirmed()
		{
			if ( KoLCharacter.isFallingDown() )
			{
				KoLmafia.updateDisplay( ERROR_STATE, "You can't pick fights while drunk." );
				return;
			}

			ProfileRequest [] selection = this.getSelection();
			Arrays.sort( selection );

			String mission = null;
			switch ( this.victorySelect.getSelectedIndex() )
			{
			case 0:
				mission = "flowers";
				break;

			case 1:
				mission = "rank";
				break;

			case 2:
				mission = "dignity";
				break;
			}

			RequestThread.openRequestSequence();

			FlowerHunterRequest request = new FlowerHunterRequest();
			RequestThread.postRequest( request );

			request = new FlowerHunterRequest( "",
				this.stanceSelect.getSelectedIndex() + 1, mission, this.winMessage.getText(), this.lossMessage.getText() );

			KoLmafiaCLI.executeFlowerHuntRequest( selection, request );
			FlowerHunterFrame.this.updateRank();

			if ( KoLmafia.permitsContinue() )
				KoLmafia.updateDisplay( "Attacks completed." );

			RequestThread.closeRequestSequence();
			this.switchToSearch();
		}

		private void switchToSearch()
		{
			int index = FlowerHunterFrame.this.isSimple ? 0 : 1;
			boolean shouldSwitch = true;

			int minimumRank = KoLCharacter.getPvpRank() - 50;

			for ( int i = 0; i < FlowerHunterFrame.this.results.length; ++i )
				shouldSwitch &= minimumRank > FlowerHunterFrame.this.results[ FlowerHunterFrame.this.sortedModel[ index ].modelIndex( i ) ].getPvpRank().intValue();

			if ( shouldSwitch )
				FlowerHunterFrame.this.tabs.setSelectedIndex(0);
		}

		public void actionCancelled()
		{
			ProfileRequest [] selection = this.getSelection();

			for ( int i = 0; i < selection.length; ++i )
				ProfileFrame.showRequest( selection[i] );
		}

		private ProfileRequest [] getSelection()
		{
			int index = FlowerHunterFrame.this.isSimple ? 0 : 1;
			Vector selectionVector = new Vector();

			for ( int i = 0; i < FlowerHunterFrame.this.results.length; ++i )
				if ( FlowerHunterFrame.this.resultsTable[ index ].getSelectionModel().isSelectedIndex( i ) )
					selectionVector.add( FlowerHunterFrame.this.results[ FlowerHunterFrame.this.sortedModel[ index ].modelIndex( i ) ] );

			ProfileRequest [] selection = new ProfileRequest[ selectionVector.size() ];
			selectionVector.toArray( selection );
			return selection;
		}
	}

	private static class SearchResultsTable extends JTable
	{
		public SearchResultsTable( DefaultTableModel model )
		{	super( model );
		}

		public TableCellRenderer getCellRenderer( int row, int column )
		{
			if ( KoLSettings.getUserProperty( "currentPvpVictories" ).indexOf( (String) this.getValueAt( row, 0 ) ) != -1 )
				return DISABLED_ROW_RENDERER;

			return ENABLED_ROW_RENDERER;
		}
	}

	private static final DefaultTableCellRenderer DISABLED_ROW_RENDERER = new DefaultTableCellRenderer();
	private static final DefaultTableCellRenderer ENABLED_ROW_RENDERER = new DefaultTableCellRenderer();

	static
	{
		DISABLED_ROW_RENDERER.setForeground( Color.gray );
	}

	private static class SearchResultsTableModel extends DefaultTableModel
	{
		public SearchResultsTableModel( Object [] headers )
		{	super( headers, 0 );
		}

		public Class getColumnClass( int c )
		{	return this.getRowCount() == 0 || this.getValueAt( 0, c ) == null ? Object.class : this.getValueAt( 0, c ).getClass();
		}

		public boolean isCellEditable( int row, int col )
		{	return false;
		}
	}
}
