/**
 * Copyright (c) 2005-2011, KoLmafia development team
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

import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class AfterLifeRequest
	extends GenericRequest
{
	// <tr><td><img style='vertical-align: middle' class=hand src='http://images.kingdomofloathing.com/itemimages/ast_bludgeon.gif' onclick='descitem(864672857)'></td><td valign=center><b><span onclick='descitem(864672857)'>astral bludgeon<span>&nbsp;&nbsp;&nbsp;&nbsp;</b></td><form action=afterlife.php method=post><input type=hidden name=action value=buyarmory><input type=hidden name=whichitem value=5028><td><input class=button type=submit value="Purchase (10 Karma)"></td></form></tr>

	// <tr><td><img style='vertical-align: middle' class=hand src='http://images.kingdomofloathing.com/itemimages/ast_dinner.gif' onclick='descitem(725022566)'></td><td valign=center><b><span onclick='descitem(725022566)'>astral hot dog dinner<span>&nbsp;&nbsp;&nbsp;&nbsp;</b></td><form action=afterlife.php method=post><input type=hidden name=action value=buydeli><input type=hidden name=whichitem value=5045><td><input class=button type=submit value="Purchase (1 Karma)"></td></form></tr>

        private static final Pattern ITEM_PATTERN = Pattern.compile( "<span onclick='descitem\\(([\\d]+)\\)'>([^<]*)<.*?name=whichitem value=([\\d]+)>", Pattern.DOTALL );

	private AfterLifeRequest()
	{
		super( "afterlife.php" );
	}

	public void processResults()
	{
		AfterLifeRequest.parseResponse( this.getURLString(), this.responseText );
	}

	public static void parseResponse( final String urlString, final String responseText )
	{
		if ( !urlString.startsWith( "afterlife.php" ) )
		{
			return;
		}

		// Learn new astral items simply by visiting an astral vendor
		Matcher matcher = ITEM_PATTERN.matcher( responseText );
		while ( matcher.find() )
		{
			String descId = matcher.group(1);
			String itemName = matcher.group(2);
			int itemId = StringUtilities.parseInt( matcher.group(3) );

			String data = ItemDatabase.getItemDataName( itemId );
			if ( data == null || !data.equals( itemName ) )
			{
				ItemDatabase.registerItem( itemId, itemName, descId );
			}
		}
	}

	public static boolean registerRequest( final String urlString )
	{
		if ( !urlString.startsWith( "afterlife.php" ) )
		{
			return false;
		}

		return false;
	}
}
