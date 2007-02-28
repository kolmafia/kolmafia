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
import java.util.List;
import net.sourceforge.foxtrot.Job;

public class KoLAdventure extends Job implements KoLConstants, Comparable
{
	private static final KoLRequest ZONE_VALIDATOR = AdventureDatabase.ZONE_VALIDATOR;

	private static final AdventureResult PERFUME_ITEM = new AdventureResult( 307, 1 );
	private static final AdventureResult PERFUME_EFFECT = new AdventureResult( "Knob Goblin Perfume", 1, false );

	private static final AdventureResult DINGHY = new AdventureResult( 141, 1 );
	private static final AdventureResult PLANS = new AdventureResult( 146, 1 );
	private static final AdventureResult TRANSFUNCTIONER = new AdventureResult( 458, 1 );
	private static final AdventureResult LIBRARY_KEY = new AdventureResult( 1764, 1 );
	private static final AdventureResult GALLERY_KEY = new AdventureResult( 1765, 1 );
	private static final AdventureResult BALLROOM_KEY = new AdventureResult( 1766, 1 );

	public static final AdventureResult MEATCAR = new AdventureResult( 134, 1 );
	public static final AdventureResult BEAN = new AdventureResult( 186, 1 );
	public static final AdventureResult ROWBOAT = new AdventureResult( 653, 1 );

	public static final AdventureResult SOCK = new AdventureResult( 609, 1 );
	public static final AdventureResult [] IMMATERIA = { new AdventureResult( 605, -1 ), new AdventureResult( 606, -1 ), new AdventureResult( 607, -1 ), new AdventureResult( 608, -1 ) };

	private static final AdventureResult MUSHROOM = new AdventureResult( 1622, 1 );
	private static final AdventureResult ASTRAL = new AdventureResult( "Half-Astral", 0 );
	public static final AdventureResult BEATEN_UP = new AdventureResult( "Beaten Up", 1, true );

	private boolean isValidAdventure = false;
	private int baseRequirement, buffedRequirement;
	private String zone, parentZone, adventureId, formSource, adventureName;
	private KoLRequest request;
	private AreaCombatData areaSummary;
	private boolean shouldRunFullCheck;

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
		{
			shouldRunFullCheck = false;
			this.request = new SewerRequest( false );
		}
		else if ( formSource.equals( "luckysewer.php" ) )
		{
			shouldRunFullCheck = false;
			this.request = new SewerRequest( true );
		}
		else if ( formSource.equals( "campground.php" ) )
		{
			shouldRunFullCheck = false;
			this.request = new CampgroundRequest( adventureId );
		}
		else if ( formSource.equals( "clan_gym.php" ) )
		{
			shouldRunFullCheck = false;
			this.request = new ClanGymRequest( StaticEntity.parseInt( adventureId ) );
		}
		else
		{
			shouldRunFullCheck = formSource.equals( "adventure.php" ) || formSource.equals( "dungeon.php" ) ||
				formSource.equals( "rats.php" ) || formSource.equals( "knob.php" ) || formSource.equals( "cyrpt.php" ) || formSource.equals( "lair3.php" );

			this.request = new AdventureRequest( adventureName, formSource, adventureId );
		}

