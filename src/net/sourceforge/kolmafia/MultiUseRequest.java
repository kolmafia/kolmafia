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

public class MultiUseRequest extends ItemCreationRequest
{
	private static final Pattern USE_PATTERN = Pattern.compile( "multiuse.php.*whichitem=(\\d+).*quantity=(\\d+)" );

	public MultiUseRequest( int itemId )
	{
		super( "multiuse.php", itemId );

		AdventureResult [] ingredients = ConcoctionsDatabase.getIngredients( itemId );

		// There must be exactly one ingredient
		if ( ingredients == null || ingredients.length != 1 )
			return;

		AdventureResult ingredient = ingredients[0];
		int use = ingredient.getItemId();
		int count = ingredient.getCount();

		this.addFormField( "action", "useitem" );
		this.addFormField( "whichitem", String.valueOf( use ) );
		this.addFormField( "quantity", String.valueOf( count ) );
	}

	public void reconstructFields()
	{
	}

	public void run()
	{
		// Attempting to make the ingredients will pull the
		// needed items from the closet if they are missing.

		if ( !this.makeIngredients() )
			return;

		for ( int i = 1; i <= this.getQuantityNeeded(); ++i )
		{
			KoLmafia.updateDisplay( "Creating " + this.getName() + " (" + i + " of " + this.getQuantityNeeded() + ")..." );
			super.run();
		}
	}

	public void processResults()
	{
		// Is there a general way to detect a failure?
	}

	public static final boolean registerRequest( String urlString )
	{
		Matcher useMatcher = USE_PATTERN.matcher( urlString );

		if ( !useMatcher.find() )
			return true;

		int use =  StaticEntity.parseInt( useMatcher.group(1) );
		int count = StaticEntity.parseInt( useMatcher.group(2) );
		AdventureResult item = new AdventureResult( use, 0 - count );

		RequestLogger.updateSessionLog();
		RequestLogger.updateSessionLog( "Use " + count + " " + item.getName() );

		StaticEntity.getClient().processResult( item );

		return true;
	}
}
