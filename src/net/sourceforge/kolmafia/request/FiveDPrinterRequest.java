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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;

import net.sourceforge.kolmafia.objectpool.Concoction;
import net.sourceforge.kolmafia.objectpool.ConcoctionPool;

import net.sourceforge.kolmafia.persistence.ConcoctionDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.preferences.Preferences;

public class FiveDPrinterRequest
	extends CreateItemRequest
{
	public FiveDPrinterRequest( final Concoction conc )
	{
		// shop.php?whichshop=5dprinter&action=buyitem&quantity=1&whichrow=340&pwd=15a3ed7ce8a5e0c8a6c7e08a03fca040
		// quantity field is not needed and is not used
		super( "shop.php", conc );

		this.addFormField( "whichshop", "5dprinter" );
		this.addFormField( "action", "buyitem" );
		int row = ConcoctionPool.idToRow( this.getItemId() );
		this.addFormField( "whichrow", String.valueOf( row ) );
	}

	@Override
	public void reconstructFields()
	{
	}

	@Override
	public void run()
	{
		// Attempting to make the ingredients will pull the
		// needed items from the closet if they are missing.

		if ( !this.makeIngredients() )
		{
			return;
		}

		super.run();
	}

	@Override
	public void processResults()
	{
		String urlString = this.getURLString();
		String responseText = this.responseText;

		if ( urlString.contains( "action=buyitem" ) && !responseText.contains( "You acquire" ) )
		{
			KoLmafia.updateDisplay( KoLConstants.MafiaState.ERROR, "5d printing was unsuccessful." );
			return;
		}

		FiveDPrinterRequest.parseResponse( urlString, responseText );
	}

	private static final Pattern DISCOVERY_PATTERN = Pattern.compile( "descitem\\((\\d+)\\)" );

	public static void parseResponse( final String urlString, final String responseText )
	{
		if ( !urlString.startsWith( "shop.php" ) || !urlString.contains( "whichshop=5dprinter" ) )
		{
			return;
		}

		Matcher matcher = FiveDPrinterRequest.DISCOVERY_PATTERN.matcher( responseText );
		while ( matcher.find() )
		{
			int id = ItemDatabase.getItemIdFromDescription( matcher.group( 1 ) );
			String pref = "unknownRecipe" + id;
			if ( id > 0 && Preferences.getBoolean( pref ) )
			{
				KoLmafia.updateDisplay( "You know the recipe for " +
					ItemDatabase.getItemName( id ) );
				Preferences.setBoolean( pref, false );
				ConcoctionDatabase.setRefreshNeeded( true );
			}
		}

		NPCPurchaseRequest.parseShopRowResponse( urlString, responseText );
	}

	public static final boolean registerRequest( final String urlString )
	{
		if ( !urlString.startsWith( "shop.php" ) || !urlString.contains( "whichshop=5dprinter" ) )
		{
			return false;
		}

		return NPCPurchaseRequest.registerShopRowRequest( urlString );
	}
}
