package net.sourceforge.kolmafia.textui.command;

import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.KoLmafiaCLI;

public class ElseStatement
	extends ConditionalStatement
{
	public ElseStatement()
	{
		this.usage = " ; <commands> - do commands if preceding if/while/try didn't execute.";
	}

	@Override
	public void run( final String command, final String parameters )
	{
		if ( !parameters.equals( "" ) )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "Condition not allowed for else." );
			return;
		}
		KoLmafiaCLI CLI = this.CLI;
		if ( CLI.elseRuns() )
		{
			CLI.executeLine( this.continuation );
		}
	}
}
