/**
 * Copyright (c) 2005-2008, KoLmafia development team
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

import java.awt.CardLayout;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JDialog;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JToolBar;
import javax.swing.WindowConstants;

import net.java.dev.spellcast.utilities.JComponentUtilities;
import net.java.dev.spellcast.utilities.SortedListModel;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.session.ChatManager;
import net.sourceforge.kolmafia.swingui.button.InvocationButton;
import net.sourceforge.kolmafia.swingui.widget.GenericScrollPane;

public class ContactListFrame
	extends GenericFrame
{
	private final SortedListModel contacts;
	private JList contactsDisplay;

	public ContactListFrame()
	{
		this( KoLConstants.contactList );
	}

	public ContactListFrame( final SortedListModel contacts )
	{
		super( "Contact List" );
		this.contacts = contacts;

		this.framePanel.setLayout( new CardLayout( 10, 10 ) );
		this.framePanel.add( new ContactListPanel(), "" );

		JToolBar toolbarPanel = this.getToolbar();
		if ( toolbarPanel != null )
		{
			toolbarPanel.add( new InvocationButton( "Show as list", "copy.gif", this, "listSelected" ) );
			toolbarPanel.add( new InvocationButton( "Mass-buff", "buff.gif", this, "buffSelected" ) );
			toolbarPanel.add( new InvocationButton( "Mass-mail", "mail.gif", this, "mailSelected" ) );
		}

		this.pack();
	}

	public JTabbedPane getTabbedPane()
	{
		return null;
	}

	public Object[] getSelectedPlayers()
	{
		Object[] selectedPlayers = this.contactsDisplay.getSelectedValues();

		// If no players are selected, and the player uses the
		// option, assume they want everyone.

		if ( selectedPlayers.length == 0 )
		{
			selectedPlayers = this.contacts.toArray();
		}

		return selectedPlayers;
	}

	public String convertToCDL()
	{
		StringBuffer listCDL = new StringBuffer();
		Object[] selectedPlayers = this.getSelectedPlayers();

		for ( int i = 0; i < selectedPlayers.length; ++i )
		{
			if ( i != 0 )
			{
				listCDL.append( ", " );
			}
			listCDL.append( (String) selectedPlayers[ i ] );
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
		public ContactListPanel()
		{
			this.setLayout( new GridLayout( 1, 1 ) );
			ContactListFrame.this.contactsDisplay = new JList( ContactListFrame.this.contacts );

			ContactListFrame.this.contactsDisplay.setVisibleRowCount( 25 );
			ContactListFrame.this.contactsDisplay.setPrototypeCellValue( "ABCDEFGHIJKLMNOPQRSTUVWXYZ" );

			ContactListFrame.this.contactsDisplay.addMouseListener( new SendInstantMessageAdapter() );
			this.add( new GenericScrollPane( ContactListFrame.this.contactsDisplay ) );
		}
	}

	private class SendInstantMessageAdapter
		extends MouseAdapter
	{
		public void mouseClicked( final MouseEvent e )
		{
			JList list = (JList) e.getSource();

			// The only event handled by the adapter is a float-click;
			// when a float-click is detected, a new ChatFrame is created
			// for the specified player.

			if ( e.getClickCount() == 2 )
			{
				int index = list.locationToIndex( e.getPoint() );

				if ( index >= 0 && index < ContactListFrame.this.contacts.size() )
				{
					ChatManager.openInstantMessage( (String) ContactListFrame.this.contacts.get( index ), true );
				}
			}
		}
	}
}
