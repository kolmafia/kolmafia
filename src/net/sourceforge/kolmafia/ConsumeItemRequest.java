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

public class ConsumeItemRequest extends KoLRequest
{
	public static final int NO_CONSUME = 0;
	public static final int CONSUME_EAT = 1;
	public static final int CONSUME_DRINK = 2;
	public static final int CONSUME_USE = 3;
	public static final int CONSUME_MULTIPLE = 4;
	public static final int GROW_FAMILIAR = 5;

	public static final int EQUIP_FAMILIAR = 6;

	private int consumptionType;
	private AdventureResult itemUsed;

	public ConsumeItemRequest( KoLmafia client, AdventureResult item )
	{	this( client, TradeableItemDatabase.getConsumptionType( item.getName() ), item );
	}

	/**
	 * Constructs a new <code>ConsumeItemRequest</code>.
	 * @param	client	The client to be notified of the logout
	 */

	private ConsumeItemRequest( KoLmafia client, int consumptionType, AdventureResult item )
	{
		super( client, consumptionType == CONSUME_EAT ? "inv_eat.php" : consumptionType == CONSUME_DRINK ? "inv_booze.php" :
			consumptionType == CONSUME_MULTIPLE ? "multiuse.php" : consumptionType == GROW_FAMILIAR ? "inv_familiar.php" : "inv_use.php" );

		if ( consumptionType == CONSUME_MULTIPLE )
		{
			addFormField( "action", "useitem" );
			addFormField( "quantity", String.valueOf( item.getCount() ) );
		}

		addFormField( "whichitem", "" + item.getItemID() );
		addFormField( "pwd", client.getPasswordHash() );

		this.consumptionType = consumptionType;
		this.itemUsed = new AdventureResult( item.getItemID(), 0 - item.getCount() );
	}

	public int getConsumptionType()
	{	return consumptionType;
	}

	public AdventureResult getItemUsed()
	{	return itemUsed;
	}

	public void run()
	{
		// Note that requests for bartenders and chefs should
		// not be run if the character already has one

		if ( (itemUsed.getName().startsWith( "chef-in" ) && client.getCharacterData().hasChef()) ||
			itemUsed.getName().startsWith( "bartender-in" ) && client.getCharacterData().hasBartender() )
		{
			client.cancelRequest();
			updateDisplay( ERROR_STATE, "You already have one installed." );
			return;
		}

		// Check to make sure the character has the item in their
		// inventory first - if not, report the error message and
		// return from the method.

		int itemIndex = client.getInventory().indexOf( itemUsed );
		if ( itemIndex == -1 || ((AdventureResult)client.getInventory().get( itemIndex )).getCount() + itemUsed.getCount() < 0 )
		{
			updateDisplay( ERROR_STATE, "You do not have enough " + itemUsed.getName() + "." );
			client.cancelRequest();
			return;
		}

		super.run();

		if ( isErrorState )
			return;

		// You know you're successful if the server
		// attempts to redirect you.

		if ( responseCode == 302 && !isErrorState )
		{
			if ( itemUsed.getName().startsWith( "chef-in" ) )
				client.getCharacterData().setChef( true );
			else if ( itemUsed.getName().startsWith( "bartender-in" ) )
				client.getCharacterData().setBartender( true );

			(new RetrieveResultRequest( client, redirectLocation )).run();
		}
		else if ( replyContent.indexOf( "Too much" ) != -1 )
		{
			client.cancelRequest();
			updateDisplay( ERROR_STATE, "Your spleen might go kabooie." );
			return;
		}
		else
		{
			client.processResult( itemUsed );
			processResults( replyContent );
		}
	}

	private class RetrieveResultRequest extends KoLRequest
	{
		public RetrieveResultRequest( KoLmafia client, String redirectLocation )
		{	super( client, redirectLocation );
		}

		public void run()
		{
			super.run();

			if ( isErrorState || responseCode != 200 )
				return;

			// Check for familiar growth - if a familiar is added,
			// make sure to update the client.

			if ( consumptionType == GROW_FAMILIAR )
			{
				if ( replyContent.indexOf( "You've already got a familiar of that type." ) != -1 )
				{
					client.cancelRequest();
					updateDisplay( ERROR_STATE, "You already have that familiar." );
					return;
				}
				else
					client.getCharacterData().addFamiliar( FamiliarsDatabase.growFamiliarItem( itemUsed.getName() ) );
			}

			// Check to make sure that it wasn't a food or drink
			// that was consumed that resulted in nothing.

			else if ( replyContent.indexOf( "too full" ) != -1 || replyContent.indexOf( "too drunk" ) != -1 )
			{
				client.cancelRequest();
				updateDisplay( ERROR_STATE, "Consumption limit reached." );
				return;
			}

			// Check to make sure that if a scroll of drastic healing
			// were used and didn't dissolve, the scroll is not consumed

			else if ( itemUsed.getName().equals( "scroll of drastic healing" ) )
			{
				client.processResult( new AdventureResult( AdventureResult.HP, client.getCharacterData().getMaximumHP() ) );
				if ( replyContent.indexOf( "crumble" ) == -1 )
					return;
			}

			// Check to see if you were using a Jumbo Dr. Lucifer, which
			// reduces your hit points to 1.

			else if ( itemUsed.getName().equals( "Jumbo Dr. Lucifer" ) )
				client.processResult( new AdventureResult( AdventureResult.HP, 1 - client.getCharacterData().getCurrentHP() ) );

			// Parse the reply, which can be found before the
			// word "Inventory".  In theory, this could've caused
			// problems in the inventory screen, but since Jick
			// is probably smarter with error-checking after so
			// long, the output/input's probably just fine.

			if ( itemUsed.getName().indexOf( "rolling" ) == -1 && itemUsed.getName().indexOf( "Protest" ) == -1 )
				client.processResult( itemUsed );

			processResults( replyContent.substring( 0, replyContent.indexOf( "Inventory:" ) ) );

			// Handle rolling and unrolling pins removing your
			// dough from the inventory.

			if ( itemUsed.getName().indexOf( "rolling" ) != -1 )
			{
				String consumedItemName = itemUsed.getName().startsWith( "r" ) ? "wad of dough" : "flat dough";
				int consumedItemIndex = client.getInventory().indexOf( new AdventureResult( consumedItemName, 0 ) );

				if ( consumedItemIndex != -1 )
					client.processResult( new AdventureResult( consumedItemName,
						0 - ((AdventureResult)client.getInventory().get( consumedItemIndex )).getCount() ) );
			}
		}
	}
}
