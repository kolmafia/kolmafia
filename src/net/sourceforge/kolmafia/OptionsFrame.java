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

// utilities
import java.util.List;
import java.util.ArrayList;
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
	private boolean isInitialized;

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
		super( "KoLmafia: Preferences", client );

		this.isInitialized = false;
		setResizable( false );

		getContentPane().setLayout( new CardLayout( 10, 10 ) );

		JTabbedPane tabs = new JTabbedPane();

		this.client = client;
		contentPanel = null;

		JPanel [] panels = new JPanel[7];
		String [] names = new String[ panels.length ];

		for ( int i = 0; i < panels.length; ++i )
		{
			panels[i] = new JPanel();
			panels[i].setLayout( new BoxLayout( panels[i], BoxLayout.Y_AXIS ) );
		}

		names[0] = "Login";
		panels[0].add( new ServerSelectPanel() );
		panels[0].add( new LoginOptionsPanel() );

		names[1] = "Proxy";
		panels[1].add( new ProxyOptionsPanel() );

		names[2] = "Combat";
		panels[2].add( new BattleOptionsPanel() );

		names[3] = "Choice";
		panels[3].add( new SewerOptionsPanel() );

		names[4] = "People";
		panels[4].add( new ChatOptionsPanel() );
		panels[4].add( new GreenOptionsPanel() );

		names[5] = "Snaps";
		panels[5].add( new SnapshotOptionsPanel() );

		names[6] = "Items";
		panels[6].add( new MallOptionsPanel() );
		panels[6].add( new CreationOptionsPanel() );

		JScrollPane currentTab;
		for ( int i = 0; i < panels.length; ++i )
		{
			currentTab = new JScrollPane( panels[i], JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER );
			JComponentUtilities.setComponentSize( currentTab, 480, 320 );
			tabs.addTab( names[i], currentTab );
		}


		getContentPane().add( tabs, BorderLayout.CENTER );
		addWindowListener( new ReturnFocusAdapter() );

		addMenuBar();
		this.isInitialized = true;
	}

	/**
	 * Utility method used to add a menu bar to the <code>LoginFrame</code>.
	 * The menu bar contains configuration options and the general license
	 * information associated with <code>KoLmafia</code>.
	 */

	private void addMenuBar()
	{
		JMenuBar menuBar = new JMenuBar();
		this.setJMenuBar( menuBar );

		addConfigureMenu( menuBar );
		addHelpMenu( menuBar );
	}

	private class ServerSelectPanel extends OptionsPanel
	{
		private static final int SERVER_COUNT = 3;
		private JRadioButton [] servers;

		public ServerSelectPanel()
		{
			super( "Server Select", new Dimension( 300, 16 ), new Dimension( 20, 16 ) );
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
			(new LoadSettingsThread()).run();
		}

		protected void actionConfirmed()
		{	(new StoreSettingsThread()).start();
		}

		/**
		 * In order to keep the user interface from freezing (or at
		 * least appearing to freeze), this internal class is used
		 * to load the default settings.
		 */

		private class LoadSettingsThread extends OptionsThread
		{
			protected void save()
			{	servers[ Integer.parseInt( getProperty( "loginServer" ) ) ].setSelected( true );
			}
		}

		/**
		 * In order to keep the user interface from freezing (or at
		 * least appearing to freeze), this internal class is used
		 * to store the new settings.
		 */

		private class StoreSettingsThread extends OptionsThread
		{
			protected void save()
			{
				for ( int i = 0; i < 4; ++i )
					if ( servers[i].isSelected() )
						setProperty( "loginServer", String.valueOf( i ) );

				KoLRequest.applySettings();
			}
		}
	}

	/**
	 * This panel handles all of the things related to login
	 * options, including which server to use for login and
	 * all other requests, as well as the user's proxy settings
	 * (if applicable).
	 */

	private class LoginOptionsPanel extends OptionsPanel
	{
		private JCheckBox [] optionBoxes;
		private final String [] optionKeys = { "skipInventory", "skipFamiliars", "sortAdventures", "savePositions" };
		private final String [] optionNames = { "Skip inventory retrieval", "Skip familiar retrieval", "Sort adventure list by name", "Save window positions" };

		/**
		 * Constructs a new <code>LoginOptionsPanel</code>, containing a
		 * place for the users to select their desired server and for them
		 * to modify any applicable proxy settings.
		 */

		public LoginOptionsPanel()
		{
			super( "Startup Activities", new Dimension( 300, 16 ), new Dimension( 20, 16 ) );
			VerifiableElement [] elements = new VerifiableElement[ optionNames.length ];

			optionBoxes = new JCheckBox[ optionNames.length ];
			for ( int i = 0; i < optionNames.length; ++i )
				optionBoxes[i] = new JCheckBox();

			for ( int i = 0; i < optionNames.length; ++i )
				elements[i] = new VerifiableElement( optionNames[i], JLabel.LEFT, optionBoxes[i] );

			setContent( elements, false );
			(new LoadSettingsThread()).run();
		}

		protected void actionConfirmed()
		{	(new StoreSettingsThread()).start();
		}

		/**
		 * In order to keep the user interface from freezing (or at
		 * least appearing to freeze), this internal class is used
		 * to load the default settings.
		 */

		private class LoadSettingsThread extends OptionsThread
		{
			protected void save()
			{
				for ( int i = 0; i < optionKeys.length; ++i )
					optionBoxes[i].setSelected( getProperty( optionKeys[i] ).equals( "true" ) );
			}
		}

		/**
		 * In order to keep the user interface from freezing (or at
		 * least appearing to freeze), this internal class is used
		 * to store the new settings.
		 */

		private class StoreSettingsThread extends OptionsThread
		{
			protected void save()
			{
				for ( int i = 0; i < optionKeys.length; ++i )
					setProperty( optionKeys[i], String.valueOf( optionBoxes[i].isSelected() ) );
			}
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
		private LockableListModel actions;
		private LockableListModel actionNames;

		private JComboBox actionSelect;
		private JComboBox hpAutoFleeSelect;
		private JComboBox hpAutoRecoverSelect;
		private JComboBox mpAutoRecoverSelect;
		private JTextField hpRecoveryScriptField;
		private JTextField mpRecoveryScriptField;

		/**
		 * Constructs a new <code>BattleOptionsPanel</code> containing a
		 * way for the users to choose the way they want to fight battles
		 * encountered during adventuring.
		 */

		public BattleOptionsPanel()
		{
			super( "Combat Options" );

			actions = new LockableListModel();
			actionNames = new LockableListModel();

			actions.add( "attack" );  actionNames.add( "Attack with Weapon" );
			actions.add( "moxman" );  actionNames.add( "Moxious Maneuver" );

			// Add in dictionary
			actions.add( "item0536" );  actionNames.add( "Use a Dictionary" );

			// Seal clubber skills
			actions.add( "1003" );  actionNames.add( "SC: Thrust-Smack" );
			actions.add( "1004" );  actionNames.add( "SC: Lunge-Smack" );
			actions.add( "1005" );  actionNames.add( "SC: Lunging Thrust-Smack" );

			// Turtle tamer skills
			actions.add( "2003" );  actionNames.add( "TT: Headbutt" );
			actions.add( "2005" );  actionNames.add( "TT: Spectral Snapper" );

			// Pastamancer skills
			actions.add( "3003" );  actionNames.add( "PM: Minor Ray" );
			actions.add( "3005" );  actionNames.add( "PM: eXtreme Ray" );
			actions.add( "3007" );  actionNames.add( "PM: Cone of Whatever" );
			actions.add( "3008" );  actionNames.add( "PM: Weapon of the Pastalord" );

			// Sauceror skills
			actions.add( "4003" );  actionNames.add( "SR: Stream of Sauce" );
			actions.add( "4005" );  actionNames.add( "SR: Saucestorm" );
			actions.add( "4009" );  actionNames.add( "SR: Wave of Sauce" );
			actions.add( "4012" );  actionNames.add( "SR: Saucegeyser" );

			// Disco bandit skills
			actions.add( "5003" );  actionNames.add( "DB: Disco Eye-Poke" );
			actions.add( "5005" );  actionNames.add( "DB: Disco Dance of Doom" );
			actions.add( "5012" );  actionNames.add( "DB: Disco Face Stab" );

			actionSelect = new JComboBox( actionNames );

			hpAutoFleeSelect = new JComboBox();
			hpAutoFleeSelect.addItem( "Never run from combat" );
			for ( int i = 1; i <= 9; ++i )
				hpAutoFleeSelect.addItem( "Autoflee at " + (i*10) + "% HP" );

			// All the components of HP autorecover

			hpAutoRecoverSelect = new JComboBox();
			hpAutoRecoverSelect.addItem( "Do not autorecover HP" );
			for ( int i = 0; i <= 9; ++i )
				hpAutoRecoverSelect.addItem( "Autorecover HP at " + (i * 10) + "%" );

			JPanel hpRecoveryScriptPanel = new JPanel();
			hpRecoveryScriptPanel.setLayout( new BorderLayout( 0, 0 ) );
			hpRecoveryScriptField = new JTextField();
			hpRecoveryScriptPanel.add( hpRecoveryScriptField, BorderLayout.CENTER );
			JButton hpRecoveryScriptButton = new JButton( "..." );
			JComponentUtilities.setComponentSize( hpRecoveryScriptButton, 20, 20 );
			hpRecoveryScriptButton.addActionListener( new HPRecoveryScriptSelectListener() );
			hpRecoveryScriptPanel.add( hpRecoveryScriptButton, BorderLayout.EAST );

			// All the components of MP autorecover

			mpAutoRecoverSelect = new JComboBox();
			mpAutoRecoverSelect.addItem( "Do not autorecover MP" );
			for ( int i = 0; i <= 9; ++i )
				mpAutoRecoverSelect.addItem( "Autorecover MP at " + (i * 10) + "%" );

			JPanel mpRecoveryScriptPanel = new JPanel();
			mpRecoveryScriptPanel.setLayout( new BorderLayout( 0, 0 ) );
			mpRecoveryScriptField = new JTextField();
			mpRecoveryScriptPanel.add( mpRecoveryScriptField, BorderLayout.CENTER );
			JButton mpRecoveryScriptButton = new JButton( "..." );
			JComponentUtilities.setComponentSize( mpRecoveryScriptButton, 20, 20 );
			mpRecoveryScriptButton.addActionListener( new MPRecoveryScriptSelectListener() );
			mpRecoveryScriptPanel.add( mpRecoveryScriptButton, BorderLayout.EAST );

			// Add the elements to the panel

			VerifiableElement [] elements = new VerifiableElement[8];
			elements[0] = new VerifiableElement( "Battle Style: ", actionSelect );
			elements[1] = new VerifiableElement( "Lion Roar Setting: ", hpAutoFleeSelect );

			elements[2] = new VerifiableElement( "", new JLabel() );

			elements[3] = new VerifiableElement( "HP Auto-Recovery: ", hpAutoRecoverSelect );
			elements[4] = new VerifiableElement( "HP Recovery Script: ", hpRecoveryScriptPanel );

			elements[5] = new VerifiableElement( "", new JLabel() );

			elements[6] = new VerifiableElement( "MP Auto-Recovery: ", mpAutoRecoverSelect );
			elements[7] = new VerifiableElement( "MP Recovery Script: ", mpRecoveryScriptPanel );

			setContent( elements );
			(new LoadSettingsThread()).start();
		}

		protected void actionConfirmed()
		{	(new StoreSettingsThread()).start();
		}

		/**
		 * In order to keep the user interface from freezing (or at
		 * least appearing to freeze), this internal class is used
		 * to load the default settings.
		 */

		private class LoadSettingsThread extends OptionsThread
		{
			protected void save()
			{
				actionNames.setSelectedIndex( actions.indexOf( getProperty( "battleAction" ) ) );
				hpAutoFleeSelect.setSelectedIndex( (int)(Double.parseDouble( getProperty( "hpAutoFlee" ) ) * 10) );
				hpAutoRecoverSelect.setSelectedIndex( (int)(Double.parseDouble( getProperty( "hpAutoRecover" ) ) * 10) + 1 );
				hpRecoveryScriptField.setText( getProperty( "hpRecoveryScript" ) );
				mpAutoRecoverSelect.setSelectedIndex( (int)(Double.parseDouble( getProperty( "mpAutoRecover" ) ) * 10) + 1 );
				mpRecoveryScriptField.setText( getProperty( "mpRecoveryScript" ) );
			}
		}

		/**
		 * In order to keep the user interface from freezing (or at
		 * least appearing to freeze), this internal class is used
		 * to store the new settings.
		 */

		private class StoreSettingsThread extends OptionsThread
		{
			protected void save()
			{
				setProperty( "battleAction", (String) actions.get( actionNames.getSelectedIndex() ) );
				setProperty( "hpAutoFlee", String.valueOf( ((double)(hpAutoFleeSelect.getSelectedIndex()) / 10.0) ) );
				setProperty( "hpAutoRecover", String.valueOf( ((double)(hpAutoRecoverSelect.getSelectedIndex() - 1) / 10.0) ) );
				setProperty( "hpRecoveryScript", hpRecoveryScriptField.getText() );
				setProperty( "mpAutoRecover", String.valueOf( ((double)(mpAutoRecoverSelect.getSelectedIndex() - 1) / 10.0) ) );
				setProperty( "mpRecoveryScript", mpRecoveryScriptField.getText() );
			}
		}

		/**
		 * This internal class is used to process the request for selecting
		 * a recovery script.
		 */

		private class HPRecoveryScriptSelectListener implements ActionListener
		{
			public void actionPerformed( ActionEvent e )
			{
				JFileChooser chooser = new JFileChooser( "." );
				int returnVal = chooser.showOpenDialog( OptionsFrame.this );

				if ( chooser.getSelectedFile() == null )
					return;

				hpRecoveryScriptField.setText( chooser.getSelectedFile().getAbsolutePath() );
			}
		}

		/**
		 * This internal class is used to process the request for selecting
		 * a recovery script.
		 */

		private class MPRecoveryScriptSelectListener implements ActionListener
		{
			public void actionPerformed( ActionEvent e )
			{
				JFileChooser chooser = new JFileChooser( "." );
				int returnVal = chooser.showOpenDialog( OptionsFrame.this );

				if ( chooser.getSelectedFile() == null )
					return;

				mpRecoveryScriptField.setText( chooser.getSelectedFile().getAbsolutePath() );
			}
		}
	}

	/**
	 * This panel allows the user to select which item they would like
	 * to trade with the gnomes in the sewers of Seaside Town, in
	 * exchange for their ten-leaf clover.  These settings only apply
	 * to the Lucky Sewer adventure.
	 */

	private class SewerOptionsPanel extends OptionsPanel
	{
		private JCheckBox [] items;

		private final String [] itemnames = { "seal-clubbing club", "seal tooth", "helmet turtle",
			"scroll of turtle summoning", "pasta spoon", "ravioli hat", "saucepan", "spices", "disco mask",
			"disco ball", "stolen accordion", "mariachi pants", "worthless trinket" };

		/**
		 * Constructs a new <code>SewerOptionsPanel</code> containing an
		 * alphabetized list of items available through the lucky sewer
		 * adventure.
		 */

		public SewerOptionsPanel()
		{
			super( "Lucky Sewer Settings", new Dimension( 200, 20 ), new Dimension( 20, 20 ) );
			items = new JCheckBox[ itemnames.length ];
			for ( int i = 0; i < items.length; ++i )
				items[i] = new JCheckBox();

			VerifiableElement [] elements = new VerifiableElement[ items.length ];
			for ( int i = 0; i < items.length; ++i )
				elements[i] = new VerifiableElement( itemnames[i], JLabel.LEFT, items[i] );

			java.util.Arrays.sort( elements );
			setContent( elements, false );
			(new LoadSettingsThread()).start();
		}

		protected void actionConfirmed()
		{	(new StoreSettingsThread()).start();
		}

		/**
		 * In order to keep the user interface from freezing (or at
		 * least appearing to freeze), this internal class is used
		 * to load the default settings.
		 */

		private class LoadSettingsThread extends OptionsThread
		{
			protected void save()
			{
				for ( int i = 0; i < items.length; ++i )
					items[i].setSelected( false );

				String [] selected = getProperty( "luckySewer" ).split( "," );
				for ( int i = 0; i < selected.length; ++i )
					items[ Integer.parseInt( selected[i] ) - 1 ].setSelected( true );
			}
		}

		/**
		 * In order to keep the user interface from freezing (or at
		 * least appearing to freeze), this internal class is used
		 * to store the new settings.
		 */

		private class StoreSettingsThread extends OptionsThread
		{
			protected void save()
			{
				List selected = new ArrayList();

				for ( int i = 0; i < items.length; ++i )
					if ( items[i].isSelected() )
						selected.add( new Integer(i) );

				if ( selected.size() != 3 )
				{
					setStatusMessage( ERROR_STATE, "You did not select exactly three items." );
					return;
				}

				setProperty( "luckySewer", selected.get(0) + "," + selected.get(1) + "," + selected.get(2) );
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
			super( "LoathingChat Preferences" );

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

			JComponentUtilities.setComponentSize( scrollArea, 200, 100 );

			VerifiableElement [] elements = new VerifiableElement[4];
			elements[0] = new VerifiableElement( "Font Size: ", fontSizeSelect );
			elements[1] = new VerifiableElement( "Chat Style: ", chatStyleSelect );
			elements[2] = new VerifiableElement( "Windowing: ", useTabsSelect );
			elements[3] = new VerifiableElement( "Chat Colors: ", scrollArea );

			setContent( elements );
			(new LoadSettingsThread()).start();
		}

		protected void actionConfirmed()
		{	(new StoreSettingsThread()).start();
		}

		/**
		 * In order to keep the user interface from freezing (or at
		 * least appearing to freeze), this internal class is used
		 * to load the default settings.
		 */

		private class LoadSettingsThread extends OptionsThread
		{
			protected void save()
			{
				fontSizeSelect.setSelectedItem( getProperty( "fontSize" ) );
				LimitedSizeChatBuffer.setFontSize( Integer.parseInt( getProperty( "fontSize" ) ) );

				chatStyleSelect.setSelectedIndex( Integer.parseInt( getProperty( "chatStyle" ) ) );
				useTabsSelect.setSelectedIndex( getProperty( "useTabbedChat" ).equals( "true" ) ? 1 : 0 );
				nameClickSelect.setSelectedIndex( Integer.parseInt( getProperty( "nameClickOpens" ) ) );

				if ( colorPanel.getComponentCount() == 0 )
				{
					String [] colors = getProperty( "channelColors" ).split( "," );
 					for ( int i = 0; i < colors.length ; ++i )
 						colorPanel.add( new ChatColorPanel( KoLMessenger.ROOMS[i], DataUtilities.toColor( colors[i] ) ) );

 					for ( int i = colors.length; i < KoLMessenger.ROOMS.length; ++i )
	 					colorPanel.add( new ChatColorPanel( KoLMessenger.ROOMS[i], Color.black ) );
				}
			}
		}

		/**
		 * In order to keep the user interface from freezing (or at
		 * least appearing to freeze), this internal class is used
		 * to store the new settings.
		 */

		private class StoreSettingsThread extends OptionsThread
		{
			protected void save()
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
			super( "Green Message Handling", new Dimension( 300, 20 ), new Dimension( 20, 20 ) );

			saveOutgoingCheckBox = new JCheckBox();
			closeSendingCheckBox = new JCheckBox();

			VerifiableElement [] elements = new VerifiableElement[2];
			elements[0] = new VerifiableElement( "Save outgoing messages", JLabel.LEFT, saveOutgoingCheckBox );
			elements[1] = new VerifiableElement( "Close green composer after successful sending", JLabel.LEFT, closeSendingCheckBox );

			setContent( elements, false );
			(new LoadSettingsThread()).start();
		}

		protected void actionConfirmed()
		{	(new StoreSettingsThread()).start();
		}

		/**
		 * In order to keep the user interface from freezing (or at
		 * least appearing to freeze), this internal class is used
		 * to load the default settings.
		 */

		private class LoadSettingsThread extends OptionsThread
		{
			protected void save()
			{
				saveOutgoingCheckBox.setSelected( getProperty( "saveOutgoing" ).equals( "true" ) );
				closeSendingCheckBox.setSelected( getProperty( "closeSending" ).equals( "true" ) );
			}
		}

		/**
		 * In order to keep the user interface from freezing (or at
		 * least appearing to freeze), this internal class is used
		 * to store the new settings.
		 */

		private class StoreSettingsThread extends OptionsThread
		{
			protected void save()
			{
				setProperty( "saveOutgoing", String.valueOf( saveOutgoingCheckBox.isSelected() ) );
				setProperty( "closeSending", String.valueOf( closeSendingCheckBox.isSelected() ) );
			}
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
		private final String [] optionKeys = { "Lv", "Mus", "Mys", "Mox", "Total", "Title", "Rank", "Karma",
			"PVP", "Class", "Meat", "Turns", "Food", "Drink", "Last Login" };
		private final String [] optionNames = { "Player level", "Muscle points", "Mysticality points", "Moxie points",
			"Total power points", "Title within clan", "Rank within clan", "Accumulated karma", "PVP ranking",
			"Class type", "Meat on hand", "Turns played", "Favorite food", "Favorite booze", "Last login date" };

		/**
		 * Constructs a new <code>LoginOptionsPanel</code>, containing a
		 * place for the users to select their desired server and for them
		 * to modify any applicable proxy settings.
		 */

		public SnapshotOptionsPanel()
		{
			super( "Clan Snapshot Columns", new Dimension( 300, 16 ), new Dimension( 20, 16 ) );
			VerifiableElement [] elements = new VerifiableElement[ optionNames.length ];

			optionBoxes = new JCheckBox[ optionNames.length ];
			for ( int i = 0; i < optionNames.length; ++i )
				optionBoxes[i] = new JCheckBox();

			for ( int i = 0; i < optionNames.length; ++i )
				elements[i] = new VerifiableElement( optionNames[i], JLabel.LEFT, optionBoxes[i] );

			setContent( elements, false );
			(new LoadSettingsThread()).run();
		}

		protected void actionConfirmed()
		{	(new StoreSettingsThread()).start();
		}

		/**
		 * In order to keep the user interface from freezing (or at
		 * least appearing to freeze), this internal class is used
		 * to load the default settings.
		 */

		private class LoadSettingsThread extends OptionsThread
		{
			protected void save()
			{
				String tableHeaderSetting = getProperty( "clanRosterHeader" );
				for ( int i = 0; i < optionKeys.length; ++i )
					optionBoxes[i].setSelected( tableHeaderSetting.indexOf( "<td>" + optionKeys[i] + "</td>" ) != -1 );
			}
		}

		/**
		 * In order to keep the user interface from freezing (or at
		 * least appearing to freeze), this internal class is used
		 * to store the new settings.
		 */

		private class StoreSettingsThread extends OptionsThread
		{
			protected void save()
			{
				StringBuffer tableHeaderSetting = new StringBuffer();

				for ( int i = 0; i < optionKeys.length; ++i )
					if ( optionBoxes[i].isSelected() )
					{
						tableHeaderSetting.append( "<td>" );
						tableHeaderSetting.append( optionKeys[i] );
						tableHeaderSetting.append( "</td>" );
					}

				setProperty( "clanRosterHeader", tableHeaderSetting.toString() );
			}
		}
	}

	/**
	 * An internal class used for handling mall options.  This includes
	 * default mall limiting, mall sorting and sending items to the mall.
	 */

	private class MallOptionsPanel extends OptionsPanel
	{
		private JTextField defaultLimitField;
		private JComboBox forceSortSelect;
		private JComboBox aggregateSelect;

		public MallOptionsPanel()
		{
			super( "Mall Configuration" );
			defaultLimitField = new JTextField( "13" );

			forceSortSelect = new JComboBox();
			forceSortSelect.addItem( "No Sorting" );
			forceSortSelect.addItem( "Force Price Sort" );

			aggregateSelect = new JComboBox();
			aggregateSelect.addItem( "Aggregate store data" );
			aggregateSelect.addItem( "Keep stores separate" );

			VerifiableElement [] elements = new VerifiableElement[3];
			elements[0] = new VerifiableElement( "Default Limit: ", defaultLimitField );
			elements[1] = new VerifiableElement( "Force Sorting: ", forceSortSelect );
			elements[2] = new VerifiableElement( "Price Scanning: ", aggregateSelect );

			setContent( elements );
			(new LoadSettingsThread()).start();
		}

		protected void actionConfirmed()
		{	(new StoreSettingsThread()).start();
		}

		/**
		 * In order to keep the user interface from freezing (or at
		 * least appearing to freeze), this internal class is used
		 * to load the default settings.
		 */

		private class LoadSettingsThread extends OptionsThread
		{
			protected void save()
			{
				defaultLimitField.setText( getProperty( "defaultLimit" ) );
				forceSortSelect.setSelectedIndex( getProperty( "forceSorting" ).equals( "true" ) ? 1 : 0 );
				aggregateSelect.setSelectedIndex( getProperty( "aggregatePrices" ).equals( "true" ) ? 1 : 0 );
			}
		}

		/**
		 * In order to keep the user interface from freezing (or at
		 * least appearing to freeze), this internal class is used
		 * to store the new settings.
		 */

		private class StoreSettingsThread extends OptionsThread
		{
			protected void save()
			{
				setProperty( "defaultLimit", defaultLimitField.getText() );
				setProperty( "forceSorting", String.valueOf( forceSortSelect.getSelectedIndex() == 1 ) );
				setProperty( "aggregatePrices", String.valueOf( aggregateSelect.getSelectedIndex() == 0 ) );
			}
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
		private JCheckBox includeAscensionRecipesCheckBox;

		public CreationOptionsPanel()
		{
			super( "Item Creation Handling", new Dimension( 300, 20 ), new Dimension( 20, 20 ) );

			useClosetForCreationCheckBox = new JCheckBox();
			autoRepairBoxesCheckBox = new JCheckBox();
			includeAscensionRecipesCheckBox = new JCheckBox();

			VerifiableElement [] elements = new VerifiableElement[3];
			elements[0] = new VerifiableElement( "Use closet as ingredient source", JLabel.LEFT, useClosetForCreationCheckBox );
			elements[1] = new VerifiableElement( "Auto-repair box servants on explosion", JLabel.LEFT, autoRepairBoxesCheckBox );
			elements[2] = new VerifiableElement( "Include post-ascension recipes", JLabel.LEFT, includeAscensionRecipesCheckBox );

			setContent( elements, false );
			(new LoadSettingsThread()).start();
		}

		protected void actionConfirmed()
		{	(new StoreSettingsThread()).start();
		}

		/**
		 * In order to keep the user interface from freezing (or at
		 * least appearing to freeze), this internal class is used
		 * to load the default settings.
		 */

		private class LoadSettingsThread extends OptionsThread
		{
			protected void save()
			{
				useClosetForCreationCheckBox.setSelected( getProperty( "useClosetForCreation" ).equals( "true" ) );
				autoRepairBoxesCheckBox.setSelected( getProperty( "autoRepairBoxes" ).equals( "true" ) );
				includeAscensionRecipesCheckBox.setSelected( getProperty( "includeAscensionRecipes" ).equals( "true" ) );
			}
		}

		/**
		 * In order to keep the user interface from freezing (or at
		 * least appearing to freeze), this internal class is used
		 * to store the new settings.
		 */

		private class StoreSettingsThread extends OptionsThread
		{
			protected void save()
			{
				setProperty( "useClosetForCreation", String.valueOf( useClosetForCreationCheckBox.isSelected() ) );
				setProperty( "autoRepairBoxes", String.valueOf( autoRepairBoxesCheckBox.isSelected() ) );
				setProperty( "includeAscensionRecipes", String.valueOf( includeAscensionRecipesCheckBox.isSelected() ) );
			}
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
			super( "Proxy Configuration" );

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
			(new LoadSettingsThread()).run();
		}

		protected void actionConfirmed()
		{	(new StoreSettingsThread()).start();
		}

		/**
		 * In order to keep the user interface from freezing (or at
		 * least appearing to freeze), this internal class is used
		 * to load the default settings.
		 */

		private class LoadSettingsThread extends OptionsThread
		{
			protected void save()
			{
				proxyHost.setText( getProperty( "http.proxyHost" ) );
				proxyPort.setText( getProperty( "http.proxyPort" ) );
				proxyLogin.setText( getProperty( "http.proxyUser" ) );
				proxyPassword.setText( getProperty( "http.proxyPassword" ) );
			}
		}

		/**
		 * In order to keep the user interface from freezing (or at
		 * least appearing to freeze), this internal class is used
		 * to store the new settings.
		 */

		private class StoreSettingsThread extends OptionsThread
		{
			protected void save()
			{
				setProperty( "proxySet", String.valueOf( proxyHost.getText().trim().length() != 0 ) );
				setProperty( "http.proxyHost", proxyHost.getText() );
				setProperty( "http.proxyPort", proxyPort.getText() );
				setProperty( "http.proxyUser", proxyLogin.getText() );
				setProperty( "http.proxyPassword", proxyPassword.getText() );

				// Save the settings that were just set; that way,
				// the next login can use them.

				KoLRequest.applySettings();
			}
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
		private KoLSettings settings;

		public OptionsPanel()
		{	this( new Dimension( 120, 20 ), new Dimension( 200, 20 ) );
		}

		public OptionsPanel( String panelTitle )
		{	this( panelTitle, new Dimension( 120, 20 ), new Dimension( 200, 20 ) );
		}

		public OptionsPanel( Dimension left, Dimension right )
		{	this( null, left, right );
		}

		public OptionsPanel( String panelTitle, Dimension left, Dimension right )
		{
			super( panelTitle, left, right );
			settings = (client == null) ? new KoLSettings() : client.getSettings();
		}

		public void setStatusMessage( int displayState, String message )
		{
			if ( isInitialized )
				JOptionPane.showMessageDialog( null, message );
		}

		protected void setProperty( String key, String value )
		{	settings.setProperty( key, value );
		}

		protected String getProperty( String key )
		{	return settings.getProperty( key );
		}

		protected abstract class OptionsThread extends RequestThread
		{
			public final void run()
			{
				save();

				if ( isInitialized )
				{
					settings.saveSettings();
					setStatusMessage( ENABLED_STATE, "Settings saved." );
				}
			}

			protected abstract void save();
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
			JComponentUtilities.setComponentSize( this, 160, 20 );
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
				channelField.requestFocus();
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
		KoLFrame uitest = new OptionsFrame( null );
		uitest.pack();  uitest.setVisible( true );  uitest.requestFocus();
	}
}
