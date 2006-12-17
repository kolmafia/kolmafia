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
import javax.swing.BoxLayout;

import java.awt.event.KeyEvent;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseAdapter;

import javax.swing.Box;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JComboBox;
import javax.swing.JTextField;
import javax.swing.JTable;
import javax.swing.JOptionPane;

import javax.swing.SwingUtilities;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableCellRenderer;

import net.java.dev.spellcast.utilities.LockableListModel;
import net.java.dev.spellcast.utilities.JComponentUtilities;

public class CakeArenaFrame extends KoLFrame
{
	private JTable familiarTable;
	private LockableListModel opponents;

	public CakeArenaFrame()
	{
		super( "Susie's Secret Bedroom!" );

		framePanel.setLayout( new BorderLayout() );
		framePanel.add( new CakeArenaPanel(), BorderLayout.CENTER );
		KoLCharacter.addCharacterListener( new KoLCharacterAdapter( new FamiliarRefresher() ) );
	}

	private class FamiliarRefresher implements Runnable
	{
		public void run()
		{
			if ( familiarTable != null )
				familiarTable.validate();
		}
	}

	private class CakeArenaPanel extends JPanel
	{
		private JComboBox opponentSelect;
		private JComboBox fightOptions;
		private JTextField battleField;

		public CakeArenaPanel()
		{
			super( new BorderLayout( 0, 10 ) );
			opponents = CakeArenaManager.getOpponentList();

			String opponentRace;
			String [] columnNames = { "Familiar", "Cage Match", "Scavenger Hunt", "Obstacle Course", "Hide and Seek" };

			// Register the data for your current familiar to be
			// rendered in the table.

			Object [][] familiarData = new Object[1][5];

			familiarTable = new JTable( familiarData, columnNames );
			familiarTable.setRowHeight( 40 );

			for ( int i = 0; i < 5; ++i )
			{
				familiarTable.setDefaultEditor( familiarTable.getColumnClass(i), null );
				familiarTable.setDefaultRenderer( familiarTable.getColumnClass(i), new OpponentRenderer() );
			}

			JPanel familiarPanel = new JPanel( new BorderLayout() );
			familiarPanel.add( familiarTable.getTableHeader(), BorderLayout.NORTH );
			familiarPanel.add( familiarTable, BorderLayout.CENTER );

			Object [][] opponentData = new Object[ opponents.size() ][5];

			// Register the data for your opponents to be rendered
			// in the table, taking into account the offset due to
			// your own familiar's data.

			for ( int i = 0; i < opponents.size(); ++i )
			{
				opponentRace = ((CakeArenaManager.ArenaOpponent)opponents.get(i)).getRace();
				opponentData[i][0] = opponents.get(i).toString();

				for ( int j = 1; j <= 4; ++j )
					opponentData[i][j] = new OpponentButton( i, j, FamiliarsDatabase.getFamiliarSkill( opponentRace, j ) );
			}

			JTable opponentTable = new JTable( opponentData, columnNames );
			opponentTable.addMouseListener( new ButtonEventListener( opponentTable ) );
			opponentTable.setRowHeight( 40 );

			for ( int i = 0; i < 5; ++i )
			{
				opponentTable.setDefaultEditor( opponentTable.getColumnClass(i), null );
				opponentTable.setDefaultRenderer( opponentTable.getColumnClass(i), new OpponentRenderer() );
			}

			JPanel opponentPanel = new JPanel( new BorderLayout() );
			opponentPanel.add( opponentTable.getTableHeader(), BorderLayout.NORTH );
			opponentPanel.add( opponentTable, BorderLayout.CENTER );

			add( familiarPanel, BorderLayout.NORTH );
			add( opponentPanel, BorderLayout.CENTER );
		}
	}

	private class OpponentButton extends NestedInsideTableButton implements MouseListener
	{
		private int row, column;
		private String opponentSkill;

		public OpponentButton( int row, int column, Integer skill )
		{
			super( JComponentUtilities.getImage( (skill == null ? "0" : skill.toString()) + "star.gif" ) );

			this.row = row;
			this.column = column;
			this.opponentSkill = skill.intValue() == 1 ? "1 star (opponent)" : skill + " stars (opponent)";
		}

		public void mouseReleased( MouseEvent e )
		{
			int yourSkillValue = FamiliarsDatabase.getFamiliarSkill( KoLCharacter.getFamiliar().getRace(), column ).intValue();
			String yourSkill = yourSkillValue == 1 ? "1 star (yours)" : yourSkillValue + " stars (yours)";

			int battleCount = StaticEntity.parseInt( JOptionPane.showInputDialog( "<html>" + opponents.get( row ).toString() + ", " +
				CakeArenaManager.getEvent( column ) + "<br>" + yourSkill + " vs. " + opponentSkill + "</html>" ) );

			if ( battleCount > 0 )
				CakeArenaManager.fightOpponent( opponents.get( row ).toString(), column, battleCount );
		}
	}

	private class OpponentRenderer implements TableCellRenderer
	{
		public Component getTableCellRendererComponent( JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column )
		{	return value == null ? getFamiliarComponent( column ) : getStandardComponent( value );
		}

		private Component getFamiliarComponent( int column )
		{
			FamiliarData currentFamiliar = KoLCharacter.getFamiliar();

			if ( column == 0 )
				return currentFamiliar == null ? getStandardComponent( "NO DATA (0 lbs)" ) :
					getStandardComponent( currentFamiliar.toString() );

			return currentFamiliar == null ? new JLabel( JComponentUtilities.getImage( "0star.gif" ) ) :
				new JLabel( JComponentUtilities.getImage( FamiliarsDatabase.getFamiliarSkill( currentFamiliar.getRace(), column ).toString() + "star.gif" ) );
		}

		private Component getStandardComponent( Object value )
		{
			if ( value instanceof OpponentButton )
				return (OpponentButton) value;

			String name = value.toString();

			JPanel component = new JPanel( new BorderLayout() );
			component.add( new JLabel( name.substring( 0, name.indexOf( "(" ) - 1 ), JLabel.CENTER ), BorderLayout.CENTER );
			component.add( new JLabel( name.substring( name.indexOf( "(" ) ), JLabel.CENTER ), BorderLayout.SOUTH );

			return component;
		}
	}
}
