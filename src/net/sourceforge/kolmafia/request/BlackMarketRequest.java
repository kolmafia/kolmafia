package net.sourceforge.kolmafia.request;

import java.util.Map;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.java.dev.spellcast.utilities.LockableListModel;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.CoinmasterData;

import net.sourceforge.kolmafia.objectpool.ItemPool;

import net.sourceforge.kolmafia.persistence.CoinmastersDatabase;

import net.sourceforge.kolmafia.session.InventoryManager;

public class BlackMarketRequest
	extends CoinMasterRequest
{
	public static final String master = "The Black Market";
	public static final AdventureResult TOKEN =  ItemPool.get( ItemPool.PRICELESS_DIAMOND, 1 );
	private static final Pattern TOKEN_PATTERN = Pattern.compile( "<td>([\\d,]+) priceless diamond" );
	public static final LockableListModel<AdventureResult> buyItems = CoinmastersDatabase.getBuyItems( BlackMarketRequest.master );
	private static final Map<Integer, Integer> buyPrices = CoinmastersDatabase.getBuyPrices( BlackMarketRequest.master );
	private static final Map<Integer, Integer> itemRows = CoinmastersDatabase.getRows( BlackMarketRequest.master );

	public static final CoinmasterData BLACK_MARKET =
		new CoinmasterData(
			BlackMarketRequest.master,
			"blackmarket",
			BlackMarketRequest.class,
			"priceless diamond",
			null,
			false,
			BlackMarketRequest.TOKEN_PATTERN,
			BlackMarketRequest.TOKEN,
			null,
			BlackMarketRequest.itemRows,
			"shop.php?whichshop=blackmarket",
			"buyitem",
			BlackMarketRequest.buyItems,
			BlackMarketRequest.buyPrices,
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
			public final boolean canBuyItem( final int itemId )
			{
				switch ( itemId )
				{
				case ItemPool.ZEPPELIN_TICKET:
					return InventoryManager.getCount( itemId ) == 0;
				}
				return super.canBuyItem( itemId );
			}
		};

	public BlackMarketRequest()
	{
		super( BlackMarketRequest.BLACK_MARKET );
	}

	public BlackMarketRequest( final boolean buying, final AdventureResult [] attachments )
	{
		super( BlackMarketRequest.BLACK_MARKET, buying, attachments );
	}

	public BlackMarketRequest( final boolean buying, final AdventureResult attachment )
	{
		super( BlackMarketRequest.BLACK_MARKET, buying, attachment );
	}

	public BlackMarketRequest( final boolean buying, final int itemId, final int quantity )
	{
		super( BlackMarketRequest.BLACK_MARKET, buying, itemId, quantity );
	}

	@Override
	public void processResults()
	{
		BlackMarketRequest.parseResponse( this.getURLString(), responseText );
	}

	public static void parseResponse( final String location, final String responseText )
	{
		if ( !location.contains( "whichshop=blackmarket" ) )
		{
			return;
		}

		CoinmasterData data = BlackMarketRequest.BLACK_MARKET;
		int itemId = CoinMasterRequest.extractItemId( data, location );

		if ( itemId == -1 )
		{
			// Purchase for Meat or a simple visit
			CoinMasterRequest.parseBalance( data, responseText );
			return;
		}

		CoinMasterRequest.parseResponse( data, location, responseText );
	}

	public static String accessible()
	{
		if ( !QuestLogRequest.isBlackMarketAvailable() )
		{
			return "The Black Market is not currently available";
		}
		return null;
	}

	public static final boolean registerRequest( final String urlString, final boolean noMeat )
	{
		if ( !urlString.startsWith( "shop.php" ) || !urlString.contains( "whichshop=blackmarket" ) )
		{
			return false;
		}

		Matcher m = GenericRequest.WHICHROW_PATTERN.matcher( urlString );
		if ( !m.find() )
		{
			// Just a visit
			return true;
		}

		CoinmasterData data = BlackMarketRequest.BLACK_MARKET;
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
