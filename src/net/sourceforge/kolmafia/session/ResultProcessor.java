/**
 * Copyright (c) 2005-2014, KoLmafia development team
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

import java.util.EnumSet;
import java.util.List;
import java.util.StringTokenizer;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.FamiliarData;
import net.sourceforge.kolmafia.KoLAdventure;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLConstants.CraftingType;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.Modifiers;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.StaticEntity;

import net.sourceforge.kolmafia.listener.NamedListenerRegistry;
import net.sourceforge.kolmafia.listener.PreferenceListenerRegistry;

import net.sourceforge.kolmafia.combat.MonsterStatusTracker;
import net.sourceforge.kolmafia.objectpool.AdventurePool;
import net.sourceforge.kolmafia.objectpool.Concoction;
import net.sourceforge.kolmafia.objectpool.ConcoctionPool;
import net.sourceforge.kolmafia.objectpool.EffectPool;
import net.sourceforge.kolmafia.objectpool.FamiliarPool;
import net.sourceforge.kolmafia.objectpool.EffectPool.Effect;
import net.sourceforge.kolmafia.objectpool.ItemPool;

import net.sourceforge.kolmafia.persistence.AdventureDatabase;
import net.sourceforge.kolmafia.persistence.ConcoctionDatabase;
import net.sourceforge.kolmafia.persistence.DebugDatabase;
import net.sourceforge.kolmafia.persistence.EquipmentDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.persistence.QuestDatabase;
import net.sourceforge.kolmafia.persistence.QuestDatabase.Quest;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.request.CreateItemRequest;
import net.sourceforge.kolmafia.request.FightRequest;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.request.HermitRequest;
import net.sourceforge.kolmafia.request.UseItemRequest;

import net.sourceforge.kolmafia.session.BugbearManager;
import net.sourceforge.kolmafia.session.HaciendaManager;
import net.sourceforge.kolmafia.session.IslandManager;

import net.sourceforge.kolmafia.swingui.CoinmastersFrame;

import net.sourceforge.kolmafia.utilities.StringUtilities;

import net.sourceforge.kolmafia.webui.BarrelDecorator;
import net.sourceforge.kolmafia.webui.CellarDecorator;

public class ResultProcessor
{
	private static final Pattern DISCARD_PATTERN = Pattern.compile( "You discard your (.*?)\\." );
	private static final Pattern INT_PATTERN = Pattern.compile( "([\\d]+)" );
	private static final Pattern TRAILING_INT_PATTERN = Pattern.compile( "(.*?)(?:\\s*\\((\\d+)\\))?" );

	private static boolean receivedClover = false;
	private static boolean receivedDisassembledClover = false;
	private static boolean autoCrafting = false;

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
		       !GenericRequest.ascending &&
		       FightRequest.getCurrentRound() == 0 &&
		       InventoryManager.cloverProtectionActive() &&
		       isCloverURL( formURLString );
	}

	public static boolean disassembledClovers( String formURLString )
	{
		return ResultProcessor.receivedDisassembledClover && InventoryManager.cloverProtectionActive() && isCloverURL( formURLString );
	}

	private static boolean isCloverURL( String formURLString )
	{
		return formURLString.startsWith( "adventure.php" ) ||
			formURLString.startsWith( "choice.php" ) ||
			formURLString.startsWith( "hermit.php" ) ||
			formURLString.startsWith( "mallstore.php" ) ||
			formURLString.startsWith( "town_fleamarket.php" ) ||
			formURLString.startsWith( "barrel.php" ) ||
			// Marmot sign can give you a clover after a fight
			formURLString.startsWith( "fight.php" ) ||
			// Using a 31337 scroll
			formURLString.contains( "whichitem=553" ) ||
			// ...without in-line loading can redirect to inventory
			( formURLString.startsWith( "inventory.php" ) &&
			  formURLString.contains( "action=message" ) );
	}

	public static Pattern ITEM_TABLE_PATTERN = Pattern.compile( "<table class=\"item\".*?rel=\"(.*?)\".*?title=\"(.*?)\".*?descitem\\(([\\d]*)\\).*?</table>" );
	public static Pattern BOLD_NAME_PATTERN = Pattern.compile( "<b>([^<]*)</b>" );

	public static String registerNewItems( boolean combatResults, String results, List<AdventureResult> data )
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
		// Pre-process all such matches and register new items.
		// Check multi-usability and plurals
		//
		// If these are not combat results, add the items to inventory.
		// (FightRequest wants to handle the items itself.)

		StringBuffer buffer = new StringBuffer();
		boolean changed = false;

		Matcher itemMatcher = ResultProcessor.ITEM_TABLE_PATTERN.matcher( results );
		while ( itemMatcher.find() )
		{
			String relString = itemMatcher.group( 1 );
			String itemName = itemMatcher.group( 2 );
			String descId = itemMatcher.group( 3 );
			Matcher boldMatcher = ResultProcessor.BOLD_NAME_PATTERN.matcher( itemMatcher.group(0) );
			String items = boldMatcher.find() ? boldMatcher.group(1) : null;

			// If we don't know this descid, it's an unknown item.
			if ( ItemDatabase.getItemIdFromDescription( descId ) == -1 )
			{
				ItemDatabase.registerItem( itemName, descId, relString, items );
			}

			// Extract item from the relstring
			AdventureResult item = ItemDatabase.itemFromRelString( relString );
			int itemId = item.getItemId();
			int count = item.getCount();

			// Check if multiusability conflicts with our expectations
			boolean multi= ItemDatabase.relStringMultiusable( relString );
			boolean ourMulti = ItemDatabase.isMultiUsable( itemId );
			if ( multi != ourMulti )
			{
				String message =
					(multi ) ?
					itemName + " is multiusable, but KoLmafia thought it was not" :
					itemName + " is not multiusable, but KoLmafia thought it was";

				RequestLogger.printLine( message );
				RequestLogger.updateSessionLog( message );
				ItemDatabase.registerMultiUsability( itemId, multi );
			}

			// If we got more than one, check if plural name
			// conflicts with our expectations
			String plural = ItemDatabase.extractItemsPlural( count, items );
			String ourPlural = plural == null ? null : ItemDatabase.getPluralName( itemId );
			if ( plural != null && !plural.equals( ourPlural ) )
			{
				String message = "Unexpected plural of '" + itemName + "' found: " + plural;
				RequestLogger.printLine( message );
				RequestLogger.updateSessionLog( message );
				ItemDatabase.registerPlural( itemId, plural );
			}

			// If these are not combat results, process the
			// acquisition and remove it from the buffer.
			if ( !combatResults )
			{
				String acquisition = count > 1 ? "You acquire" : "You acquire an item:";
				ResultProcessor.processItem( false, acquisition, item, data );
				itemMatcher.appendReplacement( buffer, "" );
				changed = true;
			}
		}

		if ( changed )
		{
			itemMatcher.appendTail( buffer );
			return buffer.toString();
		}

		return results;
	}

	public static boolean processResults( boolean combatResults, String results )
	{
		return ResultProcessor.processResults( combatResults, results, null );
	}

	public static boolean processResults( boolean combatResults, String results, List<AdventureResult> data )
	{
		ResultProcessor.receivedClover = false;
		ResultProcessor.receivedDisassembledClover = false;

		if ( data == null && RequestLogger.isDebugging() )
		{
			RequestLogger.updateDebugLog( "Processing results..." );
		}

		// If items are wrapped in a table with a "rel" string, that
		// precisely identifies what has been acquired.
		//
		// Process those first, registering new items, checking for
		// plurals and multi-usability, and moving to inventory, before
		// we strip out the HTML.

		results = ResultProcessor.registerNewItems( combatResults, results, data );

		boolean requiresRefresh = false;

		try
		{
			requiresRefresh = processNormalResults( combatResults, results, data );
		}
		finally
		{
			if ( data == null )
			{
				KoLmafia.applyEffects();
			}
		}

		return requiresRefresh;
	}

	public static boolean processNormalResults( boolean combatResults, String results, List<AdventureResult> data )
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
		     results.indexOf( "gaining weight" ) != -1 ||
		     // The following are Anapest results
		     results.indexOf( "just got heavier" ) != -1 ||
		     results.indexOf( "put on some weight" ) != -1 )
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

	private static boolean processNextResult( boolean combatResults, StringTokenizer parsedResults, List<AdventureResult> data )
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
		     lastToken.indexOf( "You have learned a skill" ) != -1 ||
		     lastToken.indexOf( "You acquire a new skill" ) != -1 )
		{
			return false;
		}

		String acquisition = lastToken.trim();

		if ( acquisition.startsWith( "You acquire an effect" ) ||
		     acquisition.startsWith( "You lose an effect" ) ||
		     acquisition.startsWith( "You lose some of an effect" ) )
		{
			return ResultProcessor.processEffect( parsedResults, acquisition, data );
		}

		if ( acquisition.startsWith( "You acquire an intrinsic" ) ||
		     acquisition.startsWith( "You lose an intrinsic" ) )
		{
			return ResultProcessor.processIntrinsic( parsedResults, acquisition, data );
		}

		if ( acquisition.startsWith( "You acquire" ) )
		{
			if ( acquisition.contains( "clan trophy" ) )
			{
				return false;
			}

			ResultProcessor.processItem( combatResults, parsedResults, acquisition, data );
			return false;
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

	private static void processItem( boolean combatResults, StringTokenizer parsedResults, String acquisition, List<AdventureResult> data )
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

	public static void processItem( boolean combatResults, String acquisition, AdventureResult result, List<AdventureResult> data )
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
	}

	private static boolean processEffect( StringTokenizer parsedResults, String acquisition, List<AdventureResult> data )
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

		// If Gar-ish is gained or loss, and autoGarish not set, benefit of Lasagna changes
		if ( effectName.equals( Effect.GARISH.effectName() ) && !Preferences.getBoolean( "autoGarish" ) )
		{
			ConcoctionDatabase.setRefreshNeeded( true );
		}

		if ( message.startsWith( "You lose" ) )
		{
			AdventureResult result = EffectPool.get( effectName );
			AdventureResult.removeResultFromList( KoLConstants.recentEffects, result );
			AdventureResult.removeResultFromList( KoLConstants.activeEffects, result );

			// If you lose Inigo's, what you can craft changes

			if ( effectName.equals( Effect.INIGO.effectName() ) )
			{
				ConcoctionDatabase.setRefreshNeeded( true );
			}

			return true;
		}

		if ( message.indexOf( "duration" ) != -1 )
		{
			Matcher m = INT_PATTERN.matcher( message );
			if ( m.find() )
			{
				int duration = StringUtilities.parseInt( m.group(1) );
				return ResultProcessor.parseEffect( effectName + " (" + duration + ")" );
			}
		}

		ResultProcessor.parseEffect( effectName );
		return false;
	}

	private static boolean processIntrinsic( StringTokenizer parsedResults, String acquisition, List<AdventureResult> data )
	{
		if ( data != null )
		{
			return false;
		}

		String effectName = parsedResults.nextToken();

		String message = acquisition + " " + effectName;
		RequestLogger.printLine( message );
		if ( Preferences.getBoolean( "logStatusEffects" ) )
		{
			RequestLogger.updateSessionLog( message );
		}

		AdventureResult result = new AdventureResult( effectName, Integer.MAX_VALUE, true );

		if ( message.startsWith( "You lose" ) )
		{
			AdventureResult.removeResultFromList( KoLConstants.activeEffects, result );
		}
		else
		{
			KoLConstants.activeEffects.add( result );
			KoLConstants.activeEffects.sort();
		}

		return true;
	}

	public static boolean processGainLoss( String lastToken, final List<AdventureResult> data )
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

			// Update Hatter deed since new hats may now be equippable
			PreferenceListenerRegistry.firePreferenceChanged( "(hats)" );

			return true;
		}

		return ResultProcessor.processStatGain( lastToken, data );
	}

	private static boolean possibleMeatDrop( int drop, int bonus )
	{
		double rate = (KoLCharacter.getMeatDropPercentAdjustment() + 100 + bonus) / 100.0;
		return Math.floor( Math.ceil( drop / rate ) * rate ) == drop;
	}

	public static boolean processMeat( String text, boolean won, boolean nunnery )
	{
		AdventureResult result = ResultProcessor.parseResult( text );
		if ( result == null )
		{
			return true;
		}

		if ( won && nunnery )
		{
			IslandManager.addNunneryMeat( result );
			return false;
		}

		if ( won && Preferences.getBoolean( "meatDropSpading" ) )
		{
			int drop = result.getCount();
			if ( !ResultProcessor.possibleMeatDrop( drop, 0 ) )
			{
				StringBuffer buf = new StringBuffer( "Alert - possible unknown meat bonus:" );
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

		return ResultProcessor.processMeat( text, result, null );
	}

	public static boolean processMeat( String lastToken, List<AdventureResult> data )
	{
		AdventureResult result = ResultProcessor.parseResult( lastToken );
		if ( result == null )
		{
			return true;
		}
		return ResultProcessor.processMeat( lastToken, result, data );
	}

	private static boolean processMeat( String lastToken, AdventureResult result, List<AdventureResult> data )
	{
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

	public static boolean processStatGain( String lastToken, List<AdventureResult> data )
	{
		if ( data != null )
		{
			return false;
		}

		AdventureResult result = ResultProcessor.parseResult( lastToken );
		if ( result == null )
		{
			return true;
		}

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
				Preferences.increment( "bankedKarma", 11 );
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
			AdventureResult retval = AdventureResult.parseResult( result );
			// If AdventureResult could not parse it, log it
			if ( retval == null )
			{
				String message = "Could not parse: " + StringUtilities.globalStringDelete( result, "&nbsp;" );
				RequestLogger.printLine( message );
				RequestLogger.updateSessionLog( message );
			}
			return retval;
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
			// Update substat delta and fullstat delta, if necessary
			int [] counts = result.getCounts();

			// Update AdventureResult.SESSION_SUBSTATS in place
			// Update AdventureResult.SESSION_FULLSTATS in place
			boolean substatChanged = false;
			boolean fullstatChanged = false;

			int count = counts[ 0 ];
			if ( count != 0 )
			{
				long stat = KoLCharacter.getTotalMuscle();
				long diff = KoLCharacter.calculateBasePoints( stat + count ) -
					    KoLCharacter.calculateBasePoints( stat );
				AdventureResult.SESSION_SUBSTATS[ 0 ] += count;
				AdventureResult.SESSION_FULLSTATS[ 0 ] += diff;
				substatChanged = true;
				fullstatChanged |= ( diff != 0 );
			}

			count = counts[ 1 ];
			if ( count != 0 )
			{
				long stat = KoLCharacter.getTotalMysticality();
				long diff = KoLCharacter.calculateBasePoints( stat + count ) -
					    KoLCharacter.calculateBasePoints( stat );
				AdventureResult.SESSION_SUBSTATS[ 1 ] += count;
				AdventureResult.SESSION_FULLSTATS[ 1 ] += diff;
				substatChanged = true;
				fullstatChanged |= ( diff != 0 );
			}

			count = counts[ 2 ];
			if ( count != 0 )
			{
				long stat = KoLCharacter.getTotalMoxie();
				long diff = KoLCharacter.calculateBasePoints( stat + count ) -
					   KoLCharacter.calculateBasePoints( stat );
				AdventureResult.SESSION_SUBSTATS[ 2 ] += count;
				AdventureResult.SESSION_FULLSTATS[ 2 ] += diff;
				substatChanged = true;
				fullstatChanged |= ( diff != 0 );
			}

			int size = KoLConstants.tally.size();
			if ( substatChanged && size > 2 )
			{
				KoLConstants.tally.fireContentsChanged( KoLConstants.tally, 2, 2 );
			}

			if ( fullstatChanged)
			{
				shouldRefresh = true;
				if ( size > 3 )
				{
					KoLConstants.tally.fireContentsChanged( KoLConstants.tally, 3, 3 );
				}
			}
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
				if ( adv >= AdventurePool.WINE_CELLAR_NORTHWEST && adv <= AdventurePool.WINE_CELLAR_SOUTHEAST )
				{
					CellarDecorator.gainItem( adv, result );
				}
			}

			if ( HermitRequest.isWorthlessItem( result.getItemId() ) )
			{
				result = HermitRequest.WORTHLESS_ITEM.getInstance( result.getCount() );
			}

			return false;
		}

		GoalManager.updateProgress( result );

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
				ConcoctionDatabase.setRefreshNeeded( result.getItemId() );
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
				ConcoctionDatabase.setRefreshNeeded( false );
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

						// If you lose Inigo's, what you can craft changes
						if ( effect.getName().equals( Effect.INIGO.effectName() ) )
						{
							ConcoctionDatabase.setRefreshNeeded( true );
						}
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
		else if ( resultName.equals( AdventureResult.FULL ) )
		{
			KoLCharacter.setFullness( KoLCharacter.getFullness() + result.getCount() );
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
		else if ( resultName.equals( AdventureResult.PVP ) )
		{
			KoLCharacter.setAttacksLeft( KoLCharacter.getAttacksLeft() + result.getCount() );
		}
	}

	private static void gainItem( boolean combatResults, AdventureResult result )
	{
		ConcoctionDatabase.setRefreshNeeded( result.getItemId() );

		// All results, whether positive or negative, are
		// handled here.

		switch ( result.getItemId() )
		{
		case ItemPool.GG_TICKET:
		case ItemPool.SNACK_VOUCHER:
		case ItemPool.LUNAR_ISOTOPE:
		case ItemPool.WORTHLESS_TRINKET:
		case ItemPool.WORTHLESS_GEWGAW:
		case ItemPool.WORTHLESS_KNICK_KNACK:
		case ItemPool.YETI_FUR:
		case ItemPool.LUCRE:
		case ItemPool.SAND_DOLLAR:
		case ItemPool.CRIMBUCK:
		case ItemPool.BONE_CHIPS:
		case ItemPool.CRIMBCO_SCRIP:
		case ItemPool.AWOL_COMMENDATION:
		case ItemPool.MR_ACCESSORY:
		case ItemPool.FAT_LOOT_TOKEN:
		case ItemPool.FUDGECULE:
		case ItemPool.FDKOL_COMMENDATION:
		case ItemPool.WARBEAR_WHOSIT:
		case ItemPool.KRUEGERAND:
		case ItemPool.SHIP_TRIP_SCRIP:
		case ItemPool.CHRONER:
		case ItemPool.TWINKLY_WAD:
			// The Traveling Trader usually wants twinkly wads
		case ItemPool.GG_TOKEN:
			// You can trade tokens for tickets
		case ItemPool.TRANSPORTER_TRANSPONDER:
			// You can go to spaaace with a transponder
			CoinmastersFrame.externalUpdate();
			break;

		case ItemPool.FAKE_HAND:
			NamedListenerRegistry.fireChange( "(fakehands)" );
			break;
		}

		// From here on out, only positive results are handled.
		if ( result.getCount() < 0 )
		{
			return;
		}

		if ( EquipmentDatabase.isHat( result ) )
		{
			PreferenceListenerRegistry.firePreferenceChanged( "(hats)" );
		}

		int itemId = result.getItemId();
		switch ( itemId )
		{
		case ItemPool.GMOB_POLLEN:
			if ( combatResults )
			{
				// Record that we beat the guy made of bees.
				Preferences.setBoolean( "guyMadeOfBeesDefeated", true );
			}
			break;

		case ItemPool.ROASTED_MARSHMALLOW:
			// Special Yuletide adventures
			if ( KoLAdventure.lastAdventureId() == AdventurePool.YULETIDE )
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

		case ItemPool.MOSQUITO_LARVA:
			QuestDatabase.setQuestProgress( Quest.LARVA, "step1" );
			break;

		case ItemPool.BITCHIN_MEATCAR:
		case ItemPool.DESERT_BUS_PASS:
		case ItemPool.PUMPKIN_CARRIAGE:
		case ItemPool.TIN_LIZZIE:
			// Desert beach unlocked
			Preferences.setInteger( "lastDesertUnlock", KoLCharacter.getAscensions() );
			break;

		case ItemPool.JUNK_JUNK:
			QuestDatabase.setQuestProgress( Quest.HIPPY, "step3" );
		case ItemPool.DINGHY_DINGY:
		case ItemPool.SKIFF:
			// Island unlocked
			Preferences.setInteger( "lastIslandUnlock", KoLCharacter.getAscensions() );
			break;

		case ItemPool.TISSUE_PAPER_IMMATERIA:
			QuestDatabase.setQuestProgress( Quest.GARBAGE, "step3" );
			break;

		case ItemPool.TIN_FOIL_IMMATERIA:
			QuestDatabase.setQuestProgress( Quest.GARBAGE, "step4" );
			break;

		case ItemPool.GAUZE_IMMATERIA:
			QuestDatabase.setQuestProgress( Quest.GARBAGE, "step5" );
			break;

		case ItemPool.PLASTIC_WRAP_IMMATERIA:
			QuestDatabase.setQuestProgress( Quest.GARBAGE, "step6" );
			break;

		case ItemPool.SOCK:
			// If you get a S.O.C.K., you lose all the Immateria
			ResultProcessor.processItem( ItemPool.TISSUE_PAPER_IMMATERIA, -1 );
			ResultProcessor.processItem( ItemPool.TIN_FOIL_IMMATERIA, -1 );
			ResultProcessor.processItem( ItemPool.GAUZE_IMMATERIA, -1 );
			ResultProcessor.processItem( ItemPool.PLASTIC_WRAP_IMMATERIA, -1 );
			QuestDatabase.setQuestProgress( Quest.GARBAGE, "step7" );
			break;

		case ItemPool.BROKEN_WINGS:
		case ItemPool.SUNKEN_EYES:
			// Make the blackbird so you don't need to have the familiar with you
			ResultProcessor.autoCreate( ItemPool.REASSEMBLED_BLACKBIRD );
			break;

		case ItemPool.BUSTED_WINGS:
		case ItemPool.BIRD_BRAIN:
			// Make the Crow so you don't need to have the familiar with you
			ResultProcessor.autoCreate( ItemPool.RECONSTITUTED_CROW );
			break;

		case ItemPool.PIRATE_FLEDGES:
			QuestDatabase.setQuestProgress( Quest.PIRATE, QuestDatabase.FINISHED );
			break;

		case ItemPool.MACGUFFIN_DIARY:
			// If you get your father's MacGuffin diary, you lose
			// your forged identification documents
			ResultProcessor.processItem( ItemPool.FORGED_ID_DOCUMENTS, -1 );
			QuestDatabase.setQuestProgress( Quest.BLACK, "step3" );
			// Automatically use the diary to open zones
			RequestThread.postRequest( UseItemRequest.getInstance( result ) );
			break;

		case ItemPool.FIRST_PIZZA:
		case ItemPool.LACROSSE_STICK:
		case ItemPool.EYE_OF_THE_STARS:
		case ItemPool.STANKARA_STONE:
		case ItemPool.MURPHYS_FLAG:
		case ItemPool.SHIELD_OF_BROOK:
			QuestDatabase.advanceQuest( Quest.SHEN );
			break;

		case ItemPool.PALINDROME_BOOK_2:
			// If you get "2 Love Me, Vol. 2", you lose
			// the items you put on the shelves
			ResultProcessor.processItem( ItemPool.PHOTOGRAPH_OF_GOD, -1 );
			ResultProcessor.processItem( ItemPool.PHOTOGRAPH_OF_RED_NUGGET, -1 );
			ResultProcessor.processItem( ItemPool.PHOTOGRAPH_OF_OSTRICH_EGG, -1 );
			ResultProcessor.processItem( ItemPool.PHOTOGRAPH_OF_DOG, -1 );
			QuestDatabase.setQuestIfBetter( Quest.PALINDOME, "step1" );
			break;

		case ItemPool.WET_STUNT_NUT_STEW:
			// If you have been asked to get the stew, you now have it.
			if ( QuestDatabase.isQuestLaterThan( Quest.PALINDOME, "step2" ) )
			{
				QuestDatabase.setQuestProgress( Quest.PALINDOME, "step4" );
			}
			break;

		case ItemPool.MEGA_GEM:
			// If you get the Mega Gem, you lose your wet stunt nut
			// stew
			ResultProcessor.processItem( ItemPool.WET_STUNT_NUT_STEW, -1 );
			QuestDatabase.setQuestIfBetter( Quest.PALINDOME, "step5" );
			break;
			
		case ItemPool.HOLY_MACGUFFIN:
			QuestDatabase.setQuestProgress( Quest.PYRAMID, QuestDatabase.FINISHED );
			break;

		case ItemPool.CONFETTI:
			// If you get the confetti, you lose the Holy MacGuffin
			if ( KoLConstants.inventory.contains( ItemPool.get( ItemPool.HOLY_MACGUFFIN, 1 ) ) )
			{
				ResultProcessor.processItem( ItemPool.HOLY_MACGUFFIN, -1 );
				QuestDatabase.setQuestProgress( Quest.PYRAMID, QuestDatabase.FINISHED );
				QuestDatabase.setQuestProgress( Quest.MANOR, QuestDatabase.FINISHED );
				QuestDatabase.setQuestProgress( Quest.WORSHIP, QuestDatabase.FINISHED );
				QuestDatabase.setQuestProgress( Quest.PALINDOME, QuestDatabase.FINISHED );
				QuestDatabase.setQuestProgress( Quest.MACGUFFIN, QuestDatabase.FINISHED );
			}
			break;

		case ItemPool.MORTAR_DISOLVING_RECIPE:
			QuestDatabase.setQuestIfBetter( Quest.MANOR, "step2" );
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
			IslandManager.startJunkyardQuest();
			break;

		case ItemPool.MOLYBDENUM_HAMMER:
		case ItemPool.MOLYBDENUM_SCREWDRIVER:
		case ItemPool.MOLYBDENUM_PLIERS:
		case ItemPool.MOLYBDENUM_WRENCH:
			// When you get a molybdenum item, tell quest handler
			IslandManager.resetGremlinTool();
			break;

		case ItemPool.SPOOKY_BICYCLE_CHAIN:
			if ( combatResults ) QuestDatabase.setQuestIfBetter( Quest.BUGBEAR, "step3" );
			break;

		case ItemPool.RONALD_SHELTER_MAP:
		case ItemPool.GRIMACE_SHELTER_MAP:
			QuestDatabase.setQuestIfBetter( Quest.GENERATOR, "step1" );
			break;

		case ItemPool.SPOOKY_LITTLE_GIRL:
			QuestDatabase.setQuestIfBetter( Quest.GENERATOR, "step2" );
			break;

		case ItemPool.EMU_UNIT:
			// If you get an E.M.U. Unit, you lose all the E.M.U. parts
			ResultProcessor.processItem( ItemPool.EMU_JOYSTICK, -1 );
			ResultProcessor.processItem( ItemPool.EMU_ROCKET, -1 );
			ResultProcessor.processItem( ItemPool.EMU_HELMET, -1 );
			ResultProcessor.processItem( ItemPool.EMU_HARNESS, -1 );
			QuestDatabase.setQuestIfBetter( Quest.GENERATOR, "step3" );
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
			QuestDatabase.setQuestIfBetter( Quest.CITADEL, "step6" );
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

		case ItemPool.SHOPPING_LIST:
			QuestDatabase.setQuestIfBetter( Quest.MEATCAR, QuestDatabase.STARTED );
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
			QuestDatabase.setQuestIfBetter( Quest.PIRATE, "step2" );
			break;
			
		case ItemPool.CARONCH_DENTURES:
			QuestDatabase.setQuestIfBetter( Quest.PIRATE, "step3" );
			break;

		case ItemPool.TEN_LEAF_CLOVER:
			ResultProcessor.receivedClover = true;
			break;

		case ItemPool.DISASSEMBLED_CLOVER:
			ResultProcessor.receivedDisassembledClover = true;
			break;

		case ItemPool.EXORCISED_SANDWICH:
			QuestDatabase.setQuestProgress( Quest.MYST, "step1" );
			break;

		case ItemPool.BAT_BANDANA:
			QuestDatabase.setQuestProgress( Quest.BAT, "step4" );
			break;

		case ItemPool.BATSKIN_BELT:
			QuestDatabase.setQuestProgress( Quest.BAT, QuestDatabase.FINISHED );
			ResultProcessor.autoCreate( ItemPool.BADASS_BELT );
			break;

		case ItemPool.KNOB_GOBLIN_CROWN:
		case ItemPool.KNOB_GOBLIN_BALLS:
		case ItemPool.KNOB_GOBLIN_CODPIECE:
			QuestDatabase.setQuestProgress( Quest.GOBLIN, QuestDatabase.FINISHED );
			break;

		case ItemPool.DODECAGRAM:
			if ( KoLConstants.inventory.contains( ItemPool.get( ItemPool.CANDLES, 1 ) ) &&
				KoLConstants.inventory.contains( ItemPool.get( ItemPool.BUTTERKNIFE, 1 ) ) )
			QuestDatabase.setQuestProgress( Quest.FRIAR, "step2" );
			break;

		case ItemPool.CANDLES:
			if ( KoLConstants.inventory.contains( ItemPool.get( ItemPool.DODECAGRAM, 1 ) ) &&
				KoLConstants.inventory.contains( ItemPool.get( ItemPool.BUTTERKNIFE, 1 ) ) )
			QuestDatabase.setQuestProgress( Quest.FRIAR, "step2" );
			break;

		case ItemPool.BUTTERKNIFE:
			if ( KoLConstants.inventory.contains( ItemPool.get( ItemPool.DODECAGRAM, 1 ) ) &&
				KoLConstants.inventory.contains( ItemPool.get( ItemPool.CANDLES, 1 ) ) )
			QuestDatabase.setQuestProgress( Quest.FRIAR, "step2" );
			break;

		case ItemPool.BONERDAGON_CHEST:
			QuestDatabase.setQuestProgress( Quest.CYRPT, "step1" );
			break;

		case ItemPool.DRAGONBONE_BELT_BUCKLE:
			QuestDatabase.setQuestProgress( Quest.CYRPT, QuestDatabase.FINISHED );
			ResultProcessor.autoCreate( ItemPool.BADASS_BELT );
			break;

		case ItemPool.GROARS_FUR:
			QuestDatabase.setQuestProgress( Quest.TRAPPER, "step5" );
			break;

		case ItemPool.MISTY_CLOAK:
		case ItemPool.MISTY_ROBE:
		case ItemPool.MISTY_CAPE:

			QuestDatabase.setQuestProgress( Quest.TOPPING, QuestDatabase.FINISHED );
			QuestDatabase.setQuestProgress( Quest.LOL, QuestDatabase.STARTED );
			return;

		case ItemPool.QUANTUM_EGG:
			ResultProcessor.autoCreate( ItemPool.ROWBOAT );
			break;

		case ItemPool.HEMP_STRING:
		case ItemPool.BONERDAGON_VERTEBRA:
			ResultProcessor.autoCreate( ItemPool.BONERDAGON_NECKLACE );
			break;

		case ItemPool.SNAKEHEAD_CHARM:
			if ( result.getCount( KoLConstants.inventory ) >= 2 &&
			     InventoryManager.getCount( ItemPool.TALISMAN ) == 0 )
			{
				ResultProcessor.autoCreate( ItemPool.TALISMAN );
			}
			break;

		case ItemPool.COPPERHEAD_CHARM:
		case ItemPool.COPPERHEAD_CHARM_RAMPANT:
			if ( InventoryManager.hasItem( ItemPool.COPPERHEAD_CHARM ) )
			{
				QuestDatabase.setQuestProgress( Quest.SHEN, QuestDatabase.FINISHED );
			}
			if ( InventoryManager.hasItem( ItemPool.COPPERHEAD_CHARM_RAMPANT ) )
			{		
				QuestDatabase.setQuestProgress( Quest.RON, QuestDatabase.FINISHED );
			}
			if ( InventoryManager.hasItem( ItemPool.COPPERHEAD_CHARM ) &&
			     InventoryManager.hasItem( ItemPool.COPPERHEAD_CHARM_RAMPANT ) )
			{
				Concoction conc = new Concoction( ItemPool.get( ItemPool.TALISMAN, 1 ),
						CraftingType.ACOMBINE,
						EnumSet.noneOf( KoLConstants.CraftingRequirements.class ),
						EnumSet.noneOf( KoLConstants.CraftingMisc.class ),
						0 );
				conc.addIngredient( ItemPool.get( ItemPool.COPPERHEAD_CHARM, 1 ) );
				conc.addIngredient( ItemPool.get( ItemPool.COPPERHEAD_CHARM_RAMPANT, 1 ) );
				ConcoctionPool.set( conc );
				if ( InventoryManager.getCount( ItemPool.TALISMAN ) == 0 )
				{
					ResultProcessor.autoCreate( ItemPool.TALISMAN );
				}
			}
			break;

		case ItemPool.EYE_OF_ED:
			QuestDatabase.setQuestProgress( Quest.MANOR, QuestDatabase.FINISHED );
			break;

		case ItemPool.MCCLUSKY_FILE_PAGE5:
			if( Preferences.getBoolean( "autoCraft" ) &&
			    InventoryManager.getCount( ItemPool.BINDER_CLIP ) == 1 )
			{
				RequestThread.postRequest( UseItemRequest.getInstance( ItemPool.BINDER_CLIP ) );
			}
			break;

		case ItemPool.MOSS_COVERED_STONE_SPHERE:
			Preferences.setInteger( "hiddenApartmentProgress", 7 );
			QuestDatabase.setQuestProgress( Quest.CURSES, QuestDatabase.FINISHED );
			if ( QuestDatabase.isQuestFinished( Quest.DOCTOR ) &&
				 QuestDatabase.isQuestFinished( Quest.BUSINESS ) &&
				 QuestDatabase.isQuestFinished( Quest.SPARE ) )
			{
				QuestDatabase.setQuestProgress( Quest.WORSHIP, "step 4" );				
			}
			break;
			
		case ItemPool.DRIPPING_STONE_SPHERE:
			Preferences.setInteger( "hiddenHospitalProgress", 7 );
			QuestDatabase.setQuestProgress( Quest.DOCTOR, QuestDatabase.FINISHED );
			if ( QuestDatabase.isQuestFinished( Quest.CURSES ) &&
				 QuestDatabase.isQuestFinished( Quest.BUSINESS ) &&
				 QuestDatabase.isQuestFinished( Quest.SPARE ) )
			{
				QuestDatabase.setQuestProgress( Quest.WORSHIP, "step 4" );				
			}
			break;
			
		case ItemPool.CRACKLING_STONE_SPHERE:
			// Lose McClusky File when you kill the Protector Spirit
			ResultProcessor.processItem( ItemPool.MCCLUSKY_FILE, -1 );
			Preferences.setInteger( "hiddenOfficeProgress", 7 );
			QuestDatabase.setQuestProgress( Quest.BUSINESS, QuestDatabase.FINISHED );
			if ( QuestDatabase.isQuestFinished( Quest.CURSES ) &&
				 QuestDatabase.isQuestFinished( Quest.DOCTOR ) &&
				 QuestDatabase.isQuestFinished( Quest.SPARE ) )
			{
				QuestDatabase.setQuestProgress( Quest.WORSHIP, "step 4" );				
			}
			break;
			
		case ItemPool.SCORCHED_STONE_SPHERE:
			Preferences.setInteger( "hiddenBowlingAlleyProgress", 7 );
			QuestDatabase.setQuestProgress( Quest.SPARE, QuestDatabase.FINISHED );
			if ( QuestDatabase.isQuestFinished( Quest.CURSES ) &&
				 QuestDatabase.isQuestFinished( Quest.BUSINESS ) &&
				 QuestDatabase.isQuestFinished( Quest.DOCTOR ) )
			{
				QuestDatabase.setQuestProgress( Quest.WORSHIP, "step 4" );				
			}
			break;
			
		case ItemPool.ANCIENT_AMULET:
			// If you get the ancient amulet, you lose the 4 stone triangles, and have definitely completed quest actions
			ResultProcessor.processItem( ItemPool.STONE_TRIANGLE, -4 );
			Preferences.setInteger( "hiddenApartmentProgress", 8 );
			Preferences.setInteger( "hiddenHospitalProgress", 8 );
			Preferences.setInteger( "hiddenOfficeProgress", 8 );
			Preferences.setInteger( "hiddenBowlingAlleyProgress", 8 );
			QuestDatabase.setQuestProgress( Quest.WORSHIP, QuestDatabase.FINISHED );
			break;
			
		case ItemPool.STAFF_OF_FATS:
			QuestDatabase.setQuestProgress( Quest.PALINDOME, QuestDatabase.FINISHED );
			break;
			
		case ItemPool.CARONCH_MAP:
			QuestDatabase.setQuestProgress( Quest.PIRATE, QuestDatabase.STARTED );
			break;
			
		case ItemPool.CARONCH_NASTY_BOOTY:
			QuestDatabase.setQuestIfBetter( Quest.PIRATE, "step1" );
			break;
			
		case ItemPool.BILLIARDS_KEY:
			QuestDatabase.setQuestProgress( Quest.SPOOKYRAVEN_NECKLACE, "step1" );
			break;

		case ItemPool.LIBRARY_KEY:
			QuestDatabase.setQuestProgress( Quest.SPOOKYRAVEN_NECKLACE, "step3" );
			break;

		case ItemPool.SPOOKYRAVEN_NECKLACE:
			QuestDatabase.setQuestProgress( Quest.SPOOKYRAVEN_NECKLACE, "step4" );
			break;

		case ItemPool.GHOST_NECKLACE:
			QuestDatabase.setQuestProgress( Quest.SPOOKYRAVEN_NECKLACE, QuestDatabase.FINISHED );
			break;

		case ItemPool.DUSTY_POPPET:
			QuestDatabase.setQuestProgress( Quest.SPOOKYRAVEN_BABIES, QuestDatabase.STARTED );
			break;

		case ItemPool.BABY_GHOSTS:
			QuestDatabase.setQuestProgress( Quest.SPOOKYRAVEN_BABIES, "step1" );
			break;

		case ItemPool.GHOST_FORMULA:
			QuestDatabase.setQuestProgress( Quest.SPOOKYRAVEN_BABIES, QuestDatabase.FINISHED );
			break;

		case ItemPool.WORSE_HOMES_GARDENS:
			QuestDatabase.setQuestProgress( Quest.HIPPY, "step1" );
			break;

		case ItemPool.STEEL_LIVER:
		case ItemPool.STEEL_STOMACH:
		case ItemPool.STEEL_SPLEEN:
			QuestDatabase.setQuestProgress( Quest.AZAZEL, QuestDatabase.FINISHED );
			break;

		case ItemPool.DAS_BOOT:
		case ItemPool.FISHY_PIPE:
		case ItemPool.FISH_MEAT_CRATE:
		case ItemPool.DAMP_WALLET:
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

		// If you acquire this item you've just completed Nemesis quest
		// Contents of Hacienda for Accordion Thief changes
		case ItemPool.BELT_BUCKLE_OF_LOPEZ:
			if ( combatResults )
			{
				HaciendaManager.questCompleted();
			}
			// fall through
		case ItemPool.INFERNAL_SEAL_CLAW:
		case ItemPool.TURTLE_POACHER_GARTER:
		case ItemPool.SPAGHETTI_BANDOLIER:
		case ItemPool.SAUCEBLOB_BELT:
		case ItemPool.NEW_WAVE_BLING:
			if ( combatResults )
			{
				Preferences.setString( "questG04Nemesis", "finished" );
			}
			break;

		case ItemPool.PIXEL_CHAIN_WHIP:
			if ( combatResults )
			{
				// If you acquire a pixel chain whip, you lose
				// the pixel whip you were wielding and wield
				// the chain whip in its place.

				AdventureResult whip = ItemPool.get( ItemPool.PIXEL_WHIP, 1 );
				EquipmentManager.transformEquipment( whip, result );
				ResultProcessor.processItem( itemId, -1 );
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
				ResultProcessor.processItem( itemId, -1 );
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

		case ItemPool.SLIME_STACK:
			if ( combatResults )
			{
				int dropped = Preferences.increment( "slimelingStacksDropped", 1 );
				if ( dropped > Preferences.getInteger( "slimelingStacksDue" ) )
				{
					// in case it's out of sync, nod and smile
					Preferences.setInteger( "slimelingStacksDue", dropped );
				}
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

		case ItemPool.DEVILISH_FOLIO:
			if ( combatResults )
			{
				Preferences.increment( "_kloopDrops", 1 );
			}
			break;

		case ItemPool.GROOSE_GREASE:
			if ( combatResults )
			{
				Preferences.increment( "_grooseDrops", 1 );
			}
			break;

		case ItemPool.GG_TOKEN:
			if ( combatResults )
			{
				Preferences.increment( "_tokenDrops", 1 );
			}
			// Fall through
		case ItemPool.GG_TICKET:
			// If this is the first token or ticket we've gotten
			// this ascension, visit the wrong side of the tracks
			// to unlock the arcade.
			if ( Preferences.getInteger( "lastArcadeAscension" ) < KoLCharacter.getAscensions() )
			{
				Preferences.setInteger( "lastArcadeAscension", KoLCharacter.getAscensions() );
				RequestThread.postRequest( new GenericRequest( "place.php?whichplace=town_wrong" ) );
			}
			break;

		case ItemPool.TRANSPORTER_TRANSPONDER:
			if ( combatResults )
			{
				Preferences.increment( "_transponderDrops", 1 );
			}
			break;

		case ItemPool.UNCONSCIOUS_COLLECTIVE_DREAM_JAR:
			if ( combatResults )
			{
				Preferences.increment( "_dreamJarDrops", 1 );
			}
			break;

		case ItemPool.HOT_ASHES:
			if ( combatResults )
			{
				Preferences.increment( "_hotAshesDrops", 1 );
			}
			break;

		case ItemPool.PSYCHOANALYTIC_JAR:
			if ( combatResults )
			{
				Preferences.increment( "_jungDrops", 1 );
				Preferences.setInteger( "jungCharge", 0 );
				KoLCharacter.findFamiliar( FamiliarPool.ANGRY_JUNG_MAN ).setCharges( 0 );
			}
			break;

		case ItemPool.LIVER_PIE:
		case ItemPool.BADASS_PIE:
		case ItemPool.FISH_PIE:
		case ItemPool.PIPING_PIE:
		case ItemPool.IGLOO_PIE:
		case ItemPool.TURNOVER:
		case ItemPool.DEAD_PIE:
		case ItemPool.THROBBING_PIE:
			if ( combatResults )
			{
				Preferences.increment( "_pieDrops", 1 );
				Preferences.setInteger( "_piePartsCount", -1 );
				Preferences.setString( "pieStuffing", "" );
			}
			break;

		case ItemPool.GOOEY_PASTE:
		case ItemPool.BEASTLY_PASTE:
		case ItemPool.OILY_PASTE:
		case ItemPool.ECTOPLASMIC:
		case ItemPool.GREASY_PASTE:
		case ItemPool.BUG_PASTE:
		case ItemPool.HIPPY_PASTE:
		case ItemPool.ORC_PASTE:
		case ItemPool.DEMONIC_PASTE:
		case ItemPool.INDESCRIBABLY_HORRIBLE_PASTE:
		case ItemPool.FISHY_PASTE:
		case ItemPool.GOBLIN_PASTE:
		case ItemPool.PIRATE_PASTE:
		case ItemPool.CHLOROPHYLL_PASTE:
		case ItemPool.STRANGE_PASTE:
		case ItemPool.MER_KIN_PASTE:
		case ItemPool.SLIMY_PASTE:
		case ItemPool.PENGUIN_PASTE:
		case ItemPool.ELEMENTAL_PASTE:
		case ItemPool.COSMIC_PASTE:
		case ItemPool.HOBO_PASTE:
		case ItemPool.CRIMBO_PASTE:
			if ( combatResults )
			{
				Preferences.increment( "_pasteDrops", 1 );
			}
			break;

		case ItemPool.BEER_LENS:
			if ( combatResults )
			{
				Preferences.increment( "_beerLensDrops", 1 );
			}
			break;

		case ItemPool.COTTON_CANDY_CONDE:
		case ItemPool.COTTON_CANDY_PINCH:
		case ItemPool.COTTON_CANDY_SMIDGEN:
		case ItemPool.COTTON_CANDY_SKOSHE:
		case ItemPool.COTTON_CANDY_PLUG:
		case ItemPool.COTTON_CANDY_PILLOW:
		case ItemPool.COTTON_CANDY_BALE:
			if ( combatResults )
			{
				Preferences.increment( "_carnieCandyDrops", 1 );
			}
			break;

		case ItemPool.LESSER_GRODULATED_VIOLET:
		case ItemPool.TIN_MAGNOLIA:
		case ItemPool.BEGPWNIA:
		case ItemPool.UPSY_DAISY:
		case ItemPool.HALF_ORCHID:
			if ( combatResults )
			{
				Preferences.increment( "_mayflowerDrops", 1 );
			}
			break;

		case ItemPool.EVILOMETER:
			Preferences.setInteger( "cyrptTotalEvilness", 200 );
			Preferences.setInteger( "cyrptAlcoveEvilness", 50 );
			Preferences.setInteger( "cyrptCrannyEvilness", 50 );
			Preferences.setInteger( "cyrptNicheEvilness", 50 );
			Preferences.setInteger( "cyrptNookEvilness", 50 );
			break;

		case ItemPool.TEACHINGS_OF_THE_FIST:
			// save which location the scroll was found in.
			String setting = AdventureDatabase.fistcoreLocationToSetting( KoLAdventure.lastAdventureId() );
			if ( setting != null )
			{
				Preferences.setBoolean( setting, true );
			}
			break;

		case ItemPool.KEYOTRON:
			BugbearManager.resetStatus();
			Preferences.setInteger( "lastKeyotronUse", KoLCharacter.getAscensions() );
			break;

		case ItemPool.JICK_JAR:
			if ( RequestLogger.getLastURLString().contains( "action=jung" ) )
			{
				Preferences.setBoolean( "_psychoJarFilled", true );
			}
		// fall through
		case ItemPool.SUSPICIOUS_JAR:
		case ItemPool.GOURD_JAR:
		case ItemPool.MYSTIC_JAR:
		case ItemPool.OLD_MAN_JAR:
		case ItemPool.ARTIST_JAR:
		case ItemPool.MEATSMITH_JAR:
			if ( RequestLogger.getLastURLString().contains( "action=jung" ) )
			{
				ResultProcessor.removeItem( ItemPool.PSYCHOANALYTIC_JAR );
			}
			break;

		case ItemPool.BRICKO_EYE:
			if ( RequestLogger.getLastURLString().startsWith( "campground.php" ) )
			{
				Preferences.increment( "_brickoEyeSummons" );
			}
			break;

		case ItemPool.DIVINE_CHAMPAGNE_POPPER:
		case ItemPool.DIVINE_CRACKER:
		case ItemPool.DIVINE_FLUTE:
			if ( RequestLogger.getLastURLString().startsWith( "campground.php" ) )
			{
				Preferences.increment( "_favorRareSummons" );
			}
			break;

		case ItemPool.YELLOW_TAFFY:
		case ItemPool.GREEN_TAFFY:
		case ItemPool.INDIGO_TAFFY:
			if ( RequestLogger.getLastURLString().startsWith( "campground.php" ) )
			{
				Preferences.increment( "_taffyRareSummons" );
			}
			break;

		case ItemPool.BOSS_HELM:
		case ItemPool.BOSS_CLOAK:
		case ItemPool.BOSS_SWORD:
		case ItemPool.BOSS_SHIELD:
		case ItemPool.BOSS_PANTS:
		case ItemPool.BOSS_GAUNTLETS:
		case ItemPool.BOSS_BOOTS:
		case ItemPool.BOSS_BELT:
			if ( combatResults )
			{
				ResultProcessor.removeItem( ItemPool.GAMEPRO_WALKTHRU );
			}
			break;

		case ItemPool.CARROT_NOSE:
			if ( combatResults )
			{
				Preferences.increment( "_carrotNoseDrops" );
			}
			break;

		case ItemPool.COSMIC_SIX_PACK:
			Preferences.setBoolean( "_cosmicSixPackConjured", true );
			break;

		case ItemPool.COBBS_KNOB_MAP:
			QuestDatabase.setQuestProgress( Quest.GOBLIN, QuestDatabase.STARTED );
			break;

		case ItemPool.MERKIN_LOCKKEY:
			String lockkeyMonster = MonsterStatusTracker.getLastMonsterName();
			Preferences.setString( "merkinLockkeyMonster", lockkeyMonster );
			if ( lockkeyMonster.equals( "mer-kin burglar" ) )
			{
				Preferences.setInteger( "choiceAdventure312", 1 );
			}
			else if ( lockkeyMonster.equals( "mer-kin raider" ) )
			{
				Preferences.setInteger( "choiceAdventure312", 2 );
			}
			else if ( lockkeyMonster.equals( "mer-kin healer" ) )
			{
				Preferences.setInteger( "choiceAdventure312", 3 );
			}
			break;

		case ItemPool.VOLCANO_MAP:
			// A counter was made in case we lost the fight against the
			// final assassin, but since this dropped we won the fight
			TurnCounter.stopCounting( "Nemesis Assassin window begin" );
			TurnCounter.stopCounting( "Nemesis Assassin window end" );
			break;

		case ItemPool.YEARBOOK_CAMERA:
		{
			String desc = DebugDatabase.rawItemDescriptionText( ItemPool.YEARBOOK_CAMERA );
			int upgrades = ItemDatabase.parseYearbookCamera( desc );
			Preferences.setInteger( "yearbookCameraAscensions", upgrades );
			break;
		}

		case ItemPool.BEER_BATTERED_ACCORDION:
		case ItemPool.BARITONE_ACCORDION:
		case ItemPool.MAMAS_SQUEEZEBOX:
		case ItemPool.GUANCERTINA:
		case ItemPool.ACCORDION_FILE:
		case ItemPool.ACCORD_ION:
		case ItemPool.BONE_BANDONEON:
		case ItemPool.PENTATONIC_ACCORDION:
		case ItemPool.NON_EUCLIDEAN_NON_ACCORDION:
		case ItemPool.ACCORDION_OF_JORDION:
		case ItemPool.AUTOCALLIOPE:
		case ItemPool.ACCORDIONOID_ROCCA:
		case ItemPool.PYGMY_CONCERTINETTE:
		case ItemPool.GHOST_ACCORDION:
		case ItemPool.PEACE_ACCORDION:
		case ItemPool.ALARM_ACCORDION:
		case ItemPool.BAL_MUSETTE_ACCORDION:
		case ItemPool.CAJUN_ACCORDION:
		case ItemPool.QUIRKY_ACCORDION:
			if ( combatResults )
			{
				StringBuilder buffer = new StringBuilder( Preferences.getString( "_stolenAccordions" ) );
				if ( buffer.length() > 0 )
				{
					buffer.append( "," );
				}
				buffer.append( itemId );
				Preferences.setString( "_stolenAccordions", buffer.toString() );
			}
			break;

		case ItemPool.DAMP_OLD_BOOT:
			Preferences.setBoolean( "dampOldBootPurchased", true );
			break;

		case ItemPool.GRIMSTONE_MASK:
			if ( combatResults )
			{
				if ( KoLCharacter.getFamiliar().equals( KoLCharacter.findFamiliar( FamiliarPool.GRIMSTONE_GOLEM ) ) )
				{
					Preferences.increment( "_grimstoneMaskDrops" );
				}
				else
				{
					Preferences.increment( "_grimstoneMaskDropsCrown" );
				}
			}
			break;

		case ItemPool.GRIM_FAIRY_TALE:
			if ( combatResults )
			{
				if ( KoLCharacter.getFamiliar().equals( KoLCharacter.findFamiliar( FamiliarPool.GRIM_BROTHER ) ) )
				{
					Preferences.increment( "_grimFairyTaleDrops" );
				}
				else
				{
					Preferences.increment( "_grimFairyTaleDropsCrown" );
				}
			}
			break;

		case ItemPool.PROFESSOR_WHAT_TSHIRT:
			if ( RequestLogger.getLastURLString().equals( "place.php?whichplace=mountains&action=mts_melvin" ) )
			{
				ResultProcessor.removeItem( ItemPool.PROFESSOR_WHAT_GARMENT );
				ResponseTextParser.learnSkill( "Torso Awaregness" );
			}
			break;

		case ItemPool.ANTICHEESE:
			if ( RequestLogger.getLastURLString().contains( "db_nukehouse ") )
			{
				Preferences.setInteger( "lastAnticheeseDay", KoLCharacter.getCurrentDays() );
			}
			break;

		case ItemPool.THINKNERD_PACKAGE:
			if ( combatResults )
			{
				Preferences.increment( "_thinknerdPackageDrops" );
			}
			break;

		case ItemPool.STEAM_FIST_1:
		case ItemPool.STEAM_FIST_2:
		case ItemPool.STEAM_FIST_3:
		case ItemPool.STEAM_TRIP_1:
		case ItemPool.STEAM_TRIP_2:
		case ItemPool.STEAM_TRIP_3:
		case ItemPool.STEAM_METEOID_1:
		case ItemPool.STEAM_METEOID_2:
		case ItemPool.STEAM_METEOID_3:
		case ItemPool.STEAM_DEMON_1:
		case ItemPool.STEAM_DEMON_2:
		case ItemPool.STEAM_DEMON_3:
		case ItemPool.STEAM_PLUMBER_1:
		case ItemPool.STEAM_PLUMBER_2:
		case ItemPool.STEAM_PLUMBER_3:
			if ( combatResults )
			{
				Preferences.increment( "_steamCardDrops" );
			}
			break;

		case ItemPool.PENCIL_THIN_MUSHROOM:
			if ( InventoryManager.getCount( ItemPool.PENCIL_THIN_MUSHROOM ) >= 9 )
			{
				QuestDatabase.setQuestProgress( Quest.JIMMY_MUSHROOM, "step1" );
			}
			break;

		case ItemPool.SAILOR_SALT:
			if ( InventoryManager.getCount( ItemPool.SAILOR_SALT ) >= 49 )
			{
				QuestDatabase.setQuestProgress( Quest.JIMMY_SALT, "step1" );
			}
			break;

		case ItemPool.TACO_DAN_RECEIPT:
			if ( InventoryManager.getCount( ItemPool.TACO_DAN_RECEIPT ) >= 9 )
			{
				QuestDatabase.setQuestProgress( Quest.TACO_DAN_AUDIT, "step1" );
			}
			break;

		case ItemPool.BROUPON:
			if ( InventoryManager.getCount( ItemPool.BROUPON ) >= 14 )
			{
				QuestDatabase.setQuestProgress( Quest.BRODEN_DEBT, "step1" );
			}
			break;

		case ItemPool.ELIZABETH_DOLLIE:
			if ( combatResults )
			{
				Preferences.setString( "nextSpookyravenElizabethRoom", "none" );
			}
			break;

		case ItemPool.STEPHEN_LAB_COAT:
			if ( combatResults )
			{
				Preferences.setString( "nextSpookyravenStephenRoom", "none" );
			}
			break;

		case ItemPool.WINE_BOMB:
			EquipmentManager.discardEquipment( ItemPool.UNSTABLE_FULMINATE );
			QuestDatabase.setQuestProgress( Quest.MANOR, "step3" );			
			break;
		}

		// Gaining items can achieve goals.
		GoalManager.updateProgress( result );
	}

	private static void autoCreate( final int itemId )
	{
		if ( ResultProcessor.autoCrafting || !Preferences.getBoolean( "autoCraft" ) )
		{
			return;
		}

		ConcoctionDatabase.refreshConcoctions( true );
		CreateItemRequest creator = CreateItemRequest.getInstance( itemId );

		// getQuantityPossible() should take meat paste or
		// Knoll Sign into account

		int possible = creator.getQuantityPossible();

		if ( possible > 0 )
		{
			// Make as many as you can
			ResultProcessor.autoCrafting = true;
			creator.setQuantityNeeded( possible );
			RequestThread.postRequest( creator );
			ResultProcessor.autoCrafting = false;
		}
	}

	private static Pattern HIPPY_PATTERN = Pattern.compile( "we donated (\\d+) meat" );
	public static boolean onlyAutosellDonationsCount = true;

	public static void handleDonations( final String urlString, final String responseText )
	{
		// Apparently, only autoselling items counts towards the trophy..
		if ( ResultProcessor.onlyAutosellDonationsCount )
		{
			return;
		}

		// ITEMS

		// Dolphin King's map:
		//
		// The treasure includes some Meat, but you give it away to
		// some moist orphans. They need it to buy dry clothes.

		if ( responseText.indexOf( "give it away to moist orphans" ) != -1 )
		{
			KoLCharacter.makeCharitableDonation( 150 );
			return;
		}

		// chest of the Bonerdagon:
		//
		// The Cola Wars Veterans Administration is really gonna
		// appreciate the huge donation you're about to make!

		if ( responseText.indexOf( "Cola Wars Veterans Administration" ) != -1 )
		{
			KoLCharacter.makeCharitableDonation( 3000 );
			return;
		}

		// ancient vinyl coin purse
		//
		// You head into town and give the Meat to a guy wearing thick
		// glasses and a tie. Maybe now he'll be able to afford eye
		// surgery and a new wardrobe.
		//
		// black pension check
		//
		// You head back to the Black Forest and give the proceeds to
		// one of the black widows. Any given widow is more or less the
		// same as any other widow, right?
		//
		// old coin purse
		//
		// You wander around town until you find somebody named
		// Charity, and give her the Meat.
		//
		// old leather wallet
		//
		// You take the Meat to a soup kitchen and hand it to the first
		// person you see. He smelled bad, so he was probably a
		// volunteer.
		//
		// orcish meat locker
		//
		// You unlock the Meat locker with your rusty metal key, and
		// then dump the contents directly into a charity box at a
		// nearby convenience store. Those kids with boneitis are sure
		// to appreciate the gesture.
		//
		// Penultimate Fantasy chest
		//
		// There some Meat in it, but you drop it off the side of the
		// airship. It'll probably land on someone needy.
		//
		// Warm Subject gift certificate
		//
		// Then you walk next door to the hat store, and you give the
		// hat store all of your Meat. We need all kinds of things in
		// this economy.

		// QUESTS

		// Spooky Forest quest:
		//
		// Thanks for the larva, Adventurer. We'll put this to good use.

		if ( responseText.indexOf( "Thanks for the larva, Adventurer" ) != -1 &&
		     responseText.indexOf( "You gain" ) == -1 )
		{
			KoLCharacter.makeCharitableDonation( 500 );
			return;
		}

		// Wizard of Ego: from the "Other Class in the Guild" -> place=ocg
		// Nemesis: from the "Same Class in the Guild" -> place=scg
		//
		// You take the Meat into town and drop it in the donation slot
		// at the orphanage. You know, the one next to the library.

		if ( responseText.indexOf( "the one next to the library" ) != -1 )
		{
			int donation =
				urlString.indexOf( "place=ocg" ) != -1 ? 500 :
				urlString.indexOf( "place=scg" ) != -1 ? 1000 :
				0;
			KoLCharacter.makeCharitableDonation( donation );
			return;
		}

		// Tr4pz0r quest:
		//
		// The furs you divide up between yourself and the Tr4pz0r, the
		// Meat you divide up between the Tr4pz0r and the needy.

		if ( responseText.indexOf( "you divide up between the Tr4pz0r and the needy" ) != -1 )
		{
			KoLCharacter.makeCharitableDonation( 5000 );
			return;
		}

		// Cap'n Caronch:
		//
		// (3000 meat with pirate fledges)

		// Post-filthworm orchard:
		//
		// Oh, hey, boss! Welcome back! Hey man, we don't want to
		// impose on your vow of poverty, so we donated 4248 meat from
		// our profits to the human fund in your honor. Thanks for
		// getting rid of those worms, man!

		Matcher matcher = ResultProcessor.HIPPY_PATTERN.matcher( responseText );
		if ( matcher.find() )
		{
			int donation = StringUtilities.parseInt( matcher.group( 1 ) );
			KoLCharacter.makeCharitableDonation( donation );
			return;
		}
	}
}
