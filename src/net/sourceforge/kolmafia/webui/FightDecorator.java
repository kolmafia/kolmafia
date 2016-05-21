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

package net.sourceforge.kolmafia.webui;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.kolmafia.KoLAdventure;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.MonsterData;
import net.sourceforge.kolmafia.RequestEditorKit;

import net.sourceforge.kolmafia.combat.MonsterStatusTracker;

import net.sourceforge.kolmafia.objectpool.AdventurePool;
import net.sourceforge.kolmafia.objectpool.ItemPool;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.request.FightRequest;

import net.sourceforge.kolmafia.session.DadManager;
import net.sourceforge.kolmafia.session.InventoryManager;

import net.sourceforge.kolmafia.utilities.StringUtilities;

public class FightDecorator
{
	private static final Pattern SKILL_FORM_PATTERN = Pattern.compile( "<form name=skill.*?</form>", Pattern.DOTALL );
	private static final Pattern SKILL_OPTION_PATTERN = Pattern.compile( "<option value=\\\"(\\d+)\\\".*?</option>", Pattern.DOTALL );

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
		String search = ">" + skill;
		if ( !oldForm.contains( search ) )
		{
			return;
		}

		// Found it.
		StringBuffer newForm = new StringBuffer( oldForm );

		// If a skill is already selected, deselect it
		StringUtilities.globalStringDelete( newForm, "selected" );

		// ... which may have moved the desired skill
		// Select the skill
		newForm.insert( newForm.indexOf( search ), " selected" );

