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
import javax.swing.Box;
import javax.swing.BoxLayout;
import java.awt.CardLayout;
import java.awt.BorderLayout;
import javax.swing.SpringLayout;

// event listeners
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

// containers
import javax.swing.JTabbedPane;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.JPasswordField;
import javax.swing.JComboBox;
import javax.swing.JCheckBox;
import javax.swing.JRadioButton;
import javax.swing.ButtonGroup;
import javax.swing.JScrollPane;

// other imports
import java.util.ArrayList;
import com.sun.java.forums.SpringUtilities;
import net.java.dev.spellcast.utilities.LockableListModel;
import net.java.dev.spellcast.utilities.JComponentUtilities;

/**
 * An extended <code>KoLFrame</code> which presents the user with the ability to
 * login to the Kingdom of Loathing.  Essentially, this class is a modification
 * of the <code>LoginDialog</code> class from the Spellcast project.
 */

public class LoginFrame extends KoLFrame
{
	private JComponent usernameField;
	private JTextField proxyHost;
	private JTextField proxyPort;
	private JTextField proxyLogin;
	private JTextField proxyPassword;

	public LoginFrame()
	{
		super( VERSION_NAME + ": Login" );
		tabs = new JTabbedPane();

		tabs.addTab( "KoL Login", constructLoginPanel() );

		JPanel breakfastPanel = new JPanel();
		breakfastPanel.setLayout( new BoxLayout( breakfastPanel, BoxLayout.Y_AXIS ) );

		breakfastPanel.add( new BreakfastPanel( "Softcore Characters", "Softcore" ) );
		breakfastPanel.add( new BreakfastPanel( "Hardcore Characters", "Hardcore" ) );
		tabs.addTab( "Breakfast", breakfastPanel );

		JPanel displayPanel = new JPanel();
		displayPanel.setLayout( new BoxLayout( displayPanel, BoxLayout.Y_AXIS ) );
		displayPanel.add( new UserInterfacePanel() );
		displayPanel.add( new StartupFramesPanel() );

		JScrollPane scroller = new JScrollPane( displayPanel,
			JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER );

		JComponentUtilities.setComponentSize( scroller, 300, 300 );

		tabs.addTab( "Displays", scroller );

		JPanel connectPanel = new JPanel();
		connectPanel.setLayout( new BoxLayout( connectPanel, BoxLayout.Y_AXIS ) );
		connectPanel.add( new ConnectionOptionsPanel() );
		connectPanel.add( new ProxyOptionsPanel() );

		tabs.addTab( "Connection", connectPanel );

		framePanel.setLayout( new CardLayout( 10, 10 ) );
		framePanel.add( tabs, "" );
	}

	public void requestFocus()
	{
		super.requestFocus();
		if ( usernameField != null )
			usernameField.requestFocus();
	}

	public void dispose()
	{
		if ( existingFrames.size() == 1 )
			SystemTrayFrame.removeTrayIcon();

		super.dispose();
	}

	public JPanel constructLoginPanel()
	{
		JPanel imagePanel = new JPanel( new BorderLayout( 0, 0 ) );
		imagePanel.add( new JLabel( " " ), BorderLayout.NORTH );
		imagePanel.add( new JLabel( JComponentUtilities.getImage( "limeglass.jpg" ), JLabel.CENTER ), BorderLayout.SOUTH );

		JPanel containerPanel = new JPanel( new BorderLayout() );
		containerPanel.add( imagePanel, BorderLayout.NORTH );
		containerPanel.add( new LoginPanel(), BorderLayout.CENTER );
		return containerPanel;
	}

	/**
	 * An internal class which represents the panel which is nested
	 * inside of the <code>LoginFrame</code>.
	 */

	private class LoginPanel extends KoLPanel
	{
		private JPasswordField passwordField;
		private ScriptSelectPanel scriptField;
		private JCheckBox savePasswordCheckBox;
		private JCheckBox autoLoginCheckBox;
		private JCheckBox getBreakfastCheckBox;

		/**
		 * Constructs a new <code>LoginPanel</code>, containing a place
		 * for the users to input their login name and password.  This
		 * panel, because it is intended to be the content panel for
		 * status message updates, also has a status label.
		 */

