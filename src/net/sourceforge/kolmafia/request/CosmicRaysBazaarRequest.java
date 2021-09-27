package net.sourceforge.kolmafia.request;

import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import net.java.dev.spellcast.utilities.LockableListModel;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.AdventureResult.AdventureLongCountResult;
import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.KoLCharacter;

import net.sourceforge.kolmafia.objectpool.IntegerPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;

import net.sourceforge.kolmafia.persistence.CoinmastersDatabase;

public class CosmicRaysBazaarRequest
	extends CoinMasterRequest
{
	public static final String master = "Cosmic Ray's Bazaar"; 
	private static final LockableListModel<AdventureResult> buyItems = CoinmastersDatabase.getBuyItems( CosmicRaysBazaarRequest.master );
	private static final Map<Integer, Integer> buyPrices = CoinmastersDatabase.getNewMap();
	private static final Map<Integer, Integer> itemRows = CoinmastersDatabase.getRows( CosmicRaysBazaarRequest.master );

	public static final AdventureResult RARE_MEAT_ISOTOPE = ItemPool.get( ItemPool.RARE_MEAT_ISOTOPE, 1 );
	public static final AdventureResult WHITE_PIXEL = ItemPool.get( ItemPool.WHITE_PIXEL, 1 );
	public static final AdventureResult FAT_LOOT_TOKEN = ItemPool.get( ItemPool.FAT_LOOT_TOKEN, 1 );
	public static final AdventureResult MEAT = new AdventureLongCountResult( AdventureResult.MEAT, 1 ) {
			@Override
			public String toString()
			{
				return this.getCount() + " Meat";
			}
			@Override
			public String getPluralName( int price )
			{
				return "Meat";
			}
		};

	public static final CoinmasterData COSMIC_RAYS_BAZAAR =
		new CoinmasterData(
			CosmicRaysBazaarRequest.master,
			"exploathing",
			CosmicRaysBazaarRequest.class,
			null,
			null,
			false,
			null,
			null,
			null,
			CosmicRaysBazaarRequest.itemRows,
			"shop.php?whichshop=exploathing",
			"buyitem",
			CosmicRaysBazaarRequest.buyItems,
			CosmicRaysBazaarRequest.buyPrices,
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
				return CosmicRaysBazaarRequest.buyCosts.get( IntegerPool.get( itemId ) );
			}
		};

	// Since there are four different currencies, we need to have a map from
	// itemId to item/count of currency; an AdventureResult.
	private static final Map<Integer, AdventureResult> buyCosts = new TreeMap<Integer, AdventureResult>();

	// Manually set up the map and change the currency, as need
	static
	{
		for ( Entry<Integer, Integer> entry : CoinmastersDatabase.getBuyPrices( CosmicRaysBazaarRequest.master ).entrySet() )
		{
			int itemId = entry.getKey().intValue();
			int price = entry.getValue().intValue();
			AdventureResult cost = null;
			switch ( itemId )
			{
			default:
				cost = RARE_MEAT_ISOTOPE.getInstance( price );
				break;
			case ItemPool.DIGITAL_KEY:
				cost = WHITE_PIXEL.getInstance( price );
				break;
			case ItemPool.BORIS_KEY:
			case ItemPool.JARLSBERG_KEY:
			case ItemPool.SNEAKY_PETE_KEY:
				cost = FAT_LOOT_TOKEN.getInstance( price );
				break;
			case ItemPool.RARE_MEAT_ISOTOPE:
				cost = MEAT.getInstance( price );
				break;
			}
			buyCosts.put( itemId, cost );
		}
	}

	public CosmicRaysBazaarRequest()
	{
		super( CosmicRaysBazaarRequest.COSMIC_RAYS_BAZAAR );
	}

	public CosmicRaysBazaarRequest( final String action )
	{
		super( CosmicRaysBazaarRequest.COSMIC_RAYS_BAZAAR, action );
	}

	public CosmicRaysBazaarRequest( final boolean buying, final AdventureResult [] attachments )
	{
		super( CosmicRaysBazaarRequest.COSMIC_RAYS_BAZAAR, buying, attachments );
	}

	public CosmicRaysBazaarRequest( final boolean buying, final AdventureResult attachment )
	{
		super( CosmicRaysBazaarRequest.COSMIC_RAYS_BAZAAR, buying, attachment );
	}

	public CosmicRaysBazaarRequest( final boolean buying, final int itemId, final int quantity )
	{
		super( CosmicRaysBazaarRequest.COSMIC_RAYS_BAZAAR, buying, itemId, quantity );
	}

	@Override
	public void run()
	{
		if ( this.action != null )
		{
			this.addFormField( "pwd" );
		}

		super.run();
	}

	@Override
	public void processResults()
	{
		CosmicRaysBazaarRequest.parseResponse( this.getURLString(), this.responseText );
	}

	public static void parseResponse( final String location, final String responseText )
	{
		if ( !location.contains( "whichshop=exploathing" ) )
		{
			return;
		}

		CoinmasterData data = CosmicRaysBazaarRequest.COSMIC_RAYS_BAZAAR;

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
		if ( !KoLCharacter.isKingdomOfExploathing() )
		{
			return "The Kingdom is not Exploathing";
		}

		return null;
	}

	public static boolean registerRequest( final String urlString )
	{
		if ( !urlString.startsWith( "shop.php" ) || !urlString.contains( "whichshop=exploathing" ) )
		{
			return false;
		}

		CoinmasterData data = CosmicRaysBazaarRequest.COSMIC_RAYS_BAZAAR;
		return CoinMasterRequest.registerRequest( data, urlString, true );
	}
}
