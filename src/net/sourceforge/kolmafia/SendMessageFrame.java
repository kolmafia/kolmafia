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
import javax.swing.JMenuItem;
import javax.swing.Box;
import javax.swing.JLabel;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JFrame;
import javax.swing.JTextField;
import javax.swing.JTextArea;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JOptionPane;
import javax.swing.JTabbedPane;

// other imports
import java.util.List;
import java.util.ArrayList;
import net.java.dev.spellcast.utilities.LockableListModel;
import net.java.dev.spellcast.utilities.SortedListModel;
import net.java.dev.spellcast.utilities.JComponentUtilities;

public class SendMessageFrame extends KoLFrame
{
	protected boolean usingStorage;
	protected JPanel messagePanel;
	protected JComboBox recipientEntry;
	protected JTextArea messageEntry;
	protected JButton sendMessageButton;

	protected LockableListModel messageTypes;
	protected JTextField attachedMeat;
	protected SortedListModel attachments;

	protected ItemManagePanel attachmentPanel;
	protected ItemManagePanel inventoryPanel;
	protected ItemManagePanel storagePanel;

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

		centerPanel.add( constructWestPanel(), BorderLayout.WEST );

		if ( !recipient.equals( "" ) )
		{
			recipientEntry.addItem( recipient );
			recipientEntry.getEditor().setItem( recipient );
			recipientEntry.setSelectedItem( recipient );
		}

		centerPanel.add( constructAttachmentPanel(), BorderLayout.CENTER );

		mainPanel.add( centerPanel );
		mainPanel.add( Box.createVerticalStrut( 18 ) );

		sendMessageButton = new JButton( "Send Message" );
		sendMessageButton.addActionListener( new SendMessageListener() );

		JPanel sendMessageButtonPanel = new JPanel();
		sendMessageButtonPanel.add( sendMessageButton );

		mainPanel.add( sendMessageButtonPanel );
		mainPanel.add( Box.createVerticalStrut( 4 ) );

		messagePanel = new JPanel( new BorderLayout() );
		messagePanel.add( mainPanel, BorderLayout.CENTER );

