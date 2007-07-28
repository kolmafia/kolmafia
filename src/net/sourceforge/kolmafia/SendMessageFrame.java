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
import java.awt.GridLayout;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
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
		this.setDefaultCloseOperation( HIDE_ON_CLOSE );

		JPanel mainPanel = new JPanel();
		mainPanel.setLayout( new BoxLayout( mainPanel, BoxLayout.Y_AXIS ) );

		JPanel centerPanel = new JPanel( new BorderLayout( 20, 20 ) );
		centerPanel.add( Box.createHorizontalStrut( 40 ), BorderLayout.CENTER );

		// Who you want to send it to.

		this.recipientEntry = new MutableComboBox( (LockableListModel) contactList.clone(), true );

		JButton refreshButton = new InvocationButton( "Refresh contact list", "refresh.gif", this, "refreshContactList" );
		JComponentUtilities.setComponentSize( this.recipientEntry, 250, 25 );
		JComponentUtilities.setComponentSize( refreshButton, 20, 20 );

		JPanel contactsPanel = new JPanel( new BorderLayout( 5, 5 ) );
		contactsPanel.add( this.recipientEntry, BorderLayout.CENTER );
		contactsPanel.add( refreshButton, BorderLayout.EAST );

		JPanel recipientPanel = new JPanel( new BorderLayout() );

		recipientPanel.add( this.getLabelPanel( "I intend to send it to this person:" ), BorderLayout.CENTER );
		recipientPanel.add( contactsPanel, BorderLayout.SOUTH );

		// And the message entry area.

		this.messageEntry = new JTextArea();
		this.messageEntry.setFont( DEFAULT_FONT );
		this.messageEntry.setRows( 7 );
		this.messageEntry.setLineWrap( true );
		this.messageEntry.setWrapStyleWord( true );

		SimpleScrollPane scrollArea = new SimpleScrollPane( this.messageEntry );

		JPanel entryPanel = new JPanel( new BorderLayout() );
		entryPanel.add( this.getLabelPanel( "I'd like them to see this message: " ), BorderLayout.CENTER );
		entryPanel.add( scrollArea, BorderLayout.SOUTH );

		this.sendMessageButton = new JButton( "Send Message" );
		this.sendMessageButton.addActionListener( new SendMessageListener() );

		// Add in meat attachments.

		this.attachedMeat = new JTextField( "0" );
		JComponentUtilities.setComponentSize( this.attachedMeat, 250, 25 );

		JPanel meatPanel = new JPanel( new BorderLayout() );
		meatPanel.add( this.getLabelPanel( "I'd like to attach this much meat: " ), BorderLayout.CENTER );
		meatPanel.add( this.attachedMeat, BorderLayout.SOUTH );

		// Add in the inventory panel

		JTabbedPane sources = this.getTabbedPane();

		this.inventoryPanel = new ItemManagePanel( inventory );
		this.inventoryPanel.elementList.setVisibleRowCount( 8 );
		this.inventoryPanel.setButtons( null );
		sources.addTab( "Inventory", this.inventoryPanel );

		this.inventoryPanel.elementList.contextMenu.add( new AddAttachmentMenuItem( this.inventoryPanel ) );

		// Add in the storage panel

		this.storagePanel = new ItemManagePanel( storage );
		this.storagePanel.elementList.setVisibleRowCount( 8 );
		this.storagePanel.setButtons( null );
		sources.addTab( "In Storage", this.storagePanel );

		this.storagePanel.elementList.contextMenu.add( new AddAttachmentMenuItem( this.storagePanel ) );

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
		sendMessageButtonPanel.add( this.sendMessageButton );

		dataPanel.add( sendMessageButtonPanel );
		dataPanel.add( Box.createVerticalGlue() );

		JPanel dataHolder = new JPanel( new CardLayout( 10, 10 ) );
		dataHolder.add( dataPanel, "" );
		this.tabs.addTab( "Message", dataHolder );

		JPanel attachmentPanel = new JPanel( new BorderLayout( 20, 20 ) );

		JPanel itemPanel = new JPanel( new BorderLayout( 5, 5 ) );

		this.attachments = new SortedListModel();
		this.attachmentList = new ShowDescriptionList( this.attachments );
		this.attachmentList.contextMenu.add( new RemoveAttachmentMenuItem() );

		itemPanel.add( this.getLabelPanel( "Attach these items to the message: " ), BorderLayout.CENTER );
		itemPanel.add( new SimpleScrollPane( this.attachmentList ), BorderLayout.SOUTH );

		attachmentPanel.add( itemPanel, BorderLayout.NORTH );
		attachmentPanel.add( sources, BorderLayout.CENTER );

		JPanel attachmentHolder = new JPanel( new CardLayout( 10, 10 ) );
		attachmentHolder.add( attachmentPanel, "" );
		this.tabs.addTab( "Attachments", attachmentHolder );

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
		this.tabs.addTab( "Layout Help", messageHolder );

		// A handy send message button

		this.framePanel.setLayout( new CardLayout( 10, 10 ) );
		this.framePanel.add( this.tabs, "" );

		this.setRecipient( recipient );
	}

	public void setRecipient( String recipient )
	{
		if ( !recipient.equals( "" ) )
			return;

		this.recipientEntry.addItem( recipient );
		this.recipientEntry.getEditor().setItem( recipient );
		this.recipientEntry.setSelectedItem( recipient );
	}

	public JPanel getLabelPanel( String text )
	{
		JPanel label = new JPanel( new GridLayout( 1, 1 ) );
		label.add( new JLabel( text, JLabel.LEFT ) );
		return label;
	}

	public boolean shouldAddStatusBar()
	{
		return (!StaticEntity.getBooleanProperty( "addStatusBarToFrames" ) &&
			StaticEntity.getGlobalProperty( "initialDesktop" ).indexOf( this.frameName ) != -1) ||
			StaticEntity.getGlobalProperty( "initialDesktop" ).indexOf( this.frameName ) == -1;
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
			Object [] items = this.elementPanel.getDesiredItems( "Attaching" );
			if ( items == null || items.length == 0 )
				return;

			if ( !SendMessageFrame.this.usingStorage && this.elementPanel == SendMessageFrame.this.storagePanel )
			{
				SendMessageFrame.this.usingStorage = true;
				SendMessageFrame.this.attachments.clear();
			}
			else if ( SendMessageFrame.this.usingStorage && this.elementPanel != SendMessageFrame.this.storagePanel )
			{
				SendMessageFrame.this.usingStorage = false;
				SendMessageFrame.this.attachments.clear();
			}

			for ( int i = 0; i < items.length; ++i )
				AdventureResult.addResultToList( SendMessageFrame.this.attachments, (AdventureResult) items[i] );
		}
	}

	private class RemoveAttachmentMenuItem extends AttachmentMenuItem
	{
		public RemoveAttachmentMenuItem()
		{	super( "Remove attachment", null );
		}

		public void run()
		{
			Object [] items = SendMessageFrame.this.attachmentList.getSelectedValues();
			if ( items == null || items.length == 0 )
				return;

			for ( int i = 0; i < items.length; ++i )
				SendMessageFrame.this.attachments.remove( items[i] );
		}
	}

	private class SendMessageListener extends ThreadedListener
	{
		public void run()
		{
			String [] recipients = StaticEntity.getClient().extractTargets( (String) SendMessageFrame.this.recipientEntry.getSelectedItem() );
			if ( recipients.length == 0 || recipients[0].equals( "" ) )
			{
				KoLmafia.updateDisplay( "You didn't specify someone to send to." );
				return;
			}

			String message = SendMessageFrame.this.messageEntry.getText();

			// Send the message to all recipients on the list.
			// If one of them fails, however, immediately stop
			// and notify the user that there was failure.

			RequestThread.openRequestSequence();
			Object [] attachments = SendMessageFrame.this.getAttachedItems();

			for ( int i = 0; i < recipients.length && KoLmafia.permitsContinue(); ++i )
				DEFAULT_SHELL.executeSendRequest( recipients[i], message, attachments, SendMessageFrame.this.usingStorage, false );

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
		if ( this.sendMessageButton == null )
			return;

		this.sendMessageButton.setEnabled( isEnabled );
	}

	public Object [] getAttachedItems()
	{
		AdventureResult meatAttachment = new AdventureResult( AdventureResult.MEAT, getValue( this.attachedMeat ) );
		this.attachments.remove( meatAttachment );

		if ( meatAttachment.getCount() > 0 )
		{
			if ( !this.usingStorage && (!KoLCharacter.canInteract() || meatAttachment.getCount() > KoLCharacter.getAvailableMeat()) )
			{
				this.attachments.clear();
				this.usingStorage = true;
			}

			this.attachments.add( meatAttachment );
		}

		return this.attachments.toArray();
	}

	public void refreshContactList()
	{
		RequestThread.postRequest( new ContactListRequest() );
		this.recipientEntry.setModel( (SortedListModel) contactList.clone() );
	}
}
