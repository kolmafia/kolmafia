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
import java.util.StringTokenizer;

/**
 * A special request used specifically to search the mall for items and retrieve the
 * stores which sell them, the prices at which the items are being sold, and the
 * number of items which are currently available for purchase.  All values are
 * subsequently added directly to the provided list.
 */

public class SearchMallRequest extends KoLRequest
{
	private List results;

	/**
	 * Constructs a new <code>SearchMallRequest</code> which searches for
	 * the given item, storing the results in the given <code>ListModel</code>.
	 * Note that the search string is exactly the same as the way KoL does
	 * it at the current time - exact matches only, wild cards permissible.
	 *
	 * @param	client	The client to be notified in case of error
	 * @param	searchString	The string (including wildcards) for the item to be found
	 * @param	cheapestCount	The number of stores to show; use a non-positive number to show all
	 * @param	results	The sorted list in which to store the results
	 */

	public SearchMallRequest( KoLmafia client, String searchString, int cheapestCount, List results )
	{
		super( client, "searchmall.php" );
		addFormField( "whichitem", searchString );

		if ( cheapestCount > 0 )
		{
			addFormField( "cheaponly", "on" );
			addFormField( "shownum", "" + cheapestCount );
		}

		this.results = results;
	}

	/**
	 * Executes the search request.  In the event that no item is found, the
	 * currently active frame will be notified.  Otherwise, all items
	 * are stored inside of the results list.  Note also that the results
	 * will be cleared before being stored.
	 */

	public void run()
	{
		super.run();
		results.clear();

		// If an error state occurred, return from this
		// request, since there's no content to parse

		if ( isErrorState || responseCode != 200 )
			return;

		// If the request failed to find any items,
		// then return from this method; there's no
		// parsing to be done.

		int startIndex = replyContent.indexOf( "<p><center>" );
		if ( startIndex == -1 )
		{
			frame.updateDisplay( KoLFrame.ENABLED_STATE, "No results found." );
			return;
		}

		// Change all multi-line store names into single line store names so that the
		// parser doesn't get confused; remove all stores where limits have already
		// been reached (which have been greyed out), and then remove all non-anchor
		// tags to make everything easy to parse.

		String plainTextResult = replyContent.substring( startIndex ).replaceAll( "<br>", " " ).replaceAll(
				"<td style=.*?<tr>", "" ).replaceAll( "</?[^a].*?>", "\n" );
		StringTokenizer parsedResults = new StringTokenizer( plainTextResult, "\n" );

		// The first four tokens are just the table
		// headers, and so they can be discarded

		skipTokens( parsedResults, 4 );

		// Now, check to see if there was actually
		// no results in a limited search

		if ( parsedResults.countTokens() == 1 )
		{
			frame.updateDisplay( KoLFrame.ENABLED_STATE, "No results found." );
			return;
		}

		while ( parsedResults.hasMoreTokens() )
		{
			// The first token contains the item name

			String itemName = parsedResults.nextToken().trim();

			// The next token contains the number of items being sold
			// in addition to any limits imposed on those items

			StringTokenizer buyDetails = new StringTokenizer( parsedResults.nextToken(), " &nbsp;()/day" );
			int total = intToken( buyDetails );
			int limit = buyDetails.hasMoreTokens() ? intToken( buyDetails ) : 0;
			int purchaseLimit = (limit == 0 || total < limit) ? total : limit;

			// The next token contains data which identifies the shop

			String shopDetails = parsedResults.nextToken();
			int borderIndex = shopDetails.indexOf( "\">" );
			int shopID = Integer.parseInt( shopDetails.substring( 35, borderIndex ) );
			String shopName = shopDetails.substring( borderIndex + 2 );

			// The last token contains the price of the item

			int price = intToken( parsedResults, 0, 10 );
			results.add( new MallPurchaseRequest( client, itemName, purchaseLimit, shopID, shopName, price ) );
		}

		results.add( "" );
		frame.updateDisplay( KoLFrame.ENABLED_STATE, "Search complete." );
	}
}