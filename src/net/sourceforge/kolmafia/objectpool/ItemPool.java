/**
 * Copyright (c) 2005-2008, KoLmafia development team
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

import java.util.ArrayList;

import net.sourceforge.kolmafia.AdventureResult;

import net.sourceforge.kolmafia.persistence.ItemDatabase;

public class ItemPool
{
	private static final ItemArray itemCache = new ItemArray();

	public static final int CHEWING_GUM = 23;
	public static final int TEN_LEAF_CLOVER = 24;
	public static final int MEAT_PASTE = 25;
	public static final int CASINO_PASS = 40;
	public static final int SCHLITZ = 41;
	public static final int SPOOKY_MAP = 74;
	public static final int SPOOKY_SAPLING = 75;
	public static final int SPOOKY_FERTILIZER = 76;
	public static final int GRAVY_BOAT = 80;
	public static final int WILLER = 81;
	public static final int MEAT_FROM_YESTERDAY = 87;
	public static final int MEAT_STACK = 88;
	public static final int KETCHUP = 106;
	public static final int CATSUP = 107;
	public static final int BITCHIN_MEATCAR = 134;
	public static final int DINGY_PLANKS = 140;
	public static final int DINGHY_DINGY = 141;
	public static final int DINGHY_PLANS = 146;
	public static final int BAKE_OVEN = 157;
	public static final int DOUGH = 159;
	public static final int ENCHANTED_BEAN = 186;
	public static final int DISASSEMBLED_CLOVER = 196;
	public static final int COCKTAIL_KIT = 236;
	public static final int TOMATO = 246;
	public static final int DENSE_STACK = 258;
	public static final int MOSQUITO_LARVA = 275;
	public static final int FLAT_DOUGH = 301;
	public static final int DRY_NOODLES = 304;
	public static final int KNOB_GOBLIN_PERFUME = 307;
	public static final int GOAT_CHEESE = 322;
	public static final int TENDER_HAMMER = 338;
	public static final int REAGENT = 346;
	public static final int DYSPEPSI_COLA = 347;
	public static final int LINOLEUM_ORE = 363;
	public static final int ASBESTOS_ORE = 364;
	public static final int CHROME_ORE = 365;
	public static final int CHEF = 438;
	public static final int BARTENDER = 440;
	public static final int TRANSFUNCTIONER = 458;
	public static final int WHITE_PIXEL = 459;
	public static final int BLACK_PIXEL = 460;
	public static final int RED_PIXEL = 461;
	public static final int GREEN_PIXEL = 462;
	public static final int BLUE_PIXEL = 463;
	public static final int TALISMAN = 486;
	public static final int ABRIDGED = 534;
	public static final int BRIDGE = 535;
	public static final int DICTIONARY = 536;
	public static final int ELITE_SCROLL = 553;
	public static final int SONAR = 563;
	public static final int REMEDY = 588;
	public static final int SOCK = 609;
	public static final int BLACK_CANDLE = 620;
	public static final int ROWBOAT = 653;
	public static final int STAR = 654;
	public static final int LINE = 655;
	public static final int STAR_CHART = 656;
	public static final int JEWELRY_PLIERS = 709;
	public static final int PUZZLE_PIECE = 727;
	public static final int HEDGE_KEY = 728;
	public static final int BADASS_BELT = 677;
	public static final int SHOCK_COLLAR = 856;
	public static final int MOONGLASSES = 857;
	public static final int LEAD_NECKLACE = 865;
	public static final int ROLLING = 873;
	public static final int UNROLLING = 874;
	public static final int PLASTIC_SWORD = 938;
	public static final int TAM_O_SHANTER = 1040;
	public static final int TARGETING_CHIP = 1102;
	public static final int CLOCKWORK_BARTENDER = 1111;
	public static final int CLOCKWORK_CHEF = 1112;
	public static final int ANNOYING_PITCHFORK = 1116;
	public static final int GRAVY_MAYPOLE = 1152;
	public static final int RAT_BALLOON = 1218;
	public static final int TOY_HOVERCRAFT = 1243;
	public static final int WAX_LIPS = 1260;
	public static final int NOSE_BONE_FETISH = 1264;
	public static final int DEAD_MIMIC = 1267;
	public static final int PINE_WAND = 1268;
	public static final int EBONY_WAND = 1269;
	public static final int HEXAGONAL_WAND = 1270;
	public static final int ALUMINUM_WAND = 1271;
	public static final int MARBLE_WAND = 1272;
	public static final int MAKEUP_KIT = 1305;
	public static final int CLOACA_COLA = 1334;
	public static final int TEDDY_SEWING_KIT = 1419;
	public static final int ICEBERGLET = 1423;
	public static final int ICE_SICKLE = 1424;
	public static final int ICE_BABY = 1425;
	public static final int ICE_PICK = 1426;
	public static final int ICE_SKATES = 1427;
	public static final int BAG_OF_CATNIP = 1486;
	public static final int HANG_GLIDER = 1487;
	public static final int MINIATURE_DORMOUSE = 1489;
	public static final int RUBBER_EMO_ROE = 1503;
	public static final int SNOOTY_DISGUISE = 1526;
	public static final int WEEGEE_SQOUIJA = 1537;
	public static final int TAM_O_SHATNER = 1539;
	public static final int MSG = 1549;
	public static final int CATALYST = 1605;
	public static final int ASTRAL_MUSHROOM = 1622;
	public static final int BADGER_BADGE = 1623;
	public static final int MILK_OF_MAGNESIUM = 1650;
	public static final int CITADEL_SATCHEL = 1656;
	public static final int GROUCHO_DISGUISE = 1678;
	public static final int LIBRARY_KEY = 1764;
	public static final int GALLERY_KEY = 1765;
	public static final int BALLROOM_KEY = 1766;
	public static final int TUNING_FORK = 1928;
	public static final int EVIL_SCROLL = 1960;
	public static final int PUMPKIN_BUCKET = 1971;
	public static final int STUFFED_COCOABO = 1974;
	public static final int NOVELTY_BUTTON = 2072;
	public static final int EVIL_TEDDY_SEWING_KIT = 2147;
	public static final int MAKESHIFT_TURBAN = 2079;
	public static final int MAKESHIFT_CAPE = 2080;
	public static final int MAKESHIFT_SKIRT = 2081;
	public static final int MAKESHIFT_CRANE = 2083;
	public static final int CAN_OF_STARCH = 2084;
	public static final int TOWEL = 2085;
	public static final int LUCRE = 2098;
	public static final int ANCIENT_CAROLS = 2191;
	public static final int SHEET_MUSIC = 2192;
	public static final int FAMILIAR_DOPPELGANGER = 2225;
	public static final int MUS_MANUAL = 2280;
	public static final int MYS_MANUAL = 2281;
	public static final int MOX_MANUAL = 2282;
	public static final int MAYFLOWER_BOUQUET = 2541;
	public static final int PLASTIC_BIB = 2846;
	public static final int ANT_HOE = 2570;
	public static final int ANT_RAKE = 2571;
	public static final int ANT_PITCHFORK = 2572;
	public static final int ANT_SICKLE = 2573;
	public static final int ANT_PICK = 2574;
	public static final int MOJO_FILTER = 2614;
	public static final int PARROT_CRACKER = 2710;
	public static final int FOIL_BOW = 3043;
	public static final int FOIL_RADAR = 3044;
	public static final int POWER_SPHERE = 3049;
	public static final int TEDDY_BORG_SEWING_KIT = 3087;
	public static final int HOBBY_HORSE = 3092;
	public static final int BALL_IN_A_CUP = 3093;
	public static final int SET_OF_JACKS = 3094;
	public static final int FISH_SCALER = 3097;
	public static final int CARD_ATTACK = 3146;
	public static final int CARD_WALL = 3155;
	public static final int EL_VIBRATO_HELMET = 3162;
	public static final int ORIGAMI_MAGAZINE = 3194;
	public static final int OVERCHARGED_POWER_SPHERE = 3215;

	public static final AdventureResult get( String itemName, int count )
	{
		return ItemPool.get( ItemDatabase.getItemId( itemName, 1, false ), count );
	}

	public static final AdventureResult get( int itemId, int count )
	{
		if ( count == 1 )
		{
			return itemCache.get( itemId );
		}

		return new AdventureResult( itemId, count );
	}

	private static class ItemArray
	{
		private ArrayList internalList = new ArrayList();

		public AdventureResult get( int itemId )
		{
			for ( int i = internalList.size(); i <= itemId; ++i )
			{
				internalList.add( new AdventureResult( i, 1 ) );
			}

			return (AdventureResult) internalList.get( itemId );
		}
	}
}
