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
import java.awt.BorderLayout;
import java.awt.CardLayout;

// containers
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JComboBox;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

// utilities
import net.java.dev.spellcast.utilities.LockableListModel;

/**
 * A Frame to provide access to supported buffbots
 */

public class BuffRequestFrame extends KoLFrame
{
	private int buffIndex = -1;
	private JList buffRequestList;
	private JComboBox buffOptions;
	private LockableListModel buffRequests = new LockableListModel();

	public BuffRequestFrame()
	{
		super( "Purchase Buffs" );
		framePanel.setLayout( new CardLayout( 10, 10 ) );
		framePanel.add( new BuffRequestPanel(), "" );
	}

	private class BuffRequestPanel extends LabeledScrollPanel
	{
		public BuffRequestPanel()
		{
			super( "", "request", "online?", new JList( buffRequests ) );

			buffOptions = new BuffOptionsComboBox();
			actualPanel.add( buffOptions, BorderLayout.NORTH );
			buffRequestList = (JList) scrollComponent;
		}

		public void actionConfirmed()
		{
			String buff = BuffBotDatabase.getBuffName( buffIndex );
			int selection = buffRequestList.getSelectedIndex();
			String bot = BuffBotDatabase.getBuffBot( buffIndex, selection );
			int price = BuffBotDatabase.getBuffPrice( buffIndex, selection );
			int turns = BuffBotDatabase.getBuffTurns( buffIndex, selection );

			KoLmafia.updateDisplay( "Buying " + turns + " turns of " + buff + " from " + bot + "..." );
			(new GreenMessageRequest( StaticEntity.getClient(), bot, VERSION_NAME,
				new AdventureResult( AdventureResult.MEAT, price ), false )).run();

			KoLmafia.updateDisplay( "Buff request complete." );
			KoLmafia.enableDisplay();
		}

		public void actionCancelled()
		{
			int selection = buffRequestList.getSelectedIndex();
			String bot = BuffBotDatabase.getBuffBot( buffIndex, selection );

			KoLRequest request = new KoLRequest( StaticEntity.getClient(), "submitnewchat.php" );
			request.addFormField( "playerid", String.valueOf( KoLCharacter.getUserID() ) );
			request.addFormField( "pwd" );
			request.addFormField( "graf", "/whois " + bot );
			request.run();

			if ( request.responseText != null && request.responseText.indexOf( "online" ) != -1 )
				JOptionPane.showMessageDialog( null, "This bot is online." );
			else
				JOptionPane.showMessageDialog( null, "This bot is probably not online." );
		}
	}

	private class BuffOptionsComboBox extends JComboBox implements ActionListener
	{
		public BuffOptionsComboBox()
		{
			for ( int i = 0; i < BuffBotDatabase.ABBREVIATIONS.length; ++i )
				addItem( UneffectRequest.skillToEffect( ClassSkillsDatabase.getSkillName( ((Integer)BuffBotDatabase.ABBREVIATIONS[i][0]).intValue() ) ) );

			addActionListener( this );
			setSelectedIndex( 0 );
		}

		public void actionPerformed( ActionEvent e )
		{
			String selectedItem = (String) getSelectedItem();

			int buffCount = BuffBotDatabase.buffCount();
			buffIndex = -1;

			for ( int i = 0; i < buffCount; ++i )
				if ( selectedItem.indexOf( BuffBotDatabase.getBuffAbbreviation( i ) ) != -1 )
					buffIndex = i;

			if ( buffIndex == -1 )
				return;

			buffRequests.clear();

			int offeringCount = BuffBotDatabase.getBuffOfferingCount( buffIndex );
			for ( int j = 0; j < offeringCount; ++j )
				buffRequests.add( BuffBotDatabase.getBuffLabel( buffIndex, j ) );
		}
	}
}
