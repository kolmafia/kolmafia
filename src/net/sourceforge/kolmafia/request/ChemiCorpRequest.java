package net.sourceforge.kolmafia.request;

import java.util.Map;

import java.util.regex.Pattern;

import net.java.dev.spellcast.utilities.LockableListModel;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.KoLCharacter;

import net.sourceforge.kolmafia.objectpool.IntegerPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;

import net.sourceforge.kolmafia.persistence.CoinmastersDatabase;

import net.sourceforge.kolmafia.session.BatManager;
import net.sourceforge.kolmafia.session.InventoryManager;
import net.sourceforge.kolmafia.session.Limitmode;

public class ChemiCorpRequest
	extends CoinMasterRequest
{
	public static final String master = "ChemiCorp";
	private static final LockableListModel<AdventureResult> buyItems = CoinmastersDatabase.getBuyItems( ChemiCorpRequest.master );
	private static final Map<Integer, Integer> buyPrices = CoinmastersDatabase.getBuyPrices( ChemiCorpRequest.master );
	private static final Map<Integer, Integer> itemRows = CoinmastersDatabase.getRows( ChemiCorpRequest.master );

	private static final Pattern TOKEN_PATTERN = Pattern.compile( "<td>([\\d,]+) dangerous chemicals" );
	public static final AdventureResult COIN = ItemPool.get( ItemPool.DANGEROUS_CHEMICALS, 1 );
	public static final CoinmasterData CHEMICORP =
		new CoinmasterData(
			ChemiCorpRequest.master,
			"ChemiCorp",
			ChemiCorpRequest.class,
			"dangerous chemicals",
			null,
			false,
			ChemiCorpRequest.TOKEN_PATTERN,
			ChemiCorpRequest.COIN,
			null,
			ChemiCorpRequest.itemRows,
			"shop.php?whichshop=batman_chemicorp",
			"buyitem",
			ChemiCorpRequest.buyItems,
			ChemiCorpRequest.buyPrices,
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
			)
		{
			@Override
			public AdventureResult itemBuyPrice( final int itemId )
			{
				int price = ChemiCorpRequest.buyPrices.get( IntegerPool.get( itemId ) );
				if ( price == 1 )
				{
					return ChemiCorpRequest.COIN;
				}
				// price increased by 3 each time you buy one
				int count = InventoryManager.getCount( itemId );
				if ( count > 0 )
				{
					price = 3 * ( count + 1 );
				}
				return ChemiCorpRequest.COIN.getInstance( price );
			}
		};

	public ChemiCorpRequest()
	{
		super( ChemiCorpRequest.CHEMICORP );
	}

	public ChemiCorpRequest( final boolean buying, final AdventureResult [] attachments )
	{
		super( ChemiCorpRequest.CHEMICORP, buying, attachments );
	}

	public ChemiCorpRequest( final boolean buying, final AdventureResult attachment )
	{
		super( ChemiCorpRequest.CHEMICORP, buying, attachment );
	}

	public ChemiCorpRequest( final boolean buying, final int itemId, final int quantity )
	{
		super( ChemiCorpRequest.CHEMICORP, buying, itemId, quantity );
	}

	@Override
	public void processResults()
	{
		ChemiCorpRequest.parseResponse( this.getURLString(), this.responseText );
	}

	public static void parseResponse( final String urlString, final String responseText )
	{
		if ( !urlString.contains( "whichshop=batman_chemicorp" ) )
		{
			return;
		}

		CoinmasterData data = ChemiCorpRequest.CHEMICORP;

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
		if ( !urlString.startsWith( "shop.php" ) || !urlString.contains( "whichshop=batman_chemicorp" ) )
		{
			return false;
		}

		CoinmasterData data = ChemiCorpRequest.CHEMICORP;
		return CoinMasterRequest.registerRequest( data, urlString, true );
	}

	public static String accessible()
	{
		if ( KoLCharacter.getLimitmode() != Limitmode.BATMAN )
		{
			return "Only Batfellow can go to ChemiCorp.";
		}
		if ( BatManager.currentBatZone() != BatManager.DOWNTOWN )
		{
			return "Batfellow can only visit ChemiCorp while Downtown.";
		}
		return null;
	}
}
