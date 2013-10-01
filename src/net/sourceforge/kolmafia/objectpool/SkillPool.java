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
	public static final int OTTER_TONGUE = 1007;
	public static final int WALRUS_TONGUE = 1010;
	public static final int CLOBBER = 1022;
	public static final int CLUBFOOT = 1033;
	public static final int TOSS = 2023;
	public static final int HEAD_KNEE = 2103;
	public static final int HEAD_SHIELD = 2105;
	public static final int KNEE_SHIELD = 2106;
	public static final int HEAD_KNEE_SHIELD = 2107;
	public static final int ENTANGLING_NOODLES = 3004;
	public static final int	TRANSCENDENTAL_NOODLES = 3006;
	public static final int BANDAGES = 3009;
	public static final int COCOON = 3012;
	public static final int SPAGHETTI_SPEAR = 3020;
	public static final int CARBOLOADING = 3024;
	public static final int SPIRIT_CAYENNE = 3101;
	public static final int SPIRIT_PEPPERMINT = 3102;
	public static final int SPIRIT_GARLIC = 3103;
	public static final int SPIRIT_WORMWOOD = 3104;
	public static final int SPIRIT_BACON = 3105;
	public static final int SPIRIT_NOTHING = 3106;
	public static final int WAY_OF_SAUCE = 4006;
	public static final int JALAPENO_SAUCESPHERE = 4008;
	public static final int JABANERO_SAUCESPHERE = 4011;
	public static final int SALSABALL = 4020;
	public static final int DISCO_NAP = 5007;
	public static final int POWER_NAP = 5011;
	public static final int SUPERHUMAN_COCKTAIL = 5014;
	public static final int SUCKERPUNCH = 5021;
	public static final int ODE_TO_BOOZE = 6014;
	public static final int THINGFINDER = 6020;
	public static final int BENETTONS = 6021;
	public static final int ELRONS = 6022;
	public static final int COMPANIONSHIP = 6023;
	public static final int PRECISION = 6024;
	public static final int SING = 6025;
	public static final int DONHOS = 6026;
	public static final int INIGOS = 6028;
	public static final int MAGIC_MISSILE = 7009;
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
	public static final int SNOWCONE = 8000;
	public static final int STICKER = 8001;
	public static final int SUGAR = 8002;
	public static final int CLIP_ART = 8003;
	public static final int RAD_LIB = 8004;
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
