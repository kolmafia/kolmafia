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
import java.util.Iterator;
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
	private boolean retainAll;
	private String searchString;

	/**
	 * Constructs a new <code>SearchMallRequest</code> which searches for
	 * the given item, storing the results in the given <code>ListModel</code>.
	 * Note that the search string is exactly the same as the way KoL does
	 * it at the current time.
	 *
	 * @param	client	The client to be notified in case of error
	 * @param	searchString	The string (including wildcards) for the item to be found
	 * @param	results	The sorted list in which to store the results
	 */

	public SearchMallRequest( KoLmafia client, String searchString, List results )
	{	this( client, searchString, results, false );
	}

	/**
	 * Constructs a new <code>SearchMallRequest</code> which searches for
	 * the given item, storing the results in the given <code>ListModel</code>.
	 * Note that the search string is exactly the same as the way KoL does
	 * it at the current time.
	 *
	 * @param	client	The client to be notified in case of error
	 * @param	searchString	The string (including wildcards) for the item to be found
	 * @param	results	The sorted list in which to store the results
	 */

	public SearchMallRequest( KoLmafia client, String searchString, List results, boolean retainAll )
	{
		this( client, searchString,
			client.getSettings().getProperty( "defaultLimit" ) == null ? 13 :
				Integer.parseInt( client.getSettings().getProperty( "defaultLimit" ) ), results, retainAll );
	}

	/**
	 * Constructs a new <code>SearchMallRequest</code> which searches for
	 * the given item, storing the results in the given <code>ListModel</code>.
	 * Note that the search string is exactly the same as the way KoL does
	 * it at the current time.
	 *
	 * @param	client	The client to be notified in case of error
	 * @param	searchString	The string (including wildcards) for the item to be found
	 * @param	cheapestCount	The number of stores to show; use a non-positive number to show all
	 * @param	results	The sorted list in which to store the results
	 */

	public SearchMallRequest( KoLmafia client, String searchString, int cheapestCount, List results )
	{	this( client, searchString, cheapestCount, results, false );
	}

	/**
	 * Constructs a new <code>SearchMallRequest</code> which searches for
	 * the given item, storing the results in the given <code>ListModel</code>.
	 * Note that the search string is exactly the same as the way KoL does
	 * it at the current time.
	 *
	 * @param	client	The client to be notified in case of error
	 * @param	searchString	The string (including wildcards) for the item to be found
	 * @param	cheapestCount	The number of stores to show; use a non-positive number to show all
	 * @param	results	The sorted list in which to store the results
	 */

	public SearchMallRequest( KoLmafia client, String searchString, int cheapestCount, List results, boolean retainAll )
	{
		super( client, "searchmall.php" );

		String searchRequest = searchString.indexOf( "ñ" ) != -1 || searchString.indexOf( "©" ) != -1 ?
			searchString.replaceAll( "[\"\']", "" ).replaceAll( "ñ", "" ).replaceAll( "©", "" ) : searchString;

		addFormField( "whichitem", searchRequest );

		if ( cheapestCount > 0 )
		{
			addFormField( "cheaponly", "on" );
			addFormField( "shownum", "" + cheapestCount );
		}

		this.searchString = searchString;
		this.results = results;
		this.retainAll = retainAll;
	}

	/**
	 * Executes the search request.  In the event that no item is found, the
	 * currently active frame will be notified.  Otherwise, all items
	 * are stored inside of the results list.  Note also that the results
	 * will be cleared before being stored.
	 */

	public void run()
	{
		if ( searchString == null || searchString.trim().length() == 0 )
			return;

		updateDisplay( DISABLED_STATE, "Searching for items..." );

		super.run();
		results.clear();

		// If an error state occurred, return from this
		// request, since there's no content to parse

		if ( isErrorState || responseCode != 200 )
			return;

		int startIndex = responseText.indexOf( "Search Results:" );
		List itemNames = TradeableItemDatabase.getMatchingNames( searchString );

		if ( startIndex == -1 )
		{
			finalizeList( itemNames );
			return;
		}

		// Change all multi-line store names into single line store names so that the
		// parser doesn't get confused; remove all stores where limits have already
		// been reached (which have been greyed out), and then remove all non-anchor
		// tags to make everything easy to parse.

		String storeListResult = responseText.substring( startIndex );
		if ( !retainAll )  storeListResult = storeListResult.replaceAll( "<td style=.*?<tr>", "" );

		String plainTextResult = storeListResult.replaceAll( "<br>", "" ).replaceAll(
			"</?b>", "\n" ).replaceAll( "</?p>", "" ).replaceAll( "</c.*?>", "" ).replaceAll( "</?t.*?>", "\n" ).replaceAll(
				"</a>", "\n" );

		StringTokenizer parsedResults = new StringTokenizer( plainTextResult, "\n" );

		// Now, check to see if there was actually
		// no results in a limited search

		if ( parsedResults.countTokens() < 9 )
		{
			finalizeList( itemNames );
			return;
		}

		// The first four tokens are just the table
		// headers, and so they can be discarded

		skipTokens( parsedResults, 4 );

		String lastItemName = "";
		boolean npcStoreAdded = true;
		int npcStorePrice = -1;

		while ( parsedResults.countTokens() > 1 )
		{
			// The first token contains the item name

			String itemName = parsedResults.nextToken().trim();

			if ( !itemName.equals( lastItemName ) )
			{
				// Theoretically, you could do a check to see if you
				// should add in the NPC store price; however, if
				// it wasn't in the top list, then don't bother.
				// Now, figure out if an NPC item exists for the
				// most recently encountered item type.

				lastItemName = itemName;
				npcStoreAdded = !NPCStoreDatabase.contains( itemName );
				if ( !npcStoreAdded )
				{
					itemNames.remove( itemName );
					npcStorePrice = NPCStoreDatabase.getNPCStorePrice( itemName );
				}
			}

			// The next token contains the number of items being sold
			// in addition to any limits imposed on those items

			StringTokenizer buyDetails = new StringTokenizer( parsedResults.nextToken(), " &nbsp;()/day" );
			int total = intToken( buyDetails );
			int limit = buyDetails.hasMoreTokens() ? intToken( buyDetails ) : 0;
			int purchaseLimit = (limit == 0 || total < limit) ? total : limit;

			// The next token contains data which identifies the shop
			// and the item (which will be used later), and the price!
			// which means you don't need to consult thenext token.

			String shopDetails = parsedResults.nextToken();
			int shopID = Integer.parseInt( shopDetails.substring( shopDetails.indexOf( "store=" ) + 6, shopDetails.indexOf( "&searchitem" ) ) );
			int itemID = Integer.parseInt( shopDetails.substring( shopDetails.indexOf( "item=" ) + 5, shopDetails.indexOf( "&searchprice" ) ) );
			String shopName = shopDetails.substring( shopDetails.indexOf( "\">" ) + 2 );
			int price = Integer.parseInt( shopDetails.substring( shopDetails.indexOf( "price=" ) + 6, shopDetails.indexOf( "\">" ) ) );

			// The last token contains the price of the item, but
			// you need to discard it.

			parsedResults.nextToken();

			// Now, check to see if you should add the NPC
			// store at the current time

			if ( !npcStoreAdded && npcStorePrice < price )
			{
				npcStoreAdded = true;
				results.add( NPCStoreDatabase.getPurchaseRequest( client, itemName ) );
			}

			results.add( new MallPurchaseRequest( client, itemName, itemID, purchaseLimit, shopID, shopName, price ) );
		}

		// Once the search is complete, add in any remaining NPC
		// store data and finalize the list.

		finalizeList( itemNames );
	}

	private void finalizeList( List itemNames )
	{
		// Now, for the items which matched, check to see if there are
		// any entries inside of the NPC store database for them and
		// add - this is just in case some of the items become notrade
		// so items can still be bought from the NPC stores.

		String lastItemName;

		Iterator itemNameIterator = itemNames.iterator();
		while ( itemNameIterator.hasNext() )
		{
			lastItemName = (String) itemNameIterator.next();
			if ( NPCStoreDatabase.contains( lastItemName ) )
				results.add( NPCStoreDatabase.getPurchaseRequest( client, lastItemName ) );
		}

		String forceSortingString = client.getSettings().getProperty( "forceSorting" );
		if ( forceSortingString != null && forceSortingString.equals( "true" ) )
			java.util.Collections.sort( results );

		updateDisplay( ENABLED_STATE, results.size() == 0 ? "No results found." : "Search complete." );
	}
}