package net.sourceforge.kolmafia.combat;


import java.util.List;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.MonsterData;

import net.sourceforge.kolmafia.objectpool.ItemPool;

import net.sourceforge.kolmafia.persistence.MonsterDatabase.Element;
import net.sourceforge.kolmafia.persistence.MonsterDatabase.Phylum;

import net.sourceforge.kolmafia.preferences.Preferences;


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
	private static boolean manuelFound = false;
	private static int originalHealth = 0;
	private static int originalAttack = 0;
	private static int originalDefense = 0;

	public static final void reset()
	{
		MonsterStatusTracker.healthModifier = 0;
		MonsterStatusTracker.attackModifier = 0;
		MonsterStatusTracker.defenseModifier = 0;
		MonsterStatusTracker.healthManuel = 0;
		MonsterStatusTracker.attackManuel = 0;
		MonsterStatusTracker.defenseManuel = 0;
		MonsterStatusTracker.manuelFound = false;
	}

	public static final MonsterData getLastMonster()
	{
		return MonsterStatusTracker.monsterData;
	}

	public static final String getLastMonsterName()
	{
		return MonsterStatusTracker.lastMonsterName;
	}

	public static void transformMonster( MonsterData monster )
	{
		MonsterData newMonster = monster.transform();
		MonsterStatusTracker.setNextMonster( newMonster );
	}

	public static void setNextMonster( MonsterData monster )
	{
		MonsterStatusTracker.reset();
		MonsterStatusTracker.monsterData = monster;

		// If we saved an array of random modifiers, apply them
		MonsterStatusTracker.monsterData = MonsterStatusTracker.monsterData.handleRandomModifiers();
		MonsterStatusTracker.monsterData = MonsterStatusTracker.monsterData.handleMonsterLevel();

		MonsterStatusTracker.originalHealth = MonsterStatusTracker.monsterData.getHP();
		MonsterStatusTracker.originalAttack = MonsterStatusTracker.monsterData.getAttack();
		MonsterStatusTracker.originalDefense = MonsterStatusTracker.monsterData.getDefense();

		MonsterStatusTracker.lastMonsterName = monster.getName();
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
		// If the user doesn't want smart pickpocket behavior, don't give it
		if ( !Preferences.getBoolean( "safePickpocket" ) )
		{
			return true;
		}

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

		return MonsterStatusTracker.originalHealth - MonsterStatusTracker.healthModifier;
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

		int baseAttack = MonsterStatusTracker.originalAttack;
		int adjustedAttack = baseAttack + MonsterStatusTracker.attackModifier;
		return baseAttack == 0 ? adjustedAttack: Math.max( adjustedAttack, 1 );
	}

	public static final int getMonsterOriginalAttack()
	{
		return MonsterStatusTracker.monsterData == null  ? 0 : MonsterStatusTracker.originalAttack;
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

		int baseDefense = MonsterStatusTracker.originalDefense;
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

	public static final int getMonsterRawInitiative()
	{
		if ( MonsterStatusTracker.monsterData == null )
		{
			return 0;
		}

		return MonsterStatusTracker.monsterData.getRawInitiative();
	}

	public static final int getMonsterInitiative()
	{
		if ( MonsterStatusTracker.monsterData == null )
		{
			return 0;
		}

		return MonsterStatusTracker.monsterData.getInitiative();
	}

	public static final int getJumpChance()
	{
		if ( MonsterStatusTracker.monsterData == null )
		{
			return 0;
		}

		return MonsterStatusTracker.monsterData.getJumpChance();
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
		// Save what Manuel reported. These are the stats at the END of
		// the round's actions - including those which automatically
		// fired on round 0 before the player did anything.
		MonsterStatusTracker.attackManuel = attack;
		MonsterStatusTracker.defenseManuel = defense;
		MonsterStatusTracker.healthManuel = hp;

		// If we don't know anything about this monster, assume that
		// Manuel is showing the original stats - even though, as
		// described above, that's not always the case.
		if ( !manuelFound && MonsterStatusTracker.originalAttack == 0 )
		{
			MonsterStatusTracker.originalAttack = attack;
			MonsterStatusTracker.originalDefense = defense;
			MonsterStatusTracker.originalHealth = hp;
		}

		MonsterStatusTracker.manuelFound = true;
	}

	public static void applyManuelStats()
	{
		if ( manuelFound )
		{
			MonsterStatusTracker.attackModifier = MonsterStatusTracker.attackManuel - MonsterStatusTracker.originalAttack;
			MonsterStatusTracker.defenseModifier = MonsterStatusTracker.defenseManuel - MonsterStatusTracker.originalDefense;
			MonsterStatusTracker.healthModifier = MonsterStatusTracker.originalHealth - MonsterStatusTracker.healthManuel;
		}
	}
}
