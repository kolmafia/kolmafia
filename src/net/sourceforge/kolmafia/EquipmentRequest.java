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

/**
 * An extension of <code>KoLRequest</code> which retrieves a list of
 * the character's equipment from the server.  At the current time,
 * there is no support for actually equipping items, so only the items
 * which are currently equipped are retrieved.
 */

public class EquipmentRequest extends KoLRequest
{
	private static final int REQUEST_ALL = 0;
	public static final int EQUIPMENT = 2;
	public static final int CLOSET = 4;

	private KoLCharacter character;
	private int requestType;

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

		// Otherwise, add the form field indicating which page
		// of the inventory you want to request

		if ( requestType != CLOSET )
			addFormField( "which", "" + requestType );

		super.run();

		// If an error state occurred, return from this
		// request, since there's no content to parse

		if ( isErrorState || responseCode != 200 )
			return;

		// The easiest way to retrieve the character sheet
		// data is to first strip all of the HTML from the
		// reply, and then tokenize on the stripped-down
		// version.  This can be done through simple regular
		// expression matching.

		String plainTextContent = replyContent.replaceAll( "<.*?>", "\n" );
		StringTokenizer parsedContent = new StringTokenizer( plainTextContent, "\n" );

		logStream.println( "Parsing data..." );

		try
		{
			switch ( requestType )
			{
				case CLOSET:
					parseCloset( parsedContent );

				case EQUIPMENT:
					parseEquipment( parsedContent );
					break;
			}

		}
		catch ( RuntimeException e )
		{
			logStream.println( e );
		}
	}

	private void parseCloset( StringTokenizer parsedContent )
	{
		// The inventory officially starts when you see the
		// token that starts with "Put"; therefore, continue
		// skipping tokens until that token is encountered.

		if ( replyContent.indexOf( "You have no items in your inventory." ) == -1 )
		{
			while ( !parsedContent.nextToken().startsWith( "Put:" ) );
			List inventory = client.getInventory();
			inventory.clear();
			inventory.add( AdventureResult.LAST_ELEMENT );

			List usableItems = client.getUsableItems();
			usableItems.clear();
			usableItems.add( AdventureResult.LAST_ELEMENT );

			parseCloset( parsedContent, inventory, true );
		}

		// The closet officially starts when you see the token
		// "Take:"; therefore, continue skipping tokens until
		// that token is encountered.

		while ( parsedContent.hasMoreTokens() &&
			!parsedContent.nextToken().startsWith( "Take:" ) );

		if ( parsedContent.hasMoreTokens() )
		{
			List closet = client.getCloset();
			closet.clear();
			closet.add( AdventureResult.LAST_ELEMENT );
			parseCloset( parsedContent, closet, false );
		}
	}

	private void parseCloset( StringTokenizer parsedContent, List resultList, boolean updateUsableList )
	{
		// The next two tokens are blank space and an
		// indicator to show that the list is about
		// to start.  Skip them both.

		List usableItems = client.getUsableItems();
		skipTokens( parsedContent, 2 );
		String lastToken;

		try
		{
			do
			{
				// Make sure to only add the result if it exists
				// in the item database; otherwise, it could cause
				// problems when you're moving items around.

				lastToken = parsedContent.nextToken();
				AdventureResult result = AdventureResult.parseResult( lastToken );

				if ( TradeableItemDatabase.contains( result.getName() ) )
				{
					AdventureResult.addResultToList( resultList, result );
					if ( updateUsableList && TradeableItemDatabase.isUsable( result.getName() ) )
						usableItems.add( result );
				}
			}
			while ( lastToken.trim().length() != 0 );
		}
		catch ( Exception e )
		{
			// If an exception occurs during the parsing, just
			// continue after notifying the LogStream of the
			// error.  This could be handled better, but not now.

			logStream.println( e );
		}
	}

	private void parseEquipment( StringTokenizer parsedContent )
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
				familiarItem = parsedContent.nextToken();
		}
		while ( !lastToken.startsWith( "Outfit" ) );

		character.setEquipment( hat, weapon, pants, accessories[0], accessories[1], accessories[2], familiarItem );

		// Now that the equipped items have been parsed, begin parsing for the items
		// which are not currently equipped, but are listed in the equipment page.
		// Normally, custom outfits would be parsed; for now, this part is skipped.

		logStream.println( "Parsing complete." );
	}
}