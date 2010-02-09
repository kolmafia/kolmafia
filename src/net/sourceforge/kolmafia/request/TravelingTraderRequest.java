/**
 * Copyright (c) 2005-2009, KoLmafia development team
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

import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.session.InventoryManager;
import net.sourceforge.kolmafia.session.ResultProcessor;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class TravelingTraderRequest
	extends GenericRequest
{
	// The traveling trader is looking to acquire:<br><img class='hand
	// item' onclick='descitem(503220568);'
	// src='http://images.kingdomofloathing.com/itemimages/scwad.gif'>
	// <b>twinkly wads</b><br>(You have <b>3,348</b> on you.)

	private static final Pattern ACQUIRE = Pattern.compile( "The traveling trader is looking to acquire.*descitem\\(([\\d]+)\\).*<b>([^<]*)</b><br>\\(You have <b>([\\d,]*|none)</b> on you.\\)" );

	// <tr><td><input type=radio name=whichitem value=4411
	// checked="checked"></td><td><a class=nounder
	// href='javascript:descitem(629749615);'> <img class='hand item'
	// src='http://images.kingdomofloathing.com/itemimages/music.gif'>
	// <b>Inigo's Incantation of Inspiration</b></a></td><td>100 twinkly
	// wads</td></tr

	private static final Pattern ITEM_PATTERN = Pattern.compile( "name=whichitem value=([\\d]+).*?>.*?descitem.*?([\\d]+).*?<b>([^<]*)</b></a></td><td>([\\d]+)", Pattern.DOTALL );

	private TravelingTraderRequest()
	{
		super( "traveler.php" );
	}

	public void processResults()
	{
		TravelingTraderRequest.parseResponse( this.getURLString(), this.responseText );
	}

	public static void parseResponse( final String urlString, final String responseText )
	{
		if ( !urlString.startsWith( "traveler.php" ) )
		{
			return;
		}

		// Learn what item he is trading for and sanity check number of
		// <items> in inventory
		Matcher matcher = ACQUIRE.matcher( responseText );
		if ( matcher.find() )
		{
			String descId = matcher.group( 1 );
			int itemId = ItemDatabase.getItemIdFromDescription( descId );
			if ( itemId != -1 )
			{
				int count = InventoryManager.getCount( itemId );
				String plural = matcher.group( 2 );
				String num = matcher.group( 3 );
				int delta = ( num == null ? 0 :
					      num.equals( "none" ) ? 0 :
					      num.equals( "one" ) ? 1 :
					      StringUtilities.parseInt( num ) ) - count;
				if ( delta != 0 )
				{
					ResultProcessor.processItem( itemId, delta );
				}
			}
		}

		// Learn new trade items by simply visiting the Traveling Trader
		matcher = ITEM_PATTERN.matcher( responseText );
		while ( matcher.find() )
		{
			int itemId = StringUtilities.parseInt( matcher.group(1) );
			String descId = matcher.group(2);
			String itemName = matcher.group(3);
			int cost = StringUtilities.parseInt( matcher.group(4) );

			String data = ItemDatabase.getItemDataName( itemId );
			if ( data == null || !data.equals( itemName ) )
			{
				ItemDatabase.registerItem( itemId, itemName, descId );
			}
		}

		KoLmafia.saveDataOverride();
	}

	public static boolean registerRequest( final String urlString )
	{
		if ( !urlString.startsWith( "traveler.php" ) )
		{
			return false;
		}

		// traveler.php?action=For Gnomeregan!&whichitem=xxxx&quantity=1&tradeall=1&usehagnk=1&pwd

		return false;
	}
}
