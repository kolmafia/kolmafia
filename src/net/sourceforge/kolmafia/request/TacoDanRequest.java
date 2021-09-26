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

public class TacoDanRequest
	extends CoinMasterRequest
{
	public static final String master = "Taco Dan's Taco Stand";
	private static final LockableListModel<AdventureResult> buyItems = CoinmastersDatabase.getBuyItems( TacoDanRequest.master );
	private static final Map<Integer, Integer> buyPrices = CoinmastersDatabase.getBuyPrices( TacoDanRequest.master );
	private static final Map<Integer, Integer> itemRows = CoinmastersDatabase.getRows( TacoDanRequest.master );

	private static final Pattern TOKEN_PATTERN = Pattern.compile( "<td>([\\d,]+) Beach Bucks" );
	public static final AdventureResult COIN = ItemPool.get( ItemPool.BEACH_BUCK, 1 );
	public static final CoinmasterData TACO_DAN =
		new CoinmasterData(
			TacoDanRequest.master,
			"taco_dan",
			TacoDanRequest.class,
			"Beach Buck",
			null,
			false,
			TacoDanRequest.TOKEN_PATTERN,
			TacoDanRequest.COIN,
			null,
			TacoDanRequest.itemRows,
			"shop.php?whichshop=sbb_taco",
			"buyitem",
			TacoDanRequest.buyItems,
			TacoDanRequest.buyPrices,
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

	public TacoDanRequest()
	{
		super( TacoDanRequest.TACO_DAN );
	}

	public TacoDanRequest( final boolean buying, final AdventureResult [] attachments )
	{
		super( TacoDanRequest.TACO_DAN, buying, attachments );
	}

	public TacoDanRequest( final boolean buying, final AdventureResult attachment )
	{
		super( TacoDanRequest.TACO_DAN, buying, attachment );
	}

	public TacoDanRequest( final boolean buying, final int itemId, final int quantity )
	{
		super( TacoDanRequest.TACO_DAN, buying, itemId, quantity );
	}

	@Override
	public void processResults()
	{
		TacoDanRequest.parseResponse( this.getURLString(), this.responseText );
	}

	public static void parseResponse( final String urlString, final String responseText )
	{
		if ( !urlString.contains( "whichshop=sbb_taco" ) )
		{
			return;
		}

		CoinmasterData data = TacoDanRequest.TACO_DAN;

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
		if ( !urlString.startsWith( "shop.php" ) || !urlString.contains( "whichshop=sbb_taco" ) )
		{
			return false;
		}

		CoinmasterData data = TacoDanRequest.TACO_DAN;
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
