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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestLogger;

import net.sourceforge.kolmafia.objectpool.EffectPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;

import net.sourceforge.kolmafia.persistence.EquipmentDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.persistence.NPCStoreDatabase;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.session.EquipmentManager;
import net.sourceforge.kolmafia.session.ResultProcessor;

import net.sourceforge.kolmafia.utilities.StringUtilities;

public class NPCPurchaseRequest
	extends PurchaseRequest
{
	private static final AdventureResult TROUSERS = ItemPool.get( ItemPool.TRAVOLTAN_TROUSERS, 1 );
	private static final AdventureResult FLEDGES = ItemPool.get( ItemPool.PIRATE_FLEDGES, 1 );

	public static final Pattern PIRATE_EPHEMERA_PATTERN =
		Pattern.compile( "pirate (?:brochure|pamphlet|tract)" );

	private static Pattern NPCSTOREID_PATTERN = Pattern.compile( "whichstore=([^&]*)" );

	private String npcStoreId;

	/**
	 * Constructs a new <code>NPCPurchaseRequest</code> which retrieves things from NPC stores.
	 */

	public NPCPurchaseRequest( final String storeName, final String storeId, final int itemId, final int price )
	{
		super( storeId.indexOf( "." ) == -1 ? "store.php" : storeId );

		this.isMallStore = false;
		this.item = new AdventureResult( itemId, 1 );

		this.shopName = storeName;
		this.npcStoreId = storeId;

		this.quantity = PurchaseRequest.MAX_QUANTITY;
		this.price = price;
		this.limit = this.quantity;
		this.canPurchase = true;

		if ( storeId.indexOf( "." ) == -1 )
		{
			this.addFormField( "whichstore", storeId );
			this.addFormField( "buying", "1" );
			this.addFormField( "ajax", "1" );
			this.hashField = "phash";
		}
		else if ( storeId.equals( "galaktik.php" ) )
		{
			// Annoying special case.
			this.addFormField( "action", "buyitem" );
			this.hashField = "pwd";
		}
		else if ( storeId.equals( "town_giftshop.php" ) )
		{
			this.addFormField( "action", "buy" );
			this.hashField = "pwd";
		}

		this.addFormField( "whichitem", String.valueOf( itemId ) );

		this.timestamp = 0L;
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
		return !NPCPurchaseRequest.usingTrousers() ? price : (int) ( price * 0.95f );
	}

	private static boolean usingTrousers()
	{
		return EquipmentManager.getEquipment( EquipmentManager.PANTS ).equals( NPCPurchaseRequest.TROUSERS );
	}

	@Override
	public void run()
	{
		this.addFormField( "howmany", String.valueOf( this.limit ) );

		super.run();
	}

	@Override
	public boolean ensureProperAttire()
	{
		int neededOutfit = 0;

		if ( this.npcStoreId.equals( "b" ) )
		{
			neededOutfit = 1;
		}
		else if ( this.npcStoreId.equals( "r" ) )
		{
			if ( !KoLCharacter.hasEquipped( NPCPurchaseRequest.FLEDGES ) )
			{
				neededOutfit = 9;
			}
		}
		else if ( this.npcStoreId.equals( "h" ) )
		{
			if ( this.shopName.equals( "Hippy Store (Pre-War)" ) )
			{
				neededOutfit = 2;
			}
			else if ( QuestLogRequest.isHippyStoreAvailable() )
			{
				neededOutfit = 0;
			}
			else if ( this.shopName.equals( "Hippy Store (Hippy)" ) )
			{
				neededOutfit = 32;
			}
			else if ( this.shopName.equals( "Hippy Store (Fratboy)" ) )
			{
				neededOutfit = 33;
			}
		}

		if ( neededOutfit == 0 )
		{
			// Maybe you can put on some Travoltan Trousers to
			// decrease the cost of the purchase.

			if ( KoLConstants.inventory.contains( NPCPurchaseRequest.TROUSERS ) && GAPcheck() )
			{
				( new EquipmentRequest( NPCPurchaseRequest.TROUSERS, EquipmentManager.PANTS ) ).run();
			}

			return true;
		}

		// Only switch outfits if the person is not
		// currently wearing the outfit.

		if ( EquipmentManager.isWearingOutfit( neededOutfit ) )
		{
			return true;
		}

		if ( !EquipmentManager.hasOutfit( neededOutfit ) )
		{
			return false;
		}

		if ( !GAPcheck() )
		{
			KoLmafia.updateDisplay( KoLConstants.ERROR_STATE,
				"You have a Greatest American Pants buff and buying this item would cause you to lose it." );
			return false;
		}
		( new EquipmentRequest( EquipmentDatabase.getOutfit( neededOutfit ) ) ).run();
		return true;
	}

	private final static  AdventureResult SUPER_SKILL = EffectPool.get( "Super Skill" );
	private final static  AdventureResult SUPER_STRUCTURE = EffectPool.get( "Super Structure" );
	private final static  AdventureResult SUPER_VISION = EffectPool.get( "Super Vision" );
	private final static  AdventureResult SUPER_SPEED = EffectPool.get( "Super Speed" );
	private final static  AdventureResult SUPER_ACCURACY = EffectPool.get( "Super Accuracy" );

	private static final boolean GAPcheck()
	{
		// returns false if you have a buff from Greatest American
		// Pants and have it set to keep the buffs, otherwise returns
		// true
		boolean keepPants = Preferences.getBoolean( "gapProtection" ) &&
			(
			KoLConstants.activeEffects.contains( NPCPurchaseRequest.SUPER_SKILL ) ||
			KoLConstants.activeEffects.contains( NPCPurchaseRequest.SUPER_STRUCTURE ) ||
			KoLConstants.activeEffects.contains( NPCPurchaseRequest.SUPER_VISION ) ||
			KoLConstants.activeEffects.contains( NPCPurchaseRequest.SUPER_SPEED ) ||
			KoLConstants.activeEffects.contains( NPCPurchaseRequest.SUPER_ACCURACY )
			);

		return !keepPants;
	}

	@Override
	public void processResults()
	{
		String urlString = this.getURLString();
		NPCPurchaseRequest.parseResponse( urlString, this.responseText );

		int quantityAcquired = this.item.getCount( KoLConstants.inventory ) - this.initialCount;

		if ( quantityAcquired > 0 )
		{
			// Normal NPC stores say "You spent xxx Meat" and we
			// have already parsed that.
			if ( !urlString.startsWith( "store.php" ) &&
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

			if ( responseText.contains( "Oh, hey, boss! Welcome back!" ) )
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
}
