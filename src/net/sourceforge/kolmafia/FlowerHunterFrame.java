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

import javax.swing.JEditorPane;
import javax.swing.JPanel;
import javax.swing.JComboBox;
import javax.swing.JToolBar;
import javax.swing.JButton;
import javax.swing.JTable;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.JScrollPane;

import java.awt.Dimension;
import java.awt.Component;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.GridLayout;

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import javax.swing.ToolTipManager;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.DefaultTableCellRenderer;

import java.util.Vector;
import com.sun.java.forums.TableSorter;
import net.java.dev.spellcast.utilities.JComponentUtilities;

public class FlowerHunterFrame extends KoLFrame implements ListSelectionListener
{
	private boolean isSimple;
	private JTabbedPane tabs;
	private CardLayout resultCards;
	private JPanel resultCardPanel;

	private JTable [] resultsTable = new JTable[2];
	private TableSorter [] sortedModel = new TableSorter[2];
	private DefaultTableModel [] resultsModel = new DefaultTableModel[2];

	private ProfileRequest [] results;

	public FlowerHunterFrame( KoLmafia client )
	{
		super( client, "Hardcore Flower Hunter" );

		tabs = new JTabbedPane();
		tabs.add( "Search", new SearchPanel() );
		tabs.add( "Attack", new AttackPanel() );

		framePanel.setLayout( new BorderLayout() );
		framePanel.add( tabs, BorderLayout.NORTH );

		results = new ProfileRequest[0];

		resultsModel[0] = new SearchResultsTableModel( new String [] { "Name", "Clan", "Class", "Level", "Rank" } );
		resultsTable[0] = new JTable( resultsModel[0] );
		sortedModel[0] = new TableSorter( resultsTable[0].getModel(), resultsTable[0].getTableHeader() );
		resultsTable[0].setModel( sortedModel[0] );
		resultsTable[0].getSelectionModel().addListSelectionListener( this );

		resultsModel[1] = new SearchResultsTableModel( new String [] { "Name", "Class", "Level", "Drink", "Fashion", "Login" } );
		resultsTable[1] = new JTable( resultsModel[1] );
		sortedModel[1] = new TableSorter( resultsTable[1].getModel(), resultsTable[1].getTableHeader() );
		resultsTable[1].setModel( sortedModel[1] );
		resultsTable[1].getSelectionModel().addListSelectionListener( this );

		JScrollPane [] resultsScroller = new JScrollPane[2];
		resultsScroller[0] = new JScrollPane( resultsTable[0], JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER );
		resultsScroller[1] = new JScrollPane( resultsTable[1], JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER );

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

	public void valueChanged( ListSelectionEvent e )
	{	tabs.setSelectedIndex( resultsTable[ isSimple ? 0 : 1 ].getSelectionModel().isSelectionEmpty() ? 0 : 1 );
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
			elements[0] = new VerifiableElement( "Level: ", levelEntry );
			elements[1] = new VerifiableElement( "Rank: ", rankEntry );
			elements[2] = new VerifiableElement( "Limit: ", limitEntry );

			setContent( elements, null, null, true, true );
			setDefaultButton( confirmedButton );
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

			client.resetContinueState();
			client.updateDisplay( DISABLE_STATE, "Conducting search..." );

			while ( !resultsModel[ index ].getDataVector().isEmpty() )
			{
				resultsModel[ index ].removeRow( 0 );
				resultsModel[ index ].fireTableRowsDeleted( 0, 0 );
			}

			FlowerHunterRequest search = new FlowerHunterRequest( client, levelEntry.getText(), rankEntry.getText() );
			search.run();

			results = new ProfileRequest[ search.getSearchResults().size() ];
			search.getSearchResults().toArray( results );

			for ( int i = 0; i < resultLimit && i < results.length && client.permitsContinue(); ++i )
			{
				resultsModel[ index ].addRow( getRow( results[i], isSimple ) );
				resultsModel[ index ].fireTableRowsInserted( i - 1, i - 1 );
			}

			if ( client.permitsContinue() )
				client.updateDisplay( ENABLE_STATE, "Search completed." );
			else
				client.updateDisplay( ERROR_STATE, "Search halted." );
		}

		public Object [] getRow( ProfileRequest result, boolean isSimple )
		{
			if ( isSimple )
				return new Object [] { result.getPlayerName(), result.getClanName(), result.getClassType(),
					result.getPlayerLevel(), result.getPvpRank() };

			return new Object [] { result.getPlayerName(), result.getClassType(), result.getPlayerLevel(),
				result.getDrink(), result.getEquipmentPower(), result.getLastLoginAsString() };
		}
	}

	private class AttackPanel extends KoLPanel
	{
		private JTextField message;
		private JComboBox stanceSelect;
		private JComboBox victorySelect;

		public AttackPanel()
		{
			super( "attack", "profile" );

			message = new JTextField();

			stanceSelect = new JComboBox();
			stanceSelect.addItem( "Bully your opponent" );
			stanceSelect.addItem( "Burninate your opponent" );
			stanceSelect.addItem( "Backstab your opponent" );

			victorySelect = new JComboBox();
			victorySelect.addItem( "Steal a pretty flower" );
			victorySelect.addItem( "Fight for leaderboard rank" );

			VerifiableElement [] elements = new VerifiableElement[3];
			elements[0] = new VerifiableElement( "Tactic: ", stanceSelect );
			elements[1] = new VerifiableElement( "Mission: ", victorySelect );
			elements[2] = new VerifiableElement( "Message: ", message );

			setContent( elements );

			if ( KoLCharacter.getClassType().startsWith( "Se" ) || KoLCharacter.getClassType().startsWith( "Tu" ) )
				stanceSelect.setSelectedIndex( 0 );
			else if ( KoLCharacter.getClassType().startsWith( "Sa" ) || KoLCharacter.getClassType().startsWith( "Pa" ) )
				stanceSelect.setSelectedIndex( 1 );
			else
				stanceSelect.setSelectedIndex( 2 );
		}

		public void actionConfirmed()
		{
			ProfileRequest [] selection = getSelection();

			for ( int i = 0; i < selection.length; ++i )
			{
				FightFrame.showRequest( new FlowerHunterRequest( client, selection[i].getPlayerID(),
					stanceSelect.getSelectedIndex() + 1, victorySelect.getSelectedIndex() == 0, message.getText() ) );
				
				KoLRequest.delay( 10000 );
			}
		}

		public void actionCancelled()
		{
			ProfileRequest [] selection = getSelection();

			for ( int i = 0; i < selection.length; ++i )
			{
				Object [] parameters = new Object[2];
				parameters[0] = client;
				parameters[1] = selection[i];

				(new CreateFrameRunnable( ProfileFrame.class, parameters )).run();
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

	public static void main( String [] args )
	{	(new CreateFrameRunnable( FlowerHunterFrame.class )).run();
	}
}