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

package net.sourceforge.kolmafia.request;

import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;

import net.sourceforge.kolmafia.objectpool.AdventurePool;
import net.sourceforge.kolmafia.objectpool.Concoction;
import net.sourceforge.kolmafia.objectpool.ConcoctionPool;

public class KOLHSRequest
	extends CreateItemRequest
{
	public static final boolean isKOLHSLocation( final int adventureId )
	{
		switch ( adventureId )
		{
		case AdventurePool.THE_HALLOWED_HALLS:
		case AdventurePool.SHOP_CLASS:
		case AdventurePool.CHEMISTRY_CLASS:
		case AdventurePool.ART_CLASS:
			return true;
		}
		return false;
	}

	private static final String getShopId( final Concoction conc )
	{
		switch ( conc.getMixingMethod() )
		{
		case CHEMCLASS:
			return "kolhs_chem";
		case ARTCLASS:
			return "kolhs_art";
		case SHOPCLASS:
			return "kolhs_shop";
		}

		return "";
	}

	private static final String shopIDToClassName( final String shopID )
	{
		return	shopID.equals( "kolhs_chem" ) ? "Chemistry Class" :
			shopID.equals( "kolhs_art" ) ? "Art Class" :
			shopID.equals( "kolhs_shop" ) ? "Shop Class" :
			shopID;
	}

	public KOLHSRequest( final Concoction conc )
	{
		super( "shop.php", conc );
		this.addFormField( "whichshop", KOLHSRequest.getShopId( conc ) );
		this.addFormField( "action", "buyitem" );
		int row = ConcoctionPool.idToRow( this.getItemId() );
		this.addFormField( "whichrow", String.valueOf( row ) );
	}

	@Override
	public void run()
	{
		// Attempt to retrieve the ingredients
		if ( !this.makeIngredients() )
		{
			return;
		}

		if ( true )
		{
			String shopID = NPCPurchaseRequest.getShopId( this.getURLString() );
			String className = KOLHSRequest.shopIDToClassName( shopID );
			KoLmafia.updateDisplay( "Visit the " + className + " after school to make that." );
			return;
		}

		KoLmafia.updateDisplay( "Creating " + this.getQuantityNeeded() + " " + this.getName() + "..." );
		this.addFormField( "quantity", String.valueOf( this.getQuantityNeeded() ) );
		super.run();
	}

	@Override
	public void processResults()
	{
		String urlString = this.getURLString();
		String responseText = this.responseText;

		if ( urlString.contains( "action=buyitem" ) && !responseText.contains( "You acquire" ) )
		{
			String shopID = NPCPurchaseRequest.getShopId( urlString );
			String className = KOLHSRequest.shopIDToClassName( shopID );
			KoLmafia.updateDisplay( KoLConstants.MafiaState.ERROR, className + " creation was unsuccessful." );
			return;
		}

		KOLHSRequest.parseResponse( urlString, responseText );
	}

	public static void parseResponse( final String urlString, final String responseText )
	{
		if ( !urlString.startsWith( "shop.php" ) || !urlString.contains( "whichshop=kolhs_" ) )
		{
			return;
		}

		NPCPurchaseRequest.parseShopRowResponse( urlString, responseText );
	}

	public static final boolean registerRequest( final String urlString )
	{
		if ( !urlString.startsWith( "shop.php" ) || !urlString.contains( "whichshop=kolhs_" ) )
		{
			return false;
		}

		return NPCPurchaseRequest.registerShopRowRequest( urlString );
	}
}
