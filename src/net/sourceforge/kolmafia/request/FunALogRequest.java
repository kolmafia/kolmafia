package net.sourceforge.kolmafia.request;

import java.util.List;
import java.util.Map;
import java.util.Set;

import java.util.regex.Pattern;
import java.util.regex.Matcher;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.CoinmasterData;

import net.sourceforge.kolmafia.objectpool.Concoction;
import net.sourceforge.kolmafia.objectpool.ConcoctionPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;

import net.sourceforge.kolmafia.persistence.CoinmastersDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase;

import net.sourceforge.kolmafia.session.InventoryManager;

import net.sourceforge.kolmafia.utilities.StringUtilities;

public class FunALogRequest
	extends CoinMasterRequest
{
	public static final String master = "PirateRealm Fun-a-Log";
	private static final List<AdventureResult> buyItems = CoinmastersDatabase.getBuyItems(FunALogRequest.master );
	private static final Map<Integer, Integer> buyPrices = CoinmastersDatabase.getBuyPrices(FunALogRequest.master );
	private static final Map<Integer, Integer> itemRows = CoinmastersDatabase.getRows(FunALogRequest.master );

	private static final Pattern TOKEN_PATTERN = Pattern.compile( "<b>You have ([\\d,]+) FunPoints?\\.</b>" );
	public static final CoinmasterData FUN_A_LOG =
		new CoinmasterData(
			FunALogRequest.master,
			"Fun-a-Log",
			FunALogRequest.class,
			"FunPoint",
			"You have no FunPoints",
			false,
			FunALogRequest.TOKEN_PATTERN,
			null,
			"availableFunPoints",
			FunALogRequest.itemRows,
			"shop.php?whichshop=piraterealm",
			"buyitem",
			FunALogRequest.buyItems,
			FunALogRequest.buyPrices,
			null,
			null,
			null,
			null,
			"whichrow",
			GenericRequest.WHICHROW_PATTERN,
			"quantity",
			GenericRequest.QUANTITY_PATTERN,
			null,
			null,
			true
			)
		{
			@Override
			public final boolean availableItem( final int itemId )
			{
				return unlockedItems.contains( ItemDatabase.getItemName( itemId ) );
			}
		};

	static
	{
		ConcoctionPool.set( new Concoction( "FunPoint", "availableFunPoints" ) );
	}

    private static String unlockedItems= "";

	public FunALogRequest()
	{
		super( FunALogRequest.FUN_A_LOG );
	}

	public FunALogRequest( final boolean buying, final AdventureResult [] attachments )
	{
		super(FunALogRequest.FUN_A_LOG, buying, attachments );
	}

	public FunALogRequest( final boolean buying, final AdventureResult attachment )
	{
		super(FunALogRequest.FUN_A_LOG, buying, attachment );
	}

	public FunALogRequest( final boolean buying, final int itemId, final int quantity )
	{
		super(FunALogRequest.FUN_A_LOG, buying, itemId, quantity );
	}

	@Override
	public void processResults()
	{
		FunALogRequest.parseResponse( this.getURLString(), this.responseText );
	}

	// <tr rel="10231"><td valign=center><input type=radio name=whichrow value=1064></td><td><img src="https://s3.amazonaws.com/images.kingdomofloathing.com/itemimages/pr_partyhat.gif" class="hand pop" rel="desc_item.php?whichitem=971293634" onClick='javascript:descitem(971293634)'></td><td valign=center><a onClick='javascript:descitem(971293634)'><b>PirateRealm party hat</b>&nbsp;&nbsp;&nbsp;&nbsp;</a></td><td>F</td><td><b>20</b>&nbsp;&nbsp;</td><td></td><td>&nbsp;&nbsp;</td><td></td><td>&nbsp;&nbsp;</td><td></td><td>&nbsp;&nbsp;</td><td></td><td>&nbsp;&nbsp;</td><td valign=center><input class="button doit multibuy "  type=button rel='shop.php?whichshop=piraterealm&action=buyitem&quantity=1&whichrow=1064&pwd=5f195b385cbe62956e089308af45f544' value='Buy'></td></tr>

	private static final Pattern ITEM_PATTERN =
		Pattern.compile( "<tr rel=\"(\\d+)\">.*?whichrow value=(\\d+)>.*?desc_item.php\\?whichitem=(\\d+).*?<b>(.*?)</b>.*?<td>F</td><td><b>([,\\d]+)</b>" );
	public static void parseResponse( final String urlString, final String responseText )
	{
		if ( !urlString.contains( "whichshop=piraterealm" ) )
		{
			return;
		}

		// Learn new Fun-a-Log simply visiting the shop
		// Refresh the Coin Master inventory every time we visit.

		CoinmasterData data = FunALogRequest.FUN_A_LOG;

		Set<Integer> originalItems = FunALogRequest.buyPrices.keySet();
		List<AdventureResult> items = FunALogRequest.buyItems;
		Map<Integer, Integer> prices = FunALogRequest.buyPrices;
		Map<Integer, Integer> rows = FunALogRequest.itemRows;

		StringBuilder unlocked = new StringBuilder();

		Matcher matcher = ITEM_PATTERN.matcher( responseText );
		while ( matcher.find() )
		{
			int itemId = StringUtilities.parseInt( matcher.group(1) );
			int row = StringUtilities.parseInt( matcher.group(2) );
			String descId = matcher.group(3);
			String itemName = matcher.group(4);
			int price = StringUtilities.parseInt( matcher.group(5) );

			String match = ItemDatabase.getItemDataName( itemId );
			if ( match == null || !match.equals( itemName ) )
			{
				ItemDatabase.registerItem( itemId, itemName, descId );
			}

			// Add it to the unlocked items
			if ( unlocked.length() > 0 )
			{
				unlocked.append( "|" );
			}
			unlocked.append( itemName );

			// If this item was not previously known, 
			if ( !originalItems.contains( itemId ) )
			{
				// Add it to the Fun-a-Log inventory
				AdventureResult item = ItemPool.get( itemId, PurchaseRequest.MAX_QUANTITY );
				items.add( item );
				prices.put( itemId, price );
				rows.put( itemId, row );

				// Print a coinmasters.txt line for it
				NPCPurchaseRequest.learnCoinmasterItem( master, itemName, String.valueOf( price ), String.valueOf( row ) );
			}
		}

		// Remember which items we have unlocked
		unlockedItems = unlocked.toString();

		// Register the purchase requests, now that we know what is available
		data.registerPurchaseRequests();

		CoinMasterRequest.parseResponse( data, urlString, responseText );
	}

	public static boolean registerRequest( final String urlString )
	{
		if ( !urlString.startsWith( "shop.php" ) || !urlString.contains( "whichshop=piraterealm" ) )
		{
			return false;
		}

		CoinmasterData data = FunALogRequest.FUN_A_LOG;
		return CoinMasterRequest.registerRequest( data, urlString, true );
	}

	public static String accessible()
	{
		// You have to have the Fun-A-Log in your inventory in order to
		// purchase from it.  It is a quest item, so if you have it, it
		// will be there.  You get it the first time you complete a
		// PirateRealm adventure.  Therefore, you needed access to the
		// PirateRealm at least once to get it, but you do not need
		// current access to PirateRealm to use it.

		return InventoryManager.hasItem( ItemPool.PIRATE_REALM_FUN_LOG )  ? null : "Need PirateRealm fun-a-log";

	}
}
