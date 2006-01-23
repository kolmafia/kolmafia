/**
 * Copyright (c) 2005, KoLmafia development team
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
 *  [3] Neither the name "KoLmafia development team" nor the names of
 *      its contributors may be used to endorse or promote products
 *      derived from this software without specific prior written
 *      permission.
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

import java.util.List;
import java.util.ArrayList;
import java.io.BufferedReader;
import javax.swing.SwingUtilities;
import net.java.dev.spellcast.utilities.SortedListModel;

/**
 * A static class which retrieves all the concoctions available in
 * the Kingdom of Loathing.  This class technically uses up a lot
 * more memory than it needs to because it creates an array storing
 * all possible item combinations, but that's okay!  Because it's
 * only temporary.  Then again, this is supposedly true of all the
 * flow-control using exceptions, but that hasn't been changed.
 */

public class ConcoctionsDatabase extends KoLDatabase
{
	public static final SortedListModel concoctionsList = new SortedListModel();
	public static final int ITEM_COUNT = TradeableItemDatabase.ITEM_COUNT;
	public static final int METHOD_COUNT = ItemCreationRequest.METHOD_COUNT;

	private static boolean [] wasPossible = new boolean[ ITEM_COUNT ];
	private static Concoction [] concoctions = new Concoction[ ITEM_COUNT ];

	private static boolean [] PERMIT_METHOD = new boolean[ METHOD_COUNT ];
	private static int [] ADVENTURE_USAGE = new int[ METHOD_COUNT ];

	private static final int CHEF = 438;
	private static final int CLOCKWORK_CHEF = 1112;
	private static final int BARTENDER = 440;
	private static final int CLOCKWORK_BARTENDER = 1111;

	public static final AdventureResult CAR = new AdventureResult( 134, 1 );
	private static final AdventureResult OVEN = new AdventureResult( 157, 1 );
	private static final AdventureResult KIT = new AdventureResult( 236, 1 );
	private static final AdventureResult HAMMER = new AdventureResult( 338, 1 );
	private static final AdventureResult PLIERS = new AdventureResult( 709, 1 );

	private static final AdventureResult ROLLING_PIN = new AdventureResult( 873, 1 );
	private static final AdventureResult UNROLLING_PIN = new AdventureResult( 873, 1 );

	private static final int DOUGH = 159;
	private static final int FLAT_DOUGH = 301;

	static
	{
		// This begins by opening up the data file and preparing
		// a buffered reader; once this is done, every line is
		// examined and double-referenced: once in the name-lookup,
		// and again in the ID lookup.

		BufferedReader reader = getReader( "concoctions.dat" );
		String [] data;

		while ( (data = readData( reader )) != null )
		{
			try
			{
				if ( data.length > 2 )
				{
					AdventureResult item = AdventureResult.parseResult( data[0] );
					int itemID = item.getItemID();

					if ( itemID != -1 )
					{
						int mixingMethod = Integer.parseInt( data[1] );
						Concoction concoction = new Concoction( item, mixingMethod );

						for ( int i = 2; i < data.length; ++i )
							concoction.addIngredient( parseIngredient( data[i] ) );

						if ( !concoction.isBadRecipe() )
						{
							concoctions[itemID] = concoction;
							continue;
						}
					}

					// Bad item or bad ingredients
					System.out.println( "Bad recipe: " + data[0] );
				}
			}
			catch ( Exception e )
			{
				// If an exception is thrown, then something bad
				// happened, so do absolutely nothing.

				e.printStackTrace( KoLmafia.getLogStream() );
				e.printStackTrace();
			}
		}

		for ( int i = 0; i < ITEM_COUNT; ++i )
		{
			wasPossible[i] = false;
			if ( concoctions[i] == null )
				concoctions[i] = new Concoction( new AdventureResult( i, 0 ), ItemCreationRequest.NOCREATE );
		}
	}

	public static final boolean isPermittedMethod( int method )
	{	return PERMIT_METHOD[ method ];
	}