		public LoginPanel()
		{
			super( "login", "cancel" );

			usernameField = saveStateNames.isEmpty() ? (JComponent)(new JTextField()) : (JComponent)(new LoginNameComboBox());
			passwordField = new JPasswordField();
			scriptField = new ScriptSelectPanel( new JTextField() );

			savePasswordCheckBox = new JCheckBox();

			autoLoginCheckBox = new JCheckBox();
			getBreakfastCheckBox = new JCheckBox();

			JPanel checkBoxPanels = new JPanel();
			checkBoxPanels.add( Box.createHorizontalStrut( 16 ) );
			checkBoxPanels.add( new JLabel( "Save Password: " ), "" );
			checkBoxPanels.add( savePasswordCheckBox );
			checkBoxPanels.add( Box.createHorizontalStrut( 16 ) );
			checkBoxPanels.add( new JLabel( "Auto-Login: " ), "" );
			checkBoxPanels.add( autoLoginCheckBox );
			checkBoxPanels.add( Box.createHorizontalStrut( 16 ) );
			checkBoxPanels.add( new JLabel( "Breakfast: " ), "" );
			checkBoxPanels.add( getBreakfastCheckBox );
			checkBoxPanels.add( Box.createHorizontalStrut( 16 ) );

			VerifiableElement [] elements = new VerifiableElement[3];
			elements[0] = new VerifiableElement( "Login: ", usernameField );
			elements[1] = new VerifiableElement( "Password: ", passwordField );
			elements[2] = new VerifiableElement( "On Login: ", scriptField );

			setContent( elements );

			actionStatusPanel.add( new JLabel( " ", JLabel.CENTER ), BorderLayout.CENTER );
			actionStatusPanel.add( checkBoxPanels, BorderLayout.NORTH );

			String autoLoginSetting = StaticEntity.getProperty( "autoLogin" );
			if ( autoLoginSetting.equals( "" ) )
				autoLoginSetting = StaticEntity.getProperty( "lastUsername" );
			else
				autoLoginCheckBox.setSelected( true );

			if ( usernameField instanceof JComboBox )
				((JComboBox)usernameField).setSelectedItem( autoLoginSetting );

			String passwordSetting = StaticEntity.getClient().getSaveState( autoLoginSetting );

			if ( passwordSetting != null )
			{
				passwordField.setText( passwordSetting );
				savePasswordCheckBox.setSelected( true );
			}

			getBreakfastCheckBox.setSelected( StaticEntity.getBooleanProperty( "alwaysGetBreakfast" ) );
			setDefaultButton( confirmedButton );

			autoLoginCheckBox.addActionListener( new AutoLoginListener() );
			savePasswordCheckBox.addActionListener( new RemovePasswordListener() );
		}

		public void setEnabled( boolean isEnabled )
		{
			if ( usernameField == null || passwordField == null || scriptField == null )
				return;

			if ( savePasswordCheckBox == null || autoLoginCheckBox == null || getBreakfastCheckBox == null )
				return;

			super.setEnabled( isEnabled );

			usernameField.setEnabled( isEnabled );
			passwordField.setEnabled( isEnabled );
			scriptField.setEnabled( isEnabled );

			savePasswordCheckBox.setEnabled( isEnabled );
			autoLoginCheckBox.setEnabled( isEnabled );
			getBreakfastCheckBox.setEnabled( isEnabled );
		}

		public void actionConfirmed()
		{
			StaticEntity.setProperty( "alwaysGetBreakfast", String.valueOf( getBreakfastCheckBox.isSelected() ) );
			String username = ((String)(usernameField instanceof JComboBox ?
				((JComboBox)usernameField).getSelectedItem() : ((JTextField)usernameField).getText() ));

			String password = new String( passwordField.getPassword() );

			if ( username == null || password == null || username.equals("") || password.equals("") )
			{
				setStatusMessage( ERROR_STATE, "Invalid login." );
				return;
			}

			if ( autoLoginCheckBox.isSelected() )
				StaticEntity.setProperty( "autoLogin", username );
			else
				StaticEntity.setProperty( "autoLogin", "" );

			StaticEntity.setGlobalProperty( username, "loginScript", scriptField.getText() );
			StaticEntity.setGlobalProperty( username, "getBreakfast", String.valueOf( getBreakfastCheckBox.isSelected() ) );

			KoLmafia.forceContinue();
			(new Thread( new LoginRequest( username, password, savePasswordCheckBox.isSelected(), getBreakfastCheckBox.isSelected(), false ) )).start();
		}

