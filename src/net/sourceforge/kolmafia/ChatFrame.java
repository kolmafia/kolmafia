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
import javax.swing.JComboBox;

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
			toolbarPanel.add( new DisplayFrameButton( "Preferences", "preferences.gif", OptionsFrame.class ) );
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
		nameClickSelect.addItem( "Name click searches mall store" );
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
				setTitle( "KoLmafia Chat: " + associatedContact );
			else
				setTitle( "KoLmafia NSIPM: " + KoLCharacter.getUsername() + " / " + associatedContact );
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
				String currentMessage = entryField.getText();
				if ( currentMessage.startsWith( "/clear" ) || currentMessage.startsWith( "/cls" ) || currentMessage.equals( "clear" ) || currentMessage.equals( "cls" ) )
				{
					KoLMessenger.clearChatBuffers();
					return;
				}

				KoLMessenger.setUpdateChannel( associatedContact );
				ChatRequest [] requests;

				if ( currentMessage.length() <= 256 )
				{
					// This is a standard-length message.  Send it
					// without adding additional divisions.

					requests = new ChatRequest[1];
					requests[0] = new ChatRequest( StaticEntity.getClient(), associatedContact, currentMessage );
				}
				else if ( currentMessage.length() < 1000 || associatedContact.equals( "/clan" ) )
				{
					// If the message is too long for one message, then
					// divide it into its component pieces.

					String trimmedMessage;
					List splitMessages = new ArrayList();
					int prevSpaceIndex = 0, nextSpaceIndex = 0;

					while ( nextSpaceIndex < currentMessage.length() )
					{
						nextSpaceIndex = prevSpaceIndex + 240 >= currentMessage.length() ? currentMessage.length() :
							currentMessage.lastIndexOf( " ", Math.min( prevSpaceIndex + 240, currentMessage.length() ) );

						if ( nextSpaceIndex == -1 )
							nextSpaceIndex = Math.min( prevSpaceIndex + 240, currentMessage.length() );

						trimmedMessage = currentMessage.substring( prevSpaceIndex, nextSpaceIndex ).trim();

						if ( prevSpaceIndex != 0 )
							trimmedMessage = "... " + trimmedMessage;
						if ( nextSpaceIndex != currentMessage.length() )
							trimmedMessage = trimmedMessage + " ...";
						if ( currentMessage.startsWith( "/" ) )
							trimmedMessage = "/me " + trimmedMessage.replaceFirst( "/[^\\s]*\\s+", "" );

						splitMessages.add( trimmedMessage );
						prevSpaceIndex = nextSpaceIndex;
					}

					requests = new ChatRequest[ splitMessages.size() ];
					for ( int i = 0; i < splitMessages.size(); ++i )
						requests[i] = new ChatRequest( StaticEntity.getClient(), associatedContact, (String) splitMessages.get(i) );
				}
				else
				{
					// If the person tried to send a message with more than
					// 1000 characters to a normal channel, that would flood
					// the chat too quickly.  Automatically truncate in this
					// case.

					requests = new ChatRequest[1];
					requests[0] = new ChatRequest( StaticEntity.getClient(), associatedContact, currentMessage.substring( 0, 256 ) );
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
				(new RequestThread( new CreateFrameRunnable( PendingTradesFrame.class ) )).start();
				return;
			}

			// <name> took your bet
			if ( location.startsWith( "bet.php" ) )
				return;

			String [] locationSplit = location.split( "[=_]" );

			// First, determine the parameters inside of the
			// location which will be passed to frame classes.

			Object [] parameters = new Object[] { StaticEntity.getClient().getPlayerName( locationSplit[1] ) };

			// Next, determine the option which had been
			// selected in the link-click.

			int linkOption = locationSplit.length == 2 ? 0 : Integer.parseInt( locationSplit[2] );

			if ( linkOption == 0 )
				linkOption = nameClickSelect.getSelectedIndex();

			Class frameClass;

			switch ( linkOption )
			{
				case 1:
					KoLMessenger.openInstantMessage( (String) parameters[0] );
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

					MallSearchFrame mall = null;
					KoLFrame [] frames = new KoLFrame[ existingFrames.size() ];
					existingFrames.toArray( frames );

					for ( int i = 0; i < frames.length; ++i )
						if ( frames[i] instanceof MallSearchFrame )
							mall = (MallSearchFrame) frames[i];

					if ( mall == null )
					{
						CreateFrameRunnable creator = new CreateFrameRunnable( MallSearchFrame.class );
						creator.run();
						mall = (MallSearchFrame) creator.getCreation();
					}

					mall.searchMall( new SearchMallRequest( StaticEntity.getClient(), Integer.parseInt( KoLmafia.getPlayerID( (String) parameters[0] ) ) ) );
					return;

				case 6:
					StaticEntity.openRequestFrame( "displaycollection.php?who=" + KoLmafia.getPlayerID( (String) parameters[0] ) );
					return;

				case 7:
					StaticEntity.openRequestFrame( "ascensionhistory.php?who=" + KoLmafia.getPlayerID( (String) parameters[0] ) );
					return;

				case 8:
					(new RequestThread( new ChatRequest( StaticEntity.getClient(), "/whois", (String) parameters[0] ) )).start();
					return;

				case 9:
					(new RequestThread( new ChatRequest( StaticEntity.getClient(), "/baleet", (String) parameters[0] ) )).start();
					return;

				default:
					frameClass = ProfileFrame.class;
					break;
			}

			// Now, determine what needs to be done based
			// on the link option.

			(new RequestThread( new CreateFrameRunnable( frameClass, parameters ) )).start();
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
