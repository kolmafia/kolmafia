/**
 * Copyright (c) 2005-2017, KoLmafia development team
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.sourceforge.kolmafia.KoLCharacter;

import net.sourceforge.kolmafia.objectpool.EffectPool;

import net.sourceforge.kolmafia.session.InventoryManager;

public class CandyDatabase
{
	public static Set<Integer> NO_CANDY = new HashSet<Integer>();	// No candies
	public static Set<Integer> tier0Candy = new HashSet<Integer>();	// Unspaded
	public static Set<Integer> tier1Candy = new HashSet<Integer>();	// Simple
	public static Set<Integer> tier2Candy = new HashSet<Integer>();	// Simple and Complex
	public static Set<Integer> tier3Candy = new HashSet<Integer>();	// Complex

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

	private static final int FLAG_AVAILABLE = 0x1;
	private static final int FLAG_ALLOWED = 0x2;

	public static int makeFlags( final boolean available, final boolean allowed )
	{
		return ( available ? FLAG_AVAILABLE : 0 ) + ( allowed ? FLAG_ALLOWED : 0 );
	}

	public static int defaultFlags()
	{
		boolean loggedIn = KoLCharacter.getUserId() > 0;
		boolean available = loggedIn && !KoLCharacter.canInteract();
		boolean allowed = loggedIn && KoLCharacter.getRestricted();
		return CandyDatabase.makeFlags( available, allowed );
	}

	public static Set<Integer> candyForTier( final int tier )
	{
		return CandyDatabase.candyForTier( tier, CandyDatabase.defaultFlags() );
	}

	public static Set<Integer> candyForTier( final int tier, final int flags )
	{
		if ( tier < 0 || tier > 3 )
		{
			return null;
		}

		Set<Integer> candies =
			tier == 0 ? CandyDatabase.tier0Candy :
			tier == 1 ? CandyDatabase.tier1Candy :
			tier == 2 ? CandyDatabase.tier2Candy :
			tier == 3 ? CandyDatabase.tier3Candy :
			null;

		// If neither flag is set, return full set
		if ( ( flags & 0x3) == 0 )
		{
			return candies;
		}

		// Otherwise, we must filter
		boolean available = ( flags & FLAG_AVAILABLE ) != 0;
		boolean allowed = ( flags & FLAG_ALLOWED ) != 0;
		Set<Integer> result = new HashSet<Integer>();

		for ( Integer itemId : candies )
		{
			if ( available && InventoryManager.getAccessibleCount( itemId ) == 0 )
			{
				continue;
			}
			if ( allowed && !ItemDatabase.isAllowed( itemId ) )
			{
				continue;
			}
			result.add( itemId );
		}

		return result;
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
		return CandyDatabase.sweetSynthesisPairing( effectId, itemId1, CandyDatabase.defaultFlags() );
	}

	public static Set<Integer> sweetSynthesisPairing( final int effectId, final int itemId1, final int flags )
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

		Set<Integer> candidates = CandyDatabase.NO_CANDY;

		switch ( tier )
		{
		case 1:
			candidates = ( candyType == SIMPLE ) ? CandyDatabase.tier1Candy : CandyDatabase.NO_CANDY;
			break;
		case 2:
			candidates = ( candyType == SIMPLE ) ? CandyDatabase.tier3Candy : CandyDatabase.tier1Candy;
			break;
		case 3:
			candidates = ( candyType == COMPLEX ) ? CandyDatabase.tier3Candy : CandyDatabase.NO_CANDY;
			break;
		}

		int desiredModulus = CandyDatabase.getEffectModulus( effectId );
		boolean available = ( flags & FLAG_AVAILABLE ) != 0;
		boolean allowed = ( flags & FLAG_ALLOWED ) != 0;

		for ( int itemId2 : candidates )
		{
			if ( ( itemId1 + itemId2 ) % 5 != desiredModulus )
			{
				continue;
			}
			if ( available )
			{
				// You can synthesize two of the same candy.
				// If using available candy and you only have
				// one, can't reuse it.
				int candy2Count = InventoryManager.getAccessibleCount( itemId2 );
				if ( ( candy2Count == 0 ) ||
				     ( itemId1 == itemId2 && candy2Count == 1 ) )
				{
					continue;
				}
			}
			if ( allowed && !ItemDatabase.isAllowed( itemId2 ) )
			{
				continue;
			}
			result.add( itemId2 );
		}

		return result;
	}

	// *** Phase 5 methods ***

	// Here will go fancy code to choose combinations of candies that are
	// either cheap (aftercore) or available (in-run) using the provided
	// Comparators to sort Candy lists appropriately

	// Use ASCENDING_MALL_PRICE_COMPARATOR in aftercore
	// Use DESCENDING_COUNT_COMPARATOR in-run

	// Pseudo-price for a non-tradeable item
	public static final int NON_TRADEABLE_PRICE = 999999999;

	public static class Candy
		implements Comparable<Candy>
	{
		private final int itemId;
		private final String name;
		private int count;
		private int mallprice;
		private boolean restricted;

		public Candy( final int itemId )
		{
			this.itemId = itemId;
			this.name = ItemDatabase.getDataName( itemId );
			this.count = InventoryManager.getAccessibleCount( itemId );
			this.mallprice = ItemDatabase.isTradeable( itemId ) ? MallPriceDatabase.getPrice( itemId ) : 0;
			this.restricted = !ItemDatabase.isAllowedInStandard( itemId );
		}

		@Override
		public boolean equals( final Object o )
		{
			return ( o instanceof Candy ) && ( this.itemId == ((Candy)o).itemId );
		}

		public int compareTo( final Candy o )
		{
			if ( o == null )
			{
				throw new NullPointerException();
			}

			return this.itemId - o.itemId;
		}

		public int getItemId()
		{
			return this.itemId;
		}

		public String getName()
		{
			return this.name;
		}

		public int getCount()
		{
			return this.count;
		}

		public int getCost()
		{
			return this.mallprice == 0 ? CandyDatabase.NON_TRADEABLE_PRICE : this.mallprice;
		}

		public int getMallPrice()
		{
			return this.mallprice;
		}

		public boolean getRestricted()
		{
			return this.restricted;
		}

		public Candy update()
		{
			this.count = InventoryManager.getAccessibleCount( this.itemId );
			this.mallprice = MallPriceDatabase.getPrice( this.itemId );
			return this;
		}

		public String toString()
		{
			return this.name;
		}
	}

	public static List<Candy> itemIdSetToCandyList( Set<Integer> itemIds )
	{
		ArrayList<Candy> list = new ArrayList<Candy>();

		for ( int itemId : itemIds )
		{
			list.add( new Candy( itemId ) );
		}

		return list;
	}

	// Compare by lowest mall price, then largest quantity, then alphabetically
	private static class MallPriceComparator
		implements Comparator<Candy>
	{
		public int compare( Candy o1, Candy o2 )
		{
			int cost1 = o1.getCost();
			int cost2 = o2.getCost();
			if ( cost1 != cost2 )
			{
				return cost1 - cost2;
			}
			int count1 = o1.getCount();
			int count2 = o2.getCount();
			if ( count1 != count2 )
			{
				return count2 - count1;
			}
			return o1.getName().compareToIgnoreCase( o2.getName() );
		}
	}

	public static final Comparator<Candy> ASCENDING_MALL_PRICE_COMPARATOR = new MallPriceComparator();

	// Compare by largest quantity, then by lowest mall price, then alphabetically
	private static class InverseCountComparator
		implements Comparator<Candy>
	{
		public int compare( Candy o1, Candy o2 )
		{
			int count1 = o1.getCount();
			int count2 = o2.getCount();
			if ( count1 != count2 )
			{
				return count2 - count1;
			}
			return o1.getName().compareToIgnoreCase( o2.getName() );
		}
	}

	public static final Comparator<Candy> DESCENDING_COUNT_COMPARATOR = new InverseCountComparator();

	public static final Candy[] NO_PAIR = new Candy[0];

	public static Candy[] synthesisPair( final int effectId )
	{
		return CandyDatabase.synthesisPair( effectId, CandyDatabase.defaultFlags() );
	}

	public static Candy[] synthesisPair( final int effectId, final int flags )
	{
		boolean available = ( flags & FLAG_AVAILABLE ) != 0;
		return  available ?
			CandyDatabase.synthesisPairByCount( effectId, flags ) :
			CandyDatabase.synthesisPairByCost( effectId, flags );
	}

	private static Candy[] synthesisPairByCount( final int effectId, final int flags )
	{
		int tier = CandyDatabase.getEffectTier( effectId );

		List<Candy> candy1List = CandyDatabase.itemIdSetToCandyList( CandyDatabase.candyForTier( tier, flags ) );
		Collections.sort( candy1List, DESCENDING_COUNT_COMPARATOR );

		for ( Candy candy : candy1List )
		{
			if ( candy.getCount() == 0 )
			{
				// Ran out of available candies
				return NO_PAIR;
			}

			int itemId = candy.getItemId();
			List<Candy> candy2List = CandyDatabase.itemIdSetToCandyList( CandyDatabase.sweetSynthesisPairing( effectId, itemId, flags ) );
			Collections.sort( candy2List, DESCENDING_COUNT_COMPARATOR );

			for ( Candy pairing : candy2List )
			{
				int count = pairing.getCount();
				if ( count == 0 )
				{
					// Nothing left in this list. Select a new candy1
					break;
				}
				
				if ( candy.equals( pairing ) && count == 1 )
				{
					// Pairs with itself but only have one.
					continue;
				}

				Candy[] result = new Candy[2];
				result[0] = candy;
				result[1] = pairing;
				return result;
			}
		}

		return NO_PAIR;
	}

	private static Candy[] synthesisPairByCost( final int effectId, final int flags )
	{
		int tier = CandyDatabase.getEffectTier( effectId );

		int bestCost = Integer.MAX_VALUE;
		Candy candy1 = null;
		Candy candy2 = null;

		List<Candy> candy1List = CandyDatabase.itemIdSetToCandyList( CandyDatabase.candyForTier( tier, flags ) );
		Collections.sort( candy1List, ASCENDING_MALL_PRICE_COMPARATOR );

		for ( Candy candy : candy1List )
		{
			int cost1 = candy.getCost();
			if ( cost1 > bestCost )
			{
				break;
			}

			int itemId = candy.getItemId();
			List<Candy> candy2List = CandyDatabase.itemIdSetToCandyList( CandyDatabase.sweetSynthesisPairing( effectId, itemId, flags ) );
			Collections.sort( candy2List, ASCENDING_MALL_PRICE_COMPARATOR );

			for ( Candy pairing : candy2List )
			{
				int cost2 = pairing.getCost();
				int currentCost = cost1 + cost2;

				if ( currentCost >= bestCost )
				{
					break;
				}

				candy1 = candy;
				candy2 = pairing;
				bestCost = currentCost;
			}
		}

		if ( candy1 == null || candy2 == null )
		{
			return NO_PAIR;
		}

		Candy[] result = new Candy[2];
		result[0] = candy1;
		result[1] = candy2;

		return result;
	}
}
