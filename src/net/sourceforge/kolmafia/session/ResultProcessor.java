/**
 * Copyright (c) 2005-2009, KoLmafia development team
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
import net.sourceforge.kolmafia.KoLAdventure;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;
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
import net.sourceforge.kolmafia.request.UseItemRequest;
import net.sourceforge.kolmafia.swingui.CoinmastersFrame;
import net.sourceforge.kolmafia.utilities.StringUtilities;
import net.sourceforge.kolmafia.webui.BarrelDecorator;
import net.sourceforge.kolmafia.webui.CellarDecorator;
import net.sourceforge.kolmafia.webui.IslandDecorator;

public class ResultProcessor
{
	private static Pattern FUMBLE_PATTERN =
		Pattern.compile( "You drop your .*? on your .*?, doing ([\\d,]+) damage" );
	private static Pattern STABBAT_PATTERN = Pattern.compile( " stabs you for ([\\d,]+) damage" );
	private static Pattern CARBS_PATTERN =
		Pattern.compile( "some of your blood, to the tune of ([\\d,]+) damage" );
	private static Pattern DISCARD_PATTERN = Pattern.compile( "You discard your (.*?)\\." );
	private static Pattern HAIKU_PATTERN = Pattern.compile( "<[tT]able><tr><td[^>]*?><img[^>]*/([^/]*\\.gif)[^>]*?('descitem\\((.*?)\\)')?></td>(<td[^>]*><[tT]able><tr>)?<td[^>]*?>(.*?)</td>(</tr></table></td>)?</tr></table>" );
	private static Pattern INT_PATTERN = Pattern.compile( ".*?([\\d]+).*" );

	private static AdventureResult haikuEffect = EffectPool.get( EffectPool.HAIKU_STATE_OF_MIND );
	private static boolean receivedClover = false;
	
	// This number will change every time an item is processed, and can be used by other code
	// to tell if an item is received, without necessarily knowing which item it was.
	public static int itemSequenceCount = 0;
	
	public static boolean shouldDisassembleClovers( String formURLString )
	{
		if ( !ResultProcessor.receivedClover || FightRequest.getCurrentRound() != 0 || !Preferences.getBoolean( "cloverProtectActive" ) )
		{
			return false;
		}

		return formURLString.startsWith( "adventure.php" ) ||
			formURLString.startsWith( "hermit.php" ) ||
			formURLString.startsWith( "mallstore.php" ) ||
			formURLString.startsWith( "barrel.php" ) ||
			formURLString.indexOf( "whichitem=553" ) != -1;
	}
	
	public static boolean receivedClover()
	{
		return ResultProcessor.receivedClover;
	}
	
	public static boolean processResults( String results )
	{
		return ResultProcessor.processResults( results, null );
	}

	public static boolean processResults( String results, List data )
	{
		ResultProcessor.receivedClover = false;
		
		if ( data == null )
		{
			RequestLogger.updateDebugLog( "Processing results..." );
		}

		boolean requiresRefresh = haveHaikuResults() ?
			processHaikuResults( results, data ) :
			processNormalResults( results, data );

		if ( data == null )
		{
			KoLmafia.applyEffects();
		}

		return requiresRefresh;
	}

	private static boolean processNormalResults( String results, List data )
	{
		if ( data == null && results.indexOf( "gains a pound" ) != -1 )
		{
			KoLCharacter.incrementFamilarWeight();

			RequestLogger.updateSessionLog();
			RequestLogger.updateSessionLog( "familiar " + KoLCharacter.getFamiliar() );
			RequestLogger.updateSessionLog();
		}

		String plainTextResult = KoLConstants.ANYTAG_PATTERN.matcher( results ).replaceAll( KoLConstants.LINE_BREAK );

		if ( data == null )
		{
			ResultProcessor.processFumble( plainTextResult );
		}

		StringTokenizer parsedResults = new StringTokenizer( plainTextResult, KoLConstants.LINE_BREAK );
		boolean shouldRefresh = false;

		while ( parsedResults.hasMoreTokens() )
		{
			shouldRefresh |= ResultProcessor.processNextResult( parsedResults, data );
		}

		return shouldRefresh;
	}

	private static boolean haveHaikuResults()
	{
		return KoLAdventure.lastAdventureId() == 138 ||
			KoLConstants.activeEffects.contains( haikuEffect );
	}

	private static boolean processHaikuResults( String results, List data )
	{
		Matcher matcher = HAIKU_PATTERN.matcher( results );
		if ( !matcher.find() )
		{
			return false;
		}

		boolean shouldRefresh = false;
		String familiar = KoLCharacter.getFamiliar().getImageLocation();

		do
		{
			String image = matcher.group(1);
			String descid = matcher.group(3);
			String haiku = matcher.group(5);

			if ( descid != null )
			{
				// Found an item
				int itemId = ItemDatabase.getItemIdFromDescription( descid );
				AdventureResult result = ItemPool.get( itemId, 1 );
				ResultProcessor.processItem( "You acquire an item:", result, data );

				continue;
			}

			if ( data == null && image.equals( familiar ) )
			{
				if ( haiku.indexOf( "gains a pound" ) != -1 )
				{
					KoLCharacter.incrementFamilarWeight();

					RequestLogger.updateSessionLog();
					RequestLogger.updateSessionLog( "familiar " + KoLCharacter.getFamiliar() );
					RequestLogger.updateSessionLog();
				}
				continue;
			}

			Matcher m = INT_PATTERN.matcher( haiku );
			if ( !m.find() )
			{
				continue;
			}

			String points = m.group(1);

			if ( image.equals( "meat.gif" ) )
			{
				String message = "You gain " + points + " Meat";
				ResultProcessor.processStatGain( message, data );
				shouldRefresh = true;
				continue;
			}

			if ( data != null )
			{
				continue;
			}

			if ( image.equals( "hp.gif" ) )
			{
				// Lost HP
				String message = "You lose " + points + " hit points";

				RequestLogger.printLine( message );

				if ( Preferences.getBoolean( "logGainMessages" ) )
				{
					RequestLogger.updateSessionLog( message );
				}

				parseResult( message );
				continue;
			}

			if ( image.equals( "strboost.gif" ) )
			{
				String message = "You gain " + points + " Strongness";
				shouldRefresh |= ResultProcessor.processStatGain( message, data );
				continue;
			}

			if ( image.equals( "snowflakes.gif" ) )
			{
				String message = "You gain " + points + " Magicalness";
				shouldRefresh |= ResultProcessor.processStatGain( message, data );
				continue;
			}

			if ( image.equals( "wink.gif" ) )
			{
				String message = "You gain " + points + " Roguishness";
				shouldRefresh |= ResultProcessor.processStatGain( message, data );
				continue;
			}
		} while ( matcher.find() );

		return shouldRefresh;
	}

	private static void processFumble( String plainTextResult )
	{
		Matcher damageMatcher = ResultProcessor.FUMBLE_PATTERN.matcher( plainTextResult );

		while ( damageMatcher.find() )
		{
			String message = "You lose " + damageMatcher.group( 1 ) + " hit points";

			RequestLogger.printLine( message );

			if ( Preferences.getBoolean( "logGainMessages" ) )
			{
				RequestLogger.updateSessionLog( message );
			}

			parseResult( message );
		}

		if ( !KoLCharacter.isUsingStabBat() )
		{
			return;
		}

		damageMatcher = ResultProcessor.STABBAT_PATTERN.matcher( plainTextResult );

		if ( damageMatcher.find() )
		{
			String message = "You lose " + damageMatcher.group( 1 ) + " hit points";

			RequestLogger.printLine( message );

			if ( Preferences.getBoolean( "logGainMessages" ) )
			{
				RequestLogger.updateSessionLog( message );
			}

			parseResult( message );
		}

		damageMatcher = ResultProcessor.CARBS_PATTERN.matcher( plainTextResult );

		if ( damageMatcher.find() )
		{
			String message = "You lose " + damageMatcher.group( 1 ) + " hit points";

			RequestLogger.printLine( message );

			if ( Preferences.getBoolean( "logGainMessages" ) )
			{
				RequestLogger.updateSessionLog( message );
			}

			parseResult( message );
		}
	}

	private static boolean processNextResult( StringTokenizer parsedResults, List data )
	{
		String lastToken = parsedResults.nextToken();

		// Skip skill acquisition - it's followed by a boldface
		// which makes the parser think it's found an item.

		if ( lastToken.indexOf( "You acquire a skill" ) != -1 ||
		     lastToken.indexOf( "You learn a skill" ) != -1 ||
		     lastToken.indexOf( "You have learned a skill" ) != -1)
		{
			return false;
		}

		String acquisition = lastToken.trim();

		// The following only under Can Has Cyborger. Sigh.

		if ( acquisition.startsWith( "O hai, I made dis" ) )
		{
			acquisition = "You acquire an item:";
		}

		if ( acquisition.startsWith( "You acquire" ) )
		{
			if ( acquisition.indexOf( "clan trophy" ) != -1 )
			{
				return false;
			}

			if ( acquisition.indexOf( "effect" ) == -1 )
			{
				ResultProcessor.processItem( parsedResults, acquisition, data );
				return false;
			}

			if ( data == null )
			{
				return ResultProcessor.processEffect( parsedResults, acquisition, data );
			}

			return false;
		}

		if ( acquisition.startsWith( "You lose an effect" ) )
		{
			if ( data == null )
			{
				return ResultProcessor.processEffect( parsedResults, acquisition, data );
			}

			return false;
		}

		// The following only under Can Has Cyborger

		if ( lastToken.startsWith( "You gets" ) )
		{
			lastToken = "You gain" + lastToken.substring( 8 );
		}
		else if ( lastToken.startsWith( "You can has" ) )
		{
			lastToken = "You gain" + lastToken.substring( 11 );
		}

		if ( lastToken.startsWith( "You gain" ) || lastToken.startsWith( "You lose " ) )
		{
			return ResultProcessor.processStatGain( lastToken, data );
		}

		if ( lastToken.startsWith( "You discard" ) )
		{
			ResultProcessor.processDiscard( lastToken );
		}

		return false;
	}

	private static void processItem( StringTokenizer parsedResults, String acquisition, List data )
	{
		String item = parsedResults.nextToken();

		if ( acquisition.indexOf( "an item" ) != -1 )
		{
			AdventureResult result = ItemPool.get( item, 1 );
			ResultProcessor.processItem( acquisition, result, data );
			return;
		}

		// The name of the item follows the number
		// that appears after the first index.

		String countString = item.split( " " )[ 0 ];
		int spaceIndex = item.indexOf( " " );

		String itemName = spaceIndex == -1 ? item : item.substring( spaceIndex ).trim();
		boolean isNumeric = spaceIndex != -1;

		for ( int i = 0; isNumeric && i < countString.length(); ++i )
		{
			isNumeric &= Character.isDigit( countString.charAt( i ) ) || countString.charAt( i ) == ',';
		}

		if ( !isNumeric )
		{
			countString = "1";
		}

		int itemCount = StringUtilities.parseInt( countString );
		AdventureResult result = ItemPool.get( ItemDatabase.getItemId( itemName, itemCount ), itemCount );

		ResultProcessor.processItem( acquisition, result, data );
	}

	private static void processItem( String acquisition, AdventureResult result, List data )
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

		processResult( result );
		++ResultProcessor.itemSequenceCount;
	}

	private static boolean processEffect( StringTokenizer parsedResults, String acquisition, List data )
	{
		String effectName = parsedResults.nextToken();

		if ( acquisition.startsWith( "You lose" ) )
		{
			String message = acquisition + " " + effectName;

			RequestLogger.printLine( message );
			if ( Preferences.getBoolean( "logStatusEffects" ) )
			{
				RequestLogger.updateSessionLog( message );
			}

			AdventureResult result = EffectPool.get( effectName );
			AdventureResult.removeResultFromList( KoLConstants.recentEffects, result );
			AdventureResult.removeResultFromList( KoLConstants.activeEffects, result );

			return true;
		}

		String lastToken = parsedResults.nextToken();
		String message = acquisition + " " + effectName + " " + lastToken;

		RequestLogger.printLine( message );

		if ( Preferences.getBoolean( "logStatusEffects" ) )
		{
			RequestLogger.updateSessionLog( message );
		}

		if ( lastToken.indexOf( "duration" ) == -1 )
		{
			parseEffect( effectName );
			return false;
		}

		String duration = lastToken.substring( 11, lastToken.length() - 11 ).trim();
		return parseEffect( effectName + " (" + duration + ")" );
	}

	private static AdventureResult parseResult( String result )
	{
		String trimResult = result.trim();
		RequestLogger.updateDebugLog( "Parsing result: " + trimResult );

		try
		{
			return AdventureResult.parseResult( trimResult );
		}
		catch ( Exception e )
		{
			// This should not happen. Therefore, print
			// a stack trace for debug purposes.

			StaticEntity.printStackTrace( e );
			return null;
		}
	}

	private static boolean processStatGain( String lastToken, List data )
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

		if ( data == null && lastToken.indexOf( "level" ) == -1 )
		{
			RequestLogger.printLine( lastToken );
		}

		if ( lastToken.startsWith( "You gain a" ) || lastToken.startsWith( "You gain some" ) )
		{
			return true;
		}

		AdventureResult result = ResultProcessor.parseResult( lastToken );
		if ( data != null )
		{
			if ( result.getName().equals( AdventureResult.MEAT ) )
			{
				AdventureResult.addResultToList( data, result );
			}

			return false;
		}

		boolean shouldRefresh =	 processResult( result );

		if ( result.getName().equals( AdventureResult.SUBSTATS ) )
		{
			if ( Preferences.getBoolean( "logStatGains" ) )
			{
				RequestLogger.updateSessionLog( lastToken );
			}
		}
		else if ( Preferences.getBoolean( "logGainMessages" ) )
		{
			RequestLogger.updateSessionLog( lastToken );
		}

		return shouldRefresh;
	}

	private static void processDiscard( String lastToken )
	{
		Matcher matcher = ResultProcessor.DISCARD_PATTERN.matcher( lastToken );
		if ( matcher.find() )
		{
			AdventureResult item = new AdventureResult( matcher.group( 1 ), -1, false );
			AdventureResult.addResultToList( KoLConstants.inventory, item );
			AdventureResult.addResultToList( KoLConstants.tally, item );
		}
	}

	private static boolean parseEffect( String result )
	{
		RequestLogger.updateDebugLog( "Parsing effect: " + result );

		StringTokenizer parsedEffect = new StringTokenizer( result, "()" );
		String parsedEffectName = parsedEffect.nextToken().trim();
		String parsedDuration = parsedEffect.hasMoreTokens() ? parsedEffect.nextToken() : "1";

		return ResultProcessor.processResult( new AdventureResult( parsedEffectName, StringUtilities.parseInt( parsedDuration ), true ) );
	}

	/**
	 * Utility. The method used to process a result. By default, this method will also add an adventure result to the
	 * tally directly. This is used whenever the nature of the result is already known and no additional parsing is
	 * needed.
	 *
	 * @param result Result to add to the running tally of adventure results
	 */

	public static boolean processResult( AdventureResult result )
	{
		// This should not happen, but punt if the result was null.

		if ( result == null )
		{
			return false;
		}

		RequestLogger.updateDebugLog( "Processing result: " + result );

		String resultName = result.getName();
		boolean shouldRefresh = false;

		// Process the adventure result in this section; if
		// it's a status effect, then add it to the recent
		// effect list. Otherwise, add it to the tally.

		if ( result.isStatusEffect() )
		{
			shouldRefresh |= !KoLConstants.activeEffects.contains( result );
			AdventureResult.addResultToList( KoLConstants.recentEffects, result );
		}
		else if ( resultName.equals( AdventureResult.ADV ) )
		{
			if ( result.getCount() < 0 )
			{
				TurnCounter.saveCounters();
				AdventureResult.addResultToList( KoLConstants.tally, result.getNegation() );
			}
			else if ( KoLmafia.isAdventuring() )
			{
				// Remember adventures gained while adventuring
				KoLmafia.adventureGains += result.getCount();
			}
		}
		else if ( resultName.equals( AdventureResult.CHOICE ) )
		{
			// Don't let ignored choices delay iteration
			KoLmafia.tookChoice = true;
		}
		else if ( result.isItem() )
		{
			AdventureResult.addResultToList( KoLConstants.tally, result );
		}
		else if ( resultName.equals( AdventureResult.SUBSTATS ) )
		{
			AdventureResult.addResultToList( KoLConstants.tally, result );
		}
		else if ( resultName.equals( AdventureResult.MEAT ) )
		{
			KoLAdventure location = KoLAdventure.lastVisitedLocation();

			if ( RequestLogger.getLastURLString().startsWith( "fight.php" ) && location != null && location.getAdventureId().equals( "126" ) && FightRequest.getCurrentRound() == 0 )
			{
				IslandDecorator.addNunneryMeat( result );
				return false;
			}

			AdventureResult.addResultToList( KoLConstants.tally, result );
			shouldRefresh = true;
		}

		ResultProcessor.tallyResult( result, true );

		if ( result.isItem() )
		{
			// Do special processing when you get certain items
			ResultProcessor.gainItem( result );

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

	public static boolean processMeat( int amount )
	{
		return ResultProcessor.processResult( new AdventureResult( AdventureResult.MEAT, amount ) );
	}

	public static boolean processAdventures( int amount )
	{
		if ( amount == 0 )
		{
			return false;
		}

		return ResultProcessor.processResult( new AdventureResult( AdventureResult.ADV, amount ) );
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
			KoLCharacter.setAdventuresLeft( KoLCharacter.getAdventuresLeft() + result.getCount() );
			if ( result.getCount() < 0 )
			{
				AdventureResult[] effectsArray = new AdventureResult[ KoLConstants.activeEffects.size() ];
				KoLConstants.activeEffects.toArray( effectsArray );

				for ( int i = effectsArray.length - 1; i >= 0; --i )
				{
					AdventureResult effect = effectsArray[ i ];
					if ( effect.getCount() + result.getCount() <= 0 )
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

	private static void gainItem( AdventureResult result )
	{
		// All results, whether positive or negative, are
		// handled here.
		
		switch ( result.getItemId() )
		{
		case ItemPool.LUCRE:
		case ItemPool.SAND_DOLLAR:
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
			if ( KoLAdventure.lastAdventureId() == 163 &&
				KoLConstants.inventory.contains( ItemPool.get( ItemPool.MARSHMALLOW, 1 ) ) )
			{
				ResultProcessor.processItem( ItemPool.MARSHMALLOW, -1 );
			}
			break;
		
		// Sticker weapons may have been folded from the other form
		case ItemPool.STICKER_SWORD:
			if ( KoLConstants.inventory.contains( ItemPool.get( ItemPool.STICKER_CROSSBOW, 1 ) ) )
			{
				ResultProcessor.processItem( ItemPool.STICKER_CROSSBOW, -1 );
			}
			break;
			
		case ItemPool.STICKER_CROSSBOW:
			if ( KoLConstants.inventory.contains( ItemPool.get( ItemPool.STICKER_SWORD, 1 ) ) )
			{
				ResultProcessor.processItem( ItemPool.STICKER_SWORD, -1 );
			}
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
			// If you get "I Love Me, Vol. I", you lose the items
			// you put on the shelves
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

		case ItemPool.BROKEN_DRONE:
			if ( KoLConstants.inventory.contains( ItemPool.get( ItemPool.DRONE, 1 ) ) )
			{
				ResultProcessor.processItem( ItemPool.DRONE, -1 );
			}
			break;

		case ItemPool.REPAIRED_DRONE:
			if ( KoLConstants.inventory.contains( ItemPool.get( ItemPool.BROKEN_DRONE, 1 ) ) )
			{
				ResultProcessor.processItem( ItemPool.BROKEN_DRONE, -1 );
			}
			break;

		case ItemPool.AUGMENTED_DRONE:
			if ( KoLConstants.inventory.contains( ItemPool.get( ItemPool.REPAIRED_DRONE, 1 ) ) )
			{
				ResultProcessor.processItem( ItemPool.REPAIRED_DRONE, -1 );
			}
			break;

		case ItemPool.TRAPEZOID:
			if ( KoLConstants.inventory.contains( ItemPool.get( ItemPool.POWER_SPHERE, 1 ) ) )
			{
				ResultProcessor.processItem( ItemPool.POWER_SPHERE, -1 );
			}
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
			if ( KoLConstants.inventory.contains( ItemPool.get( ItemPool.CHOMSKYS_COMICS, 1 ) ) )
			{
				ResultProcessor.processItem( ItemPool.CHOMSKYS_COMICS, -1 );
			}
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
			if ( KoLConstants.inventory.contains( ItemPool.get( ItemPool.DAMP_OLD_BOOT, 1 ) ) )
			{
				ResultProcessor.processItem( ItemPool.DAMP_OLD_BOOT, -1 );
			}
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
		RequestLogger.updateDebugLog( "Processing bulk items" );
		KoLmafia.updateDisplay( "Processing, this may take a while..." );
		for ( int i = 0; i < items.length; ++i )
		{
			AdventureResult result = (AdventureResult) items[ i ];
			
			// Skip adding to tally, since you haven't really gained these items -
			// merely moved them around.
			//AdventureResult.addResultToList( KoLConstants.tally, result );
			
			// Skip gainItem's processing, which is mostly concerned with quest items
			// that couldn't be in Hangk's anyway.
			//ResultProcessor.gainItem( result );
			
			AdventureResult.addResultToList( KoLConstants.inventory, result );
			EquipmentManager.processResult( result );
			
			// Skip conditions handling, since this can't happen during an adventure
			// request, and therefore the conditions will be rechecked.
		}
		
		// Assume that at least one item in the list required each of these updates:
		CoinmastersFrame.externalUpdate();
		ConcoctionDatabase.refreshConcoctions();
		
		KoLmafia.updateDisplay( "Processing complete." );
	}
}
