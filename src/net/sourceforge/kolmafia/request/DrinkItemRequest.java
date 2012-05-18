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

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.RequestThread;

import net.sourceforge.kolmafia.objectpool.ItemPool;

import net.sourceforge.kolmafia.persistence.ConcoctionDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.persistence.MonsterDatabase;
import net.sourceforge.kolmafia.persistence.SkillDatabase;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.session.InventoryManager;
import net.sourceforge.kolmafia.session.ResponseTextParser;
import net.sourceforge.kolmafia.session.ResultProcessor;

import net.sourceforge.kolmafia.swingui.GenericFrame;

import net.sourceforge.kolmafia.utilities.InputFieldUtilities;

public class DrinkItemRequest
	extends UseItemRequest
{
	private static int permittedOverdrink = 0;
	private static int askedAboutOde = 0;
	private static AdventureResult queuedDrinkHelper = null;
	private static int queuedDrinkHelperCount = 0;

	public DrinkItemRequest( final AdventureResult item )
	{
		super( ItemDatabase.getConsumptionType( item.getItemId() ), item );
	}

	public int getAdventuresUsed()
	{
		return 0;
	}

	public static final void permitOverdrink()
	{
		DrinkItemRequest.permittedOverdrink = KoLCharacter.getUserId();
	}

	public static final void clearDrinkHelper()
	{
		DrinkItemRequest.queuedDrinkHelper = null;
		DrinkItemRequest.queuedDrinkHelperCount = 0;
	}

	public static final int maximumUses( final int itemId, final String itemName, final int inebriety, final boolean allowOverDrink )
	{
		UseItemRequest.limiter = "inebriety";
		int limit = KoLCharacter.getInebrietyLimit();

		switch ( itemId )
		{
		case ItemPool.GREEN_BEER:
			// Green Beer allows drinking to limit + 10,
			// but only on SSPD. For now, always allow
			limit += 10;
			break;

		case ItemPool.RED_DRUNKI_BEAR:
		case ItemPool.GREEN_DRUNKI_BEAR:
		case ItemPool.YELLOW_DRUNKI_BEAR:
			// drunki-bears give inebriety but are limited by your fullness.
			return EatItemRequest.maximumUses( itemId, itemName, 4 );
		}

		int inebrietyLeft = limit - KoLCharacter.getInebriety();

		if ( inebrietyLeft < 0 )
		{
			// We are already drunk
			return 0;
		}

		if ( inebrietyLeft < inebriety )
		{
			// One drink will make us drunk
			return 1;
		}

		if ( allowOverDrink )
		{
			// Multiple drinks will make us drunk
			return inebrietyLeft / inebriety + 1;
		}

		// Multiple drinks do not quite make us drunk
		return inebrietyLeft / inebriety;
	}

	public void run()
	{
		if ( this.consumptionType == KoLConstants.CONSUME_DRINK_HELPER )
		{
			int count = this.itemUsed.getCount();

			if ( !InventoryManager.retrieveItem( this.itemUsed ) )
			{
				KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "Helper not available." );
				return;
			}

			if ( this.itemUsed.equals( DrinkItemRequest.queuedDrinkHelper ) )
			{
				DrinkItemRequest.queuedDrinkHelperCount += count;
			}
			else
			{
				DrinkItemRequest.queuedDrinkHelper = this.itemUsed;
				DrinkItemRequest.queuedDrinkHelperCount = count;
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

		if ( !DrinkItemRequest.sequentialConsume( itemId ) &&
		     !InventoryManager.retrieveItem( this.itemUsed ) )
		{
			return;
		}

		int iterations = 1;
		int origCount = this.itemUsed.getCount();

		// The miracle of "consume some" does not apply to TPS drinks
		if ( origCount != 1 &&
		     ( DrinkItemRequest.singleConsume( itemId ) ||
		       ( DrinkItemRequest.sequentialConsume( itemId ) && InventoryManager.getCount( itemId ) < origCount) ) )
		{
			iterations = origCount;
			this.itemUsed = this.itemUsed.getInstance( 1 );
		}

		String originalURLString = this.getURLString();

		for ( int i = 1; i <= iterations && KoLmafia.permitsContinue(); ++i )
		{
			if ( !this.allowBoozeConsumption() )
			{
				return;
			}

			this.constructURLString( originalURLString );
			this.useOnce( i, iterations, "Drinking" );
		}

		if ( KoLmafia.permitsContinue() )
		{
			KoLmafia.updateDisplay( "Finished drinking " + origCount + " " + this.itemUsed.getName() + "." );
		}
	}

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

		if ( DrinkItemRequest.queuedDrinkHelper != null && DrinkItemRequest.queuedDrinkHelperCount > 0 )
		{
			int helperItemId = DrinkItemRequest.queuedDrinkHelper.getItemId(); 
			if ( helperItemId == ItemPool.FROSTYS_MUG )
			{
				UseItemRequest.lastUpdate = UseItemRequest.elementalHelper( "Coldform", MonsterDatabase.COLD, 1000 );
				if ( !UseItemRequest.lastUpdate.equals( "" ) )
				{
					KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, UseItemRequest.lastUpdate );
					DrinkItemRequest.queuedDrinkHelper = null;
					return;
				}
			}
			this.addFormField( "utensil", String.valueOf( helperItemId ) );
			--DrinkItemRequest.queuedDrinkHelperCount;
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
		return ( DrinkItemRequest.queuedDrinkHelper != null && DrinkItemRequest.queuedDrinkHelperCount > 0 );
	}

	private static final boolean sequentialConsume( final int itemId )
	{
		switch (itemId )
		{
		case ItemPool.DIRTY_MARTINI:
		case ItemPool.GROGTINI:
		case ItemPool.CHERRY_BOMB:
		case ItemPool.VESPER:
		case ItemPool.BODYSLAM:
		case ItemPool.SANGRIA_DEL_DIABLO:
			// Allow player who owns a single tiny plastic sword to
			// make and drink multiple drinks in succession.
			return true;
		}
		return false;
	}

	private final boolean allowBoozeConsumption()
	{
		// Always allow the steel margarita
		int itemId = this.itemUsed.getItemId();
		if ( itemId == ItemPool.STEEL_LIVER )
		{
			return true;
		}

		int inebriety = ItemDatabase.getInebriety( this.itemUsed.getName() );
		int count = this.itemUsed.getCount();
		String itemName = this.itemUsed.getName();
		String advGain = ItemDatabase.getAdvRangeByName( itemName );

		return DrinkItemRequest.allowBoozeConsumption( inebriety, count, advGain );
	}

	public static final boolean allowBoozeConsumption( final int inebriety, final int count, String advGain )
	{
		int inebrietyBonus = inebriety * count;
		if ( inebrietyBonus < 1 )
		{
			return true;
		}

		if ( KoLCharacter.isFallingDown() )
		{
			return true;
		}

		if ( !GenericFrame.instanceExists() )
		{
			return true;
		}

		if ( !DrinkItemRequest.askAboutOde( inebriety, count, advGain ) )
		{
			return false;
		}

		// Make sure the player does not overdrink if they still
		// have PvP attacks remaining.

		if ( KoLCharacter.getInebriety() + inebrietyBonus > KoLCharacter.getInebrietyLimit() &&
		     DrinkItemRequest.permittedOverdrink != KoLCharacter.getUserId() )
		{
			if ( KoLCharacter.getAdventuresLeft() > 0 && !InputFieldUtilities.confirm( "Are you sure you want to overdrink?" ) )
			{
				return false;
			}
		}

		return true;
	}

	private static final boolean askAboutOde( final int inebriety, final int count, String advGain )
	{
		// If we've already asked about ode, don't nag
		if ( DrinkItemRequest.askedAboutOde == KoLCharacter.getUserId() )
		{
			return true;
		}

		// If user specifically said not to worry about ode, don't nag
		// Actually, this overloads the "allowed to overdrink" flag.
		if ( DrinkItemRequest.permittedOverdrink == KoLCharacter.getUserId() )
		{
			return true;
		}
		
		// If the item doesn't give any adventures, it won't benefit from ode
		if ( advGain == "0" )
		{
			return true;
		}

		// See if already have enough turns of Ode to Booze
		int odeTurns = ItemDatabase.ODE.getCount( KoLConstants.activeEffects );
		int consumptionTurns = count * inebriety;

		if ( consumptionTurns <= odeTurns )
		{
			return true;
		}

		// If the character doesn't know ode, there is nothing to do.
		UseSkillRequest ode = UseSkillRequest.getInstance( "The Ode to Booze" );
		boolean canOde = KoLConstants.availableSkills.contains( ode ) && UseSkillRequest.hasAccordion();

		if ( !canOde )
		{
			return true;
		}

		// Cast Ode automatically if you have enough mana,
		// when you are out of Ronin/HC
		int odeCost = SkillDatabase.getMPConsumptionById( 6014 );
		while ( KoLCharacter.canInteract() &&
			odeTurns < consumptionTurns &&
			KoLCharacter.getCurrentMP() >= odeCost &&
			KoLmafia.permitsContinue() )
		{
			ode.setBuffCount( 1 );
			RequestThread.postRequest( ode );
			int newTurns = ItemDatabase.ODE.getCount( KoLConstants.activeEffects );
			if ( odeTurns == newTurns )
			{
				// No progress
				break;
			}
			odeTurns = newTurns;
		}

		if ( consumptionTurns <= odeTurns )
		{
			return true;
		}

		String message = odeTurns > 0 ?
			"The Ode to Booze will run out before you finish drinking that. Are you sure?" :
			"Are you sure you want to drink without ode?";
		if ( !InputFieldUtilities.confirm( message ) )
		{
			return false;
		}

		DrinkItemRequest.askedAboutOde = KoLCharacter.getUserId();

		return true;
	}

	public static final void parseConsumption( final AdventureResult item, final AdventureResult helper, final String responseText )
	{
		if ( responseText.indexOf( "too drunk" ) != -1 )
		{
			UseItemRequest.lastUpdate = "Inebriety limit reached.";
			KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, UseItemRequest.lastUpdate );
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
			case ItemPool.DIVINE_FLUTE:
				// "You pour the <drink> into your divine
				// champagne flute, and it immediately begins
				// fizzing over. You drink it quickly, then
				// throw the flute in front of a plastic
				// fireplace and break it."

				if ( responseText.indexOf( "a plastic fireplace" ) == -1 )
				{
					success = false;
				}
				break;

			case ItemPool.FROSTYS_MUG:

				// "Brisk! Refreshing! You drink the frigid
				// <drink> and discard the no-longer-frosty
				// mug."

				if ( responseText.indexOf( "discard the no-longer-frosty" ) == -1 )
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

		// Assume initially that this causes the item to disappear.
		// In the event that the item is not used, then proceed to
		// undo the consumption.

		if ( consumptionType == KoLConstants.CONSUME_DRINK_HELPER )
		{
			// Consumption helpers are removed above when you
			// successfully eat or drink.
			return;
		}

		// The drink was consumed successfully
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
		case ItemPool.STEEL_LIVER:
			if ( responseText.indexOf( "You acquire a skill" ) != -1 )
			{
				ResponseTextParser.learnSkill( "Liver of Steel" );
			}
			return;

		case ItemPool.FERMENTED_PICKLE_JUICE:
			Preferences.setInteger( "currentSpleenUse",
				Math.max( 0, Preferences.getInteger( "currentSpleenUse" ) -
					5 * item.getCount() ) );
			KoLCharacter.updateStatus();
			return;
		}
	}

	public static final boolean registerRequest()
	{
		AdventureResult item = UseItemRequest.lastItemUsed;
		int count = item.getCount();
		String name = item.getName();

		String useString = "drink " + count + " " + name ;

		RequestLogger.updateSessionLog();
		RequestLogger.updateSessionLog( useString );
		return true;
	}
}