	private static AdventureResult parseIngredient( String data )
	{
		try
		{
			// If the ingredient is specified inside of brackets,
			// then a specific item ID is being designated.

			if ( data.startsWith( "[" ) )
			{
				int closeBracketIndex = data.indexOf( "]" );
				String itemIDString = data.substring( 0, closeBracketIndex ).replaceAll( "[\\[\\]]", "" ).trim();
				String quantityString = data.substring( closeBracketIndex + 1 ).trim();

				return new AdventureResult( df.parse( itemIDString ).intValue(), quantityString.length() == 0 ? 1 :
					df.parse( quantityString.replaceAll( "[\\(\\)]", "" ) ).intValue() );
			}

			// Otherwise, it's a standard ingredient - use
			// the standard adventure result parsing routine.

			return AdventureResult.parseResult( data );
		}
		catch ( Exception e )
		{
			e.printStackTrace( KoLmafia.getLogStream() );
			e.printStackTrace();

			return null;
		}
	}

	public static synchronized SortedListModel getConcoctions()
	{	return concoctionsList;
	}

	/**
	 * Returns the concoctions which are available given the list of
	 * ingredients.  The list returned contains formal requests for
	 * item creation.
	 */

	public static synchronized void refreshConcoctions()
	{
		List availableIngredients = new ArrayList();
		availableIngredients.addAll( KoLCharacter.getInventory() );

		if ( client != null )
		{
			List closetList = (List) KoLCharacter.getCloset();
			for ( int i = 0; i < closetList.size(); ++i )
				AdventureResult.addResultToList( availableIngredients, (AdventureResult) closetList.get(i) );
		}

		// First, zero out the quantities table.  Though this is not
		// actually necessary, it's a good safety and doesn't use up
		// that much CPU time.

		for ( int i = 1; i < ITEM_COUNT; ++i )
			concoctions[i].resetCalculations();

		// Make initial assessment of availability of mixing methods.
		// Do this here since some COMBINE recipes have ingredients
		// made using TINKER recipes.

		cachePermitted();

		// Next, do calculations on all mixing methods which cannot
		// be created.

		for ( int i = 1; i < ITEM_COUNT; ++i )
			if ( concoctions[i].getMixingMethod() == ItemCreationRequest.NOCREATE )
				concoctions[i].calculate( availableIngredients );

		// Adventures are considered Item #0 in the event that the
		// concoction will use ADVs.

		concoctions[0].total = KoLCharacter.getAdventuresLeft();

		// Next, meat paste and meat stacks can be created directly
		// and are dependent upon the amount of meat available.
		// This should also be calculated to allow for meat stack
		// recipes to be calculated.

		int availableMeat = KoLCharacter.getAvailableMeat() + KoLCharacter.getClosetMeat();

		concoctions[ ItemCreationRequest.MEAT_PASTE ].total += availableMeat / 10;
		concoctions[ ItemCreationRequest.MEAT_PASTE ].creatable += availableMeat / 10;
		concoctions[ ItemCreationRequest.MEAT_STACK ].total += availableMeat / 100;
		concoctions[ ItemCreationRequest.MEAT_STACK ].creatable += availableMeat / 100;
		concoctions[ ItemCreationRequest.DENSE_STACK ].total += availableMeat / 1000;
		concoctions[ ItemCreationRequest.DENSE_STACK ].creatable += availableMeat / 1000;

		// Next, increment through all of the things which can be
		// created through the use of meat paste.  This allows for box
		// servant creation to be calculated in advance.

		for ( int i = 1; i < ITEM_COUNT; ++i )
			if ( concoctions[i].getMixingMethod() == ItemCreationRequest.COMBINE )
				concoctions[i].calculate( availableIngredients );

		// Now that we have calculated how many box servants are
		// available, cache permitted mixing methods.

		cachePermitted();

		// Finally, increment through all of the things which are
		// created any other way, making sure that it's a permitted
		// mixture before doing the calculation.

		for ( int i = 1; i < ITEM_COUNT; ++i )
		{
			if ( concoctions[i].concoction.getName() == null )
				continue;

			concoctions[i].calculate( availableIngredients );
		}

		// Now, to update the list of creatables without removing
		// all creatable items.  We do this by determining the
		// number of items inside of the old list.

		ItemCreationRequest currentCreation;
		for ( int i = 1; i < ITEM_COUNT; ++i )
		{
			if ( concoctions[i].getMixingMethod() != ItemCreationRequest.NOCREATE )
			{
				if ( wasPossible[i] )
				{
					currentCreation = ItemCreationRequest.getInstance( client, i, concoctions[i].creatable );

					if ( concoctions[i].creatable > 0 && currentCreation.getCount( concoctionsList ) != concoctions[i].creatable )
					{
						concoctionsList.set( concoctionsList.indexOf( currentCreation ), currentCreation );
						wasPossible[i] = true;
					}
					else if ( concoctions[i].creatable == 0 )
					{
						concoctionsList.remove( currentCreation );
						wasPossible[i] = false;
					}
				}
				else if ( concoctions[i].creatable > 0 )
				{
					concoctionsList.add( ItemCreationRequest.getInstance( client, i, concoctions[i].creatable ) );
					wasPossible[i] = true;
				}
			}
		}
	}

