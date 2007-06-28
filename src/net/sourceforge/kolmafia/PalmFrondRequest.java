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

public class PalmFrondRequest extends ItemCreationRequest
{
	public static final int PALM_FROND = 2605;

	public static final AdventureResult MANUAL = new AdventureResult( "Hugo's Weaving Manual", -1 );
	public static final AdventureResult FRONDS = new AdventureResult( "palm frond", -1 );

	private static final Pattern FROND_PATTERN = Pattern.compile( "quantity=(\\d+)" );

	private int fronds;

	public PalmFrondRequest( int itemId )
	{
		super( "multiuse.php", itemId );

		AdventureResult [] ingredients = ConcoctionsDatabase.getIngredients( itemId );
		if ( ingredients != null )
			for ( int i = 0; i < ingredients.length; ++i )
			{
				if ( ingredients[i].getItemId() == PALM_FROND )
					this.fronds = ingredients[i].getCount();
			}

		this.addFormField( "whichitem", String.valueOf( PALM_FROND ) );
		this.addFormField( "action", "useitem" );
		this.addFormField( "quantity", String.valueOf( this.fronds ) );
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

		// Make sure you have a weaving manual

		if ( !inventory.contains( MANUAL ) )
		{
			// You can currently weave even if you don't have the
			// manual. I've reported a bug, so that may change...
		}

		for ( int i = 1; i <= this.getQuantityNeeded(); ++i )
		{
			KoLmafia.updateDisplay( "Creating " + this.getName() + " (" + i + " of " + this.getQuantityNeeded() + ")..." );
			super.run();
		}
	}

	public void processResults()
	{
		// "You can't figure out what to do with this thing. Maybe you
		//  should mess with more than one of them at a time."

		// "You can't weave anything out of that quantity of palm
		//  fronds."

		if ( this.responseText.indexOf( "You can't" ) != -1 )
		{
			KoLmafia.updateDisplay( ERROR_STATE, "You can't make that item." );
			return;
		}
	}

	public static boolean registerRequest( String urlString )
	{
		Matcher frondMatcher = FROND_PATTERN.matcher( urlString );

		if ( !frondMatcher.find() )
			return true;

		int fronds = StaticEntity.parseInt( frondMatcher.group(1) );

		RequestLogger.updateSessionLog();
		RequestLogger.updateSessionLog( "Weave " + fronds + " palm fronds" );

		StaticEntity.getClient().processResult( new AdventureResult( PALM_FROND, 0 - fronds ) );

		return true;
	}
}
