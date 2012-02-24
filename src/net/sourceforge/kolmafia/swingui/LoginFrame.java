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

import java.awt.BorderLayout;
import java.awt.Dimension;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.SwingConstants;

import net.java.dev.spellcast.utilities.JComponentUtilities;

import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.StaticEntity;

import net.sourceforge.kolmafia.persistence.HolidayDatabase;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.request.LoginRequest;

import net.sourceforge.kolmafia.swingui.listener.DefaultComponentFocusTraversalPolicy;

import net.sourceforge.kolmafia.swingui.panel.GenericPanel;
import net.sourceforge.kolmafia.swingui.panel.LabeledPanel;
import net.sourceforge.kolmafia.swingui.panel.OptionsPanel;

import net.sourceforge.kolmafia.swingui.widget.AutoFilterComboBox;
import net.sourceforge.kolmafia.swingui.widget.AutoHighlightTextField;

import net.sourceforge.kolmafia.utilities.StringUtilities;

public class LoginFrame
	extends GenericFrame
{
	private static LoginFrame INSTANCE = null;

	private String username;
	private JComboBox servers;
	private JComponent usernameField;

	private AutoHighlightTextField proxyHost;
	private AutoHighlightTextField proxyPort;
	private AutoHighlightTextField proxyLogin;
	private AutoHighlightTextField proxyPassword;

	public LoginFrame()
	{
		super( StaticEntity.getVersion() + ": Login" );

		this.tabs.addTab( "KoL Login", this.constructLoginPanel() );

		JPanel connectPanel = new JPanel();
		connectPanel.setLayout( new BoxLayout( connectPanel, BoxLayout.Y_AXIS ) );
		connectPanel.add( new ConnectionOptionsPanel() );
		connectPanel.add( new ProxyOptionsPanel() );

		this.tabs.addTab( "Connection", connectPanel );

		this.setCenterComponent( this.tabs );

		LoginFrame.INSTANCE = this;

		this.setFocusCycleRoot( true );
		this.setFocusTraversalPolicy( new DefaultComponentFocusTraversalPolicy( this.usernameField ) );
	}

	public boolean shouldAddStatusBar()
	{
		return false;
	}

	public boolean showInWindowMenu()
	{
		return false;
	}

	public static final void hideInstance()
	{
		if ( LoginFrame.INSTANCE != null )
		{
			LoginFrame.INSTANCE.setVisible( false );
		}
	}

	public static final void disposeInstance()
	{
		if ( LoginFrame.INSTANCE != null )
		{
			LoginFrame.INSTANCE.dispose();
			LoginFrame.INSTANCE = null;
		}
	}

	public void dispose()
	{
		this.honorProxySettings();
		super.dispose();
	}

	protected void checkForLogout()
	{
		if ( !LoginRequest.isInstanceRunning() )
		{
			KoLmafia.quit();
		}
	}

	public JPanel constructLoginPanel()
	{
		String logoName = Preferences.getString( "loginWindowLogo" );

		if ( logoName.endsWith( ".jpg" ) )
		{
			logoName = logoName.substring( 0, logoName.length() - 4 ) + ".gif";
			Preferences.setString( "loginWindowLogo", logoName );
		}

		JPanel imagePanel = new JPanel( new BorderLayout( 0, 0 ) );
		imagePanel.add( new JLabel( " " ), BorderLayout.NORTH );
		imagePanel.add(
			new JLabel( JComponentUtilities.getImage( logoName ), SwingConstants.CENTER ),
			BorderLayout.SOUTH );

		JPanel containerPanel = new JPanel( new BorderLayout() );
		containerPanel.add( imagePanel, BorderLayout.NORTH );
		containerPanel.add( new LoginPanel(), BorderLayout.CENTER );
		return containerPanel;
	}

	/**
	 * An internal class which represents the panel which is nested inside of the <code>LoginFrame</code>.
	 */

	private class LoginPanel
		extends GenericPanel
	{
		private final JPasswordField passwordField;

		private final JCheckBox savePasswordCheckBox;
		private final JCheckBox autoLoginCheckBox;
		private final JCheckBox getBreakfastCheckBox;

		/**
		 * Constructs a new <code>LoginPanel</code>, containing a place for the users to input their login name and
		 * password. This panel, because it is intended to be the content panel for status message updates, also has a
		 * status label.
		 */

		public LoginPanel()
		{
			super( "login" );

			boolean useTextField = KoLConstants.saveStateNames.isEmpty();
			LoginFrame.this.usernameField =
				useTextField ? (JComponent) new AutoHighlightTextField() : (JComponent) new LoginNameComboBox();
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

			VerifiableElement[] elements = new VerifiableElement[ 2 ];
			elements[ 0 ] = new VerifiableElement( "Login: ", LoginFrame.this.usernameField );
			elements[ 1 ] = new VerifiableElement( "Password: ", this.passwordField );

			this.setContent( elements );

			this.actionStatusPanel.add( new JLabel( " ", SwingConstants.CENTER ), BorderLayout.CENTER );
			this.actionStatusPanel.add( checkBoxPanels, BorderLayout.NORTH );

			String autoLoginSetting = Preferences.getString( "autoLogin" );
			if ( autoLoginSetting.equals( "" ) )
			{
				autoLoginSetting = Preferences.getString( "lastUsername" );
			}
			else
			{
				this.autoLoginCheckBox.setSelected( true );
			}

			if ( LoginFrame.this.usernameField instanceof LoginNameComboBox )
			{
				( (LoginNameComboBox) LoginFrame.this.usernameField ).setSelectedItem( autoLoginSetting );
			}

			String passwordSetting = KoLmafia.getSaveState( autoLoginSetting );

			if ( passwordSetting != null )
			{
				this.passwordField.setText( passwordSetting );
				this.savePasswordCheckBox.setSelected( true );
			}

			this.getBreakfastCheckBox.setSelected( Preferences.getBoolean( "alwaysGetBreakfast" ) );
			this.getBreakfastCheckBox.addActionListener( new GetBreakfastListener() );
			this.autoLoginCheckBox.addActionListener( new AutoLoginListener() );
			this.savePasswordCheckBox.addActionListener( new RemovePasswordListener() );

			String holiday = HolidayDatabase.getHoliday( true );
			String moonEffect = HolidayDatabase.getMoonEffect();

			String updateText;

			if ( holiday.equals( "" ) )
			{
				updateText = moonEffect;
			}
			else
			{
				updateText = holiday + ", " + moonEffect;
			}

			updateText = StringUtilities.getEntityDecode( updateText, false );
			this.setStatusMessage( updateText );
		}

		public void setEnabled( final boolean isEnabled )
		{
			if ( LoginFrame.this.usernameField == null || this.passwordField == null )
			{
				return;
			}

			if ( this.savePasswordCheckBox == null || this.autoLoginCheckBox == null || this.getBreakfastCheckBox == null )
			{
				return;
			}

			super.setEnabled( isEnabled );

			LoginFrame.this.usernameField.setEnabled( isEnabled );
			this.passwordField.setEnabled( isEnabled );
		}

		public void actionConfirmed()
		{
			Preferences.setBoolean( "relayBrowserOnly", false );
			this.doLogin();
		}

		public void actionCancelled()
		{
			if ( !LoginRequest.isInstanceRunning() )
			{
				Preferences.setBoolean( "relayBrowserOnly", true );
				this.doLogin();
			}
		}

		private void doLogin()
		{
			Preferences.setBoolean( "alwaysGetBreakfast", this.getBreakfastCheckBox.isSelected() );

			LoginFrame.this.username = null;

			if ( LoginFrame.this.usernameField instanceof AutoHighlightTextField )
			{
				LoginFrame.this.username = ( (AutoHighlightTextField) LoginFrame.this.usernameField ).getText();
			}
			else if ( ( (LoginNameComboBox) LoginFrame.this.usernameField ).getSelectedItem() != null )
			{
				LoginFrame.this.username =
					(String) ( (LoginNameComboBox) LoginFrame.this.usernameField ).getSelectedItem();
			}
			else
			{
				LoginFrame.this.username = (String) ( (LoginNameComboBox) LoginFrame.this.usernameField ).currentMatch;
			}

			String password = new String( this.passwordField.getPassword() );

			if ( LoginFrame.this.username == null || LoginFrame.this.username.equals( "" ) || password.equals( "" ) )
			{
				this.setStatusMessage( "Invalid login." );
				return;
			}

			if ( this.autoLoginCheckBox.isSelected() )
			{
				Preferences.setString( "autoLogin", LoginFrame.this.username );
			}
			else
			{
				Preferences.setString( "autoLogin", "" );
			}

			Preferences.setBoolean(
				LoginFrame.this.username, "getBreakfast", this.getBreakfastCheckBox.isSelected() );

			LoginFrame.this.honorProxySettings();
			RequestThread.postRequest( new LoginRequest( LoginFrame.this.username, password ) );
		}

		private class AutoLoginListener
			implements ActionListener
		{
			public void actionPerformed( final ActionEvent e )
			{
				if ( LoginPanel.this.autoLoginCheckBox.isSelected() )
				{
					LoginPanel.this.actionConfirmed();
				}
				else
				{
					Preferences.setString( "autoLogin", "" );
				}
			}
		}

		private class GetBreakfastListener
			implements ActionListener
		{
			public void actionPerformed( final ActionEvent e )
			{
				Preferences.setBoolean(
					LoginFrame.this.username, "getBreakfast",
					LoginPanel.this.getBreakfastCheckBox.isSelected() );
			}
		}

		private class RemovePasswordListener
			implements ActionListener
		{
			public void actionPerformed( final ActionEvent e )
			{
				if ( !LoginPanel.this.savePasswordCheckBox.isSelected() && LoginFrame.this.usernameField instanceof JComboBox )
				{
					String value = (String) KoLConstants.saveStateNames.getSelectedItem();
					if ( value == null )
					{
						return;
					}

					KoLConstants.saveStateNames.remove( value );
					KoLmafia.removeSaveState( value );
					LoginPanel.this.passwordField.setText( "" );
				}

				Preferences.setBoolean(
					"saveStateActive", LoginPanel.this.savePasswordCheckBox.isSelected() );
			}
		}

		/**
		 * Special instance of a JComboBox which overrides the default key events of a JComboBox to allow you to catch
		 * key events.
		 */

		private class LoginNameComboBox
			extends AutoFilterComboBox
		{
			public LoginNameComboBox()
			{
				super( KoLConstants.saveStateNames, true );
			}

			public void setSelectedItem( final Object anObject )
			{
				super.setSelectedItem( anObject );
				this.setPassword();
			}

			public void findMatch( final int keyCode )
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

				boolean breakfastSetting =
					Preferences.getBoolean(
						((String)this.currentMatch), "getBreakfast" );

				LoginPanel.this.getBreakfastCheckBox.setSelected( breakfastSetting );
				LoginPanel.this.setEnabled( true );
			}
		}
	}

	private class ConnectionOptionsPanel
		extends OptionsPanel
	{
		private final JCheckBox[] optionBoxes;

		private final String[][] options =
		{
			{ "proxySet", "Use a proxy to connect to the Kingdom of Loathing" },
			{ "useSecureLogin", "Switch to HTTPS for login (development in progress)" },
			{ "allowSocketTimeout", "Improve handling of semi-random lag spikes" },
			{ "connectViaAddress", "Connect to servers using IP address rather than name" },
			{ "stealthLogin", "Log in with /q to suppress your login announcement" },
		};

		public ConnectionOptionsPanel()
		{
			super( new Dimension( 20, 20 ), new Dimension( 380, 20 ) );

			LoginFrame.this.servers = new JComboBox();
			LoginFrame.this.servers.addItem( "Attempt to use dev.kingdomofloathing.com" );
			LoginFrame.this.servers.addItem( "Attempt to use www.kingdomofloathing.com" );

			this.optionBoxes = new JCheckBox[ this.options.length ];
			for ( int i = 0; i < this.options.length; ++i )
			{
				this.optionBoxes[ i ] = new JCheckBox();
			}

			VerifiableElement[] elements = new VerifiableElement[ 2 + this.options.length ];

			elements[ 0 ] = new VerifiableElement( LoginFrame.this.servers );
			elements[ 1 ] = new VerifiableElement();

			for ( int i = 0; i < this.options.length; ++i )
			{
				elements[ i + 2 ] =
					new VerifiableElement( this.options[ i ][ 1 ], SwingConstants.LEFT, this.optionBoxes[ i ] );
			}

			if ( System.getProperty( "os.name" ).startsWith( "Mac" ) )
			{
				String proxySet = System.getProperty( "proxySet" );
				this.optionBoxes[ 0 ].setSelected( proxySet != null && proxySet.equals( "true" ) );
				this.optionBoxes[ 0 ].setEnabled( false );
			}

			this.actionCancelled();
			this.setContent( elements );
		}

		public void actionConfirmed()
		{
			Preferences.setInteger(
				"defaultLoginServer", LoginFrame.this.servers.getSelectedIndex() );

			for ( int i = 0; i < this.options.length; ++i )
			{
				Preferences.setBoolean(
					this.options[ i ][ 0 ], this.optionBoxes[ i ].isSelected() );
			}
		}

		public void actionCancelled()
		{
			LoginFrame.this.servers.setSelectedIndex( Preferences.getInteger( "defaultLoginServer" ) );
			for ( int i = 0; i < this.options.length; ++i )
			{
				this.optionBoxes[ i ].setSelected( Preferences.getBoolean( this.options[ i ][ 0 ] ) );
			}
		}

		public void setEnabled( final boolean isEnabled )
		{
		}
	}

	public void honorProxySettings()
	{
		Preferences.setString( "http.proxyHost", this.proxyHost.getText() );
		Preferences.setString( "http.proxyPort", this.proxyPort.getText() );
		Preferences.setString( "http.proxyUser", this.proxyLogin.getText() );
		Preferences.setString( "http.proxyPassword", this.proxyPassword.getText() );
	}

	/**
	 * This panel handles all of the things related to proxy options (if applicable).
	 */

	private class ProxyOptionsPanel
		extends LabeledPanel
	{
		/**
		 * Constructs a new <code>ProxyOptionsPanel</code>, containing a place for the users to select their desired
		 * server and for them to modify any applicable proxy settings.
		 */

		public ProxyOptionsPanel()
		{
			super( "Proxy Settings", new Dimension( 80, 20 ), new Dimension( 240, 20 ) );

			LoginFrame.this.proxyHost = new AutoHighlightTextField();
			LoginFrame.this.proxyPort = new AutoHighlightTextField();
			LoginFrame.this.proxyLogin = new AutoHighlightTextField();
			LoginFrame.this.proxyPassword = new AutoHighlightTextField();

			VerifiableElement[] elements = new VerifiableElement[ 4 ];
			elements[ 0 ] = new VerifiableElement( "Host: ", LoginFrame.this.proxyHost );
			elements[ 1 ] = new VerifiableElement( "Port: ", LoginFrame.this.proxyPort );
			elements[ 2 ] = new VerifiableElement( "Login: ", LoginFrame.this.proxyLogin );
			elements[ 3 ] = new VerifiableElement( "Password: ", LoginFrame.this.proxyPassword );

			this.actionCancelled();
			this.setContent( elements );
		}

		public void actionConfirmed()
		{
		}

		public void actionCancelled()
		{
			if ( System.getProperty( "os.name" ).startsWith( "Mac" ) )
			{
				LoginFrame.this.proxyHost.setText( System.getProperty( "http.proxyHost" ) );
				LoginFrame.this.proxyPort.setText( System.getProperty( "http.proxyPort" ) );
				LoginFrame.this.proxyLogin.setText( System.getProperty( "http.proxyUser" ) );
				LoginFrame.this.proxyPassword.setText( System.getProperty( "http.proxyPassword" ) );

				LoginFrame.this.proxyHost.setEnabled( false );
				LoginFrame.this.proxyPort.setEnabled( false );
				LoginFrame.this.proxyLogin.setEnabled( false );
				LoginFrame.this.proxyPassword.setEnabled( false );
			}
			else
			{
				LoginFrame.this.proxyHost.setText( Preferences.getString( "http.proxyHost" ) );
				LoginFrame.this.proxyPort.setText( Preferences.getString( "http.proxyPort" ) );
				LoginFrame.this.proxyLogin.setText( Preferences.getString( "http.proxyUser" ) );
				LoginFrame.this.proxyPassword.setText( Preferences.getString( "http.proxyPassword" ) );
			}
		}

		public void setEnabled( final boolean isEnabled )
		{
		}
	}
}
