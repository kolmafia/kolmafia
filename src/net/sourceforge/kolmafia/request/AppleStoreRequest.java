package net.sourceforge.kolmafia.request;

import java.util.Map;

import java.util.regex.Pattern;

import net.java.dev.spellcast.utilities.LockableListModel;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.CoinmasterData;

import net.sourceforge.kolmafia.objectpool.ItemPool;

import net.sourceforge.kolmafia.persistence.CoinmastersDatabase;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.session.QuestManager;

public class AppleStoreRequest
	extends CoinMasterRequest
{
	public static final String master = "The Applecalypse Store";

	private static final LockableListModel<AdventureResult> buyItems = CoinmastersDatabase.getBuyItems( AppleStoreRequest.master );
	private static final Map<Integer, Integer> buyPrices = CoinmastersDatabase.getBuyPrices( AppleStoreRequest.master );
	private static final Map<Integer, Integer> itemRows = CoinmastersDatabase.getRows( AppleStoreRequest.master );
	private static final Pattern CHRONER_PATTERN = Pattern.compile( "([\\d,]+) Chroner" );
	public static final AdventureResult CHRONER = ItemPool.get( ItemPool.CHRONER, 1 );

	public static final CoinmasterData APPLE_STORE =
		new CoinmasterData(
			AppleStoreRequest.master,
			"applestore",
			AppleStoreRequest.class,
			"Chroner",
			"no Chroner",
			false,
			AppleStoreRequest.CHRONER_PATTERN,
			AppleStoreRequest.CHRONER,
			null,
			AppleStoreRequest.itemRows,
			"shop.php?whichshop=applestore",
			"buyitem",
			AppleStoreRequest.buyItems,
			AppleStoreRequest.buyPrices,
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

	public AppleStoreRequest()
	{
		super( AppleStoreRequest.APPLE_STORE );
	}

	public AppleStoreRequest( final boolean buying, final AdventureResult [] attachments )
	{
		super( AppleStoreRequest.APPLE_STORE, buying, attachments );
	}

	public AppleStoreRequest( final boolean buying, final AdventureResult attachment )
	{
		super( AppleStoreRequest.APPLE_STORE, buying, attachment );
	}

	public AppleStoreRequest( final boolean buying, final int itemId, final int quantity )
	{
		super( AppleStoreRequest.APPLE_STORE, buying, itemId, quantity );
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
		AppleStoreRequest.parseResponse( this.getURLString(), this.responseText );
	}

	public static void parseResponse( final String location, final String responseText )
	{
		if ( !location.contains( "whichshop=applestore" ) )
		{
			return;
		}

		if ( responseText.contains( "That store isn't there anymore." ) )
		{
			QuestManager.handleTimeTower( false );
			return;
		}
		
		QuestManager.handleTimeTower( true );

		CoinmasterData data = AppleStoreRequest.APPLE_STORE;

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
		if ( !Preferences.getBoolean( "timeTowerAvailable" ) )
		{
			return "You can't get to The Applecalypse Store";
		}
		return null;
	}

	public static final boolean registerRequest( final String urlString )
	{
		if ( !urlString.startsWith( "shop.php" ) || !urlString.contains( "whichshop=applestore" ) )
		{
			return false;
		}

		CoinmasterData data = AppleStoreRequest.APPLE_STORE;
		return CoinMasterRequest.registerRequest( data, urlString, true );
	}
}
