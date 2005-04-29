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

// containers
import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JFileChooser;
import javax.swing.JButton;
import javax.swing.Box;
import javax.swing.JList;
import javax.swing.JScrollPane;

// layout
import java.awt.Color;
import java.awt.Dimension;
import java.awt.CardLayout;
import java.awt.GridLayout;
import java.awt.BorderLayout;
import javax.swing.BoxLayout;

// event listeners
import java.awt.event.KeyEvent;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowAdapter;
import javax.swing.ListSelectionModel;

// other stuff
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import javax.swing.SwingUtilities;
import java.lang.reflect.Constructor;
import net.java.dev.spellcast.utilities.LicenseDisplay;
import net.java.dev.spellcast.utilities.ActionVerifyPanel;
import net.java.dev.spellcast.utilities.JComponentUtilities;
import net.java.dev.spellcast.utilities.LockableListModel;

/**
 * An extended <code>JFrame</code> which provides all the frames in
 * KoLmafia the ability to update their displays, given some integer
 * value and the message to use for updating.
 */

public abstract class KoLFrame extends javax.swing.JFrame implements KoLConstants
{
	private static final String [] LICENSE_FILENAME = { "kolmafia-license.gif", "spellcast-license.gif", "browserlauncher-license.htm" };
	private static final String [] LICENSE_NAME = { "KoLmafia BSD", "Spellcast BSD", "BrowserLauncher" };

	protected boolean isEnabled;
	protected KoLmafia client;
	protected KoLPanel contentPanel;
	protected boolean isExecutingScript;

	protected static CharsheetFrame statusPane;
	protected static GearChangeFrame gearChanger;
	protected static ItemManageFrame itemManager;
	protected static MailboxFrame mailboxDisplay;
	protected static KoLMessenger kolchat;
	protected static List existingFrames = new ArrayList();

	protected JPanel sidePanel;
	protected JLabel hpLabel, mpLabel, advLabel;
	protected JLabel meatLabel, closetLabel, drunkLabel;

	protected JMenuItem statusMenuItem;
	protected JMenuItem mailMenuItem;

	/**
	 * Constructs a new <code>KoLFrame</code> with the given title,
	 * to be associated with the given client.
	 */

	protected KoLFrame( String title, KoLmafia client )
	{
		super( title );

		this.client = client;
		this.isEnabled = true;
		setDefaultCloseOperation( DISPOSE_ON_CLOSE );
	}

	public void addCompactPane()
	{
		JPanel compactPane = new JPanel();
		compactPane.setOpaque( false );
		compactPane.setLayout( new GridLayout( 14, 1 ) );

		compactPane.add( Box.createHorizontalStrut( 80 ) );

		compactPane.add( new JLabel( JComponentUtilities.getSharedImage( "hp.gif" ), JLabel.CENTER ) );
		compactPane.add( hpLabel = new JLabel( " ", JLabel.CENTER ) );

		compactPane.add( new JLabel( JComponentUtilities.getSharedImage( "mp.gif" ), JLabel.CENTER ) );
		compactPane.add( mpLabel = new JLabel( " ", JLabel.CENTER ) );

		compactPane.add( new JLabel( JComponentUtilities.getSharedImage( "meat.gif" ), JLabel.CENTER ) );
		compactPane.add( meatLabel = new JLabel( " ", JLabel.CENTER ) );

		compactPane.add( new JLabel( JComponentUtilities.getSharedImage( "closet.gif" ), JLabel.CENTER ) );
		compactPane.add( closetLabel = new JLabel( " ", JLabel.CENTER ) );

		compactPane.add( new JLabel( JComponentUtilities.getSharedImage( "hourglass.gif" ), JLabel.CENTER ) );
		compactPane.add( advLabel = new JLabel( " ",  JLabel.CENTER) );

		compactPane.add( new JLabel( JComponentUtilities.getSharedImage( "sixpack.gif" ), JLabel.CENTER ) );
		compactPane.add( drunkLabel = new JLabel( " ",  JLabel.CENTER) );

		compactPane.add( Box.createHorizontalStrut( 80 ) );

		this.sidePanel = new JPanel();
		sidePanel.setLayout( new BorderLayout( 0, 0 ) );
		sidePanel.add( compactPane, BorderLayout.NORTH );

		getContentPane().setLayout( new BorderLayout( 0, 0 ) );
		getContentPane().add( sidePanel, BorderLayout.WEST );

		(new StatusRefresher()).run();

		if ( client != null )
			client.getCharacterData().addKoLCharacterListener( new KoLCharacterAdapter( new StatusRefresher() ) );
	}

