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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;

import java.util.Date;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.UIManager;

import tab.CloseTabPaneEnhancedUI;

import com.informit.guides.JDnDList;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import net.java.dev.spellcast.utilities.JComponentUtilities;
import net.java.dev.spellcast.utilities.LockableListModel;

public class LoginFrame extends KoLFrame
{
	private static LoginFrame INSTANCE = null;

	private String username;
	private JComboBox servers;
	private JComponent usernameField;

	private JTextField proxyHost;
	private JTextField proxyPort;
	private JTextField proxyLogin;
	private JTextField proxyPassword;

	public LoginFrame()
	{
		super( VERSION_NAME + ": Login" );

		INSTANCE = this;
		this.tabs.addTab( "KoL Login", this.constructLoginPanel() );

		JPanel breakfastPanel = new JPanel();
		breakfastPanel.setLayout( new BoxLayout( breakfastPanel, BoxLayout.Y_AXIS ) );

		breakfastPanel.add( new ScriptPanel() );
		breakfastPanel.add( new BreakfastPanel( "Softcore Characters", "Softcore" ) );
		breakfastPanel.add( new BreakfastPanel( "Hardcore Characters", "Hardcore" ) );

		this.tabs.addTab( "Main Tabs", new StartupFramesPanel() );
		this.tabs.addTab( "Look & Feel", new UserInterfacePanel()  );

		JPanel connectPanel = new JPanel();
		connectPanel.setLayout( new BoxLayout( connectPanel, BoxLayout.Y_AXIS ) );
		connectPanel.add( new ConnectionOptionsPanel() );
		connectPanel.add( new ProxyOptionsPanel() );

		this.tabs.addTab( "Connection", connectPanel );
		this.tabs.addTab( "Breakfast", breakfastPanel );

		this.framePanel.setLayout( new CardLayout( 10, 10 ) );
		this.framePanel.add( this.tabs, "" );
	}

	public boolean shouldAddStatusBar()
	{	return false;
	}

	public void requestFocus()
	{
		super.requestFocus();
		if ( this.usernameField != null )
			this.usernameField.requestFocus();
	}

	public static boolean instanceExists()
	{	return INSTANCE != null;
	}

	public static void hideInstance()
	{
		if ( INSTANCE != null )
			INSTANCE.setVisible( false );
	}

	public static void disposeInstance()
	{
		if ( INSTANCE != null )
			INSTANCE.dispose();
	}

	public void dispose()
	{
		this.honorProxySettings();

		if ( !KoLDesktop.instanceExists() )
			System.exit(0);

		super.dispose();
		INSTANCE = null;
	}

