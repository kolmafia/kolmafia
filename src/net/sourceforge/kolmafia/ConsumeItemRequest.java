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
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class ConsumeItemRequest extends KoLRequest
{
	public static final int NO_CONSUME = 0;
	public static final int CONSUME_EAT = 1;
	public static final int CONSUME_DRINK = 2;
	public static final int CONSUME_USE = 3;
	public static final int CONSUME_MULTIPLE = 4;
	public static final int GROW_FAMILIAR = 5;
	public static final int CONSUME_ZAP = 6;
	public static final int EQUIP_FAMILIAR = 7;
	public static final int EQUIP_ACCESSORY = 8;
	public static final int EQUIP_HAT = 9;
	public static final int EQUIP_PANTS = 10;
	public static final int EQUIP_SHIRT = 11;
	public static final int EQUIP_WEAPON = 12;
	public static final int EQUIP_OFFHAND = 13;

	private static final int CHEF = 438;
	private static final int BARTENDER = 440;
	private static final int ARCHES = 504;
	private static final int TOASTER = 637;
	private static final int CLOCKWORK_BARTENDER = 1111;
	private static final int CLOCKWORK_CHEF = 1112;

	private static final int FIRST_PACKAGE = 1167;
	private static final int LAST_PACKAGE = 1177;

	private static final AdventureResult AXE = new AdventureResult( 555, 1 );
	private static final AdventureResult NUTS = new AdventureResult( 509, -1 );
	private static final AdventureResult PLAN = new AdventureResult( 502, -1 );
	private static final AdventureResult FOUNTAIN = new AdventureResult( 211, -1 );
	private static final AdventureResult WINDCHIMES = new AdventureResult( 212, -1 );

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
		this.itemUsed = item;
	}

	public int getConsumptionType()
	{	return consumptionType;
	}

	public AdventureResult getItemUsed()
	{	return itemUsed;
	}

	public void run()
	{
		if ( itemUsed.getItemID() == UneffectRequest.REMEDY.getItemID() )
		{
			client.makeUneffectRequest();
			return;
		}

		if ( consumptionType == CONSUME_ZAP )
		{
			client.makeZapRequest();
			return;
		}

		// Note that requests for bartenders and chefs should
		// not be run if the character already has one

		boolean alreadyInstalled = false;

		switch ( itemUsed.getItemID() )
		{
			case CHEF:
			case CLOCKWORK_CHEF:
				alreadyInstalled = KoLCharacter.hasChef();
				break;
			case BARTENDER:
			case CLOCKWORK_BARTENDER:
				alreadyInstalled = KoLCharacter.hasBartender();
				break;
			case TOASTER:
				alreadyInstalled = KoLCharacter.hasToaster();
				break;
			case ARCHES:
				alreadyInstalled = KoLCharacter.hasArches();
				break;
		}

		if ( alreadyInstalled )
		{
			client.cancelRequest();
			updateDisplay( ERROR_STATE, "You already have one installed." );
			return;
		}

		// Check to make sure the character has the item in their
		// inventory first - if not, report the error message and
		// return from the method.

		int itemCount = itemUsed.getCount( KoLCharacter.getInventory() );
		if ( itemCount == 0 || itemUsed.getCount() > itemCount )
		{
			updateDisplay( ERROR_STATE, "You do not have enough " + itemUsed.getName() + "." );
			client.cancelRequest();
			return;
		}

		super.run();

		if ( !isErrorState && responseCode == 302 )
		{
			KoLRequest message = new KoLRequest( client, redirectLocation );
			message.run();

			responseCode = message.responseCode;
			responseText = message.responseText;

			// If it's a gift package, get the inner message
			if ( itemUsed.getItemID() >= FIRST_PACKAGE && itemUsed.getItemID() <= LAST_PACKAGE )
			{
				// "You can't receive things from other players
				// right now."
				if ( responseText.indexOf( "You can't receive things" ) != -1 )
				{
					client.cancelRequest();
					updateDisplay( ERROR_STATE, "You can't open that package yet." );
					return;
				}

				// Trim copy of responseText
				String text = responseText;

				// Get rid of first row of first table
				Matcher matcher = Pattern.compile( "<tr>.*?</tr>" ).matcher( text );
				if ( matcher.find() )
					text = matcher.replaceFirst( "" );

				// Get rid of inventory listing
				matcher = Pattern.compile( "</table><table.*?</body>" ).matcher( text );
				if ( matcher.find() )
					text = matcher.replaceFirst( "</table></body>" );
				// Find out who sent it
				matcher = Pattern.compile( "From: <b>(.*?)</b>" ).matcher( text );
				String title = matcher.find() ? "Gift from " + matcher.group(1) : "Your gift";

				// Pop up a window showing what was in the gift.
				client.showHTML( text, title );
			}
		}

		// If an error state occurred, return from this
		// request, since there's no content to parse

		if ( isErrorState || responseCode != 200 )
			return;

		if ( responseText.indexOf( "You may not" ) != -1 )
		{
			client.cancelRequest();
			updateDisplay( ERROR_STATE, "Pathed ascension." );
		}

		if ( responseText.indexOf( "Too much" ) != -1 )
		{
			client.cancelRequest();
			updateDisplay( ERROR_STATE, "Your spleen might go kabooie." );
			return;
		}

		// Check for familiar growth - if a familiar is added,
		// make sure to update the client.

		if ( consumptionType == GROW_FAMILIAR )
		{
			if ( responseText.indexOf( "You've already got a familiar of that type." ) != -1 )
			{
				client.cancelRequest();
				updateDisplay( ERROR_STATE, "You already have that familiar." );
				return;
			}

			KoLCharacter.addFamiliar( FamiliarsDatabase.growFamiliarLarva( itemUsed.getItemID() ) );
		}
		// Check to make sure that it wasn't a food or drink
		// that was consumed that resulted in nothing.

		else if ( responseText.indexOf( "too full" ) != -1 || responseText.indexOf( "too drunk" ) != -1 )
		{
			client.cancelRequest();
			updateDisplay( ERROR_STATE, "Consumption limit reached." );
			return;
		}

		// If a beanstalk grows out of an enchanted bean, visit it.

		else if ( itemUsed.getName().equals( "enchanted bean" ) )
		{
			// There are three possibilities.

			// If you haven't been give the quest, "you can't find
			// anywhere that looks like a good place to plant the
			// bean" and you're told to "wait until later"

			// If you've already planted one, "There's already a
			// beanstalk in the Nearby Plains." In either case, the
			// bean is not consumed.

			// Otherwise, "it immediately grows into an enormous beanstalk".

			if ( responseText.indexOf( "grows into an enormous beanstalk" ) == -1 )
				return;

			KoLCharacter.addAccomplishment( KoLCharacter.BEANSTALK );
		}

		// If a scroll of drastic healing was used and didn't dissolve,
		// it is not consumed

		else if ( itemUsed.getName().equals( "scroll of drastic healing" ) )
		{
			client.processResult( new AdventureResult( AdventureResult.HP, KoLCharacter.getMaximumHP() ) );
			if ( responseText.indexOf( "crumble" ) == -1 )
				return;
		}

		// Tiny houses also have an added bonus - they will remove
		// lots of different effects.  Therefore, process it.

		else if ( itemUsed.getName().equals( "tiny house" ) )
			client.applyTinyHouseEffect();

		// The first time you use an Elf Farm Raffle ticket with a
		// ten-leaf clover in your inventory, the clover disappears in
		// a puff of smoke and you get pagoda plans.
		//
		// Subsequent raffle tickets don't consume clovers.

		else if ( itemUsed.getName().equals( "Elf Farm Raffle ticket" ) && responseText.indexOf( "puff of smoke" ) != -1 )
			client.processResult( SewerRequest.CLOVER );

		// Successfully using a ketchup hound uses up the Hey Deze nuts
		// and pagoda plan.

		else if ( itemUsed.getName().equals( "ketchup hound" ) )
		{
			if ( responseText.indexOf( "pagoda" ) != -1 )
			{
				client.processResult( NUTS );
				client.processResult( PLAN );
			}

			// The ketchup hound does not go away...
			return;
		}

		// Check to see if you were using a Jumbo Dr. Lucifer, which
		// reduces your hit points to 1.

		else if ( itemUsed.getName().equals( "Jumbo Dr. Lucifer" ) )
			client.processResult( new AdventureResult( AdventureResult.HP, 1 - KoLCharacter.getCurrentHP() ) );

		// If you successfully use a 64735 scroll, you lose the
		// dictionary you untinkered from the abridged dictionary, but
		// gain another, just as usable in combat, but also smithable
		// and sellable.

		else if ( itemUsed.getName().startsWith( "64735" ) )
		{
			if ( responseText.indexOf( "You are magically transported" ) != -1 )
			{
				KoLCharacter.addAccomplishment( KoLCharacter.BARON );

				client.processResult( itemUsed.getNegation() );
				client.processResult( FightRequest.DICTIONARY1.getNegation() );
			}

			return;
		}

		// Successfully using the "Feng Shui for Big Dumb Idiots" uses
		// up the decorative fountain and windchimes

		else if ( itemUsed.getName().startsWith( "Feng Shui" ) )
		{
			// Only used up once
			if ( responseText.indexOf( "Feng Shui goodness" ) == -1 )
				return;

			client.processResult( FOUNTAIN );
			client.processResult( WINDCHIMES );
		}

		// Parse the reply, which can be found before the
		// word "Inventory".  In theory, this could've caused
		// problems in the inventory screen, but since Jick
		// is probably smarter with error-checking after so
		// long, the output/input's probably just fine.

		if ( itemUsed.getName().indexOf( "rolling" ) == -1 && itemUsed.getName().indexOf( "Protest" ) == -1 )
			client.processResult( itemUsed.getNegation() );

		// Handle rolling and unrolling pins removing your
		// dough from the inventory.

		if ( itemUsed.getName().indexOf( "rolling" ) != -1 )
		{
			AdventureResult consumedItem = new AdventureResult( itemUsed.getName().startsWith( "r" ) ? "wad of dough" : "flat dough", 0 );
			client.processResult( consumedItem.getInstance( consumedItem.getCount( KoLCharacter.getInventory() ) ).getNegation() );
		}

		// Handle campground items which change the state
		// of something related to the character.

		switch ( itemUsed.getItemID() )
		{
			case CHEF:
			case CLOCKWORK_CHEF:
				KoLCharacter.setChef( true );
				break;
			case BARTENDER:
			case CLOCKWORK_BARTENDER:
				KoLCharacter.setBartender( true );
				break;
			case TOASTER:
				KoLCharacter.setToaster( true );
				break;
			case ARCHES:
				KoLCharacter.setArches( true );
				break;
		}
	}
}
