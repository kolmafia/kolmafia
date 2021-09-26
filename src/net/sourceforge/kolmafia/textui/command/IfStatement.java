package net.sourceforge.kolmafia.textui.command;

import net.sourceforge.kolmafia.KoLmafiaCLI;

public class IfStatement
	extends ConditionalStatement
{
	public IfStatement()
	{
		this.usage = " <condition>; <commands> - do commands once if condition is true (see condref).";
	}

	@Override
	public void run( final String command, final String parameters )
	{
		KoLmafiaCLI CLI = this.CLI;
		if ( ConditionalStatement.test( parameters ) )
		{
			CLI.elseInvalid();
			CLI.executeLine( this.continuation );
			CLI.elseRuns( false );
		}
		else
		{
			CLI.elseRuns( true );
		}
	}
}
