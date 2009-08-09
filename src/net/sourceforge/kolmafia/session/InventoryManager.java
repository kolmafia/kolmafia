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

package net.sourceforge.kolmafia.session;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.FamiliarData;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.KoLmafiaASH;
import net.sourceforge.kolmafia.KoLmafiaCLI;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.SpecialOutfit;
import net.sourceforge.kolmafia.StaticEntity;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.swingui.CouncilFrame;
import net.sourceforge.kolmafia.swingui.ItemManageFrame;

import net.sourceforge.kolmafia.request.ClanStashRequest;
import net.sourceforge.kolmafia.request.ClosetRequest;
import net.sourceforge.kolmafia.request.CreateItemRequest;
import net.sourceforge.kolmafia.request.EquipmentRequest;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.request.HermitRequest;
import net.sourceforge.kolmafia.request.StorageRequest;
import net.sourceforge.kolmafia.request.UntinkerRequest;
import net.sourceforge.kolmafia.request.UseItemRequest;

import net.sourceforge.kolmafia.persistence.AdventureDatabase;
import net.sourceforge.kolmafia.persistence.ConcoctionDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.persistence.MallPriceDatabase;
import net.sourceforge.kolmafia.persistence.NPCStoreDatabase;
import net.sourceforge.kolmafia.persistence.Preferences;

import net.sourceforge.kolmafia.textui.Interpreter;

public abstract class InventoryManager
{
	private static final int BULK_PURCHASE_AMOUNT = 30;
	private static final GenericRequest FAMEQUIP_REMOVER = new GenericRequest( "familiar.php?pwd&action=unequip" );

	public static void resetInventory()
	{
		KoLConstants.inventory.clear();
	}

	public static final int getCount( final int itemId )
	{
		AdventureResult item = ItemPool.get( itemId, 1 );
		
		return item.getCount( KoLConstants.inventory );
	}

	public static final boolean hasItem( final int itemId )
	{
		return InventoryManager.hasItem( itemId, false );
	}
	
	public static final boolean hasItem( final int itemId, boolean shouldCreate )
	{
		return InventoryManager.hasItem( ItemPool.get( itemId, 1 ), shouldCreate );
	}

	public static final boolean hasItem( final AdventureResult item )
	{
		return InventoryManager.hasItem( item, false );
	}
	
	public static final boolean hasItem( final AdventureResult item, final boolean shouldCreate )
	{
		int count = InventoryManager.getAccessibleCount( item );

		if ( shouldCreate )
		{
			CreateItemRequest creation = CreateItemRequest.getInstance( item );
			if ( creation != null )
			{
				count += creation.getQuantityPossible();
			}
		}

		return count > 0 && count >= item.getCount();
	}

	public static final int getAccessibleCount( final int itemId )
	{
		return InventoryManager.getAccessibleCount( ItemPool.get( itemId, 1 ) );
	}
	
	public static final int getAccessibleCount( final AdventureResult item )
	{
		if ( item == null )
		{
			return 0;
		}

		int count = item.getCount( KoLConstants.inventory ) + item.getCount( KoLConstants.closet ) + item.getCount( KoLConstants.freepulls );

		if ( KoLCharacter.canInteract() )
		{
			count += item.getCount( KoLConstants.storage );

			if ( KoLCharacter.hasClan() && Preferences.getBoolean( "autoSatisfyWithStash" ) )
			{
				count += item.getCount( ClanManager.getStash() );
			}
		}
		
		for ( int i = 0; i <= EquipmentManager.FAMILIAR; ++i )
		{
			AdventureResult equipment = EquipmentManager.getEquipment( i );
			
			if ( equipment != null && equipment.getItemId() == item.getItemId() )
			{
				++count;
			}
		}
		
		return count;
	}
		
	public static final boolean retrieveItem( final int itemId )
	{
		return retrieveItem( ItemPool.get( itemId, 1 ) );
	}

	public static final boolean retrieveItem( final int itemId, int count )
	{
		return retrieveItem( ItemPool.get( itemId, count ) );
	}

	public static final boolean retrieveItem( final String itemName )
	{
		return retrieveItem( ItemPool.get( itemName, 1 ), true );
	}

	public static final boolean retrieveItem( final String itemName, int count )
	{
		return retrieveItem( ItemPool.get( itemName, count ), true );
	}

