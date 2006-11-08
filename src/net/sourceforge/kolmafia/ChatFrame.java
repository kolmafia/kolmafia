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
import java.awt.BorderLayout;

// event listeners
import java.awt.event.KeyEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

// containers
import javax.swing.JToolBar;
import javax.swing.JPanel;
import javax.swing.JButton;
import javax.swing.JTextField;
import javax.swing.JEditorPane;
import javax.swing.JComboBox;

// other imports
import java.util.Date;
import java.util.List;
import java.util.ArrayList;
import java.util.Locale;
import java.text.SimpleDateFormat;
import javax.swing.SwingUtilities;

/**
 * An extension of <code>KoLFrame</code> used to display the current
 * chat contents.  This updates according to the refresh rate specified
 * in <code>ChatRequest</code> and allows users to both read what has
 * already happened in chat and to log their conversations.
 */

public class ChatFrame extends KoLFrame
{
	private static final SimpleDateFormat MARK_TIMESTAMP = new SimpleDateFormat( "HH:mm:ss", Locale.US );

	private ChatPanel mainPanel;
	private JComboBox nameClickSelect;

	/**
	 * Constructs a new <code>ChatFrame</code>.
	 */

	public ChatFrame()
	{	this( "" );
	}

	/**
	 * Constructs a new <code>ChatFrame</code> which is intended to be
	 * used for instant messaging to the specified contact.
	 */

	public ChatFrame( String associatedContact )
	{
		super( associatedContact == null || associatedContact.equals( "" ) ? "KoLmafia Chat" :
			associatedContact.startsWith( "/" ) ? "KoL Chat: " + associatedContact :
				"KoL PM: " + KoLCharacter.getUserName() + " / " + associatedContact );

		framePanel.setLayout( new BorderLayout( 5, 5 ) );
		initialize( associatedContact );

		// Add the standard chat options which the user
		// simply clicks on for functionality.

		JToolBar toolbarPanel = getToolbar();
		if ( toolbarPanel != null )
		{
			toolbarPanel.add( new MessengerButton( "Clear All", "clear.gif", "clearChatBuffers" ) );
			toolbarPanel.add( new JToolBar.Separator() );
			toolbarPanel.add( new MessengerButton( "Add Highlighting", "highlight1.gif", "addHighlighting" ) );
			toolbarPanel.add( new MessengerButton( "Remove Highlighting", "highlight2.gif", "removeHighlighting" ) );
			toolbarPanel.add( new JToolBar.Separator() );
			toolbarPanel.add( new MessengerButton( "/who", "who2.gif", "checkChannel" ) );
			toolbarPanel.add( new JToolBar.Separator() );
		}

		// Add the name click options as a giant combo
		// box, rather than a hidden menu.

		nameClickSelect = new JComboBox();
		nameClickSelect.addItem( "Name click shows player profile" );
		nameClickSelect.addItem( "Name click opens blue message" );
		nameClickSelect.addItem( "Name click opens green message" );
		nameClickSelect.addItem( "Name click opens gift message" );
		nameClickSelect.addItem( "Name click opens trade message" );
		nameClickSelect.addItem( "Name click shows display case" );
		nameClickSelect.addItem( "Name click shows ascension history" );
		nameClickSelect.addItem( "Name click performs /whois" );
		nameClickSelect.addItem( "Name click baleets the player" );

		if ( toolbarPanel != null )
			toolbarPanel.add( nameClickSelect );

		nameClickSelect.setSelectedIndex(0);

		// Set the default size so that it doesn't appear super-small
		// when it's first constructed

		setSize( new Dimension( 500, 300 ) );

		if ( mainPanel != null && associatedContact != null )
		{
			if ( associatedContact.startsWith( "/" ) )
				setTitle( "KoL Chat: " + associatedContact );
			else
				setTitle( "KoL PM: " + KoLCharacter.getUserName() + " / " + associatedContact );
		}
	}

	public void dispose()
	{
		super.dispose();
		(new ChatRemoverThread()).start();
	}

