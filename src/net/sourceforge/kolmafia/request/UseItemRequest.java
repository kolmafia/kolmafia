/*
 * Copyright (c) 2005-2021, KoLmafia development team
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
import net.sourceforge.kolmafia.AdventureResult.AdventureLongCountResult;
import net.sourceforge.kolmafia.FamiliarData;
import net.sourceforge.kolmafia.KoLAdventure;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.Modifiers;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.SpecialOutfit.Checkpoint;

import net.sourceforge.kolmafia.moods.ManaBurnManager;
import net.sourceforge.kolmafia.moods.RecoveryManager;

import net.sourceforge.kolmafia.objectpool.AdventurePool;
import net.sourceforge.kolmafia.objectpool.EffectPool;
import net.sourceforge.kolmafia.objectpool.FamiliarPool;
import net.sourceforge.kolmafia.objectpool.IntegerPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.objectpool.OutfitPool;

import net.sourceforge.kolmafia.persistence.*;
import net.sourceforge.kolmafia.persistence.MonsterDatabase.Element;
import net.sourceforge.kolmafia.persistence.QuestDatabase.Quest;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.session.BugbearManager;
import net.sourceforge.kolmafia.session.ChoiceManager;
import net.sourceforge.kolmafia.session.ClanManager;
import net.sourceforge.kolmafia.session.DreadScrollManager;
import net.sourceforge.kolmafia.session.EquipmentManager;
import net.sourceforge.kolmafia.session.InventoryManager;
import net.sourceforge.kolmafia.session.Limitmode;
import net.sourceforge.kolmafia.session.QuestManager;
import net.sourceforge.kolmafia.session.ResponseTextParser;
import net.sourceforge.kolmafia.session.ResultProcessor;
import net.sourceforge.kolmafia.session.SpadingManager;
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

	private static final Pattern ABSORB_PATTERN = Pattern.compile( "absorb=(\\d+)" );

	private static final Pattern ROW_PATTERN = Pattern.compile( "<tr>.*?</tr>" );
	private static final Pattern INVENTORY_PATTERN = Pattern.compile( "</blockquote></td></tr></table>.*?</body>" );
	private static final Pattern HELPER_PATTERN = Pattern.compile( "(utensil|whichcard)=(\\d+)" );
	private static final Pattern BRICKO_PATTERN = Pattern.compile( "You break apart your ([\\w\\s]*)." );
	private static final Pattern TERMINAL_CHIP_PATTERN = Pattern.compile( "You have (\\d+) so far" );
	private static final Pattern FAMILIAR_NAME_PATTERN =
		Pattern.compile( "You decide to name (?:.*?) <b>(.*?)</b>" );
	private static final Pattern FRUIT_TUBING_PATTERN =
		Pattern.compile( "(?=.*?action=addfruit).*whichfruit=(\\d+)" );
	private static final Pattern ADVENTUROUS_RESOLUTION_PATTERN =
		Pattern.compile( "resolve to do it again" );
	private static final Pattern MERKIN_WORDQUIZ_PATTERN =
		Pattern.compile( "Your Mer-kin vocabulary mastery is now at <b>(\\d*?)%</b>" );
	private static final Pattern PURPLE_WORD_PATTERN =
		Pattern.compile( "don't forget <font color=purple><b><i>(.*?)</i></b></font>" );
	private static final Pattern GIFT_FROM_PATTERN =
		Pattern.compile( "<p>From: <b><a class=nounder href=\"showplayer.php\\?who=(\\d+)\">(.*?)</a></b>" );
	private static final Pattern BIRD_OF_THE_DAY_PATTERN =
		Pattern.compile( "Today's bird is the (.*?)!" );

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
		UseItemRequest.LIMITED_USES.put( IntegerPool.get( ItemPool.ASTRAL_MUSHROOM ), EffectPool.get( EffectPool.HALF_ASTRAL ) );

		UseItemRequest.LIMITED_USES.put( IntegerPool.get( ItemPool.ABSINTHE ), EffectPool.get( EffectPool.ABSINTHE ) );
	}

	public static String lastUpdate = "";
	public static String limiter = "";
	private static AdventureResult lastFruit = null;
	private static AdventureResult lastUntinker = null;
	private static boolean retrying = false;

	protected final int consumptionType;
	protected AdventureResult itemUsed;

	protected static AdventureResult lastItemUsed = null;
	protected static AdventureResult lastHelperUsed = null;
	private static int currentItemId = -1;
	private static String lastUrlString = null;

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
		case KoLConstants.CONSUME_SPLEEN:
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

		// We want to display the result of using certain items
		switch ( itemId )
		{
		case ItemPool.HOBO_CODE_BINDER:
		case ItemPool.STUFFED_BARON:
			return KoLConstants.MESSAGE_DISPLAY;
		}

		int consumptionType = ItemDatabase.getConsumptionType( itemId );

		// Spleen items can be marked "usable", if you can only use one
		// at a time, but must use a SpleenItemRequest
		if ( consumptionType == KoLConstants.CONSUME_SPLEEN )
		{
			return KoLConstants.CONSUME_SPLEEN;
		}

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

		return consumptionType;
	}

	private static String getConsumptionLocation( final int consumptionType, final AdventureResult item )
	{
		switch ( consumptionType )
		{
		case KoLConstants.CONSUME_EAT:
			return "inv_eat.php";
		case KoLConstants.CONSUME_DRINK:
			return "inv_booze.php";
		case KoLConstants.CONSUME_SPLEEN:
			return "inv_spleen.php";
		case KoLConstants.GROW_FAMILIAR:
			return "inv_familiar.php";
		case KoLConstants.CONSUME_HOBO:
		case KoLConstants.CONSUME_GHOST:
		case KoLConstants.CONSUME_SLIME:
			return "familiarbinger.php";
		case KoLConstants.CONSUME_ROBO:
			return "inventory.php";
		case KoLConstants.CONSUME_SPHERE:
			return "campground.php";
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

	public static final AdventureResult getLastItemUsed()
	{
		return UseItemRequest.lastItemUsed;
	}

	public static void setLastItemUsed( final AdventureResult item )
	{
		UseItemRequest.lastItemUsed = item;
		UseItemRequest.currentItemId = item.getItemId();
	}

	public static void clearLastItemUsed()
	{
		UseItemRequest.lastItemUsed = null;
		UseItemRequest.lastHelperUsed = null;
	}

	public static final int currentItemId()
	{
		return UseItemRequest.currentItemId;
	}

	private boolean isBingeRequest()
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
		case ItemPool.RESIDENCE_CUBE:
		case ItemPool.GIANT_PILGRIM_HAT:
		case ItemPool.HOUSE_SIZED_MUSHROOM:
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

	public static boolean askAboutPvP( final String itemName )
	{
		// If we've already asked about PvP, don't nag.
		if ( UseItemRequest.askedAboutPvP == KoLCharacter.getUserId() )
		{
			return true;
		}

		int PvPGain = ConsumablesDatabase.getPvPFights( itemName );

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

	private static int maximumUses( final int itemId, final String itemName, final int consumptionType, final boolean allowOverDrink )
	{
		if ( FightRequest.inMultiFight )
		{
			UseItemRequest.limiter = "multi-stage fight in progress";
			return 0;
		}

		if ( FightRequest.choiceFollowsFight )
		{
			UseItemRequest.limiter = "choice adventure follows the fight";
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
			if ( KoLCharacter.inBeecore() && ItemDatabase.unusableInBeecore( itemId ) )
			{
				UseItemRequest.limiter = "bees";
				return 0;
			}
			if ( KoLCharacter.inGLover() && ItemDatabase.unusableInGLover( itemId ) )
			{
				UseItemRequest.limiter = "g-lessness";
				return 0;
			}
			break;
		}

		if ( KoLCharacter.inRobocore() && ItemDatabase.isPotion( itemId ) && !Preferences.getString( "youRobotCPUUpgrades").contains( "robot_potions" ) )
		{
			return 0;
		}

		// Check binge requests before checking fullness or inebriety
		switch ( consumptionType )
		{
		case KoLConstants.CONSUME_HOBO:
		case KoLConstants.CONSUME_GHOST:
		case KoLConstants.CONSUME_SLIME:
			return Integer.MAX_VALUE;
		case KoLConstants.CONSUME_ROBO:
			return 1;
		case KoLConstants.CONSUME_GUARDIAN:
			UseItemRequest.limiter = "character class";
			return KoLCharacter.getClassType().equals( KoLCharacter.PASTAMANCER ) ? 1 : 0;
		}

		// Delegate to specialized classes as appropriate

		int inebriety = ConsumablesDatabase.getInebriety( itemName );
		if ( inebriety > 0 )
		{
			return DrinkItemRequest.maximumUses( itemId, itemName, inebriety, allowOverDrink );
		}

		int fullness = ConsumablesDatabase.getFullness( itemName );
		if ( fullness > 0 || itemId == ItemPool.MAGICAL_SAUSAGE )
		{
			return EatItemRequest.maximumUses( itemId, itemName, fullness );
		}

		int spleenHit = ConsumablesDatabase.getSpleenHit( itemName );
		if ( spleenHit > 0 )
		{
			return SpleenItemRequest.maximumUses( itemId, itemName, spleenHit );
		}

		if ( itemId <= 0 )
		{
			return Integer.MAX_VALUE;
		}

		long restorationMaximum = Long.MAX_VALUE;

		// User may want to use this for its effect, rather than
		// restoration, so don't limit potions
		if ( !ItemDatabase.isPotion( itemId ) && RestoresDatabase.isRestore( itemId ) )
		{
			// If neither HP or MP is usable in this path, unusable item
			if ( RestoresDatabase.getHPAverage( itemName ) == 0 &&
			     RestoresDatabase.getMPAverage( itemName ) == 0 )
			{
				UseItemRequest.limiter = "uselessness in this path";
				return 0;
			}
			restorationMaximum = UseItemRequest.getRestorationMaximum( itemName );
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
			if ( KoLConstants.activeEffects.contains( KoLAdventure.BEATEN_UP ) && restorationMaximum == 0 )
			{
				UseItemRequest.limiter = "needed restoration";
				return 1;
			}
			break;

		case ItemPool.GONG:
		case ItemPool.KETCHUP_HOUND:
			UseItemRequest.limiter = "usability";
			return 1;

		case ItemPool.MEDICINAL_HERBS:
			if ( restorationMaximum > 0 )
			{
				UseItemRequest.limiter = "usability";
				return 1;
			}
			break;

		case ItemPool.FIELD_GAR_POTION:
			// Disallow using potion if already Gar-ish
			Calendar date = Calendar.getInstance( TimeZone.getTimeZone( "GMT-0700" ) );
			if( date.get( Calendar.DAY_OF_WEEK ) == Calendar.MONDAY )
			{
				UseItemRequest.limiter = "uselessness on Mondays";
				return 0;
			}
			if ( KoLConstants.activeEffects.contains( EffectPool.get( EffectPool.GARISH ) ) )
			{
				UseItemRequest.limiter = "existing effect";
				return 0;
			}
			return 1;

		case ItemPool.TOASTER:
			UseItemRequest.limiter = "usability";
			return Preferences.getBoolean( "_toastSummoned" ) ? 0 : 1;

		case ItemPool.AMINO_ACIDS:
		{
			UseItemRequest.limiter = "usability";
			int aminoAcidsUsed = Preferences.getInteger( "aminoAcidsUsed" );
			return 3 - aminoAcidsUsed;
		}

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
			UseItemRequest.limiter = "spleen";
			return KoLCharacter.getSpleenUse();

		case ItemPool.SYNTHETIC_DOG_HAIR_PILL:
			if ( KoLCharacter.getInebriety() == 0 )
			{
				UseItemRequest.limiter = "sobriety";
				return 0;
			}
			break;

		case ItemPool.MOVEABLE_FEAST:
			String familiar = KoLCharacter.getFamiliar().getRace();
			if ( Preferences.getString( "_feastedFamiliars" ).contains( familiar ) )
			{
				UseItemRequest.limiter = "a previous " + familiar + " feasting";
				return 0;
			}
			break;

		case ItemPool.GHOSTLY_BODY_PAINT:
		case ItemPool.NECROTIZING_BODY_SPRAY:
		case ItemPool.BITE_LIPSTICK:
		case ItemPool.WHISKER_PENCIL:
		case ItemPool.PRESS_ON_RIBS:
			if ( KoLConstants.activeEffects.contains( EffectPool.get( EffectPool.HAUNTING_LOOKS ) ) ||
			     KoLConstants.activeEffects.contains( EffectPool.get( EffectPool.DEAD_SEXY ) ) ||
			     KoLConstants.activeEffects.contains( EffectPool.get( EffectPool.VAMPIN ) ) ||
			     KoLConstants.activeEffects.contains( EffectPool.get( EffectPool.YIFFABLE_YOU ) ) ||
			     KoLConstants.activeEffects.contains( EffectPool.get( EffectPool.BONE_US_ROUND ) ) )
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

		case ItemPool.DARK_CHOCOLATE_HEART:
			if ( restorationMaximum == 0 )
			{
				UseItemRequest.limiter = "already at full health";
				return 0;
			}
			break;

		case ItemPool.RESOLUTION_ADVENTUROUS:
			if ( Preferences.getInteger( "_resolutionAdv" ) == 10 )
			{
				UseItemRequest.limiter = "daily limit";
				return 0;
			}
			break;

		case ItemPool.CSA_FIRE_STARTING_KIT:
			if ( !KoLCharacter.getHippyStoneBroken() && Preferences.getInteger( "choiceAdventure595" ) == 1 )
			{
				UseItemRequest.limiter = "an unbroken hippy stone";
				return 0;
			}
			break;

		case ItemPool.LEFT_BEAR_ARM:
			UseItemRequest.limiter = "insufficient right bear arms";
			return InventoryManager.getCount( ItemPool.RIGHT_BEAR_ARM );

		case ItemPool.SUSHI_ROLLING_MAT:
			UseItemRequest.limiter = "usability";
			return KoLCharacter.hasSushiMat() ? 0 : 1;

		case ItemPool.ETERNAL_CAR_BATTERY:
			if ( restorationMaximum == 0 )
			{
				UseItemRequest.limiter = "already at full MP";
				return 0;
			}
			break;

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
			if ( !KoLCharacter.getClassType().equals( KoLCharacter.PASTAMANCER ) )
			{
				UseItemRequest.limiter = "character class";
				return 0;
			}
			if ( Preferences.getBoolean( "_pastaAdditive" ) )
			{
				UseItemRequest.limiter = "daily limit";
				return 0;
			}
			break;

		case ItemPool.CHRONER_CROSS:
			if ( !KoLConstants.inventory.contains( ItemPool.get( ItemPool.CHRONER, 1 ) ) )
			{
				UseItemRequest.limiter = "not having a Chroner";
				return 0;
			}
			break;

		case ItemPool.GAUDY_KEY:
			if ( !KoLCharacter.hasEquipped( ItemPool.get( ItemPool.PIRATE_FLEDGES, 1 ) ) &&
			     !EquipmentManager.isWearingOutfit( OutfitPool.SWASHBUCKLING_GETUP ) )
			{
				UseItemRequest.limiter = "not wearing pirate gear";
				return 0;
			}
			break;

		case ItemPool.BITTYCAR_HOTCAR:
			UseItemRequest.limiter = "already being active";
			return Preferences.getString( "_bittycar" ).equals( "hotcar" ) ? 0 : 1;

		case ItemPool.BITTYCAR_MEATCAR:
			UseItemRequest.limiter = "already being active";
			return Preferences.getString( "_bittycar" ).equals( "meatcar" ) ? 0 : 1;

		case ItemPool.BITTYCAR_SOULCAR:
			UseItemRequest.limiter = "already being active";
			return Preferences.getString( "_bittycar" ).equals( "soulcar" ) ? 0 : 1;

		case ItemPool.STILL_BEATING_SPLEEN:
			UseItemRequest.limiter = "already being active";
			return Preferences.getInteger( "lastStillBeatingSpleen" ) == KoLCharacter.getAscensions() ? 0 : 1;

		case ItemPool.MAYONEX:
		case ItemPool.MAYODIOL:
		case ItemPool.MAYOSTAT:
		case ItemPool.MAYOZAPINE:
		case ItemPool.MAYOFLEX:
			AdventureResult workshedItem = CampgroundRequest.getCurrentWorkshedItem();
			if ( workshedItem == null || workshedItem.getItemId() != ItemPool.MAYO_CLINIC )
			{
				UseItemRequest.limiter = "mayo clinic not installed";
				return 0;
			}
			UseItemRequest.limiter = "mayonaise already in mouth";
			return Preferences.getString( "mayoInMouth" ).equals( "" ) ? 1 : 0;

		case ItemPool.HOLORECORD_POWERGUY:
		case ItemPool.HOLORECORD_SHRIEKING_WEASEL:
		case ItemPool.HOLORECORD_SUPERDRIFTER:
		case ItemPool.HOLORECORD_LUCKY_STRIKES:
		case ItemPool.HOLORECORD_DRUNK_UNCLES:
		case ItemPool.HOLORECORD_EMD:
		case ItemPool.HOLORECORD_PIGS:
			UseItemRequest.limiter = "lack of Wrist-Boy";
			return InventoryManager.hasItem( ItemPool.WRIST_BOY ) ? Integer.MAX_VALUE : 0;

		case ItemPool.SCHOOL_OF_HARD_KNOCKS_DIPLOMA:
			if ( !KoLCharacter.getHippyStoneBroken() )
			{
				UseItemRequest.limiter = "an unbroken hippy stone";
				return 0;
			}
			break;

		case ItemPool.VICTOR_SPOILS:
			if ( !KoLCharacter.inBondcore() )
			{
				UseItemRequest.limiter = "not being Bond";
				return 0;
			}
			break;

		case ItemPool.M282:
		case ItemPool.SNAKE:
		case ItemPool.SPARKLER:
		case ItemPool.GREEN_ROCKET:
			if ( !HolidayDatabase.getHoliday().contains( "Dependence Day" ) )
			{
				UseItemRequest.limiter = "not Dependence Day";
				return 0;
			}
			break;
		}

		DailyLimitDatabase.DailyLimit dailyLimit = DailyLimitDatabase.DailyLimitType.USE.getDailyLimit( itemId );
		if ( dailyLimit != null )
		{
			UseItemRequest.limiter = "daily limit";
			return dailyLimit.getUsesRemaining();
		}

		if ( restorationMaximum < Long.MAX_VALUE )
		{
			UseItemRequest.limiter = "needed restoration";
			return (int) Math.min( Integer.MAX_VALUE, restorationMaximum );
		}

		if ( CampgroundRequest.isWorkshedItem( itemId ) )
		{
			UseItemRequest.limiter = "already changed workshed item";
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

	protected static long getRestorationMaximum( final String itemName )
	{
		double hpRestored = RestoresDatabase.getHPAverage( itemName );
		boolean restoresHP = hpRestored != 0;
		double mpRestored = RestoresDatabase.getMPAverage( itemName );
		boolean restoresMP = mpRestored != 0;

		if ( !restoresHP && !restoresMP )
		{
			return Long.MAX_VALUE;
		}

		long maximumSuggested = 0;

		if ( hpRestored != 0.0 )
		{
			double belowMax = KoLCharacter.getMaximumHP() - KoLCharacter.getCurrentHP();
			maximumSuggested = Math.max( maximumSuggested, (long) Math.ceil( belowMax / hpRestored ) );
		}

		if ( mpRestored != 0.0 )
		{
			double belowMax = KoLCharacter.getMaximumMP() - KoLCharacter.getCurrentMP();
			maximumSuggested = Math.max( maximumSuggested, (long) Math.ceil( belowMax / mpRestored ) );
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
		case ItemPool.DECK_OF_EVERY_CARD:
			// Treat a "use" of the deck as "play random"
			( new DeckOfEveryCardRequest() ).run();
			return;
		
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
		case ItemPool.ED_DIARY:
		{
			// Make it a RelayRequest since we don't want a charpane refresh
			RelayRequest request = new RelayRequest( false );
			request.constructURLString( "diary.php?textversion=1" );
			RequestThread.postRequest( request );
			KoLmafia.updateDisplay( "Your father's diary has been read." );
			return;
		}

		case ItemPool.VOLCANO_MAP:
		{
			try
			{
				GenericRequest.suppressUpdate( true );
				GenericRequest request = new GenericRequest( "inv_use.php?which=3&whichitem=3291&pwd" );
				RequestThread.postRequest( request );
				// This will redirect to volcanoisland.php
			}
			finally
			{
				GenericRequest.suppressUpdate( false );
			}
			KoLmafia.updateDisplay( "The secret tropical island volcano lair map has been read." );
			return;
		}

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
		case ItemPool.RESIDENCE_CUBE:
		case ItemPool.GIANT_PILGRIM_HAT:
		case ItemPool.HOUSE_SIZED_MUSHROOM:
			AdventureResult dwelling = CampgroundRequest.getCurrentDwelling();
			int oldLevel = CampgroundRequest.getCurrentDwellingLevel();
			int newLevel = CampgroundRequest.dwellingLevel( itemId );
			if ( ( oldLevel >= 7 || newLevel < oldLevel ) && dwelling != null && !UseItemRequest.confirmReplacement( dwelling.getName() ) )
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
			else
			{
				organ = "liver";
			}

			if ( !InputFieldUtilities.confirm( "A " + ItemDatabase.getItemName( itemId ) + "clears 3 " + organ +
				  " and you have not filled that yet.  Are you sure you want to use it?" ) )
			{
				return;
			}
			break;

		case ItemPool.CHOCOLATE_SCULPTURE:
			if ( Preferences.getInteger( "_chocolateSculpturesUsed" ) < 3 )
			{
				break;
			}
			if ( !InputFieldUtilities.confirm( "Fancy chocolate sculptures are wasted after" +
			     " using 3. Are you sure you want to use it?" ) )
			{
				return;
			}
			break;

		case ItemPool.ALIEN_ANIMAL_MILK:
			if ( KoLCharacter.getFullness() >= 3 )
			{
				break;
			}
			if ( !InputFieldUtilities.confirm( "Alien animal milk clears 3 stomach" +
				  " and you have not filled that yet.  Are you sure you want to use it?" ) )
			{
				return;
			}
			break;

		case ItemPool.ALIEN_PLANT_POD:
			if ( KoLCharacter.getInebriety() >= 3 )
			{
				break;
			}
			if ( !InputFieldUtilities.confirm( "Alien plant pod clears 3 liver" +
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
		case KoLConstants.CONSUME_BOOTSKIN:
		case KoLConstants.CONSUME_BOOTSPUR:
		case KoLConstants.CONSUME_SIXGUN:
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
		if ( !ConsumablesDatabase.meetsLevelRequirement( this.itemUsed.getName() ) )
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

		// If we were previously using multiuse.php but have reduced consumption to 1,
		// switch to inv_use.php
		if ( maximumUses == 1 && this.getPath().equals( "multiuse.php" ) )
		{
			this.constructURLString( "inv_use.php" );
			this.addFormField( "whichitem", String.valueOf( this.itemUsed.getItemId() ) );
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
			case KoLConstants.CONSUME_HOBO:
			case KoLConstants.CONSUME_GHOST:
			case KoLConstants.CONSUME_SLIME:
			case KoLConstants.CONSUME_ROBO:
				break;
			default:
				iterations = origCount;
				this.itemUsed = this.itemUsed.getInstance( 1 );
			}
		}

		Checkpoint checkpoint = null;
		try
		{
			if ( itemId == ItemPool.MAFIA_ARIA )
			{
				checkpoint = new Checkpoint();
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
		}
		finally
		{
			if ( checkpoint != null )
			{
				checkpoint.close();
			}
		}

		if ( KoLmafia.permitsContinue() )
		{
			KoLmafia.updateDisplay( "Finished using " + origCount + " " + this.itemUsed.getName() + "." );
		}
	}

	private static boolean sequentialConsume( final int itemId )
	{
		return false;
	}

	public static final boolean confirmReplacement( final String name )
	{
		if ( !GenericFrame.instanceExists() )
		{
			return true;
		}

		return InputFieldUtilities.confirm( "Are you sure you want to replace your " + name + "?" );
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
			DrinkItemRequest.clearBoozeHelper();
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

		case KoLConstants.CONSUME_ROBO:
			if ( KoLCharacter.getFamiliar().getId() != FamiliarPool.ROBORTENDER )
			{
				KoLmafia.updateDisplay( MafiaState.ERROR, "You don't have a Robortender equipped" );
				return;
			}
			this.addFormField( "action", "robooze" );
			this.addFormField( "ajax", "1" );
			useTypeAsString = "Boozing Robortender with";
			break;

		case KoLConstants.CONSUME_MULTIPLE:
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
			message.append( this.itemUsed.getCount() );
			message.append( " " );
		}
		message.append( this.itemUsed.getName() );
		if ( totalIterations != 1 )
		{
			message.append( " (" );
			message.append( currentIteration );
			message.append( " of " );
			message.append( totalIterations );
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
		int effectId = EffectDatabase.getEffectId( remove );
		AdventureResult effect = EffectPool.get( effectId );
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
		if ( this.getPath().startsWith( "choice.php" ) )
		{
			// We have been redirected. Unlike a redirection to fight.php,
			// which GenericRequest automates in FightRequest.INSTANCE, we
			// automate this ourself in runOneIteration. Item consumption
			// was handled in GenericRequest.checkItemRedirection, so punt.
			return;
		}
		
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
		case KoLConstants.CONSUME_ROBO:
			if ( !UseItemRequest.parseRobortenderBinge( this.getURLString(), this.responseText ) )
			{
				KoLmafia.updateDisplay( MafiaState.ERROR, "Your Robortender can't drink that." );
			}
			return;
		}

		UseItemRequest.lastItemUsed = this.itemUsed;
		UseItemRequest.currentItemId = this.itemUsed.getItemId();
		UseItemRequest.parseConsumption( this.responseText, true );
		ResponseTextParser.learnRecipe( this.getURLString(), this.responseText );
		SpadingManager.processConsumeItem( this.itemUsed, this.responseText );
	}

	public static final void parseBricko( final String responseText )
	{
		if ( responseText.contains( "You break apart your" ) )
		{
			Matcher matcher = UseItemRequest.BRICKO_PATTERN.matcher( responseText );
			if ( matcher.find() )
			{
				AdventureResult brickoItem = ItemPool.get( matcher.group( 1 ), -1 );
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

		if ( responseText.contains( "don't currently have" ) ||
		     responseText.contains( "not currently using" ) )
		{
			return false;
		}

		// You don't have that many of those.

		if ( responseText.contains( "don't have that many" ) )
		{
			return true;
		}

		// You don't actually have any of that item.</

		if ( responseText.contains( "don't actually have any" ) )
		{
			return true;
		}

		// [familiar name] approaches the [item] but doesn't seem interested.
		if ( responseText.contains( "doesn't seem interested" ) )
		{
			return true;
		}

		// That is not something you can give to your Slimeling
		if ( responseText.contains( "not something you can give" ) )
		{
			return true;
		}

		// <name> takes the <candy> and quickly consumes them. He
		// grows a bit.
		if ( responseText.contains( "He grows a bit" ) )
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
			     ConcoctionDatabase.meatStackCreation( itemId ) != null )
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

	public static final boolean parseRobortenderBinge( final String urlString, final String responseText )
	{
		// inventory.php?pwd&action=robooze&whichitem=9396
		if ( !urlString.startsWith( "inventory.php" ) || !urlString.contains( "action=robooze" ) )
		{
			return false;
		}

		if ( !responseText.contains( "the cocktail" ) && !responseText.contains( "the drink" ) )
		{
			return false;
		}

		Matcher itemMatcher = GenericRequest.WHICHITEM_PATTERN.matcher( urlString );
		if ( !itemMatcher.find() )
		{
			return false;
		}
		int itemId = StringUtilities.parseInt( itemMatcher.group( 1 ) );
		AdventureResult item = ItemPool.get( itemId, -1 );

		String pref = Preferences.getString( "_roboDrinks" );
		if ( pref.length() != 0 )
		{
			pref += ",";
		}
		pref += item.getName();
		Preferences.setString( "_roboDrinks", pref );
		KoLCharacter.recalculateAdjustments();
		KoLCharacter.updateStatus();

		ResultProcessor.processResult( item );

		return true;
	}

	public static final boolean parseAbsorb( final String urlString, final String responseText )
	{
		if ( !KoLCharacter.inNoobcore() )
		{
			return true;
		}

		AdventureResult item = UseItemRequest.extractAbsorbedItem( urlString );
		if ( item == null )
		{
			return true;
		}

		// You absorb some new knowledge of humanity!
		// You absorb the
		// You don't gain any new knowledge from absorbing that item, but you're able to extract a lot of energy from it!

		// Fail to absorb because it's a bad target:
		// That's too important to absorb.
		// You can't absorb something you don't have.
		if ( responseText.contains( "absorb some new knowledge" ) ||
		     responseText.contains( "You absorb the" ) ||
			 responseText.contains( "absorbing that item" ) )
		{
			String message = "Absorbing " + item.getName();
			KoLCharacter.incrementAbsorbs( 1 );
			RequestLogger.printLine( message );
			RequestLogger.updateSessionLog( message );
			ResultProcessor.processResult( item.getNegation() );
			KoLCharacter.recalculateAdjustments();
			KoLCharacter.updateStatus();
		}
		return true;
	}

	private static final Pattern HEWN_SPOON_PATTERN = Pattern.compile( "whichsign=(\\d+)" );
	private static String parseAscensionSign( String urlString )
	{
		Matcher matcher = UseItemRequest.HEWN_SPOON_PATTERN.matcher( urlString );
		if ( matcher.find() )
		{
			int num = StringUtilities.parseInt( matcher.group(1) );
			if ( num >= 1 && num <= 9 )
			{
				return KoLCharacter.ZODIACS[ num - 1 ];
			}
		}
		return null;
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
		int itemId = item.getItemId();
		int count = item.getCount();

		AdventureResult helper = UseItemRequest.lastHelperUsed;

		UseItemRequest.lastItemUsed = null;
		UseItemRequest.lastHelperUsed = null;

		if ( itemId == ItemPool.CARD_SLEEVE )
		{
			EquipmentRequest.parseCardSleeve( responseText );
			return;
		}

		// If you are in Beecore, certain items can't B used
		// "You are too scared of Bs to xxx that item."
		if ( responseText.contains( "You don't have the item you're trying to use." ) )
		{
			UseItemRequest.lastUpdate = "You don't have that item.";
			// If we think we do, then Mafia has the wrong information about inventory, so update it
			if ( InventoryManager.getCount( itemId ) > 0 )
			{
				UseItemRequest.lastUpdate = "KoL says don't have that item, but KoLMafia thinks you do, so refreshing KoLMafia Inventory";
				InventoryManager.refresh();
			}
			KoLmafia.updateDisplay( MafiaState.ERROR, UseItemRequest.lastUpdate );
			return;
		}
		if ( KoLCharacter.inBeecore() &&
		     responseText.contains( "You are too scared of Bs" ) )
		{
			UseItemRequest.lastUpdate = "You are too scared of Bs.";
			KoLmafia.updateDisplay( MafiaState.ERROR, UseItemRequest.lastUpdate );
			return;
		}
		if ( KoLCharacter.inGLover() &&
		     responseText.contains( "You are too in love with G to use that item right now." ) )
		{
			UseItemRequest.lastUpdate = "You are too in love with G.";
			KoLmafia.updateDisplay( MafiaState.ERROR, UseItemRequest.lastUpdate );
			return;
		}

		if ( responseText.contains( "be at least level" ) )
		{
			UseItemRequest.lastUpdate = "Item level too high.";
			KoLmafia.updateDisplay( MafiaState.ERROR, UseItemRequest.lastUpdate );
			return;
		}

		if ( responseText.contains( "You may not" ) )
		{
			UseItemRequest.lastUpdate = "Pathed ascension.";
			KoLmafia.updateDisplay( MafiaState.ERROR, UseItemRequest.lastUpdate );
			return;
		}

		if ( responseText.contains( "no|in a special area" ) )
		{
			UseItemRequest.lastUpdate = "Restricted by limitmode.";
			KoLmafia.updateDisplay( MafiaState.ERROR, UseItemRequest.lastUpdate );
			return;
		}

		if ( responseText.contains( "That item is too old to be used on this path." ) )
		{
			UseItemRequest.lastUpdate = "Restricted by Standard.";
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

		case KoLConstants.CONSUME_SPLEEN:
			SpleenItemRequest.parseConsumption( item, helper, responseText );
			return;
		}

		String name = item.getName();

		// At least one item - the ice stein - is neither a drink nor a
		// drink helper, but acts like a drink. Delegate.
 
		int inebriety = ConsumablesDatabase.getInebriety( name );
		if ( inebriety > 0 )
		{
			DrinkItemRequest.parseConsumption( item, helper, responseText );
			return;
		}

		int fullness = ConsumablesDatabase.getFullness( name );
		if ( fullness > 0 || itemId == ItemPool.MAGICAL_SAUSAGE )
		{
			EatItemRequest.parseConsumption( item, helper, responseText );
			return;
		}

		int spleenHit = ConsumablesDatabase.getSpleenHit( name );
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

				if ( responseText.contains( "A tinny voice emerges from the drone" ) )
				{
					// produces sparking, etc. drones - no special handling needed
				}
				else if ( !responseText.contains( "(for a long time)" ) )
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

		if ( ConcoctionDatabase.singleUseCreation( itemId ) != null )
		{
			SingleUseRequest.parseResponse( item, responseText );
			UseItemRequest.lastItemUsed = null;
			return;
		}

		if ( ConcoctionDatabase.multiUseCreation( itemId ) != null && count > 1 )
		{
			MultiUseRequest.parseResponse( item, responseText );
			UseItemRequest.lastItemUsed = null;
			return;
		}

		// Check for familiar growth - if a familiar is added,
		// make sure to update the StaticEntity.getClient().

		if ( consumptionType == KoLConstants.GROW_FAMILIAR )
		{
			if ( responseText.contains( "You've already got a familiar of that type." ) )
			{
				UseItemRequest.lastUpdate = "You already have that familiar.";
				KoLmafia.updateDisplay( MafiaState.ERROR, UseItemRequest.lastUpdate );
				return;
			}

			if ( responseText.contains( "you glance fearfully at the moons" ) )
			{
				UseItemRequest.lastUpdate = "Can't hatch that familiar in Bad Moon.";
				KoLmafia.updateDisplay( MafiaState.ERROR, UseItemRequest.lastUpdate );
				return;
			}

			if ( responseText.contains( "You don't have a Terrarium to put that in." ) )
			{
				UseItemRequest.lastUpdate = "You don't have a Terrarium yet.";
				KoLmafia.updateDisplay( MafiaState.ERROR, UseItemRequest.lastUpdate );
				return;
			}

			if ( responseText.contains( "Boris has no need for familiars" ) )
			{
				UseItemRequest.lastUpdate = "Boris has no need for familiars!";
				KoLmafia.updateDisplay( MafiaState.ERROR, UseItemRequest.lastUpdate );
				return;
			}

			// Remove the familiar larva from inventory.
			ResultProcessor.processResult( item.getNegation() );

			FamiliarData familiar = KoLCharacter.addFamiliar( FamiliarDatabase.growFamiliarLarva( itemId ) );
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

		if ( responseText.contains( "That item isn't usable in quantity" ) )
		{
			int attrs = ItemDatabase.getAttributes( itemId );
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

		if ( responseText.contains( "too full" ) )
		{
			if ( itemId == ItemPool.AMINO_ACIDS )
			{
				Preferences.setInteger( "aminoAcidsUsed", 3 );
			}

			UseItemRequest.lastUpdate = "Consumption limit reached.";
			KoLmafia.updateDisplay( MafiaState.ERROR, UseItemRequest.lastUpdate );
			return;
		}

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
		}

		Matcher matcher;

		// Perform item-specific processing

		// If the item is not consumed, either because we detect that
		// from the responseText or it is always reusable, return from
		// this method. Otherwise, break from the switch and it will be
		// removed from inventory afterwards

		switch ( itemId )
		{
		case ItemPool.LOATHING_LEGION_UNIVERSAL_SCREWDRIVER:
		{
			// You jam your screwdriver into your xxx and pry it
			// apart.
			if ( UseItemRequest.lastUntinker != null &&
			     responseText.contains( "You jam your screwdriver" ) )
			{
				ResultProcessor.processResult( UseItemRequest.lastUntinker.getNegation() );
				UseItemRequest.lastUntinker = null;
				return;
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
			if ( responseText.contains( "latches and clasps" ) )
			{
				// These are marked as reusable, but they go away when you fold them.
				ResultProcessor.processResult( item.getNegation() );
			}
			return;

		case ItemPool.WHAT_CARD:
		case ItemPool.WHEN_CARD:
		case ItemPool.WHO_CARD:
		case ItemPool.WHERE_CARD:
		{
			if ( !responseText.contains( "Answer:" ) )
			{
				return;
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
			     responseText.contains( "into the tube" ) )
			{
				ResultProcessor.processResult( UseItemRequest.lastFruit.getNegation() );
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
		{
			// "You can't receive things from other players
			// right now."

			if ( responseText.contains( "You can't receive things" ) )
			{
				UseItemRequest.lastUpdate = "You can't open that package yet.";
				KoLmafia.updateDisplay( MafiaState.ERROR, UseItemRequest.lastUpdate );
				return;
			}

			// Log sender of message
			Matcher giftFromMatcher = UseItemRequest.GIFT_FROM_PATTERN.matcher( responseText );
			if ( giftFromMatcher.find() )
			{
				String giftFrom = giftFromMatcher.group( 2 );
				String message = "Opening " + name + " from " + giftFrom;
				RequestLogger.printLine( "<font color=\"green\">" + message + "</font>" );
				RequestLogger.updateSessionLog( message );
			}

			if ( showHTML )
			{
				UseItemRequest.showItemUsage( true, responseText );
			}

			break;
		}

		case ItemPool.DANCE_CARD:
			TurnCounter.stopCounting( "Dance Card" );
			TurnCounter.startCounting( 3, "Dance Card loc=395", "guildapp.gif" );
			break;

		case ItemPool.TOASTER:

			// You push the lever and are rewarded with toast
			Preferences.setBoolean( "_toastSummoned", true );
			return;

		case ItemPool.GATES_SCROLL:

			// Oh, and don't forget <font color=purple><b><i>eJyu3</i></b></font>.  It's important.

			Matcher purpleWordMatcher = UseItemRequest.PURPLE_WORD_PATTERN.matcher( responseText );
			if ( purpleWordMatcher.find() )
			{
				String purpleWord = purpleWordMatcher.group( 1 );
				String message = "64735 Scroll Purple Word found: " + purpleWord + " in clan " + ClanManager.getClanName( false ) + ".";
				RequestLogger.printLine( "<font color=\"blue\">" + message + "</font>" );
				RequestLogger.updateSessionLog( message );
			}

			// You can only use a 64735 scroll if you have the
			// original dictionary in your inventory

			// "Even though your name isn't Lee, you're flattered
			// and hand over your dictionary."

			if ( !responseText.contains( "you're flattered" ) )
			{
				return;	
			}

			ResultProcessor.processResult( ItemPool.get( ItemPool.DICTIONARY, -1 ) );
			QuestDatabase.setQuestProgress( Quest.TOPPING, QuestDatabase.FINISHED );
			QuestDatabase.setQuestProgress( Quest.LOL, QuestDatabase.FINISHED );

			break;

		case ItemPool.ELITE_SCROLL:

			// "The UB3r 31337 HaX0R stands before you."

			if ( responseText.contains( "The UB3r 31337 HaX0R stands before you." ) )
			{
				if ( count > 1 )
				{
					(UseItemRequest.getInstance( item.getInstance( count - 1 ) )).run();
					item = item.getInstance( 1 );
				}
			}

			break;

		case ItemPool.HERMIT_SCRIPT:
			Preferences.setBoolean( "hermitHax0red", true );
			HermitRequest.hackHermit();
			break;

		case ItemPool.SPARKLER:
		case ItemPool.SNAKE:
		case ItemPool.M282:
		case ItemPool.GREEN_ROCKET:

			// "You've already celebrated the Fourth of Bor, and
			// now it's time to get back to work."

			// "Sorry, but these particular fireworks are illegal
			// on any day other than the Fourth of Bor. And the law
			// is a worthy institution, and you should respect and
			// obey it, no matter what."

			if ( responseText.contains( "back to work" ) || responseText.contains( "fireworks are illegal" ) )
			{
				return;
			}

			Preferences.setBoolean( "_fireworkUsed", true );
			break;

		case ItemPool.GONG:

			// "You try to bang the gong, but the mallet keeps
			// falling out of your hand. Maybe you should try it
			// later, when you've sobered up a little."

			// "You don't have time to bang a gong. Nor do you have
			// time to get it on, or to get it on."

			if ( responseText.contains( "sobered up a little" ) ||
			     responseText.contains( "don't have time to bang" ) )
			{
				UseItemRequest.lastUpdate = "Insufficient adventures or sobriety to use a gong.";
				KoLmafia.updateDisplay( MafiaState.ERROR, UseItemRequest.lastUpdate );
				return;
			}

			// "You're already in the middle of a journey of reincarnation."

			if ( responseText.contains( "middle of a journey of reincarnation" ) )
			{
				if ( UseItemRequest.retrying ||
				     KoLConstants.activeEffects.contains( EffectPool.get( EffectPool.FORM_OF_BIRD ) ) ||
				     KoLConstants.activeEffects.contains( EffectPool.get( EffectPool.SHAPE_OF_MOLE ) ) ||
				     KoLConstants.activeEffects.contains( EffectPool.get( EffectPool.FORM_OF_ROACH ) ) )
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

			// We deduct the gong when we get the intro choice
			// adventure: The Gong Has Been Bung

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

			if ( !responseText.contains( "grows into an enormous beanstalk" ) )
			{
				return;
			}

			QuestDatabase.setQuestProgress( Quest.GARBAGE, "step1" );

			break;

		case ItemPool.LIBRARY_CARD:

			// If you've already used a library card today, it is
			// not consumed.

			// "You head back to the library, but can't find
			// anything you feel like reading. You skim a few
			// celebrity-gossip magazines, and end up feeling kind
			// of dirty."

			Preferences.setBoolean( "libraryCardUsed", true );

			if ( !responseText.contains( "feeling kind of dirty" ) )
			{
				return;
			}

			break;

		case ItemPool.HEY_DEZE_MAP:

			// "Your song has pleased me greatly. I will reward you
			// with some of my crazy imps, to do your bidding."

			if ( !responseText.contains( "pleased me greatly" ) )
			{
				UseItemRequest.lastUpdate = "Your music was inadequate.";
				KoLmafia.updateDisplay( MafiaState.ERROR, UseItemRequest.lastUpdate );
				return;
			}

			break;

		case ItemPool.GIANT_CASTLE_MAP:

			// "I'm sorry, adventurer, but the Sorceress is in
			// another castle!"

			if ( !responseText.contains( "Sorceress is in another castle" ) )
			{
				UseItemRequest.lastUpdate = "You couldn't make it all the way to the back door.";
				KoLmafia.updateDisplay( MafiaState.ERROR, UseItemRequest.lastUpdate );
				return;
			}

			break;

		case ItemPool.DRASTIC_HEALING:

			// If a scroll of drastic healing was used and didn't
			// crumble, it is not consumed

			ResultProcessor.processResult( new AdventureLongCountResult( AdventureResult.HP, KoLCharacter.getMaximumHP() ) );

			if ( !responseText.contains( "crumble" ) )
			{
				return;
			}

			break;

		case ItemPool.TEARS:

			KoLConstants.activeEffects.remove( KoLAdventure.BEATEN_UP );
			break;

		case ItemPool.ANTIDOTE:
			// You're unpoisoned -- don't waste the anti-anti-antidote.

			if ( responseText.contains( "don't waste the anti" ) )
			{
				return;
			}

			break;

		case ItemPool.TBONE_KEY:

			if ( !InventoryManager.hasItem( ItemPool.LOCKED_LOCKER ) )
			{
				return;
			}

			ResultProcessor.processItem( ItemPool.LOCKED_LOCKER, -1 );
			break;

		case ItemPool.KETCHUP_HOUND:

			// Successfully using a ketchup hound uses up the Hey
			// Deze nuts and pagoda plan.

			if ( responseText.contains( "pagoda" ) )
			{
				ResultProcessor.processItem( ItemPool.HEY_DEZE_NUTS, -1 );
				ResultProcessor.processItem( ItemPool.PAGODA_PLANS, -1 );
				CampgroundRequest.setCampgroundItem( ItemPool.PAGODA_PLANS, 1 );
			}

			// The ketchup hound does not go away...
			return;

		case ItemPool.DOLPHIN_KING_MAP:

			// "You follow the Dolphin King's map to the bottom of
			// the sea, and find his glorious treasure."

			if ( !responseText.contains( "find his glorious treasure" ) )
			{
				UseItemRequest.lastUpdate = "You don't have everything you need.";
				KoLmafia.updateDisplay( MafiaState.ERROR, UseItemRequest.lastUpdate );
				return;
			}

			break;

		case ItemPool.SLUG_LORD_MAP:

			// "You make your way to the deepest part of the tank,
			// and find a chest engraved with the initials S. L."

			if ( !responseText.contains( "deepest part of the tank" ) )
			{
				UseItemRequest.lastUpdate = "You don't have everything you need.";
				KoLmafia.updateDisplay( MafiaState.ERROR, UseItemRequest.lastUpdate );
				return;
			}

			break;

		case ItemPool.DR_HOBO_MAP:

			// "You place it atop the Altar, and grab the Scalpel
			// at the exact same moment."

			if ( !responseText.contains( "exact same moment" ) )
			{
				UseItemRequest.lastUpdate = "You don't have everything you need.";
				KoLmafia.updateDisplay( MafiaState.ERROR, UseItemRequest.lastUpdate );
				return;
			}

			// Using the map consumes an asparagus knife

			ResultProcessor.processItem( ItemPool.ASPARAGUS_KNIFE, -1 );
			break;

		case ItemPool.SHOPPING_LIST:

			// "Since you've already built a bitchin' meatcar, you
			// wad the shopping list up and throw it away."

			UseItemRequest.showItemUsage( showHTML, responseText );

			if ( !responseText.contains( "throw it away" ) )
			{
				return;
			}

			break;

		case ItemPool.COBBS_KNOB_MAP:

			// "You memorize the location of the door, then eat
			// both the map and the encryption key."

			if ( !responseText.contains( "memorize the location" ) )
			{
				UseItemRequest.lastUpdate = "You don't have everything you need.";
				KoLmafia.updateDisplay( MafiaState.ERROR, UseItemRequest.lastUpdate );
				return;
			}

			// Using the map consumes the encryption key

			ResultProcessor.processItem( ItemPool.ENCRYPTION_KEY, -1 );
			QuestDatabase.setQuestProgress( Quest.GOBLIN, "step1" );
			break;

		case ItemPool.SPOOKY_MAP:

			if ( !InventoryManager.hasItem( ItemPool.SPOOKY_SAPLING ) ||
			     !InventoryManager.hasItem( ItemPool.SPOOKY_FERTILIZER ) )
			{
				UseItemRequest.lastUpdate = "You don't have everything you need.";
				KoLmafia.updateDisplay( MafiaState.ERROR, UseItemRequest.lastUpdate );
				return;
			}

			ResultProcessor.processItem( ItemPool.SPOOKY_SAPLING, -1 );
			ResultProcessor.processItem( ItemPool.SPOOKY_FERTILIZER, -1 );

			QuestDatabase.setQuestProgress( Quest.TEMPLE, QuestDatabase.FINISHED );
			Preferences.setInteger( "lastTempleUnlock", KoLCharacter.getAscensions() );

			// If quest Gotta Worship Them All is started, this completes step 1
			if ( QuestDatabase.isQuestLaterThan( Quest.WORSHIP, QuestDatabase.UNSTARTED ) )
			{
				QuestDatabase.setQuestProgress( Quest.WORSHIP, "step1" );
			}

			break;

		case ItemPool.CARONCH_MAP:

			// The item is only consumed once you turn in
			// the nasty booty. That's handled elsewhere.

			return;

		case ItemPool.FRATHOUSE_BLUEPRINTS:

			// The item is only consumed once you turn in the
			// dentures. That's handled elsewhere.

			return;

		case ItemPool.DINGHY_PLANS:

			// "You need some planks to build the dinghy."

			if ( !InventoryManager.hasItem( ItemPool.DINGY_PLANKS ) )
			{
				UseItemRequest.lastUpdate = "You need some dingy planks.";
				KoLmafia.updateDisplay( MafiaState.ERROR, UseItemRequest.lastUpdate );
				return;
			}

			ResultProcessor.processItem( ItemPool.DINGY_PLANKS, -1 );

			break;

		case ItemPool.MORTAR_DISSOLVING_RECIPE:
			if ( responseText.contains( "Screw this scavenger hunt crap" ) )
			{
				Preferences.setString( "spookyravenRecipeUsed", "with_glasses" );
			}
			else if ( !Preferences.getString( "spookyravenRecipeUsed" ).equals( "with_glasses" ) )
			{
				Preferences.setString( "spookyravenRecipeUsed", "no_glasses" );
			}

			break;

		case ItemPool.FENG_SHUI:

			if ( !InventoryManager.hasItem( ItemPool.FOUNTAIN ) || !InventoryManager.hasItem( ItemPool.WINDCHIMES ) )
			{
				break;
			}

			ResultProcessor.processItem( ItemPool.FOUNTAIN, -1 );
			ResultProcessor.processItem( ItemPool.WINDCHIMES, -1 );
			CampgroundRequest.setCampgroundItem( ItemPool.FENG_SHUI, 1 );

			break;

		case ItemPool.WARM_SUBJECT:

			// The first time you use Warm Subject gift
			// certificates when you have the Torso Awareness
			// skill consumes only one, even if you tried to
			// multi-use the item.

			// "You go to Warm Subject and browse the shirts for a
			// while. You find one that you wouldn't mind wearing
			// ironically. There seems to be only one in the store,
			// though."

			if ( responseText.contains( "ironically" ) )
			{
				if ( count > 1 )
				{
					(UseItemRequest.getInstance( item.getInstance( count - 1 ) )).run();
					item = item.getInstance( 1 );
				}
			}

			break;

		case ItemPool.MINING_OIL:
		case ItemPool.TAINTED_MINING_OIL:

			if ( responseText.contains( "Limiting to 100.  Sorry" ) )
			{
				if ( count > 100 )
				{
					(UseItemRequest.getInstance( item.getInstance( count - 100 ) )).run();
					item = item.getInstance( 1 );
				}
			}

			break;

		case ItemPool.PURPLE_SNOWCONE:
		case ItemPool.GREEN_SNOWCONE:
		case ItemPool.ORANGE_SNOWCONE:
		case ItemPool.RED_SNOWCONE:
		case ItemPool.BLUE_SNOWCONE:
		case ItemPool.BLACK_SNOWCONE:

			// "Your mouth is still cold from the last snowcone you
			// ate. Try again later."

			if ( responseText.contains( "still cold" ) )
			{
				UseItemRequest.lastUpdate = "Your mouth is too cold.";
				KoLmafia.updateDisplay( MafiaState.ERROR, UseItemRequest.lastUpdate );
				return;
			}

			break;

		case ItemPool.POTION_OF_PUISSANCE:
		case ItemPool.POTION_OF_PERSPICACITY:
		case ItemPool.POTION_OF_PULCHRITUDE:
		case ItemPool.POTION_OF_PERCEPTION:
		case ItemPool.POTION_OF_PROFICIENCY:

			// "You're already under the influence of a
			// high-pressure sauce potion. If you took this one,
			// you'd explode.  And not in a good way."

			if ( responseText.contains( "you'd explode" ) )
			{
				UseItemRequest.lastUpdate = "You're already under pressure.";
				KoLmafia.updateDisplay( MafiaState.ERROR, UseItemRequest.lastUpdate );
				return;
			}

			break;

		case ItemPool.BLUE_CUPCAKE:
		case ItemPool.GREEN_CUPCAKE:
		case ItemPool.ORANGE_CUPCAKE:
		case ItemPool.PURPLE_CUPCAKE:
		case ItemPool.PINK_CUPCAKE:

			// "Your stomach is still a little queasy from
			// digesting a cupcake that may or may not exist in
			// this dimension. You really don't feel like eating
			// another one just now."

			if ( responseText.contains( "a little queasy" ) )
			{
				UseItemRequest.lastUpdate = "Your stomach is too queasy.";
				KoLmafia.updateDisplay( MafiaState.ERROR, UseItemRequest.lastUpdate );
				return;
			}

			break;

		case ItemPool.VAGUE_AMBIGUITY:
		case ItemPool.SMOLDERING_PASSION:
		case ItemPool.ICY_REVENGE:
		case ItemPool.SUGARY_CUTENESS:
		case ItemPool.DISTURBING_OBSESSION:
		case ItemPool.NAUGHTY_INNUENDO:

			// "Your heart can't take another love song so soon
			// after the last one. The conflicting emotions would
			// drive you totally mad."

			if ( responseText.contains( "conflicting emotions" ) )
			{
				UseItemRequest.lastUpdate = "Your heart is already filled with emotions.";
				KoLmafia.updateDisplay( MafiaState.ERROR, UseItemRequest.lastUpdate );
				return;
			}

			break;

		case ItemPool.ROLLING_PIN:

			// Rolling pins remove dough from your inventory.
			// They are not consumed by being used

			ResultProcessor.processItem( ItemPool.DOUGH, -InventoryManager.getCount( ItemPool.DOUGH ) );
			return;

		case ItemPool.UNROLLING_PIN:

			// Unrolling pins remove flat dough from your inventory.
			// They are not consumed by being used

			ResultProcessor.processItem( ItemPool.FLAT_DOUGH, -InventoryManager.getCount( ItemPool.FLAT_DOUGH ) );
			return;

		case ItemPool.EXPRESS_CARD:

			// You feel charged up!

			if ( responseText.contains( "charged up" ) )
			{
				Preferences.setInteger( "_zapCount", 0 );
			}
			return;

		case ItemPool.PLUS_SIGN:

			// "Following The Oracle's advice, you treat the plus
			// sign as a book, and read it."

			if ( !responseText.contains( "you treat the plus sign as a book" ) )
			{
				UseItemRequest.lastUpdate = "You don't know how to use it.";
				KoLmafia.updateDisplay( MafiaState.ERROR, UseItemRequest.lastUpdate );
				return;
			}

			// Various punctuation mark items are replaced by their
			// identified versions. The new items will be detected
			// by result parsing, but we need to get rid of the old.

			for ( int i = 4552; i <= 4558; ++i )
			{
				AdventureResult punc = ItemPool.get( i, 1 );
				int pcount = punc.getCount( KoLConstants.inventory );
				if ( pcount > 0 )
				{
					ResultProcessor.processResult( punc.getInstance( -pcount ) );
				}
			}

			break;

		case ItemPool.OVEN:

			if ( responseText.contains( "already got an oven" ) )
			{
				return;
			}
			KoLCharacter.setOven( true );
			CampgroundRequest.setCampgroundItem( itemId, 1 );
			break;

		case ItemPool.RANGE:

			if ( responseText.contains( "already got a fancy oven" ) )
			{
				return;
			}
			KoLCharacter.setRange( true );
			CampgroundRequest.setCampgroundItem( itemId, 1 );
			break;

		case ItemPool.CLOCKWORK_CHEF:
			if ( responseText.contains( "already got a clockwork chef-in-the-box" ) )
			{
				return;
			}
			CampgroundRequest.removeCampgroundItem( ItemPool.get( ItemPool.CHEF, 1 ) );
			// Fall through
		case ItemPool.CHEF:
			if ( responseText.contains( "already got a chef-in-the-box" ) )
			{
				return;
			}
			KoLCharacter.setChef( true );
			CampgroundRequest.setCampgroundItem( itemId, 1 );
			Preferences.setInteger( "chefTurnsUsed", 0 );
			break;

		case ItemPool.SHAKER:

			if ( responseText.contains( "already got a cocktailcrafting kit" ) )
			{
				return;
			}
			KoLCharacter.setShaker( true );
			CampgroundRequest.setCampgroundItem( itemId, 1 );
			return;

		case ItemPool.COCKTAIL_KIT:

			if ( responseText.contains( "already got a fancy cocktailcrafting kit" ) )
			{
				return;
			}
			KoLCharacter.setCocktailKit( true );
			CampgroundRequest.setCampgroundItem( itemId, 1 );
			break;

		case ItemPool.CLOCKWORK_BARTENDER:
			if ( responseText.contains( "already got a clockwork bartender-in-the-box" ) )
			{
				return;
			}
			CampgroundRequest.removeCampgroundItem( ItemPool.get( ItemPool.BARTENDER, 1 ) );
			// Fall through
		case ItemPool.BARTENDER:
			if ( responseText.contains( "already got a bartender-in-the-box" ) )
			{
				return;
			}
			KoLCharacter.setBartender( true );
			Preferences.setInteger( "bartenderTurnsUsed", 0 );
			CampgroundRequest.setCampgroundItem( itemId, 1 );
			break;

		case ItemPool.SUSHI_ROLLING_MAT:

			KoLCharacter.setSushiMat( true );
			CampgroundRequest.setCampgroundItem( itemId, 1 );
			break;

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
		case ItemPool.CONFISCATOR_BOOK:
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
		case ItemPool.HJODOR_GUIDE_USED:
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
		case ItemPool.BLACK_BARTS_BOOTY:
		case ItemPool.HYPERSANE_BOOK:
		case ItemPool.INTIMIDATING_MIEN_BOOK:
		case ItemPool.HOLIDAY_FUN_BOOK:
		case ItemPool.ROM_RAPID_PROTOTYPING:
		case ItemPool.ROM_RAPID_PROTOTYPING_DIRTY:
		case ItemPool.ROM_MATHMATICAL_PRECISION:
		case ItemPool.ROM_MATHMATICAL_PRECISION_DIRTY:
		case ItemPool.ROM_RUTHLESS_EFFICIENCY:
		case ItemPool.ROM_RUTHLESS_EFFICIENCY_DIRTY:
		case ItemPool.ANTAGONISTIC_SNOWMAN_KIT:
		case ItemPool.SPELUNKER_FORTUNE:
		case ItemPool.SPELUNKER_FORTUNE_USED:
		case ItemPool.DINSEY_GUIDE_BOOK:
		case ItemPool.TRASH_MEMOIR_BOOK:
		case ItemPool.DINSEY_MAINTENANCE_MANUAL:
		case ItemPool.DINSEY_AN_AFTERLIFE:
		case ItemPool.MAP_TO_KOKOMO:
		case ItemPool.ESSENCE_OF_BEAR:
		case ItemPool.MANUAL_OF_NUMBEROLOGY:
		case ItemPool.LAVA_MINERS_DAUGHTER:
		case ItemPool.PSYCHO_FROM_THE_HEAT:
		case ItemPool.THE_FIREGATE_TAPES:
		case ItemPool.COLD_WEATHER_BARTENDER_GUIDE:
		case ItemPool.TO_BUILD_AN_IGLOO:
		case ItemPool.CHILL_OF_THE_WILD:
		case ItemPool.COLD_FANG:
		case ItemPool.SCROLL_SHATTERING_PUNCH:
		case ItemPool.SCROLL_SNOKEBOMB:
		case ItemPool.SCROLL_SHIVERING_MONKEY:
		case ItemPool.ROM_OF_OPTIMALITY:
		case ItemPool.WESTERN_BOOK_BRAGGADOCCIO:
		case ItemPool.WESTERN_BOOK_HELL:
		case ItemPool.WESTERN_BOOK_LOOK:
		case ItemPool.TROUT_FISHING_IN_LOATHING:
		case ItemPool.COMMUNISM_BOOK:
		case ItemPool.COMMUNISM_BOOK_USED:
		case ItemPool.BRAIN_TRAINER_GAME:
		case ItemPool.LASER_EYE_SURGERY_KIT:
		case ItemPool.MY_LIFE_OF_CRIME_BOOK:
		case ItemPool.POP_ART_BOOK:
		case ItemPool.NO_HATS_BOOK:
		case ItemPool.VIGILANTE_BOOK:
		case ItemPool.LUMP_STACKING_BOOK:
		case ItemPool.RETHINKING_CANDY_BOOK:
		case ItemPool.ELDRITCH_TINCTURE:
		case ItemPool.ELDRITCH_TINCTURE_DEPLETED:
		case ItemPool.SPACE_PIRATE_ASTROGATION_HANDBOOK:
		case ItemPool.NON_EUCLIDEAN_FINANCE:
		case ItemPool.PEEK_A_BOO:
		case ItemPool.CELSIUS_233:
		case ItemPool.CELSIUS_233_SINGED:
		case ItemPool.MIME_SCIENCE_VOL_1:
		case ItemPool.MIME_SCIENCE_VOL_1_USED:
		case ItemPool.MIME_SCIENCE_VOL_2:
		case ItemPool.MIME_SCIENCE_VOL_2_USED:
		case ItemPool.MIME_SCIENCE_VOL_3:
		case ItemPool.MIME_SCIENCE_VOL_3_USED:
		case ItemPool.MIME_SCIENCE_VOL_4:
		case ItemPool.MIME_SCIENCE_VOL_4_USED:
		case ItemPool.MIME_SCIENCE_VOL_5:
		case ItemPool.MIME_SCIENCE_VOL_5_USED:
		case ItemPool.MIME_SCIENCE_VOL_6:
		case ItemPool.MIME_SCIENCE_VOL_6_USED:
		case ItemPool.GET_BIG_BOOK:
		case ItemPool.GALAPAGOS_BOOK:
		case ItemPool.FUTURE_BOOK:
		case ItemPool.LOVE_POTION_BOOK:
		case ItemPool.RHINESTONE_BOOK:
		case ItemPool.LOVE_SONG_BOOK:
		case ItemPool.CONTRACTOR_MANUAL:
		case ItemPool.PARTY_PLANNING_BOOK:
		case ItemPool.DRINKING_TO_DRINK_BOOK:
		case ItemPool.GUIDE_TO_SAFARI:
		case ItemPool.MY_FIRST_ART_OF_WAR:
		case ItemPool.ISLAND_DRINKIN:
		case ItemPool.SCURVY_AND_SOBRIETY_PREVENTION:
		case ItemPool.THE_IMPLODED_WORLD:
		case ItemPool.THE_SPIRIT_OF_GIVING:
		case ItemPool.MANUAL_OF_LOCK_PICKING:
		{
			// You insert the ROM in to your... ROM receptacle and
			// absorb the knowledge of optimality. You suspect you
			// can now toggle your optimality at will (from the
			// skills page)!
			if ( !responseText.contains( "You acquire a skill" ) &&
			     !responseText.contains( "place the Grimoire on the bookshelf" ) &&
			     !responseText.contains( "absorb the knowledge of optimality" ) &&
			     !responseText.contains( "feel beary" ) &&
			     !responseText.contains( "knew how to maximize" ) &&
			     !responseText.contains( "additional carrot" ) &&
			     !responseText.contains( "larynx become even more pirate" ) &&
			     !responseText.contains( "become even more of an expert" ) &&
			     !responseText.contains( "reread the tale and really remember" ) &&
			     !responseText.contains( "Beleven" ) )
			{
				UseItemRequest.lastUpdate = "You can't learn that skill.";
				KoLmafia.updateDisplay( MafiaState.ERROR, UseItemRequest.lastUpdate );
				return;
			}

			String skill = UseItemRequest.itemToSkill( itemId );
			if ( skill != null )
			{
				ResponseTextParser.learnSkill( skill );
			}

			break;
		}

		case ItemPool.WESTERN_SLANG_VOL_1:
		case ItemPool.WESTERN_SLANG_VOL_2:
		case ItemPool.WESTERN_SLANG_VOL_3:

			// You memorize all of the violence-related slang terms in the book.
			//
			// Advanced Cowpuncher skills have been unlocked in the
			// Avatar of West of Loathing Challenge Path.
			//
			// You memorize all of the food-related slang terms in the book.
			// 
			// Advanced Beanslinger skills have been unlocked in
			// the Avatar of West of Loathing Challenge Path.
			// 
			// You memorize all of the slang terms about scams and con artists.
			// 
			// Advanced Snake Oiler skills have been unlocked in
			// the Avatar of West of Loathing Challenge Path.

			if ( !responseText.contains( "skills have been unlocked" ) )
			{
				UseItemRequest.lastUpdate = "You've already read that book.";
				KoLmafia.updateDisplay( MafiaState.ERROR, UseItemRequest.lastUpdate );
				return;
			}

			// Do we need to track this?

			break;

		case ItemPool.CHATEAU_ROOM_KEY:

			Preferences.setBoolean( "chateauAvailable", true );

			// You hike up the mountain to Chateau Mantegna and try
			// the key in various doors until you find the one it
			// unlocks. It's quite a nice room!

			if ( !responseText.contains( "you find the one it unlocks" ) )
			{
				UseItemRequest.lastUpdate = "You've already have a room at the Chateau Mantegna.";
				KoLmafia.updateDisplay( MafiaState.ERROR, UseItemRequest.lastUpdate );
				return;
			}

			break;

		case ItemPool.GINGERBREAD_CITY:

			Preferences.setBoolean( "gingerbreadCityAvailable", true );
			if ( !responseText.contains( "build a gingerbread city" ) )
			{
				return;
			}
			break;

		case ItemPool.COUNTERFEIT_CITY:
			if ( responseText.contains( "already a gingerbread city" ) )
			{
				// If you already have access it is not consumed
				return;
			}
			Preferences.setBoolean( "_gingerbreadCityToday", true );
			break;

		case ItemPool.TELEGRAPH_OFFICE_DEED:

			Preferences.setBoolean( "telegraphOfficeAvailable", true );

			// You find a vacant lot on the Right Side of the
			// Tracks and a loophole in the local tax code that
			// lets you purchase it for negative 19 Meat. Then you
			// build a telegraph office out of reclaimed materials
			// from the city dump, spend 19 Meat on a can of paint,
			// and before you know it you're in the telegraph
			// business!

			if ( !responseText.contains( "You find a vacant lot" ) )
			{
				UseItemRequest.lastUpdate = "You've already opened a telegraph office.";
				KoLmafia.updateDisplay( MafiaState.ERROR, UseItemRequest.lastUpdate );
				return;
			}

			break;

		case ItemPool.HEART_SHAPED_CRATE:
			Preferences.setBoolean( "loveTunnelAvailable", true );
			if ( !responseText.contains( "You wander" ) )
			{
				UseItemRequest.lastUpdate = "You've already opened a Tunnel of L.O.V.E.";
				KoLmafia.updateDisplay( MafiaState.ERROR, UseItemRequest.lastUpdate );
				return;
			}
			break;

		case ItemPool.BEAUTIFUL_RAINBOW:
		{
			if ( responseText.contains( "don't have the item you're trying to use" ) )
			{
				UseItemRequest.lastUpdate = "You've haven't got that item.";
				KoLmafia.updateDisplay( MafiaState.ERROR, UseItemRequest.lastUpdate );
				return;
			}

			if ( !responseText.contains( "eaten the entire thing" ) )
			{
				UseItemRequest.lastUpdate = "You've already maxed out Belch The Rainbow.";
				KoLmafia.updateDisplay( MafiaState.ERROR, UseItemRequest.lastUpdate );
				Preferences.setInteger( "skillLevel117", 11 );
				return;
			}

			ResponseTextParser.learnSkill( "Belch The Rainbow" );
			break;
		}

		case ItemPool.THUNDER_THIGH:
		case ItemPool.AQUA_BRAIN:
		case ItemPool.LIGHTNING_MILK:
		{
			// You can't learn anything else from this, so you just throw it away.
			if ( !responseText.contains( "you just throw it away" ) )
			{
				return;
			}
			break;
		}

		case ItemPool.OLFACTION_BOOK:
		{
			if ( !responseText.contains( "smell has been elevated to a superhuman level" ) )
			{
				UseItemRequest.lastUpdate = "You can't learn that skill.";
				KoLmafia.updateDisplay( MafiaState.ERROR, UseItemRequest.lastUpdate );
				return;
			}

			ResponseTextParser.learnSkill( "Transcendent Olfaction" );
			break;
		}

		case ItemPool.TEACHINGS_OF_THE_FIST:

			// You learn a different skill from each scroll
			Preferences.increment( "fistSkillsKnown", 1 );
			ResponseTextParser.learnSkillFromResponse( responseText );
			break;

		case ItemPool.BOOKE_OF_VAMPYRIC_KNOWLEDGE:
			// You learn a different skill depending on class
			// You stare at the pages of the book, trying to decipher the crimson runes written on them. Your brain doesn't understand them, but your blood does.
			// You've already learned all the darke secrettes this book has to offer you.
			if ( responseText.contains( "already learned all the darke secrettes" ) )
			{
				UseItemRequest.lastUpdate = "You've already learned the blood skill for your class.";
				KoLmafia.updateDisplay( MafiaState.ERROR, UseItemRequest.lastUpdate );
				return;
			}
			// This book is just a bunch of gibberish, written in blood.
			if ( responseText.contains( "bunch of gibberish" ) )
			{
				UseItemRequest.lastUpdate = "That book has nothing to teach your class.";
				KoLmafia.updateDisplay( MafiaState.ERROR, UseItemRequest.lastUpdate );
				return;
			}
			ResponseTextParser.learnSkillFromResponse( responseText );
			break;

		case ItemPool.SLIME_SOAKED_HYPOPHYSIS:
		case ItemPool.SLIME_SOAKED_BRAIN:
		case ItemPool.SLIME_SOAKED_SWEAT_GLAND:
		{
			for ( int i = 46; i <= 48; ++i )
			{
				GenericRequest req = new GenericRequest( "desc_skill.php?whichskill=" + i + "&self=true" );
				RequestThread.postRequest( req );
			}

			// You can learn the appropriate skill up to 10 times.
			// What does it say if you try to use the 11th?
			if ( !responseText.contains( "You gain a skill" ) )
			{
				// Item may be consumed even if you already have the skill
				break;
			}

			String skill = UseItemRequest.itemToSkill( item.getItemId() );
			ResponseTextParser.learnSkill( skill );

			break;
		}

		case ItemPool.TELESCOPE:
			// We've added or upgraded our telescope
			KoLCharacter.setTelescope( true );

			// Look through it to check number of upgrades
			Preferences.setInteger( "lastTelescopeReset", -1 );
			KoLCharacter.checkTelescope();
			break;

		case ItemPool.ASTRAL_MUSHROOM:

			// "You eat the mushroom, and are suddenly engulfed in
			// a whirling maelstrom of colors and sensations as
			// your awareness is whisked away to some strange
			// alternate dimension. Who would have thought that a
			// glowing, ethereal mushroom could have that kind of
			// effect?"

			// "Whoo, man, lemme tell you, you don't need to be
			// eating another one of those just now, okay?"

			if ( !responseText.contains( "whirling maelstrom" ) )
			{
				return;
			}

			break;

		case ItemPool.WORKYTIME_TEA:

			// You're not quite bored enough to drink that much tea.

			if ( responseText.contains( "not quite bored enough" ) )
			{
				UseItemRequest.lastUpdate = "You're not bored enough to drink that much tea.";
				KoLmafia.updateDisplay( MafiaState.ERROR, UseItemRequest.lastUpdate );
				return;
			}

			break;

		case ItemPool.ABSINTHE:

			// "You drink the bottle of absinthe. It tastes like
			// licorice, pain, and green. Your head begins to ache
			// and you see a green glow in the general direction of
			// the distant woods."

			// "No way are you gonna drink another one of those
			// until the last one wears off."

			if ( !responseText.contains( "licorice" ) )
			{
				return;
			}

			TurnCounter.startCounting( 1, "Wormwood loc=151 loc=152 loc=153 place.php?whichplace=wormwood", "tinybottle.gif" );
			TurnCounter.startCounting( 5, "Wormwood loc=151 loc=152 loc=153 place.php?whichplace=wormwood", "tinybottle.gif" );
			TurnCounter.startCounting( 9, "Wormwood loc=151 loc=152 loc=153 place.php?whichplace=wormwood", "tinybottle.gif" );

			break;

		case ItemPool.DUSTY_ANIMAL_SKULL:

			// The magic that had previously animated the animals kicks back
			// in, and it stands up shakily and looks at you. "Graaangh?"

			if ( !responseText.contains( "Graaangh?" ) )
			{
				UseItemRequest.lastUpdate = "You're missing some parts.";
				KoLmafia.updateDisplay( MafiaState.ERROR, UseItemRequest.lastUpdate );
				return;
			}

			// Remove the other 98 bones

			for ( int i = 1802; i < 1900; ++i )
			{
				ResultProcessor.removeItem( i );
			}

			break;

		case ItemPool.DRUM_MACHINE:

			// "And dammit, your hooks were still on there! Oh well."

			if ( responseText.contains( "hooks were still on" ) )
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
					ResultProcessor.removeItem( ItemPool.WORM_RIDING_HOOKS );
				}

				int gnasirProgress = Preferences.getInteger( "gnasirProgress" );
				gnasirProgress |= 16;
				Preferences.setInteger( "gnasirProgress", gnasirProgress );

				QuestManager.incrementDesertExploration( 30 );
				break;
			}

			// "You don't have time to play the drums."
			if ( responseText.contains( "don't have time" ) )
			{
				UseItemRequest.lastUpdate = "Insufficient adventures left.";
			}

			// "You're too beaten-up to play the drums."
			else if ( responseText.contains( "too beaten up" ) )
			{
				UseItemRequest.lastUpdate = "Too beaten up.";
			}

			// "You head to your campsite and crank up the drum
			// machine. You press buttons at random, waiting for
			// something interesting to happen, but you only
			// succeed in annoying your neighbors."
			else if ( responseText.contains( "head to your campsite" ) )
			{
				UseItemRequest.lastUpdate = "Can't find the beach";
			}

			if ( !UseItemRequest.lastUpdate.equals( "" ) )
			{
				KoLmafia.updateDisplay( MafiaState.ERROR, UseItemRequest.lastUpdate );
			}

			// If we are redirected to a fight, the item is
			// consumed elsewhere. If we got here, it wasn't
			// actually consumed

			return;

		case ItemPool.CURSED_PIECE_OF_THIRTEEN:

			// "You take the piece of thirteen to a rare coin
			// dealer in Seaside Town (he's got a shop set up right
			// next to that library across the street from the
			// Sleazy Back Alley) to see what you can get for
			// it. Turns out you can get X Meat for it."
			if ( responseText.contains( "rare coin dealer in Seaside Town" ) )
			{
				break;
			}

			// You consider taking the piece of thirteen to a rare
			// coin dealer to see if it's worth anything, but you
			// don't really have time.
			if ( responseText.contains( "don't really have time" ) )
			{
				UseItemRequest.lastUpdate = "Insufficient adventures left.";
			}

			// "You consider taking the piece of thirteen to a rare
			// coin dealer to see if it's worth anything, but
			// you're feeling pretty crappy right now."
			else if ( responseText.contains( "feeling pretty crappy" ) )
			{
				UseItemRequest.lastUpdate = "Too beaten up.";
			}

			if ( !UseItemRequest.lastUpdate.equals( "" ) )
			{
				KoLmafia.updateDisplay( MafiaState.ERROR, UseItemRequest.lastUpdate );
			}

			// If we are redirected to a fight, the item is
			// consumed elsewhere. If we got here, it wasn't
			// actually consumed

			return;

		case ItemPool.SPOOKY_PUTTY_MONSTER:

			// You can't tell what this is supposed to be a copy
			// of. You squish it back into a sheet.

			if ( responseText.contains( "squish it back into a sheet" ) )
			{
				Preferences.setString( "spookyPuttyMonster", "" );
				break;
			}

			// If we are redirected to a fight, the item is
			// consumed elsewhere. If we got here, it wasn't
			// actually consumed

			return;

		case ItemPool.D10:

			// You don't have time to go on an adventure. Even an imaginary one.
			if ( responseText.contains( "don't have time" ) )
			{
				UseItemRequest.lastUpdate = "Insufficient adventures left.";
			}

			// Your imagination is too drunk right now.
			else if ( responseText.contains( "Your imagination is too drunk" ) )
			{
				UseItemRequest.lastUpdate = "Inebriety limit reached.";
			}

			// Using one of these items will eventually do
			// something. I am sorry that eventually is not now,
			// but I ran out of time before KoL Con.
			else if ( responseText.contains( "eventually is not now" ) )
			{
				UseItemRequest.lastUpdate = "Not yet implemented.";
			}

			else
			{
				break;
			}

			KoLmafia.updateDisplay( MafiaState.ERROR, UseItemRequest.lastUpdate );

			return;

		case ItemPool.D12:
		{

			// You draw the bow and roll [X]d12 to see how far the arrow flies.

			Matcher m = ARROW_PATTERN.matcher( responseText );
			String distance = m.find() ? m.group( 1 ) : "";

			// It goes [Xd12] feet, and just as it's about to hit the ground,
			// a cart with a big target in it is pulled into view and
			// the arrow hits it dead center. BULLSEYE.

			if ( responseText.contains( "BULLSEYE" ) )
			{
				String message = "You get a bullseye at " + distance + " feet.";
				KoLmafia.updateDisplay( message );
				RequestLogger.updateSessionLog( message );
				break;
			}

			// It goes [Xd12] feet, and doesn't hit anything interesting.
			// You grumble and put the dice away.
			else if ( responseText.contains( "You grumble and put the dice away" ) )
			{
				UseItemRequest.lastUpdate = "You grumble and put the dice away.";
			}

			// Y'know, you're never going to be able to top what happened last time. That was awesome.
			else if ( responseText.contains( "That was awesome" ) )
			{
				UseItemRequest.lastUpdate = "You already hit a bullseye.";
			}

			// If some unknown message, assume we use up the dice.

			else
			{
				break;
			}

			KoLmafia.updateDisplay( MafiaState.ERROR, UseItemRequest.lastUpdate );

			return;
		}

		case ItemPool.D20:

			// You already rolled for initiative.
			if ( responseText.contains( "You already rolled for initiative" ) )
			{
				UseItemRequest.lastUpdate = "You already rolled for initiative";
			}

			// You can't figure out a good way to roll that
			// quantity of 20-sided dice. Maybe you should've paid
			// less attention in gym class.

			else if ( responseText.contains( "Maybe you should've paid less attention in gym class" ) )
			{
				UseItemRequest.lastUpdate = "Rolling that many d20s doesn't do anything interesting.";
			}

			else
			{
				break;
			}

			KoLmafia.updateDisplay( MafiaState.ERROR, UseItemRequest.lastUpdate );

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

			if ( responseText.contains( "You're sick of playing with BRICKOs today" ) )
			{
				Preferences.setInteger( "_brickoFights", 10 );
				UseItemRequest.lastUpdate = "You're sick of playing with BRICKOs today";
				KoLmafia.updateDisplay( UseItemRequest.lastUpdate );
				return;
			}

			// You're too drunk to mess with BRICKO right now.

			// If we are redirected to a fight, the item is
			// consumed elsewhere. If we got here, it wasn't
			// actually consumed

			return;

		case ItemPool.RECORDING_BALLAD:
		case ItemPool.RECORDING_BENETTON:
		case ItemPool.RECORDING_CHORALE:
		case ItemPool.RECORDING_DONHO:
		case ItemPool.RECORDING_ELRON:
		case ItemPool.RECORDING_INIGO:
		case ItemPool.RECORDING_PRELUDE:

			// You already have too many songs stuck in your head.

			if ( responseText.contains( "too many songs stuck in your head" ) )
			{
				UseItemRequest.lastUpdate = "You have the maximum number of AT buffs already.";
				KoLmafia.updateDisplay( MafiaState.ERROR, UseItemRequest.lastUpdate );
				return;
			}

			break;

		case ItemPool.FOSSILIZED_BAT_SKULL:
		case ItemPool.FOSSILIZED_SERPENT_SKULL:
		case ItemPool.FOSSILIZED_BABOON_SKULL:
		case ItemPool.FOSSILIZED_WYRM_SKULL:
		case ItemPool.FOSSILIZED_DEMON_SKULL:
		case ItemPool.FOSSILIZED_SPIDER_SKULL:

			// If we are redirected to a fight, the item is
			// consumed elsewhere. If we got here, it wasn't
			// actually consumed

			return;

		case ItemPool.ICE_SCULPTURE:

			Preferences.setBoolean( "_iceSculptureUsed", true );

			// Ice sculptures are a form of entertainment best
			// served sparingly. You should wait until tomorrow.

			// If we are redirected to a fight, the item is
			// consumed elsewhere. If we got here, it wasn't
			// actually consumed

			return;

		case ItemPool.SHAKING_CAMERA:

			Preferences.setBoolean( "_cameraUsed", true );

			// You get the sense that the monster in this camera
			// isn't ready to be developed just yet. It'll probably
			// be ready tomorrow. And no, you can't speed it up by
			// blowing on it.

			// If we are redirected to a fight, the item is
			// consumed elsewhere. If we got here, it wasn't
			// actually consumed

			return;

		case ItemPool.SHAKING_CRAPPY_CAMERA:

			Preferences.setBoolean( "_crappyCameraUsed", true );

			// You get the sense that the monster in this camera
			// isn't ready to be developed just yet. It'll probably
			// be ready tomorrow. And no, you can't speed it up by
			// blowing on it.

			// If we are redirected to a fight, the item is
			// consumed elsewhere. If we got here, it wasn't
			// actually consumed

			return;

		case ItemPool.PHOTOCOPIED_MONSTER:

			Preferences.setBoolean( "_photocopyUsed", true );

			// You get nauseated just thinking about the smell of
			// copier toner.  You don't think you can handle
			// another one of these things today.

			// If we are redirected to a fight, the item is
			// consumed elsewhere. If we got here, it wasn't
			// actually consumed

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

			if ( !responseText.contains( "you drop your pants and giggle" ) )
			{
				return;
			}

			Preferences.setString( "photocopyMonster", "Your butt" );
			break;

		case ItemPool.BGE_TATTOO:

			// You've already got one of those tattoos on.
			// You should give this one to somebody who will
			// appreciate it more.

			if ( responseText.contains( "You've already got one of those tattoos on" ) )
			{
				return;
			}

			break;

		case ItemPool.DOLPHIN_WHISTLE:

			// If we are redirected to a fight, the item is
			// consumed elsewhere. If we got here, it wasn't
			// actually consumed

			return;

		case ItemPool.RUSTY_HEDGE_TRIMMERS:

			// If we are redirected to a choice, the item is
			// consumed elsewhere. If we got here, it wasn't
			// actually consumed

			return;

		case ItemPool.LYNYRD_SNARE:

			// If we are redirected to a fight, the item is
			// consumed elsewhere. If we got here, it wasn't
			// actually consumed

			Preferences.setInteger( "_lynyrdSnareUses", 3 );
			return;

		case ItemPool.MOJO_FILTER:

			// You strain some of the toxins out of your mojo, and
			// discard the now-grodulated filter.

			if ( !responseText.contains( "now-grodulated" ) )
			{
				return;
			}

			Preferences.increment( "currentMojoFilters", count );

			KoLCharacter.setSpleenUse( KoLCharacter.getSpleenUse() - count );

			KoLCharacter.updateStatus();
			ConcoctionDatabase.getUsables().sort();

			break;

		case ItemPool.SPICE_MELANGE:

			// You pop the spice melange into your mouth and chew it up.
			if ( responseText.contains( "too scared to eat any more of that stuff today" ) )
			{
				Preferences.setBoolean( "spiceMelangeUsed", true );
				return;
			}

			if ( !responseText.contains( "You pop the spice melange into your mouth and chew it up" ) )
			{
				return;
			}

			KoLCharacter.setFullness( KoLCharacter.getFullness() - 3 );
			KoLCharacter.setInebriety( Math.max( 0, KoLCharacter.getInebriety() - 3 ) );
			Preferences.setBoolean( "spiceMelangeUsed", true );
			KoLCharacter.updateStatus();
			ConcoctionDatabase.getUsables().sort();

			break;

		case ItemPool.ULTRA_MEGA_SOUR_BALL:

			// You pop the candy in your mouth, and it immediately absorbs almost all of the moisture in your body.
			if ( responseText.contains( "too scared to eat any more of that candy today" ) )
			{
				Preferences.setBoolean( "_ultraMegaSourBallUsed", true );
				return;
			}

			if ( !responseText.contains( "You pop the candy in your mouth, and it immediately absorbs almost all of the moisture in your body" ) )
			{
				return;
			}

			KoLCharacter.setFullness( KoLCharacter.getFullness() - 3 );
			KoLCharacter.setInebriety( Math.max( 0, KoLCharacter.getInebriety() - 3 ) );
			Preferences.setBoolean( "_ultraMegaSourBallUsed", true );
			KoLCharacter.updateStatus();
			ConcoctionDatabase.getUsables().sort();

			break;

		case ItemPool.ALIEN_ANIMAL_MILK:
			Preferences.setBoolean( "_alienAnimalMilkUsed", true );
			KoLCharacter.setFullness( KoLCharacter.getFullness() - 3 );
			KoLCharacter.updateStatus();
			ConcoctionDatabase.getUsables().sort();
			break;

		case ItemPool.ALIEN_PLANT_POD:
			Preferences.setBoolean( "_alienPlantPodUsed", true );
			KoLCharacter.setInebriety( Math.max( 0, KoLCharacter.getInebriety() - 3 ) );
			KoLCharacter.updateStatus();
			ConcoctionDatabase.getUsables().sort();
			break;

		case ItemPool.SYNTHETIC_DOG_HAIR_PILL:

			//Your liver feels better! And quivers a bit.
			if ( responseText.contains( "liver can't take any more abuse" ) )
			{
				Preferences.setBoolean( "_syntheticDogHairPillUsed", true );
				return;
			}

			if ( !responseText.contains( "quivers" ) )
			{
				return;
			}

			KoLCharacter.setInebriety( Math.max( 0, KoLCharacter.getInebriety() - 1 ) );
			Preferences.setBoolean( "_syntheticDogHairPillUsed", true );
			KoLCharacter.updateStatus();
			ConcoctionDatabase.getUsables().sort();

			break;

		case ItemPool.DISTENTION_PILL:

			// Your stomach feels rather stretched out
			if ( responseText.contains( "stomach can't take any more abuse" ) )
			{
				Preferences.setBoolean( "_distentionPillUsed", true );
				return;
			}

			if ( !responseText.contains( "stomach feels rather stretched" ) )
			{
				return;
			}

			Preferences.setBoolean( "_distentionPillUsed", true );
			KoLCharacter.updateStatus();
			ConcoctionDatabase.getUsables().sort();

			break;

		case ItemPool.MILK_OF_MAGNESIUM:

			// You've already had some of this stuff today, and it
			// was pretty hard on the old gullet.  Best wait until
			// tomorrow to go through that again.

			if ( responseText.contains( "hard on the old gullet" ) )
			{
				Preferences.setBoolean( "_milkOfMagnesiumUsed", true );
				return;
			}

			// You swallow the liquid.  You stomach immediately
			// begins to churn, and all the wrinkles in your shirt
			// smooth out from the heat radiating from your abdomen
			if ( !responseText.contains( "stomach immediately begins to churn" ) )
			{
				return;
			}

			Preferences.setBoolean( "_milkOfMagnesiumUsed", true );
			KoLCharacter.updateStatus();
			ConcoctionDatabase.getUsables().sort();
			ConcoctionDatabase.queuedFood.touch();

			break;

		case ItemPool.NEWBIESPORT_TENT:
		case ItemPool.BARSKIN_TENT:
		case ItemPool.COTTAGE:
		case ItemPool.BRICKO_PYRAMID:
		case ItemPool.HOUSE:
		case ItemPool.SANDCASTLE:
		case ItemPool.TWIG_HOUSE:
		case ItemPool.GINGERBREAD_HOUSE:
		case ItemPool.HOBO_FORTRESS:
		case ItemPool.GIANT_FARADAY_CAGE:
		case ItemPool.SNOW_FORT:
		case ItemPool.ELEVENT:
		case ItemPool.RESIDENCE_CUBE:
		case ItemPool.GIANT_PILGRIM_HAT:
		case ItemPool.HOUSE_SIZED_MUSHROOM:

			if ( responseText.contains( "You've already got" ) )
			{
				return;
			}

			CampgroundRequest.destroyFurnishings();
			CampgroundRequest.setCurrentDwelling( itemId );
			break;

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
				return;
			}

			CampgroundRequest.setCampgroundItem( itemId, 1 );
			break;

		case ItemPool.CLOCKWORK_MAID:

			if ( responseText.contains( "You've already got" ) )
			{
				return;
			}

			CampgroundRequest.removeCampgroundItem( ItemPool.get( ItemPool.MAID, 1 ) );
			CampgroundRequest.setCampgroundItem( ItemPool.CLOCKWORK_MAID, 1 );
			break;

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
				return;
			}

			CampgroundRequest.setCurrentBed( ItemPool.get( itemId, 1 ) );
			CampgroundRequest.setCampgroundItem( itemId, 1 );
			break;

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
				if ( responseText.contains( strings[i][2] ) )
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
			if ( responseText.contains( "You decide not to drink it" ) )
			{
				return;
			}

			break;

		case ItemPool.VIAL_OF_RED_SLIME:
		case ItemPool.VIAL_OF_YELLOW_SLIME:
		case ItemPool.VIAL_OF_BLUE_SLIME:

			strings = ItemPool.slimeVialStrings[0];
			for ( int i = 0; i < strings.length; ++i )
			{
				if ( responseText.contains( strings[i][1] ) )
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
			break;

		case ItemPool.VIAL_OF_ORANGE_SLIME:
		case ItemPool.VIAL_OF_GREEN_SLIME:
		case ItemPool.VIAL_OF_VIOLET_SLIME:

			strings = ItemPool.slimeVialStrings[1];
			for ( int i = 0; i < strings.length; ++i )
			{
				if ( responseText.contains( strings[i][1] ) )
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
			break;

		case ItemPool.VIAL_OF_VERMILION_SLIME:
		case ItemPool.VIAL_OF_AMBER_SLIME:
		case ItemPool.VIAL_OF_CHARTREUSE_SLIME:
		case ItemPool.VIAL_OF_TEAL_SLIME:
		case ItemPool.VIAL_OF_INDIGO_SLIME:
		case ItemPool.VIAL_OF_PURPLE_SLIME:

			strings = ItemPool.slimeVialStrings[2];
			for ( int i = 0; i < strings.length; ++i )
			{
				if ( responseText.contains( strings[i][1] ) )
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
			break;

		case ItemPool.ANCIENT_CURSED_FOOTLOCKER:

			if ( !InventoryManager.hasItem( ItemPool.SIMPLE_CURSED_KEY ) )
			{
				return;
			}

			ResultProcessor.processItem( ItemPool.SIMPLE_CURSED_KEY, -1 );
			break;

		case ItemPool.ORNATE_CURSED_CHEST:

			if ( !InventoryManager.hasItem( ItemPool.ORNATE_CURSED_KEY ) )
			{
				return;
			}

			ResultProcessor.processItem( ItemPool.ORNATE_CURSED_KEY, -1 );
			break;

		case ItemPool.GILDED_CURSED_CHEST:
			if ( !InventoryManager.hasItem( ItemPool.GILDED_CURSED_KEY ) )
			{
				return;
			}

			ResultProcessor.processItem( ItemPool.GILDED_CURSED_KEY, -1 );
			break;

		case ItemPool.STUFFED_CHEST:
			if ( !InventoryManager.hasItem( ItemPool.STUFFED_KEY ) )
			{
				return;
			}

			ResultProcessor.processItem( ItemPool.STUFFED_KEY, -1 );
			break;

		case ItemPool.GENERAL_ASSEMBLY_MODULE:

			//  "INSUFFICIENT RESOURCES LOCATED TO DISPENSE CRIMBO
			//  CHEER. PLEASE LOCATE A VITAL APPARATUS VENT AND
			//  REQUISITION APPROPRIATE MATERIALS."

			if ( responseText.contains( "INSUFFICIENT RESOURCES LOCATED" ) )
			{
				return;
			}

			// You breathe a heavy sigh of relief as the pseudopods
			// emerge from your inventory, carrying the laser
			// cannon, laser targeting chip, and the set of
			// Unobtainium straps that you acquired earlier from
			// the Sinister Dodecahedron.

			if ( responseText.contains( "carrying the  laser cannon" ) )
			{
				ResultProcessor.processResult( ItemPool.get( ItemPool.LASER_CANON, -1 ) );
				ResultProcessor.processResult( ItemPool.get( ItemPool.LASER_TARGETING_CHIP, -1 ) );
				ResultProcessor.processResult( ItemPool.get( ItemPool.UNOBTAINIUM_STRAPS, -1 ) );
			}

			// You breathe a heavy sigh of relief as the pseudopods
			// emerge from your inventory, carrying the polymorphic
			// fastening apparatus, hi-density nylocite leg armor,
			// and the silicon-infused gluteal shield that you
			// acquired earlier from the Sinister Dodecahedron.

			else if ( responseText.contains( "carrying the  polymorphic fastening apparatus" ) )
			{
				ResultProcessor.processResult( ItemPool.get( ItemPool.FASTENING_APPARATUS, -1 ) );
				ResultProcessor.processResult( ItemPool.get( ItemPool.LEG_ARMOR, -1 ) );
				ResultProcessor.processResult( ItemPool.get( ItemPool.GLUTEAL_SHIELD, -1 ) );
			}

			// You breathe a heavy sigh of relief as the pseudopods
			// emerge from your inventory, carrying the carbonite
			// visor, plexifoam chin strap, and the kevlateflocite
			// helmet that you acquired earlier from the Sinister
			// Dodecahedron.

			else if ( responseText.contains( "carrying the carbonite visor" ) )
			{
				ResultProcessor.processResult( ItemPool.get( ItemPool.CARBONITE_VISOR, -1 ) );
				ResultProcessor.processResult( ItemPool.get( ItemPool.CHIN_STRAP, -1 ) );
				ResultProcessor.processResult( ItemPool.get( ItemPool.KEVLATEFLOCITE_HELMET, -1 ) );
			}

			break;

		case ItemPool.AUGMENTED_DRONE:

			if ( responseText.contains( "You put an overcharged sphere in the cavity" ) )
			{
				ResultProcessor.processResult( ItemPool.get( ItemPool.OVERCHARGED_POWER_SPHERE, -1 ) );
			}

			break;

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
			if ( !responseText.contains( "A voice emerges" ) )
			{
				// You try to stick the punchcard into <name>
				// and fail.
				return;
			}

			// We should save the state of the Megadrone
			break;

		case ItemPool.OUTRAGEOUS_SOMBRERO:
			Preferences.setBoolean( "outrageousSombreroUsed", true );
			return;

		case ItemPool.TRAPEZOID:
			if ( responseText.contains( "you put it on the ground at your campsite" ) )
			{
				CampgroundRequest.setCampgroundItem( ItemPool.TRAPEZOID, 1 );
				break;
			}
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
			if ( responseText.contains( "filled with revulsion" ) )
			{
				return;
			}

			break;

		case ItemPool.PERSONAL_MASSAGER:

			// You don't really need a massage right now, as your
			// neck and back aren't feeling particularly kinky.
			if ( responseText.contains( "don't really need a massage" ) )
			{
				return;
			}
			break;

		case ItemPool.BURROWGRUB_HIVE:

			// One way or another, you have used it today
			Preferences.setBoolean( "burrowgrubHiveUsed", true );

			// You pick up the burrowgrub hive to look inside it,
			// and a bunch of the grubs crawl out of it and burrow
			// under your skin.  It's horrifying.  You can still
			// feel them in there. Gah.

			if ( responseText.contains( "It's horrifying." ) )
			{
				// You have three grub summons left today
				Preferences.setInteger( "burrowgrubSummonsRemaining", 3 );
			}

			return;

		case ItemPool.BOOZEHOUND_TOKEN:

			// You'd take this thing to a bar and see what you can
			// trade it in for, but you don't know where any bars
			// are.

			if ( responseText.contains( "don't know where any bars are" ) )
			{
				return;
			}

			break;

		case ItemPool.SMALL_LAMINATED_CARD:
		case ItemPool.LITTLE_LAMINATED_CARD:
		case ItemPool.NOTBIG_LAMINATED_CARD:
		case ItemPool.UNLARGE_LAMINATED_CARD:

			DwarfFactoryRequest.useLaminatedItem( item.getItemId(), responseText );
			return;

		case ItemPool.DWARVISH_DOCUMENT:
		case ItemPool.DWARVISH_PAPER:
		case ItemPool.DWARVISH_PARCHMENT:

			DwarfFactoryRequest.useUnlaminatedItem( item.getItemId(), responseText );
			return;

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
			// consumed elsewhere. If we got here, it wasn't
			// actually consumed

		case ItemPool.DEPLETED_URANIUM_SEAL:

			// You've summoned too many Infernal seals today. Any
			// more and you're afraid the corruption will be too
			// much for you to bear.

			if ( responseText.contains( "too many Infernal seals" ) )
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

			else if ( responseText.contains( "Brotherhood of the Smackdown" ) )
			{
				UseItemRequest.lastUpdate = "You need more seal-blubber candles.";
			}

			// In order to perform this summoning ritual, you need
			// 1 imbued seal-blubber candle.

			else if ( responseText.contains( "you need 1 imbued seal-blubber candle" ) )
			{
				UseItemRequest.lastUpdate = "You need an imbued seal-blubber candle.";
			}

			// Only Seal Clubbers may use this item.

			else if ( responseText.contains( "Only Seal Clubbers may use this item." ) )
			{
				UseItemRequest.lastUpdate = "Only Seal Clubbers may use this item.";
			}

			// You need to be at least level 6 to use that item

			else if ( responseText.contains( "need to be at least level" ) )
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
			if ( !responseText.contains( "formidable club" ) )
			{
				return;
			}

			break;

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
			if ( responseText.contains( "Evilometer emits three quick beeps" ) )
			{
				int evilness = Math.min( Preferences.getInteger( "cyrptNookEvilness" ), 3*count );
				Preferences.increment( "cyrptNookEvilness", -evilness );
				Preferences.increment( "cyrptTotalEvilness", -evilness );
			}
			break;

		case ItemPool.QUASIRELGIOUS_SCULPTURE:
		case ItemPool.SOLID_GOLD_ROSARY:
			if ( responseText.contains( "entire Cyrpt feels safer" ) )
			{
				// Can't abuse this, so queue a use item to get new Evilometer value
				RequestThread.postRequest( UseItemRequest.getInstance( ItemPool.EVILOMETER ) );
			}
			break;
			
		case ItemPool.KEYOTRON:
			UseItemRequest.getBugbearBiodataLevels( responseText );
			return;

		case ItemPool.PEN_PAL_KIT:
			// You've already got a pen pal. There's no way you
			// could handle the pressure of contantly forgetting to
			// reply to two kids from Distant Lands...
			if ( responseText.contains( "already got a pen pal" ) )
			{
				return;
			}
			break;

		case ItemPool.NEW_YOU_CLUB_MEMBERSHIP_FORM:
			// Per the instructions on the back of the card, you
			// place your hand over your heart and recite the
			// membership oath: "I'm good enough, I'm smart enough,
			// and doggone it, people like me!" You feel affirmed
			// already!
			if ( !responseText.contains( "instructions on the back of the card" ) )
			{
				return;
			}
			break;

		case ItemPool.HONEYPOT:
			// You gain the "Float Like a Butterfly, Smell Like a
			// Bee" effect.	 This prevents bees from appearing
			// while it is active.	As soon as it wears off, a bee
			// will appear.	 Stop the bee counters, since turns
			// remaining of the effect give the same info.
			TurnCounter.stopCounting( "Bee window begin" );
			TurnCounter.stopCounting( "Bee window end" );
			break;

		case ItemPool.RONALD_SHELTER_MAP:
		case ItemPool.GRIMACE_SHELTER_MAP:
			// If we are redirected to a choice, the item is
			// consumed elsewhere.
			break;

		case ItemPool.BORROWED_TIME:
			// Set the preference to true both when we fail and succeed.
			Preferences.setBoolean( "_borrowedTimeUsed", true );

			if ( responseText.contains( "already borrowed some time today" ) )
			{
				return;
			}

			// You dip into your future and borrow some time. Be sure to spend it wisely!
			if ( responseText.contains( "dip into your future" ) )
			{
				KoLCharacter.updateStatus();
				Preferences.increment( "extraRolloverAdventures", -20 );
			}
			break;

		case ItemPool.MOVEABLE_FEAST:

			// The table is looking pretty bare -- you should wait
			// until tomorrow, and let some of the food magically regenerate.
			if ( responseText.contains( "wait until tomorrow" ) )
			{
				Preferences.setInteger( "_feastUsed", 5 );
			}

			// <name> chows down on the moveable feast,
			// then leans back, sighs, and loosens his belt a couple of notches.
			else if ( responseText.contains( "chows down" ) )
			{
				Preferences.increment( "_feastUsed", 1 );

				String familiar = KoLCharacter.getFamiliar().getRace();
				String oldList = Preferences.getString( "_feastedFamiliars" );
				String newList = oldList + ( oldList.equals( "" ) ? "" : ";" ) + familiar;
				Preferences.setString( "_feastedFamiliars", newList );
			}
			return;

		case ItemPool.STAFF_GUIDE:

			if ( responseText.contains( "You don't have time to screw around in a haunted house" ) )
			{
				UseItemRequest.lastUpdate = "Insufficient adventures to use a staff guide.";
				KoLmafia.updateDisplay( MafiaState.ERROR, UseItemRequest.lastUpdate );
				return;
			}

			if ( responseText.contains( "You aren't allowed to go to any Haunted Houses right now" ) )
			{
				UseItemRequest.lastUpdate = "You aren't allowed to go to any Haunted Houses right now.";
				KoLmafia.updateDisplay( MafiaState.ERROR, UseItemRequest.lastUpdate );
				return;
			}

			if ( responseText.contains( "You don't know where any haunted sorority houses are right now." ) ||
			     responseText.contains( "No way. It's boring in there now that everybody is dead." ) )
			{
				UseItemRequest.lastUpdate = "The Haunted Sorority House is unavailable.";
				KoLmafia.updateDisplay( MafiaState.ERROR, UseItemRequest.lastUpdate );
				return;
			}

			break;

		case ItemPool.GHOSTLY_BODY_PAINT:
		case ItemPool.NECROTIZING_BODY_SPRAY:
		case ItemPool.BITE_LIPSTICK:
		case ItemPool.WHISKER_PENCIL:
		case ItemPool.PRESS_ON_RIBS:

			if ( responseText.contains( "You've already got a sexy costume on" ) )
			{
				UseItemRequest.lastUpdate = "You've already got a sexy costume on.";
				KoLmafia.updateDisplay( MafiaState.ERROR, UseItemRequest.lastUpdate );
				return;
			}
			break;

		case ItemPool.BLACK_PAINT:

			if ( KoLCharacter.inFistcore() && responseText.contains( "Your teachings forbid the use of black paint." ) )
			{
				UseItemRequest.lastUpdate = "Your teachings forbid the use of black paint.";
				KoLmafia.updateDisplay( MafiaState.ERROR, UseItemRequest.lastUpdate );
				return;
			}
			break;

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

			if ( responseText.contains( "already feeling adventurous enough" ) )
			{
				// player has already used 5 resolutions today
				int extraAdv = 10 - Preferences.getInteger( "_resolutionAdv" );
				Preferences.increment( "extraRolloverAdventures", extraAdv );
				Preferences.increment( "_resolutionAdv", extraAdv );
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

			if ( used < count )
			{
				item = item.getInstance( used );
			}

			break;

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
		case ItemPool.THANKSGARDEN_SEEDS:
		case ItemPool.TALL_GRASS_SEEDS:
		case ItemPool.MUSHROOM_SPORES:

			if ( Limitmode.limitCampground() || KoLCharacter.isEd() || KoLCharacter.inNuclearAutumn() )
			{
				return;
			}

			CampgroundRequest.clearCrop();
			RequestThread.postRequest( new CampgroundRequest() );
			break;

		case ItemPool.ESSENTIAL_TOFU:
			Preferences.setBoolean( "_essentialTofuUsed", true );
			break;

		case ItemPool.CHOCOLATE_CIGAR:
			if ( responseText.contains( "You light the end" ) )
			{
				Preferences.setInteger( "_chocolateCigarsUsed", 1 );
			}
			else if ( responseText.contains( "This one doesn't taste" ) )
			{
				Preferences.setInteger( "_chocolateCigarsUsed", 2 );
			}
			else
			{
				Preferences.setInteger( "_chocolateCigarsUsed", 3 );
			}
			break;

		case ItemPool.VITACHOC_CAPSULE:
			if ( responseText.contains( "As the nutritive nanobots" ) )
			{
				Preferences.setInteger( "_vitachocCapsulesUsed", 1 );
			}
			else if ( responseText.contains( "Your body is becoming acclimated" ) )
			{
				Preferences.setInteger( "_vitachocCapsulesUsed", 2 );
			}
			else
			{
				Preferences.setInteger( "_vitachocCapsulesUsed", 3 );
			}
			break;

		case ItemPool.CHOCOLATE_SCULPTURE:
			if ( responseText.contains( "doesn't taste as good" ) )
			{
				Preferences.setInteger( "_chocolateSculpturesUsed", 2 );
			}
			else if ( responseText.contains( "starting to get tired" ) )
			{
				Preferences.setInteger( "_chocolateSculpturesUsed", 3 );
			}
			else if ( responseText.contains( "didn't enjoy" ) )
			{
				int sculptures = Preferences.getInteger( "_chocolateSculpturesUsed" );
				Preferences.setInteger( "_chocolateSculpturesUsed", Math.max( sculptures, 4 ) );
			}
			else
			{
				Preferences.setInteger( "_chocolateSculpturesUsed", 1 );
			}
			break;

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
		case ItemPool.CHOCO_CRIMBOT:
			Preferences.increment( "_chocolatesUsed" );
			break;

		case ItemPool.LOVE_CHOCOLATE:
			Preferences.increment( "_loveChocolatesUsed" );
			break;

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
			break;

		case ItemPool.TEMPURA_AIR:
			Preferences.setBoolean( "_tempuraAirUsed", true );
			break;

		case ItemPool.PRESSURIZED_PNEUMATICITY:
			if ( responseText.contains( "You pop the cork" ) )
			{
				Preferences.setBoolean( "_pneumaticityPotionUsed", true );
			}
			break;

		case ItemPool.HYPERINFLATED_SEAL_LUNG:
			Preferences.setBoolean( "_hyperinflatedSealLungUsed", true );
			break;

		case ItemPool.BALLAST_TURTLE:
			Preferences.setBoolean( "_ballastTurtleUsed", true );
			break;

		case ItemPool.LEFT_BEAR_ARM:
			// Both bear arms are used up to create the box
			// You find a box, carefully label it, and shove a couple of bear arms into it.
			if ( !responseText.contains( "You find a box" ) )
			{
				return;
			}
			ResultProcessor.removeItem( ItemPool.RIGHT_BEAR_ARM );
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
		case ItemPool.CRIMBO_STOGIE_ODORIZER:
		case ItemPool.CRIMBO_HOT_MUG:
		case ItemPool.CRIMBO_TREE_FLOCKER:
		case ItemPool.CRIMBO_RUDOLPH_DOLL:

			// Instead of checking which one was used and removing
			// the others here and this one below, remove one of
			// each here

			ResultProcessor.processResult( item );
			ResultProcessor.processItem( ItemPool.CRIMBO_CREEPY_HEAD, -1 );
			ResultProcessor.processItem( ItemPool.CRIMBO_STOGIE_ODORIZER, -1 );
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
			// You may have succeeded
			if ( !responseText.contains( "You open the jar and peer inside." ) )
			{
				// If not, don't remove it
				return;
			}

			CampgroundRequest.setCampgroundItem( itemId, 1 );
			Preferences.setBoolean( "_psychoJarUsed", true );
			break;

		case ItemPool.FISHY_PIPE:
			Preferences.setBoolean( "_fishyPipeUsed", true );
			return;

		case ItemPool.SONAR:
			if ( responseText.contains( "rubble leading west from Guano Junction collapses in a heap" ) )
			{
				QuestDatabase.setQuestProgress( Quest.BAT, "step1" );
			}
			else if ( responseText.contains( "sound waves knock down the pile of rocks on the east side" ) )
			{
				QuestDatabase.setQuestProgress( Quest.BAT, "step2" );
			}
			else if ( responseText.contains( "high frequency noise makes short work of the rubble" ) )
			{
				QuestDatabase.setQuestProgress( Quest.BAT, "step3" );
			}
			break;

		case ItemPool.SPRING_BEACH_CHARTER:
			Preferences.setBoolean( "sleazeAirportAlways", true );
			if ( !responseText.contains( "name gets added to the registry" ) )
			{
				return;
			}
			break;

		case ItemPool.SPRING_BEACH_TICKET:
			if ( responseText.contains( "already have access to that place" ) )
			{
				return;
			}
			Preferences.setBoolean( "_sleazeAirportToday", true );
			break;

		case ItemPool.SPRING_BEACH_TATTOO_COUPON:
			if ( !responseText.contains( "stagger into the back room" ) )
			{
				// If not drunk enough the items is not consumed
				return;
			}
			break;

		case ItemPool.CONSPIRACY_ISLAND_CHARTER:
			Preferences.setBoolean( "spookyAirportAlways", true );
			if ( !responseText.contains( "name gets added to the registry" ) )
			{
				return;
			}
			break;

		case ItemPool.CONSPIRACY_ISLAND_TICKET:
			if ( responseText.contains( "already have access to that place" ) )
			{
				// If you already have access it is not consumed
				return;
			}
			Preferences.setBoolean( "_spookyAirportToday", true );
			break;

		case ItemPool.SHAWARMA_KEYCARD:
			Preferences.setBoolean( "SHAWARMAInitiativeUnlocked", true );
			break;

		case ItemPool.BOTTLE_OPENER_KEYCARD:
			Preferences.setBoolean( "canteenUnlocked", true );
			break;

		case ItemPool.ARMORY_KEYCARD:
			Preferences.setBoolean( "armoryUnlocked", true );
			break;

		case ItemPool.DINSEY_CHARTER:
			Preferences.setBoolean( "stenchAirportAlways", true );
			if ( !responseText.contains( "name gets added to the registry" ) )
			{
				return;
			}
			break;

		case ItemPool.DINSEY_TICKET:
			if ( responseText.contains( "already have access to that place" ) )
			{
				// If you already have access it is not consumed
				return;
			}
			Preferences.setBoolean( "_stenchAirportToday", true );
			break;

		case ItemPool.VOLCANO_CHARTER:
			Preferences.setBoolean( "hotAirportAlways", true );
			if ( !responseText.contains( "name gets added to the registry" ) )
			{
				return;
			}
			break;

		case ItemPool.VOLCANO_TICKET:
			if ( responseText.contains( "already have access to that place" ) )
			{
				// If you already have access it is not consumed
				return;
			}
			Preferences.setBoolean( "_hotAirportToday", true );
			break;

		case ItemPool.GLACIEST_CHARTER:
			Preferences.setBoolean( "coldAirportAlways", true );
			if ( !responseText.contains( "name gets added to the registry" ) )
			{
				return;
			}
			break;

		case ItemPool.GLACIEST_TICKET:
			if ( responseText.contains( "already have access to that place" ) )
			{
				// If you already have access it is not consumed
				return;
			}
			Preferences.setBoolean( "_coldAirportToday", true );
			break;

		case ItemPool.LOVEBUG_PHEROMONES:
			Preferences.setBoolean( "lovebugsUnlocked", true );
			if ( !responseText.contains( "have been permanently unlocked" ) )
			{
				return;
			}
			break;

		case ItemPool.SPOOKYRAVEN_TELEGRAM:
			QuestDatabase.setQuestIfBetter( Quest.SPOOKYRAVEN_NECKLACE, QuestDatabase.STARTED );
			break;

		case ItemPool.MERKIN_WORDQUIZ:
			matcher = MERKIN_WORDQUIZ_PATTERN.matcher( responseText );
			if ( !matcher.find() )
			{
				// Otherwise, it is not consumed
				return;
			}

			Preferences.setInteger( "merkinVocabularyMastery", StringUtilities.parseInt( matcher.group(1) ) );
			ResultProcessor.removeItem( ItemPool.MERKIN_CHEATSHEET );
			break;

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
				break;
			}

			if ( !Preferences.getString( "merkinQuestPath" ).equals( "done" ) &&
			     responseText.contains( "The sigil burned into your forehead" ) )
			{
				Preferences.setString( "merkinQuestPath", "gladiator" );
				Preferences.setInteger( "lastColosseumRoundWon", 15 );
				break;
			}

			// Otherwise, it is not consumed
			return;

		case ItemPool.MERKIN_STASHBOX:
			ResultProcessor.removeItem( ItemPool.MERKIN_LOCKKEY );
			break;

		case ItemPool.MERKIN_KNUCKLEBONE:
			DreadScrollManager.handleKnucklebone( responseText );
			break;

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
				return;
			}
			break;

		case ItemPool.DREADSYLVANIAN_ALMANAC:
			// You've already learned everything you can learn from these things.
			if ( responseText.contains( "You've already learned everything" ) )
			{
				return;
			}
			break;

		case ItemPool.BOOK_OF_MATCHES:
			// If Hidden Tavern not already unlocked, new items available
			if ( Preferences.getInteger( "hiddenTavernUnlock" ) != KoLCharacter.getAscensions() &&
			     !responseText.contains( "admire" ) )
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

			// We detect its loss via the choice adventure.
			return;

		case ItemPool.DESERT_PAMPHLET:
			QuestManager.incrementDesertExploration( 15 );
			break;

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
				break;
			}

			// Otherwise, it is not consumed
			return;

		case ItemPool.STUFFING_FLUFFER:
			if ( responseText.contains( "hippies and frat orcs" ) )
			{
				// Look at the island map and make our best
				// effort to synch up the kill count
				RequestThread.postRequest( new IslandRequest() );
				break;
			}

			// Otherwise, it is not consumed
			return;

		case ItemPool.BLUE_LINT:
		case ItemPool.GREEN_LINT:
		case ItemPool.WHITE_LINT:
		case ItemPool.ORANGE_LINT:
			if ( responseText.contains( "very improbable thing happens" ) )
			{
				// Remove all four lints
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
			// Doesn't reset if you get the breakfast miracle
			if ( !responseText.contains( "breakfast miracle" ) )
			{
				Preferences.setBoolean( "_warbearBreakfastMachineUsed", true );
			}
			return;

		case ItemPool.WARBEAR_SODA_MACHINE:
			Preferences.setBoolean( "_warbearSodaMachineUsed", true );
			return;

		case ItemPool.WARBEAR_GYROCOPTER_BROKEN:
			Preferences.setBoolean( "_warbearGyrocopterUsed", true );
			break;

		case ItemPool.WARBEAR_BANK:
			// You don't have 25 Meat to drop into the bank.
			if ( !responseText.contains( "don't have" ) )
			{
				Preferences.setBoolean( "_warbearBankUsed", true );
			}
			return;

		case ItemPool.LUPINE_APPETITE_HORMONES:
			Preferences.setBoolean( "_lupineHormonesUsed", true );
			break;

		case ItemPool.CORRUPTED_STARDUST:
			Preferences.setBoolean( "_corruptedStardustUsed", true );
			break;

		case ItemPool.PIXEL_ORB:
			Preferences.setBoolean( "_pixelOrbUsed", true );
			break;

			case ItemPool.JARLSBERG_SOUL_FRAGMENT:
			if ( !responseText.contains( "extra skill point" ) )
			{
				return;
			}
			Preferences.increment( "jarlsbergPoints" );
			break;

		case ItemPool.SNEAKY_PETE_SHOT:
			if ( !responseText.contains( "extra skill point" ) )
			{
				return;
			}
			Preferences.increment( "sneakyPetePoints" );
			break;

		case ItemPool.SESHAT_TALISMAN:
			if ( !responseText.contains( "transform into knowledge" ) )
			{
				return;
			}
			Preferences.increment( "edPoints" );
			break;

		case ItemPool.LAZENBY:
			if ( !responseText.contains( "You lean how best to grow your social capital." ) )
			{
				return;
			}
			Preferences.increment( "bondPoints" );
			break;

		case ItemPool.VAMPYRE_BLOOD:
			// Get success text
			Preferences.increment( "darkGyfftePoints" );
			break;

		case ItemPool.ESSENCE_OF_ANNOYANCE:
			if ( !responseText.contains( "You quaff" ) )
			{
				return;
			}
			Preferences.decrement( "summonAnnoyanceCost", 1 );
			break;

		case ItemPool.SWEET_TOOTH:
			if ( responseText.contains( "You pop the sweet" ) || responseText.contains( "You already had" ) )
			{
				Preferences.setBoolean( "_sweetToothUsed", true );
			}
			break;

		case ItemPool.VORACI_TEA:
			Preferences.setBoolean( "_voraciTeaUsed", true );
			break;

		case ItemPool.SOBRIE_TEA:
			Preferences.setBoolean( "_sobrieTeaUsed", true );
			break;

		case ItemPool.CHRONER_TRIGGER:
			Preferences.setBoolean( "_chronerTriggerUsed", true );
			return;

		case ItemPool.CHRONER_CROSS:
			Preferences.setBoolean( "_chronerCrossUsed", true );
			if ( !responseText.contains( "falls right through" ) )
			{
				ResultProcessor.removeItem( ItemPool.CHRONER );
			}
			return;

		case ItemPool.PALINDROME_BOOK_2:
			QuestDatabase.setQuestIfBetter( Quest.PALINDOME, "step2" );
			return;

		case ItemPool.STEAM_FIST_1:
		case ItemPool.STEAM_FIST_2:
		case ItemPool.STEAM_FIST_3:
			if ( responseText.contains( "rub the three trading cards together" ) )
			{
				ResultProcessor.processItem( ItemPool.STEAM_FIST_1, -1 );
				ResultProcessor.processItem( ItemPool.STEAM_FIST_2, -1 );
				ResultProcessor.processItem( ItemPool.STEAM_FIST_3, -1 );
			}
			return;

		case ItemPool.STEAM_TRIP_1:
		case ItemPool.STEAM_TRIP_2:
		case ItemPool.STEAM_TRIP_3:
			if ( responseText.contains( "rub the three trading cards together" ) )
			{
				ResultProcessor.processItem( ItemPool.STEAM_TRIP_1, -1 );
				ResultProcessor.processItem( ItemPool.STEAM_TRIP_2, -1 );
				ResultProcessor.processItem( ItemPool.STEAM_TRIP_3, -1 );
			}
			return;

		case ItemPool.STEAM_METEOID_1:
		case ItemPool.STEAM_METEOID_2:
		case ItemPool.STEAM_METEOID_3:
			if ( responseText.contains( "rub the three trading cards together" ) )
			{
				ResultProcessor.processItem( ItemPool.STEAM_METEOID_1, -1 );
				ResultProcessor.processItem( ItemPool.STEAM_METEOID_2, -1 );
				ResultProcessor.processItem( ItemPool.STEAM_METEOID_3, -1 );
			}
			return;

		case ItemPool.STEAM_DEMON_1:
		case ItemPool.STEAM_DEMON_2:
		case ItemPool.STEAM_DEMON_3:
			if ( responseText.contains( "rub the three trading cards together" ) )
			{
				ResultProcessor.processItem( ItemPool.STEAM_DEMON_1, -1 );
				ResultProcessor.processItem( ItemPool.STEAM_DEMON_2, -1 );
				ResultProcessor.processItem( ItemPool.STEAM_DEMON_3, -1 );
			}
			return;

		case ItemPool.STEAM_PLUMBER_1:
		case ItemPool.STEAM_PLUMBER_2:
		case ItemPool.STEAM_PLUMBER_3:
			if ( responseText.contains( "rub the three trading cards together" ) )
			{
				ResultProcessor.processItem( ItemPool.STEAM_PLUMBER_1, -1 );
				ResultProcessor.processItem( ItemPool.STEAM_PLUMBER_2, -1 );
				ResultProcessor.processItem( ItemPool.STEAM_PLUMBER_3, -1 );
			}
			return;

		case ItemPool.LETTER_FROM_MELVIGN:
			QuestDatabase.setQuestIfBetter( Quest.SHIRT, QuestDatabase.STARTED );
			break;

		case ItemPool.XIBLAXIAN_SCHEMATIC_COWL:
		case ItemPool.XIBLAXIAN_SCHEMATIC_TROUSERS:
		case ItemPool.XIBLAXIAN_SCHEMATIC_VEST:
		case ItemPool.XIBLAXIAN_SCHEMATIC_BURRITO:
		case ItemPool.XIBLAXIAN_SCHEMATIC_WHISKEY:
		case ItemPool.XIBLAXIAN_SCHEMATIC_RESIDENCE:
		case ItemPool.XIBLAXIAN_SCHEMATIC_GOGGLES:
			if ( Preferences.getBoolean( "unknownRecipe" + itemId ) )
			{
				String message = "Learned recipe: " + name + " (" + itemId + ")";
				RequestLogger.printLine( message );
				RequestLogger.updateSessionLog( message );
				Preferences.setBoolean( "unknownRecipe" + itemId, false );
			}
			break;

		case ItemPool.CRIMBOT_TORSO_2:  case ItemPool.CRIMBOT_LEFTARM_2:  case ItemPool.CRIMBOT_RIGHTARM_2:  case ItemPool.CRIMBOT_PROPULSION_2:
		case ItemPool.CRIMBOT_TORSO_3:  case ItemPool.CRIMBOT_LEFTARM_3:  case ItemPool.CRIMBOT_RIGHTARM_3:  case ItemPool.CRIMBOT_PROPULSION_3:
		case ItemPool.CRIMBOT_TORSO_4:  case ItemPool.CRIMBOT_LEFTARM_4:  case ItemPool.CRIMBOT_RIGHTARM_4:  case ItemPool.CRIMBOT_PROPULSION_4:
		case ItemPool.CRIMBOT_TORSO_5:  case ItemPool.CRIMBOT_LEFTARM_5:  case ItemPool.CRIMBOT_RIGHTARM_5:  case ItemPool.CRIMBOT_PROPULSION_5:
		case ItemPool.CRIMBOT_TORSO_6:  case ItemPool.CRIMBOT_LEFTARM_6:  case ItemPool.CRIMBOT_RIGHTARM_6:  case ItemPool.CRIMBOT_PROPULSION_6:
		case ItemPool.CRIMBOT_TORSO_7:  case ItemPool.CRIMBOT_LEFTARM_7:  case ItemPool.CRIMBOT_RIGHTARM_7:  case ItemPool.CRIMBOT_PROPULSION_7:
		case ItemPool.CRIMBOT_TORSO_8:  case ItemPool.CRIMBOT_LEFTARM_8:  case ItemPool.CRIMBOT_RIGHTARM_8:  case ItemPool.CRIMBOT_PROPULSION_8:
		case ItemPool.CRIMBOT_TORSO_9:  case ItemPool.CRIMBOT_LEFTARM_9:  case ItemPool.CRIMBOT_RIGHTARM_9:  case ItemPool.CRIMBOT_PROPULSION_9:
		case ItemPool.CRIMBOT_TORSO_10: case ItemPool.CRIMBOT_LEFTARM_10: case ItemPool.CRIMBOT_RIGHTARM_10: case ItemPool.CRIMBOT_PROPULSION_10:
		case ItemPool.CRIMBOT_TORSO_11: case ItemPool.CRIMBOT_LEFTARM_11: case ItemPool.CRIMBOT_RIGHTARM_11: case ItemPool.CRIMBOT_PROPULSION_11:
			if ( !responseText.contains( "You feed the schematic into the Crimbot assembler" ) )
			{
				return;
			}
			break;

		case ItemPool.GAUDY_KEY:
			if ( !responseText.contains( "the key vanishes" ) )
			{
				return;
			}
			break;

		case ItemPool.PICKY_TWEEZERS:
			Preferences.setBoolean( "_pickyTweezersUsed", true );
			return;

		case ItemPool.BITTYCAR_HOTCAR:
			Preferences.setString( "_bittycar", "hotcar" );
			return;

		case ItemPool.BITTYCAR_MEATCAR:
			Preferences.setString( "_bittycar", "meatcar" );
			return;

		case ItemPool.BITTYCAR_SOULCAR:
			Preferences.setString( "_bittycar", "soulcar" );
			return;

		case ItemPool.RED_GREEN_RAIN_STICK:
			Preferences.setBoolean( "_rainStickUsed", true );
			return;

		case ItemPool.REDWOOD_RAIN_STICK:
			Preferences.setBoolean( "_redwoodRainStickUsed", true );
			return;

		case ItemPool.STILL_BEATING_SPLEEN:
			Preferences.setInteger( "lastStillBeatingSpleen", KoLCharacter.getAscensions() );
			if ( !responseText.contains( "assimilate" ) )
			{
				return;
			}
			break;

		case ItemPool.WAREHOUSE_MAP_PAGE:
		case ItemPool.WAREHOUSE_INVENTORY_PAGE:
			if ( responseText.contains( "compare the map" ) )
			{
				ResultProcessor.removeItem( ItemPool.WAREHOUSE_INVENTORY_PAGE );
				ResultProcessor.removeItem( ItemPool.WAREHOUSE_MAP_PAGE );
				Preferences.increment( "warehouseProgress", 8 );
			}
			return;

		case ItemPool.MAYONEX:
		case ItemPool.MAYODIOL:
		case ItemPool.MAYOSTAT:
		case ItemPool.MAYOZAPINE:
		case ItemPool.MAYOFLEX:
			if ( responseText.contains( "mouth is already full" ) )
			{
				return;
			}

			Preferences.setString( "mayoInMouth", item.getName() );
			Preferences.increment( "mayoLevel", 1 );
			break;

		case ItemPool.HUNGER_SAUCE:
			Preferences.setBoolean( "_hungerSauceUsed", true );
			break;

		case ItemPool.BRAIN_PRESERVATION_FLUID:
			Preferences.setBoolean( "_brainPreservationFluidUsed", true );
			break;

		case ItemPool.RESORT_CHIP:
			if ( responseText.contains( "turn in 100 chips at the redemption center" ) )
			{
				ResultProcessor.processItem( ItemPool.RESORT_CHIP, -100 );
			}
			return;

		case ItemPool.COCKTAIL_SHAKER:
			Preferences.setBoolean( "_cocktailShakerUsed", true );
			return;

		case ItemPool.TWELVE_NIGHT_ENERGY:
			Preferences.setBoolean( "_twelveNightEnergyUsed", true );
			break;

		case ItemPool.CSA_FIRE_STARTING_KIT:
			// If this worked, it redirected to choice #595
			// If it didn't redirect, it was not consumed.
			Preferences.setBoolean( "_fireStartingKitUsed", true );
			return;

		case ItemPool.SHRINE_BARREL_GOD:
			Preferences.setBoolean( "barrelShrineUnlocked", true );
			break;

		case ItemPool.HAUNTED_DOGHOUSE:
			if ( responseText.contains( "You install the doghouse at your campsite" ) )
			{
				CampgroundRequest.setCampgroundItem( ItemPool.HAUNTED_DOGHOUSE, 1 );
				break;
			}
			return;

		case ItemPool.GHOST_DOG_CHOW:
			if ( responseText.contains( "familiar doesn't seem interested" ) )
			{
				// Not used up
				UseItemRequest.lastUpdate = "Your familiar is not interested in that item.";
				return;
			}
			if ( responseText.contains( "you can't figure out how to feed" ) )
			{
				UseItemRequest.lastUpdate = "You cannot feed Ghost Dog Chow to a Ghost of Crimbo";
				return;
			}
			break;

		case ItemPool.VYKEA_INSTRUCTIONS:
			// If "using" the item works, it redirects to choice.php.
			// If it does not redirect, it gives a message and is
			// not consumed.
			return;

		case ItemPool.CIRCLE_DRUM:
			// You join the 1,427 other people sitting in a drum circle in Seaside Town.
			Pattern RHYTHM = Pattern.compile( "You join the (.*?) other people" );
			Matcher rhythmMatcher = RHYTHM.matcher( responseText );
			if ( rhythmMatcher.find() )
			{
				int boost = StringUtilities.parseIntInternal2( rhythmMatcher.group( 1 ) );
				int bonus = ( boost - 1 ) / 10 + 1;
				Preferences.setInteger( "_feelinTheRhythm", bonus );
			}
			Preferences.setBoolean( "_circleDrumUsed", true );
			return;

		case ItemPool.CLARA_BELL:
			Preferences.setBoolean( "_claraBellUsed", true );
			return;

		case ItemPool.GLENN_DICE:
			Preferences.setBoolean( "_glennGoldenDiceUsed", true );
			return;

		case ItemPool.SNOWMAN_CRATE:
			Preferences.setBoolean( "snojoAvailable", true );
			break;

		case ItemPool.ROYAL_TEA:
			Preferences.increment( "royalty", 1 );
			break;

		case ItemPool.BOO_CLUE:
			if ( responseText.contains( "already in the process of investigating" ) )
			{
				// Not used up
				return;
			}
			break;

		case ItemPool.MOUNTAIN_SKIN:
		case ItemPool.GRIZZLED_SKIN:
		case ItemPool.DIAMONDBACK_SKIN:
		case ItemPool.COAL_SKIN:
		case ItemPool.FRONTWINDER_SKIN:
		case ItemPool.ROTTING_SKIN:
			EquipmentManager.setEquipment( EquipmentManager.BOOTSKIN, item );
			break;

		case ItemPool.QUICKSILVER_SPURS:
		case ItemPool.THICKSILVER_SPURS:
		case ItemPool.WICKSILVER_SPURS:
		case ItemPool.SLICKSILVER_SPURS:
		case ItemPool.SICKSILVER_SPURS:
		case ItemPool.NICKSILVER_SPURS:
		case ItemPool.TICKSILVER_SPURS:
			EquipmentManager.setEquipment( EquipmentManager.BOOTSPUR, item );
			break;

		case ItemPool.SNAKE_OIL:
			Preferences.increment( "awolMedicine", 3 );
			Preferences.increment( "awolVenom", 3 );
			break;

		case ItemPool.HEIMZ_BEANS:
		case ItemPool.TESLA_BEANS:
		case ItemPool.MIXED_BEANS:
		case ItemPool.HELLFIRE_BEANS:
		case ItemPool.FRIGID_BEANS:
		case ItemPool.BLACKEST_EYED_PEAS:
		case ItemPool.STINKBEANS:
		case ItemPool.PORK_N_BEANS:
		case ItemPool.PREMIUM_BEANS:
			// Cans of Beans are off-hand items which can be
			// "plated" Since they are equipment, they will not be
			// removed from inventory below. Do it here.
			if ( responseText.contains( "You acquire" ) )
			{
				ResultProcessor.processResult( item.getNegation() );
			}
			return;

		case ItemPool.MEMORIES_OF_COW_PUNCHING:
			Preferences.increment( "awolDeferredPointsCowpuncher" );
			break;

		case ItemPool.MEMORIES_OF_BEAN_SLINGING:
			Preferences.increment( "awolDeferredPointsBeanslinger" );
			break;

		case ItemPool.MEMORIES_OF_SNAKE_OILING:
			Preferences.increment( "awolDeferredPointsSnakeoiler" );
			break;

		case ItemPool.CODPIECE:
		case ItemPool.BASS_CLARINET:
		case ItemPool.FISH_HATCHET:
			Preferences.setBoolean( "_floundryItemUsed", true );
			break;

		case ItemPool.BACON_MACHINE:
			Preferences.setBoolean( "_baconMachineUsed", true );
			break;

		case ItemPool.SCHOOL_OF_HARD_KNOCKS_DIPLOMA:
			Preferences.setBoolean( "_hardKnocksDiplomaUsed", true );
			break;

		case ItemPool.SOURCE_TERMINAL_PRAM_CHIP:
		case ItemPool.SOURCE_TERMINAL_GRAM_CHIP:
		case ItemPool.SOURCE_TERMINAL_SPAM_CHIP:
		{
			// Source terminal chips (10 maximum)
			// You've already installed the maximum number of those that will fit in your Source terminal.
			// You pull the cover off of your Source terminal and install the chip. It looks like there's room for up to 10 of this particular kind. You have X so far.
			int total = 10;
			Matcher chipMatcher = TERMINAL_CHIP_PATTERN.matcher( responseText );
			if ( chipMatcher.find() )
			{
				total = StringUtilities.parseInt( chipMatcher.group( 1 ) );
			}
			switch ( itemId )
			{
			case ItemPool.SOURCE_TERMINAL_PRAM_CHIP:
				Preferences.setInteger( "sourceTerminalPram", total );
				break;
			case ItemPool.SOURCE_TERMINAL_GRAM_CHIP:
				Preferences.setInteger( "sourceTerminalGram", total );
				break;
			case ItemPool.SOURCE_TERMINAL_SPAM_CHIP:
				Preferences.setInteger( "sourceTerminalSpam", total );
				break;
			}
			if ( responseText.contains( "You've already installed" ) )
			{
				return;
			}
			break;
		}

		case ItemPool.SOURCE_TERMINAL_CRAM_CHIP:
		case ItemPool.SOURCE_TERMINAL_DRAM_CHIP:
		case ItemPool.SOURCE_TERMINAL_TRAM_CHIP:
		case ItemPool.SOURCE_TERMINAL_INGRAM_CHIP:
		case ItemPool.SOURCE_TERMINAL_DIAGRAM_CHIP:
		case ItemPool.SOURCE_TERMINAL_ASHRAM_CHIP:
		case ItemPool.SOURCE_TERMINAL_SCRAM_CHIP:
		case ItemPool.SOURCE_TERMINAL_TRIGRAM_CHIP:
		{
			// Source terminal chip (1 maximum)
			// You've already installed a ASHRAM chip in your Source terminal
			String chipName = null;
			switch ( itemId )
			{
			case ItemPool.SOURCE_TERMINAL_CRAM_CHIP:
				chipName = "CRAM";
				break;
			case ItemPool.SOURCE_TERMINAL_DRAM_CHIP:
				chipName = "DRAM";
				break;
			case ItemPool.SOURCE_TERMINAL_TRAM_CHIP:
				chipName = "TRAM";
				break;
			case ItemPool.SOURCE_TERMINAL_INGRAM_CHIP:
				chipName = "INGRAM";
				break;
			case ItemPool.SOURCE_TERMINAL_DIAGRAM_CHIP:
				chipName = "DIAGRAM";
				break;
			case ItemPool.SOURCE_TERMINAL_ASHRAM_CHIP:
				chipName = "ASHRAM";
				break;
			case ItemPool.SOURCE_TERMINAL_SCRAM_CHIP:
				chipName = "SCRAM";
				break;
			case ItemPool.SOURCE_TERMINAL_TRIGRAM_CHIP:
				chipName = "TRIGRAM";
				break;
			}
			String known = Preferences.getString( "sourceTerminalChips" );
			StringBuilder knownString = new StringBuilder();
			knownString.append( known );
			if ( !known.contains( chipName ) )
			{
				if ( knownString.length() > 0 )
				{
					knownString.append( "," );
				}
				knownString.append( chipName );
				Preferences.setString( "sourceTerminalChips", knownString.toString() );
			}
			if ( responseText.contains( "You've already installed" ) )
			{
				return;
			}
			break;
		}

		case ItemPool.SOURCE_TERMINAL_SUBSTATS_ENH:
		case ItemPool.SOURCE_TERMINAL_DAMAGE_ENH:
		case ItemPool.SOURCE_TERMINAL_CRITICAL_ENH:
		case ItemPool.SOURCE_TERMINAL_PROTECT_ENQ:
		case ItemPool.SOURCE_TERMINAL_STATS_ENQ:
		case ItemPool.SOURCE_TERMINAL_COMPRESS_EDU:
		case ItemPool.SOURCE_TERMINAL_DUPLICATE_EDU:
		case ItemPool.SOURCE_TERMINAL_PORTSCAN_EDU:
		case ItemPool.SOURCE_TERMINAL_TURBO_EDU:
		case ItemPool.SOURCE_TERMINAL_FAMILIAR_EXT:
		case ItemPool.SOURCE_TERMINAL_PRAM_EXT:
		case ItemPool.SOURCE_TERMINAL_GRAM_EXT:
		case ItemPool.SOURCE_TERMINAL_SPAM_EXT:
		case ItemPool.SOURCE_TERMINAL_CRAM_EXT:
		case ItemPool.SOURCE_TERMINAL_DRAM_EXT:
		case ItemPool.SOURCE_TERMINAL_TRAM_EXT:
		{
			// Source terminal file (1 maximum)
			// You've already installed a copy of tram.ext in your Source terminal
			String fileName = ItemDatabase.getItemName( itemId ).substring( 22 );
			String preference = null;
			if ( fileName.contains( ".edu" ) )
			{
				preference = "sourceTerminalEducateKnown";
			}
			else if ( fileName.contains( ".enq" ) )
			{
				preference = "sourceTerminalEnquiryKnown";
			}
			else if ( fileName.contains( ".enh" ) )
			{
				preference = "sourceTerminalEnhanceKnown";
			}
			else if ( fileName.contains( ".ext" ) )
			{
				preference = "sourceTerminalExtrudeKnown";
			}
			String known = Preferences.getString( preference );
			StringBuilder knownString = new StringBuilder();
			knownString.append( known );
			if ( !known.contains( fileName ) )
			{
				if ( knownString.length() > 0 )
				{
					knownString.append( "," );
				}
				knownString.append( fileName );
				Preferences.setString( preference, knownString.toString() );
			}
			if ( responseText.contains( "You've already installed a copy of" ) )
			{
				return;
			}
			break;
		}

		case ItemPool.DETECTIVE_APPLICATION:
			Preferences.setBoolean( "hasDetectiveSchool", true );
			break;

		case ItemPool.HOLORECORD_POWERGUY:
		case ItemPool.HOLORECORD_SHRIEKING_WEASEL:
		case ItemPool.HOLORECORD_SUPERDRIFTER:
		case ItemPool.HOLORECORD_LUCKY_STRIKES:
		case ItemPool.HOLORECORD_DRUNK_UNCLES:
		case ItemPool.HOLORECORD_EMD:
		case ItemPool.HOLORECORD_PIGS:
			if ( responseText.contains( "already a record" ) )
			{
				ResultProcessor.processResult( item );
			}
			break;

		case ItemPool.BROKEN_CHOCOLATE_POCKETWATCH:
			// Using your candy screwdriver and your spare gingerbread parts, you manage to repair the pocketwatch
			if ( !responseText.contains( "manage to repair" ) )
			{
				return;
			}
			ResultProcessor.processItem( ItemPool.SPARE_CHOCOLATE_PARTS, -1 );
			break;

		case ItemPool.SPACEGATE_ACCESS_BADGE:

			// She doesn't give you the badge back, and when you
			// ask, she explains that the facility entrance is now
			// keyed to your genetic signature, and you can come
			// and go as you please, unless your genes change for
			// some reason.

			Preferences.setBoolean( "spacegateAlways", true );
			if ( !responseText.contains( "is now keyed to your genetic signature" ) )
			{
				return;
			}
			break;

		case ItemPool.PORTABLE_SPACEGATE:
			if ( responseText.contains( "already have access to that place" ) )
			{
				// If you already have access it is not consumed
				return;
			}
			Preferences.setBoolean( "_spacegateToday", true );
			break;

		case ItemPool.SPACE_BABY_CHILDRENS_BOOK:
			// You read the book and learn a few words in the Space
			// Baby language. Mostly about cute animals.
			if ( !responseText.contains( "learn a few words" ) )
			{
				// If you already have access it is not consumed
				return;
			}
			ChoiceManager.parseLanguageFluency( responseText, "spaceBabyLanguageFluency" );
			break;

		case ItemPool.LICENSE_TO_CHILL:
			Preferences.setBoolean( "_licenseToChillUsed", true );
			break;

		case ItemPool.VICTOR_SPOILS:
			Preferences.setBoolean( "_victorSpoilsUsed", true );
			break;

		case ItemPool.CORNUCOPIA:
			Preferences.increment( "cornucopiasOpened", count );
			break;

		case ItemPool.METEORITE_ADE:
			Preferences.increment( "_meteoriteAdesUsed" );
			break;

		case ItemPool.PERFECTLY_FAIR_COIN:
			Preferences.setBoolean( "_perfectlyFairCoinUsed", true );
			break;

		case ItemPool.CORKED_GENIE_BOTTLE:
			// If we got here, we did not redirect to choice.php
			// That would imply it did not actually consume the item
			return;

		case ItemPool.POKE_GROW_FERTILIZER:
			// You spray your garden plot with the can of Pokégro
			// fertilizer, and a large tuft of tall grass springs
			// up. It happens so fast, you can practically hear the
			// bwoooooop!
			if ( !responseText.contains( "tall grass springs up" ) )
			{
				// Already have Very Tall Grass or don't have a grass patch
				return;
			}
			CampgroundRequest.growTallGrass();
			break;

		case ItemPool.FR_MEMBER:

			// You fill out the forms in the packet and take them to the LyleCo kiosk at the monorail station in town.
			// Time to escape to a realm of fantasy!

			Preferences.setBoolean( "frAlways", true );
			if ( !responseText.contains( "escape to a realm of fantasy" ) )
			{
				return;
			}
			break;

		case ItemPool.FR_GUEST:
			//if ( responseText.contains( "????????" ) )
			//{
				// If you already have access it is not consumed
			//	return;
			//}
			Preferences.setBoolean( "_frToday", true );
			break;

		case ItemPool.FR_MOUNTAIN_MAP:
			Preferences.setBoolean( "frMountainsUnlocked", true );
			break;

		case ItemPool.FR_WOOD_MAP:
			Preferences.setBoolean( "frWoodUnlocked", true );
			break;

		case ItemPool.FR_SWAMP_MAP:
			Preferences.setBoolean( "frSwampUnlocked", true );
			break;

		case ItemPool.FR_VILLAGE_MAP:
			Preferences.setBoolean( "frVillageUnlocked", true );
			break;

		case ItemPool.FR_CEMETARY_MAP:
			Preferences.setBoolean( "frCemetaryUnlocked", true );
			break;

		case ItemPool.CHEESE_WHEEL:
			if ( !responseText.contains( "You pick a cheese!" ) )
			{
				return;
			}
			break;

		case ItemPool.NEVERENDING_PARTY_INVITE:
			Preferences.setBoolean( "neverendingPartyAlways", true );
			break;

		case ItemPool.NEVERENDING_PARTY_INVITE_DAILY:
			Preferences.setBoolean( "_neverendingPartyToday", true );
			break;

		case ItemPool.PUMP_UP_HIGH_TOPS:
			if ( responseText.contains( "pump up the high-tops" ) )
			{
				Preferences.increment( "_highTopPumps" );
				Preferences.increment( "highTopPumped" );
			}
			else if ( responseText.contains( "already pumped up" ) )
			{
				Preferences.setInteger( "_highTopPumps", 3 );
			}
			break;

		case ItemPool.VOTER_REGISTRATION_FORM:
			Preferences.setBoolean( "voteAlways", true );
			break;

		case ItemPool.VOTER_BALLOT:
			Preferences.setBoolean( "_voteToday", true );
			break;

		case ItemPool.BOXING_DAY_CARE:
			Preferences.setBoolean( "daycareOpen", true );
			break;

		case ItemPool.BOXING_DAY_PASS:
			Preferences.setBoolean( "_daycareToday", true );
			break;

		case ItemPool.JERKS_HEALTH_MAGAZINE:
			Preferences.increment( "_jerksHealthMagazinesUsed", count );
			break;

		case ItemPool.ETCHED_HOURGLASS:
			Preferences.setBoolean( "_etchedHourglassUsed", true );
			break;

		case ItemPool.PR_MEMBER:

			// You fill out the forms in the packet and take them to the LyleCo kiosk at the monorail station in town.
			// Time to escape to a realm of swashbuckling adventure!

			Preferences.setBoolean( "prAlways", true );
			if ( !responseText.contains( "escape to a realm of swashbuckling adventure" ) )
			{
				return;
			}
			break;

		case ItemPool.PR_GUEST:
			//if ( responseText.contains( "????????" ) )
			//{
				// If you already have access it is not consumed
			//	return;
			//}
			Preferences.setBoolean( "_prToday", true );
			break;

		case ItemPool.PIECE_OF_DRIFTWOOD:
			// You don't need another beach comb! Stop that!
			if ( responseText.contains( "You don't need another beach comb" ) )
			{
				// If you already have access it is not consumed
				return;
			}
			break;

		case ItemPool.HEWN_MOON_RUNE_SPOON:
			// You twist the spoon around until the reflection of the moon in the bowl looks just like you intended.
			if ( responseText.contains( "You twist the spoon around" ) )
			{
				// You did change sign and it succeeded.
				// This was redirected to inventory.php?action=message.
				// Need to extract the sign from the original URL.
				String sign = UseItemRequest.parseAscensionSign( UseItemRequest.lastUrlString );
				if ( sign != null )
				{
					// Set the new sign.
					KoLCharacter.setSign( sign );
					// If we are in TCRS, need to reload everything
					if ( KoLCharacter.isCrazyRandomTwo() )
					{
						TCRSDatabase.resetModifiers();
						TCRSDatabase.loadTCRSData();
					}
					else
					{
						// This is done for TCRS when loading data
						KoLCharacter.recalculateAdjustments();
						KoLCharacter.updateStatus();
					}
				}
				Preferences.setBoolean( "moonTuned", true );
			}
			// You can't figure out the angle to see the moon's reflection in the spoon anymore.
			else if ( responseText.contains( "You can't figure out the angle" ) )
			{
				// You already changed the sign this ascension.
				Preferences.setBoolean( "moonTuned", true );
			}
			// The item is not consumed regardless
			return;

		case ItemPool.GETAWAY_BROCHURE:
			// Grants access to your Getaway Campsite in the Distant Woods
			Preferences.setBoolean( "getawayCampsiteUnlocked", true );

			// You follow the map in the brochure until you find a
			// nice place to camp -- and it turns out someone's
			// already set up a tent and campfire for you! Wasn't
			// that nice of them? ...Unless maybe this is actually
			// just someone else's campsite, and they got eaten by
			// a bear... wasn't that nice of them?

			// I assume there is a different message if you already
			// have the campsite and the second item is not
			// consumed.
			if ( !responseText.contains( "you find a nice place to camp" ) )
			{
				return;
			}

			break;

		case ItemPool.SEWING_KIT:
			Preferences.setBoolean( "_sewingKitUsed", true );
			break;

		case ItemPool.BIRD_A_DAY_CALENDAR:
		{
			// First usage of the day tells you the daily bird
			Matcher m = BIRD_OF_THE_DAY_PATTERN.matcher( responseText );
			if ( m.find() )
			{
				// First usage of the day
				// This changes the bird of the day even in
				// carried over turns of the effect
				Preferences.setString( "_birdOfTheDay", m.group( 1 ) );
			}

			// Subsequent logins today will add skill
			Preferences.setBoolean( "_canSeekBirds", true );

			// If we've not added the skill today, learn it now
			if ( !KoLCharacter.hasSkill( "Seek out a Bird" ) )
			{
				ResponseTextParser.learnSkill( "Seek out a Bird" );
				ResultProcessor.updateBirdModifiers( EffectPool.BLESSING_OF_THE_BIRD, "_birdOfTheDay" );
			}

			break;
		}

		case ItemPool.GLITCH_ITEM:
		{
			// 1 => [This needs implementation.]
			// 2 => [This needs more implementation.]
			// 4 => [This needs some more implementation.]
			// 11 => [This needs a lot more implementation.]
			// 37 => [This needs a ton more implementation.]
			// 69 => [This needs even more than a ton more implementation.]
			// 111 => [Whoa .]

			int newGlitchLevel = 0;
			int minimumGlitchCount = 0;

			if ( responseText.contains( "needs implementation" ) )
			{
				newGlitchLevel = 1;
				minimumGlitchCount = 1;
			}
			else if ( responseText.contains( "needs more implementation" ) )
			{
				newGlitchLevel = 2;
				minimumGlitchCount = 2;
			}
			else if ( responseText.contains( "needs some more implementation" ) )
			{
				newGlitchLevel = 3;
				minimumGlitchCount = 4;
			}
			else if ( responseText.contains( "needs a lot more implementation" ) )
			{
				newGlitchLevel = 4;
				minimumGlitchCount = 11;
			}
			else if ( responseText.contains( "needs a ton more implementation" ) )
			{
				newGlitchLevel = 5;
				minimumGlitchCount = 37;
			}
			else if ( responseText.contains( "needs even more than a ton more implementation" ) )
			{
				newGlitchLevel = 6;
				minimumGlitchCount = 69;
			}
			else if ( responseText.contains( "Whoa" ) )
			{
				newGlitchLevel = 7;
				minimumGlitchCount = 111;
			}

			// glitch level is derivable from glitch  count -
			// had we been tracking that from the beginning.
			//
			// glitch level is just amusing, but glitch count
			// affects stat and meat gains from fighting a %monster%.
			//
			// Since we are now tracking glitch count, derive it
			// (as much as we can) from glitch level

			int newGlitchCount;
			if ( !Preferences.getBoolean( "_glitchItemImplemented" ) )
			{
				Preferences.setBoolean( "_glitchItemImplemented", true );
				newGlitchCount = Preferences.increment( "glitchItemImplementationCount" );
			}
			else
			{
				newGlitchCount = Preferences.getInteger( "glitchItemImplementationCount" );
			}
			if ( newGlitchCount < minimumGlitchCount )
			{
				Preferences.setInteger( "glitchItemImplementationCount", minimumGlitchCount );
			}

			// Since glitch level is funny, display it when it changes.

			int previousGlitchLevel = Preferences.getInteger( "glitchItemImplementationLevel" );
			if ( previousGlitchLevel != newGlitchLevel )
			{
				Preferences.setInteger( "glitchItemImplementationLevel", newGlitchLevel );
				UseItemRequest.showItemUsage( showHTML, responseText );
			}
			return;
		}

		case ItemPool.MYSTERIOUS_RED_BOX:
		case ItemPool.MYSTERIOUS_GREEN_BOX:
		case ItemPool.MYSTERIOUS_BLUE_BOX:
		case ItemPool.MYSTERIOUS_BLACK_BOX:
			// This box can't be opened today. Because of the mystery, you see.
			if ( responseText.contains( "This box can't be opened today" ) )
			{
				return;
			}
			break;

		case ItemPool.AMINO_ACIDS:
			if ( !responseText.contains( "you ate some delicious, delicious amino acids" ) )
			{
				return;
			}
			Preferences.increment( "aminoAcidsUsed", 1, 3, false );
			break;

		case ItemPool.EYE_OF_THE_THING:
			// Horrified, you throw the Eye on the ground and stomp it into goo before it can do any more damage.
			if ( !responseText.contains( "stomp it into goo" ) )
			{
				return;
			}
			break;

		case ItemPool.FANCY_CHESS_SET:
			// You don't have time for a game of chess right now. (You need to have 10 Adventures to use this item.)
			if ( responseText.contains( "You don't have time for a game of chess right now" ) )
			{
				return;
			}
			// You sit down at the chessboard, and the white pieces begin to move of their own accord. You play black.
			// Your poor heart can't handle more than one high-stakes chess game per day.
			if ( responseText.contains( "You sit down at the chessboard" ) ||
			     responseText.contains( "Your poor heart can't handle" ))
			{
				Preferences.setBoolean( "_fancyChessSetUsed", true );
			}
			return;

		case ItemPool.ONYX_KING:
		case ItemPool.ONYX_QUEEN:
		case ItemPool.ONYX_ROOK:
		case ItemPool.ONYX_BISHOP:
		case ItemPool.ONYX_KNIGHT:
		case ItemPool.ONYX_PAWN:
		case ItemPool.ALABASTER_KING:
		case ItemPool.ALABASTER_QUEEN:
		case ItemPool.ALABASTER_ROOK:
		case ItemPool.ALABASTER_BISHOP:
		case ItemPool.ALABASTER_KNIGHT:
		case ItemPool.ALABASTER_PAWN:
			// You don't have a full set of chess pieces, and you don't know any chess variants that can be played with fewer than 32 of them.
			// You find a fancy checkerboard in a nearby dumpster and assemble a complete chess set.
			if ( responseText.contains( "assemble a complete chess set" ) )
			{
				ResultProcessor.processItem( ItemPool.ONYX_KING, -1 );
				ResultProcessor.processItem( ItemPool.ONYX_QUEEN, -1 );
				ResultProcessor.processItem( ItemPool.ONYX_ROOK, -2 );
				ResultProcessor.processItem( ItemPool.ONYX_BISHOP, -2 );
				ResultProcessor.processItem( ItemPool.ONYX_KNIGHT, -2 );
				ResultProcessor.processItem( ItemPool.ONYX_PAWN, -8 );
				ResultProcessor.processItem( ItemPool.ALABASTER_KING, -1 );
				ResultProcessor.processItem( ItemPool.ALABASTER_QUEEN, -1 );
				ResultProcessor.processItem( ItemPool.ALABASTER_ROOK, -2 );
				ResultProcessor.processItem( ItemPool.ALABASTER_BISHOP, -2 );
				ResultProcessor.processItem( ItemPool.ALABASTER_KNIGHT, -2 );
				ResultProcessor.processItem( ItemPool.ALABASTER_PAWN, -8 );
			}
			return;

		case ItemPool.UNIVERSAL_SEASONING:
			if ( responseText.contains( "You rip open your packet" ) || responseText.contains( "You can't seem to rip the packet open" ) )
			{
				Preferences.setBoolean( "universalSeasoningActive", true );
				Preferences.setBoolean( "_universalSeasoningUsed", true );
			}
			return;

		case ItemPool.SUBSCRIPTION_COCOA_DISPENSER:
			if ( responseText.contains( "You press the button on the cocoa machine" ) )
			{
				Preferences.setBoolean( "_cocoaDispenserUsed", true );
			}
			return;

		case ItemPool.OVERFLOWING_GIFT_BASKET:
			if ( responseText.contains( "You reach into the basket" ) || responseText.contains( "If you take anything else" ) )
			{
				Preferences.setBoolean( "_overflowingGiftBasketUsed", true );
			}
			return;
		case ItemPool.BATTERY_9V:
		case ItemPool.BATTERY_LANTERN:
		case ItemPool.BATTERY_CAR:
			if ( responseText.contains( "Your tongue crackles with electricity" ) )
			{
				Preferences.increment( "shockingLickCharges", count );
			}
			break;
		case ItemPool.RAINPROOF_BARREL_CAULK:
			if ( responseText.contains( "You smear the caulk" ) )
			{
				Preferences.setBoolean( "wildfireBarrelCaulked", true );
			}
			break;
		case ItemPool.PUMP_GREASE:
			if ( responseText.contains( "You smear the grease" ) )
			{
				Preferences.setBoolean( "wildfirePumpGreased", true );
			}
			break;
		}

		if ( CampgroundRequest.isWorkshedItem( itemId ) )
		{
			Preferences.setBoolean( "_workshedItemUsed", true );
			if ( responseText.contains( "already rearranged your workshed" ) )
			{
				return;
			}
			CampgroundRequest.setCurrentWorkshedItem( ItemPool.get( itemId, 1 ) );
			CampgroundRequest.setCampgroundItem( itemId, 1 );
			
			// Get current fuel level
			if ( itemId == ItemPool.ASDON_MARTIN )
			{
				RequestThread.postRequest( new CampgroundRequest( "workshed" ) );
			}
		}

		// Finally, remove the item from inventory if it was successfully used.

		switch ( consumptionType )
		{
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
			if ( !ItemDatabase.isReusable( itemId ) )
			{
				ResultProcessor.processResult( item.getNegation() );
			}
		}
	}

	private static String itemToClass( final int itemId )
	{
		String className = Modifiers.getStringModifier( "Item", itemId, "Class" );
		return className.equals( "" ) ? null : className;
	}

	private static String itemToSkill( final int itemId )
	{
		String skillName = Modifiers.getStringModifier( "Item", itemId, "Skill" );
		return skillName.equals( "" ) ? null : skillName;
	}

	private static void getEvilLevels( final String responseText )
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

	private static void getBugbearBiodataLevels( String responseText )
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
	}

	private static void showItemUsage( final boolean showHTML, final String text )
	{
		if ( showHTML )
		{
			KoLmafia.showHTML(
				"inventory.php?action=message", UseItemRequest.trimInventoryText( text ) );
		}
	}

	private static String trimInventoryText( String text )
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
		     !urlString.startsWith( "inv_spleen.php" ) &&
		     !urlString.startsWith( "multiuse.php" ) &&
		     !urlString.startsWith( "inv_familiar.php" ) &&
		     !(urlString.startsWith( "inventory.php" ) &&
		       !urlString.contains( "action=ghost" ) &&
		       !urlString.contains( "action=hobo" ) &&
		       !urlString.contains( "action=slime" ) &&
		       !urlString.contains( "action=breakbricko" ) &&
		       !urlString.contains( "action=candy" ) ) )
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

		if ( urlString.contains( "multiuse.php" ) ||
		     urlString.contains( "inv_eat.php" ) ||
		     urlString.contains( "inv_booze.php" ) ||
		     urlString.contains( "inv_spleen.php" ) ||
		     urlString.contains( "inv_use.php" ))
		{
			Matcher quantityMatcher = GenericRequest.QUANTITY_PATTERN.matcher( urlString );
			if ( quantityMatcher.find() )
			{
				itemCount = StringUtilities.parseInt( quantityMatcher.group( 1 ) );
			}
		}

		return ItemPool.get( itemId, itemCount );
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

		return ItemPool.get( itemId, itemCount );
	}

	public static final AdventureResult extractAbsorbedItem( final String urlString )
	{
		if ( !urlString.startsWith( "inventory.php" ) )
		{
			return null;
		}

		Matcher itemMatcher = UseItemRequest.ABSORB_PATTERN.matcher( urlString );
		if ( !itemMatcher.find() )
		{
			return null;
		}

		int itemId = StringUtilities.parseInt( itemMatcher.group( 1 ) );

		return ItemPool.get( itemId, 1 );
	}

	private static AdventureResult extractHelper( final String urlString )
	{
		if ( !urlString.startsWith( "inv_eat.php" ) &&
		     !urlString.startsWith( "inv_booze.php" ) &&
		     !urlString.startsWith( "inv_spleen.php" ) &&
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

		return ItemPool.get( itemId );
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

		int itemId = item.getItemId();
		int count = item.getCount();
		String name = item.getName();

		String useString = "feed " + count + " " + name + " to " + familiar.getRace();

		if ( id == FamiliarPool.SLIMELING )
		{
			if ( itemId == ItemPool.GNOLLISH_AUTOPLUNGER ||
			     ConcoctionDatabase.meatStackCreation( itemId ) != null )
			{
				useString += " (" + count + " more slime stack(s) due)";
			}
			else
			{
				// round down for now, since we don't know how this really works
				float charges = count * EquipmentDatabase.getPower( itemId ) / 10.0F;
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

		if ( urlString.contains( "action=closetpull" ) ||
		     urlString.contains( "action=closetpush" ) )
		{
			return ClosetRequest.registerRequest( urlString );
		}

		AdventureResult item = UseItemRequest.extractItem( urlString );

		if ( item != null && item.getItemId() == ItemPool.CARD_SLEEVE )
		{
			return EquipmentRequest.registerCardSleeve( urlString );
		}

		// Special handling for twisting Boris's Helm when it is equipped
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

		// Special handling for shaking Jarlsberg's pan when it is equipped
		if ( item == null && urlString.contains( "action=shakepan" ) )
		{
			AdventureResult before = EquipmentManager.getEquipment( EquipmentManager.OFFHAND );
			AdventureResult after = ItemPool.get( before.getItemId() == ItemPool.JARLS_PAN ? ItemPool.JARLS_COSMIC_PAN : ItemPool.JARLS_PAN, 1 );
			EquipmentManager.setEquipment( EquipmentManager.OFFHAND, after );
			RequestLogger.printLine( "Shook " + before + " into " + after );
			return true;
		}

		// Special handling for shaking Sneaky Pete's leather jacket when it is equipped
		if ( item == null && urlString.contains( "action=popcollar" ) )
		{
			AdventureResult before = EquipmentManager.getEquipment( EquipmentManager.SHIRT );
			AdventureResult after = ItemPool.get( before.getItemId() == ItemPool.PETE_JACKET ? ItemPool.PETE_JACKET_COLLAR : ItemPool.PETE_JACKET, 1 );
			EquipmentManager.setEquipment( EquipmentManager.SHIRT, after );
			RequestLogger.printLine( "Popped " + before + " into " + after );
			return true;
		}

		// Special handling for twisting toggle switch
		if ( item == null && urlString.contains( "action=togglebutt" ) )
		{
			AdventureResult before = EquipmentManager.getEquipment( EquipmentManager.FAMILIAR );
			AdventureResult after = ItemPool.get( before.getItemId() == ItemPool.TOGGLE_SWITCH_BARTEND ?
			                                                            ItemPool.TOGGLE_SWITCH_BOUNCE :
				                                                      ItemPool.TOGGLE_SWITCH_BARTEND, 1 );
			EquipmentManager.discardEquipment( before );
			EquipmentManager.setEquipment( EquipmentManager.FAMILIAR, after );
			RequestLogger.printLine( "Toggled " + before + " into " + after );
			return true;
		}

		if ( item == null )
		{
			return UseItemRequest.registerBingeRequest( urlString );
		}

		// Delegate to specialized classes as appropriate

		int itemId = item.getItemId();

		boolean isSealFigurine = ItemDatabase.isSealFigurine( itemId );
		boolean isBRICKOMonster = ItemDatabase.isBRICKOMonster( itemId );

		if ( ( isSealFigurine || isBRICKOMonster ) && !urlString.contains( "checked=1" ) )
		{
			// Only log the second "use" that actually leads to a fight.
			return true;
		}

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

		UseItemRequest.lastUrlString = urlString;
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

		if ( urlString.startsWith( "inv_spleen.php" ) )
		{
			return SpleenItemRequest.registerRequest();
		}

		String name = item.getName();

		if ( ConsumablesDatabase.getSpleenHit( name ) > 0 )
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
			if ( urlString.contains( "checked" ) )
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
			if ( !urlString.contains( "fold" ) )
			{
				return true;
			}
			useString = "fold " + UseItemRequest.lastItemUsed;
			break;

		case ItemPool.LOATHING_LEGION_UNIVERSAL_SCREWDRIVER:
			// You can either use the unversal screwdriver to
			// untinker something or switch forms.
			// inv_use.php?whichitem=4926&pwd&action=screw&dowhichitem=xxx

			if ( urlString.contains( "action=screw" ) )
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

				if ( urlString.contains( "untinkerall=on" ) )
				{
					untinker = ItemPool.get( uid, untinker.getCount( KoLConstants.inventory ) );
					countStr = "*";
				}

				UseItemRequest.lastUntinker = untinker;
				useString = "unscrew " + countStr + " " + untinker.getName();
				break;
			}

			if ( !urlString.contains( "fold" ) )
			{
				return true;
			}
			useString = "fold " + UseItemRequest.lastItemUsed;
			break;

		case ItemPool.LOATHING_LEGION_TATTOO_NEEDLE:
			// You can either "use" the reusable tattoo needle or
			// switch forms.
			if ( urlString.contains( "switch" ) )
			{
				if ( !urlString.contains( "fold" ) )
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
			if ( !urlString.contains( "answerplz=1" ) )
			{
				return true;
			}
			break;

		case ItemPool.LITTLE_FIRKIN:
		case ItemPool.NORMAL_BARREL:
		case ItemPool.BIG_TUN:
		case ItemPool.WEATHERED_BARREL:
		case ItemPool.DUSTY_BARREL:
		case ItemPool.DISINTEGRATING_BARREL:
		case ItemPool.MOIST_BARREL:
		case ItemPool.ROTTING_BARREL:
		case ItemPool.MOULDERING_BARREL:
		case ItemPool.BARNACLED_BARREL:
			useString =
				urlString.contains( "choice=1" ) ?
				"Throw a barrel smashing party!" :
				( "smash " + name );
			break;

		case ItemPool.HEIMZ_BEANS:
		case ItemPool.TESLA_BEANS:
		case ItemPool.MIXED_BEANS:
		case ItemPool.HELLFIRE_BEANS:
		case ItemPool.FRIGID_BEANS:
		case ItemPool.BLACKEST_EYED_PEAS:
		case ItemPool.STINKBEANS:
		case ItemPool.PORK_N_BEANS:
			useString = "plate " + name;
			break;

		case ItemPool.HEWN_MOON_RUNE_SPOON:
		{
			String sign = parseAscensionSign( urlString );
			if ( sign != null && urlString.contains( "doit=96" ) )
			{
				useString = "tuning moon to The " + sign;
			}
		}
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
		// Some only use adventures when used as a proxy for a non adventure game location
		switch ( this.itemUsed.getItemId() )
		{
			case ItemPool.CHATEAU_WATERCOLOR:
			case ItemPool.GOD_LOBSTER:
			case ItemPool.WITCHESS_SET:
				return 0;
		}
		return UseItemRequest.getAdventuresUsedByItem( this.itemUsed );
	}

	public static int getAdventuresUsed( final String urlString )
	{
		AdventureResult item =
			urlString.contains( "action=chateau_painting" ) ? ChateauRequest.CHATEAU_PAINTING :
			urlString.contains( "fightgodlobster=1" ) ? ItemPool.get( ItemPool.GOD_LOBSTER, 1 ) :
			urlString.contains( "action=witchess" ) ? ItemPool.get( ItemPool.WITCHESS_SET, 1 ) :
			UseItemRequest.extractItem( urlString );
		return item == null ? 0 :
			item.getItemId() == ItemPool.DECK_OF_EVERY_CARD ? DeckOfEveryCardRequest.getAdventuresUsed( urlString ) :
			UseItemRequest.getAdventuresUsedByItem( item );
	}

	private static int getAdventuresUsedByItem( AdventureResult item )
	{
		int turns = 0;
		switch ( item.getItemId() )
		{
		case ItemPool.ABYSSAL_BATTLE_PLANS:
		case ItemPool.AMORPHOUS_BLOB:
		case ItemPool.BARREL_MAP:
		case ItemPool.BLACK_PUDDING:
		case ItemPool.CARONCH_MAP:
		case ItemPool.CHATEAU_WATERCOLOR:
		case ItemPool.CLARIFIED_BUTTER:
		case ItemPool.CRUDE_SCULPTURE:
		case ItemPool.CURSED_PIECE_OF_THIRTEEN:
		case ItemPool.DECK_OF_EVERY_CARD:
		case ItemPool.DOLPHIN_WHISTLE:
		case ItemPool.ENVYFISH_EGG:
		case ItemPool.FRATHOUSE_BLUEPRINTS:
		case ItemPool.GENIE_BOTTLE:
		case ItemPool.GIANT_AMORPHOUS_BLOB:
		case ItemPool.GIFT_CARD:
		case ItemPool.GOD_LOBSTER:
		case ItemPool.ICE_SCULPTURE:
		case ItemPool.LYNYRD_SNARE:
		case ItemPool.MEGACOPIA:
		case ItemPool.PHOTOCOPIED_MONSTER:
		case ItemPool.POCKET_WISH:
		case ItemPool.RAIN_DOH_MONSTER:
		case ItemPool.SCREENCAPPED_MONSTER:
		case ItemPool.SHAKING_CAMERA:
		case ItemPool.SHAKING_CRAPPY_CAMERA:
		case ItemPool.SHAKING_SKULL:
		case ItemPool.SPOOKY_PUTTY_MONSTER:
		case ItemPool.TIME_SPINNER:
		case ItemPool.WAX_BUGBEAR:
		case ItemPool.WHITE_PAGE:
		case ItemPool.WITCHESS_SET:
		case ItemPool.XIBLAXIAN_HOLOTRAINING_SIMCODE:
		case ItemPool.XIBLAXIAN_POLITICAL_PRISONER:
			// Items that can redirect to a fight that costs turns
			// Although we say some things cost turns if they involve a fight as
			// this is used as a check for whether between battle scripts should
			// run, and a loss always counts as a turn anyway.
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
		List<String> pieces = Arrays.asList( responseText.split( "(<.*?>)+" ) );
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
			String piece = pieces.get( start + i * 2 + 1 );
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

	public static final void handleDiary( final String responseText )
	{
		if ( responseText.contains( "Diary" ) )
		{
			QuestDatabase.setQuestIfBetter( Quest.MACGUFFIN, "step2" );
			QuestDatabase.setQuestIfBetter( Quest.BLACK, QuestDatabase.FINISHED );
			QuestDatabase.setQuestIfBetter( Quest.DESERT, QuestDatabase.STARTED );
			QuestDatabase.setQuestIfBetter( Quest.MANOR, QuestDatabase.STARTED );
			QuestDatabase.setQuestIfBetter( Quest.SHEN, QuestDatabase.STARTED );
			QuestDatabase.setQuestIfBetter( Quest.RON, QuestDatabase.STARTED );
			// If Hidden Temple already unlocked, this completes step 1 of Gotta Worship Them All, otherwise start it.
			String status = Preferences.getInteger( "lastTempleUnlock" ) == KoLCharacter.getAscensions() ? "step1" : QuestDatabase.STARTED;
			QuestDatabase.setQuestIfBetter( Quest.WORSHIP, status );
		}
	}
}
