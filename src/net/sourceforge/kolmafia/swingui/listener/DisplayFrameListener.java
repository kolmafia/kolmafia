package net.sourceforge.kolmafia.swingui.listener;

import net.sourceforge.kolmafia.KoLmafiaGUI;

public class DisplayFrameListener
	extends ThreadedListener
{
	private final String frameClass;

	public DisplayFrameListener( String frameClass )
	{
		this.frameClass = frameClass;
	}

	protected void execute()
	{
		KoLmafiaGUI.constructFrame( this.frameClass );
	}
}