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
import java.util.ArrayList;
import java.util.StringTokenizer;

import net.java.dev.spellcast.utilities.DataUtilities;
import net.java.dev.spellcast.utilities.UtilityConstants;
import net.java.dev.spellcast.utilities.SortedListModel;

/**
 * A static class which retrieves all the concoctions available in
 * the Kingdom of Loathing.  This class technically uses up a lot
 * more memory than it needs to because it creates an array storing
 * all possible item combinations, but that's okay!  Because it's
 * only temporary.  Then again, this is supposedly true of all the
 * flow-control using exceptions, but that hasn't been changed.
 */

public class ConcoctionsDatabase implements UtilityConstants
{
	private static final String ITEM_DBASE_FILE = "concoctions.dat";
	public static final int ITEM_COUNT = TradeableItemDatabase.ITEM_COUNT;

	private static Concoction [] concoctions = new Concoction[ ITEM_COUNT ];
	private static int [] quantityPossible = new int[ ITEM_COUNT ];

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
			while ( (line = itemdata.readLine()) != null )
			{
				StringTokenizer strtok = new StringTokenizer( line, "\t" );
				if ( strtok.countTokens() == 4 )
				{
					int itemID = TradeableItemDatabase.getItemID( strtok.nextToken() );
					concoctions[ itemID ] = new Concoction( itemID, Integer.parseInt( strtok.nextToken() ), strtok.nextToken(), strtok.nextToken() );
				}
			}
		}
		catch ( IOException e )
		{
			// If an IOException is thrown, that means there was
			// a problem reading in the appropriate data file;
			// that means that no item database exists.  This
			// exception is strange enough that it won't be
			// handled at the current time.
		}
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

	public static SortedListModel getConcoctions( KoLmafia client, List availableIngredients )
	{
		// First, you look for all items which cannot be created through
		// any creation method, and initialize their quantities.  This
		// way, the data doesn't interfere with the dynamic programming
		// algorithms used later.

		for ( int i = 0; i < ITEM_COUNT; ++i )
		{
			if ( concoctions[i] == null || !isPermittedMixtureMethod( concoctions[i].getMixingMethod(), client.getCharacterData() ) )
			{
				String itemName = TradeableItemDatabase.getItemName(i);
				if ( itemName != null )
				{
					int index = availableIngredients.indexOf( new AdventureResult( itemName, 0 ) );
					quantityPossible[i] = (index == -1) ? 0 : ((AdventureResult)availableIngredients.get( index )).getCount();
				}
				else
					quantityPossible[i] = 0;
			}
			else
				quantityPossible[i] = -1;
		}

		// Next, determine how many of each item can be created through
		// the available ingredients using dynamic programming.

		for ( int i = 0; i < ITEM_COUNT; ++i )
			if ( concoctions[i] != null )
				concoctions[i].calculateQuantityPossible( availableIngredients );

		// Finally, remove the number you have now from how many can be
		// made available - this means that the list will reflect only
		// items that are genuinely created.

		for ( int i = 0; i < availableIngredients.size(); ++i )
		{
			AdventureResult result = (AdventureResult) availableIngredients.get(i);

			if ( result.isItem() && TradeableItemDatabase.contains( result.getName() ) )
				quantityPossible[ TradeableItemDatabase.getItemID( result.getName() ) ] -= result.getCount();
		}

		// Finally, prepare the list that will be returned - the list
		// should contained all items whose quantities are greater
		// than zero.

		SortedListModel concoctionsList = new SortedListModel();

		for ( int i = 0; i < ITEM_COUNT; ++i )
			if ( quantityPossible[i] > 0 )
				concoctionsList.add( new ItemCreationRequest( client, i, concoctions[i].getMixingMethod(), quantityPossible[i] ) );

		return concoctionsList;
	}

	/**
	 * Helper method to determine whether or not the given mixing
	 * method is permitted, provided the state of the boolean
	 * variables is as specified.
	 */

	private static boolean isPermittedMixtureMethod( int mixingMethod, KoLCharacter data )
	{
		String classtype = data.getClassType();

		switch ( mixingMethod )
		{
			case ItemCreationRequest.COOK:
				return data.hasChef();

			case ItemCreationRequest.MIX:
				return data.hasBartender();

			case ItemCreationRequest.SMITH:
				return data.hasChef() && classtype.startsWith( "Se" );

			case ItemCreationRequest.COOK_REAGENT:
				return data.hasChef() && classtype.startsWith( "Sa" );

			case ItemCreationRequest.COOK_PASTA:
				return data.hasChef() && classtype.startsWith( "Pa" );

			case ItemCreationRequest.MIX_SPECIAL:
				return data.hasBartender() && classtype.startsWith( "Di" );

			default:
				return true;
		}
	}

	/**
	 * Returns the item IDs of the ingredients for the given item.
	 * Note that if there are no ingredients, then <code>null</code>
	 * will be returned instead.
	 */

	public static int [][] getIngredients( int itemID )
	{	return (concoctions[ itemID ] == null) ? null : concoctions[ itemID ].getIngredients();
	}

	/**
	 * Internal class used to represent a single concoction.  It
	 * contains all the information needed to actually make the item.
	 */

	private static class Concoction
	{
		private int concoctionID;
		private int mixingMethod;
		private AdventureResult asResult;
		private int ingredient1, ingredient2;

		public Concoction( int concoctionID, int mixingMethod, String ingredient1, String ingredient2 )
		{
			this.concoctionID = concoctionID;
			this.mixingMethod = mixingMethod;

			this.asResult = new AdventureResult( TradeableItemDatabase.getItemName( concoctionID ), 0 );

			this.ingredient1 = TradeableItemDatabase.getItemID( ingredient1 );
			this.ingredient2 = TradeableItemDatabase.getItemID( ingredient2 );
		}

		public int getMixingMethod()
		{	return mixingMethod;
		}

		public int [][] getIngredients()
		{
			int [][] ingredients = new int[2][2];

			ingredients[0][0] = ingredient1;
			ingredients[0][1] = (concoctions[ingredient1] == null) ?
				ItemCreationRequest.NOCREATE : concoctions[ingredient1].getMixingMethod();

			ingredients[1][0] = ingredient2;
			ingredients[1][1] = (concoctions[ingredient2] == null) ?
				ItemCreationRequest.NOCREATE : concoctions[ingredient2].getMixingMethod();

			return ingredients;
		}

		public void calculateQuantityPossible( List availableIngredients )
		{
			if ( quantityPossible[ concoctionID ] != -1 )
				return;

			int index = availableIngredients.indexOf( asResult );
			quantityPossible[ concoctionID ] = (index == -1) ? 0 : ((AdventureResult)availableIngredients.get( index )).getCount();

			if ( concoctions[ ingredient1 ] != null )
				concoctions[ ingredient1 ].calculateQuantityPossible( availableIngredients );

			if ( concoctions[ ingredient2 ] != null )
				concoctions[ ingredient2 ].calculateQuantityPossible( availableIngredients );

			int additionalPossible = Math.min( quantityPossible[ ingredient1 ], quantityPossible[ ingredient2 ] );
			if ( ingredient1 == ingredient2 )
				additionalPossible >>= 1;

			quantityPossible[ concoctionID ] += additionalPossible;
		}
	}
}