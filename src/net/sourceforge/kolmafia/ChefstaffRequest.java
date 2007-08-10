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

public class ChefstaffRequest extends ItemCreationRequest
{
	private static final Pattern WHICH_PATTERN = Pattern.compile( "whichstaff=(\\d+)" );

	public ChefstaffRequest( int itemId )
	{
		super( "guild.php", itemId );

		this.addFormField( "action", "makestaff" );

                // The first ingredient is the staff we will upgrade
		AdventureResult [] ingredients = ConcoctionsDatabase.getIngredients( itemId );
                AdventureResult staff = ingredients[0];

		this.addFormField( "whichstaff", String.valueOf( staff.getItemId() ) );
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

	public static final boolean registerRequest( String urlString )
	{
		Matcher itemMatcher = WHICH_PATTERN.matcher( urlString );
		if ( !itemMatcher.find() )
			return true;

		// Item ID of the base staff
		int baseId = StaticEntity.parseInt( itemMatcher.group(1) );

		// Find chefstaff item ID
		int itemId = ConcoctionsDatabase.findConcoction( STAFF, baseId );

		StringBuffer chefstaffString = new StringBuffer();
		chefstaffString.append( "Chefstaff " );

		AdventureResult [] ingredients = ConcoctionsDatabase.getIngredients( itemId );
		for ( int i = 0; i < ingredients.length; ++i )
		{
			if ( i > 0 )
				chefstaffString.append( ", " );

			chefstaffString.append( ingredients[i].getCount() );
			chefstaffString.append( " " );
			chefstaffString.append( ingredients[i].getName() );

			StaticEntity.getClient().processResult( ingredients[i].getInstance( -1 * ingredients[i].getCount() ) );
		}

		RequestLogger.updateSessionLog();
		RequestLogger.updateSessionLog( chefstaffString.toString() );

		return true;
	}
}

