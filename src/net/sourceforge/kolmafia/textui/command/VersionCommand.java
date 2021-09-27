package net.sourceforge.kolmafia.textui.command;

import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.StaticEntity;

public class VersionCommand
	extends AbstractCommand
{
	public VersionCommand()
	{
		this.usage = " - display KoLmafia version.";
	}

	@Override
	public void run( final String cmd, final String parameters )
	{
		RequestLogger.printLine( StaticEntity.getVersion() );
	}
}
