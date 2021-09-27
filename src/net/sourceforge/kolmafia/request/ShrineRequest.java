package net.sourceforge.kolmafia.request;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestLogger;

import net.sourceforge.kolmafia.objectpool.IntegerPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.session.ResultProcessor;


public class ShrineRequest
	extends GenericRequest
{
	public static final int BORIS = 1;
	public static final int JARLSBERG = 2;
	public static final int PETE = 3;

	public static final Object[][] SHRINE_DATA =
	{
		{
			IntegerPool.get( ShrineRequest.BORIS ),
			"boris",
			"Statue of Boris",
			"heroDonationBoris",
			ItemPool.get( ItemPool.BORIS_KEY, 1 ),
		},
		{
			IntegerPool.get( ShrineRequest.JARLSBERG ),
			"jarlsberg",
			"Statue of Jarlsberg",
			"heroDonationJarlsberg",
			ItemPool.get( ItemPool.JARLSBERG_KEY, 1 ),
		},
		{
			IntegerPool.get( ShrineRequest.PETE ),
			"sneakypete",
			"Statue of Sneaky Pete",
			"heroDonationSneakyPete",
			ItemPool.get( ItemPool.SNEAKY_PETE_KEY, 1 ),
		},
	};

	private static int dataId( final Object[] data )
	{
		return ( data == null ) ? 0 : ((Integer) data[0]).intValue();
	}

	private static String dataAction( final Object[] data )
	{
		return ( data == null ) ? null : ((String) data[1]);
	}

	private static String dataPlace( final Object[] data )
	{
		return ( data == null ) ? null : ((String) data[2]);
	}

	private static String dataSetting( final Object[] data )
	{
		return ( data == null ) ? null : ((String) data[3]);
	}

	private static AdventureResult dataKey( final Object[] data )
	{
		return ( data == null ) ? null : ((AdventureResult) data[4]);
	}

	private static Object[] idToData( final int id )
	{
		for ( int i = 0; i < SHRINE_DATA.length; ++i )
		{
			Object [] data = SHRINE_DATA[i];
			if ( id == dataId( data ) )
			{
				return data;
			}
		}
		return null;
	}

	private static Object[] actionToData( final String action )
	{
		for ( int i = 0; i < SHRINE_DATA.length; ++i )
		{
			Object [] data = SHRINE_DATA[i];
			if ( action.equals( dataAction( data ) ) )
			{
				return data;
			}
		}
		return null;
	}

	private static String actionToPlace( final String action )
	{
		return dataPlace( actionToData( action ) );
	}

	private final int amount;
	private final boolean hasStatueKey;

	/**
	 * Constructs a new <code>ShrineRequest</code>.
	 *
	 * @param heroId The identifier for the hero to whom you are donating
	 * @param amount The amount you're donating to the given hero
	 */

	public ShrineRequest( final int heroId, final int amount )
	{
		super( "da.php" );

		Object [] data = idToData( heroId );
		AdventureResult key = null;

		if ( data != null )
		{
			this.addFormField( "action", dataAction( data ) );
			key = dataKey( data );
		}
		this.hasStatueKey = key != null && KoLConstants.inventory.contains( key );

		this.addFormField( "howmuch", String.valueOf( amount ) );
		this.amount = amount;
	}

	/**
	 * Runs the request. Note that this does not report an error if it
	 * fails; it merely parses the results to see if any gains were made.
	 */

	@Override
	public void run()
	{
		if ( !this.hasStatueKey )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "You don't have the appropriate key." );
			return;
		}
		super.run();
	}

	@Override
	public void processResults()
	{
		String error = ShrineRequest.parseResponse( this.getURLString(), this.responseText );
		if ( error != null )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, error );
			return;
		}
		KoLmafia.updateDisplay( "Donation complete." );
	}

	public static final String parseResponse( final String urlString, final String responseText )
	{
		if ( !urlString.startsWith( "da.php" ) )
		{
			return null;
		}

		if ( responseText.contains( "bgshrine.gif" ) )
		{
			Preferences.setBoolean( "barrelShrineUnlocked", true );
			if ( responseText.contains( "already prayed to the Barrel god" ) )
			{
				Preferences.setBoolean( "_barrelPrayer", true );
			}
		}

		String action = GenericRequest.getAction( urlString );
		if ( action == null )
		{
			return null;
		}

		int qty = GenericRequest.getHowMuch( urlString );
		if ( qty < 0 )
		{
			return null;
		}

		Object [] data = actionToData( action );
		if ( data == null )
		{
			return null;
		}

		// If we get here, we tried donating

		String preference = dataSetting( data );

		if ( !responseText.contains( "You gain" ) )
		{
			return !responseText.contains( "That's not enough" ) ?
				"Donation limit exceeded." :
				"Donation must be larger.";
		}

		ResultProcessor.processMeat( 0 - qty );
		Preferences.increment( preference, qty );

		return null;
	}

	public static final boolean registerRequest( final String urlString )
	{
		if ( !urlString.startsWith( "da.php" ) )
		{
			return false;
		}

		String message = null;
		
		if ( urlString.contains( "barrelshrine=1" ) )
		{
			message = "Worshiping at the Shrine to the Barrel God";
		}
		else
		{
			String action = GenericRequest.getAction( urlString );

			// We have nothing special to do for simple visits.
			if ( action == null )
			{
				return true;
			}

			String place = ShrineRequest.actionToPlace( action );
			if ( place == null )
			{
				return false;
			}

			int qty = GenericRequest.getHowMuch( urlString );
			if ( qty < 0 )
			{
				return false;
			}

			message = "Donating " + qty + " Meat to the " + place;
		}

		if ( message == null )
		{
			return false;
		}

		RequestLogger.printLine( "" );
		RequestLogger.printLine( message );

		RequestLogger.updateSessionLog();
		RequestLogger.updateSessionLog( message );

		return true;
	}
}
