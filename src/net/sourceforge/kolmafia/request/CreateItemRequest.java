/**
 * Copyright (c) 2005-2014, KoLmafia development team
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
import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumSet;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLAdventure;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLConstants.CraftingRequirements;
import net.sourceforge.kolmafia.KoLConstants.CraftingType;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.SpecialOutfit;

import net.sourceforge.kolmafia.objectpool.Concoction;
import net.sourceforge.kolmafia.objectpool.ConcoctionPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;

import net.sourceforge.kolmafia.persistence.ConcoctionDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.session.EquipmentManager;
import net.sourceforge.kolmafia.session.InventoryManager;
import net.sourceforge.kolmafia.session.ResultProcessor;
import net.sourceforge.kolmafia.session.StoreManager;

import net.sourceforge.kolmafia.utilities.AdventureResultArray;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class CreateItemRequest
	extends GenericRequest
	implements Comparable<CreateItemRequest>
{
	public static final GenericRequest REDIRECT_REQUEST = new GenericRequest( "inventory.php?action=message" );

	public static final Pattern ITEMID_PATTERN = Pattern.compile( "item\\d?=(\\d+)" );
	private static final Pattern QUANTITY_PATTERN = Pattern.compile( "(quantity|qty)=(\\d+)" );
	public static final Pattern TARGET_PATTERN = Pattern.compile( "target=(\\d+)" );
	public static final Pattern MODE_PATTERN = Pattern.compile( "mode=([^&]+)" );
	public static final Pattern CRAFT_PATTERN_1 = Pattern.compile( "[\\&\\?](?:a|b)=(\\d+)" );
	public static final Pattern CRAFT_PATTERN_2 = Pattern.compile( "steps\\[\\]=(\\d+),(\\d+)" );

	public static final Pattern CRAFT_COMMENT_PATTERN =
		Pattern.compile( "<!-- ?cr:(\\d+)x(-?\\d+),(-?\\d+)=(\\d+) ?-->" );
	// 1=quantity, 2,3=items used, 4=result (redundant)
	public static final Pattern DISCOVERY_PATTERN = Pattern.compile( "descitem\\((\\d+)\\);" );

	public static final AdventureResult TENDER_HAMMER = ItemPool.get( ItemPool.TENDER_HAMMER, 1 );
	public static final AdventureResult GRIMACITE_HAMMER = ItemPool.get( ItemPool.GRIMACITE_HAMMER, 1 );

	public Concoction concoction;
	public AdventureResult createdItem;

	private String name;
	private int itemId;
	private CraftingType mixingMethod;
	private EnumSet<CraftingRequirements> requirements;

	protected int beforeQuantity;
	private int yield;

	protected int quantityNeeded, quantityPossible, quantityPullable;

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
	 * @param conc The Concoction for the item to be handled
	 */

	public CreateItemRequest( final String formSource, final Concoction conc )
	{
		super( formSource );

		this.concoction = conc;
		this.itemId = conc.getItemId();
		this.name = conc.getName();
		this.mixingMethod = CraftingType.SUBCLASS;
		this.requirements = conc.getRequirements();
		// is this right?
		this.calculateYield();
	}

	private CreateItemRequest( final Concoction conc )
	{
		this( "", conc );

		this.mixingMethod = conc.getMixingMethod();
	}

	private void calculateYield()
	{
		this.yield = this.concoction.getYield();
		this.createdItem = this.concoction.getItem().getInstance( this.yield );
	}

	@Override
	public void reconstructFields()
	{
		String formSource = "craft.php";
		String action = "craft";
		String mode = null;

		switch ( this.mixingMethod )
		{
		case COMBINE:
		case ACOMBINE:
			mode = "combine";
			break;

		case MIX:
		case MIX_FANCY:
			mode = "cocktail";
			break;

		case COOK:
		case COOK_FANCY:
			mode = "cook";
			break;

		case SMITH:
		case SSMITH:
			mode = "smith";
			break;

		case JEWELRY:
			mode = "jewelry";
			break;

		case ROLLING_PIN:
			formSource = "inv_use.php";
			break;

		case WOK:
			formSource = "guild.php";
			action = "wokcook";
			break;

		case MALUS:
			formSource = "guild.php";
			action = "malussmash";
			break;

		case STILL_MIXER:
			formSource = "guild.php";
			action = "stillfruit";
			break;

		case STILL_BOOZE:
			formSource = "guild.php";
			action = "stillbooze";
			break;
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
		return CreateItemRequest.getInstance( ConcoctionPool.get( itemId ), true );
	}

	public static final CreateItemRequest getInstance( final int itemId, final boolean rNINP )
	{
		return CreateItemRequest.getInstance( ConcoctionPool.get( itemId ), rNINP );
	}

	public static final CreateItemRequest getInstance( final String name )
	{
		return CreateItemRequest.getInstance( ConcoctionPool.get( name ), true );
	}

	public static final CreateItemRequest getInstance( final AdventureResult item )
	{
		return CreateItemRequest.getInstance( ConcoctionPool.get( item ), true );
	}

	public static final CreateItemRequest getInstance( final AdventureResult item, final boolean rNINP )
	{
		return CreateItemRequest.getInstance( ConcoctionPool.get( item ), rNINP );
	}

	public static final CreateItemRequest getInstance( final Concoction conc, final boolean returnNullIfNotPermitted )
	{
		if ( conc == null )
		{
			ConcoctionDatabase.excuse = null;
			return null;
		}

		CreateItemRequest instance = conc.getRequest();

		if ( instance == null )
		{
			ConcoctionDatabase.excuse = null;
			return null;
		}

		if ( instance instanceof CombineMeatRequest )
		{
			return instance;
		}

		// If the item creation process is not permitted, then return
		// null to indicate that it is not possible to create the item.

		if ( returnNullIfNotPermitted )
		{
			if ( Preferences.getBoolean( "unknownRecipe" + conc.getItemId() ) )
			{
				ConcoctionDatabase.excuse = "That item requires a recipe.  If you've already learned it, visit the crafting discoveries page in the relay browser to let KoLmafia know about it.";
				return null;
			}

			Concoction concoction = instance.concoction;
			if ( !ConcoctionDatabase.checkPermittedMethod( concoction ) )
			{	// checkPermittedMethod set the excuse
				return null;
			}
		}

		return instance;
	}

	// This API should only be called by Concoction.getRequest(), which
	// is responsible for caching the instances.
	public static final CreateItemRequest constructInstance( final Concoction conc )
	{
		if ( conc == null )
		{
			return null;
		}

		int itemId = conc.getItemId();

		if ( CombineMeatRequest.getCost( itemId ) > 0 )
		{
			return new CombineMeatRequest( conc );
		}

		// Return the appropriate subclass of item which will be
		// created.

		switch ( conc.getMixingMethod() )
		{
		case NOCREATE:
			return null;

		case STARCHART:
			return new StarChartRequest( conc );

		case SUGAR_FOLDING:
			return new SugarSheetRequest( conc );

		case PIXEL:
			return new PixelRequest( conc );

		case GNOME_TINKER:
			return new GnomeTinkerRequest( conc );

		case STAFF:
			return new ChefStaffRequest( conc );

		case SUSHI:
			return new SushiRequest( conc );

		case SINGLE_USE:
			return new SingleUseRequest( conc );

		case MULTI_USE:
			return new MultiUseRequest( conc );

		case CRIMBO05:
			return new Crimbo05Request( conc );

		case CRIMBO06:
			return new Crimbo06Request( conc );

		case CRIMBO07:
			return new Crimbo07Request( conc );

		case CRIMBO12:
			return new Crimbo12Request( conc );

		case PHINEAS:
			return new PhineasRequest( conc );

		case CLIPART:
			return new ClipArtRequest( conc );

		case JARLS:
			return new JarlsbergRequest( conc );

		case GRANDMA:
			return new GrandmaRequest( conc );

		case CHEMCLASS:
		case ARTCLASS:
		case SHOPCLASS:
			return new KOLHSRequest( conc );

		case BEER:
			return new BeerGardenRequest( conc );

		case JUNK:
			return new JunkMagazineRequest( conc );

		case WINTER:
			return new WinterGardenRequest( conc );

		case RUMPLE:
			return new RumpleRequest( conc );

		default:
			return new CreateItemRequest( conc );
		}
	}

	@Override
	public boolean equals( final Object o )
	{
		if ( o instanceof CreateItemRequest )
		{
			return this.compareTo( (CreateItemRequest) o ) == 0;
		}
		return false;
	}

	@Override
	public int hashCode()
	{
		return this.name != null ? this.name.toLowerCase().hashCode() : 0;
	}

	public int compareTo( final CreateItemRequest o )
	{
		return o == null ? -1 : this.getName().compareToIgnoreCase( ( (CreateItemRequest) o ).getName() );
	}

	/**
	 * Runs the item creation request. Note that if another item needs to be created for the request to succeed, this
	 * method will fail.
	 */

	@Override
	public void run()
	{
		if ( GenericRequest.abortIfInFightOrChoice() )
		{
			return;
		}

		if ( !KoLmafia.permitsContinue() || this.quantityNeeded <= 0 )
		{
			return;
		}

		// Acquire all needed ingredients

		if ( this.mixingMethod != CraftingType.SUBCLASS &&
		     this.mixingMethod != CraftingType.ROLLING_PIN &&
		     !this.makeIngredients() )
		{
			return;
		}

		// Save outfit in case we need to equip something - like a Grimacite hammer

		SpecialOutfit.createImplicitCheckpoint();

		while ( this.quantityNeeded > 0 && KoLmafia.permitsContinue() )
		{
			if ( !this.autoRepairBoxServant() )
			{
				KoLmafia.updateDisplay( MafiaState.ERROR, "Auto-repair was unsuccessful." );
				break;
			}

			this.reconstructFields();

			this.beforeQuantity = this.createdItem.getCount( KoLConstants.inventory );

			switch ( this.mixingMethod )
			{
			case SUBCLASS:
				super.run();
				if ( this.responseCode == 302 && this.redirectLocation.startsWith( "inventory" ) )
				{
					CreateItemRequest.REDIRECT_REQUEST.constructURLString( this.redirectLocation ).run();
				}
				break;

			case ROLLING_PIN:
				int ingredientsOnHand = InventoryManager.getAccessibleCount( this.concoction
					.getIngredients()[ 0 ] );
				if ( ingredientsOnHand > 0 )
				{
					// If we have some of the other kind of dough, make up to that amount first
					// before we make any purchases.
					int temp = this.quantityNeeded;
					this.quantityNeeded = Math.min( this.quantityNeeded, ingredientsOnHand );
					this.makeDough( true );
					this.quantityNeeded = temp;
				}
				else
				{
					this.makeDough();
				}
				break;

			case COINMASTER:
				this.makeCoinmasterPurchase();
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

			int createdQuantity = this.createdItem.getCount( KoLConstants.inventory ) - this.beforeQuantity;

			// If we created none, log error and stop iterating

			if ( createdQuantity == 0 )
			{
				// If the subclass didn't detect the failure, do so here.

				if ( KoLmafia.permitsContinue() )
				{
					KoLmafia.updateDisplay( MafiaState.ERROR, "Creation failed, no results detected." );
				}

				break;
			}

			KoLmafia.updateDisplay( "Successfully created " + this.getName() + " (" + createdQuantity + ")" );
			this.quantityNeeded -= createdQuantity;
		}

		SpecialOutfit.restoreImplicitCheckpoint();
	}

	public boolean noCreation()
	{
		return false;
	}

	public void makeDough()
	{
		makeDough( false );
	}

	public void makeDough( boolean onHand )
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
			KoLmafia.updateDisplay( MafiaState.ERROR, "Can't deduce correct tool to use." );
			return;
		}

		if ( !onHand )
		{
			// If we got here, we have some of one ingredient and none of the other.
			// It's always cheaper to buy wads of dough, so just do that.
			// Using makePurchases directly because retrieveItem does not handle this recursion gracefully.
			AdventureResult dough = ItemPool.get( ItemPool.DOUGH, this.quantityNeeded );
			ArrayList results = StoreManager.searchMall( dough );
			KoLmafia.makePurchases( results, results.toArray(), dough.getCount(), false, 50 );
		}

		// If we don't have the correct tool, and the person wishes to
		// create more than 10 dough, then notify the person that they
		// should purchase a tool before continuing.

		if ( ( this.quantityNeeded >= 10 || InventoryManager.hasItem( tool ) ) && !InventoryManager.retrieveItem( tool ) )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "Please purchase a " + ItemDatabase.getItemName( tool ) + " first." );
			return;
		}

		// If we have the correct tool, use it to
		// create the needed dough type.

		if ( InventoryManager.hasItem( tool ) )
		{
			KoLmafia.updateDisplay( "Using " + ItemDatabase.getItemName( tool ) + "..." );
			UseItemRequest.getInstance( ItemPool.get( tool, 1 ) ).run();
			return;
		}

		// Without the right tool, we must manipulate
		// the dough by hand.

		String name = ItemDatabase.getItemName( output );
		UseItemRequest request = UseItemRequest.getInstance( ItemPool.get( input, 1 ) );

		for ( int i = 1; KoLmafia.permitsContinue() && i <= this.quantityNeeded; ++i )
		{
			KoLmafia.updateDisplay( "Creating " + name + " (" + i + " of " + this.quantityNeeded + ")..." );
			request.run();
		}
	}

	public void makeCoinmasterPurchase()
	{
		PurchaseRequest request = this.concoction.getPurchaseRequest();
		if ( request == null )
		{
			return;
		}
		request.setLimit( this.quantityNeeded );
		request.run();
	}

	/**
	 * Helper routine which actually does the item combination.
	 */

	private void combineItems()
	{
		String path = this.getPath();
		String quantityField = "quantity";

		this.calculateYield();
		AdventureResult[] ingredients =
			ConcoctionDatabase.getIngredients( this.concoction.getIngredients() );

		if ( ingredients.length == 1 || this.mixingMethod == CraftingType.WOK )
		{
			if ( this.getAdventuresUsed() > KoLCharacter.getAdventuresLeft() )
			{
				KoLmafia.updateDisplay( MafiaState.ERROR, "Ran out of adventures." );
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

		int quantity = ( this.quantityNeeded + this.yield - 1 ) / this.yield;
		this.addFormField( quantityField, String.valueOf( quantity ) );

		KoLmafia.updateDisplay( "Creating " + this.name + " (" + this.quantityNeeded + ")..." );
		super.run();
	}

	@Override
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

	public static int parseCrafting( final String location, final String responseText )
	{
		if ( !location.startsWith( "craft.php" ) )
		{
			return 0;
		}

		if ( location.indexOf( "action=pulverize" ) != -1 )
		{
			return PulverizeRequest.parseResponse( location, responseText );
		}

		Matcher m = MODE_PATTERN.matcher( location );
		String mode = m.find() ? m.group(1) : "";
		if ( mode.equals( "discoveries" ) )
		{
			m = DISCOVERY_PATTERN.matcher( responseText );
			while ( m.find() )
			{
				int id = ItemDatabase.getItemIdFromDescription( m.group( 1 ) );
				String pref = "unknownRecipe" + id;
				if ( id > 0 && Preferences.getBoolean( pref ) )
				{
					KoLmafia.updateDisplay( "You know the recipe for " +
						ItemDatabase.getItemName( id ) );
					Preferences.setBoolean( pref, false );
					ConcoctionDatabase.setRefreshNeeded( true );
				}
			}

			return 0;
		}

		boolean paste = mode.equals( "combine" ) && ( !KoLCharacter.knollAvailable() || KoLCharacter.inZombiecore() );
		int created = 0;

		m = CRAFT_COMMENT_PATTERN.matcher( responseText );
		while ( m.find() )
		{
			// item ids can be -1, if crafting uses a single item
			int qty = StringUtilities.parseInt( m.group( 1 ) );
			int item1 = StringUtilities.parseInt( m.group( 2 ) );
			int item2 = StringUtilities.parseInt( m.group( 3 ) );
			if ( item1 > 0 )
			{
				ResultProcessor.processItem( item1, -qty );
			}
			if ( item2 > 0 )
			{
				ResultProcessor.processItem( item2, -qty );
			}
			if ( paste )
			{
				ResultProcessor.processItem( ItemPool.MEAT_PASTE, -qty );
			}
			if ( item1 < 0 )
			{
				RequestLogger.updateSessionLog( "Crafting used " + qty +
								ItemDatabase.getItemName( item2 ) );
			}
			else if ( item2 < 0 )
			{
				RequestLogger.updateSessionLog( "Crafting used " + qty +
								ItemDatabase.getItemName( item1 ) );
			}
			else
			{
				RequestLogger.updateSessionLog( "Crafting used " + qty + " each of " +
								ItemDatabase.getItemName( item1 ) + " and " +
								ItemDatabase.getItemName( item2 ) );
			}
			String pref = "unknownRecipe" + m.group( 4 );
			if ( Preferences.getBoolean( pref ) )
			{
				KoLmafia.updateDisplay( "(You apparently already knew this recipe.)" );
				Preferences.setBoolean( pref, false );
				ConcoctionDatabase.setRefreshNeeded( true );
			}

			if ( ItemDatabase.isFancyItem( item1 ) || ItemDatabase.isFancyItem( item2 ) )
			{
				if ( mode.equals( "cook" ) && KoLCharacter.hasChef() )
				{
					Preferences.increment( "chefTurnsUsed", qty );
				}
				else if ( mode.equals( "cocktail" ) && KoLCharacter.hasBartender() )
				{
					Preferences.increment( "bartenderTurnsUsed", qty );
				}
			}

			created = qty;
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

		return created;
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
		int quantity = CreateItemRequest.getQuantity( urlString, ingredients, multiplier );

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
		return CreateItemRequest.autoRepairBoxServant( this.mixingMethod, this.requirements );
	}

	public static boolean autoRepairBoxServant( final CraftingType mixingMethod, final EnumSet<CraftingRequirements> requirements )
	{
		if ( KoLmafia.refusesContinue() )
		{
			return false;
		}

		if ( requirements.contains( CraftingRequirements.HAMMER ) &&
		     !InventoryManager.retrieveItem( CreateItemRequest.TENDER_HAMMER ) )
		{
			return false;
		}

		if ( requirements.contains( CraftingRequirements.GRIMACITE ) )
		{
			AdventureResult hammer = CreateItemRequest.GRIMACITE_HAMMER;
			int slot = EquipmentManager.WEAPON;

			if ( !KoLCharacter.hasEquipped( hammer, slot ) &&
			     EquipmentManager.canEquip( hammer ) &&
			     InventoryManager.retrieveItem( hammer ) )
			{
				( new EquipmentRequest( hammer, slot ) ).run();
			}

			return KoLCharacter.hasEquipped( hammer, slot );
		}

		// If we are not cooking or mixing, or if we already have the
		// appropriate servant installed, we don't need to repair

		switch ( mixingMethod )
		{
		case COOK_FANCY:

			// We need range installed to cook fancy foods.
			if ( !KoLCharacter.hasRange() )
			{
				// Acquire and use a range
				if ( !InventoryManager.retrieveItem( ItemPool.RANGE ) )
				{
					return false;
				}
				UseItemRequest.getInstance( ItemPool.get( ItemPool.RANGE, 1 ) ).run();
			}

			// If we have a chef, fancy cooking is now free
			if ( KoLCharacter.hasChef() )
			{
				return true;
			}
			break;

		case MIX_FANCY:

			// We need a cocktail kit installed to mix fancy
			// drinks.
			if ( !KoLCharacter.hasCocktailKit() )
			{
				// Acquire and use cocktail kit
				if ( !InventoryManager.retrieveItem( ItemPool.COCKTAIL_KIT ) )
				{
					return false;
				}
				UseItemRequest.getInstance( ItemPool.get( ItemPool.COCKTAIL_KIT, 1 ) ).run();
			}

			// If we have a bartender, fancy mixing is now free
			if ( KoLCharacter.hasBartender() )
			{
				return true;
			}
			break;

		case SMITH:

			return ( KoLCharacter.knollAvailable() && !KoLCharacter.inZombiecore() ) || InventoryManager.retrieveItem( CreateItemRequest.TENDER_HAMMER );

		case SSMITH:

			return InventoryManager.retrieveItem( CreateItemRequest.TENDER_HAMMER );

		default:
			return true;
		}

		boolean autoRepairSuccessful = false;

		// If they want to auto-repair, make sure that the appropriate
		// item is available in their inventory

		switch ( mixingMethod )
		{
		case COOK_FANCY:
			autoRepairSuccessful =
				CreateItemRequest.useBoxServant( ItemPool.CHEF, ItemPool.CLOCKWORK_CHEF );
			break;

		case MIX_FANCY:
			autoRepairSuccessful =
				CreateItemRequest.useBoxServant( ItemPool.BARTENDER, ItemPool.CLOCKWORK_BARTENDER );
			break;
		}

		return autoRepairSuccessful && KoLmafia.permitsContinue();
	}

	private static boolean useBoxServant( final int servant, final int clockworkServant )
	{
		// We have no box servant.

		if ( !Preferences.getBoolean( "autoRepairBoxServants" ) )
		{
			// We don't want to autorepair. It's OK if we don't
			// require one and have turns available to craft.
			return !Preferences.getBoolean( "requireBoxServants" ) &&
				( KoLCharacter.getAdventuresLeft() > 0 ||
				  ConcoctionDatabase.INIGO.getCount( KoLConstants.activeEffects ) > 4 );
		}

		// We want to autorepair.

		// First, check to see if a box servant is available
		// for usage, either normally, or through some form
		// of creation.

		int usedServant;

		if ( InventoryManager.hasItem( clockworkServant, false ) )
		{
			usedServant = clockworkServant;
		}
		else if ( InventoryManager.hasItem( servant, true ) )
		{
			usedServant = servant;
		}
		else if ( KoLCharacter.canInteract() && ( Preferences.getBoolean( "autoSatisfyWithMall" ) || Preferences.getBoolean( "autoSatisfyWithStash" ) ) )
		{
			usedServant = servant;
		}
		else
		{
			// We can't autorepair. It's still OK if we are willing
			// to cook without a box servant and have turns
			// available to craft.
			return !Preferences.getBoolean( "requireBoxServants" ) &&
				( KoLCharacter.getAdventuresLeft() > 0 ||
				  ConcoctionDatabase.INIGO.getCount( KoLConstants.activeEffects ) > 4 );
		}

		// Once you hit this point, you're guaranteed to
		// have the servant in your inventory, so attempt
		// to repair the box servant.

		UseItemRequest.getInstance( ItemPool.get( usedServant, 1 ) ).run();
		return servant == ItemPool.CHEF ? KoLCharacter.hasChef() : KoLCharacter.hasBartender();
	}

	public boolean makeIngredients()
	{
		KoLmafia.updateDisplay( "Verifying ingredients for " + this.name + " (" + this.quantityNeeded + ")..." );

		this.calculateYield();
		int yield = this.yield;

		// If this is a combining request, you need meat paste as well.
		
		if ( ( this.mixingMethod == CraftingType.COMBINE || this.mixingMethod == CraftingType.ACOMBINE ) &&
		     ( !KoLCharacter.knollAvailable() || KoLCharacter.inZombiecore()  ) )
		{
			int pasteNeeded = this.concoction.getMeatPasteNeeded(
				this.quantityNeeded + this.concoction.initial );
			AdventureResult paste = ItemPool.get( ItemPool.MEAT_PASTE, pasteNeeded );

			if ( !InventoryManager.retrieveItem( paste ) )
			{
				return false;
			}
		}

		AdventureResult[] ingredients = (AdventureResult[]) ConcoctionDatabase.getIngredients(
			this.concoction.getIngredients() ).clone();

		// Sort ingredients by their creatability, so that if the overall creation
		// is going to fail, it should do so immediately, without wasted effort.
		Arrays.sort( ingredients, new Comparator() {
			public int compare( Object o1, Object o2 )
			{
				Concoction left = ConcoctionPool.get( (AdventureResult) o1 );
				if ( left == null ) return -1;
				Concoction right = ConcoctionPool.get( (AdventureResult) o2 );
				if ( right == null ) return 1;
				return left.creatable - right.creatable;
			}
		} );

		// Retrieve all the ingredients
		AdventureResult [] retrievals = new AdventureResult[ ingredients.length ];

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
					multiplier += ingredients[ j ].getCount();
				}
			}

			// Then, make enough of the ingredient in order
			// to proceed with the concoction.

			int quantity = this.quantityNeeded * multiplier;

			if ( yield > 1 )
			{
				quantity = ( quantity + yield - 1 ) / yield;
			}

			AdventureResult retrieval = ingredients[ i ].getInstance( quantity );
			if ( !InventoryManager.retrieveItem( retrieval ) )
			{
				return false;
			}

			retrievals[ i ] = retrieval;
		}

		// clockwork clockwise dome = clockwork widget + flange + spring
		// clockwork widget = flange + cog + sprocket
		//
		// Creating a clockwork counterclockwise dome retrieves a flange and a spring and recurses
		// Creating a clockwork widget sees that it has a flange, retrieves a cog and a socket,
		//   makes the widget and returns
		// The previously retrieved flange is no longer present for the clockwork counterclockwise dome
		//
		// The issue arises when an ingredient is needed both for this creation and another
		// ingredient  and we no longer have a necessary ingredient in inventory after, supposedly,
		// retrieving all the ingredients

		// Make another pass to get ingredients that were consumed
		// while creating other ingredients.

		for ( int i = 0; i < ingredients.length; ++i )
		{
			if ( !InventoryManager.retrieveItem( retrievals[ i ] ) )
			{
				return false;
			}
		}

		return true;
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

	@Override
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

	@Override
	public int getAdventuresUsed()
	{
		return CreateItemRequest.getAdventuresUsed( this );
	}

	public static int getAdventuresUsed( final GenericRequest request )
	{
		String urlString = request.getURLString();
		String path = request.getPath();

		CraftingType mixingMethod = CreateItemRequest.findMixingMethod( urlString );

		if ( mixingMethod == CraftingType.NOCREATE )
		{
			return 0;
		}

		int multiplier = CreateItemRequest.getMultiplier( mixingMethod );
		if ( multiplier == 0 )
		{
			return 0;
		}

		AdventureResult [] ingredients = CreateItemRequest.findIngredients( urlString );
		int quantity = CreateItemRequest.getQuantity( urlString, ingredients, multiplier );
		return CreateItemRequest.getAdventuresUsed( mixingMethod, quantity );
	}

	private static final CraftingType findMixingMethod( final String urlString )
	{
		if ( urlString.startsWith( "guild.php" ) )
		{
			return urlString.contains( "action=wokcook" ) ? CraftingType.WOK: CraftingType.NOCREATE;
		}
		Concoction concoction = CreateItemRequest.findConcoction( urlString );
		return concoction == null ? CraftingType.NOCREATE: concoction.getMixingMethod();
	}

	private static final Concoction findConcoction( final String urlString )
	{
		if ( urlString.startsWith( "craft.php" ) )
		{
			if ( urlString.contains( "target" ) )
			{
				Matcher targetMatcher = CreateItemRequest.TARGET_PATTERN.matcher( urlString );
				if ( targetMatcher.find() )
				{
					return null;
				}
				return ConcoctionPool.get( StringUtilities.parseInt( targetMatcher.group( 1 ) ) );
			}
			return ConcoctionPool.findConcoction( CreateItemRequest.findIngredients( urlString ) );
		}

		return null;
	}

	private static int getMultiplier( final CraftingType mixingMethod )
	{
		switch ( mixingMethod )
		{
		case SMITH:
			return ( KoLCharacter.knollAvailable() && !KoLCharacter.inZombiecore() ) ? 0 : 1;

		case SSMITH:
			return 1;

		case JEWELRY:
			return 3;

		case COOK_FANCY:
			return KoLCharacter.hasChef() ? 0 : 1;

		case MIX_FANCY:
			return KoLCharacter.hasBartender() ? 0 : 1;

		case WOK:
			return 1;
		}

		return 0;
	}

	private static int getAdventuresUsed( final CraftingType mixingMethod, final int quantityNeeded )
	{
		switch ( mixingMethod )
		{
		case SMITH:
			return Math.max( 0, ( quantityNeeded - ConcoctionDatabase.getFreeCraftingTurns() ) );

		case SSMITH:
			return Math.max( 0, ( quantityNeeded - ConcoctionDatabase.getFreeCraftingTurns() ) );

		case JEWELRY:
			return Math.max( 0, ( ( 3 * quantityNeeded ) - ConcoctionDatabase.getFreeCraftingTurns() ) );

		case COOK_FANCY:
			return Math.max( 0, ( quantityNeeded - ConcoctionDatabase.getFreeCraftingTurns() ) );

		case MIX_FANCY:
			return Math.max( 0, ( quantityNeeded - ConcoctionDatabase.getFreeCraftingTurns() ) );

		case WOK:
			return quantityNeeded;
		}

		return 0;
	}

	public static final boolean registerRequest( final boolean isExternal, final String urlString )
	{
		// Delegate to subclasses if appropriate

		if ( urlString.startsWith( "sugarsheets.php" ) )
		{
			return SugarSheetRequest.registerRequest( urlString );
		}
		
		if ( urlString.startsWith( "shop.php" ) )
		{
			// Let shop.php creation methods register themselves
			return false;
		}

		if ( urlString.startsWith( "volcanoisland.php" ) )
		{
			return PhineasRequest.registerRequest( urlString );
		}

		if ( urlString.startsWith( "sushi.php" ) )
		{
			return SushiRequest.registerRequest( urlString );
		}

		if ( urlString.startsWith( "crimbo07.php" ) )
		{
			return Crimbo07Request.registerRequest( urlString );
		}

		if ( urlString.contains( "action=makestaff" ) )
		{
			return ChefStaffRequest.registerRequest( urlString );
		}

		if ( urlString.contains( "action=makepaste" ) || urlString.contains( "action=makestuff" ) )
		{
			return CombineMeatRequest.registerRequest( urlString );
		}

		if ( urlString.startsWith( "multiuse.php" ) )
		{
			if ( MultiUseRequest.registerRequest( urlString ) )
			{
				return true;
			}

			if ( SingleUseRequest.registerRequest( urlString ) )
			{
				return true;
			}

			return false;
		}

		if ( urlString.startsWith( "gnomes.php" ) )
		{
			return GnomeTinkerRequest.registerRequest( urlString );
		}

		if ( urlString.startsWith( "inv_use.php" ) )
		{
			if ( MultiUseRequest.registerRequest( urlString ) )
			{
				return true;
			}

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

			RequestLogger.updateSessionLog();
			RequestLogger.updateSessionLog( "Use " + tool );

			// *** Should do this after we get response text back
			AdventureResult item = new AdventureResult( ingredient, 1 );
			int quantity = item.getCount( KoLConstants.inventory );
			ResultProcessor.processItem( item.getItemId(), 0 - quantity );

			return true;
		}

		// Now that we know it's not a special subclass instance,
		// all we do is parse out the ingredients which were used
		// and then print the attempt to the screen.

		String command = null;
		int multiplier = 1;
		boolean usesTurns = false;

		if ( urlString.startsWith( "craft.php" ) )
		{
			if ( urlString.contains( "action=pulverize" ) )
			{
				return false;
			}

			if ( !urlString.contains( "action=craft" ) )
			{
				return true;
			}

			if ( urlString.contains( "mode=combine" ) )
			{
				command = "Combine";
			}
			else if ( urlString.contains( "mode=cocktail" ) )
			{
				command = "Mix";
				usesTurns = !KoLCharacter.hasBartender();
			}
			else if ( urlString.contains( "mode=cook" ) )
			{
				command = "Cook";
				usesTurns = !KoLCharacter.hasChef();
			}
			else if ( urlString.contains( "mode=smith" ) )
			{
				command = "Smith";
				usesTurns = true;
			}
			else if ( urlString.contains( "mode=jewelry" ) )
			{
				command = "Ply";
				usesTurns = true;
			}
			else
			{
				// Take credit for all visits to crafting
				return true;
			}
		}
		else if ( urlString.startsWith( "guild.php" ) )
		{
			if ( urlString.contains( "action=stillbooze" ) || urlString.contains( "action=stillfruit" ) )
			{
				command = "Distill";
			}
			else if ( urlString.contains( "action=wokcook" ) )
			{
				command = "Wok";
				usesTurns = true;
			}
			else if ( urlString.contains( "action=malussmash" ) )
			{
				command = "Pulverize";
				multiplier = 5;
			}
			else
			{
				return false;
			}
		}
		else
		{
			return false;
		}

		String line = CreateItemRequest.getCreationCommand( command, urlString, multiplier, usesTurns );

		RequestLogger.updateSessionLog();
		RequestLogger.updateSessionLog( line );

		// *** We should deduct ingredients after we have verified that the creation worked.
		AdventureResult [] ingredients = CreateItemRequest.findIngredients( urlString );
		int quantity = CreateItemRequest.getQuantity( urlString, ingredients, multiplier );
		CreateItemRequest.useIngredients( urlString, ingredients, quantity );

		return true;
	}

	public static final String getCreationCommand( final String command, final String urlString, final int multiplier, final boolean usesTurns )
	{
		StringBuilder buffer = new StringBuilder();

		if ( usesTurns )
		{
			buffer.append( "[" );
			buffer.append( String.valueOf( KoLAdventure.getAdventureCount() + 1 ) );
			buffer.append( "] " );
		}

		buffer.append( command );

		AdventureResult [] ingredients = CreateItemRequest.findIngredients( urlString );
		int quantity = CreateItemRequest.getQuantity( urlString, ingredients, multiplier );

		for ( int i = 0; i < ingredients.length; ++i )
		{
			AdventureResult item = ingredients[i];
			if ( item.getItemId() == 0 )
			{
				continue;
			}

			buffer.append( ' ' );

			if ( i > 0 )
			{
				buffer.append( "+ " );
			}

			buffer.append( quantity );
			buffer.append( ' ' );
			buffer.append( item.getName() );
		}

		return buffer.toString();
	}

	public static final AdventureResult [] findIngredients( final String urlString )
	{
		if ( urlString.startsWith( "craft.php" ) && urlString.contains( "target" ) )
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

		Matcher matcher =
			urlString.startsWith( "craft.php" ) ?
			CreateItemRequest.CRAFT_PATTERN_1.matcher( urlString ) :
			CreateItemRequest.ITEMID_PATTERN.matcher( urlString );

		AdventureResultArray ingredients = new AdventureResultArray();
		while ( matcher.find() )
		{
			ingredients.add( CreateItemRequest.getIngredient( matcher.group(1) ) );
		}

		if ( urlString.contains( "action=wokcook" ) )
		{
			ingredients.add( ItemPool.get( ItemPool.DRY_NOODLES, 1 ) );
			ingredients.add( ItemPool.get( ItemPool.MSG, 1 ) );
		}

		return ingredients.toArray();
	}

	private static final AdventureResult getIngredient( final String itemId )
	{
		return ItemPool.get( StringUtilities.parseInt( itemId ), 1 );
	}

	public static final int getQuantity( final String urlString, final AdventureResult [] ingredients, int multiplier )
	{
		if ( !urlString.contains( "max=on" ) &&
		     !urlString.contains( "smashall=1" ) &&
		     !urlString.contains( "makeall=on" ) )
		{
			Matcher matcher = CreateItemRequest.QUANTITY_PATTERN.matcher( urlString );
			return matcher.find() ?
				StringUtilities.parseInt( matcher.group( 2 ) ) * multiplier :
				multiplier;
		}

		int quantity = Integer.MAX_VALUE;

		for ( int i = 0; i < ingredients.length; ++i )
		{
			AdventureResult item = ingredients[i];
			quantity = Math.min( item.getCount( KoLConstants.inventory ) / multiplier, quantity );
		}

		return quantity * multiplier;
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
}
