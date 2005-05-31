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
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * An extension of <code>KoLRequest</code> designed to handle all the
 * item creation requests.  At the current time, it is only made to
 * handle items which use meat paste and are tradeable in-game.
 */

public class ItemCreationRequest extends KoLRequest implements Comparable
{
	public static final int MEAT_PASTE = 25;
	public static final int MEAT_STACK = 88;
	public static final int DENSE_STACK = 258;

	public static final int NOCREATE = 0;
	public static final int COMBINE = 1;
	public static final int COOK = 2;
	public static final int MIX = 3;
	public static final int SMITH = 4;
	public static final int COOK_REAGENT = 5;
	public static final int COOK_PASTA = 6;
	public static final int MIX_SPECIAL = 7;

	public static final int ROLLING_PIN = 11;

	private static final AdventureResult CHEF = new AdventureResult( 438, 1 );
	private static final AdventureResult BARTENDER = new AdventureResult( 440, 1 );

	private int itemID, quantityNeeded, mixingMethod;

	/**
	 * Constructs a new <code>ItemCreationRequest</code> where you create
	 * the given number of items.
	 *
	 * @param	client	The client to be notified of the item creation
	 * @param	itemID	The identifier for the item to be created
	 * @param	mixingMethod	How the item is created
	 * @param	quantityNeeded	How many of this item are needed
	 */

	public ItemCreationRequest( KoLmafia client, int itemID, int mixingMethod, int quantityNeeded )
	{
		super( client, mixingMethod == COMBINE ? "combine.php" :
			(mixingMethod == MIX || mixingMethod == MIX_SPECIAL) ? "cocktail.php" :
			(mixingMethod == COOK || mixingMethod == COOK_REAGENT || mixingMethod == COOK_PASTA) ? "cook.php" :
			mixingMethod == SMITH ? "smith.php" : "" );

		addFormField( "action", "combine" );
		addFormField( "pwd", client.getPasswordHash() );

		this.itemID = itemID;
		this.mixingMethod = mixingMethod;
		this.quantityNeeded = quantityNeeded;
	}

	public boolean equals( Object o )
	{	return o != null && o instanceof ItemCreationRequest && itemID == ((ItemCreationRequest)o).itemID;
	}

	public int compareTo( Object o )
	{	return o == null ? -1 : this.toString().compareToIgnoreCase( o.toString() );
	}

	/**
	 * Runs the item creation request.  Note that if another item needs
	 * to be created for the request to succeed, this method will fail.
	 */

	public void run()
	{
		if ( !client.permitsContinue() || quantityNeeded <= 0 )
			return;

		switch ( itemID )
		{
			// Requests for meat paste are handled separately; the
			// full request is broken into increments of 1000, 100
			// and 10 and then submitted to the server.

			case MEAT_PASTE:
			case MEAT_STACK:
			case DENSE_STACK:
			{
				(new CombineMeatRequest( client, itemID, quantityNeeded )).run();
				break;
			}

			// In order to make indentation cleaner, an internal class
			// a secondary method is called to handle standard item
			// creation requests.  Note that smithing is not currently
			// handled because I don't have a SC and have no idea
			// which page is requested for smithing.

			default:

				switch ( mixingMethod )
				{
					case COMBINE:
					case COOK:
					case MIX:
					case SMITH:
					case COOK_REAGENT:
					case COOK_PASTA:
					case MIX_SPECIAL:
						combineItems();
						break;
					case ROLLING_PIN:
						updateDisplay( DISABLED_STATE, "Using a rolling pin..." );
						(new ConsumeItemRequest( client, new AdventureResult( 873, 1 ))).run();
						updateDisplay( ENABLED_STATE, "Flat dough created." );
						break;
				}

				break;
		}
	}

	/**
	 * Helper routine which actually does the item combination.
	 */

