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

package net.sourceforge.kolmafia.combat;

import java.io.IOException;
import java.util.ArrayList;

import java.util.List;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.MonsterData;
import net.sourceforge.kolmafia.StaticEntity;

import net.sourceforge.kolmafia.objectpool.ItemPool;

import net.sourceforge.kolmafia.persistence.MonsterDatabase;
import net.sourceforge.kolmafia.persistence.MonsterDatabase.Element;
import net.sourceforge.kolmafia.persistence.MonsterDatabase.Phylum;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.request.FightRequest;

import net.sourceforge.kolmafia.session.EquipmentManager;

import net.sourceforge.kolmafia.utilities.HTMLParserUtils;
import net.sourceforge.kolmafia.utilities.StringUtilities;

import org.htmlcleaner.HtmlCleaner;
import org.htmlcleaner.TagNode;
import org.htmlcleaner.XPatherException;

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

	public static final void setNextMonsterName( String monsterName )
	{
		MonsterStatusTracker.reset();
		
		if ( KoLCharacter.isCrazyRandom() )
		{
			monsterName = MonsterStatusTracker.handleCrazyRandom( monsterName );
		}

		MonsterStatusTracker.monsterData = MonsterDatabase.findMonster( monsterName, false );

		if ( MonsterStatusTracker.monsterData == null && EquipmentManager.getEquipment( EquipmentManager.WEAPON ).getItemId() == ItemPool.SWORD_PREPOSITIONS )
		{
			monsterName = StringUtilities.lookupPrepositions( monsterName );
			MonsterStatusTracker.monsterData = MonsterDatabase.findMonster( monsterName, false );
		}

		if ( MonsterStatusTracker.monsterData == null )
		{
			if ( monsterName.startsWith( "the " ) )
			{
				MonsterStatusTracker.monsterData = MonsterDatabase.findMonster( monsterName.substring( 4 ), false );
				if ( MonsterStatusTracker.monsterData != null )
				{
					monsterName = monsterName.substring( 4 );
				}
			}
			else if ( monsterName.startsWith( "el " ) || monsterName.startsWith( "la " ) )
			{
				MonsterStatusTracker.monsterData = MonsterDatabase.findMonster( monsterName.substring( 3 ), false );
				if ( MonsterStatusTracker.monsterData != null )
				{
					monsterName = monsterName.substring( 3 );
				}
			}
		}

		if ( MonsterStatusTracker.monsterData == null )
		{
			// Temporarily register the unknown monster so that
			// consult scripts can see it as such	
			MonsterStatusTracker.monsterData = MonsterDatabase.registerMonster( monsterName );
		}

		MonsterStatusTracker.originalHealth = MonsterStatusTracker.monsterData.getHP();
		MonsterStatusTracker.originalAttack = MonsterStatusTracker.monsterData.getAttack();
		MonsterStatusTracker.originalDefense = MonsterStatusTracker.monsterData.getDefense();

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

		if ( MonsterStatusTracker.manuelFound )
		{
			return MonsterStatusTracker.healthManuel;
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

		if ( MonsterStatusTracker.manuelFound )
		{
			return MonsterStatusTracker.attackManuel;
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

		if ( MonsterStatusTracker.manuelFound )
		{
			return MonsterStatusTracker.defenseManuel;
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
		// If these are the first stats from Manuel
		if ( !manuelFound )
		{
			// Save them as the monster's original stats
			MonsterStatusTracker.originalAttack = attack;
			MonsterStatusTracker.originalDefense = defense;
			MonsterStatusTracker.originalHealth = hp;
		}

		MonsterStatusTracker.attackManuel = attack;
		MonsterStatusTracker.defenseManuel = defense;
		MonsterStatusTracker.healthManuel = hp;
		MonsterStatusTracker.manuelFound = true;
	}

	private static String handleCrazyRandom( String monsterName )
	{
		HtmlCleaner cleaner = HTMLParserUtils.configureDefaultParser();
		String xpath = "//script/text()";
		TagNode doc;
		try
		{
			doc = cleaner.clean( FightRequest.lastResponseText );
		}
		catch( IOException e )
		{
			StaticEntity.printStackTrace( e );
			return monsterName;
		}
		
		Object[] result;
		try
		{
			result = doc.evaluateXPath( xpath );
		}
		catch ( XPatherException ex )
		{
			return monsterName;
		}
		
		String text = "";
		for ( Object result1 : result )
		{
			text = result1.toString();
			if ( !text.startsWith( "var ocrs" ) )
			{
				continue;
			}
			break;
		}
		String[] temp = text.split( "\"" );
		boolean lastAttribute = false;
		ArrayList<String> attrs = new ArrayList<String>();
		for ( int i = 1; i < temp.length - 1; i++ ) // The first and last elements are not useful
		{
			if ( temp[i].contains( ":" ) || temp[i].equals( "," ) )
			{
				continue;
			}
			attrs.add( temp[i] );
		}

		int j = 0;
		for ( String attr : attrs )
		{
			j++;
			if ( j == attrs.size() )
			{
				lastAttribute = true;
			}
			monsterName = MonsterStatusTracker.removeCrazySummerAttribute( attr, monsterName, lastAttribute );
		}

		return monsterName;
	}

	private static final String removeCrazySummerAttribute( final String attribute, String monsterName, final boolean last )
	{
		String remove = "";
		if ( attribute.equals( "annoying" ) )
		{
			remove = "annoying";
		}
		else if ( attribute.equals( "artisanal" ) )
		{
			remove = "artisanal";
		}
		else if ( attribute.equals( "askew" ) )
		{
			remove = "askew";
		}
		else if ( attribute.equals( "blinking" ) )
		{
			remove = "phase-shifting";
		}
		else if ( attribute.equals( "blue" ) )
		{
			remove = "ice-cold";
		}
		else if ( attribute.equals( "blurry" ) )
		{
			remove = "blurry";
		}
		else if ( attribute.equals( "bouncing" ) )
		{
			remove = "bouncing";
		}
		else if ( attribute.equals( "broke" ) )
		{
			remove = "broke";
		}
		else if ( attribute.equals( "clingy" ) )
		{
			remove = "clingy";
		}
		else if ( attribute.equals( "crimbo" ) )
		{
			remove = "yuletide";
		}
		else if ( attribute.equals( "curse" ) )
		{
			remove = "cursed";
		}
		else if ( attribute.equals( "disguised" ) )
		{
			remove = "disguised";
		}
		else if ( attribute.equals( "drunk" ) )
		{
			remove = "drunk";
		}
		else if ( attribute.equals( "electric" ) )
		{
			remove = "electrified";
		}
		else if ( attribute.equals( "flies" ) )
		{
			remove = "filthy";
		}
		else if ( attribute.equals( "flip" ) )
		{
			remove = "Australian";
		}
		else if ( attribute.equals( "floating" ) )
		{
			remove = "floating";
		}
		else if ( attribute.equals( "fragile" ) )
		{
			remove = "fragile";
		}
		else if ( attribute.equals( "ghostly" ) )
		{
			remove = "ghostly";
		}
		else if ( attribute.equals( "haunted" ) )
		{
			remove = "haunted";
		}
		else if ( attribute.equals( "hopping" ) )
		{
			remove = "hopping-mad";
		}
		else if ( attribute.equals( "huge" ) )
		{
			remove = "huge";
		}
		else if ( attribute.equals( "invisible" ) )
		{
			remove = "invisible";
		}
		else if ( attribute.equals( "jitter" ) )
		{
			remove = "jittery";
		}
		else if ( attribute.equals( "lazy" ) )
		{
			remove = "lazy";
		}
		else if ( attribute.equals( "leet" ) )
		{
			remove = "1337";
		}
		else if ( attribute.equals( "mirror" ) )
		{
			remove = "left-handed";
		}
		else if ( attribute.equals( "narcissistic" ) )
		{
			remove = "narcissistic";
		}
		else if ( attribute.equals( "optimal" ) )
		{
			remove = "optimal";
		}
		else if ( attribute.equals( "pixellated" ) )
		{
			remove = "pixellated";
		}
		else if ( attribute.equals( "pulse" ) )
		{
			remove = "throbbing";
		}
		else if ( attribute.equals( "purple" ) )
		{
			remove = "sleazy";
		}
		else if ( attribute.equals( "quacking" ) )
		{
			remove = "quacking";
		}
		else if ( attribute.equals( "rainbow" ) )
		{
			remove = "tie-dyed";
		}
		else if ( attribute.equals( "red" ) )
		{
			remove = "red-hot";
		}
		else if ( attribute.equals( "rotate" ) )
		{
			remove = "twirling";
		}
		else if ( attribute.equals( "shakes" ) )
		{
			remove = "shaky";
		}
		else if ( attribute.equals( "short" ) )
		{
			remove = "short";
		}
		else if ( attribute.equals( "shy" ) )
		{
			remove = "shy";
		}
		else if ( attribute.equals( "skinny" ) )
		{
			remove = "skinny";
		}
		else if ( attribute.equals( "sparkling" ) )
		{
			remove = "solid gold";
		}
		else if ( attribute.equals( "spinning" ) )
		{
			remove = "cartwheeling";
		}
		else if ( attribute.equals( "swearing" ) )
		{
			remove = "foul-mouthed";
		}
		else if ( attribute.equals( "ticking" ) )
		{
			remove = "ticking";
		}
		else if ( attribute.equals( "tiny" ) )
		{
			remove = "tiny";
		}
		else if ( attribute.equals( "turgid" ) )
		{
			remove = "turgid";
		}
		else if ( attribute.equals( "unstoppable" ) )
		{
			remove = "unstoppable";
		}
		else if ( attribute.equals( "untouchable" ) )
		{
			remove = "untouchable";
		}
		else if ( attribute.equals( "wobble" ) )
		{
			remove = "dancin'";
		}
		else if ( attribute.equals( "xray" ) )
		{
			remove = "negaverse";
		}
		else if ( attribute.equals( "zoom" ) )
		{
			remove = "restless";
		}

		if ( last )
		{
			remove += " ";
		}
		else
		{
			remove += ", ";
		}

		return StringUtilities.singleStringDelete( monsterName, remove );
	}

}
