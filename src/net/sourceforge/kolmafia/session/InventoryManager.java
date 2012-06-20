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

package net.sourceforge.kolmafia.session;

import java.lang.ref.WeakReference;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.FamiliarData;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.KoLmafiaASH;
import net.sourceforge.kolmafia.KoLmafiaCLI;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.SpecialOutfit;
import net.sourceforge.kolmafia.StaticEntity;

import net.sourceforge.kolmafia.objectpool.Concoction;
import net.sourceforge.kolmafia.objectpool.ConcoctionPool;
import net.sourceforge.kolmafia.objectpool.IntegerPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;

import net.sourceforge.kolmafia.persistence.ConcoctionDatabase;
import net.sourceforge.kolmafia.persistence.EquipmentDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.persistence.NPCStoreDatabase;

import net.sourceforge.kolmafia.preferences.PreferenceListener;
import net.sourceforge.kolmafia.preferences.PreferenceListenerRegistry;
import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.request.ApiRequest;
import net.sourceforge.kolmafia.request.ClanStashRequest;
import net.sourceforge.kolmafia.request.ClosetRequest;
import net.sourceforge.kolmafia.request.CombineMeatRequest;
import net.sourceforge.kolmafia.request.CreateItemRequest;
import net.sourceforge.kolmafia.request.EquipmentRequest;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.request.HermitRequest;
import net.sourceforge.kolmafia.request.StorageRequest;
import net.sourceforge.kolmafia.request.TrendyRequest;
import net.sourceforge.kolmafia.request.UntinkerRequest;
import net.sourceforge.kolmafia.request.UseItemRequest;

import net.sourceforge.kolmafia.swingui.GenericFrame;

import net.sourceforge.kolmafia.textui.Interpreter;

import net.sourceforge.kolmafia.utilities.InputFieldUtilities;
import net.sourceforge.kolmafia.utilities.StringUtilities;

import org.json.JSONException;
import org.json.JSONObject;

public abstract class InventoryManager
{
	private static final int BULK_PURCHASE_AMOUNT = 30;
	private static final GenericRequest FAMEQUIP_REMOVER = new GenericRequest( "familiar.php?pwd&action=unequip" );

	private static final ArrayListArray listeners = new ArrayListArray();
	private static int askedAboutCrafting = 0;

	public static void resetInventory()
	{
		KoLConstants.inventory.clear();
	}

	public static void refresh()
	{
		// Retrieve the contents of the closet and inventory
		// We can detect new items in either location
		try
		{
			KoLmafia.setIsRefreshing( true );
			InventoryManager.resetInventory();
			ConcoctionDatabase.deferRefresh( true );
			RequestThread.postRequest( new ClosetRequest() );
			RequestThread.postRequest( new ApiRequest( "inventory" ) );
		}
		finally
		{
			EquipmentManager.updateEquipmentLists();
			ConcoctionDatabase.deferRefresh( false );
			KoLmafia.setIsRefreshing( false );
			// update "Hatter" daily deed
			PreferenceListenerRegistry.firePreferenceChanged( "(hats)" );
		}
	}

	public static final int getCount( final int itemId )
	{
		return InventoryManager.getCount( ItemPool.get( itemId, 1 ) );
	}

	public static final int getCount( final AdventureResult item )
	{
		return item.getCount( KoLConstants.inventory );
	}

	public static final boolean hasItem( final int itemId )
	{
		return InventoryManager.hasItem( itemId, false );
	}

	public static final boolean hasItem( final int itemId, final boolean shouldCreate )
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

		int itemId = item.getItemId();
		boolean trendy = !KoLCharacter.isTrendy() || TrendyRequest.isTrendy( "Items", item.getName() );

		// Agree with what retrieveItem looks at
		if ( itemId == HermitRequest.WORTHLESS_ITEM.getItemId() )
		{
			return HermitRequest.getWorthlessItemCount( true );
		}

		int count = item.getCount( KoLConstants.inventory );

		// Items in closet might be accessible, but if the user has
		// marked items in the closet as out-of-bounds, honor that.
		if ( Preferences.getBoolean( "autoSatisfyWithCloset" ) )
		{
			count += item.getCount( KoLConstants.closet );
		}

		// Free Pulls from Hagnk's are accessible, except untrendy items for Trendy characters
		if ( trendy )
		{
			count += item.getCount( KoLConstants.freepulls );
		}

		if ( KoLCharacter.canInteract() && trendy )
		{
			// Storage is always accessible once you are out of
			// Ronin or have freed the king, but, again, the user
			// can mark it as out-of-bounds
			if ( Preferences.getBoolean( "autoSatisfyWithStorage" ) )
			{
				count += item.getCount( KoLConstants.storage );
			}

			if ( KoLCharacter.hasClan() && Preferences.getBoolean( "autoSatisfyWithStash" ) )
			{
				count += item.getCount( ClanManager.getStash() );
			}
		}

		count += InventoryManager.getEquippedCount( item );

		for ( int i = 0; i < KoLCharacter.getFamiliarList().size(); ++i )
		{
			FamiliarData current = (FamiliarData) KoLCharacter.getFamiliarList().get( i );

			if ( !current.equals( KoLCharacter.getFamiliar() ) &&
				current.getItem() != null && current.getItem().equals( item ) )
			{
				++count;
			}
		}

