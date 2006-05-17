/**
 * Copyright (c) 2006, KoLmafia development team
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

import java.util.List;
import java.util.ArrayList;
import net.java.dev.spellcast.utilities.LockableListModel;

public class AreaCombatData implements KoLConstants
{
	private int minHit;
	private int maxHit;
	private int minEvade;
	private int maxEvade;

	private int combats;
	private int weights;

	// Parallel lists: monsters and encounter weighting
	private List monsters;
	private List weightings;

	public AreaCombatData( int combats )
	{
		this.monsters = new ArrayList();
		this.weightings = new ArrayList();
		this.combats = combats;
		this.weights = 0;
		this.minHit = Integer.MAX_VALUE;
		this.maxHit = 0;
		this.minEvade = Integer.MAX_VALUE;
		this.maxEvade = 0;
	}

	public boolean addMonster( String name )
	{
		int weighting = 1;

		int colon = name.indexOf( ":" );
		if ( colon > 0 )
		{
			weighting = Integer.parseInt( name.substring( colon + 1 ).trim() );
			name = name.substring( 0, colon );
		}

		MonsterDatabase.Monster monster = MonsterDatabase.findMonster( name );
		if ( monster == null )
			return false;

		monsters.add( monster );
		weightings.add( new Integer( weighting ) );

		// Don't let ultra-rare monsters skew hit and evade numbers -
		// or anything else.
		if ( weighting < 0 )
			return true;

		// Don't let one-time monsters skew combat percentage numbers
		// or things derived from them, like area-wide item and meat
		// drops. Do include them in hit and evade ("safety") numbers.
		if ( weighting > 0 )
			weights += weighting;

		int attack = monster.getAttack();
		if ( attack < minEvade )
			minEvade = attack;
		if ( attack > maxEvade )
			maxEvade = attack;

		int defense = monster.getDefense();
		if ( defense < minHit )
			minHit = defense;
		if ( defense > maxHit )
			maxHit = defense;

		return true;
	}

	public MonsterDatabase.Monster getMonster( int i )
	{	return (MonsterDatabase.Monster) monsters.get(i);
	}

	public int getWeighting( int i )
	{	return ((Integer)weightings.get(i)).intValue();
	}

	public int combats()
	{	return combats;
	}

	public int minHit()
	{	return minHit;
	}

	public int maxHit()
	{	return maxHit;
	}

	public int minEvade()
	{	return minEvade;
	}

	public int maxEvade()
	{	return maxEvade;
	}

	public String toString()
	{
		boolean ranged = KoLCharacter.rangedWeapon();

		int ml = KoLCharacter.getMonsterLevelAdjustment();
		int moxie = KoLCharacter.getAdjustedMoxie() - ml;
		int hitstat = ranged ? moxie : ( KoLCharacter.getAdjustedMuscle() - ml );

		double minHitPercent = hitPercent( hitstat, minHit );
		double maxHitPercent = hitPercent( hitstat, maxHit );
		int minPerfectHit = perfectHit( hitstat, minHit );
		int maxPerfectHit = perfectHit( hitstat, maxHit );
		double minEvadePercent = hitPercent( moxie, minEvade );
		double maxEvadePercent = hitPercent( moxie, maxEvade );
		int minPerfectEvade = perfectHit( moxie, minEvade );
		int maxPerfectEvade = perfectHit( moxie, maxEvade );

		// XP constants
		FamiliarData familiar = KoLCharacter.getFamiliar();
		double xpAdjustment = KoLCharacter.getFixedXPAdjustment();

		// Area Combat percentage
		double combatFactor = areaCombatPercent() / 100.0;

		// Iterate once through monsters to calculate average XP
		double averageXP = 0.0;

		for ( int i = 0; i < monsters.size(); ++i )
		{
			int weighting = getWeighting( i );
			// Omit ultra-rare (-1) and one-time (0) monsters
			if ( weighting < 1 )
				continue;
			MonsterDatabase.Monster monster = getMonster( i );
			double weight = (double)weighting / (double)weights;
			averageXP += weight * monster.getAdjustedXP( xpAdjustment, ml,  familiar );
		}

		StringBuffer buffer = new StringBuffer();

		buffer.append( "<html><b>Hit</b>: " );
		buffer.append( getRateString( minHitPercent, minPerfectHit, maxHitPercent, maxPerfectHit, ranged ) );

		buffer.append( "<br><b>Evade</b>: " );
		buffer.append( getRateString( minEvadePercent, minPerfectEvade, maxEvadePercent, maxPerfectEvade, true ) );
		buffer.append( "<br><b>Combat encounters</b>: " );
		if ( combats > 0 )
		{
			buffer.append( ff.format( combatFactor * 100.0 ) + "%" );
			buffer.append( "<br><b>Average XP/turn from Combat</b>: " + ff.format( averageXP * combatFactor ) );
		}
		else if ( combats == 0 )
			buffer.append( "0%" );
		else
			buffer.append( "No data" );
		buffer.append( "<br>" );

		for ( int i = 0; i < monsters.size(); ++i )
		{
			MonsterDatabase.Monster monster = getMonster( i );
			int weighting = getWeighting( i );
			buffer.append( "<br>" );
			buffer.append( getMonsterString( monster, moxie, hitstat, ml, weighting, combatFactor ) );
		}

		buffer.append( "</html>" );
		return buffer.toString();
	}

        private double areaCombatPercent()
	{
		// If we don't have the data, pretend it's all combat
		if ( combats < 0 )
			return 100.0;

		double pct = (double)combats + KoLCharacter.getCombatPercentAdjustment();
		return Math.max( 0.0, Math.min( 100.0, pct ) );
	}

	private String getRateString( double minPercent, int minMargin, double maxPercent, int maxMargin, boolean isMoxieTest )
	{
		StringBuffer buffer = new StringBuffer();

		buffer.append( ff.format( minPercent ) );
		buffer.append( "%/" );

		buffer.append( ff.format( maxPercent ) );
		buffer.append( "% (" );

		buffer.append( isMoxieTest ? "Moxie " : "Muscle " );

		if ( minMargin >= 0 )
			buffer.append( "+" );
		buffer.append( minMargin );

		buffer.append( "/" );

		if ( maxMargin >= 0 )
			buffer.append( "+" );
		buffer.append( maxMargin );

		buffer.append( ")" );
		return buffer.toString();
	}

	private String getRateString( double percent, int margin )
	{
		StringBuffer buffer = new StringBuffer();

		buffer.append( ff.format( percent ) );
		buffer.append( "% (" );
		if ( margin >= 0 )
			buffer.append( "+" );
		buffer.append( margin );
		buffer.append( ")" );
		return buffer.toString();
	}

	private String getMonsterString( MonsterDatabase.Monster monster, int moxie, int hitstat, int ml, int weighting, double combatFactor )
	{
		// moxie and hitstat already adjusted for monster level

		int defense = monster.getDefense();
		double hitPercent = hitPercent( hitstat, defense );
		int perfectHit = perfectHit( hitstat, defense );

		int attack = monster.getAttack();
		double evadePercent = hitPercent( moxie, attack );
		int perfectEvade = perfectHit( moxie, attack );

		int HP = monster.getAdjustedHP( ml );
		double XP = monster.getXP();

		StringBuffer buffer = new StringBuffer();

		int ed = monster.getDefenseElement();
		int ea = monster.getAttackElement();
		int element = ( ed == MonsterDatabase.NONE ) ? ea : ed;

		// Color the monster name according to its element
		buffer.append( " <font color=" + elementColor( element ) + "><b>" );
		buffer.append( monster.getName() );
		buffer.append( "</b></font> (" );
		if ( weighting < 0 )
			buffer.append( "ultra-rare" );
		else if ( weighting == 0 )
			buffer.append( "one-time" );
		else
			buffer.append( ff.format( 100.0 * combatFactor * (double)weighting / (double)weights ) + "%" );
                buffer.append( ")<br> - Hit: <font color=" + elementColor( ed ) + ">" );
		buffer.append( ff.format( hitPercent ) );
		buffer.append( "%</font>, Evade: <font color=" + elementColor( ea ) + ">" );
		buffer.append( ff.format( evadePercent ) );
		buffer.append( "%</font><br> - HP: " + HP + ", XP: " + ff.format( XP ) );

		printItemList( buffer, "<br> - Items: ", monster.getItems() );

		return buffer.toString();
	}

        private void printItemList( StringBuffer buffer, String prefix, List items )
	{
		if ( items.size() == 0 )
			return;

		double itemModifier = ( 100.0 + KoLCharacter.getItemDropPercentAdjustment() ) / 100.0;
		buffer.append( prefix );
		for ( int i = 0; i < items.size(); ++i )
		{
			AdventureResult item = (AdventureResult)items.get(i);
			if ( i > 0 )
				buffer.append( ", " );
			double drop = Math.min( (double)item.getCount() * itemModifier, 100.0 );
			buffer.append( item.getName() + " (" + ff.format( drop ) + "%)" );
		}
	}

	public static String elementColor( int element )
	{
		if ( element == MonsterDatabase.HEAT )
			return "#ff0000";
		if ( element == MonsterDatabase.COLD )
			return "#0000ff";
		if ( element == MonsterDatabase.STENCH )
			return "#008000";
		if ( element == MonsterDatabase.SPOOKY )
			return "#808080";
		if ( element == MonsterDatabase.SLEAZE )
			return "#8a2be2";

		return "#000000";
	}

	public static double hitPercent( int attack, int defense )
	{
		// ( (Attack - Defense) / 18 ) * 100 + 50 = Hit%
		double percent = 100.0 * ( attack - defense ) / 18 + 50.0;
		if ( percent < 0.0 )
			return 0.0;
		if ( percent > 100.0 )
			return 100.0;
		return percent;
	}

	public static int perfectHit( int attack, int defense )
	{	return attack - defense - 9;
	}
}
