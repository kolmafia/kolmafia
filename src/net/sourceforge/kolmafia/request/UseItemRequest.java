/**
 * Copyright (c) 2005-2008, KoLmafia development team
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

package net.sourceforge.kolmafia.request;

import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.FamiliarData;
import net.sourceforge.kolmafia.HPRestoreItemList;
import net.sourceforge.kolmafia.KoLAdventure;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.KoLmafiaCLI;
import net.sourceforge.kolmafia.MPRestoreItemList;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.SpecialOutfit;
import net.sourceforge.kolmafia.StaticEntity;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.session.EquipmentManager;
import net.sourceforge.kolmafia.session.InventoryManager;
import net.sourceforge.kolmafia.session.ResultProcessor;
import net.sourceforge.kolmafia.session.SorceressLairManager;
import net.sourceforge.kolmafia.utilities.InputFieldUtilities;
import net.sourceforge.kolmafia.utilities.StringUtilities;

import net.sourceforge.kolmafia.persistence.FamiliarDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.persistence.Preferences;
import net.sourceforge.kolmafia.persistence.SkillDatabase;

public class UseItemRequest
	extends GenericRequest
{
	private static final GenericRequest REDIRECT_REQUEST = new GenericRequest( "inventory.php?action=message" );

	private static final Pattern ROW_PATTERN = Pattern.compile( "<tr>.*?</tr>" );
	private static final Pattern INVENTORY_PATTERN = Pattern.compile( "</blockquote></td></tr></table>.*?</body>" );
	private static final Pattern ITEMID_PATTERN = Pattern.compile( "whichitem=(\\d+)" );
	private static final Pattern QUANTITY_PATTERN = Pattern.compile( "quantity=(\\d+)" );
	private static final Pattern FORTUNE_PATTERN =
		Pattern.compile( "<font size=1>(Lucky numbers: (\\d+), (\\d+), (\\d+))</td>" );

	private static final TreeMap LIMITED_USES = new TreeMap();

	static
	{
		UseItemRequest.LIMITED_USES.put( new Integer( 1412 ), new AdventureResult( "Purple Tongue", 1, true ) );
		UseItemRequest.LIMITED_USES.put( new Integer( 1413 ), new AdventureResult( "Green Tongue", 1, true ) );
		UseItemRequest.LIMITED_USES.put( new Integer( 1414 ), new AdventureResult( "Orange Tongue", 1, true ) );
		UseItemRequest.LIMITED_USES.put( new Integer( 1415 ), new AdventureResult( "Red Tongue", 1, true ) );
		UseItemRequest.LIMITED_USES.put( new Integer( 1416 ), new AdventureResult( "Blue Tongue", 1, true ) );
		UseItemRequest.LIMITED_USES.put( new Integer( 1417 ), new AdventureResult( "Black Tongue", 1, true ) );

		UseItemRequest.LIMITED_USES.put( new Integer( 1622 ), new AdventureResult( "Half-Astral", 1, true ) );

		UseItemRequest.LIMITED_USES.put( new Integer( 1624 ), new AdventureResult( "Cupcake of Choice", 1, true ) );
		UseItemRequest.LIMITED_USES.put( new Integer( 1625 ), new AdventureResult( "The Cupcake of Wrath", 1, true ) );
		UseItemRequest.LIMITED_USES.put( new Integer( 1626 ), new AdventureResult( "Shiny Happy Cupcake", 1, true ) );
		UseItemRequest.LIMITED_USES.put( new Integer( 1627 ), new AdventureResult(
			"Tiny Bubbles in the Cupcake", 1, true ) );
		UseItemRequest.LIMITED_USES.put( new Integer( 1628 ), new AdventureResult(
			"Your Cupcake Senses Are Tingling", 1, true ) );

		UseItemRequest.LIMITED_USES.put( new Integer( 1650 ), ItemDatabase.GOT_MILK );

		UseItemRequest.LIMITED_USES.put( new Integer( 2655 ), new AdventureResult( "Absinthe-Minded", 1, true ) );
	}

	public static String lastUpdate = "";
	private static AdventureResult lastItemUsed = null;
	private static int askedAboutOde = 0;

	private static final int DOLPHIN_KING_MAP = 26;
	private static final int FORTUNE_COOKIE = 61;
	private static final int SPOOKY_TEMPLE_MAP = 74;
	private static final int TBONE_KEY = 86;
	private static final int DINGHY_PLANS = 146;
	private static final int ENCHANTED_BEAN = 186;
	private static final int FENG_SHUI = 210;
	private static final int SELTZER = 344;
	private static final int PIRATE_SKULL = 407;
	private static final int CHEF = 438;
	private static final int BARTENDER = 440;
	private static final int KETCHUP_HOUND = 493;
	private static final int RAFFLE_TICKET = 500;
	private static final int HEY_DEZE_MAP = 516;
	private static final int GATES_SCROLL = 552;
	public static final int HACK_SCROLL = 553;
	private static final int LUCIFER = 571;
	private static final int TINY_HOUSE = 592;
	private static final int DRASTIC_HEALING = 595;
	private static final int SLUG_LORD_MAP = 598;
	private static final int DR_HOBO_MAP = 601;
	private static final int WARM_SUBJECT = 621;
	private static final int TOASTER = 637;
	private static final int GIANT_CASTLE_MAP = 667;
	private static final int MAFIA_ARIA = 781;
	private static final int ANTIDOTE = 829;
	private static final int TEARS = 869;
	private static final int ROLLING_PIN = 873;
	private static final int UNROLLING_PIN = 874;
	private static final int PLUS_SIGN = 818;
	private static final int MAID = 1000;
	private static final int CLOCKWORK_BARTENDER = 1111;
	private static final int CLOCKWORK_CHEF = 1112;
	private static final int CLOCKWORK_MAID = 1113;
	private static final int PURPLE = 1412;
	private static final int GREEN = 1413;
	private static final int ORANGE = 1414;
	private static final int RED = 1415;
	private static final int BLUE = 1416;
	private static final int BLACK = 1417;
	private static final int MUNCHIES_PILL = 1619;
	private static final int ASTRAL_MUSHROOM = 1622;
	private static final int DUSTY_ANIMAL_SKULL = 1799;
	private static final int QUILL_PEN = 1957;
	public static final int MACGUFFIN_DIARY = 2044;
	private static final int BLACK_MARKET_MAP = 2054;
	public static final int DRUM_MACHINE = 2328;
	public static final int BLACK_PUDDING = 2338;
	private static final int COBBS_KNOB_MAP = 2442;
	private static final int TELESCOPE = 2599;
	private static final int ABSINTHE = 2655;
	private static final int MOJO_FILTER = 2614;
	private static final int LIBRARY_CARD = 2672;
	public static final int CARONCH_MAP = 2950;
	private static final int ANCIENT_CURSED_FOOTLOCKER = 3016;
	private static final int ORNATE_CURSED_CHEST = 3017;
	private static final int GILDED_CURSED_CHEST = 3018;
	public static final int CURSED_PIECE_OF_THIRTEEN = 3034;
	private static final int GENERAL_ASSEMBLY_MODULE = 3075;

	public static final int ICE_SICKLE = 1424;
	public static final int ICE_BABY = 1425;
	public static final int ICE_PICK = 1426;
	public static final int ICE_SKATES = 1427;

	public static final int LIARS_PANTS = 2222;
	public static final int JUGGLERS_BALLS = 2223;
	public static final int PINK_SHIRT = 2224;
	public static final int FLAMING_FAMILIAR = 2225;
	public static final int EYEBALL_PENDANT = 2226;

	public static final int FORTUNE_TELLER = 3193;
	public static final int GENTLEMENS_MAGAZINE = 3194;
	public static final int PAPER_SHURIKEN = 3195;
	public static final int ORIGAMI_PASTIES = 3196;
	public static final int RIDING_CROP = 3197;

	// Books

	private static final int SNOWCONE_BOOK = 1411;
	private static final int HILARIOUS_BOOK = 1498;
	private static final int CANDY_BOOK = 2303;
	private static final int OLFACTION_BOOK = 2463;
	private static final int JEWELRY_BOOK = 2502;
	private static final int DIVINE_BOOK = 3117;

	// Multi-use creation items

	private static final int PALM_FROND = 2605;
	private static final int MUMMY_WRAP = 2634;
	private static final int DUCT_TAPE = 2697;
	private static final int CLINGFILM = 2988;
	private static final int SAND_BRICK = 2582;

	// Gifts

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

	// Housing

	private static final int NEWBIESPORT_TENT = 69;
	private static final int BARSKIN_TENT = 73;
	private static final int COTTAGE = 143;
	private static final int HOUSE = 526;
	private static final int SANDCASTLE = 3127;

	// Bang potions

	public static final int MILKY_POTION = 819;
	public static final int SWIRLY_POTION = 820;
	public static final int BUBBLY_POTION = 821;
	public static final int SMOKY_POTION = 822;
	public static final int CLOUDY_POTION = 823;
	public static final int EFFERVESCENT_POTION = 824;
	public static final int FIZZY_POTION = 825;
	public static final int DARK_POTION = 826;
	public static final int MURKY_POTION = 827;


	// Punchcards
	public static final int PUNCHCARD_ATTACK = 3146;
	public static final int PUNCHCARD_REPAIR = 3147;
	public static final int PUNCHCARD_BUFF = 3148;
	public static final int PUNCHCARD_MODIFY = 3149;
	public static final int PUNCHCARD_BUILD = 3150;
	public static final int PUNCHCARD_TARGET = 3151;
	public static final int PUNCHCARD_SELF = 3152;
	public static final int PUNCHCARD_FLOOR = 3153;
	public static final int PUNCHCARD_DRONE = 3154;
	public static final int PUNCHCARD_WALL = 3155;
	public static final int PUNCHCARD_SPHERE = 3156;

	private static final int SPARKLER = 2679;
	private static final int SNAKE = 2680;
	private static final int M282 = 2681;

	private static final int STEEL_STOMACH = 2742;
	private static final int STEEL_LIVER = 2743;
	private static final int STEEL_SPLEEN = 2744;

	private static final int JOLLY_CHARRRM = 411;
	private static final int JOLLY_BRACELET = 413;
	private static final int RUM_CHARRRM = 2957;
	private static final int RUM_BRACELET = 2959;
	private static final int GRUMPY_CHARRRM = 2972;
	private static final int GRUMPY_BRACELET = 2973;
	private static final int TARRRNISH_CHARRRM = 2974;
	private static final int TARRRNISH_BRACELET = 2975;
	private static final int BOOTY_CHARRRM = 2980;
	private static final int BOOTY_BRACELET = 2981;
	private static final int CANNONBALL_CHARRRM = 2982;
	private static final int CANNONBALL_BRACELET = 2983;
	private static final int COPPER_CHARRRM = 2984;
	private static final int COPPER_BRACELET = 2985;
	private static final int TONGUE_CHARRRM = 2986;
	private static final int TONGUE_BRACELET = 2987;

	private static final AdventureResult CUMMERBUND = new AdventureResult( 778, 1 );

	private static final AdventureResult ASPARAGUS_KNIFE = new AdventureResult( 19, -1 );
	private static final AdventureResult SAPLING = new AdventureResult( 75, 1 );
	private static final AdventureResult FERTILIZER = new AdventureResult( 76, 1 );
	private static final AdventureResult LOCKED_LOCKER = new AdventureResult( 84, 1 );
	private static final AdventureResult PLANKS = new AdventureResult( 140, -1 );
	private static final AdventureResult DOUGH = new AdventureResult( 159, 1 );
	private static final AdventureResult FOUNTAIN = new AdventureResult( 211, 1 );
	private static final AdventureResult FLAT_DOUGH = new AdventureResult( 301, 1 );
	private static final AdventureResult SUNKEN_CHEST = new AdventureResult( 405, -1 );
	private static final AdventureResult PIRATE_PELVIS = new AdventureResult( 406, -1 );
	private static final AdventureResult SKELETON_BONE = new AdventureResult( 163, -8 );
	private static final AdventureResult NUTS = new AdventureResult( 509, -1 );
	private static final AdventureResult PLAN = new AdventureResult( 502, -1 );
	private static final AdventureResult WINDCHIMES = new AdventureResult( 212, 1 );
	private static final AdventureResult INKWELL = new AdventureResult( 1958, -1 );
	private static final AdventureResult SCRAP_OF_PAPER = new AdventureResult( 1959, -1 );
	private static final AdventureResult WORM_RIDING_HOOKS = new AdventureResult( 2302, -1 );
	private static final AdventureResult ENCRYPTION_KEY = new AdventureResult( 2441, -1 );
	private static final AdventureResult CHARRRM_BRACELET = new AdventureResult( 2953, -1 );
	private static final AdventureResult SIMPLE_CURSED_KEY = new AdventureResult( 3013, -1 );
	private static final AdventureResult ORNATE_CURSED_KEY = new AdventureResult( 3014, -1 );
	private static final AdventureResult GILDED_CURSED_KEY = new AdventureResult( 3015, -1 );
	private static final AdventureResult OVERCHARGED_SPHERE = new AdventureResult( 3215, -1 );

	// Cyborg weapon parts
	private static final AdventureResult LASER_CANON = new AdventureResult( 3069, -1 );
	private static final AdventureResult UNOBTAINIUM_STRAPS = new AdventureResult( 3073, -1 );
	private static final AdventureResult TARGETING_CHOP = new AdventureResult( 3076, -1 );

	// Cyborg hat parts
	private static final AdventureResult CHIN_STRAP = new AdventureResult( 3070, -1 );
	private static final AdventureResult CARBONITE_VISOR = new AdventureResult( 3072, -1 );
	private static final AdventureResult KEVLATEFLOCITE_HELMET = new AdventureResult( 3078, -1 );

	// Cyborg pants parts
	private static final AdventureResult GLUTEAL_SHIELD = new AdventureResult( 3071, -1 );
	private static final AdventureResult FASTENING_APPARATUS = new AdventureResult( 3074, -1 );
	private static final AdventureResult LEG_ARMOR = new AdventureResult( 3077, -1 );

	private final int consumptionType;
	private AdventureResult itemUsed = null;

	public UseItemRequest( final AdventureResult item )
	{
		this( UseItemRequest.getConsumptionType( item ), item );
	}

	public static final int getConsumptionType( final AdventureResult item )
	{
		int itemId = item.getItemId();
		switch ( itemId )
		{
			// Charrrm bracelets
		case JOLLY_BRACELET:
		case RUM_BRACELET:
		case GRUMPY_BRACELET:
		case TARRRNISH_BRACELET:
		case BOOTY_BRACELET:
		case CANNONBALL_BRACELET:
		case COPPER_BRACELET:
		case TONGUE_BRACELET:
			// Crimbo toys
		case ItemPool.HOBBY_HORSE:
		case ItemPool.BALL_IN_A_CUP:
		case ItemPool.SET_OF_JACKS:
			// Iceberglet items
		case ICE_SICKLE:
		case ICE_BABY:
		case ICE_PICK:
		case ICE_SKATES:
			// Great Ball of Frozen Fire items
		case LIARS_PANTS:
		case JUGGLERS_BALLS:
		case PINK_SHIRT:
		case FLAMING_FAMILIAR:
		case EYEBALL_PENDANT:
			// naughty origami kit items
		case FORTUNE_TELLER:
		case GENTLEMENS_MAGAZINE:
		case PAPER_SHURIKEN:
		case ORIGAMI_PASTIES:
		case RIDING_CROP:
			return KoLConstants.CONSUME_USE;
		}
		return ItemDatabase.getConsumptionType( itemId );
	}

	public UseItemRequest( final int consumptionType, final AdventureResult item )
	{
		this( UseItemRequest.getConsumptionLocation( consumptionType, item ), consumptionType, item );
	}

	private static final String getConsumptionLocation( final int consumptionType, final AdventureResult item )
	{
		switch ( consumptionType )
		{
		case KoLConstants.CONSUME_EAT:
			return "inv_eat.php";
		case KoLConstants.CONSUME_DRINK:
			return "inv_booze.php";
		case KoLConstants.GROW_FAMILIAR:
			return "inv_familiar.php";
		case KoLConstants.HPMP_RESTORE:
		case KoLConstants.MP_RESTORE:
			return "skills.php";
		case KoLConstants.CONSUME_HOBO:
		case KoLConstants.CONSUME_GHOST:
			return "inventory.php";
		case KoLConstants.CONSUME_MULTIPLE:
			return "multiuse.php";
		case KoLConstants.CONSUME_SPHERE:
			return "campground.php";
		case KoLConstants.HP_RESTORE:
			if ( item.getCount() > 1 )
			{
				return "multiuse.php";
			}
			return "inv_use.php";
		default:
			return "inv_use.php";
		}
	}

	private UseItemRequest( final String location, final int consumptionType, final AdventureResult item )
	{
		super( location );

		this.consumptionType = consumptionType;
		this.itemUsed = item;

		this.addFormField( "pwd" );
		if ( UseItemRequest.isHousing( item.getItemId() ) )
		{
			this.addFormField( "confirm", "true" );
		}
		this.addFormField( "whichitem", String.valueOf( item.getItemId() ) );
	}

	public static final int currentItemId()
	{
		return UseItemRequest.lastItemUsed == null ? -1 : UseItemRequest.lastItemUsed.getItemId();
	}

	public int getConsumptionType()
	{
		return this.consumptionType;
	}

	public AdventureResult getItemUsed()
	{
		return this.itemUsed;
	}

	public static final int maximumUses( final int itemId )
	{
		return UseItemRequest.maximumUses( itemId, KoLConstants.NO_CONSUME, true );
	}

	public static final int maximumUses( final int itemId, final int consumptionType )
	{
		return UseItemRequest.maximumUses( itemId, consumptionType, true );
	}

	public static final int maximumUses( final int itemId, final boolean allowOverDrink )
	{
		return UseItemRequest.maximumUses( itemId, KoLConstants.NO_CONSUME, allowOverDrink );
	}

	public static final int maximumUses( final int itemId, final int consumptionType, final boolean allowOverDrink )
	{
		if ( consumptionType == KoLConstants.CONSUME_HOBO || consumptionType == KoLConstants.CONSUME_GHOST )
		{
			return Integer.MAX_VALUE;
		}

		if ( itemId <= 0 )
		{
			return Integer.MAX_VALUE;
		}

		switch ( itemId )
		{
		case CHEF:
		case CLOCKWORK_CHEF:
		case BARTENDER:
		case CLOCKWORK_BARTENDER:
		case MAID:
		case CLOCKWORK_MAID:
		case TOASTER:
			return 1;

		case ANCIENT_CURSED_FOOTLOCKER:
			return UseItemRequest.SIMPLE_CURSED_KEY.getCount( KoLConstants.inventory );

		case ORNATE_CURSED_CHEST:
			return UseItemRequest.ORNATE_CURSED_KEY.getCount( KoLConstants.inventory );

		case GILDED_CURSED_CHEST:
			return UseItemRequest.GILDED_CURSED_KEY.getCount( KoLConstants.inventory );

		case MOJO_FILTER:
			return Math.max( 0, 3 - Preferences.getInteger( "currentMojoFilters" ) );

		case ItemPool.EXPRESS_CARD:
			return Preferences.getBoolean( "expressCardUsed" ) ? 0 : 1;
		}

		Integer key = new Integer( itemId );

		if ( UseItemRequest.LIMITED_USES.containsKey( key ) )
		{
			return KoLConstants.activeEffects.contains( UseItemRequest.LIMITED_USES.get( key ) ) ? 0 : 1;
		}

		String itemName = ItemDatabase.getItemName( itemId );

		int fullness = ItemDatabase.getFullness( itemName );
		if ( fullness > 0 )
		{
			return ( KoLCharacter.getFullnessLimit() - KoLCharacter.getFullness() ) / fullness;
		}

		int spleenHit = ItemDatabase.getSpleenHit( itemName );

		float hpRestored = HPRestoreItemList.getHealthRestored( itemName );
		boolean restoresHP = hpRestored != Integer.MIN_VALUE;

		float mpRestored = MPRestoreItemList.getManaRestored( itemName );
		boolean restoresMP = mpRestored != Integer.MIN_VALUE;


		if ( restoresHP || restoresMP )
		{
			int maximumSuggested = 0;

			if ( hpRestored != 0.0f )
			{
				float belowMax = KoLCharacter.getMaximumHP() - KoLCharacter.getCurrentHP();
				maximumSuggested = Math.max( maximumSuggested, (int) Math.ceil( belowMax / hpRestored ) );
			}

			if ( mpRestored != 0.0f )
			{
				float belowMax = KoLCharacter.getMaximumMP() - KoLCharacter.getCurrentMP();
				maximumSuggested = Math.max( maximumSuggested, (int) Math.ceil( belowMax / mpRestored ) );
			}

			if ( spleenHit > 0 )
			{
				maximumSuggested =
					Math.min(
						maximumSuggested, ( KoLCharacter.getSpleenLimit() - KoLCharacter.getSpleenUse() ) / spleenHit );
			}

			return maximumSuggested;
		}

		if ( spleenHit > 0 )
		{
			return ( KoLCharacter.getSpleenLimit() - KoLCharacter.getSpleenUse() ) / spleenHit;
		}

		int inebrietyHit = ItemDatabase.getInebriety( itemName );
		if ( inebrietyHit > 0 )
		{
			int inebrietyLeft = KoLCharacter.getInebrietyLimit() - KoLCharacter.getInebriety();
			return inebrietyLeft < 0 ? 0 : inebrietyLeft < inebrietyHit ? 1 : allowOverDrink ? inebrietyLeft / inebrietyHit + 1 : inebrietyLeft / inebrietyHit;
		}

		return Integer.MAX_VALUE;
	}

	public void run()
	{
		// Equipment should be handled by a different
		// kind of request.

		int itemId = this.itemUsed.getItemId();

		switch ( this.consumptionType )
		{
		case KoLConstants.EQUIP_HAT:
		case KoLConstants.EQUIP_WEAPON:
		case KoLConstants.EQUIP_OFFHAND:
		case KoLConstants.EQUIP_SHIRT:
		case KoLConstants.EQUIP_PANTS:
		case KoLConstants.EQUIP_ACCESSORY:
		case KoLConstants.EQUIP_FAMILIAR:
			( new EquipmentRequest( this.itemUsed ) ).run();
			return;
		case KoLConstants.CONSUME_SPHERE:
			( new PortalRequest( this.itemUsed ) ).run();
			return;
		}

		UseItemRequest.lastUpdate = "";

		if ( itemId == SorceressLairManager.PUZZLE_PIECE.getItemId() )
		{
			SorceressLairManager.completeHedgeMaze();
			return;
		}

		int maximumUses = UseItemRequest.maximumUses( itemId, this.consumptionType );
		if ( maximumUses < this.itemUsed.getCount() )
		{
			this.itemUsed = this.itemUsed.getInstance( maximumUses );
		}

		if ( this.itemUsed.getCount() < 1 )
		{
			return;
		}

		// If it's an elemental phial, then remove the
		// other elemental effects first.

		int phialIndex = -1;
		for ( int i = 0; i < BasementRequest.ELEMENT_PHIALS.length; ++i )
		{
			if ( itemId == BasementRequest.ELEMENT_PHIALS[ i ].getItemId() )
			{
				phialIndex = i;
			}
		}

		if ( phialIndex != -1 )
		{
			for ( int i = 0; i < BasementRequest.ELEMENT_FORMS.length && KoLmafia.permitsContinue(); ++i )
			{
				if ( i != phialIndex && KoLConstants.activeEffects.contains( BasementRequest.ELEMENT_FORMS[ i ] ) )
				{
					( new UneffectRequest( BasementRequest.ELEMENT_FORMS[ i ] ) ).run();
				}
			}

			if ( !KoLmafia.permitsContinue() )
			{
				return;
			}
		}

		int price = ItemDatabase.getPriceById( itemId );

		if ( itemId == UseItemRequest.SELTZER || itemId == UseItemRequest.MAFIA_ARIA )
		{
			SpecialOutfit.createImplicitCheckpoint();
		}

		if ( price != 0 && this.consumptionType != KoLConstants.INFINITE_USES && !InventoryManager.retrieveItem( this.itemUsed ) )
		{
			if ( itemId == UseItemRequest.SELTZER || itemId == UseItemRequest.MAFIA_ARIA )
			{
				SpecialOutfit.restoreImplicitCheckpoint();
			}

			return;
		}

		if ( itemId == UseItemRequest.SELTZER )
		{
			SpecialOutfit.restoreImplicitCheckpoint();
		}

		int iterations = 1;

		if ( this.itemUsed.getCount() != 1 )
		{
			switch ( this.consumptionType )
			{
			case KoLConstants.CONSUME_MULTIPLE:
			case KoLConstants.HP_RESTORE:
			case KoLConstants.MP_RESTORE:
			case KoLConstants.HPMP_RESTORE:
				break;
			default:
				iterations = this.itemUsed.getCount();
				this.itemUsed = this.itemUsed.getInstance( 1 );
			}
		}

		if ( itemId == UseItemRequest.MACGUFFIN_DIARY )
		{
			( new GenericRequest( "diary.php?textversion=1" ) ).run();
			KoLmafia.updateDisplay( "Your father's diary has been read." );
			return;
		}

		if ( itemId == UseItemRequest.MAFIA_ARIA )
		{
			if ( !KoLCharacter.hasEquipped( UseItemRequest.CUMMERBUND ) )
			{
				RequestThread.postRequest( new EquipmentRequest( UseItemRequest.CUMMERBUND ) );
			}
		}

		String useTypeAsString =
			this.consumptionType == KoLConstants.CONSUME_EAT ? "Eating" : this.consumptionType == KoLConstants.CONSUME_DRINK ? "Drinking" : "Using";

		String originalURLString = this.getURLString();

		for ( int i = 1; i <= iterations && KoLmafia.permitsContinue(); ++i )
		{
			this.constructURLString( originalURLString );

			if ( this.consumptionType == KoLConstants.CONSUME_DRINK && !UseItemRequest.allowBoozeConsumption( ItemDatabase.getInebriety( this.itemUsed.getName() ) ) )
			{
				return;
			}

			this.useOnce( i, iterations, useTypeAsString );
		}

		if ( itemId == UseItemRequest.MAFIA_ARIA )
		{
			SpecialOutfit.restoreImplicitCheckpoint();
		}

		if ( KoLmafia.permitsContinue() )
		{
			KoLmafia.updateDisplay( "Finished " + useTypeAsString.toLowerCase() + " " + Math.max(
				iterations, this.itemUsed.getCount() ) + " " + this.itemUsed.getName() + "." );
		}
	}

	public static final boolean allowBoozeConsumption( final int inebrietyBonus )
	{
		if ( KoLCharacter.isFallingDown() || inebrietyBonus < 1 )
		{
			return true;
		}

		if ( KoLConstants.existingFrames.isEmpty() )
		{
			return true;
		}

		// Make sure the player does not drink something without
		// having ode, if they can cast ode.

		if ( !KoLConstants.activeEffects.contains( ItemDatabase.ODE ) )
		{
			UseSkillRequest ode = UseSkillRequest.getInstance( "The Ode to Booze" );
			boolean knowsOde = KoLConstants.availableSkills.contains( ode );

			if ( knowsOde && UseSkillRequest.hasAccordion() && KoLCharacter.getCurrentMP() >= SkillDatabase.getMPConsumptionById( 6014 ) )
			{
				ode.setBuffCount( 1 );
				RequestThread.postRequest( ode );
			}
			else if ( knowsOde && UseItemRequest.askedAboutOde != KoLCharacter.getUserId() )
			{
				if ( !InputFieldUtilities.confirm( "Are you sure you want to drink without ode?" ) )
				{
					return false;
				}

				UseItemRequest.askedAboutOde = KoLCharacter.getUserId();
			}
		}

		// Make sure the player does not overdrink if they still
		// have PvP attacks remaining.

		if ( KoLCharacter.getInebriety() + inebrietyBonus > KoLCharacter.getInebrietyLimit() )
		{
			if ( KoLCharacter.getAttacksLeft() > 0 && !InputFieldUtilities.confirm( "Are you sure you want to overdrink without PvPing?" ) )
			{
				return false;
			}
			else if ( KoLCharacter.getAdventuresLeft() > 40 && !InputFieldUtilities.confirm( "Are you sure you want to overdrink?" ) )
			{
				return false;
			}
		}

		return true;
	}

	public void useOnce( final int currentIteration, final int totalIterations, String useTypeAsString )
	{
		UseItemRequest.lastUpdate = "";

		if ( this.consumptionType == KoLConstants.CONSUME_ZAP )
		{
			StaticEntity.getClient().makeZapRequest();
			return;
		}

		// Check to make sure the character has the item in their
		// inventory first - if not, report the error message and
		// return from the method.

		if ( !InventoryManager.retrieveItem( this.itemUsed ) )
		{
			UseItemRequest.lastUpdate = "Insufficient items to use.";
			return;
		}

		switch ( this.consumptionType )
		{
		case KoLConstants.HP_RESTORE:
			if ( this.itemUsed.getCount() > 1 )
			{
				this.addFormField( "action", "useitem" );
				this.addFormField( "quantity", String.valueOf( this.itemUsed.getCount() ) );
			}
			else
			{
				this.addFormField( "which", "3" );
			}
                        break;

		case KoLConstants.CONSUME_MULTIPLE:
			this.addFormField( "action", "useitem" );
			this.addFormField( "quantity", String.valueOf( this.itemUsed.getCount() ) );
			break;

		case KoLConstants.HPMP_RESTORE:
		case KoLConstants.MP_RESTORE:
			this.addFormField( "action", "useitem" );
			this.addFormField( "itemquantity", String.valueOf( this.itemUsed.getCount() ) );
			break;

		case KoLConstants.CONSUME_HOBO:
			this.addFormField( "action", "hobo" );
			this.addFormField( "which", "1" );
			useTypeAsString = "Boozing hobo with";
			break;

		case KoLConstants.CONSUME_GHOST:
			this.addFormField( "action", "ghost" );
			this.addFormField( "which", "1" );
			useTypeAsString = "Feeding ghost with";
			break;

		case KoLConstants.CONSUME_EAT:
		case KoLConstants.CONSUME_DRINK:
			this.addFormField( "which", "1" );
			break;

		default:
			this.addFormField( "which", "3" );
			break;
		}

		if ( totalIterations == 1 )
		{
			KoLmafia.updateDisplay( useTypeAsString + " " + this.itemUsed.getCount() + " " + this.itemUsed.getName() + "..." );
		}
		else
		{
			KoLmafia.updateDisplay( useTypeAsString + " " + this.itemUsed.getName() + " (" + currentIteration + " of " + totalIterations + ")..." );
		}

		super.run();

		if ( this.responseCode == 302 && this.redirectLocation.startsWith( "inventory" ) )
		{
			UseItemRequest.REDIRECT_REQUEST.constructURLString( this.redirectLocation ).run();
			UseItemRequest.lastItemUsed = this.itemUsed;
			UseItemRequest.parseConsumption( UseItemRequest.REDIRECT_REQUEST.responseText, true );
		}
	}

	public void processResults()
	{
		UseItemRequest.lastItemUsed = this.itemUsed;
		UseItemRequest.parseConsumption( this.responseText, true );
	}

	public static final void parseConsumption( final String responseText, final boolean showHTML )
	{
		if ( UseItemRequest.lastItemUsed == null )
		{
			return;
		}

		AdventureResult item = UseItemRequest.lastItemUsed;
		UseItemRequest.resetItemUsed();

		int consumptionType = ItemDatabase.getConsumptionType( item.getItemId() );

		if ( consumptionType == KoLConstants.NO_CONSUME )
		{
			return;
		}

		if ( consumptionType == KoLConstants.INFINITE_USES )
		{
			return;
		}

		if ( consumptionType == KoLConstants.MESSAGE_DISPLAY )
		{
			if ( !LoginRequest.isInstanceRunning() )
			{
				UseItemRequest.showItemUsage( showHTML, responseText, true );
			}

			return;
		}

		// Assume initially that this causes the item to disappear.
		// In the event that the item is not used, then proceed to
		// undo the consumption.

		ResultProcessor.processResult( item.getNegation() );

		// Check for familiar growth - if a familiar is added,
		// make sure to update the StaticEntity.getClient().

		if ( consumptionType == KoLConstants.GROW_FAMILIAR )
		{
			if ( responseText.indexOf( "You've already got a familiar of that type." ) != -1 )
			{
				UseItemRequest.lastUpdate = "You already have that familiar.";
				KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, UseItemRequest.lastUpdate );
				ResultProcessor.processResult( item );
				return;
			}

			// Pop up a window showing the result

			KoLCharacter.addFamiliar( FamiliarDatabase.growFamiliarLarva( item.getItemId() ) );
			UseItemRequest.showItemUsage( showHTML, responseText, true );
			return;
		}

		if ( responseText.indexOf( "You may not" ) != -1 )
		{
			KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "Pathed ascension." );
			ResultProcessor.processResult( item );
			return;
		}

		if ( responseText.indexOf( "rupture" ) != -1 )
		{
			UseItemRequest.lastUpdate = "Your spleen might go kabooie.";
			KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, UseItemRequest.lastUpdate );
			ResultProcessor.processResult( item );
			return;
		}

		// Check to make sure that it wasn't a food or drink
		// that was consumed that resulted in nothing.  Eating
		// too much is flagged as a continuable state.

		if ( responseText.indexOf( "too full" ) != -1 )
		{
			UseItemRequest.lastUpdate = "Consumption limit reached.";
			KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, UseItemRequest.lastUpdate );
			ResultProcessor.processResult( item );
			return;
		}

		if ( responseText.indexOf( "too drunk" ) != -1 )
		{
			UseItemRequest.lastUpdate = "Inebriety limit reached.";
			KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, UseItemRequest.lastUpdate );
			ResultProcessor.processResult( item );
			return;
		}

		// Perform item-specific processing

		switch ( item.getItemId() )
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
				UseItemRequest.lastUpdate = "You can't open that package yet.";
				KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, UseItemRequest.lastUpdate );
				ResultProcessor.processResult( item );
			}
			else if ( showHTML )
			{
				UseItemRequest.showItemUsage( true, responseText, true );
			}

			return;

			// If it's a fortune cookie, get the fortune

		case FORTUNE_COOKIE:

			UseItemRequest.showItemUsage( showHTML, responseText, true );

			Matcher fortuneMatcher = UseItemRequest.FORTUNE_PATTERN.matcher( responseText );
			if ( !fortuneMatcher.find() )
			{
				return;
			}

			String message = fortuneMatcher.group( 1 );
			RequestLogger.updateSessionLog( message );
			RequestLogger.printLine( message );

			if ( StaticEntity.isCounting( "Fortune Cookie" ) )
			{
				int desiredCount = 0;
				for ( int i = 2; i <= 4; ++i )
				{
					int number = StringUtilities.parseInt( fortuneMatcher.group( i ) );
					if ( StaticEntity.isCounting( "Fortune Cookie", number ) )
					{
						desiredCount = number;
					}
				}

				if ( desiredCount != 0 )
				{
					StaticEntity.stopCounting( "Fortune Cookie" );
					StaticEntity.startCounting( desiredCount, "Fortune Cookie", "fortune.gif" );
					return;
				}
			}

			for ( int i = 2; i <= 4; ++i )
			{
				int number = StringUtilities.parseInt( fortuneMatcher.group( i ) );
				StaticEntity.startCounting( number, "Fortune Cookie", "fortune.gif" );
			}

			return;

		case GATES_SCROLL:

			// You can only use a 64735 scroll if you have the
			// original dictionary in your inventory

			// "Even though your name isn't Lee, you're flattered
			// and hand over your dictionary."

			if ( responseText.indexOf( "you're flattered" ) == -1 )
			{
				ResultProcessor.processResult( item );
			}
			else
			{
				ResultProcessor.processResult( FightRequest.DICTIONARY1.getNegation() );
			}

			return;

		case HACK_SCROLL:

			// "The UB3r 31337 HaX0R stands before you."

			if ( responseText.indexOf( "The UB3r 31337 HaX0R stands before you." ) != -1 )
			{
				ResultProcessor.processResult(
					item.getInstance( item.getCount() - 1 ) );
			}

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
			{
				ResultProcessor.processResult( item );
			}

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
			{
				ResultProcessor.processResult( item );
			}

			return;

		case LIBRARY_CARD:

			// If you've already used a library card today, it is
			// not consumed.

			// "You head back to the library, but can't find
			// anything you feel like reading. You skim a few
			// celebrity-gossip magazines, and end up feeling kind
			// of dirty."

			if ( responseText.indexOf( "feeling kind of dirty" ) == -1 )
			{
				ResultProcessor.processResult( item );
			}

			return;

		case HEY_DEZE_MAP:

			// "Your song has pleased me greatly. I will reward you
			// with some of my crazy imps, to do your bidding."

			if ( responseText.indexOf( "pleased me greatly" ) == -1 )
			{
				UseItemRequest.lastUpdate = "You music was inadequate.";
				KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, UseItemRequest.lastUpdate );
				ResultProcessor.processResult( item );
			}

			return;

		case GIANT_CASTLE_MAP:

			// "I'm sorry, adventurer, but the Sorceress is in
			// another castle!"

			if ( responseText.indexOf( "Sorceress is in another castle" ) == -1 )
			{
				UseItemRequest.lastUpdate = "You couldn't make it all the way to the back door.";
				KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, UseItemRequest.lastUpdate );
				ResultProcessor.processResult( item );
			}

			return;

		case DRASTIC_HEALING:

			// If a scroll of drastic healing was used and didn't
			// crumble, it is not consumed

			ResultProcessor.processResult(
				new AdventureResult( AdventureResult.HP, KoLCharacter.getMaximumHP() ) );

			if ( responseText.indexOf( "crumble" ) == -1 )
			{
				ResultProcessor.processResult( item );
			}

			return;

		case TEARS:

			KoLConstants.activeEffects.remove( KoLAdventure.BEATEN_UP );
			return;

		case ANTIDOTE:
		case TINY_HOUSE:

			KoLConstants.activeEffects.clear();
			return;

		case TBONE_KEY:

			if ( InventoryManager.hasItem( UseItemRequest.LOCKED_LOCKER ) )
			{
				ResultProcessor.processResult( UseItemRequest.LOCKED_LOCKER.getNegation() );
			}
			else
			{
				ResultProcessor.processResult( item );
			}

		case RAFFLE_TICKET:

			// The first time you use an Elf Farm Raffle ticket
			// with a ten-leaf clover in your inventory, the clover
			// disappears in a puff of smoke and you get pagoda
			// plans.

			// Subsequent raffle tickets don't consume clovers.

			if ( responseText.indexOf( "puff of smoke" ) != -1 )
			{
				ResultProcessor.processItem( ItemPool.TEN_LEAF_CLOVER, -1 );
			}

			return;

		case KETCHUP_HOUND:

			// Successfully using a ketchup hound uses up the Hey
			// Deze nuts and pagoda plan.

			if ( responseText.indexOf( "pagoda" ) != -1 )
			{
				ResultProcessor.processResult( UseItemRequest.NUTS );
				ResultProcessor.processResult( UseItemRequest.PLAN );
			}

			// The ketchup hound does not go away...

			ResultProcessor.processResult( item );
			return;

		case LUCIFER:

			// Jumbo Dr. Lucifer reduces your hit points to 1.

			ResultProcessor.processResult(
				new AdventureResult( AdventureResult.HP, 1 - KoLCharacter.getCurrentHP() ) );

			return;

		case DOLPHIN_KING_MAP:

			// "You follow the Dolphin King's map to the bottom of
			// the sea, and find his glorious treasure."

			if ( responseText.indexOf( "find his glorious treasure" ) == -1 )
			{
				UseItemRequest.lastUpdate = "You don't have everything you need.";
				KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, UseItemRequest.lastUpdate );
				ResultProcessor.processResult( item );
			}

			return;

		case SLUG_LORD_MAP:

			// "You make your way to the deepest part of the tank,
			// and find a chest engraved with the initials S. L."

			if ( responseText.indexOf( "deepest part of the tank" ) == -1 )
			{
				UseItemRequest.lastUpdate = "You don't have everything you need.";
				KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, UseItemRequest.lastUpdate );
				ResultProcessor.processResult( item );
			}

			return;

		case DR_HOBO_MAP:

			// "You place it atop the Altar, and grab the Scalpel
			// at the exact same moment."

			if ( responseText.indexOf( "exact same moment" ) == -1 )
			{
				UseItemRequest.lastUpdate = "You don't have everything you need.";
				KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, UseItemRequest.lastUpdate );
				ResultProcessor.processResult( item );
				return;
			}

			// Using the map consumes an asparagus knife

			ResultProcessor.processResult( UseItemRequest.ASPARAGUS_KNIFE );
			return;

		case COBBS_KNOB_MAP:

			// "You memorize the location of the door, then eat
			// both the map and the encryption key."

			if ( responseText.indexOf( "memorize the location" ) == -1 )
			{
				UseItemRequest.lastUpdate = "You don't have everything you need.";
				KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, UseItemRequest.lastUpdate );
				ResultProcessor.processResult( item );
				return;
			}

			// Using the map consumes the encryption key

			ResultProcessor.processResult( UseItemRequest.ENCRYPTION_KEY );
			return;

		case BLACK_MARKET_MAP:

			// "You try to follow the map, but you can't make head
			// or tail of it. It keeps telling you to take paths
			// through completely impenetrable foliage.
			// What was it that the man in black told you about the
			// map? Something about "as the crow flies?""

			if ( responseText.indexOf( "can't make head or tail of it" ) != -1 )
			{
				UseItemRequest.lastUpdate = "You need a guide.";
				KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, UseItemRequest.lastUpdate );
				ResultProcessor.processResult( item );
				return;
			}

			// This only works if you had a Reassembled Blackbird
			// familiar. When it works, the familiar disappears.

			FamiliarData blackbird = KoLCharacter.getFamiliar();
			KoLCharacter.removeFamiliar( blackbird );

			AdventureResult blackbirdItem = blackbird.getItem();
			if ( blackbirdItem != null && !blackbirdItem.equals( EquipmentRequest.UNEQUIP ) )
			{
				AdventureResult.addResultToList( KoLConstants.inventory, blackbirdItem );
			}

			if ( !Preferences.getString( "preBlackbirdFamiliar" ).equals( "" ) )
			{
				KoLmafiaCLI.DEFAULT_SHELL.executeCommand(
					"familiar", Preferences.getString( "preBlackbirdFamiliar" ) );
				if ( blackbirdItem != null && !blackbirdItem.equals( EquipmentRequest.UNEQUIP ) &&
					KoLCharacter.getFamiliar().canEquip( blackbirdItem ) )
				{
					new EquipmentRequest( blackbirdItem ).run();
				}

				Preferences.setString( "preBlackbirdFamiliar", "" );
			}

			QuestLogRequest.setBlackMarketAvailable();
			return;

		case SPOOKY_TEMPLE_MAP:

			if ( InventoryManager.hasItem( UseItemRequest.SAPLING ) && InventoryManager.hasItem( UseItemRequest.FERTILIZER ) )
			{
				ResultProcessor.processResult( UseItemRequest.SAPLING.getNegation() );
				ResultProcessor.processResult( UseItemRequest.FERTILIZER.getNegation() );
			}
			else
			{
				UseItemRequest.lastUpdate = "You don't have everything you need.";
				KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, UseItemRequest.lastUpdate );
				ResultProcessor.processResult( item );
			}

			return;

		case DINGHY_PLANS:

			// "You need some planks to build the dinghy."

			if ( InventoryManager.hasItem( UseItemRequest.PLANKS ) )
			{
				ResultProcessor.processResult( UseItemRequest.PLANKS );
			}
			else
			{
				UseItemRequest.lastUpdate = "You need some dingy planks.";
				KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, UseItemRequest.lastUpdate );
				ResultProcessor.processResult( item );
			}

			return;

		case PIRATE_SKULL:

			// "Unable to find enough parts, the semi-formed
			// skeleton gives up and falls to pieces."
			if ( responseText.indexOf( "gives up and falls to pieces." ) != -1 )
			{
				ResultProcessor.processResult( item );
			}
			else
			{
				ResultProcessor.processResult( UseItemRequest.SUNKEN_CHEST );
				ResultProcessor.processResult( UseItemRequest.PIRATE_PELVIS );
				ResultProcessor.processResult( UseItemRequest.SKELETON_BONE );
			}
			UseItemRequest.showItemUsage( showHTML, responseText, true );
			break;

		case FENG_SHUI:

			if ( InventoryManager.hasItem( UseItemRequest.FOUNTAIN ) && InventoryManager.hasItem( UseItemRequest.WINDCHIMES ) )
			{
				ResultProcessor.processResult( UseItemRequest.FOUNTAIN.getNegation() );
				ResultProcessor.processResult( UseItemRequest.WINDCHIMES.getNegation() );
			}
			else
			{
				ResultProcessor.processResult( item );
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
			{
				ResultProcessor.processResult(
					item.getInstance( item.getCount() - 1 ) );
			}

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
				UseItemRequest.lastUpdate = "Your mouth is too cold.";
				KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, UseItemRequest.lastUpdate );
				ResultProcessor.processResult( item );
			}

			return;

		case ROLLING_PIN:

			// Rolling pins remove dough from your inventory
			// They are not consumed by being used

			ResultProcessor.processResult(
				UseItemRequest.DOUGH.getInstance( UseItemRequest.DOUGH.getCount( KoLConstants.inventory ) ).getNegation() );
			ResultProcessor.processResult( item );

			return;

		case UNROLLING_PIN:

			// Unrolling pins remove flat dough from your inventory
			// They are not consumed by being used

			ResultProcessor.processResult(
				UseItemRequest.FLAT_DOUGH.getInstance(
					UseItemRequest.FLAT_DOUGH.getCount( KoLConstants.inventory ) ).getNegation() );
			ResultProcessor.processResult( item );
			return;

		case PLUS_SIGN:

			// "Following The Oracle's advice, you treat the plus
			// sign as a book, and read it."

			if ( responseText.indexOf( "you treat the plus sign as a book" ) == -1 )
			{
				UseItemRequest.lastUpdate = "You don't know how to use it.";
				KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, UseItemRequest.lastUpdate );
				ResultProcessor.processResult( item );
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

		case SNOWCONE_BOOK:
		case HILARIOUS_BOOK:
		case CANDY_BOOK:
		case DIVINE_BOOK:

			// "You've already got a Libram of Divine Favors on
			// your bookshelf."
			//
			// old message:
			//
			// "You already know how to summon snowcones."

			if ( responseText.indexOf( "You've already got" ) != -1 ||
			     responseText.indexOf( "You already" ) != -1)
			{
				ResultProcessor.processResult( item );
				return;
			}

			KoLCharacter.setBookshelf( true );

			switch ( item.getItemId() )
			{
			case SNOWCONE_BOOK:
				KoLCharacter.addAvailableSkill( "Summon Snowcone" );
				break;
			case HILARIOUS_BOOK:
				KoLCharacter.addAvailableSkill( "Summon Hilarious Objects" );
				break;
			case CANDY_BOOK:
				KoLCharacter.addAvailableSkill( "Summon Candy Hearts" );
				break;
			case DIVINE_BOOK:
				KoLCharacter.addAvailableSkill( "Summon Party Favor" );
				break;
			}

			return;

		case JEWELRY_BOOK:

			// "You already know how to make fancy jewelry, so this
			// book contains nothing exciting for you."

			if ( responseText.indexOf( "You already" ) != -1 )
			{
				ResultProcessor.processResult( item );
				return;
			}

			KoLCharacter.addAvailableSkill( "Really Expensive Jewelrycrafting" );

			return;

		case OLFACTION_BOOK:

			if ( responseText.indexOf( "You already" ) != -1 )
			{
				ResultProcessor.processResult( item );
				return;
			}

			KoLCharacter.addAvailableSkill( "Transcendent Olfaction" );

			return;

		case TELESCOPE:
			// We've added or upgraded our telescope
			KoLCharacter.setTelescope( true );

			// Look through it to check number of upgrades
			Preferences.setInteger( "lastTelescopeReset", -1 );
			KoLCharacter.checkTelescope();
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
			{
				ResultProcessor.processResult( item );
			}

			return;

		case ABSINTHE:

			// "You drink the bottle of absinthe. It tastes like
			// licorice, pain, and green. Your head begins to ache
			// and you see a green glow in the general direction of
			// the distant woods."

			// "No way are you gonna drink another one of those
			// until the last one wears off."

			if ( responseText.indexOf( "licorice" ) == -1 )
			{
				ResultProcessor.processResult( item );
			}

			return;

		case DUSTY_ANIMAL_SKULL:

			// The magic that had previously animated the animals kicks back
			// in, and it stands up shakily and looks at you. "Graaangh?"

			if ( responseText.indexOf( "Graaangh?" ) == -1 )
			{
				UseItemRequest.lastUpdate = "You're missing some parts.";
				KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, UseItemRequest.lastUpdate );
				ResultProcessor.processResult( item );
				return;
			}

			// Remove the other 98 bones

			for ( int i = 1802; i < 1900; ++i )
			{
				ResultProcessor.processResult( new AdventureResult( i, -1 ) );
			}

			return;

		case QUILL_PEN:

			if ( responseText.indexOf( "You acquire" ) == -1 )
			{
				UseItemRequest.lastUpdate = "You're missing some parts.";
				KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, UseItemRequest.lastUpdate );
				ResultProcessor.processResult( item );
				return;
			}

			// It worked. Also remove the ink and paper.
			ResultProcessor.processResult(
				UseItemRequest.INKWELL.getInstance( 0 - item.getCount() ) );
			ResultProcessor.processResult(
				UseItemRequest.SCRAP_OF_PAPER.getInstance( 0 - item.getCount() ) );
			return;

		case PALM_FROND:
		case MUMMY_WRAP:
		case DUCT_TAPE:
		case CLINGFILM:

			if ( responseText.indexOf( "You acquire" ) == -1 )
			{
				ResultProcessor.processResult( item );
			}

			return;

		case SAND_BRICK:

			if ( responseText.indexOf( "You can't build anything" ) != -1 )
			{
				ResultProcessor.processResult( item );
			}

			return;

		case DRUM_MACHINE:

			// "Dammit! Your hooks were still on there! Oh well. At
			// least now you know where the pyramid is."

			if ( responseText.indexOf( "hooks were still on" ) != -1 )
			{
				// You lose your weapon
				EquipmentManager.setEquipment( EquipmentManager.WEAPON, EquipmentRequest.UNEQUIP );
				AdventureResult.addResultToList(
					KoLConstants.inventory, UseItemRequest.WORM_RIDING_HOOKS.getInstance( 1 ) );
				ResultProcessor.processResult( UseItemRequest.WORM_RIDING_HOOKS );
			}
			else
			{
				UseItemRequest.lastUpdate = "Insufficient adventures left.";
				KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, UseItemRequest.lastUpdate );
				ResultProcessor.processResult( item );
			}

			return;

		case STEEL_STOMACH:

			if ( responseText.indexOf( "You acquire a skill" ) != -1 )
			{
				KoLCharacter.addAvailableSkill( "Stomach of Steel" );
			}
			return;

		case STEEL_LIVER:

			if ( responseText.indexOf( "You acquire a skill" ) != -1 )
			{
				KoLCharacter.addAvailableSkill( "Liver of Steel" );
			}
			return;

		case STEEL_SPLEEN:

			if ( responseText.indexOf( "You acquire a skill" ) != -1 )
			{
				KoLCharacter.addAvailableSkill( "Spleen of Steel" );
			}
			return;

		case MOJO_FILTER:

			// You strain some of the toxins out of your mojo, and
			// discard the now-grodulated filter.

			if ( responseText.indexOf( "now-grodulated" ) == -1 )
			{
				ResultProcessor.processResult( item );
				return;
			}

			Preferences.setInteger(
				"currentMojoFilters",
				Preferences.getInteger( "currentMojoFilters" ) + item.getCount() );

			Preferences.setInteger(
				"currentSpleenUse",
				Math.max( 0, Preferences.getInteger( "currentSpleenUse" ) - item.getCount() ) );

			return;

		case NEWBIESPORT_TENT:
		case BARSKIN_TENT:
		case COTTAGE:
		case HOUSE:
		case SANDCASTLE:

			if ( responseText.indexOf( "You place the" ) == -1 && responseText.indexOf( "You build a" ) == -1 && responseText.indexOf( "You quickly burn down" ) == -1)
			{
				ResultProcessor.processResult( item );
			}
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
			{
				effectData = "inebriety";
			}
			else if ( responseText.indexOf( "You gain" ) != -1 )
			{
				effectData = "healing";
			}
			else if ( responseText.indexOf( "Confused" ) != -1 )
			{
				effectData = "confusion";
			}
			else if ( responseText.indexOf( "Izchak's Blessing" ) != -1 )
			{
				effectData = "blessing";
			}
			else if ( responseText.indexOf( "Object Detection" ) != -1 )
			{
				effectData = "detection";
			}
			else if ( responseText.indexOf( "Sleepy" ) != -1 )
			{
				effectData = "sleepiness";
			}
			else if ( responseText.indexOf( "Strange Mental Acuity" ) != -1 )
			{
				effectData = "mental acuity";
			}
			else if ( responseText.indexOf( "Strength of Ten Ettins" ) != -1 )
			{
				effectData = "ettin strength";
			}
			else if ( responseText.indexOf( "Teleportitis" ) != -1 )
			{
				effectData = "teleportitis";
			}

			if ( effectData != null )
			{
				Preferences.setString( "lastBangPotion" + item.getItemId(), effectData );
			}

			return;

		case JOLLY_CHARRRM:
		case RUM_CHARRRM:
		case GRUMPY_CHARRRM:
		case TARRRNISH_CHARRRM:
		case BOOTY_CHARRRM:
		case CANNONBALL_CHARRRM:
		case COPPER_CHARRRM:
		case TONGUE_CHARRRM:
			if ( responseText.indexOf( "You don't have anything to attach that charrrm to." ) != -1 )
			{
				UseItemRequest.lastUpdate = "You need a charrrm bracelet.";
				KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, UseItemRequest.lastUpdate );
				ResultProcessor.processResult( item );
				return;
			}
			ResultProcessor.processResult( UseItemRequest.CHARRRM_BRACELET );
			return;

		case ANCIENT_CURSED_FOOTLOCKER:
			if ( InventoryManager.hasItem( UseItemRequest.SIMPLE_CURSED_KEY ) )
			{
				ResultProcessor.processResult( UseItemRequest.SIMPLE_CURSED_KEY );
			}
			else
			{
				ResultProcessor.processResult( item );
			}
			return;

		case ORNATE_CURSED_CHEST:
			if ( InventoryManager.hasItem( UseItemRequest.ORNATE_CURSED_KEY ) )
			{
				ResultProcessor.processResult( UseItemRequest.ORNATE_CURSED_KEY );
			}
			else
			{
				ResultProcessor.processResult( item );
			}
			return;

		case GILDED_CURSED_CHEST:
			if ( InventoryManager.hasItem( UseItemRequest.GILDED_CURSED_KEY ) )
			{
				ResultProcessor.processResult( UseItemRequest.GILDED_CURSED_KEY );
			}
			else
			{
				ResultProcessor.processResult( item );
			}
			return;

		case CURSED_PIECE_OF_THIRTEEN:
			// You consider taking the piece of thirteen to a rare
			// coin dealer to see if it's worth anything, but you
			// don't really have time.
			if ( responseText.indexOf( "don't really have time" ) != -1 )
			{
				UseItemRequest.lastUpdate = "Insufficient adventures left.";
				KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, UseItemRequest.lastUpdate );
				ResultProcessor.processResult( item );
			}
			return;

		case GENERAL_ASSEMBLY_MODULE:

			//  "INSUFFICIENT RESOURCES LOCATED TO DISPENSE CRIMBO
			//  CHEER. PLEASE LOCATE A VITAL APPARATUS VENT AND
			//  REQUISITION APPROPRIATE MATERIALS."

			if ( responseText.indexOf( "INSUFFICIENT RESOURCES LOCATED" ) != -1 )
			{
				ResultProcessor.processResult( item );
				return;
			}

			// You breathe a heavy sigh of relief as the pseudopods
			// emerge from your inventory, carrying the laser
			// cannon, laser targeting chip, and the set of
			// Unobtainium straps that you acquired earlier from
			// the Sinister Dodecahedron.

			if ( responseText.indexOf( "carrying the  laser cannon" ) != -1 )
			{
				ResultProcessor.processResult( UseItemRequest.LASER_CANON );
				ResultProcessor.processResult( UseItemRequest.TARGETING_CHOP );
				ResultProcessor.processResult( UseItemRequest.UNOBTAINIUM_STRAPS );

				return;
			}

			// You breathe a heavy sigh of relief as the pseudopods
			// emerge from your inventory, carrying the polymorphic
			// fastening apparatus, hi-density nylocite leg armor,
			// and the silicon-infused gluteal shield that you
			// acquired earlier from the Sinister Dodecahedron.

			if ( responseText.indexOf( "carrying the  polymorphic fastening apparatus" ) != -1 )
			{
				ResultProcessor.processResult( UseItemRequest.FASTENING_APPARATUS );
				ResultProcessor.processResult( UseItemRequest.LEG_ARMOR );
				ResultProcessor.processResult( UseItemRequest.GLUTEAL_SHIELD );

				return;
			}

			// You breathe a heavy sigh of relief as the pseudopods
			// emerge from your inventory, carrying the carbonite
			// visor, plexifoam chin strap, and the kevlateflocite
			// helmet that you acquired earlier from the Sinister
			// Dodecahedron.

			if ( responseText.indexOf( "carrying the  carbonite visor" ) != -1 )
			{
				ResultProcessor.processResult( UseItemRequest.CARBONITE_VISOR );
				ResultProcessor.processResult( UseItemRequest.CHIN_STRAP );
				ResultProcessor.processResult( UseItemRequest.KEVLATEFLOCITE_HELMET );

				return;
			}

			return;

		case KoLmafia.AUGMENTED_DRONE:

			if ( responseText.indexOf( "You put an overcharged sphere in the cavity" ) != -1 )
			{
				ResultProcessor.processResult( UseItemRequest.OVERCHARGED_SPHERE );
			}

			return;

		case ItemPool.HOBBY_HORSE:
		case ItemPool.BALL_IN_A_CUP:
		case ItemPool.SET_OF_JACKS:
		case ItemPool.EL_VIBRATO_HELMET:

			// Certain pieces of equipment can also be "used" and
			// are not consumed.

			ResultProcessor.processResult( item );
			return;
		}
	}

	public static final boolean isHousing( final int itemId )
	{
		return	itemId == UseItemRequest.NEWBIESPORT_TENT ||
			itemId == UseItemRequest.BARSKIN_TENT ||
			itemId == UseItemRequest.COTTAGE ||
			itemId == UseItemRequest.HOUSE ||
			itemId == UseItemRequest.SANDCASTLE;
	}

	private static final void showItemUsage( final boolean showHTML, final String text, boolean consumed )
	{
		if ( showHTML )
		{
			StaticEntity.getClient().showHTML(
				"inventory.php?action=message", UseItemRequest.trimInventoryText( text ) );
		}

		if ( !consumed )
		{
			ResultProcessor.processResult( UseItemRequest.lastItemUsed );
		}
	}

	private static final String trimInventoryText( String text )
	{
		// Get rid of first row of first table: the "Results" line
		Matcher matcher = UseItemRequest.ROW_PATTERN.matcher( text );
		if ( matcher.find() )
		{
			text = matcher.replaceFirst( "" );
		}

		// Get rid of inventory listing
		matcher = UseItemRequest.INVENTORY_PATTERN.matcher( text );
		if ( matcher.find() )
		{
			text = matcher.replaceFirst( "</table></body>" );
		}

		return text;
	}

	private static final AdventureResult extractItem( final String urlString )
	{
		if ( urlString.startsWith( "inv_eat.php" ) )
		{
			;
		}
		else if ( urlString.startsWith( "inv_booze.php" ) )
		{
			;
		}
		else if ( urlString.startsWith( "multiuse.php" ) )
		{
			;
		}
		else if ( urlString.startsWith( "skills.php" ) )
		{
			;
		}
		else if ( urlString.startsWith( "inv_familiar.php" ) )
		{
			;
		}
		else if ( urlString.startsWith( "inv_use.php" ) )
		{
			;
		}
		else
		{
			return null;
		}

		Matcher itemMatcher = UseItemRequest.ITEMID_PATTERN.matcher( urlString );
		if ( !itemMatcher.find() )
		{
			return null;
		}

		int itemId = StringUtilities.parseInt( itemMatcher.group( 1 ) );
		if ( ItemDatabase.getItemName( itemId ) == null )
		{
			return null;
		}

		int itemCount = 1;

		if ( urlString.indexOf( "multiuse.php" ) != -1 || urlString.indexOf( "skills.php" ) != -1 )
		{
			Matcher quantityMatcher = UseItemRequest.QUANTITY_PATTERN.matcher( urlString );
			if ( quantityMatcher.find() )
			{
				itemCount = StringUtilities.parseInt( quantityMatcher.group( 1 ) );
			}
		}

		return new AdventureResult( itemId, itemCount );
	}

	public static final void resetItemUsed()
	{
		UseItemRequest.lastItemUsed = null;
	}

	public static final boolean registerRequest( final String urlString )
	{
		UseItemRequest.lastItemUsed = UseItemRequest.extractItem( urlString );
		if ( UseItemRequest.lastItemUsed == null )
		{
			return false;
		}

		int consumptionType = ItemDatabase.getConsumptionType( UseItemRequest.lastItemUsed.getItemId() );
		if ( consumptionType == KoLConstants.NO_CONSUME )
		{
			return false;
		}

		String useTypeAsString =
			consumptionType == KoLConstants.CONSUME_EAT ? "eat " : consumptionType == KoLConstants.CONSUME_DRINK ? "drink " : "use ";

		if ( consumptionType == KoLConstants.CONSUME_EAT )
		{
			int fullness = ItemDatabase.getFullness( UseItemRequest.lastItemUsed.getName() );
			if ( fullness > 0 && KoLCharacter.getFullness() + fullness <= KoLCharacter.getFullnessLimit() )
			{
				Preferences.setInteger( "currentFullness", KoLCharacter.getFullness() + fullness );
				Preferences.setInteger( "munchiesPillsUsed", Math.max(
					Preferences.getInteger( "munchiesPillsUsed" ) - 1, 0 ) );
			}
		}
		else
		{
			if ( UseItemRequest.lastItemUsed.getItemId() == ItemPool.EXPRESS_CARD )
			{
				Preferences.setBoolean( "expressCardUsed", true );
			}

			if ( UseItemRequest.lastItemUsed.getItemId() == UseItemRequest.MUNCHIES_PILL )
			{
				Preferences.setInteger(
					"munchiesPillsUsed",
					Preferences.getInteger( "munchiesPillsUsed" ) + UseItemRequest.lastItemUsed.getCount() );
			}

			int spleenHit =
				ItemDatabase.getSpleenHit( UseItemRequest.lastItemUsed.getName() ) * UseItemRequest.lastItemUsed.getCount();
			if ( spleenHit > 0 && KoLCharacter.getSpleenUse() + spleenHit <= KoLCharacter.getSpleenLimit() )
			{
				Preferences.setInteger(
					"currentSpleenUse",
					KoLCharacter.getSpleenUse() + spleenHit );
			}
		}

		if ( UseItemRequest.lastItemUsed.getItemId() == UseItemRequest.BLACK_MARKET_MAP && KoLCharacter.getFamiliar().getId() != 59 )
		{
			AdventureResult map = UseItemRequest.lastItemUsed;
			FamiliarData blackbird = new FamiliarData( 59 );
			if ( !KoLCharacter.getFamiliarList().contains( blackbird ) )
			{
				( new UseItemRequest( new AdventureResult( 2052, 1 ) ) ).run();
			}

			if ( !KoLmafia.permitsContinue() )
			{
				UseItemRequest.lastItemUsed = map;
				return true;
			}

			( new FamiliarRequest( blackbird ) ).run();
			UseItemRequest.lastItemUsed = map;
		}

		RequestLogger.updateSessionLog();
		RequestLogger.updateSessionLog( useTypeAsString + UseItemRequest.lastItemUsed.getCount() + " " + UseItemRequest.lastItemUsed.getName() );
		return true;
	}
}