		framePanel.setLayout( new CardLayout( 20, 20 ) );
		framePanel.add( messagePanel, "" );
	}

	protected JTabbedPane constructAttachmentPanel()
	{
		JTabbedPane attachTabs = new JTabbedPane();

		// Add in the attachments section

		attachments = new SortedListModel();
		attachmentPanel = new ItemManagePanel( attachments );
		attachmentPanel.elementList.setVisibleRowCount( 11 );
		attachmentPanel.setButtons( null );

		JPanel meatPanel = new JPanel();
		meatPanel.setLayout( new BoxLayout( meatPanel, BoxLayout.X_AXIS ) );

		attachedMeat = new JTextField( "0" );
		JComponentUtilities.setComponentSize( attachedMeat, 120, 24 );
		meatPanel.add( Box.createHorizontalStrut( 40 ) );
		meatPanel.add( new JLabel( "Enclose meat:    " ) );
		meatPanel.add( attachedMeat );
		meatPanel.add( Box.createHorizontalStrut( 40 ) );

		attachmentPanel.actualPanel.add( meatPanel, BorderLayout.SOUTH );
		attachmentPanel.elementList.contextMenu.add( new RemoveAttachmentMenuItem() );

		attachTabs.addTab( "Attachments", attachmentPanel );

		// Add in the inventory panel

		inventoryPanel = new ItemManagePanel( (SortedListModel) inventory.clone() );
		inventoryPanel.elementList.setVisibleRowCount( 11 );
		inventoryPanel.setButtons( null );
		attachTabs.addTab( "Inventory", inventoryPanel );

		inventoryPanel.elementList.contextMenu.add( new AddAttachmentMenuItem( inventoryPanel ) );

		// Add in the storage panel

		storagePanel = new ItemManagePanel( (SortedListModel) storage.clone() );
		storagePanel.elementList.setVisibleRowCount( 11 );
		storagePanel.setButtons( null );
		attachTabs.addTab( "In Storage", storagePanel );

		storagePanel.elementList.contextMenu.add( new AddAttachmentMenuItem( storagePanel ) );

		return attachTabs;
	}

	protected JPanel constructWestPanel()
	{
		// The kind of box you're sending it with.

		messageTypes = new LockableListModel();
		messageTypes.add( "Send it by kingdom mail for FREE" );
		messageTypes.addAll( GiftMessageRequest.getPackages() );

		JComboBox typeComboBox = new JComboBox( messageTypes );
		typeComboBox.setSelectedIndex(0);

		JComponentUtilities.setComponentSize( typeComboBox, 320, 20 );

		JPanel overviewPanel = new JPanel();
		overviewPanel.setLayout( new BoxLayout( overviewPanel, BoxLayout.Y_AXIS ) );
		overviewPanel.add( getLabelPanel( "I'd like to use this sending option: " ) );
		overviewPanel.add( Box.createVerticalStrut( 4 ) );
		overviewPanel.add( typeComboBox );
		overviewPanel.add( Box.createVerticalStrut( 20 ) );

		// Also who you want to send it to.

		recipientEntry = new MutableComboBox( (SortedListModel) contactList.clone(), true );
		recipientEntry.setEditable( true );

		JComponentUtilities.setComponentSize( recipientEntry, 300, 20 );

		overviewPanel.add( getLabelPanel( "I intend to send it to this person:" ) );
		overviewPanel.add( Box.createVerticalStrut( 4 ) );

		JPanel contactsPanel = new JPanel( new BorderLayout() );
		contactsPanel.add( recipientEntry, BorderLayout.CENTER );

		JButton refreshButton = new InvocationButton( "Refresh contact list", "reload.gif", this, "refreshContactList" );
		JComponentUtilities.setComponentSize( refreshButton, 20, 20 );
		contactsPanel.add( refreshButton, BorderLayout.EAST );
		overviewPanel.add( contactsPanel );
		overviewPanel.add( Box.createVerticalStrut( 20 ) );

		// And the message entry area.

		messageEntry = new JTextArea();
		messageEntry.setFont( DEFAULT_FONT );
		messageEntry.setRows( 7 );
		messageEntry.setLineWrap( true );
		messageEntry.setWrapStyleWord( true );
		SimpleScrollPane scrollArea = new SimpleScrollPane( messageEntry );

		JPanel entryPanel = new JPanel( new BorderLayout( 5, 5 ) );
		entryPanel.add( getLabelPanel( "I'd like them to see this message: " ), BorderLayout.NORTH );
		entryPanel.add( scrollArea, BorderLayout.CENTER );

		JPanel westPanel = new JPanel( new BorderLayout() );
		westPanel.add( overviewPanel, BorderLayout.NORTH );
		westPanel.add( entryPanel, BorderLayout.CENTER );

		return westPanel;
	}

	protected JPanel getLabelPanel( String text )
	{
		JPanel label = new JPanel( new GridLayout( 1, 1 ) );
		label.add( new JLabel( text, JLabel.LEFT ) );
		return label;
	}

	protected boolean sendMessage( String recipient, String message )
	{
		KoLmafia.forceContinue();

		if ( messageTypes.getSelectedIndex() == 0 )
		{
			setEnabled( false );
			(new GreenMessageRequest( recipient, message, getAttachedItems() )).run();
			setEnabled( true );

			if ( !SendMessageRequest.hadSendMessageFailure() )
			{
				KoLmafia.updateDisplay( "Message sent to " + recipient );
				setTitle( "Message sent to " + recipient );
				return true;
			}
			else
			{
				KoLmafia.updateDisplay( ERROR_STATE, "Failed to send message to " + recipient );
				setTitle( "Failed to send message to " + recipient );
				return false;
			}
		}
		else
		{
			setEnabled( false );
			(new GiftMessageRequest( recipient, "It's a secret to everybody.", message,
				messageTypes.getSelectedIndex() - 1, getAttachedItems(), usingStorage )).run();

			setEnabled( true );

			if ( !SendMessageRequest.hadSendMessageFailure() )
			{
				KoLmafia.updateDisplay( "Gift sent to " + recipient );
				setTitle( "Gift sent to " + recipient );
				return true;
			}
			else
			{
				KoLmafia.updateDisplay( ERROR_STATE, "Failed to send gift to " + recipient );
				setTitle( "Failed to send gift to " + recipient );
				return false;
			}
		}
	}

	private abstract class ThreadedMenuItem extends JMenuItem implements ActionListener, Runnable
	{
		protected ItemManagePanel elementPanel;

		public ThreadedMenuItem( String title, ItemManagePanel elementPanel )
		{
			super( title );
			addActionListener( this );
			this.elementPanel = elementPanel;
		}

		public void actionPerformed( ActionEvent e )
		{	(new Thread( this )).start();
		}
	}

	private class AddAttachmentMenuItem extends ThreadedMenuItem
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
				for ( int i = 0; i < attachments.size(); ++i )
					AdventureResult.addResultToList( inventoryPanel.elementModel, (AdventureResult) attachments.get(i) );

				attachments.clear();
			}
			else if ( usingStorage && elementPanel != storagePanel )
			{
				usingStorage = false;
				for ( int i = 0; i < attachments.size(); ++i )
					AdventureResult.addResultToList( storagePanel.elementModel, (AdventureResult) attachments.get(i) );

				attachments.clear();
			}

			for ( int i = 0; i < items.length; ++i )
			{
				AdventureResult.addResultToList( attachments, items[i] );
				AdventureResult.addResultToList( elementPanel.elementModel, items[i].getNegation() );
			}
		}
	}

	private class RemoveAttachmentMenuItem extends ThreadedMenuItem
	{
		public RemoveAttachmentMenuItem()
		{	super( "Remove attachment", attachmentPanel );
		}

		public void run()
		{
			Object [] items = attachmentPanel.elementList.getSelectedValues();
			if ( items == null || items.length == 0 )
				return;

			for ( int i = 0; i < items.length; ++i )
			{
				attachments.remove( items[i] );
				AdventureResult.addResultToList( usingStorage ? storagePanel.elementModel : inventoryPanel.elementModel, (AdventureResult) items[i] );
			}
		}
	}

	private class SendMessageListener implements ActionListener, Runnable
	{
		public void actionPerformed( ActionEvent e )
		{	(new Thread( this )).start();
		}

		public void run()
		{
			KoLmafia.forceContinue();

			String [] recipients = StaticEntity.getClient().extractTargets( (String) recipientEntry.getSelectedItem() );
			if ( recipients.length == 0 || recipients[0].equals( "" ) )
			{
				KoLmafia.updateDisplay( "You didn't specify someone to send to." );
				KoLmafia.enableDisplay();
				return;
			}

			// Limit the number of messages which can be sent
			// to just eleven, as was the case with KoLmelion.

			if ( recipients.length > 11 )
			{
				KoLmafia.updateDisplay( ERROR_STATE, "Maximum number of users exceeded." );
				KoLmafia.enableDisplay();
				return;
			}

			String message = messageEntry.getText();

			// Send the message to all recipients on the list.
			// If one of them fails, however, immediately stop
			// and notify the user that there was failure.

			for ( int i = 0; i < recipients.length && KoLmafia.permitsContinue(); ++i )
				if ( !sendMessage( recipients[i], message ) )
					return;

			KoLmafia.enableDisplay();
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

	protected Object [] getAttachedItems()
	{
		int meatAttachment = getValue( attachedMeat );
		if ( meatAttachment > 0 )
			attachments.add( new AdventureResult( AdventureResult.MEAT, meatAttachment ) );

		return attachments.toArray();
	}

	public void refreshContactList()
	{
		(new ContactListRequest()).run();
		recipientEntry.setModel( (SortedListModel) contactList.clone() );
	}
}
