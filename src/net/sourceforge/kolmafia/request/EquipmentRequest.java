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
import net.sourceforge.kolmafia.FamiliarData;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.KoLmafiaCLI;
import net.sourceforge.kolmafia.RequestLogger;
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
	private static final Pattern CELL_PATTERN = Pattern.compile( "<td>(.*?)</td>" );

	// With images:
	//
	// <table class='item' id="ic653" rel="id=653&s=0&q=1&d=0&g=0&t=0&n=1&m=0&u=."><td class="img"><img src="http://images.kingdomofloathing.com/itemimages/airboat.gif" class="hand ircm" onClick='descitem(126122919,0, event);'></td><td id='i653' valign=top><b class="ircm">intragalactic rowboat</b>&nbsp;<span></span><font size=1><br></font></td></table>
	//
	// Without images:
	//
	// <table class='item' id="ic653" rel="id=653&s=0&q=1&d=0&g=0&t=0&n=1&m=0&u=."><td id='i653' valign=top><b class="ircm"><a onClick='javascript:descitem(126122919,0, event);'>intragalactic rowboat</a></b>&nbsp;<span></span><font size=1><br></font></td></table>

	private static final Pattern ITEMTABLE_PATTERN = Pattern.compile( "<table class='item' (.*?)</table>" );
	private static final Pattern QUESTITEM_PATTERN = Pattern.compile( "id=\"ic(\\d*)\" rel=\"([^\"]*)\">(<td class=\"img\".*?descitem.(\\d*))?.*?<b class=\"ircm\">(<a.*?descitem.(\\d*)[^>]*>)?(elven <i>limbos</i> gingerbread|[^<]+)(?:</a>)?</b>(?:&nbsp;<span>)?([^<]*?)(?:</span>)" );

	private static final Pattern HAT_PATTERN =
		Pattern.compile( "Hat</a>:</td>(<td><img[^']*'descitem\\(([\\d]+)[^>]*></td>)?<td><b>(.*?)</b>.*?unequip&type=hat" );
	private static final Pattern WEAPON_PATTERN =
		Pattern.compile( "Weapon</a>:</td>(<td><img[^']*'descitem\\(([\\d]+)[^>]*></td>)?<td><b>(.*?)</b>.*?unequip&type=weapon" );
	private static final Pattern OFFHAND_PATTERN =
		Pattern.compile( "Off-Hand</a>:</td>(<td><img[^']*'descitem\\(([\\d]+)[^>]*></td>)?<td><b>([^<]+)</b> *(<font[^>]*>[^<]*</font>)? *<a[^>]*unequip&type=offhand" );
	private static final Pattern CONTAINER_PATTERN =
		Pattern.compile( "Container</a>:</td>(<td><img[^']*'descitem\\(([\\d]+)[^>]*></td>)?<td><b>(.*?)</b>.*?unequip&type=container" );
	private static final Pattern SHIRT_PATTERN =
		Pattern.compile( "Shirt</a>:</td>(<td><img[^']*'descitem\\(([\\d]+)[^>]*></td>)?<td><b>(.*?)</b>.*?unequip&type=shirt" );
	private static final Pattern PANTS_PATTERN =
		Pattern.compile( "Pants</a>:</td>(<td><img[^']*'descitem\\(([\\d]+)[^>]*></td>)?<td><b>(.*?)</b>.*?unequip&type=pants" );
	private static final Pattern ACC1_PATTERN =
		Pattern.compile( "Accessory</a>(?:&nbsp;1)?:</td>(<td><img[^']*'descitem\\(([\\d]+)[^>]*></td>)?<td><b>([^<]+)</b> *<a[^>]*unequip&type=acc1" );
	private static final Pattern ACC2_PATTERN =
		Pattern.compile( "Accessory</a>(?:&nbsp;2)?:</td>(<td><img[^']*'descitem\\(([\\d]+)[^>]*></td>)?<td><b>([^>]+)</b> *<a[^>]*unequip&type=acc2" );
	private static final Pattern ACC3_PATTERN =
		Pattern.compile( "Accessory</a>(?:&nbsp;3)?:</td>(<td><img[^']*'descitem\\(([\\d]+)[^>]*></td>)?<td><b>([^>]+)</b> *<a[^>]*unequip&type=acc3" );
	private static final Pattern FAMILIARITEM_PATTERN =
		Pattern.compile( "Familiar</a>:</td>(<td><img[^']*'descitem\\(([\\d]+)[^>]*></td>)?<td><b>(.*?)</b>.*?unequip&type=familiarequip\"" );
	private static final Pattern OUTFITLIST_PATTERN = Pattern.compile( "<select name=whichoutfit>.*?</select>" );
	private static final Pattern STICKER_PATTERN = Pattern.compile(
		"<td>\\s*(shiny|dull)?\\s*([^<]+)<a [^>]+action=peel|<td>\\s*<img [^>]+magnify" );

	private static final Pattern OUTFIT_ACTION_PATTERN = Pattern.compile(
		"([a-zA-Z])=([^=]+)(?!=)" );

	private static final Pattern OUTFIT_PATTERN = Pattern.compile( "whichoutfit=(-?\\d+|last)" );
	private static final Pattern SLOT_PATTERN = Pattern.compile( "type=([a-z123]+)" );
	private static final Pattern ITEMID_PATTERN = Pattern.compile( "whichitem=(\\d+)" );
	private static final Pattern STICKERITEM_PATTERN = Pattern.compile( "sticker=(\\d+)" );
	private static final Pattern SLOT1_PATTERN = Pattern.compile( "slot=(\\d+)" );
	private static final Pattern OUTFITNAME_PATTERN = Pattern.compile( "outfitname=([^&]*)" );
	private static final Pattern OUTFITID_PATTERN = Pattern.compile( "outfitid: (\\d+)" );

	private static final Pattern EQUIPPED_PATTERN = Pattern.compile( "<td[^>]*>Item equipped:</td><td>.*?<b>(.*?)</b></td>" );
	private static final Pattern UNEQUIPPED_PATTERN = Pattern.compile( "<td[^>]*>Item unequipped:</td><td>.*?<b>(.*?)</b></td>" );

	public static final AdventureResult UNEQUIP = new AdventureResult( "(none)", 1, false );
	public static final AdventureResult TRUSTY = ItemPool.get( ItemPool.TRUSTY, 1 );
	private static final AdventureResult SPECTACLES = ItemPool.get( ItemPool.SPOOKYRAVEN_SPECTACLES, 1 );

	private static final int FAKE_HAND = 1511;

	public static final int REFRESH = 0;
	public static final int EQUIPMENT = 1;

	public static final int SAVE_OUTFIT = 2;
	public static final int CHANGE_OUTFIT = 3;

	public static final int CHANGE_ITEM = 4;
	public static final int REMOVE_ITEM = 5;
	public static final int UNEQUIP_ALL = 6;

	public static final int BEDAZZLEMENTS = 7;

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
		"container",
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
		"container",
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
		super( EquipmentRequest.choosePage( requestType ) );

		this.requestType = requestType;
		this.outfit = null;
		this.outfitName = null;
		this.error = null;

		// Otherwise, add the form field indicating which page
		// of the inventory you want to request

		switch ( requestType )
		{
		case EquipmentRequest.EQUIPMENT:
			this.addFormField( "which", "2" );
			break;
		case EquipmentRequest.BEDAZZLEMENTS:
			// no fields necessary
			break;
		case EquipmentRequest.SAVE_OUTFIT:
			this.addFormField( "ajax", "1" );
			this.addFormField( "which", "2" );
			break;
		case EquipmentRequest.UNEQUIP_ALL:
			this.addFormField( "ajax", "1" );
			this.addFormField( "which", "2" );
			this.addFormField( "action", "unequipall" );
			break;
		}
	}

	private static String choosePage( final int requestType )
	{
		switch ( requestType )
		{
		case EquipmentRequest.BEDAZZLEMENTS:
			return "bedazzle.php";
		case EquipmentRequest.SAVE_OUTFIT:
		case EquipmentRequest.UNEQUIP_ALL:
			return "inv_equip.php";
		default:
			return "inventory.php";
		}
	}

	public EquipmentRequest( final String changeName )
	{
		this( EquipmentRequest.SAVE_OUTFIT );
		this.addFormField( "action", "customoutfit" );
		this.addFormField( "outfitname", changeName );
		this.addFormField( "ajax", "1" );
		this.outfitName = changeName;
	}

	public EquipmentRequest( final AdventureResult changeItem )
	{
		this( changeItem,
		      EquipmentRequest.chooseEquipmentSlot( ItemDatabase.getConsumptionType( changeItem.getItemId() ) ),
		      false );
	}

	public EquipmentRequest( final AdventureResult changeItem, final int equipmentSlot )
	{
		this( changeItem, equipmentSlot, false );
	}

	public EquipmentRequest( final AdventureResult changeItem, final int equipmentSlot, final boolean force )
	{
		super( equipmentSlot >= EquipmentManager.STICKER1 ? "bedazzle.php" : "inv_equip.php" );

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

	public EquipmentRequest( final SpecialOutfit change )
	{
		super( "inv_equip.php" );

		this.addFormField( "which", "2" );
		this.addFormField( "action", "outfit" );
		this.addFormField( "whichoutfit",
				   change == SpecialOutfit.PREVIOUS_OUTFIT?
				   "last" : String.valueOf( change.getOutfitId() ) );
		this.addFormField( "ajax", "1" );

		this.requestType = EquipmentRequest.CHANGE_OUTFIT;
		this.outfit = change;
		this.error = null;
	}

	public static  boolean isEquipmentChange( final String path )
	{
		return	path.startsWith( "inv_equip.php" ) &&
			// Saving a custom outfit is OK
			path.indexOf( "action=customoutfit" ) == -1;
	}

	@Override
	protected boolean shouldFollowRedirect()
	{
		return true;
	}

	@Override
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
		this.addFormField( "ajax", "1" );
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

		case EquipmentManager.CONTAINER:
			if ( this.equipmentType == KoLConstants.EQUIP_CONTAINER )
			{
				return "equip";
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
			case KoLConstants.EQUIP_PANTS:
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

		case KoLConstants.EQUIP_CONTAINER:
			return EquipmentManager.CONTAINER;

		case KoLConstants.EQUIP_SHIRT:
			return EquipmentManager.SHIRT;

		case KoLConstants.EQUIP_PANTS:
			return EquipmentManager.PANTS;

		case KoLConstants.EQUIP_ACCESSORY:
			return EquipmentRequest.availableAccessory();

		case KoLConstants.EQUIP_FAMILIAR:
			return EquipmentManager.FAMILIAR;

		case KoLConstants.CONSUME_STICKER:
			return EquipmentRequest.availableSticker();

		default:
			return -1;
		}
	}

	private static final int availableAccessory()
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

	private static final int availableSticker()
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

	public String getOutfitName()
	{
		return this.outfit == null ? null : this.outfit.toString();
	}

	/**
	 * Executes the <code>EquipmentRequest</code>. Note that at the current time, only the character's currently
	 * equipped items and familiar item will be stored.
	 */

	@Override
	public void run()
	{
		if ( this.requestType == EquipmentRequest.REFRESH )
		{
			InventoryManager.refresh();
			return;
		}

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
		case EquipmentRequest.EQUIPMENT:
			KoLmafia.updateDisplay( "Retrieving equipment..." );
			break;

		case EquipmentRequest.BEDAZZLEMENTS:
			KoLmafia.updateDisplay( "Refreshing stickers..." );
			break;

		case EquipmentRequest.SAVE_OUTFIT:
			KoLmafia.updateDisplay( "Saving outfit: " + this.outfitName );
			break;

		case EquipmentRequest.CHANGE_OUTFIT:
			KoLmafia.updateDisplay( "Putting on outfit: " + this.outfit );
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

		if ( !KoLmafia.permitsContinue() )
		{
			return;
		}

		switch ( this.requestType )
		{
		case EquipmentRequest.REFRESH:
			return;

		case EquipmentRequest.SAVE_OUTFIT:
			KoLmafia.updateDisplay( "Outfit saved" );
			return;

		case EquipmentRequest.CHANGE_ITEM:
		case EquipmentRequest.CHANGE_OUTFIT:
		case EquipmentRequest.REMOVE_ITEM:
			KoLmafia.updateDisplay( "Equipment changed." );
			break;

		case EquipmentRequest.UNEQUIP_ALL:
			KoLmafia.updateDisplay( "Everything removed." );
			break;
		}
	}

	@Override
	public void processResults()
	{
		String urlString = this.getURLString();
		String responseText = this.responseText;

		if ( urlString.startsWith( "bedazzle.php" ) )
		{
			EquipmentRequest.parseBedazzlements( responseText );
			return;
		}

		switch ( this.requestType )
		{
		case EquipmentRequest.REFRESH:
			return;

		case EquipmentRequest.EQUIPMENT:
			EquipmentRequest.parseEquipment( urlString, responseText );
			return;

		case EquipmentRequest.CHANGE_ITEM:
		case EquipmentRequest.CHANGE_OUTFIT:
			String text = this.responseText == null ? "" : this.responseText;
			// What SHOULD we do if get a null responseText?

			Matcher resultMatcher = EquipmentRequest.CELL_PATTERN.matcher( text );
			if ( resultMatcher.find() )
			{
				String result = resultMatcher.group( 1 ).replaceAll( "</?b>", "" );
				if ( result.indexOf( "You are already wearing" ) != -1 )
				{
					// Not an error
					KoLmafia.updateDisplay( result );
					return;
				}

				if ( result.indexOf( "You put on part of" ) != -1 )
				{
					KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "You only put on part of that outfit." );
					return;
				}

				// It appears you're already wearing all the
				// parts of the outfit 'outfitname' which you
				// possess or can wear.	 ... followed by a
				// table of missing pieces

				if ( result.indexOf( "which you possess or can wear" ) != -1 )
				{
					KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "You're already wearing as much of that outfit as you can." );
					return;
				}

				if ( result.indexOf( "You put" ) == -1 &&
				     result.indexOf( "You equip" ) == -1 &&
				     result.indexOf( "Item equipped" ) == -1 &&
				     result.indexOf( "equips an item" ) == -1 &&
				     result.indexOf( "You apply the shiny sticker" ) == -1 &&
				     result.indexOf( "fold it into an impromptu sword" ) == -1 )
				{
					KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, result );
					return;
				}
			}

			// Fall through
		case EquipmentRequest.SAVE_OUTFIT:
		case EquipmentRequest.REMOVE_ITEM:
		case EquipmentRequest.UNEQUIP_ALL:
			if ( this.getURLString().indexOf( "ajax=1" ) != -1 )
			{
				EquipmentRequest.parseEquipmentChange( urlString, responseText );
			}
			return;
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

	private static final void parseQuestItems( final String text )
	{
		Matcher matcher = EquipmentRequest.ITEMTABLE_PATTERN.matcher( text );
		while ( matcher.find() )
		{
			Matcher itemMatcher = EquipmentRequest.QUESTITEM_PATTERN.matcher( matcher.group( 1 ) );
			if ( !itemMatcher.find() )
			{
				continue;
			}

			int itemId = StringUtilities.parseInt( itemMatcher.group( 1 ) );
			String relString = itemMatcher.group( 2 );
			String descId = itemMatcher.group( 3 ) != null ?
				itemMatcher.group( 4 ) :
				itemMatcher.group( 5 ) != null ?
				itemMatcher.group( 6 ) : "";
			String itemName = StringUtilities.getCanonicalName( ItemDatabase.getItemName( itemId ) );
			String realName = itemMatcher.group( 7 );
			String canonicalName = StringUtilities.getCanonicalName( realName.toLowerCase() );
			String quantity = itemMatcher.group( 8 );

			if ( itemName == null || !canonicalName.equals( itemName ) )
			{
				// Lookup item with api.php for additional info
				ItemDatabase.registerItem( itemId );
			}

			// The inventory never has the plural name.
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
				newItem = ItemPool.get( matcher.group( 2 ).trim(), 1 );
			}
			AdventureResult oldItem = EquipmentManager.getEquipment( slot );
			EquipmentManager.setEquipment( slot, newItem );
			if ( !KoLmafia.isRefreshing() &&
				!newItem.equals( oldItem ) )
			{
				if ( !oldItem.equals( EquipmentRequest.UNEQUIP ) &&
					!KoLConstants.inventory.contains( oldItem ) )
				{
					// Item was in the list for this slot
					// only so that it could be displayed
					// as the current item.	 Remove it.
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

		EquipmentRequest.parseEquipment( responseText, equipment,
						 "unequip&type=hat", EquipmentRequest.HAT_PATTERN,
						 "Hat: ", EquipmentManager.HAT );
		EquipmentRequest.parseEquipment( responseText, equipment,
						 "unequip&type=weapon", EquipmentRequest.WEAPON_PATTERN,
						 "Weapon: ", EquipmentManager.WEAPON );
		EquipmentRequest.parseEquipment( responseText, equipment,
						 "unequip&type=offhand", EquipmentRequest.OFFHAND_PATTERN,
						 "Offhand: ", EquipmentManager.OFFHAND );
		EquipmentRequest.parseEquipment( responseText, equipment,
						 "unequip&type=container", EquipmentRequest.CONTAINER_PATTERN,
						 "Container: ", EquipmentManager.CONTAINER );
		EquipmentRequest.parseEquipment( responseText, equipment,
						 "unequip&type=shirt", EquipmentRequest.SHIRT_PATTERN,
						 "Shirt: ", EquipmentManager.SHIRT );
		EquipmentRequest.parseEquipment( responseText, equipment,
						 "unequip&type=pants", EquipmentRequest.PANTS_PATTERN,
						 "Pants: ", EquipmentManager.PANTS );
		EquipmentRequest.parseEquipment( responseText, equipment,
						 "unequip&type=acc1", EquipmentRequest.ACC1_PATTERN,
						 "Accessory 1: ", EquipmentManager.ACCESSORY1 );
		EquipmentRequest.parseEquipment( responseText, equipment,
						 "unequip&type=acc2", EquipmentRequest.ACC2_PATTERN,
						 "Accessory 2: ", EquipmentManager.ACCESSORY2 );
		EquipmentRequest.parseEquipment( responseText, equipment,
						 "unequip&type=acc3", EquipmentRequest.ACC3_PATTERN,
						 "Accessory 3: ", EquipmentManager.ACCESSORY3 );
		EquipmentRequest.parseEquipment( responseText, equipment,
						 "unequip&type=familiarequip", EquipmentRequest.FAMILIARITEM_PATTERN,
						 "Familiar: ", EquipmentManager.FAMILIAR );

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
		SpecialOutfit.checkOutfits( outfitsMatcher.find() ? outfitsMatcher.group() : null );

		EquipmentManager.updateOutfits();

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
			ConcoctionDatabase.setRefreshNeeded( false );
		}
	}

	private static final void parseEquipment( final String responseText, AdventureResult[] equipment,
						  final String test, final Pattern pattern,
						  final String tag, final int slot )
	{
		if ( responseText.indexOf( test ) == -1 )
		{
			return;
		}

		Matcher matcher = pattern.matcher( responseText );
		if ( !matcher.find() )
		{
			return;
		}

		String name = matcher.group( 3 ).trim();
		AdventureResult item = equipment[ slot ];
		if ( EquipmentDatabase.contains( name ) )
		{
			item = new AdventureResult( name, 1, false );
		}
		else
		{
			String descId = matcher.group( 1 ) != null ? matcher.group( 2 ) : "";
			RequestLogger.printLine( "Found unknown equipped item: \"" + name + "\" descid = " + descId );

			// No itemId available for equipped items!
			// ItemDatabase.registerItem( itemId, name, descId );

			// Put in a dummy item. If it gets unequipped, we will
			// find and identify it in inventory.
			item = AdventureResult.tallyItem( name, 1, false );
		}

		equipment[ slot ] = item;

		if ( RequestLogger.isDebugging() )
		{
			RequestLogger.updateDebugLog( tag + equipment[ slot ] );
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

		switch ( type )
		{
		case EquipmentManager.FAMILIAR:
			// Inventory is handled by familiar.setItem()
			break;

		case EquipmentManager.WEAPON:
			// Wielding a two-handed weapon automatically unequips
			// anything in the off-hand
			if ( EquipmentDatabase.getHands( newItem.getItemId() ) > 1 )
			{
				refresh |= EquipmentRequest.switchItem( EquipmentManager.OFFHAND, EquipmentRequest.UNEQUIP );
			}
			// fall through
		default:
			AdventureResult oldItem = EquipmentManager.getEquipment( type );
			refresh |= EquipmentRequest.switchItem( oldItem, newItem );
			break;
		}

		// Now update your equipment to make sure that selected
		// items are properly selected in the dropdowns.

		EquipmentManager.setEquipment( type, newItem );

		return refresh;
	}

	public static final void parseEquipmentChange( final String location, final String responseText )
	{
		Matcher matcher = GenericRequest.ACTION_PATTERN.matcher( location );

		if ( !matcher.find() )
		{
			return;
		}

		String action = matcher.group(1);

		// We have nothing special to do for simple visits.
		// inv_equip.php?action=equip&whichitem=2764&slot=1&ajax=1
		// inv_equip.php?action=equip&whichitem=1234&ajax=1

		if ( action.equals( "equip" ) )
		{
			// We equipped an item.
			int itemId = EquipmentRequest.parseItemId( location );
			if ( itemId < 0 )
			{
				return;
			}

			int slot = EquipmentRequest.findEquipmentSlot( itemId, location );
			if ( EquipmentRequest.switchItem( slot, ItemPool.get( itemId, 1 ) ) )
			{
				ConcoctionDatabase.setRefreshNeeded( false );
			}

			return;
		}

		// inv_equip.php?action=dualwield&whichitem=1325&ajax=1
		if ( action.equals( "dualwield" ) )
		{
			// We equipped an item.
			int itemId = EquipmentRequest.parseItemId( location );
			if ( itemId < 0 )
			{
				return;
			}

			if ( EquipmentRequest.switchItem( EquipmentManager.OFFHAND, ItemPool.get( itemId, 1 ) ) )
			{
				ConcoctionDatabase.setRefreshNeeded( false );
			}

			return;
		}

		// inv_equip.php?action=unequipall&ajax=1&
		if ( action.equals( "unequipall" ) )
		{
			// We unequipped everything
			if ( responseText.indexOf( "All items unequipped" ) == -1 )
			{
				return;
			}

			boolean switched = false;
			for ( int i = 0; i < EquipmentManager.SLOTS; ++i )
			{
				// Whether the familiar item is unequipped on
				// an unequip all is an account preference.
				if ( i == EquipmentManager.FAMILIAR &&
				     !KoLCharacter.getUnequipFamiliar() )
				{
					continue;
				}

				if ( EquipmentRequest.switchItem( i, EquipmentRequest.UNEQUIP ) )
				{
					switched = true;
				}
			}

			if ( switched )
			{
				ConcoctionDatabase.setRefreshNeeded( false );
			}
		}

		// inv_equip.php?action=unequip&type=acc3&ajax=1
		if ( action.equals( "unequip" ) )
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
				ConcoctionDatabase.setRefreshNeeded( false );
			}

			return;
		}

		// inv_equip.php?action=hatrack&whichitem=308&ajax=1
		if ( action.equals( "hatrack" ) )
		{
			// We equipped an item.
			int itemId = EquipmentRequest.parseItemId( location );
			if ( itemId < 0 )
			{
				return;
			}

			if ( EquipmentRequest.switchItem( EquipmentManager.FAMILIAR, ItemPool.get( itemId, 1 ) ) )
			{
				ConcoctionDatabase.setRefreshNeeded( false );
			}

			return;
		}

		// inv_equip.php?action=customoutfit&outfitname=Backup
		if ( action.equals( "customoutfit" ) )
		{
			// We saved a custom outfit. KoL assigned a new outfit
			// ID to it and was kind enough to tell it to us in an
			// HTML comment: <!-- outfitid: 61 -->

			matcher = OUTFITNAME_PATTERN.matcher( location );
			if ( !matcher.find() )
			{
				return;
			}
			String name = GenericRequest.decodeURL( matcher.group( 1 ) );

			matcher = OUTFITID_PATTERN.matcher( responseText );
			if ( !matcher.find() )
			{
				return;
			}
			int id = StringUtilities.parseInt( matcher.group( 1 ) );

			// Make a new custom outfit
			SpecialOutfit outfit = new SpecialOutfit( -id, name );

			AdventureResult[] equipment = EquipmentManager.currentEquipment();
			// Add our current equipment to it
			for ( int slot = 0; slot < EquipmentManager.FAMILIAR; ++slot )
			{
				AdventureResult piece = equipment[ slot ];
				outfit.addPiece( piece );
			}

			// Add this outfit to the list of custom outfits.
			EquipmentManager.addCustomOutfit( outfit );

			return;
		}

		// inv_equip.php?action=outfit&whichoutfit=-28&ajax=1
		if ( action.equals( "outfit" ) )
		{
			// We changed into an outfit.

			// Since KoL doesn't tell us where accessories end up,
			// we could ask for an update, but we'll apply
			// heuristics and hope for the best.

			AdventureResult[] oldEquipment = EquipmentManager.currentEquipment();
			AdventureResult[] newEquipment = EquipmentManager.currentEquipment();

			// Experimentation suggests that accessories are
			// installed in	 "Item Equipped" order like this:
			// - fill empty accessory slots from 1 to 3
			// - replace previous accessories from 3 to 1
			//
			// Note that if an already equipped accessory is part
			// of the new outfit, it stays exactly where it was.

			// Iterate over all unequipped items.
			Matcher unequipped = UNEQUIPPED_PATTERN.matcher( responseText );
			while ( unequipped.find() )
			{
				String name = unequipped.group( 1 );

				if ( !EquipmentDatabase.contains( name ) )
				{
					continue;
				}

				AdventureResult item = new AdventureResult( name, 1, false );
				int slot = EquipmentManager.itemIdToEquipmentType( item.getItemId() );
				switch ( slot )
				{
				case EquipmentManager.ACCESSORY1:
					if ( newEquipment[ EquipmentManager.ACCESSORY3 ].equals( item ) )
					{
						slot = EquipmentManager.ACCESSORY3;
					}
					else if ( newEquipment[ EquipmentManager.ACCESSORY2 ].equals( item ) )
					{
						slot = EquipmentManager.ACCESSORY2;
					}
					else if ( !newEquipment[ EquipmentManager.ACCESSORY1 ].equals( item ) )
					{
						// KoL error: accessory not found
						continue;
					}
					break;

				case EquipmentManager.WEAPON:
					if ( newEquipment[ EquipmentManager.OFFHAND ].equals( item ) )
					{
						slot = EquipmentManager.OFFHAND;
					}
					else if ( !newEquipment[ EquipmentManager.WEAPON ].equals( item ) )
					{
						// KoL error: weapon not found
						continue;
					}
					break;
				default:
					// Everything else goes into an
					// unambiguous slot.
					break;
				}

				newEquipment[ slot ] = EquipmentRequest.UNEQUIP;
			}

			// Calculate accessory fill order
			int [] accessories = new int[] {
				EquipmentManager.ACCESSORY1,
				EquipmentManager.ACCESSORY2,
				EquipmentManager.ACCESSORY3
			};
			int accessoryIndex = 0;

			// Consume unfilled slots from 1 to 3
			for ( int slot = EquipmentManager.ACCESSORY1; slot <= EquipmentManager.ACCESSORY3; slot++ )
			{
				if ( oldEquipment[ slot] == EquipmentRequest.UNEQUIP )
				{
					accessories[ accessoryIndex++ ] = slot;
				}
			}
			// Consume filled slots from 3 to 1
			for ( int slot = EquipmentManager.ACCESSORY3; accessoryIndex < 3 && slot >= EquipmentManager.ACCESSORY1; slot-- )
			{
				if ( oldEquipment[ slot] != EquipmentRequest.UNEQUIP &&
				     newEquipment[ slot ] == EquipmentRequest.UNEQUIP )
				{
					accessories[ accessoryIndex++ ] = slot;
				}
			}

			// Calculate weapon fill order
			int [] weapons = new int[] {
				EquipmentManager.WEAPON,
				EquipmentManager.OFFHAND,
			};
			int weaponIndex = 0;

			// Reset equip indices
			accessoryIndex = 0;
			weaponIndex = 0;

			// Iterate over all equipped items.
			Matcher equipped = EQUIPPED_PATTERN.matcher( responseText );
			while ( equipped.find() )
			{
				String name = equipped.group( 1 );

				if ( !EquipmentDatabase.contains( name ) )
				{
					continue;
				}

				AdventureResult item = new AdventureResult( name, 1, false );
				int slot = EquipmentManager.itemIdToEquipmentType( item.getItemId() );
				switch ( slot )
				{
				case EquipmentManager.ACCESSORY1:
					if ( accessoryIndex >= 3 )
					{
						// KoL error: four accessories
						continue;
					}
					slot = accessories[ accessoryIndex++ ];
					break;

				case EquipmentManager.WEAPON:
					if ( weaponIndex >= 2 )
					{
						// KoL error: three weapons
						continue;
					}
					slot = weapons[ weaponIndex++ ];
					break;
				default:
					// Everything else goes into an
					// unambiguous slot.
					break;
				}

				newEquipment[ slot ] = item;
			}

			if ( EquipmentRequest.switchEquipment( oldEquipment, newEquipment ) )
			{
				ConcoctionDatabase.setRefreshNeeded( false );
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
			case 't':
				KoLmafiaCLI.DEFAULT_SHELL.executeCommand(
					"enthrone", text );
				break;
			}
		}
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

		return EquipmentRequest.phpSlotNumber( name );
	}

	public static final int phpSlotNumber( final String name )
	{
		for ( int i = 0; i < EquipmentRequest.phpSlotNames.length; ++i )
		{
			if ( name.equalsIgnoreCase( EquipmentRequest.phpSlotNames[ i ] ) )
			{
				return i;
			}
		}

		return -1;
	}

	private static int findEquipmentSlot( final int itemId, final String location )
	{
		int type = EquipmentManager.itemIdToEquipmentType( itemId );

		// If it's not an accessory, slot is unambiguous
		if ( type != EquipmentManager.ACCESSORY1 )
		{
			return type;
		}

		// Accessories might specify the slot in the URL
		switch ( EquipmentRequest.parseSlot( location ) )
		{
		case 1:
			return EquipmentManager.ACCESSORY1;
		case 2:
			return EquipmentManager.ACCESSORY2;
		case 3:
			return EquipmentManager.ACCESSORY3;
		}

		// Otherwise, KoL picks the first empty accessory slot.
		return EquipmentRequest.availableAccessory();
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
			String outfitString = outfitMatcher.group( 1 );
			if ( outfitString.equals( "last" ) )
			{
				RequestLogger.updateSessionLog( "outfit last" );
				return true;
			}

			int outfitId = StringUtilities.parseInt( outfitString );
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
