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
import javax.swing.BoxLayout;

// event listeners
import java.awt.event.KeyEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import javax.swing.SwingUtilities;

// containers
import javax.swing.JPanel;
import javax.swing.JButton;
import javax.swing.JTextField;
import javax.swing.JEditorPane;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JOptionPane;

/**
 * The main class for the <code>KoLmafia</code> package.  This
 * class encapsulates most of the data relevant to any given
 * session of <code>Kingdom of Loathing</code> and currently
 * functions as the blackboard in the architecture.  When data
 * listeners are implemented, it will continue to manage most
 * of the interactions.
 */

public class KoLmafiaGUI extends KoLmafia
{
	private KoLFrame activeFrame;
	private LimitedSizeChatBuffer buffer;
	private CommandDisplayFrame graphicalCLI;

	/**
	 * The main method.  Currently, it instantiates a single instance
	 * of the <code>KoLmafia</code> client after setting the default
	 * look and feel of all <code>JFrame</code> objects to decorated.
	 */

	public static void main( String [] args )
	{
		javax.swing.JFrame.setDefaultLookAndFeelDecorated( true );
    	KoLmafiaGUI session = new KoLmafiaGUI();

		String autoLoginSetting =  session.settings.getProperty( "autoLogin" );
		if ( autoLoginSetting != null )
		{
			String password = session.getSaveState( autoLoginSetting );
			if ( password != null )
				(new LoginRequest( session, autoLoginSetting, session.getSaveState( autoLoginSetting ), false, false, false )).run();
		}
	}

	/**
	 * Updates the currently active display in the <code>KoLmafia</code>
	 * session.
	 */

	public void updateDisplay( int state, String message )
	{
		if ( activeFrame != null )
		{
			activeFrame.updateDisplay( state, message );
			if ( isBuffBotActive() )
				buffBotHome.updateStatus( message );
		}

		if ( graphicalCLI != null )
		{
			StringBuffer colorBuffer = new StringBuffer();
			if ( state == ERROR_STATE )
				colorBuffer.append( "<font color=red>" );
			else
				colorBuffer.append( "<font color=black>" );

			colorBuffer.append( message );
			colorBuffer.append( "</font><br>" );
			colorBuffer.append( System.getProperty( "line.separator" ) );
			buffer.append( colorBuffer.toString() );
		}
	}

	public void requestFocus()
	{
		if ( activeFrame != null )
			activeFrame.requestFocus();
	}

	/**
	 * Initializes the <code>KoLmafia</code> session.  Called after
	 * the login has been confirmed to notify the client that the
	 * login was successful, the user-specific settings should be
	 * loaded, and the user can begin adventuring.
	 */

	public void initialize( String loginname, String sessionID, boolean getBreakfast, boolean isQuickLogin )
	{
		super.initialize( loginname, sessionID, getBreakfast, isQuickLogin );

		if ( loginRequest != null )
		{
			updateDisplay( ENABLED_STATE, "Session timed-in." );
			return;
		}

		if ( !isLoggingIn )
		{
			KoLFrame previousActiveFrame = activeFrame;

			activeFrame = new AdventureFrame( this, tally );
			activeFrame.pack();

			if ( settings.getProperty( activeFrame.getFrameName() ) == null )
				activeFrame.setLocationRelativeTo( previousActiveFrame );

			activeFrame.setVisible( true );
			previousActiveFrame.setVisible( false );

			activeFrame.requestFocus();
			activeFrame.updateDisplay( ENABLED_STATE, MoonPhaseDatabase.getMoonEffect() );
			previousActiveFrame.dispose();
		}
	}

	/**
	 * Deinitializes the <code>KoLmafia</code> session.  Called after
	 * the user has logged out.
	 */

	public void deinitialize()
	{
		super.deinitialize();

		if ( activeFrame != null )
			return;

		activeFrame = new LoginFrame( this, saveStateNames );
		activeFrame.pack();

		if ( settings.getProperty( activeFrame.getFrameName() ) == null )
			activeFrame.setLocationRelativeTo( null );

		activeFrame.setVisible( true );
		activeFrame.requestFocus();

		if ( graphicalCLI != null )
			graphicalCLI.dispose();

		graphicalCLI = null;
	}

