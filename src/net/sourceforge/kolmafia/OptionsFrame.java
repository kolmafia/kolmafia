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

// containers
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
import javax.swing.JRadioButton;
import javax.swing.ButtonGroup;

// utilities
import java.util.Properties;
import java.util.StringTokenizer;
import net.java.dev.spellcast.utilities.LockableListModel;

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
		super( "KoLmafia: " + ((client == null) ? "UI Test" :
			(client.getLoginName() == null) ? "Global" : client.getLoginName()) + " Preferences", client );
		setResizable( false );

		getContentPane().setLayout( new CardLayout( 10, 10 ) );

		JTabbedPane tabs = new JTabbedPane();

		// Because none of the frames support setStatusMessage,
		// the content panel is arbitrary

		this.client = client;
		contentPanel = null;

		tabs.addTab( "Login", new LoginOptionsPanel() );
		tabs.addTab( "Battle", new BattleOptionsPanel() );
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
		private JComboBox serverSelect;
		private JTextField proxyHost;
		private JTextField proxyPort;
		private JTextField proxyLogin;
		private JTextField proxyPassword;

		/**
		 * Constructs a new <code>LoginOptionsPanel</code>, containing a
		 * place for the users to select their desired server and for them
		 * to modify any applicable proxy settings.
		 */

		public LoginOptionsPanel()
		{
			super( new Dimension( 120, 20 ), new Dimension( 165, 20 ) );

			LockableListModel servers = new LockableListModel();
			servers.add( "(Auto Detect)" );
			servers.add( "Use Login Server 1" );
			servers.add( "Use Login Server 2" );
			servers.add( "Use Login Server 3" );

			serverSelect = new JComboBox( servers );
			proxyHost = new JTextField();
			proxyPort = new JTextField();
			proxyLogin = new JTextField();
			proxyPassword = new JPasswordField();

			(new LoadDefaultSettingsThread()).run();

			VerifiableElement [] elements = new VerifiableElement[5];
			elements[0] = new VerifiableElement( "KoL Server: ", serverSelect );
			elements[1] = new VerifiableElement( "Proxy Host: ", proxyHost );
			elements[2] = new VerifiableElement( "Proxy Port: ", proxyPort );
			elements[3] = new VerifiableElement( "Proxy Login: ", proxyLogin );
			elements[4] = new VerifiableElement( "Proxy Password: ", proxyPassword );

			setContent( elements );
		}

		public void clear()
		{	(new LoadDefaultSettingsThread()).run();
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
				{
					System.setProperty( "loginServer", "0" );
					System.setProperty( "proxySet", "false" );
				}

				serverSelect.setSelectedIndex( Integer.parseInt( settings.getProperty( "loginServer" ) ) );

				if ( settings.getProperty( "proxySet" ).equals( "true" ) )
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

				(new StatusMessageChanger( "" )).run();
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

				// Next, change the server that's used to login;
				// find out the selected index.

				settings.setProperty( "loginServer", "" + serverSelect.getSelectedIndex() );

				// Save the settings that were just set; that way,
				// the next login can use them.

				if ( settings instanceof KoLSettings )
					((KoLSettings)settings).saveSettings();

				KoLRequest.applySettings();
				(new StatusMessageChanger( "Settings saved." )).run();
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
		private final String [] actionnames = { "attack", "moxman" };
		private final String [] actiondescs = { "Attack with Weapon", "Moxious Maneuver" };

		private ButtonGroup actionGroup;
		private JRadioButton [] actions;

		/**
		 * Constructs a new <code>BattleOptionsPanel</code> containing a
		 * way for the users to choose the way they want to fight battles
		 * encountered during adventuring.
		 */

		public BattleOptionsPanel()
		{
			super( new Dimension( 300, 20 ), new Dimension( 20, 20 ) );

			actions = new JRadioButton[ actionnames.length ];
			actionGroup = new ButtonGroup();

			for ( int i = 0; i < actions.length; ++i )
			{
				actions[i] = new JRadioButton();
				actionGroup.add( actions[i] );
			}

			(new LoadDefaultSettingsThread()).run();

			VerifiableElement [] elements = new VerifiableElement[ actions.length ];
			for ( int i = 0; i < actions.length; ++i )
				elements[i] = new VerifiableElement( actiondescs[i], JLabel.LEFT, actions[i] );

			java.util.Arrays.sort( elements );
			setContent( elements );
		}

		public void clear()
		{	(new LoadDefaultSettingsThread()).start();
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
				String battleSettings = (client == null) ? null :
					settings.getProperty( "battleAction" );

				// If there are no default settings, simply skip the
				// attempt at loading them.

				if ( battleSettings == null )
					return;

				// If there are default settings, make sure that the
				// appropriate radio box is checked.

				for ( int i = 0; i < actions.length; ++i )
					actions[i].setSelected( false );

				for ( int i = 0; i < actions.length; ++i )
					if ( actionnames[i].equals( battleSettings ) )
						actions[i].setSelected( true );

				(new StatusMessageChanger( "" )).run();
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
				if ( client != null )
				{
					for ( int i = 0; i < actions.length; ++i )
						if ( actions[i].isSelected() )
							settings.setProperty( "battleAction", actionnames[i] );

					if ( settings instanceof KoLSettings )
						((KoLSettings)settings).saveSettings();
				}

				(new StatusMessageChanger( "Settings saved." )).run();
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

			(new LoadDefaultSettingsThread()).run();

			VerifiableElement [] elements = new VerifiableElement[ items.length ];
			for ( int i = 0; i < items.length; ++i )
				elements[i] = new VerifiableElement( itemnames[i], JLabel.LEFT, items[i] );

			java.util.Arrays.sort( elements );
			setContent( elements );
		}

		public void clear()
		{	(new LoadDefaultSettingsThread()).start();
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

				(new StatusMessageChanger( "" )).run();
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
					(new StatusMessageChanger( "You did not select exactly three items." )).run();
					return;
				}

				if ( client != null )
				{
					settings.setProperty( "luckySewer", selected[0] + "," + selected[1] + "," + selected[2] );
					if ( settings instanceof KoLSettings )
						((KoLSettings)settings).saveSettings();
				}

				(new StatusMessageChanger( "Settings saved." )).run();
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
		public ChatOptionsPanel()
		{
			super( new Dimension( 200, 20 ), new Dimension( 20, 20 ) );
		}

		public void clear()
		{	(new LoadDefaultSettingsThread()).start();
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
			}
		}
	}

	/**
	 * A generic panel which adds a label to the bottom of the KoLPanel
	 * to update the panel's status.  It also provides a thread which is
	 * guaranteed to be a daemon thread for updating the frame which
	 * also retrieves a reference to the client's current settings.
	 */

	private abstract class OptionsPanel extends KoLPanel
	{
		private JPanel actionStatusPanel;
		private JLabel actionStatusLabel;

		public OptionsPanel( Dimension left, Dimension right )
		{
			super( "apply", "defaults", left, right );

			actionStatusPanel = new JPanel();
			actionStatusPanel.setLayout( new GridLayout( 2, 1 ) );

			actionStatusLabel = new JLabel( " ", JLabel.CENTER );
			actionStatusPanel.add( actionStatusLabel );
			actionStatusPanel.add( new JLabel( " ", JLabel.CENTER ) );
		}

		public void setContent( VerifiableElement [] elements )
		{	setContent( elements, false );
		}

		public void setContent( VerifiableElement [] elements, boolean isLabelPreceeding )
		{
			super.setContent( elements, isLabelPreceeding );
			add( actionStatusPanel, BorderLayout.SOUTH );
		}

		public void setStatusMessage( String s )
		{	actionStatusLabel.setText( s );
		}

		protected void actionCancelled()
		{	clear();
		}

		protected abstract class OptionsThread extends Thread
		{
			protected Properties settings;

			public OptionsThread()
			{
				setDaemon( true );
				settings = (client == null) ? System.getProperties() : client.getSettings();
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