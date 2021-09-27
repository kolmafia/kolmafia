package net.sourceforge.kolmafia.request;

import java.util.Map;

import java.util.regex.Pattern;

import net.java.dev.spellcast.utilities.LockableListModel;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.RequestThread;

import net.sourceforge.kolmafia.objectpool.ItemPool;

import net.sourceforge.kolmafia.persistence.CoinmastersDatabase;

public class FreeSnackRequest
	extends CoinMasterRequest
{
	public static final String master = "Game Shoppe Snacks"; 
	private static final LockableListModel<AdventureResult> buyItems = CoinmastersDatabase.getBuyItems( FreeSnackRequest.master );
	private static final Map<Integer, Integer> buyPrices = CoinmastersDatabase.getBuyPrices( FreeSnackRequest.master );

	private static final Pattern TOKEN_PATTERN = Pattern.compile( "You have ([\\d,]+) free snack voucher" );
	private static final Pattern SNACK_PATTERN = Pattern.compile( "whichsnack=(\\d+)" );
	public static final AdventureResult VOUCHER = ItemPool.get( ItemPool.SNACK_VOUCHER, 1 );

	public static final CoinmasterData FREESNACKS =
		new CoinmasterData(
			FreeSnackRequest.master,
			"snacks",
			FreeSnackRequest.class,
			"snack voucher",
			"The teen glances at your snack voucher",
			true,
			FreeSnackRequest.TOKEN_PATTERN,
			FreeSnackRequest.VOUCHER,
			null,
			null,
			"gamestore.php",
			"buysnack",
			FreeSnackRequest.buyItems,
			FreeSnackRequest.buyPrices,
			null,
			null,
			null,
			null,
			"whichsnack",
			FreeSnackRequest.SNACK_PATTERN,
			null,
			null,
			null,
			null,
			true
			);

	public FreeSnackRequest()
	{
		super( FreeSnackRequest.FREESNACKS );
	}

	public FreeSnackRequest( final boolean buying, final AdventureResult [] attachments )
	{
		super( FreeSnackRequest.FREESNACKS, buying, attachments );
	}

	public FreeSnackRequest( final boolean buying, final AdventureResult attachment )
	{
		super( FreeSnackRequest.FREESNACKS, buying, attachment );
	}

	public FreeSnackRequest( final boolean buying, final int itemId, final int quantity )
	{
		super( FreeSnackRequest.FREESNACKS, buying, itemId, quantity );
	}

	@Override
	public void processResults()
	{
		GameShoppeRequest.parseResponse( this.getURLString(), this.responseText );
	}

	public static void parseFreeSnackVisit( final String location, final String responseText )
	{
		if ( responseText.indexOf( "You acquire" ) != -1 )
		{
			CoinmasterData data = FreeSnackRequest.FREESNACKS;
			CoinMasterRequest.completePurchase( data, location );
		}
	}

	public static final void buy( final int itemId, final int count )
	{
		RequestThread.postRequest( new FreeSnackRequest( true, itemId, count ) );
	}

	public static String accessible()
	{
		return null;
	}

	public static final boolean registerRequest( final String urlString )
	{
		// We only claim gamestore.php?action=buysnack
		if ( !urlString.startsWith( "gamestore.php" ) )
		{
			return false;
		}

		CoinmasterData data = FreeSnackRequest.FREESNACKS;
		return CoinMasterRequest.registerRequest( data, urlString );
	}
}
