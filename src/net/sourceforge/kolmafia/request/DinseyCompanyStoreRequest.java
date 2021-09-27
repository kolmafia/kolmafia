package net.sourceforge.kolmafia.request;

import java.util.Map;

import java.util.regex.Pattern;

import net.java.dev.spellcast.utilities.LockableListModel;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.CoinmasterData;

import net.sourceforge.kolmafia.objectpool.ItemPool;

import net.sourceforge.kolmafia.persistence.CoinmastersDatabase;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.session.Limitmode;

public class DinseyCompanyStoreRequest
	extends CoinMasterRequest
{
	public static final String master = "The Dinsey Company Store";
	private static final LockableListModel<AdventureResult> buyItems = CoinmastersDatabase.getBuyItems( DinseyCompanyStoreRequest.master );
	private static final Map<Integer, Integer> buyPrices = CoinmastersDatabase.getBuyPrices( DinseyCompanyStoreRequest.master );
	private static final Map<Integer, Integer> itemRows = CoinmastersDatabase.getRows( DinseyCompanyStoreRequest.master );

	private static final Pattern TOKEN_PATTERN = Pattern.compile( "<td>([\\d,]+) FunFunds" );
	public static final AdventureResult COIN = ItemPool.get( ItemPool.FUNFUNDS, 1 );
	public static final CoinmasterData DINSEY_COMPANY_STORE =
		new CoinmasterData(
			DinseyCompanyStoreRequest.master,
			"DinsyStore",
			DinseyCompanyStoreRequest.class,
			"FunFunds&trade;",
			null,
			false,
			DinseyCompanyStoreRequest.TOKEN_PATTERN,
			DinseyCompanyStoreRequest.COIN,
			null,
			DinseyCompanyStoreRequest.itemRows,
			"shop.php?whichshop=landfillstore",
			"buyitem",
			DinseyCompanyStoreRequest.buyItems,
			DinseyCompanyStoreRequest.buyPrices,
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

	static
	{
		DINSEY_COMPANY_STORE.plural = "FunFunds&trade;";
	}

	public DinseyCompanyStoreRequest()
	{
		super( DinseyCompanyStoreRequest.DINSEY_COMPANY_STORE );
	}

	public DinseyCompanyStoreRequest( final boolean buying, final AdventureResult [] attachments )
	{
		super( DinseyCompanyStoreRequest.DINSEY_COMPANY_STORE, buying, attachments );
	}

	public DinseyCompanyStoreRequest( final boolean buying, final AdventureResult attachment )
	{
		super( DinseyCompanyStoreRequest.DINSEY_COMPANY_STORE, buying, attachment );
	}

	public DinseyCompanyStoreRequest( final boolean buying, final int itemId, final int quantity )
	{
		super( DinseyCompanyStoreRequest.DINSEY_COMPANY_STORE, buying, itemId, quantity );
	}

	@Override
	public void processResults()
	{
		DinseyCompanyStoreRequest.parseResponse( this.getURLString(), this.responseText );
	}

	public static void parseResponse( final String urlString, final String responseText )
	{
		if ( !urlString.contains( "whichshop=landfillstore" ) )
		{
			return;
		}

		CoinmasterData data = DinseyCompanyStoreRequest.DINSEY_COMPANY_STORE;

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
		if ( !urlString.startsWith( "shop.php" ) || !urlString.contains( "whichshop=landfillstore" ) )
		{
			return false;
		}

		CoinmasterData data = DinseyCompanyStoreRequest.DINSEY_COMPANY_STORE;
		return CoinMasterRequest.registerRequest( data, urlString, true );
	}

	public static String accessible()
	{
		if ( !Preferences.getBoolean( "_stenchAirportToday" ) && !Preferences.getBoolean( "stenchAirportAlways" ) )
		{
			return "You don't have access to Dinseylandfill";
		}
		if ( Limitmode.limitZone( "Dinseylandfill" ) )
		{
			return "You cannot currently access Dinseylandfill";
		}
		return null;
	}
}
