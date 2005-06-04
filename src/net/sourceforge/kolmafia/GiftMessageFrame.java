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
import net.java.dev.spellcast.utilities.LockableListModel;
import net.java.dev.spellcast.utilities.JComponentUtilities;

public class GiftMessageFrame extends KoLFrame
{
	private static final int COLS = 32;
	private static final int ROWS = 8;

	private JTextField recipientEntry;
	private JTextField meatEntry;
	private JTextArea insideEntry, outsideEntry;
	private JComboBox attachSelect;
	private JComboBox packageSelect;
	private JButton attachButton;

	private SortedListModel attachedItems;
	private JLabel sendMessageStatus;

	public GiftMessageFrame( KoLmafia client )
	{	this( client, "" );
	}

	public GiftMessageFrame( KoLmafia client, String recipient )
	{	this( client, recipient, "" );
	}

	public GiftMessageFrame( KoLmafia client, String recipient, String quotedMessage )
	{
		super( "KoLmafia: Send a Purple Message", client );

		this.attachedItems = new SortedListModel();
		this.contentPanel = new GiftMessagePanel( recipient, quotedMessage );

		this.getContentPane().setLayout( new CardLayout( 10, 10 ) );
		this.getContentPane().add( contentPanel, "" );

		setDefaultCloseOperation( DISPOSE_ON_CLOSE );
		addWindowListener( new ReturnFocusAdapter() );
		setResizable( false );
	}

	public void setEnabled( boolean isEnabled )
	{
	}

	private class GiftMessagePanel extends NonContentPanel
	{
		public GiftMessagePanel( String recipient, String quotedMessage )
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

			packageSelect = new JComboBox( (LockableListModel) GiftMessageRequest.PACKAGES.clone() );

			VerifiableElement [] elements = new VerifiableElement[4];
			elements[0] = new VerifiableElement( "Target:  ", recipientEntry );
			elements[1] = new VerifiableElement( "Package:  ", packageSelect );
			elements[2] = new VerifiableElement( "Items:  ", attachPanel );
			elements[3] = new VerifiableElement( "Meat: ", meatEntry );

			setContent( elements );

			outsideEntry = new JTextArea( ROWS, COLS );
			outsideEntry.setLineWrap( true );
			outsideEntry.setWrapStyleWord( true );
			outsideEntry.setText( quotedMessage );

			JScrollPane outsideScroll = new JScrollPane( outsideEntry, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
				JScrollPane.HORIZONTAL_SCROLLBAR_NEVER );

			JPanel outsidePanel = new JPanel();
			outsidePanel.setLayout( new BorderLayout( 0, 0 ) );
			outsidePanel.add( JComponentUtilities.createLabel( "Outside Package", JLabel.CENTER, Color.black, Color.white ), BorderLayout.NORTH );
			outsidePanel.add( outsideScroll, BorderLayout.CENTER );

			insideEntry = new JTextArea( ROWS, COLS );
			insideEntry.setLineWrap( true );
			insideEntry.setWrapStyleWord( true );
			insideEntry.setText( quotedMessage );

			JScrollPane insideScroll = new JScrollPane( insideEntry, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
				JScrollPane.HORIZONTAL_SCROLLBAR_NEVER );

			JPanel insidePanel = new JPanel();
			insidePanel.setLayout( new BorderLayout( 0, 0 ) );
			insidePanel.add( JComponentUtilities.createLabel( "Inside Package", JLabel.CENTER, Color.black, Color.white ), BorderLayout.NORTH );
			insidePanel.add( insideScroll, BorderLayout.CENTER );

			JPanel centerPanel = new JPanel();
			centerPanel.setLayout( new GridLayout( 2, 1, 10, 10 ) );
			centerPanel.add( outsidePanel );
			centerPanel.add( insidePanel );

			add( centerPanel, BorderLayout.CENTER );
		}

		public void actionConfirmed()
		{	(new SendGiftMessageThread()).start();
		}

		public void actionCancelled()
		{
			recipientEntry.setText( "" );
			meatEntry.setText( "" );
			insideEntry.setText( "" );
			attachedItems.clear();
		}

		public void setEnabled( boolean isEnabled )
		{
			super.setEnabled( isEnabled );
			recipientEntry.setEnabled( isEnabled );
			insideEntry.setEnabled( isEnabled );
			attachSelect.setEnabled( isEnabled );
			packageSelect.setEnabled( isEnabled );
			attachButton.setEnabled( isEnabled );
		}

		private class SendGiftMessageThread extends RequestThread
		{
			public void run()
			{
				if ( client == null )
					return;

				GiftMessagePanel.this.setEnabled( false );
				(new GiftMessageRequest( client, recipientEntry.getText(), outsideEntry.getText(), insideEntry.getText(),
					packageSelect.getSelectedItem(), attachedItems.toArray(), getValue( meatEntry ) )).run();
				GiftMessagePanel.this.setEnabled( true );

				if ( client.permitsContinue() )
				{
					client.updateDisplay( ENABLED_STATE, "Gift sent to " + recipientEntry.getText() );
					String closeWindowSetting = client.getSettings().getProperty( "closeSending" );
					if ( closeWindowSetting != null && closeWindowSetting.equals( "true" ) )
						GiftMessageFrame.this.dispose();
				}
				else
					client.updateDisplay( ERROR_STATE, "Failed to send gift to " + recipientEntry.getText() );

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
				Object [] items = client.getInventory().toArray();

				if ( items.length == 0 )
				{
					JOptionPane.showMessageDialog( null, "KoLmafia does not detect any items in your inventory." );
					return;
				}

				AdventureResult attachment = (AdventureResult) JOptionPane.showInputDialog(
					null, "Add to gift...", "Input", JOptionPane.INFORMATION_MESSAGE, null, items, items[0] );

				int existingIndex = attachedItems.indexOf( attachment );
				int defaultCount = existingIndex != -1 ? 0 :
					((AdventureResult)client.getInventory().get( client.getInventory().indexOf( attachment ) )).getCount();

				int attachmentCount = df.parse( JOptionPane.showInputDialog(
					"Adding " + attachment.getName() + "...", String.valueOf( defaultCount ) ) ).intValue();

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
		KoLFrame test = new GiftMessageFrame( null );
		test.pack();  test.setVisible( true );  test.requestFocus();
	}
}