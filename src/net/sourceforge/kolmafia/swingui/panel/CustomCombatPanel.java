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

package net.sourceforge.kolmafia.swingui.panel;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.PrintStream;

import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTree;
import javax.swing.tree.DefaultTreeModel;

import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.LogStream;
import net.sourceforge.kolmafia.StaticEntity;
import net.sourceforge.kolmafia.combat.CombatActionManager;
import net.sourceforge.kolmafia.preferences.PreferenceListener;
import net.sourceforge.kolmafia.preferences.PreferenceListenerRegistry;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.swingui.button.RelayBrowserButton;
import net.sourceforge.kolmafia.swingui.button.ThreadedButton;
import net.sourceforge.kolmafia.utilities.FileUtilities;
import net.sourceforge.kolmafia.utilities.InputFieldUtilities;
import net.sourceforge.kolmafia.webui.RelayLoader;

public class CustomCombatPanel
	extends JPanel
{
	protected JTree combatTree;
	protected JTextArea combatEditor;
	protected DefaultTreeModel combatModel;
	protected CardLayout combatCards;
	public JComboBox availableScripts;

	public CustomCombatPanel()
	{
		this.combatTree = new JTree();
		this.combatModel = (DefaultTreeModel) this.combatTree.getModel();

		this.combatCards = new CardLayout();

		this.setLayout( this.combatCards );
		this.availableScripts = new CombatComboBox();

		this.add( "tree", new CustomCombatTreePanel() );
		this.add( "editor", new CustomCombatEditorPanel() );

		this.updateFromPreferences();
	}

	public void updateFromPreferences()
	{
		CombatActionManager.updateFromPreferences();
		this.refreshCombatEditor();
	}

	public void refreshCombatEditor()
	{
		try
		{
			String script = (String) this.availableScripts.getSelectedItem();
			BufferedReader reader = FileUtilities.getReader( CombatActionManager.getStrategyLookupFile( script ) );

			if ( reader == null )
			{
				return;
			}

			StringBuffer buffer = new StringBuffer();
			String line;

			while ( ( line = reader.readLine() ) != null )
			{
				buffer.append( line );
				buffer.append( '\n' );
			}

			reader.close();
			reader = null;

			this.combatEditor.setText( buffer.toString() );
		}
		catch ( Exception e )
		{
			// This should not happen.  Therefore, print
			// a stack trace for debug purposes.

			StaticEntity.printStackTrace( e );
		}

		this.refreshCombatTree();
	}

	/**
	 * Internal class used to handle everything related to displaying custom combat.
	 */

	public void refreshCombatTree()
	{
		this.combatModel.setRoot( CombatActionManager.getStrategyLookup() );
		this.combatTree.setRootVisible( false );

		for ( int i = 0; i < this.combatTree.getRowCount(); ++i )
		{
			this.combatTree.expandRow( i );
		}
	}

	public class CombatComboBox
		extends JComboBox
		implements ActionListener, PreferenceListener
	{
		public CombatComboBox()
		{
			super( CombatActionManager.getAvailableLookups() );
			this.addActionListener( this );
			PreferenceListenerRegistry.registerListener( "customCombatScript", this );
		}

		public void update()
		{
			CustomCombatPanel.this.combatCards.show( CustomCombatPanel.this, "tree" );
			this.setSelectedItem( Preferences.getString( "customCombatScript" ) );
		}

		public void actionPerformed( final ActionEvent e )
		{
			String script = (String) this.getSelectedItem();
			if ( script != null )
			{
				CombatActionManager.loadStrategyLookup( script );
				CustomCombatPanel.this.refreshCombatTree();
			}
		}
	}

	private class CustomCombatEditorPanel
		extends ScrollablePanel
	{
		public CustomCombatEditorPanel()
		{
			super( "Editor", "save", "cancel", new JTextArea() );
			CustomCombatPanel.this.combatEditor = (JTextArea) this.scrollComponent;
			CustomCombatPanel.this.combatEditor.setFont( KoLConstants.DEFAULT_FONT );
			CustomCombatPanel.this.refreshCombatTree();

			this.eastPanel.add( new RelayBrowserButton( "Help", "http://kolmafia.sourceforge.net/combat.html" ), BorderLayout.SOUTH );
		}

		public void actionConfirmed()
		{
			String script = (String) CustomCombatPanel.this.availableScripts.getSelectedItem();
			String saveText = CustomCombatPanel.this.combatEditor.getText();

			File location = CombatActionManager.getStrategyLookupFile( script );
			PrintStream writer = LogStream.openStream( location, true );

			writer.print( saveText );
			writer.close();
			writer = null;

			KoLCharacter.battleSkillNames.setSelectedItem( "custom combat script" );
			Preferences.setString( "battleAction", "custom combat script" );

			// After storing all the data on disk, go ahead
			// and reload the data inside of the tree.

			CombatActionManager.loadStrategyLookup( script );
			CombatActionManager.saveStrategyLookup( script );

			CustomCombatPanel.this.refreshCombatTree();
			CustomCombatPanel.this.combatCards.show( CustomCombatPanel.this, "tree" );
		}

		public void actionCancelled()
		{
			CustomCombatPanel.this.refreshCombatEditor();
			CustomCombatPanel.this.combatCards.show( CustomCombatPanel.this, "tree" );
		}

		public void setEnabled( final boolean isEnabled )
		{
		}
	}

	public class CustomCombatTreePanel
		extends ScrollablePanel
	{
		public CustomCombatTreePanel()
		{
			super( "", "edit", "help", CustomCombatPanel.this.combatTree );
			CustomCombatPanel.this.combatTree.setVisibleRowCount( 8 );

			this.centerPanel.add( CustomCombatPanel.this.availableScripts, BorderLayout.NORTH );

			JPanel extraButtons = new JPanel( new GridLayout( 2, 1, 5, 5 ) );

			extraButtons.add( new ThreadedButton( "new", new NewScriptRunnable() ) );
			extraButtons.add( new ThreadedButton( "copy", new CopyScriptRunnable() ) );

			JPanel buttonHolder = new JPanel( new BorderLayout() );
			buttonHolder.add( extraButtons, BorderLayout.NORTH );

			this.eastPanel.add( buttonHolder, BorderLayout.SOUTH );
		}

		public void actionConfirmed()
		{
			CustomCombatPanel.this.refreshCombatEditor();
			CustomCombatPanel.this.combatCards.show( CustomCombatPanel.this, "editor" );
		}

		public void actionCancelled()
		{
			RelayLoader.openSystemBrowser( "http://kolmafia.sourceforge.net/combat.html" );
		}

		public void setEnabled( final boolean isEnabled )
		{
		}

		public class NewScriptRunnable
			implements Runnable
		{
			public void run()
			{
				String name = InputFieldUtilities.input( "Give your combat script a name!" );
				if ( name == null || name.equals( "" ) || name.equals( "default" ) )
				{
					return;
				}

				CombatActionManager.loadStrategyLookup( name );
				CustomCombatPanel.this.refreshCombatTree();
			}
		}

		public class CopyScriptRunnable
			implements Runnable
		{
			public void run()
			{
				String name = InputFieldUtilities.input( "Make a copy of current script called:" );
				if ( name == null || name.equals( "" ) || name.equals( "default" ) )
				{
					return;
				}

				CombatActionManager.copyStrategyLookup( name );
				CombatActionManager.loadStrategyLookup( name );
				CustomCombatPanel.this.refreshCombatTree();
			}
		}
	}
}
