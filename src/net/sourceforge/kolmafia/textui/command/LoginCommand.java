package net.sourceforge.kolmafia.textui.command;

import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.session.LogoutManager;

public class LoginCommand
	extends AbstractCommand
{
	public LoginCommand()
	{
		this.usage = " <username> - logout then log back in as username.";
	}

	@Override
	public void run( final String cmd, final String parameters )
	{
		if ( parameters == null || parameters.isEmpty() ) {
			// Print usage
			KoLmafia.updateDisplay( KoLConstants.MafiaState.ERROR, "login <i>username</i>" + this.usage );
			KoLmafia.updateDisplay( KoLConstants.MafiaState.ERROR, "Please provide username to login." );
			return;
		}
		if  ( KoLmafia.getSaveState( parameters ) == null || KoLmafia.getSaveState( parameters ).isEmpty() )
		{
			// Error, must have stored password for user.
			KoLmafia.updateDisplay( KoLConstants.MafiaState.ERROR, "No stored password for user: " + parameters );
			KoLmafia.updateDisplay( KoLConstants.MafiaState.ERROR, "Logout and enter credentials manually." );
			return;
		}
		LogoutManager.logout();
		this.CLI.attemptLogin( parameters );
	}
}
