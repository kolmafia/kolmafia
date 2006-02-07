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

public class StarChartRequest extends ItemCreationRequest
{
	public static final int STAR = 654;
	public static final int LINE = 655;

	private int stars, lines;
	private static final AdventureResult usedCharts = new AdventureResult( "star chart", -1 );

	public StarChartRequest( KoLmafia client, int itemID, int quantityNeeded )
	{
		super( client, "starchart.php", itemID, quantityNeeded );

		AdventureResult [] ingredients = ConcoctionsDatabase.getIngredients( itemID );
		if ( ingredients != null )
			for ( int i = 0; i < ingredients.length; ++i )
			{
				if ( ingredients[i].getItemID() == STAR )
					stars = ingredients[i].getCount();
				else if ( ingredients[i].getItemID() == LINE)
					lines = ingredients[i].getCount();
			}

		addFormField( "action", "makesomething" );
		addFormField( "numstars", String.valueOf( stars ) );
		addFormField( "numlines", String.valueOf( lines ) );
	}

	public void run()
	{
		// Attempting to make the ingredients will pull the
		// needed items from the closet if they are missing.

		makeIngredients();

		if ( !client.permitsContinue() )
			return;

		// Intermediate variables so you don't constantly
		// instantiate new adventure results each time you
		// create the item.

		AdventureResult usedStars = new AdventureResult( STAR, 0 - stars );
		AdventureResult usedLines = new AdventureResult( LINE, 0 - lines );
		AdventureResult singleCreation = new AdventureResult( getItemID(), 1 );

		for ( int i = 1; i <= getQuantityNeeded(); ++i )
		{
			// Disable controls
			updateDisplay( NORMAL_STATE, "Creating " + getName() + " (" + i + " of " + getQuantityNeeded() + ")..." );

			// Run the request
			super.run();

			// It's possible to fail. For example, you can't make a
			// shirt without the Torso Awaregness skill.

			// "You can't seem to make a reasonable picture out of
			// that number of stars and lines."

			if ( responseText.indexOf( "reasonable picture" ) != -1 )
			{
				updateDisplay( ERROR_STATE, "You can't make that item." );
				client.cancelRequest();
				return;
			}

			// Account for the results
			client.processResult( usedCharts );
			client.processResult( usedStars );
			client.processResult( usedLines );
		}
	}
}
