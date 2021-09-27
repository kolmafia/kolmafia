package net.sourceforge.kolmafia.request;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.java.dev.spellcast.utilities.LockableListModel;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.CoinmasterData;

import net.sourceforge.kolmafia.objectpool.IntegerPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;

import net.sourceforge.kolmafia.persistence.CoinmastersDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.session.QuestManager;

import net.sourceforge.kolmafia.utilities.StringUtilities;

public class MerchTableRequest
	extends CoinMasterRequest
{
	public static final String master = "KoL Con 13 Merch Table"; 

	private static final LockableListModel<AdventureResult> buyItems = CoinmastersDatabase.getNewList();
	private static final Map<Integer, Integer> buyPrices = CoinmastersDatabase.getNewMap();
	private static final Map<Integer, Integer> itemRows = CoinmastersDatabase.getOrMakeRows( MerchTableRequest.master );

	private static final Pattern MR_A_PATTERN = Pattern.compile( "You have (\\w+) Mr. Accessor(?:y|ies) to trade." );
	public static final AdventureResult MR_A = ItemPool.get( ItemPool.MR_ACCESSORY, 1 );

	private static final Pattern CHRONER_PATTERN = Pattern.compile( "You have (\\w+) Mr. Chroner to trade." );
	public static final AdventureResult CHRONER = ItemPool.get( ItemPool.CHRONER, 1 );

	public static final CoinmasterData MERCH_TABLE =
		new CoinmasterData(
			MerchTableRequest.master,
			"conmerch",
			MerchTableRequest.class,
			"Mr. A",
			"You have no Mr. Accessories to trade",
			false,
			MerchTableRequest.MR_A_PATTERN,
			MerchTableRequest.MR_A,
			null,
			MerchTableRequest.itemRows,
			"shop.php?whichshop=conmerch",
			"buyitem",
			MerchTableRequest.buyItems,
			MerchTableRequest.buyPrices,
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
			public AdventureResult itemBuyPrice( final int itemId )
			{
				return MerchTableRequest.buyCosts.get( IntegerPool.get( itemId ) );
			}
		};

	// Since there are two different currencies, we need to have a map from
	// itemId to item/count of currency; an AdventureResult.
	private static final Map<Integer, AdventureResult> buyCosts = new TreeMap<Integer, AdventureResult>();

	public MerchTableRequest()
	{
		super( MerchTableRequest.MERCH_TABLE );
	}

	public MerchTableRequest( final String action )
	{
		super( MerchTableRequest.MERCH_TABLE, action );
	}

	public MerchTableRequest( final boolean buying, final AdventureResult [] attachments )
	{
		super( MerchTableRequest.MERCH_TABLE, buying, attachments );
	}

	public MerchTableRequest( final boolean buying, final AdventureResult attachment )
	{
		super( MerchTableRequest.MERCH_TABLE, buying, attachment );
	}

	public MerchTableRequest( final boolean buying, final int itemId, final int quantity )
	{
		super( MerchTableRequest.MERCH_TABLE, buying, itemId, quantity );
	}

	@Override
	public void processResults()
	{
		MerchTableRequest.parseResponse( this.getURLString(), responseText );
	}

	// <tr rel="9148"><td valign=center></td><td><img src="https://s3.amazonaws.com/images.kingdomofloathing.com/itemimages/twitchtatkit.gif" class="hand pop" rel="desc_item.php?whichitem=216403537" onClick='javascript:descitem(216403537)'></td><td valign=center><a onClick='javascript:descitem(216403537)'><b>Twitching Television Tattoo</b>&nbsp;&nbsp;&nbsp;&nbsp;</a></td><td><img src=https://s3.amazonaws.com/images.kingdomofloathing.com/itemimages/chroner.gif width=30 height=30 onClick='javascript:descitem(783338147)' alt="Chroner" title="Chroner"></td><td><b>1,111</b>&nbsp;&nbsp;</td><td></td><td>&nbsp;&nbsp;</td><td></td><td>&nbsp;&nbsp;</td><td></td><td>&nbsp;&nbsp;</td><td></td><td>&nbsp;&nbsp;</td><td valign=center><input class="button doit multibuy "  type=button rel='shop.php?whichshop=conmerch&action=buyitem&quantity=1&whichrow=895&pwd=' value='Buy'></td></tr>

	private static final Pattern ITEM_PATTERN =
		Pattern.compile( "<tr rel=\"(\\d+)\">.*?onClick='javascript:descitem\\((\\d+)\\)'>.*?<b>(.*?)</b>.*?title=\"(.*?)\".*?<b>([\\d,]+)</b>.*?whichrow=(\\d+)", Pattern.DOTALL );
	public static void parseResponse( final String urlString, final String responseText )
	{
		if ( !urlString.contains( "whichshop=conmerch" ) )
		{
			return;
		}

		if ( responseText.contains( "That store isn't there anymore." ) )
		{
			QuestManager.handleTimeTower( false );
			return;
		}
		
		QuestManager.handleTimeTower( true );

		// Learn new items by simply visiting the Merch Table
		// Refresh the Coin Master inventory every time we visit.

		CoinmasterData data = MerchTableRequest.MERCH_TABLE;
		List<AdventureResult> items = new ArrayList<AdventureResult>();
		Map<Integer, AdventureResult> costs = new TreeMap<Integer, AdventureResult>();
		Map<Integer, Integer> rows = new TreeMap<Integer, Integer>();

		Matcher matcher = ITEM_PATTERN.matcher( responseText );
		while ( matcher.find() )
		{
			int itemId = StringUtilities.parseInt( matcher.group(1) );
			Integer iitemId = IntegerPool.get( itemId );
			String descId = matcher.group(2);
			String itemName = matcher.group(3);
			String currency = matcher.group(4);
			int price = StringUtilities.parseInt( matcher.group(5) );
			int row = StringUtilities.parseInt( matcher.group(6) );

			String match = ItemDatabase.getItemDataName( itemId );
			if ( match == null || !match.equals( itemName ) )
			{
				ItemDatabase.registerItem( itemId, itemName, descId );
			}

			AdventureResult item = ItemPool.get( itemId, PurchaseRequest.MAX_QUANTITY );
			items.add( item );
			AdventureResult cost = ItemPool.get( currency, price );
			costs.put( iitemId, cost );
			rows.put( iitemId, IntegerPool.get( row ) );
		}

		MerchTableRequest.buyItems.clear();
		MerchTableRequest.buyItems.addAll( items );
		MerchTableRequest.buyCosts.clear();
		MerchTableRequest.buyCosts.putAll( costs );
		MerchTableRequest.itemRows.clear();
		MerchTableRequest.itemRows.putAll( rows );

		// Register the purchase requests, now that we know what is available
		data.registerPurchaseRequests();

		String action = GenericRequest.getAction( urlString );
		if ( action != null )
		{
			CoinMasterRequest.parseResponse( data, urlString, responseText );
			return;
		}

		// Parse current coin balances
		CoinMasterRequest.parseBalance( data, responseText );
	}

	public static String accessible()
	{
		if ( !Preferences.getBoolean( "timeTowerAvailable" ) )
		{
			return "You can't get to the KoL Con 13 Merch Table";
		}
		return null;
	}

	public static boolean registerRequest( final String urlString )
	{
		if ( !urlString.startsWith( "shop.php" ) || !urlString.contains( "whichshop=conmerch" ) )
		{
			return false;
		}

		CoinmasterData data = MerchTableRequest.MERCH_TABLE;
		return CoinMasterRequest.registerRequest( data, urlString, true );
	}
}
