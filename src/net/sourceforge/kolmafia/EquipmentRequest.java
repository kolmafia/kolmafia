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

package net.sourceforge.kolmafia;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.java.dev.spellcast.utilities.LockableListModel;

public class EquipmentRequest
	extends PasswordHashRequest
{
	private static final KoLRequest COMBINE_PAGE = new KoLRequest( "" );
	private static final EquipmentRequest REFRESH1 = new EquipmentRequest( EquipmentRequest.CONSUMABLES );
	private static final EquipmentRequest REFRESH2 = new EquipmentRequest( EquipmentRequest.EQUIPMENT );
	private static final EquipmentRequest REFRESH3 = new EquipmentRequest( EquipmentRequest.MISCELLANEOUS );

	private static final Pattern CELL_PATTERN = Pattern.compile( "<td>(.*?)</td>" );
	private static final Pattern SELECT_PATTERN = Pattern.compile( "<select.*?</select>", Pattern.DOTALL );
	private static final Pattern MEAT_PATTERN = Pattern.compile( "[\\d,]+ meat\\.</b>" );
	private static final Pattern OUTSIDECLOSET_PATTERN = Pattern.compile( "<b>Put:.*?</select>", Pattern.DOTALL );
	private static final Pattern INSIDECLOSET_PATTERN = Pattern.compile( "<b>Take:.*?</select>", Pattern.DOTALL );
	private static final Pattern INVENTORYITEM_PATTERN =
		Pattern.compile( "<option value='?([\\d]+)'?[^>]*>([^>]*?) \\(([\\d,]+)\\)</option>" );
	private static final Pattern QUESTITEM_PATTERN = Pattern.compile( "<b>([^<]+)</b>([^<]*?)<font size=1>" );
	private static final Pattern HAT_PATTERN = Pattern.compile( "Hat:</td>.*?<b>(.*?)</b>.*unequip&type=hat" );
	private static final Pattern WEAPON_PATTERN = Pattern.compile( "Weapon:</td>.*?<b>(.*?)</b>.*unequip&type=weapon" );
	private static final Pattern OFFHAND_PATTERN =
		Pattern.compile( "Off-Hand:</td>.*?<b>([^<]*)</b> *(<font.*?/font>)?[^>]*unequip&type=offhand" );
	private static final Pattern SHIRT_PATTERN = Pattern.compile( "Shirt:</td>.*?<b>(.*?)</b>.*unequip&type=shirt" );
	private static final Pattern PANTS_PATTERN = Pattern.compile( "Pants:</td>.*?<b>(.*?)</b>.*unequip&type=pants" );
	private static final Pattern ACC1_PATTERN = Pattern.compile( "<b>([^<]*?)</b>\\s*<[^<]+unequip&type=acc1\">" );
	private static final Pattern ACC2_PATTERN = Pattern.compile( "<b>([^<]*?)</b>\\s*<[^<]+unequip&type=acc2\">" );
	private static final Pattern ACC3_PATTERN = Pattern.compile( "<b>([^<]*?)</b>\\s*<[^<]+unequip&type=acc3\">" );
	private static final Pattern FAMILIARITEM_PATTERN =
		Pattern.compile( "<b>([^<]*?)</b>\\s*<[^<]+unequip&type=familiarequip\">" );
	private static final Pattern OUTFITLIST_PATTERN = Pattern.compile( "<select name=whichoutfit>.*?</select>" );

	private static final Pattern OUTFIT_PATTERN = Pattern.compile( "whichoutfit=(\\d+)" );
	private static final Pattern SLOT_PATTERN = Pattern.compile( "type=([a-z]+)" );
	private static final Pattern ITEMID_PATTERN = Pattern.compile( "whichitem=(\\d+)" );

	public static final AdventureResult UNEQUIP = new AdventureResult( "(none)", 1, false );
	private static final AdventureResult PASTE = new AdventureResult( KoLConstants.MEAT_PASTE, 1 );
	private static final AdventureResult SPECTACLES = new AdventureResult( 1916, 1 );

	private static final int FAKE_HAND = 1511;

	public static final int CLOSET = 1;
	public static final int MISCELLANEOUS = 2;
	public static final int EQUIPMENT = 3;
	public static final int CONSUMABLES = 4;

	public static final int SAVE_OUTFIT = 5;
	public static final int CHANGE_OUTFIT = 6;

	public static final int CHANGE_ITEM = 7;
	public static final int REMOVE_ITEM = 8;
	public static final int UNEQUIP_ALL = 9;

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
		"fakehand"
	};

	private int requestType;
	private int equipmentSlot;
	private AdventureResult changeItem;
	private int itemId;
	private int equipmentType;
	private SpecialOutfit outfit;
	private String error;

	private static boolean shouldSavePreviousOutfit = false;

	public EquipmentRequest( final int requestType )
	{
		super(
			requestType == EquipmentRequest.CLOSET ? "closet.php" : requestType == EquipmentRequest.UNEQUIP_ALL ? "inv_equip.php" : "inventory.php" );

		this.requestType = requestType;
		this.outfit = null;
		this.error = null;

		// Otherwise, add the form field indicating which page
		// of the inventory you want to request

		if ( requestType == EquipmentRequest.MISCELLANEOUS )
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
			this.addFormField( "pwd" );
		}
	}

	public EquipmentRequest( final String changeName )
	{
		super( "inv_equip.php" );

		this.addFormField( "which", "2" );
		this.addFormField( "action", "customoutfit" );
		this.addFormField( "outfitname", changeName );
		this.requestType = EquipmentRequest.SAVE_OUTFIT;
	}

	public EquipmentRequest( final AdventureResult changeItem )
	{
		this(
			changeItem,
			EquipmentRequest.chooseEquipmentSlot( TradeableItemDatabase.getConsumptionType( changeItem.getItemId() ) ),
			false );
	}

	public EquipmentRequest( final AdventureResult changeItem, final int equipmentSlot )
	{
		this( changeItem, equipmentSlot, false );
	}

	public EquipmentRequest( final AdventureResult changeItem, final int equipmentSlot, final boolean force )
	{
		super( "inv_equip.php" );
		this.initializeChangeData( changeItem, equipmentSlot, force );
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
			this.error = null;
			this.addFormField( "action", "unequip" );
			this.addFormField( "type", EquipmentRequest.phpSlotNames[ equipmentSlot ] );
			this.addFormField( "pwd" );
			return;
		}

		// Find out what item is being equipped
		this.itemId = changeItem.getItemId();

		// Find out what kind of item it is
		this.equipmentType = TradeableItemDatabase.getConsumptionType( this.itemId );

		// If unspecified slot, pick based on type of item
		if ( this.equipmentSlot == -1 )
		{
			this.equipmentSlot = EquipmentRequest.chooseEquipmentSlot( this.equipmentType );
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
		this.addFormField( "pwd" );
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
		case KoLCharacter.HAT:
			if ( this.equipmentType == KoLConstants.EQUIP_HAT )
			{
				return "equip";
			}
			break;

		case KoLCharacter.WEAPON:
			if ( this.equipmentType == KoLConstants.EQUIP_WEAPON )
			{
				return "equip";
			}
			break;

		case KoLCharacter.OFFHAND:
			if ( this.equipmentType == KoLConstants.EQUIP_OFFHAND )
			{
				return "equip";
			}

			if ( this.equipmentType == KoLConstants.EQUIP_WEAPON )
			{
				return "dualwield";
			}
			break;

		case KoLCharacter.SHIRT:
			if ( this.equipmentType == KoLConstants.EQUIP_SHIRT )
			{
				return "equip";
			}
			break;

		case KoLCharacter.PANTS:
			if ( this.equipmentType == KoLConstants.EQUIP_PANTS )
			{
				return "equip";
			}
			break;

		case KoLCharacter.ACCESSORY1:
			if ( this.equipmentType == KoLConstants.EQUIP_ACCESSORY )
			{
				this.addFormField( "slot", "1" );
				return "equip";
			}
			break;

		case KoLCharacter.ACCESSORY2:
			if ( this.equipmentType == KoLConstants.EQUIP_ACCESSORY )
			{
				this.addFormField( "slot", "2" );
				return "equip";
			}
			break;

		case KoLCharacter.ACCESSORY3:
			if ( this.equipmentType == KoLConstants.EQUIP_ACCESSORY )
			{
				this.addFormField( "slot", "3" );
				return "equip";
			}
			break;

		case KoLCharacter.FAMILIAR:
			if ( this.equipmentType == KoLConstants.EQUIP_FAMILIAR )
			{
				return "equip";
			}
			break;

		default:
			return "equip";
		}

		this.error =
			"You can't equip a " + TradeableItemDatabase.getItemName( this.itemId ) + " in the " + EquipmentRequest.slotNames[ this.equipmentSlot ] + " slot.";

		return null;
	}

	public static final int chooseEquipmentSlot( final int equipmentType )
	{
		switch ( equipmentType )
		{
		case EQUIP_HAT:
			return KoLCharacter.HAT;

		case EQUIP_WEAPON:
			return KoLCharacter.WEAPON;

		case EQUIP_OFFHAND:
			return KoLCharacter.OFFHAND;

		case EQUIP_SHIRT:
			return KoLCharacter.SHIRT;

		case EQUIP_PANTS:
			return KoLCharacter.PANTS;

		case EQUIP_FAMILIAR:
			return KoLCharacter.FAMILIAR;

		case EQUIP_ACCESSORY:
		{
			AdventureResult test = KoLCharacter.getEquipment( KoLCharacter.ACCESSORY1 );
			if ( test == null || test.equals( EquipmentRequest.UNEQUIP ) )
			{
				return KoLCharacter.ACCESSORY1;
			}

			test = KoLCharacter.getEquipment( KoLCharacter.ACCESSORY2 );
			if ( test == null || test.equals( EquipmentRequest.UNEQUIP ) )
			{
				return KoLCharacter.ACCESSORY2;
			}

			test = KoLCharacter.getEquipment( KoLCharacter.ACCESSORY3 );
			if ( test == null || test.equals( EquipmentRequest.UNEQUIP ) )
			{
				return KoLCharacter.ACCESSORY3;
			}

			// All accessory slots are in use. Pick #1
			return KoLCharacter.ACCESSORY1;
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

				boolean unequipAllNeeded = false;

				for ( int i = 0; i < KoLCharacter.FAMILIAR; ++i )
				{
					unequipAllNeeded |= !KoLCharacter.getEquipment( i ).equals( EquipmentRequest.UNEQUIP );
				}

				if ( unequipAllNeeded )
				{
					( new EquipmentRequest( EquipmentRequest.UNEQUIP_ALL ) ).run();
				}

				( new EquipmentRequest( EquipmentRequest.UNEQUIP, KoLCharacter.FAMILIAR ) ).run();
				return;
			}

			int id = this.outfit.getOutfitId();

			// If this is not a custom outfit...
			if ( id > 0 )
			{
				// Return immediately if the character is already wearing the outfit
				if ( EquipmentDatabase.isWearingOutfit( id ) )
				{
					return;
				}

				// Next, ensure that you have all the pieces for the
				// given outfit.

				EquipmentDatabase.retrieveOutfit( id );

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
				// Return immediately if the character is already wearing the outfit
				if ( EquipmentDatabase.isWearingOutfit( this.outfit ) )
				{
					return;
				}

				AdventureResult[] pieces = this.outfit.getPieces();

				int equipmentType;

				boolean usesWeapon = false;
				boolean usesAccessories = false;

				for ( int i = 0; i < pieces.length; ++i )
				{
					equipmentType = TradeableItemDatabase.getConsumptionType( pieces[ i ].getItemId() );

					// If the item is an accessory, you will have to remove all
					// other accessories to be consistent.

					if ( equipmentType == KoLConstants.EQUIP_ACCESSORY )
					{
						if ( !usesAccessories )
						{
							for ( int j = KoLCharacter.ACCESSORY1; j <= KoLCharacter.ACCESSORY3; ++j )
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
							desiredSlot = KoLCharacter.OFFHAND;
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

			if ( KoLCharacter.getEquipment( this.equipmentSlot ).equals( this.changeItem ) )
			{
				return;
			}

			// If we are equipping a new weapon, a two-handed weapon will unequip any pair of
			// weapons. But a one-handed weapon much match the type of the off-hand weapon. If it
			// doesn't, unequip the off-hand weapon first

			if ( this.equipmentSlot == KoLCharacter.WEAPON )
			{
				AdventureResult offhand = KoLCharacter.getEquipment( KoLCharacter.OFFHAND );

				if ( TradeableItemDatabase.getConsumptionType( offhand.getItemId() ) == KoLConstants.EQUIP_WEAPON )
				{
					int desiredType = EquipmentDatabase.equipStat( this.changeItem.getItemId() );
					int currentType = EquipmentDatabase.equipStat( offhand.getName() );

					if ( EquipmentDatabase.getHands( this.changeItem.getItemId() ) == 1 && desiredType != currentType )
					{
						( new EquipmentRequest( EquipmentRequest.UNEQUIP, KoLCharacter.OFFHAND ) ).run();
					}
				}
			}

			if ( !AdventureDatabase.retrieveItem( this.changeItem ) )
			{
				return;
			}
		}

		if ( this.requestType == EquipmentRequest.REMOVE_ITEM && KoLCharacter.getEquipment( this.equipmentSlot ).equals(
			EquipmentRequest.UNEQUIP ) )
		{
			return;
		}

		switch ( this.requestType )
		{
		case MISCELLANEOUS:
			KoLmafia.updateDisplay( "Updating miscellaneous items..." );
			break;

		case CLOSET:
			KoLmafia.updateDisplay( "Refreshing closet..." );
			break;

		case EQUIPMENT:
			KoLmafia.updateDisplay( "Retrieving equipment..." );
			break;

		case SAVE_OUTFIT:
			KoLmafia.updateDisplay( "Saving outfit..." );
			break;

		case CHANGE_OUTFIT:
			KoLmafia.updateDisplay( "Putting on " + this.outfit + "..." );
			break;

		case CHANGE_ITEM:
			KoLmafia.updateDisplay( ( this.equipmentSlot == KoLCharacter.WEAPON ? "Wielding " : this.equipmentSlot == KoLCharacter.OFFHAND ? "Holding " : "Putting on " ) + TradeableItemDatabase.getItemName( this.itemId ) + "..." );
			break;

		case REMOVE_ITEM:
			KoLmafia.updateDisplay( "Taking off " + KoLCharacter.getEquipment( this.equipmentSlot ).getName() + "..." );
			break;

		case UNEQUIP_ALL:
			KoLmafia.updateDisplay( "Taking off everything..." );
			break;
		}

		super.run();

		switch ( this.requestType )
		{
		case SAVE_OUTFIT:
			KoLmafia.updateDisplay( "Outfit saved" );
			break;

		case CHANGE_ITEM:
		case CHANGE_OUTFIT:

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

				if ( result.indexOf( "You put" ) == -1 && result.indexOf( "You equip" ) == -1 && result.indexOf( "Item equipped" ) == -1 )
				{
					KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, result );
					return;
				}
			}

			if ( KoLCharacter.hasEquipped( EquipmentRequest.SPECTACLES ) )
			{
				TradeableItemDatabase.identifyDustyBottles();
			}

			KoLmafia.updateDisplay( "Equipment changed." );
			if ( !this.containsUpdate )
			{
				CharpaneRequest.getInstance().run();
			}

			break;

		case REMOVE_ITEM:

			KoLmafia.updateDisplay( "Equipment changed." );
			if ( !this.containsUpdate )
			{
				CharpaneRequest.getInstance().run();
			}

			break;

		case UNEQUIP_ALL:

			KoLmafia.updateDisplay( "Everything removed." );
			if ( !this.containsUpdate )
			{
				CharpaneRequest.getInstance().run();
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
			this.parseCloset();

			if ( !KoLConstants.inventory.contains( EquipmentRequest.PASTE ) && KoLCharacter.getAvailableMeat() >= 10 )
			{
				AdventureDatabase.retrieveItem( EquipmentRequest.PASTE );
			}

			// If the person has meat paste, or is able to create
			// meat paste, then do so and then parse the combines
			// page.  Otherwise, go to the equipment pages.

			if ( KoLCharacter.inMuscleSign() || KoLConstants.inventory.contains( EquipmentRequest.PASTE ) )
			{
				KoLCharacter.resetInventory();
				EquipmentRequest.COMBINE_PAGE.constructURLString(
					KoLCharacter.inMuscleSign() ? "knoll.php?place=paster" : "combine.php" ).run();

				Matcher selectMatcher =
					EquipmentRequest.SELECT_PATTERN.matcher( EquipmentRequest.COMBINE_PAGE.responseText );
				if ( selectMatcher.find() )
				{
					this.parseCloset( selectMatcher.group(), KoLConstants.inventory );
				}
			}
			else
			{
				KoLCharacter.resetInventory();
				EquipmentRequest.REFRESH1.run();
				EquipmentRequest.REFRESH3.run();
			}

			EquipmentRequest.REFRESH2.run();
			return;
		}

		int outfitDivider = this.responseText.indexOf( "Save as Custom Outfit" );

		if ( this.requestType != EquipmentRequest.MISCELLANEOUS && this.requestType != EquipmentRequest.CONSUMABLES )
		{
			// In valhalla, you can't make outfits and have no inventory.
			if ( outfitDivider == -1 )
			{
				return;
			}

			EquipmentRequest.parseEquipment( this.responseText );
		}

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

		return !ConcoctionsDatabase.getKnownUses( oldItem ).isEmpty() || !ConcoctionsDatabase.getKnownUses( newItem ).isEmpty();
	}

	private void parseCloset()
	{
		// Try to find how much meat is in your character's closet -
		// this way, the program's meat manager frame auto-updates

		Matcher meatInClosetMatcher = EquipmentRequest.MEAT_PATTERN.matcher( this.responseText );

		if ( meatInClosetMatcher.find() )
		{
			String meatInCloset = meatInClosetMatcher.group();
			KoLCharacter.setClosetMeat( StaticEntity.parseInt( meatInCloset ) );
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
			int itemId = StaticEntity.parseInt( optionMatcher.group( 1 ) );
			String itemName = TradeableItemDatabase.getCanonicalName( TradeableItemDatabase.getItemName( itemId ) );
			String realName = TradeableItemDatabase.getCanonicalName( optionMatcher.group( 2 ).toLowerCase() );

			if ( itemName == null || !realName.equals( itemName ) )
			{
				TradeableItemDatabase.registerItem( itemId, realName );
			}

			AdventureResult result = new AdventureResult( itemId, StaticEntity.parseInt( optionMatcher.group( 3 ) ) );
			if ( resultList == KoLConstants.inventory )
			{
				KoLCharacter.processResult( result, false );
			}
			else
			{
				AdventureResult.addResultToList( resultList, result );
			}
		}

		if ( resultList == KoLConstants.inventory )
		{
			KoLCharacter.updateEquipmentLists();
			ConcoctionsDatabase.refreshConcoctions();
		}
	}

	private static final void parseQuestItems( final String text )
	{
		Matcher itemMatcher = EquipmentRequest.QUESTITEM_PATTERN.matcher( text );
		while ( itemMatcher.find() )
		{
			String quantity = itemMatcher.group( 2 ).trim();
			String realName = itemMatcher.group( 1 ).trim();

			// We have encountered a brand new item, the person
			// has no meat paste, and we're in trouble.  Do not
			// continue if this is the case.

			if ( !TradeableItemDatabase.contains( realName ) )
			{
				continue;
			}

			int quantityValue =
				quantity.length() == 0 ? 1 : StaticEntity.parseInt( quantity.substring( 1, quantity.length() - 1 ) );
			int itemId = TradeableItemDatabase.getItemId( realName );

			AdventureResult item = new AdventureResult( itemId, quantityValue );
			int inventoryCount = item.getCount( KoLConstants.inventory );

			// Add the difference between your existing count
			// and the original count.

			if ( inventoryCount != quantityValue )
			{
				item = item.getInstance( quantityValue - inventoryCount );
				KoLCharacter.processResult( item );
			}
		}
	}

	public static final void parseEquipment( final String responseText )
	{
		AdventureResult[] oldEquipment = new AdventureResult[ 9 ];
		int oldFakeHands = KoLCharacter.getFakeHands();

		// Ensure that the inventory stays up-to-date by switching
		// items around, as needed.

		for ( int i = 0; i < 9; ++i )
		{
			oldEquipment[ i ] = KoLCharacter.getEquipment( i );
		}

		AdventureResult[] equipment = new AdventureResult[ 9 ];
		for ( int i = 0; i < equipment.length; ++i )
		{
			equipment[ i ] = EquipmentRequest.UNEQUIP;
		}
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
					equipment[ KoLCharacter.HAT ] = new AdventureResult( name, 1, false );
				}

				RequestLogger.updateDebugLog( "Hat: " + equipment[ KoLCharacter.HAT ] );
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
					equipment[ KoLCharacter.WEAPON ] =
						new AdventureResult( equipmentMatcher.group( 1 ).trim(), 1, false );
				}

				RequestLogger.updateDebugLog( "Weapon: " + equipment[ KoLCharacter.WEAPON ] );
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
					equipment[ KoLCharacter.OFFHAND ] =
						new AdventureResult( equipmentMatcher.group( 1 ).trim(), 1, false );
				}

				RequestLogger.updateDebugLog( "Off-hand: " + equipment[ KoLCharacter.OFFHAND ] );
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
					equipment[ KoLCharacter.SHIRT ] =
						new AdventureResult( equipmentMatcher.group( 1 ).trim(), 1, false );
				}

				RequestLogger.updateDebugLog( "Shirt: " + equipment[ KoLCharacter.SHIRT ] );
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
					equipment[ KoLCharacter.PANTS ] =
						new AdventureResult( equipmentMatcher.group( 1 ).trim(), 1, false );
				}

				RequestLogger.updateDebugLog( "Pants: " + equipment[ KoLCharacter.PANTS ] );
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
					equipment[ KoLCharacter.ACCESSORY1 ] =
						new AdventureResult( equipmentMatcher.group( 1 ).trim(), 1, false );
				}

				RequestLogger.updateDebugLog( "Accessory 1: " + equipment[ KoLCharacter.ACCESSORY1 ] );
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
					equipment[ KoLCharacter.ACCESSORY2 ] =
						new AdventureResult( equipmentMatcher.group( 1 ).trim(), 1, false );
				}

				RequestLogger.updateDebugLog( "Accessory 2: " + equipment[ KoLCharacter.ACCESSORY2 ] );
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
					equipment[ KoLCharacter.ACCESSORY3 ] =
						new AdventureResult( equipmentMatcher.group( 1 ).trim(), 1, false );
				}

				RequestLogger.updateDebugLog( "Accessory 3: " + equipment[ KoLCharacter.ACCESSORY3 ] );
			}

		}

		if ( responseText.indexOf( "unequip&type=familiarequip" ) != -1 )
		{
			equipmentMatcher = EquipmentRequest.FAMILIARITEM_PATTERN.matcher( responseText );
			if ( equipmentMatcher.find() )
			{
				name = equipmentMatcher.group( 1 ).trim();
				if ( TradeableItemDatabase.contains( name ) )
				{
					equipment[ KoLCharacter.FAMILIAR ] =
						new AdventureResult( equipmentMatcher.group( 1 ).trim(), 1, false );
				}

				RequestLogger.updateDebugLog( "Familiar: " + equipment[ KoLCharacter.FAMILIAR ] );
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

		boolean refreshCreations = false;

		if ( !KoLmafia.isRefreshing() )
		{
			for ( int i = 0; i <= KoLCharacter.FAMILIAR; ++i )
			{
				refreshCreations |= EquipmentRequest.switchItem( oldEquipment[ i ], equipment[ i ] );
			}
		}

		// Adjust inventory of fake hands

		int newFakeHands = KoLCharacter.getFakeHands();
		if ( oldFakeHands != newFakeHands )
		{
			AdventureResult.addResultToList( KoLConstants.inventory, new AdventureResult(
				EquipmentRequest.FAKE_HAND, newFakeHands - oldFakeHands ) );
			KoLCharacter.setFakeHands( fakeHands );
		}

		// Now update your equipment to make sure that selected
		// items are properly selected in the dropdowns.

		Matcher outfitsMatcher = EquipmentRequest.OUTFITLIST_PATTERN.matcher( responseText );

		LockableListModel outfits = outfitsMatcher.find() ? SpecialOutfit.parseOutfits( outfitsMatcher.group() ) : null;

		KoLCharacter.setEquipment( equipment );
		KoLCharacter.setOutfits( outfits );

		EquipmentDatabase.updateOutfits();
		KoLCharacter.recalculateAdjustments();

		// If you need to update your creatables list, do so at
		// the end of the processing.

		if ( refreshCreations )
		{
			ConcoctionsDatabase.refreshConcoctions();
		}
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
		if ( !urlString.startsWith( "inv_equip.php" ) )
		{
			return false;
		}

		Matcher outfitMatcher = EquipmentRequest.OUTFIT_PATTERN.matcher( urlString );
		if ( outfitMatcher.find() )
		{
			int outfitId = StaticEntity.parseInt( outfitMatcher.group( 1 ) );
			if ( outfitId > 0 )
			{
				RequestLogger.updateSessionLog( "outfit " + EquipmentDatabase.getOutfit( outfitId ) );
				return true;
			}
			else
			{
				RequestLogger.updateSessionLog( "outfit [custom]" );
				return true;
			}
		}

		if ( urlString.indexOf( "action=unequip" ) != -1 )
		{
			Matcher slotMatcher = EquipmentRequest.SLOT_PATTERN.matcher( urlString );
			if ( slotMatcher.find() )
			{
				RequestLogger.updateSessionLog( "unequip " + slotMatcher.group( 1 ) );
				return true;
			}

			return false;
		}

		Matcher itemMatcher = EquipmentRequest.ITEMID_PATTERN.matcher( urlString );
		if ( !itemMatcher.find() )
		{
			return true;
		}

		String itemName = TradeableItemDatabase.getItemName( StaticEntity.parseInt( itemMatcher.group( 1 ) ) );
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
		else
		{
			int slot = EquipmentRequest.chooseEquipmentSlot( TradeableItemDatabase.getConsumptionType( itemName ) );
			if ( slot >= 0 && slot < EquipmentRequest.slotNames.length )
			{
				RequestLogger.updateSessionLog();
				RequestLogger.updateSessionLog( "equip " + EquipmentRequest.slotNames[ slot ] + " " + itemName );
			}
		}

		return true;
	}
}
