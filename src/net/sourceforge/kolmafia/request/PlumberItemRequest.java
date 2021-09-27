package net.sourceforge.kolmafia.request;

import java.util.Map;

import java.util.regex.Pattern;

import net.java.dev.spellcast.utilities.LockableListModel;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.KoLCharacter;

import net.sourceforge.kolmafia.objectpool.ItemPool;

import net.sourceforge.kolmafia.persistence.CoinmastersDatabase;

public class PlumberItemRequest
	extends CoinMasterRequest
{
	public static final String master = "Mushroom District Item Shop";

	private static final LockableListModel<AdventureResult> buyItems = CoinmastersDatabase.getBuyItems( PlumberItemRequest.master );
	private static final Map<Integer, Integer> buyPrices = CoinmastersDatabase.getBuyPrices( PlumberItemRequest.master );
	private static final Map<Integer, Integer> itemRows = CoinmastersDatabase.getRows( PlumberItemRequest.master );
	private static final Pattern TOKEN_PATTERN = Pattern.compile( "([\\d,]+) coin" );
	public static final AdventureResult COIN = ItemPool.get( ItemPool.COIN, 1 );

	public static final CoinmasterData PLUMBER_ITEMS =
		new CoinmasterData(
			PlumberItemRequest.master,
			"marioitems",
			PlumberItemRequest.class,
			"coin",
			"no coins",
			false,
			PlumberItemRequest.TOKEN_PATTERN,
			PlumberItemRequest.COIN,
			null,
			PlumberItemRequest.itemRows,
			"shop.php?whichshop=marioitems",
			"buyitem",
			PlumberItemRequest.buyItems,
			PlumberItemRequest.buyPrices,
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
			);

	public PlumberItemRequest()
	{
		super( PlumberItemRequest.PLUMBER_ITEMS );
	}

	public PlumberItemRequest( final boolean buying, final AdventureResult [] attachments )
	{
		super( PlumberItemRequest.PLUMBER_ITEMS, buying, attachments );
	}

	public PlumberItemRequest( final boolean buying, final AdventureResult attachment )
	{
		super( PlumberItemRequest.PLUMBER_ITEMS, buying, attachment );
	}

	public PlumberItemRequest( final boolean buying, final int itemId, final int quantity )
	{
		super( PlumberItemRequest.PLUMBER_ITEMS, buying, itemId, quantity );
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
		PlumberItemRequest.parseResponse( this.getURLString(), this.responseText );
	}

	public static void parseResponse( final String location, final String responseText )
	{
		CoinmasterData data = PlumberItemRequest.PLUMBER_ITEMS;
		String action = GenericRequest.getAction( location );
		if ( action == null )
		{
			if ( location.contains( "whichshop=marioitems" ) )
			{
				// Parse current coin balances
				CoinMasterRequest.parseBalance( data, responseText );
			}

			return;
		}

		CoinMasterRequest.parseResponse( data, location, responseText );
	}

	public static String accessible()
	{
		if ( !KoLCharacter.isPlumber() )
		{
			return "You are not a plumber.";
		}
		return null;
	}

	public static final boolean registerRequest( final String urlString )
	{
		if ( !urlString.startsWith( "shop.php" ) || !urlString.contains( "whichshop=marioitems" ) )
		{
			return false;
		}

		CoinmasterData data = PlumberItemRequest.PLUMBER_ITEMS;
		return CoinMasterRequest.registerRequest( data, urlString, true );
	}
}
