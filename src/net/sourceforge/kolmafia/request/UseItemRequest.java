/**
 * Copyright (c) 2005-2009, KoLmafia development team
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

import java.lang.Math;
import java.util.HashMap;
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
import net.sourceforge.kolmafia.objectpool.ConcoctionPool;
import net.sourceforge.kolmafia.objectpool.EffectPool;
import net.sourceforge.kolmafia.objectpool.FamiliarPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.AdventureDatabase;
import net.sourceforge.kolmafia.persistence.ConcoctionDatabase;
import net.sourceforge.kolmafia.persistence.FamiliarDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.persistence.MonsterDatabase;
import net.sourceforge.kolmafia.persistence.Preferences;
import net.sourceforge.kolmafia.persistence.SkillDatabase;
import net.sourceforge.kolmafia.request.DwarfFactoryRequest;
import net.sourceforge.kolmafia.request.PyramidRequest;
import net.sourceforge.kolmafia.session.EquipmentManager;
import net.sourceforge.kolmafia.session.InventoryManager;
import net.sourceforge.kolmafia.session.ResultProcessor;
import net.sourceforge.kolmafia.session.SorceressLairManager;
import net.sourceforge.kolmafia.session.TurnCounter;
import net.sourceforge.kolmafia.utilities.InputFieldUtilities;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class UseItemRequest
	extends GenericRequest
{
	private static final GenericRequest REDIRECT_REQUEST = new GenericRequest( "inventory.php?action=message" );

	public static final Pattern ITEMID_PATTERN = Pattern.compile( "whichitem=(\\d+)" );
	public static final Pattern QUANTITY_PATTERN = Pattern.compile( "quantity=(\\d+)" );
	public static final Pattern QTY_PATTERN = Pattern.compile( "qty=(\\d+)" );

	private static final Pattern ROW_PATTERN = Pattern.compile( "<tr>.*?</tr>" );
	private static final Pattern INVENTORY_PATTERN = Pattern.compile( "</blockquote></td></tr></table>.*?</body>" );
	private static final Pattern HELPER_PATTERN = Pattern.compile( "(utensil|whichcard)=(\\d+)" );
	private static final Pattern FORTUNE_PATTERN =
		Pattern.compile( "<font size=1>(Lucky numbers: (\\d+), (\\d+), (\\d+))</td>" );

	private static final HashMap LIMITED_USES = new HashMap();

	static
	{
		UseItemRequest.LIMITED_USES.put( new Integer( ItemPool.ASTRAL_MUSHROOM ), EffectPool.get( EffectPool.HALF_ASTRAL ) );

		UseItemRequest.LIMITED_USES.put( new Integer( ItemPool.MILK_OF_MAGNESIUM ), ItemDatabase.MILK );

		UseItemRequest.LIMITED_USES.put( new Integer( ItemPool.ABSINTHE ), EffectPool.get( EffectPool.ABSINTHE ) );

		UseItemRequest.LIMITED_USES.put( new Integer( ItemPool.TURTLE_PHEROMONES ), EffectPool.get( EffectPool.EAU_DE_TORTUE ) );
	}

	public static String lastUpdate = "";
	private static AdventureResult lastItemUsed = null;
	private static AdventureResult lastHelperUsed = null;
	private static int askedAboutOde = 0;
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
		if ( (attrs & ItemDatabase.ATTR_REUSABLE) != 0 )
		{
			return KoLConstants.INFINITE_USES;
		}

		switch ( itemId )
		{
		case ItemPool.HOBO_CODE_BINDER:
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
		case KoLConstants.HPMP_RESTORE:
		case KoLConstants.MP_RESTORE:
			return "skills.php";
		case KoLConstants.CONSUME_HOBO:
		case KoLConstants.CONSUME_GHOST:
			return "familiarbinger.php";
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

		if ( UseItemRequest.needsConfirmation( item ) )
		{
			this.addFormField( "confirm", "true" );
		}

		this.addFormField( "whichitem", String.valueOf( item.getItemId() ) );
	}

	private static boolean needsConfirmation( final AdventureResult item )
	{
		switch ( item.getItemId() )
		{
		case ItemPool.NEWBIESPORT_TENT:
		case ItemPool.BARSKIN_TENT:
		case ItemPool.COTTAGE:
		case ItemPool.HOUSE:
		case ItemPool.SANDCASTLE:
		case ItemPool.TWIG_HOUSE:
		case ItemPool.HOBO_FORTRESS:
			return CampgroundRequest.getCurrentDwelling() != null;
 
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
		case ItemPool.GONG:
		case ItemPool.KETCHUP_HOUND:
			return 1;

		case ItemPool.TOASTER:
			return 3;

		case ItemPool.DANCE_CARD:
			// Disallow using a dance card if already active
			return TurnCounter.isCounting( "Dance Card" ) ? 0 : 1;

		case ItemPool.CHEF:
		case ItemPool.CLOCKWORK_CHEF:
		case ItemPool.BARTENDER:
		case ItemPool.CLOCKWORK_BARTENDER:
		case ItemPool.MAID:
		case ItemPool.CLOCKWORK_MAID:
		case ItemPool.SCARECROW:
		case ItemPool.MEAT_GOLEM:
			// Campground equipment
			return 1;

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
			return 1;

		case ItemPool.ANCIENT_CURSED_FOOTLOCKER:
			return InventoryManager.getCount( ItemPool.SIMPLE_CURSED_KEY );

		case ItemPool.ORNATE_CURSED_CHEST:
			return InventoryManager.getCount( ItemPool.ORNATE_CURSED_KEY );

		case ItemPool.GILDED_CURSED_CHEST:
			return InventoryManager.getCount( ItemPool.GILDED_CURSED_KEY );

		case ItemPool.STUFFED_CHEST:
			return InventoryManager.getCount( ItemPool.STUFFED_KEY );

		case ItemPool.MOJO_FILTER:
			return Math.max( 0, 3 - Preferences.getInteger( "currentMojoFilters" ) );

		case ItemPool.EXPRESS_CARD:
			return Preferences.getBoolean( "expressCardUsed" ) ? 0 : 1;

		case ItemPool.SPICE_MELANGE:
			return Preferences.getBoolean( "spiceMelangeUsed" ) ? 0 : 1;

		case ItemPool.BURROWGRUB_HIVE:
			return Preferences.getBoolean( "burrowgrubHiveUsed" ) ? 0 : 1;
		}

		switch ( consumptionType )
		{
		case KoLConstants.GROW_FAMILIAR:
		case KoLConstants.EQUIP_FAMILIAR:
		case KoLConstants.EQUIP_HAT:
		case KoLConstants.EQUIP_PANTS:
		case KoLConstants.EQUIP_SHIRT:
		case KoLConstants.EQUIP_OFFHAND:
			return 1;
		case KoLConstants.EQUIP_WEAPON:
			// Even if you can dual-wield, if we attempt to "use" a
			// weapon, it will become an "equip", which always goes
			// in the main hand.
			return 1;
		case KoLConstants.EQUIP_ACCESSORY:
			return 3;
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

			if ( inebrietyLeft < inebrietyHit )
			{
				// One drink will make us drunk
				return 1;
			}

			if ( allowOverDrink )
			{
				// Multiple drinks will make us drunk
				return inebrietyLeft / inebrietyHit + 1;
			}

			// Multiple drinks to not quite make us drunk
			return inebrietyLeft / inebrietyHit;
		}

		return Integer.MAX_VALUE;
	}

	public void run()
	{
		// Equipment should be handled by a different kind of request.

		int itemId = this.itemUsed.getItemId();
		
		switch ( itemId )
		{
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
		case ItemPool.HOUSE:
		case ItemPool.SANDCASTLE:
		case ItemPool.TWIG_HOUSE:
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
		}

		UseItemRequest.lastUpdate = "";

		int maximumUses = UseItemRequest.maximumUses( itemId, this.consumptionType );
		if ( maximumUses < this.itemUsed.getCount() )
		{
			KoLmafia.updateDisplay( "(usable quantity of " + this.itemUsed +
				" is currently limited to " + maximumUses + ")" );
			this.itemUsed = this.itemUsed.getInstance( maximumUses );
		}

		if ( this.itemUsed.getCount() < 1 )
		{
			return;
		}

		// If it's an elemental phial, remove other elemental effects
		// first.

		for ( int i = 0; i < BasementRequest.ELEMENT_PHIALS.length; ++i )
		{
			AdventureResult phial = BasementRequest.ELEMENT_PHIALS[ i ];
			if ( itemId == phial.getItemId() )
			{
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
		}

		int price = ItemDatabase.getPriceById( itemId );

		if ( itemId == ItemPool.SELTZER || itemId == ItemPool.MAFIA_ARIA )
		{
			SpecialOutfit.createImplicitCheckpoint();
		}

		if ( price != 0 && this.consumptionType != KoLConstants.INFINITE_USES && !InventoryManager.retrieveItem( this.itemUsed ) )
		{
			if ( itemId == ItemPool.SELTZER || itemId == ItemPool.MAFIA_ARIA )
			{
				SpecialOutfit.restoreImplicitCheckpoint();
			}

			return;
		}

		if ( itemId == ItemPool.SELTZER )
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
			case KoLConstants.CONSUME_HOBO:
			case KoLConstants.CONSUME_GHOST:
				break;
			case KoLConstants.CONSUME_DRINK:
			case KoLConstants.CONSUME_EAT:
				// The miracle of "consume some" does not apply
				// to TPS drinks or black puddings
				if ( !UseItemRequest.singleConsume( itemId ) )
				{
					break;
				}
				// Fall through.
			default:
				iterations = this.itemUsed.getCount();
				this.itemUsed = this.itemUsed.getInstance( 1 );
			}
		}

		if ( itemId == ItemPool.MAFIA_ARIA )
		{
			AdventureResult cummerbund = ItemPool.get( ItemPool.CUMMERBUND, 1 );
			if ( !KoLCharacter.hasEquipped( cummerbund ) )
			{
				RequestThread.postRequest( new EquipmentRequest( cummerbund ) );
			}
		}

		String useTypeAsString =
			this.consumptionType == KoLConstants.CONSUME_EAT ? "Eating" : this.consumptionType == KoLConstants.CONSUME_DRINK ? "Drinking" : "Using";

		String originalURLString = this.getURLString();
		boolean isSealFigurine = UseItemRequest.isSealFigurine( itemId );

		for ( int i = 1; i <= iterations && KoLmafia.permitsContinue(); ++i )
		{
			this.constructURLString( originalURLString );

			if ( this.consumptionType == KoLConstants.CONSUME_DRINK &&
				itemId != ItemPool.STEEL_LIVER &&
				!UseItemRequest.allowBoozeConsumption(
					ItemDatabase.getInebriety( this.itemUsed.getName() ) * this.itemUsed.getCount() ) )
			{
				return;
			}

			this.useOnce( i, iterations, useTypeAsString );

			if ( isSealFigurine && KoLmafia.permitsContinue() )
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
			KoLmafia.updateDisplay( "Finished " + useTypeAsString.toLowerCase() + " " + Math.max(
				iterations, this.itemUsed.getCount() ) + " " + this.itemUsed.getName() + "." );
		}
	}

	private static final boolean isSealFigurine( final int itemId )
	{
		switch (itemId )
		{
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
			return true;
		}
		return false;
	}

	private static final boolean singleConsume( final int itemId )
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
			return true;

		case ItemPool.BLACK_PUDDING:
			// Eating a black pudding can lead to a combat with no
			// feedback about how many were successfully eaten
			// before the combat.
			return true;
		}
		return false;
	}

	public static final void permitOverdrink()
	{
		UseItemRequest.permittedOverdrink = KoLCharacter.getUserId();
	}

	public static final boolean confirmReplacement( final String name )
	{
		if ( StaticEntity.isHeadless() )
		{
			return true;
		}

		if ( !InputFieldUtilities.confirm( "Are you sure you want to replace your " + name + "?" ) )
		{
			return false;
		}

		return true;
	}

	public static final boolean allowBoozeConsumption( final int inebrietyBonus )
	{
		if ( KoLCharacter.isFallingDown() || inebrietyBonus < 1 )
		{
			return true;
		}

		if ( StaticEntity.isHeadless() )
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
			if ( !KoLConstants.activeEffects.contains( ItemDatabase.ODE ) &&
				knowsOde && UseItemRequest.askedAboutOde != KoLCharacter.getUserId() &&
				UseItemRequest.permittedOverdrink != KoLCharacter.getUserId() )
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
		
		if ( this.getAdventuresUsed() > 0 )
		{
			StaticEntity.getClient().runBetweenBattleChecks( true );
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
				StaticEntity.learnRecipe( UseItemRequest.REDIRECT_REQUEST.responseText );
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
			StaticEntity.getClient().recoverHP( healthNeeded + 1 );
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
			if ( !UseItemRequest.parseBinge( this.getURLString(), this.responseText ) )
			{
				KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "Your current familiar can't use that." );
			}
			return;
		}

		UseItemRequest.lastItemUsed = this.itemUsed;
		UseItemRequest.parseConsumption( this.responseText, true );
		StaticEntity.learnRecipe( this.responseText );
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

				if ( responseText.indexOf( "(for a long time)" ) == -1 )
				{
					success = false;
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
		case KoLConstants.COMBAT_ITEM:
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

			KoLCharacter.addFamiliar( FamiliarDatabase.growFamiliarLarva( item.getItemId() ) );

			// Don't bother showing the result
			// UseItemRequest.showItemUsage( showHTML, responseText );
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

			int spleenHit = ItemDatabase.getSpleenHit( item.getName() ) * item.getCount();
			int estimatedSpleen = KoLCharacter.getSpleenLimit() - spleenHit + 1;

			if ( estimatedSpleen > KoLCharacter.getSpleenUse() )
			{
				Preferences.setInteger( "currentSpleenUse", estimatedSpleen );
			}

			ResultProcessor.processResult( item );
			KoLCharacter.updateStatus();

			return;
		}

		// Check to make sure that it wasn't a food or drink
		// that was consumed that resulted in nothing.  Eating
		// too much is flagged as a continuable state.

		if ( responseText.indexOf( "too full" ) != -1 )
		{
			UseItemRequest.lastUpdate = "Consumption limit reached.";
			KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, UseItemRequest.lastUpdate );

			int fullness = ItemDatabase.getFullness( item.getName() ) * item.getCount();
			int estimatedFullness = KoLCharacter.getFullnessLimit() - fullness + 1;

			if ( estimatedFullness > KoLCharacter.getFullness() )
			{
				Preferences.setInteger( "currentFullness", estimatedFullness );
			}

			ResultProcessor.processResult( item );
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
			TurnCounter.startCounting( 3, "Dance Card", "guildapp.gif" );
			return;

			// If it's a fortune cookie, get the fortune

		case ItemPool.FORTUNE_COOKIE:

			matcher = UseItemRequest.FORTUNE_PATTERN.matcher( responseText );
			while ( matcher.find() )
			{
				UseItemRequest.handleFortuneCookie( matcher );
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
				ResultProcessor.processResult(
					item.getInstance( item.getCount() - 1 ) );
			}

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
						adv = 91;	// default to Noob Cave
					}
					KoLAdventure req = AdventureDatabase.getAdventureByURL(
						"adventure.php?snarfblat=" + adv );
					// Must do some trickery here to
					// prevent the adventure location from
					// being changed, and the conditions
					// reset.
					String la = Preferences.getString( "lastAdventure" );
					Preferences.setString( "lastAdventure",
						req.getAdventureName() );
					RequestThread.postRequest( req );
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
				UseItemRequest.lastUpdate = "You music was inadequate.";
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
			CharPaneRequest.getInstance().run();
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
			}
			else
			{
				UseItemRequest.lastUpdate = "You don't have everything you need.";
				KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, UseItemRequest.lastUpdate );
				ResultProcessor.processResult( item );
			}

			return;

		case ItemPool.CARONCH_MAP:

			if ( responseText.indexOf( "fight.php" ) != -1 )
			{
				GenericRequest.checkItemRedirection( item );
			}

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
			// ate.   Try again later."

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
			// high-pressure sauce potion.  If you took this one,
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

		case ItemPool.PLUS_SIGN:

			// "Following The Oracle's advice, you treat the plus
			// sign as a book, and read it."

			if ( responseText.indexOf( "you treat the plus sign as a book" ) == -1 )
			{
				UseItemRequest.lastUpdate = "You don't know how to use it.";
				KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, UseItemRequest.lastUpdate );
				ResultProcessor.processResult( item );
			}

			return;

		case ItemPool.CHEF:
		case ItemPool.CLOCKWORK_CHEF:
			KoLCharacter.setChef( true );
			RequestThread.postRequest( new CampgroundRequest() );
			return;

		case ItemPool.BARTENDER:
		case ItemPool.CLOCKWORK_BARTENDER:
			KoLCharacter.setBartender( true );
			RequestThread.postRequest( new CampgroundRequest() );
			return;

		case ItemPool.SNOWCONE_BOOK:
		case ItemPool.STICKER_BOOK:
			// Tomes
		case ItemPool.HILARIOUS_BOOK:
		case ItemPool.TASTEFUL_BOOK:
			// Grimoires
		case ItemPool.CANDY_BOOK:
		case ItemPool.DIVINE_BOOK:
		case ItemPool.LOVE_BOOK:
			// Librams
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

			String skill = UseItemRequest.itemToSkill( item.getItemId() );
			if ( skill == null || KoLCharacter.hasSkill( skill ) )
			{
				ResultProcessor.processResult( item );
				return;
			}

			StaticEntity.learnSkill( skill );

			return;

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

		case ItemPool.RUSTY_BROKEN_DIVING_HELMET:
		case ItemPool.PIRATE_SKULL:
		case ItemPool.QUILL_PEN:
		case ItemPool.JOLLY_CHARRRM:
		case ItemPool.JOLLY_BRACELET:
		case ItemPool.RUM_CHARRRM:
		case ItemPool.GRUMPY_CHARRRM:
		case ItemPool.TARRRNISH_CHARRRM:
		case ItemPool.BOOTY_CHARRRM:
		case ItemPool.CANNONBALL_CHARRRM:
		case ItemPool.COPPER_CHARRRM:
		case ItemPool.TONGUE_CHARRRM:
		case ItemPool.DOUBLE_SIDED_TAPE:
		case ItemPool.BAT_GUANO:
		case ItemPool.TEN_LEAF_CLOVER:
		case ItemPool.DISASSEMBLED_CLOVER:
		case ItemPool.PACK_OF_CHEWING_GUM:
		case ItemPool.CATALYST:
		case ItemPool.JAMFISH_JAM:
		case ItemPool.TURTLE_WAX:
			// SingleUseRequest
		case ItemPool.PALM_FROND:
		case ItemPool.MUMMY_WRAP:
		case ItemPool.DUCT_TAPE:
		case ItemPool.CLINGFILM:
		case ItemPool.LONG_SKINNY_BALLOON:
		case ItemPool.HANDFUL_OF_SAND:
		case ItemPool.SAND_BRICK:
		case ItemPool.INTERESTING_TWIG:
		case ItemPool.LEWD_CARD:
		case ItemPool.BOXTOP:
		case ItemPool.TURTLEMAIL_BITS:
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
			// MultiUseRequest

			// These all create things via "use" or "multiuse" of a
			// an ingredient and perhaps consume other ingredients.
			// SingleUseRequest or MultiUseRequest removed all the
			// ingredients and this method removed the first
			// one. Correct for that.

			ResultProcessor.processResult( item );

			if ( responseText.indexOf( "You acquire" ) != -1 )
			{
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

			if ( responseText.indexOf( "fight.php" ) != -1 )
			{
				GenericRequest.checkItemRedirection( item );
			}

			// Even if we are redirected to a fight, the item is
			// consumed elsewhere

			ResultProcessor.processResult( item );

			return;

		case ItemPool.DRUM_MACHINE:

			// "Dammit! Your hooks were still on there! Oh well. At
			// least now you know where the pyramid is."

			if ( responseText.indexOf( "hooks were still on" ) != -1 )
			{
				// You lose your weapon
				EquipmentManager.setEquipment( EquipmentManager.WEAPON, EquipmentRequest.UNEQUIP );
				AdventureResult.addResultToList( KoLConstants.inventory, ItemPool.get( ItemPool.WORM_RIDING_HOOKS, 1 ) );
				ResultProcessor.processResult( ItemPool.get( ItemPool.WORM_RIDING_HOOKS, -1 ) );
				SpecialOutfit.forgetEquipment( ItemPool.get( ItemPool.WORM_RIDING_HOOKS, 1 ) );
				KoLmafia.updateDisplay( "Don't forget to equip a weapon!" );
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

			if ( responseText.indexOf( "fight.php" ) != -1 )
			{
				GenericRequest.checkItemRedirection( item );
			}

			// Even if we are redirected to a fight, the item is
			// consumed elsewhere

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

			if ( responseText.indexOf( "fight.php" ) != -1 )
			{
				GenericRequest.checkItemRedirection( item );
			}

			// Even if we are redirected to a fight, the item is
			// not consumed

			ResultProcessor.processResult( item );

			return;

		case ItemPool.SPOOKY_PUTTY_MONSTER:

			// You can't tell what this is supposed to be a copy
			// of. You squish it back into a sheet.

			if ( responseText.indexOf( "squish it back into a sheet" ) != -1 )
			{
				ResultProcessor.processResult( ItemPool.get( ItemPool.SPOOKY_PUTTY_SHEET, 1 ) );
				Preferences.setString( "spookyPuttyMonster", "" );
				return;
			}

			// If we are redirected to a fight, the item is
			// consumed elsewhere

			GenericRequest.checkItemRedirection( item );
			ResultProcessor.processResult( item );

			return;

		case ItemPool.STEEL_STOMACH:

			if ( responseText.indexOf( "You acquire a skill" ) != -1 )
			{
				StaticEntity.learnSkill( "Stomach of Steel" );
			}
			return;

		case ItemPool.STEEL_LIVER:

			if ( responseText.indexOf( "You acquire a skill" ) != -1 )
			{
				StaticEntity.learnSkill( "Liver of Steel" );
			}
			return;

		case ItemPool.STEEL_SPLEEN:

			if ( responseText.indexOf( "You acquire a skill" ) != -1 )
			{
				StaticEntity.learnSkill( "Spleen of Steel" );
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

			Preferences.setInteger(
				"currentMojoFilters",
				Preferences.getInteger( "currentMojoFilters" ) + item.getCount() );

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
									    "lastBangPotion" ) )
					{
						KoLmafia.updateDisplay( "All bang potions have been identified!" );
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
			// feel them in there.  Gah.

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

			KoLCharacter.ensureUpdatedPastamancerGhost();
			int itemId = item.getItemId();
			for ( int i = 0; i < KoLCharacter.COMBAT_ENTITIES.length; ++ i )
			{
				Object [] entity = KoLCharacter.COMBAT_ENTITIES[i];
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

			ResultProcessor.processResult( item );
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

			// Even if we are redirected to a fight, the item is
			// consumed elsewhere
			ResultProcessor.processResult( item );

			// You've summoned too many Infernal seals today. Any
			// more and you're afraid the corruption will be too
			// much for you to bear.

			if ( responseText.indexOf( "too many Infernal seals" ) != -1 )
			{
				UseItemRequest.lastUpdate = "Summoning limit reached.";
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

			if ( responseText.indexOf( "fight.php" ) != -1 )
			{
                                GenericRequest.checkItemRedirection( item );
                        }

			return;

		case ItemPool.SEAL_IRON_INGOT:

			// You beat the seal-iron into a formidable club.
			if ( responseText.indexOf( "formidable club" ) == -1 )
			{
				ResultProcessor.processResult( item );
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
		case ItemPool.HILARIOUS_BOOK:
			return "Summon Hilarious Objects";
		case ItemPool.TASTEFUL_BOOK:
			return "Summon Tasteful Items";
		case ItemPool.CANDY_BOOK:
			return "Summon Candy Hearts";
		case ItemPool.DIVINE_BOOK:
			return "Summon Party Favor";
		case ItemPool.LOVE_BOOK:
			return "Summon Love Song";
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
		}

		return null;
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
					return;
				}
			}
		}
			
		int minCounter;
		if ( ( KoLCharacter.canEat() || KoLCharacter.canDrink() ) &&
		     KoLCharacter.getCurrentRun() > 120 )
		{
			minCounter = 150;	// conservative, wiki claims 160 minimum
		}
		else
		{
			// Oxygenarian path, or early enough in an ascension
			// that a player might have done an oxydrop
			minCounter = 100;	// conservative, wiki claims 102 minimum			
		}

		minCounter -= KoLCharacter.turnsSinceLastSemirare();
		for ( int i = 2; i <= 4; ++i )
		{
			int number = StringUtilities.parseInt( matcher.group( i ) );
			if ( number < minCounter )
			{
				KoLmafia.updateDisplay( "Lucky number " + number +
							" ignored - too soon to be a semirare." );
			}
			else if ( number > 205 )
			{	// conservative, wiki claims 200 maximum
				KoLmafia.updateDisplay( "Lucky number " + number +
							" ignored - too large to be a semirare." );
			}
			else
			{
				TurnCounter.startCounting( number, "Fortune Cookie", "fortune.gif" );
			}
		}
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
		     !urlString.startsWith( "multiuse.php" ) &&
		     !urlString.startsWith( "skills.php" ) &&
		     !urlString.startsWith( "inv_familiar.php" ) &&
		     !urlString.startsWith( "inv_use.php" ) )
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
		     urlString.indexOf( "skills.php" ) != -1 ||
		     urlString.indexOf( "inv_eat.php" ) != -1 ||
		     urlString.indexOf( "inv_booze.php" ) != -1 )
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

		if ( id != FamiliarPool.HOBO && id != FamiliarPool.GHOST )
		{
			return false;
		}

		int count = item.getCount();
		String name = item.getName();
		String useString = "feed " + count + " " + name + " to " + familiar.getRace();

		RequestLogger.updateSessionLog();
		RequestLogger.updateSessionLog( useString );
		
		return true;
	}

	public static final boolean registerRequest( final String urlString )
	{
		UseItemRequest.lastItemUsed = UseItemRequest.extractItem( urlString );
		if ( UseItemRequest.lastItemUsed == null )
		{
			return UseItemRequest.registerBingeRequest( urlString );
		}

		UseItemRequest.lastHelperUsed = UseItemRequest.extractHelper( urlString );

		int itemId = UseItemRequest.lastItemUsed.getItemId();
		int count = UseItemRequest.lastItemUsed.getCount();
		String name = UseItemRequest.lastItemUsed.getName();
		int consumptionType = ItemDatabase.getConsumptionType( itemId );
		String useString = null;

		switch ( consumptionType )
		{
		case KoLConstants.NO_CONSUME:
			return false;

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

			int fullness = ItemDatabase.getFullness( name ) * count;
			if ( fullness > 0 && KoLCharacter.getFullness() + fullness <= KoLCharacter.getFullnessLimit() )
			{
				Preferences.setInteger( "currentFullness", KoLCharacter.getFullness() + fullness );
				Preferences.setInteger( "munchiesPillsUsed", Math.max( Preferences.getInteger( "munchiesPillsUsed" ) - 1, 0 ) );
			}
			break;
		}

		switch ( itemId )
		{
		case ItemPool.EXPRESS_CARD:
			Preferences.setBoolean( "expressCardUsed", true );
			break;

		case ItemPool.SPICE_MELANGE:
			Preferences.setBoolean( "spiceMelangeUsed", true );
			break;

		case ItemPool.MUNCHIES_PILL:
			Preferences.increment( "munchiesPillsUsed", count );
			break;

		case ItemPool.BLACK_MARKET_MAP:
			if ( KoLCharacter.getFamiliar().getId() != FamiliarPool.BLACKBIRD )
			{
				AdventureResult map = UseItemRequest.lastItemUsed;
				FamiliarData blackbird = new FamiliarData( FamiliarPool.BLACKBIRD );

				if ( !KoLCharacter.getFamiliarList().contains( blackbird ) )
				{
					( new UseItemRequest( ItemPool.get( ItemPool.REASSEMBLED_BLACKBIRD, 1 ) ) ).run();
					UseItemRequest.lastItemUsed = map;
				}

				if ( !KoLmafia.permitsContinue() )
				{
					return true;
				}

				( new FamiliarRequest( blackbird ) ).run();
			}
			break;

		case ItemPool.EL_VIBRATO_HELMET:
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
			// You only actually use a seal figurine when you
			// "Begin the Ritual"
			if ( urlString.indexOf( "checked" ) != -1 )
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
		int turns = 0;
		switch ( this.itemUsed.getItemId() )
		{
		case ItemPool.BLACK_PUDDING:
		case ItemPool.DRUM_MACHINE:
		case ItemPool.CARONCH_MAP:
		case ItemPool.FRATHOUSE_BLUEPRINTS:
		case ItemPool.CURSED_PIECE_OF_THIRTEEN:
		case ItemPool.SPOOKY_PUTTY_MONSTER:
			// Items that can redirect to a fight
			turns = 1;
			break;
		
		case ItemPool.GONG:
			// Roachform is three uninterruptible turns
			turns = Preferences.getInteger( "choiceAdventure276" ) == 1 ? 3 : 0;
			break;
		}
		
		return turns * this.itemUsed.getCount();
	}
}
