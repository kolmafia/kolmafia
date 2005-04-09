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
import java.awt.Color;
import java.awt.CardLayout;
import java.awt.BorderLayout;
import javax.swing.BoxLayout;

// event listeners
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

// containers
import javax.swing.Box;
import javax.swing.JLabel;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JFrame;
import javax.swing.JTextField;
import javax.swing.JTextArea;
import javax.swing.JScrollPane;
import javax.swing.JMenuBar;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;

// other imports
import net.java.dev.spellcast.utilities.SortedListModel;

public class GreenMessageFrame extends KoLFrame
{
	private static final int COLS = 32;
	private static final int ROWS = 8;

	private JTextField recipientEntry;
	private JTextArea messageEntry;
	private JButton sendMessageButton;

	private SortedListModel attachedItems;
	private JLabel attachedItemsDisplay;
	private JLabel sendMessageStatus;

	public GreenMessageFrame( KoLmafia client )
	{	this( client, "" );
	}

	public GreenMessageFrame( KoLmafia client, String recipient )
	{
		super( "KoLmafia: Send a Green Message", client );

		this.attachedItems = new SortedListModel();

		JPanel centerPanel = new JPanel();
		centerPanel.setLayout( new BoxLayout( centerPanel, BoxLayout.Y_AXIS ) );

		this.recipientEntry = new JTextField( recipient );
		JPanel recipientPanel = new JPanel();
		recipientPanel.setLayout( new BoxLayout( recipientPanel, BoxLayout.X_AXIS ) );
		recipientPanel.add( new JLabel( "To:  ", JLabel.LEFT ), "" );
		recipientPanel.add( recipientEntry, "" );

		this.attachedItemsDisplay = new JLabel( "(none)" );
		JPanel attachmentPanel = new JPanel();
		attachmentPanel.setLayout( new BorderLayout( 5, 5 ) );
		JButton attachButton = new JButton( "Attach" );
		attachButton.addActionListener( new AttachItemListener() );
		attachButton.setForeground( new Color( 0, 0, 128 ) );
		attachmentPanel.add( attachButton, BorderLayout.WEST );
		attachmentPanel.add( attachedItemsDisplay, BorderLayout.CENTER );

		this.messageEntry = new JTextArea( ROWS, COLS );
		messageEntry.setLineWrap( true );
		messageEntry.setWrapStyleWord( true );
		JScrollPane scrollArea = new JScrollPane( messageEntry,
			JScrollPane.VERTICAL_SCROLLBAR_NEVER, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER );

		centerPanel.add( recipientPanel, "" );
		centerPanel.add( Box.createVerticalStrut( 4 ) );
		centerPanel.add( attachmentPanel, "" );
		centerPanel.add( Box.createVerticalStrut( 4 ) );
		centerPanel.add( scrollArea, "" );
		centerPanel.add( Box.createVerticalStrut( 16 ) );

		this.sendMessageButton = new JButton( "Send" );
		sendMessageButton.addActionListener( new SendGreenMessageListener() );
		this.sendMessageStatus = new JLabel( " ", JLabel.LEFT );

		JPanel sendMessagePanel = new JPanel();
		sendMessagePanel.setLayout( new BorderLayout( 5, 5 ) );
		sendMessagePanel.add( sendMessageButton, BorderLayout.WEST );
		sendMessagePanel.add( sendMessageStatus, BorderLayout.CENTER );

		centerPanel.add( sendMessagePanel );
		centerPanel.add( Box.createVerticalStrut( 4 ) );

		this.getContentPane().setLayout( new CardLayout( 10, 10 ) );
		this.getContentPane().add( centerPanel, "" );
		setDefaultCloseOperation( DISPOSE_ON_CLOSE );
		setResizable( false );
	}

	/**
	 * Sets all of the internal panels to a disabled or enabled state; this
	 * prevents the user from modifying the data as it's getting sent, leading
	 * to uncertainty and generally bad things.
	 */

