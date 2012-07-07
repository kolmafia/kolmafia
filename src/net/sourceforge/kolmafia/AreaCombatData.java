/**
 * Copyright (c) 2005-2012, KoLmafia development team
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
import java.util.Iterator;
import java.util.List;

import net.sourceforge.kolmafia.objectpool.EffectPool;
import net.sourceforge.kolmafia.objectpool.EffectPool.Effect;
import net.sourceforge.kolmafia.objectpool.IntegerPool;

import net.sourceforge.kolmafia.persistence.MonsterDatabase;

import net.sourceforge.kolmafia.preferences.Preferences;

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

	private final int combats;
	private int weights;

	// Parallel lists: monsters and encounter weighting
	private final List<MonsterData> monsters;
	private final List<Integer> weightings;

	// Flags in low-order bits of weightings
	private static final int ASCENSION_ODD = 0x01;
	private static final int ASCENSION_EVEN = 0x02;
	private static final int WEIGHT_SHIFT = 2;

	public AreaCombatData( final int combats )
	{
		this.monsters = new ArrayList<MonsterData>();
		this.weightings = new ArrayList<Integer>();
		this.combats = combats;
		this.weights = 0;
		this.minHit = Integer.MAX_VALUE;
		this.maxHit = 0;
		this.minEvade = Integer.MAX_VALUE;
		this.maxEvade = 0;
		this.poison = Integer.MAX_VALUE;
	}

	public void recalculate()
	{
		this.minHit = Integer.MAX_VALUE;
		this.maxHit = 0;
		this.minEvade = Integer.MAX_VALUE;
		this.maxEvade = 0;

		for ( int i = 0; i < this.monsters.size(); ++i )
		{
			int weighting = this.getWeighting( i );

			// Omit impossible (-2) and ultra-rare (-1) monsters
			if ( weighting < 0 )
			{
				continue;
			}

			MonsterData monster = this.getMonster( i );
			this.addMonsterStats( monster );
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
	}

	public boolean addMonster( String name )
	{
		int weighting = 1;
		int flags = ASCENSION_EVEN | ASCENSION_ODD;

		int colon = name.indexOf( ":" );
		if ( colon > 0 )
		{
			String weight = name.substring( colon + 1 ).trim();

			if ( StringUtilities.isNumeric( weight ) )
			{
				weighting = StringUtilities.parseInt( weight );
			}
			else
			{
				weighting = StringUtilities.parseInt( weight.substring( 1 ) );
			}

			name = name.substring( 0, colon );

			switch ( weight.charAt( 0 ) )
			{
			case 'e':
				flags = ASCENSION_EVEN;
				break;
			case 'o':
				flags = ASCENSION_ODD;
				break;
			}
		}

		MonsterData monster = MonsterDatabase.findMonster( name, false );
		if ( monster == null )
		{
			return false;
		}

		this.monsters.add( monster );
		this.poison = Math.min( this.poison, monster.getPoison() );
		this.weightings.add( IntegerPool.get( (weighting << WEIGHT_SHIFT) | flags ) );

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
			this.weights += weighting;
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

		Iterator monsters = this.monsters.iterator();
		while ( monsters.hasNext() )
		{
			MonsterData monster = (MonsterData) monsters.next();
			Iterator items = monster.getItems().iterator();
			while ( items.hasNext() )
			{
				AdventureResult item = (AdventureResult) items.next();

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
		return count;
	}

	public MonsterData getMonster( final int i )
	{
		return (MonsterData) this.monsters.get( i );
	}

	public boolean hasMonster( final MonsterData m )
	{
		if ( m == null )
		{
			return false;
		}
		return this.monsters.contains( m );
	}

	public int getWeighting( final int i )
	{
		int raw = ( (Integer) this.weightings.get( i ) ).intValue();
		if ( ((raw >> (KoLCharacter.getAscensions() & 1)) & 1) == 0 )
		{
			return -2;	// impossible this ascension
		}
		return raw >> WEIGHT_SHIFT;
	}

	public int totalWeighting()
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

			// Omit impossible (-2), ultra-rare (-1) and special (0) monsters
			if ( weighting < 1 )
			{
				continue;
			}

			MonsterData monster = this.getMonster( i );
			double weight = (double) weighting / (double) this.weights;
			averageML += weight * monster.getAttack();
		}

		return averageML;
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
		buffer.append( "<br><br>" );
		this.getMonsterData( buffer, fullString );

		buffer.append( "</body></html>" );
		return buffer.toString();
	}

	public void getSummary( final StringBuffer buffer, final boolean fullString )
	{
		// Get up-to-date monster stats in area summary
		this.recalculate();

		int moxie = KoLCharacter.getAdjustedMoxie();

		String statName = EquipmentManager.getHitStatType() == KoLConstants.MOXIE ? "Mox" : "Mus";
		int hitstat = EquipmentManager.getAdjustedHitStat();

		double minHitPercent = AreaCombatData.hitPercent( hitstat, this.minHit() );
		double maxHitPercent = AreaCombatData.hitPercent( hitstat, this.maxHit );
		int minPerfectHit = AreaCombatData.perfectHit( hitstat, this.minHit() );
		int maxPerfectHit = AreaCombatData.perfectHit( hitstat, this.maxHit );
		double minEvadePercent = AreaCombatData.hitPercent( moxie, this.minEvade() );
		double maxEvadePercent = AreaCombatData.hitPercent( moxie, this.maxEvade );
		int minPerfectEvade = AreaCombatData.perfectHit( moxie, this.minEvade() );
		int maxPerfectEvade = AreaCombatData.perfectHit( moxie, this.maxEvade );

		// statGain constants
		double experienceAdjustment = KoLCharacter.getExperienceAdjustment();

		// Area Combat percentage
		double combatFactor = this.areaCombatPercent() / 100.0;

		// Iterate once through monsters to calculate average statGain
		double averageExperience = 0.0;

		for ( int i = 0; i < this.monsters.size(); ++i )
		{
			int weighting = this.getWeighting( i );

			// Omit impossible (-2), ultra-rare (-1) and special (0) monsters
			if ( weighting < 1 )
			{
				continue;
			}

			MonsterData monster = this.getMonster( i );
			double weight = (double) weighting / (double) this.weights;
			averageExperience += weight * (monster.getExperience() + experienceAdjustment);
		}

		buffer.append( "<b>Hit</b>: " );
		buffer.append( this.getRateString(
			minHitPercent, minPerfectHit, maxHitPercent, maxPerfectHit, statName, fullString ) );

		buffer.append( "<br><b>Evade</b>: " );
		buffer.append( this.getRateString(
			minEvadePercent, minPerfectEvade, maxEvadePercent, maxPerfectEvade, "Mox", fullString ) );
		buffer.append( "<br><b>Combat Rate</b>: " );

		if ( this.combats > 0 )
		{
			buffer.append( this.format( combatFactor * 100.0 ) + "%" );
			buffer.append( "<br><b>Combat XP</b>: " + KoLConstants.FLOAT_FORMAT.format( averageExperience * combatFactor ) );
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

		for ( int i = 0; i < this.monsters.size(); ++i )
		{
			int weighting = this.getWeighting( i );
			if ( weighting == -2 )
			{
				continue;
			}

			if ( i > 0 )
			{
				buffer.append( "<br><br>" );
			}

			buffer.append( this.getMonsterString(
				this.getMonster( i ), moxie, hitstat, weighting, combatFactor, fullString ) );
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
		StringBuffer buffer = new StringBuffer();

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

		if ( minMargin >= 0 )
		{
			buffer.append( "+" );
		}
		buffer.append( minMargin );

		buffer.append( "/" );

		if ( maxMargin >= 0 )
		{
			buffer.append( "+" );
		}
		buffer.append( maxMargin );

		buffer.append( ")" );
		return buffer.toString();
	}

	private String getMonsterString( final MonsterData monster, final int moxie, final int hitstat,
		final int weighting, final double combatFactor, final boolean fullString )
	{
		// moxie and hitstat NOT adjusted for monster level, since monster stats now are

		int defense = monster.getDefense();
		double hitPercent = AreaCombatData.hitPercent( hitstat, defense );

		int attack = monster.getAttack();
		double evadePercent = AreaCombatData.hitPercent( moxie, attack );

		int health = monster.getHP();
		double statGain = monster.getExperience();

		StringBuffer buffer = new StringBuffer();

		int ed = monster.getDefenseElement();
		int ea = monster.getAttackElement();
		int element = ed == MonsterDatabase.NONE ? ea : ed;

		// Color the monster name according to its element
		buffer.append( " <font color=" + AreaCombatData.elementColor( element ) + "><b>" );
		if ( monster.getPoison() < Integer.MAX_VALUE )
		{
			buffer.append( "\u2620 " );
		}
		buffer.append( monster.getName() );
		buffer.append( "</b></font> (" );

		if ( weighting == -1 )
		{
			buffer.append( "ultra-rare" );
		}
		else if ( weighting == 0 )
		{
			buffer.append( "special" );
		}
		else
		{
			buffer.append( this.format( 100.0 * combatFactor * weighting / this.weights ) + "%" );
		}

		buffer.append( ")<br>Hit: <font color=" + AreaCombatData.elementColor( ed ) + ">" );
		buffer.append( this.format( hitPercent ) );
		buffer.append( "%</font>, Evade: <font color=" + AreaCombatData.elementColor( ea ) + ">" );
		buffer.append( this.format( evadePercent ) );
		buffer.append( "%</font><br>HP: " + health + ", XP: " + KoLConstants.FLOAT_FORMAT.format( statGain ) );

		if ( fullString )
		{
			this.appendMeatDrop( buffer, monster );
		}

		this.appendItemList( buffer, monster.getItems(), monster.getPocketRates(), fullString );

		return buffer.toString();
	}

	private void appendMeatDrop( final StringBuffer buffer, final MonsterData monster )
	{
		int minMeat = monster.getMinMeat();
		int maxMeat = monster.getMaxMeat();
		if ( minMeat == 0 && maxMeat == 0 )
		{
			return;
		}

		double modifier = Math.max( 0.0, ( KoLCharacter.getMeatDropPercentAdjustment() + 100.0 ) / 100.0 );
		buffer.append( "<br>Meat: " + this.format( minMeat * modifier ) + "-" + this.format( maxMeat * modifier ) + " (" + this.format( ( minMeat + maxMeat ) * modifier / 2.0 ) + " average)" );
	}

	private void appendItemList( final StringBuffer buffer, final List items, final List pocketRates, boolean fullString )
	{
		if ( items.size() == 0 )
		{
			return;
		}

		double itemModifier = AreaCombatData.getDropRateModifier();
		boolean stealing = KoLCharacter.isMoxieClass() || KoLConstants.activeEffects.contains( EffectPool.get( Effect.FORM_OF_BIRD ) );
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

			case 'b':
				buffer.append( " (bounty)" );
				break;

			case 'n':
				buffer.append( " " );
				buffer.append( rate1 );
				buffer.append( "% (no pickpocket)" );
				break;

			case 'c':
				buffer.append( " " );
				buffer.append( rate1 );
				buffer.append( "% (conditional)" );
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
					buffer.append( " (pickpocket only)" );
				}
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

	public static final String elementColor( final int element )
	{
		if ( element == MonsterDatabase.HEAT )
		{
			return "#ff0000";
		}
		if ( element == MonsterDatabase.COLD )
		{
			return "#0000ff";
		}
		if ( element == MonsterDatabase.STENCH )
		{
			return "#008000";
		}
		if ( element == MonsterDatabase.SPOOKY )
		{
			return "#808080";
		}
		if ( element == MonsterDatabase.SLEAZE )
		{
			return "#8a2be2";
		}
		if ( element == MonsterDatabase.SLIME )
		{
			return "#006400";
		}

		return "#000000";
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
}
