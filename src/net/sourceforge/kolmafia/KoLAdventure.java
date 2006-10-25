/**
 * Copyright (c) 2005, KoLmafia development team
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
 *  [3] Neither the name "KoLmafia development team" nor the names of
 *      its contributors may be used to endorse or promote products
 *      derived from this software without specific prior written
 *      permission.
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
import net.java.dev.spellcast.utilities.LockableListModel;

/**
 * An auxiliary class which stores runnable adventures so that they
 * can be created directly from a database.  Encapsulates the nature
 * of the adventure so that they can be easily listed inside of a
 * <code>ListModel</code>, with the potential to be extended to fit
 * other requests to the Kingdom of Loathing which need to be stored
 * within a database.
 */

public class KoLAdventure implements Runnable, KoLConstants, Comparable
{
	private static final KoLRequest ZONE_VALIDATOR = AdventureDatabase.ZONE_VALIDATOR;

	private static final AdventureResult PERFUME_ITEM = new AdventureResult( 307, 1 );
	private static final AdventureResult PERFUME_EFFECT = new AdventureResult( "Knob Goblin Perfume", 1, false );

	private static final AdventureResult DINGHY = new AdventureResult( 141, 1 );
	private static final AdventureResult PLANS = new AdventureResult( 146, 1 );
	public static final AdventureResult SOCK = new AdventureResult( 609, 1 );
	public static final AdventureResult ROWBOAT = new AdventureResult( 653, 1 );
	public static final AdventureResult BEAN = new AdventureResult( 186, 1 );
	private static final AdventureResult TRANSFUNCTIONER = new AdventureResult( 458, 1 );
	private static final AdventureResult LIBRARY_KEY = new AdventureResult( 1764, 1 );
	private static final AdventureResult GALLERY_KEY = new AdventureResult( 1765, 1 );
	private static final AdventureResult BALLROOM_KEY = new AdventureResult( 1766, 1 );

	private static final AdventureResult ASTRAL = new AdventureResult( "Half-Astral", 0 );
	public static final AdventureResult BEATEN_UP = new AdventureResult( "Beaten Up", 1, true );

	private boolean isValidAdventure = false;
	private int baseRequirement, buffedRequirement;
	private String zone, parentZone, adventureID, formSource, adventureName;
	private KoLRequest request;
	private AreaCombatData areaSummary;

	private boolean shouldRunCheck;
	private boolean shouldRunFullCheck;

	/**
	 * Constructs a new <code>KoLAdventure</code> with the given
	 * specifications.
	 *
	 * @param	client	Theto which the results of the adventure are reported
	 * @param	formSource	The form associated with the given adventure
	 * @param	adventureID	The identifier for this adventure, relative to its form
	 * @param	adventureName	The string form, or name of this adventure
	 */

