package net.sourceforge.kolmafia.request;

import net.java.dev.spellcast.utilities.LockableListModel;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.CoinmastersDatabase;

import java.util.Map;
import java.util.regex.Pattern;

public class Crimbo20CandyRequest
	extends CoinMasterRequest
{
	public static final String master = "Elf Candy Drive";

	private static final LockableListModel<AdventureResult> buyItems = CoinmastersDatabase.getBuyItems( Crimbo20CandyRequest.master );
	private static final Map<Integer, Integer> buyPrices = CoinmastersDatabase.getBuyPrices( Crimbo20CandyRequest.master );
	private static final Map<Integer, Integer> itemRows = CoinmastersDatabase.getRows( Crimbo20CandyRequest.master );
	private static final Pattern TOKEN_PATTERN = Pattern.compile( "([\\d,]+) (boxes of )?donated candy" );
	public static final AdventureResult TOKEN = ItemPool.get( ItemPool.DONATED_CANDY, 1 );

	public static final CoinmasterData CRIMBO20CANDY =
		new CoinmasterData(
			Crimbo20CandyRequest.master,
			"crimbo20candy",
			Crimbo20CandyRequest.class,
			"donated candy",
			"no boxes of donated candy",
			false,
			Crimbo20CandyRequest.TOKEN_PATTERN,
			Crimbo20CandyRequest.TOKEN,
			null,
			Crimbo20CandyRequest.itemRows,
			"shop.php?whichshop=crimbo20candy",
			"buyitem",
			Crimbo20CandyRequest.buyItems,
			Crimbo20CandyRequest.buyPrices,
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
			public final boolean canBuyItem( final int itemId )
			{
				switch ( itemId )
				{
					case ItemPool.CANDY_DRIVE_BUTTON:
					case ItemPool.CANDY_MAILING_LIST:
						AdventureResult item = ItemPool.get( itemId );
						return item.getCount( KoLConstants.closet ) + item.getCount( KoLConstants.inventory ) == 0;
				}
				return super.canBuyItem( itemId );
			}
		};

	public Crimbo20CandyRequest()
	{
		super(Crimbo20CandyRequest.CRIMBO20CANDY );
	}

	public Crimbo20CandyRequest(final boolean buying, final AdventureResult [] attachments )
	{
		super(Crimbo20CandyRequest.CRIMBO20CANDY, buying, attachments );
	}

	public Crimbo20CandyRequest(final boolean buying, final AdventureResult attachment )
	{
		super(Crimbo20CandyRequest.CRIMBO20CANDY, buying, attachment );
	}

	public Crimbo20CandyRequest(final boolean buying, final int itemId, final int quantity )
	{
		super(Crimbo20CandyRequest.CRIMBO20CANDY, buying, itemId, quantity );
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
		Crimbo20CandyRequest.parseResponse( this.getURLString(), this.responseText );
	}

	public static void parseResponse( final String location, final String responseText )
	{
		if ( !location.contains( "whichshop=crimbo20candy" ) )
		{
			return;
		}

		CoinmasterData data = Crimbo20CandyRequest.CRIMBO20CANDY;

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
		return "Crimbo is gone";
	}

	public static final boolean registerRequest( final String urlString )
	{
		if ( !urlString.startsWith( "shop.php" ) || !urlString.contains( "whichshop=crimbo20candy" ) )
		{
			return false;
		}

		CoinmasterData data = Crimbo20CandyRequest.CRIMBO20CANDY;
		return CoinMasterRequest.registerRequest( data, urlString, true );
	}
}
