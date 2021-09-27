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

public class BrogurtRequest
	extends CoinMasterRequest
{
	public static final String master = "The Frozen Brogurt Stand";
	private static final LockableListModel<AdventureResult> buyItems = CoinmastersDatabase.getBuyItems( BrogurtRequest.master );
	private static final Map<Integer, Integer> buyPrices = CoinmastersDatabase.getBuyPrices( BrogurtRequest.master );
	private static final Map<Integer, Integer> itemRows = CoinmastersDatabase.getRows( BrogurtRequest.master );

	private static final Pattern TOKEN_PATTERN = Pattern.compile( "<td>([\\d,]+) Beach Bucks" );
	public static final AdventureResult COIN = ItemPool.get( ItemPool.BEACH_BUCK, 1 );
	public static final CoinmasterData BROGURT =
		new CoinmasterData(
			BrogurtRequest.master,
			"brogurt",
			BrogurtRequest.class,
			"Beach Buck",
			null,
			false,
			BrogurtRequest.TOKEN_PATTERN,
			BrogurtRequest.COIN,
			null,
			BrogurtRequest.itemRows,
			"shop.php?whichshop=sbb_brogurt",
			"buyitem",
			BrogurtRequest.buyItems,
			BrogurtRequest.buyPrices,
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

	public BrogurtRequest()
	{
		super( BrogurtRequest.BROGURT );
	}

	public BrogurtRequest( final boolean buying, final AdventureResult [] attachments )
	{
		super( BrogurtRequest.BROGURT, buying, attachments );
	}

	public BrogurtRequest( final boolean buying, final AdventureResult attachment )
	{
		super( BrogurtRequest.BROGURT, buying, attachment );
	}

	public BrogurtRequest( final boolean buying, final int itemId, final int quantity )
	{
		super( BrogurtRequest.BROGURT, buying, itemId, quantity );
	}

	@Override
	public void processResults()
	{
		BrogurtRequest.parseResponse( this.getURLString(), this.responseText );
	}

	public static void parseResponse( final String urlString, final String responseText )
	{
		if ( !urlString.contains( "whichshop=sbb_brogurt" ) )
		{
			return;
		}

		CoinmasterData data = BrogurtRequest.BROGURT;

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
		if ( !urlString.startsWith( "shop.php" ) || !urlString.contains( "whichshop=sbb_brogurt" ) )
		{
			return false;
		}

		CoinmasterData data = BrogurtRequest.BROGURT;
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