	public static final boolean retrieveItem( final AdventureResult item )
	{
		return retrieveItem( item, true );
	}

	public static final boolean retrieveItem( final AdventureResult item, final boolean isAutomated )
	{
		int itemId = item.getItemId();
		int availableCount = 0;

		if ( itemId == HermitRequest.WORTHLESS_ITEM.getItemId() )
		{
			availableCount = HermitRequest.getWorthlessItemCount();
		}
		else
		{
			availableCount = item.getCount( KoLConstants.inventory );
		}

		int missingCount = item.getCount() - availableCount;

		// If you already have enough of the given item, then return
		// from this method.

		if ( missingCount <= 0 )
		{
			return true;
		}

		for ( int i = EquipmentManager.HAT; i <= EquipmentManager.FAMILIAR; ++i )
		{
			if ( EquipmentManager.getEquipment( i ).equals( item ) )
			{
				SpecialOutfit.forgetEquipment( item );
				RequestThread.postRequest( new EquipmentRequest( EquipmentRequest.UNEQUIP, i ) );
				--missingCount;
				if ( missingCount <= 0 )
				{
					return true;
				}
			}
		}

		for ( int i = 0; i < KoLCharacter.getFamiliarList().size(); ++i )
		{
			FamiliarData current = (FamiliarData) KoLCharacter.getFamiliarList().get( i );

			if ( current.getItem() != null && current.getItem().equals( item ) )
			{
				KoLmafia.updateDisplay( "Stealing " + item.getName() + " from " + current.getName() + " the " + current.getRace() + "..." );
				InventoryManager.FAMEQUIP_REMOVER.addFormField( "famid", String.valueOf( current.getId() ) );
				RequestThread.postRequest( InventoryManager.FAMEQUIP_REMOVER );

				--missingCount;

				if ( missingCount <= 0 )
				{
					return true;
				}
			}
		}

		// Retrieve worthless items either by reading scrolls or
		// adventuring in the sewer.

		if ( itemId == HermitRequest.WORTHLESS_ITEM.getItemId() )
		{
			return InventoryManager.retrieveWorthlessItems( item );
		}

		// Handle the bridge by untinkering the abridged dictionary

		if ( itemId == ItemPool.BRIDGE )
		{
			if ( InventoryManager.hasItem( ItemPool.ABRIDGED ) )
			{
				RequestThread.postRequest( new UntinkerRequest( ItemPool.ABRIDGED, 1 ) );
			}
			return item.getCount( KoLConstants.inventory ) > 0;
		}

		// First, attempt to pull the item from the closet.
		// If this is successful, return from the method.

		boolean shouldUseCloset = Preferences.getBoolean( "autoSatisfyWithCloset" );
		int itemCount = 0;

		if ( shouldUseCloset )
		{
			itemCount = item.getCount( KoLConstants.closet );

			if ( itemCount > 0 )
			{
				RequestThread.postRequest( new ClosetRequest(
								   ClosetRequest.CLOSET_TO_INVENTORY, new AdventureResult[] { item.getInstance( Math.min(
																			itemCount, missingCount ) ) } ) );
				missingCount = item.getCount() - item.getCount( KoLConstants.inventory );

				if ( missingCount <= 0 )
				{
					return true;
				}
			}
		}

		// Next, if the item is a free pull from Hagnk's, pull it
		// If this is successful, return from the method.

		itemCount = item.getCount( KoLConstants.freepulls );

		if ( itemCount > 0 )
		{
			RequestThread.postRequest( new StorageRequest(
				StorageRequest.FREEPULL_TO_INVENTORY, new AdventureResult[] { item.getInstance( Math.min(
					itemCount, missingCount ) ) } ) );
			missingCount = item.getCount() - item.getCount( KoLConstants.inventory );

			if ( missingCount <= 0 )
			{
				return true;
			}
		}

		// Next, attempt to pull the items out of storage, if you are
		// out of ronin.

		if ( KoLCharacter.canInteract() )
		{
			itemCount = item.getCount( KoLConstants.storage );

			if ( itemCount > 0 )
			{
				RequestThread.postRequest( new StorageRequest(
					StorageRequest.STORAGE_TO_INVENTORY, new AdventureResult[] { item.getInstance( itemCount ) } ) );

				missingCount = item.getCount() - item.getCount( KoLConstants.inventory );
				if ( missingCount <= 0 )
				{
					return true;
				}
			}
		}

		// See if the item can be retrieved from the clan stash.  If it
		// can, go ahead and pull as many items as possible from there.

		boolean shouldUseStash = Preferences.getBoolean( "autoSatisfyWithStash" );
		if ( shouldUseStash && KoLCharacter.canInteract() && KoLCharacter.hasClan() )
		{
			if ( !ClanManager.isStashRetrieved() )
			{
				RequestThread.postRequest( new ClanStashRequest() );
			}

			itemCount = item.getCount( ClanManager.getStash() );
			if ( itemCount > 0 )
			{
				RequestThread.postRequest( new ClanStashRequest(
					new AdventureResult[] { item.getInstance( Math.min( itemCount, getPurchaseCount(
						itemId, missingCount ) ) ) }, ClanStashRequest.STASH_TO_ITEMS ) );

				missingCount = item.getCount() - item.getCount( KoLConstants.inventory );

				if ( missingCount <= 0 )
				{
					return true;
				}
			}
		}

		// Next, attempt to create the item from existing ingredients
		// (if possible).

		boolean scriptSaysBuy = false;

		CreateItemRequest creator = CreateItemRequest.getInstance( item );
		if ( creator != null && creator.getQuantityPossible() > 0 )
		{
			scriptSaysBuy = invokeBuyScript( item, missingCount, 2, false );
			missingCount = item.getCount() - item.getCount( KoLConstants.inventory );

			if ( missingCount <= 0 )
			{
				return true;
			}

			if ( !scriptSaysBuy )
			{
				creator.setQuantityNeeded(
					Math.min( missingCount, creator.getQuantityPossible() ) );
				RequestThread.postRequest( creator );
				missingCount = item.getCount() - item.getCount( KoLConstants.inventory );
	
				if ( missingCount <= 0 )
				{
					return true;
				}
	
				if ( !KoLmafia.permitsContinue() )
				{
					return false;
				}
			}
		}

		// Next, hermit item retrieval is possible when
		// you have worthless items.  Use this method next.

		if ( KoLConstants.hermitItems.contains( item ) )
		{
			int worthlessItemCount = HermitRequest.getWorthlessItemCount();
			if ( worthlessItemCount > 0 )
			{
				RequestThread.postRequest( new HermitRequest( itemId, Math.min( worthlessItemCount, missingCount ) ) );
				missingCount = item.getCount() - item.getCount( KoLConstants.inventory );

				if ( missingCount <= 0 )
				{
					return true;
				}
			}
		}

		if ( KoLConstants.trapperItems.contains( item ) )
		{
			int furCount = CouncilFrame.YETI_FUR.getCount( KoLConstants.inventory );
			if ( furCount > 0 )
			{
				KoLmafia.updateDisplay( "Visiting the trapper..." );
				RequestThread.postRequest( new GenericRequest(
					"trapper.php?pwd&action=Yep.&whichitem=" + itemId + "&qty=" + Math.min( missingCount, furCount ) ) );

				missingCount = item.getCount() - item.getCount( KoLConstants.inventory );
				if ( missingCount <= 0 )
				{
					return true;
				}
			}
		}

		// Try to purchase the item from the mall, if the user wishes
		// to autosatisfy through purchases, and the item is not
		// creatable through combines.
		
		boolean shouldUseMall = shouldUseMall( item );

		if ( !scriptSaysBuy && shouldUseMall && !hasAnyIngredient( itemId ) )
		{
			scriptSaysBuy = creator == null ||
				invokeBuyScript( item, missingCount, 0, true );
			missingCount = item.getCount() - item.getCount( KoLConstants.inventory );

			if ( missingCount <= 0 )
			{
				return true;
			}
		}

		boolean shouldUseNPCStore =
			NPCStoreDatabase.contains( item.getName() ) && Preferences.getBoolean( "autoSatisfyWithNPCs" );

		if ( shouldUseNPCStore || scriptSaysBuy )
		{
			ArrayList results = StoreManager.searchMall( item );
			StaticEntity.getClient().makePurchases(
				results, results.toArray(), getPurchaseCount( itemId, missingCount ), isAutomated );
			StoreManager.updateMallPrice( item, results );

			missingCount = item.getCount() - item.getCount( KoLConstants.inventory );

			if ( missingCount <= 0 )
			{
				return true;
			}
		}

		// Use budgeted pulls if the item is available from storage.
		
		if ( !KoLCharacter.canInteract() && !KoLCharacter.isHardcore() )
		{
			int pullCount = Math.min( item.getCount( KoLConstants.storage ),
				ItemManageFrame.getPullsBudgeted() );
			if ( pullCount > 0 )
			{
				pullCount = Math.min( pullCount, item.getCount() );
				int newbudget = ItemManageFrame.getPullsBudgeted() - pullCount;
				RequestThread.postRequest( new StorageRequest(
					StorageRequest.STORAGE_TO_INVENTORY,
					new AdventureResult[] { item.getInstance( pullCount ) } ) );
				ItemManageFrame.setPullsBudgeted( newbudget );

				missingCount = item.getCount() - item.getCount( KoLConstants.inventory );
				if ( missingCount <= 0 )
				{
					return true;
				}
			}		
		}

		int mixingMethod = ConcoctionDatabase.getMixingMethod( item );
		switch ( mixingMethod )
		{
		// Sub-ingredients for star charts, pixels and malus
		// ingredients can get very expensive. Therefore, skip over
		// them in this step.

		case KoLConstants.NOCREATE:
		case KoLConstants.STARCHART:
		case KoLConstants.PIXEL:
		case KoLConstants.MALUS:
		case KoLConstants.STAFF:
		case KoLConstants.MULTI_USE:
			scriptSaysBuy = true;
			break;

		default:
			scriptSaysBuy = itemId == ItemPool.DOUGH ||
				itemId == ItemPool.DISASSEMBLED_CLOVER ||
				creator == null;
		}
		
		if ( creator != null && mixingMethod != KoLConstants.NOCREATE )
		{
			scriptSaysBuy = invokeBuyScript( item, missingCount, 1,
				scriptSaysBuy || ( shouldUseMall && cheaperToBuy( item, missingCount ) ) );
			missingCount = item.getCount() - item.getCount( KoLConstants.inventory );

			if ( missingCount <= 0 )
			{
				return true;
			}
		}

		// If it's creatable, and you have at least one ingredient, see
		// if you can make it via recursion.

		if ( creator != null && mixingMethod != KoLConstants.NOCREATE && !scriptSaysBuy )
		{
			creator.setQuantityNeeded( missingCount );
			RequestThread.postRequest( creator );

			missingCount = item.getCount() - item.getCount( KoLConstants.inventory );

			if ( missingCount <= 0 )
			{
				return true;
			}

			if ( !KoLmafia.permitsContinue() && isAutomated )
			{
				return false;
			}
		}

		if ( shouldUseMall )
		{
			ArrayList results = StoreManager.searchMall( item );
			StaticEntity.getClient().makePurchases(
				results, results.toArray(), getPurchaseCount( itemId, missingCount ), isAutomated );
			StoreManager.updateMallPrice( item, results );

			missingCount = item.getCount() - item.getCount( KoLConstants.inventory );

			if ( missingCount <= 0 )
			{
				return true;
			}
		}

		// If the item does not exist in sufficient quantities,
		// then notify the user that there aren't enough items
		// available to continue and cancel the request.

		KoLmafia.updateDisplay(
			KoLConstants.ERROR_STATE, "You need " + missingCount + " more " + item.getName() + " to continue." );

		return false;
	}
	