		return count;
	}

	public static final int getEquippedCount( final AdventureResult item )
	{
		int count = 0;
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
		return InventoryManager.retrieveItem( ItemPool.get( itemId, 1 ), true );
	}

	public static final boolean retrieveItem( final int itemId, final boolean isAutomated )
	{
		return InventoryManager.retrieveItem( ItemPool.get( itemId, 1 ), isAutomated );
	}

	public static final boolean retrieveItem( final int itemId, final int count )
	{
		return InventoryManager.retrieveItem( ItemPool.get( itemId, count ), true );
	}

	public static final boolean retrieveItem( final int itemId, final int count, final boolean isAutomated )
	{
		return InventoryManager.retrieveItem( ItemPool.get( itemId, count ), isAutomated );
	}

	public static final boolean retrieveItem( final String itemName )
	{
		return InventoryManager.retrieveItem( ItemPool.get( itemName, 1 ), true );
	}

	public static final boolean retrieveItem( final String itemName, final boolean isAutomated )
	{
		return InventoryManager.retrieveItem( ItemPool.get( itemName, 1 ), isAutomated );
	}

	public static final boolean retrieveItem( final String itemName, final int count )
	{
		return InventoryManager.retrieveItem( ItemPool.get( itemName, count ), true );
	}

	public static final boolean retrieveItem( final String itemName, final int count, final boolean isAutomated )
	{
		return InventoryManager.retrieveItem( ItemPool.get( itemName, count ), isAutomated );
	}

	public static final boolean retrieveItem( final AdventureResult item )
	{
		return InventoryManager.retrieveItem( item, true );
	}

	public static final boolean retrieveItem( final AdventureResult item, final boolean isAutomated )
	{
		String rv = InventoryManager.retrieveItem( item, isAutomated, false );
		if ( rv == null )
		{
			return false;
		}
		if ( rv.equals( "" ) )
		{
			if ( EquipmentDatabase.isHat( item ) )
			{
				PreferenceListenerRegistry.firePreferenceChanged( "(hats)" );
			}
			return true;
		}
		RequestLogger.printLine( "INTERNAL ERROR: retrieveItem returned string when not simulating!" );
		return true;
	}

	public static final String simRetrieveItem( final int itemId )
	{
		return InventoryManager.simRetrieveItem( ItemPool.get( itemId, 1 ), true );
	}

	public static final String simRetrieveItem( final int itemId, final boolean isAutomated )
	{
		return InventoryManager.simRetrieveItem( ItemPool.get( itemId, 1 ), isAutomated );
	}

	public static final String simRetrieveItem( final String itemName )
	{
		return InventoryManager.simRetrieveItem( ItemPool.get( itemName, 1 ), true );
	}

	public static final String simRetrieveItem( final String itemName, final boolean isAutomated )
	{
		return InventoryManager.simRetrieveItem( ItemPool.get( itemName, 1 ), isAutomated );
	}

	public static final String simRetrieveItem( final AdventureResult item )
	{
		return InventoryManager.simRetrieveItem( item, true );
	}

	public static final String simRetrieveItem( final AdventureResult item, final boolean isAutomated )
	{
		String rv = InventoryManager.retrieveItem( item, isAutomated, true );
		if ( rv == null || rv.equals( "" ) )
		{
			RequestLogger.printLine( "INTERNAL ERROR: retrieveItem didn't return string when simulating!" );
			return "buggy";
		}
		return rv;
	}

	private static final String retrieveItem( final AdventureResult item, final boolean isAutomated, final boolean sim )
	{
		// Disable any preferences that might prevent us from acquiring the item

		boolean cloverProtectActive = Preferences.getBoolean( "cloverProtectActive" );
		String rv;

		try
		{
			Preferences.setBoolean( "cloverProtectActive", false );
			rv = InventoryManager.doRetrieveItem( item, isAutomated, sim );
		}
		finally
		{
			// Restore preferences back to what they were before calling retrieveItem
			Preferences.setBoolean( "cloverProtectActive", cloverProtectActive );
		}
		return rv;
	}

	// When called with sim=true, retrieveItem should return a non-empty string
	// indicating how at least some quantity of the item would be retrieved.
	// There are two distinguished return values: "have" indicates trivial
	// success, "fail" indicates unavoidable failure.  No side-effects, please!
	// When called with sim=false, it should return "" for success (equivalent
	// to the previous return value of true), null for failure (previously false).

	private static final String doRetrieveItem( final AdventureResult item, final boolean isAutomated, final boolean sim )
	{
		int itemId = item.getItemId();
		boolean trendy = !KoLCharacter.isTrendy() || TrendyRequest.isTrendy( "Items", item.getName() );
		CreateItemRequest creator = CreateItemRequest.getInstance( item );
		Concoction concoction = ConcoctionPool.get( item.getName() );
		boolean asked = false;

		if ( itemId < 0 )
		{
			// See if it is a Coin Master token.
			String property = concoction != null ? concoction.property : null;
			if ( property == null )
			{
				if ( sim )
				{
					return "fail";
				}

				KoLmafia.updateDisplay(
					MafiaState.ERROR, "Don't know how to retrieve a " + item.getName() );
				return null;
			}

			int have = Preferences.getInteger( property );
			int need = item.getCount() - have;
			if ( need > 0 )
			{
				if ( sim )
				{
					return "fail";
				}

				KoLmafia.updateDisplay(
					MafiaState.ERROR, "You need " + need + " more " + item.getName() + " to continue." );
				return null;
			}

			if ( sim )
			{
				return "have";
			}

			return "";
		}

		if ( itemId == 0 )
		{
			if ( sim )
			{
				return "pretend to have";
			}

			return "";
		}

		if ( itemId == HermitRequest.WORTHLESS_ITEM.getItemId() )
		{
			// Retrieve worthless items using special techniques.
			if ( sim )
			{
				return "chewing gum";
			}

			return InventoryManager.retrieveWorthlessItems( item ) ? "" : null;
		}

		int availableCount = item.getCount( KoLConstants.inventory );
		int missingCount = item.getCount() - availableCount;

		// If you already have enough of the given item, then return
		// from this method.

		if ( missingCount <= 0 )
		{
			if ( sim )
			{
				return "have";
			}

			return "";
		}

		for ( int i = EquipmentManager.HAT; i <= EquipmentManager.FAMILIAR; ++i )
		{
			if ( EquipmentManager.getEquipment( i ).equals( item ) )
			{
				if ( sim )
				{
					return "remove";
				}

				SpecialOutfit.forgetEquipment( item );
				RequestThread.postRequest( new EquipmentRequest( EquipmentRequest.UNEQUIP, i ) );
				--missingCount;

				if ( missingCount <= 0 )
				{
					return "";
				}
			}
		}

		for ( int i = 0; i < KoLCharacter.getFamiliarList().size(); ++i )
		{
			FamiliarData current = (FamiliarData) KoLCharacter.getFamiliarList().get( i );

			if ( current.getItem() != null && current.getItem().equals( item ) )
			{
				if ( sim )
				{
					return "steal";
				}

				KoLmafia.updateDisplay( "Stealing " + item.getName() + " from " + current.getName() + " the " + current.getRace() + "..." );
				InventoryManager.FAMEQUIP_REMOVER.addFormField( "famid", String.valueOf( current.getId() ) );
				RequestThread.postRequest( InventoryManager.FAMEQUIP_REMOVER );

				--missingCount;

				if ( missingCount <= 0 )
				{
					return "";
				}
			}
		}

		// Handle the bridge by untinkering the abridged dictionary

		if ( itemId == ItemPool.BRIDGE )
		{
			if ( InventoryManager.hasItem( ItemPool.ABRIDGED ) )
			{
				if ( sim )
				{
					return "untinker";
				}

				RequestThread.postRequest( new UntinkerRequest( ItemPool.ABRIDGED, 1 ) );
			}

			if ( sim )
			{
				return "fail";
			}

			return item.getCount( KoLConstants.inventory ) > 0 ? "" : null;
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
				if ( sim )
				{
					return "uncloset";
				}

				int retrieveCount = Math.min( itemCount, missingCount );
				RequestThread.postRequest( new ClosetRequest(
					ClosetRequest.CLOSET_TO_INVENTORY, item.getInstance( retrieveCount ) ) );
				missingCount = item.getCount() - item.getCount( KoLConstants.inventory );

				if ( missingCount <= 0 )
				{
					return "";
				}
			}
		}

		// Next, if the item is a free pull from Hagnk's, pull it
		// If this is successful, return from the method.

		itemCount = item.getCount( KoLConstants.freepulls );

		if ( itemCount > 0 && trendy )
		{
			if ( sim )
			{
				return "free pull";
			}

			int retrieveCount = Math.min( itemCount, missingCount );
			RequestThread.postRequest( new StorageRequest(
				StorageRequest.STORAGE_TO_INVENTORY, item.getInstance( retrieveCount ) ) );
			missingCount = item.getCount() - item.getCount( KoLConstants.inventory );

			if ( missingCount <= 0 )
			{
				return "";
			}
		}

		// Next, attempt to pull the items out of storage, if you are
		// out of ronin and the user has not marked that location as
		// inaccessible.

		boolean shouldUseStorage = Preferences.getBoolean( "autoSatisfyWithStorage" ) && trendy;
		if ( shouldUseStorage && KoLCharacter.canInteract() )
		{
			itemCount = item.getCount( KoLConstants.storage );

			if ( itemCount > 0 )
			{
				if ( sim )
				{
					return "pull";
				}

				RequestThread.postRequest( new StorageRequest(
					StorageRequest.STORAGE_TO_INVENTORY, item.getInstance( itemCount ) ) );
				missingCount = item.getCount() - item.getCount( KoLConstants.inventory );

				if ( missingCount <= 0 )
				{
					return "";
				}
			}
		}

		// See if the item can be retrieved from the clan stash.  If it
		// can, go ahead and pull as many items as possible from there.

		boolean shouldUseStash = Preferences.getBoolean( "autoSatisfyWithStash" ) && trendy;
		if ( shouldUseStash && KoLCharacter.canInteract() && KoLCharacter.hasClan() )
		{
			if ( !ClanManager.isStashRetrieved() )
			{
				RequestThread.postRequest( new ClanStashRequest() );
			}

			itemCount = item.getCount( ClanManager.getStash() );

			if ( itemCount > 0 )
			{
				if ( sim )
				{
					return "unstash";
				}

				int retrieveCount = Math.min( itemCount, InventoryManager.getPurchaseCount( itemId, missingCount ) );
				RequestThread.postRequest( new ClanStashRequest(
					item.getInstance( retrieveCount ), ClanStashRequest.STASH_TO_ITEMS ) );
				missingCount = item.getCount() - item.getCount( KoLConstants.inventory );

				if ( missingCount <= 0 )
				{
					return "";
				}
			}
		}

		// Next, attempt to create the item from existing ingredients
		// (if possible).

		boolean shouldUseMall = InventoryManager.shouldUseMall( item ) && trendy;
		boolean scriptSaysBuy = false;

		if ( creator != null && creator.getQuantityPossible() > 0 )
		{
			if ( sim )
			{
				return shouldUseMall ? "create or buy" : "create";
			}

			boolean defaultBuy = shouldUseMall && InventoryManager.cheaperToBuy( item, missingCount );
			scriptSaysBuy = InventoryManager.invokeBuyScript( item, missingCount, 2, defaultBuy );
			missingCount = item.getCount() - item.getCount( KoLConstants.inventory );

			if ( missingCount <= 0 )
			{
				return "";
			}

			if ( !scriptSaysBuy )
			{
				// Prompt about adventures if we make it here.
				creator.setQuantityNeeded(
					Math.min( missingCount, creator.getQuantityPossible() ) );

				if ( isAutomated && concoction != null &&
					concoction.getAdventuresNeeded( missingCount, true ) > 0 )
				{
					asked = true;

					if ( !InventoryManager.allowTurnConsumption( creator ) )
					{
						return null;
					}
				}

				RequestThread.postRequest( creator );
				missingCount = item.getCount() - item.getCount( KoLConstants.inventory );

				if ( missingCount <= 0 )
				{
					return "";
				}

				if ( !KoLmafia.permitsContinue() )
				{
					return null;
				}
			}
		}

		// The ten-leaf clover can be created (by using a disassembled
		// clover) or purchased from the Hermit (if he has any in
		// stock. We tried the former above. Now try the latter.

		boolean shouldUseCoinmasters = Preferences.getBoolean( "autoSatisfyWithCoinmasters" );
		if ( itemId == ItemPool.TEN_LEAF_CLOVER && shouldUseCoinmasters )
		{
			itemCount = HermitRequest.cloverCount();

			if ( itemCount > 0 )
			{
				if ( sim )
				{
					return "hermit";
				}

				int retrieveCount = Math.min( itemCount, missingCount );
				RequestThread.postRequest( new HermitRequest( itemId, retrieveCount ) );
			}

			missingCount = item.getCount() - item.getCount( KoLConstants.inventory );
			if ( missingCount <= 0 )
			{
				return "";
			}
		}

		// Try to purchase the item from the mall, if the user wishes
		// to autosatisfy through purchases, and the item is not
		// creatable through combines.

		if ( !scriptSaysBuy && shouldUseMall && !InventoryManager.hasAnyIngredient( itemId ) )
		{
			if ( sim )
			{
				return "create or buy";
			}

			if ( creator == null )
			{
				scriptSaysBuy = true;
			}
			else
			{
				boolean defaultBuy = shouldUseMall && InventoryManager.cheaperToBuy( item, missingCount );
				scriptSaysBuy = InventoryManager.invokeBuyScript( item, missingCount, 0, defaultBuy );
				missingCount = item.getCount() - item.getCount( KoLConstants.inventory );

				if ( missingCount <= 0 )
				{
					return "";
				}
			}
		}

		boolean shouldUseNPCStore =
			Preferences.getBoolean( "autoSatisfyWithNPCs" ) &&
				NPCStoreDatabase.contains( item.getName() );

		if ( shouldUseNPCStore || scriptSaysBuy )
		{
			if ( sim )
			{
				return "buy";
			}

			ArrayList results = StoreManager.searchMall( item );
			StaticEntity.getClient().makePurchases(
				results, results.toArray(), InventoryManager.getPurchaseCount( itemId, missingCount ), isAutomated );
			StoreManager.updateMallPrice( item, results );

			missingCount = item.getCount() - item.getCount( KoLConstants.inventory );

			if ( missingCount <= 0 )
			{
				return "";
			}
		}

		// Use budgeted pulls if the item is available from storage.

		if ( !KoLCharacter.canInteract() && !KoLCharacter.isHardcore() && trendy )
		{
			int pullCount = Math.min( item.getCount( KoLConstants.storage ),
				ConcoctionDatabase.getPullsBudgeted() );

			if ( pullCount > 0 )
			{
				if ( sim )
				{
					return "pull";
				}

				pullCount = Math.min( pullCount, item.getCount() );
				int newbudget = ConcoctionDatabase.getPullsBudgeted() - pullCount;

				RequestThread.postRequest( new StorageRequest(
					StorageRequest.STORAGE_TO_INVENTORY, item.getInstance( pullCount ) ) );
				ConcoctionDatabase.setPullsBudgeted( newbudget );
				missingCount = item.getCount() - item.getCount( KoLConstants.inventory );

				if ( missingCount <= 0 )
				{
					return "";
				}
			}
		}

		int mixingMethod = ConcoctionDatabase.getMixingMethod( item ) & KoLConstants.CT_MASK;

		switch ( itemId )
		{
		case ItemPool.DOUGH:
		case ItemPool.DISASSEMBLED_CLOVER:
		case ItemPool.JOLLY_BRACELET:
			scriptSaysBuy = true;
			break;
		default:
			scriptSaysBuy = creator == null || mixingMethod == KoLConstants.NOCREATE;
			break;
		}

		if ( creator != null && mixingMethod != KoLConstants.NOCREATE )
		{
			if ( sim )
			{
				return shouldUseMall ? "create or buy" : "create";
			}

			boolean defaultBuy = scriptSaysBuy || shouldUseMall && InventoryManager.cheaperToBuy( item, missingCount );
			scriptSaysBuy = InventoryManager.invokeBuyScript( item, missingCount, 1, defaultBuy );
			missingCount = item.getCount() - item.getCount( KoLConstants.inventory );

			if ( missingCount <= 0 )
			{
				return "";
			}
		}

		// If it's creatable, and you have at least one ingredient, see
		// if you can make it via recursion.

		if ( creator != null && mixingMethod != KoLConstants.NOCREATE && !scriptSaysBuy )
		{
			boolean makeFromComponents = true;
			if ( isAutomated && creator.getQuantityPossible() > 0 )
			{
				// Speculate on how much the items needed to make the creation would cost.
				// Do not retrieve if the average meat spend to make one of the item
				// exceeds the user's autoBuyPriceLimit.
				float meatSpend = InventoryManager.priceToMake( item, missingCount, 0, true, true ) / missingCount;
				int autoBuyPriceLimit = Preferences.getInteger( "autoBuyPriceLimit" );
				if ( meatSpend > autoBuyPriceLimit )
				{
					makeFromComponents = false;
					KoLmafia.updateDisplay(
						MafiaState.ERROR,
						"The average amount of meat spent on components ("
							+ KoLConstants.COMMA_FORMAT.format( meatSpend )
							+ ") for one " + item.getName() + " exceeds autoBuyPriceLimit ("
							+ KoLConstants.COMMA_FORMAT.format( autoBuyPriceLimit ) + ")" );

					// If making it from components was cheaper than buying the final product, and we
					// couldn't afford to make it, don't bother trying to buy the final product.
					shouldUseMall = false;
				}
			}

			if ( makeFromComponents )
			{
				if ( sim )
				{
					return "create";
				}

				// Second place to check for adventure usage.  Make sure we didn't already ask above.
				creator.setQuantityNeeded( missingCount );

				if ( !asked && isAutomated && concoction != null && creator != null &&
					concoction.getAdventuresNeeded( missingCount, true ) > 0 )
				{
					asked = true;
					if ( !InventoryManager.allowTurnConsumption( creator ) )
					{
						return null;
					}
				}

				RequestThread.postRequest( creator );
				missingCount = item.getCount() - item.getCount( KoLConstants.inventory );

				if ( missingCount <= 0 )
				{
					return "";
				}

				if ( !KoLmafia.permitsContinue() && isAutomated )
				{
					return null;
				}
			}
		}

		if ( shouldUseMall )
		{
			if ( sim )
			{
				return "buy";
			}

			ArrayList results = StoreManager.searchMall( item );
			StaticEntity.getClient().makePurchases(
				results, results.toArray(), InventoryManager.getPurchaseCount( itemId, missingCount ), isAutomated );
			StoreManager.updateMallPrice( item, results );
			missingCount = item.getCount() - item.getCount( KoLConstants.inventory );

			if ( missingCount <= 0 )
			{
				return "";
			}
		}

		// If the item does not exist in sufficient quantities,
		// then notify the user that there aren't enough items
		// available to continue and cancel the request.

		if ( sim )
		{
			return "fail";
		}

		KoLmafia.updateDisplay(
			MafiaState.ERROR, "You need " + missingCount + " more " + item.getName() + " to continue." );

		return null;
	}

	// *** Start of worthless item handling

	private static final AdventureResult[] WORTHLESS_ITEMS = new AdventureResult[]
	{
		ItemPool.get( ItemPool.WORTHLESS_TRINKET, 1 ),
		ItemPool.get( ItemPool.WORTHLESS_GEWGAW, 1 ),
		ItemPool.get( ItemPool.WORTHLESS_KNICK_KNACK, 1 ),
	};

	private static final AdventureResult[] STARTER_ITEMS = new AdventureResult[]
	{
		// A hat and a weapon for all six classes
		ItemPool.get( ItemPool.SEAL_HELMET, 1 ),
		ItemPool.get( ItemPool.SEAL_CLUB, 1 ),
		ItemPool.get( ItemPool.HELMET_TURTLE, 1 ),
		ItemPool.get( ItemPool.TURTLE_TOTEM, 1 ),
		ItemPool.get( ItemPool.RAVIOLI_HAT, 1 ),
		ItemPool.get( ItemPool.PASTA_SPOON, 1 ),
		ItemPool.get( ItemPool.HOLLANDAISE_HELMET, 1 ),
		ItemPool.get( ItemPool.SAUCEPAN, 1 ),
		ItemPool.get( ItemPool.DISCO_MASK, 1 ),
		ItemPool.get( ItemPool.DISCO_BALL, 1 ),
		ItemPool.get( ItemPool.MARIACHI_HAT, 1 ),
		ItemPool.get( ItemPool.STOLEN_ACCORDION, 1 ),
		// One pair of pants
		ItemPool.get( ItemPool.OLD_SWEATPANTS, 1 ),
	};

	private static boolean retrieveWorthlessItems( final AdventureResult item )
	{
		int count = HermitRequest.getWorthlessItemCount( false );
		int needed = item.getCount();

		if ( count >= needed )
		{
			return true;
		}

		// Figure out if you already have enough items in the closet
		if ( Preferences.getBoolean( "autoSatisfyWithCloset" ) )
		{
			InventoryManager.transferWorthlessItems( false );
			count = HermitRequest.getWorthlessItemCount();

			if ( count >= needed )
			{
				return true;
			}
		}

		// Figure out if you already have enough items in storage
		if ( Preferences.getBoolean( "autoSatisfyWithStorage" ) && KoLCharacter.canInteract() )
		{
			InventoryManager.pullWorthlessItems();
			count = HermitRequest.getWorthlessItemCount();

			if ( count >= needed )
			{
				return true;
			}
		}

		while ( count < needed && InventoryManager.hasItem( HermitRequest.SUMMON_SCROLL ) )
		{
			// Read a single 31337 scroll
			RequestThread.postRequest( UseItemRequest.getInstance( HermitRequest.SUMMON_SCROLL ) );

			// If we now have a hermit script in inventory, read it
			if ( InventoryManager.hasItem( HermitRequest.HACK_SCROLL ) )
			{
				RequestThread.postRequest( UseItemRequest.getInstance( HermitRequest.HACK_SCROLL ) );
			}

			count = HermitRequest.getWorthlessItemCount();
		}

		if ( count >= needed )
		{
			return true;
		}

		// Do not refresh concoctions while we transfer sewer items around.
		ConcoctionDatabase.deferRefresh( true );

		// If the character has any of the starter items, retrieve them to improve
		// the probability of getting worthless items.

		int missingStarterItemCount = InventoryManager.STARTER_ITEMS.length - InventoryManager.getStarterItemCount();

		if ( missingStarterItemCount > 0 )
		{
			InventoryManager.transferChewingGumItems( InventoryManager.STARTER_ITEMS, true, false );
			missingStarterItemCount = InventoryManager.STARTER_ITEMS.length - InventoryManager.getStarterItemCount();
		}

		// If you can interact with players, use the server-friendlier version of gum
		// retrieval that pre-computes a total amount of gum and retrieves it all
		// at once to start.

		if ( KoLCharacter.canInteract() )
		{
			// To save server hits, retrieve all the gum needed rather than constantly
			// purchase small amounts of gum.

			int totalGumCount = missingStarterItemCount + needed - count;

			if ( InventoryManager.retrieveItem( ItemPool.CHEWING_GUM, totalGumCount ) )
			{
				if ( needed - count <= 3 )
				{
					InventoryManager.transferWorthlessItems( true );
					RequestThread.postRequest( UseItemRequest.getInstance( ItemPool.CHEWING_GUM, totalGumCount ) );
				}
				else
				{
					while ( needed - count > 0 )
					{
						int gumCount =
							missingStarterItemCount == 0 ? Math.min( needed - count, 3 ) : missingStarterItemCount + 3;

						// Put the worthless items into the closet and then use the chewing gum

						int closetCount = InventoryManager.transferWorthlessItems( true );
						RequestThread.postRequest( UseItemRequest.getInstance( ItemPool.CHEWING_GUM, gumCount ) );

						// Recalculate how many worthless items are still needed and how many starter
						// items are now present in the inventory (if it's zero already, no additional
						// computations are needed).

						int inventoryCount = HermitRequest.getWorthlessItemCount();
						count = inventoryCount + closetCount;

						if ( missingStarterItemCount != 0 )
						{
							missingStarterItemCount =
								InventoryManager.STARTER_ITEMS.length - InventoryManager.getStarterItemCount();
						}
					}
				}

				// Pull the worthless items back out of the closet.

				count = InventoryManager.transferWorthlessItems( false );
			}
		}

		// Otherwise, go ahead and hit the server a little harder in order to retrieve
		// the worthless items.

		else
		{
			if ( InventoryManager.retrieveItem( ItemPool.CHEWING_GUM, needed - count ) )
			{
				while ( count < needed )
				{
					int gumUseCount = 1;

					// If you are missing at most one starter item, it is optimal
					// to use three chewing gums instead of one.

					if ( missingStarterItemCount <= 1 )
					{
						gumUseCount = Math.min( needed - count, 3 );
					}

					AdventureResult gum = ItemPool.get( ItemPool.CHEWING_GUM, gumUseCount );

					if ( gum.getCount( KoLConstants.inventory ) < gumUseCount &&
						!InventoryManager.retrieveItem( ItemPool.CHEWING_GUM, needed - count ) )
					{
						break;
					}

					// Closet your existing worthless items (since they will reduce
					// the probability of you getting more) and then use the gum.

					int closetCount = InventoryManager.transferWorthlessItems( true );
					RequestThread.postRequest( UseItemRequest.getInstance( gum ) );
					int inventoryCount = HermitRequest.getWorthlessItemCount();

					count = inventoryCount + closetCount;
				}

				// Pull the worthless items back out of the closet.

				count = InventoryManager.transferWorthlessItems( false );
			}
		}

		// Now that we have (possibly) gotten more sewer items, refresh
		ConcoctionDatabase.deferRefresh( false );

		if ( count < needed )
		{
			KoLmafia.updateDisplay(
				MafiaState.ABORT, "Unable to acquire " + item.getCount() + " worthless items." );
		}

		return count >= needed;
	}

	private static int getStarterItemCount()
	{
		int starterItemCount = 0;

		for ( int i = 0; i < InventoryManager.STARTER_ITEMS.length; ++i )
		{
			AdventureResult item = InventoryManager.STARTER_ITEMS[ i ];
			if ( item.getCount( KoLConstants.inventory ) > 0 || KoLCharacter.hasEquipped( item ) )
			{
				++starterItemCount;
			}
		}

		return starterItemCount;
	}

	private static void transferChewingGumItems(
		final AdventureResult[] items, final boolean moveOne, final boolean moveToCloset )
	{
		List source = moveToCloset ? KoLConstants.inventory : KoLConstants.closet;
		List destination = moveToCloset ? KoLConstants.closet : KoLConstants.inventory;

		List attachmentList = new ArrayList();

		for ( int i = 0; i < items.length; ++i )
		{
			AdventureResult item = items[ i ];

			if ( moveOne && !moveToCloset && ( item.getCount( destination ) > 0 || KoLCharacter.hasEquipped( item ) ) )
			{
				continue;
			}

			int itemCount = item.getCount( source );

			if ( itemCount > 0 )
			{
				attachmentList.add( ItemPool.get( item.getItemId(), moveOne ? 1 : itemCount ) );
			}
		}

		if ( !attachmentList.isEmpty() )
		{
			int moveType = moveToCloset ? ClosetRequest.INVENTORY_TO_CLOSET : ClosetRequest.CLOSET_TO_INVENTORY;
			RequestThread.postRequest( new ClosetRequest( moveType, attachmentList.toArray() ) );
		}
	}

	private static int transferWorthlessItems( final boolean moveToCloset )
	{
		InventoryManager.transferChewingGumItems( InventoryManager.WORTHLESS_ITEMS, false, moveToCloset );

		List destination = moveToCloset ? KoLConstants.closet : KoLConstants.inventory;

		int trinketCount = HermitRequest.TRINKET.getCount( destination );
		int gewgawCount = HermitRequest.GEWGAW.getCount( destination );
		int knickKnackCount = HermitRequest.KNICK_KNACK.getCount( destination );

		return trinketCount + gewgawCount + knickKnackCount;
	}

	private static int pullWorthlessItems()
	{
		int trinketCount = HermitRequest.TRINKET.getCount( KoLConstants.storage );
		int gewgawCount = HermitRequest.GEWGAW.getCount( KoLConstants.storage );
		int knickKnackCount = HermitRequest.KNICK_KNACK.getCount( KoLConstants.storage );

		AdventureResult[] items =
			new AdventureResult[]
			{
				HermitRequest.TRINKET.getInstance( trinketCount ),
				HermitRequest.GEWGAW.getInstance( gewgawCount ),
				HermitRequest.KNICK_KNACK.getInstance( knickKnackCount )
			};

		RequestThread.postRequest( new StorageRequest( StorageRequest.STORAGE_TO_INVENTORY, items ) );

		return trinketCount + gewgawCount + knickKnackCount;
	}

	/*
	  16 possible sewer items:

	  6 classes * 1 hat
	  6 classes * 1 weapon
	  1 pants
	  3 worthless items

	  Items can be in inventory or equipped.

	  Unless you have all 16 possible items, using a piece of gum will
	  retrieve one you don't have yet. If you have all the non-worthless
	  items, you are guaranteed to get a worthless item. If are missing
	  some non-worthless items, whether you get a worthless item or one of
	  the missing non-worthless-items is probabilistic.

	  Assume you have no worthless items in inventory
	  Let X = number of non-worthless sewer items you have.
	  Given X, what is the expected # of gums needed to get a worthless item?

	  Consider X = 13. Of the ( 16 - 13 ) = 3 possible items, 3 are your
	  goal and ( 3 - 3 ) = 0 are not your goal.

	  E(13) = 3/3 * 1 + 0/3 = 1.0

	  Consider X = 12. Of the ( 16 - 12 ) = 4 possible items, 3 are your
	  goal and ( 4 - 3 ) = 1 are not your goal. You have a 3/4 chance of
	  getting your goal with the first piece of gum. If you don't get one,
	  you have used 1 gum, now have 13 sewer items and will use another
	  piece of gum.

	  E(12) = 3/4 * 1 + 1/4 * ( 1 + E(13) ) = .75 + 0.50 = 1.25

	  Consider X = 11. Of the ( 16 - 11 ) = 5 possible items, 3 are your
	  goal and ( 5 - 3 ) = 2 are not your goal. You have a 3/5 chance of
	  getting your goal with the first piece of gum. If you don't get one,
	  you have used 1 gum, now have 12 sewer items and will use another
	  piece of gum.

	  E(11) = 3/5 * 1 + 2/5 * ( 1 + E(12) ) = .60 + 0.90 = 1.50

	  This generalizes:

	  E(X) = 3/(16-X) + (13-X)/(16-X) * ( 1 + E(X + 1 ) )

	  Rearranging terms:

	  E(X) = 1 + ( 13 - X ) * E( X + 1 ) / ( 16 - X )

	  This little ASH program calculates this:

	  float [14] factors;

	  factors[ 13 ] = 1.0;
	  for x from 12 downto 0
	  {
		float f2 = ( 13.0 - x ) * factors[ x + 1] / (16.0 - x );
		factors[ x ] = 1.0 + f2;
	  }

	  for i from 0 to 13
	  {
		float px = factors[ i ] ;
		print( i + ": " + px + " gum = " + ceil( 50.0 * px ) + " Meat" );
	  }

	  Resulting in this:

	  0: 4.25 gum = 213 Meat
	  1: 4.0 gum = 200 Meat
	  2: 3.75 gum = 188 Meat
	  3: 3.5 gum = 175 Meat
	  4: 3.25 gum = 163 Meat
	  5: 3.0 gum = 150 Meat
	  6: 2.75 gum = 138 Meat
	  7: 2.5 gum = 125 Meat
	  8: 2.25 gum = 113 Meat
	  9: 2.0 gum = 100 Meat
	  10: 1.75 gum = 88 Meat
	  11: 1.5 gum = 75 Meat
	  12: 1.25 gum = 63 Meat
	  13: 1.0 gum = 50 Meat

	  From this table, I derive the following formula for expected # of
	  chewing gum needed to retrieve a worthless item:

	  E(X) = ( 17 - X ) / 4
	  Cost(X) = 12.5 * ( 17 - X ) Meat
	 */

	public static int currentWorthlessItemCost()
	{
		int x = InventoryManager.getStarterItemCount();
		return (int) Math.ceil( 12.5 * ( 17 - x ) );
	}

	// *** End of worthless item handling

	private static boolean invokeBuyScript(
		final AdventureResult item, final int quantity, final int ingredientLevel, final boolean defaultBuy )
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
				String.valueOf( quantity ),
				String.valueOf( ingredientLevel ),
				String.valueOf( defaultBuy )
			} ).intValue() != 0;
		}
		return defaultBuy;
	}

	private static boolean cheaperToBuy( final AdventureResult item, final int quantity )
	{
		if ( !ItemDatabase.isTradeable( item.getItemId() ) )
		{
			return false;
		}

		int mallPrice = StoreManager.getMallPrice( item, 7.0f ) * quantity;
		if ( mallPrice <= 0 )
		{
			return false;
		}

		int makePrice = InventoryManager.priceToMake( item, quantity, 0, false );
		if ( makePrice == Integer.MAX_VALUE )
		{
			return true;
		}

		if ( mallPrice / 2 < makePrice && makePrice / 2 < mallPrice )
		{
			// Less than a 2:1 ratio, we should check more carefully
			mallPrice = StoreManager.getMallPrice( item ) * quantity;
			if ( mallPrice <= 0 )
			{
				return false;
			}

			makePrice = InventoryManager.priceToMake( item, quantity, 0, true );
			if ( makePrice == Integer.MAX_VALUE )
			{
				return true;
			}
		}

		if ( Preferences.getBoolean( "debugBuy" ) )
		{
			RequestLogger.printLine( "\u262F " + item.getInstance( quantity ) + " mall=" + mallPrice + " make=" + makePrice );
		}

		return mallPrice < makePrice;
	}

	private static int itemValue( final AdventureResult item, final boolean exact )
	{
		float factor = Preferences.getFloat( "valueOfInventory" );
		if ( factor <= 0.0f )
		{
			return 0;
		}

		int lower = 0;
		int autosell = ItemDatabase.getPriceById( item.getItemId() );
		int upper = Math.max( 0, autosell );

		if ( factor <= 1.0f )
		{
			return lower + (int) ( ( upper - lower ) * factor );
		}

		factor -= 1.0f;
		lower = upper;

		int mall = StoreManager.getMallPrice( item, exact ? 0.0f : 7.0f );
		if ( mall > Math.max( 100, 2 * Math.abs( autosell ) ) )
		{
			upper = Math.max( lower, mall );
		}

		if ( factor <= 1.0f )
		{
			return lower + (int) ( ( upper - lower ) * factor );
		}

		factor -= 1.0f;
		upper = Math.max( lower, mall );
		return lower + (int) ( ( upper - lower ) * factor );
	}

	private static int priceToAcquire(
		final AdventureResult item, int quantity, final int level, final boolean exact, final boolean mallPriceOnly )
	{
		int price = 0;
		int onhand = Math.min( quantity, item.getCount( KoLConstants.inventory ) );
		if ( onhand > 0 )
		{
			if ( item.getItemId() != ItemPool.PLASTIC_SWORD )
			{
				price = mallPriceOnly ? 0 : InventoryManager.itemValue( item, exact );
			}

			price *= onhand;
			quantity -= onhand;

			if ( quantity == 0 )
			{
				if ( Preferences.getBoolean( "debugBuy" ) )
				{
					RequestLogger.printLine( "\u262F " + item.getInstance( onhand ) + " onhand=" + price );
				}

				return price;
			}
		}

		int mallPrice = StoreManager.getMallPrice( item, exact ? 0.0f : 7.0f ) * quantity;
		if ( mallPrice <= 0 )
		{
			mallPrice = Integer.MAX_VALUE;
		}
		else
		{
			mallPrice += price;
		}

		int makePrice = InventoryManager.priceToMake( item, quantity, level, exact, mallPriceOnly );
		if ( makePrice != Integer.MAX_VALUE )
		{
			makePrice += price;
		}

		if ( !exact && mallPrice / 2 < makePrice && makePrice / 2 < mallPrice )
		{
			// Less than a 2:1 ratio, we should check more carefully
			return InventoryManager.priceToAcquire( item, quantity, level, true, mallPriceOnly );
		}

		if ( Preferences.getBoolean( "debugBuy" ) )
		{
			RequestLogger.printLine( "\u262F " + item.getInstance( quantity ) + " mall=" + mallPrice + " make=" + makePrice );
		}

		return Math.min( mallPrice, makePrice );
	}

	private static int priceToMake(
		final AdventureResult item, final int quantity, final int level, final boolean exact, final boolean mallPriceOnly )
	{
		int id = item.getItemId();
		int meatCost = CombineMeatRequest.getCost( id );
		if ( meatCost > 0 )
		{
			return meatCost * quantity;
		}

		int method = ConcoctionDatabase.getMixingMethod( item );
		if ( level > 10 || !ConcoctionDatabase.isPermittedMethod( method ) )
		{
			return Integer.MAX_VALUE;
		}

		int price = ConcoctionDatabase.CREATION_COST[ method & KoLConstants.CT_MASK ];
		int yield = ConcoctionDatabase.getYield( id );
		int madeQuantity = ( quantity + yield - 1 ) / yield;

		AdventureResult ingredients[] = ConcoctionDatabase.getIngredients( id );

		for ( int i = 0; i < ingredients.length; ++i )
		{
			AdventureResult ingredient = ingredients[ i ];
			int needed = ingredient.getCount() * madeQuantity;

			int ingredientPrice = InventoryManager.priceToAcquire( ingredient, needed, level + 1, exact, mallPriceOnly );

			if ( ingredientPrice == Integer.MAX_VALUE )
			{
				return ingredientPrice;
			}

			price += ingredientPrice;
		}

		return price * quantity / ( yield * madeQuantity );
	}

	private static int priceToMake( final AdventureResult item, final int qty, final int level, final boolean exact )
	{
		return InventoryManager.priceToMake( item, qty, level, exact, false );
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
		return InventoryManager.hasAnyIngredient( itemId, null );
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

		Integer key = IntegerPool.get( itemId );

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

		return true;
	}

	public static final void registerListener( final int itemId, final PreferenceListener listener )
	{
		if ( itemId < 1 )
		{
			return;
		}

		ArrayList list = InventoryManager.listeners.get( itemId );
		if ( list == null )
		{
			list = new ArrayList();
			InventoryManager.listeners.set( itemId, list );
		}

		list.add( new WeakReference( listener ) );
	}

	public static final void fireInventoryChanged( final int itemId )
	{
		ArrayList list = InventoryManager.listeners.get( itemId );
		if ( list != null )
		{
			Iterator i = list.iterator();
			while ( i.hasNext() )
			{
				WeakReference reference = (WeakReference) i.next();
				PreferenceListener listener = (PreferenceListener) reference.get();

				if ( listener == null )
				{
					i.remove();
				}
				else
				{
					try
					{
						listener.update();
					}
					catch ( Exception e )
					{
						// Don't let a botched listener interfere with
						// the code that modified the inventory.

						StaticEntity.printStackTrace( e );
					}
				}
			}
		}
	}

	private static class ArrayListArray
	{
		private final ArrayList internalList = new ArrayList( ItemDatabase.maxItemId() );

		public ArrayListArray()
		{
			int max = ItemDatabase.maxItemId();
			for ( int i = 0; i <= max; ++i )
			{
				this.internalList.add( null );
			}
		}

		public ArrayList get( final int index )
		{
			if ( index < 0 || index >= this.internalList.size() )
			{
				return null;
			}

			return (ArrayList) this.internalList.get( index );
		}

		public void set( final int index, final ArrayList value )
		{
			this.internalList.set( index, value );
		}
	}

	public static final void parseInventory( final JSONObject JSON )
		throws JSONException
	{
		// {"1":"1","2":"1" ... }
		Iterator keys = JSON.keys();
		while ( keys.hasNext() )
		{
			String key = (String) keys.next();
			int itemId = StringUtilities.parseInt( key );
			int count = JSON.getInt( key );
			String name = ItemDatabase.getItemDataName( itemId );
			if ( name == null )
			{
				// Fetch descid from api.php?what=item
				// and register new item.
				ItemDatabase.registerItem( itemId );
			}

			AdventureResult item = new AdventureResult( itemId, count );
			int inventoryCount = item.getCount( KoLConstants.inventory );

			// Add the difference between your existing count
			// and the original count.

			if ( inventoryCount != count )
			{
				item = item.getInstance( count - inventoryCount );
				ResultProcessor.tallyResult( item, true );
			}
		}
	}

	private static Pattern COT_PATTERN = Pattern.compile( "Current Occupant:.*?<b>.* the (.*?)</b>" );
	public static final AdventureResult CROWN_OF_THRONES = ItemPool.get( ItemPool.HATSEAT, 1 );

	public static final void checkCrownOfThrones()
	{
		// If we are wearing the Crown of Thrones, we've already seen
		// which familiar is riding in it
		if ( KoLCharacter.hasEquipped( InventoryManager.CROWN_OF_THRONES, EquipmentManager.HAT ) )
		{
			return;
		}

		// The Crown of Thrones is not trendy, but double check anyway
		AdventureResult item = InventoryManager.CROWN_OF_THRONES;
		if ( KoLCharacter.isTrendy() && !TrendyRequest.isTrendy( "Items", item.getName() ) )
		{
			return;
		}

		// See if we have a Crown of Thrones in inventory or closet
		int count = item.getCount( KoLConstants.inventory ) + item.getCount( KoLConstants.closet );
		if ( count == 0 )
		{
			return;
		}

		// See which familiar is riding in it.
		String descId = ItemDatabase.getDescriptionId( ItemPool.HATSEAT );
		GenericRequest req = new GenericRequest( "desc_item.php?whichitem=" + descId );
		RequestThread.postRequest( req );

		Matcher matcher = InventoryManager.COT_PATTERN.matcher( req.responseText );
		if ( matcher.find() )
		{
			String race = matcher.group( 1 );
			KoLCharacter.setEnthroned( KoLCharacter.findFamiliar( race ) );
		}
	}

	private static final AdventureResult GOLDEN_MR_ACCESSORY = ItemPool.get( ItemPool.GOLDEN_MR_ACCESSORY, 1 );

	public static void countGoldenMrAccesories()
	{
		int oldCount = Preferences.getInteger( "goldenMrAccessories" );
		int newCount =
			InventoryManager.GOLDEN_MR_ACCESSORY.getCount( KoLConstants.inventory ) +
				InventoryManager.GOLDEN_MR_ACCESSORY.getCount( KoLConstants.closet ) +
				InventoryManager.GOLDEN_MR_ACCESSORY.getCount( KoLConstants.storage ) +
				InventoryManager.GOLDEN_MR_ACCESSORY.getCount( KoLConstants.collection ) +
				InventoryManager.getEquippedCount( InventoryManager.GOLDEN_MR_ACCESSORY );

		// A Golden Mr. Accessory cannot be traded or discarded. Once
		// you purchase one, it's yours forever more.

		if ( newCount > oldCount )
		{
			Preferences.setInteger( "goldenMrAccessories", newCount );
		}
	}

	private static boolean allowTurnConsumption( final CreateItemRequest creator )
	{
		if ( !GenericFrame.instanceExists() )
		{
			return true;
		}

		if ( !InventoryManager.askAboutCrafting( creator ) )
		{
			return false;
		}

		return true;
	}

	private static boolean askAboutCrafting( final CreateItemRequest creator )
	{
		if ( creator.getQuantityNeeded() < 1 )
		{
			return true;
		}
		// Allow the user to permanently squash this prompt.
		if ( Preferences.getInteger( "promptAboutCrafting" ) < 1 )
		{
			return true;
		}
		// If we've already nagged, don't nag. Unless the user wants us to nag. Then, nag.
		if ( InventoryManager.askedAboutCrafting == KoLCharacter.getUserId() &&
			Preferences.getInteger( "promptAboutCrafting" ) < 2 )
		{
			return true;
		}

		// See if we have enough free crafting turns available
		int freeCrafts = ConcoctionDatabase.getFreeCraftingTurns();
		int needed = creator.concoction.getAdventuresNeeded( creator.getQuantityNeeded() );

		if ( needed <= freeCrafts )
		{
			return true;
		}

		// We could cast Inigo's automatically here, but nah. Let the user do that.

		String message =
			freeCrafts > 0 ? "You will run out of free crafting turns before you are finished. Are you sure?"
				: "You are about to spend adventures crafting.  Are you sure?";

		if ( !InputFieldUtilities.confirm( message ) )
		{
			return false;
		}

		InventoryManager.askedAboutCrafting = KoLCharacter.getUserId();

		return true;
	}
}