		this.areaSummary = AdventureDatabase.getAreaCombatData( adventureName );
	}

	public boolean runsBetweenBattleScript()
	{	return shouldRunFullCheck;
	}

	/**
	 * Returns the form source for this adventure.
	 */

	public String getFormSource()
	{	return formSource;
	}

	/**
	 * Returns the name where this zone is found.
	 */

	public String getZone()
	{	return zone;
	}

	public String getParentZone()
	{	return parentZone;
	}

	/**
	 * Returns the name of this adventure.
	 */

	public String getAdventureName()
	{	return adventureName;
	}

	/**
	 * Returns the adventure Id for this adventure.
	 * @return	The adventure Id for this adventure
	 */

	public String getAdventureId()
	{	return adventureId;
	}

	public AreaCombatData getAreaSummary()
	{	return areaSummary;
	}

	/**
	 * Returns the request associated with this adventure.
	 * @return	The request for this adventure
	 */

	public KoLRequest getRequest()
	{	return request;
	}

	private boolean meetsGeneralRequirements()
	{
		if ( !(request instanceof AdventureRequest) )
		{
			isValidAdventure = true;
			return true;
		}

		if ( formSource.equals( "lair6.php" ) )
		{
			isValidAdventure = KoLCharacter.hasEquipped( SorceressLair.NAGAMAR );
			return isValidAdventure;
		}

		if ( zone.equals( "MusSign" ) && !KoLCharacter.inMuscleSign() )
		{
			isValidAdventure = false;
			return false;
		}

		if ( zone.equals( "MysSign" ) && !KoLCharacter.inMysticalitySign() )
		{
			isValidAdventure = false;
			return false;
		}

		if ( zone.equals( "MoxSign" ) && !KoLCharacter.inMoxieSign() )
		{
			isValidAdventure = false;
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

		return baseValue >= baseRequirement && buffedValue >= buffedRequirement;
	}

	/**
	 * Checks the map location of the given zone.  This is to ensure that
	 * KoLmafia arms any needed flags (such as for the beanstalk).
	 */

	private void validate( boolean visitedCouncil )
	{
		if ( zone.equals( "Astral" ) )
		{
			// Update the choice adventure setting

			if ( adventureId.equals( "96" ) )
				StaticEntity.setProperty( "choiceAdventure71", "1" );
			else if ( adventureId.equals( "98" ) )
				StaticEntity.setProperty( "choiceAdventure71", "2" );
			else
				StaticEntity.setProperty( "choiceAdventure71", "3" );

			// If the player is not half-astral, then
			// make sure they are before continuing.

			if ( !activeEffects.contains( ASTRAL ) )
			{
				isValidAdventure = AdventureDatabase.retrieveItem( MUSHROOM );
				if ( isValidAdventure )
					DEFAULT_SHELL.executeLine( "use astral mushroom" );
			}

			isValidAdventure = activeEffects.contains( ASTRAL );
			return;
		}

		// Fighting the Goblin King requires effects

		if ( formSource.equals( "knob.php" ) )
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
		if ( adventureId.equals( "67" ) )
		{
			if ( KoLCharacter.getLevel() < 9 )
			{
				KoLmafia.updateDisplay( ABORT_STATE, "The dictionary won't drop right now." );
				return;
			}

			// If it's the pirate quest in disguise, make sure
			// you visit the council beforehand.

			if ( !isValidAdventure )
				DEFAULT_SHELL.executeLine( "council" );
		}

		// Disguise zones require outfits
		if ( adventureName.indexOf( "Disguise" ) != -1 || adventureName.indexOf( "Uniform" ) != -1 )
		{
			int outfitId = EquipmentDatabase.getOutfitId( this );

			if ( outfitId != -1 && !EquipmentDatabase.isWearingOutfit( outfitId ) )
			{
				isValidAdventure = false;
				if ( !EquipmentDatabase.retrieveOutfit( outfitId ) )
					return;

				RequestThread.postRequest( new EquipmentRequest( EquipmentDatabase.getOutfit( outfitId ) ) );
				isValidAdventure = true;
			}
		}

		// If the person has a continuum transfunctioner, then find
		// some way of equipping it.  If they do not have one, then
		// acquire one then try to equip it.  If the person has a two
		// handed weapon, then report an error.

		if ( adventureId.equals( "73" ) )
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
			isValidAdventure = true;
			return;
		}

		if ( formSource.indexOf( "adventure.php" ) == -1 )
			isValidAdventure = true;

		if ( isValidAdventure )
			return;

		// If we're trying to take a trip, make sure it's the right one
		if ( adventureId.equals( "96" ) || adventureId.equals( "97" ) || adventureId.equals( "98" ) )
		{
			// You must be Half-Astral to go on a trip
			int astral = ASTRAL.getCount( ( activeEffects ) );
			if ( astral == 0 )
			{
				DEFAULT_SHELL.executeLine( "use 1 astral mushroom" );
				if ( !KoLmafia.permitsContinue() )
				{
					isValidAdventure = false;
					return;
				}
			}

			// If we haven't selected a trip yet, do so now
			if ( astral == 5 )
			{
				String choice;
				if ( adventureId.equals( "96" ) )
					choice = "1";
				else if ( adventureId.equals( "98" ) )
					choice = "2";
				else
					choice = "3";

				// Choose the trip
				StaticEntity.setProperty( "choiceAdventure71", choice );

				String name = getAdventureName();
				StaticEntity.setProperty( "chosenTrip", name );

				isValidAdventure = true;
				return;
			}

			isValidAdventure = true;
			return;
		}

		// The casino is unlocked provided the player
		// has a casino pass in their inventory.

		if ( zone.equals( "Casino" ) )
		{
			isValidAdventure = AdventureDatabase.retrieveItem( "casino pass" );
			return;
		}

		// The island is unlocked provided the player
		// has a dingy dinghy in their inventory.

		else if ( zone.equals( "Island" ) )
		{
			isValidAdventure = KoLCharacter.hasItem( DINGHY );
			if ( !isValidAdventure )
			{
				isValidAdventure = KoLCharacter.hasItem( PLANS );
				if ( !isValidAdventure )
					return;

				isValidAdventure = AdventureDatabase.retrieveItem( "dingy planks" );
				if ( isValidAdventure )
					DEFAULT_SHELL.executeLine( "use dinghy plans" );
			}

			return;
		}

		// The Castle in the Clouds in the Sky is unlocked provided the
		// character has either a S.O.C.K. or an intragalactic rowboat

		else if ( adventureId.equals( "82" ) )
		{
			isValidAdventure = KoLCharacter.hasItem( SOCK ) || KoLCharacter.hasItem( ROWBOAT );
			return;
		}

		// The Hole in the Sky is unlocked provided the player has an
		// intragalactic rowboat in their inventory.

		else if ( adventureId.equals( "83" ) )
		{
			isValidAdventure = AdventureDatabase.retrieveItem( ROWBOAT );
			return;
		}

		// The beanstalk is unlocked when the player
		// has planted a beanstalk -- but, the zone
		// needs to be armed first.

		else if ( adventureId.equals( "81" ) && !KoLCharacter.beanstalkArmed() )
		{
			// If the character has a S.O.C.K. or an intragalactic
			// rowboat, they can get to the airship

			if ( KoLCharacter.hasItem( SOCK ) || KoLCharacter.hasItem( ROWBOAT ) )
			{
				isValidAdventure = true;
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

					DEFAULT_SHELL.executeConditionsCommand( "add enchanted bean" );
					DEFAULT_SHELL.executeLine( "adventure * beanbat" );

					if ( !conditions.isEmpty() )
					{
						KoLmafia.updateDisplay( ERROR_STATE, "Unable to complete enchanted bean quest." );
						return;
					}

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
			isValidAdventure = true;
			return;
		}

		else if ( zone.equals( "Spookyraven" ) )
		{
			// It takes RNG luck at the Haunted Pantry to unlock
			// the rest of Spookyraven Manor. Assume it is
			// unlocked. However, we can verify that the zones that
			// require keys are accessible.

			if ( adventureId.equals( "104" ) )
			{
				// Haunted Library
				isValidAdventure = AdventureDatabase.retrieveItem( LIBRARY_KEY );
				return;
			}

			if ( adventureId.equals( "106" ) )
			{
				// Haunted Gallery
				isValidAdventure = AdventureDatabase.retrieveItem( GALLERY_KEY );
				return;
			}

			// It takes a special action to make the upstairs areas
			// available. Assume they are accessible if the player
			// can get into the library
			if ( adventureId.equals( "107" ) || adventureId.equals( "108" ) )
			{
				// Haunted Bathroom & Bedroom
				isValidAdventure = AdventureDatabase.retrieveItem( LIBRARY_KEY );
				return;
			}

			if ( adventureId.equals( "109" ) )
			{
				// Haunted Ballroom
				isValidAdventure = AdventureDatabase.retrieveItem( BALLROOM_KEY );
				return;
			}
		}

		else if ( adventureId.equals( "11" ) )
		{
			isValidAdventure = true;
			return;
		}

		// If a zone validation is sufficient, then validate the
		// zone normally.

		if ( AdventureDatabase.validateZone( zone, adventureId ) )
		{
			isValidAdventure = true;
			return;
		}

		// Attempt to unlock the Degrassi Knoll by visiting Paco.
		// Though we can unlock the guild quest, sometimes people
		// don't want to open up the guild store right now.  So,
		// only keep trying until paco is unlocked.

		if ( adventureId.equals( "10" ) || adventureId.equals( "100" ) )
		{
			StaticEntity.getClient().unlockGuildStore( true );
			if ( KoLmafia.permitsContinue() )
				validate( true );

			return;
		}

		// The beach is unlocked provided the player has the meat car
		// accomplishment and a meatcar in inventory.

		if ( zone.equals( "Beach" ) )
		{
			// If the beach hasn't been unlocked, then visit Paco
			// with your meatcar.

			visitedCouncil = true;

			if ( AdventureDatabase.retrieveItem( MEATCAR ) )
			{
				StaticEntity.getClient().unlockGuildStore( true );
				isValidAdventure = KoLmafia.permitsContinue();
				return;
			}
		}

		// You can unlock pieces of the bat hole by using up to
		// three different sonars.

		if ( zone.equals( "McLarge" ) && !visitedCouncil )
		{
			// Obviate following request by checking accomplishment:
			// questlog.php?which=2
			// "You have learned how to hunt Yetis from the L337
			// Tr4pz0r."

			// See if the trapper will give it to us

			DEFAULT_SHELL.executeLine( "council" );
			RequestThread.postRequest( ZONE_VALIDATOR.constructURLString( "trapper.php" ) );

			validate( true );
			return;
		}

		// Check to see if the Knob is unlocked; all areas are
		// automatically present when this is true.

		if ( visitedCouncil )
		{
			KoLmafia.updateDisplay( ERROR_STATE, "This adventure is not yet unlocked." );
			return;
		}

		DEFAULT_SHELL.executeLine( "council" );
		validate( true );
	}

	/**
	 * Retrieves the string form of the adventure contained within this
	 * encapsulation, which is generally the name of the adventure.
	 *
	 * @return	The string form of the adventure
	 */

	public String toString()
	{
		boolean canAdventureHere = meetsGeneralRequirements();
		StringBuffer stringForm = new StringBuffer();

		if ( !canAdventureHere )
			stringForm.append( "<html><font color=gray>" );

		stringForm.append( zone );
		stringForm.append( ": " );

		stringForm.append( adventureName );
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
		if ( !formSource.equals( "campground.php" ) && !KoLmafia.isRunningBetweenBattleChecks() )
			StaticEntity.getClient().runBetweenBattleChecks( shouldRunFullCheck );

		// Abort before adventure verification in the event that
		// this person is stasis-mining.

		if ( adventureId.indexOf( "101" ) != -1 && KoLCharacter.getFamiliar().isThiefFamiliar() && KoLCharacter.canInteract() )
		{
			KoLmafia.updateDisplay( ABORT_STATE, "Please reconsider your meat farming strategy." );
			return;
		}

		// Validate the adventure before running it.
		// If it's invalid, return and do nothing.

		if ( !meetsGeneralRequirements() )
		{
			KoLmafia.updateDisplay( ERROR_STATE, formSource.equals( "lair6.php" ) ? "Did you forget to equip something?" :
				"Insufficient stats to adventure at " + adventureName + "." );

			return;
		}

		validate( false );
		if ( !isValidAdventure )
		{
			KoLmafia.updateDisplay( ERROR_STATE, "That area is not available." );
			return;
		}

		if ( getFormSource().equals( "shore.php" ) && KoLCharacter.getAvailableMeat() < 500 )
		{
			KoLmafia.updateDisplay( ERROR_STATE, "Insufficient funds for shore vacation." );
			return;
		}

		String action = StaticEntity.getProperty( "battleAction" );

		if ( request instanceof AdventureRequest && !adventureId.equals( "80" ) )
		{
			if ( shouldRunFullCheck && action.indexOf( "dictionary" ) != -1 && (FightRequest.DICTIONARY1.getCount( inventory ) < 1 && FightRequest.DICTIONARY2.getCount( inventory ) < 1) )
			{
				KoLmafia.updateDisplay( ERROR_STATE, "Sorry, you don't have a dictionary." );
				return;
			}
		}

		if ( !KoLmafia.permitsContinue() )
			return;

		// Make sure there are enough adventures to run the request
		// so that you don't spam the server unnecessarily.

		if ( KoLCharacter.getAdventuresLeft() == 0 || KoLCharacter.getAdventuresLeft() < request.getAdventuresUsed() )
		{
			KoLmafia.updateDisplay( ERROR_STATE, "Insufficient adventures to continue." );
			return;
		}

		if ( shouldRunFullCheck && request instanceof AdventureRequest )
		{
			// Check for dictionaries as a battle strategy, if the
			// person is not adventuring at the chasm.

			if ( !adventureId.equals( "80" ) && request.getAdventuresUsed() == 1 && action.indexOf( "dictionary" ) != -1 )
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

			if ( action.startsWith( "attack" ) && areaSummary != null && !areaSummary.willHitSomething() )
			{
				if ( !KoLCharacter.getFamiliar().isCombatFamiliar() )
				{
					KoLmafia.updateDisplay( ERROR_STATE, "You can't hit anything there." );
					return;
				}
			}

			if ( ( action.equals( "skill thrust-smack" ) || action.equals( "skill lunging thrust-smack" ) ) &&
				EquipmentDatabase.isRanged( KoLCharacter.getEquipment( KoLCharacter.WEAPON ).getItemId() ) )
			{
				KoLmafia.updateDisplay( ERROR_STATE, "Thrust smacks are useless with ranged weapons." );
				return;
			}

			if ( ( action.equals( "skill thrust-smack" ) || action.equals( "skill lunging thrust-smack" ) ) &&
				EquipmentDatabase.isStaff( KoLCharacter.getEquipment( KoLCharacter.WEAPON ).getItemId() ) &&
				KoLCharacter.hasSkill( "Spirit of Rigatoni" ) && KoLCharacter.hasSkill( "Eye of the Stoat" ) )
			{
				KoLmafia.updateDisplay( ERROR_STATE, "Thrust smacks are useless with staves and Spirit of Rigatoni." );
				return;
			}
		}

		// If the test is successful, then it is safe to run the
		// request (without spamming the server).

		int previousAdventures = KoLCharacter.getAdventuresLeft();
		RequestThread.postRequest( request );
	}

	public void recordToSession()
	{
		if ( !StaticEntity.getProperty( "lastAdventure" ).equals( adventureName ) )
		{
			StaticEntity.setProperty( "lastAdventure", adventureName );
			if ( shouldRunFullCheck )
				AdventureFrame.updateSelectedAdventure( this );
		}

		if ( !KoLmafia.isAdventuring() )
		{
			KoLmafiaCLI.printBlankLine();
			KoLmafiaCLI.printLine( "[" + getAdventureCount() + "] " + getAdventureName() );
		}

		KoLmafia.getSessionStream().println();
		KoLmafia.getSessionStream().println( "[" + getAdventureCount() + "] " + getAdventureName() );

		StaticEntity.getClient().registerAdventure( this );

		if ( request instanceof CampgroundRequest || request instanceof SewerRequest )
			StaticEntity.getClient().registerEncounter( getAdventureName(), "Noncombat" );
	}

	public static boolean recordToSession( String urlString )
	{
		// In the event that this is an adventure, assume "snarfblat"
		// instead of "adv" in order to determine the location.

		KoLAdventure matchingLocation = AdventureDatabase.getAdventureByURL( urlString );

		if ( matchingLocation != null )
		{
			String locationId = matchingLocation.adventureId;

			// Do some quick adventure validation, which allows you
			// to unlock the bat zone.

			if ( locationId.equals( "32" ) || locationId.equals( "33" ) || locationId.equals( "34" ) )
				if ( !AdventureDatabase.validateZone( "BatHole", locationId ) )
					return true;

			// Make sure to visit the untinkerer before adventuring
			// at Degrassi Knoll.

			if ( locationId.equals( "18" ) && !matchingLocation.isValidAdventure )
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

			matchingLocation.recordToSession();
			return true;
		}

		// Not an internal location.  Perhaps it's something related
		// to another common request?

		String location = null;

		if ( urlString.startsWith( "shore.php" ) && urlString.indexOf( "whichtrip=1" ) != -1 )
			location = "Muscle Vacation";
		else if ( urlString.startsWith( "shore.php" ) && urlString.indexOf( "whichtrip=2" ) != -1 )
			location = "Mysticality Vacation";
		else if ( urlString.startsWith( "shore.php" ) && urlString.indexOf( "whichtrip=3" ) != -1 )
			location = "Moxie Vacation";
		else if ( urlString.startsWith( "guild.php?action=chal" ) )
			location = "Guild Challenge";
		else if ( urlString.startsWith( "dungeon.php" ) )
			location = "Daily Dungeon";
		else if ( urlString.startsWith( "rats.php" ) )
			location = "Typical Tavern Quest";
		else if ( urlString.startsWith( "barrel.php" ) )
			location = "Barrel Full of Barrels";
		else if ( urlString.startsWith( "mining.php" ) )
			location = "Itznotyerzitz Mine (In Disguise)";
		else if ( urlString.startsWith( "arena.php" ) && urlString.indexOf( "action" ) != -1 )
			location = "Cake-Shaped Arena";
		else if ( urlString.startsWith( "lair4.php?action=level1" ) )
			location = "Sorceress Tower: Level 1";
		else if ( urlString.startsWith( "lair4.php?action=level2" ) )
			location = "Sorceress Tower: Level 2";
		else if ( urlString.startsWith( "lair4.php?action=level3" ) )
			location = "Sorceress Tower: Level 3";
		else if ( urlString.startsWith( "lair5.php?action=level1" ) )
			location = "Sorceress Tower: Level 4";
		else if ( urlString.startsWith( "lair5.php?action=level2" ) )
			location = "Sorceress Tower: Level 5";
		else if ( urlString.startsWith( "lair5.php?action=level3" ) )
			location = "Sorceress Tower: Level 6";
		else if ( urlString.startsWith( "lair6.php?place=0" ) )
			location = "Sorceress Tower: Door Puzzles";
		else if ( urlString.startsWith( "lair6.php?place=2" ) )
			location = "Sorceress Tower: Shadow Fight";

		if ( location == null )
			return false;

		if ( urlString.indexOf( "?" ) == -1 )
			return true;

		if ( !KoLmafia.isAdventuring() )
		{
			KoLmafiaCLI.printBlankLine();
			KoLmafiaCLI.printLine( "[" + getAdventureCount() + "] " + location );
		}

		KoLmafia.getSessionStream().println();
		KoLmafia.getSessionStream().println( "[" + getAdventureCount() + "] " + location );
		return true;
	}

	public static int getAdventureCount()
	{
		return StaticEntity.getBooleanProperty( "logReverseOrder" ) ? KoLCharacter.getAdventuresLeft() :
			KoLCharacter.getTotalTurnsUsed() + 1;
	}

	public int compareTo( Object o )
	{
		return ( o == null || !( o instanceof KoLAdventure ) ) ? 1 :
			compareTo( (KoLAdventure) o );
	}

	public int compareTo( KoLAdventure ka )
	{
		// Put things with no evade rating at bottom of list.
		int evade1 = areaSummary == null ? Integer.MAX_VALUE : areaSummary.minEvade();
		int evade2 = ka.areaSummary == null ? Integer.MAX_VALUE : ka.areaSummary.minEvade();

		if ( evade1 == evade2 )
			return adventureName.compareTo( ka.adventureName );

		return evade1 - evade2;
	}
}
