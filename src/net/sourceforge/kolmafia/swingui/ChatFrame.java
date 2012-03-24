/**
 * Copyright (c) 2005-2012, KoLmafia development team
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
import java.awt.Component;
import java.awt.Dimension;

import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;

import java.text.SimpleDateFormat;

import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.JToolBar;


import net.sourceforge.kolmafia.chat.ChatFormatter;
import net.sourceforge.kolmafia.chat.ChatManager;
import net.sourceforge.kolmafia.chat.ChatSender;
import net.sourceforge.kolmafia.chat.StyledChatBuffer;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.request.MallSearchRequest;

import net.sourceforge.kolmafia.session.ContactManager;

import net.sourceforge.kolmafia.swingui.button.InvocationButton;

import net.sourceforge.kolmafia.swingui.listener.DefaultComponentFocusTraversalPolicy;
import net.sourceforge.kolmafia.swingui.listener.HyperlinkAdapter;
import net.sourceforge.kolmafia.swingui.listener.StickyListener;
import net.sourceforge.kolmafia.swingui.listener.ThreadedListener;

import net.sourceforge.kolmafia.swingui.widget.RequestPane;

import net.sourceforge.kolmafia.utilities.StringUtilities;

import net.sourceforge.kolmafia.webui.RelayLoader;

public class ChatFrame
	extends GenericFrame
{
	private static final GenericRequest PROFILER = new GenericRequest( "" );
	private static final SimpleDateFormat MARK_TIMESTAMP = new SimpleDateFormat( "HH:mm:ss", Locale.US );

	private ChatPanel mainPanel;
	private JComboBox nameClickSelect;

	/**
	 * Constructs a new <code>ChatFrame</code> which is intended to be used for instant messaging to the specified
	 * contact.
	 */

	public ChatFrame( final String associatedContact )
	{
		super(
			associatedContact == null || associatedContact.equals( "" ) ?
			"Loathing Chat" :
			associatedContact.startsWith( "/" ) ?
			"Chat: " + associatedContact :
			"Chat PM: " + associatedContact );

		this.initialize( associatedContact );

		// Add the standard chat options which the user
		// simply clicks on for functionality.

		JToolBar toolbarPanel = this.getToolbar();

		if ( toolbarPanel != null )
		{
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
			this.nameClickSelect.addItem( "Name click friends the player" );
			this.nameClickSelect.addItem( "Name click baleets the player" );
			toolbarPanel.add( this.nameClickSelect );

			this.nameClickSelect.setSelectedIndex( 0 );
		}

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

		this.setFocusCycleRoot( true );
		this.setFocusTraversalPolicy( new DefaultComponentFocusTraversalPolicy( this.mainPanel ) );
	}

	public void focusGained( FocusEvent e )
	{
		if ( this.mainPanel != null )
		{
			this.mainPanel.requestFocus();
		}
	}

	public void focusLost( FocusEvent e )
	{
	}

	public JToolBar getToolbar()
	{
		if ( !Preferences.getBoolean( "useChatToolbar" ) )
		{
			return null;
		}

		JToolBar toolbarPanel = super.getToolbar( true );

		toolbarPanel.add( new InvocationButton( "/friends", "who2.gif", ChatManager.class, "checkFriends" ) );

		toolbarPanel.add( Box.createHorizontalStrut( 10 ) );

		toolbarPanel.add( new InvocationButton(
			"Add Highlighting", "highlight1.gif", ChatFormatter.class, "addHighlighting" ) );

		toolbarPanel.add( new InvocationButton(
			"Remove Highlighting", "highlight2.gif", ChatFormatter.class, "removeHighlighting" ) );

		return toolbarPanel;
	}

	public Component getCenterComponent()
	{
		return this.getFramePanel();
	}

	public JTabbedPane getTabbedPane()
	{
		return null;
	}

	public boolean shouldAddStatusBar()
	{
		return false;
	}

	public boolean showInWindowMenu()
	{
		return false;
	}

	public void dispose()
	{
		String contact = this.getAssociatedContact();

		if ( contact != null && contact.equals( ChatManager.getCurrentChannel() ) )
		{
			contact = null;
		}

		ChatManager.closeWindow( contact );

		super.dispose();
	}

	/**
	 * Utility method called to initialize the frame. This method should be overridden, should a different means of
	 * initializing the content of the frame be needed.
	 */

	public void initialize( final String associatedContact )
	{
		this.mainPanel = new ChatPanel( associatedContact );
		this.setCenterComponent( this.mainPanel );
	}

	/**
	 * Utility method for creating a single panel containing the chat display and the entry area. Note that calling this
	 * method changes the <code>RequestPane</code> returned by calling the <code>getChatDisplay()</code> method.
	 */

	public class ChatPanel
		extends JPanel
		implements FocusListener
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

			JScrollPane scroller = ChatManager.getBuffer( associatedContact ).addDisplay( this.chatDisplay );
			scroller.getVerticalScrollBar().addAdjustmentListener( new StickyListener( this.chatDisplay, 200 ) );
			this.add( scroller, BorderLayout.CENTER );

			this.add( entryPanel, BorderLayout.SOUTH );
			this.setFocusTraversalPolicy( new DefaultComponentFocusTraversalPolicy( this.entryField ) );

			this.addFocusListener( this );
		}

		public void focusGained( FocusEvent e )
		{
			this.entryField.requestFocus();
		}

		public void focusLost( FocusEvent e )
		{
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

		/**
		 * An action listener responsible for sending the text contained within the entry panel to the KoL chat server
		 * for processing. This listener spawns a new request to the server which then handles everything that's needed.
		 */

		private class ChatEntryListener
			extends ThreadedListener
		{
			protected void execute()
			{
				if ( this.isAction() )
				{
					this.submitChat();
					return;
				}

				int keyCode = this.getKeyCode();

				if ( keyCode == KeyEvent.VK_UP )
				{
					if ( ChatPanel.this.lastCommandIndex <= 0 )
					{
						return;
					}

					ChatPanel.this.entryField.setText( (String) ChatPanel.this.commandHistory.get( --ChatPanel.this.lastCommandIndex ) );
				}
				else if ( keyCode == KeyEvent.VK_DOWN )
				{
					if ( ChatPanel.this.lastCommandIndex + 1 >= ChatPanel.this.commandHistory.size() )
					{
						return;
					}

					ChatPanel.this.entryField.setText( (String) ChatPanel.this.commandHistory.get( ++ChatPanel.this.lastCommandIndex ) );
				}
				else if ( keyCode == KeyEvent.VK_ENTER )
				{
					this.submitChat();
				}
			}

			private void submitChat()
			{
				String message = ChatPanel.this.entryField.getText();

				if ( message.equals( "" ) )
				{
					return;
				}

				ChatPanel.this.entryField.setText( "" );

				StyledChatBuffer buffer = ChatManager.getBuffer( ChatPanel.this.associatedContact );

				if ( message.startsWith( "/clear" ) || message.startsWith( "/cls" ) || message.equals( "clear" ) || message.equals( "cls" ) )
				{
					buffer.clear();
					return;
				}

				if ( message.equals( "/m" ) || message.startsWith( "/mark" ) )
				{
					buffer.append( "<br><hr><center><font size=2>" + ChatFrame.MARK_TIMESTAMP.format( new Date() ) + "</font></center><br>" );
					return;
				}

				if ( message.startsWith( "/?" ) || message.startsWith( "/help" ) )
				{
					RelayLoader.openSystemBrowser( "http://www.kingdomofloathing.com/doc.php?topic=chat_commands" );
					return;
				}

				ChatSender.sendMessage( ChatPanel.this.associatedContact, message, false );
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
	 * Action listener responsible for displaying private message window when a username is clicked, or opening the page
	 * in a browser if you're clicking something other than the username.
	 */

	private class ChatLinkClickedListener
		extends HyperlinkAdapter
	{
		public void handleInternalLink( final String location )
		{
			if ( location.startsWith( "makeoffer" ) || location.startsWith( "counteroffer" ) || location.startsWith( "bet" ) || location.startsWith( "messages" ) )
			{
				RelayLoader.openSystemBrowser( location, true );
				return;
			}

			int equalsIndex = location.indexOf( "=" );

			if ( equalsIndex == -1 )
			{
				RelayLoader.openSystemBrowser( location, true );
				return;
			}

			String[] locationSplit = new String[ 2 ];
			locationSplit[ 0 ] = location.substring( 0, equalsIndex );
			locationSplit[ 1 ] = location.substring( equalsIndex + 1 );

			// First, determine the parameters inside of the
			// location which will be passed to frame classes.

			String playerId = locationSplit[ 1 ];
			String playerName = ContactManager.getPlayerName( playerId );

			// Next, determine the option which had been
			// selected in the link-click.

			int linkOption = ChatFrame.this.nameClickSelect != null ?
				ChatFrame.this.nameClickSelect.getSelectedIndex(): 0;

			String urlString = null;

			switch ( linkOption )
			{
			case 1:
				String bufferKey = ChatManager.getBufferKey( playerName );
				ChatManager.openWindow( bufferKey, false );
				return;

			case 2:

				Object[] parameters = new Object[]
				{ playerName
				};

				GenericFrame.createDisplay( SendMessageFrame.class, parameters );
				return;

			case 3:
				urlString = "makeoffer.php?towho=" + playerId;
				break;

			case 4:
				urlString = "displaycollection.php?who=" + playerId;
				break;

			case 5:
				urlString = "ascensionhistory.php?who=" + playerId;
				break;

			case 6:
				GenericFrame.createDisplay( MallSearchFrame.class );
				MallSearchFrame.searchMall( new MallSearchRequest( StringUtilities.parseInt( playerId ) ) );
				return;

			case 7:
				ChatSender.sendMessage( playerName, "/whois", false );
				return;

			case 8:
				ChatSender.sendMessage( playerName, "/friend", false );
				return;

			case 9:
				ChatSender.sendMessage( playerName, "/baleet", false );
				return;

			default:
				urlString = "showplayer.php?who=" + playerId;
				break;
			}

			if ( Preferences.getBoolean( "chatLinksUseRelay" ) || !urlString.startsWith( "show" ) )
			{
				RelayLoader.openSystemBrowser( urlString );
			}
			else
			{
				ProfileFrame.showRequest( ChatFrame.PROFILER.constructURLString( urlString ) );
			}
		}
	}

}
