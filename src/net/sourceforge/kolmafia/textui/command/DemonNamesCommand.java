package net.sourceforge.kolmafia.textui.command;

import net.sourceforge.kolmafia.KoLAdventure;
import net.sourceforge.kolmafia.RequestLogger;

import net.sourceforge.kolmafia.preferences.Preferences;

public class DemonNamesCommand
	extends AbstractCommand
{
	{
		this.usage = " - list the demon names you know.";
	}

	@Override
	public void run( final String cmd, final String parameters )
	{
		for ( int i = 0; i < KoLAdventure.DEMON_TYPES.length; ++i )
		{
			String index = String.valueOf( i + 1 );

			RequestLogger.printLine( index + ": " + Preferences.getString( "demonName" + index ) );
			if ( KoLAdventure.DEMON_TYPES[ i ][ 0 ] != null )
			{
				RequestLogger.printLine( " => Found in the " + KoLAdventure.DEMON_TYPES[ i ][ 0 ] );
			}
			RequestLogger.printLine( " => Gives " + KoLAdventure.DEMON_TYPES[ i ][ 1 ] );
		}
	}
}
