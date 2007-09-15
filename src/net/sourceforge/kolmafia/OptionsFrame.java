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
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;
import javax.swing.UIManager;

import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;

import com.informit.guides.JDnDList;

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
		breakfastPanel.add( new BreakfastPanel( "Ronin-Clear Characters", "Softcore" ) );
		breakfastPanel.add( new BreakfastPanel( "In-Ronin Characters", "Hardcore" ) );

		this.tabs.addTab( "Tabs", new StartupFramesPanel() );
		this.tabs.addTab( "Look", new UserInterfacePanel()  );
		this.tabs.addTab( "Tasks", breakfastPanel );

		this.addTab( "Links", addonPanel );
		this.addTab( "Logs", new SessionLogOptionsPanel() );
		this.addTab( "Chat", new ChatOptionsPanel() );

		this.framePanel.setLayout( new CardLayout( 10, 10 ) );
		this.framePanel.add( this.tabs, "" );

		if ( !KoLSettings.getBooleanProperty( "customizedTabs" ) )
			this.tabs.setSelectedIndex( 2 );
		else if ( LocalRelayServer.isRunning() )
			this.tabs.setSelectedIndex( 1 );
		else
			this.tabs.setSelectedIndex( 0 );
	}

	private class SessionLogOptionsPanel extends OptionsPanel
	{
		private JCheckBox [] optionBoxes;

		private final String [][] options =
		{
			{ "logStatusOnLogin", "Session log records your player's state on login" },
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
					KoLSettings.setUserProperty( this.options[i][0], String.valueOf( this.optionBoxes[i].isSelected() ) );
		}

		public void actionCancelled()
		{
			for ( int i = 0; i < this.options.length; ++i )
				if ( this.options[i].length != 0 )
					this.optionBoxes[i].setSelected( KoLSettings.getBooleanProperty( this.options[i][0] ) );
		}
	}


	private class RelayOptionsPanel extends OptionsPanel
	{
		private JLabel colorChanger;
		private JCheckBox [] optionBoxes;

		private final String [][] options =
		{
			{ "relayAllowsOverrides", "Enable user-scripted relay browser overrides" },
			{ "relayAddsWikiLinks", "Check wiki for item descriptions (fails for unknowns)" },
			{ "relayUsesCachedImages", "Cache KoL images to conserve bandwidth (dialup)" },

			{ "", "" },

			{ "relayViewsCustomItems", "View items registered with OneTonTomato's Kilt script" },
			{ "lucreCoreLeaderboard", "Participate in Oxbarn's KoLDB Lucre-core leaderboard" },

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

			{ "", "" },

			{ "basementBuysItems", "List effects for items you don't have while basement diving" },
			{ "relayAlwaysBuysGum", "Automatically buy gum when visiting the sewer" }
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
					KoLSettings.setUserProperty( this.options[i][0], String.valueOf( this.optionBoxes[i].isSelected() ) );
			}
		}

		public void actionCancelled()
		{
			String color = KoLSettings.getUserProperty( "defaultBorderColor" );
			if ( color.equals( "blue" ) )
				this.colorChanger.setBackground( Color.blue );
			else
				this.colorChanger.setBackground( DataUtilities.toColor( color ) );

			for ( int i = 0; i < this.options.length; ++i )
				this.optionBoxes[i].setSelected( KoLSettings.getBooleanProperty( this.options[i][0] ) );
		}
	}

	private class GeneralOptionsPanel extends OptionsPanel
	{
		private JCheckBox [] optionBoxes;

		private final String [][] options =
		{
			{ "showAllRequests", "Show all requests in a mini-browser window" },
			{ "avoidInvertingTabs", "Do not invert nested tabs in main window" },

			{ "", "" },

			{ "useZoneComboBox", "Use zone selection instead of adventure name filter" },
			{ "cacheMallSearches", "Cache mall search terms in mall search interface" },
			{ "mapLoadsMiniBrowser", "Map button loads mini browser instead of relay browser" },
			{ "useLowBandwidthRadio", "Use lower bandwidth server for KoL Radio" },

			{ "", "" },

			{ "completeHealthRestore", "Remove health-reducing status effects before health restore" },
			{ "ignoreAutoAttack", "Treat auto-attack as a null combat round for CCS" },
			{ "useFastOutfitSwitch", "Use fast outfit switching instead of piecewise switching" },

			{ "", "" },

			{ "cloverProtectActive", "Auto-disassemble hermit, marmot and barrel clovers" },
			{ "createHackerSummons", "Auto-create 31337 scrolls if no scroll conditions are set" },
			{ "mementoListActive", "Prevent accidental destruction of 'memento' items" },
			{ "allowGenericUse", "Enable generic item usage in scripted \"use\"" },

			{ "", "" },

			{ "switchEquipmentForBuffs", "Allow equipment changing when casting buffs" },
			{ "allowEncounterRateBurning", "Cast combat rate modifiers during buff balancing" },
			{ "allowBreakfastBurning", "Summon noodles/reagents/garnishes/hearts during buff balancing" },
			{ "allowNonMoodBurning", "Cast buffs not defined in moods during buff balancing" },

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
					KoLSettings.setUserProperty( this.options[i][0], String.valueOf( this.optionBoxes[i].isSelected() ) );

			this.actionCancelled();
			ConcoctionsDatabase.refreshConcoctions();
		}

		public void actionCancelled()
		{
			for ( int i = 0; i < this.options.length; ++i )
				if ( !this.options[i][0].equals( "" ) )
					this.optionBoxes[i].setSelected( KoLSettings.getBooleanProperty( this.options[i][0] ) );
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
			String [] scriptList = KoLSettings.getUserProperty( "scriptList" ).split( " \\| " );

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

			KoLSettings.setUserProperty( "scriptList", settingString.toString() );
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
					KoLSettings.setUserProperty( options[i][0], String.valueOf( optionBoxes[i].isSelected() ) );

			KoLSettings.setUserProperty( "eSoluScriptType", this.eSoluActiveOption.isSelected() ?
				(this.eSoluColorlessOption.isSelected() ? "2" : "1") : "0" );

			if ( this.fontSizes[0].isSelected() )
				KoLSettings.setUserProperty( "chatFontSize", "small" );
			else if ( this.fontSizes[1].isSelected() )
				KoLSettings.setUserProperty( "chatFontSize", "medium" );
			else if ( this.fontSizes[2].isSelected() )
				KoLSettings.setUserProperty( "chatFontSize", "large" );

			LimitedSizeChatBuffer.updateFontSize();

			commandBuffer.fireBufferChanged();
			KoLMessenger.updateFontSize();
		}

		public void actionCancelled()
		{
			for ( int i = 0; i < options.length; ++i )
				if ( options[i].length > 0 )
					optionBoxes[i].setSelected( KoLSettings.getBooleanProperty( options[i][0] ) );

			this.eSoluActiveOption.setSelected( KoLSettings.getIntegerProperty( "eSoluScriptType" ) > 0 );
			this.eSoluColorlessOption.setSelected( KoLSettings.getIntegerProperty( "eSoluScriptType" ) > 1 );

			this.innerGradient.setBackground( tab.CloseTabPaneEnhancedUI.notifiedA );
			this.outerGradient.setBackground( tab.CloseTabPaneEnhancedUI.notifiedB );

			String fontSize = KoLSettings.getUserProperty( "chatFontSize" );
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

	protected class StartupFramesPanel extends KoLPanel implements ListDataListener
	{
		private final String [][] FRAME_OPTIONS =
		{
			{ "Adventure", "AdventureFrame" },
			{ "Mini-Browser", "RequestFrame" },
			{ "Relay Server", "LocalRelayServer" },

			{ "Purchases", "MallSearchFrame" },
			{ "Graphical CLI", "CommandDisplayFrame" },

			{ "Player Status", "CharsheetFrame" },
			{ "Item Manager", "ItemManageFrame" },
			{ "Gear Changer", "GearChangeFrame" },

			{ "Store Manager", "StoreManageFrame" },
			{ "Museum Display", "MuseumFrame" },

			{ "Hall of Legends", "MeatManageFrame" },
			{ "Skill Casting", "SkillBuffFrame" },

			{ "Contact List", "ContactListFrame" },
			{ "Buffbot Manager", "BuffBotFrame" },
			{ "Purchase Buffs", "BuffRequestFrame" },

			{ "Flower Hunter", "FlowerHunterFrame" },
			{ "Mushroom Plot", "MushroomFrame" },
			{ "Familiar Trainer", "FamiliarTrainingFrame" },

			{ "IcePenguin Express", "MailboxFrame" },
			{ "Loathing Chat", "KoLMessenger" },
			{ "Recent Events", "EventsFrame" },

			{ "Clan Management", "ClanManageFrame" },
			{ "Farmer's Almanac", "CalendarFrame" },
			{ "Internal Database", "ExamineItemsFrame" },

			{ "Coin Toss Game", "MoneyMakingGameFrame" },
			{ "Preferences", "OptionsFrame" }
		};

		private boolean isRefreshing = false;

		private LockableListModel completeList = new LockableListModel();
		private LockableListModel startupList = new LockableListModel();
		private LockableListModel desktopList = new LockableListModel();

		public StartupFramesPanel()
		{
			super( new Dimension( 100, 20 ), new Dimension( 300, 20 ) );
			this.setContent( null );

			for ( int i = 0; i < this.FRAME_OPTIONS.length; ++i )
				this.completeList.add( this.FRAME_OPTIONS[i][0] );

			JPanel optionPanel = new JPanel( new GridLayout( 1, 3, 10, 10 ) );
			optionPanel.add( new LabeledScrollPanel( "Complete List", new JDnDList( this.completeList, false ) ) );
			optionPanel.add( new LabeledScrollPanel( "Startup as Window", new JDnDList( this.startupList ) ) );
			optionPanel.add( new LabeledScrollPanel( "Startup in Tabs", new JDnDList( this.desktopList ) ) );

			JTextArea message = new JTextArea(
				"These are the global settings for what shows up when KoLmafia successfully logs into the Kingdom of Loathing.  You can drag and drop options in the lists below to customize what will show up.\n\n" +

				"When you place the Local Relay Server into the 'startup in tabs' section, KoLmafia will start up the server but not open your browser.  When you place the Contact List into the 'startup in tabs' section, KoLmafia will force a refresh of your contact list on login.\n" );

			message.setColumns( 40 );
			message.setLineWrap( true );
			message.setWrapStyleWord( true );
			message.setEditable( false );
			message.setOpaque( false );
			message.setFont( DEFAULT_FONT );

			this.container.add( message, BorderLayout.NORTH );
			this.container.add( optionPanel, BorderLayout.SOUTH );
			this.actionCancelled();

			this.completeList.addListDataListener( this );
			this.startupList.addListDataListener( this );
			this.desktopList.addListDataListener( this );
		}

		public void actionConfirmed()
		{	this.actionCancelled();
		}

		public void actionCancelled()
		{
			this.isRefreshing = true;

			String username = (String) saveStateNames.getSelectedItem();
			if ( username == null )
				username = "";

			this.startupList.clear();
			this.desktopList.clear();

			KoLmafiaGUI.checkFrameSettings();

			String frameString = KoLSettings.getUserProperty( "initialFrames" );
			String desktopString = KoLSettings.getUserProperty( "initialDesktop" );

			String [] pieces;

			pieces = frameString.split( "," );
			for ( int i = 0; i < pieces.length; ++i )
				for ( int j = 0; j < this.FRAME_OPTIONS.length; ++j )
					if ( !this.startupList.contains( this.FRAME_OPTIONS[j][0] ) && this.FRAME_OPTIONS[j][1].equals( pieces[i] ) )
						this.startupList.add( this.FRAME_OPTIONS[j][0] );

			pieces = desktopString.split( "," );
			for ( int i = 0; i < pieces.length; ++i )
				for ( int j = 0; j < this.FRAME_OPTIONS.length; ++j )
					if ( !this.desktopList.contains( this.FRAME_OPTIONS[j][0] ) && this.FRAME_OPTIONS[j][1].equals( pieces[i] ) )
						this.desktopList.add( this.FRAME_OPTIONS[j][0] );

			this.isRefreshing = false;
			this.saveLayoutSettings();
		}

		public boolean shouldAddStatusLabel( VerifiableElement [] elements )
		{	return false;
		}

		public void setEnabled( boolean isEnabled )
		{
		}

		public void intervalAdded( ListDataEvent e )
		{
			if ( e.getSource() == this.startupList )
				this.desktopList.removeAll( this.startupList );

			if ( e.getSource() == this.desktopList )
				this.startupList.removeAll( this.desktopList );

			this.saveLayoutSettings();
		}

		public void intervalRemoved( ListDataEvent e )
		{	this.saveLayoutSettings();
		}

		public void contentsChanged( ListDataEvent e )
		{
		}

		public void saveLayoutSettings()
		{
			if ( this.isRefreshing )
				return;

			StringBuffer frameString = new StringBuffer();
			StringBuffer desktopString = new StringBuffer();

			for ( int i = 0; i < this.startupList.getSize(); ++i )
				for ( int j = 0; j < this.FRAME_OPTIONS.length; ++j )
					if ( this.startupList.getElementAt(i).equals( this.FRAME_OPTIONS[j][0] ) )
					{
						if ( frameString.length() != 0 ) frameString.append( "," );
						frameString.append( this.FRAME_OPTIONS[j][1] );
					}

			for ( int i = 0; i < this.desktopList.getSize(); ++i )
				for ( int j = 0; j < this.FRAME_OPTIONS.length; ++j )
					if ( this.desktopList.getElementAt(i).equals( this.FRAME_OPTIONS[j][0] ) )
					{
						if ( desktopString.length() != 0 ) desktopString.append( "," );
						desktopString.append( this.FRAME_OPTIONS[j][1] );
					}

			KoLSettings.setUserProperty( "initialFrames", frameString.toString() );
			KoLSettings.setUserProperty( "initialDesktop", desktopString.toString() );
		}
	}

	/**
	 * Allows the user to select to select the framing mode to use.
	 */

	protected class UserInterfacePanel extends OptionsPanel
	{
		private JCheckBox [] optionBoxes;

		private String [][] options =

			System.getProperty( "os.name" ).startsWith( "Windows" ) ?

			new String [][]
			{
				{ "guiUsesOneWindow", "Restrict interface to a single window" },
				{ "useSystemTrayIcon", "Minimize main interface to system tray" },
				{ "addCreationQueue", "Add creation queueing interface to item manager" },
				{ "addStatusBarToFrames", "Add a status line to independent windows" },
				{},
				{ "addExitMenuItems", "Add logout and exit options to general menu" },
				{ "useDecoratedTabs", "Use shiny decorated tabs instead of OS default" },
				{ "allowCloseableDesktopTabs", "Allow tabs on main window to be closed" },
			}

			:

			new String [][]
  			{
  				{ "guiUsesOneWindow", "Restrict interface to a single window" },
				{ "addCreationQueue", "Add creation queueing interface to item manager" },
  				{ "addStatusBarToFrames", "Add a status line to independent windows" },
  				{},
				{ "addExitMenuItems", "Add logout and exit options to general menu" },
  				{ "useDecoratedTabs", "Use shiny decorated tabs instead of OS default" },
  				{ "allowCloseableDesktopTabs", "Allow tabs on main window to be closed" },
  			};

		private JComboBox looks, toolbars, scripts;

		public UserInterfacePanel()
		{
			super( "", new Dimension( 80, 20 ), new Dimension( 280, 20 ) );

			UIManager.LookAndFeelInfo [] installed = UIManager.getInstalledLookAndFeels();
			Object [] installedLooks = new Object[ installed.length ];

			for ( int i = 0; i < installedLooks.length; ++i )
				installedLooks[i] = installed[i].getClassName();

			this.looks = new JComboBox( installedLooks );

			this.toolbars = new JComboBox();
			this.toolbars.addItem( "Show global menus only" );
			this.toolbars.addItem( "Put toolbar along top of panel" );
			this.toolbars.addItem( "Put toolbar along bottom of panel" );
			this.toolbars.addItem( "Put toolbar along left of panel" );

			this.scripts = new JComboBox();
			this.scripts.addItem( "Do not show script bar on main interface" );
			this.scripts.addItem( "Put script bar after normal toolbar" );
			this.scripts.addItem( "Put script bar along right of panel" );

			VerifiableElement [] elements = new VerifiableElement[3];

			elements[0] = new VerifiableElement( "Java L&F: ", this.looks );
			elements[1] = new VerifiableElement( "Toolbar: ", this.toolbars );
			elements[2] = new VerifiableElement( "Scripts: ", this.scripts );

			this.actionCancelled();
			this.setContent( elements );
		}

		public boolean shouldAddStatusLabel( VerifiableElement [] elements )
		{	return false;
		}

		public void setContent( VerifiableElement [] elements )
		{
			super.setContent( elements );
			this.add( new InterfaceCheckboxPanel(), BorderLayout.CENTER );
		}

		public void setEnabled( boolean isEnabled )
		{
		}

		public void actionConfirmed()
		{
			String lookAndFeel = (String) this.looks.getSelectedItem();
			if ( lookAndFeel != null )
				KoLSettings.setUserProperty( "swingLookAndFeel", lookAndFeel );

			KoLSettings.setUserProperty( "useToolbars", String.valueOf( this.toolbars.getSelectedIndex() != 0 ) );
			KoLSettings.setUserProperty( "scriptButtonPosition", String.valueOf( this.scripts.getSelectedIndex() ) );
			KoLSettings.setUserProperty( "toolbarPosition", String.valueOf( this.toolbars.getSelectedIndex() ) );
		}

		public void actionCancelled()
		{
			this.looks.setSelectedItem( KoLSettings.getUserProperty( "swingLookAndFeel" ) );
			this.toolbars.setSelectedIndex( KoLSettings.getIntegerProperty( "toolbarPosition" ) );
			this.scripts.setSelectedIndex( KoLSettings.getIntegerProperty( "scriptButtonPosition" ) );
		}

		private class InterfaceCheckboxPanel extends OptionsPanel
		{
			private JLabel innerGradient, outerGradient;

			public InterfaceCheckboxPanel()
			{
				super( new Dimension( 20, 16 ), new Dimension( 370, 16 ) );
				VerifiableElement [] elements = new VerifiableElement[ UserInterfacePanel.this.options.length + 3 ];

				UserInterfacePanel.this.optionBoxes = new JCheckBox[ UserInterfacePanel.this.options.length ];
				for ( int i = 0; i < UserInterfacePanel.this.options.length; ++i )
					UserInterfacePanel.this.optionBoxes[i] = new JCheckBox();

				for ( int i = 0; i < UserInterfacePanel.this.options.length; ++i )
				{
					if ( UserInterfacePanel.this.options[i].length == 0 )
						elements[i] = new VerifiableElement();
					else
						elements[i] = new VerifiableElement( UserInterfacePanel.this.options[i][1], JLabel.LEFT, UserInterfacePanel.this.optionBoxes[i] );
				}

				elements[ UserInterfacePanel.this.options.length ] = new VerifiableElement();

				this.outerGradient = new TabColorChanger( "outerTabColor" );
				elements[ UserInterfacePanel.this.options.length + 1 ] = new VerifiableElement( "Change the outer portion of the tab gradient (shiny tabs)",
					JLabel.LEFT, this.outerGradient );

				this.innerGradient = new TabColorChanger( "innerTabColor" );
				elements[ UserInterfacePanel.this.options.length + 2 ] = new VerifiableElement( "Change the inner portion of the tab gradient (shiny tabs)",
					JLabel.LEFT, this.innerGradient );

				this.actionCancelled();
				this.setContent( elements );
			}

			public void actionConfirmed()
			{
				for ( int i = 0; i < UserInterfacePanel.this.options.length; ++i )
					if ( UserInterfacePanel.this.options[i].length > 0 )
						KoLSettings.setUserProperty( UserInterfacePanel.this.options[i][0], String.valueOf( UserInterfacePanel.this.optionBoxes[i].isSelected() ) );
			}

			public void actionCancelled()
			{
				for ( int i = 0; i < UserInterfacePanel.this.options.length; ++i )
					if ( UserInterfacePanel.this.options[i].length > 0 )
						UserInterfacePanel.this.optionBoxes[i].setSelected( KoLSettings.getBooleanProperty( UserInterfacePanel.this.options[i][0] ) );

				this.innerGradient.setBackground( tab.CloseTabPaneEnhancedUI.selectedA );
				this.outerGradient.setBackground( tab.CloseTabPaneEnhancedUI.selectedB );
			}

			public void setEnabled( boolean isEnabled )
			{
			}

			private class TabColorChanger extends LabelColorChanger
			{
				public TabColorChanger( String property )
				{	super( property );
				}

				public void applyChanges()
				{
					if ( this.property.equals( "innerTabColor" ) )
						CloseTabPaneEnhancedUI.selectedA = InterfaceCheckboxPanel.this.innerGradient.getBackground();
					else
						CloseTabPaneEnhancedUI.selectedB = InterfaceCheckboxPanel.this.outerGradient.getBackground();

					OptionsFrame.this.tabs.repaint();
				}
			}
		}
	}
}
