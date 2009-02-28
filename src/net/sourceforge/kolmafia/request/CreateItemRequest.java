/**
 * Copyright (c) 2005-2009, KoLmafia development team
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
import java.util.List;
import java.util.TreeMap;

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
	private static final CreationRequestMap ALL_CREATIONS = new CreationRequestMap();

	public static final Pattern ITEMID_PATTERN = Pattern.compile( "item\\d?=(\\d+)" );
	public static final Pattern WHICHITEM_PATTERN = Pattern.compile( "whichitem=(\\d+)" );
	public static final Pattern QUANTITY_PATTERN = Pattern.compile( "(quantity|qty)=(\\d+)" );

	public static final Pattern TARGET_PATTERN = Pattern.compile( "target=(\\d+)" );
	public static final Pattern MODE_PATTERN = Pattern.compile( "mode=([^&]+)" );
	public static final Pattern CRAFT_PATTERN_1 = Pattern.compile( "[\\&\\?](?:a|b)=(\\d+)" );
	public static final Pattern CRAFT_PATTERN_2 = Pattern.compile( "steps\\[\\]=(\\d+),(\\d+)" );
	
	public static final Pattern CRAFT_COMMENT_PATTERN =
		Pattern.compile( "<!-- ?cr:(\\d+)x(\\d+),(\\d+)=(\\d+) ?-->" );
	// 1=quantity, 2,3=items used, 4=result (redundant)

	public AdventureResult createdItem;

	private String name;
	private int itemId, mixingMethod;

	private int beforeQuantity;
	private int yield;

	private int quantityNeeded, quantityPossible, quantityPullable;

	private static final int[][] DOUGH_DATA =
	{
		// input, tool, output
		{ ItemPool.DOUGH, ItemPool.ROLLING_PIN, ItemPool.FLAT_DOUGH },
		{ ItemPool.FLAT_DOUGH, ItemPool.UNROLLING_PIN, ItemPool.DOUGH }
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
		super( formSource );

		this.itemId = itemId;
		this.name = ItemDatabase.getItemName( itemId );
		this.mixingMethod = KoLConstants.SUBCLASS;
		this.calculateYield();
	}

	public CreateItemRequest( final String formSource, final String name )
	{
		super( formSource );

		this.itemId = -1;
		this.name = name;
		this.mixingMethod = KoLConstants.SUBCLASS;
		this.yield = 1;
		this.createdItem = AdventureResult.tallyItem( name );
	}

	private CreateItemRequest( final int itemId )
	{
		super( "" );

		this.itemId = itemId;
		this.name = ItemDatabase.getItemName( itemId );
		this.mixingMethod = ConcoctionDatabase.getMixingMethod( this.itemId );
		this.calculateYield();
	}

	private void calculateYield()
	{
		this.yield = ConcoctionDatabase.getYield( this.itemId );
		this.createdItem = new AdventureResult( this.itemId, this.yield );
	}

	public void reconstructFields()
	{
		String formSource = "craft.php";
		String action = "craft";
		String mode = null;

		if ( KoLCharacter.inMuscleSign() )
		{
			if ( this.mixingMethod == KoLConstants.COMBINE )
			{
				formSource = "knoll.php";
				action = "combine";
			}
			else if ( this.mixingMethod == KoLConstants.SMITH )
			{
				formSource = "knoll.php";
				action = "smith";
			}
		}

		if ( formSource.equals( "craft.php" ) )
		{
			switch ( this.mixingMethod )
			{
			case KoLConstants.COMBINE:
				mode = "combine";
				break;

			case KoLConstants.MIX:
			case KoLConstants.MIX_SPECIAL:
			case KoLConstants.MIX_SUPER:
			case KoLConstants.MIX_SALACIOUS:
				mode = "cocktail";
				break;

			case KoLConstants.COOK:
			case KoLConstants.COOK_REAGENT:
			case KoLConstants.SUPER_REAGENT:
			case KoLConstants.DEEP_SAUCE:
			case KoLConstants.COOK_PASTA:
			case KoLConstants.COOK_TEMPURA:
				mode = "cook";
				break;

			case KoLConstants.SMITH:
			case KoLConstants.SMITH_ARMOR:
			case KoLConstants.SMITH_WEAPON:
				mode = "smith";
				break;

			case KoLConstants.JEWELRY:
			case KoLConstants.EXPENSIVE_JEWELRY:
				mode = "jewelry";
				break;

			case KoLConstants.ROLLING_PIN:
				formSource = "inv_use.php";
				break;

			case KoLConstants.WOK:
				formSource = "guild.php";
				action = "wokcook";
				break;

			case KoLConstants.MALUS:
				formSource = "guild.php";
				action = "malussmash";
				break;

			case KoLConstants.STILL_MIXER:
				formSource = "guild.php";
				action = "stillfruit";
				break;

			case KoLConstants.STILL_BOOZE:
				formSource = "guild.php";
				action = "stillbooze";
				break;

			case KoLConstants.CRIMBO07:
				formSource = "crimbo07.php";
				action = "toys";
				break;
			}
		}

		this.constructURLString( formSource );
		this.addFormField( "action", action );

		if ( mode != null )
		{
			this.addFormField( "mode", mode );
			this.addFormField( "ajax", "1" );
		}
	}

	public static final CreateItemRequest getInstance( final int itemId )
	{
		return CreateItemRequest.getInstance( ItemPool.get( itemId, 1 ), true );
	}

	public static final CreateItemRequest getInstance( final AdventureResult item )
	{
		return CreateItemRequest.getInstance( item, true );
	}

	public static final CreateItemRequest getInstance( final AdventureResult item, final boolean returnNullIfNotPermitted )
	{
		CreateItemRequest instance = CreateItemRequest.ALL_CREATIONS.get( item.getName() );

		if ( instance == null )
		{
			return instance;
		}

		if ( instance instanceof CombineMeatRequest )
		{
			return instance;
		}

		// If the item creation process is not permitted, then return
		// null to indicate that it is not possible to create the item.

		if ( returnNullIfNotPermitted &&
		     !ConcoctionDatabase.isPermittedMethod( ConcoctionDatabase.getMixingMethod( item ) ) )
		{
			return null;
		}

		return instance;
	}

	public static final CreateItemRequest constructInstance( final String name )
	{
		AdventureResult item = AdventureResult.tallyItem( name );
		int itemId = item.getItemId();

		if ( itemId == ItemPool.MEAT_PASTE || itemId == ItemPool.MEAT_STACK || itemId == ItemPool.DENSE_STACK )
		{
			return new CombineMeatRequest( itemId );
		}

		int mixingMethod = ConcoctionDatabase.getMixingMethod( item );

		// Otherwise, return the appropriate subclass of
		// item which will be created.

		switch ( mixingMethod )
		{
		case KoLConstants.NOCREATE:
			return null;

		case KoLConstants.STARCHART:
			return new StarChartRequest( itemId );

		case KoLConstants.PIXEL:
			return new PixelRequest( itemId );

		case KoLConstants.GNOME_TINKER:
			return new GnomeTinkerRequest( itemId );

		case KoLConstants.STAFF:
			return new ChefStaffRequest( itemId );

		case KoLConstants.SUSHI:
			return new SushiRequest( name );

		case KoLConstants.SINGLE_USE:
			return new SingleUseRequest( itemId );

		case KoLConstants.MULTI_USE:
			return new MultiUseRequest( itemId );

		case KoLConstants.CRIMBO05:
			return new Crimbo05Request( itemId );

		case KoLConstants.CRIMBO06:
			return new Crimbo06Request( itemId );

		case KoLConstants.CRIMBO07:
			return new Crimbo07Request( itemId );

		default:
			return new CreateItemRequest( itemId );
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

		if ( this.mixingMethod != KoLConstants.SUBCLASS &&
		     this.mixingMethod != KoLConstants.ROLLING_PIN &&
		     !this.makeIngredients() )
		{
			return;
		}

		int createdQuantity = 0;

		do
		{
			if ( !this.autoRepairBoxServant() )
			{
				KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "Auto-repair was unsuccessful." );
				return;
			}

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

			// Certain creations are used immediately.

			if ( this.noCreation() )
			{
				break;
			}

			// Figure out how many items were created

			createdQuantity = this.createdItem.getCount( KoLConstants.inventory ) - this.beforeQuantity;

			// If we created none, set error state so iteration stops.

			if ( createdQuantity == 0 )
			{
				// If the subclass didn't detect the failure,
				// do so here.

				if ( KoLmafia.permitsContinue() )
				{
					KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "Creation failed, no results detected." );
				}

				return;
			}

			KoLmafia.updateDisplay( "Successfully created " + this.getName() + " (" + createdQuantity + ")" );
			this.quantityNeeded -= createdQuantity;
		}
		while ( this.quantityNeeded > 0 && KoLmafia.permitsContinue() );
	}

	public boolean noCreation()
	{
		return false;
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

		// If we don't have the correct tool, and the person wishes to
		// create more than 10 dough, then notify the person that they
		// should purchase a tool before continuing.

		if ( ( this.quantityNeeded >= 10 || InventoryManager.hasItem( tool ) ) && !InventoryManager.retrieveItem( tool ) )
		{
			KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "Please purchase a " + ItemDatabase.getItemName( tool ) + " first." );
			return;
		}

		// If we have the correct tool, use it to
		// create the needed dough type.

		if ( InventoryManager.hasItem( tool ) )
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
		String path = this.getPath();
		String quantityField = "quantity";

		AdventureResult[] ingredients = ConcoctionDatabase.getIngredients( this.name );

		if ( ingredients.length == 1 || this.mixingMethod == KoLConstants.WOK )
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
		else if ( path.equals( "craft.php" ) )
		{
			quantityField = "qty";

			this.addFormField( "a", String.valueOf( ingredients[ 0 ].getItemId() ) );
			this.addFormField( "b", String.valueOf( ingredients[ 1 ].getItemId() ) );
		}
		else
		{
			for ( int i = 0; i < ingredients.length; ++i )
			{
				this.addFormField( "item" + ( i + 1 ), String.valueOf( ingredients[ i ].getItemId() ) );
			}
		}

		this.calculateYield();
		int quantity = ( this.quantityNeeded + this.yield - 1 ) / this.yield;
		this.addFormField( quantityField, String.valueOf( quantity ) );

		KoLmafia.updateDisplay( "Creating " + this.name + " (" + this.quantityNeeded + ")..." );
		super.run();
	}

	public void processResults()
	{
		if ( CreateItemRequest.parseGuildCreation( this.getURLString(), this.responseText ) )
		{
			return;
		}

		CreateItemRequest.parseCrafting( this.getURLString(), this.responseText );

		// Check to see if box-servant was overworked and exploded.

		if ( this.responseText.indexOf( "Smoke" ) != -1 )
		{
			KoLmafia.updateDisplay( "Your box servant has escaped!" );
		}
	}

	public static void parseCrafting( final String location, final String responseText )
	{
		if ( !location.startsWith( "craft.php" ) )
		{
			return;
		}

		Matcher m = MODE_PATTERN.matcher( location );
		String mode = m.find() ? m.group(1) : "";
		boolean paste = mode.equals( "combine" ) && !KoLCharacter.inMuscleSign();

		m = CRAFT_COMMENT_PATTERN.matcher( responseText );
		while ( m.find() )
		{
			int qty = StringUtilities.parseInt( m.group( 1 ) );
			int item1 = StringUtilities.parseInt( m.group( 2 ) );
			int item2 = StringUtilities.parseInt( m.group( 3 ) );
			ResultProcessor.processItem( item1, -qty );
			ResultProcessor.processItem( item2, -qty );
			if ( paste )
			{
				ResultProcessor.processItem( ItemPool.MEAT_PASTE, -qty );
			}
			RequestLogger.updateSessionLog( "Crafting used " + qty + " each of " +
				ItemDatabase.getItemName( item1 ) + " and " + ItemDatabase.getItemName( item2 ) );		
		}

		if ( responseText.indexOf( "Smoke" ) != -1 )
		{
			String servant = "servant";
			if ( mode.equals( "cook" ) )
			{
				servant = "chef";
				KoLCharacter.setChef( false );
			}
			else if ( mode.equals( "cocktail" ) )
			{
				servant = "bartender";
				KoLCharacter.setBartender( false );
			}
			RequestLogger.updateSessionLog( "Your " + servant + " blew up" );
		}
	}

	public static boolean parseGuildCreation( final String urlString, final String responseText )
	{
		if ( !urlString.startsWith( "guild.php" ) )
		{
			return false;
		}

		// If nothing was created, don't deal with ingredients

		if ( responseText.indexOf( "You acquire" ) == -1 )
		{
			return true;
		}

		int multiplier = 1;
		boolean stills = false;

		// Using the Still decrements available daily uses
		if ( urlString.indexOf( "action=stillbooze" ) != -1 || urlString.indexOf( "action=stillfruit" ) != -1 )
		{
			stills = true;
		}

		// Using the Malus uses 5 ingredients at a time
		else if ( urlString.indexOf( "action=malussmash" ) != -1 )
		{
			multiplier = 5;
		}

		// The only other guild creation uses the Wok
		else if ( urlString.indexOf( "action=wokcook" ) == -1 )
		{
			return true;
		}

		AdventureResult [] ingredients = CreateItemRequest.findIngredients( urlString );
		int quantity = CreateItemRequest.getQuantity( urlString, ingredients ) * multiplier;

		for ( int i = 0; i < ingredients.length; ++i )
		{
			AdventureResult item = ingredients[i];
			ResultProcessor.processItem( item.getItemId(), -quantity );
		}

		if ( stills )
		{
			KoLCharacter.decrementStillsAvailable( quantity );
		}

		return true;
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
		case KoLConstants.MIX_SALACIOUS:

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
		case KoLConstants.MIX_SALACIOUS:
			autoRepairSuccessful =
				this.useBoxServant( ItemPool.BARTENDER, ItemPool.CLOCKWORK_BARTENDER, ItemPool.COCKTAIL_KIT );
			break;
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

			if ( !InventoryManager.retrieveItem( paste ) )
			{
				foundAllIngredients = false;
			}
		}

		AdventureResult[] ingredients = ConcoctionDatabase.getIngredients( this.name );
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

			if ( !InventoryManager.retrieveItem( ingredients[ i ].getItemId(), quantity ) )
			{
				foundAllIngredients = false;
			}
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
		return this.name;
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
	 * Returns the quantity of items that could be created with available ingredients.
	 */

	public int getQuantityPossible()
	{
		return this.quantityPossible;
	}

	/**
	 * Sets the quantity of items that could be created.  This is set by
	 * refreshConcoctions.
	 */

	public void setQuantityPossible( final int quantityPossible )
	{
		this.quantityPossible = quantityPossible;
	}

	/**
	 * Returns the quantity of items that could be pulled with the current budget.
	 */

	public int getQuantityPullable()
	{
		return this.quantityPullable;
	}

	/**
	 * Sets the quantity of items that could be pulled.  This is set by
	 * refreshConcoctions.
	 */

	public void setQuantityPullable( final int quantityPullable )
	{
		this.quantityPullable = quantityPullable;
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
		case KoLConstants.DEEP_SAUCE:
		case KoLConstants.COOK_PASTA:
		case KoLConstants.COOK_TEMPURA:
			return KoLCharacter.hasChef() ? 0 : this.quantityNeeded;

		case KoLConstants.MIX:
		case KoLConstants.MIX_SPECIAL:
		case KoLConstants.MIX_SUPER:
		case KoLConstants.MIX_SALACIOUS:
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

		if ( urlString.startsWith( "sushi.php" ) )
		{
			return SushiRequest.registerRequest( urlString );
		}

		if ( urlString.startsWith( "crimbo07.php" ) )
		{
			return Crimbo07Request.registerRequest( urlString );
		}

		if ( urlString.indexOf( "action=makestaff" ) != -1 )
		{
			return ChefStaffRequest.registerRequest( urlString );
		}

		if ( urlString.indexOf( "action=makepaste" ) != -1 || urlString.indexOf( "action=makestuff" ) != -1 )
		{
			return CombineMeatRequest.registerRequest( urlString );
		}

		if ( urlString.startsWith( "inv_use.php" ) )
		{
			if ( SingleUseRequest.registerRequest( urlString ) )
			{
				return true;
			}

			Matcher whichMatcher = CreateItemRequest.WHICHITEM_PATTERN.matcher( urlString );
			if ( !whichMatcher.find() )
			{
				return false;
			}

			int whichitem = StringUtilities.parseInt( whichMatcher.group( 1 ) );

			String tool = "";
			String ingredient = "";

			switch ( whichitem )
			{
			case ItemPool.ROLLING_PIN:
				tool = "rolling pin";
				ingredient = "wad of dough";
				break;
			case ItemPool.UNROLLING_PIN:
				tool = "unrolling pin";
				ingredient = "flat dough";
				break;
			default:
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
			if ( SingleUseRequest.registerRequest( urlString ) )
			{
				return true;
			}

			if ( MultiUseRequest.registerRequest( urlString ) )
			{
				return true;
			}

			return false;
		}

		// Now that we know it's not a special subclass instance,
		// all we do is parse out the ingredients which were used
		// and then print the attempt to the screen.

		int multiplier = 1;
		boolean usesTurns = false;
		boolean isCreationURL = false;

		StringBuffer command = new StringBuffer();

		if ( urlString.startsWith( "craft.php" ) )
		{
			if ( urlString.indexOf( "action=pulverize" ) != -1 )
			{
				return false;
			}
			else if ( urlString.indexOf( "action=craft" ) == -1 )
			{
				return true;
			}
			else if ( urlString.indexOf( "mode=combine" ) != -1 )
			{
				isCreationURL = true;
				command.append( "Combine " );
			}
			else if ( urlString.indexOf( "mode=cocktail" ) != -1 )
			{
				isCreationURL = true;
				command.append( "Mix " );
				usesTurns = !KoLCharacter.hasBartender();
			}
			else if ( urlString.indexOf( "mode=cook" ) != -1 )
			{
				isCreationURL = true;
				command.append( "Cook " );
				usesTurns = !KoLCharacter.hasChef();
			}
			else if ( urlString.indexOf( "mode=smith" ) != -1 )
			{
				isCreationURL = true;
				command.append( "Smith " );
				usesTurns = true;
			}
			else if ( urlString.indexOf( "mode=jewelry" ) != -1 )
			{
				isCreationURL = true;
				command.append( "Ply " );
				usesTurns = true;
			}
			else
			{
				// Take credit for all visits to crafting
				return true;
			}
		}
		else if ( urlString.startsWith( "knoll.php" ) )
		{
			if ( urlString.indexOf( "action=combine" ) != -1 )
			{
				isCreationURL = true;
				command.append( "Combine " );
			}
			else if ( urlString.indexOf( "action=smith" ) != -1 )
			{
				isCreationURL = true;
				command.append( "Smith " );
				usesTurns = true;
			}
		}
		else if ( urlString.startsWith( "guild.php" ) )
		{
			if ( urlString.indexOf( "action=stillbooze" ) != -1 || urlString.indexOf( "action=stillfruit" ) != -1 )
			{
				isCreationURL = true;
				command.append( "Distill " );
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
				multiplier = 5;
			}
		}
		else if ( urlString.startsWith( "gnomes.php" ) )
		{
			if ( urlString.indexOf( "action=tinksomething" ) != -1 )
			{
				isCreationURL = true;
				command.append( "Tinker " );
			}
		}

		if ( !isCreationURL )
		{
			return false;
		}

		AdventureResult [] ingredients = CreateItemRequest.findIngredients( urlString );

		int quantity = CreateItemRequest.getQuantity( urlString, ingredients ) * multiplier;

		for ( int i = 0; i < ingredients.length; ++i )
		{
			if ( i > 0 )
			{
				command.append( " + " );
			}

			AdventureResult item = ingredients[i];

			command.append( quantity );
			command.append( ' ' );
			command.append( item.getName() );
		}

		if ( usesTurns )
		{
			command.insert( 0, "[" + ( KoLAdventure.getAdventureCount() + 1 ) + "] " );
		}

		RequestLogger.updateSessionLog();
		RequestLogger.updateSessionLog( command.toString() );

		CreateItemRequest.useIngredients( urlString, ingredients, quantity );

		return true;
	}

	private static final AdventureResult [] findIngredients( final String urlString )
	{
		if ( urlString.startsWith( "craft.php" ) && urlString.indexOf( "target" ) != -1 )
		{
			// Crafting is going to make an item from ingredients.
			// Return the ingredients we think will be used.

			Matcher targetMatcher = CreateItemRequest.TARGET_PATTERN.matcher( urlString );
			if ( !targetMatcher.find() )
			{
				return null;
			}

			int itemId = StringUtilities.parseInt( targetMatcher.group( 1 ) );
			return ConcoctionDatabase.getIngredients( itemId );
		}

		ArrayList ingredients = new ArrayList();
		Matcher matcher;

		if ( urlString.startsWith( "craft.php" ) )
		{
			matcher = CreateItemRequest.CRAFT_PATTERN_1.matcher( urlString );
			while ( matcher.find() )
			{
				ingredients.add( CreateItemRequest.getIngredient( matcher.group(1) ) );
			}
		}
		else
		{
			matcher = CreateItemRequest.ITEMID_PATTERN.matcher( urlString );
			while ( matcher.find() )
			{
				ingredients.add( CreateItemRequest.getIngredient( matcher.group(1) ) );
			}
		}

		if ( urlString.indexOf( "action=wokcook" ) != -1 )
		{
			ingredients.add( ItemPool.get( ItemPool.DRY_NOODLES, 1 ) );
			ingredients.add( ItemPool.get( ItemPool.MSG, 1 ) );
		}

		AdventureResult [] ingredientArray = new AdventureResult[ ingredients.size() ];
		ingredients.toArray( ingredientArray );

		return ingredientArray;
	}

	private static final AdventureResult getIngredient( final String itemId )
	{
		return ItemPool.get( StringUtilities.parseInt( itemId ), 1 );
	}

	private static final int getQuantity(  final String urlString, final AdventureResult [] ingredients )
	{
		if ( urlString.indexOf( "max=on" ) == -1 )
		{
			Matcher matcher = CreateItemRequest.QUANTITY_PATTERN.matcher( urlString );
			return matcher.find() ? StringUtilities.parseInt( matcher.group( 2 ) ) : 1;
		}

		int quantity = Integer.MAX_VALUE;

		for ( int i = 0; i < ingredients.length; ++i )
		{
			AdventureResult item = ingredients[i];
			quantity = Math.min( item.getCount( KoLConstants.inventory ), quantity );
		}

		return quantity;
	}

	private static final void useIngredients( final String urlString, AdventureResult [] ingredients, int quantity )
	{
		// Let crafting tell us which ingredients it used and remove
		// them from inventory after the fact.
		if ( urlString.startsWith( "craft.php" ) )
		{
			return;
		}

		// Similarly,.we deal with ingredients from guild tools later
		if ( urlString.startsWith( "guild.php" ) )
		{
			return;
		}

		// If we have no ingredients, nothing to do
		if ( ingredients == null )
		{
			return;
		}

		for ( int i = 0; i < ingredients.length; ++i )
		{
			AdventureResult item = ingredients[i];
			ResultProcessor.processItem( item.getItemId(), 0 - quantity );
		}

		if ( urlString.indexOf( "mode=combine" ) != -1 )
		{
			ResultProcessor.processItem( ItemPool.MEAT_PASTE, 0 - quantity );
		}
	}

	private static class CreationRequestMap
	{
		private final TreeMap internalMap = new TreeMap();

		public CreateItemRequest get( final String name )
		{
			CreateItemRequest value = (CreateItemRequest) this.internalMap.get( name );
			if ( value == null )
			{
				value = CreateItemRequest.constructInstance( name );
				this.internalMap.put( name, value );
				
			}

			return value;
		}
	}
}
