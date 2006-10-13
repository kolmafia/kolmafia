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
import java.util.Collections;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import net.java.dev.spellcast.utilities.LockableListModel;

/**
 * A special request used specifically to search the mall for items and retrieve the
 * stores which sell them, the prices at which the items are being sold, and the
 * number of items which are currently available for purchase.  All values are
 * subsequently added directly to the provided list.
 */

public class SearchMallRequest extends KoLRequest
{
	private static final Pattern FAVORITES_PATTERN = Pattern.compile( "&action=unfave&whichstore=(\\d+)\">" );
	private static final Pattern STOREID_PATTERN = Pattern.compile( "<b>(.*?) \\(<a.*?who=(\\d+)\"" );
	private static final Pattern STORELIMIT_PATTERN = Pattern.compile( "Limit ([\\d,]+) /" );
	private static final Pattern STOREPRICE_PATTERN = Pattern.compile( "radio value=(\\d+).*?<b>(.*?)</b> \\(([\\d,]+)\\)(.*?)</td>" );
	private static final Pattern STOREDETAIL_PATTERN = Pattern.compile( "<tr>.*?</a>" );

	private static final Pattern LISTQUANTITY_PATTERN = Pattern.compile( "\\([\\d,]+\\)" );
	private static final Pattern LISTLIMIT_PATTERN = Pattern.compile( "([\\d,]+)\\&nbsp;\\/\\&nbsp;day" );
	private static final Pattern LISTDETAIL_PATTERN = Pattern.compile( "whichstore=(\\d+)\\&searchitem=(\\d+)\\&searchprice=(\\d+)\">(.*?)</a>" );

	private List results;
	private boolean retainAll;
	private boolean sortAfter;
	private String searchString;

	/**
	 * Constructs a new <code>SearchMallRequest</code> which searches for
	 * the given item, storing the results in the given <code>ListModel</code>.
	 *
	 * @param	client	Theto be notified in case of error
	 */

	public SearchMallRequest( int storeID )
	{
		super( "mallstore.php" );
		addFormField( "whichstore", String.valueOf( storeID ) );

		this.results = new ArrayList();
		this.retainAll = true;
		this.sortAfter = false;
	}

	/**
	 * Constructs a new <code>SearchMallRequest</code> which searches for
	 * the given item, storing the results in the given <code>ListModel</code>.
	 * Note that the search string is exactly the same as the way KoL does
	 * it at the current time.
	 *
	 * @param	client	Theto be notified in case of error
	 * @param	searchString	The string (including wildcards) for the item to be found
	 * @param	cheapestCount	The number of stores to show; use a non-positive number to show all
	 * @param	results	The sorted list in which to store the results
	 */

	public SearchMallRequest( String searchString, int cheapestCount, List results )
	{	this( searchString, cheapestCount, results, false, true );
	}

	/**
	 * Constructs a new <code>SearchMallRequest</code> which searches for
	 * the given item, storing the results in the given <code>ListModel</code>.
	 * Note that the search string is exactly the same as the way KoL does
	 * it at the current time.
	 *
	 * @param	client	Theto be notified in case of error
	 * @param	searchString	The string (including wildcards) for the item to be found
	 * @param	cheapestCount	The number of stores to show; use a non-positive number to show all
	 * @param	results	The sorted list in which to store the results
	 */

	public SearchMallRequest( String searchString, int cheapestCount, List results, boolean retainAll )
	{	this( searchString, cheapestCount, results, retainAll, false );
	}

	/**
	 * Constructs a new <code>SearchMallRequest</code> which searches for
	 * the given item, storing the results in the given <code>ListModel</code>.
	 * Note that the search string is exactly the same as the way KoL does
	 * it at the current time.
	 *
	 * @param	client	Theto be notified in case of error
	 * @param	searchString	The string (including wildcards) for the item to be found
	 * @param	cheapestCount	The number of stores to show; use a non-positive number to show all
	 * @param	results	The sorted list in which to store the results
	 * @param	sortAfter	Whether the results should be resorted by price afterwards
	 */

