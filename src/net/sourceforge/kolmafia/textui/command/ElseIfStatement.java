/**
 * 
 */

package net.sourceforge.kolmafia.textui.command;

import net.sourceforge.kolmafia.KoLmafiaCLI;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;

public class ElseIfStatement
	extends ConditionalStatement
{
	public ElseIfStatement()
	{
		this.usage = " <condition>; <commands> - do if condition is true but preceding condition was false.";
	}

	public void validateParameters( final String parameters )
	{
		if ( !this.CLI.elseValid() )
		{
			KoLmafia.updateDisplay(
				KoLConstants.ERROR_STATE,
				"'else' must follow a conditional command, and both must be at the outermost level." );
			return;
		}
		super.validateParameters( parameters );
	}

	public void run( final String command, final String parameters )
	{
		KoLmafiaCLI CLI = this.CLI;
		if ( !CLI.elseRuns() )
		{
			return;
		}

		if ( ConditionalStatement.test( parameters ) )
		{
			CLI.executeLine( this.continuation );
			CLI.elseRuns( false );
		}
	}
}
