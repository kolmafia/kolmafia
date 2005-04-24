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

// utilities
import java.util.Properties;
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
		setResizable( false );

		getContentPane().setLayout( new CardLayout( 10, 10 ) );

		JTabbedPane tabs = new JTabbedPane();

		// Because none of the frames support setStatusMessage,
		// the content panel is arbitrary

		this.client = client;
		contentPanel = null;

		tabs.addTab( "Login", new LoginOptionsPanel() );
		tabs.addTab( "Proxy", new ProxyOptionsPanel() );
		tabs.addTab( "Battle", new BattleOptionsPanel() );
		tabs.addTab( "Mall", new ResultsOptionsPanel() );
		tabs.addTab( "Sewer", new SewerOptionsPanel() );
		tabs.addTab( "Chat", new ChatOptionsPanel() );

		getContentPane().add( tabs, BorderLayout.CENTER );
		addWindowListener( new ReturnFocusAdapter() );

		addMenuBar();
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

	/**
	 * This panel handles all of the things related to login
	 * options, including which server to use for login and
	 * all other requests, as well as the user's proxy settings
	 * (if applicable).
	 */

	private class LoginOptionsPanel extends OptionsPanel
	{
		private static final int SERVER_COUNT = 3;

		private JRadioButton [] servers;
		private JCheckBox [] optionBoxes;
		private JCheckBox sortAdventuresBox;
		private final String [] optionKeys = { "skipCharacterData", "skipInventory", "skipFamiliarData" };
		private final String [] optionNames = { "Skip character data retrieval", "Skip inventory retrieval", "Skip familiar data retrieval" };

		/**
		 * Constructs a new <code>LoginOptionsPanel</code>, containing a
		 * place for the users to select their desired server and for them
		 * to modify any applicable proxy settings.
		 */

		public LoginOptionsPanel()
		{
			super( new Dimension( 200, 16 ), new Dimension( 20, 16 ) );
			VerifiableElement [] elements = new VerifiableElement[ 3 + SERVER_COUNT + optionNames.length ];

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

			optionBoxes = new JCheckBox[ optionNames.length ];
			for ( int i = 0; i < optionNames.length; ++i )
				optionBoxes[i] = new JCheckBox();

			elements[ SERVER_COUNT + 1 ] = new VerifiableElement( " ", new JPanel() );

			for ( int i = 0; i < optionNames.length; ++i )
				elements[i + 2 + SERVER_COUNT ] = new VerifiableElement( optionNames[i], JLabel.LEFT, optionBoxes[i] );

			sortAdventuresBox = new JCheckBox();
			elements[ elements.length - 1 ] = new VerifiableElement( "Sort adventure list by name", JLabel.LEFT, sortAdventuresBox );

			setContent( elements, false );
			(new LoadDefaultSettingsThread()).run();
		}

		protected void actionConfirmed()
		{	(new StoreSettingsThread()).start();
		}

		/**
		 * In order to keep the user interface from freezing (or at
		 * least appearing to freeze), this internal class is used
		 * to load the default settings.
		 */

		private class LoadDefaultSettingsThread extends OptionsThread
		{
			public void run()
			{
				if ( client == null )
					System.setProperty( "loginServer", "0" );

				servers[ Integer.parseInt( settings.getProperty( "loginServer" ) ) ].setSelected( true );

				for ( int i = 0; i < optionKeys.length; ++i )
					optionBoxes[i].setSelected( settings.getProperty( optionKeys[i] ) != null &&
						settings.getProperty( optionKeys[i] ).equals( "true" ) );

				sortAdventuresBox.setSelected( settings.getProperty( "sortAdventures" ) != null &&
					settings.getProperty( "sortAdventures" ).equals( "true" ) );

				setStatusMessage( ENABLED_STATE, "Settings loaded." );
			}
		}

		/**
		 * In order to keep the user interface from freezing (or at
		 * least appearing to freeze), this internal class is used
		 * to store the new settings.
		 */

		private class StoreSettingsThread extends OptionsThread
		{
			public void run()
			{
				// Next, change the server that's used to login;
				// find out the selected index.

				for ( int i = 0; i < 4; ++i )
					if ( servers[i].isSelected() )
						settings.setProperty( "loginServer", "" + i );

				for ( int i = 0; i < optionKeys.length; ++i )
					settings.setProperty( optionKeys[i], "" + optionBoxes[i].isSelected() );

				settings.setProperty( "sortAdventures", "" + sortAdventuresBox.isSelected() );

				// Save the settings that were just set; that way,
				// the next login can use them.

				saveSettings();
				KoLRequest.applySettings();
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
			actions = new LockableListModel();
			actionNames = new LockableListModel();

			actions.add( "attack" );  actionNames.add( "Attack with Weapon" );
			actions.add( "moxman" );  actionNames.add( "Moxious Maneuver" );

			// Seal clubber skills
			actions.add( "1003" );  actionNames.add( "Seal: Thrust-Smack" );
			actions.add( "1004" );  actionNames.add( "Seal: Lunge-Smack" );
			actions.add( "1007" );  actionNames.add( "Seal: Lunge Thrust-Smack" );

			// Turtle tamer skills
			actions.add( "2003" );  actionNames.add( "Turt: Headbutt" );
			actions.add( "2005" );  actionNames.add( "Turt: Spectral Snapper" );

			// Pastamancer skills
			actions.add( "3003" );  actionNames.add( "Past: Minor Ray of Something" );
			actions.add( "3005" );  actionNames.add( "Past: eXtreme Ray of Something" );
			actions.add( "3007" );  actionNames.add( "Past: Cone of Whatever" );
			actions.add( "3008" );  actionNames.add( "Past: Weapon of the Pastalord" );

			// Sauceror skills
			actions.add( "4003" );  actionNames.add( "Sauc: Stream of Sauce" );
			actions.add( "4005" );  actionNames.add( "Sauc: Saucestorm" );
			actions.add( "4009" );  actionNames.add( "Sauc: Wave of Sauce" );
			actions.add( "4012" );  actionNames.add( "Sauc: Saucegeyser" );

			// Disco bandit skills
			actions.add( "5003" );  actionNames.add( "Disc: Disco Eye-Poke" );
			actions.add( "5005" );  actionNames.add( "Disc: Disco Dance of Doom" );
			actions.add( "5012" );  actionNames.add( "Disc: Disco Face Stab" );

			actionSelect = new JComboBox( actionNames );

			LockableListModel hpAutoFlee = new LockableListModel();
			hpAutoFlee.add( "Never run from combat" );
			for ( int i = 1; i <= 9; ++i )
				hpAutoFlee.add( "Autoflee at " + (i*10) + "% HP" );

			hpAutoFleeSelect = new JComboBox( hpAutoFlee );

			// All the components of HP autorecover

			LockableListModel hpAutoRecover = new LockableListModel();
			hpAutoRecover.add( "Do not autorecover HP" );
			for ( int i = 0; i <= 9; ++i )
				hpAutoRecover.add( "Autorecover HP at " + (i * 10) + "%" );

			hpAutoRecoverSelect = new JComboBox( hpAutoRecover );

			JPanel hpRecoveryScriptPanel = new JPanel();
			hpRecoveryScriptPanel.setLayout( new BorderLayout( 0, 0 ) );
			hpRecoveryScriptField = new JTextField();
			hpRecoveryScriptPanel.add( hpRecoveryScriptField, BorderLayout.CENTER );
			JButton hpRecoveryScriptButton = new JButton( "..." );
			JComponentUtilities.setComponentSize( hpRecoveryScriptButton, 20, 20 );
			hpRecoveryScriptButton.addActionListener( new HPRecoveryScriptSelectListener() );
			hpRecoveryScriptPanel.add( hpRecoveryScriptButton, BorderLayout.EAST );

			// All the components of MP autorecover

			LockableListModel mpAutoRecover = new LockableListModel();
			mpAutoRecover.add( "Do not autorecover MP" );
			for ( int i = 0; i <= 9; ++i )
				mpAutoRecover.add( "Autorecover MP at " + (i * 10) + "%" );

			mpAutoRecoverSelect = new JComboBox( mpAutoRecover );

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
			(new LoadDefaultSettingsThread()).start();
		}

		protected void actionConfirmed()
		{	(new StoreSettingsThread()).start();
		}

		/**
		 * In order to keep the user interface from freezing (or at
		 * least appearing to freeze), this internal class is used
		 * to load the default settings.
		 */

		private class LoadDefaultSettingsThread extends OptionsThread
		{
			public void run()
			{
				String battleSettings = settings.getProperty( "battleAction" );
				String hpAutoFleeSettings = settings.getProperty( "hpAutoFlee" );
				String hpAutoRecoverSettings = settings.getProperty( "hpAutoRecover" );
				String hpRecoveryScriptSettings = settings.getProperty( "hpRecoveryScript" );
				String mpAutoRecoverSettings = settings.getProperty( "mpAutoRecover" );
				String mpRecoveryScriptSettings = settings.getProperty( "mpRecoveryScript" );

				// If there are no default settings, simply skip the
				// attempt at loading them.

				actionNames.setSelectedIndex( battleSettings == null ? 0 : actions.indexOf( battleSettings ) );
				hpAutoFleeSelect.setSelectedIndex( hpAutoFleeSettings == null ? 0 :
					(int)(Double.parseDouble( hpAutoFleeSettings ) * 10) );

				hpAutoRecoverSelect.setSelectedIndex( hpAutoRecoverSettings == null ? 0 :
					(int)(Double.parseDouble( hpAutoRecoverSettings ) * 10) + 1 );
				hpRecoveryScriptField.setText( hpRecoveryScriptSettings == null ? "" : hpRecoveryScriptSettings );

				mpAutoRecoverSelect.setSelectedIndex( mpAutoRecoverSettings == null ? 0 :
					(int)(Double.parseDouble( mpAutoRecoverSettings ) * 10) + 1 );
				mpRecoveryScriptField.setText( mpRecoveryScriptSettings == null ? "" : mpRecoveryScriptSettings );

				setStatusMessage( ENABLED_STATE, "Settings loaded." );
			}
		}

		/**
		 * In order to keep the user interface from freezing (or at
		 * least appearing to freeze), this internal class is used
		 * to store the new settings.
		 */

		private class StoreSettingsThread extends OptionsThread
		{
			public void run()
			{
				settings.setProperty( "battleAction", (String) actions.get( actionNames.getSelectedIndex() ) );
				settings.setProperty( "hpAutoFlee", "" + ((double)(hpAutoFleeSelect.getSelectedIndex()) / 10.0) );
				settings.setProperty( "hpAutoRecover", "" + ((double)(hpAutoRecoverSelect.getSelectedIndex() - 1) / 10.0) );
				settings.setProperty( "hpRecoveryScript", hpRecoveryScriptField.getText() );
				settings.setProperty( "mpAutoRecover", "" + ((double)(mpAutoRecoverSelect.getSelectedIndex() - 1) / 10.0) );
				settings.setProperty( "mpRecoveryScript", mpRecoveryScriptField.getText() );
				saveSettings();
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
			super( new Dimension( 200, 20 ), new Dimension( 20, 20 ) );
			items = new JCheckBox[ itemnames.length ];
			for ( int i = 0; i < items.length; ++i )
				items[i] = new JCheckBox();

			VerifiableElement [] elements = new VerifiableElement[ items.length ];
			for ( int i = 0; i < items.length; ++i )
				elements[i] = new VerifiableElement( itemnames[i], JLabel.LEFT, items[i] );

			java.util.Arrays.sort( elements );
			setContent( elements, false );
			(new LoadDefaultSettingsThread()).start();
		}

		protected void actionConfirmed()
		{	(new StoreSettingsThread()).start();
		}

		/**
		 * In order to keep the user interface from freezing (or at
		 * least appearing to freeze), this internal class is used
		 * to load the default settings.
		 */

		private class LoadDefaultSettingsThread extends OptionsThread
		{
			public void run()
			{
				String sewerSettings = (client == null) ? null :
					settings.getProperty( "luckySewer" );

				// If there are no default settings, simply skip the
				// attempt at loading them.

				if ( sewerSettings == null )
					return;

				// If there are default settings, make sure that the
				// appropriate check box is checked.

				StringTokenizer st = new StringTokenizer( sewerSettings, "," );
				for ( int i = 0; i < items.length; ++i )
					items[i].setSelected( false );

				while ( st.hasMoreTokens() )
					items[ Integer.parseInt( st.nextToken() ) - 1 ].setSelected( true );

				setStatusMessage( ENABLED_STATE, "Settings loaded." );
			}
		}

		/**
		 * In order to keep the user interface from freezing (or at
		 * least appearing to freeze), this internal class is used
		 * to store the new settings.
		 */

		private class StoreSettingsThread extends OptionsThread
		{
			public void run()
			{
				int [] selected = new int[3];
				int selectedCount = 0;

				for ( int i = 0; i < items.length; ++i )
				{
					if ( items[i].isSelected() )
					{
						if ( selectedCount < 3 )
							selected[selectedCount] = i + 1;
						++selectedCount;
					}
				}

				if ( selectedCount != 3 )
				{
					setStatusMessage( ERROR_STATE, "You did not select exactly three items." );
					return;
				}

				if ( client != null )
				{
					settings.setProperty( "luckySewer", selected[0] + "," + selected[1] + "," + selected[2] );
					if ( settings instanceof KoLSettings )
						((KoLSettings)settings).saveSettings();
				}

				saveSettings();
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
			LockableListModel fontSizes = new LockableListModel();
			for ( int i = 1; i <= 7; ++i )
				fontSizes.add( new Integer( i ) );
			fontSizeSelect = new JComboBox( fontSizes );

			LockableListModel chatStyles = new LockableListModel();
			chatStyles.add( "Messenger style" );
			chatStyles.add( "Trivia hosting style" );
			chatStyleSelect = new JComboBox( chatStyles );

			LockableListModel useTabs = new LockableListModel();
			useTabs.add( "Use windowed chat interface" );
			useTabs.add( "Use tabbed chat interface" );
			useTabsSelect = new JComboBox( useTabs );

			LockableListModel nameClick = new LockableListModel();
			nameClick.add( "Open blue message" );
			nameClick.add( "Open green message" );
			nameClick.add( "Open player profile" );
			nameClickSelect = new JComboBox( nameClick );

			colorPanel = new JPanel();
			colorPanel.setLayout( new BoxLayout( colorPanel, BoxLayout.Y_AXIS ) );
			JScrollPane scrollArea = new JScrollPane( colorPanel, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
				JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED );

			JComponentUtilities.setComponentSize( scrollArea, 240, 100 );

			VerifiableElement [] elements = new VerifiableElement[5];
			elements[0] = new VerifiableElement( "Font Size: ", fontSizeSelect );
			elements[1] = new VerifiableElement( "Chat Style: ", chatStyleSelect );
			elements[2] = new VerifiableElement( "Windowing: ", useTabsSelect );
			elements[3] = new VerifiableElement( "Name Clicks: ", nameClickSelect );
			elements[4] = new VerifiableElement( "Chat Colors: ", scrollArea );

			setContent( elements );
			(new LoadDefaultSettingsThread()).start();
		}

		protected void actionConfirmed()
		{	(new StoreSettingsThread()).start();
		}

		/**
		 * In order to keep the user interface from freezing (or at
		 * least appearing to freeze), this internal class is used
		 * to load the default settings.
		 */

		private class LoadDefaultSettingsThread extends OptionsThread
		{
			public void run()
			{
				// Begin by loading the font size from the user
				// settings - for backwards compatibility, this
				// may not exist yet.

				String fontSize = settings.getProperty( "fontSize" );

				if ( fontSize != null )
				{
					fontSizeSelect.setSelectedItem( Integer.valueOf( fontSize ) );
					LimitedSizeChatBuffer.setFontSize( Integer.parseInt( fontSize ) );
				}
				else
					fontSizeSelect.setSelectedItem( new Integer( 3 ) );

				// Next, load the kind of chat style the user
				// is using - again, for backwards compatibility,
				// this may not exist yet.

				String chatStyle = settings.getProperty( "chatStyle" );
				chatStyleSelect.setSelectedIndex( (chatStyle != null) ? Integer.parseInt( chatStyle ) : 0 );

				String useTabs = settings.getProperty( "useTabbedChat" );
				useTabsSelect.setSelectedIndex( (useTabs != null) ? Integer.parseInt( useTabs ) : 0 );

				String nameClick = settings.getProperty( "chatNameClick" );
				nameClickSelect.setSelectedIndex( (nameClick != null) ? Integer.parseInt( nameClick ) : 0 );

				String nameColor = settings.getProperty( "chatNameColors" );

				if ( colorPanel.getComponentCount() == 0 && nameColor != null )
				{
					String [] colors = nameColor.split( "[:;]" );
 					colorPanel.add( new PlayerColorPanel( "You", DataUtilities.toColor( colors[1] ) ) );

 					PlayerColorPanel currentPanel;
 					for ( int i = 2; i < colors.length && i < 16; i += 2 )
 					{
						currentPanel = new PlayerColorPanel( DataUtilities.toColor( colors[i+1] ) );
 						((JTextField)currentPanel.playerIDField).setText( colors[i] );
 						colorPanel.add( currentPanel );
					}

					for ( int j = colors.length; j < 16; j += 2 )
						colorPanel.add( new PlayerColorPanel() );
				}
				else if ( colorPanel.getComponentCount() == 0 )
				{
 					colorPanel.add( new PlayerColorPanel( "You" ) );
 					for ( int i = 1; i < 8; ++i )
	 					colorPanel.add( new PlayerColorPanel() );
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
			public void run()
			{
				Integer fontSize = (Integer) fontSizeSelect.getSelectedItem();
				settings.setProperty( "fontSize", fontSize.toString() );
				LimitedSizeChatBuffer.setFontSize( fontSize.intValue() );
				settings.setProperty( "chatStyle", "" + chatStyleSelect.getSelectedIndex() );
				settings.setProperty( "useTabbedChat", "" + useTabsSelect.getSelectedIndex() );
				settings.setProperty( "chatNameClick", "" + nameClickSelect.getSelectedIndex() );

				if ( client.getMessenger() != null )
					client.getMessenger().setTabbedFrameSetting( useTabsSelect.getSelectedIndex() == 1 );

				PlayerColorPanel currentPanel = (PlayerColorPanel) colorPanel.getComponent(0);
				StringBuffer nameColor = new StringBuffer();

				nameColor.append( "0:" );
				nameColor.append( DataUtilities.toHexString( currentPanel.selectedColor ) );

				for ( int i = 1; i < colorPanel.getComponentCount(); ++i )
				{
					currentPanel = (PlayerColorPanel) colorPanel.getComponent( i );
					String playerID = ((JTextField)currentPanel.playerIDField).getText().trim().replaceAll( "[\\[\\]\\#]", "" );

					if ( playerID.length() > 0 )
					{
						try
						{
							Integer.parseInt( playerID );
							nameColor.append( ';' );
							nameColor.append( playerID );
							nameColor.append( ':' );
							nameColor.append( DataUtilities.toHexString( currentPanel.selectedColor ) );
						}
						catch ( Exception e )
						{
							// If an exception is caught, then it was not
							// a valid player ID.
						}
					}
				}

				settings.setProperty( "chatNameColors", nameColor.toString() );
				LimitedSizeChatBuffer.setChatColors( nameColor.toString() );
				saveSettings();
			}
		}
	}

	/**
	 * An internal class used for handling mall options.  This includes
	 * default mall limiting, mall sorting and sending items to the mall.
	 */

	private class ResultsOptionsPanel extends OptionsPanel
	{
		private JTextField defaultLimitField;
		private JComboBox forceSortSelect;
		private JComboBox useClosetForCreationSelect;
		private JComboBox autoRepairBoxesSelect;

		public ResultsOptionsPanel()
		{
			defaultLimitField = new JTextField( "13" );

			LockableListModel forceSorting = new LockableListModel();
			forceSorting.add( "No Sorting" );
			forceSorting.add( "Force Price Sort" );

			forceSortSelect = new JComboBox( forceSorting );

			LockableListModel useClosetForCreation = new LockableListModel();
			useClosetForCreation.add( "Inventory only" );
			useClosetForCreation.add( "Closet and inventory" );

			useClosetForCreationSelect = new JComboBox( useClosetForCreation );

			LockableListModel autoRepairBoxes = new LockableListModel();
			autoRepairBoxes.add( "Halt on explosion" );
			autoRepairBoxes.add( "Auto-repair on explosion" );

			autoRepairBoxesSelect = new JComboBox( autoRepairBoxes );

			VerifiableElement [] elements = new VerifiableElement[4];
			elements[0] = new VerifiableElement( "Default Limit: ", defaultLimitField );
			elements[1] = new VerifiableElement( "Sorting Style: ", forceSortSelect );
			elements[2] = new VerifiableElement( "Ingredient Source: ", useClosetForCreationSelect );
			elements[3] = new VerifiableElement( "Auto-Repair: ", autoRepairBoxesSelect );

			setContent( elements );
			(new LoadDefaultSettingsThread()).start();
		}

		protected void actionConfirmed()
		{	(new StoreSettingsThread()).start();
		}

		/**
		 * In order to keep the user interface from freezing (or at
		 * least appearing to freeze), this internal class is used
		 * to load the default settings.
		 */

		private class LoadDefaultSettingsThread extends OptionsThread
		{
			public void run()
			{
				String defaultLimitSetting = settings.getProperty( "defaultLimit" );
				String forceSortSetting = settings.getProperty( "forceSorting" );
				String useClosetForCreationSetting = settings.getProperty( "useClosetForCreation" );
				String autoRepairBoxesSetting = settings.getProperty( "autoRepairBoxes" );

				// If there are no default settings, simply skip the
				// attempt at loading them.

				defaultLimitField.setText( defaultLimitSetting == null ? "13" : defaultLimitSetting );

				if ( forceSortSetting == null || forceSortSetting.equals( "false" ) )
					forceSortSelect.setSelectedIndex( 0 );
				else
					forceSortSelect.setSelectedIndex( 1 );

 				if ( useClosetForCreationSetting == null || useClosetForCreationSetting.equals( "false" ) )
					useClosetForCreationSelect.setSelectedIndex( 0 );
				else
					useClosetForCreationSelect.setSelectedIndex( 1 );

				if ( autoRepairBoxesSetting == null || autoRepairBoxesSetting.equals( "false" ) )
					autoRepairBoxesSelect.setSelectedIndex( 0 );
				else
					autoRepairBoxesSelect.setSelectedIndex( 1 );

				setStatusMessage( ENABLED_STATE, "Settings loaded." );
			}
		}

		/**
		 * In order to keep the user interface from freezing (or at
		 * least appearing to freeze), this internal class is used
		 * to store the new settings.
		 */

		private class StoreSettingsThread extends OptionsThread
		{
			public void run()
			{
				settings.setProperty( "defaultLimit", defaultLimitField.getText().length() == 0 ? "13" : defaultLimitField.getText() );
				settings.setProperty( "forceSorting", "" + (forceSortSelect.getSelectedIndex() == 1) );
				settings.setProperty( "useClosetForCreation", "" + (useClosetForCreationSelect.getSelectedIndex() == 1) );
				settings.setProperty( "autoRepairBoxes", "" + (autoRepairBoxesSelect.getSelectedIndex() == 1) );
				saveSettings();
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
			(new LoadDefaultSettingsThread()).run();
		}

		protected void actionConfirmed()
		{	(new StoreSettingsThread()).start();
		}

		/**
		 * In order to keep the user interface from freezing (or at
		 * least appearing to freeze), this internal class is used
		 * to load the default settings.
		 */

		private class LoadDefaultSettingsThread extends OptionsThread
		{
			public void run()
			{
				if ( client == null )
					System.setProperty( "proxySet", "false" );

				if ( settings.getProperty( "proxySet" ) != null && settings.getProperty( "proxySet" ).equals( "true" ) )
				{
					proxyHost.setText( settings.getProperty( "http.proxyHost" ) );
					proxyPort.setText( settings.getProperty( "http.proxyPort" ) );
					proxyLogin.setText( settings.getProperty( "http.proxyUser" ) );
					proxyPassword.setText( settings.getProperty( "http.proxyPassword" ) );
				}
				else
				{
					proxyHost.setText( "" );
					proxyPort.setText( "" );
					proxyLogin.setText( "" );
					proxyPassword.setText( "" );
				}

				setStatusMessage( ENABLED_STATE, "" );
			}
		}

		/**
		 * In order to keep the user interface from freezing (or at
		 * least appearing to freeze), this internal class is used
		 * to store the new settings.
		 */

		private class StoreSettingsThread extends OptionsThread
		{
			public void run()
			{
				if ( proxyHost.getText().trim().length() != 0 )
				{
					settings.setProperty( "proxySet", "true" );
					settings.setProperty( "http.proxyHost", proxyHost.getText() );
					settings.setProperty( "http.proxyPort", proxyPort.getText() );

					if ( proxyLogin.getText().trim().length() != 0 )
					{
						settings.setProperty( "http.proxyUser", proxyLogin.getText() );
						settings.setProperty( "http.proxyPassword", proxyPassword.getText() );
					}
					else
					{
						settings.remove( "http.proxyUser" );
						settings.remove( "http.proxyPassword" );
					}
				}
				else
				{
					settings.setProperty( "proxySet", "false" );
					settings.remove( "http.proxyHost" );
					settings.remove( "http.proxyPort" );
					settings.remove( "http.proxyUser" );
					settings.remove( "http.proxyPassword" );
				}

				// Save the settings that were just set; that way,
				// the next login can use them.

				saveSettings();
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
		protected Properties settings;

		public OptionsPanel()
		{	this( new Dimension( 120, 20 ), new Dimension( 240, 20 ) );
		}

		public OptionsPanel( Dimension left, Dimension right )
		{	this( null, left, right );
		}

		public OptionsPanel( String panelTitle, Dimension left, Dimension right )
		{
			super( panelTitle, left, right );
			settings = (client == null) ? System.getProperties() : client.getSettings();
		}

		protected void saveSettings()
		{
			if ( settings instanceof KoLSettings )
				((KoLSettings)settings).saveSettings();
			setStatusMessage( ENABLED_STATE, "Settings saved." );

			Object waitObject = new Object();
			try
			{
				synchronized ( waitObject )
				{
					waitObject.wait( 5000 );
					waitObject.notifyAll();
				}
			}
			catch ( InterruptedException e )
			{
			}

			setStatusMessage( ENABLED_STATE, "" );
		}

		protected abstract class OptionsThread extends Thread
		{
			public OptionsThread()
			{	setDaemon( true );
			}
		}
	}

	/**
	 * Internal class which represents the color being used
	 * for a single player in chat.
	 */

	private class PlayerColorPanel extends JPanel
	{
		private Color selectedColor;
		private JButton colorSelect;
		private JComponent playerIDField;

		public PlayerColorPanel()
		{	this( Color.black );
		}

		public PlayerColorPanel( Color c )
		{
			selectedColor = c;
			setLayout( new BorderLayout( 5, 5 ) );

			colorSelect = new JButton();
			JComponentUtilities.setComponentSize( colorSelect, 20, 20 );
			colorSelect.setBackground( selectedColor );
			colorSelect.addActionListener( new ChatColorChanger() );

			add( colorSelect, BorderLayout.WEST );

			playerIDField = new JTextField( "[######]" );
			add( playerIDField, BorderLayout.CENTER );
			JComponentUtilities.setComponentSize( this, 200, 20 );
		}

		public PlayerColorPanel( String label )
		{	this( label, Color.black );
		}

		public PlayerColorPanel( String label, Color c )
		{
			selectedColor = c;
			setLayout( new BorderLayout( 5, 5 ) );

			colorSelect = new JButton();
			JComponentUtilities.setComponentSize( colorSelect, 20, 20 );
			colorSelect.setBackground( selectedColor );
			colorSelect.addActionListener( new ChatColorChanger() );

			add( colorSelect, BorderLayout.WEST );

			playerIDField = new JLabel( label, JLabel.LEFT );
			add( playerIDField, BorderLayout.CENTER );
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
				selectedColor = JColorChooser.showDialog( OptionsFrame.this, "Choose color...", selectedColor );
				colorSelect.setBackground( selectedColor );
				playerIDField.setForeground( selectedColor );
				playerIDField.requestFocus();
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