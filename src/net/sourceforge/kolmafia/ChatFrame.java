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
import javax.swing.JOptionPane;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JButton;
import javax.swing.JTextField;
import javax.swing.JEditorPane;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JFileChooser;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JSeparator;
import javax.swing.ButtonGroup;
import javax.swing.JRadioButtonMenuItem;

// other imports
import java.util.Calendar;

/**
 * An extension of <code>KoLFrame</code> used to display the current
 * chat contents.  This updates according to the refresh rate specified
 * in <code>ChatRequest</code> and allows users to both read what has
 * already happened in chat and to log their conversations.
 */

public class ChatFrame extends KoLFrame
{
	protected ButtonGroup clickGroup;
	protected JRadioButtonMenuItem [] clickOptions;

	private JMenuBar menuBar;
	private ChatPanel mainPanel;
	protected KoLMessenger messenger;

	/**
	 * Constructs a new <code>ChatFrame</code>.
	 * @param	client	The client associated with the chat session
	 * @param	messenger	The messenger associated with the chat session
	 */

	public ChatFrame( KoLmafia client, KoLMessenger messenger )
	{	this( client, messenger, null );
	}

	/**
	 * Constructs a new <code>ChatFrame</code> which is intended to be
	 * used for instant messaging to the specified contact.
	 */

	public ChatFrame( KoLmafia client, KoLMessenger messenger, String associatedContact )
	{
		super( "", client );

		this.messenger = messenger;
		initialize( associatedContact );

		if ( client != null && mainPanel != null )
			addWindowListener( new CloseChatListener( associatedContact ) );
		else
			addWindowListener( new ExitChatListener() );

		addMenuBar( associatedContact );

		// Set the default size so that it doesn't appear super-small
		// when it's first constructed

		setSize( new Dimension( 400, 300 ) );
		if ( mainPanel != null && associatedContact != null && !associatedContact.startsWith( "/" ) )
			setTitle( "KoLmafia NSIPM: " + client.getLoginName() + " / " + associatedContact );
	}

	/**
	 * Utility method used to add a menu bar to the <code>ChatFrame</code>.
	 * The menu bar contains the general license information associated with
	 * <code>KoLmafia</code> and the ability to save the chat to a log.
	 */

	private void addMenuBar( String associatedContact )
	{
		menuBar = new JMenuBar();
		this.setJMenuBar( menuBar );

		JMenu fileMenu = new JMenu( "File" );
		fileMenu.setMnemonic( KeyEvent.VK_F );
		menuBar.add( fileMenu );

		JMenuItem loggerItem = new JMenuItem( "Log Chat", KeyEvent.VK_L );
		loggerItem.addActionListener( new LogChatListener() );
		fileMenu.add( loggerItem );

		JMenuItem clearItem = new JMenuItem( "Clear Chat", KeyEvent.VK_C );
		clearItem.addActionListener( new ClearChatBufferListener() );
		fileMenu.add( clearItem );

		JMenuItem highItem = new JMenuItem( "Highlight!", KeyEvent.VK_H );
		highItem.addActionListener( new HighlightChatListener() );
		fileMenu.add( highItem );

		addConfigureMenu( menuBar );

		clickGroup = new ButtonGroup();
		clickOptions = new JRadioButtonMenuItem[4];
		clickOptions[0] = new JRadioButtonMenuItem( "Open blue message", false );
		clickOptions[1] = new JRadioButtonMenuItem( "Open green message", false );
		clickOptions[2] = new JRadioButtonMenuItem( "Open purple message", false );
		clickOptions[3] = new JRadioButtonMenuItem( "Open player profile", false );

		int clickSelect = client == null ? 0 : client.getSettings().getProperty( "nameClickOpens" ) == null ? 0 :
			Integer.parseInt( client.getSettings().getProperty( "nameClickOpens" ) );

		clickOptions[ clickSelect ].setSelected( true );

		for ( int i = 0; i < 4; ++i )
			clickGroup.add( clickOptions[i] );

		JMenu clicksMenu = new JMenu( "N-Click" );
		clicksMenu.setMnemonic( KeyEvent.VK_N );
		menuBar.add( clicksMenu );

		for ( int i = 0; i < clickOptions.length; ++i )
			clicksMenu.add( clickOptions[i] );

		addHelpMenu( menuBar );
	}

