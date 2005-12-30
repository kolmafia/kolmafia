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

public class EquipmentRequest extends PasswordHashRequest
{
	public static final String UNEQUIP = "(none)";

	public static final int EQUIPMENT = 1;
	public static final int CLOSET = 2;
	private static final int CHANGE_OUTFIT = 3;
	private static final int CHANGE_ITEM = 4;
	private static final int REMOVE_ITEM = 5;
	public static final int UNEQUIP_ALL = 6;

	// Array indexed by equipment "slot" from KoLCharacter
	//
	// Perhaps this should be in that module, except this is closely tied
	// to the PHP files that are manipulated by THIS module.

	public static final String [] equipmentType =
	{
		"hat", "weapon", "offhand", "shirt", "pants",
		"acc1", "acc2", "acc3", "familiarequip"
	};

	private int requestType;
	private int equipmentSlot;
	private String changeItemName;
	private SpecialOutfit outfit;

	public EquipmentRequest( KoLmafia client, int requestType )
	{
		super( client, requestType == CLOSET ? "closet.php" : requestType == UNEQUIP_ALL ? "inv_equip.php" : "inventory.php" );
		this.requestType = requestType;
		this.outfit = null;

		// Otherwise, add the form field indicating which page
		// of the inventory you want to request

		if ( requestType != CLOSET )
			addFormField( "which", "2" );

		if ( requestType == UNEQUIP_ALL )
		{
			addFormField( "action", "unequipall" );
			addFormField( "pwd", client.getPasswordHash() );
		}
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
		this.equipmentSlot = equipmentSlot;

		if ( change.equals( UNEQUIP ) )
		{
			addFormField( "action", "unequip" );
			addFormField( "type", equipmentType[ equipmentSlot ] );
			addFormField( "pwd", client.getPasswordHash() );
			this.requestType = REMOVE_ITEM;
		}
		else
		{
			if ( change.indexOf( "(" ) != -1 )
				change = change.substring( 0, change.indexOf( "(" ) - 1 );
                        changeItemName = change;

			addFormField( "action", "equip" );
			addFormField( "whichitem", String.valueOf( TradeableItemDatabase.getItemID( change ) ) );
			addFormField( "pwd", client.getPasswordHash() );
			this.requestType = CHANGE_ITEM;
		}
	}