		public void actionCancelled()
		{	KoLmafia.declareWorldPeace();
		}

		private class AutoLoginListener implements ActionListener
		{
			public void actionPerformed( ActionEvent e )
			{
				if ( autoLoginCheckBox.isSelected() )
					actionConfirmed();
				else
					StaticEntity.setProperty( "autoLogin", "" );
			}
		}

		private class RemovePasswordListener implements ActionListener
		{
			public void actionPerformed( ActionEvent e )
			{
				if ( !savePasswordCheckBox.isSelected() && usernameField instanceof JComboBox )
				{
					String value = (String) ((JComboBox)usernameField).getSelectedItem();

					saveStateNames.remove( value );
					KoLmafia.removeSaveState( value );
					passwordField.setText( "" );
				}
			}
		}

		/**
		 * Special instance of a JComboBox which overrides the default
		 * key events of a JComboBox to allow you to catch key events.
		 */

		/**
		 * Special instance of a JComboBox which overrides the default
		 * key events of a JComboBox to allow you to catch key events.
		 */

		private class LoginNameComboBox extends MutableComboBox
		{
			public LoginNameComboBox()
			{	super( saveStateNames );
			}

			public void setSelectedItem( Object anObject )
			{
				super.setSelectedItem( anObject );
				setPassword();
			}

			protected void findMatch( int keyCode )
			{
				super.findMatch( keyCode );
				setPassword();
			}

			private void setPassword()
			{
				if ( currentMatch == null )
				{
					passwordField.setText( "" );
					setStatusMessage( " " );

					LoginPanel.this.setEnabled( true );
					return;
				}

				String password = StaticEntity.getClient().getSaveState( currentMatch );
				if ( password == null )
				{
					passwordField.setText( "" );
					setStatusMessage( " " );

					LoginPanel.this.setEnabled( true );
					return;
				}

				passwordField.setText( password );
				savePasswordCheckBox.setSelected( true );

				String loginScript = StaticEntity.getGlobalProperty( currentMatch, "loginScript" );
				boolean breakfastSetting = StaticEntity.getGlobalProperty( currentMatch, "getBreakfast" ).equals( "true" );

				scriptField.setText( loginScript == null ? "" : loginScript );
				getBreakfastCheckBox.setSelected( breakfastSetting );
				LoginPanel.this.setEnabled( true );
			}
		}
	}

	private class BreakfastPanel extends LabeledKoLPanel
	{
		private String breakfastType;
		private JCheckBox [] skillOptions;

		public BreakfastPanel( String title, String breakfastType )
		{
			super( title, new Dimension( 380, 20 ), new Dimension( 20, 20 ) );

			this.breakfastType = breakfastType;
			skillOptions = new JCheckBox[ KoLmafia.BREAKFAST_SKILLS.length ];
			for ( int i = 0; i < KoLmafia.BREAKFAST_SKILLS.length; ++i )
				skillOptions[i] = new JCheckBox();

			VerifiableElement [] elements = new VerifiableElement[ skillOptions.length ];

			for ( int i = 0; i < elements.length; ++i )
				elements[i] = new VerifiableElement( KoLmafia.BREAKFAST_SKILLS[i][0], JLabel.LEFT, skillOptions[i] );

			setContent( elements, false );
			actionCancelled();
		}

		public void actionConfirmed()
		{
			StringBuffer skillString = new StringBuffer();

			for ( int i = 0; i < KoLmafia.BREAKFAST_SKILLS.length; ++i )
			{
				if ( skillOptions[i].isSelected() )
				{
					if ( skillString.length() != 0 )
						skillString.append( "," );

					skillString.append( KoLmafia.BREAKFAST_SKILLS[i][0] );
				}
			}

			StaticEntity.setProperty( "breakfast" + breakfastType, skillString.toString() );
		}

