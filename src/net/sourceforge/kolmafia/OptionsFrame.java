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

// layout
import java.awt.Dimension;
import java.awt.CardLayout;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import javax.swing.BoxLayout;

// events
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import javax.swing.ListSelectionModel;

// containers
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.JComboBox;
import javax.swing.JCheckBox;
import javax.swing.JTabbedPane;
import javax.swing.JOptionPane;
import javax.swing.JButton;

import net.java.dev.spellcast.utilities.ActionPanel;
import net.java.dev.spellcast.utilities.LockableListModel;
import net.java.dev.spellcast.utilities.JComponentUtilities;

/**
 * <p>Handles all of the customizable user options in <code>KoLmafia</code>.
 * This class presents all of the options that the user can customize
 * in their adventuring and uses the appropriate <code>KoLSettings</code>
 * in order to display them.  This class also uses <code>KoLSettings</code>
 * to record the user's preferences for upcoming sessions.</p>
 *
 * <p>If this class is accessed before login, it will modify global settings
 * ONLY, and if the character already has settings, any modification of
 * global settings will not modify their own.  Accessing this class after
 * login will result in modification of the character's own settings ONLY,
 * and will not modify any global settings.</p>
 *
 * <p>Proxy settings are a special exception to this rule - because the
 * Java Virtual Machine requires the proxy settings to be specified at
 * a global level, though the settings are changed appropriately on disk,
 * only the most recently loaded settings will be active on the current
 * instance of the JVM.  If separate characters need separate proxies,
 * they cannot be run in the same JVM instance.</p>
 */

public class OptionsFrame extends KoLFrame
{
	/**
	 * Constructs a new <code>OptionsFrame</code> that will be
	 * associated with the given client.  When this frame is
	 * closed, it will attempt to return focus to the currently
	 * active frame; note that if this is done while the client
	 * is shuffling active frames, closing the window will not
	 * properly transfer focus.
	 */

	public OptionsFrame()
	{
		super( "Preferences" );

		tabs = new JTabbedPane();
		tabs.setTabLayoutPolicy( JTabbedPane.SCROLL_TAB_LAYOUT );

		JPanel addonPanel = new JPanel( new GridLayout( 2, 1, 10, 10 ) );
		addonPanel.add( new ScriptButtonPanel() );
		addonPanel.add( new BookmarkManagePanel() );

		addTab( "General", new GeneralOptionsPanel() );
		addTab( "Items", new ItemOptionsPanel() );
		addTab( "Relay", new RelayOptionsPanel() );
		addTab( "Areas", new AreaOptionsPanel() );
		addTab( "Links", addonPanel );
		addTab( "Chat", new ChatOptionsPanel() );

		framePanel.setLayout( new CardLayout( 10, 10 ) );
		framePanel.add( tabs, "" );
	}

	private class RelayOptionsPanel extends OptionsPanel
	{
		private JCheckBox [] optionBoxes;

		private final String [][] options =
		{
			{ "relayAddsUseLinks", "Add [use] links when acquiring items" },
			{ "relayAddsCustomCombat", "Add custom combat button to fight page" },
			{ "relayRemovesRunaway", "Move runaway button to skill usage dropdown" },
			{ "relayAddsQuickScripts", "Add quick script links to main browser" },
			{ "relayAddsRestoreLinks", "Add HP/MP restore links to left side pane" },
			{ "relayAddsUpArrowLinks", "Add mood maintenance links to left side pane" },
			{ "relayAddsGraphicalCLI", "Add link to in-browser command-line interface to right side pane" },
			{ "relayAddsKoLSimulator", "Add link to Ayvuir's Simulator of Loathing to right side pane" },

			{ "relayAlwaysBuysGum", "Automatically buy gum when visiting the sewer" },
			{ "relayRemovesExpensiveItems", "Remove unaffordable items from stores in browser" },
			{ "relayRemovesMinpricedItems", "Remove items priced at minimum from stores in browser" },
			{ "relayRemovesUnrelatedItems", "Remove items unrelated to your search from stores in browser" },
			{ "trackLocationChanges", "Adventuring in browser changes selected adventure location" }
		};

		/**
		 * Constructs a new <code>StartupOptionsPanel</code>, containing a
		 * place for the users to select their desired server and for them
		 * to modify any applicable proxy settings.
		 */

