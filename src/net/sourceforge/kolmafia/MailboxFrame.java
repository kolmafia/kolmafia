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

import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import net.java.dev.spellcast.utilities.LockableListModel;

public class MailboxFrame extends KoLFrame
{
	private static final int MAXIMUM_MESSAGE_SIZE = 4000;

	private KoLMailManager mailbox;
	private JEditorPane messageContent;
	private LimitedSizeChatBuffer mailBuffer;

	public MailboxFrame( KoLmafia client )
	{
		super( "KoLmafia IcePenguin Express", client );
		(new MailboxRequest( client, "Inbox" )).run();

		this.mailbox = (client == null) ? new KoLMailManager() : client.getMailManager();
		JList messageList = new MailSelectList( "Inbox" );
		JScrollPane messageListDisplay = new JScrollPane( messageList,
			JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED );

		this.messageContent = new JEditorPane();
		JScrollPane messageContentDisplay = new JScrollPane( messageContent,
			JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER );

		this.mailBuffer = new LimitedSizeChatBuffer( "KoL Mail Message", MAXIMUM_MESSAGE_SIZE );
		mailBuffer.setChatDisplay( messageContent );

		JSplitPane splitPane = new JSplitPane( JSplitPane.VERTICAL_SPLIT, true,
			messageListDisplay, messageContentDisplay );

		splitPane.setOneTouchExpandable( true );
		getContentPane().add( splitPane );
	}

	private class MailSelectList extends JList implements ListSelectionListener
	{
		private String mailboxName;

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
	}

	public static void main( String [] args )
	{
		KoLFrame mailboxFrame = new MailboxFrame( null );
		mailboxFrame.pack();  mailboxFrame.setVisible( true );
		mailboxFrame.requestFocus();
	}
}
