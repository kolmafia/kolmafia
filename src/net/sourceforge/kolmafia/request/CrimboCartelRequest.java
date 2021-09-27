package net.sourceforge.kolmafia.request;

import java.util.Map;

import java.util.regex.Pattern;

import net.java.dev.spellcast.utilities.LockableListModel;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.KoLCharacter;

import net.sourceforge.kolmafia.objectpool.ItemPool;

import net.sourceforge.kolmafia.persistence.CoinmastersDatabase;

public class CrimboCartelRequest
	extends CoinMasterRequest
{
	public static final String master = "Crimbo Cartel"; 
	private static final LockableListModel<AdventureResult> buyItems = CoinmastersDatabase.getBuyItems( CrimboCartelRequest.master );
	private static final Map<Integer, Integer> buyPrices = CoinmastersDatabase.getBuyPrices( CrimboCartelRequest.master );

	private static final Pattern TOKEN_PATTERN = Pattern.compile( "You currently have <b>([\\d,]+)</b> Crimbux" );
	public static final AdventureResult CRIMBUCK = ItemPool.get( ItemPool.CRIMBUCK, 1 );
	public static final CoinmasterData CRIMBO_CARTEL =
		new CoinmasterData(
			CrimboCartelRequest.master,
			"cartel",
			CrimboCartelRequest.class,
			"Crimbuck",
			"You do not currently have any Crimbux",
			false,
			CrimboCartelRequest.TOKEN_PATTERN,
			CrimboCartelRequest.CRIMBUCK,
			null,
			null,
			"crimbo09.php",
			"buygift",
			CrimboCartelRequest.buyItems,
			CrimboCartelRequest.buyPrices,
			null,
			null,
			null,
			null,
			"whichitem",
			GenericRequest.WHICHITEM_PATTERN,
			"howmany",
			GenericRequest.HOWMANY_PATTERN,
			null,
			null,
			true
			)
		{
			@Override
			public final boolean availableItem( final int itemId )
			{
				switch( itemId )
				{
				case ItemPool.CRIMBO_CAROL_V1:
					return KoLCharacter.getClassType().equals( KoLCharacter.SEAL_CLUBBER );

				case ItemPool.CRIMBO_CAROL_V2:
					return KoLCharacter.getClassType().equals( KoLCharacter.TURTLE_TAMER );

				case ItemPool.CRIMBO_CAROL_V3:
					return KoLCharacter.getClassType().equals( KoLCharacter.PASTAMANCER );

				case ItemPool.CRIMBO_CAROL_V4:
					return KoLCharacter.getClassType().equals( KoLCharacter.SAUCEROR );

				case ItemPool.CRIMBO_CAROL_V5:
					return KoLCharacter.getClassType().equals( KoLCharacter.DISCO_BANDIT );

				case ItemPool.CRIMBO_CAROL_V6:
					return KoLCharacter.getClassType().equals( KoLCharacter.ACCORDION_THIEF );
				}

				return super.availableItem( itemId );
			}
		};

	public CrimboCartelRequest()
	{
		super( CrimboCartelRequest.CRIMBO_CARTEL );
	}

	public CrimboCartelRequest( final boolean buying, final AdventureResult [] attachments )
	{
		super( CrimboCartelRequest.CRIMBO_CARTEL, buying, attachments );
	}

	public CrimboCartelRequest( final boolean buying, final AdventureResult attachment )
	{
		super( CrimboCartelRequest.CRIMBO_CARTEL, buying, attachment );
	}

	public CrimboCartelRequest( final boolean buying, final int itemId, final int quantity )
	{
		super( CrimboCartelRequest.CRIMBO_CARTEL, buying, itemId, quantity );
	}

	@Override
	public void processResults()
	{
		CrimboCartelRequest.parseResponse( this.getURLString(), this.responseText );
	}

	public static void parseResponse( final String location, final String responseText )
	{
		CoinmasterData data = CrimboCartelRequest.CRIMBO_CARTEL;
		String action = GenericRequest.getAction( location );
		if ( action == null )
		{
			if ( location.indexOf( "place=store" ) != -1 )
			{
				// Parse current coin balances
				CoinMasterRequest.parseBalance( data, responseText );
			}

			return;
		}

		CoinMasterRequest.parseResponse( data, location, responseText );
	}

	public static final boolean registerRequest( final String urlString )
	{
		// We only claim crimbo09.php?action=buygift
		if ( !urlString.startsWith( "crimbo09.php" ) )
		{
			return false;
		}

		CoinmasterData data = CrimboCartelRequest.CRIMBO_CARTEL;
		return CoinMasterRequest.registerRequest( data, urlString );
	}

	public static String accessible()
	{
		return "The Crimbo Cartel is not available";
	}
}
