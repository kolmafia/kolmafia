/**
 * Copyright (c) 2005-2014, KoLmafia development team
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
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants.Stat;
import net.sourceforge.kolmafia.KoLmafia;

import net.sourceforge.kolmafia.objectpool.FamiliarPool;
import net.sourceforge.kolmafia.objectpool.IntegerPool;

import net.sourceforge.kolmafia.persistence.AdventureDatabase;
import net.sourceforge.kolmafia.persistence.AdventureQueueDatabase;
import net.sourceforge.kolmafia.persistence.AdventureSpentDatabase;
import net.sourceforge.kolmafia.persistence.BountyDatabase;
import net.sourceforge.kolmafia.persistence.MonsterDatabase;
import net.sourceforge.kolmafia.persistence.MonsterDatabase.Element;
import net.sourceforge.kolmafia.persistence.MonsterDatabase.Phylum;
import net.sourceforge.kolmafia.persistence.QuestDatabase;
import net.sourceforge.kolmafia.persistence.QuestDatabase.Quest;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.request.FightRequest;

import net.sourceforge.kolmafia.session.BanishManager;
import net.sourceforge.kolmafia.session.EncounterManager;
import net.sourceforge.kolmafia.session.EncounterManager.EncounterType;
import net.sourceforge.kolmafia.session.EquipmentManager;

import net.sourceforge.kolmafia.utilities.StringUtilities;

public class AreaCombatData
{
	private static double lastDropModifier = 0.0;
	private static double lastDropMultiplier = 0.0;

	private int minHit;
	private int maxHit;
	private int minEvade;
	private int maxEvade;
	private int poison;
	private int jumpChance;

	private final int combats;
	private double weights;

	// Parallel lists: monsters and encounter weighting
	private final List<MonsterData> monsters;
	private final List<MonsterData> superlikelyMonsters;
	private final List<Integer> baseWeightings;
	private final List<Integer> currentWeightings;
	private final List<Integer>	rejection;

	private final String zone;

	// Flags in low-order bits of weightings
	private static final int ASCENSION_ODD = 0x01;
	private static final int ASCENSION_EVEN = 0x02;
	private static final int WEIGHT_SHIFT = 2;

	public AreaCombatData( String zone, final int combats )
	{
		this.zone = zone;
		this.monsters = new ArrayList<MonsterData>();
		this.superlikelyMonsters = new ArrayList<MonsterData>();
		this.baseWeightings = new ArrayList<Integer>();
		this.currentWeightings = new ArrayList<Integer>();
		this.rejection = new ArrayList<Integer>();
		this.combats = combats;
		this.weights = 0.0;
		this.minHit = Integer.MAX_VALUE;
		this.maxHit = 0;
		this.minEvade = Integer.MAX_VALUE;
		this.maxEvade = 0;
		this.poison = Integer.MAX_VALUE;
		this.jumpChance = Integer.MAX_VALUE;
	}

	private void recalculate()
	{
		this.minHit = Integer.MAX_VALUE;
		this.maxHit = 0;
		this.minEvade = Integer.MAX_VALUE;
		this.maxEvade = 0;
		this.jumpChance = 100;

		double weights = 0.0;
		List<Integer> currentWeightings = new ArrayList<Integer>();

		for ( int i = 0; i < this.monsters.size(); ++i )
		{
			// Weighting has two low bits which represent odd or even ascension restriction
			// Strip them out now and restore them at the end
			int baseWeighting = this.baseWeightings.get( i );
			int flags = baseWeighting & 3;
			baseWeighting = baseWeighting >> WEIGHT_SHIFT;

			MonsterData monster = this.getMonster( i );
			String monsterName = monster.getName();

			baseWeighting = AreaCombatData.adjustConditionalWeighting( zone, monsterName, baseWeighting );
			int currentWeighting = baseWeighting;

			// If olfacted, add three to encounter pool
			if ( Preferences.getString( "olfactedMonster" ).equals( monsterName ) &&
				KoLConstants.activeEffects.contains( FightRequest.ONTHETRAIL ) )
			{
				currentWeighting += 3 * baseWeighting;
			}
			// If Nosy Nose sniffed, and is current familiar, add one to encounter pool
			if ( Preferences.getString( "nosyNoseMonster" ).equals( monsterName ) &&
				KoLCharacter.getFamiliar().getId() == FamiliarPool.NOSY_NOSE )
			{
				currentWeighting += baseWeighting;
			}
			// If Staff of the Cream of the Cream jiggle, add two to encounter pool
			if ( Preferences.getString( "_jiggleCreamedMonster" ).equals( monsterName ) )
			{
				currentWeighting += 2 * baseWeighting;
			}
			// If Make Friends used, add three to encounter pool
			if ( Preferences.getString( "makeFriendsMonster" ).equals( monsterName ) )
			{
				currentWeighting += 3 * baseWeighting;
			}

			if ( BanishManager.isBanished( monsterName ) )
			{
				// Banishing reduces number of copies
				currentWeighting -= baseWeighting;
				// If this takes it to zero chance, it's properly banished
				if ( currentWeighting == 0 )
				{
					currentWeighting = -3;
				}
			}
			
			// Not available in current 
			if ( ( flags == ASCENSION_ODD && KoLCharacter.getAscensions() % 2 == 1 ) ||
				( flags == ASCENSION_EVEN && KoLCharacter.getAscensions() % 2 == 0 ) )
			{
				currentWeighting = -2;	// impossible this ascension
			}

			// Temporarily set to 0% chance
			if ( baseWeighting == -4 )
			{
				currentWeighting = -4;
			}

			currentWeightings.add( (currentWeighting << WEIGHT_SHIFT) | flags );

			// Omit currently 0% chance, banished (-3), impossible (-2) and ultra-rare (-1) monsters
			if ( currentWeighting < 0 )
			{
				continue;
			}
			
			weights += currentWeighting * ( 1 - (double) this.getRejection( i ) / 100 );
			this.addMonsterStats( monster );
		}
		this.weights = weights;
		Collections.copy( this.currentWeightings, currentWeightings );

		// Take into account superlikely monsters if they have a non zero chance to appear
		for ( int i = 0; i < this.superlikelyMonsters.size(); ++i )
		{
			MonsterData monster = this.getMonster( i );
			String monsterName = monster.getName();
			if ( AreaCombatData.superlikelyChance( monsterName ) > 0 )
			{
				this.addMonsterStats( monster );
			}
		}
	}

	private void addMonsterStats( MonsterData monster )
	{
		// These include current monster level and Beeosity

		int attack = monster.getAttack();
		if ( attack < this.minEvade )
		{
			this.minEvade = attack;
		}
		if ( attack > this.maxEvade )
		{
			this.maxEvade = attack;
		}

		int defense = monster.getDefense();
		if ( defense < this.minHit )
		{
			this.minHit = defense;
		}
		if ( defense > this.maxHit )
		{
			this.maxHit = defense;
		}
		
		int jumpChance = monster.getJumpChance();
		if ( jumpChance < this.jumpChance )
		{
			this.jumpChance = jumpChance;
		}
	}

	public boolean addMonster( String name )
	{
		int weighting = 1;
		int flags = ASCENSION_EVEN | ASCENSION_ODD;
		int rejection = 0;

		int colon = name.indexOf( ":" );
		if ( colon > 0 )
		{
			String weight = name.substring( colon + 1 ).trim();

			name = name.substring( 0, colon );

			if ( weight.length() == 0 )
			{
				KoLmafia.updateDisplay( "Missing entry after colon for " + name + " in combats.txt." );
				return false;
			}

			int numLength = weight.charAt( 0 ) == '-' ? 2 : 1;

			if ( !StringUtilities.isNumeric( weight.substring( 0, numLength ) ) )
			{
				KoLmafia.updateDisplay( "First entry after colon for " + name + " in combats.txt is not numeric." );
				return false;
			}

			weighting = StringUtilities.parseInt( weight.substring( 0, numLength ) );

			if ( weight.length() > numLength )
			{
				switch ( weight.charAt( numLength ) )
				{
				case 'e':
					flags = ASCENSION_EVEN;
					break;
				case 'o':
					flags = ASCENSION_ODD;
					break;
				case 'r':
					if ( weight.length() > numLength + 1 )
					{
						if ( !StringUtilities.isNumeric( weight.substring( numLength + 1 ) ) )
						{
							KoLmafia.updateDisplay( "Rejection percentatage specified for " + name + " in combats.txt is not numeric." );
							return false;
						}
						rejection = StringUtilities.parseInt( weight.substring( numLength + 1 ) );
					}
					else
					{
						KoLmafia.updateDisplay( "No rejection percentatage specified for " + name + " in combats.txt." );
						return false;
					}
					break;
				default:
					KoLmafia.updateDisplay( "Unknown flag " + weight.charAt( numLength ) + " specified for " + name + " in combats.txt." );
					return false;
				}
			}
		}

		MonsterData monster = MonsterDatabase.findMonster( name, false );
		if ( monster == null )
		{
			return false;
		}

		if ( EncounterManager.isSuperlikelyMonster( monster.getName() ) )
		{
			this.superlikelyMonsters.add( monster );
		}
		else
		{
			this.monsters.add( monster );
			this.baseWeightings.add( IntegerPool.get( (weighting << WEIGHT_SHIFT) | flags ) );
			this.currentWeightings.add( IntegerPool.get( (weighting << WEIGHT_SHIFT) | flags ) );
			this.rejection.add( IntegerPool.get( rejection ) );
		}

		this.poison = Math.min( this.poison, monster.getPoison() );

		// Don't let ultra-rare monsters skew hit and evade numbers -
		// or anything else.
		if ( weighting < 0 )
		{
			return true;
		}

		// Don't let special monsters skew combat percentage numbers
		// or things derived from them, like area-wide item and meat
		// drops. Do include them in hit and evade ("safety") numbers.
		// Assume that the number and total weights of even- and
		// odd-ascension-only monsters are equal.
		if ( weighting > 0 && flags != ASCENSION_ODD )
		{
			this.weights += weighting * ( 1 - (double) rejection / 100 );
		}

		this.addMonsterStats( monster );

		return true;
	}

	/**
	 * Counts the number of monsters in this area that drop the item with the
	 * given ID.
	 *
	 * @param itemId
	 * @return the number of monsters in this area dropping the item
	 */
	public int countMonstersDroppingItem( final int itemId )
	{
		int total = 0;

		Iterator<MonsterData> monsters = this.monsters.iterator();
		while ( monsters.hasNext() )
		{
			MonsterData monster = monsters.next();
			Iterator<AdventureResult> items = monster.getItems().iterator();
			while ( items.hasNext() )
			{
				AdventureResult item = items.next();

				if ( item.getItemId() == itemId )
				{
					total++;
					break;
				}
			}
		}

		Iterator<MonsterData> superlikelyMonsters = this.superlikelyMonsters.iterator();
		while ( superlikelyMonsters.hasNext() )
		{
			MonsterData monster = superlikelyMonsters.next();
			Iterator<AdventureResult> items = monster.getItems().iterator();
			while ( items.hasNext() )
			{
				AdventureResult item = items.next();

				if ( item.getItemId() == itemId )
				{
					total++;
					break;
				}
			}
		}

		return total;
	}

	public int getMonsterCount()
	{
		return this.monsters.size();
	}

	public int getSuperlikelyMonsterCount()
	{
		return this.superlikelyMonsters.size();
	}

	public int getAvailableMonsterCount()
	{
		int count = 0;
		int size = this.monsters.size();
		for ( int i = 0; i < size; ++i )
		{
			int weighting = this.getWeighting( i );
			if ( weighting > 0 )
			{
				count++;
			}
		}
		size = this.superlikelyMonsters.size();
		for ( int i = 0; i < size; ++i )
		{
			MonsterData monster = this.superlikelyMonsters.get( i );
			String monsterName = monster.getName();
			if ( AreaCombatData.superlikelyChance( monsterName ) > 0 )
			{
				count++;
			}
		}
		return count;
	}

	public MonsterData getMonster( final int i )
	{
		return (MonsterData) this.monsters.get( i );
	}

	public MonsterData getSuperlikelyMonster( final int i )
	{
		return (MonsterData) this.superlikelyMonsters.get( i );
	}

	public boolean hasMonster( final MonsterData m )
	{
		if ( m == null )
		{
			return false;
		}
		return this.monsters.contains( m ) || this.superlikelyMonsters.contains( m );
	}

	public int getMonsterIndex( MonsterData monster )
	{
		return this.monsters.indexOf( monster );
	}

	public int getSuperlikelyMonsterIndex( MonsterData monster )
	{
		return this.superlikelyMonsters.indexOf( monster );
	}

	public int getWeighting( final int i )
	{
		int raw = ( (Integer) this.currentWeightings.get( i ) ).intValue();
		if ( ((raw >> (KoLCharacter.getAscensions() & 1)) & 1) == 0 )
		{
			return -2;	// impossible this ascension
		}
		return raw >> WEIGHT_SHIFT;
	}

	public int getRejection( final int i )
	{
		return this.rejection.get( i );
	}

	public double totalWeighting()
	{
		return this.weights;
	}

	public int combats()
	{
		return this.combats;
	}

	public int minHit()
	{
		return this.minHit == Integer.MAX_VALUE ? 0 : this.minHit;
	}

	public int maxHit()
	{
		return this.maxHit;
	}

	public int minEvade()
	{
		return this.minEvade == Integer.MAX_VALUE ? 0 : this.minEvade;
	}

	public int maxEvade()
	{
		return this.maxEvade;
	}

	public int poison()
	{
		return this.poison;
	}

	public boolean willHitSomething()
	{
		int hitStat = EquipmentManager.getAdjustedHitStat();
		return AreaCombatData.hitPercent( hitStat, this.minHit() ) > 0.0;
	}

	public double getAverageML()
	{
		double averageML = 0.0;

		for ( int i = 0; i < this.monsters.size(); ++i )
		{
			int weighting = this.getWeighting( i );

			// Omit impossible (-2), ultra-rare (-1) and special/banished (0) monsters
			if ( weighting < 1 )
			{
				continue;
			}

			MonsterData monster = this.getMonster( i );
			double weight = (double) weighting * ( 1 - (double) this.getRejection( i ) / 100 ) / this.totalWeighting();
			averageML += weight * monster.getAttack();
		}

		double averageSuperlikelyML = 0.0;
		double superlikelyChance = 0.0;
		for ( int i = 0; i < this.superlikelyMonsters.size(); ++i )
		{
			MonsterData monster = this.superlikelyMonsters.get( i );
			String monsterName = monster.getName();
			double chance = AreaCombatData.superlikelyChance( monsterName );
			if ( chance > 0 )
			{
				averageSuperlikelyML += chance * monster.getAttack();
				superlikelyChance += chance;
			}
		}
		
		return averageML * ( 1 - superlikelyChance / 100 ) + averageSuperlikelyML;
	}

	@Override
	public String toString()
	{
		return this.toString( false );
	}

	public String toString( final boolean fullString )
	{
		StringBuffer buffer = new StringBuffer();

		buffer.append( "<html><head>" );
		buffer.append( "<style>" );

		buffer.append( "body { font-family: sans-serif; font-size: " );
		buffer.append( Preferences.getString( "chatFontSize" ) );
		buffer.append( "; }" );

		buffer.append( "</style>" );

		buffer.append( "</head><body>" );

		this.getSummary( buffer, fullString );
		this.getEncounterData( buffer );
		this.getMonsterData( buffer, fullString );

		buffer.append( "</body></html>" );
		return buffer.toString();
	}

	public void getSummary( final StringBuffer buffer, final boolean fullString )
	{
		// Get up-to-date monster stats in area summary
		this.recalculate();

		int moxie = KoLCharacter.getAdjustedMoxie();

		String statName = EquipmentManager.getHitStatType() == Stat.MOXIE ? "Mox" : "Mus";
		int hitstat = EquipmentManager.getAdjustedHitStat();

		double minHitPercent = AreaCombatData.hitPercent( hitstat, this.minHit() );
		double maxHitPercent = AreaCombatData.hitPercent( hitstat, this.maxHit );
		int minPerfectHit = AreaCombatData.perfectHit( hitstat, this.minHit() );
		int maxPerfectHit = AreaCombatData.perfectHit( hitstat, this.maxHit );
		double minEvadePercent = AreaCombatData.hitPercent( moxie, this.minEvade() );
		double maxEvadePercent = AreaCombatData.hitPercent( moxie, this.maxEvade );
		int minPerfectEvade = AreaCombatData.perfectHit( moxie, this.minEvade() );
		int maxPerfectEvade = AreaCombatData.perfectHit( moxie, this.maxEvade );
		int jumpChance = this.jumpChance;
		
		// statGain constants
		double experienceAdjustment = KoLCharacter.getExperienceAdjustment();

		// Area Combat percentage
		double combatFactor = this.areaCombatPercent() / 100.0;

		// Iterate once through monsters to calculate average statGain
		double averageExperience = 0.0;

		for ( int i = 0; i < this.monsters.size(); ++i )
		{
			int weighting = this.getWeighting( i );

			// Omit impossible (-2), ultra-rare (-1) and special/banished (0) monsters
			if ( weighting < 1 )
			{
				continue;
			}

			MonsterData monster = this.getMonster( i );
			double weight = (double) weighting * ( 1 - (double) this.getRejection( i ) / 100 ) / this.totalWeighting();
			averageExperience += weight * (monster.getExperience() + experienceAdjustment);
		}

		double averageSuperlikelyExperience = 0.0;
		double superlikelyChance = 0.0;
		for ( int i = 0; i < this.superlikelyMonsters.size(); ++i )
		{
			MonsterData monster = this.superlikelyMonsters.get( i );
			String monsterName = monster.getName();
			double chance = AreaCombatData.superlikelyChance( monsterName );
			if ( chance > 0 )
			{
				averageSuperlikelyExperience += chance / 100 * (monster.getExperience() + experienceAdjustment);
				superlikelyChance += chance;
			}
		}

		buffer.append( "<b>Hit</b>: " );
		buffer.append( this.getRateString(
			minHitPercent, minPerfectHit, maxHitPercent, maxPerfectHit, statName, fullString ) );

		buffer.append( "<br><b>Evade</b>: " );
		buffer.append( this.getRateString(
			minEvadePercent, minPerfectEvade, maxEvadePercent, maxPerfectEvade, "Mox", fullString ) );
		
		buffer.append( "<br><b>Jump Chance</b>: " );
		buffer.append( jumpChance + "%" );
		
		buffer.append( "<br><b>Combat Rate</b>: " );

		double combatXP = averageSuperlikelyExperience + averageExperience * ( 1 - superlikelyChance / 100 ) * combatFactor;
		if ( this.combats > 0 )
		{
			buffer.append( this.format( superlikelyChance + ( 1 - superlikelyChance / 100 ) * combatFactor * 100.0 ) + "%" );
			buffer.append( "<br><b>Combat XP</b>: " + KoLConstants.FLOAT_FORMAT.format( combatXP ) );
		}
		else if ( this.combats == 0 )
		{
			buffer.append( "0%" );
		}
		else
		{
			buffer.append( "No data" );
		}
	}

	public void getMonsterData( final StringBuffer buffer, final boolean fullString )
	{
		int moxie = KoLCharacter.getAdjustedMoxie();
		int hitstat = EquipmentManager.getAdjustedHitStat();
		double combatFactor = this.areaCombatPercent() / 100.0;
		double superlikelyChance = 0.0;

		for ( int i = 0; i < this.superlikelyMonsters.size(); ++i )
		{
			MonsterData monster = this.superlikelyMonsters.get( i );
			String monsterName = monster.getName();
			double chance = AreaCombatData.superlikelyChance( monsterName );
			buffer.append( "<br><br>" );
			buffer.append( this.getMonsterString(
				this.getSuperlikelyMonster( i ), moxie, hitstat, 0, 0, combatFactor, chance, fullString ) );
			superlikelyChance += chance;
		}

		for ( int i = 0; i < this.monsters.size(); ++i )
		{
			int weighting = this.getWeighting( i );
			int rejection = this.getRejection( i );
			if ( weighting == -2 )
			{
				continue;
			}

			buffer.append( "<br><br>" );
			buffer.append( this.getMonsterString(
				this.getMonster( i ), moxie, hitstat, weighting, rejection, combatFactor, superlikelyChance, fullString ) );
		}
	}

	public void getEncounterData( final StringBuffer buffer )
	{
		String environment = AdventureDatabase.getEnvironment( this.zone );
		if ( environment == null )
		{
			buffer.append( "<br>" );
			buffer.append( "<b>Environment:</b> unknown" );
		}
		else
		{
			buffer.append( "<br>" );
			buffer.append( "<b>Environment:</b> " );
			buffer.append( environment );
		}

		int recommendedStat = AdventureDatabase.getRecommendedStat( this.zone );
		if ( recommendedStat == -1 )
		{
			buffer.append( "<br>" );
			buffer.append( "<b>Recommended Mainstat:</b> unknown" );
		}
		else
		{
			buffer.append( "<br>" );
			buffer.append( "<b>Recommended Mainstat:</b> " );
			buffer.append( recommendedStat );
		}

		if ( KoLCharacter.inRaincore() )
		{
			int waterLevel = KoLCharacter.getWaterLevel();
			Boolean fixed = AdventureDatabase.getWaterLevel( this.zone ) != -1;
			if ( environment == null )
			{
				buffer.append( "<br>" );
				buffer.append( "<b>Water Level:</b> unknown" );
			}
			else if ( recommendedStat == -1 && !fixed )
			{
				buffer.append( "<br>" );
				buffer.append( "<b>Water Level:</b> " );
				buffer.append( waterLevel );
				buffer.append( " (at least)" );
			}
			else
			{
				buffer.append( "<br>" );
				buffer.append( "<b>Water Level:</b> " );
				buffer.append( waterLevel );
			}
		}

		String encounter = EncounterManager.findEncounterForLocation( this.zone, EncounterType.SEMIRARE );

		if ( null != encounter )
		{
			buffer.append( "<br>" );
			buffer.append( "<b>Semi-Rare:</b> " );
			buffer.append( encounter );
		}

		encounter = EncounterManager.findEncounterForLocation( this.zone, EncounterType.CLOVER );

		if ( null != encounter )
		{
			buffer.append( "<br>" );
			buffer.append( "<b>Clover:</b> " );
			buffer.append( encounter );
		}

		encounter = EncounterManager.findEncounterForLocation( this.zone, EncounterType.GLYPH );

		if ( null != encounter )
		{
			buffer.append( "<br>" );
			buffer.append( "<b>Hobo Glyph:</b> " );
			buffer.append( encounter );
		}

		if ( KoLCharacter.inAxecore() )
		{
			encounter = EncounterManager.findEncounterForLocation( this.zone, EncounterType.BORIS );

			if ( null != encounter )
			{
				buffer.append( "<br>" );
				buffer.append( "<b>Clancy:</b> " );
				buffer.append( encounter );
			}
		}

		if ( KoLCharacter.inBadMoon() )
		{
			encounter = EncounterManager.findEncounterForLocation( this.zone, EncounterType.BADMOON );

			if ( null != encounter )
			{
				buffer.append( "<br>" );
				buffer.append( "<b>Badmoon:</b> " );
				buffer.append( encounter );
			}
		}
	}

	private String format( final double percentage )
	{
		return String.valueOf( (int) percentage );
	}

	public double areaCombatPercent()
	{
		// If we don't have the data, pretend it's all combat
		if ( this.combats < 0 )
		{
			return 100.0;
		}

		// Some areas are inherently all combat or no combat
		if ( this.combats == 0 || this.combats == 100 )
		{
			return this.combats;
		}

		double pct = this.combats + KoLCharacter.getCombatRateAdjustment();
		return Math.max( 0.0, Math.min( 100.0, pct ) );
	}

	private String getRateString( final double minPercent, final int minMargin, final double maxPercent,
		final int maxMargin, final String statName, boolean fullString )
	{
		StringBuilder buffer = new StringBuilder();

		buffer.append( this.format( minPercent ) );
		buffer.append( "%/" );

		buffer.append( this.format( maxPercent ) );
		buffer.append( "%" );

		if ( !fullString )
		{
			return buffer.toString();
		}

		buffer.append( " (" );

		buffer.append( statName );

		if ( minMargin < Integer.MAX_VALUE / 2 )
		{
			if ( minMargin >= 0 )
			{
				buffer.append( "+" );
			}
			buffer.append( minMargin );

			buffer.append( "/" );
		}
		else
		{
			buffer.append( " always hit" );
		}

		if ( minMargin < Integer.MAX_VALUE / 2 )
		{
			if ( maxMargin >= 0 )
			{
				buffer.append( "+" );
			}
			buffer.append( maxMargin );
		}

		buffer.append( ")" );
		return buffer.toString();
	}

	private String getMonsterString( final MonsterData monster, final int moxie, final int hitstat,
		final int weighting, final int rejection, final double combatFactor, final double superlikelyChance, final boolean fullString )
	{
		// moxie and hitstat NOT adjusted for monster level, since monster stats now are

		int defense = monster.getDefense();
		double hitPercent = AreaCombatData.hitPercent( hitstat, defense );

		int attack = monster.getAttack();
		double evadePercent = AreaCombatData.hitPercent( moxie, attack );

		int health = monster.getHP();
		double statGain = monster.getExperience();

		StringBuffer buffer = new StringBuffer();

		Element ed = monster.getDefenseElement();
		Element ea = monster.getAttackElement();
		Element element = ed == Element.NONE ? ea : ed;

		Phylum phylum = monster.getPhylum();
		int init = monster.getInitiative();
		int jumpChance = monster.getJumpChance();

		// Color the monster name according to its element
		buffer.append( " <font color=" + AreaCombatData.elementColor( element ) + "><b>" );
		if ( monster.getPoison() < Integer.MAX_VALUE )
		{
			buffer.append( "\u2620 " );
		}
		String name = monster.getName();
		buffer.append( name );
		buffer.append( "</b></font> (" );

		if ( EncounterManager.isSuperlikelyMonster( name ) )
		{
			buffer.append( this.format( superlikelyChance ) + "%" );
		}
		else if ( weighting == -1 )
		{
			buffer.append( "ultra-rare" );
		}
		else if ( weighting == -3 )
		{
			buffer.append( "banished" );
		}
		else if ( weighting == -4 )
		{
			buffer.append( "0%" );
		}
		else if ( weighting == 0 )
		{
			{
				buffer.append( "special" );
			}
		}
		else
		{
			buffer.append( this.format( AdventureQueueDatabase.applyQueueEffects(
				100.0 * combatFactor * ( 1 - superlikelyChance / 100 ) * weighting * ( 1 - (double) rejection / 100 ), monster, this ) ) + "%" );
		}

		buffer.append( ")<br>Hit: <font color=" + AreaCombatData.elementColor( ed ) + ">" );
		buffer.append( this.format( hitPercent ) );
		buffer.append( "%</font>, Evade: <font color=" + AreaCombatData.elementColor( ea ) + ">" );
		buffer.append( this.format( evadePercent ) );
		buffer.append( "%</font>, Jump Chance: <font color=" + AreaCombatData.elementColor( ea ) + ">" );
		buffer.append( this.format( jumpChance ) );
		buffer.append( "%</font><br>Atk: " + attack + ", Def: " + defense );
		buffer.append( ", HP: " + health + ", XP: " + KoLConstants.FLOAT_FORMAT.format( statGain ) );
		buffer.append( "<br>Phylum: " + phylum );
		if ( init == -10000 )
		{
			buffer.append( ", Never wins initiative" );
		}
		else if ( init == 10000 )
		{
			buffer.append( "Always wins initiative" );
		}
		else
		{
			buffer.append( ", Init: " + init );
		}

		if ( fullString )
		{
			this.appendMeatDrop( buffer, monster );
		}

		this.appendItemList( buffer, monster.getItems(), monster.getPocketRates(), fullString );

		String bounty = BountyDatabase.getNameByMonster( monster.getName() );
		if ( bounty != null )
		{
			buffer.append( "<br>" + bounty + " (bounty)" );
		}
		
		return buffer.toString();
	}

	private void appendMeatDrop( final StringBuffer buffer, final MonsterData monster )
	{
		int minMeat = monster.getMinMeat();
		int maxMeat = monster.getMaxMeat();
		int avgMeat = monster.getBaseMeat();
		if ( avgMeat == 0 )
		{
			return;
		}

		double modifier = Math.max( 0.0, ( KoLCharacter.getMeatDropPercentAdjustment() + 100.0 ) / 100.0 );
		buffer.append( "<br>Meat: " + this.format( minMeat * modifier ) + "-" + this.format( maxMeat * modifier ) + " (" + this.format( ( avgMeat ) * modifier ) + " average)" );
	}

	private void appendItemList( final StringBuffer buffer, final List items, final List pocketRates, boolean fullString )
	{
		if ( items.size() == 0 )
		{
			return;
		}

		double itemModifier = AreaCombatData.getDropRateModifier();
		boolean stealing = KoLCharacter.canPickpocket();
		double pocketModifier = ( 100.0 + KoLCharacter.currentNumericModifier( Modifiers.PICKPOCKET_CHANCE ) ) / 100.0;

		for ( int i = 0; i < items.size(); ++i )
		{
			AdventureResult item = (AdventureResult) items.get( i );

			if ( !fullString )
			{
				if ( i == 0 )
				{
					buffer.append( "<br>" );
				}
				else
				{
					buffer.append( ", " );
				}

				buffer.append( item.getName() );
				continue;
			}

			buffer.append( "<br>" );

			double stealRate = Math.min( ( (Double) pocketRates.get( i ) ).doubleValue() * pocketModifier, 1.0 );
			int rawDropRate = item.getCount() >> 16;
			double dropRate = Math.min( rawDropRate * itemModifier, 100.0 );
			double effectiveDropRate = stealRate * 100.0 + ( 1.0 - stealRate ) * dropRate;

			String rateRaw = this.format( rawDropRate );
			String rate1 = this.format( dropRate );
			String rate2 = this.format( effectiveDropRate );

			buffer.append( item.getName() );
			switch ( (char) item.getCount() & 0xFFFF )
			{
			case '0':
				buffer.append( " (unknown drop rate)" );
				break;

			case 'n':
				if ( rawDropRate > 0 )
				{
					buffer.append( " " );
					buffer.append( rate1 );
					buffer.append( "% (no pickpocket)" );
				}
				else
				{
					buffer.append( " (no pickpocket, unknown drop rate)" );
				}
				break;

			case 'c':
				if ( rawDropRate > 0 )
				{
					buffer.append( " " );
					buffer.append( rate1 );
					buffer.append( "% (conditional)" );
				}
				else
				{
					buffer.append( " (conditional, unknown drop rate)" );
				}
				break;
				
			case 'f':
				buffer.append( " " );
				buffer.append( rateRaw );
				buffer.append( "% (no modifiers)" );
				break;

			case 'p':
				if ( stealing && rawDropRate > 0 )
				{
					buffer.append( " " );
					buffer.append( Math.min( rawDropRate * pocketModifier, 100.0 ) );
					buffer.append( "% (pickpocket only)" );
				}
				else
				{
					buffer.append( " (pickpocket only, unknown rate)" );
				}
				break;

			case 'a':
				buffer.append( " (stealable accordion)" );
				break;

			default:
				if ( stealing )
				{
					buffer.append( " " );
					buffer.append( rate2 );
					buffer.append( "% (" );
					buffer.append( this.format( stealRate * 100.0 ) );
					buffer.append( "% steal, " );
					buffer.append( rate1 );
					buffer.append( "% drop)" );
				}
				else
				{
					buffer.append( " " );
					buffer.append( rate1 );
					buffer.append( "%" );
				}
			}
		}
	}

	public static final double getDropRateModifier()
	{
		if ( AreaCombatData.lastDropMultiplier != 0.0 && KoLCharacter.getItemDropPercentAdjustment() == AreaCombatData.lastDropModifier )
		{
			return AreaCombatData.lastDropMultiplier;
		}

		AreaCombatData.lastDropModifier = KoLCharacter.getItemDropPercentAdjustment();
		AreaCombatData.lastDropMultiplier = Math.max( 0.0, ( 100.0 + AreaCombatData.lastDropModifier ) / 100.0 );

		return AreaCombatData.lastDropMultiplier;
	}

	public static final String elementColor( final Element element )
	{
		switch ( element )
		{
			case HOT:
				return "#ff0000";
			case COLD:
				return "#0000ff";
			case STENCH:
				return "#008000";
			case SPOOKY:
				return "#808080";
			case SLEAZE:
				return "#8a2be2";
			case SLIME:
				return "#006400";
			default:
				return "#000000";
		}
	}

	public static final double hitPercent( final int attack, final int defense )
	{
		// ( (Attack - Defense) / 18 ) * 100 + 50 = Hit%
		double percent = 100.0 * ( attack - defense ) / 18 + 50.0;
		if ( percent < 0.0 )
		{
			return 0.0;
		}
		if ( percent > 100.0 )
		{
			return 100.0;
		}
		return percent;
	}

	public static final int perfectHit( final int attack, final int defense )
	{
		return attack - defense - 9;
	}

	public String getZone()
	{
		return zone;
	}

	private static final int adjustConditionalWeighting( String zone, String monster, int weighting )
	{
		// Bossbat can appear on 4th fight, and will always appear on the 8th fight
		if ( zone.equals( "The Boss Bat's Lair" ) )
		{
			int bossTurns = AdventureSpentDatabase.getTurns( zone );
			if ( monster.equals( "Boss Bat" ) )
			{
				return bossTurns > 3 && !QuestDatabase.isQuestLaterThan( Quest.BAT, "step3" ) ? 1 : 0;
			}
			else
			{
				return bossTurns > 7 || QuestDatabase.isQuestLaterThan( Quest.BAT, "step3" ) ? -4 : 1;
			}
		}
		if ( zone.equals( "The Post-Mall" ) )
		{
			int mallTurns = AdventureSpentDatabase.getTurns( zone );
			if ( monster.equals( "sentient ATM" ) )
			{
				return mallTurns == 11 ? 1 : 0;
			}
			else
			{
				return mallTurns == 11 ? -4 : 1;
			}
		}
		return weighting;
	}

	public static final double superlikelyChance( String monster )
	{
		if ( monster.equals( "screambat" ) )
		{
			int turns = AdventureSpentDatabase.getTurns( "Guano Junction" ) +
			            AdventureSpentDatabase.getTurns( "The Batrat and Ratbat Burrow" ) +
			            AdventureSpentDatabase.getTurns( "The Beanbat Chamber" );
			// Appears every 8 turns in relevant zones
			return turns > 0 && ( turns % 8 ) == 0 ? 100.0 : 0.0;
		}
		if ( monster.equals( "modern zmobie" ) )
		{
			// Chance based on initiative
			double chance =  15 + KoLCharacter.getInitiativeAdjustment() / 10;
			return chance < 0 ? 0.0 : chance > 100 ? 100.0 : chance;
		}
		if ( monster.equals( "ninja snowman assassin" ) )
		{
			// Do not appear without positive combat rate
			double combatRate = KoLCharacter.getCombatRateAdjustment();
			if ( combatRate <= 0 )
			{
				return 0;
			}
			// Guaranteed on turns 11, 21, and 31
			int snowmanTurns = AdventureSpentDatabase.getTurns( "Lair of the Ninja Snowmen" );
			if ( snowmanTurns == 10 ||
			     snowmanTurns == 20 ||
			     snowmanTurns == 30 )
			{
				return 100.0;
			}
			double chance = combatRate / 2 + (double) snowmanTurns * 1.5;
			return chance < 0 ? 0.0 : chance > 100 ? 100.0 : chance;
		}
		if ( monster.equals( "mother hellseal" ) )
		{
			double chance = Preferences.getInteger( "_sealScreeches" ) * 10;
			return chance < 0 ? 0.0 : chance > 100 ? 100.0 : chance;
		}
		return 0;
	}
}
