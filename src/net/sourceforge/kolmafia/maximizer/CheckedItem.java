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

package net.sourceforge.kolmafia.maximizer;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.objectpool.Concoction;
import net.sourceforge.kolmafia.objectpool.ConcoctionPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.persistence.MallPriceDatabase;
import net.sourceforge.kolmafia.request.TrendyRequest;
import net.sourceforge.kolmafia.session.InventoryManager;
import net.sourceforge.kolmafia.session.StoreManager;


public class CheckedItem
	extends AdventureResult
{
	public CheckedItem( int itemId, int equipLevel, int maxPrice, int priceLevel )
	{
		super( itemId, 1 );

		// special case used to get a CheckItem that .equals( EquipmentRequest.UNEQUIP ).
		if ( itemId == 0 )
		{
			this.name = "(none)";
		}

		this.initial = InventoryManager.getAccessibleCount( itemId );

		if ( this.initial >= 3 || equipLevel < 2 )
		{
			return;
		}

		Concoction c = ConcoctionPool.get( itemId );
		if ( c == null )
			return;
		this.creatable = c.creatable;

		if ( c.price > 0 )
		{
			this.npcBuyable = maxPrice / c.price;
		}

		// TODO: check foldability

		if ( this.getCount() >= 3 || equipLevel < 3 )
		{
			return;
		}

		if ( KoLCharacter.isTrendy() && !TrendyRequest.isTrendy( "Items", this.getName() ) )
		{
			// Trendy characters can't buy or pull untrendy items, though the original code
			// just reset everything to zero

			this.initial = 0;
			this.creatable = 0;
			this.npcBuyable = 0;
		}
		else if ( KoLCharacter.canInteract() )
		{
			// consider Mall buying
			if ( this.getCount() == 0 && ItemDatabase.isTradeable( itemId ) )
			{	// but only if none are otherwise available
				if ( priceLevel == 0 ||
					MallPriceDatabase.getPrice( itemId ) < maxPrice * 2 )
				{
					this.mallBuyable = 1;
					this.buyableFlag = true;
				}
			}
		}
		else if ( !KoLCharacter.isHardcore() )
		{
			// consider pulling
			this.pullable = this.getCount( KoLConstants.storage );
		}

		// We never want to suggest turning Mr. Accessories into Ms. Accessories.
		if ( itemId == ItemPool.MS_ACCESSORY )
		{
			this.creatable = 0;
		}
	}

	@Override
	public int getCount()
	{
		if ( this.singleFlag )
		{
			return Math.min( 1, this.initial + this.creatable + this.npcBuyable + this.mallBuyable + this.foldable + this.pullable );
		}

		return this.initial + this.creatable + this.npcBuyable + this.mallBuyable + this.foldable + this.pullable;
	}

	public void validate( int maxPrice, int priceLevel )
	{
		if ( priceLevel <= 0 )
		{
			return;
		}

		if ( !this.buyableFlag )
		{
			return;
		}

		int price = StoreManager.getMallPrice( this );

		if ( price <= 0 || price > maxPrice )
		{
			this.mallBuyable = 0;
		}
	}

	public static final int TOTAL_MASK = 0xFF;
	public static final int SUBTOTAL_MASK = 0x0F;
	public static final int INITIAL_SHIFT = 8;
	public static final int CREATABLE_SHIFT = 12;
	public static final int NPCBUYABLE_SHIFT = 16;
	public static final int FOLDABLE_SHIFT = 20;
	public static final int PULLABLE_SHIFT = 24;
	public static final int BUYABLE_FLAG = 1 << 28;
	public static final int AUTOMATIC_FLAG = 1 << 29;
	public static final int CONDITIONAL_FLAG = 1 << 30;

	public int initial;
	public int creatable;
	public int npcBuyable;
	public int mallBuyable;
	public int foldable;
	public int pullable;

	public boolean buyableFlag;
	public boolean automaticFlag;
	public boolean conditionalFlag;
	public boolean singleFlag;
}
