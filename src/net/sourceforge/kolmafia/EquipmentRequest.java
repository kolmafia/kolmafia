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
	public static final int CHANGE_OUTFIT = 3;

	private KoLCharacter character;
	private int requestType;
	private String outfitName;

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

		this.outfitName = null;
		if ( requestType == EQUIPMENT )
			addFormField( "which", "2" );
	}

	public EquipmentRequest( KoLmafia client, SpecialOutfit change )
	{
		super( client, "inv_equip.php" );
		addFormField( "action", "outfit" );
		addFormField( "which", "2" );
		addFormField( "whichoutfit", "" + change.getOutfitID() );
		this.requestType = CHANGE_OUTFIT;
		this.outfitName = change.toString();
	}

	public String getOutfitName()
	{	return outfitName;
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

		if ( requestType == CHANGE_OUTFIT )
			updateDisplay( DISABLED_STATE, "Changing outfit..." );

		KoLCharacter data = client.getCharacterData();

		String oldHat = data.getHat();
		String oldWeapon = data.getWeapon();
		String oldPants = data.getPants();
		String oldAccessory1 = data.getAccessory1();
		String oldAccessory2 = data.getAccessory2();
		String oldAccessory3 = data.getAccessory3();

		super.run();

		// If you changed your outfit, there will be a redirect
		// to the equipment page - therefore, do so.

		if ( requestType == CHANGE_OUTFIT )
		{
			(new EquipmentRequest( client, EQUIPMENT )).run();

			switchItem( oldHat, data.getHat() );
			switchItem( oldWeapon, data.getWeapon() );
			switchItem( oldPants, data.getPants() );
			switchItem( oldAccessory1, data.getAccessory1() );
			switchItem( oldAccessory2, data.getAccessory2() );
			switchItem( oldAccessory3, data.getAccessory3() );

			updateDisplay( ENABLED_STATE, "Equipment changed." );
			return;
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
					updateDisplay( DISABLED_STATE, "Retrieving inventory..." );
					parseCloset();
					updateDisplay( ENABLED_STATE, "Inventory retrieved." );
					break;

				case EQUIPMENT:
					updateDisplay( DISABLED_STATE, "Retrieving equipment..." );
					String plainTextContent = replyContent.replaceAll( "<.*?>", "\n" );
					StringTokenizer parsedContent = new StringTokenizer( plainTextContent, "\n" );

					parseEquipment( parsedContent );
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
			AdventureResult.addResultToList( client.getInventory(), new AdventureResult( oldItem, 1 ) );
			AdventureResult.addResultToList( client.getInventory(), new AdventureResult( newItem, -1 ) );
		}
	}

	private void parseCloset()
	{
		// Try to find how much meat is in your character's closet -
		// this way, the program's meat manager frame auto-updates

		Matcher meatInClosetMatcher = Pattern.compile( "[\\d,]+ meat\\.</b>" ).matcher( replyContent );

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

		Matcher inventoryMatcher = Pattern.compile( "<b>Put:.*?</select>" ).matcher( replyContent );
		if ( inventoryMatcher.find() )
		{
			List inventory = client.getInventory();
			inventory.clear();

			List usableItems = client.getUsableItems();
			usableItems.clear();

			parseCloset( inventoryMatcher.group(), inventory, true );
		}

		Matcher closetMatcher = Pattern.compile( "<b>Take:.*?</select>" ).matcher( replyContent );
		if ( closetMatcher.find() )
		{
			List closet = client.getCloset();
			closet.clear();
			parseCloset( closetMatcher.group(), closet, false );
		}
	}

	private void parseCloset( String content, List resultList, boolean updateUsableList )
	{
		// The next two tokens are blank space and an
		// indicator to show that the list is about
		// to start.  Skip them both.

		List usableItems = client.getUsableItems();
		int lastFindIndex = 0;

		Matcher optionMatcher = Pattern.compile( "<option value='([\\d]+)'>.*?\\(([\\d,]+)\\)" ).matcher( content );
		while ( optionMatcher.find() )
		{
			try
			{
				lastFindIndex = optionMatcher.end();
				AdventureResult result = new AdventureResult( df.parse( optionMatcher.group(1) ).intValue(), df.parse( optionMatcher.group(2) ).intValue() );
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

	private void parseEquipment( StringTokenizer parsedContent )
	{
		try
		{
			while ( !parsedContent.nextToken().startsWith( "Hat:" ) );
			String hat = parsedContent.nextToken();

			while ( !parsedContent.nextToken().startsWith( "Weapon:" ) );
			String weapon = parsedContent.nextToken();

			while ( !parsedContent.nextToken().startsWith( "Pants:" ) );
			String pants = parsedContent.nextToken();

			String [] accessories = new String[3];
			for ( int i = 0; i < 3; ++i )
				accessories[i] = "none";
			String familiarItem = "none";

			int accessoryCount = 0;
			String lastToken;

			do
			{
				lastToken = parsedContent.nextToken();

				if ( lastToken.startsWith( "Accessory:" ) )
					accessories[ accessoryCount++ ] = parsedContent.nextToken();
				else if ( lastToken.startsWith( "Familiar:" ) )
					character.setFamiliarItem( parsedContent.nextToken() );
			}
			while ( !lastToken.startsWith( "Outfit" ) && parsedContent.hasMoreTokens() );

			// Now to determine which outfits are available.  The easiest
			// way to do this is a straightforward regular expression and
			// then use the static SpecialOutfit method to determine which
			// items are available.

			Matcher outfitsMatcher = Pattern.compile( "<select name=whichoutfit>.*?</select>" ).matcher( replyContent );

			LockableListModel outfits = outfitsMatcher.find() ?
				SpecialOutfit.parseOutfits( outfitsMatcher.group() ) : new LockableListModel();

			character.setEquipment( hat, weapon, pants,
				accessories[0], accessories[1], accessories[2], outfits );
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