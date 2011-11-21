/**
 * Copyright (c) 2005-2011, KoLmafia development team
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
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import net.java.dev.spellcast.utilities.LockableListModel;
import net.java.dev.spellcast.utilities.SortedListModel;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.FamiliarData;
import net.sourceforge.kolmafia.KoLAdventure;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.Modifiers;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.SpecialOutfit;
import net.sourceforge.kolmafia.StaticEntity;

import net.sourceforge.kolmafia.objectpool.EffectPool;
import net.sourceforge.kolmafia.objectpool.FamiliarPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;

import net.sourceforge.kolmafia.persistence.EquipmentDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase;

import net.sourceforge.kolmafia.preferences.PreferenceListenerRegistry;
import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.request.EquipmentRequest;
import net.sourceforge.kolmafia.request.TrendyRequest;

import net.sourceforge.kolmafia.swingui.CoinmastersFrame;
import net.sourceforge.kolmafia.swingui.GearChangeFrame;

import net.sourceforge.kolmafia.textui.command.ConditionsCommand;

import net.sourceforge.kolmafia.utilities.StringUtilities;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class EquipmentManager
{
	public static final int NONE = -1;

	// Mutable equipment slots
	public static final int HAT = 0;
	public static final int WEAPON = 1;
	public static final int OFFHAND = 2;
	public static final int SHIRT = 3;
	public static final int PANTS = 4;
	public static final int ACCESSORY1 = 5;
	public static final int ACCESSORY2 = 6;
	public static final int ACCESSORY3 = 7;
	public static final int FAMILIAR = 8;

	// Count of real equipment slots: HAT to FAMILIAR
	public static final int SLOTS = 9;

	// Pseudo-equipment slots
	public static final int STICKER1 = 9;
	public static final int STICKER2 = 10;
	public static final int STICKER3 = 11;

	// Count of all equipment slots: HAT to STICKER3
	public static final int ALL_SLOTS = 12;

	public static final int FAKEHAND = 12;

	private static LockableListModel equipment = new LockableListModel();
	private static final LockableListModel accessories = new SortedListModel();
	private static final LockableListModel[] equipmentLists = new LockableListModel[ EquipmentManager.ALL_SLOTS ];
	private static final ArrayList[] historyLists = new ArrayList[ EquipmentManager.ALL_SLOTS ];

	private static int fakeHandCount = 0;
	private static int stinkyCheeseLevel = 0;

	private static final LockableListModel customOutfits = new LockableListModel();
	private static final LockableListModel outfits = new LockableListModel();

	private static final int[] turnsRemaining = new int[ 3 ];

	private static AdventureResult lockedFamiliarItem = EquipmentRequest.UNEQUIP;

	static
	{
		for ( int i = 0; i < EquipmentManager.ALL_SLOTS; ++i )
		{
			EquipmentManager.equipment.add( EquipmentRequest.UNEQUIP );
			EquipmentManager.historyLists[ i ] = new ArrayList();

			switch ( i )
			{
			case EquipmentManager.ACCESSORY1:
			case EquipmentManager.ACCESSORY2:
			case EquipmentManager.ACCESSORY3:
				EquipmentManager.equipmentLists[ i ] = EquipmentManager.accessories.getMirrorImage();
				break;
				
			default:
				EquipmentManager.equipmentLists[ i ] = new SortedListModel();
				break;
			}
		}
	}

	public static void resetEquipment()
	{
		for ( int i = 0; i < EquipmentManager.equipmentLists.length; ++i )
		{
			EquipmentManager.equipmentLists[ i ].clear();
			EquipmentManager.historyLists[ i ].clear();
		}

		EquipmentManager.accessories.clear();
		GearChangeFrame.clearWeaponLists();

		EquipmentManager.equipment.clear();

		for ( int i = 0; i < EquipmentManager.ALL_SLOTS; ++i )
		{
			EquipmentManager.equipment.add( EquipmentRequest.UNEQUIP );
		}

		EquipmentManager.fakeHandCount = 0;
		EquipmentManager.stinkyCheeseLevel = 0;
		EquipmentManager.lockedFamiliarItem = EquipmentRequest.UNEQUIP;
		EquipmentManager.outfits.clear();
	}

	public static void resetOutfits()
	{
		EquipmentManager.customOutfits.clear();
	}

	public static AdventureResult[] emptyEquipmentArray()
	{
		AdventureResult[] array = new AdventureResult[ EquipmentManager.SLOTS ];

		for ( int i = 0; i < EquipmentManager.SLOTS; ++i )
		{
			array[ i ] = EquipmentRequest.UNEQUIP;
		}

		return array;
	}

	public static AdventureResult[] currentEquipment()
	{
		AdventureResult[] array = new AdventureResult[ EquipmentManager.SLOTS ];

		for ( int i = 0; i < EquipmentManager.SLOTS; ++i )
		{
			array[ i ] = EquipmentManager.getEquipment( i );
		}
		return array;
	}

	public static AdventureResult[] allEquipment()
	{
		AdventureResult[] array = new AdventureResult[ EquipmentManager.ALL_SLOTS ];
		array = (AdventureResult[]) EquipmentManager.equipment.toArray( array );
		array[ EquipmentManager.FAMILIAR ] = EquipmentManager.getFamiliarItem();
		return array;
	}

	public static final void processResult( AdventureResult item )
	{
		int itemId = item.getItemId();

		// If your current familiar can use this item, add it to familiar item list
		if ( KoLCharacter.getFamiliar().canEquip( item ) )
		{
			AdventureResult.addResultToList( EquipmentManager.equipmentLists[ EquipmentManager.FAMILIAR ], item );
			if ( ItemDatabase.getConsumptionType( itemId ) == KoLConstants.EQUIP_FAMILIAR )
			{
				return;
			}
			// Even though the familiar can use it, it's not a
			// familiar item. Continue processing, in case the
			// character can also use the item
		}

		if ( !EquipmentManager.canEquip( itemId ) )
		{
			return;
		}

		int consumeType = ItemDatabase.getConsumptionType( itemId );
		if ( consumeType == KoLConstants.EQUIP_ACCESSORY )
		{
			AdventureResult.addResultToList( EquipmentManager.accessories, item );
		}
		else if ( consumeType == KoLConstants.CONSUME_STICKER )
		{
			// The available stickers cannot be combined into a
			// single list, as is done with accessories, since
			// stickers cannot be moved to a different slot.  If a
			// slot contains your last sticker of a particular
			// type, then that type must only appear for that slot
			// (so that it can be the initially selected value),
			// not in the other two slots.	There are only six
			// types of stickers, and no reason to believe that
			// there will ever be many (or even any) more, so this
			// duplication should not present a problem.
			AdventureResult.addResultToList( EquipmentManager.equipmentLists[ 
				EquipmentManager.STICKER1 ], item );
			AdventureResult.addResultToList( EquipmentManager.equipmentLists[ 
				EquipmentManager.STICKER2 ], item );
			AdventureResult.addResultToList( EquipmentManager.equipmentLists[ 
				EquipmentManager.STICKER3 ], item );
			
			// Make sure the current sticker in each slot remains
			// in the list, even if there are no more of that type
			// in inventory.
			if ( !EquipmentManager.equipmentLists[ EquipmentManager.STICKER1 ].contains(
				EquipmentManager.getEquipment( EquipmentManager.STICKER1 ) ) )
			{
				EquipmentManager.equipmentLists[ EquipmentManager.STICKER1 ].add(
					EquipmentManager.getEquipment( EquipmentManager.STICKER1 ) );
			}
			if ( !EquipmentManager.equipmentLists[ EquipmentManager.STICKER2 ].contains(
				EquipmentManager.getEquipment( EquipmentManager.STICKER2 ) ) )
			{
				EquipmentManager.equipmentLists[ EquipmentManager.STICKER2 ].add(
					EquipmentManager.getEquipment( EquipmentManager.STICKER2 ) );
			}
			if ( !EquipmentManager.equipmentLists[ EquipmentManager.STICKER3 ].contains(
				EquipmentManager.getEquipment( EquipmentManager.STICKER3 ) ) )
			{
				EquipmentManager.equipmentLists[ EquipmentManager.STICKER3 ].add(
					EquipmentManager.getEquipment( EquipmentManager.STICKER3 ) );
			}
		}
		else if ( itemId == ItemPool.HATSEAT )
		{
			AdventureResult.addResultToList( EquipmentManager.equipmentLists[ EquipmentManager.HAT ], item );
		}
		else
		{
			int equipmentType = EquipmentManager.consumeFilterToEquipmentType( consumeType );
			if ( equipmentType != -1 )
			{
				AdventureResult.addResultToList( EquipmentManager.equipmentLists[ equipmentType ], item );
			}

			if ( equipmentType == EquipmentManager.WEAPON || equipmentType == EquipmentManager.OFFHAND )
			{
				GearChangeFrame.updateWeapons();
			}
		}

		if ( EquipmentDatabase.getOutfitWithItem( item.getItemId() ) != -1 )
		{
			EquipmentManager.updateOutfits();
		}
	}

	public static final void setEquipment( final int slot, AdventureResult item )
	{
		// Variable slots do not include the fake hand
		if ( slot >= EquipmentManager.equipmentLists.length )
		{
			return;
		}

		AdventureResult old = EquipmentManager.getEquipment( slot );

		// Accessories are special in terms of testing for existence
		// in equipment lists -- they are all mirrors of accessories.

		switch ( slot )
		{
		case -1:	// unknown item - ignore it
			return;
			
		case ACCESSORY1:
		case ACCESSORY2:
		case ACCESSORY3:
			int index = EquipmentManager.accessories.indexOf( item );
			if ( index == -1 )
			{
				EquipmentManager.accessories.add( item );
			}
			else
			{
				item = (AdventureResult) EquipmentManager.accessories.get( index );
			}
			break;
			
		default:
			if ( !EquipmentManager.equipmentLists[ slot ].contains( item ) )
			{
				EquipmentManager.equipmentLists[ slot ].add( item );
			}
			break;
		}

		EquipmentManager.equipment.set( slot, item );
		EquipmentManager.equipmentLists[ slot ].setSelectedItem( item );
		EquipmentManager.historyLists[ slot ].remove( item );
		EquipmentManager.historyLists[ slot ].add( item );

		// Certain equipment slots require special update handling
		// in addition to the above code.

		switch ( slot )
		{
		case EquipmentManager.WEAPON:
		case EquipmentManager.OFFHAND:
			EquipmentManager.checkFamiliar( slot );
			GearChangeFrame.updateWeapons();
			break;

		case EquipmentManager.HAT:
			EquipmentManager.checkFamiliar( slot );
			break;

		case EquipmentManager.FAMILIAR:
			EquipmentManager.checkFamiliar( slot );
			KoLCharacter.currentFamiliar.setItem( item );
			break;
		}

		// Certain items provide additional skills when equipped.
		// Handle the addition of those skills here.

		switch ( item.getItemId() )
		{
		case ItemPool.BOTTLE_ROCKET:
			KoLCharacter.addAvailableSkill( "Fire red bottle-rocket" );
			KoLCharacter.addAvailableSkill( "Fire blue bottle-rocket" );
			KoLCharacter.addAvailableSkill( "Fire orange bottle-rocket" );
			KoLCharacter.addAvailableSkill( "Fire purple bottle-rocket" );
			KoLCharacter.addAvailableSkill( "Fire black bottle-rocket" );
			break;
		case ItemPool.JEWEL_EYED_WIZARD_HAT:
			KoLCharacter.addAvailableSkill( "Magic Missile" );
			break;
		case ItemPool.BAKULA:
			KoLCharacter.addAvailableSkill( "Give In To Your Vampiric Urges" );
			break;
		case ItemPool.HAIKU_KATANA:
			KoLCharacter.addAvailableSkill( "Spring Raindrop Attack" );
			KoLCharacter.addAvailableSkill( "Summer Siesta" );
			KoLCharacter.addAvailableSkill( "Falling Leaf Whirlwind" );
			KoLCharacter.addAvailableSkill( "Winter's Bite Technique" );
			KoLCharacter.addAvailableSkill( "The 17 Cuts" );
			break;
		case ItemPool.BAG_O_TRICKS:
			KoLCharacter.addAvailableSkill( "Open the Bag o' Tricks" );
			break;
		case ItemPool.RED_AND_GREEN_SWEATER:
			KoLCharacter.addAvailableSkill( "Static Shock" );
			break;
		case ItemPool.STINKY_CHEESE_EYE:
			KoLCharacter.addAvailableSkill( "Give Your Opponent the Stinkeye" );
			break;
		case ItemPool.SPIDER_RING:
			KoLCharacter.addAvailableSkill( "Shoot Web" );
			break;
		case ItemPool.PATRIOT_SHIELD:
			KoLCharacter.addAvailableSkill( "Throw Shield" );
			break;
		}

		// If we are either swapping out or in a stinky cheese item,
		// recalculate stinky cheese level.
		if ( ItemDatabase.isStinkyCheeseItem( old.getItemId() ) ||
		     ItemDatabase.isStinkyCheeseItem( item.getItemId() ) )
		{
			AdventureResult weapon = EquipmentManager.getEquipment( WEAPON );
			AdventureResult offhand = EquipmentManager.getEquipment( OFFHAND );
			AdventureResult pants = EquipmentManager.getEquipment( PANTS );
			AdventureResult acc1 = EquipmentManager.getEquipment( ACCESSORY1 );
			AdventureResult acc2 = EquipmentManager.getEquipment( ACCESSORY2 );
			AdventureResult acc3 = EquipmentManager.getEquipment( ACCESSORY3 );

			boolean sword = weapon.getItemId() == ItemPool.STINKY_CHEESE_SWORD ||
				offhand.getItemId() == ItemPool.STINKY_CHEESE_SWORD;
			boolean staff = weapon.getItemId() == ItemPool.STINKY_CHEESE_STAFF;
			boolean diaper = pants.getItemId() == ItemPool.STINKY_CHEESE_DIAPER;
			boolean wheel = offhand.getItemId() == ItemPool.STINKY_CHEESE_WHEEL;
			boolean eye = acc1.getItemId() == ItemPool.STINKY_CHEESE_EYE ||
				acc2.getItemId() == ItemPool.STINKY_CHEESE_EYE ||
				acc3.getItemId() == ItemPool.STINKY_CHEESE_EYE;

			EquipmentManager.stinkyCheeseLevel =
				( sword ? 1 : 0 ) +
				( staff ? 1 : 0 ) +
				( diaper ? 1 : 0 ) +
				( wheel ? 1 : 0 ) +
				( eye ? 1 : 0 );
		}
	}
	
	public static final void transformEquipment( AdventureResult before, AdventureResult after )
	{
		SpecialOutfit.forgetEquipment( before );
		for ( int slot = 0 ; slot <= EquipmentManager.FAMILIAR ; ++slot )
		{
			if ( KoLCharacter.hasEquipped( before, slot ) )
			{
				EquipmentManager.setEquipment( slot, EquipmentRequest.UNEQUIP );
				// FamiliarData.setItem moved the current
				// familiar item to inventory when we
				// unequipped it above
				if ( slot != EquipmentManager.FAMILIAR )
				{
					AdventureResult.addResultToList( KoLConstants.inventory, before );
				}
				ResultProcessor.processResult( before.getInstance( -1 ) );
				EquipmentManager.setEquipment( slot, after );
				return;
			}
		}
		RequestLogger.printLine( "(unable to determine slot of transformed equipment)" );
	}

	public static final int discardEquipment( final int itemId )
	{
		AdventureResult item = ItemPool.get( itemId, 1 );
		return EquipmentManager.discardEquipment( item );
	}

	public static final int discardEquipment( final AdventureResult item )
	{
		SpecialOutfit.forgetEquipment( item );
		for ( int slot = 0 ; slot <= EquipmentManager.FAMILIAR ; ++slot )
		{
			if ( KoLCharacter.hasEquipped( item, slot ) )
			{
				EquipmentManager.setEquipment( slot, EquipmentRequest.UNEQUIP );
				// FamiliarData.setItem moved the current
				// familiar item to inventory when we
				// unequipped it above
				if ( slot != EquipmentManager.FAMILIAR )
				{
					AdventureResult.addResultToList( KoLConstants.inventory, item );
				}
				ResultProcessor.processItem( item.getItemId(), -1 );
				return slot;
			}
		}
		return -1;
	}
	
	public static final void breakEquipment( int itemId, String msg )
	{
		// Breaking sugar equipment resets sugar counter
		switch ( itemId )
		{
		case ItemPool.SUGAR_CHAPEAU:
		case ItemPool.SUGAR_SHANK:
		case ItemPool.SUGAR_SHIELD:
		case ItemPool.SUGAR_SHILLELAGH:
		case ItemPool.SUGAR_SHIRT:
		case ItemPool.SUGAR_SHOTGUN:
		case ItemPool.SUGAR_SHORTS:
			Preferences.setInteger( "sugarCounter" + String.valueOf( itemId ), 0 );
			break;
		}

		int slot = EquipmentManager.discardEquipment( itemId );
		if ( slot == -1 )
		{
			RequestLogger.printLine( "(unable to determine slot of broken equipment)" );
			return;
		}
		AdventureResult item = ItemPool.get( itemId, 1 );
		
		int action = Preferences.getInteger( "breakableHandling" + itemId );
		if ( action == 0 )
		{
			action = Preferences.getInteger( "breakableHandling" );
		}
		// 1: abort
		// 2: equip previous
		// 3: re-equip from inventory, or abort
		// 4: re-equip from inventory, or previous
		// 5: acquire & re-equip
		if ( action >= 5 )
		{
			InventoryManager.retrieveItem( item );
			action -= 2;
		}
		if ( action >= 3 )
		{
			if ( InventoryManager.hasItem( item ) )
			{
				RequestLogger.printLine( msg );
				RequestThread.postRequest( new EquipmentRequest( item, slot ) );
				return;
			}
			action -= 2;
		}
		if ( action <= 1 )
		{
			KoLmafia.updateDisplay( KoLConstants.PENDING_STATE, msg );
			return;
		}
		ArrayList list = EquipmentManager.historyLists[ slot ];
		for ( int i = list.size() - 1; i >= 0; --i )
		{
			AdventureResult prev = (AdventureResult) list.get( i );
			if ( prev.equals( EquipmentRequest.UNEQUIP ) ||
				prev.equals( item ) ||
				!InventoryManager.hasItem( prev ) ||
				( slot == EquipmentManager.FAMILIAR &&
				  !KoLCharacter.getFamiliar().canEquip( prev ) ) )
			{
				continue;
			}
			RequestLogger.printLine( msg );
			RequestThread.postRequest( new EquipmentRequest( prev, slot ) );
			return;
		}
		KoLmafia.updateDisplay( KoLConstants.PENDING_STATE,
			msg + "  No previous item to equip." );
	}

	public static final void checkFamiliar( final int slot )
	{
		switch ( KoLCharacter.getFamiliar().getId() )
		{
		case FamiliarPool.HATRACK:
			if ( slot == EquipmentManager.HAT )
			{
				EquipmentManager.updateEquipmentList( EquipmentManager.FAMILIAR );
			}
			else if ( slot == EquipmentManager.FAMILIAR )
			{
				EquipmentManager.updateEquipmentList( EquipmentManager.HAT );
			}
			break;

		case FamiliarPool.HAND:
			if ( slot == EquipmentManager.WEAPON || slot == EquipmentManager.OFFHAND )
			{
				EquipmentManager.updateEquipmentList( EquipmentManager.FAMILIAR );
			}
			else if ( slot == EquipmentManager.FAMILIAR )
			{
				EquipmentManager.updateEquipmentList( EquipmentManager.WEAPON );
				EquipmentManager.updateEquipmentList( EquipmentManager.OFFHAND );
			}
			break;

		case FamiliarPool.SCARECROW:
			if ( slot == EquipmentManager.PANTS )
			{
				EquipmentManager.updateEquipmentList( EquipmentManager.FAMILIAR );
			}
			else if ( slot == EquipmentManager.FAMILIAR )
			{
				EquipmentManager.updateEquipmentList( EquipmentManager.PANTS );
			}
			break;
		}
	}

	/**
	 * Accessor method to set the equipment the character is currently
	 * using. This does not take into account the power of the item or
	 * anything of that nature; only the item's name is stored. Note that
	 * if no item is equipped, the value should be <code>none</code>, not
	 * <code>null</code> or the empty string.
	 *
	 * @param equipment All of the available equipment, stored in an array
	 * index by the constants
	 */

	public static final void setEquipment( final AdventureResult[] equipment )
	{
		// Sanity check: must set ALL equipment slots

		if ( equipment.length != EquipmentManager.SLOTS )
		{
			StaticEntity.printStackTrace( "Equipment array slot mismatch: " + EquipmentManager.SLOTS + " expected, " + equipment.length + " provided." );
			return;
		}

		for ( int i = 0; i < EquipmentManager.SLOTS; ++i )
		{
			if ( equipment[ i ] == null || equipment[ i ].equals( EquipmentRequest.UNEQUIP ) )
			{
				setEquipment( i, EquipmentRequest.UNEQUIP );
			}
			else
			{
				setEquipment( i, equipment[ i ] );
			}
		}
	}

	public static final void setOutfits( final List newOutfits )
	{
		// Rebuild outfits if given a new list
		if ( newOutfits != null )
		{
			customOutfits.clear();
			customOutfits.add( SpecialOutfit.NO_CHANGE );
			customOutfits.addAll( newOutfits );
		}
	}

	/**
	 * Accessor method to retrieve the name of the item equipped on the
	 * character's familiar.
	 *
	 * @return The name of the item equipped on the character's familiar,
	 * <code>none</code> if no such item exists
	 */

	public static final AdventureResult getFamiliarItem()
	{
		return KoLCharacter.currentFamiliar == null ? EquipmentRequest.UNEQUIP : KoLCharacter.currentFamiliar.getItem();
	}

	public static final AdventureResult lockedFamiliarItem()
	{
		return EquipmentManager.lockedFamiliarItem;
	}

	public static final boolean familiarItemLockable()
	{
		return FamiliarData.lockableItem( EquipmentManager.getFamiliarItem() );
	}

	public static final void lockFamiliarItem()
	{
		EquipmentManager.lockFamiliarItem( EquipmentManager.familiarItemLocked() );
	}

	public static final boolean familiarItemLocked()
	{
		return EquipmentManager.lockedFamiliarItem != EquipmentRequest.UNEQUIP;
	}

	public static final void lockFamiliarItem( boolean lock )
	{
		EquipmentManager.lockedFamiliarItem =
			lock ? EquipmentManager.getFamiliarItem() : EquipmentRequest.UNEQUIP;
		GearChangeFrame.updateFamiliarLock();
		PreferenceListenerRegistry.firePreferenceChanged( "(familiarLock)" );
	}

	public static final void lockFamiliarItem( FamiliarData familiar )
	{
		EquipmentManager.lockedFamiliarItem = familiar.getItem();
		GearChangeFrame.updateFamiliarLock();
		PreferenceListenerRegistry.firePreferenceChanged( "(familiarLock)" );
	}

	public static final int getFakeHands()
	{
		return EquipmentManager.fakeHandCount;
	}

	public static final void setFakeHands( final int hands )
	{
		EquipmentManager.fakeHandCount = hands;
	}

	public static final int getStinkyCheeseLevel()
	{
		return EquipmentManager.stinkyCheeseLevel;
	}

	/**
	 * Accessor method to retrieve the name of a piece of equipment
	 *
	 * @param type the type of equipment
	 * @return The name of the equipment, <code>none</code> if no such item exists
	 */

	public static final AdventureResult getEquipment( final int type )
	{
		if ( type == EquipmentManager.FAMILIAR )
		{
			return getFamiliarItem();
		}

		if ( type >= 0 && type < equipment.size() )
		{
			return (AdventureResult) equipment.get( type );
		}

		return EquipmentRequest.UNEQUIP;
	}
	
	public static final int getTurns( int slot )
	{
		return EquipmentManager.turnsRemaining[ slot - EquipmentManager.STICKER1 ];
	}
	
	public static final void setTurns( int slot, int minTurns, int maxTurns )
	{
		int curr = EquipmentManager.turnsRemaining[ slot - EquipmentManager.STICKER1 ];
		if ( curr > maxTurns )
		{
			curr = maxTurns;
		}
		if ( curr < minTurns )
		{
			curr = minTurns;
		}
		EquipmentManager.turnsRemaining[ slot - EquipmentManager.STICKER1 ] = curr;
		GearChangeFrame.updateStickers(
			EquipmentManager.turnsRemaining[ 0 ],
			EquipmentManager.turnsRemaining[ 1 ],
			EquipmentManager.turnsRemaining[ 2 ] );
	}
	
	public static final boolean isStickerWeapon( AdventureResult item )
	{
		return item != null && isStickerWeapon( item.getItemId() );
	}
	
	public static final boolean isStickerWeapon( int itemId )
	{
		return itemId == ItemPool.STICKER_SWORD || itemId == ItemPool.STICKER_CROSSBOW;
	}
	
	public static final boolean usingStickerWeapon()
	{
		return isStickerWeapon( getEquipment( EquipmentManager.WEAPON ) ) ||
			isStickerWeapon( getEquipment( EquipmentManager.OFFHAND ) ) ||
			isStickerWeapon( getEquipment( EquipmentManager.FAMILIAR ) );
	}

	public static final boolean usingStickerWeapon( AdventureResult[] equipment )
	{
		return isStickerWeapon( equipment[ EquipmentManager.WEAPON ] ) ||
			isStickerWeapon( equipment[ EquipmentManager.OFFHAND ] ) ||
			isStickerWeapon( equipment[ EquipmentManager.FAMILIAR ] );
	}

	public static final boolean hasStickerWeapon()
	{
		return EquipmentManager.usingStickerWeapon() ||
			InventoryManager.hasItem( ItemPool.STICKER_SWORD ) ||
			InventoryManager.hasItem( ItemPool.STICKER_CROSSBOW );
	}

	public static final void incrementSugarCounters()
	{
		for ( int i = 0; i < EquipmentManager.SLOTS; ++i )
		{
			int itemId = EquipmentManager.getEquipment( i ).getItemId();
			switch ( itemId )
			{
			case ItemPool.SUGAR_CHAPEAU:
			case ItemPool.SUGAR_SHANK:
			case ItemPool.SUGAR_SHIELD:
			case ItemPool.SUGAR_SHILLELAGH:
			case ItemPool.SUGAR_SHIRT:
			case ItemPool.SUGAR_SHOTGUN:
			case ItemPool.SUGAR_SHORTS:
				Preferences.increment( "sugarCounter" + String.valueOf( itemId ), 1 );
				break;
			}
		}
	}
	
	public static final void decrementTurns()
	{
		if ( usingStickerWeapon() )
		{
			GearChangeFrame.updateStickers(
				--EquipmentManager.turnsRemaining[ 0 ],
				--EquipmentManager.turnsRemaining[ 1 ],
				--EquipmentManager.turnsRemaining[ 2 ] );
		}

		EquipmentManager.incrementSugarCounters();
	}
	
	public static final void stickersExpired( int count )
	{
		for ( int i = 0; i < 3; ++i )
		{
			if ( EquipmentManager.turnsRemaining[ i ] <= 0 &&
				getEquipment( EquipmentManager.STICKER1 + i ) != EquipmentRequest.UNEQUIP )
			{
				setEquipment( EquipmentManager.STICKER1 + i, EquipmentRequest.UNEQUIP );
				--count;
			}
		}
		if ( count != 0 )	// we've lost count somewhere, refresh
		{
			RequestThread.postRequest( new EquipmentRequest( EquipmentRequest.BEDAZZLEMENTS ) );
		}
	}

	/**
	 * Accessor method to retrieve a list of all available items which can be equipped by familiars. Note this lists
	 * items which the current familiar cannot equip.
	 */

	public static final LockableListModel[] getEquipmentLists()
	{
		return EquipmentManager.equipmentLists;
	}

	public static final void updateEquipmentList( final int listIndex )
	{
		int consumeFilter = EquipmentManager.equipmentTypeToConsumeFilter( listIndex );
		if ( consumeFilter == -1 )
		{
			return;
		}

		AdventureResult equippedItem = EquipmentManager.getEquipment( listIndex );

		switch ( listIndex )
		{
		case EquipmentManager.ACCESSORY1:
		case EquipmentManager.ACCESSORY2:
			return;	// do all the work when updating ACC3
			
		case EquipmentManager.ACCESSORY3:

			EquipmentManager.updateEquipmentList( consumeFilter, EquipmentManager.accessories );
			AdventureResult accessory = EquipmentManager.getEquipment( EquipmentManager.ACCESSORY1 );
			if ( accessory != EquipmentRequest.UNEQUIP )
			{
				AdventureResult.addResultToList( EquipmentManager.accessories, accessory );
			}
			accessory = EquipmentManager.getEquipment( EquipmentManager.ACCESSORY2 );
			if ( accessory != EquipmentRequest.UNEQUIP )
			{
				AdventureResult.addResultToList( EquipmentManager.accessories, accessory );
			}
			accessory = EquipmentManager.getEquipment( EquipmentManager.ACCESSORY3 );
			if ( accessory != EquipmentRequest.UNEQUIP )
			{
				AdventureResult.addResultToList( EquipmentManager.accessories, accessory );
			}
			break;

		case EquipmentManager.FAMILIAR:

			// If we are looking at familiar items, include those
			// which can be universally equipped, but are currently
			// on another familiar.

			EquipmentManager.updateEquipmentList( consumeFilter, EquipmentManager.equipmentLists[ EquipmentManager.FAMILIAR ] );

			FamiliarData[] familiarList = new FamiliarData[ KoLCharacter.familiars.size() ];
			KoLCharacter.familiars.toArray( familiarList );

			FamiliarData currentFamiliar = KoLCharacter.getFamiliar();

			for ( int i = 0; i < familiarList.length; ++i )
			{
				AdventureResult currentItem = familiarList[ i ].getItem();
				if ( currentItem != EquipmentRequest.UNEQUIP && currentFamiliar.canEquip( currentItem ) )
				{
					AdventureResult.addResultToList( EquipmentManager.equipmentLists[ EquipmentManager.FAMILIAR ], currentItem );
				}
			}

			break;

		default:

			EquipmentManager.updateEquipmentList( consumeFilter, EquipmentManager.equipmentLists[ listIndex ] );
			if ( !EquipmentManager.equipmentLists[ listIndex ].contains( equippedItem ) )
			{
				EquipmentManager.equipmentLists[ listIndex ].add( equippedItem );
			}

			break;
		}

		EquipmentManager.equipmentLists[ listIndex ].setSelectedItem( equippedItem );
	}

	private static final void updateEquipmentList( final int filterId, final List currentList )
	{
		ArrayList temporary = new ArrayList();
		temporary.add( EquipmentRequest.UNEQUIP );

		// If the character is currently equipped with a one-handed
		// weapon and the character has the ability to dual-wield
		// weapons, then also allow one-handed weapons in the off-hand.

		boolean dual = getWeaponHandedness() == 1 && KoLCharacter.hasSkill( "Double-Fisted Skull Smashing" );
		int weaponType = EquipmentManager.getWeaponType();
		FamiliarData currentFamiliar = KoLCharacter.getFamiliar();

		for ( int i = 0; i < KoLConstants.inventory.size(); ++i )
		{
			AdventureResult currentItem = (AdventureResult) KoLConstants.inventory.get( i );
			String currentItemName = currentItem.getName();

			int type = ItemDatabase.getConsumptionType( currentItem.getItemId() );

			// If we want off-hand items and we can dual wield,
			// allow one-handed weapons of same type

			if ( filterId == KoLConstants.EQUIP_OFFHAND && type == KoLConstants.EQUIP_WEAPON && dual )
			{
				if ( EquipmentDatabase.getHands( currentItemName ) != 1 || EquipmentDatabase.getWeaponType( currentItemName ) != weaponType )
				{
					continue;
				}
			}

			// If we are equipping familiar items, make sure
			// current familiar can use this one

			else if ( filterId == KoLConstants.EQUIP_FAMILIAR )
			{
				if ( currentFamiliar.canEquip( currentItem ) )
				{
					temporary.add( currentItem.getInstance( 1 ) );
				}

				continue;
			}

			// Otherwise, slot and item type must match

			else if ( filterId != type )
			{
				continue;
			}

			else if ( filterId == KoLConstants.EQUIP_WEAPON && dual )
			{
				if ( EquipmentDatabase.getHands( currentItemName ) == 1 && EquipmentDatabase.getWeaponType( currentItemName ) != weaponType )
				{
					continue;
				}
			}

			temporary.add( currentItem );
		}

		currentList.retainAll( temporary );
		temporary.removeAll( currentList );
		currentList.addAll( temporary );
	}

	/**
	 * Accessor method to retrieve a list of the custom outfits available to this character, based on the last time the
	 * equipment screen was requested.
	 *
	 * @return A <code>LockableListModel</code> of the available outfits
	 */

	public static final LockableListModel getCustomOutfits()
	{
		return customOutfits;
	}

	public static SpecialOutfit getCustomOutfit( int id )
	{
		Iterator i = customOutfits.iterator();
		while ( i.hasNext() )
		{
			Object object = i.next();
			if ( !( object instanceof SpecialOutfit ) )
			{
				continue;
			}
			SpecialOutfit outfit = (SpecialOutfit) object;
			if ( outfit.getOutfitId() == id )
			{
				return outfit;
			}
		}
		return null;
	}

	public static SpecialOutfit getCustomOutfit( String name )
	{
		Iterator i = customOutfits.iterator();
		while ( i.hasNext() )
		{
			Object object = i.next();
			if ( !( object instanceof SpecialOutfit ) )
			{
				continue;
			}
			SpecialOutfit outfit = (SpecialOutfit) object;
			if ( outfit.getName().equals( name ) )
			{
				return outfit;
			}
		}
		return null;
	}

	/**
	 * Accessor method to add or replace a custom outfit in the list of
	 * custom outfits available to this character
	 */

	public static void addCustomOutfit( SpecialOutfit outfit )
	{
		String name = outfit.getName();
		SortedListModel outfits = new SortedListModel();
		Iterator i = customOutfits.iterator();
		while ( i.hasNext() )
		{
			Object object = i.next();
			if ( !( object instanceof SpecialOutfit ) )
			{
				continue;
			}
			SpecialOutfit current = (SpecialOutfit) object;
			if ( !current.getName().equals( name ) )
			{
				outfits.add( current );
			}
		}
		outfits.add( outfit );
		SpecialOutfit.checkImplicitOutfit( outfit );
		EquipmentManager.setOutfits( outfits );
	}

	/**
	 * Accessor method to retrieve a list of the all the outfits available to this character, based on the last time the
	 * equipment screen was requested.
	 *
	 * @return A <code>LockableListModel</code> of the available outfits
	 */

	public static final LockableListModel getOutfits()
	{
		return outfits;
	}

	public static final void updateEquipmentLists()
	{
		KoLCharacter.resetTriggers();
		EquipmentManager.updateOutfits();
		for ( int i = 0; i < EquipmentManager.ALL_SLOTS; ++i )
		{
			updateEquipmentList( i );
		}
	}

	public static final int equipmentTypeToConsumeFilter( final int equipmentType )
	{
		switch ( equipmentType )
		{
		case EquipmentManager.HAT:
			return KoLConstants.EQUIP_HAT;
		case EquipmentManager.WEAPON:
			return KoLConstants.EQUIP_WEAPON;
		case EquipmentManager.OFFHAND:
			return KoLConstants.EQUIP_OFFHAND;
		case EquipmentManager.SHIRT:
			return KoLConstants.EQUIP_SHIRT;
		case EquipmentManager.PANTS:
			return KoLConstants.EQUIP_PANTS;
		case EquipmentManager.ACCESSORY1:
		case EquipmentManager.ACCESSORY2:
		case EquipmentManager.ACCESSORY3:
			return KoLConstants.EQUIP_ACCESSORY;
		case EquipmentManager.FAMILIAR:
			return KoLConstants.EQUIP_FAMILIAR;
		case EquipmentManager.STICKER1:
		case EquipmentManager.STICKER2:
		case EquipmentManager.STICKER3:
			return KoLConstants.CONSUME_STICKER;
		default:
			return -1;
		}
	}

	public static final int consumeFilterToEquipmentType( final int consumeFilter )
	{
		switch ( consumeFilter )
		{
		case KoLConstants.EQUIP_HAT:
			return EquipmentManager.HAT;
		case KoLConstants.EQUIP_WEAPON:
			return EquipmentManager.WEAPON;
		case KoLConstants.EQUIP_OFFHAND:
			return EquipmentManager.OFFHAND;
		case KoLConstants.EQUIP_SHIRT:
			return EquipmentManager.SHIRT;
		case KoLConstants.EQUIP_PANTS:
			return EquipmentManager.PANTS;
		case KoLConstants.EQUIP_ACCESSORY:
			return EquipmentManager.ACCESSORY1;
		case KoLConstants.EQUIP_FAMILIAR:
			return EquipmentManager.FAMILIAR;
		case KoLConstants.CONSUME_STICKER:
			return EquipmentManager.STICKER1;
		default:
			return -1;
		}
	}

	public static final int itemIdToEquipmentType( final int itemId )
	{
		return EquipmentManager.consumeFilterToEquipmentType( ItemDatabase.getConsumptionType( itemId ) );
	}

	/**
	 * Accessor method to retrieve # of hands character's weapon uses
	 *
	 * @return int number of hands needed
	 */

	public static final int getWeaponHandedness()
	{
		return EquipmentDatabase.getHands( EquipmentManager.getEquipment( EquipmentManager.WEAPON ).getName() );
	}

	/**
	 * Accessor method to determine if character is currently dual-wielding
	 *
	 * @return boolean true if character has two weapons equipped
	 */

	public static final boolean usingTwoWeapons()
	{
		return EquipmentDatabase.getHands( EquipmentManager.getEquipment( EquipmentManager.OFFHAND ).getName() ) == 1;
	}

	/**
	 * Accessor method to determine if character's weapon is a chefstaff
	 *
	 * @return boolean true if weapon is a chefstaff
	 */

	public static final boolean usingChefstaff()
	{
		return EquipmentDatabase.isChefStaff( EquipmentManager.getEquipment( EquipmentManager.WEAPON ) );
	}

	/**
	 * Accessor method to determine if character's weapon's is a club
	 *
	 * @return boolean true if weapon is a club
	 */

	public static final boolean wieldingClub()
	{
		return EquipmentManager.wieldingClub( true );
	}

	/**
	 * Here's a version which allows you to include or exclude the Iron Palm
	 * effect for the purpose of determining whether a sword counts as a
	 * club. Mother Hellseals require an actual club...
	 */

	public static final AdventureResult IRON_PALM = EffectPool.get( "Iron Palm" );

	public static final boolean wieldingClub( final boolean includeEffect )
	{
		String type = EquipmentDatabase.getItemType( EquipmentManager.getEquipment( EquipmentManager.WEAPON ).getItemId() );
		return type.equals( "club" ) ||
			( includeEffect && KoLConstants.activeEffects.contains( EquipmentManager.IRON_PALM ) && type.equals( "sword" ) );
	}

	/**
	 * Accessor method to determine if character is currently using a shield
	 *
	 * @return boolean true if character has a shield equipped
	 */

	public static final boolean usingShield()
	{
		return EquipmentDatabase.getItemType( EquipmentManager.getEquipment( OFFHAND ).getItemId() ).equals( "shield" );
	}

	/**
	 * Accessor method to determine what type of weapon the character is
	 * wielding.
	 *
	 * @return int MELEE or RANGED
	 */

	public static final int getWeaponType()
	{
		return EquipmentDatabase.getWeaponType( EquipmentManager.getEquipment( EquipmentManager.WEAPON ).getName() );
	}

	/**
	 * Accessor method to determine which stat determines the character's
	 * chance to hit.
	 *
	 * @return int MOXIE or MUSCLE
	 */

	public static final int getHitStatType()
	{
		switch ( EquipmentManager.getWeaponType() )
		{
		case KoLConstants.RANGED:
			return KoLConstants.MOXIE;
		default:
			return KoLConstants.MUSCLE;
		}
	}

	/**
	 * Accessor method to determine character's adjusted hit stat
	 *
	 * @return int adjusted muscle, mysticality, or moxie
	 */

	public static final int getAdjustedHitStat()
	{
		switch ( getHitStatType() )
		{
		default:
		case KoLConstants.MUSCLE:
			int hitStat = KoLCharacter.getAdjustedMuscle();
			if ( Modifiers.unarmed && KoLCharacter.hasSkill( "Master of the Surprising Fist" ) )
			{
				hitStat += 20;
			}
			return hitStat;
		case KoLConstants.MYSTICALITY:
			return KoLCharacter.getAdjustedMysticality();
		case KoLConstants.MOXIE:
			return KoLCharacter.getAdjustedMoxie();
		}
	}

	public static final boolean hasOutfit( final int id )
	{
		return getOutfits().contains( EquipmentDatabase.normalOutfits.get( id ) );
	}

	public static final void updateOutfits()
	{
		ArrayList available = new ArrayList();

		for ( int i = 0; i < EquipmentDatabase.normalOutfits.size(); ++i )
		{
			SpecialOutfit outfit = EquipmentDatabase.normalOutfits.get( i );
			
			if ( outfit != null && outfit.hasAllPieces() )
			{
				available.add( outfit );
			}
		}

		for ( int i = 0; i < EquipmentDatabase.weirdOutfits.size(); ++i )
		{
			SpecialOutfit outfit = EquipmentDatabase.weirdOutfits.get( i );
			
			if ( outfit != null && outfit.hasAllPieces() )
			{
				available.add( outfit );
			}
		}

		Collections.sort( available );
		
		List outfits = getOutfits();
		
		outfits.clear();

		// Start with the three constant outfits
		outfits.add( SpecialOutfit.NO_CHANGE );
		outfits.add( SpecialOutfit.BIRTHDAY_SUIT );
		// *** KoL bug: outfitid=last gets confused sometimes.
		// *** If/when this is fixed, uncomment this
		// outfits.add( SpecialOutfit.PREVIOUS_OUTFIT );

		// Finally any standard outfits
		outfits.addAll( available );

		// We may have gotten the war hippy or frat outfits
		CoinmastersFrame.externalUpdate();
	}

	/**
	 * Utility method which determines whether or not the equipment corresponding to the given outfit is already
	 * equipped.
	 */

	public static final boolean isWearingOutfit( final int outfitId )
	{
		if ( outfitId < 0 )
		{
			return true;
		}

		if ( outfitId == 0 )
		{
			return false;
		}

		return EquipmentManager.isWearingOutfit( EquipmentDatabase.normalOutfits.get( outfitId ) );
	}

	/**
	 * Utility method which determines whether or not the equipment
	 * corresponding to the given outfit is already equipped.
	 */

	public static final boolean isWearingOutfit( final SpecialOutfit outfit )
	{
		return outfit != null && outfit.isWearing();
	}

	public static final boolean retrieveOutfit( final int outfitId )
	{
		if ( outfitId < 0 || outfitId == Integer.MAX_VALUE )
		{
			return true;
		}

		AdventureResult[] pieces = EquipmentDatabase.normalOutfits.get( outfitId ).getPieces();

		for ( int i = 0; i < pieces.length; ++i )
		{
			if ( !KoLCharacter.hasEquipped( pieces[ i ] ) && !InventoryManager.retrieveItem( pieces[ i ] ) )
			{
				return false;
			}
		}

		return true;
	}

	public static final boolean addOutfitConditions( final KoLAdventure adventure )
	{
		int outfitId = EquipmentDatabase.getOutfitId( adventure );
		if ( outfitId <= 0 )
		{
			return false;
		}

		EquipmentManager.addOutfitConditions( outfitId );
		return true;
	}

	public static final void addOutfitConditions( final int outfitId )
	{
		// Ignore custom outfits, since there's
		// no way to know what they are (yet).

		if ( outfitId < 0 )
		{
			return;
		}

		AdventureResult[] pieces = EquipmentDatabase.normalOutfits.get( outfitId ).getPieces();
		for ( int i = 0; i < pieces.length; ++i )
		{
			if ( !KoLCharacter.hasEquipped( pieces[ i ] ) )
			{
				ConditionsCommand.update( "set", pieces[ i ].getName() );
			}
		}
	}

	/**
	 * Utility method which determines the outfit ID the character is
	 * currently wearing
	 */

	public static final SpecialOutfit currentOutfit()
	{
		for ( int id = 1; id < EquipmentDatabase.normalOutfits.size(); ++id )
		{
			SpecialOutfit outfit = EquipmentDatabase.normalOutfits.get( id );
			if ( outfit == null )
			{
				continue;
			}
			if ( outfit.isWearing() )
			{
				return outfit;
			}
		}

		return null;
	}

	public static final SpecialOutfit currentOutfit( AdventureResult[] equipment )
	{
		int hash = SpecialOutfit.equipmentHash( equipment );
		
		int size = EquipmentDatabase.normalOutfits.size();
		for ( int id = 1; id < size; ++id )
		{
			SpecialOutfit outfit = EquipmentDatabase.normalOutfits.get( id );
			if ( outfit == null )
			{
				continue;
			}
			if ( outfit.isWearing( equipment, hash ) )
			{
				return outfit;
			}
		}

		return null;
	}

	public static final boolean canEquip( final AdventureResult item )
	{
		return EquipmentManager.canEquip( item.getItemId() );
	}

	public static final boolean canEquip( final String itemName )
	{
		return EquipmentManager.canEquip( ItemDatabase.getItemId( itemName ) );
	}

	public static final boolean canEquip( final int itemId )
	{
		if ( itemId == -1 )
		{
			return false;
		}

		int type = ItemDatabase.getConsumptionType( itemId );

		if ( type == KoLConstants.EQUIP_FAMILIAR )
		{
			return KoLCharacter.getFamiliar().canEquip( ItemPool.get( itemId, 1 ) );
		}

		if ( KoLCharacter.inFistcore() &&
		     ( type == KoLConstants.EQUIP_WEAPON || type == KoLConstants.EQUIP_OFFHAND ) )
		{
			return false;
		}
		
		if ( KoLCharacter.isHardcore() )
		{
			Modifiers mods = Modifiers.getModifiers( ItemDatabase.getItemName( itemId ) );
			if ( mods != null && mods.getBoolean( Modifiers.SOFTCORE ) )
			{
				return false;
			}
		}

		String requirement = EquipmentDatabase.getEquipRequirement( itemId );
		int req;

		if ( requirement.startsWith( "Mus:" ) )
		{
			req = StringUtilities.parseInt( requirement.substring( 5 ) );
			return KoLCharacter.getBaseMuscle() >= req ||
				KoLCharacter.muscleTrigger( req, itemId );
		}

		if ( requirement.startsWith( "Mys:" ) )
		{
			req = StringUtilities.parseInt( requirement.substring( 5 ) );
			return KoLCharacter.getBaseMysticality() >= req ||
				KoLCharacter.mysticalityTrigger( req, itemId );
		}

		if ( requirement.startsWith( "Mox:" ) )
		{
			req = StringUtilities.parseInt( requirement.substring( 5 ) );
			return KoLCharacter.getBaseMoxie() >= req ||
				KoLCharacter.moxieTrigger( req, itemId );
		}

		return true;
	}

	public static final SpecialOutfit getMatchingOutfit( final String name )
	{
		String lowercaseName = name.toLowerCase().trim();
	
		if ( lowercaseName.equals( "birthday suit" ) || lowercaseName.equals( "nothing" ) )
		{
			return SpecialOutfit.BIRTHDAY_SUIT;
		}
	
		if ( lowercaseName.equals( "last" ) )
		{
			return SpecialOutfit.PREVIOUS_OUTFIT;
		}
	
		List customOutfitList = getCustomOutfits();
		int customOutfitCount = customOutfitList.size();
		int normalOutfitCount = EquipmentDatabase.getOutfitCount();
	
		// Check for exact matches. Skip "No Change" entry at index 0.
	
		for ( int i = 1; i < customOutfitCount; ++i )
		{
			SpecialOutfit outfit = (SpecialOutfit) customOutfitList.get( i );
	
			if ( lowercaseName.equals( outfit.toString().toLowerCase() ) )
			{
				return outfit;
			}
		}
	
		for ( int i = 0; i < normalOutfitCount; ++i )
		{
			SpecialOutfit outfit = EquipmentDatabase.getOutfit( i );
	
			if ( outfit != null && lowercaseName.equals( outfit.toString().toLowerCase() ) )
			{
				return outfit;
			}
		}
	
		// Check for substring matches.
	
		for ( int i = 1; i < customOutfitCount; ++i )
		{
			SpecialOutfit outfit = (SpecialOutfit) customOutfitList.get( i );
	
			if ( outfit.toString().toLowerCase().indexOf( lowercaseName ) != -1 )
			{
				return outfit;
			}
		}
	
		for ( int i = 0; i < normalOutfitCount; ++i )
		{
			SpecialOutfit outfit = EquipmentDatabase.getOutfit( i );
	
			if ( outfit != null && outfit.toString().toLowerCase().indexOf( lowercaseName ) != -1 )
			{
				return outfit;
			}
		}
	
		return null;
	}

	public static final void parseStatus( final JSONObject JSON )
		throws JSONException
	{
		// "equipment":{
		//    "hat":"1323",
		//    "shirt":"2586",
		//    "pants":"1324",
		//    "weapon":"1325",
		//    "offhand":"1325",
		//    "acc1":"3337",
		//    "acc2":"1232",
		//    "acc3":"1226",
		//    "familiarequip":"3343",
		//    "fake hands":0
		// },
		// "stickers":[0,0,0],

		AdventureResult[] equipment = EquipmentManager.emptyEquipmentArray();
		int fakeHands = 0;

		JSONObject equip = JSON.getJSONObject( "equipment" );
		Iterator keys = equip.keys();
		while ( keys.hasNext() )
		{
			String slotName = (String) keys.next();
			if ( slotName.equals( "fakehands" ) )
			{
				fakeHands = equip.getInt( slotName );
				continue;
			}

			int slot = EquipmentRequest.phpSlotNumber( slotName );
			if ( slot == -1 )
			{
				continue;
			}

			int itemId = equip.getInt( slotName );
			String name = ItemDatabase.getItemDataName( itemId );
			if ( name == null )
			{
				// Fetch descid from api.php?what=item
				// and register new item.
				ItemDatabase.registerItem( itemId );
			}

			AdventureResult item = ItemPool.get( itemId, 1 );
			equipment[ slot ] = item;
		}

		EquipmentManager.setEquipment( equipment );
		EquipmentManager.setFakeHands( fakeHands );
		EquipmentManager.updateOutfits();

		// Read stickers
		JSONArray stickers = JSON.getJSONArray( "stickers" );
		for ( int i = 0; i < 3; ++i )
		{
			int itemId = stickers.getInt( i );
			AdventureResult item;
			if ( itemId == 0 )
			{
				item = EquipmentRequest.UNEQUIP;
			}
			else
			{
				String name = ItemDatabase.getItemDataName( itemId );
				if ( name == null )
				{
					// Fetch descid from api.php?what=item
					// and register new item.
					ItemDatabase.registerItem( itemId );
				}

				item = ItemPool.get( itemId, 1 );
			}

			EquipmentManager.setEquipment( EquipmentManager.STICKER1 + i, item );
		}

		// Check if familiar equipment is locked
		// *** how?
	}
}
