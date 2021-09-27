package net.sourceforge.kolmafia.swingui.menu;

import javax.swing.JMenuItem;

import net.sourceforge.kolmafia.swingui.listener.ThreadedListener;

public class ThreadedMenuItem
	extends JMenuItem
{
	public ThreadedMenuItem( final String label, ThreadedListener action )
	{
		super( label );

		this.addActionListener( action );
	}

	public void setAction( ThreadedListener action )
	{
		this.addActionListener( action );
	}

	@Override
	public String toString()
	{
		return this.getText();
	}
}
