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

import java.awt.Dimension;
import java.awt.BorderLayout;
import javax.swing.JComboBox;
import javax.swing.JTextField;
import net.java.dev.spellcast.utilities.LockableListModel;

public class CakeArenaFrame extends KoLPanelFrame
{
	public CakeArenaFrame( KoLmafia client )
	{
		super( client, "KoLmafia: Susie's Secret Bedroom!" );

		if ( client != null && client.getCakeArenaManager().getOpponentList().isEmpty() )
			(new RequestThread( new CakeArenaRequest( client ) )).start();

		setContentPanel( new CakeArenaPanel() );
	}

	private class CakeArenaPanel extends KoLPanel
	{
		private JComboBox opponentSelect;
		private JComboBox fightOptions;
		private JTextField battleField;

		public CakeArenaPanel()
		{
			super( "fight!", "stop!", new Dimension( 80, 20 ), new Dimension( 400, 20 ) );

			opponentSelect = new JComboBox( client == null ? new LockableListModel() : client.getCakeArenaManager().getOpponentList() );

			fightOptions = new JComboBox();
			fightOptions.addItem( "Ultimate Cage Match" );
			fightOptions.addItem( "Scavenger Hunt" );
			fightOptions.addItem( "Obstacle Course" );
			fightOptions.addItem( "Hide and Seek" );

			battleField = new JTextField();

			VerifiableElement [] elements = new VerifiableElement[3];
			elements[0] = new VerifiableElement( "Opponent: ", opponentSelect );
			elements[1] = new VerifiableElement( "Event: ", fightOptions );
			elements[2] = new VerifiableElement( "Battles: ", battleField );

			setContent( elements );
		}

		public void actionConfirmed()
		{
			Object opponent = opponentSelect.getSelectedItem();
			if ( opponent == null )
				return;

			int eventID = fightOptions.getSelectedIndex() + 1;
			if ( eventID == 0 )
				return;

			int battleCount = getValue( battleField );
			client.getCakeArenaManager().fightOpponent( opponent.toString(), eventID, battleCount );
		}

		public void actionCancelled()
		{
			updateDisplay( ERROR_STATE, "Arena battles terminated." );
			client.cancelRequest();
		}
	}

	public static void main( String [] args )
	{
		KoLFrame uitest = new CakeArenaFrame( null );
		uitest.pack();  uitest.setVisible( true );  uitest.requestFocus();
	}
}