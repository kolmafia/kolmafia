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
import java.awt.event.WindowEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

// containers
import javax.swing.JPanel;
import javax.swing.JButton;
import javax.swing.JTextField;
import javax.swing.JEditorPane;
import javax.swing.JScrollPane;
import javax.swing.JFileChooser;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;

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
	private JTextField entryField;
	private JEditorPane chatDisplay;
	private KoLMessenger messenger;
	private String associatedContact;

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
		this.associatedContact = associatedContact;

		chatDisplay = new JEditorPane();
		chatDisplay.setEditable( false );
		JScrollPane scrollArea = new JScrollPane( chatDisplay, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
			JScrollPane.HORIZONTAL_SCROLLBAR_NEVER );

		JPanel entryPanel = new JPanel();
		entryField = new JTextField();
		JButton entryButton = new JButton( "chat" );
		rootPane.setDefaultButton( entryButton );

		entryPanel.setLayout( new BoxLayout( entryPanel, BoxLayout.X_AXIS ) );
		entryPanel.add( entryField, BorderLayout.CENTER );
		entryPanel.add( entryButton, BorderLayout.EAST );

		JPanel mainPanel = new JPanel();
		mainPanel.setLayout( new BorderLayout( 1, 1 ) );
		mainPanel.add( scrollArea, BorderLayout.CENTER );
		mainPanel.add( entryPanel, BorderLayout.SOUTH );

		getContentPane().setLayout( new CardLayout( 5, 5 ) );
		getContentPane().add( mainPanel, "" );
		addWindowListener( new CloseChatListener() );
		entryButton.addActionListener( new ChatEntryListener() );

		addMenuBar();

		// Set the default size so that it doesn't appear super-small
		// when it's first constructed

		setSize( new Dimension( 400, 300 ) );

		if ( associatedContact != null && !associatedContact.startsWith( "/" ) )
			setTitle( "KoLmafia NSIPM: " + client.getLoginName() + " / " + associatedContact );
	}

	/**
	 * Utility method used to add a menu bar to the <code>AdventureFrame</code>.
	 * The menu bar contains the general license information associated with
	 * <code>KoLmafia</code> and the ability to save the chat to a log.
	 */

	private void addMenuBar()
	{
		JMenuBar menuBar = new JMenuBar();
		this.setJMenuBar( menuBar );

		JMenu fileMenu = new JMenu("File");
		fileMenu.setMnemonic( KeyEvent.VK_F );
		menuBar.add( fileMenu );

		JMenuItem loggerItem = new JMenuItem( "Log Chat", KeyEvent.VK_L );
		loggerItem.addActionListener( new LogChatListener() );
		fileMenu.add( loggerItem );

		JMenuItem clearItem = new JMenuItem( "Clear Chat", KeyEvent.VK_C );
		clearItem.addActionListener( new ClearChatBufferListener() );
		fileMenu.add( clearItem );

		if ( !associatedContact.startsWith( "/" ) )
		{
			JMenu peopleMenu = new JMenu("People");
			peopleMenu.setMnemonic( KeyEvent.VK_P );
			menuBar.add( peopleMenu );

			JMenuItem addFriendItem = new JMenuItem( "Add Friend", KeyEvent.VK_A );
			addFriendItem.addActionListener( new AddFriendListener() );
			peopleMenu.add( addFriendItem );

			JMenuItem ignoreFriendItem = new JMenuItem( "Ignore / Block", KeyEvent.VK_I );
			ignoreFriendItem.addActionListener( new IgnoreFriendListener() );
			peopleMenu.add( ignoreFriendItem );
		}

		addHelpMenu( menuBar );
	}

	/**
	 * Returns the name of the contact associated with this frame.
	 * @return	The name of the contact associated with this frame
	 */

	public String getAssociatedContact()
	{	return associatedContact;
	}

	/**
	 * Requests focus to be placed on this chat frame.  It should
	 * be called whenever this window is first created.
	 */

	public void requestFocus()
	{
		super.requestFocus();
		entryField.requestFocus();
	}

	/**
	 * Returns the <code>JEditorPane</code> being used to display
	 * the chat contents.
	 *
	 * @return	The <code>JEditorPane</code> used to display the chat
	 */

	public JEditorPane getChatDisplay()
	{	return chatDisplay;
	}

	/**
	 * Action listener responsible for adding the friend to the
	 * contact list.
	 */

	private class AddFriendListener implements ActionListener
	{
		public void actionPerformed( ActionEvent e )
		{	(new AddFriendThread()).start();
		}

		private class AddFriendThread extends Thread
		{
			public AddFriendThread()
			{	setDaemon( true );
			}

			public void run()
			{	(new ChatRequest( client, associatedContact, "/friend" )).run();
			}
		}
	}

	/**
	 * Action listener responsible for placing someone on the
	 * ignore (baleet) list.
	 */

	private class IgnoreFriendListener implements ActionListener
	{
		public void actionPerformed( ActionEvent e )
		{	(new IgnoreFriendThread()).start();
		}

		private class IgnoreFriendThread extends Thread
		{
			public IgnoreFriendThread()
			{	setDaemon( true );
			}

			public void run()
			{	(new ChatRequest( client, associatedContact, "/ignore" )).run();
			}
		}
	}


	/**
	 * An action listener responsible for sending the text
	 * contained within the entry panel to the KoL chat
	 * server for processing.  This listener spawns a new
	 * request to the server which then handles everything
	 * that's needed.
	 */

	private class ChatEntryListener implements ActionListener
	{
		public void actionPerformed( ActionEvent e )
		{
			(new ChatEntryThread( entryField.getText().trim() )).start();
			entryField.setText( "" );
		}

		private class ChatEntryThread extends Thread
		{
			private String message;

			public ChatEntryThread( String message )
			{
				this.message = message;
				setDaemon( true );
			}

			public void run()
			{	(new ChatRequest( client, associatedContact, message )).run();
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
		{	(new ClearChatBufferThread()).start();
		}

		private class ClearChatBufferThread extends Thread
		{
			public ClearChatBufferThread()
			{	setDaemon( true );
			}

			public void run()
			{
				if ( client != null )
					messenger.clearChatBuffer( associatedContact );
			}
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
		{
			JFileChooser chooser = new JFileChooser();
			chooser.setFileFilter( new HTMLFileFilter() );
			int returnVal = chooser.showSaveDialog( ChatFrame.this );
			String filename = chooser.getSelectedFile().getAbsolutePath();
			if ( !filename.endsWith( ".htm" ) && !filename.endsWith( ".html" ) )
				filename += ".html";

			if ( client != null && returnVal == JFileChooser.APPROVE_OPTION )
				messenger.getChatBuffer( associatedContact ).setActiveLogFile( filename,
					"Loathing Chat: " + client.getLoginName() + " (" +
					Calendar.getInstance().getTime().toString() + ")" );
		}

		/**
		 * Internal file descriptor to make sure files are
		 * only saved to HTML format.
		 */

		private class HTMLFileFilter extends javax.swing.filechooser.FileFilter
		{
			public boolean accept( java.io.File f )
			{	return f.getPath().endsWith( ".htm" ) || f.getPath().endsWith( ".html" );
			}

			public String getDescription()
			{	return "Hypertext Documents";
			}
		}
	}

	/**
	 * Internal class to handle de-initializing the chat when
	 * the window is closed.  This helps stop constantly
	 * spamming the chat server with a request when nothing
	 * is being done with the replies.
	 */

	private class CloseChatListener extends WindowAdapter
	{
		public void windowClosed( WindowEvent e )
		{
			if ( client != null && client.getMessenger() != null )
				client.getMessenger().removeChat( associatedContact );
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
