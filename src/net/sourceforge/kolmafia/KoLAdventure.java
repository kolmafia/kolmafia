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

import java.util.ArrayList;
import net.sourceforge.foxtrot.Job;

public class KoLAdventure extends Job implements KoLConstants, Comparable
{
	private static final AdventureResult HYDRATED = new AdventureResult( "Ultrahydrated", 1, true );
	public static final KoLRequest ZONE_VALIDATOR = new KoLRequest( "", true );

	public static final AdventureResult AMNESIA = new AdventureResult( "Amnesia", 1, true );
	private static final AdventureResult PERFUME_ITEM = new AdventureResult( 307, 1 );
	private static final AdventureResult PERFUME_EFFECT = new AdventureResult( "Knob Goblin Perfume", 1, true );

	public static final AdventureResult DINGHY = new AdventureResult( 141, 1 );
	private static final AdventureResult PLANS = new AdventureResult( 146, 1 );
	private static final AdventureResult TRANSFUNCTIONER = new AdventureResult( 458, 1 );
	private static final AdventureResult TALISMAN = new AdventureResult( 486, 1 );
	private static final AdventureResult SONAR = new AdventureResult( 563, 1 );
	private static final AdventureResult LIBRARY_KEY = new AdventureResult( 1764, 1 );
	private static final AdventureResult GALLERY_KEY = new AdventureResult( 1765, 1 );
	private static final AdventureResult BALLROOM_KEY = new AdventureResult( 1766, 1 );

	public static final AdventureResult MEATCAR = new AdventureResult( 134, 1 );
	public static final AdventureResult BEAN = new AdventureResult( 186, 1 );

	public static final AdventureResult MAP = new AdventureResult( 667, 1 );
	public static final AdventureResult ROWBOAT = new AdventureResult( 653, 1 );

	public static final AdventureResult SOCK = new AdventureResult( 609, 1 );
	public static final AdventureResult [] IMMATERIA = { new AdventureResult( 605, -1 ), new AdventureResult( 606, -1 ), new AdventureResult( 607, -1 ), new AdventureResult( 608, -1 ) };

	private static final AdventureResult MUSHROOM = new AdventureResult( 1622, 1 );
	private static final AdventureResult ASTRAL = new AdventureResult( "Half-Astral", 0 );
	public static final AdventureResult BEATEN_UP = new AdventureResult( "Beaten Up", 1, true );

	private static KoLAdventure lastVisitedLocation = null;

	private static String changedAutoAttack = "";
	private static String initialAutoAttack = "";

	private boolean isValidAdventure = false;
	private int baseRequirement, buffedRequirement;
	private String zone, parentZone, adventureId, formSource, adventureName;

	private KoLRequest request;
	private AreaCombatData areaSummary;
	private boolean isNonCombatsOnly;
	private boolean isLikelyUnluckyZone;

	/**
	 * Constructs a new <code>KoLAdventure</code> with the given
	 * specifications.
	 *
	 * @param	formSource	The form associated with the given adventure
	 * @param	adventureId	The identifier for this adventure, relative to its form
	 * @param	adventureName	The string form, or name of this adventure
	 */

	public KoLAdventure( String zone, String baseRequirement, String buffedRequirement, String formSource, String adventureId, String adventureName )
	{
		this.zone = zone;
		this.parentZone = (String) AdventureDatabase.PARENT_ZONES.get( zone );
		this.baseRequirement = StaticEntity.parseInt( baseRequirement );
		this.buffedRequirement = StaticEntity.parseInt( buffedRequirement );
		this.formSource = formSource;
		this.adventureId = adventureId;
		this.adventureName = adventureName;

		if ( formSource.equals( "sewer.php" ) )
			this.request = new SewerRequest( false );
		else if ( formSource.equals( "luckysewer.php" ) )
			this.request = new SewerRequest( true );
		else if ( formSource.equals( "campground.php" ) )
			this.request = new CampgroundRequest( adventureId );
		else if ( formSource.equals( "clan_gym.php" ) )
			this.request = new ClanGymRequest( StaticEntity.parseInt( adventureId ) );
		else
			this.request = new AdventureRequest( adventureName, formSource, adventureId );

		this.areaSummary = AdventureDatabase.getAreaCombatData( adventureName );
		this.isLikelyUnluckyZone = false;

		if ( adventureId == null )
			return;

		this.isLikelyUnluckyZone =
			adventureId.equals( "15" ) ||  // Spooky Forest
			adventureId.equals( "16" ) ||  // The Haiku Dungeon
			adventureId.equals( "19" ) ||  // The Limerick Dungeon
			adventureId.equals( "112" ) || // Sleazy Back Alley
			adventureId.equals( "113" ) || // The Haunted Pantry
			adventureId.equals( "114" );   // Outskirts of The Knob

		this.isNonCombatsOnly = !(this.request instanceof AdventureRequest) ||
			(this.areaSummary != null && this.areaSummary.combats() == 0 && this.areaSummary.getMonsterCount() == 0);
	}

