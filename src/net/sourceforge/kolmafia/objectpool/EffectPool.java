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

package net.sourceforge.kolmafia.objectpool;

import net.sourceforge.kolmafia.AdventureResult;

public class EffectPool
{
	public static final int BEATEN_UP = 7;
	public static final int HARDLY_POISONED = 8;
	public static final int PERFUME = 9;
	public static final int GHOSTLY_SHELL = 18;
	public static final int EXPERT_OILINESS = 37;
	public static final int WUSSINESS = 43;
	public static final int ASTRAL_SHELL = 52;
	public static final int ELEMENTAL_SPHERE = 53;
	public static final int TELEPORTITIS = 58;
	public static final int ODE = 71;
	public static final int STABILIZING_OILINESS = 100;
	public static final int SLIPPERY_OILINESS = 101;
	public static final int GOOFBALL_WITHDRAWAL = 111;
	public static final int PURPLE_TONGUE = 139;
	public static final int GREEN_TONGUE = 140;
	public static final int ORANGE_TONGUE = 141;
	public static final int RED_TONGUE = 142;
	public static final int BLUE_TONGUE = 143;
	public static final int BLACK_TONGUE = 144;
	public static final int HALF_ASTRAL = 183;
	public static final int BLUE_CUPCAKE = 184;
	public static final int ORANGE_CUPCAKE = 185;
	public static final int PURPLE_CUPCAKE = 186;
	public static final int PINK_CUPCAKE = 187;
	public static final int GREEN_CUPCAKE = 188;
	public static final int HOTFORM = 189;
	public static final int COLDFORM = 190;
	public static final int SPOOKYFORM = 191;
	public static final int STENCHFORM = 192;
	public static final int SLEAZEFORM = 193;
	public static final int CURSED_BY_RNG = 217;
	public static final int MILK = 211;
	public static final int CHALKY_HAND = 221;
	public static final int EAU_DE_TORTUE = 263;
	public static final int MAJORLY_POISONED = 264;
	public static final int HYDRATED = 275;
	public static final int A_LITTLE_BIT_POISONED = 282;
	public static final int SOMEWHAT_POISONED = 283;
	public static final int REALLY_QUITE_POISONED = 284;
	public static final int ON_THE_TRAIL = 331;
	public static final int ABSINTHE = 357;
	public static final int TOAD_IN_THE_HOLE = 436;
	public static final int SOUL_CRUSHING_HEADACHE = 465;
	public static final int FORM_OF_ROACH = 509;
	public static final int SHAPE_OF_MOLE = 510;
	public static final int FORM_OF_BIRD = 511;
	public static final int THE_BALLAD_OF_RICHIE_THINGFINDER = 530;
	public static final int BENETTONS_MEDLEY_OF_DIVERSITY = 531;
	public static final int ELRONS_EXPLOSIVE_ETUDE = 532;
	public static final int CHORALE_OF_COMPANIONSHIP = 533;
	public static final int PRELUDE_OF_PRECISION = 534;
	public static final int HAIKU_STATE_OF_MIND = 548;
	public static final int FISHY = 549;
	public static final int DONHOS_BUBBLY_BALLAD = 614;
	public static final int COATED_IN_SLIME = 633;
	public static final int IRON_PALMS = 709;
	public static final int INIGOS = 716;
	public static final int DOWN_THE_RABBIT_HOLE = 725;
	public static final int SUPER_SKILL = 777;
	public static final int SUPER_STRUCTURE = 778;
	public static final int SUPER_VISION = 779;
	public static final int SUPER_SPEED = 780;
	public static final int SUPER_ACCURACY = 781;
	public static final int EVERYTHING_LOOKS_YELLOW = 790;
	public static final int EVERYTHING_LOOKS_BLUE = 791;
	public static final int EVERYTHING_LOOKS_RED = 792;
	public static final int KUNG_FU_FIGHTING = 806;
	public static final int WET_WILLIED = 838;
	public static final int BEE_SMELL = 845;
	public static final int TRANSPONDENT = 846;
	public static final int TIMER1 = 873;
	public static final int TIMER2 = 874;
	public static final int TIMER3 = 875;
	public static final int TIMER4 = 876;
	public static final int TIMER5 = 877;
	public static final int TIMER6 = 878;
	public static final int TIMER7 = 879;
	public static final int TIMER8 = 880;
	public static final int TIMER9 = 881;
	public static final int TIMER10 = 882;
	public static final int EARTHEN_FIST = 907;
	public static final int GARISH = 918;
	public static final int HAUNTING_LOOKS = 937;
	public static final int DEAD_SEXY = 938;
	public static final int VAMPIN = 939;
	public static final int YIFFABLE_YOU = 940;
	public static final int BONE_US_ROUND = 941;
	public static final int JUST_THE_BEST_ANAPESTS = 1003;
	public static final int GLORIOUS_LUNCH = 1005;
	public static final int OVERCONFIDENT = 1011;
	public static final int DEEP_TAINTED_MIND = 1217;
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
	public static final int GRAND_BLESSING_OF_THE_WAR_SNAPPER = 1417;
	public static final int GLORIOUS_BLESSING_OF_THE_WAR_SNAPPER = 1418;
	public static final int BLESSING_OF_SHE_WHO_WAS = 1419;
	public static final int GRAND_BLESSING_OF_SHE_WHO_WAS = 1420;
	public static final int GLORIOUS_BLESSING_OF_SHE_WHO_WAS = 1421;
	public static final int BLESSING_OF_THE_STORM_TORTOISE = 1422;
	public static final int GRAND_BLESSING_OF_THE_STORM_TORTOISE = 1423;
	public static final int GLORIOUS_BLESSING_OF_THE_STORM_TORTOISE = 1424;
	public static final int DISTAIN_OF_THE_WAR_SNAPPER = 1425;
	public static final int DISTAIN_OF_SHE_WHO_WAS = 1426;
	public static final int DISTAIN_OF_THE_STORM_TORTOISE = 1427;
	public static final int SPIRIT_PARIAH = 1431;
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
	public static final int SOME_PIGS = 1640;
	public static final int CONFIDENCE = 1791;


	public static final AdventureResult get( final int effectId )
	{
		return new AdventureResult( effectId, 1, true );
	}

	public static final AdventureResult get( final int effectId, final int turns )
	{
		return new AdventureResult( effectId, turns, true );
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
