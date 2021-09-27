package net.sourceforge.kolmafia.textui.command;

import net.sourceforge.kolmafia.KoLmafiaCLI;

public class ElseIfStatement
	extends ConditionalStatement
{
	public ElseIfStatement()
	{
		this.usage = " <condition>; <commands> - do if condition is true but preceding condition was false.";
	}

	@Override
	public void run( final String command, final String parameters )
	{
		KoLmafiaCLI CLI = this.CLI;
		if ( !CLI.elseRuns() )
		{
			CLI.elseRuns( false );
		}
		else if ( ConditionalStatement.test( parameters ) )
		{
			CLI.executeLine( this.continuation );
			CLI.elseRuns( false );
		}
		else
		{
			CLI.elseRuns( true );
		}
	}
}
