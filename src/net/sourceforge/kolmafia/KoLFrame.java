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

// containers
import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.JMenuBar;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JFileChooser;

// layout
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.BorderLayout;


// event listeners
import java.awt.event.KeyEvent;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowAdapter;

// other stuff
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import javax.swing.SwingUtilities;
import java.lang.reflect.Constructor;
import net.java.dev.spellcast.utilities.LicenseDisplay;
import net.java.dev.spellcast.utilities.ActionVerifyPanel;
import net.java.dev.spellcast.utilities.JComponentUtilities;

/**
 * An extended <code>JFrame</code> which provides all the frames in
 * KoLmafia the ability to update their displays, given some integer
 * value and the message to use for updating.
 */

public abstract class KoLFrame extends javax.swing.JFrame implements KoLConstants
{
	public static final int NOCHANGE_STATE = 0;
	public static final int ENABLED_STATE  = 1;
	public static final int DISABLED_STATE = 2;

	protected boolean isEnabled;
	protected List existingFrames;
	protected KoLmafia client;
	protected KoLPanel contentPanel;

	/**
	 * Constructs a new <code>KoLFrame</code> with the given title,
	 * to be associated with the given client.
	 */

	protected KoLFrame( String title, KoLmafia client )
	{
		super( title );

		this.client = client;
		this.isEnabled = true;
		this.existingFrames = new ArrayList();
		setDefaultCloseOperation( DISPOSE_ON_CLOSE );
	}

	/**
	 * Updates the display to reflect the given display state and
	 * to contain the given message.  Note that if there is no
	 * content panel, this method does nothing.
	 */

	public void updateDisplay( int displayState, String message )
	{
		if ( contentPanel != null && client != null )
		{
			client.getLogStream().println( message );
			contentPanel.setStatusMessage( message );

			switch ( displayState )
			{
				case ENABLED_STATE:
					setEnabled( true );
					contentPanel.clear();
					break;

				case DISABLED_STATE:
					setEnabled( false );
					break;
			}
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

	/**
	 * Utility method used to add the default <code>KoLmafia</code>
	 * scripting menu to the given menu bar.  The default menu contains
	 * the ability to load scripts.
	 */

	protected final void addScriptMenu( JMenuBar menuBar )
	{
		JMenu scriptMenu = new JMenu("Scripts");
		scriptMenu.setMnemonic( KeyEvent.VK_S );
		menuBar.add( scriptMenu );

		JMenuItem loadScriptMenuItem = new JMenuItem( "Load Script...", KeyEvent.VK_L );
		loadScriptMenuItem.addActionListener( new LoadScriptListener() );

		scriptMenu.add( loadScriptMenuItem );
	}

	/**
	 * Utility method used to add the default <code>KoLmafia</code>
	 * configuration menu to the given menu bar.  The default menu
	 * contains the ability to customize preferences (global if it
	 * is invoked before login, character-specific if after) and
	 * initialize the debugger.
	 *
	 * @param	menuBar	The <code>JMenuBar</code> to which the configuration menu will be attached
	 */

	protected final void addConfigureMenu( JMenuBar menuBar )
	{
		JMenu configureMenu = new JMenu("Configure");
		configureMenu.setMnemonic( KeyEvent.VK_C );
		menuBar.add( configureMenu );

		JMenuItem settingsItem = new JMenuItem( "Preferences", KeyEvent.VK_P );
		settingsItem.addActionListener( new DisplayFrameListener( OptionsFrame.class ) );

		configureMenu.add( settingsItem );

		JMenuItem loggerItem = new JMenuItem( "", KeyEvent.VK_D );
		loggerItem.addActionListener( new ToggleDebugListener( loggerItem ) );

		configureMenu.add( loggerItem );
	}

	private class ToggleDebugListener implements ActionListener
	{
		private JMenuItem loggerItem;

		public ToggleDebugListener( JMenuItem loggerItem )
		{
			this.loggerItem = loggerItem;
			loggerItem.setText( client == null || client.getLogStream() instanceof NullStream ?
				"Turn On Debug" : "Turn Off Debug" );
		}

		public void actionPerformed(ActionEvent e)
		{
			if ( client != null && client.getLogStream() instanceof NullStream )
			{
				client.initializeLogStream();
				loggerItem.setText( "Turn Off Debug" );
			}
			else if ( client != null )
			{
				client.deinitializeLogStream();
				loggerItem.setText( "Turn On Debug" );
			}
		}
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

		Iterator framesIterator = existingFrames.iterator();
		KoLFrame currentFrame;

		while ( framesIterator.hasNext() )
		{
			currentFrame = (KoLFrame) framesIterator.next();
			if ( currentFrame.isShowing() )
				currentFrame.setEnabled( isEnabled );
		}
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
	 * Utility method used to add the default <code>KoLmafia</code> Help
	 * menu to the given menu bar.  The default Help menu contains the
	 * copyright statement for <code>KoLmafia</code>.
	 *
	 * @param	menuBar	The <code>JMenuBar</code> to which the Help menu will be attached
	 */

	protected final void addHelpMenu( JMenuBar menuBar )
	{
		JMenu helpMenu = new JMenu("Help");
		helpMenu.setMnemonic( KeyEvent.VK_H );
		menuBar.add( helpMenu );

		JMenuItem aboutItem = new JMenuItem( "About KoLmafia..." );
		aboutItem.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent e)
			{	(new LicenseDisplay( "KoLmafia: Copyright Notice" )).requestFocus();
			}
		});

		helpMenu.add( aboutItem );
	}

