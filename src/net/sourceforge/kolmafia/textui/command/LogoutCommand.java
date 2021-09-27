package net.sourceforge.kolmafia.textui.command;

import net.sourceforge.kolmafia.session.LogoutManager;

public class LogoutCommand
	extends AbstractCommand
{
	public LogoutCommand()
	{
		this.usage = " - logout and return to login window.";
	}

	@Override
	public void run( final String cmd, final String parameters )
	{
		LogoutManager.logout();
	}
}
