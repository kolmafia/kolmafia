/**
 * Copyright (c) 2005-2015, KoLmafia development team
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

package net.sourceforge.kolmafia.session;

import java.util.TreeSet;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;

import net.sourceforge.kolmafia.objectpool.ItemPool;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.session.ResultProcessor;

import net.sourceforge.kolmafia.utilities.StringUtilities;

public class BatManager
{
	public static final int BASE_BAT_HEALTH = 30;
	public static final int BASE_BAT_PUNCH = 5;
	public static final int BASE_BAT_KICK = 5;
	public static final int BASE_BAT_ARMOR = 0;
	public static final int BASE_BAT_BULLETPROOFING = 0;
	public static final int BASE_BAT_SPOOKY_RESISTANCE = 0;
	public static final int BASE_BAT_HEAT_RESISTANCE = 0;
	public static final int BASE_BAT_STENCH_RESISTANCE = 0;

	private static final TreeSet<BatUpgrade> upgrades = new TreeSet<BatUpgrade>();
	private static final BatStats stats = new BatStats();
	private static int BatMinutes = 0;

	private static final AdventureResult[] ITEMS =
	{
		// Raw materials for Bat-Fabricator
		ItemPool.get( ItemPool.HIGH_GRADE_METAL, 1 ),
		ItemPool.get( ItemPool.HIGH_TENSILE_STRENGTH_FIBERS, 1 ),
		ItemPool.get( ItemPool.HIGH_GRADE_EXPLOSIVES, 1 ),

		// Items from Bat-Fabricator
		ItemPool.get( ItemPool.BAT_OOMERANG, 1 ),
		ItemPool.get( ItemPool.BAT_JUTE, 1 ),
		ItemPool.get( ItemPool.BAT_O_MITE, 1 ),

		// Currency & items from Orphanage
		ItemPool.get( ItemPool.KIDNAPPED_ORPHAN, 1 ),
		ItemPool.get( ItemPool.CONFIDENCE_BUILDING_HUG, 1 ),
		ItemPool.get( ItemPool.EXPLODING_KICKBALL, 1 ),

		// Currency & items from ChemiCorp
		ItemPool.get( ItemPool.DANGEROUS_CHEMICALS, 1 ),
		ItemPool.get( ItemPool.EXPERIMENTAL_GENE_THERAPY, 1 ),
		ItemPool.get( ItemPool.ULTRACOAGULATOR, 1 ),

		// Currency & items from GotPork P.D.
		ItemPool.get( ItemPool.INCRIMINATING_EVIDENCE, 1 ),
		ItemPool.get( ItemPool.SELF_DEFENSE_TRAINING, 1 ),
		ItemPool.get( ItemPool.FINGERPRINT_DUSTING_KIT, 1 ),

		// Bat-Suit upgrade
		ItemPool.get( ItemPool.BAT_AID_BANDAGE, 1 ),

		// Bat-Sedan upgrade
		ItemPool.get( ItemPool.BAT_BEARING, 1 ),

		// Bat-Cavern upgrade
		ItemPool.get( ItemPool.GLOB_OF_BAT_GLUE, 1 ),
	};

	// Bat-Suit Upgrades: whichchoice = 1137
	private static final BatUpgrade[] BAT_SUIT_UPGRADES =
	{
		new BatUpgrade( 1, "Hardened Knuckles", "Doubles the damage of Bat-Punches" ),
		new BatUpgrade( 2, "Steel-Toed Bat-Boots", "Doubles the damage of Bat-Kicks" ),
		new BatUpgrade( 3, "Extra-Swishy Cloak", "Lets you strike first in combats" ),
		new BatUpgrade( 4, "Pec-Guards", "Reduces the damage you take from melee attacks" ),
		new BatUpgrade( 5, "Kevlar Undergarments", "Reduces the damage you take from gunshots" ),
		new BatUpgrade( 6, "Improved Cowl Optics", "Lets you find more items and hidden things" ),
		new BatUpgrade( 7, "Asbestos Lining", "Provides resistance to Hot damage" ),
		new BatUpgrade( 8, "Utility Belt First Aid Kit", "Contains bandages (in theory)" ),
	};

	// Bat-Sedan Upgrades: whichchoice = 1138
	private static final BatUpgrade[] BAT_SEDAN_UPGRADES =
	{
		new BatUpgrade( 1, "Rocket Booster", "Reduce travel time by 5 minutes" ),
		new BatUpgrade( 2, "Glove Compartment First-Aid Kit", "Restore your health on the go!" ),
		new BatUpgrade( 3, "Street Sweeper", "Gather evidence as you drive around" ),
		new BatUpgrade( 4, "Advanced Air Filter", "Gather dangerous chemicals as you drive around" ),
		new BatUpgrade( 5, "Orphan Scoop", "Rescue loose orphans as you drive around" ),
		new BatUpgrade( 6, "Spotlight", "Helps you find your way through villains' lairs" ),
		new BatUpgrade( 7, "Bat-Freshener", "Provides resistance to Stench damage" ),
		new BatUpgrade( 8, "Loose Bearings", "Bearings will periodically fall out of the car." ),
	};

	// Bat-Cavern Upgrades: whichchoice = 1139
	private static final BatUpgrade[] BAT_CAVERN_UPGRADES =
	{
		new BatUpgrade( 1, "Really Long Winch", "Traveling to the Bat-Cavern is instantaneous" ),
		new BatUpgrade( 2, "Improved 3-D Bat-Printer", "Reduce materials cost in the Bat-Fabricator" ),
		new BatUpgrade( 3, "Transfusion Satellite", "Remotely restore some of your HP after fights" ),
		new BatUpgrade( 4, "Surveillance Network", "Fights take 1 minute less" ),
		new BatUpgrade( 5, "Blueprints Database", "Make faster progress through villain lairs" ),
		new BatUpgrade( 7, "Snugglybear Nightlight", "Provides resistance to Spooky damage" ),
		new BatUpgrade( 8, "Glue Factory", "An automated mail-order glue factory" ),
	};

	private static BatUpgrade findOption( final BatUpgrade[] upgrades, final int option )
	{
		for ( BatUpgrade upgrade : upgrades )
		{
			if ( upgrade.option == option )
			{
				return upgrade;
			}
		}
		return null;
	}

	private static BatUpgrade findOption( final BatUpgrade[] upgrades, final String name )
	{
		for ( BatUpgrade upgrade : upgrades )
		{
			if ( upgrade.name.equals( name ) )
			{
				return upgrade;
			}
		}
		return null;
	}

	private static void addUpgrade( final BatUpgrade newUpgrade )
	{
		BatManager.upgrades.add( newUpgrade );

		StringBuilder buffer = new StringBuilder();
		String separator = "";
		for ( BatUpgrade upgrade : BatManager.upgrades )
		{
			buffer.append( separator );
			buffer.append( upgrade.name );
			separator = ";";
		}
		Preferences.setString( "batmanUpgrades", buffer.toString() );
	}

	public static void batSuitUpgrade( final int option, final String text )
	{
		BatUpgrade upgrade = BatManager.findOption( BAT_SUIT_UPGRADES, option );
		if ( upgrade != null )
		{
			BatManager.addUpgrade( upgrade );
			if ( text.equals( "Hardened Knuckles" ) )
			{
				BatManager.stats.set( "Bat-Punch Multiplier", 2 );
			}
			else if ( text.equals( "Steel-Toed Bat-Boots" ) )
			{
				BatManager.stats.set( "Bat-Kick Multiplier", 2 );
			}
			else if ( text.equals( "Pec-Guards" ) )
			{
				BatManager.stats.increment( "Bat-Armor", 3 );
			}
			else if ( text.equals( "Kevlar Undergarments" ) )
			{
				BatManager.stats.increment( "Bat-Bulletproofing", 3 );
			}
			else if ( text.equals( "Asbestos Lining" ) )
			{
				BatManager.stats.increment( "Bat-Heat Resistance", 10 );
			}
		}
	}

	public static void batSedanUpgrade( final int option, final String text )
	{
		BatUpgrade upgrade = BatManager.findOption( BAT_SEDAN_UPGRADES, option );
		if ( upgrade != null )
		{
			BatManager.addUpgrade( upgrade );
			if ( text.equals( "Bat-Freshener" ) )
			{
				BatManager.stats.increment( "Bat-Stench Resistance", 10 );
			}
		}
	}

	public static void batCavernUpgrade( final int option, final String text )
	{
		BatUpgrade upgrade = BatManager.findOption( BAT_CAVERN_UPGRADES, option );
		if ( upgrade != null )
		{
			BatManager.addUpgrade( upgrade );
			if ( text.equals( "Snugglybear Nightlight" ) )
			{
				BatManager.stats.increment( "Bat-Spooky Resistance", 10 );
			}
		}
	}

	public static final BatUpgrade IMPROVED_3D_BAT_PRINTER = BatManager.findOption( BAT_CAVERN_UPGRADES, "Improved 3-D Bat-Printer" );

	public static boolean hasUpgrade( final BatUpgrade upgrade )
	{
		return BatManager.upgrades.contains( upgrade );
	}

	private static void reset( final boolean active )
	{
		// Zero out Time until Gotpork City explodes
		Preferences.setInteger( "batmanTimeLeft", 0 );
		BatManager.BatMinutes = 0;

		// Reset Bat-Stats
		Preferences.setString( "batmanStats", "" );
		BatManager.stats.reset( active );
		
		// Clear Bat-Upgrades
		Preferences.setString( "batmanUpgrades", "" );
		BatManager.upgrades.clear();

		// Clean up inventory
		BatManager.resetItems();
	}

	public static void begin()
	{
		BatManager.reset( true );

		// Add items that you begin with
		ResultProcessor.processItem( ItemPool.BAT_OOMERANG, 1 );
		ResultProcessor.processItem( ItemPool.BAT_JUTE, 1 );
		ResultProcessor.processItem( ItemPool.BAT_O_MITE, 1 );

		// You start with 10 h. 0 m.
		Preferences.setInteger( "batmanTimeLeft", 600 );
		BatManager.BatMinutes = 600;
	}

	public static void end()
	{
		BatManager.reset( false );
	}

	private static void resetItems()
	{
		for ( AdventureResult item : BatManager.ITEMS )
		{
			int count = item.getCount( KoLConstants.inventory );
			if ( count > 0 )
			{
				AdventureResult result = item.getInstance( -count );
				AdventureResult.addResultToList( KoLConstants.inventory, result );
				AdventureResult.addResultToList( KoLConstants.tally, result );
			}
		}
	}

	// <td><img src=http://images.kingdomofloathing.com/itemimages/watch.gif alt='Time until Gotpork City explodes' title='Time until Gotpork City explodes'></td><td valign=center><font face=arial>10 h. 0 m.</td>
	// <td><img src=http://images.kingdomofloathing.com/itemimages/watch.gif alt='Time until Gotpork City explodes' title='Time until Gotpork City explodes'></td><td valign=center><font face=arial>8 m.</td>
	public static final Pattern TIME_PATTERN = Pattern.compile( "Time until Gotpork City explodes.*?<font face=arial>(?:<font color=red>)?(?:([\\d]+) h. )?([\\d]+) m.<" );

	public static void parseCharpane( final String responseText )
	{
		if ( !responseText.contains( "You're Batfellow" ) )
		{
			return;
		}

		Matcher timeMatcher = BatManager.TIME_PATTERN.matcher( responseText );
		if ( timeMatcher.find() )
		{
			String hourString = timeMatcher.group( 1 );
			String minuteString = timeMatcher.group( 2 );
			int hours = hourString == null ? 0 : StringUtilities.parseInt( hourString );
			int minutes = StringUtilities.parseInt( minuteString );
			BatManager.BatMinutes = ( hours * 60 ) + minutes;
			Preferences.setInteger( "batmanTimeLeft", BatManager.BatMinutes );
		}
		// Current Bat-Tasks:
		// 
		// Learn the Jokester's access code:<br>&nbsp;&nbsp;&nbsp;<font size=+2><b>*********</b></font>
		// Track down Kudzu<font size=1><br>&nbsp;&nbsp;(0% progress)</font>
		// Track down Mansquito<font size=1><br>&nbsp;&nbsp;(0% progress)</font>
		// Track down Miss Graves<font size=1><br>&nbsp;&nbsp;(0% progress)</font>
		// Track down The Plumber<font size=1><br>&nbsp;&nbsp;(0% progress)</font>
		// Track down The Author<font size=1><br>&nbsp;&nbsp;(0% progress)</font>
		// Track down The Mad-Libber<font size=1><br>&nbsp;&nbsp;(0% progress)</font>
		// Track down Doc Clock<font size=1><br>&nbsp;&nbsp;(0% progress)</font>
		// Track down Mr. Burns<font size=1><br>&nbsp;&nbsp;(0% progress)</font>
		// Track down The Inquisitor<font size=1><br>&nbsp;&nbsp;(0% progress)
		// 
		// Defeat Kudzu
	}

	public static void gainItem( final AdventureResult item )
	{
		switch ( item.getItemId() )
		{
		case ItemPool.EXPERIMENTAL_GENE_THERAPY:
			BatManager.stats.increment( "Maximum Bat-Health", 10 );
			break;

		case ItemPool.SELF_DEFENSE_TRAINING:
			BatManager.stats.increment( "Bat-Armor", 1 );
			break;

		case ItemPool.CONFIDENCE_BUILDING_HUG:
			BatManager.stats.increment( "Bat-Punch Modifier", 1 );
			BatManager.stats.increment( "Bat-Kick Modifier", 1 );
			break;
		}
	}

	public static void wonFight( final String monsterName, final String responseText )
	{
		if ( monsterName.equals( "giant mosquito" ) )
		{
			if ( responseText.contains( "(+3 Maximum Bat-Health)" ) )
			{
				BatManager.stats.increment( "Maximum Bat-Health", 3 );
			}
			return;
		}
		if ( monsterName.equals( "vicious plant creature" ) )
		{
			if ( responseText.contains( "(+1 Bat-Health regeneration per fight)" ) )
			{
				BatManager.stats.increment( "Bat-Health Regeneration", 1 );
			}
			return;
		}
		if ( monsterName.equals( "walking skeleton" ) )
		{
			if ( responseText.contains( "(+1 Bat-Armor)" ) )
			{
				BatManager.stats.increment( "Bat-Armor", 1 );
			}
			return;
		}
		if ( monsterName.equals( "former guard" ) )
		{
			if ( responseText.contains( "(+1 Bat-Bulletproofing)" ) )
			{
				BatManager.stats.increment( "Bat-Bulletproofing", 1 );
			}
			return;
		}
		if ( monsterName.equals( "plumber's helper" ) )
		{
			if ( responseText.contains( "(+10% Bat-Stench Resistance)" ) )
			{
				BatManager.stats.increment( "Bat-Stench Resistance", 10 );
			}
			return;
		}
		if ( monsterName.equals( "very [adjective] henchman" ) )
		{
			if ( responseText.contains( "(+10% Bat-Spooky Resistance)" ) )
			{
				BatManager.stats.increment( "Bat-Spooky Resistance", 10 );
			}
			return;
		}
		if ( monsterName.equals( "burner" ) )
		{
			if ( responseText.contains( "(+10% Bat-Heat Resistance)" ) )
			{
				BatManager.stats.increment( "Bat-Heat Resistance", 10 );
			}
			return;
		}

		// (+3% Bat-Progress)
		// (+10 Bat-Minutes)
	}

	public static int getTimeLeft()
	{
		// Return minutes left: 0 - 600
		return BatManager.BatMinutes;
	}

	public static String getTimeLeftString()
	{
		int minutes = BatManager.getTimeLeft();
		StringBuilder buffer = new StringBuilder();
		int hours = minutes / 60;
		if ( hours > 0 )
		{
			buffer.append( String.valueOf( hours ) );
			buffer.append( " h. " );
			minutes = minutes % 60;
		}
		buffer.append( String.valueOf( minutes ) );
		buffer.append( " m." );
		return buffer.toString();
	}

	private static class BatStats
	{
		// Bat-Health
		public int BatHealth = BASE_BAT_HEALTH;
		public int MaximumBatHealth = BASE_BAT_HEALTH;
		public int BatHealthRegeneration = 0;

		// Bat-Punch
		public int BatPunch = BASE_BAT_PUNCH;
		public int BatPunchModifier = 0;
		public int BatPunchMultiplier = 1;

		// Bat-Kick
		public int BatKick = BASE_BAT_KICK;
		public int BatKickModifier = 0;
		public int BatKickMultiplier = 1;

		// Bat-Armor
		public int BatArmor = BASE_BAT_ARMOR;

		// Bat-Bulletproofing
		public int BatBulletproofing = BASE_BAT_BULLETPROOFING;

		// Bat-Spooky resistance
		public int BatSpookyResistance = BASE_BAT_SPOOKY_RESISTANCE;

		// Bat-Heat resistance
		public int BatHeatResistance = BASE_BAT_HEAT_RESISTANCE;

		// Bat-Stench resistance
		public int BatStenchResistance = BASE_BAT_STENCH_RESISTANCE;

		public String stringform = "";

		public BatStats()
		{
			this.reset( false );
		}

		public void reset( final boolean active )
		{
			this.BatHealth = BASE_BAT_HEALTH;
			this.MaximumBatHealth = BASE_BAT_HEALTH;
			this.BatHealthRegeneration = 0;
			this.BatPunch = BASE_BAT_PUNCH;
			this.BatPunchModifier = 0;
			this.BatPunchMultiplier = 1;
			this.BatKick = BASE_BAT_KICK;
			this.BatKickModifier = 0;
			this.BatKickMultiplier = 1;
			this.BatArmor = BASE_BAT_ARMOR;
			this.BatBulletproofing = BASE_BAT_BULLETPROOFING;
			this.BatSpookyResistance = BASE_BAT_SPOOKY_RESISTANCE;
			this.BatHeatResistance = BASE_BAT_HEAT_RESISTANCE;
			this.BatStenchResistance = BASE_BAT_STENCH_RESISTANCE;
			this.calculateStringform( active );
		}

		public int get( final String name )
		{
			if ( name.equals( "Bat-Health" ) )
			{
				return this.BatHealth;
			}
			if ( name.equals( "Maximum Bat-Health" ) )
			{
				return this.MaximumBatHealth;
			}
			if ( name.equals( "Bat-Health Regeneration" ) )
			{
				return this.BatHealthRegeneration;
			}
			if ( name.equals( "Bat-Punch" ) )
			{
				return this.BatPunch;
			}
			if ( name.equals( "Bat-Punch Modifier" ) )
			{
				return this.BatPunchModifier;
			}
			if ( name.equals( "Bat-Punch Multiplier" ) )
			{
				return this.BatPunchMultiplier;
			}
			if ( name.equals( "Bat-Kick" ) )
			{
				return this.BatKick;
			}
			if ( name.equals( "Bat-Kick Modifier" ) )
			{
				return this.BatKickModifier;
			}
			if ( name.equals( "Bat-Kick Multiplier" ) )
			{
				return this.BatKickMultiplier;
			}
			if ( name.equals( "Bat-Armor" ) )
			{
				return this.BatArmor;
			}
			if ( name.equals( "Bat-Bulletproofing" ) )
			{
				return this.BatBulletproofing;
			}
			if ( name.equals( "Bat-Spooky Resistance" ) )
			{
				return this.BatSpookyResistance;
			}
			if ( name.equals( "Bat-Heat Resistance" ) )
			{
				return this.BatHeatResistance;
			}
			if ( name.equals( "Bat-Stench Resistance" ) )
			{
				return this.BatStenchResistance;
			}
			return 0;
		}

		public int set( final String name, final int value )
		{
			int retval = 0;
			if ( name.equals( "Bat-Health" ) )
			{
				retval = this.BatHealth = value;
			}
			else if ( name.equals( "Maximum Bat-Health" ) )
			{
				retval = this.MaximumBatHealth = value;
			}
			else if ( name.equals( "Bat-Health Regeneration" ) )
			{
				retval = this.BatHealthRegeneration = value;
			}
			else if ( name.equals( "Bat-Punch" ) )
			{
				retval = this.BatPunch = value;
			}
			else if ( name.equals( "Bat-Punch Modifier" ) )
			{
				retval = this.BatPunchModifier = value;
			}
			else if ( name.equals( "Bat-Punch Multiplier" ) )
			{
				retval = this.BatPunchMultiplier = value;
			}
			else if ( name.equals( "Bat-Kick" ) )
			{
				retval = this.BatKick = value;
			}
			else if ( name.equals( "Bat-Kick Modifier" ) )
			{
				retval = this.BatKickModifier = value;
			}
			else if ( name.equals( "Bat-Kick Multiplier" ) )
			{
				retval = this.BatKickMultiplier = value;
			}
			else if ( name.equals( "Bat-Armor" ) )
			{
				retval = this.BatArmor = value;
			}
			else if ( name.equals( "Bat-Bulletproofing" ) )
			{
				retval = this.BatBulletproofing = value;
			}
			else if ( name.equals( "Bat-Spooky Resistance" ) )
			{
				retval = this.BatSpookyResistance = value;
			}
			else if ( name.equals( "Bat-Heat Resistance" ) )
			{
				retval = this.BatHeatResistance = value;
			}
			else if ( name.equals( "Bat-Stench Resistance" ) )
			{
				retval = this.BatStenchResistance = value;
			}
			this.calculateStringform( true );
			return retval;
		}

		public int increment( final String name, final int delta )
		{
			return this.set( name, this.get( name ) + delta );
		}

		private void appendStat( StringBuilder buffer, String tag, int stat )
		{
			if ( buffer.length() > 0 )
			{
				buffer.append( ";" );
			}
			buffer.append( tag );
			buffer.append( "=" );
			buffer.append( String.valueOf( stat ) );
		}

		private void calculateStringform( final boolean active )
		{
			StringBuilder buffer = new StringBuilder();

			this.appendStat( buffer, "Bat-Health", this.BatHealth );
			this.appendStat( buffer, "Maximum Bat-Health", this.MaximumBatHealth );
			this.appendStat( buffer, "Bat-Health Regeneration", this.BatHealthRegeneration );
			this.appendStat( buffer, "Bat-Punch", this.BatPunch );
			this.appendStat( buffer, "Bat-Punch Modifier", this.BatPunchModifier );
			this.appendStat( buffer, "Bat-Punch Multiplier", this.BatPunchMultiplier );
			this.appendStat( buffer, "Bat-Kick", this.BatKick );
			this.appendStat( buffer, "Bat-Kick Modifier", this.BatKickModifier );
			this.appendStat( buffer, "Bat-Kick Multiplier", this.BatKickMultiplier );
			this.appendStat( buffer, "Bat-Armor", this.BatArmor );
			this.appendStat( buffer, "Bat-Bulletproofing", this.BatBulletproofing );
			this.appendStat( buffer, "Bat-Spooky Resistance", this.BatSpookyResistance );
			this.appendStat( buffer, "Bat-Heat Resistance", this.BatHeatResistance );
			this.appendStat( buffer, "Bat-Stench Resistance", this.BatStenchResistance );

			this.stringform = buffer.toString();

			if ( active )
			{
				Preferences.setString( "batmanStats", this.stringform );
			}
		}

		public String toString()
		{
			return this.stringform;
		}
	}

	private static class BatUpgrade
		implements Comparable<BatUpgrade>
	{
		public final int option;
		public final String name;
		public final String description;

		public BatUpgrade( final int option, final String name, final String description )
		{
			this.option = option;
			this.name = name;
			this.description = description;
		}

		public int compareTo( final BatUpgrade that )
		{
			return this.name.compareTo( that.name );
		}

		public String toString()
		{
			return this.name;
		}
	}
}