		// Replace the skill form with the munged version
		StringUtilities.singleStringReplace( buffer, oldForm, newForm.toString() );
	}

	public static final void decorateMonster( final StringBuffer buffer )
	{
		// If we won the fight and got the volcano map, force a topmenu
		// refresh so that the "volcano" link is there.
		if ( buffer.indexOf( "WINWINWIN" ) != -1 &&
		     buffer.indexOf( "secret tropical island volcano lair map" ) != -1 )
		{
			RequestEditorKit.addTopmenuRefresh( buffer );
		}

		if ( !Preferences.getBoolean( "relayShowSpoilers" ) )
		{
			return;
		}

		MonsterData monster = MonsterStatusTracker.getLastMonster();
		if ( monster == null )
		{
			return;
		}
		String name = monster.getName().toLowerCase();
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
		if ( name.equals( "falls-from-sky" ) )
		{
			FightDecorator.decorateFallsFromSky( buffer );
			return;
		}
		if ( name.equals( "writing desk" ) )
		{
			FightDecorator.decorateWritingDesk( buffer );
			return;
		}
		if ( name.equals( "unusual construct" ) )
		{
			FightDecorator.decorateUnusualConstruct( buffer );
		}
		if ( name.equals( "performer of actions" ) || name.equals( "thinker of thoughts" ) || name.equals( "perceiver of sensations" ) )
		{
			FightDecorator.decorateMachineTunnelFight( name, buffer );
			return;
		}
		if ( name.equals( "source agent" ) )
		{
			FightDecorator.decorateSourceAgent( buffer );
			return;
		}
	}

	public static final void decorateLocation( final StringBuffer buffer )
	{
		if ( !Preferences.getBoolean( "relayShowSpoilers" ) )
		{
			return;
		}

		int adventure = KoLAdventure.lastAdventureId();

		switch ( adventure )
		{
		case AdventurePool.HAUNTED_KITCHEN:
			FightDecorator.decorateHauntedKitchen( buffer );
			break;

		case AdventurePool.TRAINING_SNOWMAN:
			FightDecorator.decorateSnojo( buffer );
			break;
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

	public static final void decorateMachineTunnelFight( final String monster, final StringBuffer buffer )
	{
		if ( monster.equals( "thinker of thoughts" ) )
		{
			RequestEditorKit.selectOption( buffer, "whichitem", String.valueOf( ItemPool.ABSTRACTION_ACTION ) );
		}
		else if ( monster.equals( "performer of actions" ) )
		{
			RequestEditorKit.selectOption( buffer, "whichitem", String.valueOf( ItemPool.ABSTRACTION_SENSATION ) );
		}
		else if ( monster.equals( "perceiver of sensations" ) )
		{
			RequestEditorKit.selectOption( buffer, "whichitem", String.valueOf( ItemPool.ABSTRACTION_THOUGHT ) );
		}
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

	private static final void decorateFallsFromSky( final StringBuffer buffer )
	{
		if ( buffer.indexOf( "begins to spin in a circle" ) != -1 )
		{
			FightDecorator.selectSkill( buffer, "Hide Under a Rock" );
			return;
		}

		if ( buffer.indexOf( "begins to paw at the ground" ) != -1 )
		{
			FightDecorator.selectSkill( buffer, "Dive Into a Puddle" );
			return;
		}

		if ( buffer.indexOf( "shuffles toward you" ) != -1 )
		{
			FightDecorator.selectSkill( buffer, "Hide Behind a Tree" );
			return;
		}
	}

	private static final void decorateWritingDesk( final StringBuffer buffer )
	{
		String indexString = "any necklaces.";
		int index = buffer.indexOf( indexString );
		if ( index == -1 )
		{
			return;
		}

		index += indexString.length();

		buffer.insert( index, " (" + Preferences.getInteger( "writingDesksDefeated" ) + "/5 defeated)" );
	}

	private static final void decorateSourceAgent( final StringBuffer buffer )
	{
		// Extract the "skill" form from the buffer
		Matcher skillForm = SKILL_FORM_PATTERN.matcher( buffer );
		if ( !skillForm.find() )
		{
			return;
		}
		Matcher option = SKILL_OPTION_PATTERN.matcher( skillForm.group( 0 ) );
		while( option.find() )
		{
			// Remove skills not starting with 21 as they can't be used
			int skill = StringUtilities.parseInt( option.group( 1 ) );
			if ( skill < 21000 || skill > 21999 )
			{
				StringUtilities.singleStringDelete( buffer, option.group( 0 ) );
			}
		}
	}

	private static final void decorateHauntedKitchen( final StringBuffer buffer )
	{
		if ( InventoryManager.hasItem( ItemPool.BILLIARDS_KEY ) )
		{
			// Don't show progress on the turn where the key is received
			return;
		}
		// The kitchen's resident flame-belching demon oven kicks into serious overdrive,
		// but you manage to tolerate the heat long enough to search through X drawers.

		// The garbage disposal turns itself on, filling the kitchen with an indescribably foul
		// odor, but you manage to tolerate it long enough to search through X drawers.
		String indexString = "drawers.";
		int index = buffer.indexOf( indexString );
		if ( index == -1 )
		{
			// You manage to dig through a single drawer looking for the key, but the
			// garbage disposal turns itself on, releasing a terrible, terrible smell.
			// It drives you back out into the hallway.
			indexString = "hallway.";
			index = buffer.indexOf( indexString );
			if ( index == -1 )
			{
				// You manage to dig through a single drawer looking for the key,
				// but the constant demonic flames belching out of the oven results
				// in the kitchen getting too hot for you, and you have to get out of it.
				indexString = "out of it.";
				index = buffer.indexOf( indexString );
				if ( index == -1 )
				{
					return;
				}
			}
		}

		index += indexString.length();

		int checked = Preferences.getInteger( "manorDrawerCount" );
		StringBuilder insertBuffer = new StringBuilder();
		insertBuffer.append( " (" ).append( checked ).append( "/21 searched" );
		if ( checked >= 21 )
		{
			insertBuffer.append( ", key next combat" );
		}
		insertBuffer.append( ")" );

		buffer.insert( index, insertBuffer );
	}

	private static final void decorateSnojo( final StringBuffer buffer )
	{
		String indexString = "Adventure Again (The X-32-F Combat Training Snowman)";
		int index = buffer.indexOf( indexString );
		if ( index == -1 ) return;

		index += indexString.length();

		int turns = Preferences.getInteger( "_snojoFreeFights" );
		StringBuilder insertBuffer = new StringBuilder();
		insertBuffer.append( " (" ).append( turns ).append( " free fight" ).append( turns == 1 ? "" : "s" ).append( " used)" );
		buffer.insert( index, insertBuffer );
	}

	private static final void decorateUnusualConstruct( final StringBuffer buffer )
	{
		Pattern COLOR_PATTERN = Pattern.compile( "(?:LANO|ROUTING) ([a-zA-Z]*)" );
		Matcher matcher = COLOR_PATTERN.matcher( buffer );
		String colorWord = null;
		if ( matcher.find() )
		{
			colorWord = matcher.group( 1 );
		}
		else
		{
			// Something went wrong
			RequestEditorKit.selectOption( buffer, "whichitem", String.valueOf( ItemPool.SEAL_TOOTH ) );
			return;
		}
		if ( colorWord.equals( "CHO" )
		  || colorWord.equals( "FUNI" )
		  || colorWord.equals( "TAZAK" )
		  || colorWord.equals( "CANARY" )
		  || colorWord.equals( "CITRINE" )
		  || colorWord.equals( "GOLD" )
		   )
		{
			RequestEditorKit.selectOption( buffer, "whichitem", String.valueOf( ItemPool.STRANGE_DISC_YELLOW ) );
			return;
		}

		if ( colorWord.equals( "CHAKRO" )
		  || colorWord.equals( "ZEVE" )
		  || colorWord.equals( "ZEVESTANO" )
		  || colorWord.equals( "CRIMSON" )
		  || colorWord.equals( "RUBY" )
		  || colorWord.equals( "VERMILLION" )
		   )
		{
			RequestEditorKit.selectOption( buffer, "whichitem", String.valueOf( ItemPool.STRANGE_DISC_RED ) );
			return;
		}

		if ( colorWord.equals( "BUPABU" )
		  || colorWord.equals( "PATA" )
		  || colorWord.equals( "SOM" )
		  || colorWord.equals( "OBSIDIAN" )
		  || colorWord.equals( "EBONY" )
		  || colorWord.equals( "JET" )
		   )
		{
			RequestEditorKit.selectOption( buffer, "whichitem", String.valueOf( ItemPool.STRANGE_DISC_BLACK ) );
			return;
		}

		if ( colorWord.equals( "BE" )
		  || colorWord.equals( "ZAKSOM" )
		  || colorWord.equals( "ZEVEBENI" )
		  || colorWord.equals( "JADE" )
		  || colorWord.equals( "VERDIGRIS" )
		  || colorWord.equals( "EMERALD" )
		   )
		{
			RequestEditorKit.selectOption( buffer, "whichitem", String.valueOf( ItemPool.STRANGE_DISC_GREEN ) );
			return;
		}

		if ( colorWord.equals( "BELA" )
		  || colorWord.equals( "BULAZAK" )
		  || colorWord.equals( "BU" )
		  || colorWord.equals( "FUFUGAKRO" )
		  || colorWord.equals( "ULTRAMARINE" )
		  || colorWord.equals( "SAPPHIRE" )
		  || colorWord.equals( "COBALT" )
		   )
		{
			RequestEditorKit.selectOption( buffer, "whichitem", String.valueOf( ItemPool.STRANGE_DISC_BLUE ) );
			return;
		}

		if ( colorWord.equals( "NIPA" )
		  || colorWord.equals( "PACHA" )
		  || colorWord.equals( "SOMPAPA" )
		  || colorWord.equals( "IVORY" )
		  || colorWord.equals( "ALABASTER" )
		  || colorWord.equals( "PEARL" )
		   )
		{
			RequestEditorKit.selectOption( buffer, "whichitem", String.valueOf( ItemPool.STRANGE_DISC_WHITE ) );
			return;
		}

		// If we made it this far, then we don't recognize the word yet.  Change to a bogus result to avoid
		// being misleading, until full support is added.

		RequestEditorKit.selectOption( buffer, "whichitem", String.valueOf( ItemPool.SEAL_TOOTH ) );
	}
}
