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

public class DiscoGiftCoRequest
	extends CoinMasterRequest
{
	public static final String master = "Disco GiftCo";
	private static final LockableListModel<AdventureResult> buyItems = CoinmastersDatabase.getBuyItems( DiscoGiftCoRequest.master );
	private static final Map<Integer, Integer> buyPrices = CoinmastersDatabase.getBuyPrices( DiscoGiftCoRequest.master );
	private static final Map<Integer, Integer> itemRows = CoinmastersDatabase.getRows( DiscoGiftCoRequest.master );

	private static final Pattern TOKEN_PATTERN = Pattern.compile( "<td>([\\d,]+) Volcoino" );
	public static final AdventureResult COIN = ItemPool.get( ItemPool.VOLCOINO, 1 );
	public static final CoinmasterData DISCO_GIFTCO =
		new CoinmasterData(
			DiscoGiftCoRequest.master,
			"DiscoGiftCo",
			DiscoGiftCoRequest.class,
			"Volcoino",
			null,
			false,
			DiscoGiftCoRequest.TOKEN_PATTERN,
			DiscoGiftCoRequest.COIN,
			null,
			DiscoGiftCoRequest.itemRows,
			"shop.php?whichshop=infernodisco",
			"buyitem",
			DiscoGiftCoRequest.buyItems,
			DiscoGiftCoRequest.buyPrices,
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

	public DiscoGiftCoRequest()
	{
		super( DiscoGiftCoRequest.DISCO_GIFTCO );
	}

	public DiscoGiftCoRequest( final boolean buying, final AdventureResult [] attachments )
	{
		super( DiscoGiftCoRequest.DISCO_GIFTCO, buying, attachments );
	}

	public DiscoGiftCoRequest( final boolean buying, final AdventureResult attachment )
	{
		super( DiscoGiftCoRequest.DISCO_GIFTCO, buying, attachment );
	}

	public DiscoGiftCoRequest( final boolean buying, final int itemId, final int quantity )
	{
		super( DiscoGiftCoRequest.DISCO_GIFTCO, buying, itemId, quantity );
	}

	@Override
	public void processResults()
	{
		DiscoGiftCoRequest.parseResponse( this.getURLString(), this.responseText );
	}

	public static void parseResponse( final String urlString, final String responseText )
	{
		if ( !urlString.contains( "whichshop=infernodisco" ) )
		{
			return;
		}

		CoinmasterData data = DiscoGiftCoRequest.DISCO_GIFTCO;

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
		if ( !urlString.startsWith( "shop.php" ) || !urlString.contains( "whichshop=infernodisco" ) )
		{
			return false;
		}

		CoinmasterData data = DiscoGiftCoRequest.DISCO_GIFTCO;
		return CoinMasterRequest.registerRequest( data, urlString, true );
	}

	public static String accessible()
	{
		if ( !Preferences.getBoolean( "_hotAirportToday" ) && !Preferences.getBoolean( "hotAirportAlways" ) )
		{
			return "You don't have access to That 70s Volcano";
		}
		if ( Limitmode.limitZone( "That 70s Volcano" ) )
		{
			return "You cannot currently access That 70s Volcano";
		}
		return null;
	}
}