	/**
	 * Utility method used to cache the current permissions on
	 * item creation, based on the given client.
	 */

	private static void cachePermitted()
	{
		boolean noServantNeeded = getProperty( "createWithoutBoxServants" ).equals( "true" );

		// It is never possible to create items which are flagged
		// NOCREATE, and it is always possible to create items
		// through meat paste combination.

		PERMIT_METHOD[ ItemCreationRequest.NOCREATE ] = false;
		ADVENTURE_USAGE[ ItemCreationRequest.NOCREATE ] = 0;

		PERMIT_METHOD[ ItemCreationRequest.COMBINE ] = true;
		ADVENTURE_USAGE[ ItemCreationRequest.COMBINE ] = 0;

		PERMIT_METHOD[ ItemCreationRequest.CLOVER ] = true;
		ADVENTURE_USAGE[ ItemCreationRequest.CLOVER ] = 0;

		// Cooking is permitted, so long as the person has a chef
		// or they don't need a box servant and have an oven.

		PERMIT_METHOD[ ItemCreationRequest.COOK ] = isAvailable( CHEF );

		if ( !PERMIT_METHOD[ ItemCreationRequest.COOK ] && noServantNeeded && KoLCharacter.getInventory().contains( OVEN ) )
		{
			PERMIT_METHOD[ ItemCreationRequest.COOK ] = true;
			ADVENTURE_USAGE[ ItemCreationRequest.COOK ] = 1;
		}
		else
			ADVENTURE_USAGE[ ItemCreationRequest.COOK ] = 0;

		// Cooking of reagents and noodles is possible whenever
		// the person can cook and has the appropriate skill.

		PERMIT_METHOD[ ItemCreationRequest.COOK_REAGENT ] = PERMIT_METHOD[ ItemCreationRequest.COOK ] && KoLCharacter.canSummonReagent();
		ADVENTURE_USAGE[ ItemCreationRequest.COOK_REAGENT ] = ADVENTURE_USAGE[ ItemCreationRequest.COOK ];

		PERMIT_METHOD[ ItemCreationRequest.COOK_PASTA ] = PERMIT_METHOD[ ItemCreationRequest.COOK ] && KoLCharacter.canSummonNoodles();
		ADVENTURE_USAGE[ ItemCreationRequest.COOK_PASTA ] = ADVENTURE_USAGE[ ItemCreationRequest.COOK ];

		// Mixing is possible whenever the person has a bartender
		// or they don't need a box servant and have a kit.

		PERMIT_METHOD[ ItemCreationRequest.MIX ] = isAvailable( BARTENDER );

		if ( !PERMIT_METHOD[ ItemCreationRequest.MIX ] && noServantNeeded && KoLCharacter.getInventory().contains( KIT ) )
		{
			PERMIT_METHOD[ ItemCreationRequest.MIX ] = true;
			ADVENTURE_USAGE[ ItemCreationRequest.MIX ] = 1;
		}
		else
			ADVENTURE_USAGE[ ItemCreationRequest.MIX ] = 0;

		// Mixing of advanced drinks is possible whenever the
		// person can mix drinks and has the appropriate skill.

		PERMIT_METHOD[ ItemCreationRequest.MIX_SPECIAL ] = PERMIT_METHOD[ ItemCreationRequest.MIX ] && KoLCharacter.canSummonShore();
		ADVENTURE_USAGE[ ItemCreationRequest.MIX_SPECIAL ] = ADVENTURE_USAGE[ ItemCreationRequest.MIX ];

		// Smithing of items is possible whenever the person
		// has a hammer.

		PERMIT_METHOD[ ItemCreationRequest.SMITH ] = KoLCharacter.getInventory().contains( HAMMER );

		// Advanced smithing is available whenever the person can
		// smith and has access to the appropriate skill.

		PERMIT_METHOD[ ItemCreationRequest.SMITH_WEAPON ] = PERMIT_METHOD[ ItemCreationRequest.SMITH ] && KoLCharacter.canSmithWeapons();
		ADVENTURE_USAGE[ ItemCreationRequest.SMITH_WEAPON ] = 1;

		PERMIT_METHOD[ ItemCreationRequest.SMITH_ARMOR ] = PERMIT_METHOD[ ItemCreationRequest.SMITH ] && KoLCharacter.canSmithArmor();
		ADVENTURE_USAGE[ ItemCreationRequest.SMITH_ARMOR ] = 1;

		// Standard smithing is also possible if the person is in
		// a muscle sign.

		if ( KoLCharacter.inMuscleSign() )
		{
			PERMIT_METHOD[ ItemCreationRequest.SMITH ] = true;
			ADVENTURE_USAGE[ ItemCreationRequest.SMITH ] = 0;
		}
		else
			ADVENTURE_USAGE[ ItemCreationRequest.SMITH ] = 1;

		// Jewelry making is possible as long as the person has the
		// appropriate pliers.

		PERMIT_METHOD[ ItemCreationRequest.JEWELRY ] = KoLCharacter.getInventory().contains( PLIERS );
		ADVENTURE_USAGE[ ItemCreationRequest.JEWELRY ] = 3;

		// Star charts and pixel chart recipes are available to all
		// players at all times.

		PERMIT_METHOD[ ItemCreationRequest.STARCHART ] = true;
		ADVENTURE_USAGE[ ItemCreationRequest.STARCHART ] = 0;

		PERMIT_METHOD[ ItemCreationRequest.PIXEL ] = true;
		ADVENTURE_USAGE[ ItemCreationRequest.PIXEL ] = 0;

		// A rolling pin or unrolling pin can be always used in item
		// creation because we can get the same effect even without the
		// tool.

		PERMIT_METHOD[ ItemCreationRequest.ROLLING_PIN ] = true;
		ADVENTURE_USAGE[ ItemCreationRequest.ROLLING_PIN ] = 0;

		// The gnomish tinkerer is available if the person is in a
		// moxie sign and they have a bitchin' meat car.

		PERMIT_METHOD[ ItemCreationRequest.TINKER ] = KoLCharacter.inMoxieSign() && KoLCharacter.getInventory().contains( CAR );
		ADVENTURE_USAGE[ ItemCreationRequest.TINKER ] = 0;

		// It's always possible to ask Uncle Crimbo to make toys

		PERMIT_METHOD[ ItemCreationRequest.TOY ] = true;
		ADVENTURE_USAGE[ ItemCreationRequest.TOY ] = 0;
	}

