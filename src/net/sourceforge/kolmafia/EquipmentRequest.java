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
	public static final String UNEQUIP = "(none)";

	private static final int REQUEST_ALL = 0;

	public static final int EQUIPMENT = 1;
	public static final int CLOSET = 2;
	public static final int UNEQUIP_ALL = 6;

	private static final int CHANGE_OUTFIT = 3;
	private static final int CHANGE_ITEM = 4;
	private static final int REMOVE_ITEM = 5;

	// Array indexed by equipment "equipmentSlot" from KolCharacter
	//
	// Perhaps this should be in that module, except this is closely tied
	// to the PHP files that are manipulated by THIS module.

	public static final String [] equipmentType =
	{
		"hat", "weapon", "shirt", "pants",
		"acc1", "acc2", "acc3", "familiarequip"
	};

	private KoLCharacter character;
	private int requestType;
	private int equipmentSlot;
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
		super( client, requestType == CLOSET ? "closet.php" : requestType == UNEQUIP_ALL ? "inv_equip.php" : "inventory.php" );
		this.character = client.getCharacterData();
		this.requestType = requestType;
		this.outfit = null;

		// Otherwise, add the form field indicating which page
		// of the inventory you want to request

		if ( requestType != CLOSET )
			addFormField( "which", "2" );

		if ( requestType == UNEQUIP_ALL )
			addFormField( "action", "unequipall" );
	}

	public EquipmentRequest( KoLmafia client, String change )
	{	this( client, change, -1 );
	}

	public EquipmentRequest( KoLmafia client, String change, Integer equipmentSlot )
	{	this( client, change, equipmentSlot.intValue() );
	}

	public EquipmentRequest( KoLmafia client, String change, int equipmentSlot )
	{
		super( client, "inv_equip.php" );
		addFormField( "which", "2" );

		this.character = client.getCharacterData();
		this.equipmentSlot = equipmentSlot;

		if ( change.equals( UNEQUIP ) )
		{
			addFormField( "action", "unequip" );
			addFormField( "type", equipmentType[ equipmentSlot ] );
			this.requestType = REMOVE_ITEM;
		}
		else
		{
			String itemName = change;

			if ( change.indexOf( "(" ) != -1 )
				itemName = change.substring( 0, change.indexOf( "(" ) - 1 );

			addFormField( "action", "equip" );
			addFormField( "whichitem", String.valueOf( TradeableItemDatabase.getItemID( itemName ) ) );
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
		// If this is a request all, instantiate each of the
		// lesser requests and then return

		if ( requestType == REQUEST_ALL )
		{
			(new EquipmentRequest( client, EQUIPMENT )).run();
			(new EquipmentRequest( client, CLOSET )).run();
			return;
		}

		// Outfit changes are a bit quirky, so they're handled
		// first for easy visibility.

		if ( requestType == CHANGE_OUTFIT )
		{
			// If this is a birthday suit outfit, then make sure
			// you remove everything first.

			if ( outfit == SpecialOutfit.BIRTHDAY_SUIT )
			{
				(new EquipmentRequest( client, UNEQUIP_ALL )).run();
				(new EquipmentRequest( client, UNEQUIP, KoLCharacter.FAMILIAR )).run();

				return;
			}
		}

		// If we are changing an accessory or familiar equipment, first
		// we must remove the old one in the slot.

		if ( requestType == CHANGE_ITEM )
		{
			switch ( equipmentSlot )
			{
				case KoLCharacter.ACCESSORY1:
				case KoLCharacter.ACCESSORY2:
				case KoLCharacter.ACCESSORY3:
				case KoLCharacter.FAMILIAR:

					// To avoid equipment requests failing after quick login,
					// unconditionally issue the unequip request if all slots
					// are empty, even if the current slot is empty.

					boolean requiresUnequip = true;
					for ( int i = 0; i < 8; ++i )
						if ( !character.getEquipment(i).equals( UNEQUIP ) )
							requiresUnequip = false;

					requiresUnequip |= !character.getEquipment( equipmentSlot ).equals( UNEQUIP );

					if ( requiresUnequip )
						(new EquipmentRequest( client, UNEQUIP, equipmentSlot )).run();

					 break;
			}
		}

		switch ( requestType )
		{
			case CHANGE_OUTFIT:
				updateDisplay( DISABLED_STATE, "Changing outfit..." );
				break;

			case CLOSET:
				updateDisplay( DISABLED_STATE, "Refreshing closet..." );
				break;

			case REMOVE_ITEM:
				updateDisplay( DISABLED_STATE, "Removing item..." );
				break;

			default:
				updateDisplay( DISABLED_STATE, "Updating equipment..." );
				break;
		}

		super.run();

		// If you changed your outfit, there will be a redirect
		// to the equipment page - therefore, process it.

		if ( !isErrorState && responseCode == 302 )
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

		// Fetch updated equipment

		try
		{
			logStream.println( "Parsing data..." );

			if ( requestType == CLOSET )
			{
				parseCloset();
				updateDisplay( NOCHANGE, "Inventory retrieved." );
			}
			else
			{
				String [] oldEquipment = new String[8];

				for ( int i = 0; i < 8; ++i )
					oldEquipment[i] = character.getEquipment( i );
				parseEquipment();

				for ( int i = 0; i < 8; ++i )
					switchItem( oldEquipment[i], character.getEquipment( i ));

				updateDisplay( ENABLED_STATE, "Equipment retrieved." );
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
			if ( !oldItem.equals( UNEQUIP ) )
				AdventureResult.addResultToList( client.getInventory(), new AdventureResult( oldItem, 1 ) );

			if ( !newItem.equals( UNEQUIP ) )
				AdventureResult.addResultToList( client.getInventory(), new AdventureResult( newItem, -1 ) );
		}
	}

	private void parseCloset()
	{
		if ( client instanceof KoLmafiaGUI )
			((KoLmafiaGUI)client).shouldRefresh = false;

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

		if ( client instanceof KoLmafiaGUI )
		{
			((KoLmafiaGUI)client).shouldRefresh = true;
			((KoLmafiaGUI)client).refresher.run();
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
		Matcher equipmentMatcher;

		for ( int i = 0; i < equipment.length; ++i )
			equipment[i] = UNEQUIP;

		if ( responseText.indexOf( "unequip&type=hat") != -1 )
		{
			 equipmentMatcher = Pattern.compile( "Hat:</td>.*?<b>(.*?)</b>.*unequip&type=hat" ).matcher( responseText );
			if ( equipmentMatcher.find() )
			{
				equipment[ KoLCharacter.HAT ] = equipmentMatcher.group(1);
				logStream.println( "Hat: " + equipment[ KoLCharacter.HAT ] );
			}
		}

		if ( responseText.indexOf( "unequip&type=weapon") != -1 )
		{
			equipmentMatcher = Pattern.compile( "Weapon:</td>.*?<b>(.*?)</b>.*unequip&type=weapon" ).matcher( responseText );
			if ( equipmentMatcher.find() )
			{
				equipment[ KoLCharacter.WEAPON ] = equipmentMatcher.group(1);
				logStream.println( "Weapon: " + equipment[ KoLCharacter.WEAPON ] );
			}
		}

		if ( responseText.indexOf( "unequip&type=shirt") != -1 )
		{
			equipmentMatcher = Pattern.compile( "Shirt:</td>.*?<b>(.*?)</b>.*unequip&type=shirt" ).matcher( responseText );
			if ( equipmentMatcher.find() )
			{
				equipment[ KoLCharacter.SHIRT ] = equipmentMatcher.group(1);
				logStream.println( "Shirt: " + equipment[ KoLCharacter.SHIRT ] );
			}
		}

		if ( responseText.indexOf( "unequip&type=pants") != -1 )
		{
			equipmentMatcher = Pattern.compile( "Pants:</td>.*?<b>(.*?)</b>.*unequip&type=pants" ).matcher( responseText );
			if ( equipmentMatcher.find() )
			{
				equipment[ KoLCharacter.PANTS ] = equipmentMatcher.group(1);
				logStream.println( "Pants: " + equipment[ KoLCharacter.PANTS ] );
			}
		}

		if ( responseText.indexOf( "unequip&type=acc1") != -1 )
		{
			equipmentMatcher = Pattern.compile( "Accessory:</td>.*?<b>([^<]*?)</b> *<a href=\"inv_equip.php\\?which=2&action=unequip&type=acc1\">" ).matcher( responseText );
			if ( equipmentMatcher.find() )
			{
				equipment[ KoLCharacter.ACCESSORY1 ] = equipmentMatcher.group(1);
				logStream.println( "Accessory 1: " + equipment[ KoLCharacter.ACCESSORY1 ] );
			}
		}

		if ( responseText.indexOf( "unequip&type=acc2") != -1 )
		{
			equipmentMatcher = Pattern.compile( "Accessory:</td>.*?<b>([^<]*?)</b> *<a href=\"inv_equip.php\\?which=2&action=unequip&type=acc2\">" ).matcher( responseText );
			if ( equipmentMatcher.find() )
			{
				equipment[ KoLCharacter.ACCESSORY2 ] = equipmentMatcher.group(1);
				logStream.println( "Accessory 2: " + equipment[ KoLCharacter.ACCESSORY2 ] );
			}
		}

		if ( responseText.indexOf( "unequip&type=acc3") != -1 )
		{
			equipmentMatcher = Pattern.compile( "Accessory:</td>.*?<b>([^<]*?)</b> *<a href=\"inv_equip.php\\?which=2&action=unequip&type=acc3\">" ).matcher( responseText );
			if ( equipmentMatcher.find() )
			{
				equipment[ KoLCharacter.ACCESSORY3 ] = equipmentMatcher.group(1);
				logStream.println( "Accessory 3: " + equipment[ KoLCharacter.ACCESSORY3 ] );
			}

		}

		if ( responseText.indexOf( "unequip&type=familiarequip") != -1 )
		{
			equipmentMatcher = Pattern.compile( "Familiar:*</td>.*?<b>([^<]*?)</b>.*unequip&type=familiarequip" ).matcher( responseText );
			if ( equipmentMatcher.find() )
			{
				equipment[ KoLCharacter.FAMILIAR ] = equipmentMatcher.group(1);
				logStream.println( "Familiar: " + equipment[ KoLCharacter.FAMILIAR ] );
			}
		}

		Matcher outfitsMatcher = Pattern.compile( "<select name=whichoutfit>.*?</select>" ).matcher( responseText );

		LockableListModel outfits = outfitsMatcher.find() ?
			SpecialOutfit.parseOutfits( outfitsMatcher.group() ) : new LockableListModel();

		character.setEquipment( equipment, outfits );
	}
}
