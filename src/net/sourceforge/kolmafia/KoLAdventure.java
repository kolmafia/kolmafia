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

package net.sourceforge.kolmafia;

import java.util.ArrayList;

import net.sourceforge.foxtrot.Job;
import net.sourceforge.kolmafia.objectpool.EffectPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.AdventureDatabase;
import net.sourceforge.kolmafia.persistence.EquipmentDatabase;
import net.sourceforge.kolmafia.persistence.HolidayDatabase;
import net.sourceforge.kolmafia.persistence.Preferences;
import net.sourceforge.kolmafia.request.AdventureRequest;
import net.sourceforge.kolmafia.request.BasementRequest;
import net.sourceforge.kolmafia.request.CampgroundRequest;
import net.sourceforge.kolmafia.request.ClanRumpusRequest;
import net.sourceforge.kolmafia.request.DwarfFactoryRequest;
import net.sourceforge.kolmafia.request.EquipmentRequest;
import net.sourceforge.kolmafia.request.FightRequest;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.request.HiddenCityRequest;
import net.sourceforge.kolmafia.request.RichardRequest;
import net.sourceforge.kolmafia.request.SewerRequest;
import net.sourceforge.kolmafia.request.UntinkerRequest;
import net.sourceforge.kolmafia.request.UseItemRequest;
import net.sourceforge.kolmafia.session.CustomCombatManager;
import net.sourceforge.kolmafia.session.EquipmentManager;
import net.sourceforge.kolmafia.session.InventoryManager;
import net.sourceforge.kolmafia.swingui.AdventureFrame;
import net.sourceforge.kolmafia.swingui.CouncilFrame;
import net.sourceforge.kolmafia.utilities.InputFieldUtilities;
import net.sourceforge.kolmafia.utilities.StringUtilities;
import net.sourceforge.kolmafia.webui.DungeonDecorator;