	private static boolean retrieveWorthlessItems( final AdventureResult item )
	{
		int count = HermitRequest.getWorthlessItemCount();
		int needed = item.getCount();

		while ( count < needed && InventoryManager.hasItem( HermitRequest.SUMMON_SCROLL ) )
		{
			// Read a single 31337 scroll
			RequestThread.postRequest( new UseItemRequest( HermitRequest.SUMMON_SCROLL ) );

			// If we now have a hermit script in inventory, read it
			if ( InventoryManager.hasItem( HermitRequest.HACK_SCROLL ) )
			{
				RequestThread.postRequest( new UseItemRequest( HermitRequest.HACK_SCROLL ) );
			}

			count = HermitRequest.getWorthlessItemCount();
		}

		if ( count >= needed )
		{
			return true;
		}

		// Handle worthless items by traveling to the sewer for as many
		// adventures as needed.

		ArrayList temporary = new ArrayList();
		temporary.addAll( KoLConstants.conditions );
		KoLConstants.conditions.clear();

		KoLConstants.conditions.add( item.getInstance( needed - count ) );
		StaticEntity.getClient().makeRequest(
			AdventureDatabase.getAdventureByURL( "sewer.php" ), KoLCharacter.getAdventuresLeft() );

		if ( !KoLConstants.conditions.isEmpty() )
		{
			KoLmafia.updateDisplay(
				KoLConstants.ABORT_STATE, "Unable to acquire " + item.getCount() + " worthless items." );
		}

		KoLConstants.conditions.clear();
		KoLConstants.conditions.addAll( temporary );

		count = HermitRequest.getWorthlessItemCount();
		return count >= needed;
	}
	
