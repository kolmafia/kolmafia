/**
 * Copyright (c) 2005, KoLmafia development team
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
 *  [3] Neither the name "KoLmafia development team" nor the names of
 *      its contributors may be used to endorse or promote products
 *      derived from this software without specific prior written
 *      permission.
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

public class PixelRequest extends ItemCreationRequest
{
	public static final int WHITE_PIXEL = 459;
	public static final int BLACK_PIXEL = 460;
	public static final int RED_PIXEL = 461;
	public static final int GREEN_PIXEL = 462;
	public static final int BLUE_PIXEL = 463;

	private AdventureResult [] ingredientCosts;

	public PixelRequest( KoLmafia client, int itemID, int quantityNeeded )
	{
		super( client, "town_wrong.php", itemID, quantityNeeded );
		addFormField( "place", "crackpot" );

		ingredientCosts = ConcoctionsDatabase.getIngredients( itemID );
		if ( ingredientCosts != null )
			for ( int i = 0; i < ingredientCosts.length; ++i )
				ingredientCosts[i] = ingredientCosts[i].getNegation();

		addFormField( "action", "makepixel" );
		addFormField( "makewhich", String.valueOf( itemID ) );
	}

	public void run()
	{
		// Attempting to make the ingredients will pull the
		// needed items from the closet if they are missing.
		// In this case, it will also create the needed white
		// pixels if they are not currently available.

		makeIngredients();

                int quantity = getQuantityNeeded();
		// Disable controls
		updateDisplay( DISABLED_STATE, "Creating " + quantity + " " + getDisplayName() + "..." );
		addFormField( "quantity", String.valueOf( quantity ) );

		// Run the request
		super.run();

		// Account for the results
		client.processResult( new AdventureResult( getItemID(), quantity ) );
                // All right. How can I automate this?
		for ( int j = 0; j < quantity; ++j )
			for ( int i = 0; i < ingredientCosts.length; ++i )
                                client.processResult( ingredientCosts[i] );
	}
}
