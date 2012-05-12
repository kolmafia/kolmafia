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

package net.sourceforge.kolmafia.session;

import java.io.BufferedReader;
import java.io.File;
import java.io.PrintStream;

import java.util.Date;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.TreeMap;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.java.dev.spellcast.utilities.DataUtilities;

import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLMailMessage;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.LogStream;
import net.sourceforge.kolmafia.RequestThread;

import net.sourceforge.kolmafia.objectpool.IntegerPool;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.request.MailboxRequest;
import net.sourceforge.kolmafia.request.ProfileRequest;
import net.sourceforge.kolmafia.request.PeeVPeeRequest;
import net.sourceforge.kolmafia.request.PvpRequest;

import net.sourceforge.kolmafia.utilities.FileUtilities;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class PvpManager
{
	private static final Pattern TATTOO_PATTERN =
		Pattern.compile( "You have unlocked (\\d+) <a class=nounder href=\"account_tattoos.php\">" );
	private static final Pattern TROPHY_PATTERN =
		Pattern.compile( "You have earned (\\d+) <a class=nounder href=\"trophies.php\">" );
	private static final Pattern FLOWER_PATTERN = Pattern.compile( "You have picked ([\\d,]+) pretty flower" );
	private static final Pattern CANADA_PATTERN =
		Pattern.compile( "white Canadian</a>&nbsp;&nbsp;&nbsp;</td><td>([\\d,]+)</td>" );

	private static int tattooCount = -1;
	private static int trophyCount = -1;
	private static int flowerCount = -1;
	private static int canadaCount = -1;

	public static void updateMinis()
	{
		GenericRequest miniChecker = new GenericRequest( "questlog.php?which=3" );
		miniChecker.run();

		Matcher miniMatcher = PvpManager.TATTOO_PATTERN.matcher( miniChecker.responseText );
		if ( miniMatcher.find() )
		{
			PvpManager.tattooCount = StringUtilities.parseInt( miniMatcher.group( 1 ) );
		}
		else
		{
			PvpManager.tattooCount = 0;
		}

		miniMatcher = PvpManager.TROPHY_PATTERN.matcher( miniChecker.responseText );
		if ( miniMatcher.find() )
		{
			PvpManager.trophyCount = StringUtilities.parseInt( miniMatcher.group( 1 ) );
		}
		else
		{
			PvpManager.trophyCount = 0;
		}

		miniMatcher = PvpManager.FLOWER_PATTERN.matcher( miniChecker.responseText );
		if ( miniMatcher.find() )
		{
			PvpManager.flowerCount = StringUtilities.parseInt( miniMatcher.group( 1 ) );
		}
		else
		{
			PvpManager.flowerCount = 0;
		}

		miniChecker.constructURLString( "showconsumption.php" );
		miniChecker.run();

		miniMatcher = PvpManager.CANADA_PATTERN.matcher( miniChecker.responseText );
		if ( miniMatcher.find() )
		{
			PvpManager.canadaCount = StringUtilities.parseInt( miniMatcher.group( 1 ) );
		}
		else
		{
			PvpManager.canadaCount = 0;
		}

	}

	public static void summarizeFlowerHunterData()
	{
		PvpManager.processDefenseContests();

		File[] attackLogs = DataUtilities.listFiles( KoLConstants.ATTACKS_LOCATION );

		TreeMap minis = new TreeMap();
		KoLmafia.updateDisplay( "Scanning attack logs..." );

		for ( int i = 0; i < attackLogs.length; ++i )
		{
			if ( !attackLogs[ i ].getName().endsWith( "__spreadsheet.txt" ) )
			{
				PvpManager.registerFlowerHunterData( minis, FileUtilities.getReader( attackLogs[ i ] ) );
			}
		}

		PrintStream spreadsheet =
			LogStream.openStream( new File( KoLConstants.ATTACKS_LOCATION, "__spreadsheet.txt" ), true );

		spreadsheet.println( "Name\tTattoos\t\tTrophies\t\tFlowers\t\tCanadians" );
		spreadsheet.println( "\tLow\tHigh\tLow\tHigh\tLow\tHigh\tLow\tHigh" );

		Iterator minisIterator = minis.entrySet().iterator();

		while ( minisIterator.hasNext() )
		{
			Entry entry = (Entry) minisIterator.next();

			Object key = entry.getKey();
			Object[] value = (Object[]) entry.getValue();

			boolean shouldPrint = false;
			for ( int i = 0; i < value.length; i += 2 )
			{
				shouldPrint |= value[ i ] != null;
			}

			if ( !shouldPrint )
			{
				continue;
			}

			spreadsheet.print( key );

			for ( int i = 0; i < value.length; i += 2 )
			{
				spreadsheet.print( "\t" );
				spreadsheet.print( value[ i ] == null ? "" : value[ i ] );
			}

			spreadsheet.println();
		}

		spreadsheet.close();
		KoLmafia.updateDisplay( "Spreadsheet generated." );
	}

	private static void registerFlowerHunterData( final TreeMap minis, final BufferedReader attackLog )
	{
		String line;
		while ( ( line = FileUtilities.readLine( attackLog ) ) != null )
		{
			// First, try to figure out whose data is being registered in
			// this giant spreadsheet.

			Matcher versusMatcher = PvpRequest.VERSUS_PATTERN.matcher( line );
			if ( !versusMatcher.find() )
			{
				line = FileUtilities.readLine( attackLog );
				versusMatcher = PvpRequest.VERSUS_PATTERN.matcher( line );

				if ( !versusMatcher.find() )
				{
					return;
				}
			}

			String opponent =
				versusMatcher.group( 2 ).equals( "you" ) ? versusMatcher.group( 1 ) : versusMatcher.group( 2 );

			line = FileUtilities.readLine( attackLog );
			Matcher minisMatcher = PvpRequest.MINIS_PATTERN.matcher( line );

			if ( !minisMatcher.find() )
			{
				return;
			}

			// Next, make sure that you have all the information needed to
			// generate a row in the spreadsheet.

			Integer[] yourData = new Integer[ 4 ];
			for ( int i = 0; i < yourData.length; ++i )
			{
				yourData[ i ] = Integer.valueOf( minisMatcher.group( i + 1 ) );
			}

			if ( !minis.containsKey( opponent ) )
			{
				minis.put( opponent, new Object[ 16 ] );
			}

			// There are seven minis to handle.  You can discard the first
			// three because they're attack minis.

			FileUtilities.readLine( attackLog );
			FileUtilities.readLine( attackLog );
			FileUtilities.readLine( attackLog );

			Object[] theirData = (Object[]) minis.get( opponent );

			PvpManager.registerFlowerContestData( yourData, theirData, FileUtilities.readLine( attackLog ) );
			PvpManager.registerFlowerContestData( yourData, theirData, FileUtilities.readLine( attackLog ) );
			PvpManager.registerFlowerContestData( yourData, theirData, FileUtilities.readLine( attackLog ) );
			PvpManager.registerFlowerContestData( yourData, theirData, FileUtilities.readLine( attackLog ) );

			// With all that information registered, go ahead and store the
			// attack information back into the tree map.

			minis.put( opponent, theirData );
		}
	}

	private static void registerFlowerContestData( final Integer[] yourData, final Object[] theirData,
		final String currentAttack )
	{
		int baseIndex = -1;
		boolean wonContest = currentAttack.endsWith( "You won." );

		if ( currentAttack.startsWith( "Tattoo Contest" ) )
		{
			baseIndex = 0;
		}

		if ( currentAttack.startsWith( "Trophy Contest" ) )
		{
			baseIndex = 1;
		}

		if ( currentAttack.startsWith( "Flower Picking Contest" ) )
		{
			baseIndex = 2;
		}

		if ( currentAttack.startsWith( "Canadianity Contest" ) )
		{
			baseIndex = 3;
		}

		if ( baseIndex < 0 )
		{
			return;
		}

		if ( wonContest )
		{
			if ( theirData[ 4 * baseIndex ] == null || yourData[ baseIndex ].intValue() < ( (Integer) theirData[ 4 * baseIndex ] ).intValue() )
			{
				theirData[ 4 * baseIndex ] = yourData[ baseIndex ];
			}
		}
		else if ( theirData[ 4 * baseIndex + 2 ] == null || yourData[ baseIndex ].intValue() > ( (Integer) theirData[ 4 * baseIndex + 2 ] ).intValue() )
		{
			theirData[ 4 * baseIndex + 2 ] = yourData[ baseIndex ];
		}
	}

	public static void executePvpRequest( final String mission, int stance )
	{
		KoLmafia.updateDisplay( "Determining remaining fights..." );
		RequestThread.postRequest( new PeeVPeeRequest( "fight" ) );

		if ( stance == 0 )
		{
			if ( KoLCharacter.getBaseMuscle() >= KoLCharacter.getBaseMysticality() && KoLCharacter.getBaseMuscle() >= KoLCharacter.getBaseMoxie() )
			{
				stance = 1;
			}
			else if ( KoLCharacter.getBaseMysticality() >= KoLCharacter.getBaseMuscle() && KoLCharacter.getBaseMysticality() >= KoLCharacter.getBaseMoxie() )
			{
				stance = 2;
			}
			else
			{
				stance = 3;
			}
		}

		PeeVPeeRequest request = new PeeVPeeRequest( "", stance, mission );
		
		int fightsCompleted = 0;
		int totalFights = KoLCharacter.getAttacksLeft();

		while ( !KoLmafia.refusesContinue() && KoLCharacter.getAttacksLeft() > 0 )
		{
			fightsCompleted++;
			KoLmafia.updateDisplay( "Attack " + fightsCompleted + " of " + totalFights );
			RequestThread.postRequest( request );

			if ( !KoLmafia.refusesContinue() )
			{
				KoLmafia.forceContinue();
			}
		}

		if ( KoLmafia.permitsContinue() )
		{
			KoLmafia.updateDisplay( "You have " + KoLCharacter.getAttacksLeft() + " attacks remaining." );
		}
	}

	public static final void executePvpRequest( final ProfileRequest[] targets, final PvpRequest request )
	{
		for ( int i = 0; i < targets.length && KoLmafia.permitsContinue() && KoLCharacter.getAttacksLeft() > 0; ++i )
		{
			if ( targets[ i ] == null )
			{
				continue;
			}

			if ( Preferences.getString( "currentPvpVictories" ).indexOf( targets[ i ].getPlayerName() ) != -1 )
			{
				continue;
			}

			if ( targets[ i ].getPlayerName().toLowerCase().startsWith( "devster" ) )
			{
				continue;
			}

			KoLmafia.updateDisplay( "Attacking " + targets[ i ].getPlayerName() + "..." );
			request.setTarget( targets[ i ].getPlayerName() );
			RequestThread.postRequest( request );

			if ( request.responseText.indexOf( "Your PvP Ranking decreased by" ) != -1 )
			{
				KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "You lost to " + targets[ i ].getPlayerName() + "." );
			}
			else
			{
				Preferences.setString(
					"currentPvpVictories",
					Preferences.getString( "currentPvpVictories" ) + targets[ i ].getPlayerName() + "," );
			}
		}
	}

	public static final void processOffenseContests( final String responseText )
	{
		int endIndex = responseText.indexOf( "Your PvP Ranking" );
		if ( endIndex == -1 )
		{
			return;
		}
		String resultText =
			StringUtilities.globalStringReplace(
				responseText.substring( responseText.indexOf( "<td>" ) + 4, endIndex ),
				"<p>", KoLConstants.LINE_BREAK );

		resultText =
			KoLConstants.ANYTAG_PATTERN.matcher( resultText.substring( 0, resultText.lastIndexOf( "<b>" ) ) ).replaceAll(
				"" );

		String[] fightData = resultText.split( "\n" );
		String target = null;

		for ( int i = 0; i < fightData.length; ++i )
		{
			if ( fightData[ i ].startsWith( "You call" ) )
			{
				target = fightData[ i ].substring( 9, fightData[ i ].indexOf( " out," ) );
				fightData[ i ] = null;
				break;
			}

			fightData[ i ] = null;
		}

		PrintStream pvpResults =
			LogStream.openStream(
				new File( KoLConstants.ATTACKS_LOCATION, KoLCharacter.baseUserName() + "_offense.txt" ), false );

		pvpResults.println();
		pvpResults.println( new Date() );
		pvpResults.println( KoLCharacter.getUserName() + " initiated a PvP attack against " + target + "." );
		pvpResults.println( "(" + PvpManager.tattooCount + " tattoos, " + PvpManager.trophyCount + " trophies, " + PvpManager.flowerCount + " flowers, " + PvpManager.canadaCount + " white canadians)" );

		pvpResults.println();

		for ( int i = 0; i < fightData.length; ++i )
		{
			if ( fightData[ i ] != null )
			{
				PvpManager.processOffenseContest( target, fightData[ i ], pvpResults );
			}
		}

		pvpResults.println();
		pvpResults.println();
		pvpResults.close();

		// If the player won flowers, increment their post-fight
		// flower count from the battle.

		if ( responseText.indexOf( "flower.gif" ) != -1 )
		{
			++PvpManager.flowerCount;
		}
	}

	public static final void processDefenseContests()
	{
		File defenseFile = new File( KoLConstants.ATTACKS_LOCATION, KoLCharacter.baseUserName() + "_defense.txt" );
		PrintStream pvpResults = LogStream.openStream( defenseFile, false );

		RequestThread.postRequest( new PvpRequest() );
		RequestThread.postRequest( new MailboxRequest( "PvP" ) );

		KoLMailMessage attack;
		String attackText;

		Iterator attackIterator = MailManager.getMessages( "PvP" ).iterator();

		while ( attackIterator.hasNext() )
		{
			attack = (KoLMailMessage) attackIterator.next();
			attackText = attack.getMessageHTML();

			int stopIndex = attackText.indexOf( "<br><p>" );
			if ( stopIndex == -1 )
			{
				stopIndex = attackText.indexOf( "<br><P>" );
			}
			if ( stopIndex == -1 )
			{
				continue;
			}

			attackText = attackText.substring( 0, stopIndex );
			attackText = StringUtilities.globalStringReplace( attackText, "<p>", "\n\n" );
			attackText = StringUtilities.globalStringReplace( attackText, "<br>", "\n" );
			attackText =
				StringUtilities.singleStringReplace(
					attackText,
					"  Here's a play-by-play report on how it went down:",
					"\n(" + tattooCount + " tattoos, " + trophyCount + " trophies, " + flowerCount + " flowers, " + canadaCount + " white canadians)" );

			attackText = attackText.trim();

			pvpResults.println();
			pvpResults.println( attack.getTimestamp() );
			pvpResults.println( attackText );
			pvpResults.println();
		}
	}

	public static final void processOffenseContest( final String target, final String line, final PrintStream ostream )
	{
		String contest = null;

		// Messages for the battle stance that the player selected
		// for their attack.

		if ( line.startsWith( "You attempt to Burninate" ) )
		{
			contest = "Buffed Mysticality";
		}
		else if ( line.startsWith( "You challenge your opponent to a game of Telekinetic Ping-Pong" ) )
		{
			contest = "Unbuffed Mysticality";
		}
		else if ( line.startsWith( "You try to embarrrass" ) )
		{
			contest = "Buffed Moxie";
		}
		else if ( line.startsWith( "You challenge your opponent to an insult contest" ) )
		{
			contest = "Unbuffed Moxie";
		}
		else if ( line.indexOf( "challenges you to an armwrestling match" ) != -1 )
		{
			contest = "Buffed Muscle";
		}
		else if ( line.indexOf( "challenges you to a game of Wizard's Croquet" ) != -1 )
		{
			contest = "Buffed Mysticality";
		}
		else if ( line.indexOf( "challenges you to a dancing contest" ) != -1 )
		{
			contest = "Buffed Moxie";
		}
		else if ( line.indexOf( "challenges you to a diet balance contest" ) != -1 )
		{
			contest = "Balanced Diet";
		}
		else if ( line.indexOf( "challenges you to a bleeding contest" ) != -1 )
		{
			contest = "Bleeding Contest";
		}
		else if ( line.indexOf( "challenges you to a burping contest" ) != -1 )
		{
			contest = "Burping Contest";
		}
		else if ( line.indexOf( "challenges you to a Canadianity contest" ) != -1 )
		{
			contest = "Canadianity Contest";
		}
		else if ( line.indexOf( "challenges you to a familiar show" ) != -1 )
		{
			contest = "Familiar Weight";
		}
		else if ( line.indexOf( "arranges an impromptu fashion show" ) != -1 )
		{
			contest = "Fashion Show";
		}
		else if ( line.indexOf( "challenges you to a flower-picking contest" ) != -1 )
		{
			contest = "Flower Picking Contest";
		}
		else if ( line.indexOf( "challenges you to a &quot;How Hung Over are You?&quot; competition" ) != -1 )
		{
			contest = "\"How Hung Over are You?\"";
		}
		else if ( line.indexOf( "challenges you to a pie-eating competition" ) != -1 )
		{
			contest = "Pie-Eating Contest";
		}
		else if ( line.indexOf( "challenges you to a popularity contest" ) != -1 )
		{
			contest = "Popularity Contest";
		}
		else if ( line.indexOf( "challenges you to a purity test" ) != -1 )
		{
			contest = "Purity Test";
		}
		else if ( line.indexOf( "challenges you to a tattoo contest" ) != -1 )
		{
			contest = "Tattoo Contest";
		}
		else if ( line.indexOf( "challenges you to a trophy-stacking contest" ) != -1 )
		{
			contest = "Trophy Contest";
		}
		else if ( line.indexOf( "challenges you to a wine tasting contest" ) != -1 )
		{
			contest = "Wine Tasting Contest";
		}
		else if ( line.indexOf( "challenges you to a work ethic contest" ) != -1 )
		{
			contest = "Work Ethic Contest";
		}
		else
		{
			contest = "Unknown Contest";
		}

		String lastMessage = line.substring( line.lastIndexOf( " " ) + 1, line.length() - 2 );
		boolean isWinner = lastMessage.toUpperCase().equals( lastMessage );

		String result = contest + ": You " + ( isWinner ? "won." : "lost." );
		ostream.println( result );
	}

}
