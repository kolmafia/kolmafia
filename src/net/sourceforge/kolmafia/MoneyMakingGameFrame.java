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

import java.util.regex.Pattern;
import java.util.regex.Matcher;

import net.java.dev.spellcast.utilities.LockableListModel;

public class MoneyMakingGameFrame extends KoLFrame
{
	private static final AdventureResult CASINO_PASS = new AdventureResult( 40, 1 );

	public MoneyMakingGameFrame()
	{
		super( "Coin Toss Game" );

		// tabs = new JTabbedPane();
		// tabs.addTab( "Analysis", new AnalysisPanel() );
		// framePanel.add( tabs, BorderLayout.CENTER );

		framePanel.setLayout( new CardLayout( 10, 10 ) );
		framePanel.add( new AnalysisPanel(), "" );
	}

	private class AnalysisPanel extends ItemManagePanel implements Runnable
	{
		public AnalysisPanel()
		{	super( "Bet History", "analyze", "cheat", new LockableListModel() );
		}

		public void actionConfirmed()
		{	(new RequestThread( this )).start();
		}

		public void actionCancelled()
		{
			KoLmafia.updateDisplay( "Cheating in progress (please wait)..." );
			KoLRequest.delay( 5000 );

			int fakedLossAmount = Math.min( 100000000, KoLCharacter.getAvailableMeat() );
			KoLmafia.updateDisplay( ERROR_STATE, "<html><font color=green><b>shwei</b> took your " +
				COMMA_FORMAT.format( fakedLossAmount ) + " Meat bet, and you <font color=red>lost</font>. Better luck next time.</font></html>" );

			StaticEntity.getClient().processResult( new AdventureResult( AdventureResult.MEAT, 0 - fakedLossAmount ) );
			KoLCharacter.updateStatus();
			KoLmafia.enableDisplay();
		}

		public void run()
		{
			MoneyMakingGameRequest analyzer = new MoneyMakingGameRequest( StaticEntity.getClient() );
			analyzer.run();

			elementList.setModel( analyzer.getBetSummary() );
			KoLmafia.updateDisplay( "Bet archive retrieved." );
		}
	}

	public static String handleBetResult( String message )
	{
		// <a target=mainpane href='showplayer.php?who=721048'><a href='bet.php' target=mainpane class=nounder><b>Interesting Sam</b></a> took your 1,000 Meat bet, and you won, earning you 1,998 Meat.</a>
		Matcher matcher = Pattern.compile( "<a href='bet.php'.*?>(.*?)</a>" ).matcher( message );

		if ( matcher.find() )
		{
			// Remove the link to bet.php
			message = matcher.replaceFirst( matcher.group(1) );
		}

		return message;
	}
}
