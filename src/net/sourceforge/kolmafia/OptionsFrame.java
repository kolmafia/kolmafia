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
import java.awt.Dimension;
import java.awt.GridLayout;

import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.ListSelectionModel;

import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import tab.CloseTabPaneEnhancedUI;

import net.java.dev.spellcast.utilities.DataUtilities;
import net.java.dev.spellcast.utilities.LockableListModel;

public class OptionsFrame extends KoLFrame
{
	public OptionsFrame()
	{
		super( "Preferences" );

		JPanel addonPanel = new JPanel( new GridLayout( 2, 1, 10, 10 ) );
		addonPanel.add( new ScriptButtonPanel() );
		addonPanel.add( new BookmarkManagePanel() );

		this.addTab( "General", new GeneralOptionsPanel() );
		this.addTab( "Relay", new RelayOptionsPanel() );

		JPanel breakfastPanel = new JPanel();
		breakfastPanel.setLayout( new BoxLayout( breakfastPanel, BoxLayout.Y_AXIS ) );

		breakfastPanel.add( new ScriptPanel() );
		breakfastPanel.add( new BreakfastPanel( "Softcore Characters", "Softcore" ) );
		breakfastPanel.add( new BreakfastPanel( "Hardcore Characters", "Hardcore" ) );

		this.tabs.addTab( "Tabs", new StartupFramesPanel() );
		this.tabs.addTab( "Look", new UserInterfacePanel()  );
		this.tabs.addTab( "Tasks", breakfastPanel );

		this.addTab( "Links", addonPanel );
		this.addTab( "Logs", new SessionLogOptionsPanel() );
		this.addTab( "Chat", new ChatOptionsPanel() );

		this.framePanel.setLayout( new CardLayout( 10, 10 ) );
		this.framePanel.add( this.tabs, "" );
	}

	private class SessionLogOptionsPanel extends OptionsPanel
	{
		private JCheckBox [] optionBoxes;

		private final String [][] options =
		{
			{ "logReverseOrder", "Log adventures left instead of adventures used" },
			{},
			{ "logBattleAction", "Session log records attacks for each round" },
			{ "logFamiliarActions", "Session log records actions made by familiars" },
			{ "logMonsterHealth", "Session log records monster health changes" },
			{},
			{ "logGainMessages", "Session log records HP/MP/meat changes" },
			{ "logStatGains", "Session log records stat gains" },
			{ "logAcquiredItems", "Session log records items acquired" },
			{ "logStatusEffects", "Session log records status effects gained" }
		};

		/**
		 * Constructs a new <code>StartupOptionsPanel</code>, containing a
		 * place for the users to select their desired server and for them
		 * to modify any applicable proxy settings.
		 */

		public SessionLogOptionsPanel()
		{
			super( "Session Log", new Dimension( 20, 16 ), new Dimension( 370, 16 ) );
			VerifiableElement [] elements = new VerifiableElement[ this.options.length ];

			this.optionBoxes = new JCheckBox[ this.options.length ];
			for ( int i = 0; i < this.options.length; ++i )
				this.optionBoxes[i] = new JCheckBox();

			for ( int i = 0; i < this.options.length; ++i )
			{
				if ( this.options[i].length == 0 )
					elements[i] = new VerifiableElement();
				else
					elements[i] = new VerifiableElement( this.options[i][1], JLabel.LEFT, this.optionBoxes[i] );
			}

			this.actionCancelled();
			this.setContent( elements );
		}

		public void actionConfirmed()
		{
			for ( int i = 0; i < this.options.length; ++i )
				if ( this.options[i].length != 0 )
					StaticEntity.setProperty( this.options[i][0], String.valueOf( this.optionBoxes[i].isSelected() ) );
		}

		public void actionCancelled()
		{
			for ( int i = 0; i < this.options.length; ++i )
				if ( this.options[i].length != 0 )
					this.optionBoxes[i].setSelected( StaticEntity.getBooleanProperty( this.options[i][0] ) );
		}
	}


	private class RelayOptionsPanel extends OptionsPanel
	{
		private JLabel colorChanger;
		private JCheckBox [] optionBoxes;

