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
import net.java.dev.spellcast.utilities.LockableListModel;

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
	private static final int ITEM_COUNT = 1000;

	private static Concoction [] concoctions = new Concoction[ ITEM_COUNT ];

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

					concoctions[ itemID ] = new Concoction( Integer.parseInt( strtok.nextToken() ),
						strtok.nextToken(), strtok.nextToken() );
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

	public static LockableListModel getConcoctions( KoLmafia client, List availableIngredients )
	{
		LockableListModel concoctionsList = new LockableListModel();

		for ( int i = 0; i < concoctions.length; ++i )
		{
			if ( concoctions[i] != null )
			{
				int quantityPossible = concoctions[i].getQuantityPossible( availableIngredients );
				if ( quantityPossible > 0 )
					concoctionsList.add( new ItemCreationRequest( client, i, concoctions[i].getMixingMethod(), quantityPossible ) );
			}
		}

		return concoctionsList;
	}

	private static class Concoction
	{
		private int mixingMethod;
		private List ingredients;
		private int [] ingredientIDs;

		public Concoction( int mixingMethod, String ingredient1, String ingredient2 )
		{
			this.mixingMethod = mixingMethod;

			ingredients = new ArrayList();
			ingredients.add( new AdventureResult( ingredient1, 0 ) );
			ingredients.add( new AdventureResult( ingredient2, 0 ) );

			ingredientIDs = new int[2];
			ingredientIDs[0] = TradeableItemDatabase.getItemID( ingredient1 );
			ingredientIDs[1] = TradeableItemDatabase.getItemID( ingredient2 );
		}

		public int getMixingMethod()
		{	return mixingMethod;
		}

		public int getQuantityPossible( List availableIngredients )
		{
			return Math.min(
				getQuantityPossible( availableIngredients,
					(AdventureResult) ingredients.get(0), ingredientIDs[0] ),
				getQuantityPossible( availableIngredients,
					(AdventureResult) ingredients.get(1), ingredientIDs[1] ) );
		}

		private int getQuantityPossible( List availableIngredients, AdventureResult ingredient, int ingredientID )
		{
			int index = availableIngredients.indexOf( ingredient );
			int quantity = (index == -1) ? 0 :
				((AdventureResult)availableIngredients.get( index )).getCount();

			if ( concoctions[ ingredientID ] != null )
				quantity += concoctions[ ingredientID ].getQuantityPossible( availableIngredients );

			return quantity;
		}
	}
}