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

public class EquipmentRequest extends KoLRequest
{
	private static final int REQUEST_ALL = 0;

	public static final int EQUIPMENT = 1;
	public static final int CLOSET = 2;
	private static final int CHANGE_OUTFIT = 3;
	private static final int CHANGE_ITEM = 4;
	private static final int REMOVE_ACC = 5;

	private KoLCharacter character;
	private int requestType;
	private SpecialOutfit outfit;

	/**
	 * Constructs a new <code>EquipmentRequest</code>, overwriting the
	 * data located in the provided character.
	 *
	 * @param	client	The client to be notified in the event of an error
	 */

	public EquipmentRequest( KoLmafia client )
	{	this( client, REQUEST_ALL );
	}

	public EquipmentRequest( KoLmafia client, int requestType )
	{
		super( client, requestType == CLOSET ? "closet.php" : "inventory.php" );
		this.character = client.getCharacterData();
		this.requestType = requestType;

		// Otherwise, add the form field indicating which page
		// of the inventory you want to request

		this.outfit = null;
		if ( requestType == EQUIPMENT )
			addFormField( "which", "2" );
	}

	public EquipmentRequest( KoLmafia client, String change )
	{
		super( client, "inv_equip.php" );
		addFormField( "which", "2" );

		this.character = client.getCharacterData();

		if ( change.equals( "acc1" ) || change.equals( "acc2" ) || change.equals( "acc3" ) || change.equals( "familiarequip" ) )
		{
			addFormField( "action", "unequip" );
			addFormField( "type", change );
			this.requestType = REMOVE_ACC;
		}
		else if ( change.equals( "unequipall" ) )
		{
			addFormField( "action", "unequipall" );
		}
		else
		{
			addFormField( "action", "equip" );
			addFormField( "whichitem", String.valueOf( TradeableItemDatabase.getItemID( change ) ) );
			addFormField( "pwd", client.getPasswordHash() );
			this.requestType = CHANGE_ITEM;
		}
	}

