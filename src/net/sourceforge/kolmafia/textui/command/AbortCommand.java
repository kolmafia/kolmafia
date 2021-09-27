package net.sourceforge.kolmafia.textui.command;

import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;

public class AbortCommand
	extends AbstractCommand
{
	public AbortCommand()
	{
		this.usage = " [message] - stop current script or automated task.";
	}

	@Override
	public void run( final String cmd, final String parameters )
	{
		KoLmafia.updateDisplay( MafiaState.ABORT, parameters.length() == 0 ? "Script abort." : parameters );
	}
}