	private static boolean invokeBuyScript( AdventureResult item, int qty,
		int ingredientLevel, boolean defaultBuy )
	{
		String scriptName = Preferences.getString( "buyScript" );
		if ( scriptName.length() == 0 )
		{
			return defaultBuy;
		}
		Interpreter interpreter = KoLmafiaASH.getInterpreter(
			KoLmafiaCLI.findScriptFile( scriptName ) );
		if ( interpreter != null )
		{
			return interpreter.execute( "main", new String[]
			{
				item.getName(),
				String.valueOf( qty ),
				String.valueOf( ingredientLevel ),
				String.valueOf( defaultBuy )
			} ).intValue() != 0;
		}
		return defaultBuy;
	}
	
	private static boolean cheaperToBuy( AdventureResult item, int qty )
	{
		int mallprice = StoreManager.getMallPrice( item ) * qty;
		if ( mallprice <= 0 )
		{
			return false;
		}
		int makeprice = priceToMake( item, qty, 0 );
		if ( makeprice == Integer.MAX_VALUE )
		{
			return true;
		}
		return mallprice < makeprice;
	}
	
	private static int priceToAcquire( AdventureResult item, int qty, int level )
	{
		int price = 0;
		int onhand = Math.min( qty, item.getCount( KoLConstants.inventory ) );
		if ( onhand > 0 )
		{
			price = MallPriceDatabase.getPrice( item.getItemId() );
			if ( price <= 0 )
			{
				price = Math.abs( ItemDatabase.getPriceById( item.getItemId() ) );
			}
			price *= onhand;
			qty -= onhand;
			if ( qty == 0 )
			{
				return price;
			}
		}
		int mallprice = StoreManager.getMallPrice( item ) * qty;
		if ( mallprice <= 0 )
		{
			mallprice = Integer.MAX_VALUE;
		}
		else
		{
			mallprice += price;
		}
		int makeprice = priceToMake( item, qty, level );
		if ( makeprice != Integer.MAX_VALUE )
		{
			makeprice += price;
		}
		return Math.min( mallprice, makeprice );
	}