	public JPanel constructLoginPanel()
	{
		JPanel imagePanel = new JPanel( new BorderLayout( 0, 0 ) );
		imagePanel.add( new JLabel( " " ), BorderLayout.NORTH );
		imagePanel.add( new JLabel( JComponentUtilities.getImage( StaticEntity.getProperty( "loginWindowLogo" ) ), JLabel.CENTER ), BorderLayout.SOUTH );

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
			super( "login", "relay" );

			boolean useTextField = saveStateNames.isEmpty();
			LoginFrame.this.usernameField = useTextField ? (JComponent) new JTextField() : (JComponent) new LoginNameComboBox();
			this.passwordField = new JPasswordField();

			this.savePasswordCheckBox = new JCheckBox();
			this.autoLoginCheckBox = new JCheckBox();
			this.getBreakfastCheckBox = new JCheckBox();

			JPanel checkBoxPanels = new JPanel();
			checkBoxPanels.add( Box.createHorizontalStrut( 16 ) );
			checkBoxPanels.add( new JLabel( "Save Password: " ), "" );
			checkBoxPanels.add( this.savePasswordCheckBox );
			checkBoxPanels.add( Box.createHorizontalStrut( 16 ) );
			checkBoxPanels.add( new JLabel( "Auto-Login: " ), "" );
			checkBoxPanels.add( this.autoLoginCheckBox );
			checkBoxPanels.add( Box.createHorizontalStrut( 16 ) );
			checkBoxPanels.add( new JLabel( "Breakfast: " ), "" );
			checkBoxPanels.add( this.getBreakfastCheckBox );
			checkBoxPanels.add( Box.createHorizontalStrut( 16 ) );

			VerifiableElement [] elements = new VerifiableElement[2];
			elements[0] = new VerifiableElement( "Login: ", LoginFrame.this.usernameField );
			elements[1] = new VerifiableElement( "Password: ", this.passwordField );

			this.setContent( elements );

			this.actionStatusPanel.add( new JLabel( " ", JLabel.CENTER ), BorderLayout.CENTER );
			this.actionStatusPanel.add( checkBoxPanels, BorderLayout.NORTH );

			String autoLoginSetting = StaticEntity.getProperty( "autoLogin" );
			if ( autoLoginSetting.equals( "" ) )
				autoLoginSetting = StaticEntity.getProperty( "lastUsername" );
			else
				this.autoLoginCheckBox.setSelected( true );

			if ( LoginFrame.this.usernameField instanceof LoginNameComboBox )
				((LoginNameComboBox)LoginFrame.this.usernameField).setSelectedItem( autoLoginSetting );

			String passwordSetting = KoLmafia.getSaveState( autoLoginSetting );

			if ( passwordSetting != null )
			{
				this.passwordField.setText( passwordSetting );
				this.savePasswordCheckBox.setSelected( true );
			}

			this.getBreakfastCheckBox.setSelected( StaticEntity.getBooleanProperty( "alwaysGetBreakfast" ) );
			this.getBreakfastCheckBox.addActionListener( new GetBreakfastListener() );
			this.autoLoginCheckBox.addActionListener( new AutoLoginListener() );
			this.savePasswordCheckBox.addActionListener( new RemovePasswordListener() );

			JComponentUtilities.addHotKey( LoginFrame.this.usernameField, KeyEvent.VK_ENTER, this.CONFIRM_LISTENER );

			try
			{
				String holiday = MoonPhaseDatabase.getHoliday( DATED_FILENAME_FORMAT.parse( DATED_FILENAME_FORMAT.format( new Date() ) ), true );
				this.setStatusMessage( holiday + ", " + MoonPhaseDatabase.getMoonEffect() );
			}
			catch ( Exception e )
			{
				// Should not happen, you're parsing something that
				// was formatted the same way.

				StaticEntity.printStackTrace( e );
			}
		}

		public void addListeners()
		{
		}

		public void setEnabled( boolean isEnabled )
		{
			if ( LoginFrame.this.usernameField == null || this.passwordField == null )
				return;

			if ( this.savePasswordCheckBox == null || this.autoLoginCheckBox == null || this.getBreakfastCheckBox == null )
				return;

			super.setEnabled( isEnabled );

			LoginFrame.this.usernameField.setEnabled( isEnabled );
			this.passwordField.setEnabled( isEnabled );
		}

		public void actionConfirmed()
		{
			StaticEntity.setProperty( "relayBrowserOnly", "false" );
			StaticEntity.setProperty( "alwaysGetBreakfast", String.valueOf( this.getBreakfastCheckBox.isSelected() ) );

			LoginFrame.this.username = null;

			if ( LoginFrame.this.usernameField instanceof JTextField )
				LoginFrame.this.username = ((JTextField)LoginFrame.this.usernameField).getText();
			else if ( ((LoginNameComboBox)LoginFrame.this.usernameField).getSelectedItem() != null )
				LoginFrame.this.username = (String) ((LoginNameComboBox)LoginFrame.this.usernameField).getSelectedItem();
			else
				LoginFrame.this.username = (String) ((LoginNameComboBox)LoginFrame.this.usernameField).currentMatch;


			String password = new String( this.passwordField.getPassword() );

			if ( LoginFrame.this.username == null || password == null || LoginFrame.this.username.equals("") || password.equals("") )
			{
				this.setStatusMessage( "Invalid login." );
				return;
			}

			if ( this.autoLoginCheckBox.isSelected() )
				StaticEntity.setProperty( "autoLogin", LoginFrame.this.username );
			else
				StaticEntity.setProperty( "autoLogin", "" );

			StaticEntity.setGlobalProperty( LoginFrame.this.username, "getBreakfast", String.valueOf( this.getBreakfastCheckBox.isSelected() ) );

			LoginFrame.this.honorProxySettings();
			RequestThread.postRequest( new LoginRequest( LoginFrame.this.username, password ) );

			if ( !KoLmafia.permitsContinue() )
				return;

			try
			{
				String holiday = MoonPhaseDatabase.getHoliday( DATED_FILENAME_FORMAT.parse( DATED_FILENAME_FORMAT.format( new Date() ) ), true );
				KoLmafia.updateDisplay( ENABLE_STATE, holiday + ", " + MoonPhaseDatabase.getMoonEffect() );
			}
			catch ( Exception e )
			{
				// Should not happen, you're parsing something that
				// was formatted the same way.

				StaticEntity.printStackTrace( e );
			}
		}

