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

import java.util.Arrays;
import java.util.List;
import java.util.Vector;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ToolTipManager;

import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import com.sun.java.forums.TableSorter;

public class FlowerHunterFrame extends KoLFrame implements ListSelectionListener
{
	private boolean isSimple;
	private CardLayout resultCards;
	private JPanel resultCardPanel;
	private AttackPanel attackPanel;

	private Vector rankLabels = new Vector();
	private JTable [] resultsTable = new JTable[2];
	private TableSorter [] sortedModel = new TableSorter[2];
	private DefaultTableModel [] resultsModel = new DefaultTableModel[2];

	private ProfileRequest [] results;

	public FlowerHunterFrame()
	{
		super( "Flower Hunter" );

		tabs.add( "Search", new SearchPanel() );

		attackPanel = new AttackPanel();
		tabs.add( "Attack", attackPanel );

		tabs.add( "Profiler", new ClanPanel() );

		updateRank();
		framePanel.setLayout( new BorderLayout() );
		framePanel.add( tabs, BorderLayout.NORTH );

		results = new ProfileRequest[0];

		constructTableModel( 0, new String [] { "Name", "Clan", "Class", "Level", "Rank" } );
		constructTableModel( 1, new String [] { "Name", "Class", "Path", "Level", "Rank", "Drink", "Fashion", "Turns", "Login" } );

		SimpleScrollPane [] resultsScroller = new SimpleScrollPane[2];
		resultsScroller[0] = new SimpleScrollPane( resultsTable[0] );
		resultsScroller[1] = new SimpleScrollPane( resultsTable[1] );

		resultCards = new CardLayout();
		resultCardPanel = new JPanel( resultCards );
		resultCardPanel.add( resultsScroller[0], "0" );
		resultCardPanel.add( resultsScroller[1], "1" );

		framePanel.add( resultCardPanel, BorderLayout.CENTER );

		this.isSimple = true;
		resultCards.show( resultCardPanel, "0" );

		ToolTipManager.sharedInstance().unregisterComponent( resultsTable[0] );
		ToolTipManager.sharedInstance().unregisterComponent( resultsTable[1] );
	}

	private void constructTableModel( int index, String [] headers )
	{
		resultsModel[ index ] = new SearchResultsTableModel( headers );

		if ( resultsTable[ index ] == null )
			resultsTable[ index ] = new JTable( resultsModel[ index ] );
		else
			resultsTable[ index ].setModel( resultsModel[ index ] );

		sortedModel[ index ] = new TableSorter( resultsModel[ index ], resultsTable[ index ].getTableHeader() );
		resultsTable[ index ].setModel( sortedModel[ index ] );
		resultsTable[ index ].getSelectionModel().addListSelectionListener( this );
		resultsTable[ index ].setPreferredScrollableViewportSize(
			new Dimension( (int) resultsTable[ index ].getPreferredScrollableViewportSize().getWidth(), 200 ) );
	}

	public void valueChanged( ListSelectionEvent e )
	{
		JTable table = resultsTable[ isSimple ? 0 : 1 ];
		int selectedIndex = table.getSelectionModel().isSelectionEmpty() ? 0 : 1;

		tabs.setSelectedIndex( selectedIndex );

		if ( selectedIndex == 1 )
		{
			int opponentCount = table.getSelectedRowCount();
			if ( opponentCount == 1 )
				attackPanel.setStatusMessage( "1 opponent selected." );
			else
				attackPanel.setStatusMessage( opponentCount + " opponents selected." );
		}
	}

	private JPanel getRankLabel()
	{
		JPanel rankPanel = new JPanel( new BorderLayout() );
		JLabel rankLabel = new JLabel( " ", JLabel.CENTER );

		rankLabels.add( rankLabel );
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
	}

	private class SearchPanel extends KoLPanel
	{
		private JTextField levelEntry;
		private JTextField rankEntry;
		private JTextField limitEntry;

		public SearchPanel()
		{
			super( "simple", "detail" );

			levelEntry = new JTextField();
			rankEntry = new JTextField();
			limitEntry = new JTextField();

			VerifiableElement [] elements = new VerifiableElement[3];
			elements[0] = new VerifiableElement( "Player Level: ", levelEntry );
			elements[1] = new VerifiableElement( "Max Rank: ", rankEntry );
			elements[2] = new VerifiableElement( "Max Results: ", limitEntry );

			setContent( elements, null, getRankLabel(), true );
		}

		public void actionConfirmed()
		{
			isSimple = true;
			executeSearch();
		}

		public void actionCancelled()
		{
			isSimple = false;
			executeSearch();
		}

		public void executeSearch()
		{
			int index = isSimple ? 0 : 1;
			int resultLimit = getValue( limitEntry, 100 );

			resultCards.show( resultCardPanel, String.valueOf( index ) );
			KoLmafia.updateDisplay( "Conducting search..." );

			while ( !resultsModel[ index ].getDataVector().isEmpty() )
			{
				resultsModel[ index ].removeRow( 0 );
				resultsModel[ index ].fireTableRowsDeleted( 0, 0 );
			}

			FlowerHunterRequest search = new FlowerHunterRequest( levelEntry.getText(), rankEntry.getText() );
			RequestThread.postRequest( search );

			results = new ProfileRequest[ search.getSearchResults().size() ];
			search.getSearchResults().toArray( results );

			for ( int i = 0; i < resultLimit && i < results.length; ++i )
			{
				resultsModel[ index ].addRow( getRow( results[i], isSimple ) );
				resultsModel[ index ].fireTableRowsInserted( i - 1, i - 1 );
			}

			KoLmafia.updateDisplay( "Search completed." );
			RequestThread.enableDisplayIfSequenceComplete();
		}

