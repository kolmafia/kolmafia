/**
 * Copyright (c) 2005-2007, KoLmafia development team
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
 *  [3] Neither the name "KoLmafia" nor the names of its contributors may
 *      be used to endorse or promote products derived from this software
 *      without specific prior written permission.
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

import java.util.TreeMap;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ConsumeItemRequest extends KoLRequest
{
	private static final Pattern ROW_PATTERN = Pattern.compile( "<tr>.*?</tr>" );
	private static final Pattern INVENTORY_PATTERN = Pattern.compile( "</table><table.*?</body>" );
	private static final Pattern ITEMID_PATTERN = Pattern.compile( "whichitem=(\\d+)" );
	private static final Pattern QUANTITY_PATTERN = Pattern.compile( "quantity=(\\d+)" );
	private static final Pattern FORTUNE_PATTERN = Pattern.compile( "<font size=1>Lucky numbers: (\\d+), (\\d+), (\\d+)</td>" );

	private static final TreeMap LIMITED_USES = new TreeMap();

	static
	{
		LIMITED_USES.put( new Integer( 1412 ), new AdventureResult( "Purple Tongue", 1, true ) );
		LIMITED_USES.put( new Integer( 1413 ), new AdventureResult( "Green Tongue", 1, true ) );
		LIMITED_USES.put( new Integer( 1414 ), new AdventureResult( "Orange Tongue", 1, true ) );
		LIMITED_USES.put( new Integer( 1415 ), new AdventureResult( "Red Tongue", 1, true ) );
		LIMITED_USES.put( new Integer( 1416 ), new AdventureResult( "Blue Tongue", 1, true ) );
		LIMITED_USES.put( new Integer( 1417 ), new AdventureResult( "Black Tongue", 1, true ) );

		LIMITED_USES.put( new Integer( 1622 ), new AdventureResult( "Half-Astral", 1, true ) );

		LIMITED_USES.put( new Integer( 1624 ), new AdventureResult( "Cupcake of Choice", 1, true ) );
		LIMITED_USES.put( new Integer( 1625 ), new AdventureResult( "The Cupcake of Wrath", 1, true ) );
		LIMITED_USES.put( new Integer( 1626 ), new AdventureResult( "Shiny Happy Cupcake", 1, true ) );
		LIMITED_USES.put( new Integer( 1627 ), new AdventureResult( "Tiny Bubbles in the Cupcake", 1, true ) );
		LIMITED_USES.put( new Integer( 1628 ), new AdventureResult( "Your Cupcake Senses Are Tingling", 1, true ) );

		LIMITED_USES.put( new Integer( 1650 ), TradeableItemDatabase.GOT_MILK );

		LIMITED_USES.put( new Integer( 2655 ), new AdventureResult( "Absinthe-Minded", 1, true ) );
	}

	public static String lastUpdate = "";
	private static AdventureResult lastItemUsed = null;
	private static String askedAboutOde = "";

	private static final int DOLPHIN_KING_MAP = 26;
	private static final int FORTUNE_COOKIE = 61;
	private static final int SPOOKY_TEMPLE_MAP = 74;
	private static final int TBONE_KEY = 86;
	private static final int DINGHY_PLANS = 146;
	private static final int ENCHANTED_BEAN = 186;
	private static final int FENG_SHUI = 210;
	private static final int SELTZER = 344;
	private static final int CHEF = 438;
	private static final int BARTENDER = 440;
	private static final int KETCHUP_HOUND = 493;
	private static final int RAFFLE_TICKET = 500;
	private static final int ARCHES = 504;
	private static final int HEY_DEZE_MAP = 516;
	private static final int GATES_SCROLL = 552;
	private static final int HACK_SCROLL = 553;
	private static final int LUCIFER = 571;
	private static final int TINY_HOUSE = 592;
	private static final int DRASTIC_HEALING = 595;
	private static final int SLUG_LORD_MAP = 598;
	private static final int DR_HOBO_MAP = 601;
	private static final int WARM_SUBJECT = 621;
	private static final int TOASTER = 637;
	private static final int GIANT_CASTLE_MAP = 667;
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
	private static final int MUNCHIES_PILL = 1619;
	private static final int ASTRAL_MUSHROOM = 1622;
	public static final int EXPRESS_CARD = 1687;
	private static final int DUSTY_ANIMAL_SKULL = 1799;
	private static final int QUILL_PEN = 1957;
	public static final int MACGUFFIN_DIARY = 2044;
	private static final int BLACK_MARKET_MAP = 2054;
	private static final int DRUM_MACHINE = 2328;
	private static final int COBBS_KNOB_MAP = 2442;
	private static final int JEWELRY_BOOK = 2502;
	private static final int ABSINTHE = 2655;
	private static final int MOJO_FILTER = 2614;
	private static final int LIBRARY_CARD = 2672;

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
	private static final int GIFTW = 2683;

	public static final int MILKY_POTION = 819;
	public static final int SWIRLY_POTION = 820;
	public static final int BUBBLY_POTION = 821;
	public static final int SMOKY_POTION = 822;
	public static final int CLOUDY_POTION = 823;
	public static final int EFFERVESCENT_POTION = 824;
	public static final int FIZZY_POTION = 825;
	public static final int DARK_POTION = 826;
	public static final int MURKY_POTION = 827;

	private static final int SPARKLER = 2679;
	private static final int SNAKE = 2680;
	private static final int M282 = 2681;

	private static final int STEEL_STOMACH = 2742;
	private static final int STEEL_LIVER = 2743;
	private static final int STEEL_SPLEEN = 2744;

	private static final AdventureResult ASPARAGUS_KNIFE = new AdventureResult( 19, -1 );
	private static final AdventureResult SAPLING = new AdventureResult( 75, 1 );
	private static final AdventureResult FERTILIZER = new AdventureResult( 76, 1 );
	private static final AdventureResult LOCKED_LOCKER = new AdventureResult( 84, 1 );
	private static final AdventureResult PLANKS = new AdventureResult( 140, -1 );
	private static final AdventureResult DOUGH = new AdventureResult( 159, 1 );
	private static final AdventureResult FOUNTAIN = new AdventureResult( 211, 1 );
	private static final AdventureResult FLAT_DOUGH = new AdventureResult( 301, 1 );
	private static final AdventureResult NUTS = new AdventureResult( 509, -1 );
	private static final AdventureResult PLAN = new AdventureResult( 502, -1 );
	private static final AdventureResult WINDCHIMES = new AdventureResult( 212, 1 );
	private static final AdventureResult INKWELL = new AdventureResult( 1958, -1 );
	private static final AdventureResult SCRAP_OF_PAPER = new AdventureResult( 1959, -1 );
	private static final AdventureResult WORM_RIDING_HOOKS = new AdventureResult( 2302, -1 );
	private static final AdventureResult ENCRYPTION_KEY = new AdventureResult( 2441, -1 );

	private int consumptionType;
	private AdventureResult itemUsed = null;

	public ConsumeItemRequest( AdventureResult item )
	{	this( TradeableItemDatabase.getConsumptionType( item.getItemId() ), item );
	}

	public ConsumeItemRequest( int consumptionType, AdventureResult item )
	{
		this( consumptionType == CONSUME_EAT ? "inv_eat.php" : consumptionType == CONSUME_DRINK ? "inv_booze.php" :
			consumptionType == GROW_FAMILIAR ? "inv_familiar.php" : consumptionType == MP_RESTORE ? "skills.php" :
			consumptionType == CONSUME_HOBO ? "inventory.php" : consumptionType == CONSUME_MULTIPLE ? "multiuse.php" :
			consumptionType == HP_RESTORE && item.getCount() > 1 ? "multiuse.php" : "inv_use.php", consumptionType, item );
	}

	private ConsumeItemRequest( String location, int consumptionType, AdventureResult item )
	{
		super( location, true );

		this.addFormField( "pwd" );
		this.addFormField( "whichitem", String.valueOf( item.getItemId() ) );

		this.consumptionType = consumptionType;
		this.itemUsed = item;
	}


	public int getConsumptionType()
	{	return this.consumptionType;
	}

	public AdventureResult getItemUsed()
	{	return this.itemUsed;
	}

	public static final int maximumUses( int itemId )
	{	return maximumUses( itemId, true );
	}

	public static final int maximumUses( int itemId, boolean allowOverDrink )
	{
		if ( itemId <= 0 )
			return Integer.MAX_VALUE;

		if ( itemId == MOJO_FILTER )
			return Math.max( 0, 3 - StaticEntity.getIntegerProperty( "currentMojoFilters" ) );

		if ( itemId == EXPRESS_CARD )
			return StaticEntity.getBooleanProperty( "expressCardUsed" ) ? 0 : 1;

		Integer key = new Integer( itemId );

		if ( LIMITED_USES.containsKey( key ) )
			return activeEffects.contains( LIMITED_USES.get( key ) ) ? 0 : 1;

		String itemName = TradeableItemDatabase.getItemName( itemId );

		int fullness = TradeableItemDatabase.getFullness( itemName );
		if ( fullness > 0 )
			return (KoLCharacter.getFullnessLimit() - KoLCharacter.getFullness()) / fullness;

		int spleenHit = TradeableItemDatabase.getSpleenHit( itemName );

		boolean restoresHP = false;
		float hpRestored = 0.0f;

		boolean restoresMP = false;
		float mpRestored = 0.0f;

		for ( int i = 0; i < HPRestoreItemList.CONFIGURES.length; ++i )
		{
			if ( HPRestoreItemList.CONFIGURES[i].getItem() != null && HPRestoreItemList.CONFIGURES[i].getItem().getItemId() == itemId )
			{
				restoresHP = true;
				HPRestoreItemList.CONFIGURES[i].updateHealthPerUse();
				hpRestored = (float) HPRestoreItemList.CONFIGURES[i].getHealthPerUse();
			}
		}

		for ( int i = 0; i < MPRestoreItemList.CONFIGURES.length; ++i )
		{
			if ( MPRestoreItemList.CONFIGURES[i].getItem() != null && MPRestoreItemList.CONFIGURES[i].getItem().getItemId() == itemId )
			{
				restoresMP = true;
				MPRestoreItemList.CONFIGURES[i].updateManaPerUse();
				mpRestored = (float) MPRestoreItemList.CONFIGURES[i].getManaPerUse();
			}
		}

		if ( restoresHP || restoresMP )
		{
			int maximumSuggested = 0;

			if ( hpRestored != 0.0f )
			{
				float belowMax = (float) (KoLCharacter.getMaximumHP() - KoLCharacter.getCurrentHP());
				maximumSuggested = Math.max( maximumSuggested, (int) Math.ceil( belowMax / hpRestored ) );
			}

			if ( mpRestored != 0.0f )
			{
				float belowMax = (float) (KoLCharacter.getMaximumMP() - KoLCharacter.getCurrentMP());
				maximumSuggested = Math.max( maximumSuggested, (int) Math.ceil( belowMax / mpRestored ) );
			}

			if ( spleenHit > 0 )
				maximumSuggested = Math.min( maximumSuggested, (KoLCharacter.getSpleenLimit() - KoLCharacter.getSpleenUse()) / spleenHit );

			return maximumSuggested;
		}

		if ( spleenHit > 0 )
			return (KoLCharacter.getSpleenLimit() - KoLCharacter.getSpleenUse()) / spleenHit;

		int inebrietyHit = TradeableItemDatabase.getInebriety( itemName );
		if ( inebrietyHit > 0 )
		{
			int inebrietyLeft = KoLCharacter.getInebrietyLimit() - KoLCharacter.getInebriety();
			return inebrietyLeft < 0 ? 0 : inebrietyLeft < inebrietyHit ? 1 :
				allowOverDrink ? inebrietyLeft / inebrietyHit + 1 : inebrietyLeft / inebrietyHit;
		}

		return Integer.MAX_VALUE;
	}

	public void run()
	{
		// Equipment should be handled by a different
		// kind of request.

		switch ( this.consumptionType )
		{
		case EQUIP_HAT:
		case EQUIP_WEAPON:
		case EQUIP_OFFHAND:
		case EQUIP_SHIRT:
		case EQUIP_PANTS:
		case EQUIP_ACCESSORY:
		case EQUIP_FAMILIAR:

			(new EquipmentRequest( this.itemUsed )).run();
			return;
		}

		lastUpdate = "";
		int itemId = this.itemUsed.getItemId();

		if ( itemId == SorceressLair.PUZZLE_PIECE.getItemId() )
		{
			SorceressLair.completeHedgeMaze();
			return;
		}

		int maximumUses = maximumUses( itemId );
		if ( maximumUses < this.itemUsed.getCount() )
			this.itemUsed = this.itemUsed.getInstance( maximumUses );

		if ( this.itemUsed.getCount() < 1 )
			return;

		int price = TradeableItemDatabase.getPriceById( itemId );

		if ( itemId == SELTZER )
			SpecialOutfit.createImplicitCheckpoint();

		if ( price != 0 && this.consumptionType != INFINITE_USES && !AdventureDatabase.retrieveItem( this.itemUsed ) )
		{
			if ( itemId == SELTZER )
				SpecialOutfit.restoreImplicitCheckpoint();

			return;
		}

		if ( itemId == SELTZER )
			SpecialOutfit.restoreImplicitCheckpoint();

		int iterations = 1;

		if ( this.itemUsed.getCount() != 1 )
		{
			switch ( this.consumptionType )
			{
			case CONSUME_MULTIPLE:
			case HP_RESTORE:
			case MP_RESTORE:
			case HPMP_RESTORE:
				break;
			default:
				iterations = this.itemUsed.getCount();
				this.itemUsed = this.itemUsed.getInstance( 1 );
			}
		}

		if ( itemId == MACGUFFIN_DIARY )
		{
			VISITOR.constructURLString( "diary.php?textversion=1" ).run();
			KoLmafia.updateDisplay( "Your father's diary has been read." );
			return;
		}

		String useTypeAsString = (this.consumptionType == CONSUME_EAT) ? "Eating" :
			(this.consumptionType == CONSUME_DRINK) ? "Drinking" : "Using";

		String originalURLString = this.getURLString();

		for ( int i = 1; i <= iterations && KoLmafia.permitsContinue(); ++i )
		{
			this.constructURLString( originalURLString );

			if ( this.consumptionType == CONSUME_DRINK && !allowBoozeConsumption( TradeableItemDatabase.getInebriety( this.itemUsed.getName() ) ) )
				return;

			this.useOnce( i, iterations, useTypeAsString );
		}

		if ( KoLmafia.permitsContinue() )
		{
			KoLmafia.updateDisplay( "Finished " + useTypeAsString.toLowerCase() + " " + Math.max( iterations, this.itemUsed.getCount() ) +
				" " + this.itemUsed.getName() + "." );
		}
	}

	public static final boolean allowBoozeConsumption( int inebrietyBonus )
	{
		if ( KoLCharacter.isFallingDown() || inebrietyBonus < 1 )
			return true;

		if ( existingFrames.isEmpty() )
			return true;

		// Make sure the player does not drink something without
		// having ode, if they can cast ode.

		if ( !activeEffects.contains( TradeableItemDatabase.ODE ) )
		{
			if ( availableSkills.contains( UseSkillRequest.getInstance( "The Ode to Booze" ) ) && !askedAboutOde.equals( KoLCharacter.getUserName() ) )
			{
				if ( !KoLFrame.confirm( "Are you sure you want to drink without ode?" ) )
					return false;

				askedAboutOde = KoLCharacter.getUserName();
			}
		}

		// Make sure the player does not overdrink if they still
		// have PvP attacks remaining.

		if ( KoLCharacter.getInebriety() + inebrietyBonus > KoLCharacter.getInebrietyLimit() )
		{
			if ( KoLCharacter.getAttacksLeft() > 0 && !KoLFrame.confirm( "Are you sure you want to overdrink without PvPing?" ) )
				return false;
			else if ( KoLCharacter.getAdventuresLeft() > 40 && !KoLFrame.confirm( "Are you sure you want to overdrink?" ) )
				return false;
		}

		return true;
	}

	public void useOnce( int currentIteration, int totalIterations, String useTypeAsString )
	{
		lastUpdate = "";

		if ( this.consumptionType == CONSUME_ZAP )
		{
			StaticEntity.getClient().makeZapRequest();
			return;
		}

		// Check to make sure the character has the item in their
		// inventory first - if not, report the error message and
		// return from the method.

		if ( !AdventureDatabase.retrieveItem( this.itemUsed ) )
		{
			lastUpdate = "Insufficient items to use.";
			return;
		}

		switch ( this.consumptionType )
		{
		case HP_RESTORE:
			if ( this.itemUsed.getCount() > 1 )
			{
				this.addFormField( "action", "useitem" );
				this.addFormField( "quantity", String.valueOf( this.itemUsed.getCount() ) );
			}
			else
			{
				this.addFormField( "which", "3" );
			}

		case CONSUME_MULTIPLE:
			this.addFormField( "action", "useitem" );
			this.addFormField( "quantity", String.valueOf( this.itemUsed.getCount() ) );
			break;

		case MP_RESTORE:
			this.addFormField( "action", "useitem" );
			this.addFormField( "itemquantity", String.valueOf( this.itemUsed.getCount() ) );
			break;

		case CONSUME_HOBO:
			this.addFormField( "action", "hobo" );
			this.addFormField( "which", "1" );
			break;

		case CONSUME_EAT:
		case CONSUME_DRINK:
			this.addFormField( "which", "1" );
			break;

		default:
			this.addFormField( "which", "3" );
			break;
		}

		if ( totalIterations == 1 )
			KoLmafia.updateDisplay( useTypeAsString + " " + this.itemUsed.getCount() + " " + this.itemUsed.getName() + "..." );
		else
			KoLmafia.updateDisplay( useTypeAsString + " " + this.itemUsed.getName() + " (" + currentIteration + " of " + totalIterations + ")..." );

		// Run to see if booze consumption is permitted
		// based on the user's current settings.

		super.run();
	}

	public void processResults()
	{
		lastItemUsed = this.itemUsed;
		parseConsumption( this.responseText, true );
	}

	public static final void parseConsumption( String responseText, boolean showHTML )
	{
		if ( lastItemUsed == null )
			return;

		int consumptionType = TradeableItemDatabase.getConsumptionType( lastItemUsed.getItemId() );

		if ( consumptionType == INFINITE_USES )
			return;

		if ( consumptionType == MESSAGE_DISPLAY )
		{
			if ( !LoginRequest.isInstanceRunning() )
				showItemUsage( showHTML, responseText, true );

			return;
		}

		// Assume initially that this causes the item to disappear.
		// In the event that the item is not used, then proceed to
		// undo the consumption.

		StaticEntity.getClient().processResult( lastItemUsed.getNegation() );

		// Check for familiar growth - if a familiar is added,
		// make sure to update the StaticEntity.getClient().

		if ( consumptionType == GROW_FAMILIAR )
		{
			if ( responseText.indexOf( "You've already got a familiar of that type." ) != -1 )
			{
				lastUpdate = "You already have that familiar.";
				KoLmafia.updateDisplay( ERROR_STATE, lastUpdate );
				StaticEntity.getClient().processResult( lastItemUsed );
				return;
			}

			// Pop up a window showing the result

			KoLCharacter.addFamiliar( FamiliarsDatabase.growFamiliarLarva( lastItemUsed.getItemId() ) );
			showItemUsage( showHTML, responseText, true );
			return;
		}

		if ( responseText.indexOf( "You may not" ) != -1 )
		{
			KoLmafia.updateDisplay( ERROR_STATE, "Pathed ascension." );
			StaticEntity.getClient().processResult( lastItemUsed );
			return;
		}

		if ( responseText.indexOf( "rupture" ) != -1 )
		{
			lastUpdate = "Your spleen might go kabooie.";
			KoLmafia.updateDisplay( ERROR_STATE, lastUpdate );
			StaticEntity.getClient().processResult( lastItemUsed );
			return;
		}

		// Check to make sure that it wasn't a food or drink
		// that was consumed that resulted in nothing.  Eating
		// too much is flagged as a continuable state.

		if ( responseText.indexOf( "too full" ) != -1 )
		{
			lastUpdate = "Consumption limit reached.";
			KoLmafia.updateDisplay( ERROR_STATE, lastUpdate );
			StaticEntity.getClient().processResult( lastItemUsed );
			return;
		}

		if ( responseText.indexOf( "too drunk" ) != -1 )
		{
			lastUpdate = "Inebriety limit reached.";
			KoLmafia.updateDisplay( ERROR_STATE, lastUpdate );
			StaticEntity.getClient().processResult( lastItemUsed );
			return;
		}

		// Perform item-specific processing

		switch ( lastItemUsed.getItemId() )
		{

		// If it's a gift package, get the inner message

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
		case GIFTW:

			// "You can't receive things from other players
			// right now."

			if ( responseText.indexOf( "You can't receive things" ) != -1 )
			{
				lastUpdate = "You can't open that package yet.";
				KoLmafia.updateDisplay( ERROR_STATE, lastUpdate );
				StaticEntity.getClient().processResult( lastItemUsed );
			}
			else if ( showHTML )
			{
				// Find out who sent it and popup a window showing
				// what was in the gift.

				showItemUsage( true, responseText, true );
			}

			return;

		// If it's a fortune cookie, get the fortune

		case FORTUNE_COOKIE:

			showItemUsage( showHTML, responseText, true );

			Matcher fortuneMatcher = FORTUNE_PATTERN.matcher( responseText );
			if ( !fortuneMatcher.find() )
				return;

			if ( StaticEntity.isCounting( "Fortune Cookie" ) )
			{
				int desiredCount = 0;
				for ( int i = 1; i <= 3; ++i )
					if ( StaticEntity.isCounting( "Fortune Cookie", StaticEntity.parseInt( fortuneMatcher.group(i) ) ) )
						desiredCount = StaticEntity.parseInt( fortuneMatcher.group(i) );

				if ( desiredCount != 0 )
				{
					StaticEntity.stopCounting( "Fortune Cookie" );
					StaticEntity.startCounting( desiredCount, "Fortune Cookie", "fortune.gif" );
					return;
				}
			}

			for ( int i = 1; i <= 3; ++i )
				StaticEntity.startCounting( StaticEntity.parseInt( fortuneMatcher.group(i) ), "Fortune Cookie", "fortune.gif" );

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

		case HACK_SCROLL:

			// "The UB3r 31337 HaX0R stands before you."

			if ( responseText.indexOf( "The UB3r 31337 HaX0R stands before you." ) != -1 )
				StaticEntity.getClient().processResult( lastItemUsed.getInstance( lastItemUsed.getCount() - 1 ) );

			return;

		case SPARKLER:
		case SNAKE:
		case M282:

			// "You've already celebrated the Fourth of Bor, and
			// now it's time to get back to work."

			// "Sorry, but these particular fireworks are illegal
			// on any day other than the Fourth of Bor. And the law
			// is a worthy institution, and you should respect and
			// obey it, no matter what."

			if ( responseText.indexOf( "back to work" ) != -1 || responseText.indexOf( "fireworks are illegal" ) != -1 )
				StaticEntity.getClient().processResult( lastItemUsed );

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

		case LIBRARY_CARD:

			// If you've already used a library card today, it is
			// not consumed.

			// "You head back to the library, but can't find
			// anything you feel like reading. You skim a few
			// celebrity-gossip magazines, and end up feeling kind
			// of dirty."

			if ( responseText.indexOf( "feeling kind of dirty" ) == -1 )
				StaticEntity.getClient().processResult( lastItemUsed );

			return;

		case HEY_DEZE_MAP:

			// "Your song has pleased me greatly. I will reward you
			// with some of my crazy imps, to do your bidding."

			if ( responseText.indexOf( "pleased me greatly" ) == -1 )
			{
				lastUpdate = "You music was inadequate.";
				KoLmafia.updateDisplay( ERROR_STATE, lastUpdate );
				StaticEntity.getClient().processResult( lastItemUsed );
			}

			return;

		case GIANT_CASTLE_MAP:

			// "I'm sorry, adventurer, but the Sorceress is in
			// another castle!"

			if ( responseText.indexOf( "Sorceress is in another castle" ) == -1 )
			{
				lastUpdate = "You couldn't make it all the way to the back door.";
				KoLmafia.updateDisplay( ERROR_STATE, lastUpdate );
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

			AdventureResult [] effects = new AdventureResult[ activeEffects.size() ];
			activeEffects.toArray( effects );

			for ( int i = 0; i < effects.length; ++i )
				if ( effects[i].getName().toLowerCase().indexOf( "poison" ) != -1 )
					activeEffects.remove( effects[i] );

			return;

		case TINY_HOUSE:

			// Tiny houses remove lots of different effects.

			activeEffects.clear();
			return;

		case TBONE_KEY:

			if ( KoLCharacter.hasItem( LOCKED_LOCKER ) )
				StaticEntity.getClient().processResult( LOCKED_LOCKER.getNegation() );
			else
				StaticEntity.getClient().processResult( lastItemUsed );


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
				KoLmafia.updateDisplay( ERROR_STATE, lastUpdate );
				StaticEntity.getClient().processResult( lastItemUsed );
			}

			return;

		case SLUG_LORD_MAP:

			// "You make your way to the deepest part of the tank,
			// and find a chest engraved with the initials S. L."

			if ( responseText.indexOf( "deepest part of the tank" ) == -1 )
			{
				lastUpdate = "You don't have everything you need.";
				KoLmafia.updateDisplay( ERROR_STATE, lastUpdate );
				StaticEntity.getClient().processResult( lastItemUsed );
			}

			return;

		case DR_HOBO_MAP:

			// "You place it atop the Altar, and grab the Scalpel
			// at the exact same moment."

			if ( responseText.indexOf( "exact same moment" ) == -1 )
			{
				lastUpdate = "You don't have everything you need.";
				KoLmafia.updateDisplay( ERROR_STATE, lastUpdate );
				StaticEntity.getClient().processResult( lastItemUsed );
				return;
			}

			// Using the map consumes an asparagus knife

			StaticEntity.getClient().processResult( ASPARAGUS_KNIFE );
			return;

		case COBBS_KNOB_MAP:

			// "You memorize the location of the door, then eat
			// both the map and the encryption key."

			if ( responseText.indexOf( "memorize the location" ) == -1 )
			{
				lastUpdate = "You don't have everything you need.";
				KoLmafia.updateDisplay( ERROR_STATE, lastUpdate );
				StaticEntity.getClient().processResult( lastItemUsed );
				return;
			}

			// Using the map consumes the encryption key

			StaticEntity.getClient().processResult( ENCRYPTION_KEY );
			return;

		case BLACK_MARKET_MAP:

			// "You try to follow the map, but you can't make head
			// or tail of it. It keeps telling you to take paths
			// through completely impenetrable foliage.
			// What was it that the man in black told you about the
			// map? Something about "as the crow flies?""

			if ( responseText.indexOf( "can't make head or tail of it" ) != -1 )
			{
				lastUpdate = "You need a guide.";
				KoLmafia.updateDisplay( ERROR_STATE, lastUpdate );
				StaticEntity.getClient().processResult( lastItemUsed );
				return;
			}

			// This only works if you had a Reassembled Blackbird
			// familiar. When it works, the familiar disappears.

			FamiliarData blackbird = KoLCharacter.getFamiliar();
			KoLCharacter.removeFamiliar( blackbird );

			AdventureResult item = blackbird.getItem();
			if ( item != null )
				AdventureResult.addResultToList( inventory, item );

			if ( !StaticEntity.getProperty( "preBlackbirdFamiliar" ).equals( "" ) )
			{
				KoLmafiaCLI.DEFAULT_SHELL.executeCommand( "familiar", StaticEntity.getProperty( "preBlackbirdFamiliar" ) );
				if ( item != null && KoLCharacter.getFamiliar().canEquip( item ) )
					(new EquipmentRequest( item )).run();

				StaticEntity.setProperty( "preBlackbirdFamiliar", "" );
			}

			QuestLogRequest.setBlackMarketAvailable();
			return;

		case SPOOKY_TEMPLE_MAP:

			if ( KoLCharacter.hasItem( SAPLING ) && KoLCharacter.hasItem( FERTILIZER ) )
			{
				StaticEntity.getClient().processResult( SAPLING.getNegation() );
				StaticEntity.getClient().processResult( FERTILIZER.getNegation() );
			}
			else
			{
				lastUpdate = "You don't have everything you need.";
				KoLmafia.updateDisplay( ERROR_STATE, lastUpdate );
				StaticEntity.getClient().processResult( lastItemUsed );
			}

			return;

		case DINGHY_PLANS:

			// "You need some planks to build the dinghy."

			if ( KoLCharacter.hasItem( PLANKS ) )
			{
				StaticEntity.getClient().processResult( PLANKS );
			}
			else
			{
				lastUpdate = "You need some dingy planks.";
				KoLmafia.updateDisplay( ERROR_STATE, lastUpdate );
				StaticEntity.getClient().processResult( lastItemUsed );
			}

			return;

		case FENG_SHUI:

			if ( KoLCharacter.hasItem( FOUNTAIN ) && KoLCharacter.hasItem( WINDCHIMES ) )
			{
				StaticEntity.getClient().processResult( FOUNTAIN.getNegation() );
				StaticEntity.getClient().processResult( WINDCHIMES.getNegation() );
			}
			else
			{
				StaticEntity.getClient().processResult( lastItemUsed );
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
				KoLmafia.updateDisplay( ERROR_STATE, lastUpdate );
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
				KoLmafia.updateDisplay( ERROR_STATE, lastUpdate );
				StaticEntity.getClient().processResult( lastItemUsed );
			}

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
			{
				StaticEntity.getClient().processResult( lastItemUsed );
				return;
			}

			KoLCharacter.addAvailableSkill( UseSkillRequest.getInstance( "Summon Snowcone" ) );
			usableSkills.sort();
			return;


		case HILARIOUS_TOME:

			// "You pore over the tome, and sophomoric humor pours
			// into your brain. The mysteries of McPhee become
			// clear to you."

			if ( responseText.indexOf( "You pore over the tome" ) == -1 )
			{
				StaticEntity.getClient().processResult( lastItemUsed );
				return;
			}

			KoLCharacter.addAvailableSkill( UseSkillRequest.getInstance( "Summon Hilarious Objects" ) );
			usableSkills.sort();
			return;

		case JEWELRY_BOOK:

			// "You read the book, and learn all sorts of advanced
			// jewelry-making techniques."

			if ( responseText.indexOf( "You read the book" ) == -1 )
				StaticEntity.getClient().processResult( lastItemUsed );
			else
				KoLCharacter.addAvailableSkill( UseSkillRequest.getInstance( "Really Expensive Jewelrycrafting" ) );

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

		case ABSINTHE:

			// "You drink the bottle of absinthe. It tastes like
			// licorice, pain, and green. Your head begins to ache
			// and you see a green glow in the general direction of
			// the distant woods."

			// "No way are you gonna drink another one of those
			// until the last one wears off."

			if ( responseText.indexOf( "licorice" ) == -1 )
				StaticEntity.getClient().processResult( lastItemUsed );

			return;

		case DUSTY_ANIMAL_SKULL:

			// The magic that had previously animated the animals kicks back
			// in, and it stands up shakily and looks at you. "Graaangh?"

			if ( responseText.indexOf( "Graaangh?" ) == -1 )
			{
				lastUpdate = "You're missing some parts.";
				KoLmafia.updateDisplay( ERROR_STATE, lastUpdate );
				StaticEntity.getClient().processResult( lastItemUsed );
				return;
			}

			// Remove the other 98 bones

			for ( int i = 1802; i < 1900; ++i )
				StaticEntity.getClient().processResult( new AdventureResult( i, -1 ) );

			return;

		case QUILL_PEN:

			if ( responseText.indexOf( "You acquire" ) == -1 )
			{
				lastUpdate = "You're missing some parts.";
				KoLmafia.updateDisplay( ERROR_STATE, lastUpdate );
				StaticEntity.getClient().processResult( lastItemUsed );
				return;
			}

			// It worked. Also remove the ink and paper.
			StaticEntity.getClient().processResult( INKWELL.getInstance( 0 - lastItemUsed.getCount() ) );
			StaticEntity.getClient().processResult( SCRAP_OF_PAPER.getInstance( 0 - lastItemUsed.getCount() ) );
			return;

		case DRUM_MACHINE:

			// "You don't have time to play the drums."

			if ( responseText.indexOf( "don't have time" ) != -1 )
			{
				lastUpdate = "Insufficient adventures left.";
				KoLmafia.updateDisplay( ERROR_STATE, lastUpdate );
				StaticEntity.getClient().processResult( lastItemUsed );
				return;
			}

			// "Dammit! Your hooks were still on there! Oh well. At
			// least now you know where the pyramid is."
			if ( responseText.indexOf( "hooks were still on" ) != -1 )
			{
				// You lose your weapon
				KoLCharacter.setEquipment( KoLCharacter.WEAPON, EquipmentRequest.UNEQUIP );
				AdventureResult.addResultToList( inventory, WORM_RIDING_HOOKS.getInstance(1) );
				StaticEntity.getClient().processResult( WORM_RIDING_HOOKS );
				return;
			}

			// You are redirected into a fight. Do we handle it?
			return;

		case STEEL_STOMACH:

			if ( responseText.indexOf( "You acquire a skill" ) != -1 )
				KoLCharacter.addAvailableSkill( UseSkillRequest.getInstance( "Stomach of Steel" ) );
			return;

		case STEEL_LIVER:

			if ( responseText.indexOf( "You acquire a skill" ) != -1 )
				KoLCharacter.addAvailableSkill( UseSkillRequest.getInstance( "Liver of Steel" ) );
			return;

		case STEEL_SPLEEN:

			if ( responseText.indexOf( "You acquire a skill" ) != -1 )
				KoLCharacter.addAvailableSkill( UseSkillRequest.getInstance( "Spleen of Steel" ) );
			return;

		case MOJO_FILTER:

			// You strain some of the toxins out of your mojo, and discard
			// the now-grodulated filter.

			if ( responseText.indexOf( "now-grodulated" ) == -1 )
			{
				StaticEntity.getClient().processResult( lastItemUsed );
				return;
			}

			StaticEntity.setProperty( "currentMojoFilters",
				String.valueOf( StaticEntity.getIntegerProperty( "currentMojoFilters" ) + lastItemUsed.getCount() ) );

			StaticEntity.setProperty( "currentSpleenUse", String.valueOf( Math.max( 0,
				StaticEntity.getIntegerProperty( "currentSpleenUse" ) - lastItemUsed.getCount() ) ) );

			return;

		case MILKY_POTION:
		case SWIRLY_POTION:
		case BUBBLY_POTION:
		case SMOKY_POTION:
		case CLOUDY_POTION:
		case EFFERVESCENT_POTION:
		case FIZZY_POTION:
		case DARK_POTION:
		case MURKY_POTION:

			String effectData = null;

			if ( responseText.indexOf( "liquid fire" ) != -1 )
				effectData = "inebriety";
			else if ( responseText.indexOf( "You gain" ) != -1 )
				effectData = "healing";
			else if ( responseText.indexOf( "Confused" ) != -1 )
				effectData = "confusion";
			else if ( responseText.indexOf( "Izchak's Blessing" ) != -1 )
				effectData = "blessing";
			else if ( responseText.indexOf( "Object Detection" ) != -1 )
				effectData = "detection";
			else if ( responseText.indexOf( "Sleepy" ) != -1 )
				effectData = "sleepiness";
			else if ( responseText.indexOf( "Strange Mental Acuity" ) != -1 )
				effectData = "mental acuity";
			else if ( responseText.indexOf( "Strength of Ten Ettins" ) != -1 )
				effectData = "ettin strength";
			else if ( responseText.indexOf( "Teleportitis" ) != -1 )
				effectData = "teleportitis";

			ensureUpdatedPotionEffects();

			if ( effectData != null )
				StaticEntity.setProperty( "lastBangPotion" + lastItemUsed.getItemId(), effectData );

			return;
		}
	}

	public static final void ensureUpdatedPotionEffects()
	{
		int lastAscension = StaticEntity.getIntegerProperty( "lastBangPotionReset" );
		if ( lastAscension < KoLCharacter.getAscensions() )
		{
			StaticEntity.setProperty( "lastBangPotionReset", String.valueOf( KoLCharacter.getAscensions() ) );
			for ( int i = 819; i <= 827; ++i )
				StaticEntity.setProperty( "lastBangPotion" + i, "" );
		}
	}

	public static final String bangPotionName( int itemId )
	{	return bangPotionName( itemId, TradeableItemDatabase.getItemName( itemId ) );
	}

	public static final String bangPotionName( int itemId, String name )
	{
		ensureUpdatedPotionEffects();
		String effect = StaticEntity.getProperty( "lastBangPotion" + itemId );
		if ( effect.equals( "" ) )
			return name;

		return name + " of " + effect;
	}

	private static final void showItemUsage( boolean showHTML, String text, boolean consumed )
	{
		if ( showHTML )
			StaticEntity.getClient().showHTML( trimInventoryText( text ) );

		if ( !consumed )
			StaticEntity.getClient().processResult( lastItemUsed );
	}

	private static final String trimInventoryText( String text )
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

	private static final AdventureResult extractItem( String urlString )
	{
		if ( urlString.startsWith( "inv_eat.php" ) );
		else if ( urlString.startsWith( "inv_booze.php" ) );
		else if ( urlString.startsWith( "multiuse.php" ) );
		else if ( urlString.startsWith( "skills.php" ) );
		else if ( urlString.startsWith( "inv_familiar.php" ) );
		else if ( urlString.startsWith( "inv_use.php" ) );
		else
			return null;

		Matcher itemMatcher = ITEMID_PATTERN.matcher( urlString );
		if ( !itemMatcher.find() )
			return null;

		int itemId = StaticEntity.parseInt( itemMatcher.group(1) );
		if ( TradeableItemDatabase.getItemName( itemId ) == null )
			return null;

		int itemCount = 1;

		if ( urlString.indexOf( "multiuse.php" ) != -1 || urlString.indexOf( "skills.php" ) != -1 )
		{
			Matcher quantityMatcher = QUANTITY_PATTERN.matcher( urlString );
			if ( quantityMatcher.find() )
				itemCount = StaticEntity.parseInt( quantityMatcher.group(1) );
		}


		return new AdventureResult( itemId, itemCount );
	}

	public static final boolean registerRequest( String urlString )
	{
		lastItemUsed = extractItem( urlString );
		if ( lastItemUsed == null )
			return false;

		int consumptionType = TradeableItemDatabase.getConsumptionType( lastItemUsed.getItemId() );
		String useTypeAsString = (consumptionType == CONSUME_EAT) ? "eat " :
			(consumptionType == CONSUME_DRINK) ? "drink " : "use ";

		if ( consumptionType == CONSUME_EAT )
		{
			int fullness = TradeableItemDatabase.getFullness( lastItemUsed.getName() );
			if ( fullness > 0 && KoLCharacter.getFullness() + fullness <= KoLCharacter.getFullnessLimit() )
			{
				StaticEntity.setProperty( "currentFullness", String.valueOf( KoLCharacter.getFullness() + fullness ) );
				StaticEntity.setProperty( "munchiesPillsUsed",
					String.valueOf( Math.max( StaticEntity.getIntegerProperty( "munchiesPillsUsed" ) - 1, 0 ) ) );
			}
		}
		else
		{
			if ( lastItemUsed.getItemId() == EXPRESS_CARD )
				StaticEntity.setProperty( "expressCardUsed", "true" );

			if ( lastItemUsed.getItemId() == MUNCHIES_PILL )
			{
				StaticEntity.setProperty( "munchiesPillsUsed",
					String.valueOf( StaticEntity.getIntegerProperty( "munchiesPillsUsed" ) + lastItemUsed.getCount() ) );
			}

			int spleenHit = TradeableItemDatabase.getSpleenHit( lastItemUsed.getName() ) * lastItemUsed.getCount();
			if ( spleenHit > 0 && KoLCharacter.getSpleenUse() + spleenHit <= KoLCharacter.getSpleenLimit() )
				StaticEntity.setProperty( "currentSpleenUse", String.valueOf( KoLCharacter.getSpleenUse() + spleenHit ) );
		}

		if ( lastItemUsed.getItemId() == BLACK_MARKET_MAP && KoLCharacter.getFamiliar().getId() != 59 )
		{
			AdventureResult map = lastItemUsed;
			FamiliarData blackbird = new FamiliarData( 59 );
			if ( !KoLCharacter.getFamiliarList().contains( blackbird ) )
				(new ConsumeItemRequest( new AdventureResult( 2052, 1 ) )).run();

			if ( !KoLmafia.permitsContinue() )
			{
				lastItemUsed = map;
				return true;
			}

			(new FamiliarRequest( blackbird )).run();
			lastItemUsed = map;
		}

		RequestLogger.updateSessionLog();
		RequestLogger.updateSessionLog( useTypeAsString + lastItemUsed.getCount() + " " + lastItemUsed.getName() );
		return true;
	}
}
