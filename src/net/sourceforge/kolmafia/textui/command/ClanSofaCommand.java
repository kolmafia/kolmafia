/**
 * 
 */

package net.sourceforge.kolmafia.textui.command;

import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.request.ClanRumpusRequest;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class ClanSofaCommand
	extends AbstractCommand
{
	public ClanSofaCommand()
	{
		this.usage = " <number> - rest on your clan sofa for number turns.";
	}

	public void run( final String cmd, final String parameters )
	{
		RequestThread.postRequest( new ClanRumpusRequest( ClanRumpusRequest.SOFA ).setTurnCount( StringUtilities.parseInt( parameters ) ) );
	}
}