	private static int priceToMake( AdventureResult item, int qty, int level )
	{
		int id = item.getItemId();
		int price = 0;
		if ( level > 10 || !ConcoctionDatabase.isPermittedMethod(
			ConcoctionDatabase.getMixingMethod( item ) ) )
		{
			return Integer.MAX_VALUE;
		}
		int yield = ConcoctionDatabase.getYield( item.getItemId() );
		int madeqty = (qty + yield - 1) / yield;
		AdventureResult ingrs[] = ConcoctionDatabase.getIngredients( id );
		if ( ingrs.length == 0 )
		{
			return Integer.MAX_VALUE;
		}
		for ( int i = 0; i < ingrs.length; ++i )
		{
			AdventureResult ingr = ingrs[ i ];
			int needed = ingr.getCount() * madeqty;
			int ingrprice = priceToAcquire( ingr, needed, level + 1 );
			if ( ingrprice == Integer.MAX_VALUE ) return ingrprice;
			price += ingrprice;
		}
		return price * qty / (yield * madeqty);
	}
	
	private static int getPurchaseCount( final int itemId, final int missingCount )
	{
		if ( missingCount >= InventoryManager.BULK_PURCHASE_AMOUNT ||
			!KoLCharacter.canInteract() ||
			KoLCharacter.getAvailableMeat() < 5000 )
		{
			return missingCount;
		}

		if ( InventoryManager.shouldBulkPurchase( itemId ) )
		{
			return InventoryManager.BULK_PURCHASE_AMOUNT;
		}

		return missingCount;
	}

