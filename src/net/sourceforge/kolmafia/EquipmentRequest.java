/**
 * Copyright (c) 2005, KoLmafia development team
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
 *  [3] Neither the name "KoLmafia development team" nor the names of
 *      its contributors may be used to endorse or promote products
 *      derived from this software without specific prior written
 *      permission.
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
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.java.dev.spellcast.utilities.LockableListModel;

/**
 * An extension of <code>KoLRequest</code> which retrieves a list of
 * the character's equipment from the server.  At the current time,
 * there is no support for actually equipping items, so only the items
 * which are currently equipped are retrieved.
 */

public class EquipmentRequest extends PasswordHashRequest
{
	private static final Pattern CELL_PATTERN = Pattern.compile( "<td>(.*?)</td>" );
	private static final Pattern SELECT_PATTERN = Pattern.compile( "<select.*?</select>", Pattern.DOTALL );
	private static final Pattern MEAT_PATTERN = Pattern.compile( "[\\d,]+ meat\\.</b>" );
	private static final Pattern OUTSIDECLOSET_PATTERN = Pattern.compile( "<b>Put:.*?</select>", Pattern.DOTALL );
	private static final Pattern INSIDECLOSET_PATTERN = Pattern.compile( "<b>Take:.*?</select>", Pattern.DOTALL );
	private static final Pattern INVENTORYITEM_PATTERN = Pattern.compile( "<option value='?([\\d]+)'?[^>]*>([^>]*?) \\(([\\d,]+)\\)</option>" );
	private static final Pattern QUESTITEM_PATTERN = Pattern.compile( "<b>(<a.*?>)?([^<]+)(</a>)?</b>([^<]*?)<font size=1>" );
	private static final Pattern HAT_PATTERN = Pattern.compile( "Hat:</td>.*?<b>(.*?)</b>.*unequip&type=hat" );
	private static final Pattern WEAPON_PATTERN = Pattern.compile( "Weapon:</td>.*?<b>(.*?)</b>.*unequip&type=weapon" );
	private static final Pattern OFFHAND_PATTERN = Pattern.compile( "Off-Hand:</td>.*?<b>([^<]*)</b> *(<font.*?/font>)?[^>]*unequip&type=offhand" );
	private static final Pattern SHIRT_PATTERN = Pattern.compile( "Shirt:</td>.*?<b>(.*?)</b>.*unequip&type=shirt" );
	private static final Pattern PANTS_PATTERN = Pattern.compile( "Pants:</td>.*?<b>(.*?)</b>.*unequip&type=pants" );
	private static final Pattern ACC1_PATTERN = Pattern.compile( "Accessory ?1?:</td>.*?<b>([^<]*?)</b> *<a href=\"inv_equip.php\\?pwd=[^&]*&which=2&action=unequip&type=acc1\">" );
	private static final Pattern ACC2_PATTERN = Pattern.compile( "Accessory ?2?:</td>.*?<b>([^<]*?)</b> *<a href=\"inv_equip.php\\?pwd=[^&]*&which=2&action=unequip&type=acc2\">" );
	private static final Pattern ACC3_PATTERN = Pattern.compile( "Accessory ?3?:</td>.*?<b>([^<]*?)</b> *<a href=\"inv_equip.php\\?pwd=[^&]*&which=2&action=unequip&type=acc3\">" );
	private static final Pattern FAMILIARITEM_PATTERN = Pattern.compile( "Familiar:*</td>.*?<b>([^<]*?)</b>.*unequip&type=familiarequip" );
	private static final Pattern OUTFITLIST_PATTERN = Pattern.compile( "<select name=whichoutfit>.*?</select>" );

	private static final Pattern OUTFIT_PATTERN = Pattern.compile( "whichoutfit=(\\d+)" );
	private static final Pattern SLOT_PATTERN = Pattern.compile( "type=([a-z]+)" );
	private static final Pattern ITEMID_PATTERN = Pattern.compile( "whichitem=(\\d+)" );

	public static final AdventureResult UNEQUIP = new AdventureResult( "(none)", 1, false );
	private static final AdventureResult PASTE = new AdventureResult( ItemCreationRequest.MEAT_PASTE, 1 );

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
	public static final String [] slotNames =
	{
		"hat", "weapon", "off-hand", "shirt", "pants",
		"acc1", "acc2", "acc3", "familiar", "fakehand"
	};

