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

package net.sourceforge.kolmafia;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PixelRequest extends ItemCreationRequest
{
	private static final Pattern WHICH_PATTERN = Pattern.compile( "makewhich=(\\d+)" );

	public PixelRequest( int itemId )
	{
		super( "mystic.php", itemId );

		addFormField( "action", "makepixel" );
		addFormField( "makewhich", String.valueOf( itemId ) );
	}

	public void reconstructFields()
	{
	}

	public void run()
	{
		// Attempting to make the ingredients will pull the
		// needed items from the closet if they are missing.
		// In this case, it will also create the needed white
		// pixels if they are not currently available.

		if ( !makeIngredients() )
			return;

		KoLmafia.updateDisplay( "Creating " + getQuantityNeeded() + " " + getName() + "..." );
		addFormField( "quantity", String.valueOf( getQuantityNeeded() ) );
		super.run();
	}

	public static boolean registerRequest( String urlString )
	{
		Matcher itemMatcher = WHICH_PATTERN.matcher( urlString );
		if ( !itemMatcher.find() )
			return true;

		int itemId = StaticEntity.parseInt( itemMatcher.group(1) );
		int quantity = 1;

		if ( urlString.indexOf( "makemax=on" ) != -1 )
		{
			quantity = getInstance( itemId ).getQuantityPossible();
		}
		else
		{
			Matcher quantityMatcher = QUANTITY_PATTERN.matcher( urlString );
			if ( quantityMatcher.find() )
				quantity = StaticEntity.parseInt( quantityMatcher.group(1) );
		}

		AdventureResult [] ingredients = ConcoctionsDatabase.getIngredients( itemId );
		for ( int i = 0; i < ingredients.length; ++i )
			StaticEntity.getClient().processResult( ingredients[i].getInstance( -1 * ingredients[i].getCount() * quantity ) );

		RequestLogger.updateSessionLog();
		RequestLogger.updateSessionLog( "make " + quantity + " " + TradeableItemDatabase.getItemName( itemId ) );

		return true;
	}
}

