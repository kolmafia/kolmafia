/**
 * Copyright (c) 2005-2012, KoLmafia development team
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
import java.util.HashMap;
import java.util.List;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.FamiliarData;
import net.sourceforge.kolmafia.KoLAdventure;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.KoLmafiaCLI;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.SpecialOutfit;
import net.sourceforge.kolmafia.StaticEntity;

import net.sourceforge.kolmafia.moods.HPRestoreItemList;
import net.sourceforge.kolmafia.moods.MPRestoreItemList;
import net.sourceforge.kolmafia.moods.ManaBurnManager;
import net.sourceforge.kolmafia.moods.RecoveryManager;

import net.sourceforge.kolmafia.objectpool.AdventurePool;
import net.sourceforge.kolmafia.objectpool.EffectPool;
import net.sourceforge.kolmafia.objectpool.FamiliarPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;

import net.sourceforge.kolmafia.persistence.AdventureDatabase;
import net.sourceforge.kolmafia.persistence.ConcoctionDatabase;
import net.sourceforge.kolmafia.persistence.EquipmentDatabase;
import net.sourceforge.kolmafia.persistence.FamiliarDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.persistence.MonsterDatabase;
import net.sourceforge.kolmafia.persistence.SkillDatabase;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.session.EquipmentManager;
import net.sourceforge.kolmafia.session.InventoryManager;
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

	private static final GenericRequest REDIRECT_REQUEST = new GenericRequest( "inventory.php?action=message" );

	public static final Pattern ITEMID_PATTERN = Pattern.compile( "whichitem=(\\d+)" );
	public static final Pattern QUANTITY_PATTERN = Pattern.compile( "quantity=(\\d+)" );
	public static final Pattern QTY_PATTERN = Pattern.compile( "qty=(\\d+)" );
	public static final Pattern DOWHICHITEM_PATTERN = Pattern.compile( "dowhichitem=(\\d+)" );

	private static final Pattern ROW_PATTERN = Pattern.compile( "<tr>.*?</tr>" );
	private static final Pattern INVENTORY_PATTERN = Pattern.compile( "</blockquote></td></tr></table>.*?</body>" );
	private static final Pattern HELPER_PATTERN = Pattern.compile( "(utensil|whichcard)=(\\d+)" );
	private static final Pattern FORTUNE_PATTERN =
		Pattern.compile( "<font size=1>(Lucky numbers: (\\d+), (\\d+), (\\d+))</td>" );
	private static final Pattern FAMILIAR_NAME_PATTERN =
		Pattern.compile( "You decide to name (?:.*?) <b>(.*?)</b>" );
	private static final Pattern FRUIT_TUBING_PATTERN =
		Pattern.compile( "(?=.*?action=addfruit).*whichfruit=(\\d+)" );
	private static final Pattern ADVENTUROUS_RESOLUTION_PATTERN =
		Pattern.compile( "resolve to do it again" );

	// It goes [Xd12] feet, and doesn't hit anything interesting.
	private static final Pattern ARROW_PATTERN =
		Pattern.compile( "It goes (\\d+) feet" );

	// You pull out the trivia card (#57/1200) and read it.
	private static final Pattern CARD_PATTERN = Pattern.compile( "You pull out the trivia card \\(#(\\d+)/(\\d+)\\) and read it." );

	// Question:  ...?
	private static final Pattern QA_PATTERN = Pattern.compile( "(Question|Answer): *([^<]*)<" );

	// <center>Total evil: <b>200</b><p>Alcove: <b>50</b><br>Cranny: <b>50</b><br>Niche: <b>50</b><br>Nook: <b>50</b></center>
	private static final Pattern EVILOMETER_PATTERN =
		Pattern.compile( "<center>Total evil: <b>(\\d+)</b><p>Alcove: <b>(\\d+)</b><br>Cranny: <b>(\\d+)</b><br>Niche: <b>(\\d+)</b><br>Nook: <b>(\\d+)</b></center>" );

	private static final HashMap LIMITED_USES = new HashMap();

	static
	{
		UseItemRequest.LIMITED_USES.put( new Integer( ItemPool.ASTRAL_MUSHROOM ), EffectPool.get( EffectPool.HALF_ASTRAL ) );

		UseItemRequest.LIMITED_USES.put( new Integer( ItemPool.ABSINTHE ), EffectPool.get( EffectPool.ABSINTHE ) );

		UseItemRequest.LIMITED_USES.put( new Integer( ItemPool.TURTLE_PHEROMONES ), EffectPool.get( EffectPool.EAU_DE_TORTUE ) );
	}

	public static String lastUpdate = "";
	public static String limiter = "";
	private static AdventureResult lastItemUsed = null;
	private static AdventureResult lastHelperUsed = null;
	private static AdventureResult lastFruit = null;
	private static AdventureResult lastUntinker = null;
	private static boolean lastLook = false;
	private static int askedAboutOde = 0;
	private static int askedAboutMilk = 0;
	private static int ignoreMilkPrompt = 0;
	private static int permittedOverdrink = 0;
	private static AdventureResult queuedFoodHelper = null;
	private static int queuedFoodHelperCount = 0;
	private static AdventureResult queuedDrinkHelper = null;
	private static int queuedDrinkHelperCount = 0;
	private static boolean retrying = false;

	private final int consumptionType;
	private AdventureResult itemUsed = null;

	public UseItemRequest( final AdventureResult item )
	{
		this( UseItemRequest.getConsumptionType( item ), item );
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

	public UseItemRequest( final int consumptionType, final AdventureResult item )
	{
		this( UseItemRequest.getConsumptionLocation( consumptionType, item ), consumptionType, item );
	}

	public static void setLastItemUsed( final AdventureResult item )
	{
		UseItemRequest.lastItemUsed = item;
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
		case KoLConstants.CONSUME_MULTIPLE:
		case KoLConstants.MP_RESTORE:
		case KoLConstants.HPMP_RESTORE:
			return "multiuse.php";
		case KoLConstants.CONSUME_SPHERE:
			return "campground.php";
		case KoLConstants.HP_RESTORE:
			if ( item.getCount() > 1 )
			{
				return "multiuse.php";
			}
			return "inv_use.php";
		case  KoLConstants.INFINITE_USES:
		{
			int type = ItemDatabase.getConsumptionType( item.getItemId() );
			return type == KoLConstants.CONSUME_MULTIPLE ?
				"multiuse.php" : "inv_use.php";
		}
		default:
			return "inv_use.php";
		}
	}

	private UseItemRequest( final String location, final int consumptionType, final AdventureResult item )
	{
		super( location );

		this.consumptionType = consumptionType;
		this.itemUsed = item;

		if ( UseItemRequest.needsConfirmation( item ) )
		{
			this.addFormField( "confirm", "true" );
		}

		this.addFormField( "whichitem", String.valueOf( item.getItemId() ) );
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
			return CampgroundRequest.getCurrentDwelling() != CampgroundRequest.BIG_ROCK;

		case ItemPool.HOT_BEDDING:
		case ItemPool.COLD_BEDDING:
		case ItemPool.STENCH_BEDDING:
		case ItemPool.SPOOKY_BEDDING:
		case ItemPool.SLEAZE_BEDDING:
		case ItemPool.BEANBAG_CHAIR:
		case ItemPool.GAUZE_HAMMOCK:
			return CampgroundRequest.getCurrentBed() != null;

		case ItemPool.MACARONI_FRAGMENTS:
		case ItemPool.SHIMMERING_TENDRILS:
		case ItemPool.SCINTILLATING_POWDER:
		case ItemPool.PARANORMAL_RICOTTA:
		case ItemPool.SMOKING_TALON:
		case ItemPool.VAMPIRE_GLITTER:
		case ItemPool.WINE_SOAKED_BONE_CHIPS:
		case ItemPool.CRUMBLING_RAT_SKULL:
		case ItemPool.TWITCHING_TRIGGER_FINGER:
		case ItemPool.DECODED_CULT_DOCUMENTS:
			String ghost = Preferences.getString( "pastamancerGhostType" );
			return !ghost.equals( "" );
		}

		return false;
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
		return UseItemRequest.maximumUses( itemId, KoLConstants.NO_CONSUME );
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
		String itemName = ItemDatabase.getItemName( itemId );
		return UseItemRequest.maximumUses( itemId, itemName, consumptionType, allowOverDrink );
	}

	public static final int maximumUses( final String itemName )
	{
		return UseItemRequest.maximumUses( itemName, false );
	}

	public static final int maximumUses( final String itemName, final boolean allowOverDrink )
	{
		int itemId = ItemDatabase.getItemId( itemName );
		return UseItemRequest.maximumUses( itemId, itemName, KoLConstants.NO_CONSUME, allowOverDrink );
	}

	private static final int maximumUses( final int itemId, final String itemName, final int consumptionType, final boolean allowOverDrink )
	{
		// Don't bother limiting this since we won't display use links
		// until the fight is all over anyway.
		if ( false && FightRequest.getCurrentRound() != 0 )
		{
			UseItemRequest.limiter = "fight in progress";
			return 0;
		}

		if ( FightRequest.inMultiFight() )
		{
			UseItemRequest.limiter = "multi-stage fight in progress";
			return 0;
		}

		if ( !GenericRequest.choiceHandled )
		{
			UseItemRequest.limiter = "choice adventure in progress";
			return 0;
		}

		// Set reasonable default if the item fails to set a specific reason
		UseItemRequest.limiter = "a wizard";
		
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

		int fullness = ItemDatabase.getFullness( itemName );
		if ( fullness > 0 )
		{
			UseItemRequest.limiter = "fullness";
			return ( KoLCharacter.getFullnessLimit() - KoLCharacter.getFullness() + ( Preferences
				.getBoolean( "distentionPillActive" ) ? 1 : 0 ) ) / fullness;
		}

		int inebriety = ItemDatabase.getInebriety( itemName );
		if ( inebriety > 0 )
		{
			UseItemRequest.limiter = "inebriety";
			int limit = KoLCharacter.getInebrietyLimit();

			// Green Beer allows drinking to limit + 10,
			// but only on SSPD. For now, always allow

			if ( itemId == ItemPool.GREEN_BEER )
			{
				limit += 10;
			}

			int inebrietyLeft = limit - KoLCharacter.getInebriety();

			if ( inebrietyLeft < 0 )
			{
				// We are already drunk
				return 0;
			}

			if ( inebrietyLeft < inebriety )
			{
				// One drink will make us drunk
				return 1;
			}

			if ( allowOverDrink )
			{
				// Multiple drinks will make us drunk
				return inebrietyLeft / inebriety + 1;
			}

			// Multiple drinks to not quite make us drunk
			return inebrietyLeft / inebriety;
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

			UseItemRequest.limiter = "needed restoration";
			if ( spleenHit > 0 )
			{
				UseItemRequest.limiter = "needed restoration or spleen";
				maximumSuggested =
					Math.min(
						maximumSuggested, ( KoLCharacter.getSpleenLimit() - KoLCharacter.getSpleenUse() ) / spleenHit );
			}

			return maximumSuggested;
		}

		if ( spleenHit > 0 )
		{
			UseItemRequest.limiter = "spleen";
			return ( KoLCharacter.getSpleenLimit() - KoLCharacter.getSpleenUse() ) / spleenHit;
		}

		if ( itemId <= 0 )
		{
			return Integer.MAX_VALUE;
		}

		switch ( itemId )
		{
		case ItemPool.BLACK_MARKET_MAP:
		case ItemPool.BALL_POLISH:
		case ItemPool.FRATHOUSE_BLUEPRINTS:
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
			// Campground equipment
		case ItemPool.BEANBAG_CHAIR:
		case ItemPool.GAUZE_HAMMOCK:
		case ItemPool.HOT_BEDDING:
		case ItemPool.COLD_BEDDING:
		case ItemPool.STENCH_BEDDING:
		case ItemPool.SPOOKY_BEDDING:
		case ItemPool.SLEAZE_BEDDING:
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
			boolean stomachAvailable = ( KoLCharacter.getFullnessLimit() - KoLCharacter.getFullness() ) > 0;

			// The distention pill is not usable when you're full.
			// Even if you plan on eating a 1-full food.
			// UPDATE: distention pill now usable when full.  Also, does not reset at rollover.
			/*if ( !stomachAvailable )
			{
				UseItemRequest.limiter = "remaining fullness";
				return 0;
			}*/
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
					EffectPool.get( EffectPool.HAUNTING_LOOKS_ID ) ) ||
				 KoLConstants.activeEffects.contains(
					EffectPool.get( EffectPool.DEAD_SEXY_ID ) ) ||
				 KoLConstants.activeEffects.contains(
					EffectPool.get( EffectPool.VAMPIN_ID ) ) ||
				 KoLConstants.activeEffects.contains(
					EffectPool.get( EffectPool.YIFFABLE_YOU_ID ) ) ||
				 KoLConstants.activeEffects.contains(
					EffectPool.get( EffectPool.BONE_US_ROUND_ID ) ) )
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
			return Preferences.getBoolean( "_jackassePlumberGame" ) ? 0 : 1;

		case ItemPool.TRIVIAL_AVOCATIONS_GAME:
			UseItemRequest.limiter = "daily limit";
			return Preferences.getBoolean( "_trivialAvocationsGame" ) ? 0 : 1;

		case ItemPool.RESOLUTION_ADVENTUROUS:
			UseItemRequest.limiter = "daily limit";
			return ( Preferences.getInteger( "_resolutionAdv" ) == 10 ? 0 : Integer.MAX_VALUE );
		}

		switch ( consumptionType )
		{
		case KoLConstants.GROW_FAMILIAR:
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

		Integer key = new Integer( itemId );

		if ( UseItemRequest.LIMITED_USES.containsKey( key ) )
		{
			UseItemRequest.limiter = "unstackable effect";
			return KoLConstants.activeEffects.contains( UseItemRequest.LIMITED_USES.get( key ) ) ? 0 : 1;
		}

		return Integer.MAX_VALUE;
	}

	public void run()
	{
		// Hide memento items from your familiars
		if ( this.isBingeRequest() &&
		     Preferences.getBoolean( "mementoListActive" ) &&
		     KoLConstants.mementoList.contains( this.itemUsed ) )
		{
			KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "Don't feed mementos to your familiars." );
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
			KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "You really should wield a club before using that." );
			return;
		}

		switch ( itemId )
		{
		case ItemPool.BRICKO_SWORD:
		case ItemPool.BRICKO_HAT:
		case ItemPool.BRICKO_PANTS:
			if ( !InventoryManager.retrieveItem( this.itemUsed ) )
			{
				KoLmafia.updateDisplay( KoLConstants.ERROR_STATE,
					"You don't have one of those." );
				return;
			}
			KoLmafia.updateDisplay( "Splitting bricks..." );
			GenericRequest req = new GenericRequest( "inventory.php?action=breakbricko&pwd&whichitem=" + itemId );
			RequestThread.postRequest( req );
			ResponseTextParser.externalUpdate( req.getURLString(), req.responseText );
			return;

		case ItemPool.STICKER_SWORD:
		case ItemPool.STICKER_CROSSBOW:
			if ( !InventoryManager.retrieveItem( this.itemUsed ) )
			{
				KoLmafia.updateDisplay( KoLConstants.ERROR_STATE,
					"You don't have one of those." );
				return;
			}
			RequestThread.postRequest( new GenericRequest( "bedazzle.php?action=fold&pwd" ) );
			return;

		case ItemPool.MACGUFFIN_DIARY:
			RequestThread.postRequest( new GenericRequest( "diary.php?textversion=1" ) );
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
		case ItemPool.BEANBAG_CHAIR:
		case ItemPool.GAUZE_HAMMOCK:
			AdventureResult bed = CampgroundRequest.getCurrentBed();
			if ( bed != null && !UseItemRequest.confirmReplacement( bed.getName() ) )
			{
				return;
			}
			break;

		case ItemPool.MACARONI_FRAGMENTS:
		case ItemPool.SHIMMERING_TENDRILS:
		case ItemPool.SCINTILLATING_POWDER:
		case ItemPool.PARANORMAL_RICOTTA:
		case ItemPool.SMOKING_TALON:
		case ItemPool.VAMPIRE_GLITTER:
		case ItemPool.WINE_SOAKED_BONE_CHIPS:
		case ItemPool.CRUMBLING_RAT_SKULL:
		case ItemPool.TWITCHING_TRIGGER_FINGER:
		case ItemPool.DECODED_CULT_DOCUMENTS:

			String ghost = Preferences.getString( "pastamancerGhostType" );
			if ( !ghost.equals( "" ) && !UseItemRequest.confirmReplacement( ghost ) )
			{
				return;
			}
			break;
		}

		int count;

		switch ( this.consumptionType )
		{
		case KoLConstants.CONSUME_STICKER:
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

		case KoLConstants.CONSUME_FOOD_HELPER:
			count = this.itemUsed.getCount();
			if ( !InventoryManager.retrieveItem( this.itemUsed ) )
			{
				KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "Helper not available." );
				return;
			}
			if ( this.itemUsed.equals( this.queuedFoodHelper ) )
			{
				queuedFoodHelperCount += count;
			}
			else
			{
				this.queuedFoodHelper = this.itemUsed;
				this.queuedFoodHelperCount = count;
			}
			KoLmafia.updateDisplay( "Helper queued for next " + count + " food" +
				(count == 1 ? "" : "s") + " eaten." );
			return;

		case KoLConstants.CONSUME_DRINK_HELPER:
			count = this.itemUsed.getCount();
			if ( !InventoryManager.retrieveItem( this.itemUsed ) )
			{
				KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "Helper not available." );
				return;
			}
			if ( this.itemUsed.equals( queuedDrinkHelper ) )
			{
				queuedDrinkHelperCount += count;
			}
			else
			{
				queuedDrinkHelper = this.itemUsed;
				queuedDrinkHelperCount = count;
			}
			KoLmafia.updateDisplay( "Helper queued for next " + count + " beverage" +
				(count == 1 ? "" : "s") + " drunk." );
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
			KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, UseItemRequest.lastUpdate );
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
			case KoLConstants.CONSUME_DRINK:
			case KoLConstants.CONSUME_EAT:
				// The miracle of "consume some" does not apply
				// to TPS drinks or black puddings
				if ( !UseItemRequest.singleConsume( itemId, this.consumptionType ) &&
					(!UseItemRequest.sequentialConsume( itemId ) ||
						InventoryManager.getCount( itemId ) >= origCount) )
				{
					break;
				}
				// Fall through.
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

		String useTypeAsString =
			this.consumptionType == KoLConstants.CONSUME_EAT ? "Eating" : this.consumptionType == KoLConstants.CONSUME_DRINK ? "Drinking" : "Using";

		String originalURLString = this.getURLString();

		for ( int i = 1; i <= iterations && KoLmafia.permitsContinue(); ++i )
		{
			this.constructURLString( originalURLString );

			if ( this.consumptionType == KoLConstants.CONSUME_DRINK &&
			     !this.allowBoozeConsumption() )
			{
				return;
			}

			if ( this.consumptionType == KoLConstants.CONSUME_EAT &&
			     !this.allowFoodConsumption() )
			{
				return;
			}

			this.useOnce( i, iterations, useTypeAsString );

			if ( itemId == ItemPool.YUMMY_TUMMY_BEAN )
			{	// the first iteration may have been short
				this.itemUsed = this.itemUsed.getInstance( 20 );
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
			KoLmafia.updateDisplay( "Finished " + useTypeAsString.toLowerCase() + " " + origCount + " " + this.itemUsed.getName() + "." );
		}
	}

	private static final boolean singleConsume( final int itemId, int consumptionType )
	{
		switch ( consumptionType )
		{	// Consume one at a time when a helper is involved.
			// Multi-consume with a helper actually DOES work, even though
			// there is no interface for doing so in game, but that's
			// probably not something that should be relied on.
		case KoLConstants.CONSUME_DRINK:
			if ( queuedDrinkHelper != null && queuedDrinkHelperCount > 0 )
			{
				return true;
			}
			break;
		case KoLConstants.CONSUME_EAT:
			if ( queuedFoodHelper != null && queuedFoodHelperCount > 0 )
			{
				return true;
			}
			break;
		}

		switch ( itemId )
		{
		case ItemPool.BLACK_PUDDING:
			// Eating a black pudding can lead to a combat with no
			// feedback about how many were successfully eaten
			// before the combat.
			return true;
		}
		return false;
	}

	private static final boolean sequentialConsume( final int itemId )
	{
		switch (itemId )
		{
		case ItemPool.DIRTY_MARTINI:
		case ItemPool.GROGTINI:
		case ItemPool.CHERRY_BOMB:
		case ItemPool.VESPER:
		case ItemPool.BODYSLAM:
		case ItemPool.SANGRIA_DEL_DIABLO:
			// Allow player who owns a single tiny plastic sword to
			// make and drink multiple drinks in succession.
		case ItemPool.BORIS_PIE:
		case ItemPool.JARLSBERG_PIE:
		case ItemPool.SNEAKY_PETE_PIE:
			// Likewise, allow multiple pies to be made and eaten
			// with only one key.
			return true;
		}
		return false;
	}

	public static final void ignoreMilkPrompt()
	{
		UseItemRequest.ignoreMilkPrompt = KoLCharacter.getUserId();
	}

	public static final void permitOverdrink()
	{
		UseItemRequest.permittedOverdrink = KoLCharacter.getUserId();
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

	private final boolean allowBoozeConsumption()
	{
		// Always allow the steel margarita
		int itemId = this.itemUsed.getItemId();
		if ( itemId == ItemPool.STEEL_LIVER )
		{
			return true;
		}

		int inebriety = ItemDatabase.getInebriety( this.itemUsed.getName() );
		int count = this.itemUsed.getCount();

		return UseItemRequest.allowBoozeConsumption( inebriety, count );
	}

	public static final boolean allowBoozeConsumption( final int inebriety, final int count )
	{
		int inebrietyBonus = inebriety * count;
		if ( inebrietyBonus < 1 )
		{
			return true;
		}

		if ( KoLCharacter.isFallingDown() )
		{
			return true;
		}

		if ( !GenericFrame.instanceExists() )
		{
			return true;
		}

		if ( !UseItemRequest.askAboutOde( inebriety, count ) )
		{
			return false;
		}

		// Make sure the player does not overdrink if they still
		// have PvP attacks remaining.

		if ( KoLCharacter.getInebriety() + inebrietyBonus > KoLCharacter.getInebrietyLimit()
			&& UseItemRequest.permittedOverdrink != KoLCharacter.getUserId() )
		{
			if ( KoLCharacter.getAttacksLeft() > 0 && !InputFieldUtilities.confirm( "Are you sure you want to overdrink without PvPing?" ) )
			{
				return false;
			}

			if ( KoLCharacter.getAdventuresLeft() > 0 && !InputFieldUtilities.confirm( "Are you sure you want to overdrink?" ) )
			{
				return false;
			}
		}

		return true;
	}

	private static final boolean askAboutOde( final int inebriety, final int count )
	{
		// If we've already asked about ode, don't nag
		if ( UseItemRequest.askedAboutOde == KoLCharacter.getUserId() )
		{
			return true;
		}

		// If user specifically said not to worry about ode, don't nag
		// Actually, this overloads the "allowed to overdrink" flag.
		if ( UseItemRequest.permittedOverdrink == KoLCharacter.getUserId() )
		{
			return true;
		}

		// See if already have enough turns of Ode to Booze
		int odeTurns = ItemDatabase.ODE.getCount( KoLConstants.activeEffects );
		int consumptionTurns = count * inebriety;

		if ( consumptionTurns <= odeTurns )
		{
			return true;
		}

		// If the character doesn't know ode, there is nothing to do.
		UseSkillRequest ode = UseSkillRequest.getInstance( "The Ode to Booze" );
		boolean canOde = KoLConstants.availableSkills.contains( ode ) &&
			UseSkillRequest.hasAccordion();

		if ( !canOde )
		{
			return true;
		}

		// Cast Ode automatically if you have enough mana,
		// when you are out of Ronin/HC
		int odeCost = SkillDatabase.getMPConsumptionById( 6014 );
		while ( KoLCharacter.canInteract() &&
			odeTurns < consumptionTurns &&
			KoLCharacter.getCurrentMP() >= odeCost &&
			KoLmafia.permitsContinue() )
		{
			ode.setBuffCount( 1 );
			RequestThread.postRequest( ode );
			int newTurns = ItemDatabase.ODE.getCount( KoLConstants.activeEffects );
			if ( odeTurns == newTurns )
			{
				// No progress
				break;
			}
			odeTurns = newTurns;
		}

		if ( consumptionTurns <= odeTurns )
		{
			return true;
		}

		String message = odeTurns > 0 ?
			"The Ode to Booze will run out before you finish drinking that. Are you sure?" :
			"Are you sure you want to drink without ode?";
		if ( !InputFieldUtilities.confirm( message ) )
		{
			return false;
		}

		UseItemRequest.askedAboutOde = KoLCharacter.getUserId();

		return true;
	}

	private final boolean allowFoodConsumption()
	{
		if ( !GenericFrame.instanceExists() )
		{
			return true;
		}

		if ( !askAboutMilk() )
		{
			return false;
		}

		// If we are not a Pastamancer, that's good enough. If we are,
		// make sure the player isn't going to accidentally scuttle the
		// stupid Spaghettihose trophy.
		if ( KoLCharacter.getClassType() != KoLCharacter.PASTAMANCER )
		{
			return true;
		}

		// If carboLoading is 0, it doesn't matter what you eat.
		// If it's 1, this might be normal aftercore eating.
		// If it's 10, the character will qualify for the trophy
		int carboLoading = Preferences.getInteger( "carboLoading" );
		if ( carboLoading <= 1 || carboLoading >= 10 )
		{
			return true;
		}

		// If the food is not made with noodles, no fear
		if ( ConcoctionDatabase.noodleCreation( this.itemUsed.getName() ) == null )
		{
			return true;
		}

		// Nag
		if ( !InputFieldUtilities.confirm( "Eating pasta with only " + carboLoading + " levels of Carboloading will ruin your chance to get the Spaghettihose trophy. Are you sure?" ) )
		{
			return false;
		}

		return true;
	}

	private final boolean askAboutMilk()
	{
		// If we've already asked about milk, don't nag
		if ( UseItemRequest.askedAboutMilk == KoLCharacter.getUserId() )
		{
			return true;
		}

		// If user specifically said not to worry about milk, don't nag
		if ( UseItemRequest.ignoreMilkPrompt == KoLCharacter.getUserId() )
		{
			return true;
		}

		// See if already have enough of the Got Milk effect
		int milkyTurns = ItemDatabase.MILK.getCount( KoLConstants.activeEffects );
		String name = this.itemUsed.getName();
		int fullness = ItemDatabase.getFullness( name );
		int count = this.itemUsed.getCount();
		int consumptionTurns = count * fullness - ( Preferences.getBoolean( "distentionPillActive" ) ? 1 : 0 );

		if ( consumptionTurns <= milkyTurns )
		{
			return true;
		}

		// Has (or can create) a milk of magnesium.
		boolean canMilk = InventoryManager.hasItem( ItemPool.MILK_OF_MAGNESIUM, true) || KoLCharacter.canInteract();

		if ( canMilk )
		{
			String message = milkyTurns > 0 ?
				"Got Milk will run out before you finish eating that. Are you sure?" :
				"Are you sure you want to eat without milk?";
			if ( !InputFieldUtilities.confirm( message ) )
			{
				return false;
			}

			UseItemRequest.askedAboutMilk = KoLCharacter.getUserId();
		}

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
			RecoveryManager.runBetweenBattleChecks( true );
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
				this.addFormField( "ajax", "1" );
			}
			break;

		case KoLConstants.CONSUME_MULTIPLE:
		case KoLConstants.MP_RESTORE:
		case KoLConstants.HPMP_RESTORE:
			this.addFormField( "action", "useitem" );
			this.addFormField( "quantity", String.valueOf( this.itemUsed.getCount() ) );
			break;

		case KoLConstants.CONSUME_HOBO:
			if ( KoLCharacter.getFamiliar().getId() != FamiliarPool.HOBO )
			{
				KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "You don't have a Spirit Hobo equipped" );
				return;
			}
			this.queuedFoodHelper = null;
			this.addFormField( "action", "binge" );
			this.addFormField( "qty", String.valueOf( this.itemUsed.getCount() ) );
			useTypeAsString = "Boozing hobo with";
			break;

		case KoLConstants.CONSUME_GHOST:
			if ( KoLCharacter.getFamiliar().getId() != FamiliarPool.GHOST )
			{
				KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "You don't have a Gluttonous Green Ghost equipped" );
				return;
			}
			this.queuedDrinkHelper = null;
			this.addFormField( "action", "binge" );
			this.addFormField( "qty", String.valueOf( this.itemUsed.getCount() ) );
			useTypeAsString = "Feeding ghost with";
			break;

		case KoLConstants.CONSUME_SLIME:
			if ( KoLCharacter.getFamiliar().getId() != FamiliarPool.SLIMELING )
			{
				KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "You don't have a Slimeling equipped" );
				return;
			}
			this.addFormField( "action", "binge" );
			this.addFormField( "qty", String.valueOf( this.itemUsed.getCount() ) );
			useTypeAsString = "Feeding slimeling with";
			break;

		case KoLConstants.CONSUME_MIMIC:
			if ( KoLCharacter.getFamiliar().getId() != FamiliarPool.STOCKING_MIMIC )
			{
				KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "You don't have a Stocking Mimic equipped" );
				return;
			}
			this.addFormField( "action", "candy" );
			useTypeAsString = "Feeding stocking mimic with";
			break;

		case KoLConstants.CONSUME_EAT:
			this.addFormField( "which", "1" );
			this.addFormField( "quantity", String.valueOf( this.itemUsed.getCount() ) );
			if ( this.queuedFoodHelper != null && this.queuedFoodHelperCount > 0 )
			{
				if ( this.queuedFoodHelper.getItemId() == ItemPool.SCRATCHS_FORK )
				{
					UseItemRequest.lastUpdate = this.elementalHelper( "Hotform",
						MonsterDatabase.HEAT, 1000 );
					if ( !UseItemRequest.lastUpdate.equals( "" ) )
					{
						KoLmafia.updateDisplay( KoLConstants.ERROR_STATE,
							UseItemRequest.lastUpdate );
						this.queuedFoodHelper = null;
						return;
					}
				}
				this.addFormField( "utensil", String.valueOf( this.queuedFoodHelper.getItemId() ) );
				--this.queuedFoodHelperCount;
			}
			else
			{
				this.removeFormField( "utensil" );
			}
			break;

		case KoLConstants.CONSUME_DRINK:
			this.addFormField( "which", "1" );
			this.addFormField( "quantity", String.valueOf( this.itemUsed.getCount() ) );
			if ( queuedDrinkHelper != null && queuedDrinkHelperCount > 0 )
			{
				if ( this.queuedDrinkHelper.getItemId() == ItemPool.FROSTYS_MUG )
				{
					UseItemRequest.lastUpdate = this.elementalHelper( "Coldform",
						MonsterDatabase.COLD, 1000 );
					if ( !UseItemRequest.lastUpdate.equals( "" ) )
					{
						KoLmafia.updateDisplay( KoLConstants.ERROR_STATE,
							UseItemRequest.lastUpdate );
						this.queuedDrinkHelper = null;
						return;
					}
				}
				this.addFormField( "utensil", String.valueOf( queuedDrinkHelper.getItemId() ) );
				--queuedDrinkHelperCount;
			}
			else
			{
				this.removeFormField( "utensil" );
			}
			break;

		default:
			this.addFormField( "which", "3" );
			this.addFormField( "ajax", "1" );
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

		if ( this.responseCode == 302 )
		{
			if ( this.redirectLocation.startsWith( "inventory" ) )
			{
				UseItemRequest.REDIRECT_REQUEST.constructURLString( this.redirectLocation ).run();
				UseItemRequest.lastItemUsed = this.itemUsed;
				UseItemRequest.parseConsumption( UseItemRequest.REDIRECT_REQUEST.responseText, true );
				ResponseTextParser.learnRecipe( this.getURLString(), UseItemRequest.REDIRECT_REQUEST.responseText );
				ConcoctionDatabase.refreshConcoctions( false );
			}
			else if ( this.redirectLocation.startsWith( "choice.php" ) )
			{
				// The choice has already been handled by GenericRequest,
				// but we still need to account for the item used.
				UseItemRequest.parseConsumption( "", true );
			}
		}
	}

	private String elementalHelper( String remove, int resist, int amount )
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

		int healthNeeded = (int) Math.ceil(amount *
			(100.0f - KoLCharacter.getElementalResistance( resist )) / 100.0f);
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
				KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "Your current familiar can't use that." );
			}
			return;
		}

		UseItemRequest.lastItemUsed = this.itemUsed;
		UseItemRequest.parseConsumption( this.responseText, true );
		ResponseTextParser.learnRecipe( this.getURLString(), this.responseText );
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

		// If you are in Beecore, certain items can't B used
		// "You are too scared of Bs to xxx that item."
		if ( KoLCharacter.inBeecore() &&
		     responseText.indexOf( "You are too scared of Bs" ) != -1 )
		{
			UseItemRequest.lastUpdate = "You are too scared of Bs";
			KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, UseItemRequest.lastUpdate );
			String name = item.getName();
			int count = item.getCount();

			int fullness = ItemDatabase.getFullness( name ) * count;
			if ( fullness > 0 )
			{
				Preferences.increment( "currentFullness", -fullness );
			}

			int spleenHit = ItemDatabase.getSpleenHit( name ) * count;
			if ( spleenHit > 0 )
			{
				Preferences.increment( "currentSpleenUse", -spleenHit );
			}

			KoLCharacter.updateStatus();
			return;
		}

		if ( responseText.indexOf( "be at least level" ) != -1 )
		{
			UseItemRequest.lastUpdate = "Item level too high.";
			KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, UseItemRequest.lastUpdate );
			String name = item.getName();
			int count = item.getCount();

			int fullness = ItemDatabase.getFullness( name ) * count;
			if ( fullness > 0 )
			{
				Preferences.increment( "currentFullness", -fullness );
			}

			int spleenHit = ItemDatabase.getSpleenHit( name ) * count;
			if ( spleenHit > 0 )
			{
				Preferences.increment( "currentSpleenUse", -spleenHit );
			}

			KoLCharacter.updateStatus();
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
			case ItemPool.DIVINE_FLUTE:
				// "You pour the <drink> into your divine
				// champagne flute, and it immediately begins
				// fizzing over. You drink it quickly, then
				// throw the flute in front of a plastic
				// fireplace and break it."

				if ( responseText.indexOf( "a plastic fireplace" ) == -1 )
				{
					success = false;
				}
				break;

			case ItemPool.SCRATCHS_FORK:

				// "You eat the now piping-hot <food> -- it's
				// sizzlicious! The salad fork cools, and you
				// discard it."

				if ( responseText.indexOf( "The salad fork cools" ) == -1 )
				{
					success = false;
				}
				break;

			case ItemPool.FROSTYS_MUG:

				// "Brisk! Refreshing! You drink the frigid
				// <drink> and discard the no-longer-frosty
				// mug."

				if ( responseText.indexOf( "discard the no-longer-frosty" ) == -1 )
				{
					success = false;
				}
				break;

			case ItemPool.FUDGE_SPORK:

				// "You eat the <food> with your fudge spork,
				// and then you eat your fudge spork. How sweet it is!"

				if ( responseText.indexOf( "you eat your fudge spork" ) == -1 )
				{
					success = false;
				}
				break;

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
				KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, UseItemRequest.lastUpdate );
				return;
			}

			// Remove the consumption helper from inventory.
			ResultProcessor.processResult( helper.getNegation() );
		}

		if ( ConcoctionDatabase.singleUseCreation( item.getName() ) != null ||
		     ConcoctionDatabase.multiUseCreation( item.getName() ) != null )
		{
			// These all create things via "use" or "multiuse" of
			// an ingredient and perhaps consume other ingredients.
			// SingleUseRequest or MultiUseRequest removed all the
			// ingredients.

			if ( responseText.indexOf( "You acquire" ) != -1 )
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

			KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, UseItemRequest.lastUpdate );
			return;
		}

		int consumptionType = UseItemRequest.getConsumptionType( item );

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

			if ( responseText.indexOf( "you glance fearfully at the moons" ) != -1 )
			{
				UseItemRequest.lastUpdate = "Can't hatch that familiar in Bad Moon.";
				KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, UseItemRequest.lastUpdate );
				ResultProcessor.processResult( item );
				return;
			}

			if ( responseText.indexOf( "You don't have a Terrarium to put that in." ) != -1 )
			{
				UseItemRequest.lastUpdate = "You don't have a Terrarium yet.";
				KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, UseItemRequest.lastUpdate );
				ResultProcessor.processResult( item );
				return;
			}

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

		// You feel the canticle take hold, and feel suddenly bloated
		// as the pasta expands in your belly.
		if ( consumptionType == KoLConstants.CONSUME_EAT &&
		     KoLCharacter.getClassType() == KoLCharacter.PASTAMANCER &&
		     responseText.indexOf( "feel suddenly bloated" ) != -1 )
		{
			Preferences.setInteger( "carboLoading", 0 );
		}

		if ( responseText.indexOf( "That item isn't usable in quantity" ) != -1 )
		{
			KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "Internal data error: item incorrectly flagged as multi-usable." );
			ResultProcessor.processResult( item );
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
			UseItemRequest.lastUpdate = "Your spleen might go kablooie.";
			KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, UseItemRequest.lastUpdate );

			int spleenHit = ItemDatabase.getSpleenHit( item.getName() ) * item.getCount();

			// Roll back what we did to spleen in registerRequest
			Preferences.increment( "currentSpleenUse", -spleenHit );

			int estimatedSpleen = KoLCharacter.getSpleenLimit() - spleenHit + 1;

			if ( estimatedSpleen > KoLCharacter.getSpleenUse() )
			{
				Preferences.setInteger( "currentSpleenUse", estimatedSpleen );
			}

			ResultProcessor.processResult( item );
			KoLCharacter.updateStatus();

			return;
		}

		// If we ate a distention pill, the next thing we eat should
		// detect the extra message and decrement fullness by 1.
		if ( responseText.indexOf( "feel your stomach shrink" ) != -1 )
		{
			int fullness = ItemDatabase.getFullness( item.getName() );
			int count = item.getCount();

			// If we got this message, we definitely used a pill today.
			Preferences.setBoolean( "_distentionPillUsed", true );
			Preferences.setBoolean( "distentionPillActive", false );
			Preferences.increment( "currentFullness", -1 );
			String message = "Incrementing fullness by " + ( fullness * count - 1 )
					+ " instead of " + ( fullness * count )
					+ " because your stomach was distended.";
			RequestLogger.updateSessionLog( message );
			RequestLogger.printLine( message );
			KoLCharacter.updateStatus();
		}

		// If we eat a non-zero fullness item and we DON'T get the shrinking message, we must be out of sync
		// with KoL. Fix that.

		if ( ItemDatabase.getFullness( item.getName() ) > 0 && Preferences.getBoolean( "distentionPillActive" ) )
		{
			Preferences.setBoolean( "distentionPillActive", false );
		}

		// Check to make sure that it wasn't a food or drink
		// that was consumed that resulted in nothing. Eating
		// too much is flagged as a continuable state.

		// Note that there is at least one item (memory of amino acids)
		// that can fail with a "too full" message, even though it's
		// not a food.

		if ( responseText.indexOf( "too full" ) != -1 )
		{
			UseItemRequest.lastUpdate = "Consumption limit reached.";
			KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, UseItemRequest.lastUpdate );

			int fullness = ItemDatabase.getFullness( item.getName() );
			// If we have no fullness data for this item, we can't
			// tell what, if anything, consumption did to our
			// fullness.
			if ( fullness == 0 )
			{
				return;
			}

			// Roll back what we did to fullness in registerRequest
			int count = item.getCount();
			Preferences.increment( "currentFullness", -fullness * count );

			int maxFullness = KoLCharacter.getFullnessLimit();
			int currentFullness = KoLCharacter.getFullness();

			// Based on what we think our current fullness is,
			// calculate how many of this item we have room for.
			int maxEat = (maxFullness - currentFullness) / fullness;

			// We know that KoL did not let us eat as many as we
			// requested, so adjust for how many we could eat.
			int couldEat = Math.max( 0, Math.min( item.getCount() - 1, maxEat ) );
			if ( couldEat > 0 )
			{
				Preferences.increment( "currentFullness", couldEat * fullness );
			}

			int estimatedFullness = maxFullness - fullness + 1;

			if ( estimatedFullness > KoLCharacter.getFullness() )
			{
				Preferences.setInteger( "currentFullness", estimatedFullness );
			}

			ResultProcessor.processResult( item.getInstance( count - couldEat ) );
			KoLCharacter.updateStatus();

			return;
		}

		if ( responseText.indexOf( "too drunk" ) != -1 )
		{
			UseItemRequest.lastUpdate = "Inebriety limit reached.";
			KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, UseItemRequest.lastUpdate );
			ResultProcessor.processResult( item );
			return;
		}

		// Re-sort consumables list if needed
		switch ( consumptionType )
		{
		case KoLConstants.CONSUME_USE:
		case KoLConstants.CONSUME_MULTIPLE:
			if ( ItemDatabase.getSpleenHit( item.getName() ) == 0 )
			{
				break;
			}
			/*FALLTHRU*/
		case KoLConstants.CONSUME_EAT:
		case KoLConstants.CONSUME_DRINK:
			if ( Preferences.getBoolean( "sortByRoom" ) )
			{
				ConcoctionDatabase.getUsables().sort();
			}
		}

		Matcher matcher;

		// Perform item-specific processing

		switch ( item.getItemId() )
		{
		case ItemPool.LOATHING_LEGION_UNIVERSAL_SCREWDRIVER: {
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

		case ItemPool.CRYSTAL_ORB:
			if ( UseItemRequest.lastLook )
			{	// [look] rather than [use]
				return;
			}
			String oldType = Preferences.getString( "pastamancerGhostType" );
			String oldName = Preferences.getString( "pastamancerGhostName" );
			int oldExp = Preferences.getInteger( "pastamancerGhostExperience" );
			String newType = Preferences.getString( "pastamancerOrbedType" );
			String newName = Preferences.getString( "pastamancerOrbedName" );
			int newExp = Preferences.getInteger( "pastamancerOrbedExperience" );

			Preferences.setString( "pastamancerGhostType", newType );
			Preferences.setString( "pastamancerGhostName", newName );
			Preferences.setInteger( "pastamancerGhostExperience", newExp );
			Preferences.setString( "pastamancerOrbedType", oldType );
			Preferences.setString( "pastamancerOrbedName", oldName );
			Preferences.setInteger( "pastamancerOrbedExperience", oldExp );

			if ( oldType.equals( "" ) ) oldType = "nothing";
			if ( newType.equals( "" ) ) newType = "nothing";
			KoLmafia.updateDisplay( "Exchanged " + newType + " (now in use) with " + oldType + " (now in orb)." );
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
				KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, UseItemRequest.lastUpdate );
				ResultProcessor.processResult( item );
			}
			else if ( showHTML )
			{
				UseItemRequest.showItemUsage( true, responseText );
			}

			return;

		case ItemPool.DANCE_CARD:
			TurnCounter.stopCounting( "Dance Card" );
			TurnCounter.startCounting( 3, "Dance Card loc=109", "guildapp.gif" );
			return;

			// If it's a fortune cookie, get the fortune

		case ItemPool.FORTUNE_COOKIE:
		case ItemPool.QUANTUM_TACO:

			matcher = UseItemRequest.FORTUNE_PATTERN.matcher( responseText );
			while ( matcher.find() )
			{
				UseItemRequest.handleFortuneCookie( matcher );
			}

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
					(new UseItemRequest( item )).run();
				}
			}

			return;

		case ItemPool.HERMIT_SCRIPT:

			HermitRequest.ensureUpdatedHermit();
			Preferences.setBoolean( "hermitHax0red", true );
			HermitRequest.resetConcoctions();
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
				KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, UseItemRequest.lastUpdate );
				return;
			}

			// "You're already in the middle of a journey of
			// reincarnation."

			if ( responseText.indexOf( "middle of a journey of reincarnation" ) != -1 )
			{
				if ( UseItemRequest.retrying ||
					KoLConstants.activeEffects.contains(
						EffectPool.get( EffectPool.FORM_OF_BIRD ) ) ||
					KoLConstants.activeEffects.contains(
						EffectPool.get( EffectPool.SHAPE_OF_MOLE ) ) ||
					KoLConstants.activeEffects.contains(
						EffectPool.get( EffectPool.FORM_OF_ROACH ) ) )
				{
					UseItemRequest.lastUpdate = "You're still under a gong effect.";
					KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, UseItemRequest.lastUpdate );
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
					KoLAdventure req = AdventureDatabase.getAdventureByURL(
						"adventure.php?snarfblat=" + adv );
					req.overrideAdventuresUsed( 0 );	// don't trigger counters
					// Must do some trickery here to
					// prevent the adventure location from
					// being changed, and the conditions
					// reset.
					String la = Preferences.getString( "lastAdventure" );
					Preferences.setString( "lastAdventure",
						req.getAdventureName() );
					RequestThread.postRequest( req );
					req.overrideAdventuresUsed( -1 );
					Preferences.setString( "lastAdventure", la );
					(new UseItemRequest( item )).run();
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

			if ( responseText.indexOf( "grows into an enormous beanstalk" ) == -1 )
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
				KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, UseItemRequest.lastUpdate );
				ResultProcessor.processResult( item );
			}

			return;

		case ItemPool.GIANT_CASTLE_MAP:

			// "I'm sorry, adventurer, but the Sorceress is in
			// another castle!"

			if ( responseText.indexOf( "Sorceress is in another castle" ) == -1 )
			{
				UseItemRequest.lastUpdate = "You couldn't make it all the way to the back door.";
				KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, UseItemRequest.lastUpdate );
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
				RequestThread.postRequest( new CampgroundRequest() );
			}

			// The ketchup hound does not go away...

			ResultProcessor.processResult( item );
			return;

		case ItemPool.LUCIFER:

			// Jumbo Dr. Lucifer reduces your hit points to 1.

			ResultProcessor.processResult(
				new AdventureResult( AdventureResult.HP, 1 - KoLCharacter.getCurrentHP() ) );

			return;

		case ItemPool.DOLPHIN_KING_MAP:

			// "You follow the Dolphin King's map to the bottom of
			// the sea, and find his glorious treasure."

			if ( responseText.indexOf( "find his glorious treasure" ) == -1 )
			{
				UseItemRequest.lastUpdate = "You don't have everything you need.";
				KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, UseItemRequest.lastUpdate );
				ResultProcessor.processResult( item );
			}

			return;

		case ItemPool.SLUG_LORD_MAP:

			// "You make your way to the deepest part of the tank,
			// and find a chest engraved with the initials S. L."

			if ( responseText.indexOf( "deepest part of the tank" ) == -1 )
			{
				UseItemRequest.lastUpdate = "You don't have everything you need.";
				KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, UseItemRequest.lastUpdate );
				ResultProcessor.processResult( item );
			}

			return;

		case ItemPool.DR_HOBO_MAP:

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
				KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, UseItemRequest.lastUpdate );
				ResultProcessor.processResult( item );
				return;
			}

			// Using the map consumes the encryption key

			ResultProcessor.processItem( ItemPool.ENCRYPTION_KEY, -1 );
			return;

		case ItemPool.BLACK_MARKET_MAP:

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
			// or Reconstituted Crow familiar. When it works, the
			// familiar disappears.

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
					RequestThread.postRequest( new EquipmentRequest( blackbirdItem ) );
				}

				Preferences.setString( "preBlackbirdFamiliar", "" );
			}

			QuestLogRequest.setBlackMarketAvailable();
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
				KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, UseItemRequest.lastUpdate );
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
				KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, UseItemRequest.lastUpdate );
				ResultProcessor.processResult( item );
			}

			return;

		case ItemPool.FENG_SHUI:

			if ( InventoryManager.hasItem( ItemPool.FOUNTAIN ) && InventoryManager.hasItem( ItemPool.WINDCHIMES ) )
			{
				ResultProcessor.processItem( ItemPool.FOUNTAIN, -1 );
				ResultProcessor.processItem( ItemPool.WINDCHIMES, -1 );
				RequestThread.postRequest( new CampgroundRequest() );
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
					(new UseItemRequest( item )).run();
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
				KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, UseItemRequest.lastUpdate );
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
				KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, UseItemRequest.lastUpdate );
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
				KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, UseItemRequest.lastUpdate );
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
				KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, UseItemRequest.lastUpdate );
				ResultProcessor.processResult( item );
			}

			return;

		case ItemPool.ROLLING_PIN:

			// Rolling pins remove dough from your inventory.
			// They are not consumed by being used

			ResultProcessor.processItem( ItemPool.DOUGH, 0 - InventoryManager.getCount( ItemPool.DOUGH ) );
			ResultProcessor.processResult( item );
			return;

		case ItemPool.UNROLLING_PIN:

			// Unrolling pins remove flat dough from your inventory.
			// They are not consumed by being used

			ResultProcessor.processItem( ItemPool.FLAT_DOUGH, 0 - InventoryManager.getCount( ItemPool.FLAT_DOUGH ) );
			ResultProcessor.processResult( item );
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
				KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, UseItemRequest.lastUpdate );
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
			// Grimoires
		case ItemPool.HILARIOUS_BOOK:
		case ItemPool.TASTEFUL_BOOK:
		case ItemPool.CARD_GAME_BOOK:
			// Librams
		case ItemPool.CANDY_BOOK:
		case ItemPool.DIVINE_BOOK:
		case ItemPool.LOVE_BOOK:
		case ItemPool.BRICKO_BOOK:
		case ItemPool.DICE_BOOK:
		case ItemPool.RESOLUTION_BOOK:
			// Others
		case ItemPool.JEWELRY_BOOK:
		case ItemPool.OLFACTION_BOOK:
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
		{
			int itemId = item.getItemId();
			String skill = UseItemRequest.itemToSkill( itemId );
			String bookClass = UseItemRequest.itemToClass( itemId );
			boolean isRightClass = bookClass == null || bookClass.equals( KoLCharacter.getClassType() );
			if ( skill == null || KoLCharacter.hasSkill( skill ) || !isRightClass )
			{
				if ( UseItemRequest.getConsumptionType( item ) != KoLConstants.INFINITE_USES )
				{
					ResultProcessor.processResult( item );
				}
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
				ResponseTextParser.externalUpdate( req.getURLString(), req.responseText );
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
				KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, UseItemRequest.lastUpdate );
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

		case ItemPool.BLACK_PUDDING:

			// "You screw up your courage and eat the black pudding.
			// It turns out to be the blood sausage sort of
			// pudding. You're not positive that that's a good
			// thing. Bleah."

			if ( responseText.indexOf( "blood sausage" ) != -1 )
			{
				return;
			}

			// "You don't have time to properly enjoy a black
			// pudding right now."
			if ( responseText.indexOf( "don't have time" ) != -1 )
			{
				UseItemRequest.lastUpdate = "Insufficient adventures left.";
			}

			// "You're way too beaten up to enjoy a black pudding
			// right now. Because they're tough to chew. Yeah."
			else if ( responseText.indexOf( "too beaten up" ) != -1 )
			{
				UseItemRequest.lastUpdate = "Too beaten up.";
			}

			if ( !UseItemRequest.lastUpdate.equals( "" ) )
			{
				KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, UseItemRequest.lastUpdate );
			}

			// Eating a black pudding via the in-line ajax support
			// no longer redirects to a fight. Instead, the fight
			// is forced by a script:

			// <script type="text/javascript">top.mainpane.document.location="fight.php";</script>

			// If we are redirected to a fight, the item is
			// consumed elsewhere. If we got here, we removed it
			// above, but it wasn't actually consumed

			ResultProcessor.processResult( item );

			return;

		case ItemPool.DRUM_MACHINE:

			// "Dammit! Your hooks were still on there! Oh well. At
			// least now you know where the pyramid is."

			if ( responseText.indexOf( "hooks were still on" ) != -1 )
			{
				if ( KoLCharacter.inFistcore() )
				{
					// You lose your hooks
					ResultProcessor.processItem( ItemPool.WORM_RIDING_HOOKS, -1 );
				}
				else
				{
					// You lose your weapon
					EquipmentManager.discardEquipment( ItemPool.WORM_RIDING_HOOKS );
					KoLmafia.updateDisplay( "Don't forget to equip a weapon!" );
				}
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
				KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, UseItemRequest.lastUpdate );
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
				KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, UseItemRequest.lastUpdate );
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
				KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, UseItemRequest.lastUpdate );
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
				KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, UseItemRequest.lastUpdate );
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
				KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, UseItemRequest.lastUpdate );
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
				KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, UseItemRequest.lastUpdate );
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

		case ItemPool.STEEL_STOMACH:

			if ( responseText.indexOf( "You acquire a skill" ) != -1 )
			{
				ResponseTextParser.learnSkill( "Stomach of Steel" );
			}
			return;

		case ItemPool.STEEL_LIVER:

			if ( responseText.indexOf( "You acquire a skill" ) != -1 )
			{
				ResponseTextParser.learnSkill( "Liver of Steel" );
			}
			return;

		case ItemPool.STEEL_SPLEEN:

			if ( responseText.indexOf( "You acquire a skill" ) != -1 )
			{
				ResponseTextParser.learnSkill( "Spleen of Steel" );
			}
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
				Preferences.setBoolean( "distentionPillActive", true );
				KoLCharacter.updateStatus();
				ConcoctionDatabase.getUsables().sort();
			}
			return;

		case ItemPool.MILK_OF_MAGNESIUM:

			ConcoctionDatabase.getUsables().sort();
			return;

		case ItemPool.FERMENTED_PICKLE_JUICE:
		case ItemPool.EXTRA_GREASY_SLIDER:
			Preferences.setInteger( "currentSpleenUse",
				Math.max( 0, Preferences.getInteger( "currentSpleenUse" ) -
					5 * item.getCount() ) );
			KoLCharacter.updateStatus();
			return;

		case ItemPool.NEWBIESPORT_TENT:
		case ItemPool.BARSKIN_TENT:
		case ItemPool.COTTAGE:
		case ItemPool.HOUSE:
		case ItemPool.SANDCASTLE:
		case ItemPool.TWIG_HOUSE:
		case ItemPool.HOBO_FORTRESS:

			if ( responseText.indexOf( "You've already got" ) != -1 )
			{
				ResultProcessor.processResult( item );
				return;
			}
			RequestThread.postRequest( new CampgroundRequest() );
			return;

		case ItemPool.MAID:
		case ItemPool.CLOCKWORK_MAID:
		case ItemPool.SCARECROW:
		case ItemPool.MEAT_GOLEM:
			RequestThread.postRequest( new CampgroundRequest() );
			return;

		case ItemPool.PRETTY_BOUQUET:
		case ItemPool.PICKET_FENCE:
		case ItemPool.BARBED_FENCE:
		case ItemPool.BEANBAG_CHAIR:
		case ItemPool.GAUZE_HAMMOCK:
		case ItemPool.HOT_BEDDING:
		case ItemPool.COLD_BEDDING:
		case ItemPool.STENCH_BEDDING:
		case ItemPool.SPOOKY_BEDDING:
		case ItemPool.SLEAZE_BEDDING:
		case ItemPool.BLACK_BLUE_LIGHT:
		case ItemPool.LOUDMOUTH_LARRY:
		case ItemPool.PLASMA_BALL:
			if ( responseText.indexOf( "already" ) != -1 )
			{
				ResultProcessor.processResult( item );
				return;
			}
			RequestThread.postRequest( new CampgroundRequest() );
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

		case ItemPool.MACARONI_FRAGMENTS:
		case ItemPool.SHIMMERING_TENDRILS:
		case ItemPool.SCINTILLATING_POWDER:
		case ItemPool.PARANORMAL_RICOTTA:
		case ItemPool.SMOKING_TALON:
		case ItemPool.VAMPIRE_GLITTER:
		case ItemPool.WINE_SOAKED_BONE_CHIPS:
		case ItemPool.CRUMBLING_RAT_SKULL:
		case ItemPool.TWITCHING_TRIGGER_FINGER:
		case ItemPool.DECODED_CULT_DOCUMENTS:

			KoLCharacter.ensureUpdatedPastaGuardians();
			int itemId = item.getItemId();
			for ( int i = 0; i < KoLCharacter.PASTA_GUARDIANS.length; ++ i )
			{
				Object [] entity = KoLCharacter.PASTA_GUARDIANS[i];
				int summonItem = ((Integer)entity[1]).intValue();
				if ( itemId != summonItem )
				{
					continue;
				}

				Pattern pattern = (Pattern)entity[2];
				matcher = pattern.matcher( responseText );
				if ( !matcher.find() )
				{
					break;
				}

				Preferences.setString( "pastamancerGhostType", (String)entity[0] );
				Preferences.setString( "pastamancerGhostName", matcher.group(1) );
				Preferences.setInteger( "pastamancerGhostExperience", 0 );
				return;
			}

			// The decoded cult documents are reusable, whether or
			// not the summon succeeded.
			if ( item.getItemId() != ItemPool.DECODED_CULT_DOCUMENTS )
			{
				ResultProcessor.processResult( item );
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
				KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, UseItemRequest.lastUpdate );
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
				KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, UseItemRequest.lastUpdate );
				ResultProcessor.processResult( item );
			}

			else if ( responseText.indexOf( "You aren't allowed to go to any Haunted Houses right now" ) != -1 )
			{
				UseItemRequest.lastUpdate = "You aren't allowed to go to any Haunted Houses right now.";
				KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, UseItemRequest.lastUpdate );
				ResultProcessor.processResult( item );
			}

			else if ( responseText.indexOf( "You don't know where any haunted sorority houses are right now." ) != -1  ||
			     responseText.indexOf( "No way. It's boring in there now that everybody is dead." ) != -1 )
			{
				UseItemRequest.lastUpdate = "The Haunted Sorority House is unavailable.";
				KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, UseItemRequest.lastUpdate );
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
				KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, UseItemRequest.lastUpdate );
				ResultProcessor.processResult( item );
			}
			return;

		case ItemPool.BLACK_PAINT:

			if ( KoLCharacter.inFistcore() &&
			     responseText.indexOf( "Your teachings forbid the use of black paint." ) != -1 )
			{
				UseItemRequest.lastUpdate = "Your teachings forbid the use of black paint.";
				KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, UseItemRequest.lastUpdate );
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
			Preferences.setBoolean( "_jackassePlumberGame", true );
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
		case ItemPool.HILARIOUS_BOOK:
			return "Summon Hilarious Objects";
		case ItemPool.TASTEFUL_BOOK:
			return "Summon Tasteful Items";
		case ItemPool.CARD_GAME_BOOK:
			return "Summon Alice's Army Cards";
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
	}

	private static final void handleFortuneCookie( final Matcher matcher )
	{
		String message = matcher.group( 1 );

		RequestLogger.updateSessionLog( message );
		RequestLogger.printLine( message );

		if ( TurnCounter.isCounting( "Fortune Cookie" ) )
		{
			for ( int i = 2; i <= 4; ++i )
			{
				int number = StringUtilities.parseInt( matcher.group( i ) );
				if ( TurnCounter.isCounting( "Fortune Cookie", number ) )
				{
					TurnCounter.stopCounting( "Fortune Cookie" );
					TurnCounter.startCounting( number, "Fortune Cookie", "fortune.gif" );
					TurnCounter.stopCounting( "Semirare window begin" );
					TurnCounter.stopCounting( "Semirare window end" );
					return;
				}
			}
		}

		int minCounter;

		// First semirare comes between 70 and 80 regardless of path

		// If we haven't played 70 turns, we definitely have not passed
		// the semirare counter yet.
		if ( KoLCharacter.getCurrentRun() < 70 )
		{
			minCounter = 70;
		}
		// If we haven't seen a semirare yet and are still within the
		// window for the first, again, expect the first one.
		else if ( KoLCharacter.getCurrentRun() < 80 &&
			  KoLCharacter.lastSemirareTurn() == 0 )
		{
			minCounter = 70;
		}
		// Otherwise, we are definitely past the first semirare,
		// whether or not we saw it. If you are not an Oxygenarian,
		// semirares come less frequently
		else if ( KoLCharacter.canEat() || KoLCharacter.canDrink() )
		{
			minCounter = 150;	// conservative, wiki claims 160 minimum
		}
		// ... than if you are on the Oxygenarian path
		else
		{
			minCounter = 100;	// conservative, wiki claims 102 minimum
		}

		minCounter -= KoLCharacter.turnsSinceLastSemirare();
		for ( int i = 2; i <= 4; ++i )
		{
			int number = StringUtilities.parseInt( matcher.group( i ) );
			int minEnd = 0;
			if ( TurnCounter.getCounters( "Semirare window begin", 0, 500 ).equals( "" ) )
			{
				// We are possibly within the window currently.
				// If the actual semirare turn has already been
				// missed, a number past the window end could
				// be valid - but it would have to be at least
				// 80 turns past the end.
				minEnd = number - 79;
			}

			if ( number < minCounter ||
			     !TurnCounter.getCounters( "Semirare window begin", number + 1, 500 ).equals( "" ) )
			{
				KoLmafia.updateDisplay( "Lucky number " + number +
							" ignored - too soon to be a semirare." );
				continue;
			}

			if ( number > 205 ||
				  !TurnCounter.getCounters( "Semirare window end", minEnd, number - 1 ).equals( "" ) )
			{	// conservative, wiki claims 200 maximum
				KoLmafia.updateDisplay( "Lucky number " + number +
							" ignored - too large to be a semirare." );
				continue;
			}

			// One fortune cookie can contain two identical numbers
			// and thereby pinpoint the semirare turn.
			if ( TurnCounter.isCounting( "Fortune Cookie", number ) )
			{
				TurnCounter.stopCounting( "Fortune Cookie" );
				TurnCounter.startCounting( number, "Fortune Cookie", "fortune.gif" );
				TurnCounter.stopCounting( "Semirare window begin" );
				TurnCounter.stopCounting( "Semirare window end" );
				return;
			}

			// Add the new lucky number
			TurnCounter.startCounting( number, "Fortune Cookie", "fortune.gif" );
		}

		TurnCounter.stopCounting( "Semirare window begin" );
		TurnCounter.stopCounting( "Semirare window end" );

	}

	public static final String lastSemirareMessage()
	{
		KoLCharacter.ensureUpdatedAscensionCounters();

		int turns = Preferences.getInteger( "semirareCounter" );
		if ( turns == 0 )
		{
			return "No semirare found yet this run.";
		}

		int current = KoLCharacter.getCurrentRun();
		String location = Preferences.getString( "semirareLocation" );
		String loc = location.equals( "" ) ? "" : ( " in " + location );
		return "Last semirare found " + ( current - turns ) + " turns ago (on turn " + turns + ")" + loc;
	}

	private static final void showItemUsage( final boolean showHTML, final String text )
	{
		if ( showHTML )
		{
			StaticEntity.getClient().showHTML(
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
		if ( !urlString.startsWith( "inv_eat.php" ) &&
		     !urlString.startsWith( "inv_booze.php" ) &&
		     !urlString.startsWith( "inv_use.php" ) &&
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

		if ( urlString.indexOf( "multiuse.php" ) != -1 ||
		     urlString.indexOf( "inv_eat.php" ) != -1 ||
		     urlString.indexOf( "inv_booze.php" ) != -1 ||
		     urlString.indexOf( "inv_use.php" ) != -1)
		{
			Matcher quantityMatcher = UseItemRequest.QUANTITY_PATTERN.matcher( urlString );
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

		Matcher itemMatcher = UseItemRequest.ITEMID_PATTERN.matcher( urlString );
		if ( !itemMatcher.find() )
		{
			return null;
		}

		int itemId = StringUtilities.parseInt( itemMatcher.group( 1 ) );
		int itemCount = 1;

		Matcher quantityMatcher = UseItemRequest.QTY_PATTERN.matcher( urlString );
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

	public static final boolean registerRequest( final String urlString )
	{
		if ( urlString.startsWith( "skills.php" ) )
		{
			// Don't overwrite lastItemUsed when restoratives are
			// used from the Skills page or quickskills menu.  The
			// request was initially made to inv_use.php (and
			// lastItemUsed was set at that time), which redirects
			// to skills.php - but without enough information in
			// the URL to determine exactly what was used.
			return true;
		}

		if ( urlString.indexOf( "action=closetpull" ) != -1 ||
		     urlString.indexOf( "action=closetpush" ) != -1 )
		{
			return ClosetRequest.registerRequest( urlString );
		}

		// A. W. O. L. commendation
		if ( urlString.indexOf( "whichitem=5116" ) != -1 )
		{
			UseItemRequest.lastItemUsed = null;
			return AWOLQuartermasterRequest.registerRequest( urlString );
		}

		// wand of fudge control
		if ( urlString.indexOf( "whichitem=5441" ) != -1 )
		{
			UseItemRequest.lastItemUsed = null;
			return FudgeWandRequest.registerRequest( urlString );
		}

		UseItemRequest.lastItemUsed = UseItemRequest.extractItem( urlString );
		if ( UseItemRequest.lastItemUsed == null )
		{
			return UseItemRequest.registerBingeRequest( urlString );
		}

		UseItemRequest.lastHelperUsed = UseItemRequest.extractHelper( urlString );
		UseItemRequest.lastLook = urlString.indexOf( "action=look" ) != -1;

		int itemId = UseItemRequest.lastItemUsed.getItemId();
		int count = UseItemRequest.lastItemUsed.getCount();
		String name = UseItemRequest.lastItemUsed.getName();
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

			int fullness = ItemDatabase.getFullness( name );
			if ( fullness <= 0 ) break;
			int maxcount = ( KoLCharacter.getFullnessLimit() - KoLCharacter.getFullness() + ( Preferences
				.getBoolean( "distentionPillActive" ) ? 1 : 0 ) ) / fullness;
			if ( count > maxcount )
			{
				count = maxcount;
				UseItemRequest.lastItemUsed = UseItemRequest.lastItemUsed.getInstance( maxcount );
			}
			Preferences.increment( "currentFullness", fullness * count );
			Preferences.setInteger( "munchiesPillsUsed", Math.max( Preferences.getInteger( "munchiesPillsUsed" ) - count, 0 ) );
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

		case ItemPool.MUNCHIES_PILL:
			Preferences.increment( "munchiesPillsUsed", count );
			break;

		case ItemPool.DRINK_ME_POTION:
			Preferences.increment( "pendingMapReflections", count );
			break;

		case ItemPool.BLACK_MARKET_MAP: {
			int needed = KoLCharacter.inBeecore() ?
				FamiliarPool.CROW : FamiliarPool.BLACKBIRD;
			int hatchling = needed == FamiliarPool.CROW ?
				ItemPool.RECONSTITUTED_CROW : ItemPool.REASSEMBLED_BLACKBIRD;
			if ( KoLCharacter.getFamiliar().getId() != needed )
			{
				AdventureResult map = UseItemRequest.lastItemUsed;

				// Get the player's current blackbird, complete
				// with whatever name it's been given.
				FamiliarData blackbird = KoLCharacter.findFamiliar( needed );

				// If there is no blackbird in the terrarium,
				// grow one from the hatchling.
				if ( blackbird == null )
				{
					( new UseItemRequest( ItemPool.get( hatchling, 1 ) ) ).run();
					UseItemRequest.lastItemUsed = map;
					blackbird = KoLCharacter.findFamiliar( needed );
				}

				// If still couldn't find it, bail
				if ( blackbird == null )
				{
					return true;
				}

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

		int spleenHit = ItemDatabase.getSpleenHit( name ) * count;
		if ( spleenHit > 0 && KoLCharacter.getSpleenUse() + spleenHit <= KoLCharacter.getSpleenLimit() )
		{
			Preferences.increment( "currentSpleenUse", spleenHit );
		}

		if ( useString == null )
		{
			useString = ( consumptionType == KoLConstants.CONSUME_EAT ? "eat " : consumptionType == KoLConstants.CONSUME_DRINK ? "drink " : "use " ) + count + " " + name ;
		}

		RequestLogger.updateSessionLog();
		RequestLogger.updateSessionLog( useString );
		return true;
	}

	public int getAdventuresUsed()
	{
		return UseItemRequest.getAdventuresUsedByItem( this.itemUsed );
	}

	public static int getAdventuresUsedByItem( AdventureResult item )
	{
		int turns = 0;
		switch ( item.getItemId() )
		{
		case ItemPool.BLACK_PUDDING:
		case ItemPool.CARONCH_MAP:
		case ItemPool.FRATHOUSE_BLUEPRINTS:
		case ItemPool.CURSED_PIECE_OF_THIRTEEN:
		case ItemPool.SPOOKY_PUTTY_MONSTER:
		case ItemPool.SHAKING_CAMERA:
		case ItemPool.PHOTOCOPIED_MONSTER:
		case ItemPool.DOLPHIN_WHISTLE:
			// Items that can redirect to a fight
			turns = 1;
			break;

		case ItemPool.D10:
			// 1d10 gives you a monster
			// 2d10 gives you a random adventure at no cost.
			turns = item.getCount() == 1 ? 1 : 0;
			break;

		case ItemPool.REFLECTION_OF_MAP:
		case ItemPool.RONALD_SHELTER_MAP:
		case ItemPool.GRIMACE_SHELTER_MAP:
		case ItemPool.STAFF_GUIDE:
			// Items that can redirect to a choice adventure
			turns = 1;
			break;

		case ItemPool.DRUM_MACHINE:
			// Drum machine doesn't take a turn if you have worm-riding hooks equipped.
			AdventureResult hooks = ItemPool.get( ItemPool.WORM_RIDING_HOOKS, 1 );
			turns = KoLCharacter.hasEquipped( hooks ) ? 0 : 1;
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
			KoLmafia.updateDisplay( KoLConstants.ERROR_STATE,
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
				KoLmafia.updateDisplay( KoLConstants.ERROR_STATE,
					"Unable to parse conduit level: " + piece );
				return;
			}
			data = data * 11 + value / 2;
		}
		Preferences.setInteger( "lastEVHelmetValue", data );
		Preferences.setInteger( "lastEVHelmetReset", KoLCharacter.getAscensions() );
	}
}
