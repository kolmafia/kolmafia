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

package net.sourceforge.kolmafia.request;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.StaticEntity;

import net.sourceforge.kolmafia.persistence.ConcoctionDatabase;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class SingleUseRequest
	extends CreateItemRequest
{
	private static final Pattern USE_PATTERN = Pattern.compile( "inv_use.php.*whichitem=(\\d+)" );

	public SingleUseRequest( final int itemId )
	{
		super( "inv_use.php", itemId );

		AdventureResult[] ingredients = ConcoctionDatabase.getIngredients( itemId );

		if ( ingredients == null )
		{
			return;
		}

		int use = ingredients[ 0 ].getItemId();
		this.addFormField( "which", "3" );
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

	public static final boolean registerRequest( final String urlString )
	{
		Matcher useMatcher = SingleUseRequest.USE_PATTERN.matcher( urlString );
		if ( !useMatcher.find() )
		{
			return true;
		}

		// Item ID of the base item
		int baseId = StringUtilities.parseInt( useMatcher.group( 1 ) );

		// Find result item ID
		int result = ConcoctionDatabase.findConcoction( KoLConstants.SINGLE_USE, baseId );

		// If this is not a concoction, let somebody else log this.
		if ( result == -1 )
		{
			return false;
		}

		AdventureResult base = new AdventureResult( baseId, -1 );

		AdventureResult[] ingredients = ConcoctionDatabase.getIngredients( result );
		StringBuffer text = new StringBuffer();
		text.append( "Use " );

		for ( int i = 0; i < ingredients.length; ++i )
		{
			if ( i > 0 )
			{
				text.append( " + " );
			}

			text.append( ingredients[ i ].getCount() );
			text.append( " " );
			text.append( ingredients[ i ].getName() );
			StaticEntity.getClient().processResult( ingredients[ i ].getInstance( -1 * ingredients[ i ].getCount() ) );
		}

		RequestLogger.updateSessionLog();
		RequestLogger.updateSessionLog( text.toString() );

		return true;
	}
}
