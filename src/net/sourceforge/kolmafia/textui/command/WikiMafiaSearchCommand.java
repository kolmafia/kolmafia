package net.sourceforge.kolmafia.textui.command;

import net.sourceforge.kolmafia.utilities.StringUtilities;

import net.sourceforge.kolmafia.webui.RelayLoader;

public class WikiMafiaSearchCommand
	extends AbstractCommand
{
	public WikiMafiaSearchCommand()
	{
		this.usage = " <searchText> - perform search on KoLmafia Wiki.";
	}

	@Override
	public void run( final String cmd, final String parameters )
	{
		RelayLoader.openSystemBrowser( "https://wiki.kolmafia.us/index.php?search=" + StringUtilities.getURLEncode( parameters ) );
	}
}
