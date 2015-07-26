
package com.jeans.trayicon;

public class TrayBalloonEvent
{

	public final static int SHOW = 1;
	public final static int HIDE = 2;
	public final static int CLICK = 4;
	public final static int TIMEOUT = 8;

	protected int m_Mask;

	public TrayBalloonEvent( final int mask )
	{
		this.m_Mask = mask;
	}

	public int getMask()
	{
		return this.m_Mask;
	}
}