	private static boolean isAvailable( int servantID )
	{
		// If it's a base case, return whether or not the
		// servant is already available at the camp.

		if ( servantID == CHEF && KoLCharacter.hasChef() )
			return true;
		if ( servantID == BARTENDER && KoLCharacter.hasBartender() )
			return true;

		// If the user did not wish to repair their boxes
		// on explosion, then the box servant is not available

		if ( getProperty( "autoRepairBoxes" ).equals( "false" ) )
			return false;

		// Otherwise, return whether or not the quantity possible for
		// the given box servants is non-zero.	This works because
		// cooking tests are made after item creation tests.

		int available = concoctions[ servantID ].total;

		if ( getProperty( "useClockworkBoxes" ).equals( "true" ) )
			available += ( servantID == CHEF) ? concoctions[ CLOCKWORK_CHEF ].total : concoctions[ CLOCKWORK_BARTENDER ].total;

		return available != 0;
	}

	/**
	 * Returns the mixing method for the item with the given ID.
	 */

	public static int getMixingMethod( int itemID )
	{	return concoctions[itemID].getMixingMethod();
	}

	/**
	 * Returns the item IDs of the ingredients for the given item.
	 * Note that if there are no ingredients, then <code>null</code>
	 * will be returned instead.
	 */

	public static AdventureResult [] getIngredients( int itemID )
	{	return concoctions[itemID].getIngredients();
	}

	/**
	 * Internal class used to represent a single concoction.  It
	 * contains all the information needed to actually make the item.
	 */

	private static class Concoction
	{
		private AdventureResult concoction;
		private int mixingMethod;