	public void pwnClanOtori()
	{
		if ( JOptionPane.NO_OPTION == JOptionPane.showConfirmDialog( null,
			"This attempt to pwn Clan Otori will cost you 13 meat.\nAre you sure you want to continue?",
			"YES!  This does use meat!", JOptionPane.YES_NO_OPTION ) )
				return;

		super.pwnClanOtori();
	}

	/**
	 * Makes a request to the hermit, looking for the given number of
	 * items.  This method should prompt the user to determine which
	 * item to retrieve the hermit.
	 */

	protected void makeHermitRequest()
	{
		Object selectedValue = JOptionPane.showInputDialog(
			null, "I want this from the hermit...", "Mugging Hermit for...", JOptionPane.INFORMATION_MESSAGE, null,
			hermitItemNames, hermitItemNames[0] );

		if ( selectedValue == null )
			return;

		int selected = -1;
		for ( int i = 0; selected == -1 && i < hermitItemNames.length; ++i )
			if ( selectedValue.equals( hermitItemNames[i] ) )
				selected = hermitItemNumbers[i];

		try
		{
			int tradeCount = df.parse( JOptionPane.showInputDialog(
				null, "How many " + selectedValue + " to get?", "I want this many!", JOptionPane.INFORMATION_MESSAGE ) ).intValue();

			(new HermitRequest( this, selected, tradeCount )).run();
		}
		catch ( Exception e )
		{
		}
	}

	/**
	 * Makes a request to the trapper, looking for the given number of
	 * items.  This method should prompt the user to determine which
	 * item to retrieve the trapper.
	 */

	protected void makeTrapperRequest()
	{
		Object selectedValue = JOptionPane.showInputDialog(
			null, "I want this from the trapper...", "1337ing Trapper for...", JOptionPane.INFORMATION_MESSAGE, null,
			trapperItemNames, trapperItemNames[0] );

		if ( selectedValue == null )
			return;

		for ( int i = 0; i < trapperItemNames.length; ++i )
			if ( selectedValue.equals( trapperItemNames[i] ) )
			{
				(new TrapperRequest( this, trapperItemNumbers[i] )).run();
				return;
			}
	}

	/**
	 * Makes a request to the hunter, looking for the given number of
	 * items.  This method should prompt the user to determine which
	 * item to retrieve the hunter.
	 */

	protected void makeHunterRequest()
	{
		if ( hunterItems.isEmpty() )
			(new BountyHunterRequest( this )).run();

		Object [] hunterItemArray = hunterItems.toArray();

		String selectedValue = (String) JOptionPane.showInputDialog(
			null, "I want to sell this to the hunter...", "The Quilted Thicker Picker Upper!", JOptionPane.INFORMATION_MESSAGE, null,
			hunterItemArray, hunterItemArray[0] );

		if ( selectedValue != null )
			(new BountyHunterRequest( this, TradeableItemDatabase.getItemID( selectedValue ) )).run();

	}

	/**
	 * Confirms whether or not the user wants to make a drunken
	 * request.  This should be called before doing requests when
	 * the user is in an inebrieted state.
	 *
	 * @return	<code>true</code> if the user wishes to adventure drunk
	 */

	protected boolean confirmDrunkenRequest()
	{
		return JOptionPane.YES_OPTION == JOptionPane.showConfirmDialog( null,
			" You see flying penguins and a dancing hermit!\nThe mafia has stolen your shoes!\n(KoLmafia thinks you're too drunk!)\nContinue adventuring anyway?\n",
			"You're not drunk!?", JOptionPane.YES_NO_OPTION );
	}

	public void setVisible( boolean isVisible )
	{	activeFrame.setVisible( isVisible );
	}

	public boolean isVisible()
	{	return activeFrame.isVisible();
	}

	public void deinitializeBuffBot()
	{
		super.deinitializeBuffBot();
		activeFrame.setVisible( true );
	}

	public void initializeGCLI()
	{
		buffer = new LimitedSizeChatBuffer( "KoLmafia: Graphical CLI" );
		graphicalCLI = new CommandDisplayFrame( this );
		graphicalCLI.setVisible( true );  graphicalCLI.requestFocus();
	}

