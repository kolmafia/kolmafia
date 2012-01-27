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

package net.sourceforge.kolmafia.objectpool;

import net.sourceforge.kolmafia.AdventureResult;

import net.sourceforge.kolmafia.persistence.EffectDatabase;

public class EffectPool
{
	public static final String ON_THE_TRAIL = "On the Trail";
	public static final String EAU_DE_TORTUE= "Eau de Tortue";

	public static final String ODE = "Ode to Booze";
	public static final String MILK = "Got Milk";

	public static final String FORM_OF_BIRD = "Form of...Bird!";
	public static final String SHAPE_OF_MOLE = "Shape of...Mole!";
	public static final String FORM_OF_ROACH = "Form of...Cockroach!";

	public static final String HAIKU_STATE_OF_MIND = "Haiku State of Mind";
	public static final String JUST_THE_BEST_ANAPESTS = "Just the Best Anapests";

	public static final String HALF_ASTRAL = "Half-Astral";
	public static final String PERFUME = "Knob Goblin Perfume";
	public static final String ABSINTHE = "Absinthe-Minded";
	public static final String HYDRATED = "Ultrahydrated";

	public static final String EXPERT_OILINESS = "Expert Oiliness";
	public static final String SLIPPERY_OILINESS = "Slippery Oiliness";
	public static final String STABILIZING_OILINESS = "Stabilizing Oiliness";

	public static final String ASTRAL_SHELL = "Astral Shell";
	public static final String ELEMENTAL_SPHERE = "Elemental Saucesphere";
	public static final String GHOSTLY_SHELL = "Ghostly Shell";

	public static final String PURPLE_TONGUE = "Purple Tongue";
	public static final String GREEN_TONGUE = "Green Tongue";
	public static final String ORANGE_TONGUE = "Orange Tongue";
	public static final String RED_TONGUE = "Red Tongue";
	public static final String BLUE_TONGUE = "Blue Tongue";
	public static final String BLACK_TONGUE = "Black Tongue";

	public static final String BLUE_CUPCAKE = "Cupcake of Choice";
	public static final String GREEN_CUPCAKE = "The Cupcake of Wrath";
	public static final String ORANGE_CUPCAKE = "Shiny Happy Cupcake";
	public static final String PURPLE_CUPCAKE = "Tiny Bubbles in the Cupcake";
	public static final String PINK_CUPCAKE = "Your Cupcake Senses Are Tingling";

	public static final String FISHY = "Fishy";
	public static final String BEE_SMELL = "Float Like a Butterfly, Smell Like a Bee";

	public static final String EARTHEN_FIST = "Earthen Fist";
	public static final String HARDLY_POISONED = "Hardly Poisoned at All";
	public static final String INIGO = "Inigo's Incantation of Inspiration";
	public static final String TELEPORTITIS = "Teleportitis";
	public static final String TRANSPONDENT = "Transpondent";
	public static final String WUSSINESS = "Wussiness";
	
	public static final String DOWN_THE_RABBIT_HOLE = "Down the Rabbit Hole";

	public static final int HARDLY_POISONED_AT_ALL_ID = 8;
	public static final int GOOFBALL_WITHDRAWAL_ID = 111;
	public static final int CURSED_BY_RNG_ID = 217;
	public static final int EAU_DE_TORTUE_ID = 263;
	public static final int MAJORLY_POISONED_ID = 264;
	public static final int A_LITTLE_BIT_POISONED_ID = 282;
	public static final int SOMEWHAT_POISONED_ID = 283;
	public static final int REALLY_QUITE_POISONED_ID = 284;
	public static final int TOAD_IN_THE_HOLE_ID = 436;
	public static final int CORSICAN_BLESSING = 460;
	public static final int FORM_OF_BIRD_ID = 511;
	public static final int COVERED_IN_SLIME_ID = 633;
	public static final int HAUNTING_LOOKS_ID = 937;
	public static final int DEAD_SEXY_ID = 938;
	public static final int VAMPIN_ID = 939;
	public static final int YIFFABLE_YOU_ID = 940;
	public static final int BONE_US_ROUND_ID = 941;

	public static final AdventureResult get( final int effectId )
	{
		return EffectPool.get( EffectDatabase.getEffectName( effectId ) );
	}

	public static final AdventureResult get( final String effectName )
	{
		return new AdventureResult( effectName, 1, true );
	}

	public static final AdventureResult get( final String effectName, final int turns )
	{
		return new AdventureResult( effectName, turns, true );
	}
}
