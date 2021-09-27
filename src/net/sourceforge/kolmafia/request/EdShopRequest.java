package net.sourceforge.kolmafia.request;

import java.util.Map;

import java.util.regex.Pattern;

import net.java.dev.spellcast.utilities.LockableListModel;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.KoLCharacter;

import net.sourceforge.kolmafia.objectpool.ItemPool;

import net.sourceforge.kolmafia.persistence.CoinmastersDatabase;

import net.sourceforge.kolmafia.session.Limitmode;
 
public class EdShopRequest
	extends CoinMasterRequest
{
	public static final String master = "Everything Under the World";

	public static final LockableListModel<AdventureResult> buyItems = CoinmastersDatabase.getBuyItems( EdShopRequest.master );
	private static final Map<Integer, Integer> buyPrices = CoinmastersDatabase.getBuyPrices( EdShopRequest.master );
	private static final Map<Integer, Integer> itemRows = CoinmastersDatabase.getRows( EdShopRequest.master );

	private static final Pattern TOKEN_PATTERN = Pattern.compile( "<td>([\\d,]+) Ka coin" );

	public static final AdventureResult KA =  ItemPool.get( ItemPool.KA_COIN, 1 );



	public static final CoinmasterData EDSHOP =
		new CoinmasterData(
			EdShopRequest.master,
			"Everything Under the World",
			EdShopRequest.class,
			"Ka coin",
			null, // probably needs updating later
			false,
			EdShopRequest.TOKEN_PATTERN,
			EdShopRequest.KA,
			null,
			EdShopRequest.itemRows,
			"shop.php?whichshop=edunder_shopshop",
			"buyitem",
			EdShopRequest.buyItems,
			EdShopRequest.buyPrices,
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


	public EdShopRequest()
	{
		super( EdShopRequest.EDSHOP );
	}

	public EdShopRequest( final boolean buying, final AdventureResult [] attachments )
	{
		super( EdShopRequest.EDSHOP, buying, attachments );
	}

	public EdShopRequest( final boolean buying, final AdventureResult attachment )
	{
		super( EdShopRequest.EDSHOP, buying, attachment );
	}

	public EdShopRequest( final boolean buying, final int itemId, final int quantity )
	{
		super( EdShopRequest.EDSHOP, buying, itemId, quantity );
	}

	@Override
	public void processResults()
	{
		EdShopRequest.parseResponse( this.getURLString(), this.responseText );
	}

	public static void parseResponse( final String location, final String responseText )
	{
		if ( !location.startsWith( "shop.php" ) || !location.contains( "whichshop=edunder_shopshop" ) )
		{
			return;
		}

		CoinmasterData data = EdShopRequest.EDSHOP;

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
		if ( !KoLCharacter.isEd() )
		{
			return "Only Ed can come here.";
		}
		if ( KoLCharacter.getLimitmode() != Limitmode.ED )
		{
			return "You must be in the Underworld to shop here.";
		}
		return null;
	}

	public static final boolean registerRequest( final String urlString )
	{
		if ( !urlString.startsWith( "shop.php" ) || !urlString.contains( "whichshop=edunder_shopshop" ) )
		{
			return false;
		}

		CoinmasterData data = EdShopRequest.EDSHOP;
		return CoinMasterRequest.registerRequest( data, urlString, true );
	}
}