	public EquipmentRequest( KoLmafia client, SpecialOutfit change )
	{
		super( client, "inv_equip.php" );

		this.character = client.getCharacterData();

		addFormField( "action", "outfit" );
		addFormField( "which", "2" );
		addFormField( "whichoutfit", String.valueOf( change.getOutfitID() ) );

		this.requestType = CHANGE_OUTFIT;
		this.outfit = change;
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
		// Outfit changes are a bit quirky, so they're handled
		// first for easy visibility.

		if ( requestType == CHANGE_OUTFIT )
		{
			// If this is a birthday suit outfit, then make sure
			// you remove everything first.

			if ( outfit == SpecialOutfit.BIRTHDAY_SUIT )
			{
				(new EquipmentRequest( client, "unequipall" )).run();
				(new EquipmentRequest( client, "familiarequip" )).run();
				return;
			}

			// Otherwise, if it's a custom outfit, you need to
			// remove all items first before continuing.

			if ( outfit.getOutfitID() < 0 )
				(new EquipmentRequest( client, "unequipall" )).run();
		}

		// If this is a request all, instantiate each of the
		// lesser requests and then return

		if ( requestType == REQUEST_ALL )
		{
			(new EquipmentRequest( client, EQUIPMENT )).run();
			(new EquipmentRequest( client, CLOSET )).run();
			return;
		}

		switch ( requestType )
		{
			case CHANGE_OUTFIT:
				updateDisplay( DISABLED_STATE, "Changing outfit..." );
				break;

			case CLOSET:
				updateDisplay( DISABLED_STATE, "Refreshing closet..." );
				break;

			case REMOVE_ACC:
				updateDisplay( DISABLED_STATE, "Removing accessory..." );
				break;

			default:
				updateDisplay( DISABLED_STATE, "Updating equipment..." );
				break;
		}


		super.run();

		// If you changed your outfit, there will be a redirect
		// to the equipment page - therefore, process it.

		if ( !isErrorState && requestType != CLOSET && requestType != EQUIPMENT )
		{
			updateDisplay( DISABLED_STATE, "Updating equipment..." );
			KoLRequest message = new KoLRequest( client, redirectLocation );
			message.run();

			responseCode = message.responseCode;
			responseText = message.responseText;
		}

		// If an error state occurred, return from this
		// request, since there's no content to parse

		if ( isErrorState || responseCode != 200 )
			return;

		// The easiest way to retrieve the character sheet
		// data is to first strip all of the HTML from the
		// reply, and then tokenize on the stripped-down
		// version.  This can be done through simple regular
		// expression matching.

		try
		{
			logStream.println( "Parsing data..." );
			switch ( requestType )
			{
				case CLOSET:
					parseCloset();
					updateDisplay( NOCHANGE, "Inventory retrieved." );
					break;

				case REMOVE_ACC:
				case CHANGE_ITEM:
				case EQUIPMENT:

					String oldHat = character.getHat();
 					String oldWeapon = character.getWeapon();
 					String oldPants = character.getPants();
 					String oldShirt = character.getShirt();
 					String oldAccessory1 = character.getAccessory1();
 					String oldAccessory2 = character.getAccessory2();
 					String oldAccessory3 = character.getAccessory3();
 					String oldFamiliarItem = character.getFamiliarItem();

					parseEquipment();

					switchItem( oldHat, character.getHat() );
					switchItem( oldWeapon, character.getWeapon() );
					switchItem( oldPants, character.getPants() );
					switchItem( oldShirt, character.getShirt() );
					switchItem( oldAccessory1, character.getAccessory1() );
					switchItem( oldAccessory2, character.getAccessory2() );
					switchItem( oldAccessory3, character.getAccessory3() );
					switchItem( oldFamiliarItem, character.getFamiliarItem() );

					updateDisplay( ENABLED_STATE, "Equipment retrieved." );
					break;
			}

			logStream.println( "Parsing complete." );
		}
		catch ( RuntimeException e )
		{
			logStream.println( e );
			e.printStackTrace( logStream );
		}
	}

	private void switchItem( String oldItem, String newItem )
	{
		if ( !oldItem.equals( newItem ) )
		{
			if ( !oldItem.equals( "none" ) )
				AdventureResult.addResultToList( client.getInventory(), new AdventureResult( oldItem, 1 ) );

			if ( !newItem.equals( "none" ) )
				AdventureResult.addResultToList( client.getInventory(), new AdventureResult( newItem, -1 ) );
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
				client.getCharacterData().setClosetMeat( df.parse( meatInCloset ).intValue() );
			}
			catch ( Exception e )
			{
			}
		}

		Matcher inventoryMatcher = Pattern.compile( "<b>Put:.*?</select>" ).matcher( responseText );
		if ( inventoryMatcher.find() )
		{
			List inventory = client.getInventory();
			inventory.clear();

			List usableItems = client.getUsableItems();
			usableItems.clear();

			parseCloset( inventoryMatcher.group(), inventory, true );
		}

