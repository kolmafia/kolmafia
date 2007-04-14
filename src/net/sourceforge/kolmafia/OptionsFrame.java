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

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JColorChooser;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.ListModel;
import javax.swing.ListSelectionModel;

import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import tab.CloseTabbedPane;
import tab.CloseTabPaneEnhancedUI;

import net.java.dev.spellcast.utilities.DataUtilities;
import net.java.dev.spellcast.utilities.LockableListModel;

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

		JPanel addonPanel = new JPanel( new GridLayout( 2, 1, 10, 10 ) );
		addonPanel.add( new ScriptButtonPanel() );
		addonPanel.add( new BookmarkManagePanel() );

		addTab( "General", new GeneralOptionsPanel() );
		addTab( "Relay", new RelayOptionsPanel() );

		addTab( "Links", addonPanel );
		addTab( "Logs", new SessionLogOptionsPanel() );
		addTab( "Chat", new ChatOptionsPanel() );

		framePanel.setLayout( new CardLayout( 10, 10 ) );
		framePanel.add( tabs, "" );

		boolean anyFrameVisible = false;
		for ( int i = 0; i < existingFrames.size(); ++i )
			anyFrameVisible |= ((KoLFrame)existingFrames.get(i)).isVisible();

		if ( !anyFrameVisible )
			tabs.setSelectedIndex(1);
		else if ( KoLDesktop.instanceExists() )
			tabs.setSelectedIndex(2);
		else
			tabs.setSelectedIndex(0);
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
			VerifiableElement [] elements = new VerifiableElement[ options.length ];

			optionBoxes = new JCheckBox[ options.length ];
			for ( int i = 0; i < options.length; ++i )
				optionBoxes[i] = new JCheckBox();

			for ( int i = 0; i < options.length; ++i )
			{
				if ( options[i].length == 0 )
					elements[i] = new VerifiableElement();
				else
					elements[i] = new VerifiableElement( options[i][1], JLabel.LEFT, optionBoxes[i] );
			}

			actionCancelled();
			setContent( elements );
		}

		public void actionConfirmed()
		{
			for ( int i = 0; i < options.length; ++i )
				if ( options[i].length != 0 )
					StaticEntity.setProperty( options[i][0], String.valueOf( optionBoxes[i].isSelected() ) );
		}

		public void actionCancelled()
		{
			for ( int i = 0; i < options.length; ++i )
				if ( options[i].length != 0 )
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
			{ "relayTextualizesEffects", "Textualize effect links in left side pane" },

			{ "", "" },

			{ "relayAddsMissingEffects", "Display mood triggers with zero duration" },
			{ "relayAddsMoodRefreshLink", "Add mood links (overrides MP burn links)" },

			{ "", "" },

			{ "relayMaintainsMoods", "Maintain moods during manual adventuring" },
			{ "relayMaintainsHealth", "Maintain health during manual adventuring" },
			{ "relayMaintainsMana", "Maintain mana during manual adventuring" },

			{ "", "" },

			{ "relayAddsQuickScripts", "Add quick script links to menu bar (see Links tab)" },
			{ "relayUsesIntegratedChat", "Integrate chat and relay browser gCLI interfaces" },
			{ "relayFormatsChatText", "Reformat incoming chat HTML to conform to web standards" },

			{ "relayAddsGraphicalCLI", "Add link to command-line interface to right side pane" },
			{ "relayAddsKoLSimulator", "Add link to Ayvuir's Simulator of Loathing to right side pane" },

			{ "", "" },

			{ "relayAddsUseLinks", "Add decorator [use] links when receiving items" },
			{ "relayUsesInlineLinks", "Force results to reload inline for [use] links" },
			{ "relayHidesJunkMallItems", "Hide junk and overpriced items in PC stores" },

			{ "", "" },

			{ "relayAddsRoundNumber", "Add current round number to fight pages" },
			{ "relayAddsCustomCombat", "Add custom combat button to fight pages" },
			{ "relayAddsBossReminders", "Always add mind-control reminder for boss fights" },
			{ "relayViewsCustomItems", "View items registered with OneTonTomato's Teh Kilt script" },

			{ "", "" },

			{ "relayAlwaysBuysGum", "Automatically buy gum when visiting the sewer" },
			{ "relayUsesCachedImages", "Cache KoL images to conserve local bandwidth (extremely slow)" }
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

			colorChanger = new LabelColorChanger( "defaultBorderColor" );
			elements[ options.length ] = new VerifiableElement( "Change the color for tables in the browser interface",
				JLabel.LEFT, colorChanger );

			setContent( elements );
			actionCancelled();
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
			{ "showAllRequests", "Show all requests in a mini-browser window" },
			{ "allowRequestQueueing", "Enable request queueing (may cause hanging)" },
			{ "useLowBandwidthRadio", "Use lower bandwidth server for KoL Radio" },

			{ "", "" },

			{ "avoidInvertingTabs", "Do not invert nested tabs in main window" },
			{ "mapLoadsMiniBrowser", "Map button loads mini browser instead of relay browser" },
			{ "sortAdventures", "Sort adventure lists by moxie evade rating" },
			{ "cacheMallSearches", "Cache mall searches (does not work on all OSes)" },

			{ "", "" },

			{ "cloverProtectActive", "Protect against automated clover adventures" },
			{ "mementoListActive", "Prevent accidental destruction of 'memento' items" },
			{ "allowGenericUse", "Enable generic item usage in scripted \"use\"" },

			{ "", "" },

			{ "useFastOutfitSwitch", "Use fast outfit switching instead of piecewise switching" },
			{ "switchEquipmentForBuffs", "Allow equipment changing when casting buffs" },
			{ "allowEncounterRateBurning", "Cast combat rate modifiers during conditional recast" },
			{ "allowBreakfastBurning", "Cast breakfast skills during conditional recast" },
			{ "allowNonMoodBurning", "Cast buffs not defined in moods during conditional recast" },

			{ "", "" },

			{ "autoRepairBoxes", "Automatically repair innaboxes on explosion" },
			{ "createWithoutBoxServants", "Allow item creation without innaboxes" },
			{ "assumeInfiniteNPCItems", "Assume NPC items are used in item creation" },

			{ "", "" },

			{ "autoBuyRestores", "Automatically buy more hp/mp restores when needed" },
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
			super( "General Options (May Require Restart to Take Effect!)", new Dimension( 20, 16 ), new Dimension( 370, 16 ) );
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
				if ( !options[i][0].equals( "" ) )
					StaticEntity.setProperty( options[i][0], String.valueOf( optionBoxes[i].isSelected() ) );

			actionCancelled();
			System.setProperty( "spellcast.actionButtonsThreaded", StaticEntity.getProperty( "allowRequestQueueing" ) );
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
		private JCheckBox useLargeFontSize;
		private JCheckBox useTabOption;
		private JCheckBox popupWhoOption;
		private JCheckBox chatLogsEnabled;

		private JCheckBox useChatMonitor, useSeparateChannel, useSeparatePrivate, greenScreenProtection;
		private JCheckBox eSoluActiveOption, eSoluColorlessOption;

		private JLabel innerGradient, outerGradient;

		public ChatOptionsPanel()
		{
			super( new Dimension( 20, 16 ), new Dimension( 370, 16 ) );

			useLargeFontSize = new JCheckBox();
			useTabOption = new JCheckBox();
			popupWhoOption = new JCheckBox();

			useChatMonitor = new JCheckBox();
			useSeparateChannel = new JCheckBox();
			useSeparatePrivate = new JCheckBox();
			greenScreenProtection = new JCheckBox();
			chatLogsEnabled = new JCheckBox();

			eSoluActiveOption = new JCheckBox();
			eSoluColorlessOption = new JCheckBox();

			VerifiableElement [] elements = new VerifiableElement[16];

			elements[0] = new VerifiableElement( "Use larger font size for HTML displays", JLabel.LEFT, useLargeFontSize );
			elements[1] = new VerifiableElement( "Use tabbed, rather than multi-window, chat", JLabel.LEFT, useTabOption );
			elements[2] = new VerifiableElement( "Use a popup window for /friends and /who", JLabel.LEFT, popupWhoOption );

			elements[3] = new VerifiableElement();
			elements[4] = new VerifiableElement( "Ignore all event messages in KoLmafia chat", JLabel.LEFT, greenScreenProtection );
			elements[5] = new VerifiableElement( "Log chats when using KoLmafia (requires restart)", JLabel.LEFT, chatLogsEnabled );

			elements[6] = new VerifiableElement();

			elements[7] = new VerifiableElement( "Add an \"as KoL would show it\" display", JLabel.LEFT, useChatMonitor );
			elements[8] = new VerifiableElement( "Put different channels into separate displays", JLabel.LEFT, useSeparateChannel );
			elements[9] = new VerifiableElement( "Put different private messages into separate displays", JLabel.LEFT, useSeparatePrivate );

			elements[10] = new VerifiableElement();

			elements[11] = new VerifiableElement( "Activate eSolu scriptlet for KoLmafia chat", JLabel.LEFT, eSoluActiveOption );
			elements[12] = new VerifiableElement( "Switch eSolu scriptlet to colorless mode", JLabel.LEFT, eSoluColorlessOption );

			elements[13] = new VerifiableElement();

			outerGradient = new TabColorChanger( "outerChatColor" );
			elements[14] = new VerifiableElement( "Change the outer portion of highlighted tab gradient",
				JLabel.LEFT, outerGradient );

			innerGradient = new TabColorChanger( "innerChatColor" );
			elements[15] = new VerifiableElement( "Change the inner portion of highlighted tab gradient",
				JLabel.LEFT, innerGradient );

			setContent( elements );
			actionCancelled();
		}

		public void actionConfirmed()
		{
			StaticEntity.setProperty( "useLargerFonts", String.valueOf( useLargeFontSize.isSelected() ) );
			StaticEntity.setProperty( "logChatMessages", String.valueOf( chatLogsEnabled.isSelected() ) );

			if ( useLargeFontSize.isSelected() )
				LimitedSizeChatBuffer.useLargerFonts();
			else
				LimitedSizeChatBuffer.useSmallerFonts();

			StaticEntity.setProperty( "useTabbedChatFrame", String.valueOf( useTabOption.isSelected() ) );
			StaticEntity.setProperty( "useContactsFrame", String.valueOf( popupWhoOption.isSelected() ) );

			StaticEntity.setProperty( "useChatMonitor", String.valueOf( useChatMonitor.isSelected() ) );
			StaticEntity.setProperty( "useSeparateChannels", String.valueOf( useSeparateChannel.isSelected() ) );
			StaticEntity.setProperty( "useSeparatePrivates", String.valueOf( useSeparatePrivate.isSelected() ) );

			StaticEntity.setProperty( "eSoluScriptType", eSoluActiveOption.isSelected() ?
				(eSoluColorlessOption.isSelected() ? "2" : "1") : "0" );

			StaticEntity.setProperty( "greenScreenProtection", String.valueOf( greenScreenProtection.isSelected() ) );

			super.actionConfirmed();
		}

		public void actionCancelled()
		{
			useLargeFontSize.setSelected( StaticEntity.getBooleanProperty( "useLargerFonts" ) );
			chatLogsEnabled.setSelected( StaticEntity.getBooleanProperty( "logChatMessages" ) );

			useTabOption.setSelected( StaticEntity.getBooleanProperty( "useTabbedChatFrame" ) );
			popupWhoOption.setSelected( StaticEntity.getBooleanProperty( "useContactsFrame" ) );
			greenScreenProtection.setSelected( StaticEntity.getBooleanProperty( "greenScreenProtection" ) );

			int chatStyle = StaticEntity.getIntegerProperty( "chatStyle" );
			useChatMonitor.setSelected( StaticEntity.getBooleanProperty( "useChatMonitor" ) );
			useSeparateChannel.setSelected( StaticEntity.getBooleanProperty( "useSeparateChannels" ) );
			useSeparatePrivate.setSelected( StaticEntity.getBooleanProperty( "useSeparatePrivates" ) );

			eSoluActiveOption.setSelected( StaticEntity.getIntegerProperty( "eSoluScriptType" ) > 0 );
			eSoluColorlessOption.setSelected( StaticEntity.getIntegerProperty( "eSoluScriptType" ) > 1 );

			innerGradient.setBackground( tab.CloseTabPaneEnhancedUI.notifiedA );
			outerGradient.setBackground( tab.CloseTabPaneEnhancedUI.notifiedB );
		}

		private class TabColorChanger extends LabelColorChanger
		{
			public TabColorChanger( String property )
			{	super( property );
			}

			public void applyChanges()
			{
				if ( property.equals( "innerChatColor" ) )
					CloseTabPaneEnhancedUI.notifiedA = innerGradient.getBackground();
				else
					CloseTabPaneEnhancedUI.notifiedB = outerGradient.getBackground();
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
}
