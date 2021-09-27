package net.sourceforge.kolmafia.request;

import java.util.Map;
import java.util.regex.Pattern;

import net.java.dev.spellcast.utilities.LockableListModel;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.CoinmasterData;

import net.sourceforge.kolmafia.objectpool.ItemPool;

import net.sourceforge.kolmafia.persistence.CoinmastersDatabase;

public class GMartRequest
	extends CoinMasterRequest
{
	public static final String master = "G-Mart";

	private static final LockableListModel<AdventureResult> buyItems = CoinmastersDatabase.getBuyItems( GMartRequest.master );
	private static final Map<Integer, Integer> buyPrices = CoinmastersDatabase.getBuyPrices( GMartRequest.master );
	private static final Map<Integer, Integer> itemRows = CoinmastersDatabase.getRows( GMartRequest.master );
	private static final Pattern G_PATTERN = Pattern.compile( "([\\d,]+) G" );
	public static final AdventureResult G = ItemPool.get( ItemPool.G, 1 );

	public static final CoinmasterData GMART =
		new CoinmasterData(
			GMartRequest.master,
			"glover",
			GMartRequest.class,
			"G",
			"no Gs",
			false,
			GMartRequest.G_PATTERN,
			GMartRequest.G,
			null,
			GMartRequest.itemRows,
			"shop.php?whichshop=glover",
			"buyitem",
			GMartRequest.buyItems,
			GMartRequest.buyPrices,
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

	public GMartRequest()
	{
		super( GMartRequest.GMART );
	}

	public GMartRequest( final boolean buying, final AdventureResult [] attachments )
	{
		super( GMartRequest.GMART, buying, attachments );
	}

	public GMartRequest( final boolean buying, final AdventureResult attachment )
	{
		super( GMartRequest.GMART, buying, attachment );
	}

	public GMartRequest( final boolean buying, final int itemId, final int quantity )
	{
		super( GMartRequest.GMART, buying, itemId, quantity );
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
		GMartRequest.parseResponse( this.getURLString(), this.responseText );
	}

	public static void parseResponse( final String location, final String responseText )
	{
		if ( !location.contains( "whichshop=glover" ) )
		{
			return;
		}

		CoinmasterData data = GMartRequest.GMART;

		String action = GenericRequest.getAction( location );
		if ( action != null )
		{
			CoinMasterRequest.parseResponse( data, location, responseText );
			return;
		}

		// Parse current coin balances
		CoinMasterRequest.parseBalance( data, responseText );
	}

	public static String accessible()
	{
		return null;
	}

	public static final boolean registerRequest( final String urlString )
	{
		if ( !urlString.startsWith( "shop.php" ) || !urlString.contains( "whichshop=glover" ) )
		{
			return false;
		}

		CoinmasterData data = GMartRequest.GMART;
		return CoinMasterRequest.registerRequest( data, urlString, true );
	}
}
