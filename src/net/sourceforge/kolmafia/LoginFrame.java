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
import java.awt.GridLayout;
import java.awt.BorderLayout;

// event listeners
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

// containers
import javax.swing.JToolBar;
import javax.swing.JTabbedPane;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.JPasswordField;
import javax.swing.JComboBox;
import javax.swing.JCheckBox;
import javax.swing.JOptionPane;

// other imports
import net.java.dev.spellcast.utilities.SortedListModel;
import net.java.dev.spellcast.utilities.LockableListModel;
import net.java.dev.spellcast.utilities.JComponentUtilities;

/**
 * An extended <code>KoLFrame</code> which presents the user with the ability to
 * login to the Kingdom of Loathing.  Essentially, this class is a modification
 * of the <code>LoginDialog</code> class from the Spellcast project.
 */

public class LoginFrame extends KoLFrame
{
	private SortedListModel saveStateNames;

	public LoginFrame( KoLmafia client )
	{	this( client, new SortedListModel() );
	}

	/**
	 * Constructs a new <code>LoginFrame</code> which allows the user to
	 * log into the Kingdom of Loathing.  The <code>LoginFrame</code>
	 * assigns its <code>LoginPanel</code> as the content panel used by
	 * <code>KoLPanel</code> and other classes for updating its display,
	 * and derived classes may access the <code>LoginPanel</code> indirectly
	 * in this fashion.
	 *
	 * @param	client	The client associated with this <code>LoginFrame</code>.
	 */

	public LoginFrame( KoLmafia client, SortedListModel saveStateNames )
	{
		super( client, "Login" );
		JTabbedPane tabs = new JTabbedPane();

		this.saveStateNames = new SortedListModel();
		this.saveStateNames.addAll( saveStateNames );

		tabs.addTab( "Login", constructLoginPanel() );

		JPanel connectPanel = new JPanel();
		connectPanel.setLayout( new BoxLayout( connectPanel, BoxLayout.Y_AXIS ) );
		connectPanel.add( new ServerSelectPanel() );
		connectPanel.add( new ProxyOptionsPanel() );
		tabs.addTab( "Settings", connectPanel );

		framePanel.setLayout( new CardLayout( 10, 10 ) );
		framePanel.add( tabs, "" );

		addWindowListener( new ExitRequestAdapter() );

		toolbarPanel.add( new DisplayFrameButton( "KoL Almanac", "calendar.gif", CalendarFrame.class ) );
		toolbarPanel.add( new DisplayFrameButton( "KoL Encyclopedia", "encyclopedia.gif", ExamineItemsFrame.class ) );

		toolbarPanel.add( new JToolBar.Separator() );

		toolbarPanel.add( new DisplayFrameButton( "Preferences", "preferences.gif", OptionsFrame.class ) );
	}

	public void dispose()
	{
		saveStateNames = null;
		super.dispose();
	}

	public JPanel constructLoginPanel()
	{
		JPanel imagePanel = new JPanel();
		imagePanel.setLayout( new BorderLayout( 0, 0 ) );
		imagePanel.add( new JLabel( " " ), BorderLayout.NORTH );
		imagePanel.add( new JLabel( JComponentUtilities.getSharedImage( "lantern.jpg" ), JLabel.CENTER ), BorderLayout.SOUTH );

		JPanel containerPanel = new JPanel( new BorderLayout() );
		containerPanel.add( imagePanel, BorderLayout.NORTH );
		containerPanel.add( new LoginPanel(), BorderLayout.CENTER );
		return containerPanel;
	}

	/**
	 * An internal class which represents the panel which is nested
	 * inside of the <code>LoginFrame</code>.
	 */

	private class LoginPanel extends KoLPanel implements ActionListener
	{
		private JComponent loginnameField;
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
			super( "login", "cancel" );

