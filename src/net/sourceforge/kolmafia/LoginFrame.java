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
import java.awt.CardLayout;
import java.awt.GridLayout;
import java.awt.BorderLayout;

// event listeners
import javax.swing.SwingUtilities;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

// containers
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.JButton;
import javax.swing.JTextField;
import javax.swing.JPasswordField;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JMenuBar;
import javax.swing.JComboBox;

// other imports
import java.util.StringTokenizer;
import net.java.dev.spellcast.utilities.LicenseDisplay;
import net.java.dev.spellcast.utilities.LockableListModel;
import net.java.dev.spellcast.utilities.JComponentUtilities;

/**
 * An extended <code>JFrame</code> which presents the user with the ability to
 * login to the Kingdom of Loathing.  Essentially, this class is a modification
 * of the <code>LoginDialog</code> class from the Spellcast project to contain
 * a single panel and to be properly named.
 */

public class LoginFrame extends KoLFrame
{
	private static final String LOGIN_CARD   = "Login";
	private static final String OPTIONS_CARD = "Options";

	private CardLayout cards;

	public LoginFrame( KoLmafia client )
	{
		super( "KoLmafia: Login", client );
		setResizable( false );

		cards = new CardLayout( 10, 10 );
		getContentPane().setLayout( cards );

		this.client = client;
		contentPanel = new LoginPanel();
		getContentPane().add( contentPanel, LOGIN_CARD );
		getContentPane().add( new LoginOptionsPanel(), OPTIONS_CARD );

		updateDisplay( ENABLED_STATE, " " );
		setDefaultCloseOperation( DISPOSE_ON_CLOSE );

		addMenuBar();
	}

