/**
 * Copyright (c) 2003, Spellcast development team
 * http://spellcast.dev.java.net/
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
 *  [3] Neither the name "Spellcast development team" nor the names of
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
import java.awt.Color;
import java.awt.Dimension;
import java.awt.CardLayout;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import javax.swing.BoxLayout;

// events
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.SwingUtilities;

// containers
import javax.swing.JComponent;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.JPasswordField;
import javax.swing.JComboBox;
import javax.swing.JCheckBox;
import javax.swing.JTabbedPane;
import javax.swing.JMenuBar;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JFileChooser;
import javax.swing.JRadioButton;
import javax.swing.ButtonGroup;
import javax.swing.JColorChooser;
import javax.swing.JScrollPane;
import javax.swing.JOptionPane;
import javax.swing.AbstractButton;
import javax.swing.Box;

// utilities
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.StringTokenizer;

import net.java.dev.spellcast.utilities.LockableListModel;
import net.java.dev.spellcast.utilities.DataUtilities;
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
	private CardLayout optionCards;

	/**
	 * Constructs a new <code>OptionsFrame</code> that will be
	 * associated with the given client.  When this frame is
	 * closed, it will attempt to return focus to the currently
	 * active frame; note that if this is done while the client
	 * is shuffling active frames, closing the window will not
	 * properly transfer focus.
	 *
	 * @param	client	The client to be associated with this <code>OptionsFrame</code>
	 */

	public OptionsFrame( KoLmafia client )
	{
		super( client, "Preferences" );

		setResizable( false );

		optionCards = new CardLayout( 10, 10 );
		getContentPane().setLayout( optionCards );

		JMenuBar menuBar = new JMenuBar();
		this.setJMenuBar( menuBar );

		int cardCount = 0;

		JMenu loginMenu = new JMenu( "Login" );
		menuBar.add( loginMenu );

		addCard( loginMenu, "Startup Options", new StartupOptionsPanel(), String.valueOf( cardCount++ ) );

		JPanel connectPanel = new JPanel();
		connectPanel.setLayout( new BoxLayout( connectPanel, BoxLayout.Y_AXIS ) );
		connectPanel.add( new ServerSelectPanel() );
		connectPanel.add( new ProxyOptionsPanel() );

		addCard( loginMenu, "Connecting to KoL", connectPanel, String.valueOf( cardCount++ ) );

		JMenu adventureMenu = new JMenu( "Advs" );
		menuBar.add( adventureMenu );

		addCard( adventureMenu, "Adventure List", new AdventureOptionsPanel(), String.valueOf( cardCount++ ) );
		addCard( adventureMenu, "Combat Options", new BattleOptionsPanel(), String.valueOf( cardCount++ ) );
		addCard( adventureMenu, "Choice Adventures", new ChoiceOptionsPanel(), String.valueOf( cardCount++ ) );

		JMenu peopleMenu = new JMenu( "People" );
		menuBar.add( peopleMenu );

		addCard( peopleMenu, "Chat Preferences", new ChatOptionsPanel(), String.valueOf( cardCount++ ) );
		addCard( peopleMenu, "Green Messages", new GreenOptionsPanel(), String.valueOf( cardCount++ ) );
		addCard( peopleMenu, "Clan Snapshots", new SnapshotOptionsPanel(), String.valueOf( cardCount++ ) );

		JMenu itemMenu = new JMenu( "Misc" );
		menuBar.add( itemMenu );

		addCard( itemMenu, "Mall Search", new MallOptionsPanel(), String.valueOf( cardCount++ ) );
		addCard( itemMenu, "Item Creation", new CreationOptionsPanel(), String.valueOf( cardCount++ ) );
	}

	private void addCard( JMenu menu, String name, JComponent panel, String cardID )
	{
		JScrollPane scroller = new JScrollPane( panel, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER );
		JComponentUtilities.setComponentSize( scroller, 520, 400 );

		getContentPane().add( scroller, cardID );
		menu.add( new ShowCardMenuItem( name, cardID ) );
	}

	public boolean isEnabled()
	{	return true;
	}

	private class ShowCardMenuItem extends JMenuItem implements ActionListener
	{
		private String cardID;

		public ShowCardMenuItem( String title, String cardID )
		{
			super( title );
			addActionListener( this );
			this.cardID = cardID;
		}

		public void actionPerformed( ActionEvent e )
		{	optionCards.show( getContentPane(), cardID );
		}
	}

	private class ServerSelectPanel extends OptionsPanel
	{
		private static final int SERVER_COUNT = 3;
		private JRadioButton [] servers;

		public ServerSelectPanel()
		{
			super( "Server Select", new Dimension( 340, 16 ), new Dimension( 20, 16 ) );
			VerifiableElement [] elements = new VerifiableElement[ SERVER_COUNT + 1 ];

			servers = new JRadioButton[ SERVER_COUNT + 1 ];
			ButtonGroup serverGroup = new ButtonGroup();
			for ( int i = 0; i <= SERVER_COUNT; ++i )
			{
				servers[i] = new JRadioButton();
				serverGroup.add( servers[i] );
			}

			elements[0] = new VerifiableElement( "Auto-detect login server", JLabel.LEFT, servers[0] );
			for ( int i = 1; i <= SERVER_COUNT; ++i )
				elements[i] = new VerifiableElement( "Use login server " + i, JLabel.LEFT, servers[i] );

			setContent( elements, false );
			actionCancelled();
		}

		protected void actionConfirmed()
		{
			for ( int i = 0; i < 4; ++i )
				if ( servers[i].isSelected() )
					setProperty( "loginServer", String.valueOf( i ) );

			super.actionConfirmed();
		}

		protected void actionCancelled()
		{	servers[ Integer.parseInt( getProperty( "loginServer" ) ) ].setSelected( true );
		}
	}

	private class AdventureOptionsPanel extends OptionsPanel
	{
		private JCheckBox [] optionBoxes;

		public AdventureOptionsPanel()
		{
			super( "Adventure List", new Dimension( 340, 16 ), new Dimension( 20, 16 ) );

			optionBoxes = new JCheckBox[ AdventureDatabase.ZONES.length + 2 ];
			for ( int i = 0; i < optionBoxes.length; ++i )
				optionBoxes[i] = new JCheckBox();

			VerifiableElement [] elements = new VerifiableElement[ AdventureDatabase.ZONES.length + 3 ];

			elements[0] = new VerifiableElement( "Sort adventure list", JLabel.LEFT, optionBoxes[0] );
			elements[1] = new VerifiableElement( "Show associated zone", JLabel.LEFT, optionBoxes[1] );
			elements[2] = new VerifiableElement( " ", new JLabel( "" ) );

			for ( int i = 0; i < AdventureDatabase.ZONES.length; ++i )
				elements[i+3] = new VerifiableElement( "Hide " + AdventureDatabase.ZONES[i][1], JLabel.LEFT, optionBoxes[i+2] );

			setContent( elements, false );
			actionCancelled();
		}

		protected void actionConfirmed()
		{
			setProperty( "sortAdventures", String.valueOf( optionBoxes[0].isSelected() ) );
			setProperty( "showAdventureZone", String.valueOf( optionBoxes[1].isSelected() ) );

			StringBuffer areas = new StringBuffer();

			for ( int i = 2; i < optionBoxes.length; ++i )
			{
				if ( optionBoxes[i].isSelected() )
				{
					if ( areas.length() != 0 )
						areas.append( ',' );

					areas.append( AdventureDatabase.ZONES[i-2][0] );
				}
			}

			setProperty( "zoneExcludeList", areas.toString() );
			super.actionConfirmed();

			client.getAdventureList().clear();
			client.getAdventureList().addAll( AdventureDatabase.getAsLockableListModel() );

			if ( client != null && optionBoxes[0].isSelected() )
				Collections.sort( client.getAdventureList() );
		}

		protected void actionCancelled()
		{
			optionBoxes[0].setSelected( getProperty( "sortAdventures" ).equals( "true" ) );
			optionBoxes[1].setSelected( getProperty( "showAdventureZone" ).equals( "true" ) );

			String zones = getProperty( "zoneExcludeList" );
			for ( int i = 0; i < AdventureDatabase.ZONES.length; ++i )
				optionBoxes[i+2].setSelected( zones.indexOf( AdventureDatabase.ZONES[i][0] ) != -1 );
		}
	}

	/**
	 * This panel handles all of the things related to login
	 * options, including which server to use for login and
	 * all other requests, as well as the user's proxy settings
	 * (if applicable).
	 */

	private class StartupOptionsPanel extends OptionsPanel
	{
		private JCheckBox [] optionBoxes;

		private final String [][] options =
		{
			{ "forceReconnect", "Automatically time-in on time-out" },
			{ "skipInventory", "Skip inventory retrieval" },
			{ "skipOutfits", "Skip outfit list retrieval" },
			{ "skipFamiliars", "Skip terrarium retrieval" },
			{ "savePositions", "Reload windows in original positions" },
			{ "cloverProtectActive", "Guard against accidental clover usage" }
		};

		/**
		 * Constructs a new <code>StartupOptionsPanel</code>, containing a
		 * place for the users to select their desired server and for them
		 * to modify any applicable proxy settings.
		 */

		public StartupOptionsPanel()
		{
			super( "Startup Options", new Dimension( 340, 16 ), new Dimension( 20, 16 ) );
			VerifiableElement [] elements = new VerifiableElement[ options.length ];

			optionBoxes = new JCheckBox[ options.length ];
			for ( int i = 0; i < options.length; ++i )
				optionBoxes[i] = new JCheckBox();

			for ( int i = 0; i < options.length; ++i )
				elements[i] = new VerifiableElement( options[i][1], JLabel.LEFT, optionBoxes[i] );

			setContent( elements, false );
			actionCancelled();
		}

		protected void actionConfirmed()
		{
			for ( int i = 0; i < options.length; ++i )
				setProperty( options[i][0], String.valueOf( optionBoxes[i].isSelected() ) );

			super.actionConfirmed();
		}

		protected void actionCancelled()
		{
			for ( int i = 0; i < options.length; ++i )
				optionBoxes[i].setSelected( getProperty( options[i][0] ).equals( "true" ) );
		}
	}

	/**
	 * This panel allows the user to select how they would like to fight
	 * their battles.  Everything from attacks, attack items, recovery items,
	 * retreat, and battle skill usage will be supported when this panel is
	 * finalized.  For now, however, it only customizes attacks.
	 */

	private class BattleOptionsPanel extends OptionsPanel
	{
		private JComboBox hpAutoFleeSelect;
		private JComboBox hpAutoRecoverSelect;
		private JComboBox mpAutoRecoverSelect;
		private JTextField hpRecoveryScriptField;
		private MPRestoreItemList mpRestoreItemList;

		/**
		 * Constructs a new <code>BattleOptionsPanel</code> containing a
		 * way for the users to choose the way they want to fight battles
		 * encountered during adventuring.
		 */

		public BattleOptionsPanel()
		{
			super( "Combat Options" );

			hpAutoFleeSelect = new JComboBox();
			hpAutoFleeSelect.addItem( "Never run from combat" );
			for ( int i = 1; i <= 9; ++i )
				hpAutoFleeSelect.addItem( "Autoflee at " + (i*10) + "% HP" );

			// All the components of autorecovery

			hpAutoRecoverSelect = new JComboBox();
			hpAutoRecoverSelect.addItem( "Do not autorecover HP" );
			for ( int i = 0; i <= 9; ++i )
				hpAutoRecoverSelect.addItem( "Autorecover HP at " + (i * 10) + "%" );

			mpAutoRecoverSelect = new JComboBox();
			mpAutoRecoverSelect.addItem( "Do not autorecover MP" );
			for ( int i = 0; i <= 9; ++i )
				mpAutoRecoverSelect.addItem( "Autorecover MP at " + (i * 10) + "%" );

			hpRecoveryScriptField = new JTextField();
			mpRestoreItemList = client == null || client.getMPRestoreItemList() == null ?
				new MPRestoreItemList( null ) : client.getMPRestoreItemList();

			// Add the elements to the panel

			VerifiableElement [] elements = new VerifiableElement[7];
			elements[0] = new VerifiableElement( "Lion Roar Setting: ", hpAutoFleeSelect );

			elements[1] = new VerifiableElement( "", new JLabel() );

			elements[2] = new VerifiableElement( "HP Auto-Recovery: ", hpAutoRecoverSelect );
			elements[3] = new VerifiableElement( "HP Recovery Script: ", new ScriptSelectPanel( hpRecoveryScriptField ) );

			elements[4] = new VerifiableElement( "", new JLabel() );

			elements[5] = new VerifiableElement( "MP Auto-Recovery: ", mpAutoRecoverSelect );
			elements[6] = new VerifiableElement( "Use these restores: ", mpRestoreItemList.getDisplay() );

			setContent( elements );
			actionCancelled();
		}

		protected void actionConfirmed()
		{
			setProperty( "hpAutoFlee", String.valueOf( ((double)(hpAutoFleeSelect.getSelectedIndex()) / 10.0) ) );
			setProperty( "hpAutoRecover", String.valueOf( ((double)(hpAutoRecoverSelect.getSelectedIndex() - 1) / 10.0) ) );
			setProperty( "hpRecoveryScript", hpRecoveryScriptField.getText() );
			setProperty( "mpAutoRecover", String.valueOf( ((double)(mpAutoRecoverSelect.getSelectedIndex() - 1) / 10.0) ) );
			mpRestoreItemList.setProperty();

			super.actionConfirmed();
		}

		protected void actionCancelled()
		{
			hpAutoFleeSelect.setSelectedIndex( (int)(Double.parseDouble( getProperty( "hpAutoFlee" ) ) * 10) );
			hpAutoRecoverSelect.setSelectedIndex( (int)(Double.parseDouble( getProperty( "hpAutoRecover" ) ) * 10) + 1 );
			hpRecoveryScriptField.setText( getProperty( "hpRecoveryScript" ) );
			mpAutoRecoverSelect.setSelectedIndex( (int)(Double.parseDouble( getProperty( "mpAutoRecover" ) ) * 10) + 1 );
		}
	}

	/**
	 * This panel allows the user to select which item they would like
	 * to do for each of the different choice adventures.
	 */

	private class ChoiceOptionsPanel extends OptionsPanel
	{
		private AbstractButton [][] optionRadios;

		/**
		 * Constructs a new <code>ChoiceOptionsPanel</code> containing an
		 * alphabetized list of optionRadios available through the lucky sewer
		 * adventure.
		 */

		public ChoiceOptionsPanel()
		{
			super( "" );
			setContent( null );

			JPanel centerPanel = new JPanel();
			centerPanel.setLayout( new BoxLayout( centerPanel, BoxLayout.Y_AXIS ) );

			JPanel selectPanel;
			JScrollPane scrollArea;
			JPanel containerPanel;

			ButtonGroup optionRadiosGroup = null;
			JPanel labelPanel, optionsPanel;
			JLabel currentLabel;

			optionRadios = new AbstractButton[ AdventureDatabase.CHOICE_ADVS.length ][];

			for ( int i = 0; i < AdventureDatabase.CHOICE_ADVS.length; ++i )
			{
				labelPanel = new JPanel();
				labelPanel.setLayout( new BoxLayout( labelPanel, BoxLayout.Y_AXIS ) );

				optionsPanel = new JPanel();
				optionsPanel.setLayout( new BoxLayout( optionsPanel, BoxLayout.Y_AXIS ) );

				if ( i != 0 )
					optionRadiosGroup = new ButtonGroup();

				optionRadios[i] = new AbstractButton[ AdventureDatabase.CHOICE_ADVS[i][2].length ];

				for ( int j = 0; j < AdventureDatabase.CHOICE_ADVS[i][2].length; ++j )
				{
					currentLabel = new JLabel( AdventureDatabase.CHOICE_ADVS[i][2][j], JLabel.LEFT );
					JComponentUtilities.setComponentSize( currentLabel, 330, 20 );
					labelPanel.add( currentLabel );

					if ( i == 0 )
						optionRadios[i][j] = new JCheckBox();
					else
					{
						optionRadios[i][j] = new JRadioButton();
						optionRadiosGroup.add( optionRadios[i][j] );
					}

					JComponentUtilities.setComponentSize( optionRadios[i][j], 30, 20 );
					optionsPanel.add( optionRadios[i][j] );
				}

				selectPanel = new JPanel();
				selectPanel.setLayout( new BorderLayout() );

				selectPanel.add( labelPanel, BorderLayout.CENTER );
				selectPanel.add( optionsPanel, BorderLayout.WEST );

				scrollArea = new JScrollPane( selectPanel, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER );
				JComponentUtilities.setComponentSize( scrollArea, 320, 80 );

				containerPanel = new JPanel();
				containerPanel.setLayout( new BorderLayout() );
				containerPanel.add( JComponentUtilities.createLabel( AdventureDatabase.CHOICE_ADVS[i][1][0], JLabel.CENTER, Color.black, Color.white ), BorderLayout.NORTH );
				containerPanel.add( scrollArea, BorderLayout.CENTER );

				centerPanel.add( containerPanel );
				centerPanel.add( Box.createVerticalStrut( 20 ) );
			}

			add( centerPanel, BorderLayout.WEST );
			actionCancelled();
		}

		protected void actionConfirmed()
		{
			StringBuffer currentSetting = new StringBuffer();

			for ( int i = 0; i < optionRadios.length; ++i )
			{
				currentSetting.setLength(0);
				for ( int j = 0; j < optionRadios[i].length; ++j )
					if ( optionRadios[i][j].isSelected() )
					{
						if ( currentSetting.length() != 0 )
							currentSetting.append( ',' );

						currentSetting.append( j + 1 );
					}

				setProperty( AdventureDatabase.CHOICE_ADVS[i][0][0], currentSetting.toString() );
			}

			super.actionConfirmed();
		}

		protected void actionCancelled()
		{
			for ( int i = 0; i < optionRadios.length; ++i )
				for ( int j = 0; j < optionRadios[i].length; ++j )
					optionRadios[i][j].setSelected( false );

			String [] selected;

			for ( int i = 0; i < AdventureDatabase.CHOICE_ADVS.length; ++i )
			{
				selected = getProperty( AdventureDatabase.CHOICE_ADVS[i][0][0] ).split( "," );
				for ( int j = 0; j < selected.length; ++j )
					optionRadios[i][ Integer.parseInt( selected[j] ) - 1 ].setSelected( true );
			}
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
		private JComboBox useTabsSelect;
		private JComboBox nameClickSelect;
		private JPanel colorPanel;

		public ChatOptionsPanel()
		{
			super( "Chat Preferences" );

			fontSizeSelect = new JComboBox();
			for ( int i = 1; i <= 7; ++i )
				fontSizeSelect.addItem( String.valueOf( i ) );

			chatStyleSelect = new JComboBox();
			chatStyleSelect.addItem( "Messenger style" );
			chatStyleSelect.addItem( "Trivia host style" );
			chatStyleSelect.addItem( "LoathingChat style" );

			useTabsSelect = new JComboBox();
			useTabsSelect.addItem( "Use windowed chat interface" );
			useTabsSelect.addItem( "Use tabbed chat interface" );

			nameClickSelect = new JComboBox();
			nameClickSelect.addItem( "Open blue message" );
			nameClickSelect.addItem( "Open green message" );
			nameClickSelect.addItem( "Open purple message" );
			nameClickSelect.addItem( "Open player profile" );

			colorPanel = new JPanel();
			colorPanel.setLayout( new BoxLayout( colorPanel, BoxLayout.Y_AXIS ) );
			JScrollPane scrollArea = new JScrollPane( colorPanel, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
				JScrollPane.HORIZONTAL_SCROLLBAR_NEVER );

			JComponentUtilities.setComponentSize( scrollArea, 240, 100 );

			VerifiableElement [] elements = new VerifiableElement[5];
			elements[0] = new VerifiableElement( "Font Size: ", fontSizeSelect );
			elements[1] = new VerifiableElement( "Chat Style: ", chatStyleSelect );
			elements[2] = new VerifiableElement( "Windowing: ", useTabsSelect );
			elements[3] = new VerifiableElement( "N-Clicks: ", nameClickSelect );
			elements[4] = new VerifiableElement( "Chat Colors: ", scrollArea );

			setContent( elements );
			actionCancelled();
		}

		protected void actionConfirmed()
		{
			setProperty( "fontSize", (String) fontSizeSelect.getSelectedItem() );
			LimitedSizeChatBuffer.setFontSize( Integer.parseInt( (String) fontSizeSelect.getSelectedItem() ) );

			setProperty( "chatStyle", String.valueOf( chatStyleSelect.getSelectedIndex() ) );
			setProperty( "useTabbedChat", String.valueOf( useTabsSelect.getSelectedIndex() ) );
			setProperty( "nameClickOpens", String.valueOf( nameClickSelect.getSelectedIndex() ) );

			StringBuffer colors = new StringBuffer();
			for ( int i = 0; i < KoLMessenger.ROOMS.length; ++i )
			{
				colors.append( DataUtilities.toHexString( ((ChatColorPanel) colorPanel.getComponent( i )).selectedColor ) );
				colors.append( ',' );
			}

			setProperty( "channelColors", colors.toString() );
			super.actionConfirmed();
		}

		protected void actionCancelled()
		{
			fontSizeSelect.setSelectedItem( getProperty( "fontSize" ) );
			LimitedSizeChatBuffer.setFontSize( Integer.parseInt( getProperty( "fontSize" ) ) );

			chatStyleSelect.setSelectedIndex( Integer.parseInt( getProperty( "chatStyle" ) ) );
			useTabsSelect.setSelectedIndex( getProperty( "useTabbedChat" ).equals( "true" ) ? 1 : 0 );
			nameClickSelect.setSelectedIndex( Integer.parseInt( getProperty( "nameClickOpens" ) ) );

			if ( colorPanel.getComponentCount() == 0 )
			{
				String [] colors = getProperty( "channelColors" ).split( "," );

				if ( colors.length == 1 && colors[0].length() == 0 )
					colors = new String[0];

				for ( int i = 0; i < colors.length ; ++i )
					colorPanel.add( new ChatColorPanel( KoLMessenger.ROOMS[i], DataUtilities.toColor( colors[i] ) ) );

				for ( int i = colors.length; i < KoLMessenger.ROOMS.length; ++i )
					colorPanel.add( new ChatColorPanel( KoLMessenger.ROOMS[i], Color.black ) );
			}
		}
	}

	/**
	 * An internal class used for handling green messaging options.  This
	 * includes whether or not you save outgoing messages.
	 */

	private class GreenOptionsPanel extends OptionsPanel
	{
		private JCheckBox saveOutgoingCheckBox;
		private JCheckBox closeSendingCheckBox;

		public GreenOptionsPanel()
		{
			super( "Green Messages", new Dimension( 340, 20 ), new Dimension( 20, 20 ) );

			saveOutgoingCheckBox = new JCheckBox();
			closeSendingCheckBox = new JCheckBox();

			VerifiableElement [] elements = new VerifiableElement[2];
			elements[0] = new VerifiableElement( "Save outgoing messages", JLabel.LEFT, saveOutgoingCheckBox );
			elements[1] = new VerifiableElement( "Close green composer after successful sending", JLabel.LEFT, closeSendingCheckBox );

			setContent( elements, false );
			actionCancelled();
		}

		protected void actionConfirmed()
		{
			setProperty( "saveOutgoing", String.valueOf( saveOutgoingCheckBox.isSelected() ) );
			setProperty( "closeSending", String.valueOf( closeSendingCheckBox.isSelected() ) );
			super.actionConfirmed();
		}

		protected void actionCancelled()
		{
			saveOutgoingCheckBox.setSelected( getProperty( "saveOutgoing" ).equals( "true" ) );
			closeSendingCheckBox.setSelected( getProperty( "closeSending" ).equals( "true" ) );
		}
	}

	/**
	 * This panel handles all of the things related to the clan
	 * snapshot.  For now, just a list of checkboxes to show
	 * which fields you want there.
	 */

	private class SnapshotOptionsPanel extends OptionsPanel
	{
		private JCheckBox [] optionBoxes;

		private final String [][] options =
		{
			{ "Lv", "Player level" }, { "Mus", "Muscle points" }, { "Mys", "Mysticality points" }, { "Mox", "Moxie points" },
			{ "Total", "Total power points" }, { "Title", "Title within clan" }, { "Rank", "Rank within clan" },
			{ "Karma", "Accumulated karma" }, { "PVP", "PVP ranking" }, { "Class", "Class type" }, { "Meat", "Meat on hand" },
			{ "Turns", "Turns played" }, { "Food", "Favorite food" }, { "Drink", "Favorite booze" }, { "Last Login", "Last login date" },
			{ "Ascensions", "Number of ascensions" }
		};

		/**
		 * Constructs a new <code>StartupOptionsPanel</code>, containing a
		 * place for the users to select their desired server and for them
		 * to modify any applicable proxy settings.
		 */

		public SnapshotOptionsPanel()
		{
			super( "Clan Snapshots", new Dimension( 340, 16 ), new Dimension( 20, 16 ) );
			VerifiableElement [] elements = new VerifiableElement[ options.length ];

			optionBoxes = new JCheckBox[ options.length ];
			for ( int i = 0; i < options.length; ++i )
				optionBoxes[i] = new JCheckBox();

			for ( int i = 0; i < options.length; ++i )
				elements[i] = new VerifiableElement( options[i][1], JLabel.LEFT, optionBoxes[i] );

			setContent( elements, false );
			actionCancelled();
		}

		protected void actionConfirmed()
		{
			StringBuffer tableHeaderSetting = new StringBuffer();

			for ( int i = 0; i < options.length; ++i )
				if ( optionBoxes[i].isSelected() )
				{
					tableHeaderSetting.append( "<td>" );
					tableHeaderSetting.append( options[i][0] );
					tableHeaderSetting.append( "</td>" );
				}

			setProperty( "clanRosterHeader", tableHeaderSetting.toString() );
			super.actionConfirmed();
		}

		protected void actionCancelled()
		{
			String tableHeaderSetting = getProperty( "clanRosterHeader" );
			for ( int i = 0; i < options.length; ++i )
				optionBoxes[i].setSelected( tableHeaderSetting.indexOf( "<td>" + options[i][0] + "</td>" ) != -1 );
		}
	}

	/**
	 * An internal class used for handling mall options.  This includes
	 * default mall limiting, mall sorting and sending items to the mall.
	 */

	private class MallOptionsPanel extends OptionsPanel
	{
		private JComboBox forceSortSelect;
		private JComboBox aggregateSelect;

		public MallOptionsPanel()
		{
			super( "Mall Search" );

			forceSortSelect = new JComboBox();
			forceSortSelect.addItem( "No Sorting" );
			forceSortSelect.addItem( "Force Price Sort" );

			aggregateSelect = new JComboBox();
			aggregateSelect.addItem( "Aggregate store data" );
			aggregateSelect.addItem( "Keep stores separate" );

			VerifiableElement [] elements = new VerifiableElement[2];
			elements[0] = new VerifiableElement( "Force Sorting: ", forceSortSelect );
			elements[1] = new VerifiableElement( "Price Scanning: ", aggregateSelect );

			setContent( elements );
			actionCancelled();
		}

		protected void actionConfirmed()
		{
			setProperty( "forceSorting", String.valueOf( forceSortSelect.getSelectedIndex() == 1 ) );
			setProperty( "aggregatePrices", String.valueOf( aggregateSelect.getSelectedIndex() == 0 ) );
			super.actionConfirmed();
		}

		protected void actionCancelled()
		{
			forceSortSelect.setSelectedIndex( getProperty( "forceSorting" ).equals( "true" ) ? 1 : 0 );
			aggregateSelect.setSelectedIndex( getProperty( "aggregatePrices" ).equals( "true" ) ? 1 : 0 );
		}
	}

	/**
	 * An internal class used for handling item creation options.  This includes
	 * whether or not the closet is used as an ingredient source and whether
	 * or not the box servant will be repaired on explosion.
	 */

	private class CreationOptionsPanel extends OptionsPanel
	{
		private JCheckBox useClosetForCreationCheckBox;
		private JCheckBox autoRepairBoxesCheckBox;
		private JCheckBox useClockworkBoxesCheckBox;
		private JCheckBox createWithoutBoxServantsCheckBox;
		private JCheckBox includeAscensionRecipesCheckBox;

		public CreationOptionsPanel()
		{
			super( "Item Creation", new Dimension( 340, 20 ), new Dimension( 20, 20 ) );

			useClosetForCreationCheckBox = new JCheckBox();
			autoRepairBoxesCheckBox = new JCheckBox();
			useClockworkBoxesCheckBox = new JCheckBox();
			createWithoutBoxServantsCheckBox = new JCheckBox();
			includeAscensionRecipesCheckBox = new JCheckBox();

			VerifiableElement [] elements = new VerifiableElement[5];
			elements[0] = new VerifiableElement( "Use closet as ingredient source", JLabel.LEFT, useClosetForCreationCheckBox );
			elements[1] = new VerifiableElement( "Auto-repair box servants on explosion", JLabel.LEFT, autoRepairBoxesCheckBox );
			elements[2] = new VerifiableElement( "Use clockwork box servants", JLabel.LEFT, useClockworkBoxesCheckBox );
			elements[3] = new VerifiableElement( "Cook or mix without a box servant", JLabel.LEFT, createWithoutBoxServantsCheckBox );
			elements[4] = new VerifiableElement( "Include post-ascension recipes", JLabel.LEFT, includeAscensionRecipesCheckBox );

			setContent( elements, false );
			actionCancelled();
		}

		protected void actionConfirmed()
		{
			setProperty( "useClosetForCreation", String.valueOf( useClosetForCreationCheckBox.isSelected() ) );
			setProperty( "autoRepairBoxes", String.valueOf( autoRepairBoxesCheckBox.isSelected() ) );
			setProperty( "useClockworkBoxes", String.valueOf( useClockworkBoxesCheckBox.isSelected() ) );
			setProperty( "createWithoutBoxServants", String.valueOf( createWithoutBoxServantsCheckBox.isSelected() ) );
			setProperty( "includeAscensionRecipes", String.valueOf( includeAscensionRecipesCheckBox.isSelected() ) );

			if ( client != null && client.getInventory() != null )
				ConcoctionsDatabase.refreshConcoctions();

			super.actionConfirmed();
		}

		protected void actionCancelled()
		{
			useClosetForCreationCheckBox.setSelected( getProperty( "useClosetForCreation" ).equals( "true" ) );
			autoRepairBoxesCheckBox.setSelected( getProperty( "autoRepairBoxes" ).equals( "true" ) );
			useClockworkBoxesCheckBox.setSelected( getProperty( "useClockworkBoxes" ).equals( "true" ) );
			includeAscensionRecipesCheckBox.setSelected( getProperty( "includeAscensionRecipes" ).equals( "true" ) );
			createWithoutBoxServantsCheckBox.setSelected( getProperty( "createWithoutBoxServants" ).equals( "true" ) );
			includeAscensionRecipesCheckBox.setSelected( getProperty( "includeAscensionRecipes" ).equals( "true" ) );
		}
	}

	/**
	 * This panel handles all of the things related to proxy
	 * options (if applicable).
	 */

	private class ProxyOptionsPanel extends OptionsPanel
	{
		private JTextField proxyHost;
		private JTextField proxyPort;
		private JTextField proxyLogin;
		private JTextField proxyPassword;

		/**
		 * Constructs a new <code>ProxyOptionsPanel</code>, containing a
		 * place for the users to select their desired server and for them
		 * to modify any applicable proxy settings.
		 */

		public ProxyOptionsPanel()
		{
			super( "Proxy Setup" );

			proxyHost = new JTextField();
			proxyPort = new JTextField();
			proxyLogin = new JTextField();
			proxyPassword = new JPasswordField();

			VerifiableElement [] elements = new VerifiableElement[4];
			elements[0] = new VerifiableElement( "Proxy Host: ", proxyHost );
			elements[1] = new VerifiableElement( "Proxy Port: ", proxyPort );
			elements[2] = new VerifiableElement( "Proxy Login: ", proxyLogin );
			elements[3] = new VerifiableElement( "Proxy Password: ", proxyPassword );

			setContent( elements, true );
			actionCancelled();
		}

		protected void actionConfirmed()
		{
			client.updateDisplay( DISABLED_STATE, "Applying network settings..." );
			setProperty( "proxySet", String.valueOf( proxyHost.getText().trim().length() != 0 ) );
			setProperty( "http.proxyHost", proxyHost.getText() );
			setProperty( "http.proxyPort", proxyPort.getText() );
			setProperty( "http.proxyUser", proxyLogin.getText() );
			setProperty( "http.proxyPassword", proxyPassword.getText() );

			// Save the settings that were just set; that way,
			// the next login can use them.

			KoLRequest.applySettings();
			super.actionConfirmed();
			client.updateDisplay( ENABLED_STATE, "Network settings applied." );
		}

		protected void actionCancelled()
		{
			proxyHost.setText( getProperty( "http.proxyHost" ) );
			proxyPort.setText( getProperty( "http.proxyPort" ) );
			proxyLogin.setText( getProperty( "http.proxyUser" ) );
			proxyPassword.setText( getProperty( "http.proxyPassword" ) );
		}
	}

	/**
	 * A generic panel which adds a label to the bottom of the KoLPanel
	 * to update the panel's status.  It also provides a thread which is
	 * guaranteed to be a daemon thread for updating the frame which
	 * also retrieves a reference to the client's current settings.
	 */

	private abstract class OptionsPanel extends LabeledKoLPanel
	{
		public OptionsPanel()
		{	this( new Dimension( 120, 20 ), new Dimension( 240, 20 ) );
		}

		public OptionsPanel( String panelTitle )
		{	this( panelTitle, new Dimension( 120, 20 ), new Dimension( 240, 20 ) );
		}

		public OptionsPanel( Dimension left, Dimension right )
		{	this( null, left, right );
		}

		public OptionsPanel( String panelTitle, Dimension left, Dimension right )
		{	super( panelTitle, left, right );
		}

		public void setStatusMessage( int displayState, String message )
		{	JOptionPane.showMessageDialog( null, message );
		}

		protected void actionConfirmed()
		{	setStatusMessage( ENABLED_STATE, "Settings saved." );
		}
	}

	/**
	 * Internal class which represents the color being used
	 * for a single player in chat.
	 */

	private class ChatColorPanel extends JPanel
	{
		private Color selectedColor;
		private JButton colorSelect;
		private JLabel channelField;

		public ChatColorPanel( String label, Color c )
		{
			selectedColor = c;
			setLayout( new BorderLayout( 5, 5 ) );

			colorSelect = new JButton();
			JComponentUtilities.setComponentSize( colorSelect, 20, 20 );
			colorSelect.setBackground( selectedColor );
			colorSelect.addActionListener( new ChatColorChanger() );

			add( colorSelect, BorderLayout.WEST );

			channelField = new JLabel( label, JLabel.LEFT );
			add( channelField, BorderLayout.CENTER );
			JComponentUtilities.setComponentSize( this, 200, 20 );
		}

		/**
		 * An internal class that processes all the information related to
		 * changing the color of the names for players in chat.
		 */

		private class ChatColorChanger implements ActionListener
		{
			public void actionPerformed( ActionEvent e )
			{
				selectedColor = JColorChooser.showDialog( OptionsFrame.this, "Choose color for channel /" + channelField.getText() + "...", selectedColor );
				colorSelect.setBackground( selectedColor );
				channelField.setForeground( selectedColor );
			}
		}
	}

	/**
	 * The main method used in the event of testing the way the
	 * user interface looks.  This allows the UI to be tested
	 * without having to constantly log in and out of KoL.
	 */

	public static void main( String [] args )
	{
		Object [] parameters = new Object[1];
		parameters[0] = null;

		(new CreateFrameRunnable( OptionsFrame.class, parameters )).run();
	}
}
