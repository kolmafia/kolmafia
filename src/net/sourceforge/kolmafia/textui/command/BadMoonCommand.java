package net.sourceforge.kolmafia.textui.command;

import net.sourceforge.kolmafia.session.BadMoonManager;

public class BadMoonCommand
	extends AbstractCommand
{
	public BadMoonCommand()
	{
		this.usage = " - List status of special bad moon adventure.";
	}

	@Override
	public void run( final String cmd, final String parameters )
	{
		BadMoonManager.report();
	}
}