	/**
	 * Utility method called to initialize the frame.  This
	 * method should be overridden, should a different means
	 * of initializing the content of the frame be needed.
	 */

	protected void initialize( String associatedContact )
	{
		getContentPane().setLayout( new CardLayout( 5, 5 ) );
		this.mainPanel = new ChatPanel( associatedContact );
		getContentPane().add( mainPanel, "" );
	}

	/**
	 * Utility method for creating a single panel containing the
	 * chat display and the entry area.  Note that calling this
	 * method changes the <code>JEditorPane</code> returned by
	 * calling the <code>getChatDisplay()</code> method.
	 */

	public class ChatPanel extends JPanel
	{
		private JTextField entryField;
		private JScrollPane scrollPane;
		private JEditorPane chatDisplay;
		private String associatedContact;

		public ChatPanel( String associatedContact )
		{
			chatDisplay = new JEditorPane();
			chatDisplay.setEditable( false );
			this.associatedContact = associatedContact;

			if ( !associatedContact.startsWith( "[" ) )
				chatDisplay.addHyperlinkListener( new ChatLinkClickedListener() );

			scrollPane = new JScrollPane( chatDisplay, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
				JScrollPane.HORIZONTAL_SCROLLBAR_NEVER );

			scrollPane.setVerticalScrollBar( new ChatScrollBar() );

			JPanel entryPanel = new JPanel();
			entryField = new JTextField();
			entryField.addKeyListener( new ChatEntryListener() );

			JButton entryButton = new JButton( "chat" );
			entryButton.addActionListener( new ChatEntryListener() );
			entryPanel.setLayout( new BoxLayout( entryPanel, BoxLayout.X_AXIS ) );
			entryPanel.add( entryField, BorderLayout.CENTER );
			entryPanel.add( entryButton, BorderLayout.EAST );

			setLayout( new BorderLayout( 1, 1 ) );
			add( scrollPane, BorderLayout.CENTER );
			add( entryPanel, BorderLayout.SOUTH );
		}

		private class ChatScrollBar extends JScrollBar
		{
			private boolean autoscroll;

			public ChatScrollBar()
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

		public JEditorPane getChatDisplay()
		{	return chatDisplay;
		}

		public String getAssociatedContact()
		{	return associatedContact;
		}

		public boolean hasFocus()
		{	return entryField.hasFocus() || chatDisplay.hasFocus();
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

		private class ChatEntryListener extends KeyAdapter implements ActionListener
		{
			public void actionPerformed( ActionEvent e )
			{	submitChat();
			}

			public void keyReleased( KeyEvent e )
			{
				if ( e.getKeyCode() == KeyEvent.VK_ENTER )
					submitChat();
			}

			private void submitChat()
			{
				(new ChatEntryThread( entryField.getText().trim() )).start();
				entryField.setText( "" );
			}

			private class ChatEntryThread extends RequestThread
			{
				private String message;

				public ChatEntryThread( String message )
				{	this.message = message;
				}

				public void run()
				{	(new ChatRequest( client, associatedContact, message )).run();
				}
			}
		}
	}

	/**
	 * Returns the name of the contact associated with this frame.
	 * @return	The name of the contact associated with this frame
	 */

	public String getAssociatedContact()
	{	return mainPanel == null ? null : mainPanel.getAssociatedContact();
	}

	/**
	 * Returns whether or not the chat frame has focus.
	 */

	public boolean hasFocus()
	{	return super.hasFocus() || menuBar.isSelected() || (mainPanel != null && mainPanel.hasFocus());
	}

	/**
	 * Requests focus to be placed on this chat frame.  It should
	 * be called whenever this window is first created.
	 */

