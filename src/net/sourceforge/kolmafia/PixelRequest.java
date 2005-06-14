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

public class PixelRequest extends ItemCreationRequest
{
	private int white, black, red, green, blue;

        public static final PixelRequest WHITE_PIXEL = new PixelRequest( "white pixel", 0, 0, 1, 1, 1 );
        public static final PixelRequest RED_PIXEL_POTION = new PixelRequest( "red pixel potion", 0, 3, 2, 0, 0 );
        public static final PixelRequest BLUE_PIXEL_POTION = new PixelRequest( "blue pixel potion", 0, 3, 0, 0, 2 );
        public static final PixelRequest GREEN_PIXEL_POTION = new PixelRequest( "green pixel potion", 0, 4, 0, 3, 0 );
        public static final PixelRequest PURPLE_PIXEL_PIE = new PixelRequest( "purple pixel pie", 5, 0, 2, 0, 2 );
        public static final PixelRequest PIXEL_HAT = new PixelRequest( "pixel hat", 10, 0, 0, 15, 0 );
        public static final PixelRequest PIXEL_PANTS = new PixelRequest( "pixel pants", 0, 15, 20, 0, 0 );
        public static final PixelRequest PIXEL_SWORD = new PixelRequest( "pixel sword", 20, 10, 0, 0, 10 );
        public static final PixelRequest DIGITAL_KEY = new PixelRequest( "digital key", 30, 0, 0, 0, 0 );

	private static final PixelRequest [] PIXEL_ITEMS =
	{
		WHITE_PIXEL, RED_PIXEL_POTION, BLUE_PIXEL_POTION, GREEN_PIXEL_POTION,
		PURPLE_PIXEL_PIE, PIXEL_HAT, PIXEL_PANTS, PIXEL_SWORD, DIGITAL_KEY
	};

	private static final AdventureResult WHITE = new AdventureResult( "white pixel", 0 );
	private static final AdventureResult BLACK = new AdventureResult( "black pixel", 0 );
	private static final AdventureResult RED = new AdventureResult( "red pixel", 0 );
	private static final AdventureResult GREEN = new AdventureResult( "green pixel", 0 );
	private static final AdventureResult BLUE = new AdventureResult( "blue pixel", 0 );

	private PixelRequest( String name, int white, int black, int red, int green, int blue )
	{
		super( null, "town_wrong.php", TradeableItemDatabase.getItemID( name ), 0 );

		this.white = white;
		this.black = black;
		this.red = red;
		this.green = green;
		this.blue = blue;
	}

	public PixelRequest( KoLmafia client, int itemID, int quantityNeeded )
	{
		super( client, "town_wrong.php", itemID, quantityNeeded );

		for ( int i = 0; i < PIXEL_ITEMS.length; ++i )
			if ( PIXEL_ITEMS[i].getItemID() == itemID )
			{
				this.white = PIXEL_ITEMS[i].white;
				this.black = PIXEL_ITEMS[i].black;
				this.red = PIXEL_ITEMS[i].red;
				this.green = PIXEL_ITEMS[i].green;
				this.blue = PIXEL_ITEMS[i].blue;
			}

		addFormField( "action", "makepixel" );
		addFormField( "makewhich", String.valueOf( itemID ) );
	}

	public static List getPossibleCombinations( KoLmafia client )
	{
		SortedListModel inventory = client.getInventory();

		int whiteValue = WHITE.getCount( inventory );
		int blackValue = BLACK.getCount( inventory );
		int redValue = RED.getCount( inventory );
		int greenValue = GREEN.getCount( inventory );
		int blueValue = BLUE.getCount( inventory );

		List results = new ArrayList();
		for ( int i = 0; i < PIXEL_ITEMS.length; ++i )
		{
			int maximumPossible = Integer.MAX_VALUE;

			if ( PIXEL_ITEMS[i].white > 0 )
				maximumPossible = Math.min( maximumPossible, whiteValue / PIXEL_ITEMS[i].white );
			if ( PIXEL_ITEMS[i].black > 0 )
				maximumPossible = Math.min( maximumPossible, blackValue / PIXEL_ITEMS[i].black );
			if ( PIXEL_ITEMS[i].red > 0 )
				maximumPossible = Math.min( maximumPossible, redValue / PIXEL_ITEMS[i].red );
			if ( PIXEL_ITEMS[i].green > 0 )
				maximumPossible = Math.min( maximumPossible, greenValue / PIXEL_ITEMS[i].green );
			if ( PIXEL_ITEMS[i].blue > 0 )
				maximumPossible = Math.min( maximumPossible, blueValue / PIXEL_ITEMS[i].blue );

			if ( maximumPossible > 0 )
				results.add( new PixelRequest( client, PIXEL_ITEMS[i].getItemID(), maximumPossible ) );
		}

		return results;
	}

	public void run()
	{
		for ( int i = 0; i < getQuantityNeeded(); ++i )
		{
			updateDisplay( DISABLED_STATE, "Creating " + getDisplayName() + " (" + (i+1) + " of " + getQuantityNeeded() + ")..." );
			makePixelItem();
		}
	}

	private void makePixelItem()
	{
		super.run();
		client.processResult( new AdventureResult( "white pixel", 0 - white ) );
		client.processResult( new AdventureResult( "black pixel", 0 - black ) );
		client.processResult( new AdventureResult( "red pixel", 0 - red ) );
		client.processResult( new AdventureResult( "green pixel", 0 - green ) );
		client.processResult( new AdventureResult( "blue pixel", 0 - blue ) );
		client.processResult( new AdventureResult( getName(), 1 ) );
	}
}
