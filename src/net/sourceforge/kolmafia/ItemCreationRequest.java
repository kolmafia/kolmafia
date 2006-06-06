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

	public static final int METHOD_COUNT = 19;
	public static final int SUBCLASS = Integer.MAX_VALUE;

	public static final int NOCREATE = 0;
	public static final int COMBINE = 1;
	public static final int COOK = 2;
	public static final int MIX = 3;
	public static final int SMITH = 4;

	public static final int COOK_REAGENT = 5;
	public static final int COOK_PASTA = 6;
	public static final int MIX_SPECIAL = 7;

	public static final int JEWELRY = 8;
	public static final int STARCHART = 9;
	public static final int PIXEL = 10;
	public static final int ROLLING_PIN = 11;

	public static final int TINKER = 12;
	public static final int SMITH_WEAPON = 13;
	public static final int SMITH_ARMOR = 14;

	public static final int TOY = 15;
	public static final int CLOVER = 16;

	public static final int STILL_BOOZE = 17;
	public static final int STILL_MIXER = 18;

	private static final AdventureResult OVEN = new AdventureResult( 157, 1 );
	private static final AdventureResult KIT = new AdventureResult( 236, 1 );

	private static final AdventureResult BOX = new AdventureResult( 427, 1 );
	private static final AdventureResult CHEF_SKULL = new AdventureResult( 437, 1 );
	private static final AdventureResult CHEF_SKULL_BOX = new AdventureResult( 438, 1 );
	private static final AdventureResult BARTENDER_SKULL = new AdventureResult( 439, 1 );
	private static final AdventureResult BARTENDER_SKULL_BOX = new AdventureResult( 440, 1 );

	private static final AdventureResult CHEF = new AdventureResult( 438, 1 );
	private static final AdventureResult CLOCKWORK_CHEF = new AdventureResult( 1112, 1 );
	private static final AdventureResult BARTENDER = new AdventureResult( 440, 1 );
	private static final AdventureResult CLOCKWORK_BARTENDER = new AdventureResult( 1111, 1 );

	private String name;
	private boolean completedRequest;
	private int itemID, quantityNeeded, mixingMethod;

	private static final AdventureResult DOUGH = new AdventureResult( 159, 1 );
	private static final AdventureResult FLAT_DOUGH = new AdventureResult( 301, 1 );
	private static final AdventureResult ROLLING = new AdventureResult( 873, 1 );
	private static final AdventureResult UNROLLING = new AdventureResult( 874, 1 );

	private static final AdventureResult [][] DOUGH_DATA =
	{       // input, tool, output
			{ DOUGH, ROLLING, FLAT_DOUGH },
			{ FLAT_DOUGH, UNROLLING, DOUGH }
	};

	/**
	 * Constructs a new <code>ItemCreationRequest</code> with nothing known
	 * other than the form to use.  This is used by descendant classes to
	 * avoid weird type-casting problems, as it assumes that there is no
	 * known way for the item to be created.
	 *
	 * @param	client	The client to be notified of the item creation
	 * @param	formSource	The form to be used for the item creation
	 */

	protected ItemCreationRequest( KoLmafia client, String formSource, int itemID, int quantityNeeded )
	{	this( client, formSource, itemID, SUBCLASS, quantityNeeded );
	}

	/**
	 * Constructs a new <code>ItemCreationRequest</code> where you create
	 * the given number of items.
	 *
	 * @param	client	The client to be notified of the item creation
	 * @param	formSource	The form to be used for the item creation
	 * @param	itemID	The identifier for the item to be created
	 * @param	mixingMethod	How the item is created
	 * @param	quantityNeeded	How many of this item are needed
	 */

	protected ItemCreationRequest( KoLmafia client, String formSource, int itemID, int mixingMethod, int quantityNeeded )
	{
		super( client, formSource );

		this.itemID = itemID;
		this.mixingMethod = mixingMethod;
		this.quantityNeeded = quantityNeeded;

        addFormField( "pwd" );

		if ( KoLCharacter.inMuscleSign() && mixingMethod == SMITH )
			addFormField( "action", "smith" );

		else if ( mixingMethod == CLOVER )
			addFormField( "action", "useitem" );

		else if ( mixingMethod == STILL_BOOZE )
			addFormField( "action", "stillbooze" );

		else if ( mixingMethod == STILL_MIXER )
			addFormField( "action", "stillfruit" );

		else if ( mixingMethod != SUBCLASS )
			addFormField( "action", "combine" );
	}

	/**
	 * Static method which determines the appropriate subclass
	 * of an ItemCreationRequest to return, based on the idea
	 * that the given AdventureResult is the item to be created.
	 */

	public static ItemCreationRequest getInstance( KoLmafia client, AdventureResult ar )
	{	return getInstance( client, ar.getItemID(), ar.getCount() );
	}

	/**
	 * Static method which determines the appropriate subclass
	 * of an ItemCreationRequest to return, based on the idea
	 * that the given quantity of the given item is to be created.
	 */

	public static ItemCreationRequest getInstance( KoLmafia client, int itemID, int quantityNeeded )
	{	return getInstance( client, itemID, quantityNeeded, true );
	}

	/**
	 * Static method which determines the appropriate subclass
	 * of an ItemCreationRequest to return, based on the idea
	 * that the given quantity of the given item is to be created.
	 */

	public static ItemCreationRequest getInstance( KoLmafia client, int itemID, int quantityNeeded, boolean returnNullIfImpossible )
	{
		if ( itemID == MEAT_PASTE || itemID == MEAT_STACK || itemID == DENSE_STACK )
			return new CombineMeatRequest( client, itemID, quantityNeeded );

		int mixingMethod = ConcoctionsDatabase.getMixingMethod( itemID );

		// If the item creation process is not permitted,
		// then return null to indicate that it is not
		// possible to create the item.

		if ( returnNullIfImpossible && !ConcoctionsDatabase.isPermittedMethod( mixingMethod ) )
			return null;

		// Otherwise, return the appropriate subclass of
		// item which will be created.

		switch ( mixingMethod )
		{
			case COMBINE:
				return new ItemCreationRequest( client, KoLCharacter.inMuscleSign() ?
					"knoll.php" : "combine.php", itemID, mixingMethod, quantityNeeded );

			case MIX:
			case MIX_SPECIAL:
				return new ItemCreationRequest( client, "cocktail.php", itemID, mixingMethod, quantityNeeded );

			case COOK:
			case COOK_REAGENT:
			case COOK_PASTA:
				return new ItemCreationRequest( client, "cook.php", itemID, mixingMethod, quantityNeeded );

			case SMITH:
				return new ItemCreationRequest( client, KoLCharacter.inMuscleSign() ?
					"knoll.php" : "smith.php", itemID, mixingMethod, quantityNeeded );

			case SMITH_ARMOR:
			case SMITH_WEAPON:
				return new ItemCreationRequest( client, "smith.php", itemID, mixingMethod, quantityNeeded );

			case JEWELRY:
				return new ItemCreationRequest( client, "jewelry.php", itemID, mixingMethod, quantityNeeded );

			case ROLLING_PIN:
				return new ItemCreationRequest( client, "inv_use.php", itemID, mixingMethod, quantityNeeded );

			case STARCHART:
				return new StarChartRequest( client, itemID, quantityNeeded );

			case PIXEL:
				return new PixelRequest( client, itemID, quantityNeeded );

			case TINKER:
				return new TinkerRequest( client, itemID, quantityNeeded );

			case TOY:
				return new ToyRequest( client, itemID, quantityNeeded );

			case CLOVER:
				return new ItemCreationRequest( client, "multiuse.php", itemID, mixingMethod, quantityNeeded );

			case STILL_MIXER:
			case STILL_BOOZE:
				return new ItemCreationRequest( client, "guild.php", itemID, mixingMethod, quantityNeeded );

			default:
				return null;
		}
	}

	public boolean equals( Object o )
	{	return o != null && o instanceof ItemCreationRequest && itemID == ((ItemCreationRequest)o).itemID;
	}

	public int compareTo( Object o )
	{	return o == null ? -1 : this.getName().compareToIgnoreCase( ((ItemCreationRequest)o).getName() );
	}

	/**
	 * Runs the item creation request.  Note that if another item needs
	 * to be created for the request to succeed, this method will fail.
	 */

	public void run()
	{
		completedRequest = false;
		if ( !KoLmafia.permitsContinue() || quantityNeeded <= 0 )
			return;

		switch ( mixingMethod )
		{
			case SUBCLASS:

				super.run();
				return;

			case ROLLING_PIN:

				makeDough();
				return;

			default:

				combineItems();
				return;
		}
	}

	protected void makeDough()
	{
		AdventureResult input = null;
		AdventureResult tool = null;
		AdventureResult output = null;

		// Find the array row and load the
		// correct tool/input/output data.

		for ( int i = 0; i < DOUGH_DATA.length; ++i )
		{
			output = DOUGH_DATA[i][2];
			if ( itemID == output.getItemID() )
			{
				tool = DOUGH_DATA[i][1];
				input = DOUGH_DATA[i][0];
				break;
			}
		}

		if ( tool == null )
		{
			KoLmafia.updateDisplay( ERROR_STATE, "Can't deduce correct tool to use." );
			return;
		}

		// If we have the correct tool, use it to
		// create the needed dough type.

		if ( quantityNeeded >= 10 )
			AdventureDatabase.retrieveItem( tool );

		if ( tool.getCount( KoLCharacter.getInventory() ) > 0 )
		{
			KoLmafia.updateDisplay( "Using " + tool.getName() + "..." );
			(new ConsumeItemRequest( client, tool )).run();
			return;
		}

		// If we don't have the correct tool, and the
		// person wishes to create more than 10 dough,
		// then notify the person that they should
		// purchase a tool before continuing.

		if ( quantityNeeded >= 10 )
		{
			KoLmafia.updateDisplay( ERROR_STATE, "Please purchase a " + tool.getName() + " first." );
			return;
		}

		// Without the right tool, we must manipulate
		// the dough by hand.

		String name = output.getName();
		ConsumeItemRequest request = new ConsumeItemRequest( client, input );
		AdventureDatabase.retrieveItem( input.getInstance( quantityNeeded ) );

		for ( int i = 1; KoLmafia.permitsContinue() && i <= quantityNeeded; ++i )
		{
			KoLmafia.updateDisplay( "Creating " + name + " (" + i + " of " + quantityNeeded + ")..." );
			request.run();
		}
	}

	/**
	 * Helper routine which actually does the item combination.
	 */

	private void combineItems()
	{
		// First, make all the required ingredients for
		// this concoction.

		makeIngredients();
		AdventureResult [] ingredients = ConcoctionsDatabase.getIngredients( itemID );

		if ( ingredients.length == 1 )
		{
			// If there is only one ingredient, then it probably
			// only needs a "whichitem" field added to the request.

			addFormField( "whichitem", String.valueOf( ingredients[0].getItemID() ) );
		}
		else
		{
			// Check to make sure that the box servant is available
			// for this combine step.  This is extra overhead when
			// no box servant is needed, but for easy readability,
			// the test always occurs.

			if ( !autoRepairBoxServant() )
				return;

			for ( int i = 0; i < ingredients.length; ++i )
				addFormField( "item" + (i+1), String.valueOf( ingredients[i].getItemID() ) );
		}

		addFormField( "quantity", String.valueOf( quantityNeeded ) );

		KoLmafia.updateDisplay( "Creating " + toString() + "..." );
		super.run();
	}

	protected void processResults()
	{
		if ( mixingMethod == STILL_BOOZE || mixingMethod == STILL_MIXER )
			KoLCharacter.setStillsAvailable( responseText );

		AdventureResult createdItem = new AdventureResult( itemID, 0 );
		int beforeQuantity = createdItem.getCount( KoLCharacter.getInventory() );

		super.processResults();

		// Check to make sure that the item creation did not fail.

		if ( responseText.indexOf( "You don't have enough" ) != -1 )
		{
			KoLmafia.updateDisplay( ERROR_STATE, "You're missing ingredients." );
			return;
		}

		if ( responseText.indexOf( "You don't have that many adventures left" ) != -1 )
		{
			KoLmafia.updateDisplay( ERROR_STATE, "You don't have enough adventures." );
			return;
		}

		// Figure out how many items were created

		String itemName = TradeableItemDatabase.getItemName( itemID );
		int createdQuantity = createdItem.getCount( KoLCharacter.getInventory() ) - beforeQuantity;

		if ( createdQuantity > 0 )
		{
			completedRequest = true;

			// Because an explosion might have occurred, the
			// quantity that has changed might not be accurate.
			// Therefore, update with the actual value.

			AdventureResult [] ingredients = ConcoctionsDatabase.getIngredients( itemID );

			for ( int i = 0; i < ingredients.length; ++i )
				client.processResult( new AdventureResult( ingredients[i].getItemID(), -1 * createdQuantity * ingredients[i].getCount() ) );

			switch ( mixingMethod )
			{
				case COMBINE:
					if ( !KoLCharacter.inMuscleSign() )
						client.processResult( new AdventureResult( MEAT_PASTE, 0 - createdQuantity ) );
					break;
			}
		}

		// Check to see if box-servant was overworked and exploded.

		if ( responseText.indexOf( "Smoke" ) != -1 )
		{
			KoLmafia.updateDisplay( "Your box servant has escaped!" );
			ItemCreationRequest leftOver = ItemCreationRequest.getInstance( client, itemID, quantityNeeded - createdQuantity );

			switch ( mixingMethod )
			{
				case COOK:
				case COOK_REAGENT:
				case COOK_PASTA:
					KoLCharacter.setChef( false );
					leftOver.run();
					break;

				case MIX:
				case MIX_SPECIAL:
					KoLCharacter.setBartender( false );
					leftOver.run();
					break;
			}
		}
		else
		{
			KoLmafia.updateDisplay( "Successfully created " + quantityNeeded + " " + getName() );
		}
	}

	private boolean autoRepairBoxServant()
	{
		if ( !KoLmafia.permitsContinue() )
			return false;

		// If we are not cooking or mixing, or if we already have the
		// appropriate servant installed, we don't need to repair

		switch ( mixingMethod )
		{
			case COOK:
			case COOK_REAGENT:
			case COOK_PASTA:

				if ( KoLCharacter.hasChef() )
					return true;
				break;

			case MIX:
			case MIX_SPECIAL:

				if ( KoLCharacter.hasBartender() )
					return true;
				break;

			default:
				return true;
		}

		boolean autoRepairSuccessful = false;

		// If they do want to auto-repair, make sure that
		// the appropriate item is available in their inventory

		switch ( mixingMethod )
		{
			case COOK:
			case COOK_REAGENT:
			case COOK_PASTA:
				autoRepairSuccessful = useBoxServant( CHEF, CLOCKWORK_CHEF, OVEN, CHEF_SKULL, CHEF_SKULL_BOX );
				break;

			case MIX:
			case MIX_SPECIAL:
				autoRepairSuccessful = useBoxServant( BARTENDER, CLOCKWORK_BARTENDER, KIT, BARTENDER_SKULL, BARTENDER_SKULL_BOX );
				break;
		}

		if ( !autoRepairSuccessful )
			KoLmafia.updateDisplay( ERROR_STATE, "Auto-repair was unsuccessful." );

		return autoRepairSuccessful && KoLmafia.permitsContinue();
	}

	private boolean useBoxServant( AdventureResult servant, AdventureResult clockworkServant, AdventureResult noServantItem, AdventureResult skullItem, AdventureResult boxedItem )
	{
		// First, check to see if a box servant is available
		// for usage, either normally, or through some form
		// of creation.

		AdventureResult usedServant = null;

		if ( getProperty( "autoRepairBoxes" ).equals( "false" ) )
			return getProperty( "createWithoutBoxServants" ).equals( "true" ) && noServantItem.getCount( KoLCharacter.getInventory() ) > 0;

		if ( KoLCharacter.hasItem( servant, false ) )
			usedServant = servant;

		else if ( KoLCharacter.hasItem( clockworkServant, false ) )
			usedServant = clockworkServant;

		else if ( KoLCharacter.hasItem( servant, true ) )
			usedServant = servant;

		else if ( KoLCharacter.hasItem( clockworkServant, true ) )
			usedServant = clockworkServant;

		if ( usedServant == null )
		{
			boolean isCreatePermitted = getProperty( "createWithoutBoxServants" ).equals( "true" );

			// If they cannot afford to buy an NPC store item, that means
			// it all depends on whether or not they currently have the item.

			if ( KoLCharacter.getAvailableMeat() < 1000 )
			{
				return isCreatePermitted &&
					noServantItem.getCount( KoLCharacter.getInventory() ) > 0;
			}

			// If the player can construct the box servant with just a few
			// more purchases, then do so.

			if ( KoLCharacter.hasItem( boxedItem, true ) )
			{
				usedServant = servant;
			}
			else if ( KoLCharacter.inMuscleSign() )
			{
				if ( KoLCharacter.hasItem( skullItem, true ) && KoLCharacter.hasItem( BOX, false ) )
					usedServant = servant;
			}
			else if ( KoLCharacter.canInteract() && getProperty( "autoSatisfyChecks" ).equals( "true" ) )
			{
				usedServant = servant;
			}

			if ( usedServant == null )
				return isCreatePermitted;
		}

		// Once you hit this point, you're guaranteed to
		// have the servant in your inventory, so attempt
		// to repair the box servant.


		(new ConsumeItemRequest( client, usedServant )).run();
		return KoLmafia.permitsContinue();
	}

	protected void makeIngredients()
	{
		AdventureResult [] ingredients = ConcoctionsDatabase.getIngredients( itemID );

		for ( int i = 0; i < ingredients.length; ++i )
		{
			// First, calculate the multiplier that's needed
			// for this ingredient to avoid not making enough
			// intermediate ingredients and getting an error.

			int multiplier = 0;
			for ( int j = 0; j < ingredients.length; ++j )
				if ( ingredients[i].getItemID() == ingredients[j].getItemID() )
					multiplier += ingredients[i].getCount();

			// Then, make enough of the ingredient in order
			// to proceed with the concoction.

			AdventureDatabase.retrieveItem( ingredients[i].getInstance( quantityNeeded * multiplier ) );
		}

		// If this is a combining request, you will need
		// to make meat paste as well.

		if ( mixingMethod == COMBINE && !KoLCharacter.inMuscleSign() && KoLmafia.permitsContinue() )
			AdventureDatabase.retrieveItem( new AdventureResult( MEAT_PASTE, quantityNeeded ) );
	}

	/**
	 * Returns the item ID for the item created by this request.
	 * @return	The item ID of the item being created
	 */

	public int getItemID()
	{	return itemID;
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
	 * Special method which simplifies the constant use of indexOf and
	 * count retrieval.  This makes intent more transparent.
	 */

	public int getCount( List list )
	{
		int index = list.indexOf( this );
		return index == -1 ? 0 : ((ItemCreationRequest)list.get( index )).getQuantityNeeded();
	}

	public String getCommandForm( int iterations )
	{	return "create " + getQuantityNeeded() + " \"" + getName() + "\"";
	}

	/**
	 * An alternative method to doing adventure calculation is determining
	 * how many adventures are used by the given request, and subtract
	 * them after the request is done.
	 *
	 * @return	The number of adventures used by this request.
	 */

	public int getAdventuresUsed()
	{
		if ( !completedRequest )
			return 0;

		switch ( mixingMethod )
		{
			case SMITH:
				if ( !KoLCharacter.inMuscleSign() )
					return quantityNeeded;
				break;

			case SMITH_ARMOR:
			case SMITH_WEAPON:
				return quantityNeeded;

			case JEWELRY:
				return 3 * quantityNeeded;

			case COOK:
			case COOK_REAGENT:
			case COOK_PASTA:
				if ( !KoLCharacter.hasChef() )
					return quantityNeeded;
				break;

			case MIX:
			case MIX_SPECIAL:
				if ( !KoLCharacter.hasBartender() )
					return quantityNeeded;
				break;
		}

		return 0;
	}
}
