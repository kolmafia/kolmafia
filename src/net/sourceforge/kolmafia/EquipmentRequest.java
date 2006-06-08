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
import java.util.StringTokenizer;
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
	public static final String UNEQUIP = "(none)";

	public static final int CLOSET = 1;
	public static final int QUESTS = 2;
	public static final int EQUIPMENT = 3;

	private static final int SAVE_OUTFIT = 4;
	private static final int CHANGE_OUTFIT = 5;

	private static final int CHANGE_ITEM = 6;
	private static final int REMOVE_ITEM = 7;
	private static final int UNEQUIP_ALL = 8;

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

	public EquipmentRequest( KoLmafia client, int requestType )
	{
		super( client, requestType == CLOSET ? "closet.php" :
			requestType == UNEQUIP_ALL ? "inv_equip.php" : "inventory.php" );

		this.requestType = requestType;
		this.outfit = null;
		this.error = null;

		// Otherwise, add the form field indicating which page
		// of the inventory you want to request

		if ( requestType == QUESTS )
			addFormField( "which", "3" );
		else if ( requestType != CLOSET )
			addFormField( "which", "2" );

		if ( requestType == UNEQUIP_ALL )
		{
			addFormField( "action", "unequipall" );
			addFormField( "pwd" );
		}
	}

	public EquipmentRequest( KoLmafia client, String outfitName )
	{
		super( client, "inv_equip.php" );
		addFormField( "which", "2" );

		addFormField( "action", "customoutfit" );
		addFormField( "outfitname", outfitName );
		requestType = SAVE_OUTFIT;
	}

	public EquipmentRequest( KoLmafia client, String change, int equipmentSlot )
	{
		super( client, "inv_equip.php" );
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

		if ( change.indexOf( "(" ) != -1 )
			change = change.substring( 0, change.indexOf( "(" ) - 1 );

		// Find out what item is being equipped
		this.itemID = TradeableItemDatabase.getItemID( change );

		// Find out what kind of item it is
		this.equipmentType = TradeableItemDatabase.getConsumptionType( itemID );

		// If unspecified slot, pick based on type of item
		if ( this.equipmentSlot == -1 )
			this.equipmentSlot = chooseEquipmentSlot();

		// Make sure you can equip it in the requested slot
		String action = getAction();
		if ( action == null )
			return;

		this.requestType = CHANGE_ITEM;
		this.changeItemName = KoLDatabase.getCanonicalName( change );

		addFormField( "action", action );
		addFormField( "whichitem", String.valueOf( itemID ) );
		addFormField( "pwd" );
	}

	public EquipmentRequest( KoLmafia client, SpecialOutfit change )
	{
		super( client, "inv_equip.php" );

		addFormField( "action", "outfit" );
		addFormField( "which", "2" );
		addFormField( "whichoutfit", String.valueOf( change.getOutfitID() ) );

		this.requestType = CHANGE_OUTFIT;
		this.outfit = change;
		this.error = null;
	}

	private String getAction()
	{
		AdventureResult item = new AdventureResult( itemID, 0 );
		if ( equipmentSlot != KoLCharacter.FAMILIAR &&
		     item.getCount( KoLCharacter.getInventory() ) == 0 )
		{
			error = "You don't have a " + item.getName();
			return null;
		}

		switch ( equipmentSlot )
		{
		case KoLCharacter.HAT:
			if ( equipmentType == ConsumeItemRequest.EQUIP_HAT )
				return "equip";
			break;

		case KoLCharacter.WEAPON:
			if ( equipmentType == ConsumeItemRequest.EQUIP_WEAPON )
			{
				if ( KoLCharacter.dualWielding() &&
				     EquipmentDatabase.isRanged( itemID ) &&
				     EquipmentDatabase.getHands( itemID ) == 1 )
				{
					error = "You can't equip a ranged weapon with a melee weapon in your off-hand.";
					return null;
				}
				return "equip";
			}
			break;

		case KoLCharacter.OFFHAND:
			if ( equipmentType == ConsumeItemRequest.EQUIP_OFFHAND )
				return "equip";

			if ( equipmentType == ConsumeItemRequest.EQUIP_WEAPON )
			{
				if ( KoLCharacter.weaponHandedness() != 1 || KoLCharacter.rangedWeapon() )
				{
					error = "You must have a 1-handed melee weapon equipped first.";
					return null;
				}
				if ( EquipmentDatabase.getHands( itemID ) > 1 )
				{
					error = "That weapon is too big to wield in your off-hand.";
					return null;
				}
				if ( EquipmentDatabase.isRanged( itemID ) )
				{
					error = "You can't wield a ranged weapon in your off-hand.";
					return null;
				}
				if ( !KoLCharacter.hasSkill( "Double-Fisted Skull Smashing" ) )
				{
					error = "You don't know how to wield two weapons.";
					return null;
				}
				return "dualwield";
			}
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
		case KoLCharacter.ACCESSORY2:
		case KoLCharacter.ACCESSORY3:
			if ( equipmentType == ConsumeItemRequest.EQUIP_ACCESSORY )
				return "equip";
			break;

		case KoLCharacter.FAMILIAR:
			if ( equipmentType == ConsumeItemRequest.EQUIP_FAMILIAR )
				return "equip";
			break;

		case -1:
			return "equip";

		default:
			error = "Internal error: bad slot (" + String.valueOf( equipmentSlot ) + ")";
			return null;
		}

		error = "You can't put your " + TradeableItemDatabase.getItemName( itemID ) + " there.";
		return null;
	}

	private int chooseEquipmentSlot()
	{
		if ( equipmentSlot != -1 )
			return equipmentSlot;

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
				(new EquipmentRequest( client, UNEQUIP_ALL )).run();

				if ( !KoLCharacter.getEquipment( KoLCharacter.FAMILIAR ).equals( UNEQUIP ) )
					(new EquipmentRequest( client, UNEQUIP, KoLCharacter.FAMILIAR )).run();

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
				// given outfit -- do this by adding all of the items
				// as conditions and then issuing a check.

				ArrayList temporaryList = new ArrayList();
				temporaryList.addAll( client.getConditions() );
				client.getConditions().clear();

				EquipmentDatabase.addOutfitConditions( outfit.getOutfitID() );
				DEFAULT_SHELL.executeConditionsCommand( "check" );
				client.getConditions().addAll( temporaryList );

				// Bail now if the conditions were not met
				if ( !KoLmafia.permitsContinue() )
					return;
			}
		}

		// If we are changing an accessory or familiar equipment, first
		// we must remove the old one in the slot.

		if ( requestType == CHANGE_ITEM )
		{
			// Do not submit a request if the item matches what you want
			// to equip on the character.

			if ( KoLCharacter.getEquipment( equipmentSlot ).indexOf( changeItemName ) != -1 )
				return;

			switch ( equipmentSlot )
			{
				case KoLCharacter.FAMILIAR:

					// If we are requesting another familiar's
					// equipment, make it available.

					AdventureResult result = new AdventureResult( itemID, 1 );
					if ( !KoLCharacter.getInventory().contains( result ) )
					{
						// Find first familiar with item
						FamiliarData [] familiars = new FamiliarData[ KoLCharacter.getFamiliarList().size() ];
						KoLCharacter.getFamiliarList().toArray( familiars );
						for ( int i = 0; i < familiars.length; ++i )
						{
							if ( familiars[i].getItem() != null && familiars[i].getItem().indexOf( changeItemName ) != -1 )
							{
								KoLmafia.updateDisplay( "Stealing " + result.getName() + " from " + familiars[i].getRace() + "..." );
								KoLRequest unequip = new KoLRequest( client, "familiar.php?pwd=&action=unequip&famid=" + familiars[i].getID(), true );
								unequip.run();

								familiars[i].setItem( UNEQUIP );
								client.processResult( result, false );

								break;
							}
						}
					}

					// Fall through

				case KoLCharacter.ACCESSORY1:
				case KoLCharacter.ACCESSORY2:
				case KoLCharacter.ACCESSORY3:

					if ( !KoLCharacter.getEquipment( equipmentSlot ).equals( UNEQUIP ) )
						(new EquipmentRequest( client, UNEQUIP, equipmentSlot )).run();

					 break;
			}
		}

		switch ( requestType )
		{
			case QUESTS:
				KoLmafia.updateDisplay( "Updating quest items..." );
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
				KoLmafia.updateDisplay( ( equipmentType == ConsumeItemRequest.EQUIP_WEAPON ? "Wielding " : "Putting on " ) + TradeableItemDatabase.getItemName( itemID ) + "..." );
				break;

			case REMOVE_ITEM:
				String item = KoLCharacter.getCurrentEquipmentName( equipmentSlot);
				if ( item == null )
					return;

				KoLmafia.updateDisplay( "Taking off " + item + "..." );
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
			KoLRequest message = new KoLRequest( client, redirectLocation );
			message.run();

			responseCode = message.responseCode;
			responseText = message.responseText;
			processResults();
		}
	}

	protected void processResults()
	{
		// Fetch updated equipment

		try
		{
			if ( requestType == CLOSET )
			{
				parseCloset();
				super.processResults();
				KoLmafia.updateDisplay( "Inventory retrieved." );
			}
			else if ( requestType == QUESTS )
			{
				parseQuestItems();
				super.processResults();
				KoLmafia.updateDisplay( "Quest item list retrieved." );
			}
			else
			{
				parseQuestItems();
				String [] oldEquipment = new String[9];
				int oldFakeHands = KoLCharacter.getFakeHands();

				// Ensure that the inventory stays up-to-date by
				// switching items around, as needed.

				for ( int i = 0; i < 9; ++i )
					oldEquipment[i] = KoLCharacter.getEquipment( i );

				parseEquipment( this.responseText );
				if ( KoLmafia.cachedLogin != null )
				{
					for ( int i = 0; i < 9; ++i )
						switchItem( oldEquipment[i], KoLCharacter.getEquipment( i ) );

					// Adjust inventory of fake hands
					int newFakeHands = KoLCharacter.getFakeHands();
					if ( oldFakeHands != newFakeHands )
						AdventureResult.addResultToList( KoLCharacter.getInventory(), new AdventureResult( FAKE_HAND, oldFakeHands - newFakeHands ) );

					CharpaneRequest.getInstance().run();
				}

				KoLCharacter.recalculateAdjustments( false );
				KoLCharacter.updateStatus();

				if ( requestType == EQUIPMENT )
					KoLmafia.updateDisplay( "Equipment updated." );
				else if ( requestType == SAVE_OUTFIT )
					KoLmafia.updateDisplay( "Outfit saved." );
				else
					KoLmafia.updateDisplay( "Gear changed." );
			}

			// After all the items have been switched,
			// update lists.

			KoLCharacter.refreshCalculatedLists();
		}
		catch ( RuntimeException e )
		{
			// This should not happen.  Therefore, print
			// a stack trace for debug purposes.

			StaticEntity.printStackTrace( e );
		}
	}

	private void switchItem( String oldItem, String newItem )
	{
		// Determine the item which is being switched
		// in and out.

		if ( !newItem.equals( UNEQUIP ) && newItem.indexOf( "(" ) != -1 )
			newItem = newItem.substring( 0, newItem.indexOf( "(" ) - 1 ).trim();

		if ( !oldItem.equals( UNEQUIP ) && oldItem.indexOf( "(" ) != -1 )
			oldItem = oldItem.substring( 0, oldItem.indexOf( "(" ) - 1 ).trim();

		int switchIn = newItem.equals( UNEQUIP ) ? -1 : TradeableItemDatabase.getItemID( newItem );
		int switchOut = oldItem.equals( UNEQUIP ) ? -1 : TradeableItemDatabase.getItemID( oldItem );

		// If the items are not equivalent, make sure
		// the items should get switched out.

		if ( switchIn != switchOut )
		{
			// Manually add/subtract items from inventory to avoid
			// excessive list updating.
			if ( switchIn != -1 )
				AdventureResult.addResultToList( KoLCharacter.getInventory(), new AdventureResult( switchIn, -1 ) );

			if ( switchOut != -1 )
				AdventureResult.addResultToList( KoLCharacter.getInventory(), new AdventureResult( switchOut, 1 ) );
		}
	}

	private void parseCloset()
	{
		// Try to find how much meat is in your character's closet -
		// this way, the program's meat manager frame auto-updates

		Matcher meatInClosetMatcher = Pattern.compile( "[\\d,]+ meat\\.</b>" ).matcher( responseText );

		if ( meatInClosetMatcher.find() )
		{
			try
			{
				String meatInCloset = meatInClosetMatcher.group();
				KoLCharacter.setClosetMeat( df.parse( meatInCloset ).intValue() );
			}
			catch ( Exception e )
			{
				// This should not happen.  Therefore, print
				// a stack trace for debug purposes.

				StaticEntity.printStackTrace( e );
			}
		}

		Matcher inventoryMatcher = Pattern.compile( "<b>Put:.*?</select>" ).matcher( responseText );
		if ( inventoryMatcher.find() )
		{
			List inventory = KoLCharacter.getInventory();
			inventory.clear();

			parseCloset( inventoryMatcher.group(), inventory, true );
		}

		Matcher closetMatcher = Pattern.compile( "<b>Take:.*?</select>" ).matcher( responseText );
		if ( closetMatcher.find() )
		{
			List closet = KoLCharacter.getCloset();
			closet.clear();
			parseCloset( closetMatcher.group(), closet, false );
		}
	}

	private void parseCloset( String content, List resultList, boolean updateUsableList )
	{
		int lastFindIndex = 0;
		Matcher optionMatcher = Pattern.compile( "<option value='([\\d]+)'>(.*?)\\(([\\d,]+)\\)" ).matcher( content );
		while ( optionMatcher.find( lastFindIndex ) )
		{
			try
			{
				lastFindIndex = optionMatcher.end();
				int itemID = df.parse( optionMatcher.group(1) ).intValue();

				if ( TradeableItemDatabase.getItemName( itemID ) == null )
					TradeableItemDatabase.registerItem( itemID, optionMatcher.group(2).trim() );

				AdventureResult result = new AdventureResult( itemID, df.parse( optionMatcher.group(3) ).intValue() );
				AdventureResult.addResultToList( resultList, result );
			}
			catch ( Exception e )
			{
				// This should not happen.  Therefore, print
				// a stack trace for debug purposes.

				StaticEntity.printStackTrace( e );
			}
		}
	}

	private void parseQuestItems()
	{
		Matcher itemMatcher = Pattern.compile( "<b>([^<]+)</b>([^<]*?)<font size=1>" ).matcher( responseText );
		while ( itemMatcher.find() )
		{
			String quantity = itemMatcher.group(2).trim();
			AdventureResult item = new AdventureResult( itemMatcher.group(1),
				quantity.length() == 0 ? 1 : Integer.parseInt( quantity.substring( 1, quantity.length() - 1 ) ) );

			if ( !KoLCharacter.getInventory().contains( item ) )
				AdventureResult.addResultToList( KoLCharacter.getInventory(), item );
		}
	}

	public static void parseEquipment( String responseText )
	{
		String [] equipment = new String[9];
		for ( int i = 0; i < equipment.length; ++i )
			equipment[i] = UNEQUIP;
		int fakeHands = 0;

		Matcher equipmentMatcher;

		if ( responseText.indexOf( "unequip&type=hat") != -1 )
		{
			 equipmentMatcher = Pattern.compile( "Hat:</td>.*?<b>(.*?)</b>.*unequip&type=hat" ).matcher( responseText );
			if ( equipmentMatcher.find() )
			{
				equipment[ KoLCharacter.HAT ] = equipmentMatcher.group(1);
				KoLmafia.getDebugStream().println( "Hat: " + equipment[ KoLCharacter.HAT ] );
			}
		}

		if ( responseText.indexOf( "unequip&type=weapon") != -1 )
		{
			equipmentMatcher = Pattern.compile( "Weapon:</td>.*?<b>(.*?)</b>.*unequip&type=weapon" ).matcher( responseText );
			if ( equipmentMatcher.find() )
			{
				equipment[ KoLCharacter.WEAPON ] = equipmentMatcher.group(1);
				KoLmafia.getDebugStream().println( "Weapon: " + equipment[ KoLCharacter.WEAPON ] );
			}
		}

		if ( responseText.indexOf( "unequip&type=offhand") != -1 )
		{
			equipmentMatcher = Pattern.compile( "Off-Hand:</td>.*?<b>([^<]*)</b> *(<font.*?/font>)?[^>]*unequip&type=offhand" ).matcher( responseText );
			if ( equipmentMatcher.find() )
			{
				equipment[ KoLCharacter.OFFHAND ] = equipmentMatcher.group(1);
				KoLmafia.getDebugStream().println( "Off-hand: " + equipment[ KoLCharacter.OFFHAND ] );
			}
		}

		if ( responseText.indexOf( "unequip&type=shirt") != -1 )
		{
			equipmentMatcher = Pattern.compile( "Shirt:</td>.*?<b>(.*?)</b>.*unequip&type=shirt" ).matcher( responseText );
			if ( equipmentMatcher.find() )
			{
				equipment[ KoLCharacter.SHIRT ] = equipmentMatcher.group(1);
				KoLmafia.getDebugStream().println( "Shirt: " + equipment[ KoLCharacter.SHIRT ] );
			}
		}

		if ( responseText.indexOf( "unequip&type=pants") != -1 )
		{
			equipmentMatcher = Pattern.compile( "Pants:</td>.*?<b>(.*?)</b>.*unequip&type=pants" ).matcher( responseText );
			if ( equipmentMatcher.find() )
			{
				equipment[ KoLCharacter.PANTS ] = equipmentMatcher.group(1);
				KoLmafia.getDebugStream().println( "Pants: " + equipment[ KoLCharacter.PANTS ] );
			}
		}

		if ( responseText.indexOf( "unequip&type=acc1") != -1 )
		{
			equipmentMatcher = Pattern.compile( "Accessory:</td>.*?<b>([^<]*?)</b> *<a href=\"inv_equip.php\\?pwd=[^&]*&which=2&action=unequip&type=acc1\">" ).matcher( responseText );
			if ( equipmentMatcher.find() )
			{
				equipment[ KoLCharacter.ACCESSORY1 ] = equipmentMatcher.group(1);
				KoLmafia.getDebugStream().println( "Accessory 1: " + equipment[ KoLCharacter.ACCESSORY1 ] );
			}
		}

		if ( responseText.indexOf( "unequip&type=acc2") != -1 )
		{
			equipmentMatcher = Pattern.compile( "Accessory:</td>.*?<b>([^<]*?)</b> *<a href=\"inv_equip.php\\?pwd=[^&]*&which=2&action=unequip&type=acc2\">" ).matcher( responseText );
			if ( equipmentMatcher.find() )
			{
				equipment[ KoLCharacter.ACCESSORY2 ] = equipmentMatcher.group(1);
				KoLmafia.getDebugStream().println( "Accessory 2: " + equipment[ KoLCharacter.ACCESSORY2 ] );
			}
		}

		if ( responseText.indexOf( "unequip&type=acc3") != -1 )
		{
			equipmentMatcher = Pattern.compile( "Accessory:</td>.*?<b>([^<]*?)</b> *<a href=\"inv_equip.php\\?pwd=[^&]*&which=2&action=unequip&type=acc3\">" ).matcher( responseText );
			if ( equipmentMatcher.find() )
			{
				equipment[ KoLCharacter.ACCESSORY3 ] = equipmentMatcher.group(1);
				KoLmafia.getDebugStream().println( "Accessory 3: " + equipment[ KoLCharacter.ACCESSORY3 ] );
			}

		}

		if ( responseText.indexOf( "unequip&type=familiarequip") != -1 )
		{
			equipmentMatcher = Pattern.compile( "Familiar:*</td>.*?<b>([^<]*?)</b>.*unequip&type=familiarequip" ).matcher( responseText );
			if ( equipmentMatcher.find() )
			{
				equipment[ KoLCharacter.FAMILIAR ] = equipmentMatcher.group(1);
				KoLmafia.getDebugStream().println( "Familiar: " + equipment[ KoLCharacter.FAMILIAR ] );
			}
		}

		int index = 0;
		while ( ( index = responseText.indexOf( "unequip&type=fakehand", index) ) != -1 )
		{
			++fakeHands;
			index += 21;
		}

		Matcher outfitsMatcher = Pattern.compile( "<select name=whichoutfit>.*?</select>" ).matcher( responseText );

		LockableListModel outfits = outfitsMatcher.find() ?
			SpecialOutfit.parseOutfits( outfitsMatcher.group() ) : null;

		KoLCharacter.setEquipment( equipment, outfits );
		KoLCharacter.setFakeHands( fakeHands );
	}

	public static int slotNumber( String name )
	{
		for ( int i = 0; i < slotNames.length; ++i )
			if ( name.equals( slotNames[i] ) )
				return i;
		return -1;
	}

	public String getCommandForm( int iterations )
	{
		String outfitName = getOutfitName();

		if ( outfitName != null )
			return "outfit " + outfitName;

		if ( requestType == REMOVE_ITEM )
			return "unequip " + slotNames[ equipmentSlot ];

		if ( requestType == CHANGE_ITEM )
		{
			if ( equipmentSlot == -1 )
				return "equip " + changeItemName;
			return "equip " + slotNames[ equipmentSlot ] + " " + changeItemName;
		}

		return "";
	}
}