	private void addMenuBar()
	{
		JMenuBar menuBar = new JMenuBar();
		this.setJMenuBar( menuBar );

		JMenu fileMenu = new JMenu("Configure");
		fileMenu.setMnemonic( KeyEvent.VK_C );
		menuBar.add( fileMenu );

		JMenuItem settingsItem = new JMenuItem( "Preferences...", KeyEvent.VK_P );
		settingsItem.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent e)
			{	(new SwitchCardDisplay( OPTIONS_CARD )).run();
			}
		});

		fileMenu.add( settingsItem );

		final JMenuItem loggerItem = new JMenuItem( "Initialize Logger", KeyEvent.VK_L );
		loggerItem.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent e)
			{
				client.initializeLogStream();
				loggerItem.setEnabled( false );
			}
		});

		fileMenu.add( loggerItem );

		JMenu helpMenu = new JMenu("Help");
		helpMenu.setMnemonic( KeyEvent.VK_H );
		menuBar.add( helpMenu );

		JMenuItem aboutItem = new JMenuItem( "About KoLmafia", KeyEvent.VK_C );
		aboutItem.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent e)
			{	(new LicenseDisplay( "KoLmafia: Copyright Notice" )).requestFocus();
			}
		});

		helpMenu.add( aboutItem );
	}

	private class LoginPanel extends KoLPanel
	{
		private JPanel actionStatusPanel;
		private JLabel actionStatusLabel;
		private JLabel serverReplyLabel;

		JTextField loginnameField;
		JPasswordField passwordField;

		public LoginPanel()
		{
			super( "login", "cancel" );

			actionStatusPanel = new JPanel();
			actionStatusPanel.setLayout( new GridLayout( 2, 1 ) );

			actionStatusLabel = new JLabel( " ", JLabel.CENTER );
			actionStatusPanel.add( actionStatusLabel );
			serverReplyLabel = new JLabel( " ", JLabel.CENTER );
			actionStatusPanel.add( serverReplyLabel );

			loginnameField = new JTextField();
			passwordField = new JPasswordField();

			VerifiableElement [] elements = new VerifiableElement[2];
			elements[0] = new VerifiableElement( "Login: ", loginnameField );
			elements[1] = new VerifiableElement( "Password: ", passwordField );

			setContent( elements );
		}

		protected void setContent( VerifiableElement [] elements )
		{
			super.setContent( elements );
			add( actionStatusPanel, BorderLayout.SOUTH );
		}

		public void setStatusMessage( String s )
		{	actionStatusLabel.setText( s );
		}

		public void setEnabled( boolean isEnabled )
		{
			super.setEnabled( isEnabled );
			loginnameField.setEnabled( isEnabled );
			passwordField.setEnabled( isEnabled );
		}

		public void clear()
		{
			loginnameField.setText( "" );
			passwordField.setText( "" );
			requestFocus();
		}

		protected void actionConfirmed()
		{
			updateDisplay( DISABLED_STATE, "Sending login..." );
			(new LoginRequestThread()).start();
		}

		protected void actionCancelled()
		{
			updateDisplay( ENABLED_STATE, "Login cancelled." );
			requestFocus();
		}

		public void requestFocus()
		{	loginnameField.requestFocus();
		}

		private class LoginRequestThread extends Thread
		{
			public LoginRequestThread()
			{
				super( "Login-Request-Thread" );
				setDaemon( true );
			}

			public void run()
			{
				String loginname = loginnameField.getText();
				String password = new String( passwordField.getPassword() );

				if ( loginname.equals("") || password.equals("") )
				{
					updateDisplay( ENABLED_STATE, "Invalid login." );
					return;
				}

				(new LoginRequest( client, loginname, password )).run();
			}
		}
	}

	private class LoginOptionsPanel extends KoLPanel
	{
		private JComboBox serverSelect;
		private JTextField proxyAddress;
		private JTextField proxyLogin;

		public LoginOptionsPanel()
		{
			super( "apply", "defaults" );

			LockableListModel servers = new LockableListModel();
			servers.add( "(Auto Detect)" );
			servers.add( "Use Login Server 1" );
			servers.add( "Use Login Server 2" );
			servers.add( "Use Login Server 3" );

			serverSelect = new JComboBox( servers );
			proxyAddress = new JTextField();
			proxyLogin = new JTextField();

			(new LoadDefaultSettingsThread()).run();

			VerifiableElement [] elements = new VerifiableElement[3];
			elements[0] = new VerifiableElement( "KoL Server: ", serverSelect );
			elements[1] = new VerifiableElement( "Proxy Address: ", proxyAddress );
			elements[2] = new VerifiableElement( "Proxy Login: ", proxyLogin );

			setContent( elements );
		}

		public void setStatusMessage( String s )
		{
			// This panel ignores setStatusMessage, since it should never
			// be the actual content panel.  In order to not be abstract,
			// this method exists and does nothing.
		}

		public void clear()
		{	(new LoadDefaultSettingsThread()).run();
		}

		protected void actionConfirmed()
		{	(new StoreSettingsThread()).start();
		}

		protected void actionCancelled()
		{
			// Clear the pane, which effectively loads
			// the original default settings.

			this.clear();
		}

		private class LoadDefaultSettingsThread extends Thread
		{
			public void run()
			{
				serverSelect.setSelectedIndex( Integer.parseInt( client.getSettings().getProperty( "loginServer" ) ) );
				String http_proxySet = client.getSettings().getProperty( "proxySet" );

				if ( http_proxySet.equals( "true" ) )
				{
					System.setProperty( "proxySet", "true" );

					String http_proxyHost = client.getSettings().getProperty( "http.proxyHost" );
					System.setProperty( "http.proxyHost", http_proxyHost );
					String http_proxyPort = client.getSettings().getProperty( "http.proxyPort" );
					System.setProperty( "http.proxyPort", http_proxyPort );

					String http_proxyUser = client.getSettings().getProperty( "http.proxyUser" );
					String http_proxyPassword = client.getSettings().getProperty( "http.proxyPassword" );

					if ( http_proxyUser != null )
					{
						System.setProperty( "http.proxyUser", http_proxyUser );
						System.setProperty( "http.proxyPassword", http_proxyPassword );
					}

					proxyAddress.setText( http_proxyHost + ":" + http_proxyPort );
					proxyLogin.setText( http_proxyUser + ":" + http_proxyPassword );
				}
				else
				{
					proxyAddress.setText( "" );
					proxyLogin.setText( "" );
				}
			}
		}

		private class StoreSettingsThread extends Thread
		{
			public void run()
			{
				// First, determine what the proxy settings chosen
				// by the user.

				StringTokenizer proxy1 = new StringTokenizer( proxyAddress.getText(), ":" );
				StringTokenizer proxy2 = new StringTokenizer( proxyLogin.getText(), ":" );

				if ( proxy1.hasMoreTokens() )
				{
					setProperty( "proxySet", "true" );
					setProperty( "http.proxyHost", proxy1.nextToken() );
					setProperty( "http.proxyPort", proxy1.hasMoreTokens() ? proxy1.nextToken() : "80" );

					if ( proxy2.hasMoreTokens() )
					{
						setProperty( "http.proxyUser", proxy2.nextToken() );
						setProperty( "http.proxyPassword", proxy2.hasMoreTokens() ? proxy2.nextToken() : "anonymoususer@defaultmailserver.com" );
					}
					else
					{
						removeProperty( "http.proxyUser" );
						removeProperty( "http.proxyPassword" );
					}
				}
				else
				{
					client.getSettings().setProperty( "proxySet", "false" );
					removeProperty( "http.proxyHost" );
					removeProperty( "http.proxyPort" );
					removeProperty( "http.proxyUser" );
					removeProperty( "http.proxyPassword" );
				}

				// Next, change the server that's used to login;
				// find out the selected index.

				if ( serverSelect.getSelectedIndex() == 0 )
					KoLRequest.autoDetectServer();
				else
					KoLRequest.setLoginServer( "www." + serverSelect.getSelectedIndex() + ".kingdomofloathing.com" );

				client.getSettings().setProperty( "loginServer", "" + serverSelect.getSelectedIndex() );

				// Save the settings that were just set; that way,
				// the next login can use them.

				client.getSettings().saveSettings();

				// Finally, switch the card display back to the
				// login card so the user can login with their
				// new login preferences.

				(new SwitchCardDisplay( LOGIN_CARD )).run();
			}

			private void setProperty( String key, String value )
			{
				client.getSettings().setProperty( key, value );
				System.getProperties().setProperty( key, value );
			}

			private void removeProperty( String key )
			{
				client.getSettings().remove( key );
				System.getProperties().remove( key );
			}
		}
	}

	private class SwitchCardDisplay implements Runnable
	{
		private String card;

		public SwitchCardDisplay( String card )
		{	this.card = card;
		}

		public void run()
		{
			if ( !SwingUtilities.isEventDispatchThread() )
			{
				SwingUtilities.invokeLater( this );
				return;
			}

			cards.show( getContentPane(), card );
		}
	}
}
