package net.sourceforge.kolmafia.textui.command;

import net.sourceforge.kolmafia.MonsterData;
import net.sourceforge.kolmafia.RequestLogger;

import net.sourceforge.kolmafia.combat.Macrofier;
import net.sourceforge.kolmafia.combat.MonsterStatusTracker;

import net.sourceforge.kolmafia.persistence.MonsterDatabase;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.request.FightRequest;

import net.sourceforge.kolmafia.utilities.StringUtilities;

public class MacroTestCommand
	extends AbstractCommand
{
	public MacroTestCommand()
	{
		this.usage = " [monster] - turns on macro debug and generates a macro for the given monster";
	}

	@Override
	public void run( final String cmd, String parameters )
	{
		int index = 0;
		if ( parameters != null )
		{
			int val = parameters.indexOf( "index=" );
			if ( val != -1 )
			{
				index = StringUtilities.parseInt( parameters.substring( val + 6).trim() );
				parameters = parameters.substring( 0, val ).trim();
			}

			if ( parameters.length() > 0 )
			{
				MonsterData monster = MonsterDatabase.findMonster( parameters );
				MonsterStatusTracker.setNextMonster( monster );
			}
		}
		
		try
		{
			Preferences.setBoolean( "macroDebug", true );
			FightRequest.setMacroPrefixLength( index );

			int lastComplexActionPrefix = 0;
			while ( true )
			{
				String macro = Macrofier.macrofy();
				int prefix = FightRequest.getMacroPrefixLength();

				if ( macro == null )
				{
					// Quit if final action in strategy is complex
					if ( lastComplexActionPrefix + 1 == prefix )
					{
						break;
					}
					lastComplexActionPrefix = prefix;
					FightRequest.setMacroPrefixLength( prefix + 1 );
					RequestLogger.printLine( "****action***" );
					RequestLogger.printLine();
					continue;
				}

				if ( prefix == 0 )
				{
					break;
				}
			}
		}
		finally
		{
			Preferences.setBoolean( "macroDebug", false );
		}
	}
}
