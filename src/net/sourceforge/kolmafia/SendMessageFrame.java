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
import java.awt.GridLayout;
import java.awt.BorderLayout;
import javax.swing.BoxLayout;

// event listeners
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

// containers
import java.awt.Component;
import javax.swing.Box;
import javax.swing.JLabel;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JFrame;
import javax.swing.JTextField;
import javax.swing.JTextArea;
import javax.swing.JScrollPane;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JMenuBar;
import javax.swing.JMenu;
import javax.swing.JMenuItem;

// other imports
import java.util.List;
import java.util.ArrayList;
import net.java.dev.spellcast.utilities.LockableListModel;
import net.java.dev.spellcast.utilities.JComponentUtilities;

public abstract class SendMessageFrame extends KoLFrame
{
	protected JPanel messagePanel;
	protected JTextField recipientEntry;
	protected JTextArea [] messageEntry;
	protected JButton sendMessageButton;
	protected JLabel sendMessageStatus;

	protected JTextField [] quantities;
	protected JComboBox [] attachments;
	protected LockableListModel inventory;

	protected SendMessageFrame( KoLmafia client, String title )
	{
		super( title, client );

		inventory = client == null ? new LockableListModel() : client.getInventory();

		JPanel mainPanel = new JPanel();
		mainPanel.setLayout( new BoxLayout( mainPanel, BoxLayout.Y_AXIS ) );

		JPanel centerPanel = new JPanel();
		centerPanel.setLayout( new BorderLayout( 5, 5 ) );
		centerPanel.add( Box.createHorizontalStrut( 40 ), BorderLayout.CENTER );

		centerPanel.add( constructWestPanel(), BorderLayout.WEST );

		JPanel attachmentPanel = new JPanel();
		attachmentPanel.setLayout( new BoxLayout( attachmentPanel, BoxLayout.Y_AXIS ) );
		attachmentPanel.add( getLabelPanel( "Enclose these items:" ) );

		this.attachments = new JComboBox[11];
		this.quantities = new JTextField[12];

		for ( int i = 0; i < 11; ++i )
		{
			this.quantities[i] = new JTextField( " " );
			JComponentUtilities.setComponentSize( quantities[i], 40, 24 );
			this.attachments[i] = new JComboBox( inventory.getMirrorImage() );

			JPanel currentPanel = new JPanel();
			currentPanel.setLayout( new BoxLayout( currentPanel, BoxLayout.X_AXIS ) );
			currentPanel.add( this.quantities[i] );
			currentPanel.add( Box.createHorizontalStrut( 4 ) );
			currentPanel.add( this.attachments[i] );

			JComponentUtilities.setComponentSize( currentPanel, 320, 24 );
			attachmentPanel.add( Box.createVerticalStrut( 4 ) );
			attachmentPanel.add( currentPanel );
		}

		JPanel meatPanel = new JPanel();
		meatPanel.setLayout( new BoxLayout( meatPanel, BoxLayout.X_AXIS ) );

		quantities[11] = new JTextField( "0" );
		JComponentUtilities.setComponentSize( quantities[11], 120, 24 );
		meatPanel.add( Box.createHorizontalStrut( 40 ) );
		meatPanel.add( new JLabel( "Enclose meat:    " ) );
		meatPanel.add( quantities[11] );
		meatPanel.add( Box.createHorizontalStrut( 40 ) );

		attachmentPanel.add( Box.createVerticalStrut( 12 ) );
		attachmentPanel.add( meatPanel );

		centerPanel.add( attachmentPanel, BorderLayout.EAST );

		mainPanel.add( centerPanel );
		mainPanel.add( Box.createVerticalStrut( 18 ) );
		mainPanel.add( contentPanel = new UpdatePanel() );
		mainPanel.add( Box.createVerticalStrut( 4 ) );

		messagePanel = new JPanel();
		messagePanel.setLayout( new BorderLayout() );
		messagePanel.add( mainPanel, BorderLayout.CENTER );

		this.getContentPane().setLayout( new CardLayout( 20, 20 ) );
		this.getContentPane().add( messagePanel, "" );
		setDefaultCloseOperation( DISPOSE_ON_CLOSE );
	}