		public void actionCancelled()
		{
			String skillString = StaticEntity.getProperty( "breakfast" + breakfastType );
			for ( int i = 0; i < KoLmafia.BREAKFAST_SKILLS.length; ++i )
				skillOptions[i].setSelected( skillString != null && skillString.indexOf( KoLmafia.BREAKFAST_SKILLS[i][0] ) != -1 );
		}
	}

	private class StartupFramesPanel extends LabeledKoLPanel
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
			{ "Hagnk's Storage", "HagnkStorageFrame" },

			{ "Hall of Legends", "MeatManageFrame" },
			{ "Skill Casting", "SkillBuffFrame" },

			{ "Buffbot Manager", "BuffBotFrame" },
			{ "Purchase Buffs", "BuffRequestFrame" },

			{ "Flower Hunter", "FlowerHunterFrame" },
			{ "Mushroom Plot", "MushroomFrame" },
			{ "Familiar Trainer", "FamiliarTrainingFrame" },

			{ "IcePenguin Express", "MailboxFrame" },
			{ "KoLmafia Chat", "KoLMessenger" },
			{ "Recent Events", "EventsFrame" },

			{ "Clan Management", "ClanManageFrame" },
			{ "Farmer's Almanac", "CalendarFrame" },
			{ "Internal Database", "ExamineItemsFrame" },

