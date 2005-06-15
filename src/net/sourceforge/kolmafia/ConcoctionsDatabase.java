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

import java.io.File;
import java.io.IOException;
import java.io.BufferedReader;

import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.StringTokenizer;

import net.java.dev.spellcast.utilities.DataUtilities;
import net.java.dev.spellcast.utilities.SortedListModel;

/**
 * A static class which retrieves all the concoctions available in
 * the Kingdom of Loathing.  This class technically uses up a lot
 * more memory than it needs to because it creates an array storing
 * all possible item combinations, but that's okay!  Because it's
 * only temporary.  Then again, this is supposedly true of all the
 * flow-control using exceptions, but that hasn't been changed.
 */

public class ConcoctionsDatabase
{
	protected static final SortedListModel concoctionsList = new SortedListModel();

	private static final String ITEM_DBASE_FILE = "concoctions.dat";
	public static final int ITEM_COUNT = TradeableItemDatabase.ITEM_COUNT;

	private static Concoction [] concoctions = new Concoction[ ITEM_COUNT ];
	private static int [] quantityPossible = new int[ ITEM_COUNT ];

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

		BufferedReader itemdata = DataUtilities.getReaderForSharedDataFile( ITEM_DBASE_FILE );

		try
		{
			String line;
			AdventureResult item;
			int mixingMethod;

			StringTokenizer strtok;

			while ( (line = itemdata.readLine()) != null )
			{
                                // Skip blank lines and comments
                                if (line.length() < 1 || line.charAt(0) == '#')
                                        continue;

				strtok = new StringTokenizer( line, "\t" );
				if ( strtok.countTokens() > 2 )
				{
					item = AdventureResult.parseResult( strtok.nextToken() );
					mixingMethod = Integer.parseInt( strtok.nextToken() );

					concoctions[ item.getItemID() ] = new Concoction( item, mixingMethod );

					while ( strtok.hasMoreTokens() )
						concoctions[ item.getItemID() ].addIngredient( AdventureResult.parseResult( strtok.nextToken() ) );
				}
			}
		}
		catch ( Exception e )
		{
			// If an IOException is thrown, that means there was
			// a problem reading in the appropriate data file;
			// that means that no item database exists.  This
			// exception is strange enough that it won't be
			// handled at the current time.
		}
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
	 * @param	availableIngredients	The list of ingredients available for mixing
	 * @return	A list of possible concoctions
	 */

