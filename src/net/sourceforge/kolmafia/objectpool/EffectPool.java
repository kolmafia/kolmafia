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

public class EffectPool
{
	public enum Effect
	{
			ON_THE_TRAIL( "On the Trail", 331 ),
			EAU_DE_TORTUE( "Eau de Tortue", 263 ),

			ODE( "Ode to Booze", 71 ),
			MILK( "Got Milk", 211 ),
			GLORIOUS_LUNCH( "Song of the Glorious Lunch", 1005 ),

			FORM_OF_BIRD( "Form of...Bird!", 511 ),
			SHAPE_OF_MOLE( "Shape of...Mole!", 510 ),
			FORM_OF_ROACH( "Form of...Cockroach!", 509 ),

			HAIKU_STATE_OF_MIND( "Haiku State of Mind", 548 ),
			JUST_THE_BEST_ANAPESTS( "Just the Best Anapests", 1003 ),

			HALF_ASTRAL( "Half-Astral", 183 ),
			PERFUME( "Knob Goblin Perfume", 9 ),
			ABSINTHE( "Absinthe-Minded", 357 ),
			HYDRATED( "Ultrahydrated", 275 ),

			EXPERT_OILINESS( "Expert Oiliness", 37 ),
			SLIPPERY_OILINESS( "Slippery Oiliness", 101 ),
			STABILIZING_OILINESS( "Stabilizing Oiliness", 100 ),

			ASTRAL_SHELL( "Astral Shell", 52 ),
			ELEMENTAL_SPHERE( "Elemental Saucesphere", 53 ),
			GHOSTLY_SHELL( "Ghostly Shell", 18 ),

			PURPLE_TONGUE( "Purple Tongue", 139 ),
			GREEN_TONGUE( "Green Tongue", 140 ),
			ORANGE_TONGUE( "Orange Tongue", 141 ),
			RED_TONGUE( "Red Tongue", 142 ),
			BLUE_TONGUE( "Blue Tongue", 143 ),
			BLACK_TONGUE( "Black Tongue", 144 ),

			BLUE_CUPCAKE( "Cupcake of Choice", 184 ),
			GREEN_CUPCAKE( "The Cupcake of Wrath", 188 ),
			ORANGE_CUPCAKE( "Shiny Happy Cupcake", 185 ),
			PURPLE_CUPCAKE( "Tiny Bubbles in the Cupcake", 186 ),
			PINK_CUPCAKE( "Your Cupcake Senses Are Tingling", 187 ),

			FISHY( "Fishy", 549 ),
			BEE_SMELL( "Float Like a Butterfly, Smell Like a Bee", 845 ),

			EARTHEN_FIST( "Earthen Fist", 907 ),
			HARDLY_POISONED( "Hardly Poisoned at All", 8 ),
			INIGO( "Inigo's Incantation of Inspiration", 716 ),
			TELEPORTITIS( "Teleportitis", 58 ),
			TRANSPONDENT( "Transpondent", 846 ),
			WUSSINESS( "Wussiness", 43 ),

			DOWN_THE_RABBIT_HOLE( "Down the Rabbit Hole", 725 ),

			GOOFBALL_WITHDRAWAL( "Goofball Withdrawal", 111 ),
			CURSED_BY_RNG( "Cursed by The RNG", 217 ),
			MAJORLY_POISONED( "Majorly Poisoned", 264 ),
			A_LITTLE_BIT_POISONED( "A Little Bit Poisoned", 282 ),
			SOMEWHAT_POISONED( "Somewhat Poisoned", 283 ),
			REALLY_QUITE_POISONED( "Really Quite Poisoned", 284 ),
			TOAD_IN_THE_HOLE( "Toad In The Hole", 436 ),
			CORSICAN_BLESSING( "Brother Corsican's Blessing", 460 ),
			COATED_IN_SLIME( "Coated in Slime", 633 ),
			GARISH( "Gar-ish", 918 ),
			HAUNTING_LOOKS( "Haunting Looks", 937 ),
			DEAD_SEXY( "Dead Sexy", 938 ),
			VAMPIN( "Vampin'", 939 ),
			YIFFABLE_YOU( "Yiffable You", 940 ),
			BONE_US_ROUND( "The Bone Us Round", 941 ),
			OVERCONFIDENT( "Overconfident", 1011 );

		private final String name;
		private final int id;

		private Effect( String name, int id )
		{
			this.name = name;
			this.id = id;
		}

		public String effectName()
		{
			return this.name;
		}

		public int effectId()
		{
			return this.id;
		}
	}

	public static final int GOOFBALL_WITHDRAWAL_ID = 111;
	public static final int CURSED_BY_RNG_ID = 217;
	public static final int EAU_DE_TORTUE_ID = 263;
	public static final int FORM_OF_BIRD_ID = 511;
	public static final int COVERED_IN_SLIME_ID = 633;

	public static final AdventureResult get( final String effectName )
	{
		return new AdventureResult( effectName, 1, true );
	}

	public static final AdventureResult get( final String effectName, final int turns )
	{
		return new AdventureResult( effectName, turns, true );
	}

	public static AdventureResult get( final Effect effect )
	{
		return get( effect.effectName() );
	}
}