	private void combineItems()
	{
		int [][] ingredients = ConcoctionsDatabase.getIngredients( itemID );

		if ( ingredients != null )
		{
			makeIngredient( ingredients[0][0], ingredients[0][1], ingredients[0][0] == ingredients[1][0] );
			if ( ingredients[0][0] != ingredients[1][0] )
				makeIngredient( ingredients[1][0], ingredients[1][1], false );
		}

		// Check to see if you need meat paste in order
		// to create the needed quantity of items, and
		// create any needed meat paste.

		if ( mixingMethod == COMBINE )
			makeIngredient( MEAT_PASTE, COMBINE, false );

		// Now that the item's been created, you can
		// actually do the request!

		addFormField( "item1", String.valueOf( ingredients[0][0] ) );
		addFormField( "item2", String.valueOf( ingredients[1][0] ) );
		addFormField( "quantity", String.valueOf( quantityNeeded ) );

		// Auto-create chef or bartender if one doesn't
		// exist and the user has opted to repair.

		if ( !autoRepairBoxServant() )
		{
			updateDisplay( ERROR_STATE, "Box servant explosion!" );
			client.cancelRequest();
			return;
		}

		// If the request has been cancelled midway, be
		// sure to return from here.

		if ( !client.permitsContinue() )
			return;

		updateDisplay( DISABLED_STATE, "Creating " + toString() + "..." );

		super.run();

		// If an error state occurred, return from this
		// request, since there's no content to parse

		if ( isErrorState || responseCode != 200 )
			return;

		// Check to make sure that the item creation
		// did not fail.

		if ( responseText.indexOf( "You don't have enough" ) != -1 )
		{
			client.cancelRequest();
			updateDisplay( ERROR_STATE, "You're missing ingredients." );
			return;
		}

		// Arbitrary results can happen - just throw the
		// entire string to the results parser and let
		// it figure out what was actually gained; note
		// that this is potentially inaccurate, as you
		// may get your initial creation attempt back.

		processResults( responseText );

		String itemName = TradeableItemDatabase.getItemName( itemID );

		Matcher resultMatcher = Pattern.compile(
			"You acquire some items\\: <b>" + itemName + " \\(([\\d,]+)\\)</b>" ).matcher( responseText );

		int createdQuantity = 0;

		try
		{
			if ( resultMatcher.find() )
				createdQuantity = df.parse( resultMatcher.group(1) ).intValue();
			else
			{
				resultMatcher = Pattern.compile( "You acquire an item\\: <b>" + itemName + "</b>" ).matcher( responseText );
				if ( resultMatcher.find() )
					createdQuantity = 1;
			}
		}
		catch ( Exception e )
		{
		}

		// Because an explosion might have occurred, the
		// quantity that has changed might not be accurate.
		// Therefore, update with the actual value.

		client.processResult( new AdventureResult( ingredients[0][0], 0 - createdQuantity ) );
		client.processResult( new AdventureResult( ingredients[1][0], 0 - createdQuantity ) );

		if ( mixingMethod == COMBINE )
			client.processResult( new AdventureResult( "meat paste", 0 - createdQuantity ) );

		// Now, check to see if your box-servant was
		// overworked and exploded.  Also handle the
		// possibility of smithing reducing adventures.

		switch ( mixingMethod )
		{
			case SMITH:
				client.processResult( new AdventureResult( AdventureResult.ADV, 0 - createdQuantity ) );
				break;

			case COOK:
			case COOK_REAGENT:
			case COOK_PASTA:

				if ( responseText.indexOf( "Smoke" ) != -1 )
				{
					client.getCharacterData().setChef( false );
					(new ItemCreationRequest( client, itemID, mixingMethod, quantityNeeded - createdQuantity )).run();
					return;
				}
				else if ( client.permitsContinue() )
				{
					updateDisplay( NOCHANGE, "Successfully cooked " + quantityNeeded + " " + itemName );
					return;
				}

				break;

			case MIX:
			case MIX_SPECIAL:
				if ( responseText.indexOf( "Smoke" ) != -1 )
				{
					client.getCharacterData().setBartender( false );
					(new ItemCreationRequest( client, itemID, mixingMethod, quantityNeeded - createdQuantity )).run();
					return;
				}
				else if ( client.permitsContinue() )
				{
					updateDisplay( NOCHANGE, "Successfully mixed " + quantityNeeded + " " + itemName );
					return;
				}

				break;

			default:
				if ( client.permitsContinue() )
				{
					updateDisplay( NOCHANGE,  "Successfully created " + quantityNeeded + " " + itemName );
					return;
				}
		}
	}