		public RelayOptionsPanel()
		{
			super( "Relay Browser", new Dimension( 20, 16 ), new Dimension( 370, 16 ) );
			VerifiableElement [] elements = new VerifiableElement[ options.length ];

			optionBoxes = new JCheckBox[ options.length ];
			for ( int i = 0; i < options.length; ++i )
				optionBoxes[i] = new JCheckBox();

			for ( int i = 0; i < options.length; ++i )
				elements[i] = new VerifiableElement( options[i][1], JLabel.LEFT, optionBoxes[i] );

			setContent( elements );
			actionCancelled();
		}

		public void actionConfirmed()
		{
			for ( int i = 0; i < options.length; ++i )
				StaticEntity.setProperty( options[i][0], String.valueOf( optionBoxes[i].isSelected() ) );
		}

		public void actionCancelled()
		{
			for ( int i = 0; i < options.length; ++i )
				optionBoxes[i].setSelected( StaticEntity.getBooleanProperty( options[i][0] ) );
		}
	}

	private class GeneralOptionsPanel extends OptionsPanel
	{
		private JCheckBox [] optionBoxes;

		private final String [][] options =
		{
			{ "defaultToRelayBrowser", "Browser shortcut button loads relay browser" },
			{ "showAllRequests", "Show requests synchronously in mini-browser" },

			{ "areaValidation", "Enable stat checks before using adventures" },
			{ "allowThiefShrugOff", "Allow shrug-off of buffs during mood changes" },

			{ "autoSetConditions", "Autofill conditions field when selecting areas" },
			{ "allowStasisTactics", "Allow stasis-type commands when using combat familiars" },
			{ "sortAdventures", "Sort adventure list display by moxie evade rating" }
		};

		/**
		 * Constructs a new <code>StartupOptionsPanel</code>, containing a
		 * place for the users to select their desired server and for them
		 * to modify any applicable proxy settings.
		 */

		public GeneralOptionsPanel()
		{
			super( "General Options", new Dimension( 20, 16 ), new Dimension( 370, 16 ) );
			VerifiableElement [] elements = new VerifiableElement[ options.length ];

			optionBoxes = new JCheckBox[ options.length ];
			for ( int i = 0; i < options.length; ++i )
				optionBoxes[i] = new JCheckBox();

			for ( int i = 0; i < options.length; ++i )
				elements[i] = new VerifiableElement( options[i][1], JLabel.LEFT, optionBoxes[i] );

			setContent( elements );
			actionCancelled();
		}

		public void actionConfirmed()
		{
			for ( int i = 0; i < options.length; ++i )
				StaticEntity.setProperty( options[i][0], String.valueOf( optionBoxes[i].isSelected() ) );

			actionCancelled();
			AdventureDatabase.refreshAdventureList();
		}

		public void actionCancelled()
		{
			for ( int i = 0; i < options.length; ++i )
				optionBoxes[i].setSelected( StaticEntity.getBooleanProperty( options[i][0] ) );
		}
	}

	private class ItemOptionsPanel extends OptionsPanel
	{
		private JCheckBox [] optionBoxes;

		private final String [][] options =
		{
			{ "allowGenericUse", "Enable generic item usage in scripted \"use\"" },
			{ "cloverProtectActive", "Enable clover protection for automated adventures" },

			{ "autoCheckpoint", "Enable outfit checkpointing during NPC purchases" },
			{ "assumeInfiniteNPCItems", "Assume infinite NPC items for item creation" },

			{ "createWithoutBoxServants", "Create without requiring a box servant" },
			{ "autoRepairBoxes", "Create and install new box servant after explosion" },

			{ "autoSatisfyWithMall", "Buy items from the mall whenever needed" },
			{ "autoSatisfyWithNPCs", "Buy items from NPC stores whenever needed" },
			{ "autoSatisfyWithStash", "Take items from the clan stash whenever needed" }
		};

		/**
		 * Constructs a new <code>StartupOptionsPanel</code>, containing a
		 * place for the users to select their desired server and for them
		 * to modify any applicable proxy settings.
		 */

		public ItemOptionsPanel()
		{
			super( "Item Options", new Dimension( 20, 16 ), new Dimension( 370, 16 ) );
			VerifiableElement [] elements = new VerifiableElement[ options.length ];

			optionBoxes = new JCheckBox[ options.length ];
			for ( int i = 0; i < options.length; ++i )
				optionBoxes[i] = new JCheckBox();

			for ( int i = 0; i < options.length; ++i )
				elements[i] = new VerifiableElement( options[i][1], JLabel.LEFT, optionBoxes[i] );

			setContent( elements );
			actionCancelled();
		}

