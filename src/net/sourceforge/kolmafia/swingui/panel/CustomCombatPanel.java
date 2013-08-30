/**
 * Copyright (c) 2005-2013, KoLmafia development team
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
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import java.io.BufferedReader;
import java.io.File;
import java.io.PrintStream;

import java.util.Arrays;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JTextArea;
import javax.swing.JTree;

import javax.swing.tree.DefaultTreeModel;

import net.java.dev.spellcast.utilities.JComponentUtilities;

import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafiaCLI;
import net.sourceforge.kolmafia.StaticEntity;

import net.sourceforge.kolmafia.combat.CombatActionManager;

import net.sourceforge.kolmafia.preferences.PreferenceListener;
import net.sourceforge.kolmafia.preferences.PreferenceListenerRegistry;
import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.swingui.button.RelayBrowserButton;
import net.sourceforge.kolmafia.swingui.button.ThreadedButton;

import net.sourceforge.kolmafia.swingui.widget.AutoFilterComboBox;

import net.sourceforge.kolmafia.utilities.FileUtilities;
import net.sourceforge.kolmafia.utilities.InputFieldUtilities;
import net.sourceforge.kolmafia.utilities.LogStream;

import net.sourceforge.kolmafia.webui.RelayLoader;

public class CustomCombatPanel
	extends JPanel
{
	private JComboBox actionSelect;
	protected JTree combatTree;
	protected JTextArea combatEditor;
	protected DefaultTreeModel combatModel;

	protected JPanel combatCardPanel;
	protected CardLayout combatCards;
	public JComboBox availableScripts;

	private static ImageIcon stealImg, entangleImg;
	private static ImageIcon potionImg, olfactImg, puttyImg;
	private static ImageIcon antidoteImg, restoreImg, safeImg;
	static
	{
		CustomCombatPanel.stealImg = CustomCombatPanel.getImage( "knobsack.gif" );
		CustomCombatPanel.entangleImg = CustomCombatPanel.getImage( "entnoodles.gif" );
		CustomCombatPanel.potionImg = CustomCombatPanel.getImage( "exclam.gif" );
		CustomCombatPanel.olfactImg = CustomCombatPanel.getImage( "footprints.gif" );
		CustomCombatPanel.puttyImg = CustomCombatPanel.getImage( "sputtycopy.gif" );
		CustomCombatPanel.antidoteImg = CustomCombatPanel.getImage( "poisoncup.gif" );
		CustomCombatPanel.restoreImg = CustomCombatPanel.getImage( "mp.gif" );
		CustomCombatPanel.safeImg = CustomCombatPanel.getImage( "cast.gif" );
	}

	public CustomCombatPanel()
	{
		this.combatTree = new JTree();
		this.combatModel = (DefaultTreeModel) this.combatTree.getModel();

		this.combatCards = new CardLayout();
		this.combatCardPanel = new JPanel( this.combatCards );

		this.availableScripts = new CombatComboBox();

		this.combatCardPanel.add( "tree", new CustomCombatTreePanel() );
		this.combatCardPanel.add( "editor", new CustomCombatEditorPanel() );

		this.setLayout( new BorderLayout( 5, 5 ) );

		this.add( new SpecialActionsPanel(), BorderLayout.NORTH );
		this.add( this.combatCardPanel, BorderLayout.CENTER );

		this.updateFromPreferences();
	}

	public void updateFromPreferences()
	{
		if ( this.actionSelect != null )
		{
			String battleAction = Preferences.getString( "battleAction" );
			int battleIndex = KoLCharacter.getBattleSkillNames().indexOf( battleAction );
			KoLCharacter.getBattleSkillNames().setSelectedIndex( battleIndex == -1 ? 0 : battleIndex );
		}

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

	private static ImageIcon getImage( final String filename )
	{
		FileUtilities.downloadImage( "http://images.kingdomofloathing.com/itemimages/" + filename );
		return JComponentUtilities.getImage( "itemimages/" + filename );
	}

	private class SpecialActionsPanel
		extends GenericPanel
		implements PreferenceListener
	{
		private final JPanel special;
		private final JPopupMenu specialPopup;

		private final JLabel stealLabel, entangleLabel;
		private final JLabel potionLabel, olfactLabel, puttyLabel;
		private final JLabel antidoteLabel, restoreLabel, safeLabel;

		private final JCheckBoxMenuItem stealItem, entangleItem;
		private final JCheckBoxMenuItem potionItem, olfactItem, puttyItem;
		private final JCheckBoxMenuItem restoreItem, safePickpocket;
		private final JMenu poisonItem;
		private boolean updating = true;

		public SpecialActionsPanel()
		{
			super( new Dimension( 70, -1 ), new Dimension( 200, -1 ) );

			CustomCombatPanel.this.actionSelect = new AutoFilterComboBox( KoLCharacter.getBattleSkillNames(), false );
			CustomCombatPanel.this.actionSelect.addActionListener( new BattleActionListener() );

			JPanel special = new JPanel( new FlowLayout( FlowLayout.LEADING, 0, 0 ) );
			this.special = special;
			special.setBackground( Color.WHITE );
			special.setBorder( BorderFactory.createLoweredBevelBorder() );

			MouseListener listener = new SpecialPopListener();
			special.addMouseListener( listener );

			this.stealLabel =
				this.label(
					special, listener, CustomCombatPanel.stealImg,
					"Pickpocketing will be tried (if appropriate) with non-CCS actions." );
			this.entangleLabel =
				this.label(
					special, listener, CustomCombatPanel.entangleImg,
					"Entangling Noodles will be cast before non-CCS actions." );
			this.olfactLabel = this.label( special, listener, CustomCombatPanel.olfactImg, null );
			this.puttyLabel = this.label( special, listener, CustomCombatPanel.puttyImg, null );
			this.potionLabel =
				this.label(
					special,
					listener,
					CustomCombatPanel.potionImg,
					"<html>Dungeons of Doom potions will be identified by using them in combat.<br>Requires 'special' action if a CCS is used.</html>" );
			this.antidoteLabel = this.label( special, listener, CustomCombatPanel.antidoteImg, null );
			this.restoreLabel =
				this.label(
					special, listener, CustomCombatPanel.restoreImg, "MP restores will be used in combat if needed." );
			this.safeLabel =
				this.label(
					special, listener, CustomCombatPanel.safeImg,
					"Pickpocketing will be skipped when there are no useful results or it is too dangerous." );

			this.specialPopup = new JPopupMenu( "Special Actions" );
			this.stealItem = this.checkbox( this.specialPopup, listener, "Pickpocket before simple actions" );
			this.entangleItem = this.checkbox( this.specialPopup, listener, "Cast Noodles before simple actions" );
			this.specialPopup.addSeparator();

			this.olfactItem = this.checkbox( this.specialPopup, listener, "One-time automatic Olfaction..." );
			this.puttyItem =
				this.checkbox( this.specialPopup, listener, "One-time automatic Spooky Putty/Rain-Doh box/4-d camera..." );
			this.potionItem = this.checkbox( this.specialPopup, listener, "Identify bang potions" );
			this.specialPopup.addSeparator();

			this.poisonItem = new JMenu( "Minimum poison level for antidote use" );
			ButtonGroup group = new ButtonGroup();
			this.poison( this.poisonItem, group, listener, "No automatic use" );
			this.poison( this.poisonItem, group, listener, "Toad In The Hole (-\u00BDHP/round)" );
			this.poison( this.poisonItem, group, listener, "Majorly Poisoned (-90%, -11)" );
			this.poison( this.poisonItem, group, listener, "Really Quite Poisoned (-70%, -9)" );
			this.poison( this.poisonItem, group, listener, "Somewhat Poisoned (-50%, -7)" );
			this.poison( this.poisonItem, group, listener, "A Little Bit Poisoned (-30%, -5)" );
			this.poison( this.poisonItem, group, listener, "Hardly Poisoned at All (-10%, -3)" );
			this.specialPopup.add( this.poisonItem );
			this.restoreItem = this.checkbox( this.specialPopup, listener, "Restore MP in combat" );
			this.safePickpocket = this.checkbox( this.specialPopup, listener, "Skip pickpocketing when no useful results or too dangerous" );

			VerifiableElement[] elements = new VerifiableElement[ 2 ];
			elements[ 0 ] = new VerifiableElement( "Action:  ", CustomCombatPanel.this.actionSelect );
			elements[ 1 ] = new VerifiableElement( "Special:  ", special );

			this.setContent( elements );
			( (BorderLayout) this.container.getLayout() ).setHgap( 0 );
			( (BorderLayout) this.container.getLayout() ).setVgap( 0 );

			PreferenceListenerRegistry.registerListener( "autoSteal", this );
			PreferenceListenerRegistry.registerListener( "autoEntangle", this );
			PreferenceListenerRegistry.registerListener( "autoOlfact", this );
			PreferenceListenerRegistry.registerListener( "autoPutty", this );
			PreferenceListenerRegistry.registerListener( "autoPotionID", this );
			PreferenceListenerRegistry.registerListener( "autoAntidote", this );
			PreferenceListenerRegistry.registerListener( "autoManaRestore", this );
			PreferenceListenerRegistry.registerListener( "safePickpocket", this );
			PreferenceListenerRegistry.registerListener( "(skill)", this );

			this.update();
		}

		public void update()
		{
			this.updating = true;

			CustomCombatPanel.this.actionSelect.setSelectedItem( Preferences.getString( "battleAction" ) );

			if ( KoLCharacter.hasSkill( "Entangling Noodles" ) )
			{
				this.entangleItem.setEnabled( true );
			}
			else
			{
				this.entangleItem.setEnabled( false );
				Preferences.setBoolean( "autoEntangle", false );
			}

			String text;
			boolean pref;
			pref = Preferences.getBoolean( "autoSteal" );
			this.stealLabel.setVisible( pref );
			this.stealItem.setSelected( pref );
			pref = Preferences.getBoolean( "autoEntangle" );
			this.entangleLabel.setVisible( pref );
			this.entangleItem.setSelected( pref );
			text = Preferences.getString( "autoOlfact" );
			pref = text.length() > 0;
			this.olfactLabel.setVisible( pref );
			this.olfactItem.setSelected( pref );
			this.olfactLabel.setToolTipText( "<html>Automatic Olfaction or odor extractor use: " + text + "<br>Requires 'special' action if a CCS is used.</html>" );
			text = Preferences.getString( "autoPutty" );
			pref = text.length() > 0;
			this.puttyLabel.setVisible( pref );
			this.puttyItem.setSelected( pref );
			this.puttyLabel.setToolTipText( "<html>Automatic Spooky Putty sheet, Rain-Doh black box, 4-d camera or portable photocopier use: " + text + "<br>Requires 'special' action if a CCS is used.</html>" );
			pref = Preferences.getBoolean( "autoPotionID" );
			this.potionLabel.setVisible( pref );
			this.potionItem.setSelected( pref );
			int antidote = Preferences.getInteger( "autoAntidote" );
			this.antidoteLabel.setVisible( antidote > 0 );
			if ( antidote >= 0 && antidote < this.poisonItem.getMenuComponentCount() )
			{
				JRadioButtonMenuItem option = (JRadioButtonMenuItem) this.poisonItem.getMenuComponent( antidote );
				option.setSelected( true );
				this.antidoteLabel.setToolTipText( "Anti-anti-antidote will be used in combat if you get " + option.getText() + " or worse." );
			}
			pref = Preferences.getBoolean( "autoManaRestore" );
			this.restoreLabel.setVisible( pref );
			this.restoreItem.setSelected( pref );
			pref = Preferences.getBoolean( "safePickpocket" );
			this.safeLabel.setVisible( pref );
			this.safePickpocket.setSelected( pref );

			this.updating = false;
		}

		private JLabel label( final JPanel special, final MouseListener listener, final ImageIcon img,
			final String toolTip )
		{
			JLabel rv = new JLabel( img );
			rv.setToolTipText( toolTip );
			rv.addMouseListener( listener );
			special.add( rv );
			return rv;
		}

		private JCheckBoxMenuItem checkbox( final JPopupMenu menu, final Object listener, final String text )
		{
			JCheckBoxMenuItem rv = new JCheckBoxMenuItem( text );
			menu.add( rv );
			rv.addItemListener( (ItemListener) listener );
			return rv;
		}

		private void poison( final JMenu menu, final ButtonGroup group, final Object listener, final String text )
		{
			JRadioButtonMenuItem rb = new JRadioButtonMenuItem( text );
			menu.add( rb );
			group.add( rb );
			rb.addItemListener( (ItemListener) listener );
		}

		@Override
		public void actionConfirmed()
		{
		}

		@Override
		public void actionCancelled()
		{
		}

		@Override
		public void addStatusLabel()
		{
		}

		private class BattleActionListener
			implements ActionListener
		{
			public void actionPerformed( ActionEvent e )
			{
				// Don't set preferences from widgets when we
				// are in the middle of loading widgets from
				// preferences.
				if ( SpecialActionsPanel.this.updating )
				{
					return;
				}

				String value = (String) CustomCombatPanel.this.actionSelect.getSelectedItem();

				if ( value != null )
				{
					Preferences.setString( "battleAction", value );
				}
			}
		}

		private class SpecialPopListener
			extends MouseAdapter
			implements ItemListener
		{
			@Override
			public void mousePressed( final MouseEvent e )
			{
				SpecialActionsPanel.this.specialPopup.show( SpecialActionsPanel.this.special, 0, 32 );
			}

			public void itemStateChanged( final ItemEvent e )
			{
				// Don't set preferences from widgets when we
				// are in the middle of loading widgets from
				// preferences.
				if ( SpecialActionsPanel.this.updating )
				{
					return;
				}

				boolean state = e.getStateChange() == ItemEvent.SELECTED;
				JMenuItem source = (JMenuItem) e.getItemSelectable();
				if ( source == SpecialActionsPanel.this.stealItem )
				{
					Preferences.setBoolean( "autoSteal", state );
				}
				else if ( source == SpecialActionsPanel.this.entangleItem )
				{
					Preferences.setBoolean( "autoEntangle", state );
				}
				else if ( source == SpecialActionsPanel.this.olfactItem )
				{
					if ( state == !Preferences.getString( "autoOlfact" ).equals( "" ) )
					{ // pref already set externally, don't prompt
						return;
					}
					String option =
						!state ? null : InputFieldUtilities.input(
							"Use Transcendent Olfaction or odor extractor when? (item, \"goals\", or \"monster\" plus name; add \"abort\" to stop adventuring)",
							"goals" );

					KoLmafiaCLI.DEFAULT_SHELL.executeCommand( "olfact", option == null ? "none" : option );
				}
				else if ( source == SpecialActionsPanel.this.puttyItem )
				{
					if ( state == !Preferences.getString( "autoPutty" ).equals( "" ) )
					{ // pref already set externally, don't prompt
						return;
					}
					String option =
						!state ? null : InputFieldUtilities.input(
							"Use Spooky Putty sheet, Rain-Doh black box, 4-d camera or portable photocopier when? (item, \"goals\", or \"monster\" plus name; add \"abort\" to stop adventuring)",
							"goals abort" );

					KoLmafiaCLI.DEFAULT_SHELL.executeCommand( "putty", option == null ? "none" : option );
				}
				else if ( source == SpecialActionsPanel.this.potionItem )
				{
					Preferences.setBoolean( "autoPotionID", state );
				}
				else if ( source == SpecialActionsPanel.this.restoreItem )
				{
					Preferences.setBoolean( "autoManaRestore", state );
				}
				else if ( source == SpecialActionsPanel.this.safePickpocket )
				{
					Preferences.setBoolean( "safePickpocket" , state );
				}
				else if ( source instanceof JRadioButtonMenuItem )
				{
					Preferences.setInteger( "autoAntidote", Arrays.asList(
						SpecialActionsPanel.this.poisonItem.getMenuComponents() ).indexOf( source ) );
				}
			}
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
			CustomCombatPanel.this.combatCards.show( CustomCombatPanel.this.combatCardPanel, "tree" );
			this.setSelectedItem( Preferences.getString( "customCombatScript" ) );
		}

		@Override
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

			this.eastPanel.add( new RelayBrowserButton( "help", "http://kolmafia.sourceforge.net/combat.html" ), BorderLayout.SOUTH );
		}

		@Override
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
			CustomCombatPanel.this.combatCards.show( CustomCombatPanel.this.combatCardPanel, "tree" );
		}

		@Override
		public void actionCancelled()
		{
			CustomCombatPanel.this.refreshCombatEditor();
			CustomCombatPanel.this.combatCards.show( CustomCombatPanel.this.combatCardPanel, "tree" );
		}

		@Override
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

		@Override
		public void actionConfirmed()
		{
			CustomCombatPanel.this.refreshCombatEditor();
			CustomCombatPanel.this.combatCards.show( CustomCombatPanel.this.combatCardPanel, "editor" );
		}

		@Override
		public void actionCancelled()
		{
			RelayLoader.openSystemBrowser( "http://kolmafia.sourceforge.net/combat.html" );
		}

		@Override
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
