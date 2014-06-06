/**
 * Copyright (c) 2005-2014, KoLmafia development team
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

import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.TimeZone;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.FamiliarData;
import net.sourceforge.kolmafia.KoLAdventure;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.SpecialOutfit;

import net.sourceforge.kolmafia.moods.HPRestoreItemList;
import net.sourceforge.kolmafia.moods.MPRestoreItemList;
import net.sourceforge.kolmafia.moods.ManaBurnManager;
import net.sourceforge.kolmafia.moods.RecoveryManager;

import net.sourceforge.kolmafia.objectpool.AdventurePool;
import net.sourceforge.kolmafia.objectpool.EffectPool;
import net.sourceforge.kolmafia.objectpool.EffectPool.Effect;
import net.sourceforge.kolmafia.objectpool.FamiliarPool;
import net.sourceforge.kolmafia.objectpool.IntegerPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;

import net.sourceforge.kolmafia.persistence.AdventureDatabase;
import net.sourceforge.kolmafia.persistence.ConcoctionDatabase;
import net.sourceforge.kolmafia.persistence.EquipmentDatabase;
import net.sourceforge.kolmafia.persistence.FamiliarDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.persistence.MonsterDatabase.Element;
import net.sourceforge.kolmafia.persistence.QuestDatabase;
import net.sourceforge.kolmafia.persistence.QuestDatabase.Quest;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.session.BugbearManager;
import net.sourceforge.kolmafia.session.ChoiceManager;
import net.sourceforge.kolmafia.session.DreadScrollManager;
import net.sourceforge.kolmafia.session.EquipmentManager;
import net.sourceforge.kolmafia.session.InventoryManager;
import net.sourceforge.kolmafia.session.QuestManager;
import net.sourceforge.kolmafia.session.ResponseTextParser;
import net.sourceforge.kolmafia.session.ResultProcessor;
import net.sourceforge.kolmafia.session.SorceressLairManager;
import net.sourceforge.kolmafia.session.TurnCounter;

import net.sourceforge.kolmafia.swingui.GenericFrame;

import net.sourceforge.kolmafia.textui.command.ZapCommand;

import net.sourceforge.kolmafia.utilities.InputFieldUtilities;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class UseItemRequest
	extends GenericRequest
{
	public static final AdventureResult INFERNAL_SEAL_CLAW = ItemPool.get( ItemPool.INFERNAL_SEAL_CLAW, 1 );

	public static final Pattern DOWHICHITEM_PATTERN = Pattern.compile( "dowhichitem=(\\d+)" );

	private static final Pattern ROW_PATTERN = Pattern.compile( "<tr>.*?</tr>" );
	private static final Pattern INVENTORY_PATTERN = Pattern.compile( "</blockquote></td></tr></table>.*?</body>" );
	private static final Pattern HELPER_PATTERN = Pattern.compile( "(utensil|whichcard)=(\\d+)" );
	private static final Pattern BRICKO_PATTERN = Pattern.compile( "You break apart your ([\\w\\s]*)." );
	private static final Pattern FAMILIAR_NAME_PATTERN =
		Pattern.compile( "You decide to name (?:.*?) <b>(.*?)</b>" );
	private static final Pattern FRUIT_TUBING_PATTERN =
		Pattern.compile( "(?=.*?action=addfruit).*whichfruit=(\\d+)" );
	private static final Pattern ADVENTUROUS_RESOLUTION_PATTERN =
		Pattern.compile( "resolve to do it again" );
	private static final Pattern MERKIN_WORDQUIZ_PATTERN =
		Pattern.compile( "Your Mer-kin vocabulary mastery is now at <b>(\\d*?)%</b>" );

	// It goes [Xd12] feet, and doesn't hit anything interesting.
	private static final Pattern ARROW_PATTERN =
		Pattern.compile( "It goes (\\d+) feet" );

	// You pull out the trivia card (#57/1200) and read it.
	private static final Pattern CARD_PATTERN = Pattern.compile( "You pull out the trivia card \\(#(\\d+)/(\\d+)\\) and read it." );

	// <p>Question:  Who wrote the banned detective novel <u>Quest of the Witch Mints</u>?<p>Answer:  Mssr. Johnny Simon.
	private static final Pattern QA_PATTERN = Pattern.compile( "(Question|Answer): *(.*?[\\.\\?])<" );

	// <center>Total evil: <b>200</b><p>Alcove: <b>50</b><br>Cranny: <b>50</b><br>Niche: <b>50</b><br>Nook: <b>50</b></center>
	private static final Pattern EVILOMETER_PATTERN =
		Pattern.compile( "<center>Total evil: <b>(\\d+)</b><p>Alcove: <b>(\\d+)</b><br>Cranny: <b>(\\d+)</b><br>Niche: <b>(\\d+)</b><br>Nook: <b>(\\d+)</b></center>" );

	private static final Pattern KEYOTRON_PATTERN =
		Pattern.compile( "Medbay:</td><td><b>(\\d)/3</b> bio-data segments collected</td></tr>"
			+ "<tr><td align=right>Waste Processing:</td><td><b>(\\d)/3</b> bio-data segments collected</td></tr>"
			+ "<tr><td align=right>Sonar:</td><td><b>(\\d)/3</b> bio-data segments collected</td></tr>"
			+ "<tr><td align=right>Science Lab:</td><td><b>(\\d)/6</b> bio-data segments collected</td></tr>"
			+ "<tr><td align=right>Morgue:</td><td><b>(\\d)/6</b> bio-data segments collected</td></tr>"
			+ "<tr><td align=right>Special Ops:</td><td><b>(\\d)/6</b> bio-data segments collected</td></tr>"
			+ "<tr><td align=right>Engineering:</td><td><b>(\\d)/9</b> bio-data segments collected</td></tr>"
			+ "<tr><td align=right>Navigation:</td><td><b>(\\d)/9</b> bio-data segments collected</td></tr>"
			+ "<tr><td align=right>Galley:</td><td><b>(\\d)/9</b> bio-data segments collected" );

	private static final HashMap<Integer,AdventureResult> LIMITED_USES = new HashMap<Integer,AdventureResult>();

	static
	{
		UseItemRequest.LIMITED_USES.put( IntegerPool.get( ItemPool.ASTRAL_MUSHROOM ), EffectPool.get( Effect.HALF_ASTRAL ) );

		UseItemRequest.LIMITED_USES.put( IntegerPool.get( ItemPool.ABSINTHE ), EffectPool.get( Effect.ABSINTHE ) );

		UseItemRequest.LIMITED_USES.put( IntegerPool.get( ItemPool.TURTLE_PHEROMONES ), EffectPool.get( Effect.EAU_DE_TORTUE ) );
	}

	public static String lastUpdate = "";
	public static String limiter = "";
	private static AdventureResult lastFruit = null;
	private static AdventureResult lastUntinker = null;
	private static boolean retrying = false;

	protected final int consumptionType;
	protected AdventureResult itemUsed = null;

	protected static AdventureResult lastItemUsed = null;
	protected static AdventureResult lastHelperUsed = null;
	private static int currentItemId = -1;

	private static int askedAboutPvP = 0;

	public static final UseItemRequest getInstance( final int itemId )
	{
		return UseItemRequest.getInstance( itemId, 1 );
	}

	public static final UseItemRequest getInstance( final int itemId, int itemCount )
	{
		return UseItemRequest.getInstance( ItemPool.get( itemId, itemCount ) );
	}

	public static final UseItemRequest getInstance( final AdventureResult item )
	{
		return UseItemRequest.getInstance( UseItemRequest.getConsumptionType( item ), item );
	}

	public static final UseItemRequest getInstance( final int consumptionType, final AdventureResult item )
	{
		switch ( consumptionType )
		{
		case KoLConstants.CONSUME_DRINK:
		case KoLConstants.CONSUME_DRINK_HELPER:
			return new DrinkItemRequest( item );
		case KoLConstants.CONSUME_EAT:
		case KoLConstants.CONSUME_FOOD_HELPER:
			return new EatItemRequest( item );
		}

		int spleenHit = ItemDatabase.getSpleenHit( item.getName() );
		if ( spleenHit > 0 )
		{
			return new SpleenItemRequest( item );
		}

		return new UseItemRequest( consumptionType, item );
	}

	protected UseItemRequest( final int consumptionType, final AdventureResult item )
	{
		this( UseItemRequest.getConsumptionLocation( consumptionType, item ), consumptionType, item );
	}

	private UseItemRequest( final String location, final int consumptionType, final AdventureResult item )
	{
		super( location );

		this.consumptionType = consumptionType;
		this.itemUsed = item;

		this.addFormField( "whichitem", String.valueOf( item.getItemId() ) );
	}

	public static final int getConsumptionType( final AdventureResult item )
	{
		int itemId = item.getItemId();
		int attrs = ItemDatabase.getAttributes( itemId );
		if ( (attrs & ItemDatabase.ATTR_USABLE) != 0 )
		{
			return KoLConstants.CONSUME_USE;
		}
		if ( (attrs & ItemDatabase.ATTR_MULTIPLE) != 0 )
		{
			return KoLConstants.CONSUME_MULTIPLE;
		}
		if ( (attrs & ItemDatabase.ATTR_REUSABLE) != 0 )
		{
			return KoLConstants.INFINITE_USES;
		}

		switch ( itemId )
		{
		case ItemPool.HOBO_CODE_BINDER:
		case ItemPool.STUFFED_BARON:
			return KoLConstants.MESSAGE_DISPLAY;
		}

		return ItemDatabase.getConsumptionType( itemId );
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
		case KoLConstants.CONSUME_HOBO:
		case KoLConstants.CONSUME_GHOST:
		case KoLConstants.CONSUME_SLIME:
			return "familiarbinger.php";
		case KoLConstants.CONSUME_SPHERE:
			return "campground.php";
		case KoLConstants.MP_RESTORE:
		case KoLConstants.HP_RESTORE:
		case KoLConstants.HPMP_RESTORE:
		case KoLConstants.CONSUME_MULTIPLE:
			if ( item.getCount() > 1 )
			{
				return "multiuse.php";
			}
			return "inv_use.php";
		case KoLConstants.INFINITE_USES:
		{
			int type = ItemDatabase.getConsumptionType( item.getItemId() );
			return type == KoLConstants.CONSUME_MULTIPLE ?
				"multiuse.php" : "inv_use.php";
		}
		default:
			return "inv_use.php";
		}
	}

	public static void setLastItemUsed( final AdventureResult item )
	{
		UseItemRequest.lastItemUsed = item;
		UseItemRequest.currentItemId = item.getItemId();
	}

	public static final int currentItemId()
	{
		return UseItemRequest.currentItemId;
	}

	private final boolean isBingeRequest()
	{
		switch ( this.consumptionType )
		{
		case KoLConstants.CONSUME_HOBO:
		case KoLConstants.CONSUME_GHOST:
		case KoLConstants.CONSUME_SLIME:
		case KoLConstants.CONSUME_MIMIC:
			return true;
		}
		return false;
	}

	private static boolean needsConfirmation( final AdventureResult item )
	{
		switch ( item.getItemId() )
		{
		case ItemPool.NEWBIESPORT_TENT:
		case ItemPool.BARSKIN_TENT:
		case ItemPool.COTTAGE:
		case ItemPool.BRICKO_PYRAMID:
		case ItemPool.HOUSE:
		case ItemPool.SANDCASTLE:
		case ItemPool.GINORMOUS_PUMPKIN:
		case ItemPool.TWIG_HOUSE:
		case ItemPool.GINGERBREAD_HOUSE:
		case ItemPool.HOBO_FORTRESS:
		case ItemPool.GIANT_FARADAY_CAGE:
		case ItemPool.SNOW_FORT:
		case ItemPool.ELEVENT:
			return CampgroundRequest.getCurrentDwelling() != CampgroundRequest.BIG_ROCK;

		case ItemPool.HOT_BEDDING:
		case ItemPool.COLD_BEDDING:
		case ItemPool.STENCH_BEDDING:
		case ItemPool.SPOOKY_BEDDING:
		case ItemPool.SLEAZE_BEDDING:
		case ItemPool.BEANBAG_CHAIR:
		case ItemPool.GAUZE_HAMMOCK:
		case ItemPool.SALTWATERBED:
		case ItemPool.SPIRIT_BED:
			return CampgroundRequest.getCurrentBed() != null;
		}

		return false;
	}

	protected static boolean askAboutPvP( final String itemName )
	{
		// If we've already asked about PvP, don't nag.
		if ( UseItemRequest.askedAboutPvP == KoLCharacter.getUserId() )
		{
			return true;
		}

		int PvPGain = ItemDatabase.getPvPFights( itemName );

		// Does this item even give us PvP fights?
		if ( PvPGain <= 0 )
		{
			return true;
		}
		
		// Is the hippy stone broken?
		if ( KoLCharacter.getHippyStoneBroken() ) 
		{
			return true;
		}

		String message = "Are you sure you want consume that before breaking the hippy stone?";
		if ( !InputFieldUtilities.confirm( message ) )
		{
			return false;
		}

		UseItemRequest.askedAboutPvP = KoLCharacter.getUserId();

		return true;
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
		String itemName = ItemDatabase.getItemName( itemId );
		return UseItemRequest.maximumUses( itemId, itemName, KoLConstants.NO_CONSUME, true );
	}

	public static final int maximumUses( final int itemId, final int consumptionType )
	{
		String itemName = ItemDatabase.getItemName( itemId );
		return UseItemRequest.maximumUses( itemId, itemName, consumptionType, true );
	}

	public static final int maximumUses( final String itemName )
	{
		int itemId = ItemDatabase.getItemId( itemName );
		return UseItemRequest.maximumUses( itemId, itemName, KoLConstants.NO_CONSUME, false );
	}

	private static final int maximumUses( final int itemId, final String itemName, final int consumptionType, final boolean allowOverDrink )
	{
		if ( FightRequest.inMultiFight )
		{
			UseItemRequest.limiter = "multi-stage fight in progress";
			return 0;
		}

		if ( ChoiceManager.handlingChoice && !ChoiceManager.canWalkAway() )
		{
			UseItemRequest.limiter = "choice adventure in progress";
			return 0;
		}

		// Beecore path check

		switch ( itemId )
		{
		case ItemPool.BLACK_MARKET_MAP:
		case ItemPool.BALL_POLISH:
		case ItemPool.FRATHOUSE_BLUEPRINTS:
		case ItemPool.BINDER_CLIP:
			// These "B" items ARE usable in Beecore.
		case ItemPool.ICE_BABY:
		case ItemPool.JUGGLERS_BALLS:
		case ItemPool.EYEBALL_PENDANT:
		case ItemPool.SPOOKY_PUTTY_BALL:
		case ItemPool.LOATHING_LEGION_ABACUS:
		case ItemPool.LOATHING_LEGION_DEFIBRILLATOR:
		case ItemPool.LOATHING_LEGION_DOUBLE_PRISM:
		case ItemPool.LOATHING_LEGION_ROLLERBLADES:
			// And so are these IOTM foldables
			return Integer.MAX_VALUE;

		case ItemPool.COBBS_KNOB_MAP:
			// This "B" item IS usable in Beecore.
			UseItemRequest.limiter = "encryption key";
			return InventoryManager.getCount( ItemPool.ENCRYPTION_KEY );

		default:
			if ( KoLCharacter.inBeecore() && KoLCharacter.hasBeeosity( itemName ) )
			{
				UseItemRequest.limiter = "bees";
				return 0;
			}
			break;
		}

		// Check binge requests before checking fullness or inebriety
		switch ( consumptionType )
		{
		case KoLConstants.CONSUME_HOBO:
		case KoLConstants.CONSUME_GHOST:
		case KoLConstants.CONSUME_SLIME:
			return Integer.MAX_VALUE;
		case KoLConstants.CONSUME_GUARDIAN:
			UseItemRequest.limiter = "character class";
			return KoLCharacter.getClassType() == KoLCharacter.PASTAMANCER ? 1 : 0;
		}

		// Delegate to specialized classes as appropriate

		int inebriety = ItemDatabase.getInebriety( itemName );
		if ( inebriety > 0 )
		{
			return DrinkItemRequest.maximumUses( itemId, itemName, inebriety, allowOverDrink );
		}

		int fullness = ItemDatabase.getFullness( itemName );
		if ( fullness > 0 )
		{
			return EatItemRequest.maximumUses( itemId, itemName, fullness );
		}

		int spleenHit = ItemDatabase.getSpleenHit( itemName );
		if ( spleenHit > 0 )
		{
			return SpleenItemRequest.maximumUses( itemId, itemName, spleenHit );
		}

		int restorationMaximum = UseItemRequest.getRestorationMaximum( itemName );
		if ( restorationMaximum < Integer.MAX_VALUE )
		{
			UseItemRequest.limiter = "needed restoration";
			return restorationMaximum;
		}

		if ( itemId <= 0 )
		{
			return Integer.MAX_VALUE;
		}

		// Set reasonable default if the item fails to set a specific reason
		UseItemRequest.limiter = "a wizard";

		switch ( itemId )
		{
		case ItemPool.TINY_HOUSE:
		case ItemPool.TEARS:
			// These restore HP/MP but also remove Beaten Up
			// If you are currently Beaten Up, allow a single
			// usage. Otherwise, let code below limit based on
			// needed HP/MP recovery
			if ( KoLConstants.activeEffects.contains( KoLAdventure.BEATEN_UP ) )
			{
				UseItemRequest.limiter = "needed restoration";
				return 1;
			}
			break;

		case ItemPool.GONG:
		case ItemPool.KETCHUP_HOUND:
		case ItemPool.MEDICINAL_HERBS:
			UseItemRequest.limiter = "usability";
			return 1;

		case ItemPool.FIELD_GAR_POTION:
			// Disallow using potion if already Gar-ish
			Calendar date = Calendar.getInstance( TimeZone.getTimeZone( "GMT-0700" ) );
			if( date.get( Calendar.DAY_OF_WEEK ) == Calendar.MONDAY )
			{
				UseItemRequest.limiter = "uselessness on Mondays";
				return 0;
			}
			if ( KoLConstants.activeEffects.contains( EffectPool.get( Effect.GARISH ) ) )
			{
				UseItemRequest.limiter = "existing effect";
				return 0;
			}
			return 1;

		case ItemPool.TOASTER:
			UseItemRequest.limiter = "usability";
			return 3 - Preferences.getInteger( "_toastSummons" );

		case ItemPool.AMINO_ACIDS:
			UseItemRequest.limiter = "usability";
			return 3;

		case ItemPool.DANCE_CARD:
			// Disallow using a dance card if already active
			if ( TurnCounter.isCounting( "Dance Card" ) )
			{
				UseItemRequest.limiter = "existing counter";
				return 0;
			}
			// Or if another counter would end on the same turn
			UseItemRequest.limiter = TurnCounter.getCounters( "", 3, 3 );
			return UseItemRequest.limiter.length() > 0 ? 0 : 1;

		case ItemPool.GREEN_PEAWEE_MARBLE:
		case ItemPool.BROWN_CROCK_MARBLE:
		case ItemPool.RED_CHINA_MARBLE:
		case ItemPool.LEMONADE_MARBLE:
		case ItemPool.BUMBLEBEE_MARBLE:
		case ItemPool.JET_BENNIE_MARBLE:
		case ItemPool.BEIGE_CLAMBROTH_MARBLE:
		case ItemPool.STEELY_MARBLE:
		case ItemPool.BEACH_BALL_MARBLE:
		case ItemPool.BLACK_CATSEYE_MARBLE:
			// Using up to 1/2 produces bigger marbles.
			// Larger quantities can be used, but do nothing.
			UseItemRequest.limiter = "1/2 inventory";
			return InventoryManager.getCount( itemId ) / 2;

		case ItemPool.CHEF:
		case ItemPool.CLOCKWORK_CHEF:
		case ItemPool.BARTENDER:
		case ItemPool.CLOCKWORK_BARTENDER:
		case ItemPool.MAID:
		case ItemPool.CLOCKWORK_MAID:
		case ItemPool.SCARECROW:
		case ItemPool.MEAT_GOLEM:
		case ItemPool.MEAT_GLOBE:
			// Campground equipment
		case ItemPool.BEANBAG_CHAIR:
		case ItemPool.GAUZE_HAMMOCK:
		case ItemPool.HOT_BEDDING:
		case ItemPool.COLD_BEDDING:
		case ItemPool.STENCH_BEDDING:
		case ItemPool.SPOOKY_BEDDING:
		case ItemPool.SLEAZE_BEDDING:
		case ItemPool.SALTWATERBED:
		case ItemPool.SPIRIT_BED:
		case ItemPool.BLACK_BLUE_LIGHT:
		case ItemPool.LOUDMOUTH_LARRY:
		case ItemPool.PLASMA_BALL:
		case ItemPool.FENG_SHUI:
			// Dwelling furnishings
			UseItemRequest.limiter = "campground regulations";
			return 1;

		case ItemPool.ANCIENT_CURSED_FOOTLOCKER:
			UseItemRequest.limiter = "simple cursed key";
			return InventoryManager.getCount( ItemPool.SIMPLE_CURSED_KEY );

		case ItemPool.ORNATE_CURSED_CHEST:
			UseItemRequest.limiter = "ornate cursed key";
			return InventoryManager.getCount( ItemPool.ORNATE_CURSED_KEY );

		case ItemPool.GILDED_CURSED_CHEST:
			UseItemRequest.limiter = "gilded cursed key";
			return InventoryManager.getCount( ItemPool.GILDED_CURSED_KEY );

		case ItemPool.STUFFED_CHEST:
			UseItemRequest.limiter = "stuffed key";
			return InventoryManager.getCount( ItemPool.STUFFED_KEY );

		case ItemPool.PHOTOCOPIER:
			UseItemRequest.limiter = "photocopied monster";
			return InventoryManager.hasItem( ItemPool.PHOTOCOPIED_MONSTER ) ? 0 : 1;

		case ItemPool.MOJO_FILTER:
			int spleenUsed = KoLCharacter.getSpleenUse();
			int mojoUsesLeft = Math.max( 0, 3 - Preferences.getInteger( "currentMojoFilters" ) );
			if( mojoUsesLeft <= spleenUsed )
			{
				UseItemRequest.limiter = "daily limit";
				return mojoUsesLeft;
			}
			UseItemRequest.limiter = "spleen";
			return spleenUsed;

		case ItemPool.EXPRESS_CARD:
			UseItemRequest.limiter = "daily limit";
			return Preferences.getBoolean( "expressCardUsed" ) ? 0 : 1;

		case ItemPool.SPICE_MELANGE:
			UseItemRequest.limiter = "daily limit";
			return Preferences.getBoolean( "spiceMelangeUsed" ) ? 0 : 1;

		case ItemPool.ULTRA_MEGA_SOUR_BALL:
			UseItemRequest.limiter = "daily limit";
			return Preferences.getBoolean( "_ultraMegaSourBallUsed" ) ? 0 : 1;

		case ItemPool.BORROWED_TIME:
			UseItemRequest.limiter = "daily limit";
			return Preferences.getBoolean( "_borrowedTimeUsed" ) ? 0 : 1;

		case ItemPool.SYNTHETIC_DOG_HAIR_PILL:
			if ( KoLCharacter.getInebriety() == 0 )
			{
				UseItemRequest.limiter = "sobriety";
				return 0;
			}
			UseItemRequest.limiter = "daily limit";
			return Preferences.getBoolean( "_syntheticDogHairPillUsed" ) ? 0 : 1;

		case ItemPool.DISTENTION_PILL:
			UseItemRequest.limiter = "daily limit";
			return Preferences.getBoolean( "_distentionPillUsed" ) ? 0 : 1;

		case ItemPool.BURROWGRUB_HIVE:
			UseItemRequest.limiter = "daily limit";
			return Preferences.getBoolean( "burrowgrubHiveUsed" ) ? 0 : 1;

		case ItemPool.MOVEABLE_FEAST:
			String familiar = KoLCharacter.getFamiliar().getRace();
			if ( Preferences.getString( "_feastedFamiliars" ).indexOf( familiar ) != -1 )
			{
				UseItemRequest.limiter = "a previous " + familiar + " feasting";
				return 0;
			}
			UseItemRequest.limiter = "daily limit";
			return Math.max( 0, 5 - Preferences.getInteger( "_feastUsed" ) );

		case ItemPool.MILK_OF_MAGNESIUM:
			UseItemRequest.limiter = "remaining fullness";
			int milkyTurns = ItemDatabase.MILK.getCount( KoLConstants.activeEffects );
			int fullnessAvailable = KoLCharacter.getFullnessLimit() - KoLCharacter.getFullness();
			// If our current dose of Got Milk is sufficient to
			// fill us up, no milk is needed.
			int unmilkedTurns = fullnessAvailable - milkyTurns;
			if ( unmilkedTurns <= 0 )
			{
				return 0;
			}

			// Otherwise, limit to number of useful potions
			int milkDuration = 10 +
				( KoLCharacter.getClassType() == KoLCharacter.SAUCEROR ? 5 : 0 ) +
				( KoLCharacter.hasSkill( "Impetuous Sauciness" ) ? 5 : 0 );

			int limit = 0;
			while ( unmilkedTurns > 0 )
			{
				unmilkedTurns -= milkDuration;
				limit++;
			}
			return limit;

		case ItemPool.GHOSTLY_BODY_PAINT:
		case ItemPool.NECROTIZING_BODY_SPRAY:
		case ItemPool.BITE_LIPSTICK:
		case ItemPool.WHISKER_PENCIL:
		case ItemPool.PRESS_ON_RIBS:
			if ( KoLConstants.activeEffects.contains(
					EffectPool.get( Effect.HAUNTING_LOOKS ) ) ||
				 KoLConstants.activeEffects.contains(
					EffectPool.get( Effect.DEAD_SEXY ) ) ||
				 KoLConstants.activeEffects.contains(
					EffectPool.get( Effect.VAMPIN ) ) ||
				 KoLConstants.activeEffects.contains(
					EffectPool.get( Effect.YIFFABLE_YOU ) ) ||
				 KoLConstants.activeEffects.contains(
					EffectPool.get( Effect.BONE_US_ROUND ) ) )
			{
				UseItemRequest.limiter = "your current sexy costume";
				return 0;
			}
			return 1;

		case ItemPool.BLACK_PAINT:
			if ( KoLCharacter.inFistcore() )
			{
				UseItemRequest.limiter = "your teachings";
				return 0;
			}
			return Integer.MAX_VALUE;

		case ItemPool.SLAPFIGHTING_BOOK:
		case ItemPool.SLAPFIGHTING_BOOK_USED:
		case ItemPool.UNCLE_ROMULUS:
		case ItemPool.UNCLE_ROMULUS_USED:
		case ItemPool.SNAKE_CHARMING_BOOK:
		case ItemPool.SNAKE_CHARMING_BOOK_USED:
		case ItemPool.ZU_MANNKASE_DIENEN:
		case ItemPool.ZU_MANNKASE_DIENEN_USED:
		case ItemPool.DYNAMITE_SUPERMAN_JONES:
		case ItemPool.DYNAMITE_SUPERMAN_JONES_USED:
		case ItemPool.INIGO_BOOK:
		case ItemPool.INIGO_BOOK_USED:
			String bookClass = UseItemRequest.itemToClass( itemId );
			if ( !bookClass.equals( KoLCharacter.getClassType() ) )
			{
				UseItemRequest.limiter = "your class";
				return 0;
			}
			return Integer.MAX_VALUE;

		case ItemPool.ALL_YEAR_SUCKER:
			UseItemRequest.limiter = "daily limit";
			return Preferences.getBoolean( "_allYearSucker" ) ? 0 : 1;

		case ItemPool.DARK_CHOCOLATE_HEART:
			UseItemRequest.limiter = "daily limit";
			return Preferences.getBoolean( "_darkChocolateHeart" ) ? 0 : 1;

		case ItemPool.JACKASS_PLUMBER_GAME:
			UseItemRequest.limiter = "daily limit";
			return Preferences.getBoolean( "_jackassPlumberGame" ) ? 0 : 1;

		case ItemPool.TRIVIAL_AVOCATIONS_GAME:
			UseItemRequest.limiter = "daily limit";
			return Preferences.getBoolean( "_trivialAvocationsGame" ) ? 0 : 1;

		case ItemPool.RESOLUTION_ADVENTUROUS:
			UseItemRequest.limiter = "daily limit";
			return ( Preferences.getInteger( "_resolutionAdv" ) == 10 ? 0 : Integer.MAX_VALUE );

		case ItemPool.ESSENTIAL_TOFU:
			UseItemRequest.limiter = "daily limit";
			return Preferences.getBoolean( "_essentialTofuUsed" ) ? 0 : 1;

		case ItemPool.VITACHOC_CAPSULE:
			UseItemRequest.limiter = "daily limit";
			return ( 3 - Preferences.getInteger( "_vitachocCapsulesUsed" ) );

		case ItemPool.CHOCOLATE_CIGAR:
			UseItemRequest.limiter = "daily limit";
			return ( 3 - Preferences.getInteger( "_chocolateCigarsUsed" ) );

		case ItemPool.FANCY_CHOCOLATE:
		case ItemPool.FANCY_CHOCOLATE_CAR:
		case ItemPool.FANCY_EVIL_CHOCOLATE:
		case ItemPool.CHOCOLATE_DISCO_BALL:
		case ItemPool.CHOCOLATE_PASTA_SPOON:
		case ItemPool.CHOCOLATE_SAUCEPAN:
		case ItemPool.CHOCOLATE_SEAL_CLUBBING_CLUB:
		case ItemPool.CHOCOLATE_STOLEN_ACCORDION:
		case ItemPool.CHOCOLATE_TURTLE_TOTEM:
		case ItemPool.BEET_MEDIOCREBAR:
		case ItemPool.CORN_MEDIOCREBAR:
		case ItemPool.CABBAGE_MEDIOCREBAR:
			UseItemRequest.limiter = "daily limit";
			return ( 3 - Preferences.getInteger( "_chocolatesUsed" ) );

		case ItemPool.CREEPY_VOODOO_DOLL:
			UseItemRequest.limiter = "daily limit";
			return Preferences.getBoolean( "_creepyVoodooDollUsed" ) ? 0 : 1;

		case ItemPool.HOBBY_HORSE:
			UseItemRequest.limiter = "daily limit";
			return Preferences.getBoolean( "_hobbyHorseUsed" ) ? 0 : 1;

		case ItemPool.BALL_IN_A_CUP:
			UseItemRequest.limiter = "daily limit";
			return Preferences.getBoolean( "_ballInACupUsed" ) ? 0 : 1;

		case ItemPool.SET_OF_JACKS:
			UseItemRequest.limiter = "daily limit";
			return Preferences.getBoolean( "_setOfJacksUsed" ) ? 0 : 1;

		case ItemPool.BAG_OF_CANDY:
			UseItemRequest.limiter = "daily limit";
			return Preferences.getBoolean( "_bagOfCandyUsed" ) ? 0 : 1;

		case ItemPool.EMBLEM_AKGYXOTH:
		case ItemPool.IDOL_AKGYXOTH:
			UseItemRequest.limiter = "daily limit";
			return Preferences.getBoolean( "_akgyxothUsed" ) ? 0 : 1;

		case ItemPool.GNOLL_EYE:
			UseItemRequest.limiter = "daily limit";
			return Preferences.getBoolean( "_gnollEyeUsed" ) ? 0 : 1;

		case ItemPool.KOL_CON_SIX_PACK:
			UseItemRequest.limiter = "daily limit";
			return Preferences.getBoolean( "_kolConSixPackUsed" ) ? 0 : 1;

		case ItemPool.MUS_MANUAL:
		case ItemPool.MYS_MANUAL:
		case ItemPool.MOX_MANUAL:
			UseItemRequest.limiter = "daily limit";
			return Preferences.getBoolean( "_guildManualUsed" ) ? 0 : 1;

		case ItemPool.STUFFED_POCKETWATCH:
			UseItemRequest.limiter = "daily limit";
			return Preferences.getBoolean( "_stuffedPocketwatchUsed" ) ? 0 : 1;

		case ItemPool.STYX_SPRAY:
			UseItemRequest.limiter = "daily limit";
			return Preferences.getBoolean( "_styxSprayUsed" ) ? 0 : 1;

		case ItemPool.STABONIC_SCROLL:
			UseItemRequest.limiter = "daily limit";
			return Preferences.getBoolean( "_stabonicScrollUsed" ) ? 0 : 1;

		case ItemPool.COAL_PAPERWEIGHT:
			UseItemRequest.limiter = "daily limit";
			return Preferences.getBoolean( "_coalPaperweightUsed" ) ? 0 : 1;

		case ItemPool.JINGLE_BELL:
			UseItemRequest.limiter = "daily limit";
			return Preferences.getBoolean( "_jingleBellUsed" ) ? 0 : 1;

		case ItemPool.BOX_OF_HAMMERS:
			UseItemRequest.limiter = "daily limit";
			return Preferences.getBoolean( "_boxOfHammersUsed" ) ? 0 : 1;

		case ItemPool.TEMPURA_AIR:
			UseItemRequest.limiter = "daily limit";
			return Preferences.getBoolean( "_tempuraAirUsed" ) ? 0 : 1;

		case ItemPool.PRESSURIZED_PNEUMATICITY:
			UseItemRequest.limiter = "daily limit";
			return Preferences.getBoolean( "_pneumaticityPotionUsed" ) ? 0 : 1;

		case ItemPool.HYPERINFLATED_SEAL_LUNG:
			UseItemRequest.limiter = "daily limit";
			return Preferences.getBoolean( "_hyperinflatedSealLungUsed" ) ? 0 : 1;

		case ItemPool.BALLAST_TURTLE:
			UseItemRequest.limiter = "daily limit";
			return Preferences.getBoolean( "_ballastTurtleUsed" ) ? 0 : 1;

		case ItemPool.CSA_FIRE_STARTING_KIT:
			if ( !KoLCharacter.getHippyStoneBroken() && Preferences.getInteger( "choiceAdventure595" ) == 1 )
			{
				UseItemRequest.limiter = "an unbroken hippy stone";
				return 0;
			}
			UseItemRequest.limiter = "daily limit";
			return Preferences.getBoolean( "_fireStartingKitUsed" ) ? 0 : 1;

		case ItemPool.LEFT_BEAR_ARM:
			UseItemRequest.limiter = "insufficient right bear arms";
			return InventoryManager.getCount( ItemPool.RIGHT_BEAR_ARM );

		case ItemPool.LEGENDARY_BEAT:
			UseItemRequest.limiter = "daily limit";
			return Preferences.getBoolean( "_legendaryBeat" ) ? 0 : 1;

		case ItemPool.CURSED_MICROWAVE:
			UseItemRequest.limiter = "daily limit";
			return Preferences.getBoolean( "_cursedMicrowaveUsed" ) ? 0 : 1;

		case ItemPool.CURSED_KEG:
			UseItemRequest.limiter = "daily limit";
			return Preferences.getBoolean( "_cursedKegUsed" ) ? 0 : 1;

		case ItemPool.TACO_FLIER:
			UseItemRequest.limiter = "daily limit";
			return Preferences.getBoolean( "_tacoFlierUsed" ) ? 0 : 1;

		case ItemPool.SUSPICIOUS_JAR:
		case ItemPool.GOURD_JAR:
		case ItemPool.MYSTIC_JAR:
		case ItemPool.OLD_MAN_JAR:
		case ItemPool.ARTIST_JAR:
		case ItemPool.MEATSMITH_JAR:
		case ItemPool.JICK_JAR:
			UseItemRequest.limiter = "daily limit";
			return Preferences.getBoolean( "_psychoJarUsed" ) ? 0 : 1;

		case ItemPool.FISHY_PIPE:
			UseItemRequest.limiter = "daily limit";
			return Preferences.getBoolean( "_fishyPipeUsed" ) ? 0 : 1;

		case ItemPool.SUSHI_ROLLING_MAT:
			UseItemRequest.limiter = "usability";
			return KoLCharacter.hasSushiMat() ? 0 : 1;

		case ItemPool.DEFECTIVE_TOKEN:
			UseItemRequest.limiter = "daily limit";
			return Preferences.getBoolean( "_defectiveTokenUsed" ) ? 0 : 1;

		case ItemPool.SILVER_DREAD_FLASK:
			UseItemRequest.limiter = "daily limit";
			return Preferences.getBoolean( "_silverDreadFlaskUsed" ) ? 0 : 1;

		case ItemPool.BRASS_DREAD_FLASK:
			UseItemRequest.limiter = "daily limit";
			return Preferences.getBoolean( "_brassDreadFlaskUsed" ) ? 0 : 1;

		case ItemPool.ETERNAL_CAR_BATTERY:
			UseItemRequest.limiter = "daily limit";
			return Preferences.getBoolean( "_eternalCarBatteryUsed" ) ? 0 : 1;

		case ItemPool.FOLDER_01:
		case ItemPool.FOLDER_02:
		case ItemPool.FOLDER_03:
		case ItemPool.FOLDER_04:
		case ItemPool.FOLDER_05:
		case ItemPool.FOLDER_06:
		case ItemPool.FOLDER_07:
		case ItemPool.FOLDER_08:
		case ItemPool.FOLDER_09:
		case ItemPool.FOLDER_10:
		case ItemPool.FOLDER_11:
		case ItemPool.FOLDER_12:
		case ItemPool.FOLDER_13:
		case ItemPool.FOLDER_14:
		case ItemPool.FOLDER_15:
		case ItemPool.FOLDER_16:
		case ItemPool.FOLDER_17:
		case ItemPool.FOLDER_18:
		case ItemPool.FOLDER_19:
		case ItemPool.FOLDER_20:
		case ItemPool.FOLDER_21:
		case ItemPool.FOLDER_22:
		case ItemPool.FOLDER_23:
			UseItemRequest.limiter = "folder holder";
			return EquipmentRequest.availableFolder() == -1 ? 0 : 1;

		case ItemPool.PASTA_ADDITIVE:
			if ( KoLCharacter.getClassType() != KoLCharacter.PASTAMANCER )
			{
				UseItemRequest.limiter = "character class";
				return 0;
			}
			UseItemRequest.limiter = "daily limit";
			return Preferences.getBoolean( "_pastaAdditive" ) ? 0 : 1;

		case ItemPool.WARBEAR_BREAKFAST_MACHINE:
			UseItemRequest.limiter = "daily limit";
			return Preferences.getBoolean( "_warbearBreakfastMachineUsed" ) ? 0 : 1;

		case ItemPool.WARBEAR_SODA_MACHINE:
			UseItemRequest.limiter = "daily limit";
			return Preferences.getBoolean( "_warbearSodaMachineUsed" ) ? 0 : 1;

		case ItemPool.WARBEAR_GYROCOPTER:
		case ItemPool.WARBEAR_GYROCOPTER_BROKEN:
			UseItemRequest.limiter = "daily limit";
			return Preferences.getBoolean( "_warbearGyrocopterUsed" ) ? 0 : 1;

		case ItemPool.WARBEAR_BANK:
			UseItemRequest.limiter = "daily limit";
			return Preferences.getBoolean( "_warbearBankUsed" ) ? 0 : 1;

		case ItemPool.LUPINE_APPETITE_HORMONES:
			UseItemRequest.limiter = "daily limit";
			return Preferences.getBoolean( "_lupineHormonesUsed" ) ? 0 : 1;

		case ItemPool.BLANK_OUT_BOTTLE:
			UseItemRequest.limiter = "daily limit";
			return Preferences.getBoolean( "_blankoutUsed" ) ? 0 : 1;

		case ItemPool.CORRUPTED_STARDUST:
		case ItemPool.PIXEL_ORB:
			UseItemRequest.limiter = "daily limit";
			return Preferences.getBoolean( "_corruptedStardustUsed" ) ? 0 : 1;

		case ItemPool.SWEET_TOOTH:
			UseItemRequest.limiter = "daily limit";
			return Preferences.getBoolean( "_sweetToothUsed" ) ? 0 : 1;
		}

		if ( CampgroundRequest.isWorkshedItem( itemId ) )
		{
			UseItemRequest.limiter = "daily limit";
			return Preferences.getBoolean( "_workshedItemUsed" ) ? 0 : 1;
		}

		switch ( consumptionType )
		{
		case KoLConstants.GROW_FAMILIAR:
			if ( KoLCharacter.inAxecore() )
			{
				UseItemRequest.limiter = "Boris's scorn for familiars";
				return 0;
			}
			UseItemRequest.limiter = "the fine print in your Familiar-Gro\u2122 Terrarium owner's manual";
			return 1;
		case KoLConstants.EQUIP_WEAPON:
			// Even if you can dual-wield, if we attempt to "use" a
			// weapon, it will become an "equip", which always goes
			// in the main hand.
		case KoLConstants.EQUIP_FAMILIAR:
		case KoLConstants.EQUIP_HAT:
		case KoLConstants.EQUIP_PANTS:
		case KoLConstants.EQUIP_CONTAINER:
		case KoLConstants.EQUIP_SHIRT:
		case KoLConstants.EQUIP_OFFHAND:
			UseItemRequest.limiter = "slot";
			return 1;
		case KoLConstants.EQUIP_ACCESSORY:
			UseItemRequest.limiter = "slot";
			return 3;
		}

		Integer key = IntegerPool.get( itemId );

		if ( UseItemRequest.LIMITED_USES.containsKey( key ) )
		{
			UseItemRequest.limiter = "unstackable effect";
			return KoLConstants.activeEffects.contains( UseItemRequest.LIMITED_USES.get( key ) ) ? 0 : 1;
		}

		return Integer.MAX_VALUE;
	}

	protected static int getRestorationMaximum( final String itemName )
	{
		float hpRestored = HPRestoreItemList.getHealthRestored( itemName );
		boolean restoresHP = hpRestored != Integer.MIN_VALUE;
		float mpRestored = MPRestoreItemList.getManaRestored( itemName );
		boolean restoresMP = mpRestored != Integer.MIN_VALUE;

		if ( !restoresHP && !restoresMP )
		{
			return Integer.MAX_VALUE;
		}

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

		return maximumSuggested;
	}

	@Override
	public void run()
	{
		if ( GenericRequest.abortIfInFightOrChoice() )
		{
			return;
		}

		// Hide memento items from your familiars
		if ( this.isBingeRequest() &&
		     Preferences.getBoolean( "mementoListActive" ) &&
		     KoLConstants.mementoList.contains( this.itemUsed ) )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "Don't feed mementos to your familiars." );
			return;
		}

		// Equipment should be handled by a different kind of request.

		int itemId = this.itemUsed.getItemId();
		boolean isSealFigurine = ItemDatabase.isSealFigurine( itemId );
		boolean isBRICKOMonster = ItemDatabase.isBRICKOMonster( itemId );

		// Seal figurines require special handling in the HTML, but
		// they also require some use protection
		if ( isSealFigurine && !EquipmentManager.wieldingClub() )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "You really should wield a club before using that." );
			return;
		}

		switch ( itemId )
		{
		case ItemPool.BRICKO_SWORD:
		case ItemPool.BRICKO_HAT:
		case ItemPool.BRICKO_PANTS:
			if ( !InventoryManager.retrieveItem( this.itemUsed ) )
			{
				KoLmafia.updateDisplay( MafiaState.ERROR,
					"You don't have one of those." );
				return;
			}
			KoLmafia.updateDisplay( "Splitting bricks..." );
			GenericRequest req = new GenericRequest( "inventory.php?action=breakbricko&pwd&whichitem=" + itemId );
			RequestThread.postRequest( req );
			return;

		case ItemPool.STICKER_SWORD:
		case ItemPool.STICKER_CROSSBOW:
			if ( !InventoryManager.retrieveItem( this.itemUsed ) )
			{
				KoLmafia.updateDisplay( MafiaState.ERROR,
					"You don't have one of those." );
				return;
			}
			RequestThread.postRequest( new GenericRequest( "bedazzle.php?action=fold&pwd" ) );
			return;

		case ItemPool.MACGUFFIN_DIARY:
			RequestThread.postRequest( new GenericRequest( "diary.php?textversion=1" ) );
			QuestDatabase.setQuestIfBetter( Quest.MACGUFFIN, "step2" );
			QuestDatabase.setQuestIfBetter( Quest.PYRAMID, QuestDatabase.STARTED );
			QuestDatabase.setQuestIfBetter( Quest.MANOR, QuestDatabase.STARTED );
			QuestDatabase.setQuestIfBetter( Quest.PALINDOME, QuestDatabase.STARTED );
			QuestDatabase.setQuestIfBetter( Quest.WORSHIP, QuestDatabase.STARTED );

			KoLmafia.updateDisplay( "Your father's diary has been read." );
			return;

		case ItemPool.PUZZLE_PIECE:
			SorceressLairManager.completeHedgeMaze();
			return;

		case ItemPool.NEWBIESPORT_TENT:
		case ItemPool.BARSKIN_TENT:
		case ItemPool.COTTAGE:
		case ItemPool.BRICKO_PYRAMID:
		case ItemPool.HOUSE:
		case ItemPool.SANDCASTLE:
		case ItemPool.GINORMOUS_PUMPKIN:
		case ItemPool.TWIG_HOUSE:
		case ItemPool.GINGERBREAD_HOUSE:
		case ItemPool.HOBO_FORTRESS:
		case ItemPool.GIANT_FARADAY_CAGE:
		case ItemPool.SNOW_FORT:
		case ItemPool.ELEVENT:
			AdventureResult dwelling = CampgroundRequest.getCurrentDwelling();
			int oldLevel = CampgroundRequest.getCurrentDwellingLevel();
			int newLevel = CampgroundRequest.dwellingLevel( itemId );
			if ( newLevel < oldLevel && dwelling != null && !UseItemRequest.confirmReplacement( dwelling.getName() ) )
			{
				return;
			}
			break;

		case ItemPool.HOT_BEDDING:
		case ItemPool.COLD_BEDDING:
		case ItemPool.STENCH_BEDDING:
		case ItemPool.SPOOKY_BEDDING:
		case ItemPool.SLEAZE_BEDDING:
		case ItemPool.SALTWATERBED:
		case ItemPool.SPIRIT_BED:
		case ItemPool.BEANBAG_CHAIR:
		case ItemPool.GAUZE_HAMMOCK:
			AdventureResult bed = CampgroundRequest.getCurrentBed();
			if ( bed != null && !UseItemRequest.confirmReplacement( bed.getName() ) )
			{
				return;
			}
			break;

		case ItemPool.SPICE_MELANGE:
		case ItemPool.ULTRA_MEGA_SOUR_BALL:
			boolean unfilledStomach = false;
			boolean unfilledLiver = false;
			String organ = null;
			if ( KoLCharacter.canEat() && KoLCharacter.getFullness() < 3 )
			{
				unfilledStomach = true;
			}
			if ( KoLCharacter.canDrink() && KoLCharacter.getInebriety() < 3 )
			{
				unfilledLiver = true;
			}
			if ( !unfilledStomach && !unfilledLiver )
			{
				break;
			}
			if ( unfilledStomach && unfilledLiver )
			{
				organ = "stomach and liver";
			}
			else if ( unfilledStomach )
			{
				organ = "stomach";
			}
			else if ( unfilledLiver )
			{
				organ = "liver";
			}

			if ( !InputFieldUtilities.confirm( "A " + ItemDatabase.getItemName( itemId ) + "clears 3 " + organ +
				  " and you have not filled that yet.  Are you sure you want to use it?" ) )
			{
				return;
			}
			break;
		}

		switch ( this.consumptionType )
		{
		case KoLConstants.CONSUME_STICKER:
		case KoLConstants.CONSUME_CARD:
		case KoLConstants.CONSUME_FOLDER:
		case KoLConstants.EQUIP_HAT:
		case KoLConstants.EQUIP_WEAPON:
		case KoLConstants.EQUIP_OFFHAND:
		case KoLConstants.EQUIP_SHIRT:
		case KoLConstants.EQUIP_PANTS:
		case KoLConstants.EQUIP_CONTAINER:
		case KoLConstants.EQUIP_ACCESSORY:
		case KoLConstants.EQUIP_FAMILIAR:
			RequestThread.postRequest( new EquipmentRequest( this.itemUsed ) );
			return;

		case KoLConstants.CONSUME_SPHERE:
			RequestThread.postRequest( new PortalRequest( this.itemUsed ) );
			return;

		case KoLConstants.NO_CONSUME:
			// no primary use, but a secondary use may be applicable
			if ( ItemDatabase.getAttribute( itemId, ItemDatabase.ATTR_CURSE ) )
			{
				RequestThread.postRequest( new CurseRequest( this.itemUsed ) );
				return;
			}
			KoLmafia.updateDisplay( this.itemUsed.getName() + " is unusable." );
			return;
		}

		UseItemRequest.lastUpdate = "";
		if ( !ItemDatabase.meetsLevelRequirement( this.itemUsed.getName() ) )
		{
			UseItemRequest.lastUpdate = "Insufficient level to consume " + this.itemUsed;
			KoLmafia.updateDisplay( MafiaState.ERROR, UseItemRequest.lastUpdate );
			return;
		}

		int maximumUses = UseItemRequest.maximumUses( itemId, this.consumptionType );
		if ( maximumUses < this.itemUsed.getCount() )
		{
			KoLmafia.updateDisplay( "(usable quantity of " + this.itemUsed +
				" is limited to " + maximumUses + " by " +
				UseItemRequest.limiter + ")" );
			this.itemUsed = this.itemUsed.getInstance( maximumUses );
		}

		if ( this.itemUsed.getCount() < 1 )
		{
			return;
		}

		switch ( itemId )
		{
		case ItemPool.LUCIFER:
			// Burn any existing MP that would otherwise be wasted.
			ManaBurnManager.burnMana( KoLCharacter.getMaximumMP() - 9 * (KoLCharacter.getCurrentHP() - 1) );
			break;
		case ItemPool.WHAT_CARD:
		case ItemPool.WHEN_CARD:
		case ItemPool.WHO_CARD:
		case ItemPool.WHERE_CARD:
			this.addFormField( "answerplz", "1" );
			break;

		case ItemPool.PHIAL_OF_HOTNESS:
		case ItemPool.PHIAL_OF_COLDNESS:
		case ItemPool.PHIAL_OF_SPOOKINESS:
		case ItemPool.PHIAL_OF_STENCH:
		case ItemPool.PHIAL_OF_SLEAZINESS:
			// If it's an elemental phial, remove other elemental effects first.
			for ( int i = 0; i < BasementRequest.ELEMENT_PHIALS.length; ++i )
			{
				AdventureResult phial = BasementRequest.ELEMENT_PHIALS[ i ];
				if ( itemId != phial.getItemId() )
				{
					continue;
				}

				// i is the index of the phial we are using
				for ( int j = 0; j < BasementRequest.ELEMENT_FORMS.length; ++j )
				{
					if ( j == i )
					{
						continue;
					}

					AdventureResult form = BasementRequest.ELEMENT_FORMS[ j ];
					if ( !KoLConstants.activeEffects.contains( form ) )
					{
						continue;
					}

					RequestThread.postRequest( new UneffectRequest( form ) );

					if ( !KoLmafia.permitsContinue() )
					{
						return;
					}

					break;
				}

				break;
			}
			break;
		}

		if ( this.consumptionType != KoLConstants.INFINITE_USES &&
		     !UseItemRequest.sequentialConsume( itemId ) &&
		     !InventoryManager.retrieveItem( this.itemUsed ) )
		{
			return;
		}

		int iterations = 1;
		int origCount = this.itemUsed.getCount();

		if ( origCount != 1 )
		{
			if ( itemId == ItemPool.YUMMY_TUMMY_BEAN )
			{	// If not divisible by 20, make the first iteration short
				iterations = (origCount + 19) / 20;
				this.itemUsed = this.itemUsed.getInstance( (origCount + 19) % 20 + 1 );
			}
			else if ( itemId == ItemPool.PACK_OF_POGS )
			{	// If not divisible by 11, make the first iteration short
				iterations = (origCount + 10) / 11;
				this.itemUsed = this.itemUsed.getInstance( (origCount + 10) % 11 + 1 );
			}
			else switch ( this.consumptionType )
			{
			case  KoLConstants.INFINITE_USES:
			{
				int type = ItemDatabase.getConsumptionType( this.itemUsed.getItemId() );
				if ( type != KoLConstants.CONSUME_MULTIPLE )
				{
					iterations = origCount;
					this.itemUsed = this.itemUsed.getInstance( 1 );
				}
				break;
			}
			case KoLConstants.CONSUME_MULTIPLE:
			case KoLConstants.HP_RESTORE:
			case KoLConstants.MP_RESTORE:
			case KoLConstants.HPMP_RESTORE:
			case KoLConstants.CONSUME_HOBO:
			case KoLConstants.CONSUME_GHOST:
			case KoLConstants.CONSUME_SLIME:
				break;
			default:
				iterations = origCount;
				this.itemUsed = this.itemUsed.getInstance( 1 );
			}
		}

		if ( itemId == ItemPool.MAFIA_ARIA )
		{
			SpecialOutfit.createImplicitCheckpoint();
			AdventureResult cummerbund = ItemPool.get( ItemPool.CUMMERBUND, 1 );
			if ( !KoLCharacter.hasEquipped( cummerbund ) )
			{
				RequestThread.postRequest( new EquipmentRequest( cummerbund ) );
			}
		}

		String originalURLString = this.getURLString();

		for ( int i = 1; i <= iterations && KoLmafia.permitsContinue(); ++i )
		{
			this.constructURLString( originalURLString );
			this.useOnce( i, iterations, "Using" );

			if ( itemId == ItemPool.YUMMY_TUMMY_BEAN )
			{	// the first iteration may have been short
				this.itemUsed = this.itemUsed.getInstance( 20 );
			}
			else if ( itemId == ItemPool.PACK_OF_POGS )
			{	// the first iteration may have been short
				this.itemUsed = this.itemUsed.getInstance( 11 );
			}

			if ( ( isSealFigurine || isBRICKOMonster ) && KoLmafia.permitsContinue() )
			{
				this.addFormField( "checked", "1" );
				super.run();
			}
		}

		if ( itemId == ItemPool.MAFIA_ARIA )
		{
			SpecialOutfit.restoreImplicitCheckpoint();
		}

		if ( KoLmafia.permitsContinue() )
		{
			KoLmafia.updateDisplay( "Finished using " + origCount + " " + this.itemUsed.getName() + "." );
		}
	}

	private static final boolean sequentialConsume( final int itemId )
	{
		return false;
	}

	public static final boolean confirmReplacement( final String name )
	{
		if ( !GenericFrame.instanceExists() )
		{
			return true;
		}

		if ( !InputFieldUtilities.confirm( "Are you sure you want to replace your " + name + "?" ) )
		{
			return false;
		}

		return true;
	}

	@Override
	protected boolean shouldFollowRedirect()
	{
		return true;
	}

	public void useOnce( final int currentIteration, final int totalIterations, String useTypeAsString )
	{
		UseItemRequest.lastUpdate = "";

		if ( this.consumptionType == KoLConstants.CONSUME_ZAP )
		{
			ZapCommand.zap( this.getItemUsed().getName() );
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

		if ( this.getAdventuresUsed() > 0 )
		{
			// If we are about to use an item which can use adventures, set location to "None"
			// for the benefit of betweenBattleScripts
			Preferences.setString( "nextAdventure", "None" );
			RecoveryManager.runBetweenBattleChecks( true );
		}

		switch ( this.consumptionType )
		{
		case KoLConstants.CONSUME_HOBO:
			if ( KoLCharacter.getFamiliar().getId() != FamiliarPool.HOBO )
			{
				KoLmafia.updateDisplay( MafiaState.ERROR, "You don't have a Spirit Hobo equipped" );
				return;
			}
			EatItemRequest.clearFoodHelper();
			this.addFormField( "action", "binge" );
			this.addFormField( "qty", String.valueOf( this.itemUsed.getCount() ) );
			useTypeAsString = "Boozing hobo with";
			break;

		case KoLConstants.CONSUME_GHOST:
			if ( KoLCharacter.getFamiliar().getId() != FamiliarPool.GHOST )
			{
				KoLmafia.updateDisplay( MafiaState.ERROR, "You don't have a Gluttonous Green Ghost equipped" );
				return;
			}
			DrinkItemRequest.clearDrinkHelper();
			this.addFormField( "action", "binge" );
			this.addFormField( "qty", String.valueOf( this.itemUsed.getCount() ) );
			useTypeAsString = "Feeding ghost with";
			break;

		case KoLConstants.CONSUME_SLIME:
			if ( KoLCharacter.getFamiliar().getId() != FamiliarPool.SLIMELING )
			{
				KoLmafia.updateDisplay( MafiaState.ERROR, "You don't have a Slimeling equipped" );
				return;
			}
			this.addFormField( "action", "binge" );
			this.addFormField( "qty", String.valueOf( this.itemUsed.getCount() ) );
			useTypeAsString = "Feeding slimeling with";
			break;

		case KoLConstants.CONSUME_MIMIC:
			if ( KoLCharacter.getFamiliar().getId() != FamiliarPool.STOCKING_MIMIC )
			{
				KoLmafia.updateDisplay( MafiaState.ERROR, "You don't have a Stocking Mimic equipped" );
				return;
			}
			this.addFormField( "action", "candy" );
			useTypeAsString = "Feeding stocking mimic with";
			break;

		case KoLConstants.CONSUME_MULTIPLE:
		case KoLConstants.HP_RESTORE:
		case KoLConstants.MP_RESTORE:
		case KoLConstants.HPMP_RESTORE:
			if ( this.itemUsed.getCount() > 1 )
			{
				this.addFormField( "action", "useitem" );
				this.addFormField( "quantity", String.valueOf( this.itemUsed.getCount() ) );
			}
			// Fall through
		default:
			this.addFormField( "ajax", "1" );
			if ( UseItemRequest.needsConfirmation( this.itemUsed ) )
			{
				this.addFormField( "confirm", "true" );
			}
			break;
		}

		this.runOneIteration( currentIteration, totalIterations, useTypeAsString );
	}

	protected void runOneIteration( final int currentIteration, final int totalIterations, String useTypeAsString )
	{
		StringBuilder message = new StringBuilder();

		message.append( useTypeAsString );
		message.append( " " );
		if ( totalIterations == 1 )
		{
			message.append( String.valueOf( this.itemUsed.getCount() ) );
			message.append( " " );
		}
		message.append( this.itemUsed.getName() );
		if ( totalIterations != 1 )
		{
			message.append( " (" );
			message.append( String.valueOf( currentIteration ) );
			message.append( " of " );
			message.append( String.valueOf( totalIterations ) );
			message.append( ")" );
		}
		message.append( "..." );

		KoLmafia.updateDisplay( message.toString() );

		super.run();

		if ( this.getPath().startsWith( "choice.php" ) )
		{
			// A UseItemRequest counts as automation. If this
			// choice option is not complete, let ChoiceManager
			// automate it.
			ChoiceManager.processChoiceAdventure( this.responseText );
		}
	}

	public static String elementalHelper( String remove, Element resist, int amount )
	{
		AdventureResult effect = new AdventureResult( remove, 1, true );
		if ( KoLConstants.activeEffects.contains( effect ) )
		{
			RequestThread.postRequest( new UneffectRequest( effect ) );
		}
		if ( KoLConstants.activeEffects.contains( effect ) )
		{
			return "Unable to remove " + remove + ", which makes this helper unusable.";
		}

		int healthNeeded = (int) Math.ceil(amount * (100.0f - KoLCharacter.getElementalResistance( resist )) / 100.0f);
		if ( KoLCharacter.getCurrentHP() <= healthNeeded )
		{
			RecoveryManager.recoverHP( healthNeeded + 1 );
		}
		if ( KoLCharacter.getCurrentHP() <= healthNeeded )
		{
			return "Unable to gain enough HP to survive the use of this helper.";
		}

		return "";
	}

	@Override
	public void processResults()
	{
		switch ( this.consumptionType )
		{
		case KoLConstants.CONSUME_GHOST:
		case KoLConstants.CONSUME_HOBO:
		case KoLConstants.CONSUME_SLIME:
		case KoLConstants.CONSUME_MIMIC:
			if ( !UseItemRequest.parseBinge( this.getURLString(), this.responseText ) )
			{
				KoLmafia.updateDisplay( MafiaState.ERROR, "Your current familiar can't use that." );
			}
			return;
		}

		UseItemRequest.lastItemUsed = this.itemUsed;
		UseItemRequest.currentItemId = this.itemUsed.getItemId();
		UseItemRequest.parseConsumption( this.responseText, true );
		ResponseTextParser.learnRecipe( this.getURLString(), this.responseText );
	}

	public static final void parseBricko( final String responseText )
	{
		if ( responseText.contains( "You break apart your" ) )
		{
			Matcher matcher = UseItemRequest.BRICKO_PATTERN.matcher( responseText );
			if ( matcher.find() )
			{
				AdventureResult brickoItem = new AdventureResult( matcher.group( 1 ), -1 );
				ResultProcessor.processResult( brickoItem );
			}
		}
	}

	public static final boolean parseBinge( final String urlString, final String responseText )
	{
		AdventureResult item = UseItemRequest.extractBingedItem( urlString );
		if ( item == null )
		{
			return true;
		}

		// Looks like you don't currently have a familiar capable of
		// binging.
		//
		// You're not currently using a Gluttonous Green Ghost.

		if ( responseText.indexOf( "don't currently have" ) != -1 ||
		     responseText.indexOf( "not currently using" ) != -1 )
		{
			return false;
		}

		// You don't have that many of those.

		if ( responseText.indexOf( "don't have that many" ) != -1 )
		{
			return true;
		}

		// [familiar name] approaches the [item] but doesn't seem interested.
		if ( responseText.indexOf( "doesn't seem interested" ) != -1 )
		{
			return true;
		}

		// That is not something you can give to your Slimeling
		if ( responseText.indexOf( "not something you can give" ) != -1 )
		{
			return true;
		}

		// <name> takes the <candy> and quickly consumes them. He
		// grows a bit.
		if ( responseText.indexOf( "He grows a bit" ) != -1 )
		{
			KoLCharacter.getFamiliar().addNonCombatExperience( item.getCount() );
		}

		FamiliarData familiar = KoLCharacter.getFamiliar();
		int id = familiar.getId();

		// Estimate Slimeling charges
		if ( id == FamiliarPool.SLIMELING )
		{
			int count = item.getCount();
			int itemId = item.getItemId();
			String name = item.getName();

			if ( itemId == ItemPool.GNOLLISH_AUTOPLUNGER ||
			     ConcoctionDatabase.meatStackCreation( name ) != null )
			{
				Preferences.increment( "slimelingStacksDue", count );
			}
			else
			{
				// round down for now, since we don't know how this really works
				float charges = count * EquipmentDatabase.getPower( itemId ) / 10.0F;
				Preferences.setFloat( "slimelingFullness", Preferences.getFloat( "slimelingFullness" ) + charges );
			}
		}

		ResultProcessor.processResult( item.getNegation() );

		return true;
	}

	public void parseConsumption()
	{
		UseItemRequest.parseConsumption( "", true );
	}

	public static final void parseConsumption( final String responseText, final boolean showHTML )
	{
		if ( UseItemRequest.lastItemUsed == null )
		{
			return;
		}

		UseItemRequest.lastUpdate = "";

		AdventureResult item = UseItemRequest.lastItemUsed;
		AdventureResult helper = UseItemRequest.lastHelperUsed;

		UseItemRequest.lastItemUsed = null;
		UseItemRequest.lastHelperUsed = null;

		if ( item.getItemId() == ItemPool.CARD_SLEEVE )
		{
			EquipmentRequest.parseCardSleeve( responseText );
			return;
		}

		// If you are in Beecore, certain items can't B used
		// "You are too scared of Bs to xxx that item."
		if ( KoLCharacter.inBeecore() &&
		     responseText.indexOf( "You are too scared of Bs" ) != -1 )
		{
			UseItemRequest.lastUpdate = "You are too scared of Bs";
			KoLmafia.updateDisplay( MafiaState.ERROR, UseItemRequest.lastUpdate );
			return;
		}

		if ( responseText.indexOf( "be at least level" ) != -1 )
		{
			UseItemRequest.lastUpdate = "Item level too high.";
			KoLmafia.updateDisplay( MafiaState.ERROR, UseItemRequest.lastUpdate );
			return;
		}

		if ( responseText.indexOf( "You may not" ) != -1 )
		{
			UseItemRequest.lastUpdate = "Pathed ascension.";
			KoLmafia.updateDisplay( MafiaState.ERROR, UseItemRequest.lastUpdate );
			return;
		}

		int consumptionType = UseItemRequest.getConsumptionType( item );
		switch ( consumptionType )
		{
		case KoLConstants.CONSUME_DRINK:
		case KoLConstants.CONSUME_DRINK_HELPER:
			DrinkItemRequest.parseConsumption( item, helper, responseText );
			return;

		case KoLConstants.CONSUME_EAT:
		case KoLConstants.CONSUME_FOOD_HELPER:
			EatItemRequest.parseConsumption( item, helper, responseText );
			return;
		}

		int spleenHit = ItemDatabase.getSpleenHit( item.getName() );
		if ( spleenHit > 0 )
		{
			SpleenItemRequest.parseConsumption( item, helper, responseText );
			return;
		}

		// Check for consumption helpers, which will need to be removed
		// from inventory if they were successfully used.

		if ( helper != null )
		{
			// Check for success message, since there are multiple
			// ways these could fail:

			boolean success = true;

			switch ( helper.getItemId() )
			{
			case ItemPool.PUNCHCARD_ATTACK:
			case ItemPool.PUNCHCARD_REPAIR:
			case ItemPool.PUNCHCARD_BUFF:
			case ItemPool.PUNCHCARD_MODIFY:
			case ItemPool.PUNCHCARD_BUILD:
			case ItemPool.PUNCHCARD_TARGET:
			case ItemPool.PUNCHCARD_SELF:
			case ItemPool.PUNCHCARD_FLOOR:
			case ItemPool.PUNCHCARD_DRONE:
			case ItemPool.PUNCHCARD_WALL:
			case ItemPool.PUNCHCARD_SPHERE:

				// A voice speaks (for a long time) from the
				// helmet:

				if ( responseText.indexOf( "A tinny voice emerges from the drone" ) != -1 )
				{
					// produces sparking, etc. drones - no special handling needed
				}
				else if ( responseText.indexOf( "(for a long time)" ) == -1 )
				{
					success = false;
				}
				else
				{
					UseItemRequest.parseEVHelmet( responseText );
				}
				break;
			}

			if ( !success )
			{
				UseItemRequest.lastUpdate = "Consumption helper failed.";
				KoLmafia.updateDisplay( MafiaState.ERROR, UseItemRequest.lastUpdate );
				return;
			}

			// Remove the consumption helper from inventory.
			ResultProcessor.processResult( helper.getNegation() );
		}

		if ( ConcoctionDatabase.singleUseCreation( item.getName() ) != null ||
		     ( ConcoctionDatabase.multiUseCreation( item.getName() ) != null && item.getCount() > 1 ) )
		{
			// These all create things via "use" or "multiuse" of
			// an ingredient and perhaps consume other ingredients.
			// SingleUseRequest or MultiUseRequest removed all the
			// ingredients.

			if ( responseText.contains( "You acquire" ) )
			{
				// If the user navigates to the equipment page,
				// we will be called again with inventory page
				// and will generate an error below.
				UseItemRequest.lastItemUsed = null;
				return;
			}

			int count = item.getCount();
			String name = item.getName();
			String plural = ItemDatabase.getPluralName( item.getItemId() );

			if ( responseText.indexOf( "You don't have that many" ) != -1 )
			{
				UseItemRequest.lastUpdate = "You don't have that many " + plural;
			}
			else
			{
				UseItemRequest.lastUpdate = "Using " + count + " " + ( count == 1 ? name : plural ) + " doesn't make anything interesting.";
			}

			KoLmafia.updateDisplay( MafiaState.ERROR, UseItemRequest.lastUpdate );
			return;
		}

		// Check for familiar growth - if a familiar is added,
		// make sure to update the StaticEntity.getClient().

		if ( consumptionType == KoLConstants.GROW_FAMILIAR )
		{
			if ( responseText.indexOf( "You've already got a familiar of that type." ) != -1 )
			{
				UseItemRequest.lastUpdate = "You already have that familiar.";
				KoLmafia.updateDisplay( MafiaState.ERROR, UseItemRequest.lastUpdate );
				return;
			}

			if ( responseText.indexOf( "you glance fearfully at the moons" ) != -1 )
			{
				UseItemRequest.lastUpdate = "Can't hatch that familiar in Bad Moon.";
				KoLmafia.updateDisplay( MafiaState.ERROR, UseItemRequest.lastUpdate );
				return;
			}

			if ( responseText.indexOf( "You don't have a Terrarium to put that in." ) != -1 )
			{
				UseItemRequest.lastUpdate = "You don't have a Terrarium yet.";
				KoLmafia.updateDisplay( MafiaState.ERROR, UseItemRequest.lastUpdate );
				return;
			}

			if ( responseText.indexOf( "Boris has no need for familiars" ) != -1 )
			{
				UseItemRequest.lastUpdate = "Boris has no need for familiars!";
				KoLmafia.updateDisplay( MafiaState.ERROR, UseItemRequest.lastUpdate );
				return;
			}

			// Remove the familiar larva from inventory.
			ResultProcessor.processResult( item.getNegation() );

			FamiliarData familiar = KoLCharacter.addFamiliar( FamiliarDatabase.growFamiliarLarva( item.getItemId() ) );
			// If this is a previously unknown familiar, punt
			if ( familiar == null )
			{
				return;
			}

			Matcher matcher = UseItemRequest.FAMILIAR_NAME_PATTERN.matcher( responseText );
			if ( matcher.find() )
			{
				familiar.setName( matcher.group(1) );
			}

			// Don't bother showing the result
			// UseItemRequest.showItemUsage( showHTML, responseText );
			return;
		}

		if ( responseText.indexOf( "That item isn't usable in quantity" ) != -1 )
		{
			int attrs = ItemDatabase.getAttributes( item.getItemId() );
			if ( ( attrs & ItemDatabase.ATTR_MULTIPLE ) == 0 )
			{
				// Multi-use was attempted and failed, but the request was not generated by KoLmafia
				// because KoLmafia already knows that it cannot be multi-used
				return;
			}
			KoLmafia.updateDisplay( MafiaState.ERROR, "Internal data error: item incorrectly flagged as multi-usable." );
			return;
		}

		// Note that there is at least one item (memory of amino acids)
		// that can fail with a "too full" message, even though it's
		// not a food.

		if ( responseText.indexOf( "too full" ) != -1 )
		{
			UseItemRequest.lastUpdate = "Consumption limit reached.";
			KoLmafia.updateDisplay( MafiaState.ERROR, UseItemRequest.lastUpdate );
			return;
		}

		// Assume initially that this causes the item to disappear.
		// In the event that the item is not used, then proceed to
		// undo the consumption.

		switch ( consumptionType )
		{
		case KoLConstants.CONSUME_FOOD_HELPER:
		case KoLConstants.CONSUME_DRINK_HELPER:
			// Consumption helpers are removed above when you
			// successfully eat or drink.
		case KoLConstants.NO_CONSUME:
			return;

		case KoLConstants.MESSAGE_DISPLAY:
			UseItemRequest.showItemUsage( showHTML, responseText );
			return;

		case KoLConstants.CONSUME_ZAP:
		case KoLConstants.EQUIP_FAMILIAR:
		case KoLConstants.EQUIP_ACCESSORY:
		case KoLConstants.EQUIP_HAT:
		case KoLConstants.EQUIP_PANTS:
		case KoLConstants.EQUIP_WEAPON:
		case KoLConstants.EQUIP_OFFHAND:
		case KoLConstants.EQUIP_CONTAINER:
		case KoLConstants.INFINITE_USES:
			break;

		default:
			ResultProcessor.processResult( item.getNegation() );
		}

		Matcher matcher;

		// Perform item-specific processing

		int itemId = item.getItemId();
		switch ( itemId )
		{
		case ItemPool.LOATHING_LEGION_UNIVERSAL_SCREWDRIVER:
		{
			// You jam your screwdriver into your xxx and pry it
			// apart.
			if ( UseItemRequest.lastUntinker != null &&
			     responseText.indexOf( "You jam your screwdriver" ) != -1 )
			{
				ResultProcessor.processResult( UseItemRequest.lastUntinker.getNegation() );
				UseItemRequest.lastUntinker = null;
				break;
			}
		}
			// Fall through
		case ItemPool.LOATHING_LEGION_KNIFE:
		case ItemPool.LOATHING_LEGION_MANY_PURPOSE_HOOK:
		case ItemPool.LOATHING_LEGION_MOONDIAL:
		case ItemPool.LOATHING_LEGION_NECKTIE:
		case ItemPool.LOATHING_LEGION_ELECTRIC_KNIFE:
		case ItemPool.LOATHING_LEGION_CORKSCREW:
		case ItemPool.LOATHING_LEGION_CAN_OPENER:
		case ItemPool.LOATHING_LEGION_CHAINSAW:
		case ItemPool.LOATHING_LEGION_ROLLERBLADES:
		case ItemPool.LOATHING_LEGION_FLAMETHROWER:
		case ItemPool.LOATHING_LEGION_TATTOO_NEEDLE:
		case ItemPool.LOATHING_LEGION_DEFIBRILLATOR:
		case ItemPool.LOATHING_LEGION_DOUBLE_PRISM:
		case ItemPool.LOATHING_LEGION_TAPE_MEASURE:
		case ItemPool.LOATHING_LEGION_KITCHEN_SINK:
		case ItemPool.LOATHING_LEGION_ABACUS:
		case ItemPool.LOATHING_LEGION_HELICOPTER:
		case ItemPool.LOATHING_LEGION_PIZZA_STONE:
		case ItemPool.LOATHING_LEGION_JACKHAMMER:
		case ItemPool.LOATHING_LEGION_HAMMER:
			// You spend a little while messing with all of the
			// latches and clasps and little bits of metal, and end
			// up with ...
			if ( responseText.indexOf( "latches and clasps" ) != -1 )
			{
				ResultProcessor.processResult( item.getNegation() );
			}
			break;

		case ItemPool.WHAT_CARD:
		case ItemPool.WHEN_CARD:
		case ItemPool.WHO_CARD:
		case ItemPool.WHERE_CARD: {
			if ( responseText.indexOf( "Answer:" ) == -1 )
			{
				ResultProcessor.processResult( item );
				break;
			}
			Matcher card_matcher = UseItemRequest.CARD_PATTERN.matcher( responseText );
			if ( card_matcher.find() )
			{
				String message = "Trivia card #" + card_matcher.group( 1 ) + "/" + card_matcher.group( 2 ) + ":";
				RequestLogger.printLine( message );
				RequestLogger.updateSessionLog( message );
			}
			Matcher QA_matcher = UseItemRequest.QA_PATTERN.matcher( responseText );
			while ( QA_matcher.find() )
			{
				String message = QA_matcher.group( 1 ) + ": " + QA_matcher.group( 2 );
				RequestLogger.printLine( message );
				RequestLogger.updateSessionLog( message );
			}
			break;
		}

		case ItemPool.LEGENDARY_BEAT:
			Preferences.setBoolean( "_legendaryBeat", true );
			return;

		case ItemPool.JACKING_MAP:
			// The <fruit> disappears into the tube and begins
			// bouncing around noisily inside the machine.

			// The <fruit> is sucked into the tube, displacing the
			// <fruit> that was bouncing around in the machine. You
			// wonder where it went.
			if ( UseItemRequest.lastFruit != null &&
			     responseText.indexOf( "into the tube" ) != -1 )
			{
				ResultProcessor.processResult(
					UseItemRequest.lastFruit.getNegation() );
				UseItemRequest.lastFruit = null;
			}
			return;

		// If it's a gift package, get the inner message

		case ItemPool.GIFT1:
		case ItemPool.GIFT2:
		case ItemPool.GIFT3:
		case ItemPool.GIFT4:
		case ItemPool.GIFT5:
		case ItemPool.GIFT6:
		case ItemPool.GIFT7:
		case ItemPool.GIFT8:
		case ItemPool.GIFT9:
		case ItemPool.GIFT10:
		case ItemPool.GIFT11:
		case ItemPool.GIFTV:
		case ItemPool.GIFTR:
		case ItemPool.GIFTW:
		case ItemPool.GIFTH:

			// "You can't receive things from other players
			// right now."

			if ( responseText.indexOf( "You can't receive things" ) != -1 )
			{
				UseItemRequest.lastUpdate = "You can't open that package yet.";
				KoLmafia.updateDisplay( MafiaState.ERROR, UseItemRequest.lastUpdate );
				ResultProcessor.processResult( item );
			}
			else if ( showHTML )
			{
				UseItemRequest.showItemUsage( true, responseText );
			}

			return;

		case ItemPool.DANCE_CARD:
			TurnCounter.stopCounting( "Dance Card" );
			TurnCounter.startCounting( 3, "Dance Card loc=395", "guildapp.gif" );
			return;

		case ItemPool.TOASTER:

			// You push the lever and are rewarded with toast
			if ( responseText.indexOf( "rewarded with toast" ) != -1 )
			{
				Preferences.increment( "_toastSummons", 1 );
			}
			else
			{
				Preferences.setInteger( "_toastSummons", 3 );
			}
			return;

		case ItemPool.GATES_SCROLL:

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
				ResultProcessor.processResult( ItemPool.get( ItemPool.DICTIONARY, -1 ) );
				QuestDatabase.setQuestProgress( Quest.TOPPING, QuestDatabase.FINISHED );
				QuestDatabase.setQuestProgress( Quest.LOL, QuestDatabase.FINISHED );
			}

			return;

		case ItemPool.ELITE_SCROLL:

			// "The UB3r 31337 HaX0R stands before you."

			if ( responseText.indexOf( "The UB3r 31337 HaX0R stands before you." ) != -1 )
			{
				int remaining = item.getCount() - 1;
				if ( remaining > 0 )
				{
					item = item.getInstance( remaining );
					ResultProcessor.processResult( item );
					(UseItemRequest.getInstance( item )).run();
				}
			}

			return;

		case ItemPool.HERMIT_SCRIPT:

			HermitRequest.hackHermit();
			return;

		case ItemPool.SPARKLER:
		case ItemPool.SNAKE:
		case ItemPool.M282:

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

		case ItemPool.GONG:

			// We deduct the gong when we get the intro choice
			// adventure: The Gong Has Been Bung

			ResultProcessor.processResult( item );

			// "You try to bang the gong, but the mallet keeps
			// falling out of your hand. Maybe you should try it
			// later, when you've sobered up a little."

			// "You don't have time to bang a gong. Nor do you have
			// time to get it on, or to get it on."
			if ( responseText.indexOf( "sobered up a little" ) != -1  ||
			     responseText.indexOf( "don't have time to bang" ) != -1 )
			{
				UseItemRequest.lastUpdate = "Insufficient adventures or sobriety to use a gong.";
				KoLmafia.updateDisplay( MafiaState.ERROR, UseItemRequest.lastUpdate );
				return;
			}

			// "You're already in the middle of a journey of
			// reincarnation."

			if ( responseText.indexOf( "middle of a journey of reincarnation" ) != -1 )
			{
				if ( UseItemRequest.retrying ||
					KoLConstants.activeEffects.contains(
						EffectPool.get( Effect.FORM_OF_BIRD ) ) ||
					KoLConstants.activeEffects.contains(
						EffectPool.get( Effect.SHAPE_OF_MOLE ) ) ||
					KoLConstants.activeEffects.contains(
						EffectPool.get( Effect.FORM_OF_ROACH ) ) )
				{
					UseItemRequest.lastUpdate = "You're still under a gong effect.";
					KoLmafia.updateDisplay( MafiaState.ERROR, UseItemRequest.lastUpdate );
					return;	// can't use another gong yet
				}

				try
				{
					UseItemRequest.retrying = true;	// prevent recursing more than once
					int adv = Preferences.getInteger( "welcomeBackAdv" );
					if ( adv <= 0 )
					{
						adv = AdventurePool.NOOB_CAVE;
					}
					KoLAdventure req = AdventureDatabase.getAdventureByURL( "adventure.php?snarfblat=" + adv );
					// Must do some trickery here to
					// prevent the adventure location from
					// being changed, and the conditions
					// reset.
					String next = Preferences.getString( "nextAdventure" );
					KoLAdventure.setNextAdventure( req );
					req.overrideAdventuresUsed( 0 );	// don't trigger counters
					RequestThread.postRequest( req );
					req.overrideAdventuresUsed( -1 );
					KoLAdventure.setNextAdventure( next );
					(UseItemRequest.getInstance( item )).run();
				}
				finally
				{
					UseItemRequest.retrying = false;
				}
			}

			return;

		case ItemPool.ENCHANTED_BEAN:

			// There are three possibilities:

			// If you haven't been give the quest, "you can't find
			// anywhere that looks like a good place to plant the
			// bean" and you're told to "wait until later"

			// If you've already planted one, "There's already a
			// beanstalk in the Nearby Plains." In either case, the
			// bean is not consumed.

			// Otherwise, "it immediately grows into an enormous
			// beanstalk".

			if ( responseText.contains( "grows into an enormous beanstalk" ) )
			{
				QuestLogRequest.setBeanstalkPlanted();
			}
			else
			{
				ResultProcessor.processResult( item );
			}

			return;

		case ItemPool.LIBRARY_CARD:

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
			Preferences.setBoolean( "libraryCardUsed", true );
			return;

		case ItemPool.HEY_DEZE_MAP:

			// "Your song has pleased me greatly. I will reward you
			// with some of my crazy imps, to do your bidding."

			if ( responseText.indexOf( "pleased me greatly" ) == -1 )
			{
				UseItemRequest.lastUpdate = "Your music was inadequate.";
				KoLmafia.updateDisplay( MafiaState.ERROR, UseItemRequest.lastUpdate );
				ResultProcessor.processResult( item );
			}

			return;

		case ItemPool.GIANT_CASTLE_MAP:

			// "I'm sorry, adventurer, but the Sorceress is in
			// another castle!"

			if ( responseText.indexOf( "Sorceress is in another castle" ) == -1 )
			{
				UseItemRequest.lastUpdate = "You couldn't make it all the way to the back door.";
				KoLmafia.updateDisplay( MafiaState.ERROR, UseItemRequest.lastUpdate );
				ResultProcessor.processResult( item );
			}

			return;

		case ItemPool.DRASTIC_HEALING:

			// If a scroll of drastic healing was used and didn't
			// crumble, it is not consumed

			ResultProcessor.processResult(
				new AdventureResult( AdventureResult.HP, KoLCharacter.getMaximumHP() ) );

			if ( responseText.indexOf( "crumble" ) == -1 )
			{
				ResultProcessor.processResult( item );
			}

			return;

		case ItemPool.TEARS:

			KoLConstants.activeEffects.remove( KoLAdventure.BEATEN_UP );
			return;

		case ItemPool.ANTIDOTE:
			// You're unpoisoned -- don't waste the anti-anti-antidote.

			if ( responseText.indexOf( "don't waste the anti" ) != -1 )
			{
				ResultProcessor.processResult( item );
				return;
			}

			return;

		case ItemPool.TBONE_KEY:

			if ( InventoryManager.hasItem( ItemPool.LOCKED_LOCKER ) )
			{
				ResultProcessor.processItem( ItemPool.LOCKED_LOCKER, -1 );
			}
			else
			{
				ResultProcessor.processResult( item );
			}
			return;

		case ItemPool.KETCHUP_HOUND:

			// Successfully using a ketchup hound uses up the Hey
			// Deze nuts and pagoda plan.

			if ( responseText.indexOf( "pagoda" ) != -1 )
			{
				ResultProcessor.processItem( ItemPool.HEY_DEZE_NUTS, -1 );
				ResultProcessor.processItem( ItemPool.PAGODA_PLANS, -1 );
				CampgroundRequest.setCampgroundItem( ItemPool.PAGODA_PLANS, 1 );
			}

			// The ketchup hound does not go away...

			ResultProcessor.processResult( item );
			return;

		case ItemPool.DOLPHIN_KING_MAP:

			// "You follow the Dolphin King's map to the bottom of
			// the sea, and find his glorious treasure."

			if ( responseText.indexOf( "find his glorious treasure" ) == -1 )
			{
				UseItemRequest.lastUpdate = "You don't have everything you need.";
				KoLmafia.updateDisplay( MafiaState.ERROR, UseItemRequest.lastUpdate );
				ResultProcessor.processResult( item );
			}

			return;

		case ItemPool.SLUG_LORD_MAP:

			// "You make your way to the deepest part of the tank,
			// and find a chest engraved with the initials S. L."

			if ( responseText.indexOf( "deepest part of the tank" ) == -1 )
			{
				UseItemRequest.lastUpdate = "You don't have everything you need.";
				KoLmafia.updateDisplay( MafiaState.ERROR, UseItemRequest.lastUpdate );
				ResultProcessor.processResult( item );
			}

			return;

		case ItemPool.DR_HOBO_MAP:

			// "You place it atop the Altar, and grab the Scalpel
			// at the exact same moment."

			if ( responseText.indexOf( "exact same moment" ) == -1 )
			{
				UseItemRequest.lastUpdate = "You don't have everything you need.";
				KoLmafia.updateDisplay( MafiaState.ERROR, UseItemRequest.lastUpdate );
				ResultProcessor.processResult( item );
				return;
			}

			// Using the map consumes an asparagus knife

			ResultProcessor.processItem( ItemPool.ASPARAGUS_KNIFE, -1 );
			return;

		case ItemPool.SHOPPING_LIST:

			// "Since you've already built a bitchin' meatcar, you
			// wad the shopping list up and throw it away."

			if ( responseText.indexOf( "throw it away" ) == -1 )
			{
				ResultProcessor.processResult( item );
			}

			UseItemRequest.showItemUsage( showHTML, responseText );

			return;

		case ItemPool.COBBS_KNOB_MAP:

			// "You memorize the location of the door, then eat
			// both the map and the encryption key."

			if ( responseText.indexOf( "memorize the location" ) == -1 )
			{
				UseItemRequest.lastUpdate = "You don't have everything you need.";
				KoLmafia.updateDisplay( MafiaState.ERROR, UseItemRequest.lastUpdate );
				ResultProcessor.processResult( item );
				return;
			}

			// Using the map consumes the encryption key

			ResultProcessor.processItem( ItemPool.ENCRYPTION_KEY, -1 );
			return;

		case ItemPool.SPOOKY_MAP:

			if ( InventoryManager.hasItem( ItemPool.SPOOKY_SAPLING ) && InventoryManager.hasItem( ItemPool.SPOOKY_FERTILIZER ) )
			{
				ResultProcessor.processItem( ItemPool.SPOOKY_SAPLING, -1 );
				ResultProcessor.processItem( ItemPool.SPOOKY_FERTILIZER, -1 );
				Preferences.setInteger( "lastTempleUnlock", KoLCharacter.getAscensions() );
			}
			else
			{
				UseItemRequest.lastUpdate = "You don't have everything you need.";
				KoLmafia.updateDisplay( MafiaState.ERROR, UseItemRequest.lastUpdate );
				ResultProcessor.processResult( item );
			}

			return;

		case ItemPool.CARONCH_MAP:

			// The item is only consumed once you turn in
			// the nasty booty. That's handled elsewhere.

			ResultProcessor.processResult( item );
			return;

		case ItemPool.FRATHOUSE_BLUEPRINTS:

			// The item is only consumed once you turn in the
			// dentures. That's handled elsewhere.

			ResultProcessor.processResult( item );
			return;

		case ItemPool.DINGHY_PLANS:

			// "You need some planks to build the dinghy."

			if ( InventoryManager.hasItem( ItemPool.DINGY_PLANKS ) )
			{
				ResultProcessor.processItem( ItemPool.DINGY_PLANKS, -1 );
			}
			else
			{
				UseItemRequest.lastUpdate = "You need some dingy planks.";
				KoLmafia.updateDisplay( MafiaState.ERROR, UseItemRequest.lastUpdate );
				ResultProcessor.processResult( item );
			}

			return;

		case ItemPool.FENG_SHUI:

			if ( InventoryManager.hasItem( ItemPool.FOUNTAIN ) && InventoryManager.hasItem( ItemPool.WINDCHIMES ) )
			{
				ResultProcessor.processItem( ItemPool.FOUNTAIN, -1 );
				ResultProcessor.processItem( ItemPool.WINDCHIMES, -1 );
				CampgroundRequest.setCampgroundItem( ItemPool.FENG_SHUI, 1 );
			}
			else
			{
				ResultProcessor.processResult( item );
			}

			return;

		case ItemPool.WARM_SUBJECT:

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
				int remaining = item.getCount() - 1;
				if ( remaining > 0 )
				{
					item = item.getInstance( remaining );
					ResultProcessor.processResult( item );
					(UseItemRequest.getInstance( item )).run();
				}
			}

			return;

		case ItemPool.PURPLE_SNOWCONE:
		case ItemPool.GREEN_SNOWCONE:
		case ItemPool.ORANGE_SNOWCONE:
		case ItemPool.RED_SNOWCONE:
		case ItemPool.BLUE_SNOWCONE:
		case ItemPool.BLACK_SNOWCONE:

			// "Your mouth is still cold from the last snowcone you
			// ate. Try again later."

			if ( responseText.indexOf( "still cold" ) != -1 )
			{
				UseItemRequest.lastUpdate = "Your mouth is too cold.";
				KoLmafia.updateDisplay( MafiaState.ERROR, UseItemRequest.lastUpdate );
				ResultProcessor.processResult( item );
			}

			return;

		case ItemPool.POTION_OF_PUISSANCE:
		case ItemPool.POTION_OF_PERSPICACITY:
		case ItemPool.POTION_OF_PULCHRITUDE:
		case ItemPool.POTION_OF_PERCEPTION:
		case ItemPool.POTION_OF_PROFICIENCY:

			// "You're already under the influence of a
			// high-pressure sauce potion. If you took this one,
			// you'd explode.  And not in a good way."

			if ( responseText.indexOf( "you'd explode" ) != -1 )
			{
				UseItemRequest.lastUpdate = "You're already under pressure.";
				KoLmafia.updateDisplay( MafiaState.ERROR, UseItemRequest.lastUpdate );
				ResultProcessor.processResult( item );
			}

			return;

		case ItemPool.BLUE_CUPCAKE:
		case ItemPool.GREEN_CUPCAKE:
		case ItemPool.ORANGE_CUPCAKE:
		case ItemPool.PURPLE_CUPCAKE:
		case ItemPool.PINK_CUPCAKE:

			// "Your stomach is still a little queasy from
			// digesting a cupcake that may or may not exist in
			// this dimension. You really don't feel like eating
			// another one just now."

			if ( responseText.indexOf( "a little queasy" ) != -1 )
			{
				UseItemRequest.lastUpdate = "Your stomach is too queasy.";
				KoLmafia.updateDisplay( MafiaState.ERROR, UseItemRequest.lastUpdate );
				ResultProcessor.processResult( item );
			}

			return;

		case ItemPool.VAGUE_AMBIGUITY:
		case ItemPool.SMOLDERING_PASSION:
		case ItemPool.ICY_REVENGE:
		case ItemPool.SUGARY_CUTENESS:
		case ItemPool.DISTURBING_OBSESSION:
		case ItemPool.NAUGHTY_INNUENDO:

			// "Your heart can't take another love song so soon
			// after the last one. The conflicting emotions would
			// drive you totally mad."

			if ( responseText.indexOf( "conflicting emotions" ) != -1 )
			{
				UseItemRequest.lastUpdate = "Your heart is already filled with emotions.";
				KoLmafia.updateDisplay( MafiaState.ERROR, UseItemRequest.lastUpdate );
				ResultProcessor.processResult( item );
			}

			return;

		case ItemPool.ROLLING_PIN:

			// Rolling pins remove dough from your inventory.

			ResultProcessor.processItem( ItemPool.DOUGH, 0 - InventoryManager.getCount( ItemPool.DOUGH ) );
			return;

		case ItemPool.UNROLLING_PIN:

			// Unrolling pins remove flat dough from your inventory.
			// They are not consumed by being used

			ResultProcessor.processItem( ItemPool.FLAT_DOUGH, 0 - InventoryManager.getCount( ItemPool.FLAT_DOUGH ) );
			return;

		case ItemPool.EXPRESS_CARD:

			// You feel charged up!

			if ( responseText.indexOf( "charged up" ) != -1 )
			{
				Preferences.setInteger( "_zapCount", 0 );
			}
			return;

		case ItemPool.PLUS_SIGN:

			// "Following The Oracle's advice, you treat the plus
			// sign as a book, and read it."

			if ( responseText.indexOf( "you treat the plus sign as a book" ) == -1 )
			{
				UseItemRequest.lastUpdate = "You don't know how to use it.";
				KoLmafia.updateDisplay( MafiaState.ERROR, UseItemRequest.lastUpdate );
				ResultProcessor.processResult( item );
			}
			else
			{
				QuestLogRequest.setDungeonOfDoomAvailable();

				// Various punctuation mark items are replaced
				// by their identified versions. The new items
				// will be detected by result parsing, but we
				// need to get rid of the old.

				for ( int i = 4552; i <= 4558; ++i )
				{
					AdventureResult punc = ItemPool.get( i, 1 );
					int count = punc.getCount( KoLConstants.inventory );
					if ( count > 0 )
					{
						ResultProcessor.processResult( punc.getInstance( -count ) );
					}
				}
			}

			return;

		case ItemPool.OVEN:
			KoLCharacter.setOven( true );
			return;

		case ItemPool.RANGE:
			KoLCharacter.setRange( true );
			return;

		case ItemPool.CHEF:
		case ItemPool.CLOCKWORK_CHEF:
			KoLCharacter.setChef( true );
			Preferences.setInteger( "chefTurnsUsed", 0 );
			return;

		case ItemPool.SHAKER:
			KoLCharacter.setShaker( true );
			return;

		case ItemPool.COCKTAIL_KIT:
			KoLCharacter.setCocktailKit( true );
			return;

		case ItemPool.BARTENDER:
		case ItemPool.CLOCKWORK_BARTENDER:
			KoLCharacter.setBartender( true );
			Preferences.setInteger( "bartenderTurnsUsed", 0 );
			return;

		case ItemPool.SUSHI_ROLLING_MAT:
			KoLCharacter.setSushiMat( true );
			return;

			// Tomes
		case ItemPool.SNOWCONE_BOOK:
		case ItemPool.STICKER_BOOK:
		case ItemPool.SUGAR_BOOK:
		case ItemPool.CLIP_ART_BOOK:
		case ItemPool.RAD_LIB_BOOK:
		case ItemPool.SMITH_BOOK:
			// Grimoires
		case ItemPool.HILARIOUS_BOOK:
		case ItemPool.TASTEFUL_BOOK:
		case ItemPool.CARD_GAME_BOOK:
		case ItemPool.GEEKY_BOOK:
			// Librams
		case ItemPool.CANDY_BOOK:
		case ItemPool.DIVINE_BOOK:
		case ItemPool.LOVE_BOOK:
		case ItemPool.BRICKO_BOOK:
		case ItemPool.DICE_BOOK:
		case ItemPool.RESOLUTION_BOOK:
			// Others
		case ItemPool.JEWELRY_BOOK:
		case ItemPool.RAINBOWS_GRAVITY:
		case ItemPool.RAGE_GLAND:
		case ItemPool.KISSIN_COUSINS:
		case ItemPool.TALES_FROM_THE_FIRESIDE:
		case ItemPool.BLIZZARDS_I_HAVE_DIED_IN:
		case ItemPool.MAXING_RELAXING:
		case ItemPool.BIDDY_CRACKERS_COOKBOOK:
		case ItemPool.TRAVELS_WITH_JERRY:
		case ItemPool.LET_ME_BE:
		case ItemPool.ASLEEP_IN_THE_CEMETERY:
		case ItemPool.SUMMER_NIGHTS:
		case ItemPool.SENSUAL_MASSAGE_FOR_CREEPS:
		case ItemPool.RICHIE_THINGFINDER:
		case ItemPool.MEDLEY_OF_DIVERSITY:
		case ItemPool.EXPLOSIVE_ETUDE:
		case ItemPool.CHORALE_OF_COMPANIONSHIP:
		case ItemPool.PRELUDE_OF_PRECISION:
		case ItemPool.HODGMAN_JOURNAL_1:
		case ItemPool.HODGMAN_JOURNAL_2:
		case ItemPool.HODGMAN_JOURNAL_3:
		case ItemPool.HODGMAN_JOURNAL_4:
		case ItemPool.CRIMBO_CAROL_V1:
		case ItemPool.CRIMBO_CAROL_V1_USED:
		case ItemPool.CRIMBO_CAROL_V2:
		case ItemPool.CRIMBO_CAROL_V2_USED:
		case ItemPool.CRIMBO_CAROL_V3:
		case ItemPool.CRIMBO_CAROL_V3_USED:
		case ItemPool.CRIMBO_CAROL_V4:
		case ItemPool.CRIMBO_CAROL_V4_USED:
		case ItemPool.CRIMBO_CAROL_V5:
		case ItemPool.CRIMBO_CAROL_V5_USED:
		case ItemPool.CRIMBO_CAROL_V6:
		case ItemPool.CRIMBO_CAROL_V6_USED:
		case ItemPool.CRIMBO_CANDY_COOKBOOK:
		case ItemPool.SLAPFIGHTING_BOOK:
		case ItemPool.SLAPFIGHTING_BOOK_USED:
		case ItemPool.UNCLE_ROMULUS:
		case ItemPool.UNCLE_ROMULUS_USED:
		case ItemPool.SNAKE_CHARMING_BOOK:
		case ItemPool.SNAKE_CHARMING_BOOK_USED:
		case ItemPool.ZU_MANNKASE_DIENEN:
		case ItemPool.ZU_MANNKASE_DIENEN_USED:
		case ItemPool.DYNAMITE_SUPERMAN_JONES:
		case ItemPool.DYNAMITE_SUPERMAN_JONES_USED:
		case ItemPool.INIGO_BOOK:
		case ItemPool.INIGO_BOOK_USED:
		case ItemPool.BLACK_HYMNAL:
		case ItemPool.ELLSBURY_BOOK:
		case ItemPool.ELLSBURY_BOOK_USED:
		case ItemPool.UNEARTHED_METEOROID:
		case ItemPool.KANSAS_TOYMAKER:
		case ItemPool.KANSAS_TOYMAKER_USED:
		case ItemPool.WASSAILING_BOOK:
		case ItemPool.WASSAILING_BOOK_USED:
		case ItemPool.CRIMBCO_MANUAL_1:
		case ItemPool.CRIMBCO_MANUAL_1_USED:
		case ItemPool.CRIMBCO_MANUAL_2:
		case ItemPool.CRIMBCO_MANUAL_2_USED:
		case ItemPool.CRIMBCO_MANUAL_3:
		case ItemPool.CRIMBCO_MANUAL_3_USED:
		case ItemPool.CRIMBCO_MANUAL_4:
		case ItemPool.CRIMBCO_MANUAL_4_USED:
		case ItemPool.CRIMBCO_MANUAL_5:
		case ItemPool.CRIMBCO_MANUAL_5_USED:
		case ItemPool.SKELETON_BOOK:
		case ItemPool.SKELETON_BOOK_USED:
		case ItemPool.NECBRONOMICON:
		case ItemPool.NECBRONOMICON_USED:
		case ItemPool.PLANT_BOOK:
		case ItemPool.GHOST_BOOK:
		case ItemPool.TATTLE_BOOK:
		case ItemPool.NOTE_FROM_CLANCY:
		case ItemPool.GRUDGE_BOOK:
		case ItemPool.JERK_BOOK:
		case ItemPool.HJODOR_GUIDE:
		case ItemPool.INFURIATING_SILENCE_RECORD:
		case ItemPool.INFURIATING_SILENCE_RECORD_USED:
		case ItemPool.TRANQUIL_SILENCE_RECORD:
		case ItemPool.TRANQUIL_SILENCE_RECORD_USED:
		case ItemPool.MENACING_SILENCE_RECORD:
		case ItemPool.MENACING_SILENCE_RECORD_USED:
		case ItemPool.WALBERG_BOOK:
		case ItemPool.OCELOT_BOOK:
		case ItemPool.DRESCHER_BOOK:
		case ItemPool.DECODED_CULT_DOCUMENTS:
		case ItemPool.WARBEAR_METALWORKING_PRIMER:
		case ItemPool.WARBEAR_METALWORKING_PRIMER_USED:
		case ItemPool.WARBEAR_EMPATHY_CHIP:
		case ItemPool.WARBEAR_EMPATHY_CHIP_USED:
		case ItemPool.OFFENSIVE_JOKE_BOOK:
		case ItemPool.COOKING_WITH_GREASE_BOOK:
		case ItemPool.DINER_HANDBOOK:
		case ItemPool.ALIEN_SOURCE_CODE:
		case ItemPool.ALIEN_SOURCE_CODE_USED:
		{
			if ( !responseText.contains( "You acquire a skill" ) )
			{
				UseItemRequest.lastUpdate = "You can't learn that skill.";
				KoLmafia.updateDisplay( MafiaState.ERROR, UseItemRequest.lastUpdate );
				if ( UseItemRequest.getConsumptionType( item ) != KoLConstants.INFINITE_USES )
				{
					ResultProcessor.processResult( item );
				}
				return;
			}

			String skill = UseItemRequest.itemToSkill( itemId );
			if ( skill == null )
			{
				return;
			}

			ResponseTextParser.learnSkill( skill );

			return;
		}

		case ItemPool.OLFACTION_BOOK:
		{
			if ( !responseText.contains( "smell has been elevated to a superhuman level" ) )
			{
				UseItemRequest.lastUpdate = "You can't learn that skill.";
				KoLmafia.updateDisplay( MafiaState.ERROR, UseItemRequest.lastUpdate );
				if ( UseItemRequest.getConsumptionType( item ) != KoLConstants.INFINITE_USES )
				{
					ResultProcessor.processResult( item );
				}
				return;
			}

			String skill = UseItemRequest.itemToSkill( itemId );
			if ( skill == null )
			{
				return;
			}

			ResponseTextParser.learnSkill( skill );

			return;
		}

		case ItemPool.TEACHINGS_OF_THE_FIST:
			// You learn a different skill from each scroll
			Preferences.increment( "fistSkillsKnown", 1 );
			ResponseTextParser.learnSkillFromResponse( responseText );
			return;

		case ItemPool.SLIME_SOAKED_HYPOPHYSIS:
		case ItemPool.SLIME_SOAKED_BRAIN:
		case ItemPool.SLIME_SOAKED_SWEAT_GLAND:
		{
			for ( int i = 46; i <= 48; ++i )
			{
				GenericRequest req = new GenericRequest(
					"desc_skill.php?whichskill=" + i + "&self=true" );
				RequestThread.postRequest( req );
			}

			// You can learn the appropriate skill up to 10 times.
			// What does it say if you try to use the 11th?
			if ( responseText.indexOf( "You gain a skill" ) == -1 )
			{	// Item may be consumed even if you already have the skill
				// ResultProcessor.processResult( item );
				return;
			}

			String skill = UseItemRequest.itemToSkill( item.getItemId() );
			ResponseTextParser.learnSkill( skill );

			return;
		}

		case ItemPool.TELESCOPE:
			// We've added or upgraded our telescope
			KoLCharacter.setTelescope( true );

			// Look through it to check number of upgrades
			Preferences.setInteger( "lastTelescopeReset", -1 );
			KoLCharacter.checkTelescope();
			return;

		case ItemPool.ASTRAL_MUSHROOM:

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

		case ItemPool.WORKYTIME_TEA:

			// You're not quite bored enough to drink that much tea.

			if ( responseText.indexOf( "not quite bored enough" ) != -1 )
			{
				UseItemRequest.lastUpdate = "You're not bored enough to drink that much tea.";
				KoLmafia.updateDisplay( MafiaState.ERROR, UseItemRequest.lastUpdate );
				ResultProcessor.processResult( item );
			}

			return;

		case ItemPool.ABSINTHE:

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

		case ItemPool.DUSTY_ANIMAL_SKULL:

			// The magic that had previously animated the animals kicks back
			// in, and it stands up shakily and looks at you. "Graaangh?"

			if ( responseText.indexOf( "Graaangh?" ) == -1 )
			{
				UseItemRequest.lastUpdate = "You're missing some parts.";
				KoLmafia.updateDisplay( MafiaState.ERROR, UseItemRequest.lastUpdate );
				ResultProcessor.processResult( item );
				return;
			}

			// Remove the other 98 bones

			for ( int i = 1802; i < 1900; ++i )
			{
				ResultProcessor.processResult( new AdventureResult( i, -1 ) );
			}

			return;

		case ItemPool.DRUM_MACHINE:

			// "And dammit, your hooks were still on there! Oh well."

			if ( responseText.indexOf( "hooks were still on" ) != -1 )
			{
				if ( KoLCharacter.hasEquipped( ItemPool.WORM_RIDING_HOOKS, EquipmentManager.WEAPON ) )
				{
					// You lose your weapon
					EquipmentManager.discardEquipment( ItemPool.WORM_RIDING_HOOKS );
					KoLmafia.updateDisplay( "Don't forget to equip a weapon!" );
				}
				else
				{
					// You lose your hooks
					ResultProcessor.processItem( ItemPool.WORM_RIDING_HOOKS, -1 );
				}

				int gnasirProgress = Preferences.getInteger( "gnasirProgress" );
				gnasirProgress |= 16;
				Preferences.setInteger( "gnasirProgress", gnasirProgress );

				QuestManager.incrementDesertExploration( 30 );
				return;
			}

			// "You don't have time to play the drums."
			if ( responseText.indexOf( "don't have time" ) != -1 )
			{
				UseItemRequest.lastUpdate = "Insufficient adventures left.";
			}

			// "You're too beaten-up to play the drums."
			else if ( responseText.indexOf( "too beaten up" ) != -1 )
			{
				UseItemRequest.lastUpdate = "Too beaten up.";
			}

			// "You head to your campsite and crank up the drum
			// machine. You press buttons at random, waiting for
			// something interesting to happen, but you only
			// succeed in annoying your neighbors."
			else if ( responseText.indexOf( "head to your campsite" ) != -1 )
			{
				UseItemRequest.lastUpdate = "Can't find the beach";
			}

			if ( !UseItemRequest.lastUpdate.equals( "" ) )
			{
				KoLmafia.updateDisplay( MafiaState.ERROR, UseItemRequest.lastUpdate );
			}

			// If we are redirected to a fight, the item is
			// consumed elsewhere. If we got here, we removed it
			// above, but it wasn't actually consumed

			ResultProcessor.processResult( item );

			return;

		case ItemPool.CURSED_PIECE_OF_THIRTEEN:

			// "You take the piece of thirteen to a rare coin
			// dealer in Seaside Town (he's got a shop set up right
			// next to that library across the street from the
			// Sleazy Back Alley) to see what you can get for
			// it. Turns out you can get X Meat for it."
			if ( responseText.indexOf( "rare coin dealer in Seaside Town" ) != -1 )
			{
				return;
			}

			// You consider taking the piece of thirteen to a rare
			// coin dealer to see if it's worth anything, but you
			// don't really have time.
			if ( responseText.indexOf( "don't really have time" ) != -1 )
			{
				UseItemRequest.lastUpdate = "Insufficient adventures left.";
			}

			// "You consider taking the piece of thirteen to a rare
			// coin dealer to see if it's worth anything, but
			// you're feeling pretty crappy right now."
			else if ( responseText.indexOf( "feeling pretty crappy" ) != -1 )
			{
				UseItemRequest.lastUpdate = "Too beaten up.";
			}

			if ( !UseItemRequest.lastUpdate.equals( "" ) )
			{
				KoLmafia.updateDisplay( MafiaState.ERROR, UseItemRequest.lastUpdate );
			}

			// If we are redirected to a fight, the item is
			// consumed elsewhere. If we got here, we removed it
			// above, but it wasn't actually consumed

			ResultProcessor.processResult( item );

			return;

		case ItemPool.SPOOKY_PUTTY_MONSTER:

			// You can't tell what this is supposed to be a copy
			// of. You squish it back into a sheet.

			if ( responseText.indexOf( "squish it back into a sheet" ) != -1 )
			{
				Preferences.setString( "spookyPuttyMonster", "" );
				return;
			}

			// If we are redirected to a fight, the item is
			// consumed elsewhere. If we got here, we removed it
			// above, but it wasn't actually consumed

			ResultProcessor.processResult( item );

			return;

		case ItemPool.D10:

			// You don't have time to go on an adventure. Even an imaginary one.
			if ( responseText.indexOf( "don't have time" ) != -1 )
			{
				UseItemRequest.lastUpdate = "Insufficient adventures left.";
			}

			// Your imagination is too drunk right now.
			else if ( responseText.indexOf( "Your imagination is too drunk" ) != -1 )
			{
				UseItemRequest.lastUpdate = "Inebriety limit reached.";
			}

			// Using one of these items will eventually do
			// something. I am sorry that eventually is not now,
			// but I ran out of time before KoL Con.
			else if ( responseText.indexOf( "eventually is not now" ) != -1 )
			{
				UseItemRequest.lastUpdate = "Not yet implemented.";
			}

			if ( !UseItemRequest.lastUpdate.equals( "" ) )
			{
				KoLmafia.updateDisplay( MafiaState.ERROR, UseItemRequest.lastUpdate );
				ResultProcessor.processResult( item );
			}

			return;

		case ItemPool.D12: {

			// You draw the bow and roll [X]d12 to see how far the arrow flies.

			Matcher m = ARROW_PATTERN.matcher( responseText );
			String distance = m.find() ? m.group( 1 ) : "";

			// It goes [Xd12] feet, and just as it's about to hit the ground,
			// a cart with a big target in it is pulled into view and
			// the arrow hits it dead center. BULLSEYE.

			if ( responseText.indexOf( "BULLSEYE" ) != -1 )
			{
				String message = "You get a bullseye at " + distance + " feet.";
				KoLmafia.updateDisplay( message );
				RequestLogger.updateSessionLog( message );
			}

			// It goes [Xd12] feet, and doesn't hit anything interesting.
			// You grumble and put the dice away.
			else if ( responseText.indexOf( "You grumble and put the dice away" ) != -1 )
			{
				UseItemRequest.lastUpdate = "You grumble and put the dice away.";
			}

			// Y'know, you're never going to be able to top what happened last time. That was awesome.
			else if ( responseText.indexOf( "That was awesome" ) != -1 )
			{
				UseItemRequest.lastUpdate = "You already hit a bullseye.";
			}

			if ( !UseItemRequest.lastUpdate.equals( "" ) )
			{
				KoLmafia.updateDisplay( MafiaState.ERROR, UseItemRequest.lastUpdate );
				ResultProcessor.processResult( item );
			}

			return;
		}

		case ItemPool.D20:

			// You already rolled for initiative.
			if ( responseText.indexOf( "You already rolled for initiative" ) != -1 )
			{
				UseItemRequest.lastUpdate = "You already rolled for initiative";
			}

			// You can't figure out a good way to roll that
			// quantity of 20-sided dice. Maybe you should've paid
			// less attention in gym class.
			else if ( responseText.indexOf( "Maybe you should've paid less attention in gym class" ) != -1 )
			{
				UseItemRequest.lastUpdate = "Rolling that many d20s doesn't do anything interesting.";
			}

			if ( !UseItemRequest.lastUpdate.equals( "" ) )
			{
				KoLmafia.updateDisplay( MafiaState.ERROR, UseItemRequest.lastUpdate );
				ResultProcessor.processResult( item );
			}

			return;

		case ItemPool.BRICKO_OOZE:
		case ItemPool.BRICKO_BAT:
		case ItemPool.BRICKO_OYSTER:
		case ItemPool.BRICKO_TURTLE:
		case ItemPool.BRICKO_ELEPHANT:
		case ItemPool.BRICKO_OCTOPUS:
		case ItemPool.BRICKO_PYTHON:
		case ItemPool.BRICKO_VACUUM_CLEANER:
		case ItemPool.BRICKO_AIRSHIP:
		case ItemPool.BRICKO_CATHEDRAL:
		case ItemPool.BRICKO_CHICKEN:

			if ( responseText.indexOf( "You're sick of playing with BRICKOs today" ) != -1 )
			{
				Preferences.setInteger( "_brickoFights", 10 );
				UseItemRequest.lastUpdate = "You're sick of playing with BRICKOs today";
				KoLmafia.updateDisplay( UseItemRequest.lastUpdate );
			}
			// You're too drunk to mess with BRICKO right now.

			// If we are redirected to a fight, the item is
			// consumed elsewhere. If we got here, we removed it
			// above, but it wasn't actually consumed

			ResultProcessor.processResult( item );

			return;

		case ItemPool.RECORDING_BALLAD:
		case ItemPool.RECORDING_BENETTON:
		case ItemPool.RECORDING_CHORALE:
		case ItemPool.RECORDING_DONHO:
		case ItemPool.RECORDING_ELRON:
		case ItemPool.RECORDING_INIGO:
		case ItemPool.RECORDING_PRELUDE:

			// You already have too many songs stuck in your head.

			if ( responseText.indexOf( "too many songs stuck in your head" ) != -1 )
			{
				UseItemRequest.lastUpdate = "You have the maximum number of AT buffs already.";
				KoLmafia.updateDisplay( MafiaState.ERROR, UseItemRequest.lastUpdate );
				ResultProcessor.processResult( item );
			}

			return;

		case ItemPool.FOSSILIZED_BAT_SKULL:
		case ItemPool.FOSSILIZED_SERPENT_SKULL:
		case ItemPool.FOSSILIZED_BABOON_SKULL:
		case ItemPool.FOSSILIZED_WYRM_SKULL:
		case ItemPool.FOSSILIZED_DEMON_SKULL:
		case ItemPool.FOSSILIZED_SPIDER_SKULL:

			// If we are redirected to a fight, the item is
			// consumed elsewhere. If we got here, we removed it
			// above, but it wasn't actually consumed

			ResultProcessor.processResult( item );

			return;

		case ItemPool.SHAKING_CAMERA:

			Preferences.setBoolean( "_cameraUsed", true );

			// You get the sense that the monster in this camera
			// isn't ready to be developed just yet. It'll probably
			// be ready tomorrow. And no, you can't speed it up by
			// blowing on it.

			// If we are redirected to a fight, the item is
			// consumed elsewhere. If we got here, we removed it
			// above, but it wasn't actually consumed

			ResultProcessor.processResult( item );

			return;

		case ItemPool.SHAKING_CRAPPY_CAMERA:

			Preferences.setBoolean( "_crappyCameraUsed", true );

			// You get the sense that the monster in this camera
			// isn't ready to be developed just yet. It'll probably
			// be ready tomorrow. And no, you can't speed it up by
			// blowing on it.

			// If we are redirected to a fight, the item is
			// consumed elsewhere. If we got here, we removed it
			// above, but it wasn't actually consumed

			ResultProcessor.processResult( item );

			return;

		case ItemPool.PHOTOCOPIED_MONSTER:

			Preferences.setBoolean( "_photocopyUsed", true );

			// You get nauseated just thinking about the smell of
			// copier toner.  You don't think you can handle
			// another one of these things today.

			// If we are redirected to a fight, the item is
			// consumed elsewhere. If we got here, we removed it
			// above, but it wasn't actually consumed

			ResultProcessor.processResult( item );

			return;

		case ItemPool.PHOTOCOPIER:

			// You look around to see if anybody's watching,
			// and when they're not, you drop your pants and
			// giggle as you make a photocopy of your ass.

			// When you go to get up, you shatter the glass of the
			// photocopier. Dammit.

			// You don't think you can handle another one of these
			// things today.

			// If you had a photocopied monster in your inventory,
			// nothing happens:

			// You don't want your desk to get all messy --
			// you probably shouldn't copy anything else until
			// you've dealt with this copy you've already got.

			if ( responseText.indexOf( "you drop your pants and giggle" ) != -1 )
			{
				Preferences.setString( "photocopyMonster", "Your butt" );
			}
			else
			{
				ResultProcessor.processResult( item );
			}
			return;

		case ItemPool.BGE_TATTOO:

			// You've already got one of those tattoos on.
			// You should give this one to somebody who will
			// appreciate it more.

			if ( responseText.indexOf( "You've already got one of those tattoos on" ) != -1 )
			{
				ResultProcessor.processResult( item );
			}
			return;

		case ItemPool.DOLPHIN_WHISTLE:

			// If we are redirected to a fight, the item is
			// consumed elsewhere. If we got here, we removed it
			// above, but it wasn't actually consumed

			ResultProcessor.processResult( item );

			return;

		case ItemPool.MOJO_FILTER:

			// You strain some of the toxins out of your mojo, and
			// discard the now-grodulated filter.

			if ( responseText.indexOf( "now-grodulated" ) == -1 )
			{
				ResultProcessor.processResult( item );
				return;
			}

			Preferences.increment( "currentMojoFilters", item.getCount() );

			Preferences.setInteger(
				"currentSpleenUse",
				Math.max( 0, Preferences.getInteger( "currentSpleenUse" ) - item.getCount() ) );

			KoLCharacter.updateStatus();
			ConcoctionDatabase.getUsables().sort();
			return;

		case ItemPool.SPICE_MELANGE:

			// You pop the spice melange into your mouth and chew it up.
			if ( responseText.indexOf( "too scared to eat any more of that stuff today" ) != -1 )
			{
				Preferences.setBoolean( "spiceMelangeUsed", true );
				ResultProcessor.processResult( item );
			}
			else if ( responseText.indexOf( "You pop the spice melange into your mouth and chew it up" ) != -1 )
			{
				Preferences.setInteger( "currentFullness", Math.max( 0, Preferences.getInteger( "currentFullness" ) - 3 ) );
				KoLCharacter.setInebriety( Math.max( 0, KoLCharacter.getInebriety() - 3 ) );
				Preferences.setBoolean( "spiceMelangeUsed", true );
				KoLCharacter.updateStatus();
				ConcoctionDatabase.getUsables().sort();
			}
			return;

		case ItemPool.ULTRA_MEGA_SOUR_BALL:

			// You pop the candy in your mouth, and it immediately absorbs almost all of the moisture in your body.
			if ( responseText.contains( "too scared to eat any more of that candy today" ) )
			{
				Preferences.setBoolean( "_ultraMegaSourBallUsed", true );
				ResultProcessor.processResult( item );
			}
			else if ( responseText.contains( "You pop the candy in your mouth, and it immediately absorbs almost all of the moisture in your body" ) )
			{
				Preferences.setInteger( "currentFullness", Math.max( 0, Preferences.getInteger( "currentFullness" ) - 3 ) );
				KoLCharacter.setInebriety( Math.max( 0, KoLCharacter.getInebriety() - 3 ) );
				Preferences.setBoolean( "_ultraMegaSourBallUsed", true );
				KoLCharacter.updateStatus();
				ConcoctionDatabase.getUsables().sort();
			}
			return;

		case ItemPool.SYNTHETIC_DOG_HAIR_PILL:

			//Your liver feels better! And quivers a bit.
			if ( responseText.indexOf( "liver can't take any more abuse" ) != -1 )
			{
				Preferences.setBoolean( "_syntheticDogHairPillUsed", true );
				ResultProcessor.processResult( item );
			}
			else if ( responseText.indexOf( "quivers" ) != -1 )
			{
				KoLCharacter.setInebriety( Math.max( 0, KoLCharacter.getInebriety() - 1 ) );
				Preferences.setBoolean( "_syntheticDogHairPillUsed", true );
				KoLCharacter.updateStatus();
				ConcoctionDatabase.getUsables().sort();
			}
			return;

		case ItemPool.DISTENTION_PILL:

			// Your stomach feels rather stretched out
			if ( responseText.indexOf( "stomach can't take any more abuse" ) != -1 )
			{
				Preferences.setBoolean( "_distentionPillUsed", true );
				ResultProcessor.processResult( item );
			}
			else if ( responseText.indexOf( "stomach feels rather stretched" ) != -1 )
			{
				Preferences.setBoolean( "_distentionPillUsed", true );
				KoLCharacter.updateStatus();
				ConcoctionDatabase.getUsables().sort();
			}
			return;

		case ItemPool.MILK_OF_MAGNESIUM:

			ConcoctionDatabase.getUsables().sort();
			return;

		case ItemPool.NEWBIESPORT_TENT:
		case ItemPool.BARSKIN_TENT:
		case ItemPool.COTTAGE:
		case ItemPool.BRICKO_PYRAMID:
		case ItemPool.HOUSE:
		case ItemPool.SANDCASTLE:
		case ItemPool.TWIG_HOUSE:
		case ItemPool.GINGERBREAD_HOUSE:
		case ItemPool.HOBO_FORTRESS:

			if ( responseText.contains( "You've already got" ) )
			{
				ResultProcessor.processResult( item );
				return;
			}
			CampgroundRequest.destroyFurnishings();
			CampgroundRequest.setCurrentDwelling( itemId );
			return;

		case ItemPool.SCARECROW:
		case ItemPool.MEAT_GOLEM:
		case ItemPool.MAID:
		case ItemPool.BLACK_BLUE_LIGHT:
		case ItemPool.LOUDMOUTH_LARRY:
		case ItemPool.PLASMA_BALL:
		case ItemPool.LED_CLOCK:
		case ItemPool.BONSAI_TREE:
		case ItemPool.MEAT_GLOBE:

			if ( responseText.contains( "You've already got" ) )
			{
				ResultProcessor.processResult( item );
				return;
			}
			CampgroundRequest.setCampgroundItem( itemId, 1 );
			return;

		case ItemPool.CLOCKWORK_MAID:

			if ( responseText.contains( "You've already got" ) )
			{
				ResultProcessor.processResult( item );
				return;
			}
			CampgroundRequest.removeCampgroundItem( ItemPool.get( ItemPool.MAID, 1 ) );
			CampgroundRequest.setCampgroundItem( ItemPool.CLOCKWORK_MAID, 1 );
			return;

		case ItemPool.BEANBAG_CHAIR:
		case ItemPool.GAUZE_HAMMOCK:
		case ItemPool.HOT_BEDDING:
		case ItemPool.COLD_BEDDING:
		case ItemPool.STENCH_BEDDING:
		case ItemPool.SPOOKY_BEDDING:
		case ItemPool.SLEAZE_BEDDING:
		case ItemPool.SLEEPING_STOCKING:
		case ItemPool.LAZYBONES_RECLINER:
		case ItemPool.SALTWATERBED:
		case ItemPool.SPIRIT_BED:
		
			if ( responseText.contains( "You've already got" ) || responseText.contains( "You don't have" ) )
			{
				ResultProcessor.processResult( item );
				return;
			}
			CampgroundRequest.setCurrentBed( ItemPool.get( itemId, 1 ) );
			CampgroundRequest.setCampgroundItem( itemId, 1 );
			return;

		case ItemPool.MILKY_POTION:
		case ItemPool.SWIRLY_POTION:
		case ItemPool.BUBBLY_POTION:
		case ItemPool.SMOKY_POTION:
		case ItemPool.CLOUDY_POTION:
		case ItemPool.EFFERVESCENT_POTION:
		case ItemPool.FIZZY_POTION:
		case ItemPool.DARK_POTION:
		case ItemPool.MURKY_POTION:

			String[][] strings = ItemPool.bangPotionStrings;

			for ( int i = 0; i < strings.length; ++i )
			{
				if ( responseText.indexOf( strings[i][2] ) != -1 )
				{
					if ( ItemPool.eliminationProcessor( strings, i,
						item.getItemId(),
						819, 827,
						"lastBangPotion", " of " ) )
					{
						KoLmafia.updateDisplay( "All bang potions have been identified!" );
					}
					break;
				}
			}

			// You don't consume inebriety potions in HCO or HCT
			if ( responseText.indexOf( "You decide not to drink it" ) != -1 )
			{
				ResultProcessor.processResult( item );
			}

			return;

		case ItemPool.VIAL_OF_RED_SLIME:
		case ItemPool.VIAL_OF_YELLOW_SLIME:
		case ItemPool.VIAL_OF_BLUE_SLIME:

			strings = ItemPool.slimeVialStrings[0];
			for ( int i = 0; i < strings.length; ++i )
			{
				if ( responseText.indexOf( strings[i][1] ) != -1 )
				{
					if ( ItemPool.eliminationProcessor( strings, i,
						item.getItemId(),
						ItemPool.VIAL_OF_RED_SLIME, ItemPool.VIAL_OF_BLUE_SLIME,
						"lastSlimeVial", ": " ) )
					{
						KoLmafia.updateDisplay( "All primary slime vials have been identified!" );
					}
					break;
				}
			}
			return;

		case ItemPool.VIAL_OF_ORANGE_SLIME:
		case ItemPool.VIAL_OF_GREEN_SLIME:
		case ItemPool.VIAL_OF_VIOLET_SLIME:

			strings = ItemPool.slimeVialStrings[1];
			for ( int i = 0; i < strings.length; ++i )
			{
				if ( responseText.indexOf( strings[i][1] ) != -1 )
				{
					if ( ItemPool.eliminationProcessor( strings, i,
						item.getItemId(),
						ItemPool.VIAL_OF_ORANGE_SLIME, ItemPool.VIAL_OF_VIOLET_SLIME,
						"lastSlimeVial", ": " ) )
					{
						KoLmafia.updateDisplay( "All secondary slime vials have been identified!" );
					}
					break;
				}
			}
			return;

		case ItemPool.VIAL_OF_VERMILION_SLIME:
		case ItemPool.VIAL_OF_AMBER_SLIME:
		case ItemPool.VIAL_OF_CHARTREUSE_SLIME:
		case ItemPool.VIAL_OF_TEAL_SLIME:
		case ItemPool.VIAL_OF_INDIGO_SLIME:
		case ItemPool.VIAL_OF_PURPLE_SLIME:

			strings = ItemPool.slimeVialStrings[2];
			for ( int i = 0; i < strings.length; ++i )
			{
				if ( responseText.indexOf( strings[i][1] ) != -1 )
				{
					if ( ItemPool.eliminationProcessor( strings, i,
						item.getItemId(),
						ItemPool.VIAL_OF_VERMILION_SLIME, ItemPool.VIAL_OF_PURPLE_SLIME,
						"lastSlimeVial", ": " ) )
					{
						KoLmafia.updateDisplay( "All tertiary slime vials have been identified!" );
					}
					break;
				}
			}
			return;

		case ItemPool.ANCIENT_CURSED_FOOTLOCKER:
			if ( InventoryManager.hasItem( ItemPool.SIMPLE_CURSED_KEY ) )
			{
				ResultProcessor.processItem( ItemPool.SIMPLE_CURSED_KEY, -1 );
			}
			else
			{
				ResultProcessor.processResult( item );
			}
			return;

		case ItemPool.ORNATE_CURSED_CHEST:
			if ( InventoryManager.hasItem( ItemPool.ORNATE_CURSED_KEY ) )
			{
				ResultProcessor.processItem( ItemPool.ORNATE_CURSED_KEY, -1 );
			}
			else
			{
				ResultProcessor.processResult( item );
			}
			return;

		case ItemPool.GILDED_CURSED_CHEST:
			if ( InventoryManager.hasItem( ItemPool.GILDED_CURSED_KEY ) )
			{
				ResultProcessor.processItem( ItemPool.GILDED_CURSED_KEY, -1 );
			}
			else
			{
				ResultProcessor.processResult( item );
			}
			return;

		case ItemPool.STUFFED_CHEST:
			if ( InventoryManager.hasItem( ItemPool.STUFFED_KEY ) )
			{
				ResultProcessor.processItem( ItemPool.STUFFED_KEY, -1 );
			}
			else
			{
				ResultProcessor.processResult( item );
			}
			return;

		case ItemPool.GENERAL_ASSEMBLY_MODULE:

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
				ResultProcessor.processResult( ItemPool.get( ItemPool.LASER_CANON, -1 ) );
				ResultProcessor.processResult( ItemPool.get( ItemPool.TARGETING_CHOP, -1 ) );
				ResultProcessor.processResult( ItemPool.get( ItemPool.UNOBTAINIUM_STRAPS, -1 ) );

				return;
			}

			// You breathe a heavy sigh of relief as the pseudopods
			// emerge from your inventory, carrying the polymorphic
			// fastening apparatus, hi-density nylocite leg armor,
			// and the silicon-infused gluteal shield that you
			// acquired earlier from the Sinister Dodecahedron.

			if ( responseText.indexOf( "carrying the  polymorphic fastening apparatus" ) != -1 )
			{
				ResultProcessor.processResult( ItemPool.get( ItemPool.FASTENING_APPARATUS, -1 ) );
				ResultProcessor.processResult( ItemPool.get( ItemPool.LEG_ARMOR, -1 ) );
				ResultProcessor.processResult( ItemPool.get( ItemPool.GLUTEAL_SHIELD, -1 ) );

				return;
			}

			// You breathe a heavy sigh of relief as the pseudopods
			// emerge from your inventory, carrying the carbonite
			// visor, plexifoam chin strap, and the kevlateflocite
			// helmet that you acquired earlier from the Sinister
			// Dodecahedron.

			if ( responseText.indexOf( "carrying the  carbonite visor" ) != -1 )
			{
				ResultProcessor.processResult( ItemPool.get( ItemPool.CARBONITE_VISOR, -1 ) );
				ResultProcessor.processResult( ItemPool.get( ItemPool.CHIN_STRAP, -1 ) );
				ResultProcessor.processResult( ItemPool.get( ItemPool.KEVLATEFLOCITE_HELMET, -1 ) );

				return;
			}

			return;

		case ItemPool.AUGMENTED_DRONE:

			if ( responseText.indexOf( "You put an overcharged sphere in the cavity" ) != -1 )
			{
				ResultProcessor.processResult( ItemPool.get( ItemPool.OVERCHARGED_POWER_SPHERE, -1 ) );
			}

			return;

		case ItemPool.NEVERENDING_SODA:
			Preferences.setBoolean( "oscusSodaUsed", true );
			return;

		case ItemPool.EL_VIBRATO_HELMET:
			// We should parse the result and save the current
			// state of the conduits
			return;

		case ItemPool.PUNCHCARD_ATTACK:
		case ItemPool.PUNCHCARD_REPAIR:
		case ItemPool.PUNCHCARD_BUFF:
		case ItemPool.PUNCHCARD_MODIFY:
		case ItemPool.PUNCHCARD_BUILD:
			if ( responseText.indexOf( "A voice emerges" ) != -1 )
			{
				// We should save the state of the Megadrone
			}
			else
			{
				// You try to stick the punchcard into <name>
				// and fail.
				ResultProcessor.processResult( item );
			}
			return;

		case ItemPool.OUTRAGEOUS_SOMBRERO:
			Preferences.setBoolean( "outrageousSombreroUsed", true );
			return;

		case ItemPool.GRUB:
		case ItemPool.MOTH:
		case ItemPool.FIRE_ANT:
		case ItemPool.ICE_ANT:
		case ItemPool.STINKBUG:
		case ItemPool.DEATH_WATCH_BEETLE:
		case ItemPool.LOUSE:

			// You briefly consider eating the plump juicy grub,
			// but are filled with revulsion at the prospect.
			if ( responseText.indexOf( "filled with revulsion" ) != -1 )
			{
				ResultProcessor.processResult( item );
			}

			return;

		case ItemPool.PERSONAL_MASSAGER:

			// You don't really need a massage right now, as your
			// neck and back aren't feeling particularly kinky.
			if ( responseText.indexOf( "don't really need a massage" ) != -1 )
			{
				ResultProcessor.processResult( item );
			}
			return;

		case ItemPool.TOMB_RATCHET:

			PyramidRequest.advancePyramidPosition();
			return;

		case ItemPool.BURROWGRUB_HIVE:

			// One way or another, you have used it today
			Preferences.setBoolean( "burrowgrubHiveUsed", true );

			// You pick up the burrowgrub hive to look inside it,
			// and a bunch of the grubs crawl out of it and burrow
			// under your skin.  It's horrifying.  You can still
			// feel them in there. Gah.

			if ( responseText.indexOf( "It's horrifying." ) != -1 )
			{
				// You have three grub summons left today
				Preferences.setInteger( "burrowgrubSummonsRemaining", 3 );
			}

			return;

		case ItemPool.BOOZEHOUND_TOKEN:

			// You'd take this thing to a bar and see what you can
			// trade it in for, but you don't know where any bars
			// are.

			if ( responseText.indexOf( "don't know where any bars are" ) != -1 )
			{
				ResultProcessor.processResult( item );
			}
			return;

		case ItemPool.SMALL_LAMINATED_CARD:
		case ItemPool.LITTLE_LAMINATED_CARD:
		case ItemPool.NOTBIG_LAMINATED_CARD:
		case ItemPool.UNLARGE_LAMINATED_CARD:
			ResultProcessor.processResult( item );
			DwarfFactoryRequest.useLaminatedItem( item.getItemId(), responseText );
			break;

		case ItemPool.DWARVISH_DOCUMENT:
		case ItemPool.DWARVISH_PAPER:
		case ItemPool.DWARVISH_PARCHMENT:
			ResultProcessor.processResult( item );
			DwarfFactoryRequest.useUnlaminatedItem( item.getItemId(), responseText );
			break;

		case ItemPool.WRETCHED_SEAL:
		case ItemPool.CUTE_BABY_SEAL:
		case ItemPool.ARMORED_SEAL:
		case ItemPool.ANCIENT_SEAL:
		case ItemPool.SLEEK_SEAL:
		case ItemPool.SHADOWY_SEAL:
		case ItemPool.STINKING_SEAL:
		case ItemPool.CHARRED_SEAL:
		case ItemPool.COLD_SEAL:
		case ItemPool.SLIPPERY_SEAL:
			// If we are redirected to a fight, the item is
			// consumed elsewhere. If we got here, we removed it
			// above, but it wasn't actually consumed

			ResultProcessor.processResult( item );

			// The depleted uranium seal was NOT removed above.
		case ItemPool.DEPLETED_URANIUM_SEAL:

			// You've summoned too many Infernal seals today. Any
			// more and you're afraid the corruption will be too
			// much for you to bear.

			if ( responseText.indexOf( "too many Infernal seals" ) != -1 )
			{
				int maxSummons = 5;
				if ( KoLCharacter.hasEquipped( INFERNAL_SEAL_CLAW ) ||
					InventoryManager.getCount( ItemPool.INFERNAL_SEAL_CLAW ) > 0 )
				{
					maxSummons = 10;
				}
				UseItemRequest.lastUpdate = "Summoning limit reached.";
				Preferences.setInteger( "_sealsSummoned", maxSummons );
			}

			// In order to perform this summoning ritual, you need
			// 1 seal-blubber candle. You can pick them up at the
			// store in the Brotherhood of the Smackdown.

			else if ( responseText.indexOf( "Brotherhood of the Smackdown" ) != -1 )
			{
				UseItemRequest.lastUpdate = "You need more seal-blubber candles.";
			}

			// In order to perform this summoning ritual, you need
			// 1 imbued seal-blubber candle.

			else if ( responseText.indexOf( "you need 1 imbued seal-blubber candle" ) != -1 )
			{
				UseItemRequest.lastUpdate = "You need an imbued seal-blubber candle.";
			}

			// Only Seal Clubbers may use this item.

			else if ( responseText.indexOf( "Only Seal Clubbers may use this item." ) != -1 )
			{
				UseItemRequest.lastUpdate = "Only Seal Clubbers may use this item.";
			}

			// You need to be at least level 6 to use that item

			else if ( responseText.indexOf( "need to be at least level" ) != -1 )
			{
				UseItemRequest.lastUpdate = "You are not high enough level.";
			}

			if ( !UseItemRequest.lastUpdate.equals( "" ) )
			{
				KoLmafia.updateDisplay( MafiaState.ERROR, UseItemRequest.lastUpdate );
				return;
			}

			// You feel your clubbing muscles begin to twitch with
			// anticipation... Begin the Ritual
			//
			// Pressing "Begin the Ritual" reissues the request
			// with an additional field: check=1
			//
			// inv_use.php?which=3&whichitem=3902&pwd
			// inv_use.php?whichitem=3902&checked=1&pwd

			return;

		case ItemPool.SEAL_IRON_INGOT:

			// You beat the seal-iron into a formidable club.
			if ( responseText.indexOf( "formidable club" ) == -1 )
			{
				ResultProcessor.processResult( item );
			}

			return;

		case ItemPool.SINISTER_ANCIENT_TABLET:

			AdventureRequest.registerDemonName( "Sinister Ancient Tablet", responseText );

			return;

		case ItemPool.BAG_O_TRICKS:
			Preferences.setBoolean( "_bagOTricksUsed", true );
			return;

		case ItemPool.EVILOMETER:
			// Parse the result and save current state
			UseItemRequest.getEvilLevels( responseText );
			return;

		case ItemPool.EVIL_EYE:
			if ( responseText.indexOf( "Evilometer emits three quick beeps" ) != -1 )
			{
				int evilness = Math.min( Preferences.getInteger( "cyrptNookEvilness" ), 3 );
				Preferences.increment( "cyrptNookEvilness", -evilness );
				Preferences.increment( "cyrptTotalEvilness", -evilness );
			}
			return;

		case ItemPool.QUASIRELGIOUS_SCULPTURE:
		case ItemPool.SOLID_GOLD_ROSARY:
			if ( responseText.contains( "entire Cyrpt feels safer" ) )
			{
				// Can't abuse this, so queue a use item to get new Evilometer value
				RequestThread.postRequest( UseItemRequest.getInstance( ItemPool.EVILOMETER ) );
			}
			return;
			
		case ItemPool.KEYOTRON:
			UseItemRequest.getBugbearBiodataLevels( responseText );
			return;

		case ItemPool.PEN_PAL_KIT:
			// You've already got a pen pal. There's no way you
			// could handle the pressure of contantly forgetting to
			// reply to two kids from Distant Lands...
			if ( responseText.indexOf( "already got a pen pal" ) != -1 )
			{
				ResultProcessor.processResult( item );
			}
			return;

		case ItemPool.HONEYPOT:
			// You gain the "Float Like a Butterfly, Smell Like a
			// Bee" effect.	 This prevents bees from appearing
			// while it is active.	As soon as it wears off, a bee
			// will appear.	 Stop the bee counters, since turns
			// remaining of the effect give the same info.
			TurnCounter.stopCounting( "Bee window begin" );
			TurnCounter.stopCounting( "Bee window end" );
			return;

		case ItemPool.RONALD_SHELTER_MAP:
		case ItemPool.GRIMACE_SHELTER_MAP:
			// If we are redirected to a choice, the item is
			// consumed elsewhere.
			return;

		case ItemPool.BORROWED_TIME:
			// Set the preference to true both when we fail and succeed.
			Preferences.setBoolean( "_borrowedTimeUsed", true );

			if ( responseText.indexOf( "already borrowed some time today" ) != -1 )
			{
				ResultProcessor.processResult( item );
			}

			// You dip into your future and borrow some time. Be sure to spend it wisely!
			else if ( responseText.indexOf( "dip into your future" ) != -1 )
			{
				KoLCharacter.updateStatus();
				Preferences.increment( "extraRolloverAdventures", -20 );
			}
			return;

		case ItemPool.MOVEABLE_FEAST:
			// The table is looking pretty bare -- you should wait
			// until tomorrow, and let some of the food magically regenerate.
			if ( responseText.indexOf( "wait until tomorrow" ) != -1 )
			{
				Preferences.setInteger( "_feastUsed", 5 );
			}

			// <name> chows down on the moveable feast,
			// then leans back, sighs, and loosens his belt a couple of notches.
			else if ( responseText.indexOf( "chows down" ) != -1 )
			{
				Preferences.increment( "_feastUsed", 1 );

				String familiar = KoLCharacter.getFamiliar().getRace();
				String oldList = Preferences.getString( "_feastedFamiliars" );
				String newList = oldList + ( oldList.equals( "" ) ? "" : ";" ) + familiar;
				Preferences.setString( "_feastedFamiliars", newList );
			}
			return;

		case ItemPool.STAFF_GUIDE:

			if ( responseText.indexOf( "You don't have time to screw around in a haunted house" ) != -1 )
			{
				UseItemRequest.lastUpdate = "Insufficient adventures to use a staff guide.";
				KoLmafia.updateDisplay( MafiaState.ERROR, UseItemRequest.lastUpdate );
				ResultProcessor.processResult( item );
			}

			else if ( responseText.indexOf( "You aren't allowed to go to any Haunted Houses right now" ) != -1 )
			{
				UseItemRequest.lastUpdate = "You aren't allowed to go to any Haunted Houses right now.";
				KoLmafia.updateDisplay( MafiaState.ERROR, UseItemRequest.lastUpdate );
				ResultProcessor.processResult( item );
			}

			else if ( responseText.indexOf( "You don't know where any haunted sorority houses are right now." ) != -1  ||
			     responseText.indexOf( "No way. It's boring in there now that everybody is dead." ) != -1 )
			{
				UseItemRequest.lastUpdate = "The Haunted Sorority House is unavailable.";
				KoLmafia.updateDisplay( MafiaState.ERROR, UseItemRequest.lastUpdate );
				ResultProcessor.processResult( item );
			}

			return;

		case ItemPool.GHOSTLY_BODY_PAINT:
		case ItemPool.NECROTIZING_BODY_SPRAY:
		case ItemPool.BITE_LIPSTICK:
		case ItemPool.WHISKER_PENCIL:
		case ItemPool.PRESS_ON_RIBS:

			if ( responseText.indexOf( "You've already got a sexy costume on" ) != -1 )
			{
				UseItemRequest.lastUpdate = "You've already got a sexy costume on.";
				KoLmafia.updateDisplay( MafiaState.ERROR, UseItemRequest.lastUpdate );
				ResultProcessor.processResult( item );
			}
			return;

		case ItemPool.BLACK_PAINT:

			if ( KoLCharacter.inFistcore() &&
			     responseText.indexOf( "Your teachings forbid the use of black paint." ) != -1 )
			{
				UseItemRequest.lastUpdate = "Your teachings forbid the use of black paint.";
				KoLmafia.updateDisplay( MafiaState.ERROR, UseItemRequest.lastUpdate );
				ResultProcessor.processResult( item );
			}
			return;

		case ItemPool.ALL_YEAR_SUCKER:
			Preferences.setBoolean( "_allYearSucker", true );
			return;

		case ItemPool.DARK_CHOCOLATE_HEART:
			Preferences.setBoolean( "_darkChocolateHeart", true );
			return;

		case ItemPool.JACKASS_PLUMBER_GAME:
			Preferences.setBoolean( "_jackassPlumberGame", true );
			return;

		case ItemPool.TRIVIAL_AVOCATIONS_GAME:
			Preferences.setBoolean( "_trivialAvocationsGame", true );
			return;

		case ItemPool.RESOLUTION_ADVENTUROUS:

			// You read aloud:
			// "I, <player name>, resolve to live this day as though it were my last day.
			// Actually, maybe tomorrow. Maybe I'll live tomorrow as though it were my last day.
			// That seems like a better idea. I'll be more adventurous tomorrow."
			// Then you resolve to do it again.
			// Then you resolve to do it again and again.
			// Then you resolve to do it again and again and again.
			// Then you resolve to do it again and again and again and again.

			if ( responseText.indexOf( "already feeling adventurous enough" ) != -1 )
			{
				// player has already used 5 resolutions today
				int extraAdv = 10 - Preferences.getInteger( "_resolutionAdv" );
				Preferences.increment( "extraRolloverAdventures", extraAdv );
				Preferences.increment( "_resolutionAdv", extraAdv );
				ResultProcessor.processResult( item );
				return;
			}

			int used = 1;
			matcher = ADVENTUROUS_RESOLUTION_PATTERN.matcher( responseText );
			while ( matcher.find() )
			{
				used += 1;
			}

			Preferences.increment( "extraRolloverAdventures", 2 * used );
			Preferences.increment( "_resolutionAdv", 2 * used );

			int count = item.getCount();
			if ( used < count )
			{
				ResultProcessor.processResult( item.getInstance( count - used ) );
			}

			return;

		case ItemPool.CLANCY_SACKBUT:
			KoLCharacter.setCurrentInstrument( CharPaneRequest.SACKBUT );
			return;

		case ItemPool.CLANCY_CRUMHORN:
			KoLCharacter.setCurrentInstrument( CharPaneRequest.CRUMHORN );
			return;

		case ItemPool.CLANCY_LUTE:
			KoLCharacter.setCurrentInstrument( CharPaneRequest.LUTE );
			return;

		case ItemPool.PEPPERMINT_PACKET:
		case ItemPool.PUMPKIN_SEEDS:
		case ItemPool.DRAGON_TEETH:
		case ItemPool.BEER_SEEDS:
		case ItemPool.WINTER_SEEDS:
			CampgroundRequest.clearCrop();
			RequestThread.postRequest( new CampgroundRequest() );
			return;

		case ItemPool.ESSENTIAL_TOFU:
			Preferences.setBoolean( "_essentialTofuUsed", true );
			return;

		case ItemPool.CHOCOLATE_CIGAR:
			if ( responseText.indexOf( "You light the end") != -1 )
			{
				Preferences.setInteger( "_chocolateCigarsUsed", 1 );
			}
			else if ( responseText.indexOf( "This one doesn't taste" ) != -1 )
			{
				Preferences.setInteger( "_chocolateCigarsUsed", 2 );
			}
			else
			{
				Preferences.setInteger( "_chocolateCigarsUsed", 3 );
			}
			return;

		case ItemPool.VITACHOC_CAPSULE:
			if ( responseText.indexOf( "As the nutritive nanobots" ) != -1 )
			{
				Preferences.setInteger( "_vitachocCapsulesUsed", 1 );
			}
			else if ( responseText.indexOf( "Your body is becoming acclimated" ) != -1 )
			{
				Preferences.setInteger( "_vitachocCapsulesUsed", 2 );
			}
			else
			{
				Preferences.setInteger( "_vitachocCapsulesUsed", 3 );
			}
			return;

		case ItemPool.FANCY_CHOCOLATE:
		case ItemPool.FANCY_EVIL_CHOCOLATE:
		case ItemPool.FANCY_CHOCOLATE_CAR:
		case ItemPool.CHOCOLATE_SEAL_CLUBBING_CLUB:
		case ItemPool.CHOCOLATE_TURTLE_TOTEM:
		case ItemPool.CHOCOLATE_PASTA_SPOON:
		case ItemPool.CHOCOLATE_SAUCEPAN:
		case ItemPool.CHOCOLATE_DISCO_BALL:
		case ItemPool.CHOCOLATE_STOLEN_ACCORDION:
		case ItemPool.BEET_MEDIOCREBAR:
		case ItemPool.CORN_MEDIOCREBAR:
		case ItemPool.CABBAGE_MEDIOCREBAR:
			Preferences.increment( "_chocolatesUsed" );
			return;

		case ItemPool.CREEPY_VOODOO_DOLL:
			Preferences.setBoolean( "_creepyVoodooDollUsed", true );
			return;

		case ItemPool.HOBBY_HORSE:
			Preferences.setBoolean( "_hobbyHorseUsed", true );
			return;

		case ItemPool.BALL_IN_A_CUP:
			Preferences.setBoolean( "_ballInACupUsed", true );
			return;

		case ItemPool.SET_OF_JACKS:
			Preferences.setBoolean( "_setOfJacksUsed", true );
			return;

		case ItemPool.BAG_OF_CANDY:
			Preferences.setBoolean( "_bagOfCandyUsed", true );
			return;

		case ItemPool.EMBLEM_AKGYXOTH:
		case ItemPool.IDOL_AKGYXOTH:
			Preferences.setBoolean( "_akgyxothUsed", true );
			return;

		case ItemPool.GNOLL_EYE:
			Preferences.setBoolean( "_gnollEyeUsed", true );
			return;

		case ItemPool.KOL_CON_SIX_PACK:
			Preferences.setBoolean( "_kolConSixPackUsed", true );
			return;

		case ItemPool.MUS_MANUAL:
		case ItemPool.MYS_MANUAL:
		case ItemPool.MOX_MANUAL:
			Preferences.setBoolean( "_guildManualUsed", true );
			return;

		case ItemPool.STUFFED_POCKETWATCH:
			// You play with the stuffed watch for a while, moving the
			// hands around at random, watching everything around you
			// speed up and slow down. It's great fun!
			if ( responseText.contains( "You play with the stuffed watch" ) )
			{
				Preferences.setBoolean( "_stuffedPocketwatchUsed", true );
			}
			return;

		case ItemPool.STYX_SPRAY:
			Preferences.setBoolean( "_styxSprayUsed", true );
			return;

		case ItemPool.STABONIC_SCROLL:
			Preferences.setBoolean( "_stabonicScrollUsed", true );
			return;

		case ItemPool.COAL_PAPERWEIGHT:
			Preferences.setBoolean( "_coalPaperweightUsed", true );
			return;

		case ItemPool.JINGLE_BELL:
			Preferences.setBoolean( "_jingleBellUsed", true );
			return;

		case ItemPool.BOX_OF_HAMMERS:
			Preferences.setBoolean( "_boxOfHammersUsed", true );
			return;

		case ItemPool.TEMPURA_AIR:
			Preferences.setBoolean( "_tempuraAirUsed", true );
			return;

		case ItemPool.PRESSURIZED_PNEUMATICITY:
			if ( responseText.contains( "You pop the cork" ) )
			{
				Preferences.setBoolean( "_pneumaticityPotionUsed", true );
			}
			return;

		case ItemPool.HYPERINFLATED_SEAL_LUNG:
			Preferences.setBoolean( "_hyperinflatedSealLungUsed", true );
			return;

		case ItemPool.BALLAST_TURTLE:
			Preferences.setBoolean( "_ballastTurtleUsed", true );
			return;

		case ItemPool.LEFT_BEAR_ARM:
			// Both bear arms are used up to create the box
			// You find a box, carefully label it, and shove a couple of bear arms into it.
			if ( responseText.contains( "You find a box" ) )
			{
				ResultProcessor.removeItem( ItemPool.RIGHT_BEAR_ARM );
			}
			else
			{
				ResultProcessor.processItem( ItemPool.LEFT_BEAR_ARM, 1 );
			}
			return;

		case ItemPool.CURSED_KEG:
			Preferences.setBoolean( "_cursedKegUsed", true );
			return;

		case ItemPool.CURSED_MICROWAVE:
			Preferences.setBoolean( "_cursedMicrowaveUsed", true );
			return;

		case ItemPool.TACO_FLIER:
			Preferences.setBoolean( "_tacoFlierUsed", true );
			return;

		case ItemPool.CRIMBO_CREEPY_HEAD:
		case ItemPool.CRIMBO_STOOGIE_ODORIZER:
		case ItemPool.CRIMBO_HOT_MUG:
		case ItemPool.CRIMBO_TREE_FLOCKER:
		case ItemPool.CRIMBO_RUDOLPH_DOLL:
			// Instead of checking which one was used and removing the others,
			// add the one that was used and remove one of each.

			ResultProcessor.processResult( item );
			ResultProcessor.processItem( ItemPool.CRIMBO_CREEPY_HEAD, -1 );
			ResultProcessor.processItem( ItemPool.CRIMBO_STOOGIE_ODORIZER, -1 );
			ResultProcessor.processItem( ItemPool.CRIMBO_HOT_MUG, -1 );
			ResultProcessor.processItem( ItemPool.CRIMBO_TREE_FLOCKER, -1 );
			ResultProcessor.processItem( ItemPool.CRIMBO_RUDOLPH_DOLL, -1 );
			return;

		case ItemPool.SUSPICIOUS_JAR:
		case ItemPool.GOURD_JAR:
		case ItemPool.MYSTIC_JAR:
		case ItemPool.OLD_MAN_JAR:
		case ItemPool.ARTIST_JAR:
		case ItemPool.MEATSMITH_JAR:
		case ItemPool.JICK_JAR:
			// You may have been successful
			if ( responseText.contains( "You open the jar and peer inside." ) )
			{
				CampgroundRequest.setCampgroundItem( itemId, 1 );
				Preferences.setBoolean( "_psychoJarUsed", true );
			}
			else
			{
				// If not, don't remove it
				ResultProcessor.processResult( item );
			}
			return;

		case ItemPool.FISHY_PIPE:
			Preferences.setBoolean( "_fishyPipeUsed", true );
			return;

		case ItemPool.BLANK_OUT_BOTTLE:
			if ( KoLCharacter.isJarlsberg() && responseText.contains( "mess with this crap" ) )
			{
				UseItemRequest.lastUpdate = "Jarlsberg hated getting his hands dirty. There is no way he would mess with this crap.";
				KoLmafia.updateDisplay( MafiaState.ERROR, UseItemRequest.lastUpdate );
				ResultProcessor.processResult( item );
			}
			else
			{
				Preferences.setBoolean( "_blankoutUsed", true );
			}
			return;

		case ItemPool.SONAR:
			if ( QuestDatabase.isQuestLaterThan( Preferences.getString( Quest.BAT.getPref() ), "step2" ) )
			{
				return;
			}
			// Sonars are single-use, so advance the quest by one step only
			QuestDatabase.advanceQuest( Quest.BAT );
			return;

		case ItemPool.SPRING_BEACH_CHARTER:
			Preferences.setBoolean( "sleazeAirportAlways", true );
			return;

		case ItemPool.SPRING_BEACH_TICKET:
			Preferences.setBoolean( "_sleazeAirportToday", true );
			return;

		case ItemPool.SPOOKYRAVEN_TELEGRAM:
			QuestDatabase.setQuestIfBetter( Quest.SPOOKYRAVEN_NECKLACE, QuestDatabase.STARTED );
			return;

		case ItemPool.WORSE_HOMES_GARDENS:
			if ( QuestDatabase.isQuestLaterThan( Preferences.getString( Quest.HIPPY.getPref() ), "step1" ) )
			{
				return;
			}
			QuestDatabase.setQuestProgress( Quest.HIPPY, "step2" );
			return;

		case ItemPool.MERKIN_WORDQUIZ:
			matcher = MERKIN_WORDQUIZ_PATTERN.matcher( responseText );
			if ( matcher.find() )
			{
				Preferences.setInteger( "merkinVocabularyMastery", StringUtilities.parseInt( matcher.group(1) ) );
				ResultProcessor.removeItem( ItemPool.MERKIN_CHEATSHEET );
			}
			else
			{
				// Otherwise, it is not consumed
				ResultProcessor.processResult( item );
			}
			return;

		case ItemPool.DREADSCROLL:

			// Only two things remove the Mer-kin dreadscroll from inventory:
			// successfully becoming the Mer-kin High Priest, and attempting
			// to read it when you are already the Champion of the Arena.
			//
			// I guess you're the Mer-kin High Priest now. Cool!
			//
			// The sigil burned into your forehead (you know, the one you got
			// for winning the tournament in the Colosseum?) flares up in
			// intense pain as you look at the dreadscroll. A tendril of fire
			// curls out and burns the scroll to ashes.

			if ( responseText.contains( "I guess you're the Mer-kin High Priest now" ) )
			{
				Preferences.setString( "merkinQuestPath", "scholar" );
				return;
			}

			if ( !Preferences.getString( "merkinQuestPath" ).equals( "done" ) &&
			     responseText.contains( "The sigil burned into your forehead" ) )
			{
				Preferences.setString( "merkinQuestPath", "gladiator" );
				Preferences.setInteger( "lastColosseumRoundWon", 15 );
				return;
			}

			// Otherwise, it is not consumed
			ResultProcessor.processResult( item );
			return;

		case ItemPool.MERKIN_STASHBOX:
			ResultProcessor.removeItem( ItemPool.MERKIN_LOCKKEY );
			return;

		case ItemPool.MERKIN_KNUCKLEBONE:
			DreadScrollManager.handleKnucklebone( responseText );
			return;

		case ItemPool.DEFECTIVE_TOKEN:
			Preferences.setBoolean( "_defectiveTokenUsed", true );
			return;

		case ItemPool.SILVER_DREAD_FLASK:
			Preferences.setBoolean( "_silverDreadFlaskUsed", true );
			return;

		case ItemPool.BRASS_DREAD_FLASK:
			Preferences.setBoolean( "_brassDreadFlaskUsed", true );
			return;
			
		case ItemPool.MARK_OF_THE_BUGBEAR:
		case ItemPool.MARK_OF_THE_WEREWOLF:
		case ItemPool.MARK_OF_THE_ZOMBIE:
		case ItemPool.MARK_OF_THE_GHOST:
		case ItemPool.MARK_OF_THE_VAMPIRE:
		case ItemPool.MARK_OF_THE_SKELETON:
			if ( !responseText.contains( "You have unlocked a new tattoo" ) )
			{
				ResultProcessor.processResult( item );
			}
			return;

		case ItemPool.DREADSYLVANIAN_ALMANAC:
			// You've already learned everything you can learn from these things.
			if ( responseText.contains( "You've already learned everything" ) )
			{
				ResultProcessor.processResult( item );
			}
			return;

		case ItemPool.BOOK_OF_MATCHES:
			// If Hidden Tavern not already unlocked, new items available
			if ( Preferences.getInteger( "hiddenTavernUnlock" ) != KoLCharacter.getAscensions() )
			{
				// Unlock Hidden Tavern
				Preferences.setInteger( "hiddenTavernUnlock", KoLCharacter.getAscensions() );
				ConcoctionDatabase.setRefreshNeeded( true );
			}
			return;

		case ItemPool.ETERNAL_CAR_BATTERY:
			Preferences.setBoolean( "_eternalCarBatteryUsed", true );
			return;

		case ItemPool.SKELETON:
			// Put this back in inventory, since we detect its loss
			// via the choice adventure.
			ResultProcessor.processResult( item );
			return;

		case ItemPool.DESERT_PAMPHLET:
			QuestManager.incrementDesertExploration( 15 );
			return;

		case ItemPool.MODELING_CLAYMORE:
			// You bury the claymore in the clay (what a coincidence!)
			// in the middle of the battlefield on the Mysterious
			// Island of Mystery. Then you hide behind a tree and
			// watch as it goes off, covering dozens of hippies and
			// frat boys with globs of wet clay, and forcing them
			// to head home and take showers.
			// 
			// Well, the frat boys leave to take showers. The
			// hippies just leave because they're lazy.
			if ( responseText.contains( "You bury the claymore in the clay" ) )
			{
				// Look at the island map and make our best
				// effort to synch up the kill count
				RequestThread.postRequest( new IslandRequest() );
				return;
			}

			// Otherwise, it is not consumed
			ResultProcessor.processResult( item );
			return;
			
		case ItemPool.BLUE_LINT:
		case ItemPool.GREEN_LINT:
		case ItemPool.WHITE_LINT:
		case ItemPool.ORANGE_LINT:
			if ( responseText.contains( "very improbable thing happens" ) )
			{
				// Remove lint
				ResultProcessor.processItem( ItemPool.BLUE_LINT, -1 );
				ResultProcessor.processItem( ItemPool.GREEN_LINT, -1 );
				ResultProcessor.processItem( ItemPool.WHITE_LINT, -1 );
				ResultProcessor.processItem( ItemPool.ORANGE_LINT, -1 );
			}
			return;

		case ItemPool.SPIRIT_PILLOW:
		case ItemPool.SPIRIT_SHEET:
		case ItemPool.SPIRIT_MATTRESS:
		case ItemPool.SPIRIT_BLANKET:
			if ( responseText.contains( "spirit bed" ) )
			{
				// Remove bedding
				ResultProcessor.processItem( ItemPool.SPIRIT_PILLOW, -1 );
				ResultProcessor.processItem( ItemPool.SPIRIT_SHEET, -1 );
				ResultProcessor.processItem( ItemPool.SPIRIT_MATTRESS, -1 );
				ResultProcessor.processItem( ItemPool.SPIRIT_BLANKET, -1 );
			}
			return;

		case ItemPool.PASTA_ADDITIVE:
			Preferences.setBoolean( "_pastaAdditive", true );
			return;

		case ItemPool.WARBEAR_BREAKFAST_MACHINE:
			Preferences.setBoolean( "_warbearBreakfastMachineUsed", true );
			return;

		case ItemPool.WARBEAR_SODA_MACHINE:
			Preferences.setBoolean( "_warbearSodaMachineUsed", true );
			return;

		case ItemPool.WARBEAR_GYROCOPTER_BROKEN:
			Preferences.setBoolean( "_warbearGyrocopterUsed", true );
			return;

		case ItemPool.WARBEAR_BANK:
			// You don't have 25 Meat to drop into the bank.
			if ( !responseText.contains( "don't have" ) )
			{
				Preferences.setBoolean( "_warbearBankUsed", true );
			}
			return;

		case ItemPool.LUPINE_APPETITE_HORMONES:
			Preferences.setBoolean( "_lupineHormonesUsed", true );
			return;

		case ItemPool.CORRUPTED_STARDUST:
		case ItemPool.PIXEL_ORB:
			Preferences.setBoolean( "_corruptedStardustUsed", true );
			return;

		case ItemPool.JARLSBERG_SOUL_FRAGMENT:
			if ( responseText.contains( "extra skill point" ) )
			{
				Preferences.increment( "jarlsbergPoints" );
			}
			return;

		case ItemPool.SNEAKY_PETE_SHOT:
			if ( responseText.contains( "extra skill point" ) )
			{
				Preferences.increment( "sneakyPetePoints" );
			}
			return;

		case ItemPool.ESSENCE_OF_ANNOYANCE:
			if ( responseText.contains( "You quaff" ) )
			{
				Preferences.decrement( "summonAnnoyanceCost", 1 );
			}
			return;

		case ItemPool.SWEET_TOOTH:
			if ( responseText.contains( "You pop the sweet" ) || responseText.contains( "You already had" ) )
			{
				Preferences.setBoolean( "_sweetToothUsed", true );
			}
			return;
		}

		if ( CampgroundRequest.isWorkshedItem( itemId ) )
		{
			Preferences.setBoolean( "_workshedItemUsed", true );
			if ( responseText.contains( "already rearranged your workshed" ) )
			{
				ResultProcessor.processResult( item );
				return;
			}
			CampgroundRequest.setCurrentWorkshedItem( ItemPool.get( itemId, 1 ) );
			CampgroundRequest.setCampgroundItem( itemId, 1 );
			return;
		}
	}

	private static final String itemToSkill( final int itemId )
	{
		switch ( itemId )
		{
		case ItemPool.SNOWCONE_BOOK:
			return "Summon Snowcones";
		case ItemPool.STICKER_BOOK:
			return "Summon Stickers";
		case ItemPool.SUGAR_BOOK:
			return "Summon Sugar Sheets";
		case ItemPool.CLIP_ART_BOOK:
			return "Summon Clip Art";
		case ItemPool.RAD_LIB_BOOK:
			return "Summon Rad Libs";
		case ItemPool.SMITH_BOOK:
			return "Summon Smithsness";
		case ItemPool.HILARIOUS_BOOK:
			return "Summon Hilarious Objects";
		case ItemPool.TASTEFUL_BOOK:
			return "Summon Tasteful Items";
		case ItemPool.CARD_GAME_BOOK:
			return "Summon Alice's Army Cards";
		case ItemPool.GEEKY_BOOK:
			return "Summon Geeky Gifts";
		case ItemPool.CANDY_BOOK:
			return "Summon Candy Hearts";
		case ItemPool.DIVINE_BOOK:
			return "Summon Party Favor";
		case ItemPool.LOVE_BOOK:
			return "Summon Love Song";
		case ItemPool.BRICKO_BOOK:
			return "Summon BRICKOs";
		case ItemPool.DICE_BOOK:
			return "Summon Dice";
		case ItemPool.RESOLUTION_BOOK:
			return "Summon Resolutions";
		case ItemPool.TAFFY_BOOK:
			return "Summon Taffy";
		case ItemPool.JEWELRY_BOOK:
			return "Really Expensive Jewelrycrafting";
		case ItemPool.OLFACTION_BOOK:
			return "Transcendent Olfaction";
		case ItemPool.RAINBOWS_GRAVITY:
			return "Rainbow Gravitation";
		case ItemPool.RAGE_GLAND:
			return "Vent Rage Gland";
		case ItemPool.KISSIN_COUSINS:
			return "Awesome Balls of Fire";
		case ItemPool.TALES_FROM_THE_FIRESIDE:
			return "Conjure Relaxing Campfire";
		case ItemPool.BLIZZARDS_I_HAVE_DIED_IN:
			return "Snowclone";
		case ItemPool.MAXING_RELAXING:
			return "Maximum Chill";
		case ItemPool.BIDDY_CRACKERS_COOKBOOK:
			return "Eggsplosion";
		case ItemPool.TRAVELS_WITH_JERRY:
			return "Mudbath";
		case ItemPool.LET_ME_BE:
			return "Raise Backup Dancer";
		case ItemPool.ASLEEP_IN_THE_CEMETERY:
			return "Creepy Lullaby";
		case ItemPool.SUMMER_NIGHTS:
			return "Grease Lightning";
		case ItemPool.SENSUAL_MASSAGE_FOR_CREEPS:
			return "Inappropriate Backrub";
		case ItemPool.RICHIE_THINGFINDER:
			return "The Ballad of Richie Thingfinder";
		case ItemPool.MEDLEY_OF_DIVERSITY:
			return "Benetton's Medley of Diversity";
		case ItemPool.EXPLOSIVE_ETUDE:
			return "Elron's Explosive Etude";
		case ItemPool.CHORALE_OF_COMPANIONSHIP:
			return "Chorale of Companionship";
		case ItemPool.PRELUDE_OF_PRECISION:
			return "Prelude of Precision";
		case ItemPool.HODGMAN_JOURNAL_1:
			return "Natural Born Scrabbler";
		case ItemPool.HODGMAN_JOURNAL_2:
			return "Thrift and Grift";
		case ItemPool.HODGMAN_JOURNAL_3:
			return "Abs of Tin";
		case ItemPool.HODGMAN_JOURNAL_4:
			return "Marginally Insane";
		case ItemPool.SLIME_SOAKED_HYPOPHYSIS:
			return "Slimy Sinews";
		case ItemPool.SLIME_SOAKED_BRAIN:
			return "Slimy Synapses";
		case ItemPool.SLIME_SOAKED_SWEAT_GLAND:
			return "Slimy Shoulders";
		case ItemPool.CRIMBO_CAROL_V1:
		case ItemPool.CRIMBO_CAROL_V1_USED:
			return "Holiday Weight Gain";
		case ItemPool.CRIMBO_CAROL_V2:
		case ItemPool.CRIMBO_CAROL_V2_USED:
			return "Jingle Bells";
		case ItemPool.CRIMBO_CAROL_V3:
		case ItemPool.CRIMBO_CAROL_V3_USED:
			return "Candyblast";
		case ItemPool.CRIMBO_CAROL_V4:
		case ItemPool.CRIMBO_CAROL_V4_USED:
			return "Surge of Icing";
		case ItemPool.CRIMBO_CAROL_V5:
		case ItemPool.CRIMBO_CAROL_V5_USED:
			return "Stealth Mistletoe";
		case ItemPool.CRIMBO_CAROL_V6:
		case ItemPool.CRIMBO_CAROL_V6_USED:
			return "Cringle's Curative Carol";
		case ItemPool.CRIMBO_CANDY_COOKBOOK:
			return "Summon Crimbo Candy";
		case ItemPool.SLAPFIGHTING_BOOK:
		case ItemPool.SLAPFIGHTING_BOOK_USED:
			return "Iron Palm Technique";
		case ItemPool.UNCLE_ROMULUS:
		case ItemPool.UNCLE_ROMULUS_USED:
			return "Curiosity of Br'er Tarrypin";
		case ItemPool.SNAKE_CHARMING_BOOK:
		case ItemPool.SNAKE_CHARMING_BOOK_USED:
			return "Stringozzi Serpent";
		case ItemPool.ZU_MANNKASE_DIENEN:
		case ItemPool.ZU_MANNKASE_DIENEN_USED:
			return "K&auml;seso&szlig;esturm";
		case ItemPool.DYNAMITE_SUPERMAN_JONES:
		case ItemPool.DYNAMITE_SUPERMAN_JONES_USED:
			return "Kung Fu Hustler";
		case ItemPool.INIGO_BOOK:
		case ItemPool.INIGO_BOOK_USED:
			return "Inigo's Incantation of Inspiration";
		case ItemPool.BLACK_HYMNAL:
			return "Canticle of Carboloading";
		case ItemPool.ELLSBURY_BOOK:
		case ItemPool.ELLSBURY_BOOK_USED:
			return "Unaccompanied Miner";
		case ItemPool.UNEARTHED_METEOROID:
			return "Volcanometeor Showeruption";
		case ItemPool.KANSAS_TOYMAKER:
		case ItemPool.KANSAS_TOYMAKER_USED:
			return "Toynado";
		case ItemPool.WASSAILING_BOOK:
		case ItemPool.WASSAILING_BOOK_USED:
			return "Wassail";
		case ItemPool.CRIMBCO_MANUAL_1:
		case ItemPool.CRIMBCO_MANUAL_1_USED:
			return "Fashionably Late";
		case ItemPool.CRIMBCO_MANUAL_2:
		case ItemPool.CRIMBCO_MANUAL_2_USED:
			return "Executive Narcolepsy";
		case ItemPool.CRIMBCO_MANUAL_3:
		case ItemPool.CRIMBCO_MANUAL_3_USED:
			return "Lunch Break";
		case ItemPool.CRIMBCO_MANUAL_4:
		case ItemPool.CRIMBCO_MANUAL_4_USED:
			return "Offensive Joke";
		case ItemPool.CRIMBCO_MANUAL_5:
		case ItemPool.CRIMBCO_MANUAL_5_USED:
			return "Managerial Manipulation";
		case ItemPool.SKELETON_BOOK:
		case ItemPool.SKELETON_BOOK_USED:
			return "Natural Born Skeleton Killer";
		case ItemPool.NECBRONOMICON:
		case ItemPool.NECBRONOMICON_USED:
			return "Summon &quot;Boner Battalion&quot;";
		case ItemPool.PLANT_BOOK:
			return "Torment Plant";
		case ItemPool.GHOST_BOOK:
			return "Pinch Ghost";
		case ItemPool.TATTLE_BOOK:
			return "Tattle";
		case ItemPool.NOTE_FROM_CLANCY:
			return "Request Sandwich";
		case ItemPool.GRUDGE_BOOK:
			return "Chip on your Shoulder";
		case ItemPool.JERK_BOOK:
			return "Thick-Skinned";
		case ItemPool.HJODOR_GUIDE:
			return "Frigidalmatian";
		case ItemPool.INFURIATING_SILENCE_RECORD:
		case ItemPool.INFURIATING_SILENCE_RECORD_USED:
			return "Silent Slam";
		case ItemPool.TRANQUIL_SILENCE_RECORD:
		case ItemPool.TRANQUIL_SILENCE_RECORD_USED:
			return "Silent Squirt";
		case ItemPool.MENACING_SILENCE_RECORD:
		case ItemPool.MENACING_SILENCE_RECORD_USED:
			return "Silent Slice";
		case ItemPool.WALBERG_BOOK:
			return "Walberg's Dim Bulb";
		case ItemPool.OCELOT_BOOK:
			return "Singer's Faithful Ocelot";
		case ItemPool.DRESCHER_BOOK:
			return "Drescher's Annoying Noise";
		case ItemPool.DECODED_CULT_DOCUMENTS:
			return "Bind Spaghetti Elemental";
		case ItemPool.WARBEAR_METALWORKING_PRIMER:
		case ItemPool.WARBEAR_METALWORKING_PRIMER_USED:
			return "Shrap";
		case ItemPool.WARBEAR_EMPATHY_CHIP:
		case ItemPool.WARBEAR_EMPATHY_CHIP_USED:
			return "Psychokinetic Hug";
		case ItemPool.OFFENSIVE_JOKE_BOOK:
			return "Unoffendable";
		case ItemPool.COOKING_WITH_GREASE_BOOK:
			return "Grease Up";
		case ItemPool.DINER_HANDBOOK:
			return "Sloppy Secrets";
		case ItemPool.ALIEN_SOURCE_CODE:
		case ItemPool.ALIEN_SOURCE_CODE_USED:
			return "Alien Source Code";
		}

		return null;
	}

	private static final String itemToClass( final int itemId )
	{
		switch ( itemId )
		{
		case ItemPool.SLAPFIGHTING_BOOK:
		case ItemPool.SLAPFIGHTING_BOOK_USED:
			return KoLCharacter.SEAL_CLUBBER;
		case ItemPool.UNCLE_ROMULUS:
		case ItemPool.UNCLE_ROMULUS_USED:
			return KoLCharacter.TURTLE_TAMER;
		case ItemPool.SNAKE_CHARMING_BOOK:
		case ItemPool.SNAKE_CHARMING_BOOK_USED:
			return KoLCharacter.PASTAMANCER;
		case ItemPool.ZU_MANNKASE_DIENEN:
		case ItemPool.ZU_MANNKASE_DIENEN_USED:
			return KoLCharacter.SAUCEROR;
		case ItemPool.DYNAMITE_SUPERMAN_JONES:
		case ItemPool.DYNAMITE_SUPERMAN_JONES_USED:
			return KoLCharacter.DISCO_BANDIT;
		case ItemPool.INIGO_BOOK:
		case ItemPool.INIGO_BOOK_USED:
			return KoLCharacter.ACCORDION_THIEF;
		}

		return null;
	}

	private static final void getEvilLevels( final String responseText )
	{
		int total = 0;
		int alcove = 0;
		int cranny = 0;
		int niche = 0;
		int nook = 0;

		Matcher matcher = EVILOMETER_PATTERN.matcher( responseText );
		if ( matcher.find() )
		{
			total = StringUtilities.parseInt( matcher.group( 1 ) );
			alcove = StringUtilities.parseInt( matcher.group( 2 ) );
			cranny = StringUtilities.parseInt( matcher.group( 3 ) );
			niche = StringUtilities.parseInt( matcher.group( 4 ) );
			nook = StringUtilities.parseInt( matcher.group( 5 ) );
		}

		Preferences.setInteger( "cyrptTotalEvilness", total );
		Preferences.setInteger( "cyrptAlcoveEvilness", alcove );
		Preferences.setInteger( "cyrptCrannyEvilness", cranny );
		Preferences.setInteger( "cyrptNicheEvilness", niche );
		Preferences.setInteger( "cyrptNookEvilness", nook );
		
		if ( responseText.contains( "give it a proper burial" ) )
		{
			ResultProcessor.removeItem( ItemPool.EVILOMETER );
		}
	}

	private static final void getBugbearBiodataLevels( String responseText )
	{
		Matcher matcher = UseItemRequest.KEYOTRON_PATTERN.matcher( responseText );

		if ( !matcher.find() )
		{
			return;
		}

		for ( int i = 1; i <= 9; ++i )
		{
			Object [] data = BugbearManager.idToData( i );
			if ( data == null )
			{
				continue;
			}
			BugbearManager.setBiodata( data, matcher.group( i ) );
		}

		return;
	}

	private static final void showItemUsage( final boolean showHTML, final String text )
	{
		if ( showHTML )
		{
			KoLmafia.showHTML(
				"inventory.php?action=message", UseItemRequest.trimInventoryText( text ) );
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

	public static final AdventureResult extractItem( final String urlString )
	{
		if ( !urlString.startsWith( "inv_use.php" ) &&
		     !urlString.startsWith( "inv_eat.php" ) &&
		     !urlString.startsWith( "inv_booze.php" ) &&
		     !urlString.startsWith( "multiuse.php" ) &&
		     !urlString.startsWith( "inv_familiar.php" ) &&
		     !(urlString.startsWith( "inventory.php" ) &&
		       urlString.indexOf( "action=ghost" ) == -1 &&
		       urlString.indexOf( "action=hobo" ) == -1 &&
		       urlString.indexOf( "action=slime" ) == -1 &&
		       urlString.indexOf( "action=breakbricko" ) == -1 &&
		       urlString.indexOf( "action=candy" ) == -1 ) )
		{
			return null;
		}

		return UseItemRequest.extractItem( urlString, true );
	}

	public static final AdventureResult extractItem( final String urlString, final boolean force )
	{
		Matcher itemMatcher = GenericRequest.WHICHITEM_PATTERN.matcher( urlString );
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

		if ( urlString.indexOf( "multiuse.php" ) != -1 ||
		     urlString.indexOf( "inv_eat.php" ) != -1 ||
		     urlString.indexOf( "inv_booze.php" ) != -1 ||
		     urlString.indexOf( "inv_use.php" ) != -1)
		{
			Matcher quantityMatcher = GenericRequest.QUANTITY_PATTERN.matcher( urlString );
			if ( quantityMatcher.find() )
			{
				itemCount = StringUtilities.parseInt( quantityMatcher.group( 1 ) );
			}
		}

		return new AdventureResult( itemId, itemCount );
	}

	public static final AdventureResult extractBingedItem( final String urlString )
	{
		if ( !urlString.startsWith( "inventory.php" ) &&
		     !urlString.startsWith( "familiarbinger.php" ) )
		{
			return null;
		}

		Matcher itemMatcher = GenericRequest.WHICHITEM_PATTERN.matcher( urlString );
		if ( !itemMatcher.find() )
		{
			return null;
		}

		int itemId = StringUtilities.parseInt( itemMatcher.group( 1 ) );
		int itemCount = 1;

		Matcher quantityMatcher = GenericRequest.QTY_PATTERN.matcher( urlString );
		if ( quantityMatcher.find() )
		{
			itemCount = StringUtilities.parseInt( quantityMatcher.group( 1 ) );
		}

		return new AdventureResult( itemId, itemCount );
	}

	private static final AdventureResult extractHelper( final String urlString )
	{
		if ( !urlString.startsWith( "inv_eat.php" ) &&
		     !urlString.startsWith( "inv_booze.php" ) &&
		     !urlString.startsWith( "inv_use.php" ) )
		{
			return null;
		}

		return UseItemRequest.extractHelper( urlString, true );
	}

	public static final AdventureResult extractHelper( final String urlString, final boolean force )
	{
		Matcher helperMatcher = UseItemRequest.HELPER_PATTERN.matcher( urlString );
		if ( !helperMatcher.find() )
		{
			return null;
		}

		int itemId = StringUtilities.parseInt( helperMatcher.group( 2 ) );
		if ( ItemDatabase.getItemName( itemId ) == null )
		{
			return null;
		}

		return new AdventureResult( itemId, 1 );
	}

	public static final boolean registerBingeRequest( final String urlString )
	{
		AdventureResult item = UseItemRequest.extractBingedItem( urlString );
		if ( item == null )
		{
			return false;
		}

		FamiliarData familiar = KoLCharacter.getFamiliar();
		int id = familiar.getId();

		if ( id != FamiliarPool.HOBO &&
		     id != FamiliarPool.GHOST &&
		     id != FamiliarPool.SLIMELING &&
		     id != FamiliarPool.STOCKING_MIMIC )
		{
			return false;
		}

		int count = item.getCount();
		int itemId = item.getItemId();
		String name = item.getName();
		String useString = "feed " + count + " " + name + " to " + familiar.getRace();

		if ( id == FamiliarPool.SLIMELING )
		{
			if ( itemId == ItemPool.GNOLLISH_AUTOPLUNGER ||
			     ConcoctionDatabase.meatStackCreation( name ) != null )
			{
				useString += " (" + count + " more slime stack(s) due)";
			}
			else
			{
				// round down for now, since we don't know how this really works
				float charges = item.getCount() * EquipmentDatabase.getPower( itemId ) / 10.0F;
				useString += " (estimated " + charges + " charges)";
			}
		}

		RequestLogger.updateSessionLog();
		RequestLogger.updateSessionLog( useString );

		return true;
	}

	public static boolean registerRequest( final String urlString )
	{
		// Don't overwrite lastItemUsed when restoratives are used from
		// the Skills page or quickskills menu.	 The request was
		// initially made to inv_use.php (and lastItemUsed was set at
		// that time), which redirects to skills.php - but without
		// enough information in the URL to determine exactly what was used.

		if ( urlString.startsWith( "skills.php" ) )
		{
			return true;
		}

		// If we are transferring to or from the closet from the
		// inventory, we are not "using" the item

		if ( urlString.indexOf( "action=closetpull" ) != -1 ||
		     urlString.indexOf( "action=closetpush" ) != -1 )
		{
			return ClosetRequest.registerRequest( urlString );
		}

		AdventureResult item = UseItemRequest.extractItem( urlString );

		if ( item != null && item.getItemId() == ItemPool.CARD_SLEEVE )
		{
			return EquipmentRequest.registerCardSleeve( urlString );
		}

		// Special handing for twisting Boris's Helm when it is equipped
		if ( item == null && urlString.contains( "action=twisthorns" ) )
		{
			int slot = -1;
			if ( urlString.contains( "slot=hat" ) )
			{
				slot = EquipmentManager.HAT;
			}
			else if ( urlString.contains( "slot=familiarequip" ) )
			{
				slot = EquipmentManager.FAMILIAR;
			}
			if ( slot != -1 )
			{
				AdventureResult before = EquipmentManager.getEquipment( slot );
				AdventureResult after = ItemPool.get( before.getItemId() == ItemPool.BORIS_HELM ? ItemPool.BORIS_HELM_ASKEW : ItemPool.BORIS_HELM, 1 );
				EquipmentManager.setEquipment( slot, after );
				RequestLogger.printLine( "Twisted " + before + " into " + after );
				return true;
			}
			return false;
		}

		// Special handing for shaking Jarlsberg's pan when it is equipped
		if ( item == null && urlString.contains( "action=shakepan" ) )
		{
			AdventureResult before = EquipmentManager.getEquipment( EquipmentManager.OFFHAND );
			AdventureResult after = ItemPool.get( before.getItemId() == ItemPool.JARLS_PAN ? ItemPool.JARLS_COSMIC_PAN : ItemPool.JARLS_PAN, 1 );
			EquipmentManager.setEquipment( EquipmentManager.OFFHAND, after );
			RequestLogger.printLine( "Shook " + before + " into " + after );
			return true;
		}

		// Special handing for shaking Sneaky Pete's leather jacket when it is equipped
		if ( item == null && urlString.contains( "action=popcollar" ) )
		{
			AdventureResult before = EquipmentManager.getEquipment( EquipmentManager.SHIRT );
			AdventureResult after = ItemPool.get( before.getItemId() == ItemPool.PETE_JACKET ? ItemPool.PETE_JACKET_COLLAR : ItemPool.PETE_JACKET, 1 );
			EquipmentManager.setEquipment( EquipmentManager.SHIRT, after );
			RequestLogger.printLine( "Popped " + before + " into " + after );
			return true;
		}

		if ( item == null )
		{
			return UseItemRequest.registerBingeRequest( urlString );
		}

		// Delegate to specialized classes as appropriate

		int itemId = item.getItemId();

		switch ( itemId )
		{
		case ItemPool.AWOL_COMMENDATION:
			return AWOLQuartermasterRequest.registerRequest( urlString );

		case ItemPool.BURT:
			return BURTRequest.registerRequest( urlString );

		case ItemPool.FDKOL_COMMENDATION:
			return FDKOLRequest.registerRequest( urlString, false );

		case ItemPool.FUDGE_WAND:
			return FudgeWandRequest.registerRequest( urlString );
		}

		// Everything below here will work with the item we extracted

		UseItemRequest.lastItemUsed = item;
		UseItemRequest.currentItemId = itemId;
		UseItemRequest.lastHelperUsed = UseItemRequest.extractHelper( urlString );

		if ( urlString.startsWith( "inv_booze.php" ) )
		{
			return DrinkItemRequest.registerRequest();
		}

		if ( urlString.startsWith( "inv_eat.php" ) )
		{
			return EatItemRequest.registerRequest();
		}

		String name = item.getName();

		if ( ItemDatabase.getSpleenHit( name ) > 0 )
		{
			return SpleenItemRequest.registerRequest();
		}

		int count = item.getCount();
		int consumptionType = ItemDatabase.getConsumptionType( itemId );
		String useString = null;

		switch ( consumptionType )
		{
		case KoLConstants.NO_CONSUME:
			if ( itemId != ItemPool.LOATHING_LEGION_JACKHAMMER )
			{
				return false;
			}
			break;

		case KoLConstants.CONSUME_USE:
		case KoLConstants.CONSUME_MULTIPLE:

			// See if it is a concoction
			if ( SingleUseRequest.registerRequest( urlString ) ||
			     MultiUseRequest.registerRequest( urlString ) )
			{
				return true;
			}
			break;

		case KoLConstants.CONSUME_EAT:

			// Fortune cookies, for example
			if ( urlString.startsWith( "inv_use" ) )
			{
				consumptionType = KoLConstants.CONSUME_USE;
				break;
			}
			break;
		}

		switch ( itemId )
		{
		case ItemPool.REFLECTION_OF_MAP:
			useString = "[" + KoLAdventure.getAdventureCount() + "] Reflection of a Map";
			KoLAdventure.locationLogged = true;
			break;

		case ItemPool.JACKING_MAP:
			UseItemRequest.lastFruit = null;
			Matcher m = UseItemRequest.FRUIT_TUBING_PATTERN.matcher( urlString );
			if ( m.find() )
			{
				UseItemRequest.lastFruit = ItemPool.get(
					StringUtilities.parseInt( m.group( 1 ) ), 1 );
				useString = "insert " + UseItemRequest.lastFruit + " into pneumatic tube interface";
			}
			break;

		case ItemPool.EXPRESS_CARD:
			Preferences.setBoolean( "expressCardUsed", true );
			break;

		case ItemPool.SPICE_MELANGE:
			Preferences.setBoolean( "spiceMelangeUsed", true );
			break;

		case ItemPool.ULTRA_MEGA_SOUR_BALL:
			Preferences.setBoolean( "_ultraMegaSourBallUsed", true );
			break;

		case ItemPool.MUNCHIES_PILL:
			Preferences.increment( "munchiesPillsUsed", count );
			break;

		case ItemPool.DRINK_ME_POTION:
			Preferences.increment( "pendingMapReflections", count );
			break;

		case ItemPool.BLACK_MARKET_MAP: {

			// In some paths, you can't use a blackbird,
			// but you must have the hatchling in your inventory.
			if ( KoLCharacter.inAxecore() || KoLCharacter.isJarlsberg() || KoLCharacter.isSneakyPete() )
			{
				if ( !InventoryManager.retrieveItem( ItemPool.REASSEMBLED_BLACKBIRD ) )
				{
					return true;
				}

				// We are good to go.
				break;
			}

			int needed = KoLCharacter.inBeecore() ?
				FamiliarPool.CROW : FamiliarPool.BLACKBIRD;
			int hatchling = needed == FamiliarPool.CROW ?
				ItemPool.RECONSTITUTED_CROW : ItemPool.REASSEMBLED_BLACKBIRD;

			if ( KoLCharacter.getFamiliar().getId() != needed )
			{
				AdventureResult map = UseItemRequest.lastItemUsed;

				// Get the player's current blackbird.
				FamiliarData blackbird = KoLCharacter.findFamiliar( needed );

				// If there is no blackbird in the terrarium,
				// grow one from the hatchling.
				if ( blackbird == null )
				{
					( UseItemRequest.getInstance( ItemPool.get( hatchling, 1 ) ) ).run();
					UseItemRequest.lastItemUsed = map;
					blackbird = KoLCharacter.findFamiliar( needed );
				}

				// If still couldn't find it, bail
				if ( blackbird == null )
				{
					return true;
				}
				FamiliarData familiar = KoLCharacter.getFamiliar();
				Preferences.setString( "preBlackbirdFamiliar", familiar.getRace() );
				// Take the blackbird out of the terrarium
				( new FamiliarRequest( blackbird ) ).run();
			}
			break;
		}

		case ItemPool.EL_VIBRATO_HELMET:
		case ItemPool.DRONE:
			if ( UseItemRequest.lastHelperUsed == null )
			{
				return true;
			}
			useString = "insert " + UseItemRequest.lastHelperUsed + " into " + UseItemRequest.lastItemUsed;
			break;

		case ItemPool.PUNCHCARD_ATTACK:
		case ItemPool.PUNCHCARD_REPAIR:
		case ItemPool.PUNCHCARD_BUFF:
		case ItemPool.PUNCHCARD_MODIFY:
		case ItemPool.PUNCHCARD_BUILD:
			if ( KoLCharacter.getFamiliar().getId() != FamiliarPool.MEGADRONE )
			{
				return true;
			}
			useString = "insert " + UseItemRequest.lastItemUsed + " into El Vibrato Megadrone";
			break;

		case ItemPool.WRETCHED_SEAL:
		case ItemPool.CUTE_BABY_SEAL:
		case ItemPool.ARMORED_SEAL:
		case ItemPool.ANCIENT_SEAL:
		case ItemPool.SLEEK_SEAL:
		case ItemPool.SHADOWY_SEAL:
		case ItemPool.STINKING_SEAL:
		case ItemPool.CHARRED_SEAL:
		case ItemPool.COLD_SEAL:
		case ItemPool.SLIPPERY_SEAL:
		case ItemPool.DEPLETED_URANIUM_SEAL:
			// You only actually use a seal figurine when you
			// "Begin the Ritual"
			if ( urlString.indexOf( "checked" ) != -1 )
			{
				return true;
			}
			break;

		case ItemPool.LOATHING_LEGION_KNIFE:
		case ItemPool.LOATHING_LEGION_MANY_PURPOSE_HOOK:
		case ItemPool.LOATHING_LEGION_MOONDIAL:
		case ItemPool.LOATHING_LEGION_NECKTIE:
		case ItemPool.LOATHING_LEGION_ELECTRIC_KNIFE:
		case ItemPool.LOATHING_LEGION_CORKSCREW:
		case ItemPool.LOATHING_LEGION_CAN_OPENER:
		case ItemPool.LOATHING_LEGION_CHAINSAW:
		case ItemPool.LOATHING_LEGION_ROLLERBLADES:
		case ItemPool.LOATHING_LEGION_FLAMETHROWER:
		case ItemPool.LOATHING_LEGION_DEFIBRILLATOR:
		case ItemPool.LOATHING_LEGION_DOUBLE_PRISM:
		case ItemPool.LOATHING_LEGION_TAPE_MEASURE:
		case ItemPool.LOATHING_LEGION_KITCHEN_SINK:
		case ItemPool.LOATHING_LEGION_ABACUS:
		case ItemPool.LOATHING_LEGION_HELICOPTER:
		case ItemPool.LOATHING_LEGION_PIZZA_STONE:
		case ItemPool.LOATHING_LEGION_JACKHAMMER:
		case ItemPool.LOATHING_LEGION_HAMMER:
			// inv_use.php?whichitem=xxx&pwd&switch=1
			// Attempting to use one of these items takes you to a
			// page where you can select the new form.
			// inv_use.php?whichitem=xxx&pwd&switch=1&eq=0&fold=yyy
			// You only lose the item when you switch form.
			if ( urlString.indexOf( "fold" ) == -1 )
			{
				return true;
			}
			useString = "fold " + UseItemRequest.lastItemUsed;
			break;

		case ItemPool.LOATHING_LEGION_UNIVERSAL_SCREWDRIVER:
			// You can either use the unversal screwdriver to
			// untinker something or switch forms.
			// inv_use.php?whichitem=4926&pwd&action=screw&dowhichitem=xxx

			if ( urlString.indexOf( "action=screw" ) != -1 )
			{
				Matcher matcher = UseItemRequest.DOWHICHITEM_PATTERN.matcher( urlString );
				if ( !matcher.find() )
				{
					UseItemRequest.lastUntinker = null;
					return true;
				}

				int uid = StringUtilities.parseInt( matcher.group( 1 ) );
				AdventureResult untinker = ItemPool.get( uid, 1 );
				String countStr = "1";

				if ( urlString.indexOf( "untinkerall=on" ) != -1 )
				{
					untinker = ItemPool.get( uid, untinker.getCount( KoLConstants.inventory ) );
					countStr = "*";
				}

				UseItemRequest.lastUntinker = untinker;
				useString = "unscrew " + countStr + " " + untinker.getName();
				break;
			}

			if ( urlString.indexOf( "fold" ) == -1 )
			{
				return true;
			}
			useString = "fold " + UseItemRequest.lastItemUsed;
			break;

		case ItemPool.LOATHING_LEGION_TATTOO_NEEDLE:
			// You can either "use" the reusable tattoo needle or
			// switch forms.
			if ( urlString.indexOf( "switch" ) != -1 )
			{
				if ( urlString.indexOf( "fold" ) == -1 )
				{
					return true;
				}
				useString = "fold " + UseItemRequest.lastItemUsed;
			}
			break;

		case ItemPool.D10:
			if ( count == 2 )
			{
				useString = "roll percentile dice";
				break;
			}
			// Fall through
		case ItemPool.D4:
		case ItemPool.D6:
		case ItemPool.D8:
		case ItemPool.D12:
		case ItemPool.D20:
			useString = "roll " + count + name;
			break;

		case ItemPool.WHAT_CARD:
		case ItemPool.WHEN_CARD:
		case ItemPool.WHO_CARD:
		case ItemPool.WHERE_CARD:
			// Unless you are looking for the answer, using a card
			// simply presents you with the question.
			if ( urlString.indexOf( "answerplz=1" ) == -1 )
			{
				return true;
			}
			break;
		}

		if ( useString == null )
		{
			useString = "use " + count + " " + name ;
		}

		RequestLogger.updateSessionLog();
		RequestLogger.updateSessionLog( useString );
		return true;
	}

	@Override
	public int getAdventuresUsed()
	{
		return UseItemRequest.getAdventuresUsedByItem( this.itemUsed );
	}

	public static int getAdventuresUsed( final String urlString )
	{
		AdventureResult item = UseItemRequest.extractItem( urlString );
		return item == null ? 0 :  UseItemRequest.getAdventuresUsedByItem( item );
	}

	public static int getAdventuresUsedByItem( AdventureResult item )
	{
		int turns = 0;
		switch ( item.getItemId() )
		{
		case ItemPool.ABYSSAL_BATTLE_PLANS:
		case ItemPool.BLACK_PUDDING:
		case ItemPool.CARONCH_MAP:
		case ItemPool.CRUDE_SCULPTURE:
		case ItemPool.CURSED_PIECE_OF_THIRTEEN:
		case ItemPool.DOLPHIN_WHISTLE:
		case ItemPool.ENVYFISH_EGG:
		case ItemPool.FRATHOUSE_BLUEPRINTS:
		case ItemPool.ICE_SCULPTURE:
		case ItemPool.PHOTOCOPIED_MONSTER:
		case ItemPool.RAIN_DOH_MONSTER:
		case ItemPool.SHAKING_CAMERA:
		case ItemPool.SHAKING_CRAPPY_CAMERA:
		case ItemPool.SHAKING_SKULL:
		case ItemPool.SPOOKY_PUTTY_MONSTER:
		case ItemPool.WAX_BUGBEAR:
			// Items that can redirect to a fight
			turns = 1;
			break;

		case ItemPool.D4:
			turns = item.getCount() == 100 ? 1 : 0;
			break;

		case ItemPool.D10:
			// 1d10 gives you a monster
			// 2d10 gives you a random result and takes a turn
			turns = ( item.getCount() == 1 || item.getCount() == 2 ) ? 1 : 0;
			break;

		case ItemPool.REFLECTION_OF_MAP:
		case ItemPool.RONALD_SHELTER_MAP:
		case ItemPool.GRIMACE_SHELTER_MAP:
		case ItemPool.STAFF_GUIDE:
			// Items that can redirect to a choice adventure
			turns = 1;
			break;

		case ItemPool.DRUM_MACHINE:
			// Drum machine doesn't take a turn if you have
			// worm-riding hooks in inventory or equipped.
			AdventureResult hooks = ItemPool.get( ItemPool.WORM_RIDING_HOOKS, 1 );
			if ( KoLConstants.inventory.contains( hooks ) || KoLCharacter.hasEquipped( hooks, EquipmentManager.WEAPON ) )
			{
				return 0;
			}
			turns = 1;
			break;

		case ItemPool.GONG:
			// Roachform is three uninterruptible turns
			turns = Preferences.getInteger( "choiceAdventure276" ) == 1 ? 3 : 0;
			break;
		}

		return turns * item.getCount();
	}

	static private void parseEVHelmet( String responseText )
	{
		List pieces = Arrays.asList( responseText.split( "(<.*?>)+" ) );
		int start = pieces.indexOf( "KROKRO LAZAK FULA:" );
		if ( start == -1 )
		{
			start = pieces.indexOf( "SPEAR POWER CONDUIT:" );
		}
		if ( start == -1 || pieces.size() - start < 20 )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR,
				"Unable to parse conduit levels: " + responseText );
			return;
		}

		int data = 0;
		// We have 9 conduits, with a value from 0 to 10 each.
		// A 9-digit base-11 number won't quite fit in an int, but not all
		// values are possible - the total of all the conduits cannot be
		// larger than 10 itself.  The largest possible value is therefore
		// A00000000(11), which is just slightly under 2**31.
		for ( int i = 0; i < 9; ++i )
		{
			String piece = (String) pieces.get( start + i * 2 + 1 );
			int value = ItemPool.EV_HELMET_LEVELS.indexOf( piece );
			if ( value == -1 )
			{
				KoLmafia.updateDisplay( MafiaState.ERROR,
					"Unable to parse conduit level: " + piece );
				return;
			}
			data = data * 11 + value / 2;
		}
		Preferences.setInteger( "lastEVHelmetValue", data );
		Preferences.setInteger( "lastEVHelmetReset", KoLCharacter.getAscensions() );
	}
}
