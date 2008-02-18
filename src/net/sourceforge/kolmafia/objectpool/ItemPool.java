/**
 * Copyright (c) 2005-2007, KoLmafia development team
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
	public static final int CASINO_PASS = 40;
	public static final int DINGY_PLANKS = 140;
	public static final int DINGHY_DINGY = 141;
	public static final int DINGHY_PLANS = 146;
	public static final int DISASSEMBLED_CLOVER = 196;
	public static final int KNOB_GOBLIN_PERFUME = 307;
	public static final int TRANSFUNCTIONER = 458;
	public static final int TALISMAN = 486;
	public static final int SONAR = 563;
	public static final int BLACK_CANDLE = 620;
	public static final int SHOCK_COLLAR = 856;
	public static final int MOONGLASSES = 857;
	public static final int LEAD_NECKLACE = 865;
	public static final int TAM_O_SHANTER = 1040;
	public static final int TARGETING_CHIP = 1102;
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
	public static final int TEDDY_SEWING_KIT = 1419;
	public static final int BAG_OF_CATNIP = 1486;
	public static final int HANG_GLIDER = 1487;
	public static final int MINIATURE_DORMOUSE = 1489;
	public static final int WEEGEE_SQOUIJA = 1537;
	public static final int SNOOTY_DISGUISE = 1526;
	public static final int TAM_O_SHATNER = 1539;
	public static final int ASTRAL_MUSHROOM = 1622;
	public static final int BADGER_BADGE = 1623;
	public static final int CITADEL_SATCHEL = 1656;
	public static final int GROUCHO_DISGUISE = 1678;
	public static final int LIBRARY_KEY = 1764;
	public static final int GALLERY_KEY = 1765;
	public static final int BALLROOM_KEY = 1766;
	public static final int TUNING_FORK = 1928;
	public static final int EVIL_SCROLL = 1960;
	public static final int PUMPKIN_BUCKET = 1971;
	public static final int NOVELTY_BUTTON = 2072;
	public static final int EVIL_TEDDY_SEWING_KIT = 2147;
	public static final int CAN_OF_STARCH = 2084;
	public static final int ANCIENT_CAROLS = 2191;
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
	public static final int PARROT_CRACKER = 2710;
	public static final int FOIL_BOW = 3043;
	public static final int FOIL_RADAR = 3044;
	public static final int POWER_SPHERE = 3049;
	public static final int TEDDY_BORG_SEWING_KIT = 3087;
	public static final int HOBBY_HORSE = 3092;
	public static final int BALL_IN_A_CUP = 3093;
	public static final int SET_OF_JACKS = 3094;
	public static final int FISH_SCALER = 3097;
	public static final int ORIGAMI_MAGAZINE = 3194;
	public static final int OVERCHARGED_POWER_SPHERE = 3215;

	public static final AdventureResult get( String itemName, int count )
	{
		return ItemPool.get( ItemDatabase.getItemId( itemName ), 1 );
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
