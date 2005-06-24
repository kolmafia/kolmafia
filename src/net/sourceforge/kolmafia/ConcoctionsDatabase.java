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
import java.util.Iterator;
import java.util.ArrayList;
import java.io.BufferedReader;
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

	private static Concoction [] concoctions = new Concoction[ ITEM_COUNT ];
	private static boolean INCLUDE_ASCENSION = false;
	private static boolean [] PERMIT_METHOD = new boolean[15];

	private static final int CHEF = 438;
	private static final int BARTENDER = 440;
	private static final AdventureResult HAMMER = new AdventureResult( 338, 1 );
	private static final AdventureResult PLIERS = new AdventureResult( 709, 1 );
	private static final AdventureResult ROLLING_PIN = new AdventureResult( 873, 1 );

	static
	{
		// This begins by opening up the data file and preparing
		// a buffered reader; once this is done, every line is
		// examined and double-referenced: once in the name-lookup,
		// and again in the ID lookup.

		BufferedReader reader = getReader( "concoctions.dat" );

		String [] data;
		int itemID, mixingMethod;
		boolean ascensionRecipe;
		AdventureResult item;

		while ( (data = readData( reader )) != null )
		{
			try
			{
				if ( data.length > 3 )
				{
					item = AdventureResult.parseResult( data[0] );
					itemID = item.getItemID();
					mixingMethod = Integer.parseInt( data[1] );
					ascensionRecipe = ( Integer.parseInt( data[2] ) != 0);

					concoctions[itemID] = new Concoction( item, mixingMethod, ascensionRecipe );

					for ( int i = 3; i < data.length; ++i )
						concoctions[itemID].addIngredient( AdventureResult.parseResult( data[i] ) );
				}
			}
			catch ( Exception e )
			{
				// If an exception is thrown, then something bad
				// happened, so do absolutely nothing.
			}
		}

		for ( int i = 0; i < ITEM_COUNT; ++i )
			if ( concoctions[i] == null )
				concoctions[i] = new Concoction( new AdventureResult( i, 0 ), ItemCreationRequest.NOCREATE, false );
	}

	public static SortedListModel getConcoctions()
	{	return concoctionsList;
	}

	/**
	 * Returns the concoctions which are available given the list of
	 * ingredients.  The list returned contains formal requests for
	 * item creation.
	 *
	 * @param	client	The client to be consulted of the results
	 * @return	A list of possible concoctions
	 */

	public static void refreshConcoctions( KoLmafia client )
	{
		List availableIngredients = new ArrayList();
		availableIngredients.addAll( client.getInventory() );

		if ( client != null )
		{
			if ( client.getSettings().getProperty( "useClosetForCreation" ).equals( "true" ) )
			{
				List closetList = (List) client.getCloset();
				for ( int i = 0; i < closetList.size(); ++i )
					AdventureResult.addResultToList( availableIngredients, (AdventureResult) closetList.get(i) );
			}
		}

		// First, zero out the quantities table.  Though this is not
		// actually necessary, it's a good safety and doesn't use up
		// that much CPU time.

		for ( int i = 1; i < ITEM_COUNT; ++i )
			concoctions[i].resetCalculations();

		// Next, do calculations on all mixing methods which cannot
		// be created.

		for ( int i = 1; i < ITEM_COUNT; ++i )
			if ( concoctions[i].getMixingMethod() == ItemCreationRequest.NOCREATE )
				concoctions[i].calculate( client, availableIngredients );

		// Next, meat paste and meat stacks can be created directly
		// and are dependent upon the amount of meat available.
		// This should also be calculated to allow for meat stack
		// recipes to be calculated.

		int availableMeat = client.getCharacterData().getAvailableMeat();
		if ( client.getSettings().getProperty( "useClosetForCreation" ).equals( "true" ) )
			availableMeat += client.getCharacterData().getClosetMeat();

		concoctions[ ItemCreationRequest.MEAT_PASTE ].total += availableMeat / 10;
		concoctions[ ItemCreationRequest.MEAT_PASTE ].creatable += availableMeat / 10;
		concoctions[ ItemCreationRequest.MEAT_STACK ].total += availableMeat / 100;
		concoctions[ ItemCreationRequest.MEAT_STACK ].creatable += availableMeat / 100;
		concoctions[ ItemCreationRequest.DENSE_STACK ].total += availableMeat / 1000;
		concoctions[ ItemCreationRequest.DENSE_STACK ].creatable += availableMeat / 1000;

		// Next, increment through all of the things which can be
		// created through the use of meat paste.  This allows for box
		// servant creation to be calculated in advance.

		cachePermitted( client );

		for ( int i = 1; i < ITEM_COUNT; ++i )
			if ( concoctions[i].getMixingMethod() == ItemCreationRequest.COMBINE )
				concoctions[i].calculate( client, availableIngredients );

		// Finally, increment through all of the things which are
		// created any other way, making sure that it's a permitted
		// mixture before doing the calculation.

		for ( int i = 1; i < ITEM_COUNT; ++i )
		{
			if ( concoctions[i].concoction.getName() == null )
				continue;
			else if ( concoctions[i].isBadRecipe() )
				client.getLogStream().println( "Bad recipe: " + concoctions[i] );
			else
				concoctions[i].calculate( client, availableIngredients );
		}

		// Finally, prepare the list that will be returned - the list
		// should contained all items whose quantities are greater
		// than zero.

		concoctionsList.clear();

		for ( int i = 1; i < ITEM_COUNT; ++i )
			if ( concoctions[i].creatable > 0 )
				concoctionsList.add( ItemCreationRequest.getInstance( client, i, concoctions[i].creatable ) );
	}

	/**
	 * Helper method to determine whether or not the given mixing
	 * method is permitted, provided the state of the boolean
	 * variables is as specified.
	 */

	private static boolean isPermitted( Concoction concoction, KoLmafia client )
	{
		return (concoction.isAscensionRecipe ? INCLUDE_ASCENSION : true) && PERMIT_METHOD[ concoction.mixingMethod ];
	}

	private static void cachePermitted( KoLmafia client )
	{
		INCLUDE_ASCENSION =	client.getSettings().getProperty( "includeAscensionRecipes" ).equals( "true" );

		for ( int i = 0; i < PERMIT_METHOD.length; ++i )
			PERMIT_METHOD[i] = true;

		KoLCharacter data = client.getCharacterData();

		PERMIT_METHOD[ ItemCreationRequest.NOCREATE ] = false;
		PERMIT_METHOD[ ItemCreationRequest.COOK ] = isAvailable( CHEF, client );
		PERMIT_METHOD[ ItemCreationRequest.MIX ] = isAvailable( BARTENDER, client );
		PERMIT_METHOD[ ItemCreationRequest.COOK_REAGENT ] = isAvailable( CHEF, client ) && data.canSummonReagent();
		PERMIT_METHOD[ ItemCreationRequest.COOK_PASTA ] = isAvailable( CHEF, client ) && data.canSummonNoodles();
		PERMIT_METHOD[ ItemCreationRequest.MIX_SPECIAL ] = isAvailable( BARTENDER, client ) && data.canSummonShore();
		PERMIT_METHOD[ ItemCreationRequest.SMITH ] = data.getInventory().contains( HAMMER );
		PERMIT_METHOD[ ItemCreationRequest.SMITH_WEAPON ] = data.getInventory().contains( HAMMER ) && data.canSmithWeapons();
		PERMIT_METHOD[ ItemCreationRequest.SMITH_ARMOR ] = data.getInventory().contains( HAMMER ) && data.canSmithArmor();
		PERMIT_METHOD[ ItemCreationRequest.JEWELRY ] = data.getInventory().contains( PLIERS );
		PERMIT_METHOD[ ItemCreationRequest.ROLLING_PIN ] = data.getInventory().contains( ROLLING_PIN );
	}

	private static boolean isAvailable( int servantID, KoLmafia client )
	{
		KoLCharacter data = client.getCharacterData();

		// If it's a base case, return whether or not the
		// servant is already available at the camp.

		if ( servantID == CHEF && data.hasChef() )
			return true;
		if ( servantID == BARTENDER && data.hasBartender() )
			return true;

		// If the user did not wish to repair their boxes
		// on explosion, then the box servant is not available

		if ( client.getSettings().getProperty( "autoRepairBoxes" ).equals( "false" ) )
			return false;

		// Otherwise, return whether or not the quantity possible
		// for the given chefs is non-zero.  This works because
		// cooking tests are made after item creation tests.

		return concoctions[ servantID ].total != 0;
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
		private boolean isAscensionRecipe;

		private List ingredients;
		private AdventureResult [] ingredientArray;

		private int modifier, multiplier;
		private int initial, creatable, total;

		public Concoction( AdventureResult concoction, int mixingMethod, boolean isAscensionRecipe )
		{
			this.concoction = concoction;
			this.mixingMethod = mixingMethod;
			this.isAscensionRecipe = isAscensionRecipe;

			this.ingredients = new ArrayList();
			this.ingredientArray = new AdventureResult[0];
		}

		public void resetCalculations()
		{
			this.initial = -1;
			this.creatable = -1;
			this.total = -1;

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
			if ( concoction.getName() == null )
				return true;

			for ( int i = 0; i < ingredientArray.length; ++i )
				if ( ingredientArray[i].getItemID() == -1 )
					return true;

			return false;
		}

		public AdventureResult [] getIngredients()
		{	return ingredientArray;
		}

		public void calculate( KoLmafia client, List availableIngredients )
		{
			// If the item doesn't exist in the item table,
			// then assume it can't be created.

			if ( concoction.getName() == null )
			{
				this.total = 0;
				return;
			}

			// If a calculation has already been done for this
			// concoction, no need to calculate again.

			if ( this.initial != -1 )
				return;

			// Determine how many were available initially in the
			// available ingredient list.

			this.initial = concoction.getCount( availableIngredients );
			this.total = initial;

			if ( this.mixingMethod == ItemCreationRequest.NOCREATE || !isPermitted( this, client ) )
				return;

			// First, preprocess the ingredients by calculating
			// how many of each ingredient is possible now.

			for ( int i = 0; i < ingredientArray.length; ++i )
				concoctions[ ingredientArray[i].getItemID() ].calculate( client, availableIngredients );

			// Next, preprocess the ingredients again by marking
			// them with the equation variables.  This can be
			// done by marking this item with a zero modifier
			// and a multiplier of one.

			this.mark( 0, 1 );

			// With all of the data preprocessed, calculate
			// the quantity creatable by solving the set of
			// linear inequalities.

			int itemID, quantity;
			this.creatable = Integer.MAX_VALUE;

			for ( int i = 0; i < ingredientArray.length; ++i )
			{
				itemID = ingredientArray[i].getItemID();

				// Next, calculate the quantity.  This value is
				// equivalent to the total, plus the modifier,
				// divided by the multiplier.

				quantity = (concoctions[itemID].total + concoctions[itemID].modifier) / concoctions[itemID].multiplier;
				this.creatable = Math.min( this.creatable, quantity - this.initial );
			}

			// Now that all the calculations are complete, unmark
			// the ingredients so that later calculations can make
			// the correct calculations.

			this.unmark();

			// The total available for other creations is
			// equivalent to the initial number plus the number
			// which can be created.

			this.total = this.initial + this.creatable;
		}

		/**
		 * Utility method which marks the ingredient for usage with
		 * the given added modifier and the given additional multiplier.
		 */

		private void mark( int modifier, int multiplier )
		{
			this.modifier += modifier;
			this.multiplier += multiplier;

			for ( int i = 0; i < ingredientArray.length; ++i )
				concoctions[ ingredientArray[i].getItemID() ].mark( (this.modifier + this.initial) * ingredientArray[i].getCount(),
					this.multiplier * ingredientArray[i].getCount() );
		}

		/**
		 * Utility method which undoes the yielding process, resetting
		 * the ingredient and current total values to the given number.
		 */

		private void unmark()
		{
			for ( int i = 0; i < ingredientArray.length; ++i )
				concoctions[ ingredientArray[i].getItemID() ].unmark();

			this.modifier = 0;
			this.multiplier = 0;
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
