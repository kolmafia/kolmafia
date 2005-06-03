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
import java.awt.Dimension;
import java.awt.CardLayout;
import java.awt.BorderLayout;
import java.awt.GridLayout;

// event listeners
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

// containers
import javax.swing.JLabel;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JTextArea;
import javax.swing.JComboBox;
import javax.swing.JScrollPane;
import javax.swing.JOptionPane;

// other imports
import net.java.dev.spellcast.utilities.SortedListModel;
import net.java.dev.spellcast.utilities.JComponentUtilities;

public class GreenMessageFrame extends KoLFrame
{
	private static final int COLS = 32;
	private static final int ROWS = 8;

	private JTextField recipientEntry;
	private JTextField meatEntry;
	private JTextArea messageEntry;
	private JComboBox attachSelect;
	private JButton attachButton;

	private SortedListModel attachedItems;
	private JLabel sendMessageStatus;

	public GreenMessageFrame( KoLmafia client )
	{	this( client, "" );
	}

	public GreenMessageFrame( KoLmafia client, String recipient )
	{	this( client, recipient, "" );
	}

	public GreenMessageFrame( KoLmafia client, String recipient, String quotedMessage )
	{
		super( "KoLmafia: Send a Green Message", client );

		this.attachedItems = new SortedListModel();
		this.contentPanel = new GreenMessagePanel( recipient, quotedMessage );

		this.getContentPane().setLayout( new CardLayout( 10, 10 ) );
		this.getContentPane().add( contentPanel, "" );

		setDefaultCloseOperation( DISPOSE_ON_CLOSE );
		addWindowListener( new ReturnFocusAdapter() );
		setResizable( false );
	}

	public void setEnabled( boolean isEnabled )
	{
	}

	private class GreenMessagePanel extends NonContentPanel
	{
		public GreenMessagePanel( String recipient, String quotedMessage )
		{
			super( "send", "clear", new Dimension( 80, 20 ), new Dimension( 320, 20 ) );

			recipientEntry = new JTextField( recipient );
			meatEntry = new JTextField();

			JPanel attachPanel = new JPanel();
			attachPanel.setLayout( new BorderLayout( 0, 0 ) );
			attachSelect = new JComboBox( attachedItems );
			attachPanel.add( attachSelect, BorderLayout.CENTER );
			attachButton = new JButton( JComponentUtilities.getSharedImage( "icon_plus.gif" ) );
			JComponentUtilities.setComponentSize( attachButton, 20, 20 );
			attachButton.addActionListener( new AttachItemListener() );
			attachPanel.add( attachButton, BorderLayout.EAST );

			VerifiableElement [] elements = new VerifiableElement[3];
			elements[0] = new VerifiableElement( "Target:  ", recipientEntry );
			elements[1] = new VerifiableElement( "Items:  ", attachPanel );
			elements[2] = new VerifiableElement( "Meat: ", meatEntry );

			setContent( elements );

			messageEntry = new JTextArea( ROWS, COLS );
			messageEntry.setLineWrap( true );
			messageEntry.setWrapStyleWord( true );
			messageEntry.setText( quotedMessage );
			JScrollPane scrollArea = new JScrollPane( messageEntry,
				JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER );

			add( scrollArea, BorderLayout.CENTER );
		}

		public void actionConfirmed()
		{	(new SendGreenMessageThread()).start();
		}

		public void actionCancelled()
		{
			recipientEntry.setText( "" );
			meatEntry.setText( "" );
			messageEntry.setText( "" );
			attachedItems.clear();
		}

		public void setEnabled( boolean isEnabled )
		{
			super.setEnabled( isEnabled );
			recipientEntry.setEnabled( isEnabled );
			messageEntry.setEnabled( isEnabled );
			attachSelect.setEnabled( isEnabled );
			attachButton.setEnabled( isEnabled );
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
				if ( client == null )
					return;

				GreenMessagePanel.this.setEnabled( false );
				(new GreenMessageRequest( client, recipientEntry.getText(), messageEntry.getText(), attachedItems.toArray(),
					getValue( meatEntry ) )).run();
				GreenMessagePanel.this.setEnabled( true );

				if ( client.permitsContinue() )
				{
					client.updateDisplay( ENABLED_STATE, "Message sent to " + recipientEntry.getText() );
					String closeWindowSetting = client.getSettings().getProperty( "closeSending" );
					if ( closeWindowSetting != null && closeWindowSetting.equals( "true" ) )
						GreenMessageFrame.this.dispose();
				}
				else
					client.updateDisplay( ERROR_STATE, "Failed to send message to " + recipientEntry.getText() );

			}
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
				AdventureResult [] possibleValues =
					client == null ? new AdventureResult[0] : new AdventureResult[ client.getInventory().size() ];

				Object [] items = client.getInventory().toArray();

				AdventureResult attachment = (AdventureResult) JOptionPane.showInputDialog(
					null, "Attach to message...", "Input", JOptionPane.INFORMATION_MESSAGE, null,
					possibleValues, possibleValues[0] );

				int existingIndex = attachedItems.indexOf( attachment );
				if ( existingIndex == -1 )
					return;

				int defaultCount = ((AdventureResult)client.getInventory().get( client.getInventory().indexOf( attachment ) )).getCount();

				int attachmentCount = df.parse( JOptionPane.showInputDialog(
					"Attaching " + attachment.getName() + "...", String.valueOf( defaultCount ) ) ).intValue();

				if ( existingIndex != -1 )
					attachedItems.remove( existingIndex );

				AdventureResult.addResultToList( attachedItems, new AdventureResult( attachment.getItemID(), attachmentCount ) );
				attachedItems.setSelectedIndex( attachedItems.size() - 1 );
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
		KoLFrame test = new GreenMessageFrame( null );
		test.pack();  test.setVisible( true );  test.requestFocus();
	}
}