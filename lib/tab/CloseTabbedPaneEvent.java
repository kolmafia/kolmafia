/*
 * Created on Jun 23, 2004
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */

package tab;

import java.awt.Event;
import java.awt.event.MouseEvent;

/**
 * @author David_211245 To change the template for this generated type comment go to Window>Preferences>Java>Code
 *         Generation>Code and Comments
 */
public class CloseTabbedPaneEvent
	extends Event
{

	private final String description;
	private final MouseEvent e;
	private final int overTabIndex;

	public CloseTabbedPaneEvent( final MouseEvent e, final String description, final int overTabIndex )
	{
		super( null, 0, null );
		this.e = e;
		this.description = description;
		this.overTabIndex = overTabIndex;
	}

	public String getDescription()
	{
		return this.description;
	}

	public MouseEvent getMouseEvent()
	{
		return this.e;
	}

	public int getOverTabIndex()
	{
		return this.overTabIndex;
	}
}