	public void requestFocus()
	{
		super.requestFocus();
		if ( mainPanel != null )
			mainPanel.requestFocus();
	}

	/**
	 * Returns the <code>JEditorPane</code> being used to display
	 * the chat contents.
	 *
	 * @return	The <code>JEditorPane</code> used to display the chat
	 */

	public JEditorPane getChatDisplay()
	{	return mainPanel == null ? null : mainPanel.getChatDisplay();
	}

	/**
	 * Returns the <code>JScrollPane</code> being used to display
	 * the chat contents.
	 *
	 * @return	The <code>JScrollPane</code> used to display the chat
	 */

	public JScrollPane getScrollPane()
	{	return mainPanel == null ? null : mainPanel.getScrollPane();
	}

	/**
	 * Action listener responsible for displaying private message
	 * window when a username is clicked, or opening the page in
	 * a browser if you're clicking something other than the username.
	 */

	private class ChatLinkClickedListener extends KoLHyperlinkAdapter
	{
		protected void handleInternalLink( String location )
		{
			if ( clickOptions[0].isSelected() )
				client.getMessenger().openInstantMessage( location );

			else if ( clickOptions[1].isSelected() )
			{
				GreenMessageFrame composer = new GreenMessageFrame( client, location );
				composer.pack();  composer.setVisible( true );
				composer.requestFocus();
			}

			else if ( clickOptions[2].isSelected() )
			{
				GiftMessageFrame composer = new GiftMessageFrame( client, location );
				composer.pack();  composer.setVisible( true );
				composer.requestFocus();
			}

			else if ( clickOptions[3].isSelected() )
			{
				ProfileFrame profile = new ProfileFrame( client, location );
				profile.pack();  profile.setVisible( true );
				profile.requestFocus();
			}
		}
	}

	/**
	 * Internal class to handle clearing the chat pane
	 * whenever the user wishes it.
	 */

	private class ClearChatBufferListener implements ActionListener
	{
		public void actionPerformed( ActionEvent e )
		{	messenger.clearChatBuffers();
		}
	}

	/**
	 * Internal class to handle logging the chat when the
	 * user wishes to log the chat.  This opens a file
	 * selector window.
	 */

	private class LogChatListener implements ActionListener
	{
		public void actionPerformed( ActionEvent e )
		{	messenger.initializeChatLogs();
		}
	}

	private class HighlightChatListener implements ActionListener
	{
		public void actionPerformed( ActionEvent e )
		{	messenger.addHighlighting();
		}
	}

	/**
	 * Internal class to handle de-initializing the chat when
	 * the window is closed.  This helps stop constantly
	 * spamming the chat server with a request when nothing
	 * is being done with the replies.
	 */

	protected final class CloseChatListener extends WindowAdapter
	{
		private String associatedContact;

		public CloseChatListener( String associatedContact )
		{	this.associatedContact = associatedContact;
		}

		public void windowClosed( WindowEvent e )
		{	(new CloseChatRequestThread()).start();
		}

		private class CloseChatRequestThread extends RequestThread
		{
			public void run()
			{
				if ( client != null && client.getMessenger() != null )
					client.getMessenger().removeChat( associatedContact );
			}
		}
	}

	/**
	 * Internal class to handle de-initializing the chat when
	 * the window is closed.  This helps stop constantly
	 * spamming the chat server with a request when nothing
	 * is being done with the replies.
	 */

	private class ExitChatListener extends WindowAdapter
	{
		public void windowClosed( WindowEvent e )
		{	(new CloseChatRequestThread()).start();
		}

		private class CloseChatRequestThread extends RequestThread
		{
			public void run()
			{
				if ( client != null )
					client.deinitializeChat();
			}
		}
	}

	/**
	 * The main method used in the event of testing the way the
	 * user interface looks.  This allows the UI to be tested
	 * without having to constantly log in and out of KoL.
	 */

	public static void main( String [] args )
	{
		KoLFrame uitest = new ChatFrame( null, null );
		uitest.setVisible( true );  uitest.requestFocus();
	}
}
