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
	private static final AdventureResult SONAR = new AdventureResult( 563, 1 );
	private static final AdventureResult DINGHY = new AdventureResult( 141, 1 );
	private static final AdventureResult SOCK = new AdventureResult( 609, 1 );
	private static final AdventureResult ROWBOAT = new AdventureResult( 653, 1 );
	private static final AdventureResult BEAN = new AdventureResult( 186, 1 );
	private static final AdventureResult ASTRAL = new AdventureResult( "Half-Astral", 0 );
	public static final AdventureResult BEATEN_UP = new AdventureResult( "Beaten Up", 1, true );

	private KoLmafia client;
	private boolean isValidAdventure = false;
	private int baseRequirement, buffedRequirement;
	private String zone, adventureID, formSource, adventureName;
	private KoLRequest request;
	private AreaCombatData areaSummary;
	private boolean shouldRunBetweenBattleChecks;

	/**
	 * Constructs a new <code>KoLAdventure</code> with the given
	 * specifications.
	 *
	 * @param	client	The client to which the results of the adventure are reported
	 * @param	formSource	The form associated with the given adventure
	 * @param	adventureID	The identifier for this adventure, relative to its form
	 * @param	adventureName	The string form, or name of this adventure
	 */

	public KoLAdventure( KoLmafia client, String zone, String baseRequirement, String buffedRequirement, String formSource, String adventureID, String adventureName )
	{
		this.client = client;
		this.zone = zone;
		this.baseRequirement = StaticEntity.parseInt( baseRequirement );
		this.buffedRequirement = StaticEntity.parseInt( buffedRequirement );
		this.formSource = formSource;
		this.adventureID = adventureID;
		this.adventureName = adventureName;

		if ( formSource.equals( "sewer.php" ) )
		{
			this.request = new SewerRequest( client, false );
			this.shouldRunBetweenBattleChecks = false;
		}
		else if ( formSource.equals( "luckysewer.php" ) )
		{
			this.request = new SewerRequest( client, true );
			this.shouldRunBetweenBattleChecks = false;
		}
		else if ( formSource.equals( "campground.php" ) )
		{
			this.request = new CampgroundRequest( client, adventureID );
			this.shouldRunBetweenBattleChecks = false;
		}
		else if ( formSource.equals( "clan_gym.php" ) )
		{
			this.request = new ClanGymRequest( client, StaticEntity.parseInt( adventureID ) );
			this.shouldRunBetweenBattleChecks = false;
		}
		else
		{
			this.request = new AdventureRequest( client, adventureName, formSource, adventureID );
			this.shouldRunBetweenBattleChecks = !formSource.equals( "shore.php" ) && !zone.equals( "Casino" );
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

	/**
	 * Returns the request associated with this adventure.
	 * @return	The request for this adventure
	 */

	public KoLRequest getRequest()
	{	return request;
	}

	private boolean meetsGeneralRequirements()
	{
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

		if ( adventureName.indexOf( "In Disguise" ) != -1 || adventureName.indexOf( "Cloaca Uniform" ) != -1 || adventureName.indexOf( "Dyspepsi Uniform" ) != -1 )
		{
			int outfitID = EquipmentDatabase.getOutfitID( this );

			if ( !EquipmentDatabase.isWearingOutfit( outfitID ) )
			{
				EquipmentDatabase.retrieveOutfit( outfitID );
				if ( !KoLmafia.permitsContinue() )
					return;

				(new EquipmentRequest( client, EquipmentDatabase.getOutfit( outfitID ) )).run();
			}
		}

		// If we're trying to take a trip, make sure it's the right one
		if ( adventureID.equals( "96" ) || adventureID.equals( "97" ) || adventureID.equals( "98" ) )
		{
			// You must be Half-Astral to go on a trip
			int astral = ASTRAL.getCount( ( KoLCharacter.getEffects() ) );
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
				client.makeRequest( this, 1 );
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

		KoLRequest request = null;

		// The casino is unlocked provided the player
		// has a casino pass in their inventory.

		if ( zone.equals( "Casino" ) )
		{
			AdventureDatabase.retrieveItem( "casino pass" );
			isValidAdventure = KoLmafia.permitsContinue();
			return;
		}

		// The island is unlocked provided the player
		// has a dingy dinghy in their inventory.

		else if ( zone.equals( "Island" ) )
		{
			if ( KoLCharacter.hasItem( DINGHY, false ) )
				AdventureDatabase.retrieveItem( DINGHY );
			else
			{
				AdventureDatabase.retrieveItem( "dingy planks" );
				DEFAULT_SHELL.executeLine( "use dinghy plans" );
			}

			isValidAdventure = KoLmafia.permitsContinue();
			return;
		}

		// The Castle in the Clouds in the Sky is unlocked provided the
		// character has either a S.O.C.K. or an intragalactic rowboat

		else if ( adventureID.equals( "82" ) )
		{
			if ( KoLCharacter.hasItem( ROWBOAT, true ) )
				AdventureDatabase.retrieveItem( ROWBOAT );
			else
				AdventureDatabase.retrieveItem( SOCK );

			isValidAdventure = KoLmafia.permitsContinue();
			return;
		}

		// The Hole in the Sky is unlocked provided the player has an
		// intragalactic rowboat in their inventory.

		else if ( adventureID.equals( "83" ) )
		{
			AdventureDatabase.retrieveItem( ROWBOAT );
			isValidAdventure = KoLmafia.permitsContinue();
			return;
		}

		// The beanstalk is unlocked when the player
		// has planted a beanstalk -- but, the zone
		// needs to be armed first.

		else if ( adventureID.equals( "81" ) && !KoLCharacter.beanstalkArmed() )
		{
			// If the character has a S.O.C.K. or an intragalactic
			// rowboat, they can get to the airship

			if ( KoLCharacter.hasItem( SOCK, false ) || KoLCharacter.hasItem( ROWBOAT, false ) )
			{
				isValidAdventure = true;
				return;
			}

			// Obviate following request by checking accomplishment:
			// questlog.php?which=3
			// "You have planted a Beanstalk in the Nearby Plains."

			request = new KoLRequest( client, "plains.php" );
			request.run();

			if ( request.responseText.indexOf( "beanstalk.php" ) == -1 )
			{
				// If not, check to see if the player has an enchanted
				// bean which can be used.  If they don't, then try to
				// find one through adventuring.

				if ( !KoLCharacter.hasItem( BEAN, false ) )
				{
					ArrayList temporary = new ArrayList();
					temporary.addAll( client.getConditions() );

					DEFAULT_SHELL.executeConditionsCommand( "clear" );
					DEFAULT_SHELL.executeConditionsCommand( "add enchanted bean" );
					DEFAULT_SHELL.executeLine( "adventure * beanbat" );

					if ( !client.getConditions().isEmpty() )
					{
						KoLmafia.updateDisplay( ERROR_STATE, "Unable to complete enchanted bean quest." );
						client.getConditions().clear();
						client.getConditions().addAll( temporary );
						return;
					}

					client.getConditions().clear();
					client.getConditions().addAll( temporary );
				}

				// Now that you've retrieved the bean, ensure that
				// it is in your inventory, and then use it.  Take
				// advantage of item consumption automatically doing
				// what's needed in grabbing the item.

				DEFAULT_SHELL.executeLine( "council" );
				DEFAULT_SHELL.executeLine( "use enchanted bean" );
			}

			request = new KoLRequest( client, "beanstalk.php" );
			request.run();

			KoLCharacter.armBeanstalk();
			return;
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

		// Attempt to unlock the Degrassi Knoll by visiting Paco.
		// Though we can unlock the guild quest, sometimes people
		// don't want to open up the guild store right now.  So,
		// just keep trying until paco is unlocked.

		if ( adventureID.equals( "10" ) )
		{
			client.unlockGuildStore( true );
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
			AdventureDatabase.retrieveItem( "bitchin' meatcar" );

			if ( KoLmafia.permitsContinue() )
			{
				client.unlockGuildStore( true );
				isValidAdventure = KoLmafia.permitsContinue();
				return;
			}
		}

		// You can unlock pieces of the bat hole by using up to
		// three different sonars.

		if ( zone.equals( "BatHole" ) && !visitedCouncil )
		{
			int sonarCount = SONAR.getCount( KoLCharacter.getInventory() );
			if ( sonarCount == 0 )
				return;

			DEFAULT_SHELL.executeLine( "use " + Math.min( 3, sonarCount ) + " sonar-in-a-biscuit" );
			validate( true );
			return;
		}

		if ( zone.equals( "McLarge" ) && !visitedCouncil )
		{
			// Obviate following request by checking accomplishment:
			// questlog.php?which=2
			// "You have learned how to hunt Yetis from the L337
			// Tr4pz0r."

			// See if the trapper will give it to us

			DEFAULT_SHELL.executeLine( "council" );
			request = new KoLRequest( client, "trapper.php" );
			request.run();

			validate( true );
			return;
		}

		// Check to see if the Knob is unlocked; all areas are
		// automatically present when this is true.

		if ( visitedCouncil )
		{
			KoLmafia.updateDisplay( "This adventure is not yet unlocked." );
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
			if ( ASTRAL.getCount( ( KoLCharacter.getEffects() ) ) == 0 )
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
		// Validate the adventure before running it.
		// If it's invalid, return and do nothing.

		if ( StaticEntity.getBooleanProperty( "areaValidation" ) )
		{
			if ( !meetsGeneralRequirements() )
			{
				KoLmafia.updateDisplay( ERROR_STATE, "Insufficient stats for this location." );
				return;
			}

			validate( false );
			if ( !isValidAdventure )
				return;
		}

		if ( getFormSource().equals( "shore.php" ) && KoLCharacter.getAvailableMeat() < 500 )
		{
			KoLmafia.updateDisplay( ERROR_STATE, "Insufficient funds for shore vacation." );
			return;
		}

		KoLCharacter.setNextAdventure( this );
		StaticEntity.setProperty( "nextAdventure", adventureName );
		FightRequest.setAutoRecovery( StaticEntity.getProperty( "battleAction" ) );

		String action = CombatSettings.getShortCombatOptionName( StaticEntity.getProperty( "battleAction" ) );
		int haltTolerance = (int)( StaticEntity.getFloatProperty( "battleStop" ) * (float) KoLCharacter.getMaximumHP() );

		if ( ( action.equals( "item536" ) && FightRequest.DICTIONARY1.getCount( KoLCharacter.getInventory() ) < 1 ) ||
			 ( action.equals( "item1316" ) && FightRequest.DICTIONARY2.getCount( KoLCharacter.getInventory() ) < 1 ) )
		{
			KoLmafia.updateDisplay( ERROR_STATE, "Sorry, you don't have a dictionary." );
			KoLCharacter.setNextAdventure( null );
			return;
		}

		// If auto-recovery failed, return from the run attempt.
		// This prevents other messages from overriding the actual
		// error message.

		client.runBetweenBattleChecks( !shouldRunBetweenBattleChecks );

		if ( !KoLmafia.permitsContinue() )
			return;

		if ( haltTolerance >= 0 && KoLCharacter.getCurrentHP() <= haltTolerance && !(request instanceof CampgroundRequest) )
		{
			KoLmafia.updateDisplay( ABORT_STATE, "Insufficient health to continue (auto-abort triggered)." );
			KoLCharacter.setNextAdventure( null );
			return;
		}

		// Make sure there are enough adventures to run the request
		// so that you don't spam the server unnecessarily.

		if ( KoLCharacter.getAdventuresLeft() == 0 || KoLCharacter.getAdventuresLeft() < request.getAdventuresUsed() )
		{
			KoLmafia.updateDisplay( ERROR_STATE, "Insufficient adventures to continue." );
			KoLCharacter.setNextAdventure( null );
			return;
		}

		boolean allowStasis = StaticEntity.getBooleanProperty( "allowStasisTactics" );

		// Check for dictionaries as a battle strategy, if the
		// person is not adventuring at the chasm.

		if ( !adventureID.equals( "80" ) && request instanceof AdventureRequest &&
			request.getAdventuresUsed() == 1 && (action.equals( "item536" ) || action.equals( "item1316" )) &&
			KoLCharacter.getFamiliar().getID() != 16 && KoLCharacter.getFamiliar().getID() != 17 && KoLCharacter.getFamiliar().getID() != 48 )
		{
			// Only allow damage-dealing familiars when using
			// stasis techniques.

			if ( !allowStasis || !KoLCharacter.getFamiliar().isCombatFamiliar() )
			{
				KoLmafia.updateDisplay( ERROR_STATE, "A dictionary would be useless there." );
				KoLCharacter.setNextAdventure( null );
				return;
			}
		}

		// If the person doesn't stand a chance of surviving,
		// automatically quit and tell them so.

		if ( action.equals( "attack" ) && areaSummary != null && !areaSummary.willHitSomething() )
		{
			if ( !allowStasis || !KoLCharacter.getFamiliar().isCombatFamiliar() )
			{
				KoLmafia.updateDisplay( ABORT_STATE, "You can't hit anything there." );
				return;
			}
		}

		if ( action.equals( "skill thrust-smack" ) || action.equals( "skill lunging thrust-smack" ) &&
			EquipmentDatabase.isRanged( KoLCharacter.getEquipment( KoLCharacter.WEAPON ) ) )
		{
			KoLmafia.updateDisplay( ABORT_STATE, "Thrust smacks should use non-ranged weapons." );
			return;
		}

		// If the test is successful, then it is safe to run the
		// request (without spamming the server).

		int previousAdventures = KoLCharacter.getAdventuresLeft();
		request.run();

		if ( previousAdventures == KoLCharacter.getAdventuresLeft() )
		{
			// Well, that was an interesting predicament.  If it
			// had a choice adventure that didn't use adventures,
			// then you re-run the request.

			while ( request.redirectLocation != null && request.redirectLocation.equals( "choice.php" ) )
				request.run();
		}

		client.runBetweenBattleChecks( !shouldRunBetweenBattleChecks );
		postValidate();
	}

	public void recordToSession()
	{
		StaticEntity.setProperty( "lastAdventure", adventureName );

		if ( StaticEntity.getBooleanProperty( "trackLocationChanges" ) )
		{
			LockableListModel adventureList = AdventureDatabase.getAsLockableListModel();
			adventureList.setSelectedItem( this );
		}

		KoLmafia.getSessionStream().println();
		KoLmafia.getSessionStream().println( "[" + (KoLCharacter.getTotalTurnsUsed() + 1) + "] " + getAdventureName() );
		client.registerAdventure( this );

		if ( request instanceof CampgroundRequest || request instanceof SewerRequest )
			client.registerEncounter( getAdventureName() );
	}

	public static boolean recordToSession( String urlString )
	{
		if ( urlString.indexOf( "dungeon.php" ) != -1 )
		{
			KoLmafia.getSessionStream().println( "[" + (KoLCharacter.getTotalTurnsUsed() + 1) + "] Daily Dungeon" );
			return true;
		}
		else if ( urlString.indexOf( "sewer.php" ) != -1 )
		{
			KoLmafia.getSessionStream().println( "[" + (KoLCharacter.getTotalTurnsUsed() + 1) + "] Market Sewer" );
			return true;
		}
		else if ( urlString.indexOf( "rats.php" ) != -1 )
		{
			KoLmafia.getSessionStream().println( "[" + (KoLCharacter.getTotalTurnsUsed() + 1) + "] Typical Tavern Quest" );
			return true;
		}
		else if ( urlString.indexOf( "barrels.php" ) != -1 )
		{
			KoLmafia.getSessionStream().println( "[" + (KoLCharacter.getTotalTurnsUsed() + 1) + "] Barrel Full of Barrels" );
			return true;
		}
		else if ( urlString.indexOf( "arena.php" ) != -1 && urlString.indexOf( "action" ) != -1 )
		{
			KoLmafia.getSessionStream().println( "[" + (KoLCharacter.getTotalTurnsUsed() + 1) + "] Cake-Shaped Arena" );
			return true;
		}
		else if ( urlString.indexOf( "lair4.php" ) != -1 && urlString.indexOf( "level1" ) != -1 )
		{
			KoLmafia.getSessionStream().println( "[" + (KoLCharacter.getTotalTurnsUsed() + 1) + "] Sorceress Tower: Level 1" );
			return true;
		}
		else if ( urlString.indexOf( "lair4.php" ) != -1 && urlString.indexOf( "level2" ) != -1 )
		{
			KoLmafia.getSessionStream().println( "[" + (KoLCharacter.getTotalTurnsUsed() + 1) + "] Sorceress Tower: Level 2" );
			return true;
		}
		else if ( urlString.indexOf( "lair4.php" ) != -1 && urlString.indexOf( "level3" ) != -1 )
		{
			KoLmafia.getSessionStream().println( "[" + (KoLCharacter.getTotalTurnsUsed() + 1) + "] Sorceress Tower: Level 3" );
			return true;
		}
		else if ( urlString.indexOf( "lair5.php" ) != -1 && urlString.indexOf( "level1" ) != -1 )
		{
			KoLmafia.getSessionStream().println( "[" + (KoLCharacter.getTotalTurnsUsed() + 1) + "] Sorceress Tower: Level 4" );
			return true;
		}
		else if ( urlString.indexOf( "lair5.php" ) != -1 && urlString.indexOf( "level2" ) != -1 )
		{
			KoLmafia.getSessionStream().println( "[" + (KoLCharacter.getTotalTurnsUsed() + 1) + "] Sorceress Tower: Level 5" );
			return true;
		}
		else if ( urlString.indexOf( "lair5.php" ) != -1 && urlString.indexOf( "level3" ) != -1 )
		{
			KoLmafia.getSessionStream().println( "[" + (KoLCharacter.getTotalTurnsUsed() + 1) + "] Sorceress Tower: Level 6" );
			return true;
		}

		return false;
	}

	public int compareTo( Object o )
	{
		return ( o == null || !( o instanceof KoLAdventure ) ) ? 1 :
			compareTo( (KoLAdventure) o );
	}

	public int compareTo( KoLAdventure ka )
	{	return areaSummary.minEvade() - ka.areaSummary.minEvade();
	}
}