	protected class StatusRefresher implements Runnable
	{
		public void run()
		{
			KoLCharacter characterData = client == null ? new KoLCharacter( "UI Test" ) : client.getCharacterData();
			hpLabel.setText( characterData.getCurrentHP() + " / " + characterData.getMaximumHP() );
			mpLabel.setText( characterData.getCurrentMP() + " / " + characterData.getMaximumMP() );
			meatLabel.setText( df.format( characterData.getAvailableMeat() ) );
			closetLabel.setText( df.format( characterData.getClosetMeat() ) );
			advLabel.setText( String.valueOf( characterData.getAdventuresLeft() ) );
			drunkLabel.setText( String.valueOf( characterData.getInebriety() ) );
		}
	}

	public void setExtendedState( int state )
	{	super.setExtendedState( state == ICONIFIED ? ICONIFIED : NORMAL );
	}

	/**
	 * Updates the display to reflect the given display state and
	 * to contain the given message.  Note that if there is no
	 * content panel, this method does nothing.
	 */

	public void updateDisplay( int displayState, String message )
	{
		if ( client != null )
			client.getLogStream().println( message );

		if ( contentPanel != null )
			contentPanel.setStatusMessage( isExecutingScript && displayState != ERROR_STATE ? DISABLED_STATE : displayState, message );

		switch ( displayState )
		{
			case ERROR_STATE:
				setEnabled( true );
				break;

			case DISABLED_STATE:
				setEnabled( false );
				break;

			case NOCHANGE:
				break;

			default:
				if ( !isExecutingScript )
					setEnabled( true );
				break;

		}
	}

	/**
	 * Utility method used to give the content panel for this
	 * <code>KoLFrame</code> focus.  Note that if the content
	 * panel is <code>null</code>< this method does nothing.
	 */

	public void requestFocus()
	{
		super.requestFocus();
		if ( contentPanel != null )
			contentPanel.requestFocus();
	}

	protected final JMenu addStatusMenu( JComponent menu )
	{
		JMenu statusMenu = new JMenu( "My KoL" );
		statusMenu.setMnemonic( KeyEvent.VK_M );
		menu.add( statusMenu );

		this.statusMenuItem = new JMenuItem( "Status Pane", KeyEvent.VK_S );
		statusMenuItem.addActionListener( new ViewStatusPaneListener() );
		statusMenu.add( statusMenuItem );

		JMenuItem gearMenuItem = new JMenuItem( "Gear Changer", KeyEvent.VK_G );
		gearMenuItem.addActionListener( new ViewGearChangerListener() );

		statusMenu.add( gearMenuItem );

		JMenuItem itemMenuItem = new JMenuItem( "Item Manager", KeyEvent.VK_I );
		itemMenuItem.addActionListener( new ViewItemManagerListener() );

		statusMenu.add( itemMenuItem );
		return statusMenu;
	}

	/**
	 * Utility method used to add the default <code>KoLmafia</code>
	 * people menu to the given menu bar.  The default menu contains
	 * the ability to open chat, compose green messages, and read
	 * current mail.
	 */

	protected final JMenu addPeopleMenu( JComponent menu )
	{
		JMenu peopleMenu = new JMenu( "People" );
		peopleMenu.setMnemonic( KeyEvent.VK_P );
		menu.add( peopleMenu );

		JMenuItem chatMenuItem = new JMenuItem( "Chat of Loathing", KeyEvent.VK_C );
		chatMenuItem.addActionListener( new ViewChatListener() );

		peopleMenu.add( chatMenuItem );

		JMenuItem composeMenuItem = new JMenuItem( "Green Composer", KeyEvent.VK_G );
		composeMenuItem.addActionListener( new DisplayFrameListener( GreenMessageFrame.class ) );

		peopleMenu.add( composeMenuItem );

		this.mailMenuItem = new JMenuItem( "IcePenguin Express", KeyEvent.VK_I );
		mailMenuItem.addActionListener( new DisplayMailListener() );

		peopleMenu.add( mailMenuItem );
		return peopleMenu;
	}

