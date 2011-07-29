/**
 * Copyright (c) 2002-2008, Simone Bordet
 * All rights reserved.
 *
 * This software is distributable under the BSD license.
 * See the terms of the BSD license in the documentation provided with this software.
 */

package net.sourceforge.foxtrot.pumps;

import java.awt.AWTEvent;
import java.awt.EventQueue;

import sun.awt.SunToolkit;

/**
 * Specialized ConditionalEventPump for Sun's JDK 1.4, 5.0 and 6.0.
 * It fixes what I think is a misbehavior of {@link EventQueue#peekEvent()},
 * that does not flush pending events to the EventQueue before peeking for them.
 * The implementation of {@link EventQueue#getNextEvent()} calls
 * {@link SunToolkit#flushPendingEvents()} before returning the next event, thus
 * ensuring that all events are flushed to the EventQueue.
 * However, the implementation of {@link EventQueue#peekEvent()} does not call
 * {@link SunToolkit#flushPendingEvents()} resulting in peekEvents() returning
 * null (no events available) while getNextEvent() returns not null (a flushed
 * event).
 *
 * @version $Revision: 255 $
 */
public class SunJDK14ConditionalEventPump extends ConditionalEventPump
{
    /**
     * Flushes pending events before waiting for the next event.
     * There is a mismatch between the behavior of {@link java.awt.EventQueue#getNextEvent()}
     * and {@link java.awt.EventQueue#peekEvent()}: the first always flushes pending events,
     * the second does not. This missing flushing is the reason why peekEvent() returns null
     * causing the proxy implementation of Conditional.evaluate() to never return
     */
    protected AWTEvent waitForEvent()
    {
        EventQueue queue = getEventQueue();
        AWTEvent nextEvent = peekEvent(queue);
        if (nextEvent != null) return nextEvent;

        while (true)
        {
            SunToolkit.flushPendingEvents();
            synchronized (queue)
            {
                nextEvent = peekEvent(queue);
                if (nextEvent != null) return nextEvent;
                if (debug) System.out.println("[SunJDK14ConditionalEventPump] Waiting for events...");
                try
                {
                    queue.wait();
                }
                catch (InterruptedException e)
                {
                    Thread.currentThread().interrupt();
                    return null;
                }
            }
        }
    }
}
