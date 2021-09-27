package net.sourceforge.kolmafia.request;

import java.util.Map;

import java.util.regex.Pattern;

import net.java.dev.spellcast.utilities.LockableListModel;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.CoinmasterData;

import net.sourceforge.kolmafia.objectpool.ItemPool;

import net.sourceforge.kolmafia.persistence.CoinmastersDatabase;

import net.sourceforge.kolmafia.preferences.Preferences;

public class SpacegateFabricationRequest
	extends CoinMasterRequest
{
	public static final String master = "Spacegate Fabrication Facility";

	private static final LockableListModel<AdventureResult> buyItems = CoinmastersDatabase.getBuyItems( SpacegateFabricationRequest.master );
	private static final Map<Integer, Integer> buyPrices = CoinmastersDatabase.getBuyPrices( SpacegateFabricationRequest.master );
	private static final Map<Integer, Integer> itemRows = CoinmastersDatabase.getRows( SpacegateFabricationRequest.master );
	private static final Pattern RESEARCH_PATTERN = Pattern.compile( "([\\d,]+) pages? of Spacegate Research" );
	public static final AdventureResult RESEARCH = ItemPool.get( ItemPool.SPACEGATE_RESEARCH, 1 );

	public static final CoinmasterData SPACEGATE_STORE =
		new CoinmasterData(
			SpacegateFabricationRequest.master,
			"spacegate",
			SpacegateFabricationRequest.class,
			"Spacegate Research",
			"no pages of Spacegate Research",
			false,
			SpacegateFabricationRequest.RESEARCH_PATTERN,
			SpacegateFabricationRequest.RESEARCH,
			null,
			SpacegateFabricationRequest.itemRows,
			"shop.php?whichshop=spacegate",
			"buyitem",
			SpacegateFabricationRequest.buyItems,
			SpacegateFabricationRequest.buyPrices,
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

	public SpacegateFabricationRequest()
	{
		super( SpacegateFabricationRequest.SPACEGATE_STORE );
	}

	public SpacegateFabricationRequest( final boolean buying, final AdventureResult [] attachments )
	{
		super( SpacegateFabricationRequest.SPACEGATE_STORE, buying, attachments );
	}

	public SpacegateFabricationRequest( final boolean buying, final AdventureResult attachment )
	{
		super( SpacegateFabricationRequest.SPACEGATE_STORE, buying, attachment );
	}

	public SpacegateFabricationRequest( final boolean buying, final int itemId, final int quantity )
	{
		super( SpacegateFabricationRequest.SPACEGATE_STORE, buying, itemId, quantity );
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
		SpacegateFabricationRequest.parseResponse( this.getURLString(), this.responseText );
	}

	public static void parseResponse( final String location, final String responseText )
	{
		if ( !location.contains( "whichshop=spacegate" ) )
		{
			return;
		}

		CoinmasterData data = SpacegateFabricationRequest.SPACEGATE_STORE;

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
		if ( !Preferences.getBoolean( "_spacegateToday" ) && !Preferences.getBoolean( "spacegateAlways" ) )
		{
			return "You can't get to the Spacegate.";
		}
		return null;
	}

	public static final boolean registerRequest( final String urlString )
	{
		if ( !urlString.startsWith( "shop.php" ) || !urlString.contains( "whichshop=spacegate" ) )
		{
			return false;
		}

		CoinmasterData data = SpacegateFabricationRequest.SPACEGATE_STORE;
		return CoinMasterRequest.registerRequest( data, urlString, true );
	}
}
