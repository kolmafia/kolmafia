package net.sourceforge.kolmafia.textui.command;

import net.sourceforge.kolmafia.KoLmafia;

import net.sourceforge.kolmafia.persistence.ConcoctionDatabase;

import net.sourceforge.kolmafia.utilities.StringUtilities;

public class BudgetCommand
	extends AbstractCommand
{
	public BudgetCommand()
	{
		this.usage = " [<number>] - show [or set] the number of budgeted Hagnk's pulls.";
	}

	@Override
	public void run( final String cmd, final String parameters )
	{
		if ( parameters.length() > 0 )
		{
			ConcoctionDatabase.setPullsBudgeted( StringUtilities.parseInt( parameters ) );
		}
		KoLmafia.updateDisplay( ConcoctionDatabase.getPullsBudgeted() + " pulls budgeted for automatic use, " + ConcoctionDatabase.getPullsRemaining() + " pulls remaining." );
	}
}
