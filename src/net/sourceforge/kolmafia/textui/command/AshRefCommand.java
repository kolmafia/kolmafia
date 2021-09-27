package net.sourceforge.kolmafia.textui.command;

import net.sourceforge.kolmafia.KoLmafiaASH;

public class AshRefCommand
	extends AbstractCommand
{
	public AshRefCommand()
	{
		this.usage = " [<filter>] - summarize ASH built-in functions [matching filter].";
	}

	@Override
	public void run( final String cmd, final String parameters )
	{
		KoLmafiaASH.showExistingFunctions( parameters );
	}
}
