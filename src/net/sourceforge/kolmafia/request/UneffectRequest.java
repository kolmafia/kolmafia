/**
 * Copyright (c) 2005-2008, KoLmafia development team
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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.KoLmafiaCLI;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.session.InventoryManager;
import net.sourceforge.kolmafia.session.MoodManager;
import net.sourceforge.kolmafia.session.ResultProcessor;
import net.sourceforge.kolmafia.swingui.RequestFrame;
import net.sourceforge.kolmafia.utilities.StringUtilities;

import net.sourceforge.kolmafia.persistence.EffectDatabase;
import net.sourceforge.kolmafia.persistence.SkillDatabase;

public class UneffectRequest
	extends GenericRequest
{
	private final int effectId;
	private boolean force;
	private boolean isShruggable;
	private final AdventureResult effect;

	public static final AdventureResult REMEDY = new AdventureResult( "soft green echo eyedrop antidote", 1 );
	public static final AdventureResult TINY_HOUSE = new AdventureResult( "tiny house", 1 );
	public static final AdventureResult FOREST_TEARS = new AdventureResult( "forest tears", 1 );

	private static final Pattern ID1_PATTERN = Pattern.compile( "whicheffect=(\\d+)" );
	private static final Pattern ID2_PATTERN = Pattern.compile( "whichbuff=(\\d+)" );

	public UneffectRequest( final AdventureResult effect )
	{
		this( effect, true );
	}

	public UneffectRequest( final AdventureResult effect, final boolean force )
	{
		super( UneffectRequest.isShruggable( effect.getName() ) ? "charsheet.php" : "uneffect.php" );

		this.force = force;

		this.effect = effect;
		this.effectId = EffectDatabase.getEffectId( effect.getName() );
		this.isShruggable = UneffectRequest.isShruggable( effect.getName() );

		if ( this.isShruggable )
		{
			this.addFormField( "pwd" );
			this.addFormField( "action", "unbuff" );
			this.addFormField( "whichbuff", String.valueOf( this.effectId ) );
		}
		else
		{
			this.addFormField( "pwd" );
			this.addFormField( "using", "Yep." );
			this.addFormField( "whicheffect", String.valueOf( this.effectId ) );
		}
	}

	protected boolean retryOnTimeout()
	{
		return true;
	}

	public static final boolean isShruggable( final String effectName )
	{
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
		if ( effectName.equalsIgnoreCase( "Polka of Plenty" ) || effectName.equalsIgnoreCase( "Power Ballad of the Arrowsmith" ) || effectName.equalsIgnoreCase( "Psalm of Pointiness" ) || effectName.equalsIgnoreCase( "Ode to Booze" ) )
		{
			return "The " + effectName;
		}

		if ( effectName.equalsIgnoreCase( "Empathy" ) )
		{
			return "Empathy of the Newt";
		}

		if ( effectName.equalsIgnoreCase( "Smooth Movements" ) )
		{
			return "Smooth Movement";
		}

		if ( effectName.equalsIgnoreCase( "Pasta Oneness" ) )
		{
			return "Manicotti Meditation";
		}

		if ( effectName.equalsIgnoreCase( "Saucemastery" ) )
		{
			return "Sauce Contemplation";
		}

		if ( effectName.equalsIgnoreCase( "Disco State of Mind" ) )
		{
			return "Disco Aerobics";
		}

		if ( effectName.equalsIgnoreCase( "Mariachi Mood" ) )
		{
			return "Moxie of the Mariachi";
		}

		return effectName;
	}

	public static final String skillToEffect( final String skillName )
	{
		if ( skillName.equals( "The Polka of Plenty" ) || skillName.equals( "The Power Ballad of the Arrowsmith" ) || skillName.equals( "The Psalm of Pointiness" ) || skillName.equals( "The Ode to Booze" ) )
		{
			return skillName.substring( 4 );
		}

		if ( skillName.equals( "Empathy of the Newt" ) )
		{
			return "Empathy";
		}

		if ( skillName.equals( "Smooth Movement" ) )
		{
			return "Smooth Movements";
		}

		if ( skillName.equals( "Manicotti Meditation" ) )
		{
			return "Pasta Oneness";
		}

		if ( skillName.equals( "Sauce Contemplation" ) )
		{
			return "Saucemastery";
		}

		if ( skillName.equals( "Disco Aerobics" ) )
		{
			return "Disco State of Mind";
		}

		if ( skillName.equals( "Moxie of the Mariachi" ) )
		{
			return "Mariachi Mood";
		}

		return skillName;
	}

	public void run()
	{
		if ( !KoLConstants.activeEffects.contains( this.effect ) )
		{
			return;
		}

		String action = MoodManager.getDefaultAction( "gain_effect", this.effect.getName() );

		if ( !action.equals( "" ) && !action.startsWith( "uneffect" ) )
		{
			KoLmafiaCLI.DEFAULT_SHELL.executeLine( action );
			return;
		}

		if ( !this.force )
		{
			return;
		}

		if ( !this.isShruggable )
		{
			if ( KoLCharacter.canInteract() )
			{
				InventoryManager.retrieveItem( UneffectRequest.REMEDY.getName() );
			}

			if ( !KoLConstants.inventory.contains( UneffectRequest.REMEDY ) )
			{
				return;
			}
		}

		KoLmafia.updateDisplay( this.isShruggable ? "Shrugging off your buff..." : "Using soft green whatever..." );
		super.run();
	}

	public void processResults()
	{
		// If it notifies you that the effect was removed, delete it
		// from the list of effects.

		if ( this.responseText != null && ( this.isShruggable || this.responseText.indexOf( "Effect removed." ) != -1 ) )
		{
			KoLConstants.activeEffects.remove( this.effect );

			if ( this.isShruggable )
			{
				CharSheetRequest.parseStatus( this.responseText );
			}
			else
			{
				ResultProcessor.processResult( UneffectRequest.REMEDY.getNegation() );
			}

			KoLmafia.updateDisplay( this.effect.getName() + " removed." );
			RequestFrame.refreshStatus();
		}
		else if ( !this.isShruggable )
		{
			KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "Failed to remove " + this.effect.getName() + "." );
		}
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

		RequestLogger.updateSessionLog();
		RequestLogger.updateSessionLog( "uneffect " + EffectDatabase.getEffectName( StringUtilities.parseInt( idMatcher.group( 1 ) ) ) );
		return true;
	}
}
