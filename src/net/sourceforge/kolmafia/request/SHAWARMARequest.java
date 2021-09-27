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

public class SHAWARMARequest
	extends CoinMasterRequest
{
	public static final String master = "The SHAWARMA Initiative";
	private static final LockableListModel<AdventureResult> buyItems = CoinmastersDatabase.getBuyItems( SHAWARMARequest.master );
	private static final Map<Integer, Integer> buyPrices = CoinmastersDatabase.getBuyPrices( SHAWARMARequest.master );
	private static final Map<Integer, Integer> itemRows = CoinmastersDatabase.getRows( SHAWARMARequest.master );

	private static final Pattern TOKEN_PATTERN = Pattern.compile( "<td>([\\d,]+) Coins-spiracy" );
	public static final AdventureResult COIN = ItemPool.get( ItemPool.COINSPIRACY, 1 );
	public static final CoinmasterData SHAWARMA =
		new CoinmasterData(
			SHAWARMARequest.master,
			"SHAWARMA",
			SHAWARMARequest.class,
			"Coinspiracy",
			null,
			false,
			SHAWARMARequest.TOKEN_PATTERN,
			SHAWARMARequest.COIN,
			null,
			SHAWARMARequest.itemRows,
			"shop.php?whichshop=si_shop1",
			"buyitem",
			SHAWARMARequest.buyItems,
			SHAWARMARequest.buyPrices,
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

	public SHAWARMARequest()
	{
		super( SHAWARMARequest.SHAWARMA );
	}

	public SHAWARMARequest( final boolean buying, final AdventureResult [] attachments )
	{
		super( SHAWARMARequest.SHAWARMA, buying, attachments );
	}

	public SHAWARMARequest( final boolean buying, final AdventureResult attachment )
	{
		super( SHAWARMARequest.SHAWARMA, buying, attachment );
	}

	public SHAWARMARequest( final boolean buying, final int itemId, final int quantity )
	{
		super( SHAWARMARequest.SHAWARMA, buying, itemId, quantity );
	}

	@Override
	public void processResults()
	{
		SHAWARMARequest.parseResponse( this.getURLString(), this.responseText );
	}

	public static void parseResponse( final String urlString, final String responseText )
	{
		if ( !urlString.contains( "whichshop=si_shop1" ) )
		{
			return;
		}

		CoinmasterData data = SHAWARMARequest.SHAWARMA;

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
		if ( !urlString.startsWith( "shop.php" ) || !urlString.contains( "whichshop=si_shop1" ) )
		{
			return false;
		}

		CoinmasterData data = SHAWARMARequest.SHAWARMA;
		return CoinMasterRequest.registerRequest( data, urlString, true );
	}

	public static String accessible()
	{
		if ( !Preferences.getBoolean( "_spookyAirportToday" ) && !Preferences.getBoolean( "spookyAirportAlways" ) )
		{
			return "You don't have access to Conspiracy Island";
		}
		if ( Limitmode.limitZone( "Conspiracy Island" ) )
		{
			return "You cannot currently access Conspiracy Island";
		}
		if ( !Preferences.getBoolean( "SHAWARMAInitiativeUnlocked" ) )
		{
			return "SHAWARMA Initiative is locked";
		}
		return null;
	}
}
