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
	private static final Pattern ROW_PATTERN = Pattern.compile( "<tr>.*?</tr>" );
	private static final Pattern GIFT_PATTERN = Pattern.compile( "From: <b>(.*?)</b>" );
	private static final Pattern INVENTORY_PATTERN = Pattern.compile( "</table><table.*?</body>" );
	private static final Pattern ITEMID_PATTERN = Pattern.compile( "whichitem=(\\d+)" );
	private static final Pattern QUANTITY_PATTERN = Pattern.compile( "quantity=(\\d+)" );

	protected static String lastUpdate = "";

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
	public static final int CONSUME_HOBO = 15;

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
	private static final AdventureResult NUTS = new AdventureResult( 509, -1 );
	private static final AdventureResult PLAN = new AdventureResult( 502, -1 );
	private static final AdventureResult FOUNTAIN = new AdventureResult( 211, -1 );
	private static final AdventureResult WINDCHIMES = new AdventureResult( 212, -1 );

	private int consumptionType;
	private AdventureResult itemUsed;
	private boolean isResultPage = false;

	public ConsumeItemRequest( KoLmafia client, AdventureResult item )
	{	this( client, TradeableItemDatabase.getConsumptionType( item.getName() ), item );
	}

	public ConsumeItemRequest( KoLmafia client, int consumptionType, AdventureResult item )
	{
		this( client, consumptionType == CONSUME_EAT ? "inv_eat.php" : consumptionType == CONSUME_DRINK ? "inv_booze.php" :
			consumptionType == CONSUME_MULTIPLE ? "multiuse.php" : consumptionType == GROW_FAMILIAR ? "inv_familiar.php" :
			consumptionType == CONSUME_RESTORE ? "skills.php" : consumptionType == CONSUME_HOBO ? "inventory.php" :
			"inv_use.php", consumptionType, item, false );
	}

	private ConsumeItemRequest( KoLmafia client, String location, int consumptionType, AdventureResult item, boolean isResultPage )
	{
		super( client, location );
		this.isResultPage = isResultPage;

		if ( !isResultPage )
		{
			switch ( consumptionType )
			{
				case CONSUME_MULTIPLE:
					addFormField( "action", "useitem" );
					addFormField( "quantity", String.valueOf( item.getCount() ) );
					break;
				case CONSUME_RESTORE:
					addFormField( "action", "useitem" );
					addFormField( "itemquantity", String.valueOf( item.getCount() ) );
					break;
				case CONSUME_HOBO:
					addFormField( "action", "hobo" );
					addFormField( "which", "1" );
					break;
				case CONSUME_EAT:
				case CONSUME_DRINK:
					addFormField( "which", "1" );
					break;
				default:
					addFormField( "which", "3" );
					break;
			}

			addFormField( "whichitem", String.valueOf( item.getItemID() ) );
			addFormField( "pwd" );
		}

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
		lastUpdate = "";

		if ( itemUsed.getItemID() == SorceressLair.PUZZLE_PIECE.getItemID() )
		{
			SorceressLair.completeHedgeMaze();
			return;
		}

		if ( itemUsed.getCount() < 1 )
			return;

		int price = TradeableItemDatabase.getPriceByID( itemUsed.getItemID() );
		if ( price > 0 )
			AdventureDatabase.retrieveItem( itemUsed );

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
		lastUpdate = "";

		if ( itemUsed.getItemID() == UneffectRequest.REMEDY.getItemID() )
		{
			StaticEntity.getClient().makeUneffectRequest();
			return;
		}

		if ( consumptionType == CONSUME_ZAP )
		{
			StaticEntity.getClient().makeZapRequest();
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
				return;
			case BARTENDER:
			case CLOCKWORK_BARTENDER:
				alreadyInstalled = KoLCharacter.hasBartender();
				return;
			case TOASTER:
				alreadyInstalled = KoLCharacter.hasToaster();
				return;
			case ARCHES:
				alreadyInstalled = KoLCharacter.hasArches();
				return;
		}

		if ( alreadyInstalled )
		{
			lastUpdate = "You already have one installed.";
			KoLmafia.updateDisplay( PENDING_STATE, lastUpdate );
			return;
		}

		// Check to make sure the character has the item in their
		// inventory first - if not, report the error message and
		// return from the method.

		AdventureDatabase.retrieveItem( itemUsed );
		if ( !KoLmafia.permitsContinue() )
		{
			lastUpdate = "Insufficient items to use.";
			return;
		}

		if ( !formURLString.startsWith( "inventory.php" ) )
		{
			if ( totalIterations == 1 )
				KoLmafia.updateDisplay( useTypeAsString + " " + getItemUsed().toString() + "..." );
			else
				KoLmafia.updateDisplay( useTypeAsString + " " + getItemUsed().getName() +
					" (" + currentIteration + " of " + totalIterations + ")..." );
		}

		super.run();

		// Follow the redirection and get the message;
		// instantiate a new consume item request so
		// that it processes the right result.

		if ( redirectLocation != null )
		{
			ConsumeItemRequest message = new ConsumeItemRequest( client, redirectLocation, consumptionType, itemUsed, true );
			message.run();
		}
	}

	protected void processResults()
	{
		int originalEffectCount = activeEffects.size();

		lastItemUsed = itemUsed;
		parseConsumption( responseText );

		// We might have removed - or added - an effect
		needsRefresh |= originalEffectCount != activeEffects.size();
	}

	protected static void parseConsumption( String responseText )
	{
		if ( lastItemUsed == null )
			return;

		// Assume initially that this causes the item to disappear.
		// In the event that the item is not used, then proceed to
		// undo the consumption.

		int consumptionType = TradeableItemDatabase.getConsumptionType( lastItemUsed.getItemID() );
		StaticEntity.getClient().processResult( lastItemUsed.getNegation() );

		// Check for familiar growth - if a familiar is added,
		// make sure to update the StaticEntity.getClient().

		if ( consumptionType == GROW_FAMILIAR )
		{
			if ( responseText.indexOf( "You've already got a familiar of that type." ) != -1 )
			{
				lastUpdate = "You already have that familiar.";
				KoLmafia.updateDisplay( PENDING_STATE, lastUpdate );
				StaticEntity.getClient().processResult( lastItemUsed );
				return;
			}

			// Pop up a window showing the result
			KoLCharacter.addFamiliar( FamiliarsDatabase.growFamiliarLarva( lastItemUsed.getItemID() ) );
			StaticEntity.getClient().showHTML( trimInventoryText( responseText ), "Your new familiar" );
			StaticEntity.getClient().processResult( lastItemUsed );
			return;
		}

		if ( responseText.indexOf( "You may not" ) != -1 )
		{
			KoLmafia.updateDisplay( PENDING_STATE, "Pathed ascension." );
			StaticEntity.getClient().processResult( lastItemUsed );
			return;
		}

		if ( responseText.indexOf( "rupture" ) != -1 )
		{
			KoLCharacter.reachSpleenLimit();
			lastUpdate = "Your spleen might go kabooie.";
			KoLmafia.updateDisplay( PENDING_STATE, lastUpdate );
			StaticEntity.getClient().processResult( lastItemUsed );
			return;
		}

		// Check to make sure that it wasn't a food or drink
		// that was consumed that resulted in nothing.  Eating
		// too much is flagged as a continuable state.

		if ( responseText.indexOf( "too full" ) != -1 )
		{
			lastUpdate = "Consumption limit reached.";
			KoLmafia.updateDisplay( PENDING_STATE, lastUpdate );
			StaticEntity.getClient().processResult( lastItemUsed );
			return;
		}

		if ( responseText.indexOf( "too drunk" ) != -1 )
		{
			lastUpdate = "Inebriety limit reached.";
			KoLmafia.updateDisplay( PENDING_STATE, lastUpdate );
			StaticEntity.getClient().processResult( lastItemUsed );
			return;
		}

		// Perform item-specific processing

		switch ( lastItemUsed.getItemID() )
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
					lastUpdate = "You can't open that package yet.";
					KoLmafia.updateDisplay( PENDING_STATE, lastUpdate );
					StaticEntity.getClient().processResult( lastItemUsed );
				}
				else
				{
					// Find out who sent it and popup a window showing
					// what was in the gift.

					Matcher matcher = GIFT_PATTERN.matcher( responseText );
					String title = matcher.find() ? "Gift from " + matcher.group(1) : "Your gift";
					StaticEntity.getClient().showHTML( trimInventoryText( responseText ), title );
				}

				return;

			case GATES_SCROLL:

				// You can only use a 64735 scroll if you have the
				// original dictionary in your inventory

				// "Even though your name isn't Lee, you're flattered
				// and hand over your dictionary."

				if ( responseText.indexOf( "you're flattered" ) == -1 )
					StaticEntity.getClient().processResult( lastItemUsed );
				else
					StaticEntity.getClient().processResult( FightRequest.DICTIONARY1.getNegation() );

				return;

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
					StaticEntity.getClient().processResult( lastItemUsed );

				return;

			case HEY_DEZE_MAP:

				// "Your song has pleased me greatly. I will reward you
				// with some of my crazy imps, to do your bidding."

				if ( responseText.indexOf( "pleased me greatly" ) == -1 )
				{
					lastUpdate = "You music was inadequate.";
					KoLmafia.updateDisplay( PENDING_STATE, lastUpdate );
					StaticEntity.getClient().processResult( lastItemUsed );
				}

				return;

			case GIANT_CASTLE_MAP:

				// "I'm sorry, adventurer, but the Sorceress is in
				// another castle!"

				if ( responseText.indexOf( "Sorceress is in another castle" ) == -1 )
				{
					lastUpdate = "You couldn't make it all the way to the back door.";
					KoLmafia.updateDisplay( PENDING_STATE, lastUpdate );
					StaticEntity.getClient().processResult( lastItemUsed );
				}

				return;

			case DRASTIC_HEALING:

				// If a scroll of drastic healing was used and didn't
				// crumble, it is not consumed

				StaticEntity.getClient().processResult( new AdventureResult( AdventureResult.HP, KoLCharacter.getMaximumHP() ) );

				if ( responseText.indexOf( "crumble" ) == -1 )
				{
					StaticEntity.getClient().processResult( lastItemUsed );
					KoLCharacter.updateStatus();
				}

				return;

			case TEARS:

				activeEffects.remove( KoLAdventure.BEATEN_UP );
				return;

			case ANTIDOTE:

				activeEffects.remove( POISON );
				return;

			case TINY_HOUSE:

				// Tiny houses remove lots of different effects.

				activeEffects.clear();
				return;

			case RAFFLE_TICKET:

				// The first time you use an Elf Farm Raffle ticket
				// with a ten-leaf clover in your inventory, the clover
				// disappears in a puff of smoke and you get pagoda
				// plans.

				// Subsequent raffle tickets don't consume clovers.

				if ( responseText.indexOf( "puff of smoke" ) != -1 )
					StaticEntity.getClient().processResult( SewerRequest.CLOVER );

				return;

			case KETCHUP_HOUND:

				// Successfully using a ketchup hound uses up the Hey
				// Deze nuts and pagoda plan.

				if ( responseText.indexOf( "pagoda" ) != -1 )
				{
					StaticEntity.getClient().processResult( NUTS );
					StaticEntity.getClient().processResult( PLAN );
				}

				// The ketchup hound does not go away...

				StaticEntity.getClient().processResult( lastItemUsed );
				return;

			case LUCIFER:

				// Jumbo Dr. Lucifer reduces your hit points to 1.

				StaticEntity.getClient().processResult( new AdventureResult( AdventureResult.HP, 1 - KoLCharacter.getCurrentHP() ) );
				return;

			case DOLPHIN_KING_MAP:

				// "You follow the Dolphin King's map to the bottom of
				// the sea, and find his glorious treasure."

				if ( responseText.indexOf( "find his glorious treasure" ) == -1 )
				{
					lastUpdate = "You don't have everything you need.";
					KoLmafia.updateDisplay( PENDING_STATE, lastUpdate );
					StaticEntity.getClient().processResult( lastItemUsed );
				}

				return;

			case SLUG_LORD_MAP:

				// "You make your way to the deepest part of the tank,
				// and find a chest engraved with the initials S. L."

				if ( responseText.indexOf( "deepest part of the tank" ) == -1 )
				{
					lastUpdate = "You don't have everything you need.";
					KoLmafia.updateDisplay( PENDING_STATE, lastUpdate );
					StaticEntity.getClient().processResult( lastItemUsed );
				}

				return;

			case DR_HOBO_MAP:

				// "You place it atop the Altar, and grab the Scalpel
				// at the exact same moment."

				if ( responseText.indexOf( "exact same moment" ) == -1 )
				{
					lastUpdate = "You don't have everything you need.";
					KoLmafia.updateDisplay( PENDING_STATE, lastUpdate );
					StaticEntity.getClient().processResult( lastItemUsed );
				}

				return;

			case SPOOKY_TEMPLE_MAP:

				// "You plant your Spooky Sapling in the loose soil at
				// the base of the Temple.  You spray it with your
				// Spooky-Gro Fertilizer, and it immediately grows to
				// 20 feet in height.  You can easily climb the
				// branches to reach the first step of the Temple
				// now..."

				if ( responseText.indexOf( "easily climb the branches" ) == -1 )
				{
					lastUpdate = "You don't have everything you need.";
					KoLmafia.updateDisplay( PENDING_STATE, lastUpdate );
					StaticEntity.getClient().processResult( lastItemUsed );
				}

				StaticEntity.getClient().processResult( SAPLING );
				StaticEntity.getClient().processResult( FERTILIZER );
				return;

			case DINGHY_PLANS:

				// "You need some planks to build the dinghy."

				if ( responseText.indexOf( "need some planks" ) != -1 )
				{
					lastUpdate = "You need some dingy planks.";
					KoLmafia.updateDisplay( PENDING_STATE, lastUpdate );
					StaticEntity.getClient().processResult( lastItemUsed );
				}
				else
				{
					StaticEntity.getClient().processResult( PLANKS );
				}

				return;

			case FENG_SHUI:

				// Successfully using "Feng Shui for Big Dumb Idiots"
				// consumes the decorative fountain and windchimes.

				// Only used up once

				if ( responseText.indexOf( "Feng Shui goodness" ) == -1 )
				{
					StaticEntity.getClient().processResult( lastItemUsed );
				}
				else
				{
					StaticEntity.getClient().processResult( FOUNTAIN );
					StaticEntity.getClient().processResult( WINDCHIMES );
				}

				return;

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
					StaticEntity.getClient().processResult( lastItemUsed.getInstance( lastItemUsed.getCount() - 1 ) );

				return;

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
					lastUpdate = "Your mouth is too cold.";
					KoLmafia.updateDisplay( PENDING_STATE, lastUpdate );
					StaticEntity.getClient().processResult( lastItemUsed );
				}

				return;

			case ROLLING_PIN:

				// Rolling pins remove dough from your inventory
				// They are not consumed by being used

				StaticEntity.getClient().processResult( DOUGH.getInstance( DOUGH.getCount( inventory ) ).getNegation() );
				StaticEntity.getClient().processResult( lastItemUsed );

				return;

			case UNROLLING_PIN:

				// Unrolling pins remove flat dough from your inventory
				// They are not consumed by being used

				StaticEntity.getClient().processResult( FLAT_DOUGH.getInstance( FLAT_DOUGH.getCount( inventory ) ).getNegation() );
				StaticEntity.getClient().processResult( lastItemUsed );
				return;

			case PLUS_SIGN:

				// "Following The Oracle's advice, you treat the plus
				// sign as a book, and read it."

				if ( responseText.indexOf( "you treat the plus sign as a book" ) == -1 )
				{
					lastUpdate = "You don't know how to use it.";
					KoLmafia.updateDisplay( PENDING_STATE, lastUpdate );
					StaticEntity.getClient().processResult( lastItemUsed );
				}

				return;

			case YETI_PROTEST_SIGN:

				// You don't use up a Yeti Protest Sign by protesting
				StaticEntity.getClient().processResult( lastItemUsed );
				return;

			case CHEF:
			case CLOCKWORK_CHEF:
				KoLCharacter.setChef( true );
				return;

			case BARTENDER:
			case CLOCKWORK_BARTENDER:
				KoLCharacter.setBartender( true );
				return;

			case TOASTER:
				KoLCharacter.setToaster( true );
				return;

			case ARCHES:
				KoLCharacter.setArches( true );
				return;

			case SNOWCONE_TOME:

				// "You read the incantation written on the pages of
				// the tome. Snowflakes coalesce in your
				// mind. Delicious snowflakes."

				if ( responseText.indexOf( "You read the incantation" ) == -1 )
					StaticEntity.getClient().processResult( lastItemUsed );
				else
					KoLCharacter.addAvailableSkill( new UseSkillRequest( StaticEntity.getClient(), "Summon Snowcone", "", 1 ) );

				return;


			case HILARIOUS_TOME:

				// "You pore over the tome, and sophomoric humor pours
				// into your brain. The mysteries of McPhee become
				// clear to you."

				if ( responseText.indexOf( "You pore over the tome" ) == -1 )
					StaticEntity.getClient().processResult( lastItemUsed );
				else
					KoLCharacter.addAvailableSkill( new UseSkillRequest( StaticEntity.getClient(), "Summon Hilarious Objects", "", 1 ) );

				return;

			case ASTRAL_MUSHROOM:

				// "You eat the mushroom, and are suddenly engulfed in
				// a whirling maelstrom of colors and sensations as
				// your awareness is whisked away to some strange
				// alternate dimension. Who would have thought that a
				// glowing, ethereal mushroom could have that kind of
				// effect?"

				// "Whoo, man, lemme tell you, you don't need to be
				// eating another one of those just now, okay?"

				if ( responseText.indexOf( "whirling maelstrom" ) == -1 )
					StaticEntity.getClient().processResult( lastItemUsed );

				return;
		}
	}

	private static String trimInventoryText( String text )
	{
		// Get rid of first row of first table: the "Results" line
		Matcher matcher = ROW_PATTERN.matcher( text );
		if ( matcher.find() )
			text = matcher.replaceFirst( "" );

		// Get rid of inventory listing
		matcher = INVENTORY_PATTERN.matcher( text );
		if ( matcher.find() )
			text = matcher.replaceFirst( "</table></body>" );

		return text;
	}

	public String getCommandForm()
	{
		if ( isResultPage )
			return "";

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

	private static AdventureResult extractItem( String urlString )
	{
		if ( urlString.indexOf( "inv_eat.php" ) != -1 );
		else if ( urlString.indexOf( "inv_booze.php" ) != -1 );
		else if ( urlString.indexOf( "multiuse.php" ) != -1 );
		else if ( urlString.indexOf( "skills.php" ) != -1 );
		else if ( urlString.indexOf( "inv_familiar.php" ) != -1 );
		else if ( urlString.indexOf( "inv_use.php" ) != -1 );
		else
			return null;

		Matcher itemMatcher = ITEMID_PATTERN.matcher( urlString );
		if ( !itemMatcher.find() )
			return null;

		int itemID = StaticEntity.parseInt( itemMatcher.group(1) );
		int itemCount = 1;

		if ( urlString.indexOf( "multiuse.php" ) != -1 || urlString.indexOf( "skills.php" ) != -1 )
		{
			Matcher quantityMatcher = QUANTITY_PATTERN.matcher( urlString );
			if ( quantityMatcher.find() )
				itemCount = StaticEntity.parseInt( quantityMatcher.group(1) );
		}

		return new AdventureResult( itemID, itemCount );
	}

	private static AdventureResult lastItemUsed = null;

	public static boolean processRequest( String urlString )
	{
		if ( urlString.indexOf( "inventory.php" ) != -1 && urlString.indexOf( "action=message" ) != -1 )
			return true;

		lastItemUsed = extractItem( urlString );
		if ( lastItemUsed == null )
			return false;

		int consumptionType = TradeableItemDatabase.getConsumptionType( lastItemUsed.getItemID() );
		String useTypeAsString = (consumptionType == ConsumeItemRequest.CONSUME_EAT) ? "eat " :
			(consumptionType == ConsumeItemRequest.CONSUME_DRINK) ? "drink " : "use ";

		KoLmafia.getSessionStream().println();
		KoLmafia.getSessionStream().println( useTypeAsString + lastItemUsed.getCount() + " " + lastItemUsed.getName() );
		return true;
	}
}