	/**
	 * Returns the form source for this adventure.
	 */

	public String getFormSource()
	{	return this.formSource;
	}

	/**
	 * Returns the name where this zone is found.
	 */

	public String getZone()
	{	return this.zone;
	}

	public String getParentZone()
	{	return this.parentZone;
	}

	/**
	 * Returns the name of this adventure.
	 */

	public String getAdventureName()
	{	return this.adventureName;
	}

	/**
	 * Returns the adventure Id for this adventure.
	 * @return	The adventure Id for this adventure
	 */

	public String getAdventureId()
	{	return this.adventureId;
	}

	public AreaCombatData getAreaSummary()
	{	return this.areaSummary;
	}

	public boolean isNonCombatsOnly()
	{	return this.isNonCombatsOnly;
	}

	/**
	 * Returns the request associated with this adventure.
	 * @return	The request for this adventure
	 */

	public KoLRequest getRequest()
	{	return this.request;
	}

	private boolean meetsGeneralRequirements()
	{
		if ( !(this.request instanceof AdventureRequest) )
		{
			this.isValidAdventure = true;
			return true;
		}

		if ( this.zone.equals( "MusSign" ) && !KoLCharacter.inMuscleSign() )
		{
			this.isValidAdventure = false;
			return false;
		}

		if ( this.zone.equals( "MysSign" ) && !KoLCharacter.inMysticalitySign() )
		{
			this.isValidAdventure = false;
			return false;
		}

		if ( this.zone.equals( "MoxSign" ) && !KoLCharacter.inMoxieSign() )
		{
			this.isValidAdventure = false;
			return false;
		}

		int baseValue = 0;
		int buffedValue = 0;

		switch ( KoLCharacter.getPrimeIndex() )
		{
		case 0:
			baseValue = KoLCharacter.getBaseMuscle();
			buffedValue = KoLCharacter.getAdjustedMuscle();
			break;
		case 1:
			baseValue = KoLCharacter.getBaseMysticality();
			buffedValue = KoLCharacter.getAdjustedMysticality();
			break;
		case 2:
			baseValue = KoLCharacter.getBaseMoxie();
			buffedValue = KoLCharacter.getAdjustedMoxie();
			break;
		}

		return baseValue >= this.baseRequirement && buffedValue >= this.buffedRequirement;
	}

	/**
	 * Checks the map location of the given zone.  This is to ensure that
	 * KoLmafia arms any needed flags (such as for the beanstalk).
	 */

