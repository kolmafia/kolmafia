package net.sourceforge.kolmafia.textui.command;

import net.sourceforge.kolmafia.KoLmafia;

public class ExitCommand
	extends AbstractCommand
{
	public ExitCommand()
	{
		this.usage = " - logout and exit KoLmafia.";
	}

	@Override
	public void run( final String cmd, final String parameters )
	{
		KoLmafia.quit();
	}

}
