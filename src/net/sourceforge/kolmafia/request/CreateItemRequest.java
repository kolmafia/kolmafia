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

package net.sourceforge.kolmafia.request;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLAdventure;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.session.InventoryManager;
import net.sourceforge.kolmafia.session.ResultProcessor;
import net.sourceforge.kolmafia.utilities.StringUtilities;

import net.sourceforge.kolmafia.persistence.ConcoctionDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.persistence.Preferences;

public class CreateItemRequest
	extends GenericRequest
	implements Comparable
{
	private static final GenericRequest REDIRECT_REQUEST = new GenericRequest( "inventory.php?action=message" );
	private static final CreationRequestArray ALL_CREATIONS = new CreationRequestArray();

	public static final Pattern ITEMID_PATTERN = Pattern.compile( "item\\d?=(\\d+)" );
	public static final Pattern QUANTITY_PATTERN = Pattern.compile( "(quantity|qty)=(\\d+)" );

	public String name;
	public AdventureResult createdItem;
	public int itemId, beforeQuantity, mixingMethod;
	public int yield;

	private int quantityNeeded, quantityPossible;

	private static final int[][] DOUGH_DATA =
	{
		// input, tool, output
		{ ItemPool.DOUGH, ItemPool.ROLLING, ItemPool.FLAT_DOUGH },
		{ ItemPool.FLAT_DOUGH, ItemPool.UNROLLING, ItemPool.DOUGH }
	};

	/**
	 * Constructs a new <code>CreateItemRequest</code> with nothing known other than the form to use. This is used
	 * by descendant classes to avoid weird type-casting problems, as it assumes that there is no known way for the item
	 * to be created.
	 *
	 * @param formSource The form to be used for the item creation
	 * @param itemId The item ID for the item to be handled
	 */

	public CreateItemRequest( final String formSource, final int itemId )
	{
		this( formSource, itemId, KoLConstants.SUBCLASS );
	}

	/**
	 * Constructs a new <code>CreateItemRequest</code> where you create the given number of items.
	 *
	 * @param formSource The form to be used for the item creation
	 * @param itemId The identifier for the item to be created
	 * @param mixingMethod How the item is created
	 * @param quantityNeeded How many of this item are needed
	 */

	private CreateItemRequest( final String formSource, final int itemId, final int mixingMethod )
	{
		super( formSource );
		this.addFormField( "pwd" );

		this.itemId = itemId;
		this.name = ItemDatabase.getItemName( itemId );
		this.yield = ConcoctionDatabase.getYield( itemId );
		this.mixingMethod = mixingMethod;
		this.createdItem = new AdventureResult( itemId, yield );
	}

	public void reconstructFields()
	{
		String formSource = "";

		switch ( this.mixingMethod )
		{
		case KoLConstants.COMBINE:
			formSource = KoLCharacter.inMuscleSign() ? "knoll.php" : "combine.php";
			break;

		case KoLConstants.MIX:
		case KoLConstants.MIX_SPECIAL:
		case KoLConstants.MIX_SUPER:
			formSource = "cocktail.php";
			break;

		case KoLConstants.COOK:
		case KoLConstants.COOK_REAGENT:
		case KoLConstants.SUPER_REAGENT:
		case KoLConstants.COOK_PASTA:
			formSource = "cook.php";
			break;

		case KoLConstants.SMITH:
			formSource = KoLCharacter.inMuscleSign() ? "knoll.php" : "smith.php";
			break;

		case KoLConstants.SMITH_ARMOR:
		case KoLConstants.SMITH_WEAPON:
			formSource = "smith.php";
			break;

		case KoLConstants.JEWELRY:
		case KoLConstants.EXPENSIVE_JEWELRY:
			formSource = "jewelry.php";
			break;

		case KoLConstants.ROLLING_PIN:
			formSource = "inv_use.php";
			break;

		case KoLConstants.CATALYST:
		case KoLConstants.CLOVER:
			formSource = "multiuse.php";
			break;

		case KoLConstants.WOK:
		case KoLConstants.MALUS:
		case KoLConstants.STILL_MIXER:
		case KoLConstants.STILL_BOOZE:
			formSource = "guild.php";
			break;

		case KoLConstants.CRIMBO07:
			formSource = "crimbo07.php";
			break;
		}

		this.constructURLString( formSource );
		this.addFormField( "pwd" );

		if ( KoLCharacter.inMuscleSign() && this.mixingMethod == KoLConstants.SMITH )
		{
			this.addFormField( "action", "smith" );
		}
		else if ( this.mixingMethod == KoLConstants.CLOVER || this.mixingMethod == KoLConstants.CATALYST )
		{
			this.addFormField( "action", "useitem" );
		}
		else if ( this.mixingMethod == KoLConstants.STILL_BOOZE )
		{
			this.addFormField( "action", "stillbooze" );
		}
		else if ( this.mixingMethod == KoLConstants.STILL_MIXER )
		{
			this.addFormField( "action", "stillfruit" );
		}
		else if ( this.mixingMethod == KoLConstants.WOK )
		{
			this.addFormField( "action", "wokcook" );
		}
		else if ( this.mixingMethod == KoLConstants.MALUS )
		{
			this.addFormField( "action", "malussmash" );
		}
		else if ( this.mixingMethod == KoLConstants.CRIMBO07 )
		{
			this.addFormField( "action", "toys" );
		}
		else if ( this.mixingMethod != KoLConstants.SUBCLASS )
		{
			this.addFormField( "action", "combine" );
		}
	}

	public static final CreateItemRequest getInstance( final AdventureResult item )
	{
		CreateItemRequest ir = CreateItemRequest.getInstance( item.getItemId(), true );
		if ( ir == null )
		{
			return null;
		}

		ir.setQuantityNeeded( item.getCount() );
		return ir;
	}

	public static final CreateItemRequest getInstance( final int itemId )
	{
		return CreateItemRequest.getInstance( itemId, true );
	}

	public static final CreateItemRequest getInstance( final int itemId, final boolean returnNullIfNotPermitted )
	{
		CreateItemRequest instance = CreateItemRequest.ALL_CREATIONS.get( itemId );

		// Assume if you have subclass that everything
		// works as intended.

		if ( instance == null || instance.mixingMethod == KoLConstants.SUBCLASS )
		{
			return instance;
		}

		// If the item creation process is not permitted,
		// then return null to indicate that it is not
		// possible to create the item.

		if ( returnNullIfNotPermitted )
		{
			if ( !ConcoctionDatabase.isPermittedMethod( instance.mixingMethod ) )
			{
				return null;
			}

			if ( ItemDatabase.getConsumptionType( itemId ) == KoLConstants.EQUIP_SHIRT && !KoLCharacter.hasSkill( "Torso Awaregness" ) )
			{
				return null;
			}
		}

		return instance;
	}

	public static final CreateItemRequest constructInstance( final int itemId )
	{
		if ( itemId == ItemPool.MEAT_PASTE || itemId == ItemPool.MEAT_STACK || itemId == ItemPool.DENSE_STACK )
		{
			return new CombineMeatRequest( itemId );
		}

		int mixingMethod = ConcoctionDatabase.getMixingMethod( itemId );

		// Otherwise, return the appropriate subclass of
		// item which will be created.

		switch ( mixingMethod )
		{
		case KoLConstants.COMBINE:
			return new CreateItemRequest(
				KoLCharacter.inMuscleSign() ? "knoll.php" : "combine.php", itemId, mixingMethod );

		case KoLConstants.MIX:
		case KoLConstants.MIX_SPECIAL:
		case KoLConstants.MIX_SUPER:
			return new CreateItemRequest( "cocktail.php", itemId, mixingMethod );

		case KoLConstants.COOK:
		case KoLConstants.COOK_REAGENT:
		case KoLConstants.SUPER_REAGENT:
		case KoLConstants.COOK_PASTA:
			return new CreateItemRequest( "cook.php", itemId, mixingMethod );

		case KoLConstants.SMITH:
			return new CreateItemRequest(
				KoLCharacter.inMuscleSign() ? "knoll.php" : "smith.php", itemId, mixingMethod );

		case KoLConstants.SMITH_ARMOR:
		case KoLConstants.SMITH_WEAPON:
			return new CreateItemRequest( "smith.php", itemId, mixingMethod );

		case KoLConstants.JEWELRY:
		case KoLConstants.EXPENSIVE_JEWELRY:
			return new CreateItemRequest( "jewelry.php", itemId, mixingMethod );

		case KoLConstants.ROLLING_PIN:
			return new CreateItemRequest( "inv_use.php", itemId, mixingMethod );

		case KoLConstants.STARCHART:
			return new StarChartRequest( itemId );

		case KoLConstants.PIXEL:
			return new PixelRequest( itemId );

		case KoLConstants.GNOME_TINKER:
			return new GnomeTinkerRequest( itemId );

		case KoLConstants.CRIMBO05:
			return new Crimbo05Request( itemId );

		case KoLConstants.CATALYST:
		case KoLConstants.CLOVER:
			return new CreateItemRequest( "multiuse.php", itemId, mixingMethod );

		case KoLConstants.WOK:
		case KoLConstants.MALUS:
		case KoLConstants.STILL_MIXER:
		case KoLConstants.STILL_BOOZE:
			return new CreateItemRequest( "guild.php", itemId, mixingMethod );

		case KoLConstants.CRIMBO06:
			return new Crimbo06Request( itemId );

		case KoLConstants.STAFF:
			return new ChefStaffRequest( itemId );

		case KoLConstants.MULTI_USE:
			return new MultiUseRequest( itemId );

		case KoLConstants.SINGLE_USE:
			return new SingleUseRequest( itemId );

		case KoLConstants.CRIMBO07:
			return new Crimbo07Request( itemId );

		default:
			return null;
		}
	}

	public boolean equals( final Object o )
	{
		return o != null && o instanceof CreateItemRequest && this.itemId == ( (CreateItemRequest) o ).itemId;
	}

	public int compareTo( final Object o )
	{
		return o == null ? -1 : this.getName().compareToIgnoreCase( ( (CreateItemRequest) o ).getName() );
	}

	/**
	 * Runs the item creation request. Note that if another item needs to be created for the request to succeed, this
	 * method will fail.
	 */

	public void run()
	{
		if ( !KoLmafia.permitsContinue() || this.quantityNeeded <= 0 )
		{
			return;
		}

		// Validate the ingredients once for the item
		// creation process.

		if ( this.mixingMethod != KoLConstants.SUBCLASS && this.mixingMethod != KoLConstants.ROLLING_PIN )
		{
			if ( !this.makeIngredients() )
			{
				return;
			}
		}

		int createdQuantity = 0;

		do
		{
			this.reconstructFields();
			this.beforeQuantity = this.createdItem.getCount( KoLConstants.inventory );

			switch ( this.mixingMethod )
			{
			case KoLConstants.SUBCLASS:

				super.run();
				if ( this.responseCode == 302 && this.redirectLocation.startsWith( "inventory" ) )
				{
					CreateItemRequest.REDIRECT_REQUEST.constructURLString( this.redirectLocation ).run();
				}
				break;

			case KoLConstants.ROLLING_PIN:

				this.makeDough();
				break;

			default:

				this.combineItems();
				break;
			}

			// After each iteration, determine how many were
			// successfully made, and rerun if you're still
			// short and continuation is possible.

			createdQuantity = this.createdItem.getCount( KoLConstants.inventory ) - this.beforeQuantity;

			// If we failed to make any at all, it's pointless to
			// try again.

			if ( createdQuantity == 0 )
			{
				// If the subclass didn't detect the failure,
				// do so here.

				if ( KoLmafia.permitsContinue() )
				{
					KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "Creation failed" );
				}

				// Stop iteration, regardless
				break;
			}

			this.quantityNeeded -= createdQuantity;
		}
		while ( this.quantityNeeded > 0 && KoLmafia.permitsContinue() );
	}

	public void makeDough()
	{
		int input = -1;
		int tool = -1;
		int output = -1;

		// Find the array row and load the
		// correct tool/input/output data.

		for ( int i = 0; i < CreateItemRequest.DOUGH_DATA.length; ++i )
		{
			output = CreateItemRequest.DOUGH_DATA[ i ][ 2 ];
			if ( this.itemId == output )
			{
				tool = CreateItemRequest.DOUGH_DATA[ i ][ 1 ];
				input = CreateItemRequest.DOUGH_DATA[ i ][ 0 ];
				break;
			}
		}

		if ( tool == -1 )
		{
			KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "Can't deduce correct tool to use." );
			return;
		}

		if ( !InventoryManager.retrieveItem( input, this.quantityNeeded ) )
		{
			return;
		}

		// If we don't have the correct tool, and the
		// person wishes to create more than 10 dough,
		// then notify the person that they should
		// purchase a tool before continuing.

		if ( ( this.quantityNeeded >= 10 || InventoryManager.hasItem( tool ) ) && !InventoryManager.retrieveItem( tool ) )
		{
			KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "Please purchase a " + ItemDatabase.getItemName( tool ) + " first." );
			return;
		}

		// If we have the correct tool, use it to
		// create the needed dough type.

		if ( InventoryManager.getCount( tool ) > 0 )
		{
			KoLmafia.updateDisplay( "Using " + ItemDatabase.getItemName( tool ) + "..." );
			new UseItemRequest( ItemPool.get( tool, 1 ) ).run();
			return;
		}

		// Without the right tool, we must manipulate
		// the dough by hand.

		String name = ItemDatabase.getItemName( output );
		UseItemRequest request = new UseItemRequest( ItemPool.get( input, 1 ) );

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
		AdventureResult[] ingredients = ConcoctionDatabase.getIngredients( this.itemId );

		if ( ingredients.length == 1 || this.mixingMethod == KoLConstants.CATALYST || this.mixingMethod == KoLConstants.WOK )
		{
			if ( this.getAdventuresUsed() > KoLCharacter.getAdventuresLeft() )
			{
				KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "Ran out of adventures." );
				return;
			}

			// If there is only one ingredient, then it probably
			// only needs a "whichitem" field added to the request.

			this.addFormField( "whichitem", String.valueOf( ingredients[ 0 ].getItemId() ) );
		}
		else
		{
			// Check to make sure that the box servant is available
			// for this combine step.  This is extra overhead when
			// no box servant is needed, but for easy readability,
			// the test always occurs.

			if ( !this.autoRepairBoxServant() )
			{
				return;
			}

			for ( int i = 0; i < ingredients.length; ++i )
			{
				this.addFormField( "item" + ( i + 1 ), String.valueOf( ingredients[ i ].getItemId() ) );
			}
		}

		int quantity = ( this.quantityNeeded + this.yield - 1 ) / this.yield;
		this.addFormField( "quantity", String.valueOf( quantity ) );

		KoLmafia.updateDisplay( "Creating " + this.name + " (" + this.quantityNeeded + ")..." );
		super.run();
	}

	public void processResults()
	{
		// Figure out how many items were created

		AdventureResult createdItem = new AdventureResult( this.itemId, 0 );
		int createdQuantity = createdItem.getCount( KoLConstants.inventory ) - this.beforeQuantity;
		KoLmafia.updateDisplay( "Successfully created " + this.getName() + " (" + createdQuantity + ")" );

		// Check to see if box-servant was overworked and exploded.

		if ( this.responseText.indexOf( "Smoke" ) != -1 )
		{
			KoLmafia.updateDisplay( "Your box servant has escaped!" );

			switch ( this.mixingMethod )
			{
			case KoLConstants.COOK:
			case KoLConstants.COOK_REAGENT:
			case KoLConstants.SUPER_REAGENT:
			case KoLConstants.COOK_PASTA:
				KoLCharacter.setChef( false );
				break;

			case KoLConstants.MIX:
			case KoLConstants.MIX_SPECIAL:
			case KoLConstants.MIX_SUPER:
				KoLCharacter.setBartender( false );
				break;
			}
		}

		// Because an explosion might have occurred, the
		// quantity that has changed might not be accurate.
		// Therefore, update with the actual value.

		if ( createdQuantity >= this.quantityNeeded )
		{
			return;
		}

		int undoAmount = this.quantityNeeded - createdQuantity;
		undoAmount = ( undoAmount + this.yield - 1 ) / this.yield;

		AdventureResult[] ingredients = ConcoctionDatabase.getIngredients( this.itemId );

		for ( int i = 0; i < ingredients.length; ++i )
		{
			ResultProcessor.processItem(
				ingredients[ i ].getItemId(), undoAmount * ingredients[ i ].getCount() );
		}

		switch ( this.mixingMethod )
		{
		case KoLConstants.COMBINE:
			if ( !KoLCharacter.inMuscleSign() )
			{
				ResultProcessor.processItem( ItemPool.MEAT_PASTE, undoAmount );
			}
			break;

		case KoLConstants.SMITH:
			if ( !KoLCharacter.inMuscleSign() )
			{
				ResultProcessor.processResult( new AdventureResult( AdventureResult.ADV, 0 - undoAmount ) );
			}
			break;

		case KoLConstants.SMITH_WEAPON:
		case KoLConstants.SMITH_ARMOR:
		case KoLConstants.WOK:
			ResultProcessor.processResult( new AdventureResult( AdventureResult.ADV, 0 - undoAmount ) );
			break;

		case KoLConstants.JEWELRY:
		case KoLConstants.EXPENSIVE_JEWELRY:
			ResultProcessor.processResult( new AdventureResult( AdventureResult.ADV, 0 - 3 * undoAmount ) );
			break;

		case KoLConstants.COOK:
		case KoLConstants.COOK_REAGENT:
		case KoLConstants.SUPER_REAGENT:
		case KoLConstants.COOK_PASTA:
			if ( !KoLCharacter.hasChef() )
			{
				ResultProcessor.processResult( new AdventureResult( AdventureResult.ADV, 0 - undoAmount ) );
			}
			break;

		case KoLConstants.MIX:
		case KoLConstants.MIX_SPECIAL:
		case KoLConstants.MIX_SUPER:
			if ( !KoLCharacter.hasBartender() )
			{
				ResultProcessor.processResult( new AdventureResult( AdventureResult.ADV, 0 - undoAmount ) );
			}
			break;

		default:
			break;
		}

		// If we created none, set error state so iteration stops.
		if ( createdQuantity == 0 )
		{
			KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "Unexpected error: nothing created" );
		}
	}

	private boolean autoRepairBoxServant()
	{
		if ( KoLmafia.refusesContinue() )
		{
			return false;
		}

		// If we are not cooking or mixing, or if we already have the
		// appropriate servant installed, we don't need to repair

		switch ( this.mixingMethod )
		{
		case KoLConstants.COOK:
		case KoLConstants.COOK_REAGENT:
		case KoLConstants.SUPER_REAGENT:
		case KoLConstants.COOK_PASTA:

			if ( KoLCharacter.hasChef() )
			{
				return true;
			}
			break;

		case KoLConstants.MIX:
		case KoLConstants.MIX_SPECIAL:
		case KoLConstants.MIX_SUPER:

			if ( KoLCharacter.hasBartender() )
			{
				return true;
			}
			break;

		case KoLConstants.SMITH:

			return KoLCharacter.inMuscleSign() || InventoryManager.retrieveItem( ItemPool.TENDER_HAMMER );

		case KoLConstants.SMITH_WEAPON:
		case KoLConstants.SMITH_ARMOR:

			return InventoryManager.retrieveItem( ItemPool.TENDER_HAMMER );

		default:
			return true;
		}

		boolean autoRepairSuccessful = false;

		// If they do want to auto-repair, make sure that
		// the appropriate item is available in their inventory

		switch ( this.mixingMethod )
		{
		case KoLConstants.COOK:
		case KoLConstants.COOK_REAGENT:
		case KoLConstants.SUPER_REAGENT:
		case KoLConstants.COOK_PASTA:
			autoRepairSuccessful =
				this.useBoxServant( ItemPool.CHEF, ItemPool.CLOCKWORK_CHEF,ItemPool.BAKE_OVEN );
			break;

		case KoLConstants.MIX:
		case KoLConstants.MIX_SPECIAL:
		case KoLConstants.MIX_SUPER:
			autoRepairSuccessful =
				this.useBoxServant( ItemPool.BARTENDER, ItemPool.CLOCKWORK_BARTENDER, ItemPool.COCKTAIL_KIT );
			break;
		}

		if ( !autoRepairSuccessful )
		{
			KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "Auto-repair was unsuccessful." );
		}

		return autoRepairSuccessful && KoLmafia.permitsContinue();
	}

	private boolean retrieveNoServantItem( final int noServantItem )
	{
		return !Preferences.getBoolean( "requireBoxServants" ) && KoLCharacter.getAdventuresLeft() > 0 && InventoryManager.retrieveItem( noServantItem );
	}

	private boolean useBoxServant( final int servant, final int clockworkServant, final int noServantItem )
	{
		if ( !Preferences.getBoolean( "autoRepairBoxServants" ) )
		{
			return this.retrieveNoServantItem( noServantItem );
		}

		// First, check to see if a box servant is available
		// for usage, either normally, or through some form
		// of creation.

		int usedServant = -1;

		if ( InventoryManager.hasItem( clockworkServant, false ) )
		{
			usedServant = clockworkServant;
		}
		else if ( InventoryManager.hasItem( servant, true ) )
		{
			usedServant = servant;
		}

		if ( usedServant == -1 )
		{
			if ( KoLCharacter.canInteract() && ( Preferences.getBoolean( "autoSatisfyWithMall" ) || Preferences.getBoolean( "autoSatisfyWithStash" ) ) )
			{
				usedServant = servant;
			}
			else
			{
				return this.retrieveNoServantItem( noServantItem );
			}
		}

		// Once you hit this point, you're guaranteed to
		// have the servant in your inventory, so attempt
		// to repair the box servant.

		new UseItemRequest( ItemPool.get( usedServant, 1 ) ).run();
		return servant == ItemPool.CHEF ? KoLCharacter.hasChef() : KoLCharacter.hasBartender();
	}

	public boolean makeIngredients()
	{
		KoLmafia.updateDisplay( "Verifying ingredients for " + this.name + " (" + this.quantityNeeded + ")..." );

		boolean foundAllIngredients = true;

		// If this is a combining request, you need meat paste as well.

		if ( this.mixingMethod == KoLConstants.COMBINE && !KoLCharacter.inMuscleSign() )
		{
			int pasteNeeded = ConcoctionDatabase.getMeatPasteRequired( this.itemId, this.quantityNeeded );
			AdventureResult paste = ItemPool.get( ItemPool.MEAT_PASTE, pasteNeeded );
			foundAllIngredients &= InventoryManager.retrieveItem( paste );
		}

		AdventureResult[] ingredients = ConcoctionDatabase.getIngredients( this.itemId );
		int yield = ConcoctionDatabase.getYield( this.itemId );

		for ( int i = 0; i < ingredients.length; ++i )
		{
			// First, calculate the multiplier that's needed
			// for this ingredient to avoid not making enough
			// intermediate ingredients and getting an error.

			int multiplier = 0;
			for ( int j = 0; j < ingredients.length; ++j )
			{
				if ( ingredients[ i ].getItemId() == ingredients[ j ].getItemId() )
				{
					multiplier += ingredients[ i ].getCount();
				}
			}

			// Then, make enough of the ingredient in order
			// to proceed with the concoction.

			int quantity = this.quantityNeeded * multiplier;
			if ( yield > 1 )
			{
				quantity = ( quantity + yield - 1 ) / yield;
			}
			foundAllIngredients &= InventoryManager.retrieveItem( ingredients[ i ].getInstance( quantity ) );
		}

		return foundAllIngredients;
	}

	/**
	 * Returns the item Id for the item created by this request.
	 *
	 * @return The item Id of the item being created
	 */

	public int getItemId()
	{
		return this.itemId;
	}

	/**
	 * Returns the name of the item created by this request.
	 *
	 * @return The name of the item being created
	 */

	public String getName()
	{
		return ItemDatabase.getItemName( this.itemId );
	}

	/**
	 * Returns the quantity of items to be created by this request if it were to run right now.
	 */

	public int getQuantityNeeded()
	{
		return this.quantityNeeded;
	}

	/**
	 * Sets the quantity of items to be created by this request. This method is used whenever the original quantity
	 * intended by the request changes.
	 */

	public void setQuantityNeeded( final int quantityNeeded )
	{
		this.quantityNeeded = quantityNeeded;
	}

	/**
	 * Returns the quantity of items to be created by this request if it were to run right now.
	 */

	public int getQuantityPossible()
	{
		return this.quantityPossible;
	}

	/**
	 * Sets the quantity of items to be created by this request. This method is used whenever the original quantity
	 * intended by the request changes.
	 */

	public void setQuantityPossible( final int quantityPossible )
	{
		this.quantityPossible = quantityPossible;
	}

	/**
	 * Returns the string form of this item creation request. This displays the item name, and the amount that will be
	 * created by this request.
	 *
	 * @return The string form of this request
	 */

	public String toString()
	{
		return this.getName() + " (" + this.getQuantityPossible() + ")";
	}

	/**
	 * An alternative method to doing adventure calculation is determining how many adventures are used by the given
	 * request, and subtract them after the request is done.
	 *
	 * @return The number of adventures used by this request.
	 */

	public int getAdventuresUsed()
	{
		switch ( this.mixingMethod )
		{
		case KoLConstants.SMITH:
			return KoLCharacter.inMuscleSign() ? 0 : this.quantityNeeded;

		case KoLConstants.SMITH_ARMOR:
		case KoLConstants.SMITH_WEAPON:
			return this.quantityNeeded;

		case KoLConstants.JEWELRY:
		case KoLConstants.EXPENSIVE_JEWELRY:
			return 3 * this.quantityNeeded;

		case KoLConstants.COOK:
		case KoLConstants.COOK_REAGENT:
		case KoLConstants.SUPER_REAGENT:
		case KoLConstants.COOK_PASTA:
			return KoLCharacter.hasChef() ? 0 : this.quantityNeeded;

		case KoLConstants.MIX:
		case KoLConstants.MIX_SPECIAL:
		case KoLConstants.MIX_SUPER:
			return KoLCharacter.hasBartender() ? 0 : this.quantityNeeded;

		case KoLConstants.WOK:
			return this.quantityNeeded;
		}

		return 0;
	}

	public static final boolean registerRequest( final boolean isExternal, final String urlString )
	{
		// First, delegate subclasses, if it's a subclass request.

		if ( urlString.startsWith( "starchart.php" ) )
		{
			return StarChartRequest.registerRequest( urlString );
		}

		if ( urlString.startsWith( "mystic.php" ) )
		{
			return PixelRequest.registerRequest( urlString );
		}

		if ( urlString.startsWith( "crimbo07.php" ) )
		{
			return Crimbo07Request.registerRequest( urlString );
		}

		if ( urlString.indexOf( "action=makestaff" ) != -1 )
		{
			return ChefStaffRequest.registerRequest( urlString );
		}

		if ( urlString.indexOf( "action=makepaste" ) != -1 )
		{
			return CombineMeatRequest.registerRequest( urlString );
		}

		if ( urlString.startsWith( "inv_use.php" ) )
		{
			if ( SingleUseRequest.registerRequest( urlString ) )
			{
				return true;
			}

			String tool = "";
			String ingredient = "";

			if ( urlString.indexOf( "whichitem=873" ) != -1 )
			{
				// Rolling Pin
				tool = "rolling pin";
				ingredient = "wad of dough";
			}
			if ( urlString.indexOf( "whichitem=874" ) != -1 )
			{
				// Unrolling Pin
				tool = "unrolling pin";
				ingredient = "flat dough";
			}
			else
			{
				return false;
			}

			AdventureResult item = new AdventureResult( ingredient, 1 );
			int quantity = item.getCount( KoLConstants.inventory );
			ResultProcessor.processItem( item.getItemId(), 0 - quantity );

			RequestLogger.updateSessionLog();
			RequestLogger.updateSessionLog( "Use " + tool );

			return true;
		}

		if ( urlString.startsWith( "multiuse.php" ) )
		{
			if ( MultiUseRequest.registerRequest( urlString ) )
			{
				return true;
			}

			int quantity = 1;
			String method = "Use ";
			int item1 = -1;
			int item2 = -1;

			if ( urlString.indexOf( "whichitem=24" ) != -1 )
			{
				// Ten-leaf clover
				item1 = ItemPool.TEN_LEAF_CLOVER;
			}
			else if ( urlString.indexOf( "whichitem=196" ) != -1 )
			{
				// Disassembled clover
				item1 = ItemPool.DISASSEMBLED_CLOVER;
			}
			else if ( urlString.indexOf( "whichitem=1605" ) != -1 )
			{
				// Delectable Catalyst
				method = "Mix ";
				item1 = ItemPool.CATALYST;
				item2 = ItemPool.REAGENT;
			}
			else
			{
				return false;
			}

			Matcher quantityMatcher = CreateItemRequest.QUANTITY_PATTERN.matcher( urlString );
			if ( quantityMatcher.find() )
			{
				quantity = StringUtilities.parseInt( quantityMatcher.group( 2 ) );
			}

			StringBuffer command = new StringBuffer();

			command.append( method );
			command.append( quantity );
			command.append( " " );
			command.append( ItemDatabase.getItemName( item1 ) );
			ResultProcessor.processItem( item1, 0 - quantity );

			if ( item2 != -1 )
			{
				command.append( " + " );
				command.append( ItemDatabase.getItemName( item2 ) );
				ResultProcessor.processItem( item2, 0 - quantity );
			}

			RequestLogger.updateSessionLog();
			RequestLogger.updateSessionLog( command.toString() );

			return true;
		}

		// Now that we know it's not a special subclass instance,
		// all we do is parse out the ingredients which were used
		// and then print the attempt to the screen.

		int multiplier = 1;
		boolean usesTurns = false;
		boolean isCreationURL = false;

		StringBuffer command = new StringBuffer();

		if ( urlString.startsWith( "combine.php" ) || urlString.startsWith( "knoll.php" ) && urlString.indexOf( "action=combine" ) != -1 )
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
		else if ( urlString.startsWith( "smith.php" ) || urlString.startsWith( "knoll.php" ) && urlString.indexOf( "action=smith" ) != -1 )
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
		{
			return false;
		}

		Matcher itemMatcher = CreateItemRequest.ITEMID_PATTERN.matcher( urlString );
		Matcher quantityMatcher = CreateItemRequest.QUANTITY_PATTERN.matcher( urlString );

		boolean needsPlus = false;
		int quantity = quantityMatcher.find() ? StringUtilities.parseInt( quantityMatcher.group( 2 ) ) : 1;

		if ( urlString.indexOf( "makemax=on" ) != -1 )
		{
			quantity = Integer.MAX_VALUE;

			while ( itemMatcher.find() )
			{
				int itemId = StringUtilities.parseInt( itemMatcher.group( 1 ) );
				AdventureResult item = new AdventureResult( itemId, 1 );
				quantity = Math.min( item.getCount( KoLConstants.inventory ), quantity );
			}

			itemMatcher = CreateItemRequest.ITEMID_PATTERN.matcher( urlString );
		}

		quantity *= multiplier;

		if ( urlString.indexOf( "action=stillbooze" ) != -1 || urlString.indexOf( "action=stillfruit" ) != -1 )
		{
			KoLCharacter.decrementStillsAvailable( quantity );
		}

		while ( itemMatcher.find() )
		{
			if ( needsPlus )
			{
				command.append( " + " );
			}

			int itemId = StringUtilities.parseInt( itemMatcher.group( 1 ) );
			String name = ItemDatabase.getItemName( itemId );

			if ( name == null )
			{
				continue;
			}

			command.append( quantity );
			command.append( ' ' );
			command.append( name );

			ResultProcessor.processItem( itemId, 0 - quantity );
			needsPlus = true;
		}

		if ( urlString.startsWith( "combine.php" ) )
		{
			ResultProcessor.processItem( ItemPool.MEAT_PASTE, 0 - quantity );
		}
		else if ( urlString.indexOf( "action=wokcook" ) != -1 )
		{
			command.append( " + " );
			command.append( quantity );
			command.append( " dry noodles" );
			ResultProcessor.processItem( ItemPool.DRY_NOODLES, 0 - quantity );

			command.append( " + " );
			command.append( quantity );
			command.append( " MSG" );
			ResultProcessor.processItem( ItemPool.MSG, 0 - quantity );
		}

		if ( usesTurns )
		{
			command.insert( 0, "[" + ( KoLAdventure.getAdventureCount() + 1 ) + "] " );
		}

		RequestLogger.updateSessionLog();
		RequestLogger.updateSessionLog( command.toString() );
		return true;
	}

	private static class CreationRequestArray
	{
		private final ArrayList internalList = new ArrayList();

		public CreateItemRequest get( final int index )
		{
			if ( index < 0 )
			{
				return null;
			}

			for ( int i = this.internalList.size(); i <= index; ++i )
			{
				this.internalList.add( CreateItemRequest.constructInstance( i ) );
			}

			return (CreateItemRequest) this.internalList.get( index );
		}

		public void set( final int index, final CreateItemRequest value )
		{
			for ( int i = this.internalList.size(); i <= index; ++i )
			{
				this.internalList.add( CreateItemRequest.constructInstance( i ) );
			}

			this.internalList.set( index, value );
		}

		public int size()
		{
			return this.internalList.size();
		}
	}
}
