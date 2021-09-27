package net.sourceforge.kolmafia.textui.command;

import net.sourceforge.kolmafia.KoLmafia;

import net.sourceforge.kolmafia.preferences.Preferences;

public class NamespaceAddCommand
	extends AbstractCommand
{
	public NamespaceAddCommand()
	{
		this.usage = " <filename> - add ASH script to namespace.";
	}

	@Override
	public void run( final String cmd, final String parameters )
	{
		// Validate the script first.

		this.CLI.executeCommand( "validate", parameters );
		if ( !KoLmafia.permitsContinue() )
		{
			return;
		}

		String namespace = Preferences.getString( "commandLineNamespace" );
		if ( namespace.startsWith( parameters + "," ) || namespace.endsWith( "," + parameters ) || namespace.indexOf( "," + parameters + "," ) != -1 )
		{
			return;
		}

		if ( namespace.equals( "" ) )
		{
			namespace = parameters;
		}
		else
		{
			namespace = namespace + "," + parameters;
		}

		Preferences.setString( "commandLineNamespace", namespace );
	}
}
