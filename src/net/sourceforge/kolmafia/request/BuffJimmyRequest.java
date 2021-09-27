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

public class BuffJimmyRequest
	extends CoinMasterRequest
{
	public static final String master = "Buff Jimmy's Souvenir Shop";
	private static final LockableListModel<AdventureResult> buyItems = CoinmastersDatabase.getBuyItems( BuffJimmyRequest.master );
	private static final Map<Integer, Integer> buyPrices = CoinmastersDatabase.getBuyPrices( BuffJimmyRequest.master );
	private static final Map<Integer, Integer> itemRows = CoinmastersDatabase.getRows( BuffJimmyRequest.master );

	private static final Pattern TOKEN_PATTERN = Pattern.compile( "<td>([\\d,]+) Beach Bucks" );
	public static final AdventureResult COIN = ItemPool.get( ItemPool.BEACH_BUCK, 1 );
	public static final CoinmasterData BUFF_JIMMY =
		new CoinmasterData(
			BuffJimmyRequest.master,
			"BuffJimmy",
			BuffJimmyRequest.class,
			"Beach Buck",
			null,
			false,
			BuffJimmyRequest.TOKEN_PATTERN,
			BuffJimmyRequest.COIN,
			null,
			BuffJimmyRequest.itemRows,
			"shop.php?whichshop=sbb_jimmy",
			"buyitem",
			BuffJimmyRequest.buyItems,
			BuffJimmyRequest.buyPrices,
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

	public BuffJimmyRequest()
	{
		super( BuffJimmyRequest.BUFF_JIMMY );
	}

	public BuffJimmyRequest( final boolean buying, final AdventureResult [] attachments )
	{
		super( BuffJimmyRequest.BUFF_JIMMY, buying, attachments );
	}

	public BuffJimmyRequest( final boolean buying, final AdventureResult attachment )
	{
		super( BuffJimmyRequest.BUFF_JIMMY, buying, attachment );
	}

	public BuffJimmyRequest( final boolean buying, final int itemId, final int quantity )
	{
		super( BuffJimmyRequest.BUFF_JIMMY, buying, itemId, quantity );
	}

	@Override
	public void processResults()
	{
		BuffJimmyRequest.parseResponse( this.getURLString(), this.responseText );
	}

	public static void parseResponse( final String urlString, final String responseText )
	{
		if ( !urlString.contains( "whichshop=sbb_jimmy" ) )
		{
			return;
		}

		CoinmasterData data = BuffJimmyRequest.BUFF_JIMMY;

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
		if ( !urlString.startsWith( "shop.php" ) || !urlString.contains( "whichshop=sbb_jimmy" ) )
		{
			return false;
		}

		CoinmasterData data = BuffJimmyRequest.BUFF_JIMMY;
		return CoinMasterRequest.registerRequest( data, urlString, true );
	}

	public static String accessible()
	{
		if ( !Preferences.getBoolean( "_sleazeAirportToday" ) && !Preferences.getBoolean( "sleazeAirportAlways" ) )
		{
			return "You don't have access to Spring Break Beach";
		}
		if ( Limitmode.limitZone( "Spring Break Beach" ) )
		{
			return "You cannot currently access Spring Break Beach";
		}
		return null;
	}
}