		private final String [][] options =
		{
			{ "relayAllowsOverrides", "Enable user-scripted relay browser overrides" },
			{ "relayUsesCachedImages", "Cache KoL images to conserve bandwidth (dialup)" },

			{ "", "" },

			{ "relayAddsRestoreLinks", "Add HP/MP restore links to left side pane" },
			{ "relayAddsUpArrowLinks", "Add buff maintenance links to left side pane" },
			{ "relayTextualizesEffects", "Textualize effect links in left side pane" },

			{ "", "" },

			{ "relayMaintainsEffects", "Run moods during manual adventuring" },
			{ "relayMaintainsHealth", "Maintain health during manual adventuring" },
			{ "relayMaintainsMana", "Maintain mana during manual adventuring" },

			{ "", "" },

			{ "relayAddsQuickScripts", "Add quick script links to menu bar (see Links tab)" },
			{ "relayUsesIntegratedChat", "Integrate chat and relay browser gCLI interfaces" },
			{ "relayFormatsChatText", "Reformat incoming chat HTML to conform to web standards" },

			{ "", "" },

			{ "relayAddsGraphicalCLI", "Add command-line interface to right side pane" },
			{ "relayAddsKoLSimulator", "Add Ayvuir's Simulator of Loathing to right side pane" },

			{ "", "" },

			{ "relayAddsUseLinks", "Add decorator [use] links when receiving items" },
			{ "relayUsesInlineLinks", "Force results to reload inline for [use] links" },
			{ "relayHidesJunkMallItems", "Hide junk and overpriced items in PC stores" },

			{ "", "" },

			{ "relayAddsRoundNumber", "Add current round number to fight pages" },
			{ "relayAddsMonsterHealth", "Add known monster data to fight pages" },
			{ "relayAddsCustomCombat", "Add custom combat button to fight pages" },
			{ "showIntermediateRounds", "Show intermediate rounds during scripted combat" },

			{ "", "" },

			{ "relayAlwaysBuysGum", "Automatically buy gum when visiting the sewer" },
			{ "relayViewsCustomItems", "View items registered with OneTonTomato's Kilt script" },
		};

		/**
		 * Constructs a new <code>StartupOptionsPanel</code>, containing a
		 * place for the users to select their desired server and for them
		 * to modify any applicable proxy settings.
		 */

		public RelayOptionsPanel()
		{
			super( "Relay Browser", new Dimension( 16, 16 ), new Dimension( 370, 16 ) );
			VerifiableElement [] elements = new VerifiableElement[ this.options.length + 1 ];

			this.optionBoxes = new JCheckBox[ this.options.length ];
			for ( int i = 0; i < this.options.length; ++i )
				this.optionBoxes[i] = new JCheckBox();

			for ( int i = 0; i < this.options.length; ++i )
			{
				elements[i] = this.options[i][0].equals( "" ) ? new VerifiableElement() :
					new VerifiableElement( this.options[i][1], JLabel.LEFT, this.optionBoxes[i] );
			}

			this.colorChanger = new LabelColorChanger( "defaultBorderColor" );
			elements[ this.options.length ] = new VerifiableElement( "Change the color for tables in the browser interface",
				JLabel.LEFT, this.colorChanger );

			this.setContent( elements );
			this.actionCancelled();
		}

		public void actionConfirmed()
		{
			for ( int i = 0; i < this.options.length; ++i )
			{
				if ( !this.options[i][0].equals( "" ) )
					StaticEntity.setProperty( this.options[i][0], String.valueOf( this.optionBoxes[i].isSelected() ) );
			}
		}

		public void actionCancelled()
		{
			String color = StaticEntity.getProperty( "defaultBorderColor" );
			if ( color.equals( "blue" ) )
				this.colorChanger.setBackground( Color.blue );
			else
				this.colorChanger.setBackground( DataUtilities.toColor( color ) );

			for ( int i = 0; i < this.options.length; ++i )
				this.optionBoxes[i].setSelected( StaticEntity.getBooleanProperty( this.options[i][0] ) );
		}
	}

	private class GeneralOptionsPanel extends OptionsPanel
	{
		private JCheckBox [] optionBoxes;

