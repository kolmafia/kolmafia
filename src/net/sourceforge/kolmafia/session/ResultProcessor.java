/**
 * Copyright (c) 2005-2008, KoLmafia development team
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
import net.sourceforge.kolmafia.StaticEntity;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.swingui.CoinmastersFrame;
import net.sourceforge.kolmafia.utilities.StringUtilities;
import net.sourceforge.kolmafia.webui.IslandDecorator;

import net.sourceforge.kolmafia.request.FightRequest;
import net.sourceforge.kolmafia.request.HermitRequest;

import net.sourceforge.kolmafia.persistence.Preferences;

public class ResultProcessor
{
	private static Pattern FUMBLE_PATTERN =
		Pattern.compile( "You drop your .*? on your .*?, doing ([\\d,]+) damage" );
	private static Pattern STABBAT_PATTERN = Pattern.compile( " stabs you for ([\\d,]+) damage" );
	private static Pattern CARBS_PATTERN =
		Pattern.compile( "some of your blood, to the tune of ([\\d,]+) damage" );
	private static Pattern DISCARD_PATTERN = Pattern.compile( "You discard your (.*?)\\." );

	public static boolean processResults( String results )
	{
		return ResultProcessor.processResults( results, null );
	}

	public static boolean processResults( String results, List data )
	{
		if ( data == null )
		{
			RequestLogger.updateDebugLog( "Processing results..." );
		}

		if ( data == null && results.indexOf( "gains a pound" ) != -1 )
		{
			KoLCharacter.incrementFamilarWeight();

			RequestLogger.updateSessionLog();
			RequestLogger.updateSessionLog( "familiar " + KoLCharacter.getFamiliar() );
			RequestLogger.updateSessionLog();
		}

		String plainTextResult = KoLConstants.ANYTAG_PATTERN.matcher( results ).replaceAll( KoLConstants.LINE_BREAK );
		StringTokenizer parsedResults = new StringTokenizer( plainTextResult, KoLConstants.LINE_BREAK );

		if ( data == null )
		{
			ResultProcessor.processFumble( plainTextResult );
		}

		boolean requiresRefresh = false;

		while ( parsedResults.hasMoreTokens() )
		{
			ResultProcessor.processNextResult( parsedResults, data );
		}

		KoLmafia.applyEffects();
		return requiresRefresh;
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

		// Skip effect acquisition - it's followed by a boldface
		// which makes the parser think it's found an item.

		if ( lastToken.indexOf( "You acquire a skill" ) != -1 || lastToken.indexOf( "You gain a skill" ) != -1 )
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
		AdventureResult lastResult;
		String item = parsedResults.nextToken();

		if ( acquisition.indexOf( "an item" ) != -1 )
		{
			if ( data == null )
			{
				RequestLogger.printLine( acquisition + " " + item );
				if ( Preferences.getBoolean( "logAcquiredItems" ) )
				{
					RequestLogger.updateSessionLog( acquisition + " " + item );
				}
			}

			lastResult = ItemPool.get( item, 1 );

			if ( data == null )
			{
				processResult( lastResult );
				return;
			}

			AdventureResult.addResultToList( data, lastResult );
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
		else if ( itemName.equals( "evil golden arches" ) )
		{
			itemName = "evil golden arch";
		}

		RequestLogger.printLine( acquisition + " " + item );

		if ( Preferences.getBoolean( "logAcquiredItems" ) )
		{
			RequestLogger.updateSessionLog( acquisition + " " + item );
		}

		lastResult = ItemPool.get( itemName, StringUtilities.parseInt( countString ) );
		if ( data == null )
		{
			processResult( lastResult );
			return;
		}

		AdventureResult.addResultToList( data, lastResult );
	}

	private static boolean processEffect( StringTokenizer parsedResults, String acquisition, List data )
	{
		String effectName = parsedResults.nextToken();
		String lastToken = parsedResults.nextToken();

		RequestLogger.printLine( acquisition + " " + effectName + " " + lastToken );

		if ( Preferences.getBoolean( "logStatusEffects" ) )
		{
			RequestLogger.updateSessionLog( acquisition + " " + effectName + " " + lastToken );
		}

		if ( lastToken.indexOf( "duration" ) == -1 )
		{
			parseEffect( effectName );
			return false;
		}

		String duration = lastToken.substring( 11, lastToken.length() - 11 ).trim();
		return parseEffect( effectName + " (" + duration + ")" );
	}

	public static AdventureResult parseResult( String result )
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

		AdventureResult lastResult = ResultProcessor.parseResult( lastToken );
		if ( data == null )
		{
			processResult( lastResult );
			if ( lastResult.getName().equals( AdventureResult.SUBSTATS ) )
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

		}
		else if ( lastResult.getName().equals( AdventureResult.MEAT ) )
		{
			AdventureResult.addResultToList( data, lastResult );
			if ( Preferences.getBoolean( "logGainMessages" ) )
			{
				RequestLogger.updateSessionLog( lastToken );
			}
		}

		return false;
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

	public static boolean parseEffect( String result )
	{
		RequestLogger.updateDebugLog( "Parsing effect: " + result );

		StringTokenizer parsedEffect = new StringTokenizer( result, "()" );
		String parsedEffectName = parsedEffect.nextToken().trim();
		String parsedDuration = parsedEffect.hasMoreTokens() ? parsedEffect.nextToken() : "1";

		return ResultProcessor.processResult( new AdventureResult( parsedEffectName, StringUtilities.parseInt( parsedDuration ), true ) );
	}

	public static boolean processItem( int itemId, int count )
	{
		return ResultProcessor.processResult( ItemPool.get( itemId, count ) );
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
		// This should not happen, but check just in case and
		// return if the result was null.

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
				StaticEntity.saveCounters();
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
			if ( location != null && location.getAdventureId().equals( "126" ) && FightRequest.getCurrentRound() == 0 )
			{
				IslandDecorator.addNunneryMeat( result );
				return false;
			}

			AdventureResult.addResultToList( KoLConstants.tally, result );
		}

		KoLCharacter.processResult( result );

		if ( result.isItem() )
		{
			// Do special processing when you get certain items
			ResultProcessor.gainItem( result );
		}

		shouldRefresh |= result.getName().equals( AdventureResult.MEAT );

		// Process the adventure result through the conditions
		// list, removing it if the condition is satisfied.

		if ( result.isItem() && HermitRequest.isWorthlessItem( result.getItemId() ) )
		{
			result = HermitRequest.WORTHLESS_ITEM.getInstance( result.getCount() );
		}

		// Now, if it's an actual stat gain, be sure to update the
		// list to reflect the current value of stats so far.

		if ( resultName.equals( AdventureResult.SUBSTATS ) && KoLConstants.tally.size() >= 3 )
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
			ResultProcessor.processCondition( result, resultName, conditionIndex );
		}

		return shouldRefresh;
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

	public static void gainItem( AdventureResult result )
	{
		switch ( result.getItemId() )
		{
		case KoLmafia.LUCRE:
			CoinmastersFrame.externalUpdate();
			break;

		case KoLmafia.SOCK:
			// If you get a S.O.C.K., you lose all the Immateria
			if ( result.getCount() == 1 )
			{
				for ( int i = 0; i < KoLAdventure.IMMATERIA.length; ++i )
				{
					processResult( KoLAdventure.IMMATERIA[ i ] );
				}
			}
			break;

		case KoLmafia.LASAGNA:
		case KoLmafia.MARGARITA:
		case KoLmafia.AIR_FRESHENER:
			// When you get a steel item, you lose Azazel's items
			if ( result.getCount() == 1 )
			{
				for ( int i = 0; i < KoLAdventure.AZAZEL.length; ++i )
				{
					processResult( KoLAdventure.AZAZEL[ i ] );
				}
			}
			break;

		case KoLmafia.MAGNET:
			// When you get the molybdenum magnet, tell quest handler
			if ( result.getCount() == 1 )
			{
				IslandDecorator.startJunkyardQuest();
			}
			break;

		case KoLmafia.HAMMER:
		case KoLmafia.SCREWDRIVER:
		case KoLmafia.PLIERS:
		case KoLmafia.WRENCH:
			// When you get a molybdenum item, tell quest handler
			if ( result.getCount() == 1 )
			{
				IslandDecorator.resetGremlinTool();
			}
			break;

		case KoLmafia.BROKEN_DRONE:
			if ( result.getCount() == 1 && KoLConstants.inventory.contains( KoLAdventure.DRONE ) )
			{
				processResult( KoLAdventure.DRONE );
			}
			break;

		case KoLmafia.REPAIRED_DRONE:
			if ( result.getCount() == 1 && KoLConstants.inventory.contains( KoLAdventure.BROKEN_DRONE ) )
			{
				processResult( KoLAdventure.BROKEN_DRONE );
			}
			break;

		case KoLmafia.AUGMENTED_DRONE:
			if ( result.getCount() == 1 && KoLConstants.inventory.contains( KoLAdventure.REPAIRED_DRONE ) )
			{
				processResult( KoLAdventure.REPAIRED_DRONE );
			}
			break;

		case KoLmafia.TRAPEZOID:
			if ( result.getCount() == 1 && KoLConstants.inventory.contains( KoLAdventure.POWER_SPHERE ) )
			{
				processResult( KoLAdventure.POWER_SPHERE );
			}
			break;

		 // These update the session results for the item swapping in
		 // the Gnome's Going Postal quest.

		case KoLmafia.REALLY_BIG_TINY_HOUSE:
			if ( result.getCount() == 1 )
			{
				processResult( KoLAdventure.RED_PAPER_CLIP );
			}
			break;
		case KoLmafia.NONESSENTIAL_AMULET:
			if ( result.getCount() == 1 )
			{
				processResult( KoLAdventure.REALLY_BIG_TINY_HOUSE );
			}
			break;

		case KoLmafia.WHITE_WINE_VINAIGRETTE:
			if ( result.getCount() == 1 )
			{
				processResult( KoLAdventure.NONESSENTIAL_AMULET );
			}
			break;
		case KoLmafia.CURIOUSLY_SHINY_AX:
			if ( result.getCount() == 1 )
			{
				processResult( KoLAdventure.WHITE_WINE_VINAIGRETTE );
			}
			break;
		case KoLmafia.CUP_OF_STRONG_TEA:
			if ( result.getCount() == 1 )
			{
				processResult( KoLAdventure.CURIOUSLY_SHINY_AX );
			}
			break;
		case KoLmafia.MARINATED_STAKES:
			if ( result.getCount() == 1 )
			{
				processResult( KoLAdventure.CUP_OF_STRONG_TEA );
			}
			break;
		case KoLmafia.KNOB_BUTTER:
			if ( result.getCount() == 1 )
			{
				processResult( KoLAdventure.MARINATED_STAKES );
			}
			break;
		case KoLmafia.VIAL_OF_ECTOPLASM:
			if ( result.getCount() == 1 )
			{
				processResult( KoLAdventure.KNOB_BUTTER );
			}
			break;
		case KoLmafia.BOOCK_OF_MAGIKS:
			if ( result.getCount() == 1 )
			{
				processResult( KoLAdventure.VIAL_OF_ECTOPLASM );
			}
			break;
		case KoLmafia.EZ_PLAY_HARMONICA_BOOK:
			if ( result.getCount() == 1 )
			{
				processResult( KoLAdventure.BOOCK_OF_MAGIKS );
			}
			break;
		case KoLmafia.FINGERLESS_HOBO_GLOVES:
			if ( result.getCount() == 1 )
			{
				processResult( KoLAdventure.EZ_PLAY_HARMONICA_BOOK );
			}
			break;
		case KoLmafia.CHOMSKYS_COMICS:
			if ( result.getCount() == 1 )
			{
				processResult( KoLAdventure.FINGERLESS_HOBO_GLOVES );
			}
			break;

		case KoLmafia.GNOME_DEMODULIZER:
			if ( result.getCount() == 1 && KoLConstants.inventory.contains( KoLAdventure.CHOMSKYS_COMICS ) )
			{
				processResult( KoLAdventure.CHOMSKYS_COMICS );
			}
			break;
		}
	}
}