	/**
	 * Utility method used to add the default <code>KoLmafia</code>
	 * scripting menu to the given menu bar.  The default menu contains
	 * the ability to load scripts.
	 */

	protected final JMenu addScriptMenu( JComponent menu )
	{
		JMenu scriptMenu = new JMenu( "Scripts" );
		scriptMenu.setMnemonic( KeyEvent.VK_S );

		JMenuItem loadScriptMenuItem = new JMenuItem( "Load Script...", KeyEvent.VK_L );
		loadScriptMenuItem.addActionListener( new LoadScriptListener() );

		scriptMenu.add( loadScriptMenuItem );

		JMenuItem loggerItem = new JMenuItem( "", KeyEvent.VK_R );
		loggerItem.addActionListener( new ToggleMacroListener( loggerItem ) );

		scriptMenu.add( loggerItem );
		menu.add( scriptMenu );
		return scriptMenu;
	}

	/**
	 * Utility method used to add the default <code>KoLmafia</code>
	 * configuration menu to the given menu bar.  The default menu
	 * contains the ability to customize preferences (global if it
	 * is invoked before login, character-specific if after) and
	 * initialize the debugger.
	 *
	 * @param	menu	The <code>JMenuBar</code> to which the configuration menu will be attached
	 */

	protected final JMenu addConfigureMenu( JComponent menu )
	{
		JMenu configureMenu = new JMenu("Configure");
		configureMenu.setMnemonic( KeyEvent.VK_C );
		menu.add( configureMenu );

		JMenuItem settingsItem = new JMenuItem( "Preferences", KeyEvent.VK_P );
		settingsItem.addActionListener( new DisplayFrameListener( OptionsFrame.class ) );

		configureMenu.add( settingsItem );

		JMenuItem loggerItem = new JMenuItem( "", KeyEvent.VK_S );
		loggerItem.addActionListener( new ToggleDebugListener( loggerItem ) );

		configureMenu.add( loggerItem );
		return configureMenu;
	}

	/**
	 * Utility method used to add the default <code>KoLmafia</code> Help
	 * menu to the given menu bar.  The default Help menu contains the
	 * copyright statement for <code>KoLmafia</code>.
	 *
	 * @param	menu	The <code>JMenuBar</code> to which the Help menu will be attached
	 */

