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

// utilities
import java.util.StringTokenizer;
import net.java.dev.spellcast.utilities.LockableListModel;

public class OptionsFrame extends KoLFrame
{
	public OptionsFrame( KoLmafia client )
	{
		super( "KoLmafia: Preferences (Global)", client );
		setResizable( false );

		getContentPane().setLayout( new BorderLayout() );

		JTabbedPane tabs = new JTabbedPane();

		// Because none of the frames support setStatusMessage,
		// the content panel is arbitrary

		this.client = client;
		contentPanel = new LoginOptionsPanel();

		tabs.setBackground( contentPanel.getBackground() );
		tabs.addTab( "Login", contentPanel );
		tabs.addTab( "Sewer", new SewerOptionsPanel() );

		getContentPane().add( tabs, BorderLayout.CENTER );

		updateDisplay( ENABLED_STATE, " " );
		setDefaultCloseOperation( DISPOSE_ON_CLOSE );
	}

	private class LoginOptionsPanel extends KoLPanel
	{
		private JComboBox serverSelect;
		private JTextField proxyHost;
		private JTextField proxyPort;
		private JTextField proxyLogin;
		private JTextField proxyPassword;

		public LoginOptionsPanel()
		{
			super( "apply", "defaults", new Dimension( 120, 20 ), new Dimension( 165, 20 ) );

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

					proxyHost.setText( http_proxyHost );
					proxyPort.setText( http_proxyPort );
					proxyLogin.setText( http_proxyUser );
					proxyPassword.setText( http_proxyPassword );
				}
				else
				{
					proxyHost.setText( "" );
					proxyPort.setText( "" );
					proxyLogin.setText( "" );
					proxyPassword.setText( "" );
				}
			}
		}

		private class StoreSettingsThread extends Thread
		{
			public void run()
			{
				// First, determine what the proxy settings chosen
				// by the user.

				if ( proxyHost.getText().trim().length() != 0 )
				{
					setProperty( "proxySet", "true" );
					setProperty( "http.proxyHost", proxyHost.getText() );
					setProperty( "http.proxyPort", proxyPort.getText() );

					if ( proxyLogin.getText().trim().length() != 0 )
					{
						setProperty( "http.proxyUser", proxyLogin.getText() );
						setProperty( "http.proxyPassword", proxyPassword.getText().trim().length() != 0 ?
							proxyPassword.getText() : "anonymoususer@defaultmailserver.com" );
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

	private class SewerOptionsPanel extends KoLPanel
	{
		private JCheckBox [] items;
		private JLabel actionStatusLabel;
		private JPanel actionStatusPanel;

		private final String [] itemnames = { "seal-clubbing club", "seal tooth", "helmet turtle",
			"scroll of turtle summoning", "pasta spoon", "ravioli hat", "saucepan", "spices", "disco mask",
			"disco ball", "stolen accordion", "mariachi pants", "worthless trinket" };

		public SewerOptionsPanel()
		{
			super( "apply", "defaults", new Dimension( 200, 20 ), new Dimension( 20, 20 ) );

			actionStatusPanel = new JPanel();
			actionStatusPanel.setLayout( new GridLayout( 2, 1 ) );

			actionStatusLabel = new JLabel( " ", JLabel.CENTER );
			actionStatusPanel.add( actionStatusLabel );
			actionStatusPanel.add( new JLabel( " ", JLabel.CENTER ) );

			items = new JCheckBox[ itemnames.length ];
			for ( int i = 0; i < itemnames.length; ++i )
				items[i] = new JCheckBox();

			(new LoadDefaultSettingsThread()).run();

			VerifiableElement [] elements = new VerifiableElement[ itemnames.length ];
			for ( int i = 0; i < itemnames.length; ++i )
				elements[i] = new VerifiableElement( itemnames[i], JLabel.LEFT, items[i] );

			java.util.Arrays.sort( elements );
			setContent( elements, false );
			add( actionStatusPanel, BorderLayout.SOUTH );
		}

		public void setStatusMessage( String s )
		{	actionStatusLabel.setText( s );
		}

		public void clear()
		{	(new LoadDefaultSettingsThread()).start();
		}

		protected void actionConfirmed()
		{	(new StoreSettingsThread()).start();
		}

		protected void actionCancelled()
		{	(new LoadDefaultSettingsThread()).start();
		}

		private class LoadDefaultSettingsThread extends Thread
		{
			public LoadDefaultSettingsThread()
			{	setDaemon( true );
			}

			public void run()
			{
				String settings = client.getSettings().getProperty( "luckySewer" );

				// If there are no default sewer settings, simply skip the
				// attempt at loading them.

				if ( settings == null )
					return;

				// If there are default sewer settings, make sure that the
				// appropriate checkboxes are checked.

				StringTokenizer st = new StringTokenizer( settings, "," );
				for ( int i = 0; i < itemnames.length; ++i )
					items[i].setSelected( false );

				for ( int i = 0; i < 3; ++i )
					items[ Integer.parseInt( st.nextToken() ) - 1 ].setSelected( true );

				(new StatusMessageChanger( "" )).run();
			}
		}

		private class StoreSettingsThread extends Thread
		{
			public void run()
			{
				int [] selected = new int[3];
				int selectedCount = 0;

				for ( int i = 0; i < itemnames.length; ++i )
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

				client.getSettings().setProperty( "luckySewer", selected[0] + "," + selected[1] + "," + selected[2] );
				client.getSettings().saveSettings();

				(new StatusMessageChanger( "Settings saved." )).run();
			}
		}
	}
}