        // These are the names used in the PHP file
	public static final String [] phpSlotNames =
	{
		"hat", "weapon", "offhand", "shirt", "pants",
		"acc1", "acc2", "acc3", "familiarequip", "fakehand"
	};

	private static final int FAKE_HAND = 1511;

	private int requestType;
	private int equipmentSlot;
	private String changeItemName;
	private int itemID;
	private int equipmentType;
	private SpecialOutfit outfit;
	private String error;

	public EquipmentRequest( int requestType )
	{
		super( requestType == CLOSET ? "closet.php" :
			requestType == UNEQUIP_ALL ? "inv_equip.php" : "inventory.php" );

		this.requestType = requestType;
		this.outfit = null;
		this.error = null;

		// Otherwise, add the form field indicating which page
		// of the inventory you want to request

		if ( requestType == MISCELLANEOUS )
			addFormField( "which", "3" );
		else if ( requestType == CONSUMABLES )
			addFormField( "which", "1" );
		else if ( requestType != CLOSET )
			addFormField( "which", "2" );

		if ( requestType == UNEQUIP_ALL )
		{
			addFormField( "action", "unequipall" );
			addFormField( "pwd" );
		}
	}

	public EquipmentRequest( String changeName )
	{
		super( "inv_equip.php" );

		if ( TradeableItemDatabase.contains( changeName ) )
		{
			initializeChangeData( new AdventureResult( changeName, 1, false ),
				chooseEquipmentSlot( TradeableItemDatabase.getConsumptionType( changeName ) ), false );
		}
		else
		{
			addFormField( "which", "2" );
			addFormField( "action", "customoutfit" );
			addFormField( "outfitname", changeName );
			requestType = SAVE_OUTFIT;
		}
	}

	public EquipmentRequest( AdventureResult change, int equipmentSlot )
	{	this( change, equipmentSlot, false );
	}

	public EquipmentRequest( AdventureResult change, int equipmentSlot, boolean force )
	{
		super( "inv_equip.php" );
		initializeChangeData( change, equipmentSlot, force );
	}

	private void initializeChangeData( AdventureResult change, int equipmentSlot, boolean force )
	{
		addFormField( "which", "2" );
		this.equipmentSlot = equipmentSlot;

		if ( change.equals( UNEQUIP ) )
		{
			this.requestType = REMOVE_ITEM;
			this.error = null;
			addFormField( "action", "unequip" );
			addFormField( "type", phpSlotNames[ equipmentSlot ] );
			addFormField( "pwd" );
			return;
		}

		// Find out what item is being equipped
		this.itemID = change.getItemID();

		// Find out what kind of item it is
		this.equipmentType = TradeableItemDatabase.getConsumptionType( itemID );

		// If unspecified slot, pick based on type of item
		if ( this.equipmentSlot == -1 )
			this.equipmentSlot = chooseEquipmentSlot( equipmentType );

		// Make sure you can equip it in the requested slot
		String action = getAction( force );
		if ( action == null )
			return;

		this.requestType = CHANGE_ITEM;
		this.changeItemName = KoLDatabase.getCanonicalName( change.getName() );

		addFormField( "action", action );
		addFormField( "whichitem", String.valueOf( itemID ) );
		addFormField( "pwd" );
	}

	public EquipmentRequest( SpecialOutfit change )
	{
		super( "inv_equip.php" );

		addFormField( "action", "outfit" );
		addFormField( "which", "2" );
		addFormField( "whichoutfit", String.valueOf( change.getOutfitID() ) );

		this.requestType = CHANGE_OUTFIT;
		this.outfit = change;
		this.error = null;
	}

