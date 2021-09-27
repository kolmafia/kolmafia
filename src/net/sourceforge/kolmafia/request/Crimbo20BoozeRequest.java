package net.sourceforge.kolmafia.request;

import net.java.dev.spellcast.utilities.LockableListModel;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.CoinmastersDatabase;

import java.util.Map;
import java.util.regex.Pattern;

public class Crimbo20BoozeRequest
	extends CoinMasterRequest
{
	public static final String master = "Elf Booze Drive";

	private static final LockableListModel<AdventureResult> buyItems = CoinmastersDatabase.getBuyItems( Crimbo20BoozeRequest.master );
	private static final Map<Integer, Integer> buyPrices = CoinmastersDatabase.getBuyPrices( Crimbo20BoozeRequest.master );
	private static final Map<Integer, Integer> itemRows = CoinmastersDatabase.getRows( Crimbo20BoozeRequest.master );
	private static final Pattern TOKEN_PATTERN = Pattern.compile( "([\\d,]+) (boxes of )?donated booze" );
	public static final AdventureResult TOKEN = ItemPool.get( ItemPool.DONATED_BOOZE, 1 );

	public static final CoinmasterData CRIMBO20BOOZE =
		new CoinmasterData(
			Crimbo20BoozeRequest.master,
			"crimbo20booze",
			Crimbo20BoozeRequest.class,
			"donated booze",
			"no boxes of donated booze",
			false,
			Crimbo20BoozeRequest.TOKEN_PATTERN,
			Crimbo20BoozeRequest.TOKEN,
			null,
			Crimbo20BoozeRequest.itemRows,
			"shop.php?whichshop=crimbo20booze",
			"buyitem",
			Crimbo20BoozeRequest.buyItems,
			Crimbo20BoozeRequest.buyPrices,
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
					case ItemPool.BOOZE_DRIVE_BUTTON:
					case ItemPool.BOOZE_MAILING_LIST:
						AdventureResult item = ItemPool.get( itemId );
						return item.getCount( KoLConstants.closet ) + item.getCount( KoLConstants.inventory ) == 0;
				}
				return super.canBuyItem( itemId );
			}
		};

	public Crimbo20BoozeRequest()
	{
		super(Crimbo20BoozeRequest.CRIMBO20BOOZE );
	}

	public Crimbo20BoozeRequest(final boolean buying, final AdventureResult [] attachments )
	{
		super(Crimbo20BoozeRequest.CRIMBO20BOOZE, buying, attachments );
	}

	public Crimbo20BoozeRequest(final boolean buying, final AdventureResult attachment )
	{
		super(Crimbo20BoozeRequest.CRIMBO20BOOZE, buying, attachment );
	}

	public Crimbo20BoozeRequest(final boolean buying, final int itemId, final int quantity )
	{
		super(Crimbo20BoozeRequest.CRIMBO20BOOZE, buying, itemId, quantity );
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
		Crimbo20BoozeRequest.parseResponse( this.getURLString(), this.responseText );
	}

	public static void parseResponse( final String location, final String responseText )
	{
		if ( !location.contains( "whichshop=crimbo20booze" ) )
		{
			return;
		}

		CoinmasterData data = Crimbo20BoozeRequest.CRIMBO20BOOZE;

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
		if ( !urlString.startsWith( "shop.php" ) || !urlString.contains( "whichshop=crimbo20booze" ) )
		{
			return false;
		}

		CoinmasterData data = Crimbo20BoozeRequest.CRIMBO20BOOZE;
		return CoinMasterRequest.registerRequest( data, urlString, true );
	}
}
