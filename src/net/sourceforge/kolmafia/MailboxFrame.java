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

import java.awt.Dimension;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JEditorPane;
import javax.swing.ListSelectionModel;
import javax.swing.JTabbedPane;

import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import net.java.dev.spellcast.utilities.LockableListModel;
import net.java.dev.spellcast.utilities.JComponentUtilities;

public class MailboxFrame extends KoLFrame implements ChangeListener
{
	private static final int MAXIMUM_MESSAGE_SIZE = 4000;

	private KoLMailManager mailbox;
	private JEditorPane messageContent;
	private JTabbedPane tabbedListDisplay;
	private LimitedSizeChatBuffer mailBuffer;

	private MailSelectList messageListInbox;
	private MailSelectList messageListOutbox;
	private MailSelectList messageListSaved;

	public MailboxFrame( KoLmafia client )
	{
		super( "KoLmafia IcePenguin Express", client );
		this.mailbox = (client == null) ? new KoLMailManager() : client.getMailManager();

		this.messageListInbox = new MailSelectList( "Inbox" );
		JScrollPane messageListInboxDisplay = new JScrollPane( messageListInbox,
			JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED );

		this.messageListOutbox = new MailSelectList( "Outbox" );
		JScrollPane messageListOutboxDisplay = new JScrollPane( messageListOutbox,
			JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED );

		this.messageListSaved = new MailSelectList( "Saved" );
		JScrollPane messageListSavedDisplay = new JScrollPane( messageListSaved,
			JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED );

		this.tabbedListDisplay = new JTabbedPane();
		tabbedListDisplay.addTab( "Inbox", messageListInbox );
		tabbedListDisplay.addTab( "Outbox", messageListOutbox );
		tabbedListDisplay.addTab( "Saved", messageListSaved );
		tabbedListDisplay.addChangeListener( this );

		this.messageContent = new JEditorPane();
		JScrollPane messageContentDisplay = new JScrollPane( messageContent,
			JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER );

		this.mailBuffer = new LimitedSizeChatBuffer( "KoL Mail Message", MAXIMUM_MESSAGE_SIZE );
		mailBuffer.setChatDisplay( messageContent );

		JSplitPane splitPane = new JSplitPane( JSplitPane.VERTICAL_SPLIT, true,
			tabbedListDisplay, messageContentDisplay );

		splitPane.setOneTouchExpandable( true );
		JComponentUtilities.setComponentSize( splitPane, 500, 300 );
		getContentPane().add( splitPane );

		(new RequestMailboxThread( "Inbox" )).start();
	}

	public void setEnabled( boolean isEnabled )
	{
		for ( int i = 0; i < tabbedListDisplay.getTabCount(); ++i )
			tabbedListDisplay.setEnabledAt( i, isEnabled );
	}

	/**
	 * Whenever the tab changes, this method is used to retrieve
	 * the messages from the appropriate client, if the mailbox
	 * is currently empty.
	 */

	public void stateChanged( ChangeEvent e )
	{
		mailBuffer.clearBuffer();
		String currentTabName = tabbedListDisplay.getTitleAt( tabbedListDisplay.getSelectedIndex() );
		boolean requestMailbox;

		if ( currentTabName.equals( "Inbox" ) )
			requestMailbox = !messageListInbox.isInitialized();
		else if ( currentTabName.equals( "Outbox" ) )
			requestMailbox = !messageListOutbox.isInitialized();
		else
			requestMailbox = !messageListSaved.isInitialized();

		if ( requestMailbox )
			(new RequestMailboxThread( currentTabName )).start();
	}

	private class RequestMailboxThread extends Thread
	{
		private String mailboxName;

		public RequestMailboxThread( String mailboxName )
		{
			super( "Request-Mailbox-Thread" );
			setDaemon( true );
			this.mailboxName = mailboxName;
		}

		public void run()
		{
			mailBuffer.append( "Retrieving messages from server..." );

			if ( client != null )
				(new MailboxRequest( client, mailboxName )).run();

			MailboxFrame.this.requestFocus();
			mailBuffer.clearBuffer();

			if ( mailboxName.equals( "Inbox" ) )
				messageListInbox.setInitialized( true );
			else if ( mailboxName.equals( "Outbox" ) )
				messageListOutbox.setInitialized( true );
			else
				messageListSaved.setInitialized( true );
		}
	}

	/**
	 * An internal class used to handle selection of a specific
	 * message from the mailbox list.
	 */

	private class MailSelectList extends JList implements ListSelectionListener
	{
		private String mailboxName;
		private boolean initialized;

		public MailSelectList( String mailboxName )
		{
			super( mailbox.getMessages( mailboxName ) );
			setSelectionMode( ListSelectionModel.SINGLE_SELECTION );
			this.mailboxName = mailboxName;
			addListSelectionListener( this );
		}

		public void valueChanged( ListSelectionEvent e )
		{
			int firstIndex = e.getFirstIndex();
			String newContent = ((KoLMailManager.KoLMailMessage)mailbox.getMessages( mailboxName ).get(firstIndex)).getMessageHTML();
			mailBuffer.clearBuffer();
			mailBuffer.append( newContent );
		}

		private boolean isInitialized()
		{	return initialized;
		}

		public void setInitialized( boolean initialized )
		{	this.initialized = initialized;
		}
	}

	public static void main( String [] args )
	{
		KoLFrame mailboxFrame = new MailboxFrame( null );
		mailboxFrame.pack();  mailboxFrame.setVisible( true );
		mailboxFrame.requestFocus();
	}
}