		private final String [][] options =
		{
			{ "showAllRequests", "Show all requests in a mini-browser window" },
			{ "mapLoadsMiniBrowser", "Map button loads mini browser instead of relay browser" },
			{ "avoidInvertingTabs", "Do not invert nested tabs in main window" },
			{ "useLowBandwidthRadio", "Use lower bandwidth server for KoL Radio" },

			{ "", "" },

			{ "autoBuyRestores", "Automatically buy more hp/mp restores when needed" },
			{ "completeHealthRestore", "Remove health-reducing status effects before health restore" },
			{ "ignoreAutoAttack", "Treat auto-attack as a null combat round for CCS" },
			{ "useFastOutfitSwitch", "Use fast outfit switching instead of piecewise switching" },

			{ "", "" },

			{ "cloverProtectActive", "Protect against automated clover adventures" },
			{ "mementoListActive", "Prevent accidental destruction of 'memento' items" },
			{ "allowGenericUse", "Enable generic item usage in scripted \"use\"" },
			{ "cacheMallSearches", "Cache mall search terms in mall search interface" },

			{ "", "" },

			{ "switchEquipmentForBuffs", "Allow equipment changing when casting buffs" },
			{ "allowEncounterRateBurning", "Cast combat rate modifiers during conditional recast" },
			{ "allowBreakfastBurning", "Cast breakfast skills during conditional recast" },
			{ "allowNonMoodBurning", "Cast buffs not defined in moods during conditional recast" },

			{ "", "" },

			{ "allowNegativeTally", "Allow item counts in session results to go negative" },
			{ "autoSatisfyWithNPCs", "Buy items from NPC stores whenever needed" },
			{ "autoSatisfyWithMall", "Buy items from the mall whenever needed" },
			{ "autoSatisfyWithStash", "Take items from the clan stash whenever needed" }
		};

		/**
		 * Constructs a new <code>StartupOptionsPanel</code>, containing a
		 * place for the users to select their desired server and for them
		 * to modify any applicable proxy settings.
		 */

		public GeneralOptionsPanel()
		{
			super( "General Options", new Dimension( 20, 16 ), new Dimension( 370, 16 ) );
			VerifiableElement [] elements = new VerifiableElement[ this.options.length ];

			this.optionBoxes = new JCheckBox[ this.options.length ];
			for ( int i = 0; i < this.options.length; ++i )
				this.optionBoxes[i] = new JCheckBox();

			for ( int i = 0; i < this.options.length; ++i )
				elements[i] = this.options[i][0].equals( "" ) ? new VerifiableElement() :
					new VerifiableElement( this.options[i][1], JLabel.LEFT, this.optionBoxes[i] );

			this.setContent( elements );
			this.actionCancelled();
		}

		public void actionConfirmed()
		{
			for ( int i = 0; i < this.options.length; ++i )
				if ( !this.options[i][0].equals( "" ) )
					StaticEntity.setProperty( this.options[i][0], String.valueOf( this.optionBoxes[i].isSelected() ) );

			this.actionCancelled();
			ConcoctionsDatabase.refreshConcoctions();
		}

		public void actionCancelled()
		{
			for ( int i = 0; i < this.options.length; ++i )
				if ( !this.options[i][0].equals( "" ) )
					this.optionBoxes[i].setSelected( StaticEntity.getBooleanProperty( this.options[i][0] ) );
		}
	}

	private abstract class ShiftableOrderPanel extends LabeledScrollPanel implements ListDataListener
	{
		public LockableListModel list;
		public JList elementList;

		public ShiftableOrderPanel( String title, LockableListModel list )
		{
			super( title, "move up", "move down", new JList( list ) );

			this.elementList = (JList) this.scrollComponent;
			this.elementList.setSelectionMode( ListSelectionModel.SINGLE_SELECTION );

			this.list = list;
			list.addListDataListener( this );
		}

		public final void actionConfirmed()
		{
			int index = this.elementList.getSelectedIndex();
			if ( index == -1 )
				return;

			Object value = this.list.remove( index );
			this.list.add( index - 1, value );
			this.elementList.setSelectedIndex( index - 1 );
		}

		public final void actionCancelled()
		{
			int index = this.elementList.getSelectedIndex();
			if ( index == -1 )
				return;

			Object value = this.list.remove( index );
			this.list.add( index + 1, value );
			this.elementList.setSelectedIndex( index + 1 );
		}

		public void intervalAdded( ListDataEvent e )
		{	this.saveSettings();
		}

		public void intervalRemoved( ListDataEvent e )
		{	this.saveSettings();
		}

		public void contentsChanged( ListDataEvent e )
		{	this.saveSettings();
		}

		public abstract void saveSettings();
	}