	public static void refreshConcoctions( KoLmafia client )
	{
		List availableIngredients = new ArrayList();
		availableIngredients.addAll( client.getInventory() );

		if ( client != null )
		{
			String useClosetForCreationSetting = client.getSettings().getProperty( "useClosetForCreation" );

			if ( useClosetForCreationSetting != null && useClosetForCreationSetting.equals( "true" ) )
			{
				List closetList = (List) client.getCloset();
				for ( int i = 0; i < closetList.size(); ++i )
					AdventureResult.addResultToList( availableIngredients, (AdventureResult) closetList.get(i) );
			}
		}

		// First, zero out the quantities table.  Though this is not
		// actually necessary, it's a good safety and doesn't use up
		// that much CPU time.

		for ( int i = 0; i < ITEM_COUNT; ++i )
			quantityPossible[i] = -1;

		// First, you look for all items which cannot be created through
		// any creation method, and initialize their quantities.  This
		// way, the data doesn't interfere with the dynamic programming
		// algorithms used later.

		for ( int i = 0; i < ITEM_COUNT; ++i )
		{
			if ( concoctions[i] == null || concoctions[i].getMixingMethod() == ItemCreationRequest.NOCREATE )
			{
				String itemName = TradeableItemDatabase.getItemName(i);
				if ( itemName != null )
					quantityPossible[i] = (new AdventureResult( i, 0 )).getCount( availableIngredients );
				else
					quantityPossible[i] = 0;
			}
			else
				quantityPossible[i] = -1;
		}

		// Next, meat paste and meat stacks can be created directly
		// and are dependent upon the amount of meat available.
		// This should also be calculated to allow for meat stack
		// recipes to be calculated.

		int availableMeat = client.getCharacterData().getAvailableMeat();
		String useClosetForCreationSetting = client.getSettings().getProperty( "useClosetForCreation" );
		if ( useClosetForCreationSetting != null && useClosetForCreationSetting.equals( "true" ) )
			availableMeat += client.getCharacterData().getClosetMeat();

		quantityPossible[ ItemCreationRequest.MEAT_PASTE ] += availableMeat / 10;
		quantityPossible[ ItemCreationRequest.MEAT_STACK ] += availableMeat / 100;
		quantityPossible[ ItemCreationRequest.DENSE_STACK ] += availableMeat / 1000;

		// Next, increment through all of the things which can be
		// created through the use of meat paste.  This allows for box
		// servant creation to be calculated in advance.

		for ( int i = 0; i < ITEM_COUNT; ++i )
			if ( concoctions[i] != null && concoctions[i].getMixingMethod() == ItemCreationRequest.COMBINE )
				concoctions[i].calculateQuantityPossible( availableIngredients );

		// Finally, increment through all of the things which are created
		// any other way, making sure that it's a permitted mixture
		// before doing the calculation.

		for ( int i = 0; i < ITEM_COUNT; ++i )
		{
			if ( concoctions[i] != null && concoctions[i].getMixingMethod() != ItemCreationRequest.NOCREATE && concoctions[i].getMixingMethod() != ItemCreationRequest.COMBINE )
			{
				if ( !isPermitted( concoctions[i].getMixingMethod(), client ) )
				{
					String itemName = TradeableItemDatabase.getItemName(i);
					if ( itemName != null )
						quantityPossible[i] = (new AdventureResult( i, 0 )).getCount( availableIngredients );
					else
						quantityPossible[i] = 0;
				}
				else
				{
					if ( concoctions[i].isBadRecipe() )
						client.getLogStream().println( "Bad recipe: " + concoctions[i] );
					else
						concoctions[i].calculateQuantityPossible( availableIngredients );
				}
			}
		}

		// Finally, remove the number you have now from how many can be
		// made available - this means that the list will reflect only
		// items that are genuinely created.

		for ( int i = 0; i < availableIngredients.size(); ++i )
		{
			AdventureResult result = (AdventureResult) availableIngredients.get(i);

			if ( result.isItem() )
				quantityPossible[ result.getItemID() ] -= result.getCount();
		}

		// Finally, prepare the list that will be returned - the list
		// should contained all items whose quantities are greater
		// than zero.

		concoctionsList.clear();

		for ( int i = 0; i < ITEM_COUNT; ++i )
			if ( quantityPossible[i] > 0 )
				concoctionsList.add( ItemCreationRequest.getInstance( client, i, quantityPossible[i] ) );
	}

	/**
	 * Helper method to determine whether or not the given mixing
	 * method is permitted, provided the state of the boolean
	 * variables is as specified.
	 */

	private static boolean isPermitted( int mixingMethod, KoLmafia client )
	{
		KoLCharacter data = client.getCharacterData();
		String classtype = data.getClassType();

		switch ( mixingMethod )
		{
			case ItemCreationRequest.COOK:
				return isAvailable( CHEF, client );

			case ItemCreationRequest.MIX:
				return isAvailable( BARTENDER, client );

			case ItemCreationRequest.COOK_REAGENT:
				return isAvailable( CHEF, client ) && data.canSummonReagent();

			case ItemCreationRequest.COOK_PASTA:
				return isAvailable( CHEF, client ) && data.canSummonNoodles();

			case ItemCreationRequest.MIX_SPECIAL:
				return isAvailable( BARTENDER, client ) && data.canSummonShore();

			case ItemCreationRequest.SMITH:
				return data.getInventory().contains( HAMMER );

			case ItemCreationRequest.JEWELRY:
				return data.getInventory().contains( PLIERS );

			case ItemCreationRequest.ROLLING_PIN:
				return data.getInventory().contains( ROLLING_PIN );

			default:
				return true;
		}
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

		String autoRepairBoxesSetting = client.getSettings().getProperty( "autoRepairBoxes" );
		if ( autoRepairBoxesSetting == null || autoRepairBoxesSetting.equals( "false" ) )
			return false;

		// Otherwise, return whether or not the quantity possible
		// for the given chefs is non-zero.  This works because
		// cooking tests are made after item creation tests.

		return quantityPossible[ servantID ] != 0;
	}

