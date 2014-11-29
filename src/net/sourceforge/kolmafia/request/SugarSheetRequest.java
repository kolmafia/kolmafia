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

import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestLogger;

import net.sourceforge.kolmafia.objectpool.Concoction;
import net.sourceforge.kolmafia.objectpool.ConcoctionPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;

import net.sourceforge.kolmafia.session.ResultProcessor;

import net.sourceforge.kolmafia.utilities.StringUtilities;

public class SugarSheetRequest
	extends CreateItemRequest
{
	private static final Pattern ITEM_PATTERN = Pattern.compile( "whichitem=(\\d+)" );

	public SugarSheetRequest( final Concoction conc )
	{
		// http://www.kingdomofloathing.com/shop.php?whichshop=sugarsheets&action=buyitem&quantity=1&whichrow=329
		// quantity field is not needed and is not used
		super( "shop.php", conc );

		this.addFormField( "whichshop", "sugarsheets" );
		this.addFormField( "action", "buyitem" );
		int row = ConcoctionPool.idToRow( this.getItemId() );
		this.addFormField( "whichrow", String.valueOf( row ) );
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
		// Since we create one at a time, override processResults so
		// superclass method doesn't undo ingredient usage.

		String urlString = this.getURLString();
		String responseText = this.responseText;

		// You moisten your sugar sheet, and quickly fold it into a new
		// shape before it dries.

		if ( urlString.contains( "action=buyitem" ) && !responseText.contains( "quickly fold it into a new shape" ) )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "You can't fold that." );
			return;
		}

		SugarSheetRequest.parseResponse( urlString, responseText );
	}

	public static void parseResponse( final String urlString, final String responseText )
	{
		if ( !urlString.startsWith( "shop.php" ) || !urlString.contains( "whichshop=sugarsheets" ) )
		{
			return;
		}

		if ( !responseText.contains( "quickly fold it into a new shape" ) )
		{
			return;
		}

		// Folding always uses exactly one sugar sheet
		ResultProcessor.processItem( ItemPool.SUGAR_SHEET, -1 );
	}

	public static final boolean registerRequest( final String urlString )
	{
		if ( !urlString.startsWith( "shop.php" ) || !urlString.contains( "whichshop=sugarsheets" ) )
		{
			return false;
		}

		Matcher rowMatcher = GenericRequest.WHICHROW_PATTERN.matcher( urlString );
		if ( !rowMatcher.find() )
		{
			return true;
		}

		int row = StringUtilities.parseInt( rowMatcher.group( 1 ) );
		int itemId = ConcoctionPool.rowToId( row );

		CreateItemRequest item = CreateItemRequest.getInstance( itemId );
		if ( itemId < 0 )
		{
			return true; // this is an unknown item
		}

		// The quantity is always 1
		if ( item.getQuantityPossible() < 1 )
		{
			return true; // attempt will fail
		}

		RequestLogger.updateSessionLog();
		RequestLogger.updateSessionLog( "Fold sugar sheet" );

		return true;
	}
}