	private class ScriptButtonPanel extends ShiftableOrderPanel
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
			this.buttonPanel.add( extraButtons, BorderLayout.SOUTH );
		}

		private class AddScriptButton extends ThreadedButton
		{
			public AddScriptButton()
			{	super( "script file" );
			}

			public void run()
			{
				String rootPath = SCRIPT_LOCATION.getAbsolutePath();
				JFileChooser chooser = new JFileChooser( rootPath );
				int returnVal = chooser.showOpenDialog( null );

				if ( chooser.getSelectedFile() == null )
					return;

				if ( returnVal == JFileChooser.APPROVE_OPTION )
				{
					String scriptPath = chooser.getSelectedFile().getAbsolutePath();
					if ( scriptPath.startsWith( rootPath ) )
						scriptPath = scriptPath.substring( rootPath.length() + 1 );

					ScriptButtonPanel.this.list.add( "call " + scriptPath );
				}

				saveSettings();
			}
		}

		private class AddCommandButton extends ThreadedButton
		{
			public AddCommandButton()
			{	super( "cli command" );
			}

			public void run()
			{
				String currentValue = input( "Enter the desired CLI Command" );
				if ( currentValue != null && currentValue.length() != 0 )
					ScriptButtonPanel.this.list.add( currentValue );

				saveSettings();
			}
		}

		private class DeleteListingButton extends ThreadedButton
		{
			public DeleteListingButton()
			{	super( "delete" );
			}

			public void run()
			{
				int index = ScriptButtonPanel.this.elementList.getSelectedIndex();
				if ( index == -1 )
					return;

				ScriptButtonPanel.this.list.remove( index );
				saveSettings();
			}
		}

		public void saveSettings()
		{
			StringBuffer settingString = new StringBuffer();
			if ( this.list.size() != 0 )
				settingString.append( (String) this.list.getElementAt(0) );

			for ( int i = 1; i < this.list.getSize(); ++i )
			{
				settingString.append( " | " );
				settingString.append( (String) this.list.getElementAt(i) );
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
		private String [][] options =
		{
			{ "useTabbedChatFrame", "Use tabbed, rather than multi-window, chat" },
			{ "useSeparateChannels", "Put different channels into separate displays" },
			{ "chatLinksUseRelay", "Use the relay browser when clicking on chat links" },
			{},
			{ "greenScreenProtection", "Ignore all event messages in KoLmafia chat" },
			{ "useChatMonitor", "Add an \"as KoL would show it\" display" },
			{ "addChatCommandLine", "Add a simplified graphical CLI to tabbed chat" },
			{},
			{ "useShinyTabbedChat", "Use shiny closeable tabs when using tabbed chat" },
			{ "useContactsFrame", "Use a popup window for /friends and /who" },
			{ "useChatToolbar", "Add a toolbar to chat windows for special commands" },
			{ "logChatMessages", "Log chats when using KoLmafia (requires restart)" },
		};

		private ButtonGroup fontSizeGroup;
		private JRadioButton [] fontSizes;

		private JCheckBox [] optionBoxes;
		private JCheckBox eSoluActiveOption, eSoluColorlessOption;
		private JLabel innerGradient, outerGradient;

		public ChatOptionsPanel()
		{
			super( new Dimension( 30, 16 ), new Dimension( 370, 16 ) );

			this.fontSizeGroup = new ButtonGroup();
			this.fontSizes = new JRadioButton[3];
			for ( int i = 0; i < 3; ++i )
			{
				this.fontSizes[i] = new JRadioButton();
				this.fontSizeGroup.add( this.fontSizes[i] );
			}

			this.optionBoxes = new JCheckBox[ options.length ];
			for ( int i = 0; i < options.length; ++i )
				optionBoxes[i] = new JCheckBox();

			this.eSoluActiveOption = new JCheckBox();
			this.eSoluColorlessOption = new JCheckBox();

			VerifiableElement [] elements = new VerifiableElement[ 4 + options.length + 6 ];

			elements[0] = new VerifiableElement( "Use small fonts in hypertext displays", JLabel.LEFT, this.fontSizes[0] );
			elements[1] = new VerifiableElement( "Use medium fonts in hypertext displays", JLabel.LEFT, this.fontSizes[1] );
			elements[2] = new VerifiableElement( "Use large fonts in hypertext displays", JLabel.LEFT, this.fontSizes[2] );
			elements[3] = new VerifiableElement();

			for ( int i = 0; i < options.length; ++i )
			{
				if ( options[i].length > 0 )
					elements[i+4] = new VerifiableElement( options[i][1], JLabel.LEFT, optionBoxes[i] );
				else
					elements[i+4] = new VerifiableElement();
			}

			int tabCount = options.length + 4;
			elements[tabCount++] = new VerifiableElement();

			elements[tabCount++] = new VerifiableElement( "Activate eSolu scriptlet for KoLmafia chat", JLabel.LEFT, this.eSoluActiveOption );
			elements[tabCount++] = new VerifiableElement( "Switch eSolu scriptlet to colorless mode", JLabel.LEFT, this.eSoluColorlessOption );

			elements[tabCount++] = new VerifiableElement();

			this.outerGradient = new TabColorChanger( "outerChatColor" );
			elements[tabCount++] = new VerifiableElement( "Change the outer portion of highlighted tab gradient",
				JLabel.LEFT, this.outerGradient );

			this.innerGradient = new TabColorChanger( "innerChatColor" );
			elements[tabCount++] = new VerifiableElement( "Change the inner portion of highlighted tab gradient",
				JLabel.LEFT, this.innerGradient );

			this.setContent( elements );
			this.actionCancelled();
		}

		public void actionConfirmed()
		{
			for ( int i = 0; i < options.length; ++i )
				if ( options[i].length > 0 )
					StaticEntity.setProperty( options[i][0], String.valueOf( optionBoxes[i].isSelected() ) );

			StaticEntity.setProperty( "eSoluScriptType", this.eSoluActiveOption.isSelected() ?
				(this.eSoluColorlessOption.isSelected() ? "2" : "1") : "0" );

			if ( this.fontSizes[0].isSelected() )
				StaticEntity.setProperty( "chatFontSize", "small" );
			else if ( this.fontSizes[1].isSelected() )
				StaticEntity.setProperty( "chatFontSize", "medium" );
			else if ( this.fontSizes[2].isSelected() )
				StaticEntity.setProperty( "chatFontSize", "large" );

			LimitedSizeChatBuffer.updateFontSize();

			commandBuffer.fireBufferChanged();
			KoLMessenger.updateFontSize();

			super.actionConfirmed();
		}

		public void actionCancelled()
		{
			for ( int i = 0; i < options.length; ++i )
				if ( options[i].length > 0 )
					optionBoxes[i].setSelected( StaticEntity.getBooleanProperty( options[i][0] ) );

			this.eSoluActiveOption.setSelected( StaticEntity.getIntegerProperty( "eSoluScriptType" ) > 0 );
			this.eSoluColorlessOption.setSelected( StaticEntity.getIntegerProperty( "eSoluScriptType" ) > 1 );

			this.innerGradient.setBackground( tab.CloseTabPaneEnhancedUI.notifiedA );
			this.outerGradient.setBackground( tab.CloseTabPaneEnhancedUI.notifiedB );

			String fontSize = StaticEntity.getProperty( "chatFontSize" );
			this.fontSizes[ fontSize.equals( "large" ) ? 2 : fontSize.equals( "medium" ) ? 1 : 0 ].setSelected( true );
		}

		private class TabColorChanger extends LabelColorChanger
		{
			public TabColorChanger( String property )
			{	super( property );
			}

			public void applyChanges()
			{
				if ( this.property.equals( "innerChatColor" ) )
					CloseTabPaneEnhancedUI.notifiedA = ChatOptionsPanel.this.innerGradient.getBackground();
				else
					CloseTabPaneEnhancedUI.notifiedB = ChatOptionsPanel.this.outerGradient.getBackground();
			}
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
			this.buttonPanel.add( extraButtons, BorderLayout.SOUTH );
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
				String newName = input( "Add a bookmark!", "http://www.google.com/" );
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
				int index = BookmarkManagePanel.this.elementList.getSelectedIndex();
				if ( index == -1 )
					return;

				String currentItem = (String)BookmarkManagePanel.this.elementList.getSelectedValue();
				if ( currentItem == null )
					return;

				String [] bookmarkData = currentItem.split( "\\|" );

				String name = bookmarkData[0];
				String location = bookmarkData[1];
				String pwdhash = bookmarkData[2];

				String newName = input( "Rename your bookmark?", name );

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
				int index = BookmarkManagePanel.this.elementList.getSelectedIndex();
				if ( index == -1 )
					return;

				bookmarks.remove( index );
			}
		}
	}
}