	private String getAction( boolean force )
	{
		switch ( equipmentSlot )
		{
		case KoLCharacter.HAT:
			if ( equipmentType == ConsumeItemRequest.EQUIP_HAT )
				return "equip";
			break;

		case KoLCharacter.WEAPON:
			if ( equipmentType == ConsumeItemRequest.EQUIP_WEAPON )
				return "equip";
			break;

		case KoLCharacter.OFFHAND:
			if ( equipmentType == ConsumeItemRequest.EQUIP_OFFHAND )
				return "equip";

			if ( equipmentType == ConsumeItemRequest.EQUIP_WEAPON )
				return "dualwield";
			break;

		case KoLCharacter.SHIRT:
			if ( equipmentType == ConsumeItemRequest.EQUIP_SHIRT )
				return "equip";
			break;

		case KoLCharacter.PANTS:
			if ( equipmentType == ConsumeItemRequest.EQUIP_PANTS )
				return "equip";
			break;

		case KoLCharacter.ACCESSORY1:
			if ( equipmentType == ConsumeItemRequest.EQUIP_ACCESSORY )
			{
				addFormField( "slot", "1" );
				return "equip";
			}
			break;

		case KoLCharacter.ACCESSORY2:
			if ( equipmentType == ConsumeItemRequest.EQUIP_ACCESSORY )
			{
				addFormField( "slot", "2" );
				return "equip";
			}
			break;

		case KoLCharacter.ACCESSORY3:
			if ( equipmentType == ConsumeItemRequest.EQUIP_ACCESSORY )
			{
				addFormField( "slot", "3" );
				return "equip";
			}
			break;

		case KoLCharacter.FAMILIAR:
			if ( equipmentType == ConsumeItemRequest.EQUIP_FAMILIAR )
				return "equip";
			break;

		default:
			return "equip";
		}

		error = "You can't equip a " + TradeableItemDatabase.getItemName( itemID ) + " in the " +
			slotNames[equipmentSlot] + " slot.";

		return null;
	}

	public static int chooseEquipmentSlot( int equipmentType )
	{
		switch ( equipmentType )
		{
		case ConsumeItemRequest.EQUIP_HAT:
			return KoLCharacter.HAT;

		case ConsumeItemRequest.EQUIP_WEAPON:
			return KoLCharacter.WEAPON;

		case ConsumeItemRequest.EQUIP_OFFHAND:
			return KoLCharacter.OFFHAND;

		case ConsumeItemRequest.EQUIP_SHIRT:
			return KoLCharacter.SHIRT;

		case ConsumeItemRequest.EQUIP_PANTS:
			return KoLCharacter.PANTS;

		case ConsumeItemRequest.EQUIP_FAMILIAR:
			return KoLCharacter.FAMILIAR;

		case ConsumeItemRequest.EQUIP_ACCESSORY:
		{
			AdventureResult test = KoLCharacter.getEquipment( KoLCharacter.ACCESSORY1 );
			if ( test == null || test.equals( UNEQUIP ) )
				return KoLCharacter.ACCESSORY1;

			test = KoLCharacter.getEquipment( KoLCharacter.ACCESSORY2 );
			if ( test == null || test.equals( UNEQUIP ) )
				return KoLCharacter.ACCESSORY2;

			test = KoLCharacter.getEquipment( KoLCharacter.ACCESSORY3 );
			if ( test == null || test.equals( UNEQUIP ) )
				return KoLCharacter.ACCESSORY3;

			// All accessory slots are in use. Pick #1
			return KoLCharacter.ACCESSORY1;
		}

		default:
			return -1;
		}
	}

	public String getOutfitName()
	{	return outfit == null ? null : outfit.toString();
	}

	/**
	 * Executes the <code>EquipmentRequest</code>.  Note that at the current
	 * time, only the character's currently equipped items and familiar item
	 * will be stored.
	 */

