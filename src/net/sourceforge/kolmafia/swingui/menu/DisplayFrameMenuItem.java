package net.sourceforge.kolmafia.swingui.menu;

import net.sourceforge.kolmafia.swingui.listener.DisplayFrameListener;

/**
 * In order to keep the user interface from freezing (or at least appearing to freeze), this internal class is used
 * to process the request for viewing frames.
 */

public class DisplayFrameMenuItem
	extends ThreadedMenuItem
{
	public DisplayFrameMenuItem( final String title, final String frameClass )
	{
		super( title, new DisplayFrameListener( frameClass ) );
	}
}
