package net.sourceforge.kolmafia.listener;

public class PreferenceListenerRegistry
	extends ListenerRegistry
{
	// The registry of listeners:
	private static final ListenerRegistry INSTANCE = new ListenerRegistry();

	public static void deferPreferenceListeners( boolean deferring )
	{
		PreferenceListenerRegistry.INSTANCE.deferListeners( deferring );
	}

	public static final void registerPreferenceListener( final String name, final Listener listener )
	{
		PreferenceListenerRegistry.INSTANCE.registerListener( name, listener );
	}

	public static final void firePreferenceChanged( final String name )
	{
		PreferenceListenerRegistry.INSTANCE.fireListener( name );
	}

	public static final void fireAllPreferencesChanged()
	{
		PreferenceListenerRegistry.INSTANCE.fireAllListeners();
	}
}