		private List ingredients;
		private AdventureResult [] ingredientArray;

		private int modifier, multiplier;
		private int initial, creatable, total;

		public Concoction( AdventureResult concoction, int mixingMethod )
		{
			this.concoction = concoction;
			this.mixingMethod = mixingMethod;

			this.ingredients = new ArrayList();
			this.ingredientArray = new AdventureResult[0];
		}

		public void resetCalculations()
		{
			this.initial = -1;
			this.creatable = 0;
			this.total = 0;

			this.modifier = 0;
			this.multiplier = 0;
		}

		public void addIngredient( AdventureResult ingredient )
		{
			ingredients.add( ingredient );
			ingredientArray = new AdventureResult[ ingredients.size() ];
			ingredients.toArray( ingredientArray );
		}

		public int getMixingMethod()
		{	return mixingMethod;
		}

		public boolean isBadRecipe()
		{
			for ( int i = 0; i < ingredientArray.length; ++i )
			{
				AdventureResult ingredient = ingredientArray[i];
				if ( ingredient == null || ingredient.getItemID() == -1 || ingredient.getName() == null )
					return true;
			}

			return false;
		}

		public AdventureResult [] getIngredients()
		{	return ingredientArray;
		}

		public void calculate( List availableIngredients )
		{
			// If a calculation has already been done for this
			// concoction, no need to calculate again.

			if ( this.initial != -1 )
				return;

			// Initialize creatable item count to 0.  This way,
			// you ensure that you're not always off by one.

			this.creatable = 0;

			// If the item doesn't exist in the item table,
			// then assume it can't be created.

			if ( concoction.getName() == null )
			{
				this.initial = 0;
				return;
			}

			// Determine how many were available initially in the
			// available ingredient list.

			this.initial = concoction.getCount( availableIngredients );
			this.total = initial;

			if ( this.mixingMethod == ItemCreationRequest.NOCREATE || !isPermitted() )
				return;

			// First, preprocess the ingredients by calculating
			// how many of each ingredient is possible now.

			for ( int i = 0; i < ingredientArray.length; ++i )
				concoctions[ ingredientArray[i].getItemID() ].calculate( availableIngredients );

			boolean inMuscleSign = KoLCharacter.inMuscleSign();
			this.mark( 0, 1, inMuscleSign );

			// With all of the data preprocessed, calculate
			// the quantity creatable by solving the set of
			// linear inequalities.

			if ( mixingMethod == ItemCreationRequest.ROLLING_PIN || mixingMethod == ItemCreationRequest.CLOVER )
			{
				// If there's only one ingredient, then the
				// quantity depends entirely on it.

				this.creatable = concoctions[ ingredientArray[0].getItemID() ].initial;
				this.total = this.initial + this.creatable;
			}
			else
			{
				this.total = Integer.MAX_VALUE;
				for ( int i = 0; i < ingredientArray.length; ++i )
					this.total = Math.min( this.total, concoctions[ ingredientArray[i].getItemID() ].quantity( inMuscleSign ) );

				// The total available for other creations is equal
				// to the total, less the initial.

				this.creatable = this.total - this.initial;
			}

			// Now that all the calculations are complete, unmark
			// the ingredients so that later calculations can make
			// the correct calculations.

			this.unmark();
		}

		/**
		 * Utility method which calculates the quantity creatable for
		 * an recipe based on the modifier/multiplier of its
		 * ingredients
		 */

		private int quantity( boolean inMuscleSign )
		{
			// If there is no multiplier, assume that an infinite
			// number is available.

			if ( this.multiplier == 0 )
				return Integer.MAX_VALUE;

			// The maximum value is equivalent to the total, plus
			// the modifier, divided by the multiplier, if the
			// multiplier exists.

			int quantity = (this.total + this.modifier) / this.multiplier;

			// Avoid mutual recursion.

			if ( mixingMethod == ItemCreationRequest.ROLLING_PIN || mixingMethod == ItemCreationRequest.CLOVER )
				return quantity;

			// The true value is affected by the maximum value for
			// the ingredients.  Therefore, calculate the quantity
			// for all other ingredients to complete the solution
			// of the linear inequality.

			for ( int i = 0; i < ingredientArray.length; ++i )
				quantity = Math.min( quantity, concoctions[ ingredientArray[i].getItemID() ].quantity( inMuscleSign ) );

			// Adventures are also considered an ingredient; if
			// no adventures are necessary, the multiplier should
			// be zero and the infinite number available will have
			// no effect on the calculation.

			if ( this != concoctions[0] )
				quantity = Math.min( quantity, concoctions[0].quantity( inMuscleSign ) );

			// In the event that this is item combination and the person
			// is in a non-muscle sign, item creation is impacted by the
			// amount of available meat paste.

			if ( mixingMethod == ItemCreationRequest.COMBINE && !inMuscleSign )
				quantity = Math.min( quantity, concoctions[ ItemCreationRequest.MEAT_PASTE ].quantity( inMuscleSign ) );

			// The true value is now calculated.  Return this
			// value to the requesting method.

			return quantity;
		}

