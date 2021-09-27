package net.sourceforge.kolmafia.textui.command;

import net.sourceforge.kolmafia.DebugModifiers;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;

public class ModifierListCommand
	extends AbstractCommand
{
	public ModifierListCommand()
	{
		this.usage = " <filter> - list all possible sources of modifiers matching filter.";
	}

	@Override
	public void run( final String cmd, final String parameters )
	{
		int count = DebugModifiers.setup( parameters.toLowerCase() );
		if ( count == 0 )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "No matching modifiers - use 'modref' to list." );
			return;
		}
		else if ( count > 10 )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "Too many matching modifiers - use 'modref' to list." );
			return;
		}
		DebugModifiers.allModifiers();
	}
}
