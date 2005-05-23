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

public class ProposeTradeFrame extends KoLFrame
{
	private String offerID;
	private static final int COLS = 32;
	private static final int ROWS = 8;

	private JTextField recipientEntry;
	private JTextArea messageEntry;
	private JComboBox attachSelect;
	private JButton attachButton;

	private SortedListModel attachedItems;
	private JLabel sendMessageStatus;

	public ProposeTradeFrame( KoLmafia client )
	{	this( client, null );
	}

	public ProposeTradeFrame( KoLmafia client, String offerID )
	{
		super( "KoLmafia: Send a Trade Proposal", client );
		this.offerID = offerID;

		this.attachedItems = new SortedListModel();
		this.contentPanel = new ProposeTradePanel();

		this.getContentPane().setLayout( new CardLayout( 10, 10 ) );
		this.getContentPane().add( contentPanel, "" );

		setDefaultCloseOperation( DISPOSE_ON_CLOSE );
		addWindowListener( new ReturnFocusAdapter() );
		setResizable( false );
	}

	public void setEnabled( boolean isEnabled )
	{
	}

	private class ProposeTradePanel extends NonContentPanel
	{
		public ProposeTradePanel()
		{
			super( "send", "clear", new Dimension( 80, 20 ), new Dimension( 320, 20 ) );

			recipientEntry = new JTextField();

			JPanel attachPanel = new JPanel();
			attachPanel.setLayout( new BorderLayout( 0, 0 ) );
			attachSelect = new JComboBox( attachedItems );
			attachPanel.add( attachSelect, BorderLayout.CENTER );
			attachButton = new JButton( JComponentUtilities.getSharedImage( "icon_plus.gif" ) );
			JComponentUtilities.setComponentSize( attachButton, 20, 20 );
			attachButton.addActionListener( new AttachItemListener() );
			attachPanel.add( attachButton, BorderLayout.EAST );

			VerifiableElement [] elements = new VerifiableElement[2];
			elements[0] = new VerifiableElement( "Target:  ", recipientEntry );
			elements[1] = new VerifiableElement( "Attach:  ", attachPanel );

			setContent( elements );

			messageEntry = new JTextArea( ROWS, COLS );
			messageEntry.setLineWrap( true );
			messageEntry.setWrapStyleWord( true );
			JScrollPane scrollArea = new JScrollPane( messageEntry,
				JScrollPane.VERTICAL_SCROLLBAR_NEVER, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER );

			add( scrollArea, BorderLayout.CENTER );

			sendMessageStatus = new JLabel( " ", JLabel.LEFT );
			add( sendMessageStatus, BorderLayout.SOUTH );
		}

		public void actionConfirmed()
		{	(new ProposeTradeThread()).start();
		}

		public void actionCancelled()
		{
			recipientEntry.setText( "" );
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

		private class ProposeTradeThread extends Thread
		{
			public ProposeTradeThread()
			{
				super( "Propose-Trade-Thread" );
				setDaemon( true );
			}

			public void run()
			{
				if ( client == null )
					return;

				ProposeTradeFrame.this.setEnabled( false );
				if ( offerID == null )
					(new ProposeTradeRequest( client, offerID, recipientEntry.getText(), messageEntry.getText(), attachedItems.toArray() )).run();

				ProposeTradeFrame.this.dispose();
				KoLFrame frame = offerID == null ? new PendingTradesFrame( client, new ProposeTradeRequest( client ) ) :
					new PendingTradesFrame( client, new ProposeTradeRequest( client, recipientEntry.getText(), messageEntry.getText(), attachedItems.toArray() ) );

				frame.pack();  frame.setVisible( true );  frame.requestFocus();
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
					client == null ? new AdventureResult[1] : new AdventureResult[ client.getInventory().size() + 1 ];

				Object [] items = client.getInventory().toArray();
				for ( int i = 0; i < items.length; ++i )
					possibleValues[i+1] = (AdventureResult) items[i];
				possibleValues[0] = new AdventureResult( AdventureResult.MEAT, client == null ? 0 :
					client.getCharacterData().getAvailableMeat() );

				AdventureResult attachment = (AdventureResult) JOptionPane.showInputDialog(
					null, "Attach to message...", "Input", JOptionPane.INFORMATION_MESSAGE, null,
					possibleValues, possibleValues[0] );

				int existingIndex = attachedItems.indexOf( attachment );
				int defaultCount = attachment.getName().equals( AdventureResult.MEAT ) ? 0 : existingIndex != -1 ? 0 :
					((AdventureResult)client.getInventory().get( client.getInventory().indexOf( attachment ) )).getCount();

				int attachmentCount = df.parse( JOptionPane.showInputDialog(
					"Attaching " + attachment.getName() + "...", String.valueOf( defaultCount ) ) ).intValue();

				if ( existingIndex != -1 )
					attachedItems.remove( existingIndex );

				if ( attachment.getName().equals( AdventureResult.MEAT ) )
					AdventureResult.addResultToList( attachedItems, new AdventureResult( AdventureResult.MEAT, attachmentCount ) );
				else
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
		KoLFrame test = new ProposeTradeFrame( null );
		test.pack();  test.setVisible( true );  test.requestFocus();
	}
}