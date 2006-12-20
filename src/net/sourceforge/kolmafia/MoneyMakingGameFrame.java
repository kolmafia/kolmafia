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

import java.awt.CardLayout;
import javax.swing.JList;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class MoneyMakingGameFrame extends KoLFrame
{
	private static final Pattern BET_PATTERN = Pattern.compile( "<a href='bet.php'.*?>(.*?)</a>" );
	private static final AdventureResult CASINO_PASS = new AdventureResult( 40, 1 );

	public MoneyMakingGameFrame()
	{
		super( "Coin Toss Game" );

		framePanel.setLayout( new CardLayout( 10, 10 ) );
		framePanel.add( new AnalysisPanel(), "" );
	}

	private class AnalysisPanel extends LabeledScrollPanel
	{
		public AnalysisPanel()
		{	super( "Bet History", null, null, new JList( MoneyMakingGameRequest.getBetSummary() ) );
		}

		public void actionConfirmed()
		{
		}

		public void actionCancelled()
		{
		}
	}

	public static String handleBetResult( String message )
	{
		// <a target=mainpane href='showplayer.php?who=721048'><a href='bet.php' target=mainpane class=nounder><b>Interesting Sam</b></a> took your 1,000 Meat bet, and you won, earning you 1,998 Meat.</a>
		Matcher matcher = BET_PATTERN.matcher( message );

		if ( matcher.find() )
		{
			// Remove the link to bet.php
			message = matcher.replaceFirst( matcher.group(1) );
		}

		return message;
	}
}
