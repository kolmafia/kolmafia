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

	/**
	 * Constructs a new <code>ChatFrame</code>.
	 * @param	client	The client associated with the chat session
	 */

	public ChatFrame( KoLmafia client )
	{
		super( "KoLmafia: " + ((client == null) ? "UI Test" : client.getLoginName()) +
			" (Chat)", client );

		JEditorPane chatDisplay = new JEditorPane();
		chatDisplay.setEditable( false );
		JScrollPane scrollArea = new JScrollPane( chatDisplay, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
			JScrollPane.HORIZONTAL_SCROLLBAR_NEVER );

		JPanel entryPanel = new JPanel();
		entryField = new JTextField();
		entryField.addActionListener( new ChatEntryListener() );
		JButton entryButton = new JButton( "chat" );
		entryButton.addActionListener( new ChatEntryListener() );

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

		if ( client != null )
			client.initializeChat( chatDisplay );

		addMenuBar();
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

		JMenu menu = new JMenu("File");
		menu.setMnemonic( KeyEvent.VK_F );
		menuBar.add( menu );

		JMenuItem loggerItem = new JMenuItem( "Log Chat", KeyEvent.VK_L );
		loggerItem.addActionListener( new LogChatListener() );

		menu.add( loggerItem );

		addConfigureMenu( menuBar );
		addHelpMenu( menuBar );
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
			{	this.message = message;
			}

			public void run()
			{	(new ChatRequest( client, message )).run();
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
			int returnVal = chooser.showSaveDialog( ChatFrame.this );

			if ( returnVal == JFileChooser.APPROVE_OPTION )
				client.getChatBuffer().setActiveLogFile(
					chooser.getSelectedFile().getAbsolutePath(),
					"Loathing Chat: " + client.getLoginName() + " (" +
					Calendar.getInstance().getTime().toString() + ")" );
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
			if ( client != null )
				client.deinitializeChat();
		}
	}

	/**
	 * The main method used in the event of testing the way the
	 * user interface looks.  This allows the UI to be tested
	 * without having to constantly log in and out of KoL.
	 */

	public static void main( String [] args )
	{
		KoLFrame uitest = new ChatFrame( null );
		uitest.setSize( new Dimension( 400, 300 ) );
		uitest.setVisible( true );  uitest.requestFocus();
	}
}
