/**
 * Copyright (c) 2005-2008, KoLmafia development team
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

package net.sourceforge.kolmafia.swingui;

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

import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.LimitedSizeChatBuffer;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.StaticEntity;
import net.sourceforge.kolmafia.persistence.Preferences;
import net.sourceforge.kolmafia.request.ChatRequest;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.request.MallSearchRequest;
import net.sourceforge.kolmafia.session.ChatManager;
import net.sourceforge.kolmafia.swingui.button.InvocationButton;
import net.sourceforge.kolmafia.swingui.listener.HyperlinkAdapter;
import net.sourceforge.kolmafia.swingui.widget.RequestPane;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class ChatFrame
	extends GenericFrame
{
	private static final GenericRequest PROFILER = new GenericRequest( "" );

	protected static final String MSGS_TAB = "/msgs";
	protected static final String GCLI_TAB = "/gcli";

	private static final SimpleDateFormat MARK_TIMESTAMP = new SimpleDateFormat( "HH:mm:ss", Locale.US );

	private ChatPanel mainPanel;
	private final JComboBox nameClickSelect;

	/**
	 * Constructs a new <code>ChatFrame</code>.
	 */

	public ChatFrame()
	{
		this( "" );
	}

	/**
	 * Constructs a new <code>ChatFrame</code> which is intended to be used for instant messaging to the specified
	 * contact.
	 */

	public ChatFrame( final String associatedContact )
	{
		super(
			associatedContact == null || associatedContact.equals( "" ) ? "Loathing Chat" : associatedContact.startsWith( "/" ) ? "Chat: " + associatedContact : "Chat PM: " + associatedContact );

		this.framePanel.setLayout( new BorderLayout( 5, 5 ) );
		this.initialize( associatedContact );

		// Add the standard chat options which the user
		// simply clicks on for functionality.

		JToolBar toolbarPanel = this.getToolbar();

		// Add the name click options as a giant combo
		// box, rather than a hidden menu.

		this.nameClickSelect = new JComboBox();
		this.nameClickSelect.addItem( "Name click shows player profile" );
		this.nameClickSelect.addItem( "Name click opens blue message" );
		this.nameClickSelect.addItem( "Name click sends kmail message" );
		this.nameClickSelect.addItem( "Name click opens trade request" );
		this.nameClickSelect.addItem( "Name click shows display case" );
		this.nameClickSelect.addItem( "Name click shows ascension history" );
		this.nameClickSelect.addItem( "Name click shows mall store" );
		this.nameClickSelect.addItem( "Name click performs /whois" );
		this.nameClickSelect.addItem( "Name click baleets the player" );

		if ( toolbarPanel != null )
		{
			toolbarPanel.add( this.nameClickSelect );
		}

		this.nameClickSelect.setSelectedIndex( 0 );

		// Set the default size so that it doesn't appear super-small
		// when it's first constructed

		this.setSize( new Dimension( 500, 300 ) );

		if ( this.mainPanel != null && associatedContact != null )
		{
			if ( associatedContact.startsWith( "/" ) )
			{
				this.setTitle( associatedContact );
			}
			else
			{
				this.setTitle( "private to " + associatedContact );
			}
		}
	}

	public JToolBar getToolbar()
	{

		if ( !Preferences.getBoolean( "useChatToolbar" ) )
		{
			return null;
		}

		JToolBar toolbarPanel = super.getToolbar( true );

		toolbarPanel.add( new MessengerButton( "/friends", "who2.gif", "checkFriends" ) );
		toolbarPanel.add( Box.createHorizontalStrut( 10 ) );

		toolbarPanel.add( new MessengerButton( "Add Highlighting", "highlight1.gif", "addHighlighting" ) );
		toolbarPanel.add( new MessengerButton( "Remove Highlighting", "highlight2.gif", "removeHighlighting" ) );

		return toolbarPanel;
	}

	public JTabbedPane getTabbedPane()
	{
		return null;
	}

	public boolean shouldAddStatusBar()
	{
		return false;
	}

	public void dispose()
	{
		super.dispose();

		if ( this.getAssociatedContact() == null )
		{
			ChatManager.dispose();
		}
		else
		{
			ChatManager.removeChat( this.getAssociatedContact() );
		}
	}

	/**
	 * Utility method called to initialize the frame. This method should be overridden, should a different means of
	 * initializing the content of the frame be needed.
	 */

	public void initialize( final String associatedContact )
	{
		this.mainPanel = new ChatPanel( associatedContact );
		this.framePanel.add( this.mainPanel, BorderLayout.CENTER );
	}

	/**
	 * Utility method for creating a single panel containing the chat display and the entry area. Note that calling this
	 * method changes the <code>RequestPane</code> returned by calling the <code>getChatDisplay()</code> method.
	 */

	public class ChatPanel
		extends JPanel
	{
		private int lastCommandIndex = 0;
		private final ArrayList commandHistory;
		private final JTextField entryField;
		private final RequestPane chatDisplay;
		private final String associatedContact;

		public ChatPanel( final String associatedContact )
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

			if ( associatedContact.equals( ChatFrame.GCLI_TAB ) )
			{
				this.add( KoLConstants.commandBuffer.setChatDisplay( this.chatDisplay ), BorderLayout.CENTER );
			}
			else
			{
				this.add(
					ChatManager.getChatBuffer( associatedContact ).setChatDisplay( this.chatDisplay ),
					BorderLayout.CENTER );
			}

			this.add( entryPanel, BorderLayout.SOUTH );
		}

		public String getAssociatedContact()
		{
			return this.associatedContact;
		}

		public boolean hasFocus()
		{
			if ( this.entryField == null || this.chatDisplay == null )
			{
				return false;
			}

			return this.entryField.hasFocus() || this.chatDisplay.hasFocus();
		}

		public void requestFocus()
		{
		}

		/**
		 * An action listener responsible for sending the text contained within the entry panel to the KoL chat server
		 * for processing. This listener spawns a new request to the server which then handles everything that's needed.
		 */

		private class ChatEntryListener
			extends KeyAdapter
			implements ActionListener
		{
			public void actionPerformed( final ActionEvent e )
			{
				this.submitChat();
			}

			public void keyReleased( final KeyEvent e )
			{
				if ( e.isConsumed() )
				{
					return;
				}

				if ( e.getKeyCode() == KeyEvent.VK_UP )
				{
					if ( ChatPanel.this.lastCommandIndex <= 0 )
					{
						return;
					}

					ChatPanel.this.entryField.setText( (String) ChatPanel.this.commandHistory.get( --ChatPanel.this.lastCommandIndex ) );
					e.consume();
				}
				else if ( e.getKeyCode() == KeyEvent.VK_DOWN )
				{
					if ( ChatPanel.this.lastCommandIndex + 1 >= ChatPanel.this.commandHistory.size() )
					{
						return;
					}

					ChatPanel.this.entryField.setText( (String) ChatPanel.this.commandHistory.get( ++ChatPanel.this.lastCommandIndex ) );
					e.consume();
				}
				else if ( e.getKeyCode() == KeyEvent.VK_ENTER )
				{
					this.submitChat();
					e.consume();
				}
			}

			private void submitChat()
			{
				String message = ChatPanel.this.entryField.getText();
				ChatPanel.this.entryField.setText( "" );

				if ( ChatPanel.this.associatedContact.equals( ChatFrame.GCLI_TAB ) )
				{
					ChatPanel.this.commandHistory.add( message );

					if ( ChatPanel.this.commandHistory.size() > 10 )
					{
						ChatPanel.this.commandHistory.remove( 0 );
					}

					ChatPanel.this.lastCommandIndex = ChatPanel.this.commandHistory.size();

					CommandDisplayFrame.executeCommand( message );
					return;
				}

				LimitedSizeChatBuffer buffer = ChatManager.getChatBuffer( ChatPanel.this.associatedContact );

				if ( message.startsWith( "/clear" ) || message.startsWith( "/cls" ) || message.equals( "clear" ) || message.equals( "cls" ) )
				{
					buffer.clearBuffer();
					return;
				}

				if ( message.equals( "/m" ) || message.startsWith( "/mark" ) )
				{
					buffer.append( "<br><hr><center><font size=2>" + ChatFrame.MARK_TIMESTAMP.format( new Date() ) + "</font></center><br>" );

					return;
				}

				if ( message.startsWith( "/?" ) || message.startsWith( "/help" ) )
				{
					StaticEntity.openSystemBrowser( "http://www.kingdomofloathing.com/doc.php?topic=chat_commands" );
					return;
				}

				if ( message.length() <= 256 )
				{
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
						nextSpaceIndex =
							prevSpaceIndex + 240 >= message.length() ? message.length() : message.lastIndexOf(
								" ", Math.min( prevSpaceIndex + 240, message.length() ) );

						if ( nextSpaceIndex == -1 )
						{
							nextSpaceIndex = Math.min( prevSpaceIndex + 240, message.length() );
						}

						trimmedMessage = message.substring( prevSpaceIndex, nextSpaceIndex ).trim();

						if ( prevSpaceIndex != 0 )
						{
							trimmedMessage = "... " + trimmedMessage;
						}
						if ( nextSpaceIndex != message.length() )
						{
							trimmedMessage = trimmedMessage + " ...";
						}
						if ( message.startsWith( "/" ) )
						{
							trimmedMessage = "/me " + trimmedMessage.replaceFirst( "/[^\\s]*\\s+", "" );
						}

						splitMessages.add( trimmedMessage );
						prevSpaceIndex = nextSpaceIndex;
					}

					for ( int i = 0; i < splitMessages.size(); ++i )
					{
						RequestThread.postRequest( new ChatRequest(
							ChatPanel.this.associatedContact, (String) splitMessages.get( i ) ) );
					}
				}
				else
				{
					RequestThread.postRequest( new ChatRequest( ChatPanel.this.associatedContact, message.substring(
						0, 256 ) ) );
				}
			}
		}
	}

	/**
	 * Returns the name of the contact associated with this frame.
	 *
	 * @return The name of the contact associated with this frame
	 */

	public String getAssociatedContact()
	{
		return this.mainPanel == null ? null : this.mainPanel.getAssociatedContact();
	}

	/**
	 * Returns whether or not the chat frame has focus.
	 */

	public boolean hasFocus()
	{
		return super.hasFocus() || this.mainPanel != null && this.mainPanel.hasFocus();
	}

	/**
	 * Requests focus to be placed on this chat frame. It should be called whenever this window is first created.
	 */

	public void requestFocus()
	{
		super.requestFocus();
		if ( this.mainPanel != null )
		{
			this.mainPanel.requestFocus();
		}
	}

	/**
	 * Action listener responsible for displaying private message window when a username is clicked, or opening the page
	 * in a browser if you're clicking something other than the username.
	 */

	private class ChatLinkClickedListener
		extends HyperlinkAdapter
	{
		public void handleInternalLink( final String location )
		{
			if ( location.startsWith( "makeoffer" ) || location.startsWith( "counteroffer" ) || location.startsWith( "bet" ) )
			{
				StaticEntity.getClient().openRelayBrowser( location );
				return;
			}

			String[] locationSplit = location.split( "[=_]" );

			// First, determine the parameters inside of the
			// location which will be passed to frame classes.

			String playerName = KoLmafia.getPlayerName( locationSplit[ 1 ] );
			Object[] parameters = new Object[] { playerName };

			// Next, determine the option which had been
			// selected in the link-click.

			int linkOption = locationSplit.length == 2 ? 0 : StringUtilities.parseInt( locationSplit[ 2 ] );

			if ( linkOption == 0 )
			{
				linkOption = ChatFrame.this.nameClickSelect.getSelectedIndex();
			}

			String urlString = null;

			switch ( linkOption )
			{
			case 1:
				ChatManager.openInstantMessage( (String) parameters[ 0 ], true );
				return;

			case 2:
				GenericFrame.createDisplay( SendMessageFrame.class, parameters );
				return;

			case 3:
				urlString = "makeoffer.php?towho=" + parameters[ 0 ];
				break;

			case 4:
				urlString = "displaycollection.php?who=" + KoLmafia.getPlayerId( (String) parameters[ 0 ] );
				break;

			case 5:
				urlString = "ascensionhistory.php?who=" + KoLmafia.getPlayerId( (String) parameters[ 0 ] );
				break;

			case 6:
				MallSearchFrame.searchMall( new MallSearchRequest(
					StringUtilities.parseInt( KoLmafia.getPlayerId( (String) parameters[ 0 ] ) ) ) );
				return;

			case 7:
				RequestThread.postRequest( new ChatRequest( "/whois", (String) parameters[ 0 ] ) );
				return;

			case 8:
				RequestThread.postRequest( new ChatRequest( "/baleet", (String) parameters[ 0 ] ) );
				return;

			default:
				urlString = "showplayer.php?who=" + KoLmafia.getPlayerId( (String) parameters[ 0 ] );
				break;
			}

			if ( Preferences.getBoolean( "chatLinksUseRelay" ) || !urlString.startsWith( "show" ) )
			{
				StaticEntity.getClient().openRelayBrowser( urlString, false );
			}
			else
			{
				ProfileFrame.showRequest( ChatFrame.PROFILER.constructURLString( urlString ) );
			}
		}
	}

	private class MessengerButton
		extends InvocationButton
	{
		public MessengerButton( final String title, final String image, final String method )
		{
			super( title, image, ChatManager.class, method );
		}

		public void run()
		{
			super.run();
		}
	}
}
