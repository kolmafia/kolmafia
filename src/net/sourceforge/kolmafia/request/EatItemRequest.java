/**
 * Copyright (c) 2005-2015, KoLmafia development team
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

import java.util.Calendar;
import java.util.TimeZone;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLConstants.CraftingType;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.RequestThread;

import net.sourceforge.kolmafia.objectpool.EffectPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.objectpool.SkillPool;

import net.sourceforge.kolmafia.persistence.ConcoctionDatabase;
import net.sourceforge.kolmafia.persistence.ConsumablesDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.persistence.MonsterDatabase.Element;

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
	private static final Pattern INSUFFICIENT_QUANTITY_PATTERN =
		Pattern.compile( "You only have (\\d+) of those, not (\\d+)" );
	private static final Pattern MAYONEX_PATTERN =
		Pattern.compile( "Force of Mayo Be With You</b><br>\\(duration: (\\d+) Adventure" );

	private static int ignoreMilkPrompt = 0;
	private static int askedAboutMilk = 0;
	private static int askedAboutLunch = 0;
	private static int askedAboutGarish = 0;
	private static int askedAboutMayodiol = 0;
	private static AdventureResult queuedFoodHelper = null;
	private static int queuedFoodHelperCount = 0;
	public static int foodConsumed = 0;

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

	public static final AdventureResult currentFoodHelper()
	{
		return ( EatItemRequest.queuedFoodHelper != null && EatItemRequest.queuedFoodHelperCount > 0 ) ?
			EatItemRequest.queuedFoodHelper.getInstance( EatItemRequest.queuedFoodHelperCount ) :
			null;
	}

	public static final int maximumUses( final int itemId, final String itemName, final int fullness )
	{
		if ( KoLCharacter.isJarlsberg() && ConcoctionDatabase.getMixingMethod( itemId ) != CraftingType.JARLS )
		{
			UseItemRequest.limiter = "its non-Jarlsbergian nature";
			return 0;
		}

		if ( KoLCharacter.inZombiecore() && !itemName.equals( "steel lasagna" ) &&
		     ( ConsumablesDatabase.getNotes( itemName ) == null || !ConsumablesDatabase.getNotes( itemName ).startsWith( "Zombie Slayer" ) ) )
		{
			UseItemRequest.limiter = "it not being a brain";
			return 0;
		}

		switch ( itemId )
		{
		case ItemPool.SPAGHETTI_BREAKFAST:
			// This is your breakfast, you need to eat it first thing
			return ( KoLCharacter.getFullness() == 0 && KoLCharacter.getFullnessLimit() > 0 &&
			         !Preferences.getBoolean( "_spaghettiBreakfastEaten" ) ) ? 1 : 0;
		}

		int limit = KoLCharacter.getFullnessLimit();
		int fullnessLeft = limit - KoLCharacter.getFullness();
		UseItemRequest.limiter = "fullness";
		return fullnessLeft / fullness;
	}

	@Override
	public void run()
	{
		if ( GenericRequest.abortIfInFightOrChoice() )
		{
			return;
		}

		if ( this.consumptionType == KoLConstants.CONSUME_FOOD_HELPER )
		{
			int count = this.itemUsed.getCount();

			if ( !InventoryManager.retrieveItem( this.itemUsed ) )
			{
				KoLmafia.updateDisplay( MafiaState.ERROR, "Helper not available." );
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

			KoLmafia.updateDisplay( this.itemUsed.getName() + " queued for next " + count + " food" +
				(count == 1 ? "" : "s") + " eaten." );

			return;
		}

		if ( !ConsumablesDatabase.meetsLevelRequirement( this.itemUsed.getName() ) )
		{
			UseItemRequest.lastUpdate = "Insufficient level to consume " + this.itemUsed;
			KoLmafia.updateDisplay( MafiaState.ERROR, UseItemRequest.lastUpdate );
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
			EatItemRequest.foodConsumed = i - 1;
			if ( !this.allowFoodConsumption() )
			{
				KoLmafia.updateDisplay( MafiaState.ERROR, "Aborted eating " + this.itemUsed.getCount() + " " + this.itemUsed.getName() + "." );
				return;
			}

			this.constructURLString( originalURLString );
			this.useOnce( i, iterations, "Eating" );
		}

		if ( KoLmafia.permitsContinue() )
		{
			EatItemRequest.foodConsumed = origCount;
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

		if ( this.consumptionType == KoLConstants.CONSUME_MULTIPLE && this.itemUsed.getCount() > 1 )
		{
			this.addFormField( "action", "useitem" );
		}

		this.addFormField( "ajax", "1" );
		this.addFormField( "quantity", String.valueOf( this.itemUsed.getCount() ) );

		if ( EatItemRequest.queuedFoodHelper != null && EatItemRequest.queuedFoodHelperCount > 0 )
		{
			int helperItemId = EatItemRequest.queuedFoodHelper.getItemId();
			switch ( helperItemId )
			{
			case ItemPool.SCRATCHS_FORK:
				// Check it can be safely used
				UseItemRequest.lastUpdate = UseItemRequest.elementalHelper( "Hotform", Element.HOT, 1000 );
				if ( !UseItemRequest.lastUpdate.equals( "" ) )
				{
					KoLmafia.updateDisplay( MafiaState.ERROR, UseItemRequest.lastUpdate );
					EatItemRequest.queuedFoodHelper = null;
					return;
				}
				// deliberate fallthrough
			case ItemPool.FUDGE_SPORK:
				// Items submitted with utensil
				this.addFormField( "utensil", String.valueOf( helperItemId ) );
				break;
			default:
				// Autoused helpers are ignored
				this.removeFormField( "utensil" );
			}
			EatItemRequest.queuedFoodHelperCount -= 1;
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

		if ( !askAboutGarish( itemName ) )
		{
			return false;
		}

		if ( !askAboutMayodiol( this.itemUsed.getItemId() ) )
		{
			return false;
		}

		if ( !EatItemRequest.askAboutMilk( itemName, this.itemUsed.getCount() ) )
		{
			return false;
		}

		if ( !UseItemRequest.askAboutPvP( itemName ) )
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
		if ( ConcoctionDatabase.noodleCreation( this.itemUsed.getItemId() ) == null )
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

	public static boolean askAboutMilk( final String name, final int count )
	{
		// If user specifically said not to worry about milk, don't nag
		int myUserId = KoLCharacter.getUserId();
		if ( EatItemRequest.ignoreMilkPrompt == myUserId )
		{
			return true;
		}

		// If the item doesn't give any adventures, it won't benefit from using milk
		String note = ConsumablesDatabase.getNotes( name );
		String advGain = ConsumablesDatabase.getAdvRangeByName( name );
		if ( advGain.equals( "0" ) )
		{
			if ( note == null || ( note != null && !note.contains( "Unspaded" ) ) )
			{
				return true;
			}
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

		// See if the character has (or can buy) a milk of magnesium.
		boolean canMilk = InventoryManager.itemAvailable( ItemPool.MILK_OF_MAGNESIUM );

		// If you either can't get or don't care about both effects, don't nag
		if ( ( !canLunch || skipLunchNag ) && ( !canMilk || skipMilkNag ) )
		{
			return true;
		}

		// Calculate how much fullness we are about to add

		int fullness = ConsumablesDatabase.getFullness( name );
		int consumptionTurns = count * fullness;

		// Check for Glorious Lunch
		if ( !skipLunchNag && canLunch )
		{
			// See if already have enough of the Glorious Lunch effect
			int lunchTurns = ConsumablesDatabase.GLORIOUS_LUNCH.getCount( KoLConstants.activeEffects );

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
			int milkyTurns = ConsumablesDatabase.MILK.getCount( KoLConstants.activeEffects );

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

	private static final boolean askAboutGarish( String itemName )
	{
		// Only affects lasagna
		if ( !ConsumablesDatabase.isLasagna( ItemDatabase.getItemId( itemName ) ) )
		{
			return true;
		}
		
		// If you've got Garish, or it's Monday, no need to ask
		Calendar date = Calendar.getInstance( TimeZone.getTimeZone( "GMT-0700" ) );
		if( KoLConstants.activeEffects.contains( EffectPool.get( EffectPool.GARISH ) ) || date.get( Calendar.DAY_OF_WEEK ) == Calendar.MONDAY )
		{
			return true;
		}
		
		// If we've already asked about Garish, don't nag
		if ( EatItemRequest.askedAboutGarish == KoLCharacter.getUserId() )
		{
			return true;
		}

		// If we don't have skill, all summons are used, and we can't get one, no need to ask
		if ( ( !KoLCharacter.hasSkill( SkillPool.CLIP_ART ) || UseSkillRequest.getUnmodifiedInstance( SkillPool.CLIP_ART ).getMaximumCast() == 0 )
			&& !InventoryManager.itemAvailable( ItemPool.FIELD_GAR_POTION ) )
		{
			return true;
		}

		// If autoGarish is true, get Gar-ish
		if ( Preferences.getBoolean( "autoGarish" ) )
		{
			RequestThread.postRequest( UseItemRequest.getInstance( ItemPool.FIELD_GAR_POTION ) );
			if ( !KoLConstants.activeEffects.contains( EffectPool.get( EffectPool.GARISH ) ) )
			{
				KoLmafia.updateDisplay( MafiaState.ERROR, "Failed to use Potion of the Field Gar." );
				return false;
			}
			else
			{
				return true;
			}
		}

		if ( !InputFieldUtilities.confirm( "Are you sure you want to eat Lasagna without Potion of the Field Gar ?" ) )
		{
			return false;
		}

		EatItemRequest.askedAboutGarish = KoLCharacter.getUserId();

		return true;
	}

	private static final boolean askAboutMayodiol( final int id )
	{
		// If we've already asked about Mayodiol, don't nag
		if ( EatItemRequest.askedAboutMayodiol == KoLCharacter.getUserId() )
		{
			return true;
		}

		// If we're not at drunk limit, it's ok
		if ( KoLCharacter.getInebrietyLimit() != KoLCharacter.getInebriety() )
		{
			return true;
		}
		
		// If it's Mayo, warning is only needed if Mayodiol
		if ( ConcoctionDatabase.isMayo( id ) )
		{
			if ( id == ItemPool.MAYODIOL )
			{
				if ( !InputFieldUtilities.confirm( "Putting this in your mouth will cause you to overdrink with the next food, are you sure ?" ) )
				{
					return false;
				}
			}
			else
			{
				return true;
			}
		}
		else
		{
			// It's food
			// If we will use MayoMinder to automatically use Mayodiol, warn
			if ( Preferences.getString( "mayoInMouth" ).equals( "" ) &&	Preferences.getString( "mayoMinderSetting" ).equals( "Mayodiol" ) &&
				InventoryManager.hasItem( ItemPool.MAYODIOL ) )
			{
				if ( !InputFieldUtilities.confirm( "Eating this will cause you to overdrink due to Mayodiol in inventory with Mayo Minder&trade; set to use it, are you sure ?" ) )
				{
					return false;
				}
			}
			// If we already have Mayodiol in our mouth, warn
			else if ( Preferences.getString( "mayoInMouth" ).equals( "Mayodiol" ) )
			{
				if ( !InputFieldUtilities.confirm( "Eating this will cause you to overdrink due to Mayodiol in your mouth, are you sure ?" ) )
				{
					return false;
				}
			}
			// Otherwise no warning needed
			else
			{
				return true;
			}
		}

		EatItemRequest.askedAboutMayodiol = KoLCharacter.getUserId();

		return true;
	}

	public static final void parseConsumption( final AdventureResult item, final AdventureResult helper, final String responseText )
	{
		// Special handling for fortune cookies, since you can smash
		// them, as well as eat them
		if ( item.getItemId() == ItemPool.FORTUNE_COOKIE &&
		     responseText.contains( "You brutally smash the fortune cookie" ) )
		{
			ResultProcessor.processResult( item.getNegation() );
			return;
		}

		if ( item.getItemId() == ItemPool.CARTON_OF_SNAKE_MILK &&
		     responseText.contains( "cream cheese" ) )
		{
			ResultProcessor.processResult( item.getNegation() );
			return;
		}

		// You're really, really hungry, but that isn't what you're hungry for.
		if ( KoLCharacter.inZombiecore() && responseText.contains( "that isn't what you're hungry for" ) )
		{
			UseItemRequest.lastUpdate = "You can only eat tasty, tasty brains.";
			KoLmafia.updateDisplay( MafiaState.ERROR, UseItemRequest.lastUpdate );
			return;
		}

		// Breakfast has to be the first thing you eat in a day. That's what breakfast means.
		if ( responseText.contains( "That's what breakfast means" ) )
		{
			UseItemRequest.lastUpdate = "A spaghetti breakfast must be your the first food of the day.";
			KoLmafia.updateDisplay( MafiaState.ERROR, UseItemRequest.lastUpdate );
			return;
		}

		// Food is restricted by Standard.
		if ( responseText.contains( "That item is too old to be used on this path" ) )
		{
			UseItemRequest.lastUpdate = item.getName() + " is too old to be used on this path.";
			KoLmafia.updateDisplay( MafiaState.ERROR, UseItemRequest.lastUpdate );
			return;
		}

		// You only have 1 of those, not 2
		Matcher quantityMatcher = EatItemRequest.INSUFFICIENT_QUANTITY_PATTERN.matcher( responseText );
		if ( quantityMatcher.find() )
		{
			int count = StringUtilities.parseInt( quantityMatcher.group( 1 ) );
			int requested = StringUtilities.parseInt( quantityMatcher.group( 2 ) );
			UseItemRequest.lastUpdate = "You only have " + count + " of those, not " + requested;
			KoLmafia.updateDisplay( MafiaState.ERROR, UseItemRequest.lastUpdate );
			return;
		}

		boolean shouldUpdateFullness = !responseText.contains( " Fullness" );
		if ( responseText.contains( "too full" ) )
		{
			int fullness = ConsumablesDatabase.getFullness( item.getName() );
			int count = item.getCount();

			UseItemRequest.lastUpdate = "Consumption limit reached.";
			KoLmafia.updateDisplay( MafiaState.ERROR, UseItemRequest.lastUpdate );

			// If we have no fullness data for this item, we can't tell what,
			// if anything, consumption did to our fullness.
			if ( fullness == 0 )
			{
				return;
			}

			int maxFullness = KoLCharacter.getFullnessLimit();

			// Based on what we think our current fullness is,
			// calculate how many of this item we have room for.
			int maxEat = ( maxFullness - KoLCharacter.getFullness()) / fullness;

			// We know that KoL did not let us eat as many as we
			// requested, so adjust for how many we could eat.
			int couldEat = Math.max( 0, Math.min( count - 1, maxEat ) );
			if ( couldEat > 0 )
			{
				if ( shouldUpdateFullness )
				{
					KoLCharacter.setFullness( KoLCharacter.getFullness() + couldEat * fullness );
				}
				Preferences.decrement( "munchiesPillsUsed", couldEat );
				ResultProcessor.processResult( item.getInstance( -couldEat ) );
			}

			int estimatedFullness = maxFullness - fullness + 1;

			if ( estimatedFullness > KoLCharacter.getFullness() )
			{
				KoLCharacter.setFullness( estimatedFullness );
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

				if ( !responseText.contains( "The salad fork cools" ) )
				{
					success = false;
				}
				break;

			case ItemPool.FUDGE_SPORK:

				// "You eat the <food> with your fudge spork,
				// and then you eat your fudge spork. How sweet it is!"

				if ( !responseText.contains( "you eat your fudge spork" ) )
				{
					success = false;
				}
				break;
			}

			if ( !success )
			{
				UseItemRequest.lastUpdate = "Consumption helper failed.";
				KoLmafia.updateDisplay( MafiaState.ERROR, UseItemRequest.lastUpdate );
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

		// The food was consumed successfully
		EatItemRequest.handleFoodHelper( item.getName(), item.getCount(), responseText );

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

			if ( responseText.contains( "blood sausage" ) )
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
			if ( shouldUpdateFullness )
			{
				KoLCharacter.setFullness( KoLCharacter.getFullness() -3 );
			}

			// "You don't have time to properly enjoy a black
			// pudding right now."
			if ( responseText.contains( "don't have time" ) )
			{
				UseItemRequest.lastUpdate = "Insufficient adventures left.";
			}

			// "You're way too beaten up to enjoy a black pudding
			// right now. Because they're tough to chew. Yeah."
			else if ( responseText.contains( "too beaten up" ) )
			{
				UseItemRequest.lastUpdate = "Too beaten up.";
			}

			if ( !UseItemRequest.lastUpdate.equals( "" ) )
			{
				KoLmafia.updateDisplay( MafiaState.ERROR, UseItemRequest.lastUpdate );
			}

			return;

		case ItemPool.STEEL_STOMACH:
			if ( responseText.contains( "You acquire a skill" ) )
			{
				ResponseTextParser.learnSkill( "Stomach of Steel" );
			}
			return;

		case ItemPool.EXTRA_GREASY_SLIDER:
			KoLCharacter.setSpleenUse( KoLCharacter.getSpleenUse() - 5 * item.getCount() );
			KoLCharacter.updateStatus();
			return;

		case ItemPool.SPAGHETTI_BREAKFAST:
			Preferences.setBoolean( "_spaghettiBreakfastEaten", true );
			return;

		case ItemPool.SMORE:
			Preferences.increment( "smoresEaten", 1 );
			ConsumablesDatabase.setSmoresData();
			ConsumablesDatabase.calculateAdventureRanges();
			return;

		}
	}

	public static final void handleFoodHelper( final String itemName, final int count, final String responseText )
	{
		// You chase it with that salt you made in the chemistry
		// lab. Man. Teenagers will eat anything.
		if ( responseText.contains( "You chase it with that salt you made" ) )
		{
			int itemsUsed = Math.min( Math.min( count, InventoryManager.getCount( ItemPool.GRAINS_OF_SALT ) ),
				3 - Preferences.getInteger( "_saltGrainsConsumed" ) );
			RequestLogger.printLine( "You ate " + itemsUsed + " grains of salt with your food" );
			ResultProcessor.processItem( ItemPool.GRAINS_OF_SALT, -itemsUsed );
			Preferences.increment( "_saltGrainsConsumed", itemsUsed );
		}

		// You dip the spaghetti breakfast in swamp honey before you
		// eat it. Mmmmm!
		if ( responseText.contains( "in swamp honey before you eat it." ) )
		{
			int itemsUsed = Math.min( count, InventoryManager.getCount( ItemPool.JAR_OF_SWAMP_HONEY ) );
			if ( itemsUsed > 1 )
			{
				RequestLogger.printLine( "You ate " + itemsUsed + " jars of swamp honey with your food" );
			}
			else
			{
				RequestLogger.printLine( "You ate a jar of swamp honey with your food" );
			}
			ResultProcessor.processItem( ItemPool.JAR_OF_SWAMP_HONEY, -itemsUsed );
		}

		// You give the barrel cracker a nice dry rubbing before going to work on it...</
		if ( responseText.contains( "a nice dry rubbing before going to work on it" ) )
		{
			int itemsUsed = Math.min( count, InventoryManager.getCount( ItemPool.DRY_RUB ) );
			if ( itemsUsed > 1 )
			{
				RequestLogger.printLine( "You ate " + itemsUsed + " shakers of dry rub with your food" );
			}
			else
			{
				RequestLogger.printLine( "You ate a shaker of dry rub with your food" );
			}
			ResultProcessor.processItem( ItemPool.DRY_RUB, -itemsUsed );
		}

		// You feel the canticle take hold, and feel suddenly bloated
		// as the pasta expands in your belly.
		if ( KoLCharacter.getClassType() == KoLCharacter.PASTAMANCER &&
		     responseText.indexOf( "feel suddenly bloated" ) != -1 )
		{
			Preferences.setInteger( "carboLoading", 0 );
		}

		// If you have Mayo Minder running, you don't need to use the mayo helpers, but they are still used up
		// Your Mayo Minder™ beeps, reminding you to squirt some mayonnaise in your mouth before eating.
		// You feel the Mayonex gurgling in your stomach.
		// The Mayodiol kicks in and converts some of what you just ate into pure ethanol.
		// The Mayostat kicks in and you belch up a mayonnaise-coated bolus of food.
		// The Mayozapine kicks in and makes the food extra-delicious!
		// The Mayoflex kicks in and makes the food more nutritious.
		if ( responseText.contains( "reminding you to squirt some mayonnaise" ) )
		{
			if ( responseText.contains( "feel the Mayonex gurgling" ) )
			{
				int itemsUsed = Math.min( count, InventoryManager.getCount( ItemPool.MAYONEX ) );
				RequestLogger.printLine( "Mayo Minder&trade; reminded you to use Mayonex (" + itemsUsed + " times)" );
				ResultProcessor.processItem( ItemPool.MAYONEX, -itemsUsed );
			}
			else if ( responseText.contains( "Mayodiol kicks in" ) )
			{
				int itemsUsed = Math.min( count, InventoryManager.getCount( ItemPool.MAYODIOL ) );
				RequestLogger.printLine( "Mayo Minder&trade; reminded you to use Mayodiol (" + itemsUsed + " times)" );
				ResultProcessor.processItem( ItemPool.MAYODIOL, -itemsUsed );
			}
			else if ( responseText.contains( "Mayostat kicks in" ) )
			{
				int itemsUsed = Math.min( count, InventoryManager.getCount( ItemPool.MAYOSTAT ) );
				RequestLogger.printLine( "Mayo Minder&trade; reminded you to use Mayostat (" + itemsUsed + " times)" );
				ResultProcessor.processItem( ItemPool.MAYOSTAT, -itemsUsed );
			}
			else if ( responseText.contains( "Mayozapine kicks in" ) )
			{
				int itemsUsed = Math.min( count, InventoryManager.getCount( ItemPool.MAYOZAPINE ) );
				RequestLogger.printLine( "Mayo Minder&trade; reminded you to use Mayozapine (" + itemsUsed + " times)" );
				ResultProcessor.processItem( ItemPool.MAYOZAPINE, -itemsUsed );
			}
			else if ( responseText.contains( "Mayoflex kicks in" ) )
			{
				int itemsUsed = Math.min( count, InventoryManager.getCount( ItemPool.MAYOFLEX ) );
				RequestLogger.printLine( "Mayo Minder&trade; reminded you to use Mayoflex (" + itemsUsed + " times)" );
				ResultProcessor.processItem( ItemPool.MAYOFLEX, -itemsUsed );
			}
		}
		
		// With Mayonex, you gain mayoLevel rather than adventures
		if ( responseText.contains( "feel the Mayonex gurgling" ) )
		{
			Matcher mayonexMatcher = EatItemRequest.MAYONEX_PATTERN.matcher( responseText );
			while ( mayonexMatcher.find() )
			{
				Preferences.increment( "mayoLevel", StringUtilities.parseInt( mayonexMatcher.group( 1 ) ) );
			}
		}

		// If you had mayo in your mouth, you do no longer
		Preferences.setString( "mayoInMouth", "" );

		// If the user has fullness display turned on ( "You gain x
		// Fullness" ) DON'T touch fullness here.  It is handled in
		// ResultProcessor.
		if ( !responseText.contains( " Fullness" ) )
		{
			int fullness = ConsumablesDatabase.getFullness( itemName );
			int fullnessUsed = fullness * count;

			// The Mayodiol kicks in and converts some of what you just ate into pure ethanol.
			if ( responseText.contains( "Mayodiol kicks in" ) )
			{
				fullnessUsed -= 1;
			}

			KoLCharacter.setFullness( KoLCharacter.getFullness() + fullnessUsed );
		}

		Preferences.decrement( "munchiesPillsUsed", count );
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
