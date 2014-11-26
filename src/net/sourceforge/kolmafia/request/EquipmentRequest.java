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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.FamiliarData;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
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
import net.sourceforge.kolmafia.session.QuestManager;
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

	private static final Pattern HAT_PATTERN =
		Pattern.compile( "Hat</a>:</td>(<td><img[^']*'descitem\\(([\\d]+)[^>]*></td>)?<td><b>(.*?)</b>.*?unequip&type=hat" );
	private static final Pattern WEAPON_PATTERN =
		Pattern.compile( "Weapon</a>:</td>(<td><img[^']*'descitem\\(([\\d]+)[^>]*></td>)?<td><b>(.*?)</b>.*?unequip&type=weapon" );
	private static final Pattern OFFHAND_PATTERN =
		Pattern.compile( "Off-Hand</a>:</td>(<td><img[^']*'descitem\\(([\\d]+)[^>]*></td>)?<td><b>([^<]+)</b> *(<font[^>]*>[^<]*</font>)? *<a[^>]*unequip&type=offhand" );
	private static final Pattern CONTAINER_PATTERN =
		Pattern.compile( "Back</a>:</td>(<td><img[^']*'descitem\\(([\\d]+)[^>]*></td>)?<td><b>(.*?)</b>.*?unequip&type=container" );
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
	private static final Pattern FOLDER_PATTERN = Pattern.compile( "folders/folder(\\d+).gif" );

	private static final Pattern OUTFIT_ACTION_PATTERN = Pattern.compile(
		"([a-zA-Z])=([^=]+)(?!=)" );

	private static final Pattern OUTFIT_PATTERN = Pattern.compile( "whichoutfit=(-?\\d+|last)" );
	private static final Pattern SLOT_PATTERN = Pattern.compile( "type=([a-z123]+)" );
	private static final Pattern STICKERITEM_PATTERN = Pattern.compile( "sticker=(\\d+)" );
	private static final Pattern SLOT1_PATTERN = Pattern.compile( "slot=(\\d+)" );
	private static final Pattern OUTFITNAME_PATTERN = Pattern.compile( "outfitname=([^&]*)" );
	private static final Pattern OUTFITID_PATTERN = Pattern.compile( "outfitid: (\\d+)" );

	private static final Pattern EQUIPPED_PATTERN = Pattern.compile( "<td[^>]*>Item equipped:</td><td>.*?<b>(.*?)</b></td>" );
	private static final Pattern UNEQUIPPED_PATTERN = Pattern.compile( "<td[^>]*>Item unequipped:</td><td>.*?<b>(.*?)</b></td>" );

	public static final AdventureResult UNEQUIP = new AdventureResult( "(none)", 1, false );
	public static final AdventureResult TRUSTY = ItemPool.get( ItemPool.TRUSTY, 1 );
	private static final AdventureResult SPECTACLES = ItemPool.get( ItemPool.SPOOKYRAVEN_SPECTACLES, 1 );
	public final static AdventureResult cardSleeve = ItemPool.get( ItemPool.CARD_SLEEVE, 1 );

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
		"back",
		"shirt",
		"pants",
		"acc1",
		"acc2",
		"acc3",
		"familiar",
		"crown-of-thrones",
		"sticker1",
		"sticker2",
		"sticker3",
		"card-sleeve",
		"folder1",
		"folder2",
		"folder3",
		"folder4",
		"folder5",
		"buddy-bjorn",
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
		"crownofthrones",
		"st1",
		"st2",
		"st3",
		"cardsleeve",
		"folder1",
		"folder2",
		"folder3",
		"folder4",
		"folder5",
		"buddybjorn",
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
		      EquipmentRequest.chooseEquipmentSlot( changeItem.getItemId() ),
		      false );
	}

	public EquipmentRequest( final AdventureResult changeItem, final int equipmentSlot )
	{
		this( changeItem, equipmentSlot, false );
	}

	public EquipmentRequest( final AdventureResult changeItem, final int equipmentSlot, final boolean force )
	{
		super( EquipmentRequest.chooseEquipmentLocation( equipmentSlot ) );

		this.error = null;

		switch ( equipmentSlot )
		{
		case EquipmentManager.CROWN_OF_THRONES:
		case EquipmentManager.BUDDY_BJORN:
			break;
		case EquipmentManager.STICKER1:
		case EquipmentManager.STICKER2:
		case EquipmentManager.STICKER3:
			this.initializeStickerData( changeItem, equipmentSlot, force );
			break;
		case EquipmentManager.CARD_SLEEVE:
			this.initializeCardSleeveData( changeItem );
			break;
		case EquipmentManager.FOLDER1:
		case EquipmentManager.FOLDER2:
		case EquipmentManager.FOLDER3:
		case EquipmentManager.FOLDER4:
		case EquipmentManager.FOLDER5:
			this.initializeFolderData( changeItem, equipmentSlot );
			break;
		default:
			this.initializeChangeData( changeItem, equipmentSlot, force );
		}
	}

	public EquipmentRequest( final SpecialOutfit change )
	{
		super( "inv_equip.php" );

		this.addFormField( "which", "2" );
		this.addFormField( "action", "outfit" );
		this.addFormField( "whichoutfit",
				   change == SpecialOutfit.PREVIOUS_OUTFIT ?
				   "last" :
				   String.valueOf( change.getOutfitId() ) );
		this.addFormField( "ajax", "1" );

		this.requestType = EquipmentRequest.CHANGE_OUTFIT;
		this.outfit = change;
		this.error = null;
	}

	private static final String chooseEquipmentLocation( final int slot )
	{
		return	slot < EquipmentManager.SLOTS ? "inv_equip.php" :
			slot == EquipmentManager.CROWN_OF_THRONES || slot == EquipmentManager.BUDDY_BJORN ? "bogus.php" :
			( slot >= EquipmentManager.STICKER1 && slot <= EquipmentManager.STICKER3 ) ? "bedazzle.php" :
			slot == EquipmentManager.CARD_SLEEVE ? "inv_use.php" :
			slot == EquipmentManager.FAKEHAND ? "inv_equip.php" :
			( slot >= EquipmentManager.FOLDER1 && slot <= EquipmentManager.FOLDER5 ) ? "choice.php" :
			"bogus.php";
	}

	public static boolean isEquipmentChange( final String path )
	{
		return	path.startsWith( "inv_equip.php" ) &&
			// Saving a custom outfit is OK
			!path.contains( "action=customoutfit" );
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
		this.addFormField( "slot", String.valueOf( equipmentSlot - EquipmentManager.STICKER1 + 1 ) );

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

	private void initializeCardSleeveData( final AdventureResult card )
	{
		this.equipmentSlot = EquipmentManager.CARD_SLEEVE;
		this.addFormField( "whichitem", String.valueOf( ItemPool.CARD_SLEEVE ) );

		if ( card.equals( EquipmentRequest.UNEQUIP ) )
		{
			this.requestType = EquipmentRequest.REMOVE_ITEM;
			this.addFormField( "removecard", "1" );
			return;
		}

		// Find out what item is being equipped
		this.itemId = card.getItemId();

		// Find out what kind of item it is
		this.equipmentType = ItemDatabase.getConsumptionType( this.itemId );

		if ( this.equipmentType != KoLConstants.CONSUME_CARD )
		{
			this.error = "You can't slide a " + ItemDatabase.getItemName( this.itemId ) + " into a card sleeze.";
			return;
		}

		this.addFormField( "sleevecard", String.valueOf( this.itemId ) );
		this.requestType = EquipmentRequest.CHANGE_ITEM;
		this.changeItem = card.getCount() == 1 ? card : card.getInstance( 1 );
	}

	private void initializeFolderData( final AdventureResult folder, final int slot )
	{
		this.equipmentSlot = slot;
		this.addFormField( "whichchoice", "774" );

		if ( folder.equals( EquipmentRequest.UNEQUIP ) )
		{
			this.requestType = EquipmentRequest.REMOVE_ITEM;
			this.addFormField( "slot", String.valueOf( slot - EquipmentManager.FOLDER1 ) );
			this.addFormField( "option", "2" );
			return;
		}

		for ( int i = EquipmentManager.FOLDER1; i <= EquipmentManager.FOLDER5; i++ )
		{
			if ( i != slot && folder.equals( EquipmentManager.getEquipment( i ) ) )
			{
				this.error = "You can't equip two of the same folder";
				return;
			}
		}

		// Find out what item is being equipped
		this.itemId = folder.getItemId();

		// Find out what kind of item it is
		this.equipmentType = ItemDatabase.getConsumptionType( this.itemId );

		if ( this.equipmentType != KoLConstants.CONSUME_FOLDER )
		{
			this.error = "You can't equip a " + ItemDatabase.getItemName( this.itemId ) + " in a folder slot.";
			return;
		}

		this.requestType = EquipmentRequest.CHANGE_ITEM;
		this.changeItem = folder;
		this.addFormField( "option", "1" );
		this.addFormField( "folder", String.valueOf( this.itemId - ItemPool.FOLDER_01 + 1 ) );
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


		case EquipmentManager.FAKEHAND:
			if ( this.itemId == ItemPool.FAKE_HAND )
			{
				return "equip";
			}
			break;

		default:
			return "equip";
		}

		this.error =
			"You can't equip a " + ItemDatabase.getItemName( this.itemId ) + " in the " + EquipmentRequest.slotNames[ this.equipmentSlot ] + " slot.";

		return null;
	}

	public static final int chooseEquipmentSlot( final int itemId )
	{
		int equipmentType = ItemDatabase.getConsumptionType( itemId );
		switch ( equipmentType )
		{
		case KoLConstants.EQUIP_HAT:
			return EquipmentManager.HAT;

		case KoLConstants.EQUIP_WEAPON:
			return EquipmentManager.WEAPON;

		case KoLConstants.EQUIP_OFFHAND:
			return itemId == ItemPool.FAKE_HAND ? EquipmentManager.FAKEHAND : EquipmentManager.OFFHAND;

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

		case KoLConstants.CONSUME_CARD:
			return EquipmentManager.CARD_SLEEVE;

		case KoLConstants.CONSUME_FOLDER:
			return EquipmentRequest.availableFolder();

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

	private static final int availableSlot( final int [] slots )
	{
		return EquipmentRequest.availableSlot( slots, slots.length );
	}

	private static final int availableSlot( final int [] slots, final int count )
	{
		for ( int i = 0; i < count; ++i )
		{
			int slot = slots[ i ];
			AdventureResult test = EquipmentManager.getEquipment( slot );
			if ( test == null || test.equals( EquipmentRequest.UNEQUIP ) )
			{
				return slot;
			}
		}

		// All slots are in use.  Abort rather than risk peeling the wrong one.
		return -1;
	}

	public static final int [] STICKER_SLOTS = new int[]
	{
		EquipmentManager.STICKER1,
		EquipmentManager.STICKER2,
		EquipmentManager.STICKER3,
	};

	private static final int availableSticker()
	{
		return EquipmentRequest.availableSlot( STICKER_SLOTS );
	}

	public static final int [] FOLDER_SLOTS = new int[]
	{
		EquipmentManager.FOLDER1,
		EquipmentManager.FOLDER2,
		EquipmentManager.FOLDER3,
		EquipmentManager.FOLDER4,
		EquipmentManager.FOLDER5,
	};

	public static final int availableFolder()
	{
		return EquipmentRequest.availableSlot( FOLDER_SLOTS, KoLCharacter.inHighschool() ? 5 : 3 );
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
		if ( GenericRequest.abortIfInFightOrChoice() )
		{
			return;
		}

		if ( this.requestType == EquipmentRequest.REFRESH )
		{
			InventoryManager.refresh();
			return;
		}

		// If we were given bogus parameters, report the error now
		if ( this.error != null )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, this.error );
			return;
		}

		// Outfit changes are a bit quirky, so they're handled
		// first for easy visibility.

		if ( this.requestType == EquipmentRequest.CHANGE_OUTFIT )
		{
			// If this is a birthday suit outfit, then remove everything.
			if ( this.outfit == SpecialOutfit.BIRTHDAY_SUIT )
			{
				// See if you are wearing anything.
				boolean found = false;
				for ( int i = 0; i < EquipmentManager.FAMILIAR; ++i )
				{
					if ( !EquipmentManager.getEquipment( i ).equals( EquipmentRequest.UNEQUIP ) )
					{
						found = true;
						break;
					}
				}

				// If not, nothing to do
				if ( !found )
				{
					return;
				}

				// Make a checkpoint, if necessary
				if ( EquipmentRequest.shouldSavePreviousOutfit )
				{
					if ( SpecialOutfit.markImplicitCheckpoint() )
					{
						( new EquipmentRequest( "Backup" ) ).run();
					}

					EquipmentRequest.shouldSavePreviousOutfit = false;
				}

				// Tell KoL to unequip everything
				( new EquipmentRequest( EquipmentRequest.UNEQUIP_ALL ) ).run();

				return;
			}
			else if ( this.outfit == SpecialOutfit.PREVIOUS_OUTFIT )
			{
				// If we donning KoL's idea of your previous outfit
				// we have no idea what the pieces are.

				// *** KoL bug: whichoutfit=last doesn't actually work
				// *** Therefore, look up the actual outfit id.
				CustomOutfitRequest request = new CustomOutfitRequest( true );
				request.run();
				int previousOutfitId = request.getPreviousOutfitId();
				if ( previousOutfitId >= 0 )
				{
					KoLmafia.updateDisplay( MafiaState.ERROR, "No previous outfit saved" );
					return;
				}
				this.addFormField( "whichoutfit", String.valueOf( previousOutfitId ) );
			}
			else	
			{
				// Otherwise, try to retrieve them.

				// If you are already wearing the outfit, nothing to do
				if ( EquipmentManager.isWearingOutfit( this.outfit ) )
				{
					return;
				}

				// Make sure we have all the pieces
				if ( !EquipmentManager.retrieveOutfit( this.outfit ) )
				{
					return;
				}
			}

			// Make a checkpoint, if necessary
			if ( EquipmentRequest.shouldSavePreviousOutfit )
			{
				if ( SpecialOutfit.markImplicitCheckpoint() )
				{
					( new EquipmentRequest( "Backup" ) ).run();
				}

				EquipmentRequest.shouldSavePreviousOutfit = false;
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
					KoLmafia.updateDisplay( MafiaState.ERROR, "You can't dual wield unless you already have a main weapon." );
					return;
				}

				if ( EquipmentDatabase.getHands( weaponItemId ) > 1 )
				{
					String message = itemType == KoLConstants.EQUIP_WEAPON ?
						( "You can't wield a " + this.changeItem.getName() + " in your off-hand while wielding a 2-handed weapon." ) :
						( "You can't equip a " + this.changeItem.getName() + " in your off-hand while wielding a 2-handed weapon." );
					KoLmafia.updateDisplay( MafiaState.ERROR, message );
					return;
				}

				if ( itemType == KoLConstants.EQUIP_WEAPON &&
				     EquipmentDatabase.getWeaponType( itemId ) != EquipmentDatabase.getWeaponType( weaponItemId ) )
				{
					KoLmafia.updateDisplay( MafiaState.ERROR, "You can't hold a " + this.changeItem.getName() + " in your off-hand when wielding a " + weapon.getName() );
					return;
				}
			}

			if ( !InventoryManager.retrieveItem( this.changeItem ) )
			{
				return;
			}

			// Must remove an existing sticker or folder before
			// installing a new one in the same slot.
			if ( ( ( this.equipmentSlot >= EquipmentManager.STICKER1 && this.equipmentSlot <= EquipmentManager.STICKER3 ) ||
			       ( this.equipmentSlot >= EquipmentManager.FOLDER1 && this.equipmentSlot <= EquipmentManager.FOLDER5 ) ) &&
			     !EquipmentManager.getEquipment( this.equipmentSlot ).equals( EquipmentRequest.UNEQUIP ) )
			{
				( new EquipmentRequest( EquipmentRequest.UNEQUIP, this.equipmentSlot ) ).run();
			}
		}

		if ( this.requestType == EquipmentRequest.REMOVE_ITEM &&
		     equipmentSlot !=  EquipmentManager.FAKEHAND &&
		     EquipmentManager.getEquipment( this.equipmentSlot ).equals( EquipmentRequest.UNEQUIP ) )
		{
			return;
		}

		if ( this.equipmentSlot >= EquipmentManager.FOLDER1 && this.equipmentSlot <= EquipmentManager.FOLDER5 )
		{
			( new GenericRequest( "inventory.php?action=useholder" ) ).run();
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
			KoLmafia.updateDisplay( 
				( this.equipmentSlot == EquipmentManager.WEAPON ? "Wielding " :
				  this.equipmentSlot == EquipmentManager.OFFHAND ? "Holding " :
				  this.equipmentSlot == EquipmentManager.CARD_SLEEVE ? "Sliding in " :
				  "Putting on " ) +
				ItemDatabase.getItemName( this.itemId ) + "..." );
			break;

		case EquipmentRequest.REMOVE_ITEM:
			KoLmafia.updateDisplay( ( this.equipmentSlot == EquipmentManager.CARD_SLEEVE ? "Sliding out " :
						  "Taking off " ) + 
						( this.equipmentSlot == EquipmentManager.FAKEHAND ?
						  "fake hands" :
						  EquipmentManager.getEquipment( this.equipmentSlot ).getName() ) +
						"..." );
			break;

		case EquipmentRequest.UNEQUIP_ALL:
			KoLmafia.updateDisplay( "Taking off everything..." );
			break;
		}

		// You can only change a card in the card sleeve while it is in inventory
		boolean changeCardSleeve =
			this.equipmentSlot == EquipmentManager.CARD_SLEEVE &&
			KoLCharacter.hasEquipped( EquipmentRequest.cardSleeve );

		if ( changeCardSleeve )
		{
			RequestThread.postRequest( new EquipmentRequest( EquipmentRequest.UNEQUIP, EquipmentManager.OFFHAND ) );
		}

		super.run();

		if ( !KoLmafia.permitsContinue() )
		{
			return;
		}

		if ( changeCardSleeve )
		{
			RequestThread.postRequest( new EquipmentRequest( EquipmentRequest.cardSleeve, EquipmentManager.OFFHAND ) );
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
 
		if ( urlString.startsWith( "choice.php" ) && urlString.contains( "whichchoice=774" ) )
		{
			EquipmentRequest.parseFolders( responseText );
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
				if ( result.contains( "You are already wearing" ) )
				{
					// Not an error
					KoLmafia.updateDisplay( result );
					return;
				}

				if ( result.contains( "You put on part of" ) )
				{
					KoLmafia.updateDisplay( MafiaState.ERROR, "You only put on part of that outfit." );
					return;
				}

				// It appears you're already wearing all the
				// parts of the outfit 'outfitname' which you
				// possess or can wear.	 ... followed by a
				// table of missing pieces

				if ( result.contains( "which you possess or can wear" ) )
				{
					KoLmafia.updateDisplay( MafiaState.ERROR, "You're already wearing as much of that outfit as you can." );
					return;
				}

				if ( !result.contains( "You put" ) &&
				     !result.contains( "You equip" ) &&
				     !result.contains( "Item equipped" ) &&
				     !result.contains( "equips an item" ) &&
				     !result.contains( "as you put it on" ) &&
				     !result.contains( "You take the existing card out of the sleeve to make room" ) &&
				     !result.contains( "You apply the shiny sticker" ) &&
				     !result.contains( "fold it into an impromptu sword" ) )
				{
					KoLmafia.updateDisplay( MafiaState.ERROR, result );
					return;
				}
			}

			// Fall through
		case EquipmentRequest.SAVE_OUTFIT:
		case EquipmentRequest.REMOVE_ITEM:
		case EquipmentRequest.UNEQUIP_ALL:
			if ( this.equipmentSlot == EquipmentManager.CARD_SLEEVE )
			{
				EquipmentRequest.parseCardSleeve( responseText );
			}
			else if ( this.getURLString().contains( "ajax=1" ) )
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
			QuestManager.updateQuestItemEquipped( newItem.getItemId() );
		}

		if ( oldItem != EquipmentRequest.UNEQUIP )
		{
			AdventureResult.addResultToList( KoLConstants.inventory, oldItem.getInstance( 1 ) );
		}

		return !ConcoctionDatabase.getKnownUses( oldItem ).isEmpty() || !ConcoctionDatabase.getKnownUses( newItem ).isEmpty();
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

	private static final Pattern ACQUIRE_PATTERN = Pattern.compile( "You acquire an item: <b>(.*?)</b>" );
	private static final Pattern CONTAINS_PATTERN = Pattern.compile( "Your card sleeve currently contains an <b>(.*?)</b>" );

	public static final void parseCardSleeve( final String responseText )
	{
		// Putting a card into an empty card sleeve
		//
		// You put the Alice's Army Sniper in the card sleeve.
		// Your card sleeve currently contains an Alice's Army Sniper.
		//
		// Putting a card into an occupied card sleeve
		//
		// You take the existing card out of the sleeve to make room:
		// You acquire an item: Alice's Army Sniper
		// You put the Alice's Army Bowman in the card sleeve.
		// Your card sleeve currently contains an Alice's Army Bowman.
		//
		// Removing a card from a card sleeve
		//
		// You pull the card out of the sleeve.
		// You acquire an item: Alice's Army Sniper
		// Your card sleeve is currently empty.

		Matcher acquiresMatcher = EquipmentRequest.ACQUIRE_PATTERN.matcher( responseText );
		String acquired = acquiresMatcher.find() ? acquiresMatcher.group( 1 ) : null;
		Matcher containsMatcher = EquipmentRequest.CONTAINS_PATTERN.matcher( responseText );
		String contains = containsMatcher.find() ? containsMatcher.group( 1 ) : null;
		AdventureResult oldItem = acquired != null ? new AdventureResult( acquired, 1, false) : EquipmentRequest.UNEQUIP;
		AdventureResult newItem = contains != null ? new AdventureResult( contains, 1, false) : EquipmentRequest.UNEQUIP;

		if ( acquired != null )
		{
			// *** result processing added it to inventory and tally.
			AdventureResult remove = oldItem.getInstance( -1 );
			AdventureResult.addResultToList( KoLConstants.tally, remove );
			AdventureResult.addResultToList( KoLConstants.inventory, remove );
		}

		// Put the old item into inventory and remove the new one
		EquipmentRequest.switchItem( oldItem, newItem );
		EquipmentManager.setEquipment( EquipmentManager.CARD_SLEEVE, newItem );
	}

	public static final AdventureResult idToFolder( final String id )
	{
		int itemId = ItemPool.FOLDER_01 + StringUtilities.parseInt( id ) - 1;
		return ( itemId < ItemPool.FOLDER_01 || itemId > ItemPool.FOLDER_28 ) ?
			EquipmentRequest.UNEQUIP :
			ItemPool.get( itemId, 1 );
	}

	public static final void parseFolders( String responseText )
	{
		int startIndex = responseText.indexOf( "Contents of your Folder Holder" );
		int stopIndex = responseText.indexOf( "Folders in your Inventory" );
		if ( startIndex == -1 || stopIndex == -1 )
		{
			return;
		}

		String text = responseText.substring( startIndex, stopIndex );
		Matcher folderMatcher = EquipmentRequest.FOLDER_PATTERN.matcher( text );

		int slot = EquipmentManager.FOLDER1;
		while ( folderMatcher.find() && slot <= EquipmentManager.FOLDER5 )
		{
			AdventureResult folder = EquipmentRequest.idToFolder( folderMatcher.group( 1 ) );
			EquipmentManager.setEquipment( slot++, folder );
		}

		while ( slot <= EquipmentManager.FOLDER5 )
		{
			EquipmentManager.setEquipment( slot++, EquipmentRequest.UNEQUIP );
		}
	}

	public static final void parseEquipment( final String location, final String responseText )
	{
		if ( location.contains( "onlyitem=" ) )
		{
			return;
		}

		AdventureResult[] oldEquipment = EquipmentManager.currentEquipment();
		int oldFakeHands = EquipmentManager.getFakeHands();
		int newFakeHands = 0;

		// Ensure that the inventory stays up-to-date by switching
		// items around, as needed.

		AdventureResult[] equipment = EquipmentManager.emptyEquipmentArray();

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
						 "Back: ", EquipmentManager.CONTAINER );
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
			newFakeHands += 1;
			index += 21;
		}

		// First, handle all of the equipment pre-processing,
		// like inventory shuffling and the like.

		boolean refresh = EquipmentRequest.switchEquipment( oldEquipment, equipment );

		// Adjust inventory of fake hands

		if ( oldFakeHands != newFakeHands )
		{
			AdventureResult.addResultToList( KoLConstants.inventory,
							 ItemPool.get( ItemPool.FAKE_HAND, oldFakeHands - newFakeHands ) );
			EquipmentManager.setFakeHands( newFakeHands );
		}

		EquipmentManager.updateNormalOutfits();

		// Look for custom outfits

		Matcher outfitsMatcher = EquipmentRequest.OUTFITLIST_PATTERN.matcher( responseText );
		SpecialOutfit.checkOutfits( outfitsMatcher.find() ? outfitsMatcher.group() : null );

		// Check if familiar equipment is locked
		FamiliarData.checkLockedItem( responseText );

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
		if ( !responseText.contains( test ) )
		{
			return;
		}

		Matcher matcher = pattern.matcher( responseText );
		if ( !matcher.find() )
		{
			return;
		}

		String name = matcher.group( 3 ).trim();
		AdventureResult item;
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

		// We have nothing special to do for simple visits.
		if ( !matcher.find() )
		{
			return;
		}

		String action = matcher.group(1);

		// inv_equip.php?action=equip&whichitem=2764&slot=1&ajax=1
		// inv_equip.php?action=equip&whichitem=1234&ajax=1

		if ( action.equals( "equip" ) )
		{
			// Detect failure to equip
			if ( !responseText.contains( "You equip an item" ) &&
			     !responseText.contains( "Item equipped" ) &&
			     !responseText.contains( "equips an item" ) )
			{
				return;
			}

			// We equipped an item.
			int itemId = EquipmentRequest.parseItemId( location );
			if ( itemId < 0 )
			{
				return;
			}

			if ( itemId == ItemPool.FAKE_HAND )
			{
				int oldFakeHands = EquipmentManager.getFakeHands();
				EquipmentManager.setFakeHands( oldFakeHands + 1 );
				AdventureResult.addResultToList( KoLConstants.inventory, ItemPool.get( ItemPool.FAKE_HAND, -1 ) );
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
			// Detect failure to equip
			if ( !responseText.contains( "You equip an item" ) && !responseText.contains( "Item equipped" ) )
			{
				return;
			}

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
			if ( !responseText.contains( "All items unequipped" ) )
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
			// Detect failure to equip
			if ( !responseText.contains( "Item unequipped" ) )
			{
				return;
			}

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

			if ( type == EquipmentManager.FAKEHAND )
			{
				int oldFakeHands = EquipmentManager.getFakeHands();
				AdventureResult.addResultToList( KoLConstants.inventory, ItemPool.get( ItemPool.FAKE_HAND, oldFakeHands ) );
				EquipmentManager.setFakeHands( 0 );
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
			// Detect failure to equip
			if ( !responseText.contains( "equips an item" ) )
			{
				return;
			}

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
			// Detect failure to equip
			if ( !responseText.contains( "Your custom outfit has been saved" ) )
			{
				return;
			}

			// We saved a custom outfit. KoL assigned a new outfit
			// ID to it and was kind enough to tell it to us in an
			// HTML comment: <!-- outfitid: 61 -->

			matcher = OUTFITNAME_PATTERN.matcher( location );
			if ( !matcher.find() )
			{
				return;
			}
			String name = GenericRequest.decodeField( matcher.group( 1 ) );

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
				// Make a brand-new AdventureResult for each item
				if ( piece != EquipmentRequest.UNEQUIP )
				{
					piece = new AdventureResult( piece.getItemId(), 1 );
				}
				outfit.addPiece( piece );
			}

			// Add this outfit to the list of custom outfits.
			EquipmentManager.addCustomOutfit( outfit );

			return;
		}

		// inv_equip.php?action=outfit&whichoutfit=-28&ajax=1
		if ( action.equals( "outfit" ) )
		{
			// Detect failure to equip
			if ( !responseText.contains( "You put on" ) )
			{
				return;
			}

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
					// A chefstaff must go in the weapon slot,
					// but KoL does not always list it first.
					if ( slot == EquipmentManager.OFFHAND &&
					     EquipmentDatabase.isChefStaff( item ) )
					{
						// Move other weapon to offhand
						newEquipment[ EquipmentManager.OFFHAND ] = newEquipment[ EquipmentManager.WEAPON ];
						slot = EquipmentManager.WEAPON;
					}
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
			case 'b':
				KoLmafiaCLI.DEFAULT_SHELL.executeCommand(
					"bjornify", text );
				break;
			}
		}
	}

	private static int parseItemId( final String location )
	{
		Matcher matcher = GenericRequest.WHICHITEM_PATTERN.matcher( location );
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

		if ( urlString.startsWith( "inv_use.php" ) && urlString.contains( "whichitem=5009" ) )
		{
			return EquipmentRequest.registerCardSleeve( urlString );
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

		if ( urlString.contains( "action=unequip" ) )
		{
			if ( urlString.contains( "terrarium=1" ) )
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

		if ( urlString.contains( "dualwield" ) )
		{
			RequestLogger.updateSessionLog();
			RequestLogger.updateSessionLog( "equip off-hand " + itemName );
		}
		else if ( urlString.contains( "slot=1" ) )
		{
			RequestLogger.updateSessionLog();
			RequestLogger.updateSessionLog( "equip acc1 " + itemName );
		}
		else if ( urlString.contains( "slot=2" ) )
		{
			RequestLogger.updateSessionLog();
			RequestLogger.updateSessionLog( "equip acc2 " + itemName );
		}
		else if ( urlString.contains( "slot=3" ) )
		{
			RequestLogger.updateSessionLog();
			RequestLogger.updateSessionLog( "equip acc3 " + itemName );
		}
		else if ( urlString.contains( "terrarium=1" ) )
		{
			FamiliarRequest.equipCurrentFamiliar( itemId );
		}
		else
		{
			int slot = EquipmentRequest.chooseEquipmentSlot( ItemDatabase.getItemId( itemName ) );
			if ( slot >= 0 && slot < EquipmentRequest.slotNames.length )
			{
				RequestLogger.updateSessionLog();
				RequestLogger.updateSessionLog( "equip " + EquipmentRequest.slotNames[ slot ] + " " + itemName );
			}
		}

		return true;
	}

	private static final Pattern SLEEVECARD_PATTERN = Pattern.compile( "sleevecard=(\\d+)" );
	public static final boolean registerCardSleeve( final String urlString )
	{
		UseItemRequest.setLastItemUsed( ItemPool.get( ItemPool.CARD_SLEEVE, 1 ) );
		Matcher m = SLEEVECARD_PATTERN.matcher( urlString );
		String message =
			m.find() ?
			"equip card-sleeve " + ItemDatabase.getItemName( StringUtilities.parseInt( m.group( 1 ) ) ) :
			urlString.contains( "removecard=1" ) ?
			"unequip card-sleeve " :
			null;
		if ( message != null )
		{
			RequestLogger.updateSessionLog();
			RequestLogger.updateSessionLog( message );
		}

		return true;
	}

	public static final void registerBedazzlement( final String urlString )
	{
		if ( urlString.contains( "action=fold" ) )
		{
			RequestLogger.updateSessionLog( "folded sticker weapon" );
			return;
		}

		if ( urlString.contains( "action=peel" ) )
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

		if ( urlString.contains( "action=juststick" ) )
		{
			RequestLogger.updateSessionLog( "stuck " + itemName + " in empty slot" );
		}
		else if ( urlString.contains( "action=stick" ) )
		{
			int slot = EquipmentRequest.parseSlot( urlString );
			if ( slot > 0 )
			{
				RequestLogger.updateSessionLog( "stuck " + itemName + " in slot " + slot );
			}
		}
	}
}
