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

// layout and containers
import java.awt.Dimension;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JEditorPane;
import javax.swing.ListSelectionModel;
import javax.swing.JTabbedPane;
import javax.swing.JOptionPane;
import javax.swing.JMenuBar;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JSeparator;

// event listeners
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.event.KeyListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

// other imports
import java.util.StringTokenizer;
import net.java.dev.spellcast.utilities.LockableListModel;
import net.java.dev.spellcast.utilities.JComponentUtilities;

/**
 * An extension of <code>KoLFrame</code> used to display the current
 * maiblox contents.  This updates whenever the user wishes to retrieve
 * more mail from their mailbox but otherwise does nothing.
 */

public class MailboxFrame extends KoLFrame implements ChangeListener
{
	private KoLMailMessage displayed;
	private JEditorPane messageContent;
	private JTabbedPane tabbedListDisplay;
	private LimitedSizeChatBuffer mailBuffer;

	private MailSelectList messageListInbox;
	private MailSelectList messageListOutbox;
	private MailSelectList messageListSaved;

	public MailboxFrame( KoLmafia client )
	{
		super( client, "IcePenguin Express" );

		this.messageListInbox = new MailSelectList( "Inbox" );
		JScrollPane messageListInboxDisplay = new JScrollPane( messageListInbox,
			JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER );

		this.messageListOutbox = new MailSelectList( "Outbox" );
		JScrollPane messageListOutboxDisplay = new JScrollPane( messageListOutbox,
			JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER );

		this.messageListSaved = new MailSelectList( "Saved" );
		JScrollPane messageListSavedDisplay = new JScrollPane( messageListSaved,
			JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER );

		this.tabbedListDisplay = new JTabbedPane();
		tabbedListDisplay.addTab( "Inbox", messageListInboxDisplay );
		tabbedListDisplay.addTab( "Outbox", messageListOutboxDisplay );
		tabbedListDisplay.addTab( "Saved", messageListSavedDisplay );
		tabbedListDisplay.addChangeListener( this );

		tabbedListDisplay.setMinimumSize( new Dimension( 0, 150 ) );

		this.messageContent = new JEditorPane();
		messageContent.setEditable( false );
		messageContent.addHyperlinkListener( new MailLinkClickedListener() );
		JScrollPane messageContentDisplay = new JScrollPane( messageContent,
			JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER );

		messageContentDisplay.setMinimumSize( new Dimension( 0, 150 ) );

		this.mailBuffer = new LimitedSizeChatBuffer( "KoL Mail Message" );
		mailBuffer.setChatDisplay( messageContent );

		JSplitPane splitPane = new JSplitPane( JSplitPane.VERTICAL_SPLIT, true,
			tabbedListDisplay, messageContentDisplay );

		splitPane.setOneTouchExpandable( true );
		JComponentUtilities.setComponentSize( splitPane, 500, 300 );
		getContentPane().add( splitPane );

		addMenuBar();

		if ( client != null )
			(new RequestMailboxThread( "Inbox" )).start();
	}

	private void addMenuBar()
	{
		JMenuBar menuBar = new JMenuBar();
		this.setJMenuBar( menuBar );

		addPeopleMenu( menuBar );

		JMenu optionsMenu = addOptionsMenu( menuBar );

		optionsMenu.add( new JSeparator() );

		JMenu refreshMenu = new JMenu( "Refresh" );
		refreshMenu.add( new BoxRefreshMenuItem( "Inbox" ) );
		refreshMenu.add( new BoxRefreshMenuItem( "Outbox" ) );
		refreshMenu.add( new BoxRefreshMenuItem( "Saved" ) );
		optionsMenu.add( refreshMenu );

		JMenu emptyMenu = new JMenu( "Move Mail" );
		emptyMenu.add( new BoxEmptyMenuItem( "" ) );
		emptyMenu.add( new BoxEmptyMenuItem( "Inbox" ) );
		emptyMenu.add( new BoxEmptyMenuItem( "Outbox" ) );
		emptyMenu.add( new BoxEmptyMenuItem( "Saved" ) );
		optionsMenu.add( emptyMenu );

		addHelpMenu( menuBar );
	}

	public void setEnabled( boolean isEnabled )
	{
		refreshMailManager();

		if ( tabbedListDisplay != null )
			for ( int i = 0; i < tabbedListDisplay.getTabCount(); ++i )
				tabbedListDisplay.setEnabledAt( i, isEnabled );

		if ( messageListInbox != null )
			messageListInbox.setEnabled( isEnabled );

		if ( messageListOutbox != null )
			messageListOutbox.setEnabled( isEnabled );

		if ( messageListSaved != null )
			messageListSaved.setEnabled( isEnabled );
	}

	/**
	 * Whenever the tab changes, this method is used to retrieve
	 * the messages from the appropriate client, if the mailbox
	 * is currently empty.
	 */

	public void stateChanged( ChangeEvent e )
	{
		refreshMailManager();
		mailBuffer.clearBuffer();
		String currentTabName = tabbedListDisplay.getTitleAt( tabbedListDisplay.getSelectedIndex() );
		boolean requestMailbox;

		if ( currentTabName.equals( "Inbox" ) )
		{
			if ( messageListInbox.isInitialized() )
				messageListInbox.valueChanged( null );
			requestMailbox = !messageListInbox.isInitialized();
		}
		else if ( currentTabName.equals( "Outbox" ) )
		{
			if ( messageListOutbox.isInitialized() )
				messageListOutbox.valueChanged( null );
			requestMailbox = !messageListOutbox.isInitialized();
		}
		else
		{
			if ( messageListSaved.isInitialized() )
				messageListSaved.valueChanged( null );
			requestMailbox = !messageListSaved.isInitialized();
		}

		if ( requestMailbox )
			(new RequestMailboxThread( currentTabName )).start();
	}

