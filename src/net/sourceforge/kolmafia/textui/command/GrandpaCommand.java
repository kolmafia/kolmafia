package net.sourceforge.kolmafia.textui.command;

import net.sourceforge.kolmafia.RequestThread;

import net.sourceforge.kolmafia.request.GrandpaRequest;

public class GrandpaCommand
	extends AbstractCommand
{
	public GrandpaCommand()
	{
		this.usage = " <query> - Ask Grandpa about something.";
	}

	@Override
	public void run( final String cmd, final String parameters )
	{
		RequestThread.postRequest( new GrandpaRequest( parameters ) );
	}
}
