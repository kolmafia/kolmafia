/**
 * Copyright (c) 2005-2012, KoLmafia development team
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

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestLogger;

import net.sourceforge.kolmafia.objectpool.Concoction;

import net.sourceforge.kolmafia.persistence.ConcoctionDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase;

import net.sourceforge.kolmafia.session.ResultProcessor;

import net.sourceforge.kolmafia.utilities.StringUtilities;

public class PixelRequest
	extends CreateItemRequest
{
	public PixelRequest( final Concoction conc )
	{
		super( "shop.php", conc );

		this.addFormField( "whichshop", "mystic" );
		this.addFormField( "action", "buyitem" );
		this.addFormField( "whichitem", String.valueOf( this.getItemId() ) );
	}

	@Override
	public void reconstructFields()
	{
		this.constructURLString( this.getURLString() );
	}

	@Override
	public void run()
	{
		// Attempting to make the ingredients will pull the
		// needed items from the closet if they are missing.
		// In this case, it will also create the needed white
		// pixels if they are not currently available.

		if ( !this.makeIngredients() )
		{
			return;
		}

		KoLmafia.updateDisplay( "Creating " + this.getQuantityNeeded() + " " + this.getName() + "..." );
		this.addFormField( "quantity", String.valueOf( this.getQuantityNeeded() ) );
		super.run();
	}

	private static final Pattern ITEM_PATTERN = Pattern.compile( "name=whichitem value=([\\d]+)[^>]*?>.*?descitem.([\\d]+)[^>]*><b>([^&]*)</b>&nbsp;", Pattern.DOTALL );

	public static void parseResponse( final String urlString, final String responseText )
	{
		if ( !urlString.startsWith( "shop.php" ) || !urlString.contains( "whichshop=mystic" ) )
		{
			return;
		}

		// Learn new trade items by simply visiting Phineas
		Matcher matcher = ITEM_PATTERN.matcher( responseText );
		while ( matcher.find() )
		{
			int id = StringUtilities.parseInt( matcher.group(1) );
			String desc = matcher.group(2);
			String name = matcher.group(3);
			String data = ItemDatabase.getItemDataName( id );
			if ( data == null || !data.equalsIgnoreCase( name ) )
			{
				ItemDatabase.registerItem( id, name.toLowerCase(), desc );
			}
		}
	}

	public static final boolean registerRequest( final String urlString )
	{
		Matcher itemMatcher = CreateItemRequest.WHICHITEM_PATTERN.matcher( urlString );
		if ( !itemMatcher.find() )
		{
			return true;
		}

		int itemId = StringUtilities.parseInt( itemMatcher.group( 1 ) );

		CreateItemRequest pixelItem = CreateItemRequest.getInstance( itemId );
		if ( pixelItem == null )
		{
			return true; // this is an unknown item
		}

		int quantity = 1;
		if ( urlString.contains( "buymax=" ) )
		{
			quantity = pixelItem.getQuantityPossible();
		}
		else
		{
			Matcher quantityMatcher = CreateItemRequest.QUANTITY_PATTERN.matcher( urlString );
			if ( quantityMatcher.find() )
			{
				String quantityString = quantityMatcher.group( 2 ).trim();
				quantity = quantityString.length() == 0 ? 1 : StringUtilities.parseInt( quantityString );
			}
		}

		if ( quantity > pixelItem.getQuantityPossible() )
		{
			return true; // attempt will fail
		}

		StringBuilder pixelString = new StringBuilder();
		pixelString.append( "Trade " );

		AdventureResult[] ingredients = ConcoctionDatabase.getIngredients( itemId );
		for ( int i = 0; i < ingredients.length; ++i )
		{
			if ( i > 0 )
			{
				pixelString.append( ", " );
			}

			pixelString.append( ingredients[ i ].getCount() * quantity );
			pixelString.append( " " );
			pixelString.append( ingredients[ i ].getName() );

			ResultProcessor.processResult(
				ingredients[ i ].getInstance( -1 * ingredients[ i ].getCount() * quantity ) );
		}

		RequestLogger.updateSessionLog();
		RequestLogger.updateSessionLog( pixelString.toString() );

		return true;
	}
}
