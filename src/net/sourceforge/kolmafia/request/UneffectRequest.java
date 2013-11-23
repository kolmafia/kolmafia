/**
 * Copyright (c) 2005-2013, KoLmafia development team
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

package net.sourceforge.kolmafia.request;

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.KoLmafiaCLI;
import net.sourceforge.kolmafia.RequestLogger;

import net.sourceforge.kolmafia.moods.MoodManager;

import net.sourceforge.kolmafia.objectpool.EffectPool;
import net.sourceforge.kolmafia.objectpool.EffectPool.Effect;
import net.sourceforge.kolmafia.objectpool.IntegerPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.objectpool.SkillPool;

import net.sourceforge.kolmafia.persistence.CoinmastersDatabase;
import net.sourceforge.kolmafia.persistence.ConcoctionDatabase;
import net.sourceforge.kolmafia.persistence.EffectDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.persistence.NPCStoreDatabase;
import net.sourceforge.kolmafia.persistence.SkillDatabase;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.session.InventoryManager;
import net.sourceforge.kolmafia.session.ResultProcessor;

import net.sourceforge.kolmafia.utilities.StringUtilities;

public class UneffectRequest
	extends GenericRequest
{
	private final int effectId;
	private boolean isShruggable;
	private boolean needsCocoa;
	private boolean isTimer;
	private final AdventureResult effect;

	private static final Set<AdventureResult> currentEffectRemovals = new HashSet<AdventureResult>();

	private static final AdventureResult USED_REMEDY = ItemPool.get( ItemPool.REMEDY, -1 );

	private static final Pattern ID1_PATTERN = Pattern.compile( "whicheffect=(\\d+)" );
	private static final Pattern ID2_PATTERN = Pattern.compile( "whichbuff=(\\d+)" );

	public static final String[][] EFFECT_SKILL =
	{
		// Effect Name
		// Skill Name
		{
			"Polka of Plenty",
			"The Polka of Plenty",
		},
		{
			"Power Ballad of the Arrowsmith",
			"The Power Ballad of the Arrowsmith",
		},
		{
			"Psalm of Pointiness",
			"The Psalm of Pointiness",
		},
		{
			"Ode to Booze",
			"The Ode to Booze",
		},
		{
			"Empathy",
			"Empathy of the Newt",
		},
		{
			"Smooth Movements",
			"Smooth Movement",
		},
		{
			"Pasta Oneness",
			"Manicotti Meditation",
		},
		{
			"Saucemastery",
			"Sauce Contemplation",
		},
		{
			"Disco State of Mind",
			"Disco Aerobics",
		},
		{
			"Mariachi Mood",
			"Moxie of the Mariachi",
		},
		{
			"A Few Extra Pounds",
			"Holiday Weight Gain",
		},
		{
			"Jingle Jangle Jingle",
			"Jingle Bells",
		},
		{
			"Iron Palms",
			"Iron Palm Technique",
		},
		{
			"Salamanderenity",
			"Salamander Kata",
		},
		{
			"Retrograde Relaxation",
			"Miyagi Massage",
		},
		{
			"Earthen Fist",
			"Worldpunch",
		},
		{
			"Boner Battalion",
			"Summon &quot;Boner Battalion&quot;",
		},
		{
			"Overconfident",
			"Pep Talk",
		},
		{
			"Chow Downed",
			"Zombie Chow",
		},
		{
			"Scavengers Scavenging",
			"Scavenge",
		},
		{
			"Zomg WTF",
			"Ag-grave-ation",
		},
		{
			"Well-Rested",
			"Hibernate",
		},
		{
			"Blubbered Up",
			"Blubber Up",
		},
		{
			"Spirit Souvenirs",
			"Spirit Vacation",
		},
		{
			"Al Dente Inferno",
			"Transcendent Al Dente",
		},
		{
			"Bloody Potato Bits",
			"Bind Vampieroghi",
		},
		{
			"Slinking Noodle Glob",
			"Bind Vermincelli",
		},
		{
			"Whispering Strands",
			"Bind Angel Hair Wisp",
		},
		{
			"Macaroni Coating",
			"Bind Undead Elbow Macaroni",
		},
		{
			"Penne Fedora",
			"Bind Penne Dreadful",
		},
		{
			"Pasta Eyeball",
			"Bind Lasagmbie",
		},
		{
			"Spice Haze",
			"Bind Spice Ghost",
		},
		{
			"Simmering",
			"Simmer",
		},
		{
			"Soulerskates",
			"Soul Rotation",
		},
		{
			"Disdain of the War Snapper",
			"Blessing of the War Snapper",
		},
		{
			"Disdain of She-Who-Was",
			"Blessing of She-Who-Was",
		},
		{
			"Disdain of the Storm Tortoise",
			"Blessing of the Storm Tortoise",
		},
		{
			"Avatar of the War Snapper",
			"Turtle Power",
		},
		{
			"Avatar of She-Who-Was",
			"Turtle Power",
		},
		{
			"Avatar of the Storm Tortoise",
			"Turtle Power",
		},
		{
			"Boon of the War Snapper",
			"Spirit Boon",
		},
		{
			"Boon of She-Who-Was",
			"Spirit Boon",
		},
		{
			"Boon of the Storm Tortoise",
			"Spirit Boon",
		},
	};

	public UneffectRequest( final AdventureResult effect )
	{
		super( UneffectRequest.isShruggable( effect.getName() ) ? "charsheet.php" : "uneffect.php" );

		this.effect = effect;
		String name = effect.getName();
		this.effectId = EffectDatabase.getEffectId( name );
		this.isShruggable = UneffectRequest.isShruggable( name );
		this.needsCocoa = UneffectRequest.needsCocoa( name );
		this.isTimer = name.startsWith( "Timer " );

		if ( this.isShruggable )
		{
			this.addFormField( "action", "unbuff" );
			this.addFormField( "ajax", "1" );
			this.addFormField( "whichbuff", String.valueOf( this.effectId ) );
		}
		else
		{
			this.addFormField( "using", "Yep." );
			this.addFormField( "whicheffect", String.valueOf( this.effectId ) );
			this.addFormField( "pwd" );
		}
	}

	@Override
	protected boolean retryOnTimeout()
	{
		return true;
	}

	public static final boolean isRemovable( final String effectName )
	{
		return UneffectRequest.isRemovable( EffectDatabase.getEffectId( effectName ) );
	}

	public static final boolean isRemovable( final int effectId )
	{
		// http://forums.kingdomofloathing.com/vb/showthread.php?t=195401
		// 
		// The following effects should now no longer be removable by any means:
		// Goofball Withdrawal
		// Soul-Crushing Headache
		// Coated in Slime
		// Everything Looks Yellow
		// Everything Looks Blue
		// Everything Looks Red
		// Deep-Tainted Mind
		// Timer effects

		switch ( effectId )
		{
		case -1:
			// So, what about the following?
		case EffectPool.EAU_DE_TORTUE_ID:
		case EffectPool.CURSED_BY_RNG_ID:
		case EffectPool.FORM_OF_BIRD_ID:

		case EffectPool.GOOFBALL_WITHDRAWAL_ID:
		case EffectPool.SOUL_CRUSHING_HEADACHE_ID:
		case EffectPool.COVERED_IN_SLIME_ID:
		case EffectPool.EVERYTHING_LOOKS_YELLOW_ID:
		case EffectPool.EVERYTHING_LOOKS_BLUE_ID:
		case EffectPool.EVERYTHING_LOOKS_RED_ID:
		case EffectPool.DEEP_TAINTED_MIND_ID:
		case EffectPool.SPIRIT_PARIAH_ID:
			return false;
		default:
			return true;
		}
	}

	public static final boolean isShruggable( final String effectName )
	{
		if ( effectName.startsWith( "Timer " ) )
		{
			return true;
		}

		if ( effectName.equals( "Just the Best Anapests" ) )
		{
			return true;
		}

		int id = SkillDatabase.getSkillId( UneffectRequest.effectToSkill( effectName ) );
		return id != -1 && SkillDatabase.isBuff( id );
	}

	public static final boolean needsCocoa( final String effectName )
	{
		return UneffectRequest.needsCocoa( EffectDatabase.getEffectId( effectName ) );
	}

	public static final boolean needsCocoa( final int effectId )
	{
		switch ( effectId )
		{
		case EffectPool.CURSE_OF_CLUMSINESS:
		case EffectPool.CURSE_OF_DULLNESS:
		case EffectPool.CURSE_OF_EXPOSURE:
		case EffectPool.CURSE_OF_FORGETFULNESS:
		case EffectPool.CURSE_OF_HOLLOWNESS:
		case EffectPool.CURSE_OF_IMPOTENCE:
		case EffectPool.CURSE_OF_LONELINESS:
		case EffectPool.CURSE_OF_MISFORTUNE:
		case EffectPool.CURSE_OF_SLUGGISHNESS:
		case EffectPool.CURSE_OF_VULNERABILITY:
		case EffectPool.CURSE_OF_WEAKNESS:
		case EffectPool.TOUCHED_BY_A_GHOST:
		case EffectPool.CHILLED_TO_THE_BONE:
		case EffectPool.NAUSEATED:
			return true;
		}
		return false;
	}

	/**
	 * Given the name of an effect, return the name of the skill that created that effect
	 *
	 * @param effectName The name of the effect
	 * @return skill The name of the skill
	 */

	public static final String effectToSkill( final String effectName )
	{
		// Handle effects where skill name and effect name don't match with a lookup
		for ( int i = 0; i < UneffectRequest.EFFECT_SKILL.length; ++i )
		{
			String [] data = UneffectRequest.EFFECT_SKILL[ i ];
			if ( effectName.equalsIgnoreCase( data[ 0 ] ) )
			{
				return data[ 1 ];
			}
		}

		return effectName;
	}

	public static final String skillToEffect( final String skillName )
	{
		// Some skills can produce different effects depending on current effects or class
		int skillId = SkillDatabase.getSkillId( skillName );
		
		switch( skillId )
		{
		case SkillPool.SPIRIT_BOON:
			if ( KoLCharacter.getBlessingType() == KoLCharacter.SHE_WHO_WAS_BLESSING )
			{
				return EffectDatabase.getEffectName( EffectPool.BOON_OF_SHE_WHO_WAS );
			}
			else if ( KoLCharacter.getBlessingType() == KoLCharacter.STORM_BLESSING )
			{
				return EffectDatabase.getEffectName( EffectPool.BOON_OF_THE_STORM_TORTOISE );
			}
			else if ( KoLCharacter.getBlessingType() == KoLCharacter.WAR_BLESSING )
			{
				return EffectDatabase.getEffectName( EffectPool.BOON_OF_THE_WAR_SNAPPER );
			}
			else
			{
				return "none";
			}
		case SkillPool.SHE_WHO_WAS_BLESSING:
			if ( KoLCharacter.getClassType() == KoLCharacter.TURTLE_TAMER )
			{
				return EffectDatabase.getEffectName( EffectPool.BLESSING_OF_SHE_WHO_WAS );
			}
			else
			{
				return EffectDatabase.getEffectName( EffectPool.DISTAIN_OF_SHE_WHO_WAS );
			}
		case SkillPool.STORM_BLESSING:
			if ( KoLCharacter.getClassType() == KoLCharacter.TURTLE_TAMER )
			{
				return EffectDatabase.getEffectName( EffectPool.BLESSING_OF_THE_STORM_TORTOISE );
			}
			else
			{
				return EffectDatabase.getEffectName( EffectPool.DISTAIN_OF_THE_STORM_TORTOISE );
			}
		case SkillPool.WAR_BLESSING:
			if ( KoLCharacter.getClassType() == KoLCharacter.TURTLE_TAMER )
			{
				return EffectDatabase.getEffectName( EffectPool.BLESSING_OF_THE_WAR_SNAPPER );
			}
			else
			{
				return EffectDatabase.getEffectName( EffectPool.DISTAIN_OF_THE_WAR_SNAPPER );
			}
		case SkillPool.TURTLE_POWER:
			if ( KoLCharacter.getBlessingType() == KoLCharacter.SHE_WHO_WAS_BLESSING )
			{
				return EffectDatabase.getEffectName( EffectPool.AVATAR_OF_SHE_WHO_WAS );
			}
			else if ( KoLCharacter.getBlessingType() == KoLCharacter.STORM_BLESSING )
			{
				return EffectDatabase.getEffectName( EffectPool.AVATAR_OF_THE_STORM_TORTOISE );
			}
			else if ( KoLCharacter.getBlessingType() == KoLCharacter.WAR_BLESSING )
			{
				return EffectDatabase.getEffectName( EffectPool.AVATAR_OF_THE_WAR_SNAPPER );
			}
			else
			{
				return "none";
			}
		}

		// Handle remaining skills where skill name and effect name don't match with a lookup
		for ( int i = 0; i < UneffectRequest.EFFECT_SKILL.length; ++i )
		{
			String [] data = UneffectRequest.EFFECT_SKILL[ i ];
			if ( skillName.equalsIgnoreCase( data[ 1 ] ) )
			{
				return data[ 0 ];
			}
		}
		
		return skillName;
	}

	private static Set REMOVABLE_BY_SKILL;
	private static final Map<String, Set<String>> removeWithSkillMap = new LinkedHashMap<String, Set<String>>();

	private static Set REMOVABLE_BY_ITEM;
	private static final Map<Integer, Set<String>> removeWithItemMap = new LinkedHashMap<Integer, Set<String>>();

	public static final void reset()
	{
		Set<String> removableEffects;

		removableEffects = new HashSet<String>();
		removeWithItemMap.put( IntegerPool.get( ItemPool.ANTIDOTE ), removableEffects );
		removableEffects.add( "Hardly Poisoned at All" );
		removableEffects.add( "Majorly Poisoned" );
		removableEffects.add( "A Little Bit Poisoned" );
		removableEffects.add( "Somewhat Poisoned" );
		removableEffects.add( "Really Quite Poisoned" );

		removableEffects = new HashSet<String>();
		removeWithItemMap.put( IntegerPool.get( ItemPool.TINY_HOUSE ), removableEffects );
		removableEffects.add( "Beaten Up" );
		removableEffects.add( "Confused" );
		removableEffects.add( "Embarrassed" );
		removableEffects.add( "Sunburned" );
		removableEffects.add( "Wussiness" );

		removableEffects = new HashSet<String>();
		removeWithItemMap.put( IntegerPool.get( ItemPool.TEARS ), removableEffects );
		removableEffects.add( "Beaten Up" );
		
		removableEffects = new HashSet<String>();
		removeWithItemMap.put( IntegerPool.get( ItemPool.TRIPPLES ), removableEffects );
		removableEffects.add( "Beaten Up" );

		removableEffects = new HashSet<String>();
		removeWithItemMap.put( IntegerPool.get( ItemPool.HOT_DREADSYLVANIAN_COCOA ), removableEffects );
		removableEffects.add( "Touched by a Ghost" );
		removableEffects.add( "Chilled to the Bone" );
		removableEffects.add( "Nauseated" );
		removableEffects.add( "Curse of Hollowness" );
		removableEffects.add( "Curse of Vulnerability" );
		removableEffects.add( "Curse of Exposure" );
		removableEffects.add( "Curse of Impotence" );
		removableEffects.add( "Curse of Dullness" );
		removableEffects.add( "Curse of Weakness" );
		removableEffects.add( "Curse of Sluggishness" );
		removableEffects.add( "Curse of Forgetfulness" );
		removableEffects.add( "Curse of Misfortune" );
		removableEffects.add( "Curse of Clumsiness" );
		removableEffects.add( "Curse of Loneliness" );

		UneffectRequest.REMOVABLE_BY_ITEM = removeWithItemMap.entrySet();

		removableEffects = new HashSet<String>();
		removeWithSkillMap.put( "Tongue of the Walrus", removableEffects );
		removableEffects.add( "Axe Wound" );
		removableEffects.add( "Beaten Up" );
		removableEffects.add( "Grilled" );
		removableEffects.add( "Half-Eaten Brain" );
		removableEffects.add( "Missing Fingers" );
		removableEffects.add( "Sunburned" );

		removableEffects = new HashSet<String>();
		removeWithSkillMap.put( "Disco Nap", removableEffects );
		removableEffects.add( "Confused" );
		removableEffects.add( "Embarrassed" );
		removableEffects.add( "Sleepy" );
		removableEffects.add( "Sunburned" );
		removableEffects.add( "Wussiness" );
		if ( KoLCharacter.hasSkill( "Adventurer of Leisure" ) )
		{
			removableEffects.add( "Affronted Decency" );
			removableEffects.add( "Apathy" );
			removableEffects.add( "Consumed by Fear" );
			removableEffects.add( "Cunctatitis" );
			removableEffects.add( "Easily Embarrassed" );
			removableEffects.add( "Existential Torment" );
			removableEffects.add( "Light-Headed" );
			removableEffects.add( "N-Spatial vision" );
			removableEffects.add( "Prestidigysfunction" );
			removableEffects.add( "Rainy Soul Miasma" );
			removableEffects.add( "Socialismydia" );
			removableEffects.add( "Tenuous Grip on Reality" );
			removableEffects.add( "Tetanus" );
			removableEffects.add( "The Colors..." );
			removableEffects.add( "\"The Disease\"" );
		}

		removableEffects = new HashSet<String>();
		removeWithSkillMap.put( "Pep Talk", removableEffects );
		removableEffects.add( "Overconfident" );

		UneffectRequest.REMOVABLE_BY_SKILL = removeWithSkillMap.entrySet();	
	}
	
	static
	{
		UneffectRequest.reset();
	}

	public static void removeEffectsWithItem( final int itemId )
	{
		HashSet effects = (HashSet) UneffectRequest.removeWithItemMap.get( IntegerPool.get( itemId ) );
		UneffectRequest.removeEffects( effects );
	}

	public static void removeEffectsWithSkill( final int skillId )
	{
		String skillName = SkillDatabase.getSkillName( skillId );

		if ( skillName == null )
		{
			return;
		}

		HashSet effects = (HashSet) UneffectRequest.removeWithSkillMap.get( skillName );
		UneffectRequest.removeEffects( effects );
	}

	private static void removeEffects( final HashSet effects )
	{
		if ( effects == null )
		{
			return;
		}

		Iterator it = effects.iterator();

		while ( it.hasNext() )
		{
			String name = (String)it.next();
			AdventureResult effect = new AdventureResult( name, 1, true );
			KoLConstants.activeEffects.remove( effect );
		}
	}

	private String getAction()
	{
		String name = this.effect.getName();

		// If there's an action defined in your mood, use it.

		String action = MoodManager.getDefaultAction( "gain_effect", name );
		String skillName = null;

		if ( action.startsWith( "cast " ) )
		{
			skillName = action.substring( 5 );
		}
		else if ( action.startsWith( "skill " ) )
		{
			skillName = action.substring( 6 );
		}

		if ( skillName != null && !KoLCharacter.hasSkill( skillName ) )
		{
			action = "";
		}

		if ( !action.equals( "" ) && !action.startsWith( "uneffect " ) )
		{
			KoLmafia.updateDisplay( name + " will be removed via pre-defined trigger (" + action + ")..." );

			return action;
		}

		// If it's shruggable, then the cleanest way is to just shrug it.

		if ( this.isShruggable )
		{
			return "uneffect " + name;
		}

		// Iterate over the effects that can be removed with skills or items
		// other than remedies.

		boolean hasRemedy = InventoryManager.hasItem( ItemPool.REMEDY );

		Iterator removableIterator = UneffectRequest.REMOVABLE_BY_SKILL.iterator();

		// See if it can be removed by a skill.

		while ( removableIterator.hasNext() )
		{
			Entry removable = (Entry) removableIterator.next();
			Set removables = (Set) removable.getValue();

			if ( !removables.contains( name ) )
			{
				continue;
			}

			skillName = (String) removable.getKey();

			if ( KoLCharacter.hasSkill( skillName ) )
			{
				KoLmafia.updateDisplay( name + " will be removed by skill " + skillName + "..." );

				return "cast " + skillName;
			}
		}

		// See if it can be removed by an item.

		removableIterator = UneffectRequest.REMOVABLE_BY_ITEM.iterator();

		while ( removableIterator.hasNext() )
		{
			Entry removable = (Entry) removableIterator.next();
			Set removables = (Set) removable.getValue();

			if ( !removables.contains( name ) )
			{
				continue;
			}

			int itemId = ( (Integer) removable.getKey() ).intValue();
			String itemName = ItemDatabase.getItemName( itemId );

			if ( InventoryManager.hasItem( itemId ) ||
			     Preferences.getBoolean( "autoSatisfyWithNPCs" ) && NPCStoreDatabase.contains( itemName ) ||
			     Preferences.getBoolean( "autoSatisfyWithCoinmasters" ) && CoinmastersDatabase.contains( itemName ) ||
			     (this.needsCocoa || !hasRemedy) && KoLCharacter.canInteract() && Preferences.getBoolean( "autoSatisfyWithMall" ) )
			{
				KoLmafia.updateDisplay( name + " will be removed by item " + itemName + "..." );

				return "use 1 " + itemName;
			}
		}

		// Default to using a remedy.

		KoLmafia.updateDisplay( name + " cannot be removed with an available item or skill..." );

		return "uneffect " + name;
	}

	@Override
	public void run()
	{
		int index = KoLConstants.activeEffects.indexOf( this.effect );
		if ( index == -1 )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "You don't have that effect." );
			return;
		}

		AdventureResult effect = (AdventureResult) KoLConstants.activeEffects.get( index );

		if ( !UneffectRequest.isRemovable( this.effectId ) )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, effect.getName() + " is unremovable." );
			return;
		}

		if ( effect.getCount() == Integer.MAX_VALUE && this.effectId != Effect.OVERCONFIDENT.effectId() )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, effect.getName() + " is intrinsic and cannot be removed." );
			return;
		}

		if ( !UneffectRequest.currentEffectRemovals.contains( effect ) )
		{
			String action = this.getAction();

			if ( !action.equals( "" ) && !action.startsWith( "uneffect" ) &&
			     !action.startsWith( "shrug" ) && !action.startsWith( "remedy" ) )
			{
				UneffectRequest.currentEffectRemovals.add( effect );
				KoLmafiaCLI.DEFAULT_SHELL.executeLine( action );
				UneffectRequest.currentEffectRemovals.remove( effect );
				return;
			}
		}

		// If you either have cocoa in inventory or allow purchasing it
		// via coinmaster or mall, we will have done so above.
		if ( this.needsCocoa )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, effect.getName() + " can be removed only with hot Dreadsylvanian cocoa." );
			return;
		}

		if ( this.isTimer )
		{
			KoLmafia.updateDisplay( "Canceling your timer..." );
		}
		else if ( this.isShruggable )
		{
			KoLmafia.updateDisplay( "Shrugging off your buff..." );
		}
		else if ( InventoryManager.retrieveItem( ItemPool.REMEDY ) )
		{
			KoLmafia.updateDisplay( "Using soft green whatever..." );
		}
		else
		{
			return;
		}

		super.run();
	}

	@Override
	public void processResults()
	{
		// Using a remedy no longer says "Effect removed." If you have
		// more remedies available, it gives a new list, not containing
		// the effect. If you have no more remedies, it says "You don't
		// have any more green fluffy antidote echo drops, or whatever
		// they're called."

		if ( this.responseText == null )
		{
			// What's wrong?
			return;
		}

		KoLConstants.activeEffects.remove( this.effect );

		// If you lose Inigo's, what you can craft changes
		if ( this.effect.getName().equals( Effect.INIGO.effectName() ) )
		{
			ConcoctionDatabase.setRefreshNeeded( true );
		}

		// If Gar-ish is gained or lost and autoGarish isn't set, benefit of Lasagna changes
		if ( this.effect.getName().equals( Effect.GARISH.effectName() ) && !Preferences.getBoolean( "autoGarish" ) )
		{
			ConcoctionDatabase.setRefreshNeeded( true );
		}

		KoLmafia.updateDisplay( this.effect.getName() + " removed." );
	}

	public static final boolean registerRequest( final String location )
	{
		if ( !location.startsWith( "uneffect.php" ) && !location.startsWith( "charsheet.php" ) )
		{
			return false;
		}

		if ( location.indexOf( "?" ) == -1 )
		{
			return true;
		}

		Matcher idMatcher =
			location.startsWith( "uneffect.php" ) ? UneffectRequest.ID1_PATTERN.matcher( location ) : UneffectRequest.ID2_PATTERN.matcher( location );

		if ( !idMatcher.find() )
		{
			return true;
		}

		int id = StringUtilities.parseInt( idMatcher.group( 1 ) );
		String name = EffectDatabase.getEffectName( id );

		if ( UneffectRequest.isRemovable( id ) && location.startsWith( "uneffect" ) )
		{
			ResultProcessor.processResult( UneffectRequest.USED_REMEDY );
		}

		RequestLogger.updateSessionLog();
		RequestLogger.updateSessionLog( "uneffect " + name );
		return true;
	}
}
