/**
 * Copyright (c) 2005-2006, KoLmafia development team
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

import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UseSkillRequest extends KoLRequest implements Comparable
{
	private static final TreeMap ALL_SKILLS = new TreeMap();
	private static final Pattern SKILLID_PATTERN = Pattern.compile( "whichskill=(\\d+)" );
	private static final Pattern COUNT_PATTERN = Pattern.compile( "(bufftimes|quantity)=([\\d,]*)" );

	public static String [][] BREAKFAST_SKILLS =
	{
		{ "Summon Snowcone", "1" },
		{ "Summon Hilarious Objects", "1" },
		{ "Advanced Saucecrafting", "3" },
		{ "Pastamastery", "3" },
		{ "Advanced Cocktailcrafting", "3" }
	};

	private static final int OTTER_TONGUE = 1007;
	private static final int WALRUS_TONGUE = 1010;
	public static String lastUpdate = "";

	private int skillId;
	private String skillName;
	private String target;
	private int buffCount;
	private String countFieldId;

	private static final AdventureResult ACCORDION = new AdventureResult( 11, 1 );
	public static final AdventureResult ROCKNROLL_LEGEND = new AdventureResult( 50, 1 );
	public static final AdventureResult WIZARD_HAT = new AdventureResult( 1653, 1 );

	private static final AdventureResult ROLL = new AdventureResult( 47, 1 );
	private static final AdventureResult BIG_ROCK = new AdventureResult( 30, 1 );
	private static final AdventureResult HEART = new AdventureResult( 48, 1 );

	// Clover weapons
	private static final AdventureResult BJORNS_HAMMER = new AdventureResult( 32, 1 );
	private static final AdventureResult TURTLESLINGER = new AdventureResult( 60, 1 );
	private static final AdventureResult PASTA_OF_PERIL = new AdventureResult( 68, 1 );
	private static final AdventureResult FIVE_ALARM_SAUCEPAN = new AdventureResult( 57, 1 );
	private static final AdventureResult DISCO_BANJO = new AdventureResult( 54, 1 );

	private static final AdventureResult [] CLOVER_WEAPONS = { BJORNS_HAMMER, TURTLESLINGER, FIVE_ALARM_SAUCEPAN, PASTA_OF_PERIL, DISCO_BANJO };

	private UseSkillRequest( String skillName )
	{
		super( "skills.php" );

		addFormField( "action", "Skillz." );
		addFormField( "pwd" );

		this.skillId = ClassSkillsDatabase.getSkillId( skillName );
		this.skillName = ClassSkillsDatabase.getSkillName( skillId );

		addFormField( "whichskill", String.valueOf( skillId ) );
		this.target = "yourself";
	}

	public void setTarget( String target )
	{
		if ( ClassSkillsDatabase.isBuff( skillId ) )
		{
			this.countFieldId = "bufftimes";

			if ( target == null || target.trim().length() == 0 || target.equals( String.valueOf( KoLCharacter.getUserId() ) ) || target.equals( KoLCharacter.getUserName() ) )
			{
				this.target = "yourself";
				addFormField( "specificplayer", KoLCharacter.getUserName() );
			}
			else
			{
				this.target = target;
				addFormField( "specificplayer", target );
			}
		}
		else
		{
			this.countFieldId = "quantity";
			this.target = null;
		}
	}

	public void setBuffCount( int buffCount )
	{
		for ( int i = 0; i < BREAKFAST_SKILLS.length; ++i )
			if ( this.skillName.equals( BREAKFAST_SKILLS[i][0] ) )
				buffCount = Math.min( StaticEntity.parseInt( BREAKFAST_SKILLS[i][1] ), buffCount );

		int maxPossible = (int) Math.floor( (float) KoLCharacter.getCurrentMP() / (float) ClassSkillsDatabase.getMPConsumptionById( skillId ) );

		if ( buffCount < 1 )
			buffCount += maxPossible;
		else if ( buffCount == Integer.MAX_VALUE )
			buffCount = maxPossible;

		this.buffCount = buffCount;
	}

	public int compareTo( Object o )
	{
		if ( o == null || !(o instanceof UseSkillRequest) )
			return -1;

		int mpDifference = ClassSkillsDatabase.getMPConsumptionById( skillId ) -
			ClassSkillsDatabase.getMPConsumptionById( ((UseSkillRequest)o).skillId );

		return mpDifference != 0 ? mpDifference : skillName.compareToIgnoreCase( ((UseSkillRequest)o).skillName );
	}

	public int getSkillId()
	{	return skillId;
	}

	public String getSkillName()
	{	return skillName;
	}

	public String toString()
	{	return skillName + " (" + ClassSkillsDatabase.getMPConsumptionById( skillId ) + " mp)";
	}

	public static AdventureResult optimizeEquipment( int skillId )
	{
		AdventureResult songWeapon = null;
		SpecialOutfit.createImplicitCheckpoint();

		// Ode to Booze is usually cast as a single shot.  So,
		// don't prepare a rock and roll legend.

		if ( skillId == 6014 )
		{
			if ( KoLCharacter.hasItem( ACCORDION ) )
				songWeapon = ACCORDION;
			else if ( KoLCharacter.hasItem( ROCKNROLL_LEGEND ) )
				songWeapon = ROCKNROLL_LEGEND;

			if ( songWeapon == null )
			{
				lastUpdate = "You need an accordion to play Accordion Thief songs.";
				KoLmafia.updateDisplay( ERROR_STATE, lastUpdate );
				return null;
			}
		}

		// All other accordion thief buffs should prepare a rock
		// and roll legend.

		else if ( skillId > 6000 && skillId < 7000 )
		{
			songWeapon = prepareAccordion();

			if ( songWeapon == null )
			{
				lastUpdate = "You need an accordion to play Accordion Thief songs.";
				KoLmafia.updateDisplay( ERROR_STATE, lastUpdate );
				return null;
			}

			// If there's a stolen accordion equipped, unequip it so the
			// Rock and Roll Legend in inventory is used to play the song

			if ( songWeapon != ACCORDION && KoLCharacter.hasEquipped( ACCORDION ) )
				(new EquipmentRequest( EquipmentRequest.UNEQUIP, KoLCharacter.WEAPON )).run();

			if ( songWeapon != null && songWeapon != ACCORDION && !KoLCharacter.hasEquipped( ROCKNROLL_LEGEND ) )
				AdventureDatabase.retrieveItem( ROCKNROLL_LEGEND );
		}

		// Ode to Booze is usually cast as a single shot.  So,
		// don't equip the jewel-eyed wizard hat.

		if ( ClassSkillsDatabase.isBuff( skillId ) && skillId > 1000 && skillId != 6014 && inventory.contains( WIZARD_HAT ) )
			(new EquipmentRequest( WIZARD_HAT, KoLCharacter.HAT )).run();

		return songWeapon;
	}

	public void run()
	{
		if ( !KoLCharacter.hasSkill( skillName ) || buffCount == 0 )
			return;

		lastUpdate = "";

		// Cast the skill as many times as needed

		SpecialOutfit.createImplicitCheckpoint();
		optimizeEquipment( skillId );

		if ( !KoLmafia.permitsContinue() )
			return;

		useSkillLoop();
		SpecialOutfit.restoreImplicitCheckpoint();
	}

	private void useSkillLoop()
	{
		// Before executing the skill, ensure that all necessary mana is
		// recovered in advance.

		int castsRemaining = buffCount;
		int mpPerCast = ClassSkillsDatabase.getMPConsumptionById( skillId );

		int currentMP = KoLCharacter.getCurrentMP();
		int maximumMP = KoLCharacter.getMaximumMP();

		if ( KoLmafia.refusesContinue() )
			return;

		if ( maximumMP < mpPerCast )
		{
			lastUpdate = "Your maximum mana is too low to cast " + skillName + ".";
			KoLmafia.updateDisplay( lastUpdate );
			return;
		}

		int currentCast = 0;
		int maximumCast = maximumMP / mpPerCast;

		while ( !KoLmafia.refusesContinue() && castsRemaining > 0 )
		{
			// Find out how many times we can cast with current MP

			currentCast = Math.min( castsRemaining, KoLCharacter.getCurrentMP() / mpPerCast );

			// If none, attempt to recover MP in order to cast;
			// take auto-recovery into account.

			if ( currentCast == 0 )
			{
				currentCast = Math.min( castsRemaining, maximumCast );

				currentMP = KoLCharacter.getCurrentMP();
				StaticEntity.getClient().recoverMP( mpPerCast * currentCast );

				// If no change occurred, that means the person was
				// unable to recover MP; abort the process.

				if ( currentMP == KoLCharacter.getCurrentMP() )
				{
					lastUpdate = "Could not restore enough mana to cast " + skillName + ".";
					KoLmafia.updateDisplay( lastUpdate );
					return;
				}

				currentCast = Math.min( castsRemaining, KoLCharacter.getCurrentMP() / mpPerCast );
			}

			if ( KoLmafia.refusesContinue() )
			{
				lastUpdate = "Error encountered during cast attempt.";
				return;
			}

			if ( currentCast > 0 )
			{
				// Attempt to cast the buff.  In the event that it
				// fails, make sure to report it and return whether
				// or not at least one cast was completed.

				buffCount = currentCast;
				addFormField( countFieldId, String.valueOf( currentCast ), false );

				if ( target == null || target.trim().length() == 0 )
					KoLmafia.updateDisplay( "Casting " + skillName + " " + currentCast + " times..." );
				else
					KoLmafia.updateDisplay( "Casting " + skillName + " on " + target + " " + currentCast + " times..." );

				super.run();

				// Otherwise, you have completed the correct number
				// of casts.  Deduct it from the number of casts
				// remaining and continue.

				castsRemaining -= currentCast;
			}
		}

		if ( KoLmafia.refusesContinue() )
			lastUpdate = "Error encountered during cast attempt.";
	}

	public static AdventureResult prepareAccordion()
	{
		// Can the rock and roll legend be acquired in some way
		// right now?  If so, retrieve it.

		if ( KoLCharacter.hasItem( ROCKNROLL_LEGEND ) || KoLCharacter.canInteract() )
			return ROCKNROLL_LEGEND;

		// He must have at least a stolen accordion

		if ( !KoLCharacter.hasItem( ACCORDION ) )
			return null;

		// Does he have a hot buttered roll?  If not,
		// untinkering weapons won't help.

		if ( !KoLCharacter.hasItem( ROLL ) )
			return ACCORDION;

		// Can we get a big rock from a clover weapon?

		AdventureResult cloverWeapon = null;
		for ( int i = 0; i < CLOVER_WEAPONS.length; ++i )
			if ( KoLCharacter.hasItem( CLOVER_WEAPONS[i] ) )
				cloverWeapon = CLOVER_WEAPONS[i];

		// If not, just use existing stolen accordion

		if ( cloverWeapon == null )
			return ACCORDION;

		// If he's already helped the Untinker, cool - but we don't
		// want to run adventures to fulfill that quest.

		if ( !canUntinker() )
			return ACCORDION;

		// Get the clover weapon from the closet, if it is there
		AdventureDatabase.retrieveItem( cloverWeapon );

		// Otherwise, unequip it
		if ( cloverWeapon.getCount( inventory ) < 1 )
			DEFAULT_SHELL.executeLine( "unequip weapon" );

		// Turn it into a big rock
		untinkerCloverWeapon( cloverWeapon );

		// Build the Rock and Roll Legend
		AdventureDatabase.retrieveItem( ROCKNROLL_LEGEND );
		return cloverWeapon;
	}

	private static boolean canUntinker()
	{
		// If you're too low level, don't even try

		if ( KoLCharacter.getLevel() < 4 )
			return false;

		// If you're in a muscle sign, KoLmafia will finish the
		// quest without problems.

		if ( KoLCharacter.inMuscleSign() )
			return true;

		// Otherwise, visit the untinker and see what he says.
		// If he mentions Degrassi Knoll, you haven't given him
		// his screwdriver yet.

		return UntinkerRequest.completeQuest();
	}

	public static void untinkerCloverWeapon( AdventureResult item )
	{
		switch ( item.getItemId() )
		{
		case 32:	// Bjorn's Hammer
			DEFAULT_SHELL.executeLine( "untinker Bjorn's Hammer" );
			DEFAULT_SHELL.executeLine( "untinker seal-toothed rock" );
			break;
		case 50:	// Rock and Roll Legend
			DEFAULT_SHELL.executeLine( "untinker Rock and Roll Legend" );
			DEFAULT_SHELL.executeLine( "untinker heart of rock and roll" );
			break;
		case 54:	// Disco Banjo
			DEFAULT_SHELL.executeLine( "untinker Disco Banjo" );
			DEFAULT_SHELL.executeLine( "untinker stone banjo" );
			break;
		case 57:	// 5-Alarm Saucepan
			DEFAULT_SHELL.executeLine( "untinker 5-Alarm Saucepan" );
			DEFAULT_SHELL.executeLine( "untinker heavy hot sauce" );
			break;
		case 60:	// Turtleslinger
			DEFAULT_SHELL.executeLine( "untinker Turtleslinger" );
			DEFAULT_SHELL.executeLine( "untinker turtle factory" );
			break;
		case 68:	// Pasta of Peril
			DEFAULT_SHELL.executeLine( "untinker Pasta of Peril" );
			DEFAULT_SHELL.executeLine( "untinker spaghetti with rock-balls" );
			break;
		}
	}

	public void processResults()
	{
		boolean encounteredError = false;

		// If a reply was obtained, check to see if it was a success message
		// Otherwise, try to figure out why it was unsuccessful.

		if ( responseText == null || responseText.trim().length() == 0 )
		{
			encounteredError = true;
			lastUpdate = "Encountered lag problems.";
		}
		else if ( responseText.indexOf( "You don't have that skill" ) != -1 )
		{
			encounteredError = true;
			lastUpdate = "That skill is unavailable.";
		}
		else if ( responseText.indexOf( "You don't have enough" ) != -1 )
		{
			encounteredError = true;
			lastUpdate = "Not enough mana to cast " + skillName + ".";
		}
		else if ( responseText.indexOf( "You can only conjure" ) != -1 || responseText.indexOf( "You can only scrounge up" ) != -1 || responseText.indexOf( "You can only summon" ) != -1 )
		{
			encounteredError = true;
			lastUpdate = "Summon limit exceeded.";
		}
		else if ( responseText.indexOf( "too many songs" ) != -1 )
		{
			encounteredError = true;
			lastUpdate = "Selected target has 3 AT buffs already.";
		}
		else if ( responseText.indexOf( "casts left of the Smile of Mr. A" ) != -1 )
		{
			encounteredError = true;
			lastUpdate = "You cannot cast that many smiles.";
		}
		else if ( responseText.indexOf( "Invalid target player" ) != -1 )
		{
			encounteredError = true;
			lastUpdate = "Selected target is not a valid target.";
		}
		else if ( responseText.indexOf( "busy fighting" ) != -1 )
		{
			encounteredError = true;
			lastUpdate = "Selected target is busy fighting.";
		}
		else if ( responseText.indexOf( "receive buffs" ) != -1 )
		{
			encounteredError = true;
			lastUpdate = "Selected target cannot receive buffs.";
		}
		else if ( responseText.indexOf( "accordion equipped" ) != -1 )
		{
			// "You need to have an accordion equipped or in your
			// inventory if you want to play that song."

			encounteredError = true;
			lastUpdate = "You need an accordion to play Accordion Thief songs.";
		}

		// Now that all the checks are complete, proceed
		// to determine how to update the user display.

		if ( encounteredError )
		{
			KoLmafia.updateDisplay( lastUpdate );

			if ( BuffBotHome.isBuffBotActive() )
				BuffBotHome.timeStampedLogEntry( BuffBotHome.ERRORCOLOR, lastUpdate );
		}
		else
		{
			if ( target == null )
				KoLmafia.updateDisplay( skillName + " was successfully cast." );
			else
				KoLmafia.updateDisplay( skillName + " was successfully cast on " + target + "." );

			// Tongue of the Walrus (1010) automatically
			// removes any beaten up.

			StaticEntity.getClient().processResult( new AdventureResult( AdventureResult.MP, 0 - (ClassSkillsDatabase.getMPConsumptionById( skillId ) * buffCount) ) );
			KoLmafia.applyEffects();

			if ( skillId == OTTER_TONGUE || skillId == WALRUS_TONGUE )
			{
				activeEffects.remove( KoLAdventure.BEATEN_UP );
				needsRefresh = true;
			}
		}
	}

	public boolean equals( Object o )
	{	return o != null && o instanceof UseSkillRequest && getSkillName().equals( ((UseSkillRequest)o).getSkillName() );
	}

	public String getCommandForm()
	{	return "cast " + buffCount + " " + skillName;
	}

	public static boolean processRequest( String urlString )
	{
		if ( urlString.indexOf( "skills.php" ) == -1 )
			return false;

		Matcher skillMatcher = SKILLID_PATTERN.matcher( urlString );
		if ( !skillMatcher.find() )
			return false;

		String skillName = ClassSkillsDatabase.getSkillName( StaticEntity.parseInt( skillMatcher.group(1) ) );
		Matcher countMatcher = COUNT_PATTERN.matcher( urlString );

		int count = countMatcher.find() || countMatcher.group(2).equals( "" ) ? 1 :
			StaticEntity.parseInt( countMatcher.group(2) );

		KoLmafia.getSessionStream().println();
		KoLmafia.getSessionStream().println( "cast " + count + " " + skillName );
		return true;
	}

	public static UseSkillRequest getInstance( int skillId )
	{	return getInstance( ClassSkillsDatabase.getSkillName( skillId ) );
	}

	public static UseSkillRequest getInstance( String skillName, int buffCount )
	{	return getInstance( skillName, KoLCharacter.getUserName(), buffCount );
	}

	public static UseSkillRequest getInstance( String skillName, String target, int buffCount )
	{
		UseSkillRequest instance = getInstance( skillName );
		if ( instance == null )
			return null;

		instance.setTarget( target == null || target.equals( "" ) ? KoLCharacter.getUserName() : target );
		instance.setBuffCount( buffCount );
		return instance;
	}

	public static UseSkillRequest getInstance( String skillName )
	{
		if ( skillName == null || !ClassSkillsDatabase.contains( skillName ) )
			return null;

		skillName = KoLDatabase.getCanonicalName( skillName );
		if ( !ALL_SKILLS.containsKey( skillName ) )
			ALL_SKILLS.put( skillName, new UseSkillRequest( skillName ) );

		UseSkillRequest request = (UseSkillRequest) ALL_SKILLS.get( skillName );
		request.setTarget( KoLCharacter.getUserName() );
		request.setBuffCount( 0 );
		return request;
	}
}
