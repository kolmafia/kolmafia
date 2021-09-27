package net.sourceforge.kolmafia.request;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.kolmafia.KoLAdventure;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestLogger;

public class RichardRequest
	extends GenericRequest
{
	public static final int MYSTICALITY = 1;
	public static final int MOXIE = 2;
	public static final int MUSCLE = 3;

	private static final Pattern TURN_PATTERN = Pattern.compile( "numturns=(\\d+)" );

	private int turnCount = 1;

	/**
	 * Constructs a new <code>RichardRequest</code>.
	 *
	 * @param equipmentId The identifier for the equipment you're using
	 */

	public RichardRequest( final int equipmentId )
	{
		super( "clan_hobopolis.php" );
		this.addFormField( "place", "3" );
		this.addFormField( "preaction", "spendturns" );
		this.addFormField( "whichservice", String.valueOf( equipmentId ) );
	}

	public RichardRequest setTurnCount( final int turnCount )
	{
		this.turnCount = turnCount;
		return this;
	}

	@Override
	public void run()
	{
		this.addFormField( "numturns", String.valueOf( this.turnCount ) );

		if ( KoLCharacter.getAdventuresLeft() < this.turnCount )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "Insufficient adventures." );
			return;
		}

		KoLmafia.updateDisplay( "Helping Richard..." );

		super.run();
	}

	@Override
	public void processResults()
	{
		KoLmafia.updateDisplay( "Workout completed." );
	}

	/**
	 * An alternative method to doing adventure calculation is determining how many adventures are used by the given
	 * request, and subtract them after the request is done. This number defaults to <code>zero</code>; overriding
	 * classes should change this value to the appropriate amount.
	 *
	 * @return The number of adventures used by this request.
	 */

	@Override
	public int getAdventuresUsed()
	{
		return this.turnCount;
	}

	public static boolean registerRequest( final String urlString )
	{
		String gymType = null;

		if ( !urlString.startsWith( "clan_hobopolis.php" ) ||
			urlString.indexOf( "place=3" ) == -1 ||
			urlString.indexOf( "preaction=spendturns" ) == -1 )
		{
			return false;
		}

		if ( urlString.indexOf( "whichservice=1" ) != -1 )
		{
			gymType = "Help Richard make bandages (Mysticality)";
		}
		if ( urlString.indexOf( "whichservice=2" ) != -1 )
		{
			gymType = "Help Richard make grenades (Moxie)";
		}
		if ( urlString.indexOf( "whichservice=3" ) != -1 )
		{
			gymType = "Help Richard make shakes (Muscle)";
		}

		if ( gymType == null )
		{
			return false;
		}

		Matcher turnMatcher = RichardRequest.TURN_PATTERN.matcher( urlString );
		if ( !turnMatcher.find() )
		{
			return false;
		}

		RequestLogger.printLine( "[" + KoLAdventure.getAdventureCount() + "] " + gymType + " (" + turnMatcher.group( 1 ) + " turns)" );
		RequestLogger.updateSessionLog( "[" + KoLAdventure.getAdventureCount() + "] " + gymType + " (" + turnMatcher.group( 1 ) + " turns)" );
		return true;
	}
}
