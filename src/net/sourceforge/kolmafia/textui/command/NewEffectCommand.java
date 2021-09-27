package net.sourceforge.kolmafia.textui.command;

import net.sourceforge.kolmafia.persistence.EffectDatabase;

public class NewEffectCommand
	extends AbstractCommand
{
	public NewEffectCommand()
	{
		this.usage = " <effect description ID> - learn a new effect";
	}

	@Override
	public void run( final String command, final String parameters )
	{
		if ( parameters.equals( "" ) )
		{
			return;
		}

		EffectDatabase.learnEffectId( null, parameters );
	}
}
