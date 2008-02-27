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

package net.sourceforge.kolmafia.session;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.java.dev.spellcast.utilities.LockableListModel;
import net.java.dev.spellcast.utilities.SortedListModel;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.FamiliarData;
import net.sourceforge.kolmafia.KoLAdventure;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafiaCLI;
import net.sourceforge.kolmafia.SpecialOutfit;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.EquipmentDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.request.EquipmentRequest;
import net.sourceforge.kolmafia.swingui.CoinmastersFrame;
import net.sourceforge.kolmafia.swingui.GearChangeFrame;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class EquipmentManager {

	public static final int HAT = 0;
	public static final int WEAPON = 1;
	public static final int OFFHAND = 2;
	public static final int SHIRT = 3;
	public static final int PANTS = 4;
	public static final int ACCESSORY1 = 5;
	public static final int ACCESSORY2 = 6;
	public static final int ACCESSORY3 = 7;
	public static final int FAMILIAR = 8;
	public static final int FAKEHAND = 9;

	private static LockableListModel equipment = new LockableListModel();
	private static final LockableListModel accessories = new SortedListModel();
	private static final LockableListModel[] equipmentLists = new LockableListModel[ 9 ];

	static
	{
		for ( int i = 0; i < 9; ++i )
		{
			EquipmentManager.equipment.add( EquipmentRequest.UNEQUIP );

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

	private static int fakeHandCount = 0;

	private static LockableListModel customOutfits = new LockableListModel();
	private static LockableListModel outfits = new LockableListModel();

	public static void resetEquipment()
	{
		for ( int i = 0; i < EquipmentManager.equipmentLists.length; ++i )
		{
			EquipmentManager.equipmentLists[ i ].clear();
		}

		EquipmentManager.accessories.clear();
		GearChangeFrame.clearWeaponLists();

		EquipmentManager.equipment.clear();

		for ( int i = 0; i < 9; ++i )
		{
			EquipmentManager.equipment.add( EquipmentRequest.UNEQUIP );
		}

		EquipmentManager.fakeHandCount = 0;
		EquipmentManager.customOutfits.clear();
		EquipmentManager.outfits.clear();
	}

	public static final void processResult( AdventureResult item )
	{
		int itemId = item.getItemId();

		if ( !EquipmentManager.canEquip( itemId ) )
		{
			return;
		}

		int consumeType = ItemDatabase.getConsumptionType( itemId );
		if ( consumeType == KoLConstants.EQUIP_ACCESSORY )
		{
			AdventureResult.addResultToList( EquipmentManager.accessories, item );
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
		// Accessories are special in terms of testing for existence
		// in equipment lists -- they are all mirrors of accessories.

		switch ( slot )
		{
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

		equipment.set( slot, item );
		EquipmentManager.equipmentLists[ slot ].setSelectedItem( item );

		// Certain equipment slots require special update handling
		// in addition to the above code.

		switch ( slot )
		{
		case WEAPON:
		case OFFHAND:
			GearChangeFrame.updateWeapons();
			break;

		case FAMILIAR:
			KoLCharacter.currentFamiliar.setItem( item );
			break;
		}

		// Certain items provide additional skills when equipped.
		// Handle the addition of those skills here.

		switch ( item.getItemId() )
		{
		case KoLCharacter.BOTTLE_ROCKET:
			KoLCharacter.addAvailableSkill( "Fire red bottle-rocket" );
			KoLCharacter.addAvailableSkill( "Fire blue bottle-rocket" );
			KoLCharacter.addAvailableSkill( "Fire orange bottle-rocket" );
			KoLCharacter.addAvailableSkill( "Fire purple bottle-rocket" );
			KoLCharacter.addAvailableSkill( "Fire black bottle-rocket" );
			break;
		case KoLCharacter.WIZARD_HAT:
			KoLCharacter.addAvailableSkill( "Magic Missile" );
			break;
		case KoLCharacter.BAKULA:
			KoLCharacter.addAvailableSkill( "Give In To Your Vampiric Urges" );
			break;
		}
	}

	/**
	 * Accessor method to set the equipment the character is currently using. This does not take into account the power
	 * of the item or anything of that nature; only the item's name is stored. Note that if no item is equipped, the
	 * value should be <code>none</code>, not <code>null</code> or the empty string.
	 *
	 * @param equipment All of the available equipment, stored in an array index by the constants
	 */

	public static final void setEquipment( final AdventureResult[] equipment )
	{
		for ( int i = HAT; i <= FAMILIAR; ++i )
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
			customOutfits.addAll( newOutfits );
		}

		EquipmentManager.updateOutfits();
	}

	/**
	 * Accessor method to retrieve the name of the item equipped on the character's familiar.
	 *
	 * @return The name of the item equipped on the character's familiar, <code>none</code> if no such item exists
	 */

	public static final AdventureResult getFamiliarItem()
	{
		return KoLCharacter.currentFamiliar == null ? EquipmentRequest.UNEQUIP : KoLCharacter.currentFamiliar.getItem();
	}

	public static final int getFakeHands()
	{
		return EquipmentManager.fakeHandCount;
	}

	public static final void setFakeHands( final int hands )
	{
		EquipmentManager.fakeHandCount = hands;
	}

	/**
	 * Accessor method to retrieve the name of a piece of equipment
	 *
	 * @param type the type of equipment
	 * @return The name of the equipment, <code>none</code> if no such item exists
	 */

	public static final AdventureResult getEquipment( final int type )
	{
		if ( type >= 0 && type < equipment.size() )
		{
			return (AdventureResult) equipment.get( type );
		}

		if ( type == FAMILIAR )
		{
			return getFamiliarItem();
		}

		return EquipmentRequest.UNEQUIP;
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

		AdventureResult equippedItem = getEquipment( listIndex );

		switch ( listIndex )
		{
		case ACCESSORY1:
		case ACCESSORY2:
		case ACCESSORY3:

			EquipmentManager.updateEquipmentList( consumeFilter, EquipmentManager.accessories );
			break;

		case FAMILIAR:

			// If we are looking at familiar items, include those which can
			// be universally equipped, but are currently on another
			// familiar.

			EquipmentManager.updateEquipmentList( consumeFilter, EquipmentManager.equipmentLists[ listIndex ] );

			FamiliarData[] familiarList = new FamiliarData[ KoLCharacter.familiars.size() ];
			KoLCharacter.familiars.toArray( familiarList );

			for ( int i = 0; i < familiarList.length; ++i )
			{
				if ( !familiarList[ i ].getItem().equals( EquipmentRequest.UNEQUIP ) )
				{
					AdventureResult.addResultToList(
						EquipmentManager.equipmentLists[ FAMILIAR ], familiarList[ i ].getItem() );
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
		int equipStat = EquipmentManager.getHitStatType();

		for ( int i = 0; i < KoLConstants.inventory.size(); ++i )
		{
			AdventureResult currentItem = (AdventureResult) KoLConstants.inventory.get( i );
			String currentItemName = currentItem.getName();

			int type = ItemDatabase.getConsumptionType( currentItem.getItemId() );

			// If we are equipping familiar items, make sure
			// current familiar can use this one

			if ( filterId == KoLConstants.EQUIP_FAMILIAR && type == KoLConstants.EQUIP_FAMILIAR )
			{
				temporary.add( currentItem );
				continue;
			}

			// If we want off-hand items and we can dual wield,
			// allow one-handed weapons of same type

			if ( filterId == KoLConstants.EQUIP_OFFHAND && type == KoLConstants.EQUIP_WEAPON && dual )
			{
				if ( EquipmentDatabase.getHands( currentItemName ) != 1 || EquipmentDatabase.getWeaponType( currentItemName ) != equipStat )
				{
					continue;
				}
			}

			// Otherwise, slot and item type must match

			else if ( filterId != type )
			{
				continue;
			}
			else if ( filterId == KoLConstants.EQUIP_WEAPON && dual )
			{
				if ( EquipmentDatabase.getHands( currentItemName ) == 1 && EquipmentDatabase.getWeaponType( currentItemName ) != equipStat )
				{
					continue;
				}
			}

			temporary.add( currentItem );
		}

		if ( currentList == EquipmentManager.accessories )
		{
			if ( !currentList.contains( getEquipment( ACCESSORY1 ) ) )
			{
				currentList.add( getEquipment( ACCESSORY1 ) );
			}
			if ( !currentList.contains( getEquipment( ACCESSORY2 ) ) )
			{
				currentList.add( getEquipment( ACCESSORY2 ) );
			}
			if ( !currentList.contains( getEquipment( ACCESSORY3 ) ) )
			{
				currentList.add( getEquipment( ACCESSORY3 ) );
			}
		}

		currentList.retainAll( temporary );
		temporary.removeAll( currentList );
		currentList.addAll( temporary );
	}

	private static final int getCount( final AdventureResult accessory )
	{
		int available = accessory.getCount( KoLConstants.inventory );
		if ( getEquipment( ACCESSORY1 ).equals( accessory ) )
		{
			++available;
		}
		if ( getEquipment( ACCESSORY2 ).equals( accessory ) )
		{
			++available;
		}
		if ( getEquipment( ACCESSORY3 ).equals( accessory ) )
		{
			++available;
		}

		return available;
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
		EquipmentManager.updateOutfits();
		for ( int i = 0; i <= FAMILIAR; ++i )
		{
			updateEquipmentList( i );
		}
	}

	public static final int equipmentTypeToConsumeFilter( final int equipmentType )
	{
		switch ( equipmentType )
		{
		case HAT:
			return KoLConstants.EQUIP_HAT;
		case WEAPON:
			return KoLConstants.EQUIP_WEAPON;
		case OFFHAND:
			return KoLConstants.EQUIP_OFFHAND;
		case SHIRT:
			return KoLConstants.EQUIP_SHIRT;
		case PANTS:
			return KoLConstants.EQUIP_PANTS;
		case ACCESSORY1:
		case ACCESSORY2:
		case ACCESSORY3:
			return KoLConstants.EQUIP_ACCESSORY;
		case FAMILIAR:
			return KoLConstants.EQUIP_FAMILIAR;
		default:
			return -1;
		}
	}

	public static final int consumeFilterToEquipmentType( final int consumeFilter )
	{
		switch ( consumeFilter )
		{
		case KoLConstants.EQUIP_HAT:
			return HAT;
		case KoLConstants.EQUIP_WEAPON:
			return WEAPON;
		case KoLConstants.EQUIP_OFFHAND:
			return OFFHAND;
		case KoLConstants.EQUIP_SHIRT:
			return SHIRT;
		case KoLConstants.EQUIP_PANTS:
			return PANTS;
		case KoLConstants.EQUIP_ACCESSORY:
			return ACCESSORY1;
		case KoLConstants.EQUIP_FAMILIAR:
			return FAMILIAR;
		default:
			return -1;
		}
	}

	/**
	 * Accessor method to retrieve # of hands character's weapon uses
	 *
	 * @return int number of hands needed
	 */

	public static final int getWeaponHandedness()
	{
		return EquipmentDatabase.getHands( EquipmentManager.getEquipment( WEAPON ).getName() );
	}

	/**
	 * Accessor method to determine if character is currently dual-wielding
	 *
	 * @return boolean true if character has two weapons equipped
	 */

	public static final boolean usingTwoWeapons()
	{
		return EquipmentDatabase.getHands( getEquipment( OFFHAND ).getName() ) == 1;
	}

	/**
	 * Accessor method to determine if character's weapon is a chefstaff
	 *
	 * @return boolean true if weapon is a chefstaff
	 */

	public static final boolean usingChefstaff()
	{
		return EquipmentDatabase.getItemType( EquipmentManager.getEquipment( WEAPON ).getItemId() ).equals( "chefstaff" );
	}

	/**
	 * Accessor method to determine if character's weapon is ranged
	 *
	 * @return boolean true if weapon is ranged
	 */

	public static final int getHitStatType()
	{
		return EquipmentDatabase.getWeaponType( getEquipment( WEAPON ).getName() );
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
		case KoLConstants.MOXIE:
			return KoLCharacter.getAdjustedMoxie();
		case KoLConstants.MYSTICALITY:
			return KoLCharacter.getAdjustedMysticality();
		default:
			return KoLCharacter.getAdjustedMuscle();
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
			if ( EquipmentDatabase.normalOutfits.get( i ) != null && EquipmentDatabase.normalOutfits.get( i ).hasAllPieces() )
			{
				available.add( EquipmentDatabase.normalOutfits.get( i ) );
			}
		}

		for ( int i = 0; i < EquipmentDatabase.weirdOutfits.size(); ++i )
		{
			if ( EquipmentDatabase.weirdOutfits.get( i ) != null && EquipmentDatabase.weirdOutfits.get( i ).hasAllPieces() )
			{
				available.add( EquipmentDatabase.weirdOutfits.get( i ) );
			}
		}

		Collections.sort( available );
		getOutfits().clear();

		// Start with the two constant outfits
		getOutfits().add( SpecialOutfit.NO_CHANGE );
		getOutfits().add( SpecialOutfit.BIRTHDAY_SUIT );

		// Finally any standard outfits
		getOutfits().addAll( available );

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
	 * Utility method which determines whether or not the equipment corresponding to the given outfit is already
	 * equipped.
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
				KoLmafiaCLI.DEFAULT_SHELL.executeConditionsCommand( "set " + pieces[ i ].getName() );
			}
		}
	}

	/**
	 * Utility method which determines the outfit ID the character is currently wearing
	 */

	public static final SpecialOutfit currentOutfit()
	{
		for ( int id = 1; id <= EquipmentDatabase.normalOutfits.size(); ++id )
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

		String requirement = EquipmentDatabase.getEquipRequirement( itemId );

		if ( requirement.startsWith( "Mus:" ) )
		{
			return KoLCharacter.getBaseMuscle() >= StringUtilities.parseInt( requirement.substring( 5 ) );
		}

		if ( requirement.startsWith( "Mys:" ) )
		{
			return KoLCharacter.getBaseMysticality() >= StringUtilities.parseInt( requirement.substring( 5 ) );
		}

		if ( requirement.startsWith( "Mox:" ) )
		{
			return KoLCharacter.getBaseMoxie() >= StringUtilities.parseInt( requirement.substring( 5 ) );
		}

		return true;
	}

}