			loginnameField = GLOBAL_SETTINGS.getProperty( "saveState" ).equals( "" ) ? (JComponent)(new JTextField()) : (JComponent)(new LoginNameComboBox());
			passwordField = new JPasswordField();
			savePasswordCheckBox = new JCheckBox();
			savePasswordCheckBox.addActionListener( this );

			autoLoginCheckBox = new JCheckBox();
			getBreakfastCheckBox = new JCheckBox();

			JPanel checkBoxPanels = new JPanel();
			checkBoxPanels.add( Box.createHorizontalStrut( 20 ) );
			checkBoxPanels.add( new JLabel( "Save Password: " ), "" );
			checkBoxPanels.add( savePasswordCheckBox );
			checkBoxPanels.add( Box.createHorizontalStrut( 20 ) );
			checkBoxPanels.add( new JLabel( "Auto-Login: " ), "" );
			checkBoxPanels.add( autoLoginCheckBox );
			checkBoxPanels.add( Box.createHorizontalStrut( 20 ) );
			checkBoxPanels.add( new JLabel( "Get Breakfast: " ), "" );
			checkBoxPanels.add( getBreakfastCheckBox );
			checkBoxPanels.add( Box.createHorizontalStrut( 20 ) );

			VerifiableElement [] elements = new VerifiableElement[2];
			elements[0] = new VerifiableElement( "Login: ", loginnameField );
			elements[1] = new VerifiableElement( "Password: ", passwordField );

			setContent( elements );

			actionStatusPanel.add( new JLabel( " ", JLabel.CENTER ), BorderLayout.CENTER );
			actionStatusPanel.add( checkBoxPanels, BorderLayout.NORTH );

			if ( client != null )
			{
				String autoLoginSetting = GLOBAL_SETTINGS.getProperty( "autoLogin" );

				if ( loginnameField instanceof JComboBox )
					((JComboBox)loginnameField).setSelectedItem( autoLoginSetting );

				String passwordSetting = client.getSaveState( autoLoginSetting );

				if ( passwordSetting != null )
				{
					passwordField.setText( passwordSetting );
					savePasswordCheckBox.setSelected( true );
					autoLoginCheckBox.setSelected( true );
				}

				getBreakfastCheckBox.setSelected( getProperty( "alwaysGetBreakfast" ).equals( "true" ) );
			}

			setDefaultButton( confirmedButton );
		}

		public void setEnabled( boolean isEnabled )
		{
			super.setEnabled( isEnabled );
			loginnameField.setEnabled( isEnabled );
			passwordField.setEnabled( isEnabled );
			savePasswordCheckBox.setEnabled( isEnabled );
			autoLoginCheckBox.setEnabled( isEnabled );
			getBreakfastCheckBox.setEnabled( isEnabled );
		}

		protected void actionConfirmed()
		{
			setProperty( "alwaysGetBreakfast", String.valueOf( getBreakfastCheckBox.isSelected() ) );
			String loginname = ((String)(loginnameField instanceof JComboBox ?
				((JComboBox)loginnameField).getSelectedItem() : ((JTextField)loginnameField).getText() ));

			String password = new String( passwordField.getPassword() );

			if ( loginname == null || password == null || loginname.equals("") || password.equals("") )
			{
				client.updateDisplay( ERROR_STATE, "Invalid login." );
				return;
			}

			if ( autoLoginCheckBox.isSelected() )
				setProperty( "autoLogin", loginname );
			else
				setProperty( "autoLogin", "" );

			if ( !loginname.endsWith( "/q" ) )
				loginname += "/q";

			client.updateDisplay( DISABLE_STATE, "Determining login settings..." );
			(new LoginRequest( client, loginname, password, savePasswordCheckBox.isSelected(), getBreakfastCheckBox.isSelected() )).run();
		}

		protected void actionCancelled()
		{
			client.updateDisplay( ERROR_STATE, "Login cancelled." );
			client.cancelRequest();
			requestFocus();
		}

		public void requestFocus()
		{	loginnameField.requestFocus();
		}

