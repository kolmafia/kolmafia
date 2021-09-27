package net.sourceforge.kolmafia.textui.command;

import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.session.DadManager;

public class DadCommand
	extends AbstractCommand
{
	public DadCommand()
	{
		this.usage = " - show the round-by-round elemental weaknesses of Dad Sea Monkee.";
	}

	@Override
	public void run( final String cmd, final String parameters )
	{

		for ( int i = 1; i <= 10; ++i )
		{
			DadManager.Element weakness = DadManager.ElementalWeakness[ i ];
			RequestLogger.printLine( "Round " + i + ": " +
						 DadManager.elementToName( weakness ) +
						 " (" +	DadManager.elementToSpell( weakness ) + ")" );
		}
	}
}
