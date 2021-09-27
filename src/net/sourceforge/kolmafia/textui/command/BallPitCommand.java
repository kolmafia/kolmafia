package net.sourceforge.kolmafia.textui.command;

import net.sourceforge.kolmafia.RequestThread;

import net.sourceforge.kolmafia.request.ClanRumpusRequest;
import net.sourceforge.kolmafia.request.ClanRumpusRequest.RequestType;

public class BallPitCommand
	extends AbstractCommand
{
	public BallPitCommand()
	{
		this.usage = " - jump in your clan's awesome ball pit";
	}

	@Override
	public void run( final String cmd, final String parameters )
	{
		RequestThread.postRequest( new ClanRumpusRequest( RequestType.BALLS ) );
	}
}