public class KoLAdventure
	extends Job
	implements Comparable
{
	public static final String[][] DEMON_TYPES =
	{
		{ "Summoning Chamber", "Pies" },
		{ "Spooky Forest", "Preternatural Greed" },
		{ "Sonofa Beach", "Fit To Be Tide" },
		{ "Deep Fat Friars' Gate", "Big Flaming Whip" },
		{ "Haunted Bathroom", "Demonic Taint" },
		{ null, "pile of smoking rags" },
		{ null, "Drinks" },
	};

	private static final GenericRequest ZONE_UNLOCK = new GenericRequest( "" );

	public static final AdventureResult BEATEN_UP = new AdventureResult( "Beaten Up", 4, true );

	private static KoLAdventure lastVisitedLocation = null;

	private boolean isValidAdventure = false;
	private final String zone, parentZone, adventureId, formSource, adventureName;

	private final String normalString, lowercaseString, parentZoneDescription;

	private GenericRequest request;
	private final AreaCombatData areaSummary;
	private final boolean isNonCombatsOnly;

	/**
	 * Constructs a new <code>KoLAdventure</code> with the given specifications.
	 *
	 * @param formSource The form associated with the given adventure
	 * @param adventureId The identifier for this adventure, relative to its form
	 * @param adventureName The string form, or name of this adventure
	 */

	public KoLAdventure( final String zone, final String formSource, final String adventureId, final String adventureName )
	{
		this.formSource = formSource;
		this.adventureId = adventureId;

		this.zone = zone;
		this.adventureName = adventureName;

		this.normalString = this.zone + ": " + this.adventureName;
		this.lowercaseString = this.normalString.toLowerCase();

		this.parentZone = (String) AdventureDatabase.PARENT_ZONES.get( zone );
		this.parentZoneDescription = (String) AdventureDatabase.ZONE_DESCRIPTIONS.get( this.parentZone );

		if ( formSource.equals( "sewer.php" ) )
		{
			this.request = new SewerRequest( false );
		}
		else if ( formSource.equals( "luckysewer.php" ) )
		{
			this.request = new SewerRequest( true );
		}
		else if ( formSource.equals( "dwarffactory.php" ) )
		{
			this.request = new DwarfFactoryRequest( "ware" );
		}
		else if ( formSource.equals( "clan_gym.php" ) )
		{
			this.request = new ClanRumpusRequest( StringUtilities.parseInt( adventureId ) );
		}
		else if ( formSource.equals( "clan_hobopolis.php" ) )
		{
			this.request = new RichardRequest( StringUtilities.parseInt( adventureId ) );
		}
		else if ( formSource.equals( "basement.php" ) )
		{
			this.request = new BasementRequest( adventureName );
		}
		else if ( adventureId.equals( "118" ) )
		{
			this.request = new HiddenCityRequest();
		}
		else
		{
			this.request = new AdventureRequest( adventureName, formSource, adventureId );
		}

		this.areaSummary = AdventureDatabase.getAreaCombatData( adventureName );

		if ( adventureId == null )
		{
			this.isNonCombatsOnly = false;
			return;
		}

		this.isNonCombatsOnly =
			!( this.request instanceof AdventureRequest ) || this.areaSummary != null && this.areaSummary.combats() == 0 && this.areaSummary.getMonsterCount() == 0;
	}

	public String toLowerCaseString()
	{
		return this.lowercaseString;
	}

	/**
	 * Returns the form source for this adventure.
	 */

	public String getFormSource()
	{
		return this.formSource;
	}

	/**
	 * Returns the name where this zone is found.
	 */

	public String getZone()
	{
		return this.zone;
	}

	public String getParentZone()
	{
		return this.parentZone;
	}

	public String getParentZoneDescription()
	{
		return this.parentZoneDescription;
	}

	/**
	 * Returns the name of this adventure.
	 */

	public String getAdventureName()
	{
		return this.adventureName;
	}

	/**
	 * Returns the adventure Id for this adventure.
	 *
	 * @return The adventure Id for this adventure
	 */

	public String getAdventureId()
	{
		return this.adventureId;
	}

	public AreaCombatData getAreaSummary()
	{
		return this.areaSummary;
	}

	public boolean isNonCombatsOnly()
	{
		return this.isNonCombatsOnly;
	}

	/**
	 * Returns the request associated with this adventure.
	 *
	 * @return The request for this adventure
	 */

	public GenericRequest getRequest()
	{
		return this.request;
	}
	
	public void overrideAdventuresUsed( int used )
	{
		if ( this.request instanceof AdventureRequest )
		{
			((AdventureRequest) this.request).overrideAdventuresUsed( used );
		}
	}

	/**
	 * Checks the map location of the given zone. This is to ensure that
	 * KoLmafia arms any needed flags (such as for the beanstalk).
	 */

	private void validate( boolean visitedCouncil )
	{
		this.isValidAdventure = false;

		if ( this.zone.equals( "Astral" ) )
		{
			// Update the choice adventure setting

			if ( this.adventureId.equals( "96" ) )
			{
				Preferences.setString( "choiceAdventure71", "1" );
			}
			else if ( this.adventureId.equals( "98" ) )
			{
				Preferences.setString( "choiceAdventure71", "2" );
			}
			else
			{
				Preferences.setString( "choiceAdventure71", "3" );
			}

			// If the player is not half-astral, then
			// make sure they are before continuing.

			AdventureResult effect = EffectPool.get( EffectPool.HALF_ASTRAL );
			if ( !KoLConstants.activeEffects.contains( effect ) )
			{
				AdventureResult mushroom = ItemPool.get( ItemPool.ASTRAL_MUSHROOM, 1 );
				this.isValidAdventure = InventoryManager.retrieveItem( mushroom );
				if ( this.isValidAdventure )
				{
					RequestThread.postRequest( new UseItemRequest( mushroom ) );
				}
			}

			this.isValidAdventure = KoLConstants.activeEffects.contains( effect );
			return;
		}

		// Fighting the Goblin King requires effects

		if ( this.formSource.equals( "knob.php" ) )
		{
			int outfitId = EquipmentDatabase.getOutfitId( this );
			AdventureResult perfumeEffect = EffectPool.get( EffectPool.PERFUME );

			if ( !KoLConstants.activeEffects.contains( perfumeEffect ) &&
			     !InventoryManager.retrieveItem( ItemPool.KNOB_GOBLIN_PERFUME ) )
			{
				return;
			}

			if ( !EquipmentManager.isWearingOutfit( outfitId ) &&
			     !EquipmentManager.retrieveOutfit( outfitId ) )
			{
				return;
			}

			RequestThread.postRequest( new EquipmentRequest( EquipmentDatabase.getOutfit( outfitId ) ) );
			if ( !KoLConstants.activeEffects.contains( perfumeEffect ) )
			{
				RequestThread.postRequest( new UseItemRequest( ItemPool.get( ItemPool.KNOB_GOBLIN_PERFUME, 1 ) ) );
			}

			this.isValidAdventure = true;
			return;
		}

		// Adventuring in the mine warehouse or office requires either
		// Mining Gear or the Dwarvish War Uniform

		if ( this.formSource.equals( "dwarffactory.php" ) || this.adventureId.equals( "176" ) )
		{
			int id1 = 8;
			int id2 = 50;

			if ( !EquipmentManager.isWearingOutfit( id1 ) && !EquipmentManager.isWearingOutfit( id2 ) )
			{
				SpecialOutfit outfit = null;
				if ( EquipmentManager.retrieveOutfit( id1 ) )
				{
					outfit = EquipmentDatabase.getOutfit( id1 );
				}
				else if ( EquipmentManager.retrieveOutfit( id2 ) )
				{
					outfit = EquipmentDatabase.getOutfit( id2 );
				}
				else
				{
					return;
				}

				RequestThread.postRequest( new EquipmentRequest( outfit ) );
			}

			this.isValidAdventure = true;
			return;
		}

		// Disguise zones require outfits
		if ( !this.adventureId.equals( "85" ) && ( this.adventureName.indexOf( "Disguise" ) != -1 || this.adventureName.indexOf( "Uniform" ) != -1 ) )
		{
			int outfitId = EquipmentDatabase.getOutfitId( this );

			if ( outfitId > 0 && !EquipmentManager.isWearingOutfit( outfitId ) )
			{
				if ( !EquipmentManager.retrieveOutfit( outfitId ) )
				{
					return;
				}

				RequestThread.postRequest( new EquipmentRequest( EquipmentDatabase.getOutfit( outfitId ) ) );
			}
			this.isValidAdventure = true;
			return;
		}

		// If the person has a continuum transfunctioner, then find
		// some way of equipping it.  If they do not have one, then
		// acquire one then try to equip it.  If the person has a two
		// handed weapon, then report an error.

		if ( this.adventureId.equals( "73" ) )
		{
			AdventureResult transfuctioner = ItemPool.get( ItemPool.TRANSFUNCTIONER, 1 );
			if ( !InventoryManager.hasItem( transfuctioner ) )
			{
				RequestThread.postRequest( KoLAdventure.ZONE_UNLOCK.constructURLString( "mystic.php" ) );
				RequestThread.postRequest( KoLAdventure.ZONE_UNLOCK.constructURLString( "mystic.php?action=crackyes1" ) );
				RequestThread.postRequest( KoLAdventure.ZONE_UNLOCK.constructURLString( "mystic.php?action=crackyes2" ) );
				RequestThread.postRequest( KoLAdventure.ZONE_UNLOCK.constructURLString( "mystic.php?action=crackyes3" ) );
			}

			if ( EquipmentDatabase.getHands( EquipmentManager.getEquipment( EquipmentManager.WEAPON ).getName() ) > 1 )
			{
				KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "You need to free up a hand." );
				return;
			}

			RequestThread.postRequest( new EquipmentRequest( transfuctioner ) );
			this.isValidAdventure = true;
			return;
		}

		if ( this.adventureId.equals( "119" ) )
		{
			AdventureResult talisman = ItemPool.get( ItemPool.TALISMAN, 1 );
			if ( !KoLCharacter.hasEquipped( talisman ) )
			{
				if ( !InventoryManager.hasItem( talisman ) )
				{
					return;
				}

				RequestThread.postRequest( new EquipmentRequest( talisman ) );
			}
			
			RequestThread.postRequest( KoLAdventure.ZONE_UNLOCK.constructURLString(
				"plains.php" ) );
			this.isValidAdventure = KoLAdventure.ZONE_UNLOCK.responseText.indexOf( "dome.gif" ) != -1;
			return;
		}

		if ( this.adventureId.equals( "166" ) )
		{
			// Don't auto-adventure unprepared in Hobopolis sewers

			this.isValidAdventure =
				 !Preferences.getBoolean( "requireSewerTestItems" ) ||
				( KoLCharacter.hasEquipped( ItemPool.get( ItemPool.GATORSKIN_UMBRELLA, 1 ) ) &&
				  InventoryManager.retrieveItem( ItemPool.SEWER_WAD ) &&
				  InventoryManager.retrieveItem( ItemPool.OOZE_O ) &&
				  InventoryManager.retrieveItem( ItemPool.DUMPLINGS ) &&
				  InventoryManager.retrieveItem( ItemPool.OIL_OF_OILINESS, 3 ) );
			return;
		}

		if ( this.formSource.indexOf( "adventure.php" ) == -1 )
		{
			this.isValidAdventure = true;
			return;
		}

		if ( this.isValidAdventure )
		{
			return;
		}

		// If we're trying to take a trip, make sure it's the right one
		if ( this.adventureId.equals( "96" ) || this.adventureId.equals( "97" ) || this.adventureId.equals( "98" ) )
		{
			// You must be Half-Astral to go on a trip

			AdventureResult effect = EffectPool.get( EffectPool.HALF_ASTRAL );
			int astral = effect.getCount( KoLConstants.activeEffects );
			if ( astral == 0 )
			{
				AdventureResult mushroom = ItemPool.get( ItemPool.ASTRAL_MUSHROOM, 1 );
				RequestThread.postRequest( new UseItemRequest( mushroom ) );
				if ( !KoLmafia.permitsContinue() )
				{
					this.isValidAdventure = false;
					return;
				}
			}

			// If we haven't selected a trip yet, do so now
			if ( astral == 5 )
			{
				String choice;
				if ( this.adventureId.equals( "96" ) )
				{
					choice = "1";
				}
				else if ( this.adventureId.equals( "98" ) )
				{
					choice = "2";
				}
				else
				{
					choice = "3";
				}

				// Choose the trip
				Preferences.setString( "choiceAdventure71", choice );

				String name = this.getAdventureName();
				Preferences.setString( "chosenTrip", name );

				this.isValidAdventure = true;
				return;
			}

			this.isValidAdventure = true;
			return;
		}

		// The casino is unlocked provided the player
		// has a casino pass in their inventory.

		if ( this.zone.equals( "Casino" ) )
		{
			this.isValidAdventure = InventoryManager.retrieveItem( ItemPool.CASINO_PASS );
			return;
		}

		// The island is unlocked provided the player
		// has a dingy dinghy in their inventory.

		if ( this.zone.equals( "Island" ) )
		{
			this.isValidAdventure = InventoryManager.hasItem( ItemPool.DINGHY_DINGY );
			if ( this.isValidAdventure )
			{
				return;
			}

			this.isValidAdventure = InventoryManager.hasItem( ItemPool.DINGHY_PLANS );
			if ( !this.isValidAdventure )
			{
				return;
			}

			this.isValidAdventure = InventoryManager.hasItem( ItemPool.DINGY_PLANKS );
			if ( !this.isValidAdventure )
			{
				return;
			}

			AdventureResult plans = ItemPool.get( ItemPool.DINGHY_PLANS, 1 );
			RequestThread.postRequest( new UseItemRequest( plans ) );

			return;
		}

		// The Castle in the Clouds in the Sky is unlocked provided the
		// character has either a S.O.C.K. or an intragalactic rowboat

		if ( this.adventureId.equals( "82" ) )
		{
			this.isValidAdventure =
				InventoryManager.hasItem( ItemPool.get( ItemPool.SOCK, 1 ) ) || InventoryManager.hasItem( ItemPool.get( ItemPool.ROWBOAT, 1 ) );
			return;
		}

		// The Hole in the Sky is unlocked provided the player has an
		// intragalactic rowboat in their inventory.

		if ( this.adventureId.equals( "83" ) )
		{
			if ( !InventoryManager.hasItem( ItemPool.get( ItemPool.ROWBOAT, 1 ) ) && InventoryManager.hasItem( ItemPool.get( ItemPool.GIANT_CASTLE_MAP, 1 ) ) )
			{
				RequestThread.postRequest( new UseItemRequest( ItemPool.get( ItemPool.GIANT_CASTLE_MAP, 1 ) ) );
			}

			this.isValidAdventure = InventoryManager.retrieveItem( ItemPool.get( ItemPool.ROWBOAT, 1 ) );
			return;
		}

		// The beanstalk is unlocked when the player has planted a
		// beanstalk -- but, the zone needs to be armed first.

		if ( this.adventureId.equals( "81" ) && !KoLCharacter.beanstalkArmed() )
		{
			// If the character has a S.O.C.K. or an intragalactic
			// rowboat, they can get to the airship

			if ( InventoryManager.hasItem( ItemPool.get( ItemPool.SOCK, 1 ) ) || InventoryManager.hasItem( ItemPool.get( ItemPool.ROWBOAT, 1 ) ) )
			{
				this.isValidAdventure = true;
				return;
			}

			// Obviate following request by checking accomplishment:
			// questlog.php?which=3
			// "You have planted a Beanstalk in the Nearby Plains."

			KoLAdventure.ZONE_UNLOCK.constructURLString( "plains.php" );
			RequestThread.postRequest( KoLAdventure.ZONE_UNLOCK );

			if ( KoLAdventure.ZONE_UNLOCK.responseText.indexOf( "beanstalk.php" ) == -1 )
			{
				// If not, check to see if the player has an enchanted
				// bean which can be used.  If they don't, then try to
				// find one through adventuring.

				if ( !InventoryManager.hasItem( ItemPool.get( ItemPool.ENCHANTED_BEAN, 1 ) ) )
				{
					ArrayList temporary = new ArrayList();
					temporary.addAll( KoLConstants.conditions );
					KoLConstants.conditions.clear();

					KoLConstants.conditions.add( ItemPool.get( ItemPool.ENCHANTED_BEAN, 1 ) );
					StaticEntity.getClient().makeRequest(
						AdventureDatabase.getAdventureByURL( "adventure.php?snarfblat=33" ),
						KoLCharacter.getAdventuresLeft() );

					if ( !KoLConstants.conditions.isEmpty() )
					{
						KoLmafia.updateDisplay( KoLConstants.ABORT_STATE, "Unable to complete enchanted bean quest." );
					}

					KoLConstants.conditions.clear();
					KoLConstants.conditions.addAll( temporary );
				}

				// Now that you've retrieved the bean, ensure that
				// it is in your inventory, and then use it.  Take
				// advantage of item consumption automatically doing
				// what's needed in grabbing the item.

				RequestThread.postRequest( CouncilFrame.COUNCIL_VISIT );
				RequestThread.postRequest( new UseItemRequest( ItemPool.get( ItemPool.ENCHANTED_BEAN, 1 ) ) );
			}

			KoLAdventure.ZONE_UNLOCK.constructURLString( "beanstalk.php" );
			RequestThread.postRequest( KoLAdventure.ZONE_UNLOCK );

			KoLCharacter.armBeanstalk();
			this.isValidAdventure = true;

			return;
		}

		if ( this.zone.equals( "Spookyraven" ) )
		{
			// It takes RNG luck at the Haunted Pantry to unlock
			// the rest of Spookyraven Manor. Assume it is
			// unlocked. However, we can verify that the zones that
			// require keys are accessible.

			if ( this.adventureId.equals( "104" ) )
			{
				// Haunted Library
				this.isValidAdventure = InventoryManager.hasItem( ItemPool.LIBRARY_KEY );
			}

			else if ( this.adventureId.equals( "106" ) )
			{
				// Haunted Gallery
				this.isValidAdventure = InventoryManager.hasItem( ItemPool.GALLERY_KEY );
			}

			// It takes a special action to make the upstairs areas
			// available. Assume they are accessible if the player
			// can get into the library
			else if ( this.adventureId.equals( "107" ) || this.adventureId.equals( "108" ) )
			{
				// Haunted Bathroom & Bedroom
				this.isValidAdventure = InventoryManager.hasItem( ItemPool.LIBRARY_KEY );
			}

			else if ( this.adventureId.equals( "109" ) )
			{
				// Haunted Ballroom
				this.isValidAdventure = InventoryManager.hasItem( ItemPool.BALLROOM_KEY );
			}

			return;
		}

		if ( this.adventureId.equals( "32" ) || this.adventureId.equals( "33" ) || this.adventureId.equals( "34" ) )
		{
			RequestThread.postRequest( KoLAdventure.ZONE_UNLOCK.constructURLString( "bathole.php" ) );

			if ( this.adventureId.equals( "32" ) && KoLAdventure.ZONE_UNLOCK.responseText.indexOf( "batrockleft.gif" ) == -1 )
			{
				this.isValidAdventure = true;
				return;
			}

			if ( this.adventureId.equals( "33" ) && KoLAdventure.ZONE_UNLOCK.responseText.indexOf( "batrockright.gif" ) == -1 )
			{
				this.isValidAdventure = true;
				return;
			}

			if ( this.adventureId.equals( "34" ) && KoLAdventure.ZONE_UNLOCK.responseText.indexOf( "batrockbottom.gif" ) == -1 )
			{
				this.isValidAdventure = true;
				return;
			}

			int sonarToUse = 0;

			if ( KoLAdventure.ZONE_UNLOCK.responseText.indexOf( "batrockleft.gif" ) != -1 )
			{
				sonarToUse = 3;
			}
			else if ( KoLAdventure.ZONE_UNLOCK.responseText.indexOf( "batrockright.gif" ) != -1 )
			{
				sonarToUse = 2;
			}
			else if ( KoLAdventure.ZONE_UNLOCK.responseText.indexOf( "batrockbottom.gif" ) != -1 )
			{
				sonarToUse = 1;
			}

			AdventureResult sonar = ItemPool.get( ItemPool.SONAR, 1 );
			sonarToUse = Math.min( sonarToUse, sonar.getCount( KoLConstants.inventory ) );

			RequestThread.postRequest( new UseItemRequest( ItemPool.get( ItemPool.SONAR, sonarToUse ) ) );
			RequestThread.postRequest( KoLAdventure.ZONE_UNLOCK );

			if ( this.adventureId.equals( "32" ) )
			{
				this.isValidAdventure = KoLAdventure.ZONE_UNLOCK.responseText.indexOf( "batrockleft.gif" ) == -1;
			}
			else if ( this.adventureId.equals( "33" ) )
			{
				this.isValidAdventure = KoLAdventure.ZONE_UNLOCK.responseText.indexOf( "batrockright.gif" ) == -1;
			}
			else
			{
				this.isValidAdventure = KoLAdventure.ZONE_UNLOCK.responseText.indexOf( "batrockbottom.gif" ) == -1;
			}

			return;
		}

		if ( this.adventureId.equals( "100" ) )
		{
			RequestThread.postRequest( KoLAdventure.ZONE_UNLOCK.constructURLString( "woods.php" ) );
			this.isValidAdventure = KoLAdventure.ZONE_UNLOCK.responseText.indexOf( "grove.gif" ) != -1;

			if ( !visitedCouncil && !this.isValidAdventure )
			{
				StaticEntity.getClient().unlockGuildStore( true );
				this.validate( true );
			}

			return;
		}

		if ( this.zone.equals( "McLarge" ) && !this.adventureId.equals( "176" ))
		{
			RequestThread.postRequest( KoLAdventure.ZONE_UNLOCK.constructURLString( "mclargehuge.php" ) );
			if ( KoLAdventure.ZONE_UNLOCK.responseText.indexOf( this.adventureId ) != -1 )
			{
				this.isValidAdventure = true;
				return;
			}

			if ( visitedCouncil )
			{
				KoLmafia.updateDisplay( "You must complete a trapper task first." );
				return;
			}

			RequestThread.postRequest( CouncilFrame.COUNCIL_VISIT );
			RequestThread.postRequest( KoLAdventure.ZONE_UNLOCK.constructURLString( "trapper.php" ) );

			this.validate( true );
			return;
		}

		this.isValidAdventure = true;
	}

	/**
	 * Retrieves the string form of the adventure contained within this encapsulation, which is generally the name of
	 * the adventure.
	 *
	 * @return The string form of the adventure
	 */

	public String toString()
	{
		return this.normalString;
	}

	/**
	 * Executes the appropriate <code>GenericRequest</code> for the adventure encapsulated by this
	 * <code>KoLAdventure</code>.
	 */

	public void run()
	{
		if ( !KoLmafia.isRunningBetweenBattleChecks() )
		{
			if ( !StaticEntity.getClient().runThresholdChecks() )
			{
				return;
			}

			KoLAdventure.lastVisitedLocation = this;
			if ( !Preferences.getString( "lastAdventure" ).equals( this.adventureName ) )
			{
				Preferences.setString( "lastAdventure", this.adventureName );
				AdventureFrame.updateSelectedAdventure( this );
			}
			StaticEntity.getClient().runBetweenBattleChecks( !this.isNonCombatsOnly() );

			if ( !KoLmafia.permitsContinue() )
			{
				return;
			}
		}

		this.validate( false );

		if ( !this.isValidAdventure )
		{
			String message = this.adventureId.equals( "166" ) ?
				"Do not venture unprepared into the sewer tunnels!" :
				"That area is not available.";
			KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, message );
			return;
		}

		if ( this.getFormSource().equals( "shore.php" ) && KoLCharacter.getAvailableMeat() < 500 )
		{
			KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "Insufficient funds for shore vacation." );
			return;
		}

		String action = Preferences.getString( "battleAction" );

		if ( this.request instanceof AdventureRequest && !this.adventureId.equals( "80" ) )
		{
			if ( !this.isNonCombatsOnly() && action.indexOf( "dictionary" ) != -1 && FightRequest.DICTIONARY1.getCount( KoLConstants.inventory ) < 1 && FightRequest.DICTIONARY2.getCount( KoLConstants.inventory ) < 1 )
			{
				KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "Sorry, you don't have a dictionary." );
				return;
			}
		}
		
		if ( this.areaSummary != null &&
			this.areaSummary.poison() <= Preferences.getInteger( "autoAntidote" ) &&
			!KoLCharacter.hasEquipped( ItemPool.get( ItemPool.BEZOAR_RING, 1 ) ) )
		{
			InventoryManager.retrieveItem( ItemPool.ANTIDOTE );
		}

		if ( !KoLmafia.permitsContinue() )
		{
			return;
		}

		// Make sure there are enough adventures to run the request
		// so that you don't spam the server unnecessarily.

		if ( KoLCharacter.getAdventuresLeft() == 0 || KoLCharacter.getAdventuresLeft() < this.request.getAdventuresUsed() )
		{
			KoLmafia.updateDisplay( KoLConstants.PENDING_STATE, "Ran out of adventures." );
			return;
		}

		if ( !this.isNonCombatsOnly() && this.request instanceof AdventureRequest )
		{
			// Check for dictionaries as a battle strategy, if the
			// person is not adventuring at the chasm.

			if ( !this.adventureId.equals( "80" ) && this.request.getAdventuresUsed() == 1 && action.indexOf( "dictionary" ) != -1 )
			{
				if ( !KoLCharacter.getFamiliar().isCombatFamiliar() )
				{
					KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "A dictionary would be useless there." );
					return;
				}
			}

			// If the person doesn't stand a chance of surviving,
			// automatically quit and tell them so.

			if ( action.startsWith( "attack" ) && this.areaSummary != null && !this.areaSummary.willHitSomething() )
			{
				if ( !KoLCharacter.getFamiliar().isCombatFamiliar() )
				{
					KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "You can't hit anything there." );
					return;
				}
			}

			if ( FightRequest.isInvalidRangedAttack( action ) )
			{
				KoLmafia.updateDisplay( KoLConstants.ERROR_STATE,
					"Your selected attack skill is useless with ranged weapons." );
				return;
			}
		}

		if ( AdventureDatabase.isPotentialCloverAdventure( adventureName ) && Preferences.getBoolean( "cloverProtectActive" ) )
		{
			KoLmafiaCLI.DEFAULT_SHELL.executeCommand( "use", "* ten-leaf clover" );
		}

		// If the test is successful, then it is safe to run the
		// request (without spamming the server).

		RequestThread.postRequest( this.request );
	}

	public static final KoLAdventure lastVisitedLocation()
	{
		return KoLAdventure.lastVisitedLocation;
	}

	public static final int lastAdventureId()
	{
		KoLAdventure location = KoLAdventure.lastVisitedLocation;
		if ( location == null )
		{
			return 0;
		}
		return StringUtilities.parseInt( location.adventureId );
	}

	public static final void resetAutoAttack()
	{
		// In the event that the user made some sort of change
		// to their auto-attack settings, do nothing.

		if ( Preferences.getBoolean( "setAutoAttack" ) )
		{
			CustomCombatManager.setAutoAttack( 0 );
		}
	}

	private void updateAutoAttack()
	{
		// If the user has already configured their own auto-attacks,
		// then KoLmafia should not interfere.

		if ( !Preferences.getBoolean( "setAutoAttack" ) && Preferences.getInteger( "defaultAutoAttack" ) != 0 )
		{
			return;
		}

		// If you're in the middle of a fight, you can't reset your
		// auto-attack.

		if ( FightRequest.getCurrentRound() != 0 )
		{
			return;
		}

		// If you're searching for special scrolls, do not enable
		// your auto-attack.

		if ( this.adventureId.equals( "80" ) )
		{
			CustomCombatManager.setAutoAttack( 0 );
			return;
		}

		// If this is not an automated request, make sure to turn off
		// auto-attack if it was off before any automation started.
		// Custom combat and deleveling do not need to have auto-attack
		// changed, because these users probably are micro-managing.

		if ( !KoLmafia.isAdventuring() || this.formSource.equals( "dungeon.php" ) )
		{
			KoLAdventure.resetAutoAttack();
			return;
		}

		// If the player is pickpocketing, they probably do not want
		// their auto-attack reset to an attack.

		if ( KoLCharacter.isMoxieClass() && Preferences.getBoolean( "autoSteal" ) )
		{
			KoLAdventure.resetAutoAttack();
			return;
		}

		String attack = Preferences.getString( "battleAction" );

		if ( attack.startsWith( "custom" ) ||
			FightRequest.filterInterp != null ||
			KoLConstants.activeEffects.contains( EffectPool.get( EffectPool.FORM_OF_BIRD ) ) )
		{
			KoLAdventure.resetAutoAttack();
			return;
		}
		
		if ( Preferences.getBoolean( "autoEntangle" ) )
		{
			CustomCombatManager.setAutoAttack( "Entangling Noodles" );
			return;
		}

		if ( attack.startsWith( "delevel" ) || attack.startsWith( "item" ) ||
			!Preferences.getString( "autoOlfact" ).equals( "" ) ||
			!Preferences.getString( "autoPutty" ).equals( "" ) )
		{
			KoLAdventure.resetAutoAttack();
			return;
		}

		if ( attack.startsWith( "attack" ) || attack.startsWith( "default" ) )
		{
			CustomCombatManager.setAutoAttack( 1 );
			return;
		}

		// If it's not a generic class skill (its id is something
		// non-standard), then don't update auto-attack.

		if ( attack.startsWith( "skill " ) )
		{
			attack = attack.substring( 6 );
		}

		CustomCombatManager.setAutoAttack( attack );
	}

	private boolean recordToSession()
	{
		this.updateAutoAttack();

		if ( this.adventureId.equals( "118" ) )
		{
			// The Hidden City is weird. It redirects you to the
			// container zone (the grid of 25 squares) if you try
			// to adventure at this adventure ID.

			// We detect adventuring in individual squares
			// elsewhere.
			return false;
		}

		if ( this.adventureId.equals( "123" ) )
		{
			AdventureResult hydrated = EffectPool.get( EffectPool.HYDRATED );
			if ( !KoLConstants.activeEffects.contains( hydrated ) )
			{
				( new AdventureRequest( "Oasis in the Desert", "adventure.php", "122" ) ).run();
			}
			if ( !KoLConstants.activeEffects.contains( hydrated ) )
			{
				( new AdventureRequest( "Oasis in the Desert", "adventure.php", "122" ) ).run();
			}
			if ( !KoLConstants.activeEffects.contains( hydrated ) )
			{
				KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "Ultrahydration failed!" );
			}
		}
		else if ( this.adventureId.equals( "158" ) )
		{
			AdventureResult mop = ItemPool.get( ItemPool.MIZZENMAST_MOP, 1 );
			AdventureResult polish = ItemPool.get( ItemPool.BALL_POLISH, 1 );
			AdventureResult sham = ItemPool.get( ItemPool.RIGGING_SHAMPOO, 1 );
			if ( InventoryManager.hasItem( mop ) &&
				InventoryManager.hasItem( polish ) &&
				InventoryManager.hasItem( sham ) )
			{
				RequestThread.postRequest( new UseItemRequest( mop ) );
				RequestThread.postRequest( new UseItemRequest( polish ) );
				RequestThread.postRequest( new UseItemRequest( sham ) );
			}
		}

		// Update selected adventure information in order to
		// keep the GUI synchronized.

		if ( !Preferences.getString( "lastAdventure" ).equals( this.adventureName ) )
		{
			Preferences.setString( "lastAdventure", this.adventureName );
			AdventureFrame.updateSelectedAdventure( this );
		}

		StaticEntity.getClient().registerAdventure( this );

		RequestLogger.printLine();
		RequestLogger.printLine( "[" + KoLAdventure.getAdventureCount() + "] " + this.getAdventureName() );

		RequestLogger.updateSessionLog();
		RequestLogger.updateSessionLog( "[" + KoLAdventure.getAdventureCount() + "] " + this.getAdventureName() );

		if ( !( this.request instanceof AdventureRequest ) )
		{
			StaticEntity.getClient().registerEncounter( this.getAdventureName(), "Noncombat", null );
		}

		return true;
	}

	public static final boolean recordToSession( final String urlString )
	{
		// In the event that this is an adventure, assume "snarfblat"
		// instead of "adv" in order to determine the location.

		KoLAdventure matchingLocation = AdventureDatabase.getAdventureByURL( urlString );

		if ( matchingLocation != null )
		{
			// If you will be in a drunken stupor, St. Sneaky
			// Pete's day or otherwise, switch to appropriate
			// adventure so logging is correct.

			matchingLocation = KoLAdventure.checkDrunkenness( matchingLocation );

			// Save where we are adventuring currently

			KoLAdventure.lastVisitedLocation = matchingLocation;

			// If we are in a drunken stupor, record it to the
			// session and return now.

			if ( KoLCharacter.isFallingDown() )
			{
				return matchingLocation.recordToSession();
			}

			// We are not drunk. The pyramid's lower chamber is an
			// adventure, but is handled elsewhere

			if ( urlString.startsWith( "pyramid.php" ) )
			{
				return false;
			}

			if ( !matchingLocation.recordToSession() )
			{
				return false;
			}

			if ( !( matchingLocation.getRequest() instanceof AdventureRequest ) || matchingLocation.isValidAdventure )
			{
				return true;
			}

			String locationId = matchingLocation.adventureId;

			// Make sure to visit the untinker before adventuring
			// at Degrassi Knoll.

			if ( locationId.equals( "18" ) )
			{
				UntinkerRequest.canUntinker();
			}

			matchingLocation.isValidAdventure = true;

			// Make sure you're wearing the appropriate equipment
			// for the King's chamber in Cobb's knob.

			if ( matchingLocation.formSource.equals( "knob.php" ) )
			{
				matchingLocation.validate( true );
			}

			return true;
		}

		// Not an internal location.  Perhaps it's something related
		// to another common request?
		
		String location = AdventureDatabase.getUnknownName( urlString );
		if ( location == null )
		{
			return false;
		}

		if ( urlString.indexOf( "?" ) == -1 )
		{
			return true;
		}

		boolean shouldReset =
			urlString.startsWith( "barrel.php" ) ||
			urlString.startsWith( "basement.php" ) ||
			urlString.startsWith( "dungeon.php" ) ||
			urlString.startsWith( "lair" ) ||
			urlString.startsWith( "rats.php" );

		if ( shouldReset )
		{
			KoLAdventure.resetAutoAttack();
		}

		RequestLogger.printLine();
		RequestLogger.printLine( "[" + KoLAdventure.getAdventureCount() + "] " + location );

		RequestLogger.updateSessionLog();
		RequestLogger.updateSessionLog( "[" + KoLAdventure.getAdventureCount() + "] " + location );

		StaticEntity.getClient().registerAdventure( location );

		String encounter = "";

		if ( urlString.startsWith( "basement.php" ) )
		{
			encounter = BasementRequest.getBasementLevelSummary();
		}
		else if ( urlString.startsWith( "dungeon.php" ) )
		{
			encounter = DungeonDecorator.getDungeonEncounter();
		}

		if ( !encounter.equals( "" ) )
		{
			RequestLogger.printLine( encounter );
			RequestLogger.updateSessionLog( encounter );
		}

		return true;
	}

	private static final KoLAdventure checkDrunkenness( KoLAdventure location )
	{
		if ( location == null )
		{
			return null;
		}

		int inebriety = KoLCharacter.getInebriety();
                int limit = KoLCharacter.getInebrietyLimit();

		if ( inebriety <= limit )
		{
			return location;
		}

		if ( inebriety >= 26 && HolidayDatabase.getHoliday().equals( "St. Sneaky Pete's Day" ) )
		{
			// St. Sneaky Pete's Day Drunken Stupor
			return AdventureDatabase.getAdventure( "St. Sneaky Pete's Day Stupor" );
		}

		// Regular Drunken Stupor
		return AdventureDatabase.getAdventure( "Drunken Stupor" );
	}

	public static final int getAdventureCount()
	{
		return Preferences.getBoolean( "logReverseOrder" ) ? KoLCharacter.getAdventuresLeft() : KoLCharacter.getCurrentRun() + 1;
	}

	public int compareTo( final Object o )
	{
		if ( o == null || !( o instanceof KoLAdventure ) )
		{
			return 1;
		}

		KoLAdventure ka = (KoLAdventure) o;

		// Put things with no evade rating at bottom of list.

		int evade1 = this.areaSummary == null ? Integer.MAX_VALUE : this.areaSummary.minEvade();
		int evade2 = ka.areaSummary == null ? Integer.MAX_VALUE : ka.areaSummary.minEvade();

		if ( evade1 == evade2 )
		{
			return this.adventureName.compareTo( ka.adventureName );
		}

		return evade1 - evade2;
	}
}
