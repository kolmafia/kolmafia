package net.sourceforge.kolmafia.swingui.listener;

import net.sourceforge.kolmafia.webui.RelayLoader;

public class RelayBrowserRunnable
	implements Runnable
{
	private String location;

	public RelayBrowserRunnable( String location )
	{
		this.location = location;
	}

	public void run()
	{
		if ( this.location == null )
		{
			RelayLoader.openRelayBrowser();
		}
		else
		{
			RelayLoader.openSystemBrowser( this.location );
		}
	}
}