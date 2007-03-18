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

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.util.List;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import net.java.dev.spellcast.utilities.JComponentUtilities;
import net.java.dev.spellcast.utilities.LockableListModel;
import net.java.dev.spellcast.utilities.SortedListModel;

public class SendMessageFrame extends KoLFrame
{
	public boolean usingStorage;
	public JPanel messagePanel;
	public JComboBox recipientEntry;
	public JTextArea messageEntry;
	public JButton sendMessageButton;

	public JTextField attachedMeat;
	public SortedListModel attachments;

	public ShowDescriptionList attachmentList;
	public ItemManagePanel inventoryPanel;
	public ItemManagePanel storagePanel;

	public SendMessageFrame()
	{	this( "" );
	}

	public SendMessageFrame( String recipient )
	{
		super( "Send a Message" );

		JPanel mainPanel = new JPanel();
		mainPanel.setLayout( new BoxLayout( mainPanel, BoxLayout.Y_AXIS ) );

		JPanel centerPanel = new JPanel( new BorderLayout( 20, 20 ) );
		centerPanel.add( Box.createHorizontalStrut( 40 ), BorderLayout.CENTER );

		// Who you want to send it to.

		recipientEntry = new MutableComboBox( (LockableListModel) contactList.clone(), true );

		JButton refreshButton = new InvocationButton( "Refresh contact list", "refresh.gif", this, "refreshContactList" );
		JComponentUtilities.setComponentSize( recipientEntry, 250, 25 );
		JComponentUtilities.setComponentSize( refreshButton, 20, 20 );

		if ( !recipient.equals( "" ) )
		{
			recipientEntry.addItem( recipient );
			recipientEntry.getEditor().setItem( recipient );
			recipientEntry.setSelectedItem( recipient );
		}

		JPanel contactsPanel = new JPanel( new BorderLayout( 5, 5 ) );
		contactsPanel.add( recipientEntry, BorderLayout.CENTER );
		contactsPanel.add( refreshButton, BorderLayout.EAST );

		JPanel recipientPanel = new JPanel( new BorderLayout() );

		recipientPanel.add( getLabelPanel( "I intend to send it to this person:" ), BorderLayout.CENTER );
		recipientPanel.add( contactsPanel, BorderLayout.SOUTH );

		// And the message entry area.

		messageEntry = new JTextArea();
		messageEntry.setFont( DEFAULT_FONT );
		messageEntry.setRows( 7 );
		messageEntry.setLineWrap( true );
		messageEntry.setWrapStyleWord( true );

		SimpleScrollPane scrollArea = new SimpleScrollPane( messageEntry );

		JPanel entryPanel = new JPanel( new BorderLayout() );
		entryPanel.add( getLabelPanel( "I'd like them to see this message: " ), BorderLayout.CENTER );
		entryPanel.add( scrollArea, BorderLayout.SOUTH );

		sendMessageButton = new JButton( "Send Message" );
		sendMessageButton.addActionListener( new SendMessageListener() );

		// Add in meat attachments.

		attachedMeat = new JTextField( "0" );
		JComponentUtilities.setComponentSize( attachedMeat, 250, 25 );

		JPanel meatPanel = new JPanel( new BorderLayout() );
		meatPanel.add( getLabelPanel( "I'd like to attach this much meat: " ), BorderLayout.CENTER );
		meatPanel.add( attachedMeat, BorderLayout.SOUTH );

		// Add in the inventory panel

		JTabbedPane sources = getTabbedPane();

		inventoryPanel = new ItemManagePanel( inventory );
		inventoryPanel.elementList.setVisibleRowCount( 8 );
		inventoryPanel.setButtons( null );
		sources.addTab( "Inventory", inventoryPanel );

		inventoryPanel.elementList.contextMenu.remove( 2 );
		inventoryPanel.elementList.contextMenu.remove( 2 );

		inventoryPanel.elementList.contextMenu.add( new AddAttachmentMenuItem( inventoryPanel ) );

		// Add in the storage panel

		storagePanel = new ItemManagePanel( storage );
		storagePanel.elementList.setVisibleRowCount( 8 );
		storagePanel.setButtons( null );
		sources.addTab( "In Storage", storagePanel );

		storagePanel.elementList.contextMenu.add( new AddAttachmentMenuItem( storagePanel ) );

		// Construct the panels

		JPanel dataPanel = new JPanel();
		dataPanel.setLayout( new BoxLayout( dataPanel, BoxLayout.Y_AXIS ) );

		dataPanel.add( recipientPanel );
		dataPanel.add( Box.createVerticalStrut( 20 ) );
		dataPanel.add( entryPanel );
		dataPanel.add( Box.createVerticalStrut( 20 ) );
		dataPanel.add( meatPanel );

		dataPanel.add( Box.createVerticalStrut( 20) );
		dataPanel.add( Box.createVerticalGlue() );

		JPanel sendMessageButtonPanel = new JPanel();
		sendMessageButtonPanel.add( sendMessageButton );

		dataPanel.add( sendMessageButtonPanel );
		dataPanel.add( Box.createVerticalGlue() );

		JPanel dataHolder = new JPanel( new CardLayout( 10, 10 ) );
		dataHolder.add( dataPanel, "" );
		tabs.addTab( "Message", dataHolder );

		JPanel attachmentPanel = new JPanel( new BorderLayout( 20, 20 ) );

		JPanel itemPanel = new JPanel( new BorderLayout( 5, 5 ) );

		attachments = new SortedListModel();
		attachmentList = new ShowDescriptionList( attachments );
		attachmentList.contextMenu.add( new RemoveAttachmentMenuItem() );

		itemPanel.add( getLabelPanel( "Attach these items to the message: " ), BorderLayout.CENTER );
		itemPanel.add( new SimpleScrollPane( attachmentList ), BorderLayout.SOUTH );

		attachmentPanel.add( itemPanel, BorderLayout.NORTH );
		attachmentPanel.add( sources, BorderLayout.CENTER );

		JPanel attachmentHolder = new JPanel( new CardLayout( 10, 10 ) );
		attachmentHolder.add( attachmentPanel, "" );
		tabs.addTab( "Attachments", attachmentHolder );

		// Create a description message

		JTextArea message = new JTextArea(
			"\nIn order to add/remove items from your message, access the \"context menu\" (right-click on Windows and Linux, control-click on Macs).\n\n" +
			"Items may only come from one source.  If your items are located in your inventory, messages will be sent as kmails.  If the kmail attempt fails, or your items are coming from storage, KoLmafia will send an appropriately-sized package.\n\nIf there are items from Hagnk's to be sent, meat will be sent from Hagnk's.  If no items are attached and there are insufficient funds on-hand, meat will be sent from Hagnk's.  However, if there are items attached which are coming from your inventory and you don't have enough meat on-hand, the meat amount takes precedence and your item attachments will be cleared." );

		message.setColumns( 40 );
		message.setLineWrap( true );
		message.setWrapStyleWord( true );
		message.setEditable( false );
		message.setOpaque( false );
		message.setFont( DEFAULT_FONT );

		JPanel messageHolder = new JPanel( new CardLayout( 10, 10 ) );
		messageHolder.add( message, "" );
		tabs.addTab( "Layout Help", messageHolder );

		// A handy send message button

		framePanel.setLayout( new CardLayout( 10, 10 ) );
		framePanel.add( tabs, "" );
	}

