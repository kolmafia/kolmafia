package net.sourceforge.kolmafia.request;

import java.util.Map;

import java.util.regex.Pattern;

import net.java.dev.spellcast.utilities.LockableListModel;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.RequestThread;

import net.sourceforge.kolmafia.objectpool.ItemPool;

import net.sourceforge.kolmafia.persistence.CoinmastersDatabase;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.request.EquipmentRequest;

import net.sourceforge.kolmafia.session.EquipmentManager;

public class RubeeRequest
	extends CoinMasterRequest
{
	public static final String master = "FantasyRealm Rubee&trade; Store";
	private static final LockableListModel<AdventureResult> buyItems = CoinmastersDatabase.getBuyItems(RubeeRequest.master );
	private static final Map<Integer, Integer> buyPrices = CoinmastersDatabase.getBuyPrices(RubeeRequest.master );
	private static final Map<Integer, Integer> itemRows = CoinmastersDatabase.getRows(RubeeRequest.master );

	private static final Pattern TOKEN_PATTERN = Pattern.compile( "<td>([\\d,]+) Rubees&trade;" );
	public static final AdventureResult COIN = ItemPool.get( ItemPool.RUBEE, 1 );
	public static final CoinmasterData RUBEE =
		new CoinmasterData(
			RubeeRequest.master,
			"FantasyRealm Store",
			RubeeRequest.class,
			"Rubee&trade;",
			null,
			false,
			RubeeRequest.TOKEN_PATTERN,
			RubeeRequest.COIN,
			null,
			RubeeRequest.itemRows,
			"shop.php?whichshop=fantasyrealm",
			"buyitem",
			RubeeRequest.buyItems,
			RubeeRequest.buyPrices,
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

	public RubeeRequest()
	{
		super(RubeeRequest.RUBEE );
	}

	public RubeeRequest( final boolean buying, final AdventureResult [] attachments )
	{
		super(RubeeRequest.RUBEE, buying, attachments );
	}

	public RubeeRequest( final boolean buying, final AdventureResult attachment )
	{
		super(RubeeRequest.RUBEE, buying, attachment );
	}

	public RubeeRequest( final boolean buying, final int itemId, final int quantity )
	{
		super(RubeeRequest.RUBEE, buying, itemId, quantity );
	}

	@Override
	public void processResults()
	{
		RubeeRequest.parseResponse( this.getURLString(), this.responseText );
	}

	public static void parseResponse( final String urlString, final String responseText )
	{
		if ( !urlString.contains( "whichshop=fantasyrealm" ) )
		{
			return;
		}

		CoinmasterData data = RubeeRequest.RUBEE;

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
		if ( !urlString.startsWith( "shop.php" ) || !urlString.contains( "whichshop=fantasyrealm" ) )
		{
			return false;
		}

		CoinmasterData data = RubeeRequest.RUBEE;
		return CoinMasterRequest.registerRequest( data, urlString, true );
	}

	public static String accessible()
	{
		return Preferences.getBoolean( "_frToday" ) || Preferences.getBoolean( "frAlways" ) ? null : "Need access to Fantasy Realm";
	}

	public void equip()
	{
		if ( !KoLCharacter.hasEquipped( ItemPool.FANTASY_REALM_GEM ) )
		{
			EquipmentRequest request = new EquipmentRequest( ItemPool.get( ItemPool.FANTASY_REALM_GEM, 1 ), EquipmentManager.ACCESSORY3 );
			RequestThread.postRequest( request );
		}
	}
}
