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

import java.util.Date;
import java.util.TreeMap;
import java.util.ArrayList;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.text.SimpleDateFormat;

import net.java.dev.spellcast.utilities.SortedListModel;

/**
 * An extension of the generic <code>KoLRequest</code> class which handles
 * placing bets at the Money Making Game
 */

public class MoneyMakingGameRequest extends KoLRequest
{
	private SortedListModel betSummary = new SortedListModel();
	private static final SimpleDateFormat RESULT_FORMAT = new SimpleDateFormat( "MM/dd/yy hh:mma" );


	public MoneyMakingGameRequest( KoLmafia client )
	{	super( client, "betarchive.php" );
	}

	public void run()
	{
		KoLmafia.updateDisplay( "Retrieving bet archive..." );
		super.run();
	}

	protected void processResults()
	{
		ArrayList madeBets = new ArrayList();
		ArrayList takenBets = new ArrayList();

		Pattern singleBet = Pattern.compile( "<tr.*?</b></td>" );

		Matcher madeMatcher = Pattern.compile( "<div id='made'>.*?</div>" ).matcher( responseText );
		if ( madeMatcher.find() )
		{
			madeMatcher = singleBet.matcher( madeMatcher.group() );
			madeMatcher.find();

			while ( madeMatcher.find() )
				if ( madeMatcher.group().indexOf( ">&nbsp;<" ) == -1 )
					madeBets.add( new MoneyMakingGameResult( madeMatcher.group(), true ) );
		}

		Matcher takenMatcher = Pattern.compile( "<div id='taken'>.*?</div>" ).matcher( responseText );
		if ( takenMatcher.find() )
		{
			takenMatcher = singleBet.matcher( takenMatcher.group() );
			takenMatcher.find();

			while ( takenMatcher.find() )
				if ( takenMatcher.group().indexOf( ">&nbsp;<" ) == -1 )
					takenBets.add( new MoneyMakingGameResult( takenMatcher.group(), false ) );
		}

		TreeMap playerMap = new TreeMap();
		ArrayList betHistory = new ArrayList();
		betHistory.addAll( madeBets );
		betHistory.addAll( takenBets );

		MoneyMakingGameResult [] betHistoryArray = new MoneyMakingGameResult[ betHistory.size() ];
		betHistory.toArray( betHistoryArray );

		for ( int i = 0; i < betHistoryArray.length; ++i )
		{
			if ( !playerMap.containsKey( betHistoryArray[i].playerID ) )
				playerMap.put( betHistoryArray[i].playerID, new Integer(0) );

			Integer currentTally = (Integer) playerMap.get( betHistoryArray[i].playerID );
			playerMap.put( betHistoryArray[i].playerID, new Integer( currentTally.intValue() + betHistoryArray[i].betAmount ) );
		}

		Object [] keys = playerMap.keySet().toArray();
		betSummary.clear();

		for ( int i = 0; i < keys.length; ++i )
			betSummary.add( new MoneyMakingGameSummary( (String) keys[i], (Integer) playerMap.get( keys[i] ) ) );
	}

	public SortedListModel getBetSummary()
	{	return betSummary;
	}

	private class MoneyMakingGameSummary implements Comparable
	{
		private String playerID;
		private Integer winAmount;

		public MoneyMakingGameSummary( String playerID, Integer winAmount )
		{
			this.playerID = playerID;
			this.winAmount = winAmount;
		}

		public int compareTo( Object o )
		{	return winAmount.compareTo( ((MoneyMakingGameSummary)o).winAmount );
		}

		public String toString()
		{
			return "<html><font color=" + (winAmount.intValue() > 0 ? "green" : "red") +
				">" + client.getPlayerName( playerID ) + ": " + (winAmount.intValue() > 0 ? "+" : "") +
				df.format( winAmount.intValue() ) + "</font></html>";
		}
	}

	private class MoneyMakingGameResult
	{
		private Date timestamp;
		private int betAmount;
		private String playerID;

		private boolean isPlacedBet;
		private boolean isPositive;

		public MoneyMakingGameResult( String resultText, boolean isPlacedBet )
		{
			try
			{
				this.isPlacedBet = isPlacedBet;
				this.isPositive = resultText.indexOf( "color='green'>+" ) != -1;

				Matcher results = Pattern.compile( "<td.*?</td>" ).matcher( resultText );

				// The first cell in the row is the timestamp
				// for the result.

				if ( results.find() )
					timestamp = RESULT_FORMAT.parse( results.group().replaceAll( "&nbsp;", " " ).replaceAll( "<.*?>", "" ) );

				// Next is the amount which was bet.  In this
				// case, parse out the value.

				if ( results.find() )
				{
					Matcher amountMatcher = Pattern.compile( ">([\\d,]+) " ).matcher( results.group() );
					if ( amountMatcher.find() )
					{
						betAmount = df.parse( amountMatcher.group(1) ).intValue();
						betAmount = (int) (((float) betAmount) * (isPositive ? 0.998f : -1.0f));
					}
				}

				// The next is the name of the player who placed
				// or took your bet.

				if ( results.find() )
				{
					Matcher playerMatcher = Pattern.compile( "who=(\\d+).*?<b>(.*?)</b>" ).matcher( results.group() );
					if ( playerMatcher.find() )
					{
						playerID = playerMatcher.group(1);
						client.registerPlayer( playerMatcher.group(2), playerID );
					}
				}
			}
			catch ( Exception e )
			{
				// This should not happen.  Therefore, print
				// a stack trace for debug purposes.

				StaticEntity.printStackTrace( e );
			}
		}
	}
}
