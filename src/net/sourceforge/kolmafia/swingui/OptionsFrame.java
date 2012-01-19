/**
 * Copyright (c) 2005-2012, KoLmafia development team
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

import com.informit.guides.JDnDList;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.io.File;
import java.io.IOException;

import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextPane;
import javax.swing.ListSelectionModel;
import javax.swing.UIManager;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;

import net.java.dev.spellcast.utilities.DataUtilities;
import net.java.dev.spellcast.utilities.JComponentUtilities;
import net.java.dev.spellcast.utilities.LockableListModel;
import net.java.dev.spellcast.utilities.UtilityConstants;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafiaGUI;
import net.sourceforge.kolmafia.persistence.ConcoctionDatabase;
import net.sourceforge.kolmafia.preferences.PreferenceListener;
import net.sourceforge.kolmafia.preferences.PreferenceListenerRegistry;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.CampgroundRequest;
import net.sourceforge.kolmafia.request.RelayRequest;
import net.sourceforge.kolmafia.request.UseSkillRequest;
import net.sourceforge.kolmafia.swingui.button.ThreadedButton;
import net.sourceforge.kolmafia.swingui.panel.AddCustomDeedsPanel;
import net.sourceforge.kolmafia.swingui.panel.DailyDeedsPanel;
import net.sourceforge.kolmafia.swingui.panel.GenericPanel;
import net.sourceforge.kolmafia.swingui.panel.OptionsPanel;
import net.sourceforge.kolmafia.swingui.panel.ScrollablePanel;
import net.sourceforge.kolmafia.swingui.widget.AutoHighlightTextField;
import net.sourceforge.kolmafia.swingui.widget.ColorChooser;
import net.sourceforge.kolmafia.swingui.widget.CreationSettingCheckBox;
import net.sourceforge.kolmafia.utilities.InputFieldUtilities;
import net.sourceforge.kolmafia.webui.RelayServer;

import tab.CloseTabPaneEnhancedUI;

public class OptionsFrame
	extends GenericFrame
{
	public OptionsFrame()
	{
		super( "Preferences" );

		JPanel generalPanel = new JPanel();
		generalPanel.setLayout( new BoxLayout( generalPanel, BoxLayout.Y_AXIS ) );
		generalPanel.add( new GeneralOptionsPanel() );
		generalPanel.add( new EditorPanel() );
		this.addTab( "General", generalPanel );

		JPanel browserPanel = new JPanel();
		browserPanel.setLayout( new BoxLayout( browserPanel, BoxLayout.Y_AXIS ) );
		browserPanel.add( new BrowserPanel() );
		browserPanel.add( new RelayOptionsPanel() );
		this.addTab( "Browser", browserPanel );

		this.addTab( "Main Tabs", new StartupFramesPanel() );
		this.addTab( "Look & Feel", new UserInterfacePanel() );

		JPanel breakfastPanel = new JPanel();
		breakfastPanel.setLayout( new BoxLayout( breakfastPanel, BoxLayout.Y_AXIS ) );

		breakfastPanel.add( new ScriptPanel() );
		breakfastPanel.add( new BreakfastPanel( "Ronin-Clear Characters", "Softcore" ) );
		breakfastPanel.add( new BreakfastPanel( "In-Ronin Characters", "Hardcore" ) );

		this.addTab( "Breakfast", breakfastPanel );

		JPanel customDeedPanel = new JPanel();
		customDeedPanel.setLayout( new BoxLayout( customDeedPanel, BoxLayout.Y_AXIS ) );
		customDeedPanel.add( new CustomizeDailyDeedsPanel( "Message" ) );
		customDeedPanel.add( new CustomizeDailyDeedsPanel() );

		this.addTab( "Daily Deeds", customDeedPanel );

		JPanel addonPanel = new JPanel( new GridLayout( 2, 1, 10, 10 ) );
		addonPanel.add( new ScriptButtonPanel() );
		addonPanel.add( new BookmarkManagePanel() );
		this.addTab( "Shortcuts", addonPanel );

		this.addTab( "Session Logs", new SessionLogOptionsPanel() );
		this.addTab( "Chat Options", new ChatOptionsPanel() );

		this.setCenterComponent( this.tabs );

		if ( !Preferences.getBoolean( "customizedTabs" ) )
		{
			this.tabs.setSelectedIndex( 2 );
		}
		else if ( RelayServer.isRunning() )
		{
			this.tabs.setSelectedIndex( 1 );
		}
		else
		{
			this.tabs.setSelectedIndex( 0 );
		}
	}

	private class SessionLogOptionsPanel
		extends OptionsPanel
	{
		private final JCheckBox[] optionBoxes;

		private final String[][] options =
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
		 * Constructs a new <code>StartupOptionsPanel</code>, containing a place for the users to select their
		 * desired server and for them to modify any applicable proxy settings.
		 */

		public SessionLogOptionsPanel()
		{
			super( "Session Log", new Dimension( 20, 16 ), new Dimension( 370, 16 ) );
			VerifiableElement[] elements = new VerifiableElement[ this.options.length ];

			this.optionBoxes = new JCheckBox[ this.options.length ];

			for ( int i = 0; i < this.options.length; ++i )
			{
				String[] option = this.options[ i ];
				JCheckBox optionBox = new JCheckBox();
				this.optionBoxes[ i ] = optionBox;
				elements[ i ] =
					option.length == 0 ?
					new VerifiableElement() :
					new VerifiableElement( option[ 1 ], JLabel.LEFT, optionBox );
			}

			this.actionCancelled();
			this.setContent( elements );
		}

		public void actionConfirmed()
		{
			for ( int i = 0; i < this.options.length; ++i )
			{
				String[] option = this.options[ i ];
				if ( option.length == 0 )
				{
					continue;
				}
				JCheckBox optionBox = this.optionBoxes[ i ];
				Preferences.setBoolean( option[ 0 ], optionBox.isSelected() );
			}
		}

		public void actionCancelled()
		{
			for ( int i = 0; i < this.options.length; ++i )
			{
				String[] option = this.options[ i ];
				if ( option.length == 0 )
				{
					continue;
				}
				JCheckBox optionBox = this.optionBoxes[ i ];
				optionBox.setSelected( Preferences.getBoolean( option[ 0 ] ) );
			}
		}
	}

	private class RelayOptionsPanel
		extends OptionsPanel
	{
		private final JLabel colorChanger;
		private final JCheckBox[] optionBoxes;
		private final String[][] options =
		{
			{ "relayAllowsOverrides", "Enable user-scripted relay browser overrides" },
			{ "relayUsesCachedImages", "Cache KoL images to conserve bandwidth (dialup)" },
			{ "relayOverridesImages", "Override certain KoL images" },
			{},
			{ "relayAddsWikiLinks", "Check wiki for item descriptions (fails for unknowns)" },
			{ "relayViewsCustomItems", "View items registered with OneTonTomato's Kilt script" },
			{ "relayAddsQuickScripts", "Add quick script links to menu bar (see Links tab)" },
			{},
			{ "relayAddsRestoreLinks", "Add HP/MP restore links to left side pane" },
			{ "relayAddsUpArrowLinks", "Add buff maintenance links to left side pane" },
			{ "relayTextualizesEffects", "Textualize effect links in left side pane" },
			{ "relayAddsDiscoHelper", "Add Disco Bandit helper to fights" },
			{ "macroLens", "Show Combat Macro helper during fights" },
			{},
			{ "relayMaintainsEffects", "Run moods during manual adventuring" },
			{ "relayMaintainsHealth", "Maintain health during manual adventuring" },
			{ "relayMaintainsMana", "Maintain mana during manual adventuring" },
			{},
			{ "relayUsesIntegratedChat", "Integrate chat and relay browser gCLI interfaces" },
			{ "relayFormatsChatText", "Reformat incoming chat HTML to conform to web standards" },
			{ "relayAddsGraphicalCLI", "Add command-line interface to right side pane" },
			{ "relayAddsKoLSimulator", "Add Ayvuir's Simulator of Loathing to right side pane" },
			{},
			{ "relayAddsUseLinks", "Add [use] links when receiving items" },
			{ "relayUsesInlineLinks", "Force results to reload inline for [use] links" },
			{ "relayHidesJunkMallItems", "Hide junk and overpriced items in PC stores" },
			{ "relayTrimsZapList", "Trim zap list to show only known zappable items" },
			{},
			{ "relayAddsCustomCombat", "Add custom buttons to the top of fight pages" },
			{ "arcadeGameHints", "Provide hints for Arcade games" },
			{ "relayShowSpoilers", "Show blatant spoilers for choices and puzzles" },
		};

		/**
		 * Constructs a new <code>StartupOptionsPanel</code>, containing a place for the users to select their
		 * desired server and for them to modify any applicable proxy settings.
		 */

		public RelayOptionsPanel()
		{
			super( "Relay Browser", new Dimension( 16, 16 ), new Dimension( 370, 16 ) );
			VerifiableElement[] elements = new VerifiableElement[ this.options.length + 1 ];

			this.optionBoxes = new JCheckBox[ this.options.length ];

			for ( int i = 0; i < this.options.length; ++i )
			{
				String[] option = this.options[ i ];
				JCheckBox optionBox = new JCheckBox();
				this.optionBoxes[ i ] = optionBox;
				elements[ i ] =
					option.length == 0 ?
					new VerifiableElement() :
					new VerifiableElement( option[ 1 ], JLabel.LEFT, optionBox );
			}

			this.colorChanger = new ColorChooser( "defaultBorderColor" );
			elements[ this.options.length ] =
				new VerifiableElement(
					"Change the color for tables in the browser interface", JLabel.LEFT, this.colorChanger );

			this.setContent( elements );
			this.actionCancelled();
		}

		public void actionConfirmed()
		{
			boolean overrideImages = Preferences.getBoolean( "relayOverridesImages" );
			for ( int i = 0; i < this.options.length; ++i )
			{
				String[] option = this.options[ i ];
				if ( option.length == 0 )
				{
					continue;
				}
				JCheckBox optionBox = this.optionBoxes[ i ];
				Preferences.setBoolean( option[ 0 ], optionBox.isSelected() );
			}

			if ( overrideImages != Preferences.getBoolean( "relayOverridesImages" ) )
			{
				RelayRequest.flushOverrideImages();
			}
		}

		public void actionCancelled()
		{
			String color = Preferences.getString( "defaultBorderColor" );
			if ( color.equals( "blue" ) )
			{
				this.colorChanger.setBackground( Color.blue );
			}
			else
			{
				this.colorChanger.setBackground( DataUtilities.toColor( color ) );
			}

			for ( int i = 0; i < this.options.length; ++i )
			{
				String[] option = this.options[ i ];
				if ( option.length == 0 )
				{
					continue;
				}
				JCheckBox optionBox = this.optionBoxes[ i ];
				optionBox.setSelected( Preferences.getBoolean( option[ 0 ] ) );
			}
		}
	}

	private class GeneralOptionsPanel
		extends OptionsPanel
	{
		private final JCheckBox[] optionBoxes;

		private final String[][] options =
		{
			{ "showAllRequests", "Show all requests in a mini-browser window" },
			{ "showExceptionalRequests", "Automatically load 'click here to load in relay browser' in mini-browser" },

			{},

			{ "useZoneComboBox", "Use zone selection instead of adventure name filter" },
			{ "cacheMallSearches", "Cache mall search terms in mall search interface" },
			{ "saveSettingsOnSet", "Save options to disk whenever they change" },

			{},

			{ "removeMalignantEffects", "Auto-remove malignant status effects" },
			{ "switchEquipmentForBuffs", "Allow equipment changing when casting buffs" },
			{ "allowNonMoodBurning", "Cast buffs not defined in moods during buff balancing" },
			{ "allowSummonBurning", "Cast summoning skills during buff balancing" },

			{},

			{ "cloverProtectActive", "Protect against accidental ten-leaf clover usage" },
			{ "requireSewerTestItems", "Require appropriate test items to adventure in clan sewers " },
			{ "mementoListActive", "Prevent accidental destruction of 'memento' items" },

			{},

			{ "allowNegativeTally", "Allow item counts in session results to go negative" },
			{ "autoSatisfyWithNPCs", "Buy items from NPC stores whenever needed", "yes" },
			{ "autoSatisfyWithStorage", "If you are out of Ronin, pull items from storage whenever needed", "yes" },
			{ "autoSatisfyWithCoinmasters", "Buy items with tokens at coin masters whenever needed", "yes" },
			{ "autoSatisfyWithMall", "Buy items from the mall whenever needed" },
			{ "autoSatisfyWithCloset", "Take items from the closet whenever needed", "yes" },
			{ "autoSatisfyWithStash", "Take items from the clan stash whenever needed" },
			{ "mmgAutoConfirmBets", "Auto-confirm bets in the MMG" },

			{},

			{ "sharePriceData", "Share recent Mall price data with other users" },

			{},
			{ "useLastUserAgent", "(Debug) Use last browser's userAgent" },
			{ "logBrowserInteractions", "(Debug) Verbosely log communication between KoLmafia and browser" },
			{ "logCleanedHTML", "(Debug) Invoke HTML cleaner and log resulting tree" },
			{ "logDecoratedResponses", "(Debug) Log decorated responses in debug log" },
			{ "logReadableHTML", "(Debug) Include line breaks in logged HTML" },
		};

		/**
		 * Constructs a new <code>StartupOptionsPanel</code>, containing a place for the users to select their
		 * desired server and for them to modify any applicable proxy settings.
		 */

		public GeneralOptionsPanel()
		{
			super( "General Options", new Dimension( 20, 16 ), new Dimension( 370, 16 ) );
			VerifiableElement[] elements = new VerifiableElement[ this.options.length ];

			this.optionBoxes = new JCheckBox[ this.options.length ];

			for ( int i = 0; i < this.options.length; ++i )
			{
				String[] option = this.options[ i ];
				JCheckBox optionBox =
					( option.length < 3 ) ?
					new JCheckBox() :
					new CreationSettingCheckBox( option[ 0 ] );
				this.optionBoxes[ i ] = optionBox;
				elements[ i ] =
					option.length == 0 ?
					new VerifiableElement() :
					new VerifiableElement( option[ 1 ], JLabel.LEFT, optionBox );
			}

			this.setContent( elements );
			this.actionCancelled();
		}

		public void actionConfirmed()
		{
			for ( int i = 0; i < this.options.length; ++i )
			{
				String[] option = this.options[ i ];
				if ( option.length == 0 )
				{
					continue;
				}
				JCheckBox optionBox = this.optionBoxes[ i ];
				Preferences.setBoolean( option[ 0 ], optionBox.isSelected() );
			}

			this.actionCancelled();
			ConcoctionDatabase.refreshConcoctions( true );
		}

		public void actionCancelled()
		{
			for ( int i = 0; i < this.options.length; ++i )
			{
				String[] option = this.options[ i ];
				if ( option.length == 0 )
				{
					continue;
				}
				JCheckBox optionBox = this.optionBoxes[ i ];
				optionBox.setSelected( Preferences.getBoolean( option[ 0 ] ) );
			}
		}
	}

	private abstract class ShiftableOrderPanel
		extends ScrollablePanel
		implements ListDataListener
	{
		public LockableListModel list;
		public JList elementList;

		public ShiftableOrderPanel( final String title, final LockableListModel list )
		{
			super( title, "move up", "move down", new JList( list ) );

			this.elementList = (JList) this.scrollComponent;
			this.elementList.setSelectionMode( ListSelectionModel.SINGLE_SELECTION );

			this.list = list;
			list.addListDataListener( this );
		}

		public void dispose()
		{
			list.removeListDataListener( this );
			super.dispose();
		}

		public final void actionConfirmed()
		{
			int index = this.elementList.getSelectedIndex();
			if ( index == -1 )
			{
				return;
			}

			Object value = this.list.remove( index );
			this.list.add( index - 1, value );
			this.elementList.setSelectedIndex( index - 1 );
		}

		public final void actionCancelled()
		{
			int index = this.elementList.getSelectedIndex();
			if ( index == -1 )
			{
				return;
			}

			Object value = this.list.remove( index );
			this.list.add( index + 1, value );
			this.elementList.setSelectedIndex( index + 1 );
		}

		public void intervalAdded( final ListDataEvent e )
		{
			this.saveSettings();
		}

		public void intervalRemoved( final ListDataEvent e )
		{
			this.saveSettings();
		}

		public void contentsChanged( final ListDataEvent e )
		{
			this.saveSettings();
		}

		public abstract void saveSettings();
	}

	private class ScriptButtonPanel
		extends ShiftableOrderPanel
	{
		public ScriptButtonPanel()
		{
			super( "gCLI Toolbar Buttons", new LockableListModel() );
			String[] scriptList = Preferences.getString( "scriptList" ).split( " \\| " );

			for ( int i = 0; i < scriptList.length; ++i )
			{
				this.list.add( scriptList[ i ] );
			}

			JPanel extraButtons = new JPanel( new BorderLayout( 2, 2 ) );
			extraButtons.add( new ThreadedButton( "script file", new AddScriptRunnable() ), BorderLayout.NORTH );
			extraButtons.add( new ThreadedButton( "cli command", new AddCommandRunnable() ), BorderLayout.CENTER );
			extraButtons.add( new ThreadedButton( "delete", new DeleteListingRunnable() ), BorderLayout.SOUTH );
			this.buttonPanel.add( extraButtons, BorderLayout.SOUTH );
		}

		private class AddScriptRunnable
			implements Runnable
		{
			public void run()
			{
				try
				{
					String rootPath = KoLConstants.SCRIPT_LOCATION.getCanonicalPath();

					JFileChooser chooser = new JFileChooser( rootPath );
					int returnVal = chooser.showOpenDialog( null );

					if ( chooser.getSelectedFile() == null )
					{
						return;
					}

					if ( returnVal == JFileChooser.APPROVE_OPTION )
					{
						String scriptPath = chooser.getSelectedFile().getCanonicalPath();
						if ( scriptPath.startsWith( rootPath ) )
						{
							scriptPath = scriptPath.substring( rootPath.length() + 1 );
						}

						ScriptButtonPanel.this.list.add( "call " + scriptPath );
					}
				}
				catch ( IOException e )
				{
				}

				ScriptButtonPanel.this.saveSettings();
			}
		}

		private class AddCommandRunnable
			implements Runnable
		{
			public void run()
			{
				String currentValue = InputFieldUtilities.input( "Enter the desired CLI Command" );
				if ( currentValue != null && currentValue.length() != 0 )
				{
					ScriptButtonPanel.this.list.add( currentValue );
				}

				ScriptButtonPanel.this.saveSettings();
			}
		}

		private class DeleteListingRunnable
			implements Runnable
		{
			public void run()
			{
				int index = ScriptButtonPanel.this.elementList.getSelectedIndex();
				if ( index == -1 )
				{
					return;
				}

				ScriptButtonPanel.this.list.remove( index );
				ScriptButtonPanel.this.saveSettings();
			}
		}

		public void saveSettings()
		{
			StringBuffer settingString = new StringBuffer();
			if ( this.list.size() != 0 )
			{
				settingString.append( (String) this.list.getElementAt( 0 ) );
			}

			for ( int i = 1; i < this.list.getSize(); ++i )
			{
				settingString.append( " | " );
				settingString.append( (String) this.list.getElementAt( i ) );
			}

			Preferences.setString( "scriptList", settingString.toString() );
		}
	}

	/**
	 * Panel used for handling chat-related options and preferences, including font size, window management and maybe,
	 * eventually, coloring options for contacts.
	 */

	private class ChatOptionsPanel
		extends OptionsPanel
	{
		private final String[][] options =
		{
			{ "useTabbedChatFrame", "Use tabbed, rather than multi-window, chat" },
			{ "useShinyTabbedChat", "Use shiny closeable tabs when using tabbed chat" },
			{ "addChatCommandLine", "Add a graphical CLI to tabbed chat" },
			{},
			{ "useContactsFrame", "Use a popup window for /friends and /who" },
			{ "chatLinksUseRelay", "Use the relay browser when clicking on chat links" },
			{ "useChatToolbar", "Add a toolbar to chat windows for special commands" },
			{},
			{ "mergeHobopolisChat", "Merge clan dungeon channel displays into /clan" },
			{ "greenScreenProtection", "Ignore event messages in KoLmafia chat" },
			{ "logChatMessages", "Log chats when using KoLmafia (requires restart)" },
		};

		private final ButtonGroup fontSizeGroup;
		private final JRadioButton[] fontSizes;

		private final JCheckBox[] optionBoxes;
		private final JLabel innerGradient, outerGradient;

		public ChatOptionsPanel()
		{
			super( new Dimension( 30, 16 ), new Dimension( 370, 16 ) );

			this.fontSizeGroup = new ButtonGroup();
			this.fontSizes = new JRadioButton[ 3 ];
			for ( int i = 0; i < 3; ++i )
			{
				this.fontSizes[ i ] = new JRadioButton();
				this.fontSizeGroup.add( this.fontSizes[ i ] );
			}

			this.optionBoxes = new JCheckBox[ this.options.length ];

			VerifiableElement[] elements = new VerifiableElement[ 4 + this.options.length + 3 ];

			elements[ 0 ] =
				new VerifiableElement( "Use small fonts in hypertext displays", JLabel.LEFT, this.fontSizes[ 0 ] );
			elements[ 1 ] =
				new VerifiableElement( "Use medium fonts in hypertext displays", JLabel.LEFT, this.fontSizes[ 1 ] );
			elements[ 2 ] =
				new VerifiableElement( "Use large fonts in hypertext displays", JLabel.LEFT, this.fontSizes[ 2 ] );

			elements[ 3 ] = new VerifiableElement();

			for ( int i = 0; i < this.options.length; ++i )
			{
				String[] option = this.options[ i ];
				JCheckBox optionBox = new JCheckBox();
				this.optionBoxes[ i ] = optionBox;

				elements[ i + 4 ] =
					option.length == 0 ?
					new VerifiableElement() :
					new VerifiableElement( option[ 1 ], JLabel.LEFT, optionBox );
			}

			int tabCount = this.options.length + 4;

			elements[ tabCount++ ] = new VerifiableElement();

			this.outerGradient = new TabColorChanger( "outerChatColor" );
			elements[ tabCount++ ] =
				new VerifiableElement(
					"Change the outer portion of highlighted tab gradient", JLabel.LEFT, this.outerGradient );

			this.innerGradient = new TabColorChanger( "innerChatColor" );
			elements[ tabCount++ ] =
				new VerifiableElement(
					"Change the inner portion of highlighted tab gradient", JLabel.LEFT, this.innerGradient );

			this.setContent( elements );
			this.actionCancelled();
		}

		public void actionConfirmed()
		{
			for ( int i = 0; i < this.options.length; ++i )
			{
				String[] option = this.options[ i ];
				if ( option.length == 0 )
				{
					continue;
				}
				JCheckBox optionBox = this.optionBoxes[ i ];
				Preferences.setBoolean( option[ 0 ], optionBox.isSelected() );
			}

			if ( this.fontSizes[ 0 ].isSelected() )
			{
				Preferences.setString( "chatFontSize", "small" );
			}
			else if ( this.fontSizes[ 1 ].isSelected() )
			{
				Preferences.setString( "chatFontSize", "medium" );
			}
			else if ( this.fontSizes[ 2 ].isSelected() )
			{
				Preferences.setString( "chatFontSize", "large" );
			}

			KoLConstants.commandBuffer.append( null );
		}

		public void actionCancelled()
		{
			for ( int i = 0; i < this.options.length; ++i )
			{
				String[] option = this.options[ i ];
				if ( option.length == 0 )
				{
					continue;
				}
				JCheckBox optionBox = this.optionBoxes[ i ];
				optionBox.setSelected( Preferences.getBoolean( option[ 0 ] ) );
			}

			this.innerGradient.setBackground( tab.CloseTabPaneEnhancedUI.notifiedA );
			this.outerGradient.setBackground( tab.CloseTabPaneEnhancedUI.notifiedB );

			String fontSize = Preferences.getString( "chatFontSize" );
			this.fontSizes[ fontSize.equals( "large" ) ? 2 : fontSize.equals( "medium" ) ? 1 : 0 ].setSelected( true );
		}

		private class TabColorChanger
			extends ColorChooser
		{
			public TabColorChanger( final String property )
			{
				super( property );
			}

			public void applyChanges()
			{
				if ( this.property.equals( "innerChatColor" ) )
				{
					CloseTabPaneEnhancedUI.notifiedA = ChatOptionsPanel.this.innerGradient.getBackground();
				}
				else
				{
					CloseTabPaneEnhancedUI.notifiedB = ChatOptionsPanel.this.outerGradient.getBackground();
				}
			}
		}
	}

	/**
	 * A special panel which generates a list of bookmarks which can subsequently be managed.
	 */

	private class BookmarkManagePanel
		extends ShiftableOrderPanel
	{
		public BookmarkManagePanel()
		{
			super( "Configure Bookmarks", KoLConstants.bookmarks );

			JPanel extraButtons = new JPanel( new BorderLayout( 2, 2 ) );
			extraButtons.add( new ThreadedButton( "add", new AddBookmarkRunnable() ), BorderLayout.NORTH );
			extraButtons.add( new ThreadedButton( "rename", new RenameBookmarkRunnable() ), BorderLayout.CENTER );
			extraButtons.add( new ThreadedButton( "delete", new DeleteBookmarkRunnable() ), BorderLayout.SOUTH );
			this.buttonPanel.add( extraButtons, BorderLayout.SOUTH );
		}

		public void saveSettings()
		{
			GenericFrame.saveBookmarks();
		}

		private class AddBookmarkRunnable
			implements Runnable
		{
			public void run()
			{
				String newName = InputFieldUtilities.input( "Add a bookmark!", "http://www.google.com/" );

				if ( newName == null )
				{
					return;
				}

				KoLConstants.bookmarks.add( "New bookmark " + ( KoLConstants.bookmarks.size() + 1 ) + "|" + newName + "|" + String.valueOf( newName.indexOf( "pwd" ) != -1 ) );
			}
		}

		private class RenameBookmarkRunnable
			implements Runnable
		{
			public void run()
			{
				int index = BookmarkManagePanel.this.elementList.getSelectedIndex();
				if ( index == -1 )
				{
					return;
				}

				String currentItem = (String) BookmarkManagePanel.this.elementList.getSelectedValue();
				if ( currentItem == null )
				{
					return;
				}

				String[] bookmarkData = currentItem.split( "\\|" );

				String name = bookmarkData[ 0 ];
				String location = bookmarkData[ 1 ];
				String pwdhash = bookmarkData[ 2 ];

				String newName = InputFieldUtilities.input( "Rename your bookmark?", name );

				if ( newName == null )
				{
					return;
				}

				KoLConstants.bookmarks.remove( index );
				KoLConstants.bookmarks.add( newName + "|" + location + "|" + pwdhash );
			}
		}

		private class DeleteBookmarkRunnable
			implements Runnable
		{
			public void run()
			{
				int index = BookmarkManagePanel.this.elementList.getSelectedIndex();
				if ( index == -1 )
				{
					return;
				}

				KoLConstants.bookmarks.remove( index );
			}
		}
	}

	protected class StartupFramesPanel
		extends GenericPanel
		implements ListDataListener
	{
		private boolean isRefreshing = false;

		private final LockableListModel completeList = new LockableListModel();
		private final LockableListModel startupList = new LockableListModel();
		private final LockableListModel desktopList = new LockableListModel();

		public StartupFramesPanel()
		{
			super( new Dimension( 100, 20 ), new Dimension( 300, 20 ) );
			this.setContent( null );

			for ( int i = 0; i < KoLConstants.FRAME_NAMES.length; ++i )
			{
				this.completeList.add( KoLConstants.FRAME_NAMES[ i ][ 0 ] );
			}

			JPanel optionPanel = new JPanel( new GridLayout( 1, 3, 10, 10 ) );
			optionPanel.add( new ScrollablePanel( "Complete List", new JDnDList( this.completeList ) ) );
			optionPanel.add( new ScrollablePanel( "Startup as Window", new JDnDList( this.startupList ) ) );
			optionPanel.add( new ScrollablePanel( "Startup in Tabs", new JDnDList( this.desktopList ) ) );

			JTextArea message =
				new JTextArea(
					"These are the global settings for what shows up when KoLmafia successfully logs into the Kingdom of Loathing.  You can drag and drop options in the lists below to customize what will show up.\n\n" +

					"When you place the Local Relay Server into the 'startup in tabs' section, KoLmafia will start up the server but not open your browser.  When you place the Contact List into the 'startup in tabs' section, KoLmafia will force a refresh of your contact list on login.\n" );

			message.setColumns( 40 );
			message.setLineWrap( true );
			message.setWrapStyleWord( true );
			message.setEditable( false );
			message.setOpaque( false );
			message.setFont( KoLConstants.DEFAULT_FONT );

			this.container.add( message, BorderLayout.NORTH );
			this.container.add( optionPanel, BorderLayout.SOUTH );
			this.actionCancelled();

			this.completeList.addListDataListener( this );
			this.startupList.addListDataListener( this );
			this.desktopList.addListDataListener( this );
		}

		public void dispose()
		{
			this.completeList.removeListDataListener( this );
			this.startupList.removeListDataListener( this );
			this.desktopList.removeListDataListener( this );

			super.dispose();
		}

		public void actionConfirmed()
		{
			this.actionCancelled();
		}

		public void actionCancelled()
		{
			this.isRefreshing = true;

			String username = (String) KoLConstants.saveStateNames.getSelectedItem();
			if ( username == null )
			{
				username = "";
			}

			this.startupList.clear();
			this.desktopList.clear();

			KoLmafiaGUI.checkFrameSettings();

			String frameString = Preferences.getString( "initialFrames" );
			String desktopString = Preferences.getString( "initialDesktop" );

			String[] pieces;

			pieces = frameString.split( "," );
			for ( int i = 0; i < pieces.length; ++i )
			{
				for ( int j = 0; j < KoLConstants.FRAME_NAMES.length; ++j )
				{
					if ( !this.startupList.contains( KoLConstants.FRAME_NAMES[ j ][ 0 ] ) && KoLConstants.FRAME_NAMES[ j ][ 1 ].equals( pieces[ i ] ) )
					{
						this.startupList.add( KoLConstants.FRAME_NAMES[ j ][ 0 ] );
					}
				}
			}

			pieces = desktopString.split( "," );
			for ( int i = 0; i < pieces.length; ++i )
			{
				for ( int j = 0; j < KoLConstants.FRAME_NAMES.length; ++j )
				{
					if ( !this.desktopList.contains( KoLConstants.FRAME_NAMES[ j ][ 0 ] ) && KoLConstants.FRAME_NAMES[ j ][ 1 ].equals( pieces[ i ] ) )
					{
						this.desktopList.add( KoLConstants.FRAME_NAMES[ j ][ 0 ] );
					}
				}
			}

			this.isRefreshing = false;
			this.saveLayoutSettings();
		}

		public boolean shouldAddStatusLabel( final VerifiableElement[] elements )
		{
			return false;
		}

		public void setEnabled( final boolean isEnabled )
		{
		}

		public void intervalAdded( final ListDataEvent e )
		{
			Object src = e.getSource();
			if ( src == this.startupList )
			{
				this.desktopList.removeAll( this.startupList );
			}
			else if ( src == this.desktopList )
			{
				this.startupList.removeAll( this.desktopList );
			}
			else if (src == this.completeList )
			{
				Object item = this.completeList.get( e.getIndex0() );
				this.desktopList.remove( item );
				this.startupList.remove( item );
			}

			this.saveLayoutSettings();
		}

		public void intervalRemoved( final ListDataEvent e )
		{
			this.saveLayoutSettings();
		}

		public void contentsChanged( final ListDataEvent e )
		{
		}

		public void saveLayoutSettings()
		{
			if ( this.isRefreshing )
			{
				return;
			}

			StringBuffer frameString = new StringBuffer();
			StringBuffer desktopString = new StringBuffer();

			for ( int i = 0; i < this.startupList.getSize(); ++i )
			{
				for ( int j = 0; j < KoLConstants.FRAME_NAMES.length; ++j )
				{
					if ( this.startupList.getElementAt( i ).equals( KoLConstants.FRAME_NAMES[ j ][ 0 ] ) )
					{
						if ( frameString.length() != 0 )
						{
							frameString.append( "," );
						}
						frameString.append( KoLConstants.FRAME_NAMES[ j ][ 1 ] );
					}
				}
			}

			for ( int i = 0; i < this.desktopList.getSize(); ++i )
			{
				for ( int j = 0; j < KoLConstants.FRAME_NAMES.length; ++j )
				{
					if ( this.desktopList.getElementAt( i ).equals( KoLConstants.FRAME_NAMES[ j ][ 0 ] ) )
					{
						if ( desktopString.length() != 0 )
						{
							desktopString.append( "," );
						}
						desktopString.append( KoLConstants.FRAME_NAMES[ j ][ 1 ] );
					}
				}
			}

			Preferences.setString( "initialFrames", frameString.toString() );
			Preferences.setString( "initialDesktop", desktopString.toString() );
		}
	}

	private class DeedsButtonPanel
		extends ScrollablePanel
		implements ListDataListener
	{
		public DeedsButtonPanel( final String title, final LockableListModel builtIns )
		{
			super( title, "add custom", "reset deeds", new JDnDList( builtIns ) );

			this.buttonPanel.add( new ThreadedButton( "help", new HelpRunnable() ), BorderLayout.CENTER );
		}

		public final void actionConfirmed()
		{
			if ( KoLCharacter.baseUserName().equals( "GLOBAL" ) )
			{
				InputFieldUtilities.alert( "You must be logged in to use the custom deed builder." );
			}
			else
			{
				JFrame builderFrame = new JFrame( "Building Custom Deed" );
				AddCustomDeedsPanel deedPanel = new AddCustomDeedsPanel();

				builderFrame.getContentPane().add( AddCustomDeedsPanel.selectorPanel );
				builderFrame.pack();
				builderFrame.setResizable( false );
				builderFrame.setLocationRelativeTo( null );
				builderFrame.setVisible( true );
			}
		}

		public final void actionCancelled()
		{
			boolean reset = InputFieldUtilities
				.confirm( "This will reset your deeds to the default settings.\nAre you sure?" );
			if ( reset )
			{
				Preferences.resetToDefault( "dailyDeedsOptions" );
			}
		}

		public void intervalAdded( final ListDataEvent e )
		{
			this.saveSettings();
		}

		public void intervalRemoved( final ListDataEvent e )
		{
			this.saveSettings();
		}

		public void contentsChanged( final ListDataEvent e )
		{
			this.saveSettings();
		}

		private class HelpRunnable
			implements Runnable
		{
			JOptionPane pane;

			public HelpRunnable()
			{
				String message = "<html><table width=750><tr><td>All deeds are specified by one comma-delimited preference \"dailyDeedsOptions\".  Order matters.  Built-in deeds are simply called by referring to their built-in name; these are viewable by pulling up the Daily Deeds tab and looking in the \"Built-in Deeds\" list."
					+ "<h3><b>Custom Deeds</b></h3>"
					+ "Custom deeds provide the user with a way of adding buttons or text to their daily deeds panel that is not natively provided for.  All deeds start with the keyword <b>$CUSTOM</b> followed by a pipe (|) symbol.  As you are constructing a custom deed, you separate the different arguments with pipes.<br>"
					+ "<br>"
					+ "All deed types except for Text require a preference to track.  If you want to add a button that is always enabled, you will have to create a dummy preference that is always false.<br>"
					+ "<br>"
					+ "There are currently 5 different types of custom deeds.  Remember that all of these \"acceptable forms\" are prefaced by $CUSTOM|.<br>"
					+ "<br>"
					+ "<b>Command</b> - execute a command with a button press<br>"
					+ "acceptable forms:"
					+ "<br>Command|displayText|preference<br>"
					+ "Command|displayText|preference|command<br>"
					+ "Command|displayText|preference|command|maxUses<br>"
					+ "<br>"
					+ "displayText - the text that will be displayed on the button<br>"
					+ "preference - the preference to track.  The button will be enabled when the preference is less than maxUses (default 1).<br>"
					+ "command - the command to execute.  If not specified, will default to displayText.<br>"
					+ "maxUses - an arbitrary integer.  Specifies a threshold to disable the button at.  A counter in the form of &lt;preference&gt;/&lt;maxUses&gt; will be displayed to the right of the button.<br>"
					+ "<br>"
					+ "<b>Item</b> - this button will use fuzzy matching to find the name of the item specified.  Will execute \"use &lt;itemName&gt;\" when clicked.  Will only be visible when you possess one or more of the item.<br>"
					+ "acceptable forms:<br>"
					+ "Item|displayText|preference<br>"
					+ "Item|displayText|preference|itemName<br>"
					+ "Item|displayText|preference|itemName|maxUses<br>"
					+ "<br>"
					+ "itemName - the name of the item that will be used.  If not specified, will default to displayText.<br>"
					+ "<br>"
					+ "<b>Skill</b> - cast a skill that is tracked by a boolean or int preference.  Will execute \"cast &lt;skillName&gt;\" when clicked.  Will not be visible if you don't know the skill.<br>"
					+ "acceptable forms:<br>"
					+ "Skill|displayText|preference<br>"
					+ "Skill|displayText|preference|skillName<br>"
					+ "Skill|displayText|preference|skillName|maxCasts<br>"
					+ "<br>"
					+ "skillName- the name of the skill that will be cast.  If not specified, will default to displayText.  Must be specified if maxCasts are specified.<br>"
					+ "maxCasts - an arbitrary integer.  Specifies a threshold to disable the button at.  A counter in the form of &lt;preference&gt;/&lt;maxCasts&gt; will be displayed to the right of the button.<br>"
					+ "<br>"
					+ "<b>Text</b><br>"
					+ "acceptable forms:<br>"
					+ "Text|pretty much anything.<br>"
					+ "<br>"
					+ "You can supply as many arguments as you want to a Text deed.  Any argument that uniquely matches a preference will be replaced by that preference's value.  If you want to use a comma in your text, immediately follow the comma with a pipe character so it will not be parsed as the end of the Text deed.</td></tr></table></html>";

				JTextPane textPane = new JTextPane();
				textPane.setContentType( "text/html" );
				textPane.setText( message );
				textPane.setOpaque( false );
				textPane.setEditable( false );
				textPane.setSelectionStart( 0 );
				textPane.setSelectionEnd( 0 ); // don't have scrollPane scrolled to the bottom when you open it

				JScrollPane scrollPane = new JScrollPane( textPane );
				scrollPane.setPreferredSize( new Dimension( 800, 550 ) );
				scrollPane.setHorizontalScrollBarPolicy( JScrollPane.HORIZONTAL_SCROLLBAR_NEVER );

				this.pane = new JOptionPane( scrollPane, JOptionPane.PLAIN_MESSAGE );
			}

			public void run()
			{
				JDialog dialog = this.pane.createDialog( null, "Daily Deeds Help" );
				dialog.setModal( false );
				dialog.setVisible( true );
			}
		}

		public void saveSettings()
		{

		}
	}

	protected class CustomizeDailyDeedsPanel
		extends GenericPanel
		implements ListDataListener, PreferenceListener
	{
		private boolean isRefreshing = false;

		private LockableListModel builtInsList = new LockableListModel();
		private LockableListModel deedsList = new LockableListModel();
		private LockableListModel customList = new LockableListModel();

		public CustomizeDailyDeedsPanel()
		{
			super( new Dimension( 2, 2 ), new Dimension( 2, 2 ) );
			this.setContent( null );

			JPanel botPanel = new JPanel( new GridLayout( 1, 0, 10, 0 ) );
			JPanel centerPanel = new JPanel( new GridLayout( 1, 2, 0, 0 ) );

			String[] customs =
			{
				"Boolean Pref", "Multi Pref", "Boolean Item", "Skill", "Text"
			};

			for ( int i = 0; i < DailyDeedsPanel.BUILTIN_DEEDS.length; ++i )
			{
				this.builtInsList.add( DailyDeedsPanel.BUILTIN_DEEDS[ i ][ 1 ] );
			}

			for ( int i = 0; i < customs.length; ++i )
			{
				this.customList.add( customs[ i ] );
			}

			centerPanel.add( new DeedsButtonPanel( "Built-In Deeds", this.builtInsList ) );
			botPanel.add( new ScrollablePanel( "Current Deeds", new JDnDList( this.deedsList ) ) );

			this.container.add( centerPanel, BorderLayout.PAGE_START );
			this.container.add( botPanel, BorderLayout.PAGE_END );
			this.actionCancelled();

			this.builtInsList.addListDataListener( this );
			this.deedsList.addListDataListener( this );
			PreferenceListenerRegistry.registerListener( "dailyDeedsOptions", this );
		}

		public CustomizeDailyDeedsPanel( String string )
		{
			super( new Dimension( 2, 2 ), new Dimension( 2, 2 ) );
			this.setContent( null );

			JTextArea message = new JTextArea(
				"Edit the appearance of your daily deeds panel.\n\n"
				+ "Drag built-in deeds into the 'Current Deeds' box down below to include, "
				+ "and delete them from there to exclude.  Drag and drop to rearrange. "
				+ "Note that some deeds added to the 'Current Deeds' box may still remain hidden "
				+ "once you add them depending on whether you posess certain "
				+ "items, skills, and/or access to zones." );

			message.setColumns( 40 );
			message.setLineWrap( true );
			message.setWrapStyleWord( true );
			message.setEditable( false );
			message.setOpaque( false );
			message.setFont( KoLConstants.DEFAULT_FONT );

			this.container.add( message, BorderLayout.NORTH );
		}

		public void dispose()
		{
			this.builtInsList.removeListDataListener( this );
			this.deedsList.removeListDataListener( this );

			super.dispose();
		}

		public void actionConfirmed()
		{
			this.actionCancelled();
		}

		public void actionCancelled()
		{
			this.isRefreshing = true;
			String deedsString = Preferences.getString( "dailyDeedsOptions" );
			String[] pieces;

			pieces = deedsString.split( ",(?!\\|)" );

			this.deedsList.clear();

			KoLmafiaGUI.checkFrameSettings();

			for ( int i = 0; i < pieces.length; ++i )
			{
				for ( int j = 0; j < DailyDeedsPanel.BUILTIN_DEEDS.length; ++j )
				{
					if ( !this.deedsList.contains( DailyDeedsPanel.BUILTIN_DEEDS[ j ][ 1 ] )
							&& DailyDeedsPanel.BUILTIN_DEEDS[ j ][ 1 ].equals( pieces[ i ] ) )
					{
						this.deedsList.add( DailyDeedsPanel.BUILTIN_DEEDS[ j ][ 1 ] );
					}
				}
				if ( pieces[ i ].split( "\\|" )[ 0 ].equals( "$CUSTOM" ) )
				{
					this.deedsList.add( pieces[ i ] );
				}
			}

			this.isRefreshing = false;
			this.saveLayoutSettings();
		}

		public boolean shouldAddStatusLabel( final VerifiableElement[] elements )
		{
			return false;
		}

		public void setEnabled( final boolean isEnabled )
		{
		}

		public void intervalAdded( final ListDataEvent e )
		{
			Object src = e.getSource();

			if ( src == this.builtInsList )
			{
				Object item = this.builtInsList.get( e.getIndex0() );

				this.deedsList.remove( item );
			}

			this.saveLayoutSettings();
		}

		public void intervalRemoved( final ListDataEvent e )
		{
			this.saveLayoutSettings();
		}

		public void contentsChanged( final ListDataEvent e )
		{
		}

		public void saveLayoutSettings()
		{
			if ( this.isRefreshing )
			{
				return;
			}

			StringBuffer frameString = new StringBuffer();

			for ( int i = 0; i < this.deedsList.getSize(); ++i )
			{
				for ( int j = 0; j < DailyDeedsPanel.BUILTIN_DEEDS.length; ++j )
				{
					if ( this.deedsList.getElementAt( i ).equals(
							DailyDeedsPanel.BUILTIN_DEEDS[ j ][ 1 ] ) )
					{
						if ( frameString.length() != 0 )
						{
							frameString.append( "," );
						}
						frameString.append( DailyDeedsPanel.BUILTIN_DEEDS[ j ][ 1 ] );
					}
				}
				if ( this.deedsList.getElementAt( i ).toString().split( "\\|" )[ 0 ].equals( "$CUSTOM" ) )
				{
					if ( frameString.length() != 0 )
					{
						frameString.append( "," );
					}
					frameString.append( this.deedsList.getElementAt( i ) );
				}
			}

			Preferences.setString( "dailyDeedsOptions", frameString.toString() );
		}

		public void update()
		{
			this.actionCancelled();
		}
	}

	/**
	 * Allows the user to select to select the framing mode to use.
	 */

	protected class UserInterfacePanel
		extends OptionsPanel
	{
		private JCheckBox[] optionBoxes;

		private final String[][] options =

			System.getProperty( "os.name" ).startsWith( "Windows" ) ?

			new String [][]
			{
				{ "guiUsesOneWindow", "Restrict interface to a single window" },
				{ "useSystemTrayIcon", "Minimize main interface to system tray" },
				{ "addCreationQueue", "Add creation queueing interface to item manager" },
				{ "addStatusBarToFrames", "Add a status line to independent windows" },
				{ "autoHighlightOnFocus", "Highlight text fields when selected" },
				{},
				{ "useDecoratedTabs", "Use shiny decorated tabs instead of OS default" },
				{ "allowCloseableDesktopTabs", "Allow tabs on main window to be closed" },
			}

			: System.getProperty( "os.name" ).startsWith( "Mac" ) ?

			new String [][]
			{
				{ "guiUsesOneWindow", "Restrict interface to a single window" },
				{ "useDockIconBadge", "Show turns remaining on Dock icon (OSX 10.5+)" },
				{ "addCreationQueue", "Add creation queueing interface to item manager" },
				{ "addStatusBarToFrames", "Add a status line to independent windows" },
				{ "autoHighlightOnFocus", "Highlight text fields when selected" },
				{},
				{ "useDecoratedTabs", "Use shiny decorated tabs instead of OS default" },
				{ "allowCloseableDesktopTabs", "Allow tabs on main window to be closed" },
			}

			:

			new String [][]
			{
				{ "guiUsesOneWindow", "Restrict interface to a single window" },
				{ "addCreationQueue", "Add creation queueing interface to item manager" },
				{ "addStatusBarToFrames", "Add a status line to independent windows" },
				{ "autoHighlightOnFocus", "Highlight text fields when selected" },
				{},
				{ "useDecoratedTabs", "Use shiny decorated tabs instead of OS default" },
				{ "allowCloseableDesktopTabs", "Allow tabs on main window to be closed" },
			};

		private final JComboBox looks, toolbars, scripts;
		private String defaultLookAndFeel;

		public UserInterfacePanel()
		{
			super( "", new Dimension( 80, 20 ), new Dimension( 280, 20 ) );

			UIManager.LookAndFeelInfo[] installed = UIManager.getInstalledLookAndFeels();
			Object[] installedLooks = new Object[ installed.length + 1 ];

			installedLooks[ 0 ] = "Always use OS default look and feel";

			for ( int i = 0; i < installed.length; ++i )
			{
				installedLooks[ i + 1 ] = installed[ i ].getClassName();
			}

			this.looks = new JComboBox( installedLooks );

			if ( System.getProperty( "os.name" ).startsWith( "Mac" ) || System.getProperty( "os.name" ).startsWith( "Win" ) )
			{
				this.defaultLookAndFeel = UIManager.getSystemLookAndFeelClassName();
			}
			else
			{
				this.defaultLookAndFeel = UIManager.getCrossPlatformLookAndFeelClassName();
			}

			this.toolbars = new JComboBox();
			this.toolbars.addItem( "Show global menus only" );
			this.toolbars.addItem( "Put toolbar along top of panel" );
			this.toolbars.addItem( "Put toolbar along bottom of panel" );
			this.toolbars.addItem( "Put toolbar along left of panel" );

			this.scripts = new JComboBox();
			this.scripts.addItem( "Do not show script bar on main interface" );
			this.scripts.addItem( "Put script bar after normal toolbar" );
			this.scripts.addItem( "Put script bar along right of panel" );

			VerifiableElement[] elements = new VerifiableElement[ 3 ];

			elements[ 0 ] = new VerifiableElement( "Java L&F: ", this.looks );
			elements[ 1 ] = new VerifiableElement( "Toolbar: ", this.toolbars );
			elements[ 2 ] = new VerifiableElement( "Scripts: ", this.scripts );

			this.actionCancelled();
			this.setContent( elements );
		}

		public boolean shouldAddStatusLabel( final VerifiableElement[] elements )
		{
			return false;
		}

		public void setContent( final VerifiableElement[] elements )
		{
			super.setContent( elements );
			this.add( new InterfaceCheckboxPanel(), BorderLayout.CENTER );
		}

		public void setEnabled( final boolean isEnabled )
		{
		}

		public void actionConfirmed()
		{
			String lookAndFeel = "";

			if ( this.looks.getSelectedIndex() > 0 )
			{
				lookAndFeel = (String) this.looks.getSelectedItem();
			}

			Preferences.setString( "swingLookAndFeel", lookAndFeel );

			Preferences.setBoolean( "useToolbars", this.toolbars.getSelectedIndex() != 0 );
			Preferences.setInteger( "scriptButtonPosition", this.scripts.getSelectedIndex() );
			Preferences.setInteger( "toolbarPosition", this.toolbars.getSelectedIndex() );
		}

		public void actionCancelled()
		{
			String lookAndFeel = Preferences.getString( "swingLookAndFeel" );

			if ( lookAndFeel.equals( "" ) )
			{
				this.looks.setSelectedIndex( 0 );
			}
			else
			{
				this.looks.setSelectedItem( lookAndFeel );
			}

			this.toolbars.setSelectedIndex( Preferences.getInteger( "toolbarPosition" ) );
			this.scripts.setSelectedIndex( Preferences.getInteger( "scriptButtonPosition" ) );
		}

		private class InterfaceCheckboxPanel
			extends OptionsPanel
		{
			private final JLabel innerGradient, outerGradient;

			public InterfaceCheckboxPanel()
			{
				super( new Dimension( 20, 16 ), new Dimension( 370, 16 ) );
				VerifiableElement[] elements = new VerifiableElement[ UserInterfacePanel.this.options.length + 3 ];

				UserInterfacePanel.this.optionBoxes = new JCheckBox[ UserInterfacePanel.this.options.length ];

				for ( int i = 0; i < UserInterfacePanel.this.options.length; ++i )
				{
					String[] option = UserInterfacePanel.this.options[ i ];
					JCheckBox optionBox = new JCheckBox();
					UserInterfacePanel.this.optionBoxes[ i ] = optionBox;
					elements[ i ] =
						option.length == 0 ?
						new VerifiableElement() :
						new VerifiableElement( option[ 1 ], JLabel.LEFT, optionBox );
				}

				elements[ UserInterfacePanel.this.options.length ] = new VerifiableElement();

				this.outerGradient = new TabColorChanger( "outerTabColor" );
				elements[ UserInterfacePanel.this.options.length + 1 ] =
					new VerifiableElement(
						"Change the outer portion of the tab gradient (shiny tabs)", JLabel.LEFT, this.outerGradient );

				this.innerGradient = new TabColorChanger( "innerTabColor" );
				elements[ UserInterfacePanel.this.options.length + 2 ] =
					new VerifiableElement(
						"Change the inner portion of the tab gradient (shiny tabs)", JLabel.LEFT, this.innerGradient );

				this.actionCancelled();
				this.setContent( elements );
			}

			public void actionConfirmed()
			{
				for ( int i = 0; i < UserInterfacePanel.this.options.length; ++i )
				{
					String[] option = UserInterfacePanel.this.options[ i ];
					if ( option.length == 0 )
					{
						continue;
					}
					JCheckBox optionBox = UserInterfacePanel.this.optionBoxes[ i ];
					Preferences.setBoolean( option[ 0 ], optionBox.isSelected() );
				}
			}

			public void actionCancelled()
			{
				for ( int i = 0; i < UserInterfacePanel.this.options.length; ++i )
				{
					String[] option = UserInterfacePanel.this.options[ i ];
					if ( option.length == 0 )
					{
						continue;
					}
					JCheckBox optionBox = UserInterfacePanel.this.optionBoxes[ i ];
					optionBox.setSelected( Preferences.getBoolean( option[ 0 ] ) );
				}

				this.innerGradient.setBackground( tab.CloseTabPaneEnhancedUI.selectedA );
				this.outerGradient.setBackground( tab.CloseTabPaneEnhancedUI.selectedB );
			}

			public void setEnabled( final boolean isEnabled )
			{
			}

			private class TabColorChanger
				extends ColorChooser
			{
				public TabColorChanger( final String property )
				{
					super( property );
				}

				public void applyChanges()
				{
					if ( this.property.equals( "innerTabColor" ) )
					{
						CloseTabPaneEnhancedUI.selectedA = InterfaceCheckboxPanel.this.innerGradient.getBackground();
					}
					else
					{
						CloseTabPaneEnhancedUI.selectedB = InterfaceCheckboxPanel.this.outerGradient.getBackground();
					}

					OptionsFrame.this.tabs.repaint();
				}
			}
		}
	}

	protected class BrowserPanel
		extends OptionsPanel
	{
		private final FileSelectPanel preferredWebBrowser;

		public BrowserPanel()
		{
			super( "Preferred Web Browser" );

			AutoHighlightTextField textField = new AutoHighlightTextField();
			boolean button = true;
			String helpText = "";
			String path = null;

			if ( UtilityConstants.USE_OSX_STYLE_DIRECTORIES )
			{
				button = false;
				path = "/Applications";
				helpText = "If KoLmafia opens a browser other than your default, enter the name of your preferred browser here. The browser must be in your Applications directory";
			}
			else if ( UtilityConstants.USE_LINUX_STYLE_DIRECTORIES )
			{
				button = true;
				path = "/";
				helpText = "If KoLmafia opens a browser other than your default, enter the name of your preferred browser here. If that doesn't work, click the button and browse to the location of your browser.";
			}
			else	// Windows
			{
				button = true;
				path = "";
				helpText = "If KoLmafia opens a browser other than your default, enter the name of your preferred browser here. If that doesn't work, click the button and browse to the location of your browser.";
			}

			this.preferredWebBrowser = new FileSelectPanel( textField, button );
			if ( button )
			{
				this.preferredWebBrowser.setPath( new File( path ) );
			}

			VerifiableElement[] elements = new VerifiableElement[ 1 ];
			elements[ 0 ] = new VerifiableElement( "Browser: ", this.preferredWebBrowser );

			this.setContent( elements );

			JTextArea message = new JTextArea( helpText );
			message.setColumns( 40 );
			message.setLineWrap( true );
			message.setWrapStyleWord( true );
			message.setEditable( false );
			message.setOpaque( false );
			message.setFont( KoLConstants.DEFAULT_FONT );

			this.container.add( message, BorderLayout.SOUTH );

			this.actionCancelled();
		}

		public void actionConfirmed()
		{
			Preferences.setString( "preferredWebBrowser", this.preferredWebBrowser.getText() );
		}

		public void actionCancelled()
		{
			this.preferredWebBrowser.setText( Preferences.getString( "preferredWebBrowser" ) );
		}
	}

	protected class EditorPanel
		extends OptionsPanel
	{
		private final FileSelectPanel preferredEditor;

		public EditorPanel()
		{
			super( "External Script Editor" );

			AutoHighlightTextField textField = new AutoHighlightTextField();
			boolean button = true;
			String helpText = "";
			String path = null;

			{
				button = false;
				path = "";
				helpText = "The command will be invoked with the full path to the script as its only parameter.";
			}

			this.preferredEditor = new FileSelectPanel( textField, button );
			if ( button )
			{
				this.preferredEditor.setPath( new File( path ) );
			}

			VerifiableElement[] elements = new VerifiableElement[ 1 ];
			elements[ 0 ] = new VerifiableElement( "Editor command: ", this.preferredEditor );

			this.setContent( elements );

			JTextArea message = new JTextArea( helpText );
			message.setColumns( 40 );
			message.setLineWrap( true );
			message.setWrapStyleWord( true );
			message.setEditable( false );
			message.setOpaque( false );
			message.setFont( KoLConstants.DEFAULT_FONT );

			this.container.add( message, BorderLayout.SOUTH );

			this.actionCancelled();
		}

		public void actionConfirmed()
		{
			Preferences.setString( "externalEditor", this.preferredEditor.getText() );
		}

		public void actionCancelled()
		{
			this.preferredEditor.setText( Preferences.getString( "externalEditor" ) );
		}
	}

	protected class ScriptPanel
		extends OptionsPanel
	{
		private final ScriptSelectPanel loginScript;
		private final ScriptSelectPanel logoutScript;

		public ScriptPanel()
		{
			super( "Miscellaneous Scripts" );

			this.loginScript = new ScriptSelectPanel( new AutoHighlightTextField() );
			this.logoutScript = new ScriptSelectPanel( new AutoHighlightTextField() );

			VerifiableElement[] elements = new VerifiableElement[ 3 ];
			elements[ 0 ] = new VerifiableElement( "On Login: ", this.loginScript );
			elements[ 1 ] = new VerifiableElement( "On Logout: ", this.logoutScript );
			elements[ 2 ] = new VerifiableElement();

			this.setContent( elements );
			this.actionCancelled();
		}

		public void actionConfirmed()
		{
			Preferences.setString( "loginScript", this.loginScript.getText() );
			Preferences.setString( "logoutScript", this.logoutScript.getText() );
		}

		public void actionCancelled()
		{
			String loginScript = Preferences.getString( "loginScript" );
			this.loginScript.setText( loginScript );

			String logoutScript = Preferences.getString( "logoutScript" );
			this.logoutScript.setText( logoutScript );
		}

	}

	protected class BreakfastPanel
		extends JPanel
		implements ActionListener
	{
		private final String breakfastType;
		private final JCheckBox[] skillOptions;

		private final JCheckBox loginRecovery;
		private final JCheckBox pathedSummons;
		private final JCheckBox rumpusRoom;
		private final JCheckBox clanLounge;

		private final JCheckBox mushroomPlot;
		private final JCheckBox grabClovers;
		private final JCheckBox readManual;
		private final JCheckBox useCrimboToys;

		private final SkillMenu tomeSkills;
		private final SkillMenu libramSkills;
		private final SkillMenu grimoireSkills;

		private final CropMenu	cropsMenu;

		public BreakfastPanel( final String title, final String breakfastType )
		{
			super( new BorderLayout() );

			this.add(
				JComponentUtilities.createLabel( title, JLabel.CENTER, Color.black, Color.white ), BorderLayout.NORTH );

			JPanel centerPanel = new JPanel( new GridLayout( 4, 3 ) );

			this.loginRecovery = new JCheckBox( "enable auto-recovery" );
			this.loginRecovery.addActionListener( this );
			centerPanel.add( this.loginRecovery );

			this.pathedSummons = new JCheckBox( "honor path restrictions" );
			this.pathedSummons.addActionListener( this );
			centerPanel.add( this.pathedSummons );

			this.rumpusRoom = new JCheckBox( "visit clan rumpus room" );
			this.rumpusRoom.addActionListener( this );
			centerPanel.add( this.rumpusRoom );

			this.clanLounge = new JCheckBox( "visit clan VIP lounge" );
			this.clanLounge.addActionListener( this );
			centerPanel.add( this.clanLounge );

			this.breakfastType = breakfastType;
			this.skillOptions = new JCheckBox[ UseSkillRequest.BREAKFAST_SKILLS.length ];
			for ( int i = 0; i < UseSkillRequest.BREAKFAST_SKILLS.length; ++i )
			{
				this.skillOptions[ i ] = new JCheckBox( UseSkillRequest.BREAKFAST_SKILLS[ i ].toLowerCase() );
				this.skillOptions[ i ].addActionListener( this );
				centerPanel.add( this.skillOptions[ i ] );
			}

			this.mushroomPlot = new JCheckBox( "plant mushrooms" );
			this.mushroomPlot.addActionListener( this );
			centerPanel.add( this.mushroomPlot );

			this.grabClovers = new JCheckBox( "get hermit clovers" );
			this.grabClovers.addActionListener( this );
			centerPanel.add( this.grabClovers );

			this.readManual = new JCheckBox( "read guild manual" );
			this.readManual.addActionListener( this );
			centerPanel.add( this.readManual );

			this.useCrimboToys = new JCheckBox( "use once-a-day items" );
			this.useCrimboToys.addActionListener( this );
			centerPanel.add( this.useCrimboToys );

			JPanel centerHolder = new JPanel( new BorderLayout() );
			centerHolder.add( centerPanel, BorderLayout.NORTH );

			centerPanel = new JPanel( new GridLayout( 1, 3 ) );

			this.tomeSkills = new SkillMenu( "Tome Skills", UseSkillRequest.TOME_SKILLS, "tomeSkills" + this.breakfastType );
			this.tomeSkills.addActionListener( this );
			centerPanel.add( this.tomeSkills );

			this.libramSkills = new SkillMenu( "Libram Skills", UseSkillRequest.LIBRAM_SKILLS, "libramSkills" + this.breakfastType );
			this.libramSkills.addActionListener( this );
			centerPanel.add( this.libramSkills );

			this.grimoireSkills = new SkillMenu( "Grimoire Skills", UseSkillRequest.GRIMOIRE_SKILLS, "grimoireSkills" + this.breakfastType );
			this.grimoireSkills.addActionListener( this );
			centerPanel.add( this.grimoireSkills );

			centerHolder.add( centerPanel, BorderLayout.CENTER );

			centerPanel = new JPanel( new GridLayout( 1, 3 ) );
			this.cropsMenu = new CropMenu( CampgroundRequest.CROPS, "harvestGarden" + this.breakfastType );
			this.cropsMenu.addActionListener( this );
			centerPanel.add( new JLabel() );
			centerPanel.add( this.cropsMenu );
			centerPanel.add( new JLabel() );

			centerHolder.add( centerPanel, BorderLayout.SOUTH );

			JPanel centerContainer = new JPanel( new CardLayout( 10, 10 ) );
			centerContainer.add( centerHolder, "" );

			this.add( centerContainer, BorderLayout.CENTER );

			this.actionCancelled();
		}

		public void actionPerformed( final ActionEvent e )
		{
			this.actionConfirmed();
		}

		public void actionConfirmed()
		{
			StringBuffer skillString = new StringBuffer();

			for ( int i = 0; i < UseSkillRequest.BREAKFAST_SKILLS.length; ++i )
			{
				if ( this.skillOptions[ i ].isSelected() )
				{
					if ( skillString.length() != 0 )
					{
						skillString.append( "," );
					}

					skillString.append( UseSkillRequest.BREAKFAST_SKILLS[ i ] );
				}
			}

			Preferences.setString( "breakfast" + this.breakfastType, skillString.toString() );
			Preferences.setBoolean(
				"loginRecovery" + this.breakfastType, this.loginRecovery.isSelected() );
			Preferences.setBoolean(
				"pathedSummons" + this.breakfastType, this.pathedSummons.isSelected() );
			Preferences.setBoolean(
				"visitRumpus" + this.breakfastType, this.rumpusRoom.isSelected() );
			Preferences.setBoolean(
				"visitLounge" + this.breakfastType, this.clanLounge.isSelected() );
			Preferences.setBoolean(
				"autoPlant" + this.breakfastType, this.mushroomPlot.isSelected() );
			Preferences.setBoolean(
				"grabClovers" + this.breakfastType, this.grabClovers.isSelected() );
			Preferences.setBoolean(
				"readManual" + this.breakfastType, this.readManual.isSelected() );
			Preferences.setBoolean(
				"useCrimboToys" + this.breakfastType, this.useCrimboToys.isSelected() );

			this.tomeSkills.setPreference();
			this.libramSkills.setPreference();
			this.grimoireSkills.setPreference();
			this.cropsMenu.setPreference();
		}

		public void actionCancelled()
		{
			String skillString = Preferences.getString( "breakfast" + this.breakfastType );
			for ( int i = 0; i < UseSkillRequest.BREAKFAST_SKILLS.length; ++i )
			{
				this.skillOptions[ i ].setSelected( skillString.indexOf( UseSkillRequest.BREAKFAST_SKILLS[ i ] ) != -1 );
			}

			this.loginRecovery.setSelected( Preferences.getBoolean( "loginRecovery" + this.breakfastType ) );
			this.pathedSummons.setSelected( Preferences.getBoolean( "pathedSummons" + this.breakfastType ) );
			this.rumpusRoom.setSelected( Preferences.getBoolean( "visitRumpus" + this.breakfastType ) );
			this.clanLounge.setSelected( Preferences.getBoolean( "visitLounge" + this.breakfastType ) );
			this.mushroomPlot.setSelected( Preferences.getBoolean( "autoPlant" + this.breakfastType ) );
			this.grabClovers.setSelected( Preferences.getBoolean( "grabClovers" + this.breakfastType ) );
			this.readManual.setSelected( Preferences.getBoolean( "readManual" + this.breakfastType ) );
			this.useCrimboToys.setSelected( Preferences.getBoolean( "useCrimboToys" + this.breakfastType ) );
		}

		public void setEnabled( final boolean isEnabled )
		{
		}
	}

	private class SkillMenu
		extends JComboBox
	{
		final String preference;

		public SkillMenu( final String name, final String[] skills, final String preference )
		{
			super();
			this.addItem( "No " + name );
			this.addItem( "All " + name );
			for ( int i = 0; i < skills.length; ++ i )
			{
				this.addItem( skills[i] );
			}

			this.preference = preference;
			this.getPreference();
		}

		public void getPreference()
		{
			String skill = Preferences.getString( this.preference );
			if ( skill.equals( "none" ) )
			{
				this.setSelectedIndex( 0 );
			}
			else if ( skill.equals( "all" ) )
			{
				this.setSelectedIndex( 1 );
			}
			else
			{
				this.setSelectedItem( skill );
			}

			if ( this.getSelectedIndex() < 0 )
			{
				this.setSelectedIndex( 0 );
			}
		}

		public void setPreference()
		{
			String skill = null;
			int index = this.getSelectedIndex();
			switch ( index )
			{
			case -1:
			case 0:
				skill = "none";
				break;
			case 1:
				skill = "all";
				break;
			default:
				skill = (String) this.getItemAt( index );
				break;
			}
			Preferences.setString( this.preference, skill );
		}
	}

	private class CropMenu
		extends JComboBox
	{
		final String preference;

		public CropMenu( final AdventureResult[] crops, final String preference )
		{
			super();
			this.addItem( "Harvest Nothing" );
			for ( int i = 0; i < crops.length; ++ i )
			{
				this.addItem( crops[i].getName() );
			}

			this.preference = preference;
			this.getPreference();
		}

		public void getPreference()
		{
			String crop = Preferences.getString( this.preference );
			if ( crop.equals( "none" ) )
			{
				this.setSelectedIndex( 0 );
			}
			else
			{
				this.setSelectedItem( crop );
			}

			if ( this.getSelectedIndex() < 0 )
			{
				this.setSelectedIndex( 0 );
			}
		}

		public void setPreference()
		{
			String crop = null;
			int index = this.getSelectedIndex();
			switch ( index )
			{
			case -1:
			case 0:
				crop = "none";
				break;
			default:
				crop = (String) this.getItemAt( index );
				break;
			}
			Preferences.setString( this.preference, crop );
		}
	}
}
