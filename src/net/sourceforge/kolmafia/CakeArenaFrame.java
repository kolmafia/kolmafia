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

import java.awt.Component;
import java.awt.Dimension;
import java.awt.BorderLayout;
import java.awt.event.KeyEvent;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JComboBox;
import javax.swing.JTextField;
import javax.swing.JScrollPane;
import javax.swing.JEditorPane;
import javax.swing.JMenuBar;
import javax.swing.JMenu;
import javax.swing.JTable;
import javax.swing.JTabbedPane;

import javax.swing.table.TableCellRenderer;
import net.java.dev.spellcast.utilities.LockableListModel;
import net.java.dev.spellcast.utilities.JComponentUtilities;

public class CakeArenaFrame extends KoLPanelFrame
{
	private static final String NO_DATA = "NO DATA (0 lbs)";

	public CakeArenaFrame( KoLmafia client )
	{
		super( client, "KoLmafia: Susie's Secret Bedroom!" );

		if ( client != null && client.getCakeArenaManager().getOpponentList().isEmpty() )
			(new CakeArenaRequest( client )).run();

		setContentPanel( new CakeArenaPanel() );
		addMenuBar();
	}

	private void addMenuBar()
	{
		JMenuBar menuBar = new JMenuBar();
		this.setJMenuBar( menuBar );

		JMenu optionsMenu = new JMenu( "Options" );
		optionsMenu.setMnemonic( KeyEvent.VK_O );

		optionsMenu.add( new InvocationMenuItem( "Clear Results", KeyEvent.VK_C,
			client == null ? new LimitedSizeChatBuffer( "" ) : client.getCakeArenaManager().getResults(), "clearBuffer" ) );

		menuBar.add( optionsMenu );
	}

	private class CakeArenaPanel extends KoLPanel
	{
		private JComboBox opponentSelect;
		private JComboBox fightOptions;
		private JTextField battleField;

		public CakeArenaPanel()
		{
			super( "fight!", "stop!", new Dimension( 100, 20 ), new Dimension( 300, 20 ) );

			LockableListModel opponents = client == null ? new LockableListModel() : client.getCakeArenaManager().getOpponentList();
			opponentSelect = new JComboBox( opponents );

			fightOptions = new JComboBox();
			fightOptions.addItem( "Ultimate Cage Match" );
			fightOptions.addItem( "Scavenger Hunt" );
			fightOptions.addItem( "Obstacle Course" );
			fightOptions.addItem( "Hide and Seek" );

			battleField = new JTextField();

			VerifiableElement [] elements = new VerifiableElement[3];
			elements[0] = new VerifiableElement( "Opponent: ", opponentSelect );
			elements[1] = new VerifiableElement( "Event: ", fightOptions );
			elements[2] = new VerifiableElement( "Battles: ", battleField );

			setContent( elements );

			String [] columnNames = { "Familiar", "Ultimate Cage Match", "Scavenger Hunt", "Obstacle Course", "Hide and Seek" };
			Object [][] opponentData = new Object[ opponents.size() + 1 ][5];

			// Register the data for your current familiar to be
			// rendered in the table.

			FamiliarData currentFamiliar = client == null ? null :
				(FamiliarData) client.getCharacterData().getFamiliars().getSelectedItem();

			String opponentRace = currentFamiliar == null ? NO_DATA : currentFamiliar.getRace();

			opponentData[0][0] = opponentRace.equals( NO_DATA ) ? (Object) NO_DATA : (Object) currentFamiliar;
			for ( int j = 1; j <= 4; ++j )
				opponentData[0][j] = opponentRace.equals( NO_DATA ) ? JComponentUtilities.getSharedImage( "0star.gif" ) :
					JComponentUtilities.getSharedImage( FamiliarsDatabase.getFamiliarSkill( opponentRace, j ).toString() + "star.gif" );

			// Register the data for your opponents to be rendered
			// in the table, taking into account the offset due to
			// your own familiar's data.

			for ( int i = 0; i < opponents.size(); ++i )
			{
				opponentRace = ((CakeArenaManager.ArenaOpponent)opponents.get(i)).getRace();
				opponentData[i+1][0] = opponents.get(i).toString();

				for ( int j = 1; j <= 4; ++j )
					opponentData[i+1][j] = JComponentUtilities.getSharedImage(
						FamiliarsDatabase.getFamiliarSkill( opponentRace, j ).toString() + "star.gif" );
			}

			JTable table = new JTable( opponentData, columnNames );
			table.setRowHeight( 40 );

			for ( int i = 0; i < 5; ++i )
			{
				table.setDefaultEditor( table.getColumnClass(i), null );
				table.setDefaultRenderer( table.getColumnClass(i), new OpponentRenderer() );
			}

			JPanel tablePanel = new JPanel();
			tablePanel.setLayout( new BorderLayout() );
			tablePanel.add( table.getTableHeader(), BorderLayout.NORTH );
			tablePanel.add( table, BorderLayout.CENTER );

			JEditorPane resultsDisplay = new JEditorPane();
			resultsDisplay.setEditable( false );

			if ( client != null )
				client.getCakeArenaManager().getResults().setChatDisplay( resultsDisplay );

			JScrollPane scroller = new JScrollPane( resultsDisplay, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
				JScrollPane.HORIZONTAL_SCROLLBAR_NEVER );
			JComponentUtilities.setComponentSize( scroller, 480, 200 );

			JTabbedPane tabs = new JTabbedPane();
			tabs.addTab( "Event Scores", tablePanel );
			tabs.addTab( "Battle Results", scroller );

			add( tabs, BorderLayout.CENTER );
		}

		public void actionConfirmed()
		{
			Object opponent = opponentSelect.getSelectedItem();
			if ( opponent == null )
				return;

			int eventID = fightOptions.getSelectedIndex() + 1;
			if ( eventID == 0 )
				return;

			int battleCount = getValue( battleField );
			client.getCakeArenaManager().fightOpponent( opponent.toString(), eventID, battleCount );
		}

		public void actionCancelled()
		{
			updateDisplay( ERROR_STATE, "Arena battles terminated." );
			client.cancelRequest();
		}
	}

	private class OpponentRenderer implements TableCellRenderer
	{
		public Component getTableCellRendererComponent( JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column )
		{
			if ( value instanceof ImageIcon )
				return new JLabel( (ImageIcon) value );

			String name = value.toString();

			JPanel component = new JPanel();
			component.setLayout( new BorderLayout() );
			component.add( new JLabel( name.substring( 0, name.indexOf( "(" ) - 1 ), JLabel.CENTER ), BorderLayout.CENTER );
			component.add( new JLabel( name.substring( name.indexOf( "(" ) ), JLabel.CENTER ), BorderLayout.SOUTH );

			return component;
		}
	}

	public static void main( String [] args )
	{
		Object [] parameters = new Object[1];
		parameters[0] = null;

		(new CreateFrameRunnable( CakeArenaFrame.class, parameters )).run();
	}
}