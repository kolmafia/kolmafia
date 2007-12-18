/**
 * Copyright (c) 2002-2005, Simone Bordet
 * All rights reserved.
 *
 * This software is distributable under the BSD license.
 * See the terms of the BSD license in the documentation provided with this software.
 */

package net.sourceforge.foxtrot.pumps;

/**
 * {@link foxtrot.EventPump EventPumps} that implement this interface have the possibility to filter AWT events before
 * they are dispatched. It is possible to retrieve the current EventPump used by {@link foxtrot.Worker Worker} and test
 * if implements this interface; if so, a custom EventFilter may be provided. <br />
 * Example usage:
 * 
 * <pre>
 * EventPump pump = Worker.getEventPump();
 * if (pump instanceof EventFilterable)
 * {
 *    // Save the old filter
 *    EventFilter old = ((EventFilterable)pump).getEventFilter();
 *
 *    try
 *    {
 *       // Set the custom filter
 *       ((EventFilterable)pump).setEventFilter(new EventFilter()
 *       {
 *          public boolean accept(AWTEvent event)
 *          {
 *             // Do something with the event...
 *             System.out.println("Event:" + event);
 *             return true;
 *          }
 *       });
 *
 *       Worker.post(new Job()
 *       {
 *          public Object run()
 *          {
 *             // ...
 *          }
 *       });
 *    }
 *    finally
 *    {
 *       // Restore the old filter
 *       ((EventFilterable)pump).setEventFilter(old);
 *    }
 * }
 * </pre>
 * 
 * Absolute care must be used when filtering AWT events, as your Swing application may not work properly if AWT events
 * are not dispatched properly.
 * 
 * @version $Revision: 1.3 $
 */
public interface EventFilterable
{
	/**
	 * Sets the EventFilter
	 * 
	 * @see #getEventFilter
	 */
	public void setEventFilter( EventFilter filter );

	/**
	 * Returns the EventFilter
	 * 
	 * @see #setEventFilter
	 */
	public EventFilter getEventFilter();
}
