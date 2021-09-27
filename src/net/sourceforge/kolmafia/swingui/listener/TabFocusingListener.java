package net.sourceforge.kolmafia.swingui.listener;

import javax.swing.JComponent;
import javax.swing.JTabbedPane;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

public class TabFocusingListener
	implements ChangeListener
{
	public TabFocusingListener()
	{
		super();
	}

	public void stateChanged( final ChangeEvent e )
	{
		JTabbedPane pane = (JTabbedPane)e.getSource();
		JComponent selected = (JComponent)pane.getSelectedComponent();
		if ( selected != null )
		{
			selected.grabFocus();
		}
	}
}