	private static final boolean hasAnyIngredient( final int itemId )
	{
		return hasAnyIngredient( itemId, null );
	}

	private static final boolean hasAnyIngredient( final int itemId, HashSet seen )
	{
		if ( itemId < 0 )
		{
			return false;
		}

		switch ( itemId )
		{
		case ItemPool.MEAT_PASTE:
			return KoLCharacter.getAvailableMeat() >= 10;
		case ItemPool.MEAT_STACK:
			return KoLCharacter.getAvailableMeat() >= 100;
		case ItemPool.DENSE_STACK:
			return KoLCharacter.getAvailableMeat() >= 1000;
		}

		AdventureResult[] ingredients = ConcoctionDatabase.getStandardIngredients( itemId );
		boolean shouldUseCloset = Preferences.getBoolean( "autoSatisfyWithCloset" );

		for ( int i = 0; i < ingredients.length; ++i )
		{
			AdventureResult ingredient = ingredients[ i ];
			// An item is immediately available if it is in your
			// inventory, or in your closet.

			if ( KoLConstants.inventory.contains( ingredient ) )
			{
				return true;
			}

			if ( shouldUseCloset && KoLConstants.closet.contains( ingredient ) )
			{
				return true;
			}
		}
		
		Integer key = new Integer( itemId );
		if ( seen == null )
		{
			seen = new HashSet();
		}
		else if ( seen.contains( key ) )
		{
			return false;
		}
		seen.add( key );

		for ( int i = 0; i < ingredients.length; ++i )
		{
			// An item is immediately available if you have the
			// ingredients for a substep.

			if ( InventoryManager.hasAnyIngredient( ingredients[ i ].getItemId(), seen ) )
			{
				return true;
			}
		}

		return false;
	}

	private static boolean shouldBulkPurchase( final int itemId )
	{
		// We always bulk purchase certain specific items.

		switch ( itemId )
		{
		case ItemPool.REMEDY: // soft green echo eyedrop antidote
		case ItemPool.TINY_HOUSE:
		case ItemPool.DRASTIC_HEALING:
		case ItemPool.ANTIDOTE:
			return true;
		}

		if ( !KoLmafia.isAdventuring() )
		{
			return false;
		}

		// We bulk purchase consumable items if we are
		// auto-adventuring.

		switch ( ItemDatabase.getConsumptionType( itemId ) )
		{
		case KoLConstants.MULTI_USE:
		case KoLConstants.HP_RESTORE:
		case KoLConstants.MP_RESTORE:
		case KoLConstants.HPMP_RESTORE:
			return true;
		}

		return false;
	}

	private static boolean shouldUseMall( final AdventureResult item )
	{
		if ( !KoLCharacter.canInteract() )
		{
			return false;
		}

		int itemId = item.getItemId();

		if ( !ItemDatabase.isTradeable( itemId ) )
		{
			return false;
		}

		if ( !Preferences.getBoolean( "autoSatisfyWithMall" ) )
		{
			return false;
		}

		int price = ItemDatabase.getPriceById( itemId );

		if ( price > 0 )
		{
			return true;
		}

		switch ( itemId )
		{
		case ItemPool.TEN_LEAF_CLOVER:
		case ItemPool.DISASSEMBLED_CLOVER:
		case ItemPool.PHIAL_OF_HOTNESS:
		case ItemPool.PHIAL_OF_COLDNESS:
		case ItemPool.PHIAL_OF_SPOOKINESS:
		case ItemPool.PHIAL_OF_STENCH:
		case ItemPool.PHIAL_OF_SLEAZINESS:
		case ItemPool.WHITE_RICE:
			// Love songs
		case ItemPool.VAGUE_AMBIGUITY:
		case ItemPool.SMOLDERING_PASSION:
		case ItemPool.ICY_REVENGE:
		case ItemPool.SUGARY_CUTENESS:
		case ItemPool.DISTURBING_OBSESSION:
		case ItemPool.NAUGHTY_INNUENDO:
			return true;
		}
		return false;
	}
}
