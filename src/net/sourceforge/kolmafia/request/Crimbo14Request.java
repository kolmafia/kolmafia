package net.sourceforge.kolmafia.request;

import java.util.Map;

import java.util.regex.Pattern;

import net.java.dev.spellcast.utilities.LockableListModel;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.CoinmasterData;

import net.sourceforge.kolmafia.objectpool.ItemPool;

import net.sourceforge.kolmafia.persistence.CoinmastersDatabase;

public class Crimbo14Request
	extends CoinMasterRequest
{
	public static final String master = "Crimbo 2014"; 
	private static final LockableListModel<AdventureResult> buyItems = CoinmastersDatabase.getBuyItems( Crimbo14Request.master );
	private static final Map<Integer, Integer> buyPrices = CoinmastersDatabase.getBuyPrices( Crimbo14Request.master );
	private static final LockableListModel<AdventureResult> sellItems = CoinmastersDatabase.getSellItems( Crimbo14Request.master );
	private static final Map<Integer, Integer> sellPrices = CoinmastersDatabase.getSellPrices( Crimbo14Request.master );
	private static final Pattern TOKEN_PATTERN = Pattern.compile( "<td>(no|[\\d,]) Crimbo Credit", Pattern.DOTALL );
	public static final AdventureResult CRIMBO_CREDIT = ItemPool.get( ItemPool.CRIMBO_CREDIT, 1 );
	private static final Map<Integer, Integer> itemRows = CoinmastersDatabase.getRows( Crimbo14Request.master );
	public static final CoinmasterData CRIMBO14 =
		new CoinmasterData(
			Crimbo14Request.master,
			"crimbo14",
			Crimbo14Request.class,
			"Crimbo Credit",
			null,
			false,
			Crimbo14Request.TOKEN_PATTERN,
			Crimbo14Request.CRIMBO_CREDIT,
			null,
			Crimbo14Request.itemRows,
			"shop.php?whichshop=crimbo14",
			"buyitem",
			Crimbo14Request.buyItems,
			Crimbo14Request.buyPrices,
			"shop.php?whichshop=crimbo14turnin",
			"buyitem",
			Crimbo14Request.sellItems,
			Crimbo14Request.sellPrices,
			"whichrow",
			GenericRequest.WHICHROW_PATTERN,
			"quantity",
			GenericRequest.QUANTITY_PATTERN,
			null,
			null,
			true
			);

	public Crimbo14Request()
	{
		super( Crimbo14Request.CRIMBO14 );
	}

	public Crimbo14Request( final boolean buying, final AdventureResult [] attachments )
	{
		super( Crimbo14Request.CRIMBO14, buying, attachments );
	}

	public Crimbo14Request( final boolean buying, final AdventureResult attachment )
	{
		super( Crimbo14Request.CRIMBO14, buying, attachment );
	}

	public Crimbo14Request( final boolean buying, final int itemId, final int quantity )
	{
		super( Crimbo14Request.CRIMBO14, buying, itemId, quantity );
	}

	@Override
	public void processResults()
	{
		Crimbo14Request.parseResponse( this.getURLString(), this.responseText );
	}

	public static void parseResponse( final String urlString, final String responseText )
	{
		if ( !urlString.startsWith( "shop.php" ) || !urlString.contains( "whichshop=crimbo14" ) )
		{
			return;
		}

		CoinmasterData data = Crimbo14Request.CRIMBO14;

		String action = GenericRequest.getAction( urlString );
		if ( action != null )
		{
			CoinMasterRequest.parseResponse( data, urlString, responseText );
		}
	}

	public static final boolean registerRequest( final String urlString )
	{
		if ( !urlString.startsWith( "shop.php" ) || !urlString.contains( "whichshop=crimbo14" ) )
		{
			return false;
		}

		CoinmasterData data = Crimbo14Request.CRIMBO14;
		return CoinMasterRequest.registerRequest( data, urlString );
	}

	public static String accessible()
	{
		return "Crimbo Credits are no longer exchangeable";
	}
}
