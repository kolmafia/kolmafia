/**
 * Copyright (c) 2005-2014, KoLmafia development team
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

package net.sourceforge.kolmafia.webui;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLConstants;

import net.sourceforge.kolmafia.objectpool.ItemPool;

import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.request.PyramidRequest;
import net.sourceforge.kolmafia.request.UseItemRequest;
import net.sourceforge.kolmafia.session.InventoryManager;

public class UseItemDecorator
{
	public static final void decorate( final String location, final StringBuffer buffer )
	{
		if ( location.startsWith( "inventory.php" ) && location.indexOf( "action=message" ) == -1 )
		{
			return;
		}

		// Saved when we executed inv_use.php, whether or not it
		// redirected to inventory.php
		int itemId = UseItemRequest.currentItemId();

		switch ( itemId )
		{
		case ItemPool.BOO_CLUE:
			UseItemDecorator.decorateBooClue( buffer );
			break;

		case ItemPool.BLACK_MARKET_MAP:
			UseItemDecorator.decorateMarketMap( buffer );
			break;
		}
	}

	// <table  width=95%  cellspacing=0 cellpadding=0><tr><td style="color: white;" align=center bgcolor=blue><b>Results:</b></td></tr><tr><td style="padding: 5px; border: 1px solid blue;"><center><table><tr><td><center><img src="http://images.kingdomofloathing.com/itemimages/ratchet.gif" width=30 height=30><br></center><blockquote>TEXT</blockquote></td></tr></table>

	private static void decorateBooClue( final StringBuffer buffer )
	{
		if ( buffer.indexOf( "A-Boo Peak" ) == -1 )
		{
			return;
		}

		String search = "</blockquote></td></tr>";
		int index = buffer.indexOf( search );

		if ( index == -1 )
		{
			return;
		}

		// We will insert things before the end of the table
		index += search.length();

		// Add the link to adventure in A-Boo Peak
		StringBuilder link = new StringBuilder();
		link.append( "<tr align=center><td>" );
		link.append( "<a href=\"adventure.php?snarfblat=296\">" );
		link.append( "[Adventure at A-Boo Peak]" );
		link.append( "</a></td></tr>" );

		buffer.insert( index, link.toString() );
	}

	private static void decorateMarketMap( final StringBuffer buffer )
	{
		if ( buffer.indexOf( "The Black Market" ) == -1 )
		{
			return;
		}

		String search = "</blockquote></td></tr>";
		int index = buffer.indexOf( search );

		if ( index == -1 )
		{
			return;
		}

		// We will insert things before the end of the table
		index += search.length();

		// Add the link to visit The Black Market
		StringBuilder link = new StringBuilder();
		link.append( "<tr align=center><td>" );
		link.append( "<a href=\"shop.php?whichshop=blackmarket\">" );
		link.append( "[Shop at The Black Market]" );
		link.append( "</a></td></tr>" );

		buffer.insert( index, link.toString() );
	}
}
