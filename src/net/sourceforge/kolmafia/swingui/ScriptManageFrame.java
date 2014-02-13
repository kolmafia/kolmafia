/**
 * Copyright (c) 2005-2014, KoLmafia development team
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
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JTextPane;
import javax.swing.ListSelectionModel;
import javax.swing.ToolTipManager;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.text.JTextComponent;

import org.jdesktop.swingx.JXTable;
import org.jdesktop.swingx.decorator.HighlighterFactory;

import net.java.dev.spellcast.utilities.JComponentUtilities;
import net.sourceforge.kolmafia.persistence.Script;
import net.sourceforge.kolmafia.persistence.ScriptManager;
import net.sourceforge.kolmafia.swingui.menu.ThreadedMenuItem;
import net.sourceforge.kolmafia.swingui.panel.GenericPanel;
import net.sourceforge.kolmafia.swingui.widget.AutoFilterTextField;
import net.sourceforge.kolmafia.swingui.widget.GenericScrollPane;
import net.sourceforge.kolmafia.swingui.widget.ShowDescriptionTable;

public class ScriptManageFrame
	extends GenericPanelFrame
{
	private class ScriptManageTable
		extends ShowDescriptionTable
	{
		public ScriptManageTable()
		{
			super( ScriptManager.getInstalledScripts(), 4, 4 );

			ScriptManageFrame.doColumnSetup( this );
			ScriptManageFrame.doHighlighterSetup( this );

			this.contextMenu.removeAll();

			ThreadedMenuItem t = new ThreadedMenuItem( "Delete script", new DeleteScriptRunnable( this ) );
			t.setIcon( JComponentUtilities.getImage( "xred.gif" ) );
			this.contextMenu.add( t );
			this.contextMenu.add( new JSeparator() );
			t = new ThreadedMenuItem( "Open forum thread", new ShowThreadRunnable( this ) );
			t.setIcon( JComponentUtilities.getImage( "home.gif" ) );
			this.contextMenu.add( t );
			t = new ThreadedMenuItem( "Refresh script list", new RefreshScriptsRunnable() );
			t.setIcon( JComponentUtilities.getImage( "reload.gif" ) );
			this.contextMenu.add( t );
			this.contextMenu.add( new ThreadedMenuItem( "Update this script", new UpdateScriptRunnable( this, false ) ) );
			this.contextMenu.add( new ThreadedMenuItem( "Update all scripts", new UpdateScriptRunnable( this, true ) ) );

		}
	}

	private class RepoManageTable
		extends ShowDescriptionTable
	{
		public RepoManageTable()
		{
			super( ScriptManager.getRepoScripts(), 4, 4 );

			ScriptManageFrame.doColumnSetup( this );
			ScriptManageFrame.doHighlighterSetup( this );

			this.contextMenu.removeAll();

			ThreadedMenuItem t = new ThreadedMenuItem( "Install script", new InstallScriptRunnable( this ) );
			t.setIcon( JComponentUtilities.getImage( "icon_plus.gif" ) );
			this.contextMenu.add( t );

			this.contextMenu.add( new JSeparator() );

			t = new ThreadedMenuItem( "Open forum thread", new ShowThreadRunnable( this ) );
			t.setIcon( JComponentUtilities.getImage( "home.gif" ) );
			this.contextMenu.add( t );
			t = new ThreadedMenuItem( "Reload remote repository", new ReloadRepoRunnable() );
			t.setIcon( JComponentUtilities.getImage( "reload.gif" ) );
			this.contextMenu.add( t );
		}
	}

	private final ScriptManageTable scriptTable = new ScriptManageTable();
	private RepoManageTable repoTable = new RepoManageTable();

	public ScriptManageFrame()
	{
		super( "Script Manager" );

		this.tabs.add( "Manage", new ManageScriptsPanel() );
		this.tabs.add( "Install", new InstallScriptsPanel() );

		this.setCenterComponent( this.tabs );
	}

	private class InstallScriptsPanel
		extends GenericPanel
	{
		private final JTextPane textPane;

		public InstallScriptsPanel()
		{
			super( new Dimension( 1, 1 ), null, true );
			GenericScrollPane manageScroller = new GenericScrollPane( ScriptManageFrame.this.repoTable );

			JPanel top = layoutTopPanel();

			JPanel managePanel = new JPanel( new BorderLayout() );
			managePanel.add( manageScroller, BorderLayout.CENTER );
			JComponentUtilities.setComponentSize( managePanel, 640, 380 );

			this.textPane = new JTextPane();
			textPane.setContentType( "text/html" );
			textPane.setText( "<html>Select a script for more details</html>" );
			textPane.setBorder( BorderFactory.createEtchedBorder() );
			textPane.setEditable( false );
			textPane.setPreferredSize( new Dimension( getPreferredSize().width, 150 ) );

			ScriptManageFrame.this.repoTable.getSelectionModel().addListSelectionListener( new LongDescriptionListener(
				ScriptManageFrame.this.repoTable, textPane ) );

			this.setContent( this.elements, true );
			this.container.remove( this.eastContainer );
			this.container.add( top, BorderLayout.NORTH );
			this.container.add( managePanel, BorderLayout.CENTER );
			this.container.add( textPane, BorderLayout.SOUTH );
		}

		private JPanel layoutTopPanel()
		{
			JPanel top = new JPanel( new BorderLayout() );
			JPanel topInnerLeft = new JPanel( new FlowLayout(FlowLayout.LEFT) );
			top.add( topInnerLeft, BorderLayout.WEST );
			JPanel topInnerRight = new JPanel( new BorderLayout() );
			top.add( topInnerRight, BorderLayout.EAST );
	
			JLabel baseLabel = new JLabel( "<html>Install new scripts from SVN here. </html>" );
			topInnerLeft.add( baseLabel );

			JLabel helpLabel = new JLabel( "<html><u>Hover for more info.</u></html>" );
			topInnerLeft.add( helpLabel );
			
			helpLabel.setForeground( Color.blue.darker() );
			helpLabel.setCursor( new Cursor( Cursor.HAND_CURSOR ) );

			top.add( Box.createVerticalStrut( 25 ), BorderLayout.CENTER );

			JLabel label = new JLabel( "Search: ");
			topInnerRight.add( label, BorderLayout.WEST );
			JComponent filter = new AutoFilterTextField( ScriptManageFrame.this.repoTable.getDisplayModel() );
			filter.setPreferredSize( new Dimension( 150, filter.getPreferredSize().height ) );
			topInnerRight.add( filter, BorderLayout.CENTER );

			String tooltip = "<html>On this panel you can install community-created scripts.<br>"
				+ "Scripts so installed will automatically receive available updates when you perform the \"svn update\" command<br>"
				+ "(which can be set to automatically run on login - look in General > Preferences > SVN).<br><br>"
				+ "Notable things you can do here:<br>"
				+ "<ul><li>Click a header column to sort ascending/descending</li>"
				+ "<li>Left-click a script to see more details on the bottom panel</li>"
				+ "<li>Right-click to install (in future: more options)</li></ul></html>";
			helpLabel.setToolTipText( tooltip );

			// remove delay and fade from tooltip
			ToolTipManager.sharedInstance().registerComponent( helpLabel );
			ToolTipManager.sharedInstance().setInitialDelay( 0 );
			ToolTipManager.sharedInstance().setDismissDelay( Integer.MAX_VALUE );
			return top;
		}

		@Override
		public void actionConfirmed()
		{

		}

		@Override
		public void actionCancelled()
		{

		}
	}

	private class LongDescriptionListener
		implements ListSelectionListener
	{
		private JTextComponent comp;
		private JXTable table;

		public LongDescriptionListener( JXTable table, JTextComponent comp )
		{
			this.table = table;
			this.comp = comp;
		}

		public void valueChanged( ListSelectionEvent e )
		{
			int row = table.getSelectedRow();

			if ( row < 0 ) // this can happen during sorting, seems buggy to me...
				return;

			Object ob = table.getValueAt( row, 0 );

			if ( ob instanceof Script )
			{
				comp.setText( ( (Script) ob ).getLongDesc() );
			}
		}
	}

	private class ManageScriptsPanel
		extends GenericPanel
	{
		public ManageScriptsPanel()
		{
			super( new Dimension( 1, 1 ), null, true );
			GenericScrollPane manageScroller = new GenericScrollPane( ScriptManageFrame.this.scriptTable );

			JPanel top = new JPanel( new BorderLayout() );
			JLabel baseLabel = new JLabel(
				"<html>Manage current SVN-installed scripts.  Right click for associated options." );
			top.add( baseLabel, BorderLayout.WEST );
			top.add( Box.createVerticalStrut( 25 ) );

			JPanel managePanel = new JPanel( new BorderLayout() );
			managePanel.add( manageScroller, BorderLayout.CENTER );
			JComponentUtilities.setComponentSize( managePanel, 500, 400 );

			this.setContent( this.elements, true );
			this.container.remove( this.eastContainer );
			this.container.add( top, BorderLayout.NORTH );
			this.container.add( managePanel, BorderLayout.CENTER );
		}

		@Override
		public void actionConfirmed()
		{
			ScriptManager.updateInstalledScripts();
		}

		@Override
		public void actionCancelled()
		{

		}
	}

	private static void doColumnSetup( ShowDescriptionTable table )
	{
		table.setSelectionMode( ListSelectionModel.SINGLE_SELECTION );

		//some magic numbers, enjoy
		table.getColumnModel().getColumn( 0 ).setPreferredWidth( 150 );
		table.getColumnModel().getColumn( 1 ).setPreferredWidth( 100 );
		table.getColumnModel().getColumn( 2 ).setPreferredWidth( 250 );
		table.getColumnModel().getColumn( 3 ).setPreferredWidth( 80 );

		//no reordering of columns, that's just silly
		table.getTableHeader().setReorderingAllowed( false );
	}

	public static void doHighlighterSetup( JXTable t )
	{
		// light gray/blue
		t.addHighlighter( HighlighterFactory.createSimpleStriping( new Color( 230, 240, 240 ) ) );
	}
}