	protected JPanel constructWestPanel()
	{
		String [] entryHeaders = getEntryHeaders();

		recipientEntry = new JTextField();
		JComponentUtilities.setComponentSize( recipientEntry, 300, 20 );

		messageEntry = new JTextArea[ entryHeaders.length ];
		JScrollPane [] scrollArea = new JScrollPane[ entryHeaders.length ];

		for ( int i = 0; i < messageEntry.length; ++i )
		{
			messageEntry[i] = new JTextArea();
			messageEntry[i].setLineWrap( true );
			messageEntry[i].setWrapStyleWord( true );
			scrollArea[i] = new JScrollPane( messageEntry[i], JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER );
		}

		JPanel recipientPanel = new JPanel();
		recipientPanel.setLayout( new BoxLayout( recipientPanel, BoxLayout.Y_AXIS ) );
		recipientPanel.add( getLabelPanel( "Send to this person:" ) );
		recipientPanel.add( Box.createVerticalStrut( 4 ) );
		recipientPanel.add( recipientEntry );
		recipientPanel.add( Box.createVerticalStrut( 20 ) );

		String [] westHeaders = getWestHeaders();
		Component [] westComponents = getWestComponents();

		for ( int i = 0; i < westHeaders.length; ++i )
		{
			recipientPanel.add( getLabelPanel( westHeaders[i] ) );
			recipientPanel.add( Box.createVerticalStrut( 4 ) );
			recipientPanel.add( westComponents[i] );
			recipientPanel.add( Box.createVerticalStrut( 20 ) );
		}

		JPanel entryPanel = new JPanel();
		entryPanel.setLayout( new GridLayout( entryHeaders.length, 1 ) );

		JPanel holderPanel;
		for ( int i = 0; i < entryHeaders.length; ++i )
		{
			holderPanel = new JPanel();
			holderPanel.setLayout( new BorderLayout( 5, 5 ) );
			holderPanel.add( getLabelPanel( entryHeaders[i] ), BorderLayout.NORTH );
			holderPanel.add( scrollArea[i], BorderLayout.CENTER );

			entryPanel.add( holderPanel );
		}

		JPanel westPanel = new JPanel();
		westPanel.setLayout( new BorderLayout() );
		westPanel.add( recipientPanel, BorderLayout.NORTH );
		westPanel.add( entryPanel, BorderLayout.CENTER );

		return westPanel;
	}

	protected String [] getEntryHeaders()
	{	return new String[0];
	}

	protected String [] getWestHeaders()
	{	return new String[0];
	}

	protected Component [] getWestComponents()
	{	return new Component[0];
	}

	protected JPanel getLabelPanel( String text )
	{
		JPanel label = new JPanel();
		label.setLayout( new GridLayout( 1, 1 ) );
		label.add( new JLabel( text, JLabel.LEFT ) );
		return label;
	}

	private class UpdatePanel extends NonContentPanel
	{
		public UpdatePanel()
		{
			super( "", "" );
			setContent( null );

 			sendMessageButton = new JButton( "Send Message" );
			sendMessageStatus = new JLabel( " ", JLabel.CENTER );

			JPanel sendMessageButtonPanel = new JPanel();
			sendMessageButtonPanel.add( sendMessageButton, BorderLayout.CENTER );

			setLayout( new BorderLayout( 20, 20 ) );
			add( sendMessageButtonPanel, BorderLayout.NORTH );
			add( sendMessageStatus, BorderLayout.SOUTH );
		}

		public void setStatusMessage( String message )
		{	sendMessageStatus.setText( message );
		}

		public void clear() {}
		public void actionConfirmed() {}
		public void actionCancelled() {}
	}

	/**
	 * Sets all of the internal panels to a disabled or enabled state; this
	 * prevents the user from modifying the data as it's getting sent, leading
	 * to uncertainty and generally bad things.
	 */

	public void setEnabled( boolean isEnabled )
	{
		if ( messageEntry != null )
			for ( int i = 0; i < messageEntry.length; ++i )
				if ( messageEntry[i] != null )
					messageEntry[i].setEnabled( isEnabled );

		if ( sendMessageButton != null )
			sendMessageButton.setEnabled( isEnabled );

		for ( int i = 0; i < attachments.length; ++i )
		{
			quantities[i].setEnabled( isEnabled );
			attachments[i].setEnabled( isEnabled );
		}

		quantities[11].setEnabled( isEnabled );
	}

	protected Object [] getAttachedItems()
	{
		List attachedItems = new ArrayList();

		AdventureResult currentItem;
		int currentQuantity;

		for ( int i = 0; i < attachments.length; ++i )
		{
			currentItem = (AdventureResult) attachments[i].getSelectedItem();
			if ( currentItem != null )
				attachedItems.add( currentItem.getInstance( getValue( quantities[i] ) ) );

		}

		return attachedItems.toArray();
	}

	protected int getAttachedMeat()
	{	return getValue( quantities[11] );
	}
}