	private class CommandDisplayFrame extends KoLFrame
	{
		public CommandDisplayFrame( KoLmafia client )
		{
			super( "KoLmafia: Graphical CLI", client );
			getContentPane().setLayout( new BorderLayout( 0, 0 ) );
			getContentPane().add( new CommandDisplayPanel() );
			setSize( new Dimension( 400, 300 ) );
		}
	}

	private class CommandDisplayPanel extends JPanel
	{
		private JTextField entryField;
		private JScrollPane scrollPane;
		private JEditorPane outputDisplay;

		public CommandDisplayPanel()
		{
			outputDisplay = new JEditorPane();
			outputDisplay.setEditable( false );

			buffer = new LimitedSizeChatBuffer( "KoLmafia Graphical CLI" );
			buffer.setChatDisplay( outputDisplay );

			scrollPane = new JScrollPane( outputDisplay, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
				JScrollPane.HORIZONTAL_SCROLLBAR_NEVER );

			scrollPane.setVerticalScrollBar( new CommandScrollBar() );
			buffer.setScrollPane( scrollPane );

			JPanel entryPanel = new JPanel();
			entryField = new JTextField();
			entryField.addKeyListener( new CommandEntryListener() );

			JButton entryButton = new JButton( "exec" );
			entryButton.addActionListener( new CommandEntryListener() );
			entryPanel.setLayout( new BoxLayout( entryPanel, BoxLayout.X_AXIS ) );
			entryPanel.add( entryField, BorderLayout.CENTER );
			entryPanel.add( entryButton, BorderLayout.EAST );

			setLayout( new BorderLayout( 1, 1 ) );
			add( scrollPane, BorderLayout.CENTER );
			add( entryPanel, BorderLayout.SOUTH );
		}

		private class CommandScrollBar extends JScrollBar
		{
			private boolean autoscroll;

			public CommandScrollBar()
			{
				super( VERTICAL );
				this.autoscroll = true;
			}

			public void setValue( int value )
			{
				if ( getValueIsAdjusting() )
					autoscroll = getMaximum() - getVisibleAmount() - getValue() < 10;

				if ( autoscroll || getValueIsAdjusting() )
					super.setValue( value );
			}

			protected void fireAdjustmentValueChanged( int id, int type, int value )
			{
				if ( autoscroll || getValueIsAdjusting() )
					super.fireAdjustmentValueChanged( id, type, value );
			}

			public void setValues( int newValue, int newExtent, int newMin, int newMax )
			{
				if ( autoscroll || getValueIsAdjusting() )
					super.setValues( newValue, newExtent, newMin, newMax );
				else
					super.setValues( getValue(), newExtent, newMin, newMax );
			}
		}

		public JScrollPane getScrollPane()
		{	return scrollPane;
		}

		public JEditorPane getOutputDisplay()
		{	return outputDisplay;
		}

		public boolean hasFocus()
		{	return entryField.hasFocus() || outputDisplay.hasFocus();
		}

		public void requestFocus()
		{	entryField.requestFocus();
		}

		/**
		 * An action listener responsible for sending the text
		 * contained within the entry panel to the KoL chat
		 * server for processing.  This listener spawns a new
		 * request to the server which then handles everything
		 * that's needed.
		 */

		private class CommandEntryListener extends KeyAdapter implements ActionListener
		{
			public void actionPerformed( ActionEvent e )
			{	submitCommand();
			}

			public void keyReleased( KeyEvent e )
			{
				if ( e.getKeyCode() == KeyEvent.VK_ENTER )
					submitCommand();
			}

			private void submitCommand()
			{
				(new CommandEntryThread( entryField.getText().trim() )).start();
				entryField.setText( "" );
			}

			private class CommandEntryThread extends RequestThread
			{
				private String command;

				public CommandEntryThread( String command )
				{	this.command = command;
				}

				public void run()
				{
					try
					{
						buffer.append( "<font color=olive>&nbsp;&gt;&nbsp;" + command + "</font><br>" );

						if ( command.toLowerCase().equals( "login" ) )
							buffer.append( "<font color=red>This command is not available in the GCLI</font><br>" );
						else
							(new KoLmafiaCLI( KoLmafiaGUI.this, (String) null )).executeLine( command );

						buffer.append( "<br>" );
					}
					catch ( Exception e )
					{
					}
				}
			}
		}
	}
}
