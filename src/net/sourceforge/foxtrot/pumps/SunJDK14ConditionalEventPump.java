/**
 * Copyright (c) 2002-2005, Simone Bordet
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
 * Specialized ConditionalEventPump for Sun's JDK 1.4 and 5.0.
 * It fixes what I think is a misbehavior of {@link java.awt.EventQueue#peekEvent()},
 * that does not flush pending events to the EventQueue before peeking for them.
 * @version $Revision: 1.9 $
 */
public class SunJDK14ConditionalEventPump extends ConditionalEventPump implements EventFilterable
{
   /**
    * Flushes pending events before peeking the EventQueue.
    * There is a mismatch between the behavior of {@link java.awt.EventQueue#getNextEvent()}
    * and {@link java.awt.EventQueue#peekEvent()}: the first always flushes pending events,
    * the second does not. This missing flushing is the reason why peekEvent() returns null
    * causing the proxy implementation of Conditional.evaluate() to never return
    */
   protected AWTEvent peekEvent(EventQueue queue)
   {
      sun.awt.SunToolkit.flushPendingEvents();
      return super.peekEvent(queue);
   }
}
