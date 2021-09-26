package net.sourceforge.kolmafia.request;

import java.util.Map;

import java.util.regex.Pattern;

import net.java.dev.spellcast.utilities.LockableListModel;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.KoLCharacter;

import net.sourceforge.kolmafia.objectpool.ItemPool;

import net.sourceforge.kolmafia.persistence.CoinmastersDatabase;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.session.InventoryManager;

public class ShoreGiftShopRequest
	extends CoinMasterRequest
{
	public static final String master = "The Shore, Inc. Gift Shop"; 

	private static final LockableListModel<AdventureResult> buyItems = CoinmastersDatabase.getBuyItems( ShoreGiftShopRequest.master );
	private static final Map<Integer, Integer> buyPrices = CoinmastersDatabase.getBuyPrices( ShoreGiftShopRequest.master );
	private static final Map<Integer, Integer> itemRows = CoinmastersDatabase.getRows( ShoreGiftShopRequest.master );
	private static final Pattern SCRIP_PATTERN = Pattern.compile( "(\\d+) Shore Inc. Ship Trip Scrip" );
	public static final AdventureResult SHIP_TRIP_SCRIP = ItemPool.get( ItemPool.SHIP_TRIP_SCRIP, 1 );

	public static final CoinmasterData SHORE_GIFT_SHOP =
		new CoinmasterData(
			ShoreGiftShopRequest.master,
			"shore",
			ShoreGiftShopRequest.class,
			"Shore Inc. Ship Trip Scrip",
			"no Shore Inc. Ship Trip Scrip",
			false,
			ShoreGiftShopRequest.SCRIP_PATTERN,
			ShoreGiftShopRequest.SHIP_TRIP_SCRIP,
			null,
			ShoreGiftShopRequest.itemRows,
			"shop.php?whichshop=shore",
			"buyitem",
			ShoreGiftShopRequest.buyItems,
			ShoreGiftShopRequest.buyPrices,
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
				case ItemPool.TOASTER:
					return !Preferences.getBoolean( "itemBoughtPerAscension637" );
				case ItemPool.UV_RESISTANT_COMPASS:
					return !InventoryManager.hasItem( itemId );
				}
				return super.canBuyItem( itemId );
			}

			@Override
			public void purchaseItem( AdventureResult item, boolean storage )
			{
				int itemId = item.getItemId();
				switch ( itemId )
				{
				case ItemPool.TOASTER:
					Preferences.setBoolean( "itemBoughtPerAscension637", true );
					break;
				}
			}
		};

	public ShoreGiftShopRequest()
	{
		super( ShoreGiftShopRequest.SHORE_GIFT_SHOP );
	}

	public ShoreGiftShopRequest( final boolean buying, final AdventureResult [] attachments )
	{
		super( ShoreGiftShopRequest.SHORE_GIFT_SHOP, buying, attachments );
	}

	public ShoreGiftShopRequest( final boolean buying, final AdventureResult attachment )
	{
		super( ShoreGiftShopRequest.SHORE_GIFT_SHOP, buying, attachment );
	}

	public ShoreGiftShopRequest( final boolean buying, final int itemId, final int quantity )
	{
		super( ShoreGiftShopRequest.SHORE_GIFT_SHOP, buying, itemId, quantity );
	}

	@Override
	public void run()
	{
		if ( this.action != null )
		{
			this.addFormField( "pwd" );
		}

		super.run();
	}

	@Override
	public void processResults()
	{
		ShoreGiftShopRequest.parseResponse( this.getURLString(), this.responseText );
	}

	public static void parseResponse( final String location, final String responseText )
	{
		if ( !location.contains( "whichshop=shore" ) )
		{
			return;
		}

		CoinmasterData data = ShoreGiftShopRequest.SHORE_GIFT_SHOP;

		String action = GenericRequest.getAction( location );
		if ( action != null )
		{
			CoinMasterRequest.parseResponse( data, location, responseText );
			return;
		}

		Preferences.setBoolean( "itemBoughtPerAscension637", !responseText.contains( "cheap toaster" ) );

		// Parse current coin balances
		CoinMasterRequest.parseBalance( data, responseText );
	}

	public static String accessible()
	{
		if ( !KoLCharacter.desertBeachAccessible() )
		{
			return "You can't get to the desert beach";
		}
		return null;
	}

	public static final boolean registerRequest( final String urlString )
	{
		if ( !urlString.startsWith( "shop.php" ) || !urlString.contains( "whichshop=shore" ) )
		{
			return false;
		}

		CoinmasterData data = ShoreGiftShopRequest.SHORE_GIFT_SHOP;
		return CoinMasterRequest.registerRequest( data, urlString, true );
	}
}