		Matcher closetMatcher = Pattern.compile( "<b>Take:.*?</select>" ).matcher( responseText );
		if ( closetMatcher.find() )
		{
			List closet = client.getCloset();
			closet.clear();
			parseCloset( closetMatcher.group(), closet, false );
		}
	}

	private void parseCloset( String content, List resultList, boolean updateUsableList )
	{
		List usableItems = client.getUsableItems();
		int lastFindIndex = 0;

		Matcher optionMatcher = Pattern.compile( "<option value='([\\d]+)'>(.*?)\\(([\\d,]+)\\)" ).matcher( content );
		while ( optionMatcher.find( lastFindIndex ) )
		{
			try
			{
				lastFindIndex = optionMatcher.end();
				int itemID = df.parse( optionMatcher.group(1) ).intValue();

				if ( TradeableItemDatabase.getItemName( itemID ) == null )
					TradeableItemDatabase.registerItem( client, itemID, optionMatcher.group(2).trim() );

				AdventureResult result = new AdventureResult( itemID, df.parse( optionMatcher.group(3) ).intValue() );
				AdventureResult.addResultToList( resultList, result );

				if ( TradeableItemDatabase.isUsable( result.getName() ) && updateUsableList )
					AdventureResult.addResultToList( usableItems, result );
			}
			catch ( Exception e )
			{
				// If an exception occurs during the parsing, just
				// continue after notifying the LogStream of the
				// error.  This could be handled better, but not now.

				logStream.println( e );
				e.printStackTrace( logStream );
			}
		}
	}

	private void parseEquipment()
	{
		String [] equipment = new String[8];
		for ( int i = 0; i < equipment.length; ++i )
			equipment[i] = "none";

		Matcher equipmentMatcher = Pattern.compile( "Hat:</td>.*?<b>(.*?)</b>" ).matcher( responseText );
		if ( equipmentMatcher.find() )
		{
			equipment[ KoLCharacter.HAT ] = equipmentMatcher.group(1);
			logStream.println( "Hat: " + equipment[ KoLCharacter.HAT ] );
		}

		equipmentMatcher = Pattern.compile( "Weapon:</td>.*?<b>(.*?)</b>" ).matcher( responseText );
		if ( equipmentMatcher.find() )
		{
			equipment[ KoLCharacter.WEAPON ] = equipmentMatcher.group(1);
			logStream.println( "Weapon: " + equipment[ KoLCharacter.WEAPON ] );
		}

		equipmentMatcher = Pattern.compile( "Shirt:</td>.*?<b>(.*?)</b>" ).matcher( responseText );
		if ( equipmentMatcher.find() )
		{
			equipment[ KoLCharacter.SHIRT ] = equipmentMatcher.group(1);
			logStream.println( "Shirt: " + equipment[ KoLCharacter.SHIRT ] );
		}

		equipmentMatcher = Pattern.compile( "Pants:</td>.*?<b>(.*?)</b>" ).matcher( responseText );
		if ( equipmentMatcher.find() )
		{
			equipment[ KoLCharacter.PANTS ] = equipmentMatcher.group(1);
			logStream.println( "Pants: " + equipment[ KoLCharacter.PANTS ] );
		}

		equipmentMatcher = Pattern.compile( "Accessory:</td>.*?<b>([^<]*?)</b> <a href=\"inv_equip.php\\?which=2&action=unequip&type=acc1\">" ).matcher( responseText );
		if ( equipmentMatcher.find() )
		{
			equipment[ KoLCharacter.ACCESSORY1 ] = equipmentMatcher.group(1);
			logStream.println( "Accessory 1: " + equipment[ KoLCharacter.ACCESSORY1 ] );
		}

		equipmentMatcher = Pattern.compile( "Accessory:</td>.*?<b>([^<]*?)</b> <a href=\"inv_equip.php\\?which=2&action=unequip&type=acc2\">" ).matcher( responseText );
		if ( equipmentMatcher.find() )
		{
			equipment[ KoLCharacter.ACCESSORY2 ] = equipmentMatcher.group(1);
			logStream.println( "Accessory 2: " + equipment[ KoLCharacter.ACCESSORY2 ] );
		}

		equipmentMatcher = Pattern.compile( "Accessory:</td>.*?<b>([^<]*?)</b> <a href=\"inv_equip.php\\?which=2&action=unequip&type=acc3\">" ).matcher( responseText );
		if ( equipmentMatcher.find() )
		{
			equipment[ KoLCharacter.ACCESSORY3 ] = equipmentMatcher.group(1);
			logStream.println( "Accessory 3: " + equipment[ KoLCharacter.ACCESSORY3 ] );
		}

		Matcher outfitsMatcher = Pattern.compile( "<select name=whichoutfit>.*?</select>" ).matcher( responseText );

		LockableListModel outfits = outfitsMatcher.find() ?
			SpecialOutfit.parseOutfits( outfitsMatcher.group() ) : new LockableListModel();

		character.setEquipment( equipment, outfits );
	}
}
