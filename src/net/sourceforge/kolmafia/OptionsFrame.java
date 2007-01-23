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
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import java.io.File;
import java.util.Arrays;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JColorChooser;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.ListModel;
import javax.swing.ListSelectionModel;

import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import net.java.dev.spellcast.utilities.DataUtilities;
import net.java.dev.spellcast.utilities.LockableListModel;
import net.sourceforge.kolmafia.MoodSettings.MoodTrigger;

public class OptionsFrame extends KoLFrame
{
	private JList moodList;

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

		JPanel moodPanel = new JPanel( new BorderLayout() );
		moodPanel.add( new MoodTriggerListPanel(), BorderLayout.CENTER );

		AddTriggerPanel triggers = new AddTriggerPanel();
		moodList.addListSelectionListener( triggers );
		moodPanel.add( triggers, BorderLayout.NORTH );

		addTab( "General", new GeneralOptionsPanel() );
		addTab( "Relay", new RelayOptionsPanel() );

		tabs.addTab( "Moods", moodPanel );

		addTab( "Links", addonPanel );
		addTab( "Logs", new SessionLogOptionsPanel() );
		addTab( "Chat", new ChatOptionsPanel() );

		framePanel.setLayout( new CardLayout( 10, 10 ) );
		framePanel.add( tabs, "" );

		boolean anyFrameVisible = false;
		for ( int i = 0; i < existingFrames.size(); ++i )
			anyFrameVisible |= ((KoLFrame)existingFrames.get(i)).isVisible();

