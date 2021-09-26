package net.sourceforge.kolmafia.textui.command;

import net.sourceforge.kolmafia.KoLmafiaGUI;

import net.sourceforge.kolmafia.request.LoginRequest;

public class PripheaCommand
	extends AbstractCommand
{
	public PripheaCommand()
	{
		this.usage = " - launch KoLmafia GUI.";
	}

	@Override
	public void run( final String cmd, final String parameters )
	{
		if ( LoginRequest.completedLogin() )
		{
			KoLmafiaGUI.intializeMainInterfaces();
		}
		else
		{
			KoLmafiaGUI.initializeLoginInterface();
		}
	}
}
