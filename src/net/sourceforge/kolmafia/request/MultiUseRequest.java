/**
 * Copyright (c) 2005-2008, KoLmafia development team
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
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestLogger;

import net.sourceforge.kolmafia.persistence.ConcoctionDatabase;
import net.sourceforge.kolmafia.request.UseItemRequest;

import net.sourceforge.kolmafia.objectpool.ConcoctionPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.session.ResultProcessor;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class MultiUseRequest
	extends CreateItemRequest
{
	private static final Pattern USE_PATTERN = Pattern.compile( "multiuse.php.*quantity=(\\d+).*whichitem=(\\d+)" );
	private AdventureResult ingredient;

	public MultiUseRequest( final int itemId )
	{
		super( "multiuse.php", itemId );

		AdventureResult[] ingredients = ConcoctionDatabase.getIngredients( itemId );

		// There must be exactly one ingredient
		if ( ingredients == null || ingredients.length != 1 )
		{
			return;
		}

		this.ingredient = ingredients[ 0 ];
		int use = this.ingredient.getItemId();
		int count = this.ingredient.getCount();

		this.addFormField( "action", "useitem" );
		this.addFormField( "quantity", String.valueOf( count ) );
		this.addFormField( "whichitem", String.valueOf( use ) );
	}

	public void reconstructFields()
	{
	}

	public void run()
	{
		// Attempting to make the ingredients will pull the
		// needed items from the closet if they are missing.

		if ( !this.makeIngredients() )
		{
			return;
		}

		for ( int i = 1; i <= this.getQuantityNeeded() && KoLmafia.permitsContinue(); ++i )
		{
			KoLmafia.updateDisplay( "Creating " + this.getName() + " (" + i + " of " + this.getQuantityNeeded() + ")..." );
			super.run();
		}
	}

	static final String[] ERRORS =
	{
		"You can't figure out what to do with this thing. Maybe you should mess with more than one of them at a time.",
		"You can't weave anything out of that quantity of palm fronds.",
		"You tie the mummy wrapping in a bow, but it's not a very good bow, so you untie it and put it back in your pocket.",
		"You can't figure out how to do anything with that particular number of wrappings.",
		"You mess with the duct tape for a while, but can't figure out how to make anything interesting",
		"You can't make anything interesting out of that number of bits of clingfilm.",
		"Nothing worthwhile can be sculpted out of a single balloon. Trust me on that one.",
		"You twist the balloons around for a while, but you can't figure out how to make anything interesting",
	};

	public void processResults()
	{
		// Is there a general way to detect a failure?

		String text = this.responseText;
		for ( int i = 0; i < ERRORS.length; ++i )
		{
			String error = ERRORS[i];
			if ( text.indexOf( error ) != -1 )
			{
				KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, error );
				return;
			}
		}
	}

	public static final boolean registerRequest( final String urlString )
	{
		Matcher useMatcher = MultiUseRequest.USE_PATTERN.matcher( urlString );
		if ( !useMatcher.find() )
		{
			return false;
		}

		// Item ID of the base item
		int baseId = StringUtilities.parseInt( useMatcher.group( 2 ) );

		// Find result item ID
		int result = ConcoctionPool.findConcoction( KoLConstants.MULTI_USE, baseId );

		// If this is not a concoction, let somebody else log this.
		if ( result == -1 )
		{
			return false;
		}

		int count = StringUtilities.parseInt( useMatcher.group( 1 ) );

		UseItemRequest.setLastItemUsed( ItemPool.get( baseId, count ) );
		AdventureResult base = ItemPool.get( baseId, 0 - count );

		RequestLogger.updateSessionLog();
		RequestLogger.updateSessionLog( "Use " + count + " " + base.getName() );

		ResultProcessor.processResult( base );

		return true;
	}
}
