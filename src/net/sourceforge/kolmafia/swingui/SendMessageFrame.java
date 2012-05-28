/**
 * Copyright (c) 2005-2012, KoLmafia development team
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

package net.sourceforge.kolmafia.swingui;

import com.sun.java.forums.SpringUtilities;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.GridLayout;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.SpringLayout;
import javax.swing.SwingConstants;

import net.java.dev.spellcast.utilities.JComponentUtilities;
import net.java.dev.spellcast.utilities.LockableListModel;
import net.java.dev.spellcast.utilities.LockableListModel.ListElementFilter;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.StaticEntity;

import net.sourceforge.kolmafia.persistence.ItemDatabase;

import net.sourceforge.kolmafia.session.ContactManager;

import net.sourceforge.kolmafia.swingui.button.InvocationButton;

import net.sourceforge.kolmafia.swingui.widget.AutoFilterComboBox;
import net.sourceforge.kolmafia.swingui.widget.AutoHighlightTextField;
import net.sourceforge.kolmafia.swingui.widget.GenericScrollPane;

import net.sourceforge.kolmafia.textui.command.SendMessageCommand;

import net.sourceforge.kolmafia.utilities.InputFieldUtilities;

public class SendMessageFrame
	extends GenericFrame
	implements ListElementFilter
{
	private boolean isStorage = false;

	private final JComboBox sourceSelect;
	private final LockableListModel contacts;
	private final AutoFilterComboBox recipientEntry;

	private final LockableListModel attachments;
	private final JList attachmentsList;
	private final AutoHighlightTextField attachedMeat;
	private final JTextArea messageEntry;

	public SendMessageFrame( final String recipient )
	{
		this();
		this.setRecipient( recipient );
	}

	public SendMessageFrame( final String recipient, final String text )
	{
		this();
		this.setRecipient( recipient );
		this.setMessageText( text );
	}

	public SendMessageFrame()
	{
		super( "Send a Message" );
		JPanel mainPanel = new JPanel( new SpringLayout() );

		// What kind of package you want to send.

		this.sourceSelect = new JComboBox();
		this.sourceSelect.addItem( "Send items/meat from inventory" );
		this.sourceSelect.addItem( "Send items/meat from ancestral storage" );
		this.sourceSelect.addActionListener( new AttachmentClearListener() );

		// Who you want to send it to.

		this.contacts = (LockableListModel) ContactManager.getMailContacts().getMirrorImage();
		this.recipientEntry = new AutoFilterComboBox( this.contacts, true );

		// How much you want to attach, in raw terms.

		this.attachments = new LockableListModel();
		this.attachedMeat = new AutoHighlightTextField( "0" );

		// Now, layout the center part of the panel.

		mainPanel.add( new JLabel( "Source:  ", SwingConstants.RIGHT ) );
		mainPanel.add( this.sourceSelect );
		mainPanel.add( new JLabel( "Recipient:  ", SwingConstants.RIGHT ) );
		mainPanel.add( this.recipientEntry );
		mainPanel.add( new JLabel( "Send Meat:  ", SwingConstants.RIGHT ) );
		mainPanel.add( this.attachedMeat );
		SpringUtilities.makeCompactGrid( mainPanel, 3, 2, 5, 5, 5, 5 );

		// Construct the east container.

		JButton attach = new InvocationButton( "Attach an item", "icon_plus.gif", this, "attachItem" );
		JComponentUtilities.setComponentSize( attach, 15, 15 );

		JButton detach = new InvocationButton( "Remove selected items", "icon_minus.gif", this, "detachItems" );
		JComponentUtilities.setComponentSize( detach, 15, 15 );

		JPanel buttonPanel = new JPanel( new GridLayout( 1, 2, 0, 0 ) );
		buttonPanel.add( attach );
		buttonPanel.add( detach );

		JPanel labelPanel = new JPanel( new BorderLayout( 5, 5 ) );
		labelPanel.add( buttonPanel, BorderLayout.WEST );
		labelPanel.add( new JLabel( "Attach or detach items", SwingConstants.LEFT ), BorderLayout.CENTER );

		GenericScrollPane pane = new GenericScrollPane( this.attachments, 3 );
		this.attachmentsList = (JList) pane.getComponent();
		this.attachmentsList.addKeyListener( new RemoveAttachmentListener() );

		JPanel attachPanel = new JPanel( new BorderLayout( 5, 5 ) );
		attachPanel.add( labelPanel, BorderLayout.NORTH );
		attachPanel.add( pane, BorderLayout.CENTER );

		JPanel northPanel = new JPanel( new BorderLayout( 20, 20 ) );
		northPanel.add( mainPanel, BorderLayout.CENTER );
		northPanel.add( attachPanel, BorderLayout.EAST );

		JPanel mainHolder = new JPanel( new BorderLayout( 20, 20 ) );
		mainHolder.add( northPanel, BorderLayout.NORTH );

		// Add the message entry to the panel.

		this.messageEntry = new JTextArea();
		this.messageEntry.setFont( KoLConstants.DEFAULT_FONT );
		this.messageEntry.setRows( 7 );
		this.messageEntry.setLineWrap( true );
		this.messageEntry.setWrapStyleWord( true );

		GenericScrollPane scrollArea = new GenericScrollPane( this.messageEntry );
		mainHolder.add( scrollArea, BorderLayout.CENTER );

		// Add a button to the bottom panel.

		JPanel sendPanel = new JPanel();
		sendPanel.add( new InvocationButton( "send message", this, "sendMessage" ) );
		mainHolder.add( sendPanel, BorderLayout.SOUTH );

		// Layout the major container.

		JPanel cardPanel = new JPanel( new CardLayout( 10, 10 ) );
		cardPanel.add( mainHolder, "" );

		this.setCenterComponent( mainHolder );
	}

	public void createItemAttachPanel()
	{

	}

	public void createMessagePanel()
	{

	}

	public void createRecipientPanel()
	{

	}

	public void setRecipient( String recipient )
	{
		this.isStorage = false;
		this.sourceSelect.setSelectedIndex( 0 );

		if ( !this.contacts.contains( recipient ) )
		{
			recipient = ContactManager.getPlayerName( recipient );
			this.contacts.add( 0, recipient );
		}

		this.recipientEntry.getEditor().setItem( recipient );
		this.recipientEntry.setSelectedItem( recipient );

		this.attachments.clear();
		this.attachedMeat.setText( "" );
		this.messageEntry.setText( "" );
	}

	public void setMessageText( String text )
	{
		this.messageEntry.setText( text );
	}

	@Override
	public boolean shouldAddStatusBar()
	{
		return true;
	}

	private class AttachmentClearListener
		implements ActionListener
	{
		public void actionPerformed( final ActionEvent e )
		{
			boolean wasStorage = SendMessageFrame.this.isStorage;
			SendMessageFrame.this.isStorage = SendMessageFrame.this.sourceSelect.getSelectedIndex() == 1;

			if ( SendMessageFrame.this.isStorage != wasStorage )
			{
				SendMessageFrame.this.attachments.clear();
			}
		}
	}

	public void sendMessage()
	{
		Object[] attachmentsArray = new Object[ this.attachments.size() + 1 ];
		this.attachments.toArray( attachmentsArray );

		attachmentsArray[ this.attachments.size() ] =
			new AdventureResult( AdventureResult.MEAT, InputFieldUtilities.getValue( this.attachedMeat, 0 ) );

		String[] recipients = StaticEntity.getClient().extractTargets( (String) this.recipientEntry.getSelectedItem() );

		for ( int i = 0; i < recipients.length; ++i )
		{
			SendMessageCommand.send(
				recipients[ i ], this.messageEntry.getText(), attachmentsArray, this.isStorage, false );
		}
	}

	public boolean isVisible( Object o )
	{
		if ( !( o instanceof AdventureResult ) )
		{
			return false;
		}

		AdventureResult ar = (AdventureResult) o;

		if ( !( ar.isItem() ) )
		{
			return false;
		}

		return ItemDatabase.isGiftable( ar.getItemId() );
	}

	public void attachItem()
	{
		LockableListModel source = this.isStorage ? KoLConstants.storage : KoLConstants.inventory;
		if ( source.isEmpty() )
		{
			return;
		}

		source = source.getMirrorImage();

		int tradeableItemCount = source.getSize();

		AdventureResult current;
		Object[] values = InputFieldUtilities.multiple( "What would you like to send?", source, this );

		if ( values.length < tradeableItemCount )
		{
			for ( int i = 0; i < values.length; ++i )
			{
				current = (AdventureResult) values[ i ];
				Integer value = InputFieldUtilities.getQuantity( "How many " + current.getName() + " to send?", current.getCount() );
				int amount = ( value == null ) ? 0 : value.intValue();

				if ( amount <= 0 )
				{
					values[ i ] = null;
				}
				else
				{
					values[ i ] = current.getInstance( amount );
				}
			}
		}

		for ( int i = 0; i < values.length; ++i )
		{
			if ( values[ i ] != null )
			{
				this.attachments.add( values[ i ] );
			}
		}
	}

	public void detachItems()
	{
		JList list = this.attachmentsList;
		int [] indices = list.getSelectedIndices();
		for ( int i = indices.length; i > 0; --i )
		{
			int index = indices[ i - 1 ];
			this.attachments.remove( index );
		}
	}

	private class RemoveAttachmentListener
		extends KeyAdapter
	{
		@Override
		public void keyReleased( final KeyEvent e )
		{
			if ( e.isConsumed() )
			{
				return;
			}

			if ( e.getKeyCode() != KeyEvent.VK_DELETE && e.getKeyCode() != KeyEvent.VK_BACK_SPACE )
			{
				return;
			}

			SendMessageFrame.this.detachItems();

			e.consume();
		}
	}
}