			{ "Coin Toss Game", "MoneyMakingGameFrame" },
			{ "Preferences", "OptionsFrame" }
		};

		private JComboBox usernameComboBox;
		private InterfaceRadioButton [] nullOptions;
		private InterfaceRadioButton [] startupOptions;
		private InterfaceRadioButton [] interfaceOptions;

		public StartupFramesPanel()
		{
			super( "Startup Windows", new Dimension( 100, 20 ), new Dimension( 200, 20 ) );

			nullOptions = new InterfaceRadioButton[ FRAME_OPTIONS.length ];
			startupOptions = new InterfaceRadioButton[ FRAME_OPTIONS.length ];
			interfaceOptions = new InterfaceRadioButton[ FRAME_OPTIONS.length ];

			JPanel contentPanel = new JPanel( new SpringLayout() );

			for ( int i = 0; i < FRAME_OPTIONS.length; ++i )
			{
				nullOptions[i] = new InterfaceRadioButton( "manual" );
				startupOptions[i] = new InterfaceRadioButton( "startup" );
				interfaceOptions[i] = new InterfaceRadioButton( "as tab" );

				ButtonGroup holder = new ButtonGroup();
				holder.add( nullOptions[i] );
				holder.add( startupOptions[i] );
				holder.add( interfaceOptions[i] );

				contentPanel.add( new JLabel( FRAME_OPTIONS[i][0] + ": ", JLabel.RIGHT ) );
				contentPanel.add( nullOptions[i] );
				contentPanel.add( startupOptions[i] );

				if ( FRAME_OPTIONS[i][1].equals( "LocalRelayServer" ) )
					interfaceOptions[i].setEnabled( false );

				contentPanel.add( interfaceOptions[i] );
			}

			usernameComboBox = new JComboBox( saveStateNames );

			String autoLoginSetting = StaticEntity.getProperty( "autoLogin" );
			if ( autoLoginSetting.equals( "" ) )
				autoLoginSetting = StaticEntity.getProperty( "lastUsername" );

			usernameComboBox.setSelectedItem( autoLoginSetting.toLowerCase() );
			usernameComboBox.addActionListener( new SettingsReloader() );

			VerifiableElement [] elements = new VerifiableElement[1];
			elements[0] = new VerifiableElement( "Settings For:  ", usernameComboBox );

			setContent( elements );
			SpringUtilities.makeCompactGrid( contentPanel, FRAME_OPTIONS.length, 4, 5, 5, 5, 5 );
			container.add( contentPanel, BorderLayout.SOUTH );
			actionCancelled();
		}

		public void actionConfirmed()
		{
		}

		public void actionCancelled()
		{
			String username = (String) usernameComboBox.getSelectedItem();
			if ( username == null )
				username = "";

			String frameString = StaticEntity.getGlobalProperty( username, "initialFrames" );
			String desktopString = StaticEntity.getGlobalProperty( username, "initialDesktop" );

			if ( frameString.equals( "" ) && desktopString.equals( "" ) )
			{
				frameString = StaticEntity.getGlobalProperty( "", "initialFrames" );
				desktopString = StaticEntity.getGlobalProperty( "", "initialDesktop" );
			}

			for ( int i = 0; i < FRAME_OPTIONS.length; ++i )
			{
				if ( frameString.indexOf( FRAME_OPTIONS[i][1] ) != -1 )
					startupOptions[i].setSelected( true );
				else if ( desktopString.indexOf( FRAME_OPTIONS[i][1] ) != -1 )
					interfaceOptions[i].setSelected( true );
				else
					nullOptions[i].setSelected( true );
			}
		}

		protected boolean shouldAddStatusLabel( VerifiableElement [] elements )
		{	return false;
		}

		private class InterfaceRadioButton extends JRadioButton implements ActionListener
		{
			public InterfaceRadioButton( String text )
			{
				super( text );
				addActionListener( this );
			}

			public void actionPerformed( ActionEvent e )
			{
				StringBuffer frameString = new StringBuffer();
				StringBuffer desktopString = new StringBuffer();

				for ( int i = 0; i < FRAME_OPTIONS.length; ++i )
				{
					if ( startupOptions[i].isSelected() )
					{
						if ( frameString.length() != 0 )
							frameString.append( "," );
						frameString.append( FRAME_OPTIONS[i][1] );
					}

					if ( interfaceOptions[i].isSelected() )
					{
						if ( desktopString.length() != 0 )
							desktopString.append( "," );
						desktopString.append( FRAME_OPTIONS[i][1] );
					}
				}

				StaticEntity.setGlobalProperty( "", "initialFrames", frameString.toString() );
				StaticEntity.setGlobalProperty( "", "initialDesktop", desktopString.toString() );

				if ( usernameComboBox.getItemCount() != 0 )
				{
					String username = (String) usernameComboBox.getSelectedItem();
					StaticEntity.setGlobalProperty( username, "initialFrames", frameString.toString() );
					StaticEntity.setGlobalProperty( username, "initialDesktop", desktopString.toString() );
				}
			}
		}

		private class SettingsReloader implements ActionListener
		{
			public void actionPerformed( ActionEvent e )
			{	actionCancelled();
			}
		}
	}

	/**
	 * Allows the user to select to select the framing mode to use.
	 */

	private class UserInterfacePanel extends OptionsPanel
	{
		private JCheckBox [] optionBoxes;

		private final String [][] options =
		{
			{ "useTextHeavySidepane", "Show detailed information in sidepane" },
			{ "guiUsesOneWindow", "Restrict interface to a single window" },
			{ "desiredLookAndFeelTitle", "Use title bar for selected Java L&F" },
			{ "useSystemTrayIcon", "Minimize to system tray (Windows only)" }
		};

		private JComboBox looks, toolbars, scripts;

		public UserInterfacePanel()
		{
			super( "Look and Feel", new Dimension( 80, 20 ), new Dimension( 280, 20 ) );

			javax.swing.UIManager.LookAndFeelInfo [] installed = javax.swing.UIManager.getInstalledLookAndFeels();
			Object [] installedLooks = new Object[ installed.length ];

			for ( int i = 0; i < installedLooks.length; ++i )
				installedLooks[i] = installed[i].getClassName();

			looks = new JComboBox( installedLooks );

			toolbars = new JComboBox();
			toolbars.addItem( "Show global menus only" );
			toolbars.addItem( "Put toolbar along top of panel" );
			toolbars.addItem( "Put toolbar along bottom of panel" );
			toolbars.addItem( "Put toolbar along left of panel" );

			scripts = new JComboBox();
			scripts.addItem( "Do not show script bar on main interface" );
			scripts.addItem( "Put script bar after normal toolbar" );
			scripts.addItem( "Put script bar along right of panel" );

			VerifiableElement [] elements = new VerifiableElement[3];
			elements[0] = new VerifiableElement( "Java L&F: ", looks );
			elements[1] = new VerifiableElement( "Toolbar: ", toolbars );
			elements[2] = new VerifiableElement( "Scripts: ", scripts );

			setContent( elements );
			actionCancelled();
		}

		protected boolean shouldAddStatusLabel( VerifiableElement [] elements )
		{	return false;
		}

		protected void setContent( VerifiableElement [] elements )
		{
			super.setContent( elements );
			container.add( new InterfaceCheckboxPanel(), BorderLayout.SOUTH );
		}

		public void actionConfirmed()
		{
			String lookAndFeel = (String) looks.getSelectedItem();
			if ( lookAndFeel != null )
				StaticEntity.setProperty( "desiredLookAndFeel", lookAndFeel );

			StaticEntity.setProperty( "useToolbars", String.valueOf( toolbars.getSelectedIndex() != 0 ) );
			StaticEntity.setProperty( "scriptButtonPosition", String.valueOf( scripts.getSelectedIndex() ) );
			StaticEntity.setProperty( "toolbarPosition", String.valueOf( toolbars.getSelectedIndex() ) );
		}

		public void actionCancelled()
		{
			looks.setSelectedItem( StaticEntity.getProperty( "desiredLookAndFeel" ) );
			toolbars.setSelectedIndex( StaticEntity.getIntegerProperty( "toolbarPosition" ) );
			scripts.setSelectedIndex( StaticEntity.getIntegerProperty( "scriptButtonPosition" ) );
		}

		private class InterfaceCheckboxPanel extends OptionsPanel
		{
			/**
			 * Constructs a new <code>StartupOptionsPanel</code>, containing a
			 * place for the users to select their desired server and for them
			 * to modify any applicable proxy settings.
			 */

			public InterfaceCheckboxPanel()
			{
				super( new Dimension( 370, 16 ), new Dimension( 20, 16 ) );
				VerifiableElement [] elements = new VerifiableElement[ options.length ];

				optionBoxes = new JCheckBox[ options.length ];
				for ( int i = 0; i < options.length; ++i )
					optionBoxes[i] = new JCheckBox();


				for ( int i = 0; i < options.length; ++i )
					elements[i] = new VerifiableElement( options[i][1], JLabel.LEFT, optionBoxes[i] );

				setContent( elements, false );
				actionCancelled();
			}

			public void actionConfirmed()
			{
				for ( int i = 0; i < options.length; ++i )
					StaticEntity.setProperty( options[i][0], String.valueOf( optionBoxes[i].isSelected() ) );

				super.actionConfirmed();
			}

			public void actionCancelled()
			{
				for ( int i = 0; i < options.length; ++i )
					optionBoxes[i].setSelected( StaticEntity.getBooleanProperty( options[i][0] ) );
			}
		}
	}

	private class ConnectionOptionsPanel extends OptionsPanel
	{
		private JCheckBox [] optionBoxes;
		private JCheckBox proxySet = null;

		private final String [][] options =
		{
			{ "useNonBlockingReader", "Use non-blocking response readers (risky)" },
			{ "autoExecuteTimeIn", "Automatically time-in whenever you're timed-out" },
			{ "proxySet", "Use a proxy to connect to the Kingdom of Loathing" }
		};

		private JComboBox servers;

		public ConnectionOptionsPanel()
		{
			super( "Connection Options", new Dimension( 80, 20 ), new Dimension( 380, 20 ) );

			servers = new JComboBox();

			servers.addItem( "Attempt to use dev.kingdomofloathing.com" );
			servers.addItem( "Attempt to use www.kingdomofloathing.com" );

			for ( int i = 2; i <= KoLRequest.SERVER_COUNT; ++i )
				servers.addItem( "Attempt to use www" + i + ".kingdomofloathing.com" );

			VerifiableElement [] elements = new VerifiableElement[1];
			elements[0] = new VerifiableElement( "Server: ", servers );

			setContent( elements );
			actionCancelled();
		}

		protected void setContent( VerifiableElement [] elements )
		{
			super.setContent( elements );
			container.add( new ConnectionCheckboxPanel(), BorderLayout.SOUTH );
		}

		public void actionConfirmed()
		{	StaticEntity.setProperty( "defaultLoginServer", String.valueOf( servers.getSelectedIndex() ) );
		}

		public void actionCancelled()
		{
			int defaultServer = StaticEntity.getIntegerProperty( "defaultLoginServer" );
			servers.setSelectedIndex( defaultServer == 0 ? 1 : defaultServer );
		}

		private class ConnectionCheckboxPanel extends OptionsPanel
		{
			public ConnectionCheckboxPanel()
			{
				super( new Dimension( 370, 16 ), new Dimension( 20, 16 ) );

				optionBoxes = new JCheckBox[ options.length ];
				for ( int i = 0; i < options.length; ++i )
					optionBoxes[i] = new JCheckBox();

				proxySet = optionBoxes[ optionBoxes.length - 1 ];
				proxyHost = new JTextField();
				proxyPort = new JTextField();
				proxyLogin = new JTextField();
				proxyPassword = new JPasswordField();

				VerifiableElement [] elements = new VerifiableElement[ options.length ];
				for ( int i = 0; i < options.length; ++i )
					elements[i] = new VerifiableElement( options[i][1], JLabel.LEFT, optionBoxes[i] );

				setContent( elements, false );
				actionCancelled();
			}

			public void actionConfirmed()
			{
				super.actionConfirmed();

				for ( int i = 0; i < optionBoxes.length; ++i )
					StaticEntity.setProperty( options[i][0], String.valueOf( optionBoxes[i].isSelected() ) );

				if ( proxySet == null )
					return;

				proxyHost.setEnabled( proxySet.isSelected() );
				proxyPort.setEnabled( proxySet.isSelected() );
				proxyLogin.setEnabled( proxySet.isSelected() );
				proxyPassword.setEnabled( proxySet.isSelected() );
			}

			public void actionCancelled()
			{
				for ( int i = 0; i < options.length; ++i )
					optionBoxes[i].setSelected( StaticEntity.getBooleanProperty( options[i][0] ) );

				if ( proxySet == null )
					return;

				proxyHost.setEnabled( proxySet.isSelected() );
				proxyPort.setEnabled( proxySet.isSelected() );
				proxyLogin.setEnabled( proxySet.isSelected() );
				proxyPassword.setEnabled( proxySet.isSelected() );
			}
		}
	}

	/**
	 * This panel handles all of the things related to proxy
	 * options (if applicable).
	 */

	private class ProxyOptionsPanel extends LabeledKoLPanel
	{
		/**
		 * Constructs a new <code>ProxyOptionsPanel</code>, containing a
		 * place for the users to select their desired server and for them
		 * to modify any applicable proxy settings.
		 */

		public ProxyOptionsPanel()
		{
			super( "Proxy Settings", new Dimension( 80, 20 ), new Dimension( 240, 20 ) );

			VerifiableElement [] elements = new VerifiableElement[4];
			elements[0] = new VerifiableElement( "Host: ", proxyHost );
			elements[1] = new VerifiableElement( "Port: ", proxyPort );
			elements[2] = new VerifiableElement( "Login: ", proxyLogin );
			elements[3] = new VerifiableElement( "Password: ", proxyPassword );

			setContent( elements, true );
			actionCancelled();
		}

		public void actionConfirmed()
		{
			StaticEntity.setProperty( "proxySet", String.valueOf( proxyHost.getText().trim().length() > 0 ) );
			StaticEntity.setProperty( "http.proxyHost", proxyHost.getText() );
			StaticEntity.setProperty( "http.proxyPort", proxyPort.getText() );
			StaticEntity.setProperty( "http.proxyUser", proxyLogin.getText() );
			StaticEntity.setProperty( "http.proxyPassword", proxyPassword.getText() );
		}

		public void actionCancelled()
		{
			proxyHost.setText( StaticEntity.getProperty( "http.proxyHost" ) );
			proxyPort.setText( StaticEntity.getProperty( "http.proxyPort" ) );
			proxyLogin.setText( StaticEntity.getProperty( "http.proxyUser" ) );
			proxyPassword.setText( StaticEntity.getProperty( "http.proxyPassword" ) );
		}
	}
}
