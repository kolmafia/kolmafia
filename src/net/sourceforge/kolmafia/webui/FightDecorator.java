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

package net.sourceforge.kolmafia.webui;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.kolmafia.combat.MonsterStatusTracker;

import net.sourceforge.kolmafia.request.FightRequest;

import net.sourceforge.kolmafia.session.DadManager;

import net.sourceforge.kolmafia.utilities.StringUtilities;

public class FightDecorator
{
	private static final Pattern SKILL_FORM_PATTERN = Pattern.compile( "<form name=skill.*?</form>", Pattern.DOTALL );

	public static final void selectSkill( final StringBuffer buffer, final String skill )
	{
		// Extract the "skill" form from the buffer
		Matcher matcher = SKILL_FORM_PATTERN.matcher( buffer );
		if ( !matcher.find() )
		{
			return;
		}

		// Find the desired skill
		String oldForm = matcher.group( 0 );
		int index = oldForm.indexOf( ">" + skill );
		if ( index == -1 )
		{
			return;
		}

		// Found it.
		StringBuffer newForm = new StringBuffer( oldForm );

		// If a skill is already selected, deselect it
		StringUtilities.globalStringDelete( newForm, "selected" );

		// Select the skill
		newForm.insert( index, " selected" );

		// Replace the skill form with the munged version
		StringUtilities.singleStringReplace( buffer, oldForm, newForm.toString() );
	}

	public static final void decorate( final StringBuffer buffer )
	{
		String name = MonsterStatusTracker.getLastMonsterName().toLowerCase();
		if ( name.equals( "dad sea monkee" ) )
		{
			FightDecorator.decorateDadSeaMonkee( buffer );
			return;
		}
		if ( name.endsWith( "balldodger" ) )
		{
			FightDecorator.decorateBallDodger( buffer );
			return;
		}
		if ( name.endsWith( "bladeswitcher" ) )
		{
			FightDecorator.decorateBladeSwitcher( buffer );
			return;
		}
		if ( name.endsWith( "netdragger" ) )
		{
			FightDecorator.decorateNetDragger( buffer );
			return;
		}
	}

	private static final void decorateDadSeaMonkee( final StringBuffer buffer )
	{
		int round = FightRequest.currentRound;
		if ( round < 1 || round > 10 )
		{
			return;
		}

		DadManager.Element element = DadManager.weakness( round );
		if ( element == DadManager.Element.NONE )
		{
			return;
		}

		String spell = DadManager.elementToSpell( element );
		if ( spell == null )
		{
			return;
		}

		FightDecorator.selectSkill( buffer, spell );
	}

	private static final void decorateBallDodger( final StringBuffer buffer )
	{
		// Looks like he's trying to gain an advantage over you...
		if ( buffer.indexOf( "<b>gain</b>" ) != -1 )
		{
			FightDecorator.selectSkill( buffer, "Net Gain" );
			return;
		}

		// He gets a crazy look in his eyes -- like he's about to experience a serious loss of control...
		if ( buffer.indexOf( "<b>loss</b>" ) != -1 )
		{
			FightDecorator.selectSkill( buffer, "Net Loss" );
			return;
		}

		// His facial features take on an ominous neutrality.
		if ( buffer.indexOf( "<b>neutrality</b>" ) != -1 )
		{
			FightDecorator.selectSkill( buffer, "Net Neutrality" );
			return;
		}
	}

	private static final void decorateBladeSwitcher( final StringBuffer buffer )
	{
		// He begins to bust an especially dope move with his switchblade.
		if ( buffer.indexOf( "<b>bust</b>" ) != -1 )
		{
			FightDecorator.selectSkill( buffer, "Ball Bust" );
			return;
		}

		// He pauses to wipe the sweat from his brow.
		if ( buffer.indexOf( "<b>sweat</b>" ) != -1 )
		{
			FightDecorator.selectSkill( buffer, "Ball Sweat" );
			return;
		}

		// He pulls a little bottle of oil out of his sack and applies it to his switchblade.
		if ( buffer.indexOf( "<b>sack</b>" ) != -1 )
		{
			FightDecorator.selectSkill( buffer, "Ball Sack" );
			return;
		}
	}

	private static final void decorateNetDragger( final StringBuffer buffer )
	{
		// He starts to fold his net up into some sort of a sling.
		if ( buffer.indexOf( "<b>sling</b>" ) != -1 )
		{
			FightDecorator.selectSkill( buffer, "Blade Sling" );
			return;
		}

		// He rolls his net up and draws it back like a baseball bat.
		if ( buffer.indexOf( "<b>rolls</b>" ) != -1 )
		{
			FightDecorator.selectSkill( buffer, "Blade Roller" );
			return;
		}

		// If you were a runner, you'd be tempted to run right now...
		if ( buffer.indexOf( "<b>runner</b>" ) != -1 )
		{
			FightDecorator.selectSkill( buffer, "Blade Runner" );
			return;
		}
	}
}
