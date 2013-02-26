/**
 * Copyright (c) 2005-2013, KoLmafia development team
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
		case ItemPool.TOMB_RATCHET:
			UseItemDecorator.decorateTombRatchet( buffer );
			break;
		}
	}

	// <table  width=95%  cellspacing=0 cellpadding=0><tr><td style="color: white;" align=center bgcolor=blue><b>Results:</b></td></tr><tr><td style="padding: 5px; border: 1px solid blue;"><center><table><tr><td><center><img src="http://images.kingdomofloathing.com/itemimages/ratchet.gif" width=30 height=30><br></center><blockquote>TEXT</blockquote></td></tr></table>

	private static final AdventureResult TOMB_RATCHET = ItemPool.get( ItemPool.TOMB_RATCHET, 1);

	private static void decorateTombRatchet( final StringBuffer buffer )
	{
		// You head down to the middle chamber of the buried pyramid
		// and affix the ratchet to the mechanism on the wall.	You
		// give it a mighty heave, and as it turns, the chamber rumbles
		// as though some great weight was shifting beneath you.  Then
		// the ratchet crumbles to powder.  Stupid cheap ratchet, only
		// lasting thousands of years.

		if ( buffer.indexOf( "Stupid cheap ratchet" ) == -1 )
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

		// Show the current pyramid position
		String pyramid = PyramidRequest.getPyramidHTML();
		buffer.insert( index, pyramid );

		// Give player a link to use another tomb ratchet
		if ( UseItemDecorator.TOMB_RATCHET.getCount( KoLConstants.inventory ) > 0 )
		{
			StringBuffer link = new StringBuffer();
			link.append( "<tr align=center><td>" );
			link.append( "<a href=\"javascript:singleUse('inv_use.php','which=3&whichitem=" );
			link.append( String.valueOf( ItemPool.TOMB_RATCHET ) );
			link.append( "&pwd=" );
			link.append( GenericRequest.passwordHash );
			link.append( "&ajax=1');void(0);\">Use another tomb ratchet</a>" );
			link.append( "</td></tr>" );

			buffer.insert( index, link.toString() );
		}
	}
}
