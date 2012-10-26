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

package net.sourceforge.kolmafia.combat;

import java.util.List;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.MonsterData;

import net.sourceforge.kolmafia.objectpool.ItemPool;

import net.sourceforge.kolmafia.persistence.MonsterDatabase;
import net.sourceforge.kolmafia.persistence.MonsterDatabase.Element;
import net.sourceforge.kolmafia.persistence.MonsterDatabase.Phylum;

import net.sourceforge.kolmafia.session.EquipmentManager;

import net.sourceforge.kolmafia.utilities.StringUtilities;

public class MonsterStatusTracker
{
	private static MonsterData monsterData = null;
	private static String lastMonsterName = "";

	private static int healthModifier = 0;
	private static int attackModifier = 0;
	private static int defenseModifier = 0;
	private static int healthManuel = 0;
	private static int attackManuel = 0;
	private static int defenseManuel = 0;

	public static final void reset()
	{
		MonsterStatusTracker.healthModifier = 0;
		MonsterStatusTracker.attackModifier = 0;
		MonsterStatusTracker.defenseModifier = 0;
		MonsterStatusTracker.healthManuel = 0;
		MonsterStatusTracker.attackManuel = 0;
		MonsterStatusTracker.defenseManuel = 0;
	}

	public static final MonsterData getLastMonster()
	{
		return MonsterStatusTracker.monsterData;
	}

	public static final String getLastMonsterName()
	{
		return MonsterStatusTracker.lastMonsterName;
	}

	public static final void setNextMonsterName( String monsterName )
	{
		MonsterStatusTracker.reset();

		MonsterStatusTracker.monsterData = MonsterDatabase.findMonster( monsterName, false );

		if ( MonsterStatusTracker.monsterData == null && EquipmentManager.getEquipment( EquipmentManager.WEAPON ).getItemId() == ItemPool.SWORD_PREPOSITIONS )
		{
			monsterName = StringUtilities.lookupPrepositions( monsterName );
			MonsterStatusTracker.monsterData = MonsterDatabase.findMonster( monsterName, false );
		}

		if ( MonsterStatusTracker.monsterData == null )
		{
			// Temporarily register the unknown monster so that
			// consult scripts can see it as such	
			MonsterStatusTracker.monsterData = MonsterDatabase.registerMonster( monsterName );
		}

		MonsterStatusTracker.lastMonsterName = monsterName;
	}

	public static final boolean dropsItem( int itemId )
	{
		if ( itemId == 0 || MonsterStatusTracker.monsterData == null )
		{
			return false;
		}

		AdventureResult item = ItemPool.get( itemId, 1 );

		return MonsterStatusTracker.monsterData.getItems().contains( item );
	}

	public static final boolean dropsItems( List<AdventureResult> items )
	{
		if ( items.isEmpty() || MonsterStatusTracker.monsterData == null )
		{
			return false;
		}

		return MonsterStatusTracker.monsterData.getItems().containsAll( items );
	}

	public static final boolean shouldSteal()
	{
		if ( MonsterStatusTracker.monsterData == null )
		{
			return true;
		}

		return MonsterStatusTracker.monsterData.shouldSteal();
	}

	public static final int getMonsterHealth()
	{
		if ( MonsterStatusTracker.monsterData == null )
		{
			return 0;
		}

		if ( MonsterStatusTracker.healthManuel > 0 )
		{
			return MonsterStatusTracker.healthManuel;
		}

		return MonsterStatusTracker.monsterData.getHP() - MonsterStatusTracker.healthModifier;
	}

	public static final void healMonster( int amount )
	{
		MonsterStatusTracker.healthModifier -= amount;

		if ( MonsterStatusTracker.healthModifier < 0 )
		{
			MonsterStatusTracker.healthModifier = 0;
		}
	}

	public static final void damageMonster( int amount )
	{
		MonsterStatusTracker.healthModifier += amount;
	}

	public static final void resetAttackAndDefense()
	{
		MonsterStatusTracker.attackModifier = 0;
		MonsterStatusTracker.defenseModifier = 0;
	}

