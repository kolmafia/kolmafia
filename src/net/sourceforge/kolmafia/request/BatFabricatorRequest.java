package net.sourceforge.kolmafia.request;

import java.util.Map;

import net.java.dev.spellcast.utilities.LockableListModel;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.KoLCharacter;

import net.sourceforge.kolmafia.objectpool.ItemPool;

import net.sourceforge.kolmafia.persistence.CoinmastersDatabase;

import net.sourceforge.kolmafia.session.BatManager;
import net.sourceforge.kolmafia.session.Limitmode;

public class BatFabricatorRequest
	extends CoinMasterRequest
{
	public static final String master = "Bat-Fabricator";
	private static final LockableListModel<AdventureResult> buyItems = CoinmastersDatabase.getBuyItems( BatFabricatorRequest.master );
	private static final Map<Integer, Integer> buyPrices = CoinmastersDatabase.getBuyPrices( BatFabricatorRequest.master );
	private static final Map<Integer, Integer> itemRows = CoinmastersDatabase.getRows( BatFabricatorRequest.master );

	public static final AdventureResult METAL = ItemPool.get( ItemPool.HIGH_GRADE_METAL, 1 );
	public static final AdventureResult FIBERS = ItemPool.get( ItemPool.HIGH_TENSILE_STRENGTH_FIBERS, 1 );
	public static final AdventureResult EXPLOSIVES = ItemPool.get( ItemPool.HIGH_GRADE_EXPLOSIVES, 1 );

	public static final CoinmasterData BAT_FABRICATOR =
		new CoinmasterData(
			BatFabricatorRequest.master,
			"Bat-Fabricator",
			BatFabricatorRequest.class,
			null,
			null,
			false,
			null,
			null,
			null,
			BatFabricatorRequest.itemRows,
			"shop.php?whichshop=batman_cave",
			"buyitem",
			BatFabricatorRequest.buyItems,
			BatFabricatorRequest.buyPrices,
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
				int cost = BatManager.hasUpgrade( BatManager.IMPROVED_3D_BAT_PRINTER ) ? 2 : 3;
				switch ( itemId )
				{
				case ItemPool.BAT_OOMERANG:
					return BatFabricatorRequest.METAL.getInstance( cost );
				case ItemPool.BAT_JUTE:
					return BatFabricatorRequest.FIBERS.getInstance( cost );
				case ItemPool.BAT_O_MITE:
					return BatFabricatorRequest.EXPLOSIVES.getInstance( cost );
				}
				return null;
			}
		};

	public BatFabricatorRequest()
	{
		super( BatFabricatorRequest.BAT_FABRICATOR );
	}

	public BatFabricatorRequest( final boolean buying, final AdventureResult [] attachments )
	{
		super( BatFabricatorRequest.BAT_FABRICATOR, buying, attachments );
	}

	public BatFabricatorRequest( final boolean buying, final AdventureResult attachment )
	{
		super( BatFabricatorRequest.BAT_FABRICATOR, buying, attachment );
	}

	public BatFabricatorRequest( final boolean buying, final int itemId, final int quantity )
	{
		super( BatFabricatorRequest.BAT_FABRICATOR, buying, itemId, quantity );
	}

	@Override
	public void processResults()
	{
		BatFabricatorRequest.parseResponse( this.getURLString(), this.responseText );
	}

	public static void parseResponse( final String urlString, final String responseText )
	{
		if ( !urlString.contains( "whichshop=batman_cave" ) )
		{
			return;
		}

		CoinmasterData data = BatFabricatorRequest.BAT_FABRICATOR;

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
		if ( !urlString.startsWith( "shop.php" ) || !urlString.contains( "whichshop=batman_cave" ) )
		{
			return false;
		}

		CoinmasterData data = BatFabricatorRequest.BAT_FABRICATOR;
		return CoinMasterRequest.registerRequest( data, urlString, true );
	}

	public static String accessible()
	{
		if ( KoLCharacter.getLimitmode() != Limitmode.BATMAN )
		{
			return "Only Batfellow can use the Bat-Fabricator.";
		}
		if ( BatManager.currentBatZone() != BatManager.BAT_CAVERN )
		{
			return "Batfellow can only use the Bat-Fabricator in the BatCavern.";
		}
		return null;
	}
}
