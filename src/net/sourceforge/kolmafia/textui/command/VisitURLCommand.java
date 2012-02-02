
package net.sourceforge.kolmafia.textui.command;

import net.sourceforge.kolmafia.RequestThread;

import net.sourceforge.kolmafia.request.GenericRequest;

import net.sourceforge.kolmafia.session.ResponseTextParser;

public class VisitURLCommand
	extends AbstractCommand
{
	public VisitURLCommand()
	{
		usage = " <URL> - show text results from visiting URL.";
	}

	public void run( String cmd, String parameters )
	{
		String url = cmd.equals( "text" ) ? parameters : cmd;
		
		GenericRequest visitor = new GenericRequest( url );

		if ( GenericRequest.shouldIgnore( visitor ) )
		{
			return;
		}

		RequestThread.postRequest( visitor );

		if ( cmd.equals( "text" ) )
		{
			this.CLI.showHTML( visitor.getURLString(), visitor.responseText );
		}
	}

}