	private boolean autoRepairBoxServant()
	{
		switch ( mixingMethod )
		{
			case COOK:
			case COOK_REAGENT:
			case COOK_PASTA:

				if ( client.getCharacterData().hasChef() )
					return true;
				break;

			case MIX:
			case MIX_SPECIAL:

				if ( client.getCharacterData().hasBartender() )
					return true;

			default:
				return true;
		}

		// Check to see if the player wants to autorepair

		String autoRepairBoxesSetting = client.getSettings().getProperty( "autoRepairBoxes" );
		if ( autoRepairBoxesSetting == null || autoRepairBoxesSetting.equals( "false" ) )
			return false;

		// If they do want to auto-repair, make sure that
		// the appropriate item is available in their inventory

		switch ( mixingMethod )
		{
			case COOK:
			case COOK_REAGENT:
			case COOK_PASTA:

				return useBoxServant( CHEF );

			case MIX:
			case MIX_SPECIAL:

				return useBoxServant( BARTENDER );
		}

		return false;
	}

	private boolean useBoxServant( AdventureResult toUse )
	{
		// Just in case, determine which items can be
		// created, to see if a box can be created
		// from the necessary materials.

		List concoctions = new ArrayList();
		List materialsList = (List) client.getInventory().clone();
		String useClosetForCreationSetting = client.getSettings().getProperty( "useClosetForCreation" );

		if ( useClosetForCreationSetting != null && useClosetForCreationSetting.equals( "true" ) )
		{
			List closetList = (List) client.getCloset();
			for ( int i = 0; i < closetList.size(); ++i )
				AdventureResult.addResultToList( materialsList, (AdventureResult) closetList.get(i) );
		}

		concoctions.addAll( ConcoctionsDatabase.getConcoctions( client, materialsList ) );
		ItemCreationRequest boxServantCreationRequest = new ItemCreationRequest( client, toUse.getItemID(), COMBINE, 1 );
		boolean canCreateBoxServant = concoctions.contains( boxServantCreationRequest );

		if ( !client.getInventory().contains( toUse ) )
		{
			if ( useClosetForCreationSetting == null || useClosetForCreationSetting.equals( "false" ) || !client.getCloset().contains( toUse ) )
			{
				if ( canCreateBoxServant )
					boxServantCreationRequest.run();
				else
				{
					updateDisplay( ERROR_STATE, "Could not auto-repair " + toUse.getName() + "." );
					return false;
				}
			}
			else
			{
				updateDisplay( DISABLED_STATE, "Retrieving " + toUse.getName() + " from closet..." );

				AdventureResult [] servant = { toUse };
				(new ItemStorageRequest( client, ItemStorageRequest.CLOSET_TO_INVENTORY, servant )).run();
			}
		}

		updateDisplay( DISABLED_STATE, "Repairing " + toUse.getName() + "..." );
		(new ConsumeItemRequest( client, toUse )).run();
		return true;
	}

	/**
	 * Helper routine which makes more of the given ingredient, if it
	 * is needed.
	 *
	 * @param	ingredientID	The ingredient to make
	 * @param	mixingMethod	How the ingredient is prepared
	 * @param	makeDouble	Whether or not you need to make double (two of same ingredient)
	 */

