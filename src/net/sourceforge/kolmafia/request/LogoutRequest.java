package net.sourceforge.kolmafia.request;

import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.session.ChoiceManager;

public class LogoutRequest
	extends GenericRequest
{
	private static boolean isRunning = false;
	private static String lastResponse = "";

	public LogoutRequest()
	{
		super( "logout.php" );
	}

	@Override
	protected boolean shouldFollowRedirect()
	{
		return true;
	}

	@Override
	protected boolean retryOnTimeout()
	{
		return false;
	}

	@Override
	public void run()
	{
		if ( LogoutRequest.isRunning )
		{
			return;
		}

		LogoutRequest.isRunning = true;

		KoLmafia.updateDisplay( "Sending logout request..." );

		super.run();

		KoLmafia.updateDisplay( "Logout request submitted." );

		LogoutRequest.isRunning = false;
		
		// Clear up things you might be in the middle of that can stop execution on another character
		if ( FightRequest.currentRound != 0 )
		{
			FightRequest.preFight( false );
		}
		if ( FightRequest.inMultiFight )
		{
			FightRequest.inMultiFight = false;
		}
		if ( FightRequest.choiceFollowsFight )
		{
			FightRequest.choiceFollowsFight = false;
		}
		if ( ChoiceManager.handlingChoice )
		{
			ChoiceManager.handlingChoice = false;
		}		
	}

	@Override
	public void processResults()
	{
		LogoutRequest.lastResponse = this.responseText;
	}

	public static final String getLastResponse()
	{
		return LogoutRequest.lastResponse;
	}
}
