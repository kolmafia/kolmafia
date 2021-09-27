package net.sourceforge.kolmafia.request;

import java.util.Map;

import java.util.regex.Pattern;

import net.java.dev.spellcast.utilities.LockableListModel;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.CoinmasterData;

import net.sourceforge.kolmafia.objectpool.ItemPool;

import net.sourceforge.kolmafia.persistence.CoinmastersDatabase;


public class LTTRequest
	extends CoinMasterRequest
{
	public static final String master = "LT&T Gift Shop";
	private static final LockableListModel<AdventureResult> buyItems = CoinmastersDatabase.getBuyItems(LTTRequest.master );
	private static final Map<Integer, Integer> buyPrices = CoinmastersDatabase.getBuyPrices(LTTRequest.master );
	private static final Map<Integer, Integer> itemRows = CoinmastersDatabase.getRows(LTTRequest.master );

	private static final Pattern TOKEN_PATTERN = Pattern.compile( "<td>([\\d,]+) buffalo dime" );
	public static final AdventureResult COIN = ItemPool.get( ItemPool.BUFFALO_DIME, 1 );
	public static final CoinmasterData LTT =
		new CoinmasterData(
			LTTRequest.master,
			"LT&T Gift Shop",
			LTTRequest.class,
			"buffalo dime",
			null,
			false,
			LTTRequest.TOKEN_PATTERN,
			LTTRequest.COIN,
			null,
			LTTRequest.itemRows,
			"shop.php?whichshop=ltt",
			"buyitem",
			LTTRequest.buyItems,
			LTTRequest.buyPrices,
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

	public LTTRequest()
	{
		super(LTTRequest.LTT );
	}

	public LTTRequest( final boolean buying, final AdventureResult [] attachments )
	{
		super(LTTRequest.LTT, buying, attachments );
	}

	public LTTRequest( final boolean buying, final AdventureResult attachment )
	{
		super(LTTRequest.LTT, buying, attachment );
	}

	public LTTRequest( final boolean buying, final int itemId, final int quantity )
	{
		super(LTTRequest.LTT, buying, itemId, quantity );
	}

	@Override
	public void processResults()
	{
		LTTRequest.parseResponse( this.getURLString(), this.responseText );
	}

	public static void parseResponse( final String urlString, final String responseText )
	{
		if ( !urlString.contains( "whichshop=ltt" ) )
		{
			return;
		}

		CoinmasterData data = LTTRequest.LTT;

		String action = GenericRequest.getAction( urlString );
		if ( action != null )
		{
			CoinMasterRequest.parseResponse( data, urlString, responseText );
			return;
		}

		// Parse current coin balances
		CoinMasterRequest.parseBalance( data, responseText );
	}

	public static boolean registerRequest( final String urlString )
	{
		if ( !urlString.startsWith( "shop.php" ) || !urlString.contains( "whichshop=ltt" ) )
		{
			return false;
		}

		CoinmasterData data = LTTRequest.LTT;
		return CoinMasterRequest.registerRequest( data, urlString, true );
	}

	public static String accessible()
	{
		return null;
	}
}
