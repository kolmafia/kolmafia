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
import net.sourceforge.kolmafia.objectpool.ConcoctionPool;
import net.sourceforge.kolmafia.objectpool.EffectPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.ConcoctionDatabase;
import net.sourceforge.kolmafia.persistence.FamiliarDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.persistence.Preferences;
import net.sourceforge.kolmafia.persistence.SkillDatabase;
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

	private static final Pattern ROW_PATTERN = Pattern.compile( "<tr>.*?</tr>" );
	private static final Pattern INVENTORY_PATTERN = Pattern.compile( "</blockquote></td></tr></table>.*?</body>" );
	private static final Pattern ITEMID_PATTERN = Pattern.compile( "whichitem=(\\d+)" );
	private static final Pattern QUANTITY_PATTERN = Pattern.compile( "quantity=(\\d+)" );
	private static final Pattern FORTUNE_PATTERN =
		Pattern.compile( "<font size=1>(Lucky numbers: (\\d+), (\\d+), (\\d+))</td>" );

	private static final TreeMap LIMITED_USES = new TreeMap();

	static
	{
		UseItemRequest.LIMITED_USES.put( new Integer( ItemPool.ASTRAL_MUSHROOM ), EffectPool.get( EffectPool.HALF_ASTRAL ) );

		UseItemRequest.LIMITED_USES.put( new Integer( ItemPool.MILK_OF_MAGNESIUM ), ItemDatabase.MILK );

		UseItemRequest.LIMITED_USES.put( new Integer( ItemPool.ABSINTHE ), EffectPool.get( EffectPool.ABSINTHE ) );
	}

	public static String lastUpdate = "";
	private static AdventureResult lastItemUsed = null;
	private static int askedAboutOde = 0;

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
		case ItemPool.JOLLY_BRACELET:
		case ItemPool.RUM_BRACELET:
		case ItemPool.GRUMPY_BRACELET:
		case ItemPool.TARRRNISH_BRACELET:
		case ItemPool.BOOTY_BRACELET:
		case ItemPool.CANNONBALL_BRACELET:
		case ItemPool.COPPER_BRACELET:
		case ItemPool.TONGUE_BRACELET:
			// Crimbo toys
		case ItemPool.HOBBY_HORSE:
		case ItemPool.BALL_IN_A_CUP:
		case ItemPool.SET_OF_JACKS:
			// La Bamba
		case ItemPool.OUTRAGEOUS_SOMBRERO:
			// Iceberglet items
		case ItemPool.ICE_SICKLE:
		case ItemPool.ICE_BABY:
		case ItemPool.ICE_PICK:
		case ItemPool.ICE_SKATES:
			// Great Ball of Frozen Fire items
		case ItemPool.LIARS_PANTS:
		case ItemPool.JUGGLERS_BALLS:
		case ItemPool.PINK_SHIRT:
		case ItemPool.FAMILIAR_DOPPELGANGER:
		case ItemPool.EYEBALL_PENDANT:
			// naughty origami kit items
		case ItemPool.FORTUNE_TELLER:
		case ItemPool.ORIGAMI_MAGAZINE:
		case ItemPool.PAPER_SHURIKEN:
		case ItemPool.ORIGAMI_PASTIES:
		case ItemPool.RIDING_CROP:
			// free candy
		case ItemPool.BAG_OF_CANDY:
			return KoLConstants.CONSUME_USE;

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
		case ItemPool.CHEF:
		case ItemPool.CLOCKWORK_CHEF:
		case ItemPool.BARTENDER:
		case ItemPool.CLOCKWORK_BARTENDER:
		case ItemPool.MAID:
		case ItemPool.CLOCKWORK_MAID:
		case ItemPool.TOASTER:
			return 1;

		case ItemPool.ANCIENT_CURSED_FOOTLOCKER:
			return InventoryManager.getCount( ItemPool.SIMPLE_CURSED_KEY );

		case ItemPool.ORNATE_CURSED_CHEST:
			return InventoryManager.getCount( ItemPool.ORNATE_CURSED_KEY );

		case ItemPool.GILDED_CURSED_CHEST:
			return InventoryManager.getCount( ItemPool.GILDED_CURSED_KEY );

		case ItemPool.MOJO_FILTER:
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
				break;
			default:
				iterations = this.itemUsed.getCount();
				this.itemUsed = this.itemUsed.getInstance( 1 );
			}
		}

		if ( itemId == ItemPool.MACGUFFIN_DIARY )
		{
			( new GenericRequest( "diary.php?textversion=1" ) ).run();
			KoLmafia.updateDisplay( "Your father's diary has been read." );
			return;
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

		for ( int i = 1; i <= iterations && KoLmafia.permitsContinue(); ++i )
		{
			this.constructURLString( originalURLString );

			if ( this.consumptionType == KoLConstants.CONSUME_DRINK && !UseItemRequest.allowBoozeConsumption( ItemDatabase.getInebriety( this.itemUsed.getName() ) ) )
			{
				return;
			}

			this.useOnce( i, iterations, useTypeAsString );
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

		int consumptionType = UseItemRequest.getConsumptionType( item );

		// Assume initially that this causes the item to disappear.
		// In the event that the item is not used, then proceed to
		// undo the consumption.

		switch ( consumptionType )
		{
		case KoLConstants.NO_CONSUME:
			return;

		case KoLConstants.MESSAGE_DISPLAY:
			UseItemRequest.showItemUsage( showHTML, responseText, true );
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
				UseItemRequest.showItemUsage( true, responseText, true );
			}

			return;

			// If it's a fortune cookie, get the fortune

		case ItemPool.FORTUNE_COOKIE:

			UseItemRequest.showItemUsage( showHTML, responseText, true );

			Matcher fortuneMatcher = UseItemRequest.FORTUNE_PATTERN.matcher( responseText );
			if ( !fortuneMatcher.find() )
			{
				return;
			}

			String message = fortuneMatcher.group( 1 );
			RequestLogger.updateSessionLog( message );
			RequestLogger.printLine( message );

			if ( TurnCounter.isCounting( "Fortune Cookie" ) )
			{
				int desiredCount = 0;
				for ( int i = 2; i <= 4; ++i )
				{
					int number = StringUtilities.parseInt( fortuneMatcher.group( i ) );
					if ( TurnCounter.isCounting( "Fortune Cookie", number ) )
					{
						desiredCount = number;
					}
				}

				if ( desiredCount != 0 )
				{
					TurnCounter.stopCounting( "Fortune Cookie" );
					TurnCounter.startCounting( desiredCount, "Fortune Cookie", "fortune.gif" );
					return;
				}
			}

			for ( int i = 2; i <= 4; ++i )
			{
				int number = StringUtilities.parseInt( fortuneMatcher.group( i ) );
				TurnCounter.startCounting( number, "Fortune Cookie", "fortune.gif" );
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
		case ItemPool.TINY_HOUSE:

			KoLConstants.activeEffects.clear();
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

		case ItemPool.KETCHUP_HOUND:

			// Successfully using a ketchup hound uses up the Hey
			// Deze nuts and pagoda plan.

			if ( responseText.indexOf( "pagoda" ) != -1 )
			{
				ResultProcessor.processItem( ItemPool.HEY_DEZE_NUTS, -1 );
				ResultProcessor.processItem( ItemPool.PAGODA_PLANS, -1 );
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
					new EquipmentRequest( blackbirdItem ).run();
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
				ResultProcessor.processResult(
					item.getInstance( item.getCount() - 1 ) );
			}

			return;

		case ItemPool.PURPLE_SNOWCONE:
		case ItemPool.GREEN_SNOWCONE:
		case ItemPool.ORANGE_SNOWCONE:
		case ItemPool.RED_SNOWCONE:
		case ItemPool.BLUE_SNOWCONE:
		case ItemPool.BLACK_SNOWCONE:

			// "Your mouth is still cold from the last snowcone you
			// ate.	 Try again later."

			if ( responseText.indexOf( "still cold" ) != -1 )
			{
				UseItemRequest.lastUpdate = "Your mouth is too cold.";
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
			return;

		case ItemPool.BARTENDER:
		case ItemPool.CLOCKWORK_BARTENDER:
			KoLCharacter.setBartender( true );
			return;

		case ItemPool.TOASTER:
			KoLCharacter.setToaster( true );
			return;

		case ItemPool.SNOWCONE_BOOK:
		case ItemPool.HILARIOUS_BOOK:
		case ItemPool.TASTEFUL_BOOK:
		case ItemPool.CANDY_BOOK:
		case ItemPool.DIVINE_BOOK:

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
			case ItemPool.SNOWCONE_BOOK:
				KoLCharacter.addAvailableSkill( "Summon Snowcone" );
				break;
			case ItemPool.HILARIOUS_BOOK:
				KoLCharacter.addAvailableSkill( "Summon Hilarious Objects" );
				break;
			case ItemPool.TASTEFUL_BOOK:
				KoLCharacter.addAvailableSkill( "Summon Tasteful Items" );
				break;
			case ItemPool.CANDY_BOOK:
				KoLCharacter.addAvailableSkill( "Summon Candy Hearts" );
				break;
			case ItemPool.DIVINE_BOOK:
				KoLCharacter.addAvailableSkill( "Summon Party Favor" );
				break;
			}

			return;

		case ItemPool.JEWELRY_BOOK:

			// "You already know how to make fancy jewelry, so this
			// book contains nothing exciting for you."

			if ( responseText.indexOf( "You already" ) != -1 )
			{
				ResultProcessor.processResult( item );
				return;
			}

			KoLCharacter.addAvailableSkill( "Really Expensive Jewelrycrafting" );

			return;

		case ItemPool.OLFACTION_BOOK:

			if ( responseText.indexOf( "You already" ) != -1 )
			{
				ResultProcessor.processResult( item );
				return;
			}

			KoLCharacter.addAvailableSkill( "Transcendent Olfaction" );

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


		case ItemPool.PIRATE_SKULL:
			UseItemRequest.showItemUsage( showHTML, responseText, true );
			// Fall through

		case ItemPool.QUILL_PEN:
		case ItemPool.JOLLY_CHARRRM:
		case ItemPool.RUM_CHARRRM:
		case ItemPool.GRUMPY_CHARRRM:
		case ItemPool.TARRRNISH_CHARRRM:
		case ItemPool.BOOTY_CHARRRM:
		case ItemPool.CANNONBALL_CHARRRM:
		case ItemPool.COPPER_CHARRRM:
		case ItemPool.TONGUE_CHARRRM:
		case ItemPool.DOUBLE_SIDED_TAPE:

			// These all create things via "use" or "multiuse" of a
			// an ingredient which also consumes other ingredients.

			// CreateItemRequest removed all the ingredients and
			// this method removed the first one. Correct for that.

			ResultProcessor.processResult( item );

			if ( responseText.indexOf( "You acquire" ) != -1 )
			{
				return;
			}

			UseItemRequest.lastUpdate = "You're missing some parts.";
			KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, UseItemRequest.lastUpdate );

			int concoction = ConcoctionPool.findConcoction( KoLConstants.SINGLE_USE, item.getItemId() );

			// CreateItemRequest already dealt with consuming the
			// raw materials. None were actually used.

			if ( concoction != -1 )
			{
				AdventureResult[] ingredients = ConcoctionDatabase.getIngredients( concoction );

				// Put back the ingredients
				for ( int i = 0; i < ingredients.length; ++i )
				{
					AdventureResult ingredient = ingredients[ i ];
					ResultProcessor.processItem( ingredient.getItemId(), ingredient.getCount() );
				}
			}

			return;

		case ItemPool.PALM_FROND:
		case ItemPool.MUMMY_WRAP:
		case ItemPool.DUCT_TAPE:
		case ItemPool.CLINGFILM:
		case ItemPool.TEN_LEAF_CLOVER:
		case ItemPool.DISASSEMBLED_CLOVER:
		case ItemPool.BAT_GUANO:
		case ItemPool.HANDFUL_OF_SAND:
		case ItemPool.SAND_BRICK:
		case ItemPool.INTERESTING_TWIG:
		case ItemPool.LEWD_CARD:

			// These all create things via "use" or "multiuse" of a
			// single ingredient. CreateItemRequest already dealt
			// with consuming the raw materials.

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
			}
			else
			{
				UseItemRequest.lastUpdate = "Insufficient adventures left.";
				KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, UseItemRequest.lastUpdate );
				ResultProcessor.processResult( item );
			}

			return;

		case ItemPool.STEEL_STOMACH:

			if ( responseText.indexOf( "You acquire a skill" ) != -1 )
			{
				KoLCharacter.addAvailableSkill( "Stomach of Steel" );
			}
			return;

		case ItemPool.STEEL_LIVER:

			if ( responseText.indexOf( "You acquire a skill" ) != -1 )
			{
				KoLCharacter.addAvailableSkill( "Liver of Steel" );
			}
			return;

		case ItemPool.STEEL_SPLEEN:

			if ( responseText.indexOf( "You acquire a skill" ) != -1 )
			{
				KoLCharacter.addAvailableSkill( "Spleen of Steel" );
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

			return;

		case ItemPool.NEWBIESPORT_TENT:
		case ItemPool.BARSKIN_TENT:
		case ItemPool.COTTAGE:
		case ItemPool.HOUSE:
		case ItemPool.SANDCASTLE:
		case ItemPool.TWIG_HOUSE:
		case ItemPool.HOBO_FORTRESS:

			if ( responseText.indexOf( "You place the" ) == -1 && responseText.indexOf( "You build a" ) == -1 && responseText.indexOf( "You quickly burn down" ) == -1)
			{
				ResultProcessor.processResult( item );
			}
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

		case ItemPool.CURSED_PIECE_OF_THIRTEEN:
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

		case ItemPool.OUTRAGEOUS_SOMBRERO:
		case ItemPool.HOBBY_HORSE:
		case ItemPool.BALL_IN_A_CUP:
		case ItemPool.SET_OF_JACKS:
		case ItemPool.EL_VIBRATO_HELMET:
		case ItemPool.BAG_OF_CANDY:
		case ItemPool.NEVERENDING_SODA:

			// Certain pieces of equipment can also be "used" and
			// are not consumed.

			ResultProcessor.processResult( item );
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
		}
	}

	public static final boolean isHousing( final int itemId )
	{
		return	itemId == ItemPool.NEWBIESPORT_TENT ||
			itemId == ItemPool.BARSKIN_TENT ||
			itemId == ItemPool.COTTAGE ||
			itemId == ItemPool.HOUSE ||
			itemId == ItemPool.SANDCASTLE || 
			itemId == ItemPool.TWIG_HOUSE ||
			itemId == ItemPool.HOBO_FORTRESS;
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

		if ( consumptionType == KoLConstants.CONSUME_USE || consumptionType == KoLConstants.CONSUME_MULTIPLE )
		{
			// See if it is a concoction
			if ( SingleUseRequest.registerRequest( urlString ) ||
			     MultiUseRequest.registerRequest( urlString ) )
			{
				return true;
			}
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

			if ( UseItemRequest.lastItemUsed.getItemId() == ItemPool.MUNCHIES_PILL )
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

		if ( UseItemRequest.lastItemUsed.getItemId() == ItemPool.BLACK_MARKET_MAP && KoLCharacter.getFamiliar().getId() != 59 )
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