		if ( !anyFrameVisible )
			tabs.setSelectedIndex(2);
		else if ( KoLDesktop.instanceExists() )
			tabs.setSelectedIndex(3);
		else
			tabs.setSelectedIndex(0);
	}

	private class SessionLogOptionsPanel extends OptionsPanel
	{
		private JCheckBox [] optionBoxes;

		private final String [][] options =
		{
			{ "logReverseOrder", "Log adventures left instead of adventures used" },
			{ "logStatGains", "Session log records stat gains" },
			{ "logAcquiredItems", "Session log records items acquired" },
			{ "logStatusEffects", "Session log records status effects gained" },
			{ "logGainMessages", "Session log records HP/MP/meat changes" },
			{ "logBattleAction", "Session log records attacks for each round" }
		};

		/**
		 * Constructs a new <code>StartupOptionsPanel</code>, containing a
		 * place for the users to select their desired server and for them
		 * to modify any applicable proxy settings.
		 */

		public SessionLogOptionsPanel()
		{
			super( "Session Log", new Dimension( 20, 16 ), new Dimension( 370, 16 ) );
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


	private class RelayOptionsPanel extends OptionsPanel
	{
		private JLabel colorChanger;
		private JCheckBox [] optionBoxes;

		private final String [][] options =
		{
			{ "relayAddsRestoreLinks", "Add HP/MP restore links to left side pane" },
			{ "relayAddsUpArrowLinks", "Add buff maintenance links to left side pane" },
			{ "relayAddsMissingEffects", "Display mood trigger buffs with zero duration" },
			{ "relayTextualizesEffects", "Textualize effect links in left side pane" },
			{ "relayHidesJunkMallItems", "Hide junk and overpriced items in PC stores" },

			{ "", "" },

			{ "relayUsesIntegratedChat", "Integrate chat and relay browser gCLI interfaces" },
			{ "relayAddsGraphicalCLI", "Add link to command-line interface to right side pane" },
			{ "relayAddsKoLSimulator", "Add link to Ayvuir's Simulator of Loathing to right side pane" },

			{ "", "" },

			{ "relayAddsQuickScripts", "Add quick script links to main browser" },
			{ "relayUsesCachedImages", "Save images from KoL to conserve bandwidth" },
			{ "relayAlwaysBuysGum", "Automatically buy gum when visiting the sewer" },
			{ "relayMaintainsMoods", "Automatically maintain health, mana, and moods in browser" }
		};

		/**
		 * Constructs a new <code>StartupOptionsPanel</code>, containing a
		 * place for the users to select their desired server and for them
		 * to modify any applicable proxy settings.
		 */

		public RelayOptionsPanel()
		{
			super( "Relay Browser", new Dimension( 16, 16 ), new Dimension( 370, 16 ) );
			VerifiableElement [] elements = new VerifiableElement[ options.length + 1 ];

			optionBoxes = new JCheckBox[ options.length ];
			for ( int i = 0; i < options.length; ++i )
				optionBoxes[i] = new JCheckBox();

			for ( int i = 0; i < options.length; ++i )
			{
				elements[i] = options[i][0].equals( "" ) ? new VerifiableElement() :
					new VerifiableElement( options[i][1], JLabel.LEFT, optionBoxes[i] );
			}

			colorChanger = new BorderColorChanger();
			elements[ options.length ] = new VerifiableElement( "Change the color for tables in the browser interface",
				JLabel.LEFT, colorChanger );

			setContent( elements );
			actionCancelled();
		}

		private class BorderColorChanger extends JLabel implements MouseListener
		{
			public BorderColorChanger()
			{
				setOpaque( true );
				addMouseListener( this );
			}

			public void mousePressed( MouseEvent e )
			{
				Color c = JColorChooser.showDialog( null, "Choose a border color:", getBackground() );
				if ( c == null )
					return;

				StaticEntity.setProperty( "defaultBorderColor", DataUtilities.toHexString( c ) );
				setBackground( c );
			}

			public void mouseReleased( MouseEvent e )
			{
			}

			public void mouseClicked( MouseEvent e )
			{
			}

			public void mouseEntered( MouseEvent e )
			{
			}

			public void mouseExited( MouseEvent e )
			{
			}
		}

		public void actionConfirmed()
		{
			for ( int i = 0; i < options.length; ++i )
			{
				if ( !options[i][0].equals( "" ) )
					StaticEntity.setProperty( options[i][0], String.valueOf( optionBoxes[i].isSelected() ) );
			}
		}

		public void actionCancelled()
		{
			String color = StaticEntity.getProperty( "defaultBorderColor" );
			if ( color.equals( "blue" ) )
				colorChanger.setBackground( Color.blue );
			else
				colorChanger.setBackground( DataUtilities.toColor( color ) );

			for ( int i = 0; i < options.length; ++i )
				optionBoxes[i].setSelected( StaticEntity.getBooleanProperty( options[i][0] ) );
		}
	}

	private class GeneralOptionsPanel extends OptionsPanel
	{
		private JCheckBox [] optionBoxes;

		private final String [][] options =
		{
			{ "showAllRequests", "Show requests synchronously in mini-browser" },
			{ "sortAdventures", "Sort adventure lists by moxie evade rating" },
			{ "cloverProtectActive", "Protect against automated clover adventures" },
			{ "protectAgainstOverdrink", "Protect against accidental overdrinking" },

			{ "", "" },

			{ "allowGenericUse", "Enable generic item usage in scripted \"use\"" },
			{ "autoSetConditions", "Automatically fill conditions field with defaults" },
			{ "autoRoninPickpocket", "Pickpocket to satisfy item conditions in Ronin" },

			{ "", "" },

			{ "allowThiefShrugOff", "Allow shrug-off of buffs during mood changes" },
			{ "autoSatisfyWithMall", "Buy items from the mall whenever needed" },
			{ "autoSatisfyWithNPCs", "Buy items from NPC stores whenever needed" }
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
				elements[i] = options[i][0].equals( "" ) ? new VerifiableElement() :
					new VerifiableElement( options[i][1], JLabel.LEFT, optionBoxes[i] );

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
				if ( !options[i][0].equals( "" ) )
					optionBoxes[i].setSelected( StaticEntity.getBooleanProperty( options[i][0] ) );
		}
	}

	private abstract class ShiftableOrderPanel extends LabeledScrollPanel implements ListDataListener
	{
		public LockableListModel list;
		public JList elementList;

		public ShiftableOrderPanel( String title, LockableListModel list )
		{
			super( title, "move up", "move down", new JList( list ) );

			this.elementList = (JList) scrollComponent;
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
		public ScriptButtonPanel()
		{
			super( "gCLI Toolbar Buttons", new LockableListModel() );
			String [] scriptList = StaticEntity.getProperty( "scriptList" ).split( " \\| " );

			for ( int i = 0; i < scriptList.length; ++i )
				this.list.add( scriptList[i] );

			JPanel extraButtons = new JPanel( new BorderLayout( 2, 2 ) );
			extraButtons.add( new AddScriptButton(), BorderLayout.NORTH );
			extraButtons.add( new AddCommandButton(), BorderLayout.CENTER );
			extraButtons.add( new DeleteListingButton(), BorderLayout.SOUTH );
			buttonPanel.add( extraButtons, BorderLayout.SOUTH );
		}

		private class AddScriptButton extends ThreadedButton
		{
			public AddScriptButton()
			{	super( "script file" );
			}

			public void run()
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

					list.add( "call " + scriptPath );
				}
			}
		}

		private class AddCommandButton extends ThreadedButton
		{
			public AddCommandButton()
			{	super( "cli command" );
			}

			public void run()
			{
				String currentValue = JOptionPane.showInputDialog( "CLI Command", "" );
				if ( currentValue != null && currentValue.length() != 0 )
					list.add( currentValue );
			}
		}

		private class DeleteListingButton extends ThreadedButton
		{
			public DeleteListingButton()
			{	super( "delete" );
			}

			public void run()
			{
				int index = elementList.getSelectedIndex();
				if ( index == -1 )
					return;

				list.remove( index );
			}
		}

		public void saveSettings()
		{
			StringBuffer settingString = new StringBuffer();
			if ( list.size() != 0 )
				settingString.append( (String) list.get(0) );

			for ( int i = 1; i < list.size(); ++i )
			{
				settingString.append( " | " );
				settingString.append( (String) list.get(i) );
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

		private class AddBookmarkButton extends ThreadedButton
		{
			public AddBookmarkButton()
			{	super( "new page" );
			}

			public void run()
			{
				String newName = JOptionPane.showInputDialog( "Add a bookmark!", "http://www.google.com/" );
				if ( newName == null )
					return;

				bookmarks.add( "New bookmark " + (bookmarks.size() + 1) + "|" + newName + "|" + String.valueOf( newName.indexOf( "pwd" ) != -1 ) );
			}
		}

		private class RenameBookmarkButton extends ThreadedButton
		{
			public RenameBookmarkButton()
			{	super( "rename" );
			}

			public void run()
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

		private class DeleteBookmarkButton extends ThreadedButton
		{
			public DeleteBookmarkButton()
			{	super( "delete" );
			}

			public void run()
			{
				int index = elementList.getSelectedIndex();
				if ( index == -1 )
					return;

				bookmarks.remove( index );
			}
		}
	}


	/**
	 * Internal class used to handle everything related to
	 * BuffBot options management
	 */

	private class AddTriggerPanel extends KoLPanel implements ListSelectionListener
	{
		private LockableListModel EMPTY_MODEL = new LockableListModel();
		private LockableListModel EFFECT_MODEL = new LockableListModel();

		private TypeComboBox typeSelect;
		private ValueComboBox valueSelect;
		private JTextField commandField;

		public AddTriggerPanel()
		{
			super( "add entry", "auto-fill" );

			typeSelect = new TypeComboBox();

			Object [] names = StatusEffectDatabase.values().toArray();
			Arrays.sort( names );

			for ( int i = 0; i < names.length; ++i )
				EFFECT_MODEL.add( names[i] );

			valueSelect = new ValueComboBox();
			commandField = new JTextField();

			VerifiableElement [] elements = new VerifiableElement[3];
			elements[0] = new VerifiableElement( "Trigger On: ", typeSelect );
			elements[1] = new VerifiableElement( "Check For: ", valueSelect );
			elements[2] = new VerifiableElement( "Command: ", commandField );

			setContent( elements );
		}

		public void valueChanged( ListSelectionEvent e )
		{
			Object selected = moodList.getSelectedValue();
			if ( selected == null )
				return;

			MoodTrigger node = (MoodTrigger) selected;
			String type = node.getType();

			// Update the selected type

			if ( type.equals( "lose_effect" ) )
				typeSelect.setSelectedIndex(0);
			else if ( type.equals( "gain_effect" ) )
				typeSelect.setSelectedIndex(1);
			else if ( type.equals( "unconditional" ) )
				typeSelect.setSelectedIndex(2);

			// Update the selected effect

			valueSelect.setSelectedItem( node.getName() );
			commandField.setText( node.getAction() );
		}

		public void actionConfirmed()
		{
			MoodSettings.addTrigger( (String) typeSelect.getSelectedType(), (String) valueSelect.getSelectedItem(), commandField.getText() );
			MoodSettings.saveSettings();
		}

		public void actionCancelled()
		{
			String [] autoFillTypes = new String [] { "maximal set (all castable buffs)", "minimal set (current active buffs)" };

			String desiredType = (String) JOptionPane.showInputDialog(
				null, "Which kind of buff set would you like to use?", "Decide!",
					JOptionPane.INFORMATION_MESSAGE, null, autoFillTypes, activeEffects.isEmpty() ? autoFillTypes[0] : autoFillTypes[1] );

			if ( desiredType == autoFillTypes[0] )
				MoodSettings.maximalSet();
			else
				MoodSettings.minimalSet();

			MoodSettings.saveSettings();
		}

		public void setEnabled( boolean isEnabled )
		{
		}

		private class ValueComboBox extends MutableComboBox
		{
			public ValueComboBox()
			{	super( EFFECT_MODEL, false );
			}

			public void setSelectedItem( Object anObject )
			{
				commandField.setText( MoodSettings.getDefaultAction( typeSelect.getSelectedType(), (String) anObject ) );
				super.setSelectedItem( anObject );
			}
		}

		private class TypeComboBox extends JComboBox
		{
			public TypeComboBox()
			{
				addItem( "When an effect is lost" );
				addItem( "When an effect is gained" );
				addItem( "Unconditional trigger" );

				addActionListener( new TypeComboBoxListener() );
			}

			public String getSelectedType()
			{
				switch ( getSelectedIndex() )
				{
				case 0:
					return "lose_effect";
				case 1:
					return "gain_effect";
				case 2:
					return "unconditional";
				default:
					return null;
				}
			}

			private class TypeComboBoxListener implements ActionListener
			{
				public void actionPerformed( ActionEvent e )
				{	valueSelect.setModel( getSelectedIndex() == 2 ? EMPTY_MODEL : EFFECT_MODEL );
				}
			}
		}
	}

	private class MoodTriggerListPanel extends LabeledScrollPanel
	{
		private JComboBox moodSelect;

		public MoodTriggerListPanel()
		{
			super( "", "edit casts", "remove", new JList( MoodSettings.getTriggers() ) );

			moodSelect = new MoodComboBox();

			centerPanel.add( moodSelect, BorderLayout.NORTH );
			moodList = (JList) scrollComponent;

			JPanel extraButtons = new JPanel( new GridLayout( 3, 1, 5, 5 ) );

			extraButtons.add( new NewMoodButton() );
			extraButtons.add( new DeleteMoodButton() );
			extraButtons.add( new CopyMoodButton() );

			buttonPanel.add( extraButtons, BorderLayout.SOUTH );
		}

		public void actionConfirmed()
		{
			String desiredLevel = JOptionPane.showInputDialog( null, "TURN CHANGE!", "Set to how many turns?" );
			if ( desiredLevel == null )
				return;

			MoodSettings.addTriggers( moodList.getSelectedValues(), StaticEntity.parseInt( desiredLevel ) );
			MoodSettings.saveSettings();
		}

		public void actionCancelled()
		{
			MoodSettings.removeTriggers( moodList.getSelectedValues() );
			MoodSettings.saveSettings();
		}

		public void setEnabled( boolean isEnabled )
		{
		}

		private class MoodComboBox extends JComboBox
		{
			public MoodComboBox()
			{
				super( MoodSettings.getAvailableMoods() );
				setSelectedItem( StaticEntity.getProperty( "currentMood" ) );
				addActionListener( new MoodComboBoxListener() );
			}

			private class MoodComboBoxListener implements ActionListener
			{
				public void actionPerformed( ActionEvent e )
				{	MoodSettings.setMood( (String) getSelectedItem() );
				}
			}
		}

		private class NewMoodButton extends ThreadedButton
		{
			public NewMoodButton()
			{	super( "new list" );
			}

			public void run()
			{
				String name = JOptionPane.showInputDialog( "Give your list a name!" );
				if ( name == null )
					return;

				MoodSettings.setMood( name );
				MoodSettings.saveSettings();
			}
		}

		private class DeleteMoodButton extends ThreadedButton
		{
			public DeleteMoodButton()
			{	super( "delete list" );
			}

			public void run()
			{
				MoodSettings.deleteCurrentMood();
				MoodSettings.saveSettings();
			}
		}

		private class CopyMoodButton extends ThreadedButton
		{
			public CopyMoodButton()
			{	super( "copy list" );
			}

			public void run()
			{
				String moodName = JOptionPane.showInputDialog( "Make a copy of current mood list called:" );
				if ( moodName == null )
					return;

				if ( moodName.equals( "default" ) )
					return;

				MoodSettings.copyTriggers( moodName );
				MoodSettings.setMood( moodName );
				MoodSettings.saveSettings();
			}
		}
	}
}
