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

import java.awt.Component;
import java.awt.BorderLayout;
import java.awt.CardLayout;

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

public class FlowerHunterFrame extends KoLFrame
{
	private JComboBox stanceSelect;
	private JComboBox victorySelect;

	public FlowerHunterFrame( KoLmafia client )
	{
		super( client, "Hardcore Flower Hunter" );

		JTabbedPane tabs = new JTabbedPane();

		tabs.addTab( "Simple Search", new SimpleFlowerHunterPanel() );
		tabs.addTab( "Detailed Search", new DetailFlowerHunterPanel() );

		framePanel.setLayout( new CardLayout( 10, 10 ) );
		framePanel.add( tabs, "" );

		stanceSelect = new JComboBox();
		stanceSelect.addItem( "Bully your opponent" );
		stanceSelect.addItem( "Burninate your opponent" );
		stanceSelect.addItem( "Backstab your opponent" );

		victorySelect = new JComboBox();
		victorySelect.addItem( "Attack for flowers" );
		victorySelect.addItem( "Attack for rank" );

		toolbarPanel.add( stanceSelect );
		toolbarPanel.add( new JToolBar.Separator() );
		toolbarPanel.add( victorySelect );

		if ( KoLCharacter.getClassType().startsWith( "Se" ) || KoLCharacter.getClassType().startsWith( "Tu" ) )
			stanceSelect.setSelectedIndex( 0 );
		else if ( KoLCharacter.getClassType().startsWith( "Sa" ) || KoLCharacter.getClassType().startsWith( "Pa" ) )
			stanceSelect.setSelectedIndex( 1 );
		else
			stanceSelect.setSelectedIndex( 2 );
	}

	private abstract class FlowerHunterPanel extends KoLPanel implements ListSelectionListener
	{
		private JTextField nameEntry;
		private JTextField levelEntry;
		private JTextField rankEntry;

		private JEditorPane preview;
		private JTable resultsTable;
		private TableSorter sortedModel;
		private DefaultTableModel resultsModel;
		private ProfileRequest [] results;

		public FlowerHunterPanel()
		{
			super( "search", "stop" );
			VerifiableElement [] elements = new VerifiableElement[2];
			elements[0] = new VerifiableElement( "Exact Level: ", levelEntry = new JTextField() );
			elements[1] = new VerifiableElement( "Maximum Rank: ", rankEntry = new JTextField() );

			setContent( elements );

			results = new ProfileRequest[0];
			resultsModel = new SearchResultsTableModel( getHeaders() );
			resultsTable = new JTable( resultsModel );
			sortedModel = new TableSorter( resultsTable.getModel(), resultsTable.getTableHeader() );

			resultsTable.setModel( resultsModel );
			resultsTable.getSelectionModel().addListSelectionListener( this );

			JScrollPane resultsScroller = new JScrollPane( resultsTable, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
				JScrollPane.HORIZONTAL_SCROLLBAR_NEVER );

			preview = new JEditorPane();
			preview.setContentType( "text/html" );
			preview.setEditable( false );

			JScrollPane previewScroller = new JScrollPane( preview, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
				JScrollPane.HORIZONTAL_SCROLLBAR_NEVER );

			JComponentUtilities.setComponentSize( resultsScroller, 450, 120 );
			JComponentUtilities.setComponentSize( previewScroller, 450, 300 );

			JPanel southPanel = new JPanel( new BorderLayout() );
			southPanel.add( resultsScroller, BorderLayout.NORTH );
			southPanel.add( previewScroller, BorderLayout.CENTER );

			add( southPanel, BorderLayout.CENTER );
			ToolTipManager.sharedInstance().unregisterComponent( resultsTable );
			setDefaultButton( confirmedButton );
		}

		public void actionConfirmed()
		{
			client.resetContinueState();
			client.updateDisplay( DISABLE_STATE, "Conducting search..." );

			while ( !resultsModel.getDataVector().isEmpty() )
				resultsModel.removeRow( 0 );

			FlowerHunterRequest search = new FlowerHunterRequest( client, levelEntry.getText(), rankEntry.getText() );
			search.run();

			results = new ProfileRequest[ search.getSearchResults().size() ];
			search.getSearchResults().toArray( results );

			for ( int i = 0; i < results.length && client.permitsContinue(); ++i )
				resultsModel.addRow( getRow( results[i] ) );

			if ( client.permitsContinue() )
				client.updateDisplay( ENABLE_STATE, "Search completed." );
			else
				client.updateDisplay( ERROR_STATE, "Search halted." );
		}

		public void actionCancelled()
		{	client.cancelRequest();
		}

		public void valueChanged( ListSelectionEvent e )
		{
			if ( e != null && e.getValueIsAdjusting() )
				return;

			int row = resultsTable.getSelectionModel().getMinSelectionIndex();
			if ( row >= 0 )
			{
				row = sortedModel.modelIndex( row );

				preview.setText( "Loading..." );
				results[ row ].initialize();
				preview.setText( RequestEditorKit.getDisplayHTML( results[ row ].responseText ) );
				preview.setCaretPosition(0);
			}
		}

		protected abstract String [] getHeaders();
		protected abstract Object [] getRow( ProfileRequest result );
	}

	private class SimpleFlowerHunterPanel extends FlowerHunterPanel
	{
		protected String [] getHeaders()
		{	return new String [] { "Name", "Clan", "Class", "Level", "Rank" };
		}

		protected Object [] getRow( ProfileRequest result )
		{
			return new Object [] { result.getPlayerName(), result.getClanName(), result.getClassType(),
				result.getPlayerLevel(), result.getPvpRank() };
		}
	}

	private class DetailFlowerHunterPanel extends FlowerHunterPanel
	{
		protected String [] getHeaders()
		{	return new String [] { "Name", "Class", "Level", "Drink", "Fashion", "Login" };
		}


		protected Object [] getRow( ProfileRequest result )
		{
			KoLRequest.delay();
			return new Object [] { result.getPlayerName(), result.getClassType(), result.getPlayerLevel(),
				result.getDrink(), result.getEquipmentPower(), result.getLastLoginAsString() };
		}
	}

	private class SearchResultsTableModel extends DefaultTableModel
	{
		public SearchResultsTableModel( Object [] headers )
		{	super( headers, 0 );
		}

		public Class getColumnClass( int c )
		{	return getValueAt( 0, c ).getClass();
		}

		public boolean isCellEditable( int row, int col )
		{	return col == getColumnCount() - 1;
		}
	}

	public static void main( String [] args )
	{	(new CreateFrameRunnable( FlowerHunterFrame.class )).run();
	}
}