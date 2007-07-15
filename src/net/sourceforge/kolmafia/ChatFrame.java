/**
 * Copyright (c) 2005-2007, KoLmafia development team
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
 *  [3] Neither the name "KoLmafia" nor the names of its contributors may
 *      be used to endorse or promote products derived from this software
 *      without specific prior written permission.
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

import java.awt.BorderLayout;
import java.awt.Dimension;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.text.SimpleDateFormat;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.JToolBar;

public class ChatFrame extends KoLFrame
{
	protected static final String MSGS_TAB = "/msgs";
	protected static final String GCLI_TAB = "/gcli";

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

		this.setDefaultCloseOperation( DISPOSE_ON_CLOSE );
		this.framePanel.setLayout( new BorderLayout( 5, 5 ) );
		this.initialize( associatedContact );

		// Add the standard chat options which the user
		// simply clicks on for functionality.

		JToolBar toolbarPanel = this.getToolbar();
		if ( toolbarPanel != null )
		{
			toolbarPanel.add( new MessengerButton( "/who", "who2.gif", "checkChannel" ) );

			toolbarPanel.add( Box.createHorizontalStrut( 10 ) );

			toolbarPanel.add( new MessengerButton( "Add Highlighting", "highlight1.gif", "addHighlighting" ) );
			toolbarPanel.add( new MessengerButton( "Remove Highlighting", "highlight2.gif", "removeHighlighting" ) );

			toolbarPanel.add( Box.createHorizontalStrut( 10 ) );


			toolbarPanel.add( Box.createHorizontalStrut( 10 ) );
		}

		// Add the name click options as a giant combo
		// box, rather than a hidden menu.

		this.nameClickSelect = new JComboBox();
		this.nameClickSelect.addItem( "Name click shows player profile" );
		this.nameClickSelect.addItem( "Name click opens blue message" );
		this.nameClickSelect.addItem( "Name click opens green message" );
		this.nameClickSelect.addItem( "Name click opens gift message" );
		this.nameClickSelect.addItem( "Name click opens trade message" );
		this.nameClickSelect.addItem( "Name click shows display case" );
		this.nameClickSelect.addItem( "Name click shows ascension history" );
		this.nameClickSelect.addItem( "Name click shows mall store" );
		this.nameClickSelect.addItem( "Name click performs /whois" );
		this.nameClickSelect.addItem( "Name click baleets the player" );

		if ( toolbarPanel != null )
			toolbarPanel.add( this.nameClickSelect );

		this.nameClickSelect.setSelectedIndex(0);

		// Set the default size so that it doesn't appear super-small
		// when it's first constructed

		this.setSize( new Dimension( 500, 300 ) );

		if ( this.mainPanel != null && associatedContact != null )
		{
			if ( associatedContact.startsWith( "/" ) )
				this.setTitle( "KoL Chat: " + associatedContact );
			else
				this.setTitle( "KoL PM: " + KoLCharacter.getUserName() + " / " + associatedContact );
		}
	}

	public JToolBar getToolbar()
	{	return StaticEntity.getBooleanProperty( "useChatToolbar" ) ? super.getToolbar() : null;
	}

	public JTabbedPane getTabbedPane()
	{	return null;
	}

	public boolean shouldAddStatusBar()
	{	return false;
	}

	public void dispose()
	{
		super.dispose();
		if ( this.getAssociatedContact() == null )
			KoLMessenger.dispose();
		else
			KoLMessenger.removeChat( this.getAssociatedContact() );
	}

	/**
	 * Utility method called to initialize the frame.  This
	 * method should be overridden, should a different means
	 * of initializing the content of the frame be needed.
	 */

	public void initialize( String associatedContact )
	{
		this.mainPanel = new ChatPanel( associatedContact );
		this.framePanel.add( this.mainPanel, BorderLayout.CENTER );
	}

	/**
	 * Utility method for creating a single panel containing the
	 * chat display and the entry area.  Note that calling this
	 * method changes the <code>RequestPane</code> returned by
	 * calling the <code>getChatDisplay()</code> method.
	 */

	public class ChatPanel extends JPanel
	{
		private int lastCommandIndex = 0;
		private ArrayList commandHistory;
		private JTextField entryField;
		private RequestPane chatDisplay;
		private String associatedContact;

		public ChatPanel( String associatedContact )
		{
			super( new BorderLayout() );
			this.chatDisplay = new RequestPane();
			this.chatDisplay.addHyperlinkListener( new ChatLinkClickedListener() );

			this.associatedContact = associatedContact;
			this.commandHistory = new ArrayList();

			ChatEntryListener listener = new ChatEntryListener();

			JPanel entryPanel = new JPanel( new BorderLayout() );
			this.entryField = new JTextField();
			this.entryField.addKeyListener( listener );

			JButton entryButton = new JButton( "chat" );
			entryButton.addActionListener( listener );
			entryPanel.add( this.entryField, BorderLayout.CENTER );
			entryPanel.add( entryButton, BorderLayout.EAST );

			if ( associatedContact.equals( GCLI_TAB ) )
				this.add( commandBuffer.setChatDisplay( this.chatDisplay ), BorderLayout.CENTER );
			else
				this.add( KoLMessenger.getChatBuffer( associatedContact ).setChatDisplay( this.chatDisplay ), BorderLayout.CENTER );

			this.add( entryPanel, BorderLayout.SOUTH );
		}

		public String getAssociatedContact()
		{	return this.associatedContact;
		}

		public boolean hasFocus()
		{
			if ( this.entryField == null || this.chatDisplay == null )
				return false;

			return this.entryField.hasFocus() || this.chatDisplay.hasFocus();
		}

		public void requestFocus()
		{
			if ( this.entryField != null )
				this.entryField.requestFocus();

			KoLMessenger.setUpdateChannel( this.getAssociatedContact() );
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
			{	this.submitChat();
			}

			public void keyReleased( KeyEvent e )
			{
				if ( e.getKeyCode() == KeyEvent.VK_UP )
				{
					if ( ChatPanel.this.lastCommandIndex <= 0 )
						return;

					ChatPanel.this.entryField.setText( (String) ChatPanel.this.commandHistory.get( --ChatPanel.this.lastCommandIndex ) );
				}
				else if ( e.getKeyCode() == KeyEvent.VK_DOWN )
				{
					if ( ChatPanel.this.lastCommandIndex + 1 >= ChatPanel.this.commandHistory.size() )
						return;

					ChatPanel.this.entryField.setText( (String) ChatPanel.this.commandHistory.get( ++ChatPanel.this.lastCommandIndex ) );
				}
				else if ( e.getKeyCode() == KeyEvent.VK_ENTER )
					this.submitChat();
			}

			private void submitChat()
			{
				String message = ChatPanel.this.entryField.getText();
				ChatPanel.this.entryField.setText( "" );

				if ( ChatPanel.this.associatedContact.equals( GCLI_TAB ) )
				{
					ChatPanel.this.commandHistory.add( message );

					if ( ChatPanel.this.commandHistory.size() > 10 )
						ChatPanel.this.commandHistory.remove(0);

					ChatPanel.this.lastCommandIndex = ChatPanel.this.commandHistory.size();

					CommandDisplayFrame.executeCommand( message );
					return;
				}

				LimitedSizeChatBuffer buffer = KoLMessenger.getChatBuffer( ChatPanel.this.associatedContact );

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

				KoLMessenger.setUpdateChannel( ChatPanel.this.associatedContact );

				if ( message.length() <= 256 )
				{
					// This is a standard-length message.  Send it
					// without adding additional divisions.

					RequestThread.postRequest( new ChatRequest( ChatPanel.this.associatedContact, message ) );
				}
				else if ( message.length() < 1000 || ChatPanel.this.associatedContact.equals( "/clan" ) )
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
						RequestThread.postRequest( new ChatRequest( ChatPanel.this.associatedContact, (String) splitMessages.get(i) ) );
				}
				else
				{
					// If the person tried to send a message with more than
					// 1000 characters to a normal channel, that would flood
					// the chat too quickly.  Automatically truncate in this
					// case.

					RequestThread.postRequest( new ChatRequest( ChatPanel.this.associatedContact, message.substring( 0, 256 ) ) );
				}
			}
		}
	}

	/**
	 * Returns the name of the contact associated with this frame.
	 * @return	The name of the contact associated with this frame
	 */

	public String getAssociatedContact()
	{	return this.mainPanel == null ? null : this.mainPanel.getAssociatedContact();
	}

	/**
	 * Returns whether or not the chat frame has focus.
	 */

	public boolean hasFocus()
	{	return super.hasFocus() || (this.mainPanel != null && this.mainPanel.hasFocus());
	}

	/**
	 * Requests focus to be placed on this chat frame.  It should
	 * be called whenever this window is first created.
	 */

	public void requestFocus()
	{
		super.requestFocus();
		if ( this.mainPanel != null )
			this.mainPanel.requestFocus();
	}

	/**
	 * Action listener responsible for displaying private message
	 * window when a username is clicked, or opening the page in
	 * a browser if you're clicking something other than the username.
	 */

	private class ChatLinkClickedListener extends KoLHyperlinkAdapter
	{
		public void handleInternalLink( String location )
		{
			if ( location.startsWith( "makeoffer.php" ) || location.startsWith( "counteroffer.php" ) )
			{
				StaticEntity.getClient().openRelayBrowser( location );
				return;
			}

			// <name> took your bet
			if ( location.startsWith( "bet.php" ) )
				return;

			String [] locationSplit = location.split( "[=_]" );

			// First, determine the parameters inside of the
			// location which will be passed to frame classes.

			String playerName = KoLmafia.getPlayerName( locationSplit[1] );
			Object [] parameters = new Object[] { playerName };

			// Next, determine the option which had been
			// selected in the link-click.

			int linkOption = locationSplit.length == 2 ? 0 : StaticEntity.parseInt( locationSplit[2] );

			if ( linkOption == 0 )
				linkOption = ChatFrame.this.nameClickSelect.getSelectedIndex();

			Class frameClass;

			switch ( linkOption )
			{
			case 1:
				KoLMessenger.openInstantMessage( (String) parameters[0], true );
				return;

			case 2:
				frameClass = SendMessageFrame.class;
				break;

			case 3:
				frameClass = SendMessageFrame.class;
				break;

			case 4:
				StaticEntity.getClient().openRelayBrowser( "makeoffer.php?action=proposeoffer&towho=" + parameters[0] );
				return;

			case 5:
				ProfileFrame.showRequest( new KoLRequest( "displaycollection.php?who=" + KoLmafia.getPlayerId( (String) parameters[0] ) ) );
				return;

			case 6:
				ProfileFrame.showRequest( new KoLRequest( "ascensionhistory.php?who=" + KoLmafia.getPlayerId( (String) parameters[0] ) ) );
				return;

			case 7:
				MallSearchFrame.searchMall( new SearchMallRequest( StaticEntity.parseInt( KoLmafia.getPlayerId( (String) parameters[0] ) ) ) );
				return;

			case 8:
				RequestThread.postRequest( new ChatRequest( "/whois", (String) parameters[0] ) );
				return;

			case 9:
				RequestThread.postRequest( new ChatRequest( "/baleet", (String) parameters[0] ) );
				return;

			default:
				ProfileFrame.showRequest( new ProfileRequest( (String) parameters[0] ) );
				return;
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

		public void run()
		{
			KoLMessenger.setUpdateChannel( ChatFrame.this.getAssociatedContact() );
			super.run();
		}
	}
}