	public KoLAdventure( String zone, String baseRequirement, String buffedRequirement, String formSource, String adventureID, String adventureName )
	{
		this.zone = zone;
		this.parentZone = (String) AdventureDatabase.PARENT_ZONES.get( zone );
		this.baseRequirement = StaticEntity.parseInt( baseRequirement );
		this.buffedRequirement = StaticEntity.parseInt( buffedRequirement );
		this.formSource = formSource;
		this.adventureID = adventureID;
		this.adventureName = adventureName;

		if ( formSource.equals( "sewer.php" ) )
		{
			shouldRunCheck = true;
			shouldRunFullCheck = false;
			this.request = new SewerRequest( false );
		}
		else if ( formSource.equals( "luckysewer.php" ) )
		{
			shouldRunCheck = true;
			shouldRunFullCheck = false;
			this.request = new SewerRequest( true );
		}
		else if ( formSource.equals( "campground.php" ) )
		{
			shouldRunCheck = false;
			shouldRunFullCheck = false;
			this.request = new CampgroundRequest( adventureID );
		}
		else if ( formSource.equals( "clan_gym.php" ) )
		{
			shouldRunCheck = false;
			shouldRunFullCheck = false;
			this.request = new ClanGymRequest( StaticEntity.parseInt( adventureID ) );
		}
		else
		{
			shouldRunCheck = true;
			shouldRunFullCheck = formSource.indexOf( "lair6.php" ) == -1 && formSource.indexOf( "shore.php" ) == -1;
			this.request = new AdventureRequest( adventureName, formSource, adventureID );
		}

		this.areaSummary = AdventureDatabase.getAreaCombatData( adventureName );
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
	 * Returns the adventure ID for this adventure.
	 * @return	The adventure ID for this adventure
	 */

	public String getAdventureID()
	{	return adventureID;
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
		if ( isValidAdventure )
			return;

		// Fighting the Goblin King requires effects
		if ( formSource.equals( "knob.php" ) )
		{
			int outfitID = EquipmentDatabase.getOutfitID( this );

			if ( !EquipmentDatabase.isWearingOutfit( outfitID ) )
			{
				if ( !EquipmentDatabase.retrieveOutfit( outfitID ) )
					return;

				if ( !activeEffects.contains( PERFUME_EFFECT ) )
				{
					if ( !AdventureDatabase.retrieveItem( PERFUME_ITEM ) )
						return;

					(new ConsumeItemRequest( PERFUME_ITEM )).run();
				}

				(new EquipmentRequest( EquipmentDatabase.getOutfit( outfitID ) )).run();
			}
		}

		if ( formSource.indexOf( "adventure.php" ) == -1 )
		{
			isValidAdventure = true;
			return;
		}

		// Disguise zones require outfits
		if ( adventureName.indexOf( "In Disguise" ) != -1 || adventureName.indexOf( "Cloaca Uniform" ) != -1 || adventureName.indexOf( "Dyspepsi Uniform" ) != -1 )
		{
			int outfitID = EquipmentDatabase.getOutfitID( this );

			if ( !EquipmentDatabase.isWearingOutfit( outfitID ) )
			{
				if ( !EquipmentDatabase.retrieveOutfit( outfitID ) )
					return;

				(new EquipmentRequest( EquipmentDatabase.getOutfit( outfitID ) )).run();
			}

			// If it's the pirate quest in disguise, make sure
			// you visit the council beforehand.

			if ( adventureName.indexOf( "Pirate" ) != -1 )
				DEFAULT_SHELL.executeLine( "council" );
		}

		// If we're trying to take a trip, make sure it's the right one
		if ( adventureID.equals( "96" ) || adventureID.equals( "97" ) || adventureID.equals( "98" ) )
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
				if ( adventureID.equals( "96" ) )
					choice = "1";
				else if ( adventureID.equals( "98" ) )
					choice = "2";
				else
					choice = "3";

				// Choose the trip
				StaticEntity.setProperty( "choiceAdventure71", choice );

				String name = getAdventureName();
				StaticEntity.setProperty( "chosenTrip", name );

				// Arm the adventure by running it once
				// to get the "Journey to the Center of
				// your Mind" choice.
				isValidAdventure = true;
				StaticEntity.getClient().makeRequest( this, 1 );
				return;
			}

			String chosenTrip = StaticEntity.getProperty( "chosenTrip" );

			// If we've already selected a trip, we can't switch
			if ( !chosenTrip.equals( getAdventureName() ) )
			{
				KoLmafia.updateDisplay( ERROR_STATE, "You're already taking a different trip." );
				isValidAdventure = false;
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

		else if ( adventureID.equals( "82" ) )
		{
			isValidAdventure = KoLCharacter.hasItem( SOCK ) || AdventureDatabase.retrieveItem( ROWBOAT );
			return;
		}

		// The Hole in the Sky is unlocked provided the player has an
		// intragalactic rowboat in their inventory.

		else if ( adventureID.equals( "83" ) )
		{
			isValidAdventure = AdventureDatabase.retrieveItem( ROWBOAT );
			return;
		}

		// The beanstalk is unlocked when the player
		// has planted a beanstalk -- but, the zone
		// needs to be armed first.

		else if ( adventureID.equals( "81" ) && !KoLCharacter.beanstalkArmed() )
		{
			// If the character has a S.O.C.K. or an intragalactic
			// rowboat, they can get to the airship

			if ( KoLCharacter.hasItem( SOCK ) || AdventureDatabase.retrieveItem( ROWBOAT ) )
			{
				isValidAdventure = true;
				return;
			}

			// Obviate following request by checking accomplishment:
			// questlog.php?which=3
			// "You have planted a Beanstalk in the Nearby Plains."

			ZONE_VALIDATOR.constructURLString( "plains.php" );
			ZONE_VALIDATOR.run();

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
			ZONE_VALIDATOR.run();

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

			if ( adventureID.equals( "104" ) )
			{
				// Haunted Library
				isValidAdventure = AdventureDatabase.retrieveItem( LIBRARY_KEY );
				return;
			}

			if ( adventureID.equals( "106" ) )
			{
				// Haunted Gallery
				isValidAdventure = AdventureDatabase.retrieveItem( GALLERY_KEY );
				return;
			}

			// It takes a special action to make the upstairs areas
			// available. Assume they are accessible if the player
			// can get into the library
			if ( adventureID.equals( "107" ) || adventureID.equals( "108" ) )
			{
				// Haunted Bathroom & Bedroom
				isValidAdventure = AdventureDatabase.retrieveItem( LIBRARY_KEY );
				return;
			}

			if ( adventureID.equals( "109" ) )
			{
				// Haunted Ballroom
				isValidAdventure = AdventureDatabase.retrieveItem( BALLROOM_KEY );
				return;
			}
		}

		else if ( adventureID.equals( "11" ) )
		{
			isValidAdventure = true;
			return;
		}

		// If a zone validation is sufficient, then validate the
		// zone normally.

		if ( AdventureDatabase.validateZone( zone, adventureID ) )
		{
			isValidAdventure = true;
			return;
		}

		// If the person has a continuum transfunctioner, then find
		// some way of equipping it.  If they do not have one, then
		// acquire one then try to equip it.  If the person has a two
		// handed weapon, then report an error.

		if ( adventureID.equals( "73" ) )
		{
			if ( !KoLCharacter.hasItem( TRANSFUNCTIONER ) )
			{
				ZONE_VALIDATOR.constructURLString( "town_wrong.php?place=crackpot" ).run();
				ZONE_VALIDATOR.constructURLString( "town_wrong.php?action=crackyes1" ).run();
				ZONE_VALIDATOR.constructURLString( "town_wrong.php?action=crackyes2" ).run();
				ZONE_VALIDATOR.constructURLString( "town_wrong.php?action=crackyes3" ).run();
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

		// Attempt to unlock the Degrassi Knoll by visiting Paco.
		// Though we can unlock the guild quest, sometimes people
		// don't want to open up the guild store right now.  So,
		// only keep trying until paco is unlocked.

		if ( adventureID.equals( "10" ) || adventureID.equals( "100" ) )
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

			if ( AdventureDatabase.retrieveItem( "bitchin' meatcar" ) )
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
			ZONE_VALIDATOR.constructURLString( "trapper.php" ).run();

			validate( true );
			return;
		}

		// Check to see if the Knob is unlocked; all areas are
		// automatically present when this is true.

		if ( visitedCouncil )
		{
			KoLmafia.updateDisplay( ABORT_STATE, "This adventure is not yet unlocked." );
			return;
		}

		DEFAULT_SHELL.executeLine( "council" );
		validate( true );
	}

	private void postValidate()
	{
		// If we're trying to take a trip, make sure we're still
		// half-astral
		if ( adventureID.equals( "96" ) || adventureID.equals( "97" ) || adventureID.equals( "98" ) )
		{
			if ( ASTRAL.getCount( ( activeEffects ) ) == 0 )
				isValidAdventure = false;
			return;
		}
	}

	/**
	 * Retrieves the string form of the adventure contained within this
	 * encapsulation, which is generally the name of the adventure.
	 *
	 * @return	The string form of the adventure
	 */

	public String toString()
	{
		StringBuffer stringForm = new StringBuffer();
		boolean hasHTML = false;

		if ( !meetsGeneralRequirements() )
		{
			hasHTML = true;
			stringForm.append( "<html><font color=gray>" );
		}

		stringForm.append( zone );
		stringForm.append( ": " );

		stringForm.append( adventureName );
		if ( hasHTML )
			stringForm.append( "</font></html>" );

		return stringForm.toString();
	}

	/**
	 * Executes the appropriate <code>KoLRequest</code> for the adventure
	 * encapsulated by this <code>KoLAdventure</code>.
	 */

	public void run()
	{
		// Abort before adventure verification in the event that
		// this person is stasis-mining.

		if ( adventureID.indexOf( "101" ) != -1 && KoLCharacter.getFamiliar().isThiefFamiliar() && KoLCharacter.canInteract() )
		{
			KoLmafia.updateDisplay( ABORT_STATE, "Please reconsider your meat farming strategy." );
			return;
		}

		// Validate the adventure before running it.
		// If it's invalid, return and do nothing.

		if ( StaticEntity.getBooleanProperty( "areaValidation" ) )
		{
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
		}

		if ( getFormSource().equals( "shore.php" ) && KoLCharacter.getAvailableMeat() < 500 )
		{
			KoLmafia.updateDisplay( ERROR_STATE, "Insufficient funds for shore vacation." );
			return;
		}

		String action = StaticEntity.getProperty( "battleAction" );

		if ( action.indexOf( "dictionary" ) != -1 && ( FightRequest.DICTIONARY1.getCount( inventory ) < 1 && FightRequest.DICTIONARY2.getCount( inventory ) < 1) )
		{
			KoLmafia.updateDisplay( ERROR_STATE, "Sorry, you don't have a dictionary." );
			return;
		}

		if ( shouldRunCheck && !KoLmafia.isRunningBetweenBattleChecks() )
			StaticEntity.getClient().runBetweenBattleChecks( shouldRunFullCheck );

		if ( !KoLmafia.permitsContinue() )
			return;

		// Make sure there are enough adventures to run the request
		// so that you don't spam the server unnecessarily.

		if ( KoLCharacter.getAdventuresLeft() == 0 || KoLCharacter.getAdventuresLeft() < request.getAdventuresUsed() )
		{
			KoLmafia.updateDisplay( ERROR_STATE, "Insufficient adventures to continue." );
			return;
		}

		boolean allowStasis = StaticEntity.getBooleanProperty( "allowStasisTactics" );

		// Check for dictionaries as a battle strategy, if the
		// person is not adventuring at the chasm.

		if ( !adventureID.equals( "80" ) && request instanceof AdventureRequest && request.getAdventuresUsed() == 1 && action.indexOf( "dictionary" ) != -1 )
		{
			// Only allow damage-dealing familiars when using
			// stasis techniques.

			if ( !allowStasis || !KoLCharacter.getFamiliar().isCombatFamiliar() )
			{
				KoLmafia.updateDisplay( ERROR_STATE, "A dictionary would be useless there." );
				return;
			}
		}

		// If the person doesn't stand a chance of surviving,
		// automatically quit and tell them so.

		if ( action.startsWith( "attack" ) && areaSummary != null && !areaSummary.willHitSomething() )
		{
			if ( !allowStasis || !KoLCharacter.getFamiliar().isCombatFamiliar() )
			{
				KoLmafia.updateDisplay( ABORT_STATE, "You can't hit anything there." );
				return;
			}
		}

		if ( ( action.equals( "skill thrust-smack" ) || action.equals( "skill lunging thrust-smack" ) ) &&
			EquipmentDatabase.isRanged( KoLCharacter.getEquipment( KoLCharacter.WEAPON ).getName() ) )
		{
			KoLmafia.updateDisplay( ABORT_STATE, "Thrust smacks should use non-ranged weapons." );
			return;
		}

		// If the test is successful, then it is safe to run the
		// request (without spamming the server).

		int previousAdventures = KoLCharacter.getAdventuresLeft();
		request.run();

		if ( shouldRunCheck && !KoLmafia.isRunningBetweenBattleChecks() )
		{
			// If you need to run a between battle script after this request,
			// this is where you would do it.  Note that fights should not have
			// scripts invoked after them unless the fight is concluded.

			StaticEntity.getClient().runBetweenBattleChecks( shouldRunFullCheck );
		}

		if ( previousAdventures == KoLCharacter.getAdventuresLeft() )
		{
			// Well, that was an interesting predicament.  If it
			// had a choice adventure that didn't use adventures,
			// then you re-run the request.

			while ( request.redirectLocation != null && request.redirectLocation.equals( "choice.php" ) )
				request.run();
		}

		postValidate();
	}

	public void recordToSession()
	{
		StaticEntity.setProperty( "lastAdventure", adventureName );

		if ( StaticEntity.getBooleanProperty( "trackLocationChanges" ) && shouldRunFullCheck )
			AdventureFrame.updateSelectedAdventure( this );

		KoLmafia.getSessionStream().println( "[" + getAdventureCount() + "] " + getAdventureName() );
		StaticEntity.getClient().registerAdventure( this );

		if ( request instanceof CampgroundRequest || request instanceof SewerRequest )
			StaticEntity.getClient().registerEncounter( getAdventureName(), "Noncombat" );
	}

	public static boolean recordToSession( String urlString )
	{
		String location = null;

		if ( urlString.indexOf( "dungeon.php" ) != -1 )
			location = "Daily Dungeon";
		else if ( urlString.indexOf( "rats.php" ) != -1 )
			location = "Typical Tavern Quest";
		else if ( urlString.indexOf( "barrels.php" ) != -1 )
			location = "Barrel Full of Barrels";
		else if ( urlString.indexOf( "arena.php" ) != -1 && urlString.indexOf( "action" ) != -1 )
			location = "Cake-Shaped Arena";
		else if ( urlString.indexOf( "lair4.php" ) != -1 && urlString.indexOf( "level1" ) != -1 )
			location = "Sorceress Tower: Level 1";
		else if ( urlString.indexOf( "lair4.php" ) != -1 && urlString.indexOf( "level2" ) != -1 )
			location = "Sorceress Tower: Level 2";
		else if ( urlString.indexOf( "lair4.php" ) != -1 && urlString.indexOf( "level3" ) != -1 )
			location = "Sorceress Tower: Level 3";
		else if ( urlString.indexOf( "lair5.php" ) != -1 && urlString.indexOf( "level1" ) != -1 )
			location = "Sorceress Tower: Level 4";
		else if ( urlString.indexOf( "lair5.php" ) != -1 && urlString.indexOf( "level2" ) != -1 )
			location = "Sorceress Tower: Level 5";
		else if ( urlString.indexOf( "lair5.php" ) != -1 && urlString.indexOf( "level3" ) != -1 )
			location = "Sorceress Tower: Level 6";
		else if ( urlString.indexOf( "lair6.php" ) != -1 && urlString.indexOf( "place=0" ) != -1 )
			location = "Sorceress Tower: Door Puzzles";
		else if ( urlString.indexOf( "lair6.php" ) != -1 && urlString.indexOf( "place=2" ) != -1 )
			location = "Sorceress Tower: Shadow Fight";

		if ( location == null )
			return false;

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
