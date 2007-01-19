/**
 * Copyright (c) 2005-2006, KoLmafia development team
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
import java.awt.Color;
import java.awt.Component;
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
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListModel;

import net.java.dev.spellcast.utilities.JComponentUtilities;
import net.java.dev.spellcast.utilities.LockableListModel;
import net.java.dev.spellcast.utilities.SortedListModel;

public abstract class ProposeTradeFrame extends KoLFrame
{
	// Source of meat & attachments

	public static boolean usingStorage = false;

	private String offerId;
	public JPanel messagePanel;
	public JComboBox recipientEntry;
	public JTextArea [] messageEntry;
	public JButton sendMessageButton;

	public ShowDescriptionList attachmentList;
	public JTextField attachedMeat;
	public LockableListModel attachments;

	public ProposeTradeFrame()
	{	this( "", null );
	}

	public ProposeTradeFrame( String recipient )
	{	this( recipient, null );
	}

	public ProposeTradeFrame( String recipient, String offerId )
	{
		super( "Send a Trade Proposal" );

		usingStorage = false;
		JPanel mainPanel = new JPanel();
		mainPanel.setLayout( new BoxLayout( mainPanel, BoxLayout.Y_AXIS ) );

		JPanel centerPanel = new JPanel( new BorderLayout( 5, 5 ) );
		centerPanel.add( Box.createHorizontalStrut( 40 ), BorderLayout.CENTER );

		centerPanel.add( constructWestPanel(), BorderLayout.WEST );

		if ( !recipient.equals( "" ) )
		{
			recipientEntry.addItem( recipient );
			recipientEntry.getEditor().setItem( recipient );
			recipientEntry.setSelectedItem( recipient );
		}

		JPanel attachmentPanel = new JPanel( new BorderLayout( 10, 10 ) );
		JPanel enclosePanel = new JPanel( new BorderLayout() );
		enclosePanel.add( new JLabel( "Enclose these items:    " ), BorderLayout.WEST );

		JButton attachButton = new InvocationButton( "Attach Items", "icon_plus.gif", this, "attachItems" );
		JComponentUtilities.setComponentSize( attachButton, 20, 20 );
		enclosePanel.add( attachButton, BorderLayout.EAST );

		attachmentPanel.add( enclosePanel, BorderLayout.NORTH );

		this.attachments = new LockableListModel();
		this.attachmentList = new ShowDescriptionList( attachments );

		SimpleScrollPane attachmentArea = new SimpleScrollPane( attachmentList );
		attachmentPanel.add( attachmentArea, BorderLayout.CENTER );

		JPanel meatPanel = new JPanel();
		meatPanel.setLayout( new BoxLayout( meatPanel, BoxLayout.X_AXIS ) );

		attachedMeat = new JTextField( "0" );
		JComponentUtilities.setComponentSize( attachedMeat, 120, 24 );
		meatPanel.add( Box.createHorizontalStrut( 40 ) );
		meatPanel.add( new JLabel( "Enclose meat:    " ) );
		meatPanel.add( attachedMeat );
		meatPanel.add( Box.createHorizontalStrut( 40 ) );

		attachmentPanel.add( meatPanel, BorderLayout.SOUTH );

		centerPanel.add( attachmentPanel, BorderLayout.EAST );

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

		this.offerId = offerId;

		if ( this.offerId != null )
			recipientEntry.setEnabled( false );
	}

	public JPanel constructWestPanel()
	{
		String [] entryHeaders = getEntryHeaders();

		recipientEntry = new MutableComboBox( (SortedListModel) contactList.clone(), true );
		recipientEntry.setEditable( true );

		JComponentUtilities.setComponentSize( recipientEntry, 300, 20 );

		messageEntry = new JTextArea[ entryHeaders.length ];
		SimpleScrollPane [] scrollArea = new SimpleScrollPane[ entryHeaders.length ];

		for ( int i = 0; i < messageEntry.length; ++i )
		{
			messageEntry[i] = new JTextArea();

			messageEntry[i].setFont( DEFAULT_FONT );
			messageEntry[i].setRows( 7 );
			messageEntry[i].setLineWrap( true );
			messageEntry[i].setWrapStyleWord( true );
			scrollArea[i] = new SimpleScrollPane( messageEntry[i] );
		}

		JPanel recipientPanel = new JPanel();
		recipientPanel.setLayout( new BoxLayout( recipientPanel, BoxLayout.Y_AXIS ) );
		recipientPanel.add( getLabelPanel( "Send to this person:" ) );
		recipientPanel.add( Box.createVerticalStrut( 4 ) );

		JPanel contactsPanel = new JPanel( new BorderLayout() );
		contactsPanel.add( recipientEntry, BorderLayout.CENTER );

		JButton refreshButton = new InvocationButton( "Refresh contact list", "reload.gif", this, "refreshContactList" );
		JComponentUtilities.setComponentSize( refreshButton, 20, 20 );
		contactsPanel.add( refreshButton, BorderLayout.EAST );
		recipientPanel.add( contactsPanel );
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

		JPanel entryPanel = new JPanel( new GridLayout( entryHeaders.length, 1 ) );

		JPanel holderPanel;
		for ( int i = 0; i < entryHeaders.length; ++i )
		{
			holderPanel = new JPanel( new BorderLayout( 5, 5 ) );
			holderPanel.add( getLabelPanel( entryHeaders[i] ), BorderLayout.NORTH );
			holderPanel.add( scrollArea[i], BorderLayout.CENTER );

			entryPanel.add( holderPanel );
		}

		JPanel westPanel = new JPanel( new BorderLayout() );
		westPanel.add( recipientPanel, BorderLayout.NORTH );
		westPanel.add( entryPanel, BorderLayout.CENTER );

		return westPanel;
	}

	public String [] getEntryHeaders()
	{	return new String[] { "Send this note:" };
	}

	public String [] getWestHeaders()
	{	return new String[0];
	}

	public Component [] getWestComponents()
	{	return new Component[0];
	}

	public JPanel getLabelPanel( String text )
	{
		JPanel label = new JPanel( new GridLayout( 1, 1 ) );
		label.add( new JLabel( text, JLabel.LEFT ) );
		return label;
	}

	public boolean sendMessage( String recipient, String [] messages )
	{
		// Close all pending trades frames first

		KoLFrame [] frames = StaticEntity.getExistingFrames();
		for ( int i = 0; i < frames.length; ++i )
			if ( frames[i] instanceof PendingTradesFrame )
				((PendingTradesFrame)frames[i]).dispose();

		// Send the offer / response

		if ( offerId != null )
			RequestThread.postRequest( new ProposeTradeRequest( StaticEntity.parseInt( offerId ), messages[0], getAttachedItems() ) );

		Object [] parameters = new Object[1];
		parameters[0] = offerId != null ? new ProposeTradeRequest() :
			new ProposeTradeRequest( recipient, messages[0], getAttachedItems() );

		createDisplay( PendingTradesFrame.class, parameters );
		return true;
	}

	private class SendMessageListener extends ThreadedListener
	{
		public void run()
		{
			String [] recipients = StaticEntity.getClient().extractTargets( (String) recipientEntry.getSelectedItem() );

			// Limit the number of messages which can be sent
			// to just eleven, as was the case with KoLmelion.

			if ( recipients.length > 11 )
			{
				KoLmafia.updateDisplay( ERROR_STATE, "Maximum number of users exceeded." );
				return;
			}

			String [] messages = new String[ messageEntry.length ];

			for ( int i = 0; i < messageEntry.length; ++i )
				messages[i] = messageEntry[i].getText();

			// Send the message to all recipients on the list.
			// If one of them fails, however, immediately stop
			// and notify the user that there was failure.

			for ( int i = 0; i < recipients.length && KoLmafia.permitsContinue(); ++i )
				if ( !sendMessage( recipients[i], messages ) )
					return;

			if ( KoLmafia.permitsContinue() )
			{
				recipientEntry.setSelectedIndex( -1 );
				dispose();
			}
		}
	}

	public void attachItems()
	{
		Object [] parameters = new Object[2];
		parameters[0] = inventory;
		parameters[1] = attachments;

		createDisplay( AttachmentFrame.class, parameters );
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

		if ( recipientEntry == null )
			return;

		super.setEnabled( isEnabled );

		if ( this.offerId != null )
			recipientEntry.setEnabled( false );

		sendMessageButton.setEnabled( isEnabled );
	}

	public Object [] getAttachedItems()
	{
		int meatAttachment = getValue( attachedMeat );
		if ( meatAttachment > 0 )
			attachments.add( new AdventureResult( AdventureResult.MEAT, meatAttachment ) );

		return attachments.toArray();
	}

	public void dispose()
	{
		messagePanel = null;
		recipientEntry = null;
		messageEntry = null;
		sendMessageButton = null;

		attachmentList = null;
		attachedMeat = null;
		attachments = null;

		// Search out the attachment frame which is
		// associated with this send message frame
		// and make sure it gets disposed.

		KoLFrame [] frames = StaticEntity.getExistingFrames();
		for ( int i = frames.length - 1; i >= 0; --i )
			if ( frames[i] instanceof AttachmentFrame && ((AttachmentFrame)frames[i]).attachments == attachments )
				((AttachmentFrame)frames[i]).dispose();

		frames = null;
		super.dispose();
	}

	public void refreshContactList()
	{
		RequestThread.postRequest( new ContactListRequest() );
		recipientEntry.setModel( (SortedListModel) contactList.clone() );
	}

	/**
	 * Internal frame used to handle attachments.  This frame
	 * appears whenever the user wishes to add non-meat attachments
	 * to the message.
	 */

	public static class AttachmentFrame extends KoLFrame
	{
		private ShowDescriptionList newAttachments;
		private LockableListModel available, attachments;

		public AttachmentFrame( LockableListModel available, LockableListModel attachments )
		{
			super( "Attachments" );

			this.available = (LockableListModel) available.clone();
			this.attachments = attachments;
			this.newAttachments = new ShowDescriptionList( this.attachments );
			this.newAttachments.setVisibleRowCount( 16 );

			SimpleScrollPane attachmentArea = new SimpleScrollPane( newAttachments );

			JPanel labeledArea = new JPanel( new BorderLayout() );
			labeledArea.add( JComponentUtilities.createLabel( "Currently Attached", JLabel.CENTER, Color.black, Color.white ), BorderLayout.NORTH );
			labeledArea.add( attachmentArea, BorderLayout.CENTER );

			JPanel eastPanel = new JPanel( new CardLayout( 10, 10 ) );
			eastPanel.add( labeledArea, "" );

			framePanel.setLayout( new BorderLayout() );
			framePanel.add( new SourcePanel(), BorderLayout.CENTER );
			framePanel.add( eastPanel, BorderLayout.EAST );
		}

		private class SourcePanel extends ItemManagePanel
		{
			private JComboBox sourceSelect;
			private LockableListModel source;

			private SourcePanel()
			{
				super( storage == null ? "Inside Inventory" : "", " > > > ", " < < < ", available );
				source = available;

				sourceSelect.addActionListener( new SourceChangeListener() );

				// Remove items from our cloned list that are
				// already on the attachments list
				for ( int i = 0; i < attachments.size(); ++i )
					AdventureResult.addResultToList( source, ((AdventureResult)attachments.get( i )).getNegation() );
			}

			private class SourceChangeListener implements ActionListener
			{
				public void actionPerformed( ActionEvent e )
				{
					if ( source == available )
						return;

					// Put back old attachments
					usingStorage = (available == storage);

					while ( !attachments.isEmpty() )
						AdventureResult.addResultToList( source, (AdventureResult) attachments.remove( 0 ) );

					source = available;
					elementList.setModel( source );
				}
			}

			public void actionConfirmed()
			{
				Object [] items = elementList.getSelectedValues();

				for ( int i = 0; i < items.length; ++i )
				{
					AdventureResult currentItem = (AdventureResult) items[i];

					// Skip filtered items
					if ( !TradeableItemDatabase.isTradeable( currentItem.getItemId() ) )
						continue;

					int attachmentCount = getQuantity( "Attaching " + currentItem.getName() + "...",
									   currentItem.getCount( source ) );
					if ( attachmentCount == 0 )
						continue;

					currentItem = currentItem.getInstance( attachmentCount );
					AdventureResult.addResultToList( attachments, currentItem );
					AdventureResult.addResultToList( source, currentItem.getNegation() );
				}
			}

			public void actionCancelled()
			{
				Object [] items = newAttachments.getSelectedValues();

				for ( int i = 0; i < items.length; ++i )
				{
					AdventureResult.addResultToList( attachments, ((AdventureResult) items[i]).getNegation() );
					AdventureResult.addResultToList( source, (AdventureResult) items[i] );
				}
			}
		}
	}
}