		public void actionConfirmed()
		{
			for ( int i = 0; i < options.length; ++i )
				StaticEntity.setProperty( options[i][0], String.valueOf( optionBoxes[i].isSelected() ) );

			actionCancelled();
			ConcoctionsDatabase.refreshConcoctions();
		}

		public void actionCancelled()
		{
			for ( int i = 0; i < options.length; ++i )
				optionBoxes[i].setSelected( StaticEntity.getBooleanProperty( options[i][0] ) );
		}
	}

	private class AreaOptionsPanel extends OptionsPanel
	{
		private String [] zones;
		private JCheckBox [] options;

		public AreaOptionsPanel()
		{
			super( "Adventure List", new Dimension( 20, 16 ), new Dimension( 370, 16 ) );

			zones = new String[ AdventureDatabase.ZONE_NAMES.size() ];
			options = new JCheckBox[ AdventureDatabase.ZONE_NAMES.size() ];

			for ( int i = 0; i < options.length; ++i )
				options[i] = new JCheckBox();

			VerifiableElement [] elements = new VerifiableElement[ AdventureDatabase.ZONE_NAMES.size() ];
			String [] names = new String[ AdventureDatabase.ZONE_NAMES.keySet().size() ];
			AdventureDatabase.ZONE_NAMES.keySet().toArray( names );

			for ( int i = 0; i < names.length; ++i )
			{
				zones[i] = (String) AdventureDatabase.ZONE_NAMES.get( names[i] );
				elements[i] = new VerifiableElement( "Hide " + AdventureDatabase.ZONE_DESCRIPTIONS.get( names[i] ), JLabel.LEFT, options[i] );
			}

			setContent( elements );
			actionCancelled();
		}

		public void actionConfirmed()
		{
			StringBuffer areas = new StringBuffer();

			for ( int i = 0; i < options.length; ++i )
			{
				if ( options[i].isSelected() )
				{
					if ( areas.length() != 0 )
						areas.append( ',' );

					areas.append( zones[i] );
				}
			}

			StaticEntity.setProperty( "zoneExcludeList", areas.toString() );
			super.actionConfirmed();
			AdventureDatabase.refreshAdventureList();
		}

		public void actionCancelled()
		{
			String excluded = StaticEntity.getProperty( "zoneExcludeList" );
			for ( int i = 0; i < zones.length; ++i )
				options[i].setSelected( excluded.indexOf( zones[i] ) != -1 );
		}
	}

	private abstract class ShiftableOrderPanel extends ItemManagePanel implements ListDataListener
	{
		private LockableListModel list;

		public ShiftableOrderPanel( String title, LockableListModel list )
		{
			super( title, "move up", "move down", list );
			elementList.setSelectionMode( ListSelectionModel.SINGLE_SELECTION );

			this.list = list;
			list.addListDataListener( this );
		}

		public final void actionConfirmed()
		{
			int index = elementList.getSelectedIndex();
			if ( index == -1 )
				return;

			Object value = list.remove( index );
			list.add( index - 1, value );
			elementList.setSelectedIndex( index - 1 );
		}

		public final void actionCancelled()
		{
			int index = elementList.getSelectedIndex();
			if ( index == -1 )
				return;

			Object value = list.remove( index );
			list.add( index + 1, value );
			elementList.setSelectedIndex( index + 1 );
		}

		public void intervalAdded( ListDataEvent e )
		{	saveSettings();
		}

		public void intervalRemoved( ListDataEvent e )
		{	saveSettings();
		}

		public void contentsChanged( ListDataEvent e )
		{	saveSettings();
		}

		public abstract void saveSettings();
	}

	private class ScriptButtonPanel extends ShiftableOrderPanel implements ListDataListener
	{
		private LockableListModel scriptList;

		public ScriptButtonPanel()
		{
			super( "gCLI Toolbar Buttons", new LockableListModel() );
			this.scriptList = (LockableListModel) elementList.getModel();

			String [] scriptList = StaticEntity.getProperty( "scriptList" ).split( " \\| " );

			for ( int i = 0; i < scriptList.length; ++i )
				this.scriptList.add( scriptList[i] );

			JPanel extraButtons = new JPanel( new BorderLayout( 2, 2 ) );
			extraButtons.add( new AddScriptButton(), BorderLayout.NORTH );
			extraButtons.add( new AddCommandButton(), BorderLayout.CENTER );
			extraButtons.add( new DeleteListingButton(), BorderLayout.SOUTH );
			buttonPanel.add( extraButtons, BorderLayout.SOUTH );
		}