	protected final JMenu addHelpMenu( JComponent menu )
	{
		JMenu helpMenu = new JMenu("Help");
		helpMenu.setMnemonic( KeyEvent.VK_H );
		menu.add( helpMenu );

		JMenuItem aboutItem = new JMenuItem( "About KoLmafia...", KeyEvent.VK_A );
		aboutItem.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent e)
			{	(new LicenseDisplay( "KoLmafia: Copyright Notice", LICENSE_FILENAME, LICENSE_NAME )).requestFocus();
			}
		});

		helpMenu.add( aboutItem );
		return helpMenu;
	}

	/**
	 * Auxilary method used to enable and disable a frame.  By default,
	 * this attempts to toggle the enable/disable status on the core
	 * content panel.  It is advised that descendants override this
	 * behavior whenever necessary.
	 *
	 * @param	isEnabled	<code>true</code> if the frame is to be re-enabled
	 */

	public void setEnabled( boolean isEnabled )
	{
		this.isEnabled = isEnabled;

		if ( contentPanel != null )
			contentPanel.setEnabled( isEnabled );
	}

	/**
	 * Overrides the default isEnabled() method, because the setEnabled()
	 * method does not call the superclass's version.
	 *
	 * @return	Whether or not this KoLFrame is enabled.
	 */

	public boolean isEnabled()
	{	return isEnabled;
	}

	/**
	 * An internal class which allows focus to be returned to the
	 * client's active frame when auxiliary windows are closed.
	 */

	protected class ReturnFocusAdapter extends WindowAdapter
	{
		public void windowClosing( WindowEvent e )
		{
			if ( client != null )
				client.requestFocus();
			else
				System.exit(0);
		}
	}

	private class ToggleMacroListener implements ActionListener
	{
		private JMenuItem loggerItem;

		public ToggleMacroListener( JMenuItem loggerItem )
		{
			this.loggerItem = loggerItem;
			loggerItem.setText( client == null || client.getMacroStream() instanceof NullStream ?
				"Record Script..." : "Stop Recording" );
		}

		public void actionPerformed(ActionEvent e)
		{	(new MacroRecordThread()).start();
		}

		private class MacroRecordThread extends Thread
		{
			public MacroRecordThread()
			{	setDaemon( true );
			}

			public void run()
			{
				if ( client != null && client.getMacroStream() instanceof NullStream )
				{
					JFileChooser chooser = new JFileChooser( "." );
					int returnVal = chooser.showSaveDialog( KoLFrame.this );

					if ( chooser.getSelectedFile() == null )
						return;

					String filename = chooser.getSelectedFile().getAbsolutePath();

					if ( client != null && returnVal == JFileChooser.APPROVE_OPTION )
						client.initializeMacroStream( filename );

					loggerItem.setText( "Stop Recording" );
				}
				else if ( client != null )
				{
					client.deinitializeMacroStream();
					loggerItem.setText( "Record Script..." );
				}
			}
		}
	}

	private class ToggleDebugListener implements ActionListener
	{
		private JMenuItem loggerItem;

		public ToggleDebugListener( JMenuItem loggerItem )
		{
			this.loggerItem = loggerItem;
			loggerItem.setText( client == null || client.getLogStream() instanceof NullStream ?
				"Start Debug" : "Stop Debug" );
		}

		public void actionPerformed(ActionEvent e)
		{
			if ( client != null && client.getLogStream() instanceof NullStream )
			{
				client.initializeLogStream();
				loggerItem.setText( "Stop Debug" );
			}
			else if ( client != null )
			{
				client.deinitializeLogStream();
				loggerItem.setText( "Start Debug" );
			}
		}
	}

	/**
	 * In order to keep the user interface from freezing (or at least
	 * appearing to freeze), this internal class is used to process
	 * the request for loading a script.
	 */

	private class LoadScriptListener implements ActionListener
	{
		public void actionPerformed( ActionEvent e )
		{	(new LoadScriptThread()).start();
		}

		private class LoadScriptThread extends Thread
		{
			public LoadScriptThread()
			{
				super( "Load-Script-Thread" );
				setDaemon( true );
			}

			public void run()
			{
				JFileChooser chooser = new JFileChooser( "." );
				int returnVal = chooser.showOpenDialog( KoLFrame.this );

				if ( chooser.getSelectedFile() == null )
					return;

				try
				{
					if ( client != null && returnVal == JFileChooser.APPROVE_OPTION )
					{
						isExecutingScript = true;
						(new KoLmafiaCLI( client, chooser.getSelectedFile().getCanonicalPath() )).listenForCommands();
					}

					isExecutingScript = false;
					if ( client.permitsContinue() )
						updateDisplay( ENABLED_STATE, "Script completed successfully." );
					else
						updateDisplay( ERROR_STATE, "Script <" + chooser.getSelectedFile().getName() + "> encountered an error." );
				}
				catch ( Exception e )
				{
					// Here, notify the display that the script
					// file specified could not be loaded

					isExecutingScript = false;
					updateDisplay( ERROR_STATE, "Script file <" + chooser.getSelectedFile().getName() + "> could not be found." );
					return;
				}
			}
		}
	}

	/**
	 * An internal class used as the basis for content panels.  This
	 * class builds upon the <code>ActionVerifyPanel</code> by adding
	 * a <code>setStatusMessage()</code>.
	 */

	protected abstract class KoLPanel extends ActionVerifyPanel
	{
		protected KoLPanel( String confirmedText, String cancelledText )
		{
			super( confirmedText, cancelledText );
		}

		protected KoLPanel( String confirmedText, String cancelledText1, String cancelledText2 )
		{
			super( confirmedText, cancelledText1, cancelledText2 );
		}

		protected KoLPanel( String confirmedText, String cancelledText, Dimension labelSize, Dimension fieldSize )
		{
			super( confirmedText, cancelledText, labelSize, fieldSize );
		}

		protected KoLPanel( String confirmedText, String cancelledText1, String cancelledText2, Dimension labelSize, Dimension fieldSize )
		{
			super( confirmedText, cancelledText1, cancelledText2, labelSize, fieldSize );
		}

		protected KoLPanel( String confirmedText, String cancelledText, Dimension labelSize, Dimension fieldSize, boolean isCenterPanel )
		{
			super( confirmedText, cancelledText, labelSize, fieldSize, isCenterPanel );
		}

		protected KoLPanel( String confirmedText, String cancelledText1, String cancelledText2, Dimension labelSize, Dimension fieldSize, boolean isCenterPanel )
		{
			super( confirmedText, cancelledText1, cancelledText2, labelSize, fieldSize, isCenterPanel );
		}

		public abstract void setStatusMessage( int displayState, String s );
	}

	/**
	 * An internal class used as the basis for non-content panels. This
	 * class builds upon the <code>KoLPanel</code>, but specifically
	 * defines the abstract methods to not do anything.
	 */

	protected abstract class NonContentPanel extends KoLPanel
	{
		protected NonContentPanel( String confirmedText, String cancelledText )
		{
			super( confirmedText, cancelledText );
		}

		protected NonContentPanel( String confirmedText, String cancelledText, Dimension labelSize, Dimension fieldSize )
		{
			super( confirmedText, cancelledText, labelSize, fieldSize );
		}

		public void setStatusMessage( int displayState, String s )
		{
		}
	}

	/**
	 * A generic panel which adds a label to the bottom of the KoLPanel
	 * to update the panel's status.  It also provides a thread which is
	 * guaranteed to be a daemon thread for updating the frame which
	 * also retrieves a reference to the client's current settings.
	 */

	protected abstract class LabeledKoLPanel extends KoLPanel
	{
		private String panelTitle;
		private JPanel actionStatusPanel;
		private JLabel actionStatusLabel;

		public LabeledKoLPanel( String panelTitle, Dimension left, Dimension right )
		{	this( panelTitle, "apply", "defaults", left, right );
		}

		public LabeledKoLPanel( String panelTitle, String confirmButton, String cancelButton, Dimension left, Dimension right )
		{
			super( confirmButton, cancelButton, left, right, true );
			this.panelTitle = panelTitle;
		}

		protected void setContent( VerifiableElement [] elements )
		{	setContent( elements, true );
		}

		protected void setContent( VerifiableElement [] elements, boolean isLabelPreceeding )
		{	setContent( elements, isLabelPreceeding, false );
		}

		protected void setContent( VerifiableElement [] elements, boolean isLabelPreceeding, boolean bothDisabledOnClick )
		{
			super.setContent( elements, null, null, null, isLabelPreceeding, bothDisabledOnClick );

			if ( panelTitle != null )
				add( JComponentUtilities.createLabel( panelTitle, JLabel.CENTER, Color.black, Color.white ), BorderLayout.NORTH );

			actionStatusPanel = new JPanel();
			actionStatusPanel.setLayout( new GridLayout( 2, 1 ) );

			actionStatusLabel = new JLabel( " ", JLabel.CENTER );
			actionStatusPanel.add( actionStatusLabel );
			actionStatusPanel.add( new JLabel( " ", JLabel.CENTER ) );

			add( actionStatusPanel, BorderLayout.SOUTH );
		}

		public void setStatusMessage( int displayState, String s )
		{	actionStatusLabel.setText( s );
		}

		protected void actionCancelled()
		{
		}

		public void requestFocus()
		{
		}
	}

	/**
	 * An internal class which creates a panel which manages items.
	 * This is done because most of the item management displays
	 * are replicated.  Note that a lot of this code was borrowed
	 * directly from the ActionVerifyPanel class in the utilities
	 * package for Spellcast.
	 */

	protected abstract class ItemManagePanel extends JPanel
	{
		protected JList elementList;
		private VerifyButtonPanel buttonPanel;

		public ItemManagePanel( String title, String confirmedText, String cancelledText, LockableListModel elements )
		{
			elementList = new JList( elements );
			elementList.setSelectionMode( ListSelectionModel.MULTIPLE_INTERVAL_SELECTION );
			elementList.setPrototypeCellValue( "ABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890@#$%^&*" );
			elementList.setVisibleRowCount( 8 );

			JPanel centerPanel = new JPanel();
			centerPanel.setLayout( new BorderLayout() );

			centerPanel.add( JComponentUtilities.createLabel( title, JLabel.CENTER,
				Color.black, Color.white ), BorderLayout.NORTH );
			centerPanel.add( new JScrollPane( elementList, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
				JScrollPane.HORIZONTAL_SCROLLBAR_NEVER ), BorderLayout.CENTER );

			buttonPanel = new VerifyButtonPanel( confirmedText, cancelledText );

			JPanel actualPanel = new JPanel();
			actualPanel.setLayout( new BorderLayout( 20, 10 ) );
			actualPanel.add( centerPanel, BorderLayout.CENTER );
			actualPanel.add( buttonPanel, BorderLayout.EAST );

			setLayout( new CardLayout( 10, 10 ) );
			add( actualPanel, "" );
		}

		protected abstract void actionConfirmed();
		protected abstract void actionCancelled();

		public void setEnabled( boolean isEnabled )
		{
			elementList.setEnabled( isEnabled );
			buttonPanel.setEnabled( isEnabled );
		}

		private class VerifyButtonPanel extends JPanel
		{
			private JButton confirmedButton;
			private JButton cancelledButton;

			public VerifyButtonPanel( String confirmedText, String cancelledText )
			{
				setLayout( new BoxLayout( this, BoxLayout.Y_AXIS ) );

				// add the "confirmed" button
				confirmedButton = new JButton( confirmedText );
				confirmedButton.addActionListener(
					new ActionListener() {
						public void actionPerformed( ActionEvent e ) {
							actionConfirmed();
						}
					} );

				addButton( confirmedButton );
				add( Box.createVerticalStrut( 4 ) );

				// add the "cancelled" button
				cancelledButton = new JButton( cancelledText );
				cancelledButton.addActionListener(
					new ActionListener() {
						public void actionPerformed( ActionEvent e ) {
							actionCancelled();
						}
					} );
				addButton( cancelledButton );

				JComponentUtilities.setComponentSize( this, 120, 100 );
			}

			private void addButton( JButton buttonToAdd )
			{
				JPanel container = new JPanel();
				container.setLayout( new GridLayout() );
				container.add( buttonToAdd );
				container.setMaximumSize( new Dimension( Integer.MAX_VALUE, 24 ) );
				add( container );
			}

			public void setEnabled( boolean isEnabled )
			{
				confirmedButton.setEnabled( isEnabled );
				cancelledButton.setEnabled( isEnabled );
			}
		}
	}

	/**
	 * In order to keep the user interface from freezing (or at least
	 * appearing to freeze), this internal class is used to process
	 * the request for viewing a character sheet.
	 */

	protected class DisplayFrameListener implements ActionListener
	{
		private Constructor creator;
		private KoLmafia [] parameters;
		protected KoLFrame lastCreatedFrame;

		public DisplayFrameListener( Class frameClass )
		{
			try
			{
				Class [] fields = new Class[1];
				fields[0] = KoLmafia.class;
				this.creator = frameClass.getConstructor( fields );
			}
			catch ( NoSuchMethodException e )
			{
				// If this happens, this is the programmer's
				// fault for not noticing that the frame was
				// more complex than is allowed by this
				// displayer.  The creator stays null, which
				// is harmless, so do nothing for now.
			}

			this.parameters = new KoLmafia[1];
			parameters[0] = KoLFrame.this.client;
		}

		public void actionPerformed( ActionEvent e )
		{	(new DisplayFrameThread()).start();
		}

		protected class DisplayFrameThread extends Thread
		{
			public DisplayFrameThread()
			{
				super( "DisplayFrame-Thread" );
				setDaemon( true );
			}

			public void run()
			{
				try
				{
					if ( creator != null )
					{
						lastCreatedFrame = (KoLFrame) creator.newInstance( parameters );
						lastCreatedFrame.pack();
						lastCreatedFrame.setLocation( KoLFrame.this.getLocation() );
						lastCreatedFrame.setVisible( true );
						lastCreatedFrame.requestFocus();
						lastCreatedFrame.setEnabled( isEnabled() );
						existingFrames.add( lastCreatedFrame );
					}
					else
					{
						updateDisplay( ERROR_STATE, "Frame could not be loaded." );
						return;
					}
				}
				catch ( Exception e )
				{
					// If this happens, update the display to indicate
					// that it failed to happen (eventhough technically,
					// this should never have happened)

					updateDisplay( ERROR_STATE, "Frame could not be loaded." );
					e.printStackTrace( client.getLogStream() );
					return;
				}
			}
		}
	}

	/**
	 * In order to keep the user interface from freezing (or at least
	 * appearing to freeze), this internal class is used to process
	 * the request for viewing the status pane.
	 */

	private class ViewStatusPaneListener extends DisplayFrameListener
	{
		public ViewStatusPaneListener()
		{	super( CharsheetFrame.class );
		}

		public void actionPerformed( ActionEvent e )
		{	(new ViewStatusPaneThread()).start();
		}

		private class ViewStatusPaneThread extends DisplayFrameThread
		{
			public void run()
			{
				if ( statusPane != null )
				{
					statusPane.setVisible( true );
					statusPane.requestFocus();
					statusPane.setEnabled( isEnabled );

					if ( isEnabled )
						statusPane.refreshStatus();
				}
				else
				{
					super.run();
					statusPane = (CharsheetFrame) lastCreatedFrame;
				}
			}
		}
	}

	/**
	 * In order to keep the user interface from freezing (or at least
	 * appearing to freeze), this internal class is used to process
	 * the request for viewing the gear changer.
	 */

	private class ViewGearChangerListener extends DisplayFrameListener
	{
		public ViewGearChangerListener()
		{	super( GearChangeFrame.class );
		}

		public void actionPerformed( ActionEvent e )
		{	(new ViewGearChangerThread()).start();
		}

		private class ViewGearChangerThread extends DisplayFrameThread
		{
			public void run()
			{
				if ( gearChanger != null )
				{
					gearChanger.setVisible( true );
					gearChanger.requestFocus();
					gearChanger.setEnabled( isEnabled );
				}
				else
				{
					super.run();
					gearChanger = (GearChangeFrame) lastCreatedFrame;
				}
			}
		}
	}

	/**
	 * In order to keep the user interface from freezing (or at least
	 * appearing to freeze), this internal class is used to process
	 * the request for viewing the item manager.
	 */

	private class ViewItemManagerListener extends DisplayFrameListener
	{
		public ViewItemManagerListener()
		{	super( ItemManageFrame.class );
		}

		public void actionPerformed( ActionEvent e )
		{	(new ViewItemManagerThread()).start();
		}

		private class ViewItemManagerThread extends DisplayFrameThread
		{
			public void run()
			{
				if ( itemManager != null )
				{
					itemManager.setVisible( true );
					itemManager.requestFocus();
					itemManager.setEnabled( isEnabled );
				}
				else
				{
					super.run();
					itemManager = (ItemManageFrame) lastCreatedFrame;
				}
			}
		}
	}

	/**
	 * In order to keep the user interface from freezing (or at least
	 * appearing to freeze), this internal class is used to process
	 * the request for viewing the chat window.
	 */

	private class ViewChatListener implements ActionListener
	{
		public void actionPerformed( ActionEvent e )
		{	(new ViewChatThread()).start();
		}

		private class ViewChatThread extends Thread
		{
			public ViewChatThread()
			{
				super( "Chat-Display-Thread" );
				setDaemon( true );
			}

			public void run()
			{
				if ( client.getMessenger() == null )
				{
					client.initializeChat();
					kolchat = client.getMessenger();
				}

				updateDisplay( ENABLED_STATE, "Chat successfully loaded." );
			}
		}
	}

	/**
	 * In order to keep the user interface from freezing (or at least
	 * appearing to freeze), this internal class is used to process
	 * the request for viewing the item manager.
	 */

	private class DisplayMailListener extends DisplayFrameListener
	{
		public DisplayMailListener()
		{	super( MailboxFrame.class );
		}

		public void actionPerformed( ActionEvent e )
		{	(new DisplayMailThread()).start();
		}

		private class DisplayMailThread extends DisplayFrameThread
		{
			public void run()
			{
				if ( mailboxDisplay != null )
				{
					mailboxDisplay.setVisible( true );
					mailboxDisplay.requestFocus();
					mailboxDisplay.setEnabled( isEnabled );

					if ( isEnabled )
						mailboxDisplay.refreshMailbox();
				}
				else
				{
					super.run();
					mailboxDisplay = (MailboxFrame) lastCreatedFrame;
					mailboxDisplay.stateChanged( null );
				}
			}
		}
	}
}