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
import java.util.StringTokenizer;

/**
 * An extension of <code>KoLRequest</code> which retrieves a list of
 * the character's equipment from the server.  At the current time,
 * there is no support for actually equipping items, so only the items
 * which are currently equipped are retrieved.
 */

public class EquipmentRequest extends KoLRequest
{
	private KoLCharacter character;

	/**
	 * Constructs a new <code>EquipmentRequest</code>, overwriting the
	 * data located in the provided character.
	 *
	 * @param	client	The client to be notified in the event of an error
	 * @param	character	The character to which this will record the retrieved equipment
	 */

	public EquipmentRequest( KoLmafia client )
	{
		// The only thing to do is to retrieve the page from
		// the client - all variable initialization comes from
		// when the request is actually run.

		super( client, "inventory.php" );
		this.character = client.getCharacterData();

		addFormField( "which", "2" );
	}

	/**
	 * Executes the <code>EquipmentRequest</code>.  Note that at the current
	 * time, only the character's currently equipped items and familiar item
	 * will be stored.
	 */

	public void run()
	{
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

		logStream.println( "Parsing equipment data..." );

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
					familiarItem = parsedContent.nextToken();
			}
			while ( !lastToken.startsWith( "Outfits:" ) );

			character.setEquipment( hat, weapon, pants, accessories[0], accessories[1], accessories[2], familiarItem );
			logStream.println( "Parsing complete." );
		}
		catch ( RuntimeException e )
		{
			logStream.println( e );
		}
	}
}