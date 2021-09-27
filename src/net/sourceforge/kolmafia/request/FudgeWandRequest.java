package net.sourceforge.kolmafia.request;

import java.util.Map;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.java.dev.spellcast.utilities.LockableListModel;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.RequestThread;

import net.sourceforge.kolmafia.objectpool.IntegerPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;

import net.sourceforge.kolmafia.persistence.CoinmastersDatabase;
import net.sourceforge.kolmafia.persistence.ConcoctionDatabase;



public class FudgeWandRequest
	extends CoinMasterRequest
{
	public static final String master = "Fudge Wand"; 
	private static final LockableListModel<AdventureResult> buyItems = CoinmastersDatabase.getBuyItems( FudgeWandRequest.master );
	private static final Map<Integer, Integer> buyPrices = CoinmastersDatabase.getBuyPrices( FudgeWandRequest.master );

	private static final Pattern TOKEN_PATTERN = Pattern.compile( "(?:You've.*?got|You.*? have) (?:<b>)?([\\d,]+)(?:</b>)? fudgecule" );
	public static final AdventureResult FUDGECULE = ItemPool.get( ItemPool.FUDGECULE, 1 );
	public static final AdventureResult FUDGE_WAND = ItemPool.get( ItemPool.FUDGE_WAND, 1 );
	private static final Pattern OPTION_PATTERN = Pattern.compile( "option=(\\d+)" );
	public static final CoinmasterData FUDGEWAND =
		new CoinmasterData(
			FudgeWandRequest.master,
			"fudge",
			FudgeWandRequest.class,
			"fudgecule",
			null,
			false,
			FudgeWandRequest.TOKEN_PATTERN,
			FudgeWandRequest.FUDGECULE,
			null,
			null,
			"choice.php?whichchoice=562",
			null,
			FudgeWandRequest.buyItems,
			FudgeWandRequest.buyPrices,
			null,
			null,
			null,
			null,
			null,
			null,
			null,
			null,
			null,
			null,
			true
			);

	private static String lastURL = null;

	private static final Object[][] OPTIONS =
	{
		{ "1", IntegerPool.get( ItemPool.FUDGIE_ROLL ) },
		{ "2", IntegerPool.get( ItemPool.FUDGE_SPORK ) },
		{ "3", IntegerPool.get( ItemPool.FUDGE_CUBE ) },
		{ "4", IntegerPool.get( ItemPool.FUDGE_BUNNY ) },
		{ "5", IntegerPool.get( ItemPool.FUDGECYCLE ) },
	};

	private static String idToOption( final int id )
	{
		for ( int i = 0; i < OPTIONS.length; ++i )
		{
			Object [] option = OPTIONS[ i ];
			if ( ( (Integer) option[ 1 ] ).intValue() == id )
			{
				return (String) option[ 0 ];
			}
		}

		return null;
	}

	private static int optionToId( final String opt )
	{
		for ( int i = 0; i < OPTIONS.length; ++i )
		{
			Object [] option = OPTIONS[ i ];
			if ( opt.equals( option[ 0 ] ) )
			{
				return ( (Integer)option[ 1 ] ).intValue();
			}
		}

		return -1;
	}

	public FudgeWandRequest()
	{
		super( FudgeWandRequest.FUDGEWAND );
	}

	public FudgeWandRequest( final boolean buying, final AdventureResult [] attachments )
	{
		super( FudgeWandRequest.FUDGEWAND, buying, attachments );
	}

	public FudgeWandRequest( final boolean buying, final AdventureResult attachment )
	{
		super( FudgeWandRequest.FUDGEWAND, buying, attachment );
	}

	public FudgeWandRequest( final boolean buying, final int itemId, final int quantity )
	{
		super( FudgeWandRequest.FUDGEWAND, buying, itemId, quantity );
	}

	@Override
	public void setItem( final AdventureResult item )
	{
		int itemId = item.getItemId();
		String option = FudgeWandRequest.idToOption( itemId );
		this.addFormField( "option", option );
	}

	@Override
	public void processResults()
	{
		FudgeWandRequest.parseResponse( this.getURLString(), this.responseText );
	}

	public static void parseResponse( final String location, final String responseText )
	{
		CoinmasterData data = FudgeWandRequest.FUDGEWAND;

		if ( location.indexOf( "option=6" ) != -1 )
		{
			// We exited the choice adventure.
			return;
		}

		Matcher optionMatcher = FudgeWandRequest.OPTION_PATTERN.matcher( location );
		if ( !optionMatcher.find() )
		{
			return;
		}

		String option = optionMatcher.group( 1 );
		int itemId = FudgeWandRequest.optionToId( option );

		if ( itemId != -1 )
		{
			CoinMasterRequest.completePurchase( data, itemId, 1, false );
		}

		CoinMasterRequest.parseBalance( data, responseText );
		ConcoctionDatabase.setRefreshNeeded( true );
	}

	public static final boolean registerRequest( final String urlString )
	{
		CoinmasterData data = FudgeWandRequest.FUDGEWAND;

		// using the wand is simply a visit
		// inv_use.php?whichitem=5441
		if ( urlString.startsWith( "inv_use.php" ) && urlString.contains( "whichitem=5441" ) )
		{
			RequestLogger.updateSessionLog();
			RequestLogger.updateSessionLog( "Visiting " + data.getMaster() );
			return true;
		}

		// choice.php?pwd&whichchoice=562&option=3
		if ( !urlString.startsWith( "choice.php" ) || !urlString.contains( "whichchoice=562" ) )
		{
			return false;
		}

		if ( urlString.contains( "option=6" ) )
		{
			// We exited the choice adventure.
			return true;
		}

		// Save URL.
		FudgeWandRequest.lastURL = urlString;

		Matcher optionMatcher = FudgeWandRequest.OPTION_PATTERN.matcher( urlString );
		if ( !optionMatcher.find() )
		{
			return true;
		}

		String option = optionMatcher.group( 1 );
		int itemId = FudgeWandRequest.optionToId( option );

		if ( itemId != -1 )
		{
			CoinMasterRequest.buyStuff( data, itemId, 1, false );
		}

		return true;
	}

	public static String accessible()
	{
		int wand = FudgeWandRequest.FUDGE_WAND.getCount( KoLConstants.inventory );
		if ( wand == 0 )
		{
			return "You don't have a wand of fudge control";
		}

		int fudgecules = FudgeWandRequest.FUDGECULE.getCount( KoLConstants.inventory );
		if ( fudgecules == 0 )
		{
			return "You don't have any fudgecules";
		}
		return null;
	}

	@Override
	public void equip()
	{
		// Use the wand of fudge control
		RequestThread.postRequest( new GenericRequest( "inv_use.php?whichitem=5441" ) );
	}

	@Override
	public void unequip()
	{
		RequestThread.postRequest( new GenericRequest( "choice.php?whichchoice=562&option=6" ) );
	}
}