	private void refreshMailManager()
	{
		messageListInbox.setModel( KoLMailManager.getMessages( "Inbox" ) );
		messageListOutbox.setModel( KoLMailManager.getMessages( "Outbox" ) );
		messageListSaved.setModel( KoLMailManager.getMessages( "Saved" ) );
	}

	public void refreshMailbox()
	{
		if ( messageListInbox.isInitialized() )
			(new RequestMailboxThread( "Inbox" )).run();

		if ( messageListOutbox.isInitialized() )
			(new RequestMailboxThread( "Outbox" )).run();

		if ( messageListSaved.isInitialized() )
			(new RequestMailboxThread( "Saved" )).run();
	}

	private class RequestMailboxThread extends DaemonThread
	{
		private String mailboxName;

		public RequestMailboxThread( String mailboxName )
		{	this.mailboxName = mailboxName;
		}

		public void run()
		{
			refreshMailManager();
			mailBuffer.append( "Retrieving messages from server..." );

			if ( client != null )
				(new MailboxRequest( client, mailboxName )).run();

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
			super( KoLMailManager.getMessages( mailboxName ) );
			setSelectionMode( ListSelectionModel.MULTIPLE_INTERVAL_SELECTION );
			this.mailboxName = mailboxName;
			addListSelectionListener( this );
			addKeyListener( new MailboxKeyListener() );
		}

		public void valueChanged( ListSelectionEvent e )
		{	(new UpdateDisplayThread()).start();
		}

		private boolean isInitialized()
		{	return initialized;
		}

		public void setInitialized( boolean initialized )
		{	this.initialized = initialized;
		}

		private class MailboxKeyListener extends KeyAdapter
		{
			public void keyPressed( KeyEvent e )
			{
				if ( e.getKeyCode() == KeyEvent.VK_BACK_SPACE || e.getKeyCode() == KeyEvent.VK_DELETE )
				{
					if ( JOptionPane.YES_OPTION == JOptionPane.showConfirmDialog( null,
						"Would you like to delete the selected messages?", "Warning", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE ) )
							KoLMailManager.deleteMessages( mailboxName, getSelectedValues() );

				}

				if ( e.getKeyCode() == KeyEvent.VK_S )
				{
					if ( JOptionPane.YES_OPTION == JOptionPane.showConfirmDialog( null,
						"Would you like to save the selected messages?", "Warning", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE ) )
							KoLMailManager.saveMessages( getSelectedValues() );
				}
			}
		}

		private class UpdateDisplayThread extends DaemonThread
		{
			public void run()
			{
				mailBuffer.clearBuffer();
				int newIndex = getSelectedIndex();

				if ( newIndex >= 0 && getModel().getSize() > 0 )
				{
					displayed = ((KoLMailMessage)KoLMailManager.getMessages( mailboxName ).get( newIndex ));
					mailBuffer.append( displayed.getMessageHTML() );
					messageContent.setCaretPosition( 0 );
				}
			}
		}
	}

	/**
	 * Action listener responsible for displaying reply and quoted message
	 * windows when a username is clicked, or opening the page in
	 * a browser if you're clicking something other than the username.
	 */

	private class MailLinkClickedListener extends KoLHyperlinkAdapter
	{
		protected void handleInternalLink( String location )
		{
			StringTokenizer tokens = new StringTokenizer( location, "?=&" );
			tokens.nextToken();  tokens.nextToken();

			String recipient = tokens.nextToken();
			String quotedMessage = displayed.getMessageHTML().substring(
				displayed.getMessageHTML().indexOf( "<br><br>" ) + 8 ).replaceAll( "<b>", " " ).replaceAll(
					"><", "" ).replaceAll( "<.*?>", LINE_BREAK );

			Object [] parameters = new Object[ tokens.hasMoreTokens() ? 2 : 3 ];
			parameters[0] = client;
			parameters[1] = recipient;

			if ( parameters.length == 3 )
				parameters[2] = quotedMessage;

			(new CreateFrameRunnable( GreenMessageFrame.class, parameters )).run();
		}
	}

	private class BoxRefreshMenuItem extends JMenuItem implements ActionListener
	{
		private String boxname;

		public BoxRefreshMenuItem( String boxname )
		{
			super( boxname );
			addActionListener( this );

			this.boxname = boxname;
		}

		public void actionPerformed( ActionEvent e )
		{
			KoLMailManager.getMessages( boxname ).clear();
			(new RequestThread( new MailboxRequest( client, boxname ) )).start();
		}
	}

	private class BoxEmptyMenuItem extends JMenuItem implements ActionListener
	{
		private String boxname;

		public BoxEmptyMenuItem( String boxname )
		{
			super( boxname.equals( "" ) ? "Save All in Inbox" : "Delete All in " + boxname );
			addActionListener( this );

			this.boxname = boxname;
		}

		public void actionPerformed( ActionEvent e )
		{
			if ( boxname.equals( "" ) )
				KoLMailManager.saveMessages( KoLMailManager.getMessages( boxname ).toArray() );
			else
				KoLMailManager.deleteMessages( boxname, KoLMailManager.getMessages( boxname ).toArray() );
		}
	}

	public static void main( String [] args )
	{
		Object [] parameters = new Object[1];
		parameters[0] = null;

		(new CreateFrameRunnable( MailboxFrame.class, parameters )).run();
	}
}