	/**
	 * Returns the mixing method for the item with the given ID.
	 */

	public static int getMixingMethod( int itemID )
	{	return concoctions[itemID] == null ? ItemCreationRequest.NOCREATE : concoctions[itemID].getMixingMethod();
	}

	/**
	 * Returns the item IDs of the ingredients for the given item.
	 * Note that if there are no ingredients, then <code>null</code>
	 * will be returned instead.
	 */

	public static AdventureResult [] getIngredients( int itemID )
	{	return (concoctions[ itemID ] == null) ? null : concoctions[ itemID ].getIngredients();
	}

	/**
	 * Internal class used to represent a single concoction.  It
	 * contains all the information needed to actually make the item.
	 */

	private static class Concoction
	{
		private int mixingMethod;
		private AdventureResult concoction;
		private List ingredients;

		public Concoction( AdventureResult concoction, int mixingMethod )
		{
			this.concoction = concoction;
			this.mixingMethod = mixingMethod;
			this.ingredients = new ArrayList();
		}

		public void addIngredient( AdventureResult ingredient )
		{	ingredients.add( ingredient );
		}

		public int getMixingMethod()
		{	return mixingMethod;
		}

		public boolean isBadRecipe()
		{
			Iterator iterator = ingredients.iterator();
			if ( ((AdventureResult)iterator.next()).getItemID() == -1 )
				return true;

			return false;
		}

		public AdventureResult [] getIngredients()
		{
			AdventureResult [] ingredientArray = new AdventureResult[ ingredients.size() ];
			ingredients.toArray( ingredientArray );
			return ingredientArray;
		}

		public void calculateQuantityPossible( List availableIngredients )
		{
			// If a calculation has already been done for this
			// concoction, simply return.

			if ( quantityPossible[ concoction.getItemID() ] != -1 )
				return;

			// Convert the list of items to an array in order to
			// make things easier to work with.

			AdventureResult [] ingredientArray = getIngredients();

			// Determine how many were available initially in the
			// available ingredient list.

			quantityPossible[ concoction.getItemID() ] = concoction.getCount( availableIngredients );

			// Calculate how many of each ingredient can be created
			// at each step.

			for ( int i = 0; i < ingredientArray.length; ++i )
				if ( concoctions[ ingredientArray[i].getItemID() ] != null )
					concoctions[ ingredientArray[i].getItemID() ].calculateQuantityPossible( availableIngredients );

			int additionalPossible = Integer.MAX_VALUE;
			int itemID, quantity, divisor;

			for ( int i = 0; i < ingredientArray.length; ++i )
			{
				itemID = ingredientArray[i].getItemID();
				quantity = quantityPossible[ itemID ];
				divisor = ingredientArray[i].getCount();

				for ( int j = 0; j < ingredientArray.length; ++j )
					if ( i != j && itemID == ingredientArray[j].getItemID() )
						divisor += ingredientArray[i].getCount();

				additionalPossible = Math.min ( additionalPossible, quantity / divisor );
			}

			// Now, factor in the possibility that the same
			// ingredient may be used twice in the same concoction

			quantityPossible[ concoction.getItemID() ] += additionalPossible;
		}

		public String toString()
		{	return concoction.toString();
		}
	}
}
