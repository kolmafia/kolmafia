/**
 * Copyright (c) 2005-2016, KoLmafia development team
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

package net.sourceforge.kolmafia.persistence;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import net.sourceforge.kolmafia.objectpool.EffectPool;

public class CandyDatabase
{
	public static Set<Integer> noCandy = new HashSet<Integer>();	// No candies
	public static Set<Integer> tier0Candy = new HashSet<Integer>();	// Unspaded
	public static Set<Integer> tier1Candy = new HashSet<Integer>();	// Simple
	public static Set<Integer> tier2Candy = new HashSet<Integer>();	// Simple and Complex
	public static Set<Integer> tier3Candy = new HashSet<Integer>();	// Complex

	public static String [] canonicalNames = new String[0];

	public static final String NONE = "none";
	public static final String UNSPADED = "unspaded";
	public static final String SIMPLE = "simple";
	public static final String COMPLEX = "complex";

	public static void registerCandy( final Integer itemId, final String type )
	{
		if ( type.equals( "candy" ) )
		{
			// Unspaded candy
			CandyDatabase.tier0Candy.add( itemId );
			return;
		}

		if ( type.equals( "candy1" ) )
		{
			// Simple candy
			CandyDatabase.tier1Candy.add( itemId );
			CandyDatabase.tier2Candy.add( itemId );
		}
		else if ( type.equals( "candy2" ) )
		{
			// Complex candy
			CandyDatabase.tier3Candy.add( itemId );
			CandyDatabase.tier2Candy.add( itemId );
		}
		else
		{
			return;
		}

		// We could add to the canonical name array, but it is more
		// efficient to do it after all the candies are registered
	}

	public static final void saveCanonicalNames()
	{
		String[] newArray = new String[ CandyDatabase.tier0Candy.size() + CandyDatabase.tier2Candy.size() ];

		int i = 0;
		for ( Integer itemId : CandyDatabase.tier0Candy )
		{
			newArray[ i++ ] = ItemDatabase.getCanonicalName( itemId );
		}
		for ( Integer itemId : CandyDatabase.tier2Candy )
		{
			newArray[ i++ ] = ItemDatabase.getCanonicalName( itemId );
		}

		Arrays.sort( newArray );
		CandyDatabase.canonicalNames = newArray;
	}

	public static final String getCandyType( final int itemId )
	{
		// We could look in our various candy sets, but more efficient
		// to just look at item attributes
		int attributes = ItemDatabase.getAttributes( itemId );
		return  ( attributes & ItemDatabase.ATTR_CANDY0 ) != 0 ? UNSPADED :
			( attributes & ItemDatabase.ATTR_CANDY1 ) != 0 ? SIMPLE :
			( attributes & ItemDatabase.ATTR_CANDY2 ) != 0 ? COMPLEX :
			NONE;
	}

	public static final int getEffectTier( final int itemId1, final int itemId2 )
	{
		String candyType1 = CandyDatabase.getCandyType( itemId1 );
		String candyType2 = CandyDatabase.getCandyType( itemId2 );
		return  ( candyType1 == NONE || candyType2 == NONE ) ? 0 :
			( candyType1 == SIMPLE && candyType2 == SIMPLE ) ? 1 :
			( candyType1 == COMPLEX && candyType2 == COMPLEX ) ? 3 :
			2;
	}

	public static final int getEffectTier( final int effectId )
	{
		switch ( effectId )
		{
		case EffectPool.SYNTHESIS_HOT:
		case EffectPool.SYNTHESIS_COLD:
		case EffectPool.SYNTHESIS_PUNGENT:
		case EffectPool.SYNTHESIS_SCARY:
		case EffectPool.SYNTHESIS_GREASY:
			return 1;
		case EffectPool.SYNTHESIS_STRONG:
		case EffectPool.SYNTHESIS_SMART:
		case EffectPool.SYNTHESIS_COOL:
		case EffectPool.SYNTHESIS_HARDY:
		case EffectPool.SYNTHESIS_ENERGY:
			return 2;
		case EffectPool.SYNTHESIS_GREED:
		case EffectPool.SYNTHESIS_COLLECTION:
		case EffectPool.SYNTHESIS_MOVEMENT:
		case EffectPool.SYNTHESIS_LEARNING:
		case EffectPool.SYNTHESIS_STYLE:
			return 3;
		default:
			return 0;
		}
	}

	public static final int getEffectModulus( final int effectId )
	{
		switch ( effectId )
		{
		case EffectPool.SYNTHESIS_HOT:
		case EffectPool.SYNTHESIS_STRONG:
		case EffectPool.SYNTHESIS_GREED:
			return 0;
		case EffectPool.SYNTHESIS_COLD:
		case EffectPool.SYNTHESIS_SMART:
		case EffectPool.SYNTHESIS_COLLECTION:
			return 1;
		case EffectPool.SYNTHESIS_PUNGENT:
		case EffectPool.SYNTHESIS_COOL:
		case EffectPool.SYNTHESIS_MOVEMENT:
			return 2;
		case EffectPool.SYNTHESIS_SCARY:
		case EffectPool.SYNTHESIS_HARDY:
		case EffectPool.SYNTHESIS_LEARNING:
			return 3;
		case EffectPool.SYNTHESIS_GREASY:
		case EffectPool.SYNTHESIS_ENERGY:
		case EffectPool.SYNTHESIS_STYLE:
			return 4;
		default:
			return -1;
		}
	}

	public static final int effectTierBase( final int tier )
	{
		switch ( tier )
		{
		case 1:
			return EffectPool.SYNTHESIS_HOT;
		case 2:
			return EffectPool.SYNTHESIS_STRONG;
		case 3:
			return EffectPool.SYNTHESIS_GREED;
		}
		return -1;
	}

	public static Set<Integer> candyForTier( final int tier )
	{
		return  tier == 0 ? CandyDatabase.tier0Candy :
			tier == 1 ? CandyDatabase.tier1Candy :
			tier == 2 ? CandyDatabase.tier2Candy :
			tier == 3 ? CandyDatabase.tier3Candy :
			null;
	}

	public static int synthesisResult( final int itemId1, final int itemId2 )
	{
		if ( !ItemDatabase.isCandyItem( itemId1 ) || !ItemDatabase.isCandyItem( itemId2 ) )
		{
			return -1;
		}

		int tier = CandyDatabase.getEffectTier( itemId1, itemId2 );
		if ( tier == 0 )
		{
			return -1;
		}

		int base = CandyDatabase.effectTierBase( tier );
		int modulus = ( itemId1 + itemId2 ) % 5;

		return base + modulus;
	}

	public static Set<Integer> sweetSynthesisPairing( final int effectId, final int itemId1 )
	{
		Set<Integer> result = new HashSet<Integer>();

		int tier = CandyDatabase.getEffectTier( effectId );
		if ( tier < 1 || tier > 3 )
		{
			return result;
		}

		String candyType = CandyDatabase.getCandyType( itemId1 );
		if ( candyType != SIMPLE && candyType != COMPLEX )
		{
			return result;
		}

		Set<Integer> candidates = CandyDatabase.noCandy;

		switch ( tier )
		{
		case 1:
			candidates = ( candyType == SIMPLE ) ? CandyDatabase.tier1Candy : CandyDatabase.noCandy;
			break;
		case 2:
			candidates = ( candyType == SIMPLE ) ? CandyDatabase.tier3Candy : CandyDatabase.tier1Candy;
			break;
		case 3:
			candidates = ( candyType == COMPLEX ) ? CandyDatabase.tier3Candy : CandyDatabase.noCandy;
			break;
		}

		int desiredModulus = CandyDatabase.getEffectModulus( effectId );

		for ( int itemId2 : candidates )
		{
			if ( ( itemId1 + itemId2 ) % 5 == desiredModulus )
			{
				result.add( itemId2 );
			}
		}

		return result;
	}
}
