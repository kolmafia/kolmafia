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
import javax.swing.JToolBar;
import javax.swing.JOptionPane;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JButton;
import javax.swing.JTextField;
import javax.swing.JEditorPane;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JFileChooser;

// other imports
import java.util.List;
import java.util.ArrayList;
import java.util.Calendar;
import net.java.dev.spellcast.utilities.JComponentUtilities;

/**
 * An extension of <code>KoLFrame</code> used to display the current
 * chat contents.  This updates according to the refresh rate specified
 * in <code>ChatRequest</code> and allows users to both read what has
 * already happened in chat and to log their conversations.
 */

public class ChatFrame extends KoLFrame
{
	private ChatPanel mainPanel;

	/**
	 * Constructs a new <code>ChatFrame</code>.
	 * @param	client	The client associated with the chat session
	 */

	public ChatFrame( KoLmafia client )
	{	this( client, "" );
	}

	/**
	 * Constructs a new <code>ChatFrame</code> which is intended to be
	 * used for instant messaging to the specified contact.
	 */

	public ChatFrame( KoLmafia client, String associatedContact )
	{
		super( client, "" );

		framePanel.setLayout( new BorderLayout( 5, 5 ) );
		initialize( associatedContact );

		if ( GLOBAL_SETTINGS.getProperty( "useToolbars" ).equals( "true" ) )
		{
			toolbarPanel.add( new MessengerButton( "Clear Chat Buffers", "clear.gif", "clearChatBuffers" ) );
			toolbarPanel.add( new MessengerButton( "Add Highlighting", "highlight1.gif", "addHighlighting" ) );
			toolbarPanel.add( new MessengerButton( "Remove Highlighting", "highlight2.gif", "removeHighlighting" ) );
			toolbarPanel.add( new MessengerButton( "/friends", "who1.gif", "checkFriends" ) );
			toolbarPanel.add( new MessengerButton( "/who on talking channel", "who2.gif", "checkChannel" ) );
		}

		// Set the default size so that it doesn't appear super-small
		// when it's first constructed

		setSize( new Dimension( 400, 300 ) );

		if ( client != null && mainPanel != null && associatedContact != null )
		{
			if ( associatedContact.startsWith( "/" ) )
				setTitle( "KoLmafia Chat: " + associatedContact );
			else
				setTitle( "KoLmafia NSIPM: " + KoLCharacter.getUsername() + " / " + associatedContact );
		}
	}

	/**
	 * Utility method called to initialize the frame.  This
	 * method should be overridden, should a different means
	 * of initializing the content of the frame be needed.
	 */

