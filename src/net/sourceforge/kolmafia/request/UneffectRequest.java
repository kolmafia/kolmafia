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
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.KoLmafiaCLI;
import net.sourceforge.kolmafia.RequestLogger;

import net.sourceforge.kolmafia.moods.MoodManager;

import net.sourceforge.kolmafia.objectpool.EffectPool;
import net.sourceforge.kolmafia.objectpool.IntegerPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;

import net.sourceforge.kolmafia.persistence.ConcoctionDatabase;
import net.sourceforge.kolmafia.persistence.EffectDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.persistence.NPCStoreDatabase;
import net.sourceforge.kolmafia.persistence.SkillDatabase;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.session.InventoryManager;
import net.sourceforge.kolmafia.session.ResultProcessor;

import net.sourceforge.kolmafia.swingui.RequestFrame;

import net.sourceforge.kolmafia.utilities.StringUtilities;

public class UneffectRequest
	extends GenericRequest
{
	private final int effectId;
	private boolean isShruggable;
	private boolean isTimer;
	private final AdventureResult effect;

	private static final Set currentEffectRemovals = new HashSet();

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
	};

	public UneffectRequest( final AdventureResult effect )
	{
		super( UneffectRequest.isShruggable( effect.getName() ) ? "charsheet.php" : "uneffect.php" );

		this.effect = effect;
		String name = effect.getName();
		this.effectId = EffectDatabase.getEffectId( name );
		this.isShruggable = UneffectRequest.isShruggable( name );
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
		}
	}

	@Override
	protected boolean retryOnTimeout()
	{
		return true;
	}

	public static final boolean isRemovable( final int effectId )
	{
		switch ( effectId )
		{
		case -1:
		case EffectPool.GOOFBALL_WITHDRAWAL_ID:
		case EffectPool.EAU_DE_TORTUE_ID:
		case EffectPool.CURSED_BY_RNG_ID:
		case EffectPool.FORM_OF_BIRD_ID:
		case EffectPool.COVERED_IN_SLIME_ID:
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

	/**
	 * Given the name of an effect, return the name of the skill that created that effect
	 *
	 * @param effectName The name of the effect
	 * @return skill The name of the skill
	 */

	public static final String effectToSkill( final String effectName )
	{
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
	private static Map removeWithSkillMap = new LinkedHashMap();

	private static Set REMOVABLE_BY_ITEM;
	private static Map removeWithItemMap = new LinkedHashMap();

	static
	{
		Set removableEffects;

		removableEffects = new HashSet();
		removeWithItemMap.put( IntegerPool.get( ItemPool.ANTIDOTE ), removableEffects );
		removableEffects.add( "Hardly Poisoned at All" );
		removableEffects.add( "Majorly Poisoned" );
		removableEffects.add( "A Little Bit Poisoned" );
		removableEffects.add( "Somewhat Poisoned" );
		removableEffects.add( "Really Quite Poisoned" );

		removableEffects = new HashSet();
		removeWithItemMap.put( IntegerPool.get( ItemPool.TINY_HOUSE ), removableEffects );
		removableEffects.add( "Beaten Up" );
		removableEffects.add( "Confused" );
		removableEffects.add( "Embarrassed" );
		removableEffects.add( "Sunburned" );
		removableEffects.add( "Wussiness" );

		removableEffects = new HashSet();
		removeWithItemMap.put( IntegerPool.get( ItemPool.TEARS ), removableEffects );
		removableEffects.add( "Beaten Up" );

		UneffectRequest.REMOVABLE_BY_ITEM = removeWithItemMap.entrySet();

		removableEffects = new HashSet();
		removeWithSkillMap.put( "Tongue of the Otter", removableEffects );
		removableEffects.add( "Beaten Up" );

		removableEffects = new HashSet();
		removeWithSkillMap.put( "Tongue of the Walrus", removableEffects );
		removableEffects.add( "Axe Wound" );
		removableEffects.add( "Beaten Up" );
		removableEffects.add( "Grilled" );
		removableEffects.add( "Half Eaten Brain" );
		removableEffects.add( "Missing Fingers" );
		removableEffects.add( "Sunburned" );

		removableEffects = new HashSet();
		removeWithSkillMap.put( "Disco Nap", removableEffects );
		removableEffects.add( "Confused" );
		removableEffects.add( "Embarrassed" );
		removableEffects.add( "Sleepy" );
		removableEffects.add( "Sunburned" );
		removableEffects.add( "Wussiness" );

		removableEffects = new HashSet();
		removeWithSkillMap.put( "Disco Power Nap", removableEffects );
		removableEffects.add( "Affronted Decency" );
		removableEffects.add( "Apathy" );
		removableEffects.add( "Confused" );
		removableEffects.add( "Cunctatitis" );
		removableEffects.add( "Embarrassed" );
		removableEffects.add( "Easily Embarrassed" );
		removableEffects.add( "Existential Torment" );
		removableEffects.add( "Light Headed" );
		removableEffects.add( "N-Spatial vision" );
		removableEffects.add( "Prestidigysfunction" );
		removableEffects.add( "Rainy Soul Miasma" );
		removableEffects.add( "Sleepy" );
		removableEffects.add( "Socialismydia" );
		removableEffects.add( "Sunburned" );
		removableEffects.add( "Tenuous Grip on Reality" );
		removableEffects.add( "Tetanus" );
		removableEffects.add( "The Colors..." );
		removableEffects.add( "\"The Disease\"" );
		removableEffects.add( "Wussiness" );

		removableEffects = new HashSet();
		removeWithSkillMap.put( "Pep Talk", removableEffects );
		removableEffects.add( "Overconfident" );

		UneffectRequest.REMOVABLE_BY_SKILL = removeWithSkillMap.entrySet();
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
			     !hasRemedy && KoLCharacter.canInteract() && Preferences.getBoolean( "autoSatisfyWithMall" ) )
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
			KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "You don't have that effect." );
			return;
		}

		AdventureResult effect = (AdventureResult) KoLConstants.activeEffects.get( index );

		if ( !UneffectRequest.isRemovable( this.effectId ) )
		{
			KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, effect.getName() + " is unremovable." );
			return;
		}

		if ( effect.getCount() == Integer.MAX_VALUE && this.effectId != EffectPool.OVERCONFIDENT_ID )
		{
			KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, effect.getName() + " is intrinsic and cannot be removed." );
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
		if ( this.effect.getName().equals( EffectPool.INIGO ) )
		{
			ConcoctionDatabase.setRefreshNeeded( true );
		}

		KoLmafia.updateDisplay( this.effect.getName() + " removed." );
		RequestFrame.refreshStatus();
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
