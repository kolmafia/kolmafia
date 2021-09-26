package net.sourceforge.kolmafia.textui.command;

import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.KoLmafiaCLI;
import net.sourceforge.kolmafia.RequestLogger;

import net.sourceforge.kolmafia.utilities.StringUtilities;

public class RepeatLineCommand
	extends AbstractCommand
{
	public RepeatLineCommand()
	{
		this.usage = " [<number>] - repeat previous line [number times].";
	}

	@Override
	public void run( final String cmd, final String parameters )
	{
		KoLmafiaCLI CLI = this.CLI;
		String previousLine = CLI.previousLine;
		if ( previousLine != null )
		{
			int repeatCount = parameters.length() == 0 ? 1 : StringUtilities.parseInt( parameters );
			for ( int i = 0; i < repeatCount && KoLmafia.permitsContinue(); ++i )
			{
				RequestLogger.printLine( "Repetition " + ( i + 1 ) + " of " + repeatCount + "..." );
				CLI.executeLine( previousLine );
			}
		}
	}
}