	public static final int getMonsterBaseAttack()
	{
		if ( MonsterStatusTracker.monsterData == null )
		{
			return 0;
		}

		return MonsterStatusTracker.monsterData.getAttack();
	}

	public static final int getMonsterAttack()
	{
		if ( MonsterStatusTracker.monsterData == null )
		{
			return 0;
		}

		if ( MonsterStatusTracker.attackManuel > 0 )
		{
			return MonsterStatusTracker.attackManuel;
		}

		int baseAttack = MonsterStatusTracker.monsterData.getAttack();
		int adjustedAttack = baseAttack + MonsterStatusTracker.attackModifier;
		return baseAttack == 0 ? adjustedAttack: Math.max( adjustedAttack, 1 );
	}

	public static final Element getMonsterAttackElement()
	{
		if ( MonsterStatusTracker.monsterData == null )
		{
			return Element.NONE;
		}

		return MonsterStatusTracker.monsterData.getAttackElement();
	}

	public static final void lowerMonsterAttack( int amount )
	{
		MonsterStatusTracker.attackModifier -= amount;
	}

	public static final int getMonsterAttackModifier()
	{
		if ( MonsterStatusTracker.monsterData == null )
		{
			return 0;
		}

		return MonsterStatusTracker.attackModifier;
	}

	public static final boolean willUsuallyDodge()
	{
		return MonsterStatusTracker.willUsuallyDodge( 0 );
	}

	public static final boolean willUsuallyDodge( final int attackModifier )
	{
		if ( MonsterStatusTracker.monsterData == null )
		{
			return false;
		}

		return MonsterStatusTracker.monsterData.willUsuallyDodge( MonsterStatusTracker.attackModifier + attackModifier );
	}

	public static final int getMonsterDefense()
	{
		if ( MonsterStatusTracker.monsterData == null )
		{
			return 0;
		}

		if ( MonsterStatusTracker.defenseManuel > 0 )
		{
			return MonsterStatusTracker.defenseManuel;
		}

		int baseDefense = MonsterStatusTracker.monsterData.getDefense();
		int adjustedDefense = baseDefense + MonsterStatusTracker.defenseModifier;
		return baseDefense == 0 ? adjustedDefense : Math.max( adjustedDefense, 1 );
	}

	public static final Element getMonsterDefenseElement()
	{
		if ( MonsterStatusTracker.monsterData == null )
		{
			return Element.NONE;
		}

		return MonsterStatusTracker.monsterData.getDefenseElement();
	}

	public static final Phylum getMonsterPhylum()
	{
		if ( MonsterStatusTracker.monsterData == null )
		{
			return Phylum.NONE;
		}

		return MonsterStatusTracker.monsterData.getPhylum();
	}

	public static final void lowerMonsterDefense( int amount )
	{
		MonsterStatusTracker.defenseModifier -= amount;
	}

	public static final int getMonsterDefenseModifier()
	{
		if ( MonsterStatusTracker.monsterData == null )
		{
			return 0;
		}

		return MonsterStatusTracker.defenseModifier;
	}

	public static final boolean willUsuallyMiss()
	{
		return MonsterStatusTracker.willUsuallyMiss( 0 );
	}

	public static final boolean willUsuallyMiss( final int defenseModifier )
	{
		if ( MonsterStatusTracker.monsterData == null )
		{
			return false;
		}

		return MonsterStatusTracker.monsterData.willUsuallyMiss( MonsterStatusTracker.defenseModifier + defenseModifier );
	}

	public static int getPoisonLevel()
	{
		if ( MonsterStatusTracker.monsterData == null )
		{
			return 0;
		}

		return MonsterStatusTracker.monsterData.getPoison();
	}

	public static void setManuelStats( int attack, int defense, int hp )
	{
		MonsterStatusTracker.attackManuel = attack;
		MonsterStatusTracker.defenseManuel = defense;
		MonsterStatusTracker.healthManuel = hp;
	}

}
