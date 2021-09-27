package net.sourceforge.kolmafia.textui.command;

import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestThread;

import net.sourceforge.kolmafia.request.ClanLoungeRequest;
import net.sourceforge.kolmafia.request.ClanLoungeSwimmingPoolRequest;

public class SwimmingPoolCommand
	extends AbstractCommand
{
	public SwimmingPoolCommand()
	{
		this.usage = " cannonball | item | laps | ml | sprints | noncombat - work out in your clan's VIP lounge swimming pool";
	}

	@Override
	public void run( final String cmd, String parameters )
	{
		parameters = parameters.trim();
		if ( parameters.equals( "" ) )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "What do you want to do in the swimming pool?" );
			return;
		}

		int option;
		option = ClanLoungeRequest.findSwimmingOption( parameters );
		if ( option == 0 )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "I don't understand what '" + parameters + "' is." );
			return;
		} 

		RequestThread.postRequest( new ClanLoungeRequest( ClanLoungeRequest.SWIMMING_POOL, option ) );

		// Do additional pool options to dive for treasure if option 1.
		if ( option == 1 )
		{
			try
			{
				RequestThread.postRequest( new ClanLoungeSwimmingPoolRequest( ClanLoungeSwimmingPoolRequest.HANDSTAND ) );
				RequestThread.postRequest( new ClanLoungeSwimmingPoolRequest( ClanLoungeSwimmingPoolRequest.TREASURE ) );
			}
			finally
			{
				RequestThread.postRequest( new ClanLoungeSwimmingPoolRequest( ClanLoungeSwimmingPoolRequest.GET_OUT ) );
			}
		}
	}
}
