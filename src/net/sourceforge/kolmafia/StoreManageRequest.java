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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StoreManageRequest extends KoLRequest
{
	private int takenItemID;
	private boolean isPriceManagement;

	public StoreManageRequest( KoLmafia client )
	{
		super( client, "manageprices.php" );
		this.isPriceManagement = true;
	}

	public StoreManageRequest( KoLmafia client, int itemID )
	{
		super( client, "managestore.php" );
		addFormField( "action", "takeall" );
		addFormField( "whichitem", "" + itemID );
		this.isPriceManagement = false;
		this.takenItemID = itemID;
	}

	public StoreManageRequest( KoLmafia client, int [] itemID, int [] prices, int [] limits )
	{
		super( client, "manageprices.php" );
		addFormField( "action", "update" );
		addFormField( "pwd", client.getPasswordHash() );
		this.isPriceManagement = true;

		for ( int i = 0; i < itemID.length; ++i )
		{
			addFormField( "price" + itemID[i], "" + prices[i] );
			addFormField( "limit" + itemID[i], "" + limits[i] );
		}
	}

	public void run()
	{
		updateDisplay( DISABLED_STATE, "Requesting store inventory..." );

		super.run();
		client.getStoreManager().clear();

		// Use a fairly ugly regular expression in order to determine each price
		// listed inside of the store manager.  This will be used to update the
		// store manager information.

		int lastFindIndex = 0;

		if ( isPriceManagement )
		{
			Matcher priceMatcher = Pattern.compile(
				"<tr>.*?value=\"(\\d+)\" name=price(\\d+)>.*?value=\"(\\d+)\".*?</tr>" ).matcher( replyContent );

			while ( priceMatcher.find( lastFindIndex ) )
			{
				lastFindIndex = priceMatcher.end();

				int price = Integer.parseInt( priceMatcher.group(1) );
				int itemID = Integer.parseInt( priceMatcher.group(2) );
				int limit = Integer.parseInt( priceMatcher.group(3) );

				client.getStoreManager().registerItem( itemID, price, limit );
			}
		}
		else
		{
			Matcher takenItemMatcher = Pattern.compile(
				"<option value=" + takenItemID + ">(.*?)</option>" ).matcher( replyContent );
			takenItemMatcher.find();
			client.parseResult( takenItemMatcher.group(1).replaceAll( "\\(.*? Meat\\) ", "" ) );

			Matcher itemMatcher = Pattern.compile(
				"<tr><td>.*?</td><td>.*?</td><td>([\\d,]+)</td><td>(.*?)</td><td><a href=\"managestore.php\\?action=take&whichitem=(\\d+)\".*?</tr>" ).matcher( replyContent );

			try
			{
				while ( itemMatcher.find( lastFindIndex ) )
				{
					lastFindIndex = itemMatcher.end();
					int price = df.parse( itemMatcher.group(1) ).intValue();
					int limit = itemMatcher.group(2).startsWith( "<" ) ? 0 :
						Integer.parseInt( itemMatcher.group(2) );

					int itemID = Integer.parseInt( itemMatcher.group(3) );
					client.getStoreManager().registerItem( itemID, price, limit );
				}
			}
			catch ( Exception e )
			{
			}
		}

		updateDisplay( ENABLED_STATE, "Store inventory request complete." );
	}
}