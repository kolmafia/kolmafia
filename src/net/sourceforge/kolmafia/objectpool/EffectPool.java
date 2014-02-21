/**
 * Copyright (c) 2005-2014, KoLmafia development team
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

		WAR_BLESSING_1( "Blessing of the War Snapper", 1416 ),
		WAR_BLESSING_2( "Grand Blessing of the War Snapper", 1417 ),
		WAR_BLESSING_3( "Glorious Blessing of the War Snapper", 1418 ),
		WAR_AVATAR( "Avatar of the War Snapper", 1432 ),
		SHE_WHO_WAS_BLESSING_1( "Blessing of She-Who-Was", 1419 ),
		SHE_WHO_WAS_BLESSING_2( "Grand Blessing of She-Who-Was", 1420 ),
		SHE_WHO_WAS_BLESSING_3( "Glorious Blessing of She-Who-Was", 1421 ),
		SHE_WHO_WAS_AVATAR( "Avatar of She-Who-Was", 1433 ),
		STORM_BLESSING_1( "Blessing of the Storm Tortoise", 1422 ),
		STORM_BLESSING_2( "Grand Blessing of the Storm Tortoise", 1423 ),
		STORM_BLESSING_3( "Glorious Blessing of the Storm Tortoise", 1424 ),
		STORM_AVATAR( "Avatar of the Storm Tortoise", 1434 ),
		SPIRIT_PARIAH( "Spirit Pariah", 1431 ),
			
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

		HOTFORM( "Hotform", 189 ),
		COLDFORM( "Coldform", 190 ),
		SPOOKYFORM( "Spookyform", 191 ),
		STENCHFORM( "Stenchform", 192 ),
		SLEAZEFORM( "Sleazeform", 193 ),

		CHALKY_HAND( "Chalky Hand", 221),

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
		REALLY_DEEP_BREATH( "Really Deep Breath", 623 ),
		COATED_IN_SLIME( "Coated in Slime", 633 ),
		KUNG_FU_FIGHTING( "Kung Fu Fighting", 806 ),
		WET_WILLIED( "Wet Willied", 838),
		GARISH( "Gar-ish", 918 ),
		HAUNTING_LOOKS( "Haunting Looks", 937 ),
		DEAD_SEXY( "Dead Sexy", 938 ),
		VAMPIN( "Vampin'", 939 ),
		YIFFABLE_YOU( "Yiffable You", 940 ),
		BONE_US_ROUND( "The Bone Us Round", 941 ),
		OVERCONFIDENT( "Overconfident", 1011 ),
		BORED_WITH_EXPLOSIONS( "Bored With Explosions", 1557 );

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

	public static final int SOUL_CRUSHING_HEADACHE_ID = 465;
	public static final int THE_BALLAD_OF_RICHIE_THINGFINDER = 530;
	public static final int BENETTONS_MEDLEY_OF_DIVERSITY = 531;
	public static final int ELRONS_EXPLOSIVE_ETUDE = 532;
	public static final int CHORALE_OF_COMPANIONSHIP = 533;
	public static final int PRELUDE_OF_PRECISION = 534;
	public static final int DONHOS_BUBBLY_BALLAD = 614;
	public static final int COVERED_IN_SLIME_ID = 633;
	public static final int INIGOS_INCANTATION_OF_INSPIRATION = 716;
	public static final int EVERYTHING_LOOKS_YELLOW_ID = 790;
	public static final int EVERYTHING_LOOKS_BLUE_ID = 791;
	public static final int EVERYTHING_LOOKS_RED_ID = 792;
	public static final int TIMER1_ID = 873;
	public static final int TIMER2_ID = 874;
	public static final int TIMER3_ID = 875;
	public static final int TIMER4_ID = 876;
	public static final int TIMER5_ID = 877;
	public static final int TIMER6_ID = 878;
	public static final int TIMER7_ID = 879;
	public static final int TIMER8_ID = 880;
	public static final int TIMER9_ID = 881;
	public static final int TIMER10_ID = 882;
	public static final int OVERCONFIDENT = 1011;
	public static final int DEEP_TAINTED_MIND_ID = 1217;
	public static final int TOUCHED_BY_A_GHOST = 1276;
	public static final int CHILLED_TO_THE_BONE = 1277;
	public static final int NAUSEATED = 1278;
	public static final int CURSE_OF_CLUMSINESS = 1313;
	public static final int CURSE_OF_DULLNESS = 1308;
	public static final int CURSE_OF_EXPOSURE = 1306;
	public static final int CURSE_OF_FORGETFULNESS = 1311;
	public static final int CURSE_OF_HOLLOWNESS = 1304;
	public static final int CURSE_OF_IMPOTENCE = 1307;
	public static final int CURSE_OF_LONELINESS = 1314;
	public static final int CURSE_OF_MISFORTUNE = 1312;
	public static final int CURSE_OF_SLUGGISHNESS = 1310;
	public static final int CURSE_OF_VULNERABILITY = 1305;
	public static final int CURSE_OF_WEAKNESS = 1309;
	public static final int BLESSING_OF_THE_WAR_SNAPPER = 1416;
	public static final int BLESSING_OF_SHE_WHO_WAS = 1419;
	public static final int BLESSING_OF_THE_STORM_TORTOISE = 1422;
	public static final int DISTAIN_OF_THE_WAR_SNAPPER = 1425;
	public static final int DISTAIN_OF_SHE_WHO_WAS = 1426;
	public static final int DISTAIN_OF_THE_STORM_TORTOISE = 1427;
	public static final int SPIRIT_PARIAH_ID = 1431;
	public static final int AVATAR_OF_THE_WAR_SNAPPER = 1432;
	public static final int AVATAR_OF_SHE_WHO_WAS = 1433;
	public static final int AVATAR_OF_THE_STORM_TORTOISE = 1434;
	public static final int BOON_OF_THE_WAR_SNAPPER = 1435;
	public static final int BOON_OF_SHE_WHO_WAS = 1436;
	public static final int BOON_OF_THE_STORM_TORTOISE = 1437;
	public static final int FLIMSY_SHIELD_OF_THE_PASTALORD = 1443;
	public static final int SHIELD_OF_THE_PASTALORD = 1444;
	public static final int BLOODY_POTATO_BITS = 1445;
	public static final int SLINKING_NOODLE_GLOB = 1446;
	public static final int WHISPERING_STRANDS = 1447;
	public static final int MACARONI_COATING = 1448;
	public static final int PENNE_FEDORA = 1449;
	public static final int PASTA_EYEBALL = 1450;
	public static final int SPICE_HAZE = 1451;
	public static final int BLOOD_SUGAR_SAUCE_MAGIC = 1458;
	public static final int SOULERSKATES = 1465;
	public static final int UNMUFFLED = 1545;
	public static final int MUFFLED = 1546;
	public static final int BORED_WITH_EXPLOSIONS = 1557;

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
