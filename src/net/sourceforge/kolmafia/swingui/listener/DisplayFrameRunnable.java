package net.sourceforge.kolmafia.swingui.listener;

import net.sourceforge.kolmafia.KoLmafiaGUI;

public class DisplayFrameRunnable
	implements Runnable
{
	private final String frameClass;

	public DisplayFrameRunnable( String frameClass )
	{
		this.frameClass = frameClass;
	}

	public void run()
	{
		KoLmafiaGUI.constructFrame( this.frameClass );
	}
}