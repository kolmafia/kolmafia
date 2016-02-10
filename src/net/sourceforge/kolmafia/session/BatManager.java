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

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;

import net.sourceforge.kolmafia.objectpool.ItemPool;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.session.ResultProcessor;

public class BatManager
{
	private static final TreeSet<BatUpgrade> upgrades = new TreeSet<BatUpgrade>();

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
		}
	}

	public static void batSedanUpgrade( final int option, final String text )
	{
		BatUpgrade upgrade = BatManager.findOption( BAT_SEDAN_UPGRADES, option );
		if ( upgrade != null )
		{
			BatManager.addUpgrade( upgrade );
		}
	}

	public static void batCavernUpgrade( final int option, final String text )
	{
		BatUpgrade upgrade = BatManager.findOption( BAT_CAVERN_UPGRADES, option );
		if ( upgrade != null )
		{
			BatManager.addUpgrade( upgrade );
		}
	}

	public static final BatUpgrade IMPROVED_3D_BAT_PRINTER = BatManager.findOption( BAT_CAVERN_UPGRADES, "Improved 3-D Bat-Printer" );

	public static boolean hasUpgrade( final BatUpgrade upgrade )
	{
		return BatManager.upgrades.contains( upgrade );
	}

	public static void begin()
	{
		// Preferences.resetToDefault( "batFellowStatus" );
		BatManager.upgrades.clear();
		Preferences.setString( "batmanUpgrades", "" );

		// Clean up inventory
		BatManager.resetItems();

		// Add items that you begin with
		ResultProcessor.processItem( ItemPool.BAT_OOMERANG, 1 );
		ResultProcessor.processItem( ItemPool.BAT_JUTE, 1 );
		ResultProcessor.processItem( ItemPool.BAT_O_MITE, 1 );
	}

	public static void end()
	{
		BatManager.upgrades.clear();
		Preferences.setString( "batmanUpgrades", "" );
		BatManager.resetItems();
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
	public static void parseCharpane( final String responseText )
	{
		if ( !responseText.contains( "You're Batfellow" ) )
		{
			return;
		}
	}

	public static int getTimeLeft()
	{
		// Return minutes left: 0 - 600
		return 600;
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
