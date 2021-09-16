/*
 * Copyright (c) 2005-2021, KoLmafia development team
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
	public static final int SLEEPY = 2;
	public static final int CONFUSED = 3;
	public static final int EMBARRASSED = 4;
	public static final int BEATEN_UP = 7;
	public static final int HARDLY_POISONED = 8;
	public static final int KNOB_GOBLIN_PERFUME = 9;
	public static final int BLOODY_HAND = 15;
	public static final int LEASH_OF_LINGUINI = 16;
	public static final int GHOSTLY_SHELL = 18;
	public static final int EXPERT_OILINESS = 37;
	public static final int HERNIA = 39;
	public static final int SUNBURNED = 42;
	public static final int WUSSINESS = 43;
	public static final int EMPATHY = 50;
	public static final int ASTRAL_SHELL = 52;
	public static final int ELEMENTAL_SPHERE = 53;
	public static final int RAINY_SOUL_MIASMA = 57;
	public static final int TELEPORTITIS = 58;
	public static final int ODE = 71;
	public static final int MISSING_FINGERS = 80;
	public static final int STABILIZING_OILINESS = 100;
	public static final int SLIPPERY_OILINESS = 101;
	public static final int CORRODED_WEAPON = 105;
	public static final int GOOFBALL_WITHDRAWAL = 111;
	public static final int APATHY = 115;
	public static final int PURPLE_TONGUE = 139;
	public static final int GREEN_TONGUE = 140;
	public static final int ORANGE_TONGUE = 141;
	public static final int RED_TONGUE = 142;
	public static final int BLUE_TONGUE = 143;
	public static final int BLACK_TONGUE = 144;
	public static final int SPIRIT_OF_CAYENNE = 167;
	public static final int SPIRIT_OF_PEPPERMINT = 168;
	public static final int SPIRIT_OF_GARLIC = 169;
	public static final int SPIRIT_OF_WORMWOOD = 170;
	public static final int SPIRIT_OF_BACON_GREASE = 171;
	public static final int HEAVY_PETTING = 176;
	public static final int TEMPORARY_BLINDNESS = 180;
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
	public static final int MILK = 211;
	public static final int CURSED_BY_RNG = 217;
	public static final int CHALKY_HAND = 221;
	public static final int DREAMS_AND_LIGHTS = 223;
	public static final int EAU_DE_TORTUE = 263;
	public static final int MAJORLY_POISONED = 264;
	public static final int TENUOUS_GRIP_ON_REALITY = 265;
	public static final int TURNED_INTO_A_SKELETON = 266;
	public static final int BARKING_DOGS = 267;
	public static final int PRESTIDIGYSFUNCTION = 268;
	public static final int HEART_OF_GREEN = 274;
	public static final int HYDRATED = 275;
	public static final int TANGLED_UP = 281;
	public static final int A_LITTLE_BIT_POISONED = 282;
	public static final int SOMEWHAT_POISONED = 283;
	public static final int REALLY_QUITE_POISONED = 284;
	public static final int FILTHWORM_LARVA_STENCH = 285;
	public static final int FILTHWORM_DRONE_STENCH = 286;
	public static final int FILTHWORM_GUARD_STENCH = 287;
	public static final int LIGHT_HEADED = 288;
	public static final int TETANUS = 292;
	public static final int HALF_EATEN_BRAIN = 293;
	public static final int SOCIALISMYDIA = 295;
	public static final int AXE_WOUND = 296;
	public static final int AMNESIA = 297;
	public static final int GRILLED = 298;
	public static final int THE_DISEASE = 299;
	public static final int CUNCTATITIS = 301;
	public static final int HEALTHY_GREEN_GLOW = 307;
	public static final int FIREPROOF_LIPS = 317;
	public static final int FEVER_FROM_THE_FLAVOR = 318;
	public static final int HYPHEMARIFFIC = 319;
	public static final int CANT_SMELL_NOTHING = 320;
	public static final int HYPEROFFENDED = 321;
	public static final int ON_THE_TRAIL = 331;
	public static final int BESTIAL_SYMPATHY = 342;
	public static final int RED_DOOR_SYNDROME = 354;
	public static final int ABSINTHE = 357;
	public static final int MANS_WORST_ENEMY = 368;
	public static final int MISSING_KIDNEY = 386;
	public static final int DUHHH = 387;
	public static final int AFFRONTED_DECENCY = 388;
	public static final int MAID_DISSERVICE = 389;
	public static final int MINIONED = 390;
	public static final int ENHANCED_ARCHAEOLOGIST = 391;
	public static final int CHRONOLOGICALLY_PUMMELLED = 392;
	public static final int ANIMAL_EXPLOITER = 393;
	public static final int SCENT_OF_A_KITCHEN_ELF = 394;
	public static final int ONCE_BITTEN_TWICE_SHY = 395;
	public static final int THE_RAGE = 396;
	public static final int SHAMED_AND_MANIPULATED = 397;
	public static final int DRUMMED_OUT = 398;
	public static final int DREADLOCKED = 399;
	public static final int HORNSWAGGLED = 400;
	public static final int THIRD_EYE_BLIND = 401;
	public static final int THE_VITUS_VIRUS = 402;
	public static final int YOUR_NUMBER_1_PROBLEM = 403;
	public static final int BRAAAINS = 404;
	public static final int BRAAAAAAINS = 405;
	public static final int RE_POSSESSED = 406;
	public static final int BURNING_HEART = 407;
	public static final int FROSTBITTEN = 408;
	public static final int FREAKED_OUT = 409;
	public static final int GUANIFIED = 410;
	public static final int BASTED = 411;
	public static final int RAVING_LUNATIC = 412;
	public static final int SOLAR_FLAIR = 413;
	public static final int HYPERBOLIC_HYPOTHERMIA = 414;
	public static final int SLIMED = 415;
	public static final int TAR_STRUCK = 416;
	public static final int GREASED = 417;
	public static final int BATIGUE = 418;
	public static final int CUPSHOTTEN = 419;
	public static final int DEEP_FRIED = 420;
	public static final int PAW_SWAP = 421;
	public static final int SIDE_AFFECTATION = 422;
	public static final int SHIRTLESS_IN_SEATTLE = 423;
	public static final int SCARED_STIFF = 424;
	public static final int MIDGETIZED = 425;
	public static final int SYNTHESIZED = 426;
	public static final int FLARED_NOSTRILS = 432;
	public static final int EASILY_EMBARRASSED = 433;
	public static final int ALL_COVERED_IN_WHATSIT = 434;
	public static final int BEER_IN_YOUR_SHOES = 435;
	public static final int TOAD_IN_THE_HOLE = 436;
	public static final int STRANGULATED = 437;
	public static final int DANG_NEAR_CUT_IN_HALF = 445;
	public static final int A_REVOLUTION_IN_YOUR_MOUTH = 453;
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
	public static final int THE_COLORS = 584;
	public static final int CLUMSY = 612;
	public static final int DONHOS_BUBBLY_BALLAD = 614;
	public static final int COATED_IN_SLIME = 633;
	public static final int EXISTENTIAL_TORMENT = 675;
	public static final int IRON_PALMS = 709;
	public static final int INIGOS = 716;
	public static final int DOWN_THE_RABBIT_HOLE = 725;
	public static final int DEADENED_PALATE = 774;
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
	public static final int TASTE_THE_INFERNO = 839;
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
	public static final int REFINED_PALATE = 910;
	public static final int GARISH = 918;
	public static final int NATURAL_1 = 930;
	public static final int HAUNTING_LOOKS = 937;
	public static final int DEAD_SEXY = 938;
	public static final int VAMPIN = 939;
	public static final int YIFFABLE_YOU = 940;
	public static final int BONE_US_ROUND = 941;
	public static final int DIS_ABLED = 963;
	public static final int JUST_THE_BEST_ANAPESTS = 1003;
	public static final int GLORIOUS_LUNCH = 1005;
	public static final int OVERCONFIDENT = 1011;
	public static final int STONE_FACED = 1031;
	public static final int N_SPATIAL_VISION = 1035;
	public static final int CONSUMED_BY_FEAR = 1146;
	public static final int DEEP_TAINTED_MIND = 1217;
	public static final int TOUCHED_BY_A_GHOST = 1276;
	public static final int CHILLED_TO_THE_BONE = 1277;
	public static final int NAUSEATED = 1278;
	public static final int FIRST_BLOOD_KIWI = 1302;
	public static final int SHEPHERDS_BREATH = 1303;
	public static final int CURSE_OF_HOLLOWNESS = 1304;
	public static final int CURSE_OF_VULNERABILITY = 1305;
	public static final int CURSE_OF_EXPOSURE = 1306;
	public static final int CURSE_OF_IMPOTENCE = 1307;
	public static final int CURSE_OF_DULLNESS = 1308;
	public static final int CURSE_OF_WEAKNESS = 1309;
	public static final int CURSE_OF_SLUGGISHNESS = 1310;
	public static final int CURSE_OF_FORGETFULNESS = 1311;
	public static final int CURSE_OF_MISFORTUNE = 1312;
	public static final int CURSE_OF_CLUMSINESS = 1313;
	public static final int CURSE_OF_LONELINESS = 1314;
	public static final int JAMMING_WITH_THE_JOCKS = 1338;
	public static final int NERD_IS_THE_WORD = 1339;
	public static final int GREASER_LIGHTNIN = 1340;
	public static final int ONCE_CURSED = 1348;
	public static final int TWICE_CURSED = 1349;
	public static final int THRICE_CURSED = 1350;
	public static final int BLESSING_OF_THE_WAR_SNAPPER = 1416;
	public static final int GRAND_BLESSING_OF_THE_WAR_SNAPPER = 1417;
	public static final int GLORIOUS_BLESSING_OF_THE_WAR_SNAPPER = 1418;
	public static final int BLESSING_OF_SHE_WHO_WAS = 1419;
	public static final int GRAND_BLESSING_OF_SHE_WHO_WAS = 1420;
	public static final int GLORIOUS_BLESSING_OF_SHE_WHO_WAS = 1421;
	public static final int BLESSING_OF_THE_STORM_TORTOISE = 1422;
	public static final int GRAND_BLESSING_OF_THE_STORM_TORTOISE = 1423;
	public static final int GLORIOUS_BLESSING_OF_THE_STORM_TORTOISE = 1424;
	public static final int DISDAIN_OF_THE_WAR_SNAPPER = 1425;
	public static final int DISDAIN_OF_SHE_WHO_WAS = 1426;
	public static final int DISDAIN_OF_THE_STORM_TORTOISE = 1427;
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
	public static final int BLOOD_SUGAR_SAUCE_MAGIC_LITE = 1457;
	public static final int BLOOD_SUGAR_SAUCE_MAGIC = 1458;
	public static final int SOULERSKATES = 1465;
	public static final int REASSURED = 1492;
	public static final int UNMUFFLED = 1545;
	public static final int MUFFLED = 1546;
	public static final int BORED_WITH_EXPLOSIONS = 1557;
	public static final int SOME_PIGS = 1640;
	public static final int CONFIDENCE = 1791;
	public static final int PURR_OF_THE_FELINE = 1800;
	public static final int RACING = 1909;
	public static final int DANCIN_FOOL_CARD = 1910;
	public static final int MAGICIANSHIP = 1911;
	public static final int STRONGLY_MOTIVATED = 1912;
	public static final int FORTUNE_OF_THE_WHEEL = 1913;
	public static final int BARREL_CHESTED = 1945;
	public static final int BARREL_OF_LAUGHS = 1946;
	public static final int PORK_BARREL = 1947;
	public static final int WARLOCK_WARSTOCK_WARBARREL = 1948;
	public static final int DOUBLE_BARRELED = 1949;
	public static final int BEER_BARREL_POLKA = 1950;
	public static final int CRAFT_TEA = 1989;
	public static final int COWRRUPTION = 2064;
	public static final int BOWLEGGED_SWAGGER = 2073;
	public static final int BENDIN_HELL = 2074;
	public static final int STEELY_EYED_SQUINT = 2075;
	public static final int FEELING_QUEASY = 2099;
	public static final int RECORD_HUNGER = 2128;
	public static final int DRUNK_AVUNCULAR = 2129;
	public static final int SHRIEKING_WEASEL = 2131;
	public static final int POWER_MAN = 2132;
	public static final int LUCKY_STRUCK = 2133;
	public static final int MINISTRATIONS_IN_THE_DARK = 2134;
	public static final int SUPERDRIFTING = 2135;
	public static final int SYNTHESIS_HOT = 2165;
	public static final int SYNTHESIS_COLD = 2166;
	public static final int SYNTHESIS_PUNGENT = 2167;
	public static final int SYNTHESIS_SCARY = 2168;
	public static final int SYNTHESIS_GREASY = 2169;
	public static final int SYNTHESIS_STRONG = 2170;
	public static final int SYNTHESIS_SMART = 2171;
	public static final int SYNTHESIS_COOL = 2172;
	public static final int SYNTHESIS_HARDY = 2173;
	public static final int SYNTHESIS_ENERGY = 2174;
	public static final int SYNTHESIS_GREED = 2175;
	public static final int SYNTHESIS_COLLECTION = 2176;
	public static final int SYNTHESIS_MOVEMENT = 2177;
	public static final int SYNTHESIS_LEARNING = 2178;
	public static final int SYNTHESIS_STYLE = 2179;
	public static final int SUPERFICIALLY_INTERESTED = 2288;
	public static final int INTENSELY_INTERESTED = 2289;
	public static final int DISAVOWED = 2294;
	public static final int OBNOXIOUSLY = 2308;
	public static final int STEALTHILY = 2309;
	public static final int WASTEFULLY = 2310;
	public static final int SAFELY = 2311;
	public static final int RECKLESSLY = 2312;
	public static final int QUICKLY = 2313;
	public static final int INTIMIDATINGLY = 2314;
	public static final int OBSERVANTLY = 2315;
	public static final int WATERPROOFLY = 2316;
	public static final int SILENT_HUNTING = 2336;
	public static final int NEARLY_SILENT_HUNTING = 2337;
	public static final int TAINTED_LOVE_POTION = 2374;
	public static final int BOXING_DAY_BREAKFAST = 2429;
	public static final int WOLF_FORM = 2449;
	public static final int MIST_FORM = 2450;
	public static final int BATS_FORM = 2451;
	public static final int BLESSING_OF_THE_BIRD = 2551;
	public static final int BLESSING_OF_YOUR_FAVORITE_BIRD = 2552;
	public static final int FIZZY_FIZZY = 2561;
	public static final int JOKE_MAD = 2582;
	public static final int CARTOGRAPHICALLY_CHARGED = 2600;
	public static final int CARTOGRAPHICALLY_AWARE = 2601;
	public static final int CARTOGRAPHICALLY_ROOTED = 2602;
	public static final int EW_THE_HUMANITY = 2647;
	public static final int A_BEASTLY_ODOR = 2648;

	public static final AdventureResult CURSE1_EFFECT = EffectPool.get( EffectPool.ONCE_CURSED );
	public static final AdventureResult CURSE2_EFFECT = EffectPool.get( EffectPool.TWICE_CURSED );
	public static final AdventureResult CURSE3_EFFECT = EffectPool.get( EffectPool.THRICE_CURSED );

	public static final AdventureResult get( final int effectId )
	{
		return new AdventureResult( effectId, 1, true );
	}

	public static final AdventureResult get( final int effectId, final int turns )
	{
		return new AdventureResult( effectId, turns, true );
	}
}