	private void validate( boolean visitedCouncil )
	{
		if ( this.zone.equals( "Astral" ) )
		{
			// Update the choice adventure setting

			if ( this.adventureId.equals( "96" ) )
				StaticEntity.setProperty( "choiceAdventure71", "1" );
			else if ( this.adventureId.equals( "98" ) )
				StaticEntity.setProperty( "choiceAdventure71", "2" );
			else
				StaticEntity.setProperty( "choiceAdventure71", "3" );

			// If the player is not half-astral, then
			// make sure they are before continuing.

			if ( !activeEffects.contains( ASTRAL ) )
			{
				this.isValidAdventure = AdventureDatabase.retrieveItem( MUSHROOM );
				if ( this.isValidAdventure )
					DEFAULT_SHELL.executeLine( "use astral mushroom" );
			}

			this.isValidAdventure = activeEffects.contains( ASTRAL );
			return;
		}

		// Fighting the Goblin King requires effects

		if ( this.formSource.equals( "knob.php" ) )
		{
			int outfitId = EquipmentDatabase.getOutfitId( this );

			if ( !activeEffects.contains( PERFUME_EFFECT ) )
			{
				if ( !AdventureDatabase.retrieveItem( PERFUME_ITEM ) )
					return;
			}

			if ( !EquipmentDatabase.isWearingOutfit( outfitId ) )
			{
				if ( !EquipmentDatabase.retrieveOutfit( outfitId ) )
					return;
			}

			RequestThread.postRequest( new EquipmentRequest( EquipmentDatabase.getOutfit( outfitId ) ) );
			if ( !activeEffects.contains( PERFUME_EFFECT ) )
				RequestThread.postRequest( new ConsumeItemRequest( PERFUME_ITEM ) );
		}

		// Pirate cove should not be visited before level 9.
		if ( this.adventureId.equals( "67" ) )
		{
			if ( KoLCharacter.getLevel() < 9 )
			{
				KoLmafia.updateDisplay( ABORT_STATE, "The dictionary won't drop right now." );
				return;
			}

			// If it's the pirate quest in disguise, make sure
			// you visit the council beforehand.

			if ( !this.isValidAdventure )
				DEFAULT_SHELL.executeLine( "council" );
		}

		// Disguise zones require outfits
		if ( !this.adventureId.equals( "85" ) && (this.adventureName.indexOf( "Disguise" ) != -1 || this.adventureName.indexOf( "Uniform" ) != -1) )
		{
			int outfitId = EquipmentDatabase.getOutfitId( this );

			if ( outfitId > 0 && !EquipmentDatabase.isWearingOutfit( outfitId ) )
			{
				this.isValidAdventure = false;
				if ( !EquipmentDatabase.retrieveOutfit( outfitId ) )
					return;

				RequestThread.postRequest( new EquipmentRequest( EquipmentDatabase.getOutfit( outfitId ) ) );
				this.isValidAdventure = true;
			}
		}

		// If the person has a continuum transfunctioner, then find
		// some way of equipping it.  If they do not have one, then
		// acquire one then try to equip it.  If the person has a two
		// handed weapon, then report an error.

		if ( this.adventureId.equals( "73" ) )
		{
			if ( !KoLCharacter.hasItem( TRANSFUNCTIONER ) )
			{
				RequestThread.postRequest( ZONE_VALIDATOR.constructURLString( "mystic.php" ) );
				RequestThread.postRequest( ZONE_VALIDATOR.constructURLString( "mystic.php?action=crackyes1" ) );
				RequestThread.postRequest( ZONE_VALIDATOR.constructURLString( "mystic.php?action=crackyes2" ) );
				RequestThread.postRequest( ZONE_VALIDATOR.constructURLString( "mystic.php?action=crackyes3" ) );
			}

			if ( EquipmentDatabase.getHands( KoLCharacter.getEquipment( KoLCharacter.WEAPON ).getName() ) > 1 )
			{
				KoLmafia.updateDisplay( ERROR_STATE, "You need to free up a hand." );
				return;
			}

			DEFAULT_SHELL.executeLine( "equip " + TRANSFUNCTIONER.getName() );
			this.isValidAdventure = true;
			return;
		}

		if ( this.adventureId.equals( "119" ) )
		{
			if ( !KoLCharacter.hasEquipped( TALISMAN ) )
			{
				if ( !AdventureDatabase.retrieveItem( TALISMAN ) )
					return;

				DEFAULT_SHELL.executeLine( "equip talisman o'nam" );
			}

			this.isValidAdventure = true;
			return;
		}

		if ( this.formSource.indexOf( "adventure.php" ) == -1 )
			this.isValidAdventure = true;

		if ( this.isValidAdventure )
			return;

		// If we're trying to take a trip, make sure it's the right one
		if ( this.adventureId.equals( "96" ) || this.adventureId.equals( "97" ) || this.adventureId.equals( "98" ) )
		{
			// You must be Half-Astral to go on a trip
			int astral = ASTRAL.getCount( ( activeEffects ) );
			if ( astral == 0 )
			{
				DEFAULT_SHELL.executeLine( "use 1 astral mushroom" );
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
					choice = "1";
				else if ( this.adventureId.equals( "98" ) )
					choice = "2";
				else
					choice = "3";

				// Choose the trip
				StaticEntity.setProperty( "choiceAdventure71", choice );

				String name = this.getAdventureName();
				StaticEntity.setProperty( "chosenTrip", name );

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
			this.isValidAdventure = AdventureDatabase.retrieveItem( "casino pass" );
			return;
		}

		// The island is unlocked provided the player
		// has a dingy dinghy in their inventory.

		else if ( this.zone.equals( "Island" ) )
		{
			this.isValidAdventure = KoLCharacter.hasItem( DINGHY );
			if ( !this.isValidAdventure )
			{
				this.isValidAdventure = KoLCharacter.hasItem( PLANS );
				if ( !this.isValidAdventure )
					return;

				this.isValidAdventure = AdventureDatabase.retrieveItem( "dingy planks" );
				if ( this.isValidAdventure )
					DEFAULT_SHELL.executeLine( "use dinghy plans" );
			}

			return;
		}

		// The Castle in the Clouds in the Sky is unlocked provided the
		// character has either a S.O.C.K. or an intragalactic rowboat

		else if ( this.adventureId.equals( "82" ) )
		{
			this.isValidAdventure = KoLCharacter.hasItem( SOCK ) || KoLCharacter.hasItem( ROWBOAT );
			return;
		}

		// The Hole in the Sky is unlocked provided the player has an
		// intragalactic rowboat in their inventory.

		else if ( this.adventureId.equals( "83" ) )
		{
			if ( !KoLCharacter.hasItem( ROWBOAT ) && KoLCharacter.hasItem( MAP ) )
				RequestThread.postRequest( new ConsumeItemRequest( MAP ) );

			this.isValidAdventure = AdventureDatabase.retrieveItem( ROWBOAT );
			return;
		}

		// The beanstalk is unlocked when the player
		// has planted a beanstalk -- but, the zone
		// needs to be armed first.

		else if ( this.adventureId.equals( "81" ) && !KoLCharacter.beanstalkArmed() )
		{
			// If the character has a S.O.C.K. or an intragalactic
			// rowboat, they can get to the airship

			if ( KoLCharacter.hasItem( SOCK ) || KoLCharacter.hasItem( ROWBOAT ) )
			{
				this.isValidAdventure = true;
				return;
			}

			// Obviate following request by checking accomplishment:
			// questlog.php?which=3
			// "You have planted a Beanstalk in the Nearby Plains."

			ZONE_VALIDATOR.constructURLString( "plains.php" );
			RequestThread.postRequest( ZONE_VALIDATOR );

			if ( ZONE_VALIDATOR.responseText.indexOf( "beanstalk.php" ) == -1 )
			{
				// If not, check to see if the player has an enchanted
				// bean which can be used.  If they don't, then try to
				// find one through adventuring.

				if ( !KoLCharacter.hasItem( BEAN ) )
				{
					ArrayList temporary = new ArrayList();
					temporary.addAll( conditions );
					conditions.clear();

					conditions.add( BEAN );
					StaticEntity.getClient().makeRequest( AdventureDatabase.getAdventureByURL( "adventure.php?snarfblat=33" ),
						KoLCharacter.getAdventuresLeft() );

					if ( !conditions.isEmpty() )
						KoLmafia.updateDisplay( ABORT_STATE, "Unable to complete enchanted bean quest." );

					conditions.clear();
					conditions.addAll( temporary );
				}

				// Now that you've retrieved the bean, ensure that
				// it is in your inventory, and then use it.  Take
				// advantage of item consumption automatically doing
				// what's needed in grabbing the item.

				DEFAULT_SHELL.executeLine( "council" );
				DEFAULT_SHELL.executeLine( "use enchanted bean" );
			}

			ZONE_VALIDATOR.constructURLString( "beanstalk.php" );
			RequestThread.postRequest( ZONE_VALIDATOR );

			KoLCharacter.armBeanstalk();
			this.isValidAdventure = true;
			return;
		}

		else if ( this.zone.equals( "Spookyraven" ) )
		{
			// It takes RNG luck at the Haunted Pantry to unlock
			// the rest of Spookyraven Manor. Assume it is
			// unlocked. However, we can verify that the zones that
			// require keys are accessible.

			if ( this.adventureId.equals( "104" ) )
			{
				// Haunted Library
				this.isValidAdventure = AdventureDatabase.retrieveItem( LIBRARY_KEY );
				return;
			}

			if ( this.adventureId.equals( "106" ) )
			{
				// Haunted Gallery
				this.isValidAdventure = AdventureDatabase.retrieveItem( GALLERY_KEY );
				return;
			}

			// It takes a special action to make the upstairs areas
			// available. Assume they are accessible if the player
			// can get into the library
			if ( this.adventureId.equals( "107" ) || this.adventureId.equals( "108" ) )
			{
				// Haunted Bathroom & Bedroom
				this.isValidAdventure = AdventureDatabase.retrieveItem( LIBRARY_KEY );
				return;
			}

			if ( this.adventureId.equals( "109" ) )
			{
				// Haunted Ballroom
				this.isValidAdventure = AdventureDatabase.retrieveItem( BALLROOM_KEY );
				return;
			}
		}

		else if ( this.adventureId.equals( "11" ) )
		{
			this.isValidAdventure = true;
			return;
		}

		else if ( this.adventureId.equals( "32" ) || this.adventureId.equals( "33" ) || this.adventureId.equals( "34" ) )
		{
			RequestThread.postRequest( ZONE_VALIDATOR.constructURLString( "bathole.php" ) );

			if ( this.adventureId.equals( "32" ) && ZONE_VALIDATOR.responseText.indexOf( "batrockleft.gif" ) == -1 )
			{
				this.isValidAdventure = true;
				return;
			}
			if ( this.adventureId.equals( "33" ) && ZONE_VALIDATOR.responseText.indexOf( "batrockright.gif" ) == -1 )
			{
				this.isValidAdventure = true;
				return;
			}
			if ( this.adventureId.equals( "34" ) && ZONE_VALIDATOR.responseText.indexOf( "batrockbottom.gif" ) == -1 )
			{
				this.isValidAdventure = true;
				return;
			}

			int sonarCount = SONAR.getCount( inventory );
			int sonarToUse = 0;

			if ( ZONE_VALIDATOR.responseText.indexOf( "batrockleft.gif" ) != -1 )
				sonarToUse = 3;
			else if ( ZONE_VALIDATOR.responseText.indexOf( "batrockright.gif" ) != -1 )
				sonarToUse = 2;
			else if ( ZONE_VALIDATOR.responseText.indexOf( "batrockbottom.gif" ) != -1 )
				sonarToUse = 1;

			RequestThread.postRequest( new ConsumeItemRequest( SONAR.getInstance( Math.min( sonarToUse, sonarCount ) ) ) );
			RequestThread.postRequest( ZONE_VALIDATOR );

			if ( this.adventureId.equals( "32" ) )
				this.isValidAdventure = ZONE_VALIDATOR.responseText.indexOf( "batrockleft.gif" ) == -1;
			else if ( this.adventureId.equals( "33" ) )
				this.isValidAdventure = ZONE_VALIDATOR.responseText.indexOf( "batrockright.gif" ) == -1;
			else
				this.isValidAdventure = ZONE_VALIDATOR.responseText.indexOf( "batrockbottom.gif" ) == -1;

			return;
		}


		if ( this.adventureId.equals( "100" ) )
		{
			RequestThread.postRequest( ZONE_VALIDATOR.constructURLString( "woods.php" ) );
			this.isValidAdventure = ZONE_VALIDATOR.responseText.indexOf( "grove.gif" ) != -1;

			if ( !visitedCouncil && !this.isValidAdventure )
			{
				StaticEntity.getClient().unlockGuildStore( true );
				this.validate( true );
			}

			return;
		}

		if ( this.zone.equals( "McLarge" ) )
		{
			if ( QuestLogRequest.finishedQuest( QuestLogRequest.TRAPPER ) )
			{
				this.isValidAdventure = true;
				return;
			}

			RequestThread.postRequest( ZONE_VALIDATOR.constructURLString( "mclargehuge.php" ) );
			if ( ZONE_VALIDATOR.responseText.indexOf( this.adventureId ) != -1 )
			{
				this.isValidAdventure = true;
				return;
			}

			if ( visitedCouncil )
			{
				KoLmafia.updateDisplay( "You must complete a trapper task first." );
				return;
			}

			DEFAULT_SHELL.executeLine( "council" );
			RequestThread.postRequest( ZONE_VALIDATOR.constructURLString( "trapper.php" ) );

			this.validate( true );
			return;
		}

		this.isValidAdventure = true;
	}

	/**
	 * Retrieves the string form of the adventure contained within this
	 * encapsulation, which is generally the name of the adventure.
	 *
	 * @return	The string form of the adventure
	 */

	public String toString()
	{
		boolean canAdventureHere = this.meetsGeneralRequirements();
		StringBuffer stringForm = new StringBuffer();

		if ( !canAdventureHere )
			stringForm.append( "<html><font color=gray>" );

		stringForm.append( this.zone );
		stringForm.append( ": " );

		stringForm.append( this.adventureName );
		if ( !canAdventureHere )
			stringForm.append( "</font></html>" );

		return stringForm.toString();
	}

	/**
	 * Executes the appropriate <code>KoLRequest</code> for the adventure
	 * encapsulated by this <code>KoLAdventure</code>.
	 */

	public void run()
	{
		if ( !KoLmafia.isRunningBetweenBattleChecks() && !(this.request instanceof CampgroundRequest) )
		{
			if ( !StaticEntity.getClient().runThresholdChecks() )
				return;

			lastVisitedLocation = this;
			StaticEntity.getClient().runBetweenBattleChecks( this.isNonCombatsOnly() );

			if ( !KoLmafia.permitsContinue() )
				return;
		}

		// Validate the adventure before running it.
		// If it's invalid, return and do nothing.

		if ( !this.meetsGeneralRequirements() )
		{
			KoLmafia.updateDisplay( ERROR_STATE, "Insufficient stats to adventure at " + this.adventureName + "." );
			return;
		}

		this.validate( false );
		if ( !this.isValidAdventure )
		{
			KoLmafia.updateDisplay( ERROR_STATE, "That area is not available." );
			return;
		}

		if ( this.getFormSource().equals( "shore.php" ) && KoLCharacter.getAvailableMeat() < 500 )
		{
			KoLmafia.updateDisplay( ERROR_STATE, "Insufficient funds for shore vacation." );
			return;
		}

		String action = StaticEntity.getProperty( "battleAction" );

		if ( this.request instanceof AdventureRequest && !this.adventureId.equals( "80" ) )
		{
			if ( !this.isNonCombatsOnly() && action.indexOf( "dictionary" ) != -1 && (FightRequest.DICTIONARY1.getCount( inventory ) < 1 && FightRequest.DICTIONARY2.getCount( inventory ) < 1) )
			{
				KoLmafia.updateDisplay( ERROR_STATE, "Sorry, you don't have a dictionary." );
				return;
			}
		}

		if ( !KoLmafia.permitsContinue() )
			return;

		// Make sure there are enough adventures to run the request
		// so that you don't spam the server unnecessarily.

		if ( KoLCharacter.getAdventuresLeft() == 0 || KoLCharacter.getAdventuresLeft() < this.request.getAdventuresUsed() )
		{
			KoLmafia.updateDisplay( ERROR_STATE, "Insufficient adventures to continue." );
			return;
		}

		if ( !this.isNonCombatsOnly() && this.request instanceof AdventureRequest )
		{
			// Check for dictionaries as a battle strategy, if the
			// person is not adventuring at the chasm.

			if ( !this.adventureId.equals( "80" ) && this.request.getAdventuresUsed() == 1 && action.indexOf( "dictionary" ) != -1 )
			{
				// Only allow damage-dealing familiars when using
				// stasis techniques.

				if ( !KoLCharacter.getFamiliar().isCombatFamiliar() )
				{
					KoLmafia.updateDisplay( ERROR_STATE, "A dictionary would be useless there." );
					return;
				}
			}

			// If the person doesn't stand a chance of surviving,
			// automatically quit and tell them so.

			if ( (action.startsWith( "attack" ) || activeEffects.contains( AMNESIA )) && this.areaSummary != null && !this.areaSummary.willHitSomething() )
			{
				if ( !KoLCharacter.getFamiliar().isCombatFamiliar() )
				{
					KoLmafia.updateDisplay( ERROR_STATE, activeEffects.contains( AMNESIA ) ? "You have amnesia." : "You can't hit anything there." );
					return;
				}
			}

			if ( ( action.equals( "skill thrust-smack" ) || action.equals( "skill lunging thrust-smack" ) ) &&
				EquipmentDatabase.isRanged( KoLCharacter.getEquipment( KoLCharacter.WEAPON ).getItemId() ) )
			{
				KoLmafia.updateDisplay( ERROR_STATE, "Thrust smacks are useless with ranged weapons." );
				return;
			}
		}

		// If the test is successful, then it is safe to run the
		// request (without spamming the server).

		RequestThread.postRequest( this.request );
	}

	public static KoLAdventure lastVisitedLocation()
	{	return lastVisitedLocation;
	}

	public static void resetAutoAttack()
	{
		// In the event that the user made some sort of change
		// to their auto-attack settings, do nothing.

		if ( initialAutoAttack.equals( "0" ) )
			DEFAULT_SHELL.executeCommand( "set", "defaultAutoAttack=0" );

		initialAutoAttack = "";
	}

	private void updateAutoAttack()
	{
		String attack = StaticEntity.getProperty( "battleAction" );
		String autoAttack = StaticEntity.getProperty( "defaultAutoAttack" );

		if ( initialAutoAttack.equals( "" ) || !autoAttack.equals( changedAutoAttack ) )
			initialAutoAttack = autoAttack;

		// If the player is pickpocketing, they probably do not want
		// their auto-attack reset to an attack.

		if ( autoAttack.equals( "3" ) || (!KoLCharacter.canInteract() && KoLCharacter.isMoxieClass()) )
			return;

		// Areas that have no combats do not need to have auto-attack
		// reset.  Therefore, skip out.

		if ( FightRequest.getActualRound() != 0 || isNonCombatsOnly() )
			return;

		// If you're searching for special scrolls, do not enable
		// your auto-attack.

		if ( adventureId.equals( "80" ) && conditions.contains( FightRequest.SCROLL_668 ) ||
			conditions.contains( FightRequest.SCROLL_64735 ) || conditions.contains( FightRequest.SCROLL_31337 ) )
		{
			resetAutoAttack();
			return;
		}

		// If this is not an automated request, make sure to turn off
		// auto-attack if it was off before any automation started.
		// Custom combat and deleveling do not need to have auto-attack
		// changed, because these users probably are micro-managing.

		if ( !KoLmafia.isAdventuring() || attack.startsWith( "custom" ) || attack.startsWith( "delevel" ) || attack.startsWith( "item" ) )
		{
			resetAutoAttack();
			return;
		}

		// If it's not a generic class skill (it's id is something
		// non-standard), then don't update auto-attack.

		if ( !attack.startsWith( "attack" ) )
		{
			int skillId = ClassSkillsDatabase.getSkillId( attack.substring(6).trim() );

			if ( skillId < 1000 || skillId > 7000 )
			{
				resetAutoAttack();
				return;
			}
		}

		DEFAULT_SHELL.executeCommand( "set", "defaultAutoAttack=" + attack );
		changedAutoAttack = StaticEntity.getProperty( "defaultAutoAttack" );
	}

	public void recordToSession()
	{
		lastVisitedLocation = this;
		this.updateAutoAttack();

		// Run between-combat scripts here to avoid potential
		// sidepane conflict issues.

		if ( adventureId.equals( "123" ) && !activeEffects.contains( HYDRATED ) )
			(new AdventureRequest( "Oasis in the Desert", "adventure.php", "122" )).run();

		// Update selected adventure information in order to
		// keep the GUI synchronized.

		if ( !StaticEntity.getProperty( "lastAdventure" ).equals( this.adventureName ) )
		{
			StaticEntity.setProperty( "lastAdventure", this.adventureName );

			if ( !this.isNonCombatsOnly() )
			{
				AdventureFrame.updateSelectedAdventure( this );
				CharsheetFrame.updateSelectedAdventure( this );
			}
		}

		if ( !KoLmafia.isAdventuring() )
		{
			RequestLogger.printLine();
			RequestLogger.printLine( "[" + getAdventureCount() + "] " + this.getAdventureName() );
		}

		RequestLogger.updateSessionLog();
		RequestLogger.updateSessionLog( "[" + getAdventureCount() + "] " + this.getAdventureName() );

		StaticEntity.getClient().registerAdventure( this );

		if ( !(request instanceof AdventureRequest) )
			StaticEntity.getClient().registerEncounter( this.getAdventureName(), "Noncombat" );
	}

	public static boolean recordToSession( String urlString )
	{
		// In the event that this is an adventure, assume "snarfblat"
		// instead of "adv" in order to determine the location.

		KoLAdventure matchingLocation = AdventureDatabase.getAdventureByURL( urlString );

		if ( matchingLocation != null )
		{
			UseSkillRequest.revertCheckpointOutfit();

			matchingLocation.recordToSession();
			String locationId = matchingLocation.adventureId;

			// Disassemble clovers when going to areas where the
			// player has a high probability of accidentally using
			// a ten-leaf clover.

			if ( StaticEntity.getBooleanProperty( "cloverProtectActive" ) && matchingLocation.isLikelyUnluckyZone )
				DEFAULT_SHELL.executeLine( "use * ten-leaf clover" );

			if ( !(matchingLocation.getRequest() instanceof AdventureRequest) || matchingLocation.isValidAdventure )
				return true;

			// Do some quick adventure validation, which allows you
			// to unlock the bat zone.

			if ( locationId.equals( "32" ) || locationId.equals( "33" ) || locationId.equals( "34" ) )
				if ( !AdventureDatabase.validateZone( "BatHole", locationId ) )
					return true;

			// Make sure to visit the untinkerer before adventuring
			// at Degrassi Knoll.

			if ( locationId.equals( "18" ) )
				UntinkerRequest.canUntinker();

			// Check the council before you adventure at the pirates
			// in disguise, if your last council visit was a long
			// time ago.

			if ( locationId.equals( "67" ) && StaticEntity.getIntegerProperty( "lastCouncilVisit" ) < 9 )
				DEFAULT_SHELL.executeLine( "council" );

			matchingLocation.isValidAdventure = true;

			// Make sure you're wearing the appropriate equipment for
			// the King's chamber in Cobb's knob.

			if ( matchingLocation.formSource.equals( "knob.php" ) )
				matchingLocation.validate( true );

			return true;
		}

		// Not an internal location.  Perhaps it's something related
		// to another common request?

		String location = null;
		boolean shouldReset = true;

		if ( urlString.startsWith( "adventure.php" ) && urlString.indexOf( "snarfblat=122" ) != -1 )
		{
			shouldReset = false;
			location = "Oasis in the Desert";
		}
		else if ( urlString.startsWith( "shore.php" ) && urlString.indexOf( "whichtrip=1" ) != -1 )
		{
			shouldReset = false;
			location = "Muscle Vacation";
		}
		else if ( urlString.startsWith( "shore.php" ) && urlString.indexOf( "whichtrip=2" ) != -1 )
		{
			shouldReset = false;
			location = "Mysticality Vacation";
		}
		else if ( urlString.startsWith( "shore.php" ) && urlString.indexOf( "whichtrip=3" ) != -1 )
		{
			shouldReset = false;
			location = "Moxie Vacation";
		}
		else if ( urlString.startsWith( "guild.php?action=chal" ) )
		{
			shouldReset = false;
			location = "Guild Challenge";
		}
		else if ( urlString.startsWith( "dungeon.php" ) )
		{
			shouldReset = true;
			location = "Daily Dungeon";
		}
		else if ( urlString.startsWith( "rats.php" ) )
		{
			shouldReset = true;
			location = "Typical Tavern Quest";
		}
		else if ( urlString.startsWith( "barrel.php" ) )
		{
			shouldReset = true;
			location = "Barrel Full of Barrels";
		}
		else if ( urlString.startsWith( "mining.php" ) )
		{
			shouldReset = false;
			location = "Itznotyerzitz Mine (In Disguise)";
		}
		else if ( urlString.startsWith( "arena.php" ) && urlString.indexOf( "action" ) != -1 )
		{
			shouldReset = false;
			location = "Cake-Shaped Arena";
		}
		else if ( urlString.startsWith( "lair4.php?action=level1" ) )
		{
			shouldReset = true;
			location = "Sorceress Tower: Level 1";
		}
		else if ( urlString.startsWith( "lair4.php?action=level2" ) )
		{
			shouldReset = true;
			location = "Sorceress Tower: Level 2";
		}
		else if ( urlString.startsWith( "lair4.php?action=level3" ) )
		{
			shouldReset = true;
			location = "Sorceress Tower: Level 3";
		}
		else if ( urlString.startsWith( "lair5.php?action=level1" ) )
		{
			shouldReset = true;
			location = "Sorceress Tower: Level 4";
		}
		else if ( urlString.startsWith( "lair5.php?action=level2" ) )
		{
			shouldReset = true;
			location = "Sorceress Tower: Level 5";
		}
		else if ( urlString.startsWith( "lair5.php?action=level3" ) )
		{
			shouldReset = true;
			location = "Sorceress Tower: Level 6";
		}
		else if ( urlString.startsWith( "lair6.php?place=0" ) )
		{
			shouldReset = false;
			location = "Sorceress Tower: Door Puzzles";
		}
		else if ( urlString.startsWith( "lair6.php?place=2" ) )
		{
			shouldReset = true;
			location = "Sorceress Tower: Shadow Fight";
		}
		else if ( urlString.startsWith( "lair6.php?place=5" ) )
		{
			shouldReset = true;
			location = "Sorceress Tower: Naughty Sorceress";
		}
		else if ( urlString.startsWith( "hiddencity.php" ) )
		{
			shouldReset = true;
			location = "Hidden City: Unexplored Ruins";
		}

		if ( location == null )
			return false;

		if ( urlString.indexOf( "?" ) == -1 )
			return true;

		UseSkillRequest.revertCheckpointOutfit();

		if ( shouldReset )
			resetAutoAttack();

		if ( !KoLmafia.isAdventuring() )
		{
			RequestLogger.printLine();
			RequestLogger.printLine( "[" + getAdventureCount() + "] " + location );
		}

		RequestLogger.updateSessionLog();
		RequestLogger.updateSessionLog( "[" + getAdventureCount() + "] " + location );
		return true;
	}

	public static int getAdventureCount()
	{
		return StaticEntity.getBooleanProperty( "logReverseOrder" ) ? KoLCharacter.getAdventuresLeft() :
			KoLCharacter.getCurrentRun() + 1;
	}

	public int compareTo( Object o )
	{
		if ( o == null || !( o instanceof KoLAdventure ) )
			return 1;

		KoLAdventure ka = (KoLAdventure) o;

		// Put things with no evade rating at bottom of list.

		int evade1 = this.areaSummary == null ? Integer.MAX_VALUE : this.areaSummary.minEvade();
		int evade2 = ka.areaSummary == null ? Integer.MAX_VALUE : ka.areaSummary.minEvade();

		if ( evade1 == evade2 )
			return this.adventureName.compareTo( ka.adventureName );

		return evade1 - evade2;
	}
}
