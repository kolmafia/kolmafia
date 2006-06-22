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

	private static final int DOLPHIN_KING_MAP = 26;
	private static final int SPOOKY_TEMPLE_MAP = 74;
	private static final int DINGHY_PLANS = 146;
	private static final int ENCHANTED_BEAN = 186;
	private static final int FENG_SHUI = 210;
	private static final int CHEF = 438;
	private static final int BARTENDER = 440;
	private static final int KETCHUP_HOUND = 493;
	private static final int RAFFLE_TICKET = 500;
	private static final int ARCHES = 504;
	private static final int HEY_DEZE_MAP = 516;
	private static final int GATES_SCROLL = 552;
	private static final int LUCIFER = 571;
	private static final int TINY_HOUSE = 592;
	private static final int DRASTIC_HEALING = 595;
	private static final int SLUG_LORD_MAP = 598;
	private static final int DR_HOBO_MAP = 601;
	private static final int WARM_SUBJECT = 621;
	private static final int TOASTER = 637;
	private static final int GIANT_CASTLE_MAP = 667;
	private static final int YETI_PROTEST_SIGN = 775;
	private static final int ANTIDOTE = 829;
	private static final int TEARS = 869;
	private static final int ROLLING_PIN = 873;
	private static final int UNROLLING_PIN = 874;
	private static final int PLUS_SIGN = 918;
	private static final int CLOCKWORK_BARTENDER = 1111;
	private static final int CLOCKWORK_CHEF = 1112;
	private static final int SNOWCONE_TOME = 1411;
	private static final int PURPLE = 1412;
	private static final int GREEN = 1413;
	private static final int ORANGE = 1414;
	private static final int RED = 1415;
	private static final int BLUE = 1416;
	private static final int BLACK = 1417;
	private static final int HILARIOUS_TOME = 1498;
	private static final int ASTRAL_MUSHROOM = 1622;

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
	private static final int GIFTV = 1460;
	private static final int GIFTR = 1534;

	private static final AdventureResult POISON = new AdventureResult( "Poisoned", 1, true );
	private static final AdventureResult SAPLING = new AdventureResult( 75, -1 );
	private static final AdventureResult FERTILIZER = new AdventureResult( 76, -1 );
	private static final AdventureResult PLANKS = new AdventureResult( 140, -1 );
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

	private ConsumeItemRequest( KoLmafia client, int consumptionType, AdventureResult item )
	{
		this( client, consumptionType == CONSUME_EAT ? "inv_eat.php" : consumptionType == CONSUME_DRINK ? "inv_booze.php" :
			consumptionType == CONSUME_MULTIPLE ? "multiuse.php" : consumptionType == GROW_FAMILIAR ? "inv_familiar.php" :
			consumptionType == CONSUME_RESTORE ? "skills.php" : "inv_use.php", consumptionType, item );
	}

	private ConsumeItemRequest( KoLmafia client, String location, int consumptionType, AdventureResult item )
	{
		super( client, location );

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
		addFormField( "pwd" );

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
		if ( itemUsed.getItemID() == SorceressLair.PUZZLE_PIECE.getItemID() )
		{
			SorceressLair.completeHedgeMaze();
			return;
		}

		int iterations = 1;
		if ( itemUsed.getCount() != 1 && consumptionType != ConsumeItemRequest.CONSUME_MULTIPLE && consumptionType != ConsumeItemRequest.CONSUME_RESTORE )
		{
			iterations = itemUsed.getCount();
			itemUsed = itemUsed.getInstance( 1 );
		}

		String useTypeAsString = (consumptionType == ConsumeItemRequest.CONSUME_EAT) ? "Eating" :
			(consumptionType == ConsumeItemRequest.CONSUME_DRINK) ? "Drinking" : "Using";

		for ( int i = 1; KoLmafia.permitsContinue() && i <= iterations; ++i )
			useOnce( i, iterations, useTypeAsString );
	}

	public void useOnce( int currentIteration, int totalIterations, String useTypeAsString )
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
			KoLmafia.updateDisplay( PENDING_STATE, "You already have one installed." );
			return;
		}

		// Check to make sure the character has the item in their
		// inventory first - if not, report the error message and
		// return from the method.

		AdventureDatabase.retrieveItem( itemUsed );
		if ( !KoLmafia.permitsContinue() )
			return;

		if ( !formURLString.startsWith( "inventory.php" ) )
		{
			if ( totalIterations == 1 )
				KoLmafia.updateDisplay( useTypeAsString + " " + getItemUsed().toString() + "..." );
			else
				KoLmafia.updateDisplay( useTypeAsString + " " + getItemUsed().getName() +
					" (" + currentIteration + " of " + totalIterations + ")..." );
		}

		super.run();

		if ( responseCode == 302 && redirectLocation.startsWith( "inventory.php" ) )
		{
			// Follow the redirection and get the message;
			// instantiate a new consume item request so
			// that it processes the right result.

			ConsumeItemRequest message = new ConsumeItemRequest( client, redirectLocation, consumptionType, itemUsed );
			message.run();
		}
	}

	protected void processResults()
	{
		// Check for familiar growth - if a familiar is added,
		// make sure to update the client.

		if ( consumptionType == GROW_FAMILIAR )
		{
			if ( responseText.indexOf( "You've already got a familiar of that type." ) != -1 )
			{
				KoLmafia.updateDisplay( PENDING_STATE, "You already have that familiar." );
				return;
			}

			KoLCharacter.addFamiliar( FamiliarsDatabase.growFamiliarLarva( itemUsed.getItemID() ) );

			// Use up the familiar larva
			client.processResult( itemUsed.getInstance( -1 ) );

			// Pop up a window showing the result
			client.showHTML( trimInventoryText( responseText ), "Your new familiar" );

			return;
		}

		if ( responseText.indexOf( "You may not" ) != -1 )
		{
			KoLmafia.updateDisplay( PENDING_STATE, "Pathed ascension." );
			return;
		}

		if ( responseText.indexOf( "rupture" ) != -1 )
		{
			KoLmafia.updateDisplay( PENDING_STATE, "Your spleen might go kabooie." );
			return;
		}

		// Check to make sure that it wasn't a food or drink
		// that was consumed that resulted in nothing.  Eating
		// too much is flagged as a continuable state.

		if ( responseText.indexOf( "too full" ) != -1 )
		{
			KoLmafia.updateDisplay( PENDING_STATE, "Consumption limit reached." );
			return;
		}

		if ( responseText.indexOf( "too drunk" ) != -1 )
		{
			KoLmafia.updateDisplay( ERROR_STATE, "Inebriety limit reached." );
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
		case GIFTV:
		case GIFTR:
			// If it's a gift package, get the inner message

			// "You can't receive things from other players
			// right now."
			if ( responseText.indexOf( "You can't receive things" ) != -1 )
			{
				KoLmafia.updateDisplay( PENDING_STATE, "You can't open that package yet." );
				return;
			}

			// Find out who sent it
			Matcher matcher = Pattern.compile( "From: <b>(.*?)</b>" ).matcher( responseText );
			String title = matcher.find() ? "Gift from " + matcher.group(1) : "Your gift";

			// Pop up a window showing what was in the gift.
			client.showHTML( trimInventoryText( responseText ), title );
			break;

		case GATES_SCROLL:
			// You can only use a 64735 scroll if you have the
			// original dictionary in your inventory

			// "Even though your name isn't Lee, you're flattered
			// and hand over your dictionary."

			if ( responseText.indexOf( "you're flattered" ) == -1 )
				return;

			// Remove the old dictionary
			client.processResult( FightRequest.DICTIONARY1.getNegation() );

			// If he was fighting with the old dictionary, switch
			// to use the new one.

			if ( getProperty( "battleAction" ).equals( "item0536" ) )
				setProperty( "battleAction", "item1316" );

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

			break;

		case HEY_DEZE_MAP:
			// "Your song has pleased me greatly. I will reward you
			// with some of my crazy imps, to do your bidding."
			if ( responseText.indexOf( "pleased me greatly" ) == -1 )
			{
				KoLmafia.updateDisplay( PENDING_STATE, "You music was inadequate." );
				return;
			}
			break;

		case GIANT_CASTLE_MAP:
			// "I'm sorry, adventurer, but the Sorceress is in
			// another castle!"
			if ( responseText.indexOf( "Sorceress is in another castle" ) == -1 )
			{
				KoLmafia.updateDisplay( PENDING_STATE, "You couldn't make it all the way to the back door." );
				return;
			}
			break;

		case DRASTIC_HEALING:
			// If a scroll of drastic healing was used and didn't
			// crumble, it is not consumed

			client.processResult( new AdventureResult( AdventureResult.HP, KoLCharacter.getMaximumHP() ) );

			if ( responseText.indexOf( "crumble" ) == -1 )
			{
				KoLCharacter.updateStatus();
				return;
			}

			break;

		case TEARS:
			KoLCharacter.getEffects().remove( KoLAdventure.BEATEN_UP );
			break;

		case ANTIDOTE:
			KoLCharacter.getEffects().remove( POISON );
			break;

		case TINY_HOUSE:
			// Tiny houses remove lots of different effects.

			int originalEffectCount = KoLCharacter.getEffects().size();
			client.applyTinyHouseEffect();
			needsRefresh = originalEffectCount != KoLCharacter.getEffects().size();
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

		case DOLPHIN_KING_MAP:
			// "You follow the Dolphin King's map to the bottom of
			// the sea, and find his glorious treasure."
			if ( responseText.indexOf( "find his glorious treasure" ) == -1 )
			{
				KoLmafia.updateDisplay( PENDING_STATE, "You don't have everything you need." );
				return;
			}
			break;

		case SLUG_LORD_MAP:
			// "You make your way to the deepest part of the tank,
			// and find a chest engraved with the initials S. L."
			if ( responseText.indexOf( "deepest part of the tank" ) == -1 )
			{
				KoLmafia.updateDisplay( PENDING_STATE, "You don't have everything you need." );
				return;
			}
			break;

		case DR_HOBO_MAP:
			// "You place it atop the Altar, and grab the Scalpel
			// at the exact same moment."
			if ( responseText.indexOf( "exact same moment" ) == -1 )
			{
				KoLmafia.updateDisplay( PENDING_STATE, "You don't have everything you need." );
				return;
			}
			break;

		case SPOOKY_TEMPLE_MAP:
			// "You plant your Spooky Sapling in the loose soil at
			// the base of the Temple.  You spray it with your
			// Spooky-Gro Fertilizer, and it immediately grows to
			// 20 feet in height.  You can easily climb the
			// branches to reach the first step of the Temple
			// now..."
			if ( responseText.indexOf( "easily climb the branches" ) == -1 )
			{
				KoLmafia.updateDisplay( PENDING_STATE, "You don't have everything you need." );
				return;
			}
			client.processResult( SAPLING );
			client.processResult( FERTILIZER );
			break;

		case DINGHY_PLANS:
			// "You need some planks to build the dinghy."
			if ( responseText.indexOf( "need some planks" ) != -1 )
			{
				KoLmafia.updateDisplay( PENDING_STATE, "You need some dingy planks." );
				return;
			}
			client.processResult( PLANKS );
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
			// The first time you use Warm Subject gift
			// certificates when you have the Torso Awaregness
			// skill consumes only one, even if you tried to
			// multi-use the item.

			// "You go to Warm Subject and browse the shirts for a
			// while. You find one that you wouldn't mind wearing
			// ironically. There seems to be only one in the store,
			// though."

			if ( responseText.indexOf( "ironically" ) != -1 )
			{
				client.processResult( itemUsed.getInstance( -1 ) );
				super.processResults();
				return;
			}
			break;

		case PURPLE:
		case GREEN:
		case ORANGE:
		case RED:
		case BLUE:
		case BLACK:
			// "Your mouth is still cold from the last snowcone you
			// ate.	 Try again later."
			if ( responseText.indexOf( "still cold" ) != -1 )
			{
				KoLmafia.updateDisplay( PENDING_STATE, "Your mouth is too cold." );
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

		case PLUS_SIGN:
			// "Following The Oracle's advice, you treat the plus
			// sign as a book, and read it."
			if ( responseText.indexOf( "you treat the plus sign as a book" ) == -1 )
			{
				KoLmafia.updateDisplay( PENDING_STATE, "You don't know how to use it." );
				return;
			}
			break;

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

		case SNOWCONE_TOME:
			// "You read the incantation written on the pages of
			// the tome. Snowflakes coalesce in your
			// mind. Delicious snowflakes."
			if ( responseText.indexOf( "You read the incantation" ) == -1 )
				return;
			KoLCharacter.addAvailableSkill( new UseSkillRequest( client, "Summon Snowcone", "", 1 ) );
			break;


		case HILARIOUS_TOME:
			// "You pore over the tome, and sophomoric humor pours
			// into your brain. The mysteries of McPhee become
			// clear to you."
			if ( responseText.indexOf( "You pore over the tome" ) == -1 )
				return;
			KoLCharacter.addAvailableSkill( new UseSkillRequest( client, "Summon Hilarious Objects", "", 1 ) );
			break;

		case ASTRAL_MUSHROOM:
			// "You eat the mushroom, and are suddenly engulfed in
			// a whirling maelstrom of colors and sensations as
			// your awareness is whisked away to some strange
			// alternate dimension. Who would have thought that a
			// glowing, ethereal mushroom could have that kind of
			// effect?"
			//
			// vs.
			//
			// "Whoo, man, lemme tell you, you don't need to be
			// eating another one of those just now, okay?"
			if ( responseText.indexOf( "whirling maelstrom" ) == -1 )
				return;
			StaticEntity.setProperty( "nextAdventure", "" );
			break;
		}

		// If we get here, we know that the item is consumed by being
		// used. Do so.

		client.processResult( itemUsed.getNegation() );
		super.processResults();
	}

	private String trimInventoryText( String text )
	{
		// Get rid of first row of first table: the "Results" line
		Matcher matcher = Pattern.compile( "<tr>.*?</tr>" ).matcher( text );
		if ( matcher.find() )
			text = matcher.replaceFirst( "" );

		// Get rid of inventory listing
		matcher = Pattern.compile( "</table><table.*?</body>" ).matcher( text );
		if ( matcher.find() )
			text = matcher.replaceFirst( "</table></body>" );

		return text;
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

		commandString.append( itemUsed.getCount() );
		commandString.append( ' ' );
		commandString.append( itemUsed.getName() );
		return commandString.toString();
	}

	public static boolean processRequest( KoLmafia client, String urlString )
	{
		int consumptionType = NO_CONSUME;

		if ( urlString.indexOf( "inv_eat.php" ) != -1 )
			consumptionType = CONSUME_EAT;
		else if ( urlString.indexOf( "inv_booze.php" ) != -1 )
			consumptionType = CONSUME_DRINK;
		else if ( urlString.indexOf( "multiuse.php" ) != -1 )
			consumptionType = CONSUME_MULTIPLE;
		else if ( urlString.indexOf( "skills.php" ) != -1 )
			consumptionType = CONSUME_RESTORE;
		else if ( urlString.indexOf( "inv_familiar.php" ) != -1 )
			consumptionType = GROW_FAMILIAR;
		else if ( urlString.indexOf( "inv_use.php" ) != -1 )
			consumptionType = CONSUME_USE;
		else
			return false;

		AdventureResult itemUsed = null;
		Matcher itemMatcher = Pattern.compile( "whichitem=(\\d+)" ).matcher( urlString );
		if ( itemMatcher.find() )
			itemUsed = new AdventureResult( Integer.parseInt( itemMatcher.group(1) ), 1 );

		if ( urlString.indexOf( "multiuse.php" ) != -1 || urlString.indexOf( "skills.php" ) != -1 )
		{
			Matcher quantityMatcher = Pattern.compile( "quantity=(\\d+)" ).matcher( urlString );
			if ( quantityMatcher.find() )
				itemUsed = itemUsed.getInstance( Integer.parseInt( quantityMatcher.group(1) ) );
		}

		String useTypeAsString = (consumptionType == ConsumeItemRequest.CONSUME_EAT) ? "eat " :
			(consumptionType == ConsumeItemRequest.CONSUME_DRINK) ? "drink " : "use ";

		KoLmafia.getSessionStream().println( useTypeAsString + itemUsed.getCount() + " " + itemUsed.getName() );
		client.processResult( itemUsed.getNegation() );
		return true;
	}
}
