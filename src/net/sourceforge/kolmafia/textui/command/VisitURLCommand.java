
package net.sourceforge.kolmafia.textui.command;

import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.StaticEntity;
import net.sourceforge.kolmafia.request.GenericRequest;

public class VisitURLCommand
	extends AbstractCommand
{
	public VisitURLCommand()
	{
		usage = " <URL> - show text results from visiting URL.";
	}

	public void run( String cmd, String parameters )
	{
		GenericRequest visitor = new GenericRequest( parameters );
		if ( GenericRequest.shouldIgnore( visitor ) )
		{
			return;
		}

		RequestThread.postRequest( visitor );
		StaticEntity.externalUpdate( visitor.getURLString(), visitor.responseText );

		if ( cmd.equals( "text" ) )
		{
			this.CLI.showHTML( visitor.getURLString(), visitor.responseText );
		}
	}

}