		private class AddScriptButton extends JButton implements ActionListener
		{
			public AddScriptButton()
			{
				super( "script file" );
				addActionListener( this );
			}

			public void actionPerformed( ActionEvent e )
			{
				String rootPath = SCRIPT_DIRECTORY.getAbsolutePath();
				JFileChooser chooser = new JFileChooser( rootPath );
				int returnVal = chooser.showOpenDialog( null );

				if ( chooser.getSelectedFile() == null )
					return;

				if ( returnVal == JFileChooser.APPROVE_OPTION )
				{
					String scriptPath = chooser.getSelectedFile().getAbsolutePath();
					if ( scriptPath.startsWith( rootPath ) )
						scriptPath = scriptPath.substring( rootPath.length() + 1 );

					scriptList.add( "call " + scriptPath );
				}
			}
		}

		private class AddCommandButton extends JButton implements ActionListener
		{
			public AddCommandButton()
			{
				super( "cli command" );
				addActionListener( this );
			}

			public void actionPerformed( ActionEvent e )
			{
				String currentValue = JOptionPane.showInputDialog( "CLI Command", "" );
				if ( currentValue != null && currentValue.length() != 0 )
					scriptList.add( currentValue );
			}
		}

		private class DeleteListingButton extends JButton implements ActionListener
		{
			public DeleteListingButton()
			{
				super( "delete" );
				addActionListener( this );
			}

			public void actionPerformed( ActionEvent e )
			{
				int index = elementList.getSelectedIndex();
				if ( index == -1 )
					return;

				scriptList.remove( index );
			}
		}

		public void saveSettings()
		{
			StringBuffer settingString = new StringBuffer();
			if ( scriptList.size() != 0 )
				settingString.append( (String) scriptList.get(0) );

			for ( int i = 1; i < scriptList.size(); ++i )
			{
				settingString.append( " | " );
				settingString.append( (String) scriptList.get(i) );
			}

			StaticEntity.setProperty( "scriptList", settingString.toString() );
		}
	}

	/**
	 * Panel used for handling chat-related options and preferences,
	 * including font size, window management and maybe, eventually,
	 * coloring options for contacts.
	 */

	private class ChatOptionsPanel extends OptionsPanel
	{
		private JComboBox fontSizeSelect;
		private JComboBox chatStyleSelect;
		private JComboBox useTabSelect;
		private JComboBox popupSelect;
		private JComboBox eSoluSelect;

		public ChatOptionsPanel()
		{
			super( "" );

			fontSizeSelect = new JComboBox();
			for ( int i = 1; i <= 7; ++i )
				fontSizeSelect.addItem( String.valueOf( i ) );

			chatStyleSelect = new JComboBox();
			chatStyleSelect.addItem( "No monitor, individual channels, individual blues" );
			chatStyleSelect.addItem( "No monitor, individual channels, combined blues" );
			chatStyleSelect.addItem( "No monitor, combined channels, individual blues" );
			chatStyleSelect.addItem( "No monitor, combined channels, combined blues" );
			chatStyleSelect.addItem( "Global monitor, individual channels, individual blues" );
			chatStyleSelect.addItem( "Global monitor, individual channels, combined blues" );
			chatStyleSelect.addItem( "Standard KoL style (no monitor, everything combined)" );

			useTabSelect = new JComboBox();
			useTabSelect.addItem( "Use windowed chat interface" );
			useTabSelect.addItem( "Use tabbed chat interface" );

			popupSelect = new JComboBox();
			popupSelect.addItem( "Display /friends and /who in chat display" );
			popupSelect.addItem( "Popup a window for /friends and /who" );

			eSoluSelect = new JComboBox();
			eSoluSelect.addItem( "Nameclick select bar only" );
			eSoluSelect.addItem( "eSolu scriptlet chat links (color)" );
			eSoluSelect.addItem( "eSolu scriptlet chat links (gray)" );

			VerifiableElement [] elements = new VerifiableElement[5];
			elements[0] = new VerifiableElement( "Font Size: ", fontSizeSelect );
			elements[1] = new VerifiableElement( "Chat Style: ", chatStyleSelect );
			elements[2] = new VerifiableElement( "Tabbed Chat: ", useTabSelect );
			elements[3] = new VerifiableElement( "Contact List: ", popupSelect );
			elements[4] = new VerifiableElement( "eSolu Script: ", eSoluSelect );

			setContent( elements );
			actionCancelled();
		}

