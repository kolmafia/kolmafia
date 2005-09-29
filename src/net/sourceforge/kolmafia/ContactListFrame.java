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

import java.awt.Color;
import java.awt.CardLayout;
import java.awt.GridLayout;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JList;
import javax.swing.JMenuBar;
import javax.swing.JMenu;
import javax.swing.ButtonGroup;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.JOptionPane;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.KeyEvent;
import javax.swing.SwingUtilities;

import net.java.dev.spellcast.utilities.SortedListModel;
import net.java.dev.spellcast.utilities.JComponentUtilities;

/**
 * An extension of a generic <code>JFrame</code> which contains the buddy list
 * for this KoLmafia session.
 */

public class ContactListFrame extends KoLFrame
{
	private SortedListModel contacts;
	private JList contactsDisplay;

	protected ButtonGroup clickGroup;
	protected JRadioButtonMenuItem [] clickOptions;

	public ContactListFrame( KoLmafia client, SortedListModel contacts )
	{
		super( client, "Contact List" );
		this.contacts = contacts;
		getContentPane().setLayout( new CardLayout( 10, 10 ) );
		getContentPane().add( new ContactListPanel(), "" );

		setDefaultCloseOperation( HIDE_ON_CLOSE );
		addMenuBar();  pack();
	}

	public void setEnabled( boolean isEnabled )
	{
	}

	public boolean isEnabled()
	{	return true;
	}

	private void addMenuBar()
	{
		JMenuBar menuBar = new JMenuBar();
		this.setJMenuBar( menuBar );

		JMenu clicksMenu = new JMenu( "Namelinks" );
		clicksMenu.setMnemonic( KeyEvent.VK_N );
		menuBar.add( clicksMenu );

		clickGroup = new ButtonGroup();
		clickOptions = new JRadioButtonMenuItem[4];
		clickOptions[0] = new JRadioButtonMenuItem( "Open blue message", false );
		clickOptions[1] = new JRadioButtonMenuItem( "Open green message", false );
		clickOptions[2] = new JRadioButtonMenuItem( "Open purple message", false );
		clickOptions[3] = new JRadioButtonMenuItem( "Open player profile", false );

		int clickSelect = client == null ? 0 : Integer.parseInt( client.getSettings().getProperty( "nameClickOpens" ) );
		clickOptions[ clickSelect ].setSelected( true );

		for ( int i = 0; i < clickOptions.length; ++i )
		{
			clickGroup.add( clickOptions[i] );
			clicksMenu.add( clickOptions[i] );
		}

		JMenu selectMenu = new JMenu( "Wholist" );
		selectMenu.setMnemonic( KeyEvent.VK_W );
		menuBar.add( selectMenu );

		selectMenu.add( new InvocationMenuItem( "Comma-delimited list", KeyEvent.VK_C, this, "convertToCDL" ) );
		selectMenu.add( new InvocationMenuItem( "Buff selected players", KeyEvent.VK_B, this, "buffSelected" ) );
	}

	public Object [] getSelectedPlayers()
	{
		Object [] selectedPlayers = contactsDisplay.getSelectedValues();

		// If no players are selected, and the player uses the
		// option, assume they want everyone.

		if ( selectedPlayers.length == 0 )
			selectedPlayers = contacts.toArray();

		return selectedPlayers;
	}

	public void buffSelected()
	{
		if ( client == null )
			return;

		Object [] selectedPlayers = getSelectedPlayers();
		UseSkillRequest selectedBuff = (UseSkillRequest) JOptionPane.showInputDialog(
			null, "I want to use this skill on " + selectedPlayers.length + " players...", "The Ultra-Rare Chat Buffing System (CBS)",
			JOptionPane.INFORMATION_MESSAGE, null, client.getCharacterData().getAvailableSkills().toArray(), client.getCharacterData().getAvailableSkills().get(0) );

		if ( selectedBuff == null )
			return;

		int buffCount = getQuantity( "This many casts per player...", 1 );

		UseSkillRequest [] requests = new UseSkillRequest[ selectedPlayers.length ];

		for ( int i = 0; i < requests.length; ++i )
			requests[i] = new UseSkillRequest( client, selectedBuff.getSkillName(), (String) selectedPlayers[i], buffCount );

		(new RequestThread( requests )).start();
	}

	public void convertToCDL()
	{
		if ( client == null )
			return;

		StringBuffer listCDL = new StringBuffer();
		Object [] selectedPlayers = getSelectedPlayers();

		for ( int i = 0; i < selectedPlayers.length; ++i )
		{
			if ( i != 0 )  listCDL.append( ", " );
			listCDL.append( (String) selectedPlayers[i] );
		}

		JOptionPane.showInputDialog( "Here's your CDL!", listCDL.toString() );
	}

	private class ContactListPanel extends JPanel
	{
		public ContactListPanel()
		{
			setLayout( new GridLayout( 1, 1 ) );
			contactsDisplay = new JList( contacts );
			contactsDisplay.setSelectionMode( ListSelectionModel.SINGLE_SELECTION );

			contactsDisplay.setVisibleRowCount( 25 );
			contactsDisplay.setPrototypeCellValue( "ABCDEFGHIJKLMNOPQRSTUVWXYZ" );

			contactsDisplay.addMouseListener( new SendInstantMessageAdapter() );

			add( new JScrollPane( contactsDisplay, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
				JScrollPane.HORIZONTAL_SCROLLBAR_NEVER ) );
		}
	}

	private class SendInstantMessageAdapter extends MouseAdapter
	{
		public void mouseClicked( MouseEvent e )
		{
			JList list = (JList) e.getSource();

			// The only event handled by the adapter is a double-click;
			// when a double-click is detected, a new ChatFrame is created
			// for the specified player.

			if ( e.getClickCount() == 2 )
			{
				int index = list.locationToIndex( e.getPoint() );

				if ( index >= 0 && index < contacts.size() )
					handleName( (String) contacts.get( index ) );
			}
		}

		private void handleName( String contactName )
		{
			Class frameClass = clickOptions[1].isSelected() ? GreenMessageFrame.class :
				clickOptions[2].isSelected() ? GiftMessageFrame.class : ProfileFrame.class;

			Object [] parameters = new Object[2];
			parameters[0] = client;
			parameters[1] = contactName;

			if ( clickOptions[0].isSelected() )
				client.getMessenger().openInstantMessage( contactName );
			else
				SwingUtilities.invokeLater( new CreateFrameRunnable( frameClass, parameters ) );
		}
	}
}