		public void actionCancelled()
		{
			if ( !LoginRequest.isInstanceRunning() )
				StaticEntity.setProperty( "relayBrowserOnly", "true" );

			StaticEntity.getClient().openRelayBrowser();
			LoginFrame.this.setVisible( false );
		}

		private class AutoLoginListener implements ActionListener
		{
			public void actionPerformed( ActionEvent e )
			{
				if ( LoginPanel.this.autoLoginCheckBox.isSelected() )
					LoginPanel.this.actionConfirmed();
				else
					StaticEntity.setProperty( "autoLogin", "" );
			}
		}

		private class GetBreakfastListener implements ActionListener
		{
			public void actionPerformed( ActionEvent e )
			{	StaticEntity.setGlobalProperty( LoginFrame.this.username, "getBreakfast", String.valueOf( LoginPanel.this.getBreakfastCheckBox.isSelected() ) );
			}
		}

		private class RemovePasswordListener implements ActionListener
		{
			public void actionPerformed( ActionEvent e )
			{
				if ( !LoginPanel.this.savePasswordCheckBox.isSelected() && LoginFrame.this.usernameField instanceof JComboBox )
				{
					String value = (String) saveStateNames.getSelectedItem();
					if ( value == null )
						return;

					saveStateNames.remove( value );
					KoLmafia.removeSaveState( value );
					LoginPanel.this.passwordField.setText( "" );
				}

				StaticEntity.setProperty( "saveStateActive", String.valueOf( LoginPanel.this.savePasswordCheckBox.isSelected() ) );
			}
		}

		/**
		 * Special instance of a JComboBox which overrides the default
		 * key events of a JComboBox to allow you to catch key events.
		 */

		private class LoginNameComboBox extends MutableComboBox
		{
			public LoginNameComboBox()
			{	super( saveStateNames, true );
			}

			public void setSelectedItem( Object anObject )
			{
				super.setSelectedItem( anObject );
				this.setPassword();
			}

			public void findMatch( int keyCode )
			{
				super.findMatch( keyCode );
				this.setPassword();
			}

