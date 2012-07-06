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

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
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
import net.sourceforge.kolmafia.swingui.listener.ThreadedListener;

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

	private LoginPanel loginPanel = new LoginPanel();
	private ProxyOptionsPanel httpProxyOptions = new ProxyOptionsPanel( "http" );
	private ProxyOptionsPanel httpsProxyOptions = new ProxyOptionsPanel( "https" );

	public LoginFrame()
	{
		super( StaticEntity.getVersion() + ": Login" );

		this.tabs.addTab( "KoL Login", this.constructLoginPanel() );

		JPanel proxyPanel = new JPanel();
		proxyPanel.setLayout( new BoxLayout( proxyPanel, BoxLayout.Y_AXIS ) );
		proxyPanel.add( new ProxySetPanel() );
		proxyPanel.add( httpProxyOptions );
		proxyPanel.add( httpsProxyOptions );

		this.tabs.addTab( "Connection", new ConnectionOptionsPanel() );
		this.tabs.addTab( "Proxy Settings", proxyPanel );

		this.setCenterComponent( this.tabs );

		LoginFrame.INSTANCE = this;

		this.setFocusCycleRoot( true );
		this.setFocusTraversalPolicy( new DefaultComponentFocusTraversalPolicy( loginPanel.usernameField ) );
	}

	@Override
	public boolean shouldAddStatusBar()
	{
		return false;
	}

	@Override
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

	@Override
	public void dispose()
	{
		super.dispose();

		this.loginPanel = null;
		this.httpProxyOptions = null;
		this.httpsProxyOptions = null;
	}

	private void honorProxySettings()
	{
		if ( this.httpProxyOptions != null )
		{
			this.httpProxyOptions.actionConfirmed();
		}

		if ( this.httpsProxyOptions != null )
		{
			this.httpsProxyOptions.actionConfirmed();
		}
	}

	@Override
	protected void checkForLogout()
	{
		this.honorProxySettings();

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
		containerPanel.add( this.loginPanel, BorderLayout.CENTER );
		return containerPanel;
	}

	/**
	 * An internal class which represents the panel which is nested inside of the <code>LoginFrame</code>.
	 */

	private class LoginPanel
		extends GenericPanel
	{
		private LoginNameComboBox usernameField;
		private final JPasswordField passwordField;

		private final JCheckBox stealthLoginCheckBox;
		private final JCheckBox savePasswordCheckBox;
		private final JCheckBox getBreakfastCheckBox;

		/**
		 * Constructs a new <code>LoginPanel</code>, containing a place for the users to input their login name and
		 * password. This panel, because it is intended to be the content panel for status message updates, also has a
		 * status label.
		 */

		public LoginPanel()
		{
			super( "login" );

			this.usernameField = new LoginNameComboBox();
			this.passwordField = new JPasswordField();

			this.savePasswordCheckBox = new JCheckBox();
			this.stealthLoginCheckBox = new JCheckBox();
			this.getBreakfastCheckBox = new JCheckBox();

			VerifiableElement[] elements = new VerifiableElement[ 2 ];
			elements[ 0 ] = new VerifiableElement( "Login: ", this.usernameField );
			elements[ 1 ] = new VerifiableElement( "Password: ", this.passwordField );

			this.setContent( elements );

			JPanel checkBoxPanels = new JPanel();
			checkBoxPanels.add( Box.createHorizontalStrut( 16 ) );
			checkBoxPanels.add( new JLabel( "Save Password: " ), "" );
			checkBoxPanels.add( this.savePasswordCheckBox );
			checkBoxPanels.add( Box.createHorizontalStrut( 16 ) );
			checkBoxPanels.add( new JLabel( "Stealth Login: " ), "" );
			checkBoxPanels.add( this.stealthLoginCheckBox );
			checkBoxPanels.add( Box.createHorizontalStrut( 16 ) );
			checkBoxPanels.add( new JLabel( "Breakfast: " ), "" );
			checkBoxPanels.add( this.getBreakfastCheckBox );
			checkBoxPanels.add( Box.createHorizontalStrut( 16 ) );

			this.actionStatusPanel.add( new JLabel( " ", SwingConstants.CENTER ), BorderLayout.CENTER );
			this.actionStatusPanel.add( checkBoxPanels, BorderLayout.NORTH );

			String lastUsername = Preferences.getString( "lastUsername" );
			this.usernameField.setSelectedItem( lastUsername );

			String passwordSetting = KoLmafia.getSaveState( lastUsername );

			if ( passwordSetting != null )
			{
				this.passwordField.setText( passwordSetting );
				this.savePasswordCheckBox.setSelected( true );
			}

			this.getBreakfastCheckBox.addActionListener( new GetBreakfastListener() );
			this.savePasswordCheckBox.addActionListener( new RemovePasswordListener() );

			LoginPanel.this.getBreakfastCheckBox.setSelected( Preferences.getBoolean( lastUsername, "getBreakfast" ) );
			LoginPanel.this.stealthLoginCheckBox.setSelected( Preferences.getBoolean( "stealthLogin" ) );

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

		@Override
		public void setEnabled( final boolean isEnabled )
		{
			if ( this.usernameField == null || this.passwordField == null )
			{
				return;
			}

			if ( this.savePasswordCheckBox == null || this.getBreakfastCheckBox == null )
			{
				return;
			}

			super.setEnabled( isEnabled );

			this.usernameField.setEnabled( isEnabled );
			this.passwordField.setEnabled( isEnabled );
		}

		@Override
		public void actionConfirmed()
		{
			Preferences.setBoolean( "relayBrowserOnly", false );
			this.doLogin();
		}

		@Override
		public void actionCancelled()
		{
			if ( !LoginRequest.isInstanceRunning() )
			{
				Preferences.setBoolean( "relayBrowserOnly", true );
				this.doLogin();
			}
		}

		private String getUsername()
		{
			if ( this.usernameField.getSelectedItem() != null )
			{
				return (String) this.usernameField.getSelectedItem();
			}

			return (String) this.usernameField.currentMatch;

		}

		private void doLogin()
		{
			String username = getUsername();
			String password = new String( this.passwordField.getPassword() );

			if ( username == null || username.equals( "" ) || password.equals( "" ) )
			{
				this.setStatusMessage( "Invalid login." );
				return;
			}

			Preferences.setBoolean(
				username, "getBreakfast", this.getBreakfastCheckBox.isSelected() );

			Preferences.setBoolean( "stealthLogin", LoginPanel.this.stealthLoginCheckBox.isSelected() );

			LoginFrame.this.honorProxySettings();

			RequestThread.postRequest( new LoginRequest( username, password ) );
		}

		private class GetBreakfastListener
			extends ThreadedListener
		{
			@Override
			protected void execute()
			{
				Preferences.setBoolean(
					getUsername(), "getBreakfast",
					LoginPanel.this.getBreakfastCheckBox.isSelected() );
			}
		}

		private class RemovePasswordListener
			extends ThreadedListener
		{
			@Override
			protected void execute()
			{
				if ( !LoginPanel.this.savePasswordCheckBox.isSelected() )
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

			@Override
			public void setSelectedItem( final Object anObject )
			{
				super.setSelectedItem( anObject );
				this.setPassword();
			}

			@Override
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

	private class ProxySetPanel
		extends OptionsPanel
	{
		private final String[][] options =
		{
			{ "proxySet", "KoLmafia needs to connect through a proxy server" },
		};

		public ProxySetPanel()
		{
			super( new Dimension( 20, 20 ), new Dimension( 250, 20 ) );

			this.setOptions( this.options );

			String httpHost = System.getProperty( "http.proxyHost" );
			String httpsHost = System.getProperty( "https.proxyHost" );

			boolean proxySet = httpHost != null && httpHost.length() > 0 || httpsHost != null && httpsHost.length() > 0;

			if ( System.getProperty( "os.name" ).startsWith( "Mac" ) )
			{
				this.optionBoxes[ 0 ].setSelected( proxySet );
				this.optionBoxes[ 0 ].setEnabled( false );
			}
			else
			{
				proxySet |= Preferences.getBoolean( "proxySet" );
				this.optionBoxes[ 0 ].setSelected( proxySet );
			}
		}

		@Override
		public void setEnabled( final boolean isEnabled )
		{
			if ( System.getProperty( "os.name" ).startsWith( "Mac" ) )
			{
				return;
			}

			super.setEnabled( isEnabled );
		}
	}

	private class ConnectionOptionsPanel
		extends OptionsPanel
	{
		private final String[][] options =
		{
			{ "useDevProxyServer", "Use devproxy.kingdomofloathing.com to login" },
			{ "useSecureLogin", "Switch to HTTPS for login (development in progress)" },
			{ "connectViaAddress", "Use IP address to connect instead of host name" },
			{ "useNaiveSecureLogin", "Do not have Java try to validate SSL certificates" },
			{ "allowSocketTimeout", "Forcibly time-out laggy requests" }
		};

		public ConnectionOptionsPanel()
		{
			super( new Dimension( 20, 20 ), new Dimension( 250, 20 ) );

			this.setOptions( this.options );
		}

		@Override
		public void setEnabled( final boolean isEnabled )
		{
			super.setEnabled( isEnabled );
		}
	}

	/**
	 * This panel handles all of the things related to proxy options (if applicable).
	 */

	private class ProxyOptionsPanel
		extends LabeledPanel
	{
		private String protocol;

		private AutoHighlightTextField proxyHost;
		private AutoHighlightTextField proxyPort;
		private AutoHighlightTextField proxyLogin;
		private AutoHighlightTextField proxyPassword;

		/**
		 * Constructs a new <code>ProxyOptionsPanel</code>, containing a place for the users to select their desired
		 * server and for them to modify any applicable proxy settings.
		 */

		public ProxyOptionsPanel( String protocol )
		{
			super( "Proxy Settings: " + protocol, new Dimension( 80, 20 ), new Dimension( 240, 20 ) );

			this.protocol = protocol;

			this.proxyHost = new AutoHighlightTextField();
			this.proxyPort = new AutoHighlightTextField();
			this.proxyLogin = new AutoHighlightTextField();
			this.proxyPassword = new AutoHighlightTextField();

			VerifiableElement[] elements = new VerifiableElement[ 4 ];
			elements[ 0 ] = new VerifiableElement( "Host: ", this.proxyHost );
			elements[ 1 ] = new VerifiableElement( "Port: ", this.proxyPort );
			elements[ 2 ] = new VerifiableElement( "Login: ", this.proxyLogin );
			elements[ 3 ] = new VerifiableElement( "Password: ", this.proxyPassword );

			this.actionCancelled();
			this.setContent( elements );
		}

		@Override
		public void actionConfirmed()
		{
			if ( System.getProperty( "os.name" ).startsWith( "Mac" ) )
			{
				return;
			}

			Preferences.setString( this.protocol + ".proxyHost", this.proxyHost.getText() );
			Preferences.setString( this.protocol + ".proxyPort", this.proxyPort.getText() );
			Preferences.setString( this.protocol + ".proxyUser", this.proxyLogin.getText() );
			Preferences.setString( this.protocol + ".proxyPassword", this.proxyPassword.getText() );
		}

		@Override
		public void actionCancelled()
		{
			String proxyHost = System.getProperty( this.protocol + ".proxyHost" );

			if ( proxyHost != null && proxyHost.length() != 0 || System.getProperty( "os.name" ).startsWith( "Mac" ) )
			{
				this.proxyHost.setText( System.getProperty( this.protocol + ".proxyHost" ) );
				this.proxyPort.setText( System.getProperty( this.protocol + ".proxyPort" ) );
				this.proxyLogin.setText( System.getProperty( this.protocol + ".proxyUser" ) );
				this.proxyPassword.setText( System.getProperty( this.protocol + ".proxyPassword" ) );
			}
			else
			{
				this.proxyHost.setText( Preferences.getString( this.protocol + ".proxyHost" ) );
				this.proxyPort.setText( Preferences.getString( this.protocol + ".proxyPort" ) );
				this.proxyLogin.setText( Preferences.getString( this.protocol + ".proxyUser" ) );
				this.proxyPassword.setText( Preferences.getString( this.protocol + ".proxyPassword" ) );
			}

			if ( System.getProperty( "os.name" ).startsWith( "Mac" ) )
			{
				this.proxyHost.setEnabled( false );
				this.proxyPort.setEnabled( false );
				this.proxyLogin.setEnabled( false );
				this.proxyPassword.setEnabled( false );
			}
		}

		@Override
		public void setEnabled( final boolean isEnabled )
		{
			if ( System.getProperty( "os.name" ).startsWith( "Mac" ) )
			{
				return;
			}

			super.setEnabled( isEnabled );
		}
	}
}
