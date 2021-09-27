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

import net.sourceforge.kolmafia.listener.NamedListenerRegistry;

import net.sourceforge.kolmafia.objectpool.IntegerPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;

import net.sourceforge.kolmafia.persistence.CoinmastersDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase;

import net.sourceforge.kolmafia.utilities.StringUtilities;

public class ArmoryAndLeggeryRequest
	extends CoinMasterRequest
{
	public static final String master = "Armory & Leggery";

	private static final LockableListModel<AdventureResult> buyItems = CoinmastersDatabase.getNewList();
	private static final Map<Integer, Integer> buyPrices = CoinmastersDatabase.getNewMap();
	private static final Map<Integer, Integer> itemRows = CoinmastersDatabase.getOrMakeRows( ArmoryAndLeggeryRequest.master );
	private static final Pattern TOKEN_PATTERN = Pattern.compile( "<td>([\\d,]+) FDKOL commendation" );

	// Since there are multiple, we need to have a map from itemId to
	// item/count of currency; an AdventureResult.
	private static final Map<Integer, AdventureResult> buyCosts = new TreeMap<Integer, AdventureResult>();

	public static final CoinmasterData ARMORY_AND_LEGGERY =
		new CoinmasterData(
			ArmoryAndLeggeryRequest.master,
			"armory",
			ArmoryAndLeggeryRequest.class,
			null,
			null,
			false,
			null,
			null,
			null,
			ArmoryAndLeggeryRequest.itemRows,
			"shop.php?whichshop=armory",
			"buyitem",
			ArmoryAndLeggeryRequest.buyItems,
			ArmoryAndLeggeryRequest.buyPrices,
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
				return ArmoryAndLeggeryRequest.buyCosts.get( IntegerPool.get( itemId ) );
			}
		};

	public ArmoryAndLeggeryRequest()
	{
		super( ArmoryAndLeggeryRequest.ARMORY_AND_LEGGERY );
	}

	public ArmoryAndLeggeryRequest( final boolean buying, final AdventureResult [] attachments )
	{
		super( ArmoryAndLeggeryRequest.ARMORY_AND_LEGGERY, buying, attachments );
	}

	public ArmoryAndLeggeryRequest( final boolean buying, final AdventureResult attachment )
	{
		super( ArmoryAndLeggeryRequest.ARMORY_AND_LEGGERY, buying, attachment );
	}

	public ArmoryAndLeggeryRequest( final boolean buying, final int itemId, final int quantity )
	{
		super( ArmoryAndLeggeryRequest.ARMORY_AND_LEGGERY, buying, itemId, quantity );
	}

	@Override
	public void processResults()
	{
		ArmoryAndLeggeryRequest.parseResponse( this.getURLString(), responseText );
	}

	// <tr rel="7985"><td valign=center></td><td><img src="https://s3.amazonaws.com/images.kingdomofloathing.com/itemimages/polyparachute.gif" class=hand onClick='javascript:descitem(973760204)'></td><td valign=center><a onClick='javascript:descitem(973760204)'><b>polyester parachute</b>&nbsp;&nbsp;&nbsp;&nbsp;</a></td><td><img src=https://s3.amazonaws.com/images.kingdomofloathing.com/itemimages/wickerbits.gif width=30 height=30 onClick='javascript:descitem(134381888)' alt="wickerbits" title="wickerbits"></td><td><b>1</b>&nbsp;&nbsp;</td><td></td><td>&nbsp;&nbsp;</td><td></td><td>&nbsp;&nbsp;</td><td></td><td>&nbsp;&nbsp;</td><td></td><td>&nbsp;&nbsp;</td><td valign=center><input class="button doit multibuy "  type=button rel='shop.php?whichshop=armory&action=buyitem&quantity=1&whichrow=804&pwd=' value='Buy'></td></tr>

	private static final Pattern ITEM_PATTERN =
		Pattern.compile( "<tr rel=\"(\\d+)\">.*?onClick='javascript:descitem\\((\\d+)\\)'>.*?<b>(.*?)</b>.*?title=\"(.*?)\".*?<b>([\\d,]+)</b>.*?whichrow=(\\d+)", Pattern.DOTALL );

	public static void parseResponse( final String location, final String responseText )
	{
		if ( !location.contains( "whichshop=armory" ) )
		{
			return;
		}

		// Learn new items by simply visiting the Armory & Leggery
		// Refresh the Coin Master inventory every time we visit.

		CoinmasterData data = ArmoryAndLeggeryRequest.ARMORY_AND_LEGGERY;
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

			if ( currency.equals( "Meat" ) )
			{
				continue;
			}

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

		ArmoryAndLeggeryRequest.buyItems.clear();
		ArmoryAndLeggeryRequest.buyItems.addAll( items );
		ArmoryAndLeggeryRequest.buyCosts.clear();
		ArmoryAndLeggeryRequest.buyCosts.putAll( costs );
		ArmoryAndLeggeryRequest.itemRows.clear();
		ArmoryAndLeggeryRequest.itemRows.putAll( rows );

		// Register the purchase requests, now that we know what is available
		data.registerPurchaseRequests();
		NamedListenerRegistry.fireChange( "(coinmaster)" );

		int itemId = CoinMasterRequest.extractItemId( data, location );

		if ( itemId == -1 )
		{
			// Purchase for Meat or a simple visit
			CoinMasterRequest.parseBalance( data, responseText );
			return;
		}

		CoinMasterRequest.parseResponse( data, location, responseText );
	}

	public static final boolean registerRequest( final String urlString, final boolean noMeat )
	{
		if ( !urlString.startsWith( "shop.php" ) || !urlString.contains( "whichshop=armory" ) )
		{
			return false;
		}

		Matcher m = GenericRequest.WHICHROW_PATTERN.matcher( urlString );
		if ( !m.find() )
		{
			// Just a visit
			return true;
		}

		CoinmasterData data = ArmoryAndLeggeryRequest.ARMORY_AND_LEGGERY;
		int itemId = CoinMasterRequest.extractItemId( data, urlString );

		if ( itemId == -1 )
		{
			// Presumably this is a purchase for Meat.
			// If we've already checked Meat, this is an unknown item
			if ( noMeat )
			{
				return false;
			}
			return NPCPurchaseRequest.registerShopRequest( urlString, true );
		}

		return CoinMasterRequest.registerRequest( data, urlString, true );
	}
}