	/**
	 * An internal class which allows focus to be returned to the
	 * client's active frame when auxiliary windows are closed.
	 */

	protected class ReturnFocusAdapter extends WindowAdapter
	{
		public void windowClosed( WindowEvent e )
		{
			if ( client != null && client != null )
				client.requestFocus();
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

				String filename = chooser.getSelectedFile().getAbsolutePath();

				try
				{
					if ( client != null && returnVal == JFileChooser.APPROVE_OPTION )
						(new KoLmafiaCLI( client, filename )).listenForCommands();

					if ( client.permitsContinue() )
						updateDisplay( KoLFrame.ENABLED_STATE, "Script completed successfully." );
				}
				catch ( Exception e )
				{
					// Here, notify the display that the script
					// file specified could not be loaded

					updateDisplay( KoLFrame.ENABLED_STATE, "Script file <" + filename + "> could not be found." );
					return;
				}
			}
		}
	}

	/**
	 * An internal class used as the basis for content panels.  This
	 * class builds upon the <code>ActionVerifyPanel</code> by adding
	 * <code>setStatusMessage()</code> and <code>clear()</code> methods
	 * as well as a method which allows GUIs to make sure that all
	 * status-message updating occurs within the AWT thread.
	 */

	protected abstract class KoLPanel extends ActionVerifyPanel
	{
		protected KoLPanel( String confirmedText, String cancelledText )
		{
			super( confirmedText, cancelledText );
		}

		protected KoLPanel( String confirmedText, String cancelledText, Dimension labelSize, Dimension fieldSize )
		{
			super( confirmedText, cancelledText, labelSize, fieldSize );
		}

		public abstract void clear();
		public abstract void setStatusMessage( String s );

		protected final class StatusMessageChanger implements Runnable
		{
			private String status;

			public StatusMessageChanger( String status )
			{	this.status = status;
			}

			public void run()
			{
				if ( !SwingUtilities.isEventDispatchThread() )
				{
					SwingUtilities.invokeLater( this );
					return;
				}

				setStatusMessage( status );
			}
		}
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

		public void setStatusMessage( String s )
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
			super( confirmButton, cancelButton, left, right );
			this.panelTitle = panelTitle;

			actionStatusPanel = new JPanel();
			actionStatusPanel.setLayout( new GridLayout( 2, 1 ) );

			actionStatusLabel = new JLabel( " ", JLabel.CENTER );
			actionStatusPanel.add( actionStatusLabel );
			actionStatusPanel.add( new JLabel( " ", JLabel.CENTER ) );
		}

		protected void setContent( VerifiableElement [] elements )
		{	setContent( elements, true );
		}

		protected void setContent( VerifiableElement [] elements, boolean isLabelPreceeding )
		{
			super.setContent( elements, isLabelPreceeding );

			if ( panelTitle != null )
				add( JComponentUtilities.createLabel( panelTitle, JLabel.CENTER,
						Color.black, Color.white ), BorderLayout.NORTH );

			add( actionStatusPanel, BorderLayout.SOUTH );
			clear();
		}

		public void setStatusMessage( String s )
		{	actionStatusLabel.setText( s );
		}

		protected void actionCancelled()
		{	clear();
		}

		public void requestFocus()
		{
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
						KoLFrame.this.setEnabled( false );

						lastCreatedFrame = (KoLFrame) creator.newInstance( parameters );
						lastCreatedFrame.pack();
						lastCreatedFrame.setLocation( KoLFrame.this.getLocation() );
						lastCreatedFrame.setVisible( true );
						lastCreatedFrame.requestFocus();
						lastCreatedFrame.setEnabled( isEnabled );
						existingFrames.add( lastCreatedFrame );

						updateDisplay( NOCHANGE_STATE, " " );
					}
					else
					{
						updateDisplay( ENABLED_STATE, "Frame could not be loaded." );
						return;
					}
				}
				catch ( Exception e )
				{
					// If this happens, update the display to indicate
					// that it failed to happen (eventhough technically,
					// this should never have happened)

					updateDisplay( ENABLED_STATE, "Frame could not be loaded." );
					return;
				}
			}
		}
	}
}