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

package net.sourceforge.kolmafia;

import java.lang.CloneNotSupportedException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import net.sourceforge.kolmafia.persistence.ConcoctionDatabase;
import net.sourceforge.kolmafia.persistence.MonsterDatabase.Element;
import net.sourceforge.kolmafia.persistence.MonsterDatabase.Phylum;

import net.sourceforge.kolmafia.session.EncounterManager.EncounterType;
import net.sourceforge.kolmafia.session.EquipmentManager;
import net.sourceforge.kolmafia.session.GoalManager;

public class MonsterData
	extends AdventureResult
{
	private Object health;
	private Object attack;
	private Object defense;
	private Object initiative;
	private Object experience;
	private Object scale;
	private Object cap;
	private final int floor;
	private Object mlMult;
	private Element attackElement;
	private Element defenseElement;
	private int physicalResistance;
	private int meat;
	private final Phylum phylum;
	private final int poison;
	private final boolean boss;
	private final boolean dummy;
	private final EncounterType type;
	private final String image;
	private final String[] images;
	private String manuelName = null;
	private final String attributes;
	private final int beeCount;

	private final ArrayList<AdventureResult> items;
	private final ArrayList<Double> pocketRates;

	// The following apply to a specific (cloned) instance of a monster
	private String[] randomModifiers;

	private static final String[][] crazyModifierMapping =
	{
		{ "annoying", "annoying" },
		{ "artisanal", "artisanal" },
		{ "askew", "askew" },
		{ "blinking", "phase-shifting" },
		{ "blue", "ice-cold" },
		{ "blurry", "blurry" },
		{ "bouncing", "bouncing" },
		{ "broke", "broke" },
		{ "clingy", "clingy" },
		{ "cloned", "cloned" },
		{ "cloud", "cloud-based" },
		{ "clowny", "clowning" },
		{ "crimbo", "yuletide" },
		{ "curse", "cursed" },
		{ "disguised", "disguised" },
		{ "drunk", "drunk" },
		{ "electric", "electrified" },
		{ "flies", "filthy" },
		{ "flip", "Australian" },
		{ "floating", "floating" },
		{ "fragile", "fragile" },
		{ "fratty", "fratty" },
		{ "frozen", "frozen" },
		{ "generous", "generous" },
		{ "ghostly", "ghostly" },
		{ "gray", "spooky" },
		{ "green", "stinky" },
		{ "haunted", "haunted" },
		{ "hilarious", "hilarious" },
		{ "hopping", "hopping-mad" },
		{ "hot", "hot" },
		{ "huge", "huge" },
		{ "invisible", "invisible" },
		{ "jitter", "jittery" },
		{ "lazy", "lazy" },
		{ "leet", "1337" },
		{ "mirror", "left-handed" },
		{ "narcissistic", "narcissistic" },
		{ "obscene", "obscene" },
		{ "optimal", "optimal" },
		{ "patriotic", "American" },
		{ "pixellated", "pixellated" },
		{ "pulse", "throbbing" },
		{ "purple", "sleazy" },
		{ "quacking", "quacking" },
		{ "rainbow", "tie-dyed" },
		{ "red", "red-hot" },
		{ "rotate", "twirling" },
		{ "shakes", "shaky" },
		{ "short", "short" },
		{ "shy", "shy" },
		{ "skinny", "skinny" },
		{ "sparkling", "solid gold" },
		{ "spinning", "cartwheeling" },
		{ "stingy", "stingy" },
		{ "swearing", "foul-mouthed" },
		{ "ticking", "ticking" },
		{ "tiny", "tiny" },
		{ "turgid", "turgid" },
		{ "unlucky", "unlucky" },
		{ "unstoppable", "unstoppable" },
		{ "untouchable", "untouchable" },
		{ "wet", "wet" },
		{ "wobble", "dancin'" },
		{ "xray", "negaverse" },
		{ "yellow", "cowardly" },
		{ "zoom", "restless" },
	};

	public static final HashMap<String, String> crazySummerModifiers = new HashMap<String, String>();
	static
	{
		for ( String[] mapping : MonsterData.crazyModifierMapping )
		{
			MonsterData.crazySummerModifiers.put( mapping[0], mapping[1] );
		}
	};

	public static final int DEFAULT_SCALE = 0;
	public static final int DEFAULT_CAP = 10000;
	public static final int DEFAULT_FLOOR = 10;

	public static String[] lastRandomModifiers = null;

	public MonsterData( final String name, final int id,
			    final Object health, final Object attack, final Object defense,
			    final Object initiative, final Object experience,
			    final Object scale, final Object cap, final int floor, final Object mlMult,
			    final Element attackElement, final Element defenseElement,
			    final int physicalResistance,
			    final int meat, final Phylum phylum, final int poison,
			    final boolean boss, final boolean dummy,
			    final EncounterType type, final String[] images,
			    final String manuelName, final String attributes )
	{
		super( AdventureResult.MONSTER_PRIORITY, name );

		this.id = id;

		this.health = health;
		this.attack = attack;
		this.defense = defense;
		this.initiative = initiative;
		this.experience = experience;
		this.scale = scale;
		this.cap = cap;
		this.floor = floor == Integer.MIN_VALUE ? MonsterData.DEFAULT_FLOOR : floor;
		this.mlMult = mlMult;
		this.attackElement = attackElement;
		this.defenseElement = defenseElement;
		this.physicalResistance = physicalResistance;
		this.meat = meat;
		this.phylum = phylum;
		this.poison = poison;
		this.boss = boss;
		this.dummy = dummy;
		this.type = type;
		this.image = images.length > 0 ? images[ 0 ] : "";
		this.images = images;
		this.manuelName = manuelName;
		this.attributes = attributes;

		int beeCount = 0;
		// Wandering bees don't have a bee count
		if ( id < 1075 || id > 1083 )
		{
			for ( int i = 0; i < name.length(); ++i )
			{
				char c = name.charAt( i );
				if ( c == 'b' || c == 'B' )
				{
					beeCount++;
				}
			}
		}
		this.beeCount = beeCount;

		this.items = new ArrayList<AdventureResult>();
		this.pocketRates = new ArrayList<Double>();

		// No random modifiers
		this.randomModifiers = new String[0];
	}

	public MonsterData handleMonsterLevel()
	{
		// If we tracked nostagger, stunresist, and start-of-combat
		int physRes = Math.min( (int) Math.floor( ML() / 2.5 ), 50 );
		if ( this.scale != null && physRes < 0 )
		{
			physRes = 0;
		}

		if ( physRes <= this.physicalResistance )
		{
			return this;
		}
		// elemental damage, this would be the place to put it.
		try
		{
			monster = (MonsterData)this.clone();
		}
		catch ( CloneNotSupportedException e )
		{
			// This should not happen. Hope for the best.
			return this;
		}
		if ( monster.physicalResistance == 0 )
		{
			monster.physicalResistance = physRes;
		}
		     monster.physicalResistance = Math.max( physRes, monster.physicalResistance );
	}

	public MonsterData handleRandomModifiers()
	{
		String[] modifiers = MonsterData.lastRandomModifiers;

		if ( modifiers == null || modifiers.length == 0 )
		{
			return this;
		}

		// Clone the monster so we don't munge the template
		MonsterData monster;
		try
		{
			monster = (MonsterData)this.clone();
		}
		catch ( CloneNotSupportedException e )
		{
			// This should not happen. Hope for the best.
			return this;
		}

		// Save the modifiers for use by scripts
		monster.randomModifiers = modifiers;

		// Iterate over them and modify the base values
		for ( int i = 0; i < modifiers.length; ++i )
		{
			String modifier = modifiers[ i ];

			if ( modifier.equals( "askew" ) )
			{
				monster.attack = new Integer( monster.getRawAttack() * 11 / 10 );
			}
			else if ( modifier.equals( "bouncing" ) )
			{
				monster.attack = new Integer( monster.getRawAttack() * 3 / 2 );
			}
			else if ( modifier.equals( "broke" ) )
			{
				monster.meat = 5;
			}
			else if ( modifier.equals( "cloned" ) )
			{
				monster.health = new Integer( monster.getRawHP() * 2 );
				monster.attack = new Integer( monster.getRawAttack() * 2 );
				monster.defense = new Integer( monster.getRawDefense() * 2 );
			}
			else if ( modifier.equals( "dancin'" ) )
			{
				monster.defense = new Integer( monster.getRawDefense() * 3 / 2 );
			}
			else if ( modifier.equals( "filthy" ) )
			{
				// Stench Aura
			}
			else if ( modifier.equals( "floating" ) )
			{
				monster.defense = new Integer( monster.getRawDefense() * 3 / 2 );
			}
			else if ( modifier.equals( "foul-mouthed" ) )
			{
				// Sleaze Aura
			}
			else if ( modifier.equals( "fragile" ) )
			{
				monster.health = new Integer( 1 );
			}
			else if ( modifier.equals( "frozen" ) )
			{
				monster.attackElement = Element.COLD;
				monster.defenseElement = Element.COLD;
			}
			else if ( modifier.equals( "ghostly" ) )
			{
				if ( monster.physicalResistance == 0 )
				{
					monster.physicalResistance = 90;
				}
			}
			else if ( modifier.equals( "haunted" ) )
			{
				// Spooky Aura
			}
			else if ( modifier.equals( "hot" ) )
			{
				// Hot Aura
			}
			else if ( modifier.equals( "huge" ) )
			{
				monster.health = new Integer( monster.getRawHP() * 2 );
				monster.attack = new Integer( monster.getRawAttack() * 2 );
				monster.defense = new Integer( monster.getRawDefense() * 2 );
			}
			else if ( modifier.equals( "ice-cold" ) )
			{
				monster.attackElement = Element.COLD;
				monster.defenseElement = Element.COLD;
			}
			else if ( modifier.equals( "left-handed" ) )
			{
				Object originalAttack = monster.attack;
				Object originalDefense = monster.defense;
				monster.attack = originalDefense;
				monster.defense = originalAttack;
			}
			else if ( modifier.equals( "red-hot" ) )
			{
				monster.attackElement = Element.HOT;
				monster.defenseElement = Element.HOT;
			}
			else if ( modifier.equals( "short" ) )
			{
				monster.health = new Integer( monster.getRawHP() / 2 );
				monster.defense = new Integer( monster.getRawDefense() * 2 );
			}
			else if ( modifier.equals( "skinny" ) )
			{
				monster.health = new Integer( monster.getRawHP() / 2 );
				monster.defense = new Integer( monster.getRawDefense() / 2 );
			}
			else if ( modifier.equals( "sleazy" ) )
			{
				monster.attackElement = Element.SLEAZE;
				monster.defenseElement = Element.SLEAZE;
			}
			else if ( modifier.equals( "solid gold" ) )
			{
				monster.meat = 1000;
			}
			else if ( modifier.equals( "spooky" ) )
			{
				monster.attackElement = Element.SPOOKY;
				monster.defenseElement = Element.SPOOKY;
			}
			else if ( modifier.equals( "stinky" ) )
			{
				monster.attackElement = Element.STENCH;
				monster.defenseElement = Element.STENCH;
			}
			else if ( modifier.equals( "throbbing" ) )
			{
				monster.health = new Integer( monster.getRawHP() * 2 );
			}
			else if ( modifier.equals( "tiny" ) )
			{
				monster.health = new Integer( monster.getRawHP() / 10 );
				monster.attack = new Integer( monster.getRawAttack() / 10 );
				monster.defense = new Integer( monster.getRawDefense() / 10 );
			}
			else if ( modifier.equals( "turgid" ) )
			{
				monster.health = new Integer( monster.getRawHP() * 5 );
			}
			else if ( modifier.equals( "unlucky" ) )
			{
				monster.health = new Integer( 13 );
				monster.attack = new Integer( 13 );
				monster.defense = new Integer( 13 );
			}
			else if ( modifier.equals( "wet" ) )
			{
				// Cold Aura
			}
		}

		return monster;
	}

	private int evaluate( Object obj, int value )
	{
		if ( obj != null )
		{
			if ( obj instanceof Integer )
			{
				return ( (Integer) obj ).intValue();
			}
			if ( obj instanceof String )
			{
				obj = compile( obj );
			}
			if ( obj instanceof MonsterExpression )
			{
				return (int) ( ( (MonsterExpression) obj ).eval() );
			}
		}
		return value;
	}

	public int ML()
	{
		/* For brevity, and to handle the possible future need for
		   asking for speculative monster stats */
		return KoLCharacter.getMonsterLevelAdjustment() * evaluate( this.mlMult, 1 );
	}

	public boolean scales()
	{
		return this.scale != null;
	}

	private MonsterExpression compile( Object expr )
	{
		return MonsterExpression.getInstance( (String) expr, this.getName() );
	}

	private double getBeeosity()
	{
		return 1.0 + ( KoLCharacter.inBeecore() ? ( this.beeCount / 5.0 ) : 0.0 );
	}

	public int getId()
	{
		return this.id;
	}

	public String getManuelName()
	{
		return this.manuelName == null ? this.name : this.manuelName;
	}

	public void setManuelName( final String manuelName)
	{
		this.manuelName = manuelName;
	}

	public int getHP()
	{
		if ( this.scale != null )
		{
			int scale = evaluate( this.scale, MonsterData.DEFAULT_SCALE );
			int hp = KoLCharacter.getAdjustedMuscle() + scale;
			int cap = evaluate( this.cap, MonsterData.DEFAULT_CAP);
			int ml = ML();

			hp = hp > cap ? cap : hp;
			ml = ml < 0 ? 0 : ml;
			hp = (int) Math.floor( ( hp + ml ) * 0.75 * getBeeosity() );
			hp = hp < this.floor ? this.floor : hp;
			return (int) Math.max( 1, hp );
		}
		if ( this.health == null )
		{
			return 0;
		}
		if ( this.health instanceof Integer )
		{
			int hp = ((Integer) this.health).intValue();

			if ( hp == 0 && ( this.attack == null || ( this.attack instanceof Integer && ((Integer) this.attack).intValue() == 0 ) ) )
			{
				// The monster is unknown, so do not apply modifiers
				return 0;
			}
			if ( KoLCharacter.inBigcore() )
			{
				hp += 150;
			}
			return (int) Math.floor( Math.max( 1, hp + ML() ) * getBeeosity() );
		}
		if ( this.health instanceof String )
		{
			this.health = compile( this.health );
		}
		return Math.max( 1, (int) (((MonsterExpression) this.health).eval() * getBeeosity() ) );
	}

	public int getRawHP()
	{
		if ( this.scale != null )
		{
			int scale = evaluate( this.scale, MonsterData.DEFAULT_SCALE );
			int hp = KoLCharacter.getAdjustedMuscle() + scale;
			int cap = evaluate( this.cap, MonsterData.DEFAULT_CAP );

			hp = hp > cap ? cap : hp < this.floor ? this.floor : hp;
			return (int) Math.floor( Math.max( 1, ( hp ) * 0.75 ) );
		}
		if ( this.health == null )
		{
			return -1;
		}
		return Math.max( 1, evaluate( this.health, 1 ) );
	}

	public int getBaseHP()
	{
		return  this.scale != null ? -1 :
			this.health == null ? 0 :
			this.health instanceof Integer ? ((Integer)(this.health)).intValue() :
			-1;
	}

	public int getAttack()
	{
		if ( this.scale != null )
		{
			int scale = evaluate( this.scale, MonsterData.DEFAULT_SCALE );
			int attack = KoLCharacter.getAdjustedMoxie() + scale;
			int cap = evaluate( this.cap, MonsterData.DEFAULT_CAP );
			attack = attack > cap ? cap : attack;
			int ml = ML();
			ml = ml < 0 ? 0 : ml;

			attack = (int) Math.floor( ( attack + ml ) * getBeeosity() );
			attack = attack < this.floor ? this.floor : attack;
			return (int) Math.max( 1, attack );
		}
		if ( this.attack == null )
		{
			return 0;
		}
		if ( this.attack instanceof Integer )
		{
			int attack = ((Integer) this.attack).intValue();
			if ( attack == 0 && ((Integer) this.health).intValue() == 0 )
			{
				// The monster is unknown, so do not apply modifiers
				return 0;
			}
			if ( KoLCharacter.inBigcore() )
			{
				// The bonus attack from BIG cannot raise a monster's attack above 300
				attack = Math.min( attack + 150, Math.max( 300, attack ) );
			}
			return (int) Math.floor( Math.max( 1, attack + ML() ) *
			       getBeeosity() );
		}
		if ( this.attack instanceof String )
		{
			this.attack = compile( this.attack );
		}
		return Math.max( 1, (int) (((MonsterExpression) this.attack).eval() * getBeeosity() ) );
	}

	public int getRawAttack()
	{
		if ( this.scale != null )
		{
			int scale = evaluate( this.scale, MonsterData.DEFAULT_SCALE );
			int attack = KoLCharacter.getAdjustedMoxie() + scale;
			int cap = evaluate( this.cap, MonsterData.DEFAULT_CAP );
			attack = attack > cap ? cap : attack < this.floor ? this.floor : attack;
			return (int) Math.max( 1, attack );
		}

		if ( this.attack == null )
		{
			return -1;
		}
		int attack = evaluate( this.attack, 0 );
		return Math.max( 1, attack );
	}

	public int getBaseAttack()
	{
		return  this.scale != null ? -1 :
			this.attack == null ? 0 :
			this.attack instanceof Integer ? ((Integer)(this.attack)).intValue() :
			-1;
	}

	public int getDefense()
	{
		double reduceMonsterDefense = KoLCharacter.currentNumericModifier( Modifiers.REDUCE_ENEMY_DEFENSE ) / 100;
		if ( this.scale != null )
		{
			int scale = evaluate( this.scale, MonsterData.DEFAULT_SCALE );
			int defense = KoLCharacter.getAdjustedMuscle() + scale;
			int cap = evaluate( this.cap, MonsterData.DEFAULT_CAP );

			defense = defense > cap ? cap : defense < this.floor ? this.floor : defense;
			int ml = ML();
			ml = ml < 0 ? 0 : ml;
			defense = (int) Math.floor( ( defense + ml ) * getBeeosity() );
			defense = defense < this.floor ? this.floor : defense;
			return (int) Math.floor( Math.max( 1, defense * ( 1 - reduceMonsterDefense ) ) );
		}
		if ( this.defense == null )
		{
			return 0;
		}
		if ( this.defense instanceof Integer )
		{
			int defense = ((Integer) this.defense).intValue();
			if ( defense == 0 && ((Integer) this.health).intValue() == 0 )
			{
				// The monster is unknown, so do not apply modifiers
				return 0;
			}
			if ( KoLCharacter.inBigcore() )
			{
				// The bonus defense from BIG cannot raise a monster's defense above 300
				defense = Math.min( defense + 150, Math.max( 300, defense ) );
			}
			return (int) Math.floor( Math.max( 1, defense + ML() ) *
			       getBeeosity() * ( 1 - reduceMonsterDefense ) );
		}
		if ( this.defense instanceof String )
		{
			this.defense = compile( this.defense );
		}
		return Math.max( 1, (int) (((MonsterExpression) this.defense).eval() *
				getBeeosity() * ( 1 - reduceMonsterDefense ) ) );
	}

	public int getRawDefense()
	{
		if ( this.scale != null )
		{
			int scale = evaluate( this.scale, MonsterData.DEFAULT_SCALE );
			int defense = KoLCharacter.getAdjustedMuscle() + scale;
			int cap = evaluate( this.cap, MonsterData.DEFAULT_CAP );
			defense = defense > cap ? cap : defense < this.floor ? this.floor : defense;
			return (int) Math.floor( Math.max( 1, defense ) );
		}
		if ( this.defense == null )
		{
			return -1;
		}
		return Math.max( 1, evaluate( this.defense, 0 ) );
	}

	public int getBaseDefense()
	{
		return  this.scale != null ? -1 :
			this.defense == null ? 0 :
			this.defense instanceof Integer ? ((Integer)(this.defense)).intValue() :
			-1;
	}

	public int getRawInitiative()
	{
		return evaluate( this.initiative, -1 );
	}

	public int getBaseInitiative()
	{
		return  this.initiative == null ? 0 :
			this.initiative instanceof Integer ? ((Integer)(this.initiative)).intValue() :
			-1;
	}

	public int getInitiative()
	{
		return this.getInitiative( KoLCharacter.getMonsterLevelAdjustment() );
	}

	public int getInitiative( final int monsterLevel )
	{
		int baseInit = this.getRawInitiative();
		if ( baseInit == -1 || baseInit == 10000 || baseInit == -10000)
		{
			return baseInit;
		}
		else
		{
			return baseInit + initPenalty( monsterLevel );
		}
	}

	public int getJumpChance()
	{
		if ( this.initiative == null )
		{
			return -1;
		}
		return this.getJumpChance( (int) KoLCharacter.getInitiativeAdjustment(), KoLCharacter.getMonsterLevelAdjustment() );
	}

	public int getJumpChance( final int initBonus )
	{
		return this.getJumpChance( initBonus, KoLCharacter.getMonsterLevelAdjustment() );
	}

	public int getJumpChance( final int initBonus, final int monsterLevel )
	{
		int monsterInit = this.getInitiative( monsterLevel );
		if ( monsterInit == 10000 )
		{
			return 0;
		}
		else if ( monsterInit == -10000 )
		{
			return 100;
		}
		int jumpChance = 100 - monsterInit + initBonus + Math.max( 0, KoLCharacter.getBaseMainstat() - this.getAttack() );
		return jumpChance > 100 ? 100 : jumpChance < 0 ? 0 : jumpChance;
	}

	public static final int initPenalty( final int monsterLevel )
	{
		return monsterLevel <= 20 ? 0 :
			monsterLevel <= 40 ? ( monsterLevel - 20 ) :
			monsterLevel <= 60 ? ( 20 + 2 * ( monsterLevel - 40 ) ) :
			monsterLevel <= 80 ? ( 60 + 3 * ( monsterLevel - 60 ) ) :
			monsterLevel <= 100 ? ( 120 + 4 * ( monsterLevel - 80 ) ) :
			( 200 + 5 * ( monsterLevel - 100 ) );
	}

	public Element getAttackElement()
	{
		return this.attackElement;
	}

	public Element getDefenseElement()
	{
		return this.defenseElement;
	}

	public int getPhysicalResistance()
	{
		return this.physicalResistance;
	}

	public int getMinMeat()
	{
		int variation = (int) Math.max( 1, Math.floor( this.meat * 0.2 ) );
		return this.meat > 0 ? this.meat - variation : 0;
	}

	public int getBaseMeat()
	{
		return this.meat;
	}

	public int getMaxMeat()
	{
		int variation = (int) Math.max( 1, Math.floor( this.meat * 0.2 ) );
		return this.meat > 0 ? this.meat + variation : 0;
	}

	public Phylum getPhylum()
	{
		return this.phylum;
	}

	public int getPoison()
	{
		return this.poison;
	}

	public boolean isBoss()
	{
		return this.boss;
	}

	public boolean isDummy()
	{
		return this.dummy;
	}

	public EncounterType getType()
	{
		return this.type;
	}

	public String getImage()
	{
		return this.image == null ? "" : this.image;
	}

	public String[] getImages()
	{
		return this.images == null ? new String[0] : this.images;
	}

	public boolean hasImage( final String test )
	{
		for ( String image : this.getImages() )
		{
			if ( image.equals( test ) )
			{
				return true;
			}
		}
		return false;
	}

	public String getAttributes()
	{
		return this.attributes == null ? "" : this.attributes;
	}

	public String[] getRandomModifiers()
	{
		return this.randomModifiers == null ? new String[0] : this.randomModifiers;
	}

	public List<AdventureResult> getItems()
	{
		return this.items;
	}

	public List getPocketRates()
	{
		return this.pocketRates;
	}

	public boolean shouldSteal()
	{
		// If the player has an acceptable dodge rate or
		// then steal anything.

		if ( this.willUsuallyDodge( 0 ) )
		{
			return this.shouldSteal( this.items );
		}

		// Otherwise, only steal from monsters that drop
		// something on your conditions list.

		return this.shouldSteal( GoalManager.getGoals() );
	}

	private boolean shouldSteal( final List checklist )
	{
		double dropModifier = AreaCombatData.getDropRateModifier();

		for ( int i = 0; i < checklist.size(); ++i )
		{
			if ( this.shouldStealItem( (AdventureResult) checklist.get( i ), dropModifier ) )
			{
				return true;
			}
		}

		return false;
	}

	private boolean shouldStealItem( AdventureResult item, final double dropModifier )
	{
		if ( !item.isItem() )
		{
			return false;
		}

		int itemIndex = this.items.indexOf( item );

		// If the monster drops this item, then return true
		// when the drop rate is less than 100%.

		if ( itemIndex != -1 )
		{
			item = (AdventureResult) this.items.get( itemIndex );
			switch ( (char) item.getCount() & 0xFFFF )
			{
			case 'p':
				return true;
			case 'n':
			case 'c':
			case 'f':
			case 'b':
				return false;
			default:
				return (item.getCount() >> 16) * dropModifier < 100.0;
			}
		}

		// If the item does not drop, check to see if maybe
		// the monster drops one of its ingredients.

		AdventureResult[] subitems = ConcoctionDatabase.getStandardIngredients( item.getItemId() );
		if ( subitems.length < 2 )
		{
			return false;
		}

		for ( int i = 0; i < subitems.length; ++i )
		{
			if ( this.shouldStealItem( subitems[ i ], dropModifier ) )
			{
				return true;
			}
		}

		// The monster doesn't drop the item or any of its
		// ingredients.

		return false;
	}

	public void clearItems()
	{
		this.items.clear();
	}

	public void addItem( final AdventureResult item )
	{
		this.items.add( item );
	}

	public void doneWithItems()
	{
		this.items.trimToSize();

		// Calculate the probability that an item will be yoinked
		// based on the integral provided by Buttons on the HCO forums.
		// http://forums.hardcoreoxygenation.com/viewtopic.php?t=3396

		double probability = 0.0;
		double[] coefficients = new double[ this.items.size() ];

		for ( int i = 0; i < this.items.size(); ++i )
		{
			coefficients[ 0 ] = 1.0;
			for ( int j = 1; j < coefficients.length; ++j )
			{
				coefficients[ j ] = 0.0;
			}

			for ( int j = 0; j < this.items.size(); ++j )
			{
				AdventureResult item = (AdventureResult) this.items.get( j );
				probability = (item.getCount() >> 16) / 100.0;
				switch ( (char) item.getCount() & 0xFFFF )
				{
				case 'p':
					if ( probability == 0.0 )
					{	// assume some probability of a pickpocket-only item
						probability = 0.05;
					}
					break;
				case 'n':
				case 'c':
				case 'f':
				case 'b':
					probability = 0.0;
					break;
				}

				if ( i == j )
				{
					for ( int k = 0; k < coefficients.length; ++k )
					{
						coefficients[ k ] = coefficients[ k ] * probability;
					}
				}
				else
				{
					for ( int k = coefficients.length - 1; k >= 1; --k )
					{
						coefficients[ k ] = coefficients[ k ] - probability * coefficients[ k - 1 ];
					}
				}
			}

			probability = 0.0;

			for ( int j = 0; j < coefficients.length; ++j )
			{
				probability += coefficients[ j ] / ( j + 1 );
			}

			this.pocketRates.add( new Double( probability ) );
		}
	}

	public double getExperience()
	{
		if ( this.scale != null )
		{
			int scale = evaluate( this.scale, MonsterData.DEFAULT_SCALE );
			int experience = KoLCharacter.getAdjustedMainstat() + scale;
			int cap = evaluate( this.cap, MonsterData.DEFAULT_CAP );
			experience = experience > cap ? cap : experience < this.floor ? this.floor : experience;
			int ml = ML();
			ml = ml < 0 ? 0 : ml;
			return (double) Math.max( 1, ( experience / 8.0 + ml / 6.0 ) );
		}
		if ( this.experience == null )
		{
			return Math.max( ( this.getAttack() / this.getBeeosity() - ML() ) / 8.0 + ML() / 6.0, 0 );
		}
		return evaluate( this.experience, 0 ) / 2.0;
	}

	public boolean willUsuallyMiss()
	{
		return this.willUsuallyMiss( 0 );
	}

	public boolean willUsuallyDodge( final int offenseModifier )
	{
		int dodgeRate = KoLCharacter.getAdjustedMoxie() - ( this.getAttack() + offenseModifier ) - 6;
		return dodgeRate > 0;
	}

	public boolean willUsuallyMiss( final int defenseModifier )
	{
		int hitStat = EquipmentManager.getAdjustedHitStat();

		return AreaCombatData.hitPercent( hitStat - defenseModifier, this.getDefense() ) <= 50.0;
	}
}