	public void run()
	{
		// If we were given bogus parameters, report the error now
		if ( error != null )
		{
			KoLmafia.updateDisplay( ERROR_STATE, error );
			return;
		}

		// Outfit changes are a bit quirky, so they're handled
		// first for easy visibility.

		if ( requestType == CHANGE_OUTFIT )
		{
			// If this is a birthday suit outfit, then remove everything.
			if ( outfit == SpecialOutfit.BIRTHDAY_SUIT )
			{
				(new EquipmentRequest( UNEQUIP_ALL )).run();
				(new EquipmentRequest( UNEQUIP, KoLCharacter.FAMILIAR )).run();

				return;
			}

			int id = outfit.getOutfitID();

			// If this is not a custom outfit...
			if ( id > 0 )
			{
				// Return immediately if the character is already wearing the outfit
				if ( EquipmentDatabase.isWearingOutfit( id ) )
					return;

				// Next, ensure that you have all the pieces for the
				// given outfit.

				EquipmentDatabase.retrieveOutfit( id );

				// Bail now if the conditions were not met
				if ( !KoLmafia.permitsContinue() )
					return;
			}
		}

		if ( requestType == CHANGE_ITEM )
		{
			// Do not submit a request if the item matches what you
			// want to equip on the character.

			if ( KoLCharacter.hasEquipped( changeItemName, equipmentSlot ) )
				return;

			// If we are requesting another familiar's equipment,
			// make it available.

			if ( equipmentSlot == KoLCharacter.FAMILIAR )
			{
				AdventureResult result = new AdventureResult( itemID, 1 );
				if ( !inventory.contains( result ) &&
				     !closet.contains( result ))
				{
					// Find first familiar with item
					FamiliarData [] familiars = new FamiliarData[ KoLCharacter.getFamiliarList().size() ];
					KoLCharacter.getFamiliarList().toArray( familiars );
					for ( int i = 0; i < familiars.length; ++i )
					{
						if ( familiars[i].getItem() != null && familiars[i].getItem().indexOf( changeItemName ) != -1 )
						{
							KoLmafia.updateDisplay( "Stealing " + result.getName() + " from " + familiars[i].getRace() + "..." );
							KoLRequest unequip = new KoLRequest( "familiar.php?pwd=&action=unequip&famid=" + familiars[i].getID(), true );
							unequip.run();

							familiars[i].setItem( UNEQUIP.toString() );
							StaticEntity.getClient().processResult( result, false );

							break;
						}
					}
				}
			}

			AdventureDatabase.retrieveItem( new AdventureResult( changeItemName, 1, false ) );
			if ( !KoLmafia.permitsContinue() )
				return;

			// If we are changing familiar equipment, first we must
			// remove the old one in the slot.

			if ( equipmentSlot == KoLCharacter.FAMILIAR && !KoLCharacter.getEquipment( equipmentSlot ).equals( UNEQUIP ) )
				(new EquipmentRequest( UNEQUIP, equipmentSlot )).run();
		}

		switch ( requestType )
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
				KoLmafia.updateDisplay( "Putting on " + outfit + "..." );
				break;

			case CHANGE_ITEM:
				KoLmafia.updateDisplay( ( equipmentSlot == KoLCharacter.WEAPON ? "Wielding " :
					equipmentSlot == KoLCharacter.OFFHAND ? "Holding " : "Putting on " ) + TradeableItemDatabase.getItemName( itemID ) + "..." );
				break;

			case REMOVE_ITEM:
				KoLmafia.updateDisplay( "Taking off " + KoLCharacter.getEquipment( equipmentSlot ).getName() + "..." );
				break;

			case UNEQUIP_ALL:
				KoLmafia.updateDisplay( "Taking off everything..." );
				break;
		}

		super.run();

		// If you changed your outfit, there will be a redirect
		// to the equipment page - therefore, process it.