		public void actionPerformed( ActionEvent e )
		{
			if ( !savePasswordCheckBox.isSelected() && loginnameField instanceof JComboBox )
				client.removeSaveState( (String) ((JComboBox)loginnameField).getSelectedItem() );
		}

		/**
		 * Special instance of a JComboBox which overrides the default
		 * key events of a JComboBox to allow you to catch key events.
		 */

		private class LoginNameComboBox extends MutableComboBox
		{
			public LoginNameComboBox()
			{
				super( saveStateNames );
				this.getEditor().getEditorComponent().addKeyListener( new PasswordFocusListener() );
			}

			public void setSelectedItem( Object anObject )
			{
				super.setSelectedItem( anObject );
				setPassword();
			}

			public void focusLost( FocusEvent e )
			{
				super.focusLost( e );
				setPassword();
			}

			private void setPassword()
			{
				if ( currentMatch == null )
				{
					passwordField.setText( "" );
					savePasswordCheckBox.setSelected( false );
					return;
				}

				String password = client.getSaveState( currentMatch );
				if ( password != null )
				{
					passwordField.setText( password );
					savePasswordCheckBox.setSelected( true );
				}
				else
				{
					passwordField.setText( "" );
					savePasswordCheckBox.setSelected( false );
				}
			}

			private class PasswordFocusListener extends KeyAdapter
			{
				public void keyReleased( KeyEvent e )
				{
					if ( e.getKeyCode() == KeyEvent.VK_ENTER )
						passwordField.requestFocus();
				}
			}
		}
	}

	/**
	 * Allows the user to select a server to use when
	 * using KoLmafia to login.  Also allows the user
	 * to select the framing mode to use.
	 */

	private class ServerSelectPanel extends LabeledKoLPanel
	{
		private JComboBox servers, uimodes, toolbars, trayicon;

		public ServerSelectPanel()
		{
			super( "Server Select", new Dimension( 80, 20 ), new Dimension( 240, 20 ) );

			servers = new JComboBox();
			servers.addItem( "Auto-detect login server" );
			for ( int i = 1; i <= KoLRequest.SERVER_COUNT; ++i )
				servers.addItem( "Use login server " + i );

			uimodes = new JComboBox();
			uimodes.addItem( "Normal mode" );
			uimodes.addItem( "Run a buffbot" );
			uimodes.addItem( "Loathing chat" );
			uimodes.addItem( "Clan manager" );
			uimodes.addItem( "Mini-browser" );

			toolbars = new JComboBox();
			toolbars.addItem( "Show global menus only" );
			toolbars.addItem( "Put toolbar along top of panel" );
			toolbars.addItem( "Put toolbar along bottom of panel" );
			toolbars.addItem( "Put toolbar left of panel" );
			toolbars.addItem( "Put toolbar right of panel" );

			trayicon = new JComboBox();
			trayicon.addItem( "Minimize KoLmafia to taskbar" );
			trayicon.addItem( "Minimize KoLmafia to system tray" );

			VerifiableElement [] elements = new VerifiableElement[ System.getProperty( "os.name" ).startsWith( "Windows" ) ? 4 : 3 ];
			elements[0] = new VerifiableElement( "Server: ", servers );
			elements[1] = new VerifiableElement( "Startup: ", uimodes );
			elements[2] = new VerifiableElement( "Toolbars: ", toolbars );

			if ( System.getProperty( "os.name" ).startsWith( "Windows" ) )
				elements[3] = new VerifiableElement( "SysTray: ", trayicon );

			setContent( elements );
			actionCancelled();
		}

		protected void actionConfirmed()
		{
			GLOBAL_SETTINGS.setProperty( "loginServer", String.valueOf( servers.getSelectedIndex() ) );
			GLOBAL_SETTINGS.setProperty( "userInterfaceMode", String.valueOf( uimodes.getSelectedIndex() ) );
			GLOBAL_SETTINGS.setProperty( "useToolbars", String.valueOf( toolbars.getSelectedIndex() != 0 ) );
			GLOBAL_SETTINGS.setProperty( "toolbarPosition", String.valueOf( toolbars.getSelectedIndex() ) );
			GLOBAL_SETTINGS.setProperty( "useSystemTrayIcon", String.valueOf( trayicon.getSelectedIndex() == 1 ) );
		}

		protected void actionCancelled()
		{
			servers.setSelectedIndex( Integer.parseInt( GLOBAL_SETTINGS.getProperty( "loginServer" ) ) );
			uimodes.setSelectedIndex( Integer.parseInt( GLOBAL_SETTINGS.getProperty( "userInterfaceMode" ) ) );
			toolbars.setSelectedIndex( Integer.parseInt( GLOBAL_SETTINGS.getProperty( "toolbarPosition" ) ) );
			trayicon.setSelectedIndex( GLOBAL_SETTINGS.getProperty( "useSystemTrayIcon" ).equals( "true" ) ? 1 : 0 );
		}
	}

	/**
	 * This panel handles all of the things related to proxy
	 * options (if applicable).
	 */

	private class ProxyOptionsPanel extends LabeledKoLPanel
	{
		private ProxySettingsCheckBox proxySet;

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
			super( "Proxy Setup", new Dimension( 80, 20 ), new Dimension( 240, 20 ) );

			proxySet = new ProxySettingsCheckBox();

			proxyHost = new JTextField();
			proxyPort = new JTextField();
			proxyLogin = new JTextField();
			proxyPassword = new JPasswordField();

			VerifiableElement [] elements = new VerifiableElement[5];
			elements[0] = new VerifiableElement( "Use Proxy: ", proxySet );
			elements[1] = new VerifiableElement( "Proxy Host: ", proxyHost );
			elements[2] = new VerifiableElement( "Proxy Port: ", proxyPort );
			elements[3] = new VerifiableElement( "Proxy Login: ", proxyLogin );
			elements[4] = new VerifiableElement( "Proxy Pass: ", proxyPassword );

			setContent( elements, true );
			actionCancelled();
		}

		protected void actionConfirmed()
		{
			setProperty( "proxySet", String.valueOf( proxySet.isSelected() && proxyHost.getText().trim().length() > 0 ) );
			setProperty( "http.proxyHost", proxyHost.getText() );
			setProperty( "http.proxyPort", proxyPort.getText() );
			setProperty( "http.proxyUser", proxyLogin.getText() );
			setProperty( "http.proxyPassword", proxyPassword.getText() );

			// Save the settings that were just set; that way,
			// the next login can use them.

			KoLRequest.applySettings();
		}

		protected void actionCancelled()
		{
			proxySet.setSelected( getProperty( "proxySet" ).equals( "true" ) );
			proxyHost.setText( getProperty( "http.proxyHost" ) );
			proxyPort.setText( getProperty( "http.proxyPort" ) );
			proxyLogin.setText( getProperty( "http.proxyUser" ) );
			proxyPassword.setText( getProperty( "http.proxyPassword" ) );

			proxySet.actionPerformed( null );
		}

		private class ProxySettingsCheckBox extends JCheckBox implements ActionListener
		{
			public ProxySettingsCheckBox()
			{	addActionListener( this );
			}

			public void actionPerformed( ActionEvent e )
			{
				proxyHost.setEnabled( isSelected() );
				proxyPort.setEnabled( isSelected() );
				proxyLogin.setEnabled( isSelected() );
				proxyPassword.setEnabled( isSelected() );
			}
		}
	}

	/**
	 * Formally exits the program if there are no active sessions when
	 * this frame is closed.
	 */

	private class ExitRequestAdapter extends WindowAdapter
	{
		public void windowClosed( WindowEvent e )
		{
			if ( client == null || client.inLoginState() )
				System.exit( 0 );
		}
	}

	public static void main( String [] args )
	{	(new CreateFrameRunnable( LoginFrame.class )).run();
	}
}
