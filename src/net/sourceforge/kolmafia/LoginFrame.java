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

// spellcast-related imports
import net.java.dev.spellcast.utilities.LicenseDisplay;
import net.java.dev.spellcast.utilities.ActionVerifyPanel;
import net.java.dev.spellcast.utilities.JComponentUtilities;

/**
 * An extended <code>JFrame</code> which presents the user with the ability to
 * login to the Kingdom of Loathing.  Essentially, this class is a modification
 * of the <code>LoginDialog</code> class from the Spellcast project to contain
 * a single panel and to be properly named.
 */

public class LoginFrame extends KoLFrame
{
	private LoginPanel contentPanel;
	private KoLmafia client;

	public LoginFrame( KoLmafia client )
	{
		super( "KoLmafia: Login" );
		setResizable( false );

		CardLayout cards = new CardLayout( 10, 10 );
		getContentPane().setLayout( cards );

		this.client = client;
		contentPanel = new LoginPanel();
		getContentPane().add( contentPanel, "" );

		updateDisplay( PRE_LOGIN_STATE, " " );
		setDefaultCloseOperation( DISPOSE_ON_CLOSE );

		// adding a menu

		JMenuBar menuBar = new JMenuBar();  setJMenuBar( menuBar );
		JMenu menu = new JMenu("Help");  menu.setMnemonic( KeyEvent.VK_H );  menuBar.add( menu );
		JMenuItem menuItem = new JMenuItem( "Copyright", KeyEvent.VK_C );
		menuItem.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent e)
			{	new LicenseDisplay( "KoLmafia: Copyright Notice" );
			}
		});
		menu.add( menuItem );
	}

	public void updateDisplay( int displayState, String message )
	{	(new DisplayLoginStatus( displayState, message )).run();
	}

	private class LoginPanel extends ActionVerifyPanel
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

			JPanel labels = new JPanel();

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
			Runnable updateAComponent = new Runnable() {
				public void run()
				{
					loginnameField.setText("");
					passwordField.setText("");
					requestFocus();
				}
			};
			SwingUtilities.invokeLater(updateAComponent);
		}

		protected void actionConfirmed()
		{
			updateDisplay( SENDING_LOGIN_STATE, "Sending login..." );

			String loginname = loginnameField.getText();
			String password = new String( passwordField.getPassword() );

			if ( loginname.equals("") || password.equals("") )
			{
				updateDisplay( PRE_LOGIN_STATE, "Invalid login." );
				return;
			}

			(new LoginRequest( client, loginname, password )).start();
		}

		protected void actionCancelled()
		{
			updateDisplay( PRE_LOGIN_STATE, "Login cancelled." );
			requestFocus();
		}

		public void requestFocus()
		{	loginnameField.requestFocus();
		}
	}

	/**
	 * A <code>Runnable</code> object which can be placed inside of
	 * a call to <code>javax.swing.SwingUtilities.invokeLater()</code>
	 * to ensure that the GUI is only modified inside of the AWT thread.
	 */

	private class DisplayLoginStatus implements Runnable
	{
		private int displayState;
		private String status;

		public DisplayLoginStatus( int displayState, String status )
		{
			this.displayState = displayState;
			this.status = status;
		}

		public void run()
		{
			if ( !SwingUtilities.isEventDispatchThread() )
			{
				SwingUtilities.invokeLater( this );
				return;
			}

			contentPanel.setStatusMessage( status );
			showAppropriateCard();
		}

		private void showAppropriateCard()
		{
			switch ( displayState )
			{
				case PRE_LOGIN_STATE:
					contentPanel.setEnabled( true );
					contentPanel.clear();
					break;

				case SENDING_LOGIN_STATE:
					contentPanel.setEnabled( false );
					break;

				case LOGGED_IN_STATE:
					break;
			}
		}
	}
}
