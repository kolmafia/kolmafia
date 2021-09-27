package net.sourceforge.kolmafia.textui.command;

import net.sourceforge.kolmafia.RequestThread;

import net.sourceforge.kolmafia.request.ClanRumpusRequest;
import net.sourceforge.kolmafia.request.ClanRumpusRequest.RequestType;

import net.sourceforge.kolmafia.utilities.StringUtilities;

public class ClanSofaCommand
	extends AbstractCommand
{
	public ClanSofaCommand()
	{
		this.usage = " <number> - rest on your clan sofa for number turns.";
	}

	@Override
	public void run( final String cmd, final String parameters )
	{
		ClanRumpusRequest request = new ClanRumpusRequest( RequestType.SOFA );
		int count = parameters.equals( "" ) ? 1 : StringUtilities.parseInt( parameters );
		request.setTurnCount( count );
		RequestThread.postRequest( request );
	}
}
