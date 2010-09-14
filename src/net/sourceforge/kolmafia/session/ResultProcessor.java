/**
 * Copyright (c) 2005-2010, KoLmafia development team
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

import java.util.List;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.FamiliarData;
import net.sourceforge.kolmafia.KoLAdventure;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.Modifiers;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.StaticEntity;
import net.sourceforge.kolmafia.objectpool.EffectPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.ConcoctionDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.persistence.Preferences;
import net.sourceforge.kolmafia.request.CreateItemRequest;
import net.sourceforge.kolmafia.request.FightRequest;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.request.HermitRequest;
import net.sourceforge.kolmafia.swingui.CoinmastersFrame;
import net.sourceforge.kolmafia.utilities.StringUtilities;
import net.sourceforge.kolmafia.webui.BarrelDecorator;
import net.sourceforge.kolmafia.webui.CellarDecorator;
import net.sourceforge.kolmafia.webui.IslandDecorator;

public class ResultProcessor
{
	private static Pattern DISCARD_PATTERN = Pattern.compile( "You discard your (.*?)\\." );
	private static Pattern INT_PATTERN = Pattern.compile( "([\\d]+)" );
	private static Pattern TRAILING_INT_PATTERN = Pattern.compile( "(.*?)(?:\\s*\\((\\d+)\\))?" );

	private static boolean receivedClover = false;
	private static boolean receivedDisassembledClover = false;

	// This number changes every time an item is processed, and can be used
	// by other code to tell if an item is received, without necessarily
	// knowing which item it was.

	public static int itemSequenceCount = 0;

	public static boolean receivedClover()
	{
		return ResultProcessor.receivedClover;
	}

	public static boolean receivedDisassembledClover()
	{
		return ResultProcessor.receivedDisassembledClover;
	}

	public static boolean shouldDisassembleClovers( String formURLString )
	{
		return ResultProcessor.receivedClover &&
		       FightRequest.getCurrentRound() == 0 &&
		       Preferences.getBoolean( "cloverProtectActive" ) &&
		       isCloverURL( formURLString );
	}

	public static boolean disassembledClovers( String formURLString )
	{
		return ResultProcessor.receivedDisassembledClover &&
		       Preferences.getBoolean( "cloverProtectActive" ) &&
		       isCloverURL( formURLString );
	}

	private static boolean isCloverURL( String formURLString )
	{
		return formURLString.startsWith( "adventure.php" ) ||
			formURLString.startsWith( "choice.php" ) ||
			formURLString.startsWith( "hermit.php" ) ||
			formURLString.startsWith( "mallstore.php" ) ||
			formURLString.startsWith( "barrel.php" ) ||
			formURLString.startsWith( "shore.php" ) ||
			formURLString.indexOf( "whichitem=553" ) != -1;
	}

	public static Pattern ITEM_TABLE_PATTERN = Pattern.compile( "<table class=\"item\".*?rel=\"(.*?)\".*?title=\"(.*?)\".*?descitem\\(([\\d]*)\\).*?</table>" );
	public static Pattern BOLD_NAME_PATTERN = Pattern.compile( "<b>([^<]*)</b>" );

	public static void registerNewItems( String results )
	{
		// Results now come in like this:
		//
		// <table class="item" style="float: none" rel="id=617&s=137&q=0&d=1&g=0&t=1&n=1&m=1&u=u">
		// <tr><td><img src="http://images.kingdomofloathing.com/itemimages/rcandy.gif"
		// alt="Angry Farmer candy" title="Angry Farmer candy" class=hand onClick='descitem(893169457)'></td>
		// <td valign=center class=effect>You acquire an item: <b>Angry Farmer candy</b></td></tr></table>
		//
		// Or, in haiku:
		//
		// <table class="item" style="float: none" rel="id=83&s=5&q=0&d=1&g=0&t=1&n=1&m=0&u=.">
		// <tr><td><img src="http://images.kingdomofloathing.com/itemimages/rustyshaft.gif"
		// alt="rusty metal shaft" title="rusty metal shaft" class=hand onClick='descitem(228339790)'></td>
		// <td valign=center class=effect><b>rusty metal shaft</b><br>was once your foe's, is now yours.<br>
		// Beaters-up, keepers.</td></tr></table>
		//
		// Pre-process all such matches and register new items

		Matcher itemMatcher = ResultProcessor.ITEM_TABLE_PATTERN.matcher( results );
		while ( itemMatcher.find() )
		{
			String relString = itemMatcher.group(1);
			String itemName = itemMatcher.group(2);
			String descId = itemMatcher.group(3);

			// If we already know this descid, known item.
			if ( ItemDatabase.getItemIdFromDescription( descId ) != -1 )
			{
				continue;
			}

			Matcher boldMatcher = ResultProcessor.BOLD_NAME_PATTERN.matcher( itemMatcher.group(0) );
			String items = boldMatcher.find() ? boldMatcher.group(1) : null;
			ItemDatabase.registerItem( itemName, descId, relString, items );
		}
	}

	public static boolean processResults( String results )
	{
		return ResultProcessor.processResults( false, results, null );
	}

	public static boolean processResults( boolean combatResults, String results )
	{
		return ResultProcessor.processResults( combatResults, results, null );
	}

	public static boolean processResults( String results, List data )
	{
		return ResultProcessor.processResults( false, results, data );
	}

	public static boolean processResults( boolean combatResults, String results, List data )
	{
		ResultProcessor.receivedClover = false;
		ResultProcessor.receivedDisassembledClover = false;

		if ( data == null && RequestLogger.isDebugging() )
		{
			RequestLogger.updateDebugLog( "Processing results..." );
		}

		ResultProcessor.registerNewItems( results );

		boolean requiresRefresh = processNormalResults( combatResults, results, data );

		if ( data == null )
		{
			KoLmafia.applyEffects();
		}

		// Save override data for new items and effects
		KoLmafia.saveDataOverride();

		return requiresRefresh;
	}

	public static boolean processNormalResults( boolean combatResults, String results, List data )
	{
		String plainTextResult = KoLConstants.ANYTAG_BUT_ITALIC_PATTERN.matcher( results ).replaceAll( KoLConstants.LINE_BREAK );

		if ( data == null )
		{
			ResultProcessor.processFamiliarWeightGain( plainTextResult );
		}

		StringTokenizer parsedResults = new StringTokenizer( plainTextResult, KoLConstants.LINE_BREAK );
		boolean shouldRefresh = false;

		while ( parsedResults.hasMoreTokens() )
		{
			shouldRefresh |= ResultProcessor.processNextResult( combatResults, parsedResults, data );
		}

		return shouldRefresh;
	}

	public static boolean processFamiliarWeightGain( final String results )
	{
		if ( results.indexOf( "gains a pound" ) != -1 ||
		     // The following are Haiku results
		     results.indexOf( "gained a pound" ) != -1 ||
		     results.indexOf( "puts on weight" ) != -1 ||
		     results.indexOf( "gaining weight" ) != -1 )
		{
			KoLCharacter.incrementFamilarWeight();

			FamiliarData familiar = KoLCharacter.getFamiliar();
			String fam1 = familiar.getName() + ", the " + familiar.getWeight() + " lb. " + familiar.getRace();

			String message = "Your familiar gains a pound: " + fam1;
			RequestLogger.printLine( message );
			RequestLogger.updateSessionLog( message	 );
			return true;
		}

		return false;
	}

	private static boolean processNextResult( boolean combatResults, StringTokenizer parsedResults, List data )
	{
		String lastToken = parsedResults.nextToken();
		
		// Skip bogus lead necklace drops from the Baby Bugged Bugbear
		
		if ( lastToken.equals( " Parse error (function not found) in arena.php line 2225" ) )
		{
			parsedResults.nextToken();
			return false;
		}

		// Skip skill acquisition - it's followed by a boldface
		// which makes the parser think it's found an item.

		if ( lastToken.indexOf( "You acquire a skill" ) != -1 ||
		     lastToken.indexOf( "You learn a skill" ) != -1 ||
		     lastToken.indexOf( "You gain a skill" ) != -1 ||
		     lastToken.indexOf( "You have learned a skill" ) != -1)
		{
			return false;
		}

		String acquisition = lastToken.trim();

		if ( acquisition.startsWith( "You acquire" ) )
		{
			if ( acquisition.indexOf( "clan trophy" ) != -1 )
			{
				return false;
			}

			if ( acquisition.indexOf( "effect" ) == -1 )
			{
				ResultProcessor.processItem( combatResults, parsedResults, acquisition, data );
				return false;
			}

			return ResultProcessor.processEffect( parsedResults, acquisition, data );
		}

		if ( acquisition.startsWith( "You lose an effect" ) || acquisition.startsWith( "You lose some of an effect" ) )
		{
			return ResultProcessor.processEffect( parsedResults, acquisition, data );
		}

		if ( lastToken.startsWith( "You gain" ) || lastToken.startsWith( "You lose " ) || lastToken.startsWith( "You spent " ) )
		{
			return ResultProcessor.processGainLoss( lastToken, data );
		}

		if ( lastToken.startsWith( "You discard" ) )
		{
			ResultProcessor.processDiscard( lastToken );
		}

		return false;
	}

	private static void processItem( boolean combatResults, StringTokenizer parsedResults, String acquisition, List data )
	{
		String item = parsedResults.nextToken();

		if ( acquisition.indexOf( "an item" ) != -1 )
		{
			AdventureResult result = ItemPool.get( item, 1 );

			if ( result.getItemId() == -1 )
			{
				RequestLogger.printLine( "Unrecognized item found: " + item );
			}

			ResultProcessor.processItem( combatResults, acquisition, result, data );
			return;
		}

		// The name of the item follows the number that appears after
		// the first index.

		int spaceIndex = item.indexOf( " " );

		String countString = "";
		String itemName;

		if ( spaceIndex != -1 )
		{
			countString = item.substring( 0, spaceIndex );
			itemName = item.substring( spaceIndex ).trim();
		}
		else
		{
			itemName = item;
		}

		if ( !StringUtilities.isNumeric( countString ) )
		{
			countString = "1";
			itemName = item;
		}

		int itemCount = StringUtilities.parseInt( countString );

		// If we got more than one, do substring matching. This might
		// allow recognition of an unknown (or changed) plural form.

		int itemId = ItemDatabase.getItemId( itemName, itemCount, itemCount > 1 );
		AdventureResult result;

		if ( itemId < 0 )
		{
			RequestLogger.printLine( "Unrecognized item found: " + item );
			result = AdventureResult.tallyItem( itemName, itemCount, false );
		}
		else
		{
			result = new AdventureResult( itemId, itemCount );
		}

		ResultProcessor.processItem( combatResults, acquisition, result, data );
	}

	public static void processItem( boolean combatResults, String acquisition, AdventureResult result, List data )
	{
		if ( data != null )
		{
			AdventureResult.addResultToList( data, result );
			return;
		}

		String message = acquisition + " " + result.toString();

		RequestLogger.printLine( message );
		if ( Preferences.getBoolean( "logAcquiredItems" ) )
		{
			RequestLogger.updateSessionLog( message );
		}

		ResultProcessor.processResult( combatResults, result );

		++ResultProcessor.itemSequenceCount;
	}

	private static boolean processEffect( StringTokenizer parsedResults, String acquisition, List data )
	{
		if ( data != null )
		{
			return false;
		}

		String effectName = parsedResults.nextToken();
                String message;

		if ( acquisition.startsWith( "You lose" ) )
		{
			message = acquisition + " " + effectName;
		}
		else
		{
			String lastToken = parsedResults.nextToken();
			message = acquisition + " " + effectName + " " + lastToken;
		}

		return ResultProcessor.processEffect( effectName, message );
	}

	public static boolean processEffect( String effectName, String message )
	{
		RequestLogger.printLine( message );

		if ( Preferences.getBoolean( "logStatusEffects" ) )
		{
			RequestLogger.updateSessionLog( message );
		}

		if ( message.startsWith( "You lose" ) )
		{
			AdventureResult result = EffectPool.get( effectName );
			AdventureResult.removeResultFromList( KoLConstants.recentEffects, result );
			AdventureResult.removeResultFromList( KoLConstants.activeEffects, result );

			return true;
		}

		if ( message.indexOf( "duration" ) != -1 )
		{
			Matcher m = INT_PATTERN.matcher( message );
			if ( m.find() )
			{
				int duration = StringUtilities.parseInt( m.group(1) );
				return parseEffect( effectName + " (" + duration + ")" );
			}
		}

		parseEffect( effectName );
		return false;
	}

	public static boolean processGainLoss( String lastToken, final List data )
	{
		int periodIndex = lastToken.indexOf( "." );
		if ( periodIndex != -1 )
		{
			lastToken = lastToken.substring( 0, periodIndex );
		}

		int parenIndex = lastToken.indexOf( "(" );
		if ( parenIndex != -1 )
		{
			lastToken = lastToken.substring( 0, parenIndex );
		}

		lastToken = lastToken.trim();

		if ( lastToken.indexOf( "Meat" ) != -1 )
		{
			return ResultProcessor.processMeat( lastToken, data );
		}

		if ( data != null )
		{
			return false;
		}

		if ( lastToken.startsWith( "You gain a" ) || lastToken.startsWith( "You gain some" ) )
		{
			RequestLogger.printLine( lastToken );
			if ( Preferences.getBoolean( "logStatGains" ) )
			{
				RequestLogger.updateSessionLog( lastToken );
			}

			return true;
		}

		return ResultProcessor.processStatGain( lastToken, data );
	}
	
	private static boolean possibleMeatDrop( int drop, int bonus )
	{
		float rate = (KoLCharacter.getMeatDropPercentAdjustment() + 100 + bonus) / 100.0f;
		return Math.floor( Math.ceil( drop / rate ) * rate ) == drop;
	}

	public static boolean processMeat( String text, boolean won, boolean nunnery )
	{
		if ( won && Preferences.getBoolean( "meatDropSpading" ) )
		{
			int drop = ResultProcessor.parseResult( text ).getCount();
			if ( !ResultProcessor.possibleMeatDrop( drop, 0 ) )
			{
				StringBuffer buf = new StringBuffer( " Alert - possible unknown meat bonus:" );
				if ( KoLCharacter.currentNumericModifier( Modifiers.SPORADIC_MEATDROP ) != 0.0f )
				{
					buf.append( " (sporadic!)" );
				}
				if ( KoLCharacter.currentNumericModifier( Modifiers.MEAT_BONUS ) != 0.0f )
				{
					buf.append( " (ant tool!)" );
				}
				for ( int i = 1; i <= 100 && buf.length() < 80; ++i )
				{
					if ( ResultProcessor.possibleMeatDrop( drop, i ) )
					{
						buf.append( " +" );
						buf.append( i );
					}
					if ( ResultProcessor.possibleMeatDrop( drop, -i ) )
					{
						buf.append( " -" );
						buf.append( i );
					}
				}
				RequestLogger.updateSessionLog( "Spade " + buf );
				buf.insert( 0, "<font color=green>\u2660" );
				buf.append( "</font>" );
				RequestLogger.printLine( buf.toString() );
			}
		}
		
		if ( won && nunnery )
		{
			AdventureResult result = ResultProcessor.parseResult( text );
			IslandDecorator.addNunneryMeat( result );
			return false;
		}

		return ResultProcessor.processMeat( text, null );
	}

	public static boolean processMeat( String lastToken, List data )
	{
		AdventureResult result = ResultProcessor.parseResult( lastToken );

		if ( data != null )
		{
			AdventureResult.addResultToList( data, result );
			return false;
		}

		// KoL can tell you that you lose meat - Leprechaun theft,
		// chewing bug vendors, etc. - but you can only lose as much
		// meat as you actually have in inventory.

		int amount = result.getCount();
		int available = KoLCharacter.getAvailableMeat();

		if ( amount < 0 && -amount > available )
		{
			amount = -available;
			lastToken = "You lose " + String.valueOf( -amount ) + " Meat";
			result = new AdventureResult( AdventureResult.MEAT, amount );
		}

		if ( amount == 0 )
		{
			return false;
		}

		RequestLogger.printLine( lastToken );
		if ( Preferences.getBoolean( "logGainMessages" ) )
		{
			RequestLogger.updateSessionLog( lastToken );
		}

		return ResultProcessor.processResult( result );
	}

	public static boolean processStatGain( String lastToken, List data )
	{
		if ( data != null )
		{
			return false;
		}

		AdventureResult result = ResultProcessor.parseResult( lastToken );

		RequestLogger.printLine( lastToken );
		if ( Preferences.getBoolean( "logStatGains" ) )
		{
			RequestLogger.updateSessionLog( lastToken );
		}

		return ResultProcessor.processResult( result );
	}

	private static void processDiscard( String lastToken )
	{
		Matcher matcher = ResultProcessor.DISCARD_PATTERN.matcher( lastToken );
		if ( matcher.find() )
		{
			AdventureResult item = new AdventureResult( matcher.group( 1 ), -1, false );
			AdventureResult.addResultToList( KoLConstants.inventory, item );
			AdventureResult.addResultToList( KoLConstants.tally, item );
			switch ( item.getItemId() )
			{
			case ItemPool.INSTANT_KARMA:
				Preferences.increment( "discardedKarma" );
				break;
			}
		}
	}

	public static AdventureResult parseResult( String result )
	{
		result = result.trim();

		if ( RequestLogger.isDebugging() )
		{
                        RequestLogger.updateDebugLog( "Parsing result: " + result );
                }

		try
		{
			return AdventureResult.parseResult( result );
		}
		catch ( Exception e )
		{
			// This should not happen. Therefore, print
			// a stack trace for debug purposes.

			StaticEntity.printStackTrace( e );
			return null;
		}
	}

	private static boolean parseEffect( String result )
	{
		if ( RequestLogger.isDebugging() )
		{
			RequestLogger.updateDebugLog( "Parsing effect: " + result );
		}

		Matcher m = ResultProcessor.TRAILING_INT_PATTERN.matcher( result );
		int count = 1;
		if ( m.matches() )
		{
			result = m.group( 1 );
			count = StringUtilities.parseInt( m.group( 2 ) );
		}

		return ResultProcessor.processResult( new AdventureResult( result, count, true ) );
	}

	/**
	 * Utility. The method used to process a result. By default, this will
	 * also add an adventure result to the tally. Use this whenever the
	 * result is already known and no additional parsing is needed.
	 *
	 * @param result Result to add to the running tally of adventure results
	 */

	public static boolean processResult( AdventureResult result )
	{
		return ResultProcessor.processResult( false, result );
	}

	public static boolean processResult( boolean combatResults, AdventureResult result )
	{
		// This should not happen, but punt if the result was null.

		if ( result == null )
		{
			return false;
		}

		if ( RequestLogger.isDebugging() )
		{
			RequestLogger.updateDebugLog( "Processing result: " + result );
		}

		String resultName = result.getName();
		boolean shouldRefresh = false;

		// Process the adventure result in this section; if
		// it's a status effect, then add it to the recent
		// effect list. Otherwise, add it to the tally.

		if ( result.isItem() )
		{
			AdventureResult.addResultToList( KoLConstants.tally, result );
		}
		else if ( result.isStatusEffect() )
		{
			shouldRefresh |= !KoLConstants.activeEffects.contains( result );
			AdventureResult.addResultToList( KoLConstants.recentEffects, result );
		}
		else if ( resultName.equals( AdventureResult.SUBSTATS ) )
		{
			AdventureResult.addResultToList( KoLConstants.tally, result );
		}
		else if ( resultName.equals( AdventureResult.MEAT ) )
		{
			AdventureResult.addResultToList( KoLConstants.tally, result );
			shouldRefresh = true;
		}
		else if ( resultName.equals( AdventureResult.ADV ) )
		{
			if ( result.getCount() < 0 )
			{
				TurnCounter.saveCounters();
				AdventureResult.addResultToList( KoLConstants.tally, result.getNegation() );
			}
		}
		else if ( resultName.equals( AdventureResult.CHOICE ) )
		{
			// Don't let ignored choices delay iteration
			KoLmafia.tookChoice = true;
		}

		ResultProcessor.tallyResult( result, true );

		if ( result.isItem() )
		{
			// Do special processing when you get certain items
			ResultProcessor.gainItem( combatResults, result );

			if ( GenericRequest.isBarrelSmash )
			{
				BarrelDecorator.gainItem( result );
			}

			if ( RequestLogger.getLastURLString().startsWith( "fight.php" ) )
			{
				int adv = KoLAdventure.lastAdventureId();
				if ( adv >= 178 && adv <= 181 )
				{
					CellarDecorator.gainItem( adv, result );
				}
			}

			if ( HermitRequest.isWorthlessItem( result.getItemId() ) )
			{
				result = HermitRequest.WORTHLESS_ITEM.getInstance( result.getCount() );
			}
		}

		// Now, if it's an actual stat gain, be sure to update the
		// list to reflect the current value of stats so far.

		if ( resultName.equals( AdventureResult.SUBSTATS ) )
		{
			int currentTest =
				KoLCharacter.calculateBasePoints( KoLCharacter.getTotalMuscle() ) - KoLmafia.initialStats[ 0 ];
			shouldRefresh |= AdventureResult.SESSION_FULLSTATS[ 0 ] != currentTest;
			AdventureResult.SESSION_FULLSTATS[ 0 ] = currentTest;

			currentTest =
				KoLCharacter.calculateBasePoints( KoLCharacter.getTotalMysticality() ) - KoLmafia.initialStats[ 1 ];
			shouldRefresh |= AdventureResult.SESSION_FULLSTATS[ 1 ] != currentTest;
			AdventureResult.SESSION_FULLSTATS[ 1 ] = currentTest;

			currentTest = KoLCharacter.calculateBasePoints( KoLCharacter.getTotalMoxie() ) - KoLmafia.initialStats[ 2 ];
			shouldRefresh |= AdventureResult.SESSION_FULLSTATS[ 2 ] != currentTest;
			AdventureResult.SESSION_FULLSTATS[ 2 ] = currentTest;

			if ( KoLConstants.tally.size() > 3 )
			{
				KoLConstants.tally.fireContentsChanged( KoLConstants.tally, 3, 3 );
			}
		}

		int conditionIndex = KoLConstants.conditions.indexOf( result );
		if ( conditionIndex != -1 )
		{
			// Process the adventure result through the conditions
			// list, removing it if the condition is satisfied.

			ResultProcessor.processCondition( result, resultName, conditionIndex );
		}

		return shouldRefresh;
	}

	public static boolean processItem( int itemId, int count )
	{
		return ResultProcessor.processResult( ItemPool.get( itemId, count ) );
	}

	public static void removeItem( int itemId )
	{
		AdventureResult ar = ItemPool.get( itemId, -1 );
		if ( KoLConstants.inventory.contains( ar ) )
		{
			ResultProcessor.processResult( ar );
		}
	}

	public static boolean processMeat( int amount )
	{
		return ResultProcessor.processResult( new AdventureResult( AdventureResult.MEAT, amount ) );
	}

	public static void processAdventuresLeft( int amount )
	{
		if ( amount != 0 )
		{
			KoLCharacter.setAdventuresLeft( KoLCharacter.getAdventuresLeft() + amount );
		}
	}

	public static void processAdventuresUsed( int amount )
	{
		if ( amount != 0 )
		{
			ResultProcessor.processResult( new AdventureResult( AdventureResult.ADV, -amount ) );
		}
	}

	private static void processCondition( AdventureResult result, String resultName, int conditionIndex )
	{
		if ( resultName.equals( AdventureResult.SUBSTATS ) )
		{
			// If the condition is a substat condition, then zero out the
			// appropriate count and remove if all counts dropped to zero.

			for ( int i = 0; i < 3; ++i )
			{
				if ( AdventureResult.CONDITION_SUBSTATS[ i ] == 0 )
				{
					continue;
				}

				AdventureResult.CONDITION_SUBSTATS[ i ] =
					Math.max( 0, AdventureResult.CONDITION_SUBSTATS[ i ] - result.getCount( i ) );
			}

			if ( AdventureResult.CONDITION_SUBSTATS_RESULT.getCount() == 0 )
			{
				KoLConstants.conditions.remove( conditionIndex );
			}
			else
			{
				KoLConstants.conditions.fireContentsChanged(
					KoLConstants.conditions, conditionIndex, conditionIndex );
			}
		}
		else
		{
			AdventureResult condition = (AdventureResult) KoLConstants.conditions.get( conditionIndex );
			condition = condition.getInstance( condition.getCount() - result.getCount() );

			if ( condition.getCount() <= 0 )
			{
				KoLConstants.conditions.remove( conditionIndex );
			}
			else
			{
				KoLConstants.conditions.set( conditionIndex, condition );
			}
		}
	}

	/**
	 * Processes a result received through adventuring. This places items
	 * inside of inventories and lots of other good stuff.
	 */

	public static final void tallyResult( final AdventureResult result, final boolean updateCalculatedLists )
	{
		// Treat the result as normal from this point forward.
		// Figure out which list the skill should be added to
		// and add it to that list.

		String resultName = result.getName();
		if ( resultName == null )
		{
			return;
		}

		if ( result.isItem() )
		{
			AdventureResult.addResultToList( KoLConstants.inventory, result );

			if ( updateCalculatedLists )
			{
				EquipmentManager.processResult( result );

				boolean shouldRefresh = false;
				List uses = ConcoctionDatabase.getKnownUses( result );

				for ( int i = 0; i < uses.size() && !shouldRefresh; ++i )
				{
					shouldRefresh =
						ConcoctionDatabase.isPermittedMethod( ConcoctionDatabase.getMixingMethod( ( (AdventureResult) uses.get( i ) ).getItemId() ) );
				}

				if ( !shouldRefresh )
				{
					switch ( ItemDatabase.getConsumptionType( result.getItemId() ) )
					{
					case KoLConstants.CONSUME_EAT:
					case KoLConstants.CONSUME_DRINK:
					case KoLConstants.CONSUME_USE:
					case KoLConstants.CONSUME_MULTIPLE:
					case KoLConstants.CONSUME_FOOD_HELPER:
					case KoLConstants.CONSUME_DRINK_HELPER:
						shouldRefresh = true;
					}
				}


				if ( !shouldRefresh )
				{
					switch ( result.getItemId() )
					{
					// Items that affect creatability of other items, but
					// aren't explicitly listed in their recipes:
					case ItemPool.WORTHLESS_TRINKET:
					case ItemPool.WORTHLESS_GEWGAW:
					case ItemPool.WORTHLESS_KNICK_KNACK:

					// Interchangeable ingredients, which might have been missed
					// by the getKnownUses check because the recipes are set to
					// use the other possible ingredient:
					case ItemPool.SCHLITZ:
					case ItemPool.WILLER:
					case ItemPool.KETCHUP:
					case ItemPool.CATSUP:
					case ItemPool.DYSPEPSI_COLA:
					case ItemPool.CLOACA_COLA:
					case ItemPool.TITANIUM_UMBRELLA:
					case ItemPool.GOATSKIN_UMBRELLA:
						shouldRefresh = true;
					}
				}

				if ( shouldRefresh )
				{
					ConcoctionDatabase.refreshConcoctions();
				}
			}
		}
		else if ( resultName.equals( AdventureResult.HP ) )
		{
			KoLCharacter.setHP(
				KoLCharacter.getCurrentHP() + result.getCount(), KoLCharacter.getMaximumHP(),
				KoLCharacter.getBaseMaxHP() );
		}
		else if ( resultName.equals( AdventureResult.MP ) )
		{
			KoLCharacter.setMP(
				KoLCharacter.getCurrentMP() + result.getCount(), KoLCharacter.getMaximumMP(),
				KoLCharacter.getBaseMaxMP() );
		}
		else if ( resultName.equals( AdventureResult.MEAT ) )
		{
			KoLCharacter.setAvailableMeat( KoLCharacter.getAvailableMeat() + result.getCount() );
			if ( updateCalculatedLists )
			{
				ConcoctionDatabase.refreshConcoctions();
			}
		}
		else if ( resultName.equals( AdventureResult.ADV ) )
		{
			if ( result.getCount() < 0 )
			{
				AdventureResult[] effectsArray = new AdventureResult[ KoLConstants.activeEffects.size() ];
				KoLConstants.activeEffects.toArray( effectsArray );

				for ( int i = effectsArray.length - 1; i >= 0; --i )
				{
					AdventureResult effect = effectsArray[ i ];
					int duration = effect.getCount();
					if ( duration == Integer.MAX_VALUE )
					{
						// Intrinsic effect
					}
					else if ( duration + result.getCount() <= 0 )
					{
						KoLConstants.activeEffects.remove( i );
					}
					else
					{
						KoLConstants.activeEffects.set( i, effect.getInstance( effect.getCount() + result.getCount() ) );
					}
				}

				KoLCharacter.setCurrentRun( KoLCharacter.getCurrentRun() - result.getCount() );
			}
		}
		else if ( resultName.equals( AdventureResult.DRUNK ) )
		{
			KoLCharacter.setInebriety( KoLCharacter.getInebriety() + result.getCount() );
		}
		else if ( resultName.equals( AdventureResult.SUBSTATS ) )
		{
			if ( result.isMuscleGain() )
			{
				KoLCharacter.incrementTotalMuscle( result.getCount() );
			}
			else if ( result.isMysticalityGain() )
			{
				KoLCharacter.incrementTotalMysticality( result.getCount() );
			}
			else if ( result.isMoxieGain() )
			{
				KoLCharacter.incrementTotalMoxie( result.getCount() );
			}
		}
	}

	private static void gainItem( boolean combatResults, AdventureResult result )
	{
		// All results, whether positive or negative, are
		// handled here.

		switch ( result.getItemId() )
		{
		case ItemPool.GG_TICKET:
			ConcoctionDatabase.refreshConcoctions();
			// Fall through
		case ItemPool.LUCRE:
		case ItemPool.SAND_DOLLAR:
		case ItemPool.CRIMBUCK:
			CoinmastersFrame.externalUpdate();
			break;
		}

		// From here on out, only positive results are handled.
		// This is to reduce the number of 'if' checks.

		if ( result.getCount() < 0 )
		{
			return;
		}

		switch ( result.getItemId() )
		{
		case ItemPool.ROASTED_MARSHMALLOW:
			// Special Yuletide adventures
			if ( KoLAdventure.lastAdventureId() == 163 )
			{
				ResultProcessor.removeItem( ItemPool.MARSHMALLOW );
			}
			break;

		// Sticker weapons may have been folded from the other form
		case ItemPool.STICKER_SWORD:
			ResultProcessor.removeItem( ItemPool.STICKER_CROSSBOW );
			break;

		case ItemPool.STICKER_CROSSBOW:
			ResultProcessor.removeItem( ItemPool.STICKER_SWORD );
			break;

		case ItemPool.SOCK:
			// If you get a S.O.C.K., you lose all the Immateria
			ResultProcessor.processItem( ItemPool.TISSUE_PAPER_IMMATERIA, -1 );
			ResultProcessor.processItem( ItemPool.TIN_FOIL_IMMATERIA, -1 );
			ResultProcessor.processItem( ItemPool.GAUZE_IMMATERIA, -1 );
			ResultProcessor.processItem( ItemPool.PLASTIC_WRAP_IMMATERIA, -1 );
			break;

		case ItemPool.MACGUFFIN_DIARY:
			// If you get your father's MacGuffin diary, you lose
			// your forged identification documents
			ResultProcessor.processItem( ItemPool.FORGED_ID_DOCUMENTS, -1 );
			break;

		case ItemPool.PALINDROME_BOOK:
			// If you get "I Love Me, Vol. I", you lose (some of)
			// the items you put on the shelves
			ResultProcessor.processItem( ItemPool.PHOTOGRAPH_OF_GOD, -1 );
			ResultProcessor.processItem( ItemPool.HARD_ROCK_CANDY, -1 );
			ResultProcessor.processItem( ItemPool.OSTRICH_EGG, -1 );
			break;

		case ItemPool.MEGA_GEM:
			// If you get the Mega Gem, you lose your wet stunt nut
			// stew
			ResultProcessor.processItem( ItemPool.WET_STUNT_NUT_STEW, -1 );
			break;

		case ItemPool.CONFETTI:
			// If you get the confetti, you lose the Holy MacGuffin
			if ( KoLConstants.inventory.contains( ItemPool.get( ItemPool.HOLY_MACGUFFIN, 1 ) ) )
			{
				ResultProcessor.processItem( ItemPool.HOLY_MACGUFFIN, -1 );
			}
			break;

		case ItemPool.STEEL_STOMACH:
		case ItemPool.STEEL_LIVER:
		case ItemPool.STEEL_SPLEEN:
			// When you get a steel item, you lose Azazel's items
			ResultProcessor.processItem( ItemPool.AZAZELS_UNICORN, -1 );
			ResultProcessor.processItem( ItemPool.AZAZELS_LOLLYPOP, -1 );
			ResultProcessor.processItem( ItemPool.AZAZELS_TUTU, -1 );
			break;

		case ItemPool.SPOOKYRAVEN_SPECTACLES:
			// When you get the spectacles, identify dusty bottles.
			// If you have not ascended, you need to put them on -
			// which we leave to the player - but otherwise, they
			// work from inventory.

			// Temporary bandaid until we figure out how this gets
			// out of synch. Not that it's harmful to do this.
			Preferences.setInteger( "lastDustyBottleReset", -1 );

			if ( KoLCharacter.getAscensions() > 0 )
			{
				ItemDatabase.identifyDustyBottles();
			}
			break;

		case ItemPool.MOLYBDENUM_MAGNET:
			// When you get the molybdenum magnet, tell quest handler
			IslandDecorator.startJunkyardQuest();
			break;

		case ItemPool.MOLYBDENUM_HAMMER:
		case ItemPool.MOLYBDENUM_SCREWDRIVER:
		case ItemPool.MOLYBDENUM_PLIERS:
		case ItemPool.MOLYBDENUM_WRENCH:
			// When you get a molybdenum item, tell quest handler
			IslandDecorator.resetGremlinTool();
			break;

		case ItemPool.OVERCHARGED_POWER_SPHERE:
		case ItemPool.EL_VIBRATO_HELMET:
		case ItemPool.EL_VIBRATO_SPEAR:
		case ItemPool.EL_VIBRATO_PANTS:
			if ( combatResults ) ResultProcessor.removeItem( ItemPool.POWER_SPHERE );
			break;

		case ItemPool.BROKEN_DRONE:
			if ( combatResults ) ResultProcessor.removeItem( ItemPool.DRONE );
			break;

		case ItemPool.REPAIRED_DRONE:
			if ( combatResults ) ResultProcessor.removeItem( ItemPool.BROKEN_DRONE );
			break;

		case ItemPool.AUGMENTED_DRONE:
			if ( combatResults ) ResultProcessor.removeItem( ItemPool.REPAIRED_DRONE );
			break;

		case ItemPool.TRAPEZOID:
			ResultProcessor.removeItem( ItemPool.POWER_SPHERE );
			break;

		case ItemPool.CITADEL_SATCHEL:
			ResultProcessor.processMeat( -300 );
			break;

		case ItemPool.HAROLDS_BELL:
			ResultProcessor.processItem( ItemPool.HAROLDS_HAMMER, -1 );
			break;

		 // These update the session results for the item swapping in
		 // the Gnome's Going Postal quest.

		case ItemPool.REALLY_BIG_TINY_HOUSE:
			ResultProcessor.processItem( ItemPool.RED_PAPER_CLIP, -1 );
			break;

		case ItemPool.NONESSENTIAL_AMULET:
			ResultProcessor.processItem( ItemPool.REALLY_BIG_TINY_HOUSE, -1 );
			break;

		case ItemPool.WHITE_WINE_VINAIGRETTE:
			ResultProcessor.processItem( ItemPool.NONESSENTIAL_AMULET, -1 );
			break;
		case ItemPool.CURIOUSLY_SHINY_AX:
			ResultProcessor.processItem( ItemPool.WHITE_WINE_VINAIGRETTE, -1 );
			break;
		case ItemPool.CUP_OF_STRONG_TEA:
			ResultProcessor.processItem( ItemPool.CURIOUSLY_SHINY_AX, -1 );
			break;
		case ItemPool.MARINATED_STAKES:
			ResultProcessor.processItem( ItemPool.CUP_OF_STRONG_TEA, -1 );
			break;
		case ItemPool.KNOB_BUTTER:
			ResultProcessor.processItem( ItemPool.MARINATED_STAKES, -1 );
			break;
		case ItemPool.VIAL_OF_ECTOPLASM:
			ResultProcessor.processItem( ItemPool.KNOB_BUTTER, -1 );
			break;
		case ItemPool.BOOCK_OF_MAGIKS:
			ResultProcessor.processItem( ItemPool.VIAL_OF_ECTOPLASM, -1 );
			break;
		case ItemPool.EZ_PLAY_HARMONICA_BOOK:
			ResultProcessor.processItem( ItemPool.BOOCK_OF_MAGIKS, -1 );
			break;
		case ItemPool.FINGERLESS_HOBO_GLOVES:
			ResultProcessor.processItem( ItemPool.EZ_PLAY_HARMONICA_BOOK, -1 );
			break;
		case ItemPool.CHOMSKYS_COMICS:
			ResultProcessor.processItem( ItemPool.FINGERLESS_HOBO_GLOVES, -1 );
			break;

		case ItemPool.GNOME_DEMODULIZER:
			ResultProcessor.removeItem( ItemPool.CHOMSKYS_COMICS );
			break;

		case ItemPool.MUS_MANUAL:
		case ItemPool.MYS_MANUAL:
		case ItemPool.MOX_MANUAL:
			ResultProcessor.processItem( ItemPool.DUSTY_BOOK, -1 );
			ResultProcessor.processItem( ItemPool.FERNSWARTHYS_KEY, -1 );
			break;

		case ItemPool.FRATHOUSE_BLUEPRINTS:
			ResultProcessor.processItem( ItemPool.CARONCH_MAP, -1 );
			ResultProcessor.processItem( ItemPool.CARONCH_NASTY_BOOTY, -1 );
			break;

		case ItemPool.TEN_LEAF_CLOVER:
			ResultProcessor.receivedClover = true;
			break;

		case ItemPool.DISASSEMBLED_CLOVER:
			ResultProcessor.receivedDisassembledClover = true;
			break;

		case ItemPool.BATSKIN_BELT:
		case ItemPool.DRAGONBONE_BELT_BUCKLE:

			CreateItemRequest beltCreator = CreateItemRequest.getInstance( ItemPool.BADASS_BELT );
			// getQuantityPossible() should take meat paste or
			// Muscle Sign into account
			if ( beltCreator.getQuantityPossible() > 0 )
			{
				beltCreator.setQuantityNeeded( 1 );
				RequestThread.postRequest( beltCreator );
			}
			break;

		case ItemPool.QUANTUM_EGG:

			CreateItemRequest rowboatCreator = CreateItemRequest.getInstance( ItemPool.ROWBOAT );
			// getQuantityPossible() should take meat paste or
			// Muscle Sign into account
			if ( rowboatCreator.getQuantityPossible() > 0 )
			{
				rowboatCreator.setQuantityNeeded( 1 );
				RequestThread.postRequest( rowboatCreator );
			}
			break;

		case ItemPool.WORM_RIDING_HOOKS:
			ResultProcessor.processItem( ItemPool.WORM_RIDING_MANUAL_1, -1 );
			ResultProcessor.processItem( ItemPool.WORM_RIDING_MANUAL_2, -1 );
			ResultProcessor.processItem( ItemPool.WORM_RIDING_MANUAL_3_15, -1 );
			break;

		case ItemPool.DAS_BOOT:
			ResultProcessor.removeItem( ItemPool.DAMP_OLD_BOOT );
			break;

		case ItemPool.PREGNANT_FLAMING_MUSHROOM:
			ResultProcessor.processItem( ItemPool.FLAMING_MUSHROOM, -1 );
			break;

		case ItemPool.PREGNANT_FROZEN_MUSHROOM:
			ResultProcessor.processItem( ItemPool.FROZEN_MUSHROOM, -1 );
			break;

		case ItemPool.PREGNANT_STINKY_MUSHROOM:
			ResultProcessor.processItem( ItemPool.STINKY_MUSHROOM, -1 );
			break;

		case ItemPool.GRANDMAS_MAP:
			ResultProcessor.processItem( ItemPool.GRANDMAS_NOTE, -1 );
			ResultProcessor.processItem( ItemPool.FUCHSIA_YARN, -1 );
			ResultProcessor.processItem( ItemPool.CHARTREUSE_YARN, -1 );
			break;

		case ItemPool.SMALL_STONE_BLOCK:
			ResultProcessor.processItem( ItemPool.IRON_KEY, -1 );
			break;

		case ItemPool.CRIMBOMINATION_CONTRAPTION:
			ResultProcessor.removeItem( ItemPool.WRENCH_HANDLE );
			ResultProcessor.removeItem( ItemPool.HEADLESS_BOLTS );
			ResultProcessor.removeItem( ItemPool.AGITPROP_INK );
			ResultProcessor.removeItem( ItemPool.HANDFUL_OF_WIRES );
			ResultProcessor.removeItem( ItemPool.CHUNK_OF_CEMENT );
			ResultProcessor.removeItem( ItemPool.PENGUIN_GRAPPLING_HOOK );
			ResultProcessor.removeItem( ItemPool.CARDBOARD_ELF_EAR );
			ResultProcessor.removeItem( ItemPool.SPIRALING_SHAPE );
			break;

		case ItemPool.HELLSEAL_DISGUISE:
			ResultProcessor.processItem( ItemPool.HELLSEAL_HIDE, -6 );
			ResultProcessor.processItem( ItemPool.HELLSEAL_BRAIN, -6 );
			ResultProcessor.processItem( ItemPool.HELLSEAL_SINEW, -6 );
			break;

		case ItemPool.DECODED_CULT_DOCUMENTS:
			ResultProcessor.processItem( ItemPool.CULT_MEMO, -5 );
			break;

		case ItemPool.PIXEL_CHAIN_WHIP:
			if ( combatResults )
			{
				// If you acquire a pixel chain whip, you lose
				// the pixel whip you were wielding and wield
				// the chain whip in its place.

				AdventureResult whip = ItemPool.get( ItemPool.PIXEL_WHIP, 1 );
				EquipmentManager.transformEquipment( whip, result );
				ResultProcessor.processItem( result.getItemId(), -1 );
			}
			break;

		case ItemPool.PIXEL_MORNING_STAR:
			if ( combatResults )
			{
				// If you acquire a pixel morning star, you
				// lose the pixel chain whip you were wielding
				// and wield the morning star in its place.

				AdventureResult chainWhip = ItemPool.get( ItemPool.PIXEL_CHAIN_WHIP, 1 );
				EquipmentManager.transformEquipment( chainWhip, result );
				ResultProcessor.processItem( result.getItemId(), -1 );
			}
			break;

		case ItemPool.REFLECTION_OF_MAP:
			if ( combatResults )
			{
				int current = Preferences.getInteger( "pendingMapReflections" );
				current = Math.max( 0, current - 1);
				Preferences.setInteger( "pendingMapReflections", current );
			}
			break;

		case ItemPool.GONG:
			if ( combatResults )
			{
				Preferences.increment( "_gongDrops", 1 );
			}
			break;

		case ItemPool.ABSINTHE:
			if ( combatResults )
			{
				Preferences.increment( "_absintheDrops", 1 );
			}
			break;

		case ItemPool.ASTRAL_MUSHROOM:
			if ( combatResults )
			{
				Preferences.increment( "_astralDrops", 1 );
			}
			break;

		case ItemPool.AGUA_DE_VIDA:
			if ( combatResults )
			{
				Preferences.increment( "_aguaDrops", 1 );
			}
			break;

		case ItemPool.GG_TOKEN:
			if ( combatResults )
			{
				Preferences.increment( "_tokenDrops", 1 );
			}
			break;
		}
	}

	/**
	 * Handle lots of items being received at once (specifically, from emptying Hangk's),
	 * deferring updates to the end as much as possible.
	 */
	public static void processBulkItems( Object[] items )
	{
		if ( items.length == 0 )
		{
			return;
		}

		if ( RequestLogger.isDebugging() )
		{
			RequestLogger.updateDebugLog( "Processing bulk items" );
		}

		KoLmafia.updateDisplay( "Processing, this may take a while..." );
		for ( int i = 0; i < items.length; ++i )
		{
			AdventureResult result = (AdventureResult) items[ i ];

			// Skip adding to tally, since you haven't really
			// gained these items - merely moved them around.

			//AdventureResult.addResultToList( KoLConstants.tally, result );

			// Skip gainItem's processing, which is mostly
			// concerned with quest items that couldn't be in
			// Hangk's anyway.

			//ResultProcessor.gainItem( result );

			AdventureResult.addResultToList( KoLConstants.inventory, result );
			EquipmentManager.processResult( result );

			// Skip conditions handling, since this can't happen
			// during an adventure request, and therefore the
			// conditions will be rechecked.
		}

		// Assume that at least one item in the list required each of
		// these updates:

		CoinmastersFrame.externalUpdate();
		ConcoctionDatabase.refreshConcoctions();

		KoLmafia.updateDisplay( "Processing complete." );
	}
}