		if ( responseCode == 302 && !redirectLocation.equals( "maint.php" ) )
		{
			constructURLString( redirectLocation );
			super.run();
		}
	}

	protected void processResults()
	{
		super.processResults();

		// Fetch updated equipment
		if ( requestType == CLOSET )
		{
			parseCloset();

			// If the person has meat paste, or is able to create
			// meat paste, then do so and then parse the combines
			// page.  Otherwise, go to the equipment pages.

			if ( KoLCharacter.inMuscleSign() || KoLCharacter.hasItem( PASTE, false ) )
			{
				KoLRequest combines = new KoLRequest( KoLCharacter.inMuscleSign() ? "knoll.php?place=paster" : "combine.php" );
				combines.run();

				Matcher selectMatcher = SELECT_PATTERN.matcher( combines.responseText );
				if ( selectMatcher.find() )
					parseCloset( selectMatcher.group(), inventory );
			}
			else
			{
				(new EquipmentRequest( EquipmentRequest.CONSUMABLES )).run();
				(new EquipmentRequest( EquipmentRequest.MISCELLANEOUS )).run();
			}

			(new EquipmentRequest( EquipmentRequest.EQUIPMENT )).run();
			return;
		}

		if ( requestType == MISCELLANEOUS || requestType == CONSUMABLES )
		{
			parseQuestItems( responseText );
			return;
		}

		// In valhalla, you can't make outfits and have no inventory.
		if ( responseText.indexOf( "Save as Custom Outfit" ) == -1 )
			return;

		// Detect possible failure
		if ( requestType == CHANGE_ITEM || requestType == CHANGE_OUTFIT )
		{
			Matcher resultMatcher = CELL_PATTERN.matcher( responseText );
			if ( resultMatcher.find() )
			{
				String result = resultMatcher.group(1).replaceAll( "</?b>", "" );
				if ( result.indexOf( "You put" ) == -1 && result.indexOf( "You equip" ) == -1 && result.indexOf( "Item equipped" ) == -1 )
				{
					KoLmafia.updateDisplay( ERROR_STATE, result );
					return;
				}
			}
		}

		parseEquipment( this.responseText );
	}

	private static void switchItem( AdventureResult oldItem, AdventureResult newItem )
	{
		// Determine the item which is being switched
		// in and out.

		int switchIn = newItem.equals( UNEQUIP ) ? -1 : newItem.getItemID();
		int switchOut = oldItem.equals( UNEQUIP ) ? -1 : oldItem.getItemID();

		// If the items are not equivalent, make sure
		// the items should get switched out.

		if ( switchIn != switchOut )
		{
			// Manually subtract item from inventory to avoid
			// excessive list updating.

			if ( switchIn != -1 )
				AdventureResult.addResultToList( inventory, new AdventureResult( switchIn, -1 ) );

            // Items will be found when we parse quest items
		}
	}

	private void parseCloset()
	{
		// Try to find how much meat is in your character's closet -
		// this way, the program's meat manager frame auto-updates

		Matcher meatInClosetMatcher = MEAT_PATTERN.matcher( responseText );

		if ( meatInClosetMatcher.find() )
		{
			String meatInCloset = meatInClosetMatcher.group();
			KoLCharacter.setClosetMeat( StaticEntity.parseInt( meatInCloset ) );
		}

		Matcher inventoryMatcher = OUTSIDECLOSET_PATTERN.matcher( responseText );
		if ( inventoryMatcher.find() )
			parseCloset( inventoryMatcher.group(), inventory );

		Matcher closetMatcher = INSIDECLOSET_PATTERN.matcher( responseText );
		if ( closetMatcher.find() )
			parseCloset( closetMatcher.group(), closet );
	}

	private void parseCloset( String content, List resultList )
	{
		int lastFindIndex = 0;
		Matcher optionMatcher = INVENTORYITEM_PATTERN.matcher( content );

		while ( optionMatcher.find( lastFindIndex ) )
		{
			lastFindIndex = optionMatcher.end();
			int itemID = StaticEntity.parseInt( optionMatcher.group(1) );
			String itemName = TradeableItemDatabase.getCanonicalName( TradeableItemDatabase.getItemName( itemID ) );
			String realName = TradeableItemDatabase.getCanonicalName( optionMatcher.group(2).toLowerCase() );

			if ( itemName == null || !realName.equals( itemName ) )
				TradeableItemDatabase.registerItem( itemID, realName );

			AdventureResult result = new AdventureResult( itemID, StaticEntity.parseInt( optionMatcher.group(3) ) );
			int difference = result.getCount() - result.getCount( resultList );

			if ( difference != 0 )
			{
				result = result.getInstance( difference );
				if ( resultList == inventory )
					KoLCharacter.processResult( result );
				else
					AdventureResult.addResultToList( resultList, result );
			}
		}

		if ( resultList == inventory )
		{
			sellables.retainAll( inventory );
			usables.retainAll( inventory );
		}
	}

	private static void parseQuestItems( String text )
	{
		Matcher itemMatcher = QUESTITEM_PATTERN.matcher( text );
		while ( itemMatcher.find() )
		{
			String quantity = itemMatcher.group(4).trim();
			String realName = itemMatcher.group(2).trim();

			// We have encountered a brand new item, the person
			// has no meat paste, and we're in trouble.  Do not
			// continue if this is the case.

			if ( !TradeableItemDatabase.contains( realName ) )
				return;

			int quantityValue = quantity.length() == 0 ? 1 : StaticEntity.parseInt( quantity.substring( 1, quantity.length() - 1 ) );
			int itemID = TradeableItemDatabase.getItemID( realName );

			AdventureResult item = new AdventureResult( itemID, quantityValue );
			int inventoryCount = item.getCount( inventory );

			// Add the difference between your existing count
			// and the original count.

			if ( inventoryCount != quantityValue )
			{
				item = item.getInstance( quantityValue - inventoryCount );
				KoLCharacter.processResult( item );
			}
		}
	}

	public static void parseEquipment( String responseText )
	{
		AdventureResult [] oldEquipment = new AdventureResult[9];
		int oldFakeHands = KoLCharacter.getFakeHands();

		// Ensure that the inventory stays up-to-date by switching
		// items around, as needed.

		for ( int i = 0; i < 9; ++i )
			oldEquipment[i] = KoLCharacter.getEquipment( i );

		AdventureResult [] equipment = new AdventureResult[9];
		for ( int i = 0; i < equipment.length; ++i )
			equipment[i] = UNEQUIP;
		int fakeHands = 0;

		Matcher equipmentMatcher;

		if ( responseText.indexOf( "unequip&type=hat") != -1 )
		{
			 equipmentMatcher = HAT_PATTERN.matcher( responseText );
			if ( equipmentMatcher.find() )
			{
				equipment[ KoLCharacter.HAT ] = new AdventureResult( equipmentMatcher.group(1).trim(), 1, false );
				KoLmafia.getDebugStream().println( "Hat: " + equipment[ KoLCharacter.HAT ] );
			}
		}

		if ( responseText.indexOf( "unequip&type=weapon") != -1 )
		{
			equipmentMatcher = WEAPON_PATTERN.matcher( responseText );
			if ( equipmentMatcher.find() )
			{
				equipment[ KoLCharacter.WEAPON ] = new AdventureResult( equipmentMatcher.group(1).trim(), 1, false );
				KoLmafia.getDebugStream().println( "Weapon: " + equipment[ KoLCharacter.WEAPON ] );
			}
		}

		if ( responseText.indexOf( "unequip&type=offhand") != -1 )
		{
			equipmentMatcher = OFFHAND_PATTERN.matcher( responseText );
			if ( equipmentMatcher.find() )
			{
				equipment[ KoLCharacter.OFFHAND ] = new AdventureResult( equipmentMatcher.group(1).trim(), 1, false );
				KoLmafia.getDebugStream().println( "Off-hand: " + equipment[ KoLCharacter.OFFHAND ] );
			}
		}

		if ( responseText.indexOf( "unequip&type=shirt") != -1 )
		{
			equipmentMatcher = SHIRT_PATTERN.matcher( responseText );
			if ( equipmentMatcher.find() )
			{
				equipment[ KoLCharacter.SHIRT ] = new AdventureResult( equipmentMatcher.group(1).trim(), 1, false );
				KoLmafia.getDebugStream().println( "Shirt: " + equipment[ KoLCharacter.SHIRT ] );
			}
		}

		if ( responseText.indexOf( "unequip&type=pants") != -1 )
		{
			equipmentMatcher = PANTS_PATTERN.matcher( responseText );
			if ( equipmentMatcher.find() )
			{
				equipment[ KoLCharacter.PANTS ] = new AdventureResult( equipmentMatcher.group(1).trim(), 1, false );
				KoLmafia.getDebugStream().println( "Pants: " + equipment[ KoLCharacter.PANTS ] );
			}
		}

		if ( responseText.indexOf( "unequip&type=acc1" ) != -1 )
		{
			equipmentMatcher = ACC1_PATTERN.matcher( responseText );
			if ( equipmentMatcher.find() )
			{
				equipment[ KoLCharacter.ACCESSORY1 ] = new AdventureResult( equipmentMatcher.group(1).trim(), 1, false );
				KoLmafia.getDebugStream().println( "Accessory 1: " + equipment[ KoLCharacter.ACCESSORY1 ] );
			}
		}

		if ( responseText.indexOf( "unequip&type=acc2" ) != -1 )
		{
			equipmentMatcher = ACC2_PATTERN.matcher( responseText );
			if ( equipmentMatcher.find() )
			{
				equipment[ KoLCharacter.ACCESSORY2 ] = new AdventureResult( equipmentMatcher.group(1).trim(), 1, false );
				KoLmafia.getDebugStream().println( "Accessory 2: " + equipment[ KoLCharacter.ACCESSORY2 ] );
			}
		}

		if ( responseText.indexOf( "unequip&type=acc3") != -1 )
		{
			equipmentMatcher = ACC3_PATTERN.matcher( responseText );
			if ( equipmentMatcher.find() )
			{
				equipment[ KoLCharacter.ACCESSORY3 ] = new AdventureResult( equipmentMatcher.group(1).trim(), 1, false );
				KoLmafia.getDebugStream().println( "Accessory 3: " + equipment[ KoLCharacter.ACCESSORY3 ] );
			}

		}

		if ( responseText.indexOf( "unequip&type=familiarequip") != -1 )
		{
			equipmentMatcher = FAMILIARITEM_PATTERN.matcher( responseText );
			if ( equipmentMatcher.find() )
			{
				equipment[ KoLCharacter.FAMILIAR ] = new AdventureResult( equipmentMatcher.group(1).trim(), 1, false );
				KoLmafia.getDebugStream().println( "Familiar: " + equipment[ KoLCharacter.FAMILIAR ] );
			}
		}

		int index = 0;
		while ( ( index = responseText.indexOf( "unequip&type=fakehand", index) ) != -1 )
		{
			++fakeHands;
			index += 21;
		}

		Matcher outfitsMatcher = OUTFITLIST_PATTERN.matcher( responseText );

		LockableListModel outfits = outfitsMatcher.find() ?
			SpecialOutfit.parseOutfits( outfitsMatcher.group() ) : null;

		KoLCharacter.setEquipment( equipment );
		KoLCharacter.setOutfits( outfits );
		EquipmentDatabase.updateOutfits();
		KoLCharacter.setFakeHands( fakeHands );

		if ( !LoginRequest.isInstanceRunning() )
		{
			for ( int i = 0; i < 9; ++i )
				switchItem( oldEquipment[i], KoLCharacter.getEquipment( i ) );

			// Adjust inventory of fake hands
			int newFakeHands = KoLCharacter.getFakeHands();
			if ( oldFakeHands > newFakeHands )
				AdventureResult.addResultToList( inventory, new AdventureResult( FAKE_HAND, newFakeHands - oldFakeHands ) );

			CharpaneRequest.getInstance().run();
		}

		// Skip past equipped gear

		parseQuestItems( responseText.substring( responseText.indexOf( "Save as Custom Outfit" ) ) );
	}

	public static int slotNumber( String name )
	{
		for ( int i = 0; i < slotNames.length; ++i )
			if ( name.equalsIgnoreCase( slotNames[i] ) )
				return i;
		return -1;
	}

	public boolean isChangeRequest()
	{
		if ( getURLString().indexOf( "action=message" ) != -1 )
			return false;

		return requestType == CHANGE_OUTFIT || requestType == CHANGE_ITEM ||
			requestType == REMOVE_ITEM || requestType == UNEQUIP_ALL;
	}


	public String getCommandForm()
	{
		if ( !isChangeRequest() || requestType == UNEQUIP_ALL )
			return "";

		String outfitName = getOutfitName();

		if ( outfitName != null )
			return "outfit " + outfitName;

		if ( requestType == REMOVE_ITEM )
			return "unequip " + slotNames[ equipmentSlot ];

		return equipmentSlot == -1 ? "equip " + changeItemName :
			"equip " + slotNames[ equipmentSlot ] + " " + changeItemName;
	}

	public static boolean processRequest( String urlString )
	{
		if ( urlString.indexOf( "inv_equip.php" ) == -1 )
			return false;

		Matcher outfitMatcher = OUTFIT_PATTERN.matcher( urlString );
		if ( outfitMatcher.find() )
		{
			int outfitID = StaticEntity.parseInt( outfitMatcher.group(1) );
			if ( outfitID > 0 )
			{
				KoLmafia.getSessionStream().println( "outfit " + EquipmentDatabase.getOutfit( outfitID ) );
				return true;
			}
			else
			{
				KoLmafia.getSessionStream().println( "outfit [unknown custom outfit]" );
				return true;
			}
		}

		if ( urlString.indexOf( "action=unequip" ) != -1 )
		{
			Matcher slotMatcher = SLOT_PATTERN.matcher( urlString );
			if ( slotMatcher.find() )
			{
				KoLmafia.getSessionStream().println( "unequip " + slotMatcher.group(1) );
				return true;
			}

			return false;
		}

		Matcher itemMatcher = ITEMID_PATTERN.matcher( urlString );
		if ( !itemMatcher.find() )
			return false;

		String itemName = TradeableItemDatabase.getItemName( StaticEntity.parseInt( itemMatcher.group(1) ) );

		if ( urlString.indexOf( "dualwield" ) != -1 )
		{
			KoLmafia.getSessionStream().println();
			KoLmafia.getSessionStream().println( "equip off-hand " + itemName );
		}
		else
		{
			KoLmafia.getSessionStream().println();
			KoLmafia.getSessionStream().println( "equip " + itemName );
		}

		return true;
	}

	protected boolean mayChangeCreatables()
	{	return true;
	}
}
