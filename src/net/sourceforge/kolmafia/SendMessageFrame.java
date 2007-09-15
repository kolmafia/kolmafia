/**
 * Copyright (c) 2005-2007, KoLmafia development team
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

package net.sourceforge.kolmafia;

import com.sun.java.forums.SpringUtilities;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.BorderLayout;
import java.awt.CardLayout;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JToolBar;

import javax.swing.SpringLayout;

import net.java.dev.spellcast.utilities.JComponentUtilities;
import net.java.dev.spellcast.utilities.LockableListModel;

public class SendMessageFrame extends KoLFrame
{
	private boolean isStorage = false;

	private JComboBox sourceSelect;
	private LockableListModel contacts;
	private MutableComboBox recipientEntry;

	private LockableListModel attachments;
	private AutoHighlightField attachedMeat;
	private JTextArea messageEntry;

	public SendMessageFrame( String recipient )
	{
		this();
		this.setRecipient( recipient );
	}

	public SendMessageFrame()
	{
		super( "Send a Message" );
		JPanel mainPanel = new JPanel( new SpringLayout() );

		// What kind of package you want to send.

		sourceSelect = new JComboBox();
		sourceSelect.addItem( "Send items/meat from inventory" );
		sourceSelect.addItem( "Send items/meat from ancestral storage" );
		sourceSelect.addActionListener( new AttachmentClearListener() );

		// Who you want to send it to.

		contacts = (LockableListModel) contactList.clone();
		recipientEntry = new MutableComboBox( contacts, true );

		// How much you want to attach, in raw terms.

		attachments = new LockableListModel();
		attachedMeat = new AutoHighlightField( "0" );

		// Now, layout the center part of the panel.

		mainPanel.add( new JLabel( "Source:  ", JLabel.RIGHT ) );
		mainPanel.add( sourceSelect );
		mainPanel.add( new JLabel( "Recipient:  ", JLabel.RIGHT ) );
		mainPanel.add( recipientEntry );
		mainPanel.add( new JLabel( "Send Meat:  ", JLabel.RIGHT ) );
		mainPanel.add( attachedMeat );
		SpringUtilities.makeCompactGrid( mainPanel, 3, 2, 5, 5, 5, 5 );

		// Construct the east container.

		JButton attach = new InvocationButton( "Attach an item", "icon_plus.gif", this, "attachItem" );
		JComponentUtilities.setComponentSize( attach, 15, 15 );
		JPanel labelPanel = new JPanel( new BorderLayout( 5, 5 ) );
		labelPanel.add( attach, BorderLayout.WEST );
		labelPanel.add( new JLabel( "Click to attach an item", JLabel.LEFT ), BorderLayout.CENTER );

		JPanel attachPanel = new JPanel( new BorderLayout( 5, 5 ) );
		attachPanel.add( labelPanel, BorderLayout.NORTH );
		attachPanel.add( new SimpleScrollPane( attachments, 3 ), BorderLayout.CENTER );

		JPanel northPanel = new JPanel( new BorderLayout( 20, 20 ) );
		northPanel.add( mainPanel, BorderLayout.CENTER );
		northPanel.add( attachPanel, BorderLayout.EAST );

		JPanel mainHolder = new JPanel( new BorderLayout( 20, 20 ) );
		mainHolder.add( northPanel, BorderLayout.NORTH );

		// Add the message entry to the panel.

		messageEntry = new JTextArea();
		messageEntry.setFont( DEFAULT_FONT );
		messageEntry.setRows( 7 );
		messageEntry.setLineWrap( true );
		messageEntry.setWrapStyleWord( true );

		SimpleScrollPane scrollArea = new SimpleScrollPane( messageEntry );
		mainHolder.add( scrollArea, BorderLayout.CENTER );

		// Add a button to the bottom panel.

		JPanel sendPanel = new JPanel();
		sendPanel.add( new InvocationButton( "send message", this, "sendMessage" ) );
		mainHolder.add( sendPanel, BorderLayout.SOUTH );

		// Layout the major container.

		JPanel cardPanel = new JPanel( new CardLayout( 10, 10 ) );
		cardPanel.add( mainHolder, "" );

		this.framePanel.add( cardPanel, BorderLayout.CENTER );
	}

	public void setRecipient( String recipient )
	{
		this.isStorage = false;
		this.sourceSelect.setSelectedIndex( 0 );

		if ( !contacts.contains( recipient ) )
		{
			recipient = KoLmafia.getPlayerName( recipient );
			contacts.add( 0, recipient );
		}

		this.recipientEntry.getEditor().setItem( recipient );
		this.recipientEntry.setSelectedItem( recipient );

		this.attachments.clear();
		this.attachedMeat.setText( "" );
		this.messageEntry.setText( "" );
	}

	public boolean shouldAddStatusBar()
	{	return true;
	}

	private class AttachmentClearListener implements ActionListener
	{
		public void actionPerformed( ActionEvent e )
		{
			boolean wasStorage = isStorage;
			isStorage = sourceSelect.getSelectedIndex() == 1;

			if ( isStorage != wasStorage )
				attachments.clear();
		}
	}

	public void sendMessage()
	{
		Object [] attachmentsArray = new Object[ attachments.size() + 1 ];
		attachments.toArray( attachmentsArray );

		attachmentsArray[ attachments.size() ] = new AdventureResult( AdventureResult.MEAT, getValue( attachedMeat, 0 ) );

		String [] recipients = StaticEntity.getClient().extractTargets( (String) this.recipientEntry.getSelectedItem() );

		RequestThread.openRequestSequence();
		for ( int i = 0; i < recipients.length; ++i )
			KoLmafiaCLI.DEFAULT_SHELL.executeSendRequest( recipients[i], this.messageEntry.getText(), attachmentsArray, isStorage, false );

		RequestThread.closeRequestSequence();
	}

	public void attachItem()
	{
		LockableListModel source = isStorage ? storage : inventory;
		if ( source.isEmpty() )
			return;

		AdventureResult current;
		Object [] values = multiple( "What would you like to send?", source );

		if ( values.length < source.size() )
		{
			for ( int i = 0; i < values.length; ++i )
			{
				current = (AdventureResult) values[i];
				int amount = getQuantity( "How many " + current.getName() + " to send?", current.getCount() );

				if ( amount <= 0 )
					values[i] = null;
				else
					values[i] = current.getInstance( amount );
			}
		}

		for ( int i = 0; i < values.length; ++i )
			if ( values[i] != null )
				attachments.add( values[i] );
	}
}
