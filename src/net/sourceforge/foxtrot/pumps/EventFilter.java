/**
 * Copyright (c) 2002-2005, Simone Bordet
 * All rights reserved.
 *
 * This software is distributable under the BSD license.
 * See the terms of the BSD license in the documentation provided with this software.
 */

package net.sourceforge.foxtrot.pumps;

import java.awt.AWTEvent;

/**
 * Filters AWT events pumped by {@link foxtrot.EventPump EventPump}s before they're dispatched.
 * @see EventFilterable
 * @version $Revision: 1.4 $
 */
public interface EventFilter
{
   /**
    * Callback called by {@link foxtrot.EventPump EventPump}s to filter the given AWT event.
    * Implementations should return true if the event should be dispatched, false otherwise.
    * Beware that installing an EventFilter in one of the synchronous Foxtrot workers, and
    * returning always false from {@link #accept(AWTEvent)} makes the posts to the worker
    * hang.
    * @param event The event to filter
    */
   public boolean accept(AWTEvent event);
}
