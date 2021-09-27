package net.sourceforge.kolmafia.request;

import java.util.Map;

import java.util.regex.Pattern;

import net.java.dev.spellcast.utilities.LockableListModel;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.KoLConstants;

import net.sourceforge.kolmafia.objectpool.ItemPool;

import net.sourceforge.kolmafia.persistence.CoinmastersDatabase;

public class NuggletCraftingRequest
	extends CoinMasterRequest
{
	public static final String master = "Topiary Nuggletcrafting";

	public static final LockableListModel<AdventureResult> buyItems = CoinmastersDatabase.getBuyItems( NuggletCraftingRequest.master );
	private static final Map<Integer, Integer> buyPrices = CoinmastersDatabase.getBuyPrices( NuggletCraftingRequest.master );
	private static final Map<Integer, Integer> itemRows = CoinmastersDatabase.getRows( NuggletCraftingRequest.master );

	private static final Pattern TOKEN_PATTERN = Pattern.compile( "<td>([\\d,]+) topiary nugglet" );
	public static final AdventureResult TOPIARY_NUGGLET =  ItemPool.get( ItemPool.TOPIARY_NUGGLET, 1 );

	public static final CoinmasterData NUGGLETCRAFTING =
		new CoinmasterData(
			NuggletCraftingRequest.master,
			"NuggletCrafting",
			NuggletCraftingRequest.class,
			"topiary nugglet",
			"no topiary nugglets",
			false,
			NuggletCraftingRequest.TOKEN_PATTERN,
			NuggletCraftingRequest.TOPIARY_NUGGLET,
			null,
			NuggletCraftingRequest.itemRows,
			"shop.php?whichshop=topiary",
			"buyitem",
			NuggletCraftingRequest.buyItems,
			NuggletCraftingRequest.buyPrices,
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

	public NuggletCraftingRequest()
	{
		super( NuggletCraftingRequest.NUGGLETCRAFTING );
	}

	public NuggletCraftingRequest( final boolean buying, final AdventureResult [] attachments )
	{
		super( NuggletCraftingRequest.NUGGLETCRAFTING, buying, attachments );
	}

	public NuggletCraftingRequest( final boolean buying, final AdventureResult attachment )
	{
		super( NuggletCraftingRequest.NUGGLETCRAFTING, buying, attachment );
	}

	public NuggletCraftingRequest( final boolean buying, final int itemId, final int quantity )
	{
		super( NuggletCraftingRequest.NUGGLETCRAFTING, buying, itemId, quantity );
	}

	@Override
	public void processResults()
	{
		NuggletCraftingRequest.parseResponse( this.getURLString(), this.responseText );
	}

	public static void parseResponse( final String location, final String responseText )
	{
		if ( !location.startsWith( "shop.php" ) || !location.contains( "whichshop=topiary" ) )
		{
			return;
		}

		CoinmasterData data = NuggletCraftingRequest.NUGGLETCRAFTING;

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
		if ( NuggletCraftingRequest.TOPIARY_NUGGLET.getCount( KoLConstants.inventory ) == 0 )
		{
			return "You do not have a topiary nugglet in inventory";
		}
		return null;
	}

	public static final boolean registerRequest( final String urlString )
	{
		// shop.php?pwd&whichshop=topiary
		if ( !urlString.startsWith( "shop.php" ) || !urlString.contains( "whichshop=topiary" ) )
		{
			return false;
		}

		CoinmasterData data = NuggletCraftingRequest.NUGGLETCRAFTING;
		return CoinMasterRequest.registerRequest( data, urlString, true );
	}
}
