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

import net.sourceforge.kolmafia.session.ResultProcessor;

import net.sourceforge.kolmafia.utilities.StringUtilities;

public class Crimbo07Request
	extends CreateItemRequest
{
	private static final Pattern CREATE_PATTERN = Pattern.compile( "crimbo07.php.*whichitem=(\\d+).*quantity=(\\d+)" );

	public Crimbo07Request( final Concoction conc )
	{
		super( "crimbo07.php", conc );

		this.addFormField( "place", "toys" );
		this.addFormField( "action", "toys" );
		this.addFormField( "whichitem", String.valueOf( this.getItemId() ) );
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

	public static final boolean registerRequest( final String urlString )
	{
		Matcher createMatcher = Crimbo07Request.CREATE_PATTERN.matcher( urlString );
		if ( !createMatcher.find() )
		{
			return false;
		}

		// Item ID of the base item
		int itemId = StringUtilities.parseInt( createMatcher.group( 1 ) );
		int quantity = StringUtilities.parseInt( createMatcher.group( 2 ) );

		AdventureResult[] ingredients = ConcoctionDatabase.getIngredients( itemId );
		StringBuffer text = new StringBuffer();
		text.append( "Combine " );

		for ( int i = 0; i < ingredients.length; ++i )
		{
			if ( i > 0 )
			{
				text.append( " + " );
			}

			text.append( ingredients[ i ].getCount() * quantity );
			text.append( " " );
			text.append( ingredients[ i ].getName() );
			ResultProcessor.processResult(
				ingredients[ i ].getInstance( -1 * ingredients[ i ].getCount() * quantity ) );
		}

		RequestLogger.updateSessionLog();
		RequestLogger.updateSessionLog( text.toString() );

		return true;
	}
}
