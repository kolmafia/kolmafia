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

import java.awt.Component;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JDialog;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JToolBar;

import net.java.dev.spellcast.utilities.JComponentUtilities;
import net.java.dev.spellcast.utilities.LockableListModel;

import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.chat.ChatManager;
import net.sourceforge.kolmafia.session.ContactManager;
import net.sourceforge.kolmafia.swingui.button.InvocationButton;
import net.sourceforge.kolmafia.swingui.widget.GenericScrollPane;
import net.sourceforge.kolmafia.utilities.HTMLListEntry;

public class ContactListFrame
	extends GenericFrame
{
	private LockableListModel contacts;
	private JList contactsDisplay;

	public ContactListFrame()
	{
		this( ContactManager.getMailContacts() );
	}

	public ContactListFrame( final LockableListModel contacts )
	{
		super( "Contact List" );

		this.contacts = contacts;

		this.contactsDisplay = new JList( contacts );
		this.contactsDisplay.setVisibleRowCount( 25 );
		this.contactsDisplay.setPrototypeCellValue( "ABCDEFGHIJKLMNOPQRSTUVWXYZ" );
		this.contactsDisplay.addMouseListener( new SendInstantMessageAdapter() );

		this.setCenterComponent( new ContactListPanel( this.contactsDisplay ) );
		this.getToolbar();

		this.setSize( 200, 200 );
	}

	public JToolBar getToolbar()
	{
		JToolBar toolbarPanel = super.getToolbar( true );
		toolbarPanel.add( new InvocationButton( "Show as list", "copy.gif", this, "listSelected" ) );
		toolbarPanel.add( new InvocationButton( "Mass-buff", "buff.gif", this, "buffSelected" ) );
		toolbarPanel.add( new InvocationButton( "Mass-mail", "mail.gif", this, "mailSelected" ) );

		return toolbarPanel;
	}

	public Component getCenterComponent()
	{
		return this.getFramePanel();
	}

	public JTabbedPane getTabbedPane()
	{
		return null;
	}

	public String[] getSelectedPlayers()
	{
		Object[] selectedValues = this.contactsDisplay.getSelectedValues();

		// If no players are selected, and the player uses the
		// option, assume they want everyone.

		if ( selectedValues.length == 0 )
		{
			selectedValues = this.contacts.toArray();
		}

		String[] selectedPlayers = new String[ selectedValues.length ];

		for ( int i = 0; i < selectedPlayers.length; ++i )
		{
			selectedPlayers[ i ] = getContactName( selectedValues[ i ] );
		}

		return selectedPlayers;
	}

	public String convertToCDL()
	{
		StringBuffer listCDL = new StringBuffer();
		String[] selectedPlayers = this.getSelectedPlayers();

		for ( int i = 0; i < selectedPlayers.length; ++i )
		{
			if ( i != 0 )
			{
				listCDL.append( ", " );
			}

			listCDL.append( selectedPlayers[ i ] );
		}

		return listCDL.toString();
	}

	public void listSelected()
	{
		JDialog dialogCDL = new JDialog( (java.awt.Frame) null, "Here's your CDL!" );
		JTextArea entryCDL = new JTextArea();

		entryCDL.setFont( KoLConstants.DEFAULT_FONT );
		entryCDL.setLineWrap( true );
		entryCDL.setWrapStyleWord( true );

		GenericScrollPane scrollCDL = new GenericScrollPane( entryCDL );
		JComponentUtilities.setComponentSize( scrollCDL, 250, 120 );
		dialogCDL.getContentPane().add( scrollCDL );

		entryCDL.setText( this.convertToCDL() );
		dialogCDL.pack();
		dialogCDL.setVisible( true );
	}

	public void buffSelected()
	{
		Object[] parameters = new Object[ 1 ];
		parameters[ 0 ] = this.convertToCDL();

		GenericFrame.createDisplay( SkillBuffFrame.class, parameters );
	}

	public void mailSelected()
	{
		// Make sure there's only eleven players
		// selected, since that's the kmail limit.

		Object[] parameters = new Object[ 1 ];
		parameters[ 0 ] = this.convertToCDL();

		GenericFrame.createDisplay( SendMessageFrame.class, parameters );
	}

	private class ContactListPanel
		extends JPanel
	{
		public ContactListPanel( JList contactsDisplay )
		{
			this.setLayout( new GridLayout( 1, 1 ) );
			this.add( new GenericScrollPane( contactsDisplay ) );
		}
	}

	private class SendInstantMessageAdapter
		extends MouseAdapter
	{
		public void mouseClicked( final MouseEvent e )
		{
			JList list = (JList) e.getSource();

			// The only event handled by the adapter is a double-click;
			// when a double-click is detected, a new ChatFrame is created
			// for the specified player.

			if ( e.getClickCount() == 2 )
			{
				int index = list.locationToIndex( e.getPoint() );

				if ( index >= 0 && index < ContactListFrame.this.contacts.size() )
				{
					String contact = getContactName( ContactListFrame.this.contacts.get( index ) );
					String bufferKey = ChatManager.getBufferKey( contact );

					ChatManager.openWindow( bufferKey, false );
				}
			}
		}
	}

	protected String getContactName( Object contact )
	{
		if ( contact instanceof HTMLListEntry )
		{
			return (String) ( (HTMLListEntry) contact ).getValue();
		}

		return (String) contact;
	}
}