	private class ChatRemoverThread extends Thread
	{
		public void run()
		{
			if ( getAssociatedContact() == null )
				KoLMessenger.dispose();
			else
				KoLMessenger.removeChat( getAssociatedContact() );
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
		private JEditorPane chatDisplay;
		private String associatedContact;

		public ChatPanel( String associatedContact )
		{
			super( new BorderLayout() );
			chatDisplay = new JEditorPane();
			chatDisplay.addHyperlinkListener( new ChatLinkClickedListener() );
			this.associatedContact = associatedContact;

			ChatEntryListener listener = new ChatEntryListener();

			JPanel entryPanel = new JPanel( new BorderLayout() );
			entryField = new JTextField();
			entryField.addKeyListener( listener );

			JButton entryButton = new JButton( "chat" );
			entryButton.addActionListener( listener );
			entryPanel.add( entryField, BorderLayout.CENTER );
			entryPanel.add( entryButton, BorderLayout.EAST );

			add( KoLMessenger.getChatBuffer( associatedContact ).setChatDisplay( chatDisplay ), BorderLayout.CENTER );
			add( entryPanel, BorderLayout.SOUTH );
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

			KoLMessenger.setUpdateChannel( getAssociatedContact() );
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
				String message = entryField.getText();
				entryField.setText( "" );
				LimitedSizeChatBuffer buffer = KoLMessenger.getChatBuffer( associatedContact );

				if ( message.startsWith( "/clear" ) || message.startsWith( "/cls" ) || message.equals( "clear" ) || message.equals( "cls" ) )
				{
					buffer.clearBuffer();
					return;
				}

				if ( message.equals( "/m" ) || message.startsWith( "/mark" ) )
				{
					buffer.append( "<center><font size=2>&mdash;&mdash;&mdash;&nbsp;" +
						MARK_TIMESTAMP.format( new Date() ) + "&nbsp;&mdash;&mdash;&mdash;</font>" );

					return;
				}

				if ( message.startsWith( "/?" ) || message.startsWith( "/help" ) )
				{
					StaticEntity.openSystemBrowser( "http://www.kingdomofloathing.com/doc.php?topic=chat_commands" );
					return;
				}

				(new Thread( new ChatSubmitter( message ) )).start();
			}

			private class ChatSubmitter implements Runnable
			{
				private String message;

				public ChatSubmitter( String message )
				{	this.message = message;
				}

				public void run()
				{
					KoLMessenger.setUpdateChannel( associatedContact );

					if ( message.length() <= 256 )
					{
						// This is a standard-length message.  Send it
						// without adding additional divisions.

						(new ChatRequest( associatedContact, message )).run();
					}
					else if ( message.length() < 1000 || associatedContact.equals( "/clan" ) )
					{
						// If the message is too long for one message, then
						// divide it into its component pieces.

						String trimmedMessage;
						List splitMessages = new ArrayList();
						int prevSpaceIndex = 0, nextSpaceIndex = 0;

						while ( nextSpaceIndex < message.length() )
						{
							nextSpaceIndex = prevSpaceIndex + 240 >= message.length() ? message.length() :
								message.lastIndexOf( " ", Math.min( prevSpaceIndex + 240, message.length() ) );

							if ( nextSpaceIndex == -1 )
								nextSpaceIndex = Math.min( prevSpaceIndex + 240, message.length() );

							trimmedMessage = message.substring( prevSpaceIndex, nextSpaceIndex ).trim();

							if ( prevSpaceIndex != 0 )
								trimmedMessage = "... " + trimmedMessage;
							if ( nextSpaceIndex != message.length() )
								trimmedMessage = trimmedMessage + " ...";
							if ( message.startsWith( "/" ) )
								trimmedMessage = "/me " + trimmedMessage.replaceFirst( "/[^\\s]*\\s+", "" );

							splitMessages.add( trimmedMessage );
							prevSpaceIndex = nextSpaceIndex;
						}

						for ( int i = 0; i < splitMessages.size(); ++i )
							(new ChatRequest( associatedContact, (String) splitMessages.get(i) )).run();
					}
					else
					{
						// If the person tried to send a message with more than
						// 1000 characters to a normal channel, that would flood
						// the chat too quickly.  Automatically truncate in this
						// case.

						(new ChatRequest( associatedContact, message.substring( 0, 256 ) )).run();
					}
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
	 * Action listener responsible for displaying private message
	 * window when a username is clicked, or opening the page in
	 * a browser if you're clicking something other than the username.
	 */

	private class ChatLinkClickedListener extends KoLHyperlinkAdapter
	{
		protected void handleInternalLink( String location )
		{
			if ( location.startsWith( "makeoffer.php" ) )
			{
				createDisplay( PendingTradesFrame.class );
				return;
			}

			// <name> took your bet
			if ( location.startsWith( "bet.php" ) )
				return;

			String [] locationSplit = location.split( "[=_]" );

			// First, determine the parameters inside of the
			// location which will be passed to frame classes.

			String playerName = KoLmafia.getPlayerName( locationSplit[1] );
			if ( playerName == null )
				playerName = "#" + locationSplit[1];

			Object [] parameters = new Object[] { playerName };

			// Next, determine the option which had been
			// selected in the link-click.

			int linkOption = locationSplit.length == 2 ? 0 : StaticEntity.parseInt( locationSplit[2] );

			if ( linkOption == 0 )
				linkOption = nameClickSelect.getSelectedIndex();

			Class frameClass;

			switch ( linkOption )
			{
			case 1:
				KoLMessenger.openInstantMessage( (String) parameters[0], true );
				return;

			case 2:
				frameClass = GreenMessageFrame.class;
				break;

			case 3:
				frameClass = GiftMessageFrame.class;
				break;

			case 4:
				frameClass = ProposeTradeFrame.class;
				break;

			case 5:
				StaticEntity.openRequestFrame( "displaycollection.php?who=" + KoLmafia.getPlayerID( (String) parameters[0] ) );
				return;

			case 6:
				StaticEntity.openRequestFrame( "ascensionhistory.php?who=" + KoLmafia.getPlayerID( (String) parameters[0] ) );
				return;

			case 7:
				(new Thread( new ChatRequest( "/whois", (String) parameters[0] ) )).start();
				return;

			case 8:
				(new Thread( new ChatRequest( "/baleet", (String) parameters[0] ) )).start();
				return;

			default:
				frameClass = ProfileFrame.class;
				break;
			}

			// Now, determine what needs to be done based
			// on the link option.

			createDisplay( frameClass, parameters );
		}
	}

	private class MessengerButton extends InvocationButton
	{
		public MessengerButton( String title, String image, String method )
		{	super( title, image, KoLMessenger.class, method );
		}

		public void actionPerformed( ActionEvent e )
		{
			KoLMessenger.setUpdateChannel( getAssociatedContact() );
			super.actionPerformed( e );
		}
	}
}
