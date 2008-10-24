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
import net.sourceforge.kolmafia.BuffBotHome;
import net.sourceforge.kolmafia.HPRestoreItemList;
import net.sourceforge.kolmafia.KoLAdventure;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.SpecialOutfit;
import net.sourceforge.kolmafia.StaticEntity;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.session.EquipmentManager;
import net.sourceforge.kolmafia.session.InventoryManager;
import net.sourceforge.kolmafia.session.MoodManager;
import net.sourceforge.kolmafia.session.ResultProcessor;
import net.sourceforge.kolmafia.utilities.StringUtilities;

import net.sourceforge.kolmafia.persistence.Preferences;
import net.sourceforge.kolmafia.persistence.SkillDatabase;

public class UseSkillRequest
	extends GenericRequest
	implements Comparable
{
	private static final TreeMap ALL_SKILLS = new TreeMap();
	private static final Pattern SKILLID_PATTERN = Pattern.compile( "whichskill=(\\d+)" );
	private static final Pattern BOOKID_PATTERN = Pattern.compile( "preaction=summon([^&]*)" );

	private static final Pattern COUNT1_PATTERN = Pattern.compile( "bufftimes=([\\d,]+)" );
	private static final Pattern COUNT2_PATTERN = Pattern.compile( "quantity=([\\d,]+)" );

	public static final String[] BREAKFAST_SKILLS =
	{
		"Advanced Cocktailcrafting",
		"Pastamastery",
		"Advanced Saucecrafting",
		"Summon Snowcone",
		"Summon Hilarious Objects",
		"Summon Tasteful Items"
	};

	public static final String[] LIBRAM_SKILLS =
	{
		"Summon Candy Hearts",
		"Summon Party Favor"
	};

	private static final int OTTER_TONGUE = 1007;
	private static final int WALRUS_TONGUE = 1010;
	private static final int BANDAGES = 3009;
	private static final int COCOON = 3012;
	private static final int DISCO_NAP = 5007;
	private static final int POWER_NAP = 5011;

	public static String lastUpdate = "";

	private final int skillId;
	private final String skillName;
	private String target;
	private int buffCount;
	private String countFieldId;

	private int lastReduction = Integer.MAX_VALUE;
	private String lastStringForm = "";

	public static final AdventureResult[] TAMER_WEAPONS = new AdventureResult[]
	{
		new AdventureResult( 2558, 1 ),	// Chelonian Morningstar
		new AdventureResult( 60, 1 ),	// Mace of the Tortoise
		new AdventureResult( 4, 1 )	// turtle totem
	};

	public static final AdventureResult[] SAUCE_WEAPONS = new AdventureResult[]
	{
		new AdventureResult( 2560, 1 ),	// 17-Alarm Saucepan
		new AdventureResult( 57, 1 ),	// 5-Alarm saucepan
		new AdventureResult( 7, 1 )	// saucepan
	};

	public static final AdventureResult[] THIEF_WEAPONS = new AdventureResult[]
	{
		new AdventureResult( 2557, 1 ),	// Squeezebox of the Ages
		new AdventureResult( 50, 1 ),	// Rock 'n Roll Legend
		new AdventureResult( 2234, 1 ),	// calavera concertina
		new AdventureResult( 11, 1 )	// stolen accordion
	};

	public static final AdventureResult PLEXI_PENDANT = new AdventureResult( 1235, 1 );
	public static final AdventureResult BRIM_BERET = new AdventureResult( 2813, 1 );
	public static final AdventureResult WIZARD_HAT = new AdventureResult( 1653, 1 );

	public static final AdventureResult PLEXI_WATCH = new AdventureResult( 1232, 1 );
	public static final AdventureResult BRIM_BRACELET = new AdventureResult( 2818, 1 );
	public static final AdventureResult SOLITAIRE = new AdventureResult( 1226, 1 );

	public static final AdventureResult WIRE_BRACELET = new AdventureResult( 2514, 1 );
	public static final AdventureResult BACON_BRACELET = new AdventureResult( 717, 1 );
	public static final AdventureResult BACON_EARRING = new AdventureResult( 715, 1 );
	public static final AdventureResult SOLID_EARRING = new AdventureResult( 2780, 1 );

	// The following list must contain only accessories!
	private static final AdventureResult[] AVOID_REMOVAL = new AdventureResult[]
	{
		UseSkillRequest.PLEXI_WATCH,
		UseSkillRequest.BRIM_BRACELET,
		UseSkillRequest.SOLITAIRE,
		UseSkillRequest.WIRE_BRACELET,
		UseSkillRequest.BACON_BRACELET,
		UseSkillRequest.BACON_EARRING,
		UseSkillRequest.SOLID_EARRING,
		// Removing the following might drop an AT song
		UseSkillRequest.PLEXI_PENDANT
	};

	private UseSkillRequest( final String skillName )
	{
		super( UseSkillRequest.chooseURL( skillName ) );

		this.skillId = SkillDatabase.getSkillId( skillName );
		this.skillName = SkillDatabase.getSkillName( this.skillId );
		this.target = "yourself";

                this.addFormFields();
	}

	private static String chooseURL( final String skillName )
	{
		switch ( SkillDatabase.getSkillId( skillName ) )
		{
		case SkillDatabase.SNOWCONE:
		case SkillDatabase.HILARIOUS:
		case SkillDatabase.TASTEFUL:
		case SkillDatabase.CANDY_HEART:
		case SkillDatabase.PARTY_FAVOR:
			return "campground.php";
		}

		return "skills.php";
	}

	private void addFormFields()
	{
		switch ( this.skillId )
		{
		case SkillDatabase.SNOWCONE:
			this.addFormField( "preaction", "summonsnowcone" );
			break;

		case SkillDatabase.HILARIOUS:
			this.addFormField( "preaction", "summonhilariousitems" );
			break;

		case SkillDatabase.TASTEFUL:
			this.addFormField( "preaction", "summonspencersitems" );
			break;

		case SkillDatabase.CANDY_HEART:
			this.addFormField( "preaction", "summoncandyheart" );
			break;

		case SkillDatabase.PARTY_FAVOR:
			this.addFormField( "preaction", "summonpartyfavor" );
			break;

		default:
			this.addFormField( "action", "Skillz." );
			this.addFormField( "whichskill", String.valueOf( this.skillId ) );
			break;
		}
	}

	public void setTarget( final String target )
	{
		if ( SkillDatabase.isBuff( this.skillId ) )
		{
			this.countFieldId = "bufftimes";

			if ( target == null || target.trim().length() == 0 || target.equals( KoLCharacter.getPlayerId() ) || target.equals( KoLCharacter.getUserName() ) )
			{
				this.target = "yourself";
				this.addFormField( "specificplayer", KoLCharacter.getPlayerId() );
			}
			else
			{
				this.target = KoLmafia.getPlayerName( target );
				this.addFormField( "specificplayer", KoLmafia.getPlayerId( target ) );
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
		int mpCost = SkillDatabase.getMPConsumptionById( this.skillId );
		if ( mpCost == 0 )
		{
			this.buffCount = 0;
			return;
		}

		int maxPossible = 0;
		int availableMP = KoLCharacter.getCurrentMP();

		if ( SkillDatabase.isLibramSkill( this.skillId ) )
		{
			maxPossible = SkillDatabase.libramSkillCasts( availableMP );
		}
		else
		{
			maxPossible = Math.min( this.getMaximumCast(), availableMP / mpCost );
		}

		if ( buffCount < 1 )
		{
			buffCount += maxPossible;
		}
		else if ( buffCount == Integer.MAX_VALUE )
		{
			buffCount = maxPossible;
		}

		this.buffCount = buffCount;
	}

	public int compareTo( final Object o )
	{
		if ( o == null || !( o instanceof UseSkillRequest ) )
		{
			return -1;
		}

		int mpDifference =
			SkillDatabase.getMPConsumptionById( this.skillId ) - SkillDatabase.getMPConsumptionById( ( (UseSkillRequest) o ).skillId );

		return mpDifference != 0 ? mpDifference : this.skillName.compareToIgnoreCase( ( (UseSkillRequest) o ).skillName );
	}

	public int getSkillId()
	{
		return this.skillId;
	}

	public String getSkillName()
	{
		return this.skillName;
	}

	public int getMaximumCast()
	{
		int maximumCast = Integer.MAX_VALUE;

		switch ( this.skillId )
		{

		// Snowcones and grimoire items can only be summoned
		// once per day.

		case SkillDatabase.SNOWCONE:

			maximumCast = Math.max( 1 - Preferences.getInteger( "snowconeSummons" ), 0 );
			break;

		case SkillDatabase.HILARIOUS:

			maximumCast = Math.max( 1 - Preferences.getInteger( "grimoire1Summons" ), 0 );
			break;

		case SkillDatabase.TASTEFUL:

			maximumCast = Math.max( 1 - Preferences.getInteger( "grimoire2Summons" ), 0 );
			break;

		// Rainbow Gravitation can be cast 3 times per day.  Each
		// casting consumes five elemental wads and a twinkly wad

		case SkillDatabase.RAINBOW:
			maximumCast = Math.max( 3 - Preferences.getInteger( "prismaticSummons" ), 0 );
			maximumCast = Math.min( InventoryManager.getCount( ItemPool.COLD_WAD ), maximumCast );
			maximumCast = Math.min( InventoryManager.getCount( ItemPool.HOT_WAD ), maximumCast );
			maximumCast = Math.min( InventoryManager.getCount( ItemPool.SLEAZE_WAD ), maximumCast );
			maximumCast = Math.min( InventoryManager.getCount( ItemPool.SPOOKY_WAD ), maximumCast );
			maximumCast = Math.min( InventoryManager.getCount( ItemPool.STENCH_WAD ), maximumCast );
			maximumCast = Math.min( InventoryManager.getCount( ItemPool.TWINKLY_WAD ), maximumCast );
			break;

		// Transcendental Noodlecraft affects # of summons for
		// Pastamastery

		case 3006:

			maximumCast = 3;
			if ( KoLCharacter.hasSkill( "Transcendental Noodlecraft" ) )
			{
				maximumCast = 5;
			}

			maximumCast = Math.max( maximumCast - Preferences.getInteger( "noodleSummons" ), 0 );
			break;

		// The Way of Sauce affects # of summons for
		// Advanced Saucecrafting

		case 4006:

			maximumCast = 3;
			if ( KoLCharacter.hasSkill( "The Way of Sauce" ) )
			{
				maximumCast = 5;
			}

			maximumCast = Math.max( maximumCast - Preferences.getInteger( "reagentSummons" ), 0 );
			break;

		// Superhuman Cocktailcrafting affects # of summons for
		// Advanced Cocktailcrafting

		case 5014:

			maximumCast = 3;
			if ( KoLCharacter.hasSkill( "Superhuman Cocktailcrafting" ) )
			{
				maximumCast = 5;
			}

			maximumCast = Math.max( maximumCast - Preferences.getInteger( "cocktailSummons" ), 0 );
			break;

		}

		return maximumCast;
	}

	public String toString()
	{
		if ( this.lastReduction == KoLCharacter.getManaCostAdjustment() && !SkillDatabase.isLibramSkill( this.skillId ) )
		{
			return this.lastStringForm;
		}

		this.lastReduction = KoLCharacter.getManaCostAdjustment();
		this.lastStringForm = this.skillName + " (" + SkillDatabase.getMPConsumptionById( this.skillId ) + " mp)";
		return this.lastStringForm;
	}

	private static final boolean canSwitchToItem( final AdventureResult item )
	{
		return !KoLCharacter.hasEquipped( item ) && EquipmentManager.canEquip( item.getName() ) && InventoryManager.hasItem(
			item, false );
	}

	public static final void optimizeEquipment( final int skillId )
	{
		boolean isBuff = SkillDatabase.isBuff( skillId );

		if ( isBuff )
		{
			if ( skillId > 2000 && skillId < 3000 )
			{
				UseSkillRequest.prepareWeapon( UseSkillRequest.TAMER_WEAPONS );
			}

			if ( skillId > 4000 && skillId < 5000 )
			{
				UseSkillRequest.prepareWeapon( UseSkillRequest.SAUCE_WEAPONS );
			}

			if ( skillId > 6000 && skillId < 7000 )
			{
				UseSkillRequest.prepareWeapon( UseSkillRequest.THIEF_WEAPONS );
			}
		}

		if ( Preferences.getBoolean( "switchEquipmentForBuffs" ) )
		{
			UseSkillRequest.reduceManaConsumption( skillId, isBuff );
		}
	}

	private static final boolean isValidSwitch( final int slotId )
	{
		AdventureResult item = EquipmentManager.getEquipment( slotId );
		for ( int i = 0; i < UseSkillRequest.AVOID_REMOVAL.length; ++i )
		{
			if ( item.equals( UseSkillRequest.AVOID_REMOVAL[ i ] ) )
			{
				return false;
			}
		}

		return true;
	}

	private static final int attemptSwitch( final int skillId, final AdventureResult item, final boolean slot1Allowed,
		final boolean slot2Allowed, final boolean slot3Allowed )
	{
		if ( slot3Allowed )
		{
			( new EquipmentRequest( item, EquipmentManager.ACCESSORY3 ) ).run();
			return EquipmentManager.ACCESSORY3;
		}

		if ( slot2Allowed )
		{
			( new EquipmentRequest( item, EquipmentManager.ACCESSORY2 ) ).run();
			return EquipmentManager.ACCESSORY2;
		}

		if ( slot1Allowed )
		{
			( new EquipmentRequest( item, EquipmentManager.ACCESSORY1 ) ).run();
			return EquipmentManager.ACCESSORY1;
		}

		return -1;
	}

	private static final void reduceManaConsumption( final int skillId, final boolean isBuff )
	{
		// Never bother trying to reduce mana consumption when casting
		// ode to booze or a libram skill

		if ( skillId == 6014 || SkillDatabase.isLibramSkill( skillId ) )
		{
			return;
		}

		if ( KoLCharacter.canInteract() )
		{
			return;
		}

		// First determine which slots are available for switching in
		// MP reduction items.

		boolean slot1Allowed = UseSkillRequest.isValidSwitch( EquipmentManager.ACCESSORY1 );
		boolean slot2Allowed = UseSkillRequest.isValidSwitch( EquipmentManager.ACCESSORY2 );
		boolean slot3Allowed = UseSkillRequest.isValidSwitch( EquipmentManager.ACCESSORY3 );

		// Best switch is a PLEXI_WATCH, since it's a guaranteed -3 to
		// spell cost.

		for ( int i = 0; i < UseSkillRequest.AVOID_REMOVAL.length; ++i )
		{
			if ( SkillDatabase.getMPConsumptionById( skillId ) == 1 || KoLCharacter.getManaCostAdjustment() == -3 )
			{
				return;
			}

			if ( !UseSkillRequest.canSwitchToItem( UseSkillRequest.AVOID_REMOVAL[ i ] ) )
			{
				continue;
			}

			switch ( UseSkillRequest.attemptSwitch(
				skillId, UseSkillRequest.AVOID_REMOVAL[ i ], slot1Allowed, slot2Allowed, slot3Allowed ) )
			{
			case EquipmentManager.ACCESSORY1:
				slot1Allowed = false;
				break;
			case EquipmentManager.ACCESSORY2:
				slot2Allowed = false;
				break;
			case EquipmentManager.ACCESSORY3:
				slot3Allowed = false;
				break;
			}
		}
	}

	public static final int songLimit()
	{
		if ( KoLCharacter.hasEquipped( UseSkillRequest.PLEXI_PENDANT ) || KoLCharacter.hasEquipped( UseSkillRequest.BRIM_BERET ) )
		{
			return 4;
		}
		return 3;
	}

	public void run()
	{
		if ( !KoLCharacter.hasSkill( this.skillName ) || this.buffCount == 0 )
		{
			return;
		}

		UseSkillRequest.lastUpdate = "";
		UseSkillRequest.optimizeEquipment( this.skillId );

		if ( !KoLmafia.permitsContinue() )
		{
			return;
		}

		this.setBuffCount( Math.min( this.buffCount, this.getMaximumCast() ) );
		this.useSkillLoop();
	}

	private void useSkillLoop()
	{
		if ( KoLmafia.refusesContinue() )
		{
			return;
		}

		// Before executing the skill, ensure that all necessary mana is
		// recovered in advance.

		int castsRemaining = this.buffCount;

		int maximumMP = KoLCharacter.getMaximumMP();
		int mpPerCast = SkillDatabase.getMPConsumptionById( this.skillId );
		int maximumCast = maximumMP / mpPerCast;

		while ( !KoLmafia.refusesContinue() && castsRemaining > 0 )
		{
			if ( SkillDatabase.isLibramSkill( this.skillId ) )
			{
				mpPerCast = SkillDatabase.getMPConsumptionById( this.skillId );
			}

			if ( maximumMP < mpPerCast )
			{
				UseSkillRequest.lastUpdate = "Your maximum mana is too low to cast " + this.skillName + ".";
				KoLmafia.updateDisplay( UseSkillRequest.lastUpdate );
				return;
			}

			// Find out how many times we can cast with current MP

			int currentCast = this.availableCasts( castsRemaining, mpPerCast );

			// If none, attempt to recover MP in order to cast;
			// take auto-recovery into account.

			if ( currentCast == 0 )
			{
				currentCast = Math.min( castsRemaining, maximumCast );
				int currentMP = KoLCharacter.getCurrentMP();

				int recoverMP = mpPerCast * currentCast;

				SpecialOutfit.createImplicitCheckpoint();
				if ( MoodManager.isExecuting() )
				{
					recoverMP = Math.min( Math.max( recoverMP, MoodManager.getMaintenanceCost() ), maximumMP );
				}
				StaticEntity.getClient().recoverMP( recoverMP  );
				SpecialOutfit.restoreImplicitCheckpoint();

				// If no change occurred, that means the person
				// was unable to recover MP; abort the process.

				if ( currentMP == KoLCharacter.getCurrentMP() )
				{
					UseSkillRequest.lastUpdate = "Could not restore enough mana to cast " + this.skillName + ".";
					KoLmafia.updateDisplay( UseSkillRequest.lastUpdate );
					return;
				}

				currentCast = this.availableCasts( castsRemaining, mpPerCast );
			}

			if ( KoLmafia.refusesContinue() )
			{
				UseSkillRequest.lastUpdate = "Error encountered during cast attempt.";
				return;
			}

			// If this happens to be a health-restorative skill,
			// then there is an effective cap based on how much
			// the skill is able to restore.

			switch ( this.skillId )
			{
			case OTTER_TONGUE:
			case WALRUS_TONGUE:
			case DISCO_NAP:
			case POWER_NAP:
			case BANDAGES:
			case COCOON:

				int healthRestored = HPRestoreItemList.getHealthRestored( this.skillName );
				int maxPossible = Math.max( 1, ( KoLCharacter.getMaximumHP() - KoLCharacter.getCurrentHP() ) / healthRestored );
				castsRemaining = Math.min( castsRemaining, maxPossible );
				currentCast = Math.min( currentCast, castsRemaining );
				break;
			}

			currentCast = Math.min( currentCast, maximumCast );

			if ( currentCast > 0 )
			{
				// Attempt to cast the buff.

				this.buffCount = currentCast;
				UseSkillRequest.optimizeEquipment( this.skillId );

				if ( KoLmafia.refusesContinue() )
				{
					UseSkillRequest.lastUpdate = "Error encountered during cast attempt.";
					return;
				}

				this.addFormField( this.countFieldId, String.valueOf( currentCast ), false );

				if ( this.target == null || this.target.trim().length() == 0 )
				{
					KoLmafia.updateDisplay( "Casting " + this.skillName + " " + currentCast + " times..." );
				}
				else
				{
					KoLmafia.updateDisplay( "Casting " + this.skillName + " on " + this.target + " " + currentCast + " times..." );
				}

				super.run();

				// Otherwise, you have completed the correct
				// number of casts.  Deduct it from the number
				// of casts remaining and continue.

				castsRemaining -= currentCast;
			}
		}

		if ( KoLmafia.refusesContinue() )
		{
			UseSkillRequest.lastUpdate = "Error encountered during cast attempt.";
		}
	}

	public final int availableCasts( int maxCasts, int mpPerCast )
	{
		int availableMP = KoLCharacter.getCurrentMP();
		int currentCast = 0;

		if ( SkillDatabase.isLibramSkill( this.skillId ) )
		{
			currentCast = SkillDatabase.libramSkillCasts( availableMP );
		}
		else
		{
			currentCast = availableMP / mpPerCast;
			currentCast = Math.min( this.getMaximumCast(), currentCast );
		}

		currentCast = Math.min( maxCasts, currentCast );

		return currentCast;
	}

	public static final boolean hasAccordion()
	{
		if ( KoLCharacter.canInteract() )
		{
			return true;
		}

		for ( int i = 0; i < UseSkillRequest.THIEF_WEAPONS.length; ++i )
		{
			if ( InventoryManager.hasItem( UseSkillRequest.THIEF_WEAPONS[ i ], true ) )
			{
				return true;
			}
		}

		return false;
	}

	public static final void prepareWeapon( final AdventureResult[] options )
	{
		if ( KoLCharacter.canInteract() )
		{
			if ( InventoryManager.hasItem( options[ 0 ], false ) || InventoryManager.hasItem( options[ 1 ], false ) )
			{
				return;
			}

			InventoryManager.retrieveItem( options[ 1 ] );
			return;
		}

		for ( int i = 0; i < options.length; ++i )
		{
			if ( !InventoryManager.hasItem( options[ i ], false ) )
			{
				continue;
			}

			if ( KoLCharacter.hasEquipped( options[ i ] ) )
			{
				return;
			}

			InventoryManager.retrieveItem( options[ i ] );
			return;
		}

		InventoryManager.retrieveItem( options[ options.length - 1 ] );
	}

	protected boolean retryOnTimeout()
	{
		return false;
	}

	protected boolean processOnFailure()
	{
		return true;
	}

	public void processResults()
	{
		boolean shouldStop = false;
		UseSkillRequest.lastUpdate = "";

		// If a reply was obtained, check to see if it was a success message
		// Otherwise, try to figure out why it was unsuccessful.

		if ( this.responseText == null || this.responseText.trim().length() == 0 )
		{
			int initialMP = KoLCharacter.getCurrentMP();
			CharPaneRequest.getInstance().run();

			if ( initialMP == KoLCharacter.getCurrentMP() )
			{
				shouldStop = false;
				UseSkillRequest.lastUpdate = "Encountered lag problems.";
			}
		}
		else if ( this.responseText.indexOf( "You don't have that skill" ) != -1 )
		{
			shouldStop = true;
			UseSkillRequest.lastUpdate = "That skill is unavailable.";
		}
		else if ( this.responseText.indexOf( "You don't have enough" ) != -1 )
		{
			shouldStop = false;
			CharPaneRequest.getInstance().run();
			UseSkillRequest.lastUpdate = "Not enough mana to cast " + this.skillName + ".";
		}
		else if ( this.responseText.indexOf( "You can only conjure" ) != -1 || this.responseText.indexOf( "You can only scrounge up" ) != -1 || this.responseText.indexOf( "You can only summon" ) != -1 )
		{
			shouldStop = false;
			UseSkillRequest.lastUpdate = "Summon limit exceeded.";
		}
		else if ( this.responseText.indexOf( "too many songs" ) != -1 )
		{
			shouldStop = false;
			UseSkillRequest.lastUpdate = "Selected target has 3 AT buffs already.";
		}
		else if ( this.responseText.indexOf( "casts left of the Smile of Mr. A" ) != -1 )
		{
			shouldStop = false;
			UseSkillRequest.lastUpdate = "You cannot cast that many smiles.";
		}
		else if ( this.responseText.indexOf( "Invalid target player" ) != -1 )
		{
			shouldStop = true;
			UseSkillRequest.lastUpdate = "Selected target is not a valid target.";
		}
		else if ( this.responseText.indexOf( "busy fighting" ) != -1 )
		{
			shouldStop = false;
			UseSkillRequest.lastUpdate = "Selected target is busy fighting.";
		}
		else if ( this.responseText.indexOf( "receive buffs" ) != -1 )
		{
			shouldStop = false;
			UseSkillRequest.lastUpdate = "Selected target cannot receive buffs.";
		}
		else if ( this.responseText.indexOf( "You need" ) != -1 )
		{
			shouldStop = true;
			UseSkillRequest.lastUpdate = "You need special equipment to cast that buff.";
		}

		// Now that all the checks are complete, proceed
		// to determine how to update the user display.

		if ( !UseSkillRequest.lastUpdate.equals( "" ) )
		{
			KoLmafia.updateDisplay(
				shouldStop ? KoLConstants.ABORT_STATE : KoLConstants.CONTINUE_STATE, UseSkillRequest.lastUpdate );

			if ( BuffBotHome.isBuffBotActive() )
			{
				BuffBotHome.timeStampedLogEntry( BuffBotHome.ERRORCOLOR, UseSkillRequest.lastUpdate );
			}
		}
		else
		{
			if ( this.target == null )
			{
				KoLmafia.updateDisplay( this.skillName + " was successfully cast." );
			}
			else
			{
				KoLmafia.updateDisplay( this.skillName + " was successfully cast on " + this.target + "." );
			}
			
			if ( !SkillDatabase.isLibramSkill( this.skillId ) )
			{
				int mpCost = SkillDatabase.getMPConsumptionById( this.skillId ) * this.buffCount;

				ResultProcessor.processResult(
					new AdventureResult( AdventureResult.MP, 0 - mpCost ) );
			}

			// Tongue of the Walrus (1010) automatically
			// removes any beaten up.

			if ( this.skillId == UseSkillRequest.OTTER_TONGUE || this.skillId == UseSkillRequest.WALRUS_TONGUE )
			{
				KoLConstants.activeEffects.remove( KoLAdventure.BEATEN_UP );
			}

			if ( this.skillId == UseSkillRequest.DISCO_NAP || this.skillId == UseSkillRequest.POWER_NAP )
			{
				KoLConstants.activeEffects.clear();
			}
		}
	}

	public boolean equals( final Object o )
	{
		return o != null && o instanceof UseSkillRequest && this.getSkillName().equals(
			( (UseSkillRequest) o ).getSkillName() );
	}

	public static final UseSkillRequest getInstance( final int skillId )
	{
		return UseSkillRequest.getInstance( SkillDatabase.getSkillName( skillId ) );
	}

	public static final UseSkillRequest getInstance( final String skillName, final int buffCount )
	{
		return UseSkillRequest.getInstance( skillName, KoLCharacter.getUserName(), buffCount );
	}

	public static final UseSkillRequest getInstance( final String skillName, final String target, final int buffCount )
	{
		UseSkillRequest instance = UseSkillRequest.getInstance( skillName );
		if ( instance == null )
		{
			return null;
		}

		instance.setTarget( target == null || target.equals( "" ) ? KoLCharacter.getUserName() : target );
		instance.setBuffCount( buffCount );
		return instance;
	}

	public static final UseSkillRequest getInstance( String skillName )
	{
		if ( skillName == null || !SkillDatabase.contains( skillName ) )
		{
			return null;
		}

		skillName = StringUtilities.getCanonicalName( skillName );
		if ( !UseSkillRequest.ALL_SKILLS.containsKey( skillName ) )
		{
			UseSkillRequest.ALL_SKILLS.put( skillName, new UseSkillRequest( skillName ) );
		}

		UseSkillRequest request = (UseSkillRequest) UseSkillRequest.ALL_SKILLS.get( skillName );
		request.setTarget( KoLCharacter.getUserName() );
		request.setBuffCount( 0 );
		return request;
	}

	public static final boolean registerRequest( final String urlString )
	{
		if ( urlString.startsWith( "skills.php" ) )
		{
			return registerSkillRequest( urlString );
		}

		if ( urlString.startsWith( "campground.php" ) )
		{
			return registerBookRequest( urlString );
		}

		return false;
	}

	private static final boolean registerSkillRequest( final String urlString )
	{
		Matcher skillMatcher = UseSkillRequest.SKILLID_PATTERN.matcher( urlString );
		if ( !skillMatcher.find() )
		{
			return false;
		}

		int skillId = StringUtilities.parseInt( skillMatcher.group( 1 ) );
		String skillName = SkillDatabase.getSkillName( skillId );

		int count = 1;
		Matcher countMatcher = UseSkillRequest.COUNT1_PATTERN.matcher( urlString );

		if ( countMatcher.find() )
		{
			count = StringUtilities.parseInt( countMatcher.group( 1 ) );
		}
		else
		{
			countMatcher = UseSkillRequest.COUNT2_PATTERN.matcher( urlString );
			if ( countMatcher.find() )
			{
				count = StringUtilities.parseInt( countMatcher.group( 1 ) );
			}
		}

		RequestLogger.updateSessionLog();
		RequestLogger.updateSessionLog( "cast " + count + " " + skillName );

		switch ( skillId )
		{
		case SkillDatabase.RAINBOW:

                        // Each cast of Rainbow Gravitation consumes five
                        // elemental wads and a twinkly wad

			ResultProcessor.processResult( ItemPool.get( ItemPool.COLD_WAD, -count ) );
			ResultProcessor.processResult( ItemPool.get( ItemPool.HOT_WAD, -count ) );
			ResultProcessor.processResult( ItemPool.get( ItemPool.SLEAZE_WAD, -count ) );
			ResultProcessor.processResult( ItemPool.get( ItemPool.SPOOKY_WAD, -count ) );
			ResultProcessor.processResult( ItemPool.get( ItemPool.STENCH_WAD, -count ) );
			ResultProcessor.processResult( ItemPool.get( ItemPool.TWINKLY_WAD, -count ) );

			Preferences.increment( "prismaticSummons", count );
			break;

		case 3006:
			Preferences.increment( "noodleSummons", count );
			break;

		case 4006:
			Preferences.increment( "reagentSummons", count );
			break;

		case 5014:
			Preferences.increment( "cocktailSummons", count );
			break;
		}

		return true;
	}

	private static final boolean registerBookRequest( final String urlString )
	{
		Matcher skillMatcher = UseSkillRequest.BOOKID_PATTERN.matcher( urlString );
		if ( !skillMatcher.find() )
		{
			return false;
		}

		String action = skillMatcher.group( 1 );
		int skillId = 0;

		if ( action.equals( "snowcone" ) )
		{
			skillId = SkillDatabase.SNOWCONE;
			Preferences.increment( "snowconeSummons", 1 );
		}
		else if ( action.equals( "hilariousitems" ) )
		{
			skillId = SkillDatabase.HILARIOUS;
			Preferences.increment( "grimoire1Summons", 1 );
		}
		else if ( action.equals( "spencersitems" ) )
		{
			skillId = SkillDatabase.TASTEFUL;
			Preferences.increment( "grimoire2Summons", 1 );
		}
		else if ( action.equals( "candyheart" ) )
		{
			skillId = SkillDatabase.CANDY_HEART;
		}
		else if ( action.equals( "partyfavor" ) )
		{
			skillId = SkillDatabase.PARTY_FAVOR;
		}
		else
		{
			return false;
		}

		Matcher countMatcher = UseSkillRequest.COUNT2_PATTERN.matcher( urlString );
		int count = 1;

		if ( countMatcher.find() )
		{
			count = StringUtilities.parseInt( countMatcher.group( 1 ) );
		}

		if ( SkillDatabase.isLibramSkill( skillId ) )
		{
			int cast = Preferences.getInteger( "libramSummons" );
			int mpCost = SkillDatabase.libramSkillMPConsumption( cast, count );

			if ( mpCost > KoLCharacter.getCurrentMP() )
			{
				return true;
			}

			ResultProcessor.processResult( new AdventureResult( AdventureResult.MP, 0 - mpCost ) );

			Preferences.increment( "libramSummons", count );
			KoLConstants.summoningSkills.sort();
			KoLConstants.usableSkills.sort();
		}

		String skillName = SkillDatabase.getSkillName( skillId );

		RequestLogger.updateSessionLog();
		RequestLogger.updateSessionLog( "cast " + count + " " + skillName );

		return true;
	}
}
