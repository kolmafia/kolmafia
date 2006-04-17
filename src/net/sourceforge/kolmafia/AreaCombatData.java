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

public class AreaCombatData implements KoLConstants
{
	private int minHit;
	private int maxHit;
	private int minEvade;
	private int maxEvade;
	private List monsters;

	public AreaCombatData()
	{
		this.monsters = new ArrayList();
		minHit = Integer.MAX_VALUE;
		maxHit = 0;
		minEvade = Integer.MAX_VALUE;
		maxEvade = 0;
	}

	public boolean addMonster( String name )
	{
		MonsterDatabase.Monster monster = MonsterDatabase.findMonster( name );
		if ( monster == null )
			return false;

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

		monsters.add( monster );
		return true;
	}

	public MonsterDatabase.Monster getMonster( int i )
	{	return (MonsterDatabase.Monster) monsters.get(i);
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

	public String safetyString()
	{
		boolean ranged = KoLCharacter.rangedWeapon();

		int ml = monsterLevelAdjustment();
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

		StringBuffer buffer = new StringBuffer();

		buffer.append( "<html><b>Hit</b>: " );
		buffer.append( getRateString( minHitPercent, minPerfectHit, maxHitPercent, maxPerfectHit, ranged ) );

		buffer.append( "<br><b>Evade</b>: " );
		buffer.append( getRateString( minEvadePercent, minPerfectEvade, maxEvadePercent, maxPerfectEvade, true ) );
		buffer.append( "<br>" );

		for ( int i = 0; i < monsters.size(); ++i )
		{
			buffer.append( "<br>" );
			MonsterDatabase.Monster monster = getMonster( i );
			buffer.append( getMonsterString( monster, moxie, hitstat ) );
		}

		buffer.append( "</html>" );
		return buffer.toString();
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

	private String getMonsterString( MonsterDatabase.Monster monster, int moxie, int hitstat )
	{
		// moxie and hitstat already adjusted for monster level

		int defense = monster.getDefense();
		double hitPercent = hitPercent( hitstat, defense );
		int perfectHit = perfectHit( hitstat, defense );

		int attack = monster.getAttack();
		double evadePercent = hitPercent( moxie, attack );
		int perfectEvade = perfectHit( moxie, attack );

		StringBuffer buffer = new StringBuffer();

		int element = monster.getDefenseElement();
		if ( element == MonsterDatabase.NONE )
			element = monster.getAttackElement();

		// Color the monster name according to its element
		if ( element == MonsterDatabase.HEAT )
			buffer.append( " <font color=red>" );
		else if ( element == MonsterDatabase.COLD )
			buffer.append( " <font color=blue>" );
		else if ( element == MonsterDatabase.STENCH )
			buffer.append( " <font color=green>" );
		else if ( element == MonsterDatabase.SPOOKY )
			buffer.append( " <font color=gray>" );
		else if ( element == MonsterDatabase.SLEAZE )
			buffer.append( " <font color=magenta>" );
		else
			buffer.append( " <font color=black>" );

		buffer.append( monster.getName() );
		buffer.append( "</font><br> - Hit: " );
		buffer.append( ff.format( hitPercent ) );
		buffer.append( "%, Evade: " );
		buffer.append( ff.format( evadePercent ) );
		buffer.append( "%" );

		return buffer.toString();
	}

	public double hitPercent( int attack, int defense )
	{
		// ( (Attack - Defense) / 18 ) * 100 + 50 = Hit%
		double percent = 100.0 * ( attack - defense ) / 18 + 50.0;
		if ( percent < 0.0 )
			return 0.0;
		if ( percent > 100.0 )
			return 100.0;
		return percent;
	}

	public int perfectHit( int attack, int defense )
	{	return attack - defense - 9;
	}

	private static final AdventureResult ARIA = new AdventureResult( "Ur-Kel's Aria of Annoyance", 0 );
	private static final int ICE_SICKLE = 1424;
	private static final int HIPPO_WHIP = 1029;
	private static final int GIANT_NEEDLE = 619;
	private static final int GOTH_KID = 703;
	private static final int HOCKEY_STICK = 1236;
	private static final int SCARF = 1227;
	private static final int AGGRAVATE_MONSTER = 835;
	private static final int PITCHFORK = 1116;

	public static int monsterLevelAdjustment()
	{
		int ml = KoLCharacter.getMindControlLevel();

		for ( int slot = KoLCharacter.WEAPON; slot <= KoLCharacter.FAMILIAR; ++slot )
		{
			if ( slot == KoLCharacter.PANTS )
				continue;

			AdventureResult item = KoLCharacter.getCurrentEquipment( slot );
			if ( item == null )
				continue;

			switch ( item.getItemID() )
			{
			case ICE_SICKLE:
				ml += 15;
				break;
			case HIPPO_WHIP:
				ml += 10;
				break;
			case GIANT_NEEDLE:
				ml += 5;
				break;
			case GOTH_KID:
				ml += 5;
				break;
			case HOCKEY_STICK:
				ml += 30;
				break;
			case SCARF:
				ml += 20;
				break;
			case AGGRAVATE_MONSTER:
				ml += 5;
				break;
			case PITCHFORK:
				ml += 5;
				break;
			}
		}

		// Effects: Aria of Annoyance
		if ( KoLCharacter.getEffects().contains( ARIA ) )
			ml += 2 * KoLCharacter.getLevel();

		return ml;
	}
}