	public SearchMallRequest( String searchString, int cheapestCount, List results, boolean retainAll, boolean sortAfter )
	{
		super( searchString == null || searchString.trim().length() == 0 ? "mall.php" : "searchmall.php" );

		this.searchString = searchString;
		addFormField( "whichitem", this.searchString );

		if ( cheapestCount > 0 )
		{
			addFormField( "cheaponly", "on" );
			addFormField( "shownum", "" + cheapestCount );
		}

		this.results = results;
		this.retainAll = retainAll;
		this.sortAfter = sortAfter;
	}

	public static String getItemName( String searchString )
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

		// All items with float quotes can be matched
		// by searching on everything before the float

		else if ( itemName.indexOf( "\"" ) > 0 )
			itemName = itemName.substring( 0, itemName.indexOf( "\"" ) );

		else if ( TradeableItemDatabase.contains( searchString ) )
			itemName = "\"" + itemName + "\"";

		// In all other cases, an exact match is only
		// available if you enclose the item name in
		// float quotes.

		return itemName;
	}

	public List getResults()
	{	return results;
	}

	public void setResults( List results )
	{	this.results = results;
	}

	/**
	 * Executes the search request.  In the event that no item is found, the
	 * currently active frame will be notified.  Otherwise, all items
	 * are stored inside of the results list.  Note also that the results
	 * will be cleared before being stored.
	 */

	public void run()
	{
		// Check to see if theis able to actually
		// use the mall -- some people are hardcore or are
		// somewhere in ronin.

		if ( searchString == null || searchString.trim().length() == 0 )
		{
			KoLmafia.updateDisplay( retainAll ? "Scanning store inventories..." : "Looking up favorite stores list..." );
		}
		else
		{
			results.clear();
			List itemNames = TradeableItemDatabase.getMatchingNames( searchString );

			// In the event that it's all NPC stores, and the person
			// cannot use the mall, then only display the items which
			// are available from NPC stores, since that's all that
			// can be used in this circumstance.

			boolean canAvoidSearch = true;
			for ( int i = 0; i < itemNames.size(); ++i )
			{
				int autoSellPrice = TradeableItemDatabase.getPriceByID( TradeableItemDatabase.getItemID( (String) itemNames.get(i) ) );
				canAvoidSearch &= NPCStoreDatabase.contains( (String) itemNames.get(i) ) ?
					(!KoLCharacter.canInteract() || (autoSellPrice > 0 && autoSellPrice < 100)) : (autoSellPrice == 0 || autoSellPrice < -1);
			}

			if ( canAvoidSearch )
			{
				finalizeList( itemNames );
				return;
			}

			if ( itemNames.size() == 1 )
			{
				searchString = "\"" + itemNames.get(0) + "\"";
				addFormField( "whichitem", this.searchString );
			}

			KoLmafia.updateDisplay( "Searching for items..." );
		}

		// Otherwise, conduct the normal mall search, processing
		// the NPC results as needed.

		super.run();
	}

	private void searchStore()
	{
		if ( retainAll )
		{
			Matcher shopMatcher = STOREID_PATTERN.matcher( responseText );
			shopMatcher.find();

			int shopID = StaticEntity.parseInt( shopMatcher.group(2) );

			// Translate the shop name to its unicode form so
			// it can be properly rendered.  In the process,
			// also handle character entities mangled by KoL.

			String shopName = RequestEditorKit.getUnicode( shopMatcher.group(1).replaceAll( "[ ]+;", ";" ) );

			int lastFindIndex = 0;
			Matcher priceMatcher = STOREPRICE_PATTERN.matcher( responseText );

			while ( priceMatcher.find( lastFindIndex ) )
			{
				lastFindIndex = priceMatcher.end();
				String priceID = priceMatcher.group(1);

				String itemName = priceMatcher.group(2);

				int itemID = StaticEntity.parseInt( priceID.substring( 0, priceID.length() - 9 ) );
				int quantity = StaticEntity.parseInt( priceMatcher.group(3) );
				int limit = quantity;

				Matcher limitMatcher = STORELIMIT_PATTERN.matcher( priceMatcher.group(4) );
				if ( limitMatcher.find() )
					limit = StaticEntity.parseInt( limitMatcher.group(1) );

				int price = StaticEntity.parseInt( priceID.substring( priceID.length() - 9 ) );
				results.add( new MallPurchaseRequest( itemName, itemID, quantity, shopID, shopName, price, limit, true ) );
			}
		}
		else
		{
			SearchMallRequest individualStore;
			Matcher storeMatcher = FAVORITES_PATTERN.matcher( responseText );

			int lastFindIndex = 0;
			while ( storeMatcher.find( lastFindIndex ) )
			{
				lastFindIndex = storeMatcher.end();
				individualStore = new SearchMallRequest( StaticEntity.parseInt( storeMatcher.group(1) ) );
				individualStore.run();

				results.addAll( individualStore.results );
			}

			KoLmafia.updateDisplay( "Search complete." );
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
		String storeListResult = responseText.substring( startIndex < 0 ? 0 : startIndex );

		Matcher linkMatcher = STOREDETAIL_PATTERN.matcher( storeListResult );
		String linkText = null;

		int previousItemID = -1;

		while ( linkMatcher.find() )
		{
			linkText = linkMatcher.group();
			Matcher quantityMatcher = LISTQUANTITY_PATTERN.matcher( linkText );
			int quantity = 0;

			if ( quantityMatcher.find() )
				quantity =	StaticEntity.parseInt( quantityMatcher.group() );

			int limit = quantity;

			Matcher limitMatcher = LISTLIMIT_PATTERN.matcher( linkText );
			if ( limitMatcher.find() )
				limit = StaticEntity.parseInt( limitMatcher.group(1) );

			// The next token contains data which identifies the shop
			// and the item (which will be used later), and the price!
			// which means you don't need to consult thenext token.

			Matcher detailsMatcher = LISTDETAIL_PATTERN.matcher( linkText );
			if ( !detailsMatcher.find() )
				continue;

			int shopID = StaticEntity.parseInt( detailsMatcher.group(1) );
			int itemID = StaticEntity.parseInt( detailsMatcher.group(2) );
			int price = StaticEntity.parseInt( detailsMatcher.group(3) );

			String shopName = detailsMatcher.group(4).replaceAll( "<br>", " " );
			String itemName = TradeableItemDatabase.getItemName( itemID );
			boolean canPurchase = linkText.indexOf( "<td style=" ) == -1;

			if ( previousItemID != itemID )
			{
				previousItemID = itemID;
				addNPCStoreItem( itemName );
				itemNames.remove( itemName );
			}

			// Only add mall store results if the NPC store option
			// is not available.

			results.add( new MallPurchaseRequest( itemName, itemID, quantity, shopID, shopName, price, limit, canPurchase ) );
		}

		// Once the search is complete, add in any remaining NPC
		// store data and finalize the list.

		finalizeList( itemNames );
	}

	private void addNPCStoreItem( String itemName )
	{
		if ( NPCStoreDatabase.contains( itemName, false ) )
		{
			MallPurchaseRequest npcitem = NPCStoreDatabase.getPurchaseRequest( itemName );
			if ( !results.contains( npcitem ) )
				results.add( npcitem );
		}
	}

	private void finalizeList( List itemNames )
	{
		// Now, for the items which matched, check to see if there are
		// any entries inside of the NPC store database for them and
		// add - this is just in case some of the items become notrade
		// so items can still be bought from the NPC stores.

		for ( int i = 0; i < itemNames.size(); ++i )
			addNPCStoreItem( (String) itemNames.get(i) );

		if ( this.sortAfter )
		{
			if ( results instanceof LockableListModel )
				((LockableListModel)results).sort();
			else
				Collections.sort( results );
		}
	}

	protected void processResults()
	{
		if ( searchString == null || searchString.trim().length() == 0 )
			searchStore();
		else
			searchMall();
	}
}
