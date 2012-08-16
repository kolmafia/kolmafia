/**
 * Copyright (c) 2005-2012, KoLmafia development team
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

import java.util.Map;
import java.util.TreeMap;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestThread;

import net.sourceforge.kolmafia.objectpool.Concoction;
import net.sourceforge.kolmafia.objectpool.ConcoctionPool;
import net.sourceforge.kolmafia.objectpool.IntegerPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;

import net.sourceforge.kolmafia.persistence.ItemDatabase;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.session.InventoryManager;
import net.sourceforge.kolmafia.session.ResultProcessor;

import net.sourceforge.kolmafia.utilities.StringUtilities;

public class HermitRequest
	extends CoinMasterRequest
{
	private static final Pattern TOKEN_PATTERN = Pattern.compile( "You have ([\\d,]+) tradable items" );
	public static final AdventureResult WORTHLESS_ITEM = ItemPool.get( ItemPool.WORTHLESS_ITEM, 1 );
	private static final Map buyPrices = new TreeMap();

	public static final CoinmasterData HERMIT =
		new CoinmasterData(
			"Hermit",
			HermitRequest.class,
			"hermit.php",
			"worthless item",
			null,
			false,
			HermitRequest.TOKEN_PATTERN,
			HermitRequest.WORTHLESS_ITEM,
			null,
			"whichitem",
			CoinMasterRequest.ITEMID_PATTERN,
			"quantity",
			CoinMasterRequest.QUANTITY_PATTERN,
			"trade",
			KoLConstants.hermitItems,
			HermitRequest.buyPrices,
			null,
			null
			);

	static
	{
		ConcoctionPool.set( new Concoction( WORTHLESS_ITEM, KoLConstants.NOCREATE ) );
	};

	private static final Pattern CLOVER_PATTERN = Pattern.compile( "(\\d+) left in stock for today" );

	public static final AdventureResult CLOVER = ItemPool.get( ItemPool.TEN_LEAF_CLOVER, 1 );
	public static final String CLOVER_FIELD = "whichitem=" + String.valueOf( ItemPool.TEN_LEAF_CLOVER );

	public static final AdventureResult PERMIT = ItemPool.get( ItemPool.HERMIT_PERMIT, 1 );

	public static final AdventureResult TRINKET = ItemPool.get( ItemPool.WORTHLESS_TRINKET, 1 );
	public static final AdventureResult GEWGAW = ItemPool.get( ItemPool.WORTHLESS_GEWGAW, 1 );
	public static final AdventureResult KNICK_KNACK = ItemPool.get( ItemPool.WORTHLESS_KNICK_KNACK, 1 );

	public static final AdventureResult HACK_SCROLL = ItemPool.get( ItemPool.HERMIT_SCRIPT, 1 );
	public static final AdventureResult SUMMON_SCROLL = ItemPool.get( ItemPool.ELITE_SCROLL, 1 );

	private static boolean checkedForClovers = false;
	private static final Integer ONE = IntegerPool.get( 1 );

	/**
	 * Constructs a new <code>HermitRequest</code> that simply checks what items the hermit has available.
	 */

	public HermitRequest()
	{
		super( HermitRequest.HERMIT );
	}

	public HermitRequest( final String action )
	{
		super( HermitRequest.HERMIT, action );
	}

	public HermitRequest( final String action, final int itemId, final int quantity )
	{
		super( HermitRequest.HERMIT, action, itemId, quantity );
	}

	public HermitRequest( final String action, final int itemId )
	{
		this( action, itemId, 1 );
	}

	public HermitRequest( final String action, final AdventureResult ar )
	{
		this( action, ar.getItemId(), ar.getCount() );
	}

	public HermitRequest( final int itemId, final int quantity )
	{
		this( "trade", itemId, quantity );
	}

	private static final void registerHermitItem( final int itemId, final int count )
	{
		AdventureResult item = ItemPool.get( itemId, count );
		String name = StringUtilities.getCanonicalName( item.getName() );
		KoLConstants.hermitItems.add( item );
		HermitRequest.buyPrices.put( name, HermitRequest.ONE );
	}

	public static final void initialize()
	{
		HermitRequest.reset();

		if ( KoLCharacter.inZombiecore() )
		{
			HermitRequest.resetPurchaseRequests();
			return;
		}

		HermitRequest.registerHermitItem( ItemPool.SEAL_TOOTH, PurchaseRequest.MAX_QUANTITY );
		HermitRequest.registerHermitItem( ItemPool.CHISEL, PurchaseRequest.MAX_QUANTITY );
		HermitRequest.registerHermitItem( ItemPool.PETRIFIED_NOODLES, PurchaseRequest.MAX_QUANTITY );
		HermitRequest.registerHermitItem( ItemPool.JABANERO_PEPPER, PurchaseRequest.MAX_QUANTITY );
		HermitRequest.registerHermitItem( ItemPool.BANJO_STRINGS, PurchaseRequest.MAX_QUANTITY );
		HermitRequest.registerHermitItem( ItemPool.BUTTERED_ROLL, PurchaseRequest.MAX_QUANTITY );
		HermitRequest.registerHermitItem( ItemPool.WOODEN_FIGURINE, PurchaseRequest.MAX_QUANTITY );
		HermitRequest.registerHermitItem( ItemPool.KETCHUP, PurchaseRequest.MAX_QUANTITY );
		HermitRequest.registerHermitItem( ItemPool.CATSUP, PurchaseRequest.MAX_QUANTITY );
		HermitRequest.registerHermitItem( ItemPool.VOLLEYBALL, PurchaseRequest.MAX_QUANTITY );
		if ( KoLCharacter.getClassType().equals( KoLCharacter.SEAL_CLUBBER ) )
		{
			HermitRequest.registerHermitItem( ItemPool.ANCIENT_SEAL, PurchaseRequest.MAX_QUANTITY );
		}

		HermitRequest.registerHermitItem( ItemPool.TEN_LEAF_CLOVER, -1 );

		HermitRequest.resetPurchaseRequests();
	}

	public static final void reset()
	{
		HermitRequest.checkedForClovers = false;
		KoLConstants.hermitItems.clear();
		HermitRequest.buyPrices.clear();
	}

	public static final void resetPurchaseRequests()
	{
		HermitRequest.HERMIT.registerPurchaseRequests();
	}

	/**
	 * Executes the <code>HermitRequest</code>. This will trade the item specified in the character's
	 * <code>KoLSettings</code> for their worthless trinket; if the character has no worthless trinkets, this method
	 * will report an error to the StaticEntity.getClient().
	 */

	@Override
	public void run()
	{
		// If we have a hermit script, read it now
		if ( InventoryManager.hasItem( HermitRequest.HACK_SCROLL ) )
		{
			RequestThread.postRequest( UseItemRequest.getInstance( HermitRequest.HACK_SCROLL ) );
		}

		// If we want to make a trade, fetch enough worthless items
		if ( HermitRequest.getWorthlessItemCount( false ) < this.quantity )
		{
			InventoryManager.retrieveItem( HermitRequest.WORTHLESS_ITEM.getInstance( this.quantity ) );
		}

		// Otherwise, we are simply visiting and don't need any

		if ( HermitRequest.getWorthlessItemCount( false ) < this.quantity )
		{
			return;
		}

		if ( this.quantity > 0 )
		{
			this.setQuantity( Math.min( this.quantity, HermitRequest.getWorthlessItemCount( false ) ) );
		}

		super.run();
	}

	@Override
	public void processResults()
	{
		// The Hermit has left in the Zombie Slayer path
		if ( KoLCharacter.inZombiecore() )
		{
			return;
		}

		if ( !HermitRequest.parseHermitTrade( this.getURLString(), this.responseText ) )
		{
			// If we got here, the hermit wouldn't talk to us.
			if ( InventoryManager.retrieveItem( HermitRequest.PERMIT ) )
			{
				this.run();
				return;
			}

			KoLmafia.updateDisplay( MafiaState.ERROR, "You're not allowed to visit the Hermit." );
			return;
		}

		if ( this.itemId == -1 )
		{
			return;
		}

		// If you don't have any hermit permits, get one
		// The Hermit looks at you expectantly, and when you don't respond, he points to a crudely-chalked
		// sign on the wall reading "Hermit Permit required, pursuant to Seaside Town Ordinance #3769"

		if ( this.responseText.indexOf( "Hermit Permit required" ) != -1 )
		{
			if ( InventoryManager.retrieveItem( HermitRequest.PERMIT ) )
			{
				this.run();
			}

			return;
		}

		// If the item is unavailable, assume he was asking for clover

		if ( this.responseText.indexOf( "doesn't have that item." ) != -1 )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "Today is not a clover day." );
			return;
		}

		// If you still didn't acquire items, what went wrong?

		if ( this.responseText.indexOf( "You acquire" ) == -1 )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "The hermit kept his stuff." );
			return;
		}
	}

	public static final boolean parseHermitTrade( final String urlString, final String responseText )
	{
		// Nothing special to do if the Hermit has departed
		if ( KoLCharacter.inZombiecore() )
		{
			return true;
		}

		// There should be a form, or an indication of item receipt,
		// for all valid hermit requests.

		if ( responseText.indexOf( "hermit.php" ) == -1 && responseText.indexOf( "You acquire" ) == -1 )
		{
			return false;
		}

		// If you don't have enough Hermit Permits, failure

		if ( responseText.indexOf( "You don't have enough Hermit Permits" ) != -1 )
		{
			HermitRequest.checkedForClovers = false;
			return true;
		}

		// If the item is unavailable, assume he was asking for clover
		// If asked for too many, you get no items

		if ( responseText.indexOf( "doesn't have that item." ) != -1 ||
		     responseText.indexOf( "You acquire" ) == -1 )
		{
			HermitRequest.parseHermitStock( responseText );
			return true;
		}

		Matcher quantityMatcher = UseItemRequest.QUANTITY_PATTERN.matcher( urlString );
		if ( !quantityMatcher.find() )
		{
			// We simply visited the hermit
			HermitRequest.parseHermitStock( responseText );
			return true;
		}

		int quantity = StringUtilities.parseInt( quantityMatcher.group( 1 ) );

		// Subtract the worthless items in order of their priority;
		// as far as we know, the priority is the item Id.

		int used = HermitRequest.subtractWorthlessItems( HermitRequest.TRINKET, quantity );
		if ( used > 0 )
		{
			quantity -= used;
		}
		used = HermitRequest.subtractWorthlessItems( HermitRequest.GEWGAW, quantity );
		if ( used > 0 )
		{
			quantity -= used;
		}
		used = HermitRequest.subtractWorthlessItems( HermitRequest.KNICK_KNACK, quantity );
		if ( used > 0 )
		{
			quantity -= used;
		}

		if ( responseText.indexOf( "he sends you packing" ) != -1 )
		{
			// No worthless items in inventory, so we can't tell if
			// clovers remain in stock
			HermitRequest.checkedForClovers = false;
			return true;
		}

		HermitRequest.parseHermitStock( responseText );

		return true;
	}

	private static final int subtractWorthlessItems( final AdventureResult item, final int total )
	{
		int count = 0 - Math.min( total, item.getCount( KoLConstants.inventory ) );
		if ( count != 0 )
		{
			ResultProcessor.processResult( item.getInstance( count ) );
		}
		return 0 - count;
	}

	// <td valign=center><img src="http://images.kingdomofloathing.com/itemimages/tooth.gif" class=hand onClick='javascript:item(617818041)'></td><td valign=center><b>seal tooth</b></td></tr>
	private static final Pattern ITEM_PATTERN = Pattern.compile( "javascript:item\\(([\\d]+)\\).*?<b>([^<]*)</b>", Pattern.DOTALL );

	private static final void parseHermitStock( final String responseText )
	{
		// Refresh the Coin Master inventory every time we visit.
		Matcher matcher = ITEM_PATTERN.matcher( responseText );
		if ( !matcher.find() )
		{
			return;
		}

		KoLConstants.hermitItems.clear();
		HermitRequest.buyPrices.clear();

		do
		{
			String descId = matcher.group(1);
			String itemName = matcher.group(2);
			int itemId = ItemDatabase.getItemId( itemName );

			// Add it to the Hermit's inventory
			HermitRequest.registerHermitItem( itemId, PurchaseRequest.MAX_QUANTITY );
		}
		while ( matcher.find() );

		Matcher cloverMatcher = CLOVER_PATTERN.matcher( responseText );
		int count = cloverMatcher.find() ? Integer.parseInt( cloverMatcher.group( 1 ) ) : 0;

		int index = KoLConstants.hermitItems.indexOf( CLOVER );
		if ( index < 0 )
		{
			HermitRequest.registerHermitItem( ItemPool.TEN_LEAF_CLOVER, count );
		}
		else
		{
			AdventureResult old = (AdventureResult) KoLConstants.hermitItems.get( index );
			int oldCount = old.getCount();
			if ( oldCount != count )
			{
				KoLConstants.hermitItems.set( index, CLOVER.getInstance( count ) );
			}
		}

		HermitRequest.checkedForClovers = true;

		// Register the purchase requests, now that we know what is available
		HermitRequest.HERMIT.registerPurchaseRequests();
	}

	public static final boolean isWorthlessItem( final int itemId )
	{
		return itemId == ItemPool.WORTHLESS_TRINKET || itemId == ItemPool.WORTHLESS_GEWGAW || itemId == ItemPool.WORTHLESS_KNICK_KNACK;
	}

	public static final int getWorthlessItemCount()
	{
		return HermitRequest.getWorthlessItemCount( false );
	}

	public static final int getWorthlessItemCount( boolean all )
	{
		int count =
			HermitRequest.TRINKET.getCount( KoLConstants.inventory ) +
			HermitRequest.GEWGAW.getCount( KoLConstants.inventory ) +
			HermitRequest.KNICK_KNACK.getCount( KoLConstants.inventory );

		if ( all )
		{
			if ( Preferences.getBoolean( "autoSatisfyWithCloset" ) )
			{
				count +=
					HermitRequest.TRINKET.getCount( KoLConstants.closet ) +
					HermitRequest.GEWGAW.getCount( KoLConstants.closet ) +
					HermitRequest.KNICK_KNACK.getCount( KoLConstants.closet );
			}

			if ( KoLCharacter.canInteract() && Preferences.getBoolean( "autoSatisfyWithStorage" ) )
			{
				count +=
					HermitRequest.TRINKET.getCount( KoLConstants.storage ) +
					HermitRequest.GEWGAW.getCount( KoLConstants.storage ) +
					HermitRequest.KNICK_KNACK.getCount( KoLConstants.storage );
			}
		}

		return count;
	}

	public static final int getAcquirableWorthlessItemCount()
	{
		int count = HermitRequest.getWorthlessItemCount( true );
		if ( Preferences.getBoolean( "autoSatisfyWithNPCs" ) )
		{
			int cost = InventoryManager.currentWorthlessItemCost();
			count += KoLCharacter.getAvailableMeat() / cost;
		}
		return count;
	}

	public static final int cloverCount()
	{
		if ( !HermitRequest.checkedForClovers )
		{
			RequestThread.postRequest( new HermitRequest() );
		}

		int index = KoLConstants.hermitItems.indexOf( CLOVER );
		return index < 0 ? 0 : ( (AdventureResult) KoLConstants.hermitItems.get( index ) ).getCount();
	}

	public static final boolean isCloverDay()
	{
		return HermitRequest.cloverCount() > 0;
	}
	
	public static final void hackHermit()
	{
		int index = KoLConstants.hermitItems.indexOf( HermitRequest.CLOVER ); 	 
		if ( index != -1 ) 	 
		{ 	 
			AdventureResult clover = ( AdventureResult)KoLConstants.hermitItems.get( index ); 	 
			KoLConstants.hermitItems.set( index, HermitRequest.CLOVER.getInstance( clover.getCount() + 1 ) );
		}
		else
		{
			HermitRequest.registerHermitItem( ItemPool.TEN_LEAF_CLOVER, 1 );
		}
	}

	public static final boolean registerRequest( final String urlString )
	{
		if ( !urlString.startsWith( "hermit.php" ) )
		{
			return false;
		}

		CoinmasterData data = HermitRequest.HERMIT;
		return CoinMasterRequest.registerRequest( data, urlString, true );
	}
}