	public EquipmentRequest( KoLmafia client, SpecialOutfit change )
	{
		super( client, "inv_equip.php" );

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
				(new EquipmentRequest( client, UNEQUIP_ALL )).run();

				if ( !KoLCharacter.getEquipment( KoLCharacter.FAMILIAR ).equals( UNEQUIP ) )
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
				case KoLCharacter.FAMILIAR:

					// If we are requesting another familiar's
					// equipment, make it available.

					AdventureResult result = new AdventureResult( changeItemName, 0 );
					if ( !KoLCharacter.getInventory().contains( result ) )
					{
						// Find first familiar with item
						LockableListModel familiars = KoLCharacter.getFamiliarList();
						for ( int i = 0; i < familiars.size(); ++i )
						{
							FamiliarData familiar = (FamiliarData)familiars.get(i);
							String item = familiar.getItem();
							if ( item != null && item.equals(changeItemName) )
							{
								FamiliarData currentFamiliar = KoLCharacter.getFamiliar();
								// Switch to it
								(new FamiliarRequest( client, familiar )).run();

								// Unequip item
								(new EquipmentRequest( client, UNEQUIP, KoLCharacter.FAMILIAR )).run();

								// Equip original familiar
								(new FamiliarRequest( client, currentFamiliar )).run();
								break;
							}
						}
					}

					// Fall through

				case KoLCharacter.ACCESSORY1:
				case KoLCharacter.ACCESSORY2:
				case KoLCharacter.ACCESSORY3:

					if ( !KoLCharacter.getEquipment(equipmentSlot).equals( UNEQUIP ) )
						(new EquipmentRequest( client, UNEQUIP, equipmentSlot )).run();

					 break;
			}
		}

		switch ( requestType )
		{
			case EQUIPMENT:
				updateDisplay( DISABLE_STATE, "Updating equipment..." );
				break;

			case CLOSET:
				updateDisplay( DISABLE_STATE, "Refreshing closet..." );
				break;

			case CHANGE_OUTFIT:
				updateDisplay( DISABLE_STATE, "Putting on " + outfit + "..." );
				break;

			case CHANGE_ITEM:
				updateDisplay( DISABLE_STATE, "Putting on " + changeItemName + "..." );
				break;

			case REMOVE_ITEM:
				updateDisplay( DISABLE_STATE, "Taking off " + KoLCharacter.getCurrentEquipmentName( equipmentSlot) + "..." );
				break;

			case UNEQUIP_ALL:
				updateDisplay( DISABLE_STATE, "Taking off everything..." );
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
		}

		// If an error state occurred, return from this
		// request, since there's no content to parse

		if ( responseCode != 200 )
			return;

		// Fetch updated equipment

		try
		{
			KoLmafia.getLogStream().println( "Parsing data..." );

			if ( requestType == CLOSET )
			{
				parseCloset();
				updateDisplay( NORMAL_STATE, "Inventory retrieved." );
			}
			else
			{
				String [] oldEquipment = new String[9];

				// Ensure that the inventory stays up-to-date by
				// switching items around, as needed.

				for ( int i = 0; i < 9; ++i )
					oldEquipment[i] = KoLCharacter.getEquipment( i );

				parseEquipment( this.responseText );

				for ( int i = 0; i < 9; ++i )
					switchItem( oldEquipment[i], KoLCharacter.getEquipment( i ) );
			}

			KoLmafia.getLogStream().println( "Parsing complete." );
		}
		catch ( RuntimeException e )
		{
			KoLmafia.getLogStream().println( e );
			e.printStackTrace( KoLmafia.getLogStream() );
		}
	}

	private void switchItem( String oldItem, String newItem )
	{
		if ( client.inLoginState() )
			return;

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
			if ( switchIn != -1 )
				client.processResult( new AdventureResult( switchIn, -1 ), false );

			if ( switchOut != -1 )
				client.processResult( new AdventureResult( switchOut, 1 ), false );
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
				// If an exception occurs during the parsing, just
				// continue after notifying the KoLmafia.getLogStream() of the
				// error.  This could be handled better, but not now.

				KoLmafia.getLogStream().println( e );
				e.printStackTrace( KoLmafia.getLogStream() );
			}
		}
	}

	public static void parseEquipment( String responseText )
	{
		String [] equipment = new String[9];
		Matcher equipmentMatcher;

		for ( int i = 0; i < equipment.length; ++i )
			equipment[i] = UNEQUIP;

		if ( responseText.indexOf( "unequip&type=hat") != -1 )
		{
			 equipmentMatcher = Pattern.compile( "Hat:</td>.*?<b>(.*?)</b>.*unequip&type=hat" ).matcher( responseText );
			if ( equipmentMatcher.find() )
			{
				equipment[ KoLCharacter.HAT ] = equipmentMatcher.group(1);
				KoLmafia.getLogStream().println( "Hat: " + equipment[ KoLCharacter.HAT ] );
			}
		}

		if ( responseText.indexOf( "unequip&type=weapon") != -1 )
		{
			equipmentMatcher = Pattern.compile( "Weapon:</td>.*?<b>(.*?)</b>.*unequip&type=weapon" ).matcher( responseText );
			if ( equipmentMatcher.find() )
			{
				equipment[ KoLCharacter.WEAPON ] = equipmentMatcher.group(1);
				KoLmafia.getLogStream().println( "Weapon: " + equipment[ KoLCharacter.WEAPON ] );
			}
		}

		if ( responseText.indexOf( "unequip&type=offhand") != -1 )
		{
			equipmentMatcher = Pattern.compile( "Off-Hand:</td>.*?<b>(.*?)</b>.*unequip&type=offhand" ).matcher( responseText );
			if ( equipmentMatcher.find() )
			{
				equipment[ KoLCharacter.OFFHAND ] = equipmentMatcher.group(1);
				KoLmafia.getLogStream().println( "Off-hand: " + equipment[ KoLCharacter.OFFHAND ] );
			}
		}

		if ( responseText.indexOf( "unequip&type=shirt") != -1 )
		{
			equipmentMatcher = Pattern.compile( "Shirt:</td>.*?<b>(.*?)</b>.*unequip&type=shirt" ).matcher( responseText );
			if ( equipmentMatcher.find() )
			{
				equipment[ KoLCharacter.SHIRT ] = equipmentMatcher.group(1);
				KoLmafia.getLogStream().println( "Shirt: " + equipment[ KoLCharacter.SHIRT ] );
			}
		}

		if ( responseText.indexOf( "unequip&type=pants") != -1 )
		{
			equipmentMatcher = Pattern.compile( "Pants:</td>.*?<b>(.*?)</b>.*unequip&type=pants" ).matcher( responseText );
			if ( equipmentMatcher.find() )
			{
				equipment[ KoLCharacter.PANTS ] = equipmentMatcher.group(1);
				KoLmafia.getLogStream().println( "Pants: " + equipment[ KoLCharacter.PANTS ] );
			}
		}

		if ( responseText.indexOf( "unequip&type=acc1") != -1 )
		{
			equipmentMatcher = Pattern.compile( "Accessory:</td>.*?<b>([^<]*?)</b> *<a href=\"inv_equip.php\\?pwd=[^&]*&which=2&action=unequip&type=acc1\">" ).matcher( responseText );
			if ( equipmentMatcher.find() )
			{
				equipment[ KoLCharacter.ACCESSORY1 ] = equipmentMatcher.group(1);
				KoLmafia.getLogStream().println( "Accessory 1: " + equipment[ KoLCharacter.ACCESSORY1 ] );
			}
		}

		if ( responseText.indexOf( "unequip&type=acc2") != -1 )
		{
			equipmentMatcher = Pattern.compile( "Accessory:</td>.*?<b>([^<]*?)</b> *<a href=\"inv_equip.php\\?pwd=[^&]*&which=2&action=unequip&type=acc2\">" ).matcher( responseText );
			if ( equipmentMatcher.find() )
			{
				equipment[ KoLCharacter.ACCESSORY2 ] = equipmentMatcher.group(1);
				KoLmafia.getLogStream().println( "Accessory 2: " + equipment[ KoLCharacter.ACCESSORY2 ] );
			}
		}

		if ( responseText.indexOf( "unequip&type=acc3") != -1 )
		{
			equipmentMatcher = Pattern.compile( "Accessory:</td>.*?<b>([^<]*?)</b> *<a href=\"inv_equip.php\\?pwd=[^&]*&which=2&action=unequip&type=acc3\">" ).matcher( responseText );
			if ( equipmentMatcher.find() )
			{
				equipment[ KoLCharacter.ACCESSORY3 ] = equipmentMatcher.group(1);
				KoLmafia.getLogStream().println( "Accessory 3: " + equipment[ KoLCharacter.ACCESSORY3 ] );
			}

		}

		if ( responseText.indexOf( "unequip&type=familiarequip") != -1 )
		{
			equipmentMatcher = Pattern.compile( "Familiar:*</td>.*?<b>([^<]*?)</b>.*unequip&type=familiarequip" ).matcher( responseText );
			if ( equipmentMatcher.find() )
			{
				equipment[ KoLCharacter.FAMILIAR ] = equipmentMatcher.group(1);
				KoLmafia.getLogStream().println( "Familiar: " + equipment[ KoLCharacter.FAMILIAR ] );
			}
		}

		Matcher outfitsMatcher = Pattern.compile( "<select name=whichoutfit>.*?</select>" ).matcher( responseText );

		LockableListModel outfits = outfitsMatcher.find() ?
			SpecialOutfit.parseOutfits( outfitsMatcher.group() ) : new LockableListModel();

		KoLCharacter.setEquipment( equipment, outfits );
	}

	public String getCommandForm( int iterations )
	{
		String outfitName = getOutfitName();
		if ( outfitName != null )
			return "outfit " + outfitName;

		return "";
	}
}
