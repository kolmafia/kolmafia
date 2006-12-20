/**
 * Copyright (c) 2005-2006, KoLmafia development team
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

package net.sourceforge.kolmafia;

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
	private static final CreationRequestArray ALL_CREATIONS = new CreationRequestArray();
	public static final Pattern ITEMID_PATTERN = Pattern.compile( "item\\d?=(\\d+)" );
	public static final Pattern QUANTITY_PATTERN = Pattern.compile( "quantity=(\\d+)" );

	public static final int MEAT_PASTE = 25;
	public static final int MEAT_STACK = 88;
	public static final int DENSE_STACK = 258;

	public static final int METHOD_COUNT = 25;
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
	public static final int MIX_SUPER = 19;

	public static final int CATALYST = 20;
	public static final int SUPER_REAGENT = 21;
	public static final int WOK = 22;
	public static final int MALUS = 23;

	public static final int UGH = 24;

	public static final AdventureResult OVEN = new AdventureResult( 157, 1 );
	public static final AdventureResult KIT = new AdventureResult( 236, 1 );

	private static final AdventureResult BOX = new AdventureResult( 427, 1 );
	private static final AdventureResult CHEF_SKULL = new AdventureResult( 437, 1 );
	private static final AdventureResult CHEF_SKULL_BOX = new AdventureResult( 438, 1 );
	private static final AdventureResult BARTENDER_SKULL = new AdventureResult( 439, 1 );
	private static final AdventureResult BARTENDER_SKULL_BOX = new AdventureResult( 440, 1 );

	private static final AdventureResult CHEF = new AdventureResult( 438, 1 );
	private static final AdventureResult CLOCKWORK_CHEF = new AdventureResult( 1112, 1 );
	private static final AdventureResult BARTENDER = new AdventureResult( 440, 1 );
	private static final AdventureResult CLOCKWORK_BARTENDER = new AdventureResult( 1111, 1 );

	public String name;
	public AdventureResult createdItem;
	public boolean shouldRerun = false;
	public int itemId, beforeQuantity, mixingMethod;

	private int quantityNeeded, quantityPossible;

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
	 * @param	formSource	The form to be used for the item creation
	 * @param	itemId	The item ID for the item to be handled
	 */

	public ItemCreationRequest( String formSource, int itemId )
	{	this( formSource, itemId, SUBCLASS );
	}

	/**
	 * Constructs a new <code>ItemCreationRequest</code> where you create
	 * the given number of items.
	 *
	 * @param	formSource	The form to be used for the item creation
	 * @param	itemId	The identifier for the item to be created
	 * @param	mixingMethod	How the item is created
	 * @param	quantityNeeded	How many of this item are needed
	 */

	private ItemCreationRequest( String formSource, int itemId, int mixingMethod )
	{
		super( formSource );
		addFormField( "pwd" );

		this.itemId = itemId;
		this.name = TradeableItemDatabase.getItemName( itemId );
		this.mixingMethod = mixingMethod;
		this.createdItem = new AdventureResult( itemId, 1 );
	}

	public void reconstructFields()
	{
		String formSource = "";

		switch ( mixingMethod )
		{
		case COMBINE:
			formSource = KoLCharacter.inMuscleSign() ? "knoll.php" : "combine.php";
			break;

		case MIX:
		case MIX_SPECIAL:
		case MIX_SUPER:
			formSource = "cocktail.php";
			break;

		case COOK:
		case COOK_REAGENT:
		case SUPER_REAGENT:
		case COOK_PASTA:
			formSource = "cook.php";
			break;

		case SMITH:
			formSource = KoLCharacter.inMuscleSign() ? "knoll.php" : "smith.php";
			break;

		case SMITH_ARMOR:
		case SMITH_WEAPON:
			formSource = "smith.php";
			break;

		case JEWELRY:
			formSource = "jewelry.php";
			break;

		case ROLLING_PIN:
			formSource = "inv_use.php";
			break;

		case CATALYST:
		case CLOVER:
			formSource = "multiuse.php";
			break;

		case WOK:
		case MALUS:
		case STILL_MIXER:
		case STILL_BOOZE:
			formSource = "guild.php";
			break;
		}

		constructURLString( formSource );
		addFormField( "pwd" );

		if ( KoLCharacter.inMuscleSign() && mixingMethod == SMITH )
			addFormField( "action", "smith" );

		else if ( mixingMethod == CLOVER || mixingMethod == CATALYST )
			addFormField( "action", "useitem" );

		else if ( mixingMethod == STILL_BOOZE )
			addFormField( "action", "stillbooze" );

		else if ( mixingMethod == STILL_MIXER )
			addFormField( "action", "stillfruit" );

		else if ( mixingMethod == WOK )
			addFormField( "action", "wokcook" );

		else if ( mixingMethod == MALUS )
			addFormField( "action", "malussmash" );

		else if ( mixingMethod != SUBCLASS )
			addFormField( "action", "combine" );
	}

	public static ItemCreationRequest getInstance( AdventureResult item )
	{
		ItemCreationRequest ir = getInstance( item.getItemId(), true );
		if ( ir == null )
			return null;

		ir.setQuantityNeeded( item.getCount() );
		return ir;
	}

	public static ItemCreationRequest getInstance( int itemId )
	{	return getInstance( itemId, true );
	}

	public static ItemCreationRequest getInstance( int itemId, boolean returnNullIfNotPermitted )
	{
		ItemCreationRequest instance = ALL_CREATIONS.get( itemId );

		// Assume if you have subclass that everything
		// works as intended.

		if ( instance == null || instance.mixingMethod == SUBCLASS )
			return instance;

		// If the item creation process is not permitted,
		// then return null to indicate that it is not
		// possible to create the item.

		if ( returnNullIfNotPermitted && !ConcoctionsDatabase.isPermittedMethod( instance.mixingMethod ) )
			return null;

		return instance;
	}

	public static ItemCreationRequest constructInstance( int itemId )
	{
		if ( itemId == MEAT_PASTE || itemId == MEAT_STACK || itemId == DENSE_STACK )
			return new CombineMeatRequest( itemId );

		int mixingMethod = ConcoctionsDatabase.getMixingMethod( itemId );

		// Otherwise, return the appropriate subclass of
		// item which will be created.

		switch ( mixingMethod )
		{
		case COMBINE:
			return new ItemCreationRequest( KoLCharacter.inMuscleSign() ?
				"knoll.php" : "combine.php", itemId, mixingMethod );

		case MIX:
		case MIX_SPECIAL:
		case MIX_SUPER:
			return new ItemCreationRequest( "cocktail.php", itemId, mixingMethod );

		case COOK:
		case COOK_REAGENT:
		case SUPER_REAGENT:
		case COOK_PASTA:
			return new ItemCreationRequest( "cook.php", itemId, mixingMethod );

		case SMITH:
			return new ItemCreationRequest( KoLCharacter.inMuscleSign() ?
				"knoll.php" : "smith.php", itemId, mixingMethod );

		case SMITH_ARMOR:
		case SMITH_WEAPON:
			return new ItemCreationRequest( "smith.php", itemId, mixingMethod );

		case JEWELRY:
			return new ItemCreationRequest( "jewelry.php", itemId, mixingMethod );

		case ROLLING_PIN:
			return new ItemCreationRequest( "inv_use.php", itemId, mixingMethod );

		case STARCHART:
			return new StarChartRequest( itemId );

		case PIXEL:
			return new PixelRequest( itemId );

		case TINKER:
			return new TinkerRequest( itemId );

		case TOY:
			return new ToyRequest( itemId );

		case CATALYST:
		case CLOVER:
			return new ItemCreationRequest( "multiuse.php", itemId, mixingMethod );

		case WOK:
		case MALUS:
		case STILL_MIXER:
		case STILL_BOOZE:
			return new ItemCreationRequest( "guild.php", itemId, mixingMethod );

		case UGH:
			return new UghRequest( itemId );

		default:
			return null;
		}
	}

	public boolean equals( Object o )
	{	return o != null && o instanceof ItemCreationRequest && itemId == ((ItemCreationRequest)o).itemId;
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
		if ( !KoLmafia.permitsContinue() || quantityNeeded <= 0 )
			return;

		do
		{
			shouldRerun = false;
			reconstructFields();

			beforeQuantity = createdItem.getCount( inventory );

			switch ( mixingMethod )
			{
			case SUBCLASS:

				super.run();
				break;

			case ROLLING_PIN:

				makeDough();
				break;

			default:

				combineItems();
				break;
			}
		}
		while ( shouldRerun && KoLmafia.permitsContinue() );
	}

	public void makeDough()
	{
		AdventureResult input = null;
		AdventureResult tool = null;
		AdventureResult output = null;

		// Find the array row and load the
		// correct tool/input/output data.

		for ( int i = 0; i < DOUGH_DATA.length; ++i )
		{
			output = DOUGH_DATA[i][2];
			if ( itemId == output.getItemId() )
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

		if ( AdventureDatabase.retrieveItem( input.getInstance( quantityNeeded ) ) )
			return;

		// If we don't have the correct tool, and the
		// person wishes to create more than 10 dough,
		// then notify the person that they should
		// purchase a tool before continuing.

		if ( (quantityNeeded >= 10 || KoLCharacter.hasItem( tool )) && !AdventureDatabase.retrieveItem( tool ) )
		{
			KoLmafia.updateDisplay( ERROR_STATE, "Please purchase a " + tool.getName() + " first." );
			return;
		}

		// If we have the correct tool, use it to
		// create the needed dough type.

		if ( tool.getCount( inventory ) > 0 )
		{
			KoLmafia.updateDisplay( "Using " + tool.getName() + "..." );
			(new ConsumeItemRequest( tool )).run();
			return;
		}

		// Without the right tool, we must manipulate
		// the dough by hand.

		String name = output.getName();
		ConsumeItemRequest request = new ConsumeItemRequest( input );

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

		if ( !makeIngredients() )
			return;

		AdventureResult [] ingredients = ConcoctionsDatabase.getIngredients( itemId );

		if ( ingredients.length == 1 || mixingMethod == CATALYST || mixingMethod == WOK )
		{
			// If there is only one ingredient, then it probably
			// only needs a "whichitem" field added to the request.

			addFormField( "whichitem", String.valueOf( ingredients[0].getItemId() ) );
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
				addFormField( "item" + (i+1), String.valueOf( ingredients[i].getItemId() ) );
		}

		addFormField( "quantity", String.valueOf( quantityNeeded ) );
		KoLmafia.updateDisplay( "Creating " + name + " (" + quantityNeeded + ")..." );
		super.run();
	}

	public void processResults()
	{
		// Figure out how many items were created

		AdventureResult createdItem = new AdventureResult( itemId, 0 );
		int createdQuantity = createdItem.getCount( inventory ) - beforeQuantity;

		if ( mixingMethod == STILL_MIXER || mixingMethod == STILL_BOOZE )
			KoLCharacter.decrementStillsAvailable( createdQuantity );

		// Check to make sure that the item creation did not fail.

		if ( createdQuantity == 0 )
			return;

		if ( createdQuantity > 0 )
		{
			// Because an explosion might have occurred, the
			// quantity that has changed might not be accurate.
			// Therefore, update with the actual value.

			AdventureResult [] ingredients = ConcoctionsDatabase.getIngredients( itemId );

			for ( int i = 0; i < ingredients.length; ++i )
				StaticEntity.getClient().processResult( new AdventureResult( ingredients[i].getItemId(), -1 * createdQuantity * ingredients[i].getCount() ) );

			if ( mixingMethod == COMBINE && !KoLCharacter.inMuscleSign() )
				StaticEntity.getClient().processResult( new AdventureResult( MEAT_PASTE, 0 - createdQuantity ) );
		}

		// Check to see if box-servant was overworked and exploded.

		if ( responseText.indexOf( "Smoke" ) != -1 )
		{
			KoLmafia.updateDisplay( "Your box servant has escaped!" );
			quantityNeeded = quantityNeeded - createdQuantity;

			switch ( mixingMethod )
			{
			case COOK:
			case COOK_REAGENT:
			case SUPER_REAGENT:
			case COOK_PASTA:
				KoLCharacter.setChef( false );
				shouldRerun = quantityNeeded > 0;
				break;

			case MIX:
			case MIX_SPECIAL:
			case MIX_SUPER:
				KoLCharacter.setBartender( false );
				shouldRerun = quantityNeeded > 0;
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
		if ( KoLmafia.refusesContinue() )
			return false;

		// If we are not cooking or mixing, or if we already have the
		// appropriate servant installed, we don't need to repair

		switch ( mixingMethod )
		{
		case COOK:
		case COOK_REAGENT:
		case SUPER_REAGENT:
		case COOK_PASTA:

			if ( KoLCharacter.hasChef() )
				return true;
			break;

		case MIX:
		case MIX_SPECIAL:
		case MIX_SUPER:

			if ( KoLCharacter.hasBartender() )
				return true;
			break;

		case SMITH:

			if ( !KoLCharacter.inMuscleSign() )
				return AdventureDatabase.retrieveItem( ConcoctionsDatabase.HAMMER );

			return true;

		case SMITH_WEAPON:
		case SMITH_ARMOR:

			return AdventureDatabase.retrieveItem( ConcoctionsDatabase.HAMMER );

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
		case SUPER_REAGENT:
		case COOK_PASTA:
			autoRepairSuccessful = useBoxServant( CHEF, CLOCKWORK_CHEF, OVEN, CHEF_SKULL, CHEF_SKULL_BOX );
			break;

		case MIX:
		case MIX_SPECIAL:
		case MIX_SUPER:
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
		boolean isCreatePermitted = StaticEntity.getBooleanProperty( "createWithoutBoxServants" );
		boolean hasNoServantItem = inventory.contains( noServantItem ) || KoLCharacter.getAvailableMeat() >= 1000;

		if ( !StaticEntity.getBooleanProperty( "autoRepairBoxes" ) )
		{
			if ( !isCreatePermitted )
				return false;

			if ( hasNoServantItem )
				return AdventureDatabase.retrieveItem( noServantItem );

			return hasNoServantItem;
		}

		if ( KoLCharacter.hasItem( clockworkServant, false ) )
			usedServant = clockworkServant;

		else if ( KoLCharacter.hasItem( clockworkServant, true ) )
			usedServant = clockworkServant;

		else if ( KoLCharacter.hasItem( servant, false ) )
			usedServant = servant;

		else if ( KoLCharacter.hasItem( servant, true ) )
			usedServant = servant;

		if ( usedServant == null )
		{
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
			else if ( isCreatePermitted )
			{
				return hasNoServantItem ? AdventureDatabase.retrieveItem( noServantItem ) : false;
			}
			else if ( KoLCharacter.canInteract() )
			{
				usedServant = servant;
			}

			if ( usedServant == null )
				return false;
		}

		// Once you hit this point, you're guaranteed to
		// have the servant in your inventory, so attempt
		// to repair the box servant.

		(new ConsumeItemRequest( usedServant )).run();
		return servant == CHEF ? KoLCharacter.hasChef() : KoLCharacter.hasBartender();
	}

	public boolean makeIngredients()
	{
		KoLmafia.updateDisplay( "Verifying ingredients for " + name + "..." );

		boolean foundAllIngredients = true;

		// If this is a combining request, you will need to make
		// paste as well.  Make an even multiple, if none is left.

		if ( mixingMethod == COMBINE && !KoLCharacter.inMuscleSign() )
		{
			int pasteNeeded = ConcoctionsDatabase.getMeatPasteRequired( itemId, quantityNeeded );
			AdventureResult paste = new AdventureResult( MEAT_PASTE, pasteNeeded );
			foundAllIngredients &= AdventureDatabase.retrieveItem( paste );
		}

 		AdventureResult [] ingredients = ConcoctionsDatabase.getIngredients( itemId );

		for ( int i = 0; i < ingredients.length; ++i )
		{
			// First, calculate the multiplier that's needed
			// for this ingredient to avoid not making enough
			// intermediate ingredients and getting an error.

			int multiplier = 0;
			for ( int j = 0; j < ingredients.length; ++j )
				if ( ingredients[i].getItemId() == ingredients[j].getItemId() )
					multiplier += ingredients[i].getCount();

			// Then, make enough of the ingredient in order
			// to proceed with the concoction.

			foundAllIngredients &= AdventureDatabase.retrieveItem( ingredients[i].getInstance( quantityNeeded * multiplier ) );
		}

		return foundAllIngredients;
	}

	/**
	 * Returns the item Id for the item created by this request.
	 * @return	The item Id of the item being created
	 */

	public int getItemId()
	{	return itemId;
	}

	/**
	 * Returns the name of the item created by this request.
	 * @return	The name of the item being created
	 */

	public String getName()
	{	return TradeableItemDatabase.getItemName( itemId );
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
	 * Returns the quantity of items to be created by this request
	 * if it were to run right now.
	 */

	public int getQuantityPossible()
	{	return quantityPossible;
	}

	/**
	 * Sets the quantity of items to be created by this request.
	 * This method is used whenever the original quantity intended
	 * by the request changes.
	 */

	public void setQuantityPossible( int quantityPossible )
	{	this.quantityPossible = quantityPossible;
	}

	/**
	 * Returns the string form of this item creation request.
	 * This displays the item name, and the amount that will
	 * be created by this request.
	 *
	 * @return	The string form of this request
	 */

	public String toString()
	{	return getName() + " (" + getQuantityPossible() + ")";
	}

	public String getCommandForm()
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
		switch ( mixingMethod )
		{
		case SMITH:
			return KoLCharacter.inMuscleSign() ? 0 : quantityNeeded;

		case SMITH_ARMOR:
		case SMITH_WEAPON:
			return quantityNeeded;

		case JEWELRY:
			return 3 * quantityNeeded;

		case COOK:
		case COOK_REAGENT:
		case SUPER_REAGENT:
		case COOK_PASTA:
			return KoLCharacter.hasChef() ? 0 : quantityNeeded;

		case MIX:
		case MIX_SPECIAL:
		case MIX_SUPER:
			return KoLCharacter.hasBartender() ? 0 : quantityNeeded;

		case WOK:
			return quantityNeeded;
		}

		return 0;
	}

	public static boolean processRequest( String urlString )
	{
		// First, delegate subclasses, if it's a subclass request.

		if ( urlString.indexOf( "starchart.php" ) != -1 )
			return StarChartRequest.processRequest( urlString );

		if ( urlString.indexOf( "action=makepixel" ) != -1 )
			return PixelRequest.processRequest( urlString );

		if ( urlString.indexOf( "action=tinksomething" ) != -1 )
			return TinkerRequest.processRequest( urlString );

		if ( urlString.indexOf( "action=makepaste" ) != -1 )
			return CombineMeatRequest.processRequest( urlString );

		// Now that we know it's not a special subclass instance,
		// all we do is parse out the ingredients which were used
		// and then print the attempt to the screen.

		boolean usesTurns = false;
		boolean isCreationURL = false;

		StringBuffer command = new StringBuffer();
		Matcher itemMatcher = ITEMID_PATTERN.matcher( urlString );
		Matcher quantityMatcher = QUANTITY_PATTERN.matcher( urlString );

		if ( urlString.indexOf( "combine.php" ) != -1 )
		{
			isCreationURL = true;
			command.append( "Combine " );
		}
		else if ( urlString.indexOf( "cocktail.php" ) != -1 )
		{
			isCreationURL = true;
			command.append( "Mix " );
			usesTurns = !KoLCharacter.hasBartender();
		}
		else if ( urlString.indexOf( "cook.php" ) != -1 )
		{
			isCreationURL = true;
			command.append( "Cook " );
			usesTurns = !KoLCharacter.hasChef();
		}
		else if ( urlString.indexOf( "smith.php" ) != -1 )
		{
			isCreationURL = urlString.indexOf( "action=pulverize" ) == -1;
			command.append( "Smith " );
			usesTurns = urlString.indexOf( "action=pulverize" ) == -1;
		}
		else if ( urlString.indexOf( "jewelry.php" ) != -1 )
		{
			isCreationURL = true;
			command.append( "Ply " );
			usesTurns = true;
		}
		else if ( urlString.indexOf( "action=stillbooze" ) != -1 || urlString.indexOf( "action=stillfruit" ) != -1 )
		{
			isCreationURL = true;
			command.append( "Distill " );
			usesTurns = false;
		}
		else if ( urlString.indexOf( "action=wokcook" ) != -1 )
		{
			isCreationURL = true;
			command.append( "Wok " );
			usesTurns = true;
		}
		else if ( urlString.indexOf( "action=malussmash" ) != -1 )
		{
			isCreationURL = true;
			command.append( "Pulverize " );
			usesTurns = false;
		}

		if ( !isCreationURL )
			return false;

		boolean needsPlus = false;
		int quantity = quantityMatcher.find() ? StaticEntity.parseInt( quantityMatcher.group(1) ) : 1;

		while ( isCreationURL && itemMatcher.find() )
		{
			if ( needsPlus )
				command.append( " + " );

			int itemId = StaticEntity.parseInt( itemMatcher.group(1) );
			String name = TradeableItemDatabase.getItemName( itemId );

			if ( name == null )
				continue;

			command.append( quantity );
			command.append( ' ' );
			command.append( name );

			StaticEntity.getClient().processResult( new AdventureResult( itemId, 0 - quantity ) );
			needsPlus = true;
		}

		if ( urlString.indexOf( "combine.php" ) != -1 )
			StaticEntity.getClient().processResult( new AdventureResult( MEAT_PASTE, 0 - quantity ) );

		if ( usesTurns )
			command.insert( 0, "[" + KoLCharacter.getTotalTurnsUsed() + "] " );

		KoLmafia.getSessionStream().println( command.toString() );
		return true;
	}

	private static class CreationRequestArray
	{
		private ArrayList internalList = new ArrayList();

		public ItemCreationRequest get( int index )
		{
			if ( index < 0 )
				return null;

			for ( int i = internalList.size(); i <= index; ++i )
				internalList.add( constructInstance( i ) );

			return (ItemCreationRequest) internalList.get( index );
		}

		public void set( int index, ItemCreationRequest value )
		{
			for ( int i = internalList.size(); i <= index; ++i )
				internalList.add( constructInstance( i ) );

			internalList.set( index, value );
		}

		public int size()
		{	return internalList.size();
		}
	}
}
