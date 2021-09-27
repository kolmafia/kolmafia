package net.sourceforge.kolmafia.textui.command;

import java.util.ArrayList;

import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.StaticEntity;

import net.sourceforge.kolmafia.listener.Listener;
import net.sourceforge.kolmafia.listener.PreferenceListenerRegistry;

import net.sourceforge.kolmafia.preferences.Preferences;

public class PrefTraceCommand
	extends AbstractCommand
{
	private static ArrayList<Listener> audience = null;	// keeps listeners from being GC'd

	public PrefTraceCommand()
	{
		this.usage = " <name> [, <name>]... - watch changes to indicated preferences";
	}

	@Override
	public synchronized void run( String command, final String parameters )
	{
		if ( audience != null )
		{
			audience = null;
			RequestLogger.printLine( "Previously watched prefs have been cleared." );
		}

		if ( parameters.equals( "" ) )
		{
			return;
		}

		String[] prefList = parameters.split( "\\s*,\\s*" );
		audience = new ArrayList<Listener>();
		for ( int i = 0; i < prefList.length; ++i )
		{
			audience.add( new PreferenceListener( prefList[ i ] ) );
		}
	}

	private static class PreferenceListener
		implements Listener
	{
		String name;

		public PreferenceListener( String name )
		{
			this.name = name;
			PreferenceListenerRegistry.registerPreferenceListener( name, this );
			this.update();
		}

		public void update()
		{
			String msg = "ptrace: " + this.name + " = " + Preferences.getString( this.name );
			RequestLogger.updateSessionLog( msg );
			if ( RequestLogger.isDebugging() )
			{
				StaticEntity.printStackTrace( msg );
				// msg also gets displayed in CLI
			}
			else
			{
				RequestLogger.printLine( msg );
			}
		}
	}
}
