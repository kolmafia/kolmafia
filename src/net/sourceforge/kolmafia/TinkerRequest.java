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
import java.util.List;
import java.util.ArrayList;
import net.java.dev.spellcast.utilities.SortedListModel;

public class TinkerRequest extends ItemCreationRequest
{
        private AdventureResult [] ingredients;

	public TinkerRequest( KoLmafia client, int itemID, int quantityNeeded )
	{
		super( client, "gnomes.php", itemID, quantityNeeded );

		addFormField( "place", "tinker" );
		addFormField( "action", "tinksomething" );

                ingredients = ConcoctionsDatabase.getIngredients( itemID );
                if (ingredients != null && ingredients.length == 3)
                {
                        addFormField( "item1", String.valueOf( ingredients[0].getItemID() ) );
                        addFormField( "item2", String.valueOf( ingredients[1].getItemID() ) );
                        addFormField( "item3", String.valueOf( ingredients[2].getItemID() ) );
                }
	}

	public void run()
	{
		for ( int i = 0; i < getQuantityNeeded(); ++i )
		{
                        // Disable controls
			updateDisplay( DISABLED_STATE, "Creating " + getDisplayName() + " (" + (i+1) + " of " + getQuantityNeeded() + ")..." );

                        // Run the request
                        super.run();

                        // Account for the results
                        client.processResult( new AdventureResult( getName(), 1 ) );
                        for ( int j = 0; j < ingredients.length; ++j )
                                client.processResult( new AdventureResult( ingredients[j].getItemID(), -1 ) );
		}
	}
}
