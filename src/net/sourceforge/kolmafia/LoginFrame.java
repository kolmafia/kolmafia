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
import javax.swing.Box;
import javax.swing.BoxLayout;
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
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.JPasswordField;
import javax.swing.JComboBox;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JMenuBar;
import javax.swing.JCheckBox;
import javax.swing.text.JTextComponent;

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
		super( "KoLmafia: Login", client );
		setResizable( false );

		this.client = client;
		this.saveStateNames = new SortedListModel();
		this.saveStateNames.addAll( saveStateNames );
		contentPanel = new LoginPanel();
		getContentPane().add( contentPanel, BorderLayout.CENTER );

		updateDisplay( ENABLED_STATE, " " );
		setDefaultCloseOperation( DISPOSE_ON_CLOSE );
		addWindowListener( new ExitRequestAdapter() );

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

		addScriptMenu( menuBar );
		addConfigureMenu( menuBar );
		addHelpMenu( menuBar );
	}

	/**
	 * An internal class which represents the panel which is nested
	 * inside of the <code>LoginFrame</code>.
	 */

	private class LoginPanel extends KoLPanel implements ActionListener
	{
		private JPanel actionStatusPanel;
		private JLabel actionStatusLabel;

		private JComponent loginnameField;
		private JPasswordField passwordField;
		private JCheckBox getBreakfastCheckBox;
		private JCheckBox savePasswordCheckBox;
		private JCheckBox autoLoginCheckBox;

		/**
		 * Constructs a new <code>LoginPanel</code>, containing a place
		 * for the users to input their login name and password.  This
		 * panel, because it is intended to be the content panel for
		 * status message updates, also has a status label.
		 */

		public LoginPanel()
		{
			super( "login", "cancel" );

			actionStatusPanel = new JPanel();
			actionStatusPanel.setLayout( new GridLayout( 2, 1 ) );

			actionStatusLabel = new JLabel( " ", JLabel.CENTER );
			actionStatusPanel.add( actionStatusLabel );
			actionStatusPanel.add( new JLabel( " ", JLabel.CENTER ) );

			loginnameField = client == null || client.getSettings().getProperty( "saveState" ) == null ||
				client.getSettings().getProperty( "saveState" ).equals( "" ) ? (JComponent)(new JTextField()) :
					(JComponent)(new LoginNameComboBox());

			if ( loginnameField instanceof JComboBox )
				((JComboBox)loginnameField).setEditable( true );

			passwordField = new JPasswordField();
			savePasswordCheckBox = new JCheckBox();
			savePasswordCheckBox.addActionListener( this );

			autoLoginCheckBox = new JCheckBox();
			getBreakfastCheckBox = new JCheckBox();

			JPanel checkBoxPanels = new JPanel();
			checkBoxPanels.setLayout( new BoxLayout( checkBoxPanels, BoxLayout.X_AXIS ) );
			checkBoxPanels.add( Box.createHorizontalStrut( 20 ) );
			checkBoxPanels.add( new JLabel( "Save Password: " ), "" );
			checkBoxPanels.add( savePasswordCheckBox );
			checkBoxPanels.add( Box.createHorizontalStrut( 20 ) );
			checkBoxPanels.add( new JLabel( "Auto-Login: " ), "" );
			checkBoxPanels.add( autoLoginCheckBox );
			checkBoxPanels.add( Box.createHorizontalStrut( 20 ) );
			checkBoxPanels.add( new JLabel( "Get Breakfast: " ), "" );
			checkBoxPanels.add( getBreakfastCheckBox );

			JPanel southPanel = new JPanel();
			southPanel.setLayout( new BorderLayout( 10, 10 ) );
			southPanel.add( checkBoxPanels, BorderLayout.NORTH );
			southPanel.add( new JPanel(), BorderLayout.CENTER );
			southPanel.add( actionStatusPanel, BorderLayout.SOUTH );

			JPanel imagePanel = new JPanel();
			imagePanel.setLayout( new BorderLayout( 0, 0 ) );
			imagePanel.add( new JLabel( " " ), BorderLayout.NORTH );
			imagePanel.add( new JLabel( JComponentUtilities.getSharedImage( "penguin.gif" ), JLabel.CENTER ), BorderLayout.SOUTH );


			VerifiableElement [] elements = new VerifiableElement[2];
			elements[0] = new VerifiableElement( "Login: ", loginnameField );
			elements[1] = new VerifiableElement( "Password: ", passwordField );

			setContent( elements );
			add( imagePanel, BorderLayout.NORTH );
			add( southPanel, BorderLayout.SOUTH );

			String autoLoginSetting =  client.getSettings().getProperty( "autoLogin" );
			if ( autoLoginSetting != null )
			{
				if ( loginnameField instanceof JComboBox )
					((JComboBox)loginnameField).setSelectedItem( autoLoginSetting );
				passwordField.setText( client.getSaveState( autoLoginSetting ) );
				savePasswordCheckBox.setSelected( true );
				autoLoginCheckBox.setSelected( true );
			}
		}

		public void setStatusMessage( String s )
		{	actionStatusLabel.setText( s );
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

		public void clear()
		{	requestFocus();
		}

		protected void actionConfirmed()
		{	(new LoginRequestThread()).start();
		}

		protected void actionCancelled()
		{
			updateDisplay( ENABLED_STATE, "Login cancelled." );
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
		 * In order to keep the user interface from freezing (or at
		 * least appearing to freeze), this internal class is used
		 * to actually make the login attempt.
		 */

		private class LoginRequestThread extends Thread
		{
			public LoginRequestThread()
			{
				super( "Login-Request-Thread" );
				setDaemon( true );
			}

			public void run()
			{
				String loginname = ((String)(loginnameField instanceof JComboBox ?
					((JComboBox)loginnameField).getSelectedItem() : ((JTextField)loginnameField).getText() )).trim();

				String password = new String( passwordField.getPassword() ).trim();

				if ( loginname == null || password == null || loginname.equals("") || password.equals("") )
				{
					updateDisplay( ENABLED_STATE, "Invalid login." );
					return;
				}

				if ( autoLoginCheckBox.isSelected() )
					client.getSettings().setProperty( "autoLogin", loginname );
				else
				{
					client.getSettings().remove( "autoLogin" );
					client.getSettings().saveSettings();
				}

				updateDisplay( DISABLED_STATE, "Determining login settings..." );
				(new LoginRequest( client, loginname, password, getBreakfastCheckBox.isSelected(), savePasswordCheckBox.isSelected() )).run();
			}
		}

		/**
		 * Special instance of a JComboBox which overrides the default
		 * key events of a JComboBox to allow you to catch key events.
		 */

		private class LoginNameComboBox extends JComboBox implements FocusListener
		{
			private String currentName;
			private String currentMatch;

			public LoginNameComboBox()
			{
				super( saveStateNames );
				setEditable( true );
				getEditor().getEditorComponent().addFocusListener( this );
				getEditor().getEditorComponent().addKeyListener( new NameInputListener() );
			}

			public void setSelectedItem( Object anObject )
			{
				if ( anObject == null )
					return;

				// Look up to see if there's a password that's
				// associated with the current name

				if ( !saveStateNames.contains( anObject ) )
					saveStateNames.add( anObject );

				super.setSelectedItem( anObject );

				String password = client.getSaveState( (String) anObject );
				if ( password != null )
				{
					passwordField.setText( password );
					savePasswordCheckBox.setSelected( true );
				}
				else if ( !savePasswordCheckBox.isSelected() )
				{
					passwordField.setText( "" );
					savePasswordCheckBox.setSelected( false );
				}
			}

			public void focusGained( FocusEvent e )
			{	getEditor().selectAll();
			}

			public void focusLost( FocusEvent e )
			{	setSelectedItem( (currentMatch == null) ? currentName : currentMatch );
			}

			private class NameInputListener extends KeyAdapter
			{
				public void keyReleased( KeyEvent e )
				{
					if ( e.getKeyCode() == KeyEvent.VK_ENTER )
					{
						passwordField.requestFocus();
						return;
					}
					else if ( e.getKeyChar() == KeyEvent.CHAR_UNDEFINED )
						return;

					// If it wasn't the enter key that was being released,
					// then make sure that the current name is stored
					// before the key typed event is fired

					currentName = ((String) getEditor().getItem()).trim();
					currentMatch = null;

					// Autohighlight and popup - note that this
					// should only happen for standard typing
					// keys, or the delete and backspace keys.

					boolean matchNotFound = true;
					Object [] currentNames = saveStateNames.toArray();

					if ( currentName.length() > 0 )
					{
						for ( int i = 0; i < currentNames.length && matchNotFound; ++i )
						{
							if ( ((String)currentNames[i]).toLowerCase().startsWith( currentName.toLowerCase() ) )
							{
								showPopup();
								setSelectedIndex(i);
								matchNotFound = false;

								if ( e.getKeyCode() == KeyEvent.VK_BACK_SPACE || e.getKeyCode() == KeyEvent.VK_DELETE )
								{
									// If this was an undefined character, then it
									// was a backspace or a delete - in this case,
									// you retain the original name after selecting
									// the index.

									getEditor().setItem( currentName );
								}
								else
								{
									// If this wasn't an undefined character, then
									// the user wants autocompletion!  Highlight
									// the rest of the possible name.

									currentMatch = (String) currentNames[i];
									getEditor().setItem( currentMatch );
									JTextComponent editor = (JTextComponent) getEditor().getEditorComponent();
									editor.setSelectionStart( currentName.length() );
									editor.setSelectionEnd( currentMatch.length() );
								}
							}
						}
					}

					// In the event that no match was found (or the
					// user hasn't entered anything), there is no
					// need to enter the loop

					if ( matchNotFound )
						hidePopup();
				}
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
}
