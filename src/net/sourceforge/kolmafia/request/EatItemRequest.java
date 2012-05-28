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

package net.sourceforge.kolmafia.request;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestLogger;

import net.sourceforge.kolmafia.objectpool.ItemPool;

import net.sourceforge.kolmafia.persistence.ConcoctionDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.persistence.MonsterDatabase;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.session.InventoryManager;
import net.sourceforge.kolmafia.session.ResponseTextParser;
import net.sourceforge.kolmafia.session.ResultProcessor;
import net.sourceforge.kolmafia.session.TurnCounter;

import net.sourceforge.kolmafia.swingui.GenericFrame;

import net.sourceforge.kolmafia.utilities.InputFieldUtilities;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class EatItemRequest
	extends UseItemRequest
{
	private static final Pattern FORTUNE_PATTERN =
		Pattern.compile( "<font size=1>(Lucky numbers: (\\d+), (\\d+), (\\d+))</td>" );

	private static int ignoreMilkPrompt = 0;
	private static int askedAboutMilk = 0;
	private static int askedAboutLunch = 0;
	private static AdventureResult queuedFoodHelper = null;
	private static int queuedFoodHelperCount = 0;

	public EatItemRequest( final AdventureResult item )
	{
		super( ItemDatabase.getConsumptionType( item.getItemId() ), item );
	}

	@Override
	public int getAdventuresUsed()
	{
		if ( this.itemUsed.getItemId() == ItemPool.BLACK_PUDDING )
		{
			// Items that can redirect to a fight
			return this.itemUsed.getCount();
		}

		return 0;
	}

	public static final void ignoreMilkPrompt()
	{
		EatItemRequest.ignoreMilkPrompt = KoLCharacter.getUserId();
	}

	public static final void clearFoodHelper()
	{
		EatItemRequest.queuedFoodHelper = null;
		EatItemRequest.queuedFoodHelperCount = 0;
	}

	public static final int maximumUses( final int itemId, final String itemName, final int fullness )
	{
		int limit = KoLCharacter.getFullnessLimit();
		int fullnessLeft = limit - KoLCharacter.getFullness();
		UseItemRequest.limiter = "fullness";
		return fullnessLeft / fullness;
	}

	@Override
	public void run()
	{
		if ( this.consumptionType == KoLConstants.CONSUME_FOOD_HELPER )
		{
			int count = this.itemUsed.getCount();

			if ( !InventoryManager.retrieveItem( this.itemUsed ) )
			{
				KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "Helper not available." );
				return;
			}

			if ( this.itemUsed.equals( EatItemRequest.queuedFoodHelper ) )
			{
				EatItemRequest.queuedFoodHelperCount += count;
			}
			else
			{
				EatItemRequest.queuedFoodHelper = this.itemUsed;
				EatItemRequest.queuedFoodHelperCount = count;
			}

			KoLmafia.updateDisplay( "Helper queued for next " + count + " beverage" +
				(count == 1 ? "" : "s") + " drunk." );

			return;
		}

		if ( !ItemDatabase.meetsLevelRequirement( this.itemUsed.getName() ) )
		{
			UseItemRequest.lastUpdate = "Insufficient level to consume " + this.itemUsed;
			KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, UseItemRequest.lastUpdate );
			return;
		}

		int itemId = this.itemUsed.getItemId();
		UseItemRequest.lastUpdate = "";

		int maximumUses = UseItemRequest.maximumUses( itemId );
		if ( maximumUses < this.itemUsed.getCount() )
		{
			KoLmafia.updateDisplay( "(usable quantity of " + this.itemUsed +
				" is limited to " + maximumUses + " by " +
				UseItemRequest.limiter + ")" );
			this.itemUsed = this.itemUsed.getInstance( maximumUses );
		}

		if ( this.itemUsed.getCount() < 1 )
		{
			return;
		}

		if ( !EatItemRequest.sequentialConsume( itemId ) &&
		     !InventoryManager.retrieveItem( this.itemUsed ) )
		{
			return;
		}

		int iterations = 1;
		int origCount = this.itemUsed.getCount();

		// The miracle of "consume some" does not apply to black puddings
		if ( origCount > 1 &&
		     ( EatItemRequest.singleConsume( itemId ) ||
		       ( EatItemRequest.sequentialConsume( itemId ) && InventoryManager.getCount( itemId ) < origCount) ) )
		{
			iterations = origCount;
			this.itemUsed = this.itemUsed.getInstance( 1 );
		}

		String originalURLString = this.getURLString();

		for ( int i = 1; i <= iterations && KoLmafia.permitsContinue(); ++i )
		{
			if ( !this.allowFoodConsumption() )
			{
				return;
			}

			this.constructURLString( originalURLString );
			this.useOnce( i, iterations, "Eating" );
		}

		if ( KoLmafia.permitsContinue() )
		{
			KoLmafia.updateDisplay( "Finished eating " + origCount + " " + this.itemUsed.getName() + "." );
		}
	}

	@Override
	public void useOnce( final int currentIteration, final int totalIterations, String useTypeAsString )
	{
		UseItemRequest.lastUpdate = "";

		// Check to make sure the character has the item in their
		// inventory first - if not, report the error message and
		// return from the method.

		if ( !InventoryManager.retrieveItem( this.itemUsed ) )
		{
			UseItemRequest.lastUpdate = "Insufficient items to use.";
			return;
		}

		this.addFormField( "ajax", "1" );
		this.addFormField( "quantity", String.valueOf( this.itemUsed.getCount() ) );

		if ( EatItemRequest.queuedFoodHelper != null && EatItemRequest.queuedFoodHelperCount > 0 )
		{
			int helperItemId = EatItemRequest.queuedFoodHelper.getItemId(); 
			if ( helperItemId == ItemPool.SCRATCHS_FORK )
			{
				UseItemRequest.lastUpdate = UseItemRequest.elementalHelper( "Hotform", MonsterDatabase.HEAT, 1000 );
				if ( !UseItemRequest.lastUpdate.equals( "" ) )
				{
					KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, UseItemRequest.lastUpdate );
					EatItemRequest.queuedFoodHelper = null;
					return;
				}
			}
			this.addFormField( "utensil", String.valueOf( helperItemId ) );
			--EatItemRequest.queuedFoodHelperCount;
		}
		else
		{
			this.removeFormField( "utensil" );
		}

		super.runOneIteration( currentIteration, totalIterations, useTypeAsString );
	}

	private static final boolean singleConsume( final int itemId )
	{
		// Consume one at a time when a helper is involved.
		// Multi-consume with a helper actually DOES work, even though
		// there is no interface for doing so in game, but that's
		// probably not something that should be relied on.
		if ( EatItemRequest.queuedFoodHelper != null && EatItemRequest.queuedFoodHelperCount > 0 )
		{
			return true;
		}

		switch ( itemId )
		{
		case ItemPool.BLACK_PUDDING:
			// Eating a black pudding can lead to a combat with no
			// feedback about how many were successfully eaten
			// before the combat.
			return true;
		}
		return false;
	}

	private static final boolean sequentialConsume( final int itemId )
	{
		switch (itemId )
		{
		case ItemPool.BORIS_PIE:
		case ItemPool.JARLSBERG_PIE:
		case ItemPool.SNEAKY_PETE_PIE:
			// Allow multiple pies to be made and eaten with only one key.
			return true;
		}
		return false;
	}

	private final boolean allowFoodConsumption()
	{
		if ( !GenericFrame.instanceExists() )
		{
			return true;
		}

		String itemName = this.itemUsed.getName();
		String advGain = ItemDatabase.getAdvRangeByName( itemName );
		if ( !askAboutMilk( advGain ) )
		{
			return false;
		}

		// If we are not a Pastamancer, that's good enough. If we are,
		// make sure the player isn't going to accidentally scuttle the
		// stupid Spaghettihose trophy.
		if ( KoLCharacter.getClassType() != KoLCharacter.PASTAMANCER )
		{
			return true;
		}

		// If carboLoading is 0, it doesn't matter what you eat.
		// If it's 1, this might be normal aftercore eating.
		// If it's 10, the character will qualify for the trophy
		int carboLoading = Preferences.getInteger( "carboLoading" );
		if ( carboLoading <= 1 || carboLoading >= 10 )
		{
			return true;
		}

		// If the food is not made with noodles, no fear
		if ( ConcoctionDatabase.noodleCreation( this.itemUsed.getName() ) == null )
		{
			return true;
		}

		// Nag
		if ( !InputFieldUtilities.confirm( "Eating pasta with only " + carboLoading + " levels of Carboloading will ruin your chance to get the Spaghettihose trophy. Are you sure?" ) )
		{
			return false;
		}

		return true;
	}

	private final boolean askAboutMilk( String advGain )
	{
		// If user specifically said not to worry about milk, don't nag
		int myUserId = KoLCharacter.getUserId();
		if ( EatItemRequest.ignoreMilkPrompt == myUserId )
		{
			return true;
		}
		
		// If the item doesn't give any adventures, it won't benefit from using milk
		if ( advGain.equals( "0" ) )
		{
			return true;
		}

		// If we are not in Axecore, don't even consider Lunch
		if ( !KoLCharacter.inAxecore() )
		{
			EatItemRequest.askedAboutLunch = myUserId;
		}

		boolean skipMilkNag = ( EatItemRequest.askedAboutMilk == myUserId );
		boolean skipLunchNag = ( EatItemRequest.askedAboutLunch == myUserId );

		// If we've already asked about milk and/or lunch, don't nag
		if ( skipMilkNag && skipLunchNag )
		{
			return true;
		}

		// See if the character can cast Song of the Glorious Lunch
		UseSkillRequest lunch = UseSkillRequest.getInstance( "Song of the Glorious Lunch" );
		boolean canLunch = KoLCharacter.inAxecore() && KoLConstants.availableSkills.contains( lunch );

		// See if the character can has (or can buy) a milk of magnesium.
		boolean canMilk = InventoryManager.hasItem( ItemPool.MILK_OF_MAGNESIUM, true) || KoLCharacter.canInteract();

		// If you either can't get or don't care about both effects, don't nag
		if ( ( !canLunch || skipLunchNag ) && ( !canMilk || skipMilkNag ) )
		{
			return true;
		}

		// Calculate how much fullness we are about to add

		String name = this.itemUsed.getName();
		int fullness = ItemDatabase.getFullness( name );
		int count = this.itemUsed.getCount();
		int consumptionTurns = count * fullness - ( Preferences.getBoolean( "distentionPillActive" ) ? 1 : 0 );

		// Check for Glorious Lunch
		if ( !skipLunchNag && canLunch )
		{
			// See if already have enough of the Glorious Lunch effect
			int lunchTurns = ItemDatabase.GLORIOUS_LUNCH.getCount( KoLConstants.activeEffects );

			if ( lunchTurns < consumptionTurns )
			{
				String message = lunchTurns > 0 ?
					"Song of the Glorious Lunch will run out before you finish eating that. Are you sure?" :
					"Are you sure you want to eat without Song of the Glorious Lunch?";
				if ( !InputFieldUtilities.confirm( message ) )
				{
					return false;
				}

				EatItemRequest.askedAboutLunch = KoLCharacter.getUserId();
			}
		}

		// Check for Got Milk
		if ( !skipMilkNag && canMilk )
		{
			// See if already have enough of the Got Milk effect
			int milkyTurns = ItemDatabase.MILK.getCount( KoLConstants.activeEffects );

			if ( milkyTurns < consumptionTurns )
			{
				String message = milkyTurns > 0 ?
					"Got Milk will run out before you finish eating that. Are you sure?" :
					"Are you sure you want to eat without milk?";
				if ( !InputFieldUtilities.confirm( message ) )
				{
					return false;
				}

				EatItemRequest.askedAboutMilk = KoLCharacter.getUserId();
			}
		}

		return true;
	}

	public static final void parseConsumption( final AdventureResult item, final AdventureResult helper, final String responseText )
	{
		// Special handling for fortune cookies, since you can smash
		// them, as well as eat them
		if ( item.getItemId() == ItemPool.FORTUNE_COOKIE &&
		     responseText.indexOf( "You brutally smash the fortune cookie" ) != -1 )
		{
			ResultProcessor.processResult( item.getNegation() );
			return;
		}

		int fullness = ItemDatabase.getFullness( item.getName() );
		int count = item.getCount();

		if ( responseText.indexOf( "too full" ) != -1 )
		{
			UseItemRequest.lastUpdate = "Consumption limit reached.";
			KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, UseItemRequest.lastUpdate );

			// If we have no fullness data for this item, we can't tell what,
			// if anything, consumption did to our fullness.
			if ( fullness == 0 )
			{
				return;
			}

			int maxFullness = KoLCharacter.getFullnessLimit();
			int currentFullness = KoLCharacter.getFullness();

			// Based on what we think our current fullness is,
			// calculate how many of this item we have room for.
			int maxEat = (maxFullness - currentFullness) / fullness;

			// We know that KoL did not let us eat as many as we
			// requested, so adjust for how many we could eat.
			int couldEat = Math.max( 0, Math.min( count - 1, maxEat ) );
			if ( couldEat > 0 )
			{
				Preferences.increment( "currentFullness", couldEat * fullness );
				Preferences.decrement( "munchiesPillsUsed", couldEat );
				ResultProcessor.processResult( item.getInstance( -couldEat ) );
			}

			int estimatedFullness = maxFullness - fullness + 1;

			if ( estimatedFullness > KoLCharacter.getFullness() )
			{
				Preferences.setInteger( "currentFullness", estimatedFullness );
			}

			KoLCharacter.updateStatus();

			return;
		}

		// Check for consumption helpers, which will need to be removed
		// from inventory if they were successfully used.

		if ( helper != null )
		{
			// Check for success message, since there are multiple
			// ways these could fail:

			boolean success = true;

			switch ( helper.getItemId() )
			{
			case ItemPool.SCRATCHS_FORK:

				// "You eat the now piping-hot <food> -- it's
				// sizzlicious! The salad fork cools, and you
				// discard it."

				if ( responseText.indexOf( "The salad fork cools" ) == -1 )
				{
					success = false;
				}
				break;

			case ItemPool.FUDGE_SPORK:

				// "You eat the <food> with your fudge spork,
				// and then you eat your fudge spork. How sweet it is!"

				if ( responseText.indexOf( "you eat your fudge spork" ) == -1 )
				{
					success = false;
				}
				break;
			}

			if ( !success )
			{
				UseItemRequest.lastUpdate = "Consumption helper failed.";
				KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, UseItemRequest.lastUpdate );
				return;
			}

			// Remove the consumption helper from inventory.
			ResultProcessor.processResult( helper.getNegation() );
		}

		int consumptionType = UseItemRequest.getConsumptionType( item );

		if ( consumptionType == KoLConstants.CONSUME_FOOD_HELPER )
		{
			// Consumption helpers are removed above when you
			// successfully eat or drink.
			return;
		}

		// You feel the canticle take hold, and feel suddenly bloated
		// as the pasta expands in your belly.
		if ( KoLCharacter.getClassType() == KoLCharacter.PASTAMANCER &&
		     responseText.indexOf( "feel suddenly bloated" ) != -1 )
		{
			Preferences.setInteger( "carboLoading", 0 );
		}

		int fullnessUsed = fullness * count;

		// If we ate a distention pill, the next thing we eat should
		// detect the extra message and decrement fullness by 1.
		if ( responseText.indexOf( "feel your stomach shrink" ) != -1 )
		{
			// If we got this message, we definitely used a pill today.
			String message = "Incrementing fullness by " + ( fullnessUsed - 1 )
					+ " instead of " + fullnessUsed
					+ " because your stomach was distended.";
			fullnessUsed--;
			RequestLogger.updateSessionLog( message );
			RequestLogger.printLine( message );
			Preferences.setBoolean( "_distentionPillUsed", true );
			Preferences.setBoolean( "distentionPillActive", false );
		}

		// If we eat a non-zero fullness item and we DON'T get the
		// shrinking message, we must be out of sync with KoL. Fix
		// that.
		else if ( Preferences.getBoolean( "distentionPillActive" ) )
		{
			Preferences.setBoolean( "distentionPillActive", false );
		}

		// The food was consumed successfully
		Preferences.increment( "currentFullness", fullnessUsed );
		Preferences.decrement( "munchiesPillsUsed", count );

		ResultProcessor.processResult( item.getNegation() );
		KoLCharacter.updateStatus();

		// Re-sort consumables list if needed
		if ( Preferences.getBoolean( "sortByRoom" ) )
		{
			ConcoctionDatabase.getUsables().sort();
		}

		// Perform item-specific processing

		switch ( item.getItemId() )
		{
		case ItemPool.FORTUNE_COOKIE:
		case ItemPool.QUANTUM_TACO:

			// If it's a fortune cookie, get the fortune

			Matcher matcher = EatItemRequest.FORTUNE_PATTERN.matcher( responseText );
			while ( matcher.find() )
			{
				EatItemRequest.handleFortuneCookie( matcher );
			}

			return;

		case ItemPool.LUCIFER:

			// Jumbo Dr. Lucifer reduces your hit points to 1.
			ResultProcessor.processResult( new AdventureResult( AdventureResult.HP, 1 - KoLCharacter.getCurrentHP() ) );
			return;

		case ItemPool.BLACK_PUDDING:

			// "You screw up your courage and eat the black pudding.
			// It turns out to be the blood sausage sort of
			// pudding. You're not positive that that's a good
			// thing. Bleah."

			if ( responseText.indexOf( "blood sausage" ) != -1 )
			{
				return;
			}

			// If we are actually redirected to a fight, the item
			// is consumed elsewhere. Eating a black pudding via
			// the in-line ajax support no longer redirects to a
			// fight. Instead, the fight is forced by a script:
			//
			// <script type="text/javascript">top.mainpane.document.location = "fight.php";</script>
			//
			// If we got here, we removed it above and incremented
			// our fullness, but it wasn't actually consumed.

			ResultProcessor.processResult( item );
			Preferences.increment( "currentFullness", -3 );

			// "You don't have time to properly enjoy a black
			// pudding right now."
			if ( responseText.indexOf( "don't have time" ) != -1 )
			{
				UseItemRequest.lastUpdate = "Insufficient adventures left.";
			}

			// "You're way too beaten up to enjoy a black pudding
			// right now. Because they're tough to chew. Yeah."
			else if ( responseText.indexOf( "too beaten up" ) != -1 )
			{
				UseItemRequest.lastUpdate = "Too beaten up.";
			}

			if ( !UseItemRequest.lastUpdate.equals( "" ) )
			{
				KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, UseItemRequest.lastUpdate );
			}

			return;

		case ItemPool.STEEL_STOMACH:
			if ( responseText.indexOf( "You acquire a skill" ) != -1 )
			{
				ResponseTextParser.learnSkill( "Stomach of Steel" );
			}
			return;

		case ItemPool.EXTRA_GREASY_SLIDER:
			Preferences.setInteger( "currentSpleenUse",
				Math.max( 0, Preferences.getInteger( "currentSpleenUse" ) -
					5 * item.getCount() ) );
			KoLCharacter.updateStatus();
			return;
		}
	}

	private static final void handleFortuneCookie( final Matcher matcher )
	{
		String message = matcher.group( 1 );

		RequestLogger.updateSessionLog( message );
		RequestLogger.printLine( message );

		if ( TurnCounter.isCounting( "Fortune Cookie" ) )
		{
			for ( int i = 2; i <= 4; ++i )
			{
				int number = StringUtilities.parseInt( matcher.group( i ) );
				if ( TurnCounter.isCounting( "Fortune Cookie", number ) )
				{
					TurnCounter.stopCounting( "Fortune Cookie" );
					TurnCounter.startCounting( number, "Fortune Cookie", "fortune.gif" );
					TurnCounter.stopCounting( "Semirare window begin" );
					TurnCounter.stopCounting( "Semirare window end" );
					return;
				}
			}
		}

		int minCounter;

		// First semirare comes between 70 and 80 regardless of path

		// If we haven't played 70 turns, we definitely have not passed
		// the semirare counter yet.
		if ( KoLCharacter.getCurrentRun() < 70 )
		{
			minCounter = 70;
		}
		// If we haven't seen a semirare yet and are still within the
		// window for the first, again, expect the first one.
		else if ( KoLCharacter.getCurrentRun() < 80 &&
			  KoLCharacter.lastSemirareTurn() == 0 )
		{
			minCounter = 70;
		}
		// Otherwise, we are definitely past the first semirare,
		// whether or not we saw it. If you are not an Oxygenarian,
		// semirares come less frequently
		else if ( KoLCharacter.canEat() || KoLCharacter.canDrink() )
		{
			minCounter = 150;	// conservative, wiki claims 160 minimum
		}
		// ... than if you are on the Oxygenarian path
		else
		{
			minCounter = 100;	// conservative, wiki claims 102 minimum
		}

		minCounter -= KoLCharacter.turnsSinceLastSemirare();
		for ( int i = 2; i <= 4; ++i )
		{
			int number = StringUtilities.parseInt( matcher.group( i ) );
			int minEnd = 0;
			if ( TurnCounter.getCounters( "Semirare window begin", 0, 500 ).equals( "" ) )
			{
				// We are possibly within the window currently.
				// If the actual semirare turn has already been
				// missed, a number past the window end could
				// be valid - but it would have to be at least
				// 80 turns past the end.
				minEnd = number - 79;
			}

			if ( number < minCounter ||
			     !TurnCounter.getCounters( "Semirare window begin", number + 1, 500 ).equals( "" ) )
			{
				KoLmafia.updateDisplay( "Lucky number " + number +
							" ignored - too soon to be a semirare." );
				continue;
			}

			if ( number > 205 ||
				  !TurnCounter.getCounters( "Semirare window end", minEnd, number - 1 ).equals( "" ) )
			{	// conservative, wiki claims 200 maximum
				KoLmafia.updateDisplay( "Lucky number " + number +
							" ignored - too large to be a semirare." );
				continue;
			}

			// One fortune cookie can contain two identical numbers
			// and thereby pinpoint the semirare turn.
			if ( TurnCounter.isCounting( "Fortune Cookie", number ) )
			{
				TurnCounter.stopCounting( "Fortune Cookie" );
				TurnCounter.startCounting( number, "Fortune Cookie", "fortune.gif" );
				TurnCounter.stopCounting( "Semirare window begin" );
				TurnCounter.stopCounting( "Semirare window end" );
				return;
			}

			// Add the new lucky number
			TurnCounter.startCounting( number, "Fortune Cookie", "fortune.gif" );
		}

		TurnCounter.stopCounting( "Semirare window begin" );
		TurnCounter.stopCounting( "Semirare window end" );
	}

	public static final String lastSemirareMessage()
	{
		KoLCharacter.ensureUpdatedAscensionCounters();

		int turns = Preferences.getInteger( "semirareCounter" );
		if ( turns == 0 )
		{
			return "No semirare found yet this run.";
		}

		int current = KoLCharacter.getCurrentRun();
		String location = Preferences.getString( "semirareLocation" );
		String loc = location.equals( "" ) ? "" : ( " in " + location );
		return "Last semirare found " + ( current - turns ) + " turns ago (on turn " + turns + ")" + loc;
	}

	public static final boolean registerRequest()
	{
		AdventureResult item = UseItemRequest.lastItemUsed;
		int count = item.getCount();
		String name = item.getName();

		String useString = "eat " + count + " " + name ;

		RequestLogger.updateSessionLog();
		RequestLogger.updateSessionLog( useString );
		return true;
	}
}
