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

package net.sourceforge.kolmafia.objectpool;

import java.util.ArrayList;
import java.util.List;

import net.java.dev.spellcast.utilities.LockableListModel;
import net.java.dev.spellcast.utilities.SortedListModel;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.persistence.ConcoctionDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.persistence.Preferences;
import net.sourceforge.kolmafia.request.MallPurchaseRequest;
import net.sourceforge.kolmafia.utilities.StringUtilities;

/**
 * Internal class used to represent a single concoction. It contains all the information needed to actually make the
 * item.
 */

public class Concoction
	implements Comparable
{
	private static final int FOOD_PRIORITY = 1;
	private static final int BOOZE_PRIORITY = 2;
	private static final int SPLEEN_PRIORITY = 3;
	private static final int NO_PRIORITY = 100;

	private final String name;
	public final AdventureResult concoction;

	private final int yield;
	private final int mixingMethod;
	private final int sortOrder;

	private final boolean isTorsoItem;
	private final boolean isReagentPotion;

	private boolean wasPossible;

	private boolean isMarking;
	private boolean isCalculating;

	private final int price;

	private final List ingredients;
	private AdventureResult[] ingredientArray;
	private int modifier, multiplier;

	public int creatable;
	public int queued;
	public int initial;
	public int total;
	public int visibleTotal;

	private final int fullness, inebriety, spleenhit;

	public Concoction( final String name, final int price )
	{
		this.name = name;
		this.concoction = null;
		this.yield = 1;

		this.mixingMethod = KoLConstants.NOCREATE;
		this.isTorsoItem = false;
		this.isReagentPotion = false;
		this.wasPossible = false;

		this.ingredients = new ArrayList();
		this.ingredientArray = new AdventureResult[ 0 ];

		this.fullness = ItemDatabase.getFullness( name );
		this.inebriety = ItemDatabase.getInebriety( name );
		this.spleenhit = ItemDatabase.getSpleenHit( name );

		int consumeType = this.fullness > 0 ? KoLConstants.CONSUME_EAT : this.inebriety > 0 ? KoLConstants.CONSUME_DRINK : KoLConstants.CONSUME_USE;

		switch ( consumeType )
		{
		case KoLConstants.CONSUME_EAT:
			this.sortOrder = this.fullness > 0 ? FOOD_PRIORITY : NO_PRIORITY;
			break;
		case KoLConstants.CONSUME_DRINK:
			this.sortOrder = this.inebriety > 0 ? BOOZE_PRIORITY : NO_PRIORITY;
			break;
		default:
			this.sortOrder = this.spleenhit > 0 ? SPLEEN_PRIORITY : NO_PRIORITY;
			break;
		}

		this.price = price;
		this.resetCalculations();
	}

	public Concoction( final AdventureResult concoction, final int mixingMethod )
	{
		this.concoction = concoction;

		this.wasPossible = false;
		this.mixingMethod = mixingMethod;

		int consumeType =
			concoction == null ? 0 : ItemDatabase.getConsumptionType( concoction.getItemId() );
		
		if ( concoction != null )
		{
			this.yield = Math.max( concoction.getCount(), 1 );
			this.name = concoction.getName();
			this.isTorsoItem = consumeType == KoLConstants.EQUIP_SHIRT;

			this.isReagentPotion =
				(this.mixingMethod == KoLConstants.COOK_REAGENT || this.mixingMethod == KoLConstants.SUPER_REAGENT) &&
				(consumeType == KoLConstants.CONSUME_USE || consumeType == KoLConstants.CONSUME_MULTIPLE);
		}
		else
		{
			this.yield = 1;
			this.name = "unknown";
			
			this.isTorsoItem = false;
			this.isReagentPotion = false;
		}

		this.fullness = ItemDatabase.getFullness( this.name );
		this.inebriety = ItemDatabase.getInebriety( this.name );
		this.spleenhit = ItemDatabase.getSpleenHit( this.name );

		this.ingredients = new ArrayList();
		this.ingredientArray = new AdventureResult[ 0 ];

		switch ( consumeType )
		{
		case KoLConstants.CONSUME_EAT:
			this.sortOrder = this.fullness > 0 ? FOOD_PRIORITY : NO_PRIORITY;
			break;
		case KoLConstants.CONSUME_DRINK:
			this.sortOrder = this.inebriety > 0 ? BOOZE_PRIORITY : NO_PRIORITY;
			break;
		default:
			this.sortOrder = this.spleenhit > 0 ? SPLEEN_PRIORITY : NO_PRIORITY;
			break;
		}

		this.price = -1;
	}

	public int getYield()
	{
		if ( ConcoctionDatabase.tripleReagent && this.isReagentPotion() )
		{
			return 3 * this.yield;
		}

		return this.yield;
	}

	public boolean isReagentPotion()
	{
		return this.isReagentPotion;
	}

	public int compareTo( final Object o )
	{
		if ( o == null || !( o instanceof Concoction ) )
		{
			return -1;
		}

		if ( this.name == null )
		{
			return ( (Concoction) o ).name == null ? 0 : 1;
		}

		if ( ( (Concoction) o ).name == null )
		{
			return -1;
		}

		if ( this.sortOrder != ( (Concoction) o ).sortOrder )
		{
			return this.sortOrder - ( (Concoction) o ).sortOrder;
		}

		if ( this.sortOrder == NO_PRIORITY )
		{
			return this.name.compareToIgnoreCase( ( (Concoction) o ).name );
		}

		if ( !Preferences.getBoolean( "showGainsPerUnit" ) )
		{
			int fullness1 = this.fullness;
			int fullness2 = ( (Concoction) o ).fullness;

			if ( fullness1 != fullness2 )
			{
				return fullness2 - fullness1;
			}

			int inebriety1 = this.inebriety;
			int inebriety2 = ( (Concoction) o ).inebriety;

			if ( inebriety1 != inebriety2 )
			{
				return inebriety2 - inebriety1;
			}

			int spleenhit1 = this.spleenhit;
			int spleenhit2 = ( (Concoction) o ).spleenhit;

			if ( spleenhit1 != spleenhit2 )
			{
				return spleenhit2 - spleenhit1;
			}
		}

		float adventures1 = StringUtilities.parseFloat( ItemDatabase.getAdventureRange( this.name ) );
		float adventures2 =
			StringUtilities.parseFloat( ItemDatabase.getAdventureRange( ( (Concoction) o ).name ) );

		if ( adventures1 != adventures2 )
		{
			return adventures2 - adventures1 > 0.0f ? 1 : -1;
		}

		return this.name.compareToIgnoreCase( ( (Concoction) o ).name );
	}

	public boolean equals( final Object o )
	{
		if ( o == null || !( o instanceof Concoction ) )
		{
			return false;
		}

		if ( this.name == null )
		{
			return ( (Concoction) o ).name == null;
		}

		if ( ( (Concoction) o ).name == null )
		{
			return false;
		}

		return this.name.equals( ( (Concoction) o ).name );
	}

	public AdventureResult getItem()
	{
		return this.concoction;
	}

	public int getItemId()
	{
		return this.concoction == null ? -1 : this.concoction.getItemId();
	}

	public String getName()
	{
		return this.name;
	}

	public int getInitial()
	{
		return this.initial;
	}

	public int getAvailable()
	{
		return this.price > 0 ? KoLCharacter.getAvailableMeat() / this.price : this.visibleTotal;
	}

	public int getQueued()
	{
		return this.queued;
	}

	public int getPrice()
	{
		return this.price;
	}

	public int getFullness()
	{
		return this.fullness;
	}

	public int getInebriety()
	{
		return this.inebriety;
	}

	public int getSpleenHit()
	{
		return this.spleenhit;
	}

	public void queue( final LockableListModel globalChanges, final ArrayList localChanges, final int amount )
	{
		this.queue( globalChanges, localChanges, amount, true );
	}

	public void queue( final LockableListModel globalChanges, final ArrayList localChanges, final int amount, boolean adjust )
	{
		if ( amount <= 0 )
		{
			return;
		}

		if ( this.concoction == null )
		{
			if ( adjust )
			{
				this.queued += amount;
			}

			return;
		}

		int decrementAmount = Math.min( this.initial, amount );
		int overAmount = amount - decrementAmount;

		// Tiny plastic swords are special in that they
		// are not used up.

		if ( this.concoction.getItemId() != ItemPool.PLASTIC_SWORD )
		{
			AdventureResult ingredient = this.concoction.getInstance( decrementAmount );
			AdventureResult.addResultToList( globalChanges, ingredient );
			AdventureResult.addResultToList( localChanges, ingredient );
		}

		int advs = ConcoctionDatabase.ADVENTURE_USAGE[ this.mixingMethod ] * overAmount;
		if ( advs != 0 )
		{
			ConcoctionDatabase.queuedAdventuresUsed += advs;
		}

		if ( this.mixingMethod == KoLConstants.STILL_BOOZE || this.mixingMethod == KoLConstants.STILL_MIXER )
		{
			ConcoctionDatabase.queuedStillsUsed += overAmount;
		}

		if ( adjust )
		{
			this.queued += amount;
		}

		// Recipes that yield multiple units require smaller
		// quantities of ingredients.

		int mult = this.getYield();
		int icount = ( overAmount + ( mult - 1 ) ) / mult;
		for ( int i = 0; i < this.ingredientArray.length; ++i )
		{
			AdventureResult ingredient = this.ingredientArray[ i ];
			Concoction c = ConcoctionPool.get( ingredient.getItemId() );
			c.queue( globalChanges, localChanges, icount, false );
		}

		// Recipes that yield multiple units might result in
		// extra product which can be used for other recipes.

		int excess = mult * icount - overAmount;
		if ( excess > 0	 )
		{
		}
	}

	public void resetCalculations()
	{
		this.initial = -1;
		this.creatable = 0;
		this.total = 0;

		this.modifier = 0;
		this.multiplier = 0;

		if ( this.concoction == null && this.name != null )
		{
			this.initial = KoLCharacter.getAvailableMeat() / this.price;
			this.creatable = -1;
			this.total = this.initial;
		}
	}

	public void setPossible( final boolean wasPossible )
	{
		this.wasPossible = wasPossible;
	}

	public boolean wasPossible()
	{
		return this.wasPossible;
	}

	public void addIngredient( final AdventureResult ingredient )
	{
		SortedListModel uses = ConcoctionDatabase.knownUses.get( ingredient.getItemId() );
		if ( uses == null )
		{
			uses = new SortedListModel();
			ConcoctionDatabase.knownUses.set( ingredient.getItemId(), uses );
		}

		uses.add( this.concoction );
		this.ingredients.add( ingredient );

		this.ingredientArray = new AdventureResult[ this.ingredients.size() ];
		this.ingredients.toArray( this.ingredientArray );
	}

	public int getMixingMethod()
	{
		return this.mixingMethod;
	}

	public AdventureResult[] getIngredients()
	{
		return this.ingredientArray;
	}

	public void calculate( final List availableIngredients )
	{
		// If a calculation has already been done for this
		// concoction, no need to calculate again.

		if ( this.initial != -1 )
		{
			return;
		}

		// Initialize creatable item count to 0.  This way,
		// you ensure that you're not always off by one.

		this.creatable = 0;

		// If the item doesn't exist in the item table,
		// then assume it can't be created.

		if ( this.concoction == null || this.name == null )
		{
			return;
		}

		// Determine how many were available initially in the
		// available ingredient list.

		this.initial = this.concoction.getCount( availableIngredients );
		this.total = this.initial;

		if ( !ConcoctionDatabase.isPermittedMethod( this.mixingMethod ) )
		{
			this.visibleTotal = this.total;
			return;
		}

		if ( this.isTorsoItem && !KoLCharacter.hasSkill( "Torso Awaregness" ) )
		{
			this.visibleTotal = this.total;
			return;
		}

		// First, preprocess the ingredients by calculating
		// how many of each ingredient is possible now.

		for ( int i = 0; i < this.ingredientArray.length; ++i )
		{
			AdventureResult ingredient = this.ingredientArray[ i ];
			Concoction c = ConcoctionPool.get( ingredient.getItemId() );
			c.calculate( availableIngredients );
		}

		this.mark( 0, 1 );

		// With all of the data preprocessed, calculate
		// the quantity creatable by solving the set of
		// linear inequalities.

		this.total = MallPurchaseRequest.MAX_QUANTITY;

		for ( int i = 0; i < this.ingredientArray.length; ++i )
		{
			AdventureResult ingredient = this.ingredientArray[ i ];
			Concoction c = ConcoctionPool.get( ingredient.getItemId() );

			int available = c.quantity();
			this.total = Math.min( this.total, available );
		}

		if ( ConcoctionDatabase.ADVENTURE_USAGE[ this.mixingMethod ] != 0 )
		{
			Concoction c = ConcoctionDatabase.adventureLimit;
			int available = c.quantity();
			this.total = Math.min( this.total, available );
		}

		if ( this.mixingMethod == KoLConstants.STILL_MIXER || this.mixingMethod == KoLConstants.STILL_BOOZE )
		{
			Concoction c = ConcoctionDatabase.stillsLimit;
			int available = c.quantity();
			this.total = Math.min( this.total, available );
		}

		// The total available for other creations is
		// equal to the total, less the initial.

		this.creatable = ( this.total - this.initial ) * this.getYield();
		this.total = this.initial + this.creatable;

		// Now that all the calculations are complete, unmark
		// the ingredients so that later calculations can make
		// the correct calculations.

		this.visibleTotal = this.total;
		this.unmark();
	}

	/**
	 * Utility method which calculates the quantity available for a recipe based on the modifier/multiplier of its
	 * ingredients
	 */

	private int quantity()
	{
		// If there is no multiplier, assume that an infinite
		// number is available.

		if ( this.multiplier == 0 )
		{
			return MallPurchaseRequest.MAX_QUANTITY;
		}

		// The maximum value is equivalent to the total, plus
		// the modifier, divided by the multiplier, if the
		// multiplier exists.

		int quantity = ( this.total + this.modifier ) / this.multiplier;
		
		// The true value is affected by the maximum value for
		// the ingredients.  Therefore, calculate the quantity
		// for all other ingredients to complete the solution
		// of the linear inequality.

		int mult = this.getYield();

		this.isCalculating = true;
		
		for ( int i = 0; quantity > 0 && i < this.ingredientArray.length; ++i )
		{
			AdventureResult ingredient = this.ingredientArray[ i ];
			Concoction c = ConcoctionPool.get( ingredient.getItemId() );

			// Avoid mutual recursion.
			
			if ( !c.isCalculating )
			{
				int available = c.quantity() * mult;
				quantity = Math.min( quantity, available );
			}			
		}
		
		// Adventures are also considered an ingredient; if
		// no adventures are necessary, the multiplier should
		// be zero and the infinite number available will have
		// no effect on the calculation.

		if ( ConcoctionDatabase.ADVENTURE_USAGE[ this.mixingMethod ] != 0 )
		{
			Concoction c = ConcoctionDatabase.adventureLimit;
			int available = c.quantity() * mult;
			quantity = Math.min( quantity, available );
		}

		// Still uses are also considered an ingredient.

		if ( this.mixingMethod == KoLConstants.STILL_MIXER || this.mixingMethod == KoLConstants.STILL_BOOZE )
		{
			Concoction c = ConcoctionDatabase.stillsLimit;
			int available = c.quantity() * mult;
			quantity = Math.min( quantity, available );
		}

		// The true value is now calculated.  Return this
		// value to the requesting method.

		this.isCalculating = false;
		return quantity;
	}

	/**
	 * Utility method which marks the ingredient for usage with the given added modifier and the given additional
	 * multiplier.
	 */

	private void mark( final int modifier, final int multiplier )
	{
		this.modifier += modifier;
		this.multiplier += multiplier;

		this.isMarking = true;
		
		// Mark all the ingredients, being sure to multiply
		// by the number of that ingredient needed in this
		// concoction.

		int instanceCount;

		for ( int i = 0; i < this.ingredientArray.length; ++i )
		{
			boolean shouldMark = true;
			instanceCount = this.ingredientArray[ i ].getCount();

			for ( int j = 0; j < i && shouldMark; ++j )
			{
				shouldMark = this.ingredientArray[ i ].getItemId() != this.ingredientArray[ j ].getItemId();
			}

			if ( !shouldMark )
			{
				continue;
			}

			for ( int j = i + 1; j < this.ingredientArray.length; ++j )
			{
				if ( this.ingredientArray[ i ].getItemId() == this.ingredientArray[ j ].getItemId() )
				{
					instanceCount += this.ingredientArray[ j ].getCount();
				}
			}

			Concoction c = ConcoctionPool.get( this.ingredientArray[ i ].getItemId() );

			// Avoid mutual recursion
			
			if ( !c.isMarking )
			{
				c.mark( ( this.modifier + this.initial ) * instanceCount, this.multiplier * instanceCount );
			}
		}

		// Mark the implicit adventure ingredient, being
		// sure to multiply by the number of adventures
		// which are required for this mixture.

		if ( ConcoctionDatabase.ADVENTURE_USAGE[ this.mixingMethod ] != 0 )
		{
			ConcoctionDatabase.adventureLimit.mark(
				( this.modifier + this.initial ) * ConcoctionDatabase.ADVENTURE_USAGE[ this.mixingMethod ],
				this.multiplier * ConcoctionDatabase.ADVENTURE_USAGE[ this.mixingMethod ] );
		}

		if ( this.mixingMethod == KoLConstants.STILL_MIXER || this.mixingMethod == KoLConstants.STILL_BOOZE )
		{
			ConcoctionDatabase.stillsLimit.mark( ( this.modifier + this.initial ), this.multiplier );
		}

		this.isMarking = false;
	}

	/**
	 * Utility method which undoes the yielding process, resetting the ingredient and current total values to the
	 * given number.
	 */

	private void unmark()
	{
		if ( this.modifier == 0 && this.multiplier == 0 )
		{
			return;
		}

		this.modifier = 0;
		this.multiplier = 0;

		for ( int i = 0; i < this.ingredientArray.length; ++i )
		{
			ConcoctionPool.get( this.ingredientArray[ i ].getItemId() ).unmark();
		}

		if ( ConcoctionDatabase.ADVENTURE_USAGE[ this.mixingMethod ] != 0 )
		{
			ConcoctionDatabase.adventureLimit.unmark();
		}

		if ( this.mixingMethod == KoLConstants.STILL_MIXER || this.mixingMethod == KoLConstants.STILL_BOOZE )
		{
			ConcoctionDatabase.stillsLimit.unmark();
		}
	}

	public int getMeatPasteNeeded( final int quantityNeeded )
	{
		// Avoid mutual recursion.

		if ( this.mixingMethod != KoLConstants.COMBINE || KoLCharacter.inMuscleSign() || quantityNeeded <= this.initial )
		{
			return 0;
		}

		// Count all the meat paste from the different
		// levels in the creation tree.

		int runningTotal = 0;
		for ( int i = 0; i < this.ingredientArray.length; ++i )
		{
			Concoction ingredient = ConcoctionPool.get( this.ingredientArray[ i ].getItemId() );

			runningTotal += ingredient.getMeatPasteNeeded( quantityNeeded - ingredient.initial );
		}

		return runningTotal + quantityNeeded;
	}

	/**
	 * Returns the string form of this concoction. This is basically the display name for the item created.
	 */

	public String toString()
	{
		return this.name;
	}
}