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

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.java.dev.spellcast.utilities.LockableListModel;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.FamiliarData;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.KoLmafiaCLI;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.SpecialOutfit;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.ConcoctionDatabase;
import net.sourceforge.kolmafia.persistence.EquipmentDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.session.EquipmentManager;
import net.sourceforge.kolmafia.session.InventoryManager;
import net.sourceforge.kolmafia.session.ResultProcessor;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class EquipmentRequest
	extends PasswordHashRequest
{
	private static final EquipmentRequest REFRESH1 = new EquipmentRequest( EquipmentRequest.CONSUMABLES );
	private static final EquipmentRequest REFRESH2 = new EquipmentRequest( EquipmentRequest.ALL_EQUIPMENT );
	private static final EquipmentRequest REFRESH3 = new EquipmentRequest( EquipmentRequest.MISCELLANEOUS );
	private static final EquipmentRequest REFRESH4 = new EquipmentRequest( EquipmentRequest.BEDAZZLEMENTS );

	private static final Pattern CELL_PATTERN = Pattern.compile( "<td>(.*?)</td>" );
	private static final Pattern MEAT_PATTERN = Pattern.compile( "[\\d,]+ meat\\.</b>" );
	private static final Pattern OUTSIDECLOSET_PATTERN = Pattern.compile( "<b>Put:.*?</select>", Pattern.DOTALL );
	private static final Pattern INSIDECLOSET_PATTERN = Pattern.compile( "<b>Take:.*?</select>", Pattern.DOTALL );
	private static final Pattern INVENTORYITEM_PATTERN =
		Pattern.compile( "<option value='?([\\d]+)'?[^>]*>([^>]*?) \\(([\\d,]+)\\)</option>" );
	private static final Pattern QUESTITEM_PATTERN = Pattern.compile( "<b>(<a[^>]*?>)?(elven <i>limbos</i> gingerbread|[^<]+)(</a>)?</b>([^<]*?)<font size=1>" );
	private static final Pattern HAT_PATTERN =
		Pattern.compile( "Hat</a>:</td>.*?<b>(.*?)</b>.*unequip&type=hat" );
	private static final Pattern WEAPON_PATTERN =
		Pattern.compile( "Weapon</a>:</td>.*?<b>(.*?)</b>.*unequip&type=weapon" );
	private static final Pattern OFFHAND_PATTERN =
		Pattern.compile( "Off-Hand</a>:</td>.*?<b>([^<]*)</b> *(<font.*?/font>)?[^>]*unequip&type=offhand" );
	private static final Pattern SHIRT_PATTERN =
		Pattern.compile( "Shirt</a>:</td>.*?<b>(.*?)</b>.*unequip&type=shirt" );
	private static final Pattern PANTS_PATTERN =
		Pattern.compile( "Pants</a>:</td>.*?<b>(.*?)</b>.*unequip&type=pants" );
	private static final Pattern ACC1_PATTERN =
		Pattern.compile( "<b>([^<]*?)</b>\\s*<[^<]+unequip&type=acc1\"" );
	private static final Pattern ACC2_PATTERN =
		Pattern.compile( "<b>([^<]*?)</b>\\s*<[^<]+unequip&type=acc2\"" );
	private static final Pattern ACC3_PATTERN =
		Pattern.compile( "<b>([^<]*?)</b>\\s*<[^<]+unequip&type=acc3\"" );
	private static final Pattern FAMILIARITEM_PATTERN =
		Pattern.compile( "<b>([^<]*?)</b>\\s*<[^<]+unequip&type=familiarequip\"" );
	private static final Pattern OUTFITLIST_PATTERN = Pattern.compile( "<select name=whichoutfit>.*?</select>" );
	private static final Pattern STICKER_PATTERN = Pattern.compile(
		"<td>\\s*(shiny|dull)?\\s*([^<]+)<a [^>]+action=peel|<td>\\s*<img [^>]+magnify" );

	private static final Pattern OUTFIT_ACTION_PATTERN = Pattern.compile(
		"([a-zA-Z])=([^=]+)(?!=)" );

	private static final Pattern OUTFIT_PATTERN = Pattern.compile( "whichoutfit=(-?\\d+)" );
	private static final Pattern SLOT_PATTERN = Pattern.compile( "type=([a-z123]+)" );
	private static final Pattern ITEMID_PATTERN = Pattern.compile( "whichitem=(\\d+)" );
	private static final Pattern STICKERITEM_PATTERN = Pattern.compile( "sticker=(\\d+)" );
	private static final Pattern SLOT1_PATTERN = Pattern.compile( "slot=(\\d+)" );

	private static final Pattern EQUIPPED_PATTERN = Pattern.compile( "<td[^>]*>Item equipped:</td><td>.*?<b>(.*?)</b></td>" );
	private static final Pattern UNEQUIPPED_PATTERN = Pattern.compile( "<td[^>]*>Item unequipped:</td><td>.*?<b>(.*?)</b></td>" );

	public static final AdventureResult UNEQUIP = new AdventureResult( "(none)", 1, false );
	private static final AdventureResult SPECTACLES = ItemPool.get( ItemPool.SPOOKYRAVEN_SPECTACLES, 1 );

	private static final int FAKE_HAND = 1511;

	public static final int CLOSET = 1;
	public static final int CONSUMABLES = 2;
	public static final int EQUIPMENT = 3;	// loads current equipment only
	public static final int MISCELLANEOUS = 4;

	public static final int SAVE_OUTFIT = 5;
	public static final int CHANGE_OUTFIT = 6;

	public static final int CHANGE_ITEM = 7;
	public static final int REMOVE_ITEM = 8;
	public static final int UNEQUIP_ALL = 9;

	public static final int BEDAZZLEMENTS = 10;
	public static final int ALL_EQUIPMENT = 11;	// loads entire equipment page

	// Array indexed by equipment "slot" from KoLCharacter
	//
	// Perhaps this should be in that module, except this is closely tied
	// to the PHP files that are manipulated by THIS module.

	// These are the public names
	public static final String[] slotNames =
	{
		"hat",
		"weapon",
		"off-hand",
		"shirt",
		"pants",
		"acc1",
		"acc2",
		"acc3",
		"familiar",
		"sticker1",
		"sticker2",
		"sticker3",
		"fakehand"
	};

	// These are the names used in the PHP file
	public static final String[] phpSlotNames =
	{
		"hat",
		"weapon",
		"offhand",
		"shirt",
		"pants",
		"acc1",
		"acc2",
		"acc3",
		"familiarequip",
		"st1",
		"st2",
		"st3",
		"fakehand"
	};

	private int requestType;
	private int equipmentSlot;
	private AdventureResult changeItem;
	private int itemId;
	private int equipmentType;
	private SpecialOutfit outfit;
	private String outfitName;
	private String error;

	private static boolean shouldSavePreviousOutfit = false;
	private static int customOutfitId = 0;

	public EquipmentRequest( final int requestType )
	{
		super(
			requestType == EquipmentRequest.BEDAZZLEMENTS ? "bedazzle.php" :
			requestType == EquipmentRequest.CLOSET ? "closet.php" :
			requestType == EquipmentRequest.UNEQUIP_ALL ? "inv_equip.php" : 
			"inventory.php" );

		this.requestType = requestType;
		this.outfit = null;
		this.outfitName = null;
		this.error = null;

		// Otherwise, add the form field indicating which page
		// of the inventory you want to request

		if ( requestType == EquipmentRequest.BEDAZZLEMENTS )
		{
			// no fields necessary
		}
		else if ( requestType == EquipmentRequest.EQUIPMENT )
		{
			this.addFormField( "ajax", "1" );
			this.addFormField( "curequip", "1" );
		}
		else if ( requestType == EquipmentRequest.MISCELLANEOUS )
		{
			this.addFormField( "which", "3" );
		}
		else if ( requestType == EquipmentRequest.CONSUMABLES )
		{
			this.addFormField( "which", "1" );
		}
		else if ( requestType != EquipmentRequest.CLOSET )
		{
			this.addFormField( "which", "2" );
		}

		if ( requestType == EquipmentRequest.UNEQUIP_ALL )
		{
			this.addFormField( "action", "unequipall" );
		}
	}

	public EquipmentRequest( final String changeName )
	{
		super( "inv_equip.php" );

		this.addFormField( "which", "2" );
		this.addFormField( "action", "customoutfit" );
		this.addFormField( "outfitname", changeName );
		this.requestType = EquipmentRequest.SAVE_OUTFIT;
		this.outfitName = changeName;
		this.error = null;
	}

	public EquipmentRequest( final AdventureResult changeItem )
	{
		this(
			changeItem,
			EquipmentRequest.chooseEquipmentSlot( ItemDatabase.getConsumptionType( changeItem.getItemId() ) ),
			false );
	}

	public EquipmentRequest( final AdventureResult changeItem, final int equipmentSlot )
	{
		this( changeItem, equipmentSlot, false );
	}

	public EquipmentRequest( final AdventureResult changeItem, final int equipmentSlot, final boolean force )
	{
		super( equipmentSlot >= EquipmentManager.STICKER1 ?
			"bedazzle.php" : "inv_equip.php" );
		this.error = null;
		if ( equipmentSlot >= EquipmentManager.STICKER1 )
		{
			this.initializeStickerData( changeItem, equipmentSlot, force );
		}
		else
		{
			this.initializeChangeData( changeItem, equipmentSlot, force );
		}
	}

	protected boolean shouldFollowRedirect()
	{
		return true;
	}

	protected boolean retryOnTimeout()
	{
		return true;
	}

	public static final void savePreviousOutfit()
	{
		EquipmentRequest.shouldSavePreviousOutfit = true;
	}

	private void initializeChangeData( final AdventureResult changeItem, final int equipmentSlot, final boolean force )
	{
		this.addFormField( "which", "2" );
		this.equipmentSlot = equipmentSlot;

		if ( changeItem.equals( EquipmentRequest.UNEQUIP ) )
		{
			this.requestType = EquipmentRequest.REMOVE_ITEM;
			this.addFormField( "action", "unequip" );
			this.addFormField( "type", EquipmentRequest.phpSlotNames[ equipmentSlot ] );
			return;
		}

		// Find out what item is being equipped
		this.itemId = changeItem.getItemId();

		// Find out what kind of item it is
		this.equipmentType = ItemDatabase.getConsumptionType( this.itemId );

		if ( this.equipmentSlot == -1 )
		{
			this.error = "No suitable slot available for " + changeItem;
			return;
		}

		// Make sure you can equip it in the requested slot
		String action = this.getAction( force );
		if ( action == null )
		{
			return;
		}

		this.requestType = EquipmentRequest.CHANGE_ITEM;
		this.changeItem = changeItem.getCount() == 1 ? changeItem : changeItem.getInstance( 1 );

		this.addFormField( "action", action );
		this.addFormField( "whichitem", String.valueOf( this.itemId ) );
	}

	private void initializeStickerData( final AdventureResult sticker, final int equipmentSlot, final boolean force )
	{
		this.equipmentSlot = equipmentSlot;
		this.addFormField( "slot",
			String.valueOf( equipmentSlot - EquipmentManager.STICKER1 + 1 ) );

		if ( sticker.equals( EquipmentRequest.UNEQUIP ) )
		{
			this.requestType = EquipmentRequest.REMOVE_ITEM;
			this.addFormField( "action", "peel" );
			return;
		}

		// Find out what item is being equipped
		this.itemId = sticker.getItemId();

		// Find out what kind of item it is
		this.equipmentType = ItemDatabase.getConsumptionType( this.itemId );
		
		if ( this.equipmentType != KoLConstants.CONSUME_STICKER )
		{
			this.error = "You can't equip a " + ItemDatabase.getItemName( this.itemId ) + 
				" in a sticker slot.";
			return;
		}

		this.addFormField( "sticker", String.valueOf( this.itemId ) );
		this.requestType = EquipmentRequest.CHANGE_ITEM;
		this.changeItem = sticker.getCount() == 1 ? sticker : sticker.getInstance( 1 );

		if ( EquipmentManager.hasStickerWeapon() )
		{
			this.addFormField( "action", "stick" );
		}
		else
		{
			this.addFormField( "action", "juststick" );
			this.removeFormField( "slot" );
		}
	}

	public EquipmentRequest( final SpecialOutfit change )
	{
		super( "inv_equip.php" );

		this.addFormField( "action", "outfit" );
		this.addFormField( "which", "2" );
		this.addFormField( "whichoutfit", String.valueOf( change.getOutfitId() ) );

		this.requestType = EquipmentRequest.CHANGE_OUTFIT;
		this.outfit = change;
		this.error = null;
	}

	private String getAction( final boolean force )
	{
		switch ( this.equipmentSlot )
		{
		case EquipmentManager.HAT:
			if ( this.equipmentType == KoLConstants.EQUIP_HAT )
			{
				return "equip";
			}
			break;

		case EquipmentManager.WEAPON:
			if ( this.equipmentType == KoLConstants.EQUIP_WEAPON )
			{
				return "equip";
			}
			break;

		case EquipmentManager.OFFHAND:
			if ( this.equipmentType == KoLConstants.EQUIP_OFFHAND )
			{
				return "equip";
			}

			if ( this.equipmentType == KoLConstants.EQUIP_WEAPON &&
			     EquipmentDatabase.getHands( this.itemId ) == 1 )
			{
				return "dualwield";
			}
			break;

		case EquipmentManager.SHIRT:
			if ( this.equipmentType == KoLConstants.EQUIP_SHIRT )
			{
				return "equip";
			}
			break;

		case EquipmentManager.PANTS:
			if ( this.equipmentType == KoLConstants.EQUIP_PANTS )
			{
				return "equip";
			}
			break;

		case EquipmentManager.ACCESSORY1:
			if ( this.equipmentType == KoLConstants.EQUIP_ACCESSORY )
			{
				this.addFormField( "slot", "1" );
				return "equip";
			}
			break;

		case EquipmentManager.ACCESSORY2:
			if ( this.equipmentType == KoLConstants.EQUIP_ACCESSORY )
			{
				this.addFormField( "slot", "2" );
				return "equip";
			}
			break;

		case EquipmentManager.ACCESSORY3:
			if ( this.equipmentType == KoLConstants.EQUIP_ACCESSORY )
			{
				this.addFormField( "slot", "3" );
				return "equip";
			}
			break;

		case EquipmentManager.FAMILIAR:
			switch ( this.equipmentType )
			{
			case KoLConstants.EQUIP_FAMILIAR:
				return "equip";

			case KoLConstants.EQUIP_HAT:
			case KoLConstants.EQUIP_WEAPON:
				return "hatrack";
			}
			break;

		default:
			return "equip";
		}

		this.error =
			"You can't equip a " + ItemDatabase.getItemName( this.itemId ) + " in the " + EquipmentRequest.slotNames[ this.equipmentSlot ] + " slot.";

		return null;
	}

	public static final int chooseEquipmentSlot( final int equipmentType )
	{
		switch ( equipmentType )
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

		case KoLConstants.EQUIP_FAMILIAR:
			return EquipmentManager.FAMILIAR;

		case KoLConstants.EQUIP_ACCESSORY:
		{
			AdventureResult test = EquipmentManager.getEquipment( EquipmentManager.ACCESSORY1 );
			if ( test == null || test.equals( EquipmentRequest.UNEQUIP ) )
			{
				return EquipmentManager.ACCESSORY1;
			}

			test = EquipmentManager.getEquipment( EquipmentManager.ACCESSORY2 );
			if ( test == null || test.equals( EquipmentRequest.UNEQUIP ) )
			{
				return EquipmentManager.ACCESSORY2;
			}

			test = EquipmentManager.getEquipment( EquipmentManager.ACCESSORY3 );
			if ( test == null || test.equals( EquipmentRequest.UNEQUIP ) )
			{
				return EquipmentManager.ACCESSORY3;
			}

			// All accessory slots are in use. Pick #1
			return EquipmentManager.ACCESSORY1;
		}

		case KoLConstants.CONSUME_STICKER:
		{
			AdventureResult test = EquipmentManager.getEquipment( EquipmentManager.STICKER1 );
			if ( test == null || test.equals( EquipmentRequest.UNEQUIP ) )
			{
				return EquipmentManager.STICKER1;
			}

			test = EquipmentManager.getEquipment( EquipmentManager.STICKER2 );
			if ( test == null || test.equals( EquipmentRequest.UNEQUIP ) )
			{
				return EquipmentManager.STICKER2;
			}

			test = EquipmentManager.getEquipment( EquipmentManager.STICKER3 );
			if ( test == null || test.equals( EquipmentRequest.UNEQUIP ) )
			{
				return EquipmentManager.STICKER3;
			}
			
			// All sticker slots are in use.  Abort rather than risk peeling the wrong one.
			return -1;
		}

		default:
			return -1;
		}
	}

	public String getOutfitName()
	{
		return this.outfit == null ? null : this.outfit.toString();
	}

	/**
	 * Executes the <code>EquipmentRequest</code>. Note that at the current time, only the character's currently
	 * equipped items and familiar item will be stored.
	 */

	public void run()
	{
		// If we were given bogus parameters, report the error now
		if ( this.error != null )
		{
			KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, this.error );
			return;
		}

		// Outfit changes are a bit quirky, so they're handled
		// first for easy visibility.

		if ( this.requestType == EquipmentRequest.CHANGE_OUTFIT )
		{
			// If this is a birthday suit outfit, then remove everything.
			if ( this.outfit == SpecialOutfit.BIRTHDAY_SUIT )
			{
				if ( EquipmentRequest.shouldSavePreviousOutfit )
				{
					if ( SpecialOutfit.markImplicitCheckpoint() )
					{
						( new EquipmentRequest( "Backup" ) ).run();
					}

					EquipmentRequest.shouldSavePreviousOutfit = false;
				}

				// Only make a request to unequip everything if
				// you are wearing something.

				for ( int i = 0; i < EquipmentManager.FAMILIAR; ++i )
				{
					if ( !EquipmentManager.getEquipment( i ).equals( EquipmentRequest.UNEQUIP ) )
					{
						( new EquipmentRequest( EquipmentRequest.UNEQUIP_ALL ) ).run();
						break;
					}
				}

				return;
			}

			int id = this.outfit.getOutfitId();

			// If this is not a custom outfit...
			if ( id > 0 )
			{
				// Return immediately if the character is
				// already wearing the outfit
				if ( EquipmentManager.isWearingOutfit( id ) )
				{
					return;
				}

				// Next, ensure that you have all the pieces
				// for the given outfit.

				EquipmentManager.retrieveOutfit( id );

				// Bail now if the conditions were not met
				if ( !KoLmafia.permitsContinue() )
				{
					return;
				}

				if ( EquipmentRequest.shouldSavePreviousOutfit )
				{
					if ( SpecialOutfit.markImplicitCheckpoint() )
					{
						( new EquipmentRequest( "Backup" ) ).run();
					}

					EquipmentRequest.shouldSavePreviousOutfit = false;
				}
			}
			else if ( id == 0 )
			{
				// Return immediately if the character is
				// already wearing the outfit
				if ( EquipmentManager.isWearingOutfit( this.outfit ) )
				{
					return;
				}

				AdventureResult[] pieces = this.outfit.getPieces();

				int equipmentType;

				boolean usesWeapon = false;
				boolean usesAccessories = false;

				for ( int i = 0; i < pieces.length; ++i )
				{
					equipmentType = ItemDatabase.getConsumptionType( pieces[ i ].getItemId() );

					// If the item is an accessory, you
					// will have to remove all other
					// accessories to be consistent.

					if ( equipmentType == KoLConstants.EQUIP_ACCESSORY )
					{
						if ( !usesAccessories )
						{
							for ( int j = EquipmentManager.ACCESSORY1; j <= EquipmentManager.ACCESSORY3; ++j )
							{
								( new EquipmentRequest( EquipmentRequest.UNEQUIP, j ) ).run();
							}
						}

						usesAccessories = true;
					}

					int desiredSlot = EquipmentRequest.chooseEquipmentSlot( equipmentType );

					// If it's a weapon, sometimes it needs to go to the offhand
					// slot when there's already a weapon equipped from the outfit.

					if ( equipmentType == KoLConstants.EQUIP_WEAPON )
					{
						if ( usesWeapon )
						{
							desiredSlot = EquipmentManager.OFFHAND;
						}

						usesWeapon = true;
					}

					// Now, execute the equipment change.  It will auto-detect
					// if you already have the item equipped.

					( new EquipmentRequest( pieces[ i ], desiredSlot ) ).run();
				}

				return;
			}
		}

		if ( this.requestType == EquipmentRequest.CHANGE_ITEM )
		{
			// Do not submit a request if the item matches what you
			// want to equip on the character.

			if ( EquipmentManager.getEquipment( this.equipmentSlot ).equals( this.changeItem ) )
			{
				return;
			}

			// If we are equipping a new weapon, a two-handed
			// weapon will unequip any pair of weapons. But a
			// one-handed weapon much match the type of the
			// off-hand weapon. If it doesn't, unequip the off-hand
			// weapon first

			int itemId = this.changeItem.getItemId();

			if ( this.equipmentSlot == EquipmentManager.WEAPON &&
			     EquipmentDatabase.getHands( itemId ) == 1 )
			{
				int offhand = EquipmentManager.getEquipment( EquipmentManager.OFFHAND ).getItemId();

				if ( ItemDatabase.getConsumptionType( offhand ) == KoLConstants.EQUIP_WEAPON &&
				     EquipmentDatabase.getWeaponType( itemId ) != EquipmentDatabase.getWeaponType( offhand ) )
				{
					( new EquipmentRequest( EquipmentRequest.UNEQUIP, EquipmentManager.OFFHAND ) ).run();
				}
			}

			// If you are equipping an off-hand weapon, don't
			// bother trying if unless it is compatible with the
			// main weapon.

			if ( this.equipmentSlot == EquipmentManager.OFFHAND )
			{
				int itemType = ItemDatabase.getConsumptionType( itemId );
				AdventureResult weapon = EquipmentManager.getEquipment( EquipmentManager.WEAPON );
				int weaponItemId = weapon.getItemId();

				if ( itemType == KoLConstants.EQUIP_WEAPON && weaponItemId <= 0 )
				{
					KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "You can't dual wield unless you already have a main weapon." );
					return;
				}

				if ( EquipmentDatabase.getHands( weaponItemId ) > 1 )
				{
					String message = itemType == KoLConstants.EQUIP_WEAPON ?
						"You can't dual wield while wielding a 2-handed weapon." :
						"You can't equip an off-hand item while wielding a 2-handed weapon.";
					KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, message );
					return;
				}

				if ( itemType == KoLConstants.EQUIP_WEAPON &&
				     EquipmentDatabase.getWeaponType( itemId ) != EquipmentDatabase.getWeaponType( weaponItemId ) )
				{
					KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "You can't hold a " + this.changeItem.getName() + " in your off-hand when wielding a " + weapon.getName() );
					return;
				}
			}

			if ( !InventoryManager.retrieveItem( this.changeItem ) )
			{
				return;
			}
			
			if ( this.equipmentSlot >= EquipmentManager.STICKER1 &&
			     this.equipmentSlot <= EquipmentManager.STICKER3 &&
			     !EquipmentManager.getEquipment( this.equipmentSlot ).equals( EquipmentRequest.UNEQUIP ) )
			{
				( new EquipmentRequest( EquipmentRequest.UNEQUIP, this.equipmentSlot ) ).run();	
			}
		}

		if ( this.requestType == EquipmentRequest.REMOVE_ITEM &&
		     EquipmentManager.getEquipment( this.equipmentSlot ).equals( EquipmentRequest.UNEQUIP ) )
		{
			return;
		}

		switch ( this.requestType )
		{
		case EquipmentRequest.CONSUMABLES:
			KoLmafia.updateDisplay( "Updating consumable items..." );
			break;

		case EquipmentRequest.EQUIPMENT:
			KoLmafia.updateDisplay( "Retrieving equipment..." );
			break;

		case EquipmentRequest.MISCELLANEOUS:
			KoLmafia.updateDisplay( "Updating miscellaneous items..." );
			break;

		case EquipmentRequest.CLOSET:
			KoLmafia.updateDisplay( "Refreshing closet..." );
			break;

		case EquipmentRequest.BEDAZZLEMENTS:
			KoLmafia.updateDisplay( "Refreshing stickers..." );
			break;

		case EquipmentRequest.SAVE_OUTFIT:
			KoLmafia.updateDisplay( "Saving outfit " + this.outfitName + "..." );
			break;

		case EquipmentRequest.CHANGE_OUTFIT:
			KoLmafia.updateDisplay( "Putting on " + this.outfit + "..." );
			break;

		case EquipmentRequest.CHANGE_ITEM:
			KoLmafia.updateDisplay( ( this.equipmentSlot == EquipmentManager.WEAPON ? "Wielding " : this.equipmentSlot == EquipmentManager.OFFHAND ? "Holding " : "Putting on " ) + ItemDatabase.getItemName( this.itemId ) + "..." );
			break;

		case EquipmentRequest.REMOVE_ITEM:
			KoLmafia.updateDisplay( "Taking off " + EquipmentManager.getEquipment( this.equipmentSlot ).getName() + "..." );
			break;

		case EquipmentRequest.UNEQUIP_ALL:
			KoLmafia.updateDisplay( "Taking off everything..." );
			break;
		}

		super.run();

		switch ( this.requestType )
		{
		case EquipmentRequest.SAVE_OUTFIT:
			KoLmafia.updateDisplay( "Outfit saved" );
			break;

		case EquipmentRequest.CHANGE_ITEM:
		case EquipmentRequest.CHANGE_OUTFIT:

			Matcher resultMatcher = EquipmentRequest.CELL_PATTERN.matcher( this.responseText );
			if ( resultMatcher.find() )
			{
				String result = resultMatcher.group( 1 ).replaceAll( "</?b>", "" );
				if ( result.indexOf( "You are already wearing" ) != -1 )
				{
					// Not an error
					KoLmafia.updateDisplay( result );
					return;
				}

				if ( result.indexOf( "You put" ) == -1 && result.indexOf( "You equip" ) == -1 && result.indexOf( "Item equipped" ) == -1 && result.indexOf( "equips an item" ) == -1  && result.indexOf( "You apply the shiny sticker" ) == -1 && result.indexOf( "fold it into an impromptu sword" ) == -1 )
				{
					KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, result );
					return;
				}
			}

			KoLmafia.updateDisplay( "Equipment changed." );
			if ( !this.containsUpdate )
			{
				CharPaneRequest.getInstance().run();
			}

			break;

		case EquipmentRequest.REMOVE_ITEM:

			KoLmafia.updateDisplay( "Equipment changed." );
			if ( !this.containsUpdate )
			{
				CharPaneRequest.getInstance().run();
			}

			break;

		case EquipmentRequest.UNEQUIP_ALL:

			KoLmafia.updateDisplay( "Everything removed." );
			if ( !this.containsUpdate )
			{
				CharPaneRequest.getInstance().run();
			}

			break;
		}
	}

	public void processResults()
	{
		super.processResults();

		// Fetch updated equipment
		if ( this.requestType == EquipmentRequest.CLOSET )
		{
			KoLmafia.setIsRefreshing( true );

			InventoryManager.resetInventory();
			EquipmentManager.resetEquipment();

			this.parseCloset();

			EquipmentRequest.REFRESH1.run();
			EquipmentRequest.REFRESH2.run();
			EquipmentRequest.REFRESH3.run();
			EquipmentRequest.REFRESH4.run();

			KoLmafia.setIsRefreshing( false );

			return;
		}

		if ( this.getURLString().startsWith( "bedazzle.php" ) )
		{
			EquipmentRequest.parseBedazzlements( this.responseText );
			return;
		}

		if ( this.requestType != EquipmentRequest.MISCELLANEOUS && this.requestType != EquipmentRequest.CONSUMABLES )
		{
			EquipmentRequest.parseEquipment( this.getURLString(), this.responseText );
		}

		int outfitDivider = this.responseText.indexOf( "Save as Custom Outfit" );

		if ( outfitDivider != -1 )
		{
			EquipmentRequest.parseQuestItems( this.responseText.substring( outfitDivider ) );
		}
		else
		{
			EquipmentRequest.parseQuestItems( this.responseText );
		}
	}

	private static final boolean switchItem( final AdventureResult oldItem, final AdventureResult newItem )
	{
		// If the items are not equivalent, make sure
		// the items should get switched out.

		if ( newItem.getItemId() == oldItem.getItemId() )
		{
			return false;
		}

		// Manually subtract item from inventory to avoid
		// excessive list updating.

		if ( newItem != EquipmentRequest.UNEQUIP )
		{
			AdventureResult.addResultToList( KoLConstants.inventory, newItem.getInstance( -1 ) );
		}

		if ( oldItem != EquipmentRequest.UNEQUIP )
		{
			AdventureResult.addResultToList( KoLConstants.inventory, oldItem.getInstance( 1 ) );
		}

		return !ConcoctionDatabase.getKnownUses( oldItem ).isEmpty() || !ConcoctionDatabase.getKnownUses( newItem ).isEmpty();
	}

	private void parseCloset()
	{
		// Try to find how much meat is in your character's closet -
		// this way, the program's meat manager frame auto-updates

		Matcher meatInClosetMatcher = EquipmentRequest.MEAT_PATTERN.matcher( this.responseText );

		if ( meatInClosetMatcher.find() )
		{
			String meatInCloset = meatInClosetMatcher.group();
			KoLCharacter.setClosetMeat( StringUtilities.parseInt( meatInCloset ) );
		}

		Matcher inventoryMatcher = EquipmentRequest.OUTSIDECLOSET_PATTERN.matcher( this.responseText );
		if ( inventoryMatcher.find() )
		{
			this.parseCloset( inventoryMatcher.group(), KoLConstants.inventory );
		}

		Matcher closetMatcher = EquipmentRequest.INSIDECLOSET_PATTERN.matcher( this.responseText );
		if ( closetMatcher.find() )
		{
			this.parseCloset( closetMatcher.group(), KoLConstants.closet );
		}
	}

	private void parseCloset( final String content, final List resultList )
	{
		int lastFindIndex = 0;
		Matcher optionMatcher = EquipmentRequest.INVENTORYITEM_PATTERN.matcher( content );

		resultList.clear();

		while ( optionMatcher.find( lastFindIndex ) )
		{
			lastFindIndex = optionMatcher.end();
			int itemId = StringUtilities.parseInt( optionMatcher.group( 1 ) );
			String itemName = StringUtilities.getCanonicalName( ItemDatabase.getItemName( itemId ) );
			String realName = StringUtilities.getCanonicalName( optionMatcher.group( 2 ).toLowerCase() );

			if ( itemName == null || !realName.equals( itemName ) )
			{
				ItemDatabase.registerItem( itemId, realName );
			}

			AdventureResult result = new AdventureResult( itemId, StringUtilities.parseInt( optionMatcher.group( 3 ) ) );
			if ( resultList == KoLConstants.inventory )
			{
				ResultProcessor.tallyResult( result, false );
			}
			else
			{
				AdventureResult.addResultToList( resultList, result );
			}
		}

		if ( resultList == KoLConstants.inventory )
		{
			EquipmentManager.updateEquipmentLists();
			ConcoctionDatabase.refreshConcoctions();
			ItemDatabase.calculateAdventureRanges();
		}
	}

	private static final void parseQuestItems( final String text )
	{
		Matcher itemMatcher = EquipmentRequest.QUESTITEM_PATTERN.matcher( text );
		while ( itemMatcher.find() )
		{
			String quantity = itemMatcher.group( 4 ).trim();
			String realName = itemMatcher.group( 2 ).trim();

			// We have encountered a brand new item, the person
			// has no meat paste, and we're in trouble.  Do not
			// continue if this is the case.

			if ( !ItemDatabase.contains( realName ) )
			{
				continue;
			}

			// The inventory never has the plural name.
			int itemId = ItemDatabase.getItemId( realName, 1, false );
			int quantityValue =
				quantity.length() == 0 ? 1 : StringUtilities.parseInt( quantity.substring( 1, quantity.length() - 1 ) );
			AdventureResult item = new AdventureResult( itemId, quantityValue );
			int inventoryCount = item.getCount( KoLConstants.inventory );

			// Add the difference between your existing count
			// and the original count.

			if ( inventoryCount != quantityValue )
			{
				item = item.getInstance( quantityValue - inventoryCount );
				ResultProcessor.tallyResult( item, true );
			}
		}
	}
	
	public static final void parseBedazzlements( final String responseText )
	{
		Matcher matcher = EquipmentRequest.STICKER_PATTERN.matcher( responseText );
		for ( int slot = EquipmentManager.STICKER1; slot <= EquipmentManager.STICKER3; ++slot )
		{
			if ( !matcher.find() )
			{
				return;	// presumably doesn't have a sticker weapon
			}
			AdventureResult newItem;
			if ( matcher.group( 2 ) == null )
			{
				newItem = EquipmentRequest.UNEQUIP;
			}
			else
			{
				newItem = ItemPool.get( matcher.group( 2 ), 1 );
			}
			AdventureResult oldItem = EquipmentManager.getEquipment( slot );
			EquipmentManager.setEquipment( slot, newItem );
			if ( !KoLmafia.isRefreshing() &&
				!newItem.equals( oldItem ) )
			{
				if ( !oldItem.equals( EquipmentRequest.UNEQUIP ) &&
					!KoLConstants.inventory.contains( oldItem ) )
				{
					// Item was in the list for this slot only so that it could be
					// displayed as the current item.  Remove it.
					EquipmentManager.getEquipmentLists()[ slot ].remove( oldItem );					
				}
				if ( !newItem.equals( EquipmentRequest.UNEQUIP ) )
				{
					ResultProcessor.processResult( newItem.getInstance( -1 ) );
				}
				EquipmentManager.setTurns( slot, 20, 20 );
			}
			
			if ( matcher.group( 1 ) != null )
			{
				String adjective = matcher.group( 1 );
				if ( adjective.equals( "shiny" ) )
				{
					EquipmentManager.setTurns( slot, 16, 20 );
				}
				else if ( adjective.equals( "dull" ) )
				{
					EquipmentManager.setTurns( slot, 1, 5 );
				}
				else
				{
					EquipmentManager.setTurns( slot, 6, 15 );
				}
			}
		}
	}

	public static final void parseEquipment( final String location, final String responseText )
	{
		if ( location.indexOf( "onlyitem=" ) != -1 )
		{
			return;
		}

		AdventureResult[] oldEquipment = EquipmentManager.currentEquipment();
		int oldFakeHands = EquipmentManager.getFakeHands();

		// Ensure that the inventory stays up-to-date by switching
		// items around, as needed.

		AdventureResult[] equipment = EquipmentManager.emptyEquipmentArray();
		int fakeHands = 0;

		String name;
		Matcher equipmentMatcher;

		if ( responseText.indexOf( "unequip&type=hat" ) != -1 )
		{
			equipmentMatcher = EquipmentRequest.HAT_PATTERN.matcher( responseText );
			if ( equipmentMatcher.find() )
			{
				name = equipmentMatcher.group( 1 ).trim();
				if ( EquipmentDatabase.contains( name ) )
				{
					equipment[ EquipmentManager.HAT ] = new AdventureResult( name, 1, false );
				}

				RequestLogger.updateDebugLog( "Hat: " + equipment[ EquipmentManager.HAT ] );
			}
		}

		if ( responseText.indexOf( "unequip&type=weapon" ) != -1 )
		{
			equipmentMatcher = EquipmentRequest.WEAPON_PATTERN.matcher( responseText );
			if ( equipmentMatcher.find() )
			{
				name = equipmentMatcher.group( 1 ).trim();
				if ( EquipmentDatabase.contains( name ) )
				{
					equipment[ EquipmentManager.WEAPON ] =
						new AdventureResult( equipmentMatcher.group( 1 ).trim(), 1, false );
				}

				RequestLogger.updateDebugLog( "Weapon: " + equipment[ EquipmentManager.WEAPON ] );
			}
		}

		if ( responseText.indexOf( "unequip&type=offhand" ) != -1 )
		{
			equipmentMatcher = EquipmentRequest.OFFHAND_PATTERN.matcher( responseText );
			if ( equipmentMatcher.find() )
			{
				name = equipmentMatcher.group( 1 ).trim();
				if ( EquipmentDatabase.contains( name ) )
				{
					equipment[ EquipmentManager.OFFHAND ] =
						new AdventureResult( equipmentMatcher.group( 1 ).trim(), 1, false );
				}

				RequestLogger.updateDebugLog( "Off-hand: " + equipment[ EquipmentManager.OFFHAND ] );
			}
		}

		if ( responseText.indexOf( "unequip&type=shirt" ) != -1 )
		{
			equipmentMatcher = EquipmentRequest.SHIRT_PATTERN.matcher( responseText );
			if ( equipmentMatcher.find() )
			{
				name = equipmentMatcher.group( 1 ).trim();
				if ( EquipmentDatabase.contains( name ) )
				{
					equipment[ EquipmentManager.SHIRT ] =
						new AdventureResult( equipmentMatcher.group( 1 ).trim(), 1, false );
				}

				RequestLogger.updateDebugLog( "Shirt: " + equipment[ EquipmentManager.SHIRT ] );
			}
		}

		if ( responseText.indexOf( "unequip&type=pants" ) != -1 )
		{
			equipmentMatcher = EquipmentRequest.PANTS_PATTERN.matcher( responseText );
			if ( equipmentMatcher.find() )
			{
				name = equipmentMatcher.group( 1 ).trim();
				if ( EquipmentDatabase.contains( name ) )
				{
					equipment[ EquipmentManager.PANTS ] =
						new AdventureResult( equipmentMatcher.group( 1 ).trim(), 1, false );
				}

				RequestLogger.updateDebugLog( "Pants: " + equipment[ EquipmentManager.PANTS ] );
			}
		}

		if ( responseText.indexOf( "unequip&type=acc1" ) != -1 )
		{
			equipmentMatcher = EquipmentRequest.ACC1_PATTERN.matcher( responseText );
			if ( equipmentMatcher.find() )
			{
				name = equipmentMatcher.group( 1 ).trim();
				if ( EquipmentDatabase.contains( name ) )
				{
					equipment[ EquipmentManager.ACCESSORY1 ] =
						new AdventureResult( equipmentMatcher.group( 1 ).trim(), 1, false );
				}

				RequestLogger.updateDebugLog( "Accessory 1: " + equipment[ EquipmentManager.ACCESSORY1 ] );
			}
		}

		if ( responseText.indexOf( "unequip&type=acc2" ) != -1 )
		{
			equipmentMatcher = EquipmentRequest.ACC2_PATTERN.matcher( responseText );
			if ( equipmentMatcher.find() )
			{
				name = equipmentMatcher.group( 1 ).trim();
				if ( EquipmentDatabase.contains( name ) )
				{
					equipment[ EquipmentManager.ACCESSORY2 ] =
						new AdventureResult( equipmentMatcher.group( 1 ).trim(), 1, false );
				}

				RequestLogger.updateDebugLog( "Accessory 2: " + equipment[ EquipmentManager.ACCESSORY2 ] );
			}
		}

		if ( responseText.indexOf( "unequip&type=acc3" ) != -1 )
		{
			equipmentMatcher = EquipmentRequest.ACC3_PATTERN.matcher( responseText );
			if ( equipmentMatcher.find() )
			{
				name = equipmentMatcher.group( 1 ).trim();
				if ( EquipmentDatabase.contains( name ) )
				{
					equipment[ EquipmentManager.ACCESSORY3 ] =
						new AdventureResult( equipmentMatcher.group( 1 ).trim(), 1, false );
				}

				RequestLogger.updateDebugLog( "Accessory 3: " + equipment[ EquipmentManager.ACCESSORY3 ] );
			}

		}

		if ( responseText.indexOf( "unequip&type=familiarequip" ) != -1 )
		{
			equipmentMatcher = EquipmentRequest.FAMILIARITEM_PATTERN.matcher( responseText );
			if ( equipmentMatcher.find() )
			{
				name = equipmentMatcher.group( 1 ).trim();
				if ( ItemDatabase.contains( name ) )
				{
					equipment[ EquipmentManager.FAMILIAR ] =
						new AdventureResult( equipmentMatcher.group( 1 ).trim(), 1, false );
				}

				RequestLogger.updateDebugLog( "Familiar: " + equipment[ EquipmentManager.FAMILIAR ] );
			}
		}

		int index = 0;
		while ( ( index = responseText.indexOf( "unequip&type=fakehand", index ) ) != -1 )
		{
			++fakeHands;
			index += 21;
		}

		// First, handle all of the equipment pre-processing,
		// like inventory shuffling and the like.

		boolean refresh = EquipmentRequest.switchEquipment( oldEquipment, equipment );

		// Adjust inventory of fake hands

		int newFakeHands = EquipmentManager.getFakeHands();
		if ( oldFakeHands != newFakeHands )
		{
			AdventureResult.addResultToList( KoLConstants.inventory, new AdventureResult(
				EquipmentRequest.FAKE_HAND, newFakeHands - oldFakeHands ) );
			EquipmentManager.setFakeHands( fakeHands );
		}

		// Look for custom outfits

		Matcher outfitsMatcher = EquipmentRequest.OUTFITLIST_PATTERN.matcher( responseText );
		LockableListModel outfits = outfitsMatcher.find() ? SpecialOutfit.parseOutfits( outfitsMatcher.group() ) : null;

		EquipmentManager.setOutfits( outfits );

		// Check if familiar equipment is locked
		FamiliarData.checkLockedItem( responseText );

		// If Lord Spookyraven's spectacles are now equipped, identify
		// the dusty bottles, if necessary.

		if ( KoLCharacter.hasEquipped( EquipmentRequest.SPECTACLES ) )
		{
			ItemDatabase.identifyDustyBottles();
		}

		// If he's wearing a custom outfit, do additional processing
		EquipmentRequest.wearCustomOutfit( location );

		// If you need to update your creatables list, do so at
		// the end of the processing.

		if ( refresh )
		{
			ConcoctionDatabase.refreshConcoctions();
		}
	}

	private static final boolean switchEquipment( final AdventureResult [] oldEquipment, final AdventureResult [] newEquipment )
	{
		boolean refresh = false;

		if ( !KoLmafia.isRefreshing() )
		{
			for ( int i = 0; i < EquipmentManager.SLOTS; ++i )
			{
				if ( i == EquipmentManager.FAMILIAR )
				{
					// Omit familiar items, since inventory
					// is handled by familiar.setItem()
					continue;
				}

				AdventureResult oldItem = oldEquipment[ i ];
				AdventureResult newItem = newEquipment[ i ];

				refresh |= EquipmentRequest.switchItem( oldItem, newItem );
			}
		}

		// Now update your equipment to make sure that selected
		// items are properly selected in the dropdowns.

		EquipmentManager.setEquipment( newEquipment );

		return refresh;
	}

	private static final boolean switchItem( final int type, final AdventureResult newItem )
	{
		boolean refresh = false;

		if ( type != EquipmentManager.FAMILIAR )
		{
			// Inventory is handled by familiar.setItem()
			AdventureResult[] oldEquipment = EquipmentManager.currentEquipment();
			AdventureResult oldItem = oldEquipment[ type ];
			refresh = EquipmentRequest.switchItem( oldItem, newItem );
		}

		// Now update your equipment to make sure that selected
		// items are properly selected in the dropdowns.

		EquipmentManager.setEquipment( type, newItem );

		return refresh;
	}

	public static final void parseEquipmentChange( final String location, final String responseText )
	{
		// inv_equip.php?action=equip&whichitem=2764&slot=1&ajax=1
		// inv_equip.php?action=equip&whichitem=1234&ajax=1

		if ( location.indexOf( "action=equip" ) != -1 )
		{
			// We equipped an item.
			int itemId = EquipmentRequest.parseItemId( location );
			if ( itemId < 0 )
			{
				return;
			}

			int type = EquipmentManager.itemIdToEquipmentType( itemId );
			if ( type == EquipmentManager.ACCESSORY1 )
			{
				int slot = EquipmentRequest.parseSlot( location );
				switch ( slot )
				{
				case 2:
					type = EquipmentManager.ACCESSORY2;
					break;
				case 3:
					type = EquipmentManager.ACCESSORY3;
					break;
				}
			}

			if ( EquipmentRequest.switchItem( type, ItemPool.get( itemId, 1 ) ) )
			{
				ConcoctionDatabase.refreshConcoctions();
			}

			return;
		}

		// inv_equip.php?action=dualwield&whichitem=1325&ajax=1
		if ( location.indexOf( "action=dualwield" ) != -1 )
		{
			// We equipped an item.
			int itemId = EquipmentRequest.parseItemId( location );
			if ( itemId < 0 )
			{
				return;
			}

			if ( EquipmentRequest.switchItem( EquipmentManager.OFFHAND, ItemPool.get( itemId, 1 ) ) )
			{
				ConcoctionDatabase.refreshConcoctions();
			}

			return;
		}

		// inv_equip.php?action=unequip&type=acc3&ajax=1
		if ( location.indexOf( "action=unequip" ) != -1 )
		{
			// We unequipped an item.
			String slotName = EquipmentRequest.parseSlotName( location );
			if ( slotName == null )
			{
				return;
			}

			int type = EquipmentRequest.slotNumber( slotName );
			if ( type < 0 )
			{
				return;
			}

			if ( EquipmentRequest.switchItem( type, EquipmentRequest.UNEQUIP ) )
			{
				ConcoctionDatabase.refreshConcoctions();
			}

			return;
		}

		// inv_equip.php?action=outfit&whichoutfit=-28&ajax=1
		if ( location.indexOf( "action=outfit" ) != -1 )
		{
			// We changed into an outfit.

			// Since we don't actually know where accessories end
			// up, we could ask KoL for an update.

			// RequestThread.postRequest( new EquipmentRequest( EquipmentRequest.EQUIPMENT ) );
			// return;

			// ...but we'll apply heuristics and hope for the best.

			AdventureResult[] oldEquipment = EquipmentManager.currentEquipment();
			AdventureResult[] newEquipment = EquipmentManager.currentEquipment();

			Matcher unequipped = UNEQUIPPED_PATTERN.matcher( responseText );
			while ( unequipped.find() )
			{
				EquipmentRequest.unequipItem( newEquipment, unequipped.group( 1 ) );
			}

			Matcher equipped = EQUIPPED_PATTERN.matcher( responseText );
			while ( equipped.find() )
			{
				EquipmentRequest.equipItem( newEquipment, equipped.group( 1 ) );
			}

			if ( EquipmentRequest.switchEquipment( oldEquipment, newEquipment ) )
			{
				ConcoctionDatabase.refreshConcoctions();
			}

                        EquipmentRequest.wearCustomOutfit( location );

			return;
		}
	}

	private static final void wearCustomOutfit( final String urlString )
	{
		int outfitId = EquipmentRequest.customOutfitId;
		EquipmentRequest.customOutfitId = 0;

		SpecialOutfit outfit = EquipmentManager.getCustomOutfit( outfitId );
		if ( outfit == null )
		{
			return;
		}

		Matcher m = EquipmentRequest.OUTFIT_ACTION_PATTERN.matcher( outfit.getName() );
		while ( m.find() )
		{
			String text = m.group( 2 ).trim();
			switch ( m.group( 1 ).toLowerCase().charAt( 0 ) )
			{
			case 'c':
				KoLmafiaCLI.DEFAULT_SHELL.executeLine( text );
				break;
			case 'e':
				KoLmafiaCLI.DEFAULT_SHELL.executeCommand(
					"equip", "familiar " + text );
				break;
			case 'f':
				KoLmafiaCLI.DEFAULT_SHELL.executeCommand(
					"familiar", text );
				break;
			case 'm':
				KoLmafiaCLI.DEFAULT_SHELL.executeCommand(
					"mood", text );
				break;
			}
		}
	}

	private static void unequipItem( final AdventureResult [] equipment, final String name )
	{
		for ( int i = 0; i < EquipmentManager.SLOTS; ++i )
		{
			AdventureResult oldItem = equipment[ i ];
			if ( oldItem.getName().equals( name ) )
			{
				equipment[ i ] = EquipmentRequest.UNEQUIP;
				break;
			}
		}
	}

	private static void equipItem( final AdventureResult [] equipment, final String name )
	{
		if ( !EquipmentDatabase.contains( name ) )
		{
			return;
		}

		AdventureResult item = new AdventureResult( name, 1, false );
		int itemId = item.getItemId();
		int slot = EquipmentManager.itemIdToEquipmentType( itemId );

		switch ( slot )
		{
		case EquipmentManager.ACCESSORY1:
			// Heuristic: KoL seems to fill the last slots first.
			if ( equipment[ EquipmentManager.ACCESSORY3 ] == EquipmentRequest.UNEQUIP )
			{
				slot = EquipmentManager.ACCESSORY3;
			}
			else if ( equipment[ EquipmentManager.ACCESSORY2 ] == EquipmentRequest.UNEQUIP )
			{
				slot = EquipmentManager.ACCESSORY2;
			}
			else if ( equipment[ EquipmentManager.ACCESSORY1 ] == EquipmentRequest.UNEQUIP )
			{
				slot = EquipmentManager.ACCESSORY1;
			}
			break;

		case EquipmentManager.WEAPON:
			if ( equipment[ EquipmentManager.WEAPON ] != EquipmentRequest.UNEQUIP )
			{
				slot = EquipmentManager.OFFHAND;
			}
			break;
		}

		equipment[ slot ] = item;
	}

	private static int parseItemId( final String location )
	{
		Matcher matcher = ITEMID_PATTERN.matcher( location );
		return matcher.find() ? StringUtilities.parseInt( matcher.group( 1 ) ) : -1;
	}

	private static int parseSlot( final String location )
	{
		Matcher matcher = SLOT1_PATTERN.matcher( location );
		return matcher.find() ? StringUtilities.parseInt( matcher.group( 1 ) ) : -1;
	}

	private static String parseSlotName( final String location )
	{
		Matcher matcher = EquipmentRequest.SLOT_PATTERN.matcher( location );
		return matcher.find() ? matcher.group( 1 ) : null;
	}

	public static final int slotNumber( final String name )
	{
		for ( int i = 0; i < EquipmentRequest.slotNames.length; ++i )
		{
			if ( name.equalsIgnoreCase( EquipmentRequest.slotNames[ i ] ) )
			{
				return i;
			}
		}

		for ( int i = 0; i < EquipmentRequest.phpSlotNames.length; ++i )
		{
			if ( name.equalsIgnoreCase( EquipmentRequest.phpSlotNames[ i ] ) )
			{
				return i;
			}
		}

		return -1;
	}

	public static final boolean registerRequest( final String urlString )
	{
		if ( urlString.startsWith( "bedazzle.php" ) )
		{
			registerBedazzlement( urlString );
			return true;
		}

		if ( !urlString.startsWith( "inv_equip.php" ) )
		{
			return false;
		}

		EquipmentRequest.customOutfitId = 0;

		Matcher outfitMatcher = EquipmentRequest.OUTFIT_PATTERN.matcher( urlString );
		if ( outfitMatcher.find() )
		{
			int outfitId = StringUtilities.parseInt( outfitMatcher.group( 1 ) );
			if ( outfitId > 0 )
			{
				RequestLogger.updateSessionLog( "outfit " + EquipmentDatabase.getOutfit( outfitId ) );
				return true;
			}

			SpecialOutfit outfit = EquipmentManager.getCustomOutfit( outfitId );
			String name = outfit == null ? String.valueOf( outfitId ) : outfit.getName();

			RequestLogger.updateSessionLog( "custom outfit " + name );
			EquipmentRequest.customOutfitId = outfitId;
			return true;
		}

		if ( urlString.indexOf( "action=unequip" ) != -1 )
		{
			if ( urlString.indexOf( "terrarium=1" ) != -1 )
			{
				FamiliarRequest.unequipCurrentFamiliar();
				return true;
			}

			String slotName = parseSlotName( urlString );
			if ( slotName != null )
			{
				RequestLogger.updateSessionLog( "unequip " + slotName );
			}

			return true;
		}

		int itemId = EquipmentRequest.parseItemId( urlString );
		if ( itemId == -1 )
		{
			return true;
		}

		String itemName = ItemDatabase.getItemName( itemId );
		if ( itemName == null )
		{
			return true;
		}

		if ( urlString.indexOf( "dualwield" ) != -1 )
		{
			RequestLogger.updateSessionLog();
			RequestLogger.updateSessionLog( "equip off-hand " + itemName );
		}
		else if ( urlString.indexOf( "slot=1" ) != -1 )
		{
			RequestLogger.updateSessionLog();
			RequestLogger.updateSessionLog( "equip acc1 " + itemName );
		}
		else if ( urlString.indexOf( "slot=2" ) != -1 )
		{
			RequestLogger.updateSessionLog();
			RequestLogger.updateSessionLog( "equip acc2 " + itemName );
		}
		else if ( urlString.indexOf( "slot=3" ) != -1 )
		{
			RequestLogger.updateSessionLog();
			RequestLogger.updateSessionLog( "equip acc3 " + itemName );
		}
		else if ( urlString.indexOf( "terrarium=1" ) != -1 )
		{
			FamiliarRequest.equipCurrentFamiliar( itemId );
		}
		else
		{
			int slot = EquipmentRequest.chooseEquipmentSlot( ItemDatabase.getConsumptionType( itemName ) );
			if ( slot >= 0 && slot < EquipmentRequest.slotNames.length )
			{
				RequestLogger.updateSessionLog();
				RequestLogger.updateSessionLog( "equip " + EquipmentRequest.slotNames[ slot ] + " " + itemName );
			}
		}

		return true;
	}

	public static final void registerBedazzlement( final String urlString )
	{
		if ( urlString.indexOf( "action=fold" ) != -1 )
		{
			RequestLogger.updateSessionLog( "folded sticker weapon" );
			return;
		}

		if ( urlString.indexOf( "action=peel" ) != -1 )
		{
			Matcher slotMatcher = EquipmentRequest.SLOT1_PATTERN.matcher( urlString );
			if ( slotMatcher.find() )
			{
				RequestLogger.updateSessionLog( "peeled sticker " + slotMatcher.group( 1 ) );
			}
			return;
		}

		Matcher itemMatcher = EquipmentRequest.STICKERITEM_PATTERN.matcher( urlString );
		if ( !itemMatcher.find() )
		{
			return;
		}

		int itemId = StringUtilities.parseInt( itemMatcher.group( 1 ) );
		String itemName = ItemDatabase.getItemName( itemId );
		if ( itemName == null )
		{
			return;
		}

		if ( urlString.indexOf( "action=juststick" ) != -1 )
		{
			RequestLogger.updateSessionLog( "stuck " + itemName + " in empty slot" );
		}
		else if ( urlString.indexOf( "action=stick" ) != -1 )
		{
			int slot = EquipmentRequest.parseSlot( urlString );
			if ( slot > 0 )
			{
				RequestLogger.updateSessionLog( "stuck " + itemName + " in slot " + slot );
			}
		}
	}
}
