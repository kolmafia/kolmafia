/**
 * Copyright (c) 2005-2013, KoLmafia development team
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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestLogger;

import net.sourceforge.kolmafia.moods.RecoveryManager;

import net.sourceforge.kolmafia.objectpool.ConcoctionPool;
import net.sourceforge.kolmafia.objectpool.EffectPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.objectpool.OutfitPool;

import net.sourceforge.kolmafia.persistence.EquipmentDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.persistence.NPCStoreDatabase;
import net.sourceforge.kolmafia.persistence.ConcoctionDatabase;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.session.EquipmentManager;
import net.sourceforge.kolmafia.session.ResultProcessor;

import net.sourceforge.kolmafia.utilities.StringUtilities;

public class NPCPurchaseRequest
	extends PurchaseRequest
{
	private static final AdventureResult TROUSERS = ItemPool.get( ItemPool.TRAVOLTAN_TROUSERS, 1 );
	private static final AdventureResult FLEDGES = ItemPool.get( ItemPool.PIRATE_FLEDGES, 1 );
	private final static AdventureResult SUPER_SKILL = EffectPool.get( "Super Skill" );
	private final static AdventureResult SUPER_STRUCTURE = EffectPool.get( "Super Structure" );
	private final static AdventureResult SUPER_VISION = EffectPool.get( "Super Vision" );
	private final static AdventureResult SUPER_SPEED = EffectPool.get( "Super Speed" );
	private final static AdventureResult SUPER_ACCURACY = EffectPool.get( "Super Accuracy" );

	public static final Pattern PIRATE_EPHEMERA_PATTERN =
		Pattern.compile( "pirate (?:brochure|pamphlet|tract)" );

	private static Pattern NPCSTOREID_PATTERN = Pattern.compile( "whichstore=([^&]*)" );
	private static Pattern NPCSHOPID_PATTERN = Pattern.compile( "whichshop=([^&]*)" );

	private String npcStoreId;
	private String quantityField;
	private int row;

	/**
	 * Constructs a new <code>NPCPurchaseRequest</code> which retrieves things from NPC stores.
	 */

	public NPCPurchaseRequest( final String storeName, final String storeId, final int itemId, final int row, final int price )
	{
		super( NPCPurchaseRequest.pickForm( storeId ) );

		this.isMallStore = false;
		this.item = new AdventureResult( itemId, 1 );

		this.shopName = storeName;
		this.npcStoreId = storeId;
		this.row = row;

		this.quantity = PurchaseRequest.MAX_QUANTITY;
		this.price = price;
		this.limit = this.quantity;
		this.canPurchase = true;

		this.timestamp = 0L;

		if ( this.row != 0 )
		{
			this.addFormField( "whichshop", storeId );
			this.addFormField( "action", "buyitem" );
			this.addFormField( "whichrow", String.valueOf( row ) );
			this.addFormField( "ajax", "1" );
			this.hashField = "pwd";
			this.quantityField = "quantity";
			return;
		}

		this.addFormField( "whichitem", String.valueOf( itemId ) );

		if ( storeId.equals( "galaktik.php" ) )
		{
			// Annoying special case.
			this.addFormField( "action", "buyitem" );
			this.hashField = "pwd";
			this.quantityField = "howmany";
		}
		else if ( storeId.equals( "town_giftshop.php" ) )
		{
			this.addFormField( "action", "buy" );
			this.hashField = "pwd";
			this.quantityField = "howmany";
		}
		else if ( storeId.equals( "fdkol" ) )
		{
			this.addFormField( "whichshop", storeId );
			this.addFormField( "action", "buyitem" );
			this.addFormField( "ajax", "1" );
			this.hashField = "pwd";
			this.quantityField = "quantity";
		}
		else
		{
			this.addFormField( "whichstore", storeId );
			this.addFormField( "buying", "1" );
			this.addFormField( "ajax", "1" );
			this.hashField = "phash";
			this.quantityField = "howmany";
		}
	}

	public static String pickForm( final String storeId )
	{
		return  storeId.indexOf( "." ) != -1 ?
			storeId :
			storeId.equals( "fdkol" ) || storeId.equals( "hiddentavern" ) ?
			"shop.php" :
			"store.php";
	}

	public static String getShopId( final String urlString )
	{
		Matcher m = NPCPurchaseRequest.NPCSHOPID_PATTERN.matcher( urlString );
		return m.find() ? m.group( 1 ) : null;
	}

	public String getStoreId()
	{
		return this.npcStoreId;
	}

	/**
	 * Retrieves the price of the item being purchased.
	 *
	 * @return The price of the item being purchased
	 */

	@Override
	public int getPrice()
	{
		return NPCPurchaseRequest.currentPrice( this.price );
	}

	private static int currentPrice( final int price )
	{
		double factor = 1.0;
		if ( NPCPurchaseRequest.usingTrousers() ) factor -= 0.05;
		if ( KoLCharacter.hasSkill( "Five Finger Discount" ) ) factor -= 0.05;
		return (int) ( price * factor );
	}

	private static boolean usingTrousers()
	{
		return EquipmentManager.getEquipment( EquipmentManager.PANTS ).equals( NPCPurchaseRequest.TROUSERS );
	}

	@Override
	public void run()
	{
		this.addFormField( this.quantityField, String.valueOf( this.limit ) );

		super.run();
	}

	@Override
	public boolean ensureProperAttire()
	{
		if ( this.npcStoreId.equals( "fdkol" ) )
		{
			// Travoltan trousers do not give a discount
			return true;
		}

		int neededOutfit = OutfitPool.NONE;

		if ( this.npcStoreId.equals( "b" ) )
		{
			neededOutfit = OutfitPool.BUGBEAR_COSTUME;
		}
		else if ( this.npcStoreId.equals( "r" ) )
		{
			if ( !KoLCharacter.hasEquipped( NPCPurchaseRequest.FLEDGES ) )
			{
				neededOutfit = OutfitPool.SWASHBUCKLING_GETUP;
			}
		}
		else if ( this.npcStoreId.equals( "h" ) )
		{
			if ( this.shopName.equals( "Hippy Store (Pre-War)" ) )
			{
				neededOutfit = OutfitPool.HIPPY_OUTFIT;
			}
			else if ( QuestLogRequest.isHippyStoreAvailable() )
			{
				neededOutfit = OutfitPool.NONE;
			}
			else if ( this.shopName.equals( "Hippy Store (Hippy)" ) )
			{
				neededOutfit = OutfitPool.WAR_HIPPY_OUTFIT;
			}
			else if ( this.shopName.equals( "Hippy Store (Fratboy)" ) )
			{
				neededOutfit = OutfitPool.WAR_FRAT_OUTFIT;
			}
		}

		// Only switch outfits if the person is not currently wearing the outfit and if they
		// have that outfit.

		if ( neededOutfit != OutfitPool.NONE )
		{
			if ( EquipmentManager.isWearingOutfit( neededOutfit ) )
			{
				return true;
			}

			if ( !EquipmentManager.hasOutfit( neededOutfit ) )
			{
				return false;
			}
		}

		// If you have a buff from Greatest American Pants and have it set to keep the buffs,
		// disallow outfit changes.

		if ( Preferences.getBoolean( "gapProtection" ) )
		{
			if ( KoLConstants.activeEffects.contains( NPCPurchaseRequest.SUPER_SKILL ) ||
			     KoLConstants.activeEffects.contains( NPCPurchaseRequest.SUPER_STRUCTURE ) ||
			     KoLConstants.activeEffects.contains( NPCPurchaseRequest.SUPER_VISION ) ||
			     KoLConstants.activeEffects.contains( NPCPurchaseRequest.SUPER_SPEED ) ||
			     KoLConstants.activeEffects.contains( NPCPurchaseRequest.SUPER_ACCURACY ) )
			{
				if ( neededOutfit != OutfitPool.NONE )
				{
					KoLmafia.updateDisplay(
						MafiaState.ERROR,
						"You have a Greatest American Pants buff and buying the necessary " + getItemName() + " would cause you to lose it." );

					return false;
				}

				return true;
			}
		}

		// If the recovery manager is running, do not change equipment as this has the potential
		// for an infinite loop.

		if ( RecoveryManager.isRecoveryActive() )
		{
			if ( neededOutfit != OutfitPool.NONE )
			{
				KoLmafia.updateDisplay(
					MafiaState.ERROR,
					"Aborting implicit outfit change due to potential infinite loop in auto-recovery. Please buy the necessary " + getItemName() + " manually." );

				return false;
			}

			return true;
		}

		// If there's an outfit that you need to use, change into it.

		if ( neededOutfit != OutfitPool.NONE )
		{
			( new EquipmentRequest( EquipmentDatabase.getOutfit( neededOutfit ) ) ).run();

			return true;
		}

		// Otherwise, maybe you can put on some Travoltan Trousers to decrease the cost of the
		// purchase, but only if auto-recovery isn't running.

		if ( !NPCPurchaseRequest.usingTrousers() && KoLConstants.inventory.contains( NPCPurchaseRequest.TROUSERS ) )
		{
			( new EquipmentRequest( NPCPurchaseRequest.TROUSERS, EquipmentManager.PANTS ) ).run();
		}

		return true;
	}

	@Override
	public void processResults()
	{
		String urlString = this.getURLString();

		if ( urlString.startsWith( "store.php" ) )
		{
			NPCPurchaseRequest.parseResponse( urlString, this.responseText );
		}
		else if ( urlString.startsWith( "shop.php" ) )
		{
			NPCPurchaseRequest.parseShopResponse( urlString, this.responseText );
		}

		int quantityAcquired = this.item.getCount( KoLConstants.inventory ) - this.initialCount;

		if ( quantityAcquired > 0 )
		{
			// Normal NPC stores say "You spent xxx Meat" and we
			// have already parsed that.
			if ( !urlString.startsWith( "store.php" ) &&
			     !urlString.startsWith( "shop.php" ) &&
			     !urlString.startsWith( "galaktik.php" ) )
			{
				ResultProcessor.processMeat( -1 * this.getPrice() * quantityAcquired );
				KoLCharacter.updateStatus();
			}

			return;
		}
	}

	private static final Pattern ITEM_PATTERN = Pattern.compile( "name=whichitem value=([\\d]+)[^>]*?>.*?descitem.([\\d]+)[^>]*>.*?<b>(.*?)</b>", Pattern.DOTALL );

	public static final void parseResponse( final String urlString, final String responseText )
	{
		if ( !urlString.startsWith( "store.php" ) )
		{
			return;
		}

		// Learn new items by simply visiting a store
		Matcher matcher = ITEM_PATTERN.matcher( responseText );
		while ( matcher.find() )
		{
			int id = StringUtilities.parseInt( matcher.group(1) );
			String desc = matcher.group(2);
			String name = matcher.group(3);
			String data = ItemDatabase.getItemDataName( id );
			if ( data == null || !data.equals( name ) )
			{
				ItemDatabase.registerItem( id, name, desc );
			}
		}

		Matcher m = NPCPurchaseRequest.NPCSTOREID_PATTERN.matcher( urlString );
		if ( !m.find() )
		{
			return;
		}

		// When we purchase items from NPC stores using ajax, the
		// response tells us nothing about the contents of the store.
		if ( urlString.indexOf( "ajax=1" ) != -1 )
		{
			return;
		}

		String storeId = m.group(1);

		if ( storeId.equals( "r" ) )
		{
			m = PIRATE_EPHEMERA_PATTERN.matcher( responseText );
			if ( m.find() )
			{
				Preferences.setInteger( "lastPirateEphemeraReset", KoLCharacter.getAscensions() );
				Preferences.setString( "lastPirateEphemera", m.group( 0 ) );
			}
			return;
		}

		if ( storeId.equals( "h" ) )
		{
			// Check to see if any of the items offered in the
			// hippy store are special.

			String side = "none";

			if ( responseText.indexOf( "peach" ) != -1 &&
			     responseText.indexOf( "pear" ) != -1 &&
			     responseText.indexOf( "plum" ) != -1 )
			{
				Preferences.setInteger( "lastFilthClearance", KoLCharacter.getAscensions() );
				side = "hippy";
			}
			else if ( responseText.indexOf( "bowl of rye sprouts" ) != -1 &&
				  responseText.indexOf( "cob of corn" ) != -1 &&
				  responseText.indexOf( "juniper berries" ) != -1 )
			{
				Preferences.setInteger( "lastFilthClearance", KoLCharacter.getAscensions() );
				side = "fratboy";
			}

			Preferences.setString( "currentHippyStore", side );
			Preferences.setString( "sidequestOrchardCompleted", side );

			if ( responseText.contains( "Oh, hey, boss!  Welcome back!" ) )
			{
				Preferences.setBoolean( "_hippyMeatCollected", true );
			}
			return;
		}
	}

	public static final boolean registerRequest( final String urlString )
	{
		if ( !urlString.startsWith( "store.php" ) && !urlString.startsWith( "galaktik.php" ) && !urlString.startsWith( "town_giftshop.php" ) )
		{
			return false;
		}

		Matcher itemMatcher = TransferItemRequest.ITEMID_PATTERN.matcher( urlString );
		if ( !itemMatcher.find() )
		{
			return true;
		}

		Matcher quantityMatcher = TransferItemRequest.HOWMANY_PATTERN.matcher( urlString );
		if ( !quantityMatcher.find() )
		{
			return true;
		}

		int itemId = StringUtilities.parseInt( itemMatcher.group( 1 ) );
		String itemName = ItemDatabase.getItemName( itemId );
		int quantity = StringUtilities.parseInt( quantityMatcher.group( 1 ) );
		int priceVal = NPCStoreDatabase.price( itemName );

		Matcher m = NPCPurchaseRequest.NPCSTOREID_PATTERN.matcher(urlString);
		String storeId = m.find() ? NPCStoreDatabase.getStoreName( m.group(1) ) : null;
		String storeName = storeId != null ? storeId : "an NPC Store";

		RequestLogger.updateSessionLog();
		RequestLogger.updateSessionLog( "buy " + quantity + " " + itemName + " for " + String.valueOf( priceVal ) + " each from " + storeName );

		return true;
	}

	public static final void parseShopRowResponse( final String urlString, final String responseText )
	{
		Matcher rowMatcher = GenericRequest.WHICHROW_PATTERN.matcher( urlString );
		if ( !rowMatcher.find() )
		{
			return;
		}

		int row = StringUtilities.parseInt( rowMatcher.group( 1 ) );
		int itemId = ConcoctionPool.rowToId( row );

		CreateItemRequest item = CreateItemRequest.getInstance( itemId, false );
		if ( item == null )
		{
			return; // this is an unknown item
		}

		int quantity = 1;
		if ( urlString.contains( "buymax=" ) )
		{
			quantity = item.getQuantityPossible();
		}
		else
		{
			Matcher quantityMatcher = GenericRequest.QUANTITY_PATTERN.matcher( urlString );
			if ( quantityMatcher.find() )
			{
				String quantityString = quantityMatcher.group( 1 ).trim();
				quantity = quantityString.length() == 0 ? 1 : StringUtilities.parseInt( quantityString );
			}
		}

		AdventureResult[] ingredients = ConcoctionDatabase.getIngredients( itemId );
		for ( int i = 0; i < ingredients.length; ++i )
		{
			ResultProcessor.processResult(
				ingredients[ i ].getInstance( -1 * ingredients[ i ].getCount() * quantity ) );
		}
	}

	public static final void parseShopResponse( final String urlString, final String responseText )
	{
		if ( !urlString.startsWith( "shop.php" ) )
		{
			return;
		}

		String shopId = NPCPurchaseRequest.getShopId( urlString );
		if ( shopId == null )
		{
			return;
		}

		// The following trade collections of ingredients for an item
		if ( shopId.equals( "mystic" ) ||
		     shopId.startsWith( "kolhs_" ) ||
		     shopId.equals( "grandma" ) ||
		     shopId.equals( "beergarden" ) ||
		     shopId.equals( "junkmagazine" ) ||
		     shopId.equals( "snowgarden" ) )
		{
			NPCPurchaseRequest.parseShopRowResponse( urlString, responseText );
			return;
		}

		// The following does too, but always makes a single item
		if ( shopId.equals( "starchart" ) )
		{
			StarChartRequest.parseResponse( urlString, responseText );
			return;
		}

		// The following does too, but wants a special message
		if ( shopId.equals( "jarl" ) )
		{
			JarlsbergRequest.parseResponse( urlString, responseText );
			return;
		}

		// The following are coinmasters

		if ( shopId.equals( "damachine" ) )
		{
			VendingMachineRequest.parseResponse( urlString, responseText );
			return;
		}

		if ( shopId.equals( "shore" ) )
		{
			ShoreGiftShopRequest.parseResponse( urlString, responseText );
			return;
		}

		if ( shopId.equals( "hiddentavern" ) )
		{
			// If Hidden Tavern not already unlocked, new items available
			if ( Preferences.getInteger( "hiddenTavernUnlock" ) != KoLCharacter.getAscensions() )
			{
				// Unlock Hidden Tavern
				Preferences.setInteger( "hiddenTavernUnlock", KoLCharacter.getAscensions() );
				ConcoctionDatabase.setRefreshNeeded( true );
			}
			return;
		}

		if ( shopId.equals( "dv" ) )
		{
			TerrifiedEagleInnRequest.parseResponse( urlString, responseText );
			return;
		}

		if ( shopId.equals( "trapper" ) )
		{
			TrapperRequest.parseResponse( urlString, responseText );
			return;
		}

		if ( shopId.equals( "fdkol" ) )
		{
			FDKOLRequest.parseResponse( urlString, responseText );
			return;
		}

		if ( shopId.equals( "elvishp1" ) ||
		     shopId.equals( "elvishp2" ) ||
		     shopId.equals( "elvishp3" ) )
		{
			SpaaaceRequest.parseResponse( urlString, responseText );
			return;
		}
	}

	public static final boolean registerShopRowRequest( final String urlString )
	{
		Matcher rowMatcher = GenericRequest.WHICHROW_PATTERN.matcher( urlString );
		if ( !rowMatcher.find() )
		{
			return true;
		}

		int row = StringUtilities.parseInt( rowMatcher.group( 1 ) );
		int itemId = ConcoctionPool.rowToId( row );

		CreateItemRequest item = CreateItemRequest.getInstance( itemId, false );
		if ( item == null )
		{
			return true; // this is an unknown item
		}

		int quantity = 1;
		if ( urlString.contains( "buymax=" ) )
		{
			quantity = item.getQuantityPossible();
		}
		else
		{
			Matcher quantityMatcher = GenericRequest.QUANTITY_PATTERN.matcher( urlString );
			if ( quantityMatcher.find() )
			{
				String quantityString = quantityMatcher.group( 1 ).trim();
				quantity = quantityString.length() == 0 ? 1 : StringUtilities.parseInt( quantityString );
			}
		}

		if ( quantity > item.getQuantityPossible() )
		{
			return true; // attempt will fail
		}

		StringBuilder buffer = new StringBuilder();
		buffer.append( "Trade " );

		AdventureResult[] ingredients = ConcoctionDatabase.getIngredients( itemId );
		for ( int i = 0; i < ingredients.length; ++i )
		{
			if ( i > 0 )
			{
				buffer.append( ", " );
			}

			buffer.append( ingredients[ i ].getCount() * quantity );
			buffer.append( " " );
			buffer.append( ingredients[ i ].getName() );
		}

		RequestLogger.updateSessionLog();
		RequestLogger.updateSessionLog( buffer.toString() );

		return true;
	}

	public static final boolean registerShopRequest( final String urlString, boolean meatOnly )
	{
		if ( !urlString.startsWith( "shop.php" ) )
		{
			return false;
		}

		String shopId = NPCPurchaseRequest.getShopId( urlString );
		if ( shopId == null )
		{
			return false;
		}

		String shopName = NPCStoreDatabase.getStoreName( shopId );

		int itemId = -1;

		Matcher m = TransferItemRequest.ITEMID_PATTERN.matcher( urlString );
		if ( m.find() )
		{
			itemId = StringUtilities.parseInt( m.group( 1 ) );
		}
		else
		{
			m = GenericRequest.WHICHROW_PATTERN.matcher( urlString );
			if ( m.find() )
			{
				int row = StringUtilities.parseInt( m.group( 1 ) );
				itemId = NPCStoreDatabase.itemIdByRow( shopId, row );
			}
		}

		if ( itemId == -1 )
		{
			return false;
		}

		String itemName = ItemDatabase.getItemName( itemId );
		int priceVal = NPCStoreDatabase.price( itemName );

		// A "shop" can have items for Meat and also for tokens.
		// If  there is no Meat price, let correct class claim it.
		if ( priceVal == 0 )
		{
			// If we've already checked tokens, this is an unknown item
			if ( meatOnly )
			{
				return false;
			}

			// The following trade collections of ingredients for an item
			if ( shopId.equals( "mystic" ) ||
			     shopId.startsWith( "kolhs_" ) ||
			     shopId.equals( "grandma" ) ||
			     shopId.equals( "beergarden" ) ||
			     shopId.equals( "snowgarden" ) )
			{
				return NPCPurchaseRequest.registerShopRowRequest( urlString );
			}

			// The following does too, but always makes a single item
			if ( shopId.equals( "starchart" ) )
			{
				return StarChartRequest.registerRequest( urlString );
			}

			// The following does too, but wants a special message
			if ( shopId.equals( "jarl" ) )
			{
				return JarlsbergRequest.registerRequest( urlString );
			}

			// The following are coinmasters

			if ( shopId.equals( "damachine" ) )
			{
				return VendingMachineRequest.registerRequest( urlString );
			}

			if ( shopId.equals( "shore" ) )
			{
				return ShoreGiftShopRequest.registerRequest( urlString );
			}

			if ( shopId.equals( "dv" ) )
			{
				return TerrifiedEagleInnRequest.registerRequest( urlString );
			}

			if ( shopId.equals( "trapper" ) )
			{
				return TrapperRequest.registerRequest( urlString );
			}

			if ( shopId.equals( "fdkol" ) )
			{
				return FDKOLRequest.registerRequest( urlString, true );
			}

			return false;
		}

		Matcher quantityMatcher = TransferItemRequest.QUANTITY_PATTERN.matcher( urlString );
		if ( !quantityMatcher.find() )
		{
			return true;
		}

		int quantity = StringUtilities.parseInt( quantityMatcher.group( 1 ) );

		RequestLogger.updateSessionLog();
		RequestLogger.updateSessionLog( "buy " + quantity + " " + itemName + " for " + String.valueOf( priceVal ) + " each from " + shopName );

		return true;
	}
}
