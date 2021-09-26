package net.sourceforge.kolmafia.session;

import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.StandardRequest;

public class VoteMonsterManager
{
	public static void checkCounter()
	{
		if ( !StandardRequest.isAllowed( "Items", "voter registration form" ) )
		{
			return;
		}

		if ( Preferences.getString( "trackVoteMonster" ).equals( "false" ) )
		{
			return;
		}

		if ( Preferences.getString( "trackVoteMonster" ).equals( "free" ) && Preferences.getInteger( "_voteFreeFights" ) >= 3 )
		{
			return;
		}

		if ( TurnCounter.isCounting( "Vote Monster" ) )
		{
			return;
		}

		int turns = 11 - ( ( KoLCharacter.getTurnsPlayed() - 1 ) % 11 );
		TurnCounter.startCounting( turns, "Vote Monster", "absballot.gif" );
	}

	public static boolean voteMonsterNow()
	{
		int totalTurns = KoLCharacter.getTurnsPlayed();
		return totalTurns % 11 == 1 && Preferences.getInteger( "lastVoteMonsterTurn" ) != totalTurns;
	}
}