	public void setEnabled( boolean isEnabled )
	{
		if ( sendMessageButton != null )
			sendMessageButton.setEnabled( isEnabled );
	}

	/**
	 * Internal class used to handle sending a green message to the server.
	 */

	private class SendGreenMessageListener implements ActionListener
	{
		public void actionPerformed( ActionEvent e )
		{	(new SendGreenMessageThread()).start();
		}

		private class SendGreenMessageThread extends Thread
		{
			public SendGreenMessageThread()
			{
				super( "Green-Message-Thread" );
				setDaemon( true );
			}

			public void run()
			{
				recipientEntry.setEnabled( false );
				messageEntry.setEnabled( false );
				sendMessageButton.setEnabled( false );

				(new GreenMessageRequest( client, recipientEntry.getText(), messageEntry.getText(), attachedItems.toArray() )).run();

				if ( client.permitsContinue() )
					sendMessageStatus.setText( "Message sent to " + recipientEntry.getText() );
				else
					sendMessageStatus.setText( "Failed to send message to " + recipientEntry.getText() );

				recipientEntry.setEnabled( true );
				messageEntry.setEnabled( true );
				sendMessageButton.setEnabled( true );
			}
		}
	}

	private void resetAttachedItemsDisplay()
	{
		if ( attachedItems.size() == 0 )
			attachedItemsDisplay.setText( "(none)" );
		else
		{
			StringBuffer text = new StringBuffer( attachedItems.get(0).toString() );
			for ( int i = 1; i < attachedItems.size(); ++i )
			{
				text.append( ", " );
				text.append( attachedItems.get(i).toString() );
			}
			attachedItemsDisplay.setText( text.toString() );
		}
	}

	/**
	 * Internal class used to handle attaching items to the message.  This
	 * is done by prompting the user with a dialog box where they choose
	 * the item they wish to attach.  Note that only one of the item will
	 * be attached at a time.  This item cannot be removed after attaching.
	 */

	private class AttachItemListener implements ActionListener
	{
		public void actionPerformed( ActionEvent e )
		{
			try
			{
				AdventureResult [] possibleValues = new AdventureResult[ client.getInventory().size() + 1 ];
				client.getInventory().toArray( possibleValues );
				for ( int i = possibleValues.length - 1; i > 0; --i )
					possibleValues[i] = possibleValues[i-1];
				possibleValues[0] = new AdventureResult( AdventureResult.MEAT, client.getCharacterData().getAvailableMeat() );

				AdventureResult attachment = (AdventureResult) JOptionPane.showInputDialog(
					null, "Attach to message...", "Input", JOptionPane.INFORMATION_MESSAGE, null,
					possibleValues, possibleValues[0] );

				int existingIndex = attachedItems.indexOf( attachment );
				int defaultCount = attachment.getName().equals( AdventureResult.MEAT ) ? 0 : existingIndex != -1 ? 0 :
					((AdventureResult)client.getInventory().get( client.getInventory().indexOf( attachment ) )).getCount();

				int attachmentCount = df.parse( JOptionPane.showInputDialog(
					"Attaching " + attachment.getName() + "...", "" + defaultCount ) ).intValue();

				if ( existingIndex != -1 )
					attachedItems.remove( existingIndex );

				if ( attachment.getName().equals( AdventureResult.MEAT ) )
					AdventureResult.addResultToList( attachedItems, new AdventureResult( AdventureResult.MEAT, attachmentCount ) );
				else
					AdventureResult.addResultToList( attachedItems, new AdventureResult( attachment.getItemID(), attachmentCount ) );
				resetAttachedItemsDisplay();
			}
			catch ( Exception e1 )
			{
				// If an exception happened, the attachment should not occur.
				// Which means, if nothing is done, everything works great.
			}
		}
	}

	/**
	 * Main class used to view the user interface without having to actually
	 * start the program.  Used primarily for user interface testing.
	 */

	public static void main( String [] args )
	{
		JFrame test = new GreenMessageFrame( null );
		test.pack();  test.setVisible( true );  test.requestFocus();
	}
}