	public JPanel getLabelPanel( String text )
	{
		JPanel label = new JPanel( new GridLayout( 1, 1 ) );
		label.add( new JLabel( text, JLabel.LEFT ) );
		return label;
	}


	private abstract class AttachmentMenuItem extends ThreadedMenuItem
	{
		public ItemManagePanel elementPanel;

		public AttachmentMenuItem( String title, ItemManagePanel elementPanel )
		{
			super( title );
			this.elementPanel = elementPanel;
		}
	}

	private class AddAttachmentMenuItem extends AttachmentMenuItem
	{
		public AddAttachmentMenuItem( ItemManagePanel elementPanel )
		{	super( "Attach to message", elementPanel );
		}

		public void run()
		{
			AdventureResult [] items = elementPanel.getDesiredItems( "Attaching" );
			if ( items == null || items.length == 0 )
				return;

			if ( !usingStorage && elementPanel == storagePanel )
			{
				usingStorage = true;
				attachments.clear();
			}
			else if ( usingStorage && elementPanel != storagePanel )
			{
				usingStorage = false;
				attachments.clear();
			}

			for ( int i = 0; i < items.length; ++i )
				AdventureResult.addResultToList( attachments, items[i] );
		}
	}

	private class RemoveAttachmentMenuItem extends AttachmentMenuItem
	{
		public RemoveAttachmentMenuItem()
		{	super( "Remove attachment", null );
		}

		public void run()
		{
			Object [] items = attachmentList.getSelectedValues();
			if ( items == null || items.length == 0 )
				return;

			for ( int i = 0; i < items.length; ++i )
				attachments.remove( items[i] );
		}
	}

	private class SendMessageListener extends ThreadedListener
	{
		public void run()
		{
			String [] recipients = StaticEntity.getClient().extractTargets( (String) recipientEntry.getSelectedItem() );
			if ( recipients.length == 0 || recipients[0].equals( "" ) )
			{
				KoLmafia.updateDisplay( "You didn't specify someone to send to." );
				return;
			}

			String message = messageEntry.getText();

			// Send the message to all recipients on the list.
			// If one of them fails, however, immediately stop
			// and notify the user that there was failure.

			RequestThread.openRequestSequence();
			Object [] attachments = getAttachedItems();

			for ( int i = 0; i < recipients.length && KoLmafia.permitsContinue(); ++i )
				DEFAULT_SHELL.executeSendRequest( recipients[i], message, attachments, usingStorage );

			RequestThread.closeRequestSequence();
		}
	}

	/**
	 * Sets all of the internal panels to a disabled or enabled state; this
	 * prevents the user from modifying the data as it's getting sent, leading
	 * to uncertainty and generally bad things.
	 */

	public void setEnabled( boolean isEnabled )
	{
		if ( sendMessageButton == null )
			return;

		sendMessageButton.setEnabled( isEnabled );
	}

	public Object [] getAttachedItems()
	{
		AdventureResult meatAttachment = new AdventureResult( AdventureResult.MEAT, getValue( attachedMeat ) );
		attachments.remove( meatAttachment );

		if ( meatAttachment.getCount() > 0 )
		{
			if ( !usingStorage && (!KoLCharacter.canInteract() || meatAttachment.getCount() > KoLCharacter.getAvailableMeat()) )
			{
				attachments.clear();
				usingStorage = true;
			}

			attachments.add( meatAttachment );
		}

		return attachments.toArray();
	}

	public void refreshContactList()
	{
		RequestThread.postRequest( new ContactListRequest() );
		recipientEntry.setModel( (SortedListModel) contactList.clone() );
	}
}