	protected void initialize( String associatedContact )
	{
		this.mainPanel = new ChatPanel( associatedContact );
		framePanel.add( mainPanel, BorderLayout.CENTER );
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
					autoscroll = getMaximum() - getVisibleAmount() - getValue() < 100;

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
		{
			if ( entryField == null || chatDisplay == null )
				return false;

			return entryField.hasFocus() || chatDisplay.hasFocus();
		}

		public void requestFocus()
		{
			if ( entryField != null )
				entryField.requestFocus();
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
			private String lastMessage = null;

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
				if ( lastMessage != null && lastMessage.equals( entryField.getText().trim() ) )
					return;

				lastMessage = entryField.getText().trim();
				ChatRequest [] requests;

				if ( lastMessage.length() <= 256 )
				{
					// This is a standard-length message.  Send it
					// without adding additional divisions.

					requests = new ChatRequest[1];
					requests[0] = new ChatRequest( client, associatedContact, lastMessage );
				}
				else if ( lastMessage.length() < 1000 || associatedContact.equals( "/clan" ) )
				{
					// If the message is too long for one message, then
					// divide it into its component pieces.

					String trimmedMessage;
					List splitMessages = new ArrayList();
					int prevSpaceIndex = 0, nextSpaceIndex = 0;

					while ( nextSpaceIndex < lastMessage.length() )
					{
						nextSpaceIndex = prevSpaceIndex + 240 >= lastMessage.length() ? lastMessage.length() :
							lastMessage.lastIndexOf( " ", Math.min( prevSpaceIndex + 240, lastMessage.length() ) );

						if ( nextSpaceIndex == -1 )
							nextSpaceIndex = Math.min( prevSpaceIndex + 240, lastMessage.length() );

						trimmedMessage = lastMessage.substring( prevSpaceIndex, nextSpaceIndex ).trim();

						if ( prevSpaceIndex != 0 )
							trimmedMessage = "... " + trimmedMessage;
						if ( nextSpaceIndex != lastMessage.length() )
							trimmedMessage = trimmedMessage + " ...";
						if ( lastMessage.startsWith( "/" ) )
							trimmedMessage = "/me " + trimmedMessage.replaceFirst( "/[^\\s]*\\s+", "" );

						splitMessages.add( trimmedMessage );
						prevSpaceIndex = nextSpaceIndex;
					}

					splitMessages.add( lastMessage.substring( prevSpaceIndex ).trim() );
					requests = new ChatRequest[ splitMessages.size() ];

					for ( int i = 0; i < splitMessages.size(); ++i )
						requests[i] = new ChatRequest( client, associatedContact, (String) splitMessages.get(i) );
				}
				else
				{
					// If the person tried to send a message with more than
					// 1000 characters to a normal channel, that would flood
					// the chat too quickly.  Automatically truncate in this
					// case.

					requests = new ChatRequest[1];
					requests[0] = new ChatRequest( client, associatedContact, lastMessage.substring( 0, 256 ) );
				}

				(new RequestThread( requests )).start();
				entryField.setText( "" );
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
	{	return super.hasFocus() || (mainPanel != null && mainPanel.hasFocus());
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

	private static final String [] CHAT_OPTIONS =
		{ "Open blue message", "Open green message", "Open purple message", "Open player profile" };

	private class ChatLinkClickedListener extends KoLHyperlinkAdapter
	{
		protected void handleInternalLink( String location )
		{
			String [] locationSplit = location.split( "[=_]" );

			// First, determine the parameters inside of the
			// location which will be passed to frame classes.

			Object [] parameters = new Object[2];
			parameters[0] = client;
			parameters[1] = client.getPlayerName( locationSplit[1] );

			// Next, determine the option which had been
			// selected in the link-click.

			int linkOption = locationSplit.length == 2 ? 0 : Integer.parseInt( locationSplit[2] );

			Class frameClass = linkOption == 2 ? GreenMessageFrame.class :
				linkOption == 3 ? GiftMessageFrame.class : ProfileFrame.class;

			// Now, determine what needs to be done based
			// on the link option.

			if ( getProperty( "eSoluScriptlet" ).equals( "false" ) || linkOption == 1 )
				KoLMessenger.openInstantMessage( (String) parameters[1] );
			else
				SwingUtilities.invokeLater( new CreateFrameRunnable( frameClass, parameters ) );
		}
	}

	private class MessengerButton extends InvocationButton
	{
		public MessengerButton( String title, String image, String method )
		{	super( title, image, KoLMessenger.class, method );
		}
	}

	protected void processWindowEvent( WindowEvent e )
	{
		super.processWindowEvent( e );

		if ( e.getID() == WindowEvent.WINDOW_CLOSING )
		{
			if ( getAssociatedContact() == null )
				KoLMessenger.dispose();
			else
				KoLMessenger.removeChat( getAssociatedContact() );
		}
	}

	/**
	 * The main method used in the event of testing the way the
	 * user interface looks.  This allows the UI to be tested
	 * without having to constantly log in and out of KoL.
	 */

	public static void main( String [] args )
	{
		Object [] parameters = new Object[2];
		parameters[0] = null;
		parameters[1] = null;

		(new CreateFrameRunnable( ChatFrame.class, parameters )).run();
	}
}
