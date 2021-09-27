package net.sourceforge.kolmafia.textui.command;

import net.sourceforge.kolmafia.session.PvpManager;

public class FlowerHuntCommand
	extends AbstractCommand
{
	public FlowerHuntCommand()
	{
		this.usage = " - commit random acts of PvP.";
	}

	@Override
	public void run( final String cmd, final String parameters )
	{
		PvpManager.executePvpRequest( 0, "flowers", 0 );
	}
}
