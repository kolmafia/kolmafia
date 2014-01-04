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

public class SkillPool
{
	public static final int STEEL_LIVER = 1;
	public static final int SMILE_OF_MR_A = 3;
	public static final int ARSE_SHOOT = 4;
	public static final int STEEL_STOMACH = 5;
	public static final int STEEL_SPLEEN = 6;
	public static final int OBSERVATIOGN = 10;
	public static final int GNEFARIOUS_PICKPOCKETING = 11;
	public static final int TORSO = 12;
	public static final int GNOMISH_HARDINESS = 13;
	public static final int COSMIC_UNDERSTANDING = 14;
	public static final int OLFACTION = 19;
	public static final int LUST = 21;
	public static final int GLUTTONY = 22;
	public static final int GREED = 23;
	public static final int SLOTH = 24;
	public static final int WRATH = 25;
	public static final int ENVY = 26;
	public static final int PRIDE = 27;
	public static final int RAINBOW = 44;
	public static final int RAGE_GLAND = 45;
	public static final int GOTHY_HANDWAVE = 49;
	public static final int BREAK_IT_ON_DOWN = 50;
	public static final int POP_AND_LOCK = 51;
	public static final int RUN_LIKE_THE_WIND = 52;
	public static final int CRIMBO_CANDY = 53;
	public static final int VOLCANOMETEOR = 55;
	public static final int LUNCH_BREAK = 60;
	public static final int MANAGERIAL_MANIPULATION = 62;
	public static final int MIYAGI_MASSAGE = 64;
	public static final int SALAMANDER_KATA = 65;
	public static final int FLYING_FIRE_FIST = 66;
	public static final int STINKPALM = 67;
	public static final int SEVEN_FINGER_STRIKE = 68;
	public static final int KNUCKLE_SANDWICH = 69;
	public static final int CHILLED_MONKEY_BRAIN = 70;
	public static final int DRUNKEN_BABY_STYLE = 71;
	public static final int WORLDPUNCH = 72;
	public static final int ZENDO_KOBUSHI_KANCHO = 73;
	public static final int SUMMON_BONERS = 75;
	public static final int THICK_SKINNED = 80;
	public static final int CHIP_ON_YOUR_SHOULDER = 81;
	public static final int REQUEST_SANDWICH = 82;
	public static final int DEEP_VISIONS = 90;
	public static final int DOG_TIRED = 91;
	public static final int CARBOHYDRATE_CUDGEL = 93;
	public static final int GRAB_A_COLD_ONE = 95;
	public static final int SPAGHETTI_BREAKFAST = 101;
	public static final int SHADOW_NOODLES = 102;
	public static final int SHRAP = 110;
	public static final int PSYCHOKINETIC_HUG = 111;
	public static final int WALRUS_TONGUE = 1010;
	public static final int BATTER_UP = 1014;
	public static final int CLOBBER = 1022;
	public static final int IRON_PALM_TECHNIQUE = 1025;
	public static final int HIBERNATE = 1027;
	public static final int FURIOUS_WALLOP = 1032;
	public static final int CLUBFOOT = 1033;
	public static final int TOSS = 2023;
	public static final int SHELL_UP = 2028;
	public static final int SPIRIT_VACATION = 2027;
	public static final int WAR_BLESSING = 2030;
	public static final int SHE_WHO_WAS_BLESSING = 2033;
	public static final int PIZZA_LOVER = 2036;
	public static final int STORM_BLESSING = 2037;
	public static final int SPIRIT_BOON = 2039;
	public static final int TURTLE_POWER = 2041;
	public static final int RAVIOLI_SHURIKENS = 3003;
	public static final int ENTANGLING_NOODLES = 3004;
	public static final int CANNELLONI_CANNON = 3005;
	public static final int PASTAMASTERY = 3006;
	public static final int STUFFED_MORTAR_SHELL = 3007;
	public static final int WEAPON_PASTALORD = 3008;
	public static final int BANDAGES = 3009;
	public static final int COCOON = 3012;
	public static final int SPAGHETTI_SPEAR = 3020;
	public static final int STRINGOZZI = 3023;
	public static final int CARBOLOADING = 3024;
	public static final int TRANSCENDENTAL_DENTE = 3026;
	public static final int BIND_VAMPIEROGHI = 3027;
	public static final int BIND_VERMINCELLI = 3029;
	public static final int BIND_ANGEL_HAIR_WISP = 3031;
	public static final int SHIELD_OF_THE_PASTALORD = 3032;
	public static final int BIND_UNDEAD_ELBOW_MACARONI = 3033;
	public static final int BIND_PENNE_DREADFUL = 3035;
	public static final int BIND_LASAGMBIE = 3037;
	public static final int BIND_SPICE_GHOST = 3039;
	public static final int BIND_SPAGHETTI_ELEMENTAL = 3041;
	public static final int SPIRIT_CAYENNE = 7176;
	public static final int SPIRIT_PEPPERMINT = 7177;
	public static final int SPIRIT_GARLIC = 7178;
	public static final int SPIRIT_WORMWOOD = 7179;
	public static final int SPIRIT_BACON = 7180;
	public static final int SPIRIT_NOTHING = 7181;
	public static final int ADVANCED_SAUCECRAFTING = 4006;
	public static final int JALAPENO_SAUCESPHERE = 4008;
	public static final int JABANERO_SAUCESPHERE = 4011;
	public static final int SALSABALL = 4020;
	public static final int SIMMER = 4025;
	public static final int INNER_SAUCE = 4028;
	public static final int BLOOD_SUGAR_SAUCE_MAGIC = 4038;
	public static final int SAUCEMAVEN = 4039;
	public static final int DISCO_NAP = 5007;
	public static final int ADVENTURER_OF_LEISURE = 5011;
	public static final int ADVANCED_COCKTAIL = 5014;
	public static final int SUCKERPUNCH = 5021;
	public static final int THATS_NOT_A_KNIFE = 5028;
	public static final int ODE_TO_BOOZE = 6014;
	public static final int THINGFINDER = 6020;
	public static final int BENETTONS = 6021;
	public static final int ELRONS = 6022;
	public static final int COMPANIONSHIP = 6023;
	public static final int PRECISION = 6024;
	public static final int SING = 6025;
	public static final int DONHOS = 6026;
	public static final int INIGOS = 6028;
	public static final int ACCORDION_BASH = 6032;
	public static final int MAGIC_MISSILE = 7009;
	public static final int CREEPY_GRIN = 7015;
	public static final int MAYFLY_SWARM = 7024;
	public static final int VICIOUS_TALON_SLASH = 7038;
	public static final int WING_BUFFET = 7039;
	public static final int TUNNEL_UP = 7040;
	public static final int TUNNEL_DOWN = 7041;
	public static final int RISE_FROM_YOUR_ASHES = 7042;
	public static final int ANTARCTIC_FLAP = 7043;
	public static final int STATUE_TREATMENT = 7044;
	public static final int FEAST_ON_CARRION = 7045;
	public static final int GIVE_OPPONENT_THE_BIRD = 7046;
	public static final int HOBO_JOKE = 7050;
	public static final int HOBO_DANCE = 7051;
	public static final int SUMMON_HOBO = 7052;
	public static final int STINKEYE = 7095;
	public static final int BADLY_ROMANTIC_ARROW = 7108;
	public static final int BOXING_GLOVE_ARROW = 7109;
	public static final int POISON_ARROW = 7110;
	public static final int FINGERTRAP_ARROW = 7111;
	public static final int SQUEEZE_STRESS_BALL = 7113;
	public static final int RELEASE_BOOTS = 7115;
	public static final int SIPHON_SPIRITS = 7117;
	public static final int UNLEASH_NANITES = 7137;
	public static final int RAGE_FLAME = 7142;
	public static final int DOUBT_SHACKLES = 7143;
	public static final int FEAR_VAPOR = 7144;
	public static final int TEAR_WAVE = 7145;
	public static final int WINK = 7168;
	public static final int TALK_ABOUT_POLITICS = 7169;
	public static final int POCKET_CRUMBS = 7170;
	public static final int SOUL_BUBBLE = 7182;
	public static final int SOUL_FINGER = 7183;
	public static final int SOUL_BLAZE = 7184;
	public static final int SOUL_FOOD = 7185;
	public static final int SOUL_ROTATION = 7186;
	public static final int SOUL_FUNK = 7187;
	public static final int DISMISS_PASTA_THRALL = 7188;
	public static final int SNOWCONE = 8000;
	public static final int STICKER = 8001;
	public static final int SUGAR = 8002;
	public static final int CLIP_ART = 8003;
	public static final int RAD_LIB = 8004;
	public static final int SMITHSNESS = 8005;
	public static final int CANDY_HEART = 8100;
	public static final int PARTY_FAVOR = 8101;
	public static final int LOVE_SONG = 8102;
	public static final int BRICKOS = 8103;
	public static final int DICE = 8104;
	public static final int RESOLUTIONS = 8105;
	public static final int TAFFY = 8106;
	public static final int HILARIOUS = 8200;
	public static final int TASTEFUL = 8201;
	public static final int CARDS = 8202;
	public static final int GEEKY = 8203;
	public static final int GOOD_SINGING_VOICE = 11016;
	public static final int BANISHING_SHOUT = 11020;
	public static final int DEMAND_SANDWICH = 11021;
	public static final int GLORIOUS_LUNCH = 11023;
	public static final int HOWL_ALPHA = 12020;
	public static final int SUMMON_MINION = 12021;
	public static final int SUMMON_HORDE = 12026;
	public static final int CONJURE_EGGS = 14001;
	public static final int CONJURE_DOUGH = 14002;
	public static final int EGGMAN = 14005;
	public static final int CONJURE_VEGGIES = 14011;
	public static final int CONJURE_CHEESE = 14012;
	public static final int RADISH_HORSE = 14015;
	public static final int CONJURE_MEAT = 14021;
	public static final int CONJURE_POTATO = 14022;
	public static final int HIPPOTATO = 14025;
	public static final int CONJURE_CREAM = 14031;
	public static final int CONJURE_FRUIT = 14032;
	public static final int CREAMPUFF = 14035;
}
