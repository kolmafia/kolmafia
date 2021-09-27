package net.sourceforge.kolmafia.textui.command;

import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.session.DreadScrollManager;

public class DreadscrollCommand
	extends AbstractCommand
{
	public DreadscrollCommand()
	{
		this.usage = " - show the clues you have discovered for the Mer-kin dreadscroll.";
	}

	@Override
	public void run( final String cmd, final String parameters )
	{
		RequestLogger.printLine( DreadScrollManager.getClues() );
		RequestLogger.printLine();
		RequestLogger.printLine( DreadScrollManager.getScrollText() );
	}
}
