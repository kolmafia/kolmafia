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
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestLogger;

import net.sourceforge.kolmafia.objectpool.Concoction;

import net.sourceforge.kolmafia.persistence.ConcoctionDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase;

import net.sourceforge.kolmafia.session.ResultProcessor;

import net.sourceforge.kolmafia.utilities.StringUtilities;

public class ChefStaffRequest
	extends CreateItemRequest
{
	private static final Pattern WHICH_PATTERN = Pattern.compile( "whichstaff=(\\d+)" );

	public ChefStaffRequest( final Concoction conc )
	{
		super( "guild.php", conc );

		this.addFormField( "action", "makestaff" );

		// The first ingredient is the staff we will upgrade
		AdventureResult[] ingredients = conc.getIngredients();
		AdventureResult staff = ingredients[ 0 ];

		this.addFormField( "whichstaff", String.valueOf( staff.getItemId() ) );
	}

	public void reconstructFields()
	{
		this.constructURLString( this.getURLString() );
	}

	public void run()
	{
		// Attempting to make the ingredients will pull the
		// needed items from the closet if they are missing.

		if ( this.makeIngredients() )
		{
			super.run();
		}
	}

	public void processResults()
	{
		// Since we create one at a time, override processResults so
		// superclass method doesn't undo ingredient usage.

		if ( ChefStaffRequest.parseCreation( this.getURLString(), this.responseText ) )
                {
			KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "You're missing some ingredients." );
                }
	}

	public static final boolean parseCreation( final String urlString, final String responseText )
	{
		if ( responseText.indexOf( "You don't have all of the items I'll need to make that Chefstaff." ) != -1 )
		{
			return true;
		}

		AdventureResult[] ingredients = ChefStaffRequest.staffIngredients( urlString );
		if ( ingredients == null )
		{
			return false;
		}

		for ( int i = 0; i < ingredients.length; ++i )
		{
			AdventureResult ingredient = ingredients[ i ];
			ResultProcessor.processResult( ingredient.getInstance( -1 * ingredient.getCount() ) );
		}

		return false;
	}

	private static final AdventureResult[] staffIngredients( final String urlString )
	{
		Matcher itemMatcher = ChefStaffRequest.WHICH_PATTERN.matcher( urlString );
		if ( !itemMatcher.find() )
		{
			return null;
		}

		// Item ID of the base staff
		int baseId = StringUtilities.parseInt( itemMatcher.group( 1 ) );
		String name = ItemDatabase.getItemName( baseId );

		// Find chefstaff recipe
		Concoction concoction = ConcoctionDatabase.chefStaffCreation( name );
		return concoction == null ? null : concoction.getIngredients();
	}

	public static final boolean registerRequest( final String urlString )
	{
		if ( !urlString.startsWith( "guild.php" ) || urlString.indexOf( "action=makestaff" ) == -1 )
		{
			return false;
		}

		AdventureResult[] ingredients = ChefStaffRequest.staffIngredients( urlString );
		if ( ingredients == null )
		{
			return true;
		}

		StringBuffer chefstaffString = new StringBuffer();
		chefstaffString.append( "Chefstaff " );

		for ( int i = 0; i < ingredients.length; ++i )
		{
			if ( i > 0 )
			{
				chefstaffString.append( ", " );
			}

			chefstaffString.append( ingredients[ i ].getCount() );
			chefstaffString.append( " " );
			chefstaffString.append( ingredients[ i ].getName() );
		}

		RequestLogger.updateSessionLog();
		RequestLogger.updateSessionLog( chefstaffString.toString() );

		return true;
	}
}
