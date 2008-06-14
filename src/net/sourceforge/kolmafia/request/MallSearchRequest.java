/**
 * Copyright (c) 2005-2008, KoLmafia development team
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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.persistence.NPCStoreDatabase;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class MallSearchRequest
	extends GenericRequest
{
	private static final Pattern FAVORITES_PATTERN = Pattern.compile( "&action=unfave&whichstore=(\\d+)\">" );
	private static final Pattern STOREID_PATTERN = Pattern.compile( "<b>(.*?) \\(<a.*?who=(\\d+)\"" );
	private static final Pattern STORELIMIT_PATTERN = Pattern.compile( "Limit ([\\d,]+) /" );
	private static final Pattern STOREPRICE_PATTERN =
		Pattern.compile( "radio value=(\\d+).*?<b>(.*?)</b> \\(([\\d,]+)\\)(.*?)</td>" );
	private static final Pattern STOREDETAIL_PATTERN = Pattern.compile( "<tr>.*?</a>" );

	private static final Pattern LISTQUANTITY_PATTERN = Pattern.compile( "\\([\\d,]+\\)" );
	private static final Pattern LISTLIMIT_PATTERN = Pattern.compile( "([\\d,]+)\\&nbsp;\\/\\&nbsp;day" );
	private static final Pattern LISTDETAIL_PATTERN =
		Pattern.compile( "whichstore=(\\d+)\\&searchitem=(\\d+)\\&searchprice=(\\d+)\">(.*?)</a>" );

	private List results;
	private final boolean retainAll;
	private String searchString;

	public MallSearchRequest( final int storeId )
	{
		super( "mallstore.php" );
		this.addFormField( "whichstore", String.valueOf( storeId ) );

		this.results = new ArrayList();
		this.retainAll = true;
	}

	/**
	 * Constructs a new <code>MallSearchRequest</code> which searches for the given item, storing the results in the
	 * given <code>ListModel</code>. Note that the search string is exactly the same as the way KoL does it at the
	 * current time.
	 *
	 * @param searchString The string (including wildcards) for the item to be found
	 * @param cheapestCount The number of stores to show; use a non-positive number to show all
	 * @param results The sorted list in which to store the results
	 */

	public MallSearchRequest( final String searchString, final int cheapestCount, final List results )
	{
		this( searchString, cheapestCount, results, false );
	}

	/**
	 * Constructs a new <code>MallSearchRequest</code> which searches for the given item, storing the results in the
	 * given <code>ListModel</code>. Note that the search string is exactly the same as the way KoL does it at the
	 * current time.
	 *
	 * @param searchString The string (including wildcards) for the item to be found
	 * @param cheapestCount The number of stores to show; use a non-positive number to show all
	 * @param results The sorted list in which to store the results
	 * @param retainAll Whether the result list should be cleared before searching
	 */

	public MallSearchRequest( final String searchString, final int cheapestCount, final List results,
		final boolean retainAll )
	{
		super( searchString == null || searchString.trim().length() == 0 ? "mall.php" : "searchmall.php" );

		this.searchString = searchString;
		this.addFormField( "whichitem", this.searchString );

		if ( cheapestCount > 0 )
		{
			this.addFormField( "cheaponly", "on" );
			this.addFormField( "shownum", "" + cheapestCount );
		}

		this.results = results;
		this.retainAll = retainAll;
	}

	protected boolean retryOnTimeout()
	{
		return true;
	}

	public static final String getSearchString( String itemName )
	{
		int itemId = ItemDatabase.getItemId( itemName );

		if ( itemId == -1 )
		{
			return itemName;
		}

		String canonicalName = StringUtilities.getCanonicalName( ItemDatabase.getItemName( itemId ) );
		int entityIndex = canonicalName.indexOf( "&" );

		if ( entityIndex == -1 )
		{
			return "\"" + canonicalName + "\"";
		}

		return StringUtilities.globalStringReplace( canonicalName, "&amp;", "&" );
	}

	public List getResults()
	{
		return this.results;
	}

	public void setResults( final List results )
	{
		this.results = results;
	}

	/**
	 * Executes the search request. In the event that no item is found, the currently active frame will be notified.
	 * Otherwise, all items are stored inside of the results list. Note also that the results will be cleared before
	 * being stored.
	 */

	public void run()
	{
		if ( this.searchString == null || this.searchString.trim().length() == 0 )
		{
			KoLmafia.updateDisplay( this.retainAll ? "Scanning store inventories..." : "Looking up favorite stores list..." );
		}
		else if ( this.searchString.startsWith( "\"" ) || this.searchString.startsWith( "\'" ) )
		{
			KoLmafia.updateDisplay( "Searching for " + this.searchString + "..." );
		}
		else
		{
			this.results.clear();
			List itemNames = ItemDatabase.getMatchingNames( this.searchString );

			// Check for any items which are not available in NPC stores and
			// known not to be tradeable to see if there's an exact match.

			Iterator itemIterator = itemNames.iterator();
			int npcItemCount = 0;

			while ( itemIterator.hasNext() )
			{
				String itemName = (String) itemIterator.next();
				int itemId = ItemDatabase.getItemId( itemName );

				if ( NPCStoreDatabase.contains( itemName ) )
				{
					++npcItemCount;
				}
				else if ( !ItemDatabase.isTradeable( itemId ) )
				{
					itemIterator.remove();
				}
			}

			// If the results contain only NPC items, then you don't need
			// to run a mall search.

			if ( npcItemCount > 0 && itemNames.size() == npcItemCount )
			{
				this.finalizeList( itemNames );
				return;
			}

			// If there is only one applicable match, then make sure you
			// search for the exact item (may be a fuzzy matched item).

			if ( itemNames.size() == 1 )
			{
				this.searchString = MallSearchRequest.getSearchString( (String) itemNames.get( 0 ) );
				this.addFormField( "whichitem", this.searchString );
			}

			KoLmafia.updateDisplay( "Searching for " + this.searchString + "..." );
		}

		super.run();
	}

	private void searchStore()
	{
		Pattern mangledEntityPattern = Pattern.compile( "\\s+;" );
		
		if ( this.retainAll )
		{
			Matcher shopMatcher = MallSearchRequest.STOREID_PATTERN.matcher( this.responseText );
			shopMatcher.find();

			int shopId = StringUtilities.parseInt( shopMatcher.group( 2 ) );

			// Handle character entities mangled by KoL.

			String shopName = mangledEntityPattern.matcher( shopMatcher.group( 1 ) ).replaceAll( ";" );

			int lastFindIndex = 0;
			Matcher priceMatcher = MallSearchRequest.STOREPRICE_PATTERN.matcher( this.responseText );

			while ( priceMatcher.find( lastFindIndex ) )
			{
				lastFindIndex = priceMatcher.end();
				String priceId = priceMatcher.group( 1 );

				String itemName = priceMatcher.group( 2 );

				int itemId = StringUtilities.parseInt( priceId.substring( 0, priceId.length() - 9 ) );
				int quantity = StringUtilities.parseInt( priceMatcher.group( 3 ) );
				int limit = quantity;

				Matcher limitMatcher = MallSearchRequest.STORELIMIT_PATTERN.matcher( priceMatcher.group( 4 ) );
				if ( limitMatcher.find() )
				{
					limit = StringUtilities.parseInt( limitMatcher.group( 1 ) );
				}

				int price = StringUtilities.parseInt( priceId.substring( priceId.length() - 9 ) );
				this.results.add( new MallPurchaseRequest(
					itemName, itemId, quantity, shopId, shopName, price, limit, true ) );
			}
		}
		else
		{
			MallSearchRequest individualStore;
			Matcher storeMatcher = MallSearchRequest.FAVORITES_PATTERN.matcher( this.responseText );

			int lastFindIndex = 0;
			while ( storeMatcher.find( lastFindIndex ) )
			{
				lastFindIndex = storeMatcher.end();
				individualStore = new MallSearchRequest( StringUtilities.parseInt( storeMatcher.group( 1 ) ) );
				individualStore.run();

				this.results.addAll( individualStore.results );
			}

			KoLmafia.updateDisplay( "Search complete." );
		}
	}

	private void searchMall()
	{
		List itemNames = ItemDatabase.getMatchingNames( this.searchString );

		// Change all multi-line store names into single line store names so that the
		// parser doesn't get confused; remove all stores where limits have already
		// been reached (which have been greyed out), and then remove all non-anchor
		// tags to make everything easy to parse.

		int startIndex = this.responseText.indexOf( "Search Results:" );
		String storeListResult = this.responseText.substring( startIndex < 0 ? 0 : startIndex );

		Matcher linkMatcher = MallSearchRequest.STOREDETAIL_PATTERN.matcher( storeListResult );
		String linkText = null;

		int previousItemId = -1;

		while ( linkMatcher.find() )
		{
			linkText = linkMatcher.group();
			Matcher quantityMatcher = MallSearchRequest.LISTQUANTITY_PATTERN.matcher( linkText );
			int quantity = 0;

			if ( quantityMatcher.find() )
			{
				quantity = StringUtilities.parseInt( quantityMatcher.group() );
			}

			int limit = quantity;

			Matcher limitMatcher = MallSearchRequest.LISTLIMIT_PATTERN.matcher( linkText );
			if ( limitMatcher.find() )
			{
				limit = StringUtilities.parseInt( limitMatcher.group( 1 ) );
			}

			// The next token contains data which identifies the shop
			// and the item (which will be used later), and the price!
			// which means you don't need to consult thenext token.

			Matcher detailsMatcher = MallSearchRequest.LISTDETAIL_PATTERN.matcher( linkText );
			if ( !detailsMatcher.find() )
			{
				continue;
			}

			int shopId = StringUtilities.parseInt( detailsMatcher.group( 1 ) );
			int itemId = StringUtilities.parseInt( detailsMatcher.group( 2 ) );
			int price = StringUtilities.parseInt( detailsMatcher.group( 3 ) );

			String shopName = detailsMatcher.group( 4 ).replaceAll( "<br>", " " );
			String itemName = ItemDatabase.getItemName( itemId );
			boolean canPurchase = linkText.indexOf( "<td style=" ) == -1;

			if ( previousItemId != itemId )
			{
				previousItemId = itemId;
				this.addNPCStoreItem( itemName );
				itemNames.remove( itemName );
			}

			// Only add mall store results if the NPC store option
			// is not available.

			this.results.add( new MallPurchaseRequest(
				itemName, itemId, quantity, shopId, shopName, price, limit, canPurchase ) );
		}

		// Once the search is complete, add in any remaining NPC
		// store data and finalize the list.

		this.finalizeList( itemNames );
	}

	private void addNPCStoreItem( final String itemName )
	{
		if ( NPCStoreDatabase.contains( itemName, false ) )
		{
			MallPurchaseRequest npcitem = NPCStoreDatabase.getPurchaseRequest( itemName );
			if ( !this.results.contains( npcitem ) )
			{
				this.results.add( npcitem );
			}
		}
	}

	private void finalizeList( final List itemNames )
	{
		// Now, for the items which matched, check to see if there are
		// any entries inside of the NPC store database for them and
		// add - this is just in case some of the items become notrade
		// so items can still be bought from the NPC stores.

		for ( int i = 0; i < itemNames.size(); ++i )
		{
			this.addNPCStoreItem( (String) itemNames.get( i ) );
		}
	}

	public void processResults()
	{
		if ( this.searchString == null || this.searchString.trim().length() == 0 )
		{
			this.searchStore();
		}
		else
		{
			this.searchMall();
		}
	}
}
