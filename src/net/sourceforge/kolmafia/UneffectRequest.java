/**
 * Copyright (c) 2005-2007, KoLmafia development team
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

public class UneffectRequest extends KoLRequest
{
	private int effectId;
	private boolean isShruggable;
	private AdventureResult effect;

	public static AdventureResult REMEDY = new AdventureResult( "soft green echo eyedrop antidote", 1 );
	public static AdventureResult TINY_HOUSE = new AdventureResult( "tiny house", 1 );
	public static AdventureResult FOREST_TEARS = new AdventureResult( "forest tears", 1 );

	public UneffectRequest( AdventureResult effect )
	{
		super( isShruggable( effect.getName() ) ? "charsheet.php" : "uneffect.php" );

		this.effect = effect;
		this.effectId = StatusEffectDatabase.getEffectId( effect.getName() );
		this.isShruggable = isShruggable( effect.getName() );

		if ( isShruggable )
		{
			addFormField( "pwd" );
			addFormField( "action", "unbuff" );
			addFormField( "whichbuff", String.valueOf( effectId ) );
		}
		else
		{
			addFormField( "pwd" );
			addFormField( "using", "Yep." );
			addFormField( "whicheffect", String.valueOf( effectId ) );
		}
	}

	public static final boolean isShruggable( String effectName )
	{
		int id = ClassSkillsDatabase.getSkillId( effectToSkill( effectName ) );
		return id != -1 && ClassSkillsDatabase.isBuff( id );
	}

	/**
	 * Given the name of an effect, return the name of the skill that
	 * created that effect
	 *
	 * @param	effectName	The name of the effect
	 * @return	skill	The name of the skill
	 */

	public static String effectToSkill( String effectName )
	{
		if ( effectName.equalsIgnoreCase( "Polka of Plenty" ) || effectName.equalsIgnoreCase( "Power Ballad of the Arrowsmith" ) || effectName.equalsIgnoreCase( "Psalm of Pointiness" ) || effectName.equalsIgnoreCase( "Ode to Booze" ) )
			return "The " + effectName;

		if ( effectName.equalsIgnoreCase( "Empathy" ) )
			return "Empathy of the Newt";

		if ( effectName.equalsIgnoreCase( "Smooth Movements" ) )
			return "Smooth Movement";

		if ( effectName.equalsIgnoreCase( "Pasta Oneness" ) )
			return "Manicotti Meditation";

		if ( effectName.equalsIgnoreCase( "Saucemastery" ) )
			return "Sauce Contemplation";

		if ( effectName.equalsIgnoreCase( "Disco State of Mind" ) )
			return "Disco Aerobics";

		if ( effectName.equalsIgnoreCase( "Mariachi Mood" ) )
			return "Moxie of the Mariachi";

		return effectName;
	}

	public static String skillToEffect( String skillName )
	{
		if ( skillName.equals( "The Polka of Plenty" ) ||
			skillName.equals( "The Power Ballad of the Arrowsmith" ) ||
			skillName.equals( "The Psalm of Pointiness" ) ||
			skillName.equals( "The Ode to Booze" ) )
				return skillName.substring(4);

		if ( skillName.equals( "Empathy of the Newt" ) )
			return "Empathy";

		if ( skillName.equals( "Smooth Movement" ) )
			return "Smooth Movements";

		if ( skillName.equals( "Manicotti Meditation" ) )
			return "Pasta Oneness";

		if ( skillName.equals( "Sauce Contemplation" ) )
			return "Saucemastery";

		if ( skillName.equals( "Disco Aerobics" ) )
			return "Disco State of Mind";

		if ( skillName.equals( "Moxie of the Mariachi" ) )
			return "Mariachi Mood";

		return skillName;
	}

	public void run()
	{
		String action = MoodSettings.getDefaultAction( "gain_effect", effect.getName() );

		if ( !action.equals( "" ) && !action.startsWith( "uneffect" ) )
		{
			DEFAULT_SHELL.executeLine( action );
			return;
		}

		if ( !isShruggable )
		{
			if ( KoLCharacter.canInteract() )
			{
				DEFAULT_SHELL.executeLine( "acquire " + REMEDY.getName() );
			}
			else if ( !inventory.contains( REMEDY ) )
			{
				KoLmafia.updateDisplay( "You don't have any soft green fluffy martians." );
				return;
			}
		}

		KoLmafia.updateDisplay( isShruggable ? "Shrugging off your buff..." : "Using soft green whatever..." );
		super.run();
	}

	public void processResults()
	{
		// If it notifies you that the effect was removed, delete it
		// from the list of effects.

		if ( responseText != null && (isShruggable || responseText.indexOf( "Effect removed." ) != -1) )
		{
			activeEffects.remove( effect );

			if ( isShruggable )
				CharsheetRequest.parseStatus( responseText );
			else
				StaticEntity.getClient().processResult( REMEDY.getNegation() );

			KoLmafia.updateDisplay( effect.getName() + " removed." );

			if ( RequestFrame.instanceExists() )
				RequestFrame.refreshStatus();
			else
				RequestThread.postRequest( CharpaneRequest.getInstance() );
		}
		else if ( !isShruggable )
			KoLmafia.updateDisplay( "Failed to remove " + effect.getName() + "." );
	}
}