	private void makeIngredient( int ingredientID, int mixingMethod, boolean makeDouble )
	{
		List inventory = client.getInventory();
		AdventureResult ingredient = new AdventureResult( ingredientID, 0 );

		int index = inventory.indexOf( ingredient );
		int currentQuantity = (index == -1) ? 0 : ((AdventureResult)inventory.get( index )).getCount();

		int actualQuantityNeeded = (makeDouble ? (quantityNeeded << 1) : quantityNeeded) - currentQuantity;

		// In order to minimize server overload by making exact quantities,
		// the client will attempt to overcompensate by making more meat
		// paste than is necessary.

		if ( actualQuantityNeeded > 0 )
		{
			// Now, if you are to retrieve an item from the closet, this is where
			// that retrieval would be done.

			String useClosetForCreationSetting = client.getSettings().getProperty( "useClosetForCreation" );
			if ( useClosetForCreationSetting != null && useClosetForCreationSetting.equals( "true" ) )
			{
				List closet = client.getCloset();
				index = closet.indexOf( ingredient );

				if ( index != -1 )
				{
					AdventureResult [] retrieval = new AdventureResult[1];
					retrieval[0] = new AdventureResult( ingredient.getName(),
						Math.min( actualQuantityNeeded, ((AdventureResult)closet.get( index )).getCount() ) );

					updateDisplay( DISABLED_STATE, "Retrieving " + retrieval[0].toString() + " from closet..." );
					(new ItemStorageRequest( client, ItemStorageRequest.CLOSET_TO_INVENTORY, retrieval )).run();
					actualQuantityNeeded -= retrieval[0].getCount();
				}
			}

			// If more items are still needed, then attempt to create the desired
			// item (since that's really the only way).

			if ( actualQuantityNeeded > 0 )
				(new ItemCreationRequest( client, ingredientID, mixingMethod, actualQuantityNeeded )).run();
		}
	}

	/**
	 * Returns the name of the item created by this request.
	 * @return	The name of the item being created
	 */

	public String getName()
	{	return TradeableItemDatabase.getItemName( itemID );
	}

	/**
	 * Returns the quantity of items to be created by this request
	 * if it were to run right now.
	 */

	public int getQuantityNeeded()
	{	return quantityNeeded;
	}

	/**
	 * Sets the quantity of items to be created by this request.
	 * This method is used whenever the original quantity intended
	 * by the request changes.
	 */

	public void setQuantityNeeded( int quantityNeeded )
	{
		if ( mixingMethod != ROLLING_PIN )
			this.quantityNeeded = quantityNeeded;
	}

	/**
	 * Returns the string form of this item creation request.
	 * This displays the item name, and the amount that will
	 * be created by this request.
	 *
	 * @return	The string form of this request
	 */

	public String toString()
	{	return getName() + " (" + quantityNeeded + ")";
	}

	/**
	 * An internal class made to create meat paste.  This class
	 * takes only values of 10, 100, or 1000; it is the job of
	 * other classes to break up the request to create as much
	 * meat paste as is desired.
	 */

	private class CombineMeatRequest extends KoLRequest
	{
		private int meatType;
		private int costToMake;
		private int quantityNeeded;

		public CombineMeatRequest( KoLmafia client, int meatType, int quantityNeeded )
		{
			super( client, "inventory.php" );
			addFormField( "pwd", client.getPasswordHash() );
			addFormField( "action", "makestuff" );
			addFormField( "quantity", String.valueOf( quantityNeeded ) );
			addFormField( "whichitem", String.valueOf( meatType ) );

			this.meatType = meatType;
			this.costToMake = meatType == MEAT_PASTE ? -10 : meatType == MEAT_STACK ? -100 : -1000;
			this.quantityNeeded = quantityNeeded;
		}

		public void run()
		{
			super.run();

			client.processResult( new AdventureResult( AdventureResult.MEAT, costToMake * quantityNeeded ) );
			client.processResult( new AdventureResult( meatType, quantityNeeded ) );
		}
	}
}