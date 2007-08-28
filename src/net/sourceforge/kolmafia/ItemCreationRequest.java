/**
 * Copyright (c) 2005-2007, KoLmafia development team
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ItemCreationRequest extends KoLRequest implements Comparable
{
	private static final CreationRequestArray ALL_CREATIONS = new CreationRequestArray();
	protected static final Pattern ITEMID_PATTERN = Pattern.compile( "item\\d?=(\\d+)" );
	protected static final Pattern QUANTITY_PATTERN = Pattern.compile( "quantity=(\\d+)" );

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

	private static final int DRY_NOODLES = 304;
	private static final int MSG = 1549;

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
		this.addFormField( "pwd" );

		this.itemId = itemId;
		this.name = TradeableItemDatabase.getItemName( itemId );
		this.mixingMethod = mixingMethod;
		this.createdItem = new AdventureResult( itemId, 1 );
	}

	public void reconstructFields()
	{
		String formSource = "";

		switch ( this.mixingMethod )
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
		case EXPENSIVE_JEWELRY:
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

		this.constructURLString( formSource );
		this.addFormField( "pwd" );

		if ( KoLCharacter.inMuscleSign() && this.mixingMethod == SMITH )
			this.addFormField( "action", "smith" );

		else if ( this.mixingMethod == CLOVER || this.mixingMethod == CATALYST )
			this.addFormField( "action", "useitem" );

		else if ( this.mixingMethod == STILL_BOOZE )
			this.addFormField( "action", "stillbooze" );

		else if ( this.mixingMethod == STILL_MIXER )
			this.addFormField( "action", "stillfruit" );

		else if ( this.mixingMethod == WOK )
			this.addFormField( "action", "wokcook" );

		else if ( this.mixingMethod == MALUS )
			this.addFormField( "action", "malussmash" );

		else if ( this.mixingMethod != SUBCLASS )
			this.addFormField( "action", "combine" );
	}

	public static final ItemCreationRequest getInstance( AdventureResult item )
	{
		ItemCreationRequest ir = getInstance( item.getItemId(), true );
		if ( ir == null )
			return null;

		ir.setQuantityNeeded( item.getCount() );
		return ir;
	}

	public static final ItemCreationRequest getInstance( int itemId )
	{	return getInstance( itemId, true );
	}

	public static final ItemCreationRequest getInstance( int itemId, boolean returnNullIfNotPermitted )
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

	public static final ItemCreationRequest constructInstance( int itemId )
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
		case EXPENSIVE_JEWELRY:
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

		case STAFF:
			return new ChefstaffRequest( itemId );

		case MULTI_USE:
			return new MultiUseRequest( itemId );

		default:
			return null;
		}
	}

	public boolean equals( Object o )
	{	return o != null && o instanceof ItemCreationRequest && this.itemId == ((ItemCreationRequest)o).itemId;
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
		if ( !KoLmafia.permitsContinue() || this.quantityNeeded <= 0 )
			return;

		boolean usesAdventures;

		do
		{
			this.shouldRerun = false;
			this.reconstructFields();

			usesAdventures = getAdventuresUsed() > 0;
			this.beforeQuantity = this.createdItem.getCount( inventory );

			switch ( this.mixingMethod )
			{
			case SUBCLASS:

				super.run();
				break;

			case ROLLING_PIN:

				this.makeDough();
				break;

			default:

				this.combineItems();
				break;
			}

			if ( !containsUpdate && usesAdventures )
				CharpaneRequest.getInstance().run();
		}
		while ( this.shouldRerun && KoLmafia.permitsContinue() );
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
			if ( this.itemId == output.getItemId() )
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

		if ( !AdventureDatabase.retrieveItem( input.getInstance( this.quantityNeeded ) ) )
			return;

		// If we don't have the correct tool, and the
		// person wishes to create more than 10 dough,
		// then notify the person that they should
		// purchase a tool before continuing.

		if ( (this.quantityNeeded >= 10 || KoLCharacter.hasItem( tool )) && !AdventureDatabase.retrieveItem( tool ) )
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

		for ( int i = 1; KoLmafia.permitsContinue() && i <= this.quantityNeeded; ++i )
		{
			KoLmafia.updateDisplay( "Creating " + name + " (" + i + " of " + this.quantityNeeded + ")..." );
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

		if ( !this.makeIngredients() )
			return;

		AdventureResult [] ingredients = ConcoctionsDatabase.getIngredients( this.itemId );

		if ( ingredients.length == 1 || this.mixingMethod == CATALYST || this.mixingMethod == WOK )
		{
			// If there is only one ingredient, then it probably
			// only needs a "whichitem" field added to the request.

			this.addFormField( "whichitem", String.valueOf( ingredients[0].getItemId() ) );
		}
		else
		{
			// Check to make sure that the box servant is available
			// for this combine step.  This is extra overhead when
			// no box servant is needed, but for easy readability,
			// the test always occurs.

			if ( !this.autoRepairBoxServant() )
				return;

			for ( int i = 0; i < ingredients.length; ++i )
				this.addFormField( "item" + (i+1), String.valueOf( ingredients[i].getItemId() ) );
		}

		if ( (mixingMethod == COOK_REAGENT || mixingMethod == SUPER_REAGENT) && KoLCharacter.getClassType().equals( KoLCharacter.SAUCEROR ) )
			this.addFormField( "quantity", String.valueOf( (int) Math.ceil( this.quantityNeeded / 3.0f ) ) );
		else
			this.addFormField( "quantity", String.valueOf( this.quantityNeeded ) );

		KoLmafia.updateDisplay( "Creating " + this.name + " (" + this.quantityNeeded + ")..." );
		super.run();
	}

	public void processResults()
	{
		// Figure out how many items were created

		AdventureResult createdItem = new AdventureResult( this.itemId, 0 );

		int createdQuantity = createdItem.getCount( inventory ) - this.beforeQuantity;

		int quantityDifference = 0;

		if ( (mixingMethod == COOK_REAGENT || mixingMethod == SUPER_REAGENT) && KoLCharacter.getClassType().equals( KoLCharacter.SAUCEROR ) )
			quantityDifference = this.quantityNeeded - (createdQuantity / 3);
		else
			quantityDifference = this.quantityNeeded - createdQuantity;

		// Because an explosion might have occurred, the
		// quantity that has changed might not be accurate.
		// Therefore, update with the actual value.

		if ( quantityDifference != 0 )
		{
			AdventureResult [] ingredients = ConcoctionsDatabase.getIngredients( this.itemId );

			for ( int i = 0; i < ingredients.length; ++i )
				StaticEntity.getClient().processResult( new AdventureResult( ingredients[i].getItemId(), quantityDifference * ingredients[i].getCount() ) );

			switch ( this.mixingMethod )
			{
			case COMBINE:
				if ( !KoLCharacter.inMuscleSign() )
					StaticEntity.getClient().processResult( new AdventureResult( MEAT_PASTE, quantityDifference ) );
				break;

			case SMITH:
				if ( !KoLCharacter.inMuscleSign() )
					StaticEntity.getClient().processResult( new AdventureResult( AdventureResult.ADV, 0 - quantityDifference ) );
				break;

			case SMITH_WEAPON:
			case SMITH_ARMOR:
			case WOK:
				StaticEntity.getClient().processResult( new AdventureResult( AdventureResult.ADV, 0 - quantityDifference ) );
				break;

			case JEWELRY:
			case EXPENSIVE_JEWELRY:
				StaticEntity.getClient().processResult( new AdventureResult( AdventureResult.ADV, 0 - 3 * quantityDifference ) );
				break;

			case COOK:
			case COOK_REAGENT:
			case SUPER_REAGENT:
			case COOK_PASTA:
				if ( !KoLCharacter.hasChef() )
					StaticEntity.getClient().processResult( new AdventureResult( AdventureResult.ADV, 0 - quantityDifference ) );
				break;

			case MIX:
			case MIX_SPECIAL:
			case MIX_SUPER:
				if ( !KoLCharacter.hasBartender() )
					StaticEntity.getClient().processResult( new AdventureResult( AdventureResult.ADV, 0 - quantityDifference ) );
				break;
			}
		}

		// Check to see if box-servant was overworked and exploded.

		KoLmafia.updateDisplay( "Successfully created " + createdQuantity + " " + this.getName() );

		if ( this.responseText.indexOf( "Smoke" ) != -1 )
		{
			KoLmafia.updateDisplay( "Your box servant has escaped!" );
			this.quantityNeeded = this.quantityNeeded - createdQuantity;

			switch ( this.mixingMethod )
			{
			case COOK:
			case COOK_REAGENT:
			case SUPER_REAGENT:
			case COOK_PASTA:
				KoLCharacter.setChef( false );
				this.shouldRerun = this.quantityNeeded > 0;
				break;

			case MIX:
			case MIX_SPECIAL:
			case MIX_SUPER:
				KoLCharacter.setBartender( false );
				this.shouldRerun = this.quantityNeeded > 0;
				break;
			}
		}
	}

	private boolean autoRepairBoxServant()
	{
		if ( KoLmafia.refusesContinue() )
			return false;

		// If we are not cooking or mixing, or if we already have the
		// appropriate servant installed, we don't need to repair

		switch ( this.mixingMethod )
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

			return KoLCharacter.inMuscleSign() ||
				AdventureDatabase.retrieveItem( ConcoctionsDatabase.HAMMER );

		case SMITH_WEAPON:
		case SMITH_ARMOR:

			return AdventureDatabase.retrieveItem( ConcoctionsDatabase.HAMMER );

		default:
			return true;
		}

		boolean autoRepairSuccessful = false;

		// If they do want to auto-repair, make sure that
		// the appropriate item is available in their inventory

		switch ( this.mixingMethod )
		{
		case COOK:
		case COOK_REAGENT:
		case SUPER_REAGENT:
		case COOK_PASTA:
			autoRepairSuccessful = this.useBoxServant( CHEF, CLOCKWORK_CHEF, OVEN, CHEF_SKULL, CHEF_SKULL_BOX );
			break;

		case MIX:
		case MIX_SPECIAL:
		case MIX_SUPER:
			autoRepairSuccessful = this.useBoxServant( BARTENDER, CLOCKWORK_BARTENDER, KIT, BARTENDER_SKULL, BARTENDER_SKULL_BOX );
			break;
		}

		if ( !autoRepairSuccessful )
			KoLmafia.updateDisplay( ERROR_STATE, "Auto-repair was unsuccessful." );

		return autoRepairSuccessful && KoLmafia.permitsContinue();
	}

	private boolean useBoxServant( AdventureResult servant, AdventureResult clockworkServant, AdventureResult noServantItem, AdventureResult skullItem, AdventureResult boxedItem )
	{
		if ( !KoLSettings.getBooleanProperty( "autoRepairBoxServants" ) )
			return !KoLSettings.getBooleanProperty( "requireBoxServants" ) && AdventureDatabase.retrieveItem( noServantItem );

		// First, check to see if a box servant is available
		// for usage, either normally, or through some form
		// of creation.

		AdventureResult usedServant = null;

		if ( KoLCharacter.hasItem( clockworkServant, false ) )
			usedServant = clockworkServant;

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

			if ( KoLCharacter.canInteract() && KoLSettings.getBooleanProperty( "autoSatisfyWithMall" ) )
				usedServant = servant;

			if ( usedServant == null )
				return !KoLSettings.getBooleanProperty( "requireBoxServants" ) && AdventureDatabase.retrieveItem( noServantItem );
		}

		// Once you hit this point, you're guaranteed to
		// have the servant in your inventory, so attempt
		// to repair the box servant.

		(new ConsumeItemRequest( usedServant )).run();
		return servant == CHEF ? KoLCharacter.hasChef() : KoLCharacter.hasBartender();
	}

	public boolean makeIngredients()
	{
		KoLmafia.updateDisplay( "Verifying ingredients for " + this.name + "..." );

		boolean foundAllIngredients = true;

		// If this is a combining request, you will need to make
		// paste as well.  Make an even multiple, if none is left.

		if ( this.mixingMethod == COMBINE && !KoLCharacter.inMuscleSign() )
		{
			int pasteNeeded = ConcoctionsDatabase.getMeatPasteRequired( this.itemId, this.quantityNeeded );
			AdventureResult paste = new AdventureResult( MEAT_PASTE, pasteNeeded );
			foundAllIngredients &= AdventureDatabase.retrieveItem( paste );
		}

 		AdventureResult [] ingredients = ConcoctionsDatabase.getIngredients( this.itemId );

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

			foundAllIngredients &= AdventureDatabase.retrieveItem( ingredients[i].getInstance( this.quantityNeeded * multiplier ) );
		}

		return foundAllIngredients;
	}

	/**
	 * Returns the item Id for the item created by this request.
	 * @return	The item Id of the item being created
	 */

	public int getItemId()
	{	return this.itemId;
	}

	/**
	 * Returns the name of the item created by this request.
	 * @return	The name of the item being created
	 */

	public String getName()
	{	return TradeableItemDatabase.getItemName( this.itemId );
	}

	/**
	 * Returns the quantity of items to be created by this request
	 * if it were to run right now.
	 */

	public int getQuantityNeeded()
	{	return this.quantityNeeded;
	}

	/**
	 * Sets the quantity of items to be created by this request.
	 * This method is used whenever the original quantity intended
	 * by the request changes.
	 */

	public void setQuantityNeeded( int quantityNeeded )
	{	this.quantityNeeded = quantityNeeded;
	}

	/**
	 * Returns the quantity of items to be created by this request
	 * if it were to run right now.
	 */

	public int getQuantityPossible()
	{	return this.quantityPossible;
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
	{	return this.getName() + " (" + this.getQuantityPossible() + ")";
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
		switch ( this.mixingMethod )
		{
		case SMITH:
			return KoLCharacter.inMuscleSign() ? 0 : this.quantityNeeded;

		case SMITH_ARMOR:
		case SMITH_WEAPON:
			return this.quantityNeeded;

		case JEWELRY:
		case EXPENSIVE_JEWELRY:
			return 3 * this.quantityNeeded;

		case COOK:
		case COOK_REAGENT:
		case SUPER_REAGENT:
		case COOK_PASTA:
			return KoLCharacter.hasChef() ? 0 : this.quantityNeeded;

		case MIX:
		case MIX_SPECIAL:
		case MIX_SUPER:
			return KoLCharacter.hasBartender() ? 0 : this.quantityNeeded;

		case WOK:
			return this.quantityNeeded;
		}

		return 0;
	}

	public static final boolean registerRequest( boolean isExternal, String urlString )
	{
		// First, delegate subclasses, if it's a subclass request.

		if ( urlString.startsWith( "starchart.php" ) )
			return StarChartRequest.registerRequest( urlString );

		if ( urlString.startsWith( "mystic.php" ) )
			return PixelRequest.registerRequest( urlString );

		if ( urlString.indexOf( "action=makestaff" ) != -1 )
			return ChefstaffRequest.registerRequest( urlString );

		if ( urlString.indexOf( "action=makepaste" ) != -1 )
			return CombineMeatRequest.registerRequest( urlString );

		if ( urlString.startsWith( "multiuse.php" ) )
			return MultiUseRequest.registerRequest( urlString );

		// Now that we know it's not a special subclass instance,
		// all we do is parse out the ingredients which were used
		// and then print the attempt to the screen.

		int multiplier = 1;
		boolean usesTurns = false;
		boolean isCreationURL = false;

		StringBuffer command = new StringBuffer();

		if ( urlString.startsWith( "combine.php" ) || (urlString.startsWith( "knoll.php" ) && urlString.indexOf( "action=combine" ) != -1) )
		{
			isCreationURL = true;
			command.append( "Combine " );
		}
		else if ( urlString.startsWith( "cocktail.php" ) )
		{
			isCreationURL = true;
			command.append( "Mix " );
			usesTurns = !KoLCharacter.hasBartender();
		}
		else if ( urlString.startsWith( "cook.php" ) )
		{
			isCreationURL = true;
			command.append( "Cook " );
			usesTurns = !KoLCharacter.hasChef();
		}
		else if ( urlString.startsWith( "smith.php" ) || (urlString.startsWith( "knoll.php" ) && urlString.indexOf( "action=smith" ) != -1) )
		{
			isCreationURL = urlString.indexOf( "action=pulverize" ) == -1;
			command.append( "Smith " );
			usesTurns = urlString.indexOf( "action=pulverize" ) == -1;
		}
		else if ( urlString.startsWith( "jewelry.php" ) )
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
		else if ( urlString.startsWith( "gnomes.php" ) )
		{
			isCreationURL = true;
			command.append( "Tinker " );
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
			multiplier = 5;
		}

		if ( !isCreationURL )
			return false;

		Matcher itemMatcher = ITEMID_PATTERN.matcher( urlString );
		Matcher quantityMatcher = QUANTITY_PATTERN.matcher( urlString );

		boolean needsPlus = false;
		int quantity = quantityMatcher.find() ? StaticEntity.parseInt( quantityMatcher.group(1) ) : 1;

		if ( urlString.indexOf( "makemax=on" ) != -1 )
		{
			quantity = Integer.MAX_VALUE;

			while ( itemMatcher.find() )
			{
				int itemId = StaticEntity.parseInt( itemMatcher.group(1) );
				AdventureResult item = new AdventureResult( itemId, 1 );
				quantity = Math.min( item.getCount( inventory ), quantity );
			}

			itemMatcher = ITEMID_PATTERN.matcher( urlString );
		}

		quantity *= multiplier;

		if ( urlString.indexOf( "action=stillbooze" ) != -1 || urlString.indexOf( "action=stillfruit" ) != -1 )
			KoLCharacter.decrementStillsAvailable( quantity );

		while ( itemMatcher.find() )
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

		if ( urlString.startsWith( "combine.php" ) )
			StaticEntity.getClient().processResult( new AdventureResult( MEAT_PASTE, 0 - quantity ) );
		else if ( urlString.indexOf( "action=wokcook" ) != -1 )
		{
			command.append( " + " );
			command.append( quantity );
			command.append( " dry noodles" );
			StaticEntity.getClient().processResult( new AdventureResult( DRY_NOODLES, 0 - quantity ) );

			command.append( " + " );
			command.append( quantity );
			command.append( " MSG'" );
			StaticEntity.getClient().processResult( new AdventureResult( MSG, 0 - quantity ) );
		}

		if ( usesTurns )
			command.insert( 0, "[" + (KoLAdventure.getAdventureCount() + 1) + "] " );

		RequestLogger.updateSessionLog();
		RequestLogger.updateSessionLog( command.toString() );
		return true;
	}

	private static class CreationRequestArray
	{
		private ArrayList internalList = new ArrayList();

		public ItemCreationRequest get( int index )
		{
			if ( index < 0 )
				return null;

			for ( int i = this.internalList.size(); i <= index; ++i )
				this.internalList.add( constructInstance( i ) );

			return (ItemCreationRequest) this.internalList.get( index );
		}

		public void set( int index, ItemCreationRequest value )
		{
			for ( int i = this.internalList.size(); i <= index; ++i )
				this.internalList.add( constructInstance( i ) );

			this.internalList.set( index, value );
		}

		public int size()
		{	return this.internalList.size();
		}
	}
}
