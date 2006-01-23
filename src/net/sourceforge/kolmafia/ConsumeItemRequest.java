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
	public static final int CONSUME_RESTORE = 14;

	private static final int ENCHANTED_BEAN = 186;
	private static final int FENG_SHUI = 210;
	private static final int CHEF = 438;
	private static final int BARTENDER = 440;
	private static final int KETCHUP_HOUND = 493;
	private static final int RAFFLE_TICKET = 500;
	private static final int ARCHES = 504;
	private static final int GATES_SCROLL = 552;
	private static final int LUCIFER = 571;
	private static final int TINY_HOUSE = 592;
	private static final int DRASTIC_HEALING = 595;
	private static final int WARM_SUBJECT = 621;
	private static final int TOASTER = 637;
	private static final int YETI_PROTEST_SIGN = 775;
	private static final int ROLLING_PIN = 873;
	private static final int UNROLLING_PIN = 874;
	private static final int CLOCKWORK_BARTENDER = 1111;
	private static final int CLOCKWORK_CHEF = 1112;

	private static final int GIFT1 = 1167;
	private static final int GIFT2 = 1168;
	private static final int GIFT3 = 1169;
	private static final int GIFT4 = 1170;
	private static final int GIFT5 = 1171;
	private static final int GIFT6 = 1172;
	private static final int GIFT7 = 1173;
	private static final int GIFT8 = 1174;
	private static final int GIFT9 = 1175;
	private static final int GIFT10 = 1176;
	private static final int GIFT11 = 1177;

	private static final AdventureResult DOUGH = new AdventureResult( 159, 1 );
	private static final AdventureResult FLAT_DOUGH = new AdventureResult( 301, 1 );
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
			consumptionType == CONSUME_MULTIPLE ? "multiuse.php" : consumptionType == GROW_FAMILIAR ? "inv_familiar.php" :
			consumptionType == CONSUME_RESTORE ? "skills.php" : "inv_use.php" );

		if ( consumptionType == CONSUME_MULTIPLE )
		{
			addFormField( "action", "useitem" );
			addFormField( "quantity", String.valueOf( item.getCount() ) );
		}
		if ( consumptionType == CONSUME_RESTORE )
		{
			addFormField( "action", "useitem" );
			addFormField( "itemquantity", String.valueOf( item.getCount() ) );
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

		if ( responseCode == 302 && redirectLocation.startsWith( "inventory.php" ) )
		{
			// Follow the redirection and get the message
			KoLRequest message = new KoLRequest( client, redirectLocation );
			message.run();

			responseCode = message.responseCode;
			String text = message.responseText;

			// If we got a successful response, trim text
			if ( responseCode == 200 )
			{
				// Get rid of first row of first table: the
				// "Results" line
				Matcher matcher = Pattern.compile( "<tr>.*?</tr>" ).matcher( text );
				if ( matcher.find() )
					text = matcher.replaceFirst( "" );

				// Get rid of inventory listing
				matcher = Pattern.compile( "</table><table.*?</body>" ).matcher( text );
				if ( matcher.find() )
					text = matcher.replaceFirst( "</table></body>" );
			}

			responseText = text;
		}

		// If an error state occurred, return from this
		// request, since there's no content to parse
		if ( responseCode != 200 )
			return;

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

			// Use up the familiar larva
			client.processResult( itemUsed.getInstance( -1 ) );

			// Pop up a window showing the result
			client.showHTML( responseText, "Your new familiar" );

			return;
		}

		if ( responseText.indexOf( "You may not" ) != -1 )
		{
			client.cancelRequest();
			updateDisplay( ERROR_STATE, "Pathed ascension." );
			return;
		}

		if ( responseText.indexOf( "Too much" ) != -1 )
		{
			client.cancelRequest();
			updateDisplay( ERROR_STATE, "Your spleen might go kabooie." );
			return;
		}

		// Check to make sure that it wasn't a food or drink
		// that was consumed that resulted in nothing.

		if ( responseText.indexOf( "too full" ) != -1 || responseText.indexOf( "too drunk" ) != -1 )
		{
			client.cancelRequest();
			updateDisplay( ERROR_STATE, "Consumption limit reached." );
			return;
		}

		// Perform item-specific processing
		switch ( itemUsed.getItemID() )
		{
		case GIFT1:
		case GIFT2:
		case GIFT3:
		case GIFT4:
		case GIFT5:
		case GIFT6:
		case GIFT7:
		case GIFT8:
		case GIFT9:
		case GIFT10:
		case GIFT11:
			// If it's a gift package, get the inner message

			// "You can't receive things from other players
			// right now."
			if ( responseText.indexOf( "You can't receive things" ) != -1 )
			{
				client.cancelRequest();
				updateDisplay( ERROR_STATE, "You can't open that package yet." );
				return;
			}

			// Find out who sent it
			Matcher matcher = Pattern.compile( "From: <b>(.*?)</b>" ).matcher( responseText );
			String title = matcher.find() ? "Gift from " + matcher.group(1) : "Your gift";

			// Pop up a window showing what was in the gift.
			client.showHTML( responseText, title );
			break;

		case GATES_SCROLL:
			// You can only use a 64375 scroll if you have the
			// original dictionary in your inventory

			// "Even though your name isn't Lee, you're flattered
			// and hand over your dictionary."

			if ( responseText.indexOf( "you're flattered" ) == -1 )
				return;

			// Get the accomplishment
			KoLCharacter.addAccomplishment( KoLCharacter.BARON );

			// Remove the old dictionary
			client.processResult( FightRequest.DICTIONARY1.getNegation() );

			// Get the new dictionary and drywall axe
			client.processResults( responseText );

			// If he was fighting with the old dictionary, switch
			// to use the new one
			if ( getProperty( "battleAction" ).equals( "item0536" ) )
				setProperty( "battleAction", "item1316" );

			// Adjust battle skills
			int originalIndex = KoLCharacter.getBattleSkillIDs().indexOf( "item0536" );
			int selectedIndex = KoLCharacter.getBattleSkillNames().getSelectedIndex();
			if ( originalIndex != -1 )
			{
				KoLCharacter.getBattleSkillIDs().remove( originalIndex );
				KoLCharacter.getBattleSkillNames().remove( originalIndex );
				KoLCharacter.addDictionary();

				if ( originalIndex == selectedIndex )
				{
					originalIndex = KoLCharacter.getBattleSkillIDs().indexOf( "item1316" );
					KoLCharacter.getBattleSkillNames().setSelectedIndex( originalIndex );
				}
			}
			break;

		case ENCHANTED_BEAN:
			// There are three possibilities:

			// If you haven't been give the quest, "you can't find
			// anywhere that looks like a good place to plant the
			// bean" and you're told to "wait until later"

			// If you've already planted one, "There's already a
			// beanstalk in the Nearby Plains." In either case, the
			// bean is not consumed.

			// Otherwise, "it immediately grows into an enormous
			// beanstalk".

			if ( responseText.indexOf( "grows into an enormous beanstalk" ) == -1 )
				return;

			KoLCharacter.addAccomplishment( KoLCharacter.BEANSTALK );
			break;

		case DRASTIC_HEALING:
			// If a scroll of drastic healing was used and didn't
			// crumble, it is not consumed

			client.processResult( new AdventureResult( AdventureResult.HP, KoLCharacter.getMaximumHP() ) );
			if ( responseText.indexOf( "crumble" ) == -1 )
				return;
			break;

		case TINY_HOUSE:
			// Tiny houses remove lots of different effects.
			client.applyTinyHouseEffect();
			break;

		case RAFFLE_TICKET:
			// The first time you use an Elf Farm Raffle ticket
			// with a ten-leaf clover in your inventory, the clover
			// disappears in a puff of smoke and you get pagoda
			// plans.
			//
			// Subsequent raffle tickets don't consume clovers.
			if ( responseText.indexOf( "puff of smoke" ) != -1 )
				client.processResult( SewerRequest.CLOVER );
			break;

		case KETCHUP_HOUND:
			// Successfully using a ketchup hound uses up the Hey
			// Deze nuts and pagoda plan.
			if ( responseText.indexOf( "pagoda" ) != -1 )
			{
				client.processResult( NUTS );
				client.processResult( PLAN );
			}
			// The ketchup hound does not go away...
			return;

		case LUCIFER:
			// Jumbo Dr. Lucifer reduces your hit points to 1.
			client.processResult( new AdventureResult( AdventureResult.HP, 1 - KoLCharacter.getCurrentHP() ) );
			break;

		case FENG_SHUI:
			// Successfully using "Feng Shui for Big Dumb Idiots"
			// consumes the decorative fountain and windchimes.

			// Only used up once
			if ( responseText.indexOf( "Feng Shui goodness" ) == -1 )
				return;

			client.processResult( FOUNTAIN );
			client.processResult( WINDCHIMES );
			break;

		case WARM_SUBJECT:
			// The first time Warm Subject gift certificate you use
			// when you have the Torso Awaregness skill, uses only
			// one, even if you tried to multi-use the item.

			// "You go to Warm Subject and browse the shirts for a
			// while. You find one that you wouldn't mind wearing
			// ironically. There seems to be only one in the store,
			// though."

			if ( responseText.indexOf( "ironically" ) != -1 )
			{
				client.processResult( itemUsed.getInstance( -1 ) );
				return;
			}
			break;

		case ROLLING_PIN:
			// Rolling pins remove dough from your inventory
			// They are not consumed by being used
			client.processResult( DOUGH.getInstance( DOUGH.getCount( KoLCharacter.getInventory() ) ).getNegation() );

			return;

		case UNROLLING_PIN:
			// Unrolling pins remove flat dough from your inventory
			// They are not consumed by being used
			client.processResult( FLAT_DOUGH.getInstance( FLAT_DOUGH.getCount( KoLCharacter.getInventory() ) ).getNegation() );
			return;

		case YETI_PROTEST_SIGN:
			// You don't use up a Yeti Protest Sign by protesting
			return;

		// Campground items which change character state
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

		// If we get here, we know that the item is consumed by being
		// used. Do so.

		client.processResult( itemUsed.getNegation() );
	}

	protected void processResults()
	{
		// The 64735 scroll might generate a dictionary, which we will
		// process incorrectly, since we don't have the Baron's quest
		// as an accomplishment.
		if ( itemUsed.getItemID() == GATES_SCROLL)
			return;

		// Otherwise, process results as normal
		super.processResults();
	}

	public String getCommandForm( int iterations )
	{
		StringBuffer commandString = new StringBuffer();

		switch ( getConsumptionType() )
		{
			case ConsumeItemRequest.CONSUME_EAT:
				commandString.append( "eat " );
				break;

			case ConsumeItemRequest.CONSUME_DRINK:
				commandString.append( "drink " );
				break;

			default:
				commandString.append( "use " );
				break;
		}

		commandString.append( iterations == 1 ? itemUsed.getCount() : iterations );
		commandString.append( " \"" );
		commandString.append( itemUsed.getName() );
		commandString.append( "\"" );

		return commandString.toString();
	}
}
