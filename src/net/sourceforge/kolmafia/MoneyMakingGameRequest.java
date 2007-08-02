/**
 * Copyright (c) 2005-2007, KoLmafia development team
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

import java.text.SimpleDateFormat;

import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.TreeMap;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.java.dev.spellcast.utilities.SortedListModel;

public class MoneyMakingGameRequest extends KoLRequest
{
	private static final Pattern ROW_PATTERN = Pattern.compile( "<tr.*?</b></td>" );
	private static final Pattern CELL_PATTERN = Pattern.compile( "<td.*?</td>" );
	private static final Pattern MADE_PATTERN = Pattern.compile( "<div id='made'>.*?</div>" );
	private static final Pattern TAKEN_PATTERN = Pattern.compile( "<div id='taken'>.*?</div>" );

	private static final Pattern AMOUNT_PATTERN = Pattern.compile( ">([\\d,]+) " );
	private static final Pattern PLAYER_PATTERN = Pattern.compile( "who=(\\d+).*?<b>(.*?)</b>" );

	private static final SortedListModel betSummary = new SortedListModel();
	private static final SimpleDateFormat RESULT_FORMAT = new SimpleDateFormat( "MM/dd/yy hh:mma", Locale.US );

	public MoneyMakingGameRequest()
	{	super( "betarchive.php" );
	}

	protected boolean retryOnTimeout()
	{	return true;
	}

	public void run()
	{
		KoLmafia.updateDisplay( "Retrieving bet archive..." );
		super.run();
	}

	public void processResults()
	{
		ArrayList madeBets = new ArrayList();
		ArrayList takenBets = new ArrayList();

		Pattern singleBet = ROW_PATTERN;

		Matcher madeMatcher = MADE_PATTERN.matcher( this.responseText );
		if ( madeMatcher.find() )
		{
			madeMatcher = singleBet.matcher( madeMatcher.group() );
			madeMatcher.find();

			while ( madeMatcher.find() )
				if ( madeMatcher.group().indexOf( ">&nbsp;<" ) == -1 )
					madeBets.add( new MoneyMakingGameResult( madeMatcher.group(), true ) );
		}

		Matcher takenMatcher = TAKEN_PATTERN.matcher( this.responseText );
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
			if ( !playerMap.containsKey( betHistoryArray[i].playerId ) )
				playerMap.put( betHistoryArray[i].playerId, new Integer(0) );

			Integer currentTally = (Integer) playerMap.get( betHistoryArray[i].playerId );
			playerMap.put( betHistoryArray[i].playerId, new Integer( currentTally.intValue() + betHistoryArray[i].betAmount ) );
		}

		Object [] keys = playerMap.keySet().toArray();
		betSummary.clear();

		for ( int i = 0; i < keys.length; ++i )
			betSummary.add( new MoneyMakingGameSummary( (String) keys[i], (Integer) playerMap.get( keys[i] ) ) );
	}

	public static final SortedListModel getBetSummary()
	{	return betSummary;
	}

	private class MoneyMakingGameSummary implements Comparable
	{
		private String playerId;
		private Integer winAmount;

		public MoneyMakingGameSummary( String playerId, Integer winAmount )
		{
			this.playerId = playerId;
			this.winAmount = winAmount;
		}

		public int compareTo( Object o )
		{	return this.winAmount.compareTo( ((MoneyMakingGameSummary)o).winAmount );
		}

		public String toString()
		{
			return "<html><font color=" + (this.winAmount.intValue() > 0 ? "green" : "red") +
				">" + KoLmafia.getPlayerName( this.playerId ) + ": " + (this.winAmount.intValue() > 0 ? "+" : "") +
				COMMA_FORMAT.format( this.winAmount.intValue() ) + "</font></html>";
		}
	}

	private class MoneyMakingGameResult
	{
		private Date timestamp;
		private int betAmount;
		private String playerId;

		private boolean isPositive;

		public MoneyMakingGameResult( String resultText, boolean isPlacedBet )
		{
			this.isPositive = resultText.indexOf( "color='green'>+" ) != -1;

			Matcher results = CELL_PATTERN.matcher( resultText );

			// The first cell in the row is the timestamp
			// for the result.

			try
			{
				if ( results.find() )
					this.timestamp = RESULT_FORMAT.parse( results.group().replaceAll( "&nbsp;", " " ).replaceAll( "<.*?>", "" ) );
			}
			catch ( Exception e )
			{
				// This should not happen.  Therefore, print
				// a stack trace for debug purposes.

				StaticEntity.printStackTrace( e );
			}

			// Next is the amount which was bet.  In this
			// case, parse out the value.

			if ( results.find() )
			{
				Matcher amountMatcher = AMOUNT_PATTERN.matcher( results.group() );
				if ( amountMatcher.find() )
				{
					this.betAmount = StaticEntity.parseInt( amountMatcher.group(1) );
					this.betAmount = (int) ((this.betAmount) * (this.isPositive ? 0.998f : -1.0f));
				}
			}

			// The next is the name of the player who placed
			// or took your bet.

			if ( results.find() )
			{
				Matcher playerMatcher = PLAYER_PATTERN.matcher( results.group() );
				if ( playerMatcher.find() )
				{
					this.playerId = playerMatcher.group(1);
					KoLmafia.registerPlayer( playerMatcher.group(2), this.playerId );
				}
			}
		}
	}
}