		public Object [] getRow( ProfileRequest result, boolean isSimple )
		{
			if ( isSimple )
				return new Object [] { result.getPlayerName(), result.getClanName(), result.getClassType(),
					result.getPlayerLevel(), result.getPvpRank() };

			KoLmafia.updateDisplay( "Retrieving profile for " + result.getPlayerName() + "..." );

			return new Object [] { result.getPlayerName(), result.getClassType(), result.getRestriction(), result.getPlayerLevel(),
				result.getPvpRank(), result.getDrink(), result.getEquipmentPower(), result.getCurrentRun(), result.getLastLogin() };
		}
	}

	private class ClanPanel extends KoLPanel
	{
		private JTextField clanId;

		public ClanPanel()
		{
			super( "profile", true );

			clanId = new JTextField();

			VerifiableElement [] elements = new VerifiableElement[1];
			elements[0] = new VerifiableElement( "Clan Id: ", clanId );

			setContent( elements, null, getRankLabel(), true );
		}

		public void actionConfirmed()
		{
			isSimple = false;

			resultCards.show( resultCardPanel, "1" );
			KoLmafia.updateDisplay( "Conducting search..." );

			while ( !resultsModel[1].getDataVector().isEmpty() )
			{
				resultsModel[1].removeRow( 0 );
				resultsModel[1].fireTableRowsDeleted( 0, 0 );
			}

			FlowerHunterRequest search = new FlowerHunterRequest( clanId.getText() );
			RequestThread.postRequest( search );

			results = new ProfileRequest[ search.getSearchResults().size() ];
			search.getSearchResults().toArray( results );

			for ( int i = 0; i < results.length; ++i )
			{
				resultsModel[1].addRow( getRow( results[i] ) );
				resultsModel[1].fireTableRowsInserted( i - 1, i - 1 );
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
		private JTextField winMessage;
		private JTextField lossMessage;

		private JComboBox stanceSelect;
		private JComboBox victorySelect;

		public AttackPanel()
		{
			super( "attack", "profile" );

			winMessage = new JTextField( StaticEntity.getProperty( "defaultFlowerWinMessage" ) );
			lossMessage = new JTextField( StaticEntity.getProperty( "defaultFlowerLossMessage" ) );

			stanceSelect = new JComboBox();
			stanceSelect.addItem( "Bully your opponent" );
			stanceSelect.addItem( "Burninate your opponent" );
			stanceSelect.addItem( "Backstab your opponent" );

			victorySelect = new JComboBox();
			victorySelect.addItem( "Steal a pretty flower" );
			victorySelect.addItem( "Fight for leaderboard rank" );

			if ( KoLCharacter.canInteract() )
				victorySelect.addItem( "Nab yourself some dignity" );

			VerifiableElement [] elements = new VerifiableElement[4];
			elements[0] = new VerifiableElement( "Fight Using: ", stanceSelect );
			elements[1] = new VerifiableElement( "PvP Objective: ", victorySelect );
			elements[2] = new VerifiableElement( "Win Message: ", winMessage );
			elements[3] = new VerifiableElement( "Loss Message: ", lossMessage );

			if ( KoLCharacter.getBaseMuscle() >= KoLCharacter.getBaseMysticality() && KoLCharacter.getBaseMuscle() >= KoLCharacter.getBaseMoxie() )
				stanceSelect.setSelectedIndex( 0 );
			else if ( KoLCharacter.getBaseMysticality() >= KoLCharacter.getBaseMuscle() && KoLCharacter.getBaseMysticality() >= KoLCharacter.getBaseMoxie() )
				stanceSelect.setSelectedIndex( 1 );
			else
				stanceSelect.setSelectedIndex( 2 );

			setContent( elements, null, getRankLabel(), true );
		}

		public void actionConfirmed()
		{
			ProfileRequest [] selection = getSelection();
			Arrays.sort( selection );

			String mission = null;
			switch ( victorySelect.getSelectedIndex() )
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
			FlowerHunterRequest request = new FlowerHunterRequest( "",
				stanceSelect.getSelectedIndex() + 1, mission, winMessage.getText(), lossMessage.getText() );

			for ( int i = 0; i < selection.length && !KoLmafia.refusesContinue(); ++i )
			{
				KoLmafia.updateDisplay( "Attacking " + selection[i].getPlayerName() + "..." );
				request.setTarget( selection[i].getPlayerName() );
				RequestThread.postRequest( request );

				updateRank();
			}

			KoLmafia.updateDisplay( "Attacks completed." );
			RequestThread.closeRequestSequence();
		}

		public void actionCancelled()
		{
			ProfileRequest [] selection = getSelection();
			Object [] parameters = new Object[1];

			for ( int i = 0; i < selection.length; ++i )
			{
				parameters[0] = selection[i];
				createDisplay( ProfileFrame.class, parameters );
			}
		}

		private ProfileRequest [] getSelection()
		{
			int index = isSimple ? 0 : 1;
			Vector selectionVector = new Vector();

			for ( int i = 0; i < results.length; ++i )
				if ( resultsTable[ index ].getSelectionModel().isSelectedIndex( i ) )
					selectionVector.add( results[ sortedModel[ index ].modelIndex( i ) ] );

			ProfileRequest [] selection = new ProfileRequest[ selectionVector.size() ];
			selectionVector.toArray( selection );
			return selection;
		}
	}

	private class SearchResultsTableModel extends DefaultTableModel
	{
		public SearchResultsTableModel( Object [] headers )
		{	super( headers, 0 );
		}

		public Class getColumnClass( int c )
		{	return getRowCount() == 0 || getValueAt( 0, c ) == null ? Object.class : getValueAt( 0, c ).getClass();
		}

		public boolean isCellEditable( int row, int col )
		{	return col == getColumnCount() - 1;
		}
	}
}