			private void setPassword()
			{
				if ( this.currentMatch == null )
				{
					LoginPanel.this.passwordField.setText( "" );
					LoginPanel.this.setStatusMessage( " " );

					LoginPanel.this.setEnabled( true );
					return;
				}

				String password = KoLmafia.getSaveState( (String) this.currentMatch );
				if ( password == null )
				{
					LoginPanel.this.passwordField.setText( "" );
					LoginPanel.this.setStatusMessage( " " );

					LoginPanel.this.setEnabled( true );
					return;
				}

				LoginPanel.this.passwordField.setText( password );
				LoginPanel.this.savePasswordCheckBox.setSelected( true );

				boolean breakfastSetting = StaticEntity.getGlobalProperty( (String) this.currentMatch, "getBreakfast" ).equals( "true" );
				LoginPanel.this.getBreakfastCheckBox.setSelected( breakfastSetting );
				LoginPanel.this.setEnabled( true );
			}
		}
	}


	private class ConnectionOptionsPanel extends OptionsPanel
	{
		private JCheckBox [] optionBoxes;

		private final String [][] options =
		{
			{ "proxySet", "Use a proxy to connect to the Kingdom of Loathing" },
			{ "ignoreLoadBalancer", "Ignore the KoL load balancer when trying to login" },
			{ "testSocketTimeout", "Allow socket timeouts for unstable connections" }
		};

		public ConnectionOptionsPanel()
		{
			super( "Connection Options", new Dimension( 20, 20 ), new Dimension( 380, 20 ) );

			LoginFrame.this.servers = new JComboBox();
			LoginFrame.this.servers.addItem( "Attempt to use dev.kingdomofloathing.com" );
			LoginFrame.this.servers.addItem( "Attempt to use www.kingdomofloathing.com" );

			for ( int i = 2; i <= KoLRequest.SERVER_COUNT; ++i )
				LoginFrame.this.servers.addItem( "Attempt to use www" + i + ".kingdomofloathing.com" );

			this.optionBoxes = new JCheckBox[ this.options.length ];
			for ( int i = 0; i < this.options.length; ++i )
				this.optionBoxes[i] = new JCheckBox();

			VerifiableElement [] elements = new VerifiableElement[ 2 + this.options.length ];

			elements[0] = new VerifiableElement( LoginFrame.this.servers );
			elements[1] = new VerifiableElement();

			for ( int i = 0; i < this.options.length; ++i )
				elements[i+2] = new VerifiableElement( this.options[i][1], JLabel.LEFT, this.optionBoxes[i] );

			this.actionCancelled();
			this.setContent( elements );
		}

		public void actionConfirmed()
		{
			StaticEntity.setProperty( "defaultLoginServer", String.valueOf( LoginFrame.this.servers.getSelectedIndex() ) );
			for ( int i = 0; i < this.options.length; ++i )
				StaticEntity.setProperty( this.options[i][0], String.valueOf( this.optionBoxes[i].isSelected() ) );
		}

		public void actionCancelled()
		{
			LoginFrame.this.servers.setSelectedIndex( StaticEntity.getIntegerProperty( "defaultLoginServer" ) );
			for ( int i = 0; i < this.options.length; ++i )
				this.optionBoxes[i].setSelected( StaticEntity.getBooleanProperty( this.options[i][0] ) );
		}

		public void setEnabled( boolean isEnabled )
		{
		}
	}

	public void honorProxySettings()
	{
		StaticEntity.setProperty( "http.proxyHost", this.proxyHost.getText() );
		StaticEntity.setProperty( "http.proxyPort", this.proxyPort.getText() );
		StaticEntity.setProperty( "http.proxyUser", this.proxyLogin.getText() );
		StaticEntity.setProperty( "http.proxyPassword", this.proxyPassword.getText() );
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

			LoginFrame.this.proxyHost = new JTextField();
			LoginFrame.this.proxyPort = new JTextField();
			LoginFrame.this.proxyLogin = new JTextField();
			LoginFrame.this.proxyPassword = new JTextField();

			VerifiableElement [] elements = new VerifiableElement[4];
			elements[0] = new VerifiableElement( "Host: ", LoginFrame.this.proxyHost );
			elements[1] = new VerifiableElement( "Port: ", LoginFrame.this.proxyPort );
			elements[2] = new VerifiableElement( "Login: ", LoginFrame.this.proxyLogin );
			elements[3] = new VerifiableElement( "Password: ", LoginFrame.this.proxyPassword );

			this.actionCancelled();
			this.setContent( elements );
		}

		public void actionConfirmed()
		{
		}

		public void actionCancelled()
		{
			LoginFrame.this.proxyHost.setText( StaticEntity.getProperty( "http.proxyHost" ) );
			LoginFrame.this.proxyPort.setText( StaticEntity.getProperty( "http.proxyPort" ) );
			LoginFrame.this.proxyLogin.setText( StaticEntity.getProperty( "http.proxyUser" ) );
			LoginFrame.this.proxyPassword.setText( StaticEntity.getProperty( "http.proxyPassword" ) );
		}

		public void setEnabled( boolean isEnabled )
		{
		}
	}
}
