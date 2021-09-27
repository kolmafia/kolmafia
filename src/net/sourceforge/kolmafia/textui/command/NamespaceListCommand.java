package net.sourceforge.kolmafia.textui.command;

import java.io.File;
import java.util.List;

import net.sourceforge.kolmafia.KoLmafiaASH;
import net.sourceforge.kolmafia.KoLmafiaCLI;
import net.sourceforge.kolmafia.RequestLogger;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.textui.AshRuntime;
import net.sourceforge.kolmafia.textui.ScriptRuntime;

public class NamespaceListCommand
	extends AbstractCommand
{
	public NamespaceListCommand()
	{
		this.usage = " [<filter>] - list namespace scripts and the functions they define.";
	}

	@Override
	public void run( final String cmd, final String parameters )
	{
		String[] scripts = Preferences.getString( "commandLineNamespace" ).split( "," );
		for ( int i = 0; i < scripts.length; ++i )
		{
			RequestLogger.printLine( scripts[i] );
			List<File> matches = KoLmafiaCLI.findScriptFile( scripts[i] );

			File f = matches.size() == 1 ? matches.get( 0 ) : null;
			if ( f == null )
			{
				continue;
			}

			ScriptRuntime interpreter = KoLmafiaASH.getInterpreter( f );
			if ( interpreter instanceof AshRuntime )
			{
				KoLmafiaASH.showUserFunctions( (AshRuntime) interpreter, parameters );
			}

			RequestLogger.printLine();
		}
	}
}
