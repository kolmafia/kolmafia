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
import java.util.StringTokenizer;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

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
	 *
	 * @param	client	The client to be notified in case of error
	 */

	public SearchMallRequest( KoLmafia client, int storeID )
	{
		super( client, "mallstore.php" );
		addFormField( "whichstore", String.valueOf( storeID ) );

		this.results = new ArrayList();
		this.retainAll = true;
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
		super( client, searchString == null || searchString.trim().length() == 0 ? "mall.php" : "searchmall.php" );

		this.searchString = getItemName( searchString );
		addFormField( "whichitem", this.searchString );

		if ( cheapestCount < 0 || cheapestCount > 37 )
			cheapestCount = 37;

		if ( cheapestCount != 0 )
		{
			addFormField( "cheaponly", "on" );
			addFormField( "shownum", "" + cheapestCount );
		}

		this.results = results;
		this.retainAll = retainAll;
	}
	
	private String getItemName( String searchString )
	{
		String itemName = searchString;
	
		if ( itemName.startsWith( "\"" ) || itemName.startsWith( "\'" ) )
		{
		}

		// For items with the n-tilde character, a
		// perfect match is available if you use the
		// substring consisting of everything after
		// the ntilde;

		else if ( itemName.indexOf( "\u00f1" ) != -1 )
			itemName = itemName.substring( itemName.indexOf( "\u00f1" ) + 1 );

		// For items with the trademark character, a
		// perfect match is available if you use the
		// substring consisting of everything before
		// the trademark character

		else if ( itemName.indexOf( "\u2122" ) != -1 )
			itemName = itemName.substring( 0, itemName.indexOf( "\u2122" ) );

		else if ( itemName.indexOf( "\u00e9" ) != -1 )
			itemName = itemName.substring( 0, itemName.indexOf( "\u00e9" ) );

		// All items with double quotes can be matched
		// by searching on everything before the double

		else if ( itemName.indexOf( "\"" ) != -1 )
			itemName = itemName.substring( 0, itemName.indexOf( "\"" ) );

		else if ( TradeableItemDatabase.contains( searchString ) )
			itemName = "\"" + itemName + "\"";

		// In all other cases, an exact match is only
		// available if you enclose the item name in
		// double quotes.

		return itemName;
	}

	public List getResults()
	{	return results;
	}

	/**
	 * Executes the search request.  In the event that no item is found, the
	 * currently active frame will be notified.  Otherwise, all items
	 * are stored inside of the results list.  Note also that the results
	 * will be cleared before being stored.
	 */

	public void run()
	{
		// Check to see if the client is able to actually
		// use the mall -- some people are hardcore or are
		// somewhere in ronin.

		if ( searchString == null || searchString.trim().length() == 0 )
		{
			DEFAULT_SHELL.updateDisplay( retainAll ? "Scanning store inventories..." : "Looking up favorite stores list..." );
		}
		else
		{
			results.clear();
			List itemNames = TradeableItemDatabase.getMatchingNames( searchString );

			// In the event that it's all NPC stores, and the person
			// cannot use the mall, then only display the items which
			// are available from NPC stores, since that's all that
			// can be used in this circumstance.

			boolean npcStoreExists = true;
			for ( int i = 0; i < itemNames.size(); ++i )
				npcStoreExists &= NPCStoreDatabase.contains( (String) itemNames.get(i) );

			if ( KoLCharacter.getLevel() < 5 || (!KoLCharacter.canInteract() && npcStoreExists) )
			{
				finalizeList( itemNames );
				return;
			}

			DEFAULT_SHELL.updateDisplay( "Searching for items..." );
		}

		// Otherwise, conduct the normal mall search, processing
		// the NPC results as needed.

		super.run();

	}

	private void searchStore()
	{
		if ( retainAll )
		{
			Matcher shopMatcher = Pattern.compile( "<b>(.*?) \\(<a.*?who=(\\d+)\"" ).matcher( responseText );
			shopMatcher.find();

			int shopID = Integer.parseInt( shopMatcher.group(2) );

			// Translate the shop name to its unicode form so
			// it can be properly rendered.  In the process,
			// also handle character entities mangled by KoL.

			String shopName = RequestEditorKit.getUnicode( shopMatcher.group(1).replaceAll( "[ ]+;", ";" ) );

			Pattern limitPattern = Pattern.compile( "Limit ([\\d,]+) /" );

			int lastFindIndex = 0;
			Matcher priceMatcher = Pattern.compile( "radio value=(\\d+).*?<b>(.*?)</b> \\(([\\d,]+)\\)(.*?)</td>" ).matcher( responseText );

			while ( priceMatcher.find( lastFindIndex ) )
			{
				lastFindIndex = priceMatcher.end();
				String priceID = priceMatcher.group(1);

				try
				{
					String itemName = priceMatcher.group(2);

					int itemID = Integer.parseInt( priceID.substring( 0, priceID.length() - 9 ) );
					int quantity = df.parse( priceMatcher.group(3) ).intValue();
					int limit = quantity;

					Matcher limitMatcher = limitPattern.matcher( priceMatcher.group(4) );
					if ( limitMatcher.find() )
						limit = df.parse( limitMatcher.group(1) ).intValue();

					int price = Integer.parseInt( priceID.substring( priceID.length() - 9 ) );
					results.add( new MallPurchaseRequest( client, itemName, itemID, quantity, shopID, shopName, price, limit, true ) );
				}
				catch ( Exception e )
				{
					// This should not happen.  Therefore, print
					// a stack trace for debug purposes.
					
					StaticEntity.printStackTrace( e );
					return;
				}
			}
		}
		else
		{
			SearchMallRequest individualStore;
			Matcher storeMatcher = Pattern.compile( "&action=unfave&whichstore=(\\d+)\">" ).matcher( responseText );

			int lastFindIndex = 0;
			while ( storeMatcher.find( lastFindIndex ) )
			{
				lastFindIndex = storeMatcher.end();
				individualStore = new SearchMallRequest( client, Integer.parseInt( storeMatcher.group(1) ) );
				individualStore.run();

				results.addAll( individualStore.results );
			}

			DEFAULT_SHELL.updateDisplay( "Search complete." );
		}
	}

	private void searchMall()
	{
		List itemNames = TradeableItemDatabase.getMatchingNames( searchString );

		// Change all multi-line store names into single line store names so that the
		// parser doesn't get confused; remove all stores where limits have already
		// been reached (which have been greyed out), and then remove all non-anchor
		// tags to make everything easy to parse.

		int startIndex = responseText.indexOf( "Search Results:" );
		String storeListResult = responseText.substring( startIndex == -1 ? 0 : startIndex );
		String plainTextResult = storeListResult.replaceAll( "<br>", " " ).replaceAll( "</?b>", "\n" ).replaceAll(
			"</?p>", "" ).replaceAll( "</c.*?>", "" ).replaceAll( "<tr><td style", "\n...\n<tr><td style" ).replaceAll( "</?t.*?>", "\n" ).replaceAll( "</a>", "\n" );

		StringTokenizer parsedResults = new StringTokenizer( plainTextResult, "\n" );

		// Now, check to see if there was actually
		// no results in a limited search

		if ( startIndex == -1 || parsedResults.countTokens() < 9 )
		{
			finalizeList( itemNames );
			return;
		}

		// The first four tokens are just the table
		// headers, and so they can be discarded

		skipTokens( parsedResults, 4 );

		String lastItemName = "";
		while ( parsedResults.countTokens() > 1 )
		{
			boolean canPurchase = true;

			// The first token contains the item name

			String itemName = parsedResults.nextToken().trim();

			if ( itemName.equals( "..." ) )
			{
				canPurchase = false;
				itemName = parsedResults.nextToken().trim();
			}

			if ( !itemName.equals( lastItemName ) )
			{
				// Theoretically, you could do a check to see if you
				// should add in the NPC store price; however, if
				// it wasn't in the top list, then don't bother.
				// Now, figure out if an NPC item exists for the
				// most recently encountered item type.

				lastItemName = itemName;
				itemNames.remove( itemName );

				if ( NPCStoreDatabase.contains( itemName ) )
					results.add( NPCStoreDatabase.getPurchaseRequest( itemName ) );
			}

			// The next token contains the number of items being sold
			// in addition to any limits imposed on those items

			StringTokenizer buyDetails = new StringTokenizer( parsedResults.nextToken(), " &nbsp;()/day" );
			int quantity = intToken( buyDetails );
			int limit = buyDetails.hasMoreTokens() ? intToken( buyDetails ) : quantity;

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

			results.add( new MallPurchaseRequest( client, itemName, itemID, quantity, shopID, shopName, price, limit, canPurchase ) );
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

		String [] names = new String[ itemNames.size() ];
		itemNames.toArray( names );

		for ( int i = 0; i < names.length; ++i )
			if ( NPCStoreDatabase.contains( names[i] ) )
				results.add( NPCStoreDatabase.getPurchaseRequest( names[i] ) );
	}

	protected void processResults()
	{
		if ( searchString == null || searchString.trim().length() == 0 )
			searchStore();
		else
			searchMall();
	}
}