		public void actionConfirmed()
		{
			StaticEntity.setProperty( "fontSize", (String) fontSizeSelect.getSelectedItem() );
			LimitedSizeChatBuffer.setFontSize( StaticEntity.parseInt( (String) fontSizeSelect.getSelectedItem() ) );

			StaticEntity.setProperty( "chatStyle", String.valueOf( chatStyleSelect.getSelectedIndex() ) );
			StaticEntity.setProperty( "useTabbedChat", String.valueOf( useTabSelect.getSelectedIndex() ) );
			StaticEntity.setProperty( "usePopupContacts", String.valueOf( popupSelect.getSelectedIndex() ) );
			StaticEntity.setProperty( "eSoluScriptType", String.valueOf( eSoluSelect.getSelectedIndex() ) );

			super.actionConfirmed();
		}

		public void actionCancelled()
		{
			fontSizeSelect.setSelectedItem( StaticEntity.getProperty( "fontSize" ) );
			LimitedSizeChatBuffer.setFontSize( StaticEntity.getIntegerProperty( "fontSize" ) );

			chatStyleSelect.setSelectedIndex( StaticEntity.getIntegerProperty( "chatStyle" ) );
			useTabSelect.setSelectedIndex( StaticEntity.getIntegerProperty( "useTabbedChat" ) );
			popupSelect.setSelectedIndex( StaticEntity.getIntegerProperty( "usePopupContacts" ) );
			eSoluSelect.setSelectedIndex( StaticEntity.getIntegerProperty( "eSoluScriptType" ) );
		}
	}

	/**
	 * A special panel which generates a list of bookmarks which
	 * can subsequently be managed.
	 */

	private class BookmarkManagePanel extends ShiftableOrderPanel
	{
		public BookmarkManagePanel()
		{
			super( "Configure Bookmarks", bookmarks );

			JPanel extraButtons = new JPanel( new BorderLayout( 2, 2 ) );
			extraButtons.add( new AddBookmarkButton(), BorderLayout.NORTH );
			extraButtons.add( new RenameBookmarkButton(), BorderLayout.CENTER );
			extraButtons.add( new DeleteBookmarkButton(), BorderLayout.SOUTH );
			buttonPanel.add( extraButtons, BorderLayout.SOUTH );
		}

		public void saveSettings()
		{	saveBookmarks();
		}

		private class AddBookmarkButton extends JButton implements ActionListener
		{
			public AddBookmarkButton()
			{
				super( "new page" );
				addActionListener( this );
			}

			public void actionPerformed( ActionEvent e )
			{
				String newName = JOptionPane.showInputDialog( "Add a bookmark!", "http://www.google.com/" );
				if ( newName == null )
					return;

				bookmarks.add( "New bookmark " + (bookmarks.size() + 1) + "|" + newName + "|" + String.valueOf( newName.indexOf( "pwd" ) != -1 ) );
			}
		}

		private class RenameBookmarkButton extends JButton implements ActionListener
		{
			public RenameBookmarkButton()
			{
				super( "rename" );
				addActionListener( this );
			}

			public void actionPerformed( ActionEvent e )
			{
				int index = elementList.getSelectedIndex();
				if ( index == -1 )
					return;

				String currentItem = (String)elementList.getSelectedValue();
				if ( currentItem == null )
					return;

				String [] bookmarkData = currentItem.split( "\\|" );

				String name = bookmarkData[0];
				String location = bookmarkData[1];
				String pwdhash = bookmarkData[2];

				String newName = JOptionPane.showInputDialog( "Rename your bookmark?", name );

				if ( newName == null )
					return;

				bookmarks.remove( index );
				bookmarks.add( newName + "|" + location + "|" + pwdhash );
			}
		}

		private class DeleteBookmarkButton extends JButton implements ActionListener
		{
			public DeleteBookmarkButton()
			{
				super( "delete" );
				addActionListener( this );
			}

			public void actionPerformed( ActionEvent e )
			{
				int index = elementList.getSelectedIndex();
				if ( index == -1 )
					return;

				bookmarks.remove( index );
			}
		}
	}
}
