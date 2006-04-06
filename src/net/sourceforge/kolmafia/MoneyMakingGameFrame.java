/**
 * Copyright (c) 2006, KoLmafia development team
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

import java.awt.CardLayout;
import java.awt.BorderLayout;
import javax.swing.JTabbedPane;
import javax.swing.JOptionPane;

import net.java.dev.spellcast.utilities.LockableListModel;

public class MoneyMakingGameFrame extends KoLFrame
{
	private static final AdventureResult CASINO_PASS = new AdventureResult( 40, 1 );

	public MoneyMakingGameFrame()
	{
		super( "The Meatsink" );

		// tabs = new JTabbedPane();
		// tabs.addTab( "Analysis", new AnalysisPanel() );
		// framePanel.add( tabs, BorderLayout.CENTER );

		framePanel.setLayout( new CardLayout( 10, 10 ) );
		framePanel.add( new AnalysisPanel(), "" );
	}

	private class AnalysisPanel extends ItemManagePanel
	{
		public AnalysisPanel()
		{	super( "Bet History", "analyze", "cheat", new LockableListModel() );
		}

		public void actionConfirmed()
		{
			MoneyMakingGameRequest analyzer = new MoneyMakingGameRequest( StaticEntity.getClient() );
			analyzer.run();
			elementList.setModel( analyzer.getBetSummary() );

			DEFAULT_SHELL.updateDisplay( "Bet archive retrieved." );
			StaticEntity.getClient().enableDisplay();
		}

		public void actionCancelled()
		{
			if ( !KoLCharacter.getInventory().contains( CASINO_PASS ) )
			{
				JOptionPane.showMessageDialog( null, "You do not have a casino pass." );
				return;
			}

			DEFAULT_SHELL.executeLine( "sell * casino pass" );
			JOptionPane.showMessageDialog( null, "Your casino passes have been sold to Jarlsberg." );
			StaticEntity.getClient().enableDisplay();
		}
	}

	public static void handleBetResult( String message )
	{
	}
}
