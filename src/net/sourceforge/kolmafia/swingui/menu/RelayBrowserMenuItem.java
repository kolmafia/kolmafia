package net.sourceforge.kolmafia.swingui.menu;

import net.sourceforge.kolmafia.swingui.listener.RelayBrowserListener;

/**
 * Internal class which displays the given request inside of the current frame.
 */

public class RelayBrowserMenuItem
	extends ThreadedMenuItem
{
	public RelayBrowserMenuItem()
	{
		this( "Relay Browser", null );
	}

	public RelayBrowserMenuItem( final String label, final String location )
	{
		super( label, new RelayBrowserListener( location ) );
	}
}
