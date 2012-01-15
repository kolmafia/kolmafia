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

import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestLogger;

import net.sourceforge.kolmafia.objectpool.Concoction;
import net.sourceforge.kolmafia.objectpool.ItemPool;

import net.sourceforge.kolmafia.session.ResultProcessor;

public class SugarSheetRequest
	extends CreateItemRequest
{
	private static final Pattern ITEM_PATTERN = Pattern.compile( "whichitem=(\\d+)" );

	public SugarSheetRequest( final Concoction conc )
	{
		super( "sugarsheets.php", conc );
		this.addFormField( "action", "fold" );
		this.addFormField( "whichitem", String.valueOf( this.getItemId() ) );
	}

	public void reconstructFields()
	{
		this.constructURLString( this.getURLString() );
	}

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

	public void processResults()
	{
		// Since we create one at a time, override processResults so
		// superclass method doesn't undo ingredient usage.

		if ( SugarSheetRequest.parseCreation( this.getURLString(), this.responseText ) )
		{
			KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "You can't fold that." );
		}
	}

	public static final boolean parseCreation( final String urlString, final String responseText )
	{
		// You moisten your sticker sheet, and quickly fold it into a
		// new shape before it dries.

		if ( responseText.indexOf( "quickly fold it into a new shape" ) == -1 )
		{
			return true;
		}

		Matcher m = SugarSheetRequest.ITEM_PATTERN.matcher( urlString );

		if ( !m.find() )
		{
			return true;
		}

		ResultProcessor.processItem( ItemPool.SUGAR_SHEET, -1 );

		return false;
	}

	public static final boolean registerRequest( final String urlString )
	{
		Matcher m = SugarSheetRequest.ITEM_PATTERN.matcher( urlString );

		if ( !m.find() )
		{
			return true;
		}

		// int itemId = StringUtilities.parseInt( m.group( 1 ) );

		RequestLogger.updateSessionLog();
		RequestLogger.updateSessionLog( "Fold sugar sheet" );

		return true;
	}
}