		/**
		 * Utility method which marks the ingredient for usage with
		 * the given added modifier and the given additional multiplier.
		 */

		private void mark( int modifier, int multiplier, boolean inMuscleSign )
		{
			this.modifier += modifier;
			this.multiplier += multiplier;

			// Avoid mutual recursion

			if ( mixingMethod == ItemCreationRequest.ROLLING_PIN || mixingMethod == ItemCreationRequest.CLOVER )
				return;

			// Mark all the ingredients, being sure to multiply
			// by the number of that ingredient needed in this
			// concoction.

			int instanceCount;

			for ( int i = 0; i < ingredientArray.length; ++i )
			{
				// In order to ensure that the multiplier
				// is added correctly, make sure you count
				// the ingredient as many times as it appears,
				// but only multi-count the ingredient once.

				instanceCount = ingredientArray[i].getCount();

				for ( int j = 0; j < i; ++j )
					if ( ingredientArray[i].getItemID() == ingredientArray[j].getItemID() )
						instanceCount += ingredientArray[j].getCount();

				// If the ingredient has already been counted
				// before, continue with the next ingredient.

				if ( instanceCount > ingredientArray[i].getCount() )
					continue;

				// Now that you know that this is the first
				// time the ingredient has been seen, proceed.

				instanceCount = ingredientArray[i].getCount();

				for ( int j = i + 1; j < ingredientArray.length; ++j )
					if ( ingredientArray[i].getItemID() == ingredientArray[j].getItemID() )
						instanceCount += ingredientArray[j].getCount();

				concoctions[ ingredientArray[i].getItemID() ].mark(
					(this.modifier + this.initial) * instanceCount, this.multiplier * instanceCount, inMuscleSign );
			}

			// Mark the implicit adventure ingredient, being
			// sure to multiply by the number of adventures
			// which are required for this mixture.

			if ( this != concoctions[0] && ADVENTURE_USAGE[ mixingMethod ] != 0 )
				concoctions[0].mark( (this.modifier + this.initial) * ADVENTURE_USAGE[ mixingMethod ],
					this.multiplier * ADVENTURE_USAGE[ mixingMethod ], inMuscleSign );

			// In the event that this is a standard combine request,
			// and the person is not in a muscle sign, make sure that
			// meat paste is marked as a limiter also.

			if ( mixingMethod == ItemCreationRequest.COMBINE && !inMuscleSign )
				concoctions[ ItemCreationRequest.MEAT_PASTE ].mark( this.modifier + this.initial, this.multiplier, inMuscleSign );
		}

		/**
		 * Utility method which undoes the yielding process, resetting
		 * the ingredient and current total values to the given number.
		 */

		private void unmark()
		{
			if ( this.modifier == 0 && this.multiplier == 0 )
				return;

			this.modifier = 0;
			this.multiplier = 0;

			for ( int i = 0; i < ingredientArray.length; ++i )
				concoctions[ ingredientArray[i].getItemID() ].unmark();

			if ( this != concoctions[0] )
				concoctions[0].unmark();

			if ( mixingMethod == ItemCreationRequest.COMBINE )
				concoctions[ ItemCreationRequest.MEAT_PASTE ].unmark();
		}

		/**
		 * Helper method to determine whether or not the given mixing
		 * method is permitted, provided the state of the boolean
		 * variables is as specified.
		 */

		private boolean isPermitted()
		{	return PERMIT_METHOD[ mixingMethod ];
		}

		/**
		 * Returns the string form of this concoction.  This is
		 * basically the display name for the item created.
		 */

		public String toString()
		{	return concoction.getName();
		}
	}
}
