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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * An extension of the generic <code>KoLRequest</code> class which handles
 * adventures involving trading with the hermit.
 */

public class HermitRequest extends KoLRequest
{
	private static final Pattern AVAILABLE_PATTERN = Pattern.compile( "<tr><td.*?><input.*?value=(\\d*)>.*?<b>(.*?)</b></td></tr>" );

	private static final AdventureResult PERMIT =  new AdventureResult( 42, 0 );
	public static final AdventureResult TRINKET = new AdventureResult( 43, 0 );
	public static final AdventureResult GEWGAW = new AdventureResult( 44, 0 );
	public static final AdventureResult KNICK_KNACK =  new AdventureResult( 45, 0 );

	private int itemID, quantity;

	/**
	 * Constructs a new <code>HermitRequest</code>.  Note that in order
	 * for the hermit request to successfully run after creation, there
	 * must be <code>KoLSettings</code> specifying the trade that takes
	 * place.
	 *
	 * @param	client	The client to which this request will report errors/results
	 */

	public HermitRequest( KoLmafia client )
	{
		super( client, "hermit.php" );

		this.itemID = -1;
		this.quantity = 0;
	}

	public HermitRequest( KoLmafia client, int itemID, int quantity )
	{
		super( client, "hermit.php" );

		this.itemID = itemID;
		this.quantity = quantity;

		addFormField( "action", "trade" );
		addFormField( "quantity", String.valueOf( quantity ) );
		addFormField( "whichitem", String.valueOf( itemID ) );
		addFormField( "pwd" );
	}

	/**
	 * Executes the <code>HermitRequest</code>.  This will trade the item
	 * specified in the character's <code>KoLSettings</code> for their
	 * worthless trinket; if the character has no worthless trinkets, this
	 * method will report an error to the client.
	 */

	public void run()
	{
		if ( itemID != -1 && quantity <= 0 )
		{
			KoLmafia.updateDisplay( ERROR_STATE, "Zero is not a valid quantity." );
			return;
		}
		else if ( quantity > getWorthlessItemCount() )
		{
			KoLmafia.updateDisplay( ERROR_STATE, "You do not have enough worthless items." );
			return;
		}

		KoLmafia.updateDisplay( "Robbing the hermit..." );
		super.run();
	}

	protected void processResults()
	{
		if ( itemID == -1 )
		{
			// "You don't have a Hermit Permit, so you're not
			// allowed to visit the Hermit."

			if ( responseText.indexOf( "you're not allowed to visit" ) != -1 )
			{
				AdventureDatabase.retrieveItem( PERMIT.getInstance( 1 ) );
				if ( KoLmafia.permitsContinue() )
					this.run();
				else
					KoLmafia.updateDisplay( ERROR_STATE, "You're not allowed to visit the Hermit." );

				return;
			}

			// "The Hermit rummages through your sack, and with a
			// disappointed look on his face, he sends you
			// packing."

			if ( responseText.indexOf( "sends you packing" ) != -1 )
			{
				// Automatically attempt to get a worthless item
				// in order to determine what's available.

				if ( KoLCharacter.getAdventuresLeft() > 0 )
				{
					DEFAULT_SHELL.executeLine( "retrieve worthless item" );
					if ( getWorthlessItemCount() != 0 )
						this.run();
				}

				return;
			}

			// Parse response and build list of items

			hermitItems.clear();
			Matcher matcher = AVAILABLE_PATTERN.matcher( responseText );

			while ( matcher.find() )
				hermitItems.add( KoLDatabase.getDisplayName( matcher.group(2) ) );

			return;
		}

		// If you don't have enough Hermit Permits, then retrieve the
		// number of hermit permits requested.

		if ( responseText.indexOf( "You don't have enough Hermit Permits" ) != -1 )
		{
			// Figure out how many you do have.

			AdventureDatabase.retrieveItem( PERMIT.getInstance( quantity ) );
			if ( KoLmafia.permitsContinue() )
				this.run();

			return;
		}

		// If you don't have enough worthless items, scale back.

		if ( responseText.indexOf( "You don't have enough stuff" ) != -1 )
		{
			// Figure out how many items you do have.

			(new EquipmentRequest( client, EquipmentRequest.CLOSET )).run();
			int actualQuantity = getWorthlessItemCount();

			if ( actualQuantity > 0 )
			{
				(new HermitRequest( client, itemID, actualQuantity )).run();
				return;
			}

			KoLmafia.updateDisplay( ERROR_STATE, "Ran out of worthless junk." );
			return;
		}

		// If the item is unavailable, assume he was asking for clover

		if ( responseText.indexOf( "doesn't have that item." ) != -1 )
		{
			KoLmafia.updateDisplay( ERROR_STATE, "Today is not a clover day." );
			return;
		}

		// If you still didn't acquire items, what went wrong?

		if ( responseText.indexOf( "You acquire" ) == -1 )
		{
			KoLmafia.updateDisplay( ERROR_STATE, "The hermit kept his stuff." );
			return;
		}

		// Tally up the trade
		super.processResults();

		// "He looks confused for a moment.  &quot;I already took your
		// Hermit Permit, right?&quot;" or "your Hermit Permits"

		if ( responseText.indexOf( "He looks confused for a moment." ) == -1 )
			client.processResult( new AdventureResult( 42, 0 - quantity ) );

		// Subtract the worthless items in order of their priority;
		// as far as we know, the priority is the item ID.

		quantity -= subtractWorthlessItems( TRINKET, quantity );
		quantity -= subtractWorthlessItems( GEWGAW, quantity );
		subtractWorthlessItems( KNICK_KNACK, quantity );

		KoLmafia.updateDisplay( "Hermit successfully looted!" );
	}

	private int subtractWorthlessItems( AdventureResult item, int total )
	{
		int count = 0 - Math.min( total, item.getCount( inventory ) );
		client.processResult( item.getInstance( count ) );
		return 0 - count;
	}

	public static final int getWorthlessItemCount()
	{
		return TRINKET.getCount( inventory ) +
				GEWGAW.getCount( inventory ) + KNICK_KNACK.getCount( inventory );
	}

	public static final boolean isCloverDay()
	{
		if ( !hermitItems.contains( "ten-leaf clover" ) )
			(new HermitRequest( StaticEntity.getClient() )).run();

		return hermitItems.contains( "ten-leaf clover" );
	